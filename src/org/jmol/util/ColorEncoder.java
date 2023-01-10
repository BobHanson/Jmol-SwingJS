/* $RCSfile$
 * $Author: nicove $
 * $Date: 2007-03-25 06:09:49 -0500 (Sun, 25 Mar 2007) $
 * $Revision: 7221 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
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
import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.Rdr;

import java.io.IOException;
import java.util.Hashtable;

import java.util.Map;


import org.jmol.script.T;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;
import org.jmol.c.PAL;

import javajs.util.P3d;

/*
 * 
 * just a simple class using crude color encoding
 * 
 * 
 * NOT THREAD-SAFE! TOO MANY STATIC FIELDS!!
 * 
 * The idea was that isosurface would have access to user-defined applet-wide color schemes.
 * but what we have is a set of globals that any applet could use to mess up any other applet.
 * 
 */


 public class ColorEncoder {

   private Viewer vwr;

  public ColorEncoder(ColorEncoder ce, Viewer vwr) {
    if (ce == null) {
      this.vwr = vwr;
      schemes = new Hashtable<String, int[]>();
      argbsCpk = PAL.argbsCpk;
      argbsRoygb = JC.argbsRoygbScale;
      argbsRwb = JC.argbsRwbScale;
      argbsAmino = argbsNucleic = argbsShapely = null;
      ihalf = JC.argbsRoygbScale.length / 3;
      this.ce = this;
    } else {
      this.ce = ce;
      this.vwr = ce.vwr;
      schemes = ce.schemes;
    }
  }
    
  public void clearCache() {
    schemes.clear();
  }
  
  private final static int GRAY = 0xFF808080;
  

  public final static String BYELEMENT_PREFIX  = "byelement";
  public final static String BYRESIDUE_PREFIX = "byresidue";
  private final static String BYELEMENT_JMOL = BYELEMENT_PREFIX + "_jmol"; 
  private final static String BYELEMENT_RASMOL = BYELEMENT_PREFIX + "_rasmol";
  private final static String BYRESIDUE_SHAPELY = BYRESIDUE_PREFIX + "_shapely"; 
  private final static String BYRESIDUE_AMINO = BYRESIDUE_PREFIX + "_amino"; 
  private final static String BYRESIDUE_NUCLEIC = BYRESIDUE_PREFIX + "_nucleic"; 
  
  public final static int CUSTOM = -1;
  public final static int ROYGB = 0;
  public final static int BGYOR = 1;
  public final static int JMOL = 2;
  public final static int RASMOL = 3;
  public final static int SHAPELY = 4;
  public final static int AMINO = 5;
  public final static int RWB   = 6;
  public final static int BWR   = 7;
  public final static int LOW   = 8;
  public final static int HIGH  = 9;
  public final static int BW  = 10;
  public final static int WB  = 11;
  public final static int FRIENDLY = 12; // color-blind friendly
  public final static int USER = -13;
  public final static int RESU = -14;
  public final static int INHERIT = 15;
  public final static int ALT = 16; // == 0
  public final static int NUCLEIC = 17;

  private final static String[] colorSchemes = {
    "roygb", "bgyor", 
    BYELEMENT_JMOL, BYELEMENT_RASMOL, BYRESIDUE_SHAPELY,
    BYRESIDUE_AMINO, 
    "rwb", "bwr", "low", "high", "bw", "wb", "friendly",
    // custom
    "user", "resu", "inherit",
    // ALT_NAMES:
    "rgb", "bgr", 
    "jmol", "rasmol", BYRESIDUE_PREFIX, BYRESIDUE_NUCLEIC
  };

  private final static int getSchemeIndex(String colorScheme) {
    for (int i = 0; i < colorSchemes.length; i++)
      if (colorSchemes[i].equalsIgnoreCase(colorScheme))
        return (i >= ALT ? i - ALT : i < -USER ? i : -i);
    return CUSTOM;
  }

  private final static String fixName(String name) {
    if (name.equalsIgnoreCase(BYELEMENT_PREFIX)) 
        return BYELEMENT_JMOL;
    int ipt = getSchemeIndex(name);
    return (ipt >= 0 ? colorSchemes[ipt] : name.indexOf("/") >= 0? name : name.toLowerCase());
  }
  
  // these are only implemented in the MASTER colorEncoder
  private int[] paletteBW;
  private int[] paletteWB;
  private int[] paletteFriendly;
  private int[] argbsCpk;
  private int[] argbsRoygb;
  private int[] argbsRwb;
  private int[] argbsShapely;
  private int[] argbsAmino;
  private int[] argbsNucleic;
  private int ihalf;
  private static int[] rasmolScale;
  public Map<String, int[]> schemes;


  public int currentPalette = ROYGB;
  public int currentSegmentCount = 1;
  public boolean isTranslucent = false;
  public double lo;
  public double hi;
  public boolean isReversed;


  //TODO  NONE OF THESE SHOULD BE STATIC:
  
  int[] userScale = new int[] { GRAY };
  int[] thisScale = new int[] { GRAY };
  String thisName = "scheme";
  boolean isColorIndex;
  
  ColorEncoder ce;


  public static int[] argbsChainAtom;
  public static int[] argbsChainHetero;

  /**
   * 
   * @param name
   * @param scale  if null, then this is a reset.
   * @param isOverloaded  if TRUE, 
   * @return  >= 0 for a default color scheme
   */
  private synchronized int makeColorScheme(String name, int[] scale,
                                                  boolean isOverloaded) {
    // from getColorScheme, setUserScale, ColorManager.setDefaultColors
    name = fixName(name);
    if (scale == null) {
      // resetting scale
      schemes.remove(name);
      int iScheme = createColorScheme(name, false, isOverloaded);
      if (isOverloaded)
        switch (iScheme) {
        case Integer.MAX_VALUE:
          return ROYGB;
        case FRIENDLY:
          paletteFriendly = getPaletteAC();
          break;
        case BW:
          paletteBW = getPaletteBW();
          break;
        case WB:
          paletteWB = getPaletteWB();
          break;
        case ROYGB:
        case BGYOR:
          argbsRoygb = JC.argbsRoygbScale;
          break;
        case RWB:
        case BWR:
          argbsRwb = JC.argbsRwbScale;
          break;
        case JMOL:
          argbsCpk = PAL.argbsCpk;
          break;
        case RASMOL:
          getRasmolScale();
          break;
        case NUCLEIC:
          getNucleic();
          break;
        case AMINO:
          getAmino();
          break;
        case SHAPELY:
          getShapely();
          break;
        }
      return iScheme;
    }
    schemes.put(name, scale);
    setThisScheme(name, scale);
    int iScheme = createColorScheme(name, false, isOverloaded);
    if (isOverloaded)
      switch (iScheme) {
      case BW:
        paletteBW = thisScale;
        break;
      case WB:
        paletteWB = thisScale;
        break;
      case ROYGB:
      case BGYOR:
        argbsRoygb = thisScale;
        ihalf = argbsRoygb.length / 3;
        break;
      case RWB:
      case BWR:
        argbsRwb = thisScale;
        break;
      case JMOL:
        argbsCpk = thisScale;
        break;
      case RASMOL:
        break;
      case AMINO:
        argbsAmino = thisScale;
        break;
      case NUCLEIC:
        argbsNucleic = thisScale;
        break;
      case SHAPELY:
        argbsShapely = thisScale;
        break;
      }
    return CUSTOM;
  }

  private int[] getShapely() {
    return (argbsShapely == null ? argbsShapely = vwr.getJBR().getArgbs(T.shapely) : argbsShapely);
  }
  
  private int[] getAmino() {
    return (argbsAmino == null ? argbsAmino = vwr.getJBR().getArgbs(T.amino) : argbsAmino);
  }
   
  private int[] getNucleic() {
    return (argbsNucleic == null ? argbsNucleic = vwr.getJBR().getArgbs(T.nucleic) : argbsNucleic);
  }
   

  /**
   * 
   * @param colorScheme
   *        name or name= or name=[x......] [x......] ....
   * @param defaultToRoygb
   * @param isOverloaded
   * @return paletteID
   */
  public int createColorScheme(String colorScheme, boolean defaultToRoygb,
                               boolean isOverloaded) {
    // main method for creating a new scheme or modifying an old one

    if (colorScheme.equalsIgnoreCase("inherit"))
      return INHERIT;

    // check for "name = [x...] [x...] ..." 
    // or "[x...] [x...] ..."
    int pte = colorScheme.lastIndexOf("=");
    int pts = colorScheme.indexOf("/"); // https...
    int pt = Math.max(pte, colorScheme.indexOf("["));
    String name = fixName(colorScheme);
    int ipt = getSchemeIndex(name);
    if ((pt < 0 || pts > 0) && ipt == CUSTOM ) {
      // no = or [ without / or has /; just a name -- must be loaded 
      int[] scale;
      String s = colorScheme;
      scale = schemes.get(name);
      if (scale == null) {
        try {
          boolean isNot = (s.charAt(1) == '~');
          if (isNot)
            s = s.substring(1);
          byte[] b = null;
          while (true) {
            if (s.indexOf("/") < 0) {
              if (s.indexOf(".") < 0)
                s += ".lut.txt";
              String[] data = new String[1];
              try {
                Logger.info("ColorEncoder opening colorschemes/" + s);
                Rdr.readAllAsString(
                    FileManager.getBufferedReaderForResource(vwr, new C(),
                        "org/jmol/util/", "colorschemes/" + s),
                    -1, false, data, 0);
              } catch (IOException e) {
                Logger.info("ColorEncoder " + e);
                break;
              }
              s = data[0];
            } else {
              s = PT.rep(s, " ", "%20");
              Logger.info("ColorEncoder opening " + s);
              Object o = vwr.fm.getFileAsBytes(s, null);
              if (o instanceof String) {
                Logger.info("ColorEncoder " + o);
                break;
              }
              b = (byte[]) o;
              Logger.info("ColorEncoder read " + b.length + " bytes");
              int i0 = 0;
              int n = b.length;
              if (n == 3 * 256 + 32 || n == 3 * 256 + 32 + 2) // JSmol bug adds CR LF to binary from string
                i0 = 32;
              if (n - i0 == 3 * 256 ||  n - i0 == 3 * 256 + 2) {
                scale = new int[256];
                n = 256 + i0;
                for (int i = 0; i < 256; i++) {
                  scale[i] = CU.rgb(b[i + i0] & 0xFF, b[i + i0 + 256] & 0xFF,
                      b[i + i0 + 512] & 0xFF);
                }
              } else {
                s = new String(b);
              }
            }
            if (scale == null) {
              String[] lines = PT.split(s.trim(), "\n");
              int n = lines.length;
              scale = new int[n];
              boolean isInt = (n > 0 && lines[0].indexOf(".") < 0);
              for (int i = 0; i < n; i++) {
                String[] tokens = PT.getTokens(lines[i]);
                if (tokens.length < 3) {
                  scale = null;
                  break;
                }
                int len = tokens.length;
                if (isInt) {
                  scale[i] = CU.rgb(PT.parseInt(tokens[len - 3]),
                      PT.parseInt(tokens[len - 2]),
                      PT.parseInt(tokens[len - 1]));
                } else {
                  scale[i] = CU.colorTriadToFFRGB(
                      PT.parseDouble(tokens[len - 3]),
                      PT.parseDouble(tokens[len - 2]),
                      PT.parseDouble(tokens[len - 1]));

                }
              }
            }
            if (scale != null && isNot) {
              for (int i = 0, n = scale.length
                  - 1, n2 = (n + 1) >> 1; i < n2; i++) {
                int v = scale[i];
                scale[i] = scale[n - i];
                scale[n - i] = v;
              }
            }
            break;
          }
        } catch (Exception e) {
          Logger.info("ColorEncoder " + e);
          scale = null;
        }
      }
      if (scale == null)
        scale = new int[] { -1 };
      schemes.put(colorScheme, scale);
      setThisScheme(colorScheme, scale);
      return CUSTOM;
    } else if (pt >= 0) {
      colorScheme = colorScheme.toLowerCase();
      name = PT.replaceAllCharacters(colorScheme.substring(0, pt), " =",
          "");
      if (name.length() > 0)
        isOverloaded = true;
      int n = 0;
      if (colorScheme.length() > pt + 1 && !colorScheme.contains("[")) {
        // also allow xxx=red,blue,green

        colorScheme = "[" + colorScheme.substring(pt + 1).trim() + "]";
        colorScheme = PT.rep(colorScheme.replace('\n', ' '), "  ", " ");
        colorScheme = PT.rep(colorScheme, ", ", ",").replace(' ', ',');
        colorScheme = PT.rep(colorScheme, ",", "][");
      }
      pt = -1;
      while ((pt = colorScheme.indexOf("[", pt + 1)) >= 0)
        n++;
      // if just "name=", then we overload it with no scale -- which will clear it
      if (n == 0)
        return makeColorScheme(name, null, isOverloaded);

      // create the scale -- error returns ROYGB

      int[] scale = new int[n];
      n = 0;
      while ((pt = colorScheme.indexOf("[", pt + 1)) >= 0) {
        int pt2 = colorScheme.indexOf("]", pt);
        if (pt2 < 0)
          pt2 = colorScheme.length() - 1;
        int c = CU.getArgbFromString(colorScheme.substring(pt, pt2 + 1));
        if (c == 0) // try without the brackets
          c = CU.getArgbFromString(colorScheme.substring(pt + 1, pt2).trim());
        if (c == 0) {
          Logger.error(
              "error in color value: " + colorScheme.substring(pt, pt2 + 1));
          return ROYGB;
        }
        scale[n++] = c;
      }

      // set the user scale if that is what this is

      if (name.equals("user")) {
        setUserScale(scale);
        return USER;
      }

      // otherwise, make a new scheme for it with the specified scale, which will NOT be null

      return makeColorScheme(name, scale, isOverloaded);
    }

    // wasn't a definition. 

    int[] scale = schemes.get(name);
    if (scale != null) {
      setThisScheme(name, scale);
      return ipt; // -1 means custom -- use "thisScale", otherwise a scheme number
    }

    // return a positive value for a known scheme or ROYGB if a default is ok, or MAX_VALUE
    return (ipt != CUSTOM ? ipt : defaultToRoygb ? ROYGB : Integer.MAX_VALUE);
  }

  public void setUserScale(int[] scale) {
    // getColorScheme
    // ColorManager.setUserScale
    ce.userScale = scale;  
    makeColorScheme("user", scale, false);
  }
  
  public int[] getColorSchemeArray(int palette) {
    // ColorManager.getColorSchemeList
    int[] b;
    switch (palette) {
    /*    case RGB:
     c = quantizeRgb(val, lo, hi, rgbRed, rgbGreen, rgbBlue);
     break;
     */
    case CUSTOM:
      return thisScale;      
    case ROYGB:
      return ce.argbsRoygb;
    case BGYOR:
      return AU.arrayCopyRangeRevI(ce.argbsRoygb, 0, -1);
    case LOW:
      return AU.arrayCopyRangeI(ce.argbsRoygb, 0, ce.ihalf);
    case HIGH:
      int[] a = AU.arrayCopyRangeI(ce.argbsRoygb, ce.argbsRoygb.length - 2 * ce.ihalf, -1);
      b = new int[ce.ihalf];
      for (int i = b.length, j = a.length; --i >= 0 && --j >= 0;)
        b[i] = a[j--];
      return b;
    case FRIENDLY:
      return getPaletteAC();
    case BW:
      return getPaletteBW();
    case WB:
      return getPaletteWB();
    case RWB:
      return ce.argbsRwb;
    case BWR:
      return AU.arrayCopyRangeRevI(ce.argbsRwb, 0, -1);
    case JMOL:
      return ce.argbsCpk;
    case RASMOL:
      return getRasmolScale();
    case SHAPELY:
      return ce.getShapely();
    case NUCLEIC:
      return ce.getNucleic();
    case AMINO:
      return ce.getAmino();
    case USER:
      return ce.userScale;
    case RESU:
      return AU.arrayCopyRangeRevI(ce.userScale, 0, -1);
    default:
      return null;
    }

  }
  
  public short getColorIndexFromPalette(double val, double lo,
                                                     double hi, int palette,
                                                     boolean isTranslucent) {
    short colix = C.getColix(getArgbFromPalette(val, lo, hi, palette));
    if (isTranslucent) {
      double f = (hi - val) / (hi - lo); 
      if (f > 1)
        f = 1; // transparent
      else if (f < 0.125d) // never fully opaque
        f = 0.125d;
      colix = C.getColixTranslucent3(colix, true, f);
    }
    return colix;
  }

  private int getPaletteColorCount(int palette) {
    switch (palette) {
    case CUSTOM:
      return thisScale.length;
    case BW:
    case WB:
      return getPaletteBW().length;
    case ROYGB:
    case BGYOR:
      return ce.argbsRoygb.length;
    case LOW:
    case HIGH:
      return ce.ihalf;
    case RWB:
    case BWR:
      return ce.argbsRwb.length;
    case USER:
    case RESU:
      return ce.userScale.length;
    case JMOL:
      return ce.argbsCpk.length;
    case RASMOL:
      return getRasmolScale().length;
    case SHAPELY:
      return ce.getShapely().length;
    case NUCLEIC:
      return ce.getNucleic().length;
    case AMINO:
      return ce.getAmino().length;
    case FRIENDLY:
      return getPaletteAC().length;
    default:
      return 0;
    }
  }
  
  public int getArgbFromPalette(double val, double lo, double hi, int palette) {
    if (Double.isNaN(val))
      return GRAY;
    int n = getPaletteColorCount(palette);
    switch (palette) {
    case CUSTOM:
      if (isColorIndex) {
        lo = 0;
        hi = thisScale.length;
      }
      return thisScale[quantize4(val, lo, hi, n)];
    case BW:
      return getPaletteBW()[quantize4(val, lo, hi, n)];
    case WB:
      return getPaletteWB()[quantize4(val, lo, hi, n)];
    case ROYGB:
      return ce.argbsRoygb[quantize4(val, lo, hi, n)];
    case BGYOR:
      return ce.argbsRoygb[quantize4(-val, -hi, -lo, n)];
    case LOW:
      return ce.argbsRoygb[quantize4(val, lo, hi, n)];
    case HIGH:
      return ce.argbsRoygb[ce.ihalf + quantize4(val, lo, hi, n) * 2];
    case RWB:
      return ce.argbsRwb[quantize4(val, lo, hi, n)];
    case BWR:
      return ce.argbsRwb[quantize4(-val, -hi, -lo, n)];
    case USER:
      return (ce.userScale.length == 0 ? GRAY : ce.userScale[quantize4(val, lo, hi, n)]);
    case RESU:
      return (ce.userScale.length == 0 ? GRAY : ce.userScale[quantize4(-val, -hi, -lo, n)]);
    case JMOL:
      return ce.argbsCpk[colorIndex(val, n)];
    case RASMOL:
      return getRasmolScale()[colorIndex(val, n)];
    case SHAPELY:
      return ce.getShapely()[colorIndex(val, n)];
    case AMINO:
      return ce.getAmino()[colorIndex(val, n)];
    case NUCLEIC:
      return ce.getNucleic()[colorIndex(val - JC.GROUPID_AMINO_MAX + 2, n)];
    case FRIENDLY:
      return getPaletteAC()[colorIndexRepeat(val, n)];
    default:
      return GRAY;
    }
  }

  private void setThisScheme(String name, int[] scale) {
    thisName = name;
    thisScale = scale;
    if (name.equals("user"))
      userScale = scale;
    isColorIndex = (name.indexOf(BYELEMENT_PREFIX) == 0 
        || name.indexOf(BYRESIDUE_PREFIX) == 0);
  }

  
  // nonstatic methods:
  
  public int getArgb(double val) {
    return (isReversed ? getArgbFromPalette(-val, -hi, -lo, currentPalette)
        : getArgbFromPalette(val, lo, hi, currentPalette));
  }
  
  public int getArgbMinMax(double val, double min, double max) {
    return (isReversed ? getArgbFromPalette(-val, -max, -min, currentPalette)
        : getArgbFromPalette(val, min, max, currentPalette));
  }
  
  public short getColorIndex(double val) {
    return (isReversed ? getColorIndexFromPalette(-val, -hi, -lo, currentPalette, isTranslucent)
        : getColorIndexFromPalette(val, lo, hi, currentPalette, isTranslucent));
  }

  public Map<String, Object> getColorKey() {
    Map<String, Object> info = new Hashtable<String, Object>();
    int segmentCount = getPaletteColorCount(currentPalette);
    Lst<P3d> colors = new  Lst<P3d>();//segmentCount);
    double[] values = new double[segmentCount + 1];
    double quantum = (hi - lo) / segmentCount;
    double f = quantum * (isReversed ? -0.5d : 0.5d);

    for (int i = 0; i < segmentCount; i++) {
      values[i] = (isReversed ? hi - i * quantum : lo + i * quantum);
      colors.addLast(CU.colorPtFromInt(getArgb(values[i] + f), null));
    }
    values[segmentCount] = (isReversed ? lo : hi);
    info.put("values", values);
    info.put("colors", colors);
    info.put("min", Double.valueOf(lo));
    info.put("max", Double.valueOf(hi));
    info.put("reversed", Boolean.valueOf(isReversed));
    info.put("name", getCurrentColorSchemeName());
    return info;
  }

  public String getColorScheme() {
    return (isTranslucent ? "translucent " : "")
        + (currentPalette < 0 ? getColorSchemeList(getColorSchemeArray(currentPalette))
            : getColorSchemeName(currentPalette));
  }

  /**
   * 
   * @param colorScheme
   * @param isTranslucent
   */
  public void setColorScheme(String colorScheme, boolean isTranslucent) {
    this.isTranslucent = isTranslucent;
    if (colorScheme != null)
      currentPalette = createColorScheme(colorScheme, true, false);
  }

  public void setRange(double lo, double hi, boolean isReversed) {
    if (hi == Double.MAX_VALUE) {
      lo = 1; 
      hi = getPaletteColorCount(currentPalette) + 1;
    }
    this.lo = Math.min(lo, hi);
    this.hi = Math.max(lo, hi);
    this.isReversed = isReversed;
  }
  
  public String getCurrentColorSchemeName() {
    return getColorSchemeName(currentPalette);  
  }
  
  public String getColorSchemeName(int i) {
    int absi = Math.abs(i);
    return (i == CUSTOM ? thisName : absi < colorSchemes.length && absi >= 0 ? colorSchemes[absi] : null);  
  }

  // legitimate static methods:
  
  public final static String getColorSchemeList(int[] scheme) {
    if (scheme == null)
      return "";
    String colors = "";
    for (int i = 0; i < scheme.length; i++)
      colors += (i == 0 ? "" : " ") + Escape.escapeColor(scheme[i]);
    return colors;
  }

  public final static synchronized int[] getRasmolScale() {
    if (rasmolScale != null)
      return rasmolScale;
    rasmolScale = new int[PAL.argbsCpk.length];
    int argb = PAL.argbsCpkRasmol[0] | 0xFF000000;
    for (int i = rasmolScale.length; --i >= 0;)
      rasmolScale[i] = argb;
    for (int i = PAL.argbsCpkRasmol.length; --i >= 0;) {
      argb = PAL.argbsCpkRasmol[i];
      rasmolScale[argb >> 24] = argb | 0xFF000000;
    }
    return rasmolScale;
  }

  private int[] getPaletteAC() {
    return (ce.paletteFriendly == null ? ce.paletteFriendly = new int[] { 0x808080, 0x104BA9, 0xAA00A2,
        0xC9F600, 0xFFA200, 0x284A7E, 0x7F207B, 0x9FB82E, 0xBF8B30, 0x052D6E,
        0x6E0069, 0x83A000, 0xA66A00, 0x447BD4, 0xD435CD, 0xD8FA3F, 0xFFBA40,
        0x6A93D4, 0xD460CF, 0xE1FA71, 0xFFCC73 } : ce.paletteFriendly);
  }

  private int[] getPaletteWB() {
    if (ce.paletteWB != null) 
      return ce.paletteWB;
    int[] b = new int[JC.argbsRoygbScale.length];
    for (int i = 0; i < b.length; i++) {
      double xff = (1f / b.length * (b.length - i));        
      b[i] = CU.colorTriadToFFRGB(xff, xff, xff);
    }
    return ce.paletteWB = b;
  }

  public static int[] getPaletteAtoB(int color1, int color2, int n) {
    if (n < 2)
      n = JC.argbsRoygbScale.length;
    int[] b = new int[n];
    double[] rgb1 = new double[3];
    double[] rgb2 = new double[3];
    CU.toRGB3(color1, rgb1);
    CU.toRGB3(color2, rgb2);
    double dr = (rgb2[0] - rgb1[0]) / (n - 1);
    double dg = (rgb2[1] - rgb1[1]) / (n - 1);
    double db = (rgb2[2] - rgb1[2]) / (n - 1);
    for (int i = 0; i < n; i++)
      b[i] = CU.colorTriadToFFRGB(rgb1[0] + dr * i, rgb1[1] + dg * i,
          rgb1[2] + db * i);
    return b;
  }
  private int[] getPaletteBW() {
    if (ce.paletteBW != null) 
      return ce.paletteBW;
    int[] b = new int[JC.argbsRoygbScale.length];
    for (int i = 0; i < b.length; i++) {
      double xff = (1f / b.length * i); 
      b[i] = CU.colorTriadToFFRGB(xff, xff, xff);
    }
    return ce.paletteBW = b;
  }

  /**
   * gets the value at the color boundary for this color range fraction 
   * @param x
   * @param isLowEnd
   * @return quantized value
   */
  public double quantize(double x, boolean isLowEnd) {
    int n = getPaletteColorCount(currentPalette);
    x = (((int) (x * n)) + (isLowEnd ? 0d : 1d)) / n;
    return (x <= 0 ? lo : x >= 1 ? hi : lo + (hi - lo) * x);
  }
  
  public final static int quantize4(double val, double lo, double hi, int segmentCount) {
    /* oy! Say you have an array with 10 values, so segmentCount=10
     * then we expect 0,1,2,...,9  EVENLY
     * If f = fractional distance from lo to hi, say 0.0 to 10.0 again,
     * then one might expect 10 even placements. BUT:
     * (int) (f * segmentCount + 0.5) gives
     * 
     * 0.0 ---> 0
     * 0.5 ---> 1
     * 1.0 ---> 1
     * 1.5 ---> 2
     * 2.0 ---> 2
     * ...
     * 8.5 ---> 9
     * 9.0 ---> 9
     * 9.5 ---> 10 --> 9
     * 
     * so the first bin is underloaded, and the last bin is overloaded.
     * With integer quantities, one would not notice this, because
     * 0, 1, 2, 3, .... --> 0, 1, 2, 3, .....
     * 
     * but with fractional quantities, it will be noticeable.
     * 
     * What we really want is:
     * 
     * 0.0 ---> 0
     * 0.5 ---> 0
     * 1.0 ---> 1
     * 1.5 ---> 1
     * 2.0 ---> 2
     * ...
     * 8.5 ---> 8
     * 9.0 ---> 9
     * 9.5 ---> 9
     * 
     * that is, no addition of 0.5. 
     * Instead, I add 0.0001, just for discreteness sake.
     * 
     * Bob Hanson, 5/2006
     * 
     */
    double range = hi - lo;
    if (range <= 0 || Double.isNaN(val))
      return segmentCount / 2;
    double t = val - lo;
    if (t <= 0)
      return 0;
    double quanta = range / segmentCount;
    int q = (int)(t / quanta + 0.0001f);  //was 0.5d!
    if (q >= segmentCount)
      q = segmentCount - 1;
    return q;
  }

  private final static int colorIndex(double q, int segmentCount) {
    return (int) Math.floor(q <= 0 || q >= segmentCount ? 0 : q);
  }

  private final static int colorIndexRepeat(double q, int segmentCount) {
    int i = (int) Math.floor(q <= 0 ? 0 : q);
    return i % segmentCount;
  }

}
