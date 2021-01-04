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
import javajs.util.CU;
import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.T3;

import java.util.Hashtable;



import javajs.util.BS;
import org.jmol.jvxl.data.JvxlCoder;
import org.jmol.jvxl.data.JvxlData;
import org.jmol.jvxl.data.MeshData;
import org.jmol.shapesurface.IsosurfaceMesh;
import org.jmol.util.C;
import org.jmol.util.ColorEncoder;
import org.jmol.util.Escape;
import org.jmol.util.Logger;

import javajs.util.P3;
import javajs.util.P4;
import javajs.util.V3;

public class JvxlXmlReader extends VolumeFileReader {

  protected String JVXL_VERSION = "2.3";
  // 2.3 adds encoding "none"
  // 2.2 adds full support for return of rendering information, 
  //     retrieval of triangle edge data when edges have been modified by slabbing.
  
  protected int surfaceDataCount;
  protected int edgeDataCount;
  protected int colorDataCount;
  private int excludedTriangleCount;
  private int excludedVertexCount;
  private int invalidatedVertexCount;
  protected boolean haveContourData;

  private XmlReader xr;
  
  protected boolean isXmlFile= true;
  
  JvxlXmlReader(){}
  
  @Override
  void init2(SurfaceGenerator sg, BufferedReader br) {
    init2JXR(sg, br);
  }

  void init2JXR(SurfaceGenerator sg, BufferedReader br) {
    init2VFR(sg, br);
    jvxlData.wasJvxl = isJvxl = true;
    isXLowToHigh = canDownsample = false;
    xr = new XmlReader(br);
  }

  protected boolean thisInside;

  /////////////reading the format///////////

  @Override
  protected boolean readVolumeData(boolean isMapData) {
    if (!readVolumeDataVFR(isMapData))
      return false;
    strFractionTemp = jvxlEdgeDataRead;
    fractionPtr = 0;
    return true;
  }

  @Override
  protected boolean gotoAndReadVoxelData(boolean isMapData) {
    initializeVolumetricData();
    if (nPointsX < 0 || nPointsY < 0 || nPointsZ < 0)
      return true;
    try {
      gotoData(params.fileIndex - 1, nPointsX * nPointsY * nPointsZ);
      if (vertexDataOnly)
        return true;
      volumeData.setMappingPlane(params.thePlane);
      readSurfaceData(isMapData);
      volumeData.setMappingPlane(null);

      if (edgeDataCount > 0)
        jvxlEdgeDataRead = jvxlReadFractionData("edge", edgeDataCount);
      params.bsExcluded = jvxlData.jvxlExcluded = new BS[4];
      hasColorData = (colorDataCount > 0); // for nonXML version of JVXL
      if (hasColorData)
        jvxlColorDataRead = jvxlReadFractionData("color", colorDataCount);
      if (excludedVertexCount > 0) {
        jvxlData.jvxlExcluded[0] = JvxlCoder.jvxlDecodeBitSet(xr.getXmlData(
            "jvxlExcludedVertexData", null, false, false));
        if (xr.isNext("jvxlExcludedPlaneData"))
          jvxlData.jvxlExcluded[2] = JvxlCoder.jvxlDecodeBitSet(xr.getXmlData(
              "jvxlExcludedPlaneData", null, false, false));
      }
      if (excludedTriangleCount > 0)
        jvxlData.jvxlExcluded[3] = JvxlCoder.jvxlDecodeBitSet(xr.getXmlData(
            "jvxlExcludedTriangleData", null, false, false));
      if (invalidatedVertexCount > 0)
        jvxlData.jvxlExcluded[1] = JvxlCoder.jvxlDecodeBitSet(xr.getXmlData(
            "jvxlInvalidatedVertexData", null, false, false));
      if (haveContourData)
        jvxlDecodeContourData(jvxlData, xr.getXmlData("jvxlContourData", null,
            false, false));
      if (jvxlDataIsColorMapped && jvxlData.nVertexColors > 0) {
        jvxlData.vertexColorMap = new Hashtable<String, BS>();
        String vdata = xr.getXmlData("jvxlVertexColorData", null, true, false);
        String baseColor = XmlReader.getXmlAttrib(vdata, "baseColor");
        jvxlData.baseColor = (baseColor.length() > 0 ? baseColor : null);
        for (int i = 0; i < jvxlData.nVertexColors; i++) {
          String s = xr.getXmlData("jvxlColorMap", vdata, true, false);
          String color = XmlReader.getXmlAttrib(s, "color");
          BS bs = JvxlCoder.jvxlDecodeBitSet(xr.getXmlData("jvxlColorMap",
              s, false, false));
          jvxlData.vertexColorMap.put(color, bs);
        }
      }

    } catch (Exception e) {
      Logger.error(e.toString());
      return false;
    }
    return true;
  }
  
  String tempDataXml; 
  
  @Override
  protected void readParameters() throws Exception {
    String s = xr.getXmlData("jvxlFileTitle", null, false, false);
    jvxlFileHeaderBuffer = SB.newS(s == null ? "" : s);
    xr.toTag("jvxlVolumeData");
    String data = tempDataXml = xr.getXmlData("jvxlVolumeData", null, true, false);
    volumetricOrigin.setT(xr.getXmlPoint(data, "origin"));
   isAngstroms = true;
   readVector(0);
   readVector(1);
   readVector(2);
   line = xr.toTag("jvxlSurfaceSet");
   nSurfaces = parseIntStr(XmlReader.getXmlAttrib(line, "count"));
   Logger.info("jvxl file surfaces: " + nSurfaces);
   Logger.info("using default edge fraction base and range");
   Logger.info("using default color fraction base and range");
   cJvxlEdgeNaN = (char) (edgeFractionBase + edgeFractionRange);
  }

