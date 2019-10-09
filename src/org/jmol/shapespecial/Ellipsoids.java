/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-05-22 07:48:05 -0500 (Tue, 22 May 2007) $
 * $Revision: 7806 $

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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.shapespecial;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.jmol.api.Interface;
import org.jmol.c.PAL;
import org.jmol.modelset.Atom;
import org.jmol.shape.AtomShape;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Escape;
import org.jmol.util.Tensor;
import org.jmol.viewer.ActionManager;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.P3i;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.V3;

public class Ellipsoids extends AtomShape {

  @Override
  public boolean checkObjectHovered(int x, int y, BS bsModels) {
    if (!vwr.getDrawHover() || simpleEllipsoids == null || simpleEllipsoids.isEmpty())
      return false;
    Ellipsoid e = findPickedObject(x, y, false, bsModels);
    if (e == null)
      return false;
    if (vwr.gdata.antialiasEnabled) {
      //because hover rendering is done in FIRST pass only
      x <<= 1;
      y <<= 1;
    }      
    vwr.hoverOnPt(x, y, e.label, e.id, e.center);
    return true;
  }

  private final static int MAX_OBJECT_CLICK_DISTANCE_SQUARED = 10 * 10;

  private final P3i ptXY = new P3i();
  
  @Override
  public Map<String, Object> checkObjectClicked(int x, int y, int action,
                                                BS bsModels,
                                                boolean drawPicking) {
    if (action == 0 || !drawPicking || simpleEllipsoids == null
        || simpleEllipsoids.isEmpty())
      return null;
    Ellipsoid e = findPickedObject(x, y, false, bsModels);
    if (e == null)
      return null;
    Map<String, Object> map = null;
    map = new Hashtable<String, Object>();
    map.put("id", e.id);
    if (e.label != null)
      map.put("label", e.label);
    map.put("pt", e.center);
    map.put("modelIndex", Integer.valueOf(e.modelIndex));
    map.put("model", vwr.getModelNumberDotted(e.modelIndex));
    map.put("type", "ellipsoid");
    if (action != 0) // not mouseMove
      vwr.setStatusAtomPicked(-2, "[\"ellipsoid\"," + PT.esc(e.id) + "," +
          + e.modelIndex + ",1," + e.center.x + "," + e.center.y + "," + e.center.z + "," 
          + (e.label == null ? "\"\"" 
                 : PT.esc(e.label))+"]", map, false);
    return map;
  }
  /**
   * 
   * @param x
   * @param y
   * @param isPicking
   *        IGNORED
   * @param bsModels
   * @return true if found
   */
  private Ellipsoid findPickedObject(int x, int y, boolean isPicking,
                                     BS bsModels) {
    int dmin2 = MAX_OBJECT_CLICK_DISTANCE_SQUARED;
    if (vwr.gdata.isAntialiased()) {
      x <<= 1;
      y <<= 1;
      dmin2 <<= 1;
    }
    Ellipsoid picked = null;
    for (String id: simpleEllipsoids.keySet()) {
      Ellipsoid e = simpleEllipsoids.get(id);
      if (!e.visible || !bsModels.get(e.modelIndex))
        continue;
      int d2 = coordinateInRange(x, y, e.center, dmin2, ptXY);
      if (d2 >= 0) {
        dmin2 = d2;
        picked = e;
      }
    }
    return picked;
  }

  private static final String PROPERTY_MODES = "ax ce co de eq mo on op sc tr la";

  public Map<String, Ellipsoid> simpleEllipsoids = new Hashtable<String, Ellipsoid>();
  public Map<Tensor, Ellipsoid> atomEllipsoids = new Hashtable<Tensor, Ellipsoid>();

  public boolean isActive() {
    return !atomEllipsoids.isEmpty() || !simpleEllipsoids.isEmpty();
  }

  private String typeSelected = "1";
  private BS selectedAtoms;
  private Lst<Ellipsoid> ellipsoidSet;

  @Override
  public int getIndexFromName(String thisID) {
    return (checkID(thisID) ? 1 : -1);
  }

  @Override
  protected void setSize(int size, BS bsSelected) {
    if (atoms == null || size == 0 && ms.atomTensors == null)
      return;
    boolean isAll = (bsSelected == null);
    if (!isAll && selectedAtoms != null)
      bsSelected = selectedAtoms;
    Lst<Object> tensors = vwr.ms.getAllAtomTensors(typeSelected);
    if (tensors == null)
      return;
    for (int i = tensors.size(); --i >= 0;) {
      Tensor t = (Tensor) tensors.get(i);
      if (isAll || t.isSelected(bsSelected, -1)) {
        Ellipsoid e = atomEllipsoids.get(t);
        boolean isNew = (size != 0 && e == null);
        if (isNew)
          atomEllipsoids.put(t,
              e = Ellipsoid.getEllipsoidForAtomTensor(t, atoms[t.atomIndex1]));
        if (e != null) {// && (isNew || size != Integer.MAX_VALUE)) { // MAX_VALUE --> "create only"
          e.setScale(size, true);
        }
      }
    }
  }

