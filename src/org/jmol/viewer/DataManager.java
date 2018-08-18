/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-10-03 20:53:36 -0500 (Wed, 03 Oct 2007) $
 * $Revision: 8351 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development Team
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

import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.JmolDataManager;
import org.jmol.c.VDW;
import org.jmol.java.BS;
import org.jmol.modelset.AtomCollection;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.Elements;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Parser;

import javajs.util.AU;
import javajs.util.PT;
import javajs.util.SB;

/*
 * a class for storing and retrieving user data,
 * including atom-related and color-related data
 * 
 */

public class DataManager implements JmolDataManager {

  private Map<String, Object[]> dataValues = new Hashtable<String, Object[]>();

  private Viewer vwr;

  public DataManager() {
    // for reflection
  }

  @Override
  public JmolDataManager set(Viewer vwr) {
    this.vwr = vwr;
    return this;
  }

  @Override
  public void clear() {
    dataValues.clear();
  }

  /**
   * This method needs to be redone using a class instead of Object[]
   */
  @Override
  public void setData(String type, Object[] data, int arrayCount,
                      int actualAtomCount, int matchField,
                      int matchFieldColumnCount, int field, int fieldColumnCount) {
    //Eval
    /*
     * data[0] -- label
     * data[1] -- string or float[] or float[][] or float[][][]
     * data[2] -- selection bitset or int[] atomMap when field > 0
     * data[3] -- arrayDepth 0(String),1(float[]),2,3(float[][][]) or -1 for set by data type
     * data[4] -- Boolean.TRUE == saveInState
     * 
     * matchField = data must match atomNo in this column, >= 1
     * field = column containing the data, >= 1:
     *   0 ==> values are a simple list; clear the data
     *   Integer.MAX_VALUE ==> values are a simple list; don't clear the data
     *   Integer.MIN_VALUE ==> one SINGLE data value should be used for all selected atoms
     */
    if (type == null) {
      clear(  );
      return;
    }
    type = type.toLowerCase();
    if (type.equals("element_vdw")) {
      String stringData = ((String) data[JmolDataManager.DATA_VALUE]).trim();
      if (stringData.length() == 0) {
        vwr.userVdwMars = null;
        vwr.userVdws = null;
        vwr.bsUserVdws = null;
        return;
      }
      if (vwr.bsUserVdws == null)
        vwr.setUserVdw(vwr.defaultVdw);
      Parser.parseFloatArrayFromMatchAndField(stringData, vwr.bsUserVdws, 1, 0,
          (int[]) data[JmolDataManager.DATA_SELECTION], 2, 0, vwr.userVdws, 1);
      for (int i = vwr.userVdws.length; --i >= 0;)
        vwr.userVdwMars[i] = (int) Math.floor(vwr.userVdws[i] * 1000);
      return;
    }
    int depth = getType(data);
    Object val = data[JmolDataManager.DATA_VALUE];
    if (depth == JmolDataManager.DATA_TYPE_UNKNOWN)
      data[JmolDataManager.DATA_TYPE] = Integer
          .valueOf(depth = (val instanceof String ? JmolDataManager.DATA_TYPE_STRING
              : AU.isAF(val) ? JmolDataManager.DATA_TYPE_AF
                  : AU.isAFF(val) ? JmolDataManager.DATA_TYPE_AFF : AU
                      .isAFFF(val) ? JmolDataManager.DATA_TYPE_AFFF
                      : JmolDataManager.DATA_TYPE_UNKNOWN));
    if (data[JmolDataManager.DATA_SELECTION] != null && arrayCount > 0) {
      boolean createNew = (matchField != 0 || field != Integer.MIN_VALUE
          && field != Integer.MAX_VALUE);
      Object[] oldData = dataValues.get(type);
      BS bs;
      float[] f = (oldData == null || createNew ? new float[actualAtomCount]
          : AU.ensureLengthA(((float[]) oldData[JmolDataManager.DATA_VALUE]), actualAtomCount));

      // check to see if the data COULD be interpreted as a string of float values
      // and if so, do that. This pre-fetches the tokens in that case.

      if (depth == JmolDataManager.DATA_TYPE_UNKNOWN) {
        Logger.error("Cannot determine data type for " + val);
        return;
      }
      String stringData = (depth == JmolDataManager.DATA_TYPE_STRING ? (String) val
          : null);
      float[] floatData = (depth == JmolDataManager.DATA_TYPE_AF ? (float[]) val
          : null);
      String[] strData = null;
      if (field == Integer.MIN_VALUE
          && (strData = PT.getTokens(stringData)).length > 1)
        field = 0;
      if (field == Integer.MIN_VALUE) {
        // set the selected data elements to a single value
        bs = (BS) data[JmolDataManager.DATA_SELECTION];
        setSelectedFloats(PT.parseFloat(stringData), bs, f);
      } else if (field == 0 || field == Integer.MAX_VALUE) {
        // just get the selected token values
        bs = (BS) data[JmolDataManager.DATA_SELECTION];
        if (floatData != null) {
          int n = floatData.length;
          if (n == bs.cardinality()) {
            for (int i = bs.nextSetBit(0), pt = 0; i >= 0; i = bs
                .nextSetBit(i + 1), pt++)
              f[i] = floatData[pt];
          } else {
            for (int i = bs.nextSetBit(0); i >= 0 && i < n; i = bs
                .nextSetBit(i + 1))
              f[i] = floatData[i];
          }
        } else {
          Parser.parseFloatArrayBsData(
              strData == null ? PT.getTokens(stringData) : strData, bs, f);
        }
      } else if (matchField <= 0) {
        // get the specified field >= 1 for the selected atoms
        bs = (BS) data[JmolDataManager.DATA_SELECTION];
        Parser.parseFloatArrayFromMatchAndField(stringData, bs, 0, 0, null,
            field, fieldColumnCount, f, 1);
      } else {
        // get the selected field, with an integer match in a specified field
        // in this case, bs is created and indicates which data points were set
        int[] iData = (int[]) data[JmolDataManager.DATA_SELECTION];
        Parser.parseFloatArrayFromMatchAndField(stringData, null, matchField,
            matchFieldColumnCount, iData, field, fieldColumnCount, f, 1);
        bs = new BS();
        for (int i = iData.length; --i >= 0;)
          if (iData[i] >= 0)
            bs.set(iData[i]);
      }
      if (oldData != null
          && oldData[JmolDataManager.DATA_SELECTION] instanceof BS
          && !createNew)
        bs.or((BS) (oldData[JmolDataManager.DATA_SELECTION]));
      data[JmolDataManager.DATA_TYPE] = Integer
          .valueOf(JmolDataManager.DATA_TYPE_AF);
      data[JmolDataManager.DATA_SELECTION] = bs;
      data[JmolDataManager.DATA_VALUE] = f;
      if (type.indexOf("property_atom.") == 0) {
        int tok = T.getSettableTokFromString(type = type.substring(14));
        if (tok == T.nada) {
          Logger.error("Unknown atom property: " + type);
          return;
        }
        int nValues = bs.cardinality();
        float[] fValues = new float[nValues];
        for (int n = 0, i = bs.nextSetBit(0); n < nValues; i = bs
            .nextSetBit(i + 1))
          fValues[n++] = f[i];
        vwr.setAtomProperty(bs, tok, 0, 0, null, fValues, null);
        return;
      }
    }
    dataValues.put(type, data);
  }

