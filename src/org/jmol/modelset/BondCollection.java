/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-10-14 12:33:20 -0500 (Sun, 14 Oct 2007) $
 * $Revision: 8408 $

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

package org.jmol.modelset;



import javajs.util.AU;

import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Edge;
import org.jmol.util.JmolMolecule;
import org.jmol.viewer.JC;
import javajs.util.BS;
import javajs.util.MeasureD;
import javajs.util.V3d;

import org.jmol.script.T;

abstract public class BondCollection extends AtomCollection {

  public Bond[] bo;
  public int bondCount;
  protected int[] numCached;
  protected Bond[][][] freeBonds;
  //note: Molecules is set up to only be calculated WHEN NEEDED
  protected JmolMolecule[] molecules;
  protected int moleculeCount;

  protected short defaultCovalentMad;

  private BS bsAromaticSingle;
  private BS bsAromaticDouble;
  protected BS bsAromatic;

  public boolean haveHiddenBonds;
  
  protected V3d v1 = new V3d();
  protected V3d v2 = new V3d();

  protected final static int BOND_GROWTH_INCREMENT = 250;
  protected final static int MAX_BONDS_LENGTH_TO_CACHE = 5;
  protected final static int MAX_NUM_TO_CACHE = 200;

  // J2S note 12/19/2014: J2S fails to instantiate fields for superclasses such as this
  // thus, using protected bsAromatic = new BS();  fails
  
  protected void setupBC() {
    bsAromatic = new BS();
    numCached = new int[MAX_BONDS_LENGTH_TO_CACHE];
    freeBonds = new Bond[MAX_BONDS_LENGTH_TO_CACHE][][];
    for (int i = MAX_BONDS_LENGTH_TO_CACHE; --i > 0;) // NOT >= 0
      freeBonds[i] = new Bond[MAX_NUM_TO_CACHE][];
    setupAC();
  }

  protected void releaseModelSetBC() {
    bo = null;
    freeBonds = null;
    releaseModelSetAC();
  }

  public BondIterator getBondIteratorForType(int bondType, BS bsAtoms) {
    //Dipoles, Sticks
    return new BondIteratorSelected(bo, bondCount, bondType, bsAtoms, 
        vwr.getBoolean(T.bondmodeor));
  }

  public BondIterator getBondIterator(BS bsBonds) {
    //Sticks
    return new BondIteratorSelected(bo, bondCount, Edge.BOND_ORDER_NULL, bsBonds, false);
  }
  
  public short getBondColix1(int i) {
    return C.getColixInherited(bo[i].colix, bo[i].atom1.colixAtom);
  }

  public short getBondColix2(int i) {
    return C.getColixInherited(bo[i].colix, bo[i].atom2.colixAtom);
  }
  
  /**
   * for general use
   * 
   * @param modelIndex the model of interest or -1 for all
   * @return the actual number of connections
   */
  protected int getBondCountInModel(int modelIndex) {
    int n = 0;
    for (int i = bondCount; --i >= 0;)
      if (bo[i].atom1.mi == modelIndex)
        n++;
    return n;
  }

  public BS getBondsForSelectedAtoms(BS bsAtoms, boolean bondSelectionModeOr) {
    BS bs = new BS();
    for (int iBond = 0; iBond < bondCount; ++iBond) {
      Bond bond = bo[iBond];
      boolean isSelected1 = bsAtoms.get(bond.atom1.i);
      boolean isSelected2 = bsAtoms.get(bond.atom2.i);
      if (bondSelectionModeOr ? isSelected1 || isSelected2 : isSelected1 && isSelected2)
        bs.set(iBond);
    }
    return bs;
  }

  public Bond bondAtoms(Atom atom1, Atom atom2, int order, short mad, BS bsBonds, double energy, boolean addGroup, boolean isNew) {
    // this method used when a bond must be flagged as new
    Bond bond = getOrAddBond(atom1, atom2, order, mad, bsBonds, energy, true);
    if (isNew) {
      bond.order |= Edge.BOND_NEW;
      if (addGroup) {
        // for adding hydrogens
        atom1.group = atom2.group;
        atom1.group.addAtoms(atom1.i);
      }
    }
    return bond;
  }

