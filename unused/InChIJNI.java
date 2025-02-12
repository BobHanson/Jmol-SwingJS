/*
 * Copyright 2006-2011 Sam Adams <sea36 at users.sourceforge.net>
 *
 * This file is part of JNI-InChI.
 *
 * JNI-InChI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JNI-InChI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JNI-InChI.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jmol.inchi;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.iupac.InChIStructureProvider;
import org.iupac.InchiUtils;
import org.jmol.adapter.smarter.AtomSetCollection;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolAdapterAtomIterator;
import org.jmol.api.JmolAdapterBondIterator;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.util.Edge;
import org.jmol.util.Elements;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.P3d;
import net.sf.jniinchi.INCHI_BOND_STEREO;
import net.sf.jniinchi.INCHI_BOND_TYPE;
import net.sf.jniinchi.JniInchiAtom;
import net.sf.jniinchi.JniInchiBond;
import net.sf.jniinchi.JniInchiInput;
import net.sf.jniinchi.JniInchiInputInchi;
import net.sf.jniinchi.JniInchiOutput;
import net.sf.jniinchi.JniInchiOutputStructure;
import net.sf.jniinchi.JniInchiStereo0D;
import net.sf.jniinchi.JniInchiStructure;
import net.sf.jniinchi.JniInchiWrapper;

/**
 * Interface with inchi.c via JNI-InChI. 
 * 
 * For MOL file data to InChi, we first create a Jmol model from 
 * the mol file data using Jmol's adapter, and then use that
 * to create a JNI-InChI model.
 * 
 * For InChI to SMILES, we use JNI-InChI to read InChI's input structure, 
 * via JNI-InChI.
 * 
 * 
 */
public class InChIJNI extends InchiJmol implements InChIStructureProvider {

  private JniInchiOutputStructure inchiModel;
  public InChIJNI() {
    System.out.println("InChiJNI loaded");
  }

  @Override
  public String getInchi(Viewer vwr, BS atoms, Object molData, String options) {
    try {
      options = setParameters(options, molData, atoms, vwr);
      if (options == null)
        return "";
      if (inputInChI) {
        // options are not passed through
        options = "inch";
          inchi = JniInchiWrapper.getInchiFromInchiString(inchi, options).getInchi();
      } else if (!optionalFixedH) {
        JniInchiInput in = new JniInchiInput(doGetSmiles ? "fixedh" : options);
        JniInchiStructure inchiInputModel;
        if (atoms == null) {
          in.setStructure(inchiInputModel = newJniInchiStructure(vwr, molData));
        } else {
          in.setStructure(inchiInputModel = newJniInchiStructureBS(vwr, atoms));
        }
        if (getInchiModel) {
          return toJSON(inchiInputModel);
        }
        JniInchiOutput out = JniInchiWrapper.getInchi(in);
        String msg = out.getMessage();
        if (msg != null)
          System.err.println(msg);
        inchi = out.getInchi();
      }
      if (doGetSmiles || getInchiModel) {
        // "getStructure" is just a debugging method 
        // to see the exposed InChI structure in string form
        // note this is the INPUT model
        inchiModel = JniInchiWrapper
            .getStructureFromInchi(new JniInchiInputInchi(inchi));
        return (doGetSmiles ? getSmiles(vwr, smilesOptions)
            : toJSON(inchiModel));
      }
      return (getKey ? JniInchiWrapper.getInchiKey(inchi).getKey() : inchi);
    } catch (Throwable e) {
      System.out.println(e);
      if (e.getMessage().indexOf("ption") >= 0)
        System.out.println(e.getMessage() + ": " + options.toLowerCase()
            + "\n See https://www.inchi-trust.org/download/104/inchi-faq.pdf for valid options");
      else
        e.printStackTrace();
      return "";
    }
  }

  private String getSmiles(Viewer vwr, String smilesOptions) {
    return new InchiToSmilesConverter(this).getSmiles(vwr, smilesOptions);
  }

