/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 10:52:44 -0500 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
 *
 * Copyright (C) 2006  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.util;

import javajs.util.P3;


/**
 * the Point3fi class allows storage of critical information involving
 * an atom, picked bond, or measurement point, including: 
 * 
 * xyz position 
 * screen position
 * screen radius (-1 for a simple point)
 * index (for atoms or for an associated bond that has be picked)
 * associated modelIndex (for measurement points)
 * 
 */
public class Point3fi extends P3 implements Cloneable {
  /**
   * model index -- may be to a trajectory
   */
  public short mi = -1;

  /**
   * index in full collection
   * 
   */
  public int i;
  /**
   * screen X
   */
  public int sX;
  /**
   * screen Y
   */
  public int sY;
  /**
   * screen Z
   * 
   */
  public int sZ;
  /**
   * screen diameter
   */
  public short sD = -1;

 public Point3fi copy() {
    try {
      return (Point3fi) clone();
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

}
