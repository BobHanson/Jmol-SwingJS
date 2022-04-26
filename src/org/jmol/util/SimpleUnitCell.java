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

import org.jmol.api.SymmetryInterface;

import javajs.util.AU;
import javajs.util.M4d;
import javajs.util.P3d;
import javajs.util.P3d;
import javajs.util.P3i;
import javajs.util.P4d;
import javajs.util.PT;
import javajs.util.T3d;
import javajs.util.T3d;
import javajs.util.V3d;



/**
 * general-purpose simple unit cell for calculations 
 * and as a super-class of unitcell, which is only part of Symmetry
 * 
 * allows one-dimensional (polymer) and two-dimensional (slab) 
 * periodicity
 * 
 */

public class SimpleUnitCell {

  public static final int PARAM_STD = 6;
  public static final int PARAM_VABC = 6;
  public static final int PARAM_M4 = 6;
  public static final int PARAM_SUPERCELL = 22;
  public static final int PARAM_SCALE = 25;
  public static final int PARAM_COUNT = 26;

//  protected float[] unitCellParams;
//  public M4d matrixCartesianToFractional;
//  public M4d matrixFractionalToCartesian;
//  protected M4d matrixCtoFNoOffset;
//  protected M4d matrixFtoCNoOffset;

  protected double[] unitCellParamsD;
  public M4d matrixCartesianToFractionalD;
  public M4d matrixFractionalToCartesianD;
  protected M4d matrixCtoFNoOffsetD;
  protected M4d matrixFtoCNoOffsetD;

  

  protected int dimension = 3;
  public double volume;

  private P3d fractionalOriginD;

  protected final static double toRadians = Math.PI * 2 / 360;
  private int na, nb, nc;
  
  public boolean isSupercell() {
    return (na > 1 || nb > 1 || nc > 1);
  }

  protected double a, b, c, alpha, beta, gamma;
  protected double cosAlpha, sinAlpha;
  protected double cosBeta, sinBeta;
  protected double cosGamma, sinGamma;
  protected double cA_, cB_;
  protected double a_;
  protected double b_, c_;

//  public static boolean isValid(float[] parameters) {
//    return (parameters != null && (parameters[0] > 0 || parameters.length > 14
//        && !Double.isNaN(parameters[14])));
//  }
//
  public static boolean isValidD(double[] parameters) {
    return (parameters != null && (parameters[0] > 0 || parameters.length > 14
        && !Double.isNaN(parameters[14])));
  }

  protected SimpleUnitCell() {
    fractionalOriginD = new P3d();
  }

  /**
   * 
   * @param params
   * 
   *        len = 6 [a b c alpha beta gamma]
   * 
   *        len = 6 [a b -1 alpha beta gamma] // slab
   * 
   *        len = 6 [a -1 -1 alpha beta gamma] // polymer
   * 
   *        or len = 15 [-1 -1 -1 -1 -1 -1 va[3] vb[3] vc[3]] // vectors only
   * 
   *        or len = 15 [a -1 -1 -1 -1 -1 va[3] vb[3] vc[3]] // polymer, vectors only
   * 
   *        or len = 15 [a b -1 -1 -1 -1 va[3] vb[3] vc[3]] // slab, vectors only
   * 
   *        or len = 22 [a b c alpha beta gamma m00 m01 .. m33] // matrix included
   * 
   *        and/or len = 25 [...................... na nb nc] // supercell
   * 
   *        and/or len = 26 [...................... na nb nc scale] // scaled supercell
   * 
   * @return a simple unit cell
   */
  public static SimpleUnitCell newA(float[] params) {
    SimpleUnitCell c = new SimpleUnitCell();
    c.init(params);
    return c;
  }
  
  public static SimpleUnitCell newAD(double[] params) {
    SimpleUnitCell c = new SimpleUnitCell();
    c.initD(params);
    return c;
  }

  protected void init(float[] params) {
    double[] paramsD = null;
    if (params != null) {
      paramsD = new double[params.length];
      for (int i = params.length; --i >= 0;)
        paramsD[i] = (Double.isNaN(params[i]) ? Double.NaN : params[i]);
    }
    initD(paramsD);
  }

