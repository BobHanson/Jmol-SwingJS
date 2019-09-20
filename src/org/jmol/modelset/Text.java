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
package org.jmol.modelset;

import javajs.util.P3;
import javajs.util.P3i;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.BS;

import org.jmol.shape.Shape;
import org.jmol.util.C;
import org.jmol.util.Font;
import org.jmol.util.Txt;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

public class Text {
 
  private Viewer vwr;

  public boolean doFormatText;

  public Font font;
  private byte fid;
  private int ascent;
  public int descent;
  private int lineHeight;

  protected int offsetX; // Labels only
  protected int offsetY; // Labels only
  public  int boxYoff2;

  private int[] widths;

  private int textWidth;
  private int textHeight;
  public String text;
  public String textUnformatted;
  public String[] lines;
  
  public Object image;
  public float imageScale = 1;



  public Text() {
    // public for reflection
    // requires .newLabel or .newEcho
    boxXY =  new float[5];
  }

  static public Text newLabel(Viewer vwr, Font font, String text,
                              short colix, short bgcolix, int align, float scalePixelsPerMicron) {
    // for labels and hover
    Text t = new Text();
    t.vwr = vwr;
    t.set(font, colix, align, scalePixelsPerMicron);
    t.setText(text);
    t.bgcolix = bgcolix;
    return t;
  }
  
  static public Text newMeasure(Viewer vwr, Font font,
                              short colix) {
    Text t = new Text();
    t.vwr = vwr;
    t.set(font, colix, 0, 0);
    t.isMeasure = true;
    return t;
  }
  
  public static Text newEcho(Viewer vwr, Font font, String target,
                      short colix, int valign, int align,
                      float scalePixelsPerMicron) {
    Text t = new Text();
    t.isEcho = true;
    t.vwr = vwr;
    t.set(font, colix, align, scalePixelsPerMicron);
    t.target = target;
    t.valign = valign;
    t.z = 2;
    t.zSlab = Integer.MIN_VALUE;
    return t;
  }

  private void set(Font font, short colix, int align,
                   float scalePixelsPerMicron) {
    this.scalePixelsPerMicron = scalePixelsPerMicron;
    this.isEcho = isEcho;
    this.colix = colix;
    this.align = align;
    this.setFont(font, !isEcho);
  }
  
  public void setOffset(int offset) {
    //Labels only
    offsetX = JC.getXOffset(offset);
    offsetY = JC.getYOffset(offset);
    pymolOffset = null;
    valign = JC.ECHO_XY;
  }

  private void getFontMetrics() {
    descent = font.getDescent();
    ascent = font.getAscent();
    lineHeight = ascent + descent;
  }

  public void setFontFromFid(byte fid) { //labels only
    if (this.fid == fid)
      return;
    fontScale = 0;
    setFont(Font.getFont3D(fid), true);
  }

  public void setText(String text) {
    if (image != null)
      getFontMetrics();
    image = null;
    if (text != null && text.length() == 0)
      text = null;
    if (this.text != null && this.text.equals(text))
      return;
    this.text = textUnformatted = text;
    doFormatText = (isEcho && text != null && (text.indexOf("%{") >= 0 || text
        .indexOf("@{") >= 0));
    if (!doFormatText)
      recalc();
  }
  
  public void setImage(Object image) {
    this.image = image;
    // this.text will be file name
    recalc();
  }

  public void setScale(float scale) {
    imageScale = scale;
    recalc();
  }
  
  public void setFont(Font f3d, boolean doAll) {
    font = f3d;
    if (font == null)
      return;
    getFontMetrics();
    if (!doAll)
      return;
    fid = font.fid;
    recalc();
  }

  public void setFontScale(float scale) {
    if (fontScale == scale)
      return;
    fontScale = scale;
    if (fontScale != 0)
      setFont(vwr.gdata.getFont3DScaled(font, scale), true);
  }

  private void recalc() {
    if (image != null) {
      textWidth = textHeight = 0;
      boxWidth = vwr.apiPlatform.getImageWidth(image) * fontScale * imageScale;
      boxHeight = vwr.apiPlatform.getImageHeight(image) * fontScale * imageScale;
      ascent = 0;
      return;
    }
    if (text == null) {
      text = null;
      lines = null;
      widths = null;
      return;
    }
    if (font == null)
      return;
    lines = PT.split(text, (text.indexOf("\n") >= 0 ? "\n" : "|"));
    textWidth = 0;
    widths = new int[lines.length];
    for (int i = lines.length; --i >= 0;)
      textWidth = Math.max(textWidth, widths[i] = stringWidth(lines[i]));
    textHeight = lines.length * lineHeight;
    boxWidth = textWidth + (fontScale >= 2 ? 16 : 8);
    boxHeight = textHeight + (fontScale >= 2 ? 16 : 8);
  }

