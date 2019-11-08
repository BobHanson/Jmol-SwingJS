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

import javajs.util.M3;
import javajs.util.M4;
import javajs.util.P3;
import org.jmol.util.Tensor;

import javajs.util.V3;

public class Ellipsoid {

  public short colix = C.GOLD;
  public boolean visible;
  public boolean isValid;
  public P3 center = P3.new3(0, 0, 0);
  public Tensor tensor;
  public String options;
  boolean isOn = true;

  String id;
  int modelIndex;
  byte pid;
  float[] lengths;
  float scale = 1;
  int percent;
  private float[] scaleXYZ;
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

  public void setCenter(P3 center) {
    this.center = center;
    validate(false);
  }

  public float getLength(int i) {
    if (lengths == null)
      setLengths();
    return (lengths == null ? Float.NaN : lengths[i]);
  }

  public void scaleAxes(float[] value) {
    scaleXYZ = value;
    setLengths();
  }
  public void setLengths() {
    if (tensor == null)
      return;
    if (lengths == null)
      lengths = new float[3];
    for (int i = 0; i < lengths.length; i++)
      lengths[i] = tensor.getFactoredValue(i) * scale * (scaleXYZ == null ? 1 : Math.abs(scaleXYZ[i]));
  }

  public void setScale(float scale, boolean isPercent) {
    if (scale <= 0) {
      this.isValid = false;
      return;
    }
    if (isPercent) {
      if (scale == Integer.MAX_VALUE)
        scale = (tensor.forThermalEllipsoid ? 50 : 100);
      percent = (int) scale;
      scale = (tensor.forThermalEllipsoid ? getThermalRadius(percent)
          : percent < 1 ? 0 : percent / 100.0f);
    }
    this.scale = scale;
    validate(true);
  }

  // from ORTEP manual ftp://ftp.ornl.gov/pub/ortep/man/pdf/chap6.pdf

  private final static float[] crtval = new float[] { 0.3389f, 0.4299f,
      0.4951f, 0.5479f, 0.5932f, 0.6334f, 0.6699f, 0.7035f, 0.7349f, 0.7644f,
      0.7924f, 0.8192f, 0.8447f, 0.8694f, 0.8932f, 0.9162f, 0.9386f, 0.9605f,
      0.9818f, 1.0026f, 1.0230f, 1.0430f, 1.0627f, 1.0821f, 1.1012f, 1.1200f,
      1.1386f, 1.1570f, 1.1751f, 1.1932f, 1.2110f, 1.2288f, 1.2464f, 1.2638f,
      1.2812f, 1.2985f, 1.3158f, 1.3330f, 1.3501f, 1.3672f, 1.3842f, 1.4013f,
      1.4183f, 1.4354f, 1.4524f, 1.4695f, 1.4866f, 1.5037f, 1.5209f, 1.5382f,
      1.5555f, 1.5729f, 1.5904f, 1.6080f, 1.6257f, 1.6436f, 1.6616f, 1.6797f,
      1.6980f, 1.7164f, 1.7351f, 1.7540f, 1.7730f, 1.7924f, 1.8119f, 1.8318f,
      1.8519f, 1.8724f, 1.8932f, 1.9144f, 1.9360f, 1.9580f, 1.9804f, 2.0034f,
      2.0269f, 2.0510f, 2.0757f, 2.1012f, 2.1274f, 2.1544f, 2.1824f, 2.2114f,
      2.2416f, 2.2730f, 2.3059f, 2.3404f, 2.3767f, 2.4153f, 2.4563f, 2.5003f,
      2.5478f, 2.5997f, 2.6571f, 2.7216f, 2.7955f, 2.8829f, 2.9912f, 3.1365f,
      3.3682f };

  final public static float getThermalRadius(int prob) {
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

  //  public float[] getEquation() {
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
  //    float[] a = new float[10];
  //    for (int i = 0; i < 10; i++)
  //      a[i] = (float) coefs[i];
  //    return a;
  //  }

  public static void getEquationForQuadricWithCenter(float x, float y, float z,
                                                     M3 mToElliptical,
                                                     V3 vTemp, M3 mTemp,
                                                     double[] coef,
                                                     M4 mDeriv) {
    /* Starting with a center point and a matrix that converts cartesian 
     * or screen coordinates to ellipsoidal coordinates, 
     * this method fills a float[10] with the terms for the 
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
    mDeriv.m00 = (float) (2 * coef[0]);
    mDeriv.m11 = (float) (2 * coef[1]);
    mDeriv.m22 = (float) (2 * coef[2]);

    mDeriv.m01 = mDeriv.m10 = (float) coef[3];
    mDeriv.m02 = mDeriv.m20 = (float) coef[4];
    mDeriv.m12 = mDeriv.m21 = (float) coef[5];

    mDeriv.m03 = (float) coef[6];
    mDeriv.m13 = (float) coef[7];
    mDeriv.m23 = (float) coef[8];
  }

}
