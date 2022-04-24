/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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

package org.jmol.modelsetbio;

import java.util.Arrays;

import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;


import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.Bond;
import org.jmol.modelset.Chain;
import org.jmol.modelset.Group;
import org.jmol.modelset.Model;
import org.jmol.modelset.ModelLoader;
import org.jmol.modelset.ModelSet;
import org.jmol.script.SV;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.Edge;
import org.jmol.util.Logger;

import javajs.util.AU;
import javajs.util.Measure;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.V3;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolAdapterAtomIterator;
import org.jmol.api.JmolAdapterStructureIterator;
import org.jmol.c.STR;

/**
 * a class used by ModelLoader to handle all loading
 * of operations specific to PDB/mmCIF files. By loading
 * only by class name, only loaded if PDB file is called.
 * 
 * In addition, constants relating only to PDB files are here
 * -- for coloring by chain, selecting by protein, etc. 
 *
 * 
 */
public final class BioResolver implements Comparator<String[]> {

  public final static Map<String, Short> htGroup = new Hashtable<String, Short>();

  private Viewer vwr;

  public BioResolver() {
    // only implemented via reflection, and only for PDB/mmCIF files
  }

  private V3 vAB;
  private V3 vNorm;
  private P4 plane;

  private ModelLoader ml;
  private ModelSet ms;

  private BS bsAddedMask;
  private int lastSetH = Integer.MIN_VALUE;
  private int maxSerial = 0;
  private boolean haveHsAlready;

  public BioResolver setLoader(ModelLoader modelLoader) {
    ml = modelLoader;
    bsAddedMask = null;
    lastSetH = Integer.MIN_VALUE;
    maxSerial = 0;
    haveHsAlready = false;
    if (modelLoader == null) {
      ms = null;
      bsAddedHydrogens = bsAtomsForHs = bsAssigned = null;
      htBondMap = null;
      htGroupBonds = null;
      hNames = null;
    } else {
      Group.specialAtomNames = specialAtomNames;
      ms = modelLoader.ms;
      vwr = modelLoader.ms.vwr;
      modelLoader.specialAtomIndexes = new int[ATOMID_MAX];
      hasCONECT = (ms.getInfoM(JC.getBoolName(JC.GLOBAL_CONECT)) == Boolean.TRUE);
    }
    return this;
  }
  
  public BioResolver setViewer(Viewer vwr) {
    this.vwr = vwr;
    if (Group.standardGroupList == null) {
      //generate a static list of common amino acids, nucleic acid bases, and solvent components
      SB s = new SB();
      //for menu presentation order      
      for (int i = 1; i < JC.GROUPID_WATER; i++)
        s.append(",[").append(predefinedGroup3Names[i]).append("]");
      s.append(allCarbohydrates);
      group3Count = s.length() / 6;
      Group.standardGroupList = s.toString();
      for (int i = 0, n = predefinedGroup3Names.length; i < n; ++i)
        addGroup3Name(predefinedGroup3Names[i].trim());
    }
    return this;
  }
  
  public Model getBioModel(int modelIndex,
                        int trajectoryBaseIndex, String jmolData,
                        Properties modelProperties,
                        Map<String, Object> modelAuxiliaryInfo) {
    return new BioModel(ms, modelIndex, trajectoryBaseIndex,
        jmolData, modelProperties, modelAuxiliaryInfo);
  }

  public Group distinguishAndPropagateGroup(Chain chain, String group3,
                                            int seqcode, int firstAtomIndex,
                                            int lastAtomIndex,
                                            int[] specialAtomIndexes,
                                            Atom[] atoms) {
    /*
     * called by finalizeGroupBuild()
     * 
     * first: build array of special atom names, for example "CA" for the alpha
     * carbon is assigned #2 see JmolConstants.specialAtomNames[] the special
     * atoms all have IDs based on Atom.lookupSpecialAtomID(atomName) these will
     * be the same for each conformation
     * 
     * second: creates the monomers themselves based on this information thus
     * building the byte offsets[] array for each monomer, indicating which
     * position relative to the first atom in the group is which atom. Each
     * monomer.offsets[i] then points to the specific atom of that type these
     * will NOT be the same for each conformation
     */

    int mask = 0;

    // clear previous specialAtomIndexes
    for (int i = ATOMID_MAX; --i >= 0;)
      specialAtomIndexes[i] = Integer.MIN_VALUE;

    // go last to first so that FIRST confirmation is default
    for (int i = lastAtomIndex; i >= firstAtomIndex; --i) {
      int specialAtomID = atoms[i].atomID;
      if (specialAtomID <= 0)
        continue;
      if (specialAtomID < JC.ATOMID_DISTINGUISHING_ATOM_MAX) {
        /*
         * save for future option -- turns out the 1jsa bug was in relation to
         * an author using the same group number for two different groups
         * 
         * if ((distinguishingBits & (1 << specialAtomID) != 0) {
         * 
         * //bh 9/21/2006: //
         * "if the group has two of the same, that cannot be right." // Thus,
         * for example, two C's doth not make a protein "carbonyl C"
         * distinguishingBits = 0; break; }
         */
        mask |= (1 << specialAtomID);
      }
      specialAtomIndexes[specialAtomID] = i;
    }

    Monomer m = null;
    if ((mask & JC.ATOMID_PROTEIN_MASK) == JC.ATOMID_PROTEIN_MASK)
      m = AminoMonomer.validateAndAllocate(chain, group3, seqcode,
          firstAtomIndex, lastAtomIndex, specialAtomIndexes, atoms);
    else if (mask == JC.ATOMID_ALPHA_ONLY_MASK)
      m = AlphaMonomer.validateAndAllocateA(chain, group3, seqcode,
          firstAtomIndex, lastAtomIndex, specialAtomIndexes);
    else if (((mask & JC.ATOMID_NUCLEIC_MASK) == JC.ATOMID_NUCLEIC_MASK))
      m = NucleicMonomer.validateAndAllocate(chain, group3, seqcode,
          firstAtomIndex, lastAtomIndex, specialAtomIndexes);
    else if (mask == JC.ATOMID_PHOSPHORUS_ONLY_MASK)
      m = PhosphorusMonomer.validateAndAllocateP(chain, group3, seqcode,
          firstAtomIndex, lastAtomIndex, specialAtomIndexes);
    else if (checkCarbohydrate(group3))
      m = CarbohydrateMonomer.validateAndAllocate(chain, group3, seqcode,
          firstAtomIndex, lastAtomIndex);
    return ( m != null && m.leadAtomIndex >= 0 ? m : null);
  }   
  
  //////////// ADDITION OF HYDROGEN ATOMS /////////////
  // Bob Hanson and Erik Wyatt, Jmol 12.1.51, 7/1/2011
  
  /*
   * for each group, as it is finished in the file reading:
   * 
   * 1) get and store atom/bond information for group type
   * 2) add placeholder (deleted) hydrogen atoms to a group
   * 
   * in the end:
   * 
   * 3) set multiple bonding and charges
   * 4) determine actual number of required hydrogen atoms
   * 5) set hydrogen atom names, atom numbers, and positions 
   * 6) undelete those atoms  
   * 
   */
  
  public void setHaveHsAlready(boolean b) {
    haveHsAlready = b;
  }

  private BS bsAddedHydrogens;
  private BS bsAtomsForHs;
  private Map<String, String>htBondMap;
  private Map<String, Boolean>htGroupBonds;
  private String[] hNames;
  private int baseBondIndex = 0;

  private boolean hasCONECT;

  public void initializeHydrogenAddition() {
    baseBondIndex = ms.bondCount;
    bsAddedHydrogens = new BS();
    bsAtomsForHs = new BS();
    htBondMap = new Hashtable<String, String>();
    htGroupBonds = new Hashtable<String, Boolean>();
    hNames = new String[3];
    vAB = new V3();
    vNorm = new V3();
    plane = new P4();
  }
  
