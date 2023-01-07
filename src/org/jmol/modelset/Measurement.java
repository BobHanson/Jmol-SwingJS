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
package org.jmol.modelset;

import java.util.Map;

import org.jmol.api.JmolDataManager;
import org.jmol.atomdata.RadiusData;
import org.jmol.atomdata.RadiusData.EnumType;
import org.jmol.c.VDW;
import org.jmol.quantum.NMRCalculation;
import org.jmol.util.Escape;
import org.jmol.util.Point3fi;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.util.A4d;
import javajs.util.Lst;
import javajs.util.MeasureD;
import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.SB;

public class Measurement {

  /*
   * a class to contain a single measurement.
   * 
   */
  
  public String thisID;
  public ModelSet ms;
  public int index;
  public boolean isVisible = true;
  public boolean isHidden = false;
  public boolean isTrajectory = false;
  public boolean isValid = true;
  public short colix;
  public short labelColix = -1; // use colix
  public int mad;
  public TickInfo tickInfo;
  public int traceX = Integer.MIN_VALUE, traceY;

  public int count;
  public int[] countPlusIndices = new int[5];
  public Point3fi[] pts;
  public double value;
  public String property;
  
  String strFormat;
  String units;
  
  public Text text;

  private Viewer vwr;
  private String strMeasurement;
  private String type;

  // next three are used by MeaurementRenderer
  
  private boolean tainted;
  public A4d renderAxis;
  public P3d renderArc;
  private String newUnits;
  public double fixedValue = Double.NaN;
  private boolean isPending;
  public boolean inFront;
  private boolean useDefaultLabel;
  
  public boolean isTainted() {
    return (tainted && !(tainted = false));
  }
  
  public Measurement setM(ModelSet modelSet, Measurement m, double value, short colix,
                          String strFormat, int index) {
    //value Double.isNaN ==> pending
    this.ms = modelSet;
    this.index = index;
    this.vwr = modelSet.vwr;
    this.colix = colix;
    this.strFormat = strFormat;
    if (m != null) {
      tickInfo = m.tickInfo;
      pts = m.pts;
      mad = m.mad;
      thisID = m.thisID;
      text = m.text;
      property = m.property;
      units = m.units;
      if (property == null && "+hz".equals(units)) {
        property = "property_J";
      }
      if (thisID != null && text != null)
        labelColix = text.colix;
    }
    if (pts == null)
      pts = new Point3fi[4];
    int[] indices = (m == null ? null : m.countPlusIndices);
    count = (indices == null ? 0 : indices[0]);
    if (count > 0) {
      System.arraycopy(indices, 0, countPlusIndices, 0, count + 1);
      isTrajectory = modelSet.isTrajectoryMeasurement(countPlusIndices);
    }
    isPending = Double.isNaN(value);
    this.value = (isPending || isTrajectory ? getMeasurement(null) : value);
    formatMeasurement(null);
    return this;
  }

  public Measurement setPoints(ModelSet modelSet, int[] indices, Point3fi[] points,
      TickInfo tickInfo) {
    // temporary holding structure only; -- no vwr
    this.ms = modelSet;
    countPlusIndices = indices;
    count = indices[0];
    this.pts = (points == null ? new Point3fi[4] : points);
    vwr = modelSet.vwr;
    this.tickInfo = tickInfo;
    return this;
  }

  public void setCount(int count) {
    setCountM(count);
  }

  protected void setCountM(int count) {
    this.count = countPlusIndices[0] = count;
  }

  public int getAtomIndex(int n) {
    return (n > 0 && n <= count ? countPlusIndices[n] : -1);
  }

  public Point3fi getAtom(int n) {
    int pt = countPlusIndices[n];
    return (pt < -1 ? pts[-2 - pt] : ms.at[pt]);
  }

  public int getLastIndex() {
    return (count > 0 ? countPlusIndices[count] : -1);
  }

  public String getString() {
    return strMeasurement;
  }

  @Override
  public String toString() {
    return getString();
  }
  
  String getStringUsing(Viewer vwr, String strFormat, String units) {
    this.vwr = vwr;
    value = getMeasurement(null);
    formatMeasurementAs(strFormat, units, true);
    if (strFormat == null)
      return getInfoAsString(units);
    return strMeasurement;
  }

  public String getStringDetail() {
    return (count == 2 ? "Distance" : count == 3 ? "Angle" : "Torsion")
        + getMeasurementScript(" - ", false) + " : " + value;
  }

