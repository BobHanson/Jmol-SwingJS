/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-30 11:40:16 -0500 (Fri, 30 Mar 2007) $
 * $Revision: 7273 $
 *
 * Copyright (C) 2007 Miguel, Bob, Jmol Development
 *
 * Contact: hansonr@stolaf.edu
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.jvxl.readers;

import java.io.BufferedReader;



import javajs.util.PT;
import javajs.util.SB;

class ApbsReader extends VolumeFileReader {

  /*
   *  http://apbs.sourceforge.net
   */
  
  ApbsReader(){}
  
  @Override
  void init2(SurfaceGenerator sg, BufferedReader br) {
    init2VFR(sg, br);
    // data are HIGH on the inside and LOW on the outside
    if (params.thePlane == null)
      params.insideOut = !params.insideOut;
    isAngstroms = true;
    nSurfaces = 1;
  }
  
  @Override
  protected void readParameters() throws Exception {
    jvxlFileHeaderBuffer = SB.newS(skipComments(false));
    while (line != null && line.length() == 0)
      rd();
    jvxlFileHeaderBuffer.append("APBS OpenDx DATA ").append(line).append("\n");
    jvxlFileHeaderBuffer.append("see http://apbs.sourceforge.net\n");
    String atomLine = rd();
    String[] tokens = PT.getTokens(atomLine);
    if (tokens.length >= 4) {
      volumetricOrigin.set(parseFloatStr(tokens[1]), parseFloatStr(tokens[2]),
          parseFloatStr(tokens[3]));
    }
    VolumeFileReader.checkAtomLine(isXLowToHigh, isAngstroms, tokens[0],
        atomLine, jvxlFileHeaderBuffer);
    readVoxelVector(0);
    readVoxelVector(1);
    readVoxelVector(2);
    rd();
    tokens = getTokens();
    /* see http://apbs.sourceforge.net/doc/user-guide/index.html#opendx-format
     object 2 class gridconnections counts nx ny nz
     object 3 class array type double rank 0 times n data follows
     * 
     */
    for (int i = 0; i < 3; i++)
      voxelCounts[i] = parseIntStr(tokens[i + 5]);
    rd();
  }
}
