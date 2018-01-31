/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2015-01-22 11:34:27 -0600 (Thu, 22 Jan 2015) $
 * $Revision: 20231 $
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

package org.jmol.util;


import javajs.util.AU;
import javajs.util.CU;
import javajs.util.PT;
import javajs.util.SB;

import org.jmol.c.PAL;

/**
 * 
 * Note: Color table is now in javajs/util/CU.java
 * 
 *<p>
 * Implements a color index model using a colix as a
 * <strong>COLor IndeX</strong>.
 *</p>
 *<p>
 * A colix is a color index represented as a short int.
 *</p>
 *<p>
 * The value 0 is considered a null value ... for no color. In Jmol this
 * generally means that the value is inherited from some other object.
 *</p>
 *<p>
 * The value 1 is used to indicate that color only is to be inherited. 
 * 
 * 0x0001 INHERIT_OPAQUE -- opaque, but with the color coming from the parent.
 * 0x4001 INHERIT_TRANSLUCENT -- translucent but with the color coming from the parent.
 * 
 * The value 2 is used to indicate that one of the palettes is to be used. 
 * 
 * 0x0002 PALETTE, opaque
 * 0x4002 PALETTE, translucent
 * 
 * Palettes themselves are coded separately in a Palette ID that is tracked with
 *</p>
 *
 * @author Miguel, miguel@jmol.org 
 */

public final class C {

  // final here because we are initializing public static fields using static{}
  
  /* ***************************************************************
   * color indexes -- colix
   * ***************************************************************/

  /* entries 0 and 1 are reserved and are special inheritance
     0 INHERIT_ALL inherits both color and translucency
     1 INHERIT_COLOR is used to inherit just the color
     
     
     0x8000 changeable flag (elements and isotopes, about 200; negative)
     0x7800 translucent flag set

     NEW:
     0x0000 translucent level 0  (opaque)
     0x0800 translucent level 1
     0x1000 translucent level 2
     0x1800 translucent level 3
     0x2000 translucent level 4
     0x2800 translucent level 5
     0x3000 translucent level 6
     0x3800 translucent level 7
     0x4000 translucent level 8 (invisible)

     0x0000 inherit color and translucency
     0x0001 inherit color; translucency determined by mask     
     0x0002 special palette ("group", "structure", etc.); translucency by mask

     Note that inherited colors and special palettes are not handled here. 
     They could be anything, including totally variable quantities such as 
     distance to an object. So there are two stages of argb color determination
     from a colix. The special palette flag is only used transiently - just to
     indicate that the color selected isn't a known color. The actual palette-based
     colix is saved here, and the atom or shape's byte paletteID is set as well.
     
     Shapes/ColorManager: responsible for assigning argb colors based on 
     color palettes. These argb colors are then used directly.
     
     Graphics3D: responsible for "system" colors and caching of user-defined rgbs.
     
     
     
     0x0004 black...
       ....
     0x0017  ...gold
     0x00?? additional colors used from JavaScript list or specified by user
     
     0x0177 last available colix

     Bob Hanson 3/2007
     
  */

  public final static short INHERIT_ALL = 0; // do not change this from 0; new colix[n] must be this
  public final static short INHERIT_COLOR = 1;
  public final static short USE_PALETTE = 2;
  public final static short RAW_RGB = 3;
  public final static short SPECIAL_COLIX_MAX = 4;

  static int colixMax = SPECIAL_COLIX_MAX;
  
  static int[] argbs = new int[128];
  
  private static int[] argbsGreyscale;

  private static final Int2IntHash colixHash = new Int2IntHash(256);
  private static final int RAW_RGB_INT = RAW_RGB;
  public final static short UNMASK_CHANGEABLE_TRANSLUCENT = 0x07FF;
  public final static short CHANGEABLE_MASK = (short) 0x8000; // negative
  public final static int LAST_AVAILABLE_COLIX = UNMASK_CHANGEABLE_TRANSLUCENT;
  public final static int TRANSLUCENT_SHIFT = 11;
  public final static int ALPHA_SHIFT = 24 - TRANSLUCENT_SHIFT;
  public final static int TRANSLUCENT_MASK = 0xF << TRANSLUCENT_SHIFT; //0x7800
  public final static int TRANSLUCENT_SCREENED = TRANSLUCENT_MASK;
  public final static int TRANSPARENT = 8 << TRANSLUCENT_SHIFT; //0x4000
  public final static short OPAQUE_MASK = ~TRANSLUCENT_MASK;

