/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-05 07:42:12 -0500 (Fri, 05 Jun 2009) $
 * $Revision: 10958 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.script;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;


import javajs.util.BS;
import org.jmol.modelset.BondSet;
import org.jmol.util.BSUtil;
import org.jmol.util.Escape;

import javajs.api.JSONEncodable;
import javajs.util.Lst;
import javajs.util.SB;


import javajs.util.AU;
import javajs.util.BArray;
import javajs.util.Base64;
import javajs.util.M3;
import javajs.util.M34;
import javajs.util.M4;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.Quat;
import javajs.util.T3;

import javajs.util.V3;


/**
 * ScriptVariable class
 * 
 */
public class SV extends T implements JSONEncodable {

  public final static SV vT = newSV(on, 1, "true");
  public final static SV vF = newSV(off, 0, "false");

  public int index = Integer.MAX_VALUE;    

  public String myName;

  public static SV newV(int tok, Object value) {
    SV sv = new SV();
    sv.tok = tok;
    sv.value = value;
    return sv;
  }

  public static SV newI(int i) {
    SV sv = new SV();
    sv.tok = integer;
    sv.intValue = i;
    return sv;
  }
  
  public static SV newF(float f) {
    SV sv = new SV();
    sv.tok = decimal;
    sv.value = Float.valueOf(f);
    return sv;
  }
  
  public static SV newS(String s) {
    return newV(string, s);
  }
  
  public static SV newT(T x) {
    return newSV(x.tok, x.intValue, x.value);
  }

  static SV newSV(int tok, int intValue, Object value) {
    SV sv = newV(tok, value);
    sv.intValue = intValue;
    return sv;
  }

  /**
   * 
   * Creates a NEW version of the variable.
   * Object values are not copied. (Just found no 
   * use for that.)
   * 
   * 
   * @param v
   * @return  new ScriptVariable
   */
  SV setv(SV v) {
    index = v.index;
    intValue = v.intValue;
    tok = v.tok;
    value = v.value;
    return this;
  }

  @SuppressWarnings("unchecked")
  static int sizeOf(T x) {
    switch (x == null ? nada : x.tok) {
    case bitset:
      return bsSelectToken(x).cardinality();
    case on:
    case off:
      return -1;
    case integer:
      return -2;
    case decimal:
      return -4;
    case point3f:
      return -8;
    case point4f:
      return -16;
    case matrix3f:
      return -32;
    case matrix4f:
      return -64;
    case barray:
      return ((BArray) x.value).data.length;
    case string:
      return ((String) x.value).length();
    case varray:
      return x.intValue == Integer.MAX_VALUE ? ((SV)x).getList().size()
          : sizeOf(selectItemTok(x, Integer.MIN_VALUE));
    case hash:
      return ((Map<String, SV>) x.value).size();
    case context:
      return ((ScriptContext) x.value).getFullMap().size();
    default:
      return 0;
    }
  }

  /**
   * Must be updated if getVariable is updated!
   * 
   * @param x
   * @return if we recognize this as a variable
   * 
   */
  public static boolean isVariableType(Object x) {
    return (x instanceof SV
        || x instanceof Boolean
        || x instanceof Integer
        || x instanceof Float
        || x instanceof String
        || x instanceof T3    // stored as point3f
        || x instanceof BS
        || x instanceof P4    // stored as point4f
        || x instanceof Quat // stored as point4f
        || x instanceof M34
        || x instanceof Map<?, ?>  // stored as Map<String, ScriptVariable>
        || x instanceof Lst<?>
        || x instanceof BArray
        || x instanceof ScriptContext
    // in JavaScript, all these will be "Array" which is fine;
        || isArray(x)); // stored as list
  }

  /**
   * Must be updated if getVariable is updated!
   * 
   * @param x
   * @return if we recognize this as an primitive array type
   * 
   */
  private static boolean isArray(Object x) {
    /**
     * @j2sNative
     * 
     *            return Clazz.instanceOf(x, Array);
     */
    {
       return x instanceof SV[] 
           || x instanceof int[] 
           || x instanceof byte[] 
           || x instanceof float[]
           || x instanceof double[] 
           || x instanceof String[]
           || x instanceof T3[]
           || x instanceof int[][] 
           || x instanceof float[][] 
           || x instanceof String[][] 
           || x instanceof double[][] 
           || x instanceof Float[]
           || x instanceof Object[];
    }
  }


  /**
   * @param x
   * @return a ScriptVariable of the input type, or if x is null, then a new
   *         ScriptVariable, or, if the type is not found, a string version
   */
  @SuppressWarnings("unchecked")
  public static SV getVariable(Object x) {
    if (x == null)
      return newS("");
    if (x instanceof SV)
      return (SV) x;

    // the eight basic types are:
    // boolean, integer, decimal, string, point3f, point4f, bitset, and list
    // listf is a special temporary type for storing results
    // of .all in preparation for .bin in the case of xxx.all.bin
    // but with some work, this could be developed into a storage class

    if (x instanceof Boolean)
      return getBoolean(((Boolean) x).booleanValue());
    if (x instanceof Integer)
      return newI(((Integer) x).intValue());
    if (x instanceof Float)
      return newV(decimal, x);
    if (x instanceof String) {
      x = unescapePointOrBitsetAsVariable(x);
      if (x instanceof SV)
        return (SV) x;
      return newV(string, x);
    }
    if (x instanceof P3)
      return newV(point3f, x);
    if (x instanceof V3) // point3f is not mutable anyway
      return newV(point3f, P3.newP((V3) x));
    if (x instanceof BS)
      return newV(bitset, x);
    if (x instanceof P4)
      return newV(point4f, x);
    // note: for quaternions, we save them {q1, q2, q3, q0} 
    // While this may seem odd, it is so that for any point4 -- 
    // planes, axisangles, and quaternions -- we can use the 
    // first three coordinates to determine the relavent axis
    // the fourth then gives us offset to {0,0,0} (plane), 
    // rotation angle (axisangle), and cos(theta/2) (quaternion).
    if (x instanceof Quat)
      return newV(point4f, ((Quat) x).toPoint4f());
    if (x instanceof M34)
      return newV(x instanceof M4 ? matrix4f : matrix3f, x);
    if (x instanceof Map)
      return getVariableMap((Map<String, ?>)x);
    if (x instanceof Lst)
      return getVariableList((Lst<?>) x);
    if (x instanceof BArray)
      return newV(barray, x);
    if (x instanceof ScriptContext)
      return newV(context, x);
    // rest are specific array types supported
    // DO NOT ADD MORE UNLESS YOU CHANGE isArray(), above
    if (x instanceof SV[])
      return getVariableAV((SV[]) x);
    if (AU.isAI(x))
      return getVariableAI((int[]) x);
    if (AU.isAB(x))
      return getVariableAB((byte[]) x);
    if (AU.isAF(x))
      return getVariableAF((float[]) x);
    if (AU.isAD(x))
      return getVariableAD((double[]) x);
    if (AU.isAS(x))
      return getVariableAS((String[]) x);
    //NOTE: isAP(x) does not return TRUE for Atom[] in JavaScript
    // only occurrence of this was for Polyhedron.getInfo()
    if (AU.isAP(x))
      return getVariableAP((T3[]) x);
    if (AU.isAII(x))
      return getVariableAII((int[][]) x);
    if (AU.isAFF(x))
      return getVariableAFF((float[][]) x);
    if (AU.isASS(x))
      return getVariableASS((String[][]) x);
    if (AU.isADD(x))
      return getVariableADD((double[][]) x);
    if (AU.isAFloat(x))
      return newV(listf, x);
    return newJSVar(x);
  }

