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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */


package org.jmol.symmetry;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.Interface;
import org.jmol.util.BoxInfo;
import org.jmol.util.Escape;
import org.jmol.util.SimpleUnitCell;
import org.jmol.util.Tensor;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.M3d;
import javajs.util.M4d;
import javajs.util.P3d;
import javajs.util.P4d;
import javajs.util.PT;
import javajs.util.Qd;
import javajs.util.T3d;
import javajs.util.T4d;
import javajs.util.V3d;


/**
 * a class pseudoprivate to the org.jmol.symmetry and org.jmol.applet.smarter.FileSymmetry
 * to be accessed generally only through the SymmetryInterface API 
 * 
 * adds vertices and offsets orientation, 
 * and a variety of additional calculations that in 
 * principle could be put in SimpleUnitCell
 * if desired, but for now are in this optional package.
 * 
 */

public class UnitCell extends SimpleUnitCell implements Cloneable {
  
  final private static double twoP2 = 2 * Math.PI * Math.PI;

  private final static V3d[] unitVectors = {
      JC.axisX, JC.axisY, JC.axisZ};
  
  Lst<String> moreInfo;
  
  String name = "";
  
  private P3d[] vertices; // eight corners
  
  private P3d fractionalOffset;

  /**
   * this flag TRUE causes an update of matrixCtoFNoOffset each time an offset is changed
   * so that it is updated and the two stay the same; set true only for isosurfaceMesh
   * 
   */
  private boolean allFractionalRelative;
  
  private final P3d cartesianOffset = new P3d();
  

  /**
   * a P3 or P4; the raw multiplier for the cell from
   * 
   * UNITCELL {ijk ijk scale}
   * 
   * UNITCELL {1iiijjjkkk 1iiijjjkkk scale}
   * 
   * (encoded as a P4: {1iiijjjkkk 1iiijjjkkk scale 1kkkkkk} )
   * 
   * Expanded cell notation:
   * 
   * 111 - 999 --> center 5,5,5; range 0 to 9 or -5 to +4
   * 
   * 1000000 - 1999999 --> center 50,50,50; range 0 to 99 or -50 to +49
   * 1000000000 - 1999999999 --> center 500, 500, 500; range 0 to 999 or -500 to
   * +499
   * 
   * for example, a 3x3x3 block of 27 cells:
   * 
   * {444 666 1} or {1494949 1515151 1} or {1499499499 1501501501 1}
   * 
   */
  private T3d unitCellMultiplier;

  /**
   * the multiplied, offset UnitCell derived from this UnitCell
   */
  private UnitCell unitCellMultiplied;

  /**
   * used for fast comparison of fractional-to-Cartesian matrix
   */
  private double[][] f2c;
  

  /**
   * Private constructor. 
   * 
   */  
  private UnitCell() {
    super();  
  }

  /**
   * 
   * A special constructor for spacially defined unit cells.
   * Not used by readers. 
   * 
   * @param oabc [origin, Va, Vb, Vc]
   * @param setRelative a flag only set true for IsosurfaceMesh
   * @return new unit cell
   */
  public static UnitCell fromOABC(T3d[] oabc, boolean setRelative) {
    UnitCell c = new UnitCell();
    if (oabc.length == 3) // not used
      oabc = new T3d[] { new P3d(), oabc[0], oabc[1], oabc[2] };
    double[] parameters = new double[] { -1, 0, 0, 0, 0, 0, oabc[1].x,
        oabc[1].y, oabc[1].z, oabc[2].x, oabc[2].y, oabc[2].z,
        oabc[3].x, oabc[3].y, oabc[3].z };
    c.init(parameters);
    c.allFractionalRelative = setRelative;
    c.initUnitcellVertices();
    c.setCartesianOffset(oabc[0]);
    return c;
  }

  /**
   * 
   * @param params
   * @param setRelative
   *        only set true for JmolData and tensors
   * @param slop
   * @return a new unit cell
   */
  public static UnitCell fromParams(double[] params, boolean setRelative,
                                    double slop) {
    UnitCell c = new UnitCell();
    c.init(params);
    c.initUnitcellVertices();
    c.allFractionalRelative = setRelative;
    c.setPrecision(slop);
    if (params.length > SimpleUnitCell.PARAM_SLOP)
      params[PARAM_SLOP] = slop;
    return c;
  }
    
  static UnitCell cloneUnitCell(UnitCell uc) {
    UnitCell ucnew = null;
    try {
      ucnew = (UnitCell) uc.clone();
    } catch (CloneNotSupportedException e) {
    }
    return ucnew;
  }

  /**
   * 
   * @param f1
   * @param f2
   * @param distance
   * @param dx
   * @param iRange
   * @param jRange
   * @param kRange
   * @param ptOffset TODO
   * @return       TRUE if pt has been set.
   */
  public boolean checkDistance(P3d f1, P3d f2, double distance, double dx,
                              int iRange, int jRange, int kRange, P3d ptOffset) {
    P3d p1 = P3d.newP(f1);
    toCartesian(p1, true);
    for (int i = -iRange; i <= iRange; i++)
      for (int j = -jRange; j <= jRange; j++)
        for (int k = -kRange; k <= kRange; k++) {
          ptOffset.set(f2.x + i, f2.y + j, f2.z + k);
          toCartesian(ptOffset, true);
          double d = p1.distance(ptOffset);
          if (dx > 0 ? Math.abs(d - distance) <= dx : d <= distance && d > 0.1d) {
            ptOffset.set(i, j, k);
            return true;
          }
        }
    return false;
  }

  /**
   * Check atom position for range [0, 1) allowing for rounding.
   * Used for SELECT UNITCELL only 

   * @param pt 
   * @return true if in [0, 1)
   */
  boolean checkPeriodic(P3d pt) {
    switch (dimension) {
    case 3:
      if (pt.z < -slop || pt.z > 1 - slop)
        return false;
      //$FALL-THROUGH$
    case 2:
      if (pt.y < -slop || pt.y > 1 - slop)
        return false;
      //$FALL-THROUGH$
    case 1:
    if (pt.x < -slop || pt.x > 1 - slop)
      return false;
    }
    return true;
  }

