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

import javajs.util.AU;
import javajs.util.P3;
import javajs.util.SB;
import javajs.util.T3;
import javajs.util.V3;

import org.jmol.api.Interface;
import org.jmol.atomdata.AtomData;
import org.jmol.java.BS;
import org.jmol.quantum.NciCalculation;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;


abstract class VolumeFileReader extends SurfaceFileReader {

  protected boolean endOfData;
  protected boolean negativeAtomCount;
  protected int ac;
  protected int nSurfaces;
  protected boolean isAngstroms;
  protected boolean canDownsample;
  protected int[] downsampleRemainders;
  private boolean getNCIPlanes;
  protected int nData;
  protected boolean readerClosed;


  VolumeFileReader() {}
  
  @Override
  void init2(SurfaceGenerator sg, BufferedReader br) {
    init2VFR(sg, br);
  }

  void init2VFR(SurfaceGenerator sg, BufferedReader br) {
    init2SFR(sg, br);
    canDownsample = isProgressive = isXLowToHigh = true;
    jvxlData.wasCubic = true;
    boundingBox = params.boundingBox;
    if (params.qmOrbitalType == Parameters.QM_TYPE_NCI_SCF) {
      hasColorData = (params.parameters == null || params.parameters[1] >= 0);
      getNCIPlanes = true;
      params.insideOut = !params.insideOut;
    }
  }

  protected float recordData(float value) {
    if (Float.isNaN(value))
      return value;
    if (value < dataMin)
      dataMin = value;
    if (value > dataMax)
      dataMax = value;
    dataMean += value;
    nData++;
    return value;
  }

  @Override
  protected void closeReader() {
    if (readerClosed)
      return;
    readerClosed = true;
    closeReaderSFR();
    if (nData == 0 || dataMax == -Float.MAX_VALUE)
      return;
    dataMean /= nData;
    Logger.info("VolumeFileReader closing file: " + nData
        + " points read \ndata min/max/mean = " + dataMin + "/" + dataMax + "/"
        + dataMean);
  }

  @Override
  protected boolean readVolumeParameters(boolean isMapData) {
    endOfData = false;
    nSurfaces = readVolumetricHeader();
    if (nSurfaces == 0)
      return false;
    if (nSurfaces < params.fileIndex) {
      Logger
          .warn("not enough surfaces in file -- resetting params.fileIndex to "
              + nSurfaces);
      params.fileIndex = nSurfaces;
    }
    return true;
  }

  @Override
  protected boolean readVolumeData(boolean isMapData) {
    return readVolumeDataVFR(isMapData);
  }

  protected boolean readVolumeDataVFR(boolean isMapData) {
    if (!gotoAndReadVoxelData(isMapData))
      return false;
    if (!vertexDataOnly)
      Logger.info("JVXL read: " + nPointsX + " x " + nPointsY + " x "
          + nPointsZ + " data points");
    return true;
  }

  private int readVolumetricHeader() {
    try {
      readParameters();
      if (ac == Integer.MIN_VALUE)
        return 0;
      if (!vertexDataOnly)
        Logger.info("voxel grid origin:" + volumetricOrigin);
      int downsampleFactor = params.downsampleFactor;
      boolean downsampling = (canDownsample && downsampleFactor > 1);
      if (downsampleFactor > 1 && !canDownsample)
        jvxlData.msg += "\ncannot downsample this file type";
      if (downsampling) {
        downsampleRemainders = new int[3];
        Logger.info("downsample factor = " + downsampleFactor);
        for (int i = 0; i < 3; ++i) {
          int n = voxelCounts[i];
          downsampleRemainders[i] = n % downsampleFactor;
          voxelCounts[i] /= downsampleFactor;
          if (isPeriodic) {
            voxelCounts[i]++;
            downsampleRemainders[i]--;
          }
          volumetricVectors[i].scale(downsampleFactor);
          Logger.info("downsampling axis " + (i + 1) + " from " + n + " to "
              + voxelCounts[i]);
        }
      }
      if (!vertexDataOnly)
        for (int i = 0; i < 3; ++i) {
          if (!isAngstroms)
            volumetricVectors[i].scale(ANGSTROMS_PER_BOHR);
          line = voxelCounts[i] + " " + volumetricVectors[i].x + " "
              + volumetricVectors[i].y + " " + volumetricVectors[i].z;
          jvxlFileHeaderBuffer.append(line).appendC('\n');
          Logger.info("voxel grid count/vector:" + line);
        }
      scaleIsosurface(params.scale);
      volumeData.setVolumetricXml();
      return nSurfaces;
    } catch (Exception e) {
      Logger.error(e.toString());
      return 0;
    }
  }