  /**
   * Get bonding info for double bonds and add implicit hydrogen atoms, if needed.
   * 
   * @param adapter
   * @param iGroup this group
   * @param nH legacy quirk
   */
  public void addImplicitHydrogenAtoms(JmolAdapter adapter, int iGroup, int nH) {
    String group3 = ml.getGroup3(iGroup);
    int nH1;
    if (haveHsAlready && hasCONECT 
        || group3 == null
        || (nH1 = getStandardPdbHydrogenCount(group3)) == 0)
      return;
    nH = (nH1 < 0 ? -1 : nH1 + nH);
    Object model = null;
    int iFirst = ml.getFirstAtomIndex(iGroup);
    int ac = ms.ac;
    if (nH < 0) {
      if (ac - iFirst == 1) // CA or P-only, or simple metals, also HOH, DOD
        return;
      model = vwr.getLigandModel(group3, "ligand_", "_data", null);
      if (model == null)
        return;
      nH = adapter.getHydrogenAtomCount(model);
      if (nH < 1)
        return;
    }
    getBondInfo(adapter, group3, model);
    ms.am[ms.at[iFirst].mi].isPdbWithMultipleBonds = true;
    if (haveHsAlready)
      return;
    bsAtomsForHs.setBits(iFirst, ac);
    bsAddedHydrogens.setBits(ac, ac + nH);
    boolean isHetero = ms.at[iFirst].isHetero();
    P3 xyz = P3.new3(Float.NaN, Float.NaN, Float.NaN);
    Atom a = ms.at[iFirst];
    for (int i = 0; i < nH; i++)
      ms.addAtom(a.mi, a.group, 1, "H", null, 0, a.getSeqID(), 0, xyz, null,
          Float.NaN, null, 0, 0, 1, 0, null, isHetero, (byte) 0, null, Float.NaN)
          .delete(null);
  }

  private void getBondInfo(JmolAdapter adapter, String group3, Object model) {
    if (htGroupBonds.get(group3) != null)
      return;
    String[][] bondInfo = (model == null ? getPdbBondInfo(group3,
        vwr.getBoolean(T.legacyhaddition)) : getLigandBondInfo(adapter, model, group3));
    if (bondInfo == null)
      return;
    htGroupBonds.put(group3, Boolean.TRUE);
    for (int i = 0; i < bondInfo.length; i++) {
      if (bondInfo[i] == null)
        continue;
      if (bondInfo[i][1].charAt(0) == 'H')
        htBondMap.put(group3 + "." + bondInfo[i][0], bondInfo[i][1]);
      else
        htBondMap.put(group3 + ":" + bondInfo[i][0] + ":" + bondInfo[i][1],
            bondInfo[i][2]);
    }
  }

  /**
   * reads PDB ligand CIF info and creates a bondInfo object.
   * 
   * @param adapter
   * @param model
   * @param group3 
   * @return      [[atom1, atom2, order]...]
   */
  private String[][] getLigandBondInfo(JmolAdapter adapter, Object model, String group3) {
    String[][] dataIn = adapter.getBondList(model);
    Map<String, P3> htAtoms = new Hashtable<String, P3>();
    JmolAdapterAtomIterator iterAtom = adapter.getAtomIterator(model);
    while (iterAtom.hasNext())
      htAtoms.put(iterAtom.getAtomName(), iterAtom.getXYZ());      
    String[][] bondInfo = new String[dataIn.length * 2][];
    int n = 0;
    for (int i = 0; i < dataIn.length; i++) {
      String[] b = dataIn[i];
      if (b[0].charAt(0) != 'H')
        bondInfo[n++] = new String[] { b[0], b[1], b[2],
            b[1].startsWith("H") ? "0" : "1" };
      if (b[1].charAt(0) != 'H')
        bondInfo[n++] = new String[] { b[1], b[0], b[2],
            b[0].startsWith("H") ? "0" : "1" };
    }
    Arrays.sort(bondInfo, this);
    // now look for 
    String[] t;
    for (int i = 0; i < n;) {
      t = bondInfo[i];
      String a1 = t[0];
      int nH = 0;
      int nC = 0;
      for (; i < n && (t = bondInfo[i])[0].equals(a1); i++) {
        if (t[3].equals("0")) {
          nH++;
          continue;
        }
        if (t[3].equals("1"))
          nC++;
      }
      int pt = i - nH - nC;
      if (nH == 1)
        continue;
      switch (nC) {
      case 1:
        char sep = (nH == 2 ? '@' : '|');
        for (int j = 1; j < nH; j++) {
          bondInfo[pt][1] += sep + bondInfo[pt + j][1];
          bondInfo[pt + j] = null;
        }
        continue;
      case 2:
        if (nH != 2)
          continue;
        String name = bondInfo[pt][0];
        String name1 = bondInfo[pt + nH][1];
        String name2 = bondInfo[pt + nH + 1][1];
        int factor = name1.compareTo(name2);
        Measure.getPlaneThroughPoints(htAtoms.get(name1), htAtoms.get(name), htAtoms.get(name2), vNorm, vAB,
            plane);
        float d = Measure.distanceToPlane(plane, htAtoms.get(bondInfo[pt][1])) * factor;
        bondInfo[pt][1] = (d > 0 ? bondInfo[pt][1] + "@" + bondInfo[pt + 1][1]
            :  bondInfo[pt + 1][1] + "@" + bondInfo[pt][1]);
        bondInfo[pt + 1] = null;
      }
    }
    for (int i = 0; i < n; i++) {
      if ((t = bondInfo[i]) != null && t[1].charAt(0) != 'H' && t[0].compareTo(t[1]) > 0) {
        bondInfo[i] = null;
        continue;
      }
      if (t != null)
        Logger.info(" ligand " + group3 + ": " + bondInfo[i][0] + " - " + bondInfo[i][1] + " order " + bondInfo[i][2]);
    }
    return bondInfo;
  }
  
  @Override
  public int compare(String[] a, String[] b) {
    return (b == null ? (a == null ? 0 : -1) : a == null ? 1 : a[0]
        .compareTo(b[0]) < 0 ? -1 : a[0].compareTo(b[0]) > 0 ? 1 : a[3]
        .compareTo(b[3]) < 0 ? -1 : a[3].compareTo(b[3]) > 0 ? 1 : a[1]
        .compareTo(b[1]) < 0 ? -1 : a[1].compareTo(b[1]) > 0 ? 1 : 0);
  }
  
  public void finalizeHydrogens() {
    vwr.getLigandModel(null, null, null, null);
    finalizePdbMultipleBonds();
    addHydrogens();
  }

  private void addHydrogens() {
    if (bsAddedHydrogens.nextSetBit(0) < 0)
      return;
    bsAddedMask = BSUtil.copy(bsAddedHydrogens);
    finalizePdbCharges();
    int[] nTotal = new int[1];
    P3[][] pts = ms.calculateHydrogens(bsAtomsForHs, nTotal, null, AtomCollection.CALC_H_DOALL);
    Group groupLast = null;
    int ipt = 0;
    Atom atom;
    for (int i = 0; i < pts.length; i++) {
      if (pts[i] == null || (atom = ms.at[i]) == null)
        continue;
      Group g = atom.group;
      if (g != groupLast) {
        groupLast = g;
        ipt = g.lastAtomIndex;
        while (bsAddedHydrogens.get(ipt))
          ipt--;
      }
      String gName = atom.getGroup3(false);
      String aName = atom.getAtomName();
      String hName = htBondMap.get(gName + "." + aName);
      if (hName == null)
        continue;
      boolean isChiral = hName.contains("@");
      boolean isMethyl = (hName.endsWith("?") || hName.indexOf("|") >= 0);
      int n = pts[i].length;
      if (n == 3 && !isMethyl && hName.equals("H@H2")) {
        hName = "H|H2|H3";
        isMethyl = true;
        isChiral = false;
      }
      if (isChiral && n == 3 || isMethyl != (n == 3)) {
        Logger.info("Error adding H atoms to " + gName + g.getResno() + ": "
            + pts[i].length + " atoms should not be added to " + aName);
        continue;
      }
      int pt = hName.indexOf("@");
      switch (pts[i].length) {
      case 1:
        if (pt > 0)
          hName = hName.substring(0, pt);
        setHydrogen(i, ++ipt, hName, pts[i][0]);
        break;
      case 2:
        String hName1,
        hName2;
        float d = -1;
        Bond[] bonds = atom.bonds;
        if (bonds != null)
          switch (bonds.length) {
          case 2:
            // could be nitrogen?
            Atom atom1 = bonds[0].getOtherAtom(atom);
            Atom atom2 = bonds[1].getOtherAtom(atom);
            int factor = atom1.getAtomName().compareTo(atom2.getAtomName());
            d = Measure.distanceToPlane(Measure.getPlaneThroughPoints(atom1, atom, atom2, vNorm, vAB,
                plane), pts[i][0]) * factor;
            break;
          }
        if (pt < 0) {
          Logger.info("Error adding H atoms to " + gName + g.getResno()
              + ": expected to only need 1 H but needed 2");
          hName1 = hName2 = "H";
        } else if (d < 0) {
          hName2 = hName.substring(0, pt);
          hName1 = hName.substring(pt + 1);
        } else {
          hName1 = hName.substring(0, pt);
          hName2 = hName.substring(pt + 1);
        }
        setHydrogen(i, ++ipt, hName1, pts[i][0]);
        setHydrogen(i, ++ipt, hName2, pts[i][1]);
        break;
      case 3:
        int pt1 = hName.indexOf('|');
        if (pt1 >= 0) {
          int pt2 = hName.lastIndexOf('|');
          hNames[0] = hName.substring(0, pt1);
          hNames[1] = hName.substring(pt1 + 1, pt2);
          hNames[2] = hName.substring(pt2 + 1);
        } else {
          hNames[0] = hName.replace('?', '1');
          hNames[1] = hName.replace('?', '2');
          hNames[2] = hName.replace('?', '3');
        }
        //          Measure.getPlaneThroughPoints(pts[i][0], pts[i][1], pts[i][2], vNorm, vAB,
        //            vAC, plane);
        //      d = Measure.distanceToPlane(plane, atom);
        //    int hpt = (d < 0 ? 1 : 2);
        setHydrogen(i, ++ipt, hNames[0], pts[i][0]);
        setHydrogen(i, ++ipt, hNames[1], pts[i][2]);
        setHydrogen(i, ++ipt, hNames[2], pts[i][1]);
        break;
      }
    }
    deleteUnneededAtoms();
    ms.fixFormalCharges(BSUtil.newBitSet2(ml.baseAtomIndex, ml.ms.ac));
  }

