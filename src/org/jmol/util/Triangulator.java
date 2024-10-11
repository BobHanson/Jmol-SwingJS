package org.jmol.util;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.MeasureD;
import javajs.util.P3d;
import javajs.util.P4d;
import javajs.util.T3d;
import javajs.util.V3d;
import javajs.util.BS;

public class Triangulator extends TriangleData {

// see also BoxInfo
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

  public final static int[][] rodEdge = new int[][] {
    { 0, 3, 0, 1 } };

  public final static int[][] friezeEdge = new int[][] {
    { 0, 1, 0, 1 } };

  public final static int[][] abFace = new int[][] {
       { 0, 4, 5, 3 }, { 5, 1, 0, 3 }  };

  public final static int[][] fullCubePolygon = new int[][] {
    /* 0 1   */ { 0, 4, 5, 3 }, { 5, 1, 0, 3 }, // back
    /* 2 3   */ { 1, 5, 6, 2 }, { 6, 2, 1, 3 }, 
    /* 4 5   */ { 2, 6, 7, 2 }, { 7, 3, 2, 3 }, // front
    /* 6 7   */ { 3, 7, 4, 2 }, { 4, 0, 3, 2 },
    /* 8 9   */ { 6, 5, 4, 0 }, { 4, 7, 6, 0 }, // top
    /* 10 11 */ { 0, 1, 2, 0 }, { 2, 3, 0, 0 }, // bottom
  };

  /**
   * For each corner 0-7:
   * 
   * {c c c t}
   * 
   *  where 
   *  
   *  c c c are the connected corners, arranged clockwise
   *  
   *  and
   *  
   *  t is the bitset of triangles associated
   *  with faces intersecting at this corner.
   *  
   */
  public final static int[][] fullCubeCorners = new int[][] {
    { 1, 4, 3,   (3 << 0) | (3 << 6) | (3 << 10) }, // 0
    { 0, 2, 5,   (3 << 0) | (3 << 2) | (3 << 10) }, // 1
    { 1, 3, 6,   (3 << 2) | (3 << 4) | (3 << 10) }, // 2
    { 2, 0, 7,   (3 << 4) | (3 << 6) | (3 << 10) }, // 3
    { 0, 5, 7,   (3 << 0) | (3 << 6) | (3 << 8) }, // 4
    { 1, 6, 4,   (3 << 0) | (3 << 2) | (3 << 8) }, // 5
    { 2, 7, 5,   (3 << 2) | (3 << 4) | (3 << 8) }, // 6
    { 3, 4, 6,   (3 << 4) | (3 << 6) | (3 << 8) }, // 7
  };

  @SuppressWarnings("null")
  public P3d[] intersectLine(P3d[] points, int nPoints, P3d ptA,
                                   V3d unitVector) {
    if (nPoints < 0)
      nPoints = points.length;
    V3d v1 = new V3d();
    V3d v2 = new V3d();
    V3d v3 = new V3d();
    V3d vAB = new V3d();
    P3d pmin = null, pmax = null, p = new P3d();
    P4d plane = new P4d();
    double dmin = Double.MAX_VALUE, dmax = -Double.MAX_VALUE;
    for (int i = 0; i < 12; i++) {
      int[] c = fullCubePolygon[i];
      if (i % 2 == 0) {
        MeasureD.getPlaneThroughPoints(points[c[0]], points[c[1]], points[c[2]],
            v1, v2, plane);
        P3d ret = MeasureD.getIntersection(ptA, unitVector, plane, p, v1, v3);
        if (ret == null) {
          i++;
          continue;
        }
        vAB.sub2(p, ptA);
      }
      if (MeasureD.isInTriangle(p, points[c[0]], points[c[1]], points[c[2]], v1,
          v2, v3)) {
        double d = unitVector.dot(vAB);
        if (d < dmin) {
          dmin = d;
          pmin = p;
        }
        if (d > dmax) {
          dmax = d;
          pmax = p;
        }
        if ((i % 2) == 0) {
          i++;
        }
        if (dmax - dmin > 0.01f)
          break;
        p = new P3d();
      }

    }
    return (pmin == null || (pmax == null || pmin.distance(pmax) < 0.001f) && (pmax = pmin) == null ? null : new P3d[] { pmin, pmax });
  }

  private Lst<Object> getCellProjection(P4d plane, T3d[] pts) {
    V3d vTemp = new V3d();
    // find the point furthest from the plane
    double d = 0, dmax = -Double.MAX_VALUE;
    int imax = 0;
    P3d[] newPts = new P3d[8];
    for (int i = 0; i < 8; i++) {
      d = pts[i].dot(plane);
      if (d > dmax) {
        dmax = d;
        imax = i;
      }
      MeasureD.getPlaneProjection(pts[i], plane, newPts[i] = new P3d(), vTemp);
    }
    int t = fullCubeCorners[imax][3];
    int[][]polygons = AU.newInt2(6);
    // set polygon triangles and adjust edges
    for (int p = 0, i = 0; i < 12; i++) {
      if ((t & Pwr2[i]) != 0) {
        int[] t1 = new int[4];
        int[] t0 = fullCubePolygon[i];
        t1[0] = t0[0];
        t1[1] = t0[1];
        t1[2] = t0[2];
        t1[3] = (t0[0] == imax ? 2 : t0[1] == imax ? 0 : t0[2] == imax ? 1 : 3);
        polygons[p++] = t1;
      }
    }
    Lst<Object> poly = new Lst<Object>();
    poly.addLast(newPts);
    poly.addLast(polygons);
    return poly;
  }
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
   *          -1 -- projection, of cell only, 0 -- polygon int[],  1 -- edges only, 2 -- triangles only 3 -- both
   * @return Lst of P3[3] triangles and P3[2] edge lines
   */

  public Lst<Object> intersectPlane(P4d plane, T3d[] vertices, int flags) {
    if (flags == -1 && vertices.length == 8) {
      return getCellProjection(plane, vertices);
    }
    Lst<Object> v = new Lst<Object>();
    P3d[] edgePoints = new P3d[12];
    int insideMask = 0;
    double[] values = new double[8];
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
      P3d result = P3d.newP(vertices[v2]);
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
      P3d[] pts = new P3d[nPoints];
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
      P3d pt1 = edgePoints[triangles[i++]];
      P3d pt2 = edgePoints[triangles[i++]];
      P3d pt3 = edgePoints[triangles[i++]];
      if ((flags & 1) == 1)
        v.addLast(new P3d[] { pt1, pt2, pt3 });
      if ((flags & 2) == 2) {
        byte b = triangles[i];
        if ((b & 1) == 1)
          v.addLast(new P3d[] { pt1, pt2 });
        if ((b & 2) == 2)
          v.addLast(new P3d[] { pt2, pt3 });
        if ((b & 4) == 4)
          v.addLast(new P3d[] { pt1, pt3 });
      }
    }
    return v;
  }
}
