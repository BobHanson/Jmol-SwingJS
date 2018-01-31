/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2013-10-30 13:47:37 -0500 (Wed, 30 Oct 2013) $
 * $Revision: 18874 $
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
package javajs.awt;


import javajs.api.FontManager;
import javajs.util.AU;


/**
 *<p>
 * Provides font support using a byte fid
 * (<strong>F</strong>ont <strong>ID</strong>) as an index into font table.
 *</p>
 *<p>
 * Supports standard font faces, font styles, and font sizes.
 *</p>
 *
 * @author Miguel, miguel@jmol.org
 */
final public class Font {

  public final byte fid;
  public final String fontFace;
  public final String fontStyle;
  public final float fontSizeNominal;
  public final int idFontFace;
  public final int idFontStyle;
  public final float fontSize;
  public final Object font;
  private final Object fontMetrics;
  private FontManager manager;
  private int ascent;
  private int descent;
  private boolean isBold;
  private boolean isItalic;
  
  private Font(FontManager manager, byte fid, int idFontFace,
      int idFontStyle, float fontSize, float fontSizeNominal, Object graphics) {
    this.manager = manager;
    this.fid = fid;
    this.fontFace = fontFaces[idFontFace];
    this.fontStyle = fontStyles[idFontStyle];
    this.idFontFace = idFontFace;
    this.idFontStyle = idFontStyle;
    this.fontSize = fontSize;
    this.isBold = (idFontStyle & FONT_STYLE_BOLD) == FONT_STYLE_BOLD;
    this.isItalic = (idFontStyle & FONT_STYLE_ITALIC) == FONT_STYLE_ITALIC;
    this.fontSizeNominal = fontSizeNominal;
    font = manager.newFont(fontFaces[idFontFace], isBold, isItalic,
        fontSize);
    fontMetrics = manager.getFontMetrics(this, graphics);
    descent = manager.getFontDescent(fontMetrics);
    ascent = manager.getFontAscent(fontMetrics);

    //System.out.println("font3d constructed for fontsizeNominal=" + fontSizeNominal + "  and fontSize=" + fontSize);
  }

  ////////////////////////////////////////////////////////////////
  
  private final static int FONT_ALLOCATION_UNIT = 8;
  private static int fontkeyCount = 1;
  private static int[] fontkeys = new int[FONT_ALLOCATION_UNIT];
  private static Font[] font3ds = new Font[FONT_ALLOCATION_UNIT];

  public static Font getFont3D(byte fontID) {
    return font3ds[fontID & 0xFF];
  }
  
  public static synchronized Font createFont3D(int fontface, int fontstyle,
                                       float fontsize, float fontsizeNominal,
                                       FontManager manager, Object graphicsForMetrics) {
    //if (graphicsForMetrics == null)
     // return null;
    if (fontsize > 0xFF)
      fontsize = 0xFF;
    int fontsizeX16 = ((int) fontsize) << 4;
    int fontkey = ((fontface & 3) | ((fontstyle & 3) << 2) | (fontsizeX16 << 4));
    // watch out for race condition here!
    for (int i = fontkeyCount; --i > 0;)
      if (fontkey == fontkeys[i]
          && font3ds[i].fontSizeNominal == fontsizeNominal)
        return font3ds[i];
    int fontIndexNext = fontkeyCount++;
    if (fontIndexNext == fontkeys.length)
      fontkeys = AU.arrayCopyI(fontkeys, fontIndexNext + FONT_ALLOCATION_UNIT);
      font3ds = (Font[]) AU.arrayCopyObject(font3ds, fontIndexNext + FONT_ALLOCATION_UNIT);
    Font font3d = new Font(manager, (byte) fontIndexNext, fontface, fontstyle,
        fontsize, fontsizeNominal, graphicsForMetrics);
    // you must set the font3d before setting the fontkey in order
    // to prevent a race condition with getFont3D
    font3ds[fontIndexNext] = font3d;
    fontkeys[fontIndexNext] = fontkey;
    return font3d;
  }

  public final static int FONT_FACE_SANS  = 0;
  public final static int FONT_FACE_SERIF = 1;
  public final static int FONT_FACE_MONO  = 2;
  
  private final static String[] fontFaces =
  {"SansSerif", "Serif", "Monospaced", ""};

  public final static int FONT_STYLE_PLAIN      = 0;
  public final static int FONT_STYLE_BOLD       = 1;
  public final static int FONT_STYLE_ITALIC     = 2;
  public final static int FONT_STYLE_BOLDITALIC = 3;
  
  private final static String[] fontStyles =
  {"Plain", "Bold", "Italic", "BoldItalic"};
  
  public static int getFontFaceID(String fontface) {
    return ("Monospaced".equalsIgnoreCase(fontface) ? FONT_FACE_MONO 
        : "Serif".equalsIgnoreCase(fontface) ? FONT_FACE_SERIF 
        : FONT_FACE_SANS);
  }

  public static int getFontStyleID(String fontstyle) {
    for (int i = 4; --i >= 0; )
      if (fontStyles[i].equalsIgnoreCase(fontstyle))
       return i;
    return -1;
  }

  public int getAscent() {
    return ascent;
  }
  
  public int getDescent() {
    return descent;
  }
  
  public int getHeight() {
    return getAscent() + getDescent();
  }

  public Object getFontMetrics() {
    return fontMetrics;
  }
  
  public int stringWidth(String text) {
    return manager.fontStringWidth(this, text);
  }

  public String getInfo() {
    return  fontSizeNominal + " " + fontFace + " " + fontStyle;
  }
}

