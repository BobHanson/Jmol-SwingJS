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

/*
 * A simple Jaguar .PLT file reader for isosurface
 * 
 *   isosurface "myfile.plt"
 *   
 * Demonstrates the generalization of a CUBE reader
 * 
 * 
 *     UNTESTED!
 * 
 */
class JaguarReader extends VolumeFileReader {

  JaguarReader(){}
  
  @Override
  void init2(SurfaceGenerator sg, BufferedReader br) {
    init2VFR(sg, br);
    nSurfaces = 1;
    // ? params.insideOut = !params.insideOut;
  }

  /*
   * 
   &plot
   iplot=    1
   iorb1a=homo                
   iorb2a=lumo                
   iorb1b=0                   
   iorb2b=0                   
   origin=   -4.423471    7.705736   14.291432
   extentx=   23.243631    0.000000    0.000000
   extenty=    0.000000   26.456166    0.000000
   extentz=    0.000000    0.000000   24.944385
   npts=  123  140  132
   &end
   <then 123*140*132 data points, one per line>
   
   *
   * NOTE: Seems odd that "extent" is npts*delta instead of (npts-1)*delta,
   * but it appears to be, because using npts*delta we get the same
   * value for delta in each direction, which is reasonable.
   * 
   * But then it really isn't "extent", is it?
   *
   * Bob Hanson, 08/2007
   */

  /**
   * nothing much here
   * 
   * @exception Exception -- generally a reader issue
   */
  @Override
  protected void readParameters() throws Exception {
    jvxlFileHeaderBuffer = new SB();
    jvxlFileHeaderBuffer.append("Jaguar data\n");
    jvxlFileHeaderBuffer.append("\n");
    String atomLine;
    while ((atomLine = rd()) != null
        && atomLine.indexOf("origin=") < 0) {
    }
    String[] tokens = PT.getTokensAt(atomLine, 0);
    if (tokens.length == 4 && tokens[0].equals("origin=")) {
      volumetricOrigin.set(parseFloatStr(tokens[1]), parseFloatStr(tokens[2]),
          parseFloatStr(tokens[3]));
      VolumeFileReader
          .checkAtomLine(isXLowToHigh, isAngstroms, "0", "0 " + tokens[1]
              + " " + tokens[2] + " " + tokens[3], jvxlFileHeaderBuffer);
      if (!isAngstroms)
        volumetricOrigin.scale(ANGSTROMS_PER_BOHR);
    }
    
    readExtents(0);
    readExtents(1);
    readExtents(2);
    
    tokens = PT.getTokens(rd());
    voxelCounts[0] = parseIntStr(tokens[1]);
    voxelCounts[1] = parseIntStr(tokens[2]);
    voxelCounts[2] = parseIntStr(tokens[3]);
    float factor = (isAngstroms ? 1 : ANGSTROMS_PER_BOHR);
    float d = extents[0] / (voxelCounts[0] - 1);
    volumetricVectors[0].set(d * factor, 0, 0);
    jvxlFileHeaderBuffer.append(voxelCounts[0] + " " + d + " 0.0 0.0\n");

    d = extents[1] / (voxelCounts[1] - 1);
    volumetricVectors[1].set(0, d * factor, 0);
    jvxlFileHeaderBuffer.append(voxelCounts[1] + " 0.0 " + d + " 0.0\n");

    d = extents[2] / (voxelCounts[2] - 1);
    volumetricVectors[2].set(0, 0, d * factor);
    jvxlFileHeaderBuffer.append(voxelCounts[2] + " 0.0 0.0 " + d + "\n");

    // Note -- the "-1" is necessary, above, even though this
    // creates a nonuniform grid. Someone made a mistake somewhere, 
    // I think, because if you don't use -1 here, then the grid
    // distances are the same, but the surface is in the wrong place!
    
    rd();

  }

  private float[] extents = new float[3];
  
  /**
   * read the extentx=, extenty=, extentz= lines and cache them
   * then read the npts= line and construct the necessary data
   * 
   * @param voxelVectorIndex   0, 1, or 2
   * @exception Exception -- generally a reader issue
   */
  private void readExtents(int voxelVectorIndex) throws Exception {
    String[] tokens = PT.getTokens(rd());
    extents[voxelVectorIndex] = parseFloatStr(tokens[voxelVectorIndex + 1]);
  }
}
