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
import org.jmol.jvxl.api.MeshDataServer;
import org.jmol.jvxl.api.VertexDataServer;
import org.jmol.jvxl.calc.MarchingCubes;
import org.jmol.jvxl.calc.MarchingSquares;
import org.jmol.jvxl.data.JvxlCoder;
import org.jmol.jvxl.data.JvxlData;
import org.jmol.jvxl.data.MeshData;
import org.jmol.jvxl.data.VolumeData;
import org.jmol.quantum.QuantumPlaneCalculation;
import org.jmol.util.BoxInfo;
import org.jmol.util.C;
import org.jmol.util.ColorEncoder;
import org.jmol.util.Escape;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.SB;

import org.jmol.util.Logger;

import javajs.util.OC;
import javajs.util.M3d;
import javajs.util.P3d;
import javajs.util.P3i;
import javajs.util.T3d;
import javajs.util.V3d;


public abstract class SurfaceReader implements VertexDataServer {

  /*
   * JVXL SurfaceReader Class
   * ----------------------
   * Bob Hanson, hansonr@stolaf.edu, 20 Apr 2007
   * 
   * SurfaceReader performs four functions:
   * 
   * 1) reading/creating volume scalar data ("voxels")
   * 2) generating a surface (vertices and triangles) from this set
   *      based on a specific cutoff value
   * 3) color-mapping this surface with other data
   * 4) creating JVXL format file data for this surface
   * 
   * In the case that the surface type does not include voxel data (EfvetReader), 
   * only steps 2 and 3 are involved, and no cutoff value is used.
   * 
   * SurfaceReader is an ABSTRACT class, instantiated as one of the 
   * following to perform specific functions:
   * 
   *     SurfaceReader (abstract MarchingReader)
   *          |
   *          |_______VolumeDataReader (uses provided predefined data)
   *          |          |
   *          |          |_____IsoFxyReader (creates data as needed)
   *          |          |_____IsoFxyzReader (creates data as needed)
   *          |          |_____IsoMepReader (creates predefined data)
   *          |          |_____IsoMOReader (creates predefined data)
   *          |          |_____IsoShapeReader (creates data as needed)
   *          |          |_____IsoSolventReader (creates predefined data)
   *          |                    |___IsoPlaneReader (predefines data)
   *          |          
   *          |_______SurfaceFileReader (abstract)
   *                    |
   *                    |_______VolumeFileReader (abstract)
   *                    |           |
   *                    |           |______ApbsReader
   *                    |           |______CubeReader
   *                    |           |______DXReader
   *                    |           |______JaguarReader
   *                    |           |______JvxlXmlReader
   *                    |           |           |______JvxlReader
   *                    |           |
   *                    |           |______ElectronDensityFileReader (abstract)
   *                    |                       |______MrcBinaryReader
   *                    |                       |______XplorReader (version 3.1)
   *                    |
   *                    |_______PolygonFileReader (abstract)
   *                                |
   *                                |______EfvetReader
   *                                |______PmeshReader
   *
   * The first step is to create a VolumeData structure:
   * 
   *   public final Point3f volumetricOrigin = new Point3f();
   *   public final Vector3f[] volumetricVectors = new Vector3f[3];
   *   public final int[] voxelCounts = new int[3];
   *   public double[][][] voxelData;
   * 
   * such as exists in a CUBE file.
   * 
   * The second step is to use the Marching Cubes algorithm to 
   * create a surface set of vertices and triangles. The data structure
   * involved for that is MeshData, containing:
   * 
   *   public int vertexCount;
   *   public Point3f[] vertices;
   *   public double[] vertexValues;
   *   public int polygonCount;
   *   public int[][] polygonIndexes;
   *   
   * The third (optional) step is to color those vertices using
   * a set of color index values provided by a color encoder. This
   * data is also stored in MeshData:  
   *   
   *   public short[] vertexColixes; 
   * 
   * Finally -- actually, throughout the process -- SurfaceReader
   * creates a JvxlData structure containing the critical information
   * that is necessary for creating Jvxl surface data files. For that,
   * we have the JvxlData structure. 
   * 
   * Two interfaces are defined, and more should be. These include 
   * VertexDataServer and MeshDataServer.
   * 
   * VertexDataServer
   * ----------------
   * 
   * contains three methods, getSurfacePointIndex, addVertexCopy, and addTriangleCheck.
   * 
   * These deliver MarchingCubes and MarchingSquares vertex data in 
   * return for a vertex index number that can later be used for defining
   * a set of triangles.
   * 
   * SurfaceReader implements this interface.
   * 
   * 
   * MeshDataServer extends VertexDataServer
   * ---------------------------------------
   * 
   * contains additional methods that allow for later processing 
   * of the vertex/triangle data:
   * 
   *   public abstract void invalidateTriangles();
   *   public abstract void fillMeshData(MeshData meshData, int mode);
   *   public abstract void notifySurfaceGenerationCompleted();
   *   public abstract void notifySurfaceMappingCompleted();
   * 
   * Note that, in addition to these interfaces, some of the readers,
   * namely IsoFxyReader, IsoMepReader,IsoMOReader, and IsoSolvenReader
   * and (due to subclassing) IsoPlaneReader all currently require
   * direct connections to Jmol Viewer and Atom classes.   
   * 
   * 
   * The rough outline of Jvxl files is 
   * given below:
   * 

   #comments (optional)
   info line1
   info line2
   -na originx originy originz   [ANGSTROMS/BOHR] optional; BOHR assumed
   n1 x y z
   n2 x y z
   n3 x y z
   a1 a1.0 x y z
   a2 a2.0 x y z
   a3 a3.0 x y z
   a4 a4.0 x y z 
   etc. -- na atoms
   -ns 35 90 35 90 Jmol voxel format version 1.0
   # more comments
   cutoff +/-nEdges +/-nVertices [more here]
   integer inside/outside edge data
   ascii-encoded fractional edge data
   ascii-encoded fractional color data
   # optional comments

   * 
   * 
   * 
   * 
   */

