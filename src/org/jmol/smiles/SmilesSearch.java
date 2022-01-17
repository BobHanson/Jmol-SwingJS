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

import java.util.Hashtable;

import java.util.Map;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.SB;
import javajs.util.T3;

import javajs.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.Edge;
import org.jmol.util.JmolMolecule;
import org.jmol.util.Logger;
import org.jmol.util.Node;
import org.jmol.util.SimpleNode;
import org.jmol.viewer.JC;

/**
 * A class to build and carry out a SMILES or SMARTS match
 * @author Bob Hanson hansonr@stolaf.edu
 */
public class SmilesSearch extends JmolMolecule {

  public SmilesSearch() {
    top = this;
    v = new VTemp();
  }

  /* ============================================================= */
  /*                             Setup                             */
  /* ============================================================= */

  boolean isSmarts;

  SmilesSearch top;
  
  String pattern;
  SmilesAtom[] patternAtoms = new SmilesAtom[16];
  Node[] targetAtoms;
  int targetAtomCount;
  private BS bsSelected;

  VTemp v; // temporary vector set
  
  boolean aromaticOpen; // will also be openSMARTS if isSmarts is TRUE
  boolean aromaticStrict;
  boolean aromaticPlanar;
  boolean aromaticDouble;
  boolean aromaticMMFF94;
  boolean aromaticDefined;
  boolean aromaticUnknown;
  boolean noAromatic;
  boolean ignoreAtomClass;
  boolean ignoreElement;
  boolean ignoreStereochemistry;
  boolean invertStereochemistry;
  boolean exitFirstMatch;  
  boolean groupByModel; // or byMolecule (default)
  boolean setAtropicity;

  /**
   * Set in set() to indicate to SmilesMatcher that 
   * the string already has aromatic atoms indicated and so
   * no aromaticity model should be applied.
   */
  
  boolean patternAromatic; 
  boolean haveTopo;  
  boolean isTopology;
  boolean patternBioSequence;
  SmilesSearch[] subSearches;
  boolean haveSelected;
  boolean haveBondStereochemistry;
  SmilesStereo stereo;
  boolean needRingData;
  boolean needAromatic = true; // we just have to always consider aromatic, except in the case of bioSequences.
  boolean needRingMemberships;

  int nDouble;
  int ringDataMax = Integer.MIN_VALUE;
  Lst<BS> ringSets;
  int ringCount;


  Lst<SmilesMeasure> measures = new  Lst<SmilesMeasure>();
  
  int flags;
  BS bsAromatic = new BS();
  BS bsAromatic5 = new BS();
  BS bsAromatic6 = new BS();
  String atropKeys;
  
  SmilesAtom lastChainAtom;

  boolean asVector;
  boolean getMaps;
  boolean isNormalized;
  boolean haveComponents;
  
  void setTop(SmilesSearch parent) {
    while (parent.top != parent)
      parent = parent.top;
    this.top = parent;
  }

  //  private data 
  
  private boolean isSilent;
  private boolean isRingCheck;
  private int selectedAtomCount;
  private BS[] ringData;
  private int[] ringCounts;
  private int[] ringConnections;
  private BS bsFound = new BS(); 
  private Map<String, Object> htNested;
  private int nNested;
  private SmilesBond nestedBond;

  private Lst<Object> vReturn;
  private Lst<BS> uniqueList;
  private BS bsReturn = new BS();
  
  private BS bsCheck;

  public boolean mapUnique;

  private BS bsAromaticRings;

  /**
   * indicates that we have [XxPHn] with no connected atoms
   */
  public SmilesStereo polyhedronStereo;

  SmilesAtom polyAtom;

  static final int addFlags(int flags, String strFlags) {
    if (strFlags.indexOf("OPEN") >= 0)
      flags |= JC.SMILES_TYPE_OPENSMILES;
    if (strFlags.indexOf("BIO") >= 0)
      flags |= JC.SMILES_GEN_BIO;

    if (strFlags.indexOf("HYDROGEN2") >= 0)
        flags |= JC.SMILES_GEN_EXPLICIT_H2_ONLY;
    else if (strFlags.indexOf("HYDROGEN") >= 0)
      flags |= JC.SMILES_GEN_EXPLICIT_H_ALL;

//    if (strFlags.indexOf("NONCANONICAL") >= 0) // no longer used
//      flags |= AROMATIC_JSME_NONCANONICAL; 
    

    if (strFlags.indexOf("FIRSTMATCHONLY") >= 0)
      flags |= JC.SMILES_FIRST_MATCH_ONLY;

    if (strFlags.indexOf("STRICT") >= 0) // MMFF94
      flags |= JC.SMILES_AROMATIC_STRICT;    
    if (strFlags.indexOf("PLANAR") >= 0) // MMFF94
      flags |= JC.SMILES_AROMATIC_PLANAR;    
    if (strFlags.indexOf("NOAROMATIC") >= 0 || strFlags.indexOf("NONAROMATIC") >= 0)
      flags |= JC.SMILES_NO_AROMATIC;
    if (strFlags.indexOf("AROMATICDOUBLE") >= 0) // MMFF94; deprecated
      flags |= JC.SMILES_AROMATIC_DOUBLE;
    if (strFlags.indexOf("AROMATICDEFINED") >= 0)
      flags |= JC.SMILES_AROMATIC_DEFINED;
    if (strFlags.indexOf("MMFF94") >= 0)
      flags |= JC.SMILES_AROMATIC_MMFF94;

    if (strFlags.indexOf("TOPOLOGY") >= 0)
      flags |= JC.SMILES_GEN_TOPOLOGY;
    if (strFlags.indexOf("NOATOMCLASS") >= 0)
      flags |= JC.SMILES_IGNORE_ATOM_CLASS;    
    if (strFlags.indexOf("NOSTEREO") >= 0) {
      flags |= JC.SMILES_IGNORE_STEREOCHEMISTRY;
    } else if (strFlags.indexOf("INVERTSTEREO") >= 0) {
      if ((flags & JC.SMILES_INVERT_STEREOCHEMISTRY) != 0)
        flags &= ~JC.SMILES_INVERT_STEREOCHEMISTRY;
      else
        flags |= JC.SMILES_INVERT_STEREOCHEMISTRY;
    }
    if (strFlags.indexOf("ATOMCOMMENT") >= 0)
      flags |= JC.SMILES_GEN_ATOM_COMMENT;

    if (strFlags.indexOf("GROUPBYMODEL") >= 0)
      flags |= JC.SMILES_GROUP_BY_MODEL;
    
    if ((flags & JC.SMILES_GEN_BIO) == JC.SMILES_GEN_BIO) {
      if (strFlags.indexOf("NOCOMMENT") >= 0)
        flags |= JC.SMILES_GEN_BIO_NOCOMMENTS;
      if (strFlags.indexOf("UNMATCHED") >= 0)
        flags |= JC.SMILES_GEN_BIO_ALLOW_UNMATCHED_RINGS;
      if (strFlags.indexOf("COVALENT") >= 0)
        flags |= JC.SMILES_GEN_BIO_COV_CROSSLINK;
      if (strFlags.indexOf("HBOND") >= 0)
        flags |= JC.SMILES_GEN_BIO_HH_CROSSLINK;
    }

    return flags;
  }

  void setFlags(int flags) {
    this.flags = flags;

    // starting with Jmol 12.3.24, we allow the flag AROMATICDOUBLE to allow a
    // distinction between single and double, as for example is necessary to distinguish
    // between n=cNH2 and ncNH2 (necessary for MMFF94 atom typing
    //
    // starting with Jmol 14.4.5, presence of a=a will set this automatically.
    // but still of possible use in terms of comparing two structures
    // and used by the SMILES generator

    // |= here, because this might be set explicitly in SmilesMatcher

    exitFirstMatch |= ((flags & JC.SMILES_FIRST_MATCH_ONLY) == JC.SMILES_FIRST_MATCH_ONLY);

    aromaticOpen = ((flags & JC.SMILES_TYPE_OPENSMILES) == JC.SMILES_TYPE_OPENSMILES);

    aromaticDouble = ((flags & JC.SMILES_AROMATIC_DOUBLE) == JC.SMILES_AROMATIC_DOUBLE); // {1.1}.find("SMILES/aromaticDouble",{2.1})

    aromaticStrict = ((flags & JC.SMILES_AROMATIC_STRICT) == JC.SMILES_AROMATIC_STRICT);

    aromaticPlanar = ((flags & JC.SMILES_AROMATIC_PLANAR) == JC.SMILES_AROMATIC_PLANAR);

    aromaticMMFF94 = ((flags & JC.SMILES_AROMATIC_MMFF94) == JC.SMILES_AROMATIC_MMFF94);
    
    aromaticDefined = ((flags & JC.SMILES_AROMATIC_DEFINED) == JC.SMILES_AROMATIC_DEFINED); 

    noAromatic |= ((flags & JC.SMILES_NO_AROMATIC) == JC.SMILES_NO_AROMATIC);

    aromaticUnknown = !noAromatic 
        && !aromaticOpen 
        && !aromaticDouble 
        && !aromaticStrict 
        && !aromaticPlanar 
        && !aromaticMMFF94 
        && !aromaticDefined;    

    groupByModel = ((flags & JC.SMILES_GROUP_BY_MODEL) == JC.SMILES_GROUP_BY_MODEL);

    ignoreAtomClass = ((flags & JC.SMILES_IGNORE_ATOM_CLASS) == JC.SMILES_IGNORE_ATOM_CLASS);

    ignoreStereochemistry = ((flags & JC.SMILES_IGNORE_STEREOCHEMISTRY) == JC.SMILES_IGNORE_STEREOCHEMISTRY);

    invertStereochemistry = !ignoreStereochemistry && ((flags & JC.SMILES_INVERT_STEREOCHEMISTRY) == JC.SMILES_INVERT_STEREOCHEMISTRY);
    
    ignoreElement = ((flags & JC.SMILES_GEN_TOPOLOGY) == JC.SMILES_GEN_TOPOLOGY);
  }