  abstract protected void readParameters() throws Exception;

  // generally useful:

  protected String skipComments(boolean allowBlankLines) throws Exception {
    SB sb = new SB();
    while (rd() != null
        && (allowBlankLines && line.length() == 0 || line.indexOf("#") == 0))
      sb.append(line).appendC('\n');
    return sb.toString();
  }

  protected void readVoxelVector(int voxelVectorIndex) throws Exception {
    rd();
    V3 voxelVector = volumetricVectors[voxelVectorIndex];
    if ((voxelCounts[voxelVectorIndex] = parseIntStr(line)) == Integer.MIN_VALUE) //unreadable
      next[0] = line.indexOf(" ");
    voxelVector.set(parseFloat(), parseFloat(), parseFloat());
    if (isAnisotropic)
      setVectorAnisotropy(voxelVector);
  }

  protected int downsampleFactor;
  protected int nSkipX, nSkipY, nSkipZ;

  void initializeSurfaceData() {
    downsampleFactor = params.downsampleFactor;
    nSkipX = 0;
    nSkipY = 0;
    nSkipZ = 0;
    if (canDownsample && downsampleFactor > 0) {
      nSkipX = downsampleFactor - 1;
      nSkipY = downsampleRemainders[2] + (downsampleFactor - 1)
          * (nSkipZ = ((nPointsZ - (isPeriodic ? 1 : 0)) * downsampleFactor + downsampleRemainders[2]));
      nSkipZ = downsampleRemainders[1] * nSkipZ + (downsampleFactor - 1)
          * nSkipZ * ((nPointsY - (isPeriodic ? 1 : 0)) * downsampleFactor + downsampleRemainders[1]);
    }

    if (params.thePlane != null) {
      params.cutoff = 0f;
    } else if (isJvxl) {
      params.cutoff = (params.isBicolorMap || params.colorBySign ? 0.01f : 0.5f);
    }
    nDataPoints = 0;
    next[0] = 0;
    line = "";
    jvxlNSurfaceInts = 0;
  }

  @Override
  protected void readSurfaceData(boolean isMapData) throws Exception {
    readSurfaceDataVFR(isMapData);
  }

  protected void readSurfaceDataVFR(boolean isMapData) throws Exception {
    /*
     * possibilities:
     * 
     * cube file data only -- monochrome surface (single pass)
     * cube file with plane (color, two pass)
     * cube file data + cube file color data (two pass)
     * jvxl file no color data (single pass)
     * jvxl file with color data (single pass)
     * jvxl file with plane (single pass)
     * 
     * cube file with multiple MO data will be interspersed 
     * 
     * 
     */
    /* 
     * This routine is used twice in the case of color mapping. 
     * First (isMapData = false) to read the surface values, which
     * might be a plane, then (isMapData = true) to color them based 
     * on a second data set.
     * 
     * Planes are compatible with data sets that return actual 
     * numbers at all grid points -- cube files, orbitals, functionXY,
     * and solvent/molecular surface calculations.
     *  
     * It is possible to map a QM orbital onto a plane. In the first pass we defined
     * the plane; in the second pass we just calculate the new voxel values and return.
     * 
     * Starting with Jmol 11.7.25, JVXL files do not create voxelData[][][]
     * and instead just fill a bitset, thus saving nx*ny*nz*8 - (nx*ny*nz/32) bytes in memory
     * 
     */

    initializeSurfaceData();
    if (isProgressive && !isMapData || isJvxl) {
      nDataPoints = volumeData.setVoxelCounts(nPointsX, nPointsY, nPointsZ);
      voxelData = null;
      if (isJvxl)
        jvxlVoxelBitSet = getVoxelBitSet(nDataPoints);
    } else if (isMapData && volumeData.hasPlane()) {
      volumeData.setVoxelMap();
      float f = volumeData.getToPlaneParameter(); // was mappingPlane
      for (int x = 0; x < nPointsX; ++x) {
        for (int y = 0; y < nPointsY; ++y) {
          for (int z = 0; z < nPointsZ; ++z) {
            float v = recordData(getNextVoxelValue()); // was mappingPlane
            if (volumeData.isNearPlane(x, y, z, f))
              volumeData.setVoxelMapValue(x, y, z, v);
            if (nSkipX != 0)
              skipVoxels(nSkipX);
          }
          if (nSkipY != 0)
            skipVoxels(nSkipY);
        }
        if (nSkipZ != 0)
          skipVoxels(nSkipZ);
      }
    } else {
      voxelData = AU.newFloat3(nPointsX, -1);
      // Note downsampling not allowed for JVXL files
      // This filling of voxelData should only be needed
      // for mapped data.

      for (int x = 0; x < nPointsX; ++x) {
        float[][] plane = AU.newFloat2(nPointsY);
        voxelData[x] = plane;
        for (int y = 0; y < nPointsY; ++y) {
          float[] strip = new float[nPointsZ];
          plane[y] = strip;
          for (int z = 0; z < nPointsZ; ++z) {
            strip[z] = recordData(getNextVoxelValue());
            if (nSkipX != 0)
              skipVoxels(nSkipX);
          }
          if (nSkipY != 0)
            skipVoxels(nSkipY);
        }
        if (nSkipZ != 0)
          skipVoxels(nSkipZ);
      }
      //Jvxl getNextVoxelValue records the data read on its own.
    }
    volumeData.setVoxelDataAsArray(voxelData);
  }