  protected void readVector(int voxelVectorIndex) throws Exception {
    String data = xr.getXmlData("jvxlVolumeVector", tempDataXml, true, true);
    tempDataXml = tempDataXml.substring(tempDataXml.indexOf(data) + data.length());
    int n = parseIntStr(XmlReader.getXmlAttrib(data, "count"));
    if (n == Integer.MIN_VALUE)
      vertexDataOnly = true;
    voxelCounts[voxelVectorIndex] = (n < 0 ? 0 : n);
    volumetricVectors[voxelVectorIndex].setT(xr.getXmlPoint(data, "vector"));
    if (isAnisotropic)
      setVectorAnisotropy(volumetricVectors[voxelVectorIndex]);
  }

  @Override
  protected void gotoData(int n, int nPoints) throws Exception {
    if (n > 0)
      Logger.info("skipping " + n + " data sets, " + nPoints + " points each");
    vertexDataOnly = jvxlData.vertexDataOnly = (nPoints == 0);
    for (int i = 0; i < n; i++) {
      jvxlSkipData(nPoints, true);
    }
    xr.toTag("jvxlSurface");
    jvxlReadSurfaceInfo();
  }

  protected void jvxlSkipData(@SuppressWarnings("unused") int nPoints,
                              @SuppressWarnings("unused") boolean doSkipColorData)
      throws Exception {
    rd();
    xr.skipTag("jvxlSurface");
  }

