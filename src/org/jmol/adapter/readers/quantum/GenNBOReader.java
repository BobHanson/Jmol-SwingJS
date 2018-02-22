/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-12 00:46:22 -0500 (Tue, 12 Sep 2006) $
 * $Revision: 5501 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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

package org.jmol.adapter.readers.quantum;

import java.io.BufferedReader;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.Rdr;
import javajs.util.SB;

import org.jmol.adapter.smarter.Atom;
import org.jmol.quantum.QS;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;


/**
 * NBO file nn reader will pull in other files as necessary
 * 
 * acknowledgments: Grange Hermitage, Frank Weinhold
 * 
 * 
 * upgrade to NBO 6 allows reading of resonance structures, including base structure
 * 
 * 
 * @author hansonr
 **/


public class GenNBOReader extends MOReader {

  
//
//  *********************************** NBO 6.0 ***********************************
//              N A T U R A L   A T O M I C   O R B I T A L   A N D
//           N A T U R A L   B O N D   O R B I T A L   A N A L Y S I S
//  **************************** Robert Hanson (100634) ***************************
//   (c) Copyright 1996-2014 Board of Regents of the University of Wisconsin System
//       on behalf of the Theoretical Chemistry Institute.  All rights reserved.
//
//           Cite this program as:
//
//           NBO 6.0.  E. D. Glendening, J. K. Badenhoop, A. E. Reed,
//           J. E. Carpenter, J. A. Bohmann, C. M. Morales, C. R. Landis,
//           and F. Weinhold (Theoretical Chemistry Institute, University
//           of Wisconsin, Madison, WI, 2013); http://nbo6.chem.wisc.edu/
//
//        /NLMO   / : Form natural localized molecular orbitals
//        /NRT    / : Natural Resonance Theory Analysis
//        /AOPNAO / : Print the AO to PNAO transformation
//        /SAO    / : Print the AO overlap matrix
//        /STERIC / : Print NBO/NLMO steric analysis
//        /CMO    / : Print analysis of canonical MOs
//        /PLOT   / : Write information for the orbital plotter
//        /FILE   / : Set to co2_p
//
//  Filename set to co2_p

  private boolean isOutputFile;
  private String nboType = "";
  private int nOrbitals0;
  private boolean is47File;
  private boolean isOpenShell;
  private boolean alphaOnly, betaOnly;

  private int nAOs;

  @Override
  protected void initializeReader() throws Exception {
    /*
     * molname.31 AO 
     * molname.32 PNAO 
     * molname.33 NAO 
     * molname.34 PNHO 
     * molname.35 NHO
     * molname.36 PNBO 
     * molname.37 NBO 
     * molname.38 PNLMO 
     * molname.39 NLMO 
     * molname.40 MO 
     * molname.41 AO density matrix 
     * molname.46 Basis label file
     * molname.47 archive file
     * molname.nbo output file
     * 
     */
    String line1 = rd().trim();
    is47File = (line1.indexOf("$GENNBO") >= 0 || line1.indexOf("$NBO") >= 0); // GENNBO 6
    alphaOnly =  is47File || checkFilterKey("ALPHA");
    betaOnly =  !is47File && checkFilterKey("BETA");
    
    
    if (is47File) {
      readData47();
      return;
    }
    boolean isOK;
    String line2 = rd();
    line = line1 + line2;
    isOutputFile = (line2.indexOf("****") >= 0);
    if (isOutputFile) {
      // this may or may not work. 
      isOK = getFile31();
      super.initializeReader();
      // keep going -- we need to read the file using MOReader
      //moData.put("isNormalized", Boolean.TRUE);
    } else if (line2.indexOf("s in the AO basis:") >= 0) {
      nboType = line2.substring(1, line2.indexOf("s"));
      asc.setCollectionName(line1 + ": " + nboType + "s");
      isOK = getFile31();
    } else {//if (line.indexOf("Basis set information") >= 0) {
      nboType = "AO";
      asc.setCollectionName(line1 + ": " + nboType + "s");
      isOK = readData31(line1); 
    }
    if (!isOK)
      Logger.error("Unimplemented shell type -- no orbitals available: " + line);
    if (isOutputFile) 
      return;
    if (isOK)
      readMOs();
    continuing = false;
  }

//  $GENNBO  NATOMS=7  NBAS=28  UPPER  BODM  FORMAT  $END
//      $NBO  $END
//      $COORD
//      Methylamine...RHF/3-21G//Pople-Gordon geometry
//          6    6       0.745914       0.011106       0.000000
//          7    7      -0.721743      -0.071848       0.000000
//          1    1       1.042059       1.060105       0.000000
//          1    1       1.129298      -0.483355       0.892539
//          1    1       1.129298      -0.483355      -0.892539
//          1    1      -1.076988       0.386322      -0.827032
//          1    1      -1.076988       0.386322       0.827032
//      $END


