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

package org.jmol.util;

import javajs.util.AU;
import javajs.util.M4;
import javajs.util.P3;
import javajs.util.P3i;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.T3;
import javajs.util.V3;



/**
 * general-purpose simple unit cell for calculations 
 * and as a super-class of unitcell, which is only part of Symmetry
 * 
 * allows one-dimensional (polymer) and two-dimensional (slab) 
 * periodicity
 * 
 */

public class SimpleUnitCell {

  protected float[] unitCellParams; //6 parameters + optional 16 matrix items
  public M4 matrixCartesianToFractional;
  public M4 matrixFractionalToCartesian;
  public double volume;

  protected final static float toRadians = (float) Math.PI * 2 / 360;

  private int na, nb, nc;
  public boolean isSupercell() {
    return (na > 1 || nb > 1 || nc > 1);
  }

  protected float a, b, c, alpha, beta, gamma;
  protected double cosAlpha, sinAlpha;
  protected double cosBeta, sinBeta;
  protected double cosGamma, sinGamma;
  protected double cA_, cB_;
  protected double a_;
  protected double b_, c_;
  protected int dimension;
  private P3 fractionalOrigin;

  public static boolean isValid(float[] parameters) {
    return (parameters != null && (parameters[0] > 0 || parameters.length > 14
        && !Float.isNaN(parameters[14])));
  }

  protected SimpleUnitCell() {
    fractionalOrigin = new P3();
  }

  /**
   * 
   * @param params len = 6 [a b c alpha beta gamma] 
   *            or len = 15 [-1 0 0 0 0 0 va vb vc]
   *            or len = 22 [a b c alpha beta gamma m00 m01 .. m33]
   *            and/or len = 25 [......................  na nb nc]
   *             
   * @return a simple unit cell
   */
  public static SimpleUnitCell newA(float[] params) {
    SimpleUnitCell c = new SimpleUnitCell();
    c.init(params);
    return c;
  }
  
  
  protected void init(float[] params) {
    if (params == null)
      params = new float[] {1, 1, 1, 90, 90, 90};
    if (!isValid(params))
      return;
    unitCellParams = AU.arrayCopyF(params, params.length);

    boolean rotateHex = false; // special gamma = -1 indicates hex rotation for AFLOW
    
    a = params[0];
    b = params[1];
    c = params[2];
    alpha = params[3];
    beta = params[4];
    gamma = params[5];
    if (gamma == -1) {
      rotateHex = true;
      gamma = 120;
    }
    
    // (int) Float.NaN == 0 (but not in JavaScript!)
    // supercell
    float fa = na = Math.max(1, params.length >= 25 && !Float.isNaN(params[22]) ? (int) params[22] : 1);
    float fb = nb = Math.max(1, params.length >= 25 && !Float.isNaN(params[23]) ? (int) params[23] : 1);
    float fc = nc = Math.max(1, params.length >= 25 && !Float.isNaN(params[24]) ? (int) params[24] : 1);
    if (params.length > 25 && !Float.isNaN(params[25])) {
      float fScale = params[25];
      fa *= fScale;
      fb *= fScale;
      fc *= fScale;
    } else {
      fa = fb = fc = 1;
    }

    if (a <= 0) {
      // must calculate a, b, c alpha beta gamma from Cartesian vectors;
      V3 va = V3.new3(params[6], params[7], params[8]);
      V3 vb = V3.new3(params[9], params[10], params[11]);
      V3 vc = V3.new3(params[12], params[13], params[14]);
      setABC(va, vb, vc);
      if (c < 0) {
        float[] n = AU.arrayCopyF(params, -1);
        if (b < 0) {
          vb.set(0, 0, 1);
          vb.cross(vb, va);
          if (vb.length() < 0.001f)
            vb.set(0, 1, 0);
          vb.normalize();
          n[9] = vb.x;
          n[10] = vb.y;
          n[11] = vb.z;
        }
        if (c < 0) {
          vc.cross(va, vb);
          vc.normalize();
          n[12] = vc.x;
          n[13] = vc.y;
          n[14] = vc.z;
        }
        params = n;
      }
    }
    //System.out.println("unitcell " + a + " " + b + " " + c);
    
    a *= fa; 
    if (b <= 0) {
      b = c = 1;
      dimension = 1;
    } else if (c <= 0) {
      c = 1;
      b *= fb;
      dimension = 2;
    } else {
      b *= fb;
      c *= fc;
      dimension = 3;
    }

    setCellParams();
    
    if (params.length > 21 && !Float.isNaN(params[21])) {
      // parameters with a 4x4 matrix
      // [a b c alpha beta gamma m00 m01 m02 m03 m10 m11.... m20...]
      // this is for PDB and CIF reader
      float[] scaleMatrix = new float[16];
      for (int i = 0; i < 16; i++) {
        float f;
        switch (i % 4) {
        case 0:
          f = fa;
          break;
        case 1:
          f = fb;
          break;
        case 2:
          f = fc;
          break;
        default:
          f = 1;
          break;
        }
        scaleMatrix[i] = params[6 + i] * f;
      }      
      matrixCartesianToFractional = M4.newA16(scaleMatrix);
      matrixCartesianToFractional.getTranslation(fractionalOrigin);
      matrixFractionalToCartesian = M4.newM4(matrixCartesianToFractional).invert();
      if (params[0] == 1)
        setParamsFromMatrix();
    } else if (params.length > 14 && !Float.isNaN(params[14])) {
      // parameters with a 3 vectors
      // [a b c alpha beta gamma ax ay az bx by bz cx cy cz...]
      M4 m = matrixFractionalToCartesian = new M4();
      m.setColumn4(0, params[6] * fa, params[7] * fa, params[8] * fa, 0);
      m.setColumn4(1, params[9] * fb, params[10] * fb, params[11] * fb, 0);
      m.setColumn4(2, params[12] * fc, params[13] * fc, params[14] * fc, 0);
      m.setColumn4(3, 0, 0, 0, 1);
      matrixCartesianToFractional = M4.newM4(matrixFractionalToCartesian).invert();
    } else {
      M4 m = matrixFractionalToCartesian = new M4();
      
      if (rotateHex) {
        // 1, 2. align a and b symmetrically about the x axis (AFLOW)
        m.setColumn4(0, (float) (-b * cosGamma), (float) (-b * sinGamma), 0, 0);
        // 2. place the b is in xy plane making a angle gamma with a
        m.setColumn4(1, (float) (-b * cosGamma), (float) (b * sinGamma), 0, 0);
      } else {
        // 1. align the a axis with x axis
        m.setColumn4(0, a, 0, 0, 0);
        // 2. place the b is in xy plane making a angle gamma with a
        m.setColumn4(1, (float) (b * cosGamma), (float) (b * sinGamma), 0, 0);
      }
      // 3. now the c axis,
      // http://server.ccl.net/cca/documents/molecular-modeling/node4.html
      m.setColumn4(2, (float) (c * cosBeta), (float) (c
          * (cosAlpha - cosBeta * cosGamma) / sinGamma), (float) (volume / (a
          * b * sinGamma)), 0);
      m.setColumn4(3, 0, 0, 0, 1);
      matrixCartesianToFractional = M4.newM4(matrixFractionalToCartesian).invert();
    }
    matrixCtoFNoOffset = matrixCartesianToFractional;
    matrixFtoCNoOffset = matrixFractionalToCartesian;
  }

