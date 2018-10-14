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

import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.P3;
import javajs.util.P3i;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.Quat;
import javajs.util.T3;
import javajs.util.T4;
import javajs.util.V3;

import org.jmol.api.Interface;
import org.jmol.util.BoxInfo;
import org.jmol.util.Escape;
import org.jmol.util.SimpleUnitCell;
import org.jmol.util.Tensor;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

/**
 * a class private to the org.jmol.symmetry package
 * to be accessed only through the SymmetryInterface API
 * 
 * adds vertices and offsets orientation, 
 * and a variety of additional calculations that in 
 * principle could be put in SimpleUnitCell
 * if desired, but for now are in this optional package.
 * 
 */

class UnitCell extends SimpleUnitCell {
  
  private P3[] vertices; // eight corners
  private P3 fractionalOffset;
  /**
   * this flag TRUE causes an update of matrixCtoFNoOffset each time an offset is changed
   * so that it is updated and the two stay the same; set true only for JmolData, tensors, and isosurfaceMesh
   * 
   * it is no longer clear to me exactly why this is necessary, and perhaps it is not for some of these
   *  
   *
   */
  private boolean allFractionalRelative;
  
  protected final P3 cartesianOffset = new P3();
  protected T3 unitCellMultiplier;
  public Lst<String> moreInfo;
  public String name = "";
  
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
  static UnitCell fromOABC(T3[] oabc, boolean setRelative) {
    UnitCell c = new UnitCell();
    if (oabc.length == 3) // not used
      oabc = new T3[] { new P3(), oabc[0], oabc[1], oabc[2] };
    float[] parameters = new float[] { -1, 0, 0, 0, 0, 0, oabc[1].x,
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
   * @param setRelative only set true for JmolData and tensors
   * @return a new unit cell
   */
  public static UnitCell fromParams(float[] params, boolean setRelative) {
    UnitCell c = new UnitCell();
    c.init(params);
    c.initUnitcellVertices();
    c.allFractionalRelative = setRelative;
    return  c;
  }

  void initOrientation(M3 mat) {
    if (mat == null)
      return;
    M4 m = new M4();
    m.setToM3(mat);
    matrixFractionalToCartesian.mul2(m, matrixFractionalToCartesian);
    matrixCartesianToFractional.setM4(matrixFractionalToCartesian).invert();
    initUnitcellVertices();
  }

  /**
   * when offset is null, use the current cell, otherwise use the original unit cell
   * 
   * @param pt
   * @param offset
   */
  final void toUnitCell(T3 pt, T3 offset) {
    if (matrixCartesianToFractional == null)
      return;
    if (offset == null) {
      // used redefined unitcell 
      matrixCartesianToFractional.rotTrans(pt);
      unitize(pt);
      matrixFractionalToCartesian.rotTrans(pt);
    } else {
      // use original unit cell
      // note that this matrix will be the same as matrixCartesianToFractional
      // when allFractionalRelative is set true (special cases only)
      matrixCtoFNoOffset.rotTrans(pt);
      unitize(pt);
      pt.add(offset); 
      matrixFtoCNoOffset.rotTrans(pt);
    }
  }
  
  public void unitize(T3 pt) {
    switch (dimension) {
    case 3:
      pt.z = toFractionalX(pt.z);  
      //$FALL-THROUGH$
    case 2:
      pt.y = toFractionalX(pt.y);
      //$FALL-THROUGH$
    case 1:
      pt.x = toFractionalX(pt.x);
    }
  }

  public void reset() {
    unitCellMultiplier = null;
    setOffset(P3.new3(0, 0, 0));
  }
  
  void setOffset(T3 pt) {
    if (pt == null)
      return;
    T4 pt4 = (pt instanceof T4 ? (T4) pt : null);
    boolean isCell555P4 = (pt4 != null && pt4.w > 999999);
    if (pt4 != null ? pt4.w <= 0 || isCell555P4 : pt.x >= 100 || pt.y >= 100) {
      // from "unitcell range {ijk ijk scale}"
      //   or "unitcell range {1iiijjjkkk 1iiijjjkkk scale}"
      //     where we have encoded this as a P4: {1iiijjjkkk 1iiijjjkkk scale 1kkkkkk}
      //   or "unitcell reset"
      unitCellMultiplier = (pt.z == 0 && pt.x == pt.y && !isCell555P4 ? null : isCell555P4 ? P4.newPt((P4) pt4) : P3.newP(pt));
      if (pt4 == null || pt4.w == 0 || isCell555P4)
        return;
      // from reset, continuing 
    }
    // from "unitcell offset {i j k}"
    if (hasOffset() || pt.lengthSquared() > 0) {
      fractionalOffset = new P3();
      fractionalOffset.setT(pt);
    }
    matrixCartesianToFractional.m03 = -pt.x;
    matrixCartesianToFractional.m13 = -pt.y;
    matrixCartesianToFractional.m23 = -pt.z;
    cartesianOffset.setT(pt);
    matrixFractionalToCartesian.m03 = 0;
    matrixFractionalToCartesian.m13 = 0;
    matrixFractionalToCartesian.m23 = 0;
    matrixFractionalToCartesian.rotTrans(cartesianOffset);
    matrixFractionalToCartesian.m03 = cartesianOffset.x;
    matrixFractionalToCartesian.m13 = cartesianOffset.y;
    matrixFractionalToCartesian.m23 = cartesianOffset.z;
    if (allFractionalRelative) {
      matrixCtoFNoOffset.setM4(matrixCartesianToFractional);
      matrixFtoCNoOffset.setM4(matrixFractionalToCartesian);
    }
  }

  private void setCartesianOffset(T3 origin) {
    cartesianOffset.setT(origin);
    matrixFractionalToCartesian.m03 = cartesianOffset.x;
    matrixFractionalToCartesian.m13 = cartesianOffset.y;
    matrixFractionalToCartesian.m23 = cartesianOffset.z;
    boolean wasOffset = hasOffset();
    fractionalOffset = new P3();
    fractionalOffset.setT(cartesianOffset);
    matrixCartesianToFractional.m03 = 0;
    matrixCartesianToFractional.m13 = 0;
    matrixCartesianToFractional.m23 = 0;
    matrixCartesianToFractional.rotTrans(fractionalOffset);
    matrixCartesianToFractional.m03 = -fractionalOffset.x;
    matrixCartesianToFractional.m13 = -fractionalOffset.y;
    matrixCartesianToFractional.m23 = -fractionalOffset.z;
    if (allFractionalRelative) {
      matrixCtoFNoOffset.setM4(matrixCartesianToFractional);
      matrixFtoCNoOffset.setM4(matrixFractionalToCartesian);
    }
    if (!wasOffset && fractionalOffset.lengthSquared() == 0)
      fractionalOffset = null;
  }

  Map<String, Object> getInfo() {
    Map<String, Object> info = new Hashtable<String, Object>();
    info.put("params", unitCellParams);
    info.put("vectors", getUnitCellVectors());
    info.put("volume", Double.valueOf(volume));
    info.put("matFtoC", matrixFractionalToCartesian);
    info.put("matCtoF", matrixCartesianToFractional);
    return info;
  }
  
  String dumpInfo(boolean isFull) {
    return "a=" + a + ", b=" + b + ", c=" + c + ", alpha=" + alpha + ", beta=" + beta + ", gamma=" + gamma
       + "\n" + Escape.eAP(getUnitCellVectors())
       + "\nvolume=" + volume
       + (isFull ? "\nfractional to cartesian: " + matrixFractionalToCartesian 
       + "\ncartesian to fractional: " + matrixCartesianToFractional : "");
  }

  P3[] getVertices() {
    return vertices; // does not include offsets
  }
  
  P3 getCartesianOffset() {
    // for slabbing isosurfaces and rendering the ucCage
    return cartesianOffset;
  }
  
  P3 getFractionalOffset() {
    return fractionalOffset;
  }
  
  final private static double twoP2 = 2 * Math.PI * Math.PI;

  private final static V3[] unitVectors = {
    JC.axisX, JC.axisY, JC.axisZ};
  
  Tensor getTensor(Viewer vwr, float[] parBorU) {
    /*
     * 
     * returns {Vector3f[3] unitVectors, float[3] lengths} from J.W. Jeffery,
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
      float f = parBorU[7];
      float[] eigenValues = new float[] {f, f, f};
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
      parBorU[7] = (float) Math.pow(B11 / twoP2 / a_ / a_ * B22 / twoP2 / b_
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

    return t.setFromThermalEquation(Bcart, Escape.eAF(parBorU));
  }
  
  /**
   * 
   * @param scale
   * @param withOffset
   * @return points in Triangulator order
   */
  P3[] getCanonicalCopy(float scale, boolean withOffset) {
    P3[] pts  = new P3[8];
    P3 cell0 = null;
    P3 cell1 = null;
    if (withOffset && unitCellMultiplier != null) {
      cell0 = new P3();
      cell1 = new P3();
      ijkToPoint3f((int) unitCellMultiplier.x, cell0, 0, 0);
      ijkToPoint3f((int) unitCellMultiplier.y, cell1, 0, 0);
      cell1.sub(cell0);
    }
    for (int i = 0; i < 8; i++) {
      P3 pt = pts[i] = P3.newP(BoxInfo.unitCubePoints[i]);
      if (cell0 != null) {
        scale *= (unitCellMultiplier.z == 0 ? 1 : unitCellMultiplier.z);
        pts[i].add3(cell0.x + cell1.x * pt.x, 
            cell0.y + cell1.y * pt.y,
            cell0.z + cell1.z * pt.z);
      }
      matrixFractionalToCartesian.rotTrans(pt);
      if (!withOffset)
        pt.sub(cartesianOffset);
    }
    return BoxInfo.getCanonicalCopy(pts, scale);
  }

  /// private methods
  
  
  private static float toFractionalX(float x) {
    // introduced in Jmol 11.7.36
    x = (float) (x - Math.floor(x));
    if (x > 0.9999f || x < 0.0001f) 
      x = 0;
    return x;
  }
  
  private void initUnitcellVertices() {
    if (matrixFractionalToCartesian == null)
      return;
    matrixCtoFNoOffset = M4.newM4(matrixCartesianToFractional);
    matrixFtoCNoOffset = M4.newM4(matrixFractionalToCartesian);
    vertices = new P3[8];
    for (int i = 8; --i >= 0;)
      vertices[i] = (P3) matrixFractionalToCartesian.rotTrans2(BoxInfo.unitCubePoints[i], new P3());
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
  public boolean checkDistance(P3 f1, P3 f2, float distance, float dx,
                              int iRange, int jRange, int kRange, P3 ptOffset) {
    P3 p1 = P3.newP(f1);
    toCartesian(p1, true);
    for (int i = -iRange; i <= iRange; i++)
      for (int j = -jRange; j <= jRange; j++)
        for (int k = -kRange; k <= kRange; k++) {
          ptOffset.set(f2.x + i, f2.y + j, f2.z + k);
          toCartesian(ptOffset, true);
          float d = p1.distance(ptOffset);
          if (dx > 0 ? Math.abs(d - distance) <= dx : d <= distance && d > 0.1f) {
            ptOffset.set(i, j, k);
            return true;
          }
        }
    return false;
  }

  public T3 getUnitCellMultiplier() {
    return unitCellMultiplier;
  }

  public P3[] getUnitCellVectors() {
    M4 m = matrixFractionalToCartesian;
    return new P3[] { 
        P3.newP(cartesianOffset),
        P3.new3(fix(m.m00), fix(m.m10), fix(m.m20)), 
        P3.new3(fix(m.m01), fix(m.m11), fix(m.m21)), 
        P3.new3(fix(m.m02), fix(m.m12), fix(m.m22)) };
  }

  private float fix(float x) {
    return (Math.abs(x) < 0.001f ? 0 : x);
  }

  public boolean isSameAs(UnitCell uc) {
    if (uc.unitCellParams.length != unitCellParams.length)
      return false;
    for (int i = unitCellParams.length; --i >= 0;)
      if (unitCellParams[i] != uc.unitCellParams[i]
          && !(Float.isNaN(unitCellParams[i]) && Float
              .isNaN(uc.unitCellParams[i])))
        return false;
    return (fractionalOffset == null ? !uc.hasOffset()
        : uc.fractionalOffset == null ? !hasOffset() 
        : fractionalOffset.distanceSquared(uc.fractionalOffset) == 0);
  }

  public boolean hasOffset() {
    return (fractionalOffset != null && fractionalOffset.lengthSquared() != 0);
  }

  public String getState() {
    String s = "";
    // unitcell offset {1 1 1}
    if (fractionalOffset != null && fractionalOffset.lengthSquared() != 0)
      s += "  unitcell offset " + Escape.eP(fractionalOffset) + ";\n";
    // unitcell range {444 555 1}
    if (unitCellMultiplier != null)
      s += "  unitcell range " + escapeMultiplier(unitCellMultiplier) + ";\n";
    return s;
  }

  /**
   * Returns a quaternion that will take the standard frame to a view down a
   * particular axis, expressed as its counterparts.
   * 
   * @param abc
   *        ab bc ca
   * @return quaternion
   */
  public Quat getQuaternionRotation(String abc) {
    T3 a = V3.newVsub(vertices[4], vertices[0]);
    T3 b = V3.newVsub(vertices[2], vertices[0]);
    T3 c = V3.newVsub(vertices[1], vertices[0]);
    T3 x = new V3();
    T3 v = new V3();

    //  qab = !quaternion({0 0 0}, cross(cxb,c), cxb);
    //  qbc = !quaternion({0 0 0}, cross(axc,a), axc)
    //  qca = !quaternion({0 0 0}, cross(bxa,b), bxa);
    //

    int mul = (abc.charAt(0) == '-' ? -1 : 1);
    if (mul < 0)
      abc = abc.substring(1);
    int quadrant = 0;
    if (abc.length() == 2) { // a1 a2 a3 a4 b1 b2 b3 b4...
      quadrant = abc.charAt(1) - 48;
      abc = abc.substring(0, 1);
    }
    boolean isEven = (quadrant % 2 == 0);
    int axis = "abc".indexOf(abc);

    T3 v1, v2;
    switch (axis) {
    case 0:
    default:
      v1 = a;
      v2 = c;
      if (quadrant > 0) {
        if (mul > 0 == isEven) {
          v2 = b;
          v1.scale(-1);
        }
      }
      break;
    case 1: // b
      v1 = b;
      v2 = a;
      if (quadrant > 0) {
        if (mul > 0 == isEven) {
          v2 = c;
          v1.scale(-1);
        }
      }
      break;
    case 2: // c
      v1 = c;
      v2 = a;
      if (quadrant > 0) {
        quadrant = 5 - quadrant;
        if (mul > 0 != isEven) {
          v2 = b;
          v1.scale(-1);
        }
      }
      break;
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
    return Quat.getQuaternionFrame(null, v, x).inv();
  }

  /**
   * 
   * @param def
   *        String "abc;offset" or M3 or M4 to origin; if String, can be
   *        preceded by ! for "reverse of". For example,
   *        "!a-b,-5a-5b,-c;7/8,0,1/8" offset is optional,
   *        and can be a definition such as "a=3.40,b=4.30,c=5.02,alpha=90,beta=90,gamma=129"
   * 
   * @return [origin va vb vc]
   */
  public T3[] getV0abc(Object def) {
    if (def instanceof T3[])
      return (T3[]) def;
    M4 m;
    boolean isRev = false;
    V3[] pts = new V3[4];
    V3 pt = pts[0] = V3.new3(0, 0, 0);
    pts[1] = V3.new3(1, 0, 0);
    pts[2] = V3.new3(0, 1, 0);
    pts[3] = V3.new3(0, 0, 1);
    M3 m3 = new M3();
    if (def instanceof String) {
      String sdef = (String) def;
      String strans = "0,0,0";
      if (sdef.indexOf("a=") == 0)
        return setOabc(sdef, null, pts);
      // a,b,c;0,0,0
      int ptc = sdef.indexOf(";");
      if (ptc >= 0) {
        strans = sdef.substring(ptc + 1);
        sdef = sdef.substring(0, ptc);
      }
      sdef += ";0,0,0";
      isRev = sdef.startsWith("!");
      if (isRev)
        sdef = sdef.substring(1);
      Symmetry symTemp = new Symmetry();
      symTemp.setSpaceGroup(false);
      int i = symTemp.addSpaceGroupOperation("=" + sdef, 0);
      if (i < 0)
        return null;
      m = symTemp.getSpaceGroupOperation(i);
      ((SymmetryOperation) m).doFinalize();
      if (strans != null) {
        String[] atrans = PT.split(strans+"0,0,0",",");
        float[] ftrans = new float[3];
        for (int j = 0; j < 3; j++) {
          String s = atrans[j];
          int sfpt = s.indexOf("/"); 
          if (sfpt >= 0) {
            ftrans[j] = PT.parseFloat(s.substring(0, sfpt)) / PT.parseFloat(s.substring(sfpt + 1));
          } else {
            ftrans[j] = PT.parseFloat(s);
          }
        }
        P3 ptrans = P3.new3(ftrans[0], ftrans[1], ftrans[2]);
        m.setTranslation(ptrans);
      }
    } else if (def instanceof M3) {
      m = M4.newMV((M3) def, new P3());
    } else if (def instanceof M4) {
      m = (M4) def;
    } else {
      // direct 4x4 Cartesian transform
      m = (M4) ((Object[]) def)[0];
      m.getRotationScale(m3);
      toCartesian(pt, false);
      m.rotTrans(pt);
      for (int i = 1; i < 4; i++) {
        toCartesian(pts[i], true);
        m3.rotate(pts[i]);
      }
      return pts;
    }   
    
    // We have an operator that may need reversing.
    // Note that translations are limited to 1/2, 1/3, 1/4, 1/6, 1/8.

    m.getRotationScale(m3);
    m.getTranslation(pt);
    if (isRev) {
      m3.invert();
      m3.transpose();
      m3.rotate(pt);
      pt.scale(-1);
    } else {
      m3.transpose();
    }

    // Note that only the origin is translated;
    // the others are vectors from the origin.

    // this is a point, so we do not ignore offset
    toCartesian(pt, false);
    for (int i = 1; i < 4; i++) {
      m3.rotate(pts[i]);
      // these are vectors, so we ignore offset
      toCartesian(pts[i], true);
    }
    return pts;
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
  public boolean toFromPrimitive(boolean toPrimitive, char type, T3[] uc,
                                 M3 primitiveToCrystal) {

    // columns are definitions of new coordinates in terms of old

    int offset = uc.length - 3;
    M3 mf = null;
    if (type == 'r' || primitiveToCrystal == null) {
      switch (type) {
      default:
        return false;
      case 'r': // reciprocal
        getReciprocal(uc, uc, 1);
        return true;
      case 'P':
        toPrimitive = true;
        mf = M3.newA9(new float[] { 1, 0, 0, 0, 1, 0, 0, 0, 1 });
        break;
      case 'A':
        mf = M3.newA9(new float[] { 1, 0, 0, 0, 0.5f, 0.5f, 0, -0.5f, 0.5f });
        break;
      case 'B':
        mf = M3.newA9(new float[] { 0.5f, 0, 0.5f, 0, 1, 0, -0.5f, 0, 0.5f });
        break;
      case 'C':
        mf = M3.newA9(new float[] { 0.5f, 0.5f, 0, -0.5f, 0.5f, 0, 0, 0, 1 });
        break;
      case 'R':
        mf = M3.newA9(new float[] { 2 / 3f, -1 / 3f, -1 / 3f, 1 / 3f, 1 / 3f,
            -2 / 3f, 1 / 3f, 1 / 3f, 1 / 3f });
        break;
      case 'I':
        mf = M3.newA9(
            new float[] { -.5f, .5f, .5f, .5f, -.5f, .5f, .5f, .5f, -.5f });
        break;
      case 'F':
        mf = M3
            .newA9(new float[] { 0, 0.5f, 0.5f, 0.5f, 0, 0.5f, 0.5f, 0.5f, 0 });
        break;
      }
      if (!toPrimitive)
        mf.invert();
    } else {
      mf = M3.newM3(primitiveToCrystal);
      if (toPrimitive)
        mf.invert();
    }
    for (int i = uc.length; --i >= offset;) {
      T3 p = uc[i];
      toFractional(p, false);
      mf.rotate(p);
      toCartesian(p, false);
    }
    return true;
  }

  /**
   * return a conventional lattice from a primitive
   * 
   * @param latticeType  "A" "B" "C" "R" etc.
   * @return [origin va vb vc]
   */
  public T3[] getConventionalUnitCell(String latticeType, M3 primitiveToCrystal) {
    T3[] oabc = getUnitCellVectors();
    if (!latticeType.equals("P") || primitiveToCrystal != null)
      toFromPrimitive(false, latticeType.charAt(0), oabc, primitiveToCrystal);
    return oabc;
  }

}