  public final static short BLACK = 4;
  public final static short ORANGE = 5;
  public final static short PINK = 6;
  public final static short BLUE = 7;
  public final static short WHITE = 8;
  public final static short CYAN = 9;
  public final static short RED = 10;
  public final static short GREEN = 11;
  public final static short GRAY = 12;
  public final static short SILVER = 13;
  public final static short LIME = 14;
  public final static short MAROON = 15;
  public final static short NAVY = 16;
  public final static short OLIVE = 17;
  public final static short PURPLE = 18;
  public final static short TEAL = 19;
  public final static short MAGENTA = 20;
  public final static short YELLOW = 21;
  public final static short HOTPINK = 22;
  public final static short GOLD = 23;
  
  public C() {
  }
  
  public static short getColix(int argb) {
    if (argb == 0)
      return 0;
    int translucentFlag = 0;
    // in JavaScript argb & 0xFF000000 will be a negative long value
    if ((argb & 0xFF000000) != (0xFF000000 & 0xFF000000)) {
      translucentFlag = getTranslucentFlag((argb >> 24) & 0xFF);
      argb |= 0xFF000000; 
    }
    int c = colixHash.get(argb);
    if ((c & RAW_RGB_INT) == RAW_RGB_INT)
      translucentFlag = 0;
    return (short) ((c > 0 ? c : allocateColix(argb, false)) | translucentFlag);
  }

  public synchronized static int allocateColix(int argb, boolean forceLast) {
    // in JavaScript argb & 0xFF000000 will be a long
    //if ((argb & 0xFF000000) != (0xFF000000 & 0xFF000000))
    //  throw new IndexOutOfBoundsException();
    // double-check to make sure that someone else did not allocate
    // something of the same color while we were waiting for the lock
    int n;
    if (forceLast) {
      n = LAST_AVAILABLE_COLIX;
    } else {
      for (int i = colixMax; --i >= SPECIAL_COLIX_MAX;)
        if ((argb & 0xFFFFFF) == (argbs[i] & 0xFFFFFF))
          return i;
      n = colixMax;
    }
    if (n >= argbs.length) {
      int newSize = (forceLast ? n + 1 : colixMax * 2);
      if (newSize > LAST_AVAILABLE_COLIX + 1)
        newSize = LAST_AVAILABLE_COLIX + 1;
      argbs = AU.arrayCopyI(argbs, newSize);
      if (argbsGreyscale != null)
        argbsGreyscale = AU.arrayCopyI(argbsGreyscale, newSize);
    }
    argbs[n] = argb;
    if (argbsGreyscale != null)
      argbsGreyscale[n] = CU.toFFGGGfromRGB(argb);
    colixHash.put(argb, n);
    return (n < LAST_AVAILABLE_COLIX ? colixMax++ : colixMax);
  }

  static void setLastGrey(int argb) {
    calcArgbsGreyscale();
    argbsGreyscale[LAST_AVAILABLE_COLIX] = CU.toFFGGGfromRGB(argb);
  }

  synchronized static void calcArgbsGreyscale() {
    if (argbsGreyscale != null)
      return;
    int[] a = new int[argbs.length];
    for (int i = argbs.length; --i >= SPECIAL_COLIX_MAX;)
      a[i] = CU.toFFGGGfromRGB(argbs[i]);
    argbsGreyscale = a;
  }

  public final static int getArgbGreyscale(short colix) {
    if (argbsGreyscale == null)
      calcArgbsGreyscale();
    return argbsGreyscale[colix & OPAQUE_MASK];
  }