  public void setPosition(float scalePixelsPerMicron, float imageFontScaling,
                          boolean isAbsolute, float[] boxXY) {
    if (boxXY == null)
      boxXY = this.boxXY;
    else
      this.boxXY = boxXY;
    setWindow(vwr.gdata.width, vwr.gdata.height, scalePixelsPerMicron);
    if (scalePixelsPerMicron != 0 && this.scalePixelsPerMicron != 0)
      setFontScale(scalePixelsPerMicron / this.scalePixelsPerMicron);
    else if (fontScale != imageFontScaling)
      setFontScale(imageFontScaling);
    if (doFormatText) {
      text = (isEcho ? Txt.formatText(vwr, textUnformatted) : textUnformatted);
      recalc();
    }
    float dx = offsetX * imageFontScaling;
    float dy = offsetY * imageFontScaling;
    xAdj = (fontScale >= 2 ? 8 : 4);
    yAdj = ascent - lineHeight + xAdj;
    if (!isEcho || pymolOffset != null) {
      boxXY[0] = movableX;
      boxXY[1] = movableY;
      if (pymolOffset != null && pymolOffset[0] != 2 && pymolOffset[0] != 3) {
        // [1,2,3] are in Angstroms, not screen pixels
        float pixelsPerAngstrom = vwr.tm.scaleToScreen(z, 1000);
        float pz = pymolOffset[3];
        float dz = (pz < 0 ? -1 : 1) * Math.max(pz == 0 ? 0.5f : 0, Math.abs(pz) - 1)
            * pixelsPerAngstrom;
        z -= (int) dz;
        pixelsPerAngstrom = vwr.tm.scaleToScreen(z, 1000);

        /* for whatever reason, Java returns an 
         * ascent that is considerably higher than a capital X
         * forget leading!
         * ______________________________________________
         *                    leading                      
         *                   ________
         *     X X    
         *      X    ascent
         * __  X X _________ _________         
         * _________ descent 
         *                                   textHeight     
         * _________
         *     X X           lineHeight
         *      X    ascent
         * __  X X__________ _________        ___________        
         * _________ descent  
         *     
         *        
         * 
         */
        // dx and dy are the overall object offset, with text
        dx = getPymolXYOffset(pymolOffset[1], textWidth, pixelsPerAngstrom);
        int dh = ascent - descent;
        dy = -getPymolXYOffset(-pymolOffset[2], dh, pixelsPerAngstrom)
            - (textHeight + dh) / 2;

        //dy: added -lineHeight (for one line)
        if (pymolOffset[0] == 1) {
          // from PyMOL - back to original plan
          dy -= descent;
        }

        // xAdj and yAdj are the adjustments for the box itself relative to the text 
        xAdj = (fontScale >= 2 ? 8 : 4);
        yAdj = -descent;
        boxXY[0] = movableX - xAdj;
        boxXY[1] = movableY - yAdj;
        isAbsolute = true;
        boxYoff2 = -2; // empirical fudge factor
      } else {
        boxYoff2 = 0;
      }
      if (pymolOffset == null)
        switch (align) {
        case JC.TEXT_ALIGN_CENTER:
          dy = 0;
          dx = 0;
          break;
        case JC.TEXT_ALIGN_RIGHT:
          boxXY[0] -= boxWidth;
          //$FALL-THROUGH$
        case JC.TEXT_ALIGN_LEFT:
          dy = 0;
        }
      //System.out.println(dx + " Text " + dy + " " + boxWidth + " " + boxHeight);
      setBoxXY(boxWidth, boxHeight, dx, dy, boxXY, isAbsolute);
    } else {
      setPos(fontScale);
    }
    boxX = boxXY[0];
    boxY = boxXY[1];

    // adjust positions if necessary

    if (adjustForWindow)
      setBoxOffsetsInWindow(/*image == null ? fontScale * 5 :*/0,
          isEcho ? 0 : 16 * fontScale + lineHeight, boxY - textHeight);
    //if (!isAbsolute)
    y0 = boxY + yAdj;
    if (isMeasure && align != JC.TEXT_ALIGN_CENTER)
      y0 += ascent + (lines.length - 1)/2f * lineHeight;
  }

