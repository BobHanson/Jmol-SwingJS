/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-29 04:39:40 -0500 (Thu, 29 Mar 2007) $
 * $Revision: 7248 $
 *
 * Copyright (C) 2003-2006  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.renderbio;


import javajs.util.P3;

import org.jmol.shapebio.BioShape;
import org.jmol.shapebio.Strands;

public class StrandsRenderer extends BioShapeRenderer {

  protected int strandCount = 1;
  protected float strandSeparation;
  protected float baseStrandOffset;

  @Override
  protected void renderBioShape(BioShape bioShape) {
    renderStrandShape();
  }
  
  protected void renderStrandShape() {
    if (!setStrandCount())
      return;
    renderStrands();
  }

  protected boolean setStrandCount() {
    if (wingVectors == null)
      return false;
    strandCount = (shape instanceof Strands ? vwr.getStrandCount(((Strands) shape).shapeID) : 10);
    strandSeparation = (strandCount <= 1) ? 0 : 1f / (strandCount - 1);
    baseStrandOffset = ((strandCount & 1) == 0 ? strandSeparation / 2
        : strandSeparation);
    return true;
  }

  protected void renderStrands() {
    P3[] screens;
    for (int i = strandCount >> 1; --i >= 0;) {
      float f = (i * strandSeparation) + baseStrandOffset;
      screens = calcScreens(f, mads);
      renderStrand(screens);
      vwr.freeTempPoints(screens);
      screens = calcScreens(-f, mads);
      renderStrand(screens);
      vwr.freeTempPoints(screens);
    }
    if (strandCount % 2 == 1) {
      screens = calcScreens(0f, mads);
      renderStrand(screens);
      vwr.freeTempPoints(screens);
    }
  }

  private void renderStrand(P3[] screens) {
    for (int i = bsVisible.nextSetBit(0); i >= 0; i = bsVisible.nextSetBit(i + 1))
      renderHermiteCylinder(screens, i);
  }
}
