/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2018-03-22 13:29:36 -0500 (Thu, 22 Mar 2018) $
 * $Revision: 21872 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.adapter.smarter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.util.Map;
import java.util.StringTokenizer;

import javajs.api.GenericBinaryDocument;
import javajs.util.LimitedLineReader;
import javajs.util.PT;
import javajs.util.Rdr;

import org.jmol.api.Interface;
import org.jmol.util.Logger;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;


public class Resolver {

  private final static String classBase = "org.jmol.adapter.readers.";
  private final static String[] readerSets = new String[] {
    "cif.", ";Cif;Cif2;MMCif;MMTF;MagCif",
    "molxyz.", ";Mol3D;Mol;Xyz;",
    "more.", ";AFLOW;BinaryDcd;Gromacs;Jcampdx;MdCrd;MdTop;Mol2;TlsDataOnly;",
    "quantum.", ";Adf;Csf;Dgrid;GamessUK;GamessUS;Gaussian;GaussianFchk;GaussianWfn;Jaguar;" +
                 "Molden;MopacGraphf;GenNBO;NWChem;Psi;Qchem;QCJSON;" +
                 "WebMO;MO;", // MO is for XmlMolpro 
    "pdb.", ";Pdb;Pqr;P2n;JmolData;",
    "pymol.", ";PyMOL;",
    "simple.", ";Alchemy;Ampac;Cube;FoldingXyz;GhemicalMM;HyperChem;Jme;JSON;Mopac;MopacArchive;Tinker;Input;FAH;",
    "spartan.", ";Spartan;SpartanSmol;Odyssey;",
    "xtal.", ";Abinit;Aims;Bilbao;Castep;Cgd;Crystal;Dmol;Espresso;Gulp;Jana;Magres;Shelx;Siesta;VaspOutcar;" +
             "VaspPoscar;Wien2k;Xcrysden;PWmat;Optimade;",
    "xml.",  ";XmlChemDraw;XmlArgus;XmlCml;XmlChem3d;XmlMolpro;XmlOdyssey;XmlXsd;XmlVasp;XmlQE;",
  };
  

  // Tinker is only as explicit Tinker::fileName.xyz
  
  public final static String getReaderClassBase(String type) {
    String name = type + "Reader";
    if (type.startsWith("Xml"))
      return classBase + "xml." + name;
    String key = ";" + type + ";";
    for (int i = 1; i < readerSets.length; i += 2)
      if (readerSets[i].indexOf(key) >= 0)
        return classBase + readerSets[i - 1] + name;
    return classBase + "???." + name;
  }
  
