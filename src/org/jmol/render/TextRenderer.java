/* $RCSfile$
 * $Author: nicove $
 * $Date: 2010-07-31 04:51:00 -0500 (Sat, 31 Jul 2010) $
 * $Revision: 13783 $

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

package org.jmol.render;

import org.jmol.api.JmolRendererInterface;
import org.jmol.modelset.Text;
import org.jmol.viewer.JC;

import org.jmol.awtjs.swing.Font;

class TextRenderer {
  
  static boolean render(Text text, JmolRendererInterface g3d,
                     float scalePixelsPerMicron, float imageFontScaling,
                     boolean isAbsolute, float[] boxXY, float[] temp) {
    if (text == null || text.image == null && !text.doFormatText && text.lines == null)
      return false;
    boolean showText = g3d.setC(text.colix);
    if (!showText
        && (text.image == null && (text.bgcolix == 0 || !g3d
            .setC(text.bgcolix))))
      return false;
    text.setPosition(scalePixelsPerMicron, imageFontScaling, isAbsolute, boxXY);
    // draw the box if necessary; colix has been set
    if (text.image == null && text.bgcolix != 0) {
      if (g3d.setC(text.bgcolix))
        showBox(g3d, text.colix, (int) text.boxX, (int) text.boxY
            + text.boxYoff2 * 2, text.z + 2, text.zSlab, (int) text.boxWidth,
            (int) text.boxHeight, text.fontScale, text.isLabelOrHover);
      if (!showText)
        return false;
    }
    // text colix will be opaque, but we need to render it in translucent pass 
    // now set x and y positions for text from (new?) box position
    if (text.image == null) {
      // now write properly aligned text
      for (int i = 0; i < text.lines.length; i++) {
        text.setXYA(temp, i);
        g3d.drawString(text.lines[i], text.font, (int) temp[0], (int) temp[1],
            text.z, text.zSlab, text.bgcolix);
      }
    } else {
      g3d.drawImage(text.image, (int) text.boxX, (int) text.boxY, text.z,
          text.zSlab, text.bgcolix, (int) text.boxWidth, (int) text.boxHeight);
    }
    // now draw the pointer, if requested

    if ((text.pointer & JC.LABEL_POINTER_ON) == 0
        || !g3d.setC((text.pointer & JC.LABEL_POINTER_BACKGROUND) != 0
            && text.bgcolix != 0 ? text.bgcolix : text.colix))
      return true;
    float w = text.boxWidth;
    float h = text.boxHeight;
    float pt = Float.NaN;
    float x = text.boxX
        + (text.boxX > text.atomX + w ? 0 : text.boxX + w < text.atomX -w ? w
            : (pt = w / 2));
    boolean setY = !Float.isNaN(pt);
    float y = text.boxY
        + (setY && text.boxY > text.atomY ? 0 : setY
            && text.boxY + h < text.atomY ? h
            : h / 2);
    g3d.drawLineXYZ(text.atomX, text.atomY, text.atomZ, (int) x, (int) y,
        text.zSlab);
    return true;
  }

  static void renderSimpleLabel(JmolRendererInterface g3d, Font font,
                                String strLabel, short colix, short bgcolix,
                                float[] boxXY, int z, int zSlab, int xOffset,
                                int yOffset, float ascent, int descent,
                                boolean doPointer, short pointerColix,
                                boolean isAbsolute) {

    // old static style -- quick, simple, no line breaks, odd alignment?
    // LabelsRenderer only

    float boxWidth = font.stringWidth(strLabel) + 8;
    float boxHeight = ascent + descent + 8;

    int x0 = (int) boxXY[0];
    int y0 = (int) boxXY[1];

    Text.setBoxXY(boxWidth, boxHeight, xOffset, yOffset, boxXY, isAbsolute);

    float x = boxXY[0];
    float y = boxXY[1];
    if (bgcolix != 0 && g3d.setC(bgcolix))
      showBox(g3d, colix, (int) x, (int) y, z, zSlab, (int) boxWidth,
          (int) boxHeight, 1, true);
    else
      g3d.setC(colix);
    g3d.drawString(strLabel, font, (int) (x + 4), (int) (y + 4 + ascent),
        z - 1, zSlab, bgcolix);

    if (doPointer) {
      g3d.setC(pointerColix);
      if (xOffset > 0)
        g3d.drawLineXYZ(x0, y0, zSlab, (int) x, (int) (y + boxHeight / 2),
            zSlab);
      else if (xOffset < 0)
        g3d.drawLineXYZ(x0, y0, zSlab, (int) (x + boxWidth),
            (int) (y + boxHeight / 2), zSlab);
    }
  }

  private static void showBox(JmolRendererInterface g3d, short colix,
                              int x, int y, int z, int zSlab,
                              int boxWidth, int boxHeight,
                              float imageFontScaling, boolean atomBased) {
    g3d.fillTextRect(x, y, z, zSlab, boxWidth, boxHeight);
    g3d.setC(colix);
    if (!atomBased)
      return;
    if (imageFontScaling >= 2) {
      g3d.drawRect(x + 3, y + 3, z - 1, zSlab, boxWidth - 6, boxHeight - 6);
      //g3d.drawRect(x + 40, y + 4, z - 1, zSlab, boxWidth - 8, boxHeight - 8);
    } else {
      g3d.drawRect(x + 1, y + 1, z - 1, zSlab, boxWidth - 2, boxHeight - 2);
    }
  }

}