  // For a progressive reader, we need to build two planes at a time
  // and keep them indexed. reading x low to high, we will first encounter
  // plane 0, then plane 1.
  // Note that we cannot do this when the file is being opened for
  // mapping. In that case we either need ALL the points
  // or, for some readers (IsoMOReader, NCI versions of file reader)
  // we can process mapped data as individual points in one pass.

  @Override
  public float[] getPlane(int x) {
    if (x == 0)
      initPlanes();
    if (getNCIPlanes)
      return getPlaneNCI(x);
    float[] plane = getPlaneSR(x);
    if (qpc == null)
      getPlaneVFR(plane, true);
    return plane;
  }

  private float[][] yzPlanesRaw;
  private int iPlaneNCI;

  /**
   * Retrieve raw file planes and pass them to the calculation object for
   * processing into new data.
   * 
   * Bob Hanson hansonr@stolaf.edu 6/7/2011
   * 
   * @param x
   * @return plane (for testing)
   */
  public float[] getPlaneNCI(int x) {
    float[] plane;
    if (iPlaneNCI == 0) {
      qpc = (NciCalculation) Interface
          .getOption("quantum.NciCalculation", (Viewer) sg.atomDataServer, null);
      AtomData atomData = new AtomData();
      atomData.modelIndex = -1; // -1 here means fill ALL atoms; any other
      // means "this model only"
      atomData.bsSelected = params.bsSelected;
      sg.fillAtomData(atomData, AtomData.MODE_FILL_COORDS);
      ((NciCalculation) qpc).setupCalculation(volumeData, sg.params.bsSelected, null, null,
          atomData.atoms, -1, true, null, params.parameters, params.testFlags);
      iPlaneNCI = 1;
      qpc.setPlanes(yzPlanesRaw = new float[4][yzCount]);
      if (hasColorData) {
        //float nan = qpc.getNoValue();
        getPlaneVFR(yzPlanesRaw[0], false);
        getPlaneVFR(yzPlanesRaw[1], false);
        plane = yzPlanes[0];
        for (int i = 0; i < yzCount; i++)
          plane[i] = Float.NaN;
        return plane;
      }
      iPlaneNCI = -1;
    }
    float nan = qpc.getNoValue();
    int x1 = nPointsX - 1;
    switch (iPlaneNCI) {
    case -1:
      plane = yzPlanes[x % 2];
      x1++;
      break;
    case 3:
      plane = yzPlanesRaw[0];
      yzPlanesRaw[0] = yzPlanesRaw[1];
      yzPlanesRaw[1] = yzPlanesRaw[2];
      yzPlanesRaw[2] = yzPlanesRaw[3];
      yzPlanesRaw[3] = plane;
      plane = yzPlanesRaw[iPlaneNCI];
      break;
    default:
      iPlaneNCI++;
      plane = yzPlanesRaw[iPlaneNCI];
    }
    if (x < x1) {
      getPlaneVFR(plane, false);
      qpc.calcPlane(x, plane = yzPlanes[x % 2]);
      for (int i = 0; i < yzCount; i++)
        if (plane[i] != nan)
          recordData(plane[i]);
    } else {
      for (int i = 0; i < yzCount; i++)
        plane[i] = Float.NaN;
    }
    return plane;
  }

