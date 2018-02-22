/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-05-13 19:17:06 -0500 (Sat, 13 May 2006) $
 * $Revision: 5114 $
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
package org.jmol.quantum.mo;

import org.jmol.quantum.MOCalculation;



/*
 * NOTE -- THIS CLASS IS INSTANTIATED USING Interface.getOptionInterface
 * NOT DIRECTLY -- FOR MODULARIZATION. NEVER USE THE CONSTRUCTOR DIRECTLY!
 * 
 */

/**
 * adds 10 cartesian F orbital contributions
 */
public class DataAdderF implements DataAdder {

  public DataAdderF() {
  }

  // Ordering from Gaussian:
  //  16        5XXX        0.00000   0.00000   0.00000   0.00397   0.00000
  //  17        5YYY        0.00000   0.00000   0.00397   0.00000   0.00000
  //  18        5ZZZ        0.00000   0.00397   0.00000   0.00000   0.00276
  //  19        5XYY        0.00000   0.00000   0.00000   0.00142   0.00000
  //  20        5XXY        0.00000   0.00000   0.00142   0.00000   0.00000
  //  21        5XXZ        0.00000   0.00142   0.00000   0.00000  -0.03279
  //  22        5XZZ        0.00000   0.00000   0.00000   0.00142   0.00000
  //  23        5YZZ        0.00000   0.00000   0.00142   0.00000   0.00000
  //  24        5YYZ        0.00000   0.00142   0.00000   0.00000  -0.03279
  //  25        5XYZ        0.00698   0.00000   0.00000   0.00000   0.00000
  
  // same as Molden
  
  @Override
  public boolean addData(MOCalculation calc, boolean havePoints) {
    double alpha;
    double c1;
    double a;
    double x, y, z, xx, yy, zz;
    double axxx, ayyy, azzz, axyy, axxy, axxz, axzz, ayzz, ayyz, axyz;
    double cxxx, cyyy, czzz, cxyy, cxxy, cxxz, cxzz, cyzz, cyyz, cxyz;

    /*
     Cartesian forms for f (l = 3) basis functions:
     Type         Normalization
     xxx          [(32768 * alpha^9) / (225 * pi^3))]^(1/4)
     xxy          [(32768 * alpha^9) / (9 * pi^3))]^(1/4)
     xxz          [(32768 * alpha^9) / (9 * pi^3))]^(1/4)
     xyy          [(32768 * alpha^9) / (9 * pi^3))]^(1/4)
     xyz          [(32768 * alpha^9) / (1 * pi^3))]^(1/4)
     xzz          [(32768 * alpha^9) / (9 * pi^3))]^(1/4)
     yyy          [(32768 * alpha^9) / (225 * pi^3))]^(1/4)
     yyz          [(32768 * alpha^9) / (9 * pi^3))]^(1/4)
     yzz          [(32768 * alpha^9) / (9 * pi^3))]^(1/4)
     zzz          [(32768 * alpha^9) / (225 * pi^3))]^(1/4)
     */

    double norm1, norm2, norm3;
    double[] coeffs = calc.coeffs;
    boolean normalizeAlpha = false;
    switch (calc.normType) {
    case MOCalculation.NORM_NONE:
    default:
      norm1 = norm2 = norm3 = 1;
      break;
    case MOCalculation.NORM_STANDARD:
      norm1 = 5.701643762839922;  //Math.pow(32768.0 / (Math.PI * Math.PI * Math.PI), 0.25);
      norm2 = 3.2918455612989796; //norm1 / Math.sqrt(3);
      norm3 = 1.4721580892990938; //norm1 / Math.sqrt(15);
      normalizeAlpha = true;
      break;
    case MOCalculation.NORM_NWCHEM:
      // contraction needs to be normalized
      norm1 = calc.getContractionNormalization(3, 1);
      norm2 = norm1;
      norm3 = norm1;        
      normalizeAlpha = true;
      break;
    case MOCalculation.NORM_NBO:
      norm1 = Math.sqrt(15);
      norm2 = Math.sqrt(5);
      norm3 = 1;
      break;
    }

    double mxxx = coeffs[0];
    double myyy = coeffs[1];
    double mzzz = coeffs[2];
    double mxyy = coeffs[3];
    double mxxy = coeffs[4];
    double mxxz = coeffs[5];
    double mxzz = coeffs[6];
    double myzz = coeffs[7];
    double myyz = coeffs[8];
    double mxyz = coeffs[9];
    for (int ig = 0; ig < calc.nGaussians; ig++) {
      alpha = calc.gaussians[calc.gaussianPtr + ig][0];
      c1 = calc.gaussians[calc.gaussianPtr + ig][1];
      calc.setE(calc.EX, alpha);

      // common factor of contraction coefficient and alpha normalization 
      // factor; only call pow once per primitive
      a = c1;
      if (normalizeAlpha)
        a *= Math.pow(alpha, 2.25);

      axxx = a * norm3 * mxxx;
      ayyy = a * norm3 * myyy;
      azzz = a * norm3 * mzzz;
      axyy = a * norm2 * mxyy;
      axxy = a * norm2 * mxxy;
      axxz = a * norm2 * mxxz;
      axzz = a * norm2 * mxzz;
      ayzz = a * norm2 * myzz;
      ayyz = a * norm2 * myyz;
      axyz = a * norm1 * mxyz;

      for (int ix = calc.xMax; --ix >= calc.xMin;) {
        x = calc.X[ix];
        xx = x * x;

        double Ex = calc.EX[ix];
        cxxx = axxx * xx * x;

        if (havePoints)
          calc.setMinMax(ix);
        for (int iy = calc.yMax; --iy >= calc.yMin;) {
          y = calc.Y[iy];
          yy = y * y;
          double Exy = Ex * calc.EY[iy];
          cyyy = ayyy * yy * y;
          cxxy = axxy * xx * y;
          cxyy = axyy * x * yy;
          float[] vd = calc.voxelDataTemp[ix][(havePoints ? 0 : iy)]; 

          for (int iz = calc.zMax; --iz >= calc.zMin;) {
            z = calc.Z[iz];
            zz = z * z;
            czzz = azzz * zz * z;
            cxxz = axxz * xx * z;
            cxzz = axzz * x * zz;
            cyyz = ayyz * yy * z;
            cyzz = ayzz * y * zz;
            cxyz = axyz * x * y * z;
            vd[(havePoints ? 0 : iz)] += (cxxx + cyyy + czzz + cxyy + cxxy
                + cxxz + cxzz + cyzz + cyyz + cxyz)
                * Exy * calc.EZ[iz];
          }
        }
      }
    }
    return true;
  }

}
