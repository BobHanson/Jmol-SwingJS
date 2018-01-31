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
package org.jmol.quantum;

import javajs.util.Lst;
import javajs.util.T3;

import org.jmol.java.BS;
import org.jmol.jvxl.data.VolumeData;
import org.jmol.modelset.Atom;
import org.jmol.util.Logger;




/*
 * See J. Computational Chemistry, vol 7, p 359, 1986.
 * thanks go to Won Kyu Park, wkpark@chem.skku.ac.kr, 
 * jmol-developers list communication "JMOL AND CALCULATED ORBITALS !!!!"
 * and his http://chem.skku.ac.kr/~wkpark/chem/mocube.f
 * based on PSI88 http://www.ccl.net/cca/software/SOURCES/FORTRAN/psi88/index.shtml
 * http://www.ccl.net/cca/software/SOURCES/FORTRAN/psi88/src/psi1.f
 * 
 * While we are not exactly copying this code, I include here the information from that
 * FORTRAN as acknowledgment of the source of the algorithmic idea to use single 
 * row arrays to reduce the number of calculations.
 *  
 * Slater functions provided by JR Schmidt and Will Polik. Many thanks!
 * 
 * Spherical functions by Matthew Zwier <mczwier@gmail.com>
 * 
 * A neat trick here is using Java Point3f. null atoms allow selective removal of
 * their contribution to the MO. Maybe a first time this has ever been done?
 * 
 * Bob Hanson hansonr@stolaf.edu 7/3/06
 * 
 C
 C      DANIEL L. SEVERANCE
 C      WILLIAM L. JORGENSEN
 C      DEPARTMENT OF CHEMISTRY
 C      YALE UNIVERSITY
 C      NEW HAVEN, CT 06511
 C
 C      THIS CODE DERIVED FROM THE PSI1 PORTION OF THE ORIGINAL PSI77
 C      PROGRAM WRITTEN BY WILLIAM L. JORGENSEN, PURDUE.
 C      IT HAS BEEN REWRITTEN TO ADD SPEED AND BASIS FUNCTIONS. DLS
 C
 C      THE CONTOURING CODE HAS BEEN MOVED TO A SEPARATE PROGRAM TO ALLOW
 C      MULTIPLE CONTOURS TO BE PLOTTED WITHOUT RECOMPUTING THE
 C      ORBITAL VALUE MATRIX.
 C
 C Redistribution and use in source and binary forms are permitted
 C provided that the above paragraphs and this one are duplicated in 
 C all such forms and that any documentation, advertising materials,
 C and other materials related to such distribution and use acknowledge 
 C that the software was developed by Daniel Severance at Purdue University
 C The name of the University or Daniel Severance may not be used to endorse 
 C or promote products derived from this software without specific prior 
 C written permission.  The authors are now at Yale University.
 C THIS SOFTWARE IS PROVIDED ``AS IS'' AND WITHOUT ANY EXPRESS OR
 C IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED
 C WARRANTIES OF MERCHANTIBILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 */

/*
 * NOTE -- THIS CLASS IS INSTANTIATED USING Interface.getOptionInterface
 * NOT DIRECTLY -- FOR MODULARIZATION. NEVER USE THE CONSTRUCTOR DIRECTLY!
 * 
 */

public class MOCalculation extends QuantumCalculation {

  private final static double CUT = -50;
  
  // slater coefficients in Bohr
  private double[] CX, CY, CZ;
  // d-orbital partial coefficients in Bohr
  private double[] DXY, DXZ, DYZ;
    // exp(-alpha x^2...)
  private double[] EX, EY, EZ;
  
  private String calculationType;
  private Lst<int[]> shells;
  private float[][] gaussians;
  //Hashtable aoOrdersDF;
  private SlaterData[] slaters;
  private float[] moCoefficients;
  private int moCoeff;
  private int gaussianPtr;
  //private float coefMax = Integer.MAX_VALUE;
  private boolean doNormalize = true;
  private boolean nwChemMode = false;
  //                                           0  S       1   P        2  SP   
  private int[][] dfCoefMaps = new int[][] {new int[1], new int[3], new int[4], 
        //    3   DS     4   DC      5   FS      6   FC
          new int[5], new int[6], new int[7], new int[10]};

  private float[] linearCombination;

  private float[][] coefs;

  private double moFactor = 1;
  private boolean havePoints;
  boolean testing = true;
  
  public MOCalculation() {
  }

