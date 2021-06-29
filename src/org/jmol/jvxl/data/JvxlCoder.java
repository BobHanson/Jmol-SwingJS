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
package org.jmol.jvxl.data;



import java.util.Map;

import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.T3;
import javajs.util.XmlUtil;

import javajs.util.BS;

import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Escape;
import org.jmol.util.Logger;


public class JvxlCoder {

  //TODO -- need to escapeXml for text data
  
  final public static String JVXL_VERSION1 = "2.0";
  final public static String JVXL_VERSION_XML = "2.3";
  
  
  // 1.4 adds -nContours to indicate contourFromZero for MEP data mapped onto planes
  // 2.0 adds vertex/triangle compression when no grid is present 
  // Jmol 11.7.25 -- recoded so that we do not create voxelData[nx][ny][nz] and instead
  //                 simply create a BitSet of length nx * ny * nz. This saves memory hugely.
  // 2.1 adds JvxlXmlReader
  // 2.2 adds color density Jmol 12.0.15/12.1.13
  // 2.3 adds discrete colors for vertex-only data (encoding="none")
  
  /**
   * 
   * @param volumeData
   * @param jvxlData
   * @param title
   * @return XML string
   */
  public static String jvxlGetFile(VolumeData volumeData, JvxlData jvxlData,
                                   String[] title) {
    // for the simple writer
    int[] counts = volumeData.getVoxelCounts();
    jvxlData.nPointsX = counts[0];
    jvxlData.nPointsY = counts[1];
    jvxlData.nPointsZ = counts[2];
    jvxlData.jvxlVolumeDataXml = volumeData.setVolumetricXml();
    return jvxlGetFile(jvxlData, null, title, null, true, 1, null, null);
  }

  /**
   * 
   * @param jvxlData
   * @param meshData
   * @param title
   * @param msg
   * @param includeHeader
   * @param nSurfaces
   * @param state
   * @param comment
   * @return JVXL file XML 
   */
  public static String jvxlGetFile(JvxlData jvxlData, MeshData meshData,
                                   String[] title, String msg,
                                   boolean includeHeader, int nSurfaces,
                                   String state, String comment) {
    
    SB data = new SB();
    if ("TRAILERONLY".equals(msg)) {
      XmlUtil.closeTag(data, "jvxlSurfaceSet");
      XmlUtil.closeTag(data, "jvxl");
      return data.toString();
    }
    
    boolean vertexDataOnly = (meshData != null);
    boolean isHeaderOnly = ("HEADERONLY".equals(msg));
    if (includeHeader) {
      XmlUtil.openDocument(data);
      XmlUtil.openTagAttr(data, "jvxl", new String[] {
          "version", JVXL_VERSION_XML,
          "jmolVersion", jvxlData.version,
          "xmlns", "http://jmol.org/jvxl_schema",
          "xmlns:cml", "http://www.xml-cml.org/schema" });
      XmlUtil.appendCdata(data, "jvxlFileTitle", null, jvxlData.jvxlFileTitle == null ? "\n" : "\n" + jvxlData.jvxlFileTitle);
      if (jvxlData.moleculeXml != null)
        data.append(jvxlData.moleculeXml);
      String volumeDataXml = (vertexDataOnly ? null : jvxlData.jvxlVolumeDataXml);
      if (volumeDataXml == null)
        volumeDataXml = (new VolumeData()).setVolumetricXml();
      data.append(volumeDataXml);
      XmlUtil.openTagAttr(data,"jvxlSurfaceSet", 
          new String[] { "count", "" + (nSurfaces > 0 ? nSurfaces : 1) });
      if (isHeaderOnly)
        return data.toString();
    }
    SB sb;
    String type = (vertexDataOnly ? "pmesh"
        : jvxlData.jvxlPlane == null ? "isosurface" : "plane");
    // TODO: contours mentioned here? when discrete?
    if (jvxlData.jvxlColorData != null && jvxlData.jvxlColorData.length() > 0)
      type = "mapped " + type;
    XmlUtil.openTagAttr(data, "jvxlSurface", new String[] { "type", type });
    data.append(jvxlGetInfoData(jvxlData, vertexDataOnly));
    jvxlAppendCommandState(data, comment, state);
    if (title != null || msg != null && msg.length() > 0) {
      sb = new SB();
      if (msg != null && msg.length() > 0)
        sb.append(msg).append("\n");
      if (title != null)
        for (int i = 0; i < title.length; i++)
          sb.append(title[i]).appendC('\n');
      XmlUtil.appendCdata(data, "jvxlSurfaceTitle", null, sb.toString());
    }
    sb = new SB();
    
    XmlUtil.openTagAttr(sb, "jvxlSurfaceData", (vertexDataOnly || jvxlData.jvxlPlane == null ? null :
      jvxlData.mapLattice == null ? new String[] { "plane", Escape.eP4(jvxlData.jvxlPlane) }
      :  new String[] { "plane", Escape.eP4(jvxlData.jvxlPlane),  "maplattice", Escape.eP(jvxlData.mapLattice)  }));
    if (vertexDataOnly) {
      appendXmlVertexOnlyData(sb, jvxlData, meshData, true);
    } else if (jvxlData.jvxlPlane == null) {
      if (jvxlData.jvxlEdgeData == null)
        return "";
      appendXmlEdgeData(sb, jvxlData);
      appendXmlColorData(sb, jvxlData.jvxlColorData,
          true, jvxlData.isJvxlPrecisionColor, jvxlData.valueMappedToRed,
          jvxlData.valueMappedToBlue);
    } else {
      appendXmlColorData(sb, jvxlData.jvxlColorData,
          true, jvxlData.isJvxlPrecisionColor, jvxlData.valueMappedToRed,
          jvxlData.valueMappedToBlue);
    }
    appendEncodedBitSetTag(sb, "jvxlInvalidatedVertexData", jvxlData.jvxlExcluded[1], -1, null);
    if (jvxlData.excludedVertexCount > 0) {
      appendEncodedBitSetTag(sb, "jvxlExcludedVertexData", jvxlData.jvxlExcluded[0], jvxlData.excludedVertexCount, null);
      appendEncodedBitSetTag(sb, "jvxlExcludedPlaneData", jvxlData.jvxlExcluded[2], -1, null);
    }
    appendEncodedBitSetTag(sb, "jvxlExcludedTriangleData", jvxlData.jvxlExcluded[3], jvxlData.excludedTriangleCount, null);
    XmlUtil.closeTag(sb, "jvxlSurfaceData");
    int len = sb.length();
    data.appendSB(sb);
    if (jvxlData.vContours != null && jvxlData.vContours.length > 0) {
      jvxlEncodeContourData(jvxlData.vContours, data);
    }
    if (jvxlData.vertexColorMap != null) {
      if (jvxlData.baseColor == null)
        XmlUtil.openTag(data, "jvxlVertexColorData");
      else
        XmlUtil.openTagAttr(data, "jvxlVertexColorData", new String[] {"baseColor", jvxlData.baseColor});
      for (Map.Entry<String, BS> entry : jvxlData.vertexColorMap.entrySet())
        appendEncodedBitSetTag(data, "jvxlColorMap", entry.getValue(), -1, new Object[] { "color", entry.getKey() });
      jvxlData.vertexColorMap = null;
      XmlUtil.closeTag(data, "jvxlVertexColorData");
    }
    XmlUtil.closeTag(data, "jvxlSurface");
    if (includeHeader) {
      XmlUtil.closeTag(data, "jvxlSurfaceSet");
      XmlUtil.closeTag(data, "jvxl");
    }
    return jvxlSetCompressionRatio(data, jvxlData, len);
  }

  private static void appendEncodedBitSetTag(SB sb, String name, BS bs, int count, Object[] attribs) {
    if (count < 0)
      count = BSUtil.cardinalityOf(bs);
    if (count == 0)
      return;
    SB sb1 = new SB();
    sb1.append("\n ");
    jvxlEncodeBitSetBuffer(bs, -1, sb1);
    XmlUtil.appendTagObj(sb, name, new Object[] {
        attribs,
        "bsEncoding", "base90+35",
        "count", "" + count,
        "len", "" + bs.length() }, 
        jvxlCompressString(sb1.toString(), true));
  }

  private static String jvxlSetCompressionRatio(SB data,
                                                JvxlData jvxlData, int len) {
    String s = data.toString();
    int r = (int) (jvxlData.nBytes > 0 ? ((float) jvxlData.nBytes) / len
        : ((float) (jvxlData.nPointsX
          * jvxlData.nPointsY * jvxlData.nPointsZ * 13)) / len);
    return PT.rep(s, "\"not calculated\"", (r > 0 ? "\"" + r +":1\"": "\"?\""));
  }

  private static void appendXmlEdgeData(SB sb, JvxlData jvxlData) {
    XmlUtil.appendTagObj(sb, "jvxlEdgeData", new String[] {
        "count", "" + (jvxlData.jvxlEdgeData.length() - 1),
        "encoding", "base90f1",
        "bsEncoding", "base90+35c",
        "isXLowToHigh", "" + jvxlData.isXLowToHigh,
        "data", jvxlCompressString(jvxlData.jvxlEdgeData, true) }, "\n" 
        + jvxlCompressString(jvxlData.jvxlSurfaceData, true));
  }

  private static void jvxlAppendCommandState(SB data, String cmd,
                                             String state) {
    if (cmd != null)
      XmlUtil.appendCdata(data, "jvxlIsosurfaceCommand", null,
          "\n" + (cmd.indexOf("#") < 0 ? cmd : cmd.substring(0, cmd.indexOf("#"))) + "\n");
    if (state != null) {
      if (state.indexOf("** XML ** ") >=0) {
        state = PT.split(state, "** XML **")[1].trim(); 
        XmlUtil.appendTag(data, "jvxlIsosurfaceState",  "\n" + state + "\n");
      } else {
        XmlUtil.appendCdata(data, "jvxlIsosurfaceState", null, "\n" + state);
      }
    }
  }

  private static void appendXmlColorData(SB sb,  
                                         String data,
                                         boolean isEncoded,
                                         boolean isPrecisionColor,
                                         float value1,
                                         float value2) {
    int n;
    if (data == null || (n = data.length() - 1) < 0)
      return;
    if (isPrecisionColor)
      n /= 2;
    XmlUtil.appendTagObj(sb, "jvxlColorData", new String[] {
        "count", "" + n, 
        "encoding", (isEncoded ? "base90f" + (isPrecisionColor ? "2" : "1") : "none"),
        "min", "" + value1,
        "max", "" + value2,
        "data", jvxlCompressString(data, true) }, null);
  }

  
  public static String jvxlGetInfo(JvxlData jvxlData) {
    return jvxlGetInfoData(jvxlData, jvxlData.vertexDataOnly);
  }

  public static String jvxlGetInfoData(JvxlData jvxlData, boolean vertexDataOnly) {
    if (jvxlData.jvxlSurfaceData == null)
      return "";
    Lst<String[]> attribs = new  Lst<String[]>();
     
    int nSurfaceInts = jvxlData.nSurfaceInts;// jvxlData.jvxlSurfaceData.length();
    int bytesUncompressedEdgeData = (vertexDataOnly ? 0
        : jvxlData.jvxlEdgeData.length() - 1);
    int nColorData = (jvxlData.jvxlColorData == null ? -1 : (jvxlData.jvxlColorData.length() - 1));
    addAttrib(attribs, "\n  isModelConnected", "" + jvxlData.isModelConnected);
    if (!vertexDataOnly) {
      // informational only:
      addAttrib(attribs, "\n  cutoff", "" + jvxlData.cutoff);
      addAttrib(attribs, "\n  isCutoffAbsolute", "" + jvxlData.isCutoffAbsolute);
      addAttrib(attribs, "\n  pointsPerAngstrom", "" + jvxlData.pointsPerAngstrom);
      int n = jvxlData.jvxlSurfaceData.length() 
          + bytesUncompressedEdgeData + nColorData + 1;
      if (n > 0)
        addAttrib(attribs, "\n  nBytesData", "" + n);

      //TODO: these should only be for information purposes, but are not:
      addAttrib(attribs, "\n  isXLowToHigh", "" + jvxlData.isXLowToHigh);
      if (jvxlData.jvxlPlane == null) {
        addAttrib(attribs, "\n  nSurfaceInts", "" + nSurfaceInts);
        addAttrib(attribs, "\n  nBytesUncompressedEdgeData", "" + bytesUncompressedEdgeData);
      }
      if (nColorData > 0)
        addAttrib(attribs, "\n  nBytesUncompressedColorData", "" + nColorData); // TODO: later?
    }
    jvxlData.excludedVertexCount = BSUtil.cardinalityOf(jvxlData.jvxlExcluded[0]);
    jvxlData.excludedTriangleCount = BSUtil.cardinalityOf(jvxlData.jvxlExcluded[3]);
    if (jvxlData.excludedVertexCount > 0)
      addAttrib(attribs, "\n  nExcludedVertexes", "" + jvxlData.excludedVertexCount);
    if (jvxlData.excludedTriangleCount > 0)
      addAttrib(attribs, "\n  nExcludedTriangles", "" + jvxlData.excludedTriangleCount);
    int n = BSUtil.cardinalityOf(jvxlData.jvxlExcluded[1]);
    if (n > 0)
      addAttrib(attribs, "\n  nInvalidatedVertexes", "" + n);
    if (jvxlData.slabInfo != null)
      addAttrib(attribs, "\n  slabInfo", jvxlData.slabInfo);
    //next is for information only -- will be superceded by "encoding" attribute of jvxlColorData
    if (jvxlData.isJvxlPrecisionColor)
      addAttrib(attribs, "\n  precisionColor", "true");
    if (jvxlData.colorDensity)
      addAttrib(attribs, "\n  colorDensity", "true");
    if (!Float.isNaN(jvxlData.pointSize))
      addAttrib(attribs, "\n  pointSize", "" + jvxlData.pointSize);
    else if (jvxlData.diameter != 0)
      addAttrib(attribs, "\n  diameter", "" + jvxlData.diameter);
    if (!jvxlData.allowVolumeRender)
      addAttrib(attribs, "\n  allowVolumeRender", "false");
    if (jvxlData.jvxlPlane == null || vertexDataOnly) {
      if (jvxlData.fixedLattice != null && !vertexDataOnly)
        addAttrib(attribs, "\n  fixedLattice", "" + jvxlData.fixedLattice);
      if (jvxlData.isContoured) {
        addAttrib(attribs, "\n  contoured", "true"); 
        addAttrib(attribs, "\n  colorMapped", "true");
      } else if (jvxlData.isBicolorMap) {
        addAttrib(attribs, "\n  bicolorMap", "true");
        addAttrib(attribs, "\n  colorNegative", C.getHexCode(jvxlData.minColorIndex));
        addAttrib(attribs, "\n  colorPositive", C.getHexCode(jvxlData.maxColorIndex));
      } else if (nColorData > 0) {
        addAttrib(attribs, "\n  colorMapped", "true");
      }
      if (jvxlData.vContours != null && jvxlData.vContours.length > 0)
        addAttrib(attribs, "\n  nContourData", "" + jvxlData.vContours.length);
    } else {
      if (jvxlData.mapLattice != null)
        addAttrib(attribs, "\n  mapLattice", "" + jvxlData.mapLattice);
      if (jvxlData.scale3d != 0)
        addAttrib(attribs, "\n  scale3d", "" + jvxlData.scale3d);
      if (nColorData > 0)
        addAttrib(attribs, "\n  colorMapped", "true");
      addAttrib(attribs, "\n  plane", Escape.eP4(jvxlData.jvxlPlane));
    }
    if (jvxlData.color != null && jvxlData.color.indexOf("null") < 0)
      addAttrib(attribs, "\n  color", jvxlData.color);
    addAttrib(attribs, "\n  translucency", "" + jvxlData.translucency);
    if (jvxlData.meshColor != null)
      addAttrib(attribs, "\n  meshColor", jvxlData.meshColor);
    if (jvxlData.colorScheme != null)
      addAttrib(attribs, "\n  colorScheme", jvxlData.colorScheme);
    if (jvxlData.rendering != null)
      addAttrib(attribs, "\n  rendering", jvxlData.rendering);
    if (jvxlData.thisSet != null) {
      String s = subsetString(jvxlData.thisSet);
      if (s.startsWith("["))
        addAttrib(attribs, "\n  subset", s);
      else
        addAttrib(attribs, "\n  set", s);
    }
    if (jvxlData.slabValue != Integer.MIN_VALUE)
      addAttrib(attribs, "\n  slabValue", "" + jvxlData.slabValue);
    if (jvxlData.isSlabbable)
      addAttrib(attribs, "\n  slabbable", "true");
    if (jvxlData.nVertexColors > 0)
      addAttrib(attribs, "\n  nVertexColors", "" + jvxlData.nVertexColors);

    float min = (jvxlData.mappedDataMin == Float.MAX_VALUE ? 0f
        : jvxlData.mappedDataMin);
    float blue = (jvxlData.isColorReversed ? jvxlData.valueMappedToRed : jvxlData.valueMappedToBlue);
    float red = (jvxlData.isColorReversed ? jvxlData.valueMappedToBlue : jvxlData.valueMappedToRed);

    if (jvxlData.jvxlColorData != null && jvxlData.jvxlColorData.length() > 0 && !jvxlData.isBicolorMap) {
      addAttrib(attribs, "\n  dataMinimum", "" + min);
      addAttrib(attribs, "\n  dataMaximum", "" + jvxlData.mappedDataMax);
      addAttrib(attribs, "\n  valueMappedToRed", "" + red);
      addAttrib(attribs, "\n  valueMappedToBlue", "" + blue);
    }
    if (jvxlData.isContoured) {
      if (jvxlData.contourValues == null || jvxlData.contourColixes == null) {
        if (jvxlData.vContours == null)
          addAttrib(attribs, "\n  nContours", "" + Math.abs(jvxlData.nContours));
      } else {
        if (jvxlData.jvxlPlane != null)
          addAttrib(attribs, "\n  contoured", "true");
        addAttrib(attribs, "\n  nContours", "" + jvxlData.contourValues.length);
        addAttrib(attribs, "\n  contourValues", Escape.eAF(jvxlData.contourValuesUsed == null ? jvxlData.contourValues : jvxlData.contourValuesUsed));
        addAttrib(attribs, "\n  contourColors", jvxlData.contourColors);
      }
      if (jvxlData.thisContour > 0)
        addAttrib(attribs, "\n  thisContour", "" + jvxlData.thisContour);
    }
    //TODO: confusing flag insideOut:
    if (jvxlData.insideOut)
      addAttrib(attribs, "\n  insideOut", "true");
    
    // rest is information only:
    if (jvxlData.vertexDataOnly)
      addAttrib(attribs, "\n  note", "vertex/face data only");
    else if (jvxlData.isXLowToHigh)
      addAttrib(attribs, "\n  note", "progressive JVXL+ -- X values read from low(0) to high("
              + (jvxlData.nPointsX - 1) + ")");
    addAttrib(attribs, "\n  xyzMin", Escape.eP(jvxlData.boundingBox[0]));
    addAttrib(attribs, "\n  xyzMax", Escape.eP(jvxlData.boundingBox[1]));
    addAttrib(attribs, "\n  approximateCompressionRatio", "not calculated");
    addAttrib(attribs, "\n  jmolVersion", jvxlData.version);
    
    SB info = new SB();
    XmlUtil.openTagAttr(info, "jvxlSurfaceInfo", attribs.toArray(new Object[attribs.size()]));
    XmlUtil.closeTag(info, "jvxlSurfaceInfo");
    return info.toString();
  }
  
