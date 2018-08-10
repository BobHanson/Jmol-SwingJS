/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2011-08-05 21:10:46 -0500 (Fri, 05 Aug 2011) $
 * $Revision: 15943 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.OC;
import javajs.util.SB;

import org.jmol.api.Interface;
import org.jmol.c.STR;
import org.jmol.dssx.DSSP;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.Group;
import org.jmol.modelset.HBond;
import org.jmol.modelset.LabelToken;
import org.jmol.modelset.Model;
import org.jmol.modelset.ModelSet;
import org.jmol.script.SV;
import org.jmol.util.Edge;
import org.jmol.util.Escape;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;


public final class BioModel extends Model {

  /*
   *   
   * Note that "monomer" extends group. A group only becomes a 
   * monomer if it can be identified as one of the following 
   * PDB/mmCIF types:
   * 
   *   amino  -- has an N, a C, and a CA
   *   alpha  -- has just a CA
   *   nucleic -- has C1',C2',C3',C4',C5',O3', and O5'
   *   phosphorus -- has P
   *   
   * The term "conformation" is a bit loose. It means "what you get
   * when you go with one or another set of alternative locations.
   *
   *  
   */
  
  private Viewer vwr;

  int bioPolymerCount = 0;
  public BioPolymer[] bioPolymers;
  boolean isMutated;

  String defaultStructure;
  
  BioModel(ModelSet modelSet, int modelIndex, int trajectoryBaseIndex, 
      String jmolData, Properties properties, Map<String, Object> auxiliaryInfo) {
    vwr = modelSet.vwr;
    set(modelSet, modelIndex, trajectoryBaseIndex, jmolData, properties, auxiliaryInfo);
    
    isBioModel = true;
    if (modelSet.bioModelset == null)
      modelSet.bioModelset = new BioModelSet().set(vwr, ms);
    clearBioPolymers();
    modelSet.am[modelIndex] = this;
    pdbID = (String) auxiliaryInfo.get("name");
  }

  int addBioPolymer(BioPolymer polymer) {
    if (bioPolymers.length == 0)
      clearBioPolymers();
    if (bioPolymerCount == bioPolymers.length)
      bioPolymers = (BioPolymer[])AU.doubleLength(bioPolymers);
    polymer.bioPolymerIndexInModel = bioPolymerCount;
    bioPolymers[bioPolymerCount++] = polymer;
    return polymer.monomerCount;
  }


  void addSecondaryStructure(STR type, String structureID,
                                    int serialID, int strandCount,
                                    int startChainID, int startSeqcode,
                                    int endChainID, int endSeqcode, int istart,
                                    int iend, BS bsAssigned) {
    for (int i = bioPolymerCount; --i >= 0;)
      if (bioPolymers[i] instanceof AlphaPolymer)
        ((AlphaPolymer) bioPolymers[i]).addStructure(type, structureID,
            serialID, strandCount, startChainID, startSeqcode, endChainID,
            endSeqcode, istart, iend, bsAssigned);
  }

  void addStructureByBS(int count, int dsspType, STR type, BS bs) {
    for (int i = bioPolymerCount; --i >= 0;) {
      BioPolymer b = bioPolymers[i];
      if (b instanceof AlphaPolymer)
        count = ((AlphaPolymer) bioPolymers[i]).setStructureBS(++count, dsspType, type, bs, true);
    }
  }

  private String calculateDssx(Lst<Bond> vHBonds, boolean doReport,
                               boolean dsspIgnoreHydrogen, boolean setStructure, int version) {
    boolean haveProt = false;
    boolean haveNucl = false;
    for (int i = 0; i < bioPolymerCount && !(haveProt && haveNucl); i++) {
      if (bioPolymers[i].isNucleic())
        haveNucl = true;
      else if (bioPolymers[i] instanceof AminoPolymer)
        haveProt = true;
    }
    String s = "";
    if (haveProt)
      s += ((DSSP) Interface.getOption("dssx.DSSP", vwr, "ms"))
        .calculateDssp(bioPolymers, bioPolymerCount, vHBonds, doReport,
            dsspIgnoreHydrogen, setStructure, version);
    if (haveNucl && auxiliaryInfo.containsKey("dssr") && vHBonds != null)
      s += vwr.getAnnotationParser(true).getHBonds(ms, modelIndex, vHBonds, doReport);
    return s;
  }