  String dumpInfo(boolean isDebug, boolean multiplied) {
    UnitCell m = (multiplied ? getUnitCellMultiplied() : this);
    if (m != this)
      return m.dumpInfo(isDebug, false);
    return "a=" + a + ", b=" + b + ", c=" + c + ", alpha=" + alpha + ", beta=" + beta + ", gamma=" + gamma
       + "\noabc=" + Escape.eAP(getUnitCellVectors())
       + "\nvolume=" + volume
       + (isDebug ? "\nfractional to cartesian: " + matrixFractionalToCartesian 
       + "\ncartesian to fractional: " + matrixCartesianToFractional : "");
  }

  private double fix000(double x) {
    return (Math.abs(x) < 0.001 ? 0 : x);
  }

  
  private double fixFloor(double d) {
    return (d == 1 ? 0 : d);
  }

  /**
   * 
   * @param scale
   * @param withOffset
   * @return points in Triangulator order
   */
  P3d[] getCanonicalCopy(double scale, boolean withOffset) {
    P3d[] pts = getScaledCell(withOffset);
    return BoxInfo.getCanonicalCopy(pts, scale);
  }

  P3d getCartesianOffset() {
    // for slabbing isosurfaces and rendering the ucCage
    return cartesianOffset;
  }

  /**
   * calculate weighting of 1 (interior), 0.5 (face), 0.25 (edge), or 0.125 (vertex)
   * @param pt
   * @return weighting
   */
  double getCellWeight(P3d pt) {
    double f = 1;
    if (pt.x <= slop || pt.x >= 1 - slop)
      f /= 2;
    if (pt.y <= slop || pt.y >= 1 - slop)
      f /= 2;
    if (pt.z <= slop || pt.z >= 1 - slop)
      f /= 2;
    return f;
  }

  /**
   * return a conventional lattice from a primitive
   * 
   * @param latticeType  "A" "B" "C" "R" etc.
   * @param primitiveToCrystal 
   * @return [origin va vb vc]
   */
  public T3d[] getConventionalUnitCell(String latticeType, M3d primitiveToCrystal) {
    T3d[] oabc = getUnitCellVectors();
    if (!latticeType.equals("P") || primitiveToCrystal != null)
      toFromPrimitive(false, latticeType.charAt(0), oabc, primitiveToCrystal);
    return oabc;
  }

  /**
   * 
   * @param pt
   *        the point to transform
   * @param flags
   *        "tofractional,fromfractional,packed"
   * @param ops
   *        space group operations
   * @param list
   *        the list to append to
   * @param i0
   *        the starting index of the list
   * @param n0
   *        the first point that is to be duplicated; prior points are just
   *        references for removing duplicates
   * @return augmented list
   */
  Lst<P3d> getEquivPoints(P3d pt, String flags, M4d[] ops, Lst<P3d> list,
                                int i0, int n0) {
    boolean fromfractional = (flags.indexOf("fromfractional") >= 0);
    boolean tofractional = (flags.indexOf("tofractional") >= 0);
    boolean packed = (flags.indexOf("packed") >= 0);
    if (list == null)
      list = new Lst<P3d>();
    P3d pf = P3d.newP(pt);
    if (!fromfractional)
      toFractional(pf, true);
    int n = list.size();
    for (int i = 0, nops = ops.length; i < nops; i++) {
      P3d p = P3d.newP(pf);
      ops[i].rotTrans(p);
      //not using unitize here, because it does some averaging
      p.x =  fixFloor(p.x - Math.floor(p.x));
      p.y =  fixFloor(p.y - Math.floor(p.y));
      p.z =  fixFloor(p.z - Math.floor(p.z));
      list.addLast(p);
      n++;
    }
    if (packed) {
      // duplicate all the points. 
      for (int i = n0; i < n; i++) {
        pf.setT(list.get(i));
        unitizeRnd(pf);
        if (pf.x == 0) {
          list.addLast(P3d.new3(0,  pf.y,  pf.z));
          list.addLast(P3d.new3(1,  pf.y,  pf.z));
          if (pf.y == 0) {
            list.addLast(P3d.new3(1, 1,  pf.z));
            list.addLast(P3d.new3(0, 0,  pf.z));
            if (pf.z == 0) {
              list.addLast(P3d.new3(1, 1, 1));
              list.addLast(P3d.new3(0, 0, 0));
            }
          }
        }
        if (pf.y == 0) {
          list.addLast(P3d.new3( pf.x, 0,  pf.z));
          list.addLast(P3d.new3( pf.x, 1,  pf.z));
          if (pf.z == 0) {
            list.addLast(P3d.new3( pf.x, 0, 0));
            list.addLast(P3d.new3( pf.x, 1, 1));
          }
        }
        if (pf.z == 0) {
          list.addLast(P3d.new3( pf.x,  pf.y, 0));
          list.addLast(P3d.new3( pf.x,  pf.y, 1));
          if (pf.x == 0) {
            list.addLast(P3d.new3(0,  pf.y, 0));
            list.addLast(P3d.new3(1,  pf.y, 1));
          }
        }
      }
    }
    removeDuplicates(list, i0, n0, -1);
    if (!tofractional) {
      for (int i = list.size(); --i >= n0;)
        toCartesian(list.get(i), true);
    }
    return list;
  }
  
  P3d getFractionalOffset() {
    return fractionalOffset;
  }