  private static String subsetString(BS bs) {
    int n = bs.cardinality();
    if (n > 1) {
      String a = "[ ";
      for (int ia = bs.nextSetBit(0); ia >= 0; ia = bs.nextSetBit(ia))
        a += (++ia) + " ";
      return a + "]";
    } 
    return "" + (bs.nextSetBit(0) + 1);
  }

  private static void addAttrib(Lst<String[]> attribs, String name, String value) {
    attribs.addLast(new String[] { name, value });
  }

  public static final int CONTOUR_NPOLYGONS = 0;
  public static final int CONTOUR_BITSET = 1;
  public static final int CONTOUR_VALUE = 2;
  public static final int CONTOUR_COLIX = 3;
  public static final int CONTOUR_COLOR = 4;
  public static final int CONTOUR_FDATA = 5;
  public static final int CONTOUR_POINTS = 6; // must be last

  /**
   * contour data are appended to a string buffer in the form of a 
   * <jmolContourData count="[nContours]">
   *   <jmolContour index="0" value="-0.033" color="[xff0000]" encoding="base90iff1" data="fractional data">triangle bitset data</jmolContour>
   *   <jmolContour index="1" value=" 0.000" color="[xffff00]" encoding="base90iff1" data="fractional data">triangle bitset data</jmolContour>
   *   <jmolContour index="2" value=" 0.033" color="[x00ffff]" encoding="base90iff1" data="fractional data">triangle bitset data</jmolContour>
   *   ...
   * </jmolContourData>
   * 
   * One presumes an ordered set of triangles.
   * The contour intersects these triangles along two edges or at two vertices. 
   * (see IsosurfaceMesh for details)
   * Each contour is a Vector containing:
   *   0 Integer number of polygons (length of BitSet) 
   *   1 BitSet of critical triangles
   *   2 Float value
   *   3 int[] [colorArgb]
   *   4 StringXBuilder containing encoded data for each segment:
   *     char type ('3', '6', '5') indicating which two edges
   *       of the triangle are connected: 
   *         '3' 0x011 AB-BC
   *         '5' 0x101 AB-CA
   *         '6' 0x110 BC-CA
   *     char fraction along first edge (jvxlFractionToCharacter)
   *     char fraction along second edge (jvxlFractionToCharacter)
   *   5- stream of pairs of points for rendering (created
   * 
   * 
   * @param contours
   * @param sb
   */
  private static void jvxlEncodeContourData(Lst<Object>[] contours, SB sb) {
    XmlUtil.openTagAttr(sb, "jvxlContourData", new String[] { "count", "" + contours.length });
    for (int i = 0; i < contours.length; i++) {
      if (contours[i].size() < CONTOUR_POINTS) {
        continue;
      }
      int nPolygons = ((Integer) contours[i].get(CONTOUR_NPOLYGONS)).intValue();
      SB sb1 = new SB();
      sb1.append("\n");
      BS bs = (BS) contours[i].get(CONTOUR_BITSET);
      jvxlEncodeBitSetBuffer(bs, nPolygons, sb1);
      XmlUtil.appendTagObj(sb, "jvxlContour", new String[] {
          "index", "" + i,
          "value", "" + contours[i].get(CONTOUR_VALUE),
          "color", Escape.escapeColor(((int[]) contours[i]
              .get(CONTOUR_COLOR))[0]),
          "count", "" + bs.length(),
          "encoding", "base90iff1",
          "bsEncoding", "base90+35c",
          "data", jvxlCompressString(contours[i].get(CONTOUR_FDATA).toString(), true) }, 
          jvxlCompressString(sb1.toString(), true));
    }
    XmlUtil.closeTag(sb, "jvxlContourData");
  }

