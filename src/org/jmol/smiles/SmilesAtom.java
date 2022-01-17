/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2018-02-22 12:04:47 -0600 (Thu, 22 Feb 2018) $
 * $Revision: 21841 $
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

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.P3;

import javajs.util.BS;
import org.jmol.util.Edge;
import org.jmol.util.Elements;
import org.jmol.util.Logger;
import org.jmol.util.Node;
import org.jmol.util.SimpleNode;
import org.jmol.viewer.JC;

//import org.jmol.util.Logger;

/**
 * This class represents an atom in a <code>SmilesMolecule</code>.
 */
public class SmilesAtom extends P3 implements Node {

  //Jmol allows * in SMILES as a wild card
  static final String UNBRACKETED_SET = "B, C, N, O, P, S, F, Cl, Br, I, *,"; 
  
  static boolean allowSmilesUnbracketed(String xx) {
    return (UNBRACKETED_SET.indexOf(xx + ",") >= 0);
  }

  public SmilesAtom() {
  }

  int patternIndex = -1;
  
  String pattern;
  int primitiveType;
  
  boolean isAND;
  SmilesAtom[] subAtoms;
  int nSubAtoms;

  int index;

  String referance;
  String residueName, residueChar;
  char insCode;
  boolean isBioAtom;
  boolean isBioResidue;
  boolean isBioAtomWild;
  char bioType = '\0'; //* p n r d c
  boolean isLeadAtom;
  int notBondedIndex = -1;
  boolean notCrossLinked;
  boolean aromaticAmbiguous = true;

  private int covalentHydrogenCount = -1;
  
  boolean not;
  boolean selected;
  boolean hasSymbol, elementDefined;
  
  String atomType;
  String bioAtomName;

  @Override
  public String getAtomType() {
    return (atomType == null ? bioAtomName : atomType);
  }

  /**
   * true if this atom is the first SMILES atom or first after a . with no connector
   */
  boolean isFirst = true;

  int jmolIndex = -1;
  int elementNumber = -2; // UNDEFINED (could be A or a or *)
  int atomNumber = Integer.MIN_VALUE; // PDB atom number Jmol 14.3.16
  int residueNumber = Integer.MIN_VALUE; // PDB residue h Jmol 14.3.16

  int explicitHydrogenCount = Integer.MIN_VALUE;
  // for a pattern atom, this is the "h<n>" number; for a topomap, this is the actual 
  // number of missing hydrogens.
  int implicitHydrogenCount = Integer.MIN_VALUE;
  SmilesAtom parent;
  SmilesBond[] bonds = new SmilesBond[4];
  int bondCount;
  int iNested = 0;
  boolean isAromatic;


  private int atomicMass = Integer.MIN_VALUE;
  private int charge = Integer.MIN_VALUE;
  private int matchingIndex = -1;
  public SmilesStereo stereo;
  
  public int getChiralClass() {
    return (stereo == null ? Integer.MIN_VALUE : stereo.getChiralClass(this));
  }

  public boolean isDefined() {
    return (hasSubpattern || iNested != 0 || isBioAtom || component != Integer.MIN_VALUE 
    || elementNumber != -2 || nSubAtoms > 0);  
  }

  void setBioAtom(char bioType) {
    isBioAtom = (bioType != '\0');
    this.bioType = bioType;
    if (parent != null) {
      parent.bioType = bioType;
      parent.isBioAtom = isBioAtom;
      parent.isBioAtomWild = isBioAtomWild;
    }
  }

  void setAtomName(String name) {
    if (name == null)
      return;
    if (name.length() > 0)
      bioAtomName = name;
    if (name.equals("\0"))
      isLeadAtom = true;
    // ensure that search does not skip groups
    if (parent != null) {
      parent.bioAtomName = name;
    }
  }

  public void setBonds(SmilesBond[] bonds) {
    this.bonds = bonds;
  }

