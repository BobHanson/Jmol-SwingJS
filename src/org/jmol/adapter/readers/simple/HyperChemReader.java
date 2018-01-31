/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
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

package org.jmol.adapter.readers.simple;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Atom;

import org.jmol.api.JmolAdapter;

/**
 * Support for .hin, HyperChem's native file format.
 * http://www.hyper.com
 * <p />
 * Record format is:
 * <code>
 * atom 1 - C ** - -0.06040 0.00000 0.00000 0.00000 3 2 a 6 a 38 s
 * ...
 * atom 67 - H ** - 0.17710 -7.10260 -3.74840 2.24660 1 34 s
 * endmol 1
 * </code>
 * interesting fields are partialCharge, x, y, z, bondCount<br />
 * bonds are atom number and s/d/t/a for single/double/triple/aromatic
 */
public class HyperChemReader extends AtomSetCollectionReader {
  
  @Override
  protected boolean checkLine() throws Exception {
    if (line.length() == 0 || line.charAt(0) == ';') // comment
      return true;
    if (line.startsWith("mol ")) {
      // we have reached the start of a molecule
      if (!doGetModel(++modelNumber, null))
        return checkLastModel();
      processMol();
      return true;
    }
    if (!doProcessLines)
      return true;
    
    if (line.startsWith("atom ")) {
      processAtom();
      return true;
    }
    if (line.startsWith("endmol ")) {
      applySymmetryAndSetTrajectory();
      return true;
    }
    return true;
  }
  
  private int atomIndex;

  private void processMol() throws Exception {
    asc.newAtomSet();
    String molName = getMolName();
    asc.setAtomSetName(molName);
    atomIndex = 0;
    baseAtomIndex = asc.ac;
  }

  private String getMolName() {
    parseTokenStr(line);
    parseToken();
    return parseToken();
  }

  private void processAtom() throws Exception {

    int fileAtomNumber = parseIntAt(line, 5);
    if (fileAtomNumber - 1 != atomIndex) {
      throw new Exception ("bad atom number sequence ... expected:" +
        (atomIndex + 1) + " found:" + fileAtomNumber);
    }
    Atom atom = asc.addNewAtom();
    parseToken(); // discard
    atom.elementSymbol = parseToken();
    parseToken(); // discard
    parseToken(); // discard
    atom.partialCharge = parseFloat();
    setAtomCoordXYZ(atom, parseFloat(), parseFloat(), parseFloat());
    
    int bondCount = parseInt();
    for (int i = 0; i < bondCount; ++i) {
      int otherAtomNumber = parseInt();
      String bondTypeToken = parseToken();
      if (otherAtomNumber > atomIndex)
        continue;
      int bondOrder;
      switch(bondTypeToken.charAt(0)) {
      case 's': 
        bondOrder = 1;
        break;
      case 'd': 
        bondOrder = 2;
        break;
      case 't': 
        bondOrder = 3;
        break;      
      case 'a':
        bondOrder = JmolAdapter.ORDER_AROMATIC;
        break;
      default:
        throw new Exception ("unrecognized bond type:" + bondTypeToken +
          " atom #" + fileAtomNumber);
      }
      asc.addNewBondWithOrder(baseAtomIndex + atomIndex,
                       baseAtomIndex + otherAtomNumber - 1,
                       bondOrder);
    }
    ++atomIndex;
  }

}