  /**
   * Conversion to Jmol variables of JavaScript variables using
   * y = javascript("x")
   * 
   * @param x a JavaScript variable, perhaps
   * @return SV
   */
  @SuppressWarnings("unused")
  private static SV newJSVar(Object x) {
    // JavaScript only
    int itype;
    boolean itest;
    float inum;
    Object[] array;
    String[] keys;
    /**
     * @j2sNative
     * 
     *            switch(x.BYTES_PER_ELEMENT ? Array : x.constructor) {
     *            case Boolean:
     *              itype = 0;
     *              itest = x;
     *              break;
     *            case Number:
     *              itype = 1;
     *              inum = x;
     *              break;
     *            case Array:
     *              itype = 2;
     *              array = x;
     *              break;
     *            case Object:
     *               itype = 3;
     *               array = x;
     *               keys = Object.keys(x);
     *               break;
     *            }
     *            
     */
    {
      if (x instanceof Object[])
        return getVariableAO((Object[]) x);
      if (true)
        return newS(x.toString());
    }

    // JavaScript only
    switch (itype) {
    case 0:
      return (itest ? vT : vF);
    case 1:
      return (inum > Integer.MAX_VALUE || inum != Math.floor(inum)
          ? SV.newF(inum) : newI((int) inum));
    case 2:
      Lst<SV> v = new javajs.util.Lst<SV>();
      for (int i = 0, n = array.length; i < n; i++)
        v.addLast(newJSVar(array[i]));
      return getVariableList(v);
    case 3:
      Map<String, Object> map = new Hashtable<String, Object>();
      for (int i = keys.length; --i >= 0;) {
        Object o = null;
        /**
         * @j2sNative
         * 
         *            o = array[keys[i]];
         * 
         */
        {
        }
        map.put(keys[i], newJSVar(o));
      }
      return SV.getVariableMap(map);
    }
    return newS(x.toString());    
  }

  @SuppressWarnings("unchecked")
  public static SV getVariableMap(Map<String, ?> x) {
    Map<String, Object> ht = (Map<String, Object>) x;
    Object o = null;
    for (Object oo : ht.values()) {
      o = oo;
      break;
    }
    if (!(o instanceof SV)) {
      Map<String, SV> x2 = new Hashtable<String, SV>();
      for (Map.Entry<String, Object> entry : ht.entrySet())
        x2.put(entry.getKey(), getVariable(entry.getValue()));
      x = x2;
    }
    return newV(hash, x);
  }

