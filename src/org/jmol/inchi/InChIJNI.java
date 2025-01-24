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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.adapter.smarter.AtomSetCollection;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolAdapterAtomIterator;
import org.jmol.api.JmolAdapterBondIterator;
import org.jmol.api.JmolInChI;
import org.jmol.api.SmilesMatcherInterface;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.smiles.SmilesAtom;
import org.jmol.smiles.SmilesBond;
import org.jmol.util.BSUtil;
import org.jmol.util.Edge;
import org.jmol.util.Elements;
import org.jmol.util.SimpleNode;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.P3d;
import javajs.util.PT;
import net.sf.jniinchi.INCHI_BOND_STEREO;
import net.sf.jniinchi.INCHI_BOND_TYPE;
import net.sf.jniinchi.INCHI_PARITY;
import net.sf.jniinchi.INCHI_STEREOTYPE;
import net.sf.jniinchi.JniInchiAtom;
import net.sf.jniinchi.JniInchiBond;
import net.sf.jniinchi.JniInchiInput;
import net.sf.jniinchi.JniInchiInputInchi;
import net.sf.jniinchi.JniInchiOutput;
import net.sf.jniinchi.JniInchiOutputStructure;
import net.sf.jniinchi.JniInchiStereo0D;
import net.sf.jniinchi.JniInchiStructure;
import net.sf.jniinchi.JniInchiWrapper;

public class InChIJNI extends InchiJmol implements InChIStructureProvider {

  private JniInchiOutputStructure inchiModel;
  public InChIJNI() {
    // for dynamic loading
  }

  @Override
  public String getInchi(Viewer vwr, BS atoms, Object molData, String options) {
    try {
      if (atoms == null ? molData == null : atoms.isEmpty())
        return "";
      if (options == null)
        options = "";
      options = setParameters(options, molData, atoms, vwr);
      if(!fixedH && !inputInChI) {
          JniInchiInput in = new JniInchiInput(getSmiles ? "fixedh" : options);
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
      if (getSmiles || getInchiModel) {
        // "getStructure" is just a debugging method 
        // to see the exposed InChI structure in string form
        inchiModel = JniInchiWrapper
            .getStructureFromInchi(new JniInchiInputInchi(inchi));
        return (getSmiles ? getSmiles(vwr, smilesOptions)
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
    getAtomList();
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
      //System.out.println(i + " " + sym + " " + a);

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
        
        
        //System.out.println(i + " " + atom1 + " " + atom2 + " " + order + " " + stereo);

        
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
    JniInchiStructure mol = new JniInchiStructure();
    Object r = null;
    try {
      if (molData instanceof String) {
        r = new BufferedReader(new StringReader((String) molData));
      } else if (molData instanceof InputStream) {
        r = molData;
      }
      Map<String, Object> htParams = new Hashtable<String, Object>();
      JmolAdapter adapter = vwr.getModelAdapter();
      Object atomSetReader = adapter.getAtomSetCollectionReader("String", null,
          r, htParams);
      if (atomSetReader instanceof String) {
        System.err.println("InChIJNI could not read molData");
        return null;
      }
      AtomSetCollection asc = (AtomSetCollection) adapter
          .getAtomSetCollection(atomSetReader);
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
        String sym = Elements.elementSymbolFromNumber(atno);
        JniInchiAtom a = new JniInchiAtom(p.x, p.y, p.z, sym);
        
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
    } catch (Throwable t) {
      t.printStackTrace();
      System.err.println(t.toString());
    } finally {
      try {
        if (r instanceof BufferedReader) {
          ((BufferedReader) r).close();
        } else {
          ((InputStream) r).close();
        }
      } catch (IOException e) {
      }
    }
    return mol;
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
    String s = "{\"atoms\":[\n";
    String sep = "";
    for (int i = 0; i < na; i++) {
      JniInchiAtom a = mol.getAtom(i);
      mapAtoms.put(a,  Integer.valueOf(i));
      if (i > 0)
        s += ",\n";
      s += "{\n";
      s += toJSON("index", Integer.valueOf(i), ",");
      s += toJSON("elementType", a.getElementType(), ",");
      s += toJSON("charge", a.getCharge(), ",");
      s += toJSON("isotopeMass", a.getIsotopicMass(), ",");
      s += toJSON("implicitH", a.getImplicitH(), ",");
      s += toJSON("radical", a.getRadical(), ",");
      s += toJSON("x", a.getX(), ",");
      s += toJSON("y", a.getY(), ",");
      s += toJSON("z", a.getZ(), ",");
      s += toJSON("implicitDeuterium", a.getImplicitDeuterium(), ",");
      s += toJSON("implicitProtium", a.getImplicitProtium(), ",");
      s += toJSON("implicitTritium", a.getImplicitTritium(), "");
      s += "}";
    }
    s += "\n],\n\"bonds\":[\n";
    
    for (int i = 0; i < nb; i++) {
      if (i > 0)
        s += ",\n";
      s += "{\n";
      JniInchiBond b = mol.getBond(i);
      s += toJSON("originAtom", mapAtoms.get(b.getOriginAtom()), ",");
      s += toJSON("targetAtom", mapAtoms.get(b.getTargetAtom()), ",");
      s += toJSON("bondType", b.getBondType() , ",");
      s += toJSON("bondStereo", b.getBondStereo(), "");
      s += "}";
    }
    s += "\n],\n\"stereo\":[\n";
    for (int i = 0; i < ns; i++) {
      if (i > 0)
        s += ",\n";
      s += "{\n";
      JniInchiStereo0D d = mol.getStereo0D(i);
      s += toJSON("centralAtomID",mapAtoms.get(d.getCentralAtom()), ",");
      s += toJSON("debugString",d.getDebugString(), ",");
      s += toJSON("disconnectedParity",d.getDisconnectedParity(), ",");
      s += toJSON("parity",d.getParity(), ",");
      s += toJSON("stereoType",d.getStereoType(), ",");
      JniInchiAtom[] an = d.getNeighbors();
      int[] nbs = new int[an.length];
      for (int j = 0; j < an.length; j++) {
        nbs[j] = mapAtoms.get(d.getNeighbor(j)).intValue();
      }
      s += toJSON("neighbors",nbs, "");
      s += "}";
    }
    s += "\n]}\n";
    return s;
  }

  private static String toJSON(String key, Object val, String term) {
    return PT.toJSON(key, val) +  term + "\n";
  }

  private Map<JniInchiAtom, Integer> map = new Hashtable<JniInchiAtom, Integer>();
  
  private JniInchiAtom thisAtom;
  private JniInchiBond thisBond;
  private JniInchiStereo0D thisStereo;

  private void getAtomList() {
    for (int i = getNumAtoms(); --i >= 0;) 
      map.put(inchiModel.getAtom(i), Integer.valueOf(i));
  }
  
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
  public int getNumBonds() {
    return inchiModel.getNumBonds();
  }

  @Override
  public int getImplicitH() {
    return thisAtom.getImplicitH();
  }

  @Override
  public double getX() {
    return thisAtom.getX();
  }

  @Override
  public InChIStructureProvider setBond(int i) {
    thisBond = inchiModel.getBond(i);
    return this;
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
  public String getElementType() {
    return thisAtom.getElementType();
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
  public InChIStructureProvider setStereo0D(int i) {
    thisStereo = inchiModel.getStereo0D(i);
    return this;
  }

  @Override
  public int getNumStereo0D() {
    return inchiModel.getNumStereo0D();
  }

  @Override
  public String getInchiBondType() {
    INCHI_BOND_TYPE type = thisBond.getBondType();
    return type.name();
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
