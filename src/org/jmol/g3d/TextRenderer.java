/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-01-06 06:26:15 -0600 (Wed, 06 Jan 2016) $
 * $Revision: 20923 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.g3d;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.JmolRendererInterface;
import org.jmol.util.Font;

import javajs.util.CU;



/**
 * implementation for text rendering
 *<p>
 * uses java fonts by rendering into an offscreen buffer.
 * strings are rasterized, and 4-bit translucency is stored as byte[] tmap.
 *<p>
 *
 * @author Miguel, miguel@jmol.org
 * @author Bob Hanson, hansonr@stolaf.edu
 */ 
class TextRenderer {
  /*
    we have a few problems here
    a message is probably going to vary in size with z depth
    a message is probably going to be repeated by more than one atom
    fonts?
      just one?
      a restricted number?
      any font?
      if we want to support more than one then a fontindex is probably
      called for in order to prevent a hashtable lookup
    color - can be applied by the painter
    rep
      array of booleans - uncompressed
      array of bits - uncompressed - i like this
      some type of run-length, using bytes
  */
  private int height; // this height is just ascent + descent ... no reason for leading
  private int ascent;
  private int width;
  private int mapWidth;
  private int size;
  private byte[] tmap;
  private boolean isInvalid;
  private final static byte[] translucency = new byte[] { 7, 6, 5, 4, 3, 2, 1, 8 };
  private static boolean working;
  private final static Map<Font, Map<String, TextRenderer>> htFont3d = new Hashtable<Font, Map<String, TextRenderer>>();
  private final static Map<Font, Map<String, TextRenderer>> htFont3dAntialias = new Hashtable<Font, Map<String, TextRenderer>>();

  synchronized static void clearFontCache() {
    if (working)
      return;
    htFont3d.clear();
    htFont3dAntialias.clear();
  }

  static int plot(int x, int y, int z, int argb, int bgargb,
                         String text, Font font3d,
                         Graphics3D g3d, JmolRendererInterface jr, boolean antialias) {
    if (text.length() == 0)
      return 0;
    //System.out.println(x + "  " + y + " " + text);
    if (text.indexOf("<su") >= 0 || text.indexOf("<color") >= 0)
      return plotByCharacter(x, y, z, argb, bgargb, text, font3d, g3d, jr,
          antialias);
    int offset = font3d.getAscent();
    //if (antialias)
      //offset += offset;
    y -= offset;

    //setColix has presumably been carried out for argb, and the two 
    //are assumed to be both the same -- translucent or not. 
    TextRenderer text3d = getPlotText3D(x, y, g3d, text, font3d, antialias);
    if (text3d.isInvalid)
      return text3d.width;
    //TODO: text width/height are calculated 4x correct size here when antialiased.
    // this is wasteful, as it requires drawing larger than necessary images
    if (antialias)
      argb = Graphics3D.fixTextImageRGB(argb);
    int textHeight = text3d.height;
    int textWidth = text3d.width;
    byte[] tmap = text3d.tmap;
    Graphics3D g = g3d;
    int width = g.width;
    int height = g.height;
    int[] zbuf = g.zbuf;
    Pixelator p = g.pixel;
    int tLog = g.translucencyLog;
    
    if (jr != null
        || (x < 0 || x + text3d.width > width || y < 0 || y + text3d.height > height) 
        && (jr = g3d) != null) {
      for (int off = 0, i = 0; i < textHeight; i++) {
        for (int j = 0; j < textWidth; j++) {
          byte shade = tmap[off++];
          if (shade != 0)
            jr.plotImagePixel(argb, x + j, y + i, z, shade, bgargb, width, height, zbuf, p, tLog);
        }
      }      
    } else {
      for (int i = 0, off = 0, pbufOffset = y * width + x; i < textHeight; i++, pbufOffset += (width - textWidth))
        for (int j = 0; j < textWidth; j++)
          p.addImagePixel(tmap[off++], tLog, pbufOffset++, z, argb, bgargb);
    }
    return text3d.width;
  }

