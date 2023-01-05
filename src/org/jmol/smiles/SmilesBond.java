/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2017-05-27 15:31:36 -0500 (Sat, 27 May 2017) $
 * $Revision: 21620 $
 *
 * Copyright (C) 2005  The Jmol Development Team
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

package org.jmol.smiles;

import org.jmol.util.Edge;
import org.jmol.util.SimpleNode;

/**
 * Bond in a SmilesMolecule
 */
public class SmilesBond extends Edge {

  // Bond orders
  // See also Edge
  public final static int TYPE_UNKNOWN = -1;
  public final static int TYPE_NONE = 0;
  public final static int TYPE_AROMATIC = 0x11;
  public final static int TYPE_RING = 0x41;
  public final static int TYPE_ANY = 0x51;
  public final static int TYPE_BIO_SEQUENCE = 0x60;
  public final static int TYPE_BIO_CROSSLINK = 0x70;
  
  // NOTE: ` is reserved for atropisomer ^^ conversion; ~ is for Jmol bioSMARTS
  
  private static final String ALL_BONDS =    "-=#$:/\\.~^`+!,&;@"; // >> for reaction --> .  
  private static final String SMILES_BONDS = "-=#$:/\\.~^`";  

  static String getBondOrderString(int order) {
    switch (order) {
    case 2:
      return "=";
    case 3:
      return "#";
    case 4:
      return "$";
    default:
      return "";
    }
  }

  /**
   * @param code Bond code
   * @return Bond type
   */
  static int getBondTypeFromCode(char code) {
    switch (code) {
    case '.':
      return TYPE_NONE;
    case '-':
      return BOND_COVALENT_SINGLE;
    case '=':
      return BOND_COVALENT_DOUBLE;
    case '#':
      return BOND_COVALENT_TRIPLE;
    case '$':
      return BOND_COVALENT_QUADRUPLE;
    case ':':
      return TYPE_AROMATIC;
    case '/':
      return BOND_STEREO_NEAR;
    case '\\':
      return BOND_STEREO_FAR;
    case '^':
      return TYPE_ATROPISOMER;
    case '`': // replacement for ^^
      return TYPE_ATROPISOMER_REV;
    case '@':
      return TYPE_RING;
    case '~':
      return TYPE_ANY;
    case '+':
      return TYPE_BIO_SEQUENCE;
    }
    return TYPE_UNKNOWN;
  }

  SmilesAtom atom1;
  SmilesAtom atom2;

  public SmilesAtom getAtom1() {
    return atom1;
  }
  
  boolean isNot;
  Edge matchingBond;

  private SmilesBond[] primitives;
  int nPrimitives;
  SmilesBond[] bondsOr;
  int nBondsOr;
  boolean isConnection;
  int[] atropType; // 1 1,1 2,1 3,2 1,2 2,2 3
  public boolean isChain; // direct connection

  void set(SmilesBond bond) {
    // not the atoms.
    order = bond.order;
    isNot = bond.isNot;
    primitives = bond.primitives;
    nPrimitives = bond.nPrimitives;
    bondsOr = bond.bondsOr;
    nBondsOr = bond.nBondsOr;
    atropType = bond.atropType;
  }

  void setAtropType(int nn) {
    atropType = new int[] {nn/10 -1, nn %10 - 1};
  }

  public SmilesBond setPrimitive(int i) {
    SmilesBond p = primitives[i];
    order = p.order;
    isNot = p.isNot;
    atropType = p.atropType;
    return p;
  }
  
  SmilesBond addBondOr() {
    if (bondsOr == null)
      bondsOr = new SmilesBond[2];
    if (nBondsOr >= bondsOr.length) {
      SmilesBond[] tmp = new SmilesBond[bondsOr.length * 2];
      System.arraycopy(bondsOr, 0, tmp, 0, bondsOr.length);
      bondsOr = tmp;
    }
    SmilesBond sBond = new SmilesBond(null, null, TYPE_UNKNOWN, false);
    bondsOr[nBondsOr] = sBond;
    nBondsOr++;
    return sBond;
  }

  SmilesBond addPrimitive() {
    if (primitives == null)
      primitives = new SmilesBond[2];
    if (nPrimitives >= primitives.length) {
      SmilesBond[] tmp = new SmilesBond[primitives.length * 2];
      System.arraycopy(primitives, 0, tmp, 0, primitives.length);
      primitives = tmp;
    }
    SmilesBond sBond = new SmilesBond(null, null, TYPE_UNKNOWN, false);
    primitives[nPrimitives] = sBond;
    nPrimitives++;
    return sBond;
  }

