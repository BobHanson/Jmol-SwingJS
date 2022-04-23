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

package org.jmol.modelset;

import org.jmol.util.Elements;
import javajs.util.P3;
import javajs.util.P3d;

import org.jmol.util.BSUtil;
import org.jmol.util.Edge;
import org.jmol.util.JmolMolecule;
import org.jmol.util.Logger;
import javajs.util.V3;
import org.jmol.viewer.JC;
import org.jmol.script.T;
import org.jmol.viewer.Viewer;

import org.jmol.api.Interface;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolAdapterAtomIterator;
import org.jmol.api.JmolAdapterBondIterator;
import org.jmol.api.JmolDataManager;
import org.jmol.api.SymmetryInterface;
import org.jmol.atomdata.RadiusData;
import org.jmol.c.VDW;
import javajs.util.BS;

import org.jmol.modelsetbio.BioModel;
import org.jmol.modelsetbio.BioResolver;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.Quat;
import javajs.util.SB;

import java.util.Arrays;

import java.util.Hashtable;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/* 
 * 
 * This class contains only the private methods 
 * used to load a model. Methods exclusively after 
 * file loading are included only in ModelSet,
 * and its superclasses, ModelCollection, BondCollection, and AtomCollection.
 * 
 * Bob Hanson, 5/2007; refactored 7/2011
 *  
 */

public final class ModelLoader {
  
  private Viewer vwr;
  public ModelSet ms;
  private ModelSet modelSet0;

  private boolean merging;
  private boolean appendNew;

  private String jmolData; // from a PDB remark "Jmol PDB-encoded data"
  public String[] group3Lists;
  public int[][] group3Counts;
  public int[] specialAtomIndexes;
  
  public ModelLoader(Viewer vwr, String modelSetName,
      SB loadScript, Object asc, ModelSet modelSet0,
      BS bsNew) {
    this.vwr = vwr;
    ms = new ModelSet(vwr, modelSetName);
    JmolAdapter adapter = vwr.getModelAdapter();
    this.modelSet0 = modelSet0;
    merging = (modelSet0 != null && modelSet0.ac > 0);
    if (merging) {
      ms.canSkipLoad = false;
    } else {
      vwr.resetShapes(false);
    }
    ms.preserveState = vwr.getPreserveState();
    ms.showRebondTimes = vwr.getBoolean(T.showtiming);
    if (bsNew == null) {
      // zap
      initializeInfo(modelSetName, null);
      createModelSet(null, null, null);
      vwr.setStringProperty("_fileType", "");
      return;
    }    
    if (!ms.preserveState)
      ms.canSkipLoad = false;
    Map<String, Object> info = adapter.getAtomSetCollectionAuxiliaryInfo(asc);
    info.put("loadScript", loadScript);
    initializeInfo(adapter.getFileTypeName(asc).toLowerCase().intern(), info);
    createModelSet(adapter, asc, bsNew);
    if (jbr != null)
      jbr.setLoader(null);
    jbr = null;
    // dumpAtomSetNameDiagnostics(adapter, asc);
  }
/*
  private void dumpAtomSetNameDiagnostics(JmolAdapter adapter, Object asc) {
    int frameModelCount = modelCount;
    int adapterAtomSetCount = adapter.getAtomSetCount(asc);
    if (Logger.debugging) {
      Logger.debug(
          "----------------\n" + "debugging of AtomSetName stuff\n" +
          "\nframeModelCount=" + frameModelCount +
          "\nadapterAtomSetCount=" + adapterAtomSetCount + "\n -- \n");
      for (int i = 0; i < adapterAtomSetCount; ++i) {
        Logger.debug(
            "atomSetName[" + i + "]=" + adapter.getAtomSetName(asc, i) +
            " atomSetNumber[" + i + "]=" + adapter.getAtomSetNumber(asc, i));
      }
    }
  }
*/

  private boolean someModelsHaveUnitcells;
  private boolean someModelsAreModulated;
  private boolean is2D, isMOL2D, isMutate;
  public boolean isTrajectory; 
  private boolean isPyMOLsession;
  private boolean doMinimize;
  private boolean doAddHydrogens;

  private String fileHeader;
  private BioResolver jbr;
  public Group[] groups;
  private int groupCount;
  private P3 modulationTUV;
  private boolean highPrecision;
  private boolean isSupercell;
  
  

  @SuppressWarnings("unchecked")
  private void initializeInfo(String name, Map<String, Object> info) {
    ms.g3d = vwr.gdata;
    //long timeBegin = System.currentTimeMillis();
    ms.modelSetTypeName = name;
    ms.isXYZ = (name == "xyz");
    ms.msInfo = info;
    ms.modelSetProperties = (Properties) ms.getInfoM("properties");
    //isMultiFile = getModelSetAuxiliaryInfoBoolean("isMultiFile"); -- no longer necessary
    ms.haveBioModels = ms.getMSInfoB(JC.getBoolName(JC.GLOBAL_ISPDB));
    isMutate = ms.getMSInfoB("isMutate");
    if (ms.haveBioModels)
      jbr = vwr.getJBR().setLoader(this);
    jmolData = (adapterModelCount == 0 ? (String) ms.getInfoM("jmolData") : null);
    fileHeader = (String) ms.getInfoM("fileHeader");
    Lst<P3[]> steps = (Lst<P3[]>) ms.getInfoM("trajectorySteps");
    isTrajectory = (steps != null);
    highPrecision = false;
    if (isTrajectory)
      ms.trajectory = newTrajectory(ms, steps);
    isPyMOLsession = ms.getMSInfoB("isPyMOL");
    doAddHydrogens = (jbr != null && !isTrajectory && !isPyMOLsession
        && !ms.getMSInfoB("pdbNoHydrogens") && (ms
        .getMSInfoB("pdbAddHydrogens") || vwr.getBoolean(T.pdbaddhydrogens)));
    if (info != null) {
      info.remove("pdbNoHydrogens");
      info.remove("pdbAddHydrogens");
      info.remove("trajectorySteps");
      if (isTrajectory)
        ms.vibrationSteps = (Lst<V3[]>) info.remove("vibrationSteps");
      highPrecision = info.containsKey("highPrecision");
      if (highPrecision) {
        // we must RESET this, because 'ZAP' has unset it in the script
        vwr.setBooleanProperty("legacyJavaFloat", true);
      }
    }
    htGroup1 = (Map<String, String>) ms.getInfoM("htGroup1");
    Object mod = ms.getInfoM("modulationOn");
    if (mod != null) {
      modulationOn = true;
      modulationTUV = (mod == Boolean.TRUE ? null : (P3) mod);
    }
    noAutoBond = ms.getMSInfoB("noAutoBond");
    is2D = ms.getMSInfoB("is2D");
    doMinimize = (is2D || ms.getMSInfoB("minimize3D")) && ms.getMSInfoB("doMinimize");
    adapterTrajectoryCount = (isTrajectory ? ms.trajectory.steps.size() : 0);
    ms.someModelsHaveSymmetry = ms.getMSInfoB(JC.getBoolName(JC.GLOBAL_SYMMETRY));
    someModelsHaveUnitcells = ms.getMSInfoB(JC.getBoolName(JC.GLOBAL_UNITCELLS));
    someModelsAreModulated = ms.getMSInfoB(JC.getBoolName(JC.GLOBAL_MODULATED));
    isSupercell = ms.getMSInfoB(JC.getBoolName(JC.GLOBAL_SUPERCELL));
    ms.someModelsHaveFractionalCoordinates = ms
        .getMSInfoB(JC.getBoolName(JC.GLOBAL_SYMMETRY));
    if (merging) {
      ms.haveBioModels |= modelSet0.haveBioModels;
      ms.bioModelset = modelSet0.bioModelset;
      if (ms.bioModelset != null)
        ms.bioModelset.set(vwr, ms);
      ms.someModelsHaveSymmetry |= modelSet0
          .getMSInfoB(JC.getBoolName(JC.GLOBAL_SYMMETRY));
      someModelsHaveUnitcells |= modelSet0
          .getMSInfoB(JC.getBoolName(JC.GLOBAL_UNITCELLS));
      ms.someModelsHaveFractionalCoordinates |= modelSet0
          .getMSInfoB(JC.getBoolName(JC.GLOBAL_FRACTCOORD));
      ms.someModelsHaveAromaticBonds |= modelSet0.someModelsHaveAromaticBonds;
      ms.msInfo.put(JC.getBoolName(JC.GLOBAL_SYMMETRY),
          Boolean.valueOf(ms.someModelsHaveSymmetry));
      ms.msInfo.put(JC.getBoolName(JC.GLOBAL_UNITCELLS),
          Boolean.valueOf(someModelsHaveUnitcells));
      ms.msInfo.put(JC.getBoolName(JC.GLOBAL_FRACTCOORD),
          Boolean.valueOf(ms.someModelsHaveFractionalCoordinates));
      ms.msInfo.put(JC.getBoolName(JC.GLOBAL_AROMATICBONDS),
          Boolean.valueOf(ms.someModelsHaveAromaticBonds));
    }
  }

