/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2018-01-28 23:38:16 -0600 (Sun, 28 Jan 2018) $
 * $Revision: 21814 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development Team
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
package org.jmol.viewer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Hashtable;
import java.util.Map;

import javajs.J2SIgnoreImport;
import javajs.api.BytePoster;
import javajs.api.GenericFileInterface;
import javajs.util.AU;
import javajs.util.BArray;
import javajs.util.Base64;
import javajs.util.CompoundDocument;
import javajs.util.DataReader;
import javajs.util.LimitedLineReader;
import javajs.util.Lst;
import javajs.util.OC;
import javajs.util.PT;
import javajs.util.Rdr;
import javajs.util.SB;

import org.jmol.adapter.readers.spartan.SpartanUtil;
import org.jmol.api.Interface;
import org.jmol.api.JmolDomReaderInterface;
import org.jmol.api.JmolFilesReaderInterface;
import org.jmol.io.FileReader;
import org.jmol.io.JmolUtil;
import org.jmol.script.SV;
import org.jmol.script.T;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer.ACCESS;


@J2SIgnoreImport({Rdr.StreamReader.class})
public class FileManager implements BytePoster {

  public static String SIMULATION_PROTOCOL = "http://SIMULATION/";

  public Viewer vwr;


  FileManager(Viewer vwr) {
    this.vwr = vwr;
    clear();
  }

  private SpartanUtil spartanDoc;
  
  /**
   * An isolated class to retrieve Spartan file data from compound documents, zip files, and directories
   * @return a SpartanUtil
   */
  public SpartanUtil spartanUtil() {
    return (spartanDoc == null ? spartanDoc = ((SpartanUtil) Interface.getInterface("org.jmol.adapter.readers.spartan.SpartanUtil", vwr, "fm getSpartanUtil()")).set(this) : spartanDoc);  
  }
  
  JmolUtil jzu;
 
  public JmolUtil getJzu() {
    return (jzu == null ? jzu = (JmolUtil) Interface.getOption("io.JmolUtil", vwr, "file") : jzu);
  }

  void clear() {
    // from zap
    setFileInfo(new String[] { vwr.getZapName() });
    spardirCache = null;   
  }

  private void setLoadState(Map<String, Object> htParams) {
    if (vwr.getPreserveState()) {
      htParams.put("loadState", vwr.g.getLoadState(htParams));
    }
  }

  private String pathForAllFiles = ""; // leave private because of setPathForAllFiles
  
  public String getPathForAllFiles() {
    return pathForAllFiles;
  }
  
  String setPathForAllFiles(String value) {
    if (value.length() > 0 && !value.endsWith("/") && !value.endsWith("|"))
        value += "/";
    return pathForAllFiles = value;
  }

  private String nameAsGiven = JC.ZAP_TITLE, fullPathName, lastFullPathName, lastNameAsGiven = JC.ZAP_TITLE, fileName;

  /**
   * Set fullPathName, fileName, and nameAsGiven
   * 
   * @param fileInfo if null, replace fullPathName and nameAsGiven with last version of such
   * 
   * 
   */
  public void setFileInfo(String[] fileInfo) {
    if (fileInfo == null) {
      fullPathName = lastFullPathName;
      nameAsGiven = lastNameAsGiven;
      return;
    }
    // used by ScriptEvaluator dataFrame and load methods to temporarily save the state here
    fullPathName = fileInfo[0];
    fileName = fileInfo[Math.min(1,  fileInfo.length - 1)];
    nameAsGiven = fileInfo[Math.min(2, fileInfo.length - 1)];
    if (!nameAsGiven.equals(JC.ZAP_TITLE)) {
      lastNameAsGiven = nameAsGiven;
      lastFullPathName = fullPathName;
    }
  }

  public String[] getFileInfo() {
    // used by ScriptEvaluator dataFrame method
    return new String[] { fullPathName, fileName, nameAsGiven };
  }

  public String getFullPathName(boolean orPrevious) {
    String f =(fullPathName != null ? fullPathName : nameAsGiven);
    return (!orPrevious || !f.equals(JC.ZAP_TITLE) ? f : lastFullPathName != null ? lastFullPathName : lastNameAsGiven);
  }

  public String getFileName() {
    return fileName != null ? fileName : nameAsGiven;
  }

  // for applet proxy
  private URL appletDocumentBaseURL = null;
  private String appletProxy;

  String getAppletDocumentBase() {
    return (appletDocumentBaseURL == null ? "" : appletDocumentBaseURL.toString());
  }

  void setAppletContext(String documentBase) {
    try {
      System.out.println("setting document base to \"" + documentBase + "\"");      
      appletDocumentBaseURL = (documentBase.length() == 0 ? null : new URL((URL) null, documentBase, null));
    } catch (MalformedURLException e) {
      System.out.println("error setting document base to " + documentBase);
    }
  }

  void setAppletProxy(String appletProxy) {
    this.appletProxy = (appletProxy == null || appletProxy.length() == 0 ? null
        : appletProxy);
  }


  /////////////// createAtomSetCollectionFromXXX methods /////////////////

  // where XXX = File, Files, String, Strings, ArrayData, DOM, Reader

  /*
   * note -- createAtomSetCollectionFromXXX methods
   * were "openXXX" before refactoring 11/29/2008 -- BH
   * 
   * The problem was that while they did open the file, they
   * (mostly) also closed them, and this was confusing.
   * 
   * The term "clientFile" was replaced by "atomSetCollection"
   * here because that's what it is --- an AtomSetCollection,
   * not a file. The file is closed at this point. What is
   * returned is the atomSetCollection object.
   * 
   * One could say this is just semantics, but there were
   * subtle bugs here, where readers were not always being 
   * closed explicitly. In the process of identifying Out of
   * Memory Errors, I felt it was necessary to clarify all this.
   * 
   * Apologies to those who feel the original clientFile notation
   * was more generalizable or understandable. 
   * 
   */
  Object createAtomSetCollectionFromFile(String name,
                                         Map<String, Object> htParams,
                                         boolean isAppend) {
    if (htParams.get("atomDataOnly") == null)
      setLoadState(htParams);
    String name0 = name;
    name = vwr.resolveDatabaseFormat(name);
    if (!name0.equals(name) && name0.indexOf("/") < 0 
        && Viewer.hasDatabasePrefix(name0)) {
      htParams.put("dbName", name0);
    }
    if (name.endsWith("%2D%")) {
      String filter = (String) htParams.get("filter");
      htParams.put("filter", (filter == null ? "" : filter) + "2D");
      name = name.substring(0, name.length() - 4);
    }
      
    int pt = name.indexOf("::");
    String nameAsGiven = (pt >= 0 ? name.substring(pt + 2) : name);
    String fileType = (pt >= 0 ? name.substring(0, pt) : null);
    Logger.info("\nFileManager.getAtomSetCollectionFromFile(" + nameAsGiven
        + ")" + (name.equals(nameAsGiven) ? "" : " //" + name));
    String[] names = getClassifiedName(nameAsGiven, true);
    if (names.length == 1)
      return names[0];
    String fullPathName = names[0];
    String fileName = names[1];
    htParams.put("fullPathName", (fileType == null ? "" : fileType + "::")
        + FileManager.fixDOSName(fullPathName));
    if (vwr.getBoolean(T.messagestylechime) && vwr.getBoolean(T.debugscript))
      vwr.getChimeMessenger().update(fullPathName);
    FileReader fileReader = new FileReader(vwr, fileName, fullPathName, nameAsGiven,
        fileType, null, htParams, isAppend);
    fileReader.run();
    return fileReader.getAtomSetCollection();
  }

