package jspecview.js2d;

import org.jmol.util.Font;

/**
 * methods required by Jmol that access java.awt.Font
 * 
 * private to org.jmol.awt
 * 
 */

class JsFont {

  static Object newFont(String fontFace, boolean isBold, boolean isItalic, float fontSize, String type) {
    // "px" are different from "pt" here.
    // "pt" is the height of an X, ascent.
    // "px" is the ascent + descent.
    fontFace = (fontFace.equals("Monospaced") ? "Courier" : fontFace.startsWith("Sans") ? "Sans-Serif" : "Serif");    
    return (isBold ? "bold " : "") 
        + (isItalic ? "italic " : "") 
        + fontSize + type + " " + fontFace;
  }

  /**
   * @param font 
   * @param context  
   * @return the context
   */
  static Object getFontMetrics(Font font, Object context) {
    /**
     * 
     * @j2sNative
     * 
     * if (context.font != font.font) {
     *  context.font = font.font;
     *  font.font = context.font;
     *  context._fontAscent = Math.ceil(font.fontSize); //pt, not px
     *  // the descent is actually (px - pt)
     *  // but I know of no way of getting access to the drawn height
     *  context._fontDescent = Math.ceil(font.fontSize * 0.25);//approx
     * }
     */
    {      
    }
    return context;
  }

  /**
   * @param context  
   * @return height of the font 
   */
  static int getAscent(Object context) {
    /**
     * 
     * @j2sNative
     * 
     * return Math.ceil(context._fontAscent);
     */
    {
    return 0;
    }
  }

  /**
   * @param context  
   * @return descent of "g"
   */
  static int getDescent(Object context) {
    /**
     * 
     * @j2sNative
     * 
     * return Math.ceil(context._fontDescent);
     */
    {
    return 0;
    }
  }

  /**
   * 
   * Note the font.fontMetrics is the context("2d")
   * @param font 
   * @param text 
   * @return width
   */
  static int stringWidth(Font font, String text) {
    /**
     * @j2sNative
     * font.fontMetrics.font = font.font;
     * return Math.ceil(font.fontMetrics.measureText(text).width);
     */
    {
     return 0;
    }
  }
}
