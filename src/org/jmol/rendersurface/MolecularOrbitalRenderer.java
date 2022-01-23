/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
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
package org.jmol.rendersurface;

import org.jmol.script.T;

//import java.text.NumberFormat;


public class MolecularOrbitalRenderer extends IsosurfaceRenderer {

  @Override
  protected boolean render() {
    imageFontScaling = vwr.imageFontScaling;
    renderIso();
    return needTranslucent;
  }

  @Override
  protected void renderInfo() {
    if (isExport || vwr.am.cmi < 0
        || mesh.title == null 
        || !g3d.setC(vwr.cm.colixBackgroundContrast)
        || vwr.gdata.getTextPosition() != 0)
      return;
    float ht = vwr.getInt(T.infofontsize);
    vwr.gdata.setFontBold("Serif", ht * imageFontScaling);
    int lineheight = Math.round((ht + 1) * imageFontScaling);
    int x = Math.round(5 * imageFontScaling);
    int y = lineheight;    
    for (int i = 0; i < mesh.title.length; i++)
      if (mesh.title[i].length() > 0) {
        g3d.drawStringNoSlab(mesh.title[i], null, x, y, 0, (short) 0);
        y += lineheight;
      }
    vwr.gdata.setTextPosition(y);
  }

}