  public boolean setupCalculation(VolumeData volumeData, BS bsSelected,
                        String calculationType, T3[] xyz, Atom[] atoms,
                        int firstAtomOffset, Lst<int[]> shells,
                        float[][] gaussians,
                        int[][] dfCoefMaps, Object slaters, float[] moCoefficients,
                        float[] linearCombination, boolean isSquaredLinear, 
                        float[][] coefs,
                        boolean doNormalize, T3[] points) {
    havePoints = (points != null);
    this.calculationType = calculationType;
    this.firstAtomOffset = firstAtomOffset;
    this.shells = shells;
    this.gaussians = gaussians;
    if (dfCoefMaps != null)
      this.dfCoefMaps = dfCoefMaps;
    coeffs = new double[this.dfCoefMaps[this.dfCoefMaps.length - 1].length];
    this.slaters = (SlaterData[]) slaters;
    this.moCoefficients = moCoefficients;
    this.linearCombination = linearCombination;
    this.isSquaredLinear = isSquaredLinear;
    this.coefs = coefs;
    this.doNormalize = doNormalize;
    Logger.info("Normalizing AOs: " + doNormalize + " slaters:" + (slaters != null));
    countsXYZ = volumeData.getVoxelCounts();
    initialize(countsXYZ[0], countsXYZ[1], countsXYZ[2], points);
    voxelData = volumeData.getVoxelData();
    voxelDataTemp = (isSquaredLinear ? new float[nX][nY][nZ] : voxelData);
    setupCoordinates(volumeData.getOriginFloat(), 
        volumeData.getVolumetricVectorLengths(), 
        bsSelected, xyz, atoms, points, false);
    doDebug = (Logger.debugging);
    return !bsSelected.isEmpty() && (slaters != null || checkCalculationType());
  }  
  
  @Override
  protected void initialize(int nX, int nY, int nZ, T3[] points) {
    initialize0(nX, nY, nZ, points);
    CX = new double[this.nX];
    CY = new double[this.nY];
    CZ = new double[this.nZ];

    DXY = new double[this.nX];
    DXZ = new double[this.nX];
    DYZ = new double[this.nY];

    EX = new double[this.nX];
    EY = new double[this.nY];
    EZ = new double[this.nZ];
  }

  @Override
  public void createCube() {
    setXYZBohr(points);
    processPoints();
    if (!isSquaredLinear && (doDebug || testing))
      calculateElectronDensity();
  }

  double sum = -1;
  
  @Override
  protected void processPoints() {
    if (linearCombination == null) {
      process();
    } else {
      if (sum < 0) {
        sum = 0;
        for (int i = 0; i < linearCombination.length; i += 2)
          sum += linearCombination[i] * linearCombination[i];
        sum = Math.sqrt(sum);
      }
      if (sum == 0)
        return;
      for (int i = 0; i < linearCombination.length; i += 2) {
        moFactor = linearCombination[i] / sum;
        if (moFactor == 0)
          continue;
        moCoefficients = coefs[(int) linearCombination[i + 1] - 1];
        process();
        if (isSquaredLinear)
          addValuesSquared(1);
      }
    }
  }

  //private double c = 1;
  @Override
  protected void process() {
    atomIndex = firstAtomOffset - 1;
    moCoeff = 0;
    if (slaters == null) {
      // each STO shell is the combination of one or more gaussians
      int nShells = shells.size();
      //Logger.info("Processing " + nShells + " Gaussian  shells");
//      if (c < 0) {
//        c = 0;
//        for (int i = 0; i < nShells; i++)
//          c += normalizeShell(i);
//        //System.out.println("sum[c^2] = " + c + "\t" + Math.sqrt(c));
//        c = Math.sqrt(c);
//        atomIndex = firstAtomOffset - 1;
//        moCoeff = 0;
//      }
      for (int i = 0; i < nShells; i++)
        processShell(i);
      return;
    }
    for (int i = 0; i < slaters.length; i++) {
      if (!processSlater(i))
        break;
    }
  }

  private boolean checkCalculationType() {
    if (calculationType == null) {
      Logger.warn("calculation type not identified -- continuing");
      return true;
    }
    nwChemMode = (calculationType.indexOf("NWCHEM") >= 0);
    if (nwChemMode)
      Logger.info("Normalization of contractions (NWCHEM)");
    /*if (calculationType.indexOf("5D") >= 0) {
     Logger
     .error("QuantumCalculation.checkCalculationType: can't read 5D basis sets yet: "
     + calculationType + " -- exit");
     return false;
     }*/
    if (calculationType.indexOf("+") >= 0 || calculationType.indexOf("*") >= 0) {
      Logger
          .warn("polarization/diffuse wavefunctions have not been tested fully: "
              + calculationType + " -- continuing");
    }
    if (calculationType.indexOf("?") >= 0) {
      Logger
          .warn("unknown calculation type may not render correctly -- continuing");
    } else if (points == null){
      Logger.info("calculation type: " + calculationType + " OK.");
    }
    return true;
  }

//  private double normalizeShell(int iShell) {
//    double c = 0;
//    int[] shell = shells.get(iShell);
//    int basisType = shell[1];
//    gaussianPtr = shell[2] - 1;
//    nGaussians = shell[3];
//    doShowShellType = doDebug;
//    //System.out.println(iShell + " basistype is " + basisType);
//    if (!setCoeffs(basisType, false))
//      return 0;
//    for (int i = map.length; --i >= 0;)
//      c += coeffs[i] * coeffs[i];
//    return c;
//  }

  private int nGaussians;
  private boolean doShowShellType;

  private String warned;
  
