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
import org.jmol.util.C;
import org.jmol.util.Font;
import org.jmol.util.GData;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.util.P3i;

class TextRenderer {

  static final int MODE_IS_ANTIALIASED = 4;

  static boolean render(Viewer vwr, Text text,
                        JmolRendererInterface g3d, double scalePixelsPerMicron,
                        double imageFontScaling, double[] boxXY, double[] temp,
                        P3i pTemp, short pointerColix, int pointerWidth,
                        int mode) {
    if (text == null
        || text.image == null && !text.doFormatText && text.lines == null)
      return false;
    boolean isAbsolute = ((mode & JC.LABEL_EXPLICIT) != 0);
    boolean doPointer = ((mode & JC.LABEL_POINTER_ON) != 0);
    boolean isAntialiased = ((mode & MODE_IS_ANTIALIASED) != 0);
    short colix = text.colix;
    if (text.isEcho && C.getArgb(colix) == JC.COLOR_CONTRAST)
      colix = vwr.cm.colixBackgroundContrast;
    boolean showText = g3d.setC(colix);
    if (!showText && (text.image == null
        && (text.bgcolix == 0 || !g3d.setC(text.bgcolix))))
      return false;
    if (text.isEcho && text.valign == JC.ECHO_XYZ)
      calcBarPixelsXYZ(vwr, text, pTemp, false);
    text.setPosition(scalePixelsPerMicron, imageFontScaling, isAbsolute, boxXY);
    int barPixels = (text.isEcho && text.valign == JC.ECHO_XYZ
        ? calcBarPixelsXYZ(vwr, text, pTemp, false)
        : text.barPixels);

    // draw the box if necessary; colix has been set
    if (text.image == null) {
      // text colix will be opaque, but we need to render it in translucent pass 
      // now set x and y positions for text from (new?) box position
      if (text.bgcolix != 0) {
        if (g3d.setC(text.bgcolix))
          showBox(g3d, text.colix,
              (int) text.boxX - (barPixels == 0 ? 0 : barPixels + 4),
              (int) text.boxY + text.boxYoff2 * 2, text.z + 2, text.zSlab,
              (int) text.boxWidth + barPixels, (int) text.boxHeight,
              text.fontScale, !text.isEcho);
        if (!showText)
          return false;
      } // now write properly aligned text
      for (int i = 0; i < text.lines.length; i++) {
        text.setXYA(temp, i);
        if (text.xyz != null)
          temp[1] += 2; // fudge
        g3d.drawString(text.lines[i], text.font, (int) temp[0], (int) temp[1],
            text.z, text.zSlab, text.bgcolix);
      }
      if (text.barPixels > 0) {
        renderScale(g3d, text, temp, barPixels, isAntialiased);
      }
    } else {
      g3d.drawImage(text.image, (int) text.boxX, (int) text.boxY, text.z,
          text.zSlab, text.bgcolix, (int) text.boxWidth, (int) text.boxHeight);
    }
    // now draw the pointer, if requested

    if (!doPointer)
      return true;
    drawLineXYZ(g3d, text.atomX, text.atomY, text.atomZ, text.boxX, text.boxY,
        text.zSlab, text.boxWidth, text.boxHeight, pointerColix,
        pointerWidth * (isAntialiased ? 2 : 1));
    return true;
  }

  static int calcBarPixelsXYZ(Viewer vwr, Text t, P3i pTemp,
                              boolean andSet) {
    int barPixels = t.barPixels;
    if (t.xyz != null) {
      vwr.tm.transformPtScr(t.xyz, pTemp);
      if (andSet)
        t.setXYZs(pTemp.x, pTemp.y, pTemp.z, pTemp.z);
      if (barPixels > 0 && vwr.tm.perspectiveDepth) {
        double d = vwr.tm.unscaleToScreen(pTemp.z, barPixels);
        barPixels = t.barPixelsXYZ = (int) (barPixels * t.barDistance / d);
      }
    }
    return barPixels;
  }