  private float getPymolXYOffset(float off, int width, float ppa) {
    float f = (off < -1 ? -1 : off > 1 ? 0 : (off - 1) / 2);
    // offset
    // -3     -2
    // -2     -1
    // -1      0 absolute, -1 width
    //-0.5    -3/4  width
    //  0     -1/2 width
    // 0.5    -1/4 width
    //  1      0
    //  2      1
    //  3      2
    off = (off < -1 || off > 1 ? off + (off < 0 ? 1 : -1) : 0);
    return f * width + off * ppa;
  }

  private void setPos(float scale) {
    float xLeft, xCenter, xRight;
    boolean is3dEcho = (xyz != null);
    if (valign == JC.ECHO_XY || valign == JC.ECHO_XYZ) {
      float x = (movableXPercent != Integer.MAX_VALUE ? movableXPercent
          * windowWidth / 100 : is3dEcho ? movableX : movableX * scale);
      float offsetX = this.offsetX * scale;
      xLeft = xRight = xCenter = x + offsetX;
    } else {
      xLeft = 5 * scale;
      xCenter = windowWidth / 2;
      xRight = windowWidth - xLeft;
    }

    // set box X from alignments

    boxXY[0] = xLeft;
    switch (align) {
    case JC.TEXT_ALIGN_CENTER:
      boxXY[0] = xCenter - boxWidth / 2;
      break;
    case JC.TEXT_ALIGN_RIGHT:
      boxXY[0] = xRight - boxWidth;
    }

    // set box Y from alignments

    boxXY[1] = 0;
    switch (valign) {
    case JC.ECHO_TOP:
      break;
    case JC.ECHO_MIDDLE:
      boxXY[1] = windowHeight / 2;
      break;
    case JC.ECHO_BOTTOM:
      boxXY[1] = windowHeight;
      break;
    default:
      float y = (movableYPercent != Integer.MAX_VALUE ? movableYPercent
          * windowHeight / 100 : is3dEcho ? movableY : movableY * scale);
      boxXY[1] = (is3dEcho ? y : (windowHeight - y)) + offsetY * scale;
   }

    if (align == JC.TEXT_ALIGN_CENTER)
      boxXY[1] -= (image != null ? boxHeight : xyz != null ? boxHeight 
          : ascent - boxHeight) / 2;
    else if (image != null)
      boxXY[1] -= 0;
    else if (xyz != null)
      boxXY[1] -= ascent / 2;
  }

  public static void setBoxXY(float boxWidth, float boxHeight, float xOffset,
                               float yOffset, float[] boxXY, boolean isAbsolute) {
    float xBoxOffset, yBoxOffset;

    // these are based on a standard |_ grid, so y is reversed.
    if (xOffset > 0 || isAbsolute) {
      xBoxOffset = xOffset;
    } else {
      xBoxOffset = -boxWidth;
      if (xOffset == 0)
        xBoxOffset /= 2;
      else
        xBoxOffset += xOffset;
    }
    if (isAbsolute || yOffset > 0) {
      yBoxOffset = -boxHeight - yOffset;
    } else if (yOffset == 0) {
      yBoxOffset = -boxHeight / 2; // - 2; removed in Jmol 11.7.45 06/24/2009
    } else {
      yBoxOffset = -yOffset;
    }
    boxXY[0] += xBoxOffset;
    boxXY[1] += yBoxOffset;
    boxXY[2] = boxWidth;
    boxXY[3] = boxHeight;
  }
  
  private int stringWidth(String str) {
    int w = 0;
    int f = 1;
    int subscale = 1; //could be something less than that
    if (str == null)
      return 0;
    if (str.indexOf("<su") < 0 && str.indexOf("<color") < 0)
      return font.stringWidth(str);
    int len = str.length();
    String s;
    for (int i = 0; i < len; i++) {
      if (str.charAt(i) == '<') {
        if (i + 8 <= len && 
            (str.substring(i, i + 7).equals("<color ") || str.substring(i, i + 8).equals("</color>"))) {
          int i1 = str.indexOf(">", i);
          if (i1 >= 0) {
            i = i1;
            continue;
          }
        }
        if (i + 5 <= len
            && ((s = str.substring(i, i + 5)).equals("<sub>") || s
                .equals("<sup>"))) {
          i += 4;
          f = subscale;
          continue;
        }
        if (i + 6 <= len
            && ((s = str.substring(i, i + 6)).equals("</sub>") || s
                .equals("</sup>"))) {
          i += 5;
          f = 1;
          continue;
        }
      }
      w += font.stringWidth(str.substring(i, i + 1)) * f;
    }
    return w;
  }

