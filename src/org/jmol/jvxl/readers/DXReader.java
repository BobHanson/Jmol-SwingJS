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

/*
 * A simple DX DataExplorer grid file reader for isosurface.
 * 
 * Same as ApbsReader but not insideout
 *
 * see https://web.cs.wpi.edu/Research/DataExplorer/tutorial/data.html#dx
 */
class DXReader extends ApbsReader {

  DXReader(){}
  
  @Override
  void init2(SurfaceGenerator sg, BufferedReader br) {
    init2VFR(sg, br);
    isAngstroms = true;
    nSurfaces = 1;
  }
//
//// object 1 class gridpositions counts      64      64      64
//// origin  0        0          0
//// delta   1        0          0
//// delta   0        1          0
//// delta   0        0          1
//// object 2 class gridconnections counts      64      64      64
//// object 3 class array type double rank 0 items  262144 data follows
//
//  @Override
//  protected void readParameters() throws Exception {
//    jvxlFileHeaderBuffer = new SB();
//    jvxlFileHeaderBuffer.append("DX data\n");
//    jvxlFileHeaderBuffer.append("\n");
//    rd();
//    while (line != null) {
//      if (line.indexOf("object") >= 0 && line.indexOf("class") >= 0) {
//        if (line.indexOf("gridposition") >= 0) {
//          readGridPositions();
//        } else if (line.indexOf("gridconnections") >= 0) {
//          readGridConnections();
//        } else if (line.indexOf("array") >= 0) {
//          break;
//        }
//      }
//    }
//    setCutoffAutomatic();
//    readData();
//  }
//  
//  private void readGridPositions() throws Exception {
//
//    // object 1 class gridpositions counts      64      64      64
//    // origin  0        0          0
//    // delta   1        0          0
//    // delta   0        1          0
//    // delta   0        0          1
//
//    String[] tokens = readObject();
//    for (int i = 0; i < tokens.length; i++) {
//      if (tokens[i].equals("counts")) {
//        voxelCounts[0] = parseIntStr(tokens[++i]);
//        voxelCounts[1] = parseIntStr(tokens[++i]);
//        voxelCounts[2] = parseIntStr(tokens[++i]);
//        Logger.info("DXReader: setting counts to " + voxelCounts[0] + " "
//            + voxelCounts[1] + " " + voxelCounts[2]);
//      } else if (tokens[i].equals("origin")) {
//        volumetricOrigin.set(parseFloatStr(tokens[++i]),
//            parseFloatStr(tokens[++i]), parseFloatStr(tokens[++i]));
//        Logger.info("DXReader: setting origin to " + volumetricOrigin);
//      } else if (tokens[i].equals("delta")) {
//        for (int j = 0; j < 3; j++, i++) {
//          volumetricVectors[j].set(parseFloatStr(tokens[++i]),
//              parseFloatStr(tokens[++i]), parseFloatStr(tokens[++i]));
//          Logger.info("DXReader: setting volumetricVector " + j + " to " + volumetricVectors[j]);
//        }
//        --i;
//      }
//    }
//  }
//
//  /**
//   * read all tokens of object, assuming line separations are meaningless.
//   * @return array of white-space separated tokens
//   * @throws Exception
//   */
//  private String[] readObject() throws Exception {
//    String data = line;
//    while (rd().indexOf("object") < 0) {
//      data += " " + line;
//    }
//    return PT.getTokens(data);
//  }
//
//  /**
//   * read gridConnections object. Ignoring for now. Cubes assumed
//   * 
//   * @throws Exception
//   */
//  private void readGridConnections() throws Exception {
//
//    // object 2 class gridconnections counts      64      64      64
//    
//    readObject();
//  }
//
//  /**
//   * Just skip this header for now.
//   * 
//   * @throws Exception
//   */
//  private void readData() throws Exception {
// // object 3 class array type double rank 0 items  262144 data follows
//    while (line.indexOf("follows") < 0)
//      rd();
//  }
//
//  protected void setCutoffAutomatic() {
//    if (params.thePlane == null && params.cutoffAutomatic) {
//      params.cutoff = 0.5f;
//      Logger.info(
//          "DXReader: setting cutoff to default value of " + params.cutoff);
//    }
//  }
//
}
