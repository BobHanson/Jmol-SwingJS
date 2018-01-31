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

import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.V3;

import org.jmol.api.JmolNMRInterface;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
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
      Lst<Tensor> tensors = (Lst<Tensor>) vwr
          .ms.getInfo(i, "interactionTensors");
      if (tensors == null)
        continue;
      int n = tensors.size();
      for (int j = 0; j < n; j++) {
        Tensor t = tensors.get(j);
        if (type == null  || t.type.equals(type) && t.isSelected(bs1, iAtom))
          list.addLast(t);
      }
    }
    return list;
  }

  /**
   * Interaction tensors are not repeated for every possible combination. They are just for the
   * base atom set. These are identified as a.atomIndex == models[b.modelIndex].firstAtomIndex + b.atomSite - 1  
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
  public float getIsoOrAnisoHz(boolean isIso, Atom a1, Atom a2, String type, Tensor isc) {
    if (isc == null) {
      type = getISCtype(a1, type);
      if (type == null || a1.mi != a2.mi)
        return 0;
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
        * getIsotopeData(a2, MAGNETOGYRIC_RATIO) * (isIso ? isc.isotropy() : isc.anisotropy()) * J_FACTOR);
  }

  @SuppressWarnings("unchecked")
  private String getISCtype(Atom a1, String type) {
    Lst<Tensor> tensors = (Lst<Tensor>) vwr.ms.getInfo(a1.mi, "interactionTensors");
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
      Logger.debug(a1 + " g=" + getIsotopeData(a1, MAGNETOGYRIC_RATIO) + "; " + a2 + " g=" + getIsotopeData(a2, MAGNETOGYRIC_RATIO));
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
      br = FileManager.getBufferedReaderForResource(vwr, this, "org/jmol/quantum/",
          resource);
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
    for (Entry<String, double[]> e: isotopeData.entrySet()) {
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
      sb.append("  set shift_").append(nuc.getKey()).append(" ").appendO(nuc.getValue()).append("\n");
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

}