  private Trajectory newTrajectory(ModelSet ms, Lst<P3[]> steps) {
    return ((Trajectory) Interface.getInterface("org.jmol.modelset.Trajectory", vwr, "load")).set(vwr, ms, steps);
  }

  private final Map<Object, Atom> htAtomMap = new Hashtable<Object, Atom>();

  private final static int defaultGroupCount = 32;
  private Chain[] chainOf;

  private String[] group3Of;
  public String getGroup3(int iGroup) {
    return (iGroup >= group3Of.length ? null : group3Of[iGroup]);
  }

  private int[] seqcodes;
  private int[] firstAtomIndexes;

  public int getFirstAtomIndex(int iGroup) {
    return firstAtomIndexes[iGroup];
  }
  
  private int iModel;
  private Model model;
  private int currentChainID;
  private boolean isNewChain;
  private Chain currentChain;
  private int currentGroupSequenceNumber;
  private char currentGroupInsertionCode = '\0';
  private String currentGroup3;

  private Group nullGroup; // used in Atom

  public int baseModelIndex = 0;
  private int baseModelCount = 0;
  public int baseAtomIndex = 0;
  public int baseGroupIndex = 0;

  private int baseTrajectoryCount = 0;
  private int adapterModelCount = 0;
  private int adapterTrajectoryCount = 0;
  private boolean noAutoBond;
  private boolean modulationOn;
  
  private Map<String, String> htGroup1; // from mmCIF and PDB
  private Integer appendToModelIndex;

  
  public int getAtomCount() {
    return ms.ac;
  }

  private void createModelSet(JmolAdapter adapter, Object asc,
                              BS bsNew) {
    int nAtoms = (adapter == null ? 0 : adapter.getAtomCount(asc));
    if (nAtoms > 0)
      Logger.info("reading " + nAtoms + " atoms");
    adapterModelCount = (adapter == null ? 1 : adapter
        .getAtomSetCount(asc));
    // cannot append a trajectory into a previous model
    appendToModelIndex = (ms.msInfo == null ? null : ((Integer) ms.msInfo.get("appendToModelIndex")));
    appendNew = !isMutate && (!merging || adapter == null || adapterModelCount > 1
        || isTrajectory || vwr.getBoolean(T.appendnew) && appendToModelIndex == null);
    htAtomMap.clear();
    chainOf = new Chain[defaultGroupCount];
    group3Of = new String[defaultGroupCount];
    seqcodes = new int[defaultGroupCount];
    firstAtomIndexes = new int[defaultGroupCount];
    currentChainID = Integer.MAX_VALUE;
    currentChain = null;
    currentGroupInsertionCode = '\uFFFF';
    currentGroup3 = "xxxxx";
    iModel = -1;
    model = null;
    if (merging)
      mergeTrajAndVib(modelSet0, ms);
    initializeAtomBondModelCounts(nAtoms);
    if (bsNew != null && (doMinimize || is2D)) {
      bsNew.setBits(baseAtomIndex, baseAtomIndex + nAtoms);
    }
    if (adapter == null) {
      setModelNameNumberProperties(0, -1, "", 1, null, null, null);
    } else {
      if (adapterModelCount > 0) {
        Logger.info("ModelSet: haveSymmetry:" + ms.someModelsHaveSymmetry
            + " haveUnitcells:" + someModelsHaveUnitcells
            + " haveFractionalCoord:" + ms.someModelsHaveFractionalCoordinates);
        Logger.info(adapterModelCount + " model" + (ms.mc == 1 ? "" : "s")
            + " in this collection. Use getProperty \"modelInfo\" or"
            + " getProperty \"auxiliaryInfo\" to inspect them.");
      }
      Quat q = (Quat) ms.getInfoM("defaultOrientationQuaternion");
      if (q != null) {
        Logger.info("defaultOrientationQuaternion = " + q);
        Logger
            .info("Use \"set autoLoadOrientation TRUE\" before loading or \"restore orientation DEFAULT\" after loading to view this orientation.");
      }
      iterateOverAllNewModels(adapter, asc);
      JmolAdapterBondIterator iterBond = adapter.getBondIterator(asc);
      boolean haveBonds = (iterBond != null);
      iterateOverAllNewAtoms(adapter, asc, haveBonds);
      short mad = vwr.getMadBond();
      if (haveBonds) {
        ms.defaultCovalentMad = (jmolData == null ? mad : 0);
        iterateOverAllNewBonds(iterBond);
      }
      ms.defaultCovalentMad = mad;
      if (merging && !appendNew) {
        Map<String, Object> info = adapter.getAtomSetAuxiliaryInfo(
            asc, 0);
        ms.setInfo(baseModelIndex, "initialAtomCount", info
            .get("initialAtomCount"));
        ms.setInfo(baseModelIndex, "initialBondCount", info
            .get("initialBondCount"));
      }
      initializeUnitCellAndSymmetry();
      initializeBonding();
    }

    finalizeGroupBuild(); // set group offsets and build monomers

    // only now can we access all of the atom's properties

    if (is2D && doMinimize) {
      applyStereochemistry();
    }

    if (doAddHydrogens)
      jbr.finalizeHydrogens();

    if (adapter != null) {
      ms.calculatePolymers(groups, groupCount, baseGroupIndex, null);
      if (jbr != null)
        jbr.iterateOverAllNewStructures(adapter, asc);
    }

    
    setDefaultRendering(vwr.getInt(T.smallmoleculemaxatoms));

    RadiusData rd = vwr.rd;
    int ac = ms.ac;
    Atom[] atoms = ms.at;
    for (int i = baseAtomIndex; i < ac; i++)
      atoms[i].setMadAtom(vwr, rd);
    Model[] models = ms.am;
    for (int i = models[baseModelIndex].firstAtomIndex; i < ac; i++)
      if (atoms[i] != null)
        models[atoms[i].mi].bsAtoms.set(i);

    freeze();
    finalizeShapes();
    vwr.setModelSet(ms);
    if (isSupercell && appendNew) {
      for (int i = baseModelIndex; i < ms.mc; i++) {
        vwr.assignSpaceGroup(null, "P1", i);
      }
    }
    setAtomProperties();
    if (adapter != null)
      adapter.finish(asc);    
    if (modelSet0 != null) {
      modelSet0.releaseModelSet();
    }
    modelSet0 = null;
  }

  private void mergeTrajAndVib(ModelSet oldSet, ModelSet newSet) {
    // merge is the OLD set
    baseModelCount = oldSet.mc;
    baseTrajectoryCount = 0;
    if (oldSet.trajectory == null) {
      if (isTrajectory)
        newTrajectory(oldSet, new Lst<P3[]>());
    }
    if (oldSet.trajectory == null || oldSet.mc == 0)
      return;
    baseTrajectoryCount = oldSet.mc;
    int  n = oldSet.trajectory.steps.size();
    for (int i = n; i < baseTrajectoryCount; i++)
      oldSet.trajectory.steps.addLast(null);
    if (isTrajectory) {
      if (oldSet.vibrationSteps == null) {
        oldSet.vibrationSteps = new Lst<V3[]>();
        for (int i = n; --i >= 0;)
          oldSet.vibrationSteps.addLast(null);
      }
      n = newSet.trajectory.steps.size();
      for (int i = 0; i < n; i++) {
        oldSet.trajectory.steps.addLast(newSet.trajectory.steps.get(i));
        oldSet.vibrationSteps.addLast(newSet.vibrationSteps == null ? null
            : newSet.vibrationSteps.get(i));
      }
    } else {
      newSet.trajectory = newTrajectory(newSet, null);
    }
    newSet.vibrationSteps = oldSet.vibrationSteps;
    newSet.trajectory.steps = oldSet.trajectory.steps;
    oldSet.trajectory = null;
  }

  private void setDefaultRendering(int maxAtoms) {
    if (isPyMOLsession)
      return;
    SB sb = new SB();
    int modelCount = ms.mc;
    Model[] models = ms.am;
    for (int i = baseModelIndex; i < modelCount; i++)
      if (models[i].isBioModel)
        ((BioModel) models[i]).getDefaultLargePDBRendering(sb, maxAtoms);
    if (sb.length() == 0)
      return;
    sb.append("select *;");
    String script = (String) ms.getInfoM("jmolscript");
    if (script == null)
      script = "";
    sb.append(script);
    ms.msInfo.put("jmolscript", sb.toString());
  }

