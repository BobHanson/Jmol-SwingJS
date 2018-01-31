/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-07-14 15:35:47 -0500 (Fri, 14 Jul 2006) $
 * $Revision: 5303 $
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
import org.jmol.adapter.smarter.Atom;

import org.jmol.api.JmolAdapter;

/**
 * Reads Ghemical (<a href="http://www.uku.fi/~thassine/ghemical/">
 * http://www.uku.fi/~thassine/ghemical</a>)
 * molecular mechanics (*.mm1gp) files.
 * <code>
 * !Header mm1gp 100
 * !Info 1
 * !Atoms 6
 * 0 6 
 * 1 6 
 * 2 1 
 * 3 1 
 * 4 1 
 * 5 1 
 * !Bonds 5
 * 1 0 D 
 * 2 0 S 
 * 3 0 S 
 * 4 1 S 
 * 5 1 S 
 * !Coord
 * 0 0.06677 -0.00197151 4.968e-07 
 * 1 -0.0667699 0.00197154 -5.19252e-07 
 * 2 0.118917 -0.097636 2.03406e-06 
 * 3 0.124471 0.0904495 -4.84021e-07 
 * 4 -0.118917 0.0976359 -2.04017e-06 
 * 5 -0.124471 -0.0904493 5.12591e-07 
 * !Charges
 * 0 -0.2
 * 1 -0.2
 * 2 0.1
 * 3 0.1
 * 4 0.1
 * 5 0.1
 * !End
 * </code>
 *
 * @author Egon Willighagen <egonw@sci.kun.nl>
 */
public class GhemicalMMReader extends AtomSetCollectionReader {
    
  @Override
  protected boolean checkLine() throws Exception {
    if (line.startsWith("!Header")) {
      processHeader();
      return true;
    }
    if (line.startsWith("!Info")) {
      processInfo();
      return true;
    }
    if (line.startsWith("!Atoms")) {
      processAtoms();
      return true;
    }
    if (line.startsWith("!Bonds")) {
      processBonds();
      return true;
    }
    if (line.startsWith("!Coord")) {
      processCoord();
      return true;
    }
    if (line.startsWith("!Charges")) {
      processCharges();
      return true;
    }
    return true;
  }

  void processHeader() {
  }

  void processInfo() {
  }

  void processAtoms() throws Exception {
    int ac = parseIntAt(line, 6);
    //Logger.debug("ac=" + ac);
    for (int i = 0; i < ac; ++i) {
      if (asc.ac != i)
        throw new Exception("GhemicalMMReader error #1");
      rd();
      int atomIndex = parseIntStr(line);
      if (atomIndex != i)
        throw new Exception("bad atom index in !Atoms" +
                            "expected: " + i + " saw:" + atomIndex);
      int elementNumber = parseInt();
      Atom atom = asc.addNewAtom();
      atom.elementNumber = (short)elementNumber;
    }
  }

  void processBonds() throws Exception {
    int bondCount = parseIntAt(line, 6);
    for (int i = 0; i < bondCount; ++i) {
      rd();
      int atomIndex1 = parseIntStr(line);
      int atomIndex2 = parseInt();
      String orderCode = parseToken();
      int order = 0;
      //bug found using FindBugs -- was not considering changes in bond order numbers
      switch(orderCode.charAt(0)) {
      case 'C': // Conjugated (aromatic)
        order = JmolAdapter.ORDER_AROMATIC;
        break;
      case 'T':
        order = JmolAdapter.ORDER_COVALENT_TRIPLE;
        break;
      case 'D':
        order = JmolAdapter.ORDER_COVALENT_DOUBLE;
        break;
      case 'S':
      default:
        order = JmolAdapter.ORDER_COVALENT_SINGLE;
      }
      asc.addNewBondWithOrder(atomIndex1, atomIndex2, order);
    }
  }

  void processCoord() throws Exception {
    Atom[] atoms = asc.atoms;
    int ac = asc.ac;
    for (int i = 0; i < ac; ++i)
      setAtomCoordScaled(atoms[i], PT.getTokens(rd()), 1, 10);
  }

  void processCharges() throws Exception {
    Atom[] atoms = asc.atoms;
    int ac = asc.ac;
    for (int i = 0; i < ac; ++i) {
      rd();
      int atomIndex = parseIntStr(line);
      if (atomIndex != i)
        throw new Exception("bad atom index in !Charges" +
                            "expected: " + i + " saw:" + atomIndex);
      atoms[i].partialCharge = parseFloat();
    }
  }
}
