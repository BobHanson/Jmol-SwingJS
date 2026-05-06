/*  
 *  The Janocchio program is (C) 2007 Eli Lilly and Co.
 *  Authors: David Evans and Gary Sharman
 *  Contact : janocchio-users@lists.sourceforge.net.
 * 
 *  It is derived in part from Jmol 
 *  (C) 2002-2006 The Jmol Development Team
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.
 *  All we ask is that proper credit is given for our work, which includes
 *  - but is not limited to - adding the above copyright notice to the beginning
 *  of your source code files, and to any copyright notice that you may distribute
 *  with programs based on this work.
 *
 *  This program is distributed in the hope that it will be useful, on an 'as is' basis,
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.openscience.jmol.app.janocchio;

public class MeasureNoe extends Measure {
 
  public MeasureNoe(String expValue, double calcValue) {
    super(expValue, calcValue, Measure.TYPE_NOE);
    if (expValue != null) {
      double dexp = Math.abs((Double.valueOf(expValue)).doubleValue());
      double dcalc = Math.abs(calcValue);
      // work around case where measured value is close to zero i.e. couldn't be integrated
      if (dexp < 0.005) {
        if (dcalc < 0.03) {
          this.diff = 0.0;
        }
        // hack - basically forcing the colour based on the default thresholds
        else if (dcalc > 0.03 && dcalc < 0.05) {
          this.diff = 0.2 + 0.5 * (0.4 - 0.2);
        } else {
          this.diff = 0.4 + 1.0;
        }
      } else {
        this.diff = Math.abs(Math.log(Math.abs(dcalc / dexp)) / Math.log(10.0));
      }
    } else {
      this.diff = 0.0;
    }
  }

}
