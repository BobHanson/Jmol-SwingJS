/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.shapespecial;

import java.util.Map;

import org.jmol.modelset.Atom;
import org.jmol.util.C;

import javajs.util.M3d;
import javajs.util.M4d;
import javajs.util.P3d;
import org.jmol.util.Tensor;

import javajs.util.V3d;

public class Ellipsoid {

  private static int ID = 0;

  public short colix = C.GOLD;
  public boolean visible;
  public boolean isValid;
  public P3d center = P3d.new3(0, 0, 0);
  public Tensor tensor;
  public String options;
  boolean isOn = true;

  String id;
  
  int myID = ++ID;

  int modelIndex;
  byte pid;
  double[] lengths;
  double scale = 1;
  int percent;
  private double[] scaleXYZ;
  public Map<String, Object> info;
  public String label;

  private Ellipsoid() {
  }

  public static Ellipsoid getEmptyEllipsoid(String id, int modelIndex) {
    Ellipsoid e = new Ellipsoid();
    e.id = id;
    e.modelIndex = modelIndex;
    return e;
  }

  public static Ellipsoid getEllipsoidForAtomTensor(Tensor t, Atom center) {
    Ellipsoid e = new Ellipsoid();
    e.tensor = t;
    e.modelIndex = t.modelIndex;
    e.colix = C.INHERIT_ALL;
    e.center = center;
    // not valid until scale is set
    return e;
  }

  public void setCenter(P3d center) {
    this.center = center;
    validate(false);
  }

  public double getLength(int i) {
    if (lengths == null)
      setLengths();
    return (lengths == null ? Double.NaN : lengths[i]);
  }

  public void scaleAxes(double[] value) {
    scaleXYZ = value;
    setLengths();
  }
  public void setLengths() {
    if (tensor == null)
      return;
    if (lengths == null)
      lengths = new double[3];
    for (int i = 0; i < lengths.length; i++)
      lengths[i] = (tensor.getFactoredValue(i) * scale * (scaleXYZ == null ? 1 : Math.abs(scaleXYZ[i])));
  }

  public void setScale(double scale, boolean isPercent) {
    if (scale <= 0) {
      this.isValid = false;
      return;
    }
    if (isPercent) {
      if (scale == Integer.MAX_VALUE)
        scale = (tensor.forThermalEllipsoid ? 50 : 100);
      percent = (int) scale;
      scale = (tensor.forThermalEllipsoid ? getThermalRadius(percent)
          : percent < 1 ? 0 : percent / 100.0d);
    }
    this.scale = scale;
    validate(true);
  }

  // from ORTEP manual ftp://ftp.ornl.gov/pub/ortep/man/pdf/chap6.pdf

  private final static double[] crtval = new double[] { 0.3389d, 0.4299d,
      0.4951d, 0.5479d, 0.5932d, 0.6334d, 0.6699d, 0.7035d, 0.7349d, 0.7644d,
      0.7924d, 0.8192d, 0.8447d, 0.8694d, 0.8932d, 0.9162d, 0.9386d, 0.9605d,
      0.9818d, 1.0026d, 1.0230d, 1.0430d, 1.0627d, 1.0821d, 1.1012d, 1.1200d,
      1.1386d, 1.1570d, 1.1751d, 1.1932d, 1.2110d, 1.2288d, 1.2464d, 1.2638d,
      1.2812d, 1.2985d, 1.3158d, 1.3330d, 1.3501d, 1.3672d, 1.3842d, 1.4013d,
      1.4183d, 1.4354d, 1.4524d, 1.4695d, 1.4866d, 1.5037d, 1.5209d, 1.5382d,
      1.5555d, 1.5729d, 1.5904d, 1.6080d, 1.6257d, 1.6436d, 1.6616d, 1.6797d,
      1.6980d, 1.7164d, 1.7351d, 1.7540d, 1.7730d, 1.7924d, 1.8119d, 1.8318d,
      1.8519d, 1.8724d, 1.8932d, 1.9144d, 1.9360d, 1.9580d, 1.9804d, 2.0034d,
      2.0269d, 2.0510d, 2.0757d, 2.1012d, 2.1274d, 2.1544d, 2.1824d, 2.2114d,
      2.2416d, 2.2730d, 2.3059d, 2.3404d, 2.3767d, 2.4153d, 2.4563d, 2.5003d,
      2.5478d, 2.5997d, 2.6571d, 2.7216d, 2.7955d, 2.8829d, 2.9912d, 3.1365d,
      3.3682d };

