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

package org.jmol.symmetry;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.util.BoxInfo;
import org.jmol.util.Logger;
import org.jmol.util.Parser;

import javajs.util.Lst;
import javajs.util.M3d;
import javajs.util.M4d;
import javajs.util.Matrix;
import javajs.util.MeasureD;
import javajs.util.P3d;
import javajs.util.P4d;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.T3d;
import javajs.util.V3d;

/*
 * Bob Hanson 4/2006
 * 
 * references: International Tables for Crystallography Vol. A. (2002) 
 *
 * http://www.iucr.org/iucr-top/cif/cifdic_html/1/cif_core.dic/Ispace_group_symop_operation_xyz.html
 * http://www.iucr.org/iucr-top/cif/cifdic_html/1/cif_core.dic/Isymmetry_equiv_pos_as_xyz.html
 *
 * LATT : http://macxray.chem.upenn.edu/LATT.pdf thank you, Patrick Carroll
 * 
 * NEVER ACCESS THESE METHODS DIRECTLY! ONLY THROUGH CLASS Symmetry
 */

public class SymmetryOperation extends M4d {
  String xyzOriginal;
  String xyzCanonical;
  String xyz;
  /**
   * "normalization" is the process of adjusting symmetry operator definitions
   * such that the center of geometry of a molecule is within the 555 unit cell
   * for each operation. It is carried out when "packed" is NOT issued and the
   * lattice is given as {i j k} or when the lattice is given as {nnn mmm 1}
   */
  private boolean doNormalize = true;
  boolean isFinalized;
  private int opId;
  private V3d centering;
  private Hashtable<String, Object> info;

  static P3d atomTest;
  


  final static int TYPE_UNKNOWN = -1;
  final static int TYPE_IDENTITY = 0;
  final static int TYPE_TRANSLATION = 1;
  final static int TYPE_ROTATION = 2;
  final static int TYPE_INVERSION = 4;
  final static int TYPE_REFLECTION = 8;  
  final static int TYPE_SCREW_ROTATION = TYPE_ROTATION | TYPE_TRANSLATION;
  final static int TYPE_ROTOINVERSION = TYPE_ROTATION | TYPE_INVERSION;
  final static int TYPE_GLIDE_REFLECTION = TYPE_REFLECTION | TYPE_TRANSLATION; 
  
  private int opType = TYPE_UNKNOWN;
  
  private int opOrder;
  private V3d opTrans;
  private P3d opPoint, opPoint2;
  private V3d opAxis;
  private P4d opPlane;
  private Boolean opIsCCW;

  boolean isIrrelevant;
  boolean isCoincident;
  
  final static int PLANE_MODE_POSITION_ONLY = 0;
  final static int PLANE_MODE_NOTRANS = 1;
  final static int PLANE_MODE_FULL = 2;
  

  String getOpName(int planeMode) {
    if (opType == TYPE_UNKNOWN)
      setOpTypeAndOrder();
    switch (opType) {
    case TYPE_IDENTITY:
      return "I";
    case TYPE_TRANSLATION:
      return "Trans" + op48(opTrans);
    case TYPE_ROTATION:
      return "Rot" + opOrder + op48(opPoint) + op48(opAxis);
    case TYPE_INVERSION:
      return "Inv" + op48(opPoint);
    case TYPE_REFLECTION:
      return (planeMode == PLANE_MODE_POSITION_ONLY ? "" : "Plane") + opPlane;
    case TYPE_SCREW_ROTATION:
      return "Screw" + opOrder + op48(opPoint) + op48(opAxis) + op48(opTrans) + opIsCCW;
    case TYPE_ROTOINVERSION:
      return "Nbar" + opOrder + op48(opPoint) + op48(opAxis);
    case TYPE_GLIDE_REFLECTION:
      return (planeMode == PLANE_MODE_POSITION_ONLY ? "" : "Glide") + opPlane + (planeMode == PLANE_MODE_FULL ? op48(opTrans) : "");
    }
    System.out.println("SymmetryOperation REJECTED TYPE FOR " + this);
    return "";
  }

  String getOpTitle() {
    if (opType == TYPE_UNKNOWN)
      setOpTypeAndOrder();
    switch (opType) {
    case TYPE_IDENTITY:
      return "identity ";
    case TYPE_TRANSLATION:
      return "translation " + opFrac(opTrans);
    case TYPE_ROTATION:
      return "rotation " + opOrder;
    case TYPE_INVERSION:
      return "inversion center " + opFrac(opPoint);
    case TYPE_REFLECTION:
      return "reflection ";
    case TYPE_SCREW_ROTATION:
      return "screw rotation " + opOrder + (opIsCCW == Boolean.TRUE ? "(+) " : "(-) ") + opFrac(opTrans);
    case TYPE_ROTOINVERSION:
      return opOrder + "-bar " + opFrac(opPoint);
    case TYPE_GLIDE_REFLECTION:
      return "glide reflection " + opFrac(opTrans);
    }
    return "";
  }
  
  private static String opFrac(T3d p) {
    return "{" + opF(p.x) + " " + opF(p.y)  + " " + opF(p.z) + "}";
  }



  private static String opF(double x) {
    boolean neg = (x < 0);
    if (neg) {
      x = -x;
    }
    int n = 0;
    if (x >= 1) {
      n = (int) x;
      x -= n;
    }
    int n48 = (int) Math.round(x * 48);
    int div;
    if (n48 % 48 == 0) {
      div = 1;
    } else if (n48 % 24 == 0) {
      div = 2;
    } else if (n48 % 16 == 0) {
      div = 3;
    } else if (n48 % 12 == 0) {
      div = 4;
    } else if (n48 % 8 == 0) {
      div = 6;
    } else if (n48 % 6 == 0) {
      div = 8;
    } else if (n48 % 4 == 0) {
      div = 12;
    } else if (n48 % 3 == 0) {
      div = 16;
    } else if (n48 % 2 == 0) {
      div = 24;
    } else {
      div = 48;
    }
    return (neg ? "-" : "") + (n*div + n48 * div / 48) + (div == 1 ? "" : "/" + div);
  }

  private static String op48(T3d p) {
    if (p == null) {
      System.err.println("SymmetryOperation.op48 null");
      return "(null)";
    }

    return "{" + Math.round(p.x*48) + " " + Math.round(p.y*48)  + " " + Math.round(p.z*48) + "}";
  }

  
  private String[] myLabels;
  int modDim;

  /**
   * A linear array for the matrix. Note that the last value in this array may
   * indicate 120 to indicate that the integer divisor should be 120, not 12.
   */
  double[] linearRotTrans;

  /**
   * rsvs is the superspace group rotation-translation matrix. It is a (3 +
   * modDim + 1) x (3 + modDim + 1) matrix from which we can extract all
   * necessary parts; so 4x4 = 16, 5x5 = 25, 6x6 = 36, 7x7 = 49
   * 
   * <code>
     [ [(3+modDim)*x + 1]   
     [(3+modDim)*x + 1]     [ Gamma_R   [0x0]   | Gamma_S
     [(3+modDim)*x + 1]  ==    [0x0]    Gamma_e | Gamma_d 
     ...                       [0]       [0]    |   1     ]
     [0 0 0 0 0...   1] ]
     </code>
   */
  Matrix rsvs;

  boolean isBio;
  Matrix sigma;
  int number;
  String subsystemCode;
  int timeReversal;

  private boolean unCentered;
  boolean isCenteringOp;
  private int magOp = Integer.MAX_VALUE;
  int divisor = 12; // could be 120 for magnetic;
  private P3d opX;
  private String opAxisCode;

  void setSigma(String subsystemCode, Matrix sigma) {
    this.subsystemCode = subsystemCode;
    this.sigma = sigma;
  }

