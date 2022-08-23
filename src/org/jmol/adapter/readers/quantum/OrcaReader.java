/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-08-27 21:07:49 -0500 (Sun, 27 Aug 2006) $
 * $Revision: 5420 $
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
package org.jmol.adapter.readers.quantum;


import java.util.Hashtable;
import java.util.Map;

import org.jmol.adapter.smarter.Atom;
import org.jmol.quantum.QS;
import org.jmol.util.Escape;
import org.jmol.util.Logger;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;

/**
 * Reads ORCA input and output files
 *
 */
public class OrcaReader extends MOReader {

  private String chargeTag;
  private int atomCount;
  private boolean xyzBohr;
  private int moModelSet;

  @Override
  protected void initializeReader() throws Exception {
    chargeTag = (checkAndRemoveFilterKey("CHARGE=LOW") ? "LOEW" : "MULL");
  }

  @Override
  protected boolean checkLine() throws Exception {
    if (line.startsWith("! Bohrs")) {
      xyzBohr = true;
      return true;
    }
    if (line.startsWith("* xyz") || line.startsWith("*xyz")) {
      processInputFile();
      continuing = false;
      return false;
    }
    if (line.indexOf("CARTESIAN COORDINATES (ANG") >= 0) {
      processAtoms();
      return true;
    }
    if (line.indexOf("ATOMIC CHARGES") >= 0 && line.indexOf(chargeTag) >= 0) {
      processAtomicCharges();
      return true;
    }
    if (line.startsWith("Total Energy")) {
      processEnergyLine();
      return true;
    }
    if (line.indexOf("BASIS SET IN INPUT FORMAT") == 0) {
      processBasis();
    }
    if (line.trim().equals("MOLECULAR ORBITALS")) {
      processMolecularOrbitals();
    }
    return true;
  }

// Total Energy       :          -76.36038546 Eh           -2077.87173 eV
    
  private void processEnergyLine() {
    String[] tokens = getTokens();
    asc.setAtomSetEnergy(tokens[3], Double.parseDouble(tokens[3]));
  }


  
//  # Simple Calculation
//  ! B3LYP def2-SVP opt 
//
//  * xyz 0 1
//
//  C         -2.48753        2.50320        0.45149
//  O         -1.37907        2.85206        0.73571
//  O         -3.59614        2.15450        0.16756
//  *

  private void processInputFile() throws Exception {
    while (rd() != null) {
      while (line.trim().length() == 0 || line.startsWith("#")) {
        rd();
      }
      if (line.indexOf("*") >= 0)
        break;
      String[] tokens = getTokens();
      Atom a = addAtomXYZSymName(tokens, 1, tokens[0], null);
      if (xyzBohr)
        a.scale(ANGSTROMS_PER_BOHR);
    }
  }

  //  CARTESIAN COORDINATES (ANGSTROEM)
  //  ---------------------------------
  //    N     -1.547600    0.308690    0.000000
  //    H     -0.527600    0.308690    0.000000
  //    H     -1.887600   -0.336090   -0.713490
  //    H     -1.887600    1.248970   -0.201650

  void processAtoms() throws Exception {
    modelNumber++;
    if (!doGetModel(modelNumber, null))
      return;
    asc.newAtomSet();
    baseAtomIndex = asc.ac;
    rd(); // -------------
    while (rd() != null) {
      String[] tokens = getTokens();
      if (tokens.length != 4)
        break;
      addAtomXYZSymName(tokens, 1, tokens[0], null);
    }
    if (baseAtomIndex == 0)
      atomCount = asc.ac;
  }

  //-----------------------
  //MULLIKEN ATOMIC CHARGES
  //-----------------------
  //   0 N :   -0.267065
  //   1 H :    0.089379
  //   2 H :    0.089033
  //   3 H :    0.088654
  //Sum of atomic charges:    0.0000000

  //LOEWDIN ATOMIC CHARGES
  //----------------------
  //   0 N :   -0.159838
  //   1 H :    0.053364
  //   2 H :    0.053340
  //   3 H :    0.053134

  void processAtomicCharges() throws Exception {
    rd();
    for (int i = 0; i < atomCount; i++) {
      rd();
      asc.atoms[i + baseAtomIndex].partialCharge = Double
          .parseDouble(line.substring(line.indexOf(":") + 1));
    }
  }