  @SuppressWarnings("unchecked")
  private void setAtomProperties() {
    // Crystal reader, PDB tlsGroup
    // assumes String line-encoded float[] or just float[] values
    int modelCount = ms.mc;
    for (int i = baseModelIndex; i < modelCount; i++) {
      Map<String, Object> atomProperties = (Map<String, Object>) ms.getInfo(i,
          "atomProperties");
      // list of properties that are to be transfered to H atoms as well.
      if (jmolData != null)
        addJmolDataProperties(ms.am[i], (Map<String, float[]>) ms.getInfo(i, "jmolDataProperties"));
      String groupList = (String) ms.getInfo(i,
          "groupPropertyList");
      if (ms.am[i].isBioModel && ms.getInfo(i, "dssr") != null)
        vwr.getAnnotationParser(true).setGroup1(ms, i);
      if (atomProperties == null)
        continue;
      for (Map.Entry<String, Object> entry : atomProperties.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();
        // no deletions yet...
        BS bs = ms.getModelAtomBitSetIncludingDeleted(i, true);
        if (doAddHydrogens) {
          boolean isGroup = (groupList != null && PT.isOneOf(key,  groupList));
          value = jbr.fixPropertyValue(bs, value, isGroup);
        }
        key = "property_" + key.toLowerCase();
        Logger.info("creating " + key + " for model " + ms.getModelName(i));
        vwr.setData(key, new Object[] { key, value, bs, Integer.valueOf(JmolDataManager.DATA_TYPE_UNKNOWN), Boolean.FALSE }, 
            ms.ac, 0, 0, Integer.MAX_VALUE, 0);
      }
    }
  }

  private Group[] mergeGroups;
  private int iChain;
  
  private void initializeAtomBondModelCounts(int nAtoms) {
    int trajectoryCount = adapterTrajectoryCount;
    if (merging) {
      if (appendNew) {
        baseModelIndex = baseModelCount;
        ms.mc = baseModelCount + adapterModelCount;
      } else {
        baseModelIndex = (appendToModelIndex == null ? vwr.am.cmi : appendToModelIndex.intValue());
        if (baseModelIndex < 0 || baseModelIndex >= baseModelCount)
          baseModelIndex = baseModelCount - 1;
        ms.mc = baseModelCount;
      }
      ms.ac = baseAtomIndex = modelSet0.ac;
      ms.bondCount = modelSet0.bondCount;
      mergeGroups = modelSet0.getGroups();
      groupCount = baseGroupIndex = mergeGroups.length;
      ms.mergeModelArrays(modelSet0);
      ms.growAtomArrays(ms.ac + nAtoms);
    } else {
      ms.mc = adapterModelCount;
      ms.ac = 0;
      ms.bondCount = 0;
      ms.at = new Atom[nAtoms];
      ms.bo = new Bond[250 + nAtoms]; // was "2 *" -- WAY overkill.
    }
    if (doAddHydrogens)
      jbr.initializeHydrogenAddition();
    if (trajectoryCount > 1)
      ms.mc += trajectoryCount - 1;
    ms.am = (Model[]) AU.arrayCopyObject(ms.am, ms.mc);
    ms.modelFileNumbers = AU.arrayCopyI(ms.modelFileNumbers, ms.mc);
    ms.modelNumbers = AU.arrayCopyI(ms.modelNumbers, ms.mc);
    ms.modelNumbersForAtomLabel = AU.arrayCopyS(ms.modelNumbersForAtomLabel, ms.mc);
    ms.modelNames = AU.arrayCopyS(ms.modelNames, ms.mc);
    ms.frameTitles = AU.arrayCopyS(ms.frameTitles, ms.mc);
    if (merging)
      for (int i = 0; i < modelSet0.mc; i++)
        (ms.am[i] = modelSet0.am[i]).ms = ms;
  }

  private void mergeGroups() {
    Map<String, Object> info = modelSet0.getAuxiliaryInfo(null);
    String[] mergeGroup3Lists = (String[]) info.get("group3Lists");
    int[][] mergeGroup3Counts = (int[][]) info.get("group3Counts");
    if (mergeGroup3Lists != null) {
      for (int i = 0; i < baseModelCount; i++) {
        group3Lists[i + 1] = mergeGroup3Lists[i + 1];
        group3Counts[i + 1] = mergeGroup3Counts[i + 1];
        structuresDefinedInFile.set(i);
      }
      group3Lists[0] = mergeGroup3Lists[0];
      group3Counts[0] = mergeGroup3Counts[0];
    }
    //if merging PDB data into an already-present model, and the 
    //structure is defined, consider the current structures in that 
    //model to be undefined. Not guaranteed to work.
    if (!appendNew && ms.haveBioModels) 
      structuresDefinedInFile.clear(baseModelIndex);
  }

  private void iterateOverAllNewModels(JmolAdapter adapter, Object asc) {

    // set private values

    group3Lists = new String[ms.mc + 1];
    group3Counts = AU.newInt2(ms.mc + 1);

    structuresDefinedInFile = new BS();

    if (merging)
      mergeGroups();

    int iTrajectory = (isTrajectory ? baseTrajectoryCount : -1);
    int ipt = baseModelIndex;
    for (int i = 0; i < adapterModelCount; ++i, ++ipt) {
      int modelNumber = adapter.getAtomSetNumber(asc, i);
      String modelName = adapter.getAtomSetName(asc, i);
      Map<String, Object> modelAuxiliaryInfo = adapter.getAtomSetAuxiliaryInfo(
          asc, i);
      if (modelAuxiliaryInfo.containsKey("modelID"))
        modelAuxiliaryInfo.put("modelID0", modelAuxiliaryInfo.get("modelID"));
      Properties modelProperties = (Properties) modelAuxiliaryInfo.get("modelProperties");
      String ftype = (String) modelAuxiliaryInfo.get("fileType");
      vwr.setStringProperty("_fileType", ftype);
      vwr.fm.setFileType(ftype);
      if (modelName == null)
        modelName = (jmolData != null && jmolData.indexOf(";") > 2 ? jmolData.substring(jmolData
            .indexOf(":") + 2, jmolData.indexOf(";"))
            : appendNew ? "" + (modelNumber % 1000000): "");
      setModelNameNumberProperties(ipt, iTrajectory,
          modelName, modelNumber, modelProperties, modelAuxiliaryInfo,
          jmolData);
      //if (ms.getInfo(ipt, "periodicOriginXyz") != null)
        //ms.someModelsHaveSymmetry = true;
    }
    Model m = ms.am[appendToModelIndex == null ? baseModelIndex : ms.mc - 1];
    vwr.setSmilesString((String) ms.msInfo.get("smilesString"));
    String loadState = (String) ms.msInfo.remove("loadState");
    SB loadScript = (SB)ms.msInfo.remove("loadScript");
    if (loadScript.indexOf("Viewer.AddHydrogens") < 0 || !m.isModelKit) {
      String[] lines = PT.split(loadState, "\n");
      SB sb = new SB();
      for (int i = 0; i < lines.length; i++) {
        int pt = m.loadState.indexOf(lines[i]);
        if (pt < 0 || pt != m.loadState.lastIndexOf(lines[i]))
          sb.append(lines[i]).appendC('\n');
      }
      m.loadState += m.loadScript.toString() + sb.toString();
      m.loadScript = new SB();
      if (loadScript.indexOf("load append ") >= 0 || loadScript.indexOf("data \"append ") >= 0) {
        loadScript.insert(0,  ";var anew = appendNew;");
        loadScript.append(";set appendNew anew");
      }
      m.loadScript.append("  ").appendSB(loadScript).append(";\n");
    }
    if (isTrajectory) {
      // fill in the rest of the data
      int n = (ms.mc - ipt + 1);
      Logger.info(n + " trajectory steps read");
      ms.setInfo(baseModelCount, "trajectoryStepCount", Integer.valueOf(n));
      for (int ia = adapterModelCount, i = ipt; i < ms.mc; i++, ia++) {
        ms.am[i] = ms.am[baseModelCount];
        ms.modelNumbers[i] = adapter.getAtomSetNumber(asc, ia);
        ms.modelNames[i] = adapter.getAtomSetName(asc, ia);
        structuresDefinedInFile.set(i);
      }
    }
    finalizeModels(baseModelCount);
  }
    
