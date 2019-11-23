/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2019-08-18 15:32:43 -0500 (Sun, 18 Aug 2019) $
 * $Revision: 21995 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

package org.jmol.shape;

import org.jmol.api.JmolMeasurementClient;
import org.jmol.atomdata.RadiusData;

import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Measurement;
import org.jmol.modelset.MeasurementData;
import org.jmol.modelset.MeasurementPending;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Escape;
import org.jmol.util.Font;
import org.jmol.util.Point3fi;
import org.jmol.modelset.TickInfo;
import org.jmol.viewer.JC;
import org.jmol.script.T;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;


import java.util.Hashtable;
import java.util.Map;


public class Measures extends AtomShape implements JmolMeasurementClient {

  private BS bsSelected;
  private String strFormat;
  private boolean mustBeConnected = false;
  private boolean mustNotBeConnected = false;
  private RadiusData radiusData;
  private Boolean intramolecular;
  private boolean measureAllModels;

  public int measurementCount = 0;
  public final Lst<Measurement> measurements = new  Lst<Measurement>();
  public MeasurementPending mPending;
  
  public short colix; // default to none in order to contrast with background
  
  TickInfo tickInfo;
  public TickInfo defaultTickInfo;
  public static Font font3d;
  private Map<String, Integer> htMin;
  
  @Override
  protected void initModelSet() {
    for (int i = measurements.size(); --i >= 0; ) {
      Measurement m = measurements.get(i);
      if (m != null)
        m.ms = ms;
    }
    atoms = ms.at;
  }
  
  @Override
  public void initShape() {
    if (font3d == null)
      font3d = vwr.gdata.getFont3D(JC.MEASURE_DEFAULT_FONTSIZE);
  }

  @Override
  protected void setSize(int size, BS bsSelected) {
    mad = (short) size;
  }

