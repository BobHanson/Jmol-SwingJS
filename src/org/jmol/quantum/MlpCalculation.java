/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-05-13 19:17:06 -0500 (Sat, 13 May 2006) $
 * $Revision: 5114 $
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
package org.jmol.quantum;


import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.util.Logger;

/*
 * 
 * NOTE -- THIS CLASS IS INSTANTIATED USING Interface.getOptionInterface
 * NOT DIRECTLY -- FOR MODULARIZATION. NEVER USE THE CONSTRUCTOR DIRECTLY!
 */
public class MlpCalculation extends MepCalculation {

  public MlpCalculation() {
    super();
    distanceMode = E_MINUS_D;  
  }  

  @Override
  public void assignPotentials(Atom[] atoms, double[] potentials,
                               BS bsAromatic, BS bsCarbonyl,
                               BS bsIgnore, String data) {
    getAtomicPotentials(data, "atomicLipophilicity.txt");
    for (int i = 0; i < atoms.length; i++) {
      double f = Math.abs(atoms[i].getFormalCharge());
      if (f == 0) {
        if (bsIgnore != null && bsIgnore.get(i)) {
          f = Double.NaN;
        } else {
          f = getTabulatedPotential(atoms[i]);
          if (Double.isNaN(f))
            switch (atoms[i].getElementNumber()) {
            case 6:
              f = (bsAromatic.get(i) ? 0.31f : bsCarbonyl.get(i) ? -0.54d
                  : 0.45d);
              break;
            case 7:
              f = (bsAromatic.get(i) ? -0.6d : bsCarbonyl.get(i) ? -0.44d
                  : -1.0d);
              break;
            case 8:
              f = (bsCarbonyl.get(i) ? -0.9d : -0.17d);
              break;
            default:
              f = Double.NaN;
            }
        }
      }
      if (Logger.debugging)
        Logger.debug(atoms[i].getInfo() + " " + f);
      potentials[i] = f;
    }
  }

}
