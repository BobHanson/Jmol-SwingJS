/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2017-03-25 12:28:14 -0400 (Sat, 25 Mar 2017) $
 * $Revision: 21449 $
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

import org.jmol.modelset.Atom;
import org.jmol.modelset.Text;
import org.jmol.script.T;
import org.jmol.shape.Echo;
import org.jmol.util.C;
import org.jmol.viewer.JC;

public class EchoRenderer extends LabelsRenderer {

  @Override
  protected boolean render() {
    if (vwr.isPreviewOnly)
      return false;
    Echo echo = (Echo) shape;
    sppm = (vwr.getBoolean(T.fontscaling) ? vwr
        .getScalePixelsPerAngstrom(true) * 10000 : 0);
    imageFontScaling = vwr.imageFontScaling;
    boolean haveTranslucent = false;
    int alias = (g3d.isAntialiased() ? TextRenderer.MODE_IS_ANTIALIASED : 0);
    for (Text t : echo.objects.values()) {
      if (!t.visible || t.hidden) {
        continue;
      }
      if (t.pointerPt instanceof Atom) {
        if (!((Atom) t.pointerPt).checkVisible())
          continue;
      }
      if (t.valign == JC.ECHO_XYZ) {
        tm.transformPtScr(t.xyz, pt0i);
        t.setXYZs(pt0i.x, pt0i.y, pt0i.z, pt0i.z);
      }
      if (t.pymolOffset != null)
        t.getPymolScreenOffset(t.xyz, pt0i, zSlab, pTemp, sppm);
      else if (t.movableZPercent != Integer.MAX_VALUE) {
        int z = vwr.tm.zValueFromPercent(t.movableZPercent % 1000);
        if (t.valign == JC.ECHO_XYZ && Math.abs(t.movableZPercent) >= 1000)
          z = pt0i.z - vwr.tm.zValueFromPercent(0) + z;
        t.setZs(z, z);
      }
      if (t.pointerPt == null) {
        t.pointer = JC.LABEL_POINTER_NONE;
      } else {
        t.pointer = JC.LABEL_POINTER_ON;
        tm.transformPtScr(t.pointerPt, pt0i);
        t.atomX = pt0i.x;
        t.atomY = pt0i.y;
        t.atomZ = pt0i.z;
        if (t.zSlab == Integer.MIN_VALUE)
          t.zSlab = 1;
      }
      if (TextRenderer.render(t, g3d, sppm, imageFontScaling, null, xy, (short) 0, 0, alias)
          && t.valign == JC.ECHO_BOTTOM
          && t.align == JC.TEXT_ALIGN_RIGHT)
        vwr.noFrankEcho = false;

      if (C.renderPass2(t.bgcolix) || C.renderPass2(t.colix))
        haveTranslucent = true;
    }
    if (!isExport) {
      String frameTitle = vwr.getFrameTitle();
      if (frameTitle != null && frameTitle.length() > 0) {
        if (g3d.setC(vwr.cm.colixBackgroundContrast)) {
          renderFrameTitle(vwr.formatText(frameTitle));
        }
      }
    }
    return haveTranslucent;
  }
  
  private void renderFrameTitle(String frameTitle) {
    vwr.gdata.setFontBold("arial", (int) (24 * imageFontScaling));
    int y = (int) Math.floor(vwr.getScreenHeight() * (g3d.isAntialiased() ? 2 : 1) - 10 * imageFontScaling);
    int x = (int) Math.floor(5 * imageFontScaling);
    g3d.drawStringNoSlab(frameTitle, null, x, y, 0, (short) 0);
  }
}
