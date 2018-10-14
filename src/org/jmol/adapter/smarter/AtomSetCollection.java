/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2018-08-16 16:01:22 -0500 (Thu, 16 Aug 2018) $
 * $Revision: 21924 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.adapter.smarter;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.V3;

import org.jmol.api.Interface;
import org.jmol.api.SymmetryInterface;
import javajs.util.BS;

import org.jmol.util.BSUtil;
import org.jmol.util.Logger;

@SuppressWarnings("unchecked")
public class AtomSetCollection {

  AtomSetCollectionReader reader;

  public BS bsAtoms; // required for CIF reader

  public String fileTypeName;

  String collectionName;

  public void setCollectionName(String collectionName) {
    if (collectionName != null
        && (collectionName = collectionName.trim()).length() > 0)
      this.collectionName = collectionName;
  }

  public Map<String, Object> atomSetInfo = new Hashtable<String, Object>();

  private final static String[] globalBooleans = {
      "someModelsHaveFractionalCoordinates", "someModelsHaveSymmetry",
      "someModelsHaveUnitcells", "someModelsHaveCONECT", "isPDB", "someModelsHaveDomains", "someModelsHaveValidations" };

  public final static int GLOBAL_FRACTCOORD = 0;
  public final static int GLOBAL_SYMMETRY = 1;
  public final static int GLOBAL_UNITCELLS = 2;
  public final static int GLOBAL_CONECT = 3;
  public final static int GLOBAL_ISPDB = 4;
  public final static int GLOBAL_DOMAINS = 5;
  public final static int GLOBAL_VALIDATIONS = 6;
  

  public void clearGlobalBoolean(int globalIndex) {
    atomSetInfo.remove(globalBooleans[globalIndex]);
  }

  public void setGlobalBoolean(int globalIndex) {
    setInfo(globalBooleans[globalIndex], Boolean.TRUE);
  }

  boolean getGlobalBoolean(int globalIndex) {
    return (atomSetInfo.get(globalBooleans[globalIndex]) == Boolean.TRUE);
  }

  public Atom[] atoms = new Atom[256];
  public int ac;
  public Bond[] bonds = new Bond[256];
  public int bondCount;
  public Structure[] structures = new Structure[16];
  public int structureCount;
  public int atomSetCount;
  public int iSet = -1;


  private int[] atomSetNumbers = new int[16];
  private int[] atomSetAtomIndexes = new int[16];
  private int[] atomSetAtomCounts = new int[16];
  private int[] atomSetBondCounts = new int[16];
  private Map<String, Object>[] atomSetAuxiliaryInfo = new Hashtable[16];

  public String errorMessage;

  public boolean coordinatesAreFractional;
  boolean isTrajectory;
  private int trajectoryStepCount = 0;

  private Lst<P3[]> trajectorySteps;
  private Lst<V3[]> vibrationSteps;
  private Lst<String> trajectoryNames;

  public boolean doFixPeriodic;
  public boolean allowMultiple; // set false only in CastepReader for a phonon file

  private Lst<AtomSetCollectionReader> readerList;

  public AtomSetCollection(String fileTypeName, AtomSetCollectionReader reader,
      AtomSetCollection[] array, Lst<?> list) {

    // merging files

    this.fileTypeName = fileTypeName;
    this.reader = reader;
    allowMultiple = (reader == null || reader.desiredVibrationNumber < 0);
    // set the default PATH properties as defined in the SmarterJmolAdapter
    Properties p = new Properties();
    p.put("PATH_KEY", SmarterJmolAdapter.PATH_KEY);
    p.put("PATH_SEPARATOR", SmarterJmolAdapter.PATH_SEPARATOR);
    setInfo("properties", p);
    if (array != null) {
      int n = 0;
      readerList = new Lst<AtomSetCollectionReader>();
      for (int i = 0; i < array.length; i++)
        if (array[i] != null && (array[i].ac > 0 || array[i].reader != null
            && array[i].reader.mustFinalizeModelSet))
          appendAtomSetCollection(n++, array[i]);
      if (n > 1)
        setInfo("isMultiFile", Boolean.TRUE);
    } else if (list != null) {
      // (from zipped zip files)
      setInfo("isMultiFile", Boolean.TRUE);
      appendAtomSetCollectionList(list);
    }
  }

  private void appendAtomSetCollectionList(Lst<?> list) {
    int n = list.size();
    if (n == 0) {
      errorMessage = "No file found!";
      return;
    }

    for (int i = 0; i < n; i++) {
      Object o = list.get(i);
      if (o instanceof Lst)
        appendAtomSetCollectionList((Lst<?>) o);
      else
        appendAtomSetCollection(i, (AtomSetCollection) o);
    }
  }

