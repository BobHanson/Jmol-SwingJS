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


import org.jmol.jvxl.data.JvxlCoder;

import javajs.util.SB;
import javajs.util.T3;
import javajs.util.P4;

class IsoFxyReader extends AtomDataReader {
  
  IsoFxyReader(){}
  
  @Override
  void init(SurfaceGenerator sg) {
    initIFR(sg);
  }

  protected void initIFR(SurfaceGenerator sg) {
    initADR(sg);
    isXLowToHigh = true;
    precalculateVoxelData = false;
    params.fullyLit = true;
    isPlanarMapping = (params.thePlane != null || params.state == Parameters.STATE_DATA_COLORED);
    volumeData.sr = (params.func == null ? null : this);
  }

  private float[][] data;
  private boolean isPlanarMapping;
  private Object[] func;
  
  @Override
  protected void setup(boolean isMapData) {
    if (params.functionInfo.size() > 5)
      data = (float[][]) params.functionInfo.get(5);
    setupType("functionXY");
  }

  protected void setupType(String type) {
    func = (Object[]) params.func;
    String functionName = (String) params.functionInfo.get(0);
    jvxlFileHeaderBuffer = new SB();
    jvxlFileHeaderBuffer.append(type).append("\n").append(functionName).append("\n");
    if (params.thePlane != null || data == null && !useOriginStepsPoints)
      setVolumeForPlane();
    else if (data == null)
      setVolumeDataParams();
    else
      setVolumeData();
    JvxlCoder.jvxlCreateHeaderWithoutTitleOrAtoms(volumeData, jvxlFileHeaderBuffer);
  }

  @Override
  protected void setVolumeData() {
    if (data == null) {
      setVolumeDataADR(); 
      return;
    }
    volumetricOrigin.setT((T3) params.functionInfo.get(1));
    for (int i = 0; i < 3; i++) {
      P4 info = (P4) params.functionInfo.get(i + 2);
      voxelCounts[i] = Math.abs((int) info.x);
      volumetricVectors[i].set(info.y, info.z, info.w);      
    }
    if (isAnisotropic)
      setVolumetricAnisotropy();
  }

  @Override
  protected void readSurfaceData(boolean isMapData) throws Exception {
    if (volumeData.sr != null)
      return;
    readSurfaceDataVDR(isMapData);
  }

  @Override
  public float[] getPlane(int x) {
    float[] plane = getPlaneSR(x);
    setPlane(x, plane);
    return plane;
  }

  private void setPlane(int x, float[] plane) {
      for (int y = 0, ptyz = 0; y < nPointsY; ++y)
        for (int z = 0; z < nPointsZ; ++z)
          plane[ptyz++] = getValue(x, y, z, 0);
  }

  @Override
  public float getValue(int x, int y, int z, int pxyz) {
    float value;
    if (data == null) {
      value = evaluateValue(x, y, z);
    } else {
      volumeData.voxelPtToXYZ(x, y, z, ptTemp);
      value = data[x][y]; 
    }
    return (isPlanarMapping ? value : value - ptTemp.z);
  }
    
  private final float[] values = new float[3];

  
  @Override
  public float getValueAtPoint(T3 pt, boolean getSource) {
    if (params.func == null)
      return 0;
    values[0] = pt.x;
    values[1] = pt.y;
    values[2] = pt.z;
    return sg.atomDataServer.evalFunctionFloat(func[0], func[1], values);
  }

  protected float evaluateValue(int x, int y, int z) {
    volumeData.voxelPtToXYZ(x, y, z, ptTemp);
    return getValueAtPoint(ptTemp, false);
  }
}