  Object createAtomSetCollectionFromFiles(String[] fileNames,
                                          Map<String, Object> htParams,
                                          boolean isAppend) {
    setLoadState(htParams);
    String[] fullPathNames = new String[fileNames.length];
    String[] namesAsGiven = new String[fileNames.length];
    String[] fileTypes = new String[fileNames.length];
    for (int i = 0; i < fileNames.length; i++) {
      int pt = fileNames[i].indexOf("::");
      String nameAsGiven = (pt >= 0 ? fileNames[i].substring(pt + 2)
          : fileNames[i]);
      
      System.out.println(i + " FM " + nameAsGiven);
      String fileType = (pt >= 0 ? fileNames[i].substring(0, pt) : null);
      String[] names = getClassifiedName(nameAsGiven, true);
      if (names.length == 1)
        return names[0];
      fullPathNames[i] = names[0];
      fileNames[i] = fixDOSName(names[0]);
      fileTypes[i] = fileType;
      namesAsGiven[i] = nameAsGiven;
    }
    htParams.put("fullPathNames", fullPathNames);
    htParams.put("fileTypes", fileTypes);
    JmolFilesReaderInterface filesReader = newFilesReader(fullPathNames, namesAsGiven,
        fileTypes, null, htParams, isAppend);
    filesReader.run();
    return filesReader.getAtomSetCollection();
  }

  Object createAtomSetCollectionFromString(String strModel,
                                           Map<String, Object> htParams,
                                           boolean isAppend) {
    setLoadState(htParams);
    boolean isAddH = (strModel.indexOf(JC.ADD_HYDROGEN_TITLE) >= 0);
    String[] fnames = (isAddH ? getFileInfo() : null);
    FileReader fileReader = new FileReader(vwr, "string", null, null, null,
        Rdr.getBR(strModel), htParams, isAppend);
    fileReader.run();
    if (fnames != null)
      setFileInfo(fnames);
    if (!isAppend && !(fileReader.getAtomSetCollection() instanceof String)) {
// zap is unnecessary  - it was done already in FileReader, and it 
// inappropriately clears the PDB chain name map
//      vwr.zap(false, true, false);
      setFileInfo(new String[] { strModel == JC.MODELKIT_ZAP_STRING ? JC.MODELKIT_ZAP_TITLE
          : "string"});
    }
    return fileReader.getAtomSetCollection();
  }

  Object createAtomSeCollectionFromStrings(String[] arrayModels,
                                           SB loadScript,
                                           Map<String, Object> htParams,
                                           boolean isAppend) {
    if (!htParams.containsKey("isData")) {
      String oldSep = "\"" + vwr.getDataSeparator() + "\"";
      String tag = "\"" + (isAppend ? "append" : "model") + " inline\"";
      SB sb = new SB();
      sb.append("set dataSeparator \"~~~next file~~~\";\ndata ").append(tag);
      for (int i = 0; i < arrayModels.length; i++) {
        if (i > 0)
          sb.append("~~~next file~~~");
        sb.append(arrayModels[i]);
      }
      sb.append("end ").append(tag).append(";set dataSeparator ")
          .append(oldSep);
      loadScript.appendSB(sb);
    }
    setLoadState(htParams);
    Logger.info("FileManager.getAtomSetCollectionFromStrings(string[])");
    String[] fullPathNames = new String[arrayModels.length];
    DataReader[] readers = new DataReader[arrayModels.length];
    for (int i = 0; i < arrayModels.length; i++) {
      fullPathNames[i] = "string[" + i + "]";
      readers[i] = newDataReader(vwr, arrayModels[i]);
    }
    JmolFilesReaderInterface filesReader = newFilesReader(fullPathNames, fullPathNames,
        null, readers, htParams, isAppend);
    filesReader.run();
    return filesReader.getAtomSetCollection();
  }

  Object createAtomSeCollectionFromArrayData(Lst<Object> arrayData,
                                             Map<String, Object> htParams,
                                             boolean isAppend) {
    // NO STATE SCRIPT -- HERE WE ARE TRYING TO CONSERVE SPACE
    Logger.info("FileManager.getAtomSetCollectionFromArrayData(Vector)");
    int nModels = arrayData.size();
    String[] fullPathNames = new String[nModels];
    DataReader[] readers = new DataReader[nModels];
    for (int i = 0; i < nModels; i++) {
      fullPathNames[i] = "String[" + i + "]";
      readers[i] = newDataReader(vwr, arrayData.get(i));
    }
    JmolFilesReaderInterface filesReader = newFilesReader(fullPathNames, fullPathNames,
        null, readers, htParams, isAppend);
    filesReader.run();
    return filesReader.getAtomSetCollection();
  }

  static DataReader newDataReader(Viewer vwr, Object data) {
    String reader = (data instanceof String ? "String"
        : AU.isAS(data) ? "Array" 
        : data instanceof Lst<?> ? "List" : null);
    if (reader == null)
      return null;
    DataReader dr = (DataReader) Interface.getInterface("javajs.util." + reader + "DataReader", vwr, "file");
    return dr.setData(data);
  }

  private JmolFilesReaderInterface newFilesReader(String[] fullPathNames,
                                                  String[] namesAsGiven,
                                                  String[] fileTypes,
                                                  DataReader[] readers,
                                                  Map<String, Object> htParams,
                                                  boolean isAppend) {
    JmolFilesReaderInterface fr = (JmolFilesReaderInterface) Interface
        .getOption("io.FilesReader", vwr, "file");
    fr.set(this, vwr, fullPathNames, namesAsGiven, fileTypes, readers, htParams,
        isAppend);
    return fr;
  }

  Object createAtomSetCollectionFromDOM(Object DOMNode,
                                        Map<String, Object> htParams) {
    JmolDomReaderInterface aDOMReader = (JmolDomReaderInterface) Interface.getOption("io.DOMReadaer", vwr, "file");
    aDOMReader.set(this, vwr, DOMNode, htParams);
    aDOMReader.run();
    return aDOMReader.getAtomSetCollection();
  }

  /**
   * not used in Jmol project -- will close reader
   * 
   * @param fullPathName
   * @param name
   * @param reader could be a Reader, or a BufferedInputStream or byte[]
   * @param htParams 
   * @return fileData
   */
  Object createAtomSetCollectionFromReader(String fullPathName, String name,
                                           Object reader,
                                           Map<String, Object> htParams) {
    FileReader fileReader = new FileReader(vwr, name, fullPathName, null, null,
        reader, htParams, false);
    fileReader.run();
    return fileReader.getAtomSetCollection();
  }

  /////////////// generally useful file I/O methods /////////////////

  // mostly internal to FileManager and its enclosed classes

  BufferedInputStream getBufferedInputStream(String fullPathName) {
    Object ret = getBufferedReaderOrErrorMessageFromName(fullPathName,
        new String[2], true, true);
    return (ret instanceof BufferedInputStream ? (BufferedInputStream) ret
        : null);
  }