  protected void jvxlReadSurfaceInfo() throws Exception {
    String s;
    String data = xr.getXmlData("jvxlSurfaceInfo", null, true, true);
    isXLowToHigh = XmlReader.getXmlAttrib(data, "isXLowToHigh").equals("true");
    jvxlCutoff = parseFloatStr(XmlReader.getXmlAttrib(data, "cutoff"));
    if (!Float.isNaN(jvxlCutoff))
      Logger.info("JVXL read: cutoff " + jvxlCutoff);
    int nContourData = parseIntStr(XmlReader.getXmlAttrib(data, "nContourData"));
    haveContourData = (nContourData > 0);
    params.isContoured = jvxlData.isModelConnected = XmlReader.getXmlAttrib(data, "contoured").equals("true");
    params.isModelConnected = XmlReader.getXmlAttrib(data, "isModelConnected").equals("true");
    if (params.isContoured) {
      int nContoursRead = parseIntStr(XmlReader.getXmlAttrib(data, "nContours"));
      if (nContoursRead <= 0) {
        nContoursRead = 0;
      } else {
        if (params.thisContour < 0)
          params.thisContour = parseIntStr(XmlReader.getXmlAttrib(data, "thisContour"));
        s = XmlReader.getXmlAttrib(data, "contourValues");
        if (s.length() > 0) {
          s = s.replace('[',' ').replace(']',' ');
          jvxlData.contourValues = params.contoursDiscrete = parseFloatArrayStr(s);
          Logger.info("JVXL read: contourValues " + Escape.eAF(jvxlData.contourValues));            
        }
        s = XmlReader.getXmlAttrib(data, "contourColors");
        if (s.length() > 0) {
          jvxlData.contourColixes = params.contourColixes = C.getColixArray(s);
          jvxlData.contourColors = C.getHexCodes(jvxlData.contourColixes);
          Logger.info("JVXL read: contourColixes " +
              C.getHexCodes(jvxlData.contourColixes));        }
        params.contourFromZero = XmlReader.getXmlAttrib(data, "contourFromZero").equals("true");
      }
      params.nContours = (haveContourData ? nContourData : nContoursRead);
      //TODO ? params.contourFromZero = false; // MEP data to complete the plane
    }
    
    jvxlData.nVertexColors = parseIntStr(XmlReader.getXmlAttrib(data, "nVertexColors"));
    params.isBicolorMap = XmlReader.getXmlAttrib(data, "bicolorMap").equals("true");
    if (params.isBicolorMap) {
      // TODO -- not quite right, because
      s = XmlReader.getXmlAttrib(data, "colorPositive");
      if (s.length() > 0 && params.colorRgb == Integer.MIN_VALUE 
          && params.colorPos == Parameters.defaultColorPositive)
        params.colorPos = CU.getArgbFromString(s);
      s = XmlReader.getXmlAttrib(data, "colorNegative");
      if (s.length() > 0 && params.colorRgb == Integer.MIN_VALUE
          && params.colorNeg == Parameters.defaultColorNegative)
        params.colorNeg = CU.getArgbFromString(s);
    }
    if (params.isBicolorMap || params.colorBySign)
      jvxlCutoff = 0;
    jvxlDataIsColorMapped = 
      ((params.colorRgb == Integer.MIN_VALUE || params.colorRgb == Integer.MAX_VALUE)
    && (params.isBicolorMap || XmlReader.getXmlAttrib(data, "colorMapped").equals("true")));
    //isJvxlPrecisionColor is for information only -- will be superceded by encoding attribute of jvxlColorData
    jvxlData.isJvxlPrecisionColor = XmlReader.getXmlAttrib(data, "precisionColor").equals("true");
    jvxlData.jvxlDataIsColorDensity = params.colorDensity = (params.colorRgb == Integer.MIN_VALUE && XmlReader.getXmlAttrib(data, "colorDensity").equals("true"));
    if (jvxlData.jvxlDataIsColorDensity && Float.isNaN(params.pointSize)) {
      s = XmlReader.getXmlAttrib(data, "pointSize");
      if (s.length() > 0)
        jvxlData.pointSize = params.pointSize = parseFloatStr(s);
    }
    s = XmlReader.getXmlAttrib(data, "allowVolumeRender");
      jvxlData.allowVolumeRender = params.allowVolumeRender = (s.length() == 0 || s.equalsIgnoreCase("true"));
    s = XmlReader.getXmlAttrib(data, "plane");
    if (s.indexOf("{") >= 0) {
      params.thePlane = null;
      params.mapLattice = null;
      try {
        params.thePlane = (P4) Escape.uP(s);
        s = XmlReader.getXmlAttrib(data, "maplattice");
        Logger.info("JVXL read: plane " + params.thePlane);
        if (s.indexOf("{") >= 0) {
          params.mapLattice = (P3) Escape.uP(s);
          Logger.info("JVXL read: mapLattice " + params.mapLattice);
        }
        if (params.scale3d == 0)
          params.scale3d = parseFloatStr(XmlReader.getXmlAttrib(data, "scale3d"));
        if (Float.isNaN(params.scale3d))
          params.scale3d = 0;
      } catch (Exception e) {
        if (params.thePlane == null) {
          Logger
              .error("JVXL Error reading plane definition -- setting to 0 0 1 0  (z=0)");
          params.thePlane = P4.new4(0, 0, 1, 0);
        } else {
          Logger
          .error("JVXL Error reading mapLattice definition -- ignored");
        }
      }
      surfaceDataCount = 0;
      edgeDataCount = 0;
    } else {
      params.thePlane = null;
      surfaceDataCount = parseIntStr(XmlReader.getXmlAttrib(data, "nSurfaceInts"));
      edgeDataCount = parseIntStr(XmlReader.getXmlAttrib(data, "nBytesUncompressedEdgeData"));
      s = XmlReader.getXmlAttrib(data, "fixedLattice");
      if (s.indexOf("{") >= 0)
        jvxlData.fixedLattice = (P3) Escape.uP(s);
        
    }
    excludedVertexCount = parseIntStr(XmlReader.getXmlAttrib(data, "nExcludedVertexes"));
    excludedTriangleCount = parseIntStr(XmlReader.getXmlAttrib(data, "nExcludedTriangles"));
    invalidatedVertexCount = parseIntStr(XmlReader.getXmlAttrib(data, "nInvalidatedVertexes"));
    s = XmlReader.getXmlAttrib(data, "slabInfo");
    if (s.length() > 0)
      jvxlData.slabInfo = s;
    colorDataCount = Math.max(0, parseIntStr(XmlReader.getXmlAttrib(data, "nBytesUncompressedColorData")));
    jvxlDataIs2dContour = (params.thePlane != null && jvxlDataIsColorMapped);

    // new Jmol 12.1.50
    jvxlData.color = XmlReader.getXmlAttrib(data, "color");
    if (jvxlData.color.length() == 0 || jvxlData.color.indexOf("null") >= 0)
      jvxlData.color = "orange";
    jvxlData.translucency = parseFloatStr(XmlReader.getXmlAttrib(data, "translucency"));
    if (Float.isNaN(jvxlData.translucency))
      jvxlData.translucency = 0;
    s = XmlReader.getXmlAttrib(data, "meshColor");
    if (s.length() > 0)
      jvxlData.meshColor = s;
    s = XmlReader.getXmlAttrib(data, "rendering");
    if (s.length() > 0)
      jvxlData.rendering = s;
    jvxlData.colorScheme = XmlReader.getXmlAttrib(data, "colorScheme");
    if (jvxlData.colorScheme.length() == 0)
      jvxlData.colorScheme = (jvxlDataIsColorMapped ? "roygb": null); // allow for legacy default
    if (jvxlData.thisSet == null) {
      int n = parseIntStr(XmlReader.getXmlAttrib(data, "set"));
      if (n > 0) {
        jvxlData.thisSet = new BS();
        jvxlData.thisSet.set(n - 1);
      }
      String a = XmlReader.getXmlAttrib(data,  "subset");
      if (a != null && a.length() > 2) {
        String[] sets = a.replace('[', ' ').replace(']', ' ').trim().split(" ");
        if (sets.length > 0) {
          jvxlData.thisSet = new BS();
          for (int i = sets.length; --i >= 0;) {
            jvxlData.thisSet.set(PT.parseInt(sets[i]) - 1);
          }

        }
      }
    }
    jvxlData.slabValue = parseIntStr(XmlReader.getXmlAttrib(data, "slabValue"));    
    jvxlData.isSlabbable = (XmlReader.getXmlAttrib(data, "slabbable").equalsIgnoreCase("true"));    
    jvxlData.diameter = parseIntStr(XmlReader.getXmlAttrib(data, "diameter"));
    if (jvxlData.diameter == Integer.MIN_VALUE)
      jvxlData.diameter = 0;
    
    if (jvxlDataIs2dContour)
      params.isContoured = true;
    
    if (params.colorBySign)
      params.isBicolorMap = true;
    boolean insideOut = XmlReader.getXmlAttrib(data, "insideOut").equals("true");
    float dataMin = Float.NaN;
    float dataMax = Float.NaN;
    float red = Float.NaN;
    float blue = Float.NaN;
    if (jvxlDataIsColorMapped) {
      dataMin = parseFloatStr(XmlReader.getXmlAttrib(data, "dataMinimum"));
      dataMax = parseFloatStr(XmlReader.getXmlAttrib(data, "dataMaximum"));
      red = parseFloatStr(XmlReader.getXmlAttrib(data, "valueMappedToRed"));
      blue = parseFloatStr(XmlReader.getXmlAttrib(data, "valueMappedToBlue"));
      if (Float.isNaN(dataMin)) {
        dataMin = red = -1f;
        dataMax = blue = 1f;
      }
    }
    jvxlSetColorRanges(dataMin, dataMax, red, blue, insideOut);
  }

