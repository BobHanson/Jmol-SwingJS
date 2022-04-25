/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-10-09 22:54:03 -0500 (Sun, 09 Oct 2016) $
 * $Revision: 21262 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.g3d;



import javajs.util.Lst;


import org.jmol.api.JmolRendererInterface;
import org.jmol.util.GData;

import javajs.util.P3d;
import javajs.util.P3i;

import javajs.util.V3d;


/**
 *<p>
 * Implementation of hermite curves for drawing smoothed curves
 * that pass through specified points.
 *</p>
 *<p>
 * Examples of usage in Jmol include the commands: <code>trace,
 * ribbons and cartoons</code>.
 *</p>
 *<p>
 * for some useful background info about hermite curves check out
 * <a href='http://www.cubic.org/docs/hermite.htm'>
 * http://www.cubic.org/docs/hermite.htm
 * </a>
 * 
 * Technically, Jmol implements a Cardinal spline varient of the Hermitian spline
 *</p>
 *
 * @author Miguel, miguel@jmol.org
 */
public class HermiteRenderer implements G3DRenderer {

  private static V3d vAB = new V3d();
  private static V3d vAC = new V3d();

  /* really a private class to g3d and export3d */

  
  private JmolRendererInterface g3d;
  private GData gdata;

  public HermiteRenderer() {
  }

  @Override
  public G3DRenderer set(JmolRendererInterface g3d, GData gdata) {
    this.g3d = g3d;
    this.gdata = gdata;
    return this;
  }

  private final P3i[] pLeft = new P3i[16];
  private final P3i[] pRight = new P3i[16];

  private final double[] sLeft = new double[16];
  private final double[] sRight = new double[16];

  private final P3d[] pTopLeft = new P3d[16];
  private final P3d[] pTopRight = new P3d[16];
  private final P3d[] pBotLeft = new P3d[16];
  private final P3d[] pBotRight = new P3d[16];
  {
    for (int i = 16; --i >= 0; ) {
      pLeft[i] = new P3i();
      pRight[i] = new P3i();

      pTopLeft[i] = new P3d();
      pTopRight[i] = new P3d();
      pBotLeft[i] = new P3d();
      pBotRight[i] = new P3d();
    }
  }

  public void renderHermiteRope(boolean fill, int tension,
                     int diameterBeg, int diameterMid, int diameterEnd,
                     P3d p0, P3d p1, P3d p2, P3d p3) {
    int z1 = (int) p1.z;
    int z2 = (int) p2.z;
    if (p0.z == 1 ||z1 == 1 ||z2 == 1 ||p3.z == 1)
      return;
    if (gdata.isClippedZ(z1) || gdata.isClippedZ(z2))
      return;
    int x1 = (int) p1.x, y1 = (int) p1.y;
    int x2 = (int) p2.x, y2 = (int) p2.y;
    int xT1 = ((x2 - (int) p0.x) * tension) / 8;
    int yT1 = ((y2 - (int) p0.y) * tension) / 8;
    int zT1 = ((z2 - (int) p0.z) * tension) / 8;
    int xT2 = (((int) p3.x - x1) * tension) / 8;
    int yT2 = (((int) p3.y - y1) * tension) / 8;
    int zT2 = (((int) p3.z - z1) * tension) / 8;
    sLeft[0] = 0;
    pLeft[0].set(x1, y1, z1);
    sRight[0] = 1;
    pRight[0].set(x2, y2, z2);
    int sp = 0;
    int dDiameterFirstHalf = 0;
    int dDiameterSecondHalf = 0;
    if (fill) {
      dDiameterFirstHalf = 2 * (diameterMid - diameterBeg);
      dDiameterSecondHalf = 2 * (diameterEnd - diameterMid);
    }
    do {
      P3i a = pLeft[sp];
      P3i b = pRight[sp];
      int dx = b.x - a.x;
      if (dx >= -1 && dx <= 1) {
        int dy = b.y - a.y;
        if (dy >= -1 && dy <= 1) {
          // mth 2003 10 13
          // I tried drawing short cylinder segments here,
          // but drawing spheres was faster
          double s = sLeft[sp];
          if (fill) {
            int d =(s < 0.5f
                    ? diameterBeg + (int)(dDiameterFirstHalf * s)
                    : diameterMid + (int)(dDiameterSecondHalf * (s - 0.5f)));
            g3d.fillSphereI(d, a);
          } else {
            g3d.plotPixelClippedP3i(a);
          }
          --sp;
          continue;
        }
      }
      double s = (sLeft[sp] + sRight[sp]) / 2;
      double s2 = s * s;
      double s3 = s2 * s;
      double h1 = 2*s3 - 3*s2 + 1;
      double h2 = -2*s3 + 3*s2;
      double h3 = s3 - 2*s2 + s;
      double h4 = s3 - s2;
      if (sp >= 15)
        break;
      P3i pMid = pRight[sp+1];
      pMid.x = (int) (h1*x1 + h2*x2 + h3*xT1 + h4*xT2);
      pMid.y = (int) (h1*y1 + h2*y2 + h3*yT1 + h4*yT2);
      pMid.z = (int) (h1*z1 + h2*z2 + h3*zT1 + h4*zT2);
      pRight[sp+1] = pRight[sp];
      sRight[sp+1] = sRight[sp];
      pRight[sp] = pMid;
      sRight[sp] = (double)s;
      ++sp;
      pLeft[sp].setT(pMid);
      sLeft[sp] = (double)s;
    } while (sp >= 0);
  }

