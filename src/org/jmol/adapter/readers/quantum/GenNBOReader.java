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
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Map;
import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.Rdr;
import javajs.util.SB;
import org.jmol.adapter.readers.quantum.MOReader;
import org.jmol.adapter.readers.quantum.NBOParser;
import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollection;
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
//*********************************** NBO 6.0 ***********************************
//            N A T U R A L   A T O M I C   O R B I T A L   A N D
//         N A T U R A L   B O N D   O R B I T A L   A N A L Y S I S
//**************************** Robert Hanson (100634) ***************************
// (c) Copyright 1996-2014 Board of Regents of the University of Wisconsin System
//     on behalf of the Theoretical Chemistry Institute.  All rights reserved.
//
//         Cite this program as:
//
//         NBO 6.0.  E. D. Glendening, J. K. Badenhoop, A. E. Reed,
//         J. E. Carpenter, J. A. Bohmann, C. M. Morales, C. R. Landis,
//         and F. Weinhold (Theoretical Chemistry Institute, University
//         of Wisconsin, Madison, WI, 2013); http://nbo6.chem.wisc.edu/
//
//      /NLMO   / : Form natural localized molecular orbitals
//      /NRT    / : Natural Resonance Theory Analysis
//      /AOPNAO / : Print the AO to PNAO transformation
//      /SAO    / : Print the AO overlap matrix
//      /STERIC / : Print NBO/NLMO steric analysis
//      /CMO    / : Print analysis of canonical MOs
//      /PLOT   / : Write information for the orbital plotter
//      /FILE   / : Set to co2_p
//
//Filename set to co2_p
  
    private boolean isOutputFile;
    private String nboType = "";
    private int nOrbitals0;
    private boolean is47File;
    private boolean isOpenShell;
    private boolean alphaOnly;
    private boolean betaOnly;
    private int nAOs,nNOs;
    private String topoType = "A";
    private int nStructures = 0;
    NBOParser nboParser;
   
    @Override
    protected void initializeReader() throws Exception {
      /**
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
        boolean isOK;
        String line1 = this.rd().trim().toUpperCase();
        this.is47File = line1.indexOf("$GENNBO") >= 0 || line1.indexOf("$NBO") >= 0;
        
        if (this.is47File) {
          if (line1.indexOf("BOHR") >= 0) {
            fileOffset = new P3();
            fileScaling = P3.new3(ANGSTROMS_PER_BOHR,ANGSTROMS_PER_BOHR,ANGSTROMS_PER_BOHR);
          }
            this.readData47();
            return;
        }
        
        this.alphaOnly = this.is47File || this.checkFilterKey("ALPHA");
        boolean bl = this.betaOnly = !this.is47File && this.checkFilterKey("BETA");
        
        String line2 = this.rd();
        this.line = line1 + line2;
        boolean bl2 = this.isOutputFile = line2.indexOf("****") >= 0;
        if (this.isOutputFile) {
            // this may or may not work.
            isOK = this.getFile31();
            super.initializeReader();
         // keep going -- we need to read the file using MOReader
            this.moData.put("isNormalized", Boolean.TRUE);
        } else if (line2.indexOf("s in the AO basis:") >= 0) {
            this.nboType = line2.substring(1, line2.indexOf("s"));
            this.asc.setCollectionName(line1 + ": " + this.nboType + "s");
            isOK = this.getFile31();
        } else {//if (line.indexOf("Basis set information") >= 0) {
            this.nboType = "AO";
            this.asc.setCollectionName(line1 + ": " + this.nboType + "s");
            isOK = this.readData31(line1);
        }
        if (!isOK) {
            Logger.error((String)("Unimplemented shell type -- no orbitals available: " + this.line));
        }
        if (this.isOutputFile) {
            return;
        }
        if (isOK) {
            this.readMOs();
        }
        this.continuing = false;
    }
    
    //  $GENNBO  NATOMS=7  NBAS=28  UPPER  BODM  FORMAT  $END
    //  $NBO  $END
    //  $COORD
    //  Methylamine...RHF/3-21G//Pople-Gordon geometry
    //      6    6       0.745914       0.011106       0.000000
    //      7    7      -0.721743      -0.071848       0.000000
    //      1    1       1.042059       1.060105       0.000000
    //      1    1       1.129298      -0.483355       0.892539
    //      1    1       1.129298      -0.483355      -0.892539
    //      1    1      -1.076988       0.386322      -0.827032
    //      1    1      -1.076988       0.386322       0.827032
    //  $END
    @Override
    protected void finalizeSubclassReader() throws Exception {
        this.appendLoadNote("NBO type " + this.nboType);
        if (this.isOpenShell) {
            this.asc.setCurrentModelInfo("isOpenShell", (Object)Boolean.TRUE);
        }
        this.finalizeReaderASCR();
    }

    private void readMOs() throws Exception {
        boolean isAO;
        this.nOrbitals0 = this.orbitals.size();
        // get the labels
        this.getFile46();
        if (this.betaOnly) {
            this.discardLinesUntilContains("BETA");
            this.filterMO();
        }
        boolean isNBO = !(isAO = this.nboType.equals("AO")) && !this.nboType.equals("MO");
        this.nOrbitals = this.orbitals.size();
        if (this.nOrbitals == 0) {
            return;
        }
        this.line = null;
        if (!isNBO) {
            this.nOrbitals = this.nOrbitals0 + this.nAOs;
        }
        int pt = 0;
        boolean discardExtra = PT.isOneOf(nboType, ";NBO;NLMO;");
        boolean hasNoBeta = PT.isOneOf(nboType, ";AO;PNAO;NAO;");
        for (int i = nOrbitals0, n = nOrbitals0 + nOrbitals; i < n; i++, pt++) {
          if (pt == nNOs) {
            if (isNBO) {
              readNBO37Occupancies(pt);
            }
            if (discardExtra)
              discardLinesUntilContains2("BETA", "beta");
          }
          Map<String, Object> mo = orbitals.get(i);
          float[] coefs = new float[nAOs];
          if (isAO) {
            coefs[pt % nAOs] = 1;
          } else if (pt >= nNOs && hasNoBeta){
            coefs = (float[]) orbitals.get(pt % nNOs).get("coefficients");
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
        if (isNBO)
          readNBO37Occupancies(pt);
        moData.put(nboType + "_coefs", orbitals);
        setMOData(false);
        moData.put("nboType", nboType);
        Logger.info((orbitals.size() - nOrbitals0) + " orbitals read");
    }
    
    /**
     * Read occupancies from .37 file. Called by readMOs.
     * @param pt
     * @throws Exception
     */
    private void readNBO37Occupancies(int pt) throws Exception {
      float[] occupancies = new float[nNOs];
      fillFloatArray(null, 0, occupancies);
      for (int i = 0; i < nNOs; i++) {
        Map<String, Object> mo = orbitals.get(nOrbitals0 + pt - nNOs + i);
        mo.put("occupancy", Float.valueOf(occupancies[i]));
      }
    }
    
    @Override
    protected boolean checkLine() throws Exception {
        // for .nbo only
        if (this.line.indexOf("SECOND ORDER PERTURBATION THEORY ANALYSIS") >= 0 && !this.orbitalsRead) {
          // Frank Weinhold suggests that NBO/.37 is not the best choice for a default.
          // PNBOs (pre-NBOs) are not orthogonalized and so "look better." But we are already
          // reading NBOs, and they are fine as well. I'd rather not change this
          // default and risk changes in PNGJ files already saved. 
            this.nboType = "NBO";
            String data = this.getFileData(".37");
            if (data == null) {
                return true;
            }
            BufferedReader readerSave = this.reader;
            this.reader = Rdr.getBR((String)data);
            this.rd();
            this.rd();
            this.readMOs();
            this.reader = readerSave;
            this.orbitalsRead = false;
            return true;
        }
        if (this.line.indexOf("$NRTSTRA") >= 0) {
            this.getStructures("NRTSTRA");
            return true;
        }
        if (this.line.indexOf("$NRTSTRB") >= 0) {
            this.getStructures("NRTSTRB");
            return true;
        }
        if (this.line.indexOf("$NRTSTR") >= 0) {
            this.getStructures("NRTSTR");
            return true;
        }
        if (this.line.indexOf(" TOPO ") >= 0) {
            this.getStructures("TOPO" + this.topoType);
            this.topoType = "B";
            return true;
        }
        if (this.line.indexOf("$CHOOSE") >= 0) {
            this.getStructures("CHOOSE");
            return true;
        }
        return this.checkNboLine();
    }

    private void getStructures(String type) throws Exception {
        if (this.nboParser == null) {
            this.nboParser = new NBOParser();
        }
        Lst<Object> structures = this.getStructureList();
        SB sb = new SB();
        while (!this.rd().trim().equals("$END")) {
            sb.append(this.line).append("\n");
        }
        this.nStructures = this.nboParser.getStructures(sb.toString(), type, structures);
        this.appendLoadNote("" + this.nStructures + " NBO " + type + " resonance structures");
    }
    
    @SuppressWarnings("unchecked")
    private Lst<Object> getStructureList() {
        Lst structures = (Lst)this.asc.getAtomSetAuxiliaryInfo(this.asc.iSet).get("nboStructures");
        if (structures == null) {
            structures = new Lst();
            this.asc.setCurrentModelInfo("nboStructures", (Object)structures);
        }
        return structures;
    }

    private String getFileData(String ext) throws Exception {
        String fileName = (String)this.htParams.get("fullPathName");
        int pt = fileName.lastIndexOf(".");
        if (pt < 0) {
            pt = fileName.length();
        }
        fileName = fileName.substring(0, pt);
        this.moData.put("nboRoot", fileName);
        fileName = fileName + ext;
        String data = this.vwr.getFileAsString3(fileName, false, null);
        Logger.info((String)("" + data.length() + " bytes read from " + fileName));
        boolean isError = (data.indexOf("java.io.") >= 0);
        if (data.length() == 0 || isError && this.nboType != "AO") {
            throw new Exception(" supplemental file " + fileName + " was not found");
        }
        return (isError? null:data);
    }
    
    /**
     * 14_a Basis set information needed for plotting orbitals
     * ---------------------------------------------------------------------------
     * 36 90 162
     * ---------------------------------------------------------------------------
     * 6 -2.992884000 -1.750577000 1.960024000 
     * 6 -2.378528000 -1.339374000 0.620578000
     */
    private boolean getFile31() throws Exception {
      try
      {
        String data = this.getFileData(".31");
        if(data==null)
          return false;
        BufferedReader readerSave = this.reader;
        this.reader = Rdr.getBR((String)data);
        return this.readData31(null) && (this.reader = readerSave) != null;
      }
      catch(Exception e)
      {
        return false;
      }
    }
    
    /**
     * read the labels from xxxx.46
     * 
     * @throws Exception
     */
    private void getFile46() throws Exception {
        String data = this.getFileData(".46");
        if(data==null)
          return;
        BufferedReader readerSave = this.reader;
        this.reader = Rdr.getBR((String)data);
        this.readData46();
        this.reader = readerSave;
    }
    
    private static String P_LIST = "101   102   103";
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
    //        as 2z3-3x2z-3y2z
    //               4xz2-x3-xy2
    //                   4yz2-x2y-y3
    //                           x2z-y2z
    //                               xyz
    //                                  x3-3xy2
    //                                     3x2y-y3
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
        this.allowNoOrbitals = true;
        this.discardLinesUntilContains("$COORD");
        this.asc.newAtomSet();
        this.asc.setAtomSetName(this.rd().trim());
        while (this.rd().indexOf("$END") < 0) {
            String[] tokens = this.getTokens();
            this.addAtomXYZSymName((String[])tokens, (int)2, (String)null, (String)null).elementNumber = (short)this.parseIntStr(tokens[0]);
        }
        this.discardLinesUntilContains("$BASIS");
        int[] centers = this.getIntData();
        int[] labels = this.getIntData();
        this.discardLinesUntilContains("NSHELL =");
        this.shellCount = this.parseIntAt(this.line, 10);
        this.gaussianCount = this.parseIntAt(this.rd(), 10);
        this.rd();
        int[] ncomp = this.getIntData();
        int[] nprim = this.getIntData();
        int[] nptr = this.getIntData();
        // read basis functions
        this.shells = new Lst();
        this.gaussians = AU.newFloat2((int)this.gaussianCount);
        for (int i = 0; i < this.gaussianCount; ++i) {
            this.gaussians[i] = new float[6];
        }
        this.nOrbitals = 0;
        int ptCenter = 0;
        String l = this.line;
        for (int i = 0; i < this.shellCount; ++i) {
            int[] slater = new int[4];
            int nc = ncomp[i];
            slater[0] = centers[ptCenter];
            this.line = "";
            for (int ii = 0; ii < nc; ++ii) {
                this.line = this.line + labels[ptCenter++] + " ";
            }
            if (this.fillSlater(slater, nc, nptr[i] - 1, nprim[i])) continue;
            return;
        }
        this.line = l;
        this.getAlphasAndExponents();
        this.nboType = "AO";
        this.readMOs();
        this.continuing = false;
    }

    private int[] getIntData() throws Exception {
        while (this.line.indexOf("=") < 0) {
            this.rd();
        }
        String s = this.line.substring(this.line.indexOf("=") + 1);
        this.line = "";
        while (this.rd().indexOf("=") < 0 && this.line.indexOf("$") < 0) {
            s = s + this.line;
        }
        String[] tokens = PT.getTokens((String)s);
        int[] f = new int[tokens.length];
        int i = f.length;
        while (--i >= 0) {
            f[i] = this.parseIntStr(tokens[i]);
        }
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
        for (int j = 0; j < 5; ++j) {
            if (this.line.indexOf("=") < 0) {
                this.rd();
            }
            if (this.line.indexOf("$END") >= 0) break;
            this.line = this.line.substring(this.line.indexOf("=") + 1);
            float[] temp = this.fillFloatArray(this.line, 0, new float[this.gaussianCount]);
            for (int i = 0; i < this.gaussianCount; ++i) {
                this.gaussians[i][j] = temp[i];
                if (j <= 1) continue;
                float[] arrf = this.gaussians[i];
                arrf[5] = arrf[5] + temp[i];
            }
        }
        // GenNBO lists S, P, D, F, G orbital coefficients separately
        // we need all of them in [1] if [1] is zero (not S or SP)
        for (int i = 0; i < this.gaussianCount; ++i) {
            if (this.gaussians[i][1] != 0.0f) continue;
            this.gaussians[i][1] = this.gaussians[i][5];
        }
        if (this.debugging) {
            Logger.debug((String)("" + this.shells.size() + " slater shells read"));
            Logger.debug((String)("" + this.gaussians.length + " gaussian primitives read"));
        }
    }

    private boolean readData31(String line1) throws Exception {
        int i;
        if (line1 == null) {
            line1 = this.rd();
            this.rd();
        }
        this.rd(); // ----------
        // read ac, shellCount, and gaussianCount
        String[] tokens = PT.getTokens((String)this.rd());
        int ac = this.parseIntStr(tokens[0]);
        this.shellCount = this.parseIntStr(tokens[1]);
        this.gaussianCount = this.parseIntStr(tokens[2]);
        
        // read atom types and positions
        this.rd();  // ----------
        this.asc.newAtomSet();
        this.asc.setAtomSetName(this.nboType + "s: " + line1.trim());
        this.asc.setCurrentModelInfo("nboType", (Object)this.nboType);
        for (i = 0; i < ac; ++i) {
            tokens = PT.getTokens((String)this.rd());
            int z = this.parseIntStr(tokens[0]);
            // dummy atom
            if (z < 0) continue;
            Atom atom = this.asc.addNewAtom();
            atom.elementNumber = (short)z;
            this.setAtomCoordTokens(atom, tokens, 1);
        }
        // read basis functions
        this.shells = new Lst();
        this.gaussians = AU.newFloat2((int)this.gaussianCount);
        for (i = 0; i < this.gaussianCount; ++i) {
            this.gaussians[i] = new float[6];
        }
        this.rd();  // ----------
        this.nOrbitals = 0;
        for (i = 0; i < this.shellCount; ++i) {
            tokens = PT.getTokens((String)this.rd());
            int[] slater = new int[4];
            slater[0] = this.parseIntStr(tokens[0]); // atom pointer; 1-based
            int n = this.parseIntStr(tokens[1]);
            int pt = this.parseIntStr(tokens[2]) - 1; // gaussian list pointer
            int ng = this.parseIntStr(tokens[3]); // number of gaussians
            this.line = this.rd().trim();
            for (int j = (n - 1) / 10; --j >= 0;)
              line += rd().substring(1);
            this.line = this.line.trim();
            System.out.println(line);
            if (this.fillSlater(slater, n, pt, ng)) continue;
            return false;
        }
        this.rd();
        this.getAlphasAndExponents();
        return true;
    }
    
    private boolean addBetaSet;
    /**
     * read labels and not proper number of NOs, nNOs, for this nboType
     * 
     * @throws Exception
     */
    private void readData46() throws Exception {
      Map<String, String[]> map = new Hashtable<String, String[]>();
      String[] tokens = new String[0];
      rd();
      int nNOs = this.nNOs = nAOs = nOrbitals;
      String labelKey = getLabelKey(nboType);
      while (line != null && line.length() > 0) {
        tokens = PT.getTokens(line);
        String type = tokens[0];
        isOpenShell = (tokens.length == 3);
        String ab = (isOpenShell ? tokens[1] : "");
        String count = tokens[tokens.length - 1];
        String key = (ab.equals("BETA") ? "beta_" : "") + type;
        if (parseIntStr(count) != nOrbitals) {
          Logger.error("file 46 number of orbitals for " + line + " (" + count + ") does not match nOrbitals: "
              + nOrbitals + "\n");
          nNOs = parseIntStr(count); 
        }
        if (type.equals(labelKey))
          this.nNOs = nNOs;
        SB sb = new SB();
        while (rd() != null && line.length() > 4 && " NA NB AO NH".indexOf(line.substring(1, 4)) < 0)
          sb.append(line.substring(1));
        System.out.println(sb.length());
        tokens = new String[sb.length() / 10];
        for (int i = 0, pt = 0; i < tokens.length; i++, pt += 10)
          tokens[i] = PT.rep(sb.substring2(pt, pt + 10), " ","");
        map.put(key, tokens);
      }
      tokens = map.get((betaOnly ? "beta_" : "") + labelKey);
      moData.put("nboLabelMap", map);
      if (tokens == null) {
        tokens = new String[nNOs];
        for (int i = 0; i < nNOs; i++)
          tokens[i] = nboType + (i + 1);
        map.put(labelKey, tokens);
        if (isOpenShell)
          map.put("beta_" + labelKey, tokens);        
      }
      moData.put("nboLabels", tokens);
      addBetaSet = (isOpenShell && !betaOnly && !is47File); 
      if (addBetaSet) 
        nOrbitals *= 2;
      for (int i = 0; i < nOrbitals; i++)
        setMO(new Hashtable<String, Object>());
      setNboLabels(tokens, nNOs, orbitals, nOrbitals0, nboType);
      if (addBetaSet) {
        moData.put("firstBeta", Integer.valueOf(nNOs));
        setNboLabels( map.get("beta_" + labelKey), nNOs, orbitals, nOrbitals0 + nNOs, nboType);
      }
      Lst<Object> structures = getStructureList();
      NBOParser.getStructures46(map.get("NBO"), "alpha", structures, asc.ac);
      NBOParser.getStructures46(map.get("beta_NBO"), "beta", structures, asc.ac);
      
    }

    public static boolean readNBOCoefficients(Map<String, Object> moData, String nboType, Viewer vwr) {
        int ext = ";AO;  ;PNAO;;NAO; ;PNHO;;NHO; ;PNBO;;NBO; ;PNLMO;NLMO;;MO;  ;NO;".indexOf(";" + nboType + ";");
        ext = ext / 6 + 31;
        boolean isAO = nboType.equals("AO");
        boolean isNBO = nboType.equals("NBO");
        
        boolean hasNoBeta = PT.isOneOf((String)nboType, (String)";AO;PNAO;NAO;");
        Map map = (Map)moData.get("nboLabelMap");
        int nAOs = ((String[])map.get("AO")).length;
        String labelKey = GenNBOReader.getLabelKey(nboType);
        String[] nboLabels = (String[])map.get(labelKey);
        if (nboLabels == null) {
            nboLabels = new String[nAOs];
            for (int i = 0; i < nAOs; ++i) {
                nboLabels[i] = nboType + (i + 1);
            }
            labelKey = nboType;
            map.put(labelKey, nboLabels);
            if (!hasNoBeta) {
                map.put("beta_" + labelKey, nboLabels);
            }
        }
        int nMOs = nboLabels.length;
        try {
            Lst orbitals = (Lst)moData.get(nboType + "_coefs");
            if (orbitals == null) {
                Map mo;
                String data = null;
                if (!isAO) {
                    String fileName = moData.get("nboRoot") + "." + ext;
                    data = vwr.getFileAsString3(fileName, true, null);
                    if (data == null) {
                        return false;
                    }
                    data = data.substring(data.indexOf("--\n") + 3).toLowerCase();
                    if (ext == 33) {
                        data = data.substring(0, data.indexOf("--\n") + 3);
                    }
                }
                orbitals = (Lst)moData.get("mos");
                Object dfCoefMaps = ((Map)orbitals.get(0)).get("dfCoefMaps");
                orbitals = new Lst();
                int len = 0;
                int[] next = null;
                int nOrbitals = nMOs;
                if (!isAO) {
                    if (data.indexOf("alpha") >= 0) {
                        nOrbitals *= 2;
                        data = data.substring(data.indexOf("alpha") + 10);
                    }
                    len = data.length();
                    next = new int[1];
                }
                int i = nOrbitals;
                while (--i >= 0) {
                    mo = new Hashtable();
                    orbitals.addLast(mo);
                    if (dfCoefMaps == null) continue;
                    mo.put((String)"dfCoefMaps", dfCoefMaps);
                }
                GenNBOReader.setNboLabels(nboLabels, nMOs, orbitals, 0, nboType);
                for (i = 0; i < nOrbitals; ++i) {
                    if (!isAO && i == nMOs) {
                        if (isNBO) {
                            GenNBOReader.getNBOOccupanciesStatic(orbitals, nMOs, 0, data, len, next);
                        }
                        nboLabels = (String[])map.get("beta_" + labelKey);
                        next[0] = hasNoBeta ? 0 : data.indexOf("beta  spin") + 12;
                    }
                    mo = (Map)orbitals.get(i);
                    float[] coefs = new float[nAOs];
                    if (isAO) {
                        coefs[i % nAOs] = 1.0f;
                    } else if (i >= nAOs && hasNoBeta) {
                        coefs = (float[])((Map)orbitals.get(i % nAOs)).get("coefficients");
                    } else {
                        for (int j = 0; j < nAOs; ++j) {
                            coefs[j] = PT.parseFloatChecked((String)data, (int)len, (int[])next, (boolean)false);
                            if (!Float.isNaN(coefs[j])) continue;
                            System.out.println("oops = IsoExt ");
                        }
                    }
                    mo.put("coefficients", coefs);
                }
                if (isNBO) {
                    GenNBOReader.getNBOOccupanciesStatic(orbitals, nMOs, nOrbitals - nMOs, data, len, next);
                }
                moData.put(nboType + "_coefs", (Object)orbitals);
            }
            moData.put("nboType", nboType);
            moData.put("nboLabels", nboLabels);
            moData.put("mos", (Object)orbitals);
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static void getNBOOccupanciesStatic(Lst<Map<String, Object>> orbitals, int nAOs, int pt, String data, int len, int[] next) {
        float[] occupancies = new float[nAOs];
        for (int j = 0; j < nAOs; ++j) {
            occupancies[j] = PT.parseFloatChecked((String)data, (int)len, (int[])next, (boolean)false);
        }
        for (int i = 0; i < nAOs; ++i) {
            Map mo = (Map)orbitals.get(pt + i);
            mo.put("occupancy", Float.valueOf(occupancies[i]));
        }
    }

    public static void setNboLabels(String[] tokens, int nLabels, Lst<Map<String, Object>> orbitals, int nOrbitals0, String moType) {
        boolean addOccupancy;
        boolean alphaBeta = orbitals.size() == nLabels * 2;
        boolean bl = addOccupancy = !PT.isOneOf((String)moType, (String)";AO;NAO;PNAO;MO;NO;");
        String ab = !alphaBeta ? "" : (nOrbitals0 == 0 ? " alpha" : " beta");
        for (int j = 0; j < nLabels; ++j) {
            Map mo = (Map)orbitals.get(j + nOrbitals0);
            String type = tokens[j];
            mo.put("type", moType + " " + type + ab);
            if (!addOccupancy) continue;
            mo.put("occupancy", Float.valueOf(alphaBeta ? 1.0f : (type.indexOf("*") >= 0 || type.indexOf("(ry)") >= 0 ? 0.0f : 2.0f)));
        }
    }

    private static String getLabelKey(String labelKey) {
        if (labelKey.startsWith("P")) {
            labelKey = labelKey.substring(1);
        }
        if (labelKey.equals("NLMO")) {
            labelKey = "NBO";
        }
        if (labelKey.equals("MO")) {
            labelKey = "NO";
        }
        return labelKey;
    }
}