  public void refresh(Point3fi[] pts) {
    value = getMeasurement(pts);
    isTrajectory = ms.isTrajectoryMeasurement(countPlusIndices);
    formatMeasurement(null);
  }

  /**
   * Used by MouseManager and Picking Manager to build the script
   * 
   * @param sep
   * @param withModelIndex is needed for points only
   * @return ((1}) ({2})....
   */
  public String getMeasurementScript(String sep, boolean withModelIndex) {
    SB sb = new SB();
    boolean asBitSet = (sep.equals(" "));
    for (int i = 1; i <= count; i++)
      sb.append(i > 1 ? sep : " ").append(getLabel(i, asBitSet, withModelIndex));
    return sb.toString();
  }

  public void formatMeasurementAs(String strFormat, String units,
                                  boolean useDefault) {
    if (strFormat != null && strFormat.length() == 0)
      strFormat = null;
    if (!useDefault && strFormat != null
        && strFormat.indexOf(countPlusIndices[0] + ":") != 0)
      return;
    this.strFormat = strFormat;
    formatMeasurement(units);
  }

  public void formatMeasurement(String units) {
    tainted = true;
    switch (Double.isNaN(value) ? 0 : count) {
    default:
      strMeasurement = null;
      return;
    case 2:
      strMeasurement = formatDistance(units);
      return;
    case 3:
    case 4:
      strMeasurement = formatAngle(value);
      return;
    }
  }

  public void reformatDistanceIfSelected() {
    if (count == 2 && vwr.slm.isSelected(countPlusIndices[1])
        && vwr.slm.isSelected(countPlusIndices[2])) {
      int pt;
      if (useDefaultLabel && strFormat != null && (pt = strFormat.indexOf("//")) >= 0)
        strFormat = strFormat.substring(0, pt);
      formatMeasurement(null);
    }
  }

  /**
   * 
   * @param units from MEASURE or measure()
   * @return format
   */
  private String formatDistance(String units) {
    String label = getLabelString();
    if (label == null)
      return "";
    int pt = strFormat.indexOf("//");
    if (units == null) {
      units = this.units;
      if (units == null) {
        if (pt >= 0) {
        units = strFormat.substring(pt + 2);
        strFormat = strFormat.substring(0, pt);
        } else {
            units = (property == null ? vwr.g.measureDistanceUnits : "");
        }
      } 
    } else if (pt >= 0){
      strFormat = strFormat.substring(0, pt);
    }
    strFormat += "//" + units;
    units = fixUnits(units);
    pt = label.indexOf("//");
    if (pt >= 0) {
      label = label.substring(0, pt);
      if (label.length() == 0)
        label = "%VALUE";
    }
    double f = fixValue(units, (label.indexOf("%V") >= 0));
    return formatString(f, newUnits, label);
  }

  /**
   * 
   * @param units  final units
   * @param andRound
   * @return  double value
   */
  public double fixValue(String units, boolean andRound) {
    checkJ(units);
    if(units != null && units.startsWith("+")) {
      if (!isPending)
        value = Math.abs(value);
      units = units.substring(1);
    }
    newUnits = units;
    if (count != 2)
      return value;
    double dist = value;
    if (units == null && property != null)
      units = "";
    if (units != null) {
      boolean isPercent = units.equals("%");
      if (property == null && (isPercent || units.endsWith("hz"))) {
        int i1 = getAtomIndex(1);
        int i2 = getAtomIndex(2);
        if (i1 >= 0 && i2 >= 0) {
          Atom a1 = (Atom) getAtom(1);
          Atom a2 = (Atom) getAtom(2);
          int itype = nmrType(units);
          boolean isDC = (!isPercent && itype == NMR_DC);
          type = (isPercent ? "percent" : isDC ? "dipoleCouplingConstant"
              : itype == NMR_NOE_OR_J ? "NOE or 3JHH" : "J-CouplingConstant");
          if (itype == NMR_NOE_OR_J) {
            double[] result = vwr.getNMRCalculation().getNOEorJHH(new Atom[] { a1, null, null, a2}, NMRCalculation.MODE_CALC_NOE | NMRCalculation.MODE_CALC_JHH);
            if (result == null) {
              dist = Double.NaN;
              newUnits = units = "";
            } else {
              dist = result[1];
              units = newUnits = (result.length == 2 ? "noe" :"hz");
            }
          } else {
            dist = (isPercent ? dist
                / (a1.getVanderwaalsRadiusFloat(vwr, VDW.AUTO) + a2
                    .getVanderwaalsRadiusFloat(vwr, VDW.AUTO)) : isDC ? vwr
                .getNMRCalculation().getDipolarConstantHz(a1, a2) : vwr
                .getNMRCalculation().getIsoOrAnisoHz(true, a1, a2, units, null));
          }
          isValid = !Double.isNaN(dist);
          if (isPercent)
            units = "pm";
        }
      }
      return toUnits(dist, units, andRound);
    }
    return (andRound ? Math.round(dist * 100) / 100d : dist);
  }