  private int getType(Object[] data) {
    return ((Integer) data[JmolDataManager.DATA_TYPE]).intValue();
  }

  /**
   * 
   * @param f
   * @param bs
   * @param data
   */
  private static void setSelectedFloats(float f, BS bs, float[] data) {
    boolean isAll = (bs == null);
    int i0 = (isAll ? 0 : bs.nextSetBit(0));
    for (int i = i0; i >= 0 && i < data.length; i = (isAll ? i + 1 : bs
        .nextSetBit(i + 1)))
      data[i] = f;
  }

  @Override
  public Object getData(String label, BS bsSelected, int dataType) {
    if (dataValues.size() == 0 || label == null)
      return null;
    label = label.toLowerCase();
    switch (dataType) {
    case JmolDataManager.DATA_TYPE_UNKNOWN:
    case JmolDataManager.DATA_TYPE_LAST:
      if (!label.equals("types"))
        return dataValues.get(label);
      String[] info = new String[2];
      info[JmolDataManager.DATA_LABEL] = "types";
      info[JmolDataManager.DATA_VALUE] = "";
      int nv = 0;
      for (String name : dataValues.keySet())
        info[JmolDataManager.DATA_VALUE] += (nv++ > 0 ? "\n" : "") + name;
      return info;
    default:
      Object[] data = dataValues.get(label);
      if (data == null || getType(data) != dataType)
        return null;
      if (bsSelected == null)
        return data[JmolDataManager.DATA_VALUE];
      // When bsSelected is not null, this returns a truncated array, which must be of type float[]
      float[] f = (float[]) data[JmolDataManager.DATA_VALUE];
      float[] fnew = new float[bsSelected.cardinality()];
      // load array
      for (int i = 0, n = f.length, p = bsSelected.nextSetBit(0); p >= 0
          && i < n; p = bsSelected.nextSetBit(p + 1))
        fnew[i++] = f[p];
      return fnew;
    }
  }

//  @Override
//  public float getDataFloatAt(String label, int atomIndex) {
//    if (dataValues.size() > 0) {
//      Object[] data = (Object[]) getData(label, null, 0);
//      if (data != null && getType(data) == JmolDataManager.DATA_TYPE_AF) {
//        float[] f = (float[]) data[JmolDataManager.DATA_VALUE];
//        if (atomIndex < f.length)
//          return f[atomIndex];
//      }
//    }
//    return Float.NaN;
//  }

