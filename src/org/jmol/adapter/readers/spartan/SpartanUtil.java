/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-05 09:07:28 -0500 (Thu, 05 Apr 2007) $
 * $Revision: 7326 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.adapter.readers.spartan;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import javajs.api.GenericBinaryDocument;
import javajs.util.CompoundDocument;
import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.Rdr;
import javajs.util.SB;

import org.jmol.api.Interface;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.viewer.FileManager;


/**
 * A class to isolate Spartan file reading methods from the rest of Jmol.
 * 
 * Two public methods: getFileList and getData
 * 
 */
public class SpartanUtil {

  public FileManager fm;

  public SpartanUtil() {
    // for reflection
  }
  
  public SpartanUtil set(FileManager fm) {
    this.fm = fm;
    return this;
  }
 
  /**
   * get a complete critical file list for a spartan file Mac directory based on
   * file extension ".spardir.zip" or ".spardir"
   * 
   * @param name
   * @param isTypeCheckOnly
   * @return critical files list if just checking type or a buffered reader for a String containing all data
   */
  public Object getFileList(String name, boolean isTypeCheckOnly) {
    int pt = name.lastIndexOf(".spardir");
    String[] info = null;
    // check for zipped up spardir -- we'll automatically take first file there
    if (name.endsWith(".spardir.zip")) {
      info = new String[] { "SpartanSmol", "Directory Entry ", name + "|output" };
    } else {
      name = name.replace('\\', '/');
      if (!name.endsWith(".spardir") && name.indexOf(".spardir/") < 0)
        return null;
      // look for .spardir or .spardir/...
      info = (name.lastIndexOf("/") > pt ?
      // a single file in the spardir directory is requested
      new String[] { "SpartanSmol", "Directory Entry ", name + "/input",
          name + "/archive", name + "/parchive",
          name + "/Molecule:asBinaryString", name + "/proparc" }
          :
          // check output file for number of models      
          new String[] { "SpartanSmol", "Directory Entry ", name + "/output" });
    }
    // info[2] == null, for example, for an SPT file load that is not just a type check
    // (type check is only for application file opening and drag-drop to
    // determine if script or load command should be used)
    if (isTypeCheckOnly)
      return info;
    String name00 = name;
    String header = info[1]; //"Directory Entry"
    String outputFileName = info[2];
    Map<String, String> fileData = new Hashtable<String, String>();
    if (info.length == 3) {
      // we need information from the output file, info[2]
      outputFileName = spartanGetObjectAsSections(outputFileName, header, fileData);
      fileData.put("OUTPUT", outputFileName);
      info = spartanFileList(name, fileData.get(outputFileName));
      if (info.length == 3) {
        // output file was not found
        // might have a second option -- zip of directory name, not just contents
        outputFileName = spartanGetObjectAsSections(info[2], header, fileData);
        fileData.put("OUTPUT", outputFileName);
        info = spartanFileList(info[1], fileData.get(outputFileName));
      }
    }
    // load each file individually, but return files IN ORDER
    SB sb = new SB();
    String s;
    if (fileData.get("OUTPUT") != null) {
      sb.append(fileData.get(fileData.get("OUTPUT")));
    }
    for (int i = 2; i < info.length; i++) {
      name = info[i];
      name = spartanGetObjectAsSections(name, header, fileData);
      Logger.info("reading " + name);
      s = fileData.get(name);
      sb.append(s);
    }
    s = sb.toString();
    if (fm.spardirCache == null)
      fm.spardirCache = new Hashtable<String, byte[]>();
    fm.spardirCache.put(name00.replace('\\', '/'), s.getBytes());
    return Rdr.getBR(s);
  }

  
  /**
   * called by SmarterJmolAdapter via JmolUtil to 
   * open a Spartan directory and get all the needed data as a string.
   * 
   * @param is
   * @param zipDirectory
   * @return String data for processing
   */
  public SB getData(InputStream is, String[] zipDirectory) {
    SB data = new SB();
    data.append("Zip File Directory: ").append("\n")
        .append(Escape.eAS(zipDirectory, true)).append("\n");
    Map<String, String> fileData = new Hashtable<String, String>();
    fm.vwr.getJzt().getAllZipData(is, new String[] {}, "", "Molecule", "__MACOSX", fileData);
    String prefix = "|";
    String outputData = fileData.get(prefix + "output");
    if (outputData == null)
      outputData = fileData.get((prefix = "|" + zipDirectory[1]) + "output");
    data.append(outputData);
    String[] files = getSpartanFileList(prefix, getSpartanDirs(outputData));
    for (int i = 2; i < files.length; i++) {
      String name = files[i];
      if (fileData.containsKey(name))
        data.append(fileData.get(name));
      else
        data.append(name + "\n");
    }
    return data;
  }

