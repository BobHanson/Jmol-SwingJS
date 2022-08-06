/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
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

package org.jmol.symmetry;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.Lst;
import javajs.util.M3d;
import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.Qd;
import javajs.util.SB;
import javajs.util.T3d;
import javajs.util.V3d;

import org.jmol.bspt.Bspt;
import org.jmol.bspt.CubeIterator;
import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.util.BSUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Node;
import org.jmol.util.Point3fi;

/*
 * Bob Hanson 7/2008
 * 
 * brute force -- preliminary from BCCE20 meeting 2008
 * 
 * NEVER ACCESS THESE METHODS DIRECTLY! ONLY THROUGH CLASS Symmetry
 * 
 *
 */

class PointGroup {

  private final static int[] axesMaxN = new int[] { 
     15, // used for plane count
      0, // n/a 
      0, // not used -- would be S2 (inversion)
      1, // S3
      3, // S4
      1, // S5
      10,// S6
      0, // n/a
      1, // S8
      0, // n/a
      6, // S10
      0, // n/a 
      1, // S12
      0, // n/a
      0, // n/a firstProper = 14
      0, // n/a 
      15,// C2 
      10,// C3 
      6, // C4
      6, // C5
      10,// C6
      0, // C7
      1, // C8
  };

  private final static int[] nUnique = new int[] { 
     1, // used for plane count
     0, // n/a 
     0, // not used -- would be S2 (inversion)
     2, // S3
     2, // S4
     4, // S5
     2, // S6
     0, // n/a
     4, // S8
     0, // n/a
     4, // S10
     0, // n/a 
     4, // S12
     0, // n/a
     0, // n/a firstProper = 14
     0, // n/a 
     1, // C2 
     2, // C3 
     2, // C4
     4, // C5
     2, // C6
     0, // C7
     4, // C8
 };

  private final static int s3 = 3;
  private final static int s4 = 4;
  private final static int s5 = 5;
  private final static int s6 = 6;
  private final static int s8 = 8;
  private final static int s10 = 10;
  private final static int s12 = 12;
  private final static int firstProper = 14;
  private final static int c2 = firstProper + 2;
  private final static int c3 = firstProper + 3;
  private final static int c4 = firstProper + 4;
  private final static int c5 = firstProper + 5;
  private final static int c6 = firstProper + 6;
  private final static int c8 = firstProper + 8;
  private final static int maxAxis = axesMaxN.length;

  private boolean isAtoms;
  String drawInfo;
  Map<String, Object> info;
  String textInfo;

  private CubeIterator iter;
  private String drawType = "";
  private int drawIndex;
  private double scale = Double.NaN;  
  private int[]  nAxes = new int[maxAxis];
  private Operation[][] axes = new Operation[maxAxis][];
  private int nAtoms;
  private double radius;
  private double distanceTolerance = 0.25d; // making this just a bit more generous
  private double distanceTolerance2;
  private double linearTolerance = 8d;
  private double cosTolerance = 0.99d; // 8 degrees
  private String name = "C_1?";
  private Operation principalAxis;
  private Operation principalPlane;


  String getName() {
    return name;
  }

  private final V3d vTemp = new V3d();
  private int centerAtomIndex = -1;
  private boolean haveInversionCenter;
  
  private T3d center;

  private T3d[] points;
  private int[] elements;
  private int[] atomMap;

  private BS bsAtoms;

  private boolean haveVibration;

  private boolean localEnvOnly;


  /**
   * Determine the point group of a set of points or atoms, allowing additionally
   * for considering the point group of vibrational modes.
   * 
   * The two parameters used are "distanceTolerance" and "linearTolerance"
   * 
   * "distanceTolerance is the distance an atom must be within relative to its symmetry-projected
   * idealized position. 
   * 
   * "linearTolerance" has dimension degrees and sets the maximum
   * deviation that two potential symmetry axes can have to be considered
   * "colinear" or "perpendicular" in the final symmetry model. Its default is 8
   * deg.
   * 
   * 
   * @param pgLast helpful to speed checking; may be null
   * @param center  known center atom; may be null
   * @param atomset the set of points or atoms to consider
   * @param bsAtoms possibly some subset of atomset
   * @param haveVibration
   * @param distanceTolerance  atom-position tolerance
   * @param linearTolerance    symmetry-axis direction tolerance
   * @param localEnvOnly set false to additionally consider valence (number of bonds) of atoms
   * 
   * @return a PointGroup
   */
  