  private final P3d a1 = new P3d();
  private final P3d a2 = new P3d();
  private final P3d b1 = new P3d();
  private final P3d b2 = new P3d();
  private final P3d c1 = new P3d();
  private final P3d c2 = new P3d();
  private final P3d d1 = new P3d();
  private final P3d d2 = new P3d();
  private final V3d T1 = new V3d();
  private final V3d T2 = new V3d();
  private final V3d depth1 = new V3d();
  private final boolean[] needToFill = new boolean[16];

  /**
   * @param fill
   * @param border
   * @param tension
   * @param p0
   * @param p1
   * @param p2
   * @param p3
   * @param p4
   * @param p5
   * @param p6
   * @param p7
   * @param aspectRatio
   * @param fillType
   *        1 front; -1 back; 0 both
   */
  public void renderHermiteRibbon(boolean fill, boolean border,
                                  int tension,
                                  //top strand segment
                                  P3d p0, P3d p1, P3d p2,
                                  P3d p3,
                                  //bottom strand segment
                                  P3d p4, P3d p5, P3d p6,
                                  P3d p7, int aspectRatio, int fillType) {
    if (p0.z == 1 || p1.z == 1 || p2.z == 1 || p3.z == 1 || p4.z == 1
        || p5.z == 1 || p6.z == 1 || p7.z == 1)
      return;
    if (!fill) {
      tension = Math.abs(tension);
      renderParallelPair(fill, tension, p0, p1, p2, p3, p4, p5, p6, p7);
      return;
    }
    boolean isRev = (tension < 0);
    if (isRev)
      tension = -tension;
    double ratio = 1f / aspectRatio;
    int x1 = (int) p1.x, y1 = (int) p1.y, z1 = (int) p1.z;
    int x2 = (int) p2.x, y2 = (int) p2.y, z2 = (int) p2.z;
    int xT1 = ((x2 - (int) p0.x) * tension) / 8;
    int yT1 = ((y2 - (int) p0.y) * tension) / 8;
    int zT1 = ((z2 - (int) p0.z) * tension) / 8;
    int xT2 = (((int) p3.x - x1) * tension) / 8;
    int yT2 = (((int) p3.y - y1) * tension) / 8;
    int zT2 = (((int) p3.z - z1) * tension) / 8;
    pTopLeft[0].set(x1,  y1, z1);
    pTopRight[0].set(x2, y2, z2);

    int x5 = (int) p5.x, y5 = (int) p5.y, z5 = (int) p5.z;
    int x6 = (int) p6.x, y6 = (int) p6.y, z6 = (int) p6.z;
    int xT5 = ((x6 - (int) p4.x) * tension) / 8;
    int yT5 = ((y6 - (int) p4.y) * tension) / 8;
    int zT5 = ((z6 - (int) p4.z) * tension) / 8;
    int xT6 = (((int) p7.x - x5) * tension) / 8;
    int yT6 = (((int) p7.y - y5) * tension) / 8;
    int zT6 = (((int) p7.z - z5) * tension) / 8;
    pBotLeft[0].set(x5, y5, z5);
    pBotRight[0].set(x6, y6, z6);

    sLeft[0] = 0;
    sRight[0] = 1;
    needToFill[0] = true;
    int sp = 0;
    boolean closeEnd = false;
    do {
      P3d a = pTopLeft[sp];
      P3d b = pTopRight[sp];
      double dxTop = b.x - a.x;
      double dxTop2 = dxTop * dxTop;
      if (dxTop2 < 10) {
        double dyTop = b.y - a.y;
        double dyTop2 = dyTop * dyTop;
        if (dyTop2 < 10) {
          P3d c = pBotLeft[sp];
          P3d d = pBotRight[sp];
          double dxBot = d.x - c.x;
          double dxBot2 = dxBot * dxBot;
          if (dxBot2 < 8) {
            double dyBot = d.y - c.y;
            double dyBot2 = dyBot * dyBot;
            if (dyBot2 < 8) {
              if (border) {
                g3d.fillSphereBits(3, a);
                g3d.fillSphereBits(3, c);
              }

              if (needToFill[sp]) {
                if (aspectRatio > 0) {
                  T1.sub2(a, c);
                  T1.scale(ratio);
                  T2.sub2(a, b);
                  depth1.cross(T1, T2);
                  depth1.scale(T1.length() / depth1.length());
                  a1.add2(a, depth1);
                  a2.sub2(a, depth1);
                  b1.add2(b, depth1);
                  b2.sub2(b, depth1);
                  c1.add2(c, depth1);
                  c2.sub2(c, depth1);
                  d1.add2(d, depth1);
                  d2.sub2(d, depth1);
                  g3d.fillQuadrilateral(a1, b1, d1, c1, false);
                  g3d.fillQuadrilateral(a2, b2, d2, c2, false);
                  g3d.fillQuadrilateral(a1, b1, b2, a2, false);
                  g3d.fillQuadrilateral(c1, d1, d2, c2, false);
                  closeEnd = true;
                } else {
                  if (fillType == 0) {
                    if (isRev)
                      g3d.fillQuadrilateral(c, d, b, a, false);
                    else
                      g3d.fillQuadrilateral(a, b, d, c, false);

                  } else {
                    if (isRev) {
                      if (fillType != isFront(a, b, d))
                        g3d.fillTriangle3f(a, b, d, false);
                      if (fillType != isFront(a, d, c))
                        g3d.fillTriangle3f(a, d, c, false);
                    } else {
                      if (fillType == isFront(a, b, d))
                        g3d.fillTriangle3f(a, b, d, false);
                      if (fillType == isFront(a, d, c))
                        g3d.fillTriangle3f(a, d, c, false);
                    }
                  }
                }
                needToFill[sp] = false;
              }
              if (dxTop2 + dyTop2 < 2 && dxBot2 + dyBot2 < 2) {
                --sp;
                continue;
              }
            }
          }
        }
      }
      double s = (sLeft[sp] + sRight[sp]) / 2;
      double s2 = s * s;
      double s3 = s2 * s;
      double h1 = 2 * s3 - 3 * s2 + 1;
      double h2 = -2 * s3 + 3 * s2;
      double h3 = s3 - 2 * s2 + s;
      double h4 = s3 - s2;

      if (sp >= 15)
        break;
      int spNext = sp + 1;
      P3d pMidTop = pTopRight[spNext];
      pMidTop.x = (double) (h1 * x1 + h2 * x2 + h3 * xT1 + h4 * xT2);
      pMidTop.y = (double) (h1 * y1 + h2 * y2 + h3 * yT1 + h4 * yT2);
      pMidTop.z = (double) (h1 * z1 + h2 * z2 + h3 * zT1 + h4 * zT2);
      P3d pMidBot = pBotRight[spNext];
      pMidBot.x = (double) (h1 * x5 + h2 * x6 + h3 * xT5 + h4 * xT6);
      pMidBot.y = (double) (h1 * y5 + h2 * y6 + h3 * yT5 + h4 * yT6);
      pMidBot.z = (double) (h1 * z5 + h2 * z6 + h3 * zT5 + h4 * zT6);

      pTopRight[spNext] = pTopRight[sp];
      pTopRight[sp] = pMidTop;
      pBotRight[spNext] = pBotRight[sp];
      pBotRight[sp] = pMidBot;

      sRight[spNext] = sRight[sp];
      sRight[sp] = (double) s;
      needToFill[spNext] = needToFill[sp];
      pTopLeft[spNext].setT(pMidTop);
      pBotLeft[spNext].setT(pMidBot);
      sLeft[spNext] = (double) s;
      ++sp;
    } while (sp >= 0);
    if (closeEnd) {
      a1.z += 1;
      c1.z += 1;
      c2.z += 1;
      a2.z += 1;
      g3d.fillQuadrilateral(a1, c1, c2, a2, false);
    }
  }
 
