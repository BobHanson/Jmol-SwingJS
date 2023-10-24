/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-10-14 12:33:20 -0500 (Sun, 14 Oct 2007) $
 * $Revision: 8408 $

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.util;


import java.util.HashMap;
import java.util.Map;

import javajs.util.MeasureD;
import javajs.util.P3d;
import javajs.util.P4d;
import javajs.util.T3d;
import javajs.util.V3d;

/**
 * The BoxInfo class holds critical information about boundboxes. 
 * These are simple tetragonal spaces lined up with x,y,z.
 * 
 */
public class BoxInfo {
 
  public final static int X   = 4;
  public final static int Y   = 2;
  public final static int Z   = 1;
  public static final int XYZ = 7;

  public final P3d bbCorner0 = new P3d();
  public final P3d bbCorner1 = new P3d();
  private final P3d bbCenter = new P3d();
  private final V3d bbVector = new V3d();
  
  /**
   * The ordering of these vertices is given below. Do not mess with that.
   * 
   */
  private final Point3fi[] bbVertices = new Point3fi[8];
  private boolean isScaleSet;
  private double margin;

  public static char[] bbcageTickEdges = {
    'z', '\0', '\0', 'y', 
    'x', '\0', '\0', '\0', 
    '\0', '\0', '\0', '\0'};
  
  public static char[] uccageTickEdges = {
    'z', 'y', 'x', '\0', 
    '\0', '\0', '\0', '\0', 
    '\0', '\0', '\0', '\0'};
  
  public final static byte edges[] = {
    0,1, 0,2, 0,4, 1,3, 
    1,5, 2,3, 2,6, 3,7, 
    4,5, 4,6, 5,7, 6,7
  };

  public BoxInfo() {
    for (int i = 8; --i >= 0;)
      bbVertices[i] = new Point3fi();
    reset();
  }
  