  private void setModelNameNumberProperties(int modelIndex,
                                            int trajectoryBaseIndex,
                                            String modelName,
                                            int modelNumber,
                                            Properties modelProperties,
                                            Map<String, Object> modelAuxiliaryInfo,
                                            String jmolData) {
    if (appendNew) {
      boolean modelIsPDB = (modelAuxiliaryInfo != null && Boolean.TRUE == modelAuxiliaryInfo
          .get(JC.getBoolName(JC.GLOBAL_ISPDB)));
      ms.am[modelIndex] = (modelIsPDB ? jbr.getBioModel(modelIndex,
          trajectoryBaseIndex, jmolData, modelProperties, modelAuxiliaryInfo)
          : new Model().set(ms, modelIndex, trajectoryBaseIndex, jmolData,
              modelProperties, modelAuxiliaryInfo));
      ms.modelNumbers[modelIndex] = modelNumber;
      ms.modelNames[modelIndex] = modelName;
      if (modelIsPDB)
        jbr.setGroupLists(modelIndex);
    } else {
      Object atomInfo = modelAuxiliaryInfo
          .get("PDB_CONECT_firstAtom_count_max");
      if (atomInfo != null)
        ms.setInfo(modelIndex, "PDB_CONECT_firstAtom_count_max", atomInfo);
    }
    Model[] models = ms.am;
    Atom[] atoms = ms.at;
    // this next sets the bitset length to avoid 
    // unnecessary calls to System.arrayCopy

    models[modelIndex].bsAtoms.set(atoms.length + 1);
    models[modelIndex].bsAtoms.clear(atoms.length + 1);
    String codes = (String) ms.getInfo(modelIndex, "altLocs");
    models[modelIndex].altLocCount = (codes == null ? 0 : codes.length());
    if (codes != null) {
      char[] altlocs = codes.toCharArray();
      Arrays.sort(altlocs);
      codes = String.valueOf(altlocs);
      ms.setInfo(modelIndex, "altLocs", codes);
    }
    codes = (String) ms.getInfo(modelIndex, "insertionCodes");
    models[modelIndex].insertionCount = (codes == null ? 0 : codes.length());
    boolean isModelKit = (ms.modelSetName != null
        && ms.modelSetName.startsWith("Jmol Model Kit")
        || modelName.startsWith("Jmol Model Kit") || "Jme".equals(ms.getInfo(
        modelIndex, "fileType")));
    models[modelIndex].isModelKit = isModelKit;
  }

  /**
   * Model numbers are considerably more complicated in Jmol 11.
   * 
   * int modelNumber
   * 
   * The adapter gives us a modelNumber, but that is not necessarily what the
   * user accesses. If a single files is loaded this is:
   * 
   * a) single file context:
   * 
   * 1) the sequential number of the model in the file , or 2) if a PDB file and
   * "MODEL" record is present, that model number
   * 
   * b) multifile context:
   * 
   * always 1000000 * (fileIndex + 1) + (modelIndexInFile + 1)
   * 
   * 
   * int fileIndex
   * 
   * The 0-based reference to the file containing this model. Used when doing
   * "_modelnumber3.2" in a multifile context
   * 
   * int modelFileNumber
   * 
   * An integer coding both the file and the model:
   * 
   * file * 1000000 + modelInFile (1-based)
   * 
   * Used all over the place. Note that if there is only one file, then
   * modelFileNumber < 1000000.
   * 
   * String modelNumberDotted
   * 
   * A number the user can use "1.3"
   * 
   * String modelNumberForAtomLabel
   * 
   * Either the dotted number or the PDB MODEL number, if there is only one file
   * 
   * @param baseModelCount
   * 
   */
  private void finalizeModels(int baseModelCount) {
    int modelCount = ms.mc;
    if (modelCount == baseModelCount)
      return;
    String sNum;
    int modelnumber = 0;
    int lastfilenumber = -1;
    int[] modelNumbers = ms.modelNumbers;
    String[] modelNames = ms.modelNames;
    if (isTrajectory)
      for (int i = baseModelCount; ++i < ms.mc;)
        modelNumbers[i] = modelNumbers[i - 1] + 1;
    if (baseModelCount > 0) {
      // load append
      if (modelNumbers[0] < 1000000) {
        // initially we had just one file
        for (int i = 0; i < baseModelCount; i++) {
          // create 1000000 model numbers for the original file models
          if (modelNames[i].length() == 0)
            modelNames[i] = "" + modelNumbers[i];
          modelNumbers[i] += 1000000;
          ms.modelNumbersForAtomLabel[i] = "1." + (i + 1);
        }
      }
      // update file number
      int filenumber = modelNumbers[baseModelCount - 1];
      filenumber -= filenumber % 1000000;
      if (modelNumbers[baseModelCount] < 1000000)
        filenumber += 1000000;
      for (int i = baseModelCount; i < modelCount; i++)
        modelNumbers[i] += filenumber;
    }
    Model[] models = ms.am;
    for (int i = baseModelCount; i < modelCount; ++i) {
      ms.setInfo(i, "fileType", ms.modelSetTypeName);
      if (fileHeader != null)
        ms.setInfo(i, "fileHeader", fileHeader);
      int filenumber = modelNumbers[i] / 1000000;
      if (filenumber != lastfilenumber) {
        modelnumber = 0;
        lastfilenumber = filenumber;
      }
      modelnumber++;
      if (filenumber == 0) {
        // only one file -- take the PDB number or sequential number as given by adapter
        sNum = "" + ms.getModelNumber(i);
        filenumber = 1;
      } else {
        sNum = filenumber + "." + modelnumber;
      }
      ms.modelNumbersForAtomLabel[i] = sNum;
      models[i].fileIndex = filenumber - 1;
      ms.modelFileNumbers[i] = filenumber * 1000000 + modelnumber;
      if (modelNames[i] == null || modelNames[i].length() == 0)
        modelNames[i] = sNum;
    }
    if (merging)
      for (int i = 0; i < baseModelCount; i++)
        models[i].ms = ms;

    // this won't do in the case of trajectories
    for (int i = 0; i < modelCount; i++) {
      ms.setInfo(i, "modelName", modelNames[i]);
      ms.setInfo(i, "modelNumber", Integer
          .valueOf(modelNumbers[i] % 1000000));
      ms.setInfo(i, "modelFileNumber", Integer
          .valueOf(ms.modelFileNumbers[i]));
      ms.setInfo(i, "modelNumberDotted", ms
          .getModelNumberDotted(i));
      String codes = (String) ms.getInfo(i, "altLocs");
      if (codes != null) {
        Logger.info("model " + ms.getModelNumberDotted(i)
            + " alternative locations: " + codes);
      }
    }
  }

  private void iterateOverAllNewAtoms(JmolAdapter adapter, Object asc, boolean haveBonds) {
    // atom is created, but not all methods are safe, because it
    // has no group -- this is only an issue for debugging
    int iLast = -1;
    boolean isPdbThisModel = false;
    boolean addH = false;
    boolean isLegacyHAddition = false;//vwr.getBoolean(T.legacyhaddition);
    JmolAdapterAtomIterator iterAtom = adapter.getAtomIterator(asc);
    int nAtoms = adapter.getAtomCount(asc);
    ms.setCapacity(adapter.getAtomCount(asc));
    int nRead = 0;
    Model[] models = ms.am;
    if (ms.mc > 0)
      nullGroup = new Group().setGroup(new Chain(ms.am[baseModelIndex], 32, 0),
          "", 0, -1, -1);
    P3d xyz = new P3d();
    while (iterAtom.hasNext()) {
      nRead++;
      int modelIndex = iterAtom.getAtomSetIndex() + baseModelIndex;
      if (modelIndex != iLast) {
        iChain = 0;
        iModel = modelIndex;
        model = models[modelIndex];
        currentChainID = Integer.MAX_VALUE;
        isNewChain = true;
        // set the internal array for model.bsAtoms to be large enough
        model.bsAtoms.set(ms.ac + nAtoms);
        model.bsAtoms.clearAll();
        model.isOrderly = (appendToModelIndex == null);
        isPdbThisModel = model.isBioModel;
        iLast = modelIndex;
        addH = isPdbThisModel && doAddHydrogens;
        if (jbr != null)
          jbr.setHaveHsAlready(false);
      }
      String group3 = iterAtom.getGroup3();
      int chainID = iterAtom.getChainID();
      checkNewGroup(adapter, chainID, group3, iterAtom.getSequenceNumber(),
          iterAtom.getInsertionCode(), addH, isLegacyHAddition);
      int isotope = iterAtom.getElementNumber();
      if (addH && Elements.getElementNumber(isotope) == 1)
        jbr.setHaveHsAlready(true);
      String name = iterAtom.getAtomName();
      int charge = (addH ? getPdbCharge(group3, name) : iterAtom.getFormalCharge());
      xyz = iterAtom.getXYZ();
      Atom atom = addAtom(isPdbThisModel, iterAtom.getSymmetry(),
          iterAtom.getAtomSite(),
          isotope,
          name,
          charge, 
          iterAtom.getPartialCharge(),
          iterAtom.getTensors(), 
          iterAtom.getOccupancy(), 
          iterAtom.getBfactor(), 
          xyz.toP3(),
          highPrecision ? xyz : null,
          iterAtom.getIsHetero(), 
          iterAtom.getSerial(), 
          iterAtom.getSeqID(),
          group3,
          iterAtom.getVib(), 
          iterAtom.getAltLoc(),
          iterAtom.getRadius(), 
          iterAtom.getBondRadius()
          );
      if (haveBonds)
        htAtomMap.put(iterAtom.getUniqueID(), atom);
    }
    if (groupCount > 0 && addH) {
      jbr.addImplicitHydrogenAtoms(adapter, groupCount - 1,
          isNewChain && !isLegacyHAddition ? 1 : 0);
    }
    iLast = -1;
    VDW vdwtypeLast = null;
    Atom[] atoms = ms.at;
    models[0].firstAtomIndex = 0;
    for (int i = 0; i < ms.ac; i++) {
      if (atoms[i] != null && atoms[i].mi > iLast) {
        iLast = atoms[i].mi;
        Model m = models[iLast];
        m.firstAtomIndex = i;
        m.isOrderly = (m.act == m.bsAtoms.length()); 
        VDW vdwtype = ms.getDefaultVdwType(iLast);
        if (vdwtype != vdwtypeLast) {
          Logger.info("Default Van der Waals type for model" + " set to "
              + vdwtype.getVdwLabel());
          vdwtypeLast = vdwtype;
        }
      }
    }
    Logger.info(nRead + " atoms created");
  }