  protected Bond getOrAddBond(Atom atom, Atom atomOther, int order, short mad,
                            BS bsBonds, double energy, boolean overrideBonding) {
    int i;
    if (order == Edge.BOND_ORDER_NULL || order == Edge.BOND_ORDER_ANY)
      order = 1;
    if (atom.isBonded(atomOther)) {
      i = atom.getBond(atomOther).index;
      if (overrideBonding) {
        bo[i].setOrder(order);
        bo[i].setMad(mad);
        if (bo[i] instanceof HBond)
          ((HBond) bo[i]).energy = energy;
      }
    } else {
      if (bondCount == bo.length)
        bo = (Bond[]) AU.arrayCopyObject(bo, bondCount
            + BOND_GROWTH_INCREMENT);
      i = setBond(bondCount++,
          bondMutually(atom, atomOther, order, mad, energy)).index;
    }
    if (bsBonds != null)
      bsBonds.set(i);
    return bo[i];
  }

  protected Bond setBond(int index, Bond bond) {
    return bo[bond.index = index] = bond;
  }

  protected Bond bondMutually(Atom atom, Atom atomOther, int order, short mad, double energy) {
    Bond bond;
    if (Edge.isOrderH(order)) {
      bond = new HBond(atom, atomOther, order, mad, C.INHERIT_ALL, energy);
    } else {
      bond = new Bond(atom, atomOther, order, mad, C.INHERIT_ALL);
    }
    addBondToAtom(atom, bond);
    addBondToAtom(atomOther, bond);
    return bond;
  }

  private void addBondToAtom(Atom atom, Bond bond) {
    if (atom.bonds == null) {
      atom.bonds = new Bond[1];
      atom.bonds[0] = bond;
    } else {
      atom.bonds = addToBonds(bond, atom.bonds);
    }
  }

  private Bond[] addToBonds(Bond newBond, Bond[] oldBonds) {
    Bond[] newBonds;
    if (oldBonds == null) {
      if (numCached[1] > 0)
        newBonds = freeBonds[1][--numCached[1]];
      else
        newBonds = new Bond[1];
      newBonds[0] = newBond;
    } else {
      int oldLength = oldBonds.length;
      int newLength = oldLength + 1;
      if (newLength < MAX_BONDS_LENGTH_TO_CACHE && numCached[newLength] > 0)
        newBonds = freeBonds[newLength][--numCached[newLength]];
      else
        newBonds = new Bond[newLength];
      newBonds[oldLength] = newBond;
      for (int i = oldLength; --i >= 0;)
        newBonds[i] = oldBonds[i];
      if (oldLength < MAX_BONDS_LENGTH_TO_CACHE
          && numCached[oldLength] < MAX_NUM_TO_CACHE)
        freeBonds[oldLength][numCached[oldLength]++] = oldBonds;
    }
    return newBonds;
  }

  ////// bonding methods //////

  public int addHBond(Atom atom1, Atom atom2, int order, double energy) {
    // from autoHbond and BioModel.getRasmolHydrogenBonds
    if (bondCount == bo.length)
      bo = (Bond[]) AU.arrayCopyObject(bo, bondCount
          + BOND_GROWTH_INCREMENT);
    return setBond(bondCount++, bondMutually(atom1, atom2, order, (short) 1,
        energy)).index;
  }

  protected void deleteAllBonds2() {
    vwr.setShapeProperty(JC.SHAPE_STICKS, "reset", null);
    for (int i = bondCount; --i >= 0;) {
      bo[i].deleteAtomReferences();
      bo[i] = null;
    }
    bondCount = 0;
  }
  

  /**
   * When creating a new bond, determine bond diameter from order
   * 
   * @param order
   * @return if hydrogen bond, default to 1; otherwise 0 (general default)
   */
  public short getDefaultMadFromOrder(int order) {
    return (short) (Edge.isOrderH(order) ? 1
        : order == Edge.BOND_STRUT  ? (int) Math.floor(vwr
            .getDouble(T.strutdefaultradius) * 2000) : defaultCovalentMad);
  }