  /**
   * 
   * @param op operation to clone or null
   * @param id opId for this operation; ignored if cloning
   * @param doNormalize 
   */
  SymmetryOperation(SymmetryOperation op, int id, boolean doNormalize) {
    this.doNormalize = doNormalize;
    if (op == null) {
      opId = id;
      return;
    }
    /*
     * externalizes and transforms an operation for use in atom reader
     * 
     */
    xyzOriginal = op.xyzOriginal;
    xyz = op.xyz;
    divisor = op.divisor;
    opId = op.opId;
    modDim = op.modDim;
    myLabels = op.myLabels;
    number = op.number;
    linearRotTrans = op.linearRotTrans;
    sigma = op.sigma;
    subsystemCode = op.subsystemCode;
    timeReversal = op.timeReversal;
    setMatrix(false);
    if (!op.isFinalized)
      doFinalize();
  }

  private void setGamma(boolean isReverse) {
    // standard M4d (this)
    //
    //  [ [rot]   | [trans] 
    //     [0]    |   1     ]
    //
    // becomes for a superspace group
    //
    //  rows\cols    (3)    (modDim)    (1)
    // (3)        [ Gamma_R   [0x0]   | Gamma_S
    // (modDim)       m*      Gamma_e | Gamma_d 
    // (1)           [0]       [0]    |   1     ]

    int n = 3 + modDim;
    double[][] a = (rsvs = new Matrix(null, n + 1, n + 1)).getArray();
    double[] t = new double[n];
    int pt = 0;
    // first retrieve all n x n values from linearRotTrans
    // and get the translation as well
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++)
        a[i][j] = linearRotTrans[pt++];
      t[i] = (isReverse ? -1 : 1) * linearRotTrans[pt++];
    }
    a[n][n] = 1;
    if (isReverse)
      rsvs = rsvs.inverse();
    // t is already reversed; set it now.
    for (int i = 0; i < n; i++)
      a[i][n] = t[i];
    // then set this operation matrix as {R|t}
    a = rsvs.getSubmatrix(0, 0, 3, 3).getArray();
    for (int i = 0; i < 3; i++)
      for (int j = 0; j < 4; j++)
        setElement(i, j, (j < 3 ? a[i][j] : t[i]));
    setElement(3, 3, 1);
  }

  void doFinalize() {
    div12(this, divisor);
    if (modDim > 0) {
      double[][] a = rsvs.getArray();
      for (int i = a.length - 1; --i >= 0;)
        a[i][3 + modDim] = finalizeD(a[i][3 + modDim],  divisor);
    }
    isFinalized = true;
  }

  private static M4d div12(M4d op, int divisor) {
    op.m03 = finalizeD(op.m03, divisor);
    op.m13 = finalizeD(op.m13, divisor);
    op.m23 = finalizeD(op.m23, divisor);
    return op;
  }

  private static double finalizeD(double m, int divisor) {
    if (divisor == 0) {
      if (m == 0)
        return 0;
      int n = (int) m;
      return ((n >> DIVISOR_OFFSET) * 1F / (n & DIVISOR_MASK));
    } 
    return m / divisor;
  }

  String getXyz(boolean normalized) {
    return (normalized && modDim == 0 || xyzOriginal == null ? xyz
        : xyzOriginal);
  }

  public String getxyzTrans(T3d t) {
    M4d m = newM4(this);
    m.add(t);
    return getXYZFromMatrix(m, false, false, false);
  }

  String dumpInfo() {
    return "\n" + xyz + "\ninternal matrix representation:\n" + toString();
  }

  final static String dumpSeitz(M4d s, boolean isCanonical) {
    SB sb = new SB();
    double[] r = new double[4];
    for (int i = 0; i < 3; i++) {
      s.getRow(i, r);
      sb.append("[\t");
      for (int j = 0; j < 3; j++)
        sb.appendI((int) r[j]).append("\t");
      double trans =  r[3];
      if (trans != (int) trans)
        trans = 12 * trans;
      sb.append(twelfthsOf(isCanonical ? normalizeTwelfths(trans / 12, 12, true) : (int) trans)).append("\t]\n");
    }
    return sb.toString();
  }

  boolean setMatrixFromXYZ(String xyz, int modDim, boolean allowScaling) {
    /*
     * sets symmetry based on an operator string "x,-y,z+1/2", for example
     * 
     */
    if (xyz == null)
      return false;
    xyzOriginal = xyz;
    divisor = setDivisor(xyz);
    xyz = xyz.toLowerCase();
    setModDim(modDim);
    boolean isReverse = false;
    boolean halfOrLess = true;
    if (xyz.startsWith("!")) {
      if (xyz.startsWith("!nohalf!")) {
        halfOrLess = false;
        xyz = xyz.substring(8);
        xyzOriginal = xyz;
      } else {
        isReverse = false;
        xyz = xyz.substring(1);
      }
    }
    if (xyz.indexOf("xyz matrix:") == 0) {
      /* note: these terms must in unit cell fractional coordinates!
       * CASTEP CML matrix is in fractional coordinates, but do not take into account
       * hexagonal systems. Thus, in wurtzite.cml, for P 6c 2'c:
       *
       * "transform3": 
       * 
       * -5.000000000000e-1  8.660254037844e-1  0.000000000000e0   0.000000000000e0 
       * -8.660254037844e-1 -5.000000000000e-1  0.000000000000e0   0.000000000000e0 
       *  0.000000000000e0   0.000000000000e0   1.000000000000e0   0.000000000000e0 
       *  0.000000000000e0   0.000000000000e0   0.000000000000e0   1.000000000000e0
       *
       * These are transformations of the STANDARD xyz axes, not the unit cell. 
       * But, then, what coordinate would you feed this? Fractional coordinates of what?
       * The real transform is something like x-y,x,z here.
       * 
       */
      this.xyz = xyz;
      Parser.parseStringInfestedDoubleArray(xyz, null, linearRotTrans);
      return setFromMatrix(null, isReverse);
    }
    if (xyz.indexOf("[[") == 0) {
      xyz = xyz.replace('[', ' ').replace(']', ' ').replace(',', ' ');
      Parser.parseStringInfestedDoubleArray(xyz, null, linearRotTrans);
      for (int i = linearRotTrans.length; --i >= 0;)
        if (Double.isNaN(linearRotTrans[i]))
          return false;
      setMatrix(isReverse);
      isFinalized = true;
      isBio = (xyz.indexOf("bio") >= 0);
      this.xyz = (isBio ? (this.xyzOriginal = super.toString())
          : getXYZFromMatrix(this, false, false, false));
      return true;
    }
    if (modDim == 0 && xyz.indexOf("x4") >= 0) {
      for (int i = 14; --i >= 4;) {
        if (xyz.indexOf("x" + i) >= 0) {
          setModDim(i - 3);
          break;
        }
      }
    }
    String mxyz = null;
    // we use ",m" and ",-m" as internal notation for time reversal.
    if (xyz.endsWith("m")) {
      timeReversal = (xyz.indexOf("-m") >= 0 ? -1 : 1);
      allowScaling = true;
    } else if (xyz.indexOf("mz)") >= 0) {
      // alternatively, we accept notation indicating explicit spin transformation "(mx,my,mz)"
      int pt = xyz.indexOf("(");
      mxyz = xyz.substring(pt + 1, xyz.length() - 1);
      xyz = xyz.substring(0, pt);
      allowScaling = false;
    }
    String strOut = getMatrixFromString(this, xyz, linearRotTrans,
        allowScaling, halfOrLess);
    if (strOut == null)
      return false;
    xyzCanonical = strOut;
    if (mxyz != null) {
      // base time reversal on relationship between x and mx in relation to determinant
      boolean isProper = (M4d.newA16(linearRotTrans).determinant3() == 1);
      timeReversal = (((xyz.indexOf("-x") < 0) == (mxyz
          .indexOf("-mx") < 0)) == isProper ? 1 : -1);
    }
    setMatrix(isReverse);
    this.xyz = (isReverse ? getXYZFromMatrix(this, true, false, false)
        : doNormalize ? strOut : xyz);
    if (timeReversal != 0)
      this.xyz += (timeReversal == 1 ? ",m" : ",-m");
    if (Logger.debugging)
      Logger.debug("" + this);
    return true;
  }

  /**
   * Sets the divisor to 0 for n/9 or n/mm
   * @param xyz
   * @return 0 or 12
   */
  private static int setDivisor(String xyz) {
    int pt = xyz.indexOf('/');
    int len = xyz.length();
    while (pt > 0 && pt < len - 1) {
      char c = xyz.charAt(pt + 1);
      if ("2346".indexOf(c) < 0 || pt < len - 2 && Character.isDigit(xyz.charAt(pt + 2))) {
        // any n/m where m is not 2,3,4,6
        // any n/nn
        return 0;
      }
      pt = xyz.indexOf('/', pt + 1);
    }
    return 12;
  }

  private void setModDim(int dim) {
    int n = (dim + 4) * (dim + 4);
    modDim = dim;
    if (dim > 0)
      myLabels = labelsXn;
    linearRotTrans = new double[n];
  }

  private void setMatrix(boolean isReverse) {
    if (linearRotTrans.length > 16) {
      setGamma(isReverse);
    } else {
      setA(linearRotTrans);
      if (isReverse) {
        P3d p3 = P3d.new3(m03, m13, m23);
        invert();
        rotate(p3);
        p3.scale(-1);
        setTranslation(p3);
      }
    }
  }

  boolean setFromMatrix(double[] offset, boolean isReverse) {
    double v = 0;
    int pt = 0;
    myLabels = (modDim == 0 ? labelsXYZ : labelsXn);
    int rowPt = 0;
    int n = 3 + modDim;
    for (int i = 0; rowPt < n; i++) {
      if (Double.isNaN(linearRotTrans[i]))
        return false;
      v = linearRotTrans[i];
      
      if (Math.abs(v) < 0.00001f)
        v = 0;
      boolean isTrans = ((i + 1) % (n + 1) == 0);
      if (isTrans) {
        int denom =  (divisor == 0 ? ((int) v) & DIVISOR_MASK : divisor);
        if (denom == 0)
          denom = 12;
        v =  finalizeD(v, divisor);
        // offset == null only in the case of "xyz matrix:" option
        if (offset != null) {
          // magnetic centering only
          if (pt < offset.length)
            v += offset[pt++];
        }
        v = normalizeTwelfths(((v < 0 ? -1 : 1) * Math.abs(v * denom) / denom),
            denom, doNormalize);
        if (divisor == 0)
          v = toDivisor(v, denom);
        rowPt++;
      }
      linearRotTrans[i] = v;
    }
    linearRotTrans[linearRotTrans.length - 1] = this.divisor;
    setMatrix(isReverse);
    isFinalized = (offset == null);
    xyz = getXYZFromMatrix(this, true, false, false);
    return true;
  }

  public static M4d getMatrixFromXYZ(String xyz, boolean halfOrLess) {
    double[] linearRotTrans = new double[16];
    xyz = getMatrixFromString(null, xyz, linearRotTrans, false, halfOrLess);
    if (xyz == null)
      return null;
    M4d m = new M4d();
    m.setA(linearRotTrans);
    return div12(m, setDivisor(xyz));
  }

  static String getJmolCanonicalXYZ(String xyz) {
    try {
      return getMatrixFromString(null, xyz, null, false, true);
    } catch (Exception e) {
      return null;
    }
  }
  /**
   * Convert the Jones-Faithful notation "x, -z+1/2, y" or "x1, x3-1/2, x2,
   * x5+1/2, -x6+1/2, x7..." to a linear array
   * 
   * Also allows a-b,-5a-5b,-c;0,0,0 format
   * 
   * @param op
   * @param xyz
   * @param linearRotTrans
   * @param allowScaling
   * @param halfOrLess 
   * @return canonized Jones-Faithful string
   */
  static String getMatrixFromString(SymmetryOperation op, String xyz,
                                    double[] linearRotTrans,
                                    boolean allowScaling, boolean halfOrLess) {    
    boolean isDenominator = false;
    boolean isDecimal = false;
    boolean isNegative = false;
    xyz = PT.rep(xyz,  "[bio[", "");
    int modDim = (op == null ? 0 : op.modDim);
    int nRows = 4 + modDim;
    int divisor = (op == null ? setDivisor(xyz) : op.divisor);
    boolean doNormalize = halfOrLess && (op == null ? !xyz.startsWith("!") : op.doNormalize);
    int dimOffset = (modDim > 0 ? 3 : 0); // allow a b c to represent x y z
    if (linearRotTrans != null)
      linearRotTrans[linearRotTrans.length - 1] = 1;
    // may be a-b,-5a-5b,-c;0,0,0 form
    int transPt = xyz.indexOf(';') + 1;
    if (transPt != 0) {
      allowScaling = true;
      if (transPt == xyz.length())
        xyz += "0,0,0";
    }
    int rotPt = -1;
    String[] myLabels = (op == null || modDim == 0 ? null : op.myLabels);
    if (myLabels == null)
      myLabels = labelsXYZ;
    xyz = xyz.toLowerCase() + ",";
    xyz = xyz.replace('(', ',');
    //        load =magndata/1.23
    //        draw symop "-x,-y,-z(mx,my,mz)"
    if (modDim > 0)
      xyz = replaceXn(xyz, modDim + 3);
    int xpt = 0;
    int tpt0 = 0;
    int rowPt = 0;
    char ch;
    double iValue = 0;
    int denom = 0;
    int numer = 0;
    double decimalMultiplier = 1d;
    String strT = "";
    String strOut = "";
    int[] ret = new int[1];
    int len = xyz.length();
    for (int i = 0; i < len; i++) {
      switch (ch = xyz.charAt(i)) {
      case ';':
        break;
      case '\'':
      case ' ':
      case '{':
      case '}':
      case '!':
        continue;
      case '-':
        isNegative = true;
        continue;
      case '+':
        isNegative = false;
        continue;
      case '/':
        denom = 0;
        isDenominator = true;
        continue;
      case 'x':
      case 'y':
      case 'z':
      case 'a':
      case 'b':
      case 'c':
      case 'd':
      case 'e':
      case 'f':
      case 'g':
      case 'h':
        tpt0 = rowPt * nRows;
        int ipt = (ch >= 'x' ? ch - 'x' : ch - 'a' + dimOffset);
        xpt = tpt0 + ipt;
        int val = (isNegative ? -1 : 1);
        if (allowScaling && iValue != 0) {
          if (linearRotTrans != null)
            linearRotTrans[xpt] = iValue;
          val = (int) iValue;
          iValue = 0;
        } else if (linearRotTrans != null) {
            linearRotTrans[xpt] = val;
        }
        strT += plusMinus(strT, val, myLabels[ipt], false);
        break;
      case ',':
        if (transPt != 0) {
          if (transPt > 0) {
            // now read translation
            rotPt = i;
            i = transPt - 1;
            transPt = -i;
            iValue = 0;
            denom = 0;
            continue;
          }
          transPt = i + 1;
          i = rotPt;
        }
        // add translation in 12ths
        iValue = normalizeTwelfths(iValue, denom == 0 ? 12 : divisor == 0 ? denom : divisor, doNormalize);
        if (linearRotTrans != null)
          linearRotTrans[tpt0 + nRows - 1] = (divisor == 0 && denom > 0 ? iValue = toDivisor(numer, denom) : iValue);
        strT += xyzFraction12(iValue, (divisor == 0 ? denom : divisor), false, halfOrLess);
        // strT += xyzFraction48(iValue, false, true);
        strOut += (strOut == "" ? "" : ",") + strT;
        if (rowPt == nRows - 2)
          return strOut;
        iValue = 0;
        numer = 0;
        denom = 0;
        strT = "";
        if (rowPt++ > 2 && modDim == 0) {
          Logger.warn("Symmetry Operation? " + xyz);
          return null;
        }
        break;
      case '.':
        isDecimal = true;
        decimalMultiplier = 1d;
        continue;
      case '0':
        if (!isDecimal && divisor == 12 && (isDenominator || !allowScaling))
          continue;
        //$FALL-THROUGH$
      default:
        //Logger.debug(isDecimal + " " + ch + " " + iValue);
        int ich = ch - '0';
        if (ich >= 0 && ich <= 9) {
          if (isDecimal) {
            decimalMultiplier /= 10d;
            if (iValue < 0)
              isNegative = true;
            iValue += decimalMultiplier * ich * (isNegative ? -1 : 1);
            continue;
          }
          if (isDenominator) {
            ret[0] = i;
            denom = PT.parseIntNext(xyz, ret);
            if (denom < 0)
              return null;
            i = ret[0] - 1;
            if (iValue == 0) {
              // a/2,....
              if (linearRotTrans != null)
                linearRotTrans[xpt] /= denom;
            } else {
              numer = (int) iValue;
              iValue /= denom;
            }
          } else {
            iValue = iValue * 10 + (isNegative ? -1 : 1) * ich;
            isNegative = false;
          }
        } else {
          Logger.warn("symmetry character?" + ch);
        }
      }
      isDecimal = isDenominator = isNegative = false;
    }
    return null;
  }

  static String replaceXn(String xyz, int n) {
    for (int i = n; --i >= 0;)
      xyz = PT.rep(xyz, labelsXn[i], labelsXnSub[i]);
    return xyz;
  }

  private final static int DIVISOR_MASK = 0xFF;
  private final static int DIVISOR_OFFSET = 8;
  
  private final static int toDivisor(double numer, int denom) {
    int n = (int) numer;
    if (n != numer) {
      // could happen with magnetic lattice centering 1/5 + 1/2 = 7/10
      double f = numer - n;
      denom = (int) Math.abs(denom/f);
      n = (int) (Math.abs(numer) / f);
    }
    return ((n << DIVISOR_OFFSET) + denom);
  }

  private final static String xyzFraction12(double n12ths, int denom, boolean allPositive,
                                            boolean halfOrLess) {
    if (n12ths == 0)
      return "";
    double n = n12ths;
    if (denom != 12) {
      int in = (int) n;
      denom = (in & DIVISOR_MASK);
      n = in >> DIVISOR_OFFSET;
    }
    int half = (denom / 2);
    if (allPositive) {
      while (n < 0)
        n += denom;
    } else if (halfOrLess) {
      while (n > half)
        n -= denom;
      while (n < -half)
        n += denom;
    }
    String s = (denom == 12 ? twelfthsOf(n) : n == 0 ? "0" : n + "/" + denom);
    return (s.charAt(0) == '0' ? "" : n > 0 ? "+" + s : s);
  }

  //  private final static String xyzFraction48ths(double n48ths, boolean allPositive, boolean halfOrLess) {
  //    double n = n48ths;
  //    if (allPositive) {
  //      while (n < 0)
  //        n += 48d;
  //    } else if (halfOrLess) {
  //      while (n > 24d)
  //        n -= 48d;
  //      while (n < -24d)
  //        n += 48d;
  //    }
  //    String s = fortyEighthsOf(n);
  //    return (s.charAt(0) == '0' ? "" : n > 0 ? "+" + s : s);
  //  }

  final static String twelfthsOf(double n12ths) {
    String str = "";
    if (n12ths < 0) {
      n12ths = -n12ths;
      str = "-";
    }
    int m = 12;
    int n = (int) Math.round(n12ths);
    if (Math.abs(n - n12ths) > 0.01f) {
      // fifths? sevenths? eigths? ninths? sixteenths?
      // Juan Manuel suggests 10 is large enough here 
      double f = n12ths / 12;
      int max = 20;
      for (m = 3; m < max; m++) {
        double fm = f * m;
        n = (int) Math.round(fm);
        if (Math.abs(n - fm) < 0.01f)
          break;
      }
      if (m == max)
        return str + f;
    } else {
      if (n == 12)
        return str + "1";
      if (n < 12)
        return str + twelfths[n % 12];
      switch (n % 12) {
      case 0:
        return str + n / 12;
      case 2:
      case 10:
        m = 6;
        break;
      case 3:
      case 9:
        m = 4;
        break;
      case 4:
      case 8:
        m = 3;
        break;
      case 6:
        m = 2;
        break;
      default:
        break;
      }
      n = (n * m / 12);
    }
    return str + n + "/" + m;
  }

  //  final static String fortyEighthsOf(double n48ths) {
  //    String str = "";
  //    if (n48ths < 0) {
  //      n48ths = -n48ths;
  //      str = "-";
  //    }
  //    int m = 12;
  //    int n = (int) Math.round(n48ths);
  //    if (Math.abs(n - n48ths) > 0.01f) {
  //      // fifths? sevenths? eigths? ninths? sixteenths?
  //      // Juan Manuel suggests 10 is large enough here 
  //      double f = n48ths / 48;
  //      int max = 20;
  //      for (m = 5; m < max; m++) {
  //        double fm = f * m;
  //        n = (int) Math.round(fm);
  //        if (Math.abs(n - fm) < 0.01f)
  //          break;
  //      }
  //      if (m == max)
  //        return str + f;
  //    } else {
  //      if (n == 48)
  //        return str + "1";
  //      if (n < 48)
  //        return str + twelfths[n % 48];
  //      switch (n % 48) {
  //      case 0:
  //        return "" + n / 48;
  //      case 2:
  //      case 10:
  //        m = 6;
  //        break;
  //      case 3:
  //      case 9:
  //        m = 4;
  //        break;
  //      case 4:
  //      case 8:
  //        m = 3;
  //        break;
  //      case 6:
  //        m = 2;
  //        break;
  //      default:
  //        break;
  //      }
  //      n = (n * m / 12);
  //    }
  //    return str + n + "/" + m;
  //  }

  private final static String[] twelfths = { "0", "1/12", "1/6", "1/4", "1/3",
      "5/12", "1/2", "7/12", "2/3", "3/4", "5/6", "11/12" };

  //  private final static String[] fortyeigths = { "0", 
  //    "1/48", "1/24", "1/16", "1/12",
  //    "5/48", "1/8", "7/48", "1/6", 
  //    "3/16", "5/24", "11/48", "1/4",
  //    "13/48", "7/24", "5/16", "1/3",
  //    "17/48", "3/8", "19/48", "5/12",
  //    "7/16", "11/24", "23/48", "1/2",
  //    "25/48", "13/24", "9/16", "7/12",
  //    "29/48", "15/24", "31/48", "2/3",
  //    "11/12", "17/16", "35/48", "3/4",
  //    "37/48", "19/24", "13/16", "5/6",
  //    "41/48", "7/8", "43/48", "11/12",
  //    "15/16", "23/24", "47/48"
  //  };
  //
  private static String plusMinus(String strT, double x, String sx, boolean allowFractions) {
    double a;
    return (x == 0 ? ""
        : (x < 0 ? "-" : strT.length() == 0 ? "" : "+")
            + (x == 1 || x == -1 ? "" : (a = Math.abs(x)) < 1 && allowFractions ? twelfthsOf(a * 12) : "" + (int) a)) + sx;
  }

  private static double normalizeTwelfths(double iValue, int divisor,
                                         boolean doNormalize) {
    iValue *= divisor;
    int half = divisor / 2;
    if (doNormalize) {
      while (iValue > half)
        iValue -= divisor;
      while (iValue <= -half)
        iValue += divisor;
    }
    return iValue;
  }

  //  private static double normalize48ths(double iValue, boolean doNormalize) {
  //    iValue *= 48d;
  //    if (doNormalize) {
  //      while (iValue > 24)
  //        iValue -= 48;
  //      while (iValue <= -24)
  //        iValue += 48;
  //    }
  //    return iValue;
  //  }
  //
  final static String[] labelsXYZ = new String[] { "x", "y", "z" };
  final static String[] labelsXn = new String[] { "x1", "x2", "x3", "x4", "x5",
      "x6", "x7", "x8", "x9", "x10", "x11", "x12", "x13" };
  final static String[] labelsXnSub = new String[] { "x", "y", "z", "a", "b",
      "c", "d", "e", "f", "g", "h", "i", "j" };

  final public static String getXYZFromMatrix(M4d mat, boolean is12ths,
                                              boolean allPositive,
                                              boolean halfOrLess) {
    return getXYZFromMatrixFrac(mat, is12ths, allPositive, halfOrLess, false);
  }
  
  final public static String getXYZFromMatrixFrac(M4d mat, boolean is12ths,
                                              boolean allPositive,
                                              boolean halfOrLess, boolean allowFractions) {
    String str = "";
    SymmetryOperation op = (mat instanceof SymmetryOperation
        ? (SymmetryOperation) mat
        : null);
    if (op != null && op.modDim > 0)
      return getXYZFromRsVs(op.rsvs.getRotation(), op.rsvs.getTranslation(),
          is12ths);
    double[] row = new double[4];
    int denom = (int) mat.getElement(3, 3);
    if (denom == 1)
      denom = 12;
    else
      mat.setElement(3, 3, 1);
    for (int i = 0; i < 3; i++) {
      int lpt = (i < 3 ? 0 : 3);
      mat.getRow(i, row);
      String term = "";
      for (int j = 0; j < 3; j++) {
        double x = row[j];
        if (approx(x) != 0) {
          term += plusMinus(term, x, labelsXYZ[j + lpt], allowFractions);
        }
      }
      if ((is12ths ? row[3] : approx(row[3])) != 0)
        term += xyzFraction12((is12ths ? row[3] : row[3] * denom), denom,
            allPositive, halfOrLess);
      str += "," + term;
    }
    return str.substring(1);
  }

  V3d[] rotateAxes(V3d[] vectors, UnitCell unitcell, P3d ptTemp, M3d mTemp) {
    V3d[] vRot = new V3d[3];
    getRotationScale(mTemp);
    for (int i = vectors.length; --i >= 0;) {
      ptTemp.setT(vectors[i]);
      unitcell.toFractional(ptTemp, true);
      mTemp.rotate(ptTemp);
      unitcell.toCartesian(ptTemp, true);
      vRot[i] = V3d.newV(ptTemp);
    }
    return vRot;
  }

  public String fcoord2(T3d p) {
    if (divisor == 12)
      return fcoord(p);
    return fc2((int) linearRotTrans[3]) + " " + fc2((int) linearRotTrans[7]) + " " + fc2((int) linearRotTrans[11]);
  }

  /**
   * Get string version of fraction when divisor == 0 
   * 
   * @param num
   * @return "1/2" for example
   */
  private String fc2(int num) {
      int denom = (num & DIVISOR_MASK);
      num = num >> DIVISOR_OFFSET;
    return (num == 0 ? "0" : num + "/" + denom);
  }

  /**
   * Get string version of fraction 
   * 
   * @param p
   * @return "1/2" for example
   */
  static String fcoord(T3d p) {
    // Castep reader only
    return fc(p.x) + " " + fc(p.y) + " " + fc(p.z);
  }

  private static String fc(double x) {
    // Castep reader only
    double xabs = Math.abs(x);
    String m = (x < 0 ? "-" : "");
    int x24 = (int) approx(xabs * 24);
    if (x24 / 24d == (int) (x24 / 24d))
      return m + (x24 / 24);
    if (x24 % 8 != 0) {
      return m + twelfthsOf(x24 >> 1);
    }
    return (x24 == 0 ? "0" : x24 == 24 ? m + "1" : m + (x24 / 8) + "/3");
  }
  
  static double approx(double f) {
    return PT.approxD(f, 100);
  }

  static double approx6(double f) {
    return PT.approxD(f, 1000000);
  }

  static String getXYZFromRsVs(Matrix rs, Matrix vs, boolean is12ths) {
    double[][] ra = rs.getArray();
    double[][] va = vs.getArray();
    int d = ra.length;
    String s = "";
    for (int i = 0; i < d; i++) {
      s += ",";
      for (int j = 0; j < d; j++) {
        double r = ra[i][j];
        if (r != 0) {
          s += (r < 0 ? "-" : s.endsWith(",") ? "" : "+")
              + (Math.abs(r) == 1 ? "" : "" + (int) Math.abs(r)) + "x"
              + (j + 1);
        }
      }
      s += xyzFraction12((int) (va[i][0] * (is12ths ? 1 : 12)), 12, false, true);
    }
    return PT.rep(s.substring(1), ",+", ",");
  }

  @Override
  public String toString() {
    return (rsvs == null ? super.toString()
        : super.toString() + " " + rsvs.toString());
  }

  /**
   * Magnetic spin is a pseudo (or "axial") vector. This means that it acts as a
   * rotation, not a vector. When a rotation about x is passed through the
   * mirror plane xz, it is reversed; when it is passed through the mirror plane
   * yz, it is not reversed -- exactly opposite what you would imagine from a
   * standard "polar" vector.
   * 
   * For example, a vector perpendicular to a plane of symmetry (det=-1) will be
   * flipped (m=1), while a vector parallel to that plane will not be flipped
   * (m=-1)
   * 
   * In addition, magnetic spin operations have a flag m=1 or m=-1 (m or -m)
   * that indicates how the vector quantity changes with symmetry. This is
   * called "time reversal" and stored here as timeReversal.
   * 
   * To apply, timeReversal must be multiplied by the 3x3 determinant, which is
   * always 1 (standard rotation) or -1 (rotation-inversion). This we store as
   * magOp. See https://en.wikipedia.org/wiki/Pseudovector
   * 
   * @return +1, -1, or 0
   */
  int getMagneticOp() {
    return (magOp == Integer.MAX_VALUE ? magOp = (int) (determinant3() * timeReversal)
        : magOp);
  }

  /**
   * set the time reversal, and indicate internally in xyz as appended ",m" or
   * ",-m"
   * 
   * @param magRev
   */
  void setTimeReversal(int magRev) {
    timeReversal = magRev;
    if (xyz.indexOf("m") >= 0)
      xyz = xyz.substring(0, xyz.indexOf("m"));
    if (magRev != 0) {
      xyz += (magRev == 1 ? ",m" : ",-m");
    }
  }

  /**
   * assumption here is that these are in order of sets, as in ITA
   * 
   * @return centering
   */
  V3d getCentering() {
    if (!isFinalized)
      doFinalize();
    if (centering == null && !unCentered) {
      if (modDim == 0 && m00 == 1 && m11 == 1 && m22 == 1 && m01 == 0
          && m02 == 0 && m10 == 0 && m12 == 0 && m20 == 0 && m21 == 0
          && (m03 != 0 || m13 != 0 || m23 != 0)) {
        isCenteringOp = true;
        centering = V3d.new3( m03, m13, m23);
      } else {
        unCentered = true;
        centering = null;
      }
    }
    return centering;
  }

  String fixMagneticXYZ(M4d m, String xyz, boolean addMag) {
    if (timeReversal == 0)
      return xyz;
    int pt = xyz.indexOf("m");
    pt -= (3 - timeReversal) / 2;
    xyz = (pt < 0 ? xyz : xyz.substring(0, pt));
    if (!addMag)
      return xyz + (timeReversal > 0 ? " +1" : " -1");
    M4d m2 = M4d.newM4(m);
    m2.m03 = m2.m13 = m2.m23 = 0;
    if (getMagneticOp() < 0)
      m2.scale(-1); // does not matter that we flip m33 - it is never checked
    xyz += "(" + PT.rep(PT
        .rep(PT.rep(getXYZFromMatrix(m2, false, false, false),
            "x", "mx"), "y", "my"),
        "z", "mz") + ")";
    return xyz;
  }

  public Map<String, Object> getInfo() {
    if (info == null) {
      info = new Hashtable<String, Object>();
      info.put("xyz", xyz);
      if (centering != null)
        info.put("centering", centering);
      info.put("index", Integer.valueOf(number - 1));
      info.put("isCenteringOp", Boolean.valueOf(isCenteringOp));
      if (linearRotTrans != null)
        info.put("linearRotTrans", linearRotTrans);
      info.put("modulationDimension", Integer.valueOf(modDim));
      info.put("matrix", M4d.newM4(this));
      if (magOp != Double.MAX_VALUE)
        info.put("magOp", Double.valueOf(magOp));
      info.put("id", Integer.valueOf(opId));
      info.put("timeReversal", Integer.valueOf(timeReversal));
      if (xyzOriginal != null)
        info.put("xyzOriginal", xyzOriginal);
    }
    return info;
  }

  /**
   * Adjust the translation for this operator so that it moves the center of
   * mass of the full set of atoms into the cell.
   * 
   * @param dim
   * @param m
   * @param atoms
   * @param atomIndex first index
   * @param count number of atoms
   */
  public static void normalizeOperationToCentroid(int dim, M4d m, P3d[] atoms, int atomIndex, int count) {
    if (count <= 0)
      return;
    double x = 0;
    double y = 0;
    double z = 0;
    if (atomTest == null)
      atomTest = new P3d();
    for (int i = atomIndex, i2 = i + count; i < i2; i++) {
      Symmetry.newPoint(m, atoms[i], 0, 0, 0, atomTest);
      x += atomTest.x;
      y += atomTest.y;
      z += atomTest.z;
    }
    x /= count;
    y /= count;
    z /= count;
    while (x < -0.001 || x >= 1.001) {
      m.m03 += (x < 0 ? 1 : -1);
      x += (x < 0 ? 1 : -1);
    }
    if (dim > 1)
    while (y < -0.001 || y >= 1.001) {
      m.m13 += (y < 0 ? 1 : -1);
      y += (y < 0 ? 1 : -1);
    }
    if (dim > 2)
    while (z < -0.001 || z >= 1.001) {
      m.m23 += (z < 0 ? 1 : -1);
      z += (z < 0 ? 1 : -1);
    }
  }

  public static Lst<P3d> getLatticeCentering(SymmetryOperation[] ops) {
    // TODO -- check 'R' types
    Lst<P3d> list = new Lst<P3d>();
    for (int i = 0; i < ops.length; i++) {
      T3d c = (ops[i]  == null ? null : ops[i].getCentering());
      if (c != null)
        list.addLast(P3d.newP(c));
    }
    return list;
  }

  public Boolean getOpIsCCW() {
    if (opType == TYPE_UNKNOWN) {
      setOpTypeAndOrder();
    }
    return opIsCCW;
  }

  public int getOpType() {
    if (opType == TYPE_UNKNOWN) {
      setOpTypeAndOrder();
    }
    return opType;
  }

  public int getOpOrder() {
    if (opType == TYPE_UNKNOWN) {
      setOpTypeAndOrder();
    }
    return opOrder;
  }
  
  public P3d getOpPoint() {
    if (opType == TYPE_UNKNOWN) {
      setOpTypeAndOrder();
    }
    return opPoint;
  }

  public V3d getOpAxis() {
    if (opType == TYPE_UNKNOWN) {
      setOpTypeAndOrder();
    }
    return opAxis;
  }
  public P3d getOpPoint2() {
   return opPoint2;
  }

  public V3d getOpTrans() {
    if (opType == TYPE_UNKNOWN) {
      setOpTypeAndOrder();
    }
    return opTrans;
  }

  private final static P3d x = P3d.new3(Math.PI, Math.E, Math.PI * Math.E);
  
  /**
   * The problem is that the 3-fold axes for cubic groups
   * have (seemingly arbitrary assignments for axes
   */
  private final static int[] C3codes = { //
      0x031112, // 3+(-1 1-1)
      0x121301, // 3-(-1 1-1)
      0x130112, // 3+(-1-1 1)
      0x021311, // 3-(-1-1 1)
      0x130102, // -3+(-1 1-1)
      0x020311, // -3-(-1 1-1)
      0x031102, // -3+(-1-1 1)
      0x120301, // -3-( 1-1-1)
  };
  
  private static int opGet3code(M4d m) {
    int c = 0;
    double[] row = new double[4];
    for (int r = 0; r < 3; r++) {
      m.getRow(r,  row);
      for (int i = 0; i < 3; i++) {
        switch ((int) row[i]) {
        case 1:
          c |= (i+1) << ((2-r)<<3);
          break;
        case -1:
          c |= (0x10 + i+1) << ((2-r)<<3);
          break;
        }
      }
    }
    //System.out.println(Integer.toHexString(c) + "\n" + m);
    return c;
  }
  
  private static P3d xpos = P3d.new3(1, 0, 0);
  private static P3d xneg = P3d.new3(-1, 0, 0);
  private static P3d opGet3x(M4d m) {
    if (m.m22 != 0) // z-axis
      return x;
    int c = opGet3code(m);
    for (int i = 0; i < 8; i++)
      if (c == C3codes[i])
        return xneg;
    return xpos;
  }

  private void setOpTypeAndOrder() {
    // From International Tables for Crystallography, Volume A, Chapter 1.2, by H. Wondratschek and M. I. Aroyo
    int det = (int) Math.round(determinant3());
    int trace = (int) Math.round(m00 + m11 + m22);
    int code1 = ((trace + 3) << 1) + ((det + 1) >> 1);
    int order = 0;
    // handle special cases of identity and inversion
    int angle = 0;
    P3d px = SymmetryOperation.x;
    switch (trace) {
    case 3:
      if (hasTrans(this)) {
        opType = TYPE_TRANSLATION;
        opTrans = new V3d();
        getTranslation(opTrans);
        opOrder = 2;
      } else {
        opType = TYPE_IDENTITY;
        opOrder = 1;
      }
      return;
    case -3:
      opType = TYPE_INVERSION;
      order = 2;
      break;
    default:
      // not identity or inversion
      order = trace * det + 3; // will be 2, 3, 4, or 5
      if (order == 5)
        order = 6;
      if (det > 0) {
        opType = TYPE_ROTATION;
        angle = (int) (Math.acos((trace - 1) / 2d) * 180 / Math.PI);
        if (angle == 120) {
          if (opX == null)
            opX = opGet3x(this);
          //System.out.println(opX);
          px = opX;
        }
      } else {
        // negative determinant
        // not simple rotation    
        if (order == 2) {
          opType = TYPE_REFLECTION;
        } else {
          opType = TYPE_ROTOINVERSION;
          if (order == 3)
            order = 6;
          angle = (int) (Math.acos((-trace - 1) / 2d) * 180 / Math.PI);
          if (angle == 120) {
            if (opX == null)
              opX = opGet3x(this);
            px = opX;
          }
        }
      }
      break;
    }
    opOrder = order;
    M4d m4 = new M4d();
    double d = 0;
    P3d p1 = new P3d(); // PURPOSELY 0 0 0
    P3d p2 = P3d.newP(px);

    m4.setM4(this);
    P3d p1sum = new P3d();
    P3d p2sum = P3d.newP(p2);
    P3d p2odd = new P3d();
    P3d p2even = P3d.newP(p2);
    V3d m4trans = new V3d();
    P3d p21 = new P3d();
    for (int i = 1; i < order; i++) {
      m4.mul(this);
      rotTrans(p1);
      rotTrans(p2);
      if (i == 1)
        p21.setT(p2);
      p1sum.add(p1);
      p2sum.add(p2);
      if (opType == TYPE_ROTOINVERSION) {
        if (i % 2 == 0) {
          p2even.add(p2);
        } else {
          p2odd.add(p2);
        }
      }
    }
    m4.getTranslation(m4trans);
    d = approx6(m4trans.length());
    if (d > 0) {
      // issue here with 1/2 1/2 1/2 group 9?
      opTrans = V3d.newV(m4trans);
      opTrans.scale(1d / order);
      m4trans.normalize();
    }

    opPoint = new P3d();
    V3d v = null;
    boolean isOK = true;
    switch (opType) {
    case TYPE_INVERSION:
      // just get average of p2 and x
      p2sum.add2(p2, px);
      p2sum.scale(0.5d);
      opPoint = P3d.newP(opClean(p2sum));
      isOK = checkOpPoint(opPoint);
      break;
    case TYPE_ROTATION:
      // the sum divided by the order gives us a point on the vector
      // both {0 0 0} and x will be translated by the same amount
      // so their difference will be the rotation vector
      // but this could be reversed if no translation
      rotTrans(p1);
      p1.scale(1d / order);
      d = approx6(p1.length()); // because we started at 0 0 0
      v = V3d.newVsub(p2sum, p1sum);
      v.normalize();
      opAxis = (V3d) opClean(v);
      // for the point, we do a final rotation on p1 to get it full circle
      p1sum.scale(1d / order);
      p1.setT(p1sum);
      if (d > 0) {
        p1sum.sub(opTrans);
      }
      opPoint = P3d.newP(opClean(p1sum));
      if (d > 0) {
        // all screws must start and terminate within the cell
          p1sum.add2(opPoint, opTrans);
          isOK = checkOpPoint(p1sum);
      }
      // average point for origin
      if (Math.abs(angle) != 180) {
          p2.cross(px, p2);
          opIsCCW = Boolean.valueOf(p2.dot(v) < 0);
      }
      isOK &= checkOpAxis(p1, (d == 0 ? opAxis : opTrans), p1sum, new V3d(), new V3d(), null);
      if (isOK) {
        opPoint.setT(p1sum);
        if (checkOpAxis(opPoint, opAxis, p1sum, new V3d(), new V3d(),
            opPoint)) {
          opPoint2 = P3d.newP(p1sum);
        }
        
      }
      break;
    case TYPE_ROTOINVERSION:
      // get the vector from the centroids off the odd and even sets
      p2odd.scale(2d / order);
      p2even.scale(2d / order);
      v = V3d.newVsub(p2odd, p2even);
      v.normalize();
      opAxis = (V3d) opClean(v);
//      opAxisCode = opGetAxisCode(opAxis);
      p1sum.add2(p2odd, p2even);
      p2sum.scale(1d / order);
      opPoint = P3d.newP(opClean(p2sum));
      isOK = checkOpPoint(opPoint);
      if (Math.abs(angle) != 180) {
        p2.cross(px, p2);
        opIsCCW = Boolean.valueOf(p2.dot(v) < 0);
      }
      break;
    case TYPE_REFLECTION:

      //.........x-------.--p-----------.
      //..........\...../.\.p........../|d
      //..........X\.DP/...\p........./.|
      //............\./.....\......../..p2
      //.............o------pv-----*/../
      //................V...p.....d|../DP
      //....................p......|./
      //....................p......p1
      //
      // Reflect and possibly translate the origin and an irrational point.
      // Then V = DP - X is the vector (o->v) for the plane
      // and the projection of p1 onto the line V (ov) gives the mirror
      // point *. The plane point is then half way between o and *.
      // d is the length of the translation, (p1 - *) is the translation
      // 

      v = V3d.newVsub(p2, p1);
      v.sub(px);
      v.normalize(); // normal to the plane
      opAxis = (V3d) opClean(v);
      if (d > 0 && approx6(v.dot(m4trans)) != 0) {
        isOK = false;
        break;
      }
      p1sum.set(0, 0, 0);
      V3d vt = new V3d();
      approx6(MeasureD.projectOntoAxis(p1, p1sum, v, vt));
      // opTrans is the point * first, then we subtract that from p1 to get the true translation 
      opPoint = P3d.newP(vt);
      opPoint.scale(0.5d);
      // now define the plane
      // now define the plane as a P4
      opPlane = new P4d();
      isOK = checkOpPlane(opPoint, v, opPlane);
      v = null;
      break;
    }
    if (isOK && v != null && (d >= 1 || v.dot(p1) < 0)) {
      isOK = false;
    }

    if (isOK && opTrans != null && d > 0) {
      //      if (opTrans.length() > 0.8)
      //        System.out.println(opTrans);
      opType |= TYPE_TRANSLATION;
      opClean(opTrans);
    } else {
      opTrans = null;
    }
    if (!isOK || opTrans != null && !checkOpTrans(opTrans)) {
      isIrrelevant = true;
    }
//    if (isOK && opAxis != null)
//      System.out.println(this + " " + opAxis + " " + opGetAxisCode(opAxis) + " " + getOpName(2));
  }