  /*
   * Called by SmilesParser when it is done
   * 
   */
  void set() throws InvalidSmilesException {
    if (patternAtoms.length > ac)
      patternAtoms = (SmilesAtom[]) AU.arrayCopyObject(patternAtoms, ac);
    nodes = patternAtoms;
    isTopology = true;
    patternAromatic = false;
    patternBioSequence = true;
    for (int i = ac; --i >= 0;) {
      SmilesAtom atom = patternAtoms[i];
      if (isTopology && atom.isDefined())
        isTopology = false;
      if (!atom.isBioResidue)
        patternBioSequence = false;
      if (atom.isAromatic)
        patternAromatic = true;
      atom.setBondArray();
      if (!isSmarts && atom.bioType == '\0' && !atom.setHydrogenCount())
        throw new InvalidSmilesException("unbracketed atoms must be one of: "
            + SmilesAtom.UNBRACKETED_SET);
    }
    if (haveComponents) {
      for (int i = ac; --i >= 0;) {
        SmilesAtom a = patternAtoms[i];
        SmilesBond[] bonds = a.bonds;
        int ia = a.component;
        for (int j = a.bondCount; --j >= 0;) {
            SmilesBond b = bonds[j];
          int ib;
          if (b.isConnection && b.atom2 == a && (ib = b.atom1.component) != ia) {
            for (int k = ac; --k >= 0;)
              if (patternAtoms[k].component == ia)
                patternAtoms[k].component = ib;
          }
        }
      }
    }
  }

  void setSelected(BS bs) {
    selectedAtomCount = (bs == null ? targetAtomCount: bs.cardinality());
    if (bs == null) {
      // null because this is an atom set
      // constructed by SmilesParser.getMolecule
      //  "CCCCC".find("SMARTS","C")
      bs = BS.newN(targetAtomCount);
      bs.setBits(0, targetAtomCount);
    }
    bsSelected = bs;
  }

  SmilesAtom addAtom() {
    return appendAtom(new SmilesAtom());
  }

  SmilesAtom appendAtom(SmilesAtom sAtom) {
    if (ac >= patternAtoms.length)
      patternAtoms = (SmilesAtom[]) AU.doubleLength(patternAtoms);
    return patternAtoms[ac] = sAtom.setIndex(ac++);
  }

  int addNested(String pattern) {
    if (htNested == null)
      htNested = new Hashtable<String, Object>();
    setNested(++nNested, pattern);
    return nNested;
  }
  
  void clear() {
    bsReturn.clearAll();
    nNested = 0;
    htNested = null;
    nestedBond = null;//new SmilesBond(0, false);
    clearBsFound(-1);
  }
  
  private void clearBsFound(int iAtom) {
    
    if (iAtom < 0) {
      if (bsCheck == null) {bsFound.clearAll();}
      }
    else
      bsFound.clear(iAtom);    
  }

  void setNested(int iNested, Object o) {
    top.htNested.put("_" + iNested, o);
  }

  Object getNested(int iNested) {
    return top.htNested.get("_" + iNested);
  }
  
  int getMissingHydrogenCount() {
    int n = 0;
    int nH;
    for (int i = 0; i < ac; i++)
      if ((nH = patternAtoms[i].explicitHydrogenCount) >= 0)
          n += nH;
    return n;
  }

  /**
   * Sets up all aromatic and ring data.
   * Called from SmilesGenerator.getSmilesComponent and SmilesMatcher.matchPriv.
   * 
   * @param bsA
   * @param vRings 
   * @param doProcessAromatic 
   * @throws InvalidSmilesException
   */
  void setRingData(BS bsA, Lst<BS>[] vRings, boolean doProcessAromatic) throws InvalidSmilesException {
    if (isTopology || patternBioSequence)
      needAromatic = false;
    needAromatic &= (bsA == null) & !noAromatic;
    if (needAromatic)
      needRingData = true;
    // when using "xxx".find("search","....")
    // or $(...), the aromatic set has already been determined
    if (!needAromatic) {
      bsAromatic.clearAll();
      if (bsA != null)
        bsAromatic.or(bsA);
      if (!needRingMemberships && !needRingData)
        return;
    }
    getRingData(vRings, needRingData, doProcessAromatic);
  }

  @SuppressWarnings("unchecked")
  void getRingData(Lst<BS>[] vRings, boolean needRingData,
                   boolean doTestAromatic) throws InvalidSmilesException {
    // isUnknown should be handled as STRICT except for biomodels.
    boolean isStrict = (needAromatic && (aromaticStrict || !aromaticOpen && !aromaticPlanar));
    if (isStrict && aromaticUnknown) {
      if (targetAtomCount > 0 && targetAtoms[bsSelected.nextSetBit(0)].modelIsRawPDB())
        isStrict = false;
    }
    boolean isOpenNotStrict = (needAromatic && aromaticOpen && !aromaticStrict);
    boolean checkExplicit = (needAromatic && !isStrict);
    boolean doFinalize = (needAromatic && doTestAromatic && (isStrict || isOpenNotStrict));
    boolean setAromatic = (needAromatic && !aromaticDefined);
    int aromaticMax = 7;
    Lst<BS> lstAromatic = (vRings == null ? new Lst<BS>()
        : (vRings[3] = new Lst<BS>()));
    Lst<SmilesRing> lstSP2 = (doFinalize ? new Lst<SmilesRing>() : null);
    int strictness = (!isStrict ? 0 : aromaticMMFF94 ? 2 : 1);
    if (needAromatic && aromaticDefined) {
      // predefined aromatic bonds
      SmilesAromatic.checkAromaticDefined(targetAtoms, bsSelected, bsAromatic);
      strictness = 0;
    }
    
    if (ringDataMax < 0)
      ringDataMax = 8;
    if (strictness > 0 && ringDataMax < 6)
      ringDataMax = 6;
    if (needRingData) {
      ringCounts = new int[targetAtomCount];
      ringConnections = new int[targetAtomCount];
      ringData = new BS[ringDataMax + 1];
    }
    ringSets = new Lst<BS>();
    if (selectedAtomCount < 3)
      return;  
    String s = "****";
    int max = ringDataMax;
    while (s.length() < max)
      s += s;
    int[] eCounts = (doFinalize && setAromatic ? new int[targetAtomCount] : null);
    boolean justCheckBonding = (setAromatic && targetAtoms[0] instanceof SmilesAtom);
    for (int i = 3; i <= max; i++) {
      if (i > targetAtomCount)
        break;
      String smarts = "*1" + s.substring(0, i - 2) + "*1";
      SmilesSearch search = SmilesParser.newSearch(smarts, true, true);
      Lst<Object> vR = (Lst<Object>) subsearch(search, SUBMODE_RINGCHECK);
      if (vRings != null && i <= 5) {
        Lst<BS> v = new Lst<BS>();
        for (int j = vR.size(); --j >= 0;)
          v.addLast((BS) vR.get(j));
        vRings[i - 3] = v;
      }
      if (vR.size() == 0)
        continue;
      if (setAromatic && i >= 4 && i <= aromaticMax) {
        SmilesAromatic.setAromatic(i, targetAtoms, bsSelected, vR, bsAromatic,
            strictness, isOpenNotStrict, justCheckBonding, checkExplicit, v, lstAromatic, lstSP2,
            eCounts, doTestAromatic);
      }
      if (needRingData) {
        ringData[i] = new BS();
        for (int k = vR.size(); --k >= 0;) {
          BS r = (BS) vR.get(k);
          ringData[i].or(r);
          for (int j = r.nextSetBit(0); j >= 0; j = r.nextSetBit(j + 1))
            ringCounts[j]++;
        }
      }
    }
    if (needAromatic) {
      if (doFinalize)
        SmilesAromatic.finalizeAromatic(targetAtoms, bsAromatic, lstAromatic,
            lstSP2, eCounts, isOpenNotStrict, isStrict);
      // clean out all nonaromatic atoms from the ring list
      // and recreate 5- and 6-membered ring bitsets
      bsAromatic5.clearAll();
      bsAromatic6.clearAll();
      for (int i = lstAromatic.size(); --i >= 0;) {
        BS bs = lstAromatic.get(i);
        bs.and(bsAromatic);
        switch (bs.cardinality()) {
        case 5:
          bsAromatic5.or(bs);
          break;
        case 6:
          bsAromatic6.or(bs);
          break;
        }
      }
    }
    if (needRingData) {
      for (int i = bsSelected.nextSetBit(0); i >= 0; i = bsSelected
          .nextSetBit(i + 1)) {
        Node atom = targetAtoms[i];
        Edge[] bonds = atom.getEdges();
        if (bonds != null)
          for (int k = bonds.length; --k >= 0;)
            if (ringCounts[atom.getBondedAtomIndex(k)] > 0)
              ringConnections[i]++;
      }
    }
  }

