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

import org.jmol.bspt.Bspt;
import org.jmol.bspt.CubeIterator;
import org.jmol.modelset.Atom;
import org.jmol.util.BSUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Node;
import org.jmol.util.Point3fi;
import org.jmol.util.Vibration;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.M3d;
import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.Qd;
import javajs.util.SB;
import javajs.util.T3d;
import javajs.util.V3d;

/**
 * 
 * A class to work with point group calculations.
 * 
 * Note that this check only goes up to C8. This was an arbitrary decision that
 * could be expanded upon. Spin space groups, for example, are indexed up to
 * C42.
 * 
 * Preliminary version was from BCCE20 meeting 2008
 * 
 * Many thanks to Sean Johnston for his all-night session with me!
 * 
 * Bob Hanson 7/2008
 * 
 * 2025.12.01 BH Adapted for very high symmetry in relation to spin space groups
 * by considering the limited possibilities for numbers of C2 axes and planes.
 * 
 * NEVER ACCESS THESE METHODS DIRECTLY! ONLY THROUGH CLASS Symmetry
 * 
 */

class PointGroup {

  private Lst<Operation> operations;

  private class Operation {
    private Operator operator;
    String drawID;
    String drawStr;
    boolean isTrivial;
    String typeName;
    int type;

    Operation(Operator o) {
      operator = o;
      type = (o == null ? OPERATION_INVERSION_CENTER : o.type);
      typeName = (o == null ? "Ci" : o.schName);
      isTrivial = checkTrivial();
    }

    private boolean checkTrivial() {
      double tol = distanceTolerance;
      for (int i = points.length; --i >= 0;) {
        double d;
        switch (type) {
        case OPERATION_PLANE:
        case OPERATION_IMPROPER_AXIS:
        case OPERATION_PROPER_AXIS:
          d = operator.distance(points[i]);
          break;
        default:
        case OPERATION_INVERSION_CENTER:
          d = points[i].length();
          break;
        }
        if (d > tol) {
          return false;
        }
      }
      System.out.println(this + " is trivial");
      return true;
    }

    @Override
    public String toString() {
      return "[op " + typeName + " " + drawID + " " + isTrivial + "]";
    }
  }

  Operator newInversionCenter(int index) {
    return new Operator(index, null, -1);
  }

  Operator newPlane(int index, V3d v) {
    return new Operator(index, v, -1);
  }

  Operator newAxis(int index, V3d v, int arrayIndex) {
    return new Operator(index, v, arrayIndex);
  }

  class Operator {
    int type;
    int index;
    V3d normalOrAxis;
    private M3d mat;

    final int order;
    private final int axisArrayIndex;

    String schName;
    private Lst<String> uvws;
    private Lst<M3d> uniqueMats;
    private Lst<M3d> mats;
    public Operation operation;
    private Lst<String> uniqueUVWs;

    /**
     * Constructor for proper and improper axes.
     * 
     * @param index
     * @param v
     * @param arrayIndex
     */
    protected Operator(int index, V3d v, int arrayIndex) {
      this.index = index;
      if (v == null) {
        type = OPERATION_INVERSION_CENTER;
        axisArrayIndex = ci;
        order = 2;
        schName = "Ci";
        mat = mInv;
      } else {
        normalOrAxis = Qd.newVA(v, 180).getNormal();
        if (arrayIndex == -1) {
          type = OPERATION_PLANE;
          axisArrayIndex = cs;
          order = 2;
          schName = "Cs";
        } else {
          type = (arrayIndex < firstProper ? OPERATION_IMPROPER_AXIS
              : OPERATION_PROPER_AXIS);
          axisArrayIndex = arrayIndex;
          order = arrayIndex % firstProper;
          schName = (type == OPERATION_IMPROPER_AXIS ? "S" : "C") + order;
        }
      }
      if (Logger.debugging)
        Logger.debug("new operation -- " + index + " " + schName
            + (normalOrAxis == null ? "" : " " + normalOrAxis));
    }

    M3d getM3() {
      if (mat != null)
        return mat;
      M3d m = M3d
          .newM3(getQuaternion(normalOrAxis, axisArrayIndex).getMatrix());
      if (type == OPERATION_PLANE || type == OPERATION_IMPROPER_AXIS)
        m.mul(mInv);
      m.clean();
      return mat = m;
    }

    double distance(T3d pt) {
      P3d p = P3d.newP(pt);
      getM3().rotate(p);
      return p.distance(pt);
    }

    Lst<M3d> getMatrices(boolean uniqueOnly) {
      if (uniqueOnly && uniqueMats != null)
        return uniqueMats;
      if (!uniqueOnly && mats != null)
        return mats;
      Lst<M3d> matrices = new Lst<>();
      M3d m = new M3d();
      m.m00 = m.m11 = m.m22 = 1;
      // Example: S10
      //
      // 1/10*,2/10,3/10*,4/10,5/10*,6/10,7/10*,8/10,9/10*  
      // only the * are unique; all others are covered by 
      // lower-symmetry elements already
      // S10(2) == C5(1)
      // S10(4) == C5(2)
      // S10(6) == C5(3) == C5(-2)
      // S10(8) == C5(4) == C5(-1)
      // We only need to go to order/2.
      // S12:
      // 1/12*,2/12,3/12,4/12,5/12*
      //
      // S9:
      // 1/9*,2/9*,3/9,4/9*
      BS bs = (uniqueOnly ? getUniqueFractions(order) : null);
      for (int i = 1; i < order; i++) {
        m.mul(getM3());
        if (!uniqueOnly || bs.get(i))
          matrices.addLast(M3d.newM3(m));
      }
      if (uniqueOnly)
        uniqueMats = matrices;
      else
        mats = matrices;
      return matrices;
    }

    @Override
    public String toString() {
      return schName + " " + normalOrAxis;
    }

    public void setInfo(Lst<V3d> vinfo, Lst<M3d> minfo, Map<String, Object> e) {
      if (vinfo != null) {
        vinfo.addLast(normalOrAxis);
        minfo.addLast(getM3());
      }
      e.put("order", Integer.valueOf(order));
      e.put("typeSch", schName);
      e.put("typeHM", getHMfromSFName(schName));
      if (normalOrAxis != null)
        e.put("direction", normalOrAxis);
      Lst<M3d> mats = getMatrices(true);
      e.put("uniqueOperations", mats);
      e.put("uniqueOperationUVWs", getUVWs(mats, true));
      if (mats.size() != order - 1)
        e.put("matrixIndices", bsUnique.get(Integer.valueOf(order)));
      mats = getMatrices(false);
      e.put("operations", mats);
      e.put("operationUVWs", getUVWs(mats, false));
    }

