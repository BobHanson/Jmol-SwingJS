/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-26 16:57:51 -0500 (Thu, 26 Apr 2007) $
 * $Revision: 7502 $
 *
 * Copyright (C) 2005  The Jmol Development Team
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
 *3
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.util;

import javajs.util.Lst;
import javajs.util.P3;

import javajs.util.BS;

public interface Node extends SimpleNode {
  
  // abstracts out the essential pieces for SMILES processing
  
  public int getAtomicAndIsotopeNumber();
  @Override
  public String getAtomName();
  public int getAtomSite();
  public int getBondedAtomIndex(int j);
  @Override
  public int getCovalentBondCount();
  public int getCovalentHydrogenCount();
  @Override
  public Edge[] getEdges();
  @Override
  public int getElementNumber();
  @Override
  public int getFormalCharge();
  @Override
  public int getIndex();
  @Override
  public int getIsotopeNumber();
  @Override
  public int getValence();
  public void set(float x, float y, float z);
  public int getMoleculeNumber(boolean inModel);
  @Override
  public float getMass();

  
  /**
   * @param property  "property_xxxx"
   * @return value or Float.NaN
   */

  public float getFloatProperty(String property);

  // abstracts out the essential pieces for SMARTS processing
  
  public BS findAtomsLike(String substring);
  public String getAtomType();
  public int getModelIndex();
  public int getAtomNumber();
  /**
   * can be > 0 for PDB model with no H atoms or for SMILES string CCC
   * 
   * @return number of missing H atoms
   */
  public int getImplicitHydrogenCount();
  /**
   *  includes actual + missing
   * @return actual + missing
   */
  public int getCovalentBondCountPlusMissingH();
  int getTotalHydrogenCount();
  public int getTotalValence();

  String getCIPChirality(boolean doCalculate);
  int getCIPChiralityCode();
  @Override
  void setCIPChirality(int c);
  @Override
  public P3 getXYZ();
  public boolean modelIsRawPDB();


  // BIOSMILES/BIOSMARTS
  
  public String getBioStructureTypeName();
  public String getGroup1(char c0);
  public String getGroup3(boolean allowNull);
  public int getResno();
  public char getInsertionCode();
  public int getChainID();
  public String getChainIDStr();
  public int getOffsetResidueAtom(String name, int offset);
  public boolean getCrossLinkVector(Lst<Integer> vReturn, boolean crosslinkCovalent, boolean crosslinkHBond);
  public void getGroupBits(BS bs);
  public boolean isLeadAtom();
  public boolean isCrossLinked(Node node);
  public boolean isPurine();
  public boolean isPyrimidine();
  public boolean isDeleted();
  char getBioSmilesType();
}