  private final static int SUBMODE_NESTED = 1;
  private final static int SUBMODE_RINGCHECK = 2;
  private final static int SUBMODE_OR = 3;

  private static final int ATROPIC_SWITCH = 1;
  
  /**
   * @param search
   * @param submode
   * @return Object
   * @throws InvalidSmilesException
   */
  Object subsearch(SmilesSearch search, int submode) throws InvalidSmilesException {
    search.ringSets = ringSets;
    search.mapUnique = mapUnique;
    search.targetAtoms = targetAtoms;
    search.targetAtomCount = targetAtomCount;
    search.bsSelected = bsSelected;
    search.selectedAtomCount = selectedAtomCount;
    search.htNested = htNested;
    search.haveTopo = haveTopo;
    search.bsCheck = bsCheck;
    search.isSmarts = true;
    search.bsAromatic = bsAromatic;
    search.bsAromatic5 = bsAromatic5;
    search.bsAromatic6 = bsAromatic6;
    search.ringData = ringData;
    search.ringCounts = ringCounts;
    search.ringConnections = ringConnections;
    switch (submode) {
    case SUBMODE_NESTED:
      // [$(....)]
      search.exitFirstMatch = false;
      break;
    case SUBMODE_RINGCHECK:
      // *1****1
      search.isRingCheck = true;
      search.isSilent = true;
      search.asVector = true;
      break;
    case SUBMODE_OR:
      // processing SMARTS || SMARTS 
      search.ignoreAtomClass = ignoreAtomClass;
      search.aromaticDouble = aromaticDouble;
      search.haveSelected = haveSelected;
      search.exitFirstMatch = exitFirstMatch;
      search.getMaps = getMaps;
      search.asVector = asVector;
      search.vReturn = vReturn;
      search.bsReturn = bsReturn;
      search.haveBondStereochemistry = haveBondStereochemistry;
      break;
    }
    return search.search2(submode == SUBMODE_NESTED);
  }
  
  /* ============================================================= */
  /*                             Search                            */
  /* ============================================================= */

  /** 
   * the start of the search. ret will be either a Vector or a BitSet
   * @return BitSet or Vector
   * @throws InvalidSmilesException 
   * 
   */
  Object search() throws InvalidSmilesException {
    return search2(false); 
  }
  
  private Object search2(boolean firstAtomOnly) throws InvalidSmilesException {

    /*
     * The essence of the search process is as follows:
     * 
     * 1) From the pattern, create an ordered set of atoms connected by bonds.
     *    
     * 2) Try all model set atoms for position 0.
     * 
     * 3) For each atom that matches position N
     *    we move to position N+1 and run through all 
     *    of the pattern bonds TO this atom (atom in position 2).
     *    Those bonds will be to atoms that have already
     *    been assigned. There may be more than one of these
     *    if the atom is associated with a ring junction.
     *    
     *    We check that previously assigned model atom,
     *    looking at all of its bonded atoms to check for 
     *    a match for our N+1 atom. This works because if 
     *    this atom is going to work in this position, then 
     *    it must be bound to the atom assigned to position N
     *    
     *    There is no need to check more than one route to this
     *    atom in this position - if it is found to be good once,
     *    that is all we need, and if it is found to be bad once,
     *    that is all we need as well.
     *    
     */

    setFlags(flags);
    // flags are passed on from SmilesParser /xxxxx/

    if (!isRingCheck && Logger.debuggingHigh && !isSilent)
      Logger.debug("SmilesSearch processing " + pattern);

    if (vReturn == null && (asVector || getMaps))
      vReturn = new  Lst<Object>();
    if (subSearches != null) {
      // SMARTS || SMARTS
      for (int i = 0; i < subSearches.length; i++) {
        if (subSearches[i] == null)
          continue;
        subsearch(subSearches[i], SUBMODE_OR);
        if (exitFirstMatch) {
          if (vReturn == null ? bsReturn.nextSetBit(0) >= 0 : vReturn.size() > 0)
            break;
        }
      }
    } else if (ac > 0 && ac <= selectedAtomCount) {
      if (nestedBond == null) {
        // specifically for non-bioSmarts or not $(....) 
        clearBsFound(-1);
      } else {
        // clear out the return when there's a nested bio atom when $(...) is in a biomolecule?
        bsReturn.clearAll();
      }
      nextPatternAtom(-1, -1, firstAtomOnly, -1);
    }
    return (asVector || getMaps ? (Object) vReturn : bsReturn);
  }

