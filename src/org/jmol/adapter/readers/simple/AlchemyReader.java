/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-10 10:36:58 -0500 (Sun, 10 Sep 2006) $
 * $Revision: 5478 $
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

import javajs.util.PT;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Atom;
import org.jmol.api.JmolAdapter;

/**
 * TRIPOS simple Alchemy reader. 
 * atomtypes based on OpenBabel data/types.txt
 * 
 * Somewhat flexible - for 3DNA compatibility. See
 * 
 *   http://rutchem.rutgers.edu/~xiangjun/3DNA/jmol/3dna_bp.alc)
 * 
 * Bob Hanson 12/2008
 * 
 * 
 * also serves as MD3 reader
 * 
 */

public class AlchemyReader extends AtomSetCollectionReader {

  protected boolean isM3D;
  
  private int ac;
  private int bondCount;

  @Override
  public void initializeReader() throws Exception {
    asc.newAtomSet();
    rd();
    if (line.indexOf("ATOMS") < 0) {
      isM3D = true;
      rd();
    }
    String[] tokens = getTokens();
    ac = parseIntStr(tokens[0]);
    bondCount = parseIntStr(tokens[isM3D ? 1 : 2]);
    readAtoms();
    readBonds();
    continuing = false;
 }

  /*
   * 
   11 ATOMS,    12 BONDS,     0 CHARGES
   1 C3     -2.4790   5.3460   0.0000     0.0000
   2 NPL3   -1.2910   4.4980   0.0000     0.0000
   3 C2      0.0240   4.8970   0.0000     0.0000
   4 NPL3    0.8770   3.9020   0.0000     0.0000
   5 C2      0.0710   2.7710   0.0000     0.0000
   6 C2      0.3690   1.3980   0.0000     0.0000
   7 NPL3    1.6110   0.9090   0.0000     0.0000
   8 NPL3   -0.6680   0.5320   0.0000     0.0000
   9 C2     -1.9120   1.0230   0.0000     0.0000
   10 NPL3   -2.3200   2.2900   0.0000     0.0000
   11 C2     -1.2670   3.1240   0.0000     0.0000
   1     1     2  SINGLE
   2     2     3  SINGLE
   3     2    11  SINGLE
   4     3     4  DOUBLE
   5     4     5  SINGLE
   6     5     6  DOUBLE
   7     5    11  SINGLE
   8     6     7  SINGLE
   9     6     8  SINGLE
   10     8     9  DOUBLE
   11     9    10  SINGLE
   12    10    11  DOUBLE
   
   or  3DNA: 
   
   12 ATOMS,    12 BONDS
   1 N      -2.2500   5.0000   0.2500
   2 N      -2.2500  -5.0000   0.2500
   3 N      -2.2500  -5.0000  -0.2500
   4 N      -2.2500   5.0000  -0.2500
   5 C       2.2500   5.0000   0.2500
   6 C       2.2500  -5.0000   0.2500
   7 C       2.2500  -5.0000  -0.2500
   8 C       2.2500   5.0000  -0.2500
   9 C      -2.2500   5.0000   0.2500
   10 C      -2.2500  -5.0000   0.2500
   11 C      -2.2500  -5.0000  -0.2500
   12 C      -2.2500   5.0000  -0.2500
   1     1     2
   2     2     3
   3     3     4
   4     4     1
   5     5     6
   6     6     7
   7     7     8
   8     5     8
   9     9     5
   10    10     6
   11    11     7
   12    12     8

   */
  
  private void readAtoms() throws Exception {
    int pt = (isM3D ? 3 : 2);
    for (int i = ac; --i >= 0;) {
      String[] tokens = PT.getTokens(rd());
      Atom atom = new Atom();
      atom.atomSerial = parseIntStr(tokens[0]);
      String name = tokens[1];
      if (!isM3D) {
        atom.atomName = name;
        atom.elementSymbol = name.substring(0, 1);
        char c1 = name.charAt(0);
        char c2 = ' ';
        // any name > 2 characters -- just read first character
        // any name = 2 characters -- check for known atom or "Du"
        int nChar = (name.length() == 2
            && (Atom.isValidSym2(c1,
                c2 = Character.toLowerCase(name.charAt(1))) || name
                .equals("Du")) ? 2 : 1);
        name = (nChar == 1 ? "" + c1 : "" + c1 + c2);
      }
      atom.elementSymbol = name;
      setAtomCoordTokens(atom, tokens, pt);
      atom.partialCharge = (tokens.length >= 6 ? parseFloatStr(tokens[pt + 3]) : 0);
      asc.addAtomWithMappedSerialNumber(atom);
    }
  }

  private void readBonds() throws Exception {
    for (int i = bondCount; --i >= 0;) {
      String[] tokens = PT.getTokens(rd());
      String atomSerial1 = tokens[1];
      String atomSerial2 = tokens[2];
      String sOrder = (tokens.length < 4 ? "1" : tokens[3].toUpperCase());
      int order = 0;
      switch (sOrder.charAt(0)) {
      default:
      case '1':
      case 'S':
        order = JmolAdapter.ORDER_COVALENT_SINGLE;
        break;
      case '2':
      case 'D':
        order = JmolAdapter.ORDER_COVALENT_DOUBLE;
        break;
      case '3':
      case 'T':
        order = JmolAdapter.ORDER_COVALENT_TRIPLE;
        break;
      case 'A':
        order = JmolAdapter.ORDER_AROMATIC;
        break;
      case 'H':
        order = JmolAdapter.ORDER_HBOND;
        break;
      }
      asc.addNewBondFromNames(atomSerial1, atomSerial2, order);
    }
  }
}
