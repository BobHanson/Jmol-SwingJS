/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2017-12-13 21:57:36 -0600 (Wed, 13 Dec 2017) $
 * $Revision: 21766 $

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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.jmol.api.AtomIndexIterator;
import org.jmol.api.Interface;
import org.jmol.api.JmolModulationSet;
import org.jmol.api.SymmetryInterface;
import org.jmol.atomdata.AtomData;
import org.jmol.atomdata.RadiusData;
import org.jmol.bspt.Bspf;
import org.jmol.bspt.CubeIterator;
import org.jmol.c.PAL;
import org.jmol.c.STR;
import org.jmol.c.VDW;
import org.jmol.modelsetbio.BioModel;
import org.jmol.script.ScriptCompiler;
import org.jmol.script.T;
import org.jmol.shape.Shape;
import org.jmol.util.BSUtil;
import org.jmol.util.BoxInfo;
import org.jmol.util.Edge;
import org.jmol.util.Elements;
import org.jmol.util.Escape;
import org.jmol.util.JmolMolecule;
import org.jmol.util.Logger;
import org.jmol.util.Point3fi;
import org.jmol.util.Rectangle;
import org.jmol.util.SimpleUnitCell;
import org.jmol.util.Tensor;
import org.jmol.util.Vibration;
import org.jmol.viewer.JC;
import org.jmol.viewer.JmolAsyncException;
import org.jmol.viewer.ShapeManager;
import org.jmol.viewer.TransformManager;
import org.jmol.viewer.Viewer;

import javajs.util.A4;
import javajs.util.AU;
import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.Quat;
import javajs.util.SB;
import javajs.util.T3;
import javajs.util.V3;


/*
 * A class always created using new ModelLoader(...)
 * 
 * Merged with methods in Mmset and ModelManager 10/2007  Jmol 11.3.32
 * 
 * ModelLoader simply pulls out all private classes that are
 * necessary only for file loading (and structure recalculation).
 * 
 * What is left here are all the methods that are 
 * necessary AFTER a model is loaded, when it is being 
 * accessed by Viewer, primarily.
 * 
 * Please:
 * 
 * 1) designate any methods used only here as private
 * 2) designate any methods accessed only by ModelLoader as protected
 * 3) designate any methods accessed within modelset as nothing
 * 4) designate any methods accessed only by Viewer as public
 * 
 * Bob Hanson, 5/2007, 10/2007
 * 
 */
public class ModelSet extends BondCollection {

  public boolean haveBioModels;
  
  protected BS bsSymmetry;

  public String modelSetName;

  public Model[] am;
  /**
   * model count
   */
  public int mc;

  public SymmetryInterface[] unitCells;
  public boolean haveUnitCells;

  protected final Atom[] closest;

  protected int[] modelNumbers; // from adapter -- possibly PDB MODEL record; possibly modelFileNumber
  public int[] modelFileNumbers; // file * 1000000 + modelInFile (1-based)
  public String[] modelNumbersForAtomLabel, modelNames, frameTitles;

  protected BS[] elementsPresent;

  protected boolean isXYZ;

  public Properties modelSetProperties;
  public Map<String, Object> msInfo;

  protected boolean someModelsHaveSymmetry;
  protected boolean someModelsHaveAromaticBonds;
  protected boolean someModelsHaveFractionalCoordinates;

  ////////////////////////////////////////////

  private boolean isBbcageDefault;
  public BS bboxModels;
  private BS bboxAtoms;
  private final BoxInfo boxInfo;
  
  public BoxInfo getBoxInfo() {
    return boxInfo;
  }

  public Lst<StateScript> stateScripts;
  /*
   * stateScripts are connect commands that must be executed in sequence.
   * 
   * What I fear is that in deleting models we must delete these connections,
   * and in deleting atoms, the bitsets may not be retrieved properly. 
   * 
   * 
   */
  private int thisStateModel;

  protected Lst<V3[]> vibrationSteps;

  private BS selectedMolecules;

  //private final static boolean MIX_BSPT_ORDER = false;
  boolean showRebondTimes = true;

  protected BS bsAll;

  public ShapeManager sm;

  private static float hbondMinRasmol = 2.5f;

  public boolean proteinStructureTainted;

  public Hashtable<String, BS> htPeaks;

  private Quat[] vOrientations;

  private final P3 ptTemp, ptTemp1, ptTemp2;
  private final M3 matTemp, matInv;
  private final M4 mat4, mat4t;
  private final V3 vTemp;

  ////////////////////////////////////////////////////////////////

  /**
   * 
   * @param vwr
   * @param name
   */
  public ModelSet(Viewer vwr, String name) {
    this.vwr = vwr;
    modelSetName = name;

    selectedMolecules = new BS();
    stateScripts = new Lst<StateScript>();

    boxInfo = new BoxInfo();
    boxInfo.addBoundBoxPoint(P3.new3(-10, -10, -10));
    boxInfo.addBoundBoxPoint(P3.new3(10, 10, 10));

    am = new Model[1];
    modelNumbers = new int[1]; // from adapter -- possibly PDB MODEL record; possibly modelFileNumber
    modelFileNumbers = new int[1]; // file * 1000000 + modelInFile (1-based)
    modelNumbersForAtomLabel = new String[1];
    modelNames = new String[1];
    frameTitles = new String[1];

    closest = new Atom[1];

    ptTemp = new P3();
    ptTemp1 = new P3();
    ptTemp2 = new P3();
    matTemp = new M3();
    matInv = new M3();
    mat4 = new M4();
    mat4t = new M4();
    vTemp = new V3();

    setupBC();
  }

  protected void releaseModelSet() {
    am = null;
    closest[0] = null;
    /*
     * Probably unnecessary, but here for general accounting.
     * 
     * I added this when I was trying to track down a memory bug.
     * I know that you don't have to do this, but I was concerned
     * that somewhere in this mess was a reference to modelSet. 
     * As it turns out, it was in models[i] (which was not actually
     * nulled but, rather, transferred to the new model set anyway).
     * Quite amazing that that worked at all, really. Some models were
     * referencing the old modelset, some the new. Yeiks!
     * 
     * Bob Hanson 11/7/07
     * 
     */
    am = null;
    bsSymmetry = null;
    bsAll = null;
    unitCells = null;
    releaseModelSetBC();
  }

  //variables that will be reset when a new frame is instantiated

  private boolean echoShapeActive = false;

  public boolean getEchoStateActive() {
    return echoShapeActive;
  }

  public void setEchoStateActive(boolean TF) {
    echoShapeActive = TF;
  }

  protected String modelSetTypeName;

  public String getModelSetTypeName() {
    return modelSetTypeName;
  }

  /**
   * 
   * @param modelNumber
   *        can be a PDB MODEL number or a simple index number, or a fffnnnnnn
   *        f.n number
   * @param useModelNumber
   * @param doSetTrajectory
   * @return index
   */
  public int getModelNumberIndex(int modelNumber, boolean useModelNumber,
                                 boolean doSetTrajectory) {
    if (useModelNumber) {
      for (int i = 0; i < mc; i++)
        if (modelNumbers[i] == modelNumber || modelNumber < 1000000
            && modelNumbers[i] == 1000000 + modelNumber)
          return i;
      return -1;
    }
    if (modelNumber < 1000000)
      return modelNumber;
    //new decimal format:   frame 1.2 1.3 1.4
    for (int i = 0; i < mc; i++)
      if (modelFileNumbers[i] == modelNumber) {
        if (doSetTrajectory && isTrajectory(i))
          setTrajectory(i);
        return i;
      }
    return -1;
  }

  public String getModelDataBaseName(BS bsAtoms) {
    for (int i = 0; i < mc; i++) {
      if (bsAtoms.equals(am[i].bsAtoms))
        return (String) getInfo(i, "dbName");
    }
    return null;
  }

  public void setTrajectory(int modelIndex) {
    if (modelIndex >= 0 && isTrajectory(modelIndex)
        && at[am[modelIndex].firstAtomIndex].mi != modelIndex)
      trajectory.setModel(modelIndex);
  }

  public BS getBitSetTrajectories() {
    return (trajectory == null ? null : trajectory.getModelsSelected());
  }

  public void setTrajectoryBs(BS bsModels) {
    if (trajectory != null)
      for (int i = 0; i < mc; i++)
        if (bsModels.get(i))
          setTrajectory(i);
  }

  public void morphTrajectories(int m1, int m2, float f) {
    if (m1 >= 0 && m2 >= 0 && isTrajectory(m1) && isTrajectory(m2))
      trajectory.morph(m1, m2, f);
  }

  public P3[] translations;

  public P3 getTranslation(int iModel) {
    return (translations == null || iModel >= translations.length ? null
        : translations[iModel]);
  }

  /**
   * move atoms by vector pt; used for co-centering with FRAME ALIGN {atoms}
   * TRUE
   * 
   * @param iModel
   * @param pt
   */
  public void translateModel(int iModel, T3 pt) {
    if (pt == null) {
      P3 t = getTranslation(iModel);
      if (t == null)
        return;
      pt = P3.newP(t);
      pt.scale(-1);
      translateModel(iModel, pt);
      translations[iModel] = null;
      return;
    }
    if (translations == null || translations.length <= iModel)
      translations = new P3[mc];
    if (translations[iModel] == null)
      translations[iModel] = new P3();
    translations[iModel].add(pt);
    BS bs = am[iModel].bsAtoms;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
      at[i].add(pt);
  }

  public P3[] getFrameOffsets(BS bsAtoms, boolean isFull) {
    if (bsAtoms == null) {
      if (isFull)
        for (int i = mc; --i >= 0;) {
          Model m = am[i];
          if (!m.isJmolDataFrame && !m.isTrajectory)
            translateModel(m.modelIndex, null);
        }
      return null;
    }
    int i0 = bsAtoms.nextSetBit(0);
    if (i0 < 0)
      return null;
    if (isFull) {
      BS bs = BSUtil.copy(bsAtoms);
      P3 pt = null;
      P3 pdiff = new P3();
      for (int i = 0; i < mc; i++) {
        Model m = am[i];
        if (!m.isJmolDataFrame && !m.isTrajectory) {
          int j = bs.nextSetBit(0);
          if (m.bsAtoms.get(j)) {
            if (pt == null) {
              pt = P3.newP(at[j]);
            } else {
              pdiff.sub2(pt, at[j]);
              translateModel(i, pdiff);
            }
          }
        }
        bs.andNot(m.bsAtoms);
      }
      return null;
    }
    P3[] offsets = new P3[mc];
    for (int i = mc; --i >= 0;)
      offsets[i] = new P3();
    int lastModel = 0;
    int n = 0;
    P3 offset = offsets[0];
    boolean asTrajectory = (trajectory != null && trajectory.steps.size() == mc);
    int m1 = (asTrajectory ? mc : 1);
    offsets[0].set(0, 0, 0);
    for (int m = 0; m < m1; m++) {
      if (asTrajectory)
        setTrajectory(m);
      for (int i = 0; i <= ac; i++) {
        if (i == ac || at[i].mi != lastModel) {
          if (n > 0) {
            offset.scale(-1.0f / n);
            if (lastModel != 0)
              offset.sub(offsets[0]);
            n = 0;
          }
          if (i == ac)
            break;
          lastModel = at[i].mi;
          offset = offsets[lastModel];
        }
        if (!bsAtoms.get(i))
          continue;
        offset.add(at[i]);
        n++;
      }
    }
    return offsets;
  }

  /**
   * general lookup for integer type -- from Eval
   * 
   * @param tokType
   * @param specInfo
   * @return bitset; null only if we mess up with name
   */
  public BS getAtoms(int tokType, Object specInfo) {
    switch (tokType) {
    default:
      return BSUtil.andNot(getAtomBitsMaybeDeleted(tokType, specInfo),
          vwr.slm.bsDeleted);
    case T.spec_model:
      int modelNumber = ((Integer) specInfo).intValue();
      int modelIndex = getModelNumberIndex(modelNumber, true, true);
      return (modelIndex < 0 && modelNumber > 0 ? new BS() : vwr
          .getModelUndeletedAtomsBitSet(modelIndex));
    case T.polyhedra:
      Object[] data = new Object[] { null, null, null };
      vwr.shm.getShapePropertyData(JC.SHAPE_POLYHEDRA, "getCenters", data);
      return (data[1] == null ? new BS() : (BS) data[1]);
    }
  }

  public int findNearestAtomIndex(int x, int y, BS bsNot, int min) {
    if (ac == 0)
      return -1;
    closest[0] = null;
    if (g3d.isAntialiased()) {
      x <<= 1;
      y <<= 1;
    }
    findNearest2(x, y, closest, bsNot, min);
    sm.findNearestShapeAtomIndex(x, y, closest, bsNot);
    int closestIndex = (closest[0] == null ? -1 : closest[0].i);
    closest[0] = null;
    return closestIndex;
  }

  /*
  private Map userProperties;

  void putUserProperty(String name, Object property) {
    if (userProperties == null)
      userProperties = new Hashtable();
    if (property == null)
      userProperties.remove(name);
    else
      userProperties.put(name, property);
  }
  */

  ///////// atom and shape selecting /////////

  public String calculatePointGroup(BS bsAtoms) {
    return (String) calculatePointGroupForFirstModel(bsAtoms, false,
        false, null, 0, 0, null, null, null);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> getPointGroupInfo(BS bsAtoms) {
    return (Map<String, Object>) calculatePointGroupForFirstModel(bsAtoms,
        false, true, null, 0, 0, null, null, null);
  }

  public String getPointGroupAsString(BS bsAtoms, String type,
                                      int index, float scale, P3[] pts, P3 center, String id) {
    return (String) calculatePointGroupForFirstModel(bsAtoms, true,
        false, type, index, scale, pts, center, id);
  }

  private BoxInfo defaultBBox;

  private Object calculatePointGroupForFirstModel(BS bsAtoms, boolean doAll,
                                                  boolean asInfo, String type,
                                                  int index, float scale,
                                                  T3[] pts, P3 center, String id) {
    SymmetryInterface pointGroup = this.pointGroup;
    SymmetryInterface symmetry = Interface.getSymmetry(vwr, "ms");
    BS bs = null;
    boolean haveVibration = false;
    boolean isPolyhedron = false;
    boolean localEnvOnly = false;
    boolean isPoints = (pts != null);
    int modelIndex = vwr.am.cmi;
    if (!isPoints) {
      int iAtom = (bsAtoms == null ? -1 : bsAtoms.nextSetBit(0));
      if (modelIndex < 0 && iAtom >= 0)
        modelIndex = at[iAtom].mi;
      if (modelIndex < 0) {
        modelIndex = vwr.getVisibleFramesBitSet().nextSetBit(0);
        bsAtoms = null;
      }
      bs = vwr.getModelUndeletedAtomsBitSet(modelIndex);
      localEnvOnly = (bsAtoms != null && bs.cardinality() != bsAtoms
          .cardinality());
      if (bsAtoms != null)
        bs.and(bsAtoms);
      iAtom = bs.nextSetBit(0);
      if (iAtom < 0) {
        bs = vwr.getModelUndeletedAtomsBitSet(modelIndex);
        iAtom = bs.nextSetBit(0);
      }
      Object obj = vwr.shm
          .getShapePropertyIndex(JC.SHAPE_VECTORS, "mad", iAtom);
      haveVibration = (obj != null && ((Integer) obj).intValue() != 0 || vwr.tm.vibrationOn);
      isPolyhedron = (type != null && type.toUpperCase().indexOf(":POLY") >= 0);
      if (isPolyhedron) {
        Object[] data = new Object[] { Integer.valueOf(iAtom), null };
        vwr.shm.getShapePropertyData(JC.SHAPE_POLYHEDRA, "points", data);
        pts = (T3[]) data[1];
        if (pts == null)
          return null;
        bs = null;
        haveVibration = false;
        pointGroup = null;
      } else {
        pts = at;
      }
    }

    int tp;
    if (type != null && (tp = type.indexOf(":")) >= 0)
      type = type.substring(0, tp);
    if (type != null && (tp = type.indexOf(".")) >= 0) {
      index = PT.parseInt(type.substring(tp + 1));
      if (index < 0)
        index = 0;
      type = type.substring(0, tp);
    }
    pointGroup = symmetry.setPointGroup(pointGroup, center, pts, bs,
        haveVibration, (isPoints ? 0 : vwr.getFloat(T.pointgroupdistancetolerance)),
        vwr.getFloat(T.pointgrouplineartolerance), localEnvOnly);
    if (!isPolyhedron && !isPoints)
      this.pointGroup = pointGroup;
    if (!doAll && !asInfo)
      return pointGroup.getPointGroupName();
    Object ret = pointGroup.getPointGroupInfo(modelIndex, id, asInfo, type,
        index, scale);
    return (asInfo ? ret : (mc > 1 ? "frame "
        + getModelNumberDotted(modelIndex) + "; " : "")
        + ret);
  }

  public String getDefaultStructure(BS bsAtoms, BS bsModified) {
    return (haveBioModels ? bioModelset.getAllDefaultStructures(bsAtoms,
        bsModified) : "");
  }
  
  public void deleteModelBonds(int modelIndex) {
    BS bsAtoms = getModelAtomBitSetIncludingDeleted(modelIndex, false);
    makeConnections(0, Float.MAX_VALUE, Edge.BOND_ORDER_NULL, T.delete, bsAtoms, bsAtoms, null, false, false, 0);
  }

  ///// super-overloaded methods ///////

  public int[] makeConnections(float minDistance, float maxDistance, int order,
                               int connectOperation, BS bsA, BS bsB,
                               BS bsBonds, boolean isBonds, boolean addGroup,
                               float energy) {
    if (connectOperation == T.auto && order != Edge.BOND_H_REGULAR) {
      String stateScript = "connect ";
      if (minDistance != JC.DEFAULT_MIN_CONNECT_DISTANCE)
        stateScript += minDistance + " ";
      if (maxDistance != JC.DEFAULT_MAX_CONNECT_DISTANCE)
        stateScript += maxDistance + " ";
      addStateScript(stateScript, (isBonds ? bsA : null),
          (isBonds ? null : bsA), (isBonds ? null : bsB), " auto", false, true);
    }
    moleculeCount = 0;
    return makeConnections2(minDistance, maxDistance, order, connectOperation,
        bsA, bsB, bsBonds, isBonds, addGroup, energy);
  }

  @SuppressWarnings("unchecked")
  public void setPdbConectBonding(int baseAtomIndex, int baseModelIndex,
                                  BS bsExclude) {
    short mad = vwr.getMadBond();
    for (int i = baseModelIndex; i < mc; i++) {
      Lst<int[]> vConnect = (Lst<int[]>) getInfo(i, "PDB_CONECT_bonds");
      if (vConnect == null)
        continue;
      int nConnect = vConnect.size();
      setInfo(i, "initialBondCount", Integer.valueOf(nConnect));
      int[] atomInfo = (int[]) getInfo(i, "PDB_CONECT_firstAtom_count_max");
      int firstAtom = atomInfo[0] + baseAtomIndex;
      int atomMax = firstAtom + atomInfo[1];
      int max = atomInfo[2];
      int[] serialMap = new int[max + 1];
      int iSerial;
      for (int iAtom = firstAtom; iAtom < atomMax; iAtom++)
        if ((iSerial = atomSerials[iAtom]) > 0)
          serialMap[iSerial] = iAtom + 1;
      for (int iConnect = 0; iConnect < nConnect; iConnect++) {
        int[] pair = vConnect.get(iConnect);
        int sourceSerial = pair[0];
        int targetSerial = pair[1];
        short order = (short) pair[2];
        if (sourceSerial < 0 || targetSerial < 0 || sourceSerial > max
            || targetSerial > max)
          continue;
        int sourceIndex = serialMap[sourceSerial] - 1;
        int targetIndex = serialMap[targetSerial] - 1;
        if (sourceIndex < 0 || targetIndex < 0)
          continue;
        Atom atomA = at[sourceIndex];
        Atom atomB = at[targetIndex];
        if (bsExclude != null) {
          if (atomA.isHetero())
            bsExclude.set(sourceIndex);
          if (atomB.isHetero())
            bsExclude.set(targetIndex);
        }
        // don't connect differing altloc
        if (atomA.altloc == atomB.altloc || atomA.altloc == '\0'
            || atomB.altloc == '\0')
          getOrAddBond(atomA, atomB, order, (order == Edge.BOND_H_REGULAR ? 1
              : mad), null, 0, false);
      }
    }
  }

  public void deleteAllBonds() {
    moleculeCount = 0;
    for (int i = stateScripts.size(); --i >= 0;) {
      if (stateScripts.get(i).isConnect()) {
        stateScripts.removeItemAt(i);
      }
    }
    deleteAllBonds2();
  }

  /* ******************************************************
   * 
   * methods for definining the state 
   * 
   ********************************************************/

  private void includeAllRelatedFrames(BS bsModels) {
    int baseModel = 0;
    for (int i = 0; i < mc; i++) {
      boolean isTraj = isTrajectory(i);
      boolean isBase = (isTraj && bsModels
          .get(baseModel = am[i].trajectoryBaseIndex));
      if (bsModels.get(i)) {
        if (isTraj && !isBase) {
          bsModels.set(baseModel);
          includeAllRelatedFrames(bsModels);
          return;
        }
      } else if (isTraj || isJmolDataFrameForModel(i)
          && bsModels.get(am[i].dataSourceFrame)) {
        bsModels.set(i);
      }
    }
  }

  public BS deleteModels(BS bsModels) {
    // full models are deleted for any model containing the specified atoms
    includeAllRelatedFrames(bsModels);

    int nModelsDeleted = bsModels.cardinality();
    if (nModelsDeleted == 0)
      return null;

    moleculeCount = 0;
    if (msInfo != null)
      msInfo.remove("models");

    // clear references to this frame if it is a dataFrame

    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1))
      clearDataFrameReference(i);

