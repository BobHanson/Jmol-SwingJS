package org.jmol.awtjs2d;


import org.jmol.api.PlatformViewer;
import org.jmol.viewer.Viewer;

import javajs.util.P3d;
import javajs.util.PT;

/**
 * methods required by Jmol that access java.awt.Component
 * 
 * This package is not used in Jmol-SwingJS
 * 
 * private to org.jmol.awt
 * 
 */

public class Display {

  /**
   * @param canvas
   * @param widthHeight
   *   
   */
  static void getFullScreenDimensions(Object canvas, int[] widthHeight) {
    /**
     * @j2sNative
     * 
     * widthHeight[0] = canvas.width;
     * widthHeight[1] = canvas.height;
     * 
     */
    {}
  }
  
  static boolean hasFocus(Object canvas) {
    /**
     * @j2sNative
     * 
     */
    {
      System.out.println(canvas);
    }
    return true;
  }

  static void requestFocusInWindow(Object canvas) {
    /**
     * @j2sNative
     * 
     */
    {
      System.out.println(canvas);
    }
  }

  /**
   * legacy apps will use this
   * 
   * @param vwr
   * @param g
   * @param size
   */
  static void renderScreenImage(PlatformViewer vwr, Object g, Object size) {
    /**
     * @j2sNative
     * 
     */
    {
      System.out.println("" + vwr + g + size);
    }
  }

  
  //  static void setTransparentCursor(Object canvas) {
  //    /**
  //     * @j2sNative
  //     * 
  //     */
  //    {
  //      System.out.println(canvas);
  //    }
  //  }

  //  static void setCursor(Object vwr, int c) {
  //    Platform.Jmol().setCursor(((Viewer) vwr).html5Applet, c);
  //  }
  //

  /**
   * @param vwr
   * @param label
   * @param data
   *        default value or "|"-separated list; return will be one of these or
   *        null
   * @param list
   *        processed only if data is null; return will be "" + index
   * @param asButtons
   * @return "null" or result of prompt
   * @throws Exception will be ScriptInterruption()
   */
  public static String prompt(PlatformViewer vwr, String label, String data,
                              String[] list, boolean asButtons) throws Exception {
    if (!asButtons) {
      /**
       * @j2sNative
       * 
       *            var s = (data == null ? alert(label) : prompt(label, data));
       *            return "" + s;
       */
      {
      }
    }
    if (data != null)
      list = PT.split(data, "|");
    String option = ((Viewer) vwr).eval.promptAsync(label, list);
    if (option != null) {
      if (data == null) {
        for (int i = 0; i < list.length; i++)
          if (list[i].equals(data))
            return "" + i;
      }
      return option;
    }
    return "null";
  }

  public static void convertPointFromScreen(Object canvas, P3d ptTemp) {    
    /**
     * @j2sNative
     * 
     */
    {
      System.out.println("" + canvas + ptTemp);
    }
  }

  /**
   * Draw the completed image from rendering. Note that the
   * image buffer (org.jmol.g3d.Graphics3D.
   * @param context
   * @param canvas
   * @param x
   * @param y
   * @param width  unused in Jmol proper
   * @param height unused in Jmol proper
   * @param isDTI 
   */
  static void drawImage(Object context, Object canvas, int x, int y, int width, int height, boolean isDTI) {
    /*
     * fixed for stereo DTI, where width = canvas.width/2
     * red=imgData.data[0];
     * green=imgData.data[1];
     * blue=imgData.data[2];
     * alpha=imgData.data[3];
     */
  
    /**
     * @j2sNative
     * 
var buf8 = canvas.buf8;
var buf32 = canvas.buf32;
var n = canvas.width * canvas.height;
var di = 1;
if (isDTI) {
 var diw = width % 2; 
 width = Math.floor(width/2);
 di = Math.floor(canvas.width/width);
}
var dw = (canvas.width - width || x) * 4;
for (var i = 0, p = 0, j = x * 4; i < n;) {
buf8[j++] = (buf32[i] >> 16) & 0xFF;
buf8[j++] = (buf32[i] >> 8) & 0xFF;
buf8[j++] = buf32[i] & 0xFF;
buf8[j++] = 0xFF;
i += di;
if (++p%width==0) {
 if (diw) {
   i += 1;
   buf8[j] = 0;
   buf8[j+1] = 0;
   buf8[j+2] = 0;
   buf8[j+3] = 0;
 }
 j += dw;
}
}
context.putImageData(canvas.imgdata,0,0);
     */
    {
    }
  }



}
