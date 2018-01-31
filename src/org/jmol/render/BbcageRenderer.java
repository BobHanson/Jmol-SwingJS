/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-06-14 23:36:56 -0500 (Tue, 14 Jun 2016) $
 * $Revision: 21144 $
 *
 * Copyright (C) 2002-2006  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.render;

import org.jmol.script.T;
import org.jmol.shape.Bbcage;
import org.jmol.util.BoxInfo;
import org.jmol.viewer.StateManager;

public class BbcageRenderer extends CageRenderer {

  @Override
  protected void initRenderer() {
    tickEdges = BoxInfo.bbcageTickEdges; 
  }
  
  @Override
  protected boolean render() {
    Bbcage bbox = (Bbcage) shape;
    boolean hiddenLines = (vwr.getBoolean(T.hiddenlinesdashed));
    // no translucent bounding box
    if (bbox.isVisible && (isExport || g3d.checkTranslucent(false))
        && !vwr.isJmolDataFrame()) {
      colix = vwr.getObjectColix(StateManager.OBJ_BOUNDBOX);
      renderCage(vwr.getObjectMad10(StateManager.OBJ_BOUNDBOX), ms.getBBoxVertices(), (hiddenLines ? BoxInfo.facePoints : null), null, 0, 0xFF, 0xFF, 1);
    }
    return false;
  }

}
