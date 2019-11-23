/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-05-13 19:17:06 -0500 (Sat, 13 May 2006) $
 * $Revision: 5114 $
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
package org.jmol.quantum;

import java.io.BufferedReader;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.V3;

import org.jmol.api.JmolNMRInterface;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.MeasurementData;
import org.jmol.modelset.Model;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Tensor;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.Viewer;

/*
 * 
 * Bob Hanson hansonr@stolaf.edu 7/4/2013
 * 
 */

/*
 * NOTE -- THIS CLASS IS INSTANTIATED USING Interface.getOptionInterface
 * NOT DIRECTLY -- FOR MODULARIZATION. NEVER USE THE CONSTRUCTOR DIRECTLY!
 * 
 */

public class NMRCalculation implements JmolNMRInterface {

  private static final int MAGNETOGYRIC_RATIO = 1;
  private static final int QUADRUPOLE_MOMENT = 2;
  private static final double e_charge = 1.60217646e-19; //C 
  private static final double h_planck = 6.62606957e-34; //J*s
  private static final double h_bar_planck = h_planck / (2 * Math.PI);
  private static final double DIPOLAR_FACTOR = h_bar_planck * 1E37; // 1e37 = (1/1e-10)^3 * 1e7 * 1e7 / 1e-7
  private static final double J_FACTOR = h_bar_planck / (2 * Math.PI) * 1E33;
  private static final double Q_FACTOR = e_charge * (9.71736e-7) / h_planck;

  private Viewer vwr;

  public NMRCalculation() {
  }

  @Override
  public JmolNMRInterface setViewer(Viewer vwr) {
    this.vwr = vwr;
    getData();
    return this;
  }

  @Override
  public float getQuadrupolarConstant(Tensor efg) {
    if (efg == null)
      return 0;
    Atom a = vwr.ms.at[efg.atomIndex1];
    return (float) (getIsotopeData(a, QUADRUPOLE_MOMENT) * efg.eigenValues[2] * Q_FACTOR);
  }

