/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-12-15 04:11:31 -0600 (Fri, 15 Dec 2006) $
 * $Revision: 6467 $

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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.shapebio;


import org.jmol.atomdata.RadiusData;
import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.script.T;

public class Backbone extends BioShapeCollection {

  BS bsSelected;
  
  @Override
  public void initShape() {
    madOn = 1;
    madHelixSheet = 1500;
    madTurnRandom = 500;
    madDnaRna = 2000;
    isActive = true;
  }
  
  @Override
  public void setProperty(String propertyName, Object value, BS bsSelected) {
    if ("bitset" == propertyName) {
      this.bsSelected = (BS) value;
      return;
    }
    setPropBSC(propertyName, value, bsSelected);
  }

  @Override
  public void setShapeSizeRD(int size, RadiusData rd, BS bsSelected) {
    short mad = (short) size;
    initialize();
    boolean useThisBsSelected = (this.bsSelected != null);
    if (useThisBsSelected)
      bsSelected = this.bsSelected;
    for (int iShape = bioShapes.length; --iShape >= 0;) {
      BioShape bioShape = bioShapes[iShape];
      if (bioShape.monomerCount == 0)
        continue;
      boolean bondSelectionModeOr = vwr.getBoolean(T.bondmodeor);
      int[] atomIndices = bioShape.bioPolymer.getLeadAtomIndices();
      // note that i is initialized to monomerCount - 1
      // in order to skip the last atom
      // but it is picked up within the loop by looking at i+1
      boolean isVisible = (mad != 0);
      if (bioShape.bsSizeSet == null)
        bioShape.bsSizeSet = new BS();
      bioShape.isActive = true;
      int n = bioShape.monomerCount;
      for (int i = n - (bioShape.bioPolymer.isCyclic() ? 0 : 1); --i >= 0;) {
        int index1 = atomIndices[i];
        int index2 = atomIndices[(i + 1) % n];
        boolean isAtom1 = bsSelected.get(index1);
        boolean isAtom2 = bsSelected.get(index2);
        if (isAtom1 && isAtom2 
            || useThisBsSelected && isAtom1 
            || bondSelectionModeOr && (isAtom1 || isAtom2)) {
          bioShape.monomers[i].setShapeVisibility(vf, isVisible);
          Atom atomA = ms.at[index1];
          if (rd != null) {
            if (Float.isNaN(rd.values[index1]) || Float.isNaN(rd.values[index2]))
              continue;
            mad = (short) ((rd.values[index1] + rd.values[index2]) * 1000); // average
            isVisible = (mad != 0);
          }
          Atom atomB = ms.at[index2];
          boolean wasVisible = (bioShape.mads[i] != 0); 
          if (wasVisible != isVisible) {
            addDisplayedBackbone(atomA, isVisible);
            addDisplayedBackbone(atomB, isVisible);
          }
          bioShape.mads[i] = mad;
          bioShape.bsSizeSet.setBitTo(i, isVisible);
          bioShape.bsSizeDefault.setBitTo(i, mad == -1);
        }
      }
    }
    if (useThisBsSelected) //one shot deal
      this.bsSelected = null;
  }
  
  public void addDisplayedBackbone(Atom a, boolean isVisible) {
    a.nBackbonesDisplayed += (isVisible ? 1 : -1);
    a.setShapeVisibility(vf, isVisible);
  }
  
  @Override
  public void setAtomClickability() {
    if (bioShapes == null)
      return;
    for (int iShape = bioShapes.length; --iShape >= 0; ) {
      BioShape bioShape = bioShapes[iShape];
      int[] atomIndices = bioShape.bioPolymer.getLeadAtomIndices();
      for (int i = bioShape.monomerCount; --i >= 0; ) {
        Atom atom = ms.at[atomIndices[i]];
        if (atom.nBackbonesDisplayed > 0 && !ms.isAtomHidden(atom.i))
          atom.setClickable(vf);
      }
    }
  }  
}
