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

package org.jmol.modelset;


import javajs.util.BS;
import org.jmol.util.Edge;

class BondIteratorSelected implements BondIterator {

  private Bond[] bonds;
  private int bondCount;
  private int bondType;
  private int iBond;
  private BS bsSelected;
  private boolean bondSelectionModeOr;

  BondIteratorSelected(Bond[] bonds, int bondCount, int bondType,
      BS bsSelected, boolean bondSelectionModeOr) {
    this.bonds = bonds;
    this.bondCount = bondCount;
    this.bondType = bondType;
    this.bsSelected = bsSelected;
    this.bondSelectionModeOr = bondSelectionModeOr;
  }

 
  @Override
  public boolean hasNext() {
    if (bondType == Edge.BOND_ORDER_NULL) {
      iBond = bsSelected.nextSetBit(iBond);
      return (iBond >= 0 && iBond < bondCount);
    }
    for (; iBond < bondCount; ++iBond) {
      Bond bond = bonds[iBond];
      if (bond == null || bondType != Edge.BOND_ORDER_ANY
          && (bond.order & bondType) == 0) {
        continue;
      } else if (bondType == Edge.BOND_ORDER_ANY
          && bond.order == Edge.BOND_STRUT)
        continue;
      boolean isSelected1 = bsSelected.get(bond.atom1.i);
      boolean isSelected2 = bsSelected.get(bond.atom2.i);
      if ((!bondSelectionModeOr && isSelected1 && isSelected2)
          || (bondSelectionModeOr && (isSelected1 || isSelected2)))
        return true;
    }
    return false;
  }

 
  @Override
  public int nextIndex() {
    return iBond;
  }

  
  @Override
  public Bond next() {
    return bonds[iBond++];
  }
}