  private float xAdj, yAdj;

  private float y0;

  public P3 pointerPt; // for echo

  public void setXYA(float[] xy, int i) {
    if (i == 0) {
      xy[2] = boxX;
      switch (align) {
      case JC.TEXT_ALIGN_CENTER:
        xy[2] += boxWidth / 2;
        break;
      case JC.TEXT_ALIGN_RIGHT:
        xy[2] += boxWidth - xAdj;
        break;
      default:
        xy[2] += xAdj;
      }
      xy[0] = xy[2];
      xy[1] = y0;
    }
    switch (align) {
    case JC.TEXT_ALIGN_CENTER:
      xy[0] = xy[2] - widths[i] / 2;
      break;
    case JC.TEXT_ALIGN_RIGHT:
      xy[0] = xy[2] - widths[i];
    }
    xy[1] += lineHeight;
  }

  public void appendFontCmd(SB s) {
    s.append("  " + Shape.getFontCommand("echo", font));
    if (scalePixelsPerMicron > 0)
      s.append(" " + (10000f / scalePixelsPerMicron)); // Angstroms per pixel
  }

  public boolean isMeasure;
  public boolean isEcho;
  public P3 xyz;
  public String target;
  public String script;
  public short colix;
  public short bgcolix;
  public int pointer;
  public float fontScale;

  public int align;
  public int valign;
  public int atomX, atomY, atomZ = Integer.MAX_VALUE;
  public int movableX, movableY, movableZ; // Echo only
  public int movableXPercent = Integer.MAX_VALUE; // Echo only
  public int movableYPercent = Integer.MAX_VALUE; // Echo only
  public int movableZPercent = Integer.MAX_VALUE; // Echo only

  public int z = 1; // front plane
  public int zSlab = Integer.MIN_VALUE; // z for slabbing purposes -- may be near an atom

  // PyMOL-type offset
  // [mode, screenoffsetx,y,z (applied after tranform), positionOffsetx,y,z (applied before transform)]
  public float[] pymolOffset;

  protected int windowWidth;
  protected int windowHeight;
  public boolean adjustForWindow;
  public float boxWidth;
  public float boxHeight;
  public float boxX;
  public float boxY;

  public int modelIndex = -1;
  public boolean visible = true;
  public boolean hidden = false;

  public float[] boxXY;

  public float scalePixelsPerMicron;

  public void setScalePixelsPerMicron(float scalePixelsPerMicron) {
    fontScale = 0;//fontScale * this.scalePixelsPerMicron / scalePixelsPerMicron;
    this.scalePixelsPerMicron = scalePixelsPerMicron;
  }

  public void setXYZ(P3 xyz, boolean doAdjust) {
    this.xyz = xyz;
    if (xyz == null)
      this.zSlab = Integer.MIN_VALUE;
    if (doAdjust) {
      valign = (xyz == null ? JC.ECHO_XY : JC.ECHO_XYZ);
     adjustForWindow = (xyz == null);
    }
  }

  public void setTranslucent(float level, boolean isBackground) {
    if (isBackground) {
      if (bgcolix != 0)
        bgcolix = C.getColixTranslucent3(bgcolix, !Float.isNaN(level), level);
    } else {
      colix = C.getColixTranslucent3(colix, !Float.isNaN(level), level);
    }
  }

  public void setMovableX(int x) {
    valign = (valign == JC.ECHO_XYZ ? JC.ECHO_XYZ : JC.ECHO_XY);
    movableX = x;
    movableXPercent = Integer.MAX_VALUE;
  }

  public void setMovableY(int y) {
    valign = (valign == JC.ECHO_XYZ ? JC.ECHO_XYZ : JC.ECHO_XY);
    movableY = y;
    movableYPercent = Integer.MAX_VALUE;
  }

  //  public void setMovableZ(int z) {
  //    if (valign != VALIGN_XYZ)
  //      valign = VALIGN_XY;
  //    movableZ = z;
  //    movableZPercent = Integer.MAX_VALUE;
  //  }

  public void setMovableXPercent(int x) {
    valign = (valign == JC.ECHO_XYZ ? JC.ECHO_XYZ : JC.ECHO_XY);
    movableX = Integer.MAX_VALUE;
    movableXPercent = x;
  }

  public void setMovableYPercent(int y) {
    valign = (valign == JC.ECHO_XYZ ? JC.ECHO_XYZ : JC.ECHO_XY);
    movableY = Integer.MAX_VALUE;
    movableYPercent = y;
  }

