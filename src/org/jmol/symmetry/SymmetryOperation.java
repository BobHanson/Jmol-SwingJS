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

import org.jmol.util.BoxInfo;
import org.jmol.util.Logger;
import org.jmol.util.Parser;

import javajs.util.Lst;
import javajs.util.M34d;
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
  public String xyz;
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
  private V3d opGlide;
  private P3d opPoint, opPoint2;
  private V3d opAxis;
  P4d opPlane;
  private Boolean opIsCCW;
  M3d spinU;
  public String suvw = null;
 
  
  /**
   * CIF space_group_symop_spin_operation_uvw_id
   * 
   * was an idea Bob had to separate out the uvw part
   * from the xyz part into separate CIF blocks. 
   * The idea was not ultimately part of the SpinCIF dictionary.
   */
  public String suvwId = null;
  public int spinIndex = -1;

  private int opPerDim;
  


  /**
   * a flag to indicate that we should not show this operation for DRAW SPACEGROUP ALL
   * 
   */
  boolean isIrrelevant;
  int iCoincident;

  final static int OP_MODE_POSITION_ONLY = 0;
  final static int OP_MODE_NOTRANS = 1;
  final static int OP_MODE_FULL = 2;

  public String getOpDesc() {
    if (opType == TYPE_UNKNOWN)
      setOpTypeAndOrder();
    switch (opType) {
    case TYPE_IDENTITY:
      return "I";
    case TYPE_TRANSLATION:
      return "Trans";
    case TYPE_ROTATION:
      return "Rot" + opOrder;
    case TYPE_INVERSION:
      return "Inv";
    case TYPE_REFLECTION:
      return "Plane";
    case TYPE_SCREW_ROTATION:
      return "Screw" + opOrder;
    case TYPE_ROTOINVERSION:
      return "Nbar" + opOrder;
    case TYPE_GLIDE_REFLECTION:
      return "Glide";
    }
    return null;
  }
  
  private String getOpName(int opMode) {
    if (opType == TYPE_UNKNOWN)
      setOpTypeAndOrder();
    switch (opType) {
    case TYPE_IDENTITY:
      return "I";
    case TYPE_TRANSLATION:
      return "Trans" + op48(opTrans);
    case TYPE_ROTATION:
      return "Rot" + opOrder + op48(opPoint) + op48(opAxis) + opIsCCW;
    case TYPE_INVERSION:
      return "Inv" + op48(opPoint);
    case TYPE_REFLECTION:
      return (opMode == OP_MODE_POSITION_ONLY ? "" : "Plane") + opRound(opPlane);
    case TYPE_SCREW_ROTATION:
      return (opMode == OP_MODE_POSITION_ONLY ? "S" + op48(opPoint) + op48(opAxis) : "Screw" + opOrder + op48(opPoint) + op48(opAxis) + op48(opTrans)
          + opIsCCW);
    case TYPE_ROTOINVERSION:
      return "Nbar" + opOrder + op48(opPoint) + op48(opAxis) + opIsCCW;
    case TYPE_GLIDE_REFLECTION:
      return (opMode == OP_MODE_POSITION_ONLY ? "" : "Glide") + opRound(opPlane)
          + (opMode == OP_MODE_FULL ? op48(opTrans) : "");
    }
    System.out.println("SymmetryOperation REJECTED TYPE FOR " + this);
    return "";
  }

  private String opRound(P4d p) {
    return Math.round(p.x * 1000) 
        + "," + Math.round(p.y * 1000) 
        + "," + Math.round(p.z * 1000)
        + "," + Math.round(p.w * 1000)
        ;
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
      return "screw rotation " + opOrder
          + (opIsCCW == null ? "" : opIsCCW == Boolean.TRUE ? "(+) " : "(-) ")
          + opFrac(opTrans);
    case TYPE_ROTOINVERSION:
      return opOrder + "-bar "
          + (opIsCCW == null ? "" : opIsCCW == Boolean.TRUE ? "(+) " : "(-) ")
          + opFrac(opPoint);
    case TYPE_GLIDE_REFLECTION:
      return "glide reflection " + opFrac(opTrans);
    }
    return "";
  }

  private static String opFrac(T3d p) {
    return "{" + opF(p.x) + " " + opF(p.y) + " " + opF(p.z) + "}";
  }

  private static String opF(double x) {
    if (x == 0)
      return "0";
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
    if (PT.approxD(n48 / 48d - x, 1000) != 0)
      return (neg ? "-" : "") + PT.approxD(x, 1000);
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
    return (neg ? "-" : "") + (n * div + n48 * div / 48)
        + (div == 1 ? "" : "/" + div);
  }

  private static String op48(T3d p) {
    if (p == null) {
      System.err.println("SymmetryOperation.op48 null");
      return "(null)";
    }

    return "{" + Math.round(p.x * 48) + " " + Math.round(p.y * 48) + " "
        + Math.round(p.z * 48) + "}";
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
  public String subsystemCode;
  int timeReversal;

  private boolean unCentered;
  boolean isCenteringOp;
  private int magOp = Integer.MAX_VALUE;
  int divisor = 12; // could be 120 for magnetic;
  private T3d opX;
  private String opAxisCode;
  public boolean opIsLong;
  private boolean isPointGroupOp;

  /**
   * Sets sigma and the subsystem code for modulated CIF
   * 
   * @param subsystemCode
   * @param sigma
   */
  public void setSigma(String subsystemCode, Matrix sigma) {
    this.subsystemCode = subsystemCode;
    this.sigma = sigma;
  }

  /**
   * 
   * @param op
   *        operation to clone or null
   * @param id
   *        opId for this operation; ignored if cloning
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
    spinU = op.spinU;
    spinIndex = op.spinIndex;
    suvw = op.suvw;
    setMatrix(false);
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
    if (isFinalized)
      return;
    div12(this, divisor);
    if (modDim > 0) {
      double[][] a = rsvs.getArray();
      for (int i = a.length - 1; --i >= 0;)
        a[i][3 + modDim] = finalizeD(a[i][3 + modDim], divisor);
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

  public String getXyz(boolean normalized) {
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
      double trans = r[3];
      if (trans == 0) {
        sb.append("0");
      } else {
        trans *= (trans == (int) trans ? 4 : 48);
        sb.append(twelfthsOf(isCanonical ? normalizeTwelfths(trans / 48, 48, true) : (int) trans));
      }
      sb.append("\t]\n");
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
      // deprecated
      // alternatively, we accept notation indicating explicit spin transformation "(mx,my,mz)"
      int pt = xyz.indexOf("(");
      mxyz = xyz.substring(pt + 1, xyz.length() - 1);
      xyz = xyz.substring(0, pt);
      allowScaling = false;
    } else if (xyz.indexOf('u') >= 0) {
      // spin-frame ssg as x,y,z (u,v,w)
      // FSGReader may append '+'
      boolean posDetOnly = xyz.endsWith("+");
      int pt = xyz.indexOf('(');
      String s;
      if (pt < 0) {
        s = xyz;
        isPointGroupOp = true;
      } else {
        s = xyz.substring(pt + 1, xyz.length() - (posDetOnly ? 2 : 1));
        xyz = xyz.substring(0, pt);
      }
      if (s.indexOf(',') < 0) {
        // temporary CIF key identifier for this spin part
        suvwId = s;
      } else {
        setSpin(s);
        if (posDetOnly && timeReversal < 0)
          return false;
      }
      allowScaling = true;
    }
    String strOut = getRotTransArrayAndXYZ(this, xyz, linearRotTrans,
        allowScaling, halfOrLess, true, null);
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
    if (spinU == null && timeReversal != 0)
      this.xyz += (timeReversal == 1 ? ",m" : ",-m");
    if (Logger.debugging) {
      Logger.debug("" + this);
      if (spinU != null) {
        Logger.debug("" + spinU.toString().replace('\n', ' '));
      }
    }
    return true;
  }

  public void setSpin(String s) {
    suvw = s;
    double[] v = new double[16];
    getRotTransArrayAndXYZ(null, s, v, true, false, false, MODE_UVW);
    spinU = new M3d();
    M4d.newA16(v).getRotationScale(spinU);
    timeReversal = (int) spinU.determinant3();    
    suvwId = null;
  }

  /**
   * Sets the divisor to 0 for n/9 or n/mm
   * 
   * @param xyz
   * @return 0 or 12
   */
  private static int setDivisor(String xyz) {
    int pt = xyz.indexOf('/');
    int len = xyz.length();
    while (pt > 0 && pt < len - 1) {
      char c = xyz.charAt(pt + 1);
      if ("2346".indexOf(c) < 0
          || pt < len - 2 && Character.isDigit(xyz.charAt(pt + 2))) {
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
      if (linearRotTrans[15] == 0) {
        // assume this was a 3x3
        m33 = 1;
        isPointGroupOp = true;
        setRotationScale(spinU = M3d.newA9(linearRotTrans));     
      } else {
        setA(linearRotTrans);
      }
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
        int denom = (divisor == 0 ? ((int) v) & DIVISOR_MASK : divisor);
        if (denom == 0)
          denom = 12;
        v = finalizeD(v, divisor);
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

  public static M4d getMatrixFromXYZ(String xyz, double[] v,
                                     boolean halfOrLess) {
    return getMatrixFromXYZScaled(xyz, v, halfOrLess);
  }

  public static M4d getMatrixFromXYZScaled(String xyz, double[] v,
                                            boolean halfOrLess) {
    if (v == null)
      v = new double[16];
    xyz = getRotTransArrayAndXYZ(null, xyz, v, false, halfOrLess, true, null);
    if (xyz == null)
      return null;
    M4d m = new M4d();
    m.setA(v);
    return div12(m, setDivisor(xyz));
  }

  /**
   * JmolCanonical is translations (-1/2,1/2]
   * @param xyz
   * @return xyz with translations (-1/2,1/2]
   */
  static String getJmolCanonicalXYZ(String xyz) {
    try {
      return getRotTransArrayAndXYZ(null, xyz, null, false, true, true, null);
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
   * @param retString
   * @param labels 
   * @return canonized Jones-Faithful string
   */
  public static String getRotTransArrayAndXYZ(SymmetryOperation op, String xyz,
                                           double[] linearRotTrans,
                                           boolean allowScaling,
                                           boolean halfOrLess,
                                           boolean retString, 
                                           String labels) {
    boolean isDenominator = false;
    boolean isDecimal = false;
    boolean isNegative = false;
    xyz = PT.rep(xyz, "[bio[", "");
    int modDim = (op == null ? 0 : op.modDim);
    int nRows = 4 + modDim;
    int divisor = (op == null ? setDivisor(xyz) : op.divisor);
    boolean doNormalize = halfOrLess
        && (op == null ? !xyz.startsWith("!") : op.doNormalize);
    int dimOffset = (modDim > 0 ? 3 : 0); // allow a b c to represent x y z
    if (linearRotTrans != null) {
      int n = linearRotTrans.length - 1;
      for (int i = n; --i >= 0;)
        linearRotTrans[i] = 0;
      linearRotTrans[n] = 1;
    }
    // may be a-b,-5a-5b,-c;0,0,0 form
    int transPt = xyz.indexOf(';') + 1;
    if (transPt != 0) {
      allowScaling = true;
      if (transPt == xyz.length())
        xyz += "0,0,0";
    }
    int rotPt = -1;
    String[] myLabels = getLabels(labels,(op == null || modDim == 0 ? null : op.myLabels));
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
    double itrans = 0;
    double decimalMultiplier = 1d;
    String strT = "";
    String strOut = (retString ? "" : null);
    int[] ret = new int[1];
    int len = xyz.length();
    int signPt = -1;
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
      case '+':
        isNegative = (ch == '-');
        signPt = i;
        itrans = iValue;
        iValue = 0;
        continue;
      case '/':
        denom = 0;
        isDenominator = true;
        continue;
      case 'x':
      case 'y':
      case 'z':
      case 'u':
      case 'v':
      case 'w':
      case 'a':
      case 'b':
      case 'c':
      case 'd':
      case 'e':
      case 'f':
      case 'g':
      case 'h':
        tpt0 = rowPt * nRows;
        int ipt = (ch >= 'x' ? ch - 'x' 
            : ch >= 'u' ? ch - 'u' 
            : ch - 'a' + dimOffset);
        xpt = tpt0 + ipt;
        int val = (isNegative ? -1 : 1);
        if (allowScaling && iValue != 0 && signPt != i - 1) {
          if (linearRotTrans != null)
            linearRotTrans[xpt] = iValue;
          if (iValue != (int) iValue) {
            if (strOut != null)
              strT += plusMinus(strT, iValue, myLabels[ipt], false, false);
            iValue = 0;
            break;
          }
          val = (int) iValue;
          iValue = 0;
        } else if (linearRotTrans != null) {
          linearRotTrans[xpt] = val;
        }
        if (strOut != null)
          strT += plusMinus(strT, val, myLabels[ipt], false, false);
        break;
      case ',':
        if (transPt != 0) {
          if (transPt > 0) {
            // now read translation
            rotPt = i;
            i = transPt - 1;
            transPt = -i;
            iValue = itrans = 0;
            denom = 0;
            continue;
          }
          transPt = i + 1;
          i = rotPt;
        } else if (itrans != 0) {
          iValue = itrans;
          itrans = 0;
        }
        // add translation in 12ths
        iValue = normalizeTwelfths(iValue,
            denom == 0 ? 12 : divisor == 0 ? denom : divisor, doNormalize);
        if (linearRotTrans != null)
          linearRotTrans[tpt0 + nRows - 1] = (divisor == 0 && denom > 0
              ? iValue = toDivisor(numer, denom)
              : iValue);
        if (strOut != null) {
          strT += xyzFraction12(iValue, (divisor == 0 ? denom : divisor), false,
              halfOrLess);
          // strT += xyzFraction48(iValue, false, true);
          strOut += (strOut == "" ? "" : ",") + strT;
        }
        if (rowPt == nRows - 2)
          return (retString ? strOut : "ok");
        iValue = itrans = 0;
        numer = 0;
        denom = 0;
        strT = "";
        tpt0 += 4;
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
          } else if (isNegative && ch == '0') {
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

  /**
   * given a number n (such as 1.05) and a divisor d (such as 20) 
   * return an integer of the hex type 0xnndd
   * where nn is (int) n * d, and dd is the divisor
   * @param numer a decimal number multiple of 1/d
   * @param denom the deminator for the fraction 1/d
   * @return encoded integer 0xnndd
   */
  private final static int toDivisor(double numer, int denom) {
    int n = (int) numer;
    if (n != numer) {
      // could happen with magnetic lattice centering 1/5 + 1/2 = 7/10
      double f = numer - n;
      denom = (int) Math.abs(denom / f);
      n = (int) (Math.abs(numer) / f);
    }
    return ((n << DIVISOR_OFFSET) + denom);
  }

  private final static String xyzFraction12(double n12ths, int denom,
                                            boolean allPositive,
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
    int n = Math.round((float) n12ths);
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
  private static String plusMinus(String strT, double x, String sx,
                                  boolean allowFractions,
                                  boolean fractionAsRational) {
    double a = Math.abs(x);
    double afrac = a % 1; // -1.3333 and 1.3333 become 0.3333
    if (a < 0.0001d) {
      return "";
    }
    String s = (a > 0.9999d && a <= 1.0001d ? ""
        : afrac <= 0.001d && !allowFractions ? "" + (int) a
            : fractionAsRational ? "" + a : twelfthsOf(a * 12));
    return (x < 0 ? "-" : strT.length() == 0 ? "" : "+") 
        + (s.equals("1") ? "" : s) + sx;
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
  /**
   * Private because this does NOT column-based
   */
  final private static String MODE_ABC = "abc"; // for internal use only
  final private static String MODE_XYZ = "xyz";
  final private static String MODE_UVW = "uvw";
  final private static String MODE_MXYZ = "mxyz";
  final static String[] labelsXYZ = new String[] { "x", "y", "z" };
  final static String[] labelsUVW = new String[] { "u", "v", "w" };
  final static String[] labelsABC = new String[] { "a", "b", "c" };
  final static String[] labelsMXYZ = new String[] { "mx", "my", "mz" };
  final static String[] labelsXn = new String[] { "x1", "x2", "x3", "x4", "x5",
      "x6", "x7", "x8", "x9", "x10", "x11", "x12", "x13" };
  final static String[] labelsXnSub = new String[] { "x", "y", "z", "a", "b",
      "c", "d", "e", "f", "g", "h", "i", "j" };

  final public static String getXYZFromMatrix(M4d mat, boolean is12ths,
                                              boolean allPositive,
                                              boolean halfOrLess) {
    return getXYZFromMatrixFrac(mat, is12ths, allPositive, halfOrLess, false, false, null);
  }

  /**
   * package-private
   * 
   * NOTE: THIS METHOD DOES NOT transpose columns to rows for ABC format. 
   * DO NOT use this method unless the matrix is already row-based but still ABC format.
   * 
   * @param mat
   * @param is12ths
   * @param allPositive
   * @param halfOrLess
   * @param allowFractions
   * @param fractionAsRational
   * @param labels  "abc", etc., or NULL for "xyz"
   * @return string row-form of matrix with the given labels
   */
  static String getXYZFromMatrixFrac(M4d mat, boolean is12ths,
                                                  boolean allPositive,
                                                  boolean halfOrLess,
                                                  boolean allowFractions, 
                                                  boolean fractionAsRational,
                                                  String labels) {
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
    String[] labels_ = getLabels(labels, labelsXYZ);
    for (int i = 0; i < 3; i++) {
      int lpt = (i < 3 ? 0 : 3);
      mat.getRow(i, row);
      String term = "";
      for (int j = 0; j < 3; j++) {
        double x = row[j];
        if (approx(x) != 0) {
          term += plusMinus(term, x, labels_[j + lpt], allowFractions, fractionAsRational);
        }
      }
      if ((is12ths ? row[3] : approx(row[3])) != 0) {
        String f = (fractionAsRational ? plusMinus(term, row[3], "", true, true) : xyzFraction12((is12ths ? row[3] : row[3] * denom), denom,
            allPositive, halfOrLess));
        if (term == "")
          f = (f.charAt(0) == '+' ? f.substring(1) : f);
        term += f;
      }
      str += "," + (term == "" ? "0" : term);
    }
    return str.substring(1);
  }

  private static String[] getLabels(String labels, String[] defLabels) {
    if (labels != null)
      switch (labels) {
      case MODE_ABC:
        return labelsABC;
      case MODE_UVW:
        return labelsUVW;
      case MODE_MXYZ:
        return labelsMXYZ;
      }
    return (defLabels == null ? labelsXYZ : defLabels);
  }

  public V3d[] rotateAxes(V3d[] vectors, UnitCell unitcell, P3d ptTemp, M3d mTemp) {
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

  /**
   * Get string version of fraction
   * 
   * @param p
   * @param sep space or comma
   * @return "1/2" for example
   */
  public static String fcoord(T3d p, String sep) {
    return opF(p.x) + sep + opF(p.y) + sep + opF(p.z);
  }

  static double approx(double f) {
    return PT.approxD(f, 100);
  }

  static double approx6(double f) {
    return PT.approxD(f, 1000000);
  }

  public static String getXYZFromRsVs(Matrix rs, Matrix vs, boolean is12ths) {
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
      s += xyzFraction12((int) (va[i][0] * (is12ths ? 1 : 12)), 12, false,
          true);
    }
    return PT.rep(s.substring(1), ",+", ",");
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
   * For spin space groups, always return 1, since the time-reversal is already built into the operator.
   * 
   * @return +1, -1, or 0
   */
  int getMagneticOp() {
        return (magOp == Integer.MAX_VALUE
        ? magOp = (int) (spinU != null ? 1 : determinant3() * timeReversal)
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
    doFinalize();
    if (centering == null && !unCentered) {
      if (modDim == 0 && isTranslation(this)) {
        isCenteringOp = true;
        centering = V3d.new3(m03, m13, m23);
      } else {
        unCentered = true;
        centering = null;
      }
    }
    return centering;
  }

  public static boolean isTranslation(M4d op) {
    return op.m00 == 1 && op.m01 == 0 && op.m02 == 0 
        && op.m10 == 0 && op.m11 == 1 && op.m12 == 0 
        && op.m20 == 0 && op.m21 == 0 && op.m22 == 1               
        && (op.m03 != 0 || op.m13 != 0 || op.m23 != 0);
  }

  String fixMagneticXYZ(M4d m, String xyz) {
    if (spinU != null)
      return xyz + getSpinString(spinU, true, true);
    if (timeReversal == 0)
      return xyz;
    int pt = xyz.indexOf("m");
    pt -= (3 - timeReversal) / 2;
    xyz = (pt < 0 ? xyz : xyz.substring(0, pt));
    M3d m3 = new M3d();
    m.getRotationScale(m3);
    if (getMagneticOp() < 0)
      m3.scale(-1); // does not matter that we flip m33 - it is never checked
    return xyz + getSpinString(m3, false, true);
  }

  /**
   * allows for 3x3 rotation-only matrix;
   * @param m
   * @param isUVW
   *        will allow 0.577.... not changing that to a fraction
   * @param withParens 
   * @return full x,y,...(u,v,....) or just u,v,w
   */
  public static String getSpinString(M34d m, boolean isUVW, boolean withParens) {
    M4d m4;
    if (m instanceof M3d) {
      m4 = new M4d();
      m4.setRotationScale((M3d) m);
    } else {
      m4 = (M4d) m;
    }
    String s = getXYZFromMatrixFrac(m4, false, false, false, isUVW, isUVW,
        (isUVW ? SymmetryOperation.MODE_UVW : SymmetryOperation.MODE_MXYZ));
    return (withParens ? "(" + s + ")" : s);
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
      if (spinU == null && magOp != Double.MAX_VALUE)
        info.put("magOp", Double.valueOf(magOp));
      info.put("id", Integer.valueOf(opId));
      if (timeReversal != 0)
        info.put("timeReversal", Integer.valueOf(timeReversal));
      if (spinU != null) {
        info.put("spinU", spinU);
        info.put("uvw",xyz.replace('x', 'u').replace('y', 'v').replace('z', 'w'));
      }
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
   * @param fracPts
   * @param i0
   *        first index
   * @param n
   *        number of atoms
   */
  public static void normalizeOperationToCentroid(int dim, M4d m, P3d[] fracPts,
                                                  int i0, int n) {
    if (n <= 0)
      return;
    double x = 0;
    double y = 0;
    double z = 0;
    if (atomTest == null)
      atomTest = new P3d();
    for (int i = i0, i2 = i + n; i < i2; i++) {
      m.rotTrans2(fracPts[i], atomTest);
      x += atomTest.x;
      y += atomTest.y;
      z += atomTest.z;
    }
    x /= n;
    y /= n;
    z /= n;
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
    Lst<P3d> list = new Lst<P3d>();
    for (int i = 0; i < ops.length; i++) {
      T3d c = (ops[i] == null ? null : ops[i].getCentering());
      if (c != null)
        list.addLast(P3d.newP(c));
    }
    return list;
  }

  public static Lst<String> getLatticeCenteringStrings(SymmetryOperation[] ops) {
    Lst<String> list = new Lst<String>();
    for (int i = 0; i < ops.length; i++) {
      T3d c = (ops[i] == null ? null : ops[i].getCentering());
      if (c != null)
        list.addLast(ops[i].xyzOriginal);
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
    return (opTrans == null ? (opTrans = new V3d()) : opTrans);
  }

  private final static P3d x = P3d.new3(Math.PI, Math.E, Math.PI * Math.E);

  /**
   * The problem is that the 3-fold axes for cubic groups have (seemingly
   * arbitrary assignments for axes
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
      m.getRow(r, row);
      for (int i = 0; i < 3; i++) {
        switch ((int) row[i]) {
        case 1:
          c |= (i + 1) << ((2 - r) << 3);
          break;
        case -1:
          c |= (0x10 + i + 1) << ((2 - r) << 3);
          break;
        }
      }
    }
    return c;
  }

  //private static V3d xpos;
  private static V3d xneg;

  private static T3d opGet3x(M4d m) {
    if (m.m22 != 0) // z-axis
      return x;
    int c = opGet3code(m);
    for (int i = 0; i < 8; i++)
      if (c == C3codes[i]) {
        if (xneg == null) {
          xneg = V3d.newV(x);
          xneg.scale(-1);
        }
        return xneg;
      }
    return x;
  }

  private void setOpTypeAndOrder() {
    clearOp();
    // From International Tables for Crystallography, Volume A, Chapter 1.2, by H. Wondratschek and M. I. Aroyo
    int det = Math.round((float) determinant3());
    int trace = Math.round((float) (m00 + m11 + m22));
    //int code1 = ((trace + 3) << 1) + ((det + 1) >> 1);
    int order = 0;
    // handle special cases of identity and inversion
    int angle = 0;
    T3d px = x;
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
    P3d p1 = new P3d(); // PURPOSELY 0 0 0
    P3d p2 = P3d.newP(px);

    m4.setM4(this);
    P3d p1sum = new P3d();
    P3d p2sum = P3d.newP(p2);
    P3d p2odd = new P3d();
    P3d p2even = P3d.newP(p2);
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
    opTrans = new V3d();
    m4.getTranslation(opTrans);
    opTrans.scale(1d / order);
    double d = approx6(opTrans.length());
    opPoint = new P3d();
    V3d v = null;
    boolean isOK = true;
    switch (opType) {
    case TYPE_INVERSION:
      // just get average of p2 and x
      p2sum.add2(p2, px);
      p2sum.scale(0.5d);
      opPoint = P3d.newP(opClean6(p2sum));
      isOK = checkOpPoint(opPoint);
      break;
    case TYPE_ROTOINVERSION:
      // get the vector from the centroids off the odd and even sets
      p2odd.scale(2d / order);
      p2even.scale(2d / order);
      v = V3d.newVsub(p2odd, p2even);
      v.normalize();
      opAxis = (V3d) opClean6(v);
      //      opAxisCode = opGetAxisCode(opAxis);
      p1sum.add2(p2odd, p2even);
      p2sum.scale(1d / order);
      opPoint.setT(opClean6(p2sum));
      isOK = checkOpPoint(opPoint);
      if (angle != 180) {
        p2.cross(px, p2);
        opIsCCW = Boolean.valueOf(p2.dot(v) < 0);
      }
      break;
    case TYPE_ROTATION:
      // the sum divided by the order gives us a point on the vector
      // both {0 0 0} and x will be translated by the same amount
      // so their difference will be the rotation vector
      // but this could be reversed if no translation

      //      rotTrans(p1);
      //      p1.scale(1d / order);
      //      d = approx6(p1.length()); // because we started at 0 0 0
      v = V3d.newVsub(p2sum, p1sum);
      v.normalize();
      opAxis = (V3d) opClean6(v);
      // for the point, we do a final rotation on p1 to get it full circle
      p1sum.scale(1d / order);
      p1.setT(p1sum);
      if (d > 0) {
        p1sum.sub(opTrans);
      }
      // average point for origin
      opPoint.setT(p1sum);
      opClean6(opPoint);
      if (angle != 180) {
        p2.cross(px, p2);
        opIsCCW = Boolean.valueOf(p2.dot(v) < 0);
      }
      isOK &= checkOpAxis(p1, (d == 0 ? opAxis : opTrans), p1sum, new V3d(),
          new V3d(), null);
      if (isOK) {
        opPoint.setT(p1sum);
        // this next changes opPoint value position to front edge or where it just touches.
        if (checkOpAxis(opPoint, opAxis, p2, new V3d(), new V3d(), opPoint)) {
            opPoint2 = P3d.newP(p2);        
          // was for vertical offset of screw components 4(+) and 4(-)
          //          if (order != 2 && opIsCCW == Boolean.FALSE && d > 0 && d <= 0.5d) {
          //            opPoint.add(opTrans);
          //          }
        }
       if (d > 0) {
          // all screws must start and terminate within the cell
          p1sum.scaleAdd2(0.5d, opTrans, opPoint);
          //          p1sum.add2(opPoint, opTrans);
          isOK = checkOpPoint(p1sum);
          if (opPoint2 != null) {
            // or at least half...
            p1sum.scaleAdd2(0.5d, opTrans, opPoint2);
            if (!checkOpPoint(p1sum))
              opPoint2 = null;
          }
          // real question here...
          // problem here with p1 not being a vector, just the base point along the axis.
          if (v.dot(p1) < 0) {
            isOK = false;
          }
        }
      }
      break;
    case TYPE_REFLECTION:
      // first plane point is half way from 0 to p1 - trans
      p1.sub(opTrans);
      p1.scale(0.5d);
      opPoint.setT(p1);
      // p2 - px - opTrans gets us the plane's normal (opAxis)
      // (we don't do this with origin because it is likely on the plane)
      p21.sub(opTrans);
      opAxis = V3d.newVsub(p21, px);
      p2.scaleAdd2(0.5d, opAxis, px);
      opAxis.normalize();
      opPlane = new P4d();
      p1.set(px.x + 1.1d, px.y + 1.7d, px.z + 2.1d); // just need a third point
      p1.scale(0.5d);
      rotTrans(p1);
      p1.sub(opTrans);
      p1.scaleAdd(0.5d, px, p1);
      p1.scale(0.5d);
      v = new V3d();
      isOK = checkOpPlane(opPoint, p1, p2, opPlane, v, new V3d());
      opClean6(opPlane);
      if (approx6(opPlane.w) == 0)
        opPlane.w = 0;
      approx6Pt(opAxis);
      normalizePlane(opPlane);
      break;
    }
    if (d > 0) {
      opClean6(opTrans);
      double dmax = 1;
      if (opType == TYPE_REFLECTION) {
        // BUT opTrans is the composite translation, not just the glide??
        if (opTrans.z == 0 && opTrans.lengthSquared() == 1.25d
            || opTrans.z == 0.5d && opTrans.lengthSquared() == 1.5d) {
          // SG 186
          // -x+y+2,y+1,z
          dmax = 1.25d;
          opIsLong = true;
        } else {
          // this is skipping "-y+2/3,-x+1/3,z+5/6" in SG 161
          dmax = 0.78d;
        }
        opGlide = V3d.newV(opTrans);
        fixNegTrans(opGlide);
        if (opGlide.length() == 0)
          opGlide = null;
        // being careful here not to disallow this for vertical planes in #156; only for #88
        if ((opTrans.x == 1 || opTrans.y == 1 || opTrans.z == 1) && m22 == -1)
          isOK = false;
      } else {
        if (opTrans.z == 0 && opTrans.lengthSquared() == 1.25d) {
          // SG 177   -x+y+2,y+1,-z+2
          dmax = 1.25d;
          opIsLong = true;
        }
      }
      opType |= TYPE_TRANSLATION;
      // opTrans is the FULL translation, not just the glide!
      if (Math.abs(approx(opTrans.x)) >= dmax
          || Math.abs(approx(opTrans.y)) >= dmax
          || Math.abs(approx(opTrans.z)) >= dmax) {
        isOK = false;
      }
    } else {
      opTrans = null;
    }
    if (!isOK) {
      isIrrelevant = true;
    }
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

  private void fixNegTrans(V3d t) {
    t.x = normHalf(t.x);
    t.y = normHalf(t.y);
    t.z = normHalf(t.z);
  }

  private static void normalizePlane(P4d plane) {
    approx6Pt(plane);
    plane.w = approx6(plane.w);
    if (plane.w > 0 || plane.w == 0 && (plane.x < 0
        || plane.x == 0 && plane.y < 0 || plane.y == 0 && plane.z < 0)) {
      plane.scale4(-1);
    }
    // unsure no -0 values; we need this for the maps
    opClean6(plane);
    plane.w = approx6(plane.w);
  }

  private static boolean isCoaxial(T3d v) {
    return (Math.abs(approx(v.x)) == 1 || Math.abs(approx(v.y)) == 1
        || Math.abs(approx(v.z)) == 1);
  }

  private void clearOp() {
    doFinalize();
    isIrrelevant = false; // this can be a problem. 
    opTrans = null;
    opPoint = opPoint2 = null;
    opPlane = null;
    opIsCCW = null;
    opIsLong = false;
  }

  private static boolean hasTrans(M4d m4) {
    return (approx6(m4.m03) != 0 || approx6(m4.m13) != 0
        || approx6(m4.m23) != 0);
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
      P3d p = MeasureD.getIntersection(pt, axis, opPlanes[map[i]], ptRet, t1,
          t2);
      if (p != null && checkOpPoint(p) && axis.dot(t1) * f < 0
          && (ptNot == null || approx(ptNot.distance(p) - 0.5d) >= 0)) {
        return true;
      }
    }

    return false;
  }

  static T3d opClean6(T3d t) {
    if (approx6(t.x) == 0)
      t.x = 0;
    if (approx6(t.y) == 0)
      t.y = 0;
    if (approx6(t.z) == 0)
      t.z = 0;
    return t;
  }

  static boolean checkOpPoint(T3d pt) {
    return checkOK(pt.x, 0) && checkOK(pt.y, 0) && checkOK(pt.z, 0);
  }

  private static boolean checkOK(double p, double a) {
    return (a != 0 || approx(p) >= 0 && approx(p) <= 1);
  }

  private static boolean checkOpPlane(P3d p1, P3d p2, P3d p3, P4d plane,
                                      V3d vtemp1, V3d vtemp2) {
    // just check all 8 cell points for directed distance to the plane
    // any mix of + and - and 0 is OK; all + or all - is a fail
    MeasureD.getPlaneThroughPoints(p1, p2, p3, vtemp1, vtemp2, plane);

    P3d[] pts = BoxInfo.unitCubePoints;
    int nPos = 0;
    int nNeg = 0;
    for (int i = 8; --i >= 0;) {
      double d = MeasureD.getPlaneProjection(pts[i], plane, p1, vtemp1);
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

  public static SymmetryOperation[] getAdditionalOperations(SymmetryOperation[] ops, int per_dim) {
    int n = ops.length;
    Lst<SymmetryOperation> lst = new Lst<SymmetryOperation>();
    SB xyzLst = new SB();
    Map<String, Lst<SymmetryOperation>> mapPlanes = new Hashtable<String, Lst<SymmetryOperation>>();
    V3d vTemp = new V3d();
    for (int i = 0; i < n; i++) {
      SymmetryOperation op = ops[i];
      op.opPerDim = per_dim;
      lst.addLast(op);
      String s = op.getOpName(OP_MODE_NOTRANS);
      xyzLst.append(s).appendC(';');
      if ((op.getOpType() & TYPE_REFLECTION) != 0)
        addCoincidentMap(mapPlanes, op, TYPE_REFLECTION, vTemp);
      else if (op.getOpType() == TYPE_SCREW_ROTATION)
        addCoincidentMap(mapPlanes, op, TYPE_SCREW_ROTATION, null);
    }
    for (int i = 1; i < n; i++) { // skip x,y,z
      ops[i].addOps(xyzLst, lst, mapPlanes, n, i, vTemp);
    }
    return lst.toArray(new SymmetryOperation[lst.size()]);
  }

  /**
   * add translated copies of this operation that contribute to the unit cell
   * [0,1]
   * 
   * @param xyzList
   * @param lst
   * @param mapCoincident
   * @param n0
   * @param isym
   * @param vTemp 
   */
  private void addOps(SB xyzList, Lst<SymmetryOperation> lst,
              Map<String, Lst<SymmetryOperation>> mapCoincident, int n0, int isym, V3d vTemp) {
    V3d t0 = new V3d();
    getTranslation(t0);
    boolean isPlane = ((getOpType() & TYPE_REFLECTION) == TYPE_REFLECTION);
    boolean isScrew = (getOpType() == TYPE_SCREW_ROTATION);
    V3d t = new V3d();
    SymmetryOperation opTemp = null;
    // originally from -2 to 2, starting with + so that we get the + version
    // but for "225:1/2b+1/2c,1/2a+1/2c,1/2a+1/2b" we need higher.
    // and needed =2 to 4 to catch all the elements
    
    // periodicity here affects how the arrays run.
    int i0 = 5, i1 = -2, j0 = 5, j1 = -2, k0 = 5, k1 = -2;
    switch (opPerDim) {
    case 0x73: // abc 3-space
      break;
    case 0x33: // ab plane, layer
    case 0x22: // ab plane, layer
      k0 = 1;
      k1 = 0;
      break;
    case 0x12: // a frieze 2-space
    case 0x13: // a rod
      j0 = 1;
      j1 = 0;
      k0 = 1;
      k1 = 0;
      break;
    case 0x43: // c rod
      i0 = 1;
      i1 = 0;
      j0 = 1;
      j1 = 0;
      break;
    case 0x23: // b rod
      i0 = 1;
      i1 = 0;
      k0 = 1;
      k1 = 0;
      break;
    }
    for (int i = i0; --i >= i1;) {
      for (int j = j0; --j >= j1;) {
        for (int k = k0; --k >= k1;) {
          if (opTemp == null)
            opTemp = new SymmetryOperation(null, 0, false);
          t.set(i, j, k);
          if (checkOpSimilar(t, vTemp))
            continue;
          if (opTemp.opCheckAdd(this, t0, n0, t, xyzList, lst, isym + 1)) {
            if (isPlane)
              addCoincidentMap(mapCoincident, opTemp, TYPE_REFLECTION, vTemp);
            else if (isScrew)
              addCoincidentMap(mapCoincident, opTemp, TYPE_SCREW_ROTATION, null);
            opTemp = null;
          }
        }
      }
    }
  }

  /**
   * Looking for coincidence.
   * 
   * see # We only concern ourselves if there is at least one non-glide
   * reflection.// why? e-glide will be coincident
   * 
   * @param mapCoincident
   *        coincident planes map
   * @param op
   * @param opType
   * @param vTemp 
   */
  private static void addCoincidentMap(Map<String, Lst<SymmetryOperation>> mapCoincident,
                                       SymmetryOperation op, int opType,
                                       V3d vTemp) {
    if (op.isIrrelevant)
      return;
    String s = op.getOpName(OP_MODE_POSITION_ONLY);
    Lst<SymmetryOperation> l = mapCoincident.get(s);
    op.iCoincident = 0;
    boolean isRotation = (opType == TYPE_SCREW_ROTATION);
    if (l == null) {
      mapCoincident.put(s, l = new Lst<SymmetryOperation>());      
    } else if (isRotation) {
      // we are getting rid of 3-screw in favor of 6-screw
      if (op.opOrder == 6) {
        for (int i = l.size(); --i >= 0;) {
          SymmetryOperation op1 = l.get(i);
          if (!op1.isIrrelevant)
            switch (op1.opOrder) {
            case 3:
              op1.isIrrelevant = true;
              break;
            case 6:
              // could check here for parity or length?
              break;
            }
        }
      }
      op.iCoincident = 1;
    } else {
      SymmetryOperation op0 = null;
      for (int i = l.size(); --i >= 0;) {
        op0 = l.get(i);
        if (op.opGlide != null && op0.opGlide != null) {
          vTemp.sub2(op.opGlide, op0.opGlide);
          
          if (vTemp.lengthSquared() < 1e-6) {
            // space groups 218, 225, 227 will fire this
            op.isIrrelevant = true;
            return;
          }
          vTemp.add2(op.opGlide, op0.opGlide);
          if (vTemp.lengthSquared() < 1e-6) {
            // space groups 218, 225, 227 will fire this
            op.isIrrelevant = true;
            return;
          }
          vTemp.add2(op.opAxis, op0.opAxis);
          if (vTemp.lengthSquared() < 1e-6) {
            // opposite axes. Should we reverse one?
            // maybe check trans vs opAxis? 
            op.isIrrelevant = true;
            return;
          }
        } else if (op.opGlide == null && op0.opGlide == null) {
          
          vTemp.add2(op.opAxis, op0.opAxis);
          if (vTemp.lengthSquared() < 1e-6) {
            // opposite axes. Should we reverse one?
            // maybe check trans vs opAxis? 
            op.isIrrelevant = true;
            return;
          }
          vTemp.sub2(op.opAxis, op0.opAxis);
          if (vTemp.lengthSquared() < 1e-6) {
            // same axes
            // how did this happen?
            op.isIrrelevant = true;
            return;
          }
        }


      }
      // FOR loop leaves op0 = lst.get(0);
      if (op0.iCoincident == 0) {
        op.iCoincident = 1;
        op0.iCoincident = -1;
      } else {
        op.iCoincident = -op0.iCoincident;
      }
    }
    l.addLast(op);
  }

  /**
   * No need to check lattice translations that are only going to contribute to
   * the inherent translation of the element. Yes, these exist. But they are
   * inconsequential and are never shown.
   * 
   * Reflections: anything perpendicular to the normal is discarded.
   * 
   * Rotations: anything parallel to the normal is discarded.
   * 
   * @param t
   * @param vTemp temp vector
   * @return true if
   */
  private boolean checkOpSimilar(V3d t, V3d vTemp) {
    switch (getOpType() & ~TYPE_TRANSLATION) {
    default:
      return false;
    case TYPE_IDENTITY:
      return true;
    case TYPE_ROTATION: // includes screw rotation
      return (approx6(t.dot(opAxis) - t.length()) == 0);
    case TYPE_REFLECTION: // includes glide reflection
      vTemp.cross(t, opAxis);
      // t.cross(opAxis)==0 is a translation PERPENDICULAR to the plane
      // t.dot(opAxis)==0 is a translation IN the plane
      return (approx6(vTemp.length()) == 0 ? false : approx6(t.dot(opAxis)) == 0);
    }
  }

  /**
   * @param opThis
   * @param t0
   * @param n0
   * @param t
   * @param xyzList
   * @param lst
   * @param itno
   * @return true if added
   */
  private boolean opCheckAdd(SymmetryOperation opThis, V3d t0, int n0, V3d t,
                             SB xyzList, Lst<SymmetryOperation> lst, int itno) {
    //int nnew = 0;
    setM4(opThis);
    V3d t1 = V3d.newV(t);
    t1.add(t0);
    setTranslation(t1);
    isFinalized = true;
    setOpTypeAndOrder();
    if (isIrrelevant || opType == TYPE_IDENTITY || opType == TYPE_TRANSLATION)
      return false;
    String s = getOpName(OP_MODE_NOTRANS) + ";";
    if ((opType & TYPE_REFLECTION) == 0 && xyzList.indexOf(s) >= 0) {
        return false;
    }
    xyzList.append(s);
    spinU = opThis.spinU;
    suvw = opThis.suvw;
    timeReversal = opThis.timeReversal;
    lst.addLast(this);
    isFinalized = true;
    xyz = getXYZFromMatrix(this, false, false, false);
    return true;
  }

  static void approx6Pt(T3d pt) {
    if (pt != null) {
      pt.x = approx6(pt.x);
      pt.y = approx6(pt.y);
      pt.z = approx6(pt.z);
    }
  }

  public static void normalize12ths(V3d vtrans) {
    vtrans.x = PT.approxD(vtrans.x, 12);
    vtrans.y = PT.approxD(vtrans.y, 12);
    vtrans.z = PT.approxD(vtrans.z, 12);
  }

  public String getCode() {
    if (opAxisCode != null) {
      return opAxisCode;
    }
    char t = getOpName(OP_MODE_FULL).charAt(0); // four bits
    int o = opOrder; // 1,2,3,4,6   // 3 bits
    int ccw = (opIsCCW == null ? 0 : opIsCCW == Boolean.TRUE ? 1 : 2);
    String g = "", m = "";
    switch (t) {
    case 'G':
      t = getGlideFromTrans(opTrans, opPlane);
      //a,b,c,n,g
      //$FALL-THROUGH$
    case 'P':
      if (!isCoaxial(opAxis)) {
        t = (t == 'P' ? 'p' : (char) (t - 32));
      }
      break;
    case 'S':
      double d = opTrans.length();
      if (opIsCCW != null
          && (d < (d > 1 ? 6 : 0.5d)) == (opIsCCW == Boolean.TRUE))
        t = 'w';
      break;
    case 'R':
      if (!isCoaxial(opAxis)) {
        t = 'o';
      }
      if (opPoint.length() == 0)
        t = (t == 'o' ? 'q' : 'Q');
      break;
    default:
      break;
    }
    return opAxisCode = g + m + t + "." + ((char) ('0' + o)) + "." + ccw + ".";
  }

  /**
   * note = this method will return 'n' for SG 161 do an operator's odd 1/2 -1/2 1/2 glide
   * (but only for additional operations, not the basic set
   * 
   * but ITA says "tetragonal and cubic only" in Table ITA1969 4.1.6 
   * 
   * 
   * @param ftrans
   * @param ax1
   * @return one of a b c d g n 
   */
  public static char getGlideFromTrans(T3d ftrans, T3d ax1) {
    double fx = Math.abs(approx(ftrans.x * 12));
    double fy = Math.abs(approx(ftrans.y * 12));
    double fz = Math.abs(approx(ftrans.z * 12));
    if (fx == 9)
      fx = 3;
    if (fy == 9)
      fy = 3;
    if (fz == 9)
      fz = 3;
    int nonzero = 3;
    if (fx == 0)
      nonzero--;
    if (fy == 0)
      nonzero--;
    if (fz == 0)
      nonzero--;
    int sum = (int) (fx + fy + fz);
    switch (nonzero) {
    default:
    case 1:
      return (fx != 0 ? 'a' : fy != 0 ? 'b' : 'c');
    case 2:
      switch (sum) {
      case 6:  // 1/4 1/4 
        return 'd';
      case 12:
        P3d n = P3d.newP(ax1);
        n.normalize();
//not sure what this was about -- no n-glides in #230
//        // #230 
//        // making sure here that this is truly a diagonal in the plane, not just
//        // a glide parallel to a face on a diagonal plane! Mois Aroyo 2018
        if (Math.abs(approx(n.x + n.y + n.z)) == 1)
          return 'n';
      }
      // 'g'
      break;
    case 3:
      switch (sum) { // 1/4 1/4 1/4 or 1/2 1/2 1/2
      case 9:
        return 'd';
      case 18:
        return 'n';
      }
      // 'g'
      break;
    }
    return 'g';
  }

  static void rotateAndTranslatePoint(M4d m, P3d src, int ta, int tb, int tc,
                                      P3d dest) {
    m.rotTrans2(src, dest);
    dest.add3(ta, tb, tc);
  }

  /**
   * Convert an operation string in one basis to the equivalent string in
   * another basis.
   * 
   * Performs trm^-1 * op * trm
   * 
   * as per
   * https://www.cryst.ehu.es/cgi-bin/cryst/programs/nph-show-tr-matrix?what=gp&way=&type=&trm=x%2By%2C%2Dx%2By%2Cz&from=ita@trgen
   * 
   * Rationale:
   * 
   * Consider operation R and transform P. Let Q = P^-1. Then we are claiming
   * that
   * 
   * R' = Q*R*P
   * 
   * The way I think about this is to think about what a matrix operation does
   * in relation to coordinates. The role of an operation is to transform a
   * coordinate c1 in real space to another point, c2. But symmetry operations
   * do not work directly on Cartesian coordinates. Rather, they operate on
   * fractional coordinates, f1 and f2, expressed in a basis relative to
   * Cartesian space -- the unit cell.
   * 
   * Thus, we have a 4x4 matrix F for converting Cartesian coordinates to
   * fractional and its inverse, C:
   * 
   * f1 = F(c1); c1 = C(f1); C = F^-1
   * 
   * and, applying operation R, we have:
   * 
   * f2 = R(f1)
   * 
   * we can write:
   * 
   * c2 = C(f2) = C(R(f1)) = C(R(F(c1)))
   * 
   * or
   * 
   * c2 = (C*R*F)(c1)
   * 
   * similarly, using the other basis we have to get the same thing, we can
   * write:
   * 
   * c2 = (C'*R'*F')(c1)
   * 
   * Thus,
   * 
   * C*R*F = C'*R'*F'
   *
   * This is just a statement that we can use any basis we want to do describe
   * the symmetry.
   * 
   * So what about Q*R*P ?
   * 
   * In general, since P is the description of the change of bases, which means
   * it relates a nonstandard fractional point description to its standard
   * description:
   * 
   * P(f') = f;
   * 
   * For example, if P is (c,a,b;1/2,0,0) and f' = {1/2 1/4 1/2}, then f = {3/4
   * 1/2 1/2}. (Trust me or do the math yourself using:
   * 
   * <pre>
   
      P = matrix("c,a,b;1/2,0,0");      
      f_ = {0.5 0.25 0.5};
      print P*f_;
   
   * </pre>
   * 
   * That is, the point {3/4 1/2 1/2} in the standard setting is described as
   * {1/2 1/4 1/2} in the nonstandard setting. These are just two descriptions
   * of the same point.
   * 
   * And the inverse is:
   * 
   * Q(f) = f'
   * 
   * We want a description of R' such that
   * 
   * c2 = (C*R*F)(c1) = (C'*R'*F')(c1)
   * 
   * 
   * c2 = (C*R*F)(c1)
   * 
   * C(f2) = C*R(f1)
   * 
   * f2 = R(f1)
   * 
   * P(f2') = R(P(f1'))
   * 
   * QP(f2') = QRP(f1')
   * 
   * f2' = QRP(f1')
   * 
   * and since
   * 
   * R'(f1') = f2'
   * 
   * we have
   * 
   * R'(f1') = Q*R*P(f1')
   * 
   * and we have the desired result, that
   * 
   * R' = Q*R*P
   * 
   * 
   * @param xyz
   * @param trm
   * @param trmInv
   * @param t
   *        temporary or null
   * @param v
   *        temporary or null
   * @param centering 
   * @param targetCentering 
   * @param normalize 
   * @param allowFractions
   * @return transformed string
   */
  static String transformStr(String xyz, M4d trm, M4d trmInv, M4d t,
                             double[] v, T3d centering, T3d targetCentering, boolean normalize, boolean allowFractions) {
    if (trmInv == null) {
      trmInv = M4d.newM4(trm);
      trmInv.invert();
    }
    if (t == null)
      t = new M4d();
    if (v == null)
      v = new double[16];
    M4d op = getMatrixFromXYZ(xyz, v, true);
    if (centering != null)
      op.add(centering);
    t.setM4(trmInv);
    t.mul(op);
    if (trm != null)
      t.mul(trm);
    if (targetCentering != null)
      op.add(targetCentering);
    if (normalize) {
      t.getColumn(3, v);
      // note that this returns only positive values, aa does the iTA
      for (int i = 0; i < 3; i++) {
        v[i] = (10+v[i]) % 1;
      }
      t.setColumnA(3, v);
    }
    String s = getXYZFromMatrixFrac(t, false, true, false, allowFractions, false, null);
    int pt = xyz.indexOf('(');
    if (pt > 0)
      s += xyz.substring(pt);
    return s;
  }

  static M4d stringToMatrix(String xyz, String labels) {
    int divisor = setDivisor(xyz);
    double[] a = new double[16];
    getRotTransArrayAndXYZ(null, xyz, a, true, false, false, labels);
    return div12(M4d.newA16(a), divisor);
  }

  public static String getTransformXYZ(M4d op) {
    return getXYZFromMatrixFrac(op, false, false,
        false, true, true, MODE_XYZ);
  }

  public static String getTransformUVW(M4d spin) {
    return getXYZFromMatrixFrac(spin, false, false, false, false, true,
        SymmetryOperation.MODE_UVW);
  }


  /**
   * This method properly transposes the matrix for ABC format, converting
   * columns to rows before passing the information to
   * getXYZFromMatrixFrac(...,"ABC").
   * 
   * @param transform the transform matrix
   * @param normalize
   *        only set to true by (undocumented?) script unitcell(m4,true); normalize translation
   *        to interval (-1/2,1/2]
   * @return a,b,c format for the given matrix
   */
  public static String getTransformABC(M34d transform, boolean normalize) {
    if (transform == null)
      return "a,b,c";
    M4d m;
    if (transform instanceof M3d) {
      m = M4d.newM4(null);
      m.setRotationScale((M3d) transform);
    } else {
      m = M4d.newM4((M4d) transform);
    }
    V3d tr = new V3d();
    m.getTranslation(tr);
    tr.scale(-1);
    m.add(tr);
    m.transpose();
    String s = SymmetryOperation
        .getXYZFromMatrixFrac(m, false, true, false, true, false, SymmetryOperation.MODE_ABC);
    if (tr.lengthSquared() < 1e-12d)
      return s;
    tr.scale(-1);
    return s + ";" + (normalize ? norm3(tr)
        : opF(tr.x) + "," + opF(tr.y) + "," + opF(tr.z));
  }

  static String norm3(T3d tr) {
    return norm(tr.x) + "," + norm(tr.y) + "," + norm(tr.z);
  }

  /**
   * normalize to interval (-1/2,1/2]
   * 
   * @param d
   * @return normalized translation
   */
  private static String norm(double d) {
    return opF(normHalf(d));
  }
  
  private static double normHalf(double d) {
    while (d <= -0.5) {
      d += 1;
    }
    while (d > 0.5) {
      d -= 1;
    }
    return d;
  }

  /**
   * Convert "1/2,1/2,0" to {0.5 0.5 0}
   * 
   * @param xyz
   * @param p
   * @return p or new P3d()
   */
  static P3d toPoint(String xyz, P3d p) {
    if (p == null)
      p = new P3d();
    String[] s = PT.split(xyz, ",");
    p.set(PT.parseDoubleFraction(s[0]), PT.parseDoubleFraction(s[1]),
        PT.parseDoubleFraction(s[2]));
    return p;
  }

  /**
   * rxyz option creates tab-separated string representation of a 3x4 (r,t) or 3x3 matrix with rational fractions
   * 
   * adds "|" prior to translation; does NOT add last row of 0 0 0 1
   * 
   * <code>
     (
        1  -1   0 |   1/2
        0   0   1 |  -1/2
        0   1   0 |    0
     )
   * </code>
   * 
   * Also accepts 3x3 matrix, in which case there is no |... part.
   * 
   * @param matrix
   * @return string representation
   */
  public static String matrixToRationalString(M34d matrix) {
    int dim = (matrix instanceof M4d ? 4 : 3);
    String ret = "(";
    for (int i = 0; i < 3; i++) {
      ret += "\n";
      for (int j = 0; j < dim; j++) {
        if (j > 0)
          ret += "\t";
        if (j == 3 && dim == 4)
          ret += "|  ";
        double d = (dim == 4 ? ((M4d) matrix).getElement(i, j) : ((M3d) matrix).getElement(i, j));
        if (d == (int) d) {
          ret += (d < 0 ? " " + (int) d : "  " + (int) d);
        } else {
          int n48 = (int) Math.round((d * 48));
          if (approx6(d * 48 - n48) != 0) {
            ret += d;
          } else {
            String s = opF(d);
            ret += (d > 0 ? " " + s : s);
          }
        }
      }
    }
    return ret + "\n)";
  }

  public void rotateSpin(T3d vib) {
    if (spinU == null)
      rotate(vib);
    else 
      spinU.rotate(vib);
  }

  public static Object staticConvertOperation(String xyz, M34d matrix34,
                                              String labels) {
    boolean toMat = (matrix34 == null);
    M4d matrix4 = null;
    if (toMat) {
        matrix4 = stringToMatrix(xyz, labels);
        if (xyz.indexOf("u") >= 0) {
          matrix34 = new M3d();
          matrix4.getRotationScale((M3d) matrix34);
          matrix4 = null;
        } else {
          matrix34 = matrix4;
        }
        // now matrix4 or matrix34 is null, but not both
    } else if (matrix34 instanceof M3d) {
      matrix4 = new M4d();
      matrix4.setRotationScale((M3d) matrix34);
    } else {
      matrix4 = (M4d) matrix34;
    }
    if ("rxyz".equals(labels)) {
      return matrixToRationalString(matrix34);
    }
    return (toMat ? matrix34
        : getXYZFromMatrixFrac(matrix4, false, false, false,
            true, false, labels));
  }

  // https://crystalsymmetry.wordpress.com/space-group-diagrams/

  @Override
  public String toString() {
    return (rsvs == null ? super.toString() 
//        
//        + " testid=" + testid + " " + spinIndex + " " + suvw 
//        
        : super.toString() + " " + rsvs.toString());
  }

  public String getSUVW() {
    if (suvw == null && spinU != null) {
      suvw = getSpinString(spinU, true, false);
    }
    return suvw;
  }

}