  @Override
  public void deleteModelAtoms(int firstAtomIndex, int nAtoms, BS bsDeleted) {
    if (dataValues.size() == 0)
      return;
    for (String name : dataValues.keySet()) {
      if (name.indexOf("property_") == 0) {
        Object[] obj = dataValues.get(name);
        BSUtil.deleteBits((BS) obj[DATA_SELECTION], bsDeleted);
        obj[DATA_VALUE] = AU.deleteElements(obj[DATA_VALUE], firstAtomIndex,
            nAtoms);
      }
    }
  }

  @Override
  public String getDefaultVdwNameOrData(VDW type, BS bs) {
    SB sb = new SB();
    sb.append(type.getVdwLabel()).append("\n");
    boolean isAll = (bs == null);
    int i0 = (isAll ? 1 : bs.nextSetBit(0));
    int i1 = (isAll ? Elements.elementNumberMax : bs.length());
    for (int i = i0; i < i1 && i >= 0; i = (isAll ? i + 1 : bs
        .nextSetBit(i + 1)))
      sb.appendI(i)
          .appendC('\t')
          .appendF(
              type == VDW.USER ? vwr.userVdws[i] : Elements.getVanderwaalsMar(
                  i, type) / 1000f).appendC('\t')
          .append(Elements.elementSymbolFromNumber(i)).appendC('\n');
    return (bs == null ? sb.toString() : "\n  DATA \"element_vdw\"\n"
        + sb.append("  end \"element_vdw\";\n\n").toString());
  }

  @Override
  public boolean getDataState(JmolStateCreator sc, SB sb) {
    if (dataValues.size() == 0)
      return false;
    boolean haveData = false;
    for (String name : dataValues.keySet()) {
      if (name.indexOf("property_") == 0) {
        Object[] obj = dataValues.get(name);
        // default for "save in state" is TRUE
        if (obj.length > JmolDataManager.DATA_SAVE_IN_STATE
            && !((Boolean) obj[JmolDataManager.DATA_SAVE_IN_STATE])
                .booleanValue())
          continue;
        haveData = true;
        Object data = obj[DATA_VALUE];
        if (data != null
            && getType(obj) == JmolDataManager.DATA_TYPE_AF) {
          sc.getAtomicPropertyStateBuffer(sb, AtomCollection.TAINT_MAX,
              (BS) obj[DATA_SELECTION], name, (float[]) data);
          sb.append("\n");
        } else {
          // should not be here -- property_xxx is always float[]
          sb.append("\n").append(
              Escape.encapsulateData(name, data,
                  JmolDataManager.DATA_TYPE_UNKNOWN));//j2s issue?
        }
        continue;
      }
      int type = (name.indexOf("data2d") == 0 ? JmolDataManager.DATA_TYPE_AFF
          : name.indexOf("data3d") == 0 ? JmolDataManager.DATA_TYPE_AFFF
              : JmolDataManager.DATA_TYPE_UNKNOWN);
      if (type == JmolDataManager.DATA_TYPE_UNKNOWN)
        continue;
      Object[] obj = dataValues.get(name);
      Object data = obj[DATA_VALUE];
      if (data != null && getType(obj) == type) {
        haveData = true;
        sb.append("\n").append(Escape.encapsulateData(name, data, type));
      }
    }
    return haveData;
  }

  @Override
  public Object[] createFileData(String strModel) {
    Object[] o = new Object[4];
    o[JmolDataManager.DATA_LABEL] = "model";
    o[JmolDataManager.DATA_VALUE] = strModel;
    o[JmolDataManager.DATA_TYPE] = Integer.valueOf(DATA_TYPE_STRING);
    return o;
  }

}