  /**
   * Jmol addition to create a JniInchiStructure from Jmol atoms. Currently only
   * supports single, double, aromatic_single and aromatic_double.
   * 
   * @param vwr
   * @param bsAtoms
   * @return a structure for JniInput
   */
  private static JniInchiStructure newJniInchiStructureBS(Viewer vwr,
                                                        BS bsAtoms) {
    JniInchiStructure mol = new JniInchiStructure();
    JniInchiAtom[] atoms = new JniInchiAtom[bsAtoms.cardinality()];
    int[] map = new int[bsAtoms.length()];
    BS bsBonds = vwr.ms.getBondsForSelectedAtoms(bsAtoms, false);
    for (int pt = 0, i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms
        .nextSetBit(i + 1)) {
      Atom a = vwr.ms.at[i];
      String sym = a.getElementSymbolIso(false);
      int iso = a.getIsotopeNumber();
      if (a.getElementNumber() == 1) {
        sym = "H"; // in case this is D
      }
      mol.addAtom(atoms[pt] = new JniInchiAtom(a.x, a.y, a.z, sym));
      atoms[pt].setCharge(a.getFormalCharge());
      if (iso > 0)
        atoms[pt].setIsotopicMass(iso);
      map[i] = pt++;
    }
    Bond[] bonds = vwr.ms.bo;
    for (int i = bsBonds.nextSetBit(0); i >= 0; i = bsBonds.nextSetBit(i + 1)) {
      Bond bond = bonds[i];
      if (bond == null)
        continue;
      // partial bonds (as in ferrocene) become at least 1
      // but oddly still do not get generated, probably because the carbons are then too strange?
      INCHI_BOND_TYPE order = getOrder(Math.max(bond.isPartial() ? 1 : 0, bond.getCovalentOrder()));
      if (order != null) {
        int atom1 = bond.getAtomIndex1();
        int atom2 = bond.getAtomIndex2();
        INCHI_BOND_STEREO stereo = getInChIStereo(bond.getBondType());
        mol.addBond(new JniInchiBond(atoms[map[atom1]],
            atoms[map[atom2]], order, stereo));
      }
    }
    return mol;
  }

  /**
   * Jmol addition to create a JniInchiStructure from MOL data. Currently only
   * supports single, double, aromatic_single and aromatic_double.
   * 
   * @param vwr
   * @param molData
   *        String or InputStream
   * @return a structure for JniInput
   */
  private static JniInchiStructure newJniInchiStructure(Viewer vwr,
                                                        Object molData) {
    try {
      JniInchiStructure mol = new JniInchiStructure();
      JmolAdapter adapter = vwr.getModelAdapter();
      System.out.println("InChIJNI using MOL file data\n" + molData);
      Object o = adapter.getAtomSetCollectionInline(molData, null);
      if (o == null || o instanceof String) {
        System.err.println("InChIJNI could not read molData " + o);
        return null;
      }
      AtomSetCollection asc = (AtomSetCollection) o;
      JmolAdapterAtomIterator ai = adapter.getAtomIterator(asc);
      JmolAdapterBondIterator bi = adapter.getBondIterator(asc);
      JniInchiAtom[] atoms = new JniInchiAtom[asc.getAtomSetAtomCount(0)];
      int n = 0;
      Map<Object, Integer> atomMap = new Hashtable<Object, Integer>();
      while (ai.hasNext() && n < atoms.length) {
        P3d p = ai.getXYZ();
        int atno = ai.getElementNumber();
        if (atno <= 0) {
          System.err.println("InChIJNI atom " + p + " index " + ai.getUniqueID()
              + " is not a valid element");
          return null;
        }
        int isotopeMass = Elements.getIsotopeNumber(atno);
        atno = Elements.getElementNumber(atno);
        String sym = Elements.elementSymbolFromNumber(atno);
        JniInchiAtom a = new JniInchiAtom(p.x, p.y, p.z, sym);
        a.setIsotopicMass(isotopeMass);
        a.setCharge(ai.getFormalCharge());
        mol.addAtom(a);
        atomMap.put(ai.getUniqueID(), Integer.valueOf(n));
        atoms[n++] = a;
      }
      int nb = 0;
      while (bi.hasNext()) {
        int jmolOrder = bi.getEncodedOrder();
        INCHI_BOND_TYPE order = getOrder(jmolOrder);
        if (order != null) {
          Integer id1 = atomMap.get(bi.getAtomUniqueID1());
          Integer id2 = atomMap.get(bi.getAtomUniqueID2());
          if (id1 == null || id2 == null) {
            System.err.println("InChIJNI bond " + nb + "has null atom "
                + (id1 == null ? bi.getAtomUniqueID1() : "") + " "
                + (id2 == null ? bi.getAtomUniqueID2() : ""));
            return null;
          }
          JniInchiAtom a = atoms[id1.intValue()];
          JniInchiAtom b = atoms[id2.intValue()];
          if (a == null || b == null) {
            System.err
                .println("InChIJNI bond " + nb + "has null atom: " + a + "/" + b
                    + " for ids " + id1 + " " + id2 + " and " + n + " atoms");
            return null;
          }
          mol.addBond(new JniInchiBond(a, b, order, getInChIStereo(jmolOrder)));
          nb++;
        }
      }
      return mol;
    } catch (Throwable t) {
      t.printStackTrace();
      System.err.println(t.toString());
    }
    return null;
  }