  public static SV getVariableList(Lst<?> v) {
    int len = v.size();
    if (len > 0 && v.get(0) instanceof SV)
      return newV(varray, v);
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < len; i++)
      objects.addLast(getVariable(v.get(i)));
    return newV(varray, objects);
  }

  static SV getVariableAV(SV[] v) {
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < v.length; i++)
      objects.addLast(v[i]);
    return newV(varray, objects);
  }

  public static SV getVariableAD(double[] f) {
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < f.length; i++)
      objects.addLast(newV(decimal, Float.valueOf((float) f[i])));
    return newV(varray, objects);
  }

  static SV getVariableAO(Object[] o) {
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < o.length; i++)
      objects.addLast(getVariable(o[i]));
    return newV(varray, objects);
  }

  static SV getVariableAS(String[] s) {
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < s.length; i++)
      objects.addLast(newV(string, s[i]));
    return newV(varray, objects);
  }

  static SV getVariableAP(T3[] p) {
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < p.length; i++)
      objects.addLast(newV(point3f, p[i]));
    return newV(varray, objects);
  }

  static SV getVariableAFF(float[][] fx) {
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < fx.length; i++)
      objects.addLast(getVariableAF(fx[i]));
    return newV(varray, objects);
  }

  static SV getVariableADD(double[][] fx) {
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < fx.length; i++)
      objects.addLast(getVariableAD(fx[i]));
    return newV(varray, objects);
  }

  static SV getVariableASS(String[][] fx) {
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < fx.length; i++)
      objects.addLast(getVariableAS(fx[i]));
    return newV(varray, objects);
  }

  static SV getVariableAII(int[][] ix) {
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < ix.length; i++)
      objects.addLast(getVariableAI(ix[i]));
    return newV(varray, objects);
  }

  static SV getVariableAF(float[] f) {
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < f.length; i++)
      objects.addLast(newV(decimal, Float.valueOf(f[i])));
    return newV(varray, objects);
  }

  static SV getVariableAI(int[] ix) {
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < ix.length; i++)
      objects.addLast(newI(ix[i]));
    return newV(varray, objects);
  }

  static SV getVariableAB(byte[] ix) {
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < ix.length; i++)
      objects.addLast(newI(ix[i]));
    return newV(varray, objects);
  }

  public SV setName(String name) {
    this.myName = name;
    //System.out.println("Variable: " + name + " " + intValue + " " + value);
    return this;
  }

  boolean canIncrement() {
    switch (tok) {
    case integer:
    case decimal:
      return true;
    default:
      return false;
    }
  }

  boolean increment(int n) {
    switch (tok) {
    case integer:
      intValue += n;
      return true;
    case decimal:
      value = Float.valueOf(((Float) value).floatValue() + n);
      return true;
    default:
      return false;
    }
  }

  public boolean asBoolean() {
    return bValue(this);
  }

  public int asInt() {
    return iValue(this);
  }

  public float asFloat() {
    return fValue(this);
  }

  public String asString() {
    return sValue(this);
  }

  // math-related Token static methods

  private final static P3 pt0 = new P3();

  /**
   * 
   * @param xx
   * @return Object-wrapped value
   */

  public static Object oValue(Object xx) {
    if (!(xx instanceof SV))
      return xx;
    SV x = (SV) xx;
    switch (x.tok) {
    case on:
      return Boolean.TRUE;
    case nada:
    case off:
      return Boolean.FALSE;
    case integer:
      return Integer.valueOf(x.intValue);
    case bitset:
    case array:
      return selectItemVar(x).value; // TODO: matrix3f?? 
    default:
      return x.value;
    }

  }

  /**
   * 
   * @param x
   * @return  numeric value -- integer or decimal
   */
  static Object nValue(T x) {
    int iValue;
    switch (x == null ? nada : x.tok) {
    case decimal:
      return x.value;
    case integer:
      iValue = x.intValue;
      break;
    case string:
      if (((String) x.value).indexOf(".") >= 0)
        return Float.valueOf(toFloat((String) x.value));
      iValue = (int) toFloat((String) x.value);
      break;
    case point3f:
      return Float.valueOf(((T3) x.value).length());
    default:
      iValue = 0;
    }
    return Integer.valueOf(iValue);
  }

  // there are reasons to use Token here rather than ScriptVariable
  // for some of these functions, in particular iValue, fValue, and sValue
  
  public static boolean bValue(T x) {
    switch (x == null ? nada : x.tok) {
    case on:
    case context:
      return true;
    case off:
      return false;
    case integer:
      return x.intValue != 0;
    case decimal:
    case string:
    case varray:
      return fValue(x) != 0;
    case bitset:
    case barray:
      return iValue(x) != 0;
    case point3f:
    case point4f:
    case matrix3f:
    case matrix4f:
      return Math.abs(fValue(x)) > 0.0001f;
    case hash:
      return !((SV) x).getMap().isEmpty();
    default:
      return false;
    }
  }

  public static int iValue(T x) {
    switch (x == null ? nada : x.tok) {
    case on:
      return 1;
    case off:
      return 0;
    case integer:
      return x.intValue;
    case decimal:
    case varray:
    case string:
//    case point3f: this makes not sense, and breaks 1/x
//    case point4f:
//    case matrix3f:
//    case matrix4f:
      return (int) fValue(x);
    case bitset:
      return bsSelectToken(x).cardinality();
    case barray:
      return ((BArray) x.value).data.length;
    default:
      return 0;
    }
  }

  public static float fValue(T x) {
    switch (x == null ? nada : x.tok) {
    case on:
      return 1;
    case off:
      return 0;
    case integer:
      return x.intValue;
    case decimal:
      return ((Float) x.value).floatValue();
    case varray:
      int i = x.intValue;
      if (i == Integer.MAX_VALUE)
        return ((SV)x).getList().size();
      //$FALL-THROUGH$
    case string:
      return toFloat(sValue(x));
    case bitset:
    case barray:
      return iValue(x);
    case point3f:
      return ((P3) x.value).length();
    case point4f:
      return Measure.distanceToPlane((P4) x.value, pt0);
    case matrix3f:
      P3 pt = new P3();
      ((M3) x.value).rotate(pt);
      return pt.length();
    case matrix4f:
      P3 pt1 = new P3();
      ((M4) x.value).rotTrans(pt1);
      return pt1.length();
    default:
      return 0;
    }
  }

  public static String sValue(T x) {
    if (x == null)
      return "";
    int i;
    SB sb;
    switch (x.tok) {
    case on:
      return "true";
    case off:
      return "false";
    case integer:
      return "" + x.intValue;
    case bitset:
      BS bs = bsSelectToken(x);
      return (x.value instanceof BondSet ? Escape.eBond(bs) : Escape.eBS(bs));
    case varray:
      Lst<SV> sv = ((SV) x).getList();
      i = x.intValue;
      if (i <= 0)
        i = sv.size() - i;
      if (i != Integer.MAX_VALUE)
        return (i < 1 || i > sv.size() ? "" : sValue(sv.get(i - 1)));
      //$FALL-THROUGH$
    case hash:
    case context:
      if (x.value instanceof String)
        return (String) x.value; // just the command
      sb = new SB();
      sValueArray(sb, (SV) x, "", "", false, true, true, Integer.MAX_VALUE, false);
      return PT.rep(sb.toString(), "\n\0", " "); // circular ref
    case string:
      String s = (String) x.value;
      i = x.intValue;
      if (i <= 0)
        i = s.length() - i;
      if (i == Integer.MAX_VALUE)
        return s;
      if (i < 1 || i > s.length())
        return "";
      return "" + s.charAt(i - 1);
    case point3f:
      return Escape.eP((T3) x.value);
    case point4f:
      return Escape.eP4((P4) x.value);
    case matrix3f:
    case matrix4f:
      return Escape.e(x.value);
    default:
      return x.value.toString();
    }
  }

  private static void sValueArray(SB sb, SV vx, String path, String tabs,
                                  boolean isEscaped, boolean isRaw,
                                  boolean addValues, int maxLevels,
                                  boolean skipEmpty) {
    switch (vx.tok) {
    case hash:
    case context:
    case varray:
      String thiskey = ";" + vx.hashCode() + ";";
      if (path.indexOf(thiskey) >= 0) {
        sb.append(isEscaped ? (vx.tok == varray ? "[ ]" : "{ }") 
            : (vx.tok == varray ? "" : "\0") + "\"<"
            + (vx.myName == null ? "circular reference" : vx.myName) + ">\"");
        break;
      }
      path += thiskey;
      if (vx.tok == varray) {
        if (!addValues)
          return;
        if (!isRaw)
          sb.append(isEscaped ? "[ " : tabs + "[\n");
        Lst<SV> sx = vx.getList();
        for (int i = 0; i < sx.size(); i++) {
          if (isEscaped && i > 0)
            sb.append(",");
          SV sv = sx.get(i);
          sValueArray(sb, sv, path, tabs + "  ", isEscaped, tabs.length() == 0
              && !isEscaped && isRawType(sv.tok), addValues, maxLevels,
              skipEmpty);
          if (!isEscaped)
            sb.append("\n");
        }
        if (!isRaw)
          sb.append(isEscaped ? " ]" : tabs + "]");
      } else if (--maxLevels >= 0) {
        Map<String, SV> ht = (vx.tok == context ? ((ScriptContext) vx.value)
            .getFullMap() : vx.getMap());
        sValueAddKeys(sb, path, ht, tabs, isEscaped, addValues, maxLevels, skipEmpty);
      }
      break;
    default:
      if (!addValues)
        return;
      if (!isRaw && !isEscaped)
        sb.append(tabs);
      sb.append(isEscaped ? vx.escape() : sValue(vx));
    }
  }
  
  @SuppressWarnings("cast")
  private static void sValueAddKeys(SB sb, String path, Map<String, SV> ht, String tabs, boolean isEscaped, boolean addValues, int maxLevels, boolean skipEmpty) {
    if (maxLevels < 0)
      return;
    Set<String> keyset = ht.keySet();
    String[] keys = ht.keySet().toArray(new String[keyset.size()]);
    Arrays.sort(keys);
    if (isEscaped) {
      sb.append("{ ");
      String sep = "";
      for (int i = 0; i < keys.length; i++) {
        String key = keys[i];
        SV val = ht.get(key);
        if (skipEmpty && (val.tok == varray && val.getList().size() == 0
            || val.tok == hash && val.getMap().isEmpty()))
          continue;
        if (addValues)
          sb.append(sep).append(PT.esc(key)).append(":");
        else
           sb.appendC(' ').append(key);
        sValueArray(sb, val, path, tabs+"  ", true, false, addValues, maxLevels, skipEmpty);
        sep = ",";
      }
      sb.append(" }");
      if (!addValues)
        sb.append("\n");

      return;
    }
    sb.append(tabs).append("{\n");
    tabs += "  ";
    for (int i = 0; i < keys.length; i++) {
      sb.append(tabs);
      String key = keys[i];
      sb.append(PT.esc(key)).append("  :");
      SB sb2 = new SB();
      if (!(ht.get(key) instanceof SV))
        ht.put(key, SV.getVariable(ht.get(key)));
      SV v = ht.get(key);
      isEscaped = isRawType(v.tok);
      sValueArray(sb2, v, path, tabs, isEscaped, false, addValues, maxLevels, skipEmpty);
      String value = sb2.toString();
      if (isEscaped && addValues)
        sb.append("  ");
      else 
        sb.append("\n");
      sb.append(value).append("\n");
    }
    sb.append(tabs.substring(1)).append("}");
  }

  private static boolean isRawType(int tok) {
    switch (tok) {
    case string:
    case decimal:
    case integer:
    case point3f:
    case point4f:
    case bitset:
    case barray:
    case on:
    case off:
      return true;
    }
    return false;
  }

  public static P3 ptValue(SV x) {
    switch (x.tok) {
    case point3f:
      return (P3) x.value;
    case string:
      Object o = Escape.uP((String) x.value);
      if (o instanceof P3)
        return (P3) o;
    }
    return null;
  }  

  public static P4 pt4Value(SV x) {
    switch (x.tok) {
    case point4f:
      return (P4) x.value;
    case string:
      Object o = Escape.uP((String) x.value);
      if (!(o instanceof P4))
        break;
      return (P4) o;
    }
    return null;
  }

  private static float toFloat(String s) {
    return (s.equalsIgnoreCase("true") ? 1 
        : s.length() == 0 || s.equalsIgnoreCase("false") ? 0 
        : PT.parseFloatStrict(PT.trim(s," \t\n")));
  }

  public static SV concatList(SV x1, SV x2, boolean asNew) {
    Lst<SV> v1 = x1.getList();
    Lst<SV> v2 = x2.getList();
    if (!asNew) {
      if (v2 == null)
        v1.addLast(newT(x2));
      else
        for (int i = 0; i < v2.size(); i++)
          v1.addLast(v2.get(i));
      return x1;
    }
    Lst<SV> vlist = new Lst<SV>();
    //(v1 == null ? 1 : v1.size()) + (v2 == null ? 1 : v2.size())
    if (v1 == null)
      vlist.addLast(x1);
    else
      for (int i = 0; i < v1.size(); i++)
        vlist.addLast(v1.get(i));
    if (v2 == null)
      vlist.addLast(x2);
    else
      for (int i = 0; i < v2.size(); i++)
        vlist.addLast(v2.get(i));
    return getVariableList(vlist);
  }

  private static BS bsSelectToken(T x) {
    return (BS) selectItemTok(x, Integer.MIN_VALUE).value;
  }

  static BS bsSelectRange(T x, int n) {
    x = selectItemTok(x, Integer.MIN_VALUE);
    x = selectItemTok(x, (n <= 0 ? n : 1));
    x = selectItemTok(x, (n <= 0 ? Integer.MAX_VALUE - 1 : n));
    return (BS) x.value;
  }

  static SV selectItemVar(SV var) {
    // pass bitsets created by the select() or for() inline functions
    // and all arrays by reference
    return (var.index != Integer.MAX_VALUE
        || (var.tok == varray || var.tok == barray)
        && var.intValue == Integer.MAX_VALUE ? var : (SV) selectItemTok(var,
        Integer.MIN_VALUE));
  }

  static T selectItemTok(T tokenIn, int i2) {
    switch (tokenIn.tok) {
    case matrix3f:
    case matrix4f:
    case bitset:
    case varray:
    case barray:
    case string:
      break;
    default:
      return ((tokenIn instanceof SV) && ((SV) tokenIn).myName != null 
      ? newI(0).setv((SV) tokenIn) 
          : tokenIn);
    }

    // negative number is a count from the end

    BS bs = null;
    String s = null;

    int i1 = tokenIn.intValue;
    boolean isOne = (i2 == Integer.MIN_VALUE);
    if (i1 == Integer.MAX_VALUE) {
      // no selections have been made yet --
      // we just create a new token with the
      // same bitset and now indicate either
      // the selected value or "ALL" (max_value)
      return newSV(tokenIn.tok, (isOne ? i1 : i2), tokenIn.value);
    }
    int len = 0;
    boolean isInputSelected = (tokenIn instanceof SV && ((SV) tokenIn).index != Integer.MAX_VALUE);
    SV tokenOut = newSV(tokenIn.tok, Integer.MAX_VALUE, null);

    switch (tokenIn.tok) {
    case bitset:
      if (tokenIn.value instanceof BondSet) {
        bs = BondSet.newBS((BS) tokenIn.value,
            ((BondSet) tokenIn.value).associatedAtoms);
        len = bs.cardinality();
      } else {
        bs = BSUtil.copy((BS) tokenIn.value);
        len = (isInputSelected ? 1 : bs.cardinality());
      }
      break;
    case barray:
      len = ((BArray) (((SV) tokenIn).value)).data.length;
      break;
    case varray:
      len = ((SV) tokenIn).getList().size();
      break;
    case string:
      s = (String) tokenIn.value;
      len = s.length();
      break;
    case matrix3f:
      len = -3;
      break;
    case matrix4f:
      len = -4;
      break;
    }

    if (len < 0) {
      // matrix mode [1][3] or [13]
      len = -len;
      if (i1 > 0 && Math.abs(i1) > len) {
        int col = i1 % 10;
        int row = (i1 - col) / 10;
        if (col > 0 && col <= len && row <= len) {
          if (tokenIn.tok == matrix3f)
            return newV(decimal, Float.valueOf(((M3) tokenIn.value).getElement(
                row - 1, col - 1)));
          return newV(decimal,
              Float.valueOf(((M4) tokenIn.value).getElement(row - 1, col - 1)));
        }
        return newV(string, "");
      }
      if (Math.abs(i1) > len)
        return newV(string, "");
      float[] data = new float[len];
      if (len == 3) {
        if (i1 < 0)
          ((M3) tokenIn.value).getColumn(-1 - i1, data);
        else
          ((M3) tokenIn.value).getRow(i1 - 1, data);
      } else {
        if (i1 < 0)
          ((M4) tokenIn.value).getColumn(-1 - i1, data);
        else
          ((M4) tokenIn.value).getRow(i1 - 1, data);
      }
      if (isOne)
        return getVariableAF(data);
      if (i2 < 1 || i2 > len)
        return newV(string, "");
      return newV(decimal, Float.valueOf(data[i2 - 1]));
    }

    // "testing"[0] gives "g"
    // "testing"[-1] gives "n"
    // "testing"[3][0] gives "sting"
    // "testing"[-1][0] gives "ng"
    // "testing"[0][-2] gives just "g" as well
    // "testing"[-10] gives ""
    if (i1 <= 0)
      i1 = len + i1;
    if (!isOne) {
      if (i1 < 1)
        i1 = 1;
      if (i2 == 0)
        i2 = len;
      else if (i2 < 0)
        i2 = len + i2;
      if (i2 < i1)
        i2 = i1;
    }

    switch (tokenIn.tok) {
    case bitset:
      tokenOut.value = bs;
      if (isInputSelected) {
        if (i1 > 1)
          bs.clearAll();
        break;
      }
      if (isOne) {
        // i2 will be Integer.MIN_VALUE at this point
        // take care of easy ones the easy way
        if (i1 == len) {
          // {xxx}[0]
          i2 = bs.length() - 1;
        } else if (i1 == 1) {
          // {xxx}[1]
          i2 = bs.nextSetBit(0);
        }
        if (i2 >= -1) {
          bs.clearAll();
          if (i2 >= 0)
            bs.set(i2);
          break;
        }
        i2 = i1;
      }
      int n = 0;
      for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1))
        if (++n < i1 || n > i2)
          bs.clear(j);
      break;
    case string:
      tokenOut.value = (--i1 < 0 || i1 >= len ? "" : isOne ? s.substring(i1,
          i1 + 1) : s.substring(i1, Math.min(i2, len)));
      break;
    case varray:
      if (--i1 < 0 || i1 >= len)
        return newV(string, "");
      if (isOne)
        return ((SV) tokenIn).getList().get(i1);
      Lst<SV> o2 = new Lst<SV>();
      Lst<SV> o1 = ((SV) tokenIn).getList();

      int nn = Math.min(i2, len) - i1;
      for (int i = 0; i < nn; i++)
        o2.addLast(newT(o1.get(i + i1)));
      tokenOut.value = o2;
      break;
    case barray:
      if (--i1 < 0 || i1 >= len)
        return newV(string, "");
      byte[] data = ((BArray) (((SV) tokenIn).value)).data;
      if (isOne)
        return newI(data[i1]);
      byte[] b = new byte[Math.min(i2, len) - i1];
      for (int i = b.length; --i >= 0;)
        b[i] = data[i1 + i];
      tokenOut.value = new BArray(b);
      break;
    }
    return tokenOut;
  }

  void setSelectedValue(int pt1, int pt2, SV var) {
    if (pt1 == Integer.MAX_VALUE)
      return;
    int len;
    switch (tok) {
    case matrix3f:
    case matrix4f:
      len = (tok == matrix3f ? 3 : 4);
      if (pt2 != Integer.MAX_VALUE) {
        int col = pt2;
        int row = pt1;
        if (col > 0 && col <= len && row <= len) {
          if (tok == matrix3f)
            ((M3) value).setElement(row - 1, col - 1, fValue(var));
          else
            ((M4) value).setElement(row - 1, col - 1, fValue(var));
          return;
        }
      }
      if (pt1 != 0 && Math.abs(pt1) <= len
          && var.tok == varray) {
        Lst<SV> sv = var.getList();
        if (sv.size() == len) {
          float[] data = new float[len];
          for (int i = 0; i < len; i++)
            data[i] = fValue(sv.get(i));
          if (pt1 > 0) {
            if (tok == matrix3f)
              ((M3) value).setRowA(pt1 - 1, data);
            else
              ((M4) value).setRowA(pt1 - 1, data);
          } else {
            if (tok == matrix3f)
              ((M3) value).setColumnA(-1 - pt1, data);
            else
              ((M4) value).setColumnA(-1 - pt1, data);
          }
          break;
        }
      }
      break;
    case string:
      String str = (String) value;
      int pt = str.length();
      if (pt1 <= 0)
        pt1 = pt + pt1;
      if (--pt1 < 0)
        pt1 = 0;
      while (pt1 >= str.length())
        str += " ";
      if (pt2 == Integer.MAX_VALUE){
        pt2 = pt1;
      } else {
        if (--pt2 < 0)
          pt2 = pt + pt2;
        while (pt2 >= str.length())
          str += " ";
      }
      if (pt2 >= pt1)
        value = str.substring(0, pt1) + sValue(var)
          + str.substring(++pt2);
      intValue = index = Integer.MAX_VALUE;
      break;
    case varray:
      @SuppressWarnings("unchecked")
      Lst<SV> v = (Lst<SV>) value;
      len = v.size();
      if (pt1 <= 0)
        pt1 = len + pt1;
      if (--pt1 < 0)
        pt1 = 0;
      if (len <= pt1)
        for (int i = len; i <= pt1; i++)
          v.addLast(newV(string, ""));
      v.set(pt1, var);
      break;
    }
  }

  public String escape() {
    switch (tok) {
    case string:
      return PT.esc((String) value);
    case matrix3f:
    case matrix4f:
      return PT.toJSON(null, value);
    case varray:
    case hash:
    case context:
      SB sb = new SB();
      sValueArray(sb, this, "", "", true, false, true, Integer.MAX_VALUE, false);
      return sb.toString();
    default:
      return sValue(this);
    }
  }

  public static Object unescapePointOrBitsetAsVariable(Object o) {
    if (o == null)
      return o;
    Object v = null;
    String s = null;
    if (o instanceof SV) {
      SV sv = (SV) o;
      switch (sv.tok) {
      case point3f:
      case point4f:
      case matrix3f:
      case matrix4f:
      case bitset:
        v = sv.value;
        break;
      case string:
        s = (String) sv.value;
        break;
      default:
        s = sValue(sv);
        break;
      }
    } else if (o instanceof String) {
      s = (String) o;
    }
    if (s != null && s.length() == 0)
      return s;
    if (v == null)
      v = Escape.uABsM(s);
    if (v instanceof P3)
      return (newV(point3f, v));
    if (v instanceof P4)
      return newV(point4f, v);
    if (v instanceof BS) {
      if (s != null && s.indexOf("[{") == 0)
        v = BondSet.newBS((BS) v, null);
      return newV(bitset, v);
    }
    if (v instanceof M34)
      return (newV(v instanceof M3 ? matrix3f : matrix4f, v));
    return o;
  }

  public static SV getBoolean(boolean value) {
    return newT(value ? vT : vF);
  }
  
  public static Object sprintf(String strFormat, SV var) {
    if (var == null)
      return strFormat;
    boolean isArray = (var.tok == varray);
    int[] vd = (strFormat.indexOf("d") >= 0 || strFormat.indexOf("i") >= 0 ? new int[1]
        : null);
    float[] vf = (strFormat.indexOf("f") >= 0 ? new float[1] : null);
    double[] ve = (strFormat.indexOf("e") >= 0 ? new double[1] : null);
    boolean getS = (strFormat.indexOf("s") >= 0);
    boolean getP = (strFormat.indexOf("p") >= 0 && (isArray || var.tok == point3f));
    boolean getQ = (strFormat.indexOf("q") >= 0 && (isArray || var.tok == point4f));
    Object[] of = new Object[] { vd, vf, ve, null, null, null};
     if (!isArray)
      return sprintf(strFormat, var, of, vd, vf, ve, getS, getP, getQ);
    Lst<SV> sv = var.getList();
    String[] list2 = new String[sv.size()];
    for (int i = 0; i < list2.length; i++)
      list2[i] = sprintf(strFormat, sv.get(i), of, vd, vf, ve, getS, getP, getQ);
    return list2;
  }

  private static String sprintf(String strFormat, SV var, Object[] of, 
                                int[] vd, float[] vf, double[] ve, boolean getS, boolean getP, boolean getQ) {
    if (var.tok == hash) {
      int pt = strFormat.indexOf("[");
      if (pt >= 0) {
        int pt1;
        var = var.getMap().get(strFormat.substring(pt + 1, pt1 = strFormat.indexOf("]")));
        strFormat = strFormat.substring(0, pt) + strFormat.substring(pt1 + 1);
      }      
   }
    if (vd != null)
      vd[0] = iValue(var);
    if (vf != null)
      vf[0] = fValue(var);
    if (ve != null)
      ve[0] = fValue(var);
    if (getS)
      of[3] = sValue(var);
    if (getP)
      of[4]= var.value;
    if (getQ)
      of[5]= var.value;
    return PT.sprintf(strFormat, "IFDspq", of );
  }

  /**
   * 
   * @param format
   * @return 0: JSON, 5: base64, 12: bytearray, 22: array
   */
  public static int getFormatType(String format) {
    return (format.indexOf(";") >= 0 ? -1 :
        ";json;base64;bytearray;array;"
    //   0    5      12        22
        .indexOf(";" + format.toLowerCase() + ";"));
  }

  /**
   * Accepts arguments from the format() function First argument is a format
   * string.
   * 
   * @param args
   * @param pt
   *        0: to JSON, 5: to base64, 12: to bytearray, 22: to array
   * @return formatted string
   */
  public static Object format(SV[] args, int pt) {
    switch (args.length) {
    case 0:
      return "";
    case 1:
      return sValue(args[0]);
    case 2:
      if (pt == Integer.MAX_VALUE)
        pt = getFormatType(args[0].asString());
      switch (pt) {
      case 0:
        String name = args[1].myName;
        args[1].myName = null;
        Object o = args[1].toJSON();
        args[1].myName = name;
        return o;
      case 5:
      case 12:
      case 22:
        byte[] bytes;
        switch (args[1].tok) {
        case barray:
          bytes = AU.arrayCopyByte(((BArray) args[1].value).data, -1);
          break;
        case varray:
          Lst<SV> l = args[1].getList();
          if (pt == 22) {
            Lst<SV> l1 = new Lst<SV>();
            for (int i = l.size(); --i >= 0;)
              l1.addLast(l.get(i));
            return l1;
          }
          bytes = new byte[l.size()];
          for (int i = bytes.length; --i >= 0;)
            bytes[i] = (byte) l.get(i).asInt();
          break;
        default:
          String s = args[1].asString();
          if (s.startsWith(";base64,")) {
            if (pt == 5)
              return s;
            bytes = Base64.decodeBase64(s);
          } else {
            bytes = s.getBytes();
          }
        }
        return (pt == 22 ? getVariable(bytes) : pt == 12 ? new BArray(bytes)
            : ";base64," + javajs.util.Base64.getBase64(bytes).toString());
      }
    }
    // use values to replace codes in format string
    String[] format = PT.split(PT.rep(sValue(args[0]), "%%", "\1"), "%");
    if (format.length == 0)
      return "";
    SB sb = new SB();
    sb.append(format[0]);
    for (int i = 1; i < format.length; i++) {
      Object ret = sprintf(PT.formatCheck("%" + format[i]),
          (args[1].tok == hash ? args[1] : args[1].tok == varray ? args[1]
              .getList().get(i - 1) : i < args.length ? args[i] : null));
      if (AU.isAS(ret)) {
        String[] list = (String[]) ret;
        for (int j = 0; j < list.length; j++)
          sb.append(list[j]).append("\n");
        continue;
      }
      sb.append((String) ret);
    }
    return sb.toString();
  }
  
  public static BS getBitSet(SV x, boolean allowNull) {
    switch (x.tok) {
    case bitset:
      // selectItemTok is important here because this may come from setVariable()
      // in the case of     a[1].xyz = ptX1
      return (BS) (x.index == Integer.MAX_VALUE ? (SV) selectItemTok(x,
          Integer.MIN_VALUE) : x).value;
    case varray:
      return unEscapeBitSetArray(x.getList(), allowNull);
    default:
      return (allowNull ? null : new BS());
    }
  }

  /**
   * Turn an array of strings in the form of "{n,n,n:n...} or an array of
   * integers into a bitset.
   * 
   * @param x
   * @param allowNull
   * @return bitset (or null if fails and allowNull is false)
   */
  static BS unEscapeBitSetArray(Lst<SV> x, boolean allowNull) {
    BS bs = new BS();
    for (int i = 0; i < x.size(); i++) {
      SV v = x.get(i);
      if (v.tok == T.integer && v.intValue >= 0) {
        bs.set(v.intValue);
      } else if (v.tok == T.varray) {
        BS bs2 = unEscapeBitSetArray(v.getList(), true);
        if (bs2 == null)
          return (allowNull ? null : new BS());
        bs.or(bs2);
      } else if (!unEscapeBitSet(v, bs)) {
        return (allowNull ? null : new BS());
      }
    }
    return bs;
  }


  /**
   * For legacy reasons, "x" == "X" but see isLike()
   * 
   * @param x1
   * @param x2
   * @return x1 == x2
   */
  public static boolean areEqual(SV x1, SV x2) {
    if (x1 == null || x2 == null)
      return false;
    if (x1.tok == x2.tok) {
      switch (x1.tok) {
      case string:
        return ((String)x1.value).equalsIgnoreCase((String) x2.value);
      case bitset:
      case barray:
      case hash:
      case varray:
      case context:
        return x1.equals(x2);
      case point3f:
        return (((P3) x1.value).distance((P3) x2.value) < 0.000001);
      case point4f:
        return (((P4) x1.value).distance4((P4) x2.value) < 0.000001);
      case matrix3f:
        return ((M3) x1.value).equals(x2.value);
      case matrix4f:
        return ((M4) x1.value).equals(x2.value);
      }
    }
    return (Math.abs(fValue(x1) - fValue(x2)) < 0.000001);
  }

  /**
   * a LIKE "x"    a is a string and equals x
   * a LIKE "*x"   a is a string and ends with x
   * a LIKE "x*"   a is a string and starts with x
   * a LIKE "*x*"  a is a string and contains x
   *  
   * @param x1
   * @param x2
   * @return  x1 LIKE x2
   */
  public static boolean isLike(SV x1, SV x2) {
    return (x1 != null && x2 != null 
        && x1.tok == string && x2.tok == string
        && PT.isLike((String)x1.value, (String) x2.value));
  }

  protected class Sort implements Comparator<SV> {
    private int arrayPt;
    private String myKey;
    
    protected Sort(int arrayPt, String myKey) {
      this.arrayPt = arrayPt;
      this.myKey = myKey;
    }
    
    @Override
    public int compare(SV x, SV y) {
      if (x.tok != y.tok) {
        if (x.tok == decimal || x.tok == integer || y.tok == decimal
            || y.tok == integer) {
          float fx = fValue(x);
          float fy = fValue(y);
          return (fx < fy ? -1 : fx > fy ? 1 : 0);
        }
        if (x.tok == string || y.tok == string)
          return sValue(x).compareTo(sValue(y));
      }
      switch (x.tok) {
      case string:
        return sValue(x).compareTo(sValue(y));
      case varray:
        Lst<SV> sx = x.getList();
        Lst<SV> sy = y.getList();
        if (sx.size() != sy.size())
          return (sx.size() < sy.size() ? -1 : 1);
        int iPt = arrayPt;
        if (iPt < 0)
          iPt += sx.size();
        if (iPt < 0 || iPt >= sx.size())
          return 0;
        return compare(sx.get(iPt), sy.get(iPt));
      case hash:
        if (myKey != null) {
          return compare(x.getMap().get(myKey), y.getMap().get(myKey));
        }
        //$FALL-THROUGH$
      default:
        float fx = fValue(x);
        float fy = fValue(y);
        return (fx < fy ? -1 : fx > fy ? 1 : 0);
      }
    } 
  }
  
  /**
   * 
   * @param arrayPt
   *        1-based or Integer.MIN_VALUE to reverse
   * @return sorted or reversed array
   */
  public SV sortOrReverse(int arrayPt) {
    Lst<SV> x = getList();
    if (x != null && x.size() > 1) {
      if (arrayPt == Integer.MIN_VALUE) {
        // reverse
        int n = x.size();
        for (int i = 0; i < n; i++) {
          SV v = x.get(i);
          x.set(i, x.get(--n));
          x.set(n, v);
        }
      } else {
        Collections.sort(getList(), new Sort(--arrayPt, null));
      }
    }
    return this;
  }

  /**
   * 
   * Script variables are pushed after cloning, because the name comes with them
   * when we do otherwise they are not mutable anyway. We do want to have actual
   * references to points, lists, and associative arrays
   * @param mapKey
   * @param value
   *        null to pop
   * 
   * @return array
   */
  public SV pushPop(SV mapKey, SV value) {
    if (mapKey == null) {
      Map<String, SV> m = getMap();
      if (m == null) {
        Lst<SV> x = getList();
        if (value == null || x == null) {
          // array.pop()
          return (x == null || x.size() == 0 ? newS("") : x.removeItemAt(x
              .size() - 1));
        }
        // array.push(value)
        x.addLast(newI(0).setv(value));
      } else {
        if (value == null) {
          // assocArray.pop()
          m.clear();   // new Jmol 14.18
        } else {
          Map<String, SV> m1 = value.getMap();
          if (m1 != null)
            m.putAll(m1);  // new Jmol 14.18
          // assocArray.push(value)
        }
      }
    } else {
      Map<String, SV> m = getMap();
      if (value == null) {
        SV v = null;
        if (m == null) {
          // array.pop(i)
          Lst<SV> lst = getList();
          int len = lst.size();
          int i = iValue(mapKey) - 1;
          if (i < 0)
            i += len;
          if (i >= 0 && i < len) {
            v = lst.removeItemAt(i);
          }
        } else {
          // assocArray.pop(key)
          v = m.remove(mapKey.asString());
        }
        return (v == null ? newS("") : v);
      }
      if (m != null) {
        //assocArray.push(key,value)
        m.put(mapKey.asString(), newI(0).setv(value));
      }
    }
    return this;
  }

  /**
   * Turn the string "({3:5})" into a bitset
   * @param x 
   * 
   * @param bs
   * @return a bitset or a string converted to one
   */
  private static boolean unEscapeBitSet(SV x, BS bs) {
    switch(x.tok) {
    case string:
      BS bs1 = BS.unescape((String) x.value);
      if (bs1 == null)
        return false;
      bs.or(bs1);
      return true;
    case bitset:
      bs.or((BS) x.value);
      return true;
    }
    return false;   
  }

  public static String[] strListValue(T x) {
    if (x.tok != varray)
      return new String[] { sValue(x) };
    Lst<SV> sv = ((SV) x).getList();
    String[] list = new String[sv.size()];
    for (int i = sv.size(); --i >= 0;)
      list[i] = sValue(sv.get(i));
    return list;
  }

  public static float[] flistValue(T x, int nMin) {
    if (x.tok != varray)
      return new float[] { fValue(x) };
    Lst<SV> sv = ((SV) x).getList();
    float[] list;
    list = new float[Math.max(nMin, sv.size())];
    if (nMin == 0)
      nMin = list.length;
    for (int i = Math.min(sv.size(), nMin); --i >= 0;)
      list[i] = fValue(sv.get(i));
    return list;
  }

  public SV toArray() {
    int dim;
    Lst<SV> o2;
    M3 m3 = null;
    M4 m4 = null;
    switch (tok) {
    case matrix3f:
      m3 = (M3) value;
      dim = 3;
      break;
    case matrix4f:
      m4 = (M4) value;
      dim = 4;
      break;
    case varray:
      return this;
    default:
      o2 = new Lst<SV>();
      o2.addLast(this);
      return newV(varray, o2);
    }
    o2 = new  Lst<SV>();
    for (int i = 0; i < dim; i++) {
      float[] a = new float[dim];
      if (m3 == null)
        m4.getRow(i, a);
      else
        m3.getRow(i, a);
      o2.addLast(getVariableAF(a));
    }
    return newV(varray, o2);
  }

  @SuppressWarnings("unchecked")
  SV mapValue(String key) {
    switch (tok) {
    case hash:
      return ((Map<String, SV>) value).get(key);
    case context:
      ScriptContext sc = ((ScriptContext) value);
      return (key.equals("_path") ? newS(sc.contextPath) : sc.getVariable(key));
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public Lst<SV> getList() {
    return (tok == varray ? (Lst<SV>) value : null);
  }

  public static boolean isScalar(SV x) {
    switch (x.tok) {
    case varray:
      return false;
    case string:
      return (((String) x.value).indexOf("\n") < 0);
    default:
      return true;
    }
  }

  @Override
  public String toJSON() {
    switch (tok) {
    case on:
    case off:
    case integer:
    case decimal:
      return sValue(this);
    case barray:
      return PT.byteArrayToJSON(((BArray) value).data);
    case context:
      return PT.toJSON(null, ((ScriptContext) value).getFullMap());
    case varray:
    case hash:
      if (myName != null) {
        myName = null;
        return (tok == hash ? "{  }" : "[  ]");
      }
      myName = "x";
      String s = PT.toJSON(null, value);
      myName = null;
      return s;
    default:
      return PT.toJSON(null, value);
    }
  }

  public SV mapGet(String key) {
    return getMap().get(key);
  }

  public void mapPut(String key, SV v) {
    getMap().put(key, v);
  }

  @SuppressWarnings("unchecked")
  public Map<String, SV> getMap() {
    switch (tok) {
    case hash:
      return (Map<String, SV>) value;
    case context:
      return ((ScriptContext) value).vars;
    }
    return null;
  }

  public String getMapKeys(int nLevels, boolean skipEmpty) {
    if (tok != hash)
      return "";
    SB sb = new SB();
    sValueArray(sb, this, "", "", true, false, false, nLevels + 1, skipEmpty);
    return sb.toString();
  }

  @Override
  public String toString() {
    return toString2() + "[" + myName + " index =" + index + " intValue=" + intValue + "]";
  }

  public String[] getKeys(boolean isAll) {
    switch (tok) {
    case hash:
    case context:
    case varray:
      break;
    default:
      return null;
    }
    Lst<String> keys = new Lst<String>();
    getKeyList(isAll, keys, "");
    String[] skeys = keys.toArray(new String[keys.size()]);
    Arrays.sort(skeys);
    return skeys;
  }

  private void getKeyList(boolean isAll, Lst<String> keys, String prefix) {
    Map<String, SV> map = getMap();
    if (map == null) {
      if (isAll) {
        Lst<SV> lst;
        int n;
        if ((lst = getList()) != null && (n = lst.size()) > 0)
          lst.get(n - 1).getKeyList(true, keys, prefix + "." + n + ".");
      }
      return;
    }
    for(Entry<String, SV> e: map.entrySet()) {
      String k = e.getKey();
      if (isAll && (k.length() == 0 || !PT.isLetter(k.charAt(0)))) {
        if (prefix.endsWith("."))
          prefix = prefix.substring(0, prefix.length() - 1);
        k = "[" + PT.esc(k) + "]";
      }
      keys.addLast(prefix + k);
      if (isAll)
        e.getValue().getKeyList(true, keys, prefix + k + ".");
    }
  }

  /**
   * Copies a hash or array deeply; invoked by Jmol script
   * 
   * x = @a
   * 
   * where a.type == "hash" or a.type == "varray"
   * 
   * 
   * @param v hash or array
   * @param isHash
   * @param isDeep TODO
   * @return deeply copied variable
   */
  @SuppressWarnings("unchecked")
  public static Object deepCopy(Object v, boolean isHash, boolean isDeep) {
    if (isHash) {
      Map<String, SV> vold = (Map<String, SV>) v;
      Map<String, SV> vnew = new Hashtable<String, SV>();
      for (Entry<String, SV> e : vold.entrySet()) {
        SV v1 = e.getValue();
        vnew.put(e.getKey(), isDeep ? deepCopySV(v1) : v1);
      }
      return vnew; 
    }
    Lst<SV> vold2 = (Lst<SV>) v;
    Lst<SV> vnew2 = new Lst<SV>();
    for (int i = 0, n = vold2.size(); i < n; i++) {
      SV vm = vold2.get(i);
      vnew2.addLast(isDeep ? deepCopySV(vm) : vm);
    }
    return vnew2;
  }

  private static SV deepCopySV(SV vm) {
    switch (vm.tok) {
    case hash:
    case varray:
      if ("\r".equals(vm.myName)) {
        vm.myName = null;
        vm = SV.newV(vm.tok, (vm.tok == hash ? new Hashtable<String, SV>() : new Lst<SV>()));
      } else {
        String name0 = vm.myName;
        vm.myName = "\r";
        SV vm0 = vm;
        vm = newV(vm.tok, deepCopy(vm.value, vm.tok == hash, true));
        vm0.myName = name0;
      }
      break;
    }
    return vm;
  }

  public SV sortMapArray(String key) {
    Lst<SV> lst = getList();
    if (lst != null) {      
      Collections.sort(getList(), new Sort(0, key));
    }
    return this;
  }

  /**
   * Safely create a JSON key - object pair, allowing for already-named arrays
   * 
   * @param key
   * @param property
   * @return JSON object
   */
  public static Object safeJSON(String key, Object property) {
    return "{"
        + (property instanceof SV ? PT.esc(key) + " : " + format(new SV[] { null, (SV) property },
            0) : PT.toJSON(key, property)) + "}";
  }

}
