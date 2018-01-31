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

import javajs.util.SB;

class XsfReader extends VolumeFileReader {

  // from XCrysDen; see http://www.xcrysden.org/doc/XSF.html
    
  XsfReader(){}
  
  @Override
  void init2(SurfaceGenerator sg, BufferedReader br) {
    init2VFR(sg, br);
  }
  
  private boolean isBXSF = false;

  @Override
  protected void readParameters() throws Exception {
    isAngstroms = false;
    params.blockCubeData = true;
    jvxlFileHeaderBuffer = new SB();
    jvxlFileHeaderBuffer.append("XsfReader file\n");
    boolean needCutoff = params.cutoffAutomatic;
    isAngstroms = true;
    String beginKey = "BEGIN_DATAGRID_3D";
    nSurfaces = 1;
    while (rd() != null && line.indexOf(beginKey) < 0) {
      Logger.info(line);
      if (line.indexOf("Fermi Energy:") >= 0) {
        isBXSF = true;
        beginKey = "BEGIN_BANDGRID_3D";
        if (needCutoff) {
          params.cutoff = parseFloatStr(getTokens()[2]);
          needCutoff = false;
        }
      }
      continue;
    }
    if (needCutoff)
      params.cutoff = 0.05f;
    if (isBXSF)
      nSurfaces = parseIntStr(rd());
    voxelCounts[0] = parseIntStr(rd());
    voxelCounts[1] = parseInt();
    voxelCounts[2] = parseInt();
    volumetricOrigin.set(parseFloatStr(rd()), parseFloat(), parseFloat());
    // SPANNING vectors here.
    for (int i = 0; i < 3; ++i) {
      volumetricVectors[i].set(parseFloatStr(rd()), parseFloat(),
          parseFloat());
      volumetricVectors[i].scale(1.0f / (voxelCounts[i] - 1));
    }
    if (isBXSF) {
/*      
      System.out.println("testing XSFREADER");
      volumetricVectors[0].set(0.1f, 0, 0);
      volumetricVectors[1].set(0, 0.1f, 0);
      volumetricVectors[2].set(0,0, 0.1f);
*/      
      // data are slowest-x
      // standard Jmol order
    } else {
      // data are slowest-z
      // reversed order -- so we just reverse the vectors
      swapXZ();
    }
  }
  
  @Override
  protected void gotoData(int n, int nPoints) throws Exception {
    if (!params.blockCubeData)
      return;
    if (n > 0)
      Logger.info("skipping " + n + " data sets, " + nPoints + " points each");
    if (isBXSF)
      Logger.info(rd()); //"BAND: <n>" line
    for (int i = 0; i < n; i++)
      skipData(nPoints);
  }

  @Override
  protected void skipData(int nPoints) throws Exception {
    skipDataVFR(nPoints);
    if (isBXSF)
      Logger.info(rd()); //"BAND: <n>" line
  }

}