    private Lst<String> getUVWs(Lst<M3d> mats, boolean isUnique) {
      Lst<String> uvws = (isUnique ? this.uniqueUVWs : this.uvws); 
          if (uvws != null)
        return uvws;
      Lst<String> list = new Lst<>();
      for (int i = 0; i < mats.size(); i++) {
        list.addLast((String) SymmetryOperation.staticConvertOperation(null, mats.get(i), "uvw"));
      }
      if (isUnique)
        uniqueUVWs = list;
      else
        uvws = list;
      return list;
    }

    public boolean isUVW(String uvw) {
      Lst<String> uvws = getUVWs(getMatrices(true), true);
      for (int i = 0, n = uvws.size(); i < n; i++) {
        if (uvws.get(i).equals(uvw))
          return true;
      }
      return false;
    }

    // utilities

  }

  final static Map<Integer, BS> bsUnique = new Hashtable<>();

  protected static BS getUniqueFractions(int order) {
    BS bs = bsUnique.get(Integer.valueOf(order));
    if (bs != null)
      return bs;
    bs = BSUtil.newBitSet2(1, order);
    int n = order / 2;
    for (int i = 1; i <= n; i++) {
      // for C10, 1-5 -- using 2 and 5
      // removing 2 4 6 8
      // removing    5
      // leaving 1 3 7 9 (S1 S-1 S3 S-3)

      // for C16, 1-8 -- using 2,(4,8)
      // for C24, 1-12 -- using 2,3,(4,6,8)
      // really we just need the lowest common denominators
      int f = order / i;
      if (f * i != order || !bs.get(f))
        continue;
      // for 24: 2,3,4,6,8
      for (int j = f; j <= n; j += f) {
        // 2: 2,4,6,8,10,12,14,16,18,20,22 cleared
        // 3: 3,6,9,12,15,18,21

        // 4: 4,8,12,16,20 (unnec because we already have removed these)
        // 6: 6,12,18 (unnec)
        // 8: 8,16 (unnec)
        // leaving 1/24,5/24,7/24,9/24,11/24,13/24,15/24,17/24,19/24,23/24
        bs.clear(j);
        bs.clear(order - j);
      }
    }
    bsUnique.put(Integer.valueOf(order), bs);
    return bs;
  }

  final static int OPERATION_PLANE = 0;
  final static int OPERATION_PROPER_AXIS = 1;
  final static int OPERATION_IMPROPER_AXIS = 2;
  final static int OPERATION_INVERSION_CENTER = 3;

  final static String[] typeNames = { "plane", "proper axis", "improper axis",
      "center of inversion" };

  final static M3d mInv = M3d
      .newA9(new double[] { -1, 0, 0, 0, -1, 0, 0, 0, -1 });

  private final static int[] axesMaxN = new int[] { //
      49, // cs up to D48h
      0, // n/a 
      0, // not used -- would be S2 (inversion)
      1, // S3
      3, // S4
      1, // S5
      10, // S6
      1, // S7
      1, // S8
      0, // n/a
      6, // S10
      0, // n/a 
      1, // S12
      0, // n/a 
      1, // S14
      0, // n/a 
      1, // S16
      0, // n/a
      0, // n/a
      0, // n/a
      0, // n/a firstProper = 20
      0, // n/a 
      49, // C2 up to C48  
      10, // C3 
      6, // C4
      6, // C5
      10, // C6
      1, // C7
      1, // C8
  };

  /**
   * counts of operations that are not included in lower-symmetry sets
   */
  private final static int[] nUnique = new int[] { 1, // used for plane count
      0, // n/a 
      0, // not used -- would be S2 (inversion)
      2, // S3
      2, // S4
      4, // S5
      2, // S6
      1, // S7
      1, // S8
      0, // n/a
      6, // S10
      0, // n/a 
      1, // S12
      0, // n/a 
      1, // S14
      0, // n/a 
      1, // S16
      0, // n/a
      0, // n/a
      0, // n/a
      0, // n/a firstProper = 20
      0, // n/a 
      1, // C2 
      2, // C3 C3(1),C3(2) 
      2, // C4 C4(1),C4(3)
      4, // C5 C5(1),C5(2),C5(3),C5(4)
      2, // C6 C6(1),C6(5)
      6, // C7 C7(1),C7(2),C7(3),C7(4),C7(5),C7(6)
      4, // C8 C8(1),C8(3),C8(5),C8(7)
  };

  private final static int cs = 0;
  private final static int ci = 1;
  private final static int s3 = 3;
  private final static int s4 = 4;
  private final static int s5 = 5;
  private final static int s6 = 6;
  private final static int s7 = 6;
  private final static int s8 = 8;
  private final static int s10 = 10;
  private final static int s12 = 12;
  private final static int s14 = 14;
  private final static int s16 = 16;
  private final static int firstProper = 20;
  private final static int c2 = firstProper + 2;
  private final static int c3 = firstProper + 3;
  private final static int c4 = firstProper + 4;
  private final static int c5 = firstProper + 5;
  private final static int c6 = firstProper + 6;
  private final static int c7 = firstProper + 7;
  private final static int c8 = firstProper + 8;
  private final static int maxAxis = axesMaxN.length;

  private int maxAtoms = 250;

  private int maxElement = 0;
  private int[] eCounts;

  private int nOps = 0;

  private boolean isAtoms;
  private CubeIterator iter;
  private String drawType = "";
  private int drawIndex;
  private double scale = Double.NaN;
  private int[] nAxes = new int[maxAxis];
  private Operator[][] axes = new Operator[maxAxis][];
  private int nAtoms;
  private double radius;
  protected double distanceTolerance = 0.25d; // making this just a bit more generous
  private double distanceTolerance2;
  private double linearTolerance = 8d;
  private double cosTolerance = 0.99d; // 8 degrees
  private String name = "C_1?";
  private Operator principalAxis;
  private Operator principalPlane;

  private Lst<Operator> highOperations;

  // outputs:
  private String drawInfo;
  private Map<String, Object> info;
  private String textInfo;

  private final static int CONVENTION_SCHOENFLIES = 0;
  private final static int CONVENTION_HERMANN_MAUGUIN = 1;

  private int convention = CONVENTION_SCHOENFLIES;