  //  -------------------------------
  //  AUXILIARY BASIS SET INFORMATION
  //  -------------------------------
  //  There are 2 groups of distinct atoms
  //
  //   Group   1 Type O   : 12s5p4d2f1g contracted to 6s4p3d1f1g pattern {711111/2111/211/2/1}
  //   Group   2 Type H   : 5s2p1d contracted to 3s1p1d pattern {311/2/1}
  //
  //  Atom   0O    basis set group =>   1
  //  Atom   1H    basis set group =>   2
  //  Atom   2H    basis set group =>   2
  //
  //  -----------------------------------
  //  AUXILIARY BASIS SET IN INPUT FORMAT
  //  -----------------------------------
  //
  //   # Auxiliary basis set for element : H 
  //   NewAuxGTO H 
  //   S 3 
  //     1      15.6752927000      0.1007184237
  //     2       3.6063578000      0.3404257499
  //     3       1.2080016000      0.6491996172
  //   S 1 
  //     1       0.4726794000      1.0000000000
  //   S 1 
  //     1       0.2018100000      1.0000000000
  //   P 2 
  //     1       2.0281365000      0.5596620919
  //     2       0.5358730000      0.5596620919
  //   D 1 
  //     1       2.2165124000      1.0000000000
  //    end;
  //
  private void processBasis() throws Exception {
    if (shells != null)
      return;
    shells = new  Lst<int[]>();
    Lst<String[]> gdata = new Lst<String[]>();

    boolean doSphericalF = true, doSphericalD = true;  // ? 
    calculationType = "5D7F"; // ?
    
    Map<String, Lst<String[]>> basisLines = new Hashtable<String, Lst<String[]>>();
    rd();
    
    while (discardLinesUntilContains2("#", "-----").indexOf("#") >= 0) {
      String element = line.substring(line.indexOf(":") + 1).trim();
      Lst<String[]> lines = new Lst<String[]>();
      basisLines.put(element,  lines);
      rd();
      while (rd().indexOf("end;") < 0) {
        if (line.length() > 10)
          line = line.substring(4); // skip index on gaussian line
        lines.addLast(getTokens());
      }
    }

    //  S 3 
    //    1      15.6752927000      0.1007184237
    //    2       3.6063578000      0.3404257499
    //    3       1.2080016000      0.6491996172
    //  S 1 
    //    1       0.4726794000      1.0000000000
    //  S 1 
    //    1       0.2018100000      1.0000000000
    //  P 2 
    //    1       2.0281365000      0.5596620919
    //    2       0.5358730000      0.5596620919
    //  D 1 
    //    1       2.2165124000      1.0000000000

    for (int ac = 0; ac < atomCount; ac++) {
      Lst<String[]> lines = basisLines.get(asc.atoms[ac].elementSymbol);
      for (int j = 0; j < lines.size();) {
        String[] tokens = lines.get(j++);
        shellCount++;
        int[] slater = new int[4];
        slater[0] = ac + 1;
        String oType = tokens[0];
        if (doSphericalF && oType.indexOf("F") >= 0
            || doSphericalD && oType.indexOf("D") >= 0)
          slater[1] = BasisFunctionReader.getQuantumShellTagIDSpherical(oType);
        else
          slater[1] = BasisFunctionReader.getQuantumShellTagID(oType);

        int nGaussians = parseIntStr(tokens[1]);
        slater[2] = gaussianCount + 1; // or parseInt(tokens[7])
        slater[3] = nGaussians;
        if (debugging)
          Logger.debug("Slater " + shells.size() + " " + Escape.eAI(slater));
        shells.addLast(slater);
        gaussianCount += nGaussians;
        for (int i = 0; i < nGaussians; i++) {
          tokens = lines.get(j++);
          if (debugging)
            Logger
                .debug("Gaussians " + (i + 1) + " " + Escape.eAS(tokens, true));
          gdata.addLast(tokens);
        }
      }
    }
    gaussians = AU.newDouble2(gaussianCount);
    for (int i = 0; i < gaussianCount; i++) {
      String[] tokens = gdata.get(i);
      gaussians[i] = new double[tokens.length];
      for (int j = 0; j < tokens.length; j++)
        gaussians[i][j] = parseDoubleStr(tokens[j]);
    }
    Logger.info(shellCount + " slater shells read");
    Logger.info(gaussianCount + " gaussian primitives read");
  }

//  ------------------
//  MOLECULAR ORBITALS
//  ------------------
//                        0         1         2         3         4         5   
//                   -18.75649  -0.88651  -0.46852  -0.29897  -0.23062   0.03125
//                     2.00000   2.00000   2.00000   2.00000   2.00000   0.00000
//                    --------  --------  --------  --------  --------  --------
//    0O   1s         0.988667  0.276495 -0.000052 -0.109450 -0.000142  0.112677
//    0O   2s        -0.040154  0.551582 -0.000072 -0.215647 -0.000273  0.208186
//    0O   3s         0.014488  0.264791 -0.000120 -0.355545 -0.000390  0.910843
//    0O   1pz       -0.001944  0.081875 -0.234942  0.364091  0.373345  0.172906
//    0O   1px       -0.001697  0.071503  0.410331  0.317584  0.000634  0.149709
//    0O   1py       -0.001408  0.059231 -0.170031  0.264432 -0.514361  0.125934
//    0O   2pz        0.000045  0.004964 -0.090187  0.236855  0.289093  0.211428
//    0O   2px        0.000038  0.004074  0.158193  0.208582  0.000093  0.181044
//    0O   2py        0.000033  0.003627 -0.065232  0.172655 -0.399112  0.152738
//    0O   1dz2       0.000165 -0.000331 -0.011144  0.002751  0.012835  0.001386
//    0O   1dxz       0.000580 -0.000950  0.005623  0.012048  0.006499  0.005722
//    0O   1dyz      -0.000943  0.005511 -0.009325  0.010295 -0.004904  0.004642
//    0O   1dx2y2    -0.000904  0.004175  0.013190  0.001900  0.007385  0.000600
//    0O   1dxy       0.000420 -0.000714  0.004074  0.008787 -0.008972  0.004113
//    1H   1s         0.002519  0.201346  0.333263  0.188938  0.000585 -0.152228
//    1H   2s        -0.002454  0.014310  0.100167  0.067086 -0.000213 -0.784160
//    1H   1pz       -0.000026  0.006148 -0.009444  0.021537  0.018322  0.004870
//    1H   1px       -0.003374 -0.032789 -0.025691 -0.007750 -0.000079 -0.013472
//    1H   1py       -0.000019  0.004435 -0.006851  0.015565 -0.025143  0.003594
//    2H   1s         0.002518  0.201043 -0.333152  0.190099  0.000600 -0.152926