  //  @SuppressWarnings("unchecked")
  //  @Override
  //  public boolean getPropertyData(String property, Object[] data) {
  //    if (property == "quadric") {
  //      Tensor t = (Tensor) data[0];
  //      if (t == null)
  //        return false;
  //      Ellipsoid e = atomEllipsoids.get(t);
  //      if (e == null) {
  //        P3 center = (P3) data[1];
  //        if (center == null)
  //          center = new P3();
  //        e = Ellipsoid.getEllipsoidForAtomTensor(t, center);
  //      }
  //      data[2] = e.getEquation();
  //      return true;
  //    }
  //    return false;
  //  }

  @Override
  public boolean getPropertyData(String property, Object[] data) {
    if (property == "checkID") {
      return (checkID((String) data[0]));
    }
    return getPropShape(property, data);
  }

  private boolean checkID(String thisID) {
    ellipsoidSet = new Lst<Ellipsoid>();
    if (thisID == null)
      return false;
    thisID = thisID.toLowerCase();
    if (PT.isWild(thisID)) {
      for (Entry<String, Ellipsoid> e : simpleEllipsoids.entrySet()) {
        String key = e.getKey().toLowerCase();
        if (PT.isMatch(key, thisID, true, true))
          ellipsoidSet.addLast(e.getValue());
      }
    }
    Ellipsoid e = simpleEllipsoids.get(thisID);
    if (e != null)
      ellipsoidSet.addLast(e);
    return (ellipsoidSet.size() > 0);
  }

  private boolean initEllipsoids(Object value) {
    boolean haveID = (value != null);
    checkID((String) value);
    if (haveID)
      typeSelected = null;
    selectedAtoms = null;
    return haveID;
  }

  @Override
  public void initShape() {
    setProperty("thisID", null, null);
  }

  @Override
  public void setProperty(String propertyName, Object value, BS bs) {
    //System.out.println(propertyName + " " + value + " " + bs);
    if (propertyName == "thisID") {
      if (initEllipsoids(value) && ellipsoidSet.size() == 0) {
        String id = (String) value;
        Ellipsoid e = Ellipsoid.getEmptyEllipsoid(id, vwr.am.cmi);
        ellipsoidSet.addLast(e);
        simpleEllipsoids.put(id, e);
      }
      return;
    }

    if ("atoms" == propertyName) {
      selectedAtoms = (BS) value;
      return;
    }

    if (propertyName == "deleteModelAtoms") {
      int modelIndex = ((int[]) ((Object[]) value)[2])[0];
      Iterator<Ellipsoid> e = simpleEllipsoids.values().iterator();
      while (e.hasNext())
        if (e.next().tensor.modelIndex == modelIndex)
          e.remove();
      e = atomEllipsoids.values().iterator();
      while (e.hasNext())
        if (e.next().modelIndex == modelIndex)
          e.remove();
      ellipsoidSet.clear();
      return;
    }
    int mode = PROPERTY_MODES.indexOf((propertyName + "  ").substring(0, 2));
    if (ellipsoidSet.size() > 0) {
      if ("translucentLevel" == propertyName) {
        setPropS(propertyName, value, bs);
        return;
      }
      if (mode >= 0)
        for (int i = ellipsoidSet.size(); --i >= 0;)
          setProp(ellipsoidSet.get(i), mode / 3, value);
      return;
    }

    if ("color" == propertyName) {
      short colix = C.getColixO(value);
      byte pid = PAL.pidOf(value);
      if (selectedAtoms != null)
        bs = selectedAtoms;
      for (Ellipsoid e : atomEllipsoids.values())
        if (e.tensor.type.equals(typeSelected) && e.tensor.isSelected(bs, -1)) {
          e.colix = getColixI(colix, pid, e.tensor.atomIndex1);
          e.pid = pid;
        }
      return;
    }

    if ("on" == propertyName) {
      boolean isOn = ((Boolean) value).booleanValue();
      if (selectedAtoms != null)
        bs = selectedAtoms;
      if (isOn)
        setSize(Integer.MAX_VALUE, bs);
      for (Ellipsoid e : atomEllipsoids.values()) {
        Tensor t = e.tensor;
        if ((t.type.equals(typeSelected) || typeSelected.equals(t.altType))
            && t.isSelected(bs, -1)) {
          e.isOn = isOn;
        }
        ((Atom) e.center).setShapeVisibility(vf, isOn);
      }
      return;
    }

    if ("options" == propertyName) {
      String options = ((String) value).toLowerCase().trim();
      if (options.length() == 0)
        options = null;
      if (selectedAtoms != null)
        bs = selectedAtoms;
      if (options != null)
        setSize(Integer.MAX_VALUE, bs);
      for (Ellipsoid e : atomEllipsoids.values())
        if (e.tensor.type.equals(typeSelected) && e.tensor.isSelected(bs, -1))
          e.options = options;
      return;
    }

    if ("params" == propertyName) {
      Object[] data = (Object[]) value;
      data[2] = null;// Jmol does not allow setting sizes this way from PyMOL yet
      typeSelected = "0";
      setSize(50, bs);
      // onward...
    }

    if ("points" == propertyName) {
      //Object[] o = (Object[]) value;
      //setPoints((P3[]) o[1], (BS) o[2]);
      return;
    }

    if ("scale" == propertyName) {
      setSize((int) (((Float) value).floatValue() * 100), bs);
      return;
    }

    if ("select" == propertyName) {
      typeSelected = ((String) value).toLowerCase();
      return;
    }

    if ("translucency" == propertyName) {
      boolean isTranslucent = (value.equals("translucent"));
      for (Ellipsoid e : atomEllipsoids.values())
        if (e.tensor.type.equals(typeSelected) && e.tensor.isSelected(bs, -1))
          e.colix = C.getColixTranslucent3(e.colix, isTranslucent,
              translucentLevel);
      return;
    }
    setPropS(propertyName, value, bs);
  }

