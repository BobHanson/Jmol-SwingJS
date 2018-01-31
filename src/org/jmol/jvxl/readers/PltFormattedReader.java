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


import javajs.util.SB;

import org.jmol.viewer.Viewer;

class PltFormattedReader extends VolumeFileReader {

  /*
   * Jaguar .plt file reader
   * 
   * free format after the first three lines
          1         2         3         4         5         6         7
012345678901234567890123456789012345678901234567890123456789012345678901234567890

      3      3
     13     13     12
-0.41532E+01 0.78468E+01-0.40155E+01 0.79845E+01-0.38912E+01 0.71088E+01
  0.169508400475E-03  0.234342571007E-03
  0.310143591477E-03  0.372117502099E-03
  0.383467878529E-03  0.330699047883E-03
 
   */
  
  PltFormattedReader(){}
  
  @Override
  void init2(SurfaceGenerator sg, BufferedReader br) {
    init2VFR(sg, br);
    isAngstroms = true;
    jvxlData.wasCubic = true;
    jvxlFileHeaderBuffer = new SB();
    nSurfaces = 1;
  }

  
  @Override
  protected void readParameters() throws Exception {
    int n1 = parseIntStr(rd());
    int n2 = parseInt();
    //yes, it's "Z Y X", but that doesn't matter. Our loop will be (X (Y (Z)))
    nPointsX = parseIntStr(rd());
    nPointsY = parseInt();
    nPointsZ = parseInt();
    jvxlFileHeaderBuffer.append("Plt formatted data (" + n1 + "," + n2 + ") "
        + nPointsX + " x " + nPointsY + " x " + nPointsZ + " \nJmol " + Viewer.getJmolVersion() + '\n');    
    volumetricOrigin.set(0, 0, 0);
    
/*            1         2         3         4         5         6         7
    012345678901234567890123456789012345678901234567890123456789012345678901234567890

    -0.41532E+01 0.78468E+01-0.40155E+01 0.79845E+01-0.38912E+01 0.71088E+01
*/
    float xmin = parseFloatStr(rd().substring(0, 12));
    float xmax = parseFloatRange(line, 12, 24);
    float ymin = parseFloatRange(line, 24, 36);
    float ymax = parseFloatRange(line, 36, 48);
    float zmin = parseFloatRange(line, 48, 60);
    float zmax = parseFloatRange(line, 60, 72);
    volumetricOrigin.set(xmin, ymin, zmin);
    voxelCounts[0] = nPointsX;
    voxelCounts[1] = nPointsY;
    voxelCounts[2] = nPointsZ;

    // because they really are Z, Y, and X !
    
    volumetricVectors[0].set(0, 0, (xmax - xmin)/nPointsX);
    volumetricVectors[1].set(0, (ymax - ymin)/nPointsY, 0);
    volumetricVectors[2].set((zmax - zmin)/nPointsZ, 0, 0);
    
  }
}