  public void setTrajectory() {
    if (!isTrajectory)
      trajectorySteps = new Lst<P3[]>();
    isTrajectory = true;
    int n = (bsAtoms == null ? ac : bsAtoms.cardinality());
    if (n == 0)
      return;
    P3[] trajectoryStep = new P3[n];
    boolean haveVibrations = (n > 0 && atoms[0].vib != null && !Float
        .isNaN(atoms[0].vib.z));
    V3[] vibrationStep = (haveVibrations ? new V3[n] : null);
    P3[] prevSteps = (trajectoryStepCount == 0 ? null : (P3[]) trajectorySteps
        .get(trajectoryStepCount - 1));
    for (int i = 0, ii = 0; i < ac; i++) {
      if (bsAtoms != null && !bsAtoms.get(i))
        continue;
      P3 pt = P3.newP(atoms[i]);
      if (doFixPeriodic && prevSteps != null)
        pt = fixPeriodic(pt, prevSteps[i]);
      trajectoryStep[ii] = pt;
      if (haveVibrations)
        vibrationStep[ii] = atoms[i].vib;
      ii++;
    }
    if (haveVibrations) {
      if (vibrationSteps == null) {
        vibrationSteps = new Lst<V3[]>();
        for (int i = 0; i < trajectoryStepCount; i++)
          vibrationSteps.addLast(null);
      }
      vibrationSteps.addLast(vibrationStep);
    }
    trajectorySteps.addLast(trajectoryStep);
    trajectoryStepCount++;
  }

  /**
   * Appends an AtomSetCollection
   * 
   * @param collectionIndex
   *        collection index for new model number
   * @param collection
   *        AtomSetCollection to append
   */
  public void appendAtomSetCollection(int collectionIndex,
                                      AtomSetCollection collection) {

    // List readers that will need calls to finalizeModelSet();
    if (collection.reader != null && collection.reader.mustFinalizeModelSet)
      readerList.addLast(collection.reader);
    // Initializations
    int existingAtomsCount = ac;

    // auxiliary info
    setInfo("loadState",
        collection.atomSetInfo.get("loadState"));

    // append to bsAtoms if necessary (CIF reader molecular mode)
    if (collection.bsAtoms != null) {
      if (bsAtoms == null)
        bsAtoms = new BS();
      for (int i = collection.bsAtoms.nextSetBit(0); i >= 0; i = collection.bsAtoms
          .nextSetBit(i + 1))
        bsAtoms.set(existingAtomsCount + i);
    }

    // Clone each AtomSet
    int clonedAtoms = 0;
    int atomSetCount0 = atomSetCount;
    for (int atomSetNum = 0; atomSetNum < collection.atomSetCount; atomSetNum++) {
      newAtomSet();
      // must fix referencing for someModelsHaveCONECT business
      Map<String, Object> info = atomSetAuxiliaryInfo[iSet] = collection.atomSetAuxiliaryInfo[atomSetNum];
      int[] atomInfo = (int[]) info.get("PDB_CONECT_firstAtom_count_max");
      if (atomInfo != null)
        atomInfo[0] += existingAtomsCount;
      setCurrentModelInfo("title", collection.collectionName);
      setAtomSetName(collection.getAtomSetName(atomSetNum));
      for (int atomNum = 0; atomNum < collection.atomSetAtomCounts[atomSetNum]; atomNum++) {
        try {
          if (bsAtoms != null)
            bsAtoms.set(ac);
          newCloneAtom(collection.atoms[clonedAtoms]);
        } catch (Exception e) {
          errorMessage = "appendAtomCollection error: " + e;
        }
        clonedAtoms++;
      }

      // numbers
      atomSetNumbers[iSet] = (collectionIndex < 0 ? iSet + 1
          : ((collectionIndex + 1) * 1000000)
              + collection.atomSetNumbers[atomSetNum]);

      // Note -- this number is used for Model.modelNumber. It is a combination of
      // file number * 1000000 + PDB MODEL NUMBER, which could be anything.
      // Adding the file number here indicates that we have multiple files.
      // But this will all be adjusted in ModelLoader.finalizeModels(). BH 11/2007

    }
    // Clone bonds
    for (int bondNum = 0; bondNum < collection.bondCount; bondNum++) {
      Bond bond = collection.bonds[bondNum];
      addNewBondWithOrder(bond.atomIndex1 + existingAtomsCount, bond.atomIndex2
          + existingAtomsCount, bond.order);
    }
    // Set globals
    for (int i = globalBooleans.length; --i >= 0;)
      if (collection.getGlobalBoolean(i))
        setGlobalBoolean(i);

    // Add structures
    for (int i = 0; i < collection.structureCount; i++) {
      Structure s = collection.structures[i];
      addStructure(s);
      s.modelStartEnd[0] += atomSetCount0;
      s.modelStartEnd[1] += atomSetCount0;
    }
  }

  public void setNoAutoBond() {
    setInfo("noAutoBond", Boolean.TRUE);
  }

  void freeze(boolean reverseModels) {
    if (atomSetCount == 1 && collectionName == null)
      collectionName = (String) getAtomSetAuxiliaryInfoValue(0, "name");
    //Logger.debug("AtomSetCollection.freeze; ac = " + ac);
    if (reverseModels)
      reverseAtomSets();
    if (trajectoryStepCount > 1)
      finalizeTrajectory();
    getList(true);
    getList(false);
    for (int i = 0; i < atomSetCount; i++) {
      setModelInfoForSet("initialAtomCount",
          Integer.valueOf(atomSetAtomCounts[i]), i);
      setModelInfoForSet("initialBondCount",
          Integer.valueOf(atomSetBondCounts[i]), i);
    }
  }