  Map<String, Object> getInfo() {
    UnitCell m = getUnitCellMultiplied();
    if (m != this)
      return m.getInfo();       
    Map<String, Object> info = new Hashtable<String, Object>();
    info.put("params", unitCellParams);
    info.put("oabc", getUnitCellVectors());
    info.put("volume", Double.valueOf(volume));
    info.put("matFtoC", matrixFractionalToCartesian);
    info.put("matCtoF", matrixCartesianToFractional);
    return info;
  }

  /**
   * Returns a quaternion that will take the standard frame to a view down a
   * particular axis, expressed as its counterparts.
   * 
   * @param abc
   *        ab bc ca
   * @return quaternion
   */
  Qd getQuaternionRotation(String abc) {
    T3d a = V3d.newVsub(vertices[4], vertices[0]);
    T3d b = V3d.newVsub(vertices[2], vertices[0]);
    T3d c = V3d.newVsub(vertices[1], vertices[0]);
    T3d x = new V3d();
    T3d v = new V3d();

    //  qab = !quaternion({0 0 0}, cross(cxb,c), cxb);
    //  qbc = !quaternion({0 0 0}, cross(axc,a), axc)
    //  qca = !quaternion({0 0 0}, cross(bxa,b), bxa);
    //

    int mul = (abc.charAt(0) == '-' ? -1 : 1);
    if (mul < 0)
      abc = abc.substring(1);
    String abc0 = abc;
    abc = PT.rep(PT.rep(PT.rep(PT.rep(PT.rep(PT.rep(abc, 
        "ab", "A"), //3
        "bc", "B"), //4
        "ca", "C"), //5
        "ba", "D"), //6
        "cb", "E"), //7
        "ac", "F"); //8
    boolean isFace = !abc0.equals(abc);
    int quadrant = (isFace ? 1 : 0);
    if (abc.length() == 2) { // a1 a2 a3 a4 b1 b2 b3 b4...
      quadrant = abc.charAt(1) - 48;
      abc = abc.substring(0, 1);
    }
    boolean isEven = (quadrant % 2 == 0);
    int axis = "abcABCDEF".indexOf(abc);

    T3d v1, v2, v3;
    switch (axis) {
    case 7: // cb
      mul = -mul;
      //$FALL-THROUGH$
    case 4: // bc
      a.cross(c, b);
      quadrant = ((5 - quadrant) % 4) + 1;
          //$FALL-THROUGH$
    case 0: // a
    default:
      v1 = a;
      v2 = c;
      v3 = b;
      break;
    case 8: // ca
      mul = -mul;
      //$FALL-THROUGH$
    case 5: // ac
      mul = -mul;
      b.cross(c, a);
      quadrant = ((2 + quadrant) % 4) + 1;
    //$FALL-THROUGH$
    case 1: // b
      v1 = b;
      v2 = a;
      v3 = c;
      mul = -mul;
      break;
    case 3: // ab
      mul = -mul;
      //$FALL-THROUGH$
    case 6: // ba
      c.cross(a,b);
      if (isEven)
        quadrant = 6 - quadrant;
      //$FALL-THROUGH$
    case 2: // c
      v1 = c;
      v2 = a;
      v3 = b;
      if (!isFace && quadrant > 0) {
        quadrant = 5 - quadrant;
      }
     break;
    }
    if (quadrant > 0) {
      if (mul > 0 != isEven) {
        v2 = v3;
        v1.scale(-1);
      }
    }
    switch (quadrant) {
    case 0:
    default:
      // upper left for a b; bottom left for c
    case 1:
      // upper left
      break;
    case 2:
      // upper right
      v1.scale(-1);
      v2.scale(-1);
      break;
    case 3:
      // lower right
      v2.scale(-1);
      break;
    case 4:
      // lower left
      v1.scale(-1);
      break;
    }
    x.cross(v1, v2);
    v.cross(x, v1);
    return Qd.getQuaternionFrame((P3d) null, v, x).inv();
  }

  private P3d[] getScaledCell(boolean withOffset) {
    P3d[] pts  = new P3d[8];
    P3d cell0 = null;
    P3d cell1 = null;
    if (withOffset && unitCellMultiplier != null && unitCellMultiplier.z == 0) {
      cell0 = new P3d();
      cell1 = new P3d();
      ijkToPoint3f((int) unitCellMultiplier.x, cell0, 0, 0);
      ijkToPoint3f((int) unitCellMultiplier.y, cell1, 0, 0);
      cell1.sub(cell0);
    }
    double scale = (unitCellMultiplier == null || unitCellMultiplier.z == 0 ? 1
        : Math.abs(unitCellMultiplier.z));
    for (int i = 0; i < 8; i++) {
      P3d pt = pts[i] = P3d.newP(BoxInfo.unitCubePoints[i]);
      if (cell0 != null) {
        pts[i].add3(cell0.x + cell1.x * pt.x, 
            cell0.y + cell1.y * pt.y,
            cell0.z + cell1.z * pt.z);
      }
      pts[i].scale(scale);
      matrixFractionalToCartesian.rotTrans(pt);
      if (!withOffset)
        pt.sub(cartesianOffset);
    }
    return pts;
  }
  
  String getState() {
    String s = "";
    // unitcell offset {1 1 1}
    if (fractionalOffset != null && fractionalOffset.lengthSquared() != 0)
      s += "  unitcell offset " + Escape.eP(fractionalOffset) + ";\n";
    // unitcell range {444 555 1}
    if (unitCellMultiplier != null)
      s += "  unitcell range " + escapeMultiplier(unitCellMultiplier) + ";\n";
    return s;
  }