  /**
   * 
   * Special loading for file directories. This method is called from the
   * FileManager via SmarterJmolAdapter. It's here because Resolver is the place
   * where all distinctions are made.
   * 
   * In the case of spt files, no need to load them; here we are just checking
   * for type.
   * 
   * In the case of .spardir directories, we need to provide a list of the
   * critical files that need loading and concatenation for the
   * SpartanSmolReader.
   * 
   * we return an array for which:
   * 
   * [0] file type (class prefix) or null for SPT file [1] header to add for
   * each BEGIN/END block (ignored) [2...] files to load and concatenate
   * 
   * @param name
   * @param outputFileData
   * @return array detailing action for this set of files
   */
  private String[] spartanFileList(String name, String outputFileData) {
    // make list of required files
    String[] dirNums = getSpartanDirs(outputFileData);
    if (dirNums.length == 0) {
      if (name.endsWith(".spardir"))
        return getSpartanFileList(name, new String[] { "M0001" });
      if (name.endsWith(".spardir.zip")) {
        if (outputFileData.indexOf(".zip|output") >= 0) {
          // try again, with the idea that 
          String sname = name.replace('\\', '/');
          int pt = name.lastIndexOf(".spardir");
          pt = sname.lastIndexOf("/");
          // mac directory zipped up?
          sname = name + "|" + PT.rep(name.substring(pt + 1, name.length() - 4), "DROP_", "");
          return new String[] { "SpartanSmol", sname, sname + "/output" };
        }
      }
    }
    return getSpartanFileList(name, dirNums);
  }

  /**
   * read the output file from the Spartan directory and decide from that what
   * files need to be read and in what order - usually M0001 or a set of
   * Profiles. But Spartan saves the Profiles in alphabetical order, not
   * numerical. So we fix that here.
   * 
   * @param outputFileData
   * @return String[] list of files to read
   */
  private String[] getSpartanDirs(String outputFileData) {
    if (outputFileData == null)
      return new String[] {};
    Lst<String> v = new Lst<String>();
    String token;
    String lastToken = "";
    if (outputFileData.startsWith("java.io.FileNotFoundException")
        || outputFileData.startsWith("FILE NOT FOUND")
        || outputFileData.indexOf("<html") >= 0)
      return new String[0];
    try {
      StringTokenizer tokens = new StringTokenizer(outputFileData, " \t\r\n");
      while (tokens.hasMoreTokens()) {
        // profile file name is just before each right-paren:
        /*
         * MacSPARTAN '08 ENERGY PROFILE: x86/Darwin 130
         * 
         * Dihedral Move : C3 - C2 - C1 - O1 [ 4] -180.000000 .. 180.000000
         * Dihedral Move : C2 - C1 - O1 - H3 [ 4] -180.000000 .. 180.000000
         * 
         * 1 ) -180.00 -180.00 -504208.11982719 2 ) -90.00 -180.00
         * -504200.18593376
         * 
         * ...
         * 
         * 24 ) 90.00 180.00 -504200.18564495 25 ) 180.00 180.00
         * -504208.12129747
         * 
         * Found a local maxima E = -504178.25455465 [ 3 3 ]
         * 
         * 
         * Reason for exit: Successful completion Mechanics CPU Time : 1:51.42
         * Mechanics Wall Time: 12:31.54
         */
        if ((token = tokens.nextToken()).equals(")"))
          v.addLast(lastToken);
        else if (token.equals("Start-")
            && tokens.nextToken().equals("Molecule"))
          v.addLast(PT.split(tokens.nextToken(), "\"")[1]);
        else if (token.equals("Molecules")) {
          //            Using internal queue
          //
          //            35 Molecules analyzed (35 succeeded)
          int n = PT.parseInt(lastToken);
          for (int i = 1; i <= n; i++) {
            String s = "0000" + i;
            v.addLast("M" + s.substring(s.length() - 4));
          }
        }
        lastToken = token;
      }
    } catch (Exception e) {
      //
    }
    return (v.size() == 0 ? new String[] { "M0001" } : v.toArray(new String[v.size()]));
  }

