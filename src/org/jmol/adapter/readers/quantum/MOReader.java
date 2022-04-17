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

import java.util.Hashtable;
import java.util.Map;

import org.jmol.adapter.smarter.Atom;
import org.jmol.quantum.QS;
import org.jmol.util.Logger;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;

/**
 * General methods for reading molecular orbital data, including embedded output
 * from the NBO program. In particular, when the AONBO keyword is included.
 *
 *
 * requires the following sort of construct: <code> 
  public AtomSetCollection readAtomSetCollection(BufferedReader reader) {
    readAtomSetCollection(reader, "some type");
  }
  
  protected boolean checkLine() {
    if (line.indexOf(...)) {
      doThis();
      return true/false;
    } 
    if (line.indexOf(...)) {
      doThat();
      return true/false;
    } 
    return checkNboLine();
  }
 * </code>
 *
 **/

public abstract class MOReader extends BasisFunctionReader {
    
//NBO output analysis is based on
//
//*********************************** NBO 5.G ***********************************
//           N A T U R A L   A T O M I C   O R B I T A L   A N D
//        N A T U R A L   B O N D   O R B I T A L   A N A L Y S I S
//*******************************************************************************
//(c) Copyright 1996-2004 Board of Regents of the University of Wisconsin System
//    on behalf of the Theoretical Chemistry Institute.  All Rights Reserved.
//
//        Cite this program as:
//
//        NBO 5.G.  E. D. Glendening, J. K. Badenhoop, A. E. Reed,
//        J. E. Carpenter, J. A. Bohmann, C. M. Morales, and F. Weinhold
//        (Theoretical Chemistry Institute, University of Wisconsin,
//        Madison, WI, 2001); http://www.chem.wisc.edu/~nbo5
//
//     /AONBO  / : Print the AO to NBO transformation
//
//

  public int shellCount = 0;
  public int gaussianCount = 0;
  public float[][] gaussians;
  protected String energyUnits = "";
  
  protected Lst<String> moTypes;
  private boolean getNBOs;
  private boolean getNBOCharges;
  protected boolean haveNboCharges;
  protected boolean haveNboOrbitals;
  protected boolean orbitalsRead;

  protected Map<String, Object> lastMoData;
  protected boolean allowNoOrbitals;
  protected boolean forceMOPAC;
  
  final protected int HEADER_GAMESS_UK_MO = 3;
  final protected int HEADER_GAMESS_OCCUPANCIES = 2;
  final protected int HEADER_GAMESS_ORIGINAL = 1;
  final protected int HEADER_NONE = 0;
  
  @Override
  protected void initializeReader() throws Exception {
    line = "\nNBOs";
    getNBOs = (filter != null && filterMO());
    line = "\nNBOCHARGES";
    getNBOCharges = (filter != null && filterMO());
    checkAndRemoveFilterKey("NBOCHARGES");
    forceMOPAC = checkAndRemoveFilterKey("MOPAC");
  }
  
  /**
   * 
   * @return true if need to read line
   * @throws Exception
   */
  protected boolean checkNboLine() throws Exception {

    // these output lines are being passed on by NBO 5.0 to whatever program is
    // using it (GAMESS, GAUSSIAN)
    if (getNBOs) {
      if (line.indexOf("(Occupancy)   Bond orbital/ Coefficients/ Hybrids") >= 0) {
        getNboTypes();
        return false;
      }
      if (line.indexOf("NBOs in the AO basis:") >= 0) {
        readMolecularOrbitals(HEADER_NONE);
        return false;
      }
      if (line.indexOf(" SECOND ORDER PERTURBATION THEORY ANALYSIS") >= 0) {
        readSecondOrderData();
        return false;
      }
    }
    if (getNBOCharges && line.indexOf("Summary of Natural Population Analysis:") >= 0) {
      getNboCharges();
      return true;
    }
    return true;
  }
  
  /*
 Summary of Natural Population Analysis:

                                     Natural Population                 Natural
             Natural    ---------------------------------------------    Spin
  Atom No    Charge        Core      Valence    Rydberg      Total      Density
 ------------------------------------------------------------------------------
    O  1   -0.25759      1.99984     6.23673    0.02102     8.25759     0.28689
    N  2    0.51518      1.99975     4.42031    0.06476     6.48482     0.42622
    O  3   -0.25759      1.99984     6.23673    0.02102     8.25759     0.28689
 ===============================================================================

   */
  