  private void processMolecularOrbitals() throws Exception {
    if (shells == null)
      return;
    Map<String, Object>[] mos = AU.createArrayOfHashtable(6);
    Lst<String>[] data = AU.createArrayOfArrayList(6);
    int nThisLine = 0;
    rd();
    Lst<String> labels = new Lst<String>();
    while (rd() != null && line.indexOf("----") < 0) {
      if (line.length() == 0)
        continue;
      String[] tokens;
      if (line.startsWith("          ")) {
        addMODataOR(nThisLine, labels, data, mos);
        labels.clear();
        rd(); // energy line
        tokens = getTokens();
        nThisLine = tokens.length;
        for (int i = 0; i < nThisLine; i++) {
          mos[i] = new Hashtable<String, Object>();
          data[i] = new Lst<String>();
          mos[i].put("energy", Double.valueOf(tokens[i]));          
        }
        rd(); // occupancy line
        tokens = getTokens();
        for (int i = 0; i < nThisLine; i++) {
          mos[i].put("occupancy", Double.valueOf(tokens[i]));          
        }
        rd(); // ---- 
        continue;
      } 
      try {
        tokens = getTokens();
        String type = tokens[tokens.length - nThisLine - 1].substring(1).toUpperCase();
        labels.addLast(type);
        if (PT.isDigit(type.charAt(0)))
          type = type.substring(1); // "1S"
        if (!QS.isQuantumBasisSupported(type.charAt(0))
            && "XYZ".indexOf(type.charAt(0)) >= 0)
          type = (type.length() == 2 ? "D" : "F") + type;
        if (!QS.isQuantumBasisSupported(type.charAt(0)))
          continue;
        tokens = getStrings(line.substring(line.length() - 10 * nThisLine),
            nThisLine, 10);
        for (int i = 0; i < nThisLine; i++)
          data[i].addLast(tokens[i]);
      } catch (Exception e) {
        Logger.error("Error reading Gaussian file Molecular Orbitals at line: "
            + line);
        break;
      }
    }
    addMODataOR(nThisLine, labels, data, mos);
    setMOData(moModelSet != asc.atomSetCount);
    moModelSet = asc.atomSetCount;    
  }

  private void addMODataOR(int nThisLine, Lst<String> labels, Lst<String>[] data,
                           Map<String, Object>[] mos) {
    if (labels.size() == 0)
      return;
    for (int i = 0; i < labels.size(); i++) {
      if (labels.get(i).equals("PZ")) {
        // Switch PX and PZ
      for (int j = 0; j < nThisLine; j++) {
        Lst<String> d = data[j];
        String s = d.removeItemAt(i);
        d.add(i+2, s);
      }
        //
      }
    }
    addMOData(nThisLine, data, mos);
  }


}

