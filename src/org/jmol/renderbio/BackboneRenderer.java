/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-11-17 10:45:52 -0600 (Fri, 17 Nov 2006) $
 * $Revision: 6250 $

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

import org.jmol.modelset.Atom;
import org.jmol.shapebio.BioShape;
import org.jmol.util.C;

public class BackboneRenderer extends BioShapeRenderer {

  private boolean isDataFrame;

  @Override
  protected void renderBioShape(BioShape bioShape) {
    boolean checkPass2 = (!isExport && !vwr.gdata.isPass2);
    isDataFrame = ms.isJmolDataFrameForModel(bioShape.modelIndex);
    int n = monomerCount;
    Atom[] atoms = ms.at;
    for (int i = bsVisible.nextSetBit(0); i >= 0; i = bsVisible
        .nextSetBit(i + 1)) {
      Atom atomA = atoms[leadAtomIndices[i]];
      short cA = colixes[i];
      mad = mads[i];
      int i1 = (i + 1) % n;
      Atom atomB = atoms[leadAtomIndices[i1]];
      short cB = colixes[i1];
      if (atomA.nBackbonesDisplayed > 0 && atomB.nBackbonesDisplayed > 0
          && !ms.isAtomHidden(atomB.i)
          && (isDataFrame || atomA.distanceSquared(atomB) < 100)) {
        cA = C.getColixInherited(cA, atomA.colixAtom);
        cB = C.getColixInherited(cB, atomB.colixAtom);
        if (!checkPass2 || setBioColix(cA) || setBioColix(cB))
          drawSegmentAB(atomA, atomB, cA, cB, 100);
      }
      //      boolean showSteps = vwr.getBoolean(T.backbonesteps)
      //          && bioShape.bioPolymer.isNucleic();
      //      if (showSteps) {
      //        NucleicMonomer g = (NucleicMonomer) monomers[i];
      //        Lst<BasePair> bps = g.getBasePairs();
      //        if (bps != null) {
      //          for (int j = bps.size(); --j >= 0;) {
      //            int iAtom = bps.get(j).getPartnerAtom(g);
      //            if (iAtom > i)
      //              drawSegment(atomA, atoms[iAtom], cA, cA, 1000, checkPass2);
      //          }
      //        }
      //      }  
    }
  }
  
}