  /**
   * From SmarterJmolAdapter.getFileTypeName(Object ascOrReader)
   * just return the file type with no exception issues
   * 
   * @param br
   * @return String file type
   */
  public static String getFileType(BufferedReader br) {
    try {
      return determineAtomSetCollectionReader(br, false);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * the main method for reading files. Called from SmarterJmolAdapter when
   * reading a file, reading a set of files, or reading a ZIP file
   * 
   * @param fullName
   * @param type
   * @param readerOrDocument
   * @param htParams
   * @param ptFile
   * @return an AtomSetCollection or a String error
   * @throws Exception
   */
  static Object getAtomCollectionReader(String fullName, String type,
                                        Object readerOrDocument,
                                        Map<String, Object> htParams, int ptFile)
      throws Exception {
    String readerName;
    fullName = FileManager.fixDOSName(fullName);
    String errMsg = null;
    if (type == null) {
      type = (String) htParams.get("filter");
      int pt = (type == null ? -1 : type.toLowerCase().indexOf("filetype"));
      type = (pt < 0 ? null : type.substring(pt + 8, (type+ ";").indexOf(";", pt)).replace('=', ' ').trim());
    }      
    if (type != null) {
      readerName = getReaderFromType(type);
      if (readerName == null)
        readerName = getReaderFromType("Xml" + type);
      if (readerName == null)
        errMsg = "unrecognized file format type " + type;
      else
        Logger.info("The Resolver assumes " + readerName);
    } else {
      readerName = determineAtomSetCollectionReader(readerOrDocument, true);
      if (readerName.charAt(0) == '\n') {
        type = (String) htParams.get("defaultType");
        if (type != null) {
          // allow for MDTOP to specify default MDCRD
          type = getReaderFromType(type);
          if (type != null)
            readerName = type;
        }
      }
      if (readerName.charAt(0) == '\n')
        errMsg = "unrecognized file format for file\n" + fullName + "\n"
            + split(readerName, 50);
      else if (readerName.equals("spt"))
        errMsg = JC.NOTE_SCRIPT_FILE + fullName + "\n";
      else if (!fullName.equals("ligand"))
        Logger.info("The Resolver thinks " + readerName);
    }
    if (errMsg != null) {
      SmarterJmolAdapter.close(readerOrDocument);
      return errMsg;
    }
    htParams.put("ptFile", Integer.valueOf(ptFile));
    if (ptFile <= 0)
      htParams.put("readerName", readerName);
    if (readerName.indexOf("Xml") == 0)
      readerName = "Xml";
    return getReader(readerName, htParams);
  }

  /**
   * Get a reader based on its name.
   * @param readerName
   * @param htParams
   * @return AtomSetCollectionReader or error message 
   */
  public static Object getReader(String readerName, Map<String, Object> htParams) {
    AtomSetCollectionReader rdr = null;
    String className = null;
    String err = null;
    className = getReaderClassBase(readerName);
    if ((rdr = (AtomSetCollectionReader) Interface.getInterface(className, (Viewer) htParams.get("vwr"), "reader")) == null) {
      err = JC.READER_NOT_FOUND  + className;
      Logger.error(err);
      return err;
    }
    return rdr;
  }

  private final static String getReaderFromType(String type) {
    type = ";" + type.toLowerCase() + ";";
    if (";zmatrix;cfi;c;vfi;v;mnd;jag;adf;gms;g;gau;mp;nw;orc;pqs;qc;".indexOf(type) >= 0)
      return "Input";

    String set;
    int pt;
    for (int i = readerSets.length; --i >= 0;)
      if ((pt = (set = readerSets[i--]).toLowerCase().indexOf(type)) >= 0)
        return set.substring(pt + 1, set.indexOf(";", pt + 2));
    return null;
  }
  
  private static String split(String a, int n) {
    String s = "";
    int l = a.length();
    for (int i = 0, j = 0; i < l; i = j)
      s += a.substring(i, (j = Math.min(i + n, l))) + "\n";
    return s;
  }

  /**
   * a largely untested reader of the DOM - where in a browser there is model
   * actually in XML format already present on the page. -- Egon Willighagen
   * 
   * @param htParams
   * @return an AtomSetCollection or a String error
   * @throws Exception
   */
  public static Object DOMResolve(Map<String, Object> htParams)
      throws Exception {
    String rdrName = getXmlType((String) htParams
        .get("nameSpaceInfo"));
    if (Logger.debugging) {
      Logger.debug("The Resolver thinks " + rdrName);
    }
    htParams.put("readerName", rdrName);
    return getReader("XmlReader", htParams);
  }

  private static final String CML_NAMESPACE_URI = "http://www.xml-cml.org/schema";

  /**
   * the main resolver method. One of the great advantages of Jmol is that it
   * can smartly determine a file type from its contents. In cases where this is
   * not possible, one can force a file type using a prefix to a filename. For
   * example:
   * 
   * load mol2::xxxx.whatever
   * 
   * This is only necessary for a few file types, where only numbers are
   * involved -- molecular dynamics coordinate files, for instance (mdcrd).
   * 
   * @param readerOrDocument
   * @param returnLines
   * @return readerName or a few lines, if requested, or null
   * @throws Exception
   */
  private static String determineAtomSetCollectionReader(Object readerOrDocument,
                                                         boolean returnLines)
      throws Exception {

    // We must do this in a very specific order. DON'T MESS WITH THIS!

    String readerName;

    if (readerOrDocument instanceof GenericBinaryDocument) {
      GenericBinaryDocument doc = (GenericBinaryDocument) readerOrDocument;
      readerName = getBinaryType(doc.getInputStream());
      return (readerName == null ? "binary file type not recognized" : readerName);
    }
    if (readerOrDocument instanceof InputStream) {
      readerName = getBinaryType((InputStream) readerOrDocument);
      if (readerName != null)
        return readerName;
      readerOrDocument = Rdr.getBufferedReader(new BufferedInputStream((InputStream) readerOrDocument), null);
    }

    LimitedLineReader llr = new LimitedLineReader(
        (BufferedReader) readerOrDocument, 16384);

    String leader = llr.getHeader(LEADER_CHAR_MAX).trim();

    // Test 1. check magic number for embedded Jmol script or PNGJ

    // PNG or BCD-encoded JPG or JPEG
    if (leader.indexOf("PNG") == 1 && leader.indexOf("PNGJ") >= 0)
      return "pngj"; // presume appended JMOL file
    if (leader.indexOf("PNG") == 1 || leader.indexOf("JPG") == 1
        || leader.indexOf("JFIF") == 6)
      return "spt"; // presume embedded script --- allows dragging into Jmol
    if (leader.indexOf("\"num_pairs\"") >= 0)
      return "dssr";
    if (leader.indexOf("output.31\n") >= 0)
      return "GenNBO|output.31";

    // Test 2. check starting 64 bytes of file

    if ((readerName = checkFileStart(leader)) != null) {
      return (readerName.equals("Xml") ? getXmlType(llr.getHeader(0))
          : readerName);
    }
   
    // now allow identification in first 16 lines
    // excluding those starting with "#"

    String[] lines = new String[16];
    int nLines = 0;
    for (int i = 0; i < lines.length; ++i) {
      lines[i] = llr.readLineWithNewline();
      if (lines[i].length() > 0)
        nLines++;
    }

    // Test 3. check special file formats (pass 1) 

    if ((readerName = checkSpecial1(nLines, lines, leader)) != null)
      return readerName;

    // Test 4. check line starts 

    if ((readerName = checkLineStarts(lines)) != null)
      return readerName;

    // Test 5. check content of initial 16K bytes of file 

    if ((readerName = checkHeaderContains(llr.getHeader(0))) != null)
      return readerName;

    // Test 6. check special file formats (pass 2) 

    if ((readerName = checkSpecial2(lines)) != null)
      return readerName;

    // Failed to identify file type

    return (returnLines ? "\n" + lines[0] + "\n" + lines[1] + "\n" + lines[2]
        + "\n" : null);
  }

  ////////////////////////////////////////////////////////////////
  // Test 2. check to see if first few bytes (trimmed) start with any of these strings
  ////////////////////////////////////////////////////////////////

  public static String getBinaryType(InputStream inputStream) {
    return (Rdr.isPickleS(inputStream) ? "PyMOL" : (Rdr.getMagic(inputStream, 1)[0] & 0xDE) == 0xDE ? "MMTF" : null);
  }

  private static String checkFileStart(String leader) {
    for (int i = 0; i < fileStartsWithRecords.length; ++i) {
      String[] recordTags = fileStartsWithRecords[i];
      for (int j = 1; j < recordTags.length; ++j) {
        String recordTag = recordTags[j];
        if (leader.startsWith(recordTag))
          return recordTags[0];
      }
    }
    return null;
  }

  private final static int LEADER_CHAR_MAX = 64;
  
  private final static String[] sptRecords = 
  { "spt", "# Jmol state", "# Jmol script", "JmolManifest" };
  
  private final static String[] m3dStartRecords = 
  { "Alchemy", "STRUCTURE  1.00     1" }; // M3D reader is very similar to Alchemy
  
  private final static String[] cubeFileStartRecords =
  {"Cube", "JVXL", "#JVXL"};

  private final static String[] mol2Records =
  {"Mol2", "mol2", "@<TRIPOS>"};

  private final static String[] webmoFileStartRecords =
  {"WebMO", "[HEADER]"};
  
  private final static String[] moldenFileStartRecords =
  {"Molden", "[Molden", "MOLDEN", "[MOLDEN"};

  private final static String[] dcdFileStartRecords =
  {"BinaryDcd", "T\0\0\0CORD", "\0\0\0TCORD"};

  private final static String[] tlsDataOnlyFileStartRecords =
  {"TlsDataOnly", "REFMAC\n\nTL", "REFMAC\r\n\r\n", "REFMAC\r\rTL"};
  
  private final static String[] inputFileStartRecords =
  {"Input", "#ZMATRIX", "%mem=", "AM1", "$rungauss"};
  
  private final static String[] magresFileStartRecords =
  {"Magres", "#$magres", "# magres"};

  private final static String[] pymolStartRecords =
  {"PyMOL", "}q" };

  private final static String[] janaStartRecords = 
  { "Jana", "Version Jana" };

  private final static String[] jsonStartRecords = 
  { "JSON", "{\"mol\":" };

  private final static String[] jcampdxStartRecords = 
  { "Jcampdx", "##TITLE" };
  
  private final static String[] jmoldataStartRecords = 
  { "JmolData", "REMARK   6 Jmol" };

  private final static String[] pqrStartRecords = 
  { "Pqr", "REMARK   1 PQR", "REMARK    The B-factors" };

  private final static String[] p2nStartRecords = 
  { "P2n", "REMARK   1 P2N" };
  
  private final static String[] cif2StartRecords = 
    { "Cif2", "#\\#CIF_2", "\u00EF\u00BB\u00BF#\\#CIF_2"};
    
  private final static String[] xmlStartRecords = 
  { "Xml", "<?xml" };

  private final static String[] cfiStartRecords = 
  { "Input", "$CFI" };

  private final static String[][] fileStartsWithRecords =
  { xmlStartRecords, sptRecords, m3dStartRecords, cubeFileStartRecords, 
    mol2Records, webmoFileStartRecords, 
    moldenFileStartRecords, dcdFileStartRecords, tlsDataOnlyFileStartRecords,
    inputFileStartRecords, magresFileStartRecords, pymolStartRecords, 
    janaStartRecords, jsonStartRecords, jcampdxStartRecords, 
    jmoldataStartRecords, pqrStartRecords, p2nStartRecords, cif2StartRecords, cfiStartRecords };

  ////////////////////////////////////////////////////////////////
  // Test 3. check first time for special file types
  ////////////////////////////////////////////////////////////////

  private final static String checkSpecial1(int nLines, String[] lines, String leader) {
    // the order here is CRITICAL

    if (nLines == 1 && lines[0].length() > 0 && PT.isDigit(lines[0].charAt(0)))
      return "Jme"; //only one line, and that line starts with a number 
    if (checkMopacGraphf(lines))
      return "MopacGraphf"; //must be prior to checkFoldingXyz and checkMol
    if (checkOdyssey(lines))
      return "Odyssey";
    switch (checkMol(lines)) {
    case 1:
    case 3:
    case 2000:
    case 3000:
      return "Mol";
    }
    switch (checkXyz(lines)) {
    case 1:
      return "Xyz";
    case 2:
      return "Bilbao";
    case 3: 
      return "PWmat";
    }
    if (checkAlchemy(lines[0]))
      return "Alchemy";
    if (checkFoldingXyz(lines))
      return "FoldingXyz";
    if (checkXSF(lines))
      return "Xcrysden";
    if (checkCube(lines))
      return "Cube";
    if (checkWien2k(lines))
      return "Wien2k";
    if (checkAims(lines))
      return "Aims";
    if (checkGenNBO(lines, leader))
      return "GenNBO";
    return null;
  }
  
  private static boolean checkXSF(String[] lines) {
    int i = 0;
    while (lines[i].length() == 0) {
      i++;
    }
    return (lines[i].startsWith("ANIMSTEPS ") || lines[i].equals("ATOMS\n") && PT.parseInt(lines[i + 1]) > 0);
  }

  private static boolean checkAims(String[] lines) {

    // use same tokenizing mechanism as in AimsReader.java to also recognize
    // AIMS geometry files with indented keywords
    // use same tokenizing mechanism as in AimsReader.java 
    //  to reliably recognize FHI-aims files
    // "atom" is a VERY generic term; just "atom" breaks HIN reader. 
    // >= token.length are necessary to allow for comments at the end of valid lines
    //  (as perfectly legal in simple Fortran list based IO) 
    for (int i = 0; i < lines.length; i++) {
      if (lines[i].startsWith("mol 1"))
        return false;  /* hin format also uses "atom " */
      String[] tokens = PT.getTokens(lines[i]);
      if (tokens.length == 0)
        continue;
      if (tokens[0].startsWith("atom") && tokens.length > 4
          && Float.isNaN(PT.parseFloat(tokens[4]))
          || tokens[0].startsWith("multipole") && tokens.length >= 6
          || tokens[0].startsWith("lattice_vector") && tokens.length >= 4)
        return true;
    }
    return false;
  }

  private static boolean checkAlchemy(String line) {
    /*
    11 ATOMS,    12 BONDS,     0 CHARGES
    */
    int pt;
    if ((pt = line.indexOf("ATOMS")) > 0 && line.indexOf("BONDS") > pt) {
        int n = PT.parseInt(line.substring(0, pt).trim());
        return (n > 0);
      }
    return false;
  }

  private static int[] n = new int[1];
  private static boolean isInt(String s) {
    n[0] = 0;
    s = s.trim();
    return s.length() > 0 && PT.parseIntNext(s, n) != Integer.MIN_VALUE && n[0] == s.length();
  }

  private static boolean isFloat(String s) {
    return !Float.isNaN(PT.parseFloat(s));
  }


  private static boolean checkCube(String[] lines) {
      for (int j = 2; j <= 5; j++) {
        StringTokenizer tokens2 = new StringTokenizer(lines[j]);
        int n = tokens2.countTokens();
        if (!(n == 4 || j == 2 && n == 5) || !isInt(tokens2.nextToken()))
          return false;
        for (int i = 3; --i >= 0;)
          if (!isFloat(tokens2.nextToken()))
              return false;
        if (n == 5 && !isInt(tokens2.nextToken()))
            return false;
      }
      return true;
  }

  /**
   * @param lines First lines of the files.
   * @return Indicates if the file may be a Folding@Home file.
   */
  private static boolean checkFoldingXyz(String[] lines) {
    // Checking first line: <number of atoms> <protein name>
    StringTokenizer tokens = new StringTokenizer(lines[0].trim(), " \t");
    if (tokens.countTokens() < 2 || !isInt(tokens.nextToken().trim()))
      return false;
    // Checking second line: <atom number> ...
    String secondLine = lines[1].trim();
    if (secondLine.length() == 0)
        secondLine = lines[2].trim();
    tokens = new StringTokenizer(secondLine, " \t");
    return (tokens.countTokens() > 0 && isInt(tokens.nextToken().trim()));
  }
  
  private static boolean checkGenNBO(String[] lines, String leader) {
    // .31-.41 file or .47 or .nbo file
    return (leader.indexOf("$GENNBO") >= 0
      || lines[1].startsWith(" Basis set information needed for plotting orbitals")
      || lines[1].indexOf("s in the AO basis:") >= 0
      || lines[1].indexOf("***** NBO ") >= 0
      || lines[2].indexOf(" N A T U R A L   A T O M I C   O R B I T A L") >= 0);
  }
  
  private static int checkMol(String[] lines) {
    String line4trimmed = ("X" + lines[3]).trim().toUpperCase();
    if (line4trimmed.length() < 7 || line4trimmed.indexOf(".") >= 0 || lines[0].startsWith("data_"))
      return 0;
    if (line4trimmed.endsWith("V2000"))
      return 2000;
    if (line4trimmed.endsWith("V3000"))
      return 3000;
    int n1 = PT.parseInt(lines[3].substring(0, 3).trim());
    int n2 = PT.parseInt(lines[3].substring(3, 6).trim());
    return (n1 > 0 && n2 >= 0 && lines[0].indexOf("@<TRIPOS>") != 0
        && lines[1].indexOf("@<TRIPOS>") != 0 
        && lines[2].indexOf("@<TRIPOS>") != 0 ? 3 : 0);
  }

  /**
   * @param lines First lines of the files.
   * @return Indicates if the file is a Mopac GRAPHF output file.
   */
  
  private static boolean checkMopacGraphf(String[] lines) {
    return (lines[0].indexOf("MOPAC-Graphical data") > 2); //nAtoms MOPAC-Graphical data
  }

  private static boolean checkOdyssey(String[] lines) {
    int i;
    for (i = 0; i < lines.length; i++)
      if (!lines[i].startsWith("C ") && lines[i].length() != 0)
        break;
    if (i >= lines.length 
        || lines[i].charAt(0) != ' ' 
        || (i = i + 2) + 1 >= lines.length)
      return false;
      // distinguishing between Spartan input and MOL file
      // MOL files have aaabbb.... on the data line
      // SPIN files have cc s on that line (c = charge; s = spin)
      // so the typical MOL file, with more parameters, will fail getting the spin
    String l = lines[i];
    if (l.length() < 3)
    	return false;
      int spin = PT.parseInt(l.substring(2).trim());
      int charge = PT.parseInt(l.substring(0, 2).trim());
      // and if it does not, then we get the next lines of info
      if ((l = lines[i + 1]).length() < 2)
    	  return false;
      int atom1 = PT.parseInt(l.substring(0, 2).trim());
      if (spin < 0 || spin > 5 || atom1 <= 0 || charge == Integer.MIN_VALUE || charge > 5)
        return false;
      // hard to believe we would get here for a MOL file
      float[] atomline = AtomSetCollectionReader.getTokensFloat(l, null, 5);
      return !Float.isNaN(atomline[1]) && !Float.isNaN(atomline[2]) && !Float.isNaN(atomline[3]) && Float.isNaN(atomline[4]);
  }
  
  private static boolean checkWien2k(String[] lines) {
    return (lines[2].startsWith("MODE OF CALC=")
        || lines[2].startsWith("             RELA") || lines[2]
          .startsWith("             NREL"));
  }
 
  private static int checkXyz(String[] lines) {
    // first and third lines numerical --> Bilbao format
    // first int and line[5] starts with "POSITION" (case insensitice) --> PWmat atom.config

    boolean checkPWM = false;
    int i = PT.parseInt(lines[0]);
    if (i >= 0 && lines[0].trim().equals("" + i)) {
      if (isInt(lines[2]))
        return 2; 
      checkPWM = true;
    }
    if (lines[0].indexOf("Bilbao Crys") >= 0)
      return 2;
    String s;
    if ((checkPWM || lines.length > 5 && i > 0)
        && ((s = lines[1].trim().toUpperCase()).startsWith("LATTICE VECTOR") || s.equals("LATTICE")))
      return 3;
    return (checkPWM ? 1 : 0);
  }
  
  ////////////////////////////////////////////////////////////////
  // Test 4. One of the first 16 lines starts with one of these strings
  ////////////////////////////////////////////////////////////////

  private static String checkLineStarts(String[] lines) {
    for (int i = 0; i < lineStartsWithRecords.length; ++i) {
      String[] recordTags = lineStartsWithRecords[i];
      for (int j = 1; j < recordTags.length; ++j) {
        String recordTag = recordTags[j];
        for (int k = 0; k < lines.length; k++) {
          if (lines[k].startsWith(recordTag))
            return recordTags[0];
        }
      }
    }
    return null;
  }

  private final static String[] mmcifLineStartRecords =
  { "MMCif", "_entry.id", "_database_PDB_", "_pdbx_", "_chem_comp.pdbx_type", "_audit_author.name", "_atom_site." };

  private final static String[] cifLineStartRecords =
  { "Cif", "data_", "_publ" };

  private final static String[] pdbLineStartRecords = {
    "Pdb", "HEADER", "OBSLTE", "TITLE ", "CAVEAT", "COMPND", "SOURCE", "KEYWDS",
    "EXPDTA", "AUTHOR", "REVDAT", "SPRSDE", "JRNL  ", "REMARK ",
    "DBREF ", "SEQADV", "SEQRES", "MODRES", 
    "HELIX ", "SHEET ", "TURN  ",
    "CRYST1", "ORIGX1", "ORIGX2", "ORIGX3", "SCALE1", "SCALE2", "SCALE3",
    "ATOM  ", "HETATM", "MODEL ", "LINK  ", "USER  MOD ",
  };

  private final static String[] cgdLineStartRecords = 
  { "Cgd", "EDGE ", "edge " };

  private final static String[] shelxLineStartRecords =
  { "Shelx", "TITL ", "ZERR ", "LATT ", "SYMM ", "CELL " };

  private final static String[] ghemicalMMLineStartRecords =
  { "GhemicalMM", "!Header mm1gp", "!Header gpr" };

  private final static String[] jaguarLineStartRecords =
  { "Jaguar", "  |  Jaguar version", };

  private final static String[] mdlLineStartRecords = 
  { "Mol", "$MDL " };

  private final static String[] spartanSmolLineStartRecords =
  { "SpartanSmol", "INPUT=" };

  private final static String[] csfLineStartRecords =
  { "Csf", "local_transform" };
  
  private final static String[] mdTopLineStartRecords =
  { "MdTop", "%FLAG TITLE" };
  
  private final static String[] hyperChemLineStartRecords = 
  { "HyperChem", "mol 1" };

  private final static String[] vaspOutcarLineStartRecords = 
  { "VaspOutcar", " vasp.", " INCAR:" };

  private final static String[][] lineStartsWithRecords =
  { mmcifLineStartRecords, cifLineStartRecords,
    pdbLineStartRecords, cgdLineStartRecords, shelxLineStartRecords, 
    ghemicalMMLineStartRecords, jaguarLineStartRecords, 
    mdlLineStartRecords, spartanSmolLineStartRecords, csfLineStartRecords, 
    mol2Records, mdTopLineStartRecords, hyperChemLineStartRecords,
    vaspOutcarLineStartRecords
    };

 
  ////////////////////////////////////////////////////////////////
  // Test 5. contents of first 16384 bytes  
  ////////////////////////////////////////////////////////////////

  private static String checkHeaderContains(String header) throws Exception {
    for (int i = 0; i < headerContainsRecords.length; ++i) {
      String[] recordTags = headerContainsRecords[i];
      for (int j = 1; j < recordTags.length; ++j) {
        String recordTag = recordTags[j];
        if (header.indexOf(recordTag) < 0)
          continue;
        String type = recordTags[0];
        if (!type.equals("Xml"))
          return type;
        if (header.indexOf("/AFLOWDATA/") >= 0 || header.indexOf("-- Structure PRE --") >= 0)
          return "AFLOW";
        // for XML check for an error message from a server -- certainly not XML
        // but new CML format includes xmlns:xhtml="http://www.w3.org/1999/xhtml" in <cml> tag.
        return (header.indexOf("<!DOCTYPE HTML PUBLIC") < 0
              && header.indexOf("XHTML") < 0 
              && (header.indexOf("xhtml") < 0 || header.indexOf("<cml") >= 0) 
              ? getXmlType(header) 
            : null);
      }
    }
    return null;
  }

  private static String getXmlType(String header) throws Exception  {
    if (header.indexOf("http://www.molpro.net/") >= 0) {
      return "XmlMolpro";
    }
    if (header.indexOf("odyssey") >= 0) {
      return "XmlOdyssey";
    }
    if (header.indexOf("C3XML") >= 0) {
      return "XmlChem3d";
    }
    if (header.indexOf("CDXML") >= 0) {
      return "XmlChemDraw";
    }
    if (header.indexOf("arguslab") >= 0) {
      return "XmlArgus";
    }
    if (header.indexOf("jvxl") >= 0 
        || header.indexOf(CML_NAMESPACE_URI) >= 0
        || header.indexOf("cml:") >= 0
        || header.indexOf("<cml>") >= 0) {
      return "XmlCml";
    }
    if (header.indexOf("XSD") >= 0) {
      return "XmlXsd";
    }
    if (header.indexOf(">vasp") >= 0) {
      return "XmlVasp";
    }
    if (header.indexOf("<GEOMETRY_INFO>") >= 0) {
      return "XmlQE";
    }
    
    return "XmlCml(unidentified)";
  }

  private final static String[] bilbaoContainsRecords =
  { "Bilbao", ">Bilbao Crystallographic Server<" };

  private final static String[] xmlContainsRecords = 
  { "Xml", "<?xml", "<atom", "<molecule", "<reaction", "<cml", "<bond", ".dtd\"",
    "<list>", "<entry", "<identifier", "http://www.xml-cml.org/schema/cml2/core" };

  private final static String[] gaussianContainsRecords =
  { "Gaussian", "Entering Gaussian System", "Entering Link 1", "1998 Gaussian, Inc." };

  private final static String[] ampacContainsRecords =
  { "Ampac", "AMPAC Version" };
  
  private final static String[] mopacContainsRecords =
  { "Mopac", "MOPAC 93 (c) Fujitsu", 
    "MOPAC FOR LINUX (PUBLIC DOMAIN VERSION)",
    "MOPAC:  VERSION  6", "MOPAC   7", "MOPAC2", "MOPAC (PUBLIC" };

  private final static String[] qchemContainsRecords = 
  { "Qchem", "Welcome to Q-Chem", "A Quantum Leap Into The Future Of Chemistry" };

  private final static String[] gamessUKContainsRecords =
  { "GamessUK", "GAMESS-UK", "G A M E S S - U K" };

  private final static String[] gamessUSContainsRecords =
  { "GamessUS", "GAMESS", "$CONTRL" };

  private final static String[] spartanBinaryContainsRecords =
  { "SpartanSmol" , "|PropertyArchive", "_spartan", "spardir", "BEGIN Directory Entry Molecule" };

  private final static String[] spartanContainsRecords =
  { "Spartan", "Spartan", "converted archive file" };  // very old Spartan files; sparchive files

  private final static String[] adfContainsRecords =
  { "Adf", "Amsterdam Density Functional" };
  
  private final static String[] psiContainsRecords =
  { "Psi", "    PSI  3", "PSI3:"};
 
  private final static String[] nwchemContainsRecords =
  { "NWChem", " argument  1 = "};

  private final static String[] uicrcifContainsRecords =
  { "Cif", "Crystallographic Information File"};
  
  private final static String[] dgridContainsRecords =
  { "Dgrid", "BASISFILE   created by DGrid" };
  
  private final static String[] crystalContainsRecords =
  { "Crystal",
      "*                                CRYSTAL", "TORINO", "DOVESI" };

  private final static String[] dmolContainsRecords =
  { "Dmol", "DMol^3" };

  private final static String[] gulpContainsRecords =
  { "Gulp", "GENERAL UTILITY LATTICE PROGRAM" };
  
  private final static String[] espressoContainsRecords =
  { "Espresso", "Program PWSCF", "Program PHONON" }; 

  private final static String[] siestaContainsRecords =
  { "Siesta", "MD.TypeOfRun", "SolutionMethod", "MeshCutoff", 
    "WELCOME TO SIESTA" };
  
  private final static String[] xcrysDenContainsRecords = 
  { "Xcrysden", "PRIMVEC", "CONVVEC", "PRIMCOORD", "ANIMSTEP" };

  private final static String[] mopacArchiveContainsRecords =
  { "MopacArchive", "SUMMARY OF PM" };
  
  private final static String[] abinitContainsRecords = { "Abinit",
    "http://www.abinit.org", "Catholique", "Louvain" };  
  
  private final static String[] qcJsonContainsRecords = 
  { "QCJSON", "\"QCJSON" };

  /*
  private final static String[] gaussianWfnRecords =
  { "GaussianWfn", "MO ORBITALS" };
  */

  private final static String[] gaussianFchkContainsRecords =
  { "GaussianFchk", "Number of point charges in /Mol/" };

  private final static String[] inputContainsRecords =
  { "Input", " ATOMS cartesian", "$molecule", "&zmat", "geometry={", "$DATA", "%coords", "GEOM=PQS", "geometry units angstroms" };
    
  private final static String[] aflowContainsRecords =
  { "AFLOW", "/AFLOWDATA/"};

  private final static String[] magCifContainsRecords =
  { "MagCif", "_space_group_magn"};

  private final static String[][] headerContainsRecords =
  { sptRecords, bilbaoContainsRecords, xmlContainsRecords, gaussianContainsRecords, 
    ampacContainsRecords, mopacContainsRecords,  
    gamessUKContainsRecords, gamessUSContainsRecords,
    qchemContainsRecords, spartanBinaryContainsRecords, spartanContainsRecords,  
    mol2Records, adfContainsRecords, psiContainsRecords,
    nwchemContainsRecords, uicrcifContainsRecords, 
    dgridContainsRecords, crystalContainsRecords, 
    dmolContainsRecords, gulpContainsRecords, 
    espressoContainsRecords, siestaContainsRecords, xcrysDenContainsRecords,
    mopacArchiveContainsRecords,abinitContainsRecords,gaussianFchkContainsRecords,
    inputContainsRecords, aflowContainsRecords, magCifContainsRecords, 
    qcJsonContainsRecords    
  };
  
  ////////////////////////////////////////////////////////////////
  // Test 6. check second time for special file types
  ////////////////////////////////////////////////////////////////

  private final static String checkSpecial2(String[] lines) {
    // the order here is CRITICAL
    if (checkGromacs(lines))
      return "Gromacs";
    if (checkCrystal(lines))
      return "Crystal";
    if (checkFAH(lines))
      return "FAH";
    String s = checkCastepVaspSiesta(lines);
    if (s != null)
      return s;
    return null;
  }
  
  
  private static boolean checkFAH(String[] lines) {
    String s = lines[0].trim() + lines[2].trim();
    return s.equals("{\"atoms\": [");
  }

  private static boolean checkCrystal(String[] lines) {
    String s = lines[1].trim();
    if (s.equals("SLAB") ||s.equals("MOLECULE")
        || s.equals("CRYSTAL") 
        || s.equals("POLYMER") || (s = lines[3]).equals("SLAB")
        || s.equals("MOLECULE") || s.equals("POLYMER"))
      return true;
    for (int i = 0; i < lines.length; i++) {
      if (lines[i].trim().equals("OPTGEOM") || lines[i].trim().equals("FREQCALC") ||
          lines[i].contains("DOVESI") 
          || lines[i].contains("TORINO") 
          || lines[i].contains("http://www.crystal.unito.it")
          //new lenghty scripts for CRYSTAL14  
          || lines[i].contains("Pcrystal")
          || lines[i].contains("MPPcrystal")
          || lines[i].contains("crystal executable"))
        return true;
    }
    return false;
  }
  
  private static boolean checkGromacs(String[] lines) {
    if (PT.parseInt(lines[1]) == Integer.MIN_VALUE)
      return false;
    int len = -1;
    for (int i = 2; i < 16 && len != 0; i++)
      if ((len = lines[i].length()) != 69 && len != 45 && len != 0)
        return false;
    return true;
  }

  private static String checkCastepVaspSiesta(String[] lines) {
    for ( int i = 0; i<lines.length; i++ ) {
      String line = lines[i].toUpperCase();
      if (line.indexOf("FREQUENCIES IN         CM-1") == 1
          || line.contains("CASTEP")
          || line.startsWith("%BLOCK LATTICE_ABC")
          || line.startsWith("%BLOCK LATTICE_CART")
          || line.startsWith("%BLOCK POSITIONS_FRAC")
          || line.startsWith("%BLOCK POSITIONS_ABS") 
          || line.contains("<-- E")) return "Castep";
      if (line.contains("%BLOCK"))
        return "Siesta";
      if (i >= 6 && i < 10 && (line.startsWith("DIRECT") || line.startsWith("CARTESIAN")))
        return "VaspPoscar";        
    }
    return null;
  }

}

