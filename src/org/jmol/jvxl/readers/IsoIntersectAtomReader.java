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



import javajs.util.BS;

import javajs.util.T3;

class IsoIntersectAtomReader extends AtomDataReader {

  private static final int TYPE_FUNCTION = 0;
  private static final int TYPE_SUM = 1;
  private static final int TYPE_DIFF = 2;
  private static final int TYPE_MAX = 3;
  private static final int TYPE_DIFF_PAIR = 4;

  IsoIntersectAtomReader(){}
  
  @Override
  void init(SurfaceGenerator sg) {
    initADR(sg);
  }

  ///// VDW intersection reader -- not mappable //////

  private final BS myBsA = new BS();
  private final BS myBsB = new BS();
  private BS[][] bsAtomMinMax = new BS[2][];
  private Object[] func;
  private int funcType = TYPE_FUNCTION;
  
  @Override
  protected boolean readVolumeParameters(boolean isMapData) {
    setup(isMapData);
    if (isMapData)
      return false;
    initializeVolumetricData();
    volumeData.setUnitVectors();
    thisPlaneB = new float[volumeData.getYzCount()];
    voxelSource = new int[volumeData.nPoints];
    vl0 = volumeData.volumetricVectorLengths[0];
    vl1 = volumeData.volumetricVectorLengths[1];
    vl2 = volumeData.volumetricVectorLengths[2];
    getAtomMinMax(myBsA, bsAtomMinMax[0] = new BS[nPointsX]);
    getAtomMinMax(myBsB, bsAtomMinMax[1] = new BS[nPointsX]);
    return true;
  }
  
  
  @Override
  protected void setup(boolean isMapData) {
    setup2();
    params.fullyLit = true;
    point = params.point;
    if (params.func instanceof String) {
      funcType = (params.func.equals("a-b") ? TYPE_DIFF : params.func
          .equals("a+b") ? TYPE_SUM : TYPE_MAX);
    } else if (params.func == null || sg.atomDataServer == null) {
      funcType = TYPE_DIFF;
    } else {
      func = (Object[]) params.func;
    }
    if (contactPair == null) {
      BS bsA = params.intersection[0];
      BS bsB = params.intersection[1];
      BS bsSelected = new BS();
      bsSelected.or(bsA);
      bsSelected.or(bsB);
      doUseIterator = true; // just means we want a map
      getAtoms(bsSelected, doAddHydrogens, true, true, false, false, false,
          Float.NaN, null);      
      for (int i = bsA.nextSetBit(0); i >= 0; i = bsA.nextSetBit(i + 1))
        myBsA.set(myIndex[i]);
      for (int i = bsB.nextSetBit(0); i >= 0; i = bsB.nextSetBit(i + 1))
        myBsB.set(myIndex[i]);
      setHeader("VDW intersection surface", params.calculationType);
      setRanges(params.solvent_ptsPerAngstrom, params.solvent_gridMax, 0);
      margin = 5f;
    } else {
      setVolumeData();
    }
    isProgressive = isXLowToHigh = true;
  }

  private float[] thisPlaneB;

  @Override
  public float[] getPlane(int x) {
    if (yzCount == 0) {
      initPlanes();
    }

    thisX = x;   
    thisPlane= yzPlanes[x % 2];
    if(contactPair == null) {
      thisAtomSet = bsAtomMinMax[0][x];
      resetPlane(Float.MAX_VALUE);
      markSphereVoxels(0, params.distance);
      thisPlane = thisPlaneB;
      thisAtomSet = bsAtomMinMax[1][x];
      resetPlane(Float.MAX_VALUE);
      markSphereVoxels(0, params.distance);
    } else {
      markPlaneVoxels(contactPair.myAtoms[0], contactPair.radii[0]);
      thisPlane = thisPlaneB;
      markPlaneVoxels(contactPair.myAtoms[1], contactPair.radii[1]);
    }
    thisPlane = yzPlanes[x % 2];
    if (!setVoxels())
      resetPlane(0);
    if (contactPair == null)
      unsetVoxelData();
    return thisPlane;
  }

  /////////////// calculation methods //////////////

  private boolean setVoxels() {
    for (int i = 0; i < yzCount; i++) {
      float va = thisPlane[i];
      float vb = thisPlaneB[i];
      float v = getValueAB(va, vb);
      if (Float.isNaN(v))
        return false;
      thisPlane[i] = v;
    }
    return true;
  }

  private final float[] values = new float[2];
  
  private float getValueAB(float va, float vb) {
    if (va == Float.MAX_VALUE || vb == Float.MAX_VALUE
        || Float.isNaN(va) || Float.isNaN(vb))
      return Float.MAX_VALUE;
    switch (funcType) {
    case TYPE_SUM:
      return (va + vb);
    case TYPE_DIFF:
    case TYPE_DIFF_PAIR:
      return (va - vb);
    case TYPE_MAX:
      return (va>vb?va:vb);
    default:
      values[0] = va;
      values[1] = vb;
      return sg.atomDataServer.evalFunctionFloat(func[0], func[1], values);
    }
  }
  
  @Override
  public float getValueAtPoint(T3 pt, boolean getSource) {
    // mapping sasurface/vdw 
    return getValueAB(getValueAtPoint2(pt, myBsA), getValueAtPoint2(pt, myBsB));
  }
  
  private float getValueAtPoint2(T3 pt, BS bs) {
    float value = Float.MAX_VALUE;
    for (int iAtom = bs.nextSetBit(0); iAtom >= 0; iAtom = bs.nextSetBit(iAtom + 1)) {
      float r = pt.distance(atomXyzTruncated[iAtom]) - atomRadius[iAtom];
      if (r < value)
        value = r;
    }
    return (value == Float.MAX_VALUE ? Float.NaN : value);
  }

}