  protected void jvxlSetColorRanges(float dataMin, float dataMax, float red,
                                    float blue, boolean insideOut) {
    if (jvxlDataIsColorMapped) {
      if (!Float.isNaN(dataMin) && !Float.isNaN(dataMax)) {
        if (dataMax == 0 && dataMin == 0) {
          //set standard -1/1; bit of a hack
          dataMin = -1;
          dataMax = 1;
        }
        params.mappedDataMin = dataMin;
        params.mappedDataMax = dataMax;
        Logger.info("JVXL read: data_min/max " + params.mappedDataMin + "/"
            + params.mappedDataMax);
      }
      if (!params.rangeDefined)
        if (!Float.isNaN(red) && !Float.isNaN(blue)) {
          if (red == 0 && blue == 0) {
            //set standard -1/1; bit of a hack
            red = -1;
            blue = 1;
          }
          params.valueMappedToRed = Math.min(red, blue);
          params.valueMappedToBlue = Math.max(red, blue);
          params.isColorReversed = (red > blue);
          params.rangeDefined = true;
        } else {
          params.valueMappedToRed = 0f;
          params.valueMappedToBlue = 1f;
          params.rangeDefined = true;
        }
      Logger.info("JVXL read: color red/blue: " + params.valueMappedToRed + "/"
          + params.valueMappedToBlue);
    }
    jvxlData.valueMappedToRed = params.valueMappedToRed;
    jvxlData.valueMappedToBlue = params.valueMappedToBlue;
    jvxlData.mappedDataMin = params.mappedDataMin;
    jvxlData.mappedDataMax = params.mappedDataMax;
    jvxlData.isColorReversed = params.isColorReversed;
    if (params.insideOut)
      insideOut = !insideOut;
    params.insideOut = jvxlData.insideOut = insideOut;
  }

  @Override
  protected void readSurfaceData(boolean isMapDataIgnored) throws Exception {
    thisInside = !params.isContoured;
    if (readSurfaceDataXML())
      return;
    tempDataXml = xr.getXmlData("jvxlEdgeData", null, true, false);
    bsVoxelBitSet = JvxlCoder.jvxlDecodeBitSet(xr.getXmlData("jvxlEdgeData",
        tempDataXml, false, false));
    // if (thisInside)
    // bsVoxelBitSet = BitSetUtil.copyInvert(bsVoxelBitSet,
    // bsVoxelBitSet.size());
    readSurfaceDataJXR();
  }

  protected boolean readSurfaceDataXML() throws Exception {
    if (vertexDataOnly) {
      getEncodedVertexData();
      return true;
    } 
    if (params.thePlane != null) {
      volumeData.setDataDistanceToPlane(params.thePlane);
      setVolumeDataV(volumeData);
      params.cutoff = 0f;
      jvxlData.setSurfaceInfo(params.thePlane, params.mapLattice, 0, "");
      jvxlData.scale3d = params.scale3d;
      return true;
    }
    return false;
  }
  
  protected void readSurfaceDataJXR() throws Exception {
    readSurfaceDataVFR(false);
    volumeData.setMappingPlane(null);
  }

  /**
   * "edge" data includes two parts -- a compressed bitset indicating exactly which edges, 
   * in order or processing by Jmol, are crossed by the surface, and a set of fractions
   * indicating how far along that edge (good to 1 part in 8100) that surface crosses that edge.
   * We are just reading he fractions here.
   *  
   * "color" data comprises the corresponding sequence of data mapping values, again stored to
   * a precision of 1 part in 8100, relative to a range of values. 
   *  
   * @param type 
   * @param nPoints  
   * @return data
   */
  protected String jvxlReadFractionData(String type,
                                 int nPoints) {
    String str;
    try {
      if (type.equals("edge")) {
        str = JvxlCoder.jvxlDecompressString(XmlReader.getXmlAttrib(tempDataXml, "data"));
      } else {
        String data = xr.getXmlData("jvxlColorData", null, true, false);
        jvxlData.isJvxlPrecisionColor = getEncoding(data).endsWith("2");
        str = JvxlCoder.jvxlDecompressString(XmlReader.getXmlAttrib(data, "data"));
      }
    } catch (Exception e) {
      Logger.error("Error reading " + type + " data " + e);
      throw new NullPointerException();
    }
    return str;
  }
  
