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
import javajs.util.V3;

class CastepDensityReader extends PeriodicVolumeFileReader {

  private int nFilePoints;
  
  CastepDensityReader(){}
  
  @Override
  void init2(SurfaceGenerator sg, BufferedReader br) {
    init2VFR(sg, br);
    isProgressive = false;
    isAngstroms = true;
  }

  private int nSkip;
  
  @Override
  protected void gotoData(int n, int nPoints) throws Exception {
    nSkip = n;
  }
  
  /*
   * 
  BEGIN header
  
           Real Lattice(A)               Lattice parameters(A)    Cell Angles
   4.5532597   0.0000000   0.0000000     a =    4.553260  alpha =   90.000000
   0.0000000   4.5532597   0.0000000     b =    4.553260  beta  =   90.000000
   0.0000000   0.0000000   2.9209902     c =    2.920990  gamma =   90.000000
  
   1                            ! nspins
  50    50    32                ! fine FFT grid along <a,b,c>
  END header: data is "<a b c> charge" in units of electrons/grid_point * number of grid_points
  
     1     1     1          591.571292
  */

  @Override
  protected void readParameters() throws Exception {
    jvxlFileHeaderBuffer = new SB();
    while (rd() != null && line.indexOf(".") < 0) {
      // skip front stuff
    }
    for (int i = 0; i < 3; ++i) {
      V3 voxelVector = volumetricVectors[i];
      voxelVector.set(parseFloatStr(line), parseFloat(), parseFloat());
      rd();
    }
    nSurfaces = parseIntStr(rd());
    rd();
    voxelCounts[0] = (nPointsX = parseIntStr(line)) + 1;
    voxelCounts[1] = (nPointsY = parseInt()) + 1;
    voxelCounts[2] = (nPointsZ = parseInt()) + 1;
    nFilePoints = (nPointsX++) * (nPointsY++) * (nPointsZ++);
    volumetricOrigin.set(0, 0, 0);
    for (int i = 0; i < 3; i++) {
      volumetricVectors[i].scale(1f/(voxelCounts[i] - 1));
      if (isAnisotropic)
        setVectorAnisotropy(volumetricVectors[i]);
    }
    while (rd().trim().length() > 0) {
      //
    }
  }

  @Override
  protected void getPeriodicVoxels() throws Exception {
    rd();
    String[] tokens = getTokens();
    if (nSkip > 0 && tokens.length < 3 + nSurfaces) {
      for (int j = 0; j < nSkip; j++)
        for (int i = 0; i < nFilePoints; i++)
          rd();
      nSkip = 0;
    }
    int dsf = downsampleFactor;
    if (dsf > 1) {
      for (int i = 0; i < nFilePoints; i++) {
        int x = parseIntStr(line) - 1;
        int y = parseInt() - 1;
        int z = parseInt() - 1;
        if (x % dsf == 0 && y % dsf == 0 && z % dsf == 0) {
          if (nSkip > 0)
            skipPoints(nSkip);
          voxelData[x / dsf][y / dsf][z / dsf] = recordData(parseFloat());
        }
        rd();
      }
    } else {
      for (int i = 0; i < nFilePoints; i++) {
        int x = parseIntStr(line) - 1;
        int y = parseInt() - 1;
        int z = parseInt() - 1;
        if (nSkip > 0)
          skipPoints(nSkip);
        voxelData[x][y][z] = recordData(parseFloat());
        rd();
      }
    }
  }

  private void skipPoints(int n) {
    int pt = next[0];
    for (int i = 0; i < n; i++) {
      while (pt < line.length() && PT.isWhitespace(line.charAt(pt++))) {
        // skip white space
      }
      while (pt < line.length() && !PT.isWhitespace(line.charAt(pt++))) {
        // skip not white space
      }
    }
    next[0] = pt;
  }
  
}