  /**
   * Delete hydrogen atoms that are still in bsAddedHydrogens, 
   * because they were not actually added.
   * Also delete ligand hydrogen atoms from CO2- and PO3(2-)
   * 
   * Note that we do this AFTER all atoms have been added. That means that
   * this operation will not mess up atom indexing
   * 
   */
  private void deleteUnneededAtoms() {
    BS bsBondsDeleted = new BS();
    for (int i = bsAtomsForHs.nextSetBit(0); i >= 0; i = bsAtomsForHs
        .nextSetBit(i + 1)) {
      Atom atom = ms.at[i];
      // specifically look for neutral HETATM O with a bond count of 2: 
      if (!atom.isHetero() || atom.getElementNumber() != 8 || atom.getFormalCharge() != 0
          || atom.getCovalentBondCount() != 2)
        continue;
      Bond[] bonds = atom.bonds;
      Atom atom1 = bonds[0].getOtherAtom(atom);
      Atom atomH = bonds[1].getOtherAtom(atom);
      if (atom1.getElementNumber() == 1) {
        Atom a = atom1;
        atom1 = atomH;
        atomH = a;
      }
      
      // Does it have an H attached?
      if (atomH.getElementNumber() != 1)
        continue;
      // If so, does it have an attached atom that is doubly bonded to O?
      // so this could be RSO4H or RPO3H2 or RCO2H
      Bond[] bonds1 = atom1.bonds;
      for (int j = 0; j < bonds1.length; j++) {
        if (bonds1[j].order == 2) {
          Atom atomO = bonds1[j].getOtherAtom(atom1);
          if (atomO.getElementNumber() == 8) {
            bsAddedHydrogens.set(atomH.i);
            atomH.delete(bsBondsDeleted);
            break;
          }
        }

      }
    }
    ms.deleteBonds(bsBondsDeleted, true);
    deleteAtoms(bsAddedHydrogens);
  }
  
  /**
   * called from org.jmol.modelsetbio.resolver when adding hydrogens.
   * 
   * @param bsDeletedAtoms
   */
  private void deleteAtoms(BS bsDeletedAtoms) {
    // get map
    int[] mapOldToNew = new int[ms.ac];
    int[] mapNewToOld = new int[ms.ac - bsDeletedAtoms.cardinality()];
    int n = ml.baseAtomIndex;
    Model[] models = ms.am;
    Atom[] atoms = ms.at;
    for (int i = ml.baseAtomIndex; i < ms.ac; i++) {
      Atom a = atoms[i];
      if (a == null)
        continue;
      models[a.mi].bsAtoms.clear(i);
      models[a.mi].bsAtomsDeleted.clear(i);
      if (bsDeletedAtoms.get(i)) {
        mapOldToNew[i] = n - 1;
        models[atoms[i].mi].act--;
      } else {
        mapNewToOld[n] = i;
        mapOldToNew[i] = n++;
      }
    }
    ms.msInfo.put("bsDeletedAtoms", bsDeletedAtoms);
    // adjust group pointers
    for (int i = ml.baseGroupIndex; i < ml.groups.length; i++) {
      Group g = ml.groups[i];
      if (g.firstAtomIndex >= ml.baseAtomIndex) {
        g.firstAtomIndex = mapOldToNew[g.firstAtomIndex];
        g.lastAtomIndex = mapOldToNew[g.lastAtomIndex];
        if (g.leadAtomIndex >= 0)
          g.leadAtomIndex = mapOldToNew[g.leadAtomIndex];
      }
    }
    // adjust atom arrays
    ms.adjustAtomArrays(mapNewToOld, ml.baseAtomIndex, n);
    ms.calcBoundBoxDimensions(null, 1);
    ms.resetMolecules();
    ms.validateBspf(false);
    bsAddedMask = BSUtil.deleteBits(bsAddedMask, bsDeletedAtoms);
    //System.out.println("res bsAddedMask = " + bsAddedMask);
    for (int i = ml.baseModelIndex; i < ms.mc; i++) { 
      fixAnnotations(i, "domains", T.domains);
      fixAnnotations(i, "validation", T.validation);
    }
  }

  private void fixAnnotations(int i, String name, int type) {
    Object o = ml.ms.getInfo(i, name);
    if (o != null) {
      Object dbObj = ((BioModel) ms.am[i]).getCachedAnnotationMap(name, o);
      if (dbObj != null)
        vwr.getAnnotationParser(false).fixAtoms(i, (SV) dbObj, bsAddedMask, type, 20);
    }
  }

  private void finalizePdbCharges() {
    Atom[] atoms = ms.at;
    // fix terminal N groups as +1
    for (int i = bsAtomsForHs.nextSetBit(0); i >= 0; i = bsAtomsForHs.nextSetBit(i + 1)) {
      Atom a = atoms[i];
      if (a.group.getNitrogenAtom() == a && a.getCovalentBondCount() == 1)
        a.setFormalCharge(1);
      if ((i = bsAtomsForHs.nextClearBit(i + 1)) < 0)
        break;
    }
  }
  
  private void finalizePdbMultipleBonds() {
    Map<String, Boolean> htKeysUsed = new Hashtable<String, Boolean>();
    int bondCount = ms.bondCount;
    Bond[] bonds = ms.bo;
    for (int i = baseBondIndex; i < bondCount; i++) {
      Atom a1 = bonds[i].atom1;
      Atom a2 = bonds[i].atom2;
      Group g = a1.group;
      if (g != a2.group)
        continue;
      SB key = new SB().append(g.getGroup3());
      key.append(":");
      String n1 = a1.getAtomName();
      String n2 = a2.getAtomName();
      if (n1.compareTo(n2) > 0)
        key.append(n2).append(":").append(n1);
      else
        key.append(n1).append(":").append(n2);
      String skey = key.toString();
      String type = htBondMap.get(skey);
      if (type == null)
        continue;
      htKeysUsed.put(skey, Boolean.TRUE);
      bonds[i].setOrder(PT.parseInt(type));
    }

    for (String key : htBondMap.keySet()) {
      if (htKeysUsed.get(key) != null)
        continue;
      if (key.indexOf(":") < 0) {
        htKeysUsed.put(key, Boolean.TRUE);
        continue;
      }
      String value = htBondMap.get(key);
      Logger.info("bond " + key + " was not used; order=" + value);
      if (htBondMap.get(key).equals("1")) {
        htKeysUsed.put(key, Boolean.TRUE);
        continue; // that's ok
      }
    }
    Map<String, String> htKeysBad = new Hashtable<String, String>();
    for (String key : htBondMap.keySet()) {
      if (htKeysUsed.get(key) != null)
        continue;
      htKeysBad.put(key.substring(0, key.lastIndexOf(":")), htBondMap.get(key));
    }
    if (htKeysBad.isEmpty())
      return;
    for (int i = 0; i < bondCount; i++) {
      Atom a1 = bonds[i].atom1;
      Atom a2 = bonds[i].atom2;
      if (a1.group == a2.group)
        continue;
      String value;
      if ((value = htKeysBad.get(a1.getGroup3(false) + ":" + a1.getAtomName())) == null
          && ((value = htKeysBad.get(a2.getGroup3(false) + ":" + a2.getAtomName())) == null))
        continue;
      bonds[i].setOrder(PT.parseInt(value));
      Logger.info("assigning order " + bonds[i].order + " to bond " + bonds[i]);
    }
  }