  protected BS bsVoxelBitSet;

  @Override
  protected BS getVoxelBitSet(int nPoints) throws Exception {
    if (bsVoxelBitSet != null)
      return bsVoxelBitSet;
    BS bs = new BS();
    int bsVoxelPtr = 0;
    if (surfaceDataCount <= 0)
      return bs; //unnecessary -- probably a plane or color density
    int nThisValue = 0;
    while (bsVoxelPtr < nPoints) {
      nThisValue = parseInt();
      if (nThisValue == Integer.MIN_VALUE) {
        rd();
        // note -- does not allow for empty lines;
        // must be a continuous block of numbers.
        if (line == null || (nThisValue = parseIntStr(line)) == Integer.MIN_VALUE) {
          if (!endOfData)
            Logger.error("end of file in JvxlReader?" + " line=" + line);
          endOfData = true;
          nThisValue = 10000;
          //throw new NullPointerException();
        }
      } 
      thisInside = !thisInside;
      ++jvxlNSurfaceInts;
      if (thisInside)
        bs.setBits(bsVoxelPtr, bsVoxelPtr + nThisValue);
      bsVoxelPtr += nThisValue;
    }
    return bs;
  }

  @Override
  protected float getSurfacePointAndFraction(float cutoff,
                                             boolean isCutoffAbsolute,
                                             float valueA, float valueB,
                                             T3 pointA,
                                             V3 edgeVector,
                                             int x, int y, int z, int vA, int vB, float[] fReturn, T3 ptReturn) {
    if (edgeDataCount <= 0)
      return getSPFv(cutoff, isCutoffAbsolute, valueA,
          valueB, pointA, edgeVector, x, y, z, vA, vB, fReturn, ptReturn);
    ptReturn.scaleAdd2(fReturn[0] = jvxlGetNextFraction(edgeFractionBase,
        edgeFractionRange, 0.5f), edgeVector, pointA);
    if (Float.isNaN(valueMin))
      setValueMinMax();      
    return (valueCount == 0 || includeValueNaN && Float.isNaN(fReturn[0]) 
        ? fReturn[0] : getNextValue());
  }

  private float getNextValue() {
    float fraction = Float.NaN;
    while (colorPtr < valueCount && Float.isNaN(fraction)) {
      if (jvxlData.isJvxlPrecisionColor) {
        // this COULD be an option for mapped surfaces; 
        // necessary for planes; used for vertex/triangle 2.0 style
        // precision is used for FULL-data range encoding, allowing full
        // treatment of JVXL files as though they were CUBE files.
        // the two parts of the "double-character-precision" value
        // are in separate lines, separated by n characters.
        fraction = JvxlCoder.jvxlFractionFromCharacter2(jvxlColorDataRead
            .charAt(colorPtr), jvxlColorDataRead.charAt((colorPtr++)
            + valueCount), colorFractionBase, colorFractionRange);
      } else {
        // my original encoding scheme
        // low precision only allows for mapping relative to the defined color range
        fraction = JvxlCoder.jvxlFractionFromCharacter(jvxlColorDataRead
            .charAt(colorPtr++), colorFractionBase, colorFractionRange, 0.5f);
      }
      break;
    }
    return valueMin + fraction * valueRange;
  }
  
  private void setValueMinMax() {
    valueCount = jvxlColorDataRead.length();
    if (jvxlData.isJvxlPrecisionColor)
      valueCount /= 2;
    includeValueNaN = (valueCount != jvxlEdgeDataRead.length());
    valueMin = (!jvxlData.isJvxlPrecisionColor ? params.valueMappedToRed
        : params.mappedDataMin == Float.MAX_VALUE ? defaultMappedDataMin
            : params.mappedDataMin);
    valueRange = (!jvxlData.isJvxlPrecisionColor ? params.valueMappedToBlue
        : params.mappedDataMin == Float.MAX_VALUE ? defaultMappedDataMax
            : params.mappedDataMax)
        - valueMin;
    haveReadColorData = true;
  }

  private boolean includeValueNaN = true;
  private int valueCount;
  private float valueMin = Float.NaN;
  private float valueRange = Float.NaN;
  private int fractionPtr;
  private int colorPtr;
  private String strFractionTemp = "";

  private float jvxlGetNextFraction(int base, int range, float fracOffset) {
    if (fractionPtr >= strFractionTemp.length()) {
      if (!endOfData)
        Logger.error("end of file reading compressed fraction data");
      endOfData = true;
      strFractionTemp = "" + (char) base;
      fractionPtr = 0;
    }
    return JvxlCoder.jvxlFractionFromCharacter(strFractionTemp.charAt(fractionPtr++),
        base, range, fracOffset);
  }

  boolean haveReadColorData;

  private String jvxlColorEncodingRead;
  