  private void addJmolDataProperties(Model m,
                                     Map<String, float[]> jmolDataProperties) {
    if (jmolDataProperties == null)
      return;
    BS bs = m.bsAtoms;
    int nAtoms = bs.cardinality();
    for (Entry<String, float[]> e : jmolDataProperties.entrySet()) {
      String key = e.getKey();
      float[] data = e.getValue();
      if (data.length != nAtoms)
        return;
      int tok = (key.startsWith("property_") ? T.property : T
          .getTokFromName(key));
      switch (tok) {
      default:
        if (T.tokAttr(tok, T.settable)) {
          vwr.setAtomProperty(bs, tok, 0, 0, null, data, null);
          break;
        }
        // not settable -- must encode as property_
        //$FALL-THROUGH$
      case T.x:
      case T.y:
      case T.z:
        key = "property_" + key;
        tok = T.property;
        //$FALL-THROUGH$
      case T.property:
        // must create data set
        vwr.setData(
            key,
            new Object[] { key, data, bs,
                Integer.valueOf(JmolDataManager.DATA_TYPE_AF) }, 0, 0, 0, 0, 0);
      }
    }
  }

  /**
   * Adjust known N and O atom formal charges.
   * Note that this does not take care of ligands.
   * 
   * @param group3
   * @param name
   * @return 0, 1, or -1
   */
  private int getPdbCharge(String group3, String name) {
    return (group3.equals("ARG") && name.equals("NH1")
        || group3.equals("LYS") && name.equals("NZ")
        || group3.equals("HIS") && name.equals("ND1") ? 1 
//            : name.equals("OXT") || group3.equals("GLU") && name.equals("OE2")
 //       || group3.equals("ASP") && name.equals("OD2") ? -1
            : 0);
  }

  private Atom addAtom(boolean isPDB, BS atomSymmetry, int atomSite, int atomicAndIsotopeNumber,
                       String atomName, int formalCharge, float partialCharge,
                       Lst<Object> tensors, float occupancy, float bfactor,
                       P3 xyz, P3d dxyz, boolean isHetero, int atomSerial, int atomSeqID,
                       String group3, V3 vib, char alternateLocationID,
                       float radius, float bondRadius) {
    byte specialAtomID = 0;
    String atomType = null;
    if (atomName != null) {
      // Q: Why were we looking up special atom names if it was not a PDB model?
      int i;
      if ((i = atomName.indexOf('\0')) >= 0) {
        atomType = atomName.substring(i + 1);
        atomName = atomName.substring(0, i);
      }
      if (isPDB) {
        if (atomName.indexOf('*') >= 0)
          atomName = atomName.replace('*', '\'');
        specialAtomID = vwr.getJBR().lookupSpecialAtomID(atomName);
        if (specialAtomID == JC.ATOMID_ALPHA_CARBON
            && "CA".equalsIgnoreCase(group3)) // calcium
          specialAtomID = 0;
      }
    }
    Atom atom = ms.addAtom(iModel, nullGroup, atomicAndIsotopeNumber, atomName,
        atomType, atomSerial, atomSeqID, atomSite, xyz, dxyz, radius, vib,
        formalCharge, partialCharge, occupancy, bfactor, tensors, isHetero,
        specialAtomID, atomSymmetry, bondRadius);
    atom.altloc = alternateLocationID;
    return atom;
  }

  private void checkNewGroup(JmolAdapter adapter, int chainID,
                             String group3, int groupSequenceNumber,
                             char groupInsertionCode, boolean addH, boolean isLegacyHAddition) {
    String group3i = (group3 == null ? null : group3.intern());
    if (chainID != currentChainID) {
      currentChainID = chainID;
      currentChain = getOrAllocateChain(model, chainID);
      currentGroupInsertionCode = '\uFFFF';
      currentGroupSequenceNumber = -1;
      currentGroup3 = "xxxx";
      isNewChain = true;
    }
    if (groupSequenceNumber != currentGroupSequenceNumber
        || groupInsertionCode != currentGroupInsertionCode
        || group3i != currentGroup3) {
      if (groupCount > 0 && addH) {
        jbr.addImplicitHydrogenAtoms(adapter, groupCount - 1, isNewChain && !isLegacyHAddition? 1 : 0);
        jbr.setHaveHsAlready(false);
      }
      currentGroupSequenceNumber = groupSequenceNumber;
      currentGroupInsertionCode = groupInsertionCode;
      currentGroup3 = group3i;
      while (groupCount >= group3Of.length) {
        chainOf = (Chain[]) AU.doubleLength(chainOf);
        group3Of = AU.doubleLengthS(group3Of);
        seqcodes = AU.doubleLengthI(seqcodes);
        firstAtomIndexes = AU.doubleLengthI(firstAtomIndexes);
      }
      firstAtomIndexes[groupCount] = ms.ac;
      chainOf[groupCount] = currentChain;
      group3Of[groupCount] = group3;
      seqcodes[groupCount] = Group.getSeqcodeFor(groupSequenceNumber,
          groupInsertionCode);
      ++groupCount;      
    }
  }

  private Chain getOrAllocateChain(Model model, int chainID) {
    //Logger.debug("chainID=" + chainID + " -> " + (chainID + 0));
    Chain chain = model.getChain(chainID);
    if (chain != null)
      return chain;
    if (model.chainCount == model.chains.length)
      model.chains = (Chain[])AU.doubleLength(model.chains);
    return model.chains[model.chainCount++] = new Chain(model, chainID, (chainID == 0 || chainID == 32 ? 0 : ++iChain));
  }

  private void iterateOverAllNewBonds(JmolAdapterBondIterator iterBond) {
    boolean force1 = isMutate && !vwr.getBoolean(T.pdbaddhydrogens);
    boolean haveMultipleBonds = false;
    while (iterBond.hasNext()) {
      int iOrder = iterBond.getEncodedOrder();
      short order = (force1 ? 1 : (short) iOrder);
      Bond b = bondAtoms(iterBond.getAtomUniqueID1(),
          iterBond.getAtomUniqueID2(), order);
      if (b != null) {
        if (order > 1 && order != Edge.BOND_STEREO_NEAR
            && order != Edge.BOND_STEREO_FAR)
          haveMultipleBonds = true;
        float radius = iterBond.getRadius();
        if (radius > 0)
          b.setMad((short) (radius * 2000));
        short colix = iterBond.getColix();
        if (colix >= 0)
          b.colix = colix;
        b.order |= (iOrder & Edge.BOND_PYMOL_MULT);
      }
    }
    if (haveMultipleBonds && ms.someModelsHaveSymmetry
        && !vwr.getBoolean(T.applysymmetrytobonds))
      Logger.info(
          "ModelSet: use \"set appletSymmetryToBonds TRUE \" to apply the file-based multiple bonds to symmetry-generated atoms.");
  }
  