  @Override
  public void setProperty(String propertyName, Object value, BS bsIgnored) {
    // the following can be used with "select measures ({bitset})"

    Measurement mt;
    if ("clearModelIndex" == propertyName) {
      for (int i = 0; i < measurementCount; i++)
        measurements.get(i).setModelIndex((short) 0);
      return;
    }

    if ("color" == propertyName) {
      setColor(C.getColixO(value));
      return;
    }

    if ("font" == propertyName) {
      font3d = (Font) value;
      return;
    }

    if ("hideAll" == propertyName) {
      showHide(((Boolean) value).booleanValue());
      return;
    }

    if ("pending" == propertyName) {
      this.mPending = (MeasurementPending) value;
      if (mPending == null)
        return;
      if (mPending.count > 1)
        vwr.setStatusMeasuring("measurePending", mPending
            .count, getMessage(mPending, false),
            mPending.value);
      return;
    }

    boolean isRefresh;
    if ((isRefresh = ("refresh" == propertyName))
        || "refreshTrajectories" == propertyName) {
      for (int i = measurements.size(); --i >= 0;)
        if ((mt = measurements.get(i)) != null
            && (isRefresh || mt.isTrajectory))
          mt.refresh(null);
      return;
    }

    if ("select" == propertyName) {
      BS bs = (BS) value;
      if (BSUtil.cardinalityOf(bs) == 0) {
        bsSelected = null;
      } else {
        bsSelected = new BS();
        bsSelected.or(bs);
      }
      return;
    }

    if ("setFormats" == propertyName) {
      setFormats((String) value);
      return;
    }

    // most of the following need defineAll, which needs measureAllModels
    measureAllModels = vwr.getBoolean(T.measureallmodels);

    if ("delete" == propertyName) {
      deleteO(value);
      setIndices();
      return;
    }

    //any one of the following clears the "select measures" business

    bsSelected = null;

    if ("maps" == propertyName) {
      int[][] maps = (int[][]) value;
      for (int i = 0; i < maps.length; i++) {
        int len = maps[i].length;
        if (len < 2 || len > 4)
          continue;
        int[] v = new int[len + 1];
        v[0] = len;
        System.arraycopy(maps[i], 0, v, 1, len);
        toggleOn(v);
      }
    } else if ("measure" == propertyName) {
      MeasurementData md = (MeasurementData) value;
      tickInfo = md.tickInfo;
      if (md.tickInfo != null && md.tickInfo.id.equals("default")) {
        defaultTickInfo = md.tickInfo;
        return;
      }
      if (md.isAll && md.points.size() == 2 && md.points.get(0) instanceof BS) {
        int type = Measurement.nmrType(vwr.getDistanceUnits(md.strFormat));
        switch (type) {
          case Measurement.NMR_JC:
          //case Measurement.NMR_DC:
          md.htMin = vwr.getNMRCalculation().getMinDistances(md);
        }
      }
      tickInfo = md.tickInfo;
      radiusData = md.radiusData;
      htMin = md.htMin;
      mustBeConnected = md.mustBeConnected;
      mustNotBeConnected = md.mustNotBeConnected;
      intramolecular = md.intramolecular;
      strFormat = md.strFormat;
      if (md.isAll) {
        if (tickInfo != null)
          define(md, T.delete);
        define(md, md.tokAction);
        setIndices();
        return;
      }
      Measurement m = setSingleItem(md.points);
      if (md.thisID != null) {
        m.thisID = md.thisID;
        m.mad = md.mad;
        if (md.colix != 0)
          m.colix = md.colix;
        m.strFormat = md.strFormat;
        m.text = md.text;
      }
      switch (md.tokAction) {
      case T.refresh:
        doAction(md, md.thisID, T.refresh);
        break;
      case T.delete:
        defineAll(Integer.MIN_VALUE, m, true, false, false);
        setIndices();
        break;
      case T.on:
        showHideM(m, false);
        break;
      case T.off:
        showHideM(m, true);
        break;
      case T.radius:
        if (md.thisID != null)
          doAction(md, md.thisID, T.radius);
        break;
      case T.define:
        if (md.thisID == null) {
          deleteM(m);
        } else {
          deleteO(md.thisID);
        }
        toggle(m);
        break;
      case T.opToggle:
        toggle(m);
      }
      return;
    }

    if ("clear" == propertyName) {
      clear();
      return;
    }

    if ("deleteModelAtoms" == propertyName) {
      atoms = (Atom[]) ((Object[]) value)[1];
      int modelIndex = ((int[]) ((Object[]) value)[2])[0];
      int firstAtomDeleted = ((int[]) ((Object[]) value)[2])[1];
      int nAtomsDeleted = ((int[]) ((Object[]) value)[2])[2];
      int atomMax = firstAtomDeleted + nAtomsDeleted;
      for (int i = measurementCount; --i >= 0;) {
        mt = measurements.get(i);
        int[] indices = mt.countPlusIndices;
        for (int j = 1; j <= indices[0]; j++) {
          int iAtom = indices[j];
          if (iAtom >= firstAtomDeleted) {
            if (iAtom < atomMax) {
              deleteI(i);
              break;
            }
            indices[j] -= nAtomsDeleted;
          } else if (iAtom < 0) {
            Point3fi pt = mt.getAtom(j);
            if (pt.mi > modelIndex) {
              pt.mi--;
            } else if (pt.mi == modelIndex) {
              deleteI(i);
              break;
            }
          }
        }
      }
      return;
    }

    if ("reformatDistances" == propertyName) {
      reformatDistances();
      return;
    }

    if ("hide" == propertyName) {
      if (value instanceof String) {
        doAction(null, (String) value, T.hide);
      } else {
        showHideM(new Measurement().setPoints(ms, (int[]) value, null,
            null), true);
      }
      return;
    }

    if ("refresh" == propertyName) {
      doAction((MeasurementData) value, null, T.refresh);
      return;
    }
    if ("show" == propertyName) {
      if (value instanceof String) {
        doAction(null, (String) value, T.show);
      } else {
        showHideM(new Measurement().setPoints(ms, (int[]) value, null,
            null), false);
      }
      return;
    }

    if ("toggle" == propertyName) {
      if (value instanceof String) {
        doAction(null, (String) value, T.opToggle);
      } else {
        toggle(new Measurement().setPoints(ms, (int[]) value, null, null));
      }
      return;
    }

    if ("toggleOn" == propertyName) {
      if (value instanceof String) {
        doAction(null, (String) value, T.on);
      } else {
        toggleOn((int[]) value);
      }
      return;
    }
  }

