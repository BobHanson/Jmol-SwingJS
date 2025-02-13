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

  boolean isCurrentCell = true;
  private String displayName;

  boolean coordinatesAreFractional;
  boolean isMultiCell;
  String sgName, sgTitle;
  SymmetryOperation[] symmetryOperations;
  SymmetryOperation[] additionalOperations;
  String infoStr;
  int[] cellRange;
  char latticeType = 'P';
  String intlTableNo;
  /**
   * actually just the Jmol id, such as 3:abc
   */
  String intlTableJmolId;
  private int spaceGroupIndex;

  private String spaceGroupF2CTitle;
  double[][] spaceGroupF2C;
  double[] spaceGroupF2CParams;
  /**
   * actual LOAD ... SUPERCELL a,b,c designation
   * 
   */
  protected String strSUPERCELL;
  String intlTableIndexNdotM;
  String intlTableTransform;
  
  SymmetryInfo() {    
  }
  
  void setSymmetryInfoFromModelkit(SpaceGroup sg) {
    // from Symmetry.getSymmetryInfoStr
    cellRange = null;
    sgName = sg.getName();
    intlTableJmolId = sg.jmolId;
    intlTableNo = sg.itaNumber;
    latticeType = sg.latticeType;
    symmetryOperations = sg.finalOperations;
    coordinatesAreFractional = true;
    setInfo(sg.getOperationCount());
  }
  
  /**
   * 
   * @param modelInfo
   *        from file reader; will be nonnull only when sg is null
   * @param unitCellParams
   *        an array of parameters could be from model, but also could be from a
   *        trajectory listing
   * @return actual unit cell parameters
   */
  double[] setSymmetryInfoFromFile(Map<String, Object> modelInfo,
                                   double[] unitCellParams) {
    // from ModelAdapter only
    spaceGroupIndex = ((Integer) modelInfo.remove(JC.INFO_SPACE_GROUP_INDEX))
        .intValue();
    //we need to be passing the unit cell that matches the symmetry
    //in the file -- primitive or otherwise -- 
    //then convert it here to the right multiple.
    cellRange = (int[]) modelInfo.remove(JC.INFO_UNIT_CELL_RANGE);
    sgName = (String) modelInfo.get(JC.INFO_SPACE_GROUP);
    spaceGroupF2C = (double[][]) modelInfo.remove("f2c");
    spaceGroupF2CTitle = (String) modelInfo.remove(JC.INFO_SPACE_GROUP_F2C_TITLE);
    spaceGroupF2CParams = (double[]) modelInfo.remove("f2cParams");
    sgTitle = (String) modelInfo.remove(JC.INFO_SPACE_GROUP_TITLE);
    strSUPERCELL = (String) modelInfo.remove("supercell");
    if (sgName == null || sgName == "")
      sgName = "spacegroup unspecified";
    intlTableNo = (String) modelInfo.get("intlTableNo");
    intlTableIndexNdotM = (String) modelInfo.get("intlTableIndex");
    intlTableTransform = (String) modelInfo.get("intlTableTransform");
    intlTableJmolId = (String) modelInfo.remove("intlTableJmolId");
    String s = (String) modelInfo.get("latticeType");
    latticeType = (s == null ? 'P' : s.charAt(0));
    symmetryOperations = (SymmetryOperation[]) modelInfo.remove(JC.INFO_SYMOPS_TEMP);
    coordinatesAreFractional = modelInfo.containsKey("coordinatesAreFractional")
        ? ((Boolean) modelInfo.get("coordinatesAreFractional")).booleanValue()
        : false;
    isMultiCell = (coordinatesAreFractional && symmetryOperations != null);
    if (unitCellParams == null)
      unitCellParams = (double[]) modelInfo.get(JC.INFO_UNIT_CELL_PARAMS);
    unitCellParams = (SimpleUnitCell.isValid(unitCellParams) ? unitCellParams
        : null);
    if (unitCellParams == null) {
      coordinatesAreFractional = false;
      symmetryOperations = null;
      cellRange = null;
      infoStr = "";
      modelInfo.remove(JC.INFO_UNIT_CELL_PARAMS);
    }
    int symmetryCount = modelInfo.containsKey("symmetryCount")
        ? ((Integer) modelInfo.get("symmetryCount")).intValue()
        : 0;
    setInfo(symmetryCount);
    return unitCellParams;
  }
  
  private void setInfo(int symmetryCount) {
    String info = "Spacegroup: " + sgName;
    if (symmetryOperations != null) {
      String c = "";
      String s = "\nNumber of symmetry operations: "
          + (symmetryCount == 0 ? 1 : symmetryCount) + "\nSymmetry Operations:";
      for (int i = 0; i < symmetryCount; i++) {
        SymmetryOperation op = symmetryOperations[i];
        s += "\n" + op.fixMagneticXYZ(op, op.xyz);
        // TODO magnetic centering
        if (op.isCenteringOp)
          c += " ("
              + PT.rep(PT.replaceAllCharacters(op.xyz, "xyz", "0"), "0+", "")
              + ")";
      }
      if (c.length() > 0)
        info += "\nCentering: " + c;
      info += s;
      info += "\n";
    }
    this.infoStr = info;
  }


  SymmetryOperation[] getAdditionalOperations() {
    if (additionalOperations == null && symmetryOperations != null) {
      additionalOperations = SymmetryOperation.getAdditionalOperations(symmetryOperations, 0x73);
    }
    return additionalOperations;
  }

  private SpaceGroup sgDerived;
  
  SpaceGroup getDerivedSpaceGroup() {
    if (sgDerived == null) {
      sgDerived = SpaceGroup.getSpaceGroupFromIndex(spaceGroupIndex);
    }
    return sgDerived;
  }

  boolean setIsCurrentCell(boolean TF) {
    return (isCurrentCell != TF && (isCurrentCell = TF) == true);
  }


  String getSpaceGroupTitle() {
    return (isCurrentCell && spaceGroupF2CTitle != null ? spaceGroupF2CTitle 
      : sgName.startsWith("cell=") ? sgName : sgTitle);
  }


  String getDisplayName(Symmetry sym) {
    if (displayName == null) {
      boolean isPolymer = sym.isPolymer();
      boolean isSlab = sym.isSlab();
      String sgName = (isPolymer ? "polymer"
          : isSlab ? "slab" : getSpaceGroupTitle());
      if (sgName == null)
        return null;
      if (sgName.startsWith("cell=!"))
        sgName = "cell=inverse[" + sgName.substring(6) + "]";
      sgName = PT.rep(sgName, ";0,0,0", "");
      if (sgName.indexOf("#") < 0) {
        String trm = intlTableTransform;
        String intTab = intlTableIndexNdotM;
        if (!isSlab && !isPolymer && intTab != null) {
          if (trm != null) {
            int pt = sgName.indexOf(trm);
            if (pt >= 0) {
              sgName = PT.rep(sgName, "(" + trm + ")", "");
            }
            if (intTab.indexOf(trm) < 0) {
              pt = intTab.indexOf(".");
              if (pt > 0)
                intTab = intTab.substring(0, pt);
              intTab += ":" + trm;
            }
          }
          sgName = (sgName.startsWith("0") ? "" : sgName.equals("unspecified!") ? "#" : sgName + " #") + intTab;
        }
      }
      if (sgName.indexOf(SpaceGroup.NO_NAME) >= 0)
        sgName = "";
      displayName = sgName;
    }
    return displayName;
  }

  private String itaNo; 
  
  public String getClegId() {
    if (itaNo == null) {
      itaNo = intlTableIndexNdotM;
      if (itaNo == null || itaNo.indexOf(":") > 0)
        return itaNo;
      int pt = itaNo.indexOf(".");
      itaNo = (pt > 0 ? itaNo.substring(0, pt) : itaNo) + ":" + intlTableTransform;
    }
    return itaNo;
  }


}