  private void setParamsFromMatrix() {
    V3 va = V3.new3(1,  0,  0);
    V3 vb = V3.new3(0,  1,  0);
    V3 vc = V3.new3(0,  0,  1);
    matrixFractionalToCartesian.rotate(va);
    matrixFractionalToCartesian.rotate(vb);
    matrixFractionalToCartesian.rotate(vc);
    setABC(va, vb, vc);
    setCellParams();
  }

  private void setABC(V3 va, V3 vb, V3 vc) {
    a = va.length();
    b = vb.length();
    c = vc.length();
    if (a == 0)
      return;
    if (b == 0)
      b = c = -1; //polymer
    else if (c == 0)
      c = -1; //slab
    alpha = (b < 0 || c < 0 ? 90 : vb.angle(vc) / toRadians);
    beta = (c < 0 ? 90 : va.angle(vc) / toRadians);
    gamma = (b < 0 ? 90 : va.angle(vb) / toRadians);
  }

  private void setCellParams() {
    //System.out.println("unitcell " + a + " " + b + " " + c);
    cosAlpha = Math.cos(toRadians * alpha);
    sinAlpha = Math.sin(toRadians * alpha);
    cosBeta = Math.cos(toRadians * beta);
    sinBeta = Math.sin(toRadians * beta);
    cosGamma = Math.cos(toRadians * gamma);
    sinGamma = Math.sin(toRadians * gamma);
    double unitVolume = Math.sqrt(sinAlpha * sinAlpha + sinBeta * sinBeta
        + sinGamma * sinGamma + 2.0 * cosAlpha * cosBeta * cosGamma - 2);
    volume = a * b * c * unitVolume;
    // these next few are for the B' calculation
    cA_ = (cosAlpha - cosBeta * cosGamma) / sinGamma;
    cB_ = unitVolume / sinGamma;
    a_ = b * c * sinAlpha / volume;
    b_ = a * c * sinBeta / volume;
    c_ = a * b * sinGamma / volume;
  }

