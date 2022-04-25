/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2017-05-22 05:59:10 -0500 (Mon, 22 May 2017) $
 * $Revision: 21610 $
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
package org.jmol.modelset;

import java.util.Hashtable;

import java.util.Map;
import java.util.Properties;

import org.jmol.api.SymmetryInterface;
import javajs.util.BS;


import javajs.util.AU;
import javajs.util.M4d;
import javajs.util.P3d;
import javajs.util.SB;

import org.jmol.util.BSUtil;
import org.jmol.viewer.FileManager;

public class Model {

  /*
   * In Jmol all atoms and bonds are kept as a set of arrays in 
   * the AtomCollection and BondCollection objects. 
   * Thus, "Model" is not atoms and bonds. 
   * It is a description of all the:
   * 
   * chains (as defined in the file)
   *   and their associated file-associated groups,  
   * polymers (same, I think, but in terms of secondary structure)
   *   and their associated monomers
   * molecules (as defined by connectivity)
   *  
   * A Model then is just a small set of fields, a few arrays pointing
   * to other objects, and a couple of hash tables for information storage
   * 
   * Additional information here includes
   * how many atoms there were before symmetry was applied
   * as well as a bit about insertions and alternative locations.
   * 
   * 
   * one model = one animation "frame", but we don't use the "f" word
   * here because that would confuse the issue.
   * 
   * If multiple files are loaded, then they will appear here in 
   * at least as many Model objects. Each vibration will be a complete
   * set of atoms as well.
   * 
   * Jmol 11.3.58 developed the trajectory idea -- where
   * multiple models may share the same structures, bonds, etc., but
   * just differ in atom positions, saved in the Trajectories Vector
   * in ModelCollection.
   *  
   */

  /**
   * BE CAREFUL: FAILURE TO NULL REFERENCES TO modelSet WILL PREVENT
   * FINALIZATION AND CREATE A MEMORY LEAK.
   * 
   */
  public ModelSet ms;

  /**
   * mat4 tracks the rotation/translation of the full model using  rotateSelected or translateSelected 
   */
  public M4d mat4;
  
  public int modelIndex; // our 0-based reference
  int fileIndex; // 0-based file reference
  public boolean isBioModel;
  public boolean isPdbWithMultipleBonds;
  public boolean isModelKit;


  public Chain[] chains = new Chain[8];

  public SymmetryInterface simpleCage;
  public Map<String, Object> dssrCache;
  public Orientation orientation;
  public Map<String, Object> auxiliaryInfo;
  public Properties properties;
  public SymmetryInterface biosymmetry;
  Map<String, Integer> dataFrames;
  P3d translation;

  int dataSourceFrame = -1;


  public String loadState = "";
  public SB loadScript = new SB();

  public boolean hasRasmolHBonds;
  public boolean structureTainted;
  public boolean isJmolDataFrame;
  boolean isTrajectory;

  public int trajectoryBaseIndex;
  
  public int altLocCount;
  int insertionCount;
  /**
   * atom count; includes deleted atoms only if not being nulled (Jmol 14.31 or below)
   */
  public int act = 0; 
  private int bondCount = -1;
  protected int chainCount = 0;
  public int groupCount = -1;
  public int hydrogenCount;
  public int moleculeCount;
  int biosymmetryCount;
  
  public int firstAtomIndex;
  int firstMoleculeIndex;
  
  /**
   * Note that this bitset may or may not include bsAtomsDeleted
   * 
   */
  public final BS bsAtoms = new BS();
  public final BS bsAtomsDeleted = new BS();

  double defaultRotationRadius;
  public long frameDelay;
  public int selectedTrajectory = -1;

  String jmolData; // from a PDB remark "Jmol PDB-encoded data"
  String jmolFrameType;

  public String pdbID;

  public Model() {
    
  }
  
  public Model set(ModelSet modelSet, int modelIndex, int trajectoryBaseIndex,
      String jmolData, Properties properties, Map<String, Object> auxiliaryInfo) {
    ms = modelSet;
    dataSourceFrame = this.modelIndex = modelIndex;
    isTrajectory = (trajectoryBaseIndex >= 0);
    this.trajectoryBaseIndex = (isTrajectory ? trajectoryBaseIndex : modelIndex);
    if (auxiliaryInfo == null) {
      auxiliaryInfo = new Hashtable<String, Object>();
    }
    this.auxiliaryInfo = auxiliaryInfo;
    Integer bc = ((Integer) auxiliaryInfo.get("biosymmetryCount"));
    if (bc != null) {
      biosymmetryCount = bc.intValue();
      biosymmetry = (SymmetryInterface) auxiliaryInfo.get("biosymmetry");
    }
    String fname = (String) auxiliaryInfo.get("fileName");
    if (fname != null)
      auxiliaryInfo.put("fileName", FileManager.stripTypePrefix(fname));
    
    this.properties = properties;
    if (jmolData == null) {
      jmolFrameType = "modelSet";
    } else {
      this.jmolData = jmolData;
      isJmolDataFrame = true;
      auxiliaryInfo.put("jmolData", jmolData);
      auxiliaryInfo.put("title", jmolData);
      jmolFrameType = (jmolData.indexOf("ramachandran") >= 0 ? "ramachandran"
          : jmolData.indexOf("quaternion") >= 0 ? "quaternion" : "data");
    }
    return this;
  }