  private void reverseAtomSets() {
    reverseArray(atomSetAtomIndexes);
    reverseArray(atomSetNumbers);
    reverseArray(atomSetAtomCounts);
    reverseArray(atomSetBondCounts);
    reverseList(trajectorySteps);
    reverseList(trajectoryNames);
    reverseList(vibrationSteps);
    reverseObject(atomSetAuxiliaryInfo);
    for (int i = 0; i < ac; i++)
      atoms[i].atomSetIndex = atomSetCount - 1 - atoms[i].atomSetIndex;
    for (int i = 0; i < structureCount; i++) {
      int m = structures[i].modelStartEnd[0];
      if (m >= 0) {
        structures[i].modelStartEnd[0] = atomSetCount - 1
            - structures[i].modelStartEnd[1];
        structures[i].modelStartEnd[1] = atomSetCount - 1 - m;
      }
    }
    for (int i = 0; i < bondCount; i++)
      bonds[i].atomSetIndex = atomSetCount - 1
          - atoms[bonds[i].atomIndex1].atomSetIndex;
    reverseSets(bonds, bondCount);
    //getAtomSetAuxiliaryInfo("PDB_CONECT_firstAtom_count_max" ??
    Lst<Atom>[] lists = AU.createArrayOfArrayList(atomSetCount);
    for (int i = 0; i < atomSetCount; i++)
      lists[i] = new Lst<Atom>();
    for (int i = 0; i < ac; i++)
      lists[atoms[i].atomSetIndex].addLast(atoms[i]);
    int[] newIndex = new int[ac];
    int n = ac;
    for (int i = atomSetCount; --i >= 0;)
      for (int j = lists[i].size(); --j >= 0;) {
        Atom a = atoms[--n] = lists[i].get(j);
        newIndex[a.index] = n;
        a.index = n;
      }
    for (int i = 0; i < bondCount; i++) {
      bonds[i].atomIndex1 = newIndex[bonds[i].atomIndex1];
      bonds[i].atomIndex2 = newIndex[bonds[i].atomIndex2];
    }
    for (int i = 0; i < atomSetCount; i++) {
      int[] conect = (int[]) getAtomSetAuxiliaryInfoValue(i,
          "PDB_CONECT_firstAtom_count_max");
      if (conect == null)
        continue;
      conect[0] = newIndex[conect[0]];
      conect[1] = atomSetAtomCounts[i];
    }
  }

  private void reverseSets(AtomSetObject[] o, int n) {
    Lst<AtomSetObject>[] lists = AU.createArrayOfArrayList(atomSetCount);
    for (int i = 0; i < atomSetCount; i++)
      lists[i] = new Lst<AtomSetObject>();
    for (int i = 0; i < n; i++) {
      int index = o[i].atomSetIndex;
      if (index < 0)
        return;
      lists[o[i].atomSetIndex].addLast(o[i]);
    }
    for (int i = atomSetCount; --i >= 0;)
      for (int j = lists[i].size(); --j >= 0;)
        o[--n] = lists[i].get(j);
  }

  private void reverseObject(Object[] o) {
    int n = atomSetCount;
    for (int i = n / 2; --i >= 0;)
      AU.swap(o, i, n - 1 - i);
  }

  private static void reverseList(Lst<?> list) {
    if (list == null)
      return;
    Collections.reverse(list);
  }

  private void reverseArray(int[] a) {
    int n = atomSetCount;
    for (int i = n / 2; --i >= 0;)
      AU.swapInt(a, i, n - 1 - i);
  }

  private void getList(boolean isAltLoc) {
    int i;
    for (i = ac; --i >= 0;)
      if (atoms[i] != null
          && (isAltLoc ? atoms[i].altLoc : atoms[i].insertionCode) != '\0')
        break;
    if (i < 0)
      return;
    String[] lists = new String[atomSetCount];
    for (i = 0; i < atomSetCount; i++)
      lists[i] = "";
    int pt;
    for (i = 0; i < ac; i++) {
      if (atoms[i] == null)
        continue;
      char id = (isAltLoc ? atoms[i].altLoc : atoms[i].insertionCode);
      if (id != '\0' && lists[pt = atoms[i].atomSetIndex].indexOf(id) < 0)
        lists[pt] += id;
    }
    String type = (isAltLoc ? "altLocs" : "insertionCodes");
    for (i = 0; i < atomSetCount; i++)
      if (lists[i].length() > 0)
        setModelInfoForSet(type, lists[i], i);
  }

  void finish() {
    if (reader != null)
      reader.finalizeModelSet();
    else if (readerList != null)
      for (int i = 0; i < readerList.size(); i++)
        readerList.get(i).finalizeModelSet();
    atoms = null;
    atomSetAtomCounts = new int[16];
    atomSetAuxiliaryInfo = new Hashtable[16];
    atomSetInfo = new Hashtable<String, Object>();
    atomSetCount = 0;
    atomSetNumbers = new int[16];
    atomSymbolicMap = new Hashtable<String, Atom>();
    bonds = null;
    iSet = -1;
    readerList = null;
    xtalSymmetry = null;
    structures = new Structure[16];
    structureCount = 0;
    trajectorySteps = null;
    vibrationSteps = null;
  }

