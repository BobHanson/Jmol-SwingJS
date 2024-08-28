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

import org.jmol.util.Logger;

import javajs.util.M4d;
import javajs.util.P3i;
import javajs.util.SB;

/**
 * Bob Hanson 9/2006
 * 
 * This class could be easily adapted for other use by implementing
 * something like M4d (4x4 matrix), P3i (integer point), and SB (stringBuilder)
 * 
 * references: International Tables for Crystallography Vol. A. (2002) 
 *
 * http://www.iucr.org/iucr-top/cif/cifdic_html/1/cif_core.dic/Ispace_group_symop_operation_xyz.html
 * http://www.iucr.org/iucr-top/cif/cifdic_html/1/cif_core.dic/Isymmetry_equiv_pos_as_xyz.html
 *
 * "Space-group notation with an explicit origin", S. R. Hall, 
 * Acta Cryst. (1981). A37, 517-525 
 * https://doi.org/10.1107/S0567739481001228
 *
 * LATT : http://macxray.chem.upenn.edu/LATT.pdf thank you, Patrick Carroll
 * 
 * Hall symbols:
 * 
 * http://cci.lbl.gov/sginfo/hall_symbols.html
 * 
 * and
 * 
 * http://cci.lbl.gov/cctbx/explore_symmetry.html
 * 
 * (-)L   [N_A^T_1]   [N_A^T_2]   ...  [N_A^T_P]   V(Nx Ny Nz)
 * 
 * lattice types S and T are not supported here
 * 
 * NEVER ACCESS THESE METHODS OUTSIDE OF THIS PACKAGE
 * 
 *
 */
final public class HallInfo {

  /**
   * An interface to receive the decoded matrices from Hall notation. These
   * matrices have only integer components. Translations are in 12ths.
   * 
   * In Jmol, org.jmol.symmetry.SpaceGroup.
   * 
   */
  public interface HallReceiver {

    /**
     * Get the receiver's current operation count.
     * 
     * @return number of operations
     */
    int getMatrixOperationCount();

    /**
     * Get the 4x4 matrix for the kth operation. Translations must be in 12ths.
     * 
     * @param k
     * @return 4x4 matrix, translations in integer 12ths
     */
    M4d getMatrixOperation(int k);

    /**
     * Add a (possibly) new operation, checking for duplicates.
     * 
     * @param operation
     * @return true if added, false if duplicate
     */
    boolean addHallOperationCheckDuplicates(M4d operation);

  }

  /**
   * A private class to process Hall rotation information.
   */
  final private static class HallRotation {

    private String rotCode;
    //int order;
    protected M4d seitzMatrix = new M4d();
    protected M4d seitzMatrixInv = new M4d();

    private HallRotation(String code, String matrixData) {
      rotCode = code;
      //order = code.charAt(0) - '0';
      double[] data = new double[16];
      double[] dataInv = new double[16];
      data[15] = dataInv[15] = 1d;

      for (int i = 0, ipt = 0; ipt < 11; i++) {
        int value = 0;
        switch (matrixData.charAt(i)) {
        case ' ':
          ipt++;
          continue;
        case '+':
        case '1':
          value = 1;
          break;
        case '-':
          value = -1;
          break;
        }
        data[ipt] = value;
        dataInv[ipt] = -value;
        ipt++;
      }
      seitzMatrix.setA(data);
      seitzMatrixInv.setA(dataInv);
    }

    protected static HallRotation lookup(String code) {
      for (int i = getHallTerms().length; --i >= 0;)
        if (hallRotationTerms[i].rotCode.equals(code))
          return hallRotationTerms[i];
      return null;
    }

    private static HallRotation[] hallRotationTerms;