  //  private void setPoints(P3[] points, BS bs) {
  //    return;
  // doesn't really work. Just something I was playing with.
  //    if (points == null)
  //      return;
  //    int n = bs.cardinality();
  //    if (n < 3)
  //      return;
  //    P3 ptCenter = new P3();
  //    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
  //      ptCenter.add(points[i]);
  //    ptCenter.scale(1.0f/n);
  //    double Sxx = 0, Syy = 0, Szz = 0, Sxy = 0, Sxz = 0, Syz = 0;
  //    P3 pt = new P3();
  //    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
  //      pt.setT(points[i]);
  //      pt.sub(ptCenter);
  //      Sxx += (double) pt.x * (double) pt.x;
  //      Sxy += (double) pt.x * (double) pt.y;
  //      Sxz += (double) pt.x * (double) pt.z;
  //      Syy += (double) pt.y * (double) pt.y;
  //      Szz += (double) pt.z * (double) pt.z;
  //      Syz += (double) pt.y * (double) pt.z;      
  //    }
  //    double[][] N = new double[3][3];
  //    N[0][0] = Syy + Szz;
  //    N[1][1] = Sxx + Szz;
  //    N[2][2] = Sxx + Syy;
  //    Eigen eigen = Eigen.newM(N);
  //    ellipsoid.setEigen(ptCenter, eigen, 1f / n / 3);
  //  }

  private void setProp(Ellipsoid e, int mode, Object value) {
    // "ax ce co de eq mo on op sc tr la"
    //  0  1  2  3  4  5  6  7  8  9  10
    switch (mode) {
    case 0: // axes
      e.setTensor(((Tensor) Interface.getUtil("Tensor", vwr, "script"))
          .setFromAxes((V3[]) value));
      break;
    case 1: // center
      e.setCenter((P3) value);
      break;
    case 2: // color
      e.colix = C.getColixO(value);
      break;
    case 3: // delete
      simpleEllipsoids.remove(e.id);
      break;
    case 4: // equation
      e.setTensor(((Tensor) Interface.getUtil("Tensor", vwr, "script"))
          .setFromThermalEquation((double[]) value, null));
      e.tensor.modelIndex = e.modelIndex;
      break;
    case 5: // modelindex
      e.modelIndex = ((Integer) value).intValue();
      if (e.tensor != null)
        e.tensor.modelIndex = e.modelIndex;
      break;
    case 6: // on
      e.isOn = ((Boolean) value).booleanValue();
      break;
    case 7: // options
      e.options = ((String) value).toLowerCase();
      break;
    case 8: // scale
      if (value instanceof Float) {
        e.setScale(((Float) value).floatValue(), false);
      } else {
        e.scaleAxes((float[]) value);
      }
      break;
    case 9: // translucency
      e.colix = C.getColixTranslucent3(e.colix, value.equals("translucent"),
          translucentLevel);
      break;
    case 10:
      e.label = (String) value;
      break;
    }
    return;
  }

