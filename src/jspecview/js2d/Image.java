/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2006  The Jmol Development Team
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package jspecview.js2d;

import org.jmol.util.Font;

/**
 * methods required by Jmol that access java.awt.Image
 * 
 * private to org.jmol.awt
 * 
 */
class Image {

  /**
   * @param canvas
   * @return width
   */
  static int getWidth(Object canvas) {
    /**
     * @j2sNative
     * 
     *            return (canvas.imageWidth ? canvas.imageWidth : canvas.width);
     */
    {
      return 0;
    }
  }

  /**
   * @param canvas
   * @return width
   */
  static int getHeight(Object canvas) {
    /**
     * @j2sNative
     * 
     *            return (canvas.imageHeight ? canvas.imageHeight : canvas.height);
     */
    {
      return 0;
    }
  }

  /**
   * @param context
   * @param width
   * @param height
   * @return null
   */
  static int[] grabPixels(Object context, int width, int height) {
  	int[] data = null;
    /**
     * @j2sNative
     *            if (context._buf32) return context._buf32; // non-canvas internal buffer for image writing
     *            data = context.getImageData(0, 0, width, height).data;
     */
    {
    }
    return toIntARGB(data);
  }

  static int[] toIntARGB(int[] imgData) {
    /*
     * red=imgData.data[0];
     * green=imgData.data[1];
     * blue=imgData.data[2];
     * alpha=imgData.data[3];
     */
    int n = imgData.length / 4;
    int[] iData = new int[n];
    for (int i = 0, j = 0; i < n; j++) {
      iData[i++] = (imgData[j++] << 16) | (imgData[j++] << 8) | imgData[j++] | 0xFF000000;
    }
    return iData;
  }
  
  static void fromIntARGB(int[] buf32, int[] buf8) {
    /*
     * red=imgData.data[0];
     * green=imgData.data[1];
     * blue=imgData.data[2];
     * alpha=imgData.data[3];
     */
    int n = buf8.length >> 2;
      for (int i = 0, j = 0; i < n; i++) {
        buf8[j++] = (buf32[i] >> 16) & 0xFF;
        buf8[j++] = (buf32[i] >> 8) & 0xFF;
        buf8[j++] = buf32[i] & 0xFF;
        buf8[j++] = 0xFF;
      }
  }
  
  /**
   * @param text  
   * @param font3d 
   * @param context 
   * @param width 
   * @param height 
   * @param ascent 
   * @return array
   */
  public static int[] getTextPixels(String text, Font font3d, Object context, 
                                    int width, int height, int ascent) {
    /**
     * @j2sNative
     * 
     * context.fillStyle = "#000000";
     * context.fillRect(0, 0, width, height);
     * context.fillStyle = "#FFFFFF";
     * context.font = font3d.font;
     * context.fillText(text, 0, ascent);
     */
    {
    }
    return grabPixels(context, width, height);
  }

  /**
   * @param windowWidth
   * @param windowHeight
   * @param pBuffer
   * @param windowSize
   * @param backgroundTransparent
   * @param canvas
   * @return a canvas
   */
  static Object allocateRgbImage(int windowWidth, int windowHeight,
                                 int[] pBuffer, int windowSize,
                                 boolean backgroundTransparent, Object canvas) {
    /**
     * 
     * null canvas indicates we are just creating a buffer for image writing
     * 
     * @j2sNative 
     * 
     * if (canvas == null) 
     *   canvas = {width:windowWidth,height:windowHeight};
     * canvas.buf32 = pBuffer; 
     */
    {
      return canvas;
    }
  }

  /**
   * @param canvas  
   * @param backgroundTransparent 
   * @return context
   */
  static Object getStaticGraphics(Object canvas, boolean backgroundTransparent) {
    // for text processing;
    return getGraphics(canvas);
  }

  /**
   * @param canvas  
   * @return context
   */
  static Object getGraphics(Object canvas) {
    /**
     * @j2sNative
     * 
     * return canvas.getContext("2d");
     */
    {
      return null;
    }
  }

  /**
   * 
   * @param context
   * @param canvas
   * @param x
   * @param y
   * @param width  unused in Jmol proper
   * @param height unused in Jmol proper
   */
  static void drawImage(Object context, Object canvas, int x, int y, int width, int height) {
    /**
     * @j2sNative
     * 
     * this.fromIntARGB(canvas.buf32, canvas.buf8);
     * context.putImageData(canvas.imgdata,x,y);
     * 
     */
    {
      fromIntARGB(null, null);
    }
  }

}
