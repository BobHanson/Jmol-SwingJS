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

package org.jmol.adapter.readers.simple;

//import javajs.util.List;
//
//import org.jmol.adapter.smarter.AtomSetCollectionReader;
//import org.jmol.adapter.smarter.Atom;
//
//import org.jmol.util.Logger;

/**
 * simple Tinker format requires Tinker:: prefix:
 * 
 * load Tinker::mydata.xyz
 * 
 * 1/2014 hansonr@stolaf.edu
 * 
 */

public class TinkerReader extends FoldingXyzReader {
  // these two are the same now.
//
//  @Override
//  protected boolean checkLine() throws Exception {
//    int modelAtomCount = parseIntStr(line);
//    if (modelAtomCount == Integer.MIN_VALUE) {
//      continuing = false;
//      return false;
//    }
//    asc.newAtomSet();
//    String name = line.substring(line.indexOf(" ") + 1);
//    asc.setAtomSetName(name);
//    readAtomsAndBonds(modelAtomCount);
//    applySymmetryAndSetTrajectory();
//    continuing = false;
//    return false;
//  }
//
//  private void readAtomsAndBonds(int n) throws Exception {
//    List<String[]> lines = new List<String[]>();
//    String[] tokens;
//    String types = "";
//    // first create the atoms
//    for (int i = 0; i < n; ++i) {
//      readLine();
//      tokens = getTokens();
//      if (tokens.length < 5) {
//        Logger.warn("line cannot be read for atom data: " + line);
//        i--;
//        continue;
//      }
//      lines.addLast(tokens);
//      Atom atom = asc.addNewAtom();
//      setElementAndIsotope(atom, tokens[1]);
//      atom.x = parseFloatStr(tokens[2]);
//      atom.y = parseFloatStr(tokens[3]);
//      atom.z = parseFloatStr(tokens[4]);
//      types += tokens[5] + "\n";
//    }
//    // add the atom types
//    asc.setAtomSetAtomProperty("atomType", types, -1);
//    // now create the bonds
//    String temp = "";
//    for (int i = 0; i < n; i++) {
//      tokens = lines.get(i); 
//      String index1 = tokens[0];
//      int i1 = parseIntStr(index1) - 1;
//      for (int j = 6; j < tokens.length; j++) {
//        String index2 = tokens[j];
//        int i2 = parseIntStr(index2) - 1;
//        String key = ";" + (i1 < i2 ? index1 : index2) + ";" + (i1 < i2 ? index2 : index1) + ";";
//        if (temp.indexOf(key) >= 0)
//          continue;
//        temp += key;
//        asc.addNewBondWithOrder(i1, i2, 1);
//      }
//    }
//  }

}