  private void setHydrogen(int iTo, int iAtom, String name, P3 pt) {
    if (!bsAddedHydrogens.get(iAtom))
      return;
    Atom[] atoms = ms.at;
    if (lastSetH == Integer.MIN_VALUE || atoms[iAtom].mi != atoms[lastSetH].mi) 
      maxSerial = ((int[]) ms.getInfo(atoms[lastSetH = iAtom].mi, "PDB_CONECT_firstAtom_count_max"))[2];
    bsAddedHydrogens.clear(iAtom);
    ms.setAtomName(iAtom, name, false);
    atoms[iAtom].setT(pt);
    ms.setAtomNumber(iAtom, ++maxSerial, false);
    atoms[iAtom].atomSymmetry = atoms[iTo].atomSymmetry;
    ml.undeleteAtom(iAtom);

    ms.bondAtoms(atoms[iTo], atoms[iAtom], Edge.BOND_COVALENT_SINGLE, 
        ms.getDefaultMadFromOrder(Edge.BOND_COVALENT_SINGLE), null, 0, true, false);
  }

  public Object fixPropertyValue(BS bsAtoms, Object data, boolean toHydrogens) {
    Atom[] atoms = ms.at;
    // we aren't doing this anymore
    // it was for TLS groups
//    if (data instanceof String) {
//      String[] sData = PT.split((String) data, "\n");
//      String[] newData = new String[bsAtoms.cardinality()];
//      String lastData = "";
//      for (int pt = 0, iAtom = 0, i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms
//          .nextSetBit(i + 1), iAtom++) {
//        if (atoms[i].getElementNumber() == 1) {
//          if (!toHydrogens)
//            continue;
//        } else {
//          lastData = sData[pt++];
//        }
//        newData[iAtom] = lastData;
//      }
//      return PT.join(newData, '\n', 0);
//    }
    // already float data
    float[] fData = (float[]) data;
    float[] newData = new float[bsAtoms.cardinality()];
    float lastData = 0;
    for (int pt = 0, iAtom = 0, i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms
        .nextSetBit(i + 1), iAtom++) {
      if (atoms[i].getElementNumber() == 1) {
        if (!toHydrogens)
          continue;
      } else {
        lastData = fData[pt++];
      }
      newData[iAtom] = lastData;
    }
    return newData;
  }

  static BioPolymer allocateBioPolymer(Group[] groups, int firstGroupIndex,
                                       boolean checkConnections, int pt0) {
    Monomer previous = null;
    int count = 0;
    for (int i = firstGroupIndex; i < groups.length; ++i) {
      Group group = groups[i];
      Monomer current;
      if (!(group instanceof Monomer)
          || (current = (Monomer) group).bioPolymer != null || previous != null
          && previous.getClass() != current.getClass() || checkConnections
          && !current.isConnectedAfter(previous))
        break;
      previous = current;
      count++;
    }
    if (count < 2)
      return null;
    Monomer[] monomers = new Monomer[count];
    for (int j = 0; j < count; ++j)
      monomers[j] = (Monomer) groups[firstGroupIndex + j];
    if (previous instanceof AminoMonomer)
      return new AminoPolymer(monomers, pt0);
    if (previous instanceof AlphaMonomer)
      return new AlphaPolymer(monomers, pt0);
    if (previous instanceof NucleicMonomer)
      return new NucleicPolymer(monomers);
    if (previous instanceof PhosphorusMonomer)
      return new PhosphorusPolymer(monomers);
    if (previous instanceof CarbohydrateMonomer)
      return new CarbohydratePolymer(monomers);
    Logger
        .error("Polymer.allocatePolymer() ... no matching polymer for monomor "
            + previous);
    throw new NullPointerException();
  }
  
  private BS bsAssigned;

  /**
   * Pull in all spans of helix, etc. in the file(s)
   * 
   * We do turn first, because sometimes a group is defined twice, and this way
   * it gets marked as helix or sheet if it is both one of those and turn.
   * 
   * Jmol 14.3 - adds sequence ANNOTATION
   * 
   * @param adapter
   * @param atomSetCollection
   */
  public void iterateOverAllNewStructures(JmolAdapter adapter,
                                          Object atomSetCollection) {
    JmolAdapterStructureIterator iterStructure = adapter
        .getStructureIterator(atomSetCollection);
    if (iterStructure == null)
      return;
    BS bs = iterStructure.getStructuredModels();
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        ml.structuresDefinedInFile.set(ml.baseModelIndex + i);
    while (iterStructure.hasNext())
      if (iterStructure.getStructureType() != STR.TURN)
        setStructure(iterStructure);

    // define turns LAST. (pulled by the iterator first)
    // so that if they overlap they get overwritten:

    iterStructure = adapter.getStructureIterator(atomSetCollection);
    while (iterStructure.hasNext())
      if (iterStructure.getStructureType() == STR.TURN)
        setStructure(iterStructure);
  }

  private static STR[] types = { STR.HELIXPI, STR.HELIXALPHA,
    STR.SHEET, STR.HELIX310, STR.TURN };

  private static int[] mytypes = {0, 2, 3, 4, 6};

  /**
   * note that istart and iend will be adjusted.
   * 
   * @param iterStructure
   */
  private void setStructure(JmolAdapterStructureIterator iterStructure) {
    STR t = iterStructure.getSubstructureType();
    String id = iterStructure.getStructureID();
    int serID = iterStructure.getSerialID();
    int count = iterStructure.getStrandCount();
    int[] atomRange = iterStructure.getAtomIndices();
    int[] modelRange = iterStructure.getModelIndices();
    BS[] bsAll = iterStructure.getBSAll();
    int m0, m1;
    Model[] models = ms.am;
    if (ml.isTrajectory) { //from PDB file
      m0 = m1 = modelRange[0];
    } else {
      m0 = modelRange[0] + ml.baseModelIndex;
      m1 = modelRange[1] + ml.baseModelIndex;
    }
    ml.structuresDefinedInFile.setBits(m0, m1 + 1);

    BS bs;
    Model m;
    if (bsAll != null) {
      for (int i = m0, t0; i <= m1; i++)
        if ((m = models[i]) instanceof BioModel)
          for (int j = 0; j < 5; j++)
            if ((bs = bsAll[t0 = mytypes[j]]) != null && !bs.isEmpty())
              ((BioModel) m).addStructureByBS(0, t0, types[j], bs);
      return;
    }

    int startChainID = iterStructure.getStartChainID();
    int startSequenceNumber = iterStructure.getStartSequenceNumber();
    char startInsertionCode = iterStructure.getStartInsertionCode();
    int endSequenceNumber = iterStructure.getEndSequenceNumber();
    int endChainID = iterStructure.getEndChainID();
    char endInsertionCode = iterStructure.getEndInsertionCode();
    STR type = (t == STR.NOT ? STR.NONE : t);
    int startSeqCode = Group.getSeqcodeFor(startSequenceNumber,
        startInsertionCode);
    int endSeqCode = Group.getSeqcodeFor(endSequenceNumber, endInsertionCode);
    if (bsAssigned == null)
      bsAssigned = new BS();
    for (int i = m0, i0 = 0; i <= m1; i++)
      if ((m = models[i]) instanceof BioModel)
        ((BioModel) m).addSecondaryStructure(type, id, serID, count,
            startChainID, startSeqCode, endChainID, endSeqCode,
            (i0 = m.firstAtomIndex) + atomRange[0], i0 + atomRange[1],
            bsAssigned);
  }

  public void setGroupLists(int ipt) {
    ml.group3Lists[ipt + 1] = Group.standardGroupList;
    ml.group3Counts[ipt + 1] = new int[group3Count + 10];
    if (ml.group3Lists[0] == null) {
      ml.group3Lists[0] = Group.standardGroupList;
      ml.group3Counts[0] = new int[group3Count + 10];
    }
  }

