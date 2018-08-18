/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-14 23:28:16 -0500 (Sat, 14 Apr 2007) $
 * $Revision: 7408 $
 *
 * Copyright (C) 2005  Miguel, The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net, jmol-developers@lists.sf.net
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

package org.jmol.shapecgo;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.BS;
import org.jmol.script.T;
import org.jmol.shapespecial.DrawMesh;
import org.jmol.util.C;

import javajs.util.CU;
import javajs.util.Lst;
import javajs.util.PT;

import org.jmol.util.Logger;
import org.jmol.util.Normix;
import org.jmol.viewer.Viewer;

import javajs.util.T3;

/*
 * Compiled Graphical Object -- ala PyMOL
 * for reading PyMOL PSE files
 * 
 */

public class CGOMesh extends DrawMesh {

  public Lst<Object> cmds;

  CGOMesh(Viewer vwr, String thisID, short colix, int index) {
    super(vwr, thisID, colix, index);
  }

  public final static int GL_POINTS = 0;
  public final static int GL_LINES = 1;
  public final static int GL_LINE_LOOP = 2;
  public final static int GL_LINE_STRIP = 3;
  public final static int GL_TRIANGLES = 4;
  public final static int GL_TRIANGLE_STRIP = 5;
  public final static int GL_TRIANGLE_FAN = 6;

  // only a few of these have been implemented

  public final static int STOP = 0;
  public final static int SIMPLE_LINE = 1;
  public final static int BEGIN = 2;
  public final static int END = 3;
  public final static int VERTEX = 4;

  public final static int NORMAL = 5;
  public final static int COLOR = 6;
  public final static int SPHERE = 7;
  public final static int TRICOLOR_TRIANGLE = 8;
  public final static int CYLINDER = 9;

  public final static int LINEWIDTH = 10;
  public final static int WIDTHSCALE = 11;
  public final static int ENABLE = 12;
  public final static int DISABLE = 13;
  public final static int SAUSAGE = 14;

  public final static int CUSTOM_CYLINDER = 15;
  public final static int DOTWIDTH = 16;
  public final static int ALPHA_TRIANGLE = 17;
  public final static int ELLIPSOID = 18;
  public final static int FONT = 19;

  public final static int FONT_SCALE = 20;
  public final static int FONT_VERTEX = 21;
  public final static int FONT_AXES = 22;
  public final static int CHAR = 23;
  public final static int INDENT = 24;

  public final static int ALPHA = 25;
  public final static int QUADRIC = 26;
  public final static int CONE = 27;
  public final static int RESET_NORMAL = 28;
  public final static int PICK_COLOR = 29;

  private final static int[] sizes = new int[] { 0, 8, 1, 0, 3, 3, 3, 4, 27,
      13, 1, 1, 1, 1, 13, 15, 1, 35, 13, 3, 2, 3, 9, 1, 2, 1, 14, 16, 1, 2 };

  private final static int[] sizes2D = new int[] { 0, 6, 1, 0, 2, 3, 3, 4, 24,
      13, 1, 1, 1, 1, 11, 15, 1, 35, 13, 3, 2, 3, 9, 1, 2, 1, 14, 16, 1, 2 };

  public static int getSize(int i, boolean is2D) {
    switch (i) {
    case JMOL_PS:
      return 13;
    case JMOL_UVMAP:
      return 15;
    case PS_SCALE:
    case PS_MOVETO:
    case PS_LINETO:
      return 2;
    case JMOL_SCREEN:
    case JMOL_DIAMETER:
    case PS_SETLINEWIDTH:
      return 1;
    case PS_CLOSEPATH:
    case PS_STROKE:
    case PS_SHOWPAGE:  
    case PS_NEWPATH:
      return 0;
    default:
      return (i >= 0 && i < sizes.length ? (is2D ? sizes2D : sizes)[i] : -1);
    }
  }

