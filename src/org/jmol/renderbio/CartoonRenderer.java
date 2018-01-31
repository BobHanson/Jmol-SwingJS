/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-11 14:30:16 -0500 (Sun, 11 Mar 2007) $
 * $Revision: 7068 $
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

package org.jmol.renderbio;

import org.jmol.api.Interface;
import org.jmol.c.STR;
import org.jmol.modelsetbio.ProteinStructure;
import org.jmol.script.T;
import org.jmol.shapebio.BioShape;

public class CartoonRenderer extends RocketsRenderer {

  private NucleicRenderer nucleicRenderer;
  
  @Override
  protected void renderBioShape(BioShape bioShape) {
    if (!setupRR(bioShape, false))
      return;
    if (isNucleic || isPhosphorusOnly) {
      if (nucleicRenderer == null)
        nucleicRenderer = (NucleicRenderer) Interface.getInterface("org.jmol.renderbio.NucleicRenderer", vwr, "render");
      calcScreenControlPoints();
      nucleicRenderer.renderNucleic(this);
      return;
    }
    boolean val = vwr.getBoolean(T.cartoonrockets);
    if (helixRockets != val) {
      bioShape.falsifyMesh();
      helixRockets = val;
    }
    ribbonTopScreens = calcScreens(0.5f, mads);
    ribbonBottomScreens = calcScreens(-0.5f, mads);
    calcRopeMidPoints();
    renderProtein();
    vwr.freeTempPoints(cordMidPoints);
    vwr.freeTempPoints(ribbonTopScreens);
    vwr.freeTempPoints(ribbonBottomScreens);
  }

  private void renderProtein() {
    boolean lastWasSheet = false;
    boolean lastWasHelix = false;
    ProteinStructure previousStructure = null;
    ProteinStructure thisStructure;

    // Key structures that must render properly
    // include 1crn and 7hvp

    // this loop goes monomerCount --> 0, because
    // we want to hit the heads first

    boolean needRockets = (helixRockets || !renderArrowHeads);
    boolean doRockets = false;
    for (int i = monomerCount; --i >= 0;) {
      // runs backwards, so it can render the heads first
      thisStructure = (ProteinStructure) monomers[i].getStructure();
      if (thisStructure != previousStructure) {
        lastWasSheet = false;
      }
      previousStructure = thisStructure;
      boolean isHelix = (structureTypes[i] == STR.HELIX);
      boolean isSheet = (structureTypes[i] == STR.SHEET);
      if (bsVisible.get(i)) {
        if (isHelix && needRockets) {
          doRockets = true;
        } else if (isSheet || isHelix) {
          if (lastWasSheet && isSheet || lastWasHelix && isHelix) {
            //uses topScreens
            renderHermiteRibbon(true, i, true);
          } else {
            renderHermiteArrowHead(i);
          }
        } else {
          renderHermiteConic(i, true, 7);
        }
      }
      lastWasSheet = isSheet;
      lastWasHelix = isHelix && !helixRockets;
    }
    if (doRockets)
      renderRockets();
  }

}