  @Override
  protected String readColorData() {
    if (!jvxlDataIsColorMapped)
      return "";
    // overloads SurfaceReader
    // standard jvxl file read for color 

    int vertexCount = jvxlData.vertexCount = meshData.vc;
    // the problem is that the new way to read data in Marching Cubes
    // is to ignore all points that are NaN. But then we also have to
    // remove those points from the color string. 

    short[] colixes = meshData.vcs;
    float[] vertexValues = meshData.vvs;
    /*
     * haveReadColorData?
     = (isJvxl ? jvxlColorDataRead : "");
    if (isJvxl && strValueTemp.length() == 0) {
      Logger
          .error("You cannot use JVXL data to map onto OTHER data, because it only contains the data for one surface. Use ISOSURFACE \"file.jvxl\" not ISOSURFACE .... MAP \"file.jvxl\".");
      return "";
    }
    */
    
    if ("none".equals(jvxlColorEncodingRead)) {
      jvxlData.vertexColors = new int[vertexCount];
      int[] nextc = new int[1];
      int n = PT.parseIntNext(jvxlColorDataRead, nextc);
      n = Math.min(n, vertexCount);
      String[] tokens = PT.getTokens(jvxlColorDataRead.substring(nextc[0]));
      boolean haveTranslucent = false;
      float trans = jvxlData.translucency;
      int lastColor = 0;
      for (int i = 0; i < n; i++)
        // colix will be one of 8 shades of translucent if A in ARGB is not FF.
        try{
          int c = getColor(tokens[i]);
          if (c == 0)
            c = lastColor;
          else
            lastColor = c;
          colixes[i] = C.getColixTranslucent(jvxlData.vertexColors[i] = c);
          if (C.isColixTranslucent(colixes[i]))
            haveTranslucent = true;
          else if (trans != 0)
            colixes[i] = C.getColixTranslucent3(colixes[i], true, trans);
        } catch (Exception e) {
          Logger.info("JvxlXmlReader: Cannot interpret color code: " + tokens[i]);
          // ignore this color if parsing error
        }
      if (haveTranslucent && trans == 0){
        // set to show in pass2
        jvxlData.translucency = 0.5f;
      }
      return "-";
    }    
    if (params.colorEncoder == null)
      params.colorEncoder = new ColorEncoder(null, null);
    params.colorEncoder.setColorScheme(null, false);
    params.colorEncoder.setRange(params.valueMappedToRed,
        params.valueMappedToBlue, params.isColorReversed);
    Logger.info("JVXL reading color data mapped min/max: "
        + params.mappedDataMin + "/" + params.mappedDataMax + " for "
        + vertexCount + " vertices." + " using encoding keys "
        + colorFractionBase + " " + colorFractionRange);
    Logger.info("mapping red-->blue for " + params.valueMappedToRed + " to "
        + params.valueMappedToBlue + " colorPrecision:"
        + jvxlData.isJvxlPrecisionColor);
    boolean getValues = (Float.isNaN(valueMin));
    if (getValues)
      setValueMinMax();
    float contourPlaneMinimumValue = Float.MAX_VALUE;
    float contourPlaneMaximumValue = -Float.MAX_VALUE;
    if (colixes == null || colixes.length < vertexCount)
      meshData.vcs = colixes = new short[vertexCount];
    //hasColorData = true;
    short colixNeg = 0, colixPos = 0;
    if (params.colorBySign) {
      colixPos = C.getColix(params.isColorReversed ? params.colorNeg
          : params.colorPos);
      colixNeg = C.getColix(params.isColorReversed ? params.colorPos
          : params.colorNeg);
    }
    int vertexIncrement = meshData.vertexIncrement;
    // here's the problem: we are assuming here that vertexCount == nPointsRead
    boolean needContourMinMax = (params.mappedDataMin == Float.MAX_VALUE);
    for (int i = 0; i < vertexCount; i += vertexIncrement) {
      float value;
      if (getValues)
        value = vertexValues[i] = getNextValue();
      else
        value = vertexValues[i];
      if (needContourMinMax) {
        if (value < contourPlaneMinimumValue)
          contourPlaneMinimumValue = value;
        if (value > contourPlaneMaximumValue)
          contourPlaneMaximumValue = value;
      }
    }
    if (needContourMinMax) {
      params.mappedDataMin = contourPlaneMinimumValue;
      params.mappedDataMax = contourPlaneMaximumValue;
    }
    if (jvxlData.colorScheme != null) {
      boolean setContourValue = (marchingSquares != null && params.isContoured);
      for (int i = 0; i < vertexCount; i += vertexIncrement) {
        float value = vertexValues[i];
        //note: these are just default colorings
        //orbital color had a bug through 11.2.6/11.3.6
        if (setContourValue) {
          marchingSquares.setContourData(i, value);
          continue;
        }
        short colix = (!params.colorBySign ? params.colorEncoder
            .getColorIndex(value) : (params.isColorReversed ? value > 0
            : value <= 0) ? colixNeg : colixPos);
        colixes[i] = C.getColixTranslucent3(colix, true,
            jvxlData.translucency);
      }
    }
    return jvxlColorDataRead + "\n";
  }

  private static int getColor(String c) {
    int n = 0;
    try {
      switch (c.charAt(0)) {
      case '[':
        n = CU.getArgbFromString(c);
        break;
      case '0': //0x
        n = PT.parseIntRadix(c.substring(2), 16);
        break;
      default:
        n = PT.parseIntRadix(c, 10);
      }
      //if (n < 0x1000000)
        //n |= 0xFF000000;
    } catch (Exception e) {
    }
    return n;
  }
  