    BS bsDeleted;
    if (nModelsDeleted == mc) {
      bsDeleted = getModelAtomBitSetIncludingDeleted(-1, true);
      vwr.zap(true, false, false);
      return bsDeleted;
    }

    // zero out reproducible arrays

    validateBspf(false);

    // create a new models array,
    // and pre-calculate Model.bsAtoms and Model.ac
    Model[] newModels = new Model[mc - nModelsDeleted];
    Model[] oldModels = am;
    bsDeleted = new BS();
    for (int i = 0, mpt = 0; i < mc; i++)
      if (bsModels.get(i)) { // get a good count now
        getAtomCountInModel(i);
        bsDeleted.or(getModelAtomBitSetIncludingDeleted(i, false));
      } else {
        am[i].modelIndex = mpt;
        newModels[mpt++] = am[i];
      }
    am = newModels;
    int oldModelCount = mc;
    // delete bonds
    BS bsBonds = getBondsForSelectedAtoms(bsDeleted, true);
    deleteBonds(bsBonds, true);

    // main deletion cycle

    for (int i = 0, mpt = 0; i < oldModelCount; i++) {
      if (!bsModels.get(i)) {
        mpt++;
        continue;
      }
      int nAtoms = oldModels[i].act;
      if (nAtoms == 0)
        continue;
      BS bsModelAtoms = oldModels[i].bsAtoms;
      int firstAtomIndex = oldModels[i].firstAtomIndex;

      // delete from symmetry set
      BSUtil.deleteBits(bsSymmetry, bsModelAtoms);

      // delete from stateScripts, model arrays and bitsets,
      // atom arrays, and atom bitsets
      deleteModel(mpt, firstAtomIndex, nAtoms, bsModelAtoms, bsBonds);

      // adjust all models after this one
      for (int j = oldModelCount; --j > i;)
        oldModels[j].fixIndices(mpt, nAtoms, bsModelAtoms);

      // adjust all shapes
      vwr.shm.deleteShapeAtoms(new Object[] { newModels, at,
          new int[] { mpt, firstAtomIndex, nAtoms } }, bsModelAtoms);
      mc--;
    }