  private void processShell(int iShell) {
    int lastAtom = atomIndex;
    int[] shell = shells.get(iShell);
    atomIndex = shell[0] - 1 + firstAtomOffset;
    int basisType = shell[1];
    gaussianPtr = shell[2] - 1;
    nGaussians = shell[3];
    doShowShellType = doDebug;
    //System.out.println("shell " + iShell + " type " + basisType);
    if (atomIndex != lastAtom && (thisAtom = qmAtoms[atomIndex]) != null)
      thisAtom.setXYZ(this, true);
    if (!setCoeffs(shell[1], true))
      return;
    if (havePoints)
      setMinMax(-1);
    switch (basisType) {
    case QS.S:
      addDataS();
      break;
    case QS.P:
      addDataP();
      break;
    case QS.SP:
      addDataSP();
      break;
    case QS.DS:
      addData5D();
      break;
    case QS.DC:
        addData6D();
      break;
    case QS.FS:
      addData7F();
      break;
    case QS.FC:
        addData10F();
      break;
    default:
      if (warned == null)
        warned = "";
      String key = "=" + (atomIndex + 1) + ": " + QS.getQuantumShellTag(basisType);
      if (warned.indexOf(key) < 0) {
        warned += key;
        Logger.warn(" Unsupported basis type for atomno" + key);
      }
      break;
    }
  }
  
  private void addValuesSquared(float occupancy) {
    for (int ix = nX; --ix >= 0;) {
      for (int iy = nY; --iy >= 0;) {
        for (int iz = nZ; --iz >= 0;) {
          float value = voxelDataTemp[ix][iy][iz];
          if (value == 0)
            continue;
          voxelData[ix][iy][iz] += value * value * occupancy;
          voxelDataTemp[ix][iy][iz] = 0;
        }
      }
    }
  }
  
  /**
   * NWCHEM only
   * 
   * @param el
   * @param cpt
   * @return
   */
  private double getContractionNormalization(int el, int cpt) {
    double sum;
    double df = (el == 3 ? 15 : el == 2 ? 3 : 1);
    double f = df * Math.pow(Math.PI, 1.5) / Math.pow(2, el);
    double p = 0.75 + el / 2.0;
    if (nGaussians == 1) {
      sum = Math.pow(2, -2 * p) * Math.pow(gaussians[gaussianPtr][cpt], 2);
    } else {
      sum = 0;
      for (int ig1 = 0; ig1 < nGaussians; ig1++) {
        double alpha1 = gaussians[gaussianPtr + ig1][0];
        double c1 = gaussians[gaussianPtr + ig1][cpt];
        double f1 = Math.pow(alpha1, p);
        for (int ig2 = 0; ig2 < nGaussians; ig2++) {
          double alpha2 = gaussians[gaussianPtr + ig2][0];
          double c2 = gaussians[gaussianPtr + ig2][cpt];
          double f2 = Math.pow(alpha2, p);
          sum += c1 * f1 * c2 * f2 / Math.pow(alpha1 + alpha2, 2 * p);
          //if (nGaussians == 1)System.out.println(c1 + " " + f1 + " " + c2 + " " + f2);
        }
      }
    }
    sum = 1 / Math.sqrt(f * sum);
    if (Logger.debuggingHigh)
      Logger.debug("\t\t\tnormalization for l=" + el + " nGaussians=" + nGaussians + " is " + sum);
    return sum;
  }

  private double[] coeffs;
  private int[] map;

  private int lastGaussianPtr = -1;
  
  private boolean setCoeffs(int type, boolean isProcess) {
    boolean isOK = false;
    map = dfCoefMaps[type];
    if (type > QS.MAX_TYPE_SUPPORTED) {
      if (isProcess && doDebug)
        dumpInfo(type);
      moCoeff += map.length;
      return false;
    }
    if (isProcess && thisAtom == null) {
      moCoeff += map.length;
      return false;
    }
    for (int i = 0; i < map.length; i++)
      isOK |= ((coeffs[i] = moCoefficients[map[i] + moCoeff++]) != 0);
    isOK &= (coeffs[0] != Integer.MIN_VALUE);
    if (isOK && doDebug && isProcess)
      dumpInfo(type);
    return isOK;
  }
  
  private void addDataS() {
    double norm, c1;
    if (doNormalize) {
      if (nwChemMode) {
        // contraction needs to be normalized
        norm = getContractionNormalization(0, 1);
      } else {
        // (8 alpha^3/pi^3)^0.25 exp(-alpha r^2)
        norm = 0.712705470f; // (8/pi^3)^0.25 = (2/pi)^3/4
      }
    } else {
      norm = 1;
    }
   
    double m1 = coeffs[0];
    for (int ig = 0; ig < nGaussians; ig++) {
      double alpha = gaussians[gaussianPtr + ig][0];
      c1 = gaussians[gaussianPtr + ig][1];
      double a = norm * m1 * c1 * moFactor;
      if (doNormalize)
        a *= Math.pow(alpha, 0.75);
      // the coefficients are all included with the X factor here
      for (int i = xMax; --i >= xMin;) {
        EX[i] = a *  Math.exp(-X2[i] * alpha);
      }
      for (int i = yMax; --i >= yMin;) {
        EY[i] =  Math.exp(-Y2[i] * alpha);
      }
      for (int i = zMax; --i >= zMin;) {
        EZ[i] =  Math.exp(-Z2[i] * alpha);
      }

      for (int ix = xMax; --ix >= xMin;) {
        double eX = EX[ix];
        if (havePoints)
          setMinMax(ix);
        for (int iy = yMax; --iy >= yMin;) {
          double eXY = eX * EY[iy];
          vd = voxelDataTemp[ix][(havePoints ? 0 : iy)]; 
          for (int iz = zMax; --iz >= zMin;)
            vd[(havePoints ? 0 : iz)] += eXY * EZ[iz];
       }
      }
    }
  }

