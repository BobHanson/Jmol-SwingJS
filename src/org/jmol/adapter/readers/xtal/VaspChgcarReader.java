/* $RCSfile$
 * $Author: hansonr $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: jmol-developers@lists.sf.net
 *
 * Copyright (C) 2009  Piero Canepa, University of Kent, UK
 *
 * Contact: pc229@kent.ac.uk or pieremanuele.canepa@gmail.com
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
 *
 */

package org.jmol.adapter.readers.xtal;

/**
 * http://cms.mpi.univie.ac.at/vasp/vasp/CHGCAR_file.html
 * 
 * very simple reader of just the header information.
 * 
 */

public class VaspChgcarReader extends VaspPoscarReader {

// subsumed by POSCAR ? 
  
//  @Override
//  protected void initializeReader() {
//    setSpaceGroupName("P1");
//    setFractionalCoordinates(true);
//  }

//  Na Cl                                   
//  5.68452685100000     
//    1.000000    0.000000    0.000000
//    0.000000    1.000000    0.000000
//    0.000000    0.000000    1.000000
//  4   4
//Direct
// 0.750000  0.250000  0.250000
// 0.250000  0.750000  0.250000
// 0.250000  0.250000  0.750000
// 0.750000  0.750000  0.750000
// 0.250000  0.250000  0.250000
// 0.750000  0.750000  0.250000
// 0.750000  0.250000  0.750000
// 0.250000  0.750000  0.750000
//

//  @Override
//  protected boolean checkLine() throws Exception {
//    String[] atomSym = getTokens();
//    float scale = parseFloatStr(rd());
//    float[] unitCellData = new float[9];
//    fillFloatArray(null, 0, unitCellData);
//    for (int i = 0; i < 9; i++)
//      unitCellData[i] *= scale;
//    addPrimitiveLatticeVector(0, unitCellData, 0);
//    addPrimitiveLatticeVector(1, unitCellData, 3);
//    addPrimitiveLatticeVector(2, unitCellData, 6);
//    String[] tokens = PT.getTokens(rd());
//    int[] atomCounts = new int[tokens.length];
//    for (int i = tokens.length; --i >= 0;)
//      atomCounts[i] = parseIntStr(tokens[i]);
//    if (atomSym.length != atomCounts.length)
//      atomSym = null;
//    /*String type = */ rd();
//    // type should be "direct"
//    for (int i = 0; i < atomCounts.length; i++)
//      for (int j = atomCounts[i]; --j >= 0;)
//        addAtomXYZSymName(PT.getTokens(rd()), 0, (atomSym == null ? "Xx" : atomSym[i]), null);
//    applySymmetryAndSetTrajectory();
//    continuing = false;
//    return false;
//  }

}
