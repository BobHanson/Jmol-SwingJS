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
import java.io.StringReader;
import java.util.Hashtable;
import java.util.IdentityHashMap;
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
import javajs.util.P3;
import net.sf.jniinchi.INCHI_BOND_TYPE;
import net.sf.jniinchi.INCHI_PARITY;
import net.sf.jniinchi.INCHI_STEREOTYPE;
import net.sf.jniinchi.JniInchiAtom;
import net.sf.jniinchi.JniInchiBond;
import net.sf.jniinchi.JniInchiInput;
import net.sf.jniinchi.JniInchiInputInchi;
import net.sf.jniinchi.JniInchiOutputStructure;
import net.sf.jniinchi.JniInchiStereo0D;
import net.sf.jniinchi.JniInchiStructure;
import net.sf.jniinchi.JniInchiWrapper;

public class InChIJNI implements JmolInChI {

  public InChIJNI() {
    // for dynamic loading
  }

  @Override
  public String getInchi(Viewer vwr, BS atoms, String molData, String options) {
    try {
      if (atoms == null ? molData == null : atoms.cardinality() == 0)
        return "";
      boolean getKey = false;
      if (options == null)
        options = "";
      String inchi = null;
      String lc = options.toLowerCase();
      boolean getSmiles = (lc.indexOf("smiles") == 0);
      boolean getStructure = (lc.indexOf("structure") >= 0);
      if (lc.startsWith("structure/")) {
        inchi = options.substring(10);
        options = lc = "";
      }
      if (molData != null && molData.startsWith("InChI=")) {
        inchi = molData;
        if (!getSmiles) {
          options = lc.replace("structure", "");
          getKey = true;
        }
      } else {
        getKey = (options.indexOf("key") >= 0);
        if (getKey) {
          options = options.replace("inchikey", "");
          options = options.replace("key", "");
        }
        JniInchiInput in = new JniInchiInput(getSmiles ? "" : options);
        JniInchiStructure struc;
        if (atoms == null) {
          in.setStructure(struc = newJniInchiStructure(vwr, molData));
        } else {
          in.setStructure(struc = newJniInchiStructure(vwr, atoms));
        }
        if (getStructure) {
          return toString(struc);
        }
        inchi = JniInchiWrapper.getInchi(in).getInchi();
      }
      if (getSmiles || getStructure) {
        JniInchiInputInchi in = new JniInchiInputInchi(inchi);
        JniInchiOutputStructure struc = JniInchiWrapper
            .getStructureFromInchi(in);
        return (getSmiles ? getSmiles(vwr, struc, options)
            : getStructure(struc));
      }
      return (getKey ? JniInchiWrapper.getInchiKey(inchi).getKey() : inchi);
    } catch (Exception e) {
      if (e.getMessage().indexOf("ption") >= 0)
        System.out.println(e.getMessage() + ": " + options.toLowerCase()
            + "\n See https://www.inchi-trust.org/download/104/inchi-faq.pdf for valid options");
      else
        e.printStackTrace();
      return "";
    }
  }

  private String getStructure(JniInchiStructure mol) {
    return toString(mol);
  }

  /**
   * Jmol addition to create a JniInchiStructure from Jmol atoms. Currently only
   * supports single, double, aromatic_single and aromatic_double.
   * 
   * @param vwr
   * @param bsAtoms
   * @return a structure for JniInput
   */
  private static JniInchiStructure newJniInchiStructure(Viewer vwr,
                                                        BS bsAtoms) {
    JniInchiStructure mol = new JniInchiStructure();
    JniInchiAtom[] atoms = new JniInchiAtom[bsAtoms.cardinality()];
    int[] map = new int[bsAtoms.length()];
    BS bsBonds = vwr.ms.getBondsForSelectedAtoms(bsAtoms, false);
    for (int pt = 0, i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms
        .nextSetBit(i + 1)) {
      Atom a = vwr.ms.at[i];
      String sym = a.getElementSymbol();
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
      INCHI_BOND_TYPE order = getOrder(bond.getCovalentOrder());
      if (order != null)
        mol.addBond(new JniInchiBond(atoms[map[bond.getAtomIndex1()]],
            atoms[map[bond.getAtomIndex2()]], order));
    }
    return mol;
  }