  private void checkJ(String units) {
    if (property != null || units != null || this.units != null)
      return;
//    if (units == null && (units = this.units) == null)
      units = vwr.g.measureDistanceUnits;
    if ("+hz".equals(units)) {
      property = "property_J";
      this.units = units;
    }
  }

  public final static int NMR_NOT = 0;
  public final static int NMR_DC = 1;
  public final static int NMR_JC = 2;
  public final static int NMR_NOE_OR_J = 3;
  
  public static int nmrType(String units) {
    return (units.indexOf("hz") < 0 ? NMR_NOT : units.equals("noe_hz") ? NMR_NOE_OR_J : units.startsWith("dc_") || units.equals("khz") ? NMR_DC : NMR_JC);
  }

  private String formatAngle(double angle) {
    String label = getLabelString();
    if (label.indexOf("%V") >= 0)
      angle = (int) Math.round(angle * 10) / 10d;
    return formatString(angle, "\u00B0", label);
  }

  private String getLabelString() {
    int atomCount = countPlusIndices[0];
    String s = atomCount + ":";
    String label = null;
    if (strFormat != null) {
      if (strFormat.length() == 0)
        return null;
      label = (strFormat.length() > 2 && strFormat.indexOf(s) == 0 ? strFormat
          : null);
    }
    if (label == null) {
      strFormat = null;
      label = vwr.getDefaultMeasurementLabel(countPlusIndices[0]);
      useDefaultLabel = true;
    }
    if (label.indexOf(s) == 0)
      label = label.substring(2);
    if (strFormat == null)
      strFormat = s + label;
    return label;
  }

  private String formatString(double value, String units, String label) {
    return LabelToken.formatLabelMeasure(vwr, this, label, value, units);
  }

  public boolean sameAsPoints(int[] indices, Point3fi[] points) {
    if (count != indices[0])
      return false;
    boolean isSame = true;
    for (int i = 1; i <= count && isSame; i++)
      isSame = (countPlusIndices[i] == indices[i]);
    if (isSame)
      for (int i = 0; i < count && isSame; i++) {
        if (points[i] != null)
          isSame = (this.pts[i].distance(points[i]) < 0.01);
      }
    if (isSame)
      return true;
    switch (count) {
    default:
      return true;
    case 2:
      return sameAsIJ(indices, points, 1, 2) && sameAsIJ(indices, points, 2, 1);
    case 3:
      return sameAsIJ(indices, points, 1, 3) && sameAsIJ(indices, points, 2, 2)
          && sameAsIJ(indices, points, 3, 1);
    case 4:
      return sameAsIJ(indices, points, 1, 4) && sameAsIJ(indices, points, 2, 3)
          && sameAsIJ(indices, points, 3, 2) && sameAsIJ(indices, points, 4, 1);
    }
  }

  private boolean sameAsIJ(int[] atoms, Point3fi[] points, int i, int j) {
    int ipt = countPlusIndices[i];
    int jpt = atoms[j];
    return (ipt >= 0 || jpt >= 0 ? ipt == jpt : this.pts[-2 - ipt]
        .distance(points[-2 - jpt]) < 0.01);
  }

  public boolean sameAs(int i, int j) {
    return sameAsIJ(countPlusIndices, pts, i, j);
  }

  
  public double getPropMeasurement(Point3fi[] pts) {
    if (countPlusIndices == null || count != 2)
      return Double.NaN;
    for (int i = count; --i >= 0;)
      if (countPlusIndices[i + 1] < 0) {
        return Double.NaN;
      }
    try {
     Atom ptA = (Atom) (pts == null ? getAtom(1) : pts[0]);
     Atom ptB = (Atom) (pts == null ? getAtom(2) : pts[1]);
    double[][] props = (double[][]) vwr.getDataObj(property, null,
        JmolDataManager.DATA_TYPE_ADD);
    int ia = ptA.i;
    int ib = ptB.i;
    return (props == null || ib >= props.length || ia >= props.length ? Double.NaN : props[ia][ib]);
    } catch (Throwable t) {
      return Double.NaN;
    }
  }