  protected int[] deleteConnections(double minD, double maxD, int order,
                                    BS bsA, BS bsB, boolean isBonds,
                                    boolean matchNull) {
    boolean minDIsFraction = (minD < 0);
    boolean maxDIsFraction = (maxD < 0);
    boolean isFractional = (minDIsFraction || maxDIsFraction);
    minD = fixD(minD, minDIsFraction);
    maxD = fixD(maxD, maxDIsFraction);
    BS bsDelete = new BS();
    int nDeleted = 0;
    int newOrder = order |= Edge.BOND_NEW;
    if (!matchNull && Edge.isOrderH(order))
      order = Edge.BOND_HYDROGEN_MASK;
    BS bsBonds;
    if (isBonds) {
      bsBonds = bsA;
    } else {
      bsBonds = new BS();
      for (int i = bsA.nextSetBit(0); i >= 0; i = bsA.nextSetBit(i + 1)) {
        Atom a = at[i];
        if (a.bonds != null)
          for (int j = a.bonds.length; --j >= 0;)
            if (bsB.get(a.getBondedAtomIndex(j)))
              bsBonds.set(a.bonds[j].index);
      }
    }
    for (int i = bsBonds.nextSetBit(0); i < bondCount && i >= 0; i = bsBonds
        .nextSetBit(i + 1)) {
      Bond bond = bo[i];
      if (!isInRange(bond.atom1, bond.atom2, minD, maxD, minDIsFraction, maxDIsFraction, isFractional))
        continue;
      if (matchNull
          || newOrder == (bond.order & ~Edge.BOND_SULFUR_MASK | Edge.BOND_NEW)
          || (order & bond.order & Edge.BOND_HYDROGEN_MASK) != 0) {
        bsDelete.set(i);
        nDeleted++;
      }
    }
    if (nDeleted > 0)
      ((ModelSet) this).deleteBonds(bsDelete, false);
    return new int[] { 0, nDeleted };
  }
  
  protected double fixD(double d, boolean isF) {
    return (isF ? -d : d * d);
  }

  /**
   * Consider the min/max option for CONNECT
   * 
   * @param atom1
   * @param atom2
   * @param minD
   * @param maxD
   * @param minFrac
   * @param maxfrac
   * @param isFractional
   * @return true if in range
   */
  protected boolean isInRange(Atom atom1, Atom atom2, double minD, double maxD,
                            boolean minFrac, boolean maxfrac,
                            boolean isFractional) {
    double d2 = atom1.distanceSquared(atom2);
    if (isFractional) {
      double dAB = Math.sqrt(d2);
      double dABcalc = atom1.getBondingRadius() + atom2.getBondingRadius();
      return ((minFrac ? dAB >= dABcalc * minD : d2 >= minD)
          && (maxfrac ? dAB <= dABcalc * maxD : d2 <= maxD));
    } 
    return (d2 >= minD && d2 <= maxD);
  }

  protected void dBb(BS bsBond, boolean isFullModel) {
    int iDst = bsBond.nextSetBit(0);
    if (iDst < 0)
      return;
    ((ModelSet) this).resetMolecules();
    int modelIndexLast = -1;
    int n = bsBond.cardinality();
    for (int iSrc = iDst; iSrc < bondCount; ++iSrc) {
      Bond bond = bo[iSrc];
      if (n > 0 && bsBond.get(iSrc)) {
        n--;
        if (!isFullModel) {
          int modelIndex = bond.atom1.mi;
          if (modelIndex != modelIndexLast)
            ((ModelSet) this).am[modelIndexLast = modelIndex]
                .resetBoundCount();
        }
        bond.deleteAtomReferences();
      } else {
        setBond(iDst++, bond);
      }
    }
    for (int i = bondCount; --i >= iDst;)
      bo[i] = null;
    bondCount = iDst;
    BS[] sets = (BS[]) vwr.getShapeProperty(
        JC.SHAPE_STICKS, "sets");
    if (sets != null)
      for (int i = 0; i < sets.length; i++)
        BSUtil.deleteBits(sets[i], bsBond);
    BSUtil.deleteBits(bsAromatic, bsBond);
  }


