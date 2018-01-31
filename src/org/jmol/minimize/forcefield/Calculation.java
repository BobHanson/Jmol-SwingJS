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

package org.jmol.minimize.forcefield;

import javajs.util.Lst;

import org.jmol.minimize.MinAtom;

abstract class Calculation {
  
  Integer key;
  
  double dE;
  
  MinAtom a, b, c, d;
  int ia, ib, ic, id;

  int[] iData;
  double[] dData;

  double delta, rab, theta;
  double energy;

  Calculations calcs;
  
  Calculation set(Calculations calcs) {
    this.calcs = calcs;
    return this;
  }

  /**
   * @param calc  
   * @param ia 
   * @param ib 
   * @param d 
   */
  void setData(Lst<Object[]> calc, int ia, int ib, double d) {
    // varies
  }

  abstract double compute(Object[] dataIn);
  
  double getEnergy() {
    return energy;
  }
  
  void getPointers(Object[] dataIn) {
    dData = (double[])dataIn[1];
    iData = (int[])dataIn[0];
    switch (iData.length) {
    default:
      id = iData[3];
      //$FALL-THROUGH$
    case 3:
      ic = iData[2];
      //$FALL-THROUGH$
    case 2:
      ib = iData[1];
      //$FALL-THROUGH$
    case 1:
      ia = iData[0];
      //$FALL-THROUGH$
    case 0:  
      break;
    }
  }
}
