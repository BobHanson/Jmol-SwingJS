/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-02-03 09:17:41 -0600 (Wed, 03 Feb 2016) $
 * $Revision: 20942 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
package org.jmol.modelset;

import org.jmol.java.BS;

/**
 * A Model is a collection of Chains of Groups of Atoms.
 * Chains hold overall information relating to a Monomer, 
 * particularly whether this monomer is RNA or DNA.
 * 
 */
public final class Chain implements Structure {

  public Model model;
  /**
   * chainID is either the integer form of a single character or a pointer into 
   * a map held in Viewer that allows retrieval of a longer string
   * 
   */
  public int chainID;
  /**
   * chainNo is for information purposes only; retrieved by {atoms}.chainNo 
   */
  public int chainNo;
  
  /**
   * Groups form the essence of what a Chain is.
   * This number will be 0 if there is no chain designation in the PDB or CIF file
   * or when the file is not of a type that would have chain designations.
   * 
   */
  public Group[] groups;
  public int groupCount;
  
  /**
   * Calculated just prior to coloring by group
   * so that the range is appropriate for each chain.
   */
  public int selectedGroupCount;

  Chain(Model model, int chainID, int chainNo) {
    this.model = model;
    this.chainID = chainID;
    this.chainNo = chainNo;
    groups = new Group[16];
  }

  /**
   * 
   * @return actual string form of the chain identifier
   */
  public String getIDStr() {
    return (chainID == 0 ? "" : chainID < 256 ? "" + (char) chainID : (String) model.ms.vwr.getChainIDStr(chainID));
  }

  /**
   * prior to coloring by group, we need the chain count per chain that is
   * selected
   * 
   * @param bsSelected
   */
  void calcSelectedGroupsCount(BS bsSelected) {
    selectedGroupCount = 0;
    for (int i = 0; i < groupCount; i++)
      groups[i].selectedIndex = (groups[i].isSelected(bsSelected) ? selectedGroupCount++
          : -1);
  }

  void fixIndices(int atomsDeleted, BS bsDeleted) {
    for (int i = 0; i < groupCount; i++)
      groups[i].fixIndices(atomsDeleted, bsDeleted);
  }

  @Override
  public void setAtomBits(BS bs) {
    for (int i = 0; i < groupCount; i++)
      groups[i].setAtomBits(bs);
  }

  @Override
  public void setAtomBitsAndClear(BS bs, BS bsOut) {
    for (int i = 0; i < groupCount; i++)
      groups[i].setAtomBitsAndClear(bs, bsOut);
  }

}