  public Object getBufferedInputStreamOrErrorMessageFromName(String name,
                                                             String fullName,
                                                             boolean showMsg,
                                                             boolean checkOnly,
                                                             byte[] outputBytes,
                                                             boolean allowReader,
                                                             boolean allowCached) {
    BufferedInputStream bis = null;
    Object ret = null;
    String errorMessage = null;
    byte[] cacheBytes = (allowCached && outputBytes == null ? cacheBytes = getPngjOrDroppedBytes(
        fullName, name) : null);
    try {
      if (allowCached && name.indexOf(".png") >= 0 && pngjCache == null
          && !vwr.getBoolean(T.testflag1))
        pngjCache = new Hashtable<String, Object>();
      if (cacheBytes == null) {
        boolean isPngjBinaryPost = (name.indexOf("?POST?_PNGJBIN_") >= 0);
        boolean isPngjPost = (isPngjBinaryPost || name.indexOf("?POST?_PNGJ_") >= 0);
        if (name.indexOf("?POST?_PNG_") > 0 || isPngjPost) {
          String[] errMsg = new String[1];
          byte[] bytes = vwr.getImageAsBytes(isPngjPost ? "PNGJ" : "PNG", 0, 0,
              -1, errMsg);
          if (errMsg[0] != null)
            return errMsg[0];
          if (isPngjBinaryPost) {
            outputBytes = bytes;
            name = PT.rep(name, "?_", "=_");
          } else {
            name = new SB().append(name).append("=")
                .appendSB(Base64.getBase64(bytes)).toString();
          }
        }
        int iurl = OC.urlTypeIndex(name);
        boolean isURL = (iurl >= 0);
        String post = null;
        if (isURL && (iurl = name.indexOf("?POST?")) >= 0) {
          post = name.substring(iurl + 6);
          name = name.substring(0, iurl);
        }
        boolean isApplet = (appletDocumentBaseURL != null);
        if (isApplet || isURL) {
          if (isApplet && isURL && appletProxy != null)
            name = appletProxy + "?url=" + urlEncode(name);
          URL url = (isApplet ? new URL(appletDocumentBaseURL, name, null)
              : new URL((URL) null, name, null));
          if (checkOnly)
            return null;
          name = url.toString();
          if (showMsg && name.toLowerCase().indexOf("password") < 0)
            Logger.info("FileManager opening url " + name);
          // note that in the case of JS, this is a javajs.util.SB.
          ret = vwr.apiPlatform.getURLContents(url, outputBytes, post, false);
          //          if ((ret instanceof SB && ((SB) ret).length() < 3
          //                || ret instanceof String && ((String) ret).startsWith("java."))
          //              && name.startsWith("http://ves-hx-89.ebi.ac.uk")) {
          //            // temporary bypass for EBI firewalled development server
          //            // defaulting to current directory and JSON file
          //            name = "http://chemapps.stolaf.edu/jmol/jsmol/data/" 
          //            + name.substring(name.lastIndexOf("/") + 1) 
          //            + (name.indexOf("/val") >= 0 ? ".val" : ".ann") + ".json";
          //            ret = getBufferedInputStreamOrErrorMessageFromName(name, fullName,
          //                showMsg, checkOnly, outputBytes, allowReader, allowCached);
          //          }

          byte[] bytes = null;
          if (ret instanceof SB) {
            SB sb = (SB) ret;
            if (allowReader && !Rdr.isBase64(sb))
              return Rdr.getBR(sb.toString());
            bytes = Rdr.getBytesFromSB(sb);
          } else if (AU.isAB(ret)) {
            bytes = (byte[]) ret;
          }
          if (bytes != null)
            ret = Rdr.getBIS(bytes);
        } else if (!allowCached
            || (cacheBytes = (byte[]) cacheGet(name, true)) == null) {
          if (showMsg)
            Logger.info("FileManager opening file " + name);
          ret = vwr.apiPlatform.getBufferedFileInputStream(name);
        }
        if (ret instanceof String)
          return ret;
      }
      bis = (cacheBytes == null ? (BufferedInputStream) ret : Rdr
          .getBIS(cacheBytes));
      if (checkOnly) {
        bis.close();
        bis = null;
      }
      return bis;
    } catch (Exception e) {
      try {
        if (bis != null)
          bis.close();
      } catch (IOException e1) {
      }
      errorMessage = "" + e;
    }
    return errorMessage;
  }

  @SuppressWarnings("null")
  public static BufferedReader getBufferedReaderForResource(Viewer vwr,
                                                            Object resourceClass,
                                                            String classPath,
                                                            String resourceName)
      throws IOException {

    URL url;
    /**
     * @j2sNative
     * 
     */
    {
      url = resourceClass.getClass().getResource(resourceName);
      if (url == null) {
        System.err.println("Couldn't find file: " + classPath + resourceName);
        throw new IOException();
      }
      if (!vwr.async)
        return Rdr.getBufferedReader(
            new BufferedInputStream((InputStream) url.getContent()), null);
    }
    resourceName = (url == null 
        ? vwr.vwrOptions.get("codePath") + classPath + resourceName
            : url.getFile());
    if (vwr.async) {
      // if we are running asynchronously, this will be a problem. 
      Object bytes = vwr.fm.cacheGet(resourceName, false);
      if (bytes == null)
        throw new JmolAsyncException(resourceName);
      return Rdr.getBufferedReader(Rdr.getBIS((byte[]) bytes), null);
    }
    // JavaScript only; here and not in JavaDoc to preserve Eclipse search reference
    return (BufferedReader) vwr.fm.getBufferedReaderOrErrorMessageFromName(
        resourceName, new String[] { null, null }, false, true);
  }

  private String urlEncode(String name) {
    try {
      return URLEncoder.encode(name, "utf-8");
    } catch (UnsupportedEncodingException e) {
      return name;
    }
  }

  public String getEmbeddedFileState(String fileName, boolean allowCached, String sptName) {
    String[] dir = getZipDirectory(fileName, false, allowCached);
    if (dir.length == 0) {
      String state = vwr.getFileAsString4(fileName, -1, false, true, false, "file");
      return (state.indexOf(JC.EMBEDDED_SCRIPT_TAG) < 0 ? ""
          : FileManager.getEmbeddedScript(state));
    }
    for (int i = 0; i < dir.length; i++)
      if (dir[i].indexOf(sptName) >= 0) {
        String[] data = new String[] { fileName + "|" + dir[i], null };
        getFileDataAsString(data, -1, false, false, false);
        return data[1];
      }
    return "";
  }

  /**
   * just check for a file as being readable. Do not go into a zip file
   * 
   * @param filename
   * @param getStream 
   * @param ret 
   * @return String[2] where [0] is fullpathname and [1] is error message or null
   */
  Object getFullPathNameOrError(String filename, boolean getStream, String[] ret) {
    String[] names = getClassifiedName(JC.fixProtocol(filename), true);
    if (names == null || names[0] == null || names.length < 2)
      return new String[] { null, "cannot read file name: " + filename };
    String name = names[0];
    String fullPath = fixDOSName(names[0]);
    name = Rdr.getZipRoot(name);
    Object errMsg = getBufferedInputStreamOrErrorMessageFromName(name, fullPath, false, !getStream, null, false, !getStream);
    ret[0] = fullPath;
    if (errMsg instanceof String)
      ret[1] = (String) errMsg;
    return errMsg;
  }

