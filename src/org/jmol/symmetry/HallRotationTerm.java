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

package org.jmol.symmetry;

import org.jmol.util.Logger;

import javajs.util.SB;
import javajs.util.M4;
import javajs.util.P3i;

class HallRotationTerm {
  
  String inputCode;
  String primitiveCode;
  String lookupCode;
  String translationString;
  HallRotation rotation;
  HallTranslation translation;
  M4 seitzMatrix12ths = new M4();
  boolean isImproper;
  int order;
  char axisType = '\0';
  char diagonalReferenceAxis = '\0';
  
  boolean allPositive = true; //for now

  HallRotationTerm(HallInfo hallInfo, String code, int prevOrder, char prevAxisType) {
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
      Logger.error("Rotation lookup could not find " + inputCode + " ? "
          + lookupCode);
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
      HallTranslation t = HallTranslation.getHallTranslation(translationCode, order);
      if (t != null) {
        translationString += "" + t.translationCode;
        translation.rotationShift12ths += t.rotationShift12ths;
        translation.vectorShift12ths.add(t.vectorShift12ths);
      }
    }
    primitiveCode = (isImproper ? "-" : "") + primitiveCode
        + translationString;

    // set matrix, including translations and vector adjustment

    seitzMatrix12ths.setM4(isImproper ? rotation.seitzMatrixInv : rotation.seitzMatrix);
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
      M4 m1 = M4.newM4(null);
      M4 m2 = M4.newM4(null);
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
    SB sb= new SB();
    sb.append("\ninput code: ")
         .append(inputCode).append("; primitive code: ").append(primitiveCode)
         .append("\norder: ").appendI(order).append(isImproper ? " (improper axis)" : "");
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
      sb.append("\noperator: ").append(getXYZ(allPositive)).append("\nSeitz matrix:\n")
          .append(SymmetryOperation.dumpSeitz(seitzMatrix12ths, false));
    return sb.toString();
  }
  
 String getXYZ(boolean allPositive) {
   return SymmetryOperation.getXYZFromMatrix(seitzMatrix12ths, true, allPositive, true);
 }

}