  public void discardPreviousAtoms() {
    for (int i = ac; --i >= 0;)
      atoms[i] = null;
    ac = 0;
    atomSymbolicMap.clear();
    atomSetCount = 0;
    iSet = -1;
    for (int i = atomSetAuxiliaryInfo.length; --i >= 0;) {
      atomSetAtomCounts[i] = 0;
      atomSetBondCounts[i] = 0;
      atomSetAuxiliaryInfo[i] = null;
    }
  }

  public void removeCurrentAtomSet() {
    if (iSet < 0)
      return;
    int ai = atomSetAtomIndexes[iSet];
    if (bsAtoms != null)
      bsAtoms.clearBits(ai, ac);
    ac = ai;
    atomSetAtomCounts[iSet] = 0;
    iSet--;
    atomSetCount--;
    reader.doCheckUnitCell = false;
  }

  public int getHydrogenAtomCount() {
    int n = 0;
    for (int i = 0; i < ac; i++)
      if (atoms[i].elementNumber == 1 || atoms[i].elementSymbol.equals("H"))
        n++;
    return n;
  }

  public Atom newCloneAtom(Atom atom) throws Exception {
    Atom clone = atom.getClone();
    addAtom(clone);
    return clone;
  }

  // FIX ME This should really also clone the other things pertaining
  // to an atomSet, like the bonds (which probably should be remade...)
  // but also the atomSetProperties and atomSetName...
  public int cloneFirstAtomSet(int atomCount) throws Exception {
    if (!allowMultiple)
      return 0;
    newAtomSet();
    if (atomCount == 0)
      atomCount = atomSetAtomCounts[0];
    for (int i = 0; i < atomCount; ++i)
      newCloneAtom(atoms[i]);
    return ac;
  }

  public void cloneAtomSetWithBonds(boolean isLast) throws Exception {
    int nBonds = atomSetBondCounts[isLast ? iSet : 0];
    int atomIncrement = (isLast ? cloneLastAtomSet() : cloneFirstAtomSet(0));
    if (atomIncrement > 0)
      for (int i = 0; i < nBonds; i++) {
        Bond bond = bonds[bondCount - nBonds];
        addNewBondWithOrder(bond.atomIndex1 + atomIncrement, bond.atomIndex2
            + atomIncrement, bond.order);
      }
  }

  public int cloneLastAtomSet() throws Exception {
    return cloneLastAtomSetFromPoints(0, null);
  }

  public int cloneLastAtomSetFromPoints(int ac, P3[] pts) throws Exception {
    if (!allowMultiple) // CASTEP reader only
      return 0;
    int count = (ac > 0 ? ac : getLastAtomSetAtomCount());
    int atomIndex = getLastAtomSetAtomIndex();
    newAtomSet();
    for (int i = 0; i < count; ++i) {
      Atom atom = newCloneAtom(atoms[atomIndex++]);
      if (pts != null)
        atom.setT(pts[i]);
    }
    return count;
  }

  public int getLastAtomSetAtomCount() {
    return atomSetAtomCounts[iSet];
  }

  public int getLastAtomSetAtomIndex() {
    //Logger.debug("atomSetCount=" + atomSetCount);
    return ac - atomSetAtomCounts[iSet];
  }

  public Atom addNewAtom() {
    return addAtom(new Atom());
  }

  public Atom addAtom(Atom atom) {
    if (ac == atoms.length) {
      if (ac > 200000)
        atoms = (Atom[]) AU.ensureLength(atoms, ac + 50000);
      else
        atoms = (Atom[]) AU.doubleLength(atoms);
    }
    if (atomSetCount == 0)
      newAtomSet();
    atom.index = ac;
    atoms[ac++] = atom;    
    atom.atomSetIndex = iSet;
    atom.atomSite = atomSetAtomCounts[iSet]++;
    return atom;
  }

  public void addAtomWithMappedName(Atom atom) {
    String atomName = addAtom(atom).atomName;
    if (atomName != null)
      atomSymbolicMap.put(atomName, atom);
  }

  public void addAtomWithMappedSerialNumber(Atom atom) {
    int atomSerial = addAtom(atom).atomSerial;
    if (atomSerial != Integer.MIN_VALUE)
      atomSymbolicMap.put("" + atomSerial, atom);
  }

  public Atom getAtomFromName(String atomName) {
    return atomSymbolicMap.get(atomName);
  }

  public int getAtomIndex(String name) {
    Atom a = atomSymbolicMap.get(name);
    return (a == null ? -1 : a.index);
  }

  public void addNewBondWithOrder(int atomIndex1, int atomIndex2, int order) {
    if (atomIndex1 >= 0 && atomIndex1 < ac && atomIndex2 >= 0
        && atomIndex2 < ac && atomIndex1 != atomIndex2)
      addBond(new Bond(atomIndex1, atomIndex2, order));
  }

  public void addNewBondFromNames(String atomName1, String atomName2, int order) {
    addNewBondWithOrderA(getAtomFromName(atomName1), getAtomFromName(atomName2), order);
  }

  public void addNewBondWithOrderA(Atom atom1, Atom atom2,
                                    int order) {
    if (atom1 != null && atom2 != null)
      addNewBondWithOrder(atom1.index, atom2.index, order);
  }