  public void reset() {
    isScaleSet = false;
    bbCorner0.set(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
    bbCorner1.set(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);
  }
  
  public static void scaleBox(P3d[] pts, double scale) {
    if (scale == 0 || scale == 1)
      return;
    P3d center = new P3d();
    V3d v = new V3d();
    for (int i = 0; i < 8; i++)
      center.add(pts[i]);
    center.scale(1/8d);
    for (int i = 0; i < 8; i++) {
      v.sub2(pts[i], center);
      v.scale(scale);
      pts[i].add2(center, v);
    }
  }

  // unitCubePoints and Edges
  //  -- constructed in a binary pattern
  //  -- used for BoundingBox
  //
  //                     Y 
  //                      2 --------6--------- 6                            
  //                     /|                   /|          
  //                    / |                  / |           
  //                   /  |                 /  |           
  //                  5   1               11   |           
  //                 /    |               /    9           
  //                /     |              /     |         
  //               3 --------7--------- 7      |         
  //               |      |             |      |         
  //               |      0 ---------2--|----- 4    X        
  //               |     /              |     /          
  //               3    /              10    /           
  //               |   0                |   8            
  //               |  /                 |  /             
  //               | /                  | /               
  //               1 ---------4-------- 5                 
  //              Z                                       
  
  public final static int[] faceOrder = new int[] { 0, 3, 5, 2, 1, 4 };
  public final static int[][] facePoints = new int[][] {
    {4, 0, 6}, // xy0
    {4, 6, 5}, // yz1
    {5, 7, 1}, // xy1
    {1, 3, 0}, // yz0
    {6, 2, 7}, // xz1
    {1, 0, 5}, // xz0
    
    {0, 2, 6},
    {6, 7, 5}, 
    {7, 3, 1}, 
    {3, 2, 0},
    {2, 3, 7}, 
    {0, 4, 5}, 
  };
  
  public final static P3d[] unitCubePoints = { 
    P3d.new3(0, 0, 0), // 0
    P3d.new3(0, 0, 1), // 1
    P3d.new3(0, 1, 0), // 2
    P3d.new3(0, 1, 1), // 3
    P3d.new3(1, 0, 0), // 4
    P3d.new3(1, 0, 1), // 5
    P3d.new3(1, 1, 0), // 6
    P3d.new3(1, 1, 1), // 7
  };
  
  /**
   * 
   * @param oabc
   *        [center a b c]
   * @return all eight vertices
   */
  public final static P3d[] getVerticesFromOABC(T3d[] oabc) {
    P3d[] vertices = new P3d[8];
    for (int i = 0; i <= XYZ; i++) {
      vertices[i] = P3d.newP(oabc[0]);
      if ((i & X) == X)
        vertices[i].add(oabc[1]);
      if ((i & Y) == Y)
        vertices[i].add(oabc[2]);
      if ((i & Z) == Z)
        vertices[i].add(oabc[3]);
    }
    return vertices;
  }

  // canonical
  //  -- relatively standard clockwise lower, then clockwise upper
  //  -- used by Triangulator and MarchingCubes
  //
  //                      Y 
  //                       4 --------4--------- 5                           
  //                      /|                   /|         
  //                     / |                  / |         
  //                    /  |                 /  |         
  //                   7   8                5   |         
  //                  /    |               /    9         
  //                 /     |              /     |         
  //                7 --------6--------- 6      |         
  //                |      |             |      |         
  //                |      0 ---------0--|----- 1    X        
  //                |     /              |     /          
  //               11    /               10   /           
  //                |   3                |   1            
  //                |  /                 |  /             
  //                | /                  | /              
  //                3 ---------2-------- 2                
  //               Z                                       
  //    
  //  protected final static P3i[] canonicalVertexOffsets = { 
  //    P3i.new3(0, 0, 0), //0 pt
  //    P3i.new3(1, 0, 0), //1 pt + yz
  //    P3i.new3(1, 0, 1), //2 pt + yz + 1
  //    P3i.new3(0, 0, 1), //3 pt + 1
  //    P3i.new3(0, 1, 0), //4 pt + z
  //    P3i.new3(1, 1, 0), //5 pt + yz + z
  //    P3i.new3(1, 1, 1), //6 pt + yz + z + 1
  //    P3i.new3(0, 1, 1)  //7 pt + z + 1 
  //  };
  
  private final static int[] toCanonical = new int[] {0, 3, 4, 7, 1, 2, 5, 6};

  /**
   * Change points references to canonical form used in Triangulator, while also scaling.
   * 
   * Box Pt to canonical:
   * <pre>
    0 to 0 
    1 to 3
    2 to 4
    3 to 7
    4 to 1
    5 to 2
    6 to 5
    7 to 6
     </pre>
   * @param boxPoints
   * @param scale
   * @return canonical P3 array
   */
  public final static P3d[] getCanonicalCopy(P3d[] boxPoints, double scale) {
    P3d[] pts = new P3d[8];
    for (int i = 0; i < 8; i++)
      pts[toCanonical[i]] = P3d.newP(boxPoints[i]);
    scaleBox(pts, scale);
    return pts;
  }
  
  /**
   * Delivers [center a b c] for generation of unit cells from a boundbox
   * 
   * @param bbVertices
   * @param offset
   * @return [center a b c]
   */
  public final static P3d[] toOABC(P3d[] bbVertices, T3d offset) {
    P3d center = P3d.newP(bbVertices[0]);
    P3d a = P3d.newP(bbVertices[X]);
    P3d b = P3d.newP(bbVertices[Y]);
    P3d c = P3d.newP(bbVertices[Z]);
    a.sub(center);
    b.sub(center);
    c.sub(center);
    if (offset != null)
      center.add(offset);
    return new P3d[] { center, a, b, c };
  }
  
  private final static P3d[] unitBboxPoints = new P3d[8];
  {
    for (int i = 0; i < 8; i++) {
      unitBboxPoints[i] = P3d.new3(-1, -1, -1);
      unitBboxPoints[i].scaleAdd2(2, unitCubePoints[i], unitBboxPoints[i]);
    }
  }

  public P3d getBoundBoxCenter() {
    if (!isScaleSet)
      setBbcage(1);
    return bbCenter;
  }

  public V3d getBoundBoxCornerVector() {
    if (!isScaleSet)
      setBbcage(1);
    return bbVector;
  }

  /**
   * Return basic info on boundbox in the form of an array.
   *  
   * @param isAll to include center and diagonal
   * @return isAll: [(0.5 0.5 0.5), diagonal, (0 0 0), (1 1 1)], otherwise just [(0 0 0), (1 1 1)]
   * 
   */
  public P3d[] getBoundBoxPoints(boolean isAll) {
    if (!isScaleSet)
      setBbcage(1);
    return (isAll ? new P3d[] { bbCenter, P3d.newP(bbVector), bbCorner0,
        bbCorner1 } : new P3d[] { bbCorner0, bbCorner1 });
  }

  public Point3fi[] getBoundBoxVertices() {
    if (!isScaleSet)
      setBbcage(1);
    return bbVertices;
  }
  
  public void setBoundBoxFromOABC(T3d[] points) {
    P3d origin = P3d.newP(points[0]);
    P3d pt111 = new P3d();
    for (int i = 0; i < 4; i++)
      pt111.add(points[i]);
    setBoundBox(origin, pt111, true, 1);
  }
  
  public void setBoundBox(T3d pt1, T3d pt2, boolean byCorner, double scale) {
    if (pt1 != null) {
      if (scale == 0)
        return;
      if (byCorner) {
        if (pt1.distance(pt2) == 0)
          return;
        bbCorner0.set(Math.min(pt1.x, pt2.x), Math.min(pt1.y, pt2.y), Math.min(
            pt1.z, pt2.z));
        bbCorner1.set(Math.max(pt1.x, pt2.x), Math.max(pt1.y, pt2.y), Math.max(
            pt1.z, pt2.z));
      } else { // center and vector
        if (pt2.x == 0 || pt2.y == 0 && pt2.z == 0)
          return;
        bbCorner0.set(pt1.x - pt2.x, pt1.y - pt2.y, pt1.z - pt2.z);
        bbCorner1.set(pt1.x + pt2.x, pt1.y + pt2.y, pt1.z + pt2.z);
      }
    }
    setBbcage(scale);
  }

  public void setMargin(double m) {
    margin = m;
  }
  
  P3d ptTemp = new P3d();
  public void addBoundBoxPointD(T3d pt) {
    isScaleSet = false;
    ptTemp.set(pt.x, pt.y, pt.z); 
    addPoint(ptTemp, bbCorner0, bbCorner1, margin);
  }

  public void addBoundBoxPoint(T3d pt) {
    isScaleSet = false;
    addPoint(pt, bbCorner0, bbCorner1, margin);
  }

  public static void addPoint(T3d pt, T3d xyzMin, T3d xyzMax, double margin) {
    if (pt.x - margin < xyzMin.x)
      xyzMin.x = pt.x - margin;
    if (pt.x + margin > xyzMax.x)
      xyzMax.x = pt.x + margin;
    if (pt.y - margin < xyzMin.y)
      xyzMin.y = pt.y - margin;
    if (pt.y + margin > xyzMax.y)
      xyzMax.y = pt.y + margin;
    if (pt.z - margin < xyzMin.z)
      xyzMin.z = pt.z - margin;
    if (pt.z + margin > xyzMax.z)
      xyzMax.z = pt.z + margin;
  }
 
  public static void addPointXYZ(double x, double y, double z, P3d xyzMin, P3d xyzMax, double margin) {
    if (x - margin < xyzMin.x)
      xyzMin.x = x - margin;
    if (x + margin > xyzMax.x)
      xyzMax.x = x + margin;
    if (y - margin < xyzMin.y)
      xyzMin.y = y - margin;
    if (y + margin > xyzMax.y)
      xyzMax.y = y + margin;
    if (z - margin < xyzMin.z)
      xyzMin.z = z - margin;
    if (z + margin > xyzMax.z)
      xyzMax.z = z + margin;
  }

  public void setBbcage(double scale) {
    isScaleSet = true;
    bbCenter.add2(bbCorner0, bbCorner1);
    bbCenter.scale(0.5d);
    bbVector.sub2(bbCorner1, bbCenter);
    if (scale > 0) {
      bbVector.scale(scale);
    } else {
      bbVector.x -= scale / 2;
      bbVector.y -= scale / 2;
      bbVector.z -= scale / 2;
    }
    for (int i = 8; --i >= 0;) {
      P3d pt = bbVertices[i];
      pt.setT(unitBboxPoints[i]);
      pt.x *= bbVector.x;
      pt.y *= bbVector.y;
      pt.z *= bbVector.z;
      pt.add(bbCenter);
    }
    if (scale != 1) {
      bbCorner0.setT(bbVertices[0]);
      bbCorner1.setT(bbVertices[7]);
    }
  }
  
  public boolean isWithin(P3d pt) {
    if (!isScaleSet)
      setBbcage(1);
   return (pt.x >= bbCorner0.x && pt.x <= bbCorner1.x 
       && pt.y >= bbCorner0.y && pt.y <= bbCorner1.y
       && pt.z >= bbCorner0.z && pt.z <= bbCorner1.z); 
  }

  public double getMaxDim() {
    return bbVector.length() * 2;
  }
  
  /**
   * for {*}.boundbox("info"|"volume"|"center"|null)
   * 
   * @param what
   * @return Double or Map or null
   */
  public Object getInfo(String what) {
    Double vol = Double
        .valueOf(Math.abs(8 * bbVector.x * bbVector.y * bbVector.z));
    if ("volume".equals(what)) {
      return vol;
    }
    P3d c = P3d.newPd(bbCenter);
    if ("center".equals(what)) {
      return c;
    }
    if (what == null || "info".equals(what)) {
      Map<String, Object> m = new HashMap<String, Object>();
      m.put("center", c);
      V3d v = V3d.newVsub(bbCorner1, bbCorner0);
      m.put("dimensions", v);
      m.put("girth", Double.valueOf(v.x + v.y + v.z));
      m.put("area", Double.valueOf(2 * (v.x * v.y + v.x * v.z + v.z * v.y)));
      m.put("volume", vol);
      return m;
    }
    return null;
  }

  @Override
  public String toString() {
    return "" + bbCorner0 + bbCorner1;
  }

  public static P4d[] getBoxFacesFromOABC(P3d[] oabc) {
    P4d[] faces = new P4d[6];
    V3d vNorm = new V3d();
    V3d vAB = new V3d();
    P3d pta = new P3d();
    P3d ptb = new P3d();
    P3d ptc = new P3d();
    P3d[] vertices = (oabc == null ? unitCubePoints : getVerticesFromOABC(oabc));
    for (int i = 0; i < 6; i++) {
      pta.setT(vertices[facePoints[i][0]]);
      ptb.setT(vertices[facePoints[i][1]]);
      ptc.setT(vertices[facePoints[i][2]]);
      faces[i] = MeasureD.getPlaneThroughPoints(pta, ptb, ptc, vNorm, vAB, new P4d());
    }
    return faces;
  }


}
