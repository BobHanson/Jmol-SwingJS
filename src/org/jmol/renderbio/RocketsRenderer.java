/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-11 14:30:16 -0500 (Sun, 11 Mar 2007) $
 * $Revision: 7068 $
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

import org.jmol.c.STR;
import org.jmol.modelsetbio.ProteinStructure;
import org.jmol.script.T;
import org.jmol.shapebio.BioShape;

import javajs.api.Interface;
import javajs.util.P3;

public class RocketsRenderer extends StrandsRenderer {

  //private final static float MIN_CONE_HEIGHT = 0.05f;

  boolean isRockets;
  protected boolean helixRockets = true;
  protected boolean renderArrowHeads;

  protected P3[] cordMidPoints;
  private RocketRenderer rr;

  @Override
  protected void renderBioShape(BioShape bioShape) {
    if (!setupRR(bioShape, true))
      return;
    calcRopeMidPoints();
    renderRockets();
    vwr.freeTempPoints(cordMidPoints);
  }

  protected void renderRockets() {
    if (rr == null)
      rr = ((RocketRenderer) Interface.getInterface("org.jmol.renderbio.RocketRenderer")).set(this);
    rr.renderRockets();
  }

  protected boolean setupRR(BioShape bioShape, boolean isRockets) {
    this.isRockets = isRockets;
    if (wireframeOnly) {
      renderStrands();
    } else if (wingVectors != null && !isCarbohydrate
        && !(isRockets && isNucleic)) {
      boolean val = !vwr.getBoolean(T.rocketbarrels);
      if (!isNucleic && renderArrowHeads != val) {
        bioShape.falsifyMesh();
        renderArrowHeads = val;
      }
      return true;
    }
    return false;
  }

  protected void calcRopeMidPoints() {
    int midPointCount = monomerCount + 1;
    cordMidPoints = vwr.allocTempPoints(midPointCount);
    ProteinStructure proteinstructurePrev = null;
    P3 point;
    int ptLastRocket = -10;
    P3 pt1 = new P3();
    P3 pt2 = new P3();
    for (int i = 0; i <= monomerCount; ++i) {
      point = cordMidPoints[i];
      if (i < monomerCount && (helixRockets && structureTypes[i] == STR.HELIX || isRockets && structureTypes[i] == STR.SHEET)) {
        ProteinStructure proteinstructure = (ProteinStructure) monomers[i].getStructure();
        if (proteinstructure == proteinstructurePrev) {
          pt1.add(pt2);
          ptLastRocket = i;
        } else {
          proteinstructurePrev = proteinstructure;
          pt1.setT(proteinstructure.getAxisStartPoint());
          pt2.sub2(proteinstructure.getAxisEndPoint(), pt1);
          //System.out.println("barrel " + i + "  " + pt1 + " " + proteinstructure.getAxisEndPoint());
          pt2.scale(1f / (proteinstructure.nRes - 1));
          if (ptLastRocket == i - 3) {
            // too tight! Thank you, Frieda!
            // TODO: in 1crn 30-32, this is still not satisfactory
            cordMidPoints[i - 1].ave(cordMidPoints[i - 2], pt1);
            //System.out.println(monomers[i-1] + " " + cordMidPoints[i-1]);
          }
        }       
        point.setT(pt1);
      } else {
        if (ptLastRocket == i - 1 && i > 1)
          cordMidPoints[i - 1].setT(cordMidPoints[i > 2 ? i - 3 : i - 2]);
        point.setT(proteinstructurePrev == null ? controlPoints[i]
            : proteinstructurePrev.getAxisEndPoint());
        proteinstructurePrev = null;
      }
   }
    controlPoints = cordMidPoints;
    calcScreenControlPoints();
  }

}
