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

import java.util.Map;

import org.jmol.util.SimpleUnitCell;
import org.jmol.viewer.JC;

import javajs.util.PT;

class SymmetryInfo {

  boolean coordinatesAreFractional;
  boolean isMultiCell;
  String sgName, sgTitle;
  SymmetryOperation[] symmetryOperations;
  SymmetryOperation[] additionalOperations;
  String infoStr;
  int[] cellRange;
  char latticeType = 'P';
  public String intlTableNo;
  /**
   * actually just the Jmol id, such as 3:abc
   */
  public String intlTableJmolID;
  private int spaceGroupIndex;

  double[][] spaceGroupF2C;
  private String spaceGroupF2CTitle;
  double[] spaceGroupF2CParams;
  /**
   * actual LOAD ... SUPERCELL a,b,c designation
   * 
   */
  protected String strSUPERCELL;
  String intlTableIndex;
  String intlTableTransform;
  
  SymmetryInfo() {    
  }
  
  
  /**
   * 
   * @param modelInfo from file reader; will be nonnull only when sg is null
   * @param unitCellParams
   *        an array of parameters could be from model, but also could be from a
   *        trajectory listing
   * @param sg space group determined by SpaceGroupFinder via modelkit
   * @return actual unit cell parameters
   */
  double[] setSymmetryInfo(Map<String, Object> modelInfo, double[] unitCellParams,
                          SpaceGroup sg) {
    int symmetryCount;
    if (sg == null) {
      // from ModelAdapter only
      spaceGroupIndex = ((Integer)modelInfo.remove(JC.INFO_SPACE_GROUP_INDEX)).intValue();
      //we need to be passing the unit cell that matches the symmetry
      //in the file -- primitive or otherwise -- 
      //then convert it here to the right multiple.
      cellRange = (int[]) modelInfo.remove(JC.INFO_UNIT_CELL_RANGE);
      sgName = (String) modelInfo.get(JC.INFO_SPACE_GROUP);
      spaceGroupF2C = (double[][]) modelInfo.remove("f2c");
      spaceGroupF2CTitle = (String) modelInfo.remove("f2cTitle");
      spaceGroupF2CParams = (double[]) modelInfo.remove("f2cParams");
      sgTitle = (String) modelInfo.remove(JC.INFO_SPACE_GROUP_TITLE);
      strSUPERCELL = (String) modelInfo.remove("supercell"); 
      if (sgName == null || sgName == "")
        sgName = "spacegroup unspecified";
      intlTableNo = (String) modelInfo.get("intlTableNo");
      intlTableIndex = (String) modelInfo.get("intlTableIndex");
      intlTableTransform = (String) modelInfo.get("intlTableTransform");
      intlTableJmolID = (String) modelInfo.remove("intlTableJmolID");
      String s = (String) modelInfo.get("latticeType");
        latticeType = (s == null ? 'P' : s.charAt(0));
      symmetryCount = modelInfo.containsKey("symmetryCount")
          ? ((Integer) modelInfo.get("symmetryCount")).intValue()
          : 0;
      symmetryOperations = (SymmetryOperation[]) modelInfo.remove("symmetryOps");
      coordinatesAreFractional = modelInfo.containsKey("coordinatesAreFractional")
          ? ((Boolean) modelInfo.get("coordinatesAreFractional")).booleanValue()
          : false;
      isMultiCell = (coordinatesAreFractional && symmetryOperations != null);
      infoStr = "Spacegroup: " + sgName;
    } else {
      // from Symmetry.getSymmetryInfoStr
      // from ModelKit
      cellRange = null;
      sgName = sg.getName();
      intlTableJmolID = sg.jmolId;
      intlTableNo = sg.itaNumber;
      latticeType = sg.latticeType;
      symmetryCount = sg.getOperationCount();
      symmetryOperations = sg.finalOperations;
      coordinatesAreFractional = true;
      infoStr = "Spacegroup: " + sgName;
    }
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
      unitCellParams = (double[]) modelInfo.get(JC.INFO_UNIT_CELL_PARAMS);
    unitCellParams = (SimpleUnitCell.isValid(unitCellParams) ? unitCellParams : null);
    if (unitCellParams == null) {
      coordinatesAreFractional = false;
      symmetryOperations = null;
      cellRange = null;
      infoStr = "";
      modelInfo.remove(JC.INFO_UNIT_CELL_PARAMS);
    }
    return unitCellParams;
  }

  public SymmetryOperation[] getAdditionalOperations() {
    if (additionalOperations == null && symmetryOperations != null) {
      additionalOperations = SymmetryOperation.getAdditionalOperations(symmetryOperations);
    }
    return additionalOperations;
  }

  private SpaceGroup sgDerived;
  
  public SpaceGroup getDerivedSpaceGroup() {
    if (sgDerived == null) {
      sgDerived = SpaceGroup.getSpaceGroupFromIndex(spaceGroupIndex);
    }
    return sgDerived;
  }

  boolean isActive = true;
  String displayName;

  public boolean setIsActiveCell(boolean TF) {
    return (isActive != TF && (isActive = TF) == true);
  }


  public String getSpaceGroupTitle() {
    return (isActive && spaceGroupF2CTitle != null ? spaceGroupF2CTitle 
      : sgName.startsWith("cell=") ? sgName : sgTitle);
  }


}