  public void addBond(Bond bond) {
    if (trajectoryStepCount > 0)
      return;
    if (bond.atomIndex1 < 0 || bond.atomIndex2 < 0
        || bond.order < 0
        || bond.atomIndex1 == bond.atomIndex2
        ||
        //do not allow bonds between models
        atoms[bond.atomIndex1].atomSetIndex != atoms[bond.atomIndex2].atomSetIndex) {
      if (Logger.debugging) {
        Logger.debug(">>>>>>BAD BOND:" + bond.atomIndex1 + "-"
            + bond.atomIndex2 + " order=" + bond.order);
      }
      return;
    }
    if (bondCount == bonds.length)
      bonds = (Bond[]) AU.arrayCopyObject(bonds, bondCount + 1024);
    bonds[bondCount++] = bond;
    atomSetBondCounts[iSet]++;
  }

  public BS bsStructuredModels;

  public void finalizeStructures() {
    if (structureCount == 0)
      return;
    bsStructuredModels = new BS();
    Map<String, Integer> map = new Hashtable<String, Integer>();
    for (int i = 0; i < structureCount; i++) {
      Structure s = structures[i];
      if (s.modelStartEnd[0] == -1) {
        s.modelStartEnd[0] = 0;
        s.modelStartEnd[1] = atomSetCount - 1;
      }
      bsStructuredModels.setBits(s.modelStartEnd[0], s.modelStartEnd[1] + 1);
      if (s.strandCount == 0)
        continue;
      String key = s.structureID + " " + s.modelStartEnd[0];
      Integer v = map.get(key);
      int count = (v == null ? 0 : v.intValue()) + 1;
      map.put(key, Integer.valueOf(count));
    }
    for (int i = 0; i < structureCount; i++) {
      Structure s = structures[i];
      if (s.strandCount == 1)
        s.strandCount = map.get(s.structureID + " " + s.modelStartEnd[0])
            .intValue();
    }
  }

  public void addStructure(Structure structure) {
    if (structureCount == structures.length)
      structures = (Structure[]) AU.arrayCopyObject(structures,
          structureCount + 32);
    structures[structureCount++] = structure;
  }

  public void addVibrationVectorWithSymmetry(int iatom, float vx, float vy,
                                             float vz, boolean withSymmetry) {
    if (!withSymmetry) {
      addVibrationVector(iatom, vx, vy, vz);
      return;
    }
    int atomSite = atoms[iatom].atomSite;
    int atomSetIndex = atoms[iatom].atomSetIndex;
    for (int i = iatom; i < ac && atoms[i].atomSetIndex == atomSetIndex; i++) {
      //TODO check this: Shouldn't we be symmetrizing here?
      if (atoms[i].atomSite == atomSite)
        addVibrationVector(i, vx, vy, vz);
    }
  }

  public V3 addVibrationVector(int iatom, float x, float y, float z) {
    if (!allowMultiple)
      iatom = iatom % ac;
    return (atoms[iatom].vib = V3.new3(x, y, z));
  }

  public void setCoordinatesAreFractional(boolean tf) {
    coordinatesAreFractional = tf;
    setCurrentModelInfo("coordinatesAreFractional", Boolean.valueOf(tf));
    if (tf)
      setGlobalBoolean(GLOBAL_FRACTCOORD);
  }

  public boolean haveAnisou;

  public void setAnisoBorU(Atom atom, float[] data, int type) {
    haveAnisou = true;
    atom.anisoBorU = data;
    data[6] = type;
  }

  public void setU(Atom atom, int i, float val) {
    // Ortep Type 8: D = 2pi^2, C = 2, a*b*
    float[] data = atom.anisoBorU;
    if (data == null)
      setAnisoBorU(atom, data = new float[8], 8);
    data[i] = val;
  }

  public int baseSymmetryAtomCount;
  public boolean checkLatticeOnly;

  public XtalSymmetry xtalSymmetry;

  public XtalSymmetry getXSymmetry() {
    if (xtalSymmetry == null)
      xtalSymmetry = ((XtalSymmetry) Interface
          .getOption("adapter.smarter.XtalSymmetry", reader.vwr, "file")).set(reader);
    return xtalSymmetry;
  }

  public SymmetryInterface getSymmetry() {
    return getXSymmetry().getSymmetry();
  }

  public SymmetryInterface setSymmetry(SymmetryInterface symmetry) {
    return (symmetry == null ? null : getXSymmetry().setSymmetry(symmetry));
  }

  public void setTensors() {
    if (haveAnisou)
      getXSymmetry().setTensors();
  }

  int bondIndex0;

  public boolean checkSpecial = true;

  public Map<String, Atom> atomSymbolicMap = new Hashtable<String, Atom>();

  public boolean haveUnitCell;

  public int vibScale;

  public void setInfo(String key, Object value) {
    if (value == null)
      atomSetInfo.remove(key);
    else
      atomSetInfo.put(key, value);
  }

  /**
   * Sets the partial atomic charges based on asc auxiliary info
   * 
   * @param auxKey
   *        The auxiliary key name that contains the charges
   * @return true if the data exist; false if not
   */

  public boolean setAtomSetCollectionPartialCharges(String auxKey) {
    if (!atomSetInfo.containsKey(auxKey))
      return false;
    Lst<Float> atomData = (Lst<Float>) atomSetInfo.get(auxKey);
    int n = atomData.size();
    for (int i = ac; --i >= 0;)
      atoms[i].partialCharge = atomData.get(i % n).floatValue();
    Logger.info("Setting partial charges type " + auxKey);
    return true;
  }