  public Tensor getTensor(Viewer vwr, double[] parBorU) {
    /*
     * 
     * returns {Vector3f[3] unitVectors, double[3] lengths} from J.W. Jeffery,
     * Methods in X-Ray Crystallography, Appendix VI, Academic Press, 1971
     * 
     * comparing with Fischer and Tillmanns, Acta Cryst C44 775-776, 1988, these
     * are really BETA values. Note that
     * 
     * T = exp(-2 pi^2 (a*b* U11h^2 + b*b* U22k^2 + c*c* U33l^2 + 2 a*b* U12hk +
     * 2 a*c* U13hl + 2 b*c* U23kl))
     * 
     * (ORTEP type 8) is the same as
     * 
     * T = exp{-2 pi^2^ sum~i~[sum~j~(U~ij~ h~i~ h~j~ a*~i~ a*~j~)]}
     * 
     * http://ndbserver.rutgers.edu/mmcif/dictionaries/html/cif_mm.dic/Items/
     * _atom_site.aniso_u[1][2].html
     * 
     * Ortep: http://www.ornl.gov/sci/ortep/man_pdf.html
     * 
     * Anisotropic temperature factor Types 0, 1, 2, 3, and 10 use the following
     * formula for the complete temperature factor.
     * 
     * Base^(-D(b11h2 + b22k2 + b33l2 + cb12hk + cb13hl + cb23kl))
     * 
     * The coefficients bij (i,j = 1,2,3) of the various types are defined with
     * the following constant settings.
     * 
     * Type 0: Base = e, c = 2, D = 1 
     * Type 1: Base = e, c = 1, D = l 
     * Type 2: Base = 2, c = 2, D = l 
     * Type 3: Base = 2, c = 1, D = l
     * 
     * Anisotropic temperature factor Types 4, 5, 8, and 9 use the following
     * formula for the complete temperature factor, in which a1* , a2*, a3* are
     * reciprocal cell dimensions.
     * 
     * exp[ -D(a1*a1*U11hh + a2*a2*U22kk + a3*a3*U33ll + C a1*a2*U12hk + C a1*a3
     * * U13hl + C a2*a3 * U23kl)]
     * 
     * The coefficients Uij (i,j = 1,2,3) of the various types are defined with
     * the following constant settings.
     * 
     * Type 4: C = 2, D = 1/4 
     * Type 5: C = 1, D = 1/4 
     * Type 8: C = 2, D = 2pi2
     * Type 9: C = 1, D = 2pi2
     * 
     * 
     * For beta, we use definitions at
     * http://www.iucr.org/iucr-top/comm/cnom/adp/finrepone/finrepone.html
     * 
     * that betaij = 2pi^2ai*aj* Uij
     * 
     * So if Type 8 is
     * 
     * exp[ -2pi^2(a1*a1*U11hh + a2*a2*U22kk + a3*a3*U33ll + 2a1*a2*U12hk +
     * 2a1*a3 * U13hl + 2a2*a3 * U23kl)]
     * 
     * then we have
     * 
     * exp[ -pi^2(beta11hh + beta22kk + beta33ll + 2beta12hk + 2beta13hl +
     * 2beta23kl)]
     * 
     * and the betaij should be entered as Type 0.
     */

    Tensor t = ((Tensor) Interface.getUtil("Tensor", vwr, "file"));
    if (parBorU[0] == 0 && parBorU[1] == 0 && parBorU[2] == 0) { // this is iso
      double f = parBorU[7];
      double[] eigenValues = new double[] {f, f, f};
      // sqrt will be taken when converted to lengths later
      // no factor of 0.5 pi^2       
      return t.setFromEigenVectors(unitVectors, eigenValues, "iso", "Uiso=" + f, null);
    }
    t.parBorU = parBorU;
    
    double[] Bcart = new double[6];

    int ortepType = (int) parBorU[6];

    if (ortepType == 12) {
      // macromolecular Cartesian

      Bcart[0] = parBorU[0] * twoP2;
      Bcart[1] = parBorU[1] * twoP2;
      Bcart[2] = parBorU[2] * twoP2;
      Bcart[3] = parBorU[3] * twoP2 * 2;
      Bcart[4] = parBorU[4] * twoP2 * 2;
      Bcart[5] = parBorU[5] * twoP2 * 2;

      parBorU[7] = (parBorU[0] + parBorU[1] + parBorU[3]) / 3;

    } else {

      boolean isFractional = (ortepType == 4 || ortepType == 5
          || ortepType == 8 || ortepType == 9);
      double cc = 2 - (ortepType % 2);
      double dd = (ortepType == 8 || ortepType == 9 || ortepType == 10 ? twoP2
          : ortepType == 4 || ortepType == 5 ? 0.25 
          : ortepType == 2 || ortepType == 3 ? Math.log(2) 
          : 1);
      // types 6 and 7 not supported

      //System.out.println("ortep type " + ortepType + " isFractional=" +
      // isFractional + " D = " + dd + " C=" + cc);
      double B11 = parBorU[0] * dd * (isFractional ? a_ * a_ : 1);
      double B22 = parBorU[1] * dd * (isFractional ? b_ * b_ : 1);
      double B33 = parBorU[2] * dd * (isFractional ? c_ * c_ : 1);
      double B12 = parBorU[3] * dd * (isFractional ? a_ * b_ : 1) * cc;
      double B13 = parBorU[4] * dd * (isFractional ? a_ * c_ : 1) * cc;
      double B23 = parBorU[5] * dd * (isFractional ? b_ * c_ : 1) * cc;

      // set bFactor = (U11*U22*U33)
      parBorU[7] = Math.pow(B11 / twoP2 / a_ / a_ * B22 / twoP2 / b_
          / b_ * B33 / twoP2 / c_ / c_, 0.3333);

      Bcart[0] = a * a * B11 + b * b * cosGamma * cosGamma * B22 + c * c
          * cosBeta * cosBeta * B33 + a * b * cosGamma * B12 + b * c * cosGamma
          * cosBeta * B23 + a * c * cosBeta * B13;
      Bcart[1] = b * b * sinGamma * sinGamma * B22 + c * c * cA_ * cA_ * B33
          + b * c * cA_ * sinGamma * B23;
      Bcart[2] = c * c * cB_ * cB_ * B33;
      Bcart[3] = 2 * b * b * cosGamma * sinGamma * B22 + 2 * c * c * cA_
          * cosBeta * B33 + a * b * sinGamma * B12 + b * c
          * (cA_ * cosGamma + sinGamma * cosBeta) * B23 + a * c * cA_ * B13;
      Bcart[4] = 2 * c * c * cB_ * cosBeta * B33 + b * c * cosGamma * B23 + a
          * c * cB_ * B13;
      Bcart[5] = 2 * c * c * cA_ * cB_ * B33 + b * c * cB_ * sinGamma * B23;

    }

    //System.out.println("UnitCell Bcart=" + Bcart[0] + " " + Bcart[1] + " "
      //  + Bcart[2] + " " + Bcart[3] + " " + Bcart[4] + " " + Bcart[5]);

    return t.setFromThermalEquation(Bcart, Escape.eAD(parBorU));
  }