  private static int isFront(P3d a, P3d b, P3d c) {
    vAB.sub2(b, a);
    vAC.sub2(c, a);
    vAB.cross(vAB, vAC);
    return (vAB.z < 0 ? -1 : 1);
  }

  /**
   * 
   * @param fill   NOT USED
   * @param tension
   * @param p0
   * @param p1
   * @param p2
   * @param p3
   * @param p4
   * @param p5
   * @param p6
   * @param p7
   */
  private void renderParallelPair(boolean fill, int tension,
                //top strand segment
                P3d p0, P3d p1, P3d p2, P3d p3,
                //bottom strand segment
                P3d p4, P3d p5, P3d p6, P3d p7) {
    
    // only used for meshRibbon, so fill = false 
    P3d[] endPoints = {p2, p1, p6, p5};
    // stores all points for top+bottom strands of 1 segment
    Lst<P3d> points = new Lst<P3d>();
    int whichPoint = 0;

    int numTopStrandPoints = 2; //first and last points automatically included
    double numPointsPerSegment = 5.0f;//use 5 for mesh

    //if (fill)
      //numPointsPerSegment = 10.0f;

    double interval = (1.0f / numPointsPerSegment);
    double currentInt = 0.0f;

    int x1 = (int) p1.x, y1 = (int) p1.y, z1 = (int) p1.z;
    int x2 = (int) p2.x, y2 = (int) p2.y, z2 = (int) p2.z;
    int xT1 = ((x2 - (int) p0.x) * tension) / 8;
    int yT1 = ((y2 - (int) p0.y) * tension) / 8;
    int zT1 = ((z2 - (int) p0.z) * tension) / 8;
    int xT2 = (((int) p3.x - x1) * tension) / 8;
    int yT2 = (((int) p3.y - y1) * tension) / 8;
    int zT2 = (((int) p3.z - z1) * tension) / 8;
    sLeft[0] = 0;
    pLeft[0].set(x1, y1, z1);
    sRight[0] = 1;
    pRight[0].set(x2, y2, z2);
    int sp = 0;

    for (int strands = 2; strands > 0; strands--) {
       if (strands == 1) {
         x1 = (int) p5.x; y1 = (int) p5.y; z1 = (int) p5.z;
         x2 = (int) p6.x; y2 = (int) p6.y; z2 = (int) p6.z;
         xT1 = ( (x2 - (int) p4.x) * tension) / 8;
         yT1 = ( (y2 - (int) p4.y) * tension) / 8;
         zT1 = ( (z2 - (int) p4.z) * tension) / 8;
         xT2 = ( ((int) p7.x - x1) * tension) / 8;
         yT2 = ( ((int) p7.y - y1) * tension) / 8;
         zT2 = ( ((int) p7.z - z1) * tension) / 8;
         sLeft[0] = 0;
         pLeft[0].set(x1, y1, z1);
         sRight[0] = 1;
         pRight[0].set(x2, y2, z2);
         sp = 0;
       }

       points.addLast(endPoints[whichPoint++]);
       currentInt = interval;
       do {
         P3i a = pLeft[sp];
         P3i b = pRight[sp];
         int dx = b.x - a.x;
         int dy = b.y - a.y;
         int dist2 = dx * dx + dy * dy;
         if (dist2 <= 2) {
           // mth 2003 10 13
           // I tried drawing short cylinder segments here,
           // but drawing spheres was faster
           double s = sLeft[sp];

           g3d.fillSphereI(3, a);
           //draw outside edges of mesh

           if (s < 1.0f - currentInt) { //if first point over the interval
             P3d temp = new P3d();
             temp.set(a.x, a.y, a.z);
             points.addLast(temp); //store it
             currentInt += interval; // increase to next interval
             if (strands == 2) {
               numTopStrandPoints++;
             }
           }
           --sp;
         }
         else {
           double s = (sLeft[sp] + sRight[sp]) / 2;
           double s2 = s * s;
           double s3 = s2 * s;
           double h1 = 2 * s3 - 3 * s2 + 1;
           double h2 = -2 * s3 + 3 * s2;
           double h3 = s3 - 2 * s2 + s;
           double h4 = s3 - s2;
           if (sp >= 15)
             break;
           P3i pMid = pRight[sp + 1];
           pMid.x = (int) (h1 * x1 + h2 * x2 + h3 * xT1 + h4 * xT2);
           pMid.y = (int) (h1 * y1 + h2 * y2 + h3 * yT1 + h4 * yT2);
           pMid.z = (int) (h1 * z1 + h2 * z2 + h3 * zT1 + h4 * zT2);
           pRight[sp + 1] = pRight[sp];
           sRight[sp + 1] = sRight[sp];
           pRight[sp] = pMid;
           sRight[sp] = (double) s;
           ++sp;
           pLeft[sp].setT(pMid);
           sLeft[sp] = (double) s;
         }
       } while (sp >= 0);
       points.addLast(endPoints[whichPoint++]);
     } //end of for loop - processed top and bottom strands
     int size = points.size();
   /*  
     if (fill) {//RIBBONS
       Point3i t1 = null;
       Point3i b1 = null;
       Point3i t2 = null;
       Point3i b2 = null;
       int top = 1;
       for (;top < numTopStrandPoints && (top + numTopStrandPoints) < size; top++) {
         t1 = (Point3i) points.elementAt(top - 1);
         b1 = (Point3i) points.elementAt(numTopStrandPoints + (top - 1));
         t2 = (Point3i) points.elementAt(top);
         b2 = (Point3i) points.elementAt(numTopStrandPoints + top);

         g3d.fillTriangle(t1, b1, t2);
         g3d.fillTriangle(b2, t2, b1);
       }
       if((numTopStrandPoints*2) != size){//BUG(DC09_MAY_2004): not sure why but
         //sometimes misses triangle at very start of segment
         //temp fix - will inestigate furture
         g3d.fillTriangle(p1, p5, t2);
         g3d.fillTriangle(b2, t2, p5);
       }
     }
     else {//MESH
     */
       for (int top = 0;
            top < numTopStrandPoints && (top + numTopStrandPoints) < size; top++)
         g3d.drawLineAB(points.get(top),
             points.get(top + numTopStrandPoints));
  }

}