  public void setMovableZPercent(int z) {
    if (valign != JC.ECHO_XYZ)
      valign = JC.ECHO_XY;
    movableZ = Integer.MAX_VALUE;
    movableZPercent = z;
  }

  public void setZs(int z, int zSlab) {
    this.z = z;
    this.zSlab = zSlab;
  }

  public void setXYZs(int x, int y, int z, int zSlab) {
    setMovableX(x);
    setMovableY(y);
    setZs(z, zSlab);
  }

  public void setScript(String script) {
    this.script = (script == null || script.length() == 0 ? null : script);
  }

  public boolean setAlignmentLCR(String align) {
    if ("left".equals(align))
      return setAlignment(JC.TEXT_ALIGN_LEFT);
    if ("center".equals(align))
      return setAlignment(JC.TEXT_ALIGN_CENTER);
    if ("right".equals(align))
      return setAlignment(JC.TEXT_ALIGN_RIGHT);
    return false;
  }

  public boolean setAlignment(int align) {
    if (this.align != align) {
      this.align = align;
      recalc();
    }
    return true;
  }

  public void setBoxOffsetsInWindow(float margin, float vMargin, float vTop) {
    // not labels

    // these coordinates are (0,0) in top left
    // (user coordinates are (0,0) in bottom left)
    float bw = boxWidth + margin;
    float x = boxX;
    if (x + bw > windowWidth)
      x = windowWidth - bw;
    if (x < margin)
      x = margin;
    boxX = x;

    float bh = boxHeight;
    float y = vTop;
    if (y + bh > windowHeight)
      y = windowHeight - bh;
    if (y < vMargin)
      y = vMargin;
    boxY = y;
  }

  public void setWindow(int width, int height, float scalePixelsPerMicron) {
    windowWidth = width;
    windowHeight = height;
    if (pymolOffset == null && this.scalePixelsPerMicron < 0
        && scalePixelsPerMicron != 0)
      setScalePixelsPerMicron(scalePixelsPerMicron);
  }

  public boolean checkObjectClicked(boolean isAntialiased, int x, int y,
                                    BS bsVisible) {
    if (hidden || script == null || modelIndex >= 0 && !bsVisible.get(modelIndex))
      return false;
    if (isAntialiased) {
      x <<= 1;
      y <<= 1;
    }
    return (x >= boxX && x <= boxX + boxWidth && y >= boxY && y <= boxY
        + boxHeight);
  }

//  
//  new feature: set labelOffset [mode sx sy sz ax ay az] (3.1.15, never documented)
//  new feature: PyMOL-like label offset options:
//     
//     set labelOffset [sx, sy, sz]
//     set labelOffset [mode, sx, sy, sz, ax, ay, az]
//     
//   where
//     
//     sx,sy,sz are screen coord offsets 
//      -- applied after view rotation
//      -- sy > 0 LOWERS label
//     ax,ay,az are xyz position (in Angstroms; applied before view rotation)
//     mode == 0 indicates xyz position is absolute and sx sy sz are Angstroms
//     mode == 1 indicates xyz position is relative to atom position and sx sy sz are Angstroms
//     mode == 2 indicates xyz is absolute, and sx sy sz positions are screen pixels
//     mode == 3 indicates xyz is relative, and sx sy sz positions are screen pixels
//     defaults: mode == 1; ax = ay = az = 0
//     
//     

  /**
   * PyMOL will use 1 here for pymolOffset[0] for relative, 0 or absolute. Jmol
   * set labelOffset or set echo offset or measure offset will set -1, when
   * using {sx sy sz}.
   * 
   * 
   * @param atomPt
   * @param screen
   * @param zSlab
   * @param pTemp
   * @param sppm
   */
  public void getPymolScreenOffset(P3 atomPt, P3i screen, int zSlab, P3 pTemp,
                                   float sppm) {
    float mode = pymolOffset[0];
    if (atomPt != null && (Math.abs(mode) % 2) == 1)
      pTemp.setT(atomPt);
    else
      pTemp.set(0, 0, 0);
    pTemp.add3(pymolOffset[4], pymolOffset[5], pymolOffset[6]);
    vwr.tm.transformPtScr(pTemp, screen);
    if (mode == 2 || mode == 3) {
      screen.x += pymolOffset[1];
      screen.y += pymolOffset[2];
      screen.z += pymolOffset[3];
    }
    setXYZs(screen.x, screen.y, screen.z, zSlab);
    setScalePixelsPerMicron(sppm);
  }

}
