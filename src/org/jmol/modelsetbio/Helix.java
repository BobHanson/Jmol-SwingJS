/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-02-25 19:38:15 -0600 (Sun, 25 Feb 2007) $
 * $Revision: 6934 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
package org.jmol.modelsetbio;


import org.jmol.c.STR;

import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.V3;

public class Helix extends ProteinStructure {

  /**
   * @param apolymer 
   * @param monomerIndex 
   * @param monomerCount 
   * @param subtype 
   * 
   */
  Helix(AlphaPolymer apolymer, int monomerIndex, int monomerCount, STR subtype) {
    setupPS(apolymer, STR.HELIX, monomerIndex,
        monomerCount);
    //System.out.println("helix " + apolymer.monomers[monomerIndex].chain.model.modelIndex + " " + apolymer.monomers[monomerIndex] + " " + apolymer.monomers[monomerCount + monomerIndex - 1]);
    this.subtype = subtype;
  }

  @Override
  public void calcAxis() {
    if (axisA != null)
      return;
    P3[] points = new P3[nRes + 1];
    for (int i = 0; i <= nRes; i++)
      apolymer.getLeadMidPoint(monomerIndexFirst + i, points[i] = new P3());
    axisA = new P3();
    axisUnitVector = new V3();
    Measure.calcBestAxisThroughPoints(points, points.length, axisA, axisUnitVector,
        vectorProjection, 4);  
    axisB = P3.newP(points[nRes]);
    Measure.projectOntoAxis(axisB, axisA, axisUnitVector, vectorProjection);
    //System.out.println("draw width 1.0 " + axisA + " " + axisB + " //" + apolymer.monomers[monomerIndexFirst] + " " + apolymer.monomers[monomerIndexFirst + nRes - 1]);
  }

  /****************************************************************
   * see also: 
   * (not implemented -- I never got around to reading this -- BH)
   * Defining the Axis of a Helix
   * Peter C Kahn
   * Computers Chem. Vol 13, No 3, pp 185-189, 1989
   *
   * Simple Methods for Computing the Least Squares Line
   * in Three Dimensions
   * Peter C Kahn
   * Computers Chem. Vol 13, No 3, pp 191-195, 1989
   ****************************************************************/

}
