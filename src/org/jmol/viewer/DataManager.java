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
import org.jmol.modelset.AtomCollection;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.Elements;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Parser;

import javajs.util.AU;
import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;

/*
 * A class for storing and retrieving user data,
 * including atom-related and color-related data.
 * 
 * Starting in Jmol 15.2.51p (precision branch), this code
 * accepts double[ ] data and preserves its integrity. 
 * 
 * The DATA_TYPE_AForD allows for either AF or AD 
 * 
 */

public class DataManager implements JmolDataManager {

  private Map<String, Object[]> dataValues = new Hashtable<String, Object[]>();

  private Viewer vwr;

  public DataManager() {
    // for reflection
  }

  @Override
  public DataManager set(Viewer vwr) {
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
  public void setData(String type, Object[] data, int atomCount,
                      int actualAtomCount, int matchField,
                      int matchFieldColumnCount, int field,
                      int fieldColumnCount) {
    try {
      /*
       * data[0] -- label
       * 
       * data[1] -- string or double[] or double[][] or double[][][]
       * 
       * data[2] -- selection bitset or int[] atomMap when field > 0
       *
       * data[3] -- arrayDepth 0(String),1(double[]),2,3(double[][][]) or -1 for set by data type
      *
       * data[4] -- Boolean.TRUE == saveInState
       * 
       * data[5] -- ModelLoader atomProperty Map.Entry for revising array data
       * 
       * matchField = data must match atomNo in this column, >= 1
       * field = column containing the data, >= 1:
       *   0 ==> values are a simple list; clear the data
       *   Integer.MAX_VALUE ==> values are a simple list; don't clear the data
       *   Integer.MIN_VALUE ==> one SINGLE data value should be used for all selected atoms
       */

      if (type == null) {
        clear();
        return;
      }
//      System.out.println(PT.toJSON(null, data));
//      System.out.println(PT.toJSON(null, dataValues));
      type = type.toLowerCase();
      if (type.equals("element_vdw")) {
        setVDW(data);
        return;
      }
      int newType = getType(data);
      data[DATA_TYPE] = Integer.valueOf(newType);
      Object newVal = data[DATA_VALUE];
      if (newType == DATA_TYPE_UNKNOWN)
        data[DATA_TYPE] = Integer.valueOf(newType = getTypeFor(newVal));
      if (data[DATA_SELECTION] == null || atomCount <= 0) {
        dataValues.put(type, data);
        return;
      }
      
      // must be atom-based bit set DATA_SELECTION on arrays

      if (newType == DATA_TYPE_UNKNOWN) {
        Logger.error("Cannot determine data type for " + newVal);
        return;
      }
      BS bs;
      double[] d = null;
      double[][] dd = null;
      String stringData = null;
      double[] dData = null;
      double[][] ddData = null;
      String[] tokens = null;

      boolean createNew = (matchField != 0
          || field != Integer.MIN_VALUE && field != Integer.MAX_VALUE);
      Object[] oldData = dataValues.get(type);
      Object oldValue = (oldData == null ? null : oldData[DATA_VALUE]);
      //int oldType = getType(oldData);
      if (newType != DATA_TYPE_ADD) {
        d = (oldData == null || createNew ? new double[actualAtomCount]
            : AU.ensureLengthD(((double[]) oldValue), actualAtomCount));
        // check for a atomProperty 
        if (d != oldValue && type.startsWith("property_") && !type.startsWith("property_atom.")) {
//          @SuppressWarnings("rawtypes")    
//          Map.Entry e = (Map.Entry) data[DATA_ATOM_PROP_ENTRY];
//          if (e != null)
//            e.setValue(d);
        }
      }

      // check to see if the data COULD be interpreted as a string of double values
      // and if so, do that. This pre-fetches the tokens in that case.

      switch (newType) {
      case DATA_TYPE_STRING:
        stringData = (String) newVal;
        break;
      case DATA_TYPE_AD:
        dData = (double[]) newVal;
        break;
      case DATA_TYPE_ADD:
        ddData = (double[][]) newVal;
        break;
      }
      if (field == Integer.MIN_VALUE
          && (tokens = PT.getTokens(stringData)).length > 1) {
        field = 0;
      }
      if (field == Integer.MIN_VALUE) {
        // set the selected data elements to a single value
        bs = (BS) data[DATA_SELECTION];
        setSelected(PT.parseDouble(stringData), bs, d);
      } else if (field == 0 || field == Integer.MAX_VALUE) {
        // just set the selected token values
        // set f, d, or ff 
        bs = (BS) data[DATA_SELECTION];
        if (dData != null) {
          int n = dData.length;
          if (n == bs.cardinality()) {
            fillSparseArray(dData, bs, d);
          } else {
            for (int i = bs.nextSetBit(0); i >= 0
                && i < n; i = bs.nextSetBit(i + 1))
              d[i] = dData[i];
          }
        } else if (stringData != null) {
          Parser.parseDoubleArrayBsData(
              tokens == null ? PT.getTokens(stringData) : tokens, bs, d);
        } else if (ddData != null) {
          int n = ddData.length;
          dd = (oldData == null || createNew ? AU.newDouble2(actualAtomCount)
              : (double[][]) AU.ensureLength(oldValue, actualAtomCount));
          if (n == bs.cardinality()) {
            for (int i = bs.nextSetBit(0), pt = 0; i >= 0; i = bs
                .nextSetBit(i + 1), pt++)
              fillSparseArray(ddData[pt], bs,
                  dd[i] = new double[actualAtomCount]);
          } else {
            for (int i = bs.nextSetBit(0); i >= 0
                && i < n; i = bs.nextSetBit(i + 1))
              dd[i] = ddData[i];
          }
        }
      } else if (matchField <= 0) {
        // get the specified field >= 1 for the selected atoms
        bs = (BS) data[DATA_SELECTION];
        Parser.parseDoubleArrayFromMatchAndField(stringData, bs, 0, 0, null,
            field, fieldColumnCount, d, 1);
      } else {
        // get the selected field, with an integer match in a specified field
        // in this case, bs is created and indicates which data points were set
        int[] iData = (int[]) data[DATA_SELECTION];
        Parser.parseDoubleArrayFromMatchAndField(stringData, null, matchField,
            matchFieldColumnCount, iData, field, fieldColumnCount, d, 1);
        bs = new BS();
        for (int i = iData.length; --i >= 0;)
          if (iData[i] >= 0)
            bs.set(iData[i]);
      }
      // add the data and the type
      if (oldData != null && oldData[DATA_SELECTION] instanceof BS
          && !createNew)
        bs.or((BS) (oldData[DATA_SELECTION]));
      data[DATA_SELECTION] = bs;
      if (dd != null) {
        data[DATA_TYPE] = Integer.valueOf(DATA_TYPE_ADD);
        data[DATA_VALUE] = dd;
      } else {
        data[DATA_TYPE] = Integer.valueOf(DATA_TYPE_AD);
        data[DATA_VALUE] = d;
      }
      if (type.indexOf("property_atom.") == 0) {
        // these must be double[] only
        int tok = T.getSettableTokFromString(type = type.substring(14));
        if (tok == T.nada) {
          Logger.error("Unknown atom property: " + type);
          return;
        }
        int nValues = bs.cardinality();
        double[] fValues = new double[nValues];
        for (int n = 0, i = bs.nextSetBit(0); n < nValues; i = bs
            .nextSetBit(i + 1))
          fValues[n++] = d[i];
        vwr.setAtomProperty(bs, tok, 0, 0, null, fValues, null);
        return;
      }
      dataValues.put(type, data);
    } catch (Exception e) {
      Logger.error("DataManager failed :" + e);
      e.printStackTrace();
    }
  }

  private int getTypeFor(Object newVal) {
    return (newVal instanceof String ? DATA_TYPE_STRING
            : AU.isAD(newVal) ? DATA_TYPE_AD
                : AU.isADD(newVal) ? DATA_TYPE_ADD
                    : AU.isADDD(newVal) ? DATA_TYPE_ADDD : DATA_TYPE_UNKNOWN);
  }

  private void setVDW(Object[] data) {
    String stringData = ((String) data[DATA_VALUE]).trim();
    if (stringData.length() == 0) {
      vwr.userVdwMars = null;
      vwr.userVdws = null;
      vwr.bsUserVdws = null;
    } else {
      if (vwr.bsUserVdws == null)
        vwr.setUserVdw(vwr.defaultVdw);
      Parser.parseDoubleArrayFromMatchAndField(stringData, vwr.bsUserVdws, 1, 0,
          (int[]) data[DATA_SELECTION], 2, 0, vwr.userVdws, 1);
      for (int i = vwr.userVdws.length; --i >= 0;)
        vwr.userVdwMars[i] = (int) Math.floor(vwr.userVdws[i] * 1000);
    }
    return;
  }

  private static void fillSparseArray(double[] doubleData, BS bs, double[] f) {
    for (int i = bs.nextSetBit(0), pt = 0; i >= 0; i = bs
        .nextSetBit(i + 1), pt++)
      f[i] = doubleData[pt];
  }

  private static int getType(Object[] data) {
    return (data == null ? 0 : ((Integer) data[DATA_TYPE]).intValue());
  }

  private static void setSelected(double f, BS bs, double[] data) {
    boolean isAll = (bs == null);
    int i0 = (isAll ? 0 : bs.nextSetBit(0));
    for (int i = i0; i >= 0
        && i < data.length; i = (isAll ? i + 1 : bs.nextSetBit(i + 1)))
      data[i] = f;
  }

  @Override
  public Object getData(String label, BS bsSelected, int dataType) {
    if (label == null)
      return null;
    // wildcard xxx* finds all data of this type are returns a list
    if (label.endsWith("*")) {
      Lst<String> list = new Lst<String>();
      label = label.substring(0, label.length() - 1);
      int len = label.length();
      for (String key : dataValues.keySet()) {
        if (len == 0 || key.length() >= len
            && key.substring(0, len).equalsIgnoreCase(label)) {
          list.addLast(key);
        }
      }
      return list;
    }
    if (dataValues.size() == 0)
      return null;
    label = label.toLowerCase();
    switch (dataType) {
    case DATA_TYPE_UNKNOWN:
    case DATA_TYPE_LAST:
      if (!label.equals("types"))
        return dataValues.get(label);
      String[] info = new String[2];
      info[DATA_LABEL] = "types";
      info[DATA_VALUE] = "";
      int nv = 0;
      for (String name : dataValues.keySet())
        info[DATA_VALUE] += (nv++ > 0 ? "\n" : "") + name;
      return info;
    }
    Object[] data = dataValues.get(label);
    int oldType = getType(data);
    if (oldType != dataType) {
      return null;
    }
    Object oldData = data[DATA_VALUE];
    if (bsSelected == null && oldData != null) {
      return oldData;
    }
    switch (oldType) {
    case DATA_TYPE_ADDD:
      double[][][] ddd = (double[][][]) oldData;
      double[][][] dddnew = AU.newDouble3(bsSelected.cardinality(), 0);
      // load array
      for (int i = 0, n = ddd.length, p = bsSelected.nextSetBit(0); p >= 0
          && i < n; p = bsSelected.nextSetBit(p + 1)) {
        dddnew[i++] = ddd[p];
      }
      return dddnew;
    case DATA_TYPE_ADD:
      double[][] dd = (double[][]) oldData;
      double[][] ddnew = AU.newDouble2(bsSelected.cardinality());
      // load array
      for (int i = 0, n = dd.length, p = bsSelected.nextSetBit(0); p >= 0
          && i < n; p = bsSelected.nextSetBit(p + 1))
        ddnew[i++] = dd[p];
      return ddnew;
    case DATA_TYPE_AD:
      // standard double[]
      double[] d = (double[]) oldData;
      double[] dnew = new double[bsSelected.cardinality()];
      // load array
      for (int i = 0, n = d.length, p = bsSelected.nextSetBit(0); p >= 0
          && i < n; p = bsSelected.nextSetBit(p + 1))
        dnew[i++] = d[p];
      return dnew;
    }
    return null;
  }

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
    for (int i = i0; i < i1
        && i >= 0; i = (isAll ? i + 1 : bs.nextSetBit(i + 1)))
      sb.appendI(i).appendC('\t')
          .appendF(type == VDW.USER ? vwr.userVdws[i]
              : Elements.getVanderwaalsMar(i, type) / 1000d)
          .appendC('\t').append(Elements.elementSymbolFromNumber(i))
          .appendC('\n');
    return (bs == null ? sb.toString()
        : "\n  DATA \"element_vdw\"\n"
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
        if (obj.length > DATA_SAVE_IN_STATE
            && !((Boolean) obj[DATA_SAVE_IN_STATE])
                .booleanValue())
          continue;
        int type = getType(obj);
        haveData = true;
        Object data = obj[DATA_VALUE];
        if (data != null && type == DATA_TYPE_AD) {
          sc.getAtomicPropertyStateBufferD(sb, AtomCollection.TAINT_MAX,
              (BS) obj[DATA_SELECTION], name, (double[]) data);
          sb.append("\n");
        } else{
          // should not be here -- property_xxx is always double[]
          sb.append("\n").append(Escape.encapsulateData(name, data,
              DATA_TYPE_UNKNOWN));//j2s issue?
        }
        continue;
      }
      int type = (name.indexOf("data2d") == 0 ? DATA_TYPE_ADD
          : name.indexOf("data3d") == 0 ? DATA_TYPE_ADDD
              : DATA_TYPE_UNKNOWN);
      if (type == DATA_TYPE_UNKNOWN)
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
    o[DATA_LABEL] = "model";
    o[DATA_VALUE] = strModel;
    o[DATA_TYPE] = Integer.valueOf(DATA_TYPE_STRING);
    return o;
  }

}