  /**
   * retrieve Jvxl 2.0 format vertex/triangle/edge/color data found
   * within <jvxlSurfaceData> element 
   * 
   * @throws Exception
   */
  protected void getEncodedVertexData() throws Exception {
    // get vertices
    String sdata = xr.getXmlData("jvxlSurfaceData", null, true, false);
    jvxlDecodeVertexData(xr.getXmlData("jvxlVertexData", sdata, true, false), false);
    // get triangles
    String tData = xr.getXmlData("jvxlTriangleData", sdata, true, false);
    String edgeData = xr.getXmlData("jvxlTriangleEdgeData", sdata, true, false);
    // note: polygon color data is always between tags, not an attribute, and not compressed:
    String polygonColorData = xr.getXmlData("jvxlPolygonColorData", sdata, false, false);
    jvxlDecodeTriangleData(tData, edgeData, polygonColorData);
    // get vertex value data or vertex color data:
    String cData = xr.getXmlData("jvxlColorData", sdata, true, false);
    jvxlColorEncodingRead = getEncoding(cData);
    jvxlData.isJvxlPrecisionColor = jvxlColorEncodingRead.endsWith("2");
    cData = getData(cData, "jvxlColorData");
    jvxlColorDataRead = (jvxlColorEncodingRead.equals("none") ? cData : JvxlCoder.jvxlDecompressString(cData));
    jvxlDataIsColorMapped = ((params.colorRgb == Integer.MIN_VALUE || params.colorRgb == Integer.MAX_VALUE) && jvxlColorDataRead.length() > 0);
    // get contours
    if (haveContourData)
      jvxlDecodeContourData(jvxlData, xr.getXmlData("jvxlContourData", null, false, false));
  }

  private String getData(String sdata, String name) throws Exception {
    String data = XmlReader.getXmlAttrib(sdata, "data");
    if (data.length() == 0)
      data = xr.getXmlData(name, sdata, false, false);
    return data;
  }

  private static String getEncoding(String data) {
    // original JVXL does not include "encoding" 
    if (XmlReader.getXmlAttrib(data, "len").length() > 0)
      return "";
    String s = XmlReader.getXmlAttrib(data, "encoding");
    return (s.length() == 0 ? "none" : s);
  }

  /**
   * decode vertex data found within <jvxlVertexData> element as created by
   * jvxlEncodeVertexData (see above)
   * 
   * @param data
   *        tag and contents
   * @param asArray
   *        or just addVertexCopy
   * @return Point3f[] if desired
   * @throws Exception
   * 
   */
  public P3[] jvxlDecodeVertexData(String data, boolean asArray)
      throws Exception {
    int vertexCount = parseIntStr(XmlReader.getXmlAttrib(data, "count"));
    if (!asArray)
      Logger.info("Reading " + vertexCount + " vertices");
    int ptCount = vertexCount * 3;
    P3[] vertices = (asArray ? new P3[vertexCount] : null);
    float fraction;
    String vData = XmlReader.getXmlAttrib(data, "data");
    String encoding = getEncoding(data);
    if ("none".equals(encoding)) {
      if (vData.length() == 0)
        vData = xr.getXmlData("jvxlVertexData", data, false, false);
      float[] fdata = PT.parseFloatArray(vData);
      // first point is count -- ignored.
      if (fdata[0] != vertexCount * 3)
        Logger.info("JvxlXmlReader: vertexData count=" + ((int)fdata[0]) + "; expected " + (vertexCount * 3));
      for (int i = 0, pt = 1; i < vertexCount; i++) {
        P3 p = P3.new3(fdata[pt++], fdata[pt++], fdata[pt++]);
        if (asArray)
          vertices[i] = p;
        else
          addVertexCopy(p, 0, i, false);
      }
    } else {
      P3 min = xr.getXmlPoint(data, "min");
      P3 range = xr.getXmlPoint(data, "max");
      range.sub(min);
      int colorFractionBase = jvxlData.colorFractionBase;
      int colorFractionRange = jvxlData.colorFractionRange;
      String s = JvxlCoder.jvxlDecompressString(vData);
      if (s.length() == 0)
        s = xr.getXmlData("jvxlVertexData", data, false, false);
      for (int i = 0, pt = -1; i < vertexCount; i++) {
        P3 p = new P3();
        fraction = JvxlCoder.jvxlFractionFromCharacter2(s.charAt(++pt), s
            .charAt(pt + ptCount), colorFractionBase, colorFractionRange);
        p.x = min.x + fraction * range.x;
        fraction = JvxlCoder.jvxlFractionFromCharacter2(s.charAt(++pt), s
            .charAt(pt + ptCount), colorFractionBase, colorFractionRange);
        p.y = min.y + fraction * range.y;
        fraction = JvxlCoder.jvxlFractionFromCharacter2(s.charAt(++pt), s
            .charAt(pt + ptCount), colorFractionBase, colorFractionRange);
        p.z = min.z + fraction * range.z;
        if (asArray)
          vertices[i] = p;
        else
          addVertexCopy(p, 0, i, false);
      }
    }
    return vertices;
  }