  /**
   * returns the list of files to read for every Spartan spardir. Simple numbers
   * are assumed to be Profiles; others are models.
   * 
   * @param name
   * @param dirNums
   * @return String[] list of files to read given a list of directory names
   * 
   */
  private String[] getSpartanFileList(String name, String[] dirNums) {
    String[] files = new String[2 + dirNums.length * 6];
    files[0] = "SpartanSmol";
    files[1] = "Directory Entry ";
    int pt = 2;
    name = name.replace('\\', '/');
    if (name.endsWith("/"))
      name = name.substring(0, name.length() - 1);
    String sep = (name.equals("|") ? "" : name.endsWith(".zip") ? "|" : "/");
    for (int i = 0; i < dirNums.length; i++) {
      String path = name + sep;
      String s = dirNums[i];
      path += (PT.isDigit(s.charAt(0)) ? "Profile." + s : s) + "/";
      files[pt++] = path + "#JMOL_MODEL " + dirNums[i];
      files[pt++] = path + "input";
      files[pt++] = path + "archive";
      files[pt++] = path + "parchive";
      files[pt++] = path + "Molecule:asBinaryString";
      files[pt++] = path + "proparc";
    }
    return files;
  }

  /**
   * delivers file contents and directory listing for a ZIP/JAR file into sb
   * 
   * @param name
   * @param header
   * @param fileData
   * @return name of entry
   */
  private String spartanGetObjectAsSections(String name, String header,
                                     Map<String, String> fileData) {
    if (name == null)
      return null;
    String[] subFileList = null;
    boolean asBinaryString = false;
    String path = name.replace('\\', '/');
    if (name.indexOf(":asBinaryString") >= 0) {
      asBinaryString = true;
      name = name.substring(0, name.indexOf(":asBinaryString"));
    }
    SB sb = null;
    if (fileData.containsKey(path))
      return path;
    if (path.indexOf("#JMOL_MODEL ") >= 0) {
      fileData.put(path, path + "\n");
      return path;
    }
    String fullName = name;
    if (name.indexOf("|") >= 0) {
      subFileList = PT.split(name, "|");
      name = subFileList[0];
    }
    BufferedInputStream bis = null;
    try {
      Object t = fm.getBufferedInputStreamOrErrorMessageFromName(name, fullName,
          false, false, null, false, true);
      if (t instanceof String) {
        fileData.put(path, (String) t + "\n");
        return path;
      }
      name = name.replace('\\', '/');
      bis = (BufferedInputStream) t;
      if (Rdr.isCompoundDocumentS(bis)) {
        // very specialized reader; assuming we have a Spartan document here
        CompoundDocument doc = (CompoundDocument) Interface
            .getInterface("javajs.util.CompoundDocument", fm.vwr, "file");
        doc.setDocStream(fm.vwr.getJzt(), bis);
        doc.getAllDataMapped(name, "Molecule", fileData);
      } else if (Rdr.isZipS(bis)) {
        fm.vwr.getJzt().getAllZipData(bis, subFileList, name, "Molecule",
            "__MACOSX", fileData);
      } else if (asBinaryString) {
        // used for Spartan binary file reading
        GenericBinaryDocument bd = (GenericBinaryDocument) Interface
            .getInterface("javajs.util.BinaryDocument", fm.vwr, "file");
        bd.setStream(bis, false);
        sb = new SB();
        //note -- these headers must match those in ZipUtil.getAllData and CompoundDocument.getAllData
        if (header != null)
          sb.append("BEGIN Directory Entry " + path + "\n");
        try {
          while (true)
            sb.append(Integer.toHexString(bd.readByte() & 0xFF)).appendC(' ');
        } catch (Exception e1) {
          sb.appendC('\n');
        }
        if (header != null)
          sb.append("\nEND Directory Entry " + path + "\n");
        fileData.put(path, sb.toString());
      } else {
        BufferedReader br = Rdr.getBufferedReader(
            Rdr.isGzipS(bis) ? new BufferedInputStream(fm.vwr.getJzt().newGZIPInputStream(bis)) : bis, null);
        String line;
        sb = new SB();
        if (header != null)
          sb.append("BEGIN Directory Entry " + path + "\n");
        while ((line = br.readLine()) != null) {
          sb.append(line);
          sb.appendC('\n');
        }
        br.close();
        if (header != null)
          sb.append("\nEND Directory Entry " + path + "\n");
        fileData.put(path, sb.toString());
      }
    } catch (Exception ioe) {
      fileData.put(path, ioe.toString());
    }
    if (bis != null)
      try {
        bis.close();
      } catch (Exception e) {
        //
      }
    if (!fileData.containsKey(path))
      fileData.put(path, "FILE NOT FOUND: " + path + "\n");
    return path;
  }  

}