  /*
  Int2IntHash hashMix2 = new Int2IntHash(32);

  short getColixMix(short colixA, short colixB) {
    if (colixA == colixB)
      return colixA;
    if (colixA <= 0)
      return colixB;
    if (colixB <= 0)
      return colixA;
    int translucentMask = colixA & colixB & TRANSLUCENT_MASK;
    colixA &= ~TRANSLUCENT_MASK;
    colixB &= ~TRANSLUCENT_MASK;
    int mixId = ((colixA < colixB)
                 ? ((colixA << 16) | colixB)
                 : ((colixB << 16) | colixA));
    int mixed = hashMix2.get(mixId);
    if (mixed == Integer.MIN_VALUE) {
      int argbA = argbs[colixA];
      int argbB = argbs[colixB];
      int r = (((argbA & 0x00FF0000)+(argbB & 0x00FF0000)) >> 1) & 0x00FF0000;
      int g = (((argbA & 0x0000FF00)+(argbB & 0x0000FF00)) >> 1) & 0x0000FF00;
      int b = (((argbA & 0x000000FF)+(argbB & 0x000000FF)) >> 1);
      int argbMixed = 0xFF000000 | r | g | b;
      mixed = getColix(argbMixed);
      hashMix2.put(mixId, mixed);
    }
    return (short)(mixed | translucentMask);
  }
  */
  
  static {
    int[] predefinedArgbs =  { // For Google Closure Compiler
      0xFF000000, // black
      0xFFFFA500, // orange
      0xFFFFC0CB, // pink
      0xFF0000FF, // blue
      0xFFFFFFFF, // white
      0xFF00FFFF, // cyan
      0xFFFF0000, // red
      0xFF008000, // green -- really!
      0xFF808080, // gray
      0xFFC0C0C0, // silver
      0xFF00FF00, // lime  -- no kidding!
      0xFF800000, // maroon
      0xFF000080, // navy
      0xFF808000, // olive
      0xFF800080, // purple
      0xFF008080, // teal
      0xFFFF00FF, // magenta
      0xFFFFFF00, // yellow
      0xFFFF69B4, // hotpink
      0xFFFFD700, // gold
    };
    // OK for J2S compiler because this is a final class
    for (int i = 0; i < predefinedArgbs.length; ++i)
      getColix(predefinedArgbs[i]);
  }

  public static short getColixO(Object obj) {
    if (obj == null)
      return INHERIT_ALL;
    if (obj instanceof PAL)
      return (((PAL) obj) == PAL.NONE ? INHERIT_ALL
          : USE_PALETTE);
    if (obj instanceof Integer)
      return getColix(((Integer) obj).intValue());
    if (obj instanceof String)
      return getColixS((String) obj);
    if (obj instanceof Byte)
      return (((Byte) obj).byteValue() == 0 ? INHERIT_ALL : USE_PALETTE);
    if (Logger.debugging) {
      Logger.debug("?? getColix(" + obj + ")");
    }
    return HOTPINK;
  }

  private static int getTranslucentFlag(float translucentLevel) {
    // 0.0 to 1.0 ==> MORE translucent   
    //                 1/8  1/4 3/8 1/2 5/8 3/4 7/8 8/8
    //     t            32  64  96  128 160 192 224 255 or 256
    //     t >> 5        1   2   3   4   5   6   7   8
    //     (t >> 5) + 1  2   3   4   5   6   7   8   9 
    // 15 is reserved for screened, so 9-14 just map to 9, "invisible"
  
    if (translucentLevel == 0) //opaque
      return 0;
    if (translucentLevel < 0) //screened
      return TRANSLUCENT_SCREENED;
//    if (translucentLevel < 0) //screened
  //    translucentLevel = 128;//return TRANSLUCENT_SCREENED;
    if (Float.isNaN(translucentLevel) || translucentLevel >= 255
        || translucentLevel == 1.0)
      return TRANSPARENT;
    int iLevel = (int) Math.floor(translucentLevel < 1 ? translucentLevel * 256
        : translucentLevel >= 15 ? translucentLevel
        : translucentLevel <= 9 ? ((int) Math.floor(translucentLevel - 1)) << 5
        : 8 << 5);
    return (((iLevel >> 5) & 0xF) << TRANSLUCENT_SHIFT);
  }

  public static boolean isColixLastAvailable(short colix) {
    return (colix > 0 && (colix & LAST_AVAILABLE_COLIX) == LAST_AVAILABLE_COLIX);
  }

  public static int getArgb(short colix) {
    return argbs[colix & OPAQUE_MASK];
  }

  public final static boolean isColixColorInherited(short colix) {
    switch (colix) {
    case INHERIT_ALL:
    case INHERIT_COLOR:
      return true;
    default: //could be translucent of some sort
      return (colix & OPAQUE_MASK) == INHERIT_COLOR;
    }
  }