  private void getPlaneVFR(float[] plane, boolean doRecord) {
    try {
      for (int y = 0, ptyz = 0; y < nPointsY; ++y) {
        for (int z = 0; z < nPointsZ; ++z) {
          float v = getNextVoxelValue();
          if (doRecord)
            recordData(v);
          plane[ptyz++] = v;
          if (nSkipX != 0)
            skipVoxels(nSkipX);
        }
        if (nSkipY != 0)
          skipVoxels(nSkipY);
      }
      if (nSkipZ != 0)
        skipVoxels(nSkipZ);
    } catch (Exception e) {
      // ignore
    }
  }

  protected P3[] boundingBox;

  @Override
  public float getValue(int x, int y, int z, int ptyz) {
    // if (x == 0 && ptyz + 1 == yzCount) {
    //first value -- ALWAYS send
    // }
    if (boundingBox != null) {
      volumeData.voxelPtToXYZ(x, y, z, ptTemp);
      if (ptTemp.x < boundingBox[0].x || ptTemp.x > boundingBox[1].x
          || ptTemp.y < boundingBox[0].y || ptTemp.y > boundingBox[1].y
          || ptTemp.z < boundingBox[0].z || ptTemp.z > boundingBox[1].z)
        return Float.NaN;
    }
    return getValue2(x, y, z, ptyz);
  }

  private void skipVoxels(int n) throws Exception {
    // not allowed for JVXL data
    for (int i = n; --i >= 0;)
      getNextVoxelValue();
  }

  /**
   * 
   * @param nPoints
   * @return JVXL bitset
   * @throws Exception
   */
  protected BS getVoxelBitSet(int nPoints) throws Exception {
    // jvxlReader will use this to read the surface voxel data
    return null;
  }

  protected float getNextVoxelValue() throws Exception {
    float voxelValue = 0;
    if (nSurfaces > 1 && !params.blockCubeData) {
      for (int i = 1; i < params.fileIndex; i++)
        nextVoxel();
      voxelValue = nextVoxel();
      for (int i = params.fileIndex; i < nSurfaces; i++)
        nextVoxel();
    } else {
      voxelValue = nextVoxel();
    }
    return voxelValue;
  }

  protected float nextVoxel() throws Exception {
    float voxelValue = parseFloat();
    if (Float.isNaN(voxelValue)) {
      while (rd() != null && Float.isNaN(voxelValue = parseFloatStr(line))) {
      }
      if (line == null) {
        if (!endOfData)
          Logger.warn("end of file reading cube voxel data? nBytes=" + nBytes
              + " nDataPoints=" + nDataPoints + " (line):" + line);
        endOfData = true;
        line = "0 0 0 0 0 0 0 0 0 0";
      }
    }
    //System.out.println(voxelValue);
    return voxelValue;
  }

  @Override
  protected void gotoData(int n, int nPoints) throws Exception {
    if (!params.blockCubeData)
      return;
    if (n > 0)
      Logger.info("skipping " + n + " data sets, " + nPoints + " points each");
    for (int i = 0; i < n; i++)
      skipData(nPoints);
  }

  protected void skipData(int nPoints) throws Exception {
    skipDataVFR(nPoints);
  }

  protected void skipDataVFR(int nPoints) throws Exception {
    int iV = 0;
    while (iV < nPoints)
      iV += countData(rd());
  }

  private int countData(String str) {
    int count = 0;
    int ich = 0;
    int ichMax = str.length();
    char ch;
    while (ich < ichMax) {
      while (ich < ichMax && ((ch = str.charAt(ich)) == ' ' || ch == '\t'))
        ++ich;
      if (ich < ichMax)
        ++count;
      while (ich < ichMax && ((ch = str.charAt(ich)) != ' ' && ch != '\t'))
        ++ich;
    }
    return count;
  }

