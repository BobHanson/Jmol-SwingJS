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

import org.jmol.api.JmolInChI;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.util.Edge;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import net.sf.jniinchi.INCHI_BOND_TYPE;
import net.sf.jniinchi.JniInchiAtom;
import net.sf.jniinchi.JniInchiBond;
import net.sf.jniinchi.JniInchiException;
import net.sf.jniinchi.JniInchiInput;
import net.sf.jniinchi.JniInchiInputInchi;
import net.sf.jniinchi.JniInchiOutputStructure;
import net.sf.jniinchi.JniInchiStructure;
import net.sf.jniinchi.JniInchiWrapper;

public class InChIJNI implements JmolInChI {

  public InChIJNI() {
    // for dynamic loading
  }

  @Override
  public String getInchi(Viewer vwr, BS atoms, String options) {
    try {
      if (options == null)
        options = "";
      if (options.startsWith("structure/")) {
        String inchi = options.substring(10);
        JniInchiInputInchi in = new JniInchiInputInchi(inchi);
        return getStructure(JniInchiWrapper.getStructureFromInchi(in));
      }
      options = options.toLowerCase();
      boolean haveKey = (options.indexOf("key") >= 0);
      if (haveKey) {
        options = options.replace("inchikey", "");
        options = options.replace("key", "");
      }
      JniInchiInput in = new JniInchiInput(options);
      in.setStructure(newJniInchiStructure(vwr, atoms));
      String s = JniInchiWrapper.getInchi(in).getInchi();
      return (haveKey ? JniInchiWrapper.getInchiKey(s).getKey() : s);
    } catch (JniInchiException e) {
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
      map[i] = pt++;
    }
    Bond[] bonds = vwr.ms.bo;
    for (int i = bsBonds.nextSetBit(0); i >= 0; i = bsBonds.nextSetBit(i + 1)) {
      Bond bond = bonds[i];
      INCHI_BOND_TYPE order;
      switch (bond.order) {
      case Edge.BOND_COVALENT_SINGLE:
      case Edge.BOND_AROMATIC_SINGLE:
        order = INCHI_BOND_TYPE.SINGLE;
        break;
      case Edge.BOND_AROMATIC_DOUBLE:
      case Edge.BOND_COVALENT_DOUBLE:
        order = INCHI_BOND_TYPE.DOUBLE;
        break;
      case Edge.BOND_COVALENT_TRIPLE:
        order = INCHI_BOND_TYPE.TRIPLE;
        break;
      default:
        continue;
      }
      mol.addBond(new JniInchiBond(atoms[map[bond.getAtomIndex1()]],
          atoms[map[bond.getAtomIndex2()]], order));
    }
    return mol;
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
