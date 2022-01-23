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
import org.jmol.util.Font;
import org.jmol.util.GData;

class TextRenderer {
  
  static boolean render(Text text, JmolRendererInterface g3d,
                        float scalePixelsPerMicron, float imageFontScaling,
                        boolean isAbsolute, float[] boxXY, float[] temp,
                        boolean doPointer, short pointerColix,
                        int pointerWidth, boolean isAntialiased) {
    if (text == null
        || text.image == null && !text.doFormatText && text.lines == null)
      return false;
    boolean showText = g3d.setC(text.colix);
    if (!showText && (text.image == null
        && (text.bgcolix == 0 || !g3d.setC(text.bgcolix))))
      return false;
    text.setPosition(scalePixelsPerMicron, imageFontScaling, isAbsolute, boxXY);
    // draw the box if necessary; colix has been set
    if (text.image == null) {
      // text colix will be opaque, but we need to render it in translucent pass 
      // now set x and y positions for text from (new?) box position
      if (text.bgcolix != 0) {
        if (g3d.setC(text.bgcolix))
          showBox(g3d, text.colix, (int) text.boxX,
              (int) text.boxY + text.boxYoff2 * 2, text.z + 2, text.zSlab,
              (int) text.boxWidth, (int) text.boxHeight, text.fontScale,
              !text.isEcho);
        if (!showText)
          return false;
      } // now write properly aligned text
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

    if (!doPointer)
      return true;
    drawLineXYZ(g3d, text.atomX, text.atomY, text.atomZ, text.boxX, text.boxY,
        text.zSlab, text.boxWidth, text.boxHeight, pointerColix, pointerWidth * (isAntialiased ? 2 : 1));
    return true;
  }

  private static void drawLineXYZ(JmolRendererInterface g3d, int x0, int y0,
                                  int z0, float x1, float y1, int z1, float w,
                                  float h, short pointerColix,
                                  int pointerWidth) {
    

    // This complex sequence ensures that the label is pointed to in a reasonable manner.
    
    float offsetX = x1 - x0;
    float offsetY = y1 - y0;

    // Set picking label and then drag!
    // System.out.println(offsetX  +"/" + w + " " + offsetY + "/" + h);
    if (offsetX <= 0 && -offsetX <= w
      && offsetY <= 0 && -offsetY <= h)
      return;
    
    boolean setX = (offsetY > 0 || offsetY < -h);
    float pt = Float.NaN;
    x1 += (setX ? (offsetX > w/2 ? 0 : offsetX < -w*3/2 ? w : (pt = w / 2))
        : (offsetX > 0 ? 0 : w));
    boolean setY = !Float.isNaN(pt);
    y1 += (setY && offsetY > 0 ? 0 : setY && offsetY < -h ? h : h / 2);
    if (pointerWidth > 1) {
      g3d.fillCylinderXYZ(pointerColix, pointerColix, GData.ENDCAPS_FLAT,
          pointerWidth, x0, y0, z0, (int) x1, (int) y1, z1);
    } else {
      g3d.setC(pointerColix);
      g3d.drawLineXYZ(x0, y0, z0, (int) x1, (int) y1, z1);
    }

  }

  static void renderSimpleLabel(JmolRendererInterface g3d, Font font,
                                String strLabel, short colix, short bgcolix,
                                float[] boxXY, int z, int zSlab, int xOffset,
                                int yOffset, float ascent, int descent,
                                boolean isAbsolute, boolean doPointer, short pointerColix,
                                int pointerWidth, boolean isAntialiased) {

    // old static style -- quick, simple, no line breaks, odd alignment?
    // LabelsRenderer only

    float w = font.stringWidth(strLabel) + 8;
    float h = ascent + descent + 8;

    int x0 = (int) boxXY[0];
    int y0 = (int) boxXY[1];

    Text.setBoxXY(w, h, xOffset, yOffset, boxXY, isAbsolute);

    float x = boxXY[0];
    float y = boxXY[1];
    if (bgcolix != 0 && g3d.setC(bgcolix)) {
      showBox(g3d, colix, (int) x, (int) y, z, zSlab, (int) w,
          (int) h, 1, true);
    } else {
      g3d.setC(colix);
    }
    g3d.drawString(strLabel, font, (int) (x + 4), (int) (y + 4 + ascent), z - 1,
        zSlab, bgcolix);
    if (doPointer && (xOffset != 0 || yOffset != 0)) {
      drawLineXYZ(g3d, x0, y0, zSlab, x, y, zSlab, w, h, pointerColix, pointerWidth * (isAntialiased ? 2 : 1));
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
