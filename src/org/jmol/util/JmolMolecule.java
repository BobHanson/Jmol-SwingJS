/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-05-18 08:19:45 -0500 (Fri, 18 May 2007) $
 * $Revision: 7742 $

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.jmol.util;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.BS;
import org.jmol.modelset.Atom;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;

/**
 * an independent class utilizing only org.jmol.api.JmolNode, not
 * org.jmol.modelset.Atom for use in finding molecules in models and SMILES
 * strings
 * 
 */
public class JmolMolecule {

  public JmolMolecule() {
  }

  public Node[] nodes;

  public int moleculeIndex;
  public int modelIndex;
  public int indexInModel;
  public int firstAtomIndex;
  public int ac;
  public int nElements;
  public int[] elementCounts = new int[Elements.elementNumberMax];
  public int[] altElementCounts = new int[Elements.altElementMax];
  public int elementNumberMax;
  public int altElementMax;
  public String mf;
  public BS atomList;

  public int[] atNos; // alternative for molecular formula only

  /**
   * Creates an array of JmolMolecules from a set of atoms in the form of simple
   * JmolNodes. Allows for appending onto an already established set of branches
   * (from BioPolymer).
   * 
   * @param atoms
   *        set of atoms to check
   * @param bsModelAtoms
   *        per-model atom list, or null
   * @param biobranches
   *        pre-defined connections, like bonds but not to be followed
   *        internally
   * @param bsExclude
   *        TODO
   * @return an array of JmolMolecules
   */
  public final static JmolMolecule[] getMolecules(Node[] atoms,
                                                  BS[] bsModelAtoms,
                                                  Lst<BS> biobranches,
                                                  BS bsExclude) {
    BS bsToTest = null;
    BS bsBranch = new BS();
    int thisModelIndex = -1;
    int indexInModel = 0;
    int moleculeCount = 0;
    JmolMolecule[] molecules = new JmolMolecule[4];
    if (bsExclude == null)
      bsExclude = new BS();

    for (int i = 0; i < atoms.length; i++)
      if (!bsExclude.get(i) && !bsBranch.get(i)) {
        Node a = atoms[i];
        if (a == null || a.isDeleted()) {
          bsExclude.set(i);
          continue;
        }
        int modelIndex = a.getModelIndex();
        if (modelIndex != thisModelIndex) {
          thisModelIndex = modelIndex;
          indexInModel = 0;
          bsToTest = bsModelAtoms[modelIndex];
        }
        bsBranch = getBranchBitSet(atoms, i, bsToTest, biobranches, -1, true,
            true);
        if (bsBranch.nextSetBit(0) >= 0) {
          molecules = addMolecule(molecules, moleculeCount++, atoms, i,
              bsBranch, modelIndex, indexInModel++, bsExclude);
        }
      }
    return allocateArray(molecules, moleculeCount);
  }

  /**
   * 
   * given a set of atoms, a subset of atoms to test, two atoms that start the
   * branch, and whether or not to allow the branch to cycle back on itself,
   * deliver the set of atoms constituting this branch.
   * 
   * @param atoms
   * @param atomIndex
   *        the first atom of the branch
   * @param bsToTest
   *        some subset of those atoms
   * @param biobranches
   *        pre-determined groups of connected atoms
   * @param atomIndexNot
   *        the "root" atom stopping branch development; often a ring atom; if
   *        -1, then this method will return all atoms in a connected set of
   *        atoms.
   * @param allowCyclic
   *        allow
   * @param allowBioResidue
   *        TODO
   * @return a bitset of atoms along this branch
   */
  public static BS getBranchBitSet(Node[] atoms, int atomIndex, BS bsToTest,
                                   Lst<BS> biobranches, int atomIndexNot,
                                   boolean allowCyclic,
                                   boolean allowBioResidue) {
    BS bs = BS.newN(atoms.length);
    if (atomIndex < 0)
      return bs;
    if (atomIndexNot >= 0)
      bsToTest.clear(atomIndexNot);
    return (getCovalentlyConnectedBitSet(atoms, atoms[atomIndex], bsToTest,
        allowCyclic, allowBioResidue, biobranches, bs, null, null) ? bs : new BS());
  }