  SmilesAtom addSubAtom(SmilesAtom sAtom, boolean isAND) {
    this.isAND = isAND;
    if (subAtoms == null)
      subAtoms = new SmilesAtom[2];
    if (nSubAtoms >= subAtoms.length)
      subAtoms = (SmilesAtom[]) AU.doubleLength(subAtoms);
    sAtom.setIndex(index);
    sAtom.parent = this;
    subAtoms[nSubAtoms++] = sAtom;
    setSymbol("*");
    hasSymbol = false;
    return sAtom;
  }
  /**
   * Constructs a <code>SmilesAtom</code>.
   * 
   * @param index Atom number in the molecule. 
   * @return this
   */
  public SmilesAtom setIndex(int index) {
    this.index = index;
    return this;
  }

  int component = Integer.MIN_VALUE;
  int matchingComponent;
  int atomSite;
  int degree = -1;
  int nonhydrogenDegree = -1;
  int valence = 0;
  int connectivity = -1;
  int ringMembership = Integer.MIN_VALUE;
  int ringSize = Integer.MIN_VALUE;
  int ringConnectivity = -1;
  private Node matchingNode;
  boolean hasSubpattern;
  int mapIndex = -1; // in  CCC we have atoms 0, 1, and 2
  float atomClass = Float.NaN; // OpenSMILES atom class is an integer
  String symbol;
  private boolean isTopoAtom;
  private int missingHydrogenCount;
  private int cipChirality;

  public SmilesAtom setTopoAtom(int iComponent, int ptAtom, String symbol,
      int charge, int patternIndex) {
    component = iComponent;
    index = ptAtom;
    this.patternIndex = patternIndex;
    setSymbol(symbol);
    this.charge = charge;
    isTopoAtom = true;
    return this;
  }

  /**
   * Finalizes the hydrogen count hydrogens in a <code>SmilesMolecule</code>.
   * "missing" here means the number of atoms not present in the SMILES string
   * for unbracketed atoms or the number of hydrogen atoms "CC" being really CH3CH3
   * or explicitly mentioned in the bracketed atom, "[CH2]". These hydrogen atoms
   * are not part of the topological model constructed and need to be accounted for. 
   * 
   * @return false if inappropriate
   */
  public boolean setHydrogenCount() {
    // only called for SMILES search -- simple C or [C]
    missingHydrogenCount = explicitHydrogenCount;
    if (explicitHydrogenCount != Integer.MIN_VALUE)
      return true;
    // Determining max count
    int count = getDefaultCount(elementNumber, isAromatic);
    if (count < 0) {
      missingHydrogenCount = 0;
      return (count == -1);
    }

    if (elementNumber == 7 && isAromatic && bondCount == 2) {
      // is it -N= or -NH- ? 
      if (bonds[0].getBondType() == Edge.BOND_COVALENT_SINGLE
           && bonds[1].getBondType() == Edge.BOND_COVALENT_SINGLE)
        count++;
    }
    for (int i = 0; i < bondCount; i++) {
      SmilesBond bond = bonds[i];
      switch (bond.getBondType()) {
      case SmilesBond.TYPE_ANY: // for aromatics
        if (elementNumber == 7) {
          Logger.info("Ambiguous bonding to aromatic N found -- MF may be in error");
        }
        count -= 1;
        break;
      case Edge.BOND_STEREO_NEAR:
      case Edge.BOND_STEREO_FAR:
      case Edge.TYPE_ATROPISOMER:
      case Edge.TYPE_ATROPISOMER_REV:
        count -= 1;
        break;
      case Edge.BOND_COVALENT_DOUBLE:
        count -= (isAromatic && elementNumber == 6 ? 1 : 2);
        break;
      case Edge.BOND_COVALENT_SINGLE:
      case Edge.BOND_COVALENT_TRIPLE:
      case Edge.BOND_COVALENT_QUADRUPLE:
        count -= bond.getBondType();
        break;
      }
    }

    if (count >= 0)
      missingHydrogenCount = explicitHydrogenCount = count;
    return true;
  }

  static int getDefaultCount(int elementNumber, boolean isAromatic) {
    // not a complete set...
    // B, C, N, O, P, S, F, Cl, Br, and I
    // B (3), C (4), N (3,5), O (2), P (3,5), S (2,4,6), and 1 for the halogens

    switch (elementNumber) {
    case 0:
    case -1: // A a
    case -2: // *
      return -1;
    case 6: // C
      return (isAromatic ? 3 : 4);
    case 8: // O
    case 16: // S
      return 2;
    case 7: // N
      // note -- it is necessary to indicate explicitly
      // single bonds to aromatic n if a proper MF is desired
      return (isAromatic ? 2 : 3);
    case 5: // B
    case 15: // P
      return 3;
    case 9: // F
    case 17: // Cl
    case 35: // Br
    case 53: // I
      return 1;
    }
    return -2;
  }

