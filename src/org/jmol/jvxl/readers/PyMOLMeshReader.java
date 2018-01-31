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
import java.util.Hashtable;

import javajs.util.Lst;
import javajs.util.SB;

import org.jmol.util.Logger;


/**
 * PyMOL surface/mesh reader.
 * 
 *  PyMOL "mesh": Describes a cutoff and "carving" distance around a set of points
 *  PyMOL "map": Volume data used by mesh (HupA_2) or direct visualization (HupA_LC)
 * @author Bob Hanson
 * 
 */


class PyMOLMeshReader extends MapFileReader {

  private Lst<Object> data;
  private Lst<Object> voxelList;
  private String surfaceName;
  private int pymolType;
  private boolean isMesh;
  //private float cutoff = Float.NaN;

  final static int cMapSourceCrystallographic = 1;
  final static int cMapSourceCCP4 = 2;
  final static int cMapSourceGeneralPurpose = 3;
  final static int cMapSourceDesc = 4;
  final static int cMapSourceFLD = 5;
  final static int cMapSourceBRIX = 6;
  final static int cMapSourceGRD = 7;
  final static int cMapSourceChempyBrick = 8;
  final static int cMapSourceVMDPlugin = 9;
  final static int cMapSourceObsolete = 10;
  
  final static int OBJECT_MAPDATA = 2;
  final static int OBJECT_MAPMESH = 3;
  
  PyMOLMeshReader(){}
  
  @SuppressWarnings("unchecked")
  @Override
  void init2(SurfaceGenerator sg, BufferedReader brNull) {
    init2MFR(sg, null);
    allowSigma = true;
    nSurfaces = 1;
    Hashtable<String, Lst<Object>> map = (Hashtable<String, Lst<Object>>) sg.getReaderData();    
    data = map.get(params.calculationType);
    if (data == null)
      return;
    pymolType = (int) getFloat(getList(data, 0), 0);
    isMesh = (pymolType == OBJECT_MAPMESH);
    surfaceName = (String) data.get(data.size() - 1); // added by adapter.readers.pymol.PyMOLReader

    Logger.info("PyMOLMeshReader for " + params.calculationType + " pymolType=" + pymolType + "; isMesh=" + isMesh +" surfaceName=" + surfaceName);

    data = getList(getList(data, 2), 0);
    if (isMesh && params.thePlane == null && params.cutoffAutomatic) {
      params.cutoff = getFloat(data, 8);
      // ignoring these for now -- implemented as slab display option instead
      // within range = getFloat(getList(getList(data, 2), 0), 11);
      // within list = getList(getList(data, 2), 0), 12);
      params.cutoffAutomatic = false;
    }
    if (isMesh)
      data = getList(getList(map.get(surfaceName), 2), 0);    
    voxelList = getList(getList(getList(data, 14), 2), 6);
    Logger.info("PyMOLMeshReader: Number of grid points = " + voxelList.size());
  }

  @SuppressWarnings("unchecked")
  private static Lst<Object> getList(Lst<Object> list, int i) {
    return (Lst<Object>) list.get(i);
  }

  // mesh data:
  //
  //HupA_2 carved mesh -- not read here. 
  //  
  //  0-2: Active, MapName, MapState,
  //  1, 1vot.2fofc.2, 0, 
  //
  //  3,4: Crystal, ExtentFlag,
  //   [[112.63200378417969, 112.63200378417969, 136.3780059814453], [90.0, 90.0, 120.0]], 1,                          
  //
  //  5,6: ExtentMin, ExtentMax
  //   [-1.6449999809265137, 62.125, 57.58100128173828], [8.47800064086914, 71.81900024414062, 67.55699920654297], 
  //   
  //  7-9: Range, Level, Radius, 
  //   [51, 55, 44, 74, 72, 60], -1.5, 0.0, 
  //   
  //  10,11: CarveFlag, CarveBuffer,
  //   1, 2.0,
  //
  //   12: AtomVertex VLA (carve points)
  //   [.....]
  //
  //   13: MeshMode
  //   0 
  //  
  //   14: AltLevel
  //   15: Quiet
  //   16: Field

  // volume (HupA_2) map data:

  //  =1vot.2fofc.2=
  //
  //    [2, 1vot.2fofc.2, 0, 
  //    [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1], 
  //    [-19.52288055419922, 26.011247634887695, 23.487323760986328], 
  //    [23.277284622192383, 100.14329528808594, 91.67632293701172], 1, 0, null, 0, 0, 
  //    [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], 0, null], 
  //    1, 
  //    [
  //
  //    0: Active
  //    [1, 
  //
  //    1: Crystal
  //    [[112.63200378417969, 112.63200378417969, 136.3780059814453], [90.0, 90.0, 120.0]],
  //    2-5: Origin, Range, Div, Grid
  //    null, null, null, null,
  //
  //    6: Corner
  //    [-19.52288055419922, 26.011247634887695, 23.487323760986328, 66.07744598388672, 26.011247634887695, 23.487323760986328, -62.32304000854492, 100.14329528808594, 23.487323760986328, 23.277284622192383, 100.14329528808594, 23.487323760986328, -19.52288055419922, 26.011247634887695, 91.67632293701172, 66.07744598388672, 26.011247634887695, 91.67632293701172, -62.32304000854492, 100.14329528808594, 91.67632293701172, 23.277284622192383, 100.14329528808594, 91.67632293701172],
  //
  //    7: ExtentMin
  //    [-19.52288055419922, 26.011247634887695, 23.487323760986328],
  //
  //    8: ExtentMax
  //    [23.277284622192383, 100.14329528808594, 91.67632293701172], 
  //    
  //    9: MapSource 
  //    2,
  //
  //    10-13: Div, Min, Max, FDim 
  //    [150, 150, 180], [-6, 40, 31], [108, 154, 121], [115, 115, 91, 3],
  //    14: field
  //    [
  //    [115, 115, 91], 0, 
  //    [0, 3, 4, 4813900, 
  //    [115, 115, 91], 
  //    [41860, 364, 4], 
  //    [....]  