  private boolean nextPatternAtom(int atomNum, int iAtom,
                                  boolean firstAtomOnly, int c)
      throws InvalidSmilesException {

    Node jmolAtom;
    Edge[] jmolBonds;

    if (++atomNum < ac) {

      SmilesAtom newPatternAtom = patternAtoms[atomNum];

      // For all the pattern bonds for this atom...
      // find the bond to atoms already assigned.
      // If it is not there, then it means this is a
      // new component.

      // the nestedBond may be set to previous search
      SmilesBond newPatternBond = (iAtom >= 0 ? newPatternAtom.getBondTo(null)
          : atomNum == 0 ? nestedBond : null);
      if (newPatternBond == null) {

        // Option 1: we are processing "."

        //
        // run through all unmatched and unbonded-to-match
        // selected Jmol atoms to see if there is a match. 

        BS bs = BSUtil.copy(bsFound);
        BS bs0 = BSUtil.copy(bsFound);
        if (newPatternAtom.notBondedIndex >= 0) {
          SmilesAtom pa = patternAtoms[newPatternAtom.notBondedIndex];
          Node a = pa.getMatchingAtom();
          if (pa.isBioAtom) {
            // clear out adjacent residues
            int ii = a.getOffsetResidueAtom("\0", 1);
            if (ii >= 0)
              bs.set(ii);
            ii = a.getOffsetResidueAtom("\0", -1);
            if (ii >= 0)
              bs.set(ii);
          } else if (pa == polyAtom){
            bs.set(pa.getMatchingAtomIndex());
          } else {
            // clear out all atoms connected to the last atom only
            jmolBonds = a.getEdges();
            for (int k = 0; k < jmolBonds.length; k++)
              bs.set(jmolBonds[k].getOtherNode(a).getIndex());
          }
        }
        boolean skipGroup = ((newPatternAtom.isBioAtomWild));
        // TODO fix the *.*.*.*.* problem
        int j1 = bsSelected.nextSetBit(0);
        j1 = (skipGroup && j1 >= 0 ? targetAtoms[j1].getOffsetResidueAtom("\0",
            j1) : j1);
        int oldJmolComponent;
        int oldPatternComponent = (atomNum > 0 ? patternAtoms[atomNum - 1]
            : newPatternAtom).component;
        int thisPatternComponent = newPatternAtom.component;
        boolean checkComponents = haveComponents
            && thisPatternComponent != Integer.MIN_VALUE;
        for (int j = j1; j >= 0; j = bsSelected.nextSetBit(j + 1)) {
          if (!bs.get(j) && !bsFound.get(j)) {

            jmolAtom = targetAtoms[j];
            
            if (checkComponents && !isRingCheck) {
              c = (groupByModel ? jmolAtom.getModelIndex() : jmolAtom
                  .getMoleculeNumber(false));
              oldJmolComponent = (atomNum > 0 ? patternAtoms[atomNum - 1].matchingComponent
                  : c);
              if ((oldPatternComponent == thisPatternComponent) != (oldJmolComponent == c))
                continue;
            }
            if (!nextTargetAtom(newPatternAtom, jmolAtom, atomNum, j,
                firstAtomOnly, c))
              return false;
          }
          if (skipGroup) {
            j1 = targetAtoms[j].getOffsetResidueAtom(
                newPatternAtom.bioAtomName, 1);
            if (j1 >= 0)
              j = j1 - 1;
          }
        }
        bsFound = bs0;
        return true;
      }

      // The new atom is connected to the old one in the pattern.
      // It doesn't so much matter WHICH connection we found -- 
      // there may be several -- but whatever we have, we must
      // have a connection in the real molecule between these two
      // particular atoms. So we just follow that connection. 

      jmolAtom = newPatternBond.atom1.getMatchingAtom();

      // Option 2: The connecting bond is a bio sequence or
      // from ~GGC(T)C:ATTC...
      // For sequences, we go to the next GROUP, either via
      // the standard sequence or via basepair/cysteine pairing. 

      switch (newPatternBond.order) {
      case SmilesBond.TYPE_BIO_SEQUENCE:
        int nextGroupAtom = jmolAtom.getOffsetResidueAtom(
            newPatternAtom.bioAtomName, 1);
        if (nextGroupAtom >= 0) {
          BS bs = BSUtil.copy(bsFound);
          jmolAtom.getGroupBits(bsFound);

          // working here
          if (doCheckAtom(nextGroupAtom)
              && !nextTargetAtom(newPatternAtom, targetAtoms[nextGroupAtom],
                  atomNum, nextGroupAtom, firstAtomOnly, c))
            return false;
          bsFound = bs;
        }
        return true;
      case SmilesBond.TYPE_BIO_CROSSLINK:
        Lst<Integer> vLinks = new Lst<Integer>();
        jmolAtom.getCrossLinkVector(vLinks, true, true);
        BS bs = BSUtil.copy(bsFound);
        jmolAtom.getGroupBits(bsFound);
        // here we only use the third entry -- lead atoms
        for (int j = 2; j < vLinks.size(); j += 3) {
          int ia = vLinks.get(j).intValue();
          if (doCheckAtom(ia)
              && !nextTargetAtom(newPatternAtom, targetAtoms[ia], atomNum, ia,
                  firstAtomOnly, c))
            return false;
        }
        bsFound = bs;
        return true;
      }

      // Option 3: Standard practice

      // We looked at the next pattern atom position and 
      // found at least one bond to it from a previous 
      // pattern atom. The only valid possibilities for this
      // pattern atom position, then, is a Jmol atom that is
      // bonded to that previous connection. So we only have
      // to check a handful of atoms. We do this so
      // that we don't have to check EVERY atom in the model.

      // Run through the bonds of that assigned atom
      // to see if any match this new connection.

      jmolBonds = jmolAtom.getEdges();
      if (jmolBonds != null)
        for (int j = 0; j < jmolBonds.length; j++) {
          int ia = jmolAtom.getBondedAtomIndex(j);
          if (doCheckAtom(ia)
              && !nextTargetAtom(newPatternAtom, targetAtoms[ia], atomNum, ia,
                  firstAtomOnly, c))
            return false;
        }

      // Done checking this atom from any one of the places
      // higher in this stack. Clear the atom and keep going...

      clearBsFound(iAtom);
      return true;
    }

    // the pattern is complete

    // check stereochemistry

    if (!ignoreStereochemistry && !isRingCheck) {
      if (Logger.debuggingHigh) {
        for (int i = 0; i < atomNum; i++)
          Logger.debug("pattern atoms " + patternAtoms[i] + " "
              + patternAtoms[i].matchingComponent);
        Logger.debug("--ss-- " + bsFound.cardinality());
      }
      
      if(!checkStereochemistry())
        return true;
    }

    // set up the return BitSet and Vector, if requested

    // bioSequences only return the "lead" atom 
    // If the search is SMILES, we add the missing hydrogens

    BS bs = new BS();
    int nMatch = 0;
    for (int j = 0; j < ac; j++) {
      int i = patternAtoms[j].getMatchingAtomIndex();
      if (!firstAtomOnly && top.haveSelected && !patternAtoms[j].selected)
        continue;
      nMatch++;
      bs.set(i);
      if (patternAtoms[j].isBioAtomWild)
        targetAtoms[i].getGroupBits(bs);
      if (firstAtomOnly)
        break;
      if (!isSmarts)
        if (!setAtropicity && patternAtoms[j].explicitHydrogenCount > 0) {
          Node atom = targetAtoms[i];
          for (int k = 0, n = atom.getEdges().length; k < n; k++) {
            int ia = atom.getBondedAtomIndex(k);
            if (targetAtoms[ia].getElementNumber() == 1)
              bs.set(ia);
          }
        }
    }
    if (!isSmarts && bs.cardinality() != selectedAtomCount)
      return true;
    if (bsCheck != null) {
      if (firstAtomOnly) {
        bsCheck.clearAll();
        for (int j = 0; j < ac; j++) {
          bsCheck.set(patternAtoms[j].getMatchingAtomIndex());
        }
        if (bsCheck.cardinality() != ac)
          return true;
      } else {
        if (bs.cardinality() != ac)
          return true;
      }
    }
    bsReturn.or(bs);

    if (getMaps) {
      if (mapUnique) {
        if (uniqueList == null)
          uniqueList = new Lst<BS>();
        for (int j = uniqueList.size(); --j >= 0;)
          if (uniqueList.get(j).equals(bs))
            return true;
        uniqueList.addLast(bs);
      }

      // every map is important always -- why??
      int[] map = new int[nMatch];
      for (int j = 0, nn = 0; j < ac; j++) {
        if (!firstAtomOnly && top.haveSelected && !patternAtoms[j].selected)
          continue;
        map[nn++] = patternAtoms[j].getMatchingAtomIndex();
      }
      vReturn.addLast(map);
      return !exitFirstMatch;
    }

    if (asVector) {
      boolean isOK = true;
      for (int j = vReturn.size(); --j >= 0 && isOK;)
        isOK = !(((BS) vReturn.get(j)).equals(bs));
      if (!isOK)
        return true;
      vReturn.addLast(bs);
    }

    if (isRingCheck) {
      BS bsRing = new BS();
      for (int k = atomNum * 3 + 2; --k > atomNum;)
        bsRing.set(patternAtoms[(k <= atomNum * 2 ? atomNum * 2 - k + 1 : k - 1)
                % atomNum].getMatchingAtomIndex());
      ringSets.addLast(bsRing);
      return true;
    }

    // requested return is a BitSet or vector of BitSets

    // TRUE means "continue searching"

    if (exitFirstMatch)
      return false;

    // only continue if we have not found all the atoms already

    return (bs.cardinality() != selectedAtomCount);

  }

  private boolean doCheckAtom(int j) {
    return bsSelected.get(j) && !bsFound.get(j);
  }

  /**
   * Check for a specific match of a model set atom with a pattern position
   * 
   * @param patternAtom
   *        Atom of the pattern that is currently tested.
   * @param jmolAtom
   * @param atomNum
   *        Current atom of the pattern.
   * @param iAtom
   *        Atom number of the Jmol atom that is currently tested to match
   *        <code>patternAtom</code>.
   * @param firstAtomOnly
   * @param c
   * @return true to continue or false if oneOnly
   * @throws InvalidSmilesException
   */
  private final boolean nextTargetAtom(SmilesAtom patternAtom, Node jmolAtom,
                                       int atomNum, int iAtom,
                                       boolean firstAtomOnly, int c)
      throws InvalidSmilesException {

//      if (!this.isRingCheck) {
//          System.out.println("testing " + patternAtom + " " + jmolAtom);
//        }


    Edge[] jmolBonds;
    // check for requested selection or not-selection

    if (!isRingCheck && !isTopology) {
      // check atoms 

      if (patternAtom.subAtoms == null) {
        if (!checkPrimitiveAtom(patternAtom, iAtom))
          return true;
      } else if (patternAtom.isAND) {
        for (int i = 0; i < patternAtom.nSubAtoms; i++)
          if (!checkPrimitiveAtom(patternAtom.subAtoms[i], iAtom))
            return true;
      } else {
        for (int i = 0; i < patternAtom.nSubAtoms; i++)
          if (!nextTargetAtom(patternAtom.subAtoms[i], jmolAtom, atomNum,
              iAtom, firstAtomOnly, c))
            return false;
        return true;
      }
    }

    // Check bonds

    jmolBonds = jmolAtom.getEdges();
    for (int i = patternAtom.getBondCount(); --i >= 0;) {
      SmilesBond patternBond = patternAtom.getBond(i);
      // Check only if the current atom is the second atom of the bond
      if (patternBond.getAtomIndex2() != patternAtom.index)
        continue;

      // note that there might be more than one of these.
      // in EACH case we need to ensure that the actual
      // bonds to the previously assigned atoms matches

      SmilesAtom atom1 = patternBond.atom1;
      int matchingAtom = atom1.getMatchingAtomIndex();

      // BIOSMILES/BIOSMARTS check is by group

      switch (patternBond.order) {
      case SmilesBond.TYPE_BIO_SEQUENCE:
      case SmilesBond.TYPE_BIO_CROSSLINK:
        if (!checkMatchBond(patternAtom, atom1, patternBond, iAtom,
            matchingAtom, null))
          return true;
        break;
      default:

        // regular SMILES/SMARTS check 
        // is to find the bond and test it against the pattern

        int k = 0;
        Edge jmolBond = null;
        for (; k < jmolBonds.length; k++)
          if ((jmolBond = jmolBonds[k]).isCovalent()
              && (jmolBond.getAtomIndex1() == matchingAtom || jmolBond
                  .getAtomIndex2() == matchingAtom))
            break;
        if (k == jmolBonds.length)
          return true; // probably wasn't a covalent bond or was an attached implicit H

        if (!checkMatchBond(patternAtom, atom1, patternBond, iAtom,
            matchingAtom, jmolBond))
          return true;
      }
    }

    // Note that we explicitly do a reference using
    // index because this could be a SEARCH [x,x] "sub" atom.

    patternAtom = patternAtoms[patternAtom.index];
    patternAtom.setMatchingAtom(targetAtoms[iAtom], iAtom);
    patternAtom.matchingComponent = c;

    // The atom has passed both the atom and the bond test.
    // Add this atom to the growing list.

    bsFound.set(iAtom);

    if (!nextPatternAtom(atomNum, iAtom, firstAtomOnly, c))
      return false;
    if (iAtom >= 0)
      clearBsFound(iAtom);
    return true;
  }

