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
 * adds 7 spherical F orbital contributions
 */
public class DataAdder7F implements DataAdder {

  public DataAdder7F() {
  }

  // Linear combination coefficients for the various Cartesian gaussians
  final static double c0_xxz_yyz = 0.6708203932499369; //(3.0 / (2.0 * Math.sqrt(5))); // ok

  final static double c1p_xzz = 1.0954451150103321; //Math.sqrt(3.0 / 5.0) * Math.sqrt(2); // ok
  final static double c1p_xxx = 0.6123724356957945; //Math.sqrt(3.0 / 4.0) * Math.sqrt(2); // ok
  final static double c1p_xyy = 0.27386127875258304; //Math.sqrt(3.0 / 5)/4 * Math.sqrt(2); // ok
  final static double c1n_yzz = c1p_xzz;
  final static double c1n_yyy = c1p_xxx;
  final static double c1n_xxy = c1p_xyy;

  final static double c2p_xxz_yyz = 0.8660254037844386; //Math.sqrt(3.0 / 4.0);

  final static double c3p_xxx = 0.7905694150420949; //Math.sqrt(5.0)/4 * Math.sqrt(2); // ok
              // /2 for NBO? 
  final static double c3p_xyy = 1.0606601717798214; //0.75 * Math.sqrt(2);
  final static double c3n_yyy = c3p_xxx;
  final static double c3n_xxy = c3p_xyy;


  @Override
  public boolean addData(MOCalculation calc, boolean havePoints) {
    // expects 7 real orbitals in the order f0, f+1, f-1, f+2, f-2, f+3, f-3

    double alpha, c1, a;
    double x, y, z, xx, yy, zz;
    double cxxx, cyyy, czzz, cxyy, cxxy, cxxz, cxzz, cyzz, cyyz, cxyz;
    double af0, af1p, af1n, af2p, af2n, af3p, af3n;
    double f0, f1p, f1n, f2p, f2n, f3p, f3n;
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

    
    // TODO:  Check if norm4 should be -1 for all programs
    
    double norm1, norm2, norm3, norm4;
        
    if (calc.doNormalize) {
      if (calc.nwChemMode) {
        norm3 = calc.getContractionNormalization(3, 1);
        norm2 = norm3 * Math.sqrt(5);
        norm1 = norm3 * Math.sqrt(15);
        norm4 = -1;
      } else {
        norm1 = 5.701643762839922;  //Math.pow(32768.0 / (Math.PI * Math.PI * Math.PI), 0.25);
        norm2 = 3.2918455612989796; //norm1 / Math.sqrt(3);
        norm3 = 1.4721580892990938; //norm1 / Math.sqrt(15);
        // norm4 verified for Gaussian using CeO2.log 
        norm4 = 1;
      }

    } else {
      norm1 = norm2 = norm3 = norm4 = 1;
    }

    double m0 = calc.coeffs[0];
    double m1p = calc.coeffs[1];
    double m1n = calc.coeffs[2];
    double m2p = calc.coeffs[3];
    double m2n = calc.coeffs[4];
    double m3p = calc.coeffs[5];
    double m3n = calc.coeffs[6];

    for (int ig = 0; ig < calc.nGaussians; ig++) {
      alpha = calc.gaussians[calc.gaussianPtr + ig][0];
      c1 = calc.gaussians[calc.gaussianPtr + ig][1];
      a = c1;
      if (calc.doNormalize)
        a *= Math.pow(alpha, 2.25);

      af0 = a * m0;
      af1p = a * m1p;
      af1n = a * m1n;
      af2p = a * m2p;
      af2n = a * m2n;
      af3p = a * m3p;
      af3n = a * m3n;

      calc.setE(calc.EX, alpha);

      for (int ix = calc.xMax; --ix >= calc.xMin;) {
        x = calc.X[ix];
        xx = x * x;
        double eX = calc.EX[ix];
        cxxx = norm3 * x * xx;
        if (havePoints)
          calc.setMinMax(ix);
        for (int iy = calc.yMax; --iy >= calc.yMin;) {
          y = calc.Y[iy];
          yy = y * y;
          double eXY = eX * calc.EY[iy];

          cyyy = norm3 * y * yy;
          cxyy = norm2 * x * yy;
          cxxy = norm2 * xx * y;
          float[] vd = calc.voxelDataTemp[ix][(havePoints ? 0 : iy)]; 

          for (int iz = calc.zMax; --iz >= calc.zMin;) {
            z = calc.Z[iz];
            zz = z * z;

            czzz = norm3 * z * zz;
            cxxz = norm2 * xx * z;
            cxzz = norm2 * x * zz;
            cyyz = norm2 * yy * z;
            cyzz = norm2 * y * zz;
            cxyz = norm1 * x * y * z;

            f0 = af0 * (czzz - c0_xxz_yyz * (cxxz + cyyz));
            f1p = norm4 * af1p * (c1p_xzz * cxzz - c1p_xxx * cxxx
                - c1p_xyy * cxyy
                );
            f1n = af1n * (c1n_yzz * cyzz
                - c1n_yyy * cyyy
                - c1n_xxy * cxxy
                );
            f2p = af2p * (c2p_xxz_yyz * (cxxz - cyyz));
            f2n = af2n * cxyz;
            f3p = norm4 * af3p * (c3p_xxx * cxxx
                - c3p_xyy * cxyy);
            f3n = -af3n * (c3n_yyy * cyyy
                - c3n_xxy * cxxy);
            vd[(havePoints ? 0 : iz)] += (f0 + f1p + f1n + f2p + f2n + f3p + f3n)
                * eXY * calc.EZ[iz];
          }
        }
      }
    }
    return true;
  }

}