  private Measurement setSingleItem(Lst<Object> vector) {
    Point3fi[] points = new Point3fi[4];
    int[] indices = new int[5];
    indices[0] = vector.size();
    for (int i = vector.size(); --i >= 0; ) {
      Object value = vector.get(i);
      if (value instanceof BS) {
        int atomIndex = ((BS) value).nextSetBit(0);
        if (atomIndex < 0)
          return null;
        indices[i + 1] = atomIndex;
      } else {
        points[i] = (Point3fi) value;
        indices[i + 1] = -2 - i;
      }
    }
    return new Measurement().setPoints(ms, indices, points, tickInfo == null ? defaultTickInfo : tickInfo);
  }

  @Override
  public Object getProperty(String property, int index) {
    if ("pending".equals(property))
      return mPending;
    if ("count".equals(property))
      return Integer.valueOf(measurementCount);
    if ("countPlusIndices".equals(property))
      return (index < measurementCount ? 
          measurements.get(index).countPlusIndices : null);
    if ("stringValue".equals(property))
      return (index < measurementCount ? measurements.get(index).getString() : null);
    if ("pointInfo".equals(property))
      return measurements.get(index / 10).getLabel(index % 10, false, false);
    if ("info".equals(property))
      return getAllInfo();
    if ("infostring".equals(property))
      return getAllInfoAsString();
    return null;
  }

  public void clear() {
    if (measurementCount == 0)
      return;
    measurementCount = 0;
    measurements.clear();
    mPending = null;
    vwr.setStatusMeasuring("measureDeleted", -1, "all", 0);
  }

  private void setColor(short colix) {
    if (bsColixSet == null)
      bsColixSet = new BS();
      if (bsSelected == null)
        this.colix = colix;
    Measurement mt;
    for (int i = measurements.size(); --i >= 0; )
      if ((mt = measurements.get(i)) != null
          && (bsSelected != null && bsSelected.get(i) || bsSelected == null
              && (colix == C.INHERIT_ALL || mt.colix == C.INHERIT_ALL))) {
        mt.colix = colix;
        bsColixSet.set(i);
      }
  }

  private void setFormats(String format) {
    if (format != null && format.length() == 0)
      format = null;
    for (int i = measurements.size(); --i >= 0;)
      if (bsSelected == null || bsSelected.get(i))
        measurements.get(i).formatMeasurementAs(format, null, false);
  }
  
  private void showHide(boolean isHide) {
    for (int i = measurements.size(); --i >= 0;)
      if (bsSelected == null || bsSelected.get(i))
        measurements.get(i).isHidden = isHide;
  }

  private void showHideM(Measurement m, boolean isHide) {
    int i = find(m);
    if (i >= 0)
      measurements.get(i).isHidden = isHide;
  }
  
  private void toggle(Measurement m) {
    radiusData = null;
    htMin = null;
    //toggling one that is hidden should be interpreted as DEFINE
    int i = find(m);
    Measurement mt;
    if (i >= 0 && !(mt = measurements.get(i)).isHidden) // delete it and all like it
      defineAll(i, mt, true, false, false);
    else // define OR turn on if measureAllModels
      defineAll(-1, m, false, true, false);
    setIndices();
  }

  private void toggleOn(int[] indices) {
    radiusData = null;
    htMin = null;
    //toggling one that is hidden should be interpreted as DEFINE
    bsSelected = new BS();
    Measurement m = new Measurement().setPoints(ms, indices, null, defaultTickInfo);
    defineAll(Integer.MIN_VALUE, m, false, true, true);
    int i = find(m);
    if (i >= 0)
      bsSelected.set(i);
    setIndices();
    reformatDistances();
  }

  private void deleteM(Measurement m) {
    radiusData = null;
    htMin = null;
    //toggling one that is hidden should be interpreted as DEFINE
    int i = find(m);
    if (i >= 0)
      defineAll(i, measurements.get(i), true, false, false);
    setIndices();
  }

  private void deleteO(Object value) {
    if (value instanceof Integer) {
      deleteI(((Integer)value).intValue());
    } else if (value instanceof String) {
      doAction(null, (String) value, T.delete);
    } else if (AU.isAI(value)) {
      defineAll(Integer.MIN_VALUE, new Measurement().setPoints(ms, (int[])value, null, null), true, false, false);
    }
  }