  @Override
  public String toString() {
    return atom1 + " -" + (isNot ? "!" : "") + order + "- " + atom2;
  }

  /**
   * SmilesBond constructor
   * 
   * @param atom1 First atom
   * @param atom2 Second atom
   * @param bondType Bond type
   * @param isNot 
   */
  public SmilesBond(SmilesAtom atom1, SmilesAtom atom2, int bondType,
      boolean isNot) {
    set2(bondType, isNot);
    set2a(atom1, atom2);
  }

  public void set2(int bondType, boolean isNot) {
    order = bondType;
    this.isNot = isNot;
  }

  void set2a(SmilesAtom a1, SmilesAtom a2) {
    if (a1 != null) {
      atom1 = a1;
      a1.addBond(this);
    }
    if (a2 != null) {
      atom2 = a2;
      if (a2.isBioAtomWild && atom1.isBioAtomWild)
        order = TYPE_BIO_SEQUENCE;
      a2.isFirst = false;
      a2.addBond(this);
    }
  }

  /**
   * from parse ring
   * @param atom
   * @param molecule 
   */
  void setAtom2(SmilesAtom atom, SmilesSearch molecule) {
    atom2 = atom;
    if (atom2 != null) {
      atom.addBond(this);
      isConnection = true;
    }
  }

  /**
   * Check to see if this is the bond to the previous atom
   * 
   * @param atom
   * @return TRUE if other atom is previous atom
   */
  boolean isFromPreviousTo(SmilesAtom atom) {
    return (!isConnection && atom2 == atom);
 }

  static int isBondType(char ch, boolean isSearch, boolean isBioSequence)
      throws InvalidSmilesException {
    if (ch == '>')
      return 1;
    if (ALL_BONDS.indexOf(ch) < 0)
      return 0;
    if (!isSearch && SMILES_BONDS.indexOf(ch) < 0)
      throw new InvalidSmilesException("SMARTS bond type " + ch
          + " not allowed in SMILES");
    switch (ch) {
    case '~':
      return(isBioSequence? 0 : 1);
    case '^':
    case '`':
      return -1;
    default:
      return 1;
    }
  }

  public int getValence() {
    return (order & 7);
  }

  public SmilesAtom getOtherAtom(SmilesAtom a) {
    return (atom1 == a ? atom2 : atom1);
  }

  @Override
  public int getAtomIndex1() {
    return atom1.index;
  }

  @Override
  public int getAtomIndex2() {
    return atom2.index;
  }

  @Override
  public int getCovalentOrder() {
    return order & BOND_RENDER_MASK;
  }

  @Override
  public SimpleNode getOtherNode(SimpleNode atom) {
    return (atom == atom1 ? atom2 : atom == atom2 || atom == null ? atom1 : null);
  }

  @Override
  public boolean isPartial() {
    return false;
  }

  @Override
  public boolean isCovalent() {
    return order != TYPE_BIO_CROSSLINK;
  }

  @Override
  public boolean isHydrogen() {
    return order == TYPE_BIO_CROSSLINK;
  }

  /**
   * Ensure that atom ordering is proper.
   * 
   * possibly not fully tested
   * 
   */
  void switchAtoms() {
    SmilesAtom a = atom1;
    atom1 = atom2;
    atom2 = a;
    switch (order & BOND_RENDER_MASK) {
    case TYPE_ATROPISOMER:
      order = TYPE_ATROPISOMER_REV;
      break;
    case TYPE_ATROPISOMER_REV:
      order = TYPE_ATROPISOMER;
      break;
    case BOND_STEREO_NEAR:
      order = BOND_STEREO_FAR;
      break;
    case BOND_STEREO_FAR:
      order = BOND_STEREO_NEAR;
      break;
    }
  }

  public int getRealCovalentOrder() {
    switch (order & BOND_RENDER_MASK) {
    case TYPE_ATROPISOMER:
    case TYPE_ATROPISOMER_REV:
    case BOND_STEREO_NEAR:
    case BOND_STEREO_FAR:
      return BOND_COVALENT_SINGLE;
    }
    return order & BOND_RENDER_MASK;
  }

  public Edge getMatchingBond() {
    return matchingBond == null ? this : matchingBond;
  }

  @Override
  public SimpleNode getAtom(int i) {
    return (i == 1 ? atom2 : atom1);
  }

}