  /**
   * @param g3
   * @param max max ID (e.g. 20); can be Integer.MAX_VALUE to allow carbohydrate
   * @return true if found
   */
  public boolean isKnownPDBGroup(String g3, int max) {
    int pt = knownGroupID(g3);
    return (pt > 0 ? pt < max : max == Integer.MAX_VALUE && checkCarbohydrate(g3));
  }

  public byte lookupSpecialAtomID(String name) {
    if (htSpecialAtoms == null) {
      htSpecialAtoms = new Hashtable<String, Byte>();
      for (int i = specialAtomNames.length; --i >= 0;) {
        String specialAtomName = specialAtomNames[i];
        if (specialAtomName != null)
          htSpecialAtoms.put(specialAtomName, Byte.valueOf((byte) i));
      }
    }
    Byte boxedAtomID = htSpecialAtoms.get(name);
    return (boxedAtomID == null ? 0 : boxedAtomID.byteValue());
  }

  private static Map<String, String[][]> htPdbBondInfo;

  private String[][] getPdbBondInfo(String group3, boolean isLegacy) {
    if (htPdbBondInfo == null)
      htPdbBondInfo = new Hashtable<String, String[][]>();
    String[][] info = htPdbBondInfo.get(group3);
    if (info != null)
      return info;
    int pt = knownGroupID(group3);
    if (pt < 0 || pt > pdbBondInfo.length)
      return null;
    String s = pdbBondInfo[pt];
    // unfortunately, this change is not backward compatible.
    if (isLegacy && (pt = s.indexOf("O3'")) >= 0)
      s = s.substring(0, pt);
    String[] temp = PT.getTokens(s);
    info = new String[temp.length / 2][];
    for (int i = 0, p = 0; i < info.length; i++) {
      String source = temp[p++];
      String target = temp[p++];
      // a few shortcuts here:
      if (target.length() == 1)
        switch (target.charAt(0)) {
        case 'N':
          target = "H@H2";
          break;
        case 'B': // CB
          target = "HB3@HB2";
          break;
        case 'D': // CD
          target = "HD3@HD2";
          break;
        case 'G': // CG
          target = "HG3@HG2";
          break;
        case '2': // C2'
          target = "H2'@H2''";
          break;
        case '5': // C5'
          target = "H5''@H5'";
          break;
        }
      if (target.charAt(0) != 'H' && source.compareTo(target) > 0) {
        s = target;
        target = source;
        source = s;
      }
      info[i] = new String[] { source, target,
          (target.startsWith("H") ? "1" : "2") };
    }
    htPdbBondInfo.put(group3, info);
    return info;
  }
  /**
   * pdbBondInfo describes in a compact way what the hydrogen atom
   * names are for each standard amino acid. This list consists
   * of pairs of attached atom/hydrogen atom names, with abbreviations
   * N, C, O, B, D, G, 1, and 2 (for N, C, O, CB, CD, CG, C1', and C2', respectively)
   * given in pdbHAttachments, above. Note that we never add HXT or NH3
   * "?" here is for methyl groups with H1, H2, H3.
   * "@" indicates a prochiral center, with the assignment order given here
   * 
   */
  public final static String[] pdbBondInfo = {
    // added O3' HO3' O5' HO5' for nucleic and added 1 H atom for res 1 for 13.1.17
    // this could throw off states from previous versions
    // CH2 and NH2 labeling revised 2015.02.07
    
    "",
    /*ALA*/ "N N CA HA C O CB HB?",
    /*ARG*/ "N N CA HA C O CB B CG G CD D NE HE CZ NH1 NH1 HH11@HH12 NH2 HH22@HH21", 
    /*ASN*/ "N N CA HA C O CB B CG OD1 ND2 HD21@HD22", 
    /*ASP*/ "N N CA HA C O CB B CG OD1", 
    /*CYS*/ "N N CA HA C O CB B SG HG", 
    /*GLN*/ "N N CA HA C O CB B CG G CD OE1 NE2 HE22@HE21", 
    /*GLU*/ "N N CA HA C O CB B CG G CD OE1", 
    /*GLY*/ "N N CA HA2@HA3 C O", 
    /*HIS*/ "N N CA HA C O CB B CG CD2 ND1 CE1 ND1 HD1 CD2 HD2 CE1 HE1 NE2 HE2", 
    /*ILE*/ "N N CA HA C O CB HB CG1 HG13@HG12 CG2 HG2? CD1 HD1?", 
    /*LEU*/ "N N CA HA C O CB B CG HG CD1 HD1? CD2 HD2?", 
    /*LYS*/ "N N CA HA C O CB B CG G CD HD2@HD3 CE HE3@HE2 NZ HZ?", 
    /*MET*/ "N N CA HA C O CB B CG G CE HE?", 
    /*PHE*/ "N N CA HA C O CB B CG CD1 CD1 HD1 CD2 CE2 CD2 HD2 CE1 CZ CE1 HE1 CE2 HE2 CZ HZ", 
    /*PRO*/ "N H CA HA C O CB B CG G CD D", 
    /*SER*/ "N N CA HA C O CB B OG HG", 
    /*THR*/ "N N CA HA C O CB HB OG1 HG1 CG2 HG2?", 
    /*TRP*/ "N N CA HA C O CB B CG CD1 CD1 HD1 CD2 CE2 NE1 HE1 CE3 CZ3 CE3 HE3 CZ2 CH2 CZ2 HZ2 CZ3 HZ3 CH2 HH2", 
    /*TYR*/ "N N CA HA C O CB B CG CD1 CD1 HD1 CD2 CE2 CD2 HD2 CE1 CZ CE1 HE1 CE2 HE2 OH HH", 
    /*VAL*/ "N N CA HA C O CB HB CG1 HG1? CG2 HG2?",
    /*ASX*/ "N N CA HA C O CB B",
    /*GLX*/ "N N CA HA C O CB B CG G", 
    /*UNK*/ "",
    /*G*/ "P OP1 C5' 5 C4' H4' C3' H3' C2' H2' O2' HO2' C1' H1' C8 N7 C8 H8 C5 C4 C6 O6 N1 H1 C2 N3 N2 H22@H21 O3' HO3' O5' HO5'", 
    /*C*/ "P OP1 C5' 5 C4' H4' C3' H3' C2' H2' O2' HO2' C1' H1' C2 O2 N3 C4 N4 H41@H42 C5 C6 C5 H5 C6 H6 O3' HO3' O5' HO5'", 
    /*A*/ "P OP1 C5' 5 C4' H4' C3' H3' C2' H2' O2' HO2' C1' H1' C8 N7 C8 H8 C5 C4 C6 N1 N6 H61@H62 C2 N3 C2 H2 O3' HO3' O5' HO5'",
    /*T*/ "P OP1 C5' 5 C4' H4' C3' H3' C2' 2 C1' H1' C2 O2 N3 H3 C4 O4 C5 C6 C7 H7? C6 H6 O3' HO3' O5' HO5'",
    /*U*/ "P OP1 C5' 5 C4' H4' C3' H3' C2' H2' O2' HO2' C1' H1' C2 O2 N3 H3 C4 O4 C5 C6 C5 H5 C6 H6 O3' HO3' O5' HO5'", 
    /*I*/ "P OP1 C5' 5 C4' H4' C3' H3' C2' H2' O2' HO2' C1' H1' C8 N7 C8 H8 C5 C4 C6 O6 N1 H1 C2 N3 C2 H2 O3' HO3' O5' HO5'",
    /*DG*/ "P OP1 C5' 5 C4' H4' C3' H3' C2' 2 C1' H1' C8 N7 C8 H8 C5 C4 C6 O6 N1 H1 C2 N3 N2 H22@H21 O3' HO3' O5' HO5'", 
    /*DC*/ "P OP1 C5' 5 C4' H4' C3' H3' C2' 2 C1' H1' C2 O2 N3 C4 N4 H41@H42 C5 C6 C5 H5 C6 H6 O3' HO3' O5' HO5'", 
    /*DA*/ "P OP1 C5' 5 C4' H4' C3' H3' C2' 2 C1' H1' C8 N7 C8 H8 C5 C4 C6 N1 N6 H61@H62 C2 N3 C2 H2 O3' HO3' O5' HO5'", 
    /*DT*/ "P OP1 C5' 5 C4' H4' C3' H3' C2' 2 C1' H1' C2 O2 N3 H3 C4 O4 C5 C6 C7 H7? C6 H6 O3' HO3' O5' HO5'",
    /*DU*/ "P OP1 C5' 5 C4' H4' C3' H3' C2' 2 C1' H1' C2 O2 N3 H3 C4 O4 C5 C6 C5 H5 C6 H6 O3' HO3' O5' HO5'",  
    /*DI*/ "P OP1 C5' 5 C4' H4' C3' H3' C2' 2 C1' H1' C8 N7 C8 H8 C5 C4 C6 O6 N1 H1 C2 N3 C2 H2 O3' HO3' O5' HO5'",  
      };
  private final static int[] pdbHydrogenCount = {
            0,
    /*ALA*/ 6,
    /*ARG*/ 16,
    /*ASN*/ 7,
    /*ASP*/ 6,
    /*CYS*/ 6,
    /*GLN*/ 9,
    /*GLU*/ 8,
    /*GLY*/ 4,
    /*HIS*/ 9,
    /*ILE*/ 12,
    /*LEU*/ 12,
    /*LYS*/ 14,
    /*MET*/ 10,
    /*PHE*/ 10,
    /*PRO*/ 8,
    /*SER*/ 6,
    /*THR*/ 8,
    /*TRP*/ 11,
    /*TYR*/ 10,
    /*VAL*/ 10,  
    /*ASX*/ 3,
    /*GLX*/ 5,
    /*UNK*/ 0,
    /*G*/ 13,
    /*C*/ 13,
    /*A*/ 13,
    /*T*/ -1,
    /*U*/ 12,
    /*I*/ 12,
    /*DG*/ 13,
    /*DC*/ 13,
    /*DA*/ 13,
    /*DT*/ 14,
    /*DU*/ 12,
    /*DI*/ 12,
  };
  