  protected SurfaceGenerator sg;
  protected MeshDataServer meshDataServer;

  protected Parameters params;
  protected MeshData meshData;
  protected JvxlData jvxlData;
  VolumeData volumeData;
  private String edgeData;

  protected boolean haveSurfaceAtoms = false;
  protected boolean allowSigma = false;
  protected boolean isProgressive = false;
  protected boolean isXLowToHigh = false; //can be overridden in some readers by --progressive
  private double assocCutoff = 0.3d;
  protected boolean isQuiet;
  protected boolean isPeriodic;
  
  boolean vertexDataOnly;
  boolean hasColorData;

  protected double dataMin = Double.MAX_VALUE;
  protected double dataMax = -Double.MAX_VALUE;
  protected double dataMean;
  protected P3d xyzMin, xyzMax;

  protected P3d center;
  protected double[] anisotropy;
  protected boolean isAnisotropic;
  protected M3d eccentricityMatrix;
  protected M3d eccentricityMatrixInverse;
  protected boolean isEccentric;
  protected double eccentricityScale;
  protected double eccentricityRatio;

  SurfaceReader() {}
  
  /**
   * implemented in SurfaceFileReader and 
   * 
   * @param sg
   */
  abstract void init(SurfaceGenerator sg);

  void initSR(SurfaceGenerator sg) {
    this.sg = sg;
    params = sg.params;
    assocCutoff = params.assocCutoff;
    isXLowToHigh = params.isXLowToHigh;
    center = params.center;
    anisotropy = params.anisotropy;
    isAnisotropic = params.isAnisotropic;
    eccentricityMatrix = params.eccentricityMatrix;
    eccentricityMatrixInverse = params.eccentricityMatrixInverse;
    isEccentric = params.isEccentric;
    eccentricityScale = params.eccentricityScale;
    eccentricityRatio = params.eccentricityRatio;
    marchingSquares = sg.marchingSquares;
    meshData = sg.meshData;
    jvxlData = sg.jvxlData;
    setVolumeDataV(sg.volumeDataTemp); // initialize volume data to surfaceGenerator's
    meshDataServer = sg.meshDataServer;
    cJvxlEdgeNaN = (char) (JvxlCoder.defaultEdgeFractionBase + JvxlCoder.defaultEdgeFractionRange);
  }

  final static double ANGSTROMS_PER_BOHR = 0.5291772f;
  final static double defaultMappedDataMin = 0d;
  final static double defaultMappedDataMax = 1.0d;
  final static double defaultCutoff = 0.02f;

  private int edgeCount;

  protected P3d volumetricOrigin;
  protected V3d[] volumetricVectors;
  protected int[] voxelCounts;
  protected double[][][] voxelData;
  
  abstract protected void closeReader();
  
  /**
   * 
   * @param out
   */
  protected void setOutputChannel(OC out) {
    // only for file readers
  }
 
  protected void newVoxelDataCube() {
    volumeData.setVoxelDataAsArray(voxelData = new double[nPointsX][nPointsY][nPointsZ]);
  }

  protected void setVolumeDataV(VolumeData v) {
    nBytes = 0;
    volumetricOrigin = v.volumetricOrigin;
    volumetricVectors = v.volumetricVectors;
    voxelCounts = v.voxelCounts;
    voxelData = v.getVoxelData();
    volumeData = v;
  }

  protected abstract boolean readVolumeParameters(boolean isMapData);

  protected abstract boolean readVolumeData(boolean isMapData);

  ////////////////////////////////////////////////////////////////
  // CUBE/APBS/JVXL file reading stuff
  ////////////////////////////////////////////////////////////////

  protected long nBytes;
  protected int nDataPoints;
  protected int nPointsX, nPointsY, nPointsZ;

  protected boolean isJvxl;

  protected int edgeFractionBase;
  protected int edgeFractionRange;
  protected int colorFractionBase;
  protected int colorFractionRange;

  protected SB jvxlFileHeaderBuffer;
  protected SB fractionData;
  protected String jvxlEdgeDataRead = "";
  protected String jvxlColorDataRead = "";
  protected BS jvxlVoxelBitSet;
  protected boolean jvxlDataIsColorMapped;
  protected boolean jvxlDataIsPrecisionColor;
  protected boolean jvxlDataIs2dContour;
  protected boolean jvxlDataIsColorDensity;
  protected double jvxlCutoff;
  protected double[] jvxlCutoffRange;
  protected int jvxlNSurfaceInts;
  protected char cJvxlEdgeNaN = '\0';

  protected int contourVertexCount;