//  private static String opGetAxisCode(V3d v) {
//    double d = Double.MAX_VALUE;
//    if (v.x != 0 && Math.abs(v.x)< d)
//       d = Math.abs(v.x);
//    if (v.y != 0 && Math.abs(v.y)< d)
//      d = Math.abs(v.y);
//    if (v.z != 0 && Math.abs(v.z)< d)
//      d = Math.abs(v.z);
//    V3d v1 = V3d.newV(v);
//    v1.scale(1/d);
//    return "" + ((int)approx(v1.x)) + ((int)approx(v1.y)) + ((int)approx(v1.z));
//  }

  private static boolean hasTrans(M4d m4) {
    return (approx6(m4.m03) != 0 || approx6(m4.m13) != 0 || approx6(m4.m23) != 0);
  }
  
  private static P4d[] opPlanes;

  private static boolean checkOpAxis(P3d pt, V3d axis, P3d ptRet, V3d t1,
                                     V3d t2, P3d ptNot) {
    if (opPlanes == null) {
      opPlanes = BoxInfo.getBoxFacesFromOABC(null);
    }
    int[] map = BoxInfo.faceOrder;
    double f = (ptNot == null ? 1 : -1);
    for (int i = 0; i < 6; i++) {
      P3d p = MeasureD.getIntersection(pt, axis, opPlanes[map[i]], ptRet, t1, t2);
      if (p != null && checkOpPoint(p) && axis.dot(t1) * f < 0 && (ptNot == null || approx(ptNot.distance(p) - 0.5d) >= 0)) {
        return true;
      }
    }
 
    return false;
  }

  private static boolean checkOpTrans(T3d pt) {
    return pt.lengthSquared() < 1;
  }

  static T3d opClean(T3d t) {
    if (approx6(t.x) == 0)
      t.x = 0;
    if (approx6(t.y)== 0)
      t.y = 0;
    if (approx6(t.z) == 0)
      t.z = 0;
    return t;
  }

  static boolean checkOpPoint(T3d pt) {
    return checkOK(pt.x, 0) && checkOK(pt.y, 0) && checkOK(pt.z, 0);
  }

  private static boolean checkOK(double p, double a) {
    return (a != 0 || p >= 0 && p <= 1);
  }

  private static boolean checkOpPlane(P3d p2sum, V3d v, P4d plane) {
    // just check all 8 cell points for directed distance to the plane
    // any mix of + and - and 0 is OK; all + or all - is a fail
    MeasureD.getPlaneThroughPoint(p2sum, v, plane);
    plane.w = approx6(plane.w);
    P3d[] pts = BoxInfo.unitCubePoints;
    int nPos = 0;
    int nNeg = 0;
    for (int i = 8; --i >= 0;) {
      double d = MeasureD.getPlaneProjection(pts[i], plane, p2sum, v);
      switch ((int) Math.signum(approx6(d))) {
      case 1:
        if (nNeg > 0)
          return true;
        nPos++;
        break;
      case 0:
        break;
      case -1:
        if (nPos > 0)
          return true;
        nNeg++;
      }
    }
    // all + or all - means the plane is out of scope
    return !(nNeg == 8 || nPos == 8);
  }
  
  public static SymmetryOperation[] getAdditionalOperations(SymmetryOperation[] ops) {
    int n = ops.length;
    Lst<SymmetryOperation> lst = new Lst<SymmetryOperation>();
    HashSet<String> xyzLst = new HashSet<String>();

    Map<String, Lst<SymmetryOperation>> mapPlanes = new Hashtable<String, Lst<SymmetryOperation>>();
    for (int i = 0; i < n; i++) {
      SymmetryOperation op = ops[i];
      lst.addLast(op);
      String s = op.getOpName(PLANE_MODE_NOTRANS);
      //System.out.println(s);
      xyzLst.add(s);
      if ((op.getOpType() & TYPE_REFLECTION) == TYPE_REFLECTION)
        addPlaneMap(mapPlanes, op);
    }
    for (int i = 1; i < n; i++) { // skip x,y,z
      ops[i].addOps(xyzLst, lst, mapPlanes, n);
    }
    return lst.toArray(new SymmetryOperation[lst.size()]);
  }

  /**
   * add translated copies of this operation that contribute to the unit cell [0,1]
   * @param xyzList 
   * @param lst
   * @param mapPlanes 
   * @param n0 
   */
  void addOps(HashSet<String> xyzList, Lst<SymmetryOperation> lst, Map<String, Lst<SymmetryOperation>> mapPlanes, int n0) {
    V3d t0 = new V3d();
    getTranslation(t0);
    boolean isPlane = ((getOpType() & TYPE_REFLECTION) == TYPE_REFLECTION);
    //System.out.println("SO? addAdd for " + getOpName(PLANE_MODE_FULL) + " " + getOpAxis());
    V3d t = new V3d();
    
    SymmetryOperation opTemp = null;
    // from -2 to 2, starting with + so that we get the + version
    for (int i = 3; --i > -3;) {
      for (int j = 3; --j > -3;) {
        for (int k = 3; --k > -3;) {
          if (opTemp == null)
            opTemp = new SymmetryOperation(null, 0, false);

          // allow 0 0 0, as that brings in the translation?
          t.set(i, j, k);
          if (checkOpSimilar(t))
            continue;
          if (opTemp.opCheckAdd(this, t0, n0, t, xyzList, lst)) {
            if (isPlane)
              addPlaneMap(mapPlanes, opTemp);
            opTemp = null;            
          }
        }
      }
    }
  }

  /**
   * Looking for coincidence. We only concern ourselves if there is 
   * at least one non-glide reflection.
   * 
   * @param mapPlanes
   * @param op
   */
  private static void addPlaneMap(Map<String, Lst<SymmetryOperation>> mapPlanes,
                             SymmetryOperation op) {
    String s = op.getOpName(PLANE_MODE_POSITION_ONLY);
    Lst<SymmetryOperation> l = mapPlanes.get(s);
    //System.out.println("SO ====" + s + "====" + op.getOpName(PLANE_MODE_FULL));
    op.isCoincident = false;
    if (l == null) {
      mapPlanes.put(s, l = new Lst<SymmetryOperation>());
    } else {
      for (int i = l.size(); --i >= 0;) {
         SymmetryOperation op0 = l.get(i);
         if (op0.isCoincident || op0.getOpType() == TYPE_REFLECTION) {
           op.isCoincident = op0.isCoincident = true;
           break;
         }
      }
    }
    l.addLast(op);
  }

  /**
   * No need to check lattice translations that are only
   * going to contribute to the inherent translation 
   * of the element. Yes, these exist. But they are 
   * inconsequential and are never shown.
   * 
   * Reflections: anything perpendicular to the normal is discarded.
   * 
   * Rotations: anything parallel to the normal is discarded.
   *  
   * @param t
   * @return true if 
   */
  private boolean checkOpSimilar(V3d t) {
    switch (getOpType() &~ TYPE_TRANSLATION) {
    default:
      return false;
    case TYPE_IDENTITY:
      return true;
    case TYPE_ROTATION: // includes screw rotation
      return (approx6(t.dot(opAxis) - t.length()) == 0);
    case TYPE_REFLECTION: // includes glide reflection
      return (approx6(t.dot(opAxis)) == 0);
    }
  }

  /**
   * @param opThis 
   * @param t0 
   * @param n0  
   * @param t 
   * @param xyzList 
   * @param lst 
   * @return true if added
   */
  private boolean opCheckAdd(SymmetryOperation opThis, V3d t0, int n0, V3d t,
                          HashSet<String> xyzList, Lst<SymmetryOperation> lst) {
    //int nnew = 0;
    setM4(opThis);
    t.add(t0);
    setTranslation(t);
    opType = TYPE_UNKNOWN;
    isIrrelevant = false;
    int type = getOpType();
    if (!isIrrelevant && type != TYPE_IDENTITY && type != TYPE_TRANSLATION) {
      String s = getOpName(PLANE_MODE_NOTRANS);
      if (!xyzList.contains(s)) {
        xyzList.add(s);
        lst.addLast(this);
        isFinalized = true;
        xyz = getXYZFromMatrix(this, false, false, false);
//        System.out.println(//"SO " +//+ opTemp + "\n" + 
//        " "  + t00 +
//        " "+ ++nnew + " " + xyz + " type is " + s
        //+ 
        //"\n$draw s" + nnew + " symop \""+xyz+"\" // " + t00 + " " + s
//        );
        return true;
      }
    }
    return false;
  }

  static T3d approxPt(T3d pt) {
    if (pt != null) {
      pt.x = approx6(pt.x);
      pt.y = approx6(pt.y);
      pt.z = approx6(pt.z);
    }
    return pt;
  }

  public static void normalize12ths(V3d vtrans) {
    vtrans.x = PT.approxD(vtrans.x, 12);
    vtrans.y = PT.approxD(vtrans.y, 12);
    vtrans.z = PT.approxD(vtrans.z, 12);    
  }

  public String getCode() {
    getOpType();
    String s = "";
    return null;
  }
  

  // https://crystalsymmetry.wordpress.com/space-group-diagrams/
 
}
