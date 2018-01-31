/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-28 23:13:00 -0500 (Thu, 28 Sep 2006) $
 * $Revision: 5772 $
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

import javajs.util.PT;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Bond;
import org.jmol.adapter.smarter.Atom;

import org.jmol.api.JmolAdapter;

public class JmeReader extends AtomSetCollectionReader {
  /*
   * see http://www.molinspiration.com/jme/doc/jme_functions.html
   * 
   * recognized simply as a file with a single line and a digit as the first
   * character.
   * 
   * the format of the JME String is as follows natoms nbonds (atomic_symbol
   * x_coord y_coord) for all atoms (atom1 atom2 bond_order) for all bonds (for
   * stereo bonds the bond order is -1 for up and -2 for down from the first to
   * the second atom) Molecules in multipart system are separated by the |
   * character. Components of the reaction are separated by the > character. The
   * JME string for the reaction is thus "reactant1 | reactant 2 ... >
   * modulator(s) > product(s)"
   * 
   * Which, unfortunately, is not much to go on.
   * 
   * But the really interesting thing is that Jmol 12.0 can do minimization of
   * the 2D structure to generate a 3D equivalent.
   * 
   * The load command FILTER keyword NOMIN prevents this minimization. Note that
   * with jmolLoadInline you must explicitly add the script
   * 
   * minimize silent addHydrogens
   * 
   * to get the 2D -> 3D conversion after the file is loaded.
   * 
   * Bob Hanson hansonr@stolaf.edu 4/11/2010
   */

  @Override
  public void initializeReader() throws Exception {
    asc.setCollectionName("JME");
    asc.newAtomSet();
    line = rd().replace('\t', ' ');
    checkCurrentLineForScript();
    addJmolScript("jmeString='" + line + "'");
    int ac = parseInt();
    int bondCount = parseInt();
    readAtoms(ac);
    readBonds(bondCount);
    set2D();
    continuing = false;
  }

  private void readAtoms(int ac) throws Exception {
    for (int i = 0; i < ac; ++i) {
      String strAtom = parseToken();
      Atom atom = asc.addNewAtom();
      setAtomCoordXYZ(atom, parseFloat(), parseFloat(), 0);
      int indexColon = strAtom.indexOf(':');
      String elementSymbol = (indexColon > 0 ? strAtom.substring(0, indexColon)
          : strAtom);
      if (elementSymbol.indexOf("+") >= 0) {
        elementSymbol = PT.trim(elementSymbol, "+");
        atom.formalCharge = 1;
      } else if (elementSymbol.indexOf("-") >= 0) {
        elementSymbol = PT.trim(elementSymbol, "-");
        atom.formalCharge = -1;
      }
      atom.elementSymbol = elementSymbol;
    }
    /*
    if (!doMinimize)
      return;
    Atom[] atoms = asc.atoms;
    for (int i = 0; i < ac; i++) {
      atoms[i].z += ((i % 2) == 0 ? 0.05f : -0.05f);
    }
    */
  }

  private void readBonds(int bondCount) throws Exception {
    for (int i = 0; i < bondCount; ++i) {
      int atomIndex1 = parseInt() - 1;
      int atomIndex2 = parseInt() - 1;
      int order = parseInt();
      switch (order) {
      default:
        continue;
      case 1:
      case 2:
      case 3:
        break;
      case -1:
        order = JmolAdapter.ORDER_STEREO_NEAR;
        break;
      case -2:
        order = JmolAdapter.ORDER_STEREO_FAR;
        break;
      }
      asc.addBond(new Bond(atomIndex1, atomIndex2, order));
    }
  }
}