  private void addDataP() {
    double mx = coeffs[0];
    double my = coeffs[1];
    double mz = coeffs[2];
    double norm;

    if (doNormalize) {
      if (nwChemMode) {
        norm = getContractionNormalization(1, 1);
      } else {
        // (128 alpha^5/pi^3)^0.25 [x|y|z]exp(-alpha r^2)
        norm = 1.42541094f;
      }
    } else {
      norm = 1;
    }

    for (int ig = 0; ig < nGaussians; ig++) {
      double alpha = gaussians[gaussianPtr + ig][0];
      double c1 = gaussians[gaussianPtr + ig][1];
      double a = c1;
      if (doNormalize)
        a *= Math.pow(alpha, 1.25) * norm;
      calcSP(alpha, 0, a * mx, a * my, a * mz);
    }
  }

  private void addDataSP() {
    // spartan uses format "1" for BOTH SP and P, which is fine, but then
    // when c1 = 0, there is no mo coefficient, of course. 
    boolean isP = (map.length == 3);
    int pPt = (isP ? 0 : 1);
    double ms = (isP ? 0 : coeffs[0]);
    double mx = coeffs[pPt++];
    double my = coeffs[pPt++];
    double mz = coeffs[pPt++];
    double norm1, norm2;
    if (doNormalize) {
      if (nwChemMode) {
        norm1 = getContractionNormalization(0, 1);
        norm2 = getContractionNormalization(1, 2);
      } else {
        norm1 = 0.712705470f;
        norm2 = 1.42541094f;
      }
    } else {
      norm1 = norm2 = 1;
    }
    double a1, a2, c1, c2, alpha;
    for (int ig = 0; ig < nGaussians; ig++) {
      alpha = gaussians[gaussianPtr + ig][0];
      c1 = gaussians[gaussianPtr + ig][1];
      c2 = gaussians[gaussianPtr + ig][2];
      a1 = c1;
      a2 = c2;
      if (doNormalize) {
        a1 *= Math.pow(alpha, 0.75) * norm1;
        a2 *= Math.pow(alpha, 1.25) * norm2;
      }
      calcSP(alpha, a1 * ms, a2 * mx, a2 * my, a2 * mz);
    }
  }

  private void setCE(double alpha, double as, double ax,
                     double ay, double az) {
    for (int i = xMax; --i >= xMin;) {
      CX[i] = as + ax * X[i];
      EX[i] =  Math.exp(-X2[i] * alpha) * moFactor;
    }
    for (int i = yMax; --i >= yMin;) {
      CY[i] = ay * Y[i];
      EY[i] =  Math.exp(-Y2[i] * alpha);
    }
    for (int i = zMax; --i >= zMin;) {
      CZ[i] = az * Z[i];
      EZ[i] =  Math.exp(-Z2[i] * alpha);
    }
  }

  private void setE(double[] EX, double alpha) {
    for (int i = xMax; --i >= xMin;)
      EX[i] =  Math.exp(-X2[i] * alpha) * moFactor;
    for (int i = yMax; --i >= yMin;)
      EY[i] =  Math.exp(-Y2[i] * alpha);
    for (int i = zMax; --i >= zMin;)
      EZ[i] =  Math.exp(-Z2[i] * alpha);
  }

  private void calcSP(double alpha, double as, double ax, double ay, double az) {
    setCE(alpha, as, ax, ay, az);
    for (int ix = xMax; --ix >= xMin;) {
      double eX = EX[ix];
      double cX = CX[ix];
      if (havePoints)
        setMinMax(ix);
      for (int iy = yMax; --iy >= yMin;) {
        double eXY = eX * EY[iy];
        double cXY = cX + CY[iy];
        vd = voxelDataTemp[ix][(havePoints ? 0 : iy)]; 
        for (int iz = zMax; --iz >= zMin;) {
          vd[(havePoints ? 0 : iz)] += (cXY + CZ[iz]) * eXY * EZ[iz];
        }
      }
    }
  }

  private final static double ROOT3 = 1.73205080756887729f;