  @Override
  protected void finalizeSubclassReader() throws Exception {
    appendLoadNote("NBO type " + nboType);
    if (isOpenShell)
      asc.setCurrentModelInfo("isOpenShell", Boolean.TRUE);
    finalizeReaderASCR();
  }
  
  private String topoType = "A";
  
  @Override
  protected boolean checkLine() throws Exception {
    // for .nbo only
    if (line.indexOf("SECOND ORDER PERTURBATION THEORY ANALYSIS") >= 0
        && !orbitalsRead) {
      // Frank Weinhold suggests that NBO/.37 is not the best choice for a default.
      // PNBOs (pre-NBOs) are not orthogonalized and so "look better." But we are already
      // reading NBOs, and they are fine as well. I'd rather not change this
      // default and risk changes in PNGJ files already saved. 
      nboType = "NBO";
      String data = getFileData(".37");
      if (data == null)
        return true;
      BufferedReader readerSave = reader;
      reader = Rdr.getBR(data);
      rd();
      rd();
      readMOs();
      reader = readerSave;
      orbitalsRead = false;
      return true;
    }
    if (line.indexOf("$NRTSTRA") >= 0) {
      getStructures("NRTSTRA");
      return true;
    } 
    if (line.indexOf("$NRTSTRB") >= 0) {
      getStructures("NRTSTRB");
      return true;
    } 
    if (line.indexOf("$NRTSTR") >= 0) {
        getStructures("NRTSTR");
        return true;
    }
    if (line.indexOf(" TOPO ") >= 0) {
      getStructures("TOPO" + topoType);
      topoType = "B";
      return true;
    }
    if (line.indexOf("$CHOOSE") >= 0) {
      getStructures("CHOOSE");
      return true;
    }
    return checkNboLine();
  }

  private int nStructures = 0;
  NBOParser nboParser;
  private boolean addBetaSet;
  
  private void getStructures(String type) throws Exception {
    if (nboParser == null)
      nboParser = new NBOParser();
    
    Lst<Object> structures = getStructureList();
    SB sb = new SB();
    while (!rd().trim().equals("$END"))
      sb.append(line).append("\n");
    nStructures = nboParser.getStructures(sb.toString(), type, structures);
    appendLoadNote(nStructures + " NBO " + type + " resonance structures");
  }

  @SuppressWarnings("unchecked")
  private Lst<Object> getStructureList() {
    Lst<Object> structures = (Lst<Object>) asc.getAtomSetAuxiliaryInfo(asc.iSet).get("nboStructures");
    if (structures  == null) 
      asc.setCurrentModelInfo("nboStructures", structures = new Lst<Object>());
    return structures;
  }

  private String getFileData(String ext) throws Exception {
    String fileName = (String) htParams.get("fullPathName");
    int pt = fileName.lastIndexOf(".");
    if (pt < 0)
      pt = fileName.length();
    fileName = fileName.substring(0, pt);
    moData.put("nboRoot", fileName);
    fileName += ext;
    String data = vwr.getFileAsString3(fileName, false, null);
    Logger.info(data.length() + " bytes read from " + fileName);
    if (data.length() == 0 || data.indexOf("java.io.FileNotFound") >= 0 && nboType != "AO")
      throw new Exception(" supplemental file " + fileName + " was not found");
    return data;
  }

// 14_a Basis set information needed for plotting orbitals
// ---------------------------------------------------------------------------
// 36 90 162
// ---------------------------------------------------------------------------
// 6 -2.992884000 -1.750577000 1.960024000 
// 6 -2.378528000 -1.339374000 0.620578000
//