  public void mapPartialCharge(String atomName, float charge) {
    getAtomFromName(atomName).partialCharge = charge;
  }

  ////////////////////////////////////////////////////////////////
  // atomSet stuff
  ////////////////////////////////////////////////////////////////

  private static P3 fixPeriodic(P3 pt, P3 pt0) {
    pt.x = fixPoint(pt.x, pt0.x);
    pt.y = fixPoint(pt.y, pt0.y);
    pt.z = fixPoint(pt.z, pt0.z);
    return pt;
  }

  private static float fixPoint(float x, float x0) {
    while (x - x0 > 0.9) {
      x -= 1;
    }
    while (x - x0 < -0.9) {
      x += 1;
    }
    return x;
  }

  public void finalizeTrajectoryAs(Lst<P3[]> trajectorySteps,
                                   Lst<V3[]> vibrationSteps) {
    this.trajectorySteps = trajectorySteps;
    this.vibrationSteps = vibrationSteps;
    trajectoryStepCount = trajectorySteps.size();
    finalizeTrajectory();
  }

  private void finalizeTrajectory() {
    if (trajectoryStepCount == 0)
      return;
    //reset atom positions to original trajectory
    P3[] trajectory = trajectorySteps.get(0);
    V3[] vibrations = (vibrationSteps == null ? null : vibrationSteps.get(0));
    int n = (bsAtoms == null ? ac : bsAtoms.cardinality());
    if (vibrationSteps != null && vibrations != null && vibrations.length < n
        || trajectory.length < n) {
      errorMessage = "File cannot be loaded as a trajectory";
      return;
    }
    V3 v = new V3();
    for (int i = 0, ii = 0; i < ac; i++) {
      if (bsAtoms != null && !bsAtoms.get(i))
        continue;
      if (vibrationSteps != null)
        atoms[i].vib = (vibrations == null ? v : vibrations[ii]);
      if (trajectory[ii] != null)
        atoms[i].setT(trajectory[ii]);
      ii++;
    }
    setInfo("trajectorySteps", trajectorySteps);
    if (vibrationSteps != null)
      setInfo("vibrationSteps", vibrationSteps);
  }

  /**
   * Create a new atoms set, clearing the atom map
   * 
   */
  public void newAtomSet() {
    newAtomSetClear(true);
  }

  /**
   * Create a new atom set, optionally clearing the atom map.
   * 
   * @param doClearMap set to false only in CastepReader
   */
  public void newAtomSetClear(boolean doClearMap) {

    // we call reader.discardPreviousAtoms here because it may be 
    // overridden to do more than we do here (such as in BasisFunctionReader).
    if (!allowMultiple && iSet >= 0)
      reader.discardPreviousAtoms();
    bondIndex0 = bondCount;
    if (isTrajectory)
      reader.discardPreviousAtoms();
    iSet = atomSetCount++;
    if (atomSetCount > atomSetNumbers.length) {
      atomSetAtomIndexes = AU.doubleLengthI(atomSetAtomIndexes);
      atomSetAtomCounts = AU.doubleLengthI(atomSetAtomCounts);
      atomSetBondCounts = AU.doubleLengthI(atomSetBondCounts);
      atomSetAuxiliaryInfo = (Map<String, Object>[]) AU
          .doubleLength(atomSetAuxiliaryInfo);
    }
    atomSetAtomIndexes[iSet] = ac;
    if (atomSetCount + trajectoryStepCount > atomSetNumbers.length) {
      atomSetNumbers = AU.doubleLengthI(atomSetNumbers);
    }
    if (isTrajectory) {
      atomSetNumbers[iSet + trajectoryStepCount] = atomSetCount
          + trajectoryStepCount;
    } else {
      atomSetNumbers[iSet] = atomSetCount;
    }
    if (doClearMap) // false for CASTEP reader only
      atomSymbolicMap.clear();
    setCurrentModelInfo("title", collectionName);
  }

  public int getAtomSetAtomIndex(int i) {
    if  (i < 0)
      System.out.println("??");
    return atomSetAtomIndexes[i];
  }

  public int getAtomSetAtomCount(int i) {
    return atomSetAtomCounts[i];
  }

  public int getAtomSetBondCount(int i) {
    return atomSetBondCounts[i];
  }

  /**
   * Sets the name for the current AtomSet
   * 
   * @param atomSetName
   *        The name to be associated with the current AtomSet
   */
  public void setAtomSetName(String atomSetName) {
    if (atomSetName == null)
      return;
    if (isTrajectory) {
      setTrajectoryName(atomSetName);
      return;
    }
    String name0 = (iSet < 0 ? null : getAtomSetName(iSet));
    setModelInfoForSet("name", atomSetName, iSet);
    if (reader != null && atomSetName.length() > 0 && !atomSetName.equals(name0))
      reader.appendLoadNote(atomSetName);
    // TODO -- trajectories could have different names. Need this for vibrations?
    if (!allowMultiple)
      setCollectionName(atomSetName);
  }