  private void addData6D() {
    //expects 6 orbitals in the order XX YY ZZ XY XZ YZ
    double mxx = coeffs[0];
    double myy = coeffs[1];
    double mzz = coeffs[2];
    double mxy = coeffs[3];
    double mxz = coeffs[4];
    double myz = coeffs[5];
    double norm1, norm2;
    if (doNormalize) {
      if (nwChemMode) {
        norm1 = getContractionNormalization(2, 1);
        norm2 = norm1;
      } else {
        norm1 = 2.8508219178923f;
        norm2 = norm1 / ROOT3;
      }
    } else {
      norm2 = 1 / ROOT3;
      norm1 = 1;
    }
    for (int ig = 0; ig < nGaussians; ig++) {
      double alpha = gaussians[gaussianPtr + ig][0];
      double c1 = gaussians[gaussianPtr + ig][1];
      // xx|yy|zz: (2048 alpha^7/9pi^3)^0.25 [xx|yy|zz]exp(-alpha r^2)
      // xy|xz|yz: (2048 alpha^7/pi^3)^0.25 [xy|xz|yz]exp(-alpha r^2)
      double a = c1;
      if (doNormalize)
        a *=  Math.pow(alpha, 1.75);
      double axy = a * norm1 * mxy;
      double axz = a * norm1 * mxz;
      double ayz = a * norm1 * myz;
      double axx = a * norm2 * mxx;
      double ayy = a * norm2 * myy;
      double azz = a * norm2 * mzz;
      setCE(alpha, 0, axx, ayy, azz);

      for (int i = xMax; --i >= xMin;) {
        DXY[i] = axy * X[i];
        DXZ[i] = axz * X[i];
      }
      for (int i = yMax; --i >= yMin;) {
        DYZ[i] = ayz * Y[i];
      }
      for (int ix = xMax; --ix >= xMin;) {
        double axx_x2 = CX[ix] * X[ix];
        double axy_x = DXY[ix];
        double axz_x = DXZ[ix];
        double eX = EX[ix];
        if (havePoints)
          setMinMax(ix);
        for (int iy = yMax; --iy >= yMin;) {
          double axx_x2__ayy_y2__axy_xy = axx_x2 + (CY[iy] + axy_x) * Y[iy];
          double axz_x__ayz_y = axz_x + DYZ[iy];
          double eXY = eX * EY[iy];
          vd = voxelDataTemp[ix][(havePoints ? 0 : iy)]; 
          for (int iz = zMax; --iz >= zMin;) {
            vd[(havePoints ? 0 : iz)] += (axx_x2__ayy_y2__axy_xy + (CZ[iz] + axz_x__ayz_y) * Z[iz])
                * eXY * EZ[iz];
            // giving (axx_x2 + ayy_y2 + azz_z2 + axy_xy + axz_xz + ayz_yz)e^-br2; 
          }
        }
      }
    }
  }

  private void addData5D() {
    // expects 5 real orbitals in the order d0, d+1, d-1, d+2, d-2
    // (i.e. dz^2, dxz, dyz, dx^2-y^2, dxy)
    // To avoid actually having to use spherical harmonics, we use 
    // linear combinations of Cartesian harmonics.  

    // For conversions between spherical and Cartesian gaussians, see
    // "Trasnformation Between Cartesian and Pure Spherical Harmonic Gaussians",
    // Schlegel and Frisch, Int. J. Quant. Chem 54, 83-87, 1995

    double alpha, c1, a;
    double x, y, z;
    double cxx, cyy, czz, cxy, cxz, cyz;
    double ad0, ad1p, ad1n, ad2p, ad2n;

    /*
     Cartesian forms for d (l = 2) basis functions:
     Type         Normalization
     xx           [(2048 * alpha^7) / (9 * pi^3))]^(1/4)
     xy           [(2048 * alpha^7) / (1 * pi^3))]^(1/4)
     xz           [(2048 * alpha^7) / (1 * pi^3))]^(1/4)
     yy           [(2048 * alpha^7) / (9 * pi^3))]^(1/4)
     yz           [(2048 * alpha^7) / (1 * pi^3))]^(1/4)
     zz           [(2048 * alpha^7) / (9 * pi^3))]^(1/4)
     */

    double norm1, norm2, norm3, norm4;
    
    if (doNormalize) {
      if (nwChemMode) {
        norm2 = getContractionNormalization(2, 1);
        norm1 = norm2 * ROOT3;
        norm4 = -1;
      } else {
        // same as above, except for norm4. 
        // norm4 verified using CeO2.log 
        norm1 = Math.pow(2048.0 / (Math.PI * Math.PI * Math.PI), 0.25);
        norm2 = norm1 / ROOT3;
        norm4 = 1;
      }
      norm3 = ROOT3 / 2; // Normalization constant that shows up for dx^2-y^2
    } else {
      norm1 = norm2 = norm3 = norm4 = 1;
    }

    double m0 = coeffs[0];
    double m1p = coeffs[1];
    double m1n = coeffs[2];
    double m2p = coeffs[3];
    double m2n = coeffs[4];
    
    for (int ig = 0; ig < nGaussians; ig++) {
      alpha = gaussians[gaussianPtr + ig][0];
      c1 = gaussians[gaussianPtr + ig][1];
      a = c1;
      if (doNormalize)
        a *=  Math.pow(alpha, 1.75);

      ad0 = a * m0;
      ad1p = norm4 * a * m1p;
      ad1n = a * m1n;
      ad2p = a * m2p;
      ad2n = a * m2n;

      setE(EX, alpha);

      for (int ix = xMax; --ix >= xMin;) {
        x = X[ix];
        double eX = EX[ix];
        cxx = norm2 * x * x;

        if (havePoints)
          setMinMax(ix);
        for (int iy = yMax; --iy >= yMin;) {
          y = Y[iy];
          double eXY = eX * EY[iy];

          cyy = norm2 * y * y;
          cxy = norm1 * x * y;
          vd = voxelDataTemp[ix][(havePoints ? 0 : iy)]; 

          for (int iz = zMax; --iz >= zMin;) {
            z = Z[iz];

            czz = norm2 * z * z;
            cxz = norm1 * x * z;
            cyz = norm1 * y * z;

            vd[(havePoints ? 0 : iz)] += (
                ad0 * (czz - 0.5f * (cxx + cyy)) 
                + ad1p * cxz 
                + ad1n * cyz 
                + ad2p * norm3 * (cxx - cyy) 
                + ad2n * cxy)
                * eXY * EZ[iz];
          }
        }
      }
    }
  }

