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
package org.jmol.adapter.readers.simple;


import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollectionReader;

/**
 * Reads ORCA input and output files
 *
 */
public class OrcaReader extends AtomSetCollectionReader {

  private String chargeTag;
  private int atomCount;
  private boolean xyzBohr;

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
      processCoordinates();
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

  void processCoordinates() throws Exception {
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

}

