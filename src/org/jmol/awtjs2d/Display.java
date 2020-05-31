package org.jmol.awtjs2d;


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
   * @param label 
   * @param data 
   * @param list 
   * @param asButtons  
   * @return "null" or result of prompt
   */
  public static String prompt(String label, String data, String[] list,
                              boolean asButtons) {
    /**
     * @j2sNative
     * 
     * var s = (data == null ? alert(label) : prompt(label, data));
     * if (s != null)return s;
     */
    {}
    //TODO -- list and asButtons business
    return "null";
  }

  public static void convertPointFromScreen(Object canvas, P3 ptTemp) {    
    /**
     * @j2sNative
     * 
     */
    {
      System.out.println("" + canvas + ptTemp);
    }
  }



}