  /*
   * aromatic single/double bond assignment 
   * by Bob Hanson, hansonr@stolaf.edu, Oct. 2007
   * Jmol 11.3.29.
   * 
   * This algorithm assigns alternating single/double bonds to all 
   * sets of bonds of type AROMATIC in a system. Any bonds already
   * assigned AROMATICSINGLE or AROMATICDOUBLE by the user are preserved.
   * 
   * In this way the user can assign ONE bond, and Jmol will take it from
   * there.
   * 
   * The algorithm is highly recursive.
   * 
   * We track two bond bitsets: bsAromaticSingle and bsAromaticDouble.
   *  
   * Loop through all aromatic bonds. 
   *   If unassigned, assignAromaticDouble(Bond bond).
   *   If unsuccessful, assignAromaticSingle(Bond bond).
   * 
   * assignAromaticDouble(Bond bond):
   * 
   *   Each of the two atoms must have exactly one double bond.
   *   
   *   bsAromaticDouble.set(thisBond)
   *   
   *   For each aromatic bond connected to each atom that is not
   *   already assigned AROMATICSINGLE or AROMATICDOUBLE:
   *   
   *     assignAromaticSingle(Bond bond)
   *     
   *   If unsuccessful, bsAromaticDouble.clear(thisBond) and 
   *   return FALSE, otherwise return TRUE.
   * 
   * assignAromaticSingle(Bond bond):
   * 
   *   Each of the two atoms must have exactly one double bond.
   *   
   *   bsAromaticSingle.set(thisBond)
   *   
   *   For each aromatic bond connected to this atom that is not
   *   already assigned:
   *   
   *     for one: assignAromaticDouble(Bond bond) 
   *     the rest: assignAromaticSingle(Bond bond)
   *     
   *   If two AROMATICDOUBLE bonds to the same atom are found
   *   or unsuccessful in assigning AROMATICDOUBLE or AROMATICSINGLE, 
   *   bsAromaticSingle.clear(thisBond) and 
   *   return FALSE, otherwise return TRUE.
   *   
   * The process continues until all bonds are processed. It is quite
   * possible that the first assignment will fail either because somewhere
   * down the line the user has assigned an incompatible AROMATICDOUBLE or
   * AROMATICSINGLE bond. 
   * 
   * This is no problem though, because the assignment is self-correcting, 
   * and in the second pass the process will be opposite, and success will
   * be achieved.
   * 
   * It is possible that no correct assignment is possible because the structure
   * has no valid closed-shell Lewis structure. In that case, AROMATICSINGLE 
   * bonds will be assigned to problematic areas.  
   * 
   * Bob Hanson -- 10/2007
   * 
   */

  public void resetAromatic() {
    for (int i = bondCount; --i >= 0;) {
      Bond bond = bo[i];
      if (bond.isAromatic())
        bond.setOrder(Edge.BOND_AROMATIC);
    }
  }
  