  private boolean getFile31() throws Exception {
    String data = getFileData(".31");
    BufferedReader readerSave = reader;
    reader = Rdr.getBR(data);
    return (readData31(null) && (reader = readerSave) != null);
  }

  /**
   * read the labels from xxxx.46
   * 
   * @throws Exception
   */
  private void getFile46() throws Exception {
    String data = getFileData(".46");
    BufferedReader readerSave = reader;
    reader = Rdr.getBR(data);
    readData46();
    reader = readerSave;
  }

  private static String P_LIST =  "101   102   103";
  // GenNBO may be 103 101 102 
  
  private static String SP_LIST = "1     101   102   103";

  private static String DS_LIST = "255   252   253   254   251"; 
  // GenNBO is 251 252 253 254 255 
  //   for     Dxy Dxz Dyz Dx2-y2 D2z2-x2-y2
  // org.jmol.quantum.MOCalculation expects 
  //   d2z^2-x2-y2, dxz, dyz, dx2-y2, dxy

  private static String DC_LIST = "201   204   206   202   203   205";
  // GenNBO is 201 202 203 204 205 206 
  //       for Dxx Dxy Dxz Dyy Dyz Dzz
  // org.jmol.quantum.MOCalculation expects 
  //      Dxx Dyy Dzz Dxy Dxz Dyz

  private static String FS_LIST = "351   352   353   354   355   356   357";
  // GenNBO is 351 352 353 354 355 356 357
  //  f(0)     2z3-3x2z-3y2z   
  //  f(c1)        4xz2-x3-xy2
  //  f(s1)            4yz2-x2y-y3
  //  f(c2)                x2z-y2z
  //  f(s2)                    xyz
  //  f(c3)                        x3-3xy2
  //  f(s3)                            3x2y-y3
  // org.jmol.quantum.MOCalculation expects the same
  private static String FC_LIST = "301   307   310   304   302   303   306   309   308   305";
  // GenNBO is 301 302 303 304 305 306 307 308 309 310
  //       for xxx xxy xxz xyy xyz xzz yyy yyz yzz zzz
  // org.jmol.quantum.MOCalculation expects
  //           xxx yyy zzz xyy xxy xxz xzz yzz yyz xyz
  //           301 307 310 304 302 303 306 309 308 305

  private static String GS_LIST = "451   452   453   454   455   456   457   458   459"; 
  // GenNBO assumes same
  // org.jmol.quantum.MOCalculation expects
  //  9G: G 0, G+1, G-1, G+2, G-2, G+3, G-3, G+4, G-4
  
  private static String GC_LIST = "415   414   413   412   411   410   409   408   407   406   405   404   403   402   401";
  // GenNBO 401  402  403  404  405  406  407  408  409  410 
  //    for xxxx xxxy xxxz xxyy xxyz xxzz xyyy xyyz xyzz xzzz 
  // GenNBO 411  412  413  414  415  
  //        yyyy yyyz yyzz yzzz zzzz
  // 
  // Gaussian is exactly opposite this.
  
  
  private static String HS_LIST = "551   552   553   554   555   556   557   558   559   560   561";
  
  private static String HC_LIST = 
      "521   520   519   518   517   516   515   514   513   512   " +
  		"511   510   509   508   507   506   505   504   503   502   501"; 
  // GenNBO is 501-521
  //        501   502   503   504   505   506   507   508   509   510
  //    for xxxxx xxxxy xxxxz xxxyy xxxyz xxxzz xxyyy xxyyz xxyzz xxzzz
  //        511   512   513   514   515   516   517   518   519   520
  //        xyyyy xyyyz xyyzz xyzzz xzzzz yyyyy yyyyz yyyzz yyzzz yzzzz
  //        521
  //        zzzzz
  //
  // Gaussian is opposite
  
  private static String IS_LIST = "651   652   653   654   655   656   657   658   659   660   661   662   663";