  public T3 getFractionalOrigin() {
    return fractionalOrigin ;
  }

  protected M4 matrixCtoFNoOffset;
  protected M4 matrixFtoCNoOffset;
  
  public final static int INFO_DIMENSIONS = 6;
  public final static int INFO_GAMMA = 5;
  public final static int INFO_BETA = 4;
  public final static int INFO_ALPHA = 3;
  public final static int INFO_C = 2;
  public final static int INFO_B = 1;
  public final static int INFO_A = 0;

  /**
   * convenience return only after changing fpt
   * 
   * @param fpt
   * @return adjusted fpt
   */
  public P3 toSupercell(P3 fpt) {
    fpt.x /= na;
    fpt.y /= nb;
    fpt.z /= nc;
    return fpt;
  }

  public final void toCartesian(T3 pt, boolean ignoreOffset) {
    if (matrixFractionalToCartesian != null)
      (ignoreOffset ? matrixFtoCNoOffset : matrixFractionalToCartesian)
          .rotTrans(pt);
  }

  public void toFractionalM(M4 m) {
    if (matrixCartesianToFractional == null)
      return;
    m.mul(matrixFractionalToCartesian);
    m.mul2(matrixCartesianToFractional, m);
  }
  
  public final void toFractional(T3 pt, boolean ignoreOffset) {
    if (matrixCartesianToFractional == null)
      return;
    (ignoreOffset ? matrixCtoFNoOffset : matrixCartesianToFractional)
        .rotTrans(pt);
  }

  public boolean isPolymer() {
    return (dimension == 1);
  }

  public boolean isSlab() {
    return (dimension == 2);
  }

  public final float[] getUnitCellParams() {
    return unitCellParams;
  }

  public final float[] getUnitCellAsArray(boolean vectorsOnly) {
    M4 m = matrixFractionalToCartesian;
    return (vectorsOnly ? new float[] { 
        m.m00, m.m10, m.m20, // Va
        m.m01, m.m11, m.m21, // Vb
        m.m02, m.m12, m.m22, // Vc
      } 
      : new float[] { 
        a, b, c, alpha, beta, gamma, 
        m.m00, m.m10, m.m20, // Va
        m.m01, m.m11, m.m21, // Vb
        m.m02, m.m12, m.m22, // Vc
        dimension, (float) volume,
      } 
    );
  }

  public final float getInfo(int infoType) {
    switch (infoType) {
    case INFO_A:
      return a;
    case INFO_B:
      return b;
    case INFO_C:
      return c;
    case INFO_ALPHA:
      return alpha;
    case INFO_BETA:
      return beta;
    case INFO_GAMMA:
      return gamma;
    case INFO_DIMENSIONS:
      return dimension;
    }
    return Float.NaN;
  }

  /**
   * Expanded cell notation:
   * 
   * 111 - 1000 --> center 5,5,5; range 0 to 9 or -5 to +4
   * 
   * 1000000 - 1999999 --> center 50,50,50; range 0 to 99 or -50 to +49
   * 1000000000 - 1999999999 --> center 500, 500, 500; range 0 to 999 or -500 to
   * +499
   * 
   * @param nnn
   * @param cell
   * @param offset
   *        0 or 1 typically; < 0 means "apply no offset"
   * @param kcode
   *        Generally the multiplier is just {ijk ijk scale}, but when we have
   *        1iiijjjkkk 1iiijjjkkk scale, floats lose kkk due to Java float
   *        precision issues so we use P4 {1iiijjjkkk 1iiijjjkkk scale,
   *        1kkkkkk}. Here, our offset -- initially 0 or 1 from the uccage
   *        renderer, but later -500 or -499 -- tells us which code we are
   *        looking at, the first one or the second one.
   * 
   */
  public static void ijkToPoint3f(int nnn, P3 cell, int offset, int kcode) {
    int f = (nnn > 1000000000 ? 1000 : nnn > 1000000 ? 100 : 10);
    int f2 = f * f;
    offset -= (offset >= 0 ? 5 * f / 10 : offset);
    cell.x = ((nnn / f2) % f) + offset;
    cell.y = (nnn % f2) / f + offset;
    cell.z = (kcode == 0 ? nnn % f 
        : (offset == -500 ? kcode / f : kcode) % f) + offset;
  }