  public final static short getColixInherited(short myColix, short parentColix) {
    switch (myColix) {
    case INHERIT_ALL:
      return parentColix;
    case INHERIT_COLOR:
      return (short) (parentColix & OPAQUE_MASK);
    default:
      //check this colix irrespective of translucency, and if inherit, then
      //it must be inherit color but not translucent level; 
      return ((myColix & OPAQUE_MASK) == INHERIT_COLOR ? (short) (parentColix
          & OPAQUE_MASK | myColix & TRANSLUCENT_MASK) : myColix);
    }
  }

  public final static boolean renderPass2(short colix) {
    int c = colix & TRANSLUCENT_MASK;
    return (c != 0 && c != TRANSLUCENT_SCREENED);
  }

  public final static boolean isColixTranslucent(short colix) {
    return ((colix & TRANSLUCENT_MASK) != 0);
  }

  public final static short getChangeableColixIndex(short colix) {
    return (colix >= 0 ? -1 : (short) (colix & UNMASK_CHANGEABLE_TRANSLUCENT));
  }

  public final static short getColixTranslucent3(short colix,
                                                 boolean isTranslucent,
                                                 float translucentLevel) {
    colix &= ~TRANSLUCENT_MASK;
    if (colix == INHERIT_ALL)
      colix = INHERIT_COLOR;
    return (isTranslucent ? (short) (colix | getTranslucentFlag(translucentLevel))
        : colix);
  }

  public final static short copyColixTranslucency(short colixFrom, short colixTo) {
    return getColixTranslucent3(colixTo, isColixTranslucent(colixFrom),
        getColixTranslucencyLevel(colixFrom));
  }

  public static float getColixTranslucencyFractional(short colix) {
    int translevel = getColixTranslucencyLevel(colix);
    return (translevel == -1 ? 0.5f : translevel == 0 ? 0
        : translevel == 255 ? 1 : translevel / 256f);
  }

  public static String getColixTranslucencyLabel(short colix) {
    return  "translucent " + ((colix & TRANSLUCENT_SCREENED) == TRANSLUCENT_SCREENED ? -1 : getColixTranslucencyFractional(colix));
  }

  public final static int getColixTranslucencyLevel(short colix) {
    int logAlpha = (colix >> TRANSLUCENT_SHIFT) & 0xF;
    switch (logAlpha) {
    case 0:
      return 0;
    case 1: //  32
    case 2: //  64
    case 3: //  96
    case 4: // 128
    case 5: // 160
    case 6: // 192
    case 7: // 224
      return logAlpha << 5;
    case 15:
      return -1;
    default:
      return 255;
    }
  }

  public static short getColixS(String colorName) {
    int argb = CU.getArgbFromString(colorName);
    if (argb != 0)
      return getColix(argb);
    if ("none".equalsIgnoreCase(colorName))
      return INHERIT_ALL;
    if ("opaque".equalsIgnoreCase(colorName))
      return INHERIT_COLOR;
    return USE_PALETTE;
  }

  public static short[] getColixArray(String colorNames) {
    if (colorNames == null || colorNames.length() == 0)
      return null;
    String[] colors = PT.getTokens(colorNames);
    short[] colixes = new short[colors.length];
    for (int j = 0; j < colors.length; j++) {
      colixes[j] = getColix(CU.getArgbFromString(colors[j]));
      if (colixes[j] == 0)
        return null;
    }
    return colixes;
  }

  public static String getHexCode(short colix) {
    return Escape.escapeColor(getArgb(colix));
  }

  public static String getHexCodes(short[] colixes) {
    if (colixes == null)
      return null;
    SB s = new SB();
    for (int i = 0; i < colixes.length; i++)
      s.append(i == 0 ? "" : " ").append(getHexCode(colixes[i]));
    return s.toString();
  }

  public static short getColixTranslucent(int argb) {
    int a = (argb >> 24) & 0xFF;
    return (a == 0xFF ? getColix(argb) : getColixTranslucent3(getColix(argb), true, a / 255f));
  }  

  public static short getBgContrast(int argb) {
    return ((CU.toFFGGGfromRGB(argb) & 0xFF) < 128 ? WHITE
        : BLACK);
  }

}