  /**
   * not actually accessed -- just pointing out what it is
   * @return true atom count
   */
  // this one is variable and calculated only if necessary:
  public int getTrueAtomCount() {
    return BSUtil.andNot(bsAtoms, bsAtomsDeleted).cardinality();
  }

  private BS bsCheck;

  boolean hasChirality;

  /**
   * a flag that, when false, indicates that the model has atoms in different regions of the Atom[] array
   * 
   */
  public boolean isOrderly = true;

  /**
   * tracks all presymmetry asymmetric unit atoms; atoms added using the ModelKit will add to this.
   */
  public BS bsAsymmetricUnit;
  
  /**
   * 
   * @param bs
   * @return true if all undeleted atom bits in this model are in bs
   */
  public boolean isContainedIn(BS bs) {
    if (bsCheck == null)
      bsCheck = new BS();
    bsCheck.clearAll();
    bsCheck.or(bs);
    BS bsa = BSUtil.andNot(bsAtoms, bsAtomsDeleted);
    bsCheck.and(bsa);
    return bsCheck.equals(bsa);
  }

  public void resetBoundCount() {
    bondCount = -1;
  }

  public int getBondCount() {
    if (bondCount >= 0)
      return bondCount;
    Bond[] bonds = ms.bo;
    bondCount = 0;
    for (int i = ms.bondCount; --i >= 0;)
      if (bonds[i].atom1.mi == modelIndex)
        bondCount++;
    return bondCount;
  }

  public int getChainCount(boolean countWater) {
    if (chainCount > 1 && !countWater)
      for (int i = 0; i < chainCount; i++)
        if (chains[i].chainID == '\0')
          return chainCount - 1;
    return chainCount;
  }

  void calcSelectedGroupsCount(BS bsSelected) {
    for (int i = chainCount; --i >= 0;)
      chains[i].calcSelectedGroupsCount(bsSelected);
  }

  public int getGroupCount() {
    if (groupCount < 0) {
      groupCount = 0;
      for (int i = chainCount; --i >= 0;)
        groupCount += chains[i].groupCount;
    }
    return groupCount;
  }

  public Chain getChainAt(int i) {
    return (i < chainCount ? chains[i] : null);
  }

  Chain getChain(int chainID) {
    for (int i = chainCount; --i >= 0;) {
      Chain chain = chains[i];
      if (chain.chainID == chainID)
        return chain;
    }
    return null;
  }

  /**
   * Something has changed; clear the DSSR cache and possibly remove DSSR entirely.
   * 
   * 
   * @param totally set TRUE if atoms have moved so we force a new DSSR calculation.
   */
  public void resetDSSR(boolean totally) {
    dssrCache = null;
    if (totally)
      auxiliaryInfo.remove("dssr");
  }

  public void fixIndices(int modelIndex, int nAtomsDeleted, BS bsDeleted) {
    // also in BioModel
    fixIndicesM(modelIndex, nAtomsDeleted, bsDeleted);
  }

  protected void fixIndicesM(int modelIndex, int nAtomsDeleted, BS bsDeleted) {
    if (dataSourceFrame > modelIndex)
      dataSourceFrame--;
    if (trajectoryBaseIndex > modelIndex)
      trajectoryBaseIndex--;
    firstAtomIndex -= nAtomsDeleted;
    for (int i = 0; i < chainCount; i++)
      chains[i].fixIndices(nAtomsDeleted, bsDeleted);
    BSUtil.deleteBits(bsAtoms, bsDeleted);
    BSUtil.deleteBits(bsAtomsDeleted, bsDeleted);
  }

  public boolean freeze() {
    freezeM();
    return false;
  }

  protected void freezeM() {
    for (int i = 0; i < chainCount; i++)
      if (chains[i].groupCount == 0) {
        for (int j = i + 1; j < chainCount; j++)
          chains[j - 1] = chains[j];
        chainCount--;
      }    
    chains = (Chain[]) AU.arrayCopyObject(chains, chainCount);
    groupCount = -1;
    getGroupCount();
    for (int i = 0; i < chainCount; ++i)
      chains[i].groups = (Group[]) AU.arrayCopyObject(chains[i].groups,
          chains[i].groupCount);
  }

  public void setSimpleCage(SymmetryInterface ucell) {
    simpleCage = ucell;
    if (ucell != null) {
      auxiliaryInfo.put("unitCellParams", ucell.getUnitCellParams());
    }
  }

 }