  /**
   * Returns a list of tensors that are of the specified type and have both
   * atomIndex1 and atomIndex2 in bsA. If there is just one atom specified, then
   * the list is "all tensors involving this atom".
   * 
   * We have to use atom sites, because interaction tensors are not duplicated.
   * 
   * @param type
   * @param bsA
   * @return list of Tensors
   */
  @SuppressWarnings("unchecked")
  private Lst<Tensor> getInteractionTensorList(String type, BS bsA) {
    if (type != null)
      type = type.toLowerCase();
    BS bsModels = vwr.ms.getModelBS(bsA, false);
    BS bs1 = getAtomSiteBS(bsA);
    int iAtom = (bs1.cardinality() == 1 ? bs1.nextSetBit(0) : -1);
    Lst<Tensor> list = new Lst<Tensor>();
    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1)) {
      Lst<Tensor> tensors = (Lst<Tensor>) vwr.ms.getInfo(i,
          "interactionTensors");
      if (tensors == null)
        continue;
      int n = tensors.size();
      for (int j = 0; j < n; j++) {
        Tensor t = tensors.get(j);
        if (type == null || t.type.equals(type) && t.isSelected(bs1, iAtom))
          list.addLast(t);
      }
    }
    return list;
  }

  /**
   * Interaction tensors are not repeated for every possible combination. They
   * are just for the base atom set. These are identified as a.atomIndex ==
   * models[b.modelIndex].firstAtomIndex + b.atomSite - 1
   * 
   * @param bsA
   * @return new bs in terms of atom sites
   */
  private BS getAtomSiteBS(BS bsA) {
    if (bsA == null)
      return null;
    BS bs = new BS();
    Atom[] atoms = vwr.ms.at;
    Model[] models = vwr.ms.am;

    for (int i = bsA.nextSetBit(0); i >= 0; i = bsA.nextSetBit(i + 1)) {
      if (!bsA.get(i))
        continue;
      Atom a = atoms[i];
      bs.set(models[a.mi].firstAtomIndex - 1 + a.atomSite);
    }
    return bs;
  }

  //  private int getOtherAtom(Tensor t, int iAtom) {
  //      return (t.atomIndex1 == iAtom ? t.atomIndex2 : t.atomIndex1);
  //  }

  @Override
  public BS getUniqueTensorSet(BS bsAtoms) {
    BS bs = new BS();
    Atom[] atoms = vwr.ms.at;
    for (int i = vwr.ms.mc; --i >= 0;) {
      BS bsModelAtoms = vwr.getModelUndeletedAtomsBitSet(i);
      bsModelAtoms.and(bsAtoms);
      // exclude any models without symmetry
      if (vwr.ms.getUnitCell(i) == null)
        continue;
      // exclude any symmetry-
      for (int j = bsModelAtoms.nextSetBit(0); j >= 0; j = bsModelAtoms
          .nextSetBit(j + 1))
        if (atoms[j].atomSite != atoms[j].i + 1)
          bsModelAtoms.clear(j);
      bs.or(bsModelAtoms);
      // march through all the atoms in the model...
      for (int j = bsModelAtoms.nextSetBit(0); j >= 0; j = bsModelAtoms
          .nextSetBit(j + 1)) {
        Object[] ta = atoms[j].getTensors();
        if (ta == null)
          continue;
        // go through all this atom's tensors...
        for (int jj = ta.length; --jj >= 0;) {
          Tensor t = (Tensor) ta[jj];
          if (t == null)
            continue;
          // for each tensor in A, go through all atoms after the first-selected one...
          for (int k = bsModelAtoms.nextSetBit(j + 1); k >= 0; k = bsModelAtoms
              .nextSetBit(k + 1)) {
            Object[] tb = atoms[k].getTensors();
            if (tb == null)
              continue;
            // for each tensor in B, go through all this atom's tensors... 
            for (int kk = tb.length; --kk >= 0;) {
              // if equivalent, reject it.
              if (t.isEquiv((Tensor) tb[kk])) {
                bsModelAtoms.clear(k);
                bs.clear(k);
                break;
              }
            }
          }
        }
      }
    }
    return bs;
  }

  public float getJCouplingHz(Atom a1, Atom a2, String type, Tensor isc) {
    return getIsoOrAnisoHz(true, a1, a2, type, isc);
  }

  @Override
  public float getIsoOrAnisoHz(boolean isIso, Atom a1, Atom a2, String units,
                               Tensor isc) {
    if (isc == null) {
      String type = getISCtype(a1, units);
      if (type == null || a1.mi != a2.mi)
        return (units.equals("hz") ? (float)calc3J(a1, a2)[1] : 0);
      BS bs = new BS();
      bs.set(a1.i);
      bs.set(a2.i);
      Lst<Tensor> list = getInteractionTensorList(type, bs);
      if (list.size() == 0)
        return Float.NaN;
      isc = list.get(0);
    } else {
      a1 = vwr.ms.at[isc.atomIndex1];
      a2 = vwr.ms.at[isc.atomIndex2];
    }
    return (float) (getIsotopeData(a1, MAGNETOGYRIC_RATIO)
        * getIsotopeData(a2, MAGNETOGYRIC_RATIO)
        * (isIso ? isc.isotropy() : isc.anisotropy()) * J_FACTOR);
  }

  @SuppressWarnings("unchecked")
  private String getISCtype(Atom a1, String type) {
    Lst<Tensor> tensors = (Lst<Tensor>) vwr.ms.getInfo(a1.mi,
        "interactionTensors");
    if (tensors == null)
      return null;
    type = (type == null ? "" : type.toLowerCase());
    int pt = -1;
    if ((pt = type.indexOf("_hz")) >= 0 || (pt = type.indexOf("_khz")) >= 0
        || (pt = type.indexOf("hz")) >= 0 || (pt = type.indexOf("khz")) >= 0)
      type = type.substring(0, pt);
    if (type.length() == 0)
      type = "isc";
    return type;
  }

  @Override
  public float getDipolarConstantHz(Atom a1, Atom a2) {
    if (Logger.debugging)
      Logger.debug(a1 + " g=" + getIsotopeData(a1, MAGNETOGYRIC_RATIO) + "; "
          + a2 + " g=" + getIsotopeData(a2, MAGNETOGYRIC_RATIO));
    float v = (float) (-getIsotopeData(a1, MAGNETOGYRIC_RATIO)
        * getIsotopeData(a2, MAGNETOGYRIC_RATIO) / Math.pow(a1.distance(a2), 3) * DIPOLAR_FACTOR);
    return (v == 0 || a1 == a2 ? Float.NaN : v);
  }

  @Override
  public float getDipolarCouplingHz(Atom a1, Atom a2, V3 vField) {
    V3 v12 = V3.newVsub(a2, a1);
    double r = v12.length();
    double costheta = v12.dot(vField) / r / vField.length();
    return (float) (getDipolarConstantHz(a1, a2) * (3 * costheta - 1) / 2);
  }

  /**
   * isotopeData keyed by nnnSym, for example: 1H, 19F, etc.; and also by
   * element name itself: H, F, etc., for default
   */
  private Map<String, double[]> isotopeData;

  /**
   * NOTE! Do not change this txt file! Instead, edit
   * trunk/Jmol/_documents/nmr_data.xls and then clip its contents to
   * org/jmol/quantum/nmr_data.txt.
   * 
   */
  private final static String resource = "nmr_data.txt";

  /**
   * Get magnetogyricRatio (gamma/10^7 rad s^-1 T^-1) and quadrupoleMoment
   * (Q/10^-2 fm^2) for a given isotope or for the default isotope of an
   * element.
   * 
   * @param a
   * 
   * @param iType
   * @return g or Q
   */
  private double getIsotopeData(Atom a, int iType) {
    int iso = a.getIsotopeNumber();
    String sym = a.getElementSymbolIso(false);
    double[] d = isotopeData.get(iso == 0 ? sym : "" + iso + sym);
    return (d == null ? 0 : d[iType]);
  }

  /**
   * Creates the data set necessary for doing NMR calculations. Values are
   * retrievable using getProperty "nmrInfo" "Xx"; each entry is
   * float[+/-isotopeNumber, g, Q], where [0] < 0 for the default value.
   * 
   */
  @SuppressWarnings("resource")
  // Resource leak: 'br' is not closed at this location -- Nonsense -- there's a finally{} block
  private void getData() {
    BufferedReader br = null;
    try {
      boolean debugging = Logger.debugging;
      br = FileManager.getBufferedReaderForResource(vwr, this,
          "org/jmol/quantum/", resource);
      // #extracted by Simone Sturniolo from ROBIN K. HARRIS, EDWIN D. BECKER, SONIA M. CABRAL DE MENEZES, ROBIN GOODFELLOW, AND PIERRE GRANGER, Pure Appl. Chem., Vol. 73, No. 11, pp. 1795–1818, 2001. NMR NOMENCLATURE. NUCLEAR SPIN PROPERTIES AND CONVENTIONS FOR CHEMICAL SHIFTS (IUPAC Recommendations 2001)
      // #element atomNo  isotopeDef  isotope1  G1  Q1  isotope2  G2  Q2  isotope3  G3  Q3
      // H 1 1 1 26.7522128  0 2 4.10662791  0.00286 3 28.5349779  0
      isotopeData = new Hashtable<String, double[]>();
      String line;
      while ((line = br.readLine()) != null) {
        if (debugging)
          Logger.info(line);
        if (line.indexOf("#") >= 0)
          continue;
        String[] tokens = PT.getTokens(line);
        String name = tokens[0];
        String defaultIso = tokens[2] + name;
        if (debugging)
          Logger.info(name + " default isotope " + defaultIso);
        for (int i = 3; i < tokens.length; i += 3) {
          int n = Integer.parseInt(tokens[i]);
          String isoname = n + name;
          double[] dataGQ = new double[] { n,
              Double.parseDouble(tokens[i + 1]),
              Double.parseDouble(tokens[i + 2]) };
          if (debugging)
            Logger.info(isoname + "  " + Escape.eAD(dataGQ));
          isotopeData.put(isoname, dataGQ);
        }
        double[] defdata = isotopeData.get(defaultIso);
        if (defdata == null) {
          Logger.error("Cannot find default NMR data in nmr_data.txt for "
              + defaultIso);
          throw new NullPointerException();
        }
        defdata[0] = -defdata[0];
        isotopeData.put(name, defdata);
      }
    } catch (Exception e) {
      Logger.error("Exception " + e.toString() + " reading " + resource);
    } finally {
      try {
        br.close();
      } catch (Exception ee) {
        // ignore        
      }
    }
  }

  @Override
  public Object getInfo(String what) {
    if (what.equals("all")) {
      Map<String, Object> map = new Hashtable<String, Object>();
      map.put("isotopes", isotopeData);
      map.put("shiftRefsPPM", shiftRefsPPM);
      return map;
    }
    if (PT.isDigit(what.charAt(0)))
      return isotopeData.get(what);
    Lst<Object> info = new Lst<Object>();
    for (Entry<String, double[]> e : isotopeData.entrySet()) {
      String key = e.getKey();
      if (PT.isDigit(key.charAt(0)) && key.endsWith(what))
        info.addLast(e.getValue());
    }
    return info;
  }

  @Override
  public float getChemicalShift(Atom atom) {
    float v = getMagneticShielding(atom);
    if (Float.isNaN(v))
      return v;
    Float ref = shiftRefsPPM.get(atom.getElementSymbol());
    return (ref == null ? 0 : ref.floatValue()) - v;
  }

  @Override
  public float getMagneticShielding(Atom atom) {
    Tensor t = vwr.ms.getAtomTensor(atom.i, "ms");
    return (t == null ? Float.NaN : t.isotropy());
  }

  private Map<String, Float> shiftRefsPPM = new Hashtable<String, Float>();

  @Override
  public boolean getState(SB sb) {
    if (shiftRefsPPM.isEmpty())
      return false;
    for (Entry<String, Float> nuc : shiftRefsPPM.entrySet())
      sb.append("  set shift_").append(nuc.getKey()).append(" ")
          .appendO(nuc.getValue()).append("\n");
    return true;
  }

  @Override
  public boolean setChemicalShiftReference(String element, float value) {
    if (element == null) {
      shiftRefsPPM.clear();
      return false;
    }
    element = element.substring(0, 1).toUpperCase() + element.substring(1);
    shiftRefsPPM.put(element, Float.valueOf(value));
    return true;
  }

  @Override
  public Lst<Object> getTensorInfo(String tensorType, String infoType, BS bs) {
    if ("".equals(tensorType))
      tensorType = null;
    infoType = (infoType == null ? ";all." : ";" + infoType + ".");
    Lst<Object> data = new Lst<Object>();
    Lst<Object> list1;
    if (";dc.".equals(infoType)) {
      // tensorType is irrelevant for dipolar coupling constant
      Atom[] atoms = vwr.ms.at;
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        for (int j = bs.nextSetBit(i + 1); j >= 0; j = bs.nextSetBit(j + 1)) {
          list1 = new Lst<Object>();
          list1.addLast(Integer.valueOf(atoms[i].i));
          list1.addLast(Integer.valueOf(atoms[j].i));
          list1
              .addLast(Float.valueOf(getDipolarConstantHz(atoms[i], atoms[j])));
          data.addLast(list1);
        }
      return data;
    }
    if (tensorType == null || tensorType.startsWith("isc")) {
      boolean isJ = infoType.equals(";j.");
      boolean isEta = infoType.equals(";eta.");
      Lst<Tensor> list = getInteractionTensorList(tensorType, bs);
      int n = (list == null ? 0 : list.size());
      for (int i = 0; i < n; i++) {
        Tensor t = list.get(i);
        list1 = new Lst<Object>();
        list1.addLast(Integer.valueOf(t.atomIndex1));
        list1.addLast(Integer.valueOf(t.atomIndex2));
        list1.addLast(isEta || isJ ? Float.valueOf(getIsoOrAnisoHz(isJ, null,
            null, null, t)) : t.getInfo(infoType));
        data.addLast(list1);
      }
      if (tensorType != null)
        return data;
    }
    boolean isChi = tensorType != null && tensorType.startsWith("efg")
        && infoType.equals(";chi.");
    boolean isFloat = (isChi || Tensor.isFloatInfo(infoType));
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      if (tensorType == null) {
        Object[] a = vwr.ms.getAtomTensorList(i);
        if (a != null)
          for (int j = 0; j < a.length; j++)
            data.addLast(((Tensor) a[j]).getInfo(infoType));
      } else {
        Tensor t = vwr.ms.getAtomTensor(i, tensorType);
        data.addLast(t == null ? (isFloat ? Float.valueOf(0) : "")
            : isChi ? Float.valueOf(getQuadrupolarConstant(t)) : t
                .getInfo(infoType));
      }
    }
    return data;
  }

  @Override
  public Map<String, Integer> getMinDistances(MeasurementData md) {
    BS bsPoints1 = (BS) md.points.get(0);
    int n1 = bsPoints1.cardinality();
    if (n1 == 0 || !(md.points.get(1) instanceof BS))
      return null;
    BS bsPoints2 = (BS) md.points.get(1);
    int n2 = bsPoints2.cardinality();
    if (n1 < 2 && n2 < 2)
      return null;
    Map<String, Integer> htMin = new Hashtable<String, Integer>();
    Atom[] atoms = vwr.ms.at;
    for (int i = bsPoints1.nextSetBit(0); i >= 0; i = bsPoints1
        .nextSetBit(i + 1)) {
      Atom a1 = atoms[i];
      String name = a1.getAtomName();
      for (int j = bsPoints2.nextSetBit(0); j >= 0; j = bsPoints2
          .nextSetBit(j + 1)) {
        Atom a2 = atoms[j];
        int d = (int) (a2.distanceSquared(a1) * 100);
        if (d == 0)
          continue;
        String name1 = a2.getAtomName();
        String key = (name.compareTo(name1) < 0 ? name + name1 : name1 + name);
        Integer min = htMin.get(key);
        if (min == null) {
          min = Integer.valueOf(d);
          htMin.put(key, min);
          continue;
        }
        if (d < min.intValue())
          htMin.put(key, Integer.valueOf(d));
      }
    }
    return htMin;
  }

  //// non-quantum calcs

  /**
   * Calculate an H-X-X-H or C-X-X-H coupling constant.
   * 
   * Use Altoona equation (Tetrahedron 36, 2783-2792)
   * 
   * If there are fewer than three substituents on each central atom, or if
   * either central atom is not carbon, defaults to general Karplus equation.
   * 
   * 
   * 
   * @param elements
   * @param subElements
   * @param subVectors
   * @param v21
   * @param v34
   * @param v23
   * @param theta
   * @param CHequation
   * @return estimated hA-xA-xB-hB or cA-xA-xB-hB or hA-xA-xB-cB coupling
   *         constant
   */
  public static double calcJ3(String[] elements, String[][] subElements,
                              V3[][] subVectors, V3 v21, V3 v34,
                              V3 v23, double theta, String CHequation) {
    if (subElements[0][2] != null && subElements[1][2] != null) {
      if (elements[0].equals("H") && elements[3].equals("H")) {
        if (elements[1].equals("C") && elements[2].equals("C"))
          return calc3JHH(subElements, subVectors, v21, v34, v23,
              theta);
      } else if ((elements[0].equals("C") && elements[3].equals("H"))
          || (elements[0].equals("H") && elements[3].equals("C"))) {
        return calc3JCH(CHequation, theta);
      }
    }
    return calcJKarplus(theta);
  }

  static Hashtable<String, Double> deltaElectro = new Hashtable<String, Double>();

  static {
    // Values taken from www.spectroscopynow.com/Spy/tools/proton-proton.html website. Need to find original source
    // 2.20 is eneg of H
    double enegH = 2.20;
    deltaElectro.put("C", new Double(2.60 - enegH));
    deltaElectro.put("O", new Double(3.50 - enegH));
    deltaElectro.put("N", new Double(3.05 - enegH));
    deltaElectro.put("F", new Double(3.90 - enegH));
    deltaElectro.put("Cl", new Double(3.15 - enegH));
    deltaElectro.put("Br", new Double(2.95 - enegH));
    deltaElectro.put("I", new Double(2.65 - enegH));
    deltaElectro.put("S", new Double(2.58 - enegH));// Pauling
    deltaElectro.put("Si", new Double(1.90 - enegH));// Pauling
  }

  static double[][] pAltoona = new double[5][8];
  static {
    for (int nNonH = 0; nNonH < 5; nNonH++) {
      double[] p = pAltoona[nNonH];
      switch (nNonH) {
      case 0:
      case 1:
      case 2:
        p[1] = 13.89;
        p[2] = -0.98;
        p[3] = 0;
        p[4] = 1.02;
        p[5] = -3.40;
        p[6] = 14.9;
        p[7] = 0.24;
        break;
      case 3:
        p[1] = 13.22;
        p[2] = -0.99;
        p[3] = 0;
        p[4] = 0.87;
        p[5] = -2.46;
        p[6] = 19.9;
        p[7] = 0;
        break;
      case 4:
        p[1] = 13.24;
        p[2] = -0.91;
        p[3] = 0;
        p[4] = 0.53;
        p[5] = -2.41;
        p[6] = 15.5;
        p[7] = 0.19;
        break;
      }
      p[6] = p[6] * Math.PI / 180.0;

    }
  }

  public static double calcTheta(V3 v21, V3 v34, V3 v23) {
    V3 n1 = new V3();
    V3 n2 = new V3();
    n1.cross(v21, v23);
    n2.cross(v34, v23);
    double angle = n1.angle(n2);
    return n1.dot(v34) > 0 ? -angle : angle;
  }

  public static double calcJKarplus(double theta) {
    // Simple Karplus equation for 3JHH, ignoring differences in C-substituents
    final double j0 = 8.5;
    final double j180 = 9.5;
    final double jconst = 0.28;

    double jab = 0;
    if (theta >= -90.0 && theta < 90.0) {
      jab = j0 * Math.pow((Math.cos(theta)), 2) - jconst;
    } else {
      jab = j180 * Math.pow((Math.cos(theta)), 2) - jconst;
    }

    return jab;
  }

  /**
   * 
   * 
   * @param subElements  int[2][3] with element names
   * @param subVectors   V3[2][4] with vectors TO these substituents from their respective centers 
   * @param v21  vector from cA to hA
   * @param v34  vector from cB to hB
   * @param v23  vector from cA to cB
   * @param theta  dihedral angle  hA-cA-cB-hB
   * @return estimated coupling constant
   */
  private static double calc3JHH(String[][] subElements, V3[][] subVectors, V3 v21, V3 v34, V3 v23, double theta) {
    // Substituents on atoms A and B
    // Check number of substituents

    int nNonH = 0;

    // Count substituents of carbons A and B  
    for (int i = 0; i < 3; i++) {
      if (!subElements[0][i].equals("H")) {
        nNonH++;
      }
      if (!subElements[1][i].equals("H")) {
        nNonH++;
      }
    }

    double jvalue = getInitialJValue(nNonH, theta);
    
    for (int i = 0; i < 3; i++) {
      String element = subElements[0][i];
      if (!element.equals("H")) {
        jvalue += getIncrementalJValue(nNonH, element, subVectors[0][i],
            v21, v23, theta);
      }
      element = subElements[1][i];
      if (!element.equals("H")) {
        jvalue += getIncrementalJValue(nNonH, element, subVectors[1][i],
            v34, v23, theta);
      }
    }
    return jvalue;
  }


  private static double getIncrementalJValue(int nNonH, String element, V3 sA_cA,
                                            V3 v21, V3 v23, double theta) {
    if (nNonH < 0 || nNonH > 5)
      return 0;
    Double de = deltaElectro.get(element);
    if (de == null)
      return 0;
    double e = de.doubleValue();
    int sign = getSubSign(sA_cA, v21, v23);
    double[] p = pAltoona[nNonH];
    return e
        * (p[4] + p[5] * Math.cos(sign * theta + p[6] * Math.abs(e))
            * Math.cos(sign * theta + p[6] * Math.abs(e)));
  }

  private static double getInitialJValue(int nNonH, double theta) {
    double[] p = pAltoona[nNonH];
    return p[1] * Math.cos(theta) * Math.cos(theta) + p[2]
        * Math.cos(theta);
  }

  
