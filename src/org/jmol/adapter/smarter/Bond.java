/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-09-07 17:50:21 -0500 (Sun, 07 Sep 2014) $
 * $Revision: 19982 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.adapter.smarter;
public class Bond extends AtomSetObject {
  public int atomIndex1;
  public int atomIndex2;
  public int order;
  public float radius = -1;
  public short colix = -1;
  public int uniqueID = -1;


  /**
   * @param atomIndex1 
   * @param atomIndex2 
   * @param order 
   * @j2sIgnoreSuperConstructor
   */
  public Bond (int atomIndex1, int atomIndex2, int order) {
    this.atomIndex1 = atomIndex1;
    this.atomIndex2 = atomIndex2;
    this.order = order;
  }
}
