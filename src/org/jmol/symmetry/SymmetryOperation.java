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

import java.util.Hashtable;
import java.util.Map;

import javajs.util.M3;
import javajs.util.M4;
import javajs.util.Matrix;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.T3;
import javajs.util.V3;

import org.jmol.util.Logger;
import org.jmol.util.Parser;

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


public class SymmetryOperation extends M4 {
  String xyzOriginal;
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
  private V3 centering;

  private static P3 atomTest;

  private String[] myLabels;
  int modDim;

// rsvs:
//    [ [(3+modDim)*x + 1]    
//      [(3+modDim)*x + 1]     [ Gamma_R   [0x0]   | Gamma_S
//      [(3+modDim)*x + 1]  ==    [0x0]    Gamma_e | Gamma_d 
//      ...                       [0]       [0]    |   1     ]
//      [0 0 0 0 0...   1] ]
  
  float[] linearRotTrans;
  
  Matrix rsvs;
  boolean isBio;
  private Matrix sigma;
  int index;
  String subsystemCode;
  int timeReversal;
  
  void setSigma(String subsystemCode, Matrix sigma) {
    this.subsystemCode = subsystemCode;
    this.sigma = sigma;
  }

  /**
   * @j2sIgnoreSuperConstructor
   * @j2sOverride
   * 
   * @param op
   * @param atoms
   * @param atomIndex
   * @param countOrId
   * @param doNormalize
   */
  SymmetryOperation(SymmetryOperation op, P3[] atoms,
                           int atomIndex, int countOrId, boolean doNormalize) {
    this.doNormalize = doNormalize;
    if (op == null) {
      opId = countOrId;
      return;
    }
    /*
     * externalizes and transforms an operation for use in atom reader
     * 
     */
    xyzOriginal = op.xyzOriginal;
    xyz = op.xyz;
    opId = op.opId;
    modDim = op.modDim;
    myLabels = op.myLabels;
    index = op.index;
    linearRotTrans = op.linearRotTrans;
    sigma = op.sigma;
    subsystemCode = op.subsystemCode;
    timeReversal = op.timeReversal;
    setMatrix(false);
    if (!op.isFinalized)
      doFinalize();
    if (doNormalize && sigma == null)
      setOffset(this, atoms, atomIndex, countOrId);
  }

  

  /**
   * rsvs is the superspace group rotation-translation matrix.
   * It is a (3 + modDim + 1) x (3 + modDim + 1) matrix from 
   * which we can extract all necessary parts;
   * @param isReverse 
   * 
   */
  private void setGamma(boolean isReverse) {
  // standard M4 (this)
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
    a = rsvs.getSubmatrix(0,  0,  3,  3).getArray();
    for (int i = 0; i < 3; i++)
      for (int j = 0; j < 4; j++)
        setElement(i,  j, (float) (j < 3 ? a[i][j] : t[i]));
    setElement(3,3,1);
  }

  void doFinalize() {
    div12(this);
    if (modDim > 0) {
      double[][] a = rsvs.getArray();
      for (int i = a.length - 1; --i >= 0;)
        a[i][3 + modDim] /= 12;
    }
    isFinalized = true;
  }
  
  private static M4 div12(M4 op) {
    op.m03 /= 12;
    op.m13 /= 12;
    op.m23 /= 12;
    return op;
  }

  String getXyz(boolean normalized) {
    return (normalized && modDim == 0 || xyzOriginal == null ? xyz : xyzOriginal);
  }

  static void newPoint(M4 m, P3 atom1, P3 atom2, int x, int y, int z) {
    m.rotTrans2(atom1, atom2);
    atom2.add3(x,  y,  z);
  }

  String dumpInfo() {
    return "\n" + xyz + "\ninternal matrix representation:\n"
        + toString();
  }