  String calculateStructures(boolean asDSSP, boolean doReport,
                                    boolean dsspIgnoreHydrogen,
                                    boolean setStructure, boolean includeAlpha, int version) {
    if (bioPolymerCount == 0 || !setStructure && !asDSSP)
      return "";
    ms.proteinStructureTainted = structureTainted = true;
    if (setStructure)
      for (int i = bioPolymerCount; --i >= 0;)
        if (!asDSSP || bioPolymers[i].monomers[0].getNitrogenAtom() != null)
          bioPolymers[i].clearStructures();
    if (!asDSSP || includeAlpha)
      for (int i = bioPolymerCount; --i >= 0;)
        if (bioPolymers[i] instanceof AlphaPolymer)
          ((AlphaPolymer) bioPolymers[i]).calculateStructures(includeAlpha);
    return (asDSSP ? calculateDssx(null, doReport, dsspIgnoreHydrogen, setStructure, version) : "");
  }

  
  void clearBioPolymers() {
    bioPolymers = new BioPolymer[8];
    bioPolymerCount = 0;
  }
  
  @Override
  public void fixIndices(int modelIndex, int nAtomsDeleted, BS bsDeleted) {
    fixIndicesM(modelIndex, nAtomsDeleted, bsDeleted);
    recalculateLeadMidpointsAndWingVectors();
  }
  
  @Override
  public boolean freeze() {
    freezeM();
    bioPolymers = (BioPolymer[])AU.arrayCopyObject(bioPolymers, bioPolymerCount);
    return true;
  }

  public Lst<BS> getBioBranches(Lst<BS> biobranches) {
    // scan through biopolymers quickly -- 
    BS bsBranch;
    for (int j = 0; j < bioPolymerCount; j++) {
      bsBranch = new BS();
      bioPolymers[j].getRange(bsBranch, isMutated);
      int iAtom = bsBranch.nextSetBit(0);
      if (iAtom >= 0) {
        if (biobranches == null)
          biobranches = new  Lst<BS>();
        biobranches.addLast(bsBranch);
      }
    }
    return biobranches;
  }
  
  public int getBioPolymerCount() {
    return bioPolymerCount;
  }

  Object getCachedAnnotationMap(String key, Object ann) {
    Map<String, Object> cache = (dssrCache == null && ann != null ? dssrCache = new Hashtable<String, Object>()
        : dssrCache);
    if (cache == null)
      return null;
    Object annotv = cache.get(key);
    if (annotv == null && ann != null) {
      annotv = (ann instanceof SV || ann instanceof Hashtable ? ann
              : vwr.parseJSONMap((String) ann));
      cache.put(key, annotv);
    }
    return (annotv instanceof SV || annotv instanceof Hashtable ? annotv : null);
  }