  UnitCell getUnitCellMultiplied() {
    if (unitCellMultiplier == null || unitCellMultiplier.z > 0 && unitCellMultiplier.z == (int) unitCellMultiplier.z)
      return this;
    if (unitCellMultiplied == null) {
      P3d[] pts = BoxInfo.toOABC(getScaledCell(true), null);
      unitCellMultiplied = fromOABC (pts, false);
    }
    return unitCellMultiplied;
  }

  T3d getUnitCellMultiplier() {
    return unitCellMultiplier;
  }
  
  boolean isStandard() {
    // not allowing {555 666 1} here
    return (unitCellMultiplier == null || unitCellMultiplier.x == unitCellMultiplier.y);    
  }

  P3d[] getUnitCellVectors() {
    M4d m = matrixFractionalToCartesian;
    return new P3d[] { 
        P3d.newP(cartesianOffset),
        P3d.new3(fix000(m.m00), fix000(m.m10), fix000(m.m20)), 
        P3d.new3(fix000(m.m01), fix000(m.m11), fix000(m.m21)), 
        P3d.new3(fix000(m.m02), fix000(m.m12), fix000(m.m22)) };
  }
  
  /**
   * 
   * @param uc
   *        generally this or null
   * @param def
   *        String "abc;offset" or M3d or M4d to origin; if String, can be
   *        preceded by ! for "reverse of". For example,
   *        "!a-b,-5a-5b,-c;7/8,0,1/8" offset is optional, and can be a
   *        definition such as "a=3.40,b=4.30,c=5.02,alpha=90,beta=90,gamma=129"
   * @param retMatrix
   *        if a string, return the 4x4 matrix corresponding to this definition;
   *        may be null to ignore
   * 
   * @return [origin va vb vc]
   */
  public static T3d[] getMatrixAndUnitCell(SimpleUnitCell uc, Object def,
                                           M4d retMatrix) {
    if (def == null)
      def = "a,b,c";
    if (retMatrix == null ? uc == null : !(def instanceof String))
      return null;
    M4d m;
    P3d[] pts = new P3d[4];
    P3d pt = pts[0] = P3d.new3(0, 0, 0);
    pts[1] = P3d.new3(1, 0, 0);
    pts[2] = P3d.new3(0, 1, 0);
    pts[3] = P3d.new3(0, 0, 1);
    M3d m3 = new M3d();
    if (AU.isAD(def)) {
      return setAbcFromParams((double[]) def, pts);
    }
    if (def instanceof String) {
      String sdef = (String) def;
      String strans;
      String strans2 = null;
      if (sdef.indexOf("a=") == 0)
        return setAbc(sdef, null, pts);
      // a,b,c;0,0,0
      // or a+1/2,b,c+1/2
      String[] ret = new String[1];
      int ptc = sdef.indexOf(";");
      if (ptc >= 0) {
        strans = sdef.substring(ptc + 1).trim();
        sdef = sdef.substring(0, ptc);
        // allow mixed
        ret[0] = sdef;
        strans2 = fixABC(ret);
        if (sdef != ret[0]) {
          sdef = ret[0];
        }
      } else if (sdef.equals("a,b,c")) {
        strans = null;
      } else {
        ret[0] = sdef;
        strans = fixABC(ret);
        sdef = ret[0];
      }
      sdef += ";0,0,0";
      while (sdef.startsWith("!!"))
        sdef = sdef.substring(2);
      boolean isRev = sdef.startsWith("!");
      if (isRev)
        sdef = sdef.substring(1);
      if (sdef.startsWith("r;"))
        sdef = SpaceGroup.SET_R + sdef.substring(1);
      Symmetry symTemp = new Symmetry();
      symTemp.setSpaceGroup(false);
      int i = symTemp.addSpaceGroupOperation("=" + sdef, 0);
      if (i < 0)
        return null;
      m = symTemp.getSpaceGroupOperation(i);
      ((SymmetryOperation) m).doFinalize();
      P3d t = new P3d();
      addTrans(strans, t);
      addTrans(strans2, t);
      m.setTranslation(t);
      boolean isABC = (sdef.indexOf("c") >= 0);
      if (isABC) {
        m.transpose33();
      }
      if (isRev) {
        m.invert();
      }
      if (retMatrix != null) {
        retMatrix.setM4(m);
      }
      if (uc == null)
        return pts;
    } else if (def instanceof M3d) {
      m = M4d.newMV((M3d) def, new P3d());
    } else if (def instanceof M4d) {
      m = (M4d) def;
    } else {
      // direct 4x4 Cartesian transform
      m = (M4d) ((Object[]) def)[0];
      m.getRotationScale(m3);
      m.rotTrans(pt);
      uc.toCartesian(pt, false);

      for (int i = 1; i < 4; i++) {
        m3.rotate(pts[i]);
        uc.toCartesian(pts[i], true);
      }
      return pts;
    }

    // everything must happen in the CURRENT frame

    // Note that only the origin is translated;
    // the others are vectors from the origin.

    // this is a point, so we do not ignore offset
    m.getRotationScale(m3);
    m.getTranslation(pt);
    uc.toCartesian(pt, false);
    for (int i = 1; i < 4; i++) {
      m3.rotate(pts[i]);
      // these are vectors, so we ignore offset
      uc.toCartesian(pts[i], true);
    }
    return pts;
  }