  private void getNboCharges() throws Exception {
    if (haveNboCharges)
      return; // don't use alpha/beta spin charges
    discardLinesUntilContains("----");
    discardLinesUntilContains("----");
    haveNboCharges = true;
    int ac = asc.ac;
    int i0 = asc.getLastAtomSetAtomIndex();
    Atom[] atoms = asc.atoms;
    for (int i = i0; i < ac; ++i) {
      // first skip over the dummy atoms
      while (atoms[i].elementNumber == 0)
        ++i;
      // assign the partial charge
      String[] tokens = PT.getTokens(rd());
      double charge;
      if (tokens == null || tokens.length < 3 || Double.isNaN(charge = parseDoubleStr(tokens[2]))) {
        Logger.info("Error reading NBO charges: " + line);
        return;
      }
      atoms[i].partialCharge = charge;      
      if (debugging)
        Logger.debug("Atom " + i + " using NBOcharge: " + charge);
    }
    Logger.info("Using NBO charges for Model " + asc.atomSetCount);
  }
  
  
  /*
   * 
     (Occupancy)   Bond orbital/ Coefficients/ Hybrids
 -------------------------------------------------------------------------------
   1. (1.98839) BD ( 1) C  1- C  2       
               ( 51.09%)   0.7148* C  1 s( 26.14%)p 2.82( 73.68%)d 0.01(  0.18%)
                                        -0.0003 -0.5112 -0.0085  0.0025  0.8344
                                         0.0173 -0.1824 -0.0055 -0.0841  0.0006
                                         0.0133  0.0063 -0.0009 -0.0349  0.0180
               ( 48.91%)   0.6994* C  2 s( 38.95%)p 1.56( 60.89%)d 0.00(  0.16%)
                                         0.0002 -0.6233  0.0310 -0.0067 -0.7603
                                         0.0081  0.1752 -0.0039 -0.0028  0.0014
                                         0.0235  0.0042  0.0002 -0.0285  0.0140

   */
  protected void getNboTypes() throws Exception {
    moTypes = new  Lst<String>();
    iMo0 = (orbitals == null ? 0 : orbitals.size()) + 1;
    rd();
    rd();
    int n = 0;
    int pt = 0;
    while (line != null && (pt = line.indexOf(".")) >= 0 && pt < 10) {
      if (parseIntRange(line, 0, pt) != n + 1)
        break;
      moTypes.add(n++, line.substring(pt + 1, Math.min(40, line.length())).trim());
      while (rd() != null && line.startsWith("       ")) {
      }
    }
    Logger.info(n + " natural bond AO basis functions found");
  }

  private boolean haveCoeffMap;
  
  /*
   * 
   * GAMESS:
   * 
   ------------------
   MOLECULAR ORBITALS
   ------------------

          ------------
          EIGENVECTORS
          ------------

                      1          2          3          4          5
                  -79.9156   -20.4669   -20.4579   -20.4496   -20.4419
                     A          A          A          A          A   
    1  C  1  S   -0.000003  -0.000029  -0.000004   0.000011   0.000016
    2  C  1  S   -0.000009   0.000140   0.000001   0.000057   0.000065
    3  C  1  X    0.000007  -0.000241  -0.000022  -0.000010  -0.000061
    4  C  1  Y   -0.000008   0.000017  -0.000027  -0.000010   0.000024
    5  C  1  Z    0.000007   0.000313   0.000009  -0.000002  -0.000001
    6  C  1  S    0.000049   0.000875  -0.000164  -0.000521  -0.000440
    7  C  1  X   -0.000066   0.000161   0.000125   0.000034   0.000406
    8  C  1  Y    0.000042   0.000195  -0.000165  -0.000254  -0.000573
    9  C  1  Z    0.000003   0.000045   0.000052   0.000112  -0.000129
   10  C  1 XX   -0.000010   0.000010  -0.000040   0.000019   0.000045
   11  C  1 YY   -0.000010  -0.000031   0.000000  -0.000003   0.000019
  ...

                      6          7          8          9         10
                  -20.4354   -20.4324   -20.3459   -20.3360   -11.2242
                     A          A          A          A          A   
    1  C  1  S    0.000000  -0.000001   0.000001   0.000000   0.008876
    2  C  1  S   -0.000003   0.000002   0.000003   0.000002   0.000370

  ...
  TOTAL NUMBER OF BASIS SET SHELLS             =  101
   *
   * NBO: --- note, "-" can be in column with " " causing tokenization failure
   * 
          AO         1       2       3       4       5       6       7       8
      ---------- ------- ------- ------- ------- ------- ------- ------- -------
   1.  C 1 (s)    0.0364  0.0407 -0.0424  0.0428  0.0056 -0.0009  0.0052  0.0018
   2.  C 1 (s)   -0.1978 -0.1875  0.1959 -0.1992 -0.0159  0.0054 -0.0130  0.0084

   */
  
