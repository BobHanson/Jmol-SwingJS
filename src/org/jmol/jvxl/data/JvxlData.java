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
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */


/*
 
 * The JVXL file format
 * --------------------
 * 
 * see http://www.stolaf.edu/academics/chemapps/jmol/docs/misc/JVXL-format.pdf
 *
 * The JVXL (Jmol VoXeL) format is a file format specifically designed
 * to encode an isosurface or planar slice through a set of 3D scalar values
 * in lieu of a that set. A JVXL file can contain coordinates, and in fact
 * it must contain at least one coordinate, but additional coordinates are
 * optional. The file can contain any finite number of encoded surfaces. 
 * However, the compression of 300-500:1 is based on the reduction of the 
 * data to a SINGLE surface. 
 * 
 * 
 * The original Marching Cubes code was written by Miguel Howard in 2005.
 * The classes Parser, ArrayUtil, and TextFormat are condensed versions
 * of the classes found in org.jmol.util.
 * 
 * All code relating to JVXL format is copyrighted 2006-2009 and invented by 
 * Robert M. Hanson, 
 * Professor of Chemistry, 
 * St. Olaf College, 
 * 1520 St. Olaf Ave.
 * Northfield, MN. 55057.
 * 
 * Implementations of the JVXL format should reference 
 * "Robert M. Hanson, St. Olaf College" and the opensource Jmol project.
 * 
 * 
 * implementing marching squares; see 
 * http://www.secam.ex.ac.uk/teaching/ug/studyres/COM3404/COM3404-2006-Lecture15.pdf
 * 
 */

package org.jmol.jvxl.data;




import java.util.Map;


import javajs.util.BS;

import javajs.util.Lst;
import javajs.util.SB;
import javajs.util.P3;
import javajs.util.P4;


/*
 * the JvxlData class holds parameters and data
 * that needs to be passed among IsosurfaceMesh, 
 * marching cubes/squares, JvxlCoder, and JvxlReader. 
 * 
 */
public class JvxlData {
  public JvxlData() {    
  }
 
  public String msg = "";
  public boolean wasJvxl;
  public boolean wasCubic;
  
  public String jvxlFileTitle;
  public String jvxlFileMessage;
  public String jvxlSurfaceData;
  public String jvxlEdgeData;
  public String jvxlColorData;
  public String jvxlVolumeDataXml;
  public BS[] jvxlExcluded = new BS[4];
  
  public P4 jvxlPlane;

  public boolean isJvxlPrecisionColor;
  public boolean jvxlDataIsColorMapped;
  public boolean jvxlDataIs2dContour;
  public boolean jvxlDataIsColorDensity;
  public boolean isColorReversed;
  public int thisSet = Integer.MIN_VALUE;
  
  public int edgeFractionBase = JvxlCoder.defaultEdgeFractionBase;
  public int edgeFractionRange = JvxlCoder.defaultEdgeFractionRange;
  public int colorFractionBase = JvxlCoder.defaultColorFractionBase;
  public int colorFractionRange = JvxlCoder.defaultColorFractionRange;

  public boolean isValid = true; // set false if calculation gave no surface
  public boolean insideOut;
  public boolean isXLowToHigh;
  public boolean isContoured;
  public boolean isBicolorMap;
  public boolean isTruncated;
  public boolean isCutoffAbsolute;
  public boolean isModelConnected;
  public boolean vertexDataOnly;
  public float mappedDataMin;
  public float mappedDataMax;
  public float valueMappedToRed;
  public float valueMappedToBlue;
  public float cutoff;
  public float pointsPerAngstrom; 
  public int nPointsX, nPointsY, nPointsZ;
  public long nBytes;
  public int nContours;
  public int nEdges;
  public int nSurfaceInts;
  public int vertexCount;

  // contour data is here instead of in MeshData because
  // sometimes it comes from the file or marching squares
  // directly. 
  
  public Lst<Object>[] vContours;
  public short[] contourColixes;
  public String contourColors;
  public float[] contourValues;
  public float[] contourValuesUsed;
  public int thisContour = -1;
  public float scale3d;

  public short minColorIndex = -1;
  public short maxColorIndex = 0;

  public String[] title;
  public String version;
  public P3[] boundingBox;
  public int excludedTriangleCount;
  public int excludedVertexCount;
  public boolean colorDensity;
  public float pointSize;
  public String moleculeXml;
  public float dataMin, dataMax;
  public int saveVertexCount;
  
  // added Jmol 12.1.50
  public Map<String, BS> vertexColorMap; // from color isosurface {atom subset} red 
  public int nVertexColors;
  public int[] vertexColors;
  public String color;
  public String meshColor;
  public float translucency;
  public String colorScheme;
  public String rendering;
  public int slabValue = Integer.MIN_VALUE;
  public boolean isSlabbable;
  public int diameter;
  public String slabInfo;
  public boolean allowVolumeRender;
  public float voxelVolume;
  public P3 mapLattice;
  public P3 fixedLattice;
  public String baseColor;
  public float integration = Float.NaN;

  public void clear() {
    allowVolumeRender = true;
    jvxlSurfaceData = "";
    jvxlEdgeData = "";
    jvxlColorData = "";
    jvxlVolumeDataXml = "";
    color = null;
    colorScheme = null;
    colorDensity = false;
    pointSize = Float.NaN;
    contourValues = null;
    contourValuesUsed = null;
    contourColixes = null;
    contourColors = null;
    integration = Float.NaN;
    isSlabbable = false;
    isValid = true;
    mapLattice = null;
    meshColor = null;
    msg = "";
    nPointsX = 0;
    nVertexColors = 0;
    fixedLattice = null;
    slabInfo = null;
    slabValue = Integer.MIN_VALUE;
    thisSet = Integer.MIN_VALUE;
    rendering = null;
    thisContour = -1;
    translucency = 0;
    vContours = null;
    vertexColorMap = null;
    vertexColors = null;
    voxelVolume = 0;
  }

  public void setSurfaceInfo(P4 thePlane, P3 mapLattice, int nSurfaceInts, String surfaceData) {
    jvxlSurfaceData = surfaceData;
    if (jvxlSurfaceData.indexOf("--") == 0)
      jvxlSurfaceData = jvxlSurfaceData.substring(2);
    jvxlPlane = thePlane;
    this.mapLattice = mapLattice;
    this.nSurfaceInts = nSurfaceInts;
  }

  public void setSurfaceInfoFromBitSet(BS bs, P4 thePlane) {
    setSurfaceInfoFromBitSetPts(bs, thePlane, null);
  }
  public void setSurfaceInfoFromBitSetPts(BS bs, P4 thePlane, P3 mapLattice) {
    SB sb = new SB();
    int nSurfaceInts = (thePlane != null ? 0 : JvxlCoder.jvxlEncodeBitSetBuffer(bs,
        nPointsX * nPointsY * nPointsZ, sb));
    setSurfaceInfo(thePlane, mapLattice, nSurfaceInts, sb.toString());
  }
    
  public void jvxlUpdateInfo(String[] title, long nBytes) {
    this.title = title;
    this.nBytes = nBytes;
  }

  public static String updateSurfaceData(String edgeData, float[] vertexValues,
                                         int vertexCount, int vertexIncrement,
                                         char isNaN) {
    if (edgeData.length() == 0)
      return "";
    char[] chars = edgeData.toCharArray();
    for (int i = 0, ipt = 0; i < vertexCount; i += vertexIncrement, ipt++)
      if (Float.isNaN(vertexValues[i]))
        chars[ipt] = isNaN;
    return String.copyValueOf(chars);
  }

  
}