  private static void addTrans(String strans, P3d t) {
    if (strans == null)
      return;
    String[] atrans = PT.split(strans, ",");
    double[] ftrans = new double[3];
    if (atrans.length == 3) {
      for (int j = 0; j < 3; j++) {
        String s = atrans[j];
        int sfpt = s.indexOf("/");
        if (sfpt >= 0) {
          ftrans[j] = PT.parseDouble(s.substring(0, sfpt))
              / PT.parseDouble(s.substring(sfpt + 1));
        } else {
          ftrans[j] = PT.parseDouble(s);
        }
      }
    }
    t.add3(ftrans[0], ftrans[1], ftrans[2]);
  }

  private static String fixABC(String[] ret) {
    // checking here for embedded translation
    String[] tokens = PT.split(ret[0], ",");
    if (tokens.length != 3)
      return null;
    String trans = "";
    String abc = "";
    boolean haveT = false;
    for (int i = 0; i < 3; i++) {
      String a = tokens[i];
      int p;
      int n = 0;
      for (p = a.length(); --p >= 0;) {
        char c = a.charAt(p);
        switch (c) {
        default:
          if (c >= 'a')
            p = 0;
          break;
        case '+':
          n = 1;
          //$FALL-THROUGH$
        case '-':
          p = -p;
          break;
        }
      }
      p = -1 - p;
      if (p == 0) {
        trans += ",0";
        abc += "," + a;
      } else {
        haveT = true;
        trans += "," + a.substring(p + n);
        abc += "," + a.substring(0, p);
      }
    }
    ret[0] = abc.substring(1);
    return (haveT ? trans.substring(1) : null);
  }
  
  P3d[] getVertices() {
    return vertices; // does not include offsets
  }
  
  boolean hasOffset() {
    return (fractionalOffset != null && fractionalOffset.lengthSquared() != 0);
  }
  
  void initOrientation(M3d mat) {
    if (mat == null)
      return;
    M4d m = new M4d();
    m.setToM3(mat);
    matrixFractionalToCartesian.mul2(m, matrixFractionalToCartesian);
    matrixCartesianToFractional.setM4(matrixFractionalToCartesian).invert();
    initUnitcellVertices();
  }
  
  private void initUnitcellVertices() {
    if (matrixFractionalToCartesian == null)
      return;
    matrixCtoFNoOffset = M4d.newM4(matrixCartesianToFractional);
    matrixFtoCNoOffset = M4d.newM4(matrixFractionalToCartesian);
    vertices = new P3d[8];
    for (int i = 8; --i >= 0;)
      vertices[i] = (P3d) matrixFractionalToCartesian.rotTrans2(BoxInfo.unitCubePoints[i], new P3d());
  }
  
  boolean isSameAs(double[][] f2c2) {
    if (f2c2 == null)
      return false;
    double[][] f2c = getF2C();
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 4; j++) {
        if (!approx0(f2c[i][j] - f2c2[i][j]))
          return false;
      }
    }
    return true;
  }
        

