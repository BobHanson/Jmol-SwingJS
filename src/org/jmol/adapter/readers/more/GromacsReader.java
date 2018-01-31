/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-15 17:34:01 -0500 (Sun, 15 Oct 2006) $
 * $Revision: 5957 $
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

package org.jmol.adapter.readers.more;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Atom;


import org.jmol.util.Logger;
import javajs.util.P3;

public class GromacsReader extends AtomSetCollectionReader {
  
  @Override
  protected void initializeReader() {
    setIsPDB();
    asc.newAtomSet();
    setModelPDB(true);
  }
  
  @Override
  protected boolean checkLine() throws Exception {
      checkCurrentLineForScript();
      asc.setAtomSetName(line.trim());
      readAtoms();
      readUnitCell();
      continuing = false;
      return false;
  }

   /*

"Check Your Input" (D. van der Spoel)
   59
    1TYR      N    1   1.521   1.583   2.009  0.0000  0.0000  0.0000
    1TYR     H1    2   1.421   1.595   2.009  0.0000  0.0000  0.0000
    1TYR     H2    3   1.548   1.533   1.927  0.0000  0.0000  0.0000
    1TYR     CA    4   1.585   1.713   2.009  0.0000  0.0000  0.0000
    1TYR     HA    5   1.555   1.760   1.926  0.0000  0.0000  0.0000
    1TYR     CB    6   1.541   1.791   2.133  0.0000  0.0000  0.0000

   */
  private void readAtoms() throws Exception {
    int modelAtomCount = parseIntStr(rd());
    for (int i = 0; i < modelAtomCount; ++i) {
      rd();
      int len = line.length();
      if (len != 44 && len != 68) {
        Logger.warn("line cannot be read for GROMACS atom data: " + line);
        continue;
      }
      Atom atom = new Atom();
      atom.sequenceNumber = parseIntRange(line, 0, 5);
      setAtomName(atom, parseTokenRange(line, 5, 9).trim(), line.substring(11, 15).trim());
      atom.atomSerial = parseIntRange(line, 15, 20);
      atom.x = parseFloatRange(line, 20, 28) * 10;
      atom.y = parseFloatRange(line, 28, 36) * 10;
      atom.z = parseFloatRange(line, 36, 44) * 10;
      if (Float.isNaN(atom.x) || Float.isNaN(atom.y) || Float.isNaN(atom.z)) {
        Logger.warn("line cannot be read for GROMACS atom data: " + line);
        atom.set(0, 0, 0);
      }
      setAtomCoord(atom);
      atom.elementSymbol = deduceElementSymbol(atom.group3, atom.atomName);
      if (!filterAtom(atom, i))
        continue;
      atom.isHetero = false;
      asc.addAtom(atom);
      if (len < 69) 
        continue;
      float vx = parseFloatRange(line, 44, 52) * 10;
      float vy = parseFloatRange(line, 52, 60) * 10;
      float vz = parseFloatRange(line, 60, 68) * 10;
      if (Float.isNaN(vx) || Float.isNaN(vy) || Float.isNaN(vz))
        continue;
      asc.addVibrationVector(atom.index, vx, vy, vz);
    }
  }

  private void setAtomName(Atom atom, String gname, String aname) {
    atom.atomName = aname;
    if (gname.equals("SOL") && aname.length() == 3 && "OW1;HW2;HW3".indexOf(aname) >= 0)
      gname="WAT";
    atom.group3 = gname;  //allowing for 4 characters
  }

  String deduceElementSymbol(String group3, String atomName) {
    // best we can do
    if (atomName.length() <= 2 && group3.equals(atomName))
      return atomName;
    char ch1 = (atomName.length() == 4 ? atomName.charAt(0) : '\0');
    char ch2 = atomName.charAt(atomName.length() == 4 ? 1 : 0);
    boolean isHetero = vwr.getJBR().isHetero(group3);
    if (Atom.isValidSymNoCase(ch1, ch2))
      return (isHetero || ch1 != 'H' ? "" + ch1 + ch2 : "H");
    if (Atom.isValidSym1(ch2))
      return "" + ch2;
    if (Atom.isValidSym1(ch1))
      return "" + ch1;
    return "Xx";
  }

  private void readUnitCell() throws Exception {
    if (rd() == null)
      return;
    String[] tokens = getTokens();
    if (tokens.length < 3 || !doApplySymmetry)
      return;
    float a = 10 * parseFloatStr(tokens[0]);
    float b = 10 * parseFloatStr(tokens[1]);
    float c = 10 * parseFloatStr(tokens[2]);
    setUnitCell(a, b, c, 90, 90, 90);
    setSpaceGroupName("P1");
    Atom[] atoms = asc.atoms;
    P3 pt = P3.new3(0.5f, 0.5f, 0.5f);
    for (int i = asc.ac; --i >= 0;) {
      setAtomCoord(atoms[i]);
      atoms[i].add(pt);
    }
  }


}