  private Lst<Bond> vStereo;
  
  private int lastModel = -1;
  
  private Bond bondAtoms(Object atomUid1, Object atomUid2, short order) {
    Atom atom1 = htAtomMap.get(atomUid1);
    if (atom1 == null) {
      Logger.error("bondAtoms cannot find atomUid1?:" + atomUid1);
      return null;
    }
    Atom atom2 = htAtomMap.get(atomUid2);
    if (atom2 == null) {
      Logger.error("bondAtoms cannot find atomUid2?:" + atomUid2);
      return null;
    }
    // note that if the atoms are already bonded then
    // Atom.bondMutually(...) will return null
    if (atom1.isBonded(atom2))
      return null;
    boolean isNear = (order == Edge.BOND_STEREO_NEAR);
    boolean isFar = (order == Edge.BOND_STEREO_FAR);
    Bond bond;
    if (isNear || isFar) {
      int m = atom1.getModelIndex();
      if (m != lastModel) {
        lastModel = m;
        Map<String, Object> info = ms.getModelAuxiliaryInfo(m);
        isMOL2D = (info != null && "2D".equals(info.get("dimension")));
      }
      bond = ms.bondMutually(atom1, atom2, (isMOL2D ? order : 1), ms.getDefaultMadFromOrder(1), 0);
      if (vStereo == null) {
        vStereo = new  Lst<Bond>();
      }
      vStereo.addLast(bond);
    } else {
      bond = ms.bondMutually(atom1, atom2, order, ms.getDefaultMadFromOrder(order), 0);
      if (bond.isAromatic()) {
        ms.someModelsHaveAromaticBonds = true;
      }
    }
    if (ms.bondCount == ms.bo.length) {
      ms.bo = (Bond[]) AU.arrayCopyObject(ms.bo, ms.bondCount + BondCollection.BOND_GROWTH_INCREMENT);
    }
    ms.setBond(ms.bondCount++, bond);
    return bond;
  }
  
  public BS structuresDefinedInFile = new BS();
  private int stereodir = 1;

  ////// symmetry ///////

  private void initializeUnitCellAndSymmetry() {
    /*
     * really THREE issues here:
     * 1) does a model have an associated unit cell that could be displayed?
     * 2) are the coordinates fractional and so need to be transformed?
     * 3) does the model have symmetry operations that were applied?
     * 
     * This must be done for each model individually.
     * 
     */

    if (someModelsAreModulated && ms.bsModulated == null)
      ms.bsModulated = new BS();
    if (someModelsHaveUnitcells) {
      ms.unitCells = new SymmetryInterface[ms.mc];
      ms.haveUnitCells = true;
      boolean haveMergeCells = (modelSet0 != null
          && modelSet0.unitCells != null);
      for (int i = 0, pt = 0; i < ms.mc; i++) {
        if (haveMergeCells && i < baseModelCount) {
          ms.unitCells[i] = modelSet0.unitCells[i];
        } else {
          ms.unitCells[i] = Interface.getSymmetry(vwr, "file");
          double[] notionalCell = null;
          if (isTrajectory) {
            @SuppressWarnings("unchecked")
            Lst<double[]> lst = (Lst<double[]>) ms.getInfoM("unitCells");
            if (lst != null)
              notionalCell = lst.get(pt++);
          }
          ms.unitCells[i].setSymmetryInfo(i, ms.getModelAuxiliaryInfo(i), notionalCell);
        }
      }
    }
    if (appendNew && ms.someModelsHaveSymmetry) {
      ms.getAtoms(T.symmetry, null);
      Atom[] atoms = ms.at;
      for (int iAtom = baseAtomIndex, iModel = -1, i0 = 0; iAtom < ms.ac; iAtom++) {
        if (atoms[iAtom].mi != iModel) {
          iModel = atoms[iAtom].mi;
          i0 = baseAtomIndex + ms.getInfoI(iModel, "presymmetryAtomIndex");
          if (i0 < 0)
            break;
          int n = ms.getInfoI(iModel, "presymmetryAtomCount");
          ms.am[iModel].auxiliaryInfo.put("presymmetryAtomIndex", Integer.valueOf(i0));
          ms.setInfo(iModel, "asymmetricUnit", ms.am[iModel].bsAsymmetricUnit = BSUtil.newBitSet2(i0,  i0 + n));
          i0 += n;
        }
        if (iAtom >= i0)
          ms.bsSymmetry.set(iAtom);
      }
    }
    if (appendNew && ms.someModelsHaveFractionalCoordinates) {
      Atom[] atoms = ms.at;
      int modelIndex = -1;
      SymmetryInterface c = null;
      boolean isFractional = false;
      boolean roundCoords = (!vwr.getBoolean(T.doubleprecision) && !vwr.getBoolean(T.legacyjavafloat));
      for (int i = baseAtomIndex; i < ms.ac; i++) {
        if (atoms[i].mi != modelIndex) {
          modelIndex = atoms[i].mi;
          c = ms.getUnitCell(modelIndex);
          isFractional = (c != null && c.getCoordinatesAreFractional());
        }
        if (isFractional) {
          // it is possible for atoms to have specific unit cells, not just models
          // this happens for commensurately modulated composite compounds
          c = atoms[i].getUnitCell();
          c.toCartesianF(c.toSupercell(atoms[i]), false);
          if (roundCoords)
            PT.fixPtFloats(atoms[i], PT.CARTESIAN_PRECISION);
        }
      }
      for (int imodel = baseModelIndex; imodel < ms.mc; imodel++)
        if (ms.isTrajectory(imodel))
          ms.trajectory.setUnitCell(imodel);
    }
  }

  private void initializeBonding() {
    // perform bonding if necessary

    // 1. apply CONECT records and set bsExclude to omit them
    // 2. apply stereochemistry from JME

    int modelCount = ms.mc;
    Model[] models = ms.am;
    int modelAtomCount = 0;
    BS bsExclude = (BS) ms.getInfoM("bsExcludeBonding");
    if (bsExclude == null) {
      bsExclude = (ms.getInfoM(JC.getBoolName(JC.GLOBAL_CONECT)) == null ? null
          : new BS());
      if (bsExclude != null)
        ms.setPdbConectBonding(baseAtomIndex, baseModelIndex, bsExclude);
    }

    // 2. for each model in the collection,
    boolean symmetryAlreadyAppliedToBonds = vwr
        .getBoolean(T.applysymmetrytobonds);
    boolean doAutoBond = vwr.getBoolean(T.autobond);
    boolean forceAutoBond = vwr.getBoolean(T.forceautobond);
    BS bs = null;
    boolean autoBonding = false;
    if (!noAutoBond)
      for (int i = baseModelIndex; i < modelCount; i++) {
        modelAtomCount = models[i].bsAtoms.cardinality();
        int modelBondCount = ms.getInfoI(i, "initialBondCount");

        boolean modelIsPDB = models[i].isBioModel;
        if (modelBondCount < 0) {
          modelBondCount = ms.bondCount;
        }
        // check for PDB file with fewer than one bond per every two atoms
        // this is in case the PDB format is being usurped for non-RCSB uses
        // In other words, say someone uses the PDB format to indicate atoms and
        // connectivity. We do NOT want to mess up that connectivity here.
        // It would be OK if people used HETATM for every atom, but I think
        // people
        // use ATOM, so that's a problem. Those atoms would not be excluded from
        // the
        // automatic bonding, and additional bonds might be made.
        boolean doBond = (forceAutoBond || doAutoBond && (modelBondCount == 0
            || modelIsPDB && jmolData == null
                && (ms.getMSInfoB("havePDBHeaderName")
                    || modelBondCount < modelAtomCount / 2)
            || ms.getInfoB(i, "hasSymmetry") && !symmetryAlreadyAppliedToBonds
                && !ms.getInfoB(i, "hasBonds")));
        if (!doBond)
          continue;
        autoBonding = true;
        if (merging || modelCount > 1) {
          if (bs == null)
            bs = BS.newN(ms.ac);
          if (i == baseModelIndex || !isTrajectory)
            bs.or(models[i].bsAtoms);
        }
      }
    if (modulationOn)
      ms.setModulation(null, true, modulationTUV, false);
    if (autoBonding) {
      ms.autoBondBs4(bs, bs, bsExclude, null, ms.defaultCovalentMad,
          vwr.getBoolean(T.legacyautobonding));
      Logger.info(
          "ModelSet: autobonding; use  autobond=false  to not generate bonds automatically");
    } else {
      //ms.initializeBspf();
      Logger.info(
          "ModelSet: not autobonding; use  forceAutobond=true  to force automatic bond creation");
    }
  }