  private static final String P_LIST =  "(PX)  (PY)  (PZ)";
  private static final String DS_LIST = "(D5)  (D2)  (D3)  (D4)  (D1)";
  private static final String DC_LIST = "(D1)  (D4)  (D6)  (D2)  (D3)  (D5)";
  // DC_LIST is inferred from the order in NBO 5.G as well as the fact that for GenNBO
  // we have:   201 dxx, 202 dxy, 203 dxz, 204 dyy, 205 dyz, 206 dzz
  // thanks to Justin Shorb for helping straighten that out. BH 7/1/2010
  

  private static final String FS_LIST =  "(F1)  (F2)  (F3)  (F4)  (F5)  (F6)  (F7)";
  
  private static String FC_LIST  =       "(F1)  (F2)  (F10) (F4)  (F2)  (F3)  (F6)  (F9)  (F8)  (F5)";

  // note that higher-L orbitals (G,H,I) are not enabled here.
  
  protected void readMolecularOrbitals(int headerType) throws Exception {
    // GamessUK, GamessUS, and general NBO reader
    if (ignoreMOs) {
      // but for now can override this with FILTER=" LOCALIZED ORBITALS"
      //should read alpha and beta
      rd();
      return;
    }
    addSlaterBasis();
    // reset the coefficient maps
    dfCoefMaps = null;
    // Idea here is to concatenate results from gennbo if desired,
    // and these will replace previous results. 
    // we still need atom positions and bases functions.
    if (haveNboOrbitals) {
      orbitals = new Lst<Map<String, Object>>();
      alphaBeta = "";
    }
    haveNboOrbitals = true;
    orbitalsRead = true;
    Map<String, Object>[] mos = null;
    Lst<String>[] data = null;
    String dCoeffLabels = "";
    String fCoeffLabels = "";
    String pCoeffLabels = "";
    int ptOffset = -1;
    int fieldSize = 0;
    int nThisLine = 0;
    rd();
    int moCount = 0;
    int nBlank = 0;
    boolean haveMOs = false;
    if (line.indexOf("---") >= 0)
      rd();
    while (rd() != null) {
      String[] tokens = getTokens();
      if (debugging) {
        Logger.debug(tokens.length + " --- " + line);
      }
      if (line.indexOf("end") >= 0)
        break;
      if (line.indexOf(" ALPHA SET ") >= 0) {
        alphaBeta = "alpha";
        if (rd() == null)
          break;
      } else if (line.indexOf(" BETA SET ") >= 0) {
        if (haveMOs)
          break;
        alphaBeta = "beta";
        if (rd() == null)
          break;
      }
      //not everyone has followed the conventions for ending a section of output
      String str = line.toUpperCase();
      if (str.length() == 0 || str.indexOf("--") >= 0
          || str.indexOf(".....") >= 0 || str.indexOf("NBO BASIS") >= 0 // reading NBOs
          || str.indexOf("CI EIGENVECTORS WILL BE LABELED") >= 0 //this happens when doing MCSCF optimizations
          || str.indexOf("LZ VALUE") >= 0 //open-shelled 
          || str.indexOf("   THIS LOCALIZATION HAD") >= 0) { //this happens with certain localization methods
        if (!haveCoeffMap) {
          haveCoeffMap = true;
          boolean isOK = true;
          if (pCoeffLabels.length() > 0)
            isOK = getDFMap("P", pCoeffLabels, QS.P, P_LIST, 4);
          if (dCoeffLabels.length() > 0) {
            if (dCoeffLabels.indexOf("X") >= 0)
              isOK = getDFMap("DC", dCoeffLabels, QS.DC, QS.CANONICAL_DC_LIST, 2);
            else if (dCoeffLabels.indexOf("(D6)") >= 0)
              isOK = getDFMap("DC", dCoeffLabels, QS.DC, DC_LIST, 4);
            else
              isOK = getDFMap("DS", dCoeffLabels, QS.DS, DS_LIST, 4);
          }
          if (fCoeffLabels.length() > 0) {
            if (fCoeffLabels.indexOf("X") >= 0)
              isOK = getDFMap("FC", fCoeffLabels, QS.FC, QS.CANONICAL_FC_LIST, 2);
            else if (fCoeffLabels.indexOf("(F10)") >= 0)
              isOK = getDFMap("FC", fCoeffLabels, QS.FC, FC_LIST, 5);
            else
              isOK = getDFMap("FS", fCoeffLabels, QS.FS, FS_LIST, 4);
          }
          if (!isOK) {
            //
          }
        }
        if (str.length() == 0)
          nBlank++;
        else
          nBlank = 0;
        if (nBlank == 2)
          break;
        if (str.indexOf("LZ VALUE") >= 0)
          discardLinesUntilBlank();
        for (int iMo = 0; iMo < nThisLine; iMo++) {
          double[] coefs = new double[data[iMo].size()];
          int iCoeff = 0;
          while (iCoeff < coefs.length) {
            coefs[iCoeff] = parseDoubleStr(data[iMo].get(iCoeff));
            iCoeff++;
          }
          haveMOs = true;
          addCoef(mos[iMo], coefs, null, Double.NaN, Double.NaN, moCount++);
        }
        nThisLine = 0;
        if (line.length() == 0)
          continue;
        break;
      }
      //read the data line:
      nBlank = 0;
      if (nThisLine == 0) {
        nThisLine = tokens.length;
        if (tokens[0].equals("AO")) {
          //01234567890123456789
          // 480. Li31 (s)   -7.3005  1.8135 -9.4655 -0.5137 -5.1614-23.4537-20.3894-37.6613
          nThisLine--;
          ptOffset = 16;
          fieldSize = 8;
          // NBOs
        }
        if (mos == null || nThisLine > mos.length) {
          mos = AU.createArrayOfHashtable(nThisLine);
          data = AU.createArrayOfArrayList(nThisLine);
        }
        for (int i = 0; i < nThisLine; i++) {
          mos[i] = new Hashtable<String, Object>();
          data[i] = new Lst<String>();
        }
        getMOHeader(headerType, tokens, mos, nThisLine);
        continue;
      }
      int nSkip = tokens.length - nThisLine;
      String type = tokens[nSkip - 1];
      char ch;
      if (type.charAt(0) == '(') {
        ch = type.charAt(1);
        if (!haveCoeffMap) {
          switch (ch) {
          case 'p':
            pCoeffLabels += " " + type.toUpperCase();
            break;
          case 'd':
            dCoeffLabels += " "
                + canonicalizeQuantumSubshellTag(type.toUpperCase());
            break;
          case 'f':
            // unchecked
            fCoeffLabels += " "
                + canonicalizeQuantumSubshellTag(type.toUpperCase());
            break;
          case 's':
            // could be sp??? 
          }
        }
      } else {
        int nChar = type.length();
        ch = (nChar < 4 ? 'S' : nChar == 4 ? 'G' : nChar == 5 ? 'H' : '?');
        if (!haveCoeffMap && nChar == 3)
          fCoeffLabels += " "
              + canonicalizeQuantumSubshellTag(type.toUpperCase());
        else if (!haveCoeffMap && nChar == 2)
          dCoeffLabels += " "
              + canonicalizeQuantumSubshellTag(type.toUpperCase());
      }
      if (QS.isQuantumBasisSupported(ch)) {
        if (ptOffset < 0) {
          for (int i = 0; i < nThisLine; i++)
            data[i].addLast(tokens[i + nSkip]);
        } else {
          int pt = ptOffset;
          for (int i = 0; i < nThisLine; i++, pt += fieldSize)
            data[i].addLast(line.substring(pt, pt + fieldSize).trim());
        }
      }
      line = "";
    }
    energyUnits = "a.u.";
    setMOData(!alphaBeta.equals("alpha"));
    // reset the coefficient maps again
    haveCoeffMap = false;
    dfCoefMaps = null;
  }
  