  public Object getBufferedReaderOrErrorMessageFromName(String name,
                                                 String[] fullPathNameReturn,
                                                 boolean isBinary,
                                                 boolean doSpecialLoad) {
    name = JC.fixProtocol(name);
    Object data = cacheGet(name, false);
    boolean isBytes = AU.isAB(data);
    byte[] bytes = (isBytes ? (byte[]) data : null);
    if (name.startsWith("cache://")) {
      if (data == null)
        return "cannot read " + name;
      if (isBytes) {
        bytes = (byte[]) data;
      } else {
        return Rdr.getBR((String) data);
      }
    }
    String[] names = getClassifiedName(name, true);
    if (names == null)
      return "cannot read file name: " + name;
    if (fullPathNameReturn != null)
      fullPathNameReturn[0] = fixDOSName(names[0]);
    return getUnzippedReaderOrStreamFromName(names[0], bytes,
        false, isBinary, false, doSpecialLoad, null);
  }

  /**
   * 
   * @param name
   * @param bytesOrStream
   *        cached bytes or a BufferedInputStream
   * @param allowZipStream
   *        if the file is a zip file, allow a return that is a ZipInputStream
   * @param forceInputStream
   *        always return a raw BufferedInputStream, not a BufferedReader, and
   *        do not process PNGJ files
   * @param isTypeCheckOnly
   *        when possibly reading a spartan file for content (doSpecialLoad ==
   *        true), just return the compound document's file list
   * @param doSpecialLoad
   *        check for a Spartan file
   * @param htParams
   * @return String if error or String[] if a type check or BufferedReader or
   *         BufferedInputStream
   */
  @SuppressWarnings("resource")
  public Object getUnzippedReaderOrStreamFromName(String name,
                                                  Object bytesOrStream,
                                                  boolean allowZipStream,
                                                  boolean forceInputStream,
                                                  boolean isTypeCheckOnly,
                                                  boolean doSpecialLoad,
                                                  Map<String, Object> htParams) {
    if (doSpecialLoad && bytesOrStream == null) {
      Object o = (name.endsWith(".spt") ? new String[] { null, null, null } // DO NOT actually load any file
          : name.indexOf(".spardir") < 0 ? null 
          : spartanUtil().getFileList(name, isTypeCheckOnly));
      if (o != null)
        return o;
    }
    name = JC.fixProtocol(name);
    if (bytesOrStream == null
        && (bytesOrStream = getCachedPngjBytes(name)) != null
        && htParams != null)
      htParams.put("sourcePNGJ", Boolean.TRUE);
    name = name.replace("#_DOCACHE_", "");
    String fullName = name;
    String[] subFileList = null;
    if (name.indexOf("|") >= 0) {
      subFileList = PT.split(name.replace('\\', '/'), "|");
      if (bytesOrStream == null)
        Logger.info("FileManager opening zip " + name);
      name = subFileList[0];
    }
    Object t = (bytesOrStream == null ? getBufferedInputStreamOrErrorMessageFromName(
        name, fullName, true, false, null, !forceInputStream, true) : AU
        .isAB(bytesOrStream) ? Rdr.getBIS((byte[]) bytesOrStream)
        : (BufferedInputStream) bytesOrStream);
    try {
      if (t instanceof String || t instanceof BufferedReader)
        return t;
      BufferedInputStream bis = (BufferedInputStream) t;
      if (Rdr.isGzipS(bis))
        bis = Rdr.getUnzippedInputStream(vwr.getJzt(), bis);
      // if we have a subFileList, we don't want to return the stream for the zip file itself
      else if (Rdr.isBZip2S(bis))
        bis = Rdr.getUnzippedInputStreamBZip2(vwr.getJzt(), bis);
      // if we have a subFileList, we don't want to return the stream for the zip file itself
      if (forceInputStream && subFileList == null)
        return bis;
      if (Rdr.isCompoundDocumentS(bis)) {
        // very specialized reader; assuming we have a Spartan document here
        CompoundDocument doc = (CompoundDocument) Interface.getInterface(
            "javajs.util.CompoundDocument", vwr, "file");
        doc.setDocStream(vwr.getJzt(), bis);
        String s = doc.getAllDataFiles("Molecule", "Input").toString();
        return (forceInputStream ? Rdr.getBIS(s.getBytes()) : Rdr.getBR(s));
      }
      // check for PyMOL or MMTF
      if (Rdr.isMessagePackS(bis) || Rdr.isPickleS(bis))
        return bis;
      bis = Rdr.getPngZipStream(bis, true);
      if (Rdr.isZipS(bis)) {
        if (allowZipStream)
          return vwr.getJzt().newZipInputStream(bis);
        Object o = vwr.getJzt().getZipFileDirectory(bis, subFileList, 1,
            forceInputStream);
        return (o instanceof String ? Rdr.getBR((String) o) : o);
      }
      return (forceInputStream ? bis : Rdr.getBufferedReader(bis, null));
    } catch (Exception ioe) {
      return ioe.toString();
    }
  }


  /**
   * 
   * @param fileName
   * @param addManifest
   * @param allowCached 
   * @return [] if not a zip file;
   */
  public String[] getZipDirectory(String fileName, boolean addManifest, boolean allowCached) {
    Object t = getBufferedInputStreamOrErrorMessageFromName(fileName, fileName,
        false, false, null, false, allowCached);
    return vwr.getJzt().getZipDirectoryAndClose((BufferedInputStream) t, addManifest ? "JmolManifest" : null);
  }

  public Object getFileAsBytes(String name, OC out) {
    // ?? used by eval of "WRITE FILE"
    // will be full path name
    if (name == null)
      return null;
    String fullName = name;
    String[] subFileList = null;
    if (name.indexOf("|") >= 0) {
      subFileList = PT.split(name, "|");
      name = subFileList[0];
    }
    // JavaScript will use this method to load a cached file,
    // but in that case we can get the bytes directly and not
    // fool with a BufferedInputStream, and we certainly do not want to 
    // open it twice in the case of the returned interior file being another PNGJ file
    Object bytes = (subFileList == null ? null : getPngjOrDroppedBytes(
        fullName, name));
    if (bytes == null) {
      Object t = getBufferedInputStreamOrErrorMessageFromName(name, fullName,
          false, false, null, false, true);
      if (t instanceof String)
        return "Error:" + t;
      try {
        BufferedInputStream bis = (BufferedInputStream) t;
        bytes = (out != null || subFileList == null || subFileList.length <= 1
            || !Rdr.isZipS(bis) && !Rdr.isPngZipStream(bis) ? Rdr
            .getStreamAsBytes(bis, out) : vwr.getJzt()
            .getZipFileContentsAsBytes(bis, subFileList, 1));
        bis.close();
      } catch (Exception ioe) {
        return ioe.toString();
      }
    }
    if (out == null || !AU.isAB(bytes))
      return bytes;
      out.write((byte[]) bytes, 0, -1);
    return ((byte[]) bytes).length + " bytes";
  }

  public Map<String, Object> getFileAsMap(String name, String type) {
    Map<String, Object> bdata = new Hashtable<String, Object>();
    Object t;
    if (name == null) {
      // return the current state as a PNGJ or ZIPDATA
      String[] errMsg = new String[1];
      byte[] bytes = vwr.getImageAsBytes(type, -1, -1, -1, errMsg);
      if (errMsg[0] != null) {
        bdata.put("_ERROR_", errMsg[0]);
        return bdata;
      }
      t = Rdr.getBIS(bytes);
    } else {
      String[] data = new String[2];
      t = getFullPathNameOrError(name, true, data);
      if (t instanceof String) {
        bdata.put("_ERROR_", t);
        return bdata;
      }
      if (!checkSecurity(data[0])) {
        bdata.put("_ERROR_", "java.io. Security exception: cannot read file "
            + data[0]);
        return bdata;
      }
    }
    try {
      vwr.getJzt().readFileAsMap((BufferedInputStream) t, bdata, name);
      
    } catch (Exception e) {
      bdata.clear();
      bdata.put("_ERROR_", "" + e);
    }
    return bdata;
  }