  private static String IC_LIST = 
      "628   627   626   625   624   623   622   621   620   " +
  		"619   618   617   616   615   614   613   612   611   610   " +
      "609   608   607   606   605   604   603   602   601";
  // GenNBO is 601-628
  // for xxxxxx xxxxxy xxxxxz xxxxyy xxxxyz xxxxzz xxxyyy xxxyyz xxxyzz xxxzzz 
  //     xxyyyy xxyyyz xxyyzz xxyzzz xxzzzz xyyyyy xyyyyz xyyyzz xyyzzz xyzzzz 
  //     xzzzzz yyyyyy yyyyyz yyyyzz yyyzzz yyzzzz yzzzzz zzzzzz
  //
  // Gaussian is opposite this 
  
  private void readData47() throws Exception {
    allowNoOrbitals = true;
    discardLinesUntilContains("$COORD");
    asc.newAtomSet();
    asc.setAtomSetName(rd().trim());
    while (rd().indexOf("$END") < 0) {
      String[] tokens = getTokens();
      addAtomXYZSymName(tokens, 2, null, null).elementNumber = (short) parseIntStr(tokens[0]);
    }
    discardLinesUntilContains("$BASIS");
    int[] centers = getIntData();
    int[] labels = getIntData();
    
    discardLinesUntilContains("NSHELL =");
    
    
    
    shellCount = parseIntAt(line, 10);
    gaussianCount = parseIntAt(rd(), 10);
    rd();
    int[] ncomp = getIntData();
    int[] nprim = getIntData();
    int[] nptr = getIntData();
    // read basis functions
    shells = new  Lst<int[]>();
    gaussians = AU.newFloat2(gaussianCount);
    for (int i = 0; i < gaussianCount; i++)
      gaussians[i] = new float[6];
    nOrbitals = 0;
    int ptCenter = 0;
    String l = line;
    for (int i = 0; i < shellCount; i++) {
      int[] slater = new int[4];
      int nc = ncomp[i];
      slater[0] = centers[ptCenter];
      line = "";
      for (int ii = 0; ii < nc; ii++)
        line += labels[ptCenter++] + " ";
      if (!fillSlater(slater, nc, nptr[i] - 1, nprim[i]))
        return;
    }
    line = l;
    getAlphasAndExponents();
    nboType = "AO";
    readMOs();
    continuing = false;
  }

  private int[] getIntData() throws Exception {
    while (line.indexOf("=") < 0)
      rd();
    String s = line.substring(line.indexOf("=") + 1);
    line = "";
    while (rd().indexOf("=") < 0 && line.indexOf("$") < 0)
      s += line;
    String[] tokens = PT.getTokens(s);
    int[] f = new int[tokens.length];
    for (int i = f.length; --i >= 0;)
      f[i] = parseIntStr(tokens[i]);
    return f;
  }

  private boolean fillSlater(int[] slater, int n, int pt, int ng) {
    nOrbitals += n;
    switch (n) {
    case 1:
      slater[1] = QS.S;
      break;
    case 3:
      if (!getDFMap("P", line, QS.P, P_LIST, 3))
        return false;
      slater[1] = QS.P;
      break;
    case 4:
      if (!getDFMap("SP", line, QS.SP, SP_LIST, 1))
        return false;
      slater[1] = QS.SP;
      break;
      
    case 5:
      if (!getDFMap("DS", line, QS.DS, DS_LIST, 3))
        return false;
      slater[1] = QS.DS;
      break;
    case 6:
      if (!getDFMap("DC", line, QS.DC, DC_LIST, 3))
        return false;
      slater[1] = QS.DC;
      break;
      
    case 7:
      if (!getDFMap("FS", line, QS.FS, FS_LIST, 3))
        return false;
      slater[1] = QS.FS;
      break;
    case 10:
      if (!getDFMap("FC", line, QS.FC, FC_LIST, 3))
        return false;
      slater[1] = QS.FC;
      break;
      
    case 9:
      if (!getDFMap("GS", line, QS.GS, GS_LIST, 3))
        return false;
      slater[1] = QS.GS;
      break;
    case 15:
      if (!getDFMap("GC", line, QS.GC, GC_LIST, 3))
        return false;
      slater[1] = QS.GC;
      break;
      
    case 11:
      if (!getDFMap("HS", line, QS.HS, HS_LIST, 3))
        return false;
      slater[1] = QS.HS;
      break;
    case 21:
      if (!getDFMap("HC", line, QS.HC, HC_LIST, 3))
        return false;
      slater[1] = QS.HC;
      break;
      
    case 13:
      if (!getDFMap("IS", line, QS.IS, IS_LIST, 3))
        return false;
      slater[1] = QS.IS;
      break;
    case 28:
      if (!getDFMap("IC", line, QS.IC, IC_LIST, 3))
        return false;
      slater[1] = QS.IC;
      break;
    default:
      Logger.error("Unrecognized orbital slater count: " + n);
      break;
    }
    slater[2] = pt + 1; // gaussian list pointer
    slater[3] = ng; // number of gaussians
    shells.addLast(slater);
    return true;
  }