  /**
   * 
   * @param patternAtom
   * @param iTarget
   * @return true if a match
   * @throws InvalidSmilesException
   */
  private boolean checkPrimitiveAtom(SmilesAtom patternAtom, int iTarget)
      throws InvalidSmilesException {
    
    if (patternAtom.nSubAtoms > 0) {
      // must be ORing here, because you cannot have x & (x & x)
      // since we have no parenthesis and we have already taken care of x ; x & x
      for (int i = 0; i < patternAtom.nSubAtoms; i++)
        if (checkPrimitiveAtom(patternAtom.subAtoms[i], iTarget))
          return true;
      return false;
    }

    Node targetAtom = targetAtoms[iTarget];
    boolean foundAtom = patternAtom.not;

    while (true) {

      // _<n> apply "recursive" SEARCH -- for example, C[$(aaaO);$(aaC)] or [C&$(C[$(aaaO);$(aaC)])]"
      if (patternAtom.iNested > 0) {
        Object o = getNested(patternAtom.iNested);
        if (o instanceof SmilesSearch) {
          SmilesSearch search = (SmilesSearch) o;
          if (patternAtom.isBioAtom)
            search.nestedBond = patternAtom.getBondTo(null);
          o = subsearch(search, SUBMODE_NESTED);
          if (o == null)
            o = new BS();
          if (!patternAtom.isBioAtom)
            setNested(patternAtom.iNested, o);
        }
        foundAtom = (patternAtom.not != (((BS) o).get(iTarget)));
        break;
      }

      // all types
      int na = targetAtom.getElementNumber();
      // #<n> or Symbol Check atomic number
      // look out for atomElemNo == -2 --> "*" in target SMILES
      int n = patternAtom.elementNumber;
      if (na >= 0 && n >= 0 && n != na && !ignoreElement)
        break;

      if (patternAtom.isBioResidue) {
        Node a = targetAtom;
        // <*.name>
        if (patternAtom.bioAtomName != null
            && (patternAtom.isLeadAtom() ? !a.isLeadAtom()
                : !patternAtom.bioAtomName
                    .equals(a.getAtomName().toUpperCase())))
          break;
        // <res.*>
        if (patternAtom.residueName != null
            && !patternAtom.residueName
                .equals(a.getGroup3(false).toUpperCase()))
          break;
        // <res#n
        if (patternAtom.residueNumber != Integer.MIN_VALUE
            && patternAtom.residueNumber != a.getResno())
          break;
        if (patternAtom.insCode != '\0'
            && patternAtom.insCode != a.getInsertionCode())
          break;
        if (patternAtom.residueChar != null || patternAtom.elementNumber == -2) {
          char atype = a.getBioSmilesType();
          char ptype = patternAtom.getBioSmilesType();
          boolean ok = true;
          boolean isNucleic = false;
          switch (ptype) {
          case '\0':
          case '*':
            ok = true;
            break;
          case 'n':
            ok = (atype == 'r' || atype == 'c');
            isNucleic = true;
            break;
          case 'r':
          case 'c':
            isNucleic = true;
            //$FALL-THROUGH$
          default:
            ok = (atype == ptype);
            break;
          }
          if (!ok)
            break;
          String s = a.getGroup1('\0').toUpperCase();
          char resChar = (patternAtom.residueChar == null ? '*'
              : patternAtom.residueChar.charAt(0));
          boolean isOK = (resChar == s.charAt(0));
          switch (resChar) {
          case '*':
            isOK = true;
            break;
          case 'N':
            isOK = isNucleic ? (atype == 'r' || atype == 'c') : isOK;
            break;
          case 'R': // arginine purine
            isOK = isNucleic ? a.isPurine() : isOK;
            break;
          case 'Y': // tyrosine or pyrimidine
            isOK = isNucleic ? a.isPyrimidine() : isOK;
            break;
          }
          if (!isOK)
            break;
        }
        if (patternAtom.isBioAtom) {
          // BIOSMARTS
          // cross linking, residueChar, 
          if (patternAtom.notCrossLinked
              && a.getCrossLinkVector(null, true, true))
            break;
        }
        
      } else {

        // not a bioResidue

        // [<mass>symbol<stereo><hcount><charge><:class>]

        // application-specific 
        // #-<n> application-specific atom number
        if (patternAtom.atomNumber != Integer.MIN_VALUE
            && patternAtom.atomNumber != targetAtom.getAtomNumber())
          break;
        // =<n>  Jmol index
        if (patternAtom.jmolIndex >= 0
            && targetAtom.getIndex() != patternAtom.jmolIndex)
          break;
        // <"xxx">
        if (patternAtom.atomType != null
            && !patternAtom.atomType.equals(targetAtom.getAtomType()))
          break;

        // could  be *
        // <n> Check isotope
        // smiles indicates [13C] or [12C]
        // must match perfectly -- [12C] matches only explicit C-12, not
        // "unlabeled" C
        if ((n = patternAtom.getAtomicMass()) != Integer.MIN_VALUE
            && (n >= 0 && n != (na = targetAtom.getIsotopeNumber()) || n < 0
                && na != 0 && -n != na))
          break;

        // Check aromatic
        // aromaticAmbiguous could be [#6] or [D3]
        if (!noAromatic && !patternAtom.aromaticAmbiguous
            && patternAtom.isAromatic != bsAromatic.get(iTarget))
          break;

        // <+/-> Check charge
        if ((n = patternAtom.getCharge()) != Integer.MIN_VALUE
            && n != targetAtom.getFormalCharge())
          break;

        // H<n> TOTAL H count
        //problem here is that you can have [CH][H]
        n = patternAtom.getCovalentHydrogenCount()
            + patternAtom.explicitHydrogenCount;
        if (n >= 0 && n != targetAtom.getTotalHydrogenCount())
          break;

        // h<n> implicit H count -- will be 0 for standard Jmol model; 
        // may be > 0 for SMILES string or PDB model
        // may be -1, from [h] alone to indicate "at least 1"
        if ((n = patternAtom.implicitHydrogenCount) != Integer.MIN_VALUE) {
          na = targetAtom.getImplicitHydrogenCount();
          if (n == -1 ? na == 0 : n != na)
            break;
        }

        // D<n> explicit degree -- does not count missing hydrogens
        // so is NOT appropriate for PDB file MMFF94 calc
        if (patternAtom.degree > 0
            && patternAtom.degree != targetAtom.getCovalentBondCount()
                //- targetAtom.getImplicitHydrogenCount()
                )
          break;

        // d<n> degree
        if (patternAtom.nonhydrogenDegree > 0
            && patternAtom.nonhydrogenDegree != targetAtom.getCovalentBondCount()
                - targetAtom.getCovalentHydrogenCount())
          break;

        // v<n> valence
        if (isSmarts && patternAtom.valence > 0
            && patternAtom.valence != targetAtom.getTotalValence())
          break;

        // X<n> connectivity  -- includes all missing H atoms
        if (patternAtom.connectivity > 0
            && patternAtom.connectivity != targetAtom
                .getCovalentBondCountPlusMissingH())
          break;

        // #-<n> application-specific atom number
        if (patternAtom.atomNumber != Integer.MIN_VALUE
            && patternAtom.atomNumber != targetAtom.getAtomNumber())
          break;
        // =<n>  Jmol index
        if (patternAtom.jmolIndex >= 0
            && targetAtom.getIndex() != patternAtom.jmolIndex)
          break;
        // <"xxx">
        if (patternAtom.atomType != null
            && !patternAtom.atomType.equals(targetAtom.getAtomType()))
          break;

        if (!ignoreAtomClass || isSmarts) {
          // :<n> atom class  -- will be Float.NaN, and Float.NaN is not equal to any number
          if (!Float.isNaN(patternAtom.atomClass)
              && patternAtom.atomClass != targetAtom
                  .getFloatProperty("property_atomclass"))
            break;
        }
        
        if (ringData != null) {
          // r<n> ring of a given size or [R]
          if (patternAtom.ringSize >= -1) {
            if (patternAtom.ringSize <= 0) {
              if ((ringCounts[iTarget] == 0) != (patternAtom.ringSize == 0))
                break;
            } else {
              BS rd = ringData[patternAtom.ringSize == 500 ? 5
                  : patternAtom.ringSize == 600 ? 6 : patternAtom.ringSize];
              if (rd == null || !rd.get(iTarget))
                break;
              if (!noAromatic)
                if (patternAtom.ringSize == 500) {
                  if (!bsAromatic5.get(iTarget))
                    break;
                } else if (patternAtom.ringSize == 600) {
                  if (!bsAromatic6.get(iTarget))
                    break;
                }
            }
          }
          // R<n> a certain number of rings
          if (patternAtom.ringMembership >= -1) {
            //  R --> -1 implies "!R0"
            if (patternAtom.ringMembership == -1 ? ringCounts[iTarget] == 0
                : ringCounts[iTarget] != patternAtom.ringMembership)
              break;
          }
          // x<n>
          if (patternAtom.ringConnectivity >= 0) {
            // default > 0
            n = ringConnections[iTarget];
            if (patternAtom.ringConnectivity == -1 && n == 0
                || patternAtom.ringConnectivity != -1
                && n != patternAtom.ringConnectivity)
              break;
          }
        }
      }
      foundAtom = !foundAtom;
      break;
    }

    return foundAtom;
  }

