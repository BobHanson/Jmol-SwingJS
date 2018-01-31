/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-06-10 13:04:22 -0500 (Thu, 10 Jun 2010) $
 * $Revision: 13329 $
 *
 * Copyright (C) 2005  The Jmol Development Team
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

package org.jmol.smiles;


import javajs.util.P3;
import javajs.util.PT;
import javajs.util.T3;

public class SmilesMeasure  {

  // processing of C(.d:1.5,1.6)C
  // or C(.d1:1.5)C(.d1:1.6)
  SmilesSearch search;
  int nPoints;
  int type;
  int index;
  boolean isNot;
  
  private int[] indices = new int[4];
  static final String TYPES = "__dat";
  final private float[] minmax;
  
  SmilesMeasure(SmilesSearch search, int index, int type, boolean isNot,
      float[] minmax) {
    this.search = search;
    this.type = Math.min(4, Math.max(type, 2));
    this.index = index;
    this.isNot = isNot;
    this.minmax = minmax;
    for (int i = minmax.length - 2; i >= 0; i -= 2)
      if (minmax[i] > minmax[i + 1]) {
        float min = minmax[i + 1];
        minmax[i + 1] = minmax[i];
        minmax[i] = min;
      }
  }

  boolean addPoint(int index) {
    if (nPoints == type)
      return false;
    if (nPoints == 0)
      for (int i = 1; i < type; i++)
        indices[i] = index + i;
    indices[nPoints++] = index;
    return true;
  }
  
  private final static float radiansPerDegree = (float) (2 * Math.PI / 360);

  private final P3[] points = new P3[4];
  
  boolean check() {
    for (int i = 0; i < type; i++) {
      int iAtom = search.patternAtoms[indices[i]].getMatchingAtomIndex();
      //System.out.print(iAtom + "-");
      points[i] = (P3) search.targetAtoms[iAtom];
      //System.out.println(points[i]);
    }
    float d = 0;
    switch (type) {
    case 2:
      d = points[0].distance(points[1]);
      break;
    case 3:
      search.v.vA.sub2(points[0], points[1]);
      search.v.vB.sub2(points[2], points[1]);
      d = search.v.vA.angle(search.v.vB) / radiansPerDegree;
      break;
    case 4:
      d = setTorsionData(points[0], points[1], points[2], points[3], search.v, true);
      break;
    }
    for (int i = minmax.length - 2; i >= 0; i -= 2)
      if (d >= minmax[i] && d <= minmax[i + 1])
        return !isNot;
    return isNot;
  }

  /**
   * calculate temporary points
   * @param pt1a
   * @param pt1
   * @param pt2
   * @param pt2a
   * @param v
   * @param withDihedral
   * @return dihedral or 0
   */
  public static float setTorsionData(T3 pt1a, T3 pt1,
                                    T3 pt2, T3 pt2a,
                                    VTemp v, boolean withDihedral) {
    // We cross dihedral bonds with the bond axis
    // to get two vector projections in the
    // plane perpendicular to the bond axis
    
    //          pt1a_                _pt2a
    //             |\ vtemp1         /| vtemp2
    //               \              /
    //               pt1-----------pt2
    
    
    
    v.vTemp1.sub2(pt1a, pt1);
    v.vTemp2.sub2(pt2a, pt2);
    if (!withDihedral)
      return 0;
    
    //          pt1a                  pt2a
    //              \                /
    //               \              /
    //               pt1---<vnorm2-pt2
    
    //            vtemp1 = (pt1a-pt1) x vnorm2
    //            vtemp2 = (pt2a-pt2) x vnorm2
    //            vnorm3 = vtemp1 x vtemp2
    

    v.vNorm2.sub2(pt1, pt2);
    v.vNorm2.normalize();
    v.vTemp1.cross(v.vTemp1, v.vNorm2);
    v.vTemp1.normalize();
    v.vTemp2.cross(v.vTemp2, v.vNorm2);
    v.vTemp2.normalize();
    v.vNorm3.cross(v.vTemp1, v.vTemp2);
    return v.vTemp1.angle(v.vTemp2) / radiansPerDegree
        * (v.vNorm2.dot(v.vNorm3) < 0 ? 1 : -1);

  }
  
  @Override
  public String toString() {
    String s = "(." + TYPES.charAt(type) + index + ":" + PT.toJSON(null, minmax) + ") for";
    for (int i = 0; i < type; i++)
      s+= " " + (i >= nPoints ? "?" : "" + indices[i]);
    return s;
  }

}