  /**
   * 
   * @param pgLast
   * @param center
   * @param atomset a list of Atom or other Point3fi that implements Node
   * @param bsAtoms
   * @param haveVibration     if true, then all items in atomset must be Atom class 
   * @param distanceTolerance
   * @param linearTolerance
   * @param maxAtoms
   * @param localEnvOnly
   * @return a PointGroup object, possibly the last calculated for efficiency
   */
  static PointGroup getPointGroup(PointGroup pgLast, T3d center,
                                         T3d[] atomset, BS bsAtoms,
                                         boolean haveVibration,
                                         double distanceTolerance, double linearTolerance, int maxAtoms, boolean localEnvOnly) {
    PointGroup pg = new PointGroup();
    if (distanceTolerance <= 0) {
      distanceTolerance = 0.01f;
    }
    if (linearTolerance <= 0) {
      linearTolerance = 0.5d;
    }
    if (maxAtoms <= 0)
      maxAtoms = 250;
    pg.distanceTolerance = distanceTolerance;
    pg.distanceTolerance2 = distanceTolerance * distanceTolerance;
    pg.linearTolerance = linearTolerance;
    pg.maxAtoms = maxAtoms;
    pg.isAtoms = (bsAtoms != null);
    pg.bsAtoms = (pg.isAtoms ? bsAtoms : BSUtil.newBitSet2(0, atomset.length));
    pg.haveVibration = haveVibration;
    pg.center = center;
    pg.localEnvOnly = localEnvOnly;
    if (Logger.debugging)
      pgLast = null;
    return (pg.set(pgLast, atomset) ? pg : pgLast);
  }

  private PointGroup() {
  }
  
  private boolean isEqual(PointGroup pg) {
    if (pg == null)
      return false;
    if (linearTolerance != pg.linearTolerance 
        || distanceTolerance != pg.distanceTolerance
        || nAtoms != pg.nAtoms
        || localEnvOnly != pg.localEnvOnly
        || haveVibration != pg.haveVibration
        || bsAtoms ==  null ? pg.bsAtoms != null : !bsAtoms.equals(pg.bsAtoms))
      return false;
    for (int i = 0; i < nAtoms; i++) {
      // real floating == 0 here because they must be IDENTICAL POSITIONS
      if (elements[i] != pg.elements[i] || !points[i].equals(pg.points[i]))
        return false;
    }
    return true;
  }
  
  private boolean set(PointGroup pgLast, T3d[] atomset) {
    cosTolerance =  (Math.cos(linearTolerance / 180 * Math.PI));
    if (!getPointsAndElements(atomset)) {
      Logger.error("Too many atoms for point group calculation");
      name = "point group not determined -- ac > " + maxAtoms
          + " -- select fewer atoms and try again.";
      return true;
    }
    getElementCounts();
    if (haveVibration) {
      P3d[] atomVibs = new P3d[points.length];
      for (int i = points.length; --i >= 0;) {
        atomVibs[i] = P3d.newP(points[i]);
        V3d v = ((Atom) points[i]).getVibrationVector();
        if (v != null)
          atomVibs[i].add(v);
      }
      points = atomVibs;
    }
    if (isEqual(pgLast))
      return false;
    try {

      findInversionCenter();

      if (isLinear(points)) {
        if (haveInversionCenter) {
          name = "D(infinity)h";
        } else {
          name = "C(infinity)v";
        }
        vTemp.sub2(points[1], points[0]);
        addAxis(c2, vTemp);
        principalAxis = axes[c2][0];
        if (haveInversionCenter) {
          axes[0] = new Operation[1];
          principalPlane = axes[0][nAxes[0]++] = new Operation(vTemp);
        }
        return true;
      }
      axes[0] = new Operation[15];
      int nPlanes = 0;
      findCAxes();
      nPlanes = findPlanes();
      findAdditionalAxes(nPlanes);

      /* flow chart contribution of Dean Johnston */

      int n = getHighestOrder();
      if (nAxes[c3] > 1) {
        // must be Ix, Ox, or Tx
        if (nAxes[c5] > 1) {
          if (haveInversionCenter) {
            name = "Ih";
          } else {
            name = "I";
          }
        } else if (nAxes[c4] > 1) {
          if (haveInversionCenter) {
            name = "Oh";
          } else {
            name = "O";
          }
        } else {
          if (nPlanes > 0) {
            if (haveInversionCenter) {
              name = "Th";
            } else {
              name = "Td";
            }
          } else {
            name = "T";
          }
        }
      } else {
        // Cs, Ci, C1
        if (n < 2) {
          if (nPlanes == 1) {
            name = "Cs";
            return true;
          }
          if (haveInversionCenter) {
            name = "Ci";
            return true;
          }
          name = "C1";
        } else if ((n % 2) == 1 && nAxes[c2] > 0
            || (n % 2) == 0 && nAxes[c2] > 1) {
          // Dnh, Dnd, Dn, S4

          // here based on the presence of C2 axes in any odd-order group
          // and more than one C2 if even order (since the one will be part of the 
          // principal axis

          principalAxis = setPrincipalAxis(n, nPlanes);
          if (nPlanes == 0) {
            if (n < firstProper) {
              name = "S" + n;
            } else {
              name = "D" + (n - firstProper);
            }
          } else {
            // highest axis may be S8, but this is really D4h/D4d
            if (n < firstProper)
              n = n / 2;
            else
              n -= firstProper;
            if (nPlanes == n) {
              name = "D" + n + "d";
            } else {
              name = "D" + n + "h";
            }
          }
        } else if (nPlanes == 0) {
          // Cn, S3, S6 
          principalAxis = axes[n][0];
          if (n < firstProper) {
            name = "S" + n;
          } else {
            name = "C" + (n - firstProper);
          }
        } else if (nPlanes == n - firstProper) {
          principalAxis = axes[n][0];
          name = "C" + nPlanes + "v";
        } else {
          principalAxis = axes[n < firstProper ? n + firstProper : n][0];
          principalPlane = axes[0][0];
          if (n < firstProper)
            n /= 2;
          else
            n -= firstProper;
          name = "C" + n + "h";
        }
      }
    } catch (Exception e) {
      name = "??";
    } finally {
      Logger.info("Point group found: " + name);
    }
    return true;
  }

