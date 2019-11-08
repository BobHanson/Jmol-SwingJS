/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2008-09-16 22:39:58 -0500 (Tue, 16 Sep 2008) $
 * $Revision: 9905 $

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

import org.jmol.util.Logger;

public class HBond extends Bond {

  float energy;
  
  public HBond(Atom atom1, Atom atom2, int order, short mad, short colix, float energy) {
    super(atom1, atom2, order, mad, colix);
    this.energy = energy;
    if (Logger.debugging)
      Logger.debug("HBond energy = " + energy + " #" + getIdentity());
  }
  
  @Override
  public float getEnergy() {
    return energy;
  }
  
  /*
   * A crude calculation based on simple distances.
   * In the NH -- O=C case this reads DH -- A=C
   * 
   *    (+0.20)  H .......... A (-0.42)
   *             |            |
   *             |            |
   *    (-0.20)  D            C (+0.42)
   * 
   * 
   *   E = Q/rAH - Q/rAD + Q/rCD - Q/rCH
   *   
   *   http://en.wikipedia.org/wiki/DSSP_%28protein%29
   * 
   * Kabsch and Sander DSSP hydrogen bond calculation
   * Kabsch W, Sander C (1983). Dictionary of protein secondary 
   * structure: pattern recognition of hydrogen-bonded and geometrical 
   * features. Biopolymers 22 (12): 2577-2637
   * 
   * 
   */
  
  private final static double QConst = -332 * 0.42 * 0.2 * 1000;  
  
  /**
   * 
   * @param distAH
   * @param distCH
   * @param distCD
   * @param distAD
   * @return          cal/mol
   */
  public final static int getEnergy(double distAH, double distCH, double distCD,
                              double distAD) {
    
    // A crude calculation based on simple distances. In the NH -- O=C
    // case this reads DH -- A=C
    // 
    // (+) H .......... A (-) | | | | (-) D C (+)
    // 
    // 
    // E = Q/rAH - Q/rAD + Q/rCD - Q/rCH
        
    int energy = (int) Math.round(QConst / distAH - QConst / distAD + QConst / distCD - QConst
        / distCH);   
    //Logger.info("HBond: distAH=" + distAH + " distAD=" + distAD + " distCD=" + distCD
      //  + " distCH=" + distCH + " energy=" + energy);
    return energy;
  }
}