  protected void initD(double[] params) {
    if (params == null)
      params = new double[] {1, 1, 1, 90, 90, 90};
    if (!isValidD(params))
      return;
    params = unitCellParamsD = AU.arrayCopyD(params, params.length);

    boolean rotateHex = false; // special gamma = -1 indicates hex rotation for AFLOW
    
    a = params[0];
    b = params[1];
    c = params[2];
    if (b < 0) {
      dimension = 1;
    } else if (c < 0) {
      dimension = 2;
    }
    
    alpha = params[3];
    beta = params[4];
    gamma = params[5];
    if (gamma == -1 && c > 0) {
      rotateHex = true;
      gamma = 120;
    }
    
    // (int) double.NaN == 0 (but not in JavaScript!)
    // supercell
    double fa = na = Math.max(1, params.length > PARAM_SUPERCELL+2 && !Double.isNaN(params[PARAM_SUPERCELL]) ? (int) params[PARAM_SUPERCELL] : 1);
    double fb = nb = Math.max(1, params.length > PARAM_SUPERCELL+2 && !Double.isNaN(params[PARAM_SUPERCELL+1]) ? (int) params[PARAM_SUPERCELL+1] : 1);
    double fc = nc = Math.max(1, params.length > PARAM_SUPERCELL+2 && !Double.isNaN(params[PARAM_SUPERCELL+2]) ? (int) params[PARAM_SUPERCELL+2] : 1);
    if (params.length > PARAM_SCALE && !Double.isNaN(params[PARAM_SCALE])) {
      double fScale = params[PARAM_SCALE];
      fa *= fScale;
      fb *= fScale;
      fc *= fScale;
    } else {
      fa = fb = fc = 1;
    }

    if (c <= 0) {
      // must calculate a, b, c alpha beta gamma from Cartesian vectors;
      V3d va = V3d.new3(params[PARAM_VABC], params[PARAM_VABC+1], params[PARAM_VABC+2]);
      V3d vb = V3d.new3(params[PARAM_VABC+3], params[PARAM_VABC+4], params[PARAM_VABC+5]);
      V3d vc = V3d.new3(params[PARAM_VABC+6], params[PARAM_VABC+7], params[PARAM_VABC+8]);
      setABC(va, vb, vc);
      if (c < 0) {
        double[] n = AU.arrayCopyD(params, -1);
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
    
    // checking here for still a dimension issue with b or c
    // was < 0 above; here <= 0
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
    }
    setCellParams();
    
    if (params.length > 21 && !Double.isNaN(params[21])) {
      // parameters with a 4x4 matrix
      // [a b c alpha beta gamma m00 m01 m02 m03 m10 m11.... m20...]
      // this is for PDB and CIF reader
      double[] scaleMatrix = new double[16];
      for (int i = 0; i < 16; i++) {
        double f;
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
      matrixCartesianToFractionalD = M4d.newA16(scaleMatrix);
      matrixCartesianToFractionalD.getTranslation(fractionalOriginD);
      matrixFractionalToCartesianD = M4d.newM4(matrixCartesianToFractionalD).invert();
      if (params[0] == 1)
        setParamsFromMatrix();
    } else if (params.length > 14 && !Double.isNaN(params[14])) {
      // parameters with a 3 vectors
      // [a b c alpha beta gamma ax ay az bx by bz cx cy cz...]
      M4d m = matrixFractionalToCartesianD = new M4d();
      m.setColumn4(0, params[6] * fa, params[7] * fa, params[8] * fa, 0);
      m.setColumn4(1, params[9] * fb, params[10] * fb, params[11] * fb, 0);
      m.setColumn4(2, params[12] * fc, params[13] * fc, params[14] * fc, 0);
      m.setColumn4(3, 0, 0, 0, 1);
      matrixCartesianToFractionalD = M4d.newM4(matrixFractionalToCartesianD).invert();
    } else {
      M4d m = matrixFractionalToCartesianD = new M4d();
      
      if (rotateHex) {
        // 1, 2. align a and b symmetrically about the x axis (AFLOW)
        m.setColumn4(0, (double) (-b * cosGamma), (double) (-b * sinGamma), 0, 0);
        // 2. place the b is in xy plane making a angle gamma with a
        m.setColumn4(1, (double) (-b * cosGamma), (double) (b * sinGamma), 0, 0);
      } else {
        // 1. align the a axis with x axis
        m.setColumn4(0, a, 0, 0, 0);
        // 2. place the b is in xy plane making a angle gamma with a
        m.setColumn4(1, (double) (b * cosGamma), (double) (b * sinGamma), 0, 0);
      }
      // 3. now the c axis,
      // http://server.ccl.net/cca/documents/molecular-modeling/node4.html
      m.setColumn4(2, (double) (c * cosBeta), (double) (c
          * (cosAlpha - cosBeta * cosGamma) / sinGamma), (double) (volume / (a
          * b * sinGamma)), 0);
      m.setColumn4(3, 0, 0, 0, 1);
      matrixCartesianToFractionalD = M4d.newM4(matrixFractionalToCartesianD).invert();
    }
    matrixCtoFNoOffsetD = matrixCartesianToFractionalD;
    matrixFtoCNoOffsetD = matrixFractionalToCartesianD;
  }

//  public static void addVectors(float[] params) {
//    SimpleUnitCell c = SimpleUnitCell.newA(params);
//    M4d m = c.matrixFractionalToCartesian;
//    for (int i = 0; i < 9; i++)
//    params[PARAM_VABC + i] = m.getElement(i%3, i/3);
//  }

  public static void addVectorsD(double[] params) {
    SimpleUnitCell c = SimpleUnitCell.newAD(params);
    M4d m = c.matrixFractionalToCartesianD;
    for (int i = 0; i < 9; i++)
    params[PARAM_VABC + i] = m.getElement(i%3, i/3);
  }



  private void setParamsFromMatrix() {
    V3d va = V3d.new3(1,  0,  0);
    V3d vb = V3d.new3(0,  1,  0);
    V3d vc = V3d.new3(0,  0,  1);
    matrixFractionalToCartesianD.rotate(va);
    matrixFractionalToCartesianD.rotate(vb);
    matrixFractionalToCartesianD.rotate(vc);
    setABC(va, vb, vc);
    setCellParams();
  }

  private void setABC(V3d va, V3d vb, V3d vc) {
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
    double[] p = unitCellParamsD;
    p[0] = a;
    p[1] = b;
    p[2] = c;
    p[3] = alpha;
    p[4] = beta;
    p[5] = gamma;
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

  private P3d fo = new P3d();
  
  public P3d getFractionalOrigin() {
    return (P3d) fractionalOriginD.putP(fo);
  }

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
  public P3d toSupercell(P3d fpt) {
    fpt.x /= na;
    fpt.y /= nb;
    fpt.z /= nc;
    return fpt;
  }

  public final void toCartesian(T3d pt, boolean ignoreOffset) {
    if (matrixFractionalToCartesianD != null)
      (ignoreOffset ? matrixFtoCNoOffsetD : matrixFractionalToCartesianD)
          .rotTrans(pt);
  }

  public final void toCartesianF(T3d pt, boolean ignoreOffset) {
    if (matrixFractionalToCartesianD != null)
      (ignoreOffset ? matrixFtoCNoOffsetD : matrixFractionalToCartesianD)
          .rotTrans(pt);
  }

  public void toFractionalM(M4d m) {
    if (matrixCartesianToFractionalD == null)
      return;
    m.mul(matrixFractionalToCartesianD);
    m.mul2(matrixCartesianToFractionalD, m);
  }
  
  public final void toFractionalF(T3d pt, boolean ignoreOffset) {
    if (matrixCartesianToFractionalD == null)
      return;
    (ignoreOffset ? matrixCtoFNoOffsetD : matrixCartesianToFractionalD)
        .rotTrans(pt);
  }

  public final void toFractionalD(T3d pt, boolean ignoreOffset) {
    if (matrixCartesianToFractionalD == null)
      return;
    (ignoreOffset ? matrixCtoFNoOffsetD : matrixCartesianToFractionalD)
        .rotTrans(pt);
  }

  public boolean isPolymer() {
    return (dimension == 1);
  }

  public boolean isSlab() {
    return (dimension == 2);
  }

  public final double[] getUnitCellParamsD() {
    return unitCellParamsD;
  }

  public final float[] getUnitCellParamsF() {
    return AU.toFloatA(unitCellParamsD);
  }

  public final float[] getUnitCellAsArray(boolean vectorsOnly) {
    return AU.toFloatA(getUnitCellAsArrayD(vectorsOnly));
  }

  public final double[] getUnitCellAsArrayD(boolean vectorsOnly) {
    M4d m = matrixFractionalToCartesianD;
    return (vectorsOnly ? new double[] { 
        m.m00, m.m10, m.m20, // Va
        m.m01, m.m11, m.m21, // Vb
        m.m02, m.m12, m.m22, // Vc
      } 
      : new double[] { 
        a, b, c, alpha, beta, gamma, 
        m.m00, m.m10, m.m20, // Va
        m.m01, m.m11, m.m21, // Vb
        m.m02, m.m12, m.m22, // Vc
        dimension, volume,
      } 
    );
  }

  public final double getInfo(int infoType) {
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
    return Double.NaN;
  }

  public final static float SLOP = 0.02f;
  private final static float SLOP1 = 1 - SLOP;

  /**
   * calculate weighting of 1 (interior), 0.5 (face), 0.25 (edge), or 0.125 (vertex)
   * @param pt
//   * @param tolerance fractional allowance to consider this on an edge
   * @return weighting
   */
  public static float getCellWeight(P3d pt) {
    float f = 1;
    if (pt.x <= SLOP || pt.x >= SLOP1)
      f /= 2;
    if (pt.y <= SLOP || pt.y >= SLOP1)
      f /= 2;
    if (pt.z <= SLOP || pt.z >= SLOP1)
      f /= 2;
    return f;
  }
  
  /**
   * Generate the reciprocal unit cell, scaled as desired
   * 
   * @param abc [a,b,c] or [o,a,b,c]
   * @param ret
   * @param scale 0 for 2pi, general reciprocal lattice
   * @return oabc
   */
  public static T3d[] getReciprocal(T3d[] abc, T3d[] ret, double scale) {
    if (scale == 0)
      scale = 2 * Math.PI;
    P3d[] rabc = new P3d[4];
    int off = (abc.length == 4 ? 1 : 0);
    rabc[0] = (off == 1 ? P3d.newP(abc[0]) : new P3d()); // origin
    // a' = 2pi/V * b x c  = 2pi * (b x c) / (a . (b x c))
    // b' = 2pi/V * c x a 
    // c' = 2pi/V * a x b 
    for (int i = 0; i < 3; i++) {
      P3d v = rabc[i + 1] = new P3d();
      v.cross(abc[((i + 1) % 3) + off], abc[((i + 2) % 3) + off]);
      v.scale(scale / abc[i + off].dot(v));
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
  public static T3d[] setOabc(String abcabg, float[] params, T3d[] ucnew) {
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

  public static T3d[] setOabcD(String abcabg, double[] params, T3d[] ucnew) {
    if (abcabg != null) {
      if (params == null)
        params = new double[6];
      String[] tokens = PT.split(abcabg.replace(',', '='), "=");
      if (tokens.length >= 12)
        for (int i = 0; i < 6; i++)
          params[i] = PT.parseFloat(tokens[i * 2 + 1]);
    }
    if (ucnew == null)
      return null;
    double[] f = newAD(params).getUnitCellAsArrayD(true);
      ucnew[1].set(f[0], f[1], f[2]);
      ucnew[2].set(f[3], f[4], f[5]);
      ucnew[3].set(f[6], f[7], f[8]);
    return ucnew;
  }

  public static void unitizeDim(int dimension, T3d pt) {
    switch (dimension) {
    case 3:
      pt.z = unitizeX(pt.z);  
      //$FALL-THROUGH$
    case 2:
      pt.y = unitizeX(pt.y);
      //$FALL-THROUGH$
    case 1:
      pt.x = unitizeX(pt.x);
    }
  }

  public static void unitizeDimRnd(int dimension, T3d pt) {
    switch (dimension) {
    case 3:
      pt.z = unitizeXRnd(pt.z);  
      //$FALL-THROUGH$
    case 2:
      pt.y = unitizeXRnd(pt.y);
      //$FALL-THROUGH$
    case 1:
      pt.x = unitizeXRnd(pt.x);
    }
  }

  public static double unitizeX(double x) {
    // introduced in Jmol 11.7.36
    x = (x - Math.floor(x));
    // question - does this cause problems with dragatom?
    if (x > 0.999 || x < 0.001)  // 0.9999, 0.0001 was just too tight ams/jolliffeite
      x = 0;
    return x;
  }

  public static double unitizeXRnd(double x) {
    // introduced in Jmol 11.7.36
    x = (x - Math.floor(x));
    if (x > 0.9999d || x < 0.0001f) 
      x = 0;
    return x;
  }
  


  public static float normalizeX12ths(double x) {
    return Math.round(x*12) / 12;
  }

  
  /**
   * allowance for rounding in [0,1)
   */
  private final static float SLOP2 = 0.0001f;
  
  /**
   * check atom position for range [0, 1) allowing for rounding

   * @param pt 
   * @return true if in [0, 1)
   */
  public static boolean checkPeriodic(P3d pt) {
    return (pt.x >= -SLOP2 && pt.x < 1 - SLOP2
        && pt.y >= -SLOP2 && pt.y < 1 - SLOP2
        && pt.z >= -SLOP2 && pt.z < 1 - SLOP2
        );
  }

  public static boolean checkUnitCell(SymmetryInterface uc, P3d cell, P3d ptTemp) {
    uc.toFractionalF(ptTemp, false);
    // {1 1 1} here is the original cell
    return (ptTemp.x >= cell.x - 1d - SLOP && ptTemp.x <= cell.x + SLOP
        && ptTemp.y >= cell.y - 1d - SLOP && ptTemp.y <= cell.y + SLOP
        && ptTemp.z >= cell.z - 1d - SLOP && ptTemp.z <= cell.z + SLOP);
  }

  ////// lattice methods //////
  
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
  public static void ijkToPoint3f(int nnn, P3d cell, int offset, int kcode) {
    int f = (nnn > 1000000000 ? 1000 : nnn > 1000000 ? 100 : 10);
    int f2 = f * f;
    offset -= (offset >= 0 ? 5 * f / 10 : offset);
    cell.x = ((nnn / f2) % f) + offset;
    cell.y = (nnn % f2) / f + offset;
    cell.z = (kcode == 0 ? nnn % f 
        : (offset == -500 ? kcode / f : kcode) % f) + offset;
  }
  

  /**
   * Convert user's {3 2 1} to {1500500500, 1503502501, 0 or 1, 1500501}
   * @param pt
   * @param scale 1 for block of unit cells; 0 for one large supercell
   * @return converted P4
   */
  public static P4d ptToIJK(T3d pt, int scale) {
    if (pt.x <= 5 && pt.y <= 5 && pt.z <= 5) {
      return P4d.new4(555, (pt.x + 4) * 100 + (pt.y + 4) * 10 + pt.z + 4, scale, 0);
    } 
    int i555 = 1500500500;
    return P4d.new4(i555, i555 + pt.x*1000000 + pt.y * 1000 + pt.z, scale, 1500500 + pt.z);
  }

  /**
   * Generally the multiplier is just {ijk ijk scale}, but when we have
   * 1iiijjjkkk 1iiijjjkkk scale, floats lose kkk due to Java float precision
   * issues so we use P4 {1iiijjjkkk 1iiijjjkkk scale, 1kkkkkk}
   * 
   * @param pt
   * @return String representation for state
   */
  public static String escapeMultiplier(T3d pt) {
    if (pt instanceof P4d) {
      P4d pt4 = (P4d) pt;
      int x = (int) Math.floor(pt4.x / 1000)*1000 
                  + (int) Math.floor(pt4.w / 1000) - 1000;
      int y = (int) Math.floor(pt4.y / 1000)*1000 
          + (int) Math.floor(pt4.w) % 1000;
      return "{" + x + " " + y + " " + pt.z + "}"; 
    }
    return Escape.eP(pt);
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
      P3d pt = new P3d();
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


  @Override
  public String toString() {
    return "[" + a + " " + b + " " + c + " " + alpha + " " + beta + " " + gamma + "]";
  }

}