    private synchronized static HallRotation[] getHallTerms() {
      // in matrix definitions, "+" = 1; "-" = -1;
      // just a compact way of indicating a 3x3
      return (hallRotationTerms == null
          ? hallRotationTerms = new HallRotation[] {
              new HallRotation("1_", "+00 0+0 00+"),
              new HallRotation("2x", "+00 0-0 00-"),
              new HallRotation("2y", "-00 0+0 00-"),
              new HallRotation("2z", "-00 0-0 00+"),
              new HallRotation("2'", "0-0 -00 00-") //z implied
              , new HallRotation("2\"", "0+0 +00 00-") //z implied
              , new HallRotation("2x'", "-00 00- 0-0"),
              new HallRotation("2x\"", "-00 00+ 0+0"),
              new HallRotation("2y'", "00- 0-0 -00"),
              new HallRotation("2y\"", "00+ 0-0 +00"),
              new HallRotation("2z'", "0-0 -00 00-"),
              new HallRotation("2z\"", "0+0 +00 00-"),
              new HallRotation("3x", "+00 00- 0+-"),
              new HallRotation("3y", "-0+ 0+0 -00"),
              new HallRotation("3z", "0-0 +-0 00+"),
              new HallRotation("3*", "00+ +00 0+0"),
              new HallRotation("4x", "+00 00- 0+0"),
              new HallRotation("4y", "00+ 0+0 -00"),
              new HallRotation("4z", "0-0 +00 00+"),
              new HallRotation("6x", "+00 0+- 0+0"),
              new HallRotation("6y", "00+ 0+0 -0+"),
              new HallRotation("6z", "+-0 +00 00+") }
          : hallRotationTerms);
    }
  }

  /**
   * A private class to process Hall translation information.
   */
  private final static class HallTranslation {

    protected char translationCode = '\0';
    protected int rotationOrder;
    protected int rotationShift12ths;
    protected P3i vectorShift12ths;

    protected HallTranslation(char translationCode, P3i params) {
      this.translationCode = translationCode;
      if (params != null) {
        if (params.z >= 0) {
          // just a shift
          vectorShift12ths = params;
          return;
        }
        // just a screw axis
        rotationOrder = params.x;
        rotationShift12ths = params.y;
      }
      vectorShift12ths = new P3i();
    }

    protected static int getLatticeIndex(char latt) {
      /*
       * returns lattice code (1-9, including S and T) for a given lattice type
       * 1-7 match SHELX codes
       * 
       */
      for (int i = 1, ipt = 3; i <= nLatticeTypes; i++, ipt += 3)
        if (latticeTranslationData[ipt].charAt(0) == latt)
          return i;
      return 0;
    }

    /**
     * 
     * @param latt
     *        SHELX index or character
     * @return lattice character P I R F A B C S T or \0
     * 
     */
    protected static char getLatticeCode(int latt) {
      if (latt < 0)
        latt = -latt;
      return (latt == 0 ? '\0'
          : latt > nLatticeTypes ? getLatticeCode(getLatticeIndex((char) latt))
              : latticeTranslationData[latt * 3].charAt(0));
    }

    protected static String getLatticeDesignation(int latt) {
      boolean isCentrosymmetric = (latt > 0);
      String str = (isCentrosymmetric ? "-" : "");
      if (latt < 0)
        latt = -latt;
      if (latt == 0 || latt > nLatticeTypes)
        return "";
      return str + getLatticeCode(latt) + ": "
          + (isCentrosymmetric ? "centrosymmetric " : "")
          + latticeTranslationData[latt * 3 + 1];
    }

    protected static String getLatticeDesignation2(char latticeCode,
                                               boolean isCentrosymmetric) {
      int latt = getLatticeIndex(latticeCode);
      if (!isCentrosymmetric)
        latt = -latt;
      return getLatticeDesignation(latt);
    }

    protected static String getLatticeExtension(char latt,
                                            boolean isCentrosymmetric) {
      /*
       * returns a set of rotation terms that are equivalent to the lattice code
       * 
       */
      for (int i = 1, ipt = 3; i <= nLatticeTypes; i++, ipt += 3)
        if (latticeTranslationData[ipt].charAt(0) == latt)
          return latticeTranslationData[ipt + 2]
              + (isCentrosymmetric ? " -1" : "");
      return "";
    }