  /**
   * Returns the atom index of the atom.
   * 
   * @return Atom index.
   */
  @Override
  public int getIndex() {
    return index;
  }

  /**
   * Sets the symbol of the atm.
   * 
   * @param symbol Atom symbol.
   * @return  false if invalid symbol
   */
  public boolean setSymbol(String symbol) {
    this.symbol = symbol;
    isAromatic = symbol.equals(symbol.toLowerCase());
    hasSymbol = true;
    elementDefined = true;
    if (symbol.equals("*")) {
      isAromatic = false;
      // but isAromaticAmbiguous = true
      elementNumber = -2;
      return true;
    }
    if (symbol.equals("Xx")) {
      // but isAromaticAmbiguous = true
      elementNumber = 0;
      return true;
    }
    aromaticAmbiguous = false;
    if (symbol.equals("a") || symbol.equals("A")) {
      // allow #6a ??
      if (elementNumber < 0)
        elementNumber = -1;
      return true;
    }
    if (isAromatic)
      symbol = symbol.substring(0, 1).toUpperCase()
          + (symbol.length() == 1 ? "" : symbol.substring(1));
    elementNumber = Elements.elementNumberFromSymbol(symbol, true);
    return (elementNumber != 0);
  }

  /**
   *  Returns the atomic number of the element or 0
   * 
   * @return atomicNumber
   */
  @Override
  public int getElementNumber() {
    return elementNumber;
  }

  /**
   * Returns the atomic mass of the atom.
   * 
   * @return Atomic mass.
   */
  public int getAtomicMass() {
    return atomicMass;
  }

  /**
   * Returns the Jmol atom number
   */
  @Override
  public int getAtomNumber() {
    return atomNumber;
  } 

  /**
   * Sets the atomic mass of the atom.
   * 
   * @param mass Atomic mass.
   */
  public void setAtomicMass(int mass) {
    atomicMass = mass;
  }

  /**
   * Returns the charge of the atom.
   * 
   * @return Charge.
   */
  public int getCharge() {
    return charge;
  }

  /**
   * Sets the charge of the atom.
   * 
   * @param charge Charge.
   */
  public void setCharge(int charge) {
    this.charge = charge;
  }

  /**
   * Returns the number of a matching atom in a molecule.
   * This value is temporary, it is used during the pattern matching algorithm.
   * 
   * @return matching atom index
   */
  public int getMatchingAtomIndex() {
    return matchingIndex;
  }

  /**
   * Returns the matching atom or null.
   * @return matching atom
   */
  public Node getMatchingAtom() {
    return matchingNode == null ? this : matchingNode;
  }

  /**
   * Sets the number of a matching atom in a molecule.
   * This value is temporary, it is used during the pattern matching algorithm.
   * @param jmolAtom 
   * 
   * @param index Temporary: number of a matching atom in a molecule.
   */
  public void setMatchingAtom(Node jmolAtom, int index) {
    matchingNode = jmolAtom;
    matchingIndex = index;
  }

  /**
   * Sets the number of explicit hydrogen atoms bonded with this atom.
   * 
   * @param count Number of hydrogen atoms.
   */
  public void setExplicitHydrogenCount(int count) {
    explicitHydrogenCount = count;
  }

  /**
   * Sets the number of implicit hydrogen atoms bonded with this atom.
   * 
   * @param count Number of hydrogen atoms.
   */
  public void setImplicitHydrogenCount(int count) {
    //if (count == Integer.MIN_VALUE)
    // set count implicitly for pattern atom in SMILES match
    implicitHydrogenCount = count;
  }

  public void setDegree(int degree) {
    this.degree = degree;
  }

  public void setNonhydrogenDegree(int degree) {
    nonhydrogenDegree = degree;
  }

  public void setValence(int valence) {
    this.valence = valence;
  }

  public void setConnectivity(int connectivity) {
    this.connectivity = connectivity;
  }

  public void setRingMembership(int rm) {
    ringMembership = rm;
  }

