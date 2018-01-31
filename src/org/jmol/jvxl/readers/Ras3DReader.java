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
import java.util.Map;

import javajs.util.CU;
import javajs.util.P3;

import org.jmol.jvxl.data.JvxlCoder;
import org.jmol.util.Logger;

/*
 * Raster3D file reader -- see http://skuld.bmsc.washington.edu/raster3d/html/render.html
 * 
 * just triangles; implemented specifically for DSSR cartoon-block representation
 * 
 * experimental only
 * 
 */


class Ras3DReader extends PolygonFileReader {

  private String pmeshError;
  private String type;
  private boolean asQuads; 
  private int nPolygons;


  Ras3DReader(){}
  
  @Override
  void init2(SurfaceGenerator sg, BufferedReader br) {
    init2PR(sg, br);
  }
  
  protected void init2PR(SurfaceGenerator sg, BufferedReader br) {
    init2PFR(sg, br);
    String fileName = (String) ((Object[])sg.getReaderData())[0];
    if (fileName == null)
      return;
    type = "ras3d";
    setHeader();
  }

  protected void setHeader() {
    jvxlFileHeaderBuffer.append(type
        + " file format\nvertices and triangles only\n");
    JvxlCoder.jvxlCreateHeaderWithoutTitleOrAtoms(volumeData,
        jvxlFileHeaderBuffer);
  }

  @Override
  void getSurfaceData() throws Exception {
    if (readVerticesAndPolygons())
      Logger.info(type  + " file contains "
          + nVertices + " vertices and " + nPolygons + " polygons for "
          + nTriangles + " triangles");
    else
      Logger.error(params.fileName + ": " 
          + (pmeshError == null ? "Error reading pmesh data "
              : pmeshError));
  }

  protected boolean readVerticesAndPolygons() {
    try {
      if (readVertices())
        return true;
    } catch (Exception e) {
      if (pmeshError == null)
        pmeshError = type  + " ERROR: " + e;
    }
    return false;
  }

  Map<String, Integer> htVertices;

  private boolean readVertices() throws Exception {
    htVertices = new Hashtable<String, Integer>();
    int[] v0 = new int[3];
    int[] v1 = new int[3];
    int[] v2;
    int c0 = 0, c1 = 0, c2;
    if (rd().indexOf("DSSR") >= 0)
      asQuads = true;
    while (rd() != null) {
      while (!line.equals("1")) {
        rd();
      }
      // triangles only
      rd();
      String[] tokens = getTokens();
      v0[0] = getPoint(tokens, 0);
      v0[1] = getPoint(tokens, 3);
      v0[2] = getPoint(tokens, 6);
      nTriangles++;
      c0 = CU.colorTriadToFFRGB(parseFloatStr(tokens[9]),
          parseFloatStr(tokens[10]), parseFloatStr(tokens[11]));
      if (asQuads) {
        if (nTriangles % 2 == 1) {
          v2 = v1;
          v1 = v0;
          v0 = v2;
          c2 = c1;
          c1 = c0;
          c0 = c2;
          continue;
        }
        addTriangleCheck(v0[0], v0[1], v0[2], 6, 0, false, c0);
        addTriangleCheck(v1[0], v1[1], v1[2], 3, 0, false, c1);
      } else {        
        addTriangleCheck(v0[0], v0[1], v0[2], 7, 0, false, c0);
      }
      nPolygons++;
    }
    return true;
  }

  private int getPoint(String[] tokens, int i) {
    String key = tokens[i] + ";" + tokens[i+1] + ";" + tokens[i+2];
    Integer v = htVertices.get(key);
    if (v == null) {
      addVertexCopy(P3.new3(parseFloatStr(tokens[i]),parseFloatStr(tokens[i+1]),parseFloatStr(tokens[i+2])), 0, nVertices, false);
      htVertices.put(key,  v = Integer.valueOf(nVertices++));
    }
    return v.intValue();
  }


}