  private static int plotByCharacter(int x, int y, int z, int argb, int bgargb,
                                      String text, Font font3d, 
                                      Graphics3D g3d, JmolRendererInterface jmolRenderer,
                                      boolean antialias) {
    //int subscale = 1; //could be something less than that
    int w = 0;
    int len = text.length();
    int suboffset = Math.round(font3d.getHeight() * 0.25f);
    int supoffset = -Math.round(font3d.getHeight() * 0.3f);
    int argb0 = 0;
    for (int i = 0; i < len; i++) {
      if (text.charAt(i) == '<') {
        if (i + 5 < len && text.substring(i, i + 6).equals("<color")) {
          argb0 = argb;
          int pt = text.indexOf(">", i);
          if (pt < 0)
            continue;
          argb = CU.getArgbFromString(text.substring(i + 7, pt).trim());
          i = pt;
          continue;
        }
        if (i + 7 < len && text.substring(i, i + 8).equals("</color>")) {
          i += 7;
          argb = argb0;
          continue;
        }
        if (i + 4 < len && text.substring(i, i + 5).equals("<sub>")) {
          i += 4;
          y += suboffset;
          continue;
        }
        if (i + 4 < len && text.substring(i, i + 5).equals("<sup>")) {
          i += 4;
          y += supoffset;
          continue;
        }
        if (i + 5 < len  && text.substring(i, i + 6).equals("</sub>")) {
          i += 5;
          y -= suboffset;
          continue;
        }
        if (i + 5 < len  && text.substring(i, i + 6).equals("</sup>")) {
          i += 5;
          y -= supoffset;
          continue;
        }
      }
      int width = plot(x + w, y, z, argb, bgargb, text.substring(i, i + 1), 
          font3d, g3d, jmolRenderer, antialias);
      w += width;
    }
    //System.out.println("w=" + w);
    return w;
  }
  
  /**
   * 
   * @param text
   * @param font3d
   */
  private TextRenderer(String text, Font font3d) {
    ascent = font3d.getAscent();
    height = font3d.getHeight();
    width = font3d.stringWidth(text);
    if (width == 0)
      return;
    mapWidth = width;
    size = mapWidth * height;
  }
  
  private synchronized static TextRenderer getPlotText3D(int x, int y, Graphics3D g3d,
                                               String text, Font font3d,
                                               boolean antialias) {
    TextRenderer.working = true;
    Map<Font, Map<String, TextRenderer>> ht = (antialias ? TextRenderer.htFont3dAntialias : TextRenderer.htFont3d);
    Map<String, TextRenderer> htForThisFont = ht.get(font3d);
    TextRenderer text3d = null;
    boolean newFont = false;
    boolean newText = false;
    if (htForThisFont != null) {
      text3d = htForThisFont.get(text);
    } else {
      htForThisFont = new Hashtable<String, TextRenderer>();
      newFont = true;
    }
    if (text3d == null) {
      text3d = new TextRenderer(text, font3d);
      newText = true;
    }
    text3d.isInvalid = (text3d.width == 0 || x + text3d.width <= 0
        || x >= g3d.width || y + text3d.height <= 0 || y >= g3d.height);
    if (text3d.isInvalid)
      return text3d;
    if (newFont)
      ht.put(font3d, htForThisFont);
    if (newText) {
      text3d.setTranslucency(text, font3d, g3d);
      htForThisFont.put(text, text3d);
    }
    TextRenderer.working = false;
    return text3d;
  }

  /**
   * retrieve grey-scale pixel map from the platform, then round it off
   * 
   * @param text
   * @param font3d
   * @param g3d
   */
  private void setTranslucency(String text, Font font3d, Graphics3D g3d) {
    int[] pixels = g3d.apiPlatform.getTextPixels(text, font3d, g3d.platform
        .getGraphicsForTextOrImage(mapWidth, height),
        g3d.platform.offscreenImage, mapWidth, height, ascent);
    if (pixels == null)
      return;
    tmap = new byte[size];
    for (int i = pixels.length; --i >= 0;) {
      int p = pixels[i] & 0xFF;
      if (p != 0) {
        tmap[i] = translucency[p >> 5]; // 3-bit precision
      }
    }
  }

}