  private boolean checkMatchBond(SmilesAtom patternAtom, SmilesAtom atom1,
                                 SmilesBond patternBond, int iAtom,
                                 int matchingAtom, Edge bond) {

    // apply SEARCH [ , , & ; ] logic

    if (patternBond.bondsOr != null) {
      for (int ii = 0; ii < patternBond.nBondsOr; ii++)
        if (checkMatchBond(patternAtom, atom1, patternBond.bondsOr[ii], iAtom,
            matchingAtom, bond))
          return true;
      return false;
    }

    if (!isRingCheck && !isTopology)
      if (patternBond.nPrimitives == 0) {
        if (!checkPrimitiveBond(patternBond, iAtom, matchingAtom, bond))
          return false;
      } else {
        for (int i = 0; i < patternBond.nPrimitives; i++) {
          SmilesBond prim = patternBond.setPrimitive(i);
          if (!checkPrimitiveBond(prim, iAtom,
              matchingAtom, bond))
            return false;
        }
      }
    patternBond.matchingBond = bond;
    return true;
  }

  private boolean checkPrimitiveBond(SmilesBond patternBond, int iAtom1,
                                     int iAtom2, Edge bond) {
    boolean bondFound = false;

    switch (patternBond.order) {
    case SmilesBond.TYPE_BIO_SEQUENCE: // +
      return (patternBond.isNot != (targetAtoms[iAtom2].getOffsetResidueAtom("\0",
          1) == targetAtoms[iAtom1].getOffsetResidueAtom("\0", 0)));
    case SmilesBond.TYPE_BIO_CROSSLINK: // :
      return (patternBond.isNot != targetAtoms[iAtom1]
          .isCrossLinked(targetAtoms[iAtom2]));
    }

    boolean isAromatic1 = (!noAromatic && bsAromatic.get(iAtom1));
    boolean isAromatic2 = (!noAromatic && bsAromatic.get(iAtom2));
    int order = bond.getCovalentOrder();
    int patternOrder = patternBond.order;
    if (isAromatic1 && isAromatic2) {
      switch (patternOrder) {
      case SmilesBond.TYPE_AROMATIC:
      case SmilesBond.TYPE_RING:
        bondFound = isRingBond(ringSets, null, iAtom1, iAtom2);
        break;
      case Edge.BOND_COVALENT_SINGLE:
        // for SMARTS, single bond in aromatic means TO ANOTHER RING;
        // for SMILES, we don't care
        bondFound = !isSmarts || !isRingBond(ringSets, getBSAromaticRings(), iAtom1, iAtom2);
        break;
      case Edge.BOND_COVALENT_DOUBLE:
        // note: Freiburg considers TYPE_DOUBLE to be NOT aromatic
        // changed for Jmol 12.2.RC8
        // but this is ambiguous at http://www.daylight.com/dayhtml/doc/theory/theory.smarts.html
        // see, for example: http://opentox.informatik.uni-freiburg.de/depict?data=[H]C%3D1C%28[H]%29%3DC%28[H]%29C%28%3DC%28C%3D1%28[H]%29%29C%28F%29%28F%29F%29S[H]&smarts=[%236]=[%236]
        // however, if it is not SMARTS, then we consider this fine -- it does
        // not matter what the order is for double/single bonds around the ring
        // 
        // starting with JmpatternBond.isNotol 12.3.24, we allow the directive aromaticDouble to allow a
        // distinction between single and double, as for example is necessary to distinguish
        // between n=cNH2 and ncNH2 (necessary for MMFF94 atom typing
        //
        // starting  with Jmol 14.4.5 we allow any presence of a=a to set the aromaticDouble flag automatically
        // and deprecate the  aromaticDouble directive.

        //
        bondFound = isNormalized
            || aromaticDouble
            && (order == Edge.BOND_COVALENT_DOUBLE || order == Edge.BOND_AROMATIC_DOUBLE);
        break;
      case Edge.TYPE_ATROPISOMER:
      case Edge.TYPE_ATROPISOMER_REV:
        bondFound = !patternBond.isNot; // negates this; ensures isNot is used only in stereochem 
        break;
      case SmilesBond.TYPE_ANY:
      case SmilesBond.TYPE_UNKNOWN:
        bondFound = true;
        break;
      }
    } else {
      switch (patternOrder) {
      case SmilesBond.TYPE_AROMATIC: // :
        if (!noAromatic)
          break;
        //$FALL-THROUGH$
      case SmilesBond.TYPE_ANY:
      case SmilesBond.TYPE_UNKNOWN:
        bondFound = true;
        break;
      case Edge.BOND_COVALENT_SINGLE:
      case Edge.BOND_STEREO_NEAR:
      case Edge.BOND_STEREO_FAR:
        switch (order) {
        case Edge.BOND_COVALENT_SINGLE:
        case Edge.BOND_STEREO_NEAR:
        case Edge.BOND_STEREO_FAR:
          bondFound = true;
          break;
        }
        break;
      case Edge.TYPE_ATROPISOMER:
      case Edge.TYPE_ATROPISOMER_REV:
        switch (order) {
        case Edge.BOND_COVALENT_SINGLE:
        case Edge.TYPE_ATROPISOMER:
        case Edge.TYPE_ATROPISOMER_REV:
          bondFound = !patternBond.isNot; // negates this; ensures isNot is used only in stereochem 
          break;
        }
        break;
      case Edge.BOND_COVALENT_DOUBLE:
      case Edge.BOND_COVALENT_TRIPLE:
      case Edge.BOND_COVALENT_QUADRUPLE:
        bondFound = (order == patternOrder);
        break;
      case SmilesBond.TYPE_RING:
        bondFound = isRingBond(ringSets, null, iAtom1, iAtom2);
        break;
      }
    }
    return bondFound != patternBond.isNot;
  }

  private BS getBSAromaticRings() {
    if (bsAromaticRings == null) {
      bsAromaticRings = new BS();
      if (ringSets != null && bsAromatic != null) {
        for (int i = ringSets.size(); --i >= 0;) {
          BS bsRing = (BS) ringSets.get(i).clone();
          bsRing.andNot(bsAromatic);
          if (bsRing.isEmpty())
            bsAromaticRings.set(i);
        }
      }
    }
    return bsAromaticRings;
  }

  static boolean isRingBond(Lst<BS> ringSets, BS bsAromaticRings, int a1, int a2) {
    if (ringSets != null)
      for (int i = ringSets.size(); --i >= 0;) {
        BS bsRing = ringSets.get(i);
        if (bsRing.get(a1) && bsRing.get(a2)) {
          if (bsAromaticRings == null || bsAromaticRings.get(i))
          return true;
        }
      }
    return false;
  }
  
  /* ============================================================= */
  /*                          Stereochemistry                      */
  /* ============================================================= */