  /**
   * Generally the multiplier is just {ijk ijk scale}, but when we have
   * 1iiijjjkkk 1iiijjjkkk scale, floats lose kkk due to Java float precision
   * issues so we use P4 {1iiijjjkkk 1iiijjjkkk scale, 1kkkkkk}
   * 
   * @param pt
   * @return String representation for state
   */
  public static String escapeMultiplier(T3 pt) {
    if (pt instanceof P4) {
      P4 pt4 = (P4) pt;
      int x = (int) Math.floor(pt4.x / 1000)*1000 
                  + (int) Math.floor(pt4.w / 1000) - 1000;
      int y = (int) Math.floor(pt4.y / 1000)*1000 
          + (int) Math.floor(pt4.w) % 1000;
      return "{" + x + " " + y + " " + pt.z + "}"; 
    }
    return Escape.eP(pt);
  }
  

  public final static float SLOP = 0.02f;
  private final static float SLOP1 = 1 - SLOP;

  /**
   * calculate weighting of 1 (interior), 0.5 (face), 0.25 (edge), or 0.125 (vertex)
   * @param pt
//   * @param tolerance fractional allowance to consider this on an edge
   * @return weighting
   */
  public static float getCellWeight(P3 pt) {
    float f = 1;
    if (pt.x <= SLOP || pt.x >= SLOP1)
      f /= 2;
    if (pt.y <= SLOP || pt.y >= SLOP1)
      f /= 2;
    if (pt.z <= SLOP || pt.z >= SLOP1)
      f /= 2;
    return f;
  }
  
  public static T3[] getReciprocal(T3[] abc, T3[] ret, float scale) {
    P3[] rabc = new P3[4];
    int off = (abc.length == 4 ? 1 : 0);
    rabc[0] = (off == 1 ? P3.newP(abc[0]) : new P3()); // origin
    for (int i = 0; i < 3; i++) {
      rabc[i + 1] = new P3();
      rabc[i + 1].cross(abc[((i + off) % 3) + off], abc[((i + off + 1) % 3)
          + off]);
      rabc[i + 1].scale(scale / abc[i + off].dot(rabc[i + 1]));
    }
    if (ret == null)
      return rabc;
    for (int i = 0; i < 4; i++)
      ret[i] = rabc[i];
    return ret;
  }

  /**
   * set cell vectors by string
   * 
   * 
   * @param abcabg "a=...,b=...,c=...,alpha=...,beta=..., gamma=..." or null  
   * @param params to use if not null 
   * @param ucnew  to create and return; null if only to set params
   * @return T3[4] origin, a, b c
   */
  public static T3[] setOabc(String abcabg, float[] params, T3[] ucnew) {
    if (abcabg != null) {
      if (params == null)
        params = new float[6];
      String[] tokens = PT.split(abcabg.replace(',', '='), "=");
      if (tokens.length >= 12)
        for (int i = 0; i < 6; i++)
          params[i] = PT.parseFloat(tokens[i * 2 + 1]);
    }
    if (ucnew == null)
      return null;
    float[] f = newA(params).getUnitCellAsArray(true);
      ucnew[1].set(f[0], f[1], f[2]);
      ucnew[2].set(f[3], f[4], f[5]);
      ucnew[3].set(f[6], f[7], f[8]);
    return ucnew;
  }

  /**
   * 
   * @param dimension
   * @param minXYZ
   * @param maxXYZ
   * @param kcode
   *        Generally the multiplier is just {ijk ijk scale}, but when we have
   *        1iiijjjkkk 1iiijjjkkk scale, floats lose kkk due to Java float
   *        precision issues so we use P4 {1iiijjjkkk 1iiijjjkkk scale,
   *        1kkkkkk}. Here, our offset -- initially 0 or 1 from the uccage
   *        renderer, but later -500 or -499 -- tells us which code we are
   *        looking at, the first one or the second one.
   */
  public static void setMinMaxLatticeParameters(int dimension, P3i minXYZ, P3i maxXYZ, int kcode) {
    
    if (maxXYZ.x <= maxXYZ.y && maxXYZ.y >= 555) {
      //alternative format for indicating a range of cells:
      //{111 666}
      //555 --> {0 0 0}
      P3 pt = new P3();
      ijkToPoint3f(maxXYZ.x, pt, 0, kcode);
      minXYZ.x = (int) pt.x;
      minXYZ.y = (int) pt.y;
      minXYZ.z = (int) pt.z;
      ijkToPoint3f(maxXYZ.y, pt, 1, kcode);
      //555 --> {1 1 1}
      maxXYZ.x = (int) pt.x;
      maxXYZ.y = (int) pt.y;
      maxXYZ.z = (int) pt.z;
    }
    switch (dimension) {
    case 1: // polymer
      minXYZ.y = 0;
      maxXYZ.y = 1;
      //$FALL-THROUGH$
    case 2: // slab
      minXYZ.z = 0;
      maxXYZ.z = 1;
    }
  }


  public String toString() {
    return "[" + a + " " + b + " " + c + " " + alpha + " " + beta + " " + gamma + "]";
  }
}