  void jvxlUpdateInfo() {
    jvxlData.jvxlUpdateInfo(params.title, nBytes);
  }

  boolean readAndSetVolumeParameters(boolean isMapData) {
    if (!readVolumeParameters(isMapData))
        return false;
    if (vertexDataOnly)
      return true;
    //if (volumeData.sr != null)
      //return true;
    return (volumeData.setUnitVectors());// || isMapData && params.thePlane != null);
    
//        && (vertexDataOnly 
//            || isMapData && params.thePlane != null 
//            || volumeData.sr != null
//            || volumeData.setUnitVectors()));
  }

  boolean createIsosurface(boolean justForPlane) {
    resetIsosurface();
    if (params.showTiming)
      Logger.startTimer("isosurface creation");
    jvxlData.cutoff = Double.NaN;
    jvxlData.cutoffRange = null;
    if (!readAndSetVolumeParameters(justForPlane))
      return false;
    if (!justForPlane && !Double.isNaN(params.sigma) && !allowSigma) {
      if (params.sigma > 0)
        Logger
            .error("Reader does not support SIGMA option -- using cutoff 1.6");
      params.cutoff = 1.6d;
    }
    // negative sigma just ignores the error message
    // and means it was inserted by Jmol as a default option
    if (params.sigma < 0)
      params.sigma = -params.sigma;
    nPointsX = voxelCounts[0];
    nPointsY = voxelCounts[1];
    nPointsZ = voxelCounts[2];
    jvxlData.isSlabbable = ((params.dataType & Parameters.IS_SLABBABLE) != 0);
    jvxlData.insideOut = params.isInsideOut();
    jvxlData.isBicolorMap = params.isBicolorMap;
    jvxlData.nPointsX = nPointsX;
    jvxlData.nPointsY = nPointsY;
    jvxlData.nPointsZ = nPointsZ;
    jvxlData.jvxlVolumeDataXml = volumeData.xmlData;
    jvxlData.voxelVolume = volumeData.voxelVolume;
    if (justForPlane) {
      //double[][][] voxelDataTemp =  volumeData.voxelData;
      volumeData.setMappingPlane(params.thePlane);
      //volumeData.setDataDistanceToPlane(params.thePlane);
      if (meshDataServer != null)
        meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES, null);
      params.setMapRanges(this, false);
      generateSurfaceData();
      volumeData.setMappingPlane(null);

      //if (volumeData != null)
      //  volumeData.voxelData = voxelDataTemp;
    } else {
      if (!readVolumeData(false))
        return false;
      generateSurfaceData();
    }
    
    if (jvxlFileHeaderBuffer == null) {
      jvxlData.jvxlFileTitle = "";
      jvxlData.jvxlFileSource = null;
      jvxlData.jvxlFileMessage = null;
    } else {
      String s = jvxlFileHeaderBuffer.toString();
      int i = s.indexOf('\n', s.indexOf('\n', s.indexOf('\n') + 1) + 1) + 1;
      jvxlData.jvxlFileTitle = s.substring(0, i);
      jvxlData.jvxlFileSource = params.fileName;
    }
    if (params.contactPair == null)
      setBBoxAll();
    jvxlData.isValid = (xyzMin.x != Double.MAX_VALUE);
    if (!params.isSilent) {
      if (!jvxlData.isValid)
        Logger.error("no isosurface points were found!");
      else
        Logger.info("boundbox corners " + Escape.eP(xyzMin) + " "
          + Escape.eP(xyzMax));
    }
    jvxlData.boundingBox = new P3d[] { xyzMin, xyzMax };
    jvxlData.dataMin = dataMin;
    jvxlData.dataMax = dataMax;
    jvxlData.cutoff = (isJvxl ? jvxlCutoff : params.cutoff);
    jvxlData.cutoffRange = (isJvxl ? jvxlCutoffRange : params.cutoffRange);
    jvxlData.isCutoffAbsolute = params.isCutoffAbsolute;
    jvxlData.isModelConnected = params.isModelConnected;
    jvxlData.pointsPerAngstrom = 1d / volumeData.volumetricVectorLengths[0];
    jvxlData.jvxlColorData = "";
    jvxlData.jvxlPlane = params.thePlane;
    jvxlData.jvxlEdgeData = edgeData;
    jvxlData.isBicolorMap = params.isBicolorMap;
    jvxlData.isContoured = params.isContoured;
    jvxlData.colorDensity = params.colorDensity;
    jvxlData.pointSize = params.pointSize;
    if (jvxlData.vContours != null)
      params.nContours = jvxlData.vContours.length;
    jvxlData.nContours = (params.contourFromZero ? params.nContours : -1
        - params.nContours);
    jvxlData.thisContour = params.thisContour;
    jvxlData.nEdges = edgeCount;
    jvxlData.edgeFractionBase = edgeFractionBase;
    jvxlData.edgeFractionRange = edgeFractionRange;
    jvxlData.colorFractionBase = colorFractionBase;
    jvxlData.colorFractionRange = colorFractionRange;
    jvxlData.jvxlDataIs2dContour = jvxlDataIs2dContour;
    jvxlData.jvxlDataIsColorMapped = jvxlDataIsColorMapped;
    jvxlData.jvxlDataIsColorDensity = jvxlDataIsColorDensity;
    jvxlData.isXLowToHigh = isXLowToHigh;
    jvxlData.vertexDataOnly = vertexDataOnly;
    jvxlData.saveVertexCount = 0;

