package javajs.api;

import javajs.awt.Font;

/**
 * A generic interface for font queries.
 * In JSmol it is handled by org.jmol.api.ApiPlatform
 */

public interface FontManager {

  int fontStringWidth(Font font, String text);

  int getFontAscent(Object fontMetrics);

  int getFontDescent(Object fontMetrics);

  Object getFontMetrics(Font font, Object graphics);

  Object newFont(String fontFace, boolean isBold, boolean isItalic, float fontSize);


}