//  static {
//    for (int n = 0; n < 360; n+= 10)
//    System.out.println(n + "\t" + getInitialJValue(2, n*Math.PI/180));
//    System.out.println("NMRCalc???");
//  }
  final public static String JCH3_WASYLISHEN_SCHAEFER = "was";
  final public static String JCH3_TVAROSKA_TARAVEL = "tva";
  final public static String JCH3_AYDIN_GUETHER = "ayd";

  /**
   * 
   * @param CHequation
   * 
   *        'was' Simple equation for 3JCH, from Wasylishen and Schaefer Can J
   *        Chem (1973) 51 961 used in Kozerski et al. J Chem Soc Perkin 2,
   *        (1997) 1811
   * 
   *        'tva' Tvaroska and Taravel Adv. Carbohydrate Chem. Biochem. (1995)
   *        51, 15-61
   * 
   *        'ayd' Aydin and Guether Mag. Res. Chem. (1990) 28, 448-457
   * 
   * @param theta
   *        dihedral
   * 
   * @return 3JCH prediction
   */
  public static double calc3JCH(String CHequation, double theta) {

    if (CHequation == null)
      CHequation = "was";
    if (!PT.isOneOf(CHequation, ";was;tva;ayd;"))
      throw new IllegalArgumentException(
          "Equation must be one of was, tva, or ayd");

    if (CHequation.equals("was")) {

      final double A = 3.56;
      final double C = 4.26;

      double j = A * Math.cos(2 * theta) - Math.cos(theta) + C;
      return j;

    } else if (CHequation.equals("tva")) {
      double j = 4.5 - 0.87 * Math.cos(theta) + Math.cos(2.0 * theta);
      return j;
    } else if (CHequation.equals("ayd")) {
      double j = 5.8 * Math.pow(Math.cos(theta), 2) - 1.6 * Math.cos(theta)
          + 0.28 * Math.sin(2.0 * theta) - 0.02 * Math.sin(theta) + 0.52;
      return j;
    } else {
      return 0.0;
    }
  }

  private static int getSubSign(V3 sA_cA, V3 v21, V3 v23) {

    // Look for sign of (v23 x v21).(sA_cA)
    V3 cross = new V3();
    cross.cross(v23, v21);
    int sign;
    if (cross.dot(sA_cA) > 0) {
      sign = 1;
    } else {
      sign = -1;
    }
    return sign;
  }

  public static double[] calc3J(Atom atom1, Atom atom4) {
    Atom atom2 = atom1.bonds[0].getOtherAtom(atom1);
    Atom atom3 = atom4.bonds[0].getOtherAtom(atom4);
    if (!atom2.isCovalentlyBonded(atom3))
      return new double[2];

    String[] elements = new String[] { atom1.getElementSymbol(),
        atom2.getElementSymbol(), atom3.getElementSymbol(),
        atom4.getElementSymbol() };
    String[][] subElements = new String[2][3];
    V3[][] subVectors = new V3[2][3];

    V3 v23 = V3.newVsub(atom3, atom2);
    V3 v21 = V3.newVsub(atom1, atom2);
    V3 v34 = V3.newVsub(atom4, atom3);

    Lst<Atom> subs = new Lst<Atom>();
    
    Bond[] bonds = atom2.bonds;
    for (int pt = 0, i = Math.min(bonds.length, 4); --i >= 0;) {
      Atom sub = bonds[i].getOtherAtom(atom2);
      if (sub == atom3)
        continue;
      subElements[0][pt] = sub.getElementSymbol();
      subVectors[0][pt] = V3.newVsub(sub, atom2);
      pt++;
    }
    subs.clear();
    bonds = atom3.bonds;
    for (int pt = 0, i = Math.min(bonds.length, 4); --i >= 0;) {
      Atom sub = bonds[i].getOtherAtom(atom3);
      if (sub == atom2)
        continue;
      subElements[1][pt] = sub.getElementSymbol();
      subVectors[1][pt] = V3.newVsub(sub, atom3);
      pt++;
    }

    double theta = NMRCalculation.calcTheta(v21, v34, v23);
    double jvalue = NMRCalculation.calcJ3(elements, subElements, subVectors, v21, v34, v23, theta, null);
    return new double[] { theta, jvalue };
  }

}