  /**
   * Jmol addition to create a JniInchiStructure from MOL data. Currently only
   * supports single, double, aromatic_single and aromatic_double.
   * 
   * @param vwr
   * @param molData
   * @return a structure for JniInput
   */
  private static JniInchiStructure newJniInchiStructure(Viewer vwr,
                                                        String molData) {
    JniInchiStructure mol = new JniInchiStructure();
    BufferedReader r = new BufferedReader(new StringReader(molData));
    try {
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
      while (ai.hasNext() && n < atoms.length) {
        P3 p = ai.getXYZ();
        JniInchiAtom a = new JniInchiAtom(p.x, p.y, p.z,
            Elements.elementSymbolFromNumber(ai.getElementNumber()));
        a.setCharge(ai.getFormalCharge());
        mol.addAtom(a);
        atoms[n++] = a;
      }
      while (bi.hasNext()) {
        INCHI_BOND_TYPE order = getOrder(bi.getEncodedOrder());
        if (order != null)
          mol.addBond(new JniInchiBond(
              atoms[((Integer) bi.getAtomUniqueID1()).intValue()],
              atoms[((Integer) bi.getAtomUniqueID2()).intValue()], order));
      }
    } finally {
      try {
        r.close();
      } catch (IOException e) {
      }
    }
    return mol;
  }