  /**
   * See MopacSlaterReader
   */
  protected void addSlaterBasis() {
  }

  public void addCoef(Map<String, Object> mo, double[] coefs, String type, double energy, double occ, int moCount) {
    mo.put("coefficients", coefs);
    if (moTypes != null) {
      type = moTypes.get(moCount % moTypes.size());
      occ = (type.indexOf("*") >= 0 ? 0 : 2);
    } else if (alphaBeta.length() > 0) {
      type = alphaBeta;
    } 
    if (type != null)
      mo.put("type", type);
    if (!Double.isNaN(energy))
      mo.put("energy", Double.valueOf(energy));
    if (!Double.isNaN(occ))
      mo.put("occupancy", Double.valueOf(occ));
    setMO(mo);
  }

  private int iMo0 = 1;
  
  protected void getMOHeader(int headerType, String[] tokens, Map<String, Object>[] mos, int nThisLine)
      throws Exception {
    rd();
    switch (headerType) {
    default:
    case HEADER_NONE:
      return;
    case HEADER_GAMESS_UK_MO:
      for (int i = 0; i < nThisLine; i++)
        mos[i].put("energy", Double.valueOf(tokens[i]));
      readLines(5);
      return;
    case HEADER_GAMESS_ORIGINAL:
      // this is the original functionality
      tokens = getTokens();
      if (tokens.length == 0)
        tokens = PT.getTokens(rd());
      for (int i = 0; i < nThisLine; i++) {
        mos[i].put("energy", Double.valueOf(tokens[i]));
      }
      rd();
      break;
    case HEADER_GAMESS_OCCUPANCIES:
      // MCSCF NATURAL ORBITALS only have occupancy
      boolean haveSymmetry = (line.length() > 0 || rd() != null);
      tokens = getTokens();
      for (int i = 0; i < nThisLine; i++)
        mos[i].put("occupancy", Double.valueOf(tokens[i].charAt(0) == '-' ? 2.0f
            : parseDoubleStr(tokens[i])));
      rd(); // blank or symmetry
      if (!haveSymmetry)
        return;
      // MCSCF NATURAL ORBITALS (from GUGA) using CSF configurations have
      // occupancy and symmetry
    }
    if (line.length() > 0) {
      tokens = getTokens();
      for (int i = 0; i < nThisLine; i++)
        mos[i].put("symmetry", tokens[i]);
    }
  }