  private final V3d vTemp = new V3d();
  private int centerAtomIndex = -1;
  private boolean haveInversionCenter;

  private T3d center;

  protected T3d[] points;
  private int[] elements;
  private int[] atomMap;

  private BS bsAtoms;

  private boolean haveVibration;

  private boolean localEnvOnly;

  private boolean isLinear;

  private double sppa;
  private boolean isSpinGroup;
  private int highestOrder;
  private int modelIndex;
  private String drawID;
  private String type;
  private int index;
  private double scaleFactor;

  /**
   * Determine the point group of a set of points or atoms, allowing
   * additionally for considering the point group of vibrational modes.
   * 
   * The two parameters used are "distanceTolerance" and "linearTolerance"
   * 
   * "distanceTolerance is the distance an atom must be within relative to its
   * symmetry-projected idealized position.
   * 
   * "linearTolerance" has dimension degrees and sets the maximum deviation that
   * two potential symmetry axes can have to be considered "colinear" or
   * "perpendicular" in the final symmetry model. Its default is 8 deg.
   * 
   * 
   * @param pgLast
   *        helpful to speed checking; may be null
   * @param center
   *        known center atom; may be null
   * @param atomset
   *        the set of points or atoms to consider
   * @param bsAtoms
   *        possibly some subset of atomset
   * @param haveVibration
   * @param distanceTolerance
   *        atom-position tolerance
   * @param linearTolerance
   *        symmetry-axis direction tolerance
   * @param maxAtoms
   * @param localEnvOnly
   *        set false to additionally consider valence (number of bonds) of
   *        atoms
   * @param isHM
   * @param sppa
   * 
   * @return a PointGroup
   */

  static PointGroup getPointGroup(PointGroup pgLast, T3d center, T3d[] atomset,
                                  BS bsAtoms, boolean haveVibration,
                                  double distanceTolerance,
                                  double linearTolerance, int maxAtoms,
                                  boolean localEnvOnly, boolean isHM,
                                  double sppa) {
    PointGroup pg = new PointGroup(isHM);
    
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
    pg.sppa = sppa;
    if (Logger.debugging)
      pgLast = null;
    return (pg.set(pgLast, atomset) ? pg : pgLast);
  }

  private PointGroup(boolean isHM) {
    convention = (isHM ? CONVENTION_HERMANN_MAUGUIN : CONVENTION_SCHOENFLIES);
  }