  /**
   * 
   * @param data
   *        [0] initially path name, but returned as full path name; [1]file
   *        contents (directory listing for a ZIP/JAR file) or error string
   * @param nBytesMax
   *        or -1
   * @param doSpecialLoad
   * @param allowBinary
   * @param checkProtected
   *        TODO
   * @return true if successful; false on error
   */

  public boolean getFileDataAsString(String[] data, int nBytesMax,
                                     boolean doSpecialLoad,
                                     boolean allowBinary, boolean checkProtected) {
    data[1] = "";
    String name = data[0];
    if (name == null)
      return false;
    Object t = getBufferedReaderOrErrorMessageFromName(name, data, false,
        doSpecialLoad);
    if (t instanceof String) {
      data[1] = (String) t;
      return false;
    }
    if (checkProtected && !checkSecurity(data[0])) {
      data[1] = "java.io. Security exception: cannot read file " + data[0];
      return false;
    }
    try {
      return Rdr.readAllAsString((BufferedReader) t, nBytesMax, allowBinary,
          data, 1);
    } catch (Exception e) {
      return false;
    }
  }

  private boolean checkSecurity(String f) {
    // when load() function and local file: 
    if (!f.startsWith("file:"))
       return true;
    int pt = f.lastIndexOf('/');
    // root directory C:/foo or file:///c:/foo or "/foo"
    // no hidden files 
    // no files without extension
    if (f.lastIndexOf(":/") == pt - 1 
      || f.indexOf("/.") >= 0 
      || f.lastIndexOf('.') < f.lastIndexOf('/'))
      return false;
    return true;
  }

  /**
   * Load an image
   * @param nameOrBytes
   * @param echoName
   * @param forceSync TODO
   * @return true if asynchronous
   */
  @SuppressWarnings("unchecked")
  public boolean loadImage(Object nameOrBytes, String echoName, boolean forceSync) {
    Object image = null;
    String nameOrError = null;
    byte[] bytes = null;
    boolean isPopupImage = (echoName != null && echoName.startsWith("\1"));
    if (isPopupImage) {
      if (echoName.equals("\1closeall\1null"))
        return vwr.loadImageData(Boolean.TRUE, "\1closeall", "\1closeall", null);
      if ("\1close".equals(nameOrBytes))
        return vwr.loadImageData(Boolean.FALSE, "\1close", echoName, null);
    }
    if (nameOrBytes instanceof Map) {
      nameOrBytes = (((Map<String, Object>) nameOrBytes).containsKey("_DATA_") ? ((Map<String, Object>) nameOrBytes)
          .get("_DATA_") : ((Map<String, Object>) nameOrBytes).get("_IMAGE_"));
    }
    if (nameOrBytes instanceof SV)
      nameOrBytes = ((SV) nameOrBytes).value;
    String name = (nameOrBytes instanceof String ? (String) nameOrBytes : null);
    boolean isAsynchronous = false;
    if (name != null && name.startsWith(";base64,")) {
      bytes = Base64.decodeBase64(name);
    } else if (nameOrBytes instanceof BArray) {
      bytes = ((BArray) nameOrBytes).data;
    } else if (echoName == null || nameOrBytes instanceof String) {
      String[] names = getClassifiedName((String) nameOrBytes, true);
      nameOrError = (names == null ? "cannot read file name: " + nameOrBytes
          : fixDOSName(names[0]));
      if (names != null)
        image = getJzu().getImage(vwr, nameOrError, echoName, forceSync);
      isAsynchronous = (image == null);        
    } else {
      image = nameOrBytes;
    }
    if (bytes != null) {
      image = getJzu().getImage(vwr, bytes, echoName, true);
      isAsynchronous = false;
    }
    if (image instanceof String) {
      nameOrError = (String) image;
      image = null;
    }
    if (!vwr.isJS && image != null && bytes != null)
      nameOrError = ";base64," + Base64.getBase64(bytes).toString();
    if (!vwr.isJS || isPopupImage && nameOrError == null
        || !isPopupImage && image != null)
      return vwr.loadImageData(image, nameOrError, echoName, null);
    return isAsynchronous;
    // JSmol will call that from awtjs2d.Platform.java asynchronously
  }

  /**
   * [0] and [2] may return same as [1] in the 
   * case of a local unsigned applet.
   * 
   * @param name
   * @param isFullLoad
   *        false only when just checking path
   * @return [0] full path name, [1] file name without path, [2] full URL
   */
  private String[] getClassifiedName(String name, boolean isFullLoad) {
    if (name == null)
      return new String[] { null };
    boolean doSetPathForAllFiles = (pathForAllFiles.length() > 0);
    if (name.startsWith("?") || name.startsWith("http://?")) {
      if (!vwr.isJS && (name = vwr.dialogAsk("Load", name, null)) == null)
        return new String[] { isFullLoad ? "#CANCELED#" : null };
      doSetPathForAllFiles = false;
    }
    GenericFileInterface file = null;
    URL url = null;
    String[] names = null;
    if (name.startsWith("cache://")) {
      names = new String[3];
      names[0] = names[2] = name;
      names[1] = stripPath(names[0]);
      return names;
    }
    name = vwr.resolveDatabaseFormat(name);
    if (name.indexOf(":") < 0 && name.indexOf("/") != 0)
      name = addDirectory(vwr.getDefaultDirectory(), name);
    if (appletDocumentBaseURL == null) {
      // This code is for the app or signed local applet 
      // -- no local file reading for headless
      if (OC.urlTypeIndex(name) >= 0 || vwr.haveAccess(ACCESS.NONE)
          || vwr.haveAccess(ACCESS.READSPT) && !name.endsWith(".spt")
          && !name.endsWith("/")) {
        try {
          url = new URL((URL) null, name, null);
        } catch (MalformedURLException e) {
          return new String[] { isFullLoad ? e.toString() : null };
        }
      } else {
        file = vwr.apiPlatform.newFile(name);
        String s = file.getFullPath();
        // local unsigned applet may have access control issue here and get a null return
        String fname = file.getName();
        names = new String[] { (s == null ? fname : s), fname,
            (s == null ? fname : "file:/" + s.replace('\\', '/')) };
      }
    } else {
      // This code is only for the non-local applet
      try {
        if (name.indexOf(":\\") == 1 || name.indexOf(":/") == 1)
          name = "file:/" + name;
        //        else if (name.indexOf("/") == 0 && vwr.isSignedApplet())
        //        name = "file:" + name;
        url = new URL(appletDocumentBaseURL, name, null);
      } catch (MalformedURLException e) {
        return new String[] { isFullLoad ? e.toString() : null };
      }
    }
    if (url != null) {
      names = new String[3];
      names[0] = names[2] = url.toString();
      names[1] = stripPath(names[0]);
    }
    if (doSetPathForAllFiles) {
      String name0 = names[0];
      names[0] = pathForAllFiles + names[1];
      Logger.info("FileManager substituting " + name0 + " --> " + names[0]);
    }
    if (isFullLoad && (file != null || OC.urlTypeIndex(names[0]) == OC.URL_LOCAL)) {
      String path = (file == null ? PT.trim(names[0].substring(5), "/")
          : names[0]);
      int pt = path.length() - names[1].length() - 1;
      if (pt > 0) {
        path = path.substring(0, pt);
        setLocalPath(vwr, path, true);
      }
    }
    return names;
  }