    private final static String[] latticeTranslationData = { //
        "\0", "unknown",         "" //
        ,"P", "primitive",       "" //
        ,"I", "body-centered",   " 1n" //
        ,"R", "rhombohedral",    " 1r 1r" //
        ,"F", "face-centered",   " 1ab 1bc 1ac" //
        ,"A", "A-centered",      " 1bc" //
        ,"B", "B-centered",      " 1ac" //
        ,"C", "C-centered",      " 1ab" //
        ,"S", "rhombohedral(S)", " 1s 1s" //
        ,"T", "rhombohedral(T)", " 1t 1t" //
      };

    private final static int nLatticeTypes = latticeTranslationData.length / 3 - 1;

    private static HallTranslation[] hallTranslationTerms;

    private synchronized static HallTranslation[] getHallTerms() {
      return (hallTranslationTerms == null
          ? hallTranslationTerms = new HallTranslation[] { //
              new HallTranslation('a', P3i.new3(6, 0, 0)), //
              new HallTranslation('b', P3i.new3(0, 6, 0)), //
              new HallTranslation('c', P3i.new3(0, 0, 6)), //
              new HallTranslation('n', P3i.new3(6, 6, 6)), //
              new HallTranslation('u', P3i.new3(3, 0, 0)), //
              new HallTranslation('v', P3i.new3(0, 3, 0)), //
              new HallTranslation('w', P3i.new3(0, 0, 3)), //
              new HallTranslation('d', P3i.new3(3, 3, 3)), //
              new HallTranslation('1', P3i.new3(2, 6, -1)), //
              new HallTranslation('1', P3i.new3(3, 4, -1)), //
              new HallTranslation('2', P3i.new3(3, 8, -1)), //
              new HallTranslation('1', P3i.new3(4, 3, -1)), //
              new HallTranslation('3', P3i.new3(4, 9, -1)), //
              new HallTranslation('1', P3i.new3(6, 2, -1)), //
              new HallTranslation('2', P3i.new3(6, 4, -1)), //
              new HallTranslation('4', P3i.new3(6, 8, -1)), //
              new HallTranslation('5', P3i.new3(6, 10, -1)) //
              // extension to handle rhombohedral lattice as primitive
              , new HallTranslation('r', P3i.new3(4, 8, 8)), //
              new HallTranslation('s', P3i.new3(8, 8, 4)), //
              new HallTranslation('t', P3i.new3(8, 4, 8))  //
            } //
          : hallTranslationTerms);
    }

    static HallTranslation getHallTranslation(char translationCode, int order) {
      HallTranslation ht = null;
      for (int i = getHallTerms().length; --i >= 0;) {
        HallTranslation h = hallTranslationTerms[i];
        if (h.translationCode == translationCode) {
          if (h.rotationOrder == 0 || h.rotationOrder == order) {
            ht = new HallTranslation(translationCode, null);
            ht.translationCode = translationCode;
            ht.rotationShift12ths = h.rotationShift12ths;
            ht.vectorShift12ths = h.vectorShift12ths;
            return ht;
          }
        }
      }
      return ht;
    }
  }

  private String hallSymbol;
  private String primitiveHallSymbol;
  private char latticeCode = '\0';
  private String latticeExtension;
  private boolean isCentrosymmetric;
  private HallRotationTerm[] rotationTerms = new HallRotationTerm[16];

  protected int nRotations;
  protected P3i vector12ths;
  protected String vectorCode;

  HallInfo(String hallSymbol) {
    init(hallSymbol);
  }

  public int getRotationCount() {
    return nRotations;
  }

  public boolean isGenerated() {
    return nRotations > 0;
  }

  public char getLatticeCode() {
    return latticeCode;
  }

  public boolean isCentrosymmetric() {
    return isCentrosymmetric;
  }