  public void setRingSize(int rs) {
    ringSize = rs;
    if (ringSize == 500 || ringSize == 600)
      isAromatic = true;
  }

  public void setRingConnectivity(int rc) {
    ringConnectivity = rc;
  }

  @Override
  public int getModelIndex() {
    return component;
  }

  @Override
  public int getMoleculeNumber(boolean inModel) {
    return component;
  }

  @Override
  public int getAtomSite() {
    return atomSite;
  }

  @Override
  public int getFormalCharge() {
    return charge;
  }

  @Override
  public int getIsotopeNumber() {
    return atomicMass;
  }

  @Override
  public int getAtomicAndIsotopeNumber() {
    return Elements.getAtomicAndIsotopeNumber(elementNumber, atomicMass);
  }

  @Override
  public String getAtomName() {
    return bioAtomName == null ? "" : bioAtomName;
  }

  @Override
  public String getGroup3(boolean allowNull) {
    return residueName == null ? "" : residueName;
  }

  @Override
  public String getGroup1(char c0) {
    return residueChar == null ? "" : residueChar;
  }

  /**
   * Add a bond to the atom.
   * 
   * @param bond Bond to add.
   */
  void addBond(SmilesBond bond) {
    if (bondCount >= bonds.length)
      bonds = (SmilesBond[]) AU.doubleLength(bonds);
    //if (Logger.debugging)
    //Logger.debug("adding bond " + bondCount + " to " + this + ": " + bond.atom1 + " " + bond.atom2);
    bonds[bondCount] = bond;
    bondCount++;
  }

  public void setBondArray() {
    if (bonds.length > bondCount) 
      bonds = (SmilesBond[]) AU.arrayCopyObject(bonds, bondCount);
    if (subAtoms != null && subAtoms.length > nSubAtoms)
      subAtoms = (SmilesAtom[]) AU.arrayCopyObject(subAtoms, subAtoms.length);
    for (int i = 0; i < bonds.length; i++) {
      SmilesBond b = bonds[i];
      if (isBioAtom && b.getBondType() == SmilesBond.TYPE_AROMATIC)
        b.order = SmilesBond.TYPE_BIO_CROSSLINK;
      if (b.atom1.index > b.atom2.index) {
        // it is possible, particularly for a connection to a an atom 
        // with a branch:   C1(CCCN1)
        // for the second assigned atom to not have the
        // higher index. That would prevent SmilesParser
        // from checking bonds. (atom 1 in this case, for 
        // example, would be the second atom in a bond for
        // which the first atom (N) would not yet be assigned.
        b.switchAtoms();
      }
    }
  }

  @Override
  public Edge[] getEdges() {
    return (parent != null ? parent.getEdges() : bonds);
  }

  /**
   * Returns the bond at index <code>number</code>.
   * 
   * @param number Bond number.
   * @return Bond.
   */
  public SmilesBond getBond(int number) {
    return (parent != null ? parent.getBond(number) : number >= 0
        && number < bondCount ? bonds[number] : null);
  }

  /**
   * Returns the number of bonds of this atom.
   * 
   * @return Number of bonds.
   */
  @Override
  public int getCovalentBondCount() {
    return getBondCount();
  }

  @Override
  public int getBondCount() {
    return (parent != null ? parent.getBondCount() : bondCount);
  }

  @Override
  public int getCovalentBondCountPlusMissingH() {
    return getBondCount() + (isTopoAtom ? 0 : missingHydrogenCount);
  }

  @Override
  public int getTotalHydrogenCount() {
    return getCovalentHydrogenCount() + (isTopoAtom ? 0 : missingHydrogenCount);
  }

  @Override
  public int getImplicitHydrogenCount() {
    // this is not called by the MF calculation, as "includeImplicitHydrogens is false in that method's parameters
    return implicitHydrogenCount;
  }

  public int getExplicitHydrogenCount() {
    return explicitHydrogenCount;
  }

  public int getMatchingBondedAtom(int i) {
    if (parent != null)
      return parent.getMatchingBondedAtom(i);
    if (i >= bondCount)
      return -1;
    SmilesBond b = bonds[i];
    return (b.atom1 == this ? b.atom2 : b.atom1).matchingIndex;
  }

