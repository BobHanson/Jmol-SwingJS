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

import javajs.util.A4d;
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
  
  private Lst<String> moreInfo;
  
  public String name = "";
  
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

  private static V3d v0;


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

  private double fix00000(double x) {
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
   *        the first point that is to be duplicated; 
   * @param dup0 
   *        start for checking for removing duplicates, either i0 or no?
   * @param periodicity 
   * @return augmented list
   */
  Lst<P3d> getEquivPoints(P3d pt, String flags, M4d[] ops, Lst<P3d> list,
                                int i0, int n0, int dup0, int periodicity) {
    boolean fromfractional = (flags.indexOf("fromfractional") >= 0);
    boolean tofractional = (flags.indexOf("tofractional") >= 0);
    boolean packed = (flags.indexOf("packed") >= 0);
    if (list == null)
      list = new Lst<P3d>();
    P3d pf = P3d.newP(pt);
    if (!fromfractional)
      toFractional(pf, true);
    int n = list.size();
    boolean adjustA = ((periodicity & 0x1) != 0);
    boolean adjustB = ((periodicity & 0x2) != 0);
    boolean adjustC = ((periodicity & 0x4) != 0);
    for (int i = 0, nops = ops.length; i < nops; i++) {
      P3d p = P3d.newP(pf);
      ops[i].rotTrans(p);
      //not using unitize here, because it does some averaging
      if (adjustA)
        p.x =  fixFloor(p.x - Math.floor(p.x));
      if (adjustB)
        p.y =  fixFloor(p.y - Math.floor(p.y));
      if (adjustC)
        p.z =  fixFloor(p.z - Math.floor(p.z));
      list.addLast(p);
      n++;
    }
    if (packed) {
      // but when we lack periodicity, as in a layer group,
      // we need to duplicate based on 0.5!!! 
      // duplicate all the points. 
      if (!adjustC) {
        P3d offset = P3d.new3(0, 0, 0.5);
        for (int i = n0; i < n; i++) {
          list.get(i).add(offset);
        }          
      }
      if (!adjustB) {
        P3d offset = P3d.new3(0, 0.5, 0);
        for (int i = n0; i < n; i++) {
          list.get(i).add(offset);
        }          
      }
      if (!adjustA) {
        P3d offset = P3d.new3(0.5, 0, 0);
        for (int i = n0; i < n; i++) {
          list.get(i).add(offset);
        }          
      }
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
      n = list.size();
      if (!adjustA) {
        P3d offset = P3d.new3(-0.5, 0, 0);
        for (int i = n0; i < n; i++) {
          list.get(i).add(offset);
        }          
      }
      if (!adjustB) {
        n = list.size();
        P3d offset = P3d.new3(0, -0.5, 0);
        for (int i = n0; i < n; i++) {
          list.get(i).add(offset);
        }          
      }
      if (!adjustC) {
        n = list.size();
        P3d offset = P3d.new3(0, 0, -0.5);
        for (int i = n0; i < n; i++) {
          list.get(i).add(offset);
        }          
      }
    }
    removeDuplicates(list, i0, dup0, -1);
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
    return getScaledCellMult(null, withOffset);
  }
  
  private P3d[] getScaledCellMult(T3d mult, boolean withOffset) {
    P3d[] pts  = new P3d[8];
    P3d cell0 = null;
    P3d cell1 = null;
    boolean isFrac = (mult != null);
    if (!isFrac)
      mult = this.unitCellMultiplier;
    if (withOffset && mult != null && mult.z == 0) {
      cell0 = new P3d();
      cell1 = new P3d();
      ijkToPoint3f((int) mult.x, cell0, 0, 0);
      ijkToPoint3f((int) mult.y, cell1, 0, 0);
      cell1.sub(cell0);
    }
    double scale = (isFrac || mult == null || mult.z == 0 ? 1
        : Math.abs(mult.z));
    for (int i = 0; i < 8; i++) {
      P3d pt = pts[i] = P3d.newP(BoxInfo.unitCubePoints[i]);
      if (cell0 != null) {
        pts[i].add3(cell0.x + cell1.x * pt.x, 
            cell0.y + cell1.y * pt.y,
            cell0.z + cell1.z * pt.z);
      } else if (isFrac) {
        pt.scaleT(mult);
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
        P3d.new3(fix00000(m.m00), fix00000(m.m10), fix00000(m.m20)), 
        P3d.new3(fix00000(m.m01), fix00000(m.m11), fix00000(m.m21)), 
        P3d.new3(fix00000(m.m02), fix00000(m.m12), fix00000(m.m22)) };
  }
  
  public static M4d toTrm(String transform, M4d trm) {
    if (trm == null)
      trm = new M4d();
    getMatrixAndUnitCell(null, null, transform, trm);
    return trm;
  }

  /**
   * 
   * @param vwr 
   * @param uc
   *        generally this or null
   * @param def
   *        String "abc;offset" or M3d or M4d or Object[M4d]; if String, can be
   *        preceded by ! for "reverse of". For example,
   *        "!a-b,-5a-5b,-c;7/8,0,1/8" offset is optional, and can be a
   *        definition such as "a=3.40,b=4.30,c=5.02,alpha=90,beta=90,gamma=129"
   *        also allows for reciprocal lattice a*, b*, c
   * @param retMatrix
   *        if a string, return the 4x4 matrix corresponding to this definition;
   *        may be null to ignore
   * @return [origin va vb vc]
   */
  public static T3d[] getMatrixAndUnitCell(Viewer vwr, SimpleUnitCell uc, Object def,
                                           M4d retMatrix) {
    if (def == null)
      def = "a,b,c";
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
    boolean isString = def instanceof String;
    if (isString && ((String) def).indexOf("(") >= 0)
      def = SimpleUnitCell.parseSimpleMath(vwr, (String) def);
    if (isString && ((String) def).charAt(0) == '[') {
      // string representation of matrix
      // generate matrix, save if nec. and continue on
      def = Escape.unescapeMatrixD((String) def);
      if (def instanceof M3d) {
        def = M4d.newMV((M3d) def, new P3d());
      } else if (!(def instanceof M4d)) {
        return null;
      }
      if (retMatrix != null) {
        retMatrix.setM4((M4d) def);
        retMatrix = null;
      }
      isString = false;
    }
    if (isString) {
      String sdef = (String) def;
      String strans;
      String strans2 = null;
      if (sdef.indexOf("a=") == 0)
        return setAbc(sdef, null, pts);
      // a,b,c;0,0,0
      // or a+1/2,b,c+1/2
      // or !b,c,a>a-c,b,2c;0,0,1/2>a,-a-c,b
      if (sdef.indexOf(">") > 0) {
        // must have return matrix and no unit cell
        if (uc != null || retMatrix == null)
          return null;
        String[] sdefs = sdef.split(">");
        retMatrix.setIdentity();
        M4d m4 = new M4d();
        for (int i = sdefs.length; --i >= 0;) {
          getMatrixAndUnitCell(null, null, sdefs[i], m4);
          retMatrix.mul2(m4, retMatrix);
        }
        return pts; // just not null
      }
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
        if (sdef.indexOf("w") > 0) {
          sdef = sdef.replace('u', 'x').replace('v', 'y').replace('w', 'z');
        }
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
      if (sdef.equals("r;0,0,0"))
        sdef = HEX_TO_RHOMB + sdef.substring(1);
      else if (sdef.equals("h;0,0,0"))
        sdef = RHOMB_TO_HEX + sdef.substring(1);

      boolean isABC = sdef.indexOf("x") < 0 && (sdef.indexOf("a") >= 0
          || sdef.indexOf("b") >= 0 || sdef.indexOf("c") >= 0);
      if (isABC && sdef.indexOf("*") >= 0 && uc != null) {
        // These will have been ignored;
        // get the reciprocal lattice.
        // 
        M4d mSpinPp = M4d.newM4(null);
        T3d[] oabc = getMatrixAndUnitCell(vwr, uc, "a,b,c", mSpinPp);
        getMatrixAndUnitCell(null, null, sdef.replace('*', ' '), mSpinPp);
        // this is in xyz, but we need it in abc
        boolean[] flags = new boolean[] { (sdef.indexOf("a*") >= 0),
            (sdef.indexOf("b*") >= 0), (sdef.indexOf("c*") >= 0) };
        m = M4d.newM4(null);
        adjustForReciprocal(uc, oabc, mSpinPp, flags, m, pts);
        uc = null; // pts are already set
      } else {
        Symmetry symTemp = new Symmetry(); // no vwr here
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
      }
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
    } else if (retMatrix != null || uc == null) {
      return null;
    } else if (def instanceof M3d) {
      m = M4d.newMV((M3d) def, new P3d());
    } else if (def instanceof M4d) {
      m = (M4d) def;
    } else {
      // Object[M4d] from Modelkit only
      // direct 4x4 Cartesian transform
      m = (M4d) ((Object[]) def)[0];
      m.getRotationScale(m3);
      m.rotTrans(pt);
      // false here to return the actual new origin
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

  /**
   * Just replace a with a*, b with b*, c with c*. 
   * Note if one a* is in the formula, then ALL a are a*. 
   * Does not support a,b,a* for example.
   * 
   * @param uc 
   * @param oabc
   *        the Cartesian cell lattice vectors o, a, b, c
   * @param mSpinPp
   *        the coefficients for the spin setting ignoring asterisks
   * @param flags
   *        true for a*, b*, or c
   * @param mRet
   *        the array to fill 
   * @param pts
   *        the Cartesian result o a' b' c'
   *
   */
  private static void adjustForReciprocal(SimpleUnitCell uc, T3d[] oabc,
                                          M4d mSpinPp, boolean[] flags, M4d mRet,
                                          P3d[] pts) {
    T3d[] recipOABC = new T3d[4];
    getReciprocal(oabc, recipOABC, 1);
    P3d t = new P3d();
    double[] abc = new double[4];
    for (int i = 1; i <= 3; i++) {
      P3d vnew = new P3d();
      mSpinPp.getColumn(i - 1, abc);
      for (int j = 0; j < 3; j++) {
        if (abc[j] == 0)
          continue;
        vnew.scaleAdd(abc[j], (flags[j] ? recipOABC[1 + j] : oabc[1 + j]), vnew);
      }
      pts[i] = vnew;
      t.setP(vnew);
      uc.toFractional(t, true);
      mRet.setColumn4(i - 1, fixZero(t.x, 1e-10), fixZero(t.y, 1e-10), fixZero(t.z, 1e-10), 0);
    }        
  }

  
  private static double fixZero(double x, double err) {
    return (Math.abs(x) < err ? 0 : x);
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
  
  private boolean hasOffset() {
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
        //$FALL-THROUGH$
      case 'A':
      case 'B':
      case 'C':
      case 'R':
      case 'I':
      case 'F':
        mf = getPrimitiveTransform(type);
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

  static M3d getPrimitiveTransform(char type) {
    switch (type) {
    case 'P':
      return M3d.newA9(new double[] { 1, 0, 0, 0, 1, 0, 0, 0, 1 });
    case 'A':
      return M3d.newA9(new double[] { 1, 0, 0, 0, 0.5d, 0.5d, 0, -0.5d, 0.5d });
    case 'B':
      return M3d.newA9(new double[] { 0.5d, 0, 0.5d, 0, 1, 0, -0.5d, 0, 0.5d });
    case 'C':
      return M3d.newA9(new double[] { 0.5d, 0.5d, 0, -0.5d, 0.5d, 0, 0, 0, 1 });
    case 'R':
      return M3d.newA9(new double[] { 2 / 3d, -1 / 3d, -1 / 3d, 1 / 3d, 1 / 3d,
          -2 / 3d, 1 / 3d, 1 / 3d, 1 / 3d });
    case 'I':
      return M3d.newA9(
          new double[] { -.5d, .5d, .5d, .5d, -.5d, .5d, .5d, .5d, -.5d });
    case 'F':
      return M3d
          .newA9(new double[] { 0, 0.5d, 0.5d, 0.5d, 0, 0.5d, 0.5d, 0.5d, 0 });
    }
    return null;
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

  /**
   * Takes into account subperiodic groups using BoxInfo.unitCubePoints
   * 
   * P3d.new3(0, 0, 0), // 0
   * 
   * P3d.new3(0, 0, 1), // 1 c
   *
   * P3d.new3(0, 1, 0), // 2 b
   * 
   * P3d.new3(0, 1, 1), // 3 bc
   * 
   * P3d.new3(1, 0, 0), // 4 a
   * 
   * P3d.new3(1, 0, 1), // 5 ac
   * 
   * P3d.new3(1, 1, 0), // 6 ab
   * 
   * P3d.new3(1, 1, 1), // 7 }; abc
   * 
   * 
   * @param periodicity 
   * @return center
   */
  public P3d getCenter(int periodicity) {
    P3d center = new P3d();
    P3d off = getCartesianOffset();
    P3d[] pts = getVertices();
    // 0x1 a 0x2 b 0x4 c
    // possibilities include abc (space), 
    // ab (0,2,3,6 layer, plane), 
    // c (0,2 rod), and 
    // a (0,4 frieze) 
    int j2, jd;
    switch (periodicity) {
    default:
    case 0x7:
      j2 = 8;
      jd = 1;
      break;
    case 0x3: // ab
      j2 = 8;
      jd = 2;
      break;
    case 0x4: // c
      j2 = 2;
      jd = 1;
      break;
    case 0x2: // b
      j2 = 3;
      jd = 2;
      break;
    case 0x1: // a
      j2 = 8;
      jd = 4;
      break;
    }
    int n = 0;
    for (int j = 0; j < j2; j += jd) {
      center.add(pts[j]);
      center.add(off);
      n++;
    }
    center.scale(1d / n);
    return center;
  }

  public void setSpinAxisAngle(A4d aa) {
    if (moreInfo == null) {
      moreInfo = new Lst<>();
    }
    String s = "rotation_axis_xyz=";
    String a = "rotation_angle=";
    int ptAxis = -1, ptAngle = -1;
    for (int i = moreInfo.size(); --i >= 0 && (ptAxis < 0 || ptAngle < 0);) {
      String s0 = moreInfo.get(i);
      if (s0.startsWith(s)) {
        ptAxis = i;
      } else if (s0.startsWith(a)) {
        ptAngle = i;
      }
    }
    Qd q = Qd.newAA(aa);
    V3d v = new V3d();
    if (v0 == null)
      v0 = V3d.new3(Math.PI, Math.E, Math.sqrt(2));
    v = q.getNormalDirected(v0);
    V3d.newV(aa);
    toFractional(v, true);
    v.normalize();
    double f = getAxisMultiplier(v);
    s += "" + Math.round(v.x * f) + "," + Math.round(v.y * f) + "," + Math.round(v.z * f);
    if (ptAxis < 0) {
      moreInfo.addLast(s);
      if (ptAngle > ptAxis)
        ptAngle++;
    } else {
      moreInfo.set(ptAxis, s);
    }
    f = Math.round(q.getThetaDirectedV(v0) * 1000) / 1000;
    s = a + (f == Math.round(f) ? "" + Math.round(f) : round000(f));
    if (ptAngle < 0) {
      moreInfo.addLast(s);
    } else {
      moreInfo.set(ptAngle, s);
    }
  }

  private double getAxisMultiplier(V3d v) {
    if (approx00(v.x - Math.round(v.x)) && approx00(v.y - Math.round(v.y))
        && approx00(v.z - Math.round(v.z))) {
      return 1;
    }
    double d = Math.min(approx00(v.x) ? 10000 : Math.abs(v.x),
        approx00(v.y) ? 10000
            : Math.min(Math.abs(v.y), approx00(v.z) ? 10000 : Math.abs(v.z)));
    if (approx01(v.x / d) && approx01(v.y / d) && approx01(v.z / d)) {
      return 1 / d;
    }
    return 1000;
  }

  private static boolean approx01(double f) {
    f = f % 1;
    return (approx00(f) || Math.abs(f) > 0.999d);
  }

  private static boolean approx00(double f) {
    return (Math.abs(f) < 0.001d);
  }

  private String round000(double y) {
    y = Math.round(y*1000)/1000d;
    return (y == Math.round(y) ? "" + Math.round(y) : "" + y);
  }

  void setMoreInfo(Lst<String> info) {
    moreInfo = info;
  }

  Lst<String> getMoreInfo() {
    return moreInfo;
  }
}