  final public static double getThermalRadius(int prob) {
    return crtval[prob < 1 ? 0 : prob > 99 ? 98 : prob - 1];
  }

  protected void setTensor(Tensor tensor) {
    isValid = false;
    this.tensor = tensor;
    validate(tensor != null);
  }

  private void validate(boolean andSetLengths) {
    if (tensor == null)
      return;
    if (andSetLengths)
      setLengths();
    isValid = true;
  }

  //  public double[] getEquation() {
  //    Matrix3f mat = new Matrix3f();
  //    Matrix3f mTemp = new Matrix3f();
  //    V3 v1 = new V3();
  //    double[] coefs = new double[10];
  //    for (int i = 0; i < 3; i++) {
  //      v1.setT(tensor.eigenVectors[i]);
  //      v1.scale(getLength(i));
  //      mat.setColumnV(i, v1);
  //    }
  //    mat.invertM(mat);
  //    getEquationForQuadricWithCenter(center.x, center.y, center.z,
  //        mat, v1, mTemp, coefs, null);
  //    double[] a = new double[10];
  //    for (int i = 0; i < 10; i++)
  //      a[i] = coefs[i];
  //    return a;
  //  }

  public static void getEquationForQuadricWithCenter(double x, double y, double z,
                                                     M3d mToElliptical,
                                                     V3d vTemp, M3d mTemp,
                                                     double[] coef,
                                                     M4d mDeriv) {
    /* Starting with a center point and a matrix that converts cartesian 
     * or screen coordinates to ellipsoidal coordinates, 
     * this method fills a double[10] with the terms for the 
     * equation for the ellipsoid:
     * 
     * c0 x^2 + c1 y^2 + c2 z^2 + c3 xy + c4 xz + c5 yz + c6 x + c7 y + c8 z - 1 = 0 
     * 
     * I made this up; I haven't seen it in print. -- Bob Hanson, 4/2008
     * 
     */

    vTemp.set(x, y, z);
    mToElliptical.rotate(vTemp);
    double f = 1 - vTemp.dot(vTemp); // J
    mTemp.transposeM(mToElliptical);
    mTemp.rotate(vTemp);
    mTemp.mul(mToElliptical);
    coef[0] = mTemp.m00 / f; // A = aXX
    coef[1] = mTemp.m11 / f; // B = aYY
    coef[2] = mTemp.m22 / f; // C = aZZ
    coef[3] = mTemp.m01 * 2 / f; // D = aXY
    coef[4] = mTemp.m02 * 2 / f; // E = aXZ
    coef[5] = mTemp.m12 * 2 / f; // F = aYZ
    coef[6] = -2 * vTemp.x / f; // G = aX
    coef[7] = -2 * vTemp.y / f; // H = aY
    coef[8] = -2 * vTemp.z / f; // I = aZ
    coef[9] = -1; // J = -1
    /*
     * f = Ax^2 + By^2 + Cz^2 + Dxy + Exz + Fyz + Gx + Hy + Iz + J
     * df/dx = 2Ax +  Dy +  Ez + G
     * df/dy =  Dx + 2By +  Fz + H
     * df/dz =  Ex +  Fy + 2Cz + I
     */

    if (mDeriv == null)
      return;
    mDeriv.setIdentity();
    mDeriv.m00 = (2 * coef[0]);
    mDeriv.m11 = (2 * coef[1]);
    mDeriv.m22 = (2 * coef[2]);

    mDeriv.m01 = mDeriv.m10 = coef[3];
    mDeriv.m02 = mDeriv.m20 = coef[4];
    mDeriv.m12 = mDeriv.m21 = coef[5];

    mDeriv.m03 = coef[6];
    mDeriv.m13 = coef[7];
    mDeriv.m23 = coef[8];
  }

}
