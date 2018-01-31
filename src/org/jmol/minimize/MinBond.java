/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-11-23 12:49:25 -0600 (Fri, 23 Nov 2007) $
 * $Revision: 8655 $
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

package org.jmol.minimize;

public class MinBond extends MinObject {
  public int rawIndex;
  public int index;
  public int order;
  public boolean isAromatic; // never set?
  public boolean isAmide;    // never set?
  
  MinBond(int rawIndex, int index, int atomIndex1, int atomIndex2, int order, int type, Integer key) {
    this.rawIndex = rawIndex;
    this.index = index;
    this.type = type;
    data = new int[] { atomIndex1, atomIndex2 };
    this.order = order;
    this.key = key;
  }
  
  public int getOtherAtom(int index) {
    return data[data[0] == index ? 1 : 0];    
  }
}