  private void getAlphasAndExponents() throws Exception {
    // EXP  CS  CP  CD  ??
    for (int j = 0; j < 5; j++) {
      if (line.indexOf("=") < 0)
        rd();
      if (line.indexOf("$END") >= 0)
        break;
      line = line.substring(line.indexOf("=") + 1);
      float[] temp = fillFloatArray(line, 0, new float[gaussianCount]);
      for (int i = 0; i < gaussianCount; i++) {
        gaussians[i][j] = temp[i];
        if (j > 1)
          gaussians[i][5] += temp[i];
      }
    }
    // GenNBO lists S, P, D, F, G orbital coefficients separately
    // we need all of them in [1] if [1] is zero (not S or SP)
    for (int i = 0; i < gaussianCount; i++) {
      if (gaussians[i][1] == 0)
        gaussians[i][1] = gaussians[i][5];
    }
    if (debugging) {
      Logger.debug(shells.size() + " slater shells read");
      Logger.debug(gaussians.length + " gaussian primitives read");
    }
  }

  private boolean readData31(String line1) throws Exception {
    
// HCN(-), E(UB3LYP) = -93.4597027357 [nu: 804(2),2186,3329] Vibrational te
//     Basis set information needed for plotting orbitals
//     ---------------------------------------------------------------------------
//          3    74   111
//     ---------------------------------------------------------------------------
//        1      0.000000      0.000000     -1.567118
//        6      0.000000      0.000000     -0.496424
//        7      0.000000      0.000000      0.649380
//     ---------------------------------------------------------------------------
//          1     1     1     4
//          1
//          1     1     5     1
//          1
//          1     1     6     1
//          1
//          1     1     7     1
//          1
//          1     1     8     1
//          1
//          1     1     9     1
//          1
//          1     3    10     1
//        101   102   103
// ...
//          2    11    66     1
//        551   552   553   554   555   556   557   558   559   560
//        561

    
    if (line1 == null) {
      line1 = rd();
      rd();
    }
    rd(); // ----------

    // read ac, shellCount, and gaussianCount
    String[] tokens = PT.getTokens(rd());
    int ac = parseIntStr(tokens[0]);
    shellCount = parseIntStr(tokens[1]);
    gaussianCount = parseIntStr(tokens[2]);

    // read atom types and positions
    rd(); // ----------
    asc.newAtomSet();
    asc.setAtomSetName(nboType + "s: " + line1.trim());
    asc.setCurrentModelInfo("nboType", nboType);
    for (int i = 0; i < ac; i++) {
      tokens = PT.getTokens(rd());
      int z = parseIntStr(tokens[0]);
      if (z < 0) // dummy atom
        continue;
      Atom atom = asc.addNewAtom();
      atom.elementNumber = (short) z;
      setAtomCoordTokens(atom, tokens, 1);
    }

    // read basis functions
    shells = new  Lst<int[]>();
    gaussians = AU.newFloat2(gaussianCount);
    for (int i = 0; i < gaussianCount; i++)
      gaussians[i] = new float[6];
    rd(); // ----------
    nOrbitals = 0;
    for (int i = 0; i < shellCount; i++) {
      tokens = PT.getTokens(rd());
      int[] slater = new int[4];
      slater[0] = parseIntStr(tokens[0]); // atom pointer; 1-based
      int n = parseIntStr(tokens[1]);
      int pt = parseIntStr(tokens[2]) - 1; // gaussian list pointer
      int ng = parseIntStr(tokens[3]);     // number of gaussians
      line = rd();
      for (int j = (n - 1) / 10; --j >= 0;)
        line += rd().substring(1);
      line = line.trim();
      System.out.println(line);
      if (!fillSlater(slater, n, pt, ng))
        return false;
    }
    rd();
    getAlphasAndExponents();
    return true;
  }