  private void addData10F() {
    // expects 10 orbitals in the order XXX, YYY, ZZZ, XYY, XXY, 
    //                                  XXZ, XZZ, YZZ, YYZ, XYZ
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
    if (doNormalize) {
      if (nwChemMode) {
        norm1 = getContractionNormalization(3, 1);
        norm2 = norm1;
        norm3 = norm1;        
      } else {
        norm1 = 5.701643762839922;  //Math.pow(32768.0 / (Math.PI * Math.PI * Math.PI), 0.25);
        norm2 = 3.2918455612989796; //norm1 / Math.sqrt(3);
        norm3 = 1.4721580892990938; //norm1 / Math.sqrt(15);
      }

    } else {
      norm1 = norm2 = norm3 = 1;
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
    for (int ig = 0; ig < nGaussians; ig++) {
      alpha = gaussians[gaussianPtr + ig][0];
      c1 = gaussians[gaussianPtr + ig][1];
      setE(EX, alpha);

      // common factor of contraction coefficient and alpha normalization 
      // factor; only call pow once per primitive
      a = c1;
      if (doNormalize)
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

      for (int ix = xMax; --ix >= xMin;) {
        x = X[ix];
        xx = x * x;

        double Ex = EX[ix];
        cxxx = axxx * xx * x;

        if (havePoints)
          setMinMax(ix);
        for (int iy = yMax; --iy >= yMin;) {
          y = Y[iy];
          yy = y * y;
          double Exy = Ex * EY[iy];
          cyyy = ayyy * yy * y;
          cxxy = axxy * xx * y;
          cxyy = axyy * x * yy;
          vd = voxelDataTemp[ix][(havePoints ? 0 : iy)]; 

          for (int iz = zMax; --iz >= zMin;) {
            z = Z[iz];
            zz = z * z;
            czzz = azzz * zz * z;
            cxxz = axxz * xx * z;
            cxzz = axzz * x * zz;
            cyyz = ayyz * yy * z;
            cyzz = ayzz * y * zz;
            cxyz = axyz * x * y * z;
            vd[(havePoints ? 0 : iz)] += (cxxx + cyyy + czzz + cxyy + cxxy
                + cxxz + cxzz + cyzz + cyyz + cxyz)
                * Exy * EZ[iz];
          }
        }
      }
    }
  }

  private void addData7F() {
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
        
    if (doNormalize) {
      if (nwChemMode) {
        norm3 = getContractionNormalization(3, 1);
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

    // Linear combination coefficients for the various Cartesian gaussians
    final double c0_xxz_yyz = 0.6708203932499369; //(3.0 / (2.0 * Math.sqrt(5)));

    final double c1p_xzz = 1.0954451150103321; //Math.sqrt(6.0 / 5.0);
    final double c1p_xxx = 0.6123724356957945; //Math.sqrt(3.0 / 8.0);
    final double c1p_xyy = 0.27386127875258304; //Math.sqrt(3.0 / 40.0);
    final double c1n_yzz = c1p_xzz;
    final double c1n_yyy = c1p_xxx;
    final double c1n_xxy = c1p_xyy;

    final double c2p_xxz_yyz = 0.8660254037844386; //Math.sqrt(3.0 / 4.0);

    final double c3p_xxx = 0.7905694150420949; //Math.sqrt(5.0 / 8.0);
    final double c3p_xyy = 1.0606601717798214; //0.75 * Math.sqrt(2);
    final double c3n_yyy = c3p_xxx;
    final double c3n_xxy = c3p_xyy;

    double m0 = coeffs[0];
    double m1p = coeffs[1];
    double m1n = coeffs[2];
    double m2p = coeffs[3];
    double m2n = coeffs[4];
    double m3p = coeffs[5];
    double m3n = coeffs[6];

    for (int ig = 0; ig < nGaussians; ig++) {
      alpha = gaussians[gaussianPtr + ig][0];
      c1 = gaussians[gaussianPtr + ig][1];
      a = c1;
      if (doNormalize)
        a *= Math.pow(alpha, 2.25);

      af0 = a * m0;
      af1p = a * m1p;
      af1n = a * m1n;
      af2p = a * m2p;
      af2n = a * m2n;
      af3p = a * m3p;
      af3n = a * m3n;

      setE(EX, alpha);

      for (int ix = xMax; --ix >= xMin;) {
        x = X[ix];
        xx = x * x;
        double eX = EX[ix];
        cxxx = norm3 * x * xx;
        if (havePoints)
          setMinMax(ix);
        for (int iy = yMax; --iy >= yMin;) {
          y = Y[iy];
          yy = y * y;
          double eXY = eX * EY[iy];

          cyyy = norm3 * y * yy;
          cxyy = norm2 * x * yy;
          cxxy = norm2 * xx * y;
          vd = voxelDataTemp[ix][(havePoints ? 0 : iy)]; 

          for (int iz = zMax; --iz >= zMin;) {
            z = Z[iz];
            zz = z * z;

            czzz = norm3 * z * zz;
            cxxz = norm2 * xx * z;
            cxzz = norm2 * x * zz;
            cyyz = norm2 * yy * z;
            cyzz = norm2 * y * zz;
            cxyz = norm1 * x * y * z;

            f0 = af0 * (czzz - c0_xxz_yyz * (cxxz + cyyz));
            f1p = norm4 * af1p * (c1p_xzz * cxzz - c1p_xxx * cxxx - c1p_xyy * cxyy);
            f1n = af1n * (c1n_yzz * cyzz - c1n_yyy * cyyy - c1n_xxy * cxxy);
            f2p = af2p * (c2p_xxz_yyz * (cxxz - cyyz));
            f2n = af2n * cxyz;
            f3p = norm4 * af3p * (c3p_xxx * cxxx - c3p_xyy * cxyy);
            f3n = -af3n * (c3n_yyy * cyyy - c3n_xxy * cxxy);
            vd[(havePoints ? 0 : iz)] += (f0 + f1p + f1n + f2p + f2n + f3p + f3n)
                * eXY * EZ[iz];
          }
        }
      }
    }
  }

  private boolean processSlater(int slaterIndex) {
    /*
     * We have two data structures for each slater, using the WebMO format: 
     * 
     * int[] slaterInfo[] = {iatom, a, b, c, d}
     * float[] slaterData[] = {zeta, coef}
     * 
     * where
     * 
     *  psi = (coef)(x^a)(y^b)(z^c)(r^d)exp(-zeta*r)
     * 
     * except: a == -2 ==> z^2 ==> (coef)(2z^2-x^2-y^2)(r^d)exp(-zeta*r)
     *    and: b == -2 ==> (coef)(x^2-y^2)(r^d)exp(-zeta*r)
     *    
     *    NOTE: A negative zeta means this is contracted!
     */

    int lastAtom = atomIndex;
    SlaterData slater = slaters[slaterIndex];
    atomIndex = slater.atomNo - 1;
    //System.out.println("MOCALC SLATER " + slaterIndex + " " + lastAtom + " " + atomIndex);
    double minuszeta = -slater.zeta;
    if ((thisAtom = qmAtoms[atomIndex]) == null) {
      if (minuszeta <= 0)
        moCoeff++;
      return true;
    }
    if (minuszeta > 0) { //this is contracted; use previous moCoeff
      minuszeta = -minuszeta;
      moCoeff--;
    }
    if (moCoeff >= moCoefficients.length)
      return false;
    double coef = slater.coef * moCoefficients[moCoeff++];
    //coefMax = 0.2f;
    if (coef == 0) { //|| coefMax != Integer.MAX_VALUE && Math.abs(coef) > coefMax) {
      atomIndex = -1;
      return true;
    }
    coef *= moFactor;
    if (atomIndex != lastAtom)
      thisAtom.setXYZ(this, true);
    int a = slater.x;
    int b = slater.y;
    int c = slater.z;
    int d = slater.r;
    //  System.out.println("MOCALC " + slaterIndex + " atomNo=" + (atomIndex+1) + "\tx^" + a + " y^"+ b + " z^" + c + " r^" + d + "\tzeta=" + (-minuszeta) + "\tcoef=" + coef);
          //+ " minmax " + xMin + " " + xMax + " " + yMin + " " + yMax + " " + zMin + " " + zMax
    if (a == -2) /* if dz2 */
      for (int ix = xMax; --ix >= xMin;) {
        double dx2 = X2[ix];
        if (havePoints)
          setMinMax(ix);
        for (int iy = yMax; --iy >= yMin;) {
          double dy2 = Y2[iy];
          double dx2y2 = dx2 + dy2;
          vd = voxelDataTemp[ix][(havePoints ? 0 : iy)]; 
          for (int iz = zMax; --iz >= zMin;) {
            double dz2 = Z2[iz];
            double r2 = dx2y2 + dz2;
            double r = Math.sqrt(r2);
            double exponent = minuszeta * r;
            if (exponent < CUT)
              continue;
            double value = (coef * Math.exp(exponent)
                * (3 * dz2 - r2));
            switch(d) {
            case 3:
              value *= r;
              //$FALL-THROUGH$
            case 2:
              value *= r2;
              break;
            case 1:
              value *= r;
              break;
            }
            vd[(havePoints ? 0 : iz)] += value;
          }
        }
      }
    else if (b == -2) /* if dx2-dy2 */
      for (int ix = xMax; --ix >= xMin;) {
        double dx2 = X2[ix];
        if (havePoints)
          setMinMax(ix);
        for (int iy = yMax; --iy >= yMin;) {
          double dy2 = Y2[iy];
          double dx2y2 = dx2 + dy2;
          double dx2my2 = coef * (dx2 - dy2);
          vd = voxelDataTemp[ix][(havePoints ? 0 : iy)]; 
          for (int iz = zMax; --iz >= zMin;) {
            double dz2 = Z2[iz];
            double r2 = dx2y2 + dz2;
            double r = Math.sqrt(r2);
            double exponent = minuszeta * r;
            if (exponent < CUT)
              continue;
            double value = dx2my2 * Math.exp(exponent);
            switch(d) {
            case 3:
              value *= r;
              //$FALL-THROUGH$
            case 2:
              value *= r2;
              break;
            case 1:
              value *= r;
              break;
            }
            vd[(havePoints ? 0 : iz)] += value;
          }
        }
      }
    else
      /* everything else */
      for (int ix = xMax; --ix >= xMin;) {
        double dx2 = X2[ix];
        double vdx = coef;
        switch(a) {
        case 3:
          vdx *= X[ix];
          //$FALL-THROUGH$
        case 2:
          vdx *= dx2;
          break;
        case 1:
          vdx *= X[ix];
          break;
        }
        if (havePoints)
          setMinMax(ix);
        for (int iy = yMax; --iy >= yMin;) {
          double dy2 = Y2[iy];
          double dx2y2 = dx2 + dy2;
          double vdy = vdx;
          switch(b) {
          case 3:
            vdy *= Y[iy];
            //$FALL-THROUGH$
          case 2:
            vdy *= dy2;
            break;
          case 1:
            vdy *= Y[iy];
            break;
          }
          vd = voxelDataTemp[ix][(havePoints ? 0 : iy)]; 
          for (int iz = zMax; --iz >= zMin;) {
            double dz2 = Z2[iz];
            double r2 = dx2y2 + dz2;
            double r = Math.sqrt(r2);
            double exponent = minuszeta * r;
            if (exponent < CUT)
              continue;
            double value = vdy * Math.exp(exponent);
            switch(c) {
            case 3:
              value *= Z[iz];
              //$FALL-THROUGH$
            case 2:
              value *= dz2;
              break;
            case 1:
              value *= Z[iz];
              break;
            }
            switch(d) {
            case 3:
              value *= r;
              //$FALL-THROUGH$
            case 2:
              value *= r2;
              break;
            case 1:
              value *= r;
              break;
            }
            vd[(havePoints ? 0 : iz)] += value;
            //if (ix == 27 && iy == 27)
              //System.out.println(iz + "\t"  
                //  + xBohr[ix] + " " + yBohr[iy] + " " 
                //  +  zBohr[iz] + "\t" 
                //  + X[ix] + " " + Y[iy] + " " + Z[iz] 
                //  + "--  r=" + r + " v=" + value + "\t" + voxelDataTemp[ix][iy][iz]);
             
            

          }
        }
      }
    
/*    for (int iz = 0; iz < 55; iz++) {
      System.out.println(iz + "\t"  
          //  + xBohr[ix] + " " + yBohr[iy] + " " 
            +  zBohr[iz] + "\t" 
          //  + X[ix] + " " + Y[iy] + " " + Z[iz] 
            + "\t"  + voxelDataTemp[27][27][iz]);
    }
*/ 
    return true;
  }

  private void dumpInfo(int shell) {
    if (doShowShellType) {
      Logger.debug("\n\t\t\tprocessShell: " + shell + " type="
          + QS.getQuantumShellTag(shell) + " nGaussians="
          + nGaussians + " atom=" + atomIndex);
      doShowShellType = false;
    }
    if (Logger.isActiveLevel(Logger.LEVEL_DEBUGHIGH) && gaussianPtr != lastGaussianPtr) {
      lastGaussianPtr  = gaussianPtr;
      for (int ig = 0; ig < nGaussians; ig++) {
        double alpha = gaussians[gaussianPtr + ig][0];
        double c1 = gaussians[gaussianPtr + ig][1];
        Logger.debug("\t\t\tGaussian " + (ig + 1) + " alpha=" + alpha + " c="
            + c1);
      }
    }
    String[] so = getShellOrder(shell);
      for (int i = 0; i < map.length; i++) {
        int n = map[i] + moCoeff - map.length + i + 1;
        double c = coeffs[i];
          Logger.debug("MO coeff " + (so == null ? "?" : so[i]) + " "
              + n + "\t" + c + "\t" + thisAtom.atom);
      
    }
  }
  
  private final static String[][] shellOrder = { 
    {"S"},
    {"X", "Y", "Z"},
    {"S", "X", "Y", "Z"},
    {"d0/z2", "d1+/xz", "d1-/yz", "d2+/x2-y2", "d2-/xy"},
    {"XX", "YY", "ZZ", "XY", "XZ", "YZ"},
    {"f0/2z3-3x2z-3y2z", "f1+/4xz2-x3-xy2", "f1-/4yz2-x2y-y3", 
      "f2+/x2z-y2z", "f2-/xyz", "f3+/x3-3xy2", "f3-/3x2y-y3"},
    {"XXX", "YYY", "ZZZ", "XYY", "XXY", "XXZ", "XZZ", "YZZ", "YYZ", "XYZ"},
  };

  final private static String[] getShellOrder(int i) {
    return (i < 0 || i >= shellOrder.length ? null : shellOrder[i]);
  }
  


  private boolean isSquaredLinear;

  public void calculateElectronDensity() {
    if (points != null)
      return;
    integration = 0;
    for (int ix = nX; --ix >= 0;)
      for (int iy = nY; --iy >= 0;)
        for (int iz = nZ; --iz >= 0;) {
          float x = voxelData[ix][iy][iz];
          integration += x * x;
        }
    float volume = stepBohr[0] * stepBohr[1] * stepBohr[2]; 
        // / bohr_per_angstrom / bohr_per_angstrom / bohr_per_angstrom;
    integration *= volume;
    Logger.info("Integrated density = " + integration);
  }

}