  public final static JmolMolecule[] addMolecule(JmolMolecule[] molecules,
                                                 int iMolecule, Node[] atoms,
                                                 int iAtom, BS bsBranch,
                                                 int modelIndex,
                                                 int indexInModel,
                                                 BS bsExclude) {
    bsExclude.or(bsBranch);
    if (iMolecule == molecules.length)
      molecules = allocateArray(molecules, iMolecule * 2 + 1);
    molecules[iMolecule] = initialize(atoms, iMolecule, iAtom, bsBranch,
        modelIndex, indexInModel);
    return molecules;
  }

  public static String getMolecularFormulaAtoms(Node[] atoms, BS bsSelected,
                                                double[] wts,
                                                boolean isEmpirical) {
    JmolMolecule m = new JmolMolecule();
    m.nodes = atoms;
    m.atomList = bsSelected;
    return m.getMolecularFormula(false, wts, isEmpirical);
  }

  public String getMolecularFormula(boolean includeMissingHydrogens,
                                    double[] wts, boolean isEmpirical) {
    getMFArray(includeMissingHydrogens, wts, isEmpirical);
    if (elementCounts[0] < 0)
      return "?";
    String mf = "";
    String sep = "";
    int nX;
    for (int i = 1; i <= elementNumberMax; i++) {
      nX = elementCounts[i];
      if (nX != 0) {
        mf += sep + Elements.elementSymbolFromNumber(i) + " " + nX;
        sep = " ";
      }
    }
    return mf;
  }

  public int[] getMFArray(boolean includeMissingHydrogens,
                                        double[] wts, boolean isEmpirical) {
    // get element and atom counts
    if (atomList == null) {
      atomList = new BS();
      atomList.setBits(0, atNos == null ? nodes.length : atNos.length);
    }
    elementCounts = new int[Elements.elementNumberMax];
    altElementCounts = new int[Elements.altElementMax];
    ac = atomList.cardinality();
    nElements = 0;
    for (int p = 0, i = atomList.nextSetBit(0); i >= 0; i = atomList
        .nextSetBit(i + 1), p++) {
      int n;
      Node node = null;
      if (atNos == null) {
        node = nodes[i];
        if (node == null)
          continue;
        n = node.getAtomicAndIsotopeNumber();
      } else {
        n = atNos[i];
      }
      int f = (wts == null ? 1 : (int) (8 * wts[p]));
      if (n < Elements.elementNumberMax) {
        if (elementCounts[n] == 0)
          nElements++;
        elementCounts[n] += f;
        elementNumberMax = Math.max(elementNumberMax, n);
      } else {
        n = Elements.altElementIndexFromNumber(n);
        if (altElementCounts[n] == 0)
          nElements++;
        altElementCounts[n] += f;
        altElementMax = Math.max(altElementMax, n);
      }
      if (includeMissingHydrogens) {
        int nH = Math.max(0, node.getImplicitHydrogenCount())
            + Math.max(node.getExplicitHydrogenCount(), 0);
        if (nH > 0) {
          if (elementCounts[1] == 0)
            nElements++;
          elementCounts[1] += nH * f;
          elementNumberMax = Math.max(elementNumberMax, 1);
        }
      }
    }
    if (wts != null)
      for (int i = 1; i <= elementNumberMax; i++) {
        int c = elementCounts[i] / 8;
        if (c * 8 != elementCounts[i]) {
          elementCounts[0] = -1;
          return elementCounts;
        }
        elementCounts[i] = c;
      }
    if (isEmpirical) {
      int min = 2;
      boolean ok = true;
      while (ok) {
        min = 100000;
        int c;
        for (int i = 1; i <= elementNumberMax; i++)
          if ((c = elementCounts[i]) > 0 && c < min)
            min = c;
        if (min == 1)
          break;
        int j = min;
        for (; j > 1; j--) {
          ok = true;
          for (int i = 1; i <= elementNumberMax && ok; i++)
            if ((c = elementCounts[i]) / j * j != c)
              ok = false;
          if (ok) {
            for (int i = 1; i <= elementNumberMax; i++)
              elementCounts[i] /= j;
            break;
          }
        }
      }
    }
    return elementCounts;
  }

  private static JmolMolecule initialize(Node[] nodes, int moleculeIndex,
                                         int firstAtomIndex, BS atomList,
                                         int modelIndex, int indexInModel) {
    JmolMolecule jm = new JmolMolecule();
    jm.nodes = nodes;
    jm.firstAtomIndex = firstAtomIndex;
    jm.atomList = atomList;
    jm.ac = atomList.cardinality();
    jm.moleculeIndex = moleculeIndex;
    jm.modelIndex = modelIndex;
    jm.indexInModel = indexInModel;
    return jm;
  }

