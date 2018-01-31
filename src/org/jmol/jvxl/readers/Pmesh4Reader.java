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


import org.jmol.jvxl.data.JvxlCoder;
import org.jmol.util.Logger;

import javajs.util.CU;
import javajs.util.P4;

/*
 * 
 * four-dimensional mesh -- must be rotated with a 3D mouse!
 * 
4MESHC
0.200000 0.200000 0.600000 1.000000
7 4
1.189475 0.000000 0.862984 0.626994
1.178874 -0.095131 0.948624 0.509644
1.147442 -0.191896 1.028624 0.397614
1.096463 -0.292096 1.096463 0.292096
1.028624 -0.397614 1.147442 0.191896
0.948624 -0.509644 1.178874 0.095131
0.862984 -0.626994 1.189475 0.000000
1.086534 0.000000 0.708278 0.514594
1.074680 -0.066675 0.805534 0.395033
1.039204 -0.135681 0.900732 0.294532
0.980592 -0.210098 0.980592 0.210098
0.900732 -0.294532 1.039204 0.135681
0.805534 -0.395033 1.074680 0.066675
0.708278 -0.514594 1.086534 0.000000
1.022062 0.000000 0.525187 0.381571
1.008882 -0.034694 0.668219 0.244227
0.968859 -0.071256 0.800746 0.165024
0.900420 -0.112621 0.900420 0.112621
0.800746 -0.165024 0.968859 0.071256
0.668219 -0.244227 1.008882 0.034694
0.525187 -0.381571 1.022062 0.000000
1.000000 0.000000 0.000000 0.000000
0.986228 0.000000 0.582369 0.000000
0.944088 0.000000 0.757858 0.000000
0.870551 0.000000 0.870551 0.000000
0.757858 0.000000 0.944088 0.000000
0.582369 0.000000 0.986228 0.000000
0.000001 0.000000 1.000000 0.000000

 *
 */


class Pmesh4Reader extends PolygonFileReader {

  private int nPolygons;
  private String pmeshError;
  private String type;
  private int color;
  float transparency;
  private int nX;
  private int nY;


  Pmesh4Reader(){}
  
  @Override
  void init2(SurfaceGenerator sg, BufferedReader br) {
    init2PFR(sg, br);
    String fileName = (String) ((Object[])sg.getReaderData())[0];
    if (fileName == null)
      return;
    params.fullyLit = true;
    type = "pmesh4";
    jvxlFileHeaderBuffer.append(type
        + " file format\nvertices and triangles only\n");
    JvxlCoder.jvxlCreateHeaderWithoutTitleOrAtoms(volumeData,
        jvxlFileHeaderBuffer);
  }

  @Override
  void getSurfaceData() throws Exception {
    rd();
    if (readVerticesAndPolygons())
      Logger.info(type  + " file contains "
          + nVertices + " 4D vertices and " + nPolygons + " polygons for "
          + nTriangles + " triangles");
    else
      Logger.error(params.fileName + ": " 
          + (pmeshError == null ? "Error reading pmesh4 data "
              : pmeshError));
  }

  private boolean readVerticesAndPolygons() {
    try {
      readColor();
      nY = getInt();
      nX = getInt();
      readVertices();
      createMesh();

      return true;
    } catch (Exception e) {
      if (pmeshError == null)
        pmeshError = type  + " ERROR: " + e;
    }
    return false;
  }

  private void readColor() throws Exception {
    color = CU.colorTriadToFFRGB(getFloat(), getFloat(), getFloat());
    transparency = getFloat();
  }

  private boolean readVertices() throws Exception {
    nVertices = nX * nY;
    iToken = Integer.MAX_VALUE;
    pmeshError = type + " ERROR: invalid vertex list";
    for (int i = 0; i < nVertices; i++) {
      P4 pt = P4.new4(getFloat(), getFloat(), getFloat(), getFloat());
      //if (isAnisotropic)
        //setVertexAnisotropy(pt);
      if (Logger.debugging)
        Logger.debug(i + ": " + pt);
      addVertexCopy(pt, 0, i, false);
      iToken = Integer.MAX_VALUE;
    }
    pmeshError = null;
    return true;
  }

  private void createMesh() {
    for (int i = 0; i < nX - 1; i++) {
      for (int j = 0; j < nY - 1; j++) {
        nTriangles += 2;
        addTriangleCheck(i * nY + j, (i + 1) * nY + j, (i + 1) * nY + j + 1, 3, 0,
            false, color);
        addTriangleCheck((i + 1) * nY + j + 1, i * nY + j + 1, i * nY + j, 3,
            0, false, color);
      }
    }
  }

  private String[] tokens = new String[0];
  private int iToken = 0;

  private String nextToken() throws Exception {
    while (iToken >= tokens.length) { 
      iToken = 0;
      rd();
      tokens = getTokens();
    }
    return tokens[iToken++];
  }

  private int getInt() throws Exception {
    return parseIntStr(nextToken());
  }

  private float getFloat() throws Exception {
    return parseFloatStr(nextToken());
  }

}
