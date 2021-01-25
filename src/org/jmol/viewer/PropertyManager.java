/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2017-12-14 05:53:15 -0600 (Thu, 14 Dec 2017) $
 * $Revision: 21773 $
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
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.viewer;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.jmol.adapter.writers.QCJSONWriter;
import org.jmol.api.Interface;
import org.jmol.api.JmolDataManager;
import org.jmol.api.JmolPropertyManager;
import org.jmol.api.SymmetryInterface;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.BondSet;
import org.jmol.modelset.Chain;
import org.jmol.modelset.Group;
import org.jmol.modelset.LabelToken;
import org.jmol.modelset.Model;
import org.jmol.modelset.ModelSet;
import org.jmol.modelsetbio.BioModel;
import org.jmol.script.SV;
import org.jmol.script.T;
import org.jmol.shape.Shape;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Edge;
import org.jmol.util.Escape;
import org.jmol.util.JmolMolecule;
import org.jmol.util.Logger;
import org.jmol.util.MolWriter;
import org.jmol.viewer.binding.Binding;

import javajs.util.AU;
import javajs.util.BArray;
import javajs.util.BS;
import javajs.util.Base64;
import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.OC;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.Quat;
import javajs.util.SB;
import javajs.util.V3;
import javajs.util.XmlUtil;

/**
 * 
 * The PropertyManager handles all operations relating to delivery of properties
 * with the getProperty() method, or its specifically cast forms
 * getPropertyString() or getPropertyJSON().
 * 
 * It is instantiated by reflection
 * 
 */


public class PropertyManager implements JmolPropertyManager {

  public PropertyManager() {
    // required for reflection
  }

  Viewer vwr;
  private Map<String, Integer> map = new Hashtable<String, Integer>();

  @Override
  public void setViewer(Viewer vwr) {
    this.vwr = vwr;
    for (int i = 0, p = 0; i < propertyTypes.length; i += 3, p++)
      if (propertyTypes[i].length() > 0)
        map.put(propertyTypes[i].toLowerCase(), Integer.valueOf(p));
  }

  @Override
  public int getPropertyNumber(String infoType) {
    Integer n = map.get(infoType == null ? "" : infoType.toLowerCase());
    return (n == null ? -1 : n.intValue());
  }

  @Override
  public String getDefaultPropertyParam(int propID) {
    return (propID < 0 ? "" : propertyTypes[propID * 3 + 2]);
  }

  @Override
  public boolean checkPropertyParameter(String name) {
    int propID = getPropertyNumber(name);
    String type = getParamType(propID);
    return (type.length() > 0 && type != atomExpression);
  }

  private final static String atomExpression = "<atom selection>";

  private final static String[] propertyTypes = {
    "appletInfo"      , "", "",
    "fileName"        , "", "",
    "fileHeader"      , "", "",
    "fileContents"    , "<pathname>", "",
    "fileContents"    , "", "",
    "animationInfo"   , "", "",
    "modelInfo"       , atomExpression, "{*}",
    //"X -vibrationInfo", "", "",  //not implemented -- see modelInfo
    "ligandInfo"      , atomExpression, "{*}",
    "shapeInfo"       , "", "",
    "measurementInfo" , "", "",
    
    "centerInfo"      , "", "",
    "orientationInfo" , "", "",
    "transformInfo"   , "", "",
    ""                , "", "", //was prior to 14.3.17 "atomList"        , atomExpression, "(visible)",
    "atomInfo"        , atomExpression, "(visible)",
    
    "bondInfo"        , atomExpression, "(visible)",
    "chainInfo"       , atomExpression, "(visible)",
    "polymerInfo"     , atomExpression, "(visible)",
    "moleculeInfo"    , atomExpression, "(visible)",
    "stateInfo"       , "<state type>", "all",
    
    "extractModel"    , atomExpression, "(visible)",
    "jmolStatus"      , "statusNameList", "",
    "jmolViewer"      , "", "",
    "messageQueue"    , "", "",
    "auxiliaryInfo"   , atomExpression, "{*}",
    
    "boundBoxInfo"    , "", "",  
    "dataInfo"        , "<data type>", "types",
    "image"           , "<width=www,height=hhh>", "",
    "evaluate"        , "<expression>", "",
    "menu"            , "<type>", "current",
    "minimizationInfo", "", "",
    "pointGroupInfo"  , atomExpression, "(visible)",
    "fileInfo"        , "<type>", "",
    "errorMessage"    , "", "",
    "mouseInfo"       , "", "",
    "isosurfaceInfo"  , "", "",
    "isosurfaceData"  , "", "",
    "consoleText"     , "", "",
    "JSpecView"       , "<key>", "",
    "scriptQueueInfo" , "", "",
    "nmrInfo" , "<elementSymbol> or 'all' or 'shifts'", "all",
    "variableInfo","<name>","all",
    "domainInfo"  , atomExpression, "{visible}",
    "validationInfo"  , atomExpression, "{visible}",
    "service"    , "<hashTable>", "",
    "CIFInfo"        , "<filename>", "",
    "modelkitInfo", "<key>","data",

  };

  private final static int PROP_APPLET_INFO = 0;
  private final static int PROP_FILENAME = 1;
  private final static int PROP_FILEHEADER = 2;
  private final static int PROP_FILECONTENTS_PATH = 3;
  private final static int PROP_FILECONTENTS = 4;

  private final static int PROP_ANIMATION_INFO = 5;
  private final static int PROP_MODEL_INFO = 6;
  //private final static int PROP_VIBRATION_INFO = 7; //not implemented -- see auxiliaryInfo
  private final static int PROP_LIGAND_INFO = 7;
  private final static int PROP_SHAPE_INFO = 8;
  private final static int PROP_MEASUREMENT_INFO = 9;

  private final static int PROP_CENTER_INFO = 10;
  private final static int PROP_ORIENTATION_INFO = 11;
  private final static int PROP_TRANSFORM_INFO = 12;
  
  private final static int PROP_ATOM_INFO = 14;
  
  private final static int PROP_BOND_INFO = 15;
  private final static int PROP_CHAIN_INFO = 16;
  private final static int PROP_POLYMER_INFO = 17;
  private final static int PROP_MOLECULE_INFO = 18;
  private final static int PROP_STATE_INFO = 19;

  private final static int PROP_EXTRACT_MODEL = 20;
  private final static int PROP_JMOL_STATUS = 21;
  private final static int PROP_JMOL_VIEWER = 22;
  private final static int PROP_MESSAGE_QUEUE = 23;
  private final static int PROP_AUXILIARY_INFO = 24;

  private final static int PROP_BOUNDBOX_INFO = 25;
  private final static int PROP_DATA_INFO = 26;
  private final static int PROP_IMAGE = 27;
  private final static int PROP_EVALUATE = 28;
  private final static int PROP_MENU = 29;
  private final static int PROP_MINIMIZATION_INFO = 30;
  private final static int PROP_POINTGROUP_INFO = 31;
  private final static int PROP_FILE_INFO = 32;
  private final static int PROP_ERROR_MESSAGE = 33;
  private final static int PROP_MOUSE_INFO = 34;
  private final static int PROP_ISOSURFACE_INFO = 35;
  private final static int PROP_ISOSURFACE_DATA = 36;
  private final static int PROP_CONSOLE_TEXT = 37;
  private final static int PROP_JSPECVIEW = 38;
  private final static int PROP_SCRIPT_QUEUE_INFO = 39;
  private final static int PROP_NMR_INFO = 40;
  private final static int PROP_VAR_INFO = 41;
  private final static int PROP_DOM_INFO = 42;
  private final static int PROP_VAL_INFO = 43;
  private final static int PROP_SERVICE = 44;
  private final static int PROP_CIF_INFO = 45;
  private final static int PROP_MODELKIT_INFO = 46;
  private final static int PROP_COUNT = 47;

  //// static methods used by Eval and Viewer ////

  @Override
  public Object getProperty(String returnType, String infoType,
                            Object paramInfo) {
    if (propertyTypes.length != PROP_COUNT * 3)
      Logger.warn("propertyTypes is not the right length: "
          + propertyTypes.length + " != " + PROP_COUNT * 3);
    Object info;
    if (infoType.indexOf(".") >= 0 || infoType.indexOf("[") >= 0) {
      SV[] args = getArguments(infoType);
      Object h = getPropertyAsObject(args[0].asString(), paramInfo, null);
      info = extractProperty(h, args, 1, null, false);
    } else {
      info = getPropertyAsObject(infoType, paramInfo, returnType);
    }
    if (returnType == null)
      return info;
    boolean requestedReadable = returnType.equalsIgnoreCase("readable");
    if (requestedReadable)
      returnType = (isReadableAsString(infoType) ? "String" : "JSON");
    if (returnType.equalsIgnoreCase("String"))
      return (info == null ? "" : info.toString());
    if (requestedReadable)
      return Escape.toReadable(infoType, info);
    if (returnType.equalsIgnoreCase("JSON"))
      return SV.safeJSON(infoType, info);
    return info;
  }

  private SV[] getArguments(String propertyName) {
    String lc = propertyName.toLowerCase();
    int pt = -1;
    if (propertyName.indexOf('"') >= 0 || propertyName.indexOf('\'') >= 0)
      propertyName = fixSelectQuotes(propertyName);
    while ((pt = lc.indexOf("[select ", ++pt)) >= 0) {
      int pt2 = lc.indexOf(" where ", pt);
      int pt2b = lc.indexOf(" wherein ", pt);
      if (pt2b > 0 && pt2b < pt2)
        pt2 = pt2b;
      int pt3 = lc.indexOf("][select ", pt);
      if (pt3 < 0)
        pt3 = lc.lastIndexOf("]");
      pt2b = lc.indexOf("[", pt);
      if (pt2b >= 0 && pt2b < pt3)
        pt2 = pt2b;
      if (pt2 < 0 || pt2 > pt3)
        continue;
      propertyName = propertyName.substring(0, pt + 1) 
          + propertyName.substring(pt + 1, pt3).replace('.', '\1').replace('[', '\2').replace(']', '\3')
          + propertyName.substring(pt3);
    }
    propertyName = PT.rep(PT.rep(propertyName.replace(']', '\0').replace('[', '\0'), "..", "\4")
        .replace('.', '\0').replace('\1', '.').replace('\2', '[').replace('\3', ']'),"\4","..");
    propertyName = PT.rep(propertyName, "\0\0", "\0");
    String[] names = PT.split(PT.trim(propertyName, "\0"), "\0");
    SV[] args = new SV[names.length];
    for (int i = 0, n; i < names.length; i++)
      args[i] = (names[i].startsWith("'") || names[i].startsWith("\"") 
          ? SV.newS(PT.trim(names[i], "'\""))
              : (n = PT.parseInt(names[i])) == Integer.MIN_VALUE ? 
                  SV.newS(names[i]) : SV.newI(n));
    return args;
  }

