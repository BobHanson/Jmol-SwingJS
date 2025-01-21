/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2017-05-27 15:31:36 -0500 (Sat, 27 May 2017) $
 * $Revision: 21620 $

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

package org.jmol.modelset;

import org.jmol.util.C;
import org.jmol.util.Edge;
import org.jmol.util.SimpleNode;
import org.jmol.viewer.JC;

public class Bond extends Edge {

  public final static int myVisibilityFlag = JC.getShapeVisibilityFlag(JC.SHAPE_STICKS);

  public Atom atom1;
  public Atom atom2;

  public short mad; 
  public short colix;

  public int shapeVisibilityFlags;
  
  /**
   * @param atom1
   * @param atom2
   * @param order
   * @param mad
   * @param colix
   */
  public Bond(Atom atom1, Atom atom2, int order,
              short mad, short colix) {
    this.atom1 = atom1;
    this.atom2 = atom2;
    this.colix = colix;
    setOrder(order);
    setMad(mad);
  }

  public void setMad(short mad) {
    this.mad = mad;
    setShapeVisibility(mad != 0);
  }

  void setShapeVisibility(boolean isVisible) {
    boolean wasVisible = ((shapeVisibilityFlags & myVisibilityFlag) != 0);
    if (wasVisible == isVisible)
      return;
    atom1.addDisplayedBond(myVisibilityFlag, isVisible);
    atom2.addDisplayedBond(myVisibilityFlag, isVisible);
    if (isVisible)
      shapeVisibilityFlags |= myVisibilityFlag;
    else
      shapeVisibilityFlags &= ~myVisibilityFlag;
  }
            
  
  public String getIdentity() {
    return (index + 1) + " "+ Edge.getBondOrderNumberFromOrder(order) + " " + atom1.getInfo() + " -- "
        + atom2.getInfo() + " " + atom1.distance(atom2);
  }

  @Override
  public boolean isCovalent() {
    return (order & BOND_COVALENT_MASK) != 0;
  }

  @Override
  public boolean isHydrogen() {
    return isOrderH(order);
  }

  public boolean isAromatic() {
    return (order & BOND_AROMATIC_MASK) != 0;
  }

  public double getEnergy() {
    // hbonds only
    return 0;
  }
  
  public int getValence() {
    return (!isCovalent() || order == BOND_PARTIAL01 ? 0
        : is(BOND_AROMATIC) ? 1
        : order & 7);
  }

  void deleteAtomReferences() {
    if (atom1 != null)
      atom1.deleteBond(this);
    if (atom2 != null)
      atom2.deleteBond(this);
    atom1 = atom2 = null;
  }

  public void setTranslucent(boolean isTranslucent, double translucentLevel) {
    colix = C.getColixTranslucent3(colix, isTranslucent, translucentLevel);
  }
  
  public void setOrder(int order) {
    if (atom1.getElementNumber() == 16 && atom2.getElementNumber() == 16)
      order |= BOND_SULFUR_MASK;
    if (order == BOND_AROMATIC_MASK)
      order = BOND_AROMATIC;
    this.order = order | (this.order & BOND_NEW);
  }

  @Override
  public int getAtomIndex1() {
    // required for Smiles parser
    return atom1.i;
  }
  
  @Override
  public int getAtomIndex2() {
    // required for Smiles parser
    return atom2.i;
  }
  
  @Override
  public int getCovalentOrder() {
    return Edge.getCovalentBondOrder(order);
  }

  public Atom getOtherAtom(Atom thisAtom) {
    return (atom1 == thisAtom ? atom2 : atom2 == thisAtom ? atom1 : null);
  }
  
  public boolean is(int bondType) {
    return (order & BOND_RENDER_MASK) == bondType;
  }

  @Override
  public SimpleNode getOtherNode(SimpleNode thisAtom) {
    return (atom1 == thisAtom ? atom2 : atom2 == thisAtom || thisAtom == null ? atom1 : null);
  }
  
  public boolean setAtropisomerOptions() {
    if (getCovalentOrder() > 1)
      return false;
    int i1, i2 = Integer.MAX_VALUE;
    Bond[] bonds = atom1.bonds;
    for (i1 = 0; i1 < bonds.length; i1++) {
      Atom a = bonds[i1].getOtherAtom(atom1);
      if (a != atom2)
        break;
    }
    if (i1 < bonds.length) {
      bonds = atom2.bonds;
      for (i2 = 0; i2 < bonds.length; i2++) {
        Atom a = bonds[i2].getOtherAtom(atom2);
        if (a != atom1)
          break;
      }
    }
    if (i1 > 2 || i2 >= bonds.length || i2 > 2) {
      return false;
    }
    order = getAtropismOrder(i1 + 1, i2 + 1);
    return true;    
  }

  /**
   * Not implemented.
   * 
   * @param doCalculate 
   * 
   * @return "" or "Z" or "E"
   */
  @Override
  public String getCIPChirality(boolean doCalculate) {
//    int flags = (order & BOND_CIP_STEREO_MASK) >> BOND_CIP_STEREO_SHIFT;
//    if (flags == 0 && getCovalentOrder() == 2 && doCalculate) {
//      flags = atom1.group.chain.model.ms.getBondCIPChirality(this);
//      order |= ((flags == 0 ? 3 : flags) << BOND_CIP_STEREO_SHIFT);
//    }
//    switch (flags) {
//    case BOND_CIP_STEREO_E:
//      return "E";
//    case BOND_CIP_STEREO_Z:
//      return "Z";
//    default:
      return "";
//    }
  }
  
  /**
    * Not implemented.
   * 
   * @param c [0:unknown; 1: Z; 2: E; 3: none]
   */
  @Override
  public void setCIPChirality(int c) {
//    order = (byte)((order & ~BOND_CIP_STEREO_MASK) 
//        | (c << BOND_CIP_STEREO_SHIFT));
  }



  @Override
  public String toString() {
    return atom1 + " - " + atom2;
  }

  @Override
  public SimpleNode getAtom(int i) {
    return (i == 1 ? atom2 : atom1);
  }

  public boolean isCovalentNotPartial0() {
    return ((order & Edge.BOND_COVALENT_MASK) != 0
        && order != Edge.BOND_PARTIAL01);
  }

}