  @Override
  public int getBondedAtomIndex(int j) {
    return (parent != null ? parent.getBondedAtomIndex(j) : bonds[j]
        .getOtherAtom(this).index);
  }

  @Override
  public int getCovalentHydrogenCount() {
    if (covalentHydrogenCount >= 0)
      return covalentHydrogenCount;
    if (parent != null)
      return (covalentHydrogenCount = parent.getCovalentHydrogenCount());
    covalentHydrogenCount = 0;
    for (int k = 0; k < bonds.length; k++)
      if (bonds[k].getOtherAtom(this).elementNumber == 1)
        covalentHydrogenCount++;
    return covalentHydrogenCount;
  }

  @Override
  public int getValence() {
    if (parent != null)
      return parent.getValence();
    int n = valence;
    if (n <= 0 && bonds != null)
      for (int i = bonds.length; --i >= 0;)
        n += bonds[i].getValence();
    valence = n;
    return n;
  }

  @Override
  public int getTotalValence() {
    return getValence() + (isTopoAtom ? 0 : missingHydrogenCount);
  }

  /**
   * if atom is null, return bond TO this atom (bond.atom2 == this)
   * otherwise, return bond connecting this atom with
   * that atom
   *  
   * @param atom
   * @return  bond
   */
  SmilesBond getBondTo(SmilesAtom atom) {
    if (parent != null)
      return parent.getBondTo(atom);
    SmilesBond bond;
    for (int k = 0; k < bonds.length; k++) {
      if ((bond = bonds[k]) == null)
        continue;
      if (atom == null ? bond.atom2 == this 
          : bond.getOtherAtom(this) == atom)
        return bond;
    }
    return null;
  }

  SmilesBond getBondNotTo(SmilesAtom atom, boolean allowH) {
    SmilesBond bond;
    for (int k = 0; k < bonds.length; k++) {
      if ((bond = bonds[k]) == null)
        continue;
      SmilesAtom atom2 = bond.getOtherAtom(this);
      if (atom != atom2 && (allowH || atom2.elementNumber != 1))
        return bond;
    }
    return null;
  }

  @Override
  public boolean isLeadAtom() {
    return isLeadAtom;
  }

  @Override
  public int getOffsetResidueAtom(String name, int offset) {
    if (isBioAtom) {
      if (offset == 0)
        return index;
      for (int k = 0; k < bonds.length; k++)
        if (bonds[k].getAtomIndex1() == index
            && bonds[k].getBondType() == SmilesBond.TYPE_BIO_SEQUENCE)
          return bonds[k].getOtherAtom(this).index;
    }
    return -1;
  }

  @Override
  public void getGroupBits(BS bs) {
    bs.set(index);
    return;
  }

  @Override
  public boolean isCrossLinked(Node node) {
    SmilesBond bond = getBondTo((SmilesAtom) node);
    return bond.isHydrogen();
  }

  @Override
  public boolean getCrossLinkVector(Lst<Integer> vLinks, boolean crosslinkCovalent, boolean crosslinkHBond) {
    boolean haveCrossLinks = false;
    for (int k = 0; k < bonds.length; k++)
      if (bonds[k].order == SmilesBond.TYPE_BIO_CROSSLINK) {
        if (vLinks == null)
          return true;
        vLinks.addLast(Integer.valueOf(this.index));
        vLinks.addLast(Integer.valueOf(bonds[k].getOtherAtom(this).index));
        vLinks.addLast(Integer.valueOf(bonds[k].getOtherAtom(this).index));
        haveCrossLinks = true;
      }
    return haveCrossLinks;
  }

  @Override
  public String getBioStructureTypeName() {
    return null;
  }

  @Override
  public char getInsertionCode() {
    return insCode;
  }
  @Override
  public int getResno() {
    return residueNumber;
  }

  @Override
  public int getChainID() {
    return 0;
  }

  @Override
  public String getChainIDStr() {
    return "";
  }
  