  /**
   * checks an atom line for "ANGSTROMS", possibly overriding the data's natural
   * units, BOHR (similar to Gaussian CUBE files).
   * 
   * @param isXLowToHigh
   * @param isAngstroms
   * @param strAtomCount
   * @param atomLine
   * @param bs
   * @return isAngstroms
   */
  protected static boolean checkAtomLine(boolean isXLowToHigh,
                                         boolean isAngstroms,
                                         String strAtomCount, String atomLine,
                                         SB bs) {
    if (atomLine.indexOf("ANGSTROMS") >= 0)
      isAngstroms = true;
    int ac = (strAtomCount == null ? Integer.MAX_VALUE : javajs.util.PT
        .parseInt(strAtomCount));
    switch (ac) {
    case Integer.MIN_VALUE:
      ac = 0;
      atomLine = " " + atomLine.substring(atomLine.indexOf(" ") + 1);
      break;
    case Integer.MAX_VALUE:
      ac = Integer.MIN_VALUE;
      break;
    default:
      String s = "" + ac;
      atomLine = atomLine.substring(atomLine.indexOf(s) + s.length());
    }
    if (isAngstroms) {
      if (atomLine.indexOf("ANGSTROM") < 0)
        atomLine += " ANGSTROMS";
    } else {
      if (atomLine.indexOf("BOHR") < 0)
        atomLine += " BOHR";
    }
    atomLine = (ac == Integer.MIN_VALUE ? ""
        : (isXLowToHigh ? "+" : "-") + Math.abs(ac))
        + atomLine + "\n";
    bs.append(atomLine);
    return isAngstroms;
  }

  @Override
  protected float getSurfacePointAndFraction(float cutoff,
                                             boolean isCutoffAbsolute,
                                             float valueA, float valueB,
                                             T3 pointA,
                                             V3 edgeVector, int x, int y,
                                             int z, int vA, int vB,
                                             float[] fReturn, T3 ptReturn) {
    return getSPFv(cutoff, isCutoffAbsolute,
        valueA, valueB, pointA, edgeVector, x, y, z, vA, vB, fReturn, ptReturn);
  }
  
  protected float getSPFv(float cutoff, boolean isCutoffAbsolute, float valueA,
                          float valueB, T3 pointA, V3 edgeVector, int x, int y,
                          int z, int vA, int vB, float[] fReturn, T3 ptReturn) {
    float zero = getSPF(cutoff, isCutoffAbsolute,
        valueA, valueB, pointA, edgeVector, x, y, z, vA, vB, fReturn, ptReturn);
    if (qpc == null || Float.isNaN(zero) || !hasColorData)
      return zero;
    /*
     * in the case of an NCI calculation, we need to process
     * the two end points vA an vB individually, then do
     * the interpolation.
     * 
     */
    vA = marchingCubes.getLinearOffset(x, y, z, vA);
    vB = marchingCubes.getLinearOffset(x, y, z, vB);
    return qpc.process(vA, vB, fReturn[0]);
  }

  private boolean isScaledAlready;
  private void scaleIsosurface(float scale) {
    if (isScaledAlready)
      return;
    isScaledAlready = true;
    if (isAnisotropic)
      setVolumetricAnisotropy();
    if (Float.isNaN(scale))
      return;
    Logger.info("applying scaling factor of " + scale);
    volumetricOrigin.scaleAdd2((1 - scale) / 2, volumetricVectors[0], volumetricOrigin);
    volumetricOrigin.scaleAdd2((1 - scale) / 2, volumetricVectors[1], volumetricOrigin);
    volumetricOrigin.scaleAdd2((1 - scale) / 2, volumetricVectors[2], volumetricOrigin);
    volumetricVectors[0].scale(scale);
    volumetricVectors[1].scale(scale);
    volumetricVectors[2].scale(scale);
  }

  protected void swapXZ() {
    V3 v = volumetricVectors[0];
    volumetricVectors[0] = volumetricVectors[2];
    volumetricVectors[2] = v;
    int n = voxelCounts[0];
    voxelCounts[0] = voxelCounts[2];
    voxelCounts[2] = n;
    params.insideOut = !params.insideOut;
  }

}