  public String getHallSymbol() {
    return hallSymbol;
  }

  private void init(String hallSymbol) {
    try {
      String str = this.hallSymbol = hallSymbol.trim();
      str = extractLatticeInfo(str);
      if (HallTranslation.getLatticeIndex(latticeCode) == 0)
        return;
      latticeExtension = HallTranslation.getLatticeExtension(latticeCode,
          isCentrosymmetric);
      str = extractVectorInfo(str) + latticeExtension;
      if (Logger.debugging)
        Logger.debug("Hallinfo: " + hallSymbol + " " + str);
      int prevOrder = 0;
      char prevAxisType = '\0';
      primitiveHallSymbol = "P";
      while (str.length() > 0 && nRotations < 16) {
        str = extractRotationInfo(str, prevOrder, prevAxisType);
        HallRotationTerm r = rotationTerms[nRotations - 1];
        prevOrder = r.order;
        prevAxisType = r.axisType;
        primitiveHallSymbol += " " + r.primitiveCode;
      }
      primitiveHallSymbol += vectorCode;
    } catch (Exception e) {
      Logger.error("Invalid Hall symbol " + e);
      nRotations = 0;
    }
  }

  String dumpInfo() {
    SB sb = new SB();
    sb.append("\nHall symbol: ").append(hallSymbol)
        .append("\nprimitive Hall symbol: ").append(primitiveHallSymbol)
        .append("\nlattice type: ").append(getLatticeDesignation());
    for (int i = 0; i < nRotations; i++) {
      sb.append("\n\nrotation term ").appendI(i + 1)
          .append(rotationTerms[i].dumpInfo(vectorCode));
    }
    return sb.toString();
  }

  private String getLatticeDesignation() {
    return HallTranslation.getLatticeDesignation2(latticeCode,
        isCentrosymmetric);
  }

  private String extractLatticeInfo(String name) {
    int i = name.indexOf(" ");
    if (i < 0)
      return "";
    String term = name.substring(0, i).toUpperCase();
    latticeCode = term.charAt(0);
    if (latticeCode == '-') {
      isCentrosymmetric = true;
      latticeCode = term.charAt(1);
    }
    return name.substring(i + 1).trim();
  }

  private String extractVectorInfo(String name) {
    // (nx ny nz)  where n is 1/12 of the edge. 
    // also allows for (nz), though that is not standard
    vector12ths = new P3i();
    vectorCode = "";
    int i = name.indexOf("(");
    int j = name.indexOf(")", i);
    if (i > 0 && j > i) {
      String term = name.substring(i + 1, j);
      vectorCode = " (" + term + ")";
      name = name.substring(0, i).trim();
      i = term.indexOf(" ");
      if (i >= 0) {
        vector12ths.x = Integer.parseInt(term.substring(0, i));
        term = term.substring(i + 1).trim();
        i = term.indexOf(" ");
        if (i >= 0) {
          vector12ths.y = Integer.parseInt(term.substring(0, i));
          term = term.substring(i + 1).trim();
        }
      }
      vector12ths.z = Integer.parseInt(term);
    }
    return name;
  }

  private String extractRotationInfo(String name, int prevOrder,
                                     char prevAxisType) {
    int i = name.indexOf(" ");
    String code;
    if (i >= 0) {
      code = name.substring(0, i);
      name = name.substring(i + 1).trim();
    } else {
      code = name;
      name = "";
    }
    rotationTerms[nRotations] = new HallRotationTerm(this, code, prevOrder,
        prevAxisType);
    nRotations++;
    return name;
  }

  @Override
  public String toString() {
    return hallSymbol;
  }

  /**
   * A private class
   */
  private final static class HallRotationTerm {

    String primitiveCode;
    M4d seitzMatrix12ths = new M4d();
    int order;
    char axisType = '\0';

    private String inputCode;
    private String lookupCode;
    private String translationString;
    private HallRotation rotation;
    private HallTranslation translation;
    private boolean isImproper;
    private char diagonalReferenceAxis = '\0';

