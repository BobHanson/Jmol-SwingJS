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

import org.jmol.util.Logger;
import org.jmol.util.SimpleUnitCell;

import javajs.util.PT;
import javajs.util.SB;

class VaspChgcarReader extends PeriodicVolumeFileReader {

  private float volume;

  VaspChgcarReader(){
  }
  
  @Override
  void init2(SurfaceGenerator sg, BufferedReader br) {
    init2VFR(sg, br);
    isAngstroms = true;
    isPeriodic = true;
    isProgressive = false; // for now
    nSurfaces = 1;
  }

  //  pt                                      
  //  10.0000000000000     
  //    1.000000    0.000000    0.000000
  //    0.000000    1.000000    0.000000
  //    0.000000    0.000000    1.000000
  //  2   4
  //Direct
  // 0.080067  0.160200  0.131800
  // 0.213232  0.160200  0.131800
  // 0.022723  0.101273  0.060100
  // 0.022724  0.219127  0.203500
  // 0.270578  0.101273  0.060100
  // 0.270577  0.219127  0.203499
  //
  // 140  140  140
  //0.12994340141E+02 0.13765428641E+02 0.14355609008E+02 0.14732384059E+02 0.14873912610E+02

//  WRITE(IU,FORM) (((C(NX,NY,NZ),NX=1,NGXC),NY=1,NGYZ),NZ=1,NGZC)
//
// The x index is the fastest index, and the z index the slowest index. 
// The file can be read format-free, because at least in new versions, 
// it is guaranteed that spaces separate each number. Please do not forget 
// to divide by the volume before visualizing the file!
  
  @Override
  protected void readParameters() throws Exception {
    jvxlFileHeaderBuffer = new SB();
    jvxlFileHeaderBuffer.append("Vasp CHGCAR format\n\n\n");
    rd(); // atoms
    float scale = parseFloatStr(rd());
    float[] data = new float[15];
    data[0] = -1;
    for (int i = 0, pt = 6; i < 3; ++i)
      volumetricVectors[i].set(data[pt++] = parseFloatStr(rd()) * scale,
          data[pt++] = parseFloat() * scale, data[pt++] = parseFloat() * scale);
    volume = (float) SimpleUnitCell.newA(data).volume;
    // v0 here will be the slowest, not the fastest
    while (rd().length() > 2) {
    }    
    rd();
    String[] counts = getTokens();
    for (int i = 0; i < 3; ++i) {
      volumetricVectors[i]
          .scale(1f / ((voxelCounts[i] = parseIntStr(counts[i]) + 1) - 1));
      if (isAnisotropic)
        setVectorAnisotropy(volumetricVectors[i]);
    }
    swapXZ();
    volumetricOrigin.set(0, 0, 0);
    if (params.thePlane == null && (params.cutoffAutomatic || !Float.isNaN(params.sigma))) {
      params.cutoff = 0.5f;
      Logger.info("Cutoff set to " + params.cutoff);
    }
  }
  
  private int pt, nPerLine;
  
  @Override
  protected float nextVoxel() throws Exception {
    if (pt++ % nPerLine == 0 && nData > 0) {
      rd();
      next[0] = 0;
    }
    return parseFloat() / volume;
  }

  @Override
  protected void getPeriodicVoxels() throws Exception {
    // we are not reading the final periodic values
    int ni = voxelCounts[0] - 1;
    int nj = voxelCounts[1] - 1;
    int nk = voxelCounts[2] - 1;
    boolean downSampling = (nSkipX > 0);
    nPerLine = PT.countTokens(rd(), 0);
    for (int i = 0; i < ni; i++) {
      for (int j = 0; j < nj; j++) {
        for (int k = 0; k < nk; k++) {
          voxelData[i][j][k] = recordData(nextVoxel());
          if (downSampling)
            for (int m = nSkipX; --m >= 0;)
              nextVoxel();
        }
        if (downSampling)
          for (int m = nSkipY; --m >= 0;)
            nextVoxel();
      }
      if (downSampling)
        for (int m = nSkipZ; --m >= 0;)
          nextVoxel();
    }
  }

}