  final static String dumpSeitz(M4 s, boolean isCanonical) {
    SB sb = new SB();
    float[] r = new float[4];
    for (int i = 0; i < 3; i++) {
      s.getRow(i,r);
      sb.append("[\t");
      for (int j = 0; j < 3; j++)
        sb.appendI((int) r[j]).append("\t");
      float trans = r[3];
      if (trans != (int) trans)
        trans = 12 * trans;
      sb.append(twelfthsOf(isCanonical ? ((int)trans + 12)%12 : (int) trans)).append("\t]\n");
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
    xyz = xyz.toLowerCase();
    setModDim(modDim);
    boolean isReverse = (xyz.startsWith("!"));
    if (isReverse)
      xyz = xyz.substring(1);
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
      Parser.parseStringInfestedFloatArray(xyz, null, linearRotTrans);
      return setFromMatrix(null, isReverse);
    }
    if (xyz.indexOf("[[") == 0) {
      xyz = xyz.replace('[', ' ').replace(']', ' ').replace(',', ' ');
      Parser.parseStringInfestedFloatArray(xyz, null, linearRotTrans);
      for (int i = linearRotTrans.length; --i >= 0;)
        if (Float.isNaN(linearRotTrans[i]))
          return false;
      setMatrix(isReverse);
      isFinalized = true;
      isBio = (xyz.indexOf("bio") >= 0);
      this.xyz = (isBio ? toString() : getXYZFromMatrix(this, false, false,
          false));
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
    String strOut = getMatrixFromString(this, xyz, linearRotTrans, allowScaling);
    if (strOut == null)
      return false;
    if (mxyz != null) {
      // base time reversal on relationship between x and mx in relation to determinant
      boolean isProper = (M4.newA16(linearRotTrans).determinant3() == 1); 
      timeReversal = (((xyz.indexOf("-x") < 0) == (mxyz.indexOf("-mx") < 0)) == isProper ? 1 : -1);
    }
    setMatrix(isReverse);
    this.xyz = (isReverse ? getXYZFromMatrix(this, true, false, false) : strOut);
    if (timeReversal != 0)
      this.xyz += (timeReversal == 1 ? ",m" : ",-m");
    if (Logger.debugging)
      Logger.debug("" + this);
    return true;
  }

  private void setModDim(int dim) {
    int n = (dim + 4) * (dim + 4);
    modDim = dim;
    if (dim > 0)
      myLabels = labelsXn;
    linearRotTrans = new float[n];
  }

  private void setMatrix(boolean isReverse) {
    if (linearRotTrans.length > 16) {
      setGamma(isReverse);
    } else {
      setA(linearRotTrans);
      if (isReverse) {
        P3 p3 = P3.new3(m03,  m13,  m23);
        invert();
        rotate(p3);
        p3.scale(-1);
        setTranslation(p3);
      }
    }
  }

  boolean setFromMatrix(float[] offset, boolean isReverse) {
    float v = 0;
    int pt = 0;
    myLabels = (modDim == 0 ? labelsXYZ : labelsXn);
    int rowPt = 0;
    int n = 3 + modDim;
    for (int i = 0; rowPt < n; i++) {
      if (Float.isNaN(linearRotTrans[i]))
        return false;
      v = linearRotTrans[i];
      if (Math.abs(v) < 0.00001f)
        v = 0;
      boolean isTrans = ((i + 1) % (n + 1) == 0);
      if (isTrans) {
        if (offset != null) {
          v /= 12;
          if (pt < offset.length)
            v += offset[pt++];
        }
        v = normalizeTwelfths((v < 0 ? -1 : 1) * Math.abs(v * 12)
            / 12f, doNormalize);
        rowPt++;
      }
      linearRotTrans[i] = v;
    }
    linearRotTrans[linearRotTrans.length - 1] = 1;
    setMatrix(isReverse);
    isFinalized = (offset == null);
    xyz = getXYZFromMatrix(this, true, false, false);
    //System.out.println("testing " + xyz + " " + this + "\n" + Escape.eAF(linearRotTrans));
    return true;
  }

  public static M4 getMatrixFromXYZ(String xyz) {
    float[] linearRotTrans = new float[16];
    xyz = getMatrixFromString(null, xyz, linearRotTrans, false);
    return (xyz == null ? null : div12(M4.newA16(linearRotTrans)));  
  }

  /**
   * Convert the Jones-Faithful notation 
   *   "x, -z+1/2, y"  or "x1, x3-1/2, x2, x5+1/2, -x6+1/2, x7..."
   * to a linear array
   * 
   * Also allows a-b,-5a-5b,-c;0,0,0  format
   * 
   * @param op
   * @param xyz
   * @param linearRotTrans
   * @param allowScaling
   * @return canonized Jones-Faithful string
   */
  static String getMatrixFromString(SymmetryOperation op, String xyz,
                                    float[] linearRotTrans, boolean allowScaling) {
    boolean isDenominator = false;
    boolean isDecimal = false;
    boolean isNegative = false;
    int modDim = (op == null ? 0 : op.modDim);
    int nRows = 4 + modDim;
    boolean doNormalize = (op != null && op.doNormalize);
    int dimOffset = (modDim > 0 ? 3 : 0); // allow a b c to represent x y z
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
    float iValue = 0;
    int tensDenom = 0;
    float decimalMultiplier = 1f;
    String strT = "";
    String strOut = "";
    for (int i = 0; i < xyz.length(); i++) {
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
        tensDenom = 0;
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
        int ipt = (ch >= 'x' ? ch - 'x' :ch - 'a' + dimOffset);
        xpt = tpt0 + ipt;
        int val = (isNegative ? -1 : 1);
        if (allowScaling && iValue != 0) {
          linearRotTrans[xpt] = iValue; 
          val = (int) iValue;
          iValue = 0;
        } else {
          linearRotTrans[xpt] = val; 
        }
        strT += plusMinus(strT, val, myLabels[ipt]);
        break;
      case ',':
        if (transPt != 0) {
          if (transPt > 0) {
            // now read translation
            rotPt = i;
            i = transPt - 1;
            transPt = -i;
            iValue = 0;
            tensDenom = 0;
            continue;
          }
          transPt = i + 1;
          i = rotPt;
        }
        // add translation in 12ths
        iValue = normalizeTwelfths(iValue, doNormalize);//normalizeTwelfths(iValue, doNormalize);
        linearRotTrans[tpt0 + nRows - 1] = iValue;
        strT += xyzFraction12(iValue, false, true);
       // strT += xyzFraction48(iValue, false, true);
        strOut += (strOut == "" ? "" : ",") + strT;
        if (rowPt == nRows - 2)
          return strOut;
        iValue = 0;
        strT = "";
        if (rowPt++ > 2 && modDim == 0) {
          Logger.warn("Symmetry Operation? " + xyz);
          return null;
        }
        break;
      case '.':
        isDecimal = true;
        decimalMultiplier = 1f;
        continue;
      case '0':
        if (!isDecimal && (isDenominator || !allowScaling))
          continue;
        //$FALL-THROUGH$
      default:
        //Logger.debug(isDecimal + " " + ch + " " + iValue);
        int ich = ch - '0';
        if (ich >= 0 && ich <= 9) {
          if (isDecimal) {
            decimalMultiplier /= 10f;
            if (iValue < 0)
              isNegative = true;
            iValue += decimalMultiplier * ich * (isNegative ? -1 : 1);
            continue;
          }
          if (isDenominator) {
            if (ich == 1) {
              tensDenom = 1;
              continue;
            }
            if (tensDenom == 1) {
              ich += tensDenom * 10; // 12, 16
            }
            if (iValue == 0) {
              // a/2,....
              linearRotTrans[xpt] /= ich;
            } else {
              iValue /= ich;
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

  private final static String xyzFraction12(float n12ths, boolean allPositive, boolean halfOrLess) {
    float n = n12ths;
    if (allPositive) {
      while (n < 0)
        n += 12f;
    } else if (halfOrLess) {
      while (n > 6f)
        n -= 12f;
      while (n < -6f)
        n += 12f;
    }
    String s = twelfthsOf(n);
    return (s.charAt(0) == '0' ? "" : n > 0 ? "+" + s : s);
  }

  private final static String xyzFraction48ths(float n48ths, boolean allPositive, boolean halfOrLess) {
    float n = n48ths;
    if (allPositive) {
      while (n < 0)
        n += 48f;
    } else if (halfOrLess) {
      while (n > 24f)
        n -= 48f;
      while (n < -24f)
        n += 48f;
    }
    String s = fortyEighthsOf(n);
    return (s.charAt(0) == '0' ? "" : n > 0 ? "+" + s : s);
  }

  final static String twelfthsOf(float n12ths) {
    String str = "";
    if (n12ths < 0) {
      n12ths = -n12ths;
      str = "-";
    }
    int m = 12;
    int n = Math.round(n12ths);
    if (Math.abs(n - n12ths) > 0.01f) {
      // fifths? sevenths? eigths? ninths? sixteenths?
      // Juan Manuel suggests 10 is large enough here 
      float f = n12ths / 12;
      int max = 20;
      for (m = 3; m < max; m++) {
        float fm = f * m;
        n = Math.round(fm);
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
        return "" + n / 12;
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

  final static String fortyEighthsOf(float n48ths) {
    String str = "";
    if (n48ths < 0) {
      n48ths = -n48ths;
      str = "-";
    }
    int m = 12;
    int n = Math.round(n48ths);
    if (Math.abs(n - n48ths) > 0.01f) {
      // fifths? sevenths? eigths? ninths? sixteenths?
      // Juan Manuel suggests 10 is large enough here 
      float f = n48ths / 48;
      int max = 20;
      for (m = 5; m < max; m++) {
        float fm = f * m;
        n = Math.round(fm);
        if (Math.abs(n - fm) < 0.01f)
          break;
      }
      if (m == max)
        return str + f;
    } else {
      if (n == 48)
        return str + "1";
      if (n < 48)
        return str + twelfths[n % 48];
      switch (n % 48) {
      case 0:
        return "" + n / 48;
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

  private final static String[] twelfths = { "0", "1/12", "1/6", "1/4", "1/3",
  "5/12", "1/2", "7/12", "2/3", "3/4", "5/6", "11/12" };

  private final static String[] fortyeigths = { "0", 
    "1/48", "1/24", "1/16", "1/12",
    "5/48", "1/8", "7/48", "1/6", 
    "3/16", "5/24", "11/48", "1/4",
    "13/48", "7/24", "5/16", "1/3",
    "17/48", "3/8", "19/48", "5/12",
    "7/16", "11/24", "23/48", "1/2",
    "25/48", "13/24", "9/16", "7/12",
    "29/48", "15/24", "31/48", "2/3",
    "11/12", "17/16", "35/48", "3/4",
    "37/48", "19/24", "13/16", "5/6",
    "41/48", "7/8", "43/48", "11/12",
    "15/16", "23/24", "47/48"
  };

  private static String plusMinus(String strT, float x, String sx) {
    return (x == 0 ? "" : (x < 0 ? "-" : strT.length() == 0 ? "" : "+") + (x == 1 || x == -1 ? "" : "" + (int) Math.abs(x)) + sx);
  }

  private static float normalizeTwelfths(float iValue, boolean doNormalize) {
    iValue *= 12f;
    if (doNormalize) {
      while (iValue > 6)
        iValue -= 12;
      while (iValue <= -6)
        iValue += 12;
    }
    return iValue;
  }

  private static float normalize48ths(float iValue, boolean doNormalize) {
    iValue *= 48f;
    if (doNormalize) {
      while (iValue > 24)
        iValue -= 48;
      while (iValue <= -24)
        iValue += 48;
    }
    return iValue;
  }

  final static String[] labelsXYZ = new String[] {"x", "y", "z"};
  final static String[] labelsXn = new String[] {"x1", "x2", "x3", "x4", "x5", "x6", "x7", "x8", "x9", "x10", "x11", "x12", "x13"};
  final static String[] labelsXnSub = new String[] {"x", "y", "z", "a",  "b",  "c",  "d",  "e",  "f",  "g",   "h",   "i",   "j"};

  final public static String getXYZFromMatrix(M4 mat, boolean is12ths,
                                       boolean allPositive, boolean halfOrLess) {
    String str = "";
    SymmetryOperation op = (mat instanceof SymmetryOperation ? (SymmetryOperation) mat
        : null);
    if (op != null && op.modDim > 0)
      return getXYZFromRsVs(op.rsvs.getRotation(), op.rsvs.getTranslation(), is12ths);
    float[] row = new float[4];
    for (int i = 0; i < 3; i++) {
      int lpt = (i < 3 ? 0 : 3);
      mat.getRow(i, row);
      String term = "";
      for (int j = 0; j < 3; j++)
        if (row[j] != 0)
          term += plusMinus(term, row[j], labelsXYZ[j + lpt]);
      term += xyzFraction12((is12ths ? row[3] : row[3] * 12), allPositive,
          halfOrLess);
      str += "," + term;
    }
    return str.substring(1);
  }

  public static void setOffset(M4 m, P3[] atoms, int atomIndex, int count) {
    if (count == 0)
      return;
    /*
     * the center of mass of the full set of atoms is moved into the cell with this
     *  
     */
    float x = 0;
    float y = 0;
    float z = 0;
    if (atomTest == null)
      atomTest = new P3();
    for (int i = atomIndex, i2 = i + count; i < i2; i++) {
      newPoint(m, atoms[i], atomTest, 0, 0, 0);
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
    while (y < -0.001 || y >= 1.001) {
      m.m13 += (y < 0 ? 1 : -1);
      y += (y < 0 ? 1 : -1);
    }
    while (z < -0.001 || z >= 1.001) {
      m.m23 += (z < 0 ? 1 : -1);
      z += (z < 0 ? 1 : -1);
    }
  }

//  // action of this method depends upon setting of unitcell
//  private void transformCartesian(UnitCell unitcell, P3 pt) {
//    unitcell.toFractional(pt, false);
//    transform(pt);
//    unitcell.toCartesian(pt, false);
//
//  }
  
  V3[] rotateAxes(V3[] vectors, UnitCell unitcell, P3 ptTemp, M3 mTemp) {
    V3[] vRot = new V3[3];
    getRotationScale(mTemp);    
    for (int i = vectors.length; --i >=0;) {
      ptTemp.setT(vectors[i]);
      unitcell.toFractional(ptTemp, true);
      mTemp.rotate(ptTemp);
      unitcell.toCartesian(ptTemp, true);
      vRot[i] = V3.newV(ptTemp);
    }
    return vRot;
  }
  
  static String fcoord(T3 p) {
    return fc(p.x) + " " + fc(p.y) + " " + fc(p.z);
  }

  private static String fc(float x) {
    float xabs = Math.abs(x);
    String m = (x < 0 ? "-" : "");
    int x24 = (int) approxF(xabs * 24);
    if (x24 / 24f == (int)(x24 / 24f))
      return m + (x24 / 24);
    if (x24%8 != 0)
      return m + twelfthsOf(x24 >> 1);
    return (x24 == 0 ? "0" : x24 == 24 ? m + "1" : m + (x24/8) + "/3");
  }

  static float approxF(float f) {
    return PT.approx(f, 100);
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
          s += (r < 0 ? "-" : s.endsWith(",") ? "" : "+") + (Math.abs(r) == 1 ? "" : "" + (int) Math.abs(r)) + "x" + (j + 1);
        }
      }
      s += xyzFraction12((int) (va[i][0] * (is12ths ? 1 : 12)), false, true);
    }
    return PT.rep(s.substring(1), ",+", ",");
  }

  @Override
  public String toString() {
    return (rsvs == null ? super.toString() : super.toString() + " " + rsvs.toString());
  }

  private boolean unCentered;
  boolean isCenteringOp;

  private float magOp = Float.MAX_VALUE;

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
  float getMagneticOp() {
    return (magOp == Float.MAX_VALUE ? magOp = determinant3()
        * timeReversal : magOp);
  }

  /**
   * set the time reversal, and indicate internally in xyz as appended ",m" or ",-m"
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
  V3 getCentering() {
    if (!isFinalized)
      doFinalize();
    if (centering == null && !unCentered) {
      if (modDim == 0 && m00 == 1 && m11 == 1 && m22 == 1
          && m01 == 0 && m02 == 0 && m10 == 0 && m12 == 0 && m20 == 0
          && m21 == 0 && (m03 != 0 || m13 != 0 || m23 != 0)) {
        isCenteringOp = true;
        centering = V3.new3(m03, m13, m23);
      } else {
        unCentered = true;
        centering = null;
      }
    }
    return centering;
  }

  String fixMagneticXYZ(M4 m, String xyz, boolean addMag) {
    if (timeReversal == 0)
      return xyz;
    int pt = xyz.indexOf("m");
    pt -= (3 - timeReversal) / 2;
    xyz = (pt < 0 ? xyz : xyz.substring(0, pt));
    if (!addMag)
      return xyz + (timeReversal > 0 ? " +1" : " -1"); 
    M4 m2 = M4.newM4(m);
    m2.m03 = m2.m13 = m2.m23 = 0;
    if (getMagneticOp() < 0)
      m2.scale(-1); // does not matter that we flip m33 - it is never checked
    xyz += "("
        + PT.rep(PT.rep(PT.rep(
            SymmetryOperation.getXYZFromMatrix(m2, false, false, false), "x",
            "mx"), "y", "my"), "z", "mz") + ")";
    return xyz;
  }

  private Hashtable<String, Object> info;
  
  public Map<String, Object> getInfo() {
    if (info == null) {
      info = new Hashtable<String, Object>();
      info.put("xyz", xyz);
      if (centering != null)
        info.put("centering", centering);
      info.put("index", Integer.valueOf(index));
      info.put("isCenteringOp", Boolean.valueOf(isCenteringOp));
      if (linearRotTrans != null)
        info.put("linearRotTrans", linearRotTrans);
      info.put("modulationDimension", Integer.valueOf(modDim));
      info.put("matrix", M4.newM4(this));
      if (magOp != Float.MAX_VALUE)
        info.put("magOp", Float.valueOf(magOp));
      info.put("id", Integer.valueOf(opId));
      info.put("timeReversal",Integer.valueOf(timeReversal));
      if (xyzOriginal != null)
        info.put("xyzOriginal", xyzOriginal);
    }
    return info;
  }
  
}
