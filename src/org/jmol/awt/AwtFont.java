package org.jmol.awt;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.Hashtable;
import java.util.Map;

/**
 * methods required by Jmol that access java.awt.Font
 * 
 * private to org.jmol.awt
 * 
 */

class AwtFont {

  static Object newFont(String fontFace, boolean isBold, boolean isItalic, float fontSize) {
    Map<Attribute, Object> fontMap = new Hashtable<Attribute, Object>();
    fontMap.put(TextAttribute.FAMILY, fontFace);
    if (isBold)
      fontMap.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
    if (isItalic)
      fontMap.put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
    fontMap.put(TextAttribute.SIZE, Float.valueOf(fontSize));
    return new java.awt.Font(fontMap);
  }

  static Object getFontMetrics(javajs.awt.Font font, Object graphics) {
    return ((Graphics) graphics).getFontMetrics((java.awt.Font) font.font);
  }

  static int getAscent(Object fontMetrics) {
    return ((FontMetrics) fontMetrics).getAscent();
  }

  static int getDescent(Object fontMetrics) {
    return ((FontMetrics) fontMetrics).getDescent();
  }

  static int stringWidth(Object fontMetrics, String text) {
    return ((FontMetrics) fontMetrics).stringWidth(text);
  }
}