  ///// DIRECTORY BUSINESS
  
  private static String addDirectory(String defaultDirectory, String name) {
    if (defaultDirectory.length() == 0)
      return name;
    char ch = (name.length() > 0 ? name.charAt(0) : ' ');
    String s = defaultDirectory.toLowerCase();
    if ((s.endsWith(".zip") || s.endsWith(".tar")) && ch != '|' && ch != '/')
      defaultDirectory += "|";
    return defaultDirectory
        + (ch == '/'
            || ch == '/'
            || (ch = defaultDirectory.charAt(defaultDirectory.length() - 1)) == '|'
            || ch == '/' ? "" : "/") + name;
  }

  String getDefaultDirectory(String name) {
    String[] names = getClassifiedName(name, true);
    if (names == null)
      return "";
    name = fixPath(names[0]);
    return (name == null ? "" : name.substring(0, name.lastIndexOf("/")));
  }

  private static String fixPath(String path) {
    path = fixDOSName(path);
    path = PT.rep(path, "/./", "/");
    int pt = path.lastIndexOf("//") + 1;
    if (pt < 1)
      pt = path.indexOf(":/") + 1;
    if (pt < 1)
      pt = path.indexOf("/");
    if (pt < 0)
      return null;
    String protocol = path.substring(0, pt);
    path = path.substring(pt);

    while ((pt = path.lastIndexOf("/../")) >= 0) {
      int pt0 = path.substring(0, pt).lastIndexOf("/");
      if (pt0 < 0)
        return PT.rep(protocol + path, "/../", "/");
      path = path.substring(0, pt0) + path.substring(pt + 3);
    }
    if (path.length() == 0)
      path = "/";
    return protocol + path;
  }

  public String getFilePath(String name, boolean addUrlPrefix,
                            boolean asShortName) {
    String[] names = getClassifiedName(name, false);
    return (names == null || names.length == 1 ? "" : asShortName ? names[1]
        : addUrlPrefix ? names[2] 
        : names[0] == null ? ""
        : fixDOSName(names[0]));
  }

  public static GenericFileInterface getLocalDirectory(Viewer vwr, boolean forDialog) {
    String localDir = (String) vwr
        .getP(forDialog ? "currentLocalPath" : "defaultDirectoryLocal");
    if (forDialog && localDir.length() == 0)
      localDir = (String) vwr.getP("defaultDirectoryLocal");
    if (localDir.length() == 0)
      return (vwr.isApplet ? null : vwr.apiPlatform.newFile(System
          .getProperty("user.dir", ".")));
    if (vwr.isApplet && localDir.indexOf("file:/") == 0)
      localDir = localDir.substring(6);
    GenericFileInterface f = vwr.apiPlatform.newFile(localDir);
    try {
      return f.isDirectory() ? f : f.getParentAsFile();
    } catch (Exception e) {
      return  null;
    }
  }

  /**
   * called by getImageFileNameFromDialog 
   * called by getOpenFileNameFromDialog
   * called by getSaveFileNameFromDialog
   * 
   * called by classifyName for any full file load
   * called from the CD command
   * 
   * currentLocalPath is set in all cases
   *   and is used specifically for dialogs as a first try
   * defaultDirectoryLocal is set only when not from a dialog
   *   and is used only in getLocalPathForWritingFile or
   *   from an open/save dialog.
   * In this way, saving a file from a dialog doesn't change
   *   the "CD" directory. 
   * Neither of these is saved in the state, but 
   * 
   * 
   * @param vwr
   * @param path
   * @param forDialog
   */
  public static void setLocalPath(Viewer vwr, String path,
                                  boolean forDialog) {
    while (path.endsWith("/") || path.endsWith("\\"))
      path = path.substring(0, path.length() - 1);
    vwr.setStringProperty("currentLocalPath", path);
    if (!forDialog)
      vwr.setStringProperty("defaultDirectoryLocal", path);
  }

  public static String getLocalPathForWritingFile(Viewer vwr, String file) {
    if (file.startsWith("http://"))
      return file;
    file = PT.rep(file, "?", "");
    if (file.indexOf("file:/") == 0)
      return file.substring(6);
    if (file.indexOf("/") == 0 || file.indexOf(":") >= 0)
      return file;
    GenericFileInterface dir = null;
    try {
      dir = getLocalDirectory(vwr, false);
    } catch (Exception e) {
      // access control for unsigned applet
    }
    return (dir == null ? file : fixPath(dir.toString() + "/" + file));
  }

  /**
   * Switch \ for / only for DOS names such as C:\temp\t.xyz, not names like http://cactus.nci.nih.gov/chemical/structure/CC/C=C\CC
   * @param fileName
   * @return fixed name
   */
  public static String fixDOSName(String fileName) {
    return (fileName.indexOf(":\\") >= 0 ? fileName.replace('\\', '/') : fileName);
  }
   
  public static String stripPath(String name) {
    int pt = Math.max(name.lastIndexOf("|"), name.lastIndexOf("/"));
    return name.substring(pt + 1);
  }

  ///// FILE TYPING /////
  
  private final static String DELPHI_BINARY_MAGIC_NUMBER = "\24\0\0\0"; //0x14 0 0 0 == "20-byte character string follows"
  public final static String PMESH_BINARY_MAGIC_NUMBER = "PM\1\0";
  public static final String JPEG_CONTINUE_STRING = " #Jmol...\0";
  

  
  public static String determineSurfaceTypeIs(InputStream is) {
    // drag-drop only
    BufferedReader br;
    try {
      br = Rdr.getBufferedReader(new BufferedInputStream(is), "ISO-8859-1");
    } catch (IOException e) {
      return null;
    }
    return determineSurfaceFileType(br);
  }
  
  public static boolean isScriptType(String fname) {
    return PT.isOneOf(fname.toLowerCase().substring(fname.lastIndexOf(".")+1), ";pse;spt;png;pngj;jmol;zip;");
  }

  public static boolean isSurfaceType(String fname) {
    return PT.isOneOf(fname.toLowerCase().substring(fname.lastIndexOf(".")+1), ";jvxl;kin;o;msms;map;pmesh;mrc;efvet;cube;obj;dssr;bcif;");
  }
  