//    return hashCode() == uc.hashCode();
//    
//    
//    if (uc.unitCellParams.length != unitCellParams.length)
//      return false;
//    for (int i = Math.min(unitCellParams.length, PARAM_SLOP); --i >= 0;)
//      if (unitCellParams[i] != uc.unitCellParams[i]
//          && !(Double.isNaN(unitCellParams[i]) && Double
//              .isNaN(uc.unitCellParams[i])))
//        return false;
//    return (fractionalOffset == null ? !uc.hasOffset()
//        : uc.fractionalOffset == null ? !hasOffset() 
//        : fractionalOffset.distanceSquared(uc.fractionalOffset) == 0);
 
  
  
  public double[][] getF2C() {
    if (f2c == null) {
      f2c = new double[3][4];
      for (int i = 0; i < 3; i++)
        matrixFractionalToCartesian.getRow(i, f2c[i]);
    }
    return f2c;
  }

  /**
   * Used for SELECT UNITCELL and for adding H atoms only.
   * 
   * @param a range on a axis
   * @param b range on b axis
   * @param c range on c axis
   * @param pt
   * @return true if within bounds
   */
  boolean isWithinUnitCell(double a, double b, double c, P3d pt) {
    switch (dimension) {
    case 3:
      if (pt.z < c - 1d - slop || pt.z > c + slop)
        return false;
      //$FALL-THROUGH$
    case 2:
      if (pt.y < b - 1d - slop || pt.y > b + slop)
        return false;
      //$FALL-THROUGH$
    case 1:
    if (pt.x < a - 1d - slop || pt.x > a + slop)
      return false;
    }
    return true;
  }

  /**
   * SpaceGroupFinder only
   * 
   * @param origin
   */
  void setCartesianOffset(T3d origin) {
    cartesianOffset.setT(origin);
    
    matrixFractionalToCartesian.m03 = cartesianOffset.x;
    matrixFractionalToCartesian.m13 = cartesianOffset.y;
    matrixFractionalToCartesian.m23 = cartesianOffset.z;
    boolean wasOffset = hasOffset();
    fractionalOffset = P3d.newP(cartesianOffset);
    matrixCartesianToFractional.rotate(fractionalOffset);
    matrixCartesianToFractional.m03 = -fractionalOffset.x;
    matrixCartesianToFractional.m13 = -fractionalOffset.y;
    matrixCartesianToFractional.m23 = -fractionalOffset.z;
    if (allFractionalRelative) {
      matrixCtoFNoOffset.setM4(matrixCartesianToFractional);
      matrixFtoCNoOffset.setM4(matrixFractionalToCartesian);
    }
    if (!wasOffset && fractionalOffset.lengthSquared() == 0)
      fractionalOffset = null;
    f2c = null;
  }
  
  void setOffset(T3d pt) {
    if (pt == null)
      return;
    unitCellMultiplied = null;
    T4d pt4 = (pt instanceof T4d ? (T4d) pt : null);
    double w = (pt4 == null ? Double.MIN_VALUE : pt4.w);
    boolean isCell555P4 = (w > 999999);
    if (pt4 != null ? 
    		w <= 0 || isCell555P4 
    		: pt.x >= 100 || pt.y >= 100) {
      unitCellMultiplier = (pt.z == 0 && pt.x == pt.y 
    		  && !isCell555P4 ? null 
    				  : isCell555P4 ? P4d.newPt((P4d) pt4) 
    						  : P3d.newP(pt));
      unitCellMultiplied = null;
      if (pt4 == null || pt4.w == 0 || isCell555P4)
        return;
      // pt4.w == -1 from reset, continuing 
    }
    // from "unitcell offset {i j k}"
    if (hasOffset() || pt.lengthSquared() > 0) {
      fractionalOffset = P3d.newP(pt);
    }
    matrixCartesianToFractional.m03 = -pt.x;
    matrixCartesianToFractional.m13 = -pt.y;
    matrixCartesianToFractional.m23 = -pt.z;
    cartesianOffset.setT(pt);
    matrixFractionalToCartesian.rotate(cartesianOffset);
    matrixFractionalToCartesian.m03 = cartesianOffset.x;
    matrixFractionalToCartesian.m13 = cartesianOffset.y;
    matrixFractionalToCartesian.m23 = cartesianOffset.z;
    if (allFractionalRelative) {
      matrixCtoFNoOffset.setM4(matrixCartesianToFractional);
      matrixFtoCNoOffset.setM4(matrixFractionalToCartesian);
    }
    f2c = null;
  }  
  
  /**
   * 
   * @param toPrimitive
   *        or assumed conventional
   * @param type
   *        P, R, A, B, C, I(BCC), or F(FCC)
   * @param uc
   *        either [origin, va, vb, vc] or just [va, vb, vc]
   * @param primitiveToCrystal
   * @return true if successful
   */
  boolean toFromPrimitive(boolean toPrimitive, char type, T3d[] uc,
                                 M3d primitiveToCrystal) {

    // columns are definitions of new coordinates in terms of old

    int offset = uc.length - 3;
    M3d mf = null;
    if (type == 'r' || primitiveToCrystal == null) {
      switch (type) {
      default:
        return false;
      case 'r': // reciprocal
        getReciprocal(uc, uc, 1);
        return true;
      case 'P':
        toPrimitive = true;
        mf = M3d.newA9(new double[] { 1, 0, 0, 0, 1, 0, 0, 0, 1 });
        break;
      case 'A':
        mf = M3d.newA9(new double[] { 1, 0, 0, 0, 0.5d, 0.5d, 0, -0.5d, 0.5d });
        break;
      case 'B':
        mf = M3d.newA9(new double[] { 0.5d, 0, 0.5d, 0, 1, 0, -0.5d, 0, 0.5d });
        break;
      case 'C':
        mf = M3d.newA9(new double[] { 0.5d, 0.5d, 0, -0.5d, 0.5d, 0, 0, 0, 1 });
        break;
      case 'R':
        mf = M3d.newA9(new double[] { 2 / 3d, -1 / 3d, -1 / 3d, 1 / 3d, 1 / 3d,
            -2 / 3d, 1 / 3d, 1 / 3d, 1 / 3d });
        break;
      case 'I':
        mf = M3d.newA9(
            new double[] { -.5d, .5d, .5d, .5d, -.5d, .5d, .5d, .5d, -.5d });
        break;
      case 'F':
        mf = M3d
            .newA9(new double[] { 0, 0.5d, 0.5d, 0.5d, 0, 0.5d, 0.5d, 0.5d, 0 });
        break;
      }
      if (!toPrimitive)
        mf.invert();
    } else {
      mf = M3d.newM3(primitiveToCrystal);
      if (toPrimitive)
        mf.invert();
    }
    // transform vectors a, b, and c
    // true to ignore offests -- this is a lattice vector, so relative
    for (int i = uc.length; --i >= offset;) {
      T3d p = uc[i];
      toFractional(p, true);
      mf.rotate(p);
      toCartesian(p, true);
    }
    return true;
  }

  /**
   * when offset is null, use the current cell, otherwise use the original unit cell
   * 
   * @param pt
   * @param offset
   */
  final void toUnitCell(T3d pt, T3d offset) {
    if (matrixCartesianToFractional == null)
      return;
    if (offset == null) {
      // used redefined unitcell 
      matrixCartesianToFractional.rotTrans(pt);
      unitize(pt);
      matrixFractionalToCartesian.rotTrans(pt);
    } else {
      // ONLY in the single instance of binary operation point % n. No idea what this is about!
      
      // use original unit cell
      // note that this matrix will be the same as matrixCartesianToFractional
      // when allFractionalRelative is set true (isosurfaceMesh special cases only)
      matrixCtoFNoOffset.rotTrans(pt);
      unitize(pt);
      pt.add(offset);
      matrixFtoCNoOffset.rotTrans(pt);
    }
  }

  /**
   * when offset is null, use the current cell, otherwise use the original unit cell
   * 
   * @param pt
   * @param offset
   */
  public final void toUnitCellRnd(T3d pt, T3d offset) {
    if (matrixCartesianToFractional == null)
      return;
    if (offset == null) {
      // used redefined unitcell 
      matrixCartesianToFractional.rotTrans(pt);
      unitizeRnd(pt);
      matrixFractionalToCartesian.rotTrans(pt);
    } else {
      // use original unit cell
      // note that this matrix will be the same as matrixCartesianToFractional
      // when allFractionalRelative is set true (isosurfaceMesh special cases only)
      matrixCtoFNoOffset.rotTrans(pt);
      unitizeRnd(pt);
      pt.add(offset); 
      matrixFtoCNoOffset.rotTrans(pt);
    }
  }

  /**
   * returns [0,1)
   * 
   * @param pt
   */
  void unitize(T3d pt) {
    unitizeDim(dimension, pt);
  }

  /**
   * returns [0,1) with rounding to 0.0001
   * 
   * @param pt
   */
  void unitizeRnd(T3d pt) {
    unitizeDimRnd(dimension, pt, slop);
  }

  /**
   * Create a unit cell compatible with
   * 
   * @param sg
   * @param params
   * @param newParams
   * @param allowSame true to allow same-distance a,b,c for lower-symmetry sg
   * @return true if changes have occurred
   */
  public static boolean createCompatibleUnitCell(SpaceGroup sg, double[] params,
                                                 double[] newParams,
                                                 boolean allowSame) {
    if (newParams == null)
      newParams = params;
    double a = params[0];
    double b = params[1];
    double c = params[2];
    double alpha = params[3];
    double beta = params[4];
    double gamma = params[5];

    int n = (sg == null || sg.itaNumber == null ? 0 : PT.parseInt(sg.itaNumber));
    boolean toHex = (n != 0 && isHexagonalSG(n, null));
    boolean isHex = (toHex && isHexagonalSG(-1, params));
    boolean toRhom = (n != 0 && sg.axisChoice == 'r');
    boolean isRhom = (toRhom && isRhombohedral(params));
    if (toHex && isHex || toRhom && isRhom) {
      allowSame = true;
    }
    if (n > (allowSame ? 2 : 0)) {

      boolean absame = approx0(a - b);
      boolean bcsame = approx0(b - c);
      boolean acsame = approx0(c - a);
      boolean albesame = approx0(alpha - beta);
      boolean begasame = approx0(beta - gamma);
      boolean algasame = approx0(gamma - alpha);

      if (!allowSame) {
        // make a, b, and c all distinct
        if (a > b) {
          double d = a;
          a = b;
          b = d;
        }
        bcsame = approx0(b - c);
        if (bcsame)
          c = b * 1.5d;
        absame = approx0(a - b);
        if (absame)
          b = a * 1.2d;
        acsame = approx0(c - a);
        if (acsame)
          c = a * 1.1d;

        // make alpha, beta, and gamma all distinct

        if (approx0(alpha - 90)) {
          alpha = 80;
        }
        if (approx0(beta - 90)) {
          beta = 100;
        }
        if (approx0(gamma - 90)) {
          gamma = 110;
        }
        if (alpha > beta) {
          double d = alpha;
          alpha = beta;
          beta = d;
        }
        albesame = approx0(alpha - beta);
        begasame = approx0(beta - gamma);
        algasame = approx0(gamma - alpha);

        if (albesame) {
          beta = alpha * 1.2d;
        }
        if (begasame) {
          gamma = beta * 1.3d;
        }
        if (algasame) {
          gamma = alpha * 1.4d;
        }
      }
      if (toHex) {
        if (toRhom ? isRhom
            : isHex) {
          // nothing to do
        } else if (sg.axisChoice == 'r') {
          c = b = a;
          if (!allowSame && alpha > 85 && alpha < 95)
            alpha = 80;
          gamma = beta = alpha;
        } else {
          b = a;
          alpha = beta = 90;
          gamma = 120;
        }
      } else if (n >= 195) {
        // cubic
        c = b = a;
        alpha = beta = gamma = 90;
      } else if (n >= 75) {
        // tetragonal
        b = a;
        if (acsame && !allowSame)
          c = a * 1.5d;
        alpha = beta = gamma = 90;
      } else if (n >= 16) {
        // orthorhombic
        alpha = beta = gamma = 90;
      } else if (n >= 3) {
        // monoclinic
        switch (sg.uniqueAxis) {
        case 'a':
          beta = gamma = 90;
          break;
        default:
        case 'b':
          alpha = gamma = 90;
          break;
        case 'c':
          alpha = beta = 90;
          break;
        }
      }
    }
    boolean isNew = !(a == params[0] && b == params[1] && c == params[2]
        && alpha == params[3] && beta == params[4] && gamma == params[5]);

    newParams[0] = a;
    newParams[1] = b;
    newParams[2] = c;
    newParams[3] = alpha;
    newParams[4] = beta;
    newParams[5] = gamma;
    return isNew;
  }

  public static boolean isHexagonalSG(int n, double[] params) {
    return (n < 1 ? isHexagonal(params)
        : n >= 143 && n <= 194);
  }
  
  public static boolean isMonoclinicSG(int n) {
    return (n >= 3 && n <= 15);
  }
  
  public static boolean isTetragonalSG(int n) {
    return (n >= 75 && n <= 142);
  }
  
  public static boolean isPolarSG(int n) {
    return ( n == 1 // 1
        || n >= 3 && n <= 5 // 2
        || n >= 6 && n <= 9 // m
        || n >= 25 && n <= 46 // 2mm
        || n >= 75 && n <= 80 // 4
        || n >= 99 && n <= 110 // 4mm
        || n >= 143 && n <= 146 // 3
        || n >= 156 && n <= 161 // 3mm
        || n >= 168 && n <= 173 // 6
        || n >= 183 && n <= 186 // 6mm
        );
  }
  
  private static void removeDuplicates(Lst<P3d> list, int i0, int n0, int n) {
    if (n < 0)
      n = list.size();
    for (int i = i0; i < n; i++) {
      P3d p = list.get(i);
      for (int j = Math.max(i + 1, n0); j < n; j++) {
        if (list.get(j).distanceSquared(p) < JC.UC_TOLERANCE2) {
          list.removeItemAt(j);
          n--;
          j--;
        }
      }
    }
  }

}
