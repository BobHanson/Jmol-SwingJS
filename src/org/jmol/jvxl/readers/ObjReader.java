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


import javajs.util.BS;

import javajs.util.CU;
import javajs.util.P3;
import javajs.util.PT;

/*
 * 
 * See at http://www.eg-models.de/formats/Format_Obj.html
 * an extension of the Edinburgh pmesh file format 
 *   see http://i-sight.sourceforge.net/docs/GridView/MeshFileReader.html#ReadPMeshFile(java.lang.String,%20GridView.OutputPanel,%20javax.swing.JProgressBar)
 * 
 * Here we are just looking for v, g, and f.
 *  
 * Groups are designated as any multi-isosurface file using an
 * integer after the file name. In this case, no integer
 * means "read all the data"
 * 
 * Header #obj or #pmesh will identify the mesh as an OBJ file format
 *
 */

//  #obj 6825 vertices 12620 faces 9 groups
//  
//  mtllib g_visible.mtl
//  
//  v 111.025230 65.735298 2.483954
//  v 111.221596 65.772804 2.270540
//  v 111.046539 65.643066 2.200877
//  ...
//  g k000000
//  usemtl k000000
//  f 1 2 3
//  f 1 3 4
//  f 1 4 5
//  
//  g k0066FF
//  usemtl k0066FF
//  f 6 7 8
//  f 8 7 9
//  ...
//  f 6825 6805 6824
//  f 6821 6808 6825
//  f 6808 6825 6804
//  f 6824 6807 6822
//  f 6807 6822 6815
//  f 6805 6824 6806
//  f 6806 6824 6807
//  


class ObjReader extends PmeshReader {

  ObjReader(){}
  
  @Override
  void init2(SurfaceGenerator sg, BufferedReader br) {
    init2PR(sg, br);
    type = "obj";
    setHeader();
  }

  @Override
  protected boolean readVertices() throws Exception {
    // also reads polygons
    pmeshError = "pmesh ERROR: invalid vertex/face list";
    P3 pt = new P3();
    int color = 0;
    int ia, ib, ic, id = 0;
    int i = 0;
    int nPts = 0;
    Map<String, Integer> htPymol = new Hashtable<String, Integer>();
    Integer ipt = null;
    String spt = null;
    int[] pymolMap = new int[3];
    // pymol writes a crude file with much re-writing of vertices

    BS bsOK = new BS();
    while (rd() != null) {
      if (line.length() < 2 || line.charAt(1) != ' ') {
        if (params.readAllData && line.startsWith("usemtl"))
          // usemtl k00FF00
            color = CU.getArgbFromString("[x" + line.substring(8) + "]");
        continue;
      }
      switch (line.charAt(0)) {
      case 'v':
        next[0] = 2;
        pt.set(PT.parseFloatNext(line, next), PT.parseFloatNext(line, next),
            PT.parseFloatNext(line, next));
        boolean addHt = false;
        if (htPymol == null) {
          i = nVertices;
        } else if ((ipt = htPymol.get(spt = "" + pt)) == null) {
          addHt = true;
          i = nVertices;
        } else {
          i = ipt.intValue();
        }

        int j = i;
        if (i == nVertices) {
          if (isAnisotropic)
            setVertexAnisotropy(pt);
          j = addVertexCopy(pt, 0, nVertices++, true);
          if (j >= 0)
            bsOK.set(i);
        }
        pymolMap[nPts % 3] = j;
        if (addHt)
          htPymol.put(spt, Integer.valueOf(i));
        nPts++;
        if (htPymol != null && nPts > 3)
          htPymol = null;
        break;
      case 'f':
        if (nPts == 3 && line.indexOf("//") < 0)
          htPymol = null;
        nPts = 0;
        nPolygons++;
        String[] tokens = PT.getTokens(line);
        int vertexCount = tokens.length - 1;
        if (vertexCount == 4)
          htPymol = null;
        if (htPymol == null) {
          ia = PT.parseInt(tokens[1]) - 1;
          ib = PT.parseInt(tokens[2]) - 1;
          ic = PT.parseInt(tokens[3]) - 1;
          pmeshError = " " + ia + " " + ib + " " + ic + " " + line;
          if (!bsOK.get(ia) || !bsOK.get(ib) || !bsOK.get(ic))
            continue;
          if (vertexCount == 4) {
            id = PT.parseInt(tokens[4]) - 1;
            boolean isOK = (bsOK.get(id));
            nTriangles = addTriangleCheck(ia, ib, ic, (isOK ? 3 : 7), 0, false, color);
            if (isOK)
              nTriangles = addTriangleCheck(ia, ic, id, 6, 0, false, color);
            continue;
          }
        } else {
          ia = pymolMap[0];
          ib = pymolMap[1];
          ic = pymolMap[2];
          if (ia < 0 || ib < 0 || ic < 0)
            continue;
        }
        nTriangles = addTriangleCheck(ia, ib, ic, 7, 0, false, color);
        break;
      case 'g':
        htPymol = null;
        if (params.readAllData) 
          try {
            color = PT.parseIntRadix(line.substring(3), 16);
          } catch (Throwable e) {
            color = 0;
          }
        break;
      }
    }
    pmeshError = null;
    return true;
  }

  @Override
  protected boolean readPolygons() {
    return true;
  }
}