  /**
   * Render a short |---| bar with label from ECHO "%SCALE"
   * 
   * @param g3d
   * @param text
   * @param temp
   * @param barPixels
   * @param isAntialiased
   */
  private static void renderScale(JmolRendererInterface g3d, Text text,
                                  double[] temp, int barPixels,
                                  boolean isAntialiased) {
    int z = text.z;
    int xoff = (text.xyz == null ? 0 : 2);
    // barPixels has a 4-pixel margin
    int ia = (isAntialiased ? 2 : 1);
    //for (int i = (ia == 2 ? 3 : 1); i > 0; i -= 1) {
    int i = 1;
    int x1 = xoff + (int) temp[0] - barPixels - i - ia * 2;
    int x2 = xoff + (int) temp[0] - i - ia * 2;
    int h = (text.lineHeight) / 2;
    int y = (int) temp[1] - i;
    g3d.fillTextRect(x1, y - h / 2 - ia, z, text.zSlab, x2 - x1, 2 * ia);
    g3d.fillTextRect(x1, y - h * 2 / 2, z, text.zSlab, 2 * ia, h * 2 / 2);
    g3d.fillTextRect(x2, y - h * 2 / 2, z, text.zSlab, 2 * ia, h * 2 / 2);
    for (int j = 1; j < 10; j++) {
      int x1b = x1 + j * barPixels / 10;
      int len = (j == 5 ? h : h / 2);
      g3d.fillTextRect(x1b, y - len, z, text.zSlab, 2 * ia, len);

    }
    //
    //    g3d.drawLinePixels(sA, sB, text.z, text.zSlab);
    //    sA.y = y + h;
    //    sB.y = y - h;    
    //    sB.x = x1;
    //    g3d.drawLinePixels(sA, sB, text.z, text.zSlab);
    //    sA.x = sB.x = x2;
    //    g3d.drawLinePixels(sA, sB, text.z, text.zSlab);
    //    
    //}
  }

  private static void drawLineXYZ(JmolRendererInterface g3d, int x0, int y0,
                                  int z0, double x1, double y1, int z1,
                                  double w, double h, short pointerColix,
                                  int pointerWidth) {

    // This complex sequence ensures that the label is pointed to in a reasonable manner.

    double offsetX = x1 - x0;
    double offsetY = y1 - y0;

    // Set picking label and then drag!

    if (offsetX <= 0 && -offsetX <= w && offsetY <= 0 && -offsetY <= h)
      return;

    boolean setX = (offsetY > 0 || offsetY < -h);
    double pt = Double.NaN;
    x1 += (setX
        ? (offsetX > w / 2 ? 0 : offsetX < -w * 3 / 2 ? w : (pt = w / 2))
        : (offsetX > 0 ? 0 : w));
    boolean setY = !Double.isNaN(pt);
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
                                double[] boxXY, int z, int zSlab, int xOffset,
                                int yOffset, double ascent, int descent,
                                short pointerColix, int pointerWidth,
                                int mode) {

    // old static style -- quick, simple, no line breaks, odd alignment?
    // LabelsRenderer only

    double w = font.stringWidth(strLabel) + 8;
    double h = ascent + descent + 8;

    int x0 = (int) boxXY[0];
    int y0 = (int) boxXY[1];

    boolean isAbsolute = ((mode & JC.LABEL_EXPLICIT) != 0);
    boolean doPointer = ((mode & JC.LABEL_POINTER_ON) != 0);
    boolean isAntialiased = ((mode & MODE_IS_ANTIALIASED) != 0);
    Text.setBoxXY(w, h, xOffset, yOffset, boxXY, isAbsolute);

    double x = boxXY[0];
    double y = boxXY[1];
    if (bgcolix != 0 && g3d.setC(bgcolix)) {
      showBox(g3d, colix, (int) x, (int) y, z, zSlab, (int) w, (int) h, 1,
          true);
    } else {
      g3d.setC(colix);
    }
    g3d.drawString(strLabel, font, (int) (x + 4), (int) (y + 4 + ascent), z - 1,
        zSlab, bgcolix);
    if (doPointer && (xOffset != 0 || yOffset != 0)) {
      drawLineXYZ(g3d, x0, y0, zSlab, x, y, zSlab, w, h, pointerColix,
          pointerWidth * (isAntialiased ? 2 : 1));
    }
  }

  private static void showBox(JmolRendererInterface g3d, short colix, int x,
                              int y, int z, int zSlab, int boxWidth,
                              int boxHeight, double imageFontScaling,
                              boolean atomBased) {
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