  @Override
  public String getShapeState() {
    if (!isActive())
      return "";
    SB sb = new SB();
    sb.append("\n");
    if (!simpleEllipsoids.isEmpty())
      getStateID(sb);
    if (!atomEllipsoids.isEmpty())
      getStateAtoms(sb);
    return sb.toString();
  }

  private void getStateID(SB sb) {
    V3 v1 = new V3();
    for (Ellipsoid ellipsoid : simpleEllipsoids.values()) {
      Tensor t = ellipsoid.tensor;
      if (!ellipsoid.isValid || t == null)
        continue;
      sb.append("  Ellipsoid ID ").append(ellipsoid.id).append(" modelIndex ")
          .appendI(t.modelIndex).append(" center ")
          .append(Escape.eP(ellipsoid.center)).append(" axes");
      for (int i = 0; i < 3; i++) {
        v1.setT(t.eigenVectors[i]);
        v1.scale(ellipsoid.lengths[i]);
        sb.append(" ").append(Escape.eP(v1));
      }
      sb.append(" "
          + getColorCommandUnk("", ellipsoid.colix, translucentAllowed));
      if (ellipsoid.label != null)
        sb.append(" label " + PT.esc(ellipsoid.label));
      if (ellipsoid.options != null)
        sb.append(" options ").append(PT.esc(ellipsoid.options));
      if (!ellipsoid.isOn)
        sb.append(" off");
      sb.append(";\n");
    }
  }

  private void getStateAtoms(SB sb) {
    BS bsDone = new BS();
    Map<String, BS> temp = new Hashtable<String, BS>();
    Map<String, BS> temp2 = new Hashtable<String, BS>();
    for (Ellipsoid e : atomEllipsoids.values()) {
      int iType = e.tensor.iType;
      if (bsDone.get(iType + 1))
        continue;
      bsDone.set(iType + 1);
      boolean isADP = (e.tensor.iType == Tensor.TYPE_ADP);
      String cmd = (isADP ? null : "Ellipsoids set " + PT.esc(e.tensor.type));
      for (Ellipsoid e2 : atomEllipsoids.values()) {
        if (e2.tensor.iType != iType || isADP && !e2.isOn)
          continue;
        int i = e2.tensor.atomIndex1;
        // 
        BSUtil.setMapBitSet(temp, i, i, (isADP ? "Ellipsoids " + e2.percent
            : cmd + " scale " + e2.scale
                + (e2.options == null ? "" : " options " + PT.esc(e2.options))
                + (e2.isOn ? " ON" : " OFF")));
        if (e2.colix != C.INHERIT_ALL)
          BSUtil.setMapBitSet(temp2, i, i,
              getColorCommand(cmd, e2.pid, e2.colix, translucentAllowed));
      }
    }
    sb.append(vwr.getCommands(temp, temp2, "select"));
  }

  @Override
  public void setModelVisibilityFlags(BS bsModels) {
    /*
     * set all fixed objects visible; others based on model being displayed
     *      
     */
    if (!isActive())
      return;
    setVis(simpleEllipsoids, bsModels, atoms);
    setVis(atomEllipsoids, bsModels, atoms);
  }

  private void setVis(Map<?, Ellipsoid> ellipsoids, BS bs, Atom[] atoms) {
    for (Ellipsoid e : ellipsoids.values()) {
      Tensor t = e.tensor;
      boolean isOK = (t != null && e.isValid && e.isOn);
      if (isOK && t.atomIndex1 >= 0) {
        if (t.iType == Tensor.TYPE_ADP) {
          boolean isModTensor = t.isModulated;
          boolean isUnmodTensor = t.isUnmodulated;
          boolean isModAtom = ms.isModulated(t.atomIndex1);
          isOK = (!isModTensor && !isUnmodTensor || isModTensor == isModAtom);
        }
        atoms[t.atomIndex1].setShapeVisibility(vf, true);
      }
      e.visible = isOK && (e.modelIndex < 0 || bs.get(e.modelIndex));
    }
  }

  @Override
  public void setAtomClickability() {
    if (atomEllipsoids.isEmpty())
      return;
    for (Ellipsoid e : atomEllipsoids.values()) {
      int i = e.tensor.atomIndex1;
      Atom atom = atoms[i];
      if ((atom.shapeVisibilityFlags & vf) == 0 || ms.isAtomHidden(i))
        continue;
      atom.setClickable(vf);
    }
  }

}
