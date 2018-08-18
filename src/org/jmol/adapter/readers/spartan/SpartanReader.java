/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-11 23:56:13 -0500 (Mon, 11 Sep 2006) $
 * $Revision: 5499 $
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

package org.jmol.adapter.readers.spartan;

import org.jmol.adapter.readers.quantum.BasisFunctionReader;
import org.jmol.adapter.smarter.Atom;

import java.util.Hashtable;

/*
 * perhaps unnecessary ? I think this was for when all you had was 
 * a piece of the archive file that started with:
 * Spartan '04 Quantum Mechanics Program:  (PC/x86)           Release  121
 * 
 * but now we can read SMOL files, so this should not be necessary 
 * 
 * no bonds here.
 * 
 */

public class SpartanReader extends BasisFunctionReader {

  @Override
  public void initializeReader() throws Exception {
    String cartesianHeader = "Cartesian Coordinates (Ang";
    if (isSpartanArchive(cartesianHeader)) {
      moData = new Hashtable<String, Object>();
      SpartanArchive spartanArchive = new SpartanArchive(this, "", null, 0);
      int ac = spartanArchive.readArchive(line, true, 0, true);
      if (ac > 0)
        asc.setAtomSetName("Spartan file");
    } else if (line.indexOf(cartesianHeader) >= 0) {
      readAtoms();
      discardLinesUntilContains("Vibrational Frequencies");
      if (line != null)
        readFrequencies();
    }
    continuing = false;
  }

  private boolean isSpartanArchive(String strNotArchive) throws Exception {
    String lastLine = "";
    while (rd() != null) {
      if (line.equals("GEOMETRY")) {
        line = lastLine;
        return true;
      }
      if (line.indexOf(strNotArchive) >= 0)
        return false;
      lastLine = line;
    }
    return false;
  }

  private void readAtoms() throws Exception {
    discardLinesUntilBlank();
    while (rd() != null && (/* atomNum = */parseIntRange(line, 0, 3)) > 0) {
      Atom atom = asc.addNewAtom();
      atom.elementSymbol = parseTokenRange(line, 4, 6);
      atom.atomName = parseTokenRange(line, 7, 13);
      setAtomCoordXYZ(atom, parseFloatRange(line, 17, 30), parseFloatRange(line, 31, 44), parseFloatRange(
          line, 45, 58));
    }
  }

  private void readFrequencies() throws Exception {
    int ac = asc.getAtomSetAtomCount(0);
    while (true) {
      discardLinesUntilNonBlank();
      int lineBaseFreqCount = vibrationNumber;
      next[0] = 16;
      int lineFreqCount;
      boolean[] ignore = new boolean[3];
      for (lineFreqCount = 0; lineFreqCount < 3; ++lineFreqCount) {
        float frequency = parseFloat();
        if (Float.isNaN(frequency))
          break; // ////////////// loop exit is here
        ignore[lineFreqCount] = !doGetVibration(++vibrationNumber);
        if (!ignore[lineFreqCount]) {
          if (vibrationNumber > 1)
            asc.cloneFirstAtomSet(0);
          asc.setAtomSetFrequency(vibrationNumber, null, null, "" + frequency, null);
        }
      }
      if (lineFreqCount == 0)
        return;
      readLines(2);
      for (int i = 0; i < ac; ++i) {
        rd();
        for (int j = 0; j < lineFreqCount; ++j) {
          int ichCoords = j * 23 + 10;
          float x = parseFloatRange(line, ichCoords, ichCoords + 7);
          float y = parseFloatRange(line, ichCoords + 7, ichCoords + 14);
          float z = parseFloatRange(line, ichCoords + 14, ichCoords + 21);
          if (!ignore[j])
            asc.addVibrationVector(i + (lineBaseFreqCount + j)
                * ac, x, y, z);
        }
      }
    }
  }
}