  private void defineAll(int iPt, Measurement m, boolean isDelete,
                         boolean isShow, boolean doSelect) {
    if (!measureAllModels) {
      if (isDelete) {
        if (iPt == Integer.MIN_VALUE)
          iPt = find(m);
        if (iPt >= 0)
          deleteI(iPt);
        return;
      }
      defineMeasurement(iPt, m, doSelect);
      return;
    }
    if (isShow) { // make sure all like this are deleted, not just hidden
      defineAll(iPt, m, true, false, false); // self-reference
      if (isDelete)
        return;
    }
    // we create a set of atoms involving all atoms with the
    // same atom number in each model
    Lst<Object> points = new  Lst<Object>();
    int nPoints = m.count;
    for (int i = 1; i <= nPoints; i++) {
      int atomIndex = m.getAtomIndex(i);
      points.addLast(atomIndex >= 0 ? (Object) vwr.ms.getAtoms(T.atomno,
          Integer.valueOf(atoms[atomIndex].getAtomNumber())) : (Object) m
          .getAtom(i));
    }
    define((new MeasurementData().init(null, vwr, points)).set(tokAction, htMin, radiusData, strFormat, null, tickInfo,
        mustBeConnected, mustNotBeConnected, intramolecular, true, 0, (short) 0, null),
        (isDelete ? T.delete : T.define));
  }

  private int find(Measurement m) {
    return (m.thisID == null ? Measurement.find(measurements, m) : -1);
  }

  private void setIndices() {
    for (int i = 0; i < measurementCount; i++)
      measurements.get(i).index = i;
  }
  
  private int tokAction;
  
  private void define(MeasurementData md, int tokAction) {
    this.tokAction = tokAction;
    md.define(this, ms);
  }

  @Override
  public void processNextMeasure(Measurement m) {
    // a callback from Measurement.define
    // all atom bitsets have been iterated
    int iThis = find(m);
    if (iThis >= 0) {
      if (tokAction == T.delete) {
        deleteI(iThis);
      } else if (strFormat != null) {
        measurements.get(iThis).formatMeasurementAs(strFormat,
            null, true);
      } else {
        measurements.get(iThis).isHidden = (tokAction == T.off);
      }
    } else if (tokAction == T.define || tokAction == T.opToggle) {
      m.tickInfo = (tickInfo == null ? defaultTickInfo : tickInfo);
      defineMeasurement(-1, m, true);
    }
  }

  private void defineMeasurement(int i, Measurement m, boolean doSelect) {
    float value = m.getMeasurement(null);
    if (htMin != null && !m.isMin(htMin) 
        || radiusData != null && !m.isInRange(radiusData, value))
      return;
    if (i == Integer.MIN_VALUE)
      i = find(m);
    if (i >= 0) {
      measurements.get(i).isHidden = false;
      if (doSelect)
        bsSelected.set(i);
      return;
    }
    Measurement measureNew = new Measurement().setM(ms, m, value, (m.colix == 0 ? colix : m.colix),
        strFormat, measurementCount);
    if (!measureNew.isValid)
      return;
    measurements.addLast(measureNew);
    vwr.setStatusMeasuring("measureCompleted", measurementCount++,
        getMessage(measureNew, false), measureNew.value);
  }

  private static String getMessage(Measurement m, boolean asBitSet) {
    // only for callback messages
    SB sb = new SB();
    sb.append("[");
    for (int i = 1; i <= m.count; i++) {
      if (i > 1)
        sb.append(", ");
      sb.append(m.getLabel(i, asBitSet, false));
    }
    sb.append("]");
    return sb.toString();
  }

  private void deleteI(int i) {
    if (i >= measurements.size() || i < 0)
      return;
    String msg = getMessage(measurements.get(i), true);
    measurements.removeItemAt(i);
    measurementCount--;
    vwr.setStatusMeasuring("measureDeleted", i, msg, 0);
  }

  private void doAction(MeasurementData md, String id, int tok) {
    id = id.toUpperCase().replace('?', '*');
    boolean isWild = PT.isWild(id);
    for (int i = measurements.size(); --i >= 0;) {
      Measurement m = measurements.get(i);
      if (m.thisID != null && (m.thisID.equalsIgnoreCase(id)
          || isWild && PT.isMatch(m.thisID.toUpperCase(), id, true, true)))
        switch (tok) {
        case T.refresh:
          if (md.colix != 0)
            m.colix = md.colix;
          if (md.mad != 0)
            m.mad = md.mad;
          if (md.strFormat != null) {
            m.strFormat = m.strFormat.substring(0, 2)
                + md.strFormat.substring(2);
          }
          if (md.text != null) {
            if (m.text == null) {
              m.text = md.text;
            } else {
              if (md.text.font != null)
                m.text.font = md.text.font;
              m.text.text = null;
              if (md.text.align != 0)
                m.text.align = md.text.align;
              if (md.colix != 0)
                m.labelColix = m.text.colix = md.text.colix;
            }
          }
          m.formatMeasurement(null);
          break;
        case T.radius:
          m.mad = md.mad;
          break;
        case T.delete:
          String msg = getMessage(measurements.get(i), true);
          measurements.removeItemAt(i);
          measurementCount--;
          vwr.setStatusMeasuring("measureDeleted", i, msg, 0);
          break;
        case T.show:
          m.isHidden = false;
          break;
        case T.hide:
          m.isHidden = true;
          break;
        case T.opToggle:
          m.isHidden = !m.isHidden;
          break;
        case T.on:
          m.isHidden = false;
          break;
        }
    }
  }