  private void setTrajectoryName(String name) {
    if (trajectoryStepCount == 0)
      return;
    if (trajectoryNames == null) {
      trajectoryNames = new Lst<String>();
    }
    for (int i = trajectoryNames.size(); i < trajectoryStepCount; i++)
      trajectoryNames.addLast(null);
    trajectoryNames.set(trajectoryStepCount - 1, name);
  }

  /**
   * Sets the number for the current AtomSet
   * 
   * @param atomSetNumber
   *        The number for the current AtomSet.
   */
  public void setCurrentAtomSetNumber(int atomSetNumber) {
    setAtomSetNumber(iSet + (isTrajectory ? trajectoryStepCount : 0),
        atomSetNumber);
  }

  public void setAtomSetNumber(int index, int atomSetNumber) {
    atomSetNumbers[index] = atomSetNumber;
  }

  /**
   * Sets a property for the current AtomSet used specifically for creating
   * directories and plots of frequencies and molecular energies
   * 
   * @param key
   *        The key for the property
   * @param value
   *        The value to be associated with the key
   */
  public void setAtomSetModelProperty(String key, String value) {
    setAtomSetModelPropertyForSet(key, value, iSet);
  }

  /**
   * Sets the a property for the an AtomSet
   * 
   * @param key
   *        The key for the property
   * @param value
   *        The value for the property
   * @param atomSetIndex
   *        The index of the AtomSet to get the property
   */
  public void setAtomSetModelPropertyForSet(String key, String value,
                                            int atomSetIndex) {
    // lazy instantiation of the Properties object
    Properties p = (Properties) getAtomSetAuxiliaryInfoValue(atomSetIndex,
        "modelProperties");
    if (p == null)
      setModelInfoForSet("modelProperties", p = new Properties(),
          atomSetIndex);
    p.put(key, value);
    if (key.startsWith(".")) //.PATH will not be usable in Jmol
      p.put(key.substring(1), value);
  }

  /**
   * @param key 
   * @param data 
   * @param atomSetIndex 
   * @param isGroup  
   */
  public void setAtomProperties(String key, Object data, int atomSetIndex, boolean isGroup) {
    if (data instanceof String && !((String) data).endsWith("\n"))
      data = data + "\n";
    if (atomSetIndex < 0)
      atomSetIndex = iSet;
    Map<String, Object> p = (Map<String, Object>) getAtomSetAuxiliaryInfoValue(
        atomSetIndex, "atomProperties");
    if (p == null)
      setModelInfoForSet("atomProperties",
          p = new Hashtable<String, Object>(), atomSetIndex);
    p.put(key, data);
  }

  /**
   * Sets the partial atomic charges based on atomSet auxiliary info
   * 
   * @param auxKey
   *        The auxiliary key name that contains the charges
   * @return true if the data exist; false if not
   */

  boolean setAtomSetPartialCharges(String auxKey) {
    if (!atomSetAuxiliaryInfo[iSet].containsKey(auxKey)) {
      return false;
    }
    Lst<Float> atomData = (Lst<Float>) getAtomSetAuxiliaryInfoValue(iSet,
        auxKey);
    for (int i = atomData.size(); --i >= 0;) {
      atoms[i].partialCharge = atomData.get(i).floatValue();
    }
    return true;
  }

  public Object getAtomSetAuxiliaryInfoValue(int index, String key) {
    return atomSetAuxiliaryInfo[index >= 0 ? index : iSet].get(key);
  }

  /**
   * Sets auxiliary information for the AtomSet
   * 
   * @param key
   *        The key for the property
   * @param value
   *        The value to be associated with the key
   */
  public void setCurrentModelInfo(String key, Object value) {
    setModelInfoForSet(key, value, iSet);
  }

  /**
   * Sets auxiliary information for an AtomSet
   * 
   * @param key
   *        The key for the property
   * @param value
   *        The value for the property
   * @param atomSetIndex
   *        The index of the AtomSet to get the property
   */
  public void setModelInfoForSet(String key, Object value,
                                            int atomSetIndex) {
    if (atomSetIndex < 0)
      return;
    if (atomSetAuxiliaryInfo[atomSetIndex] == null)
      atomSetAuxiliaryInfo[atomSetIndex] = new Hashtable<String, Object>();
    if (value == null)
      atomSetAuxiliaryInfo[atomSetIndex].remove(key);
    else
      atomSetAuxiliaryInfo[atomSetIndex].put(key, value);
  }

  int getAtomSetNumber(int atomSetIndex) {
    return atomSetNumbers[atomSetIndex >= atomSetCount ? 0 : atomSetIndex];
  }

  String getAtomSetName(int atomSetIndex) {
    if (trajectoryNames != null && atomSetIndex < trajectoryNames.size())
      return trajectoryNames.get(atomSetIndex);
    if (atomSetIndex >= atomSetCount)
      atomSetIndex = atomSetCount - 1;
    return (String) getAtomSetAuxiliaryInfoValue(atomSetIndex, "name");
  }

  public Map<String, Object> getAtomSetAuxiliaryInfo(int atomSetIndex) {
    int i = (atomSetIndex >= atomSetCount ? atomSetCount - 1
        : atomSetIndex);
    return (i < 0 ? null : atomSetAuxiliaryInfo[i]);
  }

  //// for XmlChem3dReader, but could be for CUBE