  public double getMeasurement(Point3fi[] pts) {
    checkJ(null);
    if (!Double.isNaN(fixedValue))
      return fixedValue;
    if (property != null)
      return getPropMeasurement(pts);
    if (countPlusIndices == null)
      return Double.NaN;
    if (count < 2)
      return Double.NaN;
    for (int i = count; --i >= 0;)
      if (countPlusIndices[i + 1] == -1) {
        return Double.NaN;
      }
    Point3fi ptA = (pts == null ? getAtom(1) : pts[0]);
    Point3fi ptB = (pts == null ? getAtom(2) : pts[1]);
    Point3fi ptC;
    switch (count) {
    case 2:
      return ptA.distance(ptB);
    case 3:
      ptC = (pts == null ? getAtom(3) : pts[2]);
      return MeasureD.computeAngleABC(ptA, ptB, ptC, true);
    case 4:
      ptC = (pts == null ? getAtom(3) : pts[2]);
      Point3fi ptD = (pts == null ? getAtom(4) : pts[3]);
      return MeasureD.computeTorsion(ptA, ptB, ptC, ptD, true);
    default:
      return Double.NaN;
    }
  }

  public String getLabel(int i, boolean asBitSet, boolean withModelIndex) {
    int atomIndex = countPlusIndices[i];
    // double parens USED TO BE here because of situations like
    //  draw symop({3}), which the compiler USED TO interpret as symop()
    return (
        atomIndex < 0 ? (withModelIndex ? "modelIndex "
             + getAtom(i).mi + " " : "") + Escape.eP(getAtom(i)) 
        : asBitSet ? "({" + atomIndex + "})" 
        :vwr.getAtomInfo(atomIndex)
       );
  }

  public void setModelIndex(short modelIndex) {
    if (pts == null)
      return;
    for (int i = 0; i < count; i++) {
      if (pts[i] != null)
        pts[i].mi = modelIndex;
    }
  }

  public boolean isValid() {
    // valid: no A-A, A-B-A, A-B-C-B
    return !(sameAs(1, 2) || count > 2 && sameAs(1, 3) || count == 4
        && sameAs(2, 4));
  }

  public static int find(Lst<Measurement> measurements, Measurement m) {
    int[] indices = m.countPlusIndices;
    Point3fi[] points = m.pts;
    for (int i = measurements.size(); --i >= 0;)
      if (measurements.get(i).sameAsPoints(indices, points))
        return i;
    return -1;
  }

  public boolean isConnected(Atom[] atoms, int count) {
    int atomIndexLast = -1;
    for (int i = 1; i <= count; i++) {
      int atomIndex = getAtomIndex(i);
      if (atomIndex < 0)
        continue;
      if (atomIndexLast >= 0
          && !atoms[atomIndex].isBonded(atoms[atomIndexLast]))
        return false;
      atomIndexLast = atomIndex;
    }
    return true;
  }

  public String getInfoAsString(String units) {
    double f = fixValue(units, true);
    SB sb = new SB();
    sb.append(count == 2 ? (property != null ? property : type == null ? "distance" : type) : count == 3 ? "angle" : "dihedral");
    sb.append(" \t").appendD(f);
    sb.append(" \t").append(PT.esc(strMeasurement));
    for (int i = 1; i <= count; i++)
      sb.append(" \t").append(getLabel(i, false, false));
    if (thisID != null)
      sb.append(" \t").append(thisID);
    return sb.toString();
  }

  public boolean isInRange(RadiusData radiusData, double value) {
    if (radiusData.factorType == EnumType.FACTOR) {
      Atom atom1 = (Atom) getAtom(1);
      Atom atom2 = (Atom) getAtom(2);
      double d = (atom1.getVanderwaalsRadiusFloat(vwr, radiusData.vdwType) + atom2
          .getVanderwaalsRadiusFloat(vwr, radiusData.vdwType))
          * radiusData.value;
      return (value <= d);
    }
    return (radiusData.values[0] == Double.MAX_VALUE || value >= radiusData.values[0]
        && value <= radiusData.values[1]);
  }