  private static INCHI_BOND_TYPE getOrder(int order) {
    switch (order) {
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

  private static String toString(JniInchiStructure mol) {
    int na = mol.getNumAtoms();
    int nb = mol.getNumBonds();
    String s = "";
    for (int i = 0; i < na; i++) {
      s += mol.getAtom(i).getDebugString() + "\n";
    }
    for (int i = 0; i < nb; i++) {
      s += mol.getBond(i).getDebugString() + "\n";
    }
    return s;
  }

  private Map<BS, int[]> mapTet;
  private Map<Integer, Boolean> mapPlanar;

  private String getSmiles(Viewer vwr, JniInchiOutputStructure struc,
                           String options) {
    int nAtoms = struc.getNumAtoms();
    int nBonds = struc.getNumBonds();
    int nh = 0;
    for (int i = 0; i < nAtoms; i++) {
      JniInchiAtom a = struc.getAtom(i);
      nh += a.getImplicitH();
    }
    SmilesAtom[] atoms = new SmilesAtom[nAtoms + nh];
    Map<JniInchiAtom, SmilesAtom> map = new Hashtable<JniInchiAtom, SmilesAtom>();
    mapTet = new Hashtable<BS, int[]>();
    mapPlanar = new Hashtable<Integer, Boolean>();
    int nb = 0;
    int na = 0;
    for (int i = 0; i < nAtoms; i++) {
      JniInchiAtom a = struc.getAtom(i);
      SmilesAtom n = atoms[na] = new SmilesAtom() {
        @Override
        public boolean definesStereo() {
          return true;
        }
        
        @Override
        public String getStereoAtAt(SimpleNode[] nodes) {
          return decodeInchiStereo(nodes);
        }
        
        @Override
        public Boolean isStereoOpposite(int iatom) {
          return isInchiOpposite(getIndex(), iatom);
        }

      };
      n.set((float) a.getX(), (float) a.getY(), (float) a.getZ());
      n.setIndex(na++);
      n.setCharge(a.getCharge());
      n.setSymbol(a.getElementType());
      nh = a.getImplicitH();
      for (int j = 0; j < nh; j++) {
        SmilesAtom h = atoms[na] = new SmilesAtom();
        h.setIndex(na++);
        h.setSymbol("H");
        SmilesBond sb = new SmilesBond(n, h, Edge.BOND_COVALENT_SINGLE, false);
        sb.index = nb++;
      }
      map.put(a, n);
    }
    for (int i = 0; i < nBonds; i++) {
      JniInchiBond b = struc.getBond(i);
      JniInchiAtom a1 = b.getOriginAtom();
      JniInchiAtom a2 = b.getTargetAtom();
      int bt = getJmolBondType(b);
      SmilesBond sb = new SmilesBond(map.get(a1), map.get(a2), bt, false);
      sb.index = nb++;
    }
    for (int i = 0; i < na; i++) {
      atoms[i].setBondArray();
    }
    for (int i = struc.getNumStereo0D(); --i >= 0;) {
      JniInchiStereo0D sd = struc.getStereo0D(i);
      JniInchiAtom[] an = sd.getNeighbors();
      if (an.length != 4)
        continue;
      JniInchiAtom ca = sd.getCentralAtom();
      int i0 = map.get(an[0]).getIndex();
      int i1 = map.get(an[1]).getIndex();
      int i2 = map.get(an[2]).getIndex();
      int i3 = map.get(an[3]).getIndex();
      boolean isEven = (sd.getParity() == INCHI_PARITY.EVEN);
      INCHI_STEREOTYPE type = sd.getStereoType();
      switch (type) {
      case ALLENE:
      case DOUBLEBOND:
        // alkene or 2N-cumnulene
        i1 = getOtherEneAtom(atoms, i1, i0);
        i2 = getOtherEneAtom(atoms, i2, i3);
        break;
      case NONE:
        continue;
      case TETRAHEDRAL:
        break;
      }
      if (ca == null) {
        mapPlanar.put(getIntKey(i0, i3), Boolean.valueOf(isEven));
        mapPlanar.put(getIntKey(i0, i2), Boolean.valueOf(!isEven));
        mapPlanar.put(getIntKey(i1, i3), Boolean.valueOf(!isEven));
        mapPlanar.put(getIntKey(i1, i2), Boolean.valueOf(isEven));
      } else {
        int[] list = new int[] { isEven ? i0 : i1, isEven ? i1 : i0, i2, i3 };
        mapTet.put(orderList(list), list);
      }
    }
    try {
      SmilesMatcherInterface m = vwr.getSmilesMatcher();
      return m.getSmiles(atoms, na, BSUtil.newBitSet2(0, na), options,
          JC.SMILES_TYPE_SMILES);
    } catch (Exception e) {
      e.printStackTrace();
      return "";
    }
  }

  protected Boolean isInchiOpposite(int index, int iatom) {
    return mapPlanar.get(getIntKey(index, iatom));
  }

  protected String decodeInchiStereo(SimpleNode[] nodes) {
    int[] list = new int[] { nodes[0].getIndex(),
        nodes[1].getIndex(), nodes[2].getIndex(), nodes[3].getIndex() };
    int[] list2 = mapTet.get(orderList(list));
    return (list2 == null ? null : isPermutation(list, list2) ? "@@" : "@");
  }

  private static Integer getIntKey(int i, int j) {
    return Integer.valueOf((Math.min(i, j) << 12) + Math.max(i, j));
  }

  private static BS orderList(int[] list) {
    BS bs = new BS();
    for (int i = 0; i < list.length; i++)
      bs.set(list[i]);
    return bs;
  }

  private static boolean isPermutation(int[] list, int[] list2) {
    boolean ok = true;
    for (int i = 0; i < 3; i++) {
      int l1 = list[i];
      for (int j = i + 1; j < 4; j++) {
        int l2 = list2[j];
        if (l2 == l1) {
          if (j != i) {
            list2[j] = list2[i];
            list2[i] = l2;
            ok = !ok;
          }
        }
      }
    }
    return ok;
  }

  private static int getOtherEneAtom(SmilesAtom[] atoms, int i0, int i1) {
    SmilesAtom a = atoms[i0];
    for (int i = a.getBondCount(); --i >= 0;) {
      if (a.getBond(i).getBondType() == Edge.BOND_COVALENT_SINGLE) {
        int i2 = a.getBondedAtomIndex(i);
        if (i2 != i1) {
          return i2;
        }
      }
    }
    // could be imine
    return -1;
  }

  private static int getJmolBondType(JniInchiBond b) {
    INCHI_BOND_TYPE type = b.getBondType();
    switch (type) {
    case NONE:
      return 0;
    case ALTERN:
      return Edge.BOND_AROMATIC;
    case DOUBLE:
      return Edge.BOND_COVALENT_DOUBLE;
    case TRIPLE:
      return Edge.BOND_COVALENT_TRIPLE;
    case SINGLE:
    default:
      return Edge.BOND_COVALENT_SINGLE;
    }
  }

}
