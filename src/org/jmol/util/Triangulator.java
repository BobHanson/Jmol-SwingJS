package org.jmol.util;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.T3;

import org.jmol.java.BS;

public class Triangulator extends TriangleData {

  
//                       Y 
//                        4 --------4--------- 5                   
//                       /|                   /| 
//                      / |                  / | 
//                     /  |                 /  | 
//                    7   8                5   | 
//                   /    |               /    9 
//                  /     |              /     | 
//                 7 --------6--------- 6      | 
//                 |      |             |      | 
//                 |      0 ---------0--|----- 1  X    
//                 |     /              |     /   
//                11    /               10   /    
//                 |   3                |   1 
//                 |  /                 |  /  
//                 | /                  | /   
//                 3 ---------2-------- 2     
//                Z                           

  public final static int[][] fullCubePolygon = new int[][] {
    { 0, 4, 5, 3 }, { 5, 1, 0, 3 }, // back
    { 1, 5, 6, 2 }, { 6, 2, 1, 3 }, 
    { 2, 6, 7, 2 }, { 7, 3, 2, 3 }, // front
    { 3, 7, 4, 2 }, { 4, 0, 3, 2 },
    { 6, 5, 4, 0 }, { 4, 7, 6, 0 }, // top
    { 0, 1, 2, 0 }, { 2, 3, 0, 0 }, // bottom
  };

  /**
   * a generic cell - plane intersector -- used for finding the plane through a
   * 
   * not static so as to allow JavaScript to not load it as core.
   * 
   * unit cell
   * 
   * @param plane  intersecting plane, or null for a full list of all faces
   * @param vertices the vertices of the box or unit cell in canonical format
   * @param flags
   *          0 -- polygon int[]  1 -- edges only 2 -- triangles only 3 -- both
   * @return Lst of P3[3] triangles and P3[2] edge lines
   */

  public Lst<Object> intersectPlane(P4 plane, T3[] vertices, int flags) {
    Lst<Object> v = new Lst<Object>();
    P3[] edgePoints = new P3[12];
    int insideMask = 0;
    float[] values = new float[8];
    for (int i = 0; i < 8; i++) {
      values[i] = plane.x * vertices[i].x + plane.y * vertices[i].y + plane.z
          * vertices[i].z + plane.w;
      if (values[i] < 0)
        insideMask |= Pwr2[i];
    }
    byte[] triangles = triangleTable2[insideMask];
    if (triangles == null)
      return null;
    for (int i = 0; i < 24; i+=2) {
      int v1 = edgeVertexes[i];
      int v2 = edgeVertexes[i + 1];
      // (P - P1) / (P2 - P1) = (0 - v1) / (v2 - v1)
      // or
      // P = P1 + (P2 - P1) * (0 - v1) / (v2 - v1)
      P3 result = P3.newP(vertices[v2]);
      result.sub(vertices[v1]);
      result.scale(values[v1] / (values[v1] - values[v2]));
      result.add(vertices[v1]);
      edgePoints[i >> 1] = result;
    }
    if (flags == 0) {
      BS bsPoints = new BS();
      for (int i = 0; i < triangles.length; i++) {
        bsPoints.set(triangles[i]);
        if (i % 4 == 2)
          i++;
      }
      int nPoints = bsPoints.cardinality();
      P3[] pts = new P3[nPoints];
      v.addLast(pts);
      int[]list = new int[12];
      int ptList = 0;
      for (int i = 0; i < triangles.length; i++) {
        int pt = triangles[i];
        if (bsPoints.get(pt)) {
          bsPoints.clear(pt);
          pts[ptList] = edgePoints[pt];
          list[pt] = (byte) ptList++;
        }          
        if (i % 4 == 2)
          i++;
      }
      
      int[][]polygons = AU.newInt2(triangles.length >> 2);
      v.addLast(polygons);
      for (int i = 0; i < triangles.length; i++)
          polygons[i >> 2] = new int[] { list[triangles[i++]], 
              list[triangles[i++]], list[triangles[i++]], triangles[i] };
      return v;
    }
    for (int i = 0; i < triangles.length; i++) {
      P3 pt1 = edgePoints[triangles[i++]];
      P3 pt2 = edgePoints[triangles[i++]];
      P3 pt3 = edgePoints[triangles[i++]];
      if ((flags & 1) == 1)
        v.addLast(new P3[] { pt1, pt2, pt3 });
      if ((flags & 2) == 2) {
        byte b = triangles[i];
        if ((b & 1) == 1)
          v.addLast(new P3[] { pt1, pt2 });
        if ((b & 2) == 2)
          v.addLast(new P3[] { pt2, pt3 });
        if ((b & 4) == 4)
          v.addLast(new P3[] { pt1, pt3 });
      }
    }
    return v;
  }
}
