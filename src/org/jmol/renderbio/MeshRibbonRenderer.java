/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-11-16 01:43:57 -0600 (Thu, 16 Nov 2006) $
 * $Revision: 6225 $
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

package org.jmol.renderbio;

import org.jmol.shapebio.BioShape;

public class MeshRibbonRenderer extends StrandsRenderer {

  @Override
  protected void renderBioShape(BioShape bioShape) {
    if (wireframeOnly)
      renderStrands();
    else
      renderMeshRibbon();
  }

  protected void renderMeshRibbon() {
    if (!setStrandCount())
      return;
    float offset = ((strandCount >> 1) * strandSeparation) + baseStrandOffset;
    render2Strand(false, offset, offset);
    renderStrands();
  }

  protected void render2Strand(boolean doFill, float offsetTop, float offsetBottom) {
    calcScreenControlPoints();
    ribbonTopScreens = calcScreens(offsetTop, mads);
    ribbonBottomScreens = calcScreens(-offsetBottom, mads);
    for (int i = bsVisible.nextSetBit(0); i >= 0; i = bsVisible.nextSetBit(i + 1))
      renderHermiteRibbon(doFill, i, false);
    vwr.freeTempPoints(ribbonTopScreens);
    vwr.freeTempPoints(ribbonBottomScreens);
  }
}
