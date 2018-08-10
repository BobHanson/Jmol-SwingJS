/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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

import javajs.util.P3;
import javajs.util.T3;

import org.jmol.modelset.Atom;
import org.jmol.util.Logger;

class QMAtom extends P3 {

  // grid coordinates relative to orbital center in Bohr 
  private float[] myX, myY, myZ;

  // grid coordinate squares relative to orbital center in Bohr
  private float[] myX2, myY2, myZ2;

  Atom atom;
  int index;
  int znuc;
  int iMolecule;
  //boolean isExcluded; -- not implemented?

  /**
   * @j2sIgnoreSuperConstructor
   * @j2sOverride
   * 
   * @param i
   * @param xyzAng 
   * @param atom 
   * @param X
   * @param Y
   * @param Z
   * @param X2
   * @param Y2
   * @param Z2
   * @param unitFactor
   */
  QMAtom(int i, T3 xyzAng, Atom atom, float[] X, float[] Y, float[] Z, 
      float[] X2, float[] Y2, float[] Z2, float unitFactor) {
    index = i;
    myX = X;
    myY = Y;
    myZ = Z;
    myX2 = X2;
    myY2 = Y2;
    myZ2 = Z2;
    this.atom = atom;
    
    //this.isExcluded = isExcluded;
    setT(xyzAng);
    scale(unitFactor);
    znuc = atom.getElementNumber();
  }

  protected void setXYZ(QuantumCalculation qc, boolean setMinMax) {
    int i;
    try {
      if (setMinMax) {
        if (qc.points != null) {
          qc.xMin = qc.yMin = qc.zMin = 0;
          qc.xMax = qc.yMax = qc.zMax = qc.points.length;
        } else {
          i = (int) Math.floor((x - qc.xBohr[0] - qc.rangeBohrOrAngstroms)
              / qc.stepBohr[0]);
          qc.xMin = (i < 0 ? 0 : i);
          i = (int) Math.floor(1 + (x - qc.xBohr[0] + qc.rangeBohrOrAngstroms)
              / qc.stepBohr[0]);
          qc.xMax = (i >= qc.nX ? qc.nX : i + 1);
          i = (int) Math.floor((y - qc.yBohr[0] - qc.rangeBohrOrAngstroms)
              / qc.stepBohr[1]);
          qc.yMin = (i < 0 ? 0 : i);
          i = (int) Math.floor(1 + (y - qc.yBohr[0] + qc.rangeBohrOrAngstroms)
              / qc.stepBohr[1]);
          qc.yMax = (i >= qc.nY ? qc.nY : i + 1);
          i = (int) Math.floor((z - qc.zBohr[0] - qc.rangeBohrOrAngstroms)
              / qc.stepBohr[2]);
          qc.zMin = (i < 0 ? 0 : i);
          i = (int) Math.floor(1 + (z - qc.zBohr[0] + qc.rangeBohrOrAngstroms)
              / qc.stepBohr[2]);
          qc.zMax = (i >= qc.nZ ? qc.nZ : i + 1);
        }
      }
      for (i = qc.xMax; --i >= qc.xMin;) {
        myX2[i] = myX[i] = qc.xBohr[i] - x;
        myX2[i] *= myX[i];
      }
      for (i = qc.yMax; --i >= qc.yMin;) {
        myY2[i] = myY[i] = qc.yBohr[i] - y;
        myY2[i] *= myY[i];
      }
      for (i = qc.zMax; --i >= qc.zMin;) {
        myZ2[i] = myZ[i] = qc.zBohr[i] - z;
        myZ2[i] *= myZ[i];
      }
      if (qc.points != null) {
        qc.yMax = qc.zMax = 1;
      }

    } catch (Exception e) {
      Logger.error("Error in QuantumCalculation setting bounds");
    }
  }
}