  private static INCHI_BOND_STEREO getInChIStereo(int jmolOrder) {
    switch (jmolOrder) {
    case Edge.BOND_STEREO_FAR:
      return INCHI_BOND_STEREO.SINGLE_1DOWN;
    case Edge.BOND_STEREO_NEAR:
      return INCHI_BOND_STEREO.SINGLE_1UP;
    case Edge.BOND_STEREO_EITHER:
      // this will generate a stereo ?
      return INCHI_BOND_STEREO.SINGLE_1EITHER;
    default:
      return INCHI_BOND_STEREO.NONE;
    }
  }

  private static INCHI_BOND_TYPE getOrder(int jmolOrder) {
    switch (jmolOrder) {
    case Edge.BOND_STEREO_EITHER:
    case Edge.BOND_STEREO_FAR:
    case Edge.BOND_STEREO_NEAR:
    case Edge.BOND_COVALENT_SINGLE:
      return INCHI_BOND_TYPE.SINGLE;
    case Edge.BOND_COVALENT_DOUBLE:
      return INCHI_BOND_TYPE.DOUBLE;
    case Edge.BOND_COVALENT_TRIPLE:
      return INCHI_BOND_TYPE.TRIPLE;
    default:
      return null;
    }
  }

  @SuppressWarnings("boxing")
  private static String toJSON(JniInchiStructure mol) {
    int na = mol.getNumAtoms();
    int nb = mol.getNumBonds();
    int ns = mol.getNumStereo0D();
    Map<JniInchiAtom, Integer> mapAtoms = new HashMap<>();
    boolean haveXYZ = false;
    for (int i = 0; i < na; i++) {
      JniInchiAtom a = mol.getAtom(i);
      if (a.getX() != 0 || a.getY() != 0 || a.getZ() != 0) {
        haveXYZ = true;
        break;
      }
    }
    String s = "{";
    s += "\n\"atomCount\":" + na + ",\n";
    s += "\"atoms\":[\n";
    for (int i = 0; i < na; i++) {
      JniInchiAtom a = mol.getAtom(i);
      mapAtoms.put(a, Integer.valueOf(i));
      if (i > 0)
        s += ",\n";
      s += "{";
      s += toJSONInt("index", Integer.valueOf(i), "");
      s += toJSONNotNone("elname", a.getElementType(), ",");
      if (haveXYZ) {
        s += toJSONDouble("x", a.getX(), ",");
        s += toJSONDouble("y", a.getY(), ",");
        s += toJSONDouble("z", a.getZ(), ",");
      }
      s += toJSONNonZero("isotopeMass", a.getIsotopicMass(), ",");
      s += toJSONNonZero("charge", a.getCharge(), ",");
      s += toJSONNotNone("radical", a.getRadical(), ",");
      if (a.getImplicitH() > 0)
        s += toJSONNonZero("implicitH", a.getImplicitH(), ",");
      s += toJSONNonZero("implicitDeuterium", a.getImplicitDeuterium(), ",");
      s += toJSONNonZero("implicitProtium", a.getImplicitProtium(), ",");
      s += toJSONNonZero("implicitTritium", a.getImplicitTritium(), ",");
      s += "}";
    }
    s += "\n],";
    s += "\n\"bondCount\":" + nb + ",\n";
    s += "\n\"bonds\":[\n";

    for (int i = 0; i < nb; i++) {
      if (i > 0)
        s += ",\n";
      s += "{";
      JniInchiBond b = mol.getBond(i);
      s += toJSONInt("originAtom", mapAtoms.get(b.getOriginAtom()).intValue(),
          "");
      s += toJSONInt("targetAtom", mapAtoms.get(b.getTargetAtom()).intValue(),
          ",");
      String bt = b.getBondType().toString();
      if (!bt.equals("SINGLE"))
        s += toJSONString("type", bt, ",");
      s += toJSONNotNone("stereo", b.getBondStereo(), ",");
      s += "}";
    }
    s += "\n]";
    if (ns > 0) {
      s += ",\n\"stereoCount\":" + ns + ",\n";
      s += "\"stereo\":[\n";
      for (int i = 0; i < ns; i++) {
        if (i > 0)
          s += ",\n";
        s += "{";
        JniInchiStereo0D d = mol.getStereo0D(i);
        s += toJSONNotNone("type", d.getStereoType(), "");
        s += toJSONNotNone("parity", d.getParity(), ",");
        //s += toJSON("debugString",d.getDebugString(), ",");
        // never implemented? s += toJSON("disconnectedParity",d.getDisconnectedParity(), ",");
        JniInchiAtom[] an = d.getNeighbors();
        int[] nbs = new int[an.length];
        for (int j = 0; j < an.length; j++) {
          nbs[j] = mapAtoms.get(d.getNeighbor(j)).intValue();
        }
        s += toJSONArray("neighbors", nbs, ",");
        JniInchiAtom a = d.getCentralAtom();
        if (a != null)
          s += toJSONInt("centralAtom", mapAtoms.get(a).intValue(), ",");
        s += "}";
      }
      s += "\n]";
    }
    s += "}";
    System.out.println(s);
    return s;
  }