  /**
   * @param conformationIndex0
   * @param doSet 
   * @param bsAtoms
   * @param bsRet
   * @return true;
   */
  public boolean getConformation(int conformationIndex0, boolean doSet, BS bsAtoms, BS bsRet) {
    if (conformationIndex0 >= 0) {
      int nAltLocs = altLocCount;
      if (nAltLocs > 0) {
        Atom[] atoms = ms.at;
        Group g = null;
        char ch = '\0';
        int conformationIndex = conformationIndex0;
        BS bsFound = new BS();
        for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
            Atom atom = atoms[i];
            char altloc = atom.altloc;
            // ignore (include) atoms that have no designation
            if (altloc == '\0')
              continue;
            if (atom.group != g) {
              g = atom.group;
              ch = '\0';
              conformationIndex = conformationIndex0;
              bsFound.clearAll();
            }
            // count down until we get the desired index into the list
            if (conformationIndex >= 0 && altloc != ch && !bsFound.get(altloc)) {
              ch = altloc;
              conformationIndex--;
              bsFound.set(altloc);
            }
            if (conformationIndex >= 0 || altloc != ch)
              bsAtoms.clear(i);
          }
      }
    }
    if (bsAtoms.nextSetBit(0) >= 0) {
      bsRet.or(bsAtoms);      
      if (doSet)
        for (int j = bioPolymerCount; --j >= 0;)
          bioPolymers[j].setConformation(bsAtoms);
    }
    return true;
  }

  public void getDefaultLargePDBRendering(SB sb, int maxAtoms) {
    BS bs = new BS();
    if (getBondCount() == 0)
      bs = bsAtoms;
    // all biopolymer atoms...
    if (bs != bsAtoms)
      for (int i = 0; i < bioPolymerCount; i++)
        bioPolymers[i].getRange(bs, isMutated);
    if (bs.nextSetBit(0) < 0)
      return;
    // ...and not connected to backbone:
    BS bs2 = new BS();
    if (bs == bsAtoms) {
      bs2 = bs;
    } else {
      for (int i = 0; i < bioPolymerCount; i++)
        if (bioPolymers[i].getType() == BioPolymer.TYPE_NOBONDING)
          bioPolymers[i].getRange(bs2, isMutated);
    }
    if (bs2.nextSetBit(0) >= 0)
      sb.append("select ").append(Escape.eBS(bs2)).append(";backbone only;");
    if (act <= maxAtoms)
      return;
    // ...and it's a large model, to wireframe:
      sb.append("select ").append(Escape.eBS(bs)).append(" & connected; wireframe only;");
    // ... and all non-biopolymer and not connected to stars...
    if (bs != bsAtoms) {
      bs2.clearAll();
      bs2.or(bsAtoms);
      bs2.andNot(bs);
      if (bs2.nextSetBit(0) >= 0)
        sb.append("select " + Escape.eBS(bs2) + " & !connected;stars 0.5;spacefill off;");
    }
  }

  public String getFullPDBHeader() {
    if (modelIndex < 0)
      return "";
    String info = (String) auxiliaryInfo.get("fileHeader");
    if (info != null)
      return info;
    return ms.bioModelset.getBioExt().getFullPDBHeader(auxiliaryInfo);
  }

  public void getPdbData(String type, char ctype, boolean isDraw,
                         BS bsSelected, OC out,
                         LabelToken[] tokens, SB pdbCONECT, BS bsWritten) {
    ms.bioModelset.getBioExt().getPdbDataM(this, vwr, type, ctype, isDraw, bsSelected, out, tokens, pdbCONECT, bsWritten);
  }

  void getRasmolHydrogenBonds(BS bsA, BS bsB, Lst<Bond> vHBonds,
                                      boolean nucleicOnly, int nMax,
                                      boolean dsspIgnoreHydrogens, BS bsHBonds, int version) {
    boolean doAdd = (vHBonds == null);
    if (doAdd)
      vHBonds = new Lst<Bond>();
    if (nMax < 0)
      nMax = Integer.MAX_VALUE;
    boolean asDSSX = (bsB == null);
    BioPolymer bp, bp1;
    if (asDSSX && bioPolymerCount > 0) {
      calculateDssx(vHBonds, false, dsspIgnoreHydrogens, false, version);
    } else {
      for (int i = bioPolymerCount; --i >= 0;) {
        bp = bioPolymers[i];
        if (bp.monomerCount == 0)
          continue;
        int type = bp.getType();
        boolean isRNA = false;
        switch  (type) {
        case BioPolymer.TYPE_AMINO:
          if (nucleicOnly)
            continue;
          bp.calcRasmolHydrogenBonds(null, bsA, bsB, vHBonds, nMax, null, true,
              false);
          break;
        case BioPolymer.TYPE_NUCLEIC:
          isRNA = bp.monomers[0].isRna();
          break;
        default:
          continue;
        }
        for (int j = bioPolymerCount; --j >= 0;) {
          if ((bp1 = bioPolymers[j]) != null && (isRNA || i != j)
              && type == bp1.getType()) {
            bp1.calcRasmolHydrogenBonds(bp, bsA, bsB, vHBonds, nMax, null,
                true, false);
          }
        }
      }
    }

    if (vHBonds.size() == 0 || !doAdd)
      return;
    hasRasmolHBonds = true;
    for (int i = 0; i < vHBonds.size(); i++) {
      HBond bond = (HBond) vHBonds.get(i);
      Atom atom1 = bond.atom1;
      Atom atom2 = bond.atom2;
      if (atom1.isBonded(atom2))
        continue;
      int index = ms.addHBond(atom1, atom2, bond.order, bond.getEnergy());
      if (bsHBonds != null)
        bsHBonds.set(index);
    }
  }
  
  /**
   * Get a unitID. Note that we MUST go through the | after InsCode, because
   * if we do not do that we cannot match residues only using string matching.
   * 
   * @param atom
   * @param flags 
   * @return a unitID
   */
  public String getUnitID(Atom atom, int flags) {
    
    //  type: (ID)|model|chain|resid|resno|(atomName)|(altID)|(InsCode)|(symmetry)
    //  res:  ID|model|chain|resid|resno|atomName|altID|InsCode|symmetry  
    SB sb = new SB();
    Group m = atom.group;
    boolean noTrim =((flags & JC.UNITID_TRIM) != JC.UNITID_TRIM); 
    char ch = ((flags & JC.UNITID_INSCODE) == JC.UNITID_INSCODE ? m.getInsertionCode() : '\0');
    boolean isAll = (ch != '\0');
    if ((flags & JC.UNITID_MODEL) == JC.UNITID_MODEL && (pdbID != null))
      sb.append(pdbID);      
    sb.append("|").appendO(ms.getInfo(modelIndex, "modelNumber"))
      .append("|").append(vwr.getChainIDStr(m.chain.chainID))
      .append("|").append(m.getGroup3())
      .append("|").appendI(m.getResno());
    if ((flags & JC.UNITID_ATOM) == JC.UNITID_ATOM) {
      sb.append("|").append(atom.getAtomName());
      if (atom.altloc != '\0')
        sb.append("|").appendC(atom.altloc);
      else if (noTrim || isAll)
        sb.append("|");
    } else if (noTrim || isAll) {
      sb.append("||");
    }
    if (isAll)
      sb.append("|").appendC(ch);
    else if (noTrim)
      sb.append("|");
    if (noTrim)
      sb.append("|");
    return sb.toString();
  }

  void recalculateLeadMidpointsAndWingVectors() {
    for (int ip = 0; ip < bioPolymerCount; ip++)
      bioPolymers[ip].recalculateLeadMidpointsAndWingVectors();
  }


  /**
   * from Trajectory.setAtomPositions
   * 
   * base models only; not trajectories
   * @param bs 
   * @param dsspVersion 
   */
  public void resetRasmolBonds(BS bs, int dsspVersion) {
    BS bsDelete = new BS();
    hasRasmolHBonds = false;
    Model[] am = ms.am;
    Bond[] bo = ms.bo;
    for (int i = ms.bondCount; --i >= 0;) {
      Bond bond = bo[i];
      // trajectory atom .mi will be pointing to the trajectory;
      // here we check to see if their base model is this model
      if ((bond.order & Edge.BOND_H_CALC_MASK) != 0
          && am[bond.atom1.mi].trajectoryBaseIndex == modelIndex)
        bsDelete.set(i);
    }
    if (bsDelete.nextSetBit(0) >= 0)
      ms.deleteBonds(bsDelete, false);
    getRasmolHydrogenBonds(bs, bs, null, false, Integer.MAX_VALUE, false, null, dsspVersion);
  }

  public void getAtomicDSSRData(float[] dssrData, String dataType) {
    if (auxiliaryInfo.containsKey("dssr"))
      vwr.getAnnotationParser(true).getAtomicDSSRData(ms, modelIndex, dssrData, dataType);
  }

}