    if (jvxlDataIsColorMapped || jvxlData.nVertexColors > 0) {
      if (meshDataServer != null) {
        meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES, null);
        meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_COLOR_INDEXES,
            null);
      }
      jvxlData.jvxlColorData = readColorData();
      updateSurfaceData();
      if (meshDataServer != null)
        meshDataServer.notifySurfaceMappingCompleted();
    }
    if (params.showTiming)
      Logger.checkTimer("isosurface creation", false);
    return true;
  }

  void resetIsosurface() {
    meshData = new MeshData();
    xyzMin = xyzMax = null;
    jvxlData.isBicolorMap = params.isBicolorMap;
    if (meshDataServer != null)
      meshDataServer.fillMeshData(null, 0, null);
    contourVertexCount = 0;
    if (params.cutoff == Double.MAX_VALUE)
      params.cutoff = defaultCutoff;
    jvxlData.jvxlSurfaceData = "";
    jvxlData.jvxlEdgeData = "";
    jvxlData.jvxlColorData = "";
    //TODO: more resets of jvxlData?
    edgeCount = 0;
    edgeFractionBase = JvxlCoder.defaultEdgeFractionBase;
    edgeFractionRange = JvxlCoder.defaultEdgeFractionRange;
    colorFractionBase = JvxlCoder.defaultColorFractionBase;
    colorFractionRange = JvxlCoder.defaultColorFractionRange;
    params.mappedDataMin = Double.MAX_VALUE;
  }

  void discardTempData(boolean discardAll) {
    discardTempDataSR(discardAll);
  }

  protected void discardTempDataSR(boolean discardAll) {
    if (!discardAll)
      return;
    voxelData = null;
    sg.marchingSquares = marchingSquares = null;
    marchingCubes = null;
  }

  void initializeVolumetricData() {
    nPointsX = voxelCounts[0];
    nPointsY = voxelCounts[1];
    nPointsZ = voxelCounts[2];
    setVolumeDataV(volumeData);
  }

  // this needs to be specific for each reader
  abstract protected void readSurfaceData(boolean isMapData) throws Exception;

  protected boolean gotoAndReadVoxelData(boolean isMapData) {
    //overloaded in jvxlReader
    initializeVolumetricData();
    if (nPointsX > 0 && nPointsY > 0 && nPointsZ > 0)
      try {
        gotoData(params.fileIndex - 1, nPointsX * nPointsY * nPointsZ);
        readSurfaceData(isMapData);
      } catch (Exception e) {
        Logger.error(e.toString());
        return false;
      }
    return true;
  }

  /**
   * 
   * @param n
   * @param nPoints
   * @throws Exception
   */
  protected void gotoData(int n, int nPoints) throws Exception {
    //only for file reader
  }

  protected String readColorData() {
    if (jvxlData.vertexColors == null)
      return "";
    int vertexCount = jvxlData.vertexCount;
    short[] colixes = meshData.vcs;
    double[] vertexValues = meshData.vvs;
    if (colixes == null || colixes.length < vertexCount)
      meshData.vcs = colixes = new short[vertexCount];
    if (vertexValues == null || vertexValues.length < vertexCount)
      meshData.vvs = vertexValues = new double[vertexCount];
    for (int i = 0; i < vertexCount; i++)
      colixes[i] = C.getColix(jvxlData.vertexColors[i]);
    return "-";
  }

  ////////////////////////////////////////////////////////////////
  // marching cube stuff
  ////////////////////////////////////////////////////////////////

  protected MarchingSquares marchingSquares;
  protected MarchingCubes marchingCubes;

  protected double[][] yzPlanes;
  protected int yzCount;

  protected QuantumPlaneCalculation qpc;
  
  @Override
  public double[] getPlane(int x) {
    return getPlaneSR(x);
  }

  protected double[] getPlaneSR(int x) {
    if (yzCount == 0)
      initPlanes();
    if (qpc != null) // NCICalculation only
      qpc.getPlane(x, yzPlanes[x % 2]);
    return yzPlanes[x % 2];
  }

  void initPlanes() {
    yzCount = nPointsY * nPointsZ;
    if (!isQuiet)
      Logger.info("reading data progressively -- yzCount = " + yzCount);
    yzPlanes = AU.newDouble2(2);
    yzPlanes[0] = new double[yzCount];
    yzPlanes[1] = new double[yzCount];
  }

  @Override
  public double getValue(int x, int y, int z, int ptyz) {
    return getValue2(x, y, z, ptyz);
  }

  protected double getValue2(int x, int y, int z, int ptyz) {
    return (yzPlanes == null ? voxelData[x][y][z] : yzPlanes[x % 2][ptyz]);
  }

  private void generateSurfaceData() {
    edgeData = "";
    if (vertexDataOnly) {
      try {
        readSurfaceData(false);
      } catch (Exception e) {
        System.out.println(e.toString());
        Logger.error("Exception in SurfaceReader::readSurfaceData: "
            + e.toString());
      }
      return;
    }
    contourVertexCount = 0;
    int contourType = -1;
    marchingSquares = null;

    if (params.thePlane != null || params.isContoured) {
      marchingSquares = new MarchingSquares(this, volumeData, params.thePlane,
          params.contoursDiscrete, params.nContours, params.thisContour,
          params.contourFromZero);
      contourType = marchingSquares.contourType;
      marchingSquares.setMinMax(params.valueMappedToRed,
          params.valueMappedToBlue);
    }
    params.contourType = contourType;
    params.isXLowToHigh = isXLowToHigh;
    marchingCubes = new MarchingCubes(this, volumeData, params, jvxlVoxelBitSet);
    String data = marchingCubes.getEdgeData();
    if (params.thePlane == null)
      edgeData = data;
    jvxlData.setSurfaceInfoFromBitSetPts(marchingCubes.bsVoxels,
        params.thePlane, params.mapLattice);
    jvxlData.jvxlExcluded = params.bsExcluded;
    if (isJvxl)
      edgeData = jvxlEdgeDataRead;
    postProcessVertices();
  }

  protected void postProcessVertices() {
    // optional
  }
  
  /////////////////  MarchingReader Interface Methods ///////////////////

  protected final P3d ptTemp = new P3d();

  @Override
  public int getSurfacePointIndexAndFraction(double cutoff, boolean isCutoffAbsolute,
                                  int x, int y, int z, P3i offset, int vA,
                                  int vB, double valueA, double valueB,
                                  T3d pointA, V3d edgeVector,
                                  boolean isContourType, double[] fReturn) {
    double thisValue = getSurfacePointAndFraction(cutoff, isCutoffAbsolute, valueA,
        valueB, pointA, edgeVector, x, y, z, vA, vB, fReturn, ptTemp);
    /* 
     * from MarchingCubes
     * 
     * In the case of a setup for a Marching Squares calculation,
     * we are collecting just the desired type of intersection for the 2D marching
     * square contouring -- x, y, or z. In the case of a contoured f(x,y) surface, 
     * we take every point.
     * 
     */
        
    if (marchingSquares != null && params.isContoured)
      return marchingSquares.addContourVertex(ptTemp, cutoff);
    int assocVertex = (assocCutoff > 0 ? (fReturn[0] < assocCutoff ? vA
        : fReturn[0] > 1 - assocCutoff ? vB : MarchingSquares.CONTOUR_POINT)
        : MarchingSquares.CONTOUR_POINT);
    if (assocVertex >= 0)
      assocVertex = marchingCubes.getLinearOffset(x, y, z, assocVertex);
    int n = addVertexCopy(ptTemp, thisValue, assocVertex, true);
    if (n >= 0 && params.iAddGridPoints) {
      marchingCubes.calcVertexPoint(x, y, z, vB, ptTemp);
      addVertexCopy(valueA < valueB ? pointA : ptTemp, Math.min(valueA, valueB),
          MarchingSquares.EDGE_POINT, true);
      addVertexCopy(valueA < valueB ? ptTemp : pointA, Math.max(valueA, valueB),
          MarchingSquares.EDGE_POINT, true);
    }
    return n;
  }

  protected double getSurfacePointAndFraction(double cutoff, boolean isCutoffAbsolute,
                                   double valueA, double valueB, T3d pointA,
                                   V3d edgeVector, int x,
                                   int y, int z, int vA, int vB, double[] fReturn, T3d ptReturn) {
    // will be subclassed in many cases.
    // JavaScript optimization: DO NOT CALL THIS method from subclassed method of the same name!
    return getSPF(cutoff, isCutoffAbsolute, valueA, valueB, pointA, edgeVector, x, y, z, vA, vB, fReturn, ptReturn);
  }

  /**
   * 
   * @param cutoff
   * @param isCutoffAbsolute
   * @param valueA
   * @param valueB
   * @param pointA
   * @param edgeVector
   * @param x TODO
   * @param y TODO
   * @param z TODO
   * @param vA
   * @param vB
   * @param fReturn
   * @param ptReturn
   * @return          fractional distance from A to B
   */
  protected double getSPF(double cutoff, boolean isCutoffAbsolute, double valueA,
                     double valueB, T3d pointA, V3d edgeVector, int x, int y,
                     int z, int vA, int vB, double[] fReturn, T3d ptReturn) {

    //JvxlReader may or may not call this
    //IsoSolventReader overrides this for nonlinear Marching Cubes (12.1.29)

    double diff = valueB - valueA;
    double fraction = (cutoff - valueA) / diff;
    if (isCutoffAbsolute && (fraction < 0 || fraction > 1))
      fraction = (-cutoff - valueA) / diff;

    if (fraction < 0 || fraction > 1) {
      //Logger.error("problem with unusual fraction=" + fraction + " cutoff="
      //  + cutoff + " A:" + valueA + " B:" + valueB);
      fraction = Double.NaN;
    }
    fReturn[0] = fraction;
    ptReturn.scaleAdd2(fraction, edgeVector, pointA);
    return valueA + fraction * diff;
  }

  @Override
  public int addVertexCopy(T3d vertexXYZ, double value, int assocVertex, boolean asCopy) {
    return addVC(vertexXYZ, value, assocVertex, asCopy);
  }

  protected int addVC(T3d vertexXYZ, double value, int assocVertex, boolean asCopy) {
    return (Double.isNaN(value) && assocVertex != MarchingSquares.EDGE_POINT ? -1
      : meshDataServer == null ? meshData.addVertexCopy(vertexXYZ, value, assocVertex, asCopy) : meshDataServer.addVertexCopy(vertexXYZ, value, assocVertex, asCopy));
  }

  @Override
  public int addTriangleCheck(int iA, int iB, int iC, int check, int iContour,
                              boolean isAbsolute, int color) {
    if (marchingSquares != null && params.isContoured) {
      if (color == 0) // from marching cubes 
        return marchingSquares.addTriangle(iA, iB, iC, check, iContour);
      color = 0; // from marchingSquares
    }
    return (meshDataServer != null ? meshDataServer.addTriangleCheck(iA, iB,
        iC, check, iContour, isAbsolute, color) : isAbsolute
        && !MeshData.checkCutoff(iA, iB, iC, meshData.vvs) ? -1
        : meshData.addTriangleCheck(iA, iB, iC, check, iContour, color));
  }

  ////////////////////////////////////////////////////////////////
  // color mapping methods
  ////////////////////////////////////////////////////////////////

  void colorIsosurface() {
    if (params.isSquared && volumeData != null)
      volumeData.filterData(true, Double.NaN);
/*    if (params.isContoured && marchingSquares == null) {
      //    if (params.isContoured && !(jvxlDataIs2dContour || params.thePlane != null)) {
      Logger.error("Isosurface error: Cannot contour this type of data.");
      return;
    }
*/
    if (meshDataServer != null) {
      meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES, null);
    }

    jvxlData.saveVertexCount = 0;
    if (params.isContoured && marchingSquares != null) {
      initializeMapping();
      params.setMapRanges(this, false);
      marchingSquares.setMinMax(params.valueMappedToRed,
          params.valueMappedToBlue);
      jvxlData.saveVertexCount = marchingSquares.contourVertexCount;
      contourVertexCount = marchingSquares
          .generateContourData(jvxlDataIs2dContour, (params.isSquared ? 1e-8d : 1e-4d));      
      jvxlData.contourValuesUsed = marchingSquares.contourValuesUsed;
      minMax = marchingSquares.getMinMax();
      if (meshDataServer != null)
        meshDataServer.notifySurfaceGenerationCompleted();
      finalizeMapping();
    }

    applyColorScale();
    jvxlData.nContours = (params.contourFromZero 
        ? params.nContours : -1 - params.nContours);
    jvxlData.thisContour = params.thisContour;
    jvxlData.jvxlFileMessage = "mapped: min = " + params.valueMappedToRed
        + "; max = " + params.valueMappedToBlue;
  }

  void applyColorScale() {
    colorFractionBase = jvxlData.colorFractionBase = JvxlCoder.defaultColorFractionBase;
    colorFractionRange = jvxlData.colorFractionRange = JvxlCoder.defaultColorFractionRange;
    if (params.colorPhase == 0)
      params.colorPhase = 1;
    if (meshDataServer == null) {
      meshData.vcs = new short[meshData.vc];
    } else {
      meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES, null);
      if (params.contactPair == null)
        meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_COLOR_INDEXES,
            null);
    }
    //colorBySign is true when colorByPhase is true, but not vice-versa
    //old: boolean saveColorData = !(params.colorByPhase && !params.isBicolorMap && !params.colorBySign); //sorry!
    boolean saveColorData = (params.colorDensity || params.isBicolorMap
        || params.colorBySign || !params.colorByPhase);
    if (params.contactPair != null)
      saveColorData = false;
    // colors mappable always now
    jvxlData.isJvxlPrecisionColor = true;
    jvxlData.vertexCount = (contourVertexCount > 0 ? contourVertexCount
        : meshData.vc);
    jvxlData.minColorIndex = -1;
    jvxlData.maxColorIndex = 0;
    jvxlData.contourValues = params.contoursDiscrete;
    jvxlData.isColorReversed = params.isColorReversed;
    if (!params.colorDensity)
      if (params.isBicolorMap && !params.isContoured || params.colorBySign) {
        jvxlData.minColorIndex = C
            .getColixTranslucent3(C.getColix(params.isColorReversed ? params.colorPos
                : params.colorNeg), jvxlData.translucency != 0, jvxlData.translucency);
        jvxlData.maxColorIndex = C
        .getColixTranslucent3(C.getColix(params.isColorReversed ? params.colorNeg
                : params.colorPos), jvxlData.translucency != 0, jvxlData.translucency);
      }
    jvxlData.isTruncated = (jvxlData.minColorIndex >= 0 && !params.isContoured);
    boolean useMeshDataValues = jvxlDataIs2dContour
        ||
        //      !jvxlDataIs2dContour && (params.isContoured && jvxlData.jvxlPlane != null || 
        hasColorData || vertexDataOnly || params.colorDensity || params.isBicolorMap
        && !params.isContoured;
    if (!useMeshDataValues) {
      if (haveSurfaceAtoms && meshData.vertexSource == null)
        meshData.vertexSource = new int[meshData.vc];
      double min = Double.MAX_VALUE;
      double max = -Double.MAX_VALUE;
      double value;
      initializeMapping();
      for (int i = meshData.vc; --i >= meshData.mergeVertexCount0;) {
        /* right, so what we are doing here is setting a range within the 
         * data for which we want red-->blue, but returning the actual
         * number so it can be encoded more precisely. This turned out to be
         * the key to making the JVXL contours work.
         *  
         */
        if (params.colorBySets) {
          value = meshData.vertexSets[i];
        } else if (params.colorByPhase) {
          value = getPhase(meshData.vs[i]);
        //else if (jvxlDataIs2dContour)
        //marchingSquares
        //    .getInterpolatedPixelValue(meshData.vertices[i]);
        } else {
          boolean needSource = haveSurfaceAtoms;//(haveSurfaceAtoms && meshData.vertexSource[i] < 0);
          value = volumeData.lookupInterpolatedVoxelValue(meshData.vs[i], needSource);
          //System.out.println(i + " " + meshData.vertices[i] + " " + value);
          if (needSource)
            meshData.vertexSource[i] = getSurfaceAtomIndex();
        }
        if (value < min)
          min = value;
        if (value > max && value != Double.MAX_VALUE)
          max = value;
        meshData.vvs[i] = value;
      }
      if (params.rangeSelected && minMax == null)
        minMax = new double[] { min, max };
      finalizeMapping();
    }
    params.setMapRanges(this, true);
    jvxlData.mappedDataMin = params.mappedDataMin;
    jvxlData.mappedDataMax = params.mappedDataMax;
    jvxlData.valueMappedToRed = params.valueMappedToRed;
    jvxlData.valueMappedToBlue = params.valueMappedToBlue;
    if (params.contactPair == null && jvxlData.vertexColors == null)
      colorData();
    
    JvxlCoder.jvxlCreateColorData(jvxlData,
        (saveColorData ? meshData.vvs : null));

    if (haveSurfaceAtoms && meshDataServer != null)
      meshDataServer.fillMeshData(meshData, MeshData.MODE_PUT_VERTICES, null);

    if (meshDataServer != null && params.colorBySets)
      meshDataServer.fillMeshData(meshData, MeshData.MODE_PUT_SETS, null);
  }

  private void colorData() {

    double[] vertexValues = meshData.vvs;
    short[] vertexColixes = meshData.vcs;
    meshData.pcs = null;
    double valueBlue = jvxlData.valueMappedToBlue;
    double valueRed = jvxlData.valueMappedToRed;
    short minColorIndex = jvxlData.minColorIndex;
    short maxColorIndex = jvxlData.maxColorIndex;
    if (params.colorEncoder == null)
      params.colorEncoder = new ColorEncoder(null, null);
    params.colorEncoder.setRange(params.valueMappedToRed,
        params.valueMappedToBlue, params.isColorReversed);
    for (int i = meshData.vc; --i >= 0;) {
      double value = vertexValues[i];
      if (minColorIndex >= 0) {
        if (value <= 0)
          vertexColixes[i] = minColorIndex;
        else if (value > 0)
          vertexColixes[i] = maxColorIndex;
      } else {
        if (value <= valueRed)
          value = valueRed;
        if (value >= valueBlue)
          value = valueBlue;         
        vertexColixes[i] = params.colorEncoder.getColorIndex(value);
      }
    }

    if ((params.nContours > 0 || jvxlData.contourValues != null) && jvxlData.contourColixes == null) {
      int n = (jvxlData.contourValues == null ? params.nContours : jvxlData.contourValues.length);
      short[] colors = jvxlData.contourColixes = new short[n];
      double[] values = jvxlData.contourValues;
      if (values == null)
        values = jvxlData.contourValuesUsed;
      if (jvxlData.contourValuesUsed == null)
        jvxlData.contourValuesUsed = (values == null ? new double[n] : values);
      double dv = (valueBlue - valueRed) / (n + 1);
      // n + 1 because we want n lines between n + 1 slices
      params.colorEncoder.setRange(params.valueMappedToRed,
          params.valueMappedToBlue, params.isColorReversed);
      for (int i = 0; i < n; i++) {
        double v = (values == null ? valueRed + (i + 1) * dv : values[i]);
        jvxlData.contourValuesUsed[i] = v;
        colors[i] = C.getColixTranslucent(params.colorEncoder.getArgb((double) v));
      }
      //TODO -- this strips translucency
      jvxlData.contourColors = C.getHexCodes(colors);
    }
  }
  
  private final static String[] colorPhases = { "_orb", "x", "y", "z", "xy",
      "yz", "xz", "x2-y2", "z2" };

  static int getColorPhaseIndex(String color) {
    int colorPhase = -1;
    for (int i = 0; i < colorPhases.length; i++)
      if (color.equalsIgnoreCase(colorPhases[i])) {
        colorPhase = i;
        break;
      }
    return colorPhase;
  }

  private double getPhase(T3d pt) {
    switch (params.colorPhase) {
    case 0:
    case -1:
    case 1:
      return (pt.x > 0 ? 1 : -1);
    case 2:
      return (pt.y > 0 ? 1 : -1);
    case 3:
      return (pt.z > 0 ? 1 : -1);
    case 4:
      return (pt.x * pt.y > 0 ? 1 : -1);
    case 5:
      return (pt.y * pt.z > 0 ? 1 : -1);
    case 6:
      return (pt.x * pt.z > 0 ? 1 : -1);
    case 7:
      return (pt.x * pt.x - pt.y * pt.y > 0 ? 1 : -1);
    case 8:
      return (pt.z * pt.z * 2d - pt.x * pt.x - pt.y * pt.y > 0 ? 1 : -1);
    }
    return 1;
  }

  protected double[] minMax;

  public double[] getMinMaxMappedValues(boolean haveData) {
    if (minMax != null && minMax[0] != Double.MAX_VALUE)
      return minMax;
    if (params.colorBySets)
      return (minMax = new double[] { 0, Math.max(meshData.nSets - 1, 0) });
    double min = Double.MAX_VALUE;
    double max = -Double.MAX_VALUE;
    if (params.usePropertyForColorRange && params.theProperty != null) {
      for (int i = params.theProperty.length; --i >= 0;) {
        if (params.rangeSelected && !params.bsSelected.get(i))
          continue;
        double p = params.theProperty[i];
        if (Double.isNaN(p))
          continue;
        if (p < min)
          min = p;
        if (p > max)
          max = p;
      }
      return (minMax = new double[] { min, max });
    }
    int vertexCount = (contourVertexCount > 0 ? contourVertexCount
        : meshData.vc);
    T3d[] vertexes = meshData.vs;
    boolean useVertexValue = (haveData || jvxlDataIs2dContour || vertexDataOnly || params.colorDensity);
    for (int i = meshData.mergeVertexCount0; i < vertexCount; i++) {
      double v;
      if (useVertexValue)
        v = meshData.vvs[i];
      else
        v = volumeData.lookupInterpolatedVoxelValue(vertexes[i], false);
      if (v < min)
        min = v;
      if (v > max && v != Double.MAX_VALUE)
        max = v;
    }
    return (minMax = new double[] { min, max });
  }

  void updateTriangles() {
    if (meshDataServer == null) {
      meshData.invalidatePolygons();
    } else {
      meshDataServer.invalidateTriangles();
    }
  }

  void updateSurfaceData() {
    meshData.setVertexSets(true);
    updateTriangles();
    if (params.bsExcluded[1] == null)
      params.bsExcluded[1] = new BS();
    meshData.updateInvalidatedVertices(params.bsExcluded[1]);
  }

  /**
   * 
   * @param doExclude
   */
  public void selectPocket(boolean doExclude) {
    // solvent reader implements this
  }

  void excludeMinimumSet() {
    if (meshDataServer != null)
      meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES, null);
    meshData.getSurfaceSet();
    BS bs;
    for (int i = meshData.nSets; --i >= 0;)
      if ((bs = meshData.surfaceSet[i]) != null 
          && bs.cardinality() < params.minSet)
        meshData.invalidateSurfaceSet(i);
    updateSurfaceData();
    if (meshDataServer != null)
      meshDataServer.fillMeshData(meshData, MeshData.MODE_PUT_SETS, null);
  }

  void excludeMaximumSet() {
    if (meshDataServer != null)
      meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES, null);
    meshData.getSurfaceSet();
    BS bs;
    for (int i = meshData.nSets; --i >= 0;)
      if ((bs = meshData.surfaceSet[i]) != null 
          && bs.cardinality() > params.maxSet)
        meshData.invalidateSurfaceSet(i);
    updateSurfaceData();
    if (meshDataServer != null)
      meshDataServer.fillMeshData(meshData, MeshData.MODE_PUT_SETS, null);
  }
  
  public void slabIsosurface(Lst<Object[]> slabInfo) {
    if (meshDataServer != null)
      meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES, null);
    meshData.slabPolygonsList(slabInfo, true);
    if (meshDataServer != null)
      meshDataServer.fillMeshData(meshData, MeshData.MODE_PUT_VERTICES, null);
  }
 
  protected void setVertexAnisotropy(T3d pt) {
    pt.x *= anisotropy[0];
    pt.y *= anisotropy[1];
    pt.z *= anisotropy[2];
    pt.add(center);
  }

  protected void setVectorAnisotropy(T3d v) {
    haveSetAnisotropy = true;
    v.x *= anisotropy[0];
    v.y *= anisotropy[1];
    v.z *= anisotropy[2];
  }

  private boolean haveSetAnisotropy;
  
  protected void setVolumetricAnisotropy() {
    if (haveSetAnisotropy)
      return;
    setVolumetricOriginAnisotropy();
    setVectorAnisotropy(volumetricVectors[0]);
    setVectorAnisotropy(volumetricVectors[1]);
    setVectorAnisotropy(volumetricVectors[2]);
    
  }
  
  protected void setVolumetricOriginAnisotropy() {
    volumetricOrigin.setT(center);
  }

  private void setBBoxAll() {
    if (meshDataServer != null)
      meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES, null);
    xyzMin = new P3d();
    xyzMax = new P3d();
    meshData.setBox(xyzMin, xyzMax);
  }

  protected void setBBox(T3d pt, double margin) {
    if (xyzMin == null) {
      xyzMin = P3d.new3(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
      xyzMax = P3d.new3(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);
    }
    BoxInfo.addPoint(pt, xyzMin, xyzMax, margin);
  }

  /**
   * 
   * @param pt
   * @param getSource TODO
   * @return   value
   */
  public double getValueAtPoint(T3d pt, boolean getSource) {
    // only for readers that can support it (IsoShapeReader, AtomPropertyMapper)
    return 0;
  }

  void initializeMapping() {
    // initiate any iterators
  }

  protected void finalizeMapping() {
    // release any iterators
  }

  public int getSurfaceAtomIndex() {
    // atomPropertyMapper
    return -1;
  }

}

