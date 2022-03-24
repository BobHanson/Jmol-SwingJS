/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-11-22 04:06:32 -0600 (Thu, 22 Nov 2007) $
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
package org.jmol.bspt;

import javajs.util.BS;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.Point3fi;

import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.T3;

public class PointIterator {

  /**
   * carry out various functions of the within() script function
   * 
   * @param distance
   *        0 for closest only
   * @param pt
   *        if not null and pt.x == Float.NaN, this is an internal comparison,
   *        to return a "cleaned" list of points
   * @param ap3
   *        the list of points, required
   * @param ap31
   *        a second list of points, optional
   * @param bsSelected
   *        TODO
   * @param ret
   *        null, "", int[], Lst<T3>, or P3
   * @return T.nada, T.string, T.array, T.list, T.point
   */
  public static int withinDistPoints(float distance, P3 pt, P3[] ap3, P3[] ap31,
                                     BS bsSelected, Object[] ret) {
    Lst<T3> pts = new Lst<T3>();
    Bspt bspt = new Bspt(3, 0);
    CubeIterator iter;
    if (pt != null && Float.isNaN(pt.x)) {
      // internal comparison
      Point3fi[] pt3 = new Point3fi[ap3.length];
      Point3fi p;
      for (int i = pt3.length; --i >= 0;) {
        P3 p3 = ap3[i];
        if (p3 == null)
          return T.nada;
        if (bsSelected == null) {
          p = new Point3fi();
          p.setT(p3);
          p.i = i;
          pt3[i] = p;
          bspt.addTuple(p);
        } else {
          bspt.addTuple(p3);
        }
      }
      iter = bspt.allocateCubeIterator();
      BS bsp;
      if (bsSelected != null) {
        // {*}.within(0.1, [points])
        bsp = BS.newN(ap31.length);
        for (int i = bsSelected.nextSetBit(0); i >= 0; i = bsSelected
            .nextSetBit(i + 1)) {
          iter.initialize(ap31[i], distance, false);
          float d2 = distance * distance;
          while (iter.hasMoreElements()) {
            if (iter.nextElement().distanceSquared(ap31[i]) <= d2) {
              bsp.set(i);
              break;
            }
          }
        }
        ret[0] = bsp;
        return T.bitset;
      }
      bsp = BSUtil.newBitSet2(0, ap3.length);
      for (int i = pt3.length; --i >= 0;) {
        iter.initialize(p = pt3[i], distance, false);
        float d2 = distance * distance;
        int n = 0;
        while (iter.hasMoreElements()) {
          Point3fi pt2 = (Point3fi) iter.nextElement();
          if (bsp.get(pt2.i) && pt2.distanceSquared(p) <= d2 && (++n > 1))
            bsp.clear(pt2.i);
        }
      }
      for (int i = bsp.nextSetBit(0); i >= 0; i = bsp.nextSetBit(i + 1))
        pts.addLast(P3.newP(pt3[i]));
      ret[0] = pts;
      return T.list;
    }
    if (distance == 0) {
      // closest
      if (ap31 == null) {
        float d2 = Float.MAX_VALUE;
        P3 pt3 = null;
        for (int i = ap3.length; --i >= 0;) {
          P3 pta = ap3[i];
          distance = pta.distanceSquared(pt);
          if (distance < d2) {
            pt3 = pta;
            d2 = distance;
          }
        }
        ret[0] = (pt3 == null ? "" : pt3);
        return (pt3 == null ? T.string : T.point);
      }
      int[] ptsOut = new int[ap31.length];
      for (int i = ptsOut.length; --i >= 0;) {
        float d2 = Float.MAX_VALUE;
        int imin = -1;
        pt = ap31[i];
        for (int j = ap3.length; --j >= 0;) {
          P3 pta = ap3[j];
          distance = pta.distanceSquared(pt);
          if (distance < d2) {
            imin = j;
            d2 = distance;
          }
        }
        ptsOut[i] = imin;
      }
      ret[0] = ptsOut;
      return T.array;
    }
    for (int i = ap3.length; --i >= 0;)
      bspt.addTuple(ap3[i]);
    iter = bspt.allocateCubeIterator();
    iter.initialize(pt, distance, false);
    float d2 = distance * distance;
    while (iter.hasMoreElements()) {
      T3 pt2 = iter.nextElement();
      if (pt2.distanceSquared(pt) <= d2)
        pts.addLast(pt2);
    }
    iter.release();
    ret[0] = pts;
    return T.list;
  }    
}