  /**
   *
   * called from SmilesGenerator
   * 
   * @param atomicNumber
   * @param isotopeNumber
   * @param valence set -1 to force brackets
   * @param charge
   * @param osclass OpenSMILES value
   * @param nH
   * @param isAromatic
   * @param stereo
   * @param is2D 
   * @return label
   */
  static String getAtomLabel(int atomicNumber, int isotopeNumber, int valence,
                             int charge, float osclass, int nH, boolean isAromatic,
                             String stereo, boolean is2D) {
    String sym = Elements.elementSymbolFromNumber(atomicNumber);
    if (isAromatic) {
      sym = sym.toLowerCase();
      if (atomicNumber != 6)
        valence = Integer.MAX_VALUE; // force [n]
    }
    boolean simple = (valence != Integer.MAX_VALUE && isotopeNumber <= 0 
        && charge == 0 && Float.isNaN(osclass) && (stereo == null || stereo.length() == 0)); 
    int norm = getDefaultCount(atomicNumber, false);
    if (is2D && nH == 0) {
      if (simple && atomicNumber == 6)
        return sym;    
      nH = norm - valence;
    }
    return (simple && norm == valence ? sym : 
      // rearranged 14.5.3_2016.03.06 to 
      "["
        + (isotopeNumber <= 0 ? "" : "" + isotopeNumber) 
        + sym
        + (stereo == null ? "" : stereo)
        + (nH > 1 ? "H" + nH : nH == 1 ? "H" : "")
        + (charge < 0 && charge != Integer.MIN_VALUE ? "" + charge 
            : charge > 0 ? "+" + charge : "") 
        + (Float.isNaN(osclass) ? "" : ":" + (int) osclass)
        + "]");
  }

  @Override
  public char getBioSmilesType() {
    return  bioType;
  }

  public boolean isNucleic() {
    return bioType == 'n' || bioType == 'r' || bioType == 'd';
  }

  @Override
  public boolean isPurine() {
    return residueChar != null && isNucleic() && "AG".indexOf(residueChar) >= 0;
  }

  @Override
  public boolean isPyrimidine() {
    return residueChar != null && isNucleic()
        && "CTUI".indexOf(residueChar) >= 0;
  }

  @Override
  public boolean isDeleted() {
    return false;
  }

  @Override
  public BS findAtomsLike(String substring) {
    // n/a
    return null;
  }

  @Override
  public String toString() {
    String s = (residueChar != null || residueName != null ? (residueChar == null ? residueName
        : residueChar)
        + "." + bioAtomName
        : (bioAtomName != null && atomNumber != Integer.MIN_VALUE ? null : elementNumber == -1 ? "A" : elementNumber == -2 ? "*" : Elements
            .elementSymbolFromNumber(elementNumber)));
    if (s == null)
      return bioAtomName + " #" + atomNumber;
    if (isAromatic)
      s = s.toLowerCase();
    String s2 = "";
    for (int i = 0; i < bondCount; i++)
      s2 += bonds[i].getOtherAtom(this).index + ", ";
    
    return "[" + s + '.' + index
        + (matchingIndex >= 0 ? "(" + matchingNode + ")" : "")
        //    + " ch:" + charge 
        //    + " ar:" + isAromatic 
        //    + " H:" + explicitHydrogenCount
        //    + " h:" + implicitHydrogenCount
        + "]->" + s2 + "(" + x + "," + y + "," + z + ")";
  }

  @Override
  public float getFloatProperty(String property) {
    if (property == "property_atomclass") // == is OK here.  
      return atomClass;
    return Float.NaN;
  }

  @Override
  public float getMass() {
    // TODO
    return atomicMass;
  }

  @Override
  public String getCIPChirality(boolean doCalculate) {
    return JC.getCIPChiralityName(cipChirality & ~JC.CIP_CHIRALITY_NAME_MASK);
  }

  @Override
  public void setCIPChirality(int c) {
    cipChirality = c;
  }

  @Override
  public int getCIPChiralityCode() {
    return cipChirality;
  }


  @Override
  public P3 getXYZ() {
    return this;
  }

  public SmilesStereo getStereo() {
    return stereo;
  }

  public int getPatternIndex() {
    return patternIndex;
  }

  @Override
  public boolean modelIsRawPDB() {
    return false;
  }

  /**
   * for InChI or any other system that self-defines stereochemistry.
   * 
   * @return
   */
  public boolean definesStereo() {
    return false;
  }

  public String getStereoAtAt(SimpleNode[] nodes) {
    return null;
  }

  public Boolean isStereoOpposite(int iatom) {
    return null;
  }

}