  private boolean checkStereochemistry() {

    // first, @ stereochemistry

    for (int i = 0; i < measures.size(); i++)
      if (!measures.get(i).check())
        return false;
    if (stereo != null && !stereo.checkStereoChemistry(this, v))
      return false;

    if (!haveBondStereochemistry)
      return true;

    //  /C=C/ and /C=N/ double bond stereochemistry
    //  a^nn-a stereochemistry

    Lst<SmilesBond> lstAtrop = null;
    SmilesBond b = null;
    for (int k = 0; k < ac; k++) {
      SmilesAtom sAtom1 = patternAtoms[k];
      SmilesAtom sAtom2 = null;
      SmilesAtom sAtomDirected1 = null;
      SmilesAtom sAtomDirected2 = null;
      int dir1 = 0;
      int dir2 = 0;
      int bondType = 0;
      int nBonds = sAtom1.getBondCount();
      boolean isAtropisomer = false;
      boolean indexOrder = true;
      for (int j = 0; j < nBonds; j++) {
        b = sAtom1.getBond(j);
        boolean isAtom2 = (b.atom2 == sAtom1);
        indexOrder = (b.atom1.index < b.atom2.index);
        int type = b.getBondType();
        switch (type) {
        case Edge.TYPE_ATROPISOMER:
        case Edge.TYPE_ATROPISOMER_REV:
          if (!indexOrder)
            continue;
          //$FALL-THROUGH$
        case Edge.BOND_COVALENT_DOUBLE:
          if (isAtom2)
            continue;
          sAtom2 = b.atom2;
          bondType = type;
          isAtropisomer = (type != Edge.BOND_COVALENT_DOUBLE);
          if (isAtropisomer)
            dir1 = (b.isNot ? -1 : 1);
          break;
        case Edge.BOND_STEREO_NEAR:
        case Edge.BOND_STEREO_FAR:
          sAtomDirected1 = (isAtom2 ? b.atom1 : b.atom2);
          dir1 = (isAtom2 != (type == Edge.BOND_STEREO_NEAR) ? 1 : -1);
          break;
        }
      }
      if (isAtropisomer) {

        if (setAtropicity) {
          if (lstAtrop == null)
            lstAtrop = new Lst<SmilesBond>();
          lstAtrop.addLast(b);
          continue;
        }

        SmilesBond b1 = sAtom1.getBond(b.atropType[0]);
        if (b1 == null)
          return false;
        sAtomDirected1 = b1.getOtherAtom(sAtom1);
        b1 = sAtom2.getBond(b.atropType[1]);
        if (b1 == null)
          return false;
        sAtomDirected2 = b1.getOtherAtom(sAtom2);
        if (Logger.debugging)
          Logger.info("atropisomer check for atoms " + sAtomDirected1 + sAtom1
              + " " + sAtom2 + sAtomDirected2);
      } else {
        // double bond
        if (sAtom2 == null || dir1 == 0)
          continue;
        // cumulene stuff here
        // --> new sAtom2
        Node a10 = sAtom1;
        int nCumulene = 0;
        while (sAtom2.getBondCount() == 2 && sAtom2.getValence() == 4) {
          nCumulene++;
          Edge[] e2 = sAtom2.getEdges();
          Edge e = e2[e2[0].getOtherNode(sAtom2) == a10 ? 1 : 0];
          a10 = sAtom2;
          sAtom2 = (SmilesAtom) e.getOtherNode(sAtom2);
        }
        if (nCumulene % 2 == 1)
          continue;
        nBonds = sAtom2.getBondCount();
        for (int j = 0; j < nBonds && dir2 == 0; j++) {
          b = sAtom2.getBond(j);
          int type = b.getBondType();
          switch (type) {
          case Edge.BOND_STEREO_NEAR:
          case Edge.BOND_STEREO_FAR:
            boolean isAtom2 = (b.atom2 == sAtom2);
            sAtomDirected2 = (isAtom2 ? b.atom1 : b.atom2);
            dir2 = (isAtom2 != (type == Edge.BOND_STEREO_NEAR) ? 1 : -1);
            break;
          }
        }
        if (dir2 == 0)
          continue;
      }
      Node dbAtom1 = sAtom1.getMatchingAtom();
      Node dbAtom2 = sAtom2.getMatchingAtom();
      Node dbAtom1a = sAtomDirected1.getMatchingAtom();
      Node dbAtom2a = sAtomDirected2.getMatchingAtom();
      if (dbAtom1a == null || dbAtom2a == null)
        return false;
      if (haveTopo)
        setTopoCoordinates((SmilesAtom) dbAtom1, (SmilesAtom) dbAtom2,
            (SmilesAtom) dbAtom1a, (SmilesAtom) dbAtom2a, bondType);
      float d = SmilesMeasure.setTorsionData((T3) dbAtom1a, (T3) dbAtom1,
          (T3) dbAtom2, (T3) dbAtom2a, v, isAtropisomer);
      if (isAtropisomer) {
        // just looking for d value that is positive (0 to 180)
        // the dihedral, from front to back, will be positive:  0 to 180 range 
        // dir1 is 1 or -1(NOT)
        d *= dir1 * (bondType == Edge.TYPE_ATROPISOMER ? 1 : -1) * (indexOrder ? 1 : -1)* ATROPIC_SWITCH * -1;
        if (Logger.debugging)
          Logger.info("atrop dihedral " + d + " " + sAtom1 + " " + sAtom2 + " " +  b);
        if (d < 1.0f) // don't count a fraction of a degree as sufficient
          return false;
      } else {
        // for \C=C\, (dir1*dir2 == -1), dot product should be negative
        // because the bonds are oppositely directed
        // for \C=C/, (dir1*dir2 == 1), dot product should be positive
        // because the bonds are only about 60 degrees apart
        if (v.vTemp1.dot(v.vTemp2) * dir1 * dir2 < 0)
          return false;
      }
    }
    if (setAtropicity) {
      // the goal here is to match up the Jmol atropisomerism dihedral
      // atoms with the SMILES version
      atropKeys = "";
      for (int i = 0; i < lstAtrop.size(); i++)
        atropKeys += "," + getAtropIndex(lstAtrop.get(i));
    }
    return true;

  }

  private String getAtropIndex(SmilesBond b) {
    Node[] nodes = new Node[4];
    String s = "";
    nodes[1] = b.atom1.getMatchingAtom();
    nodes[2] = b.atom2.getMatchingAtom();
    SmilesBond[] b1 = b.atom1.bonds;
    SmilesAtom a;
    for (int i = b.atom1.getBondCount(); --i >= 0;) {
      if ((a = (SmilesAtom) b1[i].getOtherNode(b.atom1)) != b.atom2) {
        s += (i + 1);
        nodes[0] = a.getMatchingAtom();
        break;
      }
    }
    b1 = b.atom2.bonds;
    for (int i = 0; i <= b.atom2.getBondCount(); i++) {
      if ((a = (SmilesAtom) b1[i].getOtherNode(b.atom2)) != b.atom1) {
        s += (i + 1);
        nodes[3] = a.getMatchingAtom();
        break;
      }
    }
    if (s.equals("22"))
      s = "";
    s = (SmilesStereo.getAtropicStereoFlag(nodes) == 1 ? "" : "^") + s;
    return (s + "   ").substring(0, 3);
  }

  private static void setTopoCoordinates(SmilesAtom dbAtom1, SmilesAtom dbAtom2,
                                      SmilesAtom dbAtom1a, SmilesAtom dbAtom2a,
                                      int bondType) {
    dbAtom1.set(-1, 0, 0);
    dbAtom2.set(1, 0, 0);
    if (bondType != Edge.BOND_COVALENT_DOUBLE) {

      // atropisomerism
      //
      // we will be looking for a + or - dihedral angle
      // so just set that

      // have to reconcile 12,23 choice with atropType for TOPO -- currently NULL!
      
      
      SmilesBond bond = dbAtom1.getBondTo(dbAtom2);
      boolean ok1 = dbAtom1.getBondedAtomIndex(bond.atropType[0]) == dbAtom1a.index;
      boolean ok2 = dbAtom2.getBondedAtomIndex(bond.atropType[1]) == dbAtom2a.index;
      int dir = (bond.order == Edge.TYPE_ATROPISOMER ? 1 : -1) * (ok1 == ok2 ? 1 : -1);
      dbAtom1a.set(-1, 1, 0);
      dbAtom2a.set(1, 1, dir / 2.0f * ATROPIC_SWITCH * -1);
      //System.out.println(Arrays.toString(bond.atropType) + " " + bond.order + " " + dbAtom1a + " " + dbAtom1 + " " + dbAtom2 + " " + dbAtom2a + " " + dir);
      return;
    }

    // Note that the directionality of the bond depends upon whether
    // the alkene C is the first or the second atom in the bond. 
    // if it is the first -- C(/X)= or C/1= -- then the X is UP
    // but if it is the second: -- X/C= or X/1... C1= -- then the X is DOWN
    //
    //                         C C       C     C
    //                        / /         \   /
    //      C(/C)=C/C  ==    C=C     ==    C=C     ==   C\C=C/C   
    //
    // because what we are doing is translating the / or \ vertically
    // to match the atoms it is connected to. Same with rings:
    //
    //                       CCC C     CCC     C
    //                        / /         \   /
    //  C1CC.C/1=C/C  ==     C=C    ==     C=C     ==   CCC\C=C/C   
    //
    // If the branch ALSO has a double bond,
    // then for THAT double bond we will have it the normal way:
    //
    //                              Br
    //                             /    BR
    //                          C=C      \
    //                         / C        C=C     C
    //                        / /            \   /
    //  C(/C=C/Br)=C/C  ==   C=C     ==       C=C     ==  Br\C=C\C=C/C   
    // 
    // interesting case for ring connections:
    //
    // Br/C=C\1OCCC.C/1=C/C=C/CCS/C=C\2CCCC.NN/2
    //
    // Note that that directionality of the matching ring bonds must be OPPOSITE.
    // Better is to not show it both places:
    //
    // Br/C=C\1OCCC.C/1=C/C=C/CCS/C=C\2CCCC.NN/2
    //

    int nBonds = 0;
    int dir1 = 0;
    Edge[] bonds = dbAtom1.getEdges();
    for (int k = bonds.length; --k >= 0;) {
      Edge bond = bonds[k];
      if (bond.order == Edge.BOND_COVALENT_DOUBLE)
        continue;
      SimpleNode atom = bond.getOtherNode(dbAtom1);
      ((Node) atom).set(-1, (nBonds++ == 0) ? -1 : 1, 0);
      int mode = (bond.getAtomIndex2() == dbAtom1.getIndex() ? nBonds : -nBonds);
      switch (bond.order) {
      case Edge.BOND_STEREO_NEAR:
        dir1 = mode;
        break;
      case Edge.BOND_STEREO_FAR:
        dir1 = -mode;
      }
    }
    int dir2 = 0;
    nBonds = 0;
    SimpleNode[] atoms = new Node[2];
    bonds = dbAtom2.getEdges();
    for (int k = bonds.length; --k >= 0;) {
      Edge bond = bonds[k];
      if (bond.order == Edge.BOND_COVALENT_DOUBLE)
        continue;
      SimpleNode atom = bond.getOtherNode(dbAtom2);
      atoms[nBonds] = atom;
      ((Node) atom).set(1, (nBonds++ == 0) ? 1 : -1, 0);
      int mode = (bond.getAtomIndex2() == dbAtom2.getIndex() ? nBonds : -nBonds);
      switch (bond.order) {
      case Edge.BOND_STEREO_NEAR:
        dir2 = mode;
        break;
      case Edge.BOND_STEREO_FAR:
        dir2 = -mode;
      }

    }
    //     2     3
    //      \   /
    //       C=C
    //      /   \
    //     1     4
    //
    // check for overall directionality matching even/oddness of bond order
    // and switch Y positions of 3 and 4 if necessary
    //  
    if ((dir1 * dir2 > 0) == (Math.abs(dir1) % 2 == Math.abs(dir2) % 2)) {
      float y = ((P3) atoms[0]).y;
      ((P3) atoms[0]).y = ((P3) atoms[1]).y;
      ((P3) atoms[1]).y = y;
    }
  }