  /**
   *  this form is used for counting groups in ModelSet
   *  
   *  GLX added for 13.1.16
   *
   */
  private final static String allCarbohydrates = 
    ",[AHR],[ALL],[AMU],[ARA],[ARB],[BDF],[BDR],[BGC],[BMA]" +
    ",[FCA],[FCB],[FRU],[FUC],[FUL],[GAL],[GLA],[GLC],[GXL]" +
    ",[GUP],[LXC],[MAN],[RAM],[RIB],[RIP],[XYP],[XYS]" +
    ",[CBI],[CT3],[CTR],[CTT],[LAT],[MAB],[MAL],[MLR],[MTT]" +
    ",[SUC],[TRE],[GCU],[MTL],[NAG],[NDG],[RHA],[SOR],[SOL],[SOE]" +  
    ",[XYL],[A2G],[LBT],[NGA],[SIA],[SLB]" + 
    ",[AFL],[AGC],[GLB],[NAN],[RAA]"; //these 4 are deprecated in PDB

  // from Eric Martz; revision by Angel Herraez
  public static short knownGroupID(String group3) {
    if (group3 == null || group3.length() == 0)
      return 0;
    Short boxedGroupID = htGroup.get(group3);
    return (boxedGroupID == null ? -1 : boxedGroupID.shortValue());
  }
  /**
   * @param group3 a potential group3 name
   * @return whether this is a carbohydrate from the list
   */
  private final static boolean checkCarbohydrate(String group3) {
    return (group3 != null 
        && allCarbohydrates.indexOf("[" + group3.toUpperCase() + "]") >= 0);
  }
  private static int group3Count;
  final static char[] predefinedGroup1Names = {
  /* rmh
   * 
   * G   Glycine   Gly                   P   Proline   Pro
   * A   Alanine   Ala                   V   Valine    Val
   * L   Leucine   Leu                   I   Isoleucine    Ile
   * M   Methionine    Met               C   Cysteine    Cys
   * F   Phenylalanine   Phe             Y   Tyrosine    Tyr
   * W   Tryptophan    Trp               H   Histidine   His
   * K   Lysine    Lys                   R   Arginine    Arg
   * Q   Glutamine   Gln                 N   Asparagine    Asn
   * E   Glutamic Acid   Glu             D   Aspartic Acid   Asp
   * S   Serine    Ser                   T   Threonine   Thr
   */
  '\0', //  0 this is the null group
  
  'A', // 1
  'R',
  'N',
  'D',
  'C', // 5 Cysteine
  'Q',
  'E',
  'G',
  'H',
  'I',
  'L',
  'K',
  'M',
  'F',
  'P', // 15 Proline
  'S',
  'T',
  'W',
  'Y',
  'V',
  'A', // 21 ASP/ASN ambiguous
  'G', // 22 GLU/GLN ambiguous
  '?', // 23 unknown -- 23
  
  'G', // X nucleics
  'C',
  'A',
  'T',
  'U',
  'I',
  
  'G', // DX nucleics
  'C',
  'A',
  'T',
  'U',
  'I',
  
  'G', // +X nucleics
  'C',
  'A',
  'T',
  'U',
  'I',
  };


  /**
   * MMCif, Gromacs, MdTop, Mol2 readers only
   * @param group3 
   * @return true if an identified hetero group
   * 
   */
  public boolean isHetero(String group3) {
    switch (group3.length()) {
    case 1:
      group3 += "  ";
      break;
    case 2:
      group3 += " ";
      break;
    case 3:
      break;
    default:
      return true;
    }
    int pt = Group.standardGroupList.indexOf(group3);
    return (pt < 0 || pt / 6 + 1 >= JC.GROUPID_WATER);
  }
  
  public static short group3NameCount;
  private final static String[] predefinedGroup3Names = {
    // taken from PDB spec
    "   ", //  0 this is the null group
    "ALA", // 1
    "ARG", // 2 arginine -- hbond donor
    "ASN", // 3 asparagine -- hbond donor
    "ASP", // 4 aspartate -- hbond acceptor
    "CYS",
    "GLN", // 6 glutamine -- hbond donor
    "GLU", // 7 glutamate -- hbond acceptor
    "GLY",
    "HIS", // 9 histidine -- hbond ambiguous
    "ILE",
    "LEU",
    "LYS", // 12 lysine -- hbond donor
    "MET",
    "PHE",
    "PRO", // 15 proline -- no NH
    "SER",
    "THR",
    "TRP",
    "TYR", // 19 tryptophan -- hbond donor
    "VAL",
    "ASX", // 21 ASP/ASN ambiguous
    "GLX", // 22 GLU/GLN ambiguous
    "UNK", // 23 unknown -- 23
  
    // if you change these numbers you *must* update
    // the predefined sets below
  
    // with the deprecation of +X, we will need a new
    // way to handle these. 
    
    "G  ", // 24 starts nucleics //0 
    "C  ", 
    "A  ",
    "T  ", 
    "U  ", 
    "I  ", // 29 / 5
    
    "DG ", // 30 / 6
    "DC ",
    "DA ",
    "DT ",
    "DU ",
    "DI ", // 35 / 11
    
    "+G ", // 36 / 12
    "+C ",
    "+A ",
    "+T ",
    "+U ",
    "+I ", // 41 / 17
    /* removed bh 7/1/2011 this is isolated inosine, not a polymer "NOS", // inosine */
    
    // solvent types: -- if these numbers change, also change GROUPID_WATER,_SOLVENT,and_SULFATE
    
    "HOH", // 42 water
    "DOD", // 43
    "WAT", // 44
    "UREA",// 45 urea, a cosolvent
    "PO4", // 46 phosphate ions  -- from here on is "ligand"
    "SO4", // 47 sulphate ions
    "UNL", // 48 unknown ligand
    
  };
  
  /*
   * Convert "AVG" to "ALA VAL GLY"; unknowns to UNK
   * 
   */
  public String toStdAmino3(String g1) {
    if (g1.length() == 0)
      return "";
    SB s = new SB();
    int pt = knownGroupID("==A");
    if (pt < 0) {
      // just the amino acids
      for (int i = 1; i <= 20; i++) {
        pt = knownGroupID(predefinedGroup3Names[i]);
        htGroup.put("==" + predefinedGroup1Names[i], Short.valueOf((short) pt));
      }
    }
    for (int i = 0, n = g1.length(); i < n; i++) {
      char ch = g1.charAt(i);
      pt = knownGroupID("==" + ch);
      if (pt < 0)
        pt = 23;
      s.append(" ").append(predefinedGroup3Names[pt]);
    }
    return s.toString().substring(1);
  }
  