  public final static int JMOL_DIAMETER = -100;
  public final static int JMOL_SCREEN = -101; // SCREEN 50 50%; -3000 absolute z=3000
  public final static int JMOL_UVMAP = -102; // MAP {pt0} {ptx} {pty} x0 y0 x1 y1 scaleX scaleY as in PostScript
  public final static int JMOL_PS = -103;
  public final static int PS_NEWPATH = -104;
  public final static int PS_CLOSEPATH = -105;
  public final static int PS_STROKE = -106;
  public final static int PS_SETLINEWIDTH = -107;
  public final static int PS_SCALE = -108;
  public final static int PS_MOVETO = -109;
  public final static int PS_LINETO = -110;
  public final static int PS_SHOWPAGE = -111;

  private final static String KEY_LIST = "BEGIN:2 END:3 STOP:0 "
      + "POINT:0 POINTS:0 LINES:1 LINE_LOOP:2 LINE_STRIP:3 TRIANGLES:4 TRIANGLE_STRIP:5 TRIANGLE_FAN:6 "
      + "LINE:1 VERTEX:4 NORMAL:5 COLOR:6 LINEWIDTH:10 SAUSAGE:14 "
      + "DIAMETER:-100 SCREEN:-101 UVMAP:-102 "
      + "PS:-103 NEWPATH:-104 CLOSEPATH:-105 STROKE:-106 SETLINEWIDTH:-107 "
      + "SCALE:-108 MOVETO:-109 LINETO:-110 SHOWPAGE:-111";

  public static Map<String, Integer> getKeyMap() {
    Map<String, Integer> keyMap = new Hashtable<String, Integer>();
    String[] tokens = PT.getTokens(KEY_LIST);
    for (int i = tokens.length; --i >= 0;) {
      int pt = tokens[i].indexOf(":");
      keyMap.put(tokens[i].substring(0, pt),
          Integer.valueOf(Integer.parseInt(tokens[i].substring(pt + 1))));
    }
    return keyMap;
  }

  private static Map<String, Integer> keyMap;
  
  static boolean getData(Object[] d) {
    if (keyMap == null)
      keyMap = getKeyMap();
    T[] st = (T[]) d[0];
    int[] ai = (int[]) d[1];
    @SuppressWarnings("unchecked")
    Lst<Object> data = (Lst<Object>) d[2];
    Viewer vwr = (Viewer) d[3];
    int i = ai[0];
    int slen = ai[1];
    int tok = st[i].tok;
    i = (tok == T.leftsquare ? i + 1 : i + 2);
    if (i >= slen)
      return false;
    String s = st[i].value.toString().toUpperCase();
    int type = ";PS;BEGIN;SCREEN;UVMAP;".indexOf(";" + s + ";");
    i = addItems(i, st, slen, data, vwr);
    if (type == 0) {
      if (i + 5 >= slen || st[i + 1].tok != T.data)
        return false;
      if (!parseEPSData(st[i + 3].value.toString(), data))
        return false;
      i += 5;
    }
    ai[0] = i;
    return true;
  }
  
  private static boolean parseEPSData(String eps, Lst<Object> data) {
    int pt = eps.indexOf("%%BoundingBox:");
    if (pt < 0)
      return false;
    Lst<Object> stack = new Lst<Object>();
    int[] next = new int[] {pt + 14};
    for (int i = 0; i < 4; i++)
      data.addLast(Float.valueOf(PT.parseFloatNext(eps, next)));
    pt = eps.indexOf("%%EndProlog");
    if (pt < 0)
      return false;
    next[0] = pt + 11;
    int len = eps.length();
    while (true) {
      float f = PT.parseFloatChecked(eps, len, next, false);
      if (next[0] >= len)
        break;
      if (Float.isNaN(f)) {
        String s = PT.parseTokenChecked(eps, len, next);
        if (s.startsWith("%%")) // no spaces here
          continue;
        if (!addKey(data, s))
          return false;
        if (stack.size() > 0) {
          for (int k = 0, n = stack.size(); k < n; k++)
            data.addLast(stack.get(k));
          stack.clear();
        }
      } else {
         stack.addLast(Float.valueOf(f));
      }
    }
    return true;
  }