  private Operation setPrincipalAxis(int n, int nPlanes) {
    Operation principalPlane = setPrincipalPlane(n, nPlanes);
    if (nPlanes == 0 && n < firstProper || nAxes[n] == 1) {
      if (nPlanes > 0 && n < firstProper)
        n = firstProper + n / 2;
        return axes[n][0];
    }
    // D2, D2d, D2h -- which c2 axis is it?
    if (principalPlane == null)
      return null;
    for (int i = 0; i < nAxes[c2]; i++)
      if (isParallel(principalPlane.normalOrAxis, axes[c2][i].normalOrAxis)) {
        if (i != 0) {
          Operation o = axes[c2][0];
          axes[c2][0] = axes[c2][i];
          axes[c2][i] = o;
        }
        return axes[c2][0];
      }
    return null;
  }

  private Operation setPrincipalPlane(int n, int nPlanes) {
    // principal plane is perpendicular to more than two other planes
    if (nPlanes == 1)
      return principalPlane = axes[0][0];
    if (nPlanes == 0 || nPlanes == n - firstProper)
      return null;
    for (int i = 0; i < nPlanes; i++)
      for (int j = 0, nPerp = 0; j < nPlanes; j++)
        if (isPerpendicular(axes[0][i].normalOrAxis, axes[0][j].normalOrAxis) && ++nPerp > 2) {
          if (i != 0) {
            Operation o = axes[0][0];
            axes[0][0] = axes[0][i];
            axes[0][i] = o;
          }
          return principalPlane = axes[0][0];
        }
    return null;
  }

  int maxAtoms = 250;

