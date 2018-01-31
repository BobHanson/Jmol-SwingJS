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
import java.util.Date;

import javajs.util.SB;



abstract class PolygonFileReader extends SurfaceFileReader {

  protected int nVertices;
  protected int nTriangles;

  PolygonFileReader(){}
  
  protected void init2PFR(SurfaceGenerator sg, BufferedReader br) {
    init2SFR(sg, br);
    jvxlFileHeaderBuffer = new SB();
    jvxlFileHeaderBuffer.append("#created ").append("" + new Date()).append("\n");
    vertexDataOnly = true;
  }

  @Override
  protected boolean readVolumeParameters(boolean isMapData) {
    // required by SurfaceReader
    return true;
  }
  
  @Override
  protected boolean readVolumeData(boolean isMapData) {
    // required by SurfaceReader
    return true;
  }

  @Override
  protected void readSurfaceData(boolean isMapData) throws Exception {
    getSurfaceData();
    // required by SurfaceReader
  }

  abstract void getSurfaceData() throws Exception;
  
}