  public boolean isIntramolecular(Atom[] atoms, int count) {
    int molecule = -1;
    for (int i = 1; i <= count; i++) {
      int atomIndex = getAtomIndex(i);
      if (atomIndex < 0)
        continue;
      int m = atoms[atomIndex].getMoleculeNumber(false);
      if (molecule < 0)
        molecule = m;
      else if (m != molecule)
        return false;
    }
    return true;
  }

  public boolean isMin(Map<String, Integer> htMin) {
    Atom a1 = (Atom) getAtom(1);
    Atom a2 = (Atom) getAtom(2);
    int d = (int) (a2.distanceSquared(a1)*100);
    String n1 = a1.getAtomName();
    String n2 = a2.getAtomName();
    String key = (n1.compareTo(n2) < 0 ? n1 + n2 : n2 + n1);
    Integer min = htMin.get(key);
    return (min != null && d == min.intValue());
  }

  public static boolean isUnits(String s) {
    return (PT.isOneOf((s.startsWith("+") ? s.substring(1) : s).toLowerCase(),
      ";nm;nanometers;pm;picometers;angstroms;angstroms;ang;\u00C5;au;vanderwaals;vdw;%;noe;")
      || s.indexOf(" ") < 0 && s.endsWith("hz"));
  }

  public static double toUnits(double dist, String units, boolean andRound) {
    if (Double.isNaN(dist))
      return Double.NaN;
    if (units.equals("hz"))
      return (andRound ? Math.round(dist * 10) / 10d : dist);
    if (units.equals("nm"))
      return (andRound ? Math.round(dist * 100) / 1000d : dist / 10);
    if (units.equals("pm"))
      return (andRound ? Math.round(dist * 1000) / 10d : dist * 100);
    if (units.equals("au"))
      return (andRound ? Math.round(dist / JC.ANGSTROMS_PER_BOHR * 1000) / 1000d
          : dist / JC.ANGSTROMS_PER_BOHR);
    if (units.endsWith("khz"))
      return (andRound ? Math.round(dist / 10) / 100d : dist / 1000);
//    if (units.equals("noe"))
//      return (andRound ? Math.round(dist * 100) / 100d : dist);
    return (andRound ? Math.round(dist * 100) / 100d : dist);
  }

  public static double fromUnits(double dist, String units) {
    if (units.equals("nm"))
      return dist * 10;
    if (units.equals("pm"))
      return dist / 100;
    if (units.equals("au"))
      return dist * JC.ANGSTROMS_PER_BOHR;
    if (units.equals("\u00C5"))
      return dist;
    return 0;
  }

  public static String fixUnits(String u) {
    String units = (u.endsWith("s") ? u.substring(0, u.length() - 1) : u);
    if (units.equals("nanometer"))
      return "nm";
    else if (units.equals("bohr") || units.equals("atomicunits") || units.equals("atomic"))
      return "au";
    else if (units.equals("picometer"))
      return "pm";
    else if (units.equals("\u00E5") || units.equals("angstrom") || units.equals("a") || units.equals("ang"))
      return "\u00C5";
    else if (units.equals("vanderwaal") || units.equals("vdw"))
      return "%";
    return u;
  }

  public String getDistanceFormatForState() {
    return (useDefaultLabel ? null : strFormat);
  }
  
  public void setFromMD(MeasurementData md, boolean andText) {
    if (md.thisID != null) {
      thisID = md.thisID;
      mad = md.mad;
      if (md.colix != 0)
        colix = md.colix;
      strFormat = md.strFormat;
      text = md.text;
    }
    units = md.units;
    property = md.property;
    fixedValue = md.fixedValue;
    if (!andText)
      return;
    if (md.colix != 0)
      colix = md.colix;
    if (md.mad != 0)
      mad = md.mad;
    if (md.strFormat != null) {
      strFormat = strFormat.substring(0, 2)
          + md.strFormat.substring(2);
    }
    if (md.text != null) {
      if (text == null) {
        text = md.text;
      } else {
        if (md.text.font != null)
          text.font = md.text.font;
        text.text = null;
        if (md.text.align != 0)
          text.align = md.text.align;
        if (md.colix != 0)
          labelColix = text.colix = md.text.colix;
      }
    }
    formatMeasurement(null);
  }


}
