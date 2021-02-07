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
import org.jmol.util.Logger;

import javajs.util.AU;
import javajs.util.SB;
import javajs.util.P3;

class VolumeDataReader extends SurfaceReader {

  /*        (requires AtomDataServer)
   *                |-- IsoSolventReader
   *                |-- IsoIntersectReader
   *                |-- IsoMOReader, IsoMepReader
   *                |-- IsoPlaneReader
   *                |
   *            AtomDataReader (abstract)
   *                |
   *                |         |-- IsoFxyReader (not precalculated)
   *                |         |-- IsoShapeReader (not precalculated)  
   *                |         |         
   *            VolumeDataReader (precalculated data)       
   *                   |
   *                SurfaceReader
   * 
   * 
   */
  
  protected int dataType;
  protected boolean precalculateVoxelData;
  protected boolean allowMapData;
  protected P3 point;
  protected float ptsPerAngstrom;
  protected int maxGrid;
  protected boolean useOriginStepsPoints;

  VolumeDataReader() {
    // could be instantiated by map data from a reader
    // but this is almost certainly not implemented
  }
  
  @Override
  void init(SurfaceGenerator sg) {
    initVDR(sg);
  }

  protected void initVDR(SurfaceGenerator sg) {
    initSR(sg);
    useOriginStepsPoints = (params.origin != null && params.points != null && params.steps != null);
    dataType = params.dataType;
    precalculateVoxelData = true;
    allowMapData = true;    
  }

  /**
   * @param isMapData  
   */
  void setup(boolean isMapData) {
    //as is, just the volumeData as we have it.
    //but subclasses can modify this behavior.
    jvxlFileHeaderBuffer = new SB().append("volume data read from file\n\n");
    JvxlCoder.jvxlCreateHeaderWithoutTitleOrAtoms(volumeData, jvxlFileHeaderBuffer);
  }
  
  @Override
  protected boolean readVolumeParameters(boolean isMapData) {
    setup(isMapData);
    initializeVolumetricData();
    return true;
  }

  @Override
  protected boolean readVolumeData(boolean isMapData) {
    try {
      readSurfaceData(isMapData);
    } catch (Exception e) {
      System.out.println(e.toString());
      /**
       * @j2sNative
       */
      {
      e.printStackTrace();
      }
      return false;
    }
    return true;
  }

  protected void readVoxelDataIndividually(boolean isMapData) throws Exception {
    if (isMapData && !allowMapData)
      return; //not applicable
    if (!isMapData || volumeData.sr != null) {
      volumeData.setVoxelDataAsArray(voxelData = null);
      return;      
    }
    newVoxelDataCube();
    for (int x = 0; x < nPointsX; ++x) {
      float[][] plane = AU.newFloat2(nPointsY);
      voxelData[x] = plane;
      int ptyz = 0;
      for (int y = 0; y < nPointsY; ++y) {
        float[] strip = plane[y] = new float[nPointsZ];
        for (int z = 0; z < nPointsZ; ++z, ++ptyz) {
          strip[z] = getValue(x, y, z, ptyz);
        }
      }
    }
  }
  
  protected void setVolumeData() {
    // required; just that this is not an abstract class;
  }
  
  protected boolean setVolumeDataParams() {
    if (params.volumeData != null) {
      setVolumeDataV(params.volumeData);
      return true;
    }
    if (!useOriginStepsPoints) {
      return false;
    }
    volumetricOrigin.setT(params.origin);
    volumetricVectors[0].set(params.steps.x, 0, 0);
    volumetricVectors[1].set(0, params.steps.y, 0);
    volumetricVectors[2].set(0, 0, params.steps.z);
    voxelCounts[0] = (int) params.points.x;
    voxelCounts[1] = (int) params.points.y;
    voxelCounts[2] = (int) params.points.z;
    if (voxelCounts[0] < 1 || voxelCounts[1] < 1 || voxelCounts[2] < 1)
      return false;
    showGridInfo();
    return true;
  }  

  protected void showGridInfo() {
    Logger.info("grid origin  = " + params.origin);
    Logger.info("grid steps   = " + params.steps);
    Logger.info("grid points  = " + params.points);
    ptTemp.x = params.steps.x * params.points.x;
    ptTemp.y = params.steps.y * params.points.y;
    ptTemp.z = params.steps.z * params.points.z;
    Logger.info("grid lengths = " + ptTemp);
    ptTemp.add(params.origin);
    Logger.info("grid max xyz = " + ptTemp);
  }
  