  /**
   * decode triangle data found within <jvxlTriangleData> element as created
   * with jvxlEncodeTriangleData (see above)
   * 
   * @param tdata
   *        tag and contents
   * @param edgeData
   * @param colorData
   * @throws Exception
   */
  void jvxlDecodeTriangleData(String tdata, String edgeData, String colorData)
      throws Exception {
    int nTriangles = parseIntStr(XmlReader.getXmlAttrib(tdata, "count"));
    if (nTriangles < 0)
      return;
    int[] nextc = new int[1];
    int nColors = (colorData == null ? -1 : 1);
    int color = 0;
    Logger.info("Reading " + nTriangles + " triangles");
    String encoding = getEncoding(tdata);
    tdata = getData(tdata, "jvxlTriangleData");
    String edata = getData(edgeData, "jvxlTriangleEdgeData");
    int[] vertex = new int[3];
    int[] nextp = new int[1];
    int[] nexte = null;
    int edgeMask = 7;
    boolean haveEdgeInfo;
    boolean haveEncoding = !"none".equals(encoding);
    if (haveEncoding) {
      tdata = JvxlCoder.jvxlDecompressString(tdata);
      edata = JvxlCoder.jvxlDecompressString(edata).trim();
      haveEdgeInfo = (edata.length() == nTriangles);
    } else {
      int n = PT.parseIntNext(tdata, nextp);
      haveEdgeInfo = (edata.length() > 0);
      if (haveEdgeInfo) {
        nexte = new int[1];
        PT.parseIntNext(edata, nexte); // throw away count
      } else if (n > 0) {
        Logger.info("JvxlXmlReader: jvxlTriangleEdgeData count=" + n
            + "; expected " + nTriangles);
      }
    }
    for (int i = 0, v = 0, p = 0, pt = -1; i < nTriangles;) {
      if (haveEncoding) {
        char ch = tdata.charAt(++pt);
        int diff;
        switch (ch) {
        case '!':
          diff = 0;
          break;
        case '+':
        case '.':
        case ' ':
        case '\n':
        case '\r':
        case '\t':
        case ',':
          continue;
        case '-':
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
          nextp[0] = pt;
          diff = PT.parseIntNext(tdata, nextp);
          pt = nextp[0] - 1;
          break;
        default:
          diff = ch - 92; // '\' character
        }
        v += diff;
      } else {
        v = PT.parseIntNext(tdata, nextp) - 1;
      }
      vertex[p] = v;
      if (++p == 3) {
        p = 0;
        if (haveEdgeInfo) {
          edgeMask = (nexte == null ? edata.charAt(i) - '0' : javajs.util.PT
              .parseIntNext(edata, nexte));
          if (edgeMask < 0 || edgeMask > 7)
            edgeMask = 7;
        }
        if (--nColors == 0) {
          nColors = (PT.parseIntNext(colorData, nextc));
          int c = PT.parseIntNext(colorData, nextc);
          if (c == Integer.MIN_VALUE)
            nColors = 0;
          else
            color = c | 0xFF000000;
        }
        addTriangleCheck(vertex[0], vertex[1], vertex[2], edgeMask, color, false,
            color);
        i++;
      }
    }
  }

  protected void jvxlDecodeContourData(JvxlData jvxlData, String data)
      throws Exception {
    Lst<Lst<Object>> vs = new  Lst<Lst<Object>>();
    SB values = new SB();
    SB colors = new SB();
    int pt = -1;
    jvxlData.vContours = null;
    if (data == null)
      return;
    while ((pt = data.indexOf("<jvxlContour", pt + 1)) >= 0) {
      Lst<Object> v = new  Lst<Object>();
      String s = xr.getXmlData("jvxlContour", data.substring(pt), true, false);
      float value = parseFloatStr(XmlReader.getXmlAttrib(s, "value"));
      values.append(" ").appendF(value);
      int color = getColor(XmlReader.getXmlAttrib(s, "color"));
      short colix = C.getColix(color);
      colors.append(" ").append(Escape.escapeColor(color));
      String fData = JvxlCoder.jvxlDecompressString(XmlReader.getXmlAttrib(s,
          "data"));
      BS bs = JvxlCoder.jvxlDecodeBitSet(xr.getXmlData("jvxlContour", s,
          false, false));
      int n = bs.length();
      IsosurfaceMesh.setContourVector(v, n, bs, value, colix, color,
          SB.newS(fData));
      vs.addLast(v);
    }
    int n = vs.size();
    if (n > 0) {
      jvxlData.vContours = AU.createArrayOfArrayList(n);
      // 3D contour values and colors
      jvxlData.contourColixes = params.contourColixes = new short[n];
      jvxlData.contourValues = params.contoursDiscrete = new float[n];
      for (int i = 0; i < n; i++) {
        jvxlData.vContours[i] = vs.get(i);
        jvxlData.contourValues[i] = ((Float) jvxlData.vContours[i].get(2))
            .floatValue();
        jvxlData.contourColixes[i] = ((short[]) jvxlData.vContours[i].get(3))[0];
      }
      jvxlData.contourColors = C.getHexCodes(jvxlData.contourColixes);
      Logger.info("JVXL read: " + n + " discrete contours");
      Logger.info("JVXL read: contour values: " + values);
      Logger.info("JVXL read: contour colors: " + colors);
    }
  }
  
  @Override
  protected void postProcessVertices() {
    BS bsInvalid = params.bsExcluded[1]; 
    if (bsInvalid != null) {
      if (meshDataServer != null)
        meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES, null);
      meshData.invalidateVertices(bsInvalid); 
      if (meshDataServer != null) {
        meshDataServer.fillMeshData(meshData, MeshData.MODE_PUT_VERTICES, null);
        meshData = new MeshData();
      }
      updateTriangles();
    }
  }

}
