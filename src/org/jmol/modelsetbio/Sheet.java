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

import javajs.util.MeasureD;
import javajs.util.P3d;
import javajs.util.V3d;

public class Sheet extends ProteinStructure {

  /**
   * @param apolymer
   * @param monomerIndex
   * @param monomerCount
   * @param subtype
   * 
   */
  Sheet(AlphaPolymer apolymer, int monomerIndex, int monomerCount, STR subtype) {
    setupPS(apolymer, STR.SHEET, monomerIndex, monomerCount);
    this.subtype = subtype;
  }

  @Override
  public void calcAxis() {
    if (axisA != null)
      return;
    if (nRes == 2) {
      axisA = apolymer.getLeadPoint(monomerIndexFirst);
      axisB = apolymer.getLeadPoint(monomerIndexFirst + 1);
    } else {
      axisA = new P3d();
      apolymer.getLeadMidPoint(monomerIndexFirst + 1, axisA);
      axisB = new P3d();
      apolymer.getLeadMidPoint(monomerIndexFirst + nRes - 1, axisB);
    }

    axisUnitVector = new V3d();
    axisUnitVector.sub2(axisB, axisA);
    axisUnitVector.normalize();

    P3d tempA = new P3d();
    apolymer.getLeadMidPoint(monomerIndexFirst, tempA);
    if (notHelixOrSheet(monomerIndexFirst - 1))
      MeasureD
          .projectOntoAxis(tempA, axisA, axisUnitVector, vectorProjection);
    P3d tempB = new P3d();
    apolymer.getLeadMidPoint(monomerIndexFirst + nRes, tempB);
    if (notHelixOrSheet(monomerIndexFirst + nRes))
      MeasureD
          .projectOntoAxis(tempB, axisA, axisUnitVector, vectorProjection);
    axisA = tempA;
    axisB = tempB;
  }

  private boolean notHelixOrSheet(int i) {
    return (i < 0 || i >= apolymer.monomerCount 
        || !apolymer.monomers[i].isHelix() 
          && !apolymer.monomers[i].isSheet());
  }

  V3d widthUnitVector;
  V3d heightUnitVector;

  void calcSheetUnitVectors() {
    if (!(apolymer instanceof AminoPolymer))
      return;
    if (widthUnitVector == null) {
      V3d vectorCO = new V3d();
      V3d vectorCOSum = new V3d();
      AminoMonomer amino = (AminoMonomer) apolymer.monomers[monomerIndexFirst];
      vectorCOSum.sub2(amino.getCarbonylOxygenAtom(), amino
          .getCarbonylCarbonAtom());
      for (int i = nRes; --i > monomerIndexFirst;) {
        amino = (AminoMonomer) apolymer.monomers[i];
        vectorCO.sub2(amino.getCarbonylOxygenAtom(), amino
            .getCarbonylCarbonAtom());
        if (vectorCOSum.angle(vectorCO) < Math.PI / 2)
          vectorCOSum.add(vectorCO);
        else
          vectorCOSum.sub(vectorCO);
      }
      heightUnitVector = vectorCO; // just reuse the same temp vector;
      heightUnitVector.cross(axisUnitVector, vectorCOSum);
      heightUnitVector.normalize();
      widthUnitVector = vectorCOSum;
      widthUnitVector.cross(axisUnitVector, heightUnitVector);
    }
  }

  public void setBox(double w, double h, P3d pt, V3d vW, V3d vH, P3d ptC, double scale) {
    if (heightUnitVector == null)
      calcSheetUnitVectors();
    vW.setT(widthUnitVector);
    vW.scale(scale * w);
    vH.setT(heightUnitVector);
    vH.scale(scale * h);
    ptC.ave(vW, vH);
    ptC.sub2(pt, ptC);
  }
}
