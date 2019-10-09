/* $RCSfiodelle$allrueFFFF
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
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

package org.jmol.symmetry;

import java.util.Map;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.Matrix;
import javajs.util.P3;
import javajs.util.P3i;
import javajs.util.P4;
import javajs.util.Quat;
import javajs.util.SB;
import javajs.util.T3;
import javajs.util.V3;

import org.jmol.api.AtomIndexIterator;
import org.jmol.api.GenericPlatform;
import org.jmol.api.Interface;
import org.jmol.api.SymmetryInterface;
import org.jmol.bspt.Bspt;
import org.jmol.bspt.CubeIterator;
import org.jmol.modelset.Atom;
import org.jmol.modelset.ModelSet;
import org.jmol.script.T;
import org.jmol.util.Escape;
import org.jmol.util.JmolMolecule;
import org.jmol.util.Logger;
import org.jmol.util.SimpleUnitCell;
import org.jmol.util.Tensor;
import org.jmol.viewer.Viewer;

public class Symmetry implements SymmetryInterface {
  // NOTE: THIS CLASS IS VERY IMPORTANT.
  // IN ORDER TO MODULARIZE IT, IT IS REFERENCED USING 
  // xxxx = Interface.getSymmetry();

  /* Symmetry is a wrapper class that allows access to the package-local
   * classes PointGroup, SpaceGroup, SymmetryInfo, and UnitCell.
   * 
   * When symmetry is detected in ANY model being loaded, a SymmetryInterface
   * is established for ALL models.
   * 
   * The SpaceGroup information could be saved with each model, but because this 
   * depends closely on what atoms have been selected, and since tracking that with atom
   * deletion is a bit complicated, instead we just use local instances of that class.
   * 
   * The three PointGroup methods here could be their own interface; they are just here
   * for convenience.
   * 
   * The file readers use SpaceGroup and UnitCell methods
   * 
   * The modelSet and modelLoader classes use UnitCell and SymmetryInfo 
   * 
   */
  private PointGroup pointGroup;
  SpaceGroup spaceGroup;
  private SymmetryInfo symmetryInfo;
  private UnitCell unitCell;
  private boolean isBio;

  @Override
  public boolean isBio() {
    return isBio;
  }

  public Symmetry() {
    // instantiated ONLY using
    // symmetry = Interface.getSymmetry();
    // DO NOT use symmetry = new Symmetry();
    // as that will invalidate the Jar file modularization    
  }

  @Override
  public SymmetryInterface setPointGroup(SymmetryInterface siLast,
                                         T3 center, T3[] atomset,
                                         BS bsAtoms,
                                         boolean haveVibration,
                                         float distanceTolerance,
                                         float linearTolerance, boolean localEnvOnly) {
    pointGroup = PointGroup.getPointGroup(siLast == null ? null
        : ((Symmetry) siLast).pointGroup, center, atomset, bsAtoms,
        haveVibration, distanceTolerance, linearTolerance, localEnvOnly);
    return this;
  }

  @Override
  public String getPointGroupName() {
    return pointGroup.getName();
  }

  @Override
  public Object getPointGroupInfo(int modelIndex, String drawID,
                                  boolean asInfo, String type, int index,
                                  float scale) {
    if (drawID == null && !asInfo && pointGroup.textInfo != null)
      return pointGroup.textInfo;
    else if (drawID == null && pointGroup.isDrawType(type, index, scale))
      return pointGroup.drawInfo;
    else if (asInfo && pointGroup.info != null)
      return pointGroup.info;
    return pointGroup.getInfo(modelIndex, drawID, asInfo, type, index, scale);
  }

  // SpaceGroup methods

  @Override
  public void setSpaceGroup(boolean doNormalize) {
    if (spaceGroup == null)
      spaceGroup = SpaceGroup.getNull(true, doNormalize, false);
  }

  @Override
  public int addSpaceGroupOperation(String xyz, int opId) {
    return spaceGroup.addSymmetry(xyz, opId, false);
  }

  @Override
  public int addBioMoleculeOperation(M4 mat, boolean isReverse) {
    isBio = spaceGroup.isBio = true;
    return spaceGroup.addSymmetry((isReverse ? "!" : "") + "[[bio" + mat, 0,
        false);
  }

  @Override
  public void setLattice(int latt) {
    spaceGroup.setLatticeParam(latt);
  }

  @Override
  public Object getSpaceGroup() {
    return spaceGroup;
  }

  @Override
  public void setSpaceGroupFrom(SymmetryInterface symmetry) {
    spaceGroup = (SpaceGroup) symmetry.getSpaceGroup();
  }

  /**
   * 
   * @param desiredSpaceGroupIndex
   * @param name
   * @param data a Lst<SymmetryOperation> or Lst<M4> 
   * @param d in [3+d] modulation dimension
   * @return true if a known space group
   */
  @Override
  public boolean createSpaceGroup(int desiredSpaceGroupIndex, String name,
                                  Object data, int modDim) {
    spaceGroup = SpaceGroup.createSpaceGroup(desiredSpaceGroupIndex, name,
        data, modDim);
    if (spaceGroup != null && Logger.debugging)
      Logger.debug("using generated space group " + spaceGroup.dumpInfo());
    return spaceGroup != null;
  }

  @Override
  public Object getSpaceGroupInfoObj(String name, SymmetryInterface cellInfo, boolean isFull) {
    return SpaceGroup.getInfo(spaceGroup, name, cellInfo, isFull);
  }

  @Override
  public Object getLatticeDesignation() {
    return spaceGroup.getLatticeDesignation();
  }

  @Override
  public void setFinalOperations(String name, P3[] atoms, int iAtomFirst,
                                 int noSymmetryCount, boolean doNormalize,
                                 String filterSymop) {
    if (name != null && (name.startsWith("bio") || name.indexOf(" *(") >= 0)) // filter SYMOP
      spaceGroup.name = name;
    if (filterSymop != null) {
      Lst<SymmetryOperation> lst = new Lst<SymmetryOperation>();
      lst.addLast(spaceGroup.operations[0]);
      for (int i = 1; i < spaceGroup.operationCount; i++)
        if (filterSymop.contains(" " + (i + 1) + " "))
          lst.addLast(spaceGroup.operations[i]);
      spaceGroup = SpaceGroup.createSpaceGroup(-1,
          name + " *(" + filterSymop.trim() + ")", lst, -1);
    }
    spaceGroup.setFinalOperations(atoms, iAtomFirst, noSymmetryCount,
        doNormalize);
  }

  @Override
  public M4 getSpaceGroupOperation(int i) {
    return (spaceGroup == null || spaceGroup.operations == null // bio 
        || i >= spaceGroup.operations.length ? null
        : spaceGroup.finalOperations == null ? spaceGroup.operations[i]
            : spaceGroup.finalOperations[i]);
  }

  @Override
  public String getSpaceGroupXyz(int i, boolean doNormalize) {
    return spaceGroup.getXyz(i, doNormalize);
  }

  @Override
  public void newSpaceGroupPoint(int i, P3 atom1, P3 atom2, int transX,
                                 int transY, int transZ, M4 o) {
    if (o == null && spaceGroup.finalOperations == null) {
      SymmetryOperation op = spaceGroup.operations[i]; 
      // temporary spacegroups don't have to have finalOperations
      if (!op.isFinalized)
        op.doFinalize();
      SymmetryOperation.newPoint(op, atom1, atom2, transX, transY, transZ);
      return;
    }
    SymmetryOperation.newPoint((o == null ? spaceGroup.finalOperations[i] : o), atom1, atom2, transX, transY, transZ);
  }

  @Override
  public V3[] rotateAxes(int iop, V3[] axes, P3 ptTemp, M3 mTemp) {
    return (iop == 0 ? axes : spaceGroup.finalOperations[iop].rotateAxes(axes,
        unitCell, ptTemp, mTemp));
  }

  @Override
  public String getSpaceGroupOperationCode(int iOp) {
    return spaceGroup.operations[iOp].subsystemCode;
  }

  @Override
  public void setTimeReversal(int op, int val) {
    spaceGroup.operations[op].setTimeReversal(val);
  }

  @Override
  public float getSpinOp(int op) {
    return spaceGroup.operations[op].getMagneticOp();
  }

  @Override
  public boolean addLatticeVectors(Lst<float[]> lattvecs) {
    return spaceGroup.addLatticeVectors(lattvecs);
  }

  @Override
  public int getLatticeOp() {
    return spaceGroup.latticeOp;
  }

  @Override
  public Matrix getOperationRsVs(int iop) {
    return (spaceGroup.finalOperations == null ? spaceGroup.operations
        : spaceGroup.finalOperations)[iop].rsvs;
  }

  @Override
  public int getSiteMultiplicity(P3 pt) {
    return spaceGroup.getSiteMultiplicity(pt, unitCell);
  }

  @Override
  /**
   * @param rot is a full (3+d)x(3+d) array of epsilons
   * @param trans is a (3+d)x(1) array of translations
   * @return Jones-Faithful representation
   */
  public String addOp(String code, Matrix rs, Matrix vs, Matrix sigma) {
    spaceGroup.isSSG = true;
    String s = SymmetryOperation.getXYZFromRsVs(rs, vs, false);
    int i = spaceGroup.addSymmetry(s, -1, true);
    spaceGroup.operations[i].setSigma(code, sigma);
    return s;
  }

  @Override
  public String getMatrixFromString(String xyz, float[] rotTransMatrix,
                                    boolean allowScaling, int modDim) {
    return SymmetryOperation.getMatrixFromString(null, xyz, rotTransMatrix,
        allowScaling);
  }

  /// symmetryInfo ////

  // symmetryInfo is (inefficiently) passed to Jmol from the adapter 
  // in lieu of saving the actual unit cell read in the reader. Not perfect.
  // The idea was to be able to create the unit cell from "scratch" independent
  // of the reader. 
  
  @Override
  public String getSpaceGroupName() {
    return (symmetryInfo != null ? symmetryInfo.sgName
        : spaceGroup != null ? spaceGroup.getName() : unitCell != null
            && unitCell.name.length() > 0 ? "cell=" + unitCell.name : "");
  }

  @Override
  public void setSpaceGroupName(String name) {
    if (spaceGroup != null) 
      spaceGroup.setName(name);
  }

  @Override
  public int getSpaceGroupOperationCount() {
    return (symmetryInfo != null ? symmetryInfo.symmetryOperations.length
        : spaceGroup != null && spaceGroup.finalOperations != null ? spaceGroup.finalOperations.length
            : 0);
  }

  @Override
  public String getLatticeType() {
    return (symmetryInfo != null ? symmetryInfo.latticeType 
        : spaceGroup == null ? "P" 
            : spaceGroup.latticeType);
  }

  @Override
  public void setLatticeType(String type) {
    if (spaceGroup != null)
      spaceGroup.latticeType = type;
  }

  @Override
  public String getIntTableNumber() {
    return (symmetryInfo != null ? symmetryInfo.intlTableNo 
        : spaceGroup == null ? null 
            : spaceGroup.intlTableNumber);
  }

  @Override
  public boolean getCoordinatesAreFractional() {
    return symmetryInfo == null || symmetryInfo.coordinatesAreFractional;
  }

  @Override
  public int[] getCellRange() {
    return symmetryInfo == null ? null : symmetryInfo.cellRange;
  }

  @Override
  public String getSymmetryInfoStr() {
    return (symmetryInfo == null ? "" : symmetryInfo.infoStr);
  }

  @Override
  public M4[] getSymmetryOperations() {
    if (symmetryInfo != null)
      return symmetryInfo.symmetryOperations;
    if (spaceGroup == null)
      spaceGroup = SpaceGroup.getNull(true, false, true);
    return spaceGroup.finalOperations;
  }

  @Override
  public boolean isSimple() {
    return (symmetryInfo == null || symmetryInfo.symmetryOperations == null);
  }

  /**
   * Set space group and unit cell from the auxiliary info generated by the
   * model adapter.
   * 
   */
  @SuppressWarnings("unchecked")
  @Override
  public SymmetryInterface setSymmetryInfo(int modelIndex,
                                           Map<String, Object> modelAuxiliaryInfo,
                                           float[] unitCellParams) {
    symmetryInfo = new SymmetryInfo();
    float[] params = symmetryInfo.setSymmetryInfo(modelAuxiliaryInfo,
        unitCellParams);
    if (params != null) {
      setUnitCell(params, modelAuxiliaryInfo.containsKey("jmolData"));
      unitCell.moreInfo = (Lst<String>) modelAuxiliaryInfo
          .get("moreUnitCellInfo");
      modelAuxiliaryInfo.put("infoUnitCell", getUnitCellAsArray(false));
      setOffsetPt((T3) modelAuxiliaryInfo.get("unitCellOffset"));
      M3 matUnitCellOrientation = (M3) modelAuxiliaryInfo
          .get("matUnitCellOrientation");
      if (matUnitCellOrientation != null)
        initializeOrientation(matUnitCellOrientation);
      if (Logger.debugging)
        Logger.debug("symmetryInfos[" + modelIndex + "]:\n"
            + unitCell.dumpInfo(true));
    }
    return this;
  }

  // UnitCell methods

  @Override
  public boolean haveUnitCell() {
    return (unitCell != null);
  }

  @Override
  public boolean checkUnitCell(SymmetryInterface uc, P3 cell, P3 ptTemp,
                               boolean isAbsolute) {
    uc.toFractional(ptTemp, isAbsolute);
    // {1 1 1} here is the original cell
    return (ptTemp.x >= cell.x - 1f - SimpleUnitCell.SLOP && ptTemp.x <= cell.x + SimpleUnitCell.SLOP
        && ptTemp.y >= cell.y - 1f - SimpleUnitCell.SLOP && ptTemp.y <= cell.y + SimpleUnitCell.SLOP
        && ptTemp.z >= cell.z - 1f - SimpleUnitCell.SLOP && ptTemp.z <= cell.z + SimpleUnitCell.SLOP);
  }

  @Override
  public void setUnitCell(float[] unitCellParams, boolean setRelative) {
    unitCell = UnitCell.fromParams(unitCellParams, setRelative);
  }

  @Override
  public boolean unitCellEquals(SymmetryInterface uc2) {
    return ((Symmetry) (uc2)).unitCell.isSameAs(unitCell);
  }

  @Override
  public String getUnitCellState() {
    return (unitCell == null ? "" : unitCell.getState());
  }

  @Override
  public Lst<String> getMoreInfo() {
    return unitCell.moreInfo;
  }

  public String getUnitsymmetryInfo() {
    // not used in Jmol?
    return unitCell.dumpInfo(false);
  }

  @Override
  public void initializeOrientation(M3 mat) {
    unitCell.initOrientation(mat);
  }

  @Override
  public void unitize(T3 ptFrac) {
    unitCell.unitize(ptFrac);
  }

  @Override
  public void toUnitCell(T3 pt, T3 offset) {
    unitCell.toUnitCell(pt, offset);
  }

  @Override
  public P3 toSupercell(P3 fpt) {
    return unitCell.toSupercell(fpt);
  }

  @Override
  public void toFractional(T3 pt, boolean ignoreOffset) {
    if (!isBio)
      unitCell.toFractional(pt, ignoreOffset);
  }
  
  @Override
  public void toFractionalM(M4 m) {
    if (!isBio)
      unitCell.toFractionalM(m);
  }

  @Override
  public void toCartesian(T3 fpt, boolean ignoreOffset) {
    if (!isBio)
      unitCell.toCartesian(fpt, ignoreOffset);
  }

  @Override
  public float[] getUnitCellParams() {
    return unitCell.getUnitCellParams();
  }

  @Override
  public float[] getUnitCellAsArray(boolean vectorsOnly) {
    return unitCell.getUnitCellAsArray(vectorsOnly);
  }

  @Override
  public Tensor getTensor(Viewer vwr, float[] parBorU) {
    if (parBorU == null)
      return null;
    if (unitCell == null)
      unitCell = UnitCell.fromParams(new float[] { 1, 1, 1, 90, 90, 90 }, true);
    return unitCell.getTensor(vwr, parBorU);
  }

  @Override
  public P3[] getUnitCellVerticesNoOffset() {
    return unitCell.getVertices();
  }

  @Override
  public P3 getCartesianOffset() {
    return unitCell.getCartesianOffset();
  }

  @Override
  public P3 getFractionalOffset() {
    return unitCell.getFractionalOffset();
  }

  @Override
  public void setOffsetPt(T3 pt) {
    unitCell.setOffset(pt);
  }

  @Override
  public void setOffset(int nnn) {
    P3 pt = new P3();
    SimpleUnitCell.ijkToPoint3f(nnn, pt, 0, 0);
    unitCell.setOffset(pt);
  }

  @Override
  public T3 getUnitCellMultiplier() {
    return unitCell.getUnitCellMultiplier();
  }

  @Override
  public P3[] getCanonicalCopy(float scale, boolean withOffset) {
    return unitCell.getCanonicalCopy(scale, withOffset);
  }

  @Override
  public float getUnitCellInfoType(int infoType) {
    return unitCell.getInfo(infoType);
  }

  @Override
  public String getUnitCellInfo() {
    return unitCell.dumpInfo(false);
  }

  @Override
  public boolean isSlab() {
    return unitCell.isSlab();
  }

  @Override
  public boolean isPolymer() {
    return unitCell.isPolymer();
  }

  @Override
  public boolean checkDistance(P3 f1, P3 f2, float distance, float dx,
                               int iRange, int jRange, int kRange, P3 ptOffset) {
    return unitCell.checkDistance(f1, f2, distance, dx, iRange, jRange, kRange,
        ptOffset);
  }

  @Override
  public P3[] getUnitCellVectors() {
    return unitCell.getUnitCellVectors();
  }

  /**
   * @param oabc  [ptorigin, va, vb, vc]
   * @param setRelative a flag only set true for IsosurfaceMesh
   * @param name
   * @return this SymmetryInterface
   */
  @Override
  public SymmetryInterface getUnitCell(T3[] oabc, boolean setRelative,
                                       String name) {
    if (oabc == null)
      return null;
    unitCell = UnitCell.fromOABC(oabc, setRelative);
    if (name != null)
      unitCell.name = name;
    return this;
  }

  @Override
  public boolean isSupercell() {
    return unitCell.isSupercell();
  }

  @Override
  public BS notInCentroid(ModelSet modelSet, BS bsAtoms, int[] minmax) {
    try {
      BS bsDelete = new BS();
      int iAtom0 = bsAtoms.nextSetBit(0);
      JmolMolecule[] molecules = modelSet.getMolecules();
      int moleculeCount = molecules.length;
      Atom[] atoms = modelSet.at;
      boolean isOneMolecule = (molecules[moleculeCount - 1].firstAtomIndex == modelSet.am[atoms[iAtom0].mi].firstAtomIndex);
      P3 center = new P3();
      boolean centroidPacked = (minmax[6] == 1);
      nextMol: for (int i = moleculeCount; --i >= 0
          && bsAtoms.get(molecules[i].firstAtomIndex);) {
        BS bs = molecules[i].atomList;
        center.set(0, 0, 0);
        int n = 0;
        for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1)) {
          if (isOneMolecule || centroidPacked) {
            center.setT(atoms[j]);
            if (isNotCentroid(center, 1, minmax, centroidPacked)) {
              if (isOneMolecule)
                bsDelete.set(j);
            } else if (!isOneMolecule) {
              continue nextMol;
            }
          } else {
            center.add(atoms[j]);
            n++;
          }
        }
        if (centroidPacked || n > 0 && isNotCentroid(center, n, minmax, false))
          bsDelete.or(bs);
      }
      return bsDelete;
    } catch (Exception e) {
      return null;
    }
  }

  private boolean isNotCentroid(P3 center, int n, int[] minmax,
                                boolean centroidPacked) {
    center.scale(1f / n);
    toFractional(center, false);
    // we have to disallow just a tiny slice of atoms due to rounding errors
    // so  -0.000001 is OK, but 0.999991 is not.
    if (centroidPacked)
      return (center.x + 0.000005f <= minmax[0]
          || center.x - 0.000005f > minmax[3]
          || center.y + 0.000005f <= minmax[1]
          || center.y - 0.000005f > minmax[4]
          || center.z + 0.000005f <= minmax[2] || center.z - 0.000005f > minmax[5]);

    return (center.x + 0.000005f <= minmax[0]
        || center.x + 0.00005f > minmax[3] || center.y + 0.000005f <= minmax[1]
        || center.y + 0.00005f > minmax[4] || center.z + 0.000005f <= minmax[2] || center.z + 0.00005f > minmax[5]);
  }

  // info

  private SymmetryDesc desc;

  private SymmetryDesc getDesc(ModelSet modelSet) {
    return (desc == null ? (desc = ((SymmetryDesc) Interface.getInterface(
        "org.jmol.symmetry.SymmetryDesc", modelSet.vwr, "eval"))) : desc).set(modelSet);
  }

  @Override
  public Object getSymmetryInfoAtom(ModelSet modelSet, int iatom, String xyz,
                                    int op, P3 pt, P3 pt2, String id, int type, float scaleFactor, int nth, int options) {
    return getDesc(modelSet).getSymopInfo(iatom, xyz, op, pt, pt2,
        id, type, scaleFactor, nth, options);
  }

  @Override
  public Map<String, Object> getSpaceGroupInfo(ModelSet modelSet, String sgName, int modelIndex, boolean isFull, float[] cellParams) {
    boolean isForModel = (sgName == null);
    if (sgName == null) {
      Map<String, Object> info = modelSet.getModelAuxiliaryInfo(modelSet.vwr.am.cmi);
      if (info != null)
        sgName = (String) info.get("spaceGroup");
    }
    SymmetryInterface cellInfo = null;
    if (cellParams != null) {
      cellInfo = new Symmetry();
      cellInfo.setUnitCell(cellParams, false);
    }
    return getDesc(modelSet).getSpaceGroupInfo(this, modelIndex, sgName, 0, null, null,
        null, 0, -1, isFull, isForModel, 0, cellInfo);
  }

  
  @Override
  public String fcoord(T3 p) {
    return SymmetryOperation.fcoord(p);
  }

  @Override
  public T3[] getV0abc(Object def) {
    return (unitCell == null ? null : unitCell.getV0abc(def));
  } 

  @Override
  public Quat getQuaternionRotation(String abc) {
    return (unitCell == null ? null : unitCell.getQuaternionRotation(abc));
  }

  @Override
  public T3 getFractionalOrigin() {
    return unitCell.getFractionalOrigin();
  }

  @Override
  public boolean getState(SB commands) {
    T3 pt = getFractionalOffset();
    boolean loadUC = false;
    if (pt != null && (pt.x != 0 || pt.y != 0 || pt.z != 0)) {
      commands.append("; set unitcell ").append(Escape.eP(pt));
      loadUC = true;
    }
    pt = getUnitCellMultiplier();
    if (pt != null) {
      commands.append("; set unitcell ").append(SimpleUnitCell.escapeMultiplier(pt));
      loadUC = true;
    }
    return loadUC;
  }

  @Override
  public AtomIndexIterator getIterator(Viewer vwr, Atom atom, Atom[] atoms,
                                       BS bsAtoms, float radius) {
    return ((UnitCellIterator) Interface.getInterface("org.jmol.symmetry.UnitCellIterator", vwr, "script"))
        .set(this, atom, atoms, bsAtoms, radius);
  }

  @Override
  public boolean toFromPrimitive(boolean toPrimitive, char type, T3[] oabc, M3 primitiveToCrystal) {
    if (unitCell == null)
      unitCell = UnitCell.fromOABC(oabc, false);
    return unitCell.toFromPrimitive(toPrimitive, type, oabc, primitiveToCrystal);
  }

  @Override
  public Lst<P3> generateCrystalClass(P3 pt0) {
     M4[] ops = getSymmetryOperations();
    Lst<P3> lst = new Lst<P3>();
    boolean isRandom = (pt0 == null);
    float rand1=0,rand2=0,rand3=0;
    if (isRandom) {
      rand1 = (float) Math.E;
      rand2 = (float) Math.PI;
      rand3 = (float) Math.log10(2000);
      pt0 = P3.new3(rand1 + 1, rand2 + 2, rand3 + 3);
    } else {
      pt0 = P3.newP(pt0);
    }
    if (ops == null || unitCell == null) {
      lst.addLast(pt0);
    } else {
      unitCell.toFractional(pt0, true); // ignoreOffset
      P3 pt1 = null;
      P3 pt2 = null;
      P3 pt3 = null;
      if (isRandom) {
        pt1 = P3.new3(rand2 + 4, rand3 + 5, rand1 + 6);
        unitCell.toFractional(pt1, true); // ignoreOffset
        pt2 = P3.new3(rand3 + 7, rand1 + 8, rand2 + 9);
        unitCell.toFractional(pt2, true); // ignoreOffset
      }
      Bspt bspt = new Bspt(3, 0);
      CubeIterator iter = bspt.allocateCubeIterator();
      P3 pt = new P3();
      out: for (int i = ops.length; --i >= 0;) {
        ops[i].rotate2(pt0, pt);
        iter.initialize(pt, 0.001f, false);
        if (iter.hasMoreElements())
          continue out;
        P3 ptNew = P3.newP(pt);
        lst.addLast(ptNew);
        bspt.addTuple(ptNew);
        if (isRandom) {
          if (pt2 != null) {
            pt3 = new P3();
            ops[i].rotate2(pt2, pt3);
            lst.addLast(pt3);
          }
          if (pt1 != null) {
            // pt2 is necessary to distinguish between Cs, Ci, and C1
            pt3 = new P3();
            ops[i].rotate2(pt1, pt3);
            lst.addLast(pt3);
          }
        }
      }
      for (int j = lst.size(); --j >= 0;)
        unitCell.toCartesian(lst.get(j), true); // ignoreOffset
    }
    return lst;
  }

  @Override
  public void calculateCIPChiralityForAtoms(Viewer vwr, BS bsAtoms) {
    vwr.setCursor(GenericPlatform.CURSOR_WAIT);
    CIPChirality cip = getCIPChirality(vwr);
    String dataClass = (vwr.getBoolean(T.testflag1) ? "CIPData" : "CIPDataTracker");
    CIPData data = ((CIPData) Interface.getInterface("org.jmol.symmetry." + dataClass, vwr, "script")).set(vwr, bsAtoms);
    data.setRule6Full(vwr.getBoolean(T.ciprule6full));
    cip.getChiralityForAtoms(data);
    vwr.setCursor(GenericPlatform.CURSOR_DEFAULT);
  }
  
  @Override
  public String[] calculateCIPChiralityForSmiles(Viewer vwr, String smiles) throws Exception {
    vwr.setCursor(GenericPlatform.CURSOR_WAIT);
    CIPChirality cip = getCIPChirality(vwr);
    CIPDataSmiles data = ((CIPDataSmiles) Interface.getInterface("org.jmol.symmetry.CIPDataSmiles", vwr, "script")).setAtomsForSmiles(vwr, smiles);
    cip.getChiralityForAtoms(data);
    vwr.setCursor(GenericPlatform.CURSOR_DEFAULT);
       return data.getSmilesChiralityArray();
  }
  
  CIPChirality cip;
  
  private CIPChirality getCIPChirality(Viewer vwr) {
    return (cip == null ? (cip = ((CIPChirality) Interface.getInterface("org.jmol.symmetry.CIPChirality", vwr, "script"))) : cip);
  }

  
  /**
   * return a conventional lattice from a primitive
   * 
   * @param latticeType
   *        "A" "B" "C" "R" etc.
   * @return [origin va vb vc]
   */
  @Override
  public T3[] getConventionalUnitCell(String latticeType,
                                      M3 primitiveToCrystal) {
    return (unitCell == null || latticeType == null ? null
        : unitCell.getConventionalUnitCell(latticeType, primitiveToCrystal));
  }

  @Override
  public Map<String, Object> getUnitCellInfoMap() {
    return (unitCell == null ? null : unitCell.getInfo());
  }

  @Override
  public void setUnitCell(Symmetry uc) {
    unitCell = UnitCell.cloneUnitCell(uc.unitCell);   
  }

}