    private boolean allPositive = true; //for now

    HallRotationTerm(HallInfo hallInfo, String code, int prevOrder,
        char prevAxisType) {
      inputCode = code;
      code += "   ";
      if (code.charAt(0) == '-') {
        isImproper = true;
        code = code.substring(1);
      }
      primitiveCode = "";
      order = code.charAt(0) - '0';
      diagonalReferenceAxis = '\0';
      axisType = '\0';
      int ptr = 2; // pointing to "c" in 2xc
      char c;
      switch (c = code.charAt(1)) {
      case 'x':
      case 'y':
      case 'z':
        switch (code.charAt(2)) {
        case '\'':
        case '"':
          diagonalReferenceAxis = c;
          c = code.charAt(2);
          ptr++;
        }
        //$FALL-THROUGH$
      case '*':
        axisType = c;
        break;
      case '\'':
      case '"':
        axisType = c;
        switch (code.charAt(2)) {
        case 'x':
        case 'y':
        case 'z':
          diagonalReferenceAxis = code.charAt(2);
          ptr++;
          break;
        default:
          diagonalReferenceAxis = prevAxisType;
        }
        break;
      default:
        // implicit axis type
        axisType = (order == 1 ? '_'// no axis for 1
            : hallInfo.nRotations == 0 ? 'z' // z implied for first rotation
                : hallInfo.nRotations == 2 ? '*' // 3* implied for third rotation
                    : prevOrder == 2 || prevOrder == 4 ? 'x' // x implied for 2
                        // or 4
                        : '\'' // a-b (') implied for 3 or 6 previous
        );
        code = code.substring(0, 1) + axisType + code.substring(1);
      }
      primitiveCode += (axisType == '_' ? "1" : code.substring(0, 2));
      if (diagonalReferenceAxis != '\0') {
        // 2' needs x or y or z designation
        code = code.substring(0, 1) + diagonalReferenceAxis + axisType
            + code.substring(ptr);
        primitiveCode += diagonalReferenceAxis;
        ptr = 3;
      }
      lookupCode = code.substring(0, ptr);
      rotation = HallRotation.lookup(lookupCode);
      if (rotation == null) {
        Logger.error(
            "Rotation lookup could not find " + inputCode + " ? " + lookupCode);
        return;
      }

      // now for translational part 1 2 3 4 5 6 a b c n u v w d r
      // The "r" is my addition to handle rhombohedral lattice with
      // primitive notation. This made coding FAR simpler -- all lattice
      // operations indicated by one to three 1xxx or -1 extensions.

      translation = new HallTranslation('\0', null);
      translationString = "";
      int len = code.length();
      for (int i = ptr; i < len; i++) {
        char translationCode = code.charAt(i);
        HallTranslation t = HallTranslation.getHallTranslation(translationCode,
            order);
        if (t != null) {
          translationString += "" + t.translationCode;
          translation.rotationShift12ths += t.rotationShift12ths;
          translation.vectorShift12ths.add(t.vectorShift12ths);
        }
      }
      primitiveCode = (isImproper ? "-" : "") + primitiveCode
          + translationString;

      // set matrix, including translations and vector adjustment

      seitzMatrix12ths
          .setM4(isImproper ? rotation.seitzMatrixInv : rotation.seitzMatrix);
      seitzMatrix12ths.m03 = translation.vectorShift12ths.x;
      seitzMatrix12ths.m13 = translation.vectorShift12ths.y;
      seitzMatrix12ths.m23 = translation.vectorShift12ths.z;
      switch (axisType) {
      case 'x':
        seitzMatrix12ths.m03 += translation.rotationShift12ths;
        break;
      case 'y':
        seitzMatrix12ths.m13 += translation.rotationShift12ths;
        break;
      case 'z':
        seitzMatrix12ths.m23 += translation.rotationShift12ths;
        break;
      }

      if (hallInfo.vectorCode.length() > 0) {
        M4d m1 = M4d.newM4(null);
        M4d m2 = M4d.newM4(null);
        P3i v = hallInfo.vector12ths;
        m1.m03 = v.x;
        m1.m13 = v.y;
        m1.m23 = v.z;
        m2.m03 = -v.x;
        m2.m13 = -v.y;
        m2.m23 = -v.z;
        seitzMatrix12ths.mul2(m1, seitzMatrix12ths);
        seitzMatrix12ths.mul(m2);
      }
      if (Logger.debugging) {
        Logger.debug("code = " + code + "; primitive code =" + primitiveCode
            + "\n Seitz Matrix(12ths):" + seitzMatrix12ths);
      }
    }