  private void reformatDistances() {
    for (int i = measurementCount; --i >= 0; )
      measurements.get(i).reformatDistanceIfSelected();    
  }
  
  private Lst<Map<String, Object>> getAllInfo() {
    Lst<Map<String, Object>> info = new  Lst<Map<String,Object>>();
    for (int i = 0; i< measurementCount; i++) {
      info.addLast(getInfo(i));
    }
    return info;
  }
  
  private String getAllInfoAsString() {
    String info = "Measurement Information";
    for (int i = 0; i< measurementCount; i++) {
      info += "\n" + getInfoAsString(i);
    }
    return info;
  }
  
  private Map<String, Object> getInfo(int index) {
    Measurement m = measurements.get(index);
    int count = m.count;
    Map<String, Object> info = new Hashtable<String, Object>();
    info.put("index", Integer.valueOf(index));
    info.put("type", (count == 2 ? "distance" : count == 3 ? "angle"
        : "dihedral"));
    info.put("strMeasurement", m.getString());
    info.put("count", Integer.valueOf(count));
    info.put("id",  "" + m.thisID);
    info.put("value", Float.valueOf(m.value));
    info.put("hidden", Boolean.valueOf(m.isHidden));
    info.put("visible", Boolean.valueOf(m.isVisible));
    TickInfo tickInfo = m.tickInfo;
    if (tickInfo != null) {
      info.put("ticks", tickInfo.ticks);
      if (tickInfo.scale != null)
        info.put("tickScale", tickInfo.scale);
      if (tickInfo.tickLabelFormats != null)
        info.put("tickLabelFormats", tickInfo.tickLabelFormats);
      if (!Float.isNaN(tickInfo.first))
        info.put("tickStart", Float.valueOf(tickInfo.first));
    }
    Lst<Map<String, Object>> atomsInfo = new  Lst<Map<String,Object>>();
    for (int i = 1; i <= count; i++) {
      Map<String, Object> atomInfo = new Hashtable<String, Object>();
      int atomIndex = m.getAtomIndex(i);
      atomInfo.put("_ipt", Integer.valueOf(atomIndex));
      atomInfo.put("coord", Escape.eP(m.getAtom(i)));
      atomInfo.put("atomno", Integer.valueOf(atomIndex < 0 ? -1 : atoms[atomIndex].getAtomNumber()));
      atomInfo.put("info", (atomIndex < 0 ? "<point>" : atoms[atomIndex].getInfo()));
      atomsInfo.addLast(atomInfo);
    }
    info.put("atoms", atomsInfo);
    return info;
  }

  @Override
  public String getInfoAsString(int index) {
    return measurements.get(index).getInfoAsString(null);
  }
  
  public void setVisibilityInfo() {
    BS bsModels = vwr.getVisibleFramesBitSet();
    out:
    for (int i = measurementCount; --i >= 0; ) {
      Measurement m = measurements.get(i);
      m.isVisible = false;
      if(mad == 0 || m.isHidden)
        continue;
      for (int iAtom = m.count; iAtom > 0; iAtom--) {
        int atomIndex = m.getAtomIndex(iAtom);
        if (atomIndex >= 0) {
          if (!ms.at[atomIndex].isClickable())
            continue out;
        } else {
          int modelIndex = m.getAtom(iAtom).mi;
          if (modelIndex >= 0 && !bsModels.get(modelIndex))
            continue out;
        }
      }
      m.isVisible = true;
    }
  }
  
//  @Override
//  public String getShapeState() {
//    // see StateCreator
//    return null;
//  }
  
}
