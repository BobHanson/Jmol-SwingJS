/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2015-08-25 00:56:29 -0500 (Tue, 25 Aug 2015) $
 * $Revision: 20734 $
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
package org.jmol.render;
import org.jmol.script.T;
import org.jmol.shape.Frank;
import org.jmol.util.C;

public class FrankRenderer extends ShapeRenderer {

  //we render Frank last just for the touch that if there are translucent
  //objects, then it becomes translucent. Just for fun.

  // no Frank export

  @Override
  protected boolean render() {
    Frank frank = (Frank) shape;
    boolean allowKeys = vwr.getBooleanProperty("allowKeyStrokes");
    boolean modelKitMode = vwr.getBoolean(T.modelkitmode);
    colix = (modelKitMode ? C.MAGENTA : vwr.isSignedApplet ? (allowKeys
        || vwr.isJS && !vwr.isWebGL ? C.ORANGE : C.RED) : allowKeys ? C.BLUE
        : C.GRAY);
    if (isExport
        || !vwr.getShowFrank()
        || !g3d.setC(colix))
      return false;
    if (vwr.frankOn && !vwr.noFrankEcho)
      return vwr.noFrankEcho;
    vwr.noFrankEcho = true;
    float imageFontScaling = vwr.imageFontScaling;
    frank.getFont(imageFontScaling);
    int dx = (int) (frank.frankWidth + Frank.frankMargin * imageFontScaling);
    int dy = frank.frankDescent;
    g3d.drawStringNoSlab(frank.frankString, frank.font3d, vwr.gdata.width - dx,
        vwr.gdata.height - dy, 0, (short) 0);
    if (modelKitMode) {
      //g3d.setColix(GData.GRAY);
      g3d.fillTextRect(0, 0, 0, 0, dy * 2, dx * 3 / 2);
    }
    return false;
  }
}
