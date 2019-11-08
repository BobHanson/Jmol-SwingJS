package jspecview.js2d;


import org.jmol.api.PlatformViewer;

import javajs.util.P3;


/**
 * methods required by Jmol that access java.awt.Component
 * 
 * private to org.jmol.awt
 * 
 */

class Display {

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
//    /**
//     * @j2sNative
//     * 
//     */
//    {
//      System.out.println(canvas);
//    }
    return true;
  }

  static void requestFocusInWindow(Object canvas) {
//    /**
//     * @j2sNative
//     * 
//     */
//    {
//      System.out.println(canvas);
//    }
  }

  /**
   * @param canvas  
   */
  static void repaint(Object canvas) {
    // N/A -- RepaintManager will never call this in JavaScript 
  }

  /**
   * legacy apps will use this
   * 
   * @param viewer
   * @param g
   * @param size
   */
  static void renderScreenImage(PlatformViewer viewer, Object g, Object size) {
    /**
     * @j2sNative
     * 
     */
    {
      System.out.println("" + viewer + g + size);
    }
  }

  static void setTransparentCursor(Object canvas) {
//    /**
//     * @j2sNative
//     * 
//     */
//    {
//      System.out.println(canvas);
//    }
  }

  static void setCursor(int c, Object canvas) {
//    /**
//     * @j2sNative
//     * 
//     */
//    {
//      System.out.println("" + c + canvas);
//    }
  }

  /**
	 * @param label  
   * @param data 
   * @param list 
   * @param asButtons 
   * @return "null" or response
	 */
  public static String prompt(String label, String data, String[] list,
                              boolean asButtons) {
    /**
     * @j2sNative
     * 
     * var s = prompt(label, data);
     * if (s != null)return s;
     */
    {}
    //TODO -- list and asButtons business
    return "null";
  }

  public static void convertPointFromScreen(Object canvas, P3 ptTemp) {
//    /**
//     * @j2sNative
//     * 
//     */
//    {
//      System.out.println("" + canvas + ptTemp);
//    }
  }



}
