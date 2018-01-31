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

import org.jmol.util.C;
import org.jmol.util.Escape;
import org.jmol.util.Logger;

import javajs.util.PT;
import javajs.util.SB;
import javajs.util.P4;
import org.jmol.jvxl.data.JvxlCoder;

public class JvxlReader extends JvxlXmlReader {

  // 1.4 adds -nContours to indicate contourFromZero for MEP data mapped onto planes
  // 2.0 adds vertex/triangle compression when no grid is present 
  // Jmol 11.7.25 -- recoded so that we do not create voxelData[nx][ny][nz] and instead
  //                 simply create a BitSet of length nx * ny * nz. This saves memory hugely.
  // 2.1 adds JvxlXmlReader
  
  JvxlReader(){}
  
  @Override
  void init2(SurfaceGenerator sg, BufferedReader br) {
    init2JXR(sg, br);
    isXmlFile = false;
    JVXL_VERSION = "2.0";
  }

  // // methods used for reading any file format, but creating a JVXL file

  // ///////////reading the format///////////

  @Override
  protected void readParameters() throws Exception {
    jvxlFileHeaderBuffer = new SB().append(skipComments(false));
    if (line == null || line.length() == 0)
      line = "Line 1";
    jvxlFileHeaderBuffer.append(line).appendC('\n');
    if (rd() == null || line.length() == 0)
      line = "Line 2";
    jvxlFileHeaderBuffer.append(line).appendC('\n');
    jvxlFileHeaderBuffer.append(skipComments(false));
    String atomLine = line;
    String[] tokens = PT.getTokensAt(atomLine, 0);
    isXLowToHigh = false;
    negativeAtomCount = true;
    ac = 0;
    if (tokens[0] == "-0") {
    } else if (tokens[0].charAt(0) == '+') {
      isXLowToHigh = true;
      ac = parseIntStr(tokens[0].substring(1));
    } else {
      ac = -parseIntStr(tokens[0]);
    }
    if (ac == Integer.MIN_VALUE)
      return;
    volumetricOrigin.set(parseFloatStr(tokens[1]), parseFloatStr(tokens[2]),
        parseFloatStr(tokens[3]));
    isAngstroms = VolumeFileReader.checkAtomLine(isXLowToHigh, isAngstroms,
        null, atomLine, jvxlFileHeaderBuffer);
    if (!isAngstroms)
      volumetricOrigin.scale(ANGSTROMS_PER_BOHR);
    readVoxelVector(0);
    readVoxelVector(1);
    readVoxelVector(2);
    for (int i = 0; i < ac; ++i)
      jvxlFileHeaderBuffer.append(rd() + "\n");    
    skipComments(true);
    Logger.info("Reading extra JVXL information line: " + line);
    nSurfaces = parseIntStr(line);
    if (!(isJvxl = (nSurfaces < 0)))
      return;
    nSurfaces = -nSurfaces;
    Logger.info("jvxl file surfaces: " + nSurfaces);
    int ich;
    if ((ich = parseInt()) == Integer.MIN_VALUE) {
      Logger.info("using default edge fraction base and range");
    } else {
      edgeFractionBase = ich;
      edgeFractionRange = parseInt();
    }
    if ((ich = parseInt()) == Integer.MIN_VALUE) {
      Logger.info("using default color fraction base and range");
    } else {
      colorFractionBase = ich;
      colorFractionRange = parseInt();
    }
    cJvxlEdgeNaN = (char)(edgeFractionBase + edgeFractionRange);
    vertexDataOnly = jvxlData.vertexDataOnly = (volumetricVectors[0].length() == 0);
  }

  @Override
  protected String jvxlReadFractionData(String type, int nPoints) {
    String str = "";
    try {
      while (str.length() < nPoints) {
        rd();
        str += JvxlCoder.jvxlDecompressString(line);
      }
    } catch (Exception e) {
      Logger.error("Error reading " + type + " data " + e);
      throw new NullPointerException();
    }
    return str;
  }

  @Override
  protected void gotoData(int n, int nPoints) throws Exception {

    //called by VolumeFileReader.readVoxelData

    if (n > 0)
      Logger.info("skipping " + n + " data sets, " + nPoints + " points each");
    vertexDataOnly = jvxlData.vertexDataOnly = (nPoints == 0);
    for (int i = 0; i < n; i++) {
      jvxlReadDefinitionLine(true);
      Logger.info("JVXL skipping: jvxlSurfaceDataCount=" + surfaceDataCount
          + " jvxlEdgeDataCount=" + edgeDataCount
          + " jvxlDataIsColorMapped=" + jvxlDataIsColorMapped);
      jvxlSkipData(nPoints, true);
    }
    jvxlReadDefinitionLine(true);
  }