  @Override
  protected void readParameters() throws Exception {

    // reading the map data

    Lst<Object> t;

    jvxlFileHeaderBuffer = new SB();
    jvxlFileHeaderBuffer.append("PyMOL surface reader\n");
    jvxlFileHeaderBuffer.append(surfaceName + " (" + params.calculationType
        + ")\n");

    // cell parameters
    Lst<Object> s = getList(data, 1);
    t = getList(s, 0);
    // change in format between PyMOL versions? States?
    // sometimes list is in a wrapper list.

    // Crystal, but this might not be used, and conceivably could be null.
    boolean haveUnitCell = false;
    if (t != null) {
      if (t.size() < 3)
        t = getList(s = getList(s, 0), 0);
      a = getFloat(t, 0);
      haveUnitCell = (a != 1);
      if (haveUnitCell) {
        b = getFloat(t, 1);
        c = getFloat(t, 2);
        t = getList(s, 1);
        alpha = getFloat(t, 0);
        beta = getFloat(t, 1);
        gamma = getFloat(t, 2);
      }
    }

    // ExtentMin
    t = getList(data, 7);
    origin.set(getFloat(t, 0), getFloat(t, 1), getFloat(t, 2));

    // Div
    // These are actual crystallographic unit-cell based values.
    // The grid itself is based on {a/na, b/nb, c/nc), but 
    // could have completely different origin and extent.
    // In PyMOL they may mean nothing and can even be totally undefined. 

    t = getList(data, 10);
    na = (int) getFloat(t, 0);
    nb = (int) getFloat(t, 1);
    nc = (int) getFloat(t, 2);

    // Min
    t = getList(data, 11);
    xyzStart[0] = getFloat(t, 0);
    xyzStart[1] = getFloat(t, 1);
    xyzStart[2] = getFloat(t, 2);

    // FDim
    // These are the actual grid extents for the map, inclusive of both end points.
    // We will end up with xyz, but we use zyx here because the storage
    // scheme is different for Jmol (z fastest, x slowest), and this takes care of that.

    t = getList(data, 13);
    n2 = (int) getFloat(t, 0);
    n1 = (int) getFloat(t, 1);
    n0 = (int) getFloat(t, 2);

    // if is no crystallographic unit cell, then na, nb, nc, above, are total garbage,
    // and we need to get them from nz, ny, nz. And we need to get a,b,c from ExtentMax - ExtentMin:

    if (!haveUnitCell) {
      // mep grid, for example (electrostatics2.pse)
      na = n2 - 1;
      nb = n1 - 1;
      nc = n0 - 1;
      t = getList(data, 8); // ExtentMax
      a = getFloat(t, 0) - origin.x;
      b = getFloat(t, 1) - origin.y;
      c = getFloat(t, 2) - origin.z;
      alpha = beta = gamma = 90;
    }

    mapc = 3; // fastest
    mapr = 2;
    maps = 1; // slowest

    getVectorsAndOrigin();
    setCutoffAutomatic();

  }
   
  private int pt;
  @Override
  protected float nextVoxel() throws Exception {
    return getFloat(voxelList, pt++);
  }

  private float getFloat(Lst<Object> list, int i) {
    return ((Number) list.get(i)).floatValue();
  }

  @Override
  protected void skipData(int nPoints) throws Exception {
  }
  
  @Override
  protected void setCutoffAutomatic() {
    if (params.thePlane != null)
      return;
    if (Float.isNaN(params.sigma)) {
      if (!params.cutoffAutomatic)
        return;
      params.cutoff = (boundingBox == null ? 3.0f : 1.6f);
      if (dmin != Float.MAX_VALUE) {
        if (params.cutoff > dmax)
          params.cutoff = dmax / 4; // just a guess
      }
    } else {
      params.cutoff = calculateCutoff();
    }
    Logger.info("MapReader: setting cutoff to default value of "
        + params.cutoff
        + (boundingBox == null ? " (no BOUNDBOX parameter)\n" : "\n"));
  }

  private float calculateCutoff() {
    int n = voxelList.size();
    float sum = 0;
    float sum2 = 0;
    for (int i = 0; i < n; i++) {
      float v = getFloat(voxelList, i);
      sum += v;
      sum2 += v * v;
    }
    float mean = sum / n;
    float rmsd = (float) Math.sqrt(sum2 / n);
    Logger.info("PyMOLMeshReader rmsd=" + rmsd + " mean=" + mean);
    return params.sigma * rmsd + mean;
  }

}
