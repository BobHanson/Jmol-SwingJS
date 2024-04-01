/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.quantum;

public class SlaterData {

  public boolean isCore;
  public int atomNo;
  public int elemNo;
  public int x;
  public int y;
  public int z;
  public int r;
  public double zeta;
  public double coef;
  public int index;
      
  public SlaterData(int iAtom, int x, int y, int z, int r, double zeta, double coef) {
    this.atomNo = iAtom; // 1-based
    this.x = x;
    this.y = y;
    this.z = z;
    this.r = r;
    this.zeta = zeta;
    this.coef = coef;
  }
  
  @Override
  public String toString() {
    return "[" + atomNo 
        + "," + x
        + "," + y
        + "," + z
        + "," + r
        + "," + zeta
        + "," + coef
        + "]";
  }
}