  public static String determineSurfaceFileType(BufferedReader bufferedReader) {
    // drag-drop and isosurface command only
    // JVXL should be on the FIRST line of the file, but it may be 
    // after comments or missing.

    // Apbs, Jvxl, or Cube, also efvet and DHBD

    String line = null;

    if (bufferedReader instanceof Rdr.StreamReader) {
      BufferedInputStream is = ((Rdr.StreamReader) bufferedReader).getStream();
      if (is.markSupported()) {
        try {
          is.mark(300);
          byte[] buf = new byte[300];
          is.read(buf, 0, 300);
          is.reset();
          if ((buf[0] & 0xFF) == 0x83) // Finite map(3)
            return "BCifDensity";
          if (buf[0] == 'P' && buf[1] == 'M' && buf[2] == 1 && buf[3] == 0)//          "PM\1\0"
            return "Pmesh";
          if (buf[208] == 'M' && buf[209] == 'A' && buf[210] == 'P')//          "MAP" at 208
            return "Mrc";
          if (buf[0] == '\24' && buf[1] == 0 && buf[2] == 0 && buf[3] == 0)
            return "DelPhi";
          if (buf[36] == 0 && buf[37] == 100) // header19 (short)100
            return "Dsn6";
        } catch (IOException e) {
          // ignore
        }
      }
    }

    LimitedLineReader br = null;

    try {
      br = new LimitedLineReader(bufferedReader, 16000);
      line = br.getHeader(0);
    } catch (Exception e) {
      //
    }
    if (br == null || line == null || line.length() == 0)
      return null;

    // binary formats: problem here is that the buffered reader
    // may be translating byte sequences into unicode
    // and thus shifting the offset
    int pt0 = line.indexOf('\0');
    if (pt0 >= 0) {
      if (line.charAt(0) == 0x83) // Finite map(3)
        return "BCifDensity";
      if (line.indexOf(FileManager.PMESH_BINARY_MAGIC_NUMBER) == 0)
        return "Pmesh";
      if (line.indexOf("MAP ") == 208)
        return "Mrc";
      if (line.indexOf(DELPHI_BINARY_MAGIC_NUMBER) == 0)
        return "DelPhi";
      if (line.length() > 37
          && (line.charAt(36) == 0 && line.charAt(37) == 100 || line.charAt(36) == 0
              && line.charAt(37) == 100)) {
        // header19 (short)100
        return "Dsn6";
      }
    }

    //for (int i = 0; i < 220; i++)
    //  System.out.print(" " + i + ":" + (0 + line.charAt(i)));
    //System.out.println("");

    switch (line.charAt(0)) {
    case '@':
      if (line.indexOf("@text") == 0)
        return "Kinemage";
      break;
    case '#':
      if (line.indexOf(".obj") >= 0)
        return "Obj"; // #file: pymol.obj
      if (line.indexOf("MSMS") >= 0)
        return "Msms";
      break;
    case '&':
      if (line.indexOf("&plot") == 0)
        return "Jaguar";
      break;
    case '\r':
    case '\n':
      if (line.indexOf("ZYX") >= 0)
        return "Xplor";
      break;
    }
    if (line.indexOf("Here is your gzipped map") >= 0)
      return "UPPSALA" + line;
    if (line.startsWith("data_SERVER"))
      return "CifDensity";
    if (line.startsWith("4MESHC"))
      return "Pmesh4";
    if (line.indexOf("! nspins") >= 0)
      return "CastepDensity";
    if (line.indexOf("<jvxl") >= 0 && line.indexOf("<?xml") >= 0)
      return "JvxlXml";
    if (line.indexOf("#JVXL+") >= 0)
      return "Jvxl+";
    if (line.indexOf("#JVXL") >= 0)
      return "Jvxl";
    if (line.indexOf("#JmolPmesh") >= 0)
      return "Pmesh";
    if (line.indexOf("#obj") >= 0)
      return "Obj";
    if (line.indexOf("#pmesh") >= 0)
      return "Obj";
    if (line.indexOf("<efvet ") >= 0)
      return "Efvet";
    if (line.indexOf("usemtl") >= 0)
      return "Obj";
    if (line.indexOf("# object with") == 0)
      return "Nff";
    if (line.indexOf("BEGIN_DATAGRID_3D") >= 0
        || line.indexOf("BEGIN_BANDGRID_3D") >= 0)
      return "Xsf";
    if (line.indexOf("tiles in x, y") >= 0)
      return "Ras3D";
    if (line.indexOf(" 0.00000e+00 0.00000e+00      0      0\n") >= 0)
      return "Uhbd"; // older APBS http://sourceforge.net/p/apbs/code/ci/9527462a39126fb6cd880924b3cc4880ec4b78a9/tree/src/mg/vgrid.c

    // Apbs, Jvxl, Obj, or Cube, maybe formatted Plt

    line = br.readLineWithNewline();
    if (line.indexOf("object 1 class gridpositions counts") == 0)
      return "Apbs";

    String[] tokens = PT.getTokens(line);
    String line2 = br.readLineWithNewline();// second line
    if (tokens.length == 2 && PT.parseInt(tokens[0]) == 3
        && PT.parseInt(tokens[1]) != Integer.MIN_VALUE) {
      tokens = PT.getTokens(line2);
      if (tokens.length == 3 && PT.parseInt(tokens[0]) != Integer.MIN_VALUE
          && PT.parseInt(tokens[1]) != Integer.MIN_VALUE
          && PT.parseInt(tokens[2]) != Integer.MIN_VALUE)
        return "PltFormatted";
    }
    String line3 = br.readLineWithNewline(); // third line
    if (line.startsWith("v ") && line2.startsWith("v ")
        && line3.startsWith("v "))
      return "Obj";
    //next line should be the atom line
    int nAtoms = PT.parseInt(line3);
    if (nAtoms == Integer.MIN_VALUE)
      return (line3.indexOf("+") == 0 ? "Jvxl+" : null);
    tokens = PT.getTokens(line3);
    if (tokens[0].indexOf(".") > 0)
      return (line3.length() >= 60 || tokens.length != 3 ? null : "VaspChgcar"); // M40 files are > 60 char
    if (nAtoms >= 0)
      return (tokens.length == 4 || tokens.length == 5 && tokens[4].equals("1") ? "Cube"
          : null); //Can't be a Jvxl file; 
    nAtoms = -nAtoms;
    for (int i = 4 + nAtoms; --i >= 0;)
      if ((line = br.readLineWithNewline()) == null)
        return null;
    int nSurfaces = PT.parseInt(line);
    if (nSurfaces == Integer.MIN_VALUE)
      return null;
    return (nSurfaces < 0 ? "Jvxl" : "Cube"); //Final test looks at surface definition line    
  }

  ///// JMOL SCRIPT FILE PROCESSING
  
  /**
   * check a JmolManifest for a reference to a script file (.spt)
   * 
   * @param manifest
   * @return null, "", or a directory entry in the ZIP file
   */
  
  public static String getManifestScriptPath(String manifest) {
    if (manifest.indexOf("$SCRIPT_PATH$") >= 0)
      return "";
    String ch = (manifest.indexOf('\n') >= 0 ? "\n" : "\r");
    if (manifest.indexOf(".spt") >= 0) {
      String[] s = PT.split(manifest, ch);
      for (int i = s.length; --i >= 0;)
        if (s[i].indexOf(".spt") >= 0)
          return "|" + PT.trim(s[i], "\r\n \t");
    }
    return null;
  }

  public static String getEmbeddedScript(String script) {
    if (script == null)
      return script;
    int pt = script.indexOf(JC.EMBEDDED_SCRIPT_TAG);
    if (pt < 0)
      return script;
    int pt1 = script.lastIndexOf("/*", pt);
    int pt2 = script.indexOf((script.charAt(pt1 + 2) == '*' ? "*" : "") + "*/",
        pt);
    if (pt1 >= 0 && pt2 >= pt)
      script = script.substring(
          pt + JC.EMBEDDED_SCRIPT_TAG.length(), pt2)
          + "\n";
    while ((pt1 = script.indexOf(JPEG_CONTINUE_STRING)) >= 0)
      script = script.substring(0, pt1)
          + script.substring(pt1 + JPEG_CONTINUE_STRING.length() + 4);
    if (Logger.debugging)
      Logger.debug(script);
    return script;
  }

