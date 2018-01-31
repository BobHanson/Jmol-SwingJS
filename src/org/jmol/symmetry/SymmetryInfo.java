/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-05-18 08:19:45 -0500 (Fri, 18 May 2007) $
 * $Revision: 7742 $

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

import javajs.util.PT;

import org.jmol.util.SimpleUnitCell;

import java.util.Map;

class SymmetryInfo {

  boolean coordinatesAreFractional;
  boolean isMultiCell;
  String sgName;
  SymmetryOperation[] symmetryOperations;
  String infoStr;
  int[] cellRange;
  String latticeType = "P";
  public String intlTableNo;

  SymmetryInfo() {    
  }
  
  
  /**
   * 
   * @param info
   * @param unitCellParams
   *        an array of parameters could be from model, but also could be from a
   *        trajectory listing
   * @return actual unit cell parameters
   */
  float[] setSymmetryInfo(Map<String, Object> info, float[] unitCellParams) {
    cellRange = (int[]) info.get("unitCellRange");
    sgName = (String) info.get("spaceGroup");
    if (sgName == null || sgName == "")
      sgName = "spacegroup unspecified";
    infoStr = "Spacegroup: " + sgName;
    if ((latticeType = (String) info.get("latticeType")) == null)
      latticeType = "P";
    intlTableNo = (String) info.get("intlTableNo");
    int symmetryCount = info.containsKey("symmetryCount") ? ((Integer) info
        .get("symmetryCount")).intValue() : 0;
    symmetryOperations = (SymmetryOperation[]) info.remove("symmetryOps");
    if (symmetryOperations != null) {
      String c = "";
      String s = "\nNumber of symmetry operations: "
          + (symmetryCount == 0 ? 1 : symmetryCount) + "\nSymmetry Operations:";
      for (int i = 0; i < symmetryCount; i++) {
        SymmetryOperation op = symmetryOperations[i];
        s += "\n" + op.fixMagneticXYZ(op, op.xyz, true);
        // TODO magnetic centering
        if (op.isCenteringOp)
          c += " ("
              + PT.rep(PT.replaceAllCharacters(op.xyz, "xyz", "0"), "0+", "")
              + ")";
      }
      if (c.length() > 0)
        infoStr += "\nCentering: " + c;
      infoStr += s;
      infoStr += "\n";
    }
    if (unitCellParams == null)
      unitCellParams = (float[]) info.get("unitCellParams");
    if (!SimpleUnitCell.isValid(unitCellParams))
      return null;
    coordinatesAreFractional = info.containsKey("coordinatesAreFractional") ? ((Boolean) info
        .get("coordinatesAreFractional")).booleanValue() : false;
    isMultiCell = (coordinatesAreFractional && symmetryOperations != null);
    return unitCellParams;
  }
}

