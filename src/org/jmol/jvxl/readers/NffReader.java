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


import org.jmol.jvxl.data.JvxlCoder;
import org.jmol.util.Logger;

import javajs.util.CU;
import javajs.util.P3;

/* simple Neutral File Format reader 
 * 
 * see: http://paulbourke.net/dataformats/nff/nff1.html
 * 
 * for Eric Cole and tomography results from U. Colorado Boulder
 * electron microscopy of cells.
 *  
 * IMOD program write of objects http://bio3d.colorado.edu/imod/
 *
 * The scale is probably huge -- you probably want to use 
 * 
 * isosurface... SCALE 0.1 ....
 * 
 */


class NffReader extends PolygonFileReader {

  protected int nPolygons;
  
  NffReader(){}
  
  @Override
  void init2(SurfaceGenerator sg, BufferedReader br) {
    init2PFR(sg, br);
  }

  protected void setHeader() {
    jvxlFileHeaderBuffer.append("NFF file format\nvertices and triangles only\n");
    JvxlCoder.jvxlCreateHeaderWithoutTitleOrAtoms(volumeData,
        jvxlFileHeaderBuffer);
  }

  @Override
  void getSurfaceData() throws Exception {
    if (readVerticesAndPolygons())
      Logger.info("NFF file contains " + nVertices + " vertices and "
          + nTriangles + " triangles");
    else
      Logger.error(params.fileName + ": Error reading Nff data ");
  }

  protected boolean readVerticesAndPolygons() {
    int color = 0xFF0000;
    try {
      while(rd() != null) {
        if (line.length() == 0)
          continue;
        String[] tokens = getTokens();
        switch (line.charAt(0)) {
        case '#':
          vertexMap.clear();
          continue;
        case 'f':
          color = CU.colorTriadToFFRGB(parseFloatStr(tokens[1]), parseFloatStr(tokens[2]), parseFloatStr(tokens[3]));
          continue;
        case 'p':
          if (line.equals("pp 3")) {
            int i1 = getVertex();
            int i2 = getVertex();
            int i3 = getVertex();
            nTriangles++;
            addTriangleCheck(i1, i2, i3, 7, 0, false, color);
          } 
          continue;
        }
      }
    } catch (Exception e) {
      // end of file
    }
    return true;
  }

  private final Map<String, Integer> vertexMap = new Hashtable<String, Integer>();
  
  private final P3 pt = new P3();
    
  private int getVertex() throws Exception {
    Integer i = vertexMap.get(rd());
    if (i == null) {
      String[] tokens = getTokens();
      pt.set(parseFloatStr(tokens[0]), parseFloatStr(tokens[1]), parseFloatStr(tokens[2]));
      if (!Float.isNaN(params.scale))
        pt.scale(params.scale);
      if (isAnisotropic)
        setVertexAnisotropy(pt);
      i = Integer.valueOf(addVertexCopy(pt, 0, nVertices++, true));
      vertexMap.put(line, i);
    }
    return i.intValue();
  }

}