  /**
   * read labels
   * 
   * @throws Exception
   */
  private void readData46() throws Exception {
    Map<String, String[]> map = new Hashtable<String, String[]>();
    String[] tokens = new String[0];
    rd();
    nAOs = nOrbitals;
    while (line != null && line.length() > 0) {
      tokens = PT.getTokens(line);
      String type = tokens[0];
      isOpenShell = (tokens.length == 3);
      String ab = (isOpenShell ? tokens[1] : "");
      String count = tokens[tokens.length - 1];
      String key = (ab.equals("BETA") ? "beta_" : "") + type;
      if (parseIntStr(count) != nOrbitals) {
        Logger.error("file 46 number of orbitals (" + count + ") does not match nOrbitals: "
            + nOrbitals + "\n");
        return;
      }
      SB sb = new SB();
      while (rd() != null && line.length() > 4 && " NA NB AO NH".indexOf(line.substring(1, 4)) < 0)
        sb.append(line);
      sb.appendC(' ');
      String data = PT.rep(sb.toString(), " )", ")");
      sb = new SB();
      for (int i = 0, n = data.length() - 1; i < n; i++) { 
        char c = data.charAt(i);
        switch (c) {
        case '(':
        case '-':
          if (data.charAt(i + 1) == ' ')
            i++;
          break;
        case ' ':
          if (PT.isDigit(data.charAt(i + 1)) || data.charAt(i + 1) == '(')
            continue;
          break;
        }
        sb.appendC(c);
      }
      tokens = PT.getTokens(sb.toString());
      map.put(key, tokens);
    }
    String labelKey = getLabelKey(nboType);
    tokens = map.get((betaOnly ? "beta_" : "") + labelKey);
    moData.put("nboLabelMap", map);
    if (tokens == null) {
      tokens = new String[nAOs];
      for (int i = 0; i < nAOs; i++)
        tokens[i] = nboType + (i + 1);
      map.put(nboType, tokens);
      if (isOpenShell)
        map.put("beta_" + nboType, tokens);        
    }
    moData.put("nboLabels", tokens);
    addBetaSet = (isOpenShell && !betaOnly && !is47File); 
    if (addBetaSet) 
      nOrbitals *= 2;
    for (int i = 0; i < nOrbitals; i++)
      setMO(new Hashtable<String, Object>());
    setNboLabels(tokens, nAOs, orbitals, nOrbitals0, nboType);
    if (addBetaSet) {
      moData.put("firstBeta", Integer.valueOf(nAOs));
      setNboLabels( map.get("beta_" + labelKey), nAOs, orbitals, nOrbitals0 + nAOs, nboType);
    }
    Lst<Object> structures = getStructureList();
    NBOParser.getStructures46(map.get("NBO"), "alpha", structures, asc.ac);
    NBOParser.getStructures46(map.get("beta_NBO"), "beta", structures, asc.ac);
    
  }

  private static String getLabelKey(String labelKey) {
    if (labelKey.startsWith("P"))
      labelKey = labelKey.substring(1);
    if (labelKey.equals("NLMO"))
      labelKey = "NBO";
    if (labelKey.equals("MO"))
      labelKey = "NO";
    return labelKey;
  }