  private void jvxlReadDefinitionLine(boolean showMsg) throws Exception {
    // params values come from user adding options to the isosurface command
    // jvxlData values are from this file
    String comment = skipComments(true);
    if (showMsg)
      Logger.info("reading jvxl data set: " + comment + line);
    haveContourData = (comment.indexOf("+contourlines") >= 0);
    jvxlCutoff = parseFloatStr(line);
    Logger.info("JVXL read: cutoff " + jvxlCutoff);

    //  optional comment line for compatibility with earlier Jmol versions:
    //  #+contourlines
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

    // * nInts saved as -1 - nInts
    
    // it's possible that a plane will not be contoured (-1 -1) when it is a solid color.
    // why you would want to save this as JVXL is another question.
    // instead, we just set "contour 1" to indicate just one contour to demo that.
    // In addition, now we consider contouring functionXY, so in that case we would
    // have surface data, edge data, and color data

    int param1 = parseInt();
    int param2 = parseInt();
    int param3 = parseInt();
    if (param3 == Integer.MIN_VALUE || param3 == -1)
      param3 = 0;

    if (param1 == -1) {
      // a plane is defined
      try {
        params.thePlane = P4.new4(parseFloat(), parseFloat(), parseFloat(),
            parseFloat());
      } catch (Exception e) {
        Logger
            .error("Error reading 4 floats for PLANE definition -- setting to 0 0 1 0  (z=0)");
        params.thePlane = P4.new4(0, 0, 1, 0);
      }
      Logger.info("JVXL read: plane " + params.thePlane);
      if (param2 == -1 && param3 < 0)
        param3 = -param3;
      //error in some versions of Jmol. (fixed in 11.3.54)
    } else {
      params.thePlane = null;
    }
    if (param1 < 0 && param2 != -1) {
      // contours are defined (possibly overridden -- this is just a display option
      // could be plane or functionXY
      params.isContoured = (param3 != 0);
      int nContoursRead = parseInt();
      if (nContoursRead == Integer.MIN_VALUE) {
        if (line.charAt(next[0]) == '[') {
           jvxlData.contourValues = params.contoursDiscrete = parseFloatArray(null, null, null);
           Logger.info("JVXL read: contourValues " + Escape.eAF(jvxlData.contourValues));            
           jvxlData.contourColixes = params.contourColixes = C.getColixArray(getQuotedStringNext());
           jvxlData.contourColors = C.getHexCodes(jvxlData.contourColixes);
           Logger.info("JVXL read: contourColixes " + jvxlData.contourColors); 
           params.nContours = jvxlData.contourValues.length;
                 }
      } else {
        if (nContoursRead < 0) {
          nContoursRead = -1 - nContoursRead;
          params.contourFromZero = false; //MEP data to complete the plane
        }
        if (nContoursRead != 0 && params.nContours == 0) {
          params.nContours = nContoursRead;
          Logger.info("JVXL read: contours " + params.nContours);
        }
      }
    } else {
      params.isContoured = false;
    }

    jvxlData.isJvxlPrecisionColor = (param1 == -1 && param2 == -2 
        || param3 < 0);
    params.isBicolorMap = (param1 > 0 && param2 < 0);
    jvxlDataIsColorMapped = (param3 != 0);
    if (jvxlDataIsColorMapped)
      jvxlData.colorScheme = "RGB"; // legacy; but can't remap contours
    jvxlDataIs2dContour = (jvxlDataIsColorMapped && params.isContoured);

    if (params.isBicolorMap || params.colorBySign)
      jvxlCutoff = 0;
    surfaceDataCount = (param1 < -1 ? -1 - param1 : param1 > 0 ? param1 : 0);
    //prior to JVXL 1.1 (4/2007), this number counts the bytes of integer data.
    //after that, the number of integers, for the progressive reader
    
    if (param1 == -1)
      edgeDataCount = 0; //plane
    else
      edgeDataCount = (param2 < -1 ? -param2 : param2 > 0 ? param2 : 0);
    colorDataCount = (params.isBicolorMap ? -param2 : param3 < -1 ? -param3
        : param3 > 0 ? param3 : 0);
    if (params.colorBySign)
      params.isBicolorMap = true;
    float dataMin = Float.NaN;
    float dataMax = Float.NaN;
    float red = Float.NaN;
    float blue = Float.NaN;
    boolean insideOut = (line.indexOf("insideOut") >= 0);
    if (jvxlDataIsColorMapped) {
      dataMin = parseFloat();
      dataMax = parseFloat();
      red = parseFloat();
      blue = parseFloat();
    }
    jvxlSetColorRanges(dataMin, dataMax, red, blue, insideOut);
  }

  @Override
  protected void readSurfaceData(boolean isMapDataIgnored) throws Exception {
    thisInside = !params.isContoured;
    if (!readSurfaceDataXML())
      readSurfaceDataJXR();
  }

  @Override
  protected void jvxlSkipData(int nPoints, boolean doSkipColorData)
      throws Exception {
    // surfaceDataCount is quantitatively unreliable in pre-4/2007 versions (Jvxl 1.0)
    // so we just add them all up -- they must sum to nX * nY * nZ points 
    if (surfaceDataCount > 0)
      jvxlSkipDataBlock(nPoints, true);
    if (edgeDataCount > 0)
      jvxlSkipDataBlock(edgeDataCount, false);
    if (jvxlDataIsColorMapped && doSkipColorData)
      jvxlSkipDataBlock(colorDataCount, false);
  }

  private void jvxlSkipDataBlock(int nPoints, boolean isInt) throws Exception {
    int n = 0;
    while (n < nPoints) {
      rd();
      n += (isInt ? countData(line) : JvxlCoder.jvxlDecompressString(line).length());
    }
  }

  private int countData(String str) {
    int count = 0;
    int n = parseIntStr(str);
    while (n != Integer.MIN_VALUE) {
      count += n;
      n = parseIntNext(str);
    }
    return count;
  }
}