  public short getGroupID(String g3) {
    return getGroupIdFor(g3);
  }

  static short getGroupIdFor(String group3) {
    if (group3 != null)
      group3 = group3.trim();
    short groupID = knownGroupID(group3);
    return (groupID == -1 ? addGroup3Name(group3) : groupID);
  }

  /**
   * These can overrun 3 characters; that is not significant.
   * 
   * @param group3
   * @return  a short group ID
   */
  private synchronized static short addGroup3Name(String group3) {
    if (group3NameCount == Group.group3Names.length)
      Group.group3Names = AU.doubleLengthS(Group.group3Names);
    short groupID = group3NameCount++;
    Group.group3Names[groupID] = group3;
    htGroup.put(group3, Short.valueOf(groupID));
    return groupID;
  }

  private static int getStandardPdbHydrogenCount(String group3) {
    int pt = knownGroupID(group3);
    return (pt < 0 || pt >= pdbHydrogenCount.length ? -1 : pdbHydrogenCount[pt]);
  }
  ////////////////////////////////////////////////////////////////
  // static stuff for group ids
  ////////////////////////////////////////////////////////////////

  private final static String[] specialAtomNames = {
    
    ////////////////////////////////////////////////////////////////
    // The ordering of these entries can be changed ... BUT ...
    // the offsets must be kept consistent with the ATOMID definitions
    // below.
    //
    // Used in Atom to look up special atoms. Any "*" in a PDB entry is
    // changed to ' for comparison here
    // 
    // null is entry 0
    // The first 32 entries are reserved for null + 31 'distinguishing atoms'
    // see definitions below. 32 is magical because bits are used in an
    // int to distinguish groups. If we need more then we can go to 64
    // bits by using a long ... but code must change. See Resolver.java
    //
    // All entries from 64 on are backbone entries
    ////////////////////////////////////////////////////////////////
    null, // 0
  
    // protein backbone
    //
    "N",   //  1 - amino nitrogen        SPINE
    "CA",  //  2 - alpha carbon          SPINE
    "C",   //  3 - carbonyl carbon       SPINE
    "O",   //  4 - carbonyl oxygen
    "O1",  //  5 - carbonyl oxygen in some protein residues (4THN)
  
    // nucleic acid backbone sugar
    //
    "O5'", //  6 - sugar 5' oxygen       SPINE
    "C5'", //  7 - sugar 5' carbon       SPINE
    "C4'", //  8 - sugar ring 4' carbon  SPINE
    "C3'", //  9 - sugar ring 3' carbon  SPINE
    "O3'", // 10 - sugar 3' oxygen       SPINE
    "C2'", // 11 - sugar ring 2' carbon
    "C1'", // 12 - sugar ring 1' carbon
    // Phosphorus is not required for a nucleic group because
    // at the terminus it could have H5T or O5T ...
    "P",   // 13 - phosphate phosphorus  SPINE
  
    // END OF FIRST BACKBONE SET
    
    // ... But we need to distinguish phosphorus separately because
    // it could be found in phosphorus-only nucleic polymers
  
    "OD1",   // 14  ASP/ASN carbonyl/carbonate
    "OD2",   // 15  ASP carbonyl/carbonate
    "OE1",   // 16  GLU/GLN carbonyl/carbonate
    "OE2",   // 17  GLU carbonyl/carbonate
    "SG",    // 18  CYS sulfur
    // reserved for future expansion ... lipids & carbohydrates
    // 9/2006 -- carbohydrates are just handled as group3 codes
    // see below
    null, // 18 - 19
    null, null, null, null, // 20 - 23
    null, null, null, null, // 24 - 27
    null, null, null, null, // 28 - 31
  
    // nucleic acid bases
    //
    "N1",   // 32
    "C2",   // 33
    "N3",   // 34
    "C4",   // 35
    "C5",   // 36
    "C6",   // 37 -- currently defined as the nucleotide wing
            // this determines the vector for the sheet
            // could be changed if necessary
  
    // pyrimidine O2
    //
    "O2",   // 38
  
    // purine stuff
    //
    "N7",   // 39
    "C8",   // 40
    "N9",   // 41
    
    // nucleic acid base ring functional groups
    // DO NOT CHANGE THESE NUMBERS WITHOUT ALSO CHANGING
    // NUMBERS IN THE PREDEFINED SETS _a=...
    
    "N4",  // 42 - base ring N4, unique to C
    "N2",  // 43 - base amino N2, unique to G
    "N6",  // 44 - base amino N6, unique to A
    "C5M", // 45 - base methyl carbon, unique to T
  
    "O6",  // 46 - base carbonyl O6, only in G and I
    "O4",  // 47 - base carbonyl O4, only in T and U
    "S4",  // 48 - base thiol sulfur, unique to thio-U
  
    "C7", // 49 - base methyl carbon, unique to DT
  
    "H1",  // 50  - NOT backbone
    "H2",  // 51 - NOT backbone -- see 1jve
    "H3",  // 52 - NOT backbone
    null, null, //53
    null, null, null, null, null, //55
    null, null, null, null,       //60 - 63
    
    // everything from here on is backbone
  
    // protein backbone
    //
    "OXT", // 64 - second carbonyl oxygen, C-terminus only
  
    // protein backbone hydrogens
    //
    "H",   // 65 - amino hydrogen
    // these appear on the N-terminus end of 1ALE & 1LCD
    "1H",  // 66 - N-terminus hydrogen
    "2H",  // 67 - second N-terminus hydrogen
    "3H",  // 68 - third N-terminus hydrogen
    "HA",  // 69 - H on alpha carbon
    "1HA", // 70 - H on alpha carbon in Gly only
    "2HA", // 71 - 1ALE calls the two GLY hdrogens 1HA & 2HA
  
    // Terminal nuclic acid
  
    "H5T", // 72 - 5' terminus hydrogen which replaces P + O1P + O2P
    "O5T", // 73 - 5' terminus oxygen which replaces P + O1P + O2P
    "O1P", // 74 - first equivalent oxygen on phosphorus of phosphate
    "OP1", // 75 - first equivalent oxygen on phosphorus of phosphate -- new designation
    "O2P", // 76 - second equivalent oxygen on phosphorus of phosphate    
    "OP2", // 77 - second equivalent oxygen on phosphorus of phosphate -- new designation
  
    "O4'", // 78 - sugar ring 4' oxygen ... not present in +T ... maybe others
    "O2'", // 79 - sugar 2' oxygen, unique to RNA
  
    // nucleic acid backbone hydrogens
    //
    "1H5'", // 80 - first  equivalent H on sugar 5' carbon
    "2H5'", // 81 - second  equivalent H on sugar 5' carbon 
    "H4'",  // 82 - H on sugar ring 4' carbon
    "H3'",  // 83 - H on sugar ring 3' carbon
    "1H2'", // 84 - first equivalent H on sugar ring 2' carbon
    "2H2'", // 85 - second equivalent H on sugar ring 2' carbon
    "2HO'", // 86 - H on sugar 2' oxygen, unique to RNA 
    "H1'",  // 87 - H on sugar ring 1' carbon 
    "H3T",  // 88 - 3' terminus hydrogen    
        
    // add as many as necessary -- backbone only
  
    "HO3'", // 89 - 3' terminus hydrogen (new)
    "HO5'", // 90 - 5' terminus hydrogen (new)
    "HA2",
    "HA3",
    "HA2", 
    "H5'", 
    "H5''",
    "H2'",
    "H2''",
    "HO2'",
  
    "O3P", // 99    - third equivalent oxygen on phosphorus of phosphate    
    "OP3", //100    - third equivalent oxygen on phosphorus of phosphate -- new designation
        
  };
  
  public final static int ATOMID_MAX = specialAtomNames.length;

  final static String getSpecialAtomName(int atomID) {
    return specialAtomNames[atomID];
  }

  private static Map<String, Byte> htSpecialAtoms;