  private void finalizeGroupBuild() {
    // run this loop in increasing order so that the
    // groups get defined going up
    groups = new Group[groupCount];
    if (merging)
      for (int i = 0; i < mergeGroups.length; i++)
        (groups[i] = mergeGroups[i]).chain.model.ms = ms;
    for (int i = baseGroupIndex; i < groupCount; ++i)
      distinguishAndPropagateGroup(i, chainOf[i], group3Of[i], seqcodes[i],
          firstAtomIndexes[i], (i == groupCount - 1 ? ms.ac
              : firstAtomIndexes[i + 1]) - 1);
    if (group3Lists != null) {
      ms.msInfo.put("group3Lists", group3Lists);
      ms.msInfo.put("group3Counts", group3Counts);
      for (int i = 0; i < group3Counts.length; i++)
        if (group3Counts[i] == null)
          group3Counts[i] = new int[0];
    }
  }

  private void distinguishAndPropagateGroup(int groupIndex, Chain chain,
                                            String group3, int seqcode,
                                            int firstAtomIndex, int lastAtomIndex) {
    /*
     * called by finalizeGroupBuild()
     * 
     * first: build array of special atom names, 
     * for example "CA" for the alpha carbon is assigned #2
     * see JmolConstants.specialAtomNames[]
     * the special atoms all have IDs based on Atom.lookupSpecialAtomID(atomName)
     * these will be the same for each conformation
     * 
     * second: creates the monomers themselves based on this information
     * thus building the byte offsets[] array for each monomer, indicating which
     * position relative to the first atom in the group is which atom.
     * Each monomer.offsets[i] then points to the specific atom of that type
     * these will NOT be the same for each conformation  
     * 
     */
    if (lastAtomIndex < firstAtomIndex)
      throw new NullPointerException();
    
    Group group = (group3 == null || jbr == null ? null : jbr
        .distinguishAndPropagateGroup(chain, group3, seqcode, firstAtomIndex,
            lastAtomIndex, specialAtomIndexes, ms.at));
    String key;
    if (group == null) {
      group = new Group().setGroup(chain, group3, seqcode, firstAtomIndex,
          lastAtomIndex);
      if (jbr != null)
        group.groupID = jbr.getGroupID(group3);
      key = "o>";
    } else {
      key = (group.isProtein() ? "p>" : group.isNucleic() ? "n>" : group
          .isCarbohydrate() ? "c>" : "o>");
    }
    if (group3 != null) {
      countGroup(ms.at[firstAtomIndex].mi, key, group3);
      if (group.isNucleic()) {
        String g1 = (htGroup1 == null ? null : htGroup1.get(group3));
        if (g1 != null) // from SEQADV or _struct_ref_seq_dif.db_mon_id
          group.group1 = g1.charAt(0);
      }
    }
    addGroup(chain, group);
    groups[groupIndex] = group;
    group.groupIndex = groupIndex;

    for (int i = lastAtomIndex + 1; --i >= firstAtomIndex;)
        ms.at[i].group = group;

  }

  private void addGroup(Chain chain, Group group) {
    if (chain.groupCount == chain.groups.length)
      chain.groups = (Group[])AU.doubleLength(chain.groups);
    chain.groups[chain.groupCount++] = group;
  }

  private void countGroup(int modelIndex, String code, String group3) {
    int ptm = modelIndex + 1;
    if (group3Lists == null || group3Lists[ptm] == null)
      return;
    String g3code = (group3 + "   ").substring(0, 3);
    int pt = group3Lists[ptm].indexOf(g3code);
    if (pt < 0) {
      group3Lists[ptm] += ",[" + g3code + "]";
      pt = group3Lists[ptm].indexOf(g3code);
      group3Counts[ptm] = AU.arrayCopyI(
          group3Counts[ptm], group3Counts[ptm].length + 10);
    }
    group3Counts[ptm][pt / 6]++;
    pt = group3Lists[ptm].indexOf(",[" + g3code);
    if (pt >= 0)
      group3Lists[ptm] = group3Lists[ptm].substring(0, pt) + code
          + group3Lists[ptm].substring(pt + 2);
    //becomes x> instead of ,[ 
    //these will be used for setting up the popup menu
    if (modelIndex >= 0)
      countGroup(-1, code, group3);
  }

  private void freeze() {
    htAtomMap.clear();
    // resize arrays
    if (ms.ac < ms.at.length)
      ms.growAtomArrays(ms.ac);
    if (ms.bondCount < ms.bo.length)
      ms.bo = (Bond[]) AU.arrayCopyObject(ms.bo, ms.bondCount);

    // free bonds cache 

    for (int i = BondCollection.MAX_BONDS_LENGTH_TO_CACHE; --i > 0;) { // .GT. 0
      ms.numCached[i] = 0;
      Bond[][] bondsCache = ms.freeBonds[i];
      for (int j = bondsCache.length; --j >= 0;)
        bondsCache[j] = null;
    }

    ms.setAtomNamesAndNumbers(0, baseAtomIndex, modelSet0, false);

    // find elements for the popup menus

    findElementsPresent();

    ms.resetMolecules();
    model = null;
    currentChain = null;

    // finalize all structures

    if (!ms.haveBioModels || isPyMOLsession || isMutate) {
      ms.freezeModels();
      return;
    }
    boolean asDSSP = vwr.getBoolean(T.defaultstructuredssp);
    // code in 14.4 but will require 14.5 for this implementation
    String ret = ms.calculateStructuresAllExcept(structuresDefinedInFile, 
          asDSSP, 
          false, true, true, asDSSP, JC.versionInt >= 1405000 && ms.getInfoM("DSSP1") == null ? 2 : 1); // now DSSP 2.0
    if (ret.length() > 0)
      Logger.info(ret);
  }

  private void findElementsPresent() {
    ms.elementsPresent = new BS[ms.mc];
    for (int i = 0; i < ms.mc; i++)
      ms.elementsPresent[i] = BS.newN(64);
    for (int i = ms.ac; --i >= 0;) {
      Atom a = ms.at[i];
      if (a == null)
        continue;
      int n = a.getAtomicAndIsotopeNumber();
      if (n >= Elements.elementNumberMax)
        n = Elements.elementNumberMax
            + Elements.altElementIndexFromNumber(n);
      ms.elementsPresent[a.mi].set(n);
    }
  }