  /**
   * 
   * @param index
   * @param min
   * @param max
   * @param ptsPerAngstrom
   * @param gridMax
   * @param minPointsPerAngstrom -- necessary for highly prolate models such a 6ef8
   * @return  number of grid points total
   */
  protected int setVoxelRange(int index, float min, float max,
                              float ptsPerAngstrom, int gridMax, 
                              float minPointsPerAngstrom) {
    int nGrid;
    float d;
    if (min - max >= -0.0001f) {
      min = -10;
      max = 10;
    }
    float range = max - min;
    float resolution = params.resolution;
    if (resolution != Float.MAX_VALUE) {
      ptsPerAngstrom = resolution;
      minPointsPerAngstrom = 0;
    }
      
    nGrid = (int) Math.floor(range * ptsPerAngstrom) + 1;
    if (nGrid > gridMax) {
      if ((dataType & Parameters.HAS_MAXGRID) > 0) {
        if (resolution == Float.MAX_VALUE) {
          if (!isQuiet)
            Logger.info("Maximum number of voxels for index=" + index
                + " exceeded (" + nGrid + ") -- set to " + gridMax);
          nGrid = gridMax;
        } else {
          if (!isQuiet)
            Logger.info("Warning -- high number of grid points: " + nGrid);
        }
      } else if (resolution == Float.MAX_VALUE) {
        nGrid = gridMax;
      }
    }
    if (nGrid == 1)
      nGrid = 2;
    ptsPerAngstrom = (nGrid - 1) / range;
    if (ptsPerAngstrom < minPointsPerAngstrom) {
      ptsPerAngstrom = minPointsPerAngstrom;
      nGrid = (int) Math.floor(ptsPerAngstrom * range + 1);
      ptsPerAngstrom = (nGrid - 1) / range;
    }
    d = volumeData.volumetricVectorLengths[index] = 1f / ptsPerAngstrom;
    voxelCounts[index] = nGrid;// + ((dataType & Parameters.IS_SOLVENTTYPE) != 0 ? 3 : 0);
    if (params.sbOut != null)
      params.sbOut.append("isosurface resolution for axis " + (index + 1) + " set to "
          + ptsPerAngstrom + " points/Angstrom; " + voxelCounts[index]
          + " voxels\n");
    switch (index) {
    case 0:
      volumetricVectors[0].set(d, 0, 0);
      volumetricOrigin.x = min;
      break;
    case 1:
      volumetricVectors[1].set(0, d, 0);
      volumetricOrigin.y = min;
      break;
    case 2:
      volumetricVectors[2].set(0, 0, d);
      volumetricOrigin.z = min;
      if (isEccentric)
        eccentricityMatrix.rotate(volumetricOrigin);
      if (center != null && !Float.isNaN(center.x))
        volumetricOrigin.add(center);
      if (params.sbOut != null)
        params.sbOut.append((voxelCounts[0] * voxelCounts[1] * voxelCounts[2]) + " voxels total\n");
      //System.out.println("/*volumeDatareader*/draw vector " + volumetricOrigin + " {" 
        // +volumetricVectors[0].x * voxelCounts[0] 
        //  + " " + volumetricVectors[1].y *voxelCounts[1] 
        //  + " " + volumetricVectors[2].z *voxelCounts[2] + "}");
      
    }
    if (isEccentric)
      eccentricityMatrix.rotate(volumetricVectors[index]);
    return voxelCounts[index];
  }

  @Override
  protected void readSurfaceData(boolean isMapData) throws Exception {
    readSurfaceDataVDR(isMapData);
  }
  
  protected void readSurfaceDataVDR(boolean isMapData) throws Exception {
    //precalculated -- just creating the JVXL equivalent
    if (isProgressive && !isMapData) {
      nDataPoints = volumeData.setVoxelCounts(nPointsX, nPointsY, nPointsZ);
      voxelData = null;
      return;
    }
    if (precalculateVoxelData) 
      generateCube();
    else
      readVoxelDataIndividually(isMapData);
  }

  protected void generateCube() {
    Logger.info("data type: user volumeData");
    Logger.info("voxel grid origin:" + volumetricOrigin);
    for (int i = 0; i < 3; ++i)
      Logger.info("voxel grid vector:" + volumetricVectors[i]);
    Logger.info("Read " + nPointsX + " x " + nPointsY + " x " + nPointsZ
        + " data points");
  }

  @Override
  protected void closeReader() {
    // unnecessary -- no file opened
  }

 }