  private String fixSelectQuotes(String propertyName) {
    char[] a = propertyName.toCharArray();
    boolean inQuotes = false;
    boolean inQuotes1 = false;
    boolean inQuotes2 = false;
    for (int i = a.length; --i >= 0;) {
      switch (a[i]) {
      case '\'':
        if (!inQuotes2)
          inQuotes = inQuotes1 = !inQuotes;
        break;
      case '"':
        if (!inQuotes1)
          inQuotes = inQuotes2 = !inQuotes;
        break;
      case '.':
        if (inQuotes)
          a[i] = '\1';
        break;
      case '[':
        if (inQuotes)
          a[i] = '\2';
        break;
      case ']':
        if (inQuotes)
          a[i] = '\3';
        break;
        
      }
    }
    propertyName = new String(a);
    return propertyName;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object extractProperty(Object prop, Object args, int ptr,
                                Lst<Object> v2, boolean isCompiled) {
     if (ptr < 0) {
      args = getArguments((String) args);
      ptr = 0;
    }
    if (ptr >= ((SV[]) args).length)
      return prop;
    if (!isCompiled) {
      args = compileSelect((SV[]) args);
      SV[] svargs = (SV[]) args; 
      for (int i = ptr, n = svargs.length; i < n; i++) {
        if (svargs[i].tok == T.select) {
          SV[] a = new SV[i + 1];
          for (int p = 0; p <= i; p++) 
            a[p] = svargs[p];
          prop = extractProperty(prop, a, ptr, null, true);
          for (; ++i < n;) {
            a[a.length - 1] = svargs[i];
            prop = extractProperty(prop, a, a.length - 1, null, true);
          }
          return prop;                   
        }
      }
      args = svargs;
    }
    int pt;
    SV arg = ((SV[]) args)[ptr++];
    Object property = SV.oValue(prop);
    switch (arg.tok) {
    case T.integer:
      pt = arg.intValue - 1; //one-based, as for array selectors
      if (property instanceof Lst<?>) {
        Lst<Object> v = (Lst<Object>) property;
        if (pt < 0)
          pt += v.size();
        return (pt >= 0 && pt < v.size() ? extractProperty(v.get(pt), args,
            ptr, null, true) : "");
      }
      if (property instanceof M3) {
        M3 m = (M3) property;
        float[][] f = new float[][] { new float[] { m.m00, m.m01, m.m02 },
            new float[] { m.m10, m.m11, m.m12 },
            new float[] { m.m20, m.m21, m.m22 } };
        if (pt < 0)
          pt += 3;
        return (pt >= 0 && pt < 3 ? extractProperty(f, args, --ptr, null, true) : "");
      }
      if (property instanceof M4) {
        M4 m = (M4) property;
        float[][] f = new float[][] {
            new float[] { m.m00, m.m01, m.m02, m.m03 },
            new float[] { m.m10, m.m11, m.m12, m.m13 },
            new float[] { m.m20, m.m21, m.m22, m.m23 },
            new float[] { m.m30, m.m31, m.m32, m.m33 } };
        if (pt < 0)
          pt += 4;
        return (pt >= 0 && pt < 4 ? extractProperty(f, args, --ptr, null, true) : "");
      }
      if (AU.isAI(property)) {
        int[] ilist = (int[]) property;
        if (pt < 0)
          pt += ilist.length;
        return (pt >= 0 && pt < ilist.length ? Integer.valueOf(ilist[pt]) : "");
      }
      if (AU.isAD(property)) {
        double[] dlist = (double[]) property;
        if (pt < 0)
          pt += dlist.length;
        return (pt >= 0 && pt < dlist.length ? Double.valueOf(dlist[pt]) : "");
      }
      if (AU.isAF(property)) {
        float[] flist = (float[]) property;
        if (pt < 0)
          pt += flist.length;
        return (pt >= 0 && pt < flist.length ? Float.valueOf(flist[pt]): "");
      }
      if (AU.isAII(property)) {
        int[][] iilist = (int[][]) property;
        if (pt < 0)
          pt += iilist.length;
        return (pt >= 0 && pt < iilist.length ? extractProperty(iilist[pt], args, ptr, null, true): "");
      }
      if (AU.isAFF(property)) {
        float[][] fflist = (float[][]) property;
        if (pt < 0)
          pt += fflist.length;
        return (pt >= 0 && pt < fflist.length ? extractProperty(fflist[pt], args, ptr, null, true): "");
      }
      if (AU.isAS(property)) {
        String[] slist = (String[]) property;
        if (pt < 0)
          pt += slist.length;
        return (pt >= 0 && pt < slist.length ? slist[pt]: "");
      }
      if (property instanceof Object[]) {
        Object[] olist = (Object[]) property;
        if (pt < 0)
          pt += olist.length;
        return (pt >= 0 && pt < olist.length ? olist[pt]: "");
      }
      break;
    case T.select:
    case T.string:
      if (property instanceof Map<?, ?>) {
        Map<String, ?> h = (Map<String, ?>) property;
        String key;
        boolean asMap = false;
        boolean asArray = false;
        boolean isCaseSensitive = false;
        Lst<String> keys = (arg.tok == T.select ? (Lst<String>) ((Object[]) arg.value)[0]
            : null);
        T[] whereArgs = null;
        if (arg.tok == T.select) {
          isCaseSensitive = true;
          whereArgs = (T[]) ((Object[]) arg.value)[1];
          key = arg.myName;
          asArray = (key.indexOf(";") >= 0);
          if (key.contains("**")) {
            boolean isAll = keys.size() == 0;
            String newKey = "";
            for (Entry<?, ?> e : h.entrySet()) {
              String k = (String) e.getKey();
              for (int i = (isAll ? 1 : keys.size()); --i >= 0;) {
                if (!isAll && !PT.isLike(k, keys.get(i)))
                  continue;
                Object o = e.getValue();
                boolean isList = false;
                switch (o instanceof SV ? ((SV) o).tok : T.nada) {
                case T.varray:
                  isList = true;
                  o = ((SV) o).getList();
                  break;
                case T.hash:
                case T.context:
                  o = ((SV) o).getMap();
                  break;
                default:
                  if (!(o instanceof Map<?, ?>)
                      && !(isList = (o instanceof Lst<?>)))
                    o = null;
                }
                if (o != null) {
                  if (isList) {
                    if (v2 == null)
                      v2 = new Lst<Object>();
                    Lst<?> olst = (Lst<?>) o;
                    for (int n = olst.size(), j = 0; j < n; j++) {
                      o = olst.get(j);
                      if (!(o instanceof SV) || (o = ((SV) o).getMap()) == null)
                        continue;
                      if (whereArgs == null
                          || vwr.checkSelect((Map<String, SV>) o, whereArgs))
                        v2.addLast(o);
                    }
                    return v2;
                  }
                  if (whereArgs == null
                      || vwr.checkSelect((Map<String, SV>) o, whereArgs))
                    newKey += "," + k;
                }
              }
            }
            if (newKey.length() == 0)
              return new Lst<SV>();
            key = newKey.substring(1);
            asMap = !asArray;
            keys = null;
          } else if (whereArgs != null
              && !vwr.checkSelect((Map<String, SV>) property, whereArgs))
            return "";
        } else {
          key = arg.asString();
          if (key.equalsIgnoreCase("keys")) {
            Lst<Object> lst = new Lst<Object>();
            for (String k : h.keySet())
              lst.addLast(k);
            return extractProperty(lst, args, ptr, null, true);
          }
        }
        boolean havePunctuation = (asArray || key.indexOf(",") >= 0 || key
            .indexOf(";") >= 0);
        if (isCaseSensitive && !havePunctuation) {
          havePunctuation = true;
          key += ",";
        }
        boolean isWild = (asArray || key.startsWith("*") || key.endsWith("*") || havePunctuation);
        boolean wasV2 = (v2 != null);
        if (isWild) {
          if (!wasV2)
            v2 = new Lst<Object>();
          if (!asArray && (keys == null ? key.length() == 1 : keys.size() == 0)) {
            if (ptr == ((SV[]) args).length) {
              if (!wasV2)
                return property;
              v2.addLast(property);
              return v2;
            }
            return extractProperty(property, args, ptr, v2, true);
          }
        }
        if (key.contains("**"))
          key = PT.rep(key, "**", "*");
        if (isWild && !havePunctuation)
          key += ",";
        if (asMap || asArray || key.contains(",")) {
          Map<String, Object> mapNew = new Hashtable<String, Object>();
          if (keys != null && keys.size() == 0) {
            keys = null;
            key = "*";
          }
          asArray |= (arg.index == 1);
          if (keys == null) {
            String[] tokens = PT.split(key, ",");
            for (int i = tokens.length; --i >= 0;)
              getMapSubset(h, tokens[i], mapNew, asArray ? v2 : null);
          } else {
            for (int i = 0; i < keys.size(); i++)
              getMapSubset(h, keys.get(i), mapNew, asArray ? v2 : null);
          }
          if (asMap && !wasV2)
            return mapNew;
          if (ptr == ((SV[]) args).length) {
            if (!asArray) {
              if (!wasV2)
                return mapNew;
              v2.addLast(mapNew);
            }
            return v2;
          }
          return extractProperty(mapNew, args, ptr, (wasV2 ? v2 : null), true);
        }
        key = checkMap(h, key, isWild, v2, args, ptr, isCaseSensitive);
        return (key != null && !isWild ? extractProperty(h.get(key), args, ptr,
            null, true) : !isWild ? "" : wasV2 ? v2 : v2);
      }
      if (property instanceof Lst<?>) {
        // drill down into vectors for this key
        Lst<Object> v = (Lst<Object>) property;
        if (v2 == null)
          v2 = new Lst<Object>();
        ptr--;
        boolean isList = false;
        for (pt = 0; pt < v.size(); pt++) {
          Object o = v.get(pt);
          if (o instanceof Map<?, ?> || (isList = (o instanceof Lst<?>))
              || (o instanceof SV)
              && (((SV) o).getMap() != null || 
              (isList = (((SV) o).getList() != null)))
              ) {
            if (isList || (arg.index == 1)) {
              Object ret = extractProperty(o, args, ptr, null, true);
              if (ret != "")
                v2.addLast(ret);
            } else {
              extractProperty(o, args, ptr, v2, true);
            }
          }
        }
        return v2;
      }
      break;
    }
    return prop;
  }

  private static void getMapSubset(Map<String, ?> h, String key,
                                   Map<String, Object> h2, Lst<Object> v2) {
    if (key.startsWith("\"") || key.startsWith("'"))
      key = PT.trim(key,  "\"'");
    Object val = h.get(key);
    if (val != null) {
      if (v2 == null)
        h2.put(key, val);
      else
        v2.addLast(val);
      return;
    }
    for (Entry<String, ?> e : h.entrySet()) {
      String k = e.getKey();
      if (PT.isLike(k, key)) {
        if (v2 == null)
          h2.put(k, e.getValue());
        else
          v2.addLast(e.getValue());
      }
    }
  }

  private SV[] compileSelect(SV[] args) {
    SV[] argsNew = null;
    for (int i = args.length; --i >= 0;) {
      if (args[i].tok == T.string) {
        String key = (String) args[i].value;
        // SELECT nt* WHERE name!=WC
        // SELECT a,b,c WHERE x.in(...)

        String ucKey = key.toUpperCase();
        if (ucKey.startsWith("WHERE"))
          ucKey = (key = "SELECT * " + key).toUpperCase(); 
        if (ucKey.startsWith("SELECT ")) {
          if (argsNew == null)
            argsNew = (SV[]) AU.arrayCopyObject(args, args.length);
          ucKey = (key = key.substring(6).trim()).toUpperCase();
          if (ucKey.startsWith("WHERE ") || ucKey.startsWith("WHEREIN "))
            ucKey = (key = "* " + key).toUpperCase();
          int pt = ucKey.indexOf(" WHEREIN ");
          String ext = (pt < 0 ? "" : key.indexOf(";") >= 0 ? ";**" : ",**");
          if (pt < 0)
            pt = ucKey.indexOf(" WHERE ");
          String select = key.substring(0, pt < 0 ? key.length() : pt).trim();
          int index = 0;
          if (select.startsWith("(") && select.endsWith(")")) {
            select = select.substring(1, select.length() - 1) + ";";
          } else if (select.startsWith("[") && select.endsWith("]")) {
            select = select.substring(1, select.length() - 1);
            index = 1;
          }
          if (pt < 0) {
            argsNew[i] = SV.newV(T.select, new Object[] { getKeys(select), null });
            argsNew[i].myName = select;
            argsNew[i].index = index;
          } else {
            // allow for (A*,B) to be same as A*;B
            select += ext;
            argsNew[i] = SV.newV(
                T.select,
                new Object[] { getKeys(select),
                    vwr.compileExpr(key.substring(pt + 6 + ext.length()).trim()) });
            argsNew[i].index = index;
            argsNew[i].myName = select;
          }
        }
      }
    }
    return (argsNew == null ? args : argsNew);
  }

  private Lst<String> getKeys(String select) {
    Lst<String> keys = new Lst<String>();
    select = PT.rep(PT.rep(select, "**", "*"), ";",",") + ",";
    int pt0 = 0, pt1 = -1;
    while ((pt1 = select.indexOf(",", pt1 + 1)) >= 0) {
      if (pt1 > pt0) {
        String key = select.substring(pt0, pt1);
        if (key.equals("*")) {
          // skip "*" if at end; it may  just indicated **
          if (keys.size() == 0)
            return keys;
          continue;
        }
        keys.addLast(key);
        pt0 = pt1 + 1;
      }
    }
    return keys;
  }

  private String checkMap(Map<String, ?> h, String key, boolean isWild,
                          Lst<Object> v2, Object args, int ptr, boolean isCaseSensitive) {
    boolean isOK = (v2 == null && h.containsKey(key));
    if (!isOK) {
      boolean hasSemi = key.contains(";");
      String[] keys = (hasSemi ? PT.split(key, ";") : null);
      String lckey = (isWild && !isCaseSensitive ? key.toLowerCase() : null);
      for (String k : h.keySet()) {
        if (hasSemi) {
          for (int i = keys.length; --i >= 0; key = null) {
            key = keys[i];
            if (key.length() == 0)
              continue;
            if (isCaseSensitive) {
              if (!PT.isLike(k, key))
                continue;
              break;
            }
            lckey = (key.indexOf("*") >= 0 ? key.toLowerCase() : null);
            if (checkKey(k, key, lckey))
              break;
          }
          if (key == null)
            continue;
        } else if (isCaseSensitive ? !PT.isLike(k,  key) : !checkKey(k, key, lckey))
          continue;
        if (v2 == null)
          return k;
        v2.addLast(extractProperty(h.get(k), args, ptr, null, true));
        if (!isWild && !hasSemi)
          return null;
      }
    }
    return (isOK ? key : null);
  }

  private boolean checkKey(String k, String key, String lckey) {
    return k.equalsIgnoreCase(key) ||
        lckey != null && PT.isLike(k.toLowerCase(), lckey);
  }

  private static String getPropertyName(int propID) {
    return (propID < 0 ? "" : propertyTypes[propID * 3]);
  }

  private static String getParamType(int propID) {
    return (propID < 0 ? "" : propertyTypes[propID * 3 + 1]);
  }

  private final static String[] readableTypes = { "", "stateinfo",
      "extractmodel", "filecontents", "fileheader", "image", "menu",
      "minimizationInfo" };

  private static boolean isReadableAsString(String infoType) {
    for (int i = readableTypes.length; --i >= 0;)
      if (infoType.equalsIgnoreCase(readableTypes[i]))
        return true;
    return false;
  }

  private Object getPropertyAsObject(String infoType, Object paramInfo,
                                     String returnType) {
    //Logger.debug("getPropertyAsObject(\"" + infoType+"\", \"" + paramInfo + "\")");
    if (infoType.equals("tokenList")) {
      return T.getTokensLike((String) paramInfo);
    }
    Object myParam = null;
    int pt = infoType.indexOf("#");
    if (pt > 0) {
      myParam = new Object[] { infoType.substring(pt + 1), paramInfo };
      infoType = infoType.substring(0, pt);      
    }
    int id = getPropertyNumber(infoType);
    boolean iHaveParameter = (myParam != null || paramInfo != null && paramInfo != "");
    if (myParam == null)
      myParam = (iHaveParameter ? paramInfo: getDefaultPropertyParam(id));
    switch (id) {
    case PROP_MODELKIT_INFO:
      return vwr.getModelkitProperty(myParam);
    case PROP_APPLET_INFO:
      return getAppletInfo();
    case PROP_ANIMATION_INFO:
      return getAnimationInfo();
    case PROP_ATOM_INFO:
      return getAllAtomInfo(vwr.getAtomBitSet(myParam));
    case PROP_AUXILIARY_INFO:
      return vwr.getAuxiliaryInfoForAtoms(myParam);
    case PROP_BOND_INFO:
      return getAllBondInfo(myParam);
    case PROP_BOUNDBOX_INFO:
      return getBoundBoxInfo();
    case PROP_CENTER_INFO:
      return vwr.tm.fixedRotationCenter;
    case PROP_CHAIN_INFO:
      return getAllChainInfo(vwr.getAtomBitSet(myParam));
    case PROP_CONSOLE_TEXT:
      return vwr.getProperty("DATA_API", "consoleText", null);
    case PROP_DATA_INFO:
      return vwr.getDataObj(myParam.toString(), null, JmolDataManager.DATA_TYPE_UNKNOWN);
    case PROP_ERROR_MESSAGE:
      return vwr.getErrorMessageUn();
    case PROP_EVALUATE:
      return vwr.evaluateExpression(myParam.toString());
    case PROP_EXTRACT_MODEL:
      return vwr.getModelExtract(myParam, true, false, "MOL");
    case PROP_FILE_INFO:
      return getFileInfo(vwr.getFileData(), myParam.toString());
    case PROP_CIF_INFO:
      return vwr.readCifData(myParam.toString(), null);
    case PROP_FILENAME:
      return vwr.fm.getFullPathName(false);
    case PROP_FILEHEADER:
      return vwr.getFileHeader();
    case PROP_FILECONTENTS:
    case PROP_FILECONTENTS_PATH:
      return (iHaveParameter ? vwr.getFileAsString3(myParam.toString(), true, null)
          : vwr.getCurrentFileAsString("prop"));
    case PROP_IMAGE:
      String params = myParam.toString().toLowerCase();
      return getImage(params,
          params.indexOf("g64") < 0 && params.indexOf("base64") < 0
              && (returnType == null || returnType.equalsIgnoreCase("java")));
    case PROP_ISOSURFACE_INFO:
      return vwr.getShapeProperty(JC.SHAPE_ISOSURFACE, "info");
    case PROP_ISOSURFACE_DATA:
      return vwr.getShapeProperty(JC.SHAPE_ISOSURFACE, "data");
    case PROP_NMR_INFO:
      return vwr.getNMRCalculation().getInfo(myParam.toString());
    case PROP_VAR_INFO:
      return getVariables(myParam.toString());
    case PROP_JMOL_STATUS:
      return vwr.getStatusChanged(myParam.toString());
    case PROP_JMOL_VIEWER:
      return vwr;
    case PROP_JSPECVIEW:
      return vwr.getJspecViewProperties(myParam);
    case PROP_LIGAND_INFO:
      return getLigandInfo(vwr.getAtomBitSet(myParam));
    case PROP_MEASUREMENT_INFO:
      return getMeasurementInfo();
    case PROP_MENU:
      return vwr.getMenu(myParam.toString());
    case PROP_MESSAGE_QUEUE:
      return vwr.sm.messageQueue;
    case PROP_MINIMIZATION_INFO:
      return vwr.getMinimizationInfo();
    case PROP_MODEL_INFO:
      return getModelInfo(vwr.getAtomBitSet(myParam));
    case PROP_MOLECULE_INFO:
      return getMoleculeInfo(vwr.getAtomBitSet(myParam));
    case PROP_MOUSE_INFO:
      return getMouseInfo();
    case PROP_ORIENTATION_INFO:
      return vwr.tm.getOrientationInfo();
    case PROP_POINTGROUP_INFO:
      return vwr.ms.getPointGroupInfo(vwr.getAtomBitSet(myParam));
    case PROP_POLYMER_INFO:
      return getAllPolymerInfo(vwr.getAtomBitSet(myParam));
    case PROP_SCRIPT_QUEUE_INFO:
      return vwr.getScriptQueueInfo();
    case PROP_SHAPE_INFO:
      return getShapeInfo();
    case PROP_STATE_INFO:
      return vwr.getStateInfo3(myParam.toString(), 0, 0);
    case PROP_TRANSFORM_INFO:
      return M3.newM3(vwr.tm.matrixRotate);
    case PROP_DOM_INFO:
      return getAnnotationInfo(myParam, T.domains);
    case PROP_VAL_INFO:
      return getAnnotationInfo(myParam, T.validation);
    case PROP_SERVICE:
      myParam = SV.oValue(myParam);
      @SuppressWarnings("unchecked")
      Map<String, Object> info = (myParam instanceof Map<?,?> ? (Map<String, Object>) myParam : null);
      return (info == null ? null : vwr.sm.processService(info));
    }
    String[] data = new String[PROP_COUNT];
    for (int i = 0; i < PROP_COUNT; i++) {
      String paramType = getParamType(i);
      String paramDefault = getDefaultPropertyParam(i);
      String name = getPropertyName(i);
      data[i] = (name.length() ==  0 || name.charAt(0) == 'X' ? "" : name
          + (paramType != "" ? " "
              + getParamType(i)
              + (paramDefault != "" ? " #default: "
                  + getDefaultPropertyParam(i) : "") : ""));
    }
    Arrays.sort(data);
    SB info = new SB();
    info.append("getProperty ERROR\n").append(infoType)
        .append("?\nOptions include:\n");
    for (int i = 0; i < PROP_COUNT; i++)
      if (data[i].length() > 0)
        info.append("\n getProperty ").append(data[i]);
    return info.toString();
  }

  private Object getImage(String params, boolean asBytes) {
    int height = -1,
    width = -1;
    int pt;
    if ((pt = params.indexOf("height=")) >= 0)
      height = PT.parseInt(params.substring(pt + 7));
    if ((pt = params.indexOf("width=")) >= 0)
      width = PT.parseInt(params.substring(pt + 6));
    if (width < 0 && height < 0)
      height = width = -1;
    else if (width < 0)
      width = height;
    else
      height = width;
    String type = "JPG";
    if (params.indexOf("type=") >= 0)
      type = PT.getTokens(PT.replaceWithCharacter(params.substring(params.indexOf("type=") + 5), ";,", ' '))[0];
    String[] errMsg = new String[1];
    byte[] bytes = vwr.getImageAsBytes(type.toUpperCase(), width,  height, -1, errMsg);
    return (errMsg[0] != null ? errMsg[0] : asBytes ? new BArray(bytes) : Base64
        .getBase64(bytes).toString());
  }

  private Object getVariables(String name) {
    return (name.toLowerCase().equals("all") ? vwr.g.getAllVariables()
        : vwr.evaluateExpressionAsVariable(name));
  }

  static Object getFileInfo(Object objHeader, String type) {
    Map<String, String> ht = new Hashtable<String, String>();
    if (objHeader == null)
      return ht;
    boolean haveType = (type != null && type.length() > 0);
    if (objHeader instanceof Map) {
      return (haveType ? ((Map<?, ?>) objHeader).get(type) : objHeader);
    }
    String[] lines = PT.split((String) objHeader, "\n");
    // this is meant to be for PDB files only
    // will be "[zapped]" if there is no file loaded
    if (lines.length == 0
        || lines[0].length() < 7
        || lines[0].charAt(6) != ' '
        || !lines[0].substring(0, 6).equals(
            lines[0].substring(0, 6).toUpperCase())) {
      ht.put("fileHeader", (String) objHeader);
      return ht;
    }
    String keyLast = "";
    SB sb = new SB();
    if (haveType)
      type = type.toUpperCase();
    String key = "";
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      if (line.length() < 12)
        continue;
      key = line.substring(0, 6).trim();
      String cont = line.substring(7, 10).trim();
      if (key.equals("REMARK")) {
        key += cont;
      }
      if (!key.equals(keyLast)) {
        if (haveType && keyLast.equals(type))
          return sb.toString();
        if (!haveType) {
          ht.put(keyLast, sb.toString());
          sb = new SB();
        }
        keyLast = key;
      }
      if (!haveType || key.equals(type))
        sb.append(line).appendC('\n');
    }
    if (!haveType) {
      ht.put(keyLast, sb.toString());
    }
    if (haveType)
      return (key.equals(type) ? sb.toString() : "");
    return ht;
  }