  private void applyStereochemistry() {

    // 1) initialize average bond lengths
    set2DLengths(baseAtomIndex, ms.ac);
    
    V3 v = new V3();
    
    if (vStereo != null) {
      out: for (int i = vStereo.size(); --i >= 0;) {
        Bond b = vStereo.get(i);
        Atom a1 = b.atom1;
        Bond[] bonds = a1.bonds;
        for (int j = a1.getBondCount(); --j >= 0;) {
          Bond b2 = bonds[j];
          if (b2 == b)
            continue;
          Atom a2 = b2.getOtherAtom(a1);
          v.sub2(a2, a1);
          //System.out.println(b2 + " " + v);
          // in the case where we have a vertical stereochemical indicator, 
          // South and wedge or North and hash sets to invert stereo??
          if (Math.abs(v.x) < 0.1) {
            if ((b.order == Edge.BOND_STEREO_NEAR) == (v.y < 0))
              stereodir = -1;
            break out;
          }
          
        }
      }}
    
    // 2) implicit stereochemistry 
    
    set2dZ(baseAtomIndex, ms.ac, v);

    // 3) explicit stereochemistry
    
    if (vStereo != null) {
      BS bsToTest = new BS();
      bsToTest.setBits(baseAtomIndex, ms.ac);
      for (int i = vStereo.size(); --i >= 0;) {
        Bond b = vStereo.get(i);
        float dz2 = (b.order == Edge.BOND_STEREO_NEAR ? 3 : -3);
        b.order = 1;
        if (b.atom2.z != b.atom1.z && (dz2 < 0) == (b.atom2.z < b.atom1.z))
          dz2 /= 3;
        //float dz1 = dz2/3;
        //b.atom1.z += dz1;
        BS bs = JmolMolecule.getBranchBitSet(ms.at, b.atom2.i, bsToTest, null, b.atom1.i, false, true);
        bs.set(b.atom2.i); // ring structures
        for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1))
          ms.at[j].z += dz2;
        // move atom2 somewhat closer to but not directly above atom1
        b.atom2.x = (b.atom1.x + b.atom2.x) /2;
        b.atom2.y = (b.atom1.y + b.atom2.y) /2;
      }
      vStereo = null;
    } 
    is2D = false;
  }

  private void set2DLengths(int iatom1, int iatom2) {
    float scaling = 0;
    int n = 0;
    for (int i = iatom1; i < iatom2; i++) {
      Atom a = ms.at[i];
      Bond[] bonds = a.bonds;
      if (bonds == null)
        continue;
      for (int j = bonds.length; --j >= 0;) {
        if (bonds[j] == null)
          continue;
        Atom b = bonds[j].getOtherAtom(a);
        if (b.getAtomNumber() != 1 && b.getIndex() > i) {
          scaling += b.distance(a);
          n++;
        }
      }
    }
    if (n == 0)
      return;
    scaling = 1.45f / (scaling/n);
    for (int i = iatom1; i < iatom2; i++) {
      ms.at[i].scale(scaling);
    }
  }

  private void set2dZ(int iatom1, int iatom2, V3 v) {
    BS atomlist = BS.newN(iatom2);
    BS bsBranch = new BS();
    V3 v0 = V3.new3(0, 1, 0);
    V3 v1 = new V3();
    BS bs0 = new BS();
    bs0.setBits(iatom1, iatom2);
    for (int i = iatom1; i < iatom2; i++)
      if (!atomlist.get(i) && !bsBranch.get(i)) {
        bsBranch = getBranch2dZ(i, -1, bs0, bsBranch, v, v0, v1, stereodir );
        atomlist.or(bsBranch);
      }
  }
  
  
  /**
   * @param atomIndex 
   * @param atomIndexNot 
   * @param bs0 
   * @param bsBranch  
   * @param v 
   * @param v0 
   * @param v1 
   * @param dir 
   * @return   atom bitset
   */
  private BS getBranch2dZ(int atomIndex, int atomIndexNot, BS bs0, 
                              BS bsBranch, V3 v, V3 v0, V3 v1, int dir) {
    BS bs = BS.newN(ms.ac);
    if (atomIndex < 0)
      return bs;
    BS bsToTest = new BS();
    bsToTest.or(bs0);
    if (atomIndexNot >= 0)
      bsToTest.clear(atomIndexNot);
    setBranch2dZ(ms.at[atomIndex], bs, bsToTest, v, v0, v1, dir);
    return bs;
  }

  private static void setBranch2dZ(Atom atom, BS bs,
                                            BS bsToTest, V3 v,
                                            V3 v0, V3 v1, int dir) {
    int atomIndex = atom.i;
    if (!bsToTest.get(atomIndex))
      return;
    bsToTest.clear(atomIndex);
    bs.set(atomIndex);
    if (atom.bonds == null)
      return;
    for (int i = atom.bonds.length; --i >= 0;) {
      Bond bond = atom.bonds[i];
      if (bond.isHydrogen())
        continue;
      Atom atom2 = bond.getOtherAtom(atom);
      setAtom2dZ(atom, atom2, v, v0, v1, dir);
      setBranch2dZ(atom2, bs, bsToTest, v, v0, v1, dir);
    }
  }

  /**
   * set the z based on a cos relationship to the y axis. Specifically,
   *
   *   theta = acos(v.dot.v0)
   * 
   *   z = 0.8 * sin(4 theta)
   * so:
   * 
   *    theta     4 theta
   * y  0           0 
   * x  pi/2       2 pi
   * -y pi         4 pi         
   * -x pi/2       2 pi
   * 
   * y   -1/4  -pi/4
   * x   +1/4   pi/4
   * -y  -1/4   3pi/4
   * -x  +1/4  -pi/4
   * 
   *  
   * @param atomRef
   * @param atom2
   * @param v
   * @param v0
   * @param v1
   * @param dir 
   */
  private static void setAtom2dZ(Atom atomRef, Atom atom2, V3 v, V3 v0, V3 v1, int dir) {
    v.sub2(atom2, atomRef);
    v.z = 0;
    v.normalize();
    v1.cross(v0, v);
    double theta = Math.acos(v.dot(v0));
    float f = (float) (0.4f * -dir * Math.sin(4*theta)); // was 0.8
    atom2.z = atomRef.z + f;
//    System.out.println(atomRef + " " + atomRef.z + " " + atom2 + " " + atom2.z + " " + f + " " + v + " " + (theta * 180/Math.PI));
  }

  ///////////////  shapes  ///////////////
  
  private void finalizeShapes() {
    ms.sm = vwr.shm;
    ms.sm.setModelSet(ms);
    ms.setBsHidden(vwr.slm.getHiddenSet());
    if (!merging)
      ms.sm.resetShapes(false);
    ms.sm.loadDefaultShapes(ms);
    if (ms.someModelsHaveAromaticBonds && vwr.getBoolean(T.smartaromatic))      
      ms.assignAromaticBondsBs(false, null);
    if (merging && baseModelCount == 1)
        ms.sm.setShapePropertyBs(JC.SHAPE_MEASURES, "clearModelIndex", null, null);
  }

  /**
   * called from org.jmol.modelsetbio.resolver when adding hydrogens.
   * 
   * @param iAtom
   */
  public void undeleteAtom(int iAtom) {
    ms.at[iAtom].valence = 0; 
  }

  
  public static String createAtomDataSet(Viewer vwr, ModelSet modelSet, int tokType, Object asc,
                                BS bsSelected) {
    if (asc == null)
      return null;
    // must be one of JmolConstants.LOAD_ATOM_DATA_TYPES
    JmolAdapter adapter = vwr.getModelAdapter();
    P3 pt = new P3();
    Atom[] atoms = modelSet.at;
    float tolerance = vwr.getFloat(T.loadatomdatatolerance);
    if (modelSet.unitCells != null)
      for (int i = bsSelected.nextSetBit(0); i >= 0; i = bsSelected
          .nextSetBit(i + 1))
        if (atoms[i].atomSymmetry != null) {
          tolerance = -tolerance;
          break;
        }
    int i = -1;
    int n = 0;
    boolean loadAllData = (BSUtil.cardinalityOf(bsSelected) == vwr
        .ms.ac);
    for (JmolAdapterAtomIterator iterAtom = adapter
        .getAtomIterator(asc); iterAtom.hasNext();) {
      P3d xyz = iterAtom.getXYZ();
      if (Double.isNaN(xyz.x + xyz.y + xyz.z))
        continue;
      if (tokType == T.xyz) {
        // we are loading selected coordinates only
        i = bsSelected.nextSetBit(i + 1);
        if (i < 0)
          break;
        n++;
        if (Logger.debugging)
          Logger.debug("atomIndex = " + i + ": " + atoms[i]
              + " --> (" + xyz.x + "," + xyz.y + "," + xyz.z);
        modelSet.setPrecisionCoord(i, xyz, true);
        continue;
      }
      xyz.setP(pt);
      BS bs = BS.newN(modelSet.ac);
      modelSet.getAtomsWithin(tolerance, pt, bs, -1);
      bs.and(bsSelected);
      if (loadAllData) {
        n = BSUtil.cardinalityOf(bs);
        if (n == 0) {
          Logger.warn("createAtomDataSet: no atom found at position " + pt);
          continue;
        } else if (n > 1 && Logger.debugging) {
          Logger.debug("createAtomDataSet: " + n + " atoms found at position "
              + pt);
        }
      }
      switch (tokType) {
      case T.vibxyz:
        V3 vib = iterAtom.getVib();
        if (vib == null)
          continue;
        if (Logger.debugging)
          Logger.debug("xyz: " + pt + " vib: " + vib);
        modelSet.setAtomCoords(bs, T.vibxyz, vib);
        break;
      case T.occupancy:
        // [0 to 100], default 100
        modelSet.setAtomProperty(bs, tokType, 0, iterAtom.getOccupancy(), null, null,
            null);
        break;
      case T.partialcharge:
        // anything but NaN, default NaN
        modelSet.setAtomProperty(bs, tokType, 0, iterAtom.getPartialCharge(), null,
            null, null);
        break;
      case T.temperature:
        // anything but NaN but rounded to 0.01 precision and stored as a short (-32000 - 32000), default NaN
        modelSet.setAtomProperty(bs, tokType, 0, iterAtom.getBfactor(), null, null, null);
        break;
      }
    }
    //finally:
    switch (tokType) {
    case T.vibxyz:
      String vibName = adapter.getAtomSetName(asc, 0);
      Logger.info("_vibrationName = " + vibName);
      vwr.setStringProperty("_vibrationName", vibName);
      break;
    case T.xyz:
      Logger.info(n + " atom positions read");
      modelSet.recalculateLeadMidpointsAndWingVectors(-1);
      if (n == modelSet.ac)
        return "boundbox {*};reset";
      break;
    }
    return null;
  }

}