  public static void getFileReferences(String script, Lst<String> fileList) {
    for (int ipt = 0; ipt < scriptFilePrefixes.length; ipt++) {
      String tag = scriptFilePrefixes[ipt];
      int i = -1;
      while ((i = script.indexOf(tag, i + 1)) >= 0) {
        String s = PT.getQuotedStringAt(script, i);
        if (s.indexOf("::") >= 0)
          s = PT.split(s, "::")[1];
        fileList.addLast(s);
      }
    }
  }
  
  private static String[] scriptFilePrefixes = new String[] { "/*file*/\"",
    "FILE0=\"", "FILE1=\"" };

  public static String setScriptFileReferences(String script, String localPath,
                                               String remotePath,
                                               String scriptPath) {
    if (localPath != null)
      script = setScriptFileRefs(script, localPath, true);
    if (remotePath != null)
      script = setScriptFileRefs(script, remotePath, false);
    script = PT.rep(script, "\1\"", "\"");
    if (scriptPath != null) {
      while (scriptPath.endsWith("/"))
        scriptPath = scriptPath.substring(0, scriptPath.length() - 1);
      for (int ipt = 0; ipt < scriptFilePrefixes.length; ipt++) {
        String tag = scriptFilePrefixes[ipt];
        script = PT.rep(script, tag + ".", tag + scriptPath);
      }
    }
    return script;
  }

  /**
   * Sets all local file references in a script file to point to files within
   * dataPath. If a file reference contains dataPath, then the file reference is
   * left with that RELATIVE path. Otherwise, it is changed to a relative file
   * name within that dataPath. 
   * 
   * Only file references starting with "file://" are changed.
   * 
   * @param script
   * @param dataPath
   * @param isLocal 
   * @return revised script
   */
  private static String setScriptFileRefs(String script, String dataPath,
                                                boolean isLocal) {
    if (dataPath == null)
      return script;
    boolean noPath = (dataPath.length() == 0);
    Lst<String> fileNames = new  Lst<String>();
    FileManager.getFileReferences(script, fileNames);
    Lst<String> oldFileNames = new  Lst<String>();
    Lst<String> newFileNames = new  Lst<String>();
    int nFiles = fileNames.size();
    for (int iFile = 0; iFile < nFiles; iFile++) {
      String name0 = fileNames.get(iFile);
      String name = name0;
      if (isLocal == OC.isLocal(name)) {
        int pt = (noPath ? -1 : name.indexOf("/" + dataPath + "/"));
        if (pt >= 0) {
          name = name.substring(pt + 1);
        } else {
          pt = name.lastIndexOf("/");
          if (pt < 0 && !noPath)
            name = "/" + name;
          if (pt < 0 || noPath)
            pt++;
          name = dataPath + name.substring(pt);
        }
      }
      Logger.info("FileManager substituting " + name0 + " --> " + name);
      oldFileNames.addLast("\"" + name0 + "\"");
      newFileNames.addLast("\1\"" + name + "\"");
    }
    return PT.replaceStrings(script, oldFileNames, newFileNames);
  }

  //// CACHING ////
  
  private Map<String, Object> cache = new Hashtable<String, Object>();
  public Map<String, Object> pngjCache;
  public Map<String, byte[]> spardirCache;

  void cachePut(String key, Object data) {
    key = fixDOSName(key);
    if (Logger.debugging)
      Logger.debug("cachePut " + key);
    if (data == null || "".equals(data)) { // J2S error -- cannot implement Int32Array.equals 
      cache.remove(key);
      return;
    }
    cache.put(key, data);
    getCachedPngjBytes(key);
  }
  
  public Object cacheGet(String key, boolean bytesOnly) {
    key = fixDOSName(key);
    // in the case of JavaScript local file reader, 
    // this will be a cached file, and the filename will not be known.
    int pt = key.indexOf("|");
    if (pt >= 0 && !key.endsWith("##JmolSurfaceInfo##")) // check for PyMOL surface creation
      key = key.substring(0, pt);
    key = getFilePath(key, true, false);
    Object data = null;
    /**
     * @j2sNative
     * 
     * (data = Jmol.Cache.get(key)) || (data = this.cache.get(key));
     * 
     */
    {
    //if (Logger.debugging)
      //Logger.debug
       data = cache.get(key);
       if (data != null)
         Logger.info("cacheGet " + key);
    }    
    return (bytesOnly && (data instanceof String) ? null : data);
  }

  void cacheClear() {
    Logger.info("cache cleared");
    cache.clear();
    //String fileName = null;
    //fileName = fileName == null ? null : getCanonicalName(Rdr.getZipRoot(fileName));
    if (pngjCache == null)// || fileName != null && !pngjCache.containsKey(fileName))
      return;
    pngjCache = null;
    Logger.info("PNGJ cache cleared");
  }

  public int cacheFileByNameAdd(String fileName, boolean isAdd) {
    if (fileName == null || !isAdd && fileName.equalsIgnoreCase("")) {
      cacheClear();
      return -1;
    }
    Object data;
    if (isAdd) {
      fileName = JC.fixProtocol(vwr.resolveDatabaseFormat(fileName));
      data = getFileAsBytes(fileName, null);
      if (data instanceof String)
        return 0;
      cachePut(fileName, data);
    } else {
      if (fileName.endsWith("*"))
        return AU.removeMapKeys(cache, fileName.substring(0, fileName.length() - 1));
      data = cache.remove(fixDOSName(fileName));
    }
    return (data == null ? 0 : data instanceof String ? ((String) data).length()
        : ((byte[]) data).length);
  }

  public Map<String, Integer> cacheList() {
    Map<String, Integer> map = new Hashtable<String, Integer>();
    for (Map.Entry<String, Object> entry : cache.entrySet())
      map.put(entry.getKey(), Integer
          .valueOf(AU.isAB(entry.getValue()) ? ((byte[]) entry
              .getValue()).length : entry.getValue().toString().length()));
    return map;
  }

  public String getCanonicalName(String pathName) {
    String[] names = getClassifiedName(pathName, true);
    return (names == null ? pathName : names[2]);
  }

  public void recachePngjBytes(String fileName, byte[] bytes) {
    if (pngjCache == null || !pngjCache.containsKey(fileName))
      return;
    pngjCache.put(fileName, bytes);
    Logger.info("PNGJ recaching " + fileName + " (" + bytes.length + ")");
  }

  private byte[] getPngjOrDroppedBytes(String fullName, String name) {
    byte[] bytes = getCachedPngjBytes(fullName);
    return (bytes == null ? (byte[]) cacheGet(name, true) : bytes);
  }

  private byte[] getCachedPngjBytes(String pathName) {
    return (
        pathName == null || pngjCache == null || pathName.indexOf(".png") < 0 ? null 
            : getJzu().getCachedPngjBytes(this, pathName));
  }

  //// BytePoster interface 
  
  @Override
  public String postByteArray(String fileName, byte[] bytes) {
    // BytePoster interface - for javajs.util.OC (output channel)
    // in principle, could have sftp or ftp here
    // but sftp is not implemented
    if (fileName.startsWith("cache://")) {
      cachePut(fileName, bytes);
      return "OK " + bytes.length + "cached";
    }
    Object ret = getBufferedInputStreamOrErrorMessageFromName(fileName, null, false,
            false, bytes, false, true);
    if (ret instanceof String)
      return (String) ret;
    try {
      ret = Rdr.getStreamAsBytes((BufferedInputStream) ret, null);
    } catch (IOException e) {
      try {
        ((BufferedInputStream) ret).close();
      } catch (IOException e1) {
        // ignore
      }
    }
    return (ret == null ? "" : Rdr.fixUTF((byte[]) ret));
  }


}