  private boolean set(PointGroup pgLast, T3d[] atomset) {
    cosTolerance = (Math.cos(linearTolerance / 180 * Math.PI));
    if (!getPointsAndElements(atomset)) {
      Logger.error("Too many atoms for point group calculation");
      name = "point group not determined -- ac > " + maxAtoms
          + " -- select fewer atoms and try again.";
      return true;
    }
    getElementCounts();
    P3d[] atomVibs = new P3d[points.length];
    for (int i = 0; i < points.length; i++) {
      atomVibs[i] = P3d.newP(points[i]);
      Vibration v = ((Atom) points[i]).getVibrationVector();
      if (v != null) {
        if (v.isFrom000) {
          isSpinGroup = true;
          // just continue with these atoms
          atomVibs = null;
          haveVibration = true;
          break;
        } else if (!haveVibration) {
          break;
        }
        atomVibs[i].add(v);
      }
    }
    if (haveVibration && atomVibs != null)
      points = atomVibs;

    if (isEqual(pgLast))
      return false;
    try {
      findInversionCenter();
      isLinear = isLinear(points);
      if (isLinear) {
        if (haveInversionCenter) {
          name = "D\u221eh";
        } else {
          name = "C\u221ev";
        }
        vTemp.sub2(points[1], points[0]);
        addAxis(c2, vTemp);
        principalAxis = axes[c2][0];
        if (haveInversionCenter) {
          axes[cs] = new Operator[] {
              principalPlane = newPlane(++nOps, vTemp) };
          nAxes[cs] = 1;
        }
        return true;
      }
      axes[cs] = new Operator[axesMaxN[cs]];
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
        int n2 = nAxes[c2];
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
        } else if ((n % 2) == 1 && n2 > 0 || (n % 2) == 0 && n2 > 1) {
          // Dnh, Dnd, Dn, S4

          // here based on the presence of C2 axes in any odd-order group
          // and more than one C2 if even order (since the one will be part of the 
          // principal axis

          principalAxis = setPrincipalAxis(n, nPlanes);
          if (nPlanes == 0) {
            // Sn or Dn
            if (n < firstProper) {
              name = "S" + n;
            } else {
              name = "D" + (n - firstProper);
            }
          } else {
            // Dnd or Dnh
            int arrayIndexTop = n;
            if (n < firstProper) {
              // has S8, ...
              n = n / 2;
            } else {
              n -= firstProper;
            }
            if (n2 > n + 1) {
              addHighOperations(n2, nPlanes, arrayIndexTop, n);
              // we missed some higher business
              if (principalPlane == null) {
                // Dnd
                n = nPlanes;
              } else {
                n = nPlanes - 1;
              }
            } else {
            }
            if (nPlanes == n) {
              name = "D" + n + "d";
            } else {
              name = "D" + n + "h";
            }
          }
        } else if (nPlanes == 0) {
          // Cn, S3, S6 
          // we cannot find very high order axes
          // if there are no other elements. 
          principalAxis = axes[n][0];
          if (n < firstProper) {
            name = "S" + n;
          } else {
            name = "C" + (n - firstProper);
          }
        } else {
          // Cnv or Cnh
          // C4h will have an S8 axis
          if (nPlanes > 1) {
            principalAxis = axes[n][0];
            name = "C" + nPlanes + "v";
          } else {
            principalPlane = axes[cs][0];
            principalAxis = axes[n < firstProper ? n + firstProper : n][0];
            // TODO here we are stuck if order is very high
            if (n < firstProper) {
              n /= 2;
            } else {
              n -= firstProper;
            }
            name = "C" + n + "h";
          }
        }
      }
    } catch (Exception e) {
      name = "??";
    } finally {
      Logger.info("Point group found: " + name);
    }
    return true;
  }

  /**
   * @param n2  
   * @param nPlanes 
   * @param arrayIndexTop 
   * @param nTop 
   */
  private void addHighOperations(int n2, int nPlanes, int arrayIndexTop,
                                 int nTop) {
    // TODO -- add more operations
    // C20, S20 need to add operations
    // C20 --> C10, C5, and all operators
    // inbetween
    boolean isS = (arrayIndexTop < firstProper);
    if (isS) {

    }
  }

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
        Point3fi newPt = Point3fi.newPF(p, -1 - nAtoms);
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
    radius = Math.sqrt(radius);
    if (radius > 90) {
      // plot spin
      distanceTolerance = 0.3;
    }
    if (radius > 90 || radius < 1.5d && distanceTolerance > 0.15d) {
      distanceTolerance = radius / 10;
      distanceTolerance2 = distanceTolerance * distanceTolerance;
      System.out
          .println("PointGroup calculation adjusting distanceTolerance to "
              + distanceTolerance);
    }
    return true;
  }

  private boolean checkOperation(Qd q, T3d center, int arrayIndex) {
    P3d pt = new P3d();
    int nFound = 0;
    boolean isInversion = (arrayIndex < firstProper);

    out: for (int n = points.length, i = n; --i >= 0 && nFound < n;) {
      if (i == centerAtomIndex)
        continue;
      T3d a1 = points[i];
      int e1 = elements[i];

      // check if point transforms to itself
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

        if (centerAtomIndex >= 0 && j == centerAtomIndex || j >= elements.length
            || elements[j] != e1) {
          continue;
        }
        if (pt.distanceSquared(a2) < distanceTolerance2) {
          nFound++;
          continue out;
        }
      }
      return false;
    }
    return true;
  }

  private void findInversionCenter() {
    haveInversionCenter = checkOperation(null, center, -1);
    if (haveInversionCenter) {
      axes[ci] = new Operator[] { newInversionCenter(++nOps) };
      nAxes[ci] = 1;
    }
  }

  private Operator setPrincipalAxis(int n, int nPlanes) {
    principalPlane = setPrincipalPlane(n, nPlanes);
    if (nPlanes == 0 && n < firstProper || nAxes[n] == 1) {
      //      if (nPlanes > 0 && n < firstProper)
      //        n = firstProper + n / 2;
      return axes[n][0];
    }
    // D2, D2d, D2h -- which c2 axis is it?
    if (principalPlane == null)
      return null;
    Operator[] c2axes = axes[c2];

    for (int i = 0; i < nAxes[c2]; i++)
      if (isParallel(principalPlane.normalOrAxis, c2axes[i].normalOrAxis)) {
        if (i != 0) {
          Operator o = c2axes[0];
          c2axes[0] = c2axes[i];
          c2axes[i] = o;
        }
        return c2axes[0];
      }
    return null;
  }

  private Operator setPrincipalPlane(int n, int nPlanes) {
    // principal plane is perpendicular to more than two other planes
    Operator[] planes = axes[cs];
    if (nPlanes == 1)
      return principalPlane = planes[0];
    if (nPlanes == 0 || nPlanes == n - firstProper)
      return null;
    for (int i = 0; i < nPlanes; i++) {
      for (int j = 0, nPerp = 0; j < nPlanes; j++) {
        if (isPerpendicular(planes[i].normalOrAxis, planes[j].normalOrAxis)
            && ++nPerp > 2) {
          if (i != 0) {
            Operator o = planes[0];
            planes[0] = planes[i];
            planes[i] = o;
          }
          return principalPlane = planes[0];
        }
      }
    }
    return null;
  }

  private int getPointIndex(int j) {
    return (j < 0 ? -j : atomMap[j]) - 1;
  }

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

        double order = (2 * Math.PI / v1.angle(v2));
        int iOrder = (int) Math.floor(order + 0.01f);
        boolean isIntegerOrder = (order - iOrder <= 0.02f);
        int arrayIndex = iOrder + firstProper;
        if (!isIntegerOrder || (arrayIndex) >= maxAxis)
          continue;
        if (nAxes[arrayIndex] < axesMaxN[arrayIndex]) {
          v3.cross(v1, v2);
          checkForAxis(arrayIndex, v3);
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
          if (v3.length() < 1)
            continue;
          checkForAxis(c3, v3);
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
          checkForAxis(c2, v1);

        }

    return getHighestOrder();
  }

  private void getAllAxes(V3d v3) {
    for (int o = c2; o < maxAxis; o++)
      if (nAxes[o] < axesMaxN[o]) {
        checkForAxis(o, v3);
      }
  }

  private int getHighestOrder() {
    int n = 0;
    // highest S
    for (n = firstProper; --n > 1 && nAxes[n] == 0;) {
    }
    // or highest C
    if (n > 1)
      return (n + firstProper < maxAxis && nAxes[n + firstProper] > 0
          ? n + firstProper
          : n);
    for (n = maxAxis; --n > 1 && nAxes[n] == 0;) {
    }
    return n;
  }

  /**
   * Check to see that this symmetry is allowed
   * 
   * @param arrayIndex
   * @param v
   * @return true if OK
   */
  private boolean checkForAxis(int arrayIndex, V3d v) {
    if (!isCompatible(arrayIndex))
      return false;
    v.normalize();
    if (haveAxis(arrayIndex, v))
      return false;
    Qd q = getQuaternion(v, arrayIndex);
    if (!checkOperation(q, center, arrayIndex))
      return false;
    addAxis(arrayIndex, v);
    checkForAssociatedAxes(arrayIndex, v);
    return true;
  }

  private void checkForAssociatedAxes(int arrayIndex, V3d v) {
    // check for Sn:
    switch (arrayIndex) {
    case c2:
      checkForAxis(s4, v);//D2d, D4h, D6d
      break;
    case c3:
      checkForAxis(s3, v);//C3h, D3h
      if (haveInversionCenter)
        addAxis(s6, v);
      break;
    case c4:
      addAxis(c2, v);
      checkForAxis(s4, v);//D2d, D4h, D6d
      checkForAxis(s8, v);//D4d
      break;
    case c5:
      checkForAxis(s5, v); //C5h, D5h
      if (haveInversionCenter)
        addAxis(s10, v);
      break;
    case c6:
      addAxis(c2, v);
      addAxis(c3, v);
      checkForAxis(s3, v);//C6h, D6h
      checkForAxis(s6, v);//C6h, D6h
      checkForAxis(s12, v);//D6d
      break;
    case c7:
      checkForAxis(s7, v); //C7h, D7h
      if (haveInversionCenter)
        addAxis(s14, v);
      break;
    case c8:
      //note -- D8d would have a S16 axis
      addAxis(c2, v);
      addAxis(c4, v);
      checkForAxis(s4, v);//C8h, D8h
      checkForAxis(s8, v);//D8h, D8h
      checkForAxis(s16, v);//D8d
      break;
    }
  }

  private boolean isCompatible(int arrayIndex) {
    switch (arrayIndex) {
    case c8:
      if (nAxes[7] > 0 || nAxes[c3] > 0)
        return false;
      //$FALL-THROUGH$;
    case c6:
    case c4:
      if (nAxes[c7] > 0 || nAxes[c5] > 0)
        return false;
      break;
    case c2:
      break;
    case c3:
      if (nAxes[c7] > 0 || nAxes[c8] > 0)
        return false;
      break;
    case c5:
      if (nAxes[c4] > 0 || nAxes[c6] > 0 || nAxes[c7] > 0 || nAxes[c8] > 0)
        return false;
      break;
    case c7:
      if (nAxes[c3] > 0 || nAxes[c4] > 0 || nAxes[c5] > 0 || nAxes[c6] > 0
          || nAxes[c8] > 0)
        return false;
      break;
    }
    return true;
  }

  private boolean haveAxis(int arrayIndex, V3d v) {
    if (nAxes[arrayIndex] == axesMaxN[arrayIndex]) {
      return true;
    }
    if (nAxes[arrayIndex] > 0)
      for (int i = nAxes[arrayIndex]; --i >= 0;) {
        if (isParallel(v, axes[arrayIndex][i].normalOrAxis))
          return true;
      }
    return false;
  }

  private void addAxis(int arrayIndex, V3d v) {
    if (haveAxis(arrayIndex, v))
      return;
    if (axes[arrayIndex] == null)
      axes[arrayIndex] = new Operator[axesMaxN[arrayIndex]];
    axes[arrayIndex][nAxes[arrayIndex]++] = newAxis(++nOps, v,
        arrayIndex);
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
        v1.normalize();
        v2.normalize();
        if (!isParallel(v1, v2)) {
          v3.cross(v1, v2);
          v3.normalize();
          nPlanes = addPlane(v3);
        }

        // second, look for planes perpendicular to the A -- B line

        v3.sub2(a2, a1);
        v3.normalize();
        nPlanes = addPlane(v3);
        if (nPlanes == axesMaxN[0])
          return nPlanes;
      }
    }

    // also look for planes normal to any C axis
    if (haveAxes)
      for (int i = c2; i < maxAxis; i++)
        for (int j = 0; j < nAxes[i]; j++)
          nPlanes = addPlane(axes[i][j].normalOrAxis);
    return nPlanes;
  }

  private void findAdditionalAxes(int nPlanes) {
    Operator[] planes = axes[0];
    int Cn = 0;
    if (nPlanes > 1 && ((Cn = nPlanes + firstProper) < maxAxis)
        && nAxes[Cn] == 0) {
      // cross pairs of plane normals. We don't need many.
      vTemp.cross(planes[0].normalOrAxis, planes[1].normalOrAxis);
      if (!checkForAxis(Cn, vTemp) && nPlanes > 2) {
        vTemp.cross(planes[1].normalOrAxis, planes[2].normalOrAxis);
        checkForAxis(Cn - 1, vTemp);
      }
    }
    if (nAxes[c2] == 0 && nPlanes > 2) {
      // check for C2 axis relating 
      for (int i = 0; i < nPlanes - 1; i++) {
        for (int j = i + 1; j < nPlanes; j++) {
          vTemp.add2(planes[1].normalOrAxis, planes[2].normalOrAxis);
          //if (
          checkForAxis(c2, vTemp);
          //)
          //Logger.error("found a C2 axis by adding plane normals");
        }
      }
    }
  }

  private int addPlane(V3d v3) {
    if (!haveAxis(cs, v3) && checkOperation(Qd.newVA(v3, 180), center, -1))
      axes[cs][nAxes[cs]++] = newPlane(++nOps, v3);
    return nAxes[cs];
  }

  String getName() {
    return getNameByConvention(name);
  }

  String updateDraw() {
    return null;
 //   return (drawID == null ? null : (String) getInfo(modelIndex, drawID, false, type, index, scaleFactor));
  }

  Object getInfo(int modelIndex, T3d a1, T3d a2, String drawID, boolean asMap, String type,
                 int index, double scaleFactor) {
    if (drawID == null && type == null && !asMap && textInfo != null)
      return textInfo;
    if (drawID != null) {
      this.modelIndex = modelIndex;
      this.drawID = drawID;
      this.type = type;
      this.index = index;
      this.scaleFactor = scaleFactor;
    }
    operations = new Lst<>();
    boolean justThisUVW = (type != null && type.indexOf("u") >= 0);
    if (a1 == null && a2 == null && drawID == null && drawInfo != null && drawIndex == index
        && this.scale == scale && drawType.equals(type == null ? "" : type))
      return drawInfo;
    if (asMap && info != null)
      return info;
    boolean asDraw = (drawID != null);
    info = null;
    Lst<Map<String, Object>> elements = null;
    V3d v = new V3d();
    Operator op;
    if (scaleFactor == 0)
      scaleFactor = 1;
    scale = scaleFactor;
    int[][] nType = new int[4][2];
    for (int i = 1; i < maxAxis; i++)
      for (int j = nAxes[i]; --j >= 0;)
        nType[axes[i][j].type][0]++;
    SB sb = new SB().append("# ").appendI(nAtoms).append(" atoms\n");
    String name = getNameByConvention(this.name);

    boolean haveThisType = false;
    Operation operationInv = (haveInversionCenter ? new Operation(null) : null);
    if (operationInv != null) {
      operations.addLast(operationInv);
    }

    if (asDraw) {
      boolean haveType = (type != null && type.length() > 0);
      drawType = type = (haveType ? type : "");
      drawIndex = index;
      boolean anyProperAxis = (type
          .equalsIgnoreCase(getNameByConvention("Cn")));
      boolean anyImproperAxis = (type
          .equalsIgnoreCase(getNameByConvention("Sn")));
      String head = "set perspectivedepth off;draw " + drawID + " delete;\n";
      String m = "_" + modelIndex + "_";
      if (justThisUVW || !haveType)
        sb.append(getDrawID("pg0" + m + "* delete;") //
            + getDrawID("pgva" + m + "* delete;") //
            + getDrawID("pgvp" + m + "* delete;"));
      if (!haveType || type.equalsIgnoreCase("Ci")
          || type.equalsIgnoreCase("-u,-v,-w")) {
        int pt = sb.length();
        sb.append(getDrawID("pg0" + m + (haveInversionCenter ? "inv" : "")));
        if (operationInv != null) {
          operationInv.drawID = sb.substring(pt);
          haveThisType = justThisUVW;
        }
        sb.append(" ").append(Escape.eP(center))
            .append(haveInversionCenter ? "\"i\";\n" : ";\n");
        if (operationInv != null) {
          operationInv.drawStr = sb.substring(pt);
          System.out.println(operationInv);
        }
      }

      double offset = 0.1d;
      double axisWidth = (isSpinGroup ? 1.9d : 0.05d);
      for (int i = 2; i < maxAxis && !haveThisType; i++) {
        if (i == firstProper)
          offset = 0.1d;
        if (nAxes[i] == 0)
          continue;
        String sglabel = (isLinear ? "C\u221e" : getOpName(axes[i][0], false));
        String label = (isLinear ? "\u221e" : getOpName(axes[i][0], true));
        offset += 0.25d;
        double scale = scaleFactor * 1.05d * radius + offset * 80 / sppa;
        boolean isProper = (i >= firstProper);
        highestOrder = getHighestOrder();
        if (justThisUVW || !haveType || type.equalsIgnoreCase(label)
            || anyProperAxis && isProper || anyImproperAxis && !isProper) {
          for (int j = 0; j < nAxes[i]; j++) {
            if (index > 0 && j + 1 != index)
              continue;
            op = axes[i][j];
            if (justThisUVW && !op.isUVW(type))
              continue;
            haveThisType = justThisUVW;
            operations.addLast(op.operation = new Operation(op));

            int pt = sb.length();
            String s = getDrawID(
                "pgva" + m + sglabel.replace("\u221e", "infinity") + (j + 1) + "\1");
            op.operation.drawID = sb.substring(pt).replace('\1', ' ');
            drawAxis(sb, op, s, label, axisWidth, scale, isProper, 'a', v);
            drawAxis(sb, op, s, label, axisWidth, scale, isProper, 'b', v);
            op.operation.drawStr = sb.substring(pt);
            if (Logger.debugging)
              Logger.debug(op.operation.toString());
          }
        }
      }
      if (justThisUVW || !haveType
          || type.equalsIgnoreCase(getNameByConvention("Cs"))) {
        for (int j = 0; j < nAxes[cs] && !haveThisType; j++) {
          if (index > 0 && j + 1 != index)
            continue;
          op = axes[cs][j];
          if (justThisUVW && !op.isUVW(type))
            continue;
          haveThisType = justThisUVW;
          operations.addLast(op.operation = new Operation(op));
          String s = op.operation.drawID = getDrawID("pgvp" + m + (j + 1) + "\1");
          int pt = sb.length();
          drawPlane(sb, op, s, scaleFactor, v);
          op.operation.drawStr = sb.substring(pt);
          if (Logger.debugging)
            Logger.debug(op.operation.toString());
        }
        if (justThisUVW && a1 != null && a2 != null) {
          String cmd = getDrawID("pg0" + m + "_line");
          sb.append(cmd).append(" width " + axisWidth/2 + " " + a1 + a2 + " color yellow;");
        }
      }
      sb.append("# name=").append(name);
      sb.append(", n" + getNameByConvention("Ci") + "=")
          .appendI(haveInversionCenter ? 1 : 0);
      sb.append(", n" + getNameByConvention("Cs") + "=")
          .appendI(nAxes[OPERATION_PLANE]);
      sb.append(", n" + getNameByConvention("Cn") + "=")
          .appendI(nType[OPERATION_PROPER_AXIS][0]);
      sb.append(", n" + getNameByConvention("Sn") + "=")
          .appendI(nType[OPERATION_IMPROPER_AXIS][0]);
      sb.append(": ");
      for (int i = maxAxis; --i >= 2;) {
        if (nAxes[i] > 0) {
          String axisName = getNameByConvention(
              (i < firstProper ? "S" : "C") + (i % firstProper));
          sb.append(" n").append(axisName);
          sb.append("=").appendI(nAxes[i]);
        }
      }
      sb.append(";\n");
      sb.append("print '" + name + "';\n");
      drawInfo = head + sb.toString();
      if (Logger.debugging)
        Logger.info(drawInfo);
      return drawInfo;
    }
    int n = 0;
    int nTotal = 1;
    int nElements = 0; // planes Cs

    String ctype = (haveInversionCenter ? getNameByConvention("Ci") : "center");
    if (haveInversionCenter) {
      nTotal++;
      nElements++;
    }
    // get information, either as a String or a Map
    if (asMap) {
      info = new Hashtable<String, Object>();
      elements = new Lst<>();
      if (center != null) {
        info.put(ctype, center);
        if (haveInversionCenter)
          info.put("center", center);
        info.put(ctype, center);
      }
      info.put("elements", elements);
      if (haveInversionCenter) {
        info.put("Ci_m", M3d.newM3(mInv));
        Map<String, Object> e = new Hashtable<String, Object>();
        axes[ci][0].setInfo(null, null, e);
        e.put("location", center);
        e.put("type", getNameByConvention("Ci"));
        elements.addLast(e);
      }
    } else {
      sb.append("\n\n").append(name).append("\t").append(ctype).append("\t")
          .append(Escape.eP(center));
    }
    for (int i = maxAxis; --i >= 0;) {
      if (i == ci || nAxes[i] == 0)
        continue;

      // includes planes
      n = nUnique[i];
      Operator[] a = axes[i];
      String sglabel = getOpName(a[0], false);
      String label = getOpName(a[0], true);
      int ni = nAxes[i];
      if (asMap) {
        info.put("n" + sglabel, Integer.valueOf(nAxes[i]));
      } else {
        sb.append("\n\n").append(name).append("\tn").append(label).append("\t")
            .appendI(ni).append("\t").appendI(n);
      }
      // not right for uvw business
      n *= ni;
      nTotal += n;
      nElements += ni;
      nType[a[0].type][1] += n;
      Lst<V3d> vinfo = (asMap ? new Lst<V3d>() : null);
      Lst<M3d> minfo = (asMap ? new Lst<M3d>() : null);
      for (int j = 0; j < ni; j++) {
        //axes[i][j].typeIndex = j + 1;
        Operator aop = a[j];
        if (type != null && !aop.isUVW(type))
          continue;
        if (!asDraw && aop.operation == null)
          operations.addLast(aop.operation = new Operation(aop));
        if (asMap) {
          Map<String, Object> e = new Hashtable<String, Object>();
          aop.setInfo(vinfo, minfo, e);
          e.put("type", label);
          elements.addLast(e);
        } else {
          sb.append("\n").append(name).append("\t").append(sglabel).append("_")
              .appendI(j + 1).append("\t").appendO(aop.normalOrAxis);
        }
      }
      if (asMap) {
        info.put(sglabel, vinfo);
        info.put(sglabel + "_m", minfo);
      }
    }
    if (asMap && highOperations != null) {
      for (Operator o : highOperations) {
        Map<String, Object> e = new Hashtable<String, Object>();
        o.setInfo(null, null, e);
        e.put("type", this.getNameByConvention(o.schName));
        elements.addLast(e);
      }
    }

    if (!asMap)
      return textInfo = getTextInfo(sb, nType, nTotal);

    info.put("name", this.name);
    info.put("hmName", getHermannMauguinName());
    info.put("nAtoms", Integer.valueOf(nAtoms));
    info.put("nTotal", Integer.valueOf(nTotal));
    info.put("nElements", Integer.valueOf(nElements));
    info.put("nCi", Integer.valueOf(nAxes[ci]));
    info.put("nC2", Integer.valueOf(nAxes[c2]));
    info.put("nC3", Integer.valueOf(nAxes[c3]));
    info.put("nCs", Integer.valueOf(nAxes[cs]));
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

  private String getDrawID(String id) {
    int pt = id.indexOf(' ');
    if (pt < 0)
      pt = id.length();
    id = id.substring(0, pt) + "\"" + id.substring(pt);
    return "draw"+(id.indexOf("*") >= 0 ? "" : " model " + modelIndex)+" ID \"" + id;
  }

  private void drawPlane(SB sb, Operator op, String s, double scaleFactor,
                         V3d v) {
    sb.append(s.replace("\1", "disk"));
    sb.append(" scale ").appendD(scaleFactor * radius * 2)
        .append(" CIRCLE PLANE ").append(Escape.eP(center));
    v.add2(op.normalOrAxis, center);
    sb.append(Escape.eP(v)).append(" color translucent yellow;\n");
    v.add2(op.normalOrAxis, center);
    sb.append(s.replace("\1", "ring"));
    sb.append(" width 0.05 scale ").appendD(scaleFactor * radius * 2)
        .append(" arc ").append(Escape.eP(v));
    v.scaleAdd2(-2, op.normalOrAxis, v);
    sb.append(Escape.eP(v));
    v.add3(0.011f, 0.012f, 0.013f);
    sb.append(Escape.eP(v)).append("{0 360 0.5} color ")
        .append(
            principalPlane != null && op.index == principalPlane.index ? "red"
                : "blue")
        .append(";\n");
  }

  private void drawAxis(SB sb, Operator op, String s, String label, double axisWidth, double scale,
                        boolean isProper, char c, V3d v) {
    
    boolean isPA = (!isLinear && principalAxis != null
        && op.index == principalAxis.index);
    int pt = sb.length();
    sb.append(" color ")
    .append(isPA ? "red"
        : op.type == OPERATION_IMPROPER_AXIS ? "blue" : "orange")
    .append(";\n");
    String tail = sb.substring(pt);
    sb.setLength(pt);
    sb.append(s.replace('\1', c));
    v.scaleAdd2((c == 'a'? 1 : -1) * scale, op.normalOrAxis, center);
    sb.append(" width " + axisWidth)
    .append(" ").append(Escape.eP(center))
    .append(Escape.eP(v));
    if (isProper && op.order != 2 && c == 'a'
        || op.order == 2 || !isProper && c == 'b') {
      label = PT.esc(">" + 
          getAxisLabelOffset(op, highestOrder) + label);
      sb.append(label);
    }
    
    sb.append(tail);
    
  }

  String getAxisLabelOffset(Operator op, int highestOrder) {
    //boolean isPA = isPrincipalAxis(op);
    switch (op.type) {
    case OPERATION_IMPROPER_AXIS:
    case OPERATION_PROPER_AXIS:
//      if (isPrincipalAxis(op))
//        return "";
      switch (op.order) {
      case 3:
        return (highestOrder % 6 == 0 ? "   " 
            : "");
      case 4:
        return (highestOrder % 12 == 0 ? "       " : "   ");
      case 6:
        return (highestOrder % 12 == 0 ? "           " : "");
      case 8:
        return "       ";
      case 12:
        return "               ";
      default:
        return "";
      }
    default:
    case OPERATION_PLANE:
    case OPERATION_INVERSION_CENTER:
      return "";
    }
  }

  private boolean isPrincipalAxis(Operator op) {
    return (principalAxis == null ? false : (Math.abs(Math.abs(principalAxis.normalOrAxis.dot(op.normalOrAxis)) - 1) > 0.01d));    
  }

  private String getTextInfo(SB sb, int[][] nType, int nTotal) {
    // finally, string tabulation only

    sb.append("\n");
    sb.append("\n").append(name).append("\ttype\tnElements\tnUnique");
    sb.append("\n").append(name)
        .append("\t" + getNameByConvention("E") + "\t  1\t  1");

    sb.append("\n").append(name)
        .append("\t" + getNameByConvention("Ci") + "\t  ").appendI(nAxes[ci])
        .append("\t  ").appendI(nAxes[ci]);

    sb.append("\n").append(name)
        .append("\t" + getNameByConvention("Cs") + "\t");
    PT.rightJustify(sb, "    ", nAxes[cs] + "\t");
    PT.rightJustify(sb, "    ", nAxes[cs] + "\n");

    sb.append(name).append("\t" + getNameByConvention("Cn") + "\t");
    PT.rightJustify(sb, "    ", nType[OPERATION_PROPER_AXIS][0] + "\t");
    PT.rightJustify(sb, "    ", nType[OPERATION_PROPER_AXIS][1] + "\n");

    sb.append(name).append("\t" + getNameByConvention("Sn") + "\t");
    PT.rightJustify(sb, "    ", nType[OPERATION_IMPROPER_AXIS][0] + "\t");
    PT.rightJustify(sb, "    ", nType[OPERATION_IMPROPER_AXIS][1] + "\n");

    sb.append(name).append("\t\tTOTAL\t");
    PT.rightJustify(sb, "    ", nTotal + "\n");
    return sb.toString();
  }

  final static Qd getQuaternion(V3d v, int arrayIndex) {
    return Qd.newVA(v, (arrayIndex < firstProper ? 180 : 0)
        + (arrayIndex == 0 ? 0 : 360 / (arrayIndex % firstProper)));
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

  private boolean isEqual(PointGroup pg) {
    if (pg == null)
      return false;
    if (convention != pg.convention || linearTolerance != pg.linearTolerance
        || distanceTolerance != pg.distanceTolerance || nAtoms != pg.nAtoms
        || localEnvOnly != pg.localEnvOnly || haveVibration != pg.haveVibration
        || bsAtoms == null ? pg.bsAtoms != null : !bsAtoms.equals(pg.bsAtoms))
      return false;
    for (int i = 0; i < nAtoms; i++) {
      // real floating == 0 here because they must be IDENTICAL POSITIONS
      if (elements[i] != pg.elements[i] || !points[i].equals(pg.points[i]))
        return false;
    }
    return true;
  }

  // Schoenflies to Hermann-Mauguin

  private String getHermannMauguinName() {
    return getHMfromSFName(name);
  }

  private String getNameByConvention(String name) {
    switch (convention) {
    default:
    case CONVENTION_SCHOENFLIES:
      return name;
    case CONVENTION_HERMANN_MAUGUIN:
      return getHMfromSFName(name);
    }
  }

  /**
   * Get the label for the operation based on the convention chosen (H-M or
   * Schoenflies). Note that inversion is not included here.
   * 
   * @param op
   * @param conventional
   *        if false, just return Schoenflies
   * 
   * @return label Cs, Cn, Sn (Schoenflies) or m n, -n
   */
  private String getOpName(Operator op, boolean conventional) {
    return (conventional ? getNameByConvention(op.schName) : op.schName);
  }

  // using https://en.wikipedia.org/wiki/Hermann%E2%80%93Mauguin_notation
  // using https://en.wikipedia.org/wiki/Point_group with added infm and inf/mm
  private final static String[] SF2HM = ("Cn,1,2,3,4,5,6,7,8,9,10,11,12"
      + "|Cnv,m,2m,3m,4mm,5m,6mm,7m,8mm,9m,10mm,11m,12mm,\u221em"
      + "|Sn,,-1,-6,-4,(-10),-3,(-14),-8,(-18),-5,(-22),(-12)"
      + "|Cnh,m,2/m,-6,4/m,-10,6/m,-14,8/m,-18,10/m,-22,12/m"
      + "|Dn,,222,32,422,52,622,72,822,92,(10)22,(11)2,(12)22"
      + "|Dnd,,-42m,-3m,-82m,-5m,(-12)2m,-7m,(-16)2m,-9m,(-20)2m,(-11)m,(-24)2m"
      + "|Dnh,,mmm,-6m2,4/mmm,(-10)m2,6/mmm,(-14)m2,8/mmm,(-18)m2,10/mmm,(-22)m2,12/mmm,\u221e/mm"
      + "|Ci,-1" + "|Cs,m" + "|T,23" + "|Th,m-3" + "|Td,-43m" + "|O,432"
      + "|Oh,m-3m").split("\\|");

  private static Map<String, String> htSFToHM;

  /**
   * Get Hermann-Mauguin name from Schoenflies name
   * 
   * @param name
   * @return HM name
   */
  public static String getHMfromSFName(String name) {
    if (htSFToHM == null) {
      htSFToHM = new Hashtable<String, String>();
      String[] syms = SF2HM;
      addNames("E", "1");
      addNames("Ci", "-1");
      addNames("Cn", "n");
      addNames("Sn", "-n");
      for (int i = 0; i < syms.length; i++) {
        String[] list = syms[i].split(",");
        String sym = list[0];
        if (list.length == 2) {
          addNames(sym, list[1]);
          continue;
        }
        String type = sym.substring(0, 1);
        String ext = sym.substring(2, sym.length());
        for (int n = 1; n < 13; n++) {
          String val = list[n];
          if (val.length() > 0) {
            addNames(type + n + ext, val);
            if (Logger.debugging)
              Logger.debug(type + n + ext + "\t" + val);
          }
        }
        if (list.length == 14) {
          // "Dinfd
          addNames(type + "\u221e" + ext, list[13]);
        }
      }

    }
    String hm = htSFToHM.get(name);
    return (hm == null ? name : hm);
  }

  private static void addNames(String sch, String hm) {
    htSFToHM.put(sch, hm);
    htSFToHM.put(hm, sch);
  }


  //  C1  1
  //  C2  2
  //  C3  3
  //  C4  4
  //  C5  5
  //  C6  6
  //  C7  7
  //  C8  8
  //  C9  9
  //  C10 10
  //  C11 11
  //  C12 12
  //  C1v 1m
  //  C2v 2m
  //  C3v 3m
  //  C4v 4mm
  //  C5v 5m
  //  C6v 6mm
  //  C7v 7m
  //  C8v 8mm
  //  C9v 9m
  //  C10v  10mm
  //  C11v  11m
  //  C12v  12mm
  //  S2  -1
  //  S4  -4
  //  S6  -3
  //  S8  -8
  //  S10 -5
  //  S12 (-12)
  //  C1h -2
  //  C2h 2/m
  //  C3h -6
  //  C4h 4/m
  //  C5h -10
  //  C6h 6/m
  //  C7h -14
  //  C8h 8/m
  //  C9h -18
  //  C10h  10/m
  //  C11h  -22
  //  C12h  12/m
  //  D2  22
  //  D3  32
  //  D4  422
  //  D5  52
  //  D6  622
  //  D7  72
  //  D8  822
  //  D9  92
  //  D10 (10)22
  //  D11 (11)2
  //  D12 (12)22
  //  D2d -42m
  //  D3d -32/m
  //  D4d -82m
  //  D5d -52/m
  //  D6d (-12)2m
  //  D7d -72/m
  //  D8d (-16)2m
  //  D9d -92/m
  //  D10d  (-20)2m
  //  D11d  (-11)2/m
  //  D12d  (-24)2m
  //  D2h 2/m2/m2/m
  //  D3h -6m2
  //  D4h 4/m2/m2/m
  //  D5h (-10)m2
  //  D6h 6/m2/m2/m
  //  D7h (-14)m2
  //  D8h 8/m2/m2/m
  //  D9h (-18)m2
  //  D10h  10/m2/m2/m
  //  D11h  (-22)m2
  //  D12h  12/m2/m2/mDn/2h
  //  Ci  -1
  //  Cs  -2
  //  T 23
  //  Th  m-3
  //  Td  -43m
  //  O 432
  //  Oh  m-3m

}