  /// info ///

  public Lst<Map<String, Object>> getMoleculeInfo(Object atomExpression) {
    BS bsAtoms = vwr.getAtomBitSet(atomExpression);
    JmolMolecule[] molecules = vwr.ms.getMolecules();
    Lst<Map<String, Object>> V = new  Lst<Map<String, Object>>();
    BS bsTemp = new BS();
    for (int i = 0; i < molecules.length; i++) {
      bsTemp = BSUtil.copy(bsAtoms);
      JmolMolecule m = molecules[i];
      bsTemp.and(m.atomList);
      if (bsTemp.length() > 0) {
        Map<String, Object> info = new Hashtable<String, Object>();
        info.put("mf", m.getMolecularFormula(true, null, false)); // sets ac and nElements
        info.put("number", Integer.valueOf(m.moleculeIndex + 1)); //for now
        info.put("modelNumber", vwr.ms.getModelNumberDotted(m.modelIndex));
        info.put("numberInModel", Integer.valueOf(m.indexInModel + 1));
        info.put("nAtoms", Integer.valueOf(m.ac));
        info.put("nElements", Integer.valueOf(m.nElements));
        V.addLast(info);
      }
    }
    return V;
  }

  @Override
  public Map<String, Object> getModelInfo(Object atomExpression) {

    BS bsModels = vwr.ms.getModelBS(vwr
        .getAtomBitSet(atomExpression), false);

    ModelSet m = vwr.ms;
    Map<String, Object> info = new Hashtable<String, Object>();
    info.put("modelSetName", m.modelSetName);
    info.put("modelIndex", Integer.valueOf(vwr.am.cmi));
    info.put("modelCount", Integer.valueOf(m.mc));
    info.put("isTainted", Boolean.valueOf(m.tainted != null));
    info.put("canSkipLoad", Boolean.valueOf(m.canSkipLoad));
    info.put("modelSetHasVibrationVectors", Boolean.valueOf(m
        .modelSetHasVibrationVectors()));
    if (m.modelSetProperties != null) {
      info.put("modelSetProperties", m.modelSetProperties);
    }
    info.put("modelCountSelected", Integer.valueOf(bsModels.cardinality()));
    info.put("modelsSelected", bsModels);
    Lst<Map<String, Object>> vModels = new  Lst<Map<String, Object>>();
    m.getMolecules();

    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1)) {
      Map<String, Object> model = new Hashtable<String, Object>();
      model.put("_ipt", Integer.valueOf(i));
      model.put("num", Integer.valueOf(m.getModelNumber(i)));
      model.put("file_model", m.getModelNumberDotted(i));
      model.put("name", m.getModelName(i));
      String s = m.getModelTitle(i);
      if (s != null)
        model.put("title", s);
      s = m.getModelFileName(i);
      if (s != null)
        model.put("file", s);
      s = (String) m.getInfo(i, "modelID");
      if (s != null)
        model.put("id", s);
      model.put("vibrationVectors", Boolean.valueOf(vwr.modelHasVibrationVectors(i)));
      Model mi = m.am[i];
      model.put("atomCount", Integer.valueOf(mi.act));
      model.put("bondCount", Integer.valueOf(mi.getBondCount()));
      model.put("groupCount", Integer.valueOf(mi.getGroupCount()));
      model.put("moleculeCount", Integer.valueOf(mi.moleculeCount));
      if (mi.isBioModel)
        model.put("polymerCount", Integer.valueOf(((BioModel) mi).getBioPolymerCount()));
      model.put("chainCount", Integer.valueOf(m.getChainCountInModelWater(i, true)));
      if (mi.properties != null) {
        model.put("modelProperties", mi.properties);
      }
      Float energy = (Float) m.getInfo(i, "Energy");
      if (energy != null) {
        model.put("energy", energy);
      }
      model.put("atomCount", Integer.valueOf(mi.act));
      vModels.addLast(model);
    }
    info.put("models", vModels);
    return info;
  }

  @Override
  public Map<String, Object> getLigandInfo(Object atomExpression) {
    BS bsAtoms = vwr.getAtomBitSet(atomExpression);
    BS bsSolvent = vwr.getAtomBitSet("solvent");
    Map<String, Object> info = new Hashtable<String, Object>();
    Lst<Map<String, Object>> ligands = new  Lst<Map<String, Object>>();
    info.put("ligands", ligands);
    ModelSet ms = vwr.ms;
    BS bsExclude = BSUtil.copyInvert(bsAtoms, ms.ac);
    bsExclude.or(bsSolvent);
    Atom[] atoms = ms.at;
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1))
      if (atoms[i].group.isProtein() || atoms[i].group.isDna() || atoms[i].group.isRna())
        atoms[i].group.setAtomBitsAndClear(bsExclude, bsAtoms);
    BS[] bsModelAtoms = new BS[ms.mc];
    for (int i = ms.mc; --i >= 0;) {
      bsModelAtoms[i] = vwr.getModelUndeletedAtomsBitSet(i);
      bsModelAtoms[i].andNot(bsExclude);
    }
    JmolMolecule[] molList = JmolMolecule.getMolecules(atoms, bsModelAtoms,
        null, bsExclude);
    for (int i = 0; i < molList.length; i++) {
      BS bs = molList[i].atomList;
      Map<String, Object> ligand = new Hashtable<String, Object>();
      ligands.addLast(ligand);
      ligand.put("atoms", Escape.eBS(bs));
      String names = "";
      String sep = "";
      Group lastGroup = null;
      int iChainLast = 0;
      String sChainLast = null;
      String reslist = "";
      String model = "";
      int resnolast = Integer.MAX_VALUE;
      int resnofirst = Integer.MAX_VALUE;
      for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1)) {
        Atom atom = atoms[j];
        if (lastGroup == atom.group)
          continue;
        lastGroup = atom.group;
        int resno = atom.getResno();
        int chain = atom.getChainID();
        if (resnolast != resno - 1) {
          if (reslist.length() != 0 && resnolast != resnofirst)
            reslist += "-" + resnolast;
          chain = -1;
          resnofirst = resno;
        }
        model = "/" + ms.getModelNumberDotted(atom.mi);
        if (iChainLast != 0 && chain != iChainLast)
          reslist += ":" + sChainLast + model;
        if (chain == -1)
          reslist += " " + resno;
        resnolast = resno;
        iChainLast = atom.getChainID();
        sChainLast = atom.getChainIDStr();
        names += sep + atom.getGroup3(false);
        sep = "-";
      }
      reslist += (resnofirst == resnolast ? "" : "-" + resnolast)
          + (iChainLast == 0 ? "" : ":" + sChainLast) + model;
      ligand.put("groupNames", names);
      ligand.put("residueList", reslist.substring(1));
    }
    return info;
  }

  /**
   * use lower case to indicate coord data only (xyz, xyzrn, xyzvib, pdb. use
   * USER: or PROPERTY_xxxx for a property
   * 
   * all other types return full data
   */
  @Override
  public String getAtomData(String atomExpression, String type, boolean allTrajectories) {
    // lower case is only used in Jmol script function print data({*}, "datatype")
    if (!atomExpression.startsWith("{"))
      atomExpression = "{" + atomExpression + "}";
    boolean isUser = type.toLowerCase().startsWith("user:");
    boolean isProp = type.toLowerCase().startsWith("property_");
    if (allTrajectories && !isUser && !isProp)
      type = type.toUpperCase();
    String exp = (isProp ? "%{" + type + "}"
        : isUser ? type.substring(5)
            : type.equals("xyzrn") ? "%-2e %8.3x %8.3y %8.3z %4.2[vdw] 1 [%n]%r.%a#%i"
                : type.equals("xyzvib") ? "%-2e %10.5x %10.5y %10.5z %10.5vx %10.5vy %10.5vz"
                    : type.equals("pdb") ? "{selected and not hetero}.label(\"ATOM  %5i %-4a%1A%3.3n %1c%4R%1E   %8.3x%8.3y%8.3z%6.2Q%6.2b          %2e  \").lines"
                        + "+{selected and hetero}.label(\"HETATM%5i %-4a%1A%3.3n %1c%4R%1E   %8.3x%8.3y%8.3z%6.2Q%6.2b          %2e  \").lines"
                        : type.equals("xyz") ? "%-2e %10.5x %10.5y %10.5z"
                            : type.equals("cfi") ? "print '$CFI from Jmol" + Viewer.getJmolVersion() + "\n'+{selected}.count + ' ' + {selected}.bonds.count + '\n'   + {selected}.format('%10.0[atomno] %10.0[elemno] %10.4[xyz]')  + {selected}.bonds.format('%i1 %i2') + '\n' + {selected}.bonds.format('%ORDER')"
                            : null);
    if (exp == null)
      return getModelExtract(vwr.getAtomBitSet(atomExpression), false, false,
          type.toUpperCase(), allTrajectories);
    if (exp.startsWith("print")) {
      if (!atomExpression.equals("selected"))
        exp = PT.rep(exp, "selected", atomExpression.substring(1, atomExpression.length() - 1));
      return vwr.runScriptCautiously(exp);
    }
    if (exp.indexOf("label") < 0)
      exp = atomExpression + ".label(\"" + exp + "\").lines";
    else if (!atomExpression.equals("selected"))
      exp = PT.rep(exp, "selected", atomExpression.substring(1, atomExpression.length() - 1));
    return (String) vwr.evaluateExpression(exp);
  }


  /**
   * 
   * V3000, SDF, MOL, JSON, CD, XYZ, XYZVIB, XYZRN, CML, PDB, PQR, QCJSON
   * 
   * MOL67 is MOL with bonds of type 6 or 7 (aromatic single/double)
   * 
   */
  @Override
  public String getModelExtract(BS bs, boolean doTransform, boolean isModelKit,
                                String type, boolean allTrajectories) {
    
    if (type.equalsIgnoreCase("CIF"))
      return getModelCif(bs);
    if (type.equalsIgnoreCase("QCJSON"))
      return getQCJSON(bs);
    if (type.equalsIgnoreCase("CML"))
      return getModelCml(bs, Integer.MAX_VALUE, true, doTransform, allTrajectories);
    if (type.equals("PDB") || type.equals("PQR"))
      return getPdbAtomData(bs, null, type.equals("PQR"), doTransform, allTrajectories);
    boolean asV3000 = type.equalsIgnoreCase("V3000");
    boolean asSDF = type.equalsIgnoreCase("SDF");
    boolean noAromatic = type.equalsIgnoreCase("MOL");
    boolean asXYZVIB = (!doTransform && type.equalsIgnoreCase("XYZVIB"));
    boolean asXYZRN = type.equalsIgnoreCase("XYZRN");
    boolean isXYZ = type.toUpperCase().startsWith("XYZ");
    boolean asJSON = type.equalsIgnoreCase("JSON") || type.equalsIgnoreCase("CD");
    SB mol = new SB();
    ModelSet ms = vwr.ms;
    BS bsAtoms = BSUtil.copy(bs);
    BS bsModels = vwr.ms.getModelBS(bsAtoms, true);
    if (!isXYZ && !asJSON) {
      String title = vwr.ms.getFrameTitle(bsModels.nextSetBit(0));      
      title = (title != null ? title.replace('\n',' ') : isModelKit ? "Jmol Model Kit" : FileManager.fixDOSName(vwr.fm.getFullPathName(false)));
      if (title.length() > 80)
        title = title.substring(0, 77) + "...";
      mol.append(title);
      String version = Viewer.getJmolVersion();
      mol.append("\n__Jmol-").append(version.substring(0, 2));
      int cMM, cDD, cYYYY, cHH, cmm;
      /**
       * @j2sNative
       * 
       *            var c = new Date(); cMM = c.getMonth(); cDD = c.getDate();
       *            cYYYY = c.getFullYear(); cHH = c.getHours(); cmm =
       *            c.getMinutes();
       */
      {
        Calendar c = Calendar.getInstance();
        cMM = c.get(Calendar.MONTH);
        cDD = c.get(Calendar.DAY_OF_MONTH);
        cYYYY = c.get(Calendar.YEAR);
        cHH = c.get(Calendar.HOUR_OF_DAY);
        cmm = c.get(Calendar.MINUTE);
      }
      PT.rightJustify(mol, "_00", "" + (1 + cMM));
      PT.rightJustify(mol, "00", "" + cDD);
      mol.append(("" + cYYYY).substring(2, 4));
      PT.rightJustify(mol, "00", "" + cHH);
      PT.rightJustify(mol, "00", "" + cmm);
      mol.append("3D 1   1.00000     0.00000     0");
      //       This line has the format:
      //  IIPPPPPPPPMMDDYYHHmmddSSssssssssssEEEEEEEEEEEERRRRRR
      //  A2<--A8--><---A10-->A2I2<--F10.5-><---F12.5--><-I6->
      mol.append("\nJmol version ").append(Viewer.getJmolVersion())
          .append(" EXTRACT: ").append(Escape.eBS(bs)).append("\n");
    }
    Atom[] atoms = ms.at;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
      if (doTransform && atoms[i].isDeleted())
        bsAtoms.clear(i);
    BS bsBonds = getCovalentBondsForAtoms(ms.bo, ms.bondCount, bsAtoms);
    if (!asXYZVIB && bsAtoms.isEmpty())
      return "";
    boolean isOK = true;
    if (ms.trajectory != null && !allTrajectories)
      ms.trajectory.selectDisplayed(bsModels);
    Quat q = (doTransform ? vwr.tm.getRotationQ() : null);
    if (isXYZ) {
      LabelToken[] tokensXYZ = LabelToken.compile(vwr,
          (asXYZRN ? "%-2e _XYZ_ %4.2[vdw] 1 [%n]%r.%a#%i\n" : "%-2e _XYZ_\n"),
          '\0', null);
      LabelToken[] tokensVib = (asXYZVIB ? LabelToken.compile(vwr,
          "%-2e _XYZ_ %12.5vx %12.5vy %12.5vz\n", '\0', null) : null);
      P3 ptTemp = new P3();
      for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels
          .nextSetBit(i + 1)) {
        BS bsTemp = BSUtil.copy(bsAtoms);
        bsTemp.and(ms.getModelAtomBitSetIncludingDeleted(i, false));
        if (bsTemp.isEmpty())
          continue;
        mol.appendI(bsTemp.cardinality()).appendC('\n');
        Properties props = ms.am[i].properties;
        mol.append("Model[" + (i + 1) + "]: ");
        if (ms.frameTitles[i] != null && ms.frameTitles[i].length() > 0) {
          mol.append(ms.frameTitles[i].replace('\n', ' '));
        } else if (props == null) {
          mol.append("Jmol " + Viewer.getJmolVersion());
        } else {
          SB sb = new SB();
          Enumeration<?> e = props.propertyNames();
          String path = null;
          while (e.hasMoreElements()) {
            String propertyName = (String) e.nextElement();
            if (propertyName.equals(".PATH"))
              path = props.getProperty(propertyName);
            else
              sb.append(";").append(propertyName).append("=")
                  .append(props.getProperty(propertyName));
          }
          if (path != null)
            sb.append(";PATH=").append(path);
          path = sb.substring(sb.length() > 0 ? 1 : 0);
          mol.append(path.replace('\n', ' '));
        }
        mol.appendC('\n');
        Object[] o = new Object[] { ptTemp };
        for (int j = bsTemp.nextSetBit(0); j >= 0; j = bsTemp.nextSetBit(j + 1)) {
          String s = LabelToken.formatLabelAtomArray(vwr, atoms[j], (asXYZVIB
              && ms.getVibration(j, false) != null ? tokensVib : tokensXYZ),
              '\0', null, ptTemp);
          getPointTransf(i, ms, atoms[j], q, ptTemp);
          s = PT.rep(s, "_XYZ_", PT.sprintf("%12.5p %12.5p %12.5p", "p", o));
          mol.append(s);
        }
      }
    } else {
      MolWriter mw = ((MolWriter) Interface.getInterface("org.jmol.util.MolWriter", vwr, "write")).setViewer(vwr);
      if (asSDF) {
        String header = mol.toString();
        mol = new SB();
        for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels
            .nextSetBit(i + 1)) {
          mol.append(header);
          BS bsTemp = BSUtil.copy(bsAtoms);
          bsTemp.and(ms.getModelAtomBitSetIncludingDeleted(i, false));
          bsBonds = getCovalentBondsForAtoms(ms.bo, ms.bondCount, bsTemp);
          if (!(isOK = mw.addMolFile(i, mol, bsTemp, bsBonds, false, false, noAromatic, q)))
            break;
        }
      } else {
        isOK = mw.addMolFile(-1, mol, bsAtoms, bsBonds, asV3000, asJSON, noAromatic, q);
      }
    } 
    return (isOK ? mol.toString()
        : "ERROR: Too many atoms or bonds -- use V3000 format.");
  }

  /**
   * 
   * EXPERIMENTAL
   * 
   * create a QCJSON file -- very preliminary
   * 
   * @param bs  ignored
   * @return JSON string
   */
  private String getQCJSON(BS bs) {
    // bs is ignored here
    QCJSONWriter writer = (QCJSONWriter) Interface.getInterface("org.jmol.adapter.writers.QCJSONWriter", vwr, "script");
    writer.set(vwr, null);
    writer.writeJSON();
    return writer.toString();
  }

  /**
   * just a very primitive CIF file reader
   * @param bs
   * @return CIf data
   */
  private String getModelCif(BS bs) {    
    SB sb = new SB();
    sb.append("# primitive CIF file created by Jmol " + Viewer.getJmolVersion() + "\ndata_primitive");
    SymmetryInterface unitcell = vwr.ms.getUnitCellForAtom(bs.nextSetBit(0));
    float[] params = (unitcell == null ? new float[] {1,1,1,90,90,90} : unitcell.getUnitCellAsArray(false));
    sb.append("\n_cell_length_a ").appendF(params[0]);
    sb.append("\n_cell_length_b ").appendF(params[1]);
    sb.append("\n_cell_length_c ").appendF(params[2]);
    sb.append("\n_cell_angle_alpha ").appendF(params[3]);
    sb.append("\n_cell_angle_beta ").appendF(params[4]);
    sb.append("\n_cell_angle_gamma ").appendF(params[5]);
    sb.append("\n\n_symmetry_space_group_name_H-M 'P 1'\nloop_\n_space_group_symop_operation_xyz\n'x,y,z'");
    sb.append("\n\nloop_\n_atom_site_label\n_atom_site_fract_x\n_atom_site_fract_y\n_atom_site_fract_z\n");
    Atom[] atoms = vwr.ms.at;
    P3 p = new P3();
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      Atom a = atoms[i];
      p.setT(a);
      if (unitcell != null)
      unitcell.toFractional(p, false);
      sb.append(a.getElementSymbol()).append("\t")
      .append(PT.formatF(p.x, 10, 5, true, false)).append("\t")
      .append(PT.formatF(p.y, 10, 5, true, false)).append("\t")
      .append(PT.formatF(p.z, 10, 5, true, false)).append("\n");
    }
     return sb.toString();
  }

  private static BS getCovalentBondsForAtoms(Bond[] bonds, int bondCount, BS bsAtoms) {
    BS bsBonds = new BS();
    for (int i = 0; i < bondCount; i++) {
      Bond bond = bonds[i];
      if (bsAtoms.get(bond.atom1.i) && bsAtoms.get(bond.atom2.i)
          && bond.isCovalent())
        bsBonds.set(i);
    }
    return bsBonds;
  }

  /**
   * pick up the appropriate value for this atom
   * @param i 
   * 
   * @param ms
   * @param a
   * @param q
   * @param pTemp
   */
  public static void getPointTransf(int i, ModelSet ms, Atom a, Quat q, P3 pTemp) {
    if (ms.isTrajectory(i >= 0 ? i : a.mi))
      ms.trajectory.getFractional(a, pTemp);
    else
      pTemp.setT(a);
    if (q != null)
      q.transform2(pTemp, pTemp);
  }

  @Override
  public String getChimeInfo(int tok, BS bs) {
    switch (tok) {
    case T.info:
      break;
    case T.basepair:
      return getBasePairInfo(bs);
    default:
      return getChimeInfoA(vwr.ms.at, tok, bs);
    }
    SB sb = new SB();
    vwr.getChimeMessenger().getAllChimeInfo(sb);
    return sb.appendC('\n').toString().substring(1);
  }

  private String getChimeInfoA(Atom[] atoms, int tok, BS bs) {
    SB info = new SB();
    info.append("\n");
    String s = "";
    Chain clast = null;
    Group glast = null;
    int modelLast = -1;
    int n = 0;
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        Atom a = atoms[i];
        switch (tok) {
        default:
          return "";
        case T.selected:
          s = a.getInfo();
          break;
        case T.atoms:
          s = "" + a.getAtomNumber();
          break;
        case T.group:
          s = a.getGroup3(false);
          break;
        case T.chain:
        case T.residue:
        case T.sequence:
        case T.group1:
          int id = a.getChainID();
          s = (id == 0 ? " " : a.getChainIDStr());
          if (id >= 300)
            s = PT.esc(s);
          switch (tok) {
          case T.residue:
            s = "[" + a.getGroup3(false) + "]" + a.group.getSeqcodeString() + ":" + s;
            break;
          case T.sequence:
          case T.group1:
            if (a.mi != modelLast) {
              info.appendC('\n');
              n = 0;
              modelLast = a.mi;
              info.append("Model " + a.getModelNumber());
              glast = null;
              clast = null;
            }
            if (a.group.chain != clast) {
              info.appendC('\n');
              n = 0;
              clast = a.group.chain;
              info.append("Chain " + s + ":\n");
              glast = null;
            }
            Group g = a.group;
            if (g != glast) {
              glast = g;
              if (tok == T.group1) {
                info.append(a.getGroup1('?'));
              } else {
                if ((n++) % 5 == 0 && n > 1)
                  info.appendC('\n');
                PT.leftJustify(info, "          ", "[" + a.getGroup3(false)
                    + "]" + a.getResno() + " ");
              }
            }
            continue;
          }
          break;
        }
        if (info.indexOf("\n" + s + "\n") < 0)
          info.append(s).appendC('\n');
      }
    if (tok == T.sequence)
      info.appendC('\n');
    return info.toString().substring(1);
  }

  @Override
  public String getModelFileInfo(BS frames) {
    ModelSet ms = vwr.ms;
    SB sb = new SB();
    for (int i = 0; i < ms.mc; ++i) {
      if (frames != null && !frames.get(i))
        continue;
      String s = "[\"" + ms.getModelNumberDotted(i) + "\"] = ";
      sb.append("\n\nfile").append(s).append(PT.esc(ms.getModelFileName(i)));
      String id = (String) ms.getInfo(i, "modelID");
      if (id != null)
        sb.append("\nid").append(s).append(PT.esc(id));
      sb.append("\ntitle").append(s).append(PT.esc(ms.getModelTitle(i)));
      sb.append("\nname").append(s).append(PT.esc(ms.getModelName(i)));
      sb.append("\ntype").append(s).append(PT.esc(ms.getModelFileType(i)));
    }
    return sb.toString();
  }

  public Lst<Map<String, Object>> getAllAtomInfo(BS bs) {
    Lst<Map<String, Object>> V = new  Lst<Map<String, Object>>();
    P3 ptTemp = new P3();
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      V.addLast(getAtomInfoLong(i, ptTemp));
    }
    return V;
  }

  private Map<String, Object> getAtomInfoLong(int i, P3 ptTemp) {
    ModelSet ms = vwr.ms;
    Atom atom = ms.at[i];
    Map<String, Object> info = new Hashtable<String, Object>();
    ms.getAtomIdentityInfo(i, info, ptTemp);
    info.put("element", ms.getElementName(i));
    info.put("elemno", Integer.valueOf(ms.at[i].getElementNumber()));
    info.put("x", Float.valueOf(atom.x));
    info.put("y", Float.valueOf(atom.y));
    info.put("z", Float.valueOf(atom.z));
    info.put("coord", P3.newP(atom));
    if (ms.vibrations != null && ms.vibrations[i] != null)
      ms.vibrations[i].getInfo(info);
    info.put("bondCount", Integer.valueOf(atom.getCovalentBondCount()));
    info.put("radius", Float.valueOf((float) (atom.getRasMolRadius() / 120.0)));
    info.put("model", atom.getModelNumberForLabel());
    String shape = atom.atomPropertyString(vwr, T.shape);
    if (shape != null)
      info.put("shape", shape);
    info.put("visible", Boolean.valueOf(atom.checkVisible()));
    info.put("clickabilityFlags", Integer.valueOf(atom.clickabilityFlags));
    info.put("visibilityFlags", Integer.valueOf(atom.shapeVisibilityFlags));
    info.put("spacefill", Float.valueOf(atom.getRadius()));
    String strColor = Escape.escapeColor(vwr
        .gdata.getColorArgbOrGray(atom.colixAtom));
    if (strColor != null)
      info.put("color", strColor);
    info.put("colix", Integer.valueOf(atom.colixAtom));
    boolean isTranslucent = C.isColixTranslucent(atom.colixAtom);
    if (isTranslucent)
      info.put("translucent", Boolean.valueOf(isTranslucent));
    info.put("formalCharge", Integer.valueOf(atom.getFormalCharge()));
    info.put("partialCharge", Float.valueOf(atom.getPartialCharge()));
    float d = atom.getSurfaceDistance100() / 100f;
    if (d >= 0)
      info.put("surfaceDistance", Float.valueOf(d));
    if (ms.am[atom.mi].isBioModel) {
      info.put("resname", atom.getGroup3(false));
      char insCode = atom.group.getInsertionCode();
      int seqNum = atom.getResno();
      if (seqNum > 0)
        info.put("resno", Integer.valueOf(seqNum));
      if (insCode != 0)
        info.put("insertionCode", "" + insCode);
      info.put("name", ms.at[i].getAtomName());
      info.put("chain", atom.getChainIDStr());
      info.put("atomID", Integer.valueOf(atom.atomID));
      info.put("groupID", Integer.valueOf(atom.group.groupID));
      if (atom.altloc != '\0')
        info.put("altLocation", "" + atom.altloc);
      info.put("structure", Integer.valueOf(atom.group.getProteinStructureType()
          .getId()));
      info.put("polymerLength", Integer.valueOf(atom.group.getBioPolymerLength()));
      info.put("occupancy", Integer.valueOf(atom.getOccupancy100()));
      int temp = atom.getBfactor100();
      info.put("temp", Integer.valueOf(temp / 100));
    }
    return info;
  }

  public Lst<Map<String, Object>> getAllBondInfo(Object bsOrArray) {
    Lst<Map<String, Object>> v = new Lst<Map<String, Object>>();
    ModelSet ms = vwr.ms;
    int bondCount = ms.bondCount;
    Bond[] bonds = ms.bo;
    BS bs1;
    if (bsOrArray instanceof String) {
      bsOrArray = vwr.getAtomBitSet(bsOrArray);
    }
    P3 ptTemp = new P3();
    if (bsOrArray instanceof BS[]) {
      bs1 = ((BS[]) bsOrArray)[0];
      BS bs2 = ((BS[]) bsOrArray)[1];
      for (int i = 0; i < bondCount; i++) {
        int ia = bonds[i].atom1.i;
        int ib = bonds[i].atom2.i;
        if (bs1.get(ia) && bs2.get(ib) || bs2.get(ia) && bs1.get(ib))
          v.addLast(getBondInfo(i, ptTemp));
      }
    } else if (bsOrArray instanceof BondSet) {
      bs1 = (BS) bsOrArray;
      for (int i = bs1.nextSetBit(0); i >= 0 && i < bondCount; i = bs1
          .nextSetBit(i + 1))
        v.addLast(getBondInfo(i, ptTemp));
    } else if (bsOrArray instanceof BS) {
      bs1 = (BS) bsOrArray;
      int thisAtom = (bs1.cardinality() == 1 ? bs1.nextSetBit(0) : -1);
      for (int i = 0; i < bondCount; i++) {
        if (thisAtom >= 0 ? (bonds[i].atom1.i == thisAtom || bonds[i].atom2.i == thisAtom)
            : bs1.get(bonds[i].atom1.i) && bs1.get(bonds[i].atom2.i))
          v.addLast(getBondInfo(i, ptTemp));
      }
    }
    return v;
  }

  private Map<String, Object> getBondInfo(int i, P3 ptTemp) {
    Bond bond = vwr.ms.bo[i];
    Atom atom1 = bond.atom1;
    Atom atom2 = bond.atom2;
    Map<String, Object> info = new Hashtable<String, Object>();
    info.put("_bpt", Integer.valueOf(i));
    Map<String, Object> infoA = new Hashtable<String, Object>();
    vwr.ms.getAtomIdentityInfo(atom1.i, infoA, ptTemp);
    Map<String, Object> infoB = new Hashtable<String, Object>();
    vwr.ms.getAtomIdentityInfo(atom2.i, infoB, ptTemp);
    info.put("atom1", infoA);
    info.put("atom2", infoB);
    info.put("jmol_order", "0x" + Integer.toHexString(bond.order));
    info.put("order", Float.valueOf(Edge
        .getBondOrderNumberFromOrder(bond.order)));
    info.put("type", Edge.getBondOrderNameFromOrder(bond.order));
    info.put("radius", Float.valueOf((float) (bond.mad / 2000.)));
    info.put("length_Ang", Float.valueOf(atom1.distance(atom2)));
    info.put("visible", Boolean.valueOf(bond.shapeVisibilityFlags != 0));
    String strColor = Escape.escapeColor(vwr.gdata.getColorArgbOrGray(bond.colix));
    if (strColor != null)
      info.put("color", strColor);
    info.put("colix", Integer.valueOf(bond.colix));
    if (C.isColixTranslucent(bond.colix))
      info.put("translucent", Boolean.TRUE);
    return info;
  }

  public Map<String, Lst<Map<String, Object>>> getAllChainInfo(BS bs) {
    Map<String, Lst<Map<String, Object>>> finalInfo = new Hashtable<String, Lst<Map<String, Object>>>();
    Lst<Map<String, Object>> modelVector = new  Lst<Map<String, Object>>();
    int modelCount = vwr.ms.mc;
    for (int i = 0; i < modelCount; ++i) {
      Map<String, Object> modelInfo = new Hashtable<String, Object>();
      Lst<Map<String, Lst<Map<String, Object>>>> info = getChainInfo(i, bs);
      if (info.size() > 0) {
        modelInfo.put("modelIndex", Integer.valueOf(i));
        modelInfo.put("chains", info);
        modelVector.addLast(modelInfo);
      }
    }
    finalInfo.put("models", modelVector);
    return finalInfo;
  }

  private Lst<Map<String, Lst<Map<String, Object>>>> getChainInfo(
                                                                    int modelIndex,
                                                                    BS bs) {
    Model model = vwr.ms.am[modelIndex];
    int nChains = model.getChainCount(true);
    Lst<Map<String, Lst<Map<String, Object>>>> infoChains = new  Lst<Map<String, Lst<Map<String, Object>>>>();
    P3 ptTemp = new P3();
        for (int i = 0; i < nChains; i++) {
      Chain chain = model.getChainAt(i);
      Lst<Map<String, Object>> infoChain = new  Lst<Map<String, Object>>();
      int nGroups = chain.groupCount;
      Map<String, Lst<Map<String, Object>>> arrayName = new Hashtable<String, Lst<Map<String, Object>>>();
      for (int igroup = 0; igroup < nGroups; igroup++) {
        Group group = chain.groups[igroup];
        if (bs.get(group.firstAtomIndex))
          infoChain.addLast(group.getGroupInfo(igroup, ptTemp));
      }
      if (!infoChain.isEmpty()) {
        arrayName.put("residues", infoChain);
        infoChains.addLast(arrayName);
      }
    }
    return infoChains;
  }

  private Map<String, Lst<Map<String, Object>>> getAllPolymerInfo(BS bs) {
    Map<String, Lst<Map<String, Object>>> info = new Hashtable<String, Lst<Map<String, Object>>>();
    if (vwr.ms.bioModelset != null)
      vwr.ms.bioModelset.getAllPolymerInfo(bs, info);
    return info;
  }

  private String getBasePairInfo(BS bs) {
    SB info = new SB();
    Lst<Bond> vHBonds = new  Lst<Bond>();
    vwr.ms.calcRasmolHydrogenBonds(bs, bs, vHBonds, true, 1, false, null);
    for (int i = vHBonds.size(); --i >= 0;) {
      Bond b = vHBonds.get(i);
      getAtomResidueInfo(info, b.atom1);
      info.append(" - ");
      getAtomResidueInfo(info, b.atom2);
      info.append("\n");
    }
    return info.toString();
  }

  private static void getAtomResidueInfo(SB info, Atom atom) {
    info.append("[").append(atom.getGroup3(false)).append("]").append(
        atom.group.getSeqcodeString()).append(":");
    int id = atom.getChainID();
    info.append(id == 0 ? " " : atom.getChainIDStr());
  }

  private Map<String, Object> getAppletInfo() {
    Map<String, Object> info = new Hashtable<String, Object>();
    info.put("htmlName", vwr.htmlName);
    info.put("syncId", vwr.syncId);
    info.put("fullName", vwr.fullName);
    info.put("codeBase", "" + Viewer.appletCodeBase);
    if (vwr.isApplet) {
      info.put("documentBase", Viewer.appletDocumentBase);
      info.put("registry", vwr.sm.getRegistryInfo());
    }
    info.put("version", JC.version);
    info.put("date", JC.date);
    info.put("javaVendor", Viewer.strJavaVendor);
    info.put("javaVersion", Viewer.strJavaVersion
        + (!Viewer.isJS ? "" : Viewer.isWebGL ? "(WebGL)" : "(HTML5)"));
    info.put("operatingSystem", Viewer.strOSName);
    return info;
  }

  private Map<String, Object> getAnimationInfo() {
    AnimationManager am = vwr.am;
    Map<String, Object> info = new Hashtable<String, Object>();
    info.put("firstModelIndex", Integer.valueOf(am.firstFrameIndex));
    info.put("lastModelIndex", Integer.valueOf(am.lastFrameIndex));
    info.put("animationDirection", Integer.valueOf(am.animationDirection));
    info.put("currentDirection", Integer.valueOf(am.currentDirection));
    info.put("displayModelIndex", Integer.valueOf(am.cmi));
    if (am.animationFrames != null) {
      info.put("isMovie", Boolean.TRUE);
      info.put("frames", Escape.eAI(am.animationFrames));
      info.put("currentAnimationFrame", Integer.valueOf(am.caf));
    }
    info.put("displayModelNumber", vwr.getModelNumberDotted(am.cmi));
    info.put("displayModelName", (am.cmi >= 0 ? vwr.getModelName(am.cmi) : ""));
    info.put("animationFps", Integer.valueOf(am.animationFps));
    info.put("animationReplayMode", T.nameOf(am.animationReplayMode));
    info.put("firstFrameDelay", Float.valueOf(am.firstFrameDelay));
    info.put("lastFrameDelay", Float.valueOf(am.lastFrameDelay));
    info.put("animationOn", Boolean.valueOf(am.animationOn));
    info.put("animationPaused", Boolean.valueOf(am.animationPaused));
    return info;
  }

  private Map<String, Object> getBoundBoxInfo() {
    P3[] pts = vwr.ms.getBoxInfo(null, 1).getBoundBoxPoints(true);
    Map<String, Object> info = new Hashtable<String, Object>();
    info.put("center", P3.newP(pts[0]));
    info.put("vector", V3.newV(pts[1]));
    info.put("corner0", P3.newP(pts[2]));
    info.put("corner1", P3.newP(pts[3]));
    return info;
  }

  private Map<String, Object> getShapeInfo() {
    Map<String, Object> info = new Hashtable<String, Object>();
    SB commands = new SB();
    Shape[] shapes = vwr.shm.shapes;
    if (shapes != null)
      for (int i = 0; i < JC.SHAPE_MAX; ++i) {
        Shape shape = shapes[i];
        if (shape != null) {
          String shapeType = JC.shapeClassBases[i];
          Object shapeDetail = shape.getShapeDetail();
          if (shapeDetail != null)
            info.put(shapeType, shapeDetail);
        }
      }
    if (commands.length() > 0)
      info.put("shapeCommands", commands.toString());
    return info;
  }

  private SV getAnnotationInfo(Object atomExpression, int type) {
    BS bsAtoms = vwr.getAtomBitSet(atomExpression);
    int iModel = vwr.ms.getModelBS(bsAtoms, false).nextSetBit(0);
    if (iModel < 0)
      return null;
    Map<String, Object> modelinfo = vwr.ms.getModelAuxiliaryInfo(iModel);
    SV objAnn = (SV) modelinfo.get(type == T.domains ? "domains"
        : "validation");
    if (objAnn == null || objAnn.tok != T.hash)
      return null;
    vwr.getAnnotationParser(false).initializeAnnotation(objAnn, type, iModel);
    return objAnn.mapGet("_list");    
  }

  @SuppressWarnings("unchecked")
  private Lst<Map<String, Object>> getMeasurementInfo() {
    return (Lst<Map<String, Object>>) vwr.getShapeProperty(JC.SHAPE_MEASURES,
        "info");
  }


  private Object getMouseInfo() {
    if (!vwr.haveDisplay)
      return null;
    Map<String, Object> info = new Hashtable<String, Object>();
    Lst<Object> list = new Lst<Object>();
    ActionManager am = vwr.acm;
    for (Object obj : am.b.getBindings().values()) {
      if (obj instanceof Boolean)
        continue;
      if (AU.isAI(obj)) {
        int[] binding = (int[]) obj;
        obj = new String[] { Binding.getMouseActionName(binding[0], false),
            ActionManager.getActionName(binding[1]) };
      }
      list.addLast(obj);
    }
    info.put("bindings", list);
    info.put("bindingName", am.b.name);
    info.put("actionNames", ActionManager.actionNames);
    info.put("actionInfo", ActionManager.actionInfo);
    info.put("bindingInfo", PT.split(am.getBindingInfo(null), "\n"));
    return info;
  }

  /**
   * PDB or PQR only
   * 
   * @param bs
   *        selected atoms
   * @param out
   *        StringXBuilder or BufferedWriter
   * @return PDB file data string
   */
  @Override
  public String getPdbAtomData(BS bs, OC out, boolean isPQR,
                               boolean doTransform, boolean allTrajectories) {
    if (vwr.ms.ac == 0 || bs.nextSetBit(0) < 0)
      return "";
    if (out == null) {
      out = vwr.getOutputChannel(null, null);
    } else {
      isPQR |= (out.getType().indexOf("PQR") >= 0);
      doTransform |= (out.getType().indexOf("-coord true") >= 0);
    }
    Atom[] atoms = vwr.ms.at;
    Model[] models = vwr.ms.am;
    String occTemp = "%6.2Q%6.2b          ";
    if (isPQR) {
      occTemp = "%8.4P%7.4V       ";
      float charge = 0;
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        charge += atoms[i].getPartialCharge();
      out.append(
          "REMARK   1 PQR file generated by Jmol " + Viewer.getJmolVersion())
          .append("\nREMARK   1 " + "created " + (new Date()))
          .append("\nREMARK   1 Forcefield Used: unknown\nREMARK   1")
          .append("\nREMARK   5")
          .append(
              "\nREMARK   6 Total charge on this protein: " + charge
                  + " e\nREMARK   6\n");
    }

    int iModel = atoms[bs.nextSetBit(0)].mi;
    int iModelLast = -1;
    int lastAtomIndex = bs.length() - 1;
    int lastModelIndex = atoms[lastAtomIndex].mi;
    boolean isMultipleModels = (iModel != lastModelIndex);
    BS bsModels = vwr.ms.getModelBS(bs, true);
    int nModels = bsModels.cardinality();
    Lst<String> lines = new Lst<String>();
    boolean isMultipleBondPDB = models[iModel].isPdbWithMultipleBonds;
    boolean uniqueAtomNumbers = false;
    if (nModels > 1) {
      Object conectArray = null;
      for (int nFiles = 0, i = bsModels.nextSetBit(0); i >= 0; i = bsModels
          .nextSetBit(i + 1)) {
        Object a = vwr.ms.getModelAuxiliaryInfo(i).get("PDB_CONECT_bonds");
        if (a != conectArray || !vwr.ms.am[i].isBioModel) {
          conectArray = a;
          if (nFiles++ > 0) {
            uniqueAtomNumbers = true;
            break;
          }
        }
      }
    }
    LabelToken[] tokens;
    P3 ptTemp = new P3();
    Object[] o = new Object[] { ptTemp };
    Quat q = (doTransform ? vwr.tm.getRotationQ() : null);
    Map<String, Integer> map = new Hashtable<String, Integer>();
    boolean isBiomodel = false;
    int[] firstAtomIndexNew = (uniqueAtomNumbers ? new int[nModels] : null);
    int modelPt = 0;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      Atom a = atoms[i];
      isBiomodel = models[a.mi].isBioModel;
      if (isMultipleModels && a.mi != iModelLast) {
        if (iModelLast != -1) {
          modelPt = fixPDBFormat(lines, map, out, firstAtomIndexNew, modelPt);
          out.append("ENDMDL\n");
        }
        lines = new Lst<String>();
        iModel = iModelLast = a.mi;
        out.append("MODEL     " + (iModelLast + 1) + "\n");
      }
      String sa = a.getAtomName();
      boolean leftJustify = (a.getElementSymbol().length() == 2
          || sa.length() >= 4 || PT.isDigit(sa.charAt(0)));
      boolean isHetero = a.isHetero();
      if (!isBiomodel)
        tokens = (leftJustify ? LabelToken.compile(vwr,
            "HETATM%5.-5i %-4.4a%1AUNK %1c   1%1E   _XYZ_" + occTemp, '\0',
            null) : LabelToken.compile(vwr,
            "HETATM%5.-5i  %-3.3a%1AUNK %1c   1%1E   _XYZ_" + occTemp, '\0',
            null)

        );
      else if (isHetero)
        tokens = (leftJustify ? LabelToken.compile(vwr,
            "HETATM%5.-5i %-4.4a%1A%3.3n %1c%4.-4R%1E   _XYZ_" + occTemp, '\0',
            null) : LabelToken.compile(vwr,
            "HETATM%5.-5i  %-3.3a%1A%3.3n %1c%4.-4R%1E   _XYZ_" + occTemp,
            '\0', null));
      else
        tokens = (leftJustify ? LabelToken.compile(vwr,
            "ATOM  %5.-5i %-4.4a%1A%3.3n %1c%4.-4R%1E   _XYZ_" + occTemp, '\0',
            null) : LabelToken.compile(vwr,
            "ATOM  %5.-5i  %-3.3a%1A%3.3n %1c%4.-4R%1E   _XYZ_" + occTemp,
            '\0', null));
      String XX = a.getElementSymbolIso(false).toUpperCase();
      XX = pdbKey(a.group.getBioPolymerIndexInModel())
          + pdbKey(a.group.groupIndex)
          + LabelToken.formatLabelAtomArray(vwr, a, tokens, '\0', null, ptTemp)
          + (XX.length() == 1 ? " " + XX : XX.substring(0, 2)) + "  ";
      getPointTransf(-1, vwr.ms, a, q, ptTemp);
      String xyz = PT.sprintf("%8.3p%8.3p%8.3p", "p", o);
      if (xyz.length() > 24)
        xyz = PT.sprintf("%8.2p%8.2p%8.2p", "p", o);
      XX = PT.rep(XX, "_XYZ_", xyz);
      lines.addLast(XX);
    }
    fixPDBFormat(lines, map, out, firstAtomIndexNew, modelPt);
    if (isMultipleModels)
      out.append("ENDMDL\n");

    // now for CONECT records...
    modelPt = -1;
    iModelLast = -1;
    String conectKey = "" + (isMultipleModels ? modelPt : 0);
   isBiomodel = false;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      Atom a = atoms[i];
      if (isMultipleModels && a.mi != iModelLast) {
        iModelLast = a.mi;
        isBiomodel = models[iModelLast].isBioModel;
        modelPt++;
      }
      boolean isHetero = (!isBiomodel || a.isHetero());
      boolean isCysS = !isHetero && (a.getElementNumber() == 16);
      if (isHetero || isMultipleBondPDB || isCysS) {
        Bond[] bonds = a.bonds;
        if (bonds != null)
          for (int j = 0; j < bonds.length; j++) {
            int iThis = a.getAtomNumber();
            Atom a2 = bonds[j].getOtherAtom(a);
            if (!bs.get(a2.i))
              continue;
            int n = bonds[j].getCovalentOrder();
            if (n == 1
                && (isMultipleBondPDB && !isHetero && !isCysS || isCysS
                    && a2.getElementNumber() != 16))
              continue;
            int iOther = a2.getAtomNumber();
            switch (n) {
            case 2:
            case 3:
              if (iOther < iThis)
                continue; // only one entry in this case -- pseudo-PmapDB style
              //$FALL-THROUGH$
            case 1:
              Integer inew = map.get(conectKey + "."
                  + Integer.valueOf(iThis));
              Integer inew2 = map.get(conectKey + "."
                  + Integer.valueOf(iOther));
              if (inew == null || inew2 == null)
                break;
              out.append("CONECT").append(
                  PT.formatStringS("%5s", "s", "" + inew));
              String s = PT.formatStringS("%5s", "s", "" + inew2);
              for (int k = 0; k < n; k++)
                out.append(s);
              out.append("\n");
              break;
            }
          }
      }
    }

    return out.toString();
  }

  private String pdbKey(int np) {
    String xp = (np < 0 ? "~999" : "   " + np); 
    return xp.substring(xp.length() - 4);
  }

  /**
   * must re-order by resno and then renumber atoms and add TER records based on
   * BioPolymers
   * 
   * note: 3hbt has a break between residues 39 and 51 with no TER record, but
   * Jmol will put that in.
   * 
   * @param lines
   * @param map
   * @param out
   * @param modelPt
   * @param firstAtomIndexNew
   * @return new modelPt
   */
  private int fixPDBFormat(Lst<String> lines, Map<String, Integer> map, OC out,
                           int[] firstAtomIndexNew, int modelPt) {
    lines.addLast("~999~999XXXXXX99999999999999999999~99~");
    String[] alines = new String[lines.size()];
    lines.toArray(alines);
    Arrays.sort(alines);
    lines.clear();
    for (int i = 0, n = alines.length; i < n; i++) {
      lines.addLast(alines[i]);
    }
    String lastPoly = null;
    String lastLine = null;
    int n = lines.size();
    int newAtomNumber = 0;
    int iBase = (firstAtomIndexNew == null ? 0 : firstAtomIndexNew[modelPt]);
    for (int i = 0; i < n; i++) {
      String s = lines.get(i);
      String poly = s.substring(0, 4);
      s = s.substring(8);
      boolean isTerm = false;
      boolean isLast = (s.indexOf("~99~") >= 0);
      if (!poly.equals(lastPoly) || isLast) {
        if (lastPoly != null && !lastPoly.equals("~999")) {
          isTerm = true;
          //TER     458      ASN A  78C                                                      
          s = "TER   " + lastLine.substring(6, 11) + "      "
              + lastLine.substring(17, 27);
          lines.add(i, poly + "~~~~" + s);
          n++;
        }
        lastPoly = poly;
      }
      if (isLast && !isTerm)
        break;
      lastLine = s;
      newAtomNumber = i + 1 + iBase;
      if (map != null && !isTerm)
        map.put(
            "" + modelPt + "."
                + Integer.valueOf(PT.parseInt(s.substring(6, 11))),
            Integer.valueOf(newAtomNumber));
      String si = "     " + newAtomNumber;
      out.append(s.substring(0, 6)).append(si.substring(si.length() - 5))
          .append(s.substring(11)).append("\n");
    }
    if (firstAtomIndexNew != null && ++modelPt < firstAtomIndexNew.length)
      firstAtomIndexNew[modelPt] = newAtomNumber;
    return modelPt;
  }