  private static boolean getCovalentlyConnectedBitSet(Node[] atoms, Node atom,
                                                      BS bsToTest,
                                                      boolean allowCyclic,
                                                      boolean allowBioResidue,
                                                      Lst<BS> biobranches,
                                                      BS bsResult,
                                                      Node origAtom, Node prevAtom) {
    int atomIndex = atom.getIndex();
    if (!bsToTest.get(atomIndex))
      return allowCyclic;
    if (!allowBioResidue && atom.getBioStructureTypeName().length() > 0)
      return allowCyclic;
    bsToTest.clear(atomIndex);
    if (biobranches != null && !bsResult.get(atomIndex)) {
      for (int i = biobranches.size(); --i >= 0;) {
        BS b = biobranches.get(i);
        if (b.get(atomIndex)) {
          bsResult.or(b); // prevent iteration to same set 
          bsToTest.andNot(b);
          for (int j = b.nextSetBit(0); j >= 0; j = b.nextSetBit(j + 1)) {
            Node atom1 = atoms[j];
            // allow just this atom for now
            if (atom1 == null)
              continue;
            bsToTest.set(j);
            getCovalentlyConnectedBitSet(atoms, atom1, bsToTest, allowCyclic,
                allowBioResidue, biobranches, bsResult, origAtom, atom);
            bsToTest.clear(j);
          }
          break;
        }
      }
    }
    bsResult.set(atomIndex);
    Edge[] bonds = atom.getEdges();
    if (bonds == null)
      return true;

    // now do bonds

    for (int i = bonds.length; --i >= 0;) {
      Edge bond = bonds[i];
      if (bond != null && bond.isCovalent()) {
        Node n = (Node) bond.getOtherNode(atom);
        if (n == prevAtom)
          continue;
        if (n == origAtom)
          return false;
        if (!getCovalentlyConnectedBitSet(atoms, n, bsToTest,
            allowCyclic, allowBioResidue, biobranches, bsResult, origAtom, atom))
          return false;
      }
    }
    return true;
  }

  private static JmolMolecule[] allocateArray(JmolMolecule[] molecules,
                                              int len) {
    return (len == molecules.length ? molecules
        : (JmolMolecule[]) AU.arrayCopyObject(molecules, len));
  }

  public static BS getBitSetForMF(Atom[] at, BS bsAtoms, String mf) {
    Map<String, int[]> map = new Hashtable<String, int[]>();
    char ch;
    boolean isDigit;
    mf = PT.rep(PT.clean(mf + "Z"), " ", "");
    for (int i = 0, pt = 0, pt0 = 0, n = mf.length(); i < n; i++) {
      if ((isDigit = Character.isDigit((ch = mf.charAt(i))))
          || i > 0 && Character.isUpperCase(ch)) {
        pt0 = i;
        String s = mf.substring(pt, pt0).trim();
        if (isDigit)
          while (i < n && Character.isDigit(mf.charAt(i)))
            i++;
        pt = i;
        map.put(s,
            new int[] { isDigit ? PT.parseInt(mf.substring(pt0, pt)) : 1 });
      }
    }
    BS bs = new BS();
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
      String a = at[i].getElementSymbol();
      int[] c = map.get(a);
      if (c == null || c[0]-- < 1)
        continue;
      bs.set(i);
    }
    for (int[] e : map.values())
      if (e[0] > 0)
        return new BS();
    return bs;
  }

  public static BS getBranchesForInversion(Node[] at, int atomIndex,
                                           BS bsToTest) {
    BS bs = new BS();
    Node a = at[atomIndex];
    Edge[] bonds = a.getEdges();
    for (int i = a.getBondCount(); --i >= 0;) {
      if (bonds[i].getBondType() == Edge.BOND_COVALENT_SINGLE)
        bs.set(bonds[i].getOtherNode(a).getIndex());
    }
    if (bs.cardinality() < 2) {
      bs.clearAll();
    } else {
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        if (at[i].getCovalentBondCount() == 1)
          continue;
        BS bs0 = BS.copy(bsToTest);
        bs0.clear(atomIndex);
        BS bs1 = BS.newN(at.length);
        if (!getCovalentlyConnectedBitSet(at, at[i], bs0,
            true, true, null, bs1, at[atomIndex], at[atomIndex])) {
          bs.clear(i);
        }
      }
    }
    return bs;
  }

}
