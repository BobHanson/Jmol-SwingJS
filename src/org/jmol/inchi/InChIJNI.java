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
import java.util.Map;

import org.jmol.adapter.smarter.AtomSetCollection;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolAdapterAtomIterator;
import org.jmol.api.JmolAdapterBondIterator;
import org.jmol.api.JmolInChI;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.util.Edge;
import org.jmol.util.Elements;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.P3;
import net.sf.jniinchi.INCHI_BOND_TYPE;
import net.sf.jniinchi.JniInchiAtom;
import net.sf.jniinchi.JniInchiBond;
import net.sf.jniinchi.JniInchiInput;
import net.sf.jniinchi.JniInchiInputInchi;
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
      if (options == null)
        options = "";
      if (options.startsWith("structure/")) {
        String inchi = options.substring(10);
        JniInchiInputInchi in = new JniInchiInputInchi(inchi);
        return getStructure(JniInchiWrapper.getStructureFromInchi(in));
      }
      String inchi = null;
      boolean haveKey = false;
      if (molData != null && molData.startsWith("InChI=")) {
        inchi = molData;
        haveKey = true;
      } else {
        options = options.toLowerCase();
        haveKey = (options.indexOf("key") >= 0);
        if (haveKey) {
          options = options.replace("inchikey", "");
          options = options.replace("key", "");
        }
        JniInchiInput in = new JniInchiInput(options);
        if (atoms == null) {
          in.setStructure(newJniInchiStructure(vwr, molData));
        } else {
          in.setStructure(newJniInchiStructure(vwr, atoms));          
        }
        inchi = JniInchiWrapper.getInchi(in).getInchi();
      }
      return (haveKey ? JniInchiWrapper.getInchiKey(inchi).getKey() : inchi);
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
  private static JniInchiStructure newJniInchiStructure(Viewer vwr, BS bsAtoms) {
    JniInchiStructure mol = new JniInchiStructure();
    JniInchiAtom[] atoms = new JniInchiAtom[bsAtoms.cardinality()];
    int[] map = new int[bsAtoms.length()];
    BS bsBonds = vwr.ms.getBondsForSelectedAtoms(bsAtoms, false);
    for (int pt = 0, i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms
        .nextSetBit(i + 1)) {
      Atom a = vwr.ms.at[i];
      mol.addAtom(
          atoms[pt] = new JniInchiAtom(a.x, a.y, a.z, a.getElementSymbol()));
      atoms[pt].setCharge(a.getFormalCharge());
      map[i] = pt++;
    }
    Bond[] bonds = vwr.ms.bo;
    for (int i = bsBonds.nextSetBit(0); i >= 0; i = bsBonds.nextSetBit(i + 1)) {
      Bond bond = bonds[i];
      INCHI_BOND_TYPE order = getOrder(bond.order);
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
    case Edge.BOND_AROMATIC_SINGLE:
      return INCHI_BOND_TYPE.SINGLE;
    case Edge.BOND_AROMATIC_DOUBLE:
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

}