  private boolean getPointsAndElements(T3d[] atomset) {
    int ac = bsAtoms.cardinality();
    if (isAtoms && ac > maxAtoms)
      return false;
    points = new P3d[ac];
    elements = new int[ac];
    if (ac == 0)
      return true;

    // All Node points will be Point3fi, actually. But they might not be Atom.
    // It's not perfect.
    int atomIndexMax = 0;
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
      T3d p = atomset[i];
      if (p instanceof Node)
        atomIndexMax = Math.max(atomIndexMax, ((Point3fi) p).i);
    }
    atomMap = new int[atomIndexMax + 1];
    nAtoms = 0;
    boolean needCenter = (center == null);
    if (needCenter)
      center = new P3d();
    // we optionally include bonding information
    Bspt bspt = new Bspt(3, 0);
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms
        .nextSetBit(i + 1), nAtoms++) {
      T3d p = atomset[i];
      if (p instanceof Node) {
        int bondIndex = (localEnvOnly ? 1
            : 1 + Math.max(3, ((Node) p).getCovalentBondCount()));
        elements[nAtoms] = ((Node) p).getElementNumber() * bondIndex;
        atomMap[((Point3fi) p).i] = nAtoms + 1;
      } else {
        Point3fi newPt = new Point3fi();
        newPt.setT(p);
        newPt.i = -1 - nAtoms;
        if (p instanceof Point3fi)
          elements[nAtoms] = Math.max(0, ((Point3fi) p).sD);
        p = newPt;
      }
      bspt.addTuple(p);
      if (needCenter)
        center.add(p);
      points[nAtoms] = p;
    }
    iter = bspt.allocateCubeIterator();
    if (needCenter)
      center.scale(1d / nAtoms);
    for (int i = nAtoms; --i >= 0;) {
      double r2 = center.distanceSquared(points[i]);
      if (isAtoms && r2 < distanceTolerance2)
        centerAtomIndex = i;
      radius = Math.max(radius, r2);
    }
    radius =  Math.sqrt(radius);
    if (radius < 1.5d && distanceTolerance > 0.15d) {
      distanceTolerance = radius / 10;
      distanceTolerance2 = distanceTolerance * distanceTolerance;
      System.out
          .println("PointGroup calculation adjusting distanceTolerance to "
              + distanceTolerance);
    }
    return true;
  }

  private void findInversionCenter() {
    haveInversionCenter = checkOperation(null, center, -1);
    if (haveInversionCenter) {
      axes[1] = new Operation[1];
      axes[1][0] = new Operation();
    }
  }

  private boolean checkOperation(Qd q, T3d center, int iOrder) {
    P3d pt = new P3d();
    int nFound = 0;
    boolean isInversion = (iOrder < firstProper);

    out: for (int n = points.length, i = n; --i >= 0 && nFound < n;) {
      if (i == centerAtomIndex)
        continue;
      T3d a1 = points[i];
      int e1 = elements[i];
      if (q != null) {
        pt.sub2(a1, center);
        q.transform2(pt, pt).add(center);
      } else {
        pt.setT(a1);
      }
      if (isInversion) {
        // A trick here: rather than 
        // actually doing a rotation/reflection
        // we do a rotation INVERSION. This works
        // due to the symmetry of S2, S4, and S8
        // For S3 and S6, we play the trick of b
        // rotating as C6 and C3, respectively, 
        // THEN doing the rotation/inversion. 
        vTemp.sub2(center, pt);
        pt.scaleAdd2(2, vTemp, pt);
      }
      if ((q != null || isInversion)
          && pt.distanceSquared(a1) < distanceTolerance2) {
        nFound++;
        continue;
      }
      // did not find the point...
      iter.initialize(pt, distanceTolerance, false);
      while (iter.hasMoreElements()) {
        T3d a2 = iter.nextElement();
        if (a2 == a1)
          continue;
        int j = getPointIndex(((Point3fi) a2).i); // will be true atom index for an atom, not just in first molecule
        
        if (centerAtomIndex >= 0 && j == centerAtomIndex 
            || j >= elements.length
            || 
            elements[j] != e1 )
          continue;
        if (pt.distanceSquared(a2) < distanceTolerance2) {
          nFound++;
        //System.out.println("#pt=" + pt + " a2=" + a2 + " dist=" + pt.distanceSquared(a2));
        //System.out.println("draw pt" + i + " " + pt + " color red");
        //System.out.println("draw a" + i + " " + a2 + " color green");
          continue out;
        } 
//        System.out.println("none found for " + a1 + " "  +a2 + " " + pt);
      }
      return false;
    }
    return true;
  }

  private int getPointIndex(int j) {
    return (j < 0 ? -j : atomMap[j]) - 1;
  }

  private boolean isLinear(T3d[] atoms) {
    V3d v1 = null;
    if (atoms.length < 2)
      return false;
    for (int i = atoms.length; --i >= 0;) {
      if (i == centerAtomIndex)
        continue;
      if (v1 == null) {
        v1 = new V3d();
        v1.sub2(atoms[i], center);
        v1.normalize();
        vTemp.setT(v1);
        continue;
      }
      vTemp.sub2(atoms[i], center);
      vTemp.normalize();
      if (!isParallel(v1, vTemp))
        return false;
    }
    return true;
  }

  private boolean isParallel(V3d v1, V3d v2) {
    // note -- these MUST be unit vectors
    return (Math.abs(v1.dot(v2)) >= cosTolerance);
  }

  private boolean isPerpendicular(V3d v1, V3d v2) {
    // note -- these MUST be unit vectors
    return (Math.abs(v1.dot(v2)) <= 1 - cosTolerance);
  }

  int maxElement = 0;
  int[] eCounts;

  private void getElementCounts() {
    for (int i = points.length; --i >= 0;) {
      int e1 = elements[i];
      if (e1 > maxElement)
        maxElement = e1;
    }
    eCounts = new int[++maxElement];
    for (int i = points.length; --i >= 0;)
      eCounts[elements[i]]++; 
  }

  private int findCAxes() {
    V3d v1 = new V3d();
    V3d v2 = new V3d();
    V3d v3 = new V3d();

    // look for the proper and improper axes relating pairs of atoms

    for (int i = points.length; --i >= 0;) {
      if (i == centerAtomIndex)
        continue;
      T3d a1 = points[i];
      int e1 = elements[i];
      for (int j = points.length; --j > i;) {
        T3d a2 = points[j];
        if (elements[j] != e1)
          continue;

        // check if A - 0 - B is linear

        v1.sub2(a1, center);
        v2.sub2(a2, center);
        v1.normalize();
        v2.normalize();
        if (isParallel(v1, v2)) {
          getAllAxes(v1);
          continue;
        }

        // look for all axes to average position of A and B

        if (nAxes[c2] < axesMaxN[c2]) {
          v3.ave(a1, a2);
          v3.sub(center);
          getAllAxes(v3);
        }

        // look for the axis perpendicular to the A -- 0 -- B plane

        double order =  (2 * Math.PI / v1.angle(v2));
        int iOrder = (int) Math.floor(order + 0.01f);
        boolean isIntegerOrder = (order - iOrder <= 0.02f);
        if (!isIntegerOrder || (iOrder = iOrder + firstProper) >= maxAxis)
          continue;
        if (nAxes[iOrder] < axesMaxN[iOrder]) {
          v3.cross(v1, v2);
          checkAxisOrder(iOrder, v3, center);
        }
      }
    }

    // check all C2 axes for C3-related axes

    V3d[] vs = new V3d[nAxes[c2] * 2];
    for (int i = 0; i < vs.length; i++)
      vs[i] = new V3d();
    int n = 0;
    for (int i = 0; i < nAxes[c2]; i++) {
      vs[n++].setT(axes[c2][i].normalOrAxis);
      vs[n].setT(axes[c2][i].normalOrAxis);
      vs[n++].scale(-1);
    }
    for (int i = vs.length; --i >= 2;)
      for (int j = i; --j >= 1;)
        for (int k = j; --k >= 0;) {
          v3.add2(vs[i], vs[j]);
          v3.add(vs[k]);
          if (v3.length() < 1.0)
            continue;
          checkAxisOrder(c3, v3, center);
        }

    // Now check for triples of elements that will define
    // axes using the element with the smallest
    // number of atoms n, with n >= 3 
    // cross all triples of vectors looking for standard
    // principal axes quantities.

    // Also check for vectors from {0 0 0} to
    // the midpoint of each triple of atoms

    // get minimum element count > 2

    int nMin = Integer.MAX_VALUE;
    int iMin = -1;
    for (int i = 0; i < maxElement; i++) {
      if (eCounts[i] < nMin && eCounts[i] > 2) {
        nMin = eCounts[i];
        iMin = i;
      }
    }

    out: for (int i = 0; i < points.length - 2; i++)
      if (elements[i] == iMin)
        for (int j = i + 1; j < points.length - 1; j++)
          if (elements[j] == iMin)
            for (int k = j + 1; k < points.length; k++)
              if (elements[k] == iMin) {
                v1.sub2(points[i], points[j]);
                v2.sub2(points[i], points[k]);
                v1.normalize();
                v2.normalize();
                v3.cross(v1, v2);
                getAllAxes(v3);
//                checkAxisOrder(3, v3, center);
                v1.add2(points[i], points[j]);
                v1.add(points[k]);
                v1.normalize();
                if (!isParallel(v1, v3))
                  getAllAxes(v1);
                if (nAxes[c5] == axesMaxN[c5])
                  break out;
              }

    //check for C2 by looking for axes along element-based geometric centers

    vs = new V3d[maxElement];
    for (int i = points.length; --i >= 0;) {
      int e1 = elements[i];
      if (vs[e1] == null)
        vs[e1] = new V3d();
      else if (haveInversionCenter)
        continue;
      vs[e1].add(points[i]);
    }
    if (!haveInversionCenter)
      for (int i = 0; i < maxElement; i++)
        if (vs[i] != null)
          vs[i].scale(1d / eCounts[i]);

    // check for vectors from {0 0 0} to
    // the midpoint of each pair of atoms
    // within the same element if there is no inversion center,
    // otherwise, check for cross-product axis

    for (int i = 0; i < maxElement; i++)
      if (vs[i] != null)
        for (int j = 0; j < maxElement; j++) {
          if (i == j || vs[j] == null)
            continue;
          if (haveInversionCenter)
           v1.cross(vs[i], vs[j]);
          else
            v1.sub2(vs[i], vs[j]);
          checkAxisOrder(c2, v1, center);
          
        }

    return getHighestOrder();
  }

  private void getAllAxes(V3d v3) {
    for (int o = c2; o < maxAxis; o++)
      if (nAxes[o] < axesMaxN[o])
        checkAxisOrder(o, v3, center);
  }

  private int getHighestOrder() {
    int n = 0;
    // highest S
    for (n = firstProper; --n > 1 && nAxes[n] == 0;) {
    }
    // or highest C
    if (n > 1)
      return (n + firstProper < maxAxis && nAxes[n + firstProper] > 0 ? n + firstProper : n);
    for (n = maxAxis; --n > 1 && nAxes[n] == 0;) {
    }
    return n;
  }

  private boolean checkAxisOrder(int iOrder, V3d v, T3d center) {
    switch (iOrder) {
    case c8:
      if (nAxes[c3] > 0)
        return false;
      //$FALL-THROUGH$;
    case c6:
    case c4:
      if (nAxes[c5] > 0)
        return false;
      break;
    case c3:
      if (nAxes[c8] > 0)
        return false;
      break;
    case c5:
      if (nAxes[c4] > 0 || nAxes[c6] > 0 || nAxes[c8] > 0) 
        return false;
      break;
    case c2:
      break;
    }

    v.normalize();
    if (haveAxis(iOrder, v))
      return false;
    Qd q = getQuaternion(v, iOrder);
    if (!checkOperation(q, center, iOrder))
      return false;
    addAxis(iOrder, v);
    // check for Sn:
    switch (iOrder) {
    case c2:
      checkAxisOrder(s4, v, center);//D2d, D4h, D6d
      break;
    case c3:
      checkAxisOrder(s3, v, center);//C3h, D3h
      if (haveInversionCenter)
        addAxis(s6, v);
      break;
    case c4:
      addAxis(c2, v);
      checkAxisOrder(s4, v, center);//D2d, D4h, D6d
      checkAxisOrder(s8, v, center);//D4d
      break;
    case c5:
      checkAxisOrder(s5, v, center); //C5h, D5h
      if (haveInversionCenter)
        addAxis(s10, v);
      break;
    case c6:
      addAxis(c2, v);
      addAxis(c3, v);
      checkAxisOrder(s3, v, center);//C6h, D6h
      checkAxisOrder(s6, v, center);//C6h, D6h
      checkAxisOrder(s12, v, center);//D6d
      break;
    case c8:
      //note -- D8d would have a S16 axis. This will not be found.
      addAxis(c2, v);
      addAxis(c4, v);
      break;
    }
    return true;
  }

  private void addAxis(int iOrder, V3d v) {
    if (haveAxis(iOrder, v))
      return;
    if (axes[iOrder] == null)
      axes[iOrder] = new Operation[axesMaxN[iOrder]];
    axes[iOrder][nAxes[iOrder]++] = new Operation(v, iOrder);
  }

  private boolean haveAxis(int iOrder, V3d v) {
    if (nAxes[iOrder] == axesMaxN[iOrder]) {
      return true;
    }
    if (nAxes[iOrder] > 0)
      for (int i = nAxes[iOrder]; --i >= 0;) {
        if (isParallel(v, axes[iOrder][i].normalOrAxis))
          return true;
      }
    return false;
  }

  private int findPlanes() {
    P3d pt = new P3d();
    V3d v1 = new V3d();
    V3d v2 = new V3d();
    V3d v3 = new V3d();
    int nPlanes = 0;
    boolean haveAxes = (getHighestOrder() > 1);
    for (int i = points.length; --i >= 0;) {
      if (i == centerAtomIndex)
        continue;
      T3d a1 = points[i];
      int e1 = elements[i];
      for (int j = points.length; --j > i;) {
        if (haveAxes && elements[j] != e1)
          continue;

        // plane are treated as S2 axes here

        // first, check planes through two atoms and the center
        // or perpendicular to a linear A -- 0 -- B set

        T3d a2 = points[j];
        pt.add2(a1, a2);
        pt.scale(0.5d);
        v1.sub2(a1, center);
        v2.sub2(a2, center);
        if (!isParallel(v1, v2)) {
          v3.cross(v1, v2);
          v3.normalize();
          nPlanes = getPlane(v3);
        }

        // second, look for planes perpendicular to the A -- B line

        v3.sub2(a2, a1);
        v3.normalize();
        nPlanes = getPlane(v3);
        if (nPlanes == axesMaxN[0])
          return nPlanes;
      }
    }

    // also look for planes normal to any C axis
    if (haveAxes)
      for (int i = c2; i < maxAxis; i++)
        for (int j = 0; j < nAxes[i]; j++)
          nPlanes = getPlane(axes[i][j].normalOrAxis);
    return nPlanes;
  }

  private int getPlane(V3d v3) {
    if (!haveAxis(0, v3)
        && checkOperation(Qd.newVA(v3, 180), center,
            -1))
      axes[0][nAxes[0]++] = new Operation(v3);
    return nAxes[0];
  }

  private void findAdditionalAxes(int nPlanes) {

    Operation[] planes = axes[0];
    int Cn = 0;
    if (nPlanes > 1
        && ((Cn = nPlanes + firstProper) < maxAxis) 
        && nAxes[Cn] == 0) {
      // cross pairs of plane normals. We don't need many.
      vTemp.cross(planes[0].normalOrAxis, planes[1].normalOrAxis);
      if (!checkAxisOrder(Cn, vTemp, center)
          && nPlanes > 2) {
        vTemp.cross(planes[1].normalOrAxis, planes[2].normalOrAxis);
        checkAxisOrder(Cn - 1, vTemp, center);
      }
    }
    if (nAxes[c2] == 0 && nPlanes > 2) {
      // check for C2 axis relating 
      for (int i = 0; i < nPlanes - 1; i++) {
        for (int j = i + 1; j < nPlanes; j++) {
          vTemp.add2(planes[1].normalOrAxis, planes[2].normalOrAxis);
          //if (
          checkAxisOrder(c2, vTemp, center);
          //)
            //Logger.error("found a C2 axis by adding plane normals");
        }
      }
    }
  }

  final static int OPERATION_PLANE = 0;
  final static int OPERATION_PROPER_AXIS = 1;
  final static int OPERATION_IMPROPER_AXIS = 2;
  final static int OPERATION_INVERSION_CENTER = 3;

  final static String[] typeNames = { "plane", "proper axis", "improper axis",
      "center of inversion" };

  final static M3d mInv = M3d.newA9(new double[] {
      -1, 0, 0, 
      0, -1, 0,
      0, 0, -1
      });

  static Qd getQuaternion(V3d v, int iOrder) {
    return Qd.newVA(v, (iOrder < firstProper ? 180 : 0) + (iOrder == 0 ? 0 : 360 / (iOrder % firstProper)));
  }

  int nOps = 0;
  private class Operation {
    int type;
    int order;
    int index;
    V3d normalOrAxis;
    private int typeOrder;

    Operation() {
      index = ++nOps;
      type = OPERATION_INVERSION_CENTER;
      order = 1;
      typeOrder = 1;
      if (Logger.debugging)
        Logger.debug("new operation -- " + typeNames[type]);
    }

    Operation(V3d v, int i) {
      index = ++nOps;
      type = (i < firstProper ? OPERATION_IMPROPER_AXIS : OPERATION_PROPER_AXIS);
      typeOrder = i;
      order = i % firstProper;
      normalOrAxis = Qd.newVA(v, 180).getNormal();
      if (Logger.debugging)
        Logger.debug("new operation -- " + (order == i ? "S" : "C") + order + " "
            + normalOrAxis);
    }

    Operation(V3d v) {
      if (v == null)
        return;
      index = ++nOps;
      type = OPERATION_PLANE;
      normalOrAxis = Qd.newVA(v, 180).getNormal();
      if (Logger.debugging)
        Logger.debug("new operation -- plane " + normalOrAxis);
    }

    String getLabel() {
      switch (type) {
      case OPERATION_PLANE:
        return "Cs";
      case OPERATION_IMPROPER_AXIS:
        return "S" + order;
      default:
        return "C" + order;
      }
    }

    M3d mat;
    
    public M3d getM3() {
      if (mat != null)
        return mat;
      M3d m = M3d.newM3(getQuaternion(normalOrAxis, typeOrder).getMatrix());
      if (type == OPERATION_PLANE || type == OPERATION_IMPROPER_AXIS)
        m.mul(mInv);
      m.clean();
      return mat = m;
    }

  }

  Object getInfo(int modelIndex, String drawID, boolean asInfo, String type,
                 int index, double scaleFactor) {
    boolean asDraw = (drawID != null);
    info = (asInfo ? new Hashtable<String, Object>() : null);
    V3d v = new V3d();
    Operation op;
    if (scaleFactor == 0)
      scaleFactor = 1;
    scale = scaleFactor;
    int[][] nType = new int[4][2];
    for (int i = 1; i < maxAxis; i++)
      for (int j = nAxes[i]; --j >= 0;)
        nType[axes[i][j].type][0]++;
    SB sb = new SB()
      .append("# ").appendI(nAtoms).append(" atoms\n");
    if (asDraw) {
      drawID = "draw " + drawID;
      boolean haveType = (type != null && type.length() > 0);
      drawType = type = (haveType ? type : "");
      drawIndex = index;
      boolean anyProperAxis = (type.equalsIgnoreCase("Cn"));
      boolean anyImproperAxis = (type.equalsIgnoreCase("Sn"));
      sb.append("set perspectivedepth off;\n");
      String m = "_" + modelIndex + "_";
      if (!haveType)
        sb.append(drawID + "pg0").append(m).append("* delete;draw pgva").append(m
           ).append("* delete;draw pgvp").append(m).append("* delete;");
      if (!haveType || type.equalsIgnoreCase("Ci"))
        sb.append(drawID + "pg0").append(m).append(
            haveInversionCenter ? "inv " : " ").append(
            Escape.eP(center)).append(haveInversionCenter ? "\"i\";\n" : ";\n");
      double offset = 0.1d;
      for (int i = 2; i < maxAxis; i++) {
        if (i == firstProper)
          offset = 0.1d;
        if (nAxes[i] == 0)
          continue;
        String label = axes[i][0].getLabel();
        offset += 0.25d;
        double scale = scaleFactor * radius + offset;
        boolean isProper = (i >= firstProper);
        if (!haveType || type.equalsIgnoreCase(label) || anyProperAxis
            && isProper || anyImproperAxis && !isProper)
          for (int j = 0; j < nAxes[i]; j++) {
            if (index > 0 && j + 1 != index)
              continue;
            op = axes[i][j];
            v.add2(op.normalOrAxis, center);
            if (op.type == OPERATION_IMPROPER_AXIS)
              scale = -scale;
            sb.append(drawID + "pgva").append(m).append(label).append("_").appendI(
                j + 1).append(" width 0.05 scale ").appendD(scale).append(" ").append(
                Escape.eP(v));
            v.scaleAdd2(-2, op.normalOrAxis, v);
            boolean isPA = (principalAxis != null && op.index == principalAxis.index);
            sb.append(Escape.eP(v)).append(
                "\"").append(label).append(isPA ? "*" : "").append("\" color ").append(
                isPA ? "red" : op.type == OPERATION_IMPROPER_AXIS ? "blue"
                    : "orange").append(";\n");
          }
      }
      if (!haveType || type.equalsIgnoreCase("Cs"))
        for (int j = 0; j < nAxes[0]; j++) {
          if (index > 0 && j + 1 != index)
            continue;
          op = axes[0][j];
          sb.append(drawID + "pgvp").append(m).appendI(j + 1).append(
              "disk scale ").appendD(scaleFactor * radius * 2).append(" CIRCLE PLANE ")
              .append(Escape.eP(center));
          v.add2(op.normalOrAxis, center);
          sb.append(Escape.eP(v)).append(" color translucent yellow;\n");
          v.add2(op.normalOrAxis, center);
          sb.append(drawID + "pgvp").append(m).appendI(j + 1).append(
              "ring width 0.05 scale ").appendD(scaleFactor * radius * 2).append(" arc ")
              .append(Escape.eP(v));
          v.scaleAdd2(-2, op.normalOrAxis, v);
          sb.append(Escape.eP(v));
          v.add3(0.011f,  0.012f,  0.013f);
          sb.append(Escape.eP(v))
              .append("{0 360 0.5} color ")
              .append(
                  principalPlane != null && op.index == principalPlane.index ? "red"
                      : "blue").append(";\n");
        }
      sb.append("# name=").append(name);
      sb.append(", nCi=").appendI(haveInversionCenter ? 1 : 0);
      sb.append(", nCs=").appendI(nAxes[OPERATION_PLANE]);
      sb.append(", nCn=").appendI(nType[OPERATION_PROPER_AXIS][0]);
      sb.append(", nSn=").appendI(nType[OPERATION_IMPROPER_AXIS][0]);
      sb.append(": ");
      for (int i = maxAxis; --i >= 2;)
        if (nAxes[i] > 0) {
          sb.append(" n").append(i < firstProper ? "S" : "C").appendI(i % firstProper);
          sb.append("=").appendI(nAxes[i]);
        }
      sb.append(";\n");
      sb.append("print '" + name + "';\n");
      drawInfo = sb.toString();
      if (Logger.debugging)
        Logger.info(drawInfo);
      return drawInfo;
    }
    int n = 0;
    int nTotal = 1;
    int nElements = 0; // planes Cs
    
    String ctype = (haveInversionCenter ? "Ci" : "center");
    if (haveInversionCenter) {
      nTotal++;
      nElements++;
    }
    if (asInfo) {
      if (center != null) {
      info.put(ctype, center);
      if (haveInversionCenter)
        info.put("center", center);
      info.put(ctype, center);
      }
    } else {
      sb.append("\n\n").append(name).append("\t").append(ctype).append("\t").append(Escape.eP(center));
    }
    for (int i = maxAxis; --i >= 0;) {
      if (nAxes[i] > 0) {
        // includes planes
        n = nUnique[i];
        String label = axes[i][0].getLabel();
        if (asInfo)
          info.put("n" + label, Integer.valueOf(nAxes[i]));
        else
          sb.append("\n\n").append(name).append("\tn").append(label).append("\t").appendI(nAxes[i]).append("\t").appendI(n);
        n *= nAxes[i];
        nTotal += n;
        nElements += nAxes[i];
        nType[axes[i][0].type][1] += n;
        Lst<V3d> vinfo = (asInfo ? new  Lst<V3d>() : null);
        Lst<M3d> minfo = (asInfo ? new  Lst<M3d>() : null);
        for (int j = 0; j < nAxes[i]; j++) {
          //axes[i][j].typeIndex = j + 1;
          Operation aop = axes[i][j];
          if (asInfo) {
            vinfo.addLast(aop.normalOrAxis);
            minfo.addLast(aop.getM3());
          } else {
            sb.append("\n").append(name).append("\t").append(label).append("_").appendI(j + 1).append("\t"
                ).appendO(aop.normalOrAxis);
          }
        }
        if (asInfo) {
          info.put(label, vinfo);
          info.put(label + "_m", minfo);
        }
      }
    }
    
    if (!asInfo) {
      sb.append("\n");
      sb.append("\n").append(name).append("\ttype\tnElements\tnUnique");
      sb.append("\n").append(name).append("\tE\t  1\t  1");

      n = (haveInversionCenter ? 1 : 0);
      sb.append("\n").append(name).append("\tCi\t  ").appendI(n).append("\t  ").appendI(n);

      sb.append("\n").append(name).append("\tCs\t");
      PT.rightJustify(sb, "    ", nAxes[0] + "\t");
      PT.rightJustify(sb, "    ", nAxes[0] + "\n");

      sb.append(name).append("\tCn\t");
      PT.rightJustify(sb, "    ", nType[OPERATION_PROPER_AXIS][0] + "\t");
      PT.rightJustify(sb, "    ", nType[OPERATION_PROPER_AXIS][1] + "\n");

      sb.append(name).append("\tSn\t");
      PT.rightJustify(sb, "    ", nType[OPERATION_IMPROPER_AXIS][0] + "\t");
      PT.rightJustify(sb, "    ", nType[OPERATION_IMPROPER_AXIS][1] + "\n");

      sb.append(name).append("\t\tTOTAL\t");
      PT.rightJustify(sb, "    ", nTotal + "\n");
      return (textInfo = sb.toString());
    }
    info.put("name", name);
    info.put("nAtoms", Integer.valueOf(nAtoms));
    info.put("nTotal", Integer.valueOf(nTotal));
    info.put("nElements", Integer.valueOf(nElements));
    info.put("nCi", Integer.valueOf(haveInversionCenter ? 1 : 0));
    if (haveInversionCenter)
      info.put("Ci_m", M3d.newM3(mInv));
    info.put("nCs", Integer.valueOf(nAxes[0]));
    info.put("nCn", Integer.valueOf(nType[OPERATION_PROPER_AXIS][0]));
    info.put("nSn", Integer.valueOf(nType[OPERATION_IMPROPER_AXIS][0]));
    info.put("distanceTolerance", Double.valueOf(distanceTolerance));
    info.put("linearTolerance", Double.valueOf(linearTolerance));
    info.put("points", points);
    info.put("detail", sb.toString().replace('\n', ';'));
    if (principalAxis != null && principalAxis.index > 0)
      info.put("principalAxis", principalAxis.normalOrAxis);
    if (principalPlane != null && principalPlane.index > 0)
      info.put("principalPlane", principalPlane.normalOrAxis);
    return info;
  }

  boolean isDrawType(String type, int index, double scale) {
    return (drawInfo != null && drawType.equals(type == null ? "" : type) 
        && drawIndex == index && this.scale  == scale);
  }
  
}