  private static int addItems(int i, T[] st, int slen, Lst<Object> data, Viewer vwr) {
    int tok;
    T t;
    for (int j = i; j < slen; j++) {
      switch (tok = (t = st[j]).tok) {
      case T.rightsquare:
        i = j;
        j = slen;
        continue;
        //$FALL-THROUGH$
      case T.integer:
        data.addLast(Float.valueOf(t.intValue));
        break;
      case T.decimal:
        data.addLast(t.value);
        break;
      case T.point3f:
      case T.bitset:
        T3 pt = (tok == T.point3f ? (T3) t.value : vwr.ms.getAtomSetCenter((BS) t.value));
        data.addLast(Float.valueOf(pt.x));
        data.addLast(Float.valueOf(pt.y));
        data.addLast(Float.valueOf(pt.z));
        break;
      default:
        if (!addKey(data, st[j].value.toString())) {
          Logger.error("CGO unknown: " + st[j].value);
          i = j = slen;
          break;
        }
        break;
      }
    }
    return i;
  }

  private static boolean addKey(Lst<Object> data, String key) {
    key = key.toUpperCase();
    Object ii = keyMap.get(key.toUpperCase());
    if (ii == null)
      return false;
    data.addLast(ii);
    return true;
  }

  @Override
  public void clear(String meshType) {
    super.clear(meshType);
    useColix = false;
  }

  @SuppressWarnings("unchecked")
  boolean set(Lst<Object> list) {
    // vertices will be in list.get(0). normals?
    width = 200;
    diameter = 0;//200;
    useColix = true;
    bsTemp = new BS();
    try {
      if (list.get(0) instanceof Number) {
        cmds = list;
      } else {
        cmds = (Lst<Object>) list.get(1);
        if (cmds == null)
          cmds = (Lst<Object>) list.get(0);
        cmds = (Lst<Object>) cmds.get(1);
      }

      int n = cmds.size();
      boolean is2D = false;
      for (int i = 0; i < n; i++) {
        int type = ((Number) cmds.get(i)).intValue();
        int len = getSize(type, is2D);
        if (len < 0) {
          Logger.error("CGO unknown type: " + type);
          return false;
        }
        switch (type) {
        case JMOL_SCREEN:
        case JMOL_UVMAP:
          is2D = true;
          break;
        case SIMPLE_LINE:
          // para_closed_wt-MD-27.9.12.pse
          // total hack.... could be a strip of lines?
          break;
        case STOP:
          return true;
        case NORMAL:
          addNormix(i + 1);
          break;
        case COLOR:
          addColix(i + 1);
          useColix = false;
          break;
        case CGOMesh.SAUSAGE:
          addColix(i + 8);
          addColix(i + 11);
          break;
        case CGOMesh.TRICOLOR_TRIANGLE:
          addNormix(i + 10);
          addNormix(i + 13);
          addNormix(i + 16);
          addColix(i + 19);
          addColix(i + 22);
          addColix(i + 25);
          break;
        }
        //Logger.info("CGO " + thisID + " type " + type + " len " + len);
        i += len;
      }
      return true;
    } catch (Exception e) {
      Logger.error("CGOMesh error: " + e);
      cmds = null;
      return false;
    }
  }

  private void addColix(int i) {
    getPoint(i, vTemp);
    cList.addLast(Short.valueOf(C.getColix(CU.colorPtToFFRGB(vTemp))));
  }

  private void addNormix(int i) {
    getPoint(i, vTemp);
    nList.addLast(Short.valueOf(Normix.get2SidedNormix(vTemp, bsTemp)));
  }

  public Lst<Short> nList = new Lst<Short>();
  public Lst<Short> cList = new Lst<Short>();

  /**
   * 
   * @param i
   *        pointer to PRECEDING item
   * @param pt
   */
  public void getPoint(int i, T3 pt) {
    pt.set(getFloat(i++), getFloat(i++), getFloat(i));
  }

  /**
   * 
   * @param i
   *        pointer to THIS value
   * @return int
   */
  public int getInt(int i) {
    return ((Number) cmds.get(i)).intValue();
  }

  /**
   * 
   * @param i
   *        pointer to THIS value
   * @return float
   */
  public float getFloat(int i) {
    return ((Number) cmds.get(i)).floatValue();
  }

}