  @SuppressWarnings("unchecked")
  public static boolean readNBOCoefficients(Map<String, Object> moData, String nboType,
                                   Viewer vwr) {
    //         31    32    33    34    35    36    37    38    39    40    41
    int ext = ";AO;  ;PNAO;;NAO; ;PNHO;;NHO; ;PNBO;;NBO; ;PNLMO;NLMO;;MO;  ;NO;"
        .indexOf(";" + nboType + ";");
    ext = ext / 6 + 31;
    boolean isAO = nboType.equals("AO");
    boolean isNBO = nboType.equals("NBO");
    //boolean discardExtra = PT.isOneOf(nboType, ";NBO;NLMO;");
    boolean hasNoBeta = PT.isOneOf(nboType, ";AO;PNAO;NAO;");
    Map<String, String[]> map = (Map<String, String[]>) moData.get("nboLabelMap");
    String labelKey = getLabelKey(ext >= 40 ? "PNBO" : nboType);
    String[] nboLabels = map.get(labelKey);
    int nAOs = nboLabels.length;
    if (ext >= 40) {
      // MO and NO need simple MO1 MO2 .. labels
      nboLabels = new String[nboLabels.length];
      for (int i = 0; i < nAOs; i++)
        nboLabels[i] = nboType + (i + 1);
      labelKey = nboType;
      map.put(nboType, nboLabels);
      if (!hasNoBeta)
        map.put("beta_" + nboType, nboLabels);
    }
    try {
      Lst<Map<String, Object>> orbitals = (Lst<Map<String, Object>>) moData
          .get(nboType + "_coefs");
      if (orbitals == null) {
        String data = null;
        if (!isAO) {
          String fileName = moData.get("nboRoot") + "." + ext;
          if ((data = vwr.getFileAsString3(fileName, true, null)) == null)
            return false;
          data = data.substring(data.indexOf("--\n") + 3).toLowerCase();
          if (ext == 33)
            data = data.substring(0, data.indexOf("--\n") + 3);
        }
        orbitals = (Lst<Map<String, Object>>) moData.get("mos");
        Object dfCoefMaps = orbitals.get(0).get("dfCoefMaps");
        int nOrbitals = orbitals.size();
        orbitals = new Lst<Map<String, Object>>();
        for (int i = nOrbitals; --i >= 0;) {
          Map<String, Object> mo = new Hashtable<String, Object>();
          orbitals.addLast(mo);
          mo.put("dfCoefMaps", dfCoefMaps);
        }
        setNboLabels(nboLabels, nAOs, orbitals, 0, nboType);
        int len = 0;
        int[] next = null;
        if (!isAO) {
          if (data.indexOf("alpha") >= 0) {
            data = data.substring(data.indexOf("alpha") + 10); // "alpha spin"
          }
          len = data.length();
          next = new int[1];
        }
        for (int i = 0; i < nOrbitals; i++) {
          if (!isAO && i == nAOs) {
            if (isNBO)
              getNBOOccupanciesStatic(orbitals, nAOs, nOrbitals - nAOs, data, len, next);
            nboLabels = map.get("beta_" + labelKey);
            // always discarding extra here

            next[0] = (hasNoBeta ? 0 : data.indexOf("beta  spin") + 12);
            // do skips, occupancy here
            // must skip "beta  spin"
          }
          Map<String, Object> mo = orbitals.get(i);
          float[] coefs = new float[nAOs];
          if (isAO) {
            coefs[i % nAOs] = 1;
          } else if (i >= nAOs && hasNoBeta){
            coefs = (float[]) orbitals.get(i % nAOs).get("coefficients");
          } else {
            for (int j = 0; j < nAOs; j++) {
              coefs[j] = PT.parseFloatChecked(data, len, next, false);
              if (Float.isNaN(coefs[j]))
                System.out.println("oops = IsoExt ");
            }
          }
          mo.put("coefficients", coefs);
        }
        if (isNBO)
          getNBOOccupanciesStatic(orbitals, nAOs, nOrbitals - nAOs, data, len, next);
        moData.put(nboType + "_coefs", orbitals);
      }
      moData.put("nboType", nboType);
      moData.put("nboLabels", nboLabels);
      moData.put("mos", orbitals);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  private static void getNBOOccupanciesStatic(Lst<Map<String, Object>> orbitals,
                                              int nAOs, int pt, String data,
                                              int len, int[] next) {
    float[] occupancies = new float[nAOs];
    for (int j = 0; j < nAOs; j++)
      occupancies[j] = PT.parseFloatChecked(data, len, next, false);
    for (int i = 0; i < nAOs; i++) {
      Map<String, Object> mo = orbitals.get(pt + i);
      mo.put("occupancy", Float.valueOf(occupancies[i]));
    }
  }

  private void readMOs() throws Exception {
    
    // .31 AO basis  
    // .32 PNAO no beta coef
    // .33 NAO no beta coef ; adds orbital types
    // .34 PNHO alpha+beta
    // .35 NHO alpha+beta
    // .36 PNBO alpha+beta
    // .37 NBO alpha+extra+beta+extra
    // .38 PNLMO alpha+beta
    // .39 NLMO alpha+extra+beta+extra
    // .40 MO alpha+beta
    // .41 NO alpha+beta
    // .42 (density matrix)
    // .46 (labels)
    // .47 (coords)
    
    boolean isAO = nboType.equals("AO");
    boolean isNBO = nboType.equals("NBO");
    boolean discardExtra = PT.isOneOf(nboType, ";NBO;NLMO;");
    boolean hasNoBeta = PT.isOneOf(nboType, ";AO;PNAO;NAO;");
    nOrbitals0 = orbitals.size();
    // get the labels
    getFile46();
    if (betaOnly) {
      discardLinesUntilContains("BETA");
      filterMO();
    }
    //boolean isNBO = !isAO && !nboType.equals("MO");
    nOrbitals = orbitals.size();
    if (nOrbitals == 0)
      return;
    line = null;
//    if (!isNBO)
//      nOrbitals = nOrbitals0 + nAOs;
//  
    for (int pt = 0, i = nOrbitals0, n = nOrbitals0 + nOrbitals; i < n; i++, pt++) {
      if (pt == nAOs && isNBO)
        getNBOOccupancies(pt);
      if (pt == nAOs && discardExtra)
        discardLinesUntilContains("BETA");        
      Map<String, Object> mo = orbitals.get(i);
      float[] coefs = new float[nAOs];
      if (isAO) {
        coefs[pt % nAOs] = 1;
      } else if (pt >= nAOs && hasNoBeta){
        coefs = (float[]) orbitals.get(pt % nAOs).get("coefficients");
      } else {
        if (line == null) {
          while (rd() != null && Float.isNaN(parseFloatStr(line))) {
            filterMO(); //switch a/b
          }
        } else {
          line = null;
        }
        fillFloatArray(line, 0, coefs);
        line = null;
      }
      mo.put("coefficients", coefs);
    }
    if (isNBO && nAOs != nOrbitals)
      getNBOOccupancies(nOrbitals);
    moData.put(nboType + "_coefs", orbitals);
    setMOData(false);
    moData.put("nboType", nboType);
    Logger.info((orbitals.size() - nOrbitals0) + " orbitals read");

  }

  private void getNBOOccupancies(int pt) throws Exception {
    float[] occupancies = new float[nAOs];
    fillFloatArray(null, 0, occupancies);
    for (int i = 0; i < nAOs; i++) {
      Map<String, Object> mo = orbitals.get(nOrbitals0 + pt - nAOs + i);
      mo.put("occupancy", Float.valueOf(occupancies[i]));
    }
  }

  public static void setNboLabels(String[] tokens, int nLabels,
                           Lst<Map<String, Object>> orbitals, int nOrbitals0,
                           String moType) {
    boolean alphaBeta = (orbitals.size() == nLabels * 2);
    boolean addOccupancy = !PT.isOneOf(moType, ";AO;NAO;PNAO;MO;NO;");
    String ab = (!alphaBeta ? "" : nOrbitals0 == 0 ? " alpha" : " beta");
    for (int j = 0; j < nLabels; j++) {
      Map<String, Object> mo = orbitals.get(j + nOrbitals0);
      String type = tokens[j];
      mo.put("type", moType + " " + type + ab);
      if (addOccupancy)
        mo.put(
            "occupancy",
            Float.valueOf(alphaBeta ? 1 : type.indexOf("*") >= 0
                || type.indexOf("(ry)") >= 0 ? 0 : 2));
    }
  }

}