  public void setAtomSetEnergy(String energyString, float value) {
    if (iSet < 0)
      return;
    Logger.info("Energy for model " + (iSet + 1) + " = " + energyString);
    setCurrentModelInfo("EnergyString", energyString);
    setCurrentModelInfo("Energy", Float.valueOf(value));
    setAtomSetModelProperty("Energy", "" + value);
  }

  public String setAtomSetFrequency(int mode, String pathKey, String label,
                                    String freq, String units) {
    setAtomSetModelProperty("FreqValue", freq);
    freq += " " + (units == null ? "cm^-1" : units);
    String name = (label == null ? "" : label + " ") + freq;
    setAtomSetName(name);
    setAtomSetModelProperty("Frequency", freq);
    setAtomSetModelProperty("Mode", "" + mode);
    setModelInfoForSet("vibrationalMode", Integer.valueOf(mode), iSet);
    if (label != null)
      setAtomSetModelProperty("FrequencyLabel", label);
    setAtomSetModelProperty(SmarterJmolAdapter.PATH_KEY, (pathKey == null ? ""
        : pathKey + SmarterJmolAdapter.PATH_SEPARATOR + "Frequencies")
        + "Frequencies");
    return name;
  }

  public String[][] getBondList() {
    String[][] info = new String[bondCount][];
    for (int i = 0; i < bondCount; i++) {
      info[i] = new String[] { atoms[bonds[i].atomIndex1].atomName,
          atoms[bonds[i].atomIndex2].atomName, "" + bonds[i].order };
    }
    return info;
  }

  public void centralize() {
    P3 pt = new P3();
    for (int i = 0; i < atomSetCount; i++) {
      int n = atomSetAtomCounts[i];
      int atom0 = atomSetAtomIndexes[i];
      pt.set(0, 0, 0);
      for (int j = atom0 + n; --j >= atom0;)
        pt.add(atoms[j]);
      pt.scale(1f / n);
      for (int j = atom0 + n; --j >= atom0;)
        atoms[j].sub(pt);
    }
  }

  void mergeTrajectories(AtomSetCollection a) {
    if (!isTrajectory || !a.isTrajectory || vibrationSteps != null)
      return;
    for (int i = 0; i < a.trajectoryStepCount; i++)
      trajectorySteps.add(trajectoryStepCount++, a.trajectorySteps.get(i));
    setInfo("trajectorySteps", trajectorySteps);
    setInfo("ignoreUnitCell", a.atomSetInfo.get("ignoreUnitCell"));
  }

  /**
   * note that sets must be iterated from LAST to FIRST
   * not a general method -- would mess up if we had unit cells
   * 
   * @param imodel
   */
  public void removeAtomSet(int imodel) {
    if (bsAtoms == null)
      bsAtoms = BSUtil.newBitSet2(0, ac);
    int i0 = atomSetAtomIndexes[imodel];
    int nAtoms = atomSetAtomCounts[imodel];
    int i1 = i0 + nAtoms;
    bsAtoms.clearBits(i0, i1);
    for (int i = i1; i < ac; i++)
      atoms[i].atomSetIndex--;
    for (int i = imodel + 1; i < atomSetCount; i++) {
      atomSetAuxiliaryInfo[i - 1] = atomSetAuxiliaryInfo[i];
      atomSetAtomIndexes[i - 1] = atomSetAtomIndexes[i];
      atomSetBondCounts[i - 1] = atomSetBondCounts[i];
      atomSetAtomCounts[i - 1] = atomSetAtomCounts[i];
      atomSetNumbers[i - 1] = atomSetNumbers[i];
    }
    for (int i = 0; i < bondCount; i++)
      bonds[i].atomSetIndex = atoms[bonds[i].atomIndex1].atomSetIndex;
    atomSetAuxiliaryInfo[--atomSetCount] = null;
    int n = 0;
    for (int i = 0; i < structureCount; i++) {
      Structure s = structures[i];
      if (s.modelStartEnd[0] == imodel && s.modelStartEnd[1] == imodel) {
        structures[i] = null;
        n++;
      }
    }
    if (n > 0) {
      Structure[] ss = new Structure[structureCount - n];
      for (int i = 0, pt = 0; i < structureCount; i++)
        if (structures[i] != null)
          ss[pt++] = structures[i];
      structures = ss;
    }
  }

  public void removeLastUnselectedAtoms() {
    int n = ac;
    int nremoved = 0;
    int i0 = getLastAtomSetAtomIndex();
    int nnow = 0;
    for (int i = i0; i < n; i++) { 
      if (!bsAtoms.get(i)) {
        nremoved++;
        ac--;
        atoms[i] = null;
        continue;
      } 
      if (nremoved > 0) {
        atoms[atoms[i].index = i - nremoved] = atoms[i];
        atoms[i] = null;
      }
      nnow++;
    }
    atomSetAtomCounts[iSet] = nnow;
    if (nnow == 0) {
      iSet--;
      atomSetCount--;
    } else {
      bsAtoms.setBits(i0, i0 + nnow);
    }
  }

  public void checkNoEmptyModel() {
    while (atomSetCount > 0 && atomSetAtomCounts[atomSetCount - 1] == 0)
      atomSetCount--;
  }



}