    String dumpInfo(String vectorCode) {
      SB sb = new SB();
      sb.append("\ninput code: ").append(inputCode).append("; primitive code: ")
          .append(primitiveCode).append("\norder: ").appendI(order)
          .append(isImproper ? " (improper axis)" : "");
      if (axisType != '_') {
        sb.append("; axisType: ").appendC(axisType);
        if (diagonalReferenceAxis != '\0')
          sb.appendC(diagonalReferenceAxis);
      }
      if (translationString.length() > 0)
        sb.append("; translation: ").append(translationString);
      if (vectorCode.length() > 0)
        sb.append("; vector offset: ").append(vectorCode);
      if (rotation != null)
        sb.append("\noperator: ").append(getXYZ(allPositive))
            .append("\nSeitz matrix:\n")
            .append(SymmetryOperation.dumpSeitz(seitzMatrix12ths, false));
      return sb.toString();
    }

    String getXYZ(boolean allPositive) {
      return SymmetryOperation.getXYZFromMatrix(seitzMatrix12ths, true,
          allPositive, true);
    }
  }

  public void generateAllOperators(HallReceiver sg) {
    M4d mat1 = new M4d();
    M4d operation = new M4d();
    M4d[] newOps = new M4d[7];
    for (int i = 0; i < 7; i++)
      newOps[i] = new M4d();
    int nOps = sg.getMatrixOperationCount();
    for (int i = 0; i < nRotations; i++) {
      HallRotationTerm rt = rotationTerms[i];
      mat1.setM4(rt.seitzMatrix12ths);
      int nRot = rt.order;
      // this would iterate int nOps = operationCount;
      newOps[0].setIdentity();
      for (int j = 1; j <= nRot; j++) {
        M4d m = newOps[j];
        m.mul2(mat1, newOps[0]);
        newOps[0].setM4(m);
        int nNew = 0;
        for (int k = 0; k < nOps; k++) {
          operation.mul2(m, sg.getMatrixOperation(k));
          // normalize 12ths
          operation.m03 = ((int) operation.m03 + 12) % 12;
          operation.m13 = ((int) operation.m13 + 12) % 12;
          operation.m23 = ((int) operation.m23 + 12) % 12;
          if (sg.addHallOperationCheckDuplicates(operation)) {
            nNew++;
          }
        }
        nOps += nNew;
      }
    }
  }

  /**
   * 
   * @param shellXLATTCode
   * @return Hall equivalent of SHELX LATT value
   */
  static String getHallLatticeEquivalent(int shellXLATTCode) {
    char latticeCode = HallTranslation.getLatticeCode(shellXLATTCode);
    boolean isCentrosymmetric = (shellXLATTCode > 0);
    return (isCentrosymmetric ? "-" : "") + latticeCode + " 1";
  }

  static String getLatticeDesignation(int latt) {
    return HallTranslation.getLatticeDesignation(latt);
  }

  public static int getLatticeIndex(char latticeCode) {
    return HallTranslation.getLatticeIndex(latticeCode);
  }

  public static int getLatticeIndexFromCode(int latticeParameter) {
    return getLatticeIndex(HallTranslation.getLatticeCode(latticeParameter));
  }

}