  /**
   * 
   * @param bsAro
   *        null for molecular formula calculation only
   * @throws InvalidSmilesException
   * 
   */
  void createTopoMap(BS bsAro) throws InvalidSmilesException {
    boolean isForMF = (bsAro == null);
    int nAtomsMissing = getMissingHydrogenCount();
    int totalAtoms = ac + nAtomsMissing;
    SmilesAtom[] atoms = new SmilesAtom[totalAtoms];
    targetAtoms = atoms;
    for (int i = 0, ptAtom = 0; i < ac; i++, ptAtom++) {
      SmilesAtom sAtom = patternAtoms[i];
      // this number will include the number of Hs in [CH2] as well as the number needed by C by itself
      int n = sAtom.explicitHydrogenCount;
      if (n < 0)
        n = 0;
      // create a Jmol atom for this pattern atom
      // we co-opt atom.matchingAtom here
      // because this search will never actually be run
      SmilesAtom atom = atoms[ptAtom] = new SmilesAtom().setTopoAtom(sAtom.component, ptAtom,
          sAtom.symbol, sAtom.getCharge(), i);
      atom.implicitHydrogenCount = n;
      if (isForMF)
        continue;
      atom.mapIndex = i;
      atom.stereo = sAtom.stereo;
      atom.setAtomicMass(sAtom.getAtomicMass());
      atom.bioAtomName = sAtom.bioAtomName;
      atom.residueName = sAtom.residueName;
      atom.residueChar = sAtom.residueChar;
      atom.residueNumber = sAtom.residueNumber;
      atom.atomNumber = sAtom.residueNumber;
      atom.insCode = sAtom.insCode;
      atom.atomClass = sAtom.atomClass;
      atom.explicitHydrogenCount = 0;
      atom.isBioAtom = sAtom.isBioAtom;
      atom.bioType = sAtom.bioType;
      atom.isLeadAtom = sAtom.isLeadAtom;

      // we pass on the aromatic flag because
      // we don't want SmilesSearch to calculate
      // that for us
      if (!isForMF && sAtom.isAromatic)
        bsAro.set(ptAtom);

      // set up the bonds array and fill with H atoms
      // when there is only 1 H and the atom is NOT FIRST, then it will
      // be important to designate the bonds in order -- with the
      // H SECOND not first
      // this is still not satisfactory for allenes or the second atom of 
      // imines and possibly double bonds. We handle that later.

      sAtom.setMatchingAtom(null, ptAtom);
      SmilesBond[] bonds = new SmilesBond[sAtom.getBondCount() + n];
      atom.setBonds(bonds);
      while (--n >= 0) {
        SmilesAtom atomH = atoms[++ptAtom] = new SmilesAtom().setTopoAtom(atom.component,
            ptAtom, "H", 0, -1);
        atomH.mapIndex = -i - 1;
        atomH.setBonds(new SmilesBond[1]);
        SmilesBond b = new SmilesBond(atom, atomH, Edge.BOND_COVALENT_SINGLE,
            false);
        if (Logger.debugging)
          Logger.info("" + b);
      }
    }
    if (isForMF)
      return;
    // set up bonds
    for (int i = 0; i < ac; i++) {
      SmilesAtom sAtom = patternAtoms[i];
      int i1 = sAtom.getMatchingAtomIndex();
      SmilesAtom atom1 = atoms[i1];
      int n = sAtom.getBondCount();
      for (int j = 0; j < n; j++) {
        SmilesBond sBond = sAtom.getBond(j);
        boolean firstAtom = (sBond.atom1 == sAtom);
        //SmilesBond b;
        if (firstAtom) {
          int order = 1;
          switch (sBond.order) {
          case Edge.BOND_COVALENT_SINGLE:
          case Edge.BOND_COVALENT_DOUBLE:
          case Edge.BOND_COVALENT_TRIPLE:
          case Edge.BOND_COVALENT_QUADRUPLE:
          case Edge.BOND_STEREO_NEAR:
          case Edge.BOND_STEREO_FAR:
          case Edge.TYPE_ATROPISOMER:
          case Edge.TYPE_ATROPISOMER_REV:
          case SmilesBond.TYPE_BIO_CROSSLINK:
          case SmilesBond.TYPE_BIO_SEQUENCE:
            order = sBond.order;
            break;
          case SmilesBond.TYPE_AROMATIC:
            order = Edge.BOND_AROMATIC_DOUBLE;
            break;
          }
          SmilesAtom atom2 = atoms[sBond.atom2.getMatchingAtomIndex()];
          SmilesBond b = new SmilesBond(atom1, atom2, order, false);
          b.isConnection = sBond.isConnection;
          b.atropType = sBond.atropType;
          b.isNot = sBond.isNot;
          // do NOT add this bond to the second atom -- we will do that later;
          atom2.bondCount--;
          if (Logger.debugging)
            Logger.info("" + b);
        } else {
          SmilesAtom atom2 = atoms[sBond.atom1.getMatchingAtomIndex()];
          SmilesBond b = atom2.getBondTo(atom1);
          // NOW we can add this bond
          atom1.addBond(b);
        }
      }
    }
    // fix H atoms
    for (int i = 0; i < totalAtoms; i++) {
      SmilesAtom a = atoms[i];
      SmilesBond[] bonds = a.bonds;
      if (bonds.length < 2 || bonds[0].isFromPreviousTo(a))
        continue;
      for (int k = bonds.length; --k >= 1;)
        if (bonds[k].isFromPreviousTo(a)) {
          SmilesBond b = bonds[k];
          bonds[k] = bonds[0];
          bonds[0] = b;
          break;          
        }
    }
    if (!ignoreStereochemistry)
      // should also be checking for subsearches and htNested?
      for (int i = ac; --i >= 0;) {
        SmilesAtom sAtom = patternAtoms[i];
        if (sAtom.stereo != null)
          sAtom.stereo.fixStereo(sAtom);
      }

  }

  /**
   * create a temporary object to generate the aromaticity in a SMILES pattern
   * for which there is no explicit aromaticity (Kekule)
   * 
   * Not applicable to SMARTS
   * 
   * @param bsAromatic
   * @throws InvalidSmilesException
   */
  void normalizeAromaticity(BS bsAromatic)
      throws InvalidSmilesException {
    SmilesAtom[] atoms = this.patternAtoms;
    SmilesSearch ss = new SmilesSearch();
    ss.noAromatic = noAromatic;
    ss.setFlags(flags);
    ss.targetAtoms = atoms;
    ss.targetAtomCount = atoms.length;
    ss.bsSelected = BSUtil.newBitSet2(0, atoms.length);
    Lst<BS>[] vRings = AU.createArrayOfArrayList(4);
    ss.setRingData(null, vRings, true);
    bsAromatic.or(ss.bsAromatic);
    if (!bsAromatic.isEmpty()) {
      Lst<BS> lst = vRings[3]; // aromatic rings
      for (int i = lst.size(); --i >= 0;) {
        BS bs = lst.get(i);
        for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1)) {
          SmilesAtom a = atoms[j];
          if (a.isAromatic || a.elementNumber == -2 || a.elementNumber == 0)
            continue;
          a.setSymbol(a.symbol.toLowerCase());
        }
      }
    }
  }

  /**
   * htNested may contain $(select xxxx) primitives. 
   * We want to clear those up before we start any search.
   * 
   */
  void getSelections() {
    Map<String, Object> ht = top.htNested;
    if (ht == null || targetAtoms.length == 0)
      return;
    Map<String, Object> htNew = new Hashtable<String, Object>();
    for (Map.Entry<String, Object> entry : ht.entrySet()) {
      String key = entry.getValue().toString();
      if (key.startsWith("select")) {
        BS bs = (htNew.containsKey(key) ? (BS) htNew.get(key) 
            : targetAtoms[0].findAtomsLike(key.substring(6)));
        if (bs == null)
          bs = new BS();
        htNew.put(key, bs);
        entry.setValue(bs);
      }
    }
  }
  
  Node findImplicitHydrogen(Node atom) {
//    if (haveTopo) {
//      SmilesAtom sAtom = (SmilesAtom) atom;
//      SmilesBond[] b = sAtom.bonds;
//      for (int i = 0; i < b.length; i++) {
//        SmilesAtom a = b[i].getOtherAtom(sAtom);
//        if (a.mapIndex < 0)
//          return a;
//      }
//    }
    Edge[] edges = atom.getEdges();
    for (int i = edges.length; --i >= 0;) {
      int k = atom.getBondedAtomIndex(i);
      if (targetAtoms[k].getElementNumber() == 1 && !bsFound.get(k))
        return targetAtoms[k];
    }
    return null;
  }

  @Override
  public String toString() {
    SB sb = new SB().append(pattern);
    sb.append("\nmolecular formula: " + getMolecularFormula(true, null, false)); 
    return sb.toString();    
  }


}