  /**
   * Interpret fractional data in terms of actual vertex positions and
   * create the elements of a Vector in Vector[] vContours starting at 
   * the CONTOUR_POINTS position.
   *  
   * @param v
   * @param polygonIndexes
   * @param vertices
   */
  public static void set3dContourVector(Lst<Object> v, int[][] polygonIndexes, T3[] vertices) {
    // we must add points only after the MarchingCubes process has completed.
    if (v.size() < CONTOUR_POINTS)
      return;
    SB fData = (SB) v.get(CONTOUR_FDATA);
    BS bs = (BS) v.get(CONTOUR_BITSET);
    //int nPolygons = ((Integer)v.get(CONTOUR_NPOLYGONS)).intValue();
    int pt = 0;
    int nBuf = fData.length();
    int type = 0;
    char c1 = ' ';
    char c2 = ' ';
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      int[] vertexIndexes = polygonIndexes[i];
      while (pt < nBuf && !PT.isDigit(c1 = fData.charAt(pt++))) {
        // skip non-digit data
      }
      type = c1 - 48;
      while (pt < nBuf && PT.isWhitespace(c1 = fData.charAt(pt++))) {
        // skip whitespace
      }
      while (pt < nBuf && PT.isWhitespace(c2 = fData.charAt(pt++))) {
        // skip whitespace
      }
      float f1 = jvxlFractionFromCharacter(c1, defaultEdgeFractionBase, defaultEdgeFractionRange, 0);
      float f2 = jvxlFractionFromCharacter(c2, defaultEdgeFractionBase, defaultEdgeFractionRange, 0);
      int i1, i2, i3, i4;
      /*
       *     char type ('3', '6', '5') indicating which two edges
       *       of the triangle are connected: 
       *         '3' 0x011 AB-BC
       *         '5' 0x101 AB-CA
       *         '6' 0x110 BC-CA
       */
      if ((type & 1) == 0) { //BC-CA
        i1 = vertexIndexes[1];
        i2 = i3 = vertexIndexes[2];
        i4 = vertexIndexes[0];
      } else { //AB-BC or //AB-CA
        i1 = vertexIndexes[0];
        i2 = vertexIndexes[1];
        if ((type & 2) != 0) {
          i3 = i2;
          i4 = vertexIndexes[2];
        } else {
          i3 = vertexIndexes[2];
          i4 = i1;          
        }
      }
      v.addLast(getContourPoint(vertices, i1, i2, f1));
      v.addLast(getContourPoint(vertices, i3, i4, f2));
    }
  }

  private static T3 getContourPoint(T3[] vertices, int i, int j, float f) {
    P3 pt = new P3();
    pt.sub2(vertices[j], vertices[i]);
    pt.scaleAdd2(f, pt, vertices[i]);
    return pt;
  }

  /**
   * appends an integer (3, 5, or 6) representing two sides of a triangle ABC -- 
   * AB/BC(3), AB/CA(5), or BC/CA(6) -- along with two fractions along the edges 
   * for the intersection point base-90-encoded. This version is single precision.
   * 
   * type     f1     f2
   *  3       AB     BC 
   *  5       AB     CA
   *  6       BC     CA
   * 
   * @param type 
   * @param f1 -- character-encoded fraction
   * @param f2 -- character-encoded fraction
   * @param fData
   */
  public static void appendContourTriangleIntersection(int type, float f1, float f2, SB fData) {
    fData.appendI(type);
    fData.appendC(jvxlFractionAsCharacter(f1));
    fData.appendC(jvxlFractionAsCharacter(f2));    
  }
  
  /**
   * 
   * @param jvxlData
   * @param vertexValues
   */
  public static void jvxlCreateColorData(JvxlData jvxlData, float[] vertexValues) {
    if (vertexValues == null) {
      jvxlData.jvxlColorData = "";
      return;
    }
    boolean writePrecisionColor = jvxlData.isJvxlPrecisionColor;
    boolean doTruncate = jvxlData.isTruncated;
    int colorFractionBase = jvxlData.colorFractionBase;
    int colorFractionRange = jvxlData.colorFractionRange;
    float valueBlue = jvxlData.valueMappedToBlue;
    float valueRed = jvxlData.valueMappedToRed;
    int vertexCount = (jvxlData.saveVertexCount > 0 ? jvxlData.saveVertexCount
        : jvxlData.vertexCount);
    if(vertexCount > vertexValues.length)
      System.out.println("JVXLCODER ERROR");
    float min = jvxlData.mappedDataMin;
    float max = jvxlData.mappedDataMax;
    SB list1 = new SB();
    SB list2 = new SB();
    if (vertexValues.length < vertexCount)
      System.out.println("JVXLCOLOR OHOHO");  
    for (int i = 0; i < vertexCount; i++) {
      float value = vertexValues[i];
      if (Float.isNaN(value))
        value = min;
      if (doTruncate)
        value = (value > 0 ? 0.999f : -0.999f);
      if (writePrecisionColor)
        jvxlAppendCharacter2(value, min, max, colorFractionBase,
            colorFractionRange, list1, list2);
      else
        list1.appendC(jvxlValueAsCharacter(value, valueRed, valueBlue,
            colorFractionBase, colorFractionRange));
    }
    jvxlData.jvxlColorData = list1.appendSB(list2).appendC('\n').toString();
  }

  /* ******************************************************************
   * 
   * JVXL 2.0 encoding of vertices, triangles, and vertex values:
   * 
   * <jvxlSurfaceData>
   *   <jvxlTriangleData data="!]][[_]]Y`WbVa^]]] ... cZ_T^ZdUdTc!^[Dv][Bx-43,+44,]-55,f+43,_W`Z^X`Z ...">
   *   </jvxlTriangleData>
   *   <jvxlVertexData min="(15.218472, -28.304049, 34.71112)" max="(97.8228, 54.011948, 109.95208)" data=0HY0HZ0HZ0HY0H[0GZ0IZ0IZ0H[0FZ...">
   *   </jvxlVertexData>
   *   <jvxlColorData type="range|discrete" data="015.86++1@<?<D~4 CD2BDDCD*D?BCB?~6 @@.=??CAAC@?~4 A@A?CCD@?@?ABA?>B=<=~4 <====???>>>???,0<<<0/5;:;=><;=<<;,8:;:;=><BAA=?<;+,)*0+/<=<<<<==<~7 =<=<=<====<??<<>>>?=>>??>~5 ?>=>>>===<<<<;<<;;<<<=<<<===><====<==<=~4 <=<9::<<;;;::;;:;;<:;>~4 ;;==<=;=~4 <====>??>==>>>==<=<==<==>=~4 >>>>=>>==?==><~7 ;;;<;<<<;;;;/..0/<;316268<<<;;22:~18 ;:;:<<<=<~9 ;;<;~4 <~6 =<;;<;~4 <<<;<<;<<<;~5 <~16 ;;;;<;:;;:~4 ;:;;;<;;;:;<<<;::;;:;:99;;;:?@>@<<<;==<;::<<;=;<>;:<==>;<;@@>===<<;=AAAA;=~4 ;:=<::><::9::::9:;;;;::::;~4 :;;:;~7 <<;;;<:;::;~5 <;<;~5 ::;9~7 :999;:::;:9~4 ;:;~6 :;;:~4 ;;;:::9;~4 <;<<::999:999:~7 ;;;:::;:<<<:<;<::<;<:::;:~5 9:::99:999::999<<<=<===>~4 ?@?A@A?BABA===>>>==<A>><><>???B>>>BBB?~6 >>BBBB>@AA>~4 ?AB~4 <~5 9~16 :~4 9:~5 9:::9~7 8999:::9:::999:99::9::9~11 ;::::;;99:9::99::9999:9~6 :~5 ;;;988889~5 8889889998:989989~4 8~5 9==>==;<;=>;<====<;>>>=<<<<;;?A?AB<~4 ;=<;ABB<;<~6 ==<=<~4 ;;;;9~7 8989~4 88898~4 989~11 8999989889:::998888::7788789999:8989888898988988988989~31 ;:;<:9888999:;;;;<~6 ;::<<;<:~4 9;;::8887777556878887666787867757775~6 6~4 8789889:99:998999:999899::99:9~23 8898~4 99998~4 989998989767788876776696668766667899777889989999898~4 987778~4 9998989~5 898~4 78668777788767555655688786668998666967888668898~11 66687778~8 78~16 7766686655667688786~14 7766778898899:9:7899995554454~5 5544655456655568~9 9998889899998~4 99888777887~4 8889998766999896859988986666558865886695~4 8888998~5 9887776787~4 878~4 788445444554436666776~6 76~4 7778867788778666886667663~10 433223334444338~14 998~6 99899988898~16 66545888776787865777883333445456343~10 8~10 98~4 989~7 88993~11 878~5 9867766676776~10 333343~7 443~9 76776776633357776554755888878~6 787788877676666765999988878877766668866557563335564554555656577657755756655665664554434564465544467778878~4 7777898798989998889~5 5~5 6~13 776445565756675446766565566446~5 7~5 56665556666565644765588775767686676878~5 78787777886667~5 6~8 78~4 7777887678887869996~7 76~5 7667~6 8~5 78~5 98778887787887~7 ::886828::88988:9::9:88:98888:::88777769768987978889:96777788:::;::;:87789~13 8999777766665~7 6666566555599888898998988:::7787:::99897:889787~4 88887778887998898999-)**,+)*)*()*(+++(***7878889788989998999898777677776777876~4 7665652231~5 4/24656657677<<;<<<<5665656555665~5 4456565~11 6665~4 6655656556665657~17 6655777677554~6 33;4444;<;;4555;;5<;<<;;449;;;<7~7 67776777767~5 6656677657<<<;~4 <<<=<;;<;<~6 =<;5~14 =~16 <===8889999899998~4 99997887877878~7 998888987888798989999;;;9:~6 ;:::989;;;988887788665589886577887778877767~4 6655565789899998787878788788998999:9:~4 988766676~10 565<<<<=~7 <===<<=<====>>=>=:;~4 <<;;;:<::78878777677767~4 8877888787787~5 8~7 7788878878887~5 8777787~4 66788776767667~15 877887988988778978876~6 565~4 66656656;<;;;<<:;:~4 ;;;;<:~5 9:99:9~6 7~10 8~6 7~6 887~10 88878~4 7878866678898~6 6787768668778887777888788878778~21 6~4 5676~4 56878878~8 99888999::9:::9~7 :4~8 54~7 54446458~7 9888898889~14 8998989898~6 9889~4 88999898779889889969899687869969867866556989~7 89888898889~8 8894556568~19 98989~9 4~5 333434~5 55455454445~5 88889988899898988989~7 8~5 99998~8 98~4 988878656787888645~4 675455546778~8 988998884~4 34~17 9~10 889899989~5 89~5 8999989:9::9~7 :9~8 787899989~5 88799989889~4 :9~10 :9:9~4 :9:5598898769~4 688899:566645477767774644;;;;:::;;;;9:::6~4 777::::6~12 ::;:::4456~4 447~5 66664557776669~7 :::9:9~5 :;;:;;:~16 99::9::98:7:999988778988::::9:::99::999:~8 ;777:::8989:~4 98:7:8:;:8;~7 :7778<;<<<;<;~7 <<:<<::;<;;;;<;;<<;~4 <;;<~4 78;;:;:;;9;;:;<;~6 :;:;;:;~11 :::;:;:;~8 :~7 ;;::::9998~4 :::899888:;;::978;:;9999:9;;:<<;::;::::;<:::9<<<:;<<;<;;<;;;;:;;;;:9;;:;;;::;~5 :;;::;~5 ::9;::9:~5 ;::::;;:::;;9:~5 ;:~4 ;~20 :;;::;~6 :::;:;;::;~16 :;;;9::::;:;;;;:;~4 :;;;;:~4 ;~4 :;~6 :~8 ;;;;8988998889:~4 99:9:~7 99:9997789~5 889898899879878899889:;;;:~7 ;:~15 889999:9:9:89779~5 8~4 ::9:::9::9::9:~19 9:999989~5 ::;9~5 ::9::;:9::99:998888;~6 88;;;:9:9:::98;;::9:;9:9999::;;:::;;:~5 ;:99:~4 ;<;;<<:;:<;;<;::::<<<<::<;:::<~4 =:::9<;;<<<;<<<;:;<:;<;~6 ::999989:9:99::9998898:9~17 8887778767668~4 98:~8 ;<~4 ;<<<;<;:;:;;:;;:::99::::98:~7 ;:~5 ;:9;;:;9::9;:;8:9:9::98988899::9888878:~12 9999:~8 7677787676669~24 54~8 5656559:9:9:9:9:::9::;99::9;;;::9999::::;:;~5 :;:::9;::9~8 897798~5 999886667766678::9999898~5 7~4 88:;::99:99:;:5566456654~8 5449~9 ::::;:::;~6 ::;;;888989~7 88899988989989~12 89~7 667767755888776487888855666675644545555664~4 5~5 45~5 ;::;;;;<<<<;<~5 ==<~4 99::;;<<:;;:77998989989898979~4 :~5 9:9:;;::;;;;:<;<66699796999:767668888978998454566655;;;:::;;;;99:9::;;9;;;;8899899998<<<<;;;=<=:;;<<5556555=~8 <=<<====<666656776~6 55664446688998~10 98::9~5 :9:::99:988::;;;;:98889986666788889988669688677789978~5 776778~15 9889~8 ::::88666766766:::;:;;:;65~5 665556~4 56~9 8~18 98988998998899998~8 9999889888988878~5 6666777888688877778~15 998989998889955566555:~5 9999::;;:;:~4 ;;;;<<;;5~4 666565~4 7~8 88877878~15 7~5 87888778~10 7~6 87787766777677788677665567766667755675557~9 87787777566665~8 9::;::::;888899888989878~10 78787878778887778~6 7~10 8~6 7~4 8887888777876667768787666555665~9 787877887787788787~6 875~4 4457888778887~4 878778~4 5~8 4554559~11 8988898888798898~4 9877788889988877789876786::9:~5 7::::9889~4 8:788766999855775~4 9~5 889998998887978889988878~14 756667676577877776677767756666778776785667~4 577675759~13 :~9 99:99::9:9~6 ::::99::999898998889988997998897899889987888879~5 8979899879889~5 8899988989899998988899889~7 78~5 778978~4 76~4 56~8 999897~6 877889:::99978879:99:9999899:99987998889889~5 ::99998:::99:99:~20 7~8 6768787777676776554554664677677766989::::8899::999::99888:::9:~15 9999:99989998~6 :898~4 97887878988898888::9:::99:~16 8~4 :::99998:99:778566786998666:::9:~5 9:99:::98897996678;;;:;::;;:::56545644333343~4 4534:9:9::999:~11 ;;;:;899898999;<;99:99<<<;;:;;<877;<::99:::;:~8 =~4 <<=>====:=~9 <~4 66766559:;<<6;=46789:3334333435633=>~5 ==><;=>~4 ==>>>?=:<;:<;=:;54~12 55455534~12 9:9:;;::9;:::;;<;;<~4 ;;;;>==>==<<:;><~8 :=<<<<===>~7 7~4 87884555764577658799::;9:;988999::9::::668987=><89;;<;::95555657779986887955<==<<89;<;::;97778~5 9?>>???>>?>=>=56556666>>?@A<???@?>???<==<>;~6 <<>>;;;<;;=;?>?>@<;<<;666656~12 566668888:99:999;;:98~4 ;<::9::999::=>=<;;;:8~6 777677878~6 77878776677776777788;:99:;:~6 9:99:8>>>=;<=?>;>~5 ;<;?>?>7~8 666?~9 >?~5 >;;;;<;;?<?<<><<;~4 ?>;;;??@????;;<;;<9~4 889998~6 997878987786:999:999::9998889898889998898:999:9::99::9::99:~5 9:::888:99:::9::88889998899989~7 :9:~4 9:9~7 ::::9:~5 ;;:;;:<;8~5 ::::>><><==?;;:;::::;;<<;7978878889~20 89~6 89999899:99:9:::999:9::99988998889~4 ::999888878888779~5 :99:9~4 ::::9:9998:9:~17 9:::9::;9::;~4 ::88989898~8 9:~4 9:9:~9 99:999::9::::9:;;;:;::;:::;::::;::<;<~4 ;<;<;;;9::99988;;::98<~4 ;;<~4 ::9;9<;;:::99<<<;<<<;;;:;:<<<<;:899;;<<<;:;::<<;;<;;;;::<:::<<;;<<;<;<;;<<;<<<;:99<;:;;;:9:;;;;<;;:;;;;9<9;==>==>~5 =<<=~7 9999===:::<<<;;;9::=<;~4 <::>==>??>?~8 >?>>>>=>?>>?>~6 ==>~6 =>88788878799888>>>>====>>>====>?>>==?:586649568;88967878~8 78889:7:6786777766577:898:=~9 <<=<;;<::::;:<<<<;:;:9;7979::984655796:97877<~4 ====::<<=<<;=<<<<;:::989897778887788867;;:;998:;=~5 ;<~4 ====<<===<~16 =<>~4 ?>>=>?>=<;;;:;<;<=~5 ><>~6 ==>>==>=;<<;:;<<:;=:::?~4 >~6 ?>????>~5 ?>~6 =>~8 =<=<<;=;=><<>~6 =<>><<=>?=<?~5 >>?>?>>>>A@@@?@???>?>@==@>>?@@?>>>>??=<=<<>>====>==><?<=?>>>?<;;<;<<;<<<==<=<==<=<<<=~5 <====<>=~5 <~4 ;<~5 ;;<;~5 :;;;<<=<~5 ;;<~12 ==;<>====>=>=>~8 ??=>>>=~7 ???>>==>==>==????>>>>?====>=>=>>>=~19 <<=<=<<<====<~8 =<==<<<<====>==>=>===<<==>=~4 <=~4 <<=<~4 =<~4 ;<;~7 ::;;:;;<;<;<~5 ===<>=>=><<<<==>>>=>==>~9 ==>====>>=>~5 =>=~14 <=~9 <=~22 >~11 <<<<==>=<<====>====>~7 <~4 ;<<=~4 >>==<<<<=~4 <~5 ==<~12 =<~4 =~4 <<==<=<~12 =~5 ><>===<=<~8 =;;;<<;<;<~16 ===<<<;~8 =~5 ;==;;;<=<<<<=~6 >>>=>===>>=<=<====>~6 ?>>><====<>=~10 <===<~9 =<<<==<=>~12 ?>~4 ===<~5 =~5 <====>>=>=>~4 ;~6 <;;<<<<;;;<<;;<<<DFEDEFFFFGFFE~8 FFE~5 FFFEEFE~6 DDD=~50 <~11 =<=<=~8 <;;;;<<;<;<;~4 <~5 ;;<;;;;<<<;;;==<=<<=<===<=<===<~36 ;<<<===<====<=>>=<<<;<<=>=>>>?>>>>:~7 ;::;:::;<::;::A?@AA@?>A?@@@?@>???@A@?@?C???>>=>=;;:?<<>9=<7:>>>====<===<~14 =DBAC<<B===<:;<<;:?<<?=>8:8<;;<=<9<=<<=<>==<;>=>@@@<<=<~9 ?<>??<?A@?<;>@B9<=?=@>:@B@<<<=<===<~6 ;;<;<;<<<<;9:999::;;<:9;;:=<<<<=>>==>>?<==<=>@>==<=<=<<<====@AA@B@@AAAB@AAAA@<~6 ??>??>?~4 ===<=<<===<~5 ==>>==<<===>>@=<====>?===>==@=>=@>@~4 ?>>?@?@@@?<;<<;;<<=<<<;;<;;=>><<<;<??@>==:<:;;=~4 <=<====>=>===>>>=>=~4 >=>??====??>>?>>===>89<===>~4 :899<>=9989;9;;:>>?A???ABBABB@???>?~11 @A?>?>???>??>~5 ???>~9 ??>?@@????>~7 =>@>~9 ??==>=>=~6 ???@~4 ?AAAA@@AAB@BB@A???====>~5 ?>=>>??@@====@A@???AA??@@@?~4 >???>~4 =>>>=>~4 =~5 <=<==>=<<==>==<<<<==>>???>???>>>>????@~4 ?@@@?@@@AABAB@AA@@?BB@~5 AA@@@?@???AA@~7 A@~6 A@@?@~5 A@?~4 @~6 ?@@@?@@@?@~5 >?~4 @~5 ??@?@??@??>>>===>====>=~12 >=~8 <=<<===>==>==<<>><<=>><>>>==>>>==>>>=>?==?>~8 ?~8 >@???@@=>===>===?@>@@?=>>>>??=>??>==<<=?<~5 ==>????@?>==>???>>>>???>?=>>>@@???@@?>>;9?>>>=:9<>>>>?>>>??>>?><>9::?>?=?=>>>:<<<><>?;7999:8=?<;<?>66;==<==?~5 >A@@A?>@@>@?@@?@@@@???>~4 ?<>>?@CA?@??@@??@@?@@@@?@@>?>>>=>>>>====<<==<<<;<<<;;<;;;<<<;<<;~4 <~5 ;;;::;:9::9~4 ;:>>====<===<<=<<<<?@>?>?>>?@>=?=~6 ?>>?>?===???@?>==?<=~4 <~7 :;:;999:99887789~4 8777899976669988<;<<<:::;;<9998898798768776~4 7:;:;:9888989889~6 8~5 9889888898899988898~8 787~4 66687776776665889:9~5 8999;;::;:~5 9999::;~7 :~4 ;:;:~7 ;<<::::<<<8~5 98~5 98?>?>~6 ====?===?>>>>=>::;;;:9:99:8<<==;:>96=<=~7 <<=~4 >>=>=~4 <==>>><=<~8 =<~8 ==<=<<<=<~4 ==<~4 ==>~5 =~5 <~5 =~15 >=>~5 =>====>==>>>====>~7 =~5 >>>=~4 >>>====;~4 <;;====:;;:;;==<=<<<<;<=<;<=~18 @@@?@?@@?@?<=<<=?===>==>>?=?>>=~4 >=>=>?<?=;<?<>=>==<==>>?>?>??>>>====>=>==>>=~6 >=~7 <<;;;>;;<=<<;=;;===<<=<=~7 >=<=;:7>@?@?<<;<<;<~10 =>==?==?>>?>>>???=99;998~10 :8998;::9;;98<;:=9=:<;8;664687787574325289:<66FFFGFGGGFGF~5 EFFFFEFEF~4 EFCCCEBDGEEFCC@DDBF=~5 <=<<<=<~7 =<~7 ;<<;~6 :;;::<~8 ;;;:9:899;<<:;:<;;;;<;<<<;;<<<;:<<:::;~4 =?>887;<=>;:99::::;:;~10 :;::;~4 79777878E~6 DEE8EEEED;=;9;<<;;<;;67;~5 <<6;<EDDDEDEDEEEED~7 ;;::;::;<;~4 <~7 ;<:;;:;~9 <;~48 <~7 ===<=<~12 =<<<<===;<====<==<<<=<~20 >~8 =>?>=><==>=><<<<=<<<=~4 <<=<>=~4 <~10 >>><<<<==>===><<==<=~5 <<<=<==<~7 =~9 >>>>=>~4 ==>>=>>>=<<===<~4 ===<=<==<=~11 <~5 ===>??>==>>=~10 >=>===<<<===<~6 =<=<<=<<<<>>>=~14 >=~9 <====<====>>?>~4 ?>>??>?>?>>=>>>>=>>>>==>~4 ?>====>>=>~24 ?>???>???>>?>~5 ?>=~4 >=<=<~8 =<=<=<====<<=<<<=<~4 ;=>=>==<==<<=<<<<===<;?=<>=<;;;<=<<==>>=>=>>>><C?C@C@=BABCD@?@@??@???@??@@@@??@???>===><>;<@@@??=>>?~4 =>????<>=??@@?><<<<==<=<==><;:;;<>=~5 >====^__^`_]]]^^]]_^__^^]]]^^^_^~4 babadffcgddfffcdeb^^^```????@?~10 >?~8 !!![!^_^][!]!^!![![[???>?~5 >@??=?=?~4 ><<<>>><;<=<=>>=>=<=>~4 ?>=>~6 kkjkkhhhg!^!_^Y^!Yhfd_`!]gbbeeY[![[!ZYZ!Z[[_!`a`^Y_dfZWWX[kkjjjkhjgjjdjjedkkkk_[a[aXXWWWX[cdicXVXVYYVXrrrsposlkkkkjkkkikilkmknlh_^^]lgmhb`eaoonqlYZ[XVYY[Z[Y[[[[Z]^][[]!^!]YYYXXXYXYYYWXXVWXWXZXSSVRSRRRSRSRRVSVZXV<~7 ;~7 <;<;;<<;<~4 ;<~14 99::9::<<;;<;;<:~6 <<<<;<;~4 <<:9:9:9:;;;:::98899<~22 ;;<;;<<<8887~4 877877;:9999:<<<<;<<<;<;>~5 ?>????9889????:=:>>899788<?>????>?;<<<<>>>>=<<==<<=>>?>>>>=<=<>>;<<;==<=;<====;==ECEC???BB;=<HIJ@EE88:9878:3443333654545444455432553:845545~4 :89955588798554544456<<B?89=56=?=<<<<:<<<<;<~4 ;<~6 ;~5 <;<4636.333387867767741354655623665564044547~6 8777865=>>>??>?=;:;;<<=<=;;;<;EGFHGHELLKKAGENK7679HDI86NONDC567::;::::;::<9:?~8 <GHIGGHIIHGGGHHIEIGH204D364=HIIJI3<H./KFGFFFIGGG--//0;:;:;;;;:::;:~4 ^_^a]cd`alllkleeflkihli;::99:aagg9f``b^]]]][^!::9999fffc]a_a_`zyyzrr{twwzy{|zzkklkklkjkikwqppvtli^^iWWVVX`Wj[[]Z[[[[Z[f`WXvxWWfbZYY![]]]!]!]![~5 Z[[[ZZ]^~9 _[[^^^__^^ZWYXXYYZXYWYWXXYX[:;;;:;;;:;;9:;~4 :;::;;:;;::::=>=:;::<==>;:;>=<=<=;====<>>==>>>??>=??>A~5 @AAAA@~4 AAAA@@A@AAAA@@AA@@A????>?>>>=>>>>?>>>A@@A@@??A===>>A@?@A@@?A?~4 @?>?@???@@??A~5 @@@A@?A@??@@@A~5 @A@@@@???>>>?~6 ;<<;<~5 ;<<>=<=<;>?@??@?@@?@?@@??@@??>??@???>~6 ?>?>>?=~4 <<===>>???>>=>>=?~5 =>===<=<<<>=>=>=<<=>>>==<<=~4 >>=~8 >>==>>>>?>??@@????@?>===<~13 ;<<<;~8 <<<=<=<<<===<<<===<=<=<~16 =<=<=<~5 =~4 <=>=>?>;;<~4 =<<;;;;<<<<;;;<~4 =;~4 <<<;;;;<~9 ==<=<<;=>=?==>>=<<==<==<====>>>=>>=>~4 ??>>??>~7 ====?>>>>??>>>>??@@?~4 @????>=~4 <<=<;~6 <;<<:;;;;<<>;=9<<<?=<<<<;=~14 <=<<=~11 <===<=;===>>>==<=~5 <=>=>====<<<<==<<=<<<===<<<=~6 <=~6 <=>=<<<=;===<=~7 <~12 =<====<==>=~6 <=~5 DA@DC<=?FDCEDHJJH=~11 <==CEEBGIBH<~19 =<=<=~4 <=>BC=~4 ><<===<E<CJKIGHBJ====<=<==<<<===<<<==;;<~11 =<~9 ;~7 <;~12 <;<<<=>====;<>>>?=::9:;;;;:::9<<<9=;;;<~6 =<=<<<<;;<;;<;<;~7 <;~6 <<;~5 <<;;;<;~53 :;~10 <;;;<~6 ;;<;;<;;;;:::<:~4 <~4 ;<<;~5 <<<;~8 :~5 ;<;;;<<<;;;;<~17 =<<;;9::;;:;;;<;;;::9998;:;99889:98888;;:::9<;;;;GGIFJHLLBCLFHI<CA==<===<<<<===<=><=<>>=<=~12 <====<<<=~7 <;<<==;<;:=:::;~4 <<:;:~4 8:9988977===<;<;hggjhrnotvnsiqvfhffeefjyxyhghxyxtyzxy|xtmpxxxefcccdecbcbccddgffbabccabadcccd|zz{~7 zz{{zz{wz|zzz{{zyyyzwywwwsttrrnzxwssssnllmmvqpR~13 QRRQRQRQRRPRQQQRQQrwnvxxruuolsjihivgebaa```gb```cdl```bbb`bi`^_`elfnahae`p^_^^_^skoressnajbga`b_e?>>>??>~5 ?>>=IMIK:;:~7 ;:~4 ;;;9~5 :999:9:~9 ;<~5 ;;;<;;:9:~13 9999888899998899889~4 ;~7 <;;<~4 ;;<<;~10 :~5 ;<<;;;:::;:::;~10 <<<<;;<;<<<<;;:;:9<=<;<~4 =<<<===9:9:889::;;<;<99=;98:<<===<<;<;;;;:<=:<~8 ;;;9~5 :99:;;;;:;99<~5 ;<;<;;<9988977778788865767::8998:89==<=<<=<==<==<==<=<~8 ;;<<<<:;:;;;;<<<;;;<;:::=~11 <===<~12 =<=<<<<=<=<~14 =<=<;<;<;~7 =~6 <<=;;=<<=<==;<<=;=~4 >=>><<===<<=~10 >=>>=~6 >=>>=;<<<=<<<====>>==>??@?>>>??>>?@?>><~5 >==>??>=>>>>?>?>::;~6 <;~10 <~8 ;<;~5 <<;<<<<;;<;~5 :::;::;~13 <;;<;;<==;::;<;::<~6 ;99998899889;;:;:;;<:;:;99;:;;9:;:;:;;<;<<;;;;<<;<;~4 88898899;;<<;~4 <<<<;<;<~5 =<<=<;<<<==<=<<==@>??;==<??<<<<>==>>>;;>>=<=<;<=><9;;:9:;;;;<~4 ;~5 ::::;;:::9::9898::::999;898878989;9:9988::87887878778888::::;:89::::9:<==<<<=<9::999:99::;::=<>>=>>><;>>><~4 :::;;<<;::::;<;;;:~5 ;::<;<=>==<<;<;:==<<;;<<<=~4 ;;<<;<<<;;:<=<==<;>=>>;;;==<==<>~5 ====>==<=~4 <=~4 <<<=<~5 ;=~10 <<=~6 <===>>>==>=~15 >~5 =?>===>==>>>=>>=>===>>>=~10 >:;~9 :;~4 ::;~5 <<<<=<;<<<<;<;<==;;<<<<;;>>>>?>>>><<<>=<=<<<;>>>=<>>:<>>=9::;;::;9=;<<;::;==9:::<;;::=::::>=>~4 8998999899:88787889999:9999:999:~12 ;~5 ::::;;::;:~5 ;<==<<=~5 <;~5 =<<<=~11 <;<<;;;]aa]_a_ba^eeakljlihkllkkkgffeed<<>>>=;<<;~4 <::Y!Z!Z_]ZYX^`db^!!ff[cccbbababcbcb_`bc[[Wdb^[]Z[]`dXW[eaYXVYehgdextsdccccnm{xystuw{wsf[XeXW_Wjk_XZmk7~4 :;::::88::9:999acb_`_`^^a_accba__]bca````a``ab``<=<@@@```_``_^_`]^^==<==5657989<6>:>:=443444756><<=D?>??>>>>???>>>??>~4 ==>=>=>>>===BBADCG====>=>?>>====>~18 EEDFEGGE~19 DDEDEDE>~8 5453552256;5986A:<;<<77767;::;99665569797;::7656<<;<:;=>====:9;9:66:967577888989766555656687677768978<;99;;<:;9::;~9 :787;;99;77;:977;;;<~8 ===<<==;;==>=>=>=>====<=<;==>=>=;;;<~5 ;<<;;<<<;;;<;~4 <<<;;;<;~13 <;;<<;:;9;9:::;<74;;22487;<;<<;<<;<;<~5 ;:;;::;~5 :;;9:::9~7 ::;999:9:;;998;;:99:9999778788789889::99898~10 778777688758877887675656777<~6 ===<=<~10 ===<~5 ;~4 :;::;:;;:9===<<<=~5 <~6 =<<=~7 <=<~5 ;<;;<~4 ==<=<====<<<::<<<;;<=<~6 :<;:9~6 <<<;;;<<;<<<;:;;::;;;;:::99;99888;;::9:<<;~8 <<;~9 :;;9~5 8887~4 677667678898:;;;:;~13 :<:?;:~5 ;9::;;;;<;;;999;9:::877887898978:~5 9::98:966677887777668777A@;<BF8~7 56679887~4 889877998998:::89~4 77;;;9:;97878~5 ;;;;99:999:999987::;>989:;::::9>?@@A9~17 <<<::;<::;<:9~6 =<<<;<<<;::;:;9<;;:9;:;:;;<<>==>><<<>===><QQQQRQQRRQPQQQP~8 ==>>==>=~7 @@@A@A@A@@@AA>>>>?@@?~4 @?>~8 ?=>>>=>?>???@@@?@???>?@DDDCDDDD?@?@CCCADDD@@???AA?@AAACABDDEEEDECAAA@@????C???GGGEFHHHFHEFGFFFGFHFHGHHHGG?~4 @@>~4 ?>??BBDB>??>~8 =>~4 ?===>>>>==>~5 AA??@>>@ABBA@@B?@?@??>A@?A?>>@@???A~5 >@?~4 @?>>@??@@@@?@@??@~5 A??@~4 >>?>~9 ==>@@@@>???>?@>?>>>====>=>=<=<~5 ===>>=>>==>>><<>;>=><<<;;==<=<<===<~5 ====;;;:::;;;::;<<;:=<;::;<;=~4 >=~8 ??@@===>>>@=@@@==?==A~4 BA][]XXZZVVXYVUUWUVVUZ[Z![]^_^^_^_^^_^^ZVTSRSTSSTSSSR<~8 ;;RROOQOKHHLJFRQOQOPOQFGGGFFGGHGFHFFFFHFHLKL8~13 77877878~6 <<;<;<;;;;8~4 <<;;<899::;;:::;:98888998;;<;:998:;?@@@>?>>??>>?>?>>?888?>>>8~6 =>>===9:;8:8~6 ;=:89;98~4 ?==<9;<<;98?@???>><>~4 =~4 <<;<>>>===<8888;;<;88989:::9<9:99:~4 ;:;:<:::;<:8:9::::8~6 >~6 =>>>887888877779899878778:7;99:::9998~4 77676~5 :;::;:::>~8 =~4 <<===<<<<;;<:~4 ;:;;99::9<=;:89889ONNPNQQPOPPQPQPOOIHHJJIPOPPLLLMNLKOHGHLFHEFL:~7 ;~6 :~5 9::9;<~6 ??@?~8 >?~32 >?~10 @@@?==<>;<;;<<<;>=;;;<=>=?E~11 D~4 ==<=<;::<;::;;<=;;;;:;;;<;<~9 =<<<<==<<<;<<<<;~4 <<;<~21 =<<=<;;<;<;;<;<<<<;<~9 ===<====<==<<;;<<;<====<~21 ;;;;<;<<<;~4 <;;<;;;===<<====<<<<==;<<;==<=<~13 ;~19 <~5 ;;;;=;;<~4 ==<<=;===<~8 ===<~4 =<~11 ;;<=;<~4 ====<<====<==<~6 =<<===>=<<=~5 >=~4 >>=>?>==>>??>??=>>>==>==?>@?>@>~11 =>==<<==<????@??>????@@@@>>????@???><~5 ;;<~5 ;;;;<;<<;<;~6 :~7 <<<<;:~10 ;:;<<;:~10 9~5 8~4 9989988:;<;:::HHIHIIIGFHIIIHIGFHGIFFF>?JI?>?JJJGG?>FFFH?>=OPOOOOKMMKJLNONKPPOPILIJ???>>?>????@>=~5 >@==@<A>=<~4 ===>>=>=?>=>><<=<>>>?>>A@A@CC?~4 @@@?@?ABC@>>?A@BACDEAAB~5 ADQQPRRPPOPMKKMNORPRRRIIJIHPQPPONMMLHKHHHHA@@A~4 ?AA=>>>=>@@@B@@@?@?BA@???@@>@@@>@>>===>>>=>>=~10 <<<<=~5 >=>??>@@@=>=>>?>>>?>>@@@@????@>=>??=>???@?~5 @=?==<<<@?>?>?@@<@@>>>??>???>?~7 >A@@@?~4 @??@~8 ?@@@??@?@?@@??>?~6 >?>@~9 ?@?@@?@?~5 >?~7 >~6 ?>?>????>>??>~4 =>>====>??>=~5 ????>~5 ?>>?=>>?~8 >>>??>AAA@?A@AAA@=@@>=><=~4 >?====<===>=>>;<;;<;=<<<=;;;=~14 ;;;<<;<;~6 :~5 ??>?>=@@@>==>=@===?@>>>=>=<<====>~5 @~4 ??>>@@>@>?~7 >>>?>??@@??@????@BB?@?>?>@B@~4 A@??>??@=~6 ;<~5 >>>>=<=<=;;<===<===<:;;;<;9;9;=<;=89:9:9;:;7776667755668568778888=<<;<;::999:9:;9:999;88788;<<;:~4 ;::;:98:~6 7877878:::;;;:::99:885799<:<<<;:;:===<=<=;;<;<<<<;~4 :~4 898~4 ::9:99:::89:9999899<===<<<;;<~5 ;;:<:9:<::9998998~5 98878778877<<<;<;99::;;;;::98~5 96~6 99777766887798978787688878~4 7878998888989:9:;88:9898877768778~6 78876677677767666868~4 9~6 ::99:9;9;;:::;;99:~5 ;:9::::99:;;;@~9 ?@?@A~8 @B~4 ABB?@@ABBAA>A@AAAA=~5 >>A=>AA===@@==><=~5 <<>>?>>=~4 ?@??>====>>?>>>@A????@A?><@??@AAA<BAB@?@AA@?;?<=B<BBBB<<<BABAAA@AAAAB@@>?=><<<<==>?@<<<<@?@<<<<>><~5 ;<<;~4 <~5 =<=;==<;=~4 <;::;:<~13 ?@~4 ?AA@;:;::<<<::<<;:;:~6 99@AA@~6 AA@~5 A@@@@A@@@B~4 ABDCAAABABBCBA@>>AAA@>@AAA<=<@99::999:::9:@~4 A@~5 AA>?>?CBBCABB@A@AABB:;;:::<<<;;:~4 ;;;<;=;<=~8 <>>>=>>===>>>=>>==>>>=>>????>>>>????>=>?<;<<<=;==;<;;<<<<;;;;=;;;:;:~4 ?>?<<<?~7 =>=<=>??<?==>>><>~4 A~7 BAAAA<<;;<<=;=BABA==AAA==BB=~4 AAA@A==BCBB:;;BBBCBB:::;BBBB;<;AA=<B<;=AA@@A@@???>=>A>>>>?>>?=<===>=?B@@@?AA>??@??@???>~7 ====@~5 ??>==@@=>@>>>=~10 >>>=>>=>=~37 >>=>??>~5 ?>?>>??>?>>=~4 >==>>>==>=~6 A~5 @A~6 @????@@?>?>>?=@@A??@@A=~5 >>?===>=>>===>~5 =>>=>~11 ==>==>~5 =>>=~7 >=>>=~15 <=~4 <=<===<<==<==<<<=~7 <=<=~15 <===<~5 =<<<=<<==<<=~8 <=<<<<=<<<<====<==>>=~6 <~8 =<<>>>=~4 <<<<=~5 ;<<;;;<;;;<~15 =<<<<;<;;=~6 <>>===<=~6 >~19 ==>>;~11 <~34 >~7 <=~4 <==<<<>>=<<====><<;==<=<;<=~29 >=~5 <<====>=<=<>=>=<~4 ;;<;<=~21 <=<<<<===<<<=<=<<<=~5 >===>>=~12 >=~7 <<>~22 =~17 <<=~11 <=<<==<====<===<<<<=<<=<====<=<<<<==LMLLLLKIJLLLJK=>=~6 >=~5 >>===IHKJGJHKLLLLM===>~7 KFJKJJHFJJ>=~10 <=<<=<==EFFFFHFHFIHFHE?DCA<===?<<=CDCC<=~6 FGGBD;;;;DGFE;<;;=H;=>>CH<=<~4 FHHIEIG=<==<~4 ==<=<>=~4 <<<<==>~8 ====>==<=~6 <=EE>===E@;<~5 ;~4 <<==<;<<<;;<<<==<=<==<=<==<<=<=<~6 ====<<=<====A?@@@C??=>?>?>=?<<==<=<===>>=~4 <=~4 >>?>>??<<<<=~4 <<=???>>???===<=<EEDEEDDDDEDEDDCD;~7 <<<<;;<;;;;::;;:;;::;;;::;~4 <;=FBFBHHHG=>=>>=<<<=IKID;<=GJJJ?A@CB?>>@?C@?><;<;<<<<====>;=<==>;;;>;~7 ====<=<=~6 <<<=<<<===<<>=~10 >=<====>=<<><~5 ;=<=<=~4 <<<;;;;<;~5 <<;9999:::9::;<<<=<<<;=<<<===<~5 >>=>~4 :::<:;:;:9~11 ;<<=:<;====:<=~9 <<=~6 >>=~5 >>>=::8:999;;;999;:;;<~7 =;;:::;;:<<:~4 ;~4 <~4 :<<<:::;::;<==>==<<>=>;=>>>==<<<<=<=<=<<<<=<~4 =<<<==<=~4 >DFFDEHEKJGKIIJIGILKKKMCKKKLJKKKII?AA><<?B=<=<ACD==<=<=KLKLKMJJ=~11 >=>~4 =>>=>~6 ==>==>==>>==>>>=~4 >~4 =~4 <==<=<=<=~4 <<<<==<=<<=;<;;=~18 >==>=~10 >>=~10 >=~4 <=<==<====<<<;;;;<;=;9~10 ::::;;9999::9;~8 :9::;;:<=999899:;:::;::;<<;=;<<<;:~8 ;~4 ::;9:999:98889:;;;;::;;:;8787788997~4 99::9877888968867776788898899989997798887;<;~7 <;;<<<===<<==<<===<<=<<=~4 <===<<<==<<<===<===>=~8 :<9<;<:<<<<==<;;;;::899:;9998~4 JGGLD>F9;=>?@46<@E>>=F>===>=~4 ::<<;<;<<=>>>>=>>>=<==;=>>=CC;6@8=6DA7:98798;98?~5 @CAJDG@CLD?8?::?55<6@A6675@95553434558C9C;74755566665~7 DQ77:9NNOFM::9:99<?~9 >~4 =~4 ????>???>>=>====HLKJ=~5 AB<<<=<<<=<<====<===<====<==;;<;=:;;;:<<=<==<<=<;<<GEEEGDDED<=<<==>==<=>>==<~5 =<<=~5 <==CF>FCF<=<<=<~8 =<<<>>=<==<<?<<<<=<<<@?B@??@??@?@~4 =~4 <<<==<~6 =<~6 ===<~6 :;;;<~5 ;;<;;<==::::<~4 ;;<~5 ;;<<;~4 <;;<;~7 <<;~10 :;:;;;;:~4 ;:;<;;<;~9 :;~4 A>?;;;;@?;~7 ?>??@<;<;~4 <;~8 <;;;<;;<~4 ;<;<;~10 <;;<;~26 <;~5 <~4 ;~4 <~5 ==<==<<<<=<=<<<==;<~4 =<<<<;;<<;;=~4 <~4 =<~20 ===<====<=~7 <<<;;<<;;==;~4 <<<<===<~4 =;~10 <<<;<~10 =<<<=<==;<;<;;<<;;<;~11 <;:::;<<;;<<<==<;<=:;=<:<;<~4 ;;=<~4 ==<<<=>>=<<=<=<~6 ====<<<<===;;:;<<;;;:;:<:::<;:=>=~8 <=<=~7 <===<==<<<=<<==<>==<<<>>>=~8 >=<<>=>==>>=>=>>>>=~10 >>;<<;<;;==>=~7 >=<<;<=<;~6 >~13 ====<<====<=~12 <=~26 >>?>?>===<===>??>=>>>=~5 >>>>=>==EEEDEDDDC~5 DDCCCDCCDDDDCDCD~7 EBAABBABA~4 BA~12 @?@@A@AA?@AAAA@AA@?@A?@>?>>???@@?A@?@??>?>>?>>>??>~6 b~26 acaabcbbaaabbbabbbbaA~7 @A@A~6 WWWWVWXVWYXXWWXWWTUWUXWXWY@A@@@@AA@AABAAA?A??@A~5 @@@AXXXYXWZYhhgih[]!]eefcdeefcdca`dde`bcT[T^^XZUWZXZ[Y[Xcdd^]_ZYWWZmlkffmmhjeWVWW!!]bb`UUTUWZTTVV[^!]ZZ!Zcc`ccdddffihi[!jllkikkjkic`b_Zj9==:?<<@<?:<::;::<::?>~6 =>~5 <=<<=>=>><<==>>>=<???>==<>~5 ???>>>>???9:99=<====;;<==<:999==<99:;8<=<=;::;=>;:>>>???>=<<><=>?<<=?>;>~10 =>~10 =>====<<=>==<<<=<<<<;<;~6 >====;:;====<<<;~5 <<;:<;===>>=>====>~4 =~6 >====>?~5 >>=???==?~10 =>~5 ?>~10 ??>A??A?>>?>?~5 ==>=>~5 =>>>=;~6 <<;<~5 =>?~4 A@@@?@????@@?@A~4 BA@SSU?BBWSVZPPP?BEKXQPNNONNPQP>ONNNJOMMMJKQQPN~4 B>@@>>>=~4 >C==?PQ>=F>====HDLC?>=>>>>???@B@=@<<<==<<=<<==<===>>====<?>>><~8 >>>>@==??<==???>==<<<=<<====<>>>=????=<<<?>>=~11 <~4 ;<=;===<<;:::;~9 ::;:;<=<=<<<;<>>>>=<==>;<=;<=>?:~6 ;~11 <;<~4 =;~5 =<=<;<;;;<~6 ;:;<~9 ;<~8 ;<~9 :;::;:;;::::;:<<;;:~5 ;;9:;<<===<<<==<<<;;;:::;<;9;:;::=~5 <=~6 :::====>>====chbh_``bcmnmnhlnl]]]]b]^!]]bklhklkglll=~6 >>>==?>???>==bb_fg[^[T]mmgpmlm]mngbebdcfebaaVTVTSYTUX!!Y!W]VV!`[YYTXa^_aVU_ZZZgfefeebRUQURUWWUT^baWUUU]bdcTTY]WVVVbbbaddecdeecdcd@=<A?=<=<=<=>><<<;;:;;<<<;;;:;;;<=<~5 ===<<===<===>>>=~8 ;<~5 =<=<~4 =<~5 =<==<<<<=>=>=<=<==<~6 ;<~16 JJLJHKLLLLKLMMLLKLLKHKNNNPOLI>?>????;<<>>?===;~7 <~14 ===<~16 ;?>??>~5 ???>>>><~4 =~8 <>~4 ?>><==>===<=<<<==>>>?==<=>>=<<?>???<~4 ???>?>?>>A@A@???@~4 ;=<<<==>>?=;>=<?;<;;=;;BA@>=AABBAA<<<A87666989::;:8;<;:;:9:KGIIEFGLGHKNKJ>3HL1BE<6CO47657999:;;5699986897:660116575224<;;:~6 ;;<;~5 :::;:;<~6 ;;;<<;;;<<<;<;;<;;;<<<<;~4 :;::<;~5 <;;;<<;;=<=<<===:;=;:;:::;::=;::>><===>>>==>>>?>??A?>?AAA>?>A>>>?>?AAA;;:;HFEG[!YWZ!XWUYUWVWUYXWWVWUY!XWWWWRRRSSQSRSS^~8 99:~17 ;::;::9:9786658666;596857996<8<7989~4 :97899778876997=~13 :~10 ;:;~13 :;;::99;:::778;8~4 788:;8989~9 89:78~4 586885:5698;6677888766588>>>><<<;==<;><;<=><;=~4 <;<;<=;;<=>>===<<<<=<<<<;<~20 ===<~28 ;~8 <;;;9:9898999::999:~5 9~4 :9:::;<;<<;<<<<;;;<<;;<;<<;<<=<=<<=~5 <<<<=<===<==<<<<=<~8 ;;;:;::;:;:;::;:;;;<<;:;~5 <;<<;<<=<<;;;<~15 ;<<<;;<;<;<;=<<<;;<<<:<:;:<<;<<nopllkjkhikjlmlprrpppqnqoorppmkknnmghjhhjfffgh~4 ileefddeh~4 ei:~9 srsrptmrptqqpowwtejflllhmunlffdhfjsvvrlrkknmnvvlehfdckgfmmdljfjmllkmqpqlmkmussutturihjffigfgffikleffefjkefedccdddcghflpelmkjkngkbbccbcbcbbccbbbbcifdcjffe77888878:9:;97:<;858768949~7 59999859795=~4 <<==<=~8 E~16 DDEDE~11 99:98868:7;;<;<<;~9 <;<=<<;;;;<888799989;~4 ::;~8 :::;:;;;;:;:~4 <;~4 ::;;:;::;;:;:<<<;<<;;<<<;<<<;<<<<=<<==<<<;~9 <<;<;;;<;;=:::9999<;;9;<<<=<<;<<::;9;;;999<==<;;;;=;<;;;<;;<~6 =<<<=<~4 ==<;=;;:;;::9;~4 :<9:=;<;;:;;9;;<<<<====<<<;;=;;===;~10 <<<;;;=<~11 ;<<==<~6 =~7 <~6 ;<<;;<~16 ;;<<<;<<<:;<:<<===<;;;<~4 ;<;~9 <<<=<<<==;<==;<=<=<<=<<==<==<==<=~9 <=~7 <====>><=<<=<=~8 <=>>>=~4 >=~4 >====>===????=<<=>>=><===>=><==<====;==;<==>~5 ?>~9 ?>;<~4 ;;;;<;;;<;;;<<====<=<<<=<~11 =<<<<=<<<;;;;<;;<;;;:;~7 <~6 ;<<==;<<<>==::;;;:==;;;;>>=;=<==<<=;:::;~7 :;~7 <:<;;<;;;<~5 ===<<;;;<~5 ;;;<;<>???@@@?@@@@?AA??>@<~5 >>>>=<<=><<<<=>?=>=><~5 :::;;;::<;~7 <<<<;;;:;;;;<~6 ;::;<;<;;<<<;;;;<<<<;~4 <<<<==<<<;:<;=<<<>>?@>??A@AA@?>??==<<?>??=???<=<=~4 <=<;<~4 =><<<=<<<<====>?>>=~34 >>>>=>>>=>>===>>>==>>>==>=~4 <<==<;==<=~7 ;~4 >~5 ====>>==>=>=>~10 ===>==>>=>~11 ==>~5 =>~7 =>>==>~4 =>==>=>=~8 >~4 =A?A@@@???>?=@A@@?@@>>>?@?>=>>?==>?><>=~4 >=~5 >===<<<=~8 <<===>~8 =>==>~4 ?>>?>~6 =~4 >>>=>??A@@AABAB@@@?>>CABBA@B?BB??B~4 =<==<<<===<=<=<~8 ;<;;;<;;<<<=>=>>>==<><=<<=~7 <=~12 <=~6 A@AAB?B^h]gfBf^_fbXXZ!YXZYYBBkkjj!C?@????efhnnkmkmlnlkllk??>^d]VZ[WVX_eXZ[kjkUYVTTTRQcddddecccddddcd`cacc^^VWaaVTWacca!ccbbU!VVTTUTYYXUWWXbXWecRc`TUVUafj_XVkiffdblXnmoUTTUh`befmXompoek]k<=>=ebcbceecdbbbfabhhhghfhgddA~4 DDFEEDDCCDDDDCCDDDEDE~4 DDhfececfcbdbbchgfd<<<<==<<<==<E~4 FE~11 DEEEDDDE~6 ::;:~4 99989989:979::9:988;<<>;B?;>>:<:;>>=>>>>=>>=~12 ><=>>====<==<;;====<~6 ;;<;<;;;<<<;;;<;~5 @<AAEEADDAAA<C::99:8998:<999889~4 :9;:;;==:9:8:9<<<<=<<:::<<<=:~5 ;:;<;::;;;:99;;<;;9999<:9;:::9:~5 9~4 ;:;<<<:;:;;<;99:999::9:9999:~5 999:;~10 9:9;;;=<<==<~8 =<=<;<<<<;<~4 ;<<<;<<:;;:;::<;~6 <<;;<<;~10 <<;;;;<<;~21 =><;?;~6 >;?;;<<<;<::<<<<;<<<;;;<<;:<~9 ;;;<;;<;~4 <;;;;898;:;;<<<<;:;;9898:::;9;9;;;::;99::;;;::;;9:;<<;;;;:~5 ;:~4 ;::;:::;::;;:::9:~6 99:;:~9 ;;;;<~12 ===<<<=:;9:9::;::;:9;;;999;<;<~8 ===;;;<<<<=<~5 ==<<=<~5 =<~9 =<;<;<<::;<;~5 :;<<<<:;;:9999;::9:;;:~9 ;::;;:~5 ;:~4 9:::;;<;;;::;~7 :;;;:;;:~6 <<<<;~4 <;;<;;;;<~4 ;;;:9;~4 :::99;<<;<<;<;:~5 ;::::>>;;?;:~7 <;~5 :;;;;:;~9 :~9 ;;;;:::;::;:::;~4 ::;;<<<:;~4 :;~15 <;;;;<<<<;<;<;~4 :;:~4 9:9999F<?:;=;<9999:::C=99::BECDB:;~5 <<<;;;::;~5 <<;:<;;;9:::;;;;<~4 ==;<>><<=<;;<~5 ==>====<<=<=<=~5 <<<::;:;;>??@C=~5 ;~4 :;:;;:;;;==<;<;<;~5 <<<<;;<;;<~4 ;;<<;;<~4 D~8 CDEDDEDDDC~4 DCCDDDDCDD67657776676669~13 4633300-..10/31.769955566715157599555342:0812.:;;-,12,;;;;<;;-6,;;9;<=>=>::>><=>;:<>><<<>~6 ==>.0-9~10 :9::9999::99:~5 ;;:::17/73:9~5 <<:<<99;:;;9::9<<<;;;::<~10 =<=>>=>=<==<==<;<;::;<;:<::;:<<9:98;99<98<9;<9<9:9~4 :99:9:9~4 :<<<<=<<====<=~8 <<<=~4 <<=<==<=<<<;;<;;;;<<<;:;:<<;;;<;<::<~4 ;==<<=9::98999:999::;9=~8 99::;:;998:999;<===;:<<;=;::<<===<=~4 <<<<::;:99:99:::99;;;:::98899;:;::9999:;<998~4 9999877988778799977:98979:;;;::::;;;;<:;;=<<;;<<<<=<<==<=<=<=~5 <=<>==;<;;<;<<999::::<:::;:;<<<9999:;:;;::<:<99<:<>?~4 >?=~7 >>>>?=~5 >?A~13 @AA@A@~6 AA@~5 A~5 @AAA@A~7 @AAA?>?>><=?===>><<<>>>====<=<==<~6 ===<;<<;<=>=><>>><<=<<;=<><<>;:;:;:;;;:;=~8 <<==<<=<<<=<==<<<<=<<<==<~12 =~12 <~13 =<<<<=<<====<=~4 <<<<=<<=<<<<==<<<=<=<~10 =<<==>=>===>=>>=<?<=?~4 =~4 <==<~9 =<=~8 ><=>><<==>>=>=>>>?>>==>?>>>====>>===>>=>>=~11 <<<=<<==<<====>>>=>==>~9 ===<===<==>=~10 >==>???>>@?>?>@A@~4 AA@@AAA??@?>>>A>~6 =<>=>>>>==>>=>A~10 BA@A@AABBACCABCC?~5 >~4 ?>>>?>>?>=~6 <===<===>===<<=?>~6 ?>=~5 >=>=~9 >=>~4 =>~4 =>>=~9 <<<<;;;<:77788788:;889889;:9789777=><;<>>=>==>===>><<<;<;<777;:7~6 12:;;85+..98889:87988872/*0..7~5 17~9 337789899978,.))(672655===34<<<9<22<4=2-==>===<6===>=>>>>=~5 ;<<;<<;<<;;<;;:;::;;;:<<=~4 <=99:9<9:;8~5 9999:8;:;;;<:::;<;:676~8 7~12 879888978777787778777789<<<;;<=<;?>?AAB>>?~4 BD>>>?>?==>?=>>==>=?==<<>;<;:;~4 :;~5 :~6 9:9:;;:~14 ;;;:::;;9<;<=<<;<;;;;<;;<;<;;<=<==<=;~7 <~4 =<;;;<;<<;;;<=9~7 88898:987:88988877788798899464767~4 676654778777755644445664556~7 44546~9 767~4 6~8 766676666877787888778787888878787~5 8~4 999899888998:~11 9989:::9:::9:9:9:~6 9:~5 ;;:;;:;::;;:;~6 :::<;<<<;:~9 ;;:;;;:~4 9778~4 :::88::9899:9::9::9:9777789899989766678877877588887547777556676645778~4 77778~9 98888989~5 7787777886566756678668545556666455676776889899998~5 798~5 99::::97~4 8889~4 ::88998888778779898999:;~6 ::;9::99;~7 ::;;;:;:::;~7 A@A~4 @~5 AAA@AA@A~6 @@AA@ABBA@A@@@A@~4 AA@A@@@>~18 BBBCCBBBCBBAAABBBADDDEDB@~4 BB@BAA@BBBB@@@AA@@CC@@@?~6 @~7 ??>~4 =>=>~8 @>~18 ????>>?>~20 ?>????>@~6 >>>?=>==@???>?>=~4 >>=~4 >>===>>==>=~17 @ABBBA@B~32 ?BA@??AB>B~4 >===>>?>?=?~4 >??>??B~9 @?@??@A~4 BA?A@B@???@@ABBBB????@@BBBA?~9 >?>?>~4 ????>==>=<=<=~4 >><~4 ===>~8 ==>>>?>?>>=>===>??>>===>==<=~6 <=?~8 >?AB?@@AA???A????@@?@?@@?@?@@A@@A@@@@????@BB@B???@@??@@?~11 @@?~4 @@?@???@?~11 >@=?~8 @@@@?~13 @@?@@>?~5 >?>?<<<<>===<<<>=>=<=>>>>==<=>~8 ??>??>?@@?~4 >???@????@??@@@@A@~5 ?~4 @????@??@???@????@?>?>?@??>>????>>?~7 >>>=>=>~16 ?>~9 ?>~9 =>>===>~7 =>~7 @?@?~4 >>>?==<<=<<<>==<<<???>>>===<=>>>=><<<<;==<;=<=>;?~6 >~5 ?>~12 =>=>>>>==>>>==>=~4 <~11 >~15 <==><==<~15 =<~19 >>>=<=>>>=~4 <=~5 >>>>=>>=>====<<<====<~14 =<<<==<~5 ===<=<=>~7 =<<==>>=>>==>====<===<~8 ;<<;~7 ?~12 <~14 =<==>>??<<<<>=><~4 =~4 <?>~4 =>>>===>><~9 ;=<;;;<<;;;<=<=<<;?~9 >=~5 >=>>><<<>=<<<==<<=>???>???>?~11 ===;<;<<=<~5 ;<<;;===<~5 ;;;<~7 ;<<<=;;;<<<;;<;<;<=;;;;<;<;;;TQRRSSRRSPS==<<==<<==KNONNOOOQOPSS=~5 >>>==???>?>????>>?>SPRRQRSQNNQR===<>=>=KKLKLKKLKLLMOLQMMKFJGPNQONOHEHKMOLFEEEIEI>=>==>><==<<<==<<<<==<<<=<<;<<<;LKLJLMLNPPNPKPPNOQNIJNKPKMMJMKJO??>~5 ????>>?~5 @???@?@@>=>===>>====POPPNOOOOPOONMLNMMMNM?M?~6 >~5 ?>?>==?=>>>=>===>>=>??>??>=~14 >===>=<<===<=<=<<<<=~6 <=<<<===<~5 =<<=~5 <=<==<<<<=<~5 ;<~4 ;<<==<=<;~4 ONMNOQOOOFGIHJ>=HJ>~9 =~17 >=>=>=>>===>=@??@AGDEHC?@?@>>>>DH>>>FA?NFNGILMMMMKJIIL?>IHLL?MO>KO?>===>=~5 <=~5 <==<===<<<<===<====<==<~9 =<<<<;<~6 >?>??@@??=>?>=@@@?<<<=<~5 ;<<;<<;;:::;<<;;<::<<:9<;<<===;;;<>;:<<;;;;::;::<==;<<=~6 ?~5 :~8 ;;:~16 ;~4 :===>;=~4 >>>??@?@@??@@A@A;=~4 <~6 =<>?<?<<<?=?<=<=<<<=>AB<<B=<<?~6 >BBBB<B@>>BB<<<>><~5 ;@AAAA<<<<@A>>A<<@>@>ABBBAA<~4 =;<=<?@@@<@~7 A~10 @@@A;~10 :;~5 :;=>==DFCF=>=;<<<<;==<<=<===>A;<>;<=A====<;::999;;::;;::;<;<;<=<?LNDQO??FO>PVI>PJG@K@===?>==IPQFUSVVTYJDVVU>~6 @>>?>??@>==AB???=?>>??@?<<;;::;@@><?::::<;:::@>??>?@?@@@@?@@A@ZXQ[QQOMMKJJQMNLMMLKJKHIIJILIMNJMMONOPPPPNKOPMMNLNMNOMMKKHLMNMPPOOMLKJONPONMNMNLNONNMNOPPPPOJMMPOOONGDFMN=<<===GHF>JFE>>=<<<=<<=OOOQONOPPO=<<=<=<<<<=<<<=~5 >=>>===>~6 ==>>>=>>>==>=>>==>>=>>>>==>=<~8 ===<<<<=<<<<;<<;<===<<>==<<<<=<=~5 <=<=<<<====<<;;;;=~5 <<<;=;<>=>=<<=<<;;;;<~10 ;;<~8 ;~5 <~6 ;<;<<<;::9;;;9:9:;;:9;;;<<;;9<<<;<~14 ;~6 :;;;;:::<<<;;;;<:;<;~8 <;<;~6 <~10 ;~7 :;~6 :;~4 <;~4 <~5 ;<<<==<=~5 <==<;;;<<;<~4 ==>>=;<???;~16 ECCFGGF@@@?FFEFG~6 BC@@?@DA>?>=MMMKLORTSRRRRQRSRSPONMPMPOLMLNQPOOQOTORSNNNOSQORQQQRQSPOONOPOPQNOMNMLMLOMKMKLKKK>~4 ?>>>?>>=~5 >=>=>=>=~7 :;:~6 ;;:::>~4 ===<===<;=<<==<====>>>=~23 >~4 <<=<==<===@>?>=@A=<=>??CACACCC=~4 >=>=<=~7 <=<~5 ;<;;<<<:;;<<<;<;=C??A===<D@BD<====>=~4 >>?=<=;<=>==;;<;:;OMQQNPPPQQONNNOOONMQPQOLJKKNM?@?>>>@?===>=???>=?==?~4 >>>>==>==>~5 ==>=<====><=?@=@<<>><<?;;;:;;;;::PNPOPPNOMKKLONRRQNRSRMLLMMLLMNNP???@@????@~4 ??PPQPNOPOMP@???>=>=?@?>>=><=~6 ><~7 ;~7 <<<<;;<<;;@?>??@>@=?@=?>@@@@<~8 ;<<;<;:;:9~5 :;;88:;:;<<<;;;;<;:;~4 :;:;;<<<;<;~15 <<;;;<<;<;;;;<;;;:~7 9:~9 99989:9~4 :::99::::8:::;:99:8<;;;<:;:::;;;;<;;;<<<;;::;;::;::;;;;:;~5 ::;;<;<<;~6 :;~5 <;;<<<;<<<<;;;;<<;~7 <;:;~7 :9;;;:;;;;<=<<<==<=~9 <<=<===<<<<==;<==<~6 ;<;;=<;;=~6 ;;<~18 =<=<<<=~5 ;====<=~4 <;;;<<<<==<;;<<=;;<;<;<~5 =~5 <=<~4 ==<=~6 <~11 ;<~16 ><<<=~4 <=~4 :::=@?<<::@?@>@?@=<:>::@::<;:@<;<;<<;:<=~5 <=<<=<<;<:;<<===<=~5 <;===<~6 ==<<<==<>?==>=<<<=<=~12 <<<;<<;;<<;;;;<~4 ==;;:::=<<<<=~11 <=~9 <=~5 >=~4 >??<=<@?=?=>?=~4 <<=;;=~7 <===;<;====;;;===<:;;;;<<=;==<;;;<<::;:?;;@=~6 >=:~4 <:;===<~5 =<<<<=<<;=<<?>??@@@B?>?==>>=<<><<><~6 :;:<==<~6 =<~7 ====>><<@>????>=<<<==<=<?>><>===><===<<<<><?:9::999;:;98989~7 898998999BBBAAAABABBBA~4 B~6 AAABA@B?@@ABBAABAA@@?~4 BBA?>??>===>~7 @??>>>?@@????>==>!~5 [![!~11 [!!!B~4 ABAB~5 AB~16 A~8 B~11 AAAAB~5 !_]_[^[^[X][Xhiihiihhhg~4 hhfeehfegdehmnfiX^Yab[]XWXXttr!ZXY[YZ[Z!Yxvvxzvyqsuvnhp![[[!![[]Z!_!!c_chkXWWZZX[jo!ZX^`_`^iij[^[e~4 aedfhfhfjlkjkikadf?>>?=>>>>??>?=>~7 ?~10 >?>=>====>>====?=~4 >~11 @~9 ?~10 ====>?>=>><<>>>=<<?>??>>===><<=<?>>?>===>>?~9 >?>??>>>??>>>>?>>?~4 @@???>=>==>>????>???>?>~7 ??>>>>?~9 >>?==?==>=>==>>===>>>=~18 <==<~4 ==?>~4 =>>>>=?>>>>=~14 >=>????>>>>?>=~5 >=~5 <<<<=<=~11 <=<<=<=?==>~6 ==>===>>=>~4 =>=~4 >>=>?>~6 ===>=>~7 =~4 <=~6 <<=~9 >>>=~4 >>=>>==>===>>>=~24 <=~24 >>==>==>=>==>~13 =>>><===<<====>>===<<=<<=~4 >~4 =~5 >??>=>>>=>>==<=<===<===<=>>>>===>=>====<~16 =<=<=~7 <<=<<<<=<=<=<~6 =<<<?~4 >~4 =>><<<<=~4 >><=djbioojc`rmru`^~6 aa_!!ecdffcuuwxwyxwxvvr]lr?=>>=>~4 ?>???==>~4 choio]^!Y`cb^ruvwwwxwyxXZ[][!WWYWXZYY[!`[bedhi!lnlmlllhhYWWY!!XXpgegglkZXabkeaeedffceYXa!]YY>~4 ?>?>?~4 =~7 >>>=>>>>=~6 >===>>>=~7 >>>=~4 >====<===>>>?>??>??>~7 ?>>><;<:89>>>=>=>::9:9;9<<=<=~4 <~6 ====<<====68576:8687787;;85:ED~4 @@BBB@AEECCABD>?A>@A@AA@~5 A@~8 AAA@~5 A@@A~6 @A:;;;:::;:;;:<~10 >===?~7 >=<<;<;;:~9 ;<<<<@???<<=>=<;;<;<===>===<>=~4 ?@?@>>>=~4 >=?@@????AA@===@<===?@@@?~5 @?~5 @~7 >???@??@?>>>??>>>>=>=@>>>>??>?<~11 ;<;<<>=~8 >=>=<====>=>====>>><<<>>=~8 <~4 =~6 >==?>@?===>=<=>@==@@@<<>=>><<>===?<=?=<=<=99:9~18 :9999:~24 ;:::;:::<<=<;;<;;;:;;<:9~4 ;::;:~8 ;;;::::;~7 99::=~4 <;<<=<<<=<<>==<<<;<=;<==:::;<;=<<===;<:<~5 ;<;<<<<==>???=>>=>>>====>====>~7 ====>>><=<>>?>>???>~4 ===>==>~7 99:999:::9<<:<<;<;:::<~12 ::;::;;;<;<~5 ::;;==<==<<<===<<==<>=~6 <~6 .10,54+.--+48731/0::;,0---*-/-+-//./0/0-0/0.32561-,,,,-165?=@@?9=BA,,,,<<=~5 >==@??CAACCAC===>>>D?=?>?@>=>)+*,++*'*'(&('&&#$#*+:~7 ;:~6 9~4 79<;::9;887777;~4 77867:9~4 888879~6 :::9999::9999:::9:~6 999:~12 ;~6 ====<<<=<*))&%-,++'(($$%#&%*##&#,-.---,,===;<<=<=<<<<;;:;<==<<<;=~5 ;;;:;;=<<=~5 >==<=~12 <====;<====<====<=>===>===;:<::::;;<;<<==;=~4 9:8=<<<:;<<;===<;<<=<~5 77778898698;;:;;::9:::9:;;<:<;~5 98999::::<;;::;;:;<;;<;<;<;;;;<;~5 =~13 <<=~5 <===<<<<;===<=~9 >==>=~10 8987889899887*+*777,,+---.-0,-.-,,-,((*'(&*-#(%(+(0178736546678777#1449~4 :;:;;:::30./2460/664567567665676464-455;~6 9999:::999:9~6 :~6 8:::7887788779997878::9~6 ::;;:;~5 ::;::;;9;;;;:;::;~4 ::8:9:98987878:;~5 8778<::=~4 <=<<8779;>B?B<8878896787>;=;9>=?;95Arpnnmpsqrrrnmmfghfdgfihlfkihkijfldea~4 bb`^_``a`a``_a`a`agggebbdbab9899998897~8 rrpstrtwwuuwuoonooppqporquuvvsurssvotswtklhoolggpmmmheeflkoqvwwmhhkfhirsieipddhe_`]]_]_]bc]!!]!``^_]]!]!]!!d]^]c`]^]^]]]!!]eqqgghdfahfa__gjhfaplmmii_]^ghlhfg_afdeh]^^]]!]!]]h]^]!~7 97978887::8;?><<.(,//1-'.CAA??@===::A7@7<;;;==<;<<;<~10 >>>><><=>=<=<==>==>~4 =>~6 =;==;=;=<>::;;<<;:;<;;;<;<;;::;;<<;;<;<<;<<<<::::9:;<:;;;====;;>=>~7 =<<<=;<==<<===<?=<=<<=;<<;=<;;<;;;;<=<;:::;;:;;:;<;:;:;;:~4 >=<<<>>>?>>@?~7 <=?>>=~6 <===A~5 >=>=?@<<<<?=>?><~4 >>>>?>>>>@AA?A@??>>=>~4 ===>===>>=~5 >~8 ?>?>>?>>?>??>?@?@??@??>=>>>===>>?>>>>?>?==>>==>AAA@@=~4 <===>=~9 >=~5 ;<;<;;<;<;<=<===;;;=>>>>?@@@==??>>@>=>=>=~6 >>A@@ABB?>?>?@A>===>>>=>?>;=<~4 ;<==;<;;<<;<;<<=<=<==>=>>>???>?A==>>===>~4 ==>~7 ?>??>?>~4 ??>>>>=>=>>>>?>?>>>>=>>=>==><=<>>>>=>====>==>=>>>==>?==>==>~10 <~4 ===<<;<;~13 ==<;<;<;<~8 =<:;;:::;;<;<<<<;<;~10 <<<<;<<<<:~8 ;;:;;;:;;<:;<88889988987888:~7 9:~6 88:;:;9;<<:9:;;:878::78::87~5 @??@A???AAAA?A;;;<=<====;;@@@A;@@@==>~4 @==<<=;?A>>@?@;<=;>=>=::;~8 <<;<<==;;<=:::;::9:::88::9::;:;::;988:~8 777899978:::9888787789~4 8988?@@A@AA=~7 @ABAAAA@AAA??@BB==>====>=~13 >?>@>??AAA>>==>?~4 BB?B====;:;;:::9:::;:=~12 <~8 ====<~10 ===<<<<>=~7 ;=<<<<=;==<<;==>===;=<<====<><==>><>>=>~4 ==<<>=>><==>=><<>~6 =>>>====>==>>===>>>>==>=><~10 ==<<===<<<====>===>>>=~6 >==>>??=>=?===>==>=><=<==<<==<<<<=<=<==<>>@?@@?@?@@@???>~5 =>=>~13 =>=>>>=>~5 CBB@@>@?@>>?>~11 =~4 ::;9:::===:<<<:;;>=~6 <=<<===;<=<=<====<<====<<==AAAA@caadc_ae]^^`outuqtwpv!]!!``![!ilcllc>~4 yxwxuyyywyywy>>a`XYZ[Y^_]_]a^c[[Ydbdca```acbb^`__Y[W!]fffed]!]!!XZfe][^[![Z[!!!![[[!aZcXXXZffeec`hcYXce`eednqmmfeYYkienoilkYWWYW!!bdqrmlZfj777a_^`_^]]]]^!]![!!!]_!]^``]]]^A~4 BBA]!]~4 ::99;;:89;<;<;<;9::]!!]]!!!!?D?@??:;;::;<9;9:;~5 :;;:>>=>=~7 >>>====>===<===???>>===A>>>>=>=~13 >~7 ==>=<=?><<<=<<<==>??<?==<<?@@=??<=<<==<<=~7 <~4 ;<<<==<<==<<=<<====<~12 =<~10 =<<;<~6 >=<==>===<~11 =<~11 =<~4 ==<=<~4 ===<~5 =<~5 =<=~7 <=<<=<~6 =~6 <===<~5 ;~5 <<;~5 <;;:::;;;::;:;::;:;::;~4 :;:~5 ;;<;<~4 =~8 <===<>=>==>=>><<;<===<=<==<=????>AA@AAA@A??>>??=>>?A:A:A:~8 <=;<;;;:;;A;::B:BBA::::???=<<>?>===<<<<==;<=<====>>>;;:<;;>==<=<;~5 ?@A???@@@ABA??=>=>@@B?>>>>==>?>==>==>>=><<>=>;;<=::;;:~4 ;~9 <<=<;~9 <;<<<;~4 <~4 ;<<;<;~12 ::;~9 <~8 ===::;;;<:<~4 ;<~4 ;;:;:::9;:~4 9::9998:9988;~10 9;;;:;;;;::;;;;:;;;:;;:9::;::8=~5 <~4 =<=<<<===<~5 ===<~6 ;<====<====<<<<=~9 >=<~4 ==<<=~5 <====;<<==<;<~5 ==<==<?@?>>===??==@~5 ?@?;=<~5 ==<<==;<<<==>=>=~4 >===>===>>>=>?=?>~4 ===>>>>====<~4 ===<<;<>>?=~5 ??==?=<<;;;;::899:889:~4 ;<;<;<;:;;;;RRSRSTSQQSSTSSPQQQRQPPRTRBCDBDBDDFCDBDBBD??@DCCC==-1<=CD1484:0<?A?C-BD;===:../0=<~14 =<>==>>=<====<=<==<~6 ==>><@>?@DD<?@B?DBD~4 EEGEGDDED?=DHGFDEEFEGHHHGGGFHHHEH<3<<<72>~4 =>>>>====>~4 ====>~5 ====>><=>~5 <<====1052==>=~7 >~6 @A@@@?@@?A??>A@AAA@>>>>?>>@~4 ??>@???@@@??@~5 ??@>~13 =>>===>~5 ?>>?>==>>>>?>~6 ?>>=~4 >=>=~8 ??>>>==???==>~9 ?>=~4 >>>??>><<=<<<>=>>=~4 <~8 ====>==<====><>>>>==>>=>=><=<~8 ?>?>>=>=>=>~16 ?~9 ===>>>>=>====<<=<>===<<<=<=????>~16 ?>>>D~4 CD>~9 D~8 B@A@A??CD?>?>>>?>>DDCD??A???>??>?~4 >>>>?>???>~11 =>~14 ?~12 >>=>?=?==>>==>>?>>>EDEDDD>~6 ===>>=>>>>====<<<<==<<<<==DCDFEDGFGDFGCEBAC?@@CB@??@??@?>>>???A??<=<===<<===<~5 =<~5 =<<>~7 A~10 B~11 AAB~9 AAA@AAA@?@@@?BA~5 BBBCCBB?>>@?>~10 ??><<==<<;:;::;<;;:;;;:;;:~5 ;=:;<~9 ====<=;<<<<=<<=<~7 ;~20 <~9 ;<~16 =<<<==<~11 ;<~19 ===<<=<<<<=<==<<=<=<<=<=<<==<<<=<~10 =~18 <=<=~4 <===<=<<<==<~11 =>>=;:;==>==>>=~6 >>?>>>???>~4 ?>?>?>=?~5 =>???>>??<<<=>><=<>=><~4 =<=~6 <~4 =<<=;;==>>>>=~4 >>????>?>=>~6 <<=~8 >==>>==?>@~4 A>??>@?>?@BB>>??AA@AB@@A@??>?@@===>=~4 >~4 =~6 <<<<=~6 BBCCBCCB~4 A~6 @ABAA@AAA@A@@@@>~5 ?>>????>????>?>>>??>=><=>?;;;<<=<=>>??;<==<<<<=<~14 ;<;<<<;;<<;;;;<;;<~4 ;~4 ?>><<=>=G~15 C~4 DECBDDBEEDDCDCCCBBBBCBBDCC@@@BB@@?>?>@??=>=?>?>@?@=?<?>?<<??<;<?>>?=:>=>>====>===87;A9;>>;<~6 >>==<===:~5 @:~7 @@A::::<=~25 >>;<=;:~4 ??>??=>=:<??=::=@=;=<;<<;;<~8 ::<=~6 <@?>==<>>>?@?>=~4 ;=;<<<;<;;;<<<??@~8 A@?@~19 =~5 <><==>=@>>><=>~11 =<=>>AAA@><<<<A@ABB;;<<;<?@=@@@<<???=??>=??>??>??@@=>~4 ?>BBB<=;;;;B;@>=>~4 ==>>=~12 ;=<====<:;<;<::<<==>~5 :~5 <;<;;;;<>>>I>><;<==>;;<;;<<=>==><<<:~4 ;99:99:<;<;<;~11 <;<<<;<;;:;;;<<<::<???<<==><<<==><@???@>=<~6 @@<<<<@>>==<>=>==<~25 >~10 <<=<<>=>>>=~5 <=>===<==>~5 ====<<<<=~4 <~5 =<=<===<=<=<=<;~9 =>>==><<=;;;=>;<<??=>=>?=><<<<;=887788878~4 7~4 88777999:99889988989899998999::99:~6 ;;::;::;<<<;~7 <;::::9998878898:9:87778787778<<<<;<;<;;:;;;:;;:;;;:~4 99::::8::99::999::899897899:788:9:9<~7 ;<<<;<:;<~6 ;;;:::9999;:;8:9<<==<===<<<=<<=~4 <==<<<<=<=<<<;;<<===<<<<===;<;=<;;:;;;;:;<::;89::8:::8:::8787:::79:9:99989:9:;:;;;:;;<;;;;<~7 ;<99;;:">
   *   </jvxlColorData>
   * </jvxlSurfaceData>
   * 
   * 
   **********************************************************/

  private static void appendXmlVertexOnlyData(SB sb, 
                                        JvxlData jvxlData, MeshData meshData, boolean escapeXml) {
    int[] vertexIdNew = new int[meshData.vc];
    if (appendXmlTriangleData(sb, meshData.pis,
        meshData.pc, meshData.bsSlabDisplay,
        vertexIdNew, escapeXml))
      appendXmlVertexData(sb, jvxlData, vertexIdNew,
          meshData.vs, meshData.vvs, meshData.vc,
          meshData.polygonColorData, meshData.pc, 
          meshData.bsSlabDisplay,
          jvxlData.vertexColors, 
          jvxlData.jvxlColorData.length() > 0, escapeXml);
  }

  /**
   * encode triangle data -- [ia ib ic]  [ia ib ic]  [ia ib ic] ...
   * algorithm written by Bob Hanson, 11/2008. The principle is that
   * not all vertices may be represented -- we only need the 
   * used vertices here. Capitalizing on the fact that triangle sets
   * tend to have common edges and similar numbers for sequential triangles.
   * 
   * a) Renumbering vertices as they appear in the triangle set
   * 
   *    [2456 2457 2458] [2456 2459 2458]
   *    
   *   becomes
   *    
   *    [   1    2    3] [   1    4    3]
   * 
   * b) This allows efficient encoding of differences, not absolute numbers.
   * 
   *        0    1    2     -2    3   -1
   *        
   * c) Which can then be represented often using a single ASCII character. 
   *    I chose \ to be 0, and replace that with !.
   *    
   *    ASCII:
   *    -30       -20       -10         0       +10       +20       +30      
   *    <=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrstuvwxyz{|
   *    
   *    So the above sequence would simply be:
   *    
   *      !]^Z_[
   *
   *    When the range falls outside of +/-32, we simply use a number.
   *    When a positive number follows another number, we add a "+" to it.
   *    
   *      !]^Z_[-33+250]230-210]]
   *      
   *    Preliminary trials indicated that on average a triangle
   *    can be encoded in about 7 bytes, or roughly half the 12 bytes
   *    necessary for standard binary encoding of integers. The advantage
   *    here is that we have an ASCII-readable file and no little-/big-endian issue.
   * 
   * @param sb
   * @param triangles
   * @param nData
   * @param bsSlabDisplay 
   * @param vertexIdNew
   * @param escapeXml 
   * @return (triangles are present)
   */
  private static boolean appendXmlTriangleData(SB sb, int[][] triangles, int nData,
                                              BS bsSlabDisplay, 
                                              int[] vertexIdNew, boolean escapeXml) {
    SB list1 = new SB();
    SB list2 = new SB();
    int ilast = 1;
    int p = 0;
    int inew = 0;
    boolean addPlus = false;
    int nTri = 0;
    
    // note that the slabbing present becomes irreversible if there is no ghosting.
    boolean removeSlabbed = (bsSlabDisplay != null);
    
    for (int i = 0; i < nData;) {
      if (triangles[i] == null || (removeSlabbed && !bsSlabDisplay.get(i))) {
        i++;
        continue;
      }
      int idata = triangles[i][p];
      if (vertexIdNew[idata] > 0) {
        idata = vertexIdNew[idata];
      } else {
        idata = vertexIdNew[idata] = ++inew;
      }
      int diff = idata - ilast;
      ilast = idata;
      if (diff == 0) {
        list1.appendC('!');
        addPlus = false;
      } else if (diff > 32) {
        if (addPlus)
          list1.appendC('+');
        list1.appendI(diff);
        addPlus = true;
      } else if (diff < -32) {
        list1.appendI(diff);
        addPlus = true;
      } else {
        list1.appendC((char) ('\\' + diff));
        addPlus = false;
      }
      if (++p % 3 == 0) {
        list2.appendI(triangles[i][3]);
        p = 0;
        i++;
        nTri++;
      }
    }
    if (list1.length() == 0)
      return true;
    XmlUtil.appendTagObj(sb, "jvxlTriangleData", new String[] {
        "count", "" + nTri,
        "encoding", "jvxltdiff",
        "data" , jvxlCompressString(list1.toString(), escapeXml), 
        }, null);
    XmlUtil.appendTagObj(sb, "jvxlTriangleEdgeData", new String[] { // Jmol 12.1.50
        "count", "" + nTri,
        "encoding", "jvxlsc",
        "data" , jvxlCompressString(list2.toString(), escapeXml) }, null);
    return true;
  }

  /**
   * encode the vertex data. This must be done AFTER encoding the triangles,
   * because the triangles redefine the order of vertices.
   * 
   * Bob Hanson 11/2008
   * 
   * If another program has created the triangles, we probably do not know the
   * grid that was used for Marching Cubes, or quite possibly no grid was used.
   * In that case, we just save the vertex/triangle/value data in a compact
   * form.
   * 
   * For the we use an extension of the way edge points are encoded. We simply
   * identify the minimum and maximum x, y, and z coordinates and then express
   * the point as a fraction along each of those directions. Thus, the x, y, and
   * z coordinate are within the interval [0,1].
   * 
   * We opt for the two-byte double-precision JVXL character compression. This
   * allows a 1 part in 8100 resolution, which is plenty for these purposes.
   * 
   * The tag will indicate the minimum and maximum values:
   * 
   * <jvxlVertexData count="150" min="(15.218472, -28.304049, 34.71112)"
   * max="(97.8228, 54.011948, 109.95208)" data="...."> </jvxlVertexData>
   * 
   * The resultant string is really two strings of length nData where the first
   * string lists the "high" part of the positions, and the second string lists
   * the "low" part of the positions.
   * 
   * @param sb
   * @param jvxlData
   * @param vertexIdNew
   * @param vertices
   * @param vertexValues
   * @param vertexCount
   * @param polygonColorData
   * @param polygonCount
   * @param bsSlabDisplay
   * @param vertexColors
   * @param addColorData
   * @param escapeXml
   */
  private static void appendXmlVertexData(SB sb, JvxlData jvxlData,
                                          int[] vertexIdNew, T3[] vertices,
                                          float[] vertexValues,
                                          int vertexCount,
                                          String polygonColorData,
                                          int polygonCount, BS bsSlabDisplay,
                                          int[] vertexColors,
                                          boolean addColorData,
                                          boolean escapeXml) {
    int colorFractionBase = jvxlData.colorFractionBase;
    int colorFractionRange = jvxlData.colorFractionRange;
    T3 p;
    P3 min = jvxlData.boundingBox[0];
    P3 max = jvxlData.boundingBox[1];
    SB list1 = new SB();
    SB list2 = new SB();
    int[] vertexIdOld = null;
    boolean removeSlabbed = (bsSlabDisplay != null);
    if (polygonCount > 0) {
      if (removeSlabbed)
        polygonCount = bsSlabDisplay.cardinality();
      removeSlabbed = false;
      vertexIdOld = new int[vertexCount];
      for (int i = 0; i < vertexCount; i++)
        if (vertexIdNew[i] > 0) // not all vertices may be in triangle -- that's OK
          vertexIdOld[vertexIdNew[i] - 1] = i;
    }
    int n = 0;
    for (int i = 0; i < vertexCount; i++)
      if (!removeSlabbed || bsSlabDisplay.get(i)) {
        n++;
        p = vertices[(polygonCount == 0 ? i : vertexIdOld[i])];
        jvxlAppendCharacter2(p.x, min.x, max.x, colorFractionBase,
            colorFractionRange, list1, list2);
        jvxlAppendCharacter2(p.y, min.y, max.y, colorFractionBase,
            colorFractionRange, list1, list2);
        jvxlAppendCharacter2(p.z, min.z, max.z, colorFractionBase,
            colorFractionRange, list1, list2);
      }
    list1.appendSB(list2);
    XmlUtil.appendTagObj(sb, "jvxlVertexData", new String[] { "count", "" + n,
        "min", Escape.eP(min), "max", Escape.eP(max), "encoding", "base90xyz2",
        "data", jvxlCompressString(list1.toString(), escapeXml), }, null);
    if (polygonColorData != null)
      XmlUtil.appendTagObj(sb, "jvxlPolygonColorData", new String[] {
          "encoding", "jvxlnc", "count", "" + polygonCount }, "\n"
          + polygonColorData);
    if (!addColorData)
      return;

    // now add the color data, again as a double-precision value.

    list1 = new SB();
    list2 = new SB();
    if (vertexColors == null) {
      for (int i = 0; i < vertexCount; i++)
        if (!removeSlabbed || bsSlabDisplay.get(i)) {
          float value = vertexValues[polygonCount == 0 ? i : vertexIdOld[i]];
          jvxlAppendCharacter2(value, jvxlData.mappedDataMin,
              jvxlData.mappedDataMax, colorFractionBase, colorFractionRange,
              list1, list2);
        }
    } else {
      int lastColor = 0;
      list1.appendI(n).append(" ");
      for (int i = 0; i < vertexCount; i++)
        if (!removeSlabbed || bsSlabDisplay.get(i)) {
          int c = vertexColors[polygonCount == 0 ? i : vertexIdOld[i]];
          if (c == lastColor)
            c = 0;
          else
            lastColor = c;
          list1.appendI(c);
          list1.append(" ");
        }
    }
    appendXmlColorData(sb, list1.appendSB(list2).append("\n")
        .toString(), (vertexColors == null), true, jvxlData.valueMappedToRed,
        jvxlData.valueMappedToBlue);
  }

  ////////// character - fraction encoding and decoding
  
  // NEVER change the numbers for these next defaults
  
  final public static int defaultEdgeFractionBase = 35; //#$%.......
  final public static int defaultEdgeFractionRange = 90;
  final public static int defaultColorFractionBase = 35;
  final public static int defaultColorFractionRange = 90;

  /* character-encoding of factions in base 90:
   * 
   * characters ASC(35) - ASC(124) are used for this encoding with the
   * exception of ASC(92)'\\', which is encoded as ASC(33)'!'.
   * ASC(125)'}' is reserved for "NaN".
   * Double-quote is not in this range, but '<' and '>' are, so this
   * is only XML-safe when quoted as an attribute. 
   * 
   */
  public static char jvxlFractionAsCharacter(float fraction) {
    return jvxlFractionAsCharacterRange(fraction, defaultEdgeFractionBase, defaultEdgeFractionRange);  
  }
  
  public static char jvxlFractionAsCharacterRange(float fraction, int base, int range) {
    if (fraction > 0.9999f)
      fraction = 0.9999f;
    else if (Float.isNaN(fraction))
      fraction = 1.0001f;
    int ich = (int) Math.floor(fraction * range + base);
    if (ich < base)
      return (char) base;
    if (ich == 92)
      return '!'; // \ --> !
    //if (logCompression)
    //Logger.info("fac: " + fraction + " --> " + ich + " " + (char) ich);
    return (char) ich;
  }

  private static void jvxlAppendCharacter2(float value, float min, float max,
                                           int base, int range,
                                           SB list1,
                                           SB list2) {
    float fraction = (min == max ? value : (value - min) / (max - min));
    char ch1 = jvxlFractionAsCharacterRange(fraction, base, range);
    list1.appendC(ch1);
    fraction -= jvxlFractionFromCharacter(ch1, base, range, 0);
    list2.appendC(jvxlFractionAsCharacterRange(fraction * range, base, range));
  }

  public static float jvxlFractionFromCharacter(int ich, int base, int range,
                                                float fracOffset) {
    if (ich == base + range)
      return Float.NaN;
    if (ich < base)
      ich = 92; // ! --> \
    float fraction = (ich - base + fracOffset) / range;
    if (fraction < 0f)
      return 0f;
    if (fraction > 1f)
      return 0.999999f;
    //System.out.println("ffc: " + fraction + " <-- " + ich + " " + (char)
    // ich);
    return fraction;
  }

  public static float jvxlFractionFromCharacter2(int ich1, int ich2, int base,
                                          int range) {
    float fraction = jvxlFractionFromCharacter(ich1, base, range, 0);
    float remains = jvxlFractionFromCharacter(ich2, base, range, 0.5f);
    return fraction + remains / range;
  }

  public static char jvxlValueAsCharacter(float value, float min, float max, int base,
                                   int range) {
    float fraction = (min == max ? value : (value - min) / (max - min));
    return jvxlFractionAsCharacterRange(fraction, base, range);
  }

  protected static float jvxlValueFromCharacter2(int ich, int ich2, float min,
                                                 float max, int base, int range) {
    float fraction = jvxlFractionFromCharacter2(ich, ich2, base, range);
    return (max == min ? fraction : min + fraction * (max - min));
  }

  // /// differential bitset encoding and decoding (Bob Hanson hansonr@stolaf.edu for Jmol)

  public static int jvxlEncodeBitSet0(BS bs, int nPoints, SB sb) {
    // nunset nset nunset ...
    // for repeated numbers:
    // 3 3 3 3 3 3 3 becomes 3 -6
    int dataCount = 0;
    int prevCount = -1;
    int nPrev = 0;
    if (nPoints < 0)
      nPoints = bs.length();
    int n = 0;
    boolean isset = false;
    int lastPoint = nPoints - 1;
    
    for (int i = 0; i < nPoints; ++i) {
      if (isset == bs.get(i)) {
        dataCount++;
      } else {
        if (dataCount == prevCount && i != lastPoint) {
          nPrev++;
        } else {
          if (nPrev > 0) {
            sb.appendC(' ').appendI(-nPrev);
            nPrev = 0;
            n++;
          }
          sb.appendC(' ').appendI(dataCount);
          n++;
          prevCount = dataCount;
        }
        dataCount = 1;
        isset = !isset;
      }
    }
    sb.appendC(' ').appendI(dataCount).appendC('\n');
    return n;
  }
  
  public static String jvxlEncodeBitSet(BS bs) {
    SB sb = new SB();
    jvxlEncodeBitSetBuffer(bs, -1, sb);
    return sb.toString();
  }
  
  public static int jvxlEncodeBitSetBuffer(BS bs, int nPoints, SB sb) {
    //System.out.println("jvxlcoder " + Escape.escape(bs));
    int dataCount = 0;
    int n = 0;
    boolean isset = false;
    if (nPoints < 0)
      nPoints = bs.length();
    if (nPoints == 0)
      return 0;
    sb.append("-");
    for (int i = 0; i < nPoints; ++i) {
      if (isset == bs.get(i)) {
        dataCount++;
      } else {
         jvxlAppendEncodedNumber(sb, dataCount, defaultEdgeFractionBase, defaultEdgeFractionRange);
        n++;
        dataCount = 1;
        isset = !isset;
      }
    }
    jvxlAppendEncodedNumber(sb, dataCount, defaultEdgeFractionBase, defaultEdgeFractionRange);
    sb.appendC('\n');
    return n;
  }

  public static void jvxlAppendEncodedNumber(SB sb, int n, int base, int range) {
    boolean isInRange = (n < range);
    if (n == 0)
      sb.appendC((char) base);
    else if (!isInRange)
      sb.appendC((char)(base + range));
    while (n > 0) {
      int n1 = n / range;
      int x = base + n - n1 * range;
      if (x == 92)
        x = 33;  // \ --> !
      sb.appendC((char) x);
      n = n1;
    }
    if (!isInRange)
      sb.append(" ");
  }

  public static BS jvxlDecodeBitSetRange(String data, int base, int range) {
    BS bs = new BS();
    int dataCount = 0;
    int ptr = 0;
    boolean isset = false;
    int[] next = new int[1];
    while ((dataCount = jvxlParseEncodedInt(data, base, range, next)) != Integer.MIN_VALUE) {
      if (isset)
        bs.setBits(ptr, ptr + dataCount);
      ptr += dataCount;
      isset = !isset;
    }
    return bs;
  }

  public static int jvxlParseEncodedInt(String str, int offset, int base, int[] next) {
    boolean digitSeen = false;
    int value = 0;
    int ich = next[0];
    int ichMax = str.length();
    if (ich < 0)
      return Integer.MIN_VALUE;
    while (ich < ichMax && PT.isWhitespace(str.charAt(ich)))
      ++ich;
    if (ich >= ichMax)
      return Integer.MIN_VALUE;
    int factor = 1;
    boolean isLong = (str.charAt(ich) == (offset + base));
    if (isLong)
      ich++;
    while (ich < ichMax && !PT.isWhitespace(str.charAt(ich))) {
      int i = str.charAt(ich);
      if (i < offset)
        i = 92;   // ! --> \ 
      value += (i - offset) * factor;
      digitSeen = true;
      ++ich;
      if (!isLong)
        break;
      factor *= base;
    }
    if (!digitSeen)
      value = Integer.MIN_VALUE;
    next[0] = ich;
    return value;
  }

  public static BS jvxlDecodeBitSet(String data) {
    if (data.startsWith("-"))
      return jvxlDecodeBitSetRange(jvxlDecompressString(data.substring(1)), defaultEdgeFractionBase, defaultEdgeFractionRange);
    // nunset nset nunset ...
    BS bs = new BS();
    int dataCount = 0;
    int lastCount = 0;
    int nPrev = 0;
    int ptr = 0;
    boolean isset = false;
    int[] next = new int[1];
    while (true) {
      dataCount = (nPrev++ < 0 ? dataCount : PT.parseIntNext(data, next));
      if (dataCount == Integer.MIN_VALUE) 
        break;
      if (dataCount < 0) {
        nPrev = dataCount;
        dataCount = lastCount;
        continue;
      }
      if (isset)
        bs.setBits(ptr, ptr + dataCount);
      ptr += dataCount;
      lastCount = dataCount;
      isset = !isset;
    }
    return bs;
  }
  
  /////// string data compression/decompression
  
  public static String jvxlCompressString(String data, boolean escapeXml) {
    
    
    /* just a simple compression, but allows 2000-6000:1 CUBE:JVXL for planes!
     * 
     *   "X~nnn " means "nnn copies of character X" 
     *   
     *   ########## becomes "#~10 " 
     *   
     *   ~ is not encoded, as it is ASC(126), outside the range of 33--125.
     *   
     *   for escaping XML, we also do:
     *
     *   < becomes "~;0 "
     *   & becomes "~%0 "
     *   
     *   and repeats of those become:
     *   
     *   "~;nnn "
     *   "~%nnn "
     *
     */
    if (data.indexOf("~") >= 0)
      return data;
    SB dataOut = new SB();
    char chLast = '\0';
    boolean escaped = false;
    boolean lastEscaped = false;
    int nLast = 0;
    int n = data.length();
    for (int i = 0; i <= n; i++) {
      char ch = (i == n ? '\0' : data.charAt(i));
      switch (ch) {
      case '\n':
      case '\r':
        continue;
      case '&':
      case '<':
        escaped = escapeXml;
        break;
      default:
        escaped = false;
      }
      if (ch == chLast) {
        ++nLast;
        ch = '\0';
      } else if (nLast > 0 || lastEscaped) {
        if (nLast < 4 && !lastEscaped || chLast == ' '
            || chLast == '\t') {
          while (--nLast >= 0)
            dataOut.appendC(chLast);
        } else {
          if (lastEscaped)
            lastEscaped = false;
          else
            dataOut.appendC('~');
          dataOut.appendI(nLast);
          dataOut.appendC(' ');
        }
        nLast = 0;
      }
      if (ch != '\0') {
        if (escaped) {
          lastEscaped = true;
          escaped = false;
          dataOut.appendC('~');
          chLast = ch;
          --ch;
        } else {
          chLast = ch;          
        }
        dataOut.appendC(ch);
      }
    }
    
    return dataOut.toString();
  }

  public static String jvxlDecompressString(String data) {
    if (data.indexOf("~") < 0)
      return data;
    SB dataOut = new SB();
    char chLast = '\0';
    int[] next = new int[1];
    for (int i = 0; i < data.length(); i++) {
      char ch = data.charAt(i);
      if (ch == '~') {
        next[0] = ++i;
        switch (ch = data.charAt(i)) {
        case ';':
        case '%':
          next[0]++;
          dataOut.appendC(chLast = ++ch);
          //$FALL-THROUGH$
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
          int nChar = PT.parseIntNext(data, next);
          for (int c = 0; c < nChar; c++)
            dataOut.appendC(chLast);
          i = next[0];
          continue;
        case '~':
          --i;
          break;
        default:
          Logger.error("Error uncompressing string " + data.substring(0, i) + "?");
        }
      }
      dataOut.appendC(ch);
      chLast = ch;
    }
    return dataOut.toString();
  }

  // VERSION 1 methods -- deprecated but still available through Jmol 11.9.18
  
  public static void jvxlCreateHeaderWithoutTitleOrAtoms(VolumeData v, SB bs) {
    jvxlCreateHeader(v, bs);
  }

  /**
   * Creates a two-line header for the XJVXL file. It is no longer necessary
   * to create the atom set or generate the vectors here. Please leave the 
   * commented code for posterity. 
   * 
   * @param v 
   * @param sb 
   */
  public static void jvxlCreateHeader(VolumeData v, SB sb) {
    // if the StringXBuilder comes in non-empty, it should have two lines
    // that do not start with # already present.
    v.setVolumetricXml();
    if (sb.length() == 0)
      sb.append("Line 1\nLine 2\n");
    /* no longer necessary
    sb.append(nAtoms == Integer.MIN_VALUE ? "+2" 
        : nAtoms == Integer.MAX_VALUE ? "-2" : "" + (-nAtoms))
      .append(' ')
      .append(v.volumetricOrigin.x).append(' ')
      .append(v.volumetricOrigin.y).append(' ')
      .append(v.volumetricOrigin.z).append(" ANGSTROMS\n");
    for (int i = 0; i < 3; i++)
      sb.append(v.voxelCounts[i]).append(' ')
        .append(v.volumetricVectors[i].x).append(' ')
        .append(v.volumetricVectors[i].y).append(' ')
        .append(v.volumetricVectors[i].z).append('\n');
    if (nAtoms != Integer.MAX_VALUE && nAtoms != Integer.MIN_VALUE) {
      nAtoms = Math.abs(nAtoms);
      for (int i = 0, n = 0; i < nAtoms; i++)
        sb.append((n = Math.abs(atomNo[i])) + " " + n + ".0 "
            + atomXyz[i].x + " " + atomXyz[i].y + " " + atomXyz[i].z + "\n");
      return;
    }
    Point3f pt = Point3f.new3(v.volumetricOrigin);
    sb.append("1 1.0 ").append(pt.x).append(' ').append(pt.y).append(' ')
        .append(pt.z).append(" //BOGUS H ATOM ADDED FOR JVXL FORMAT\n");
    for (int i = 0; i < 3; i++)
      pt.scaleAdd(v.voxelCounts[i] - 1, v.volumetricVectors[i], pt);
    sb.append("2 2.0 ").append(pt.x).append(' ').append(pt.y).append(' ')
        .append(pt.z).append(" //BOGUS He ATOM ADDED FOR JVXL FORMAT\n");
    */
  }

  /*
  private static String jvxlGetFileVersion1(JvxlData jvxlData,
                                            MeshData meshData, String[] title,
                                            String msg, boolean includeHeader,
                                            int nSurfaces, String state,
                                            String comment) {
    // pre-XML
    if ("TRAILERONLY".equals(msg))
      return "";
    StringXBuilder data = new StringXBuilder();
    if (includeHeader) {
      String s = jvxlData.jvxlFileHeader
          + (nSurfaces > 0 ? -nSurfaces : -1) +" " + jvxlData.edgeFractionBase + " "
          + jvxlData.edgeFractionRange + " " + jvxlData.colorFractionBase + " "
          + jvxlData.colorFractionRange + " Jmol voxel format version " +  JVXL_VERSION1 + "\n";
      if (s.indexOf("#JVXL") != 0)
        data.append("#JVXL").append(jvxlData.isXLowToHigh ? "+" : "").append(
            " VERSION ").append(JVXL_VERSION1).append("\n");
      data.append(s);
    }
    if ("HEADERONLY".equals(msg))
      return data.toString();
    data.append("# ").append(msg).append('\n');
    if (title != null)
      for (int i = 0; i < title.length; i++)
        data.append("# ").append(title[i]).append('\n');
    state = (state == null ? "" : " rendering:" + state);
    String definitionLine = jvxlGetDefinitionLineVersion1(jvxlData);
    data.append(definitionLine).append(state).append('\n');
    StringXBuilder sb = new StringXBuilder();
    String colorData = (jvxlData.jvxlColorData == null ? "" : jvxlData.jvxlColorData);
    // if (jvxlData.vertexDataOnly) {  // see XML version
    //  sb.append("<jvxlSurfaceData>\n");
    //  jvxlAppendMeshXml(sb, jvxlData, meshData, false);
    //  sb.append("</jvxlSurfaceData>\n");
    //} else  
    if (jvxlData.jvxlPlane == null) {
      if (jvxlData.jvxlEdgeData == null)
        return "";
      //no real point in compressing this unless it's a sign-based coloring
      sb.append(jvxlData.jvxlSurfaceData);
      sb.append(jvxlCompressString(jvxlData.jvxlEdgeData, false)).append('\n').append(
          jvxlCompressString(colorData, false)).append('\n');
    } else if (colorData != null) {
      sb.append(jvxlCompressString(colorData, false)).append('\n');
    }
    int len = sb.length();
    data.append(sb);
    if (includeHeader) {
      if (msg != null && !jvxlData.vertexDataOnly)
        data.append("#-------end of jvxl file data-------\n");
      data.append(jvxlGetInfo(jvxlData, false)).append('\n');
        jvxlAppendCommandState(data, comment, state, false);
      if (includeHeader)
        XmlUtil.appendTag(data, "jvxlFileTitle", null, null, jvxlData.jvxlFileTitle, false, true);
    }
    return jvxlSetCompressionRatio(data, jvxlData, len);
  }

  private static String jvxlGetDefinitionLineVersion1(JvxlData jvxlData) {
    String definitionLine = 
    //(jvxlData.vContours == null ? ""  : "#+contourlines\n")+
       jvxlData.cutoff + " ";

    //  optional comment line for compatibility with earlier Jmol versions:
    //  #+contourlines (no longer used -- see XML version)
    //  cutoff       nInts     (+/-)bytesEdgeData (+/-)bytesColorData
    //               param1              param2         param3    
    //                 |                   |              |
    //   when          |                   |        >  0 ==> jvxlDataIsColorMapped
    //   when          |                   |       == -1 ==> not color mapped
    //   when          |                   |        < -1 ==> jvxlDataIsPrecisionColor    
    //   when        == -1     &&   == -1 ==> noncontoured plane
    //   when        == -1     &&   == -2 ==> contourable plane
    //   when        < -1*     &&    >  0 ==> contourable functionXY
    //   when        > 0       &&    <  0 ==> jvxlDataisBicolorMap

    //  nInts saved as -1 - nInts

    if (jvxlData.jvxlSurfaceData == null)
      return "";
    int nSurfaceInts = jvxlData.nSurfaceInts;// jvxlData.jvxlSurfaceData.length();
    int bytesUncompressedEdgeData = (jvxlData.vertexDataOnly ? 0
        : jvxlData.jvxlEdgeData.length() - 1);
    int nColorData = (jvxlData.jvxlColorData == null ? -1 : (jvxlData.jvxlColorData.length() - 1));
    if (jvxlData.jvxlPlane == null) {
      if (jvxlData.isContoured) {
        definitionLine += (-1 - nSurfaceInts) + " " + bytesUncompressedEdgeData;
      } else if (jvxlData.isBicolorMap) {
        definitionLine += (nSurfaceInts) + " " + (-bytesUncompressedEdgeData);
      } else {
        definitionLine += nSurfaceInts + " " + bytesUncompressedEdgeData;
      }
      definitionLine += " "
          + (jvxlData.isJvxlPrecisionColor && nColorData != -1 ? -nColorData
              : nColorData);
    } else {
      String s = " " + jvxlData.jvxlPlane.x + " " + jvxlData.jvxlPlane.y + " "
          + jvxlData.jvxlPlane.z + " " + jvxlData.jvxlPlane.w;
      definitionLine += (jvxlData.isContoured ? "-1 -2 " + (-nColorData)
          : "-1 -1 " + nColorData)
          + s;
    }
    if (jvxlData.isContoured) {
      if (jvxlData.contourValues == null || jvxlData.contourColixes == null) {
        definitionLine += " " + jvxlData.nContours;
      } else {
        definitionLine += " " + Escape.escapeArray(jvxlData.contourValues)
            + " \"" + jvxlData.contourColors + "\"";
      }
    }
    // ... mappedDataMin mappedDataMax valueMappedToRed valueMappedToBlue ...
    float min = (jvxlData.mappedDataMin == Float.MAX_VALUE ? 0f
        : jvxlData.mappedDataMin);
    definitionLine += " " + min + " " + jvxlData.mappedDataMax + " "
        + jvxlData.valueMappedToRed + " " + jvxlData.valueMappedToBlue;
    if (jvxlData.insideOut) {
      definitionLine += " insideOut";
    }
    return definitionLine;
  }

  */

}