    // set final values
    //final deletions
    haveBioModels = false;
    for (int i = mc; --i >= 0;)
      if (am[i].isBioModel) {
        haveBioModels = true;
        bioModelset.set(vwr, this);
      }
    validateBspf(false);
    bsAll = null;
    resetMolecules();
    isBbcageDefault = false;
    calcBoundBoxDimensions(null, 1);
    return bsDeleted;
  }

  public void resetMolecules() {
    bsAll = null;
    molecules = null;
    moleculeCount = 0;
    resetChirality();
  }

  private void resetChirality() {
    if (haveChirality) {
      int modelIndex = -1;
      for (int i = ac; --i >= 0;) {
        Atom a = at[i];
        a.setCIPChirality(0);
        if (a.mi != modelIndex && a.mi < am.length)
          am[modelIndex = a.mi].hasChirality = false;
      }
    }
  }

  private void deleteModel(int modelIndex, int firstAtomIndex, int nAtoms,
                           BS bsModelAtoms, BS bsBonds) {
    /*
     *   ModelCollection.modelSetAuxiliaryInfo["group3Lists", "group3Counts, "models"]
     * ModelCollection.stateScripts ?????
     */
    if (modelIndex < 0) {
      return;
    }

    modelNumbers = (int[]) AU.deleteElements(modelNumbers, modelIndex, 1);
    modelFileNumbers = (int[]) AU.deleteElements(modelFileNumbers, modelIndex,
        1);
    modelNumbersForAtomLabel = (String[]) AU.deleteElements(
        modelNumbersForAtomLabel, modelIndex, 1);
    modelNames = (String[]) AU.deleteElements(modelNames, modelIndex, 1);
    frameTitles = (String[]) AU.deleteElements(frameTitles, modelIndex, 1);
    thisStateModel = -1;
    String[] group3Lists = (String[]) getInfoM("group3Lists");
    int[][] group3Counts = (int[][]) getInfoM("group3Counts");
    int ptm = modelIndex + 1;
    if (group3Lists != null && group3Lists[ptm] != null) {
      for (int i = group3Lists[ptm].length() / 6; --i >= 0;)
        if (group3Counts[ptm][i] > 0) {
          group3Counts[0][i] -= group3Counts[ptm][i];
          if (group3Counts[0][i] == 0)
            group3Lists[0] = group3Lists[0].substring(0, i * 6) + ",["
                + group3Lists[0].substring(i * 6 + 2);
        }
    }
    if (group3Lists != null) {
      msInfo.put("group3Lists", AU.deleteElements(group3Lists, modelIndex, 1));
      msInfo
          .put("group3Counts", AU.deleteElements(group3Counts, modelIndex, 1));
    }

    //fix cellInfos array
    if (unitCells != null) {
      unitCells = (SymmetryInterface[]) AU.deleteElements(unitCells,
          modelIndex, 1);
    }

    // correct stateScripts, particularly CONNECT scripts
    for (int i = stateScripts.size(); --i >= 0;) {
      if (!stateScripts.get(i).deleteAtoms(modelIndex, bsBonds, bsModelAtoms)) {
        stateScripts.removeItemAt(i);
      }
    }
    deleteModelAtoms(firstAtomIndex, nAtoms, bsModelAtoms);
    vwr.deleteModelAtoms(modelIndex, firstAtomIndex, nAtoms, bsModelAtoms);
  }

  public void setAtomProperty(BS bs, int tok, int iValue, float fValue,
                              String sValue, float[] values, String[] list) {
    switch (tok) {
    case T.backbone:
    case T.cartoon:
    case T.meshRibbon:
    case T.ribbon:
    case T.rocket:
    case T.strands:
    case T.trace:
      if (fValue > Shape.RADIUS_MAX)
        fValue = Shape.RADIUS_MAX;
      if (values != null) {
        // convert to atom indices
        float[] newValues = new float[ac];
        try {
          for (int i = bs.nextSetBit(0), ii = 0; i >= 0; i = bs
              .nextSetBit(i + 1))
            newValues[i] = values[ii++];
        } catch (Exception e) {
          return;
        }
        values = newValues;
      }
      //$FALL-THROUGH$
    case T.halo:
    case T.star:
      RadiusData rd = null;
      int mar = 0;
      if (values == null) {
        if (fValue > Atom.RADIUS_MAX)
          fValue = Atom.RADIUS_GLOBAL;
        if (fValue < 0)
          fValue = 0;
        mar = (int) Math.floor(fValue * 2000);
      } else {
        rd = new RadiusData(values, 0, null, null);
      }
      sm.setShapeSizeBs(JC.shapeTokenIndex(tok), mar, rd, bs);
      return;
    }
    setAPm(bs, tok, iValue, fValue, sValue, values, list);
  }

  @SuppressWarnings("unchecked")
  public Object getFileData(int modelIndex) {
    if (modelIndex < 0)
      return "";
    Map<String, Object> fileData = (Map<String, Object>) getInfo(modelIndex,
        "fileData");
    if (fileData != null)
      return fileData;
    if (!getInfoB(modelIndex, "isCIF"))
      return getPDBHeader(modelIndex);
    fileData = vwr.getCifData(modelIndex);
    setInfo(modelIndex, "fileData", fileData);
    return fileData;
  }

  /**
   * these are hydrogens that are being added due to a load 2D command and are
   * therefore not to be flagged as NEW
   * 
   * @param vConnections
   * @param pts
   * @return BitSet of new atoms
   */
  public BS addHydrogens(Lst<Atom> vConnections, P3[] pts) {
    int modelIndex = mc - 1;
    BS bs = new BS();
    if (isTrajectory(modelIndex) || am[modelIndex].getGroupCount() > 1) {
      // can't add atoms to a trajectory or a system with multiple groups!
      return bs;
    }
    growAtomArrays(ac + pts.length);
    RadiusData rd = vwr.rd;
    short mad = getDefaultMadFromOrder(1);
    am[modelIndex].resetDSSR(false);
    for (int i = 0, n = am[modelIndex].act + 1; i < vConnections.size(); i++, n++) {
      Atom atom1 = vConnections.get(i);
      // hmm. atom1.group will not be expanded, though...
      // something like within(group,...) will not select these atoms!
      Atom atom2 = addAtom(modelIndex, atom1.group, 1, "H" + n, null, n,
          atom1.getSeqID(), n, pts[i], Float.NaN, null, 0, 0, 100, Float.NaN,
          null, false, (byte) 0, null, Float.NaN);

      atom2.setMadAtom(vwr, rd);
      bs.set(atom2.i);
      bondAtoms(atom1, atom2, Edge.BOND_COVALENT_SINGLE, mad, null, 0, false,
          false);
    }
    // must reset the shapes to give them new atom counts and arrays
    sm.loadDefaultShapes(this);
    return bs;
  }

  /**
   * initial transfer of model data from old to new model set. Note that all new
   * models are added later, AFTER thfe old ones. This is very important,
   * because all of the old atom numbers must map onto the same numbers in the
   * new model set, or the state script will not run properly, among other
   * problems.
   * 
   * @param mergeModelSet
   */
  protected void mergeModelArrays(ModelSet mergeModelSet) {
    at = mergeModelSet.at;
    bo = mergeModelSet.bo;
    stateScripts = mergeModelSet.stateScripts;
    proteinStructureTainted = mergeModelSet.proteinStructureTainted;
    thisStateModel = -1;
    bsSymmetry = mergeModelSet.bsSymmetry;
    modelFileNumbers = mergeModelSet.modelFileNumbers; // file * 1000000 + modelInFile (1-based)
    modelNumbersForAtomLabel = mergeModelSet.modelNumbersForAtomLabel;
    modelNames = mergeModelSet.modelNames;
    modelNumbers = mergeModelSet.modelNumbers;
    frameTitles = mergeModelSet.frameTitles;
    haveChirality = mergeModelSet.haveChirality;
    if (msInfo != null)
      msInfo.remove("models");
    mergeAtomArrays(mergeModelSet);
  }

  public SymmetryInterface getUnitCell(int modelIndex) {
    if (modelIndex < 0 || modelIndex >= mc)
      return null;
    if (am[modelIndex].simpleCage != null)
      return am[modelIndex].simpleCage;
    if (unitCells != null && modelIndex < unitCells.length
        && unitCells[modelIndex] != null && unitCells[modelIndex].haveUnitCell())
      return unitCells[modelIndex];
    if (getInfo(modelIndex, "unitCellParams") != null) {
      if (unitCells == null)
        unitCells = new SymmetryInterface[mc];
      haveUnitCells = true;
      return unitCells[modelIndex] = vwr.getSymTemp().setSymmetryInfo(modelIndex,
          am[modelIndex].auxiliaryInfo, null);
    }
    return null;
  }

  public void setModelCage(int modelIndex, SymmetryInterface simpleCage) {
    if (modelIndex >= 0 && modelIndex < mc) {
      am[modelIndex].simpleCage = simpleCage;
      haveUnitCells = true;
    }
  }

  public String getModelName(int modelIndex) {
    return mc < 1 ? "" : modelIndex >= 0 ? modelNames[modelIndex]
        : modelNumbersForAtomLabel[-1 - modelIndex];
  }

  public String getModelTitle(int modelIndex) {
    return (String) getInfo(modelIndex, "title");
  }

  public String getModelFileName(int modelIndex) {
    return (String) getInfo(modelIndex, "fileName");
  }

  public String getModelFileType(int modelIndex) {
    return (String) getInfo(modelIndex, "fileType");
  }

  public void setFrameTitle(BS bsFrames, Object title) {
    if (title instanceof String) {
      for (int i = bsFrames.nextSetBit(0); i >= 0; i = bsFrames
          .nextSetBit(i + 1))
        frameTitles[i] = (String) title;
    } else {
      String[] list = (String[]) title;
      for (int i = bsFrames.nextSetBit(0), n = 0; i >= 0; i = bsFrames
          .nextSetBit(i + 1))
        if (n < list.length)
          frameTitles[i] = list[n++];
    }
  }

  public String getFrameTitle(int modelIndex) {
    return (modelIndex >= 0 && modelIndex < mc ? frameTitles[modelIndex] : "");
  }

  public String getModelNumberForAtomLabel(int modelIndex) {
    return modelNumbersForAtomLabel[modelIndex];
  }

  /**
   * In versions earlier than 12.1.51, groups[] was a field of ModelCollection.
   * But this is not necessary, and it was wasting space. This method is only
   * called when polymers are recreated.
   * 
   * @return full array of groups in modelSet
   */
  Group[] getGroups() {
    int n = 0;
    for (int i = 0; i < mc; i++)
      n += am[i].getGroupCount();
    Group[] groups = new Group[n];
    for (int i = 0, iGroup = 0; i < mc; i++)
      for (int j = 0; j < am[i].chainCount; j++)
        for (int k = 0; k < am[i].chains[j].groupCount; k++) {
          groups[iGroup] = am[i].chains[j].groups[k];
          groups[iGroup].groupIndex = iGroup;
          iGroup++;
        }
    return groups;
  }

  /**
   * deprecated due to multimodel issues, but required by an interface -- do NOT
   * remove.
   * 
   * @return just the first unit cell
   * 
   */
  public float[] getUnitCellParams() {
    SymmetryInterface c = getUnitCell(0);
    return (c == null ? null : c.getUnitCellParams());
  }

  public boolean setCrystallographicDefaults() {
    return !haveBioModels && someModelsHaveSymmetry
        && someModelsHaveFractionalCoordinates;
  }

  public P3 getBoundBoxCenter(int modelIndex) {
    return (isJmolDataFrameForModel(modelIndex) ? new P3()
        : (getDefaultBoundBox() == null ? boxInfo : defaultBBox)
            .getBoundBoxCenter());
  }

  public V3 getBoundBoxCornerVector() {
    return boxInfo.getBoundBoxCornerVector();
  }

  public Point3fi[] getBBoxVertices() {
    return boxInfo.getBoundBoxVertices();
  }

  public void setBoundBox(T3 pt1, T3 pt2, boolean byCorner, float scale) {
    isBbcageDefault = false;
    bboxModels = null;
    bboxAtoms = null;
    boxInfo.setBoundBox(pt1, pt2, byCorner, scale);
  }

  public String getBoundBoxCommand(boolean withOptions) {
    if (!withOptions && bboxAtoms != null)
      return "boundbox " + Escape.eBS(bboxAtoms);
    ptTemp.setT(boxInfo.getBoundBoxCenter());
    V3 bbVector = boxInfo.getBoundBoxCornerVector();
    String s = (withOptions ? "boundbox " + Escape.eP(ptTemp) + " "
        + Escape.eP(bbVector) + "\n#or\n" : "");
    ptTemp.sub(bbVector);
    s += "boundbox corners " + Escape.eP(ptTemp) + " ";
    ptTemp.scaleAdd2(2, bbVector, ptTemp);
    float v = Math.abs(8 * bbVector.x * bbVector.y * bbVector.z);
    s += Escape.eP(ptTemp) + " # volume = " + v;
    return s;
  }

  public BS findAtomsInRectangle(Rectangle rect) {
    BS bsModels = vwr.getVisibleFramesBitSet();
    BS bs = new BS();
    for (int i = ac; --i >= 0;) {
      Atom atom = at[i];
      if (!bsModels.get(atom.mi))
        i = am[atom.mi].firstAtomIndex;
      else if (atom.checkVisible() && rect.contains(atom.sX, atom.sY))
        bs.set(i);
    }
    return bs;
  }

  public VDW getDefaultVdwType(int modelIndex) {
    return (!am[modelIndex].isBioModel ? VDW.AUTO_BABEL
        : am[modelIndex].hydrogenCount == 0 ? VDW.AUTO_JMOL : VDW.AUTO_BABEL); // RASMOL is too small
  }

  public boolean setRotationRadius(int modelIndex, float angstroms) {
    if (isJmolDataFrameForModel(modelIndex)) {
      am[modelIndex].defaultRotationRadius = angstroms;
      return false;
    }
    return true;
  }

  public float calcRotationRadius(int modelIndex, P3 center, boolean useBoundBox) {
    if (isJmolDataFrameForModel(modelIndex)) {
      float r = am[modelIndex].defaultRotationRadius;
      return (r == 0 ? 10 : r);
    }
    if (useBoundBox && getDefaultBoundBox() != null)
      return defaultBBox.getMaxDim() / 2 * 1.2f;
    float maxRadius = 0;
    for (int i = ac; --i >= 0;) {
      if (isJmolDataFrameForAtom(at[i])) {
        modelIndex = at[i].mi;
        while (i >= 0 && at[i].mi == modelIndex)
          i--;
        continue;
      }
      Atom atom = at[i];
      float distAtom = center.distance(atom);
      float outerVdw = distAtom + getRadiusVdwJmol(atom);
      if (outerVdw > maxRadius)
        maxRadius = outerVdw;
    }
    return (maxRadius == 0 ? 10 : maxRadius);
  }

  public void calcBoundBoxDimensions(BS bs, float scale) {
    if (bs != null && bs.nextSetBit(0) < 0)
      bs = null;
    if (bs == null && isBbcageDefault || ac == 0)
      return;
    if (getDefaultBoundBox() == null) {
      bboxModels = getModelBS(bboxAtoms = BSUtil.copy(bs), false);
      if (calcAtomsMinMax(bs, boxInfo) == ac)
        isBbcageDefault = true;
      if (bs == null) { // from modelLoader or reset
        if (unitCells != null)
          calcUnitCellMinMax();
      }
    } else {
      P3[] vertices = defaultBBox.getBoundBoxVertices();
      boxInfo.reset();
      for (int j = 0; j < 8; j++)
        boxInfo.addBoundBoxPoint(vertices[j]);
    }
    boxInfo.setBbcage(scale);
  }

  /**
   * The default bounding box is created when the LOAD .... FILL BOUNDBOX or
   * FILL UNITCELL is use.
   * 
   * @return default bounding box, possibly null
   */
  private BoxInfo getDefaultBoundBox() {
    T3[] bbox = (T3[]) getInfoM("boundbox");
    if (bbox == null)
      defaultBBox = null;
    else {
      if (defaultBBox == null)
        defaultBBox = new BoxInfo();
      defaultBBox.setBoundBoxFromOABC(bbox);
    }
    return defaultBBox;
  }

  public BoxInfo getBoxInfo(BS bs, float scale) {
    if (bs == null)
      return boxInfo;
    BoxInfo bi = new BoxInfo();
    calcAtomsMinMax(bs, bi);
    bi.setBbcage(scale);
    return bi;
  }

  public int calcAtomsMinMax(BS bs, BoxInfo boxInfo) {
    boxInfo.reset();
    int nAtoms = 0;
    boolean isAll = (bs == null);
    int i0 = (isAll ? ac - 1 : bs.nextSetBit(0));
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bs.nextSetBit(i + 1))) {
      nAtoms++;
      if (!isJmolDataFrameForAtom(at[i]))
        boxInfo.addBoundBoxPoint(at[i]);
    }
    return nAtoms;
  }

  private void calcUnitCellMinMax() {
    P3 pt = new P3();
    for (int i = 0; i < mc; i++) {
      if (!unitCells[i].getCoordinatesAreFractional())
        continue;
      P3[] vertices = unitCells[i].getUnitCellVerticesNoOffset();
      P3 offset = unitCells[i].getCartesianOffset();
      for (int j = 0; j < 8; j++) {
        pt.add2(offset, vertices[j]);
        boxInfo.addBoundBoxPoint(pt);
      }
    }
  }

  public float calcRotationRadiusBs(BS bs) {
    // Eval getZoomFactor
    P3 center = getAtomSetCenter(bs);
    float maxRadius = 0;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      Atom atom = at[i];
      float distAtom = center.distance(atom);
      float outerVdw = distAtom + getRadiusVdwJmol(atom);
      if (outerVdw > maxRadius)
        maxRadius = outerVdw;
    }
    return (maxRadius == 0 ? 10 : maxRadius);
  }

  /**
   * 
   * @param vAtomSets
   * @param addCenters
   * @return array of two lists of points, centers first if desired
   */

  public P3[][] getCenterAndPoints(Lst<Object[]> vAtomSets, boolean addCenters) {
    BS bsAtoms1, bsAtoms2;
    int n = (addCenters ? 1 : 0);
    for (int ii = vAtomSets.size(); --ii >= 0;) {
      Object[] bss = vAtomSets.get(ii);
      bsAtoms1 = (BS) bss[0];
      if (bss[1] instanceof BS) {
        bsAtoms2 = (BS) bss[1];
        n += Math.min(bsAtoms1.cardinality(), bsAtoms2.cardinality());
      } else {
        n += Math.min(bsAtoms1.cardinality(), ((P3[]) bss[1]).length);
      }
    }
    P3[][] points = new P3[2][n];
    if (addCenters) {
      points[0][0] = new P3();
      points[1][0] = new P3();
    }
    for (int ii = vAtomSets.size(); --ii >= 0;) {
      Object[] bss = vAtomSets.get(ii);
      bsAtoms1 = (BS) bss[0];
      if (bss[1] instanceof BS) {
        bsAtoms2 = (BS) bss[1];
        for (int i = bsAtoms1.nextSetBit(0), j = bsAtoms2.nextSetBit(0); i >= 0
            && j >= 0; i = bsAtoms1.nextSetBit(i + 1), j = bsAtoms2
            .nextSetBit(j + 1)) {
          points[0][--n] = at[i];
          points[1][n] = at[j];
          if (addCenters) {
            points[0][0].add(at[i]);
            points[1][0].add(at[j]);
          }
        }
      } else {
        P3[] coords = (P3[]) bss[1];
        for (int i = bsAtoms1.nextSetBit(0), j = 0; i >= 0 && j < coords.length; i = bsAtoms1
            .nextSetBit(i + 1), j++) {
          points[0][--n] = at[i];
          points[1][n] = coords[j];
          if (addCenters) {
            points[0][0].add(at[i]);
            points[1][0].add(coords[j]);
          }
        }
      }
    }
    if (addCenters) {
      points[0][0].scale(1f / (points[0].length - 1));
      points[1][0].scale(1f / (points[1].length - 1));
    }
    return points;
  }

  public P3 getAtomSetCenter(BS bs) {
    P3 ptCenter = new P3();
    int nPoints = 0;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      if (!isJmolDataFrameForAtom(at[i])) {
        nPoints++;
        ptCenter.add(at[i]);
      }
    }
    if (nPoints > 1)
      ptCenter.scale(1.0f / nPoints);
    return ptCenter;
  }

  public P3 getAverageAtomPoint() {
//    if (averageAtomPoint == null)
//      averageAtomPoint = getAtomSetCenter(vwr.getAllAtoms());
    return getAtomSetCenter(vwr.bsA());//averageAtomPoint;
  }

  protected void setAPm(BS bs, int tok, int iValue, float fValue,
                        String sValue, float[] values, String[] list) {
    setAPa(bs, tok, iValue, fValue, sValue, values, list);
    switch (tok) {
    case T.valence:
    case T.formalcharge:
      if (vwr.getBoolean(T.smartaromatic))
        assignAromaticBondsBs(true, null);
      break;
    }
  }

  public StateScript addStateScript(String script1, BS bsBonds, BS bsAtoms1,
                                    BS bsAtoms2, String script2,
                                    boolean addFrameNumber,
                                    boolean postDefinitions) {
    int iModel = vwr.am.cmi;
    if (addFrameNumber) {
      if (thisStateModel != iModel)
        script1 = "frame "
            + (iModel < 0 ? "all #" + iModel : getModelNumberDotted(iModel))
            + ";\n  " + script1;
      thisStateModel = iModel;
    } else {
      thisStateModel = -1;
    }
    StateScript stateScript = new StateScript(thisStateModel, script1, bsBonds,
        bsAtoms1, bsAtoms2, script2, postDefinitions);
    if (stateScript.isValid()) {
      stateScripts.addLast(stateScript);
    }
    return stateScript;
  }

  void freezeModels() {
    haveBioModels = false;
    for (int iModel = mc; --iModel >= 0;)
      haveBioModels |= am[iModel].freeze();
  }

  public Map<STR, float[]> getStructureList() {
    return vwr.getStructureList();
  }

  public Object getInfoM(String keyName) {
    // the preferred method now
    return (msInfo == null ? null : msInfo.get(keyName));
  }

  public boolean getMSInfoB(String keyName) {
    Object val = getInfoM(keyName);
    return (val instanceof Boolean && ((Boolean) val).booleanValue());
  }

  /**
   * could be the base model or one of the subframes
   * 
   * @param modelIndex
   * @return is any part of a trajectory
   */
  public boolean isTrajectory(int modelIndex) {
    return am[modelIndex].isTrajectory;
  }

  public boolean isTrajectorySubFrame(int i) {
    return (am[i].trajectoryBaseIndex != i);
  }

  public boolean isTrajectoryMeasurement(int[] countPlusIndices) {
    return (trajectory != null && trajectory.hasMeasure(countPlusIndices));
  }

  public BS getModelBS(BS atomList, boolean allTrajectories) {
    BS bs = new BS();
    int modelIndex = 0;
    boolean isAll = (atomList == null);
    allTrajectories &= (trajectory != null);
    int i0 = (isAll ? 0 : atomList.nextSetBit(0));
    for (int i = i0; i >= 0 && i < ac; i = (isAll ? i + 1 : atomList
        .nextSetBit(i + 1))) {
      bs.set(modelIndex = at[i].mi);
      if (allTrajectories)
        trajectory.getModelBS(modelIndex, bs);
      i = am[modelIndex].firstAtomIndex + am[modelIndex].act - 1;
    }
    return bs;
  }

  /**
   * only some models can be iterated through. models for which
   * trajectoryBaseIndexes[i] != i are trajectories only
   * 
   * @param allowJmolData
   * @return bitset of models
   */
  public BS getIterativeModels(boolean allowJmolData) {
    BS bs = new BS();
    for (int i = 0; i < mc; i++) {
      if (!allowJmolData && isJmolDataFrameForModel(i))
        continue;
      if (!isTrajectorySubFrame(i))
        bs.set(i);
    }
    return bs;
  }

  public void fillAtomData(AtomData atomData, int mode) {
    if ((mode & AtomData.MODE_FILL_MOLECULES) != 0) {
      getMolecules();
      atomData.bsMolecules = new BS[molecules.length];
      atomData.atomMolecule = new int[ac];
      BS bs;
      for (int i = 0; i < molecules.length; i++) {
        bs = atomData.bsMolecules[i] = molecules[i].atomList;
        for (int iAtom = bs.nextSetBit(0); iAtom >= 0; iAtom = bs
            .nextSetBit(iAtom + 1))
          atomData.atomMolecule[iAtom] = i;
      }
    }
    if ((mode & AtomData.MODE_GET_ATTACHED_HYDROGENS) != 0) {
      int[] nH = new int[1];
      atomData.hAtomRadius = vwr.getVanderwaalsMar(1) / 1000f;
      atomData.hAtoms = calculateHydrogens(atomData.bsSelected, nH, null, AtomCollection.CALC_H_JUSTC);
      atomData.hydrogenAtomCount = nH[0];
      return;
    }
    if (atomData.modelIndex < 0)
      atomData.firstAtomIndex = (atomData.bsSelected == null ? 0 : Math.max(0,
          atomData.bsSelected.nextSetBit(0)));
    else
      atomData.firstAtomIndex = am[atomData.modelIndex].firstAtomIndex;
    atomData.lastModelIndex = atomData.firstModelIndex = (ac == 0 ? 0
        : at[atomData.firstAtomIndex].mi);
    atomData.modelName = getModelNumberDotted(atomData.firstModelIndex);
    fillADa(atomData, mode);
  }

  public String getModelNumberDotted(int modelIndex) {
    return (mc < 1 || modelIndex >= mc || modelIndex < 0 ? "" : Escape
        .escapeModelFileNumber(modelFileNumbers[modelIndex]));
  }

  public int getModelNumber(int modelIndex) {
    return modelNumbers[modelIndex == Integer.MAX_VALUE ? mc - 1 : modelIndex];
  }

  public String getModelProperty(int modelIndex, String property) {
    Properties props = am[modelIndex].properties;
    return props == null ? null : props.getProperty(property);
  }

  public Map<String, Object> getModelAuxiliaryInfo(int modelIndex) {
    return (modelIndex < 0 ? null : am[modelIndex].auxiliaryInfo);
  }

  public void setInfo(int modelIndex, Object key, Object value) {
    if (modelIndex >= 0 && modelIndex < mc)
      am[modelIndex].auxiliaryInfo.put((String) key, value);
  }

  public Object getInfo(int modelIndex, String key) {
    return (modelIndex < 0 ? null : am[modelIndex].auxiliaryInfo.get(key));
  }

  protected boolean getInfoB(int modelIndex, String keyName) {
    Map<String, Object> info = am[modelIndex].auxiliaryInfo;
    return (info != null && info.containsKey(keyName) && ((Boolean) info
        .get(keyName)).booleanValue());
  }

  protected int getInfoI(int modelIndex, String keyName) {
    Map<String, Object> info = am[modelIndex].auxiliaryInfo;
    if (info != null && info.containsKey(keyName)) {
      return ((Integer) info.get(keyName)).intValue();
    }
    return Integer.MIN_VALUE;
  }

  public int getInsertionCountInModel(int modelIndex) {
    return am[modelIndex].insertionCount;
  }

  public static int modelFileNumberFromFloat(float fDotM) {
    //only used in the case of select model = someVariable
    //2.1 and 2.10 will be ambiguous and reduce to 2.1  

    int file = (int) Math.floor(fDotM);
    int model = (int) Math.floor((fDotM - file + 0.00001) * 10000);
    while (model != 0 && model % 10 == 0)
      model /= 10;
    return file * 1000000 + model;
  }

  public int getChainCountInModelWater(int modelIndex, boolean countWater) {
    if (modelIndex < 0) {
      int chainCount = 0;
      for (int i = mc; --i >= 0;)
        chainCount += am[i].getChainCount(countWater);
      return chainCount;
    }
    return am[modelIndex].getChainCount(countWater);
  }

  public int getGroupCountInModel(int modelIndex) {
    if (modelIndex < 0) {
      int groupCount = 0;
      for (int i = mc; --i >= 0;)
        groupCount += am[i].getGroupCount();
      return groupCount;
    }
    return am[modelIndex].getGroupCount();
  }

  public void calcSelectedGroupsCount() {
    BS bsSelected = vwr.bsA();
    for (int i = mc; --i >= 0;)
      am[i].calcSelectedGroupsCount(bsSelected);
  }

  public boolean isJmolDataFrameForModel(int modelIndex) {
    return (modelIndex >= 0 && modelIndex < mc && am[modelIndex].isJmolDataFrame);
  }

  private boolean isJmolDataFrameForAtom(Atom atom) {
    return (am[atom.mi].isJmolDataFrame);
  }

  public void setJmolDataFrame(String type, int modelIndex, int modelDataIndex) {
    Model model = am[type == null ? am[modelDataIndex].dataSourceFrame
        : modelIndex];
    if (type == null) {
      //leaving a data frame -- just set generic to this one if quaternion
      type = am[modelDataIndex].jmolFrameType;
    }
    if (modelIndex >= 0) {
      if (model.dataFrames == null) {
        model.dataFrames = new Hashtable<String, Integer>();
      }
      am[modelDataIndex].dataSourceFrame = modelIndex;
      am[modelDataIndex].jmolFrameType = type;
      model.dataFrames.put(type, Integer.valueOf(modelDataIndex));
    }
    if (type.startsWith("quaternion") && type.indexOf("deriv") < 0) { //generic quaternion
      type = type.substring(0, type.indexOf(" "));
      model.dataFrames.put(type, Integer.valueOf(modelDataIndex));
    }
  }

  public int getJmolDataFrameIndex(int modelIndex, String type) {
    if (am[modelIndex].dataFrames == null) {
      return -1;
    }
    Integer index = am[modelIndex].dataFrames.get(type);
    return (index == null ? -1 : index.intValue());
  }

  protected void clearDataFrameReference(int modelIndex) {
    for (int i = 0; i < mc; i++) {
      Map<String, Integer> df = am[i].dataFrames;
      if (df == null) {
        continue;
      }
      Iterator<Integer> e = df.values().iterator();
      while (e.hasNext()) {
        if ((e.next()).intValue() == modelIndex) {
          e.remove();
        }
      }
    }
  }

  public String getJmolFrameType(int modelIndex) {
    return (modelIndex >= 0 && modelIndex < mc ? am[modelIndex].jmolFrameType
        : "modelSet");
  }

  public int getJmolDataSourceFrame(int modelIndex) {
    return (modelIndex >= 0 && modelIndex < mc ? am[modelIndex].dataSourceFrame
        : -1);
  }

  public void saveModelOrientation(int modelIndex, Orientation orientation) {
    am[modelIndex].orientation = orientation;
  }

  public Orientation getModelOrientation(int modelIndex) {
    return am[modelIndex].orientation;
  }

  /*
   final static String[] pdbRecords = { "ATOM  ", "HELIX ", "SHEET ", "TURN  ",
   "MODEL ", "SCALE",  "HETATM", "SEQRES",
   "DBREF ", };
   */

  public String getPDBHeader(int modelIndex) {
    return (am[modelIndex].isBioModel ? ((BioModel) am[modelIndex])
        .getFullPDBHeader() : getFileHeader(modelIndex));
  }

  public String getFileHeader(int modelIndex) {
    if (modelIndex < 0)
      return "";
    if (am[modelIndex].isBioModel)
      return getPDBHeader(modelIndex);
    String info = (String) getInfo(modelIndex, "fileHeader");
    if (info == null)
      info = modelSetName;
    if (info != null)
      return info;
    return "no header information found";
  }

  //////////////  individual models ////////////////

  public int getAltLocCountInModel(int modelIndex) {
    return am[modelIndex].altLocCount;
  }

  public int getAltLocIndexInModel(int modelIndex, char alternateLocationID) {
    if (alternateLocationID == '\0') {
      return 0;
    }
    String altLocList = getAltLocListInModel(modelIndex);
    if (altLocList.length() == 0) {
      return 0;
    }
    return altLocList.indexOf(alternateLocationID) + 1;
  }

  public int getInsertionCodeIndexInModel(int modelIndex, char insertionCode) {
    if (insertionCode == '\0')
      return 0;
    String codeList = getInsertionListInModel(modelIndex);
    if (codeList.length() == 0)
      return 0;
    return codeList.indexOf(insertionCode) + 1;
  }

  public String getAltLocListInModel(int modelIndex) {
    String str = (String) getInfo(modelIndex, "altLocs");
    return (str == null ? "" : str);
  }

  private String getInsertionListInModel(int modelIndex) {
    String str = (String) getInfo(modelIndex, "insertionCodes");
    return (str == null ? "" : str);
  }

  public int getModelSymmetryCount(int modelIndex) {
    return (am[modelIndex].biosymmetryCount > 0 ? am[modelIndex].biosymmetryCount
        : unitCells == null || unitCells[modelIndex] == null ? 0
            : unitCells[modelIndex].getSpaceGroupOperationCount());
  }

  public int[] getModelCellRange(int modelIndex) {
    return (unitCells == null ? null : unitCells[modelIndex].getCellRange());
  }

  public int getLastVibrationVector(int modelIndex, int tok) {
    if (vibrations != null && modelIndex < vwr.ms.mc) {
      Vibration v;
      int a1 = (modelIndex < 0 || isTrajectory(modelIndex)
          || modelIndex >= mc - 1 ? ac : am[modelIndex + 1].firstAtomIndex);
      int a0 = (modelIndex <= 0 ? 0 : am[modelIndex].firstAtomIndex);
      for (int i = a1; --i >= a0;) {
        if ((modelIndex < 0 || at[i].mi == modelIndex)
            && ((tok == T.modulation || tok == 0)
                && (v = (Vibration) getModulation(i)) != null || (tok == T.vibration || tok == 0)
                && (v = getVibration(i, false)) != null) && v.isNonzero())
          return i;
      }
    }
    return -1;
  }

  public Lst<Object> getModulationList(BS bs, char type, P3 t456) {
    Lst<Object> list = new Lst<Object>();
    if (vibrations != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        if (vibrations[i] instanceof JmolModulationSet)
          list.addLast(((JmolModulationSet) vibrations[i]).getModulation(type,
              t456));
        else
          list.addLast(Float.valueOf(type == 'O' ? Float.NaN : -1));
    return list;
  }

  public BS getElementsPresentBitSet(int modelIndex) {
    if (modelIndex >= 0)
      return elementsPresent[modelIndex];
    BS bs = new BS();
    for (int i = 0; i < mc; i++)
      bs.or(elementsPresent[i]);
    return bs;
  }

  ///////// molecules /////////

  public int getMoleculeIndex(int atomIndex, boolean inModel) {
    //ColorManager
    if (moleculeCount == 0)
      getMolecules();
    for (int i = 0; i < moleculeCount; i++) {
      if (molecules[i].atomList.get(atomIndex))
        return (inModel ? molecules[i].indexInModel : i);
    }
    return 0;
  }

  /**
   * return cumulative sum of all atoms in molecules containing these atoms
   * 
   * @param bs
   * @return bitset of atoms   
   */
  public BS getMoleculeBitSet(BS bs) {
    if (moleculeCount == 0)
      getMolecules();
    BS bsResult = BSUtil.copy(bs);
    BS bsInitial = BSUtil.copy(bs);
    int i = 0;
    BS bsTemp = new BS();
    while ((i = bsInitial.length() - 1) >= 0) {
      bsTemp = getMoleculeBitSetForAtom(i);
      if (bsTemp == null) {
        // atom has been deleted
        bsInitial.clear(i);
        bsResult.clear(i);
        continue;
      }
      bsInitial.andNot(bsTemp);
      bsResult.or(bsTemp);
    }
    return bsResult;
  }

  public BS getMoleculeBitSetForAtom(int atomIndex) {
    if (moleculeCount == 0)
      getMolecules();
    for (int i = 0; i < moleculeCount; i++)
      if (molecules[i].atomList.get(atomIndex))
        return molecules[i].atomList;
    return null;
  }

  public V3 getModelDipole(int modelIndex) {
    if (modelIndex < 0)
      return null;
    V3 dipole = (V3) getInfo(modelIndex, "dipole");
    if (dipole == null)
      dipole = (V3) getInfo(modelIndex, "DIPOLE_VEC");
    return dipole;
  }

  public V3 calculateMolecularDipole(int modelIndex, BS bsAtoms) throws JmolAsyncException {
    if (bsAtoms != null) {
      int ia = bsAtoms.nextSetBit(0);
      if (ia < 0)
        return null;
      modelIndex = at[ia].mi;
    }
    if (modelIndex < 0)
      return null;
    int nPos = 0;
    int nNeg = 0;
    float cPos = 0;
    float cNeg = 0;
    V3 pos = new V3();
    V3 neg = new V3();
    if (bsAtoms == null)
      bsAtoms = getModelAtomBitSetIncludingDeleted(-1, false);
    vwr.getOrCalcPartialCharges(am[modelIndex].bsAtoms, null);
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
      if (at[i].mi != modelIndex || at[i].isDeleted()) {
        continue;
      }
      float c = partialCharges[i];
      if (c < 0) {
        nNeg++;
        cNeg += c;
        neg.scaleAdd2(c, at[i], neg);
      } else if (c > 0) {
        nPos++;
        cPos += c;
        pos.scaleAdd2(c, at[i], pos);
      }
    }
    if (Math.abs(cPos + cNeg) > 0.015) { // BH Jmol 14.22.2 was 0.01, but PubChem charges are only to 0.01 precision
      Logger.info("Dipole calculation requires balanced charges: " + cPos + " "
          + cNeg);
      return null;
    }
    if (nNeg == 0 || nPos == 0)
      return null;
    pos.add(neg);
    pos.scale(4.8f); //1e-10f * 1.6e-19f/ 3.336e-30f;
    // 1 Debye = 3.336e-30 Coulomb-meter; C_e = 1.6022e-19 C
    return pos;
  }

  public int getMoleculeCountInModel(int modelIndex) {
    //ColorManager
    //not implemented for pop-up menu -- will slow it down.
    int n = 0;
    if (moleculeCount == 0)
      getMolecules();
    if (modelIndex < 0)
      return moleculeCount;
    for (int i = 0; i < mc; i++) {
      if (modelIndex == i)
        n += am[i].moleculeCount;
    }
    return n;
  }

  public void calcSelectedMoleculesCount() {
    BS bsSelected = vwr.bsA();
    if (moleculeCount == 0)
      getMolecules();
    selectedMolecules.xor(selectedMolecules);
    //selectedMoleculeCount = 0;
    BS bsTemp = new BS();
    for (int i = 0; i < moleculeCount; i++) {
      BSUtil.copy2(bsSelected, bsTemp);
      bsTemp.and(molecules[i].atomList);
      if (bsTemp.length() > 0) {
        selectedMolecules.set(i);
        //selectedMoleculeCount++;
      }
    }
  }

  /**
   * deletes molecules based on: CENTROID -- molecular centroid is not in unit
   * cell CENTROID PACKED -- all molecule atoms are not in unit cell
   * 
   * @param bs
   * @param minmax
   *        fractional [xmin, ymin, zmin, xmax, ymax, zmax, 1=packed]
   */
  public void setCentroid(BS bs, int[] minmax) {
    BS bsDelete = getNotInCentroid(bs, minmax);
    if (bsDelete != null && bsDelete.nextSetBit(0) >= 0)
      vwr.deleteAtoms(bsDelete, false);
  }

  private BS getNotInCentroid(BS bs, int[] minmax) {
    int iAtom0 = bs.nextSetBit(0);
    if (iAtom0 < 0)
      return null;
    SymmetryInterface uc = getUnitCell(at[iAtom0].mi);
    return (uc == null ? null : uc.notInCentroid(this, bs, minmax));
  }

  public JmolMolecule[] getMolecules() {
    if (moleculeCount > 0)
      return molecules;
    if (molecules == null)
      molecules = new JmolMolecule[4];
    moleculeCount = 0;
    Model m = null;
    BS[] bsModelAtoms = new BS[mc];
    Lst<BS> biobranches = null;
    for (int i = 0; i < mc; i++) {
      // TODO: Trajectories?
      bsModelAtoms[i] = vwr.getModelUndeletedAtomsBitSet(i);
      m = am[i];
      m.moleculeCount = 0;
      biobranches = (m.isBioModel ? ((BioModel) m)
          .getBioBranches(biobranches) : null);
    }
    // problem, as with 1gzx, is that this does not include non-protein cofactors that are 
    // covalently bonded. So we indicate a set of "biobranches" in JmolMolecule.getMolecules
    molecules = JmolMolecule.getMolecules(at, bsModelAtoms, biobranches, null);
    moleculeCount = molecules.length;
    for (int i = moleculeCount; --i >= 0;) {
      m = am[molecules[i].modelIndex];
      m.firstMoleculeIndex = i;
      m.moleculeCount++;
    }
    return molecules;
  }

  //////////// iterators //////////

  protected void initializeBspf() {
    if (bspf != null && bspf.isValid)
      return;
    if (showRebondTimes)
      Logger.startTimer("build bspf");
    Bspf bspf = new Bspf(3);
    if (Logger.debugging)
      Logger.debug("sequential bspt order");
    BS bsNew = BS.newN(mc);
    for (int i = ac; --i >= 0;) {
      // important that we go backward here, because we are going to 
      // use System.arrayCopy to expand the array ONCE only
      Atom atom = at[i];
      if (!atom.isDeleted() && !isTrajectorySubFrame(atom.mi)) {
        bspf.addTuple(am[atom.mi].trajectoryBaseIndex, atom);
        bsNew.set(atom.mi);
      }
    }
    //      }
    if (showRebondTimes) {
      Logger.checkTimer("build bspf", false);
      bspf.stats();
      //        bspf.dump();
    }
    for (int i = bsNew.nextSetBit(0); i >= 0; i = bsNew.nextSetBit(i + 1))
      bspf.validateModel(i, true);
    bspf.isValid = true;
    this.bspf = bspf;

  }

  protected void initializeBspt(int modelIndex) {
    initializeBspf();
    if (bspf.isInitializedIndex(modelIndex))
      return;
    bspf.initialize(modelIndex, at,
        vwr.getModelUndeletedAtomsBitSet(modelIndex));
  }

  public void setIteratorForPoint(AtomIndexIterator iterator, int modelIndex,
                                  T3 pt, float distance) {
    if (modelIndex < 0) {
      iterator.setCenter(pt, distance);
      return;
    }
    initializeBspt(modelIndex);
    iterator.setModel(this, modelIndex, am[modelIndex].firstAtomIndex,
        Integer.MAX_VALUE, pt, distance, null);
  }

  public void setIteratorForAtom(AtomIndexIterator iterator, int modelIndex,
                                 int atomIndex, float distance, RadiusData rd) {
    if (modelIndex < 0)
      modelIndex = at[atomIndex].mi;
    modelIndex = am[modelIndex].trajectoryBaseIndex;
    initializeBspt(modelIndex);
    iterator.setModel(this, modelIndex, am[modelIndex].firstAtomIndex,
        atomIndex, at[atomIndex], distance, rd);
  }

  /**
   * @param bsSelected
   * @param isGreaterOnly
   * @param modelZeroBased
   * @param hemisphereOnly
   * @param isMultiModel
   * @return an iterator
   */
  public AtomIndexIterator getSelectedAtomIterator(BS bsSelected,
                                                   boolean isGreaterOnly,
                                                   boolean modelZeroBased,
                                                   boolean hemisphereOnly,
                                                   boolean isMultiModel) {
    //EnvelopeCalculation, IsoSolventReader
    // This iterator returns only atoms OTHER than the atom specified
    // and with the specified restrictions. 
    // Model zero-based means the index returned is within the model, 
    // not the full atom set. broken in 12.0.RC6; repaired in 12.0.RC15

    initializeBspf();
    AtomIteratorWithinModel iter;
    if (isMultiModel) {
      BS bsModels = getModelBS(bsSelected, false);
      for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels
          .nextSetBit(i + 1))
        initializeBspt(i);
      iter = new AtomIteratorWithinModelSet(bsModels);
    } else {
      iter = new AtomIteratorWithinModel();
    }
    iter.initialize(bspf, bsSelected, isGreaterOnly, modelZeroBased,
        hemisphereOnly, vwr.isParallel());
    return iter;
  }

  ////////// bonds /////////

  @Override
  public int getBondCountInModel(int modelIndex) {
    return (modelIndex < 0 ? bondCount : am[modelIndex].getBondCount());
  }

  public int getAtomCountInModel(int modelIndex) {
    return (modelIndex < 0 ? ac : am[modelIndex].act);
  }

  /**
   * note -- this method returns ALL atoms, including deleted.
   * 
   * @param bsModels
   * @return bitset of atoms
   */
  public BS getModelAtomBitSetIncludingDeletedBs(BS bsModels) {
    BS bs = new BS();
    if (bsModels == null && bsAll == null)
      bsAll = BSUtil.setAll(ac);
    if (bsModels == null)
      bs.or(bsAll);
    else
      for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels
          .nextSetBit(i + 1))
        bs.or(getModelAtomBitSetIncludingDeleted(i, false));
    return bs;
  }

  /**
   * Note that this method returns all atoms, included deleted ones. If you
   * don't want deleted atoms, then use
   * vwr.getModelAtomBitSetUndeleted(modelIndex, TRUE)
   * 
   * @param modelIndex
   * @param asCopy
   *        MUST BE TRUE IF THE BITSET IS GOING TO BE MODIFIED!
   * @return either the actual bitset or a copy
   */
  public BS getModelAtomBitSetIncludingDeleted(int modelIndex, boolean asCopy) {
    BS bs = (modelIndex < 0 ? bsAll : am[modelIndex].bsAtoms);
    if (bs == null)
      bs = bsAll = BSUtil.setAll(ac);
    return (asCopy ? BSUtil.copy(bs) : bs);
  }

  protected BS getAtomBitsMaybeDeleted(int tokType, Object specInfo) {
    BS bs;
    switch (tokType) {
    default:
      return getAtomBitsMDa(tokType, specInfo, bs = new BS());
    case T.domains:
    case T.validation:
    case T.dssr:
    case T.rna3d:
    case T.basepair:
    case T.sequence:
      bs = new BS();
      return (haveBioModels ? bioModelset.getAtomBitsStr(tokType,
          (String) specInfo, bs) : bs);
    case T.bonds:
    case T.isaromatic:
      return getAtomBitsMDb(tokType, specInfo);
    case T.boundbox:
      BoxInfo boxInfo = getBoxInfo((BS) specInfo, 1);
      bs = getAtomsWithin(boxInfo.getBoundBoxCornerVector().length() + 0.0001f,
          boxInfo.getBoundBoxCenter(), null, -1);
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        if (!boxInfo.isWithin(at[i]))
          bs.clear(i);
      return bs;
    case T.cell:
      // select cell=555 (NO NOT an absolute quantity)
      // select cell=1505050
      // select cell=1500500500
      bs = new BS();
      P3 pt = (P3) specInfo;
      boolean ignoreOffset = false;//!vwr.getBoolean(T.fractionalrelative);
      for (int i = ac; --i >= 0;)
        if (isInLatticeCell(i, pt, ptTemp2, ignoreOffset))
          bs.set(i);
      return bs;
    case T.centroid:
      // select centroid=555  -- like cell=555 but for whole molecules
      // if it  is one full molecule, then return the EMPTY bitset      
      bs = BSUtil.newBitSet2(0, ac);
      P3 pt1 = (P3) specInfo;
      int[] minmax = new int[] { (int) pt1.x - 1, (int) pt1.y - 1, (int) pt1.z - 1, 
          (int) pt1.x, (int) pt1.y, (int) pt1.z, 0 };
      for (int i = mc; --i >= 0;) {
        SymmetryInterface uc = getUnitCell(i);
        if (uc == null) {
          BSUtil.andNot(bs, am[i].bsAtoms);
          continue;
        }
        bs.andNot(uc.notInCentroid(this, am[i].bsAtoms, minmax));
      }
      return bs;
    case T.molecule:
      return getMoleculeBitSet((BS) specInfo);
    case T.spec_seqcode_range:
      return getSelectCodeRange((int[]) specInfo);
    case T.specialposition:
      bs = BS.newN(ac);
      int modelIndex = -1;
      int nOps = 0;
      for (int i = ac; --i >= 0;) {
        Atom atom = at[i];
        BS bsSym = atom.atomSymmetry;
        if (bsSym != null) {
          if (atom.mi != modelIndex) {
            modelIndex = atom.mi;
            if (getModelCellRange(modelIndex) == null)
              continue;
            nOps = getModelSymmetryCount(modelIndex);
          }
          // special positions are characterized by
          // multiple operator bits set in the first (overall)
          // block of nOpts bits.
          // only strictly true with load {nnn mmm 1}

          int n = 0;
          for (int j = nOps; --j >= 0;)
            if (bsSym.get(j))
              if (++n > 1) {
                bs.set(i);
                break;
              }
        }
      }
      return bs;
    case T.symmetry:
      return BSUtil.copy(bsSymmetry == null ? bsSymmetry = BS.newN(ac)
          : bsSymmetry);
    case T.unitcell:
      // select UNITCELL (a relative quantity)
      bs = new BS();
      SymmetryInterface unitcell = vwr.getCurrentUnitCell();
      if (unitcell == null)
        return bs;
      ptTemp1.set(1, 1, 1);
      for (int i = ac; --i >= 0;)
        if (isInLatticeCell(i, ptTemp1, ptTemp2, false))
          bs.set(i);
      return bs;
    }
  }

  private BS getSelectCodeRange(int[] info) {
    // could be a PDB format file that is all UNK
    BS bs = new BS();
    int seqcodeA = info[0];
    int seqcodeB = info[1];
    int chainID = info[2];
    boolean caseSensitive = vwr.getBoolean(T.chaincasesensitive);
    if (chainID >= 0 && chainID < 300 && !caseSensitive)
      chainID = chainToUpper(chainID);
    for (int iModel = mc; --iModel >= 0;)
      if (am[iModel].isBioModel) {
        BioModel m = (BioModel) am[iModel];
        int id;
        for (int i = m.chainCount; --i >= 0;) {
          Chain chain = m.chains[i];
          if (chainID == -1 || chainID == (id = chain.chainID)
              || !caseSensitive && id > 0 && id < 300
              && chainID == chainToUpper(id)) {
            Group[] groups = chain.groups;
            int n = chain.groupCount;
            for (int index = 0; index >= 0;) {
              index = selectSeqcodeRange(groups, n, index, seqcodeA, seqcodeB,
                  bs);
            }
          }
        }
      }
    return bs;
  }

  private static int selectSeqcodeRange(Group[] groups, int n, int index,
                                        int seqcodeA, int seqcodeB, BS bs) {
    int seqcode, indexA, indexB, minDiff;
    boolean isInexact = false;
    for (indexA = index; indexA < n && groups[indexA].seqcode != seqcodeA; indexA++) {
    }
    if (indexA == n) {
      // didn't find A exactly -- go find the nearest that is GREATER than this value
      if (index > 0)
        return -1;
      isInexact = true;
      minDiff = Integer.MAX_VALUE;
      for (int i = n; --i >= 0;)
        if ((seqcode = groups[i].seqcode) > seqcodeA
            && (seqcode - seqcodeA) < minDiff) {
          indexA = i;
          minDiff = seqcode - seqcodeA;
        }
      if (minDiff == Integer.MAX_VALUE)
        return -1;
    }
    if (seqcodeB == Integer.MAX_VALUE) {
      indexB = n - 1;
      isInexact = true;
    } else {
      for (indexB = indexA; indexB < n && groups[indexB].seqcode != seqcodeB; indexB++) {
      }
      if (indexB == n) {
        // didn't find B exactly -- get the nearest that is LESS than this value
        if (index > 0)
          return -1;
        isInexact = true;
        minDiff = Integer.MAX_VALUE;
        for (int i = indexA; i < n; i++)
          if ((seqcode = groups[i].seqcode) < seqcodeB
              && (seqcodeB - seqcode) < minDiff) {
            indexB = i;
            minDiff = seqcodeB - seqcode;
          }
        if (minDiff == Integer.MAX_VALUE)
          return -1;
      }
    }
    for (int i = indexA; i <= indexB; ++i)
      groups[i].setAtomBits(bs);
    return (isInexact ? -1 : indexB + 1);
  }

  private boolean isInLatticeCell(int i, P3 cell, P3 ptTemp, boolean isAbsolute) {
    // this is the one method that allows for an absolute fractional cell business
    // but it is always called with isAbsolute FALSE.
    // so then it is determining values for select UNITCELL and the like.

    int iModel = at[i].mi;
    SymmetryInterface uc = getUnitCell(iModel);
    ptTemp.setT(at[i]);
    return (uc != null && uc.checkUnitCell(uc, cell, ptTemp, isAbsolute));
  }

  /**
   * Get atoms within a specific distance of any atom in a specific set of atoms
   * either within all models or within just the model(s) of those atoms
   * 
   * @param distance
   * @param bs
   * @param withinAllModels
   * @param rd
   * @return the set of atoms
   */
  public BS getAtomsWithinRadius(float distance, BS bs,
                                 boolean withinAllModels, RadiusData rd) {
    BS bsResult = new BS();
    BS bsCheck = getIterativeModels(false);
    bs = BSUtil.andNot(bs, vwr.slm.bsDeleted);
    AtomIndexIterator iter = getSelectedAtomIterator(null, false, false, false,
        false);
    if (withinAllModels) {
      boolean fixJavaFloat = !vwr.g.legacyJavaFloat;
      P3 ptTemp = new P3();
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        for (int iModel = mc; --iModel >= 0;) {
          if (!bsCheck.get(iModel))
            continue;
          if (distance < 0) {
            getAtomsWithin(distance,
                at[i].getFractionalUnitCoordPt(fixJavaFloat, true, ptTemp),
                bsResult, -1);
            continue;
          }
          setIteratorForAtom(iter, iModel, i, distance, rd);
          iter.addAtoms(bsResult);
        }
    } else {
      bsResult.or(bs);
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        if (distance < 0) {
          getAtomsWithin(distance, at[i], bsResult, at[i].mi);
          continue;
        }
        setIteratorForAtom(iter, -1, i, distance, rd);
        iter.addAtoms(bsResult);
      }
    }
    iter.release();
    return bsResult;
  }

  public BS getAtomsWithin(float distance, T3 coord, BS bsResult, int modelIndex) {

    if (bsResult == null)
      bsResult = new BS();

    if (distance < 0) { // check all unitCell distances
      distance = -distance;
      for (int i = ac; --i >= 0;) {
        Atom atom = at[i];
        if (modelIndex >= 0 && at[i].mi != modelIndex)
          continue;
        if (!bsResult.get(i)
            && atom.getFractionalUnitDistance(coord, ptTemp1, ptTemp2) <= distance)
          bsResult.set(atom.i);
      }
      return bsResult;
    }

    BS bsCheck = getIterativeModels(true);
    AtomIndexIterator iter = getSelectedAtomIterator(null, false, false, false,
        false);
    for (int iModel = mc; --iModel >= 0;) {
      if (!bsCheck.get(iModel) || am[iModel].bsAtoms.isEmpty())
        continue;
      setIteratorForAtom(iter, -1, am[iModel].firstAtomIndex, -1, null);
      iter.setCenter(coord, distance);
      iter.addAtoms(bsResult);
    }
    iter.release();
    return bsResult;
  }

  // ////////// Bonding methods ////////////

  public void deleteBonds(BS bsBonds, boolean isFullModel) {
    if (!isFullModel) {
      BS bsA = new BS();
      BS bsB = new BS();
      for (int i = bsBonds.nextSetBit(0); i >= 0; i = bsBonds.nextSetBit(i + 1)) {
        Atom atom1 = bo[i].atom1;
        if (am[atom1.mi].isModelKit)
          continue;
        bsA.clearAll();
        bsB.clearAll();
        bsA.set(atom1.i);
        bsB.set(bo[i].getAtomIndex2());
        addStateScript("connect ", null, bsA, bsB, "delete", false, true);
      }
    }
    dBb(bsBonds, isFullModel);
  }

  public int[] makeConnections2(float minD, float maxD, int order,
                                int connectOperation, BS bsA, BS bsB,
                                BS bsBonds, boolean isBonds, boolean addGroup,
                                float energy) {
    if (bsBonds == null)
      bsBonds = new BS();
    boolean matchAny = (order == Edge.BOND_ORDER_ANY);
    boolean matchNull = (order == Edge.BOND_ORDER_NULL);
    boolean isAtrop = (order == Edge.TYPE_ATROPISOMER);
    if (matchNull)
      order = Edge.BOND_COVALENT_SINGLE; //default for setting
    boolean matchHbond = Edge.isOrderH(order);
    boolean identifyOnly = false;
    boolean idOrModifyOnly = false;
    boolean createOnly = false;
    boolean autoAromatize = false;
    switch (connectOperation) {
    case T.delete:
      return deleteConnections(minD, maxD, order, bsA, bsB, isBonds, matchNull);
    case T.legacyautobonding:
    case T.auto:
      if (order != Edge.BOND_AROMATIC) {
        if (isBonds) {
          BS bs = bsA;
          bsA = new BS();
          bsB = new BS();
          for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            bsA.set(bo[i].atom1.i);
            bsB.set(bo[i].atom2.i);
          }
        }
        return new int[] {
            matchHbond ? autoHbond(bsA, bsB, false) : autoBondBs4(bsA, bsB,
                null, bsBonds, vwr.getMadBond(),
                connectOperation == T.legacyautobonding), 0 };
      }
      idOrModifyOnly = autoAromatize = true;
      break;
    case T.identify:
      identifyOnly = idOrModifyOnly = true;
      break;
    case T.modify:
      idOrModifyOnly = true;
      break;
    case T.create:
      createOnly = true;
      break;
    }
    boolean anyOrNoId = (!identifyOnly || matchAny);
    boolean notAnyAndNoId = (!identifyOnly && !matchAny);

    defaultCovalentMad = vwr.getMadBond();
    boolean minDIsFrac = (minD < 0);
    boolean maxDIsFrac = (maxD < 0);
    boolean isFractional = (minDIsFrac || maxDIsFrac);
    boolean checkDistance = (!isBonds
        || minD != JC.DEFAULT_MIN_CONNECT_DISTANCE || maxD != JC.DEFAULT_MAX_CONNECT_DISTANCE);
    if (checkDistance) {
      minD = fixD(minD, minDIsFrac);
      maxD = fixD(maxD, maxDIsFrac);
    }
    short mad = getDefaultMadFromOrder(order);
    int nNew = 0;
    int nModified = 0;
    Bond bondAB = null;
    Atom atomA = null;
    Atom atomB = null;
    char altloc = '\0';
    short newOrder = (short) (order | Edge.BOND_NEW);
    boolean isAromatic = ((order & Edge.BOND_AROMATIC_MASK) != 0);
    try {
      for (int i = bsA.nextSetBit(0); i >= 0; i = bsA.nextSetBit(i + 1)) {
        if (isBonds) {
          bondAB = bo[i];
          atomA = bondAB.atom1;
          atomB = bondAB.atom2;
        } else {
          atomA = at[i];
          if (atomA.isDeleted())
            continue;
          altloc = (isModulated(i) ? '\0' : atomA.altloc);
        }
        for (int j = (isBonds ? 0 : bsB.nextSetBit(0)); j >= 0; j = bsB
            .nextSetBit(j + 1)) {
          if (isBonds) {
            j = 2147483646; // Integer.MAX_VALUE - 1; one pass only
          } else {
            if (j == i)
              continue;
            atomB = at[j];
            if (atomA.mi != atomB.mi || atomB.isDeleted())
              continue;
            if (altloc != '\0' && altloc != atomB.altloc
                && atomB.altloc != '\0')
              continue;
            bondAB = atomA.getBond(atomB);
          }
          if ((bondAB == null ? idOrModifyOnly : createOnly)
              || checkDistance
              && !isInRange(atomA, atomB, minD, maxD, minDIsFrac, maxDIsFrac,
                  isFractional)
              || isAromatic && !allowAromaticBond(bondAB)
              )
            continue;
          if (bondAB == null) {
            bsBonds.set(bondAtoms(atomA, atomB, order, mad, bsBonds, energy,
                addGroup, true).index);
            nNew++;
          } else {
            if (notAnyAndNoId) {
              bondAB.setOrder(order);
              if (isAtrop) {
                haveAtropicBonds = true;
                bondAB.setAtropisomerOptions(bsA, bsB);
              }
              bsAromatic.clear(bondAB.index);
            }
            if (anyOrNoId || order == bondAB.order || newOrder == bondAB.order
                || matchHbond && bondAB.isHydrogen()) {
              bsBonds.set(bondAB.index);
              nModified++;
            }
          }
        }
      }
    } catch (Exception e) {
      // well, we tried -- probably ran over
    }
    if (autoAromatize)
      assignAromaticBondsBs(true, bsBonds);
    if (!identifyOnly)
      sm.setShapeSizeBs(JC.SHAPE_STICKS, Integer.MIN_VALUE, null, bsBonds);
    return new int[] { nNew, nModified };
  }

  public int autoBondBs4(BS bsA, BS bsB, BS bsExclude, BS bsBonds, short mad,
                         boolean preJmol11_9_24) {
    // unfortunately, 11.9.24 changed the order with which atoms were processed
    // for autobonding. This means that state script prior to that that use
    // select BOND will be misread by later version.
    // In addition, prior to Jmol 14.2.6, the difference between double (JavaScript) and 
    // float (Java) was not accounted for. However, this would only affect
    // very borderline cases -- where we are just at the edge of the bond tolerance. 
    // Is it worth it to fix? This is the question.
    if (preJmol11_9_24)
      return autoBond_Pre_11_9_24(bsA, bsB, bsExclude, bsBonds, mad);
    if (ac == 0)
      return 0;
    if (mad == 0)
      mad = 1;
    if (maxBondingRadius == PT.FLOAT_MIN_SAFE)
      findMaxRadii();
    float bondTolerance = vwr.getFloat(T.bondtolerance);
    float minBondDistance = vwr.getFloat(T.minbonddistance);
    float minBondDistance2 = minBondDistance * minBondDistance;
    int nNew = 0;
    if (showRebondTimes)// && Logger.debugging)
      Logger.startTimer("autobond");
    int lastModelIndex = -1;
    boolean isAll = (bsA == null);
    BS bsCheck;
    int i0;
    if (isAll) {
      i0 = 0;
      bsCheck = null;
    } else {
      if (bsA.equals(bsB)) {
        bsCheck = bsA;
      } else {
        bsCheck = BSUtil.copy(bsA);
        bsCheck.or(bsB);
      }
      i0 = bsCheck.nextSetBit(0);
    }
    AtomIndexIterator iter = getSelectedAtomIterator(null, false, false, true,
        false);
    boolean useOccupation = false;
    for (int i = i0; i >= 0 && i < ac; i = (isAll ? i + 1 : bsCheck
        .nextSetBit(i + 1))) {
      boolean isAtomInSetA = (isAll || bsA.get(i));
      boolean isAtomInSetB = (isAll || bsB.get(i));
      Atom atom = at[i];
      if (atom.isDeleted())
        continue;
      int modelIndex = atom.mi;
      // no connections allowed in a data frame
      if (modelIndex != lastModelIndex) {
        lastModelIndex = modelIndex;
        if (isJmolDataFrameForModel(modelIndex)) {
          i = am[modelIndex].firstAtomIndex + am[modelIndex].act - 1;
          continue;
        }
        useOccupation = getInfoB(modelIndex, "autoBondUsingOccupation"); // JANA reader
      }
      // Covalent bonds
      float myBondingRadius = atom.getBondingRadius();
      if (myBondingRadius == 0)
        continue;
      float myFormalCharge = atom.getFormalCharge();  
      boolean useCharge = (myFormalCharge != 0);
      if (useCharge)
        myFormalCharge = Math.signum(myFormalCharge);
      boolean isFirstExcluded = (bsExclude != null && bsExclude.get(i));
      float searchRadius = myBondingRadius + maxBondingRadius + bondTolerance;
      setIteratorForAtom(iter, -1, i, searchRadius, null);
      while (iter.hasNext()) {
        Atom atomNear = at[iter.next()];
        if (atomNear.isDeleted())
          continue;
        int j = atomNear.i;
        boolean isNearInSetA = (isAll || bsA.get(j));
        boolean isNearInSetB = (isAll || bsB.get(j));
        // BOTH must be excluded in order to ignore bonding
        if (!isNearInSetA && !isNearInSetB
            || !(isAtomInSetA && isNearInSetB || isAtomInSetB && isNearInSetA)
            || isFirstExcluded && bsExclude.get(j) || useOccupation
            && occupancies != null
            && (occupancies[i] < 50) != (occupancies[j] < 50)
            || useCharge && (Math.signum(atomNear.getFormalCharge()) == myFormalCharge)            
            )
          continue;
        int order = (isBondable(myBondingRadius, atomNear.getBondingRadius(),
            iter.foundDistance2(), minBondDistance2, bondTolerance) ? 1 : 0);
        if (order > 0 && autoBondCheck(atom, atomNear, order, mad, bsBonds))
          nNew++;
      }
      iter.release();
    }
    if (showRebondTimes)
      Logger.checkTimer("autoBond", false);
    return nNew;
  }

  public boolean isBondable(float bondingRadiusA, float bondingRadiusB,
                            float distance2, float minBondDistance2,
                            float bondTolerance) {
    if (bondingRadiusA == 0 || bondingRadiusB == 0
        || distance2 < minBondDistance2)
      return false;
    float maxAcceptable = bondingRadiusA + bondingRadiusB + bondTolerance;
    float maxAcceptable2 = maxAcceptable * maxAcceptable;
    return (distance2 <= maxAcceptable2);
  }

  private boolean maxBondWarned;

  private boolean autoBondCheck(Atom atomA, Atom atomB, int order, short mad,
                                BS bsBonds) {
    if (atomA.getCurrentBondCount() > JC.MAXIMUM_AUTO_BOND_COUNT
        || atomB.getCurrentBondCount() > JC.MAXIMUM_AUTO_BOND_COUNT) {
      if (!maxBondWarned)
        Logger.warn("maximum auto bond count reached");
      maxBondWarned = true;
      return false;
    }
    int formalChargeA = atomA.getFormalCharge();
    if (formalChargeA != 0) {
      int formalChargeB = atomB.getFormalCharge();
      if ((formalChargeA < 0 && formalChargeB < 0)
          || (formalChargeA > 0 && formalChargeB > 0))
        return false;
    }
    // don't connect differing altloc unless there are modulations
    if (atomA.altloc != atomB.altloc && atomA.altloc != '\0'
        && atomB.altloc != '\0' && getModulation(atomA.i) == null)
      return false;
    getOrAddBond(atomA, atomB, order, mad, bsBonds, 0, false);
    return true;
  }

  private int autoBond_Pre_11_9_24(BS bsA, BS bsB, BS bsExclude, BS bsBonds,
                                   short mad) {
    if (ac == 0)
      return 0;
    if (mad == 0)
      mad = 1;
    // null values for bitsets means "all"
    if (maxBondingRadius == PT.FLOAT_MIN_SAFE)
      findMaxRadii();
    float bondTolerance = vwr.getFloat(T.bondtolerance);
    float minBondDistance = vwr.getFloat(T.minbonddistance);
    float minBondDistance2 = minBondDistance * minBondDistance;
    int nNew = 0;
    initializeBspf();
    /*
     * miguel 2006 04 02
     * note that the way that these loops + iterators are constructed,
     * everything assumes that all possible pairs of atoms are going to
     * be looked at.
     * for example, the hemisphere iterator will only look at atom indexes
     * that are >= (or <= ?) the specified atom.
     * if we are going to allow arbitrary sets bsA and bsB, then this will
     * not work.
     * so, for now I will do it the ugly way.
     * maybe enhance/improve in the future.
     */
    int lastModelIndex = -1;
    for (int i = ac; --i >= 0;) {
      boolean isAtomInSetA = (bsA == null || bsA.get(i));
      boolean isAtomInSetB = (bsB == null || bsB.get(i));
      if (!isAtomInSetA && !isAtomInSetB)
        //|| bsExclude != null && bsExclude.get(i))
        continue;
      Atom atom = at[i];
      if (atom.isDeleted())
        continue;
      int modelIndex = atom.mi;
      //no connections allowed in a data frame
      if (modelIndex != lastModelIndex) {
        lastModelIndex = modelIndex;
        if (isJmolDataFrameForModel(modelIndex)) {
          for (; --i >= 0;)
            if (at[i].mi != modelIndex)
              break;
          i++;
          continue;
        }
      }
      // Covalent bonds
      float myBondingRadius = atom.getBondingRadius();
      if (myBondingRadius == 0)
        continue;
      float searchRadius = myBondingRadius + maxBondingRadius + bondTolerance;
      initializeBspt(modelIndex);
      CubeIterator iter = bspf.getCubeIterator(modelIndex);
      iter.initialize(atom, searchRadius, true);
      while (iter.hasMoreElements()) {
        Atom atomNear = (Atom) iter.nextElement();
        if (atomNear == atom || atomNear.isDeleted())
          continue;
        int atomIndexNear = atomNear.i;
        boolean isNearInSetA = (bsA == null || bsA.get(atomIndexNear));
        boolean isNearInSetB = (bsB == null || bsB.get(atomIndexNear));
        if (!isNearInSetA && !isNearInSetB || bsExclude != null
            && bsExclude.get(atomIndexNear) && bsExclude.get(i) //this line forces BOTH to be excluded in order to ignore bonding
        )
          continue;
        if (!(isAtomInSetA && isNearInSetB || isAtomInSetB && isNearInSetA))
          continue;
        int order = (isBondable(myBondingRadius, atomNear.getBondingRadius(),
            iter.foundDistance2(), minBondDistance2, bondTolerance) ? 1 : 0);
        if (order > 0) {
          if (autoBondCheck(atom, atomNear, order, mad, bsBonds))
            nNew++;
        }
      }
      iter.release();
    }
    return nNew;
  }

  /**
   * a generalized formation of HBONDS, carried out in relation to calculate
   * HBONDS {atomsFrom} {atomsTo}. The calculation can create pseudo-H bonds for
   * files that do not contain H atoms.
   * 
   * @param bsA
   *        "from" set (must contain H if that is desired)
   * @param bsB
   *        "to" set
   * @param onlyIfHaveCalculated
   * @return negative number of pseudo-hbonds or number of actual hbonds formed
   */
  public int autoHbond(BS bsA, BS bsB, boolean onlyIfHaveCalculated) {
    if (onlyIfHaveCalculated) {
      BS bsModels = getModelBS(bsA, false);
      for (int i = bsModels.nextSetBit(0); i >= 0
          && onlyIfHaveCalculated; i = bsModels.nextSetBit(i + 1))
        onlyIfHaveCalculated = !am[i].hasRasmolHBonds;
      if (onlyIfHaveCalculated)
        return 0;
    }
    boolean haveHAtoms = false;
    for (int i = bsA.nextSetBit(0); i >= 0; i = bsA.nextSetBit(i + 1))
      if (at[i].getElementNumber() == 1) {
        haveHAtoms = true;
        break;
      }
    BS bsHBonds = new BS();
    boolean useRasMol = vwr.getBoolean(T.hbondsrasmol);
    if (bsB == null || useRasMol && !haveHAtoms) {
      Logger.info((bsB == null ? "DSSP/DSSR " : "RasMol")
          + " pseudo-hbond calculation");
      calcRasmolHydrogenBonds(bsA, bsB, null, false, Integer.MAX_VALUE, false,
          bsHBonds);
      return -bsHBonds.cardinality();
    }
    Logger.info(haveHAtoms ? "Standard Hbond calculation"
        : "Jmol pseudo-hbond calculation");
    BS bsCO = null;
    if (!haveHAtoms) {
      bsCO = new BS();
      for (int i = bsA.nextSetBit(0); i >= 0; i = bsA.nextSetBit(i + 1)) {
        int atomID = at[i].atomID;
        switch (atomID) {
        case JC.ATOMID_TERMINATING_OXT:
        case JC.ATOMID_CARBONYL_OXYGEN:
        case JC.ATOMID_CARBONYL_OD1:
        case JC.ATOMID_CARBONYL_OD2:
        case JC.ATOMID_CARBONYL_OE1:
        case JC.ATOMID_CARBONYL_OE2:
          bsCO.set(i);
          break;
        }
      }
    }
    float dmax;
    float min2;
    if (haveHAtoms) {
      // no set maximumn for this anymore -- default is 2.5A
      dmax = vwr.getFloat(T.hbondhxdistancemaximum);
      min2 = 1f;
    } else {
      dmax = vwr.getFloat(T.hbondnodistancemaximum);
      // default 3.25 for pseudo; user can make longer or shorter
      min2 = hbondMinRasmol * hbondMinRasmol;
    }
    float max2 = dmax * dmax;
    float minAttachedAngle = (float) (vwr.getFloat(T.hbondsangleminimum)
        * Math.PI / 180);
    int nNew = 0;
    float d2 = 0;
    V3 v1 = new V3();
    V3 v2 = new V3();
    if (showRebondTimes && Logger.debugging)
      Logger.startTimer("hbond");
    P3 C = null;
    P3 D = null;
    AtomIndexIterator iter = getSelectedAtomIterator(bsB, false, false, false,
        false);
    for (int i = bsA.nextSetBit(0); i >= 0; i = bsA.nextSetBit(i + 1)) {
      Atom atom = at[i];
      int elementNumber = atom.getElementNumber();
      boolean isH = (elementNumber == 1);
      // If this is an H atom, then skip if we don't have H atoms in set A
      // If this is NOT an H atom, then skip if we have hydrogen atoms or this is not N or O
      if (isH ? !haveHAtoms
          : haveHAtoms || elementNumber != 7 && elementNumber != 8)
        continue;

      boolean firstIsCO;
      if (isH) {
        firstIsCO = false;
        Bond[] b = atom.bonds;
        if (b == null)
          continue;
        // must have OH or NH
        boolean isOK = false;
        for (int j = 0; !isOK && j < b.length; j++) {
          Atom a2 = b[j].getOtherAtom(atom);
          int element = a2.getElementNumber();
          isOK = (element == 7 || element == 8);
        }
        if (!isOK)
          continue;
      } else {
        // check if the first atom is C=O
        firstIsCO = bsCO.get(i);
      }
      setIteratorForAtom(iter, -1, atom.i, dmax, null);
      while (iter.hasNext()) {
        Atom atomNear = at[iter.next()];
        int elementNumberNear = atomNear.getElementNumber();
        if (atomNear == atom
            || (isH ? elementNumberNear == 1 : elementNumberNear != 7 && elementNumberNear != 8)
            || (d2 = iter.foundDistance2()) < min2 || d2 > max2
            || firstIsCO && bsCO.get(atomNear.i) 
            || atom.isBonded(atomNear)) {
          continue;
        }
        if (minAttachedAngle > 0) {
          v1.sub2(atom, atomNear);
          if ((D = checkMinAttachedAngle(atom, minAttachedAngle, v1, v2,
              haveHAtoms)) == null)
            continue;
          v1.scale(-1);
          if ((C = checkMinAttachedAngle(atomNear, minAttachedAngle, v1, v2,
              haveHAtoms)) == null)
            continue;
        }
        float energy = 0;
        short bo;
        if (isH && !Float.isNaN(C.x) && !Float.isNaN(D.x)) {
          bo = Edge.BOND_H_CALC;
          // (+) H .......... A (-)   
          //     |            |
          //     |            | 
          // (-) D            C (+)
          //
          //    AH args[0], CH args[1], CD args[2], DA args[3]
          //
          energy = HBond.getEnergy((float) Math.sqrt(d2), C.distance(atom),
              C.distance(D), atomNear.distance(D)) / 1000f;
        } else {
          bo = Edge.BOND_H_REGULAR;
        }
        bsHBonds.set(addHBond(atom, atomNear, bo, energy));
        nNew++;
      }
    }
    iter.release();
    sm.setShapeSizeBs(JC.SHAPE_STICKS, Integer.MIN_VALUE, null, bsHBonds);
    if (showRebondTimes)
      Logger.checkTimer("hbond", false);
    return (haveHAtoms ? nNew : -nNew);
  }

  private static P3 checkMinAttachedAngle(Atom atom1, float minAngle, V3 v1,
                                          V3 v2, boolean haveHAtoms) {
    Bond[] bonds = atom1.bonds;
    boolean ignore = true;
    Atom X = null;
    if (bonds != null && bonds.length > 0) {
      float dMin = Float.MAX_VALUE;
      for (int i = bonds.length; --i >= 0;)
        if (bonds[i].isCovalent()) {
          ignore = false;
          Atom atomA = bonds[i].getOtherAtom(atom1);
          if (!haveHAtoms && atomA.getElementNumber() == 1)
            continue;
          v2.sub2(atom1, atomA);
          float d = v2.angle(v1);
          if (d < minAngle)
            return null;
          if (d < dMin) {
            X = atomA;
            dMin = d;
          }
        }
    }
    return (ignore ? P3.new3(Float.NaN, 0, 0) : X);
  }

  //////////// state definition ///////////

  public void setStructureIndexes() {
    int id;
    int idnew = 0;
    int lastid = -1;
    int imodel = -1;
    int lastmodel = -1;
    for (int i = 0; i < ac; i++) {
      if ((imodel = at[i].mi) != lastmodel) {
        idnew = 0;
        lastmodel = imodel;
        lastid = -1;
      }
      if ((id = at[i].group.getStrucNo()) != lastid && id != 0) {
        at[i].group.setStrucNo(++idnew);
        lastid = idnew;
      }
    }
  }

  public String getModelInfoAsString() {
    SB sb = new SB().append("<models count=\"");
    sb.appendI(mc).append("\" modelSetHasVibrationVectors=\"")
        .append(modelSetHasVibrationVectors() + "\">\n<properties>");
    if (modelSetProperties != null) {
      Enumeration<?> e = modelSetProperties.propertyNames();
      while (e.hasMoreElements()) {
        String propertyName = (String) e.nextElement();
        sb.append("\n <property name=\"").append(propertyName)
            .append("\" value=")
            .append(PT.esc(modelSetProperties.getProperty(propertyName)))
            .append(" />");
      }
      sb.append("\n</properties>");
    }
    for (int i = 0; i < mc; ++i) {
      sb.append("\n<model index=\"").appendI(i).append("\" n=\"")
          .append(getModelNumberDotted(i)).append("\" id=")
          .append(PT.esc("" + getInfo(i, "modelID")));
      int ib = vwr.getJDXBaseModelIndex(i);
      if (ib != i)
        sb.append(" baseModelId=").append(
            PT.esc((String) getInfo(ib, "jdxModelID")));
      sb.append(" name=").append(PT.esc(getModelName(i))).append(" title=")
          .append(PT.esc(getModelTitle(i))).append(" hasVibrationVectors=\"")
          .appendB(vwr.modelHasVibrationVectors(i)).append("\" />");
    }
    sb.append("\n</models>");
    return sb.toString();
  }

  public String getSymmetryInfoAsString() {
    SB sb = new SB().append("Symmetry Information:");
    for (int i = 0; i < mc; ++i) {
      sb.append("\nmodel #").append(getModelNumberDotted(i)).append("; name=")
          .append(getModelName(i)).append("\n");
      SymmetryInterface unitCell = getUnitCell(i);
      sb.append(unitCell == null ? "no symmetry information" : unitCell
          .getSymmetryInfoStr());
    }
    return sb.toString();
  }

  public void createModels(int n) {
    int newModelCount = mc + n;
    Model[] newModels = (Model[]) AU.arrayCopyObject(am, newModelCount);
    validateBspf(false);
    modelNumbers = AU.arrayCopyI(modelNumbers, newModelCount);
    modelFileNumbers = AU.arrayCopyI(modelFileNumbers, newModelCount);
    modelNumbersForAtomLabel = AU.arrayCopyS(modelNumbersForAtomLabel,
        newModelCount);
    modelNames = AU.arrayCopyS(modelNames, newModelCount);
    frameTitles = AU.arrayCopyS(frameTitles, newModelCount);
    int f = modelFileNumbers[mc - 1] / 1000000 + 1;
    for (int i = mc, pt = 0; i < newModelCount; i++) {
      modelNumbers[i] = i + mc;
      modelFileNumbers[i] = f * 1000000 + (++pt);
      modelNumbersForAtomLabel[i] = modelNames[i] = f + "." + pt;
    }
    thisStateModel = -1;
    String[] group3Lists = (String[]) getInfoM("group3Lists");
    if (group3Lists != null) {
      int[][] group3Counts = (int[][]) getInfoM("group3Counts");
      group3Lists = AU.arrayCopyS(group3Lists, newModelCount);
      group3Counts = AU.arrayCopyII(group3Counts, newModelCount);
      msInfo.put("group3Lists", group3Lists);
      msInfo.put("group3Counts", group3Counts);
    }
    unitCells = (SymmetryInterface[]) AU.arrayCopyObject(unitCells,
        newModelCount);
    for (int i = mc; i < newModelCount; i++) {
      newModels[i] = new Model().set(this, i, -1, null, null, null);
      newModels[i].loadState = " model create #" + i + ";";
    }
    am = newModels;
    mc = newModelCount;
  }

  public void deleteAtoms(BS bs) {
    //averageAtomPoint = null;
    if (bs == null)
      return;
    BS bsBonds = new BS();
    for (int i = bs.nextSetBit(0); i >= 0 && i < ac; i = bs.nextSetBit(i + 1))
      at[i].delete(bsBonds);
    for (int i = 0; i < mc; i++) {
      am[i].bsAtomsDeleted.or(bs);
      am[i].bsAtomsDeleted.and(am[i].bsAtoms);
      am[i].resetDSSR(false);
    }
    deleteBonds(bsBonds, false);
    validateBspf(false);
  }

  public void clearDB(int atomIndex) {
    getModelAuxiliaryInfo(at[atomIndex].mi).remove("dbName");
  }


  // atom addition //

  public void adjustAtomArrays(int[] map, int i0, int ac) {
    // from ModelLoader, after hydrogen atom addition
    this.ac = ac;
    for (int i = i0; i < ac; i++) {
      at[i] = at[map[i]];
      at[i].i = i;
      Model m = am[at[i].mi];
      if (m.firstAtomIndex == map[i])
        m.firstAtomIndex = i;
      m.bsAtoms.set(i);
    }
    if (vibrations != null)
      for (int i = i0; i < ac; i++)
        vibrations[i] = vibrations[map[i]];
    if (atomTensorList != null) {
      for (int i = i0; i < ac; i++) {
        Object[] list = atomTensorList[i] = atomTensorList[map[i]];
        if (list != null)
          for (int j = list.length; --j >= 0;) {
            Tensor t = (Tensor) list[j];
            if (t != null) {
              t.atomIndex1 = i;
            }
          }
      }
    }
    if (atomNames != null)
      for (int i = i0; i < ac; i++)
        atomNames[i] = atomNames[map[i]];
    if (atomTypes != null)
      for (int i = i0; i < ac; i++)
        atomTypes[i] = atomTypes[map[i]];

    if (atomResnos != null)
      for (int i = i0; i < ac; i++)
        atomResnos[i] = atomResnos[map[i]];
    if (atomSerials != null)
      for (int i = i0; i < ac; i++)
        atomSerials[i] = atomSerials[map[i]];
    if (atomSeqIDs != null)
      for (int i = i0; i < ac; i++)
        atomSeqIDs[i] = atomSeqIDs[map[i]];

    if (bfactor100s != null)
      for (int i = i0; i < ac; i++)
        bfactor100s[i] = bfactor100s[map[i]];

    if (occupancies != null)
      for (int i = i0; i < ac; i++)
        occupancies[i] = occupancies[map[i]];
    if (partialCharges != null)
      for (int i = i0; i < ac; i++)
        partialCharges[i] = partialCharges[map[i]];
  }

  protected void growAtomArrays(int newLength) {
    at = (Atom[]) AU.arrayCopyObject(at, newLength);
    if (vibrations != null)
      vibrations = (Vibration[]) AU.arrayCopyObject(vibrations, newLength);
    if (occupancies != null)
      occupancies = AU.arrayCopyF(occupancies, newLength);
    if (bfactor100s != null)
      bfactor100s = AU.arrayCopyShort(bfactor100s, newLength);
    if (partialCharges != null)
      partialCharges = AU.arrayCopyF(partialCharges, newLength);
    if (atomTensorList != null)
      atomTensorList = (Object[][]) AU.arrayCopyObject(atomTensorList,
          newLength);
    if (atomNames != null)
      atomNames = AU.arrayCopyS(atomNames, newLength);
    if (atomTypes != null)
      atomTypes = AU.arrayCopyS(atomTypes, newLength);
    if (atomResnos != null)
      atomResnos = AU.arrayCopyI(atomResnos, newLength);
    if (atomSerials != null)
      atomSerials = AU.arrayCopyI(atomSerials, newLength);
    if (atomSeqIDs != null)
      atomSeqIDs = AU.arrayCopyI(atomSeqIDs, newLength);
  }

  public Atom addAtom(int modelIndex, Group group, int atomicAndIsotopeNumber,
                      String atomName, String atomType, int atomSerial,
                      int atomSeqID, int atomSite, P3 xyz, float radius,
                      V3 vib, int formalCharge, float partialCharge,
                      float occupancy, float bfactor, Lst<Object> tensors,
                      boolean isHetero, byte specialAtomID, BS atomSymmetry, float bondRadius) {
    Atom atom = new Atom().setAtom(modelIndex, ac, xyz, radius, atomSymmetry,
        atomSite, (short) atomicAndIsotopeNumber, formalCharge, isHetero);
    am[modelIndex].act++;
    am[modelIndex].bsAtoms.set(ac);
    if (Elements.isElement(atomicAndIsotopeNumber, 1))
      am[modelIndex].hydrogenCount++;
    if (ac >= at.length)
      growAtomArrays(ac + 100); // only due to added hydrogens

    at[ac] = atom;
    setBFactor(ac, bfactor, false);
    setOccupancy(ac, occupancy, false);
    setPartialCharge(ac, partialCharge, false);
    if (tensors != null)
      setAtomTensors(ac, tensors);
    atom.group = group;
    atom.colixAtom = vwr.cm.getColixAtomPalette(atom, PAL.CPK.id);
    if (atomName != null) {
      if (atomType != null) {
        if (atomTypes == null)
          atomTypes = new String[at.length];
        atomTypes[ac] = atomType;
      }
      atom.atomID = specialAtomID;
      if (specialAtomID == 0) {
        if (atomNames == null)
          atomNames = new String[at.length];
        atomNames[ac] = atomName.intern();
      }
    }
    if (atomSerial != Integer.MIN_VALUE) {
      if (atomSerials == null)
        atomSerials = new int[at.length];
      atomSerials[ac] = atomSerial;
    }
    if (atomSeqID != 0) {
      if (atomSeqIDs == null)
        atomSeqIDs = new int[at.length];
      atomSeqIDs[ac] = atomSeqID;
    }
    if (vib != null)
      setVibrationVector(ac, vib);
    if (!Float.isNaN(bondRadius))
      setBondingRadius(ac, bondRadius);
    ac++;
    return atom;
  }

  public String getInlineData(int modelIndex) {
    SB data = null;
    if (modelIndex >= 0)
      data = am[modelIndex].loadScript;
    else
      for (modelIndex = mc; --modelIndex >= 0;)
        if ((data = am[modelIndex].loadScript).length() > 0)
          break;
    int pt = data.lastIndexOf("data \"");
    if (pt < 0) { // load inline ...
      String s = PT.getQuotedStringAt(data.toString(), 0);
      return ScriptCompiler.unescapeString(s, 0, s.length());
    }
    pt = data.indexOf2("\"", pt + 7);
    int pt2 = data.lastIndexOf("end \"");
    if (pt2 < pt || pt < 0)
      return null;
    return data.substring2(pt + 2, pt2);
  }

  public boolean isAtomPDB(int i) {
    return i >= 0 && am[at[i].mi].isBioModel;
  }

  /**
   * Ensure the atom index is >= 0 and that the
   * atom's model is the last model.
   * 
   * @param i
   * @return true if that is the case
   */
  public boolean isAtomInLastModel(int i) {
    return i >= 0 && at[i].mi == mc - 1;
  }

  public boolean haveModelKit() {
    for (int i = 0; i < mc; i++)
      if (am[i].isModelKit)
        return true;
    return false;
  }

  public BS getModelKitStateBitset(BS bs, BS bsDeleted) {
    // task here is to remove bits from bs that are deleted atoms in 
    // models that are model kits.

    BS bs1 = BSUtil.copy(bsDeleted);
    for (int i = 0; i < mc; i++)
      if (!am[i].isModelKit)
        bs1.andNot(am[i].bsAtoms);
    return BSUtil.deleteBits(bs, bs1);
  }

  /**
   * 
   * @param iFirst
   *        0 from ModelLoader.freeze; -1 from Viewer.assignAtom
   * @param baseAtomIndex
   * @param mergeSet
   */
  public void setAtomNamesAndNumbers(int iFirst, int baseAtomIndex,
                                     AtomCollection mergeSet) {
    // first, validate that all atomSerials are NaN
    if (baseAtomIndex < 0)
      iFirst = am[at[iFirst].mi].firstAtomIndex;
    if (atomSerials == null)
      atomSerials = new int[ac];
    if (atomNames == null)
      atomNames = new String[ac];
    // now, we'll assign 1-based atom numbers within each model
    boolean isZeroBased = isXYZ && vwr.getBoolean(T.zerobasedxyzrasmol);
    int lastModelIndex = Integer.MAX_VALUE;
    int atomNo = 1;
    for (int i = iFirst; i < ac; ++i) {
      Atom atom = at[i];
      if (atom.mi != lastModelIndex) {
        lastModelIndex = atom.mi;
        atomNo = (isZeroBased ? 0 : 1);
      }
      // 1) do not change numbers assigned by adapter
      // 2) do not change the number already assigned when merging
      // 3) restart numbering with new atoms, not a continuation of old
      if (i >= -baseAtomIndex) {
        if (atomSerials[i] == 0 || baseAtomIndex < 0)
          atomSerials[i] = (i < baseAtomIndex ? mergeSet.atomSerials[i]
              : atomNo);
        if (atomNames[i] == null || baseAtomIndex < 0)
          atomNames[i] = (atom.getElementSymbol() + atomSerials[i]).intern();
      }

      if (!am[lastModelIndex].isModelKit || atom.getElementNumber() > 0
          && !atom.isDeleted())
        atomNo++;
    }
  }

  public void setUnitCellOffset(SymmetryInterface unitCell, T3 pt, int ijk) {
    //    for (int i = modelIndex; i < modelCount; i++) {
    //      if (i < 0 || modelIndex >= 0 && i != modelIndex
    //          && models[i].trajectoryBaseIndex != modelIndex)
    //        continue;
    //      unitCell = getUnitCell(i);
    if (unitCell == null)
      return;
    if (pt == null)
      unitCell.setOffset(ijk);
    else
      unitCell.setOffsetPt(pt);
    //    }
  }

  public void connect(float[][] connections) {
    // array of [index1 index2 order diameter energy]
    resetMolecules();
    BS bsDelete = new BS();
    for (int i = 0; i < connections.length; i++) {
      float[] f = connections[i];
      if (f == null || f.length < 2)
        continue;
      int index1 = (int) f[0];
      boolean addGroup = (index1 < 0);
      if (addGroup)
        index1 = -1 - index1;
      int index2 = (int) f[1];
      if (index2 < 0 || index1 >= ac || index2 >= ac)
        continue;
      int order = (f.length > 2 ? (int) f[2] : Edge.BOND_COVALENT_SINGLE);
      if (order < 0)
        order &= 0xFFFF; // 12.0.1 was saving struts as negative numbers
      short mad = (f.length > 3 ? (short) (1000f * connections[i][3])
          : getDefaultMadFromOrder(order));
      if (order == 0 || mad == 0 && order != Edge.BOND_STRUT
          && !Edge.isOrderH(order)) {
        Bond b = at[index1].getBond(at[index2]);
        if (b != null)
          bsDelete.set(b.index);
        continue;
      }
      float energy = (f.length > 4 ? f[4] : 0);
      bondAtoms(at[index1], at[index2], order, mad, null, energy, addGroup,
          true);
    }
    if (bsDelete.nextSetBit(0) >= 0)
      deleteBonds(bsDelete, false);
  }

  public void setFrameDelayMs(long millis, BS bsModels) {
    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1))
      am[am[i].trajectoryBaseIndex].frameDelay = millis;
  }

  public long getFrameDelayMs(int i) {
    return (i < am.length && i >= 0 ? am[am[i].trajectoryBaseIndex].frameDelay
        : 0);
  }

  public int getModelIndexFromId(String id) {
    boolean haveFile = (id.indexOf("#") >= 0);
    boolean isBaseModel = id.toLowerCase().endsWith(".basemodel");
    if (isBaseModel)
      id = id.substring(0, id.length() - 10);
    int errCode = -1;
    String fname = null;
    for (int i = 0; i < mc; i++) {
      String mid = (String) getInfo(i, "modelID");
      String mnum = (id.startsWith("~") ? "~" + getModelNumberDotted(i) : null);
      if (mnum == null && mid == null && (mid = getModelTitle(i)) == null)
        continue;
      if (haveFile) {
        fname = getModelFileName(i);
        if (fname.endsWith("#molfile")) {
          mid = fname;
        } else {
          fname += "#";
          mid = fname + mid;
        }
      }
      if (id.equalsIgnoreCase(mid) || id.equalsIgnoreCase(mnum))
        return (isBaseModel ? vwr.getJDXBaseModelIndex(i) : i);
      if (fname != null && id.startsWith(fname))
        errCode = -2;
    }
    return (fname == null && !haveFile ? -2 : errCode);
  }

  /**
   * Retrieve the main modelset info Hashtable (or a new non-null Hashtable)
   * with an up-to-date "models" key.
   * 
   * @param bsModels
   * @return Map
   */
  public Map<String, Object> getAuxiliaryInfo(BS bsModels) {
    Map<String, Object> info = msInfo;
    if (info == null)
      info = new Hashtable<String, Object>();
    if (bsModels != null || !info.containsKey("models")) {
      Lst<Map<String, Object>> minfo = new Lst<Map<String, Object>>();
      for (int i = 0; i < mc; ++i)
        if (bsModels == null || bsModels.get(i)) {
          Map<String, Object> m = getModelAuxiliaryInfo(i);
          m.put("modelIndex", Integer.valueOf(i));
          minfo.addLast(m);
        }
      info.put("models", minfo);
    }
    return info;
  }

  public int[][] getDihedralMap(int[] alist) {
    Lst<int[]> list = new Lst<int[]>();
    int n = alist.length;
    Atom ai = null, aj = null, ak = null, al = null;
    for (int i = n - 1; --i >= 0;)
      for (int j = n; --j > i;) {
        ai = at[alist[i]];
        aj = at[alist[j]];
        if (ai.isBonded(aj)) {
          for (int k = n; --k >= 0;)
            if (k != i && k != j && (ak = at[alist[k]]).isBonded(ai))
              for (int l = n; --l >= 0;)
                if (l != i && l != j && l != k
                    && (al = at[alist[l]]).isBonded(aj)) {
                  int[] a = new int[4];
                  a[0] = ak.i;
                  a[1] = ai.i;
                  a[2] = aj.i;
                  a[3] = al.i;
                  list.addLast(a);
                }
        }
      }
    n = list.size();
    int[][] ilist = AU.newInt2(n);
    for (int i = n; --i >= 0;)
      ilist[n - i - 1] = list.get(i);
    return ilist;
  }

  /**
   * Sets the modulation for all atoms in bs.
   * 
   * @param bs
   * @param isOn
   * @param qtOffset
   *        multiples of q or just t.
   * @param isQ
   *        true if multiples of q.
   * 
   * 
   */
  public void setModulation(BS bs, boolean isOn, P3 qtOffset, boolean isQ) {
    if (bsModulated == null) {
      if (isOn)
        bsModulated = new BS();
      else if (bs == null)
        return;
    }
    if (bs == null)
      bs = getModelAtomBitSetIncludingDeleted(-1, false);
    float scale = vwr.getFloat(T.modulation);
    boolean haveMods = false;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      JmolModulationSet ms = getModulation(i);
      if (ms == null)
        continue;
      ms.setModTQ(at[i], isOn, qtOffset, isQ, scale);
      if (bsModulated != null)
        bsModulated.setBitTo(i, isOn);
      haveMods = true;
    }
    if (!haveMods)
      bsModulated = null;
  }

  /**
   * 
   * @param type
   *        volume, best, x, y, z, unitcell
   * @param bsAtoms
   * @return quaternion for best rotation or, for volume, string with volume
   *         \t{dx dy dz}
   */
  public Object getBoundBoxOrientation(int type, BS bsAtoms) {
    float dx = 0, dy = 0, dz = 0;
    Quat q = null, qBest = null;
    int j0 = bsAtoms.nextSetBit(0);
    float vMin = 0;
    if (j0 >= 0) {
      int n = (vOrientations == null ? 0 : vOrientations.length);
      if (n == 0) {
        V3[] av = new V3[15 * 15 * 15];
        n = 0;
        P4 p4 = new P4();
        for (int i = -7; i <= 7; i++)
          for (int j = -7; j <= 7; j++)
            for (int k = 0; k <= 14; k++, n++)
              if ((av[n] = V3.new3(i / 7f, j / 7f, k / 14f)).length() > 1)
                --n;
        vOrientations = new Quat[n];
        for (int i = n; --i >= 0;) {
          float cos = (float) Math.sqrt(1 - av[i].lengthSquared());
          if (Float.isNaN(cos))
            cos = 0;
          p4.set4(av[i].x, av[i].y, av[i].z, cos);
          vOrientations[i] = Quat.newP4(p4);
        }
      }
      P3 pt = new P3();
      vMin = Float.MAX_VALUE;
      BoxInfo bBest = null;
      float v;
      BoxInfo b = new BoxInfo();
      b.setMargin(type == T.volume ? 0 : 0.1f);
      for (int i = 0; i < n; i++) {
        q = vOrientations[i];
        b.reset();
        for (int j = j0; j >= 0; j = bsAtoms.nextSetBit(j + 1))
          b.addBoundBoxPoint(q.transform2(at[j], pt));
        switch (type) {
        default:
        case T.volume:
        case T.best:
        case T.unitcell:
          v = (b.bbCorner1.x - b.bbCorner0.x) * (b.bbCorner1.y - b.bbCorner0.y)
              * (b.bbCorner1.z - b.bbCorner0.z);
          break;
        case T.x:
          v = b.bbCorner1.x - b.bbCorner0.x;
          break;
        case T.y:
          v = b.bbCorner1.y - b.bbCorner0.y;
          break;
        case T.z:
          v = b.bbCorner1.z - b.bbCorner0.z;
          break;
        }
        if (v < vMin) {
          qBest = q;
          bBest = b;
          b = new BoxInfo();
          b.setMargin(0.1f);
          vMin = v;
        }
      }
      switch (type) {
      default:
        return qBest;
      case T.unitcell:
        P3[] pts = bBest.getBoundBoxVertices();
        pts = new P3[] {pts[0], pts[BoxInfo.X], pts[BoxInfo.Y], pts[BoxInfo.Z]};
        qBest = qBest.inv();
        for (int i = 0; i < 4; i++) {
          qBest.transform2(pts[i], pts[i]);
          if (i > 0)
            pts[i].sub(pts[0]);
        }
        return pts;
      case T.volume:
      case T.best:
        // we want dz < dy < dx
        q = Quat.newQ(qBest);
        dx = bBest.bbCorner1.x - bBest.bbCorner0.x;
        dy = bBest.bbCorner1.y - bBest.bbCorner0.y;
        dz = bBest.bbCorner1.z - bBest.bbCorner0.z;
        if (dx < dy) {
          pt.set(0, 0, 1);
          q = Quat.newVA(pt, 90).mulQ(q);
          float f = dx;
          dx = dy;
          dy = f;
        }
        if (dy < dz) {
          if (dz > dx) {
            // is dy < dx < dz
            pt.set(0, 1, 0);
            q = Quat.newVA(pt, 90).mulQ(q);
            float f = dx;
            dx = dz;
            dz = f;
          }
          // is dy < dz < dx
          pt.set(1, 0, 0);
          q = Quat.newVA(pt, 90).mulQ(q);
          float f = dy;
          dy = dz;
          dz = f;
        }
        break;
      }
    }
    return (type == T.volume ? vMin + "\t{" + dx + " " + dy + " " + dz + "}\t" + bsAtoms
        : type == T.unitcell ? null : q == null || q.getTheta() == 0 ? new Quat() : q);
  }

  public SymmetryInterface getUnitCellForAtom(int index) {
    if (index < 0 || index > ac)
      return null;
    if (bsModulated != null) {
      JmolModulationSet ms = getModulation(index);
      SymmetryInterface uc = (ms == null ? null : ms.getSubSystemUnitCell());
      if (uc != null)
        return uc; // subsystems
    }
    return getUnitCell(at[index].mi);
  }

  public void clearCache() {
    for (int i = mc; --i >= 0;)
      am[i].resetDSSR(false);
  }

  public M4[] getSymMatrices(int modelIndex) {
    int n = getModelSymmetryCount(modelIndex);
    if (n == 0)
      return null;
    M4[] ops = new M4[n];
    SymmetryInterface unitcell = am[modelIndex].biosymmetry;
    if (unitcell == null)
      unitcell = getUnitCell(modelIndex);
    for (int i = n; --i >= 0;)
      ops[i] = unitcell.getSpaceGroupOperation(i);
    return ops;
  }

  public BS[] getBsBranches(float[] dihedralList) {
    int n = dihedralList.length / 6;
    BS[] bsBranches = new BS[n];
    Map<String, Boolean> map = new Hashtable<String, Boolean>();
    for (int i = 0, pt = 0; i < n; i++, pt += 6) {
      float dv = dihedralList[pt + 5] - dihedralList[pt + 4];
      if (Math.abs(dv) < 1f)
        continue;
      int i0 = (int) dihedralList[pt + 1];
      int i1 = (int) dihedralList[pt + 2];
      String s = "" + i0 + "_" + i1;
      if (map.containsKey(s))
        continue;
      map.put(s, Boolean.TRUE);
      BS bs = vwr.getBranchBitSet(i1, i0, true);
      Bond[] bonds = at[i0].bonds;
      Atom a0 = at[i0];
      for (int j = 0; j < bonds.length; j++) {
        Bond b = bonds[j];
        if (!b.isCovalent())
          continue;
        int i2 = b.getOtherAtom(a0).i;
        if (i2 == i1)
          continue;
        if (bs.get(i2)) {
          bs = null;
          break;
        }
      }
      bsBranches[i] = bs;
    }
    return bsBranches;
  }

  public void recalculatePositionDependentQuantities(BS bs, M4 mat) {
    if ((vwr.shm.getShape(JC.SHAPE_POLYHEDRA) != null))
      vwr.shm.getShapePropertyData(JC.SHAPE_POLYHEDRA, "move", new Object[] {
          bs, mat });
    if (haveStraightness)
      calculateStraightnessAll();
    recalculateLeadMidpointsAndWingVectors(-1);
    BS bsModels = getModelBS(bs, false);
    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1)) {
      sm.notifyAtomPositionsChanged(i, bs, mat);
      if (mat != null) {
        Model m = am[i];
        if (m.isContainedIn(bs)) {
          if (m.mat4 == null)
            m.mat4 = M4.newM4(null);
          m.mat4.mul2(mat, m.mat4);
        }
      }
    }
    //averageAtomPoint = null;
    /* but we would need to somehow indicate this in the state
    if (ellipsoids != null)
      for (int i = bs.nextSetBit(0); i >= 0 && i < ellipsoids.length; i = bs.nextSetBit(i + 1))
        ellipsoids[i].rotate(mat);
    if (vibrationVectors != null)
      for (int i = bs.nextSetBit(0); i >= 0 && i < vibrationVectors.length; i = bs.nextSetBit(i + 1))
        if (vibrationVectors[i] != null)
            mat.transform(vibrationVectors[i]);
            */
  }

  public void moveAtoms(M4 m4, M3 mNew, M3 rotation, V3 translation, BS bs,
                        P3 center, boolean isInternal, boolean translationOnly) {
    if (m4 != null) {
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        m4.rotTrans(at[i]);
        taintAtom(i, TAINT_COORD);
      }
      mat4.setM4(m4);
      translation = null;
    } else if (!translationOnly) {
      if (mNew == null) {
        matTemp.setM3(rotation);
      } else {
        // screen frame?
        // must do inv(currentRot) * mNew * currentRot
        ptTemp.set(0, 0, 0);
        matInv.setM3(rotation);
        matInv.invert();
        matTemp.mul2(mNew, rotation);
        matTemp.mul2(matInv, matTemp);
      }
      if (isInternal) {
        // adjust rotation to be around center of this set of atoms
        vTemp.setT(center);
        mat4.setIdentity();
        mat4.setTranslation(vTemp);
        mat4t.setToM3(matTemp);
        mat4.mul(mat4t);
        mat4t.setIdentity();
        vTemp.scale(-1);
        mat4t.setTranslation(vTemp);
        mat4.mul(mat4t);
      } else {
        mat4.setToM3(matTemp);
      }
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        if (isInternal) {
          mat4.rotTrans(at[i]);
        } else {
          ptTemp.add(at[i]);
          mat4.rotTrans(at[i]);
          ptTemp.sub(at[i]);
        }
        taintAtom(i, TAINT_COORD);
      }
      if (!isInternal) {
        ptTemp.scale(1f / bs.cardinality());
        if (translation == null)
          translation = new V3();
        translation.add(ptTemp);
      }
    }
    if (translation != null) {
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        at[i].add(translation);
        taintAtom(i, TAINT_COORD);
      }
      if (!translationOnly) {
        mat4t.setIdentity();
        mat4t.setTranslation(translation);
        mat4.mul2(mat4t, mat4);
      }
    }
    recalculatePositionDependentQuantities(bs, mat4);
  }

  public void setDihedrals(float[] dihedralList, BS[] bsBranches, float f) {
    int n = dihedralList.length / 6;
    if (f > 1)
      f = 1;
    for (int j = 0, pt = 0; j < n; j++, pt += 6) {
      BS bs = bsBranches[j];
      if (bs == null || bs.isEmpty())
        continue;
      Atom a1 = at[(int) dihedralList[pt + 1]];
      V3 v = V3.newVsub(at[(int) dihedralList[pt + 2]], a1);
      float angle = (dihedralList[pt + 5] - dihedralList[pt + 4]) * f;
      A4 aa = A4.newVA(v, (float) (-angle / TransformManager.degreesPerRadian));
      matTemp.setAA(aa);
      ptTemp.setT(a1);
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        at[i].sub(ptTemp);
        matTemp.rotate(at[i]);
        at[i].add(ptTemp);
        taintAtom(i, TAINT_COORD);
      }
    }
  }

  public void setAtomCoordsRelative(T3 offset, BS bs) {
    setAtomsCoordRelative(bs, offset.x, offset.y, offset.z);
    mat4.setIdentity();
    vTemp.setT(offset);
    mat4.setTranslation(vTemp);
    recalculatePositionDependentQuantities(bs, mat4);
  }

  public void setAtomCoords(BS bs, int tokType, Object xyzValues) {
    setAtomCoord2(bs, tokType, xyzValues);
    switch (tokType) {
    case T.vibx:
    case T.viby:
    case T.vibz:
    case T.vibxyz:
      break;
    default:
      recalculatePositionDependentQuantities(bs, null);
    }
  }

  /**
   * Carries out a stereochemical inversion through a point, across a plane, or at a chirality center.
   * 
   * @param pt point to invert around if not null
   * @param plane plane to invert across if not null
   * @param iAtom atom to switch two groups on if >= 0
   * @param bsAtoms atoms to switch for the atom option
   */
  public void invertSelected(P3 pt, P4 plane, int iAtom, BS bsAtoms) {
    resetChirality();
    if (pt != null) {
      for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
        float x = (pt.x - at[i].x) * 2;
        float y = (pt.y - at[i].y) * 2;
        float z = (pt.z - at[i].z) * 2;
        setAtomCoordRelative(i, x, y, z);
      }
      return;
    }
    if (plane != null) {
      // ax + by + cz + d = 0
      V3 norm = V3.new3(plane.x, plane.y, plane.z);
      norm.normalize();
      float d = (float) Math.sqrt(plane.x * plane.x + plane.y * plane.y
          + plane.z * plane.z);
      for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
        float twoD = -Measure.distanceToPlaneD(plane, d, at[i]) * 2;
        float x = norm.x * twoD;
        float y = norm.y * twoD;
        float z = norm.z * twoD;
        setAtomCoordRelative(i, x, y, z);
      }
      return;
    }
    if (iAtom >= 0) {
      Atom thisAtom = at[iAtom];
      // stereochemical inversion at iAtom
      Bond[] bonds = thisAtom.bonds;
      if (bonds == null)
        return;
      BS bsToMove = new BS();
      Lst<P3> vNot = new Lst<P3>();
      BS bsModel = vwr.getModelUndeletedAtomsBitSet(thisAtom.mi);
      for (int i = 0; i < bonds.length; i++) {
        Atom a = bonds[i].getOtherAtom(thisAtom);
        if (bsAtoms.get(a.i)) {
          bsToMove.or(JmolMolecule.getBranchBitSet(at, a.i, bsModel, null,
              iAtom, true, true));
        } else {
          vNot.addLast(a);
        }
      }
      if (vNot.size() == 0)
        return;
      pt = Measure.getCenterAndPoints(vNot)[0];
      V3 v = V3.newVsub(thisAtom, pt);
      Quat q = Quat.newVA(v, 180);
      moveAtoms(null, null, q.getMatrix(), null, bsToMove, thisAtom, true, false);
    }
  }

  public float[] getCellWeights(BS bsAtoms) {
    float[] wts = null;
    int i = bsAtoms.nextSetBit(0);
    int iModel = -1;
    if (i >= 0 && getUnitCell(iModel = at[i].mi) != null) {
      P3 pt = new P3();
      BS bs = getModelAtomBitSetIncludingDeleted(iModel, true);
      bs.and(bsAtoms);
      wts = new float[bsAtoms.cardinality()];
      for (int p = 0; i >= 0; i = bsAtoms.nextSetBit(i + 1))
        wts[p++] = SimpleUnitCell.getCellWeight(at[i].getFractionalUnitCoordPt(
            true, false, pt));
    }
    return wts;
  }

  public Quat[] getAtomGroupQuaternions(BS bsAtoms, int nMax, char qtype) {
    // run through list, getting quaternions. For simple groups, 
    // go ahead and take first three atoms
    // for PDB files, do not include NON-protein groups.
    int n = 0;
    Lst<Quat> v = new Lst<Quat>();
    bsAtoms = BSUtil.copy(bsAtoms);
    BS bsDone = new BS();
    for (int i = bsAtoms.nextSetBit(0); i >= 0 && n < nMax; i = bsAtoms
        .nextSetBit(i + 1)) {
      Group g = at[i].group;
      g.setAtomBits(bsDone);
      bsAtoms.andNot(bsDone);
      Quat q = g.getQuaternion(qtype);
      if (q == null) {
        if (!am[at[i].mi].isBioModel)
          q = g.getQuaternionFrame(at); // non-PDB just use first three atoms
        if (q == null)
          continue;
      }
      n++;
      v.addLast(q);
    }
    return v.toArray(new Quat[v.size()]);
  }

  public BS getConformation(int modelIndex, int conformationIndex,
                            boolean doSet, BS bsSelected) {
    BS bs = new BS();
    if (conformationIndex >= 0)
      for (int i = mc; --i >= 0;) {
        if (i != modelIndex && modelIndex >= 0)
          continue;
        Model m = am[i];
        BS bsAtoms = vwr.getModelUndeletedAtomsBitSet(modelIndex);
        if (bsSelected != null)
          bsAtoms.and(bsSelected);
        if (bsAtoms.nextSetBit(0) < 0)
          continue;
        if (conformationIndex >= m.altLocCount) {
          if (conformationIndex == 0)
            bs.or(bsAtoms);
          continue;
        }
        if (am[i].isBioModel
            && ((BioModel) am[i]).getConformation(conformationIndex, doSet,
                bsAtoms, bs))
          continue;
        int nAltLocs = getAltLocCountInModel(i);
        String altLocs = getAltLocListInModel(i);
        BS bsTemp = new BS();
        for (int c = nAltLocs; --c >= 0;)
          if (c != conformationIndex)
            bsAtoms.andNot(getAtomBitsMDa(T.spec_alternate,
                altLocs.substring(c, c + 1), bsTemp));
        if (bsAtoms.nextSetBit(0) >= 0)
          bs.or(bsAtoms);
      }
    return bs;
  }

  ///// bio-only methods /////

  public BS getSequenceBits(String specInfo, BS bsAtoms, BS bsResult) {
    return (haveBioModels ? bioModelset.getAllSequenceBits(specInfo, bsAtoms, bsResult)
        : bsResult);
  }

  public int getBioPolymerCountInModel(int modelIndex) {
    return (haveBioModels ? bioModelset.getBioPolymerCountInModel(modelIndex)
        : 0);
  }

  public void getPolymerPointsAndVectors(BS bs, Lst<P3[]> vList,
                                         boolean isTraceAlpha,
                                         float sheetSmoothing) {
    if (haveBioModels)
      bioModelset.getAllPolymerPointsAndVectors(bs, vList, isTraceAlpha,
          sheetSmoothing);
  }

  public void recalculateLeadMidpointsAndWingVectors(int modelIndex) {
    if (haveBioModels)
      bioModelset.recalculatePoints(modelIndex);
  }

  /**
   * These are not actual hydrogen bonds. They are N-O bonds in proteins and
   * nucleic acids The method is called by AminoPolymer and NucleicPolymer
   * methods, which are indirectly called by ModelCollection.autoHbond
   * 
   * @param bsA
   * @param bsB
   * @param vHBonds
   *        vector of bonds to fill; if null, creates the HBonds
   * @param nucleicOnly
   * @param nMax
   * @param dsspIgnoreHydrogens
   * @param bsHBonds
   */

  public void calcRasmolHydrogenBonds(BS bsA, BS bsB, Lst<Bond> vHBonds,
                                      boolean nucleicOnly, int nMax,
                                      boolean dsspIgnoreHydrogens, BS bsHBonds) {
    if (haveBioModels)
      bioModelset.calcAllRasmolHydrogenBonds(bsA, bsB, vHBonds, nucleicOnly,
          nMax, dsspIgnoreHydrogens, bsHBonds, 2); // DSSP version 2 here
  }

  public void calculateStraightnessAll() {
    if (haveBioModels && !haveStraightness)
      bioModelset.calculateStraightnessAll();
  }

  /**
   * see comments in org.jmol.modelsetbio.AlphaPolymer.java
   * 
   * Struts are calculated for atoms in bs1 connecting to atoms in bs2. The two
   * bitsets may overlap.
   * 
   * @param bs1
   * @param bs2
   * @return number of struts found
   */
  public int calculateStruts(BS bs1, BS bs2) {
    return (haveBioModels ? bioModelset.calculateStruts(bs1, bs2) : 0);
  }

  public BS getGroupsWithin(int nResidues, BS bs) {
    return (haveBioModels ? bioModelset.getGroupsWithinAll(nResidues, bs)
        : new BS());
  }

  public String getProteinStructureState(BS bsAtoms, int mode) {
    return (haveBioModels ? bioModelset.getFullProteinStructureState(bsAtoms,
        mode) : "");
  }

  public String calculateStructures(BS bsAtoms, boolean asDSSP,
                                    boolean doReport,
                                    boolean dsspIgnoreHydrogen,
                                    boolean setStructure, int version) {
    return (haveBioModels ? bioModelset.calculateAllStuctures(bsAtoms, asDSSP,
        doReport, dsspIgnoreHydrogen, setStructure, version) : "");
  }

  /**
   * allows rebuilding of PDB structures; also accessed by ModelManager from
   * Eval
   * 
   * @param alreadyDefined
   *        set to skip calculation
   * @param asDSSP
   * @param doReport
   * @param dsspIgnoreHydrogen
   * @param setStructure
   * @param includeAlpha
   * @param version TODO
   * @return report
   * 
   */
  public String calculateStructuresAllExcept(BS alreadyDefined, boolean asDSSP,
                                             boolean doReport,
                                             boolean dsspIgnoreHydrogen,
                                             boolean setStructure,
                                             boolean includeAlpha, int version) {
    freezeModels();
    return (haveBioModels ? bioModelset.calculateAllStructuresExcept(
        alreadyDefined, asDSSP, doReport, dsspIgnoreHydrogen, setStructure,
        includeAlpha, version) : "");
  }

  public void recalculatePolymers(BS bsModelsExcluded) {
    bioModelset.recalculateAllPolymers(bsModelsExcluded, getGroups());
  }

  protected void calculatePolymers(Group[] groups, int groupCount,
                                   int baseGroupIndex, BS modelsExcluded) {
    if (bioModelset != null) // not using haveBioModels because not frozen yet
      bioModelset.calculateAllPolymers(groups, groupCount, baseGroupIndex,
          modelsExcluded);
  }

  public void calcSelectedMonomersCount() {
    if (haveBioModels)
      bioModelset.calcSelectedMonomersCount();
  }

  public void setProteinType(BS bs, STR type) {
    if (haveBioModels)
      bioModelset.setAllProteinType(bs, type);
  }

  public void setStructureList(Map<STR, float[]> structureList) {
    if (haveBioModels)
      bioModelset.setAllStructureList(structureList);
  }

  public BS setConformation(BS bsAtoms) {
    if (haveBioModels)
      bioModelset.setAllConformation(bsAtoms);
    return bsAtoms;
  }

  @SuppressWarnings("unchecked")
  public Map<String, String> getHeteroList(int modelIndex) {
    Object o = (haveBioModels ? bioModelset.getAllHeteroList(modelIndex) : null);
    return (Map<String, String>) (o == null ? getInfoM("hetNames") : o);
  }

  public Object getUnitCellPointsWithin(float distance, BS bs, P3 pt,
                                        boolean asMap) {
    Lst<P3> lst = new Lst<P3>();
    Map<String, Object> map = null;
    Lst<Integer> lstI = null;
    if (asMap) {
      map = new Hashtable<String, Object>();
      lstI = new Lst<Integer>();
      map.put("atoms", lstI);
      map.put("points", lst);
    }
    int iAtom = (bs == null ? -1 : bs.nextSetBit(0));
    bs = vwr
        .getModelUndeletedAtomsBitSet(iAtom < 0 ? vwr.am.cmi : at[iAtom].mi);
    if (iAtom < 0)
      iAtom = bs.nextSetBit(0);
    if (iAtom >= 0) {
      SymmetryInterface unitCell = getUnitCellForAtom(iAtom);
      if (unitCell != null) {
        AtomIndexIterator iter = unitCell.getIterator(vwr, at[iAtom], at, bs,
            distance);
        if (pt != null)
          iter.setCenter(pt, distance);
        while (iter.hasNext()) {
          iAtom = iter.next();
          pt = iter.getPosition();
          lst.addLast(pt);
          if (asMap) {
            lstI.addLast(Integer.valueOf(iAtom));
          }
        }
      }
    }
    return (asMap ? map : lst);
  }

  public void calculateDssrProperty(String dataType) {
    if (dataType == null)
      return;
    if (dssrData == null || dssrData.length < ac)
      dssrData = new float[ac];
    for (int i = 0; i < ac; i++)
      dssrData[i] = Float.NaN;
    for (int i = mc; --i >= 0;)
      if (am[i].isBioModel)
        ((BioModel) am[i]).getAtomicDSSRData(dssrData, dataType);
  }

  public float getAtomicDSSRData(int i) {
    return (dssrData == null || dssrData.length <= i ? Float.NaN : dssrData[i]);
  }

  /**
   * Determine the Cahn-Ingold-Prelog R/S chirality of an atom
   * 
   * @param atom
   * @return [0:none, 1:R, 2:S]
   */
  public int getAtomCIPChiralityCode(Atom atom) {
    haveChirality = true;
    Model m = am[atom.mi];
    if (!m.hasChirality) {
      calculateChiralityForAtoms(m.bsAtoms, false);
      m.hasChirality = true; 
    }
    return atom.getCIPChiralityCode();
  }

  public String calculateChiralityForAtoms(BS bsAtoms, boolean withReturn) {
    haveChirality = true;
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) 
      at[i].setCIPChirality(0);
    Interface.getSymmetry(vwr, "ms").calculateCIPChiralityForAtoms(vwr, bsAtoms);
    if (!withReturn)
      return null;
    String s = "";
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) 
      s += at[i].getCIPChirality(false);
    return s;
  }

}