  private static String toJSONArray(String key, int[] val, String term) {
    String s = term  + "\"" + key + "\":[" + val[0];
    for (int i = 1; i < val.length; i++) {
      s += "," + val[i];
    }
    return s + "]";
  }

  private static String toJSONNonZero(String key, int val, String term) {
    return (val == 0 ? "" : toJSONInt(key, val, term));
  }

  private static String toJSONInt(String key, int val, String term) {
    return term + "\"" + key + "\":" + val;
  }

  private static String toJSONDouble(String key, double val, String term) {
    String s;
    if (val == 0) {
      s = "0";
    } else {
      s = "" + (val + (val > 0 ? 0.00000001 : -0.00000001));
      s = s.substring(0, s.indexOf(".") + 5);
      int n = s.length();
      while (s.charAt(--n) == '0') {
      }
      s = s.substring(0, n + 1);
    }
    return term + "\"" + key + "\":" + s;
  }

  private static String toJSONString(String key, String val, String term) {
    return term + "\"" + key + "\":\"" + val + "\"";
  }
  private static String toJSONNotNone(String key, Object val, String term) {
    String s = val.toString();
    return ("NONE".equals(s) ? "" : term + "\"" + key + "\":\"" + s + "\"");
  }

  private Map<JniInchiAtom, Integer> map = new Hashtable<JniInchiAtom, Integer>();
  
  private JniInchiAtom thisAtom;
  private JniInchiBond thisBond;
  private JniInchiStereo0D thisStereo;

  @Override
  public void initializeModelForSmiles() {
    for (int i = getNumAtoms(); --i >= 0;) 
      map.put(inchiModel.getAtom(i), Integer.valueOf(i));
  }

  /// atoms ///
  
  @Override
  public int getNumAtoms() {
    return inchiModel.getNumAtoms();
  }

  
  @Override
  public InChIStructureProvider setAtom(int i) {
    thisAtom = inchiModel.getAtom(i);
    return this;
  }

  @Override
  public String getElementType() {
    return thisAtom.getElementType();
  }

  @Override
  public double getX() {
    return thisAtom.getX();
  }

  @Override
  public double getY() {
    return thisAtom.getY();
  }

  @Override
  public double getZ() {
    return thisAtom.getZ();
  }

  @Override
  public int getCharge() {
    return thisAtom.getCharge();
  }

  @Override
  public int getIsotopicMass() {
    return InchiUtils.getActualMass(getElementType(), thisAtom.getIsotopicMass());
  }

  @Override
  public int getImplicitH() {
    return thisAtom.getImplicitH();
  }

  
  /// bonds ///
  
  @Override
  public int getNumBonds() {
    return inchiModel.getNumBonds();
  }

  @Override
  public InChIStructureProvider setBond(int i) {
    thisBond = inchiModel.getBond(i);
    return this;
  }


  @Override
  public int getIndexOriginAtom() {
    return map.get(thisBond.getOriginAtom()).intValue();
  }

  @Override
  public int getIndexTargetAtom() {
    return map.get(thisBond.getTargetAtom()).intValue();
  }

  @Override
  public String getInchiBondType() {
    INCHI_BOND_TYPE type = thisBond.getBondType();
    return type.name();
  }

  /// Stereo ///
  
  @Override
  public int getNumStereo0D() {
    return inchiModel.getNumStereo0D();
  }

  @Override
  public InChIStructureProvider setStereo0D(int i) {
    thisStereo = inchiModel.getStereo0D(i);
    return this;
  }
    
  @Override
  public int[] getNeighbors() {
    JniInchiAtom[] an = thisStereo.getNeighbors();
    
    int n = an.length;
    int[] a = new int[n];
    
    //add for loop
    for(int i = 0; i < n; i++) {
      a[i] = map.get(an[i]).intValue();
    }
    return a;
  }

  @Override
  public int getCenterAtom() {
    JniInchiAtom ca = thisStereo.getCentralAtom();
    return (ca == null? -1: map.get(ca).intValue());
  }

  @Override
  public String getStereoType() {
    return thisStereo.getStereoType().toString();
  }

  @Override
  public String getParity() {
    return thisStereo.getParity().toString();
  }
  


}