  /**
   * algorithm discussed above.
   * 
   * @param isUserCalculation
   *        if set, don't reset the base aromatic bitset and do report changes
   *        to STICKS as though this were a bondOrder command.
   * @param bsBonds
   *        passed to us by autoBond routine
   */
  public void assignAromaticBondsBs(boolean isUserCalculation, BS bsBonds) {
    // bsAromatic tracks what was originally in the file, but
    // individual bonds are cleared if the connect command has been used.
    // in this way, users can override the file designations.
    if (!isUserCalculation)
      bsAromatic = new BS();

    //set up the two temporary bitsets and reset bonds.

    bsAromaticSingle = new BS();
    bsAromaticDouble = new BS();
    boolean isAll = (bsBonds == null);
    int i0 = (isAll ? bondCount - 1 : bsBonds.nextSetBit(0));
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bsBonds.nextSetBit(i + 1))) {
      Bond bond = bo[i];
      if (bsAromatic.get(i))
        bond.setOrder(Edge.BOND_AROMATIC);
      switch (bond.order & Edge.BOND_RENDER_MASK) {
      case Edge.BOND_AROMATIC:
        if (!assignAromaticMustBeSingle(bond.atom1)
            && !assignAromaticMustBeSingle(bond.atom2)) {
          bsAromatic.set(i);
          break;
        }
        bond.order = Edge.BOND_AROMATIC_SINGLE;
        //$FALL-THROUGH$
      case Edge.BOND_AROMATIC_SINGLE:
        bsAromaticSingle.set(i);
        break;
      case Edge.BOND_AROMATIC_DOUBLE:
        bsAromaticDouble.set(i);
        break;
      }
    }
    // main recursive loop
    Bond bond;
    isAll = (bsBonds == null);
    i0 = (isAll ? bondCount - 1 : bsBonds.nextSetBit(0));
    BS bsTest = new BS();
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bsBonds.nextSetBit(i + 1))) {
      bond = bo[i];
      if (!bond.is(Edge.BOND_AROMATIC) || bsAromaticDouble.get(i)
          || bsAromaticSingle.get(i))
        continue;
      bsTest.set(i);
      if (bond.atom1.getElementNumber() == 8
          || bond.atom2.getElementNumber() == 8) {
        if (!assignAromaticDouble(bond))
          assignAromaticSingle(bond);
//?        } else {
//          bsTest.set(i);
      }
    }
    // now test non-O atoms
    for (int i = bsTest.nextSetBit(0); i >= 0; i = bsTest.nextSetBit(i + 1))
      if (!assignAromaticDouble(bond = bo[i]))
        assignAromaticSingle(bond);
    // all done: do the actual assignments and clear arrays.
    BS bsModels = new BS();
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bsBonds.nextSetBit(i + 1))) {
      bond = bo[i];
      if (bsAromaticDouble.get(i)) {
        if (!bond.is(Edge.BOND_AROMATIC_DOUBLE)) {
          bsAromatic.set(i);
          bsModels.set(bond.atom1.mi);
          bond.setOrder(isLinear(bond, v1, v2) ? Edge.BOND_COVALENT_TRIPLE : Edge.BOND_AROMATIC_DOUBLE);
        }
      } else if (bsAromaticSingle.get(i) || bond.isAromatic()) {
        if (!bond.is(Edge.BOND_AROMATIC_SINGLE)) {
          bsAromatic.set(i);
          bond.setOrder(Edge.BOND_AROMATIC_SINGLE);
        }
      }
    }
    Model[] models = ((ModelSet) this).am;
    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1))
      if (models[i].isBioModel)
        models[i].isPdbWithMultipleBonds = true;

    
    assignAromaticNandO(bsBonds);

    bsAromaticSingle = null;
    bsAromaticDouble = null;

    ///////
    // This was in the former method in ModelSet, which was not accessible:
    //
    // send a message to STICKS indicating that these bonds
    // should be part of the state of the model. They will 
    // appear in the state as bondOrder commands.    
    //if (isUserCalculation)
    //shapeManager.setShapeSizeBs(JC.SHAPE_STICKS, Integer.MIN_VALUE, null, bsAromatic);
    ///////

  }
  
  private boolean isLinear(Bond b, V3d v1, V3d v2) {
    if (b.order == Edge.BOND_COVALENT_TRIPLE)
      return true;
    if (b.atom1.getCovalentBondCount() != 2 || b.atom2.getCovalentBondCount() != 2)
      return false;
    Edge[] edges = b.atom1.getEdges();
    for (int i = edges.length; -i >= 0;) {
      if (edges[i] != b && edges[i].isCovalent()) {
        if (MeasureD.computeAngle((Atom) edges[i].getOtherNode(b.atom1), b.atom1, b.atom2, v1, v2, true) < 175)
            return false;
        break;
      }
    }
    edges = b.atom2.getEdges();
    for (int i = edges.length; -i >= 0;) {
      if (edges[i] != b && edges[i].isCovalent()) {
        if (MeasureD.computeAngle((Atom) edges[i].getOtherNode(b.atom2), b.atom2, b.atom1, v1, v2, true) < 175)
            return false;
        break;
      }
    }
    return true;
  }


  /**
   * try to assign AROMATICDOUBLE to this bond. Each atom needs to be
   * have all single bonds except for this one.  
   * 
   * @param bond
   * @return      true if successful; false otherwise
   */
  private boolean assignAromaticDouble(Bond bond) {
    int bondIndex = bond.index;
    if (bsAromaticSingle.get(bondIndex))
      return false;
    if (bsAromaticDouble.get(bondIndex))
      return true;
    bsAromaticDouble.set(bondIndex);
    if (!assignAromaticSingleForAtom(bond.atom1, bondIndex)
        || !assignAromaticSingleForAtom(bond.atom2, bondIndex)) {
      bsAromaticDouble.clear(bondIndex);
      return false;
    }
    return true;
  }
  
  /**
   * try to assign AROMATICSINGLE to this bond. Each atom needs to be
   * able to have one aromatic double bond attached.  
   * 
   * @param bond
   * @return      true if successful; false otherwise
   */
  private boolean assignAromaticSingle(Bond bond) {
    int bondIndex = bond.index;
    if (bsAromaticDouble.get(bondIndex))
      return false;
    if (bsAromaticSingle.get(bondIndex))
      return true;
    bsAromaticSingle.set(bondIndex);
    if (!assignAromaticDoubleForAtom(bond.atom1) || !assignAromaticDoubleForAtom(bond.atom2)) {
      bsAromaticSingle.clear(bondIndex);
      return false;
    }
    return true;
  }

  /**
   * N atoms with 3 bonds cannot also have a double bond; 
   * other atoms needs all single bonds, 
   * because the bond leading up to it is double.
   * 
   * @param atom
   * @param notBondIndex  that index of the bond leading to this atom --- to be ignored
   * @return      true if successful, false if not
   */
  private boolean assignAromaticSingleForAtom(Atom atom, int notBondIndex) {
    Bond[] bonds = atom.bonds;
    if (bonds == null)// || assignAromaticMustBeSingle(atom))
      return false;
    for (int i = bonds.length; --i >= 0;) {
      Bond bond = bonds[i];
      int bondIndex = bond.index;
      if (bondIndex == notBondIndex || !bond.isAromatic()
          || bsAromaticSingle.get(bondIndex))
        continue;
      if (bsAromaticDouble.get(bondIndex) || !assignAromaticSingle(bond)) {
        return false;
      }
    }
    return true;
  }
 
  /**
   * N atoms with 3 bonds cannot also have a double bond; 
   * other atoms need one and only one double bond;
   * the rest must be single bonds.
   * 
   * @param atom
   * @return      true if successful, false if not
   */
  private boolean assignAromaticDoubleForAtom(Atom atom) {
    Bond[] bonds = atom.bonds;
    if (bonds == null)
      return false;
    boolean haveDouble = false;//assignAromaticMustBeSingle(atom);
    int lastBond = -1;
    for (int i = bonds.length; --i >= 0;) {
      if (bsAromaticDouble.get(bonds[i].index))
        haveDouble = true;
      if (bonds[i].isAromatic())
        lastBond = i;
    }
    for (int i = bonds.length; --i >= 0;) {
      Bond bond = bonds[i];
      int bondIndex = bond.index;
      if (!bond.isAromatic() || bsAromaticDouble.get(bondIndex)
          || bsAromaticSingle.get(bondIndex))
        continue;
      if (!haveDouble && assignAromaticDouble(bond))
        haveDouble = true;
      else if ((haveDouble || i < lastBond) && !assignAromaticSingle(bond)) {
        return false;
      }
    }
    return haveDouble;
  } 
  
  protected boolean allowAromaticBond(Bond b) {
    if (assignAromaticMustBeSingle(b.atom1)
        || assignAromaticMustBeSingle(b.atom2))
      return false;
    switch (b.getCovalentOrder()) {
    case Edge.BOND_COVALENT_SINGLE:
    case Edge.BOND_COVALENT_DOUBLE:
      return b.atom1.getCovalentBondCount() <= 3 && b.atom2.getCovalentBondCount() <= 3;
    default:
      return false;
    }
  }
  
  private boolean assignAromaticMustBeSingle(Atom atom) {
    // only C N O S may be a problematic:
    int n = atom.getElementNumber();
    switch (n) {
    case 6: // C
    case 7: // N
    case 8: // O
    case 16: // S
      break;
    default:
      return true;
    }
    int valence = atom.getValenceAromatic(false);
    switch (n) {
    case 6: // C
      return (valence == 4);
    case 7: // N
      return (atom.group.getNitrogenAtom() == atom || valence == 3 && atom.getFormalCharge() < 1);
    case 8: // O
      return (atom.group.getCarbonylOxygenAtom() != atom && valence == 2 && atom.getFormalCharge() < 1);
    case 16: // S
      return (atom.group.groupID == JC.GROUPID_CYSTEINE || valence == 2 && atom.getFormalCharge() < 1);
    }
    return false;
  }
  
  private void assignAromaticNandO(BS bsSelected) {
    Bond bond;
    boolean isAll = (bsSelected == null);
    int i0 = (isAll ? bondCount - 1 : bsSelected.nextSetBit(0));
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bsSelected.nextSetBit(i + 1))) {
        bond = bo[i];
        if (!bond.is(Edge.BOND_AROMATIC_SINGLE))
          continue;
        Atom atom1;
        Atom atom2 = bond.atom2;
        int n1;
        int n2 = atom2.getElementNumber();
        if (n2 == 7 || n2 == 8) {
          n1 = n2;
          atom1 = atom2;
          atom2 = bond.atom1;
          n2 = atom2.getElementNumber();
        } else {
          atom1 = bond.atom1;
          n1 = atom1.getElementNumber();
        }
        if (n1 != 7 && n1 != 8)
          continue;
        int valence = atom1.getValence();
        if (valence < 0)
          continue; // deleted
        int bondorder = atom1.getCovalentBondCount();
        int charge = atom1.getFormalCharge();
        switch (n1) {
        case 7:
          //trivalent nonpositive N with lone pair in p orbital
          //next to trivalent C --> N=C
          if (valence == 3 && bondorder == 3 && charge < 1 && n2 == 6
              && atom2.getValence() == 3)
            bond.setOrder(Edge.BOND_AROMATIC_DOUBLE);
          break;
        case 8:
          //monovalent nonnegative O next to P or S
          if (valence == 1 && charge == 0 && (n2 == 14 || n2 == 16))
            bond.setOrder(Edge.BOND_AROMATIC_DOUBLE);
          break;
        }
      }
  }

  protected BS getAtomBitsMDb(int tokType, Object specInfo) {
    BS bs = new BS();
    switch (tokType) {
    default:
      return getAtomBitsMDa(tokType, specInfo, bs);
    case T.bonds:
      BS bsBonds = (BS) specInfo;
      for (int i = bsBonds.nextSetBit(0); i >= 0; i = bsBonds.nextSetBit(i + 1)) {
        if (i < bondCount) {
          bs.set(bo[i].atom1.i);
          bs.set(bo[i].atom2.i);
        } else {
          bsBonds.clear(i);
        }
      }
      return bs;
    case T.isaromatic:
      for (int i = bondCount; --i >= 0;)
        if (bo[i].isAromatic()) {
          bs.set(bo[i].atom1.i);
          bs.set(bo[i].atom2.i);
        }
      return bs;
    }
  }
 
  public void removeUnnecessaryBonds(Atom atom, boolean deleteAtom) {
    BS bs = new BS();
    BS bsBonds = new BS();
    Bond[] bonds = atom.bonds;
    if (bonds == null)
      return;
    for (int i = 0; i < bonds.length; i++)
      if (bonds[i].isCovalent()) {
        Atom atom2 = bonds[i].getOtherAtom(atom);
        if (atom2.getElementNumber() == 1)
          bs.set(bonds[i].getOtherAtom(atom).i);
      } else {
        bsBonds.set(bonds[i].index);
      }
    if (bsBonds.nextSetBit(0) >= 0)
      ((ModelSet) this).deleteBonds(bsBonds, false);
    if (deleteAtom)
      bs.set(atom.i);
    if (bs.nextSetBit(0) >= 0)
      vwr.deleteAtoms(bs, false);
  }
  
  public void displayBonds(BondSet bs, boolean isDisplay) {
    if (!isDisplay)
      haveHiddenBonds = true;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
      if (i < bondCount && bo[i].mad != 0)
        bo[i].setShapeVisibility(isDisplay);
  }

  public BS getAtomsConnected(double min, double max, int intType, BS bs) {
    boolean isBonds = bs instanceof BondSet;
    BS bsResult = (isBonds ? new BondSet() : new BS());
    int[] nBonded = new int[ac];
    int i;
    boolean ishbond = (intType == Edge.BOND_HYDROGEN_MASK);
    boolean isall = (intType == Edge.BOND_ORDER_ANY);
    for (int ibond = 0; ibond < bondCount; ibond++) {
      Bond bond = bo[ibond];
      if (isall || bond.is(intType) || ishbond && bond.isHydrogen()) {
        if (isBonds) {
          bsResult.set(ibond);
        } else {
        if (bs.get(bond.atom1.i)) {
          nBonded[i = bond.atom2.i]++;
          bsResult.set(i);
        }
        if (bs.get(bond.atom2.i)) {
          nBonded[i = bond.atom1.i]++;
          bsResult.set(i);
        }
        }
      }
    }
    if (isBonds)
      return bsResult;
    boolean nonbonded = (min == 0);
    for (i = ac; --i >= 0;) {
      int n = nBonded[i];
      if (at[i] == null || n < min || n > max)
        bsResult.clear(i);
      else if (nonbonded && n == 0)
        bsResult.set(i);
    }
    return bsResult;
  }

  protected boolean haveAtropicBonds;
  
//  public void setAtropicBonds(BS bs) {
//    if (!haveAtropicBonds)
//      return;
//    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
//      Atom a = at[i];
//      Bond[] bonds = a.bonds;
//      if (bonds == null)
//        continue;
//      for (int j = a.getBondCount(); --j >= 0;) {
//        Bond b = bonds[j];
//        if ((b.order & Edge.TYPE_ATROPISOMER) == Edge.TYPE_ATROPISOMER) {
//          
//        }
//      }
//    }
//  }
//


}

