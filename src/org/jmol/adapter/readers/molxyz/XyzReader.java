/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-22 14:12:46 -0500 (Sun, 22 Oct 2006) $
 * $Revision: 5999 $
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

package org.jmol.adapter.readers.molxyz;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Atom;

import org.jmol.util.Logger;

/**
 * Minnesota SuperComputer Center XYZ file format
 * 
 * simple symmetry extension via load command:
 * 9/2006 hansonr@stolaf.edu
 * 
 *  setAtomCoord(atom)
 *  applySymmetryAndSetTrajectory()
 *  
 *  extended to read XYZI files (Bob's invention -- allows isotope numbers)
 * 
 * extended to read XYZ files with fractional charges as, for example:
 * 
 * http://www.ccl.net/cca/software/SOURCES/FORTRAN/molden/test/reacpth.xyz
 * 
 * http://web.archive.org/web/20000120031517/www.msc.edu/msc/docs/xmol/v1.3/g94toxyz.c
 * 
 * 
 * 
 */

public class XyzReader extends AtomSetCollectionReader {

  @Override
  protected boolean checkLine() throws Exception {
    int modelAtomCount = parseIntStr(line);
    if (modelAtomCount == Integer.MIN_VALUE) {
      continuing = false;
      return false;
    }

    // models and vibrations are the same for XYZ files
    vibrationNumber = ++modelNumber;
    if (desiredVibrationNumber <= 0 ? doGetModel(modelNumber, null)
        : doGetVibration(vibrationNumber)) {
      rd();
      checkCurrentLineForScript();
      asc.newAtomSet();
      String name = line.trim();
      readAtoms(modelAtomCount);
      applySymmetryAndSetTrajectory();
      asc.setAtomSetName(name);
      if (isLastModel(modelNumber)) {
        continuing = false;
        return false;
      }
    } else {
      skipAtomSet(modelAtomCount);
    }
    discardLinesUntilNonBlank();
    return false;
  }

  @Override
  protected void finalizeSubclassReader() throws Exception {
    isTrajectory = false;
    finalizeReaderASCR();
  }

  private void skipAtomSet(int modelAtomCount) throws Exception {
    rd(); //comment
    for (int i = modelAtomCount; --i >= 0;)
      rd(); //atoms
  }

  private void readAtoms(int modelAtomCount) throws Exception {
    for (int i = 0; i < modelAtomCount; ++i) {
      rd();
      String[] tokens = getTokens();
      if (tokens.length < 4) {
        Logger.warn("line cannot be read for XYZ atom data: " + line);
        continue;
      }
      Atom atom = addAtomXYZSymName(tokens, 1, null, null);
      setElementAndIsotope(atom, tokens[0]);
      int vpt = 4;
      switch (tokens.length) {
      case 4:
        continue;
      case 5:
      case 6:
      case 8:
      case 9:
        // accepts  sym x y z c
        // accepts  sym x y z c r
        // accepts  sym x y z c vx vy vz
        // accepts  sym x y z c vx vy vz atomno
        if (tokens[4].indexOf(".") >= 0) {
          atom.partialCharge = parseFloatStr(tokens[4]);
        } else {
          int charge = parseIntStr(tokens[4]);
          if (charge != Integer.MIN_VALUE)
            atom.formalCharge = charge;
        }
        switch (tokens.length) {
        case 5:
          continue;
        case 6:
          atom.radius = parseFloatStr(tokens[5]);
          continue;
        case 9:
          atom.atomSerial = parseIntStr(tokens[8]);
        }
        vpt++;
        //$FALL-THROUGH$:
      default:
        // or       sym x y z vx vy vz
        float vx = parseFloatStr(tokens[vpt++]);
        float vy = parseFloatStr(tokens[vpt++]);
        float vz = parseFloatStr(tokens[vpt++]);
        if (Float.isNaN(vx) || Float.isNaN(vy) || Float.isNaN(vz))
          continue;
        asc.addVibrationVector(atom.index, vx, vy, vz);
      }
    }
  }

}
