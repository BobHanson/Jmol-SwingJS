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

import javajs.util.T3d;

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
    thisPlaneB = new double[volumeData.getYzCount()];
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
          Double.NaN, null);      
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

  private double[] thisPlaneB;

  @Override
  public double[] getPlane(int x) {
    if (yzCount == 0) {
      initPlanes();
    }

    thisX = x;   
    thisPlane= yzPlanes[x % 2];
    if(contactPair == null) {
      thisAtomSet = bsAtomMinMax[0][x];
      resetPlane(Double.MAX_VALUE);
      markSphereVoxels(0, params.distance);
      thisPlane = thisPlaneB;
      thisAtomSet = bsAtomMinMax[1][x];
      resetPlane(Double.MAX_VALUE);
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
      double va = thisPlane[i];
      double vb = thisPlaneB[i];
      double v = getValueAB(va, vb);
      if (Double.isNaN(v))
        return false;
      thisPlane[i] = v;
    }
    return true;
  }

  private final double[] values = new double[2];
  
  private double getValueAB(double va, double vb) {
    if (va == Double.MAX_VALUE || vb == Double.MAX_VALUE
        || Double.isNaN(va) || Double.isNaN(vb))
      return Double.MAX_VALUE;
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
  public double getValueAtPoint(T3d pt, boolean getSource) {
    // mapping sasurface/vdw 
    return getValueAB(getValueAtPoint2(pt, myBsA), getValueAtPoint2(pt, myBsB));
  }
  
  private double getValueAtPoint2(T3d pt, BS bs) {
    double value = Double.MAX_VALUE;
    for (int iAtom = bs.nextSetBit(0); iAtom >= 0; iAtom = bs.nextSetBit(iAtom + 1)) {
      double r = pt.distance(atomXyzTruncated[iAtom]) - atomRadius[iAtom];
      if (r < value)
        value = r;
    }
    return (value == Double.MAX_VALUE ? Double.NaN : value);
  }

}