  protected void addMOData(int nColumns, Lst<String>[] data, Map<String, Object>[] mos) {
    for (int i = 0; i < nColumns; i++) {
      double[] coefs = new double[data[i].size()];
      for (int j = coefs.length; --j >= 0;)
        coefs[j] = parseDoubleStr(data[i].get(j));
      mos[i].put("coefficients", coefs);
      setMO(mos[i]);
    }
  }

  public void setMOData(boolean clearOrbitals) {
    if (!allowNoOrbitals && orbitals.size() == 0)
      return;
    if (shells != null && gaussians != null) {
      moData.put("calculationType", calculationType);
      moData.put("energyUnits", energyUnits);
      moData.put("shells", shells);
      moData.put("gaussians", gaussians);
      moData.put("mos", orbitals);
      finalizeMOData(lastMoData = moData);
    }
    if (clearOrbitals) {
      clearOrbitals();
    }
  }
  
  /*
  SECOND ORDER PERTURBATION THEORY ANALYSIS OF FOCK MATRIX IN NBO BASIS

    Threshold for printing:   0.50 kcal/mol
                                                          E(2)  E(j)-E(i) F(i,j)
     Donor NBO (i)              Acceptor NBO (j)       kcal/mol   a.u.    a.u. 
  ===============================================================================

 within unit  1
   1. BD ( 1) C 1- C 2       47. RY*( 1) O 4              1.28    1.86    0.044
   1. BD ( 1) C 1- C 2       66. BD*( 1) C 2- O 4         0.67    1.27    0.026
xxxxxxxxxxxxxxxxxxxxxxxxxx yyyyyyyyyyyyyyyyyyyyyyyyyyy zzzzzzz ....... ffffffff 
0         1         2         3         4         5         6         7         8        
01234567890123456789012345678901234567890123456789012345678901234567890123456789012
   */
  private void readSecondOrderData() throws Exception {

    readLines(5);
    if (lastMoData == null || moTypes == null)
      return;
    Hashtable<String, Integer> ht = new Hashtable<String, Integer>();
    for (int i = moTypes.size(); --i >= 0;)
      ht.put(PT.rep(moTypes.get(i).substring(10), " ", ""),
          Integer.valueOf(i + iMo0));
    Lst<String[]> strSecondOrderData = new  Lst<String[]>();
    while (rd() != null && line.indexOf("NBO") < 0) {
      if (line.length() < 5 || line.charAt(4) != '.')
        continue;
      strSecondOrderData.addLast(new String[] {
          PT.rep(line.substring(5, 27).trim(), " ", ""),
          PT.rep(line.substring(32, 54).trim(), " ", ""),
          line.substring(55, 62).trim(), line.substring(71).trim() });
    }
    double[][] secondOrderData = new double[strSecondOrderData.size()][4];
    lastMoData.put("secondOrderData", secondOrderData);
    lastMoData = null;
    Integer IMO;
    for (int i = strSecondOrderData.size(); --i >= 0;) {
      String[] a = strSecondOrderData.get(i);
      IMO = ht.get(a[0]);
      if (IMO != null)
        secondOrderData[i][0] = IMO.intValue();
      IMO = ht.get(a[1]);
      if (IMO != null)
        secondOrderData[i][1] = IMO.intValue();
      secondOrderData[i][2] = parseDoubleStr(a[2]);
      secondOrderData[i][3] = parseDoubleStr(a[3]);
    }
  }
  
}