  private final static int[] argbsAmino = {
    0xFFBEA06E, // default tan
    // note that these are the rasmol colors and names, not xwindows
    0xFFC8C8C8, // darkGrey   ALA
    0xFF145AFF, // blue       ARG
    0xFF00DCDC, // cyan       ASN
    0xFFE60A0A, // brightRed  ASP
    0xFFE6E600, // yellow     CYS
    0xFF00DCDC, // cyan       GLN
    0xFFE60A0A, // brightRed  GLU
    0xFFEBEBEB, // lightGrey  GLY
    0xFF8282D2, // paleBlue   HIS
    0xFF0F820F, // green      ILE
    0xFF0F820F, // green      LEU
    0xFF145AFF, // blue       LYS
    0xFFE6E600, // yellow     MET
    0xFF3232AA, // midBlue    PHE
    0xFFDC9682, // mauve      PRO
    0xFFFA9600, // orange     SER
    0xFFFA9600, // orange     THR
    0xFFB45AB4, // purple     TRP
    0xFF3232AA, // midBlue    TYR
    0xFF0F820F, // green      VAL

    0xFFFF69B4, // pick a new color ASP/ASN ambiguous
    0xFFFF69B4, // pick a new color GLU/GLN ambiguous
    0xFFBEA06E, // default tan UNK
  };

  private final static int[] argbsNucleic = {
    0xFFBEA06E, // default tan
    0xFFA0A0A0, // grey       P
    0xFF0F820F, // green      G
    0xFFE6E600, // yellow     C
    0xFFE60A0A, // brightRed  A
    0xFF145AFF, // blue       T
    0xFF00DCDC, // cyan       U
    0xFF00DCDC, // cyan       I
    
    0xFF0F820F, // green      DG
    0xFFE6E600, // yellow     DC
    0xFFE60A0A, // brightRed  DA
    0xFF145AFF, // blue       DT
    0xFF00DCDC, // cyan       DU
    0xFF00DCDC, // cyan       DI
    
    0xFF0F820F, // green      +G
    0xFFE6E600, // yellow     +C
    0xFFE60A0A, // brightRed  +A
    0xFF145AFF, // blue       +T
    0xFF00DCDC, // cyan       +U
    0xFF00DCDC, // cyan       +I
  };



  /**
   * colors used for chains
   *
   */

  /****************************************************************
   * some pastel colors
   * 
   * C0D0FF - pastel blue
   * B0FFB0 - pastel green
   * B0FFFF - pastel cyan
   * FFC0C8 - pink
   * FFC0FF - pastel magenta
   * FFFF80 - pastel yellow
   * FFDEAD - navajowhite
   * FFD070 - pastel gold

   * FF9898 - light coral
   * B4E444 - light yellow-green
   * C0C000 - light olive
   * FF8060 - light tomato
   * 00FF7F - springgreen
   * 
cpk on; select atomno>100; label %i; color chain; select selected & hetero; cpk off
   ****************************************************************/

  private final static int[] argbsChainAtom = {
    // ' '->0 'A'->1, 'B'->2
    0xFFffffff, // ' ' & '0' white
    //
    0xFFC0D0FF, // skyblue
    0xFFB0FFB0, // pastel green
    0xFFFFC0C8, // pink
    0xFFFFFF80, // pastel yellow
    0xFFFFC0FF, // pastel magenta
    0xFFB0F0F0, // pastel cyan
    0xFFFFD070, // pastel gold
    0xFFF08080, // lightcoral

    0xFFF5DEB3, // wheat
    0xFF00BFFF, // deepskyblue
    0xFFCD5C5C, // indianred
    0xFF66CDAA, // mediumaquamarine
    0xFF9ACD32, // yellowgreen
    0xFFEE82EE, // violet
    0xFF00CED1, // darkturquoise
    0xFF00FF7F, // springgreen
    0xFF3CB371, // mediumseagreen

    0xFF00008B, // darkblue
    0xFFBDB76B, // darkkhaki
    0xFF006400, // darkgreen
    0xFF800000, // maroon
    0xFF808000, // olive
    0xFF800080, // purple
    0xFF008080, // teal
    0xFFB8860B, // darkgoldenrod
    0xFFB22222, // firebrick
  };

  private final static int[] argbsChainHetero = {
    // ' '->0 'A'->1, 'B'->2
    0xFFffffff, // ' ' & '0' white
    //
    0xFFC0D0FF - 0x00303030, // skyblue
    0xFFB0FFB0 - 0x00303018, // pastel green
    0xFFFFC0C8 - 0x00303018, // pink
    0xFFFFFF80 - 0x00303010, // pastel yellow
    0xFFFFC0FF - 0x00303030, // pastel magenta
    0xFFB0F0F0 - 0x00303030, // pastel cyan
    0xFFFFD070 - 0x00303010, // pastel gold
    0xFFF08080 - 0x00303010, // lightcoral

    0xFFF5DEB3 - 0x00303030, // wheat
    0xFF00BFFF - 0x00001830, // deepskyblue
    0xFFCD5C5C - 0x00181010, // indianred
    0xFF66CDAA - 0x00101818, // mediumaquamarine
    0xFF9ACD32 - 0x00101808, // yellowgreen
    0xFFEE82EE - 0x00301030, // violet
    0xFF00CED1 - 0x00001830, // darkturquoise
    0xFF00FF7F - 0x00003010, // springgreen
    0xFF3CB371 - 0x00081810, // mediumseagreen

    0xFF00008B + 0x00000030, // darkblue
    0xFFBDB76B - 0x00181810, // darkkhaki
    0xFF006400 + 0x00003000, // darkgreen
    0xFF800000 + 0x00300000, // maroon
    0xFF808000 + 0x00303000, // olive
    0xFF800080 + 0x00300030, // purple
    0xFF008080 + 0x00003030, // teal
    0xFFB8860B + 0x00303008, // darkgoldenrod
    0xFFB22222 + 0x00101010, // firebrick
  };

  private final static int[] argbsShapely = {
    0xFFFF00FF, // default
    // these are rasmol values, not xwindows colors
    0xFF00007C, // ARG
    0xFFFF7C70, // ASN
    0xFF8CFF8C, // ALA
    0xFFA00042, // ASP
    0xFFFFFF70, // CYS
    0xFFFF4C4C, // GLN
    0xFF660000, // GLU
    0xFFFFFFFF, // GLY
    0xFF7070FF, // HIS
    0xFF004C00, // ILE
    0xFF455E45, // LEU
    0xFF4747B8, // LYS
    0xFF534C52, // PHE
    0xFFB8A042, // MET
    0xFF525252, // PRO
    0xFFFF7042, // SER
    0xFFB84C00, // THR
    0xFF4F4600, // TRP
    0xFF8C704C, // TYR
    0xFFFF8CFF, // VAL
  
    0xFFFF00FF, // ASX ASP/ASN ambiguous
    0xFFFF00FF, // GLX GLU/GLN ambiguous
    0xFFFF00FF, // UNK unknown -- 23
  
    0xFFFF7070, // G  
    0xFFFF8C4B, // C
    0xFFA0A0FF, // A
    0xFFA0FFA0, // T
    0xFFFF8080, // U miguel made up this color
    0xFF80FFFF, // I miguel made up this color
  
    0xFFFF7070, // DG
    0xFFFF8C4B, // DC
    0xFFA0A0FF, // DA
    0xFFA0FFA0, // DT
    0xFFFF8080, // DU
    0xFF80FFFF, // DI
    
    0xFFFF7070, // +G
    0xFFFF8C4B, // +C
    0xFFA0A0FF, // +A
    0xFFA0FFA0, // +T
    0xFFFF8080, // +U
    0xFF80FFFF, // +I
  
    // what to do about remediated +X names?
    // we will need a map
    
  };
    
  static {
    /**
     * @j2sNative
     * 
     */
    {
      if (argbsShapely.length != JC.GROUPID_WATER) {
        Logger.error("argbsShapely wrong length");
        throw new NullPointerException();
      }
      if (argbsAmino.length != JC.GROUPID_AMINO_MAX) {
        Logger.error("argbsAmino wrong length");
        throw new NullPointerException();
      }
      if (argbsChainHetero.length != argbsChainAtom.length) {
        Logger.error("argbsChainHetero wrong length");
        throw new NullPointerException();
      }
    }

  }

  public int[] getArgbs(int tok) {
    switch (tok) {
    case T.nucleic:
      return argbsNucleic;
    case T.amino:
      return argbsAmino;
    case T.shapely:
      return argbsShapely;
    case T.atoms:
      return argbsChainAtom;
    case T.hetero:
      return argbsChainHetero;
    }
    return null;
  }

  public BioModelSet getBioModelSet(ModelSet modelSet) {
    if (modelSet.bioModelset == null)
      modelSet.bioModelset = new BioModelSet().set(vwr, modelSet);
    return modelSet.bioModelset;
  }

}