//
//  /**
//   * PDB line sorter 
//   * @param s1 
//   * @param s2 
//   * @return -1, 0, or 1
//   */
//  public int compare(String s1, String s2) {
//    int atA = PT.parseInt(s1.substring(10, 16));
//    int atB = PT.parseInt(s2.substring(10, 16));
//    int resA = PT.parseInt(s1.substring(26, 30));
//    int resB = PT.parseInt(s2.substring(26, 30));
//    return (resA < resB ? -1 : resA > resB ? 1 : atA < atB ? -1
//        : atA > atB ? 1 : 0);
//  }

  /* **********************
   * 
   * Jmol Data Frame methods
   * 
   *****************************/

  @Override
  @SuppressWarnings("static-access")
  public String getPdbData(int modelIndex, String type, BS bsSelected,
                           Object[] parameters, OC out, boolean addStructure) {
    if (vwr.ms.isJmolDataFrameForModel(modelIndex))
      modelIndex = vwr.ms.getJmolDataSourceFrame(modelIndex);
    if (modelIndex < 0)
      return "";
    Model model = vwr.ms.am[modelIndex];
    boolean isPDB = model.isBioModel;
    if (parameters == null && !isPDB)
      return null;
    if (out == null)
      out = vwr.getOutputChannel(null, null);
    SB pdbCONECT = new SB();
    boolean isDraw = (type.indexOf("draw") >= 0);
    BS bsAtoms = null;
    BS bsWritten = new BS();
    char ctype = '\0';
    LabelToken[] tokens = vwr.ms.getLabeler().compile(vwr,
        "ATOM  %-6i%4a%1A%3.-3n %1c%4R%1E   ", '\0', null);
    if (parameters == null) {
      ctype = (type.length() > 11 && type.indexOf("quaternion ") >= 0 ? type
          .charAt(11) : 'R');
      ((BioModel) model).getPdbData(type, ctype, isDraw, bsSelected, out, tokens, pdbCONECT,
          bsWritten);
      bsAtoms = vwr.getModelUndeletedAtomsBitSet(modelIndex);
    } else {
      // plot property x y z....
      bsAtoms = (BS) parameters[0];
      float[] dataX = (float[]) parameters[1];
      float[] dataY = (float[]) parameters[2];
      float[] dataZ = (float[]) parameters[3];
      boolean haveY = (dataY != null);
      boolean haveZ = (dataZ != null);
      P3 minXYZ = (P3) parameters[4];
      P3 maxXYZ = (P3) parameters[5];
      P3 factors = (P3) parameters[6];
      P3 center = (P3) parameters[7];
      String format = (String) parameters[8];
      String[] properties = (String[]) parameters[9];
      boolean isPDBFormat = (factors != null && format == null);
      Atom[] atoms = vwr.ms.at;
      if (isPDBFormat) {
        out.append("REMARK   6 Jmol PDB-encoded data: ").append(type)
            .append("; ").append(Viewer.getJmolVersion()).append("; ").append(vwr.apiPlatform.getDateFormat(null)).append("\n");
        out.append("REMARK   6 Jmol data").append(" min = ")
            .append(Escape.eP(minXYZ)).append(" max = ")
            .append(Escape.eP(maxXYZ)).append(" unScaledXyz = xyz * ")
            .append(Escape.eP(factors)).append(" + ").append(Escape.eP(center))
            .append(";\n");
        String atomNames = null;
        for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms
            .nextSetBit(i + 1)) {
          String name = "" + atoms[i].getAtomName();
          if (atomNames != null || name.length() > 4) {
            if (atomNames == null) {
              atomNames = "";
              i = -1;
              continue;
            }
            atomNames += " " + name;
          }
        }
        if (atomNames != null)
          out.append("REMARK   6 Jmol atom names").append(atomNames).append("\n");
        String resNames = null;
        for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms
            .nextSetBit(i + 1)) {
          String name = "" + atoms[i].getGroup3(true);
          if (resNames != null || name.length() > 3) {
            if (resNames == null) {
              resNames = "";
              i = -1;
              continue;
            }
            resNames += " " + name;
          }
        }
        if (resNames != null)
          out.append("REMARK   6 Jmol residue names").append(resNames).append("\n");
        for (int i = 0; i < properties.length; i++)
          if (properties[i] != null)
            out.append("REMARK   6 Jmol property ").append(properties[i]).append(";\n");
      }
      String strExtra = "";
      Atom atomLast = null;
      P3 ptTemp = new P3();
      if (!isPDBFormat) {
        if (format == null)
          format = "%-5i %-10s %-13.5f "
              + (haveZ ? "%-13.5f %-13.5f" : haveY ? "%-13.5f" : "");
        format += "\n";

      }
      for (int i = bsAtoms.nextSetBit(0), n = 0; i >= 0; i = bsAtoms
          .nextSetBit(i + 1), n++) {
        float x = dataX[n];
        float y = (haveY ? dataY[n] : 0f);
        float z = (haveZ ? dataZ[n] : 0f);
        if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
          continue;
        Atom a = atoms[i];
        if (isPDBFormat) {
          out.append(LabelToken.formatLabelAtomArray(vwr, a, tokens, '\0',
              null, ptTemp));
          if (isPDB)
            bsWritten.set(i);
          out.append(PT.sprintf(
              "%-8.2f%-8.2f%-10.2f    %6.3f          %2s    %s\n", "ssF",
              new Object[] { a.getElementSymbolIso(false).toUpperCase(),
                  strExtra, new float[] { x, y, z, 0f } }));
          if (atomLast != null
              && atomLast.group.getBioPolymerIndexInModel() == a.group
                  .getBioPolymerIndexInModel())
            pdbCONECT.append("CONECT")
                .append(PT.formatStringI("%5i", "i", atomLast.getAtomNumber()))
                .append(PT.formatStringI("%5i", "i", a.getAtomNumber()))
                .appendC('\n');
        } else if (haveZ) {
          out.append(PT.sprintf(
              format,
              "isF",
              new Object[] { Integer.valueOf(a.getAtomNumber()),
                  a.getAtomName(), new float[] { x, y, z } }));
        } else if (haveY) {
          out.append(PT.sprintf(
              format,
              "isF",
              new Object[] { Integer.valueOf(a.getAtomNumber()),
                  a.getAtomName(), new float[] { x, y } }));
        } else {
          out.append(PT.sprintf(
              format,
              "isF",
              new Object[] { Integer.valueOf(a.getAtomNumber()),
                  a.getAtomName(), new float[] { x } }));
        }
        atomLast = a;
      }
    }
    out.append(pdbCONECT.toString());
    if (isDraw)
      return out.toString();
    bsSelected.and(bsAtoms);
    if (isPDB && addStructure)
      out.append("\n\n"
          + vwr.ms.getProteinStructureState(bsWritten, ctype == 'R' ? T.ramachandran : T.pdb));
    return out.toString();
  }

  /*
   * <molecule title="acetic_acid.mol"
   * xmlns="http://www.xml-cml.org/schema/cml2/core"
   * xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   * xsi:schemaLocation="http://www.xml-cml.org/schema/cml2/core cmlAll.xsd">
   * <atomArray> <atom id="a1" elementType="C" x3="0.1853" y3="0.0096"
   * z3="0.4587"/> <atom id="a2" elementType="O" x3="0.6324" y3="1.0432"
   * z3="0.8951"/> <atom id="a3" elementType="C" x3="-1.0665" y3="-0.1512"
   * z3="-0.3758"/> <atom id="a4" elementType="O" x3="0.7893" y3="-1.1734"
   * z3="0.6766" formalCharge="-1"/> <atom id="a5" elementType="H" x3="-1.7704"
   * y3="-0.8676" z3="0.1055"/> <atom id="a6" elementType="H" x3="-0.8068"
   * y3="-0.5215" z3="-1.3935"/> <atom id="a7" elementType="H" x3="-1.5889"
   * y3="0.8259" z3="-0.4854"/> </atomArray> <bondArray> <bond atomRefs2="a1 a2"
   * order="partial12"/> <bond atomRefs2="a1 a3" order="S"/> <bond
   * atomRefs2="a1 a4" order="partial12"/> <bond atomRefs2="a3 a5" order="S"/>
   * <bond atomRefs2="a3 a6" order="S"/> <bond atomRefs2="a3 a7" order="S"/>
   * </bondArray> </molecule>
   */
  @Override
  public String getModelCml(BS bs, int atomsMax, boolean addBonds, boolean doTransform, boolean allTrajectories) {
    // not allowing full trajectory business here. 
    SB sb = new SB();
    int nAtoms = bs.cardinality();
    if (nAtoms == 0)
      return "";
    // creating an instance prevents pre-loading by JavaScript
    if (Viewer.isJS)
      Interface.getInterface("javajs.util.XmlUtil", vwr, "file");
    XmlUtil.openTag(sb, "molecule");
    XmlUtil.openTag(sb, "atomArray");
    BS bsAtoms = new BS();
    Atom[] atoms = vwr.ms.at;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      if (--atomsMax < 0)
        break;
      Atom atom = atoms[i];
      String name = atom.getAtomName();
      PT.rep(name, "\"", "''");
      bsAtoms.set(atom.i);
      XmlUtil.appendTag(sb, "atom/", new String[] { "id",
          "a" + (atom.i + 1), "title", atom.getAtomName(), "elementType",
          atom.getElementSymbol(), "x3", "" + atom.x, "y3", "" + atom.y, "z3",
          "" + atom.z });
    }
    XmlUtil.closeTag(sb, "atomArray");
    if (addBonds) {
      XmlUtil.openTag(sb, "bondArray");
      int bondCount = vwr.ms.bondCount;
      Bond[] bonds = vwr.ms.bo;
      for (int i = 0; i < bondCount; i++) {
        Bond bond = bonds[i];
        Atom a1 = bond.atom1;
        Atom a2 = bond.atom2;
        if (!bsAtoms.get(a1.i) || !bsAtoms.get(a2.i))
          continue;
        String order = Edge.getCmlBondOrder(bond.order);
        if (order == null)
          continue;
        XmlUtil.appendTag(sb, "bond/", new String[] { "atomRefs2",
            "a" + (bond.atom1.i + 1) + " a" + (bond.atom2.i + 1),
            "order", order, });
      }
      XmlUtil.closeTag(sb, "bondArray");
    }
    XmlUtil.closeTag(sb, "molecule");
    return sb.toString();
  }

  /**
   * Fix a JME string returned from NCI CIR to have the proper formal charges.
   */
  @Override
  public String fixJMEFormalCharges(BS bsAtoms, String jme) {
    boolean haveCharges = false;
    if (bsAtoms == null)
      return jme;
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
      Atom a = vwr.ms.at[i];
      if (a.getFormalCharge() != 0) {
        haveCharges = true;
        break;
      }
    }
    if (!haveCharges)
      return jme;
    int[][] map = vwr.getSmilesMatcher().getMapForJME(jme, vwr.ms.at, bsAtoms);
    if (map == null)
      return jme;
    int[] jmeMap = map[0];
    int[] jmolMap = map[1];
    String[] tokens = PT.getTokens(jme);
    // map points only to 
    int nAtoms = PT.parseInt(tokens[0]);
    int[] mapjj = new int[nAtoms];
    for (int i = jmeMap.length; --i >= 0;) {
      mapjj[jmeMap[i]] = jmolMap[i] + 1;
    }
    int ipt = 0;
    for (int pt = 2; pt < tokens.length; pt += 3) {
      String jmeAtom = tokens[pt];
      // stop if we are at the bonds
      if (PT.parseInt(jmeAtom) != Integer.MIN_VALUE)
        break;
      jmeAtom = PT.replaceAllCharacters(jmeAtom, "+-", "");
      // look out for H still in there
      Atom a = vwr.ms.at[mapjj[ipt++] - 1];
      String elem = a.getElementSymbol();
      if (!elem.equals(jmeAtom)) {
        // we tried, but this is not working
        return jme;
      }
      int charge = a.getFormalCharge();
      if (charge != 0)
        tokens[pt] = jmeAtom + (charge > 0 ? "+" : "-")
            + (Math.abs(charge) > 1 ? "" + Math.abs(charge) : "");
   }
    return PT.join(tokens, ' ', 0);
  }


}
