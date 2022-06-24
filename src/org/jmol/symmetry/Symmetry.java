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
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.M3d;
import javajs.util.M4d;
import javajs.util.Matrix;
import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.Qd;
import javajs.util.SB;
import javajs.util.T3d;
import javajs.util.V3d;

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
public class Symmetry implements SymmetryInterface {
  // NOTE: THIS CLASS IS VERY IMPORTANT.
  // IN ORDER TO MODULARIZE IT, IT IS REFERENCED USING 
  // xxxx = Interface.getSymmetry();

  SpaceGroup spaceGroup;
  private PointGroup pointGroup;
  private SymmetryInfo symmetryInfo;
  private UnitCell unitCell;
  private CIPChirality cip;
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
  public SymmetryInterface setPointGroup(SymmetryInterface siLast, T3d center,
                                         T3d[] atomset, BS bsAtoms,
                                         boolean haveVibration,
                                         double distanceTolerance,
                                         double linearTolerance,
                                         boolean localEnvOnly) {
    pointGroup = PointGroup.getPointGroup(
        siLast == null ? null : ((Symmetry) siLast).pointGroup, center, atomset,
        bsAtoms, haveVibration, distanceTolerance, linearTolerance,
        localEnvOnly);
    return this;
  }

  @Override
  public String getPointGroupName() {
    return pointGroup.getName();
  }

  @Override
  public Object getPointGroupInfo(int modelIndex, String drawID, boolean asInfo,
                                  String type, int index, double scale) {
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
  public int addBioMoleculeOperation(M4d mat, boolean isReverse) {
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

  /**
   * 
   * @param desiredSpaceGroupIndex
   * @param name
   * @param data
   *        a Lst<SymmetryOperation> or Lst<M4d>
   * @param modDim
   *        in [3+d] modulation dimension
   * @return true if a known space group
   */
  @Override
  public boolean createSpaceGroup(int desiredSpaceGroupIndex, String name,
                                  Object data, int modDim) {
    spaceGroup = SpaceGroup.createSpaceGroup(desiredSpaceGroupIndex, name, data,
        modDim);
    if (spaceGroup != null && Logger.debugging)
      Logger.debug("using generated space group " + spaceGroup.dumpInfo());
    return spaceGroup != null;
  }

  @Override
  public Object getSpaceGroupInfoObj(String name, SymmetryInterface cellInfo,
                                     boolean isFull, boolean addNonstandard) {
    return SpaceGroup.getInfo(spaceGroup, name, cellInfo, isFull,
        addNonstandard);
  }

  @Override
  public Object getLatticeDesignation() {
    return spaceGroup.getLatticeDesignation();
  }

  @Override
  public void setFinalOperations(int dim, String name, P3d[] atoms,
                                 int iAtomFirst, int noSymmetryCount,
                                 boolean doNormalize, String filterSymop) {
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
    spaceGroup.setFinalOperationsForAtoms(dim, atoms, iAtomFirst,
        noSymmetryCount, doNormalize);
  }

  @Override
  public M4d getSpaceGroupOperation(int i) {
    return (spaceGroup == null || spaceGroup.operations == null // bio 
        || i >= spaceGroup.operations.length ? null
            : spaceGroup.finalOperations == null ? spaceGroup.operations[i]
                : spaceGroup.finalOperations[i]);
  }

//  @Override
//  public M4d getSpaceGroupOperationRaw(int i) {
//    return spaceGroup.getRawOperation(i);
//  }

  @Override
  public String getSpaceGroupXyz(int i, boolean doNormalize) {
    return spaceGroup.getXyz(i, doNormalize);
  }

  @Override
  public void newSpaceGroupPoint(P3d pt, int i, M4d o, int transX,
                                 int transY, int transZ, P3d retPoint) {
    if (o == null && spaceGroup.finalOperations == null) {
      SymmetryOperation op = spaceGroup.operations[i];
      // temporary spacegroups don't have to have finalOperations
      if (!op.isFinalized)
        op.doFinalize();
      o = op;
    }
    newPoint((o == null ? spaceGroup.finalOperations[i] : o), pt, transX,
        transY, transZ, retPoint);
  }

  @Override
  public V3d[] rotateAxes(int iop, V3d[] axes, P3d ptTemp, M3d mTemp) {
    return (iop == 0 ? axes
        : spaceGroup.finalOperations[iop].rotateAxes(axes, unitCell, ptTemp,
            mTemp));
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
  public int getSpinOp(int op) {
    return spaceGroup.operations[op].getMagneticOp();
  }

  @Override
  public boolean addLatticeVectors(Lst<double[]> lattvecs) {
    return spaceGroup.addLatticeVectors(lattvecs);
  }

  @Override
  public int getLatticeOp() {
    return spaceGroup.latticeOp;
  }

  @Override
  public Lst<P3d> getLatticeCentering() {
    return SymmetryOperation.getLatticeCentering(getSymmetryOperations());
  }

  @Override
  public Matrix getOperationRsVs(int iop) {
    return (spaceGroup.finalOperations == null ? spaceGroup.operations
        : spaceGroup.finalOperations)[iop].rsvs;
  }

  @Override
  public int getSiteMultiplicity(P3d pt) {
    return spaceGroup.getSiteMultiplicity(pt, unitCell);
  }

  @Override
  /**
   * @param rot
   *        is a full (3+d)x(3+d) array of epsilons
   * @param trans
   *        is a (3+d)x(1) array of translations
   * @return Jones-Faithful representation
   */
  public String addSubSystemOp(String code, Matrix rs, Matrix vs,
                               Matrix sigma) {
    spaceGroup.isSSG = true;
    String s = SymmetryOperation.getXYZFromRsVs(rs, vs, false);
    int i = spaceGroup.addSymmetry(s, -1, true);
    spaceGroup.operations[i].setSigma(code, sigma);
    return s;
  }

  @Override
  public String getMatrixFromString(String xyz, double[] rotTransMatrix,
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
        : spaceGroup != null ? spaceGroup.getName()
            : unitCell != null && unitCell.name.length() > 0
                ? "cell=" + unitCell.name
                : "");
  }

  @Override
  public String getSpaceGroupNameType(String type) {
    return (spaceGroup == null ? null : spaceGroup.getNameType(type, this));
  }

  @Override
  public void setSpaceGroupName(String name) {
    if (spaceGroup != null)
      spaceGroup.setName(name);
  }

  @Override
  public int getSpaceGroupOperationCount() {
    return (symmetryInfo != null 
        && symmetryInfo.symmetryOperations != null ? // null here for PDB 
            symmetryInfo.symmetryOperations.length
        : spaceGroup != null && spaceGroup.finalOperations != null
            ? spaceGroup.finalOperations.length
            : 0);
  }

  @Override
  public char getLatticeType() {
    return (symmetryInfo != null ? symmetryInfo.latticeType
        : spaceGroup == null ? 'P' : spaceGroup.latticeType);
  }

  @Override
  public String getIntTableNumber() {
    return (symmetryInfo != null ? symmetryInfo.intlTableNo
        : spaceGroup == null ? null : spaceGroup.intlTableNumber);
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
    if (symmetryInfo != null)
      return symmetryInfo.infoStr;
    if (spaceGroup == null)
      return "";
    symmetryInfo = new SymmetryInfo();
    symmetryInfo.setSymmetryInfo(null, getUnitCellParams(), spaceGroup);
    return symmetryInfo.infoStr;
  }

  @Override
  public SymmetryOperation[] getSymmetryOperations() {
    if (symmetryInfo != null)
      return symmetryInfo.symmetryOperations;
    if (spaceGroup == null)
      spaceGroup = SpaceGroup.getNull(true, false, true);
    spaceGroup.setFinalOperations();
    return spaceGroup.finalOperations;
  }

  @Override
  public boolean isSimple() {
    return (spaceGroup == null
        && (symmetryInfo == null || symmetryInfo.symmetryOperations == null));
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
                                           double[] unitCellParams) {
    symmetryInfo = new SymmetryInfo();
    double[] params = symmetryInfo.setSymmetryInfo(modelAuxiliaryInfo,
        unitCellParams, null);
    if (params != null) {
      setUnitCell(params, modelAuxiliaryInfo.containsKey("jmolData"));
      unitCell.moreInfo = (Lst<String>) modelAuxiliaryInfo
          .get("moreUnitCellInfo");
      modelAuxiliaryInfo.put("infoUnitCell", getUnitCellAsArray(false));
      setOffsetPt((T3d) modelAuxiliaryInfo.get("unitCellOffset"));
      M3d matUnitCellOrientation = (M3d) modelAuxiliaryInfo
          .get("matUnitCellOrientation");
      if (matUnitCellOrientation != null)
        initializeOrientation(matUnitCellOrientation);
      if (Logger.debugging)
        Logger.debug("symmetryInfos[" + modelIndex + "]:\n"
            + unitCell.dumpInfo(true, true));
    }
    return this;
  }

  // UnitCell methods

  @Override
  public boolean haveUnitCell() {
    return (unitCell != null);
  }

  @Override
  public SymmetryInterface setUnitCell(double[] unitCellParams,
                                       boolean setRelative) {
    unitCell = UnitCell.fromParams(unitCellParams, setRelative);
    return this;
  }

  @Override
  public boolean unitCellEquals(SymmetryInterface uc2) {
    return ((Symmetry) (uc2)).unitCell.isSameAs(unitCell);
  }

  @Override
  public String getUnitCellState() {
    if (unitCell == null)
      return "";
    return unitCell.getState();
  }

  @Override
  public Lst<String> getMoreInfo() {
    return unitCell.moreInfo;
  }

  public String getUnitsymmetryInfo() {
    // not used in Jmol?
    return unitCell.dumpInfo(false, true);
  }

  @Override
  public void initializeOrientation(M3d mat) {
    unitCell.initOrientation(mat);
  }

  @Override
  public void unitize(T3d ptFrac) {
    unitCell.unitize(ptFrac);
  }

  @Override
  public void toUnitCellD(T3d pt, T3d offset) {
    unitCell.toUnitCellD(pt, offset);
  }

  @Override
  public void toUnitCellRnd(T3d pt, T3d offset) {
    unitCell.toUnitCellRnd(pt, offset);
  }

  @Override
  public P3d toSupercell(P3d fpt) {
    return unitCell.toSupercell(fpt);
  }

  @Override
  public void toFractional(T3d pt, boolean ignoreOffset) {
    if (!isBio)
      unitCell.toFractional(pt, ignoreOffset);
  }

  @Override
  public void toFractionalM(M4d m) {
    if (!isBio)
      unitCell.toFractionalM(m);
  }

  @Override
  public void toCartesian(T3d pt, boolean ignoreOffset) {
    if (!isBio)
      unitCell.toCartesian(pt, ignoreOffset);
  }

  @Override
  public double[] getUnitCellParams() {
    return unitCell.getUnitCellParams();
  }

//  @Override
//  public double[] getUnitCellParamsF() {
//    return unitCell.getUnitCellParamsF();
//  }
//
  @Override
  public double[] getUnitCellAsArray(boolean vectorsOnly) {
    return unitCell.getUnitCellAsArray(vectorsOnly);
  }

  @Override
  public Tensor getTensor(Viewer vwr, double[] parBorU) {
    if (parBorU == null)
      return null;
    if (unitCell == null)
      unitCell = UnitCell.fromParams(new double[] { 1, 1, 1, 90, 90, 90 }, true);
    return unitCell.getTensor(vwr, parBorU);
  }

  @Override
  public P3d[] getUnitCellVerticesNoOffset() {
    return unitCell.getVertices();
  }

  @Override
  public P3d getCartesianOffset() {
    return unitCell.getCartesianOffset();
  }

  @Override
  public P3d getFractionalOffset() {
    return unitCell.getFractionalOffset();
  }

  @Override
  public void setOffsetPt(T3d pt) {
    unitCell.setOffset(pt);
  }

  @Override
  public void setOffset(int nnn) {
    P3d pt = new P3d();
    SimpleUnitCell.ijkToPoint3f(nnn, pt, 0, 0);
    unitCell.setOffset(pt);
  }

  @Override
  public T3d getUnitCellMultiplier() {
    return unitCell.getUnitCellMultiplier();
  }

  @Override
  public SymmetryInterface getUnitCellMultiplied() {
    UnitCell uc = unitCell.getUnitCellMultiplied();
    if (uc == unitCell)
      return this;
    Symmetry s = new Symmetry();
    s.unitCell = uc;
    return s;
  }

  @Override
  public P3d[] getCanonicalCopy(double scale, boolean withOffset) {
    return unitCell.getCanonicalCopy(scale, withOffset);
  }

  @Override
  public double getUnitCellInfoType(int infoType) {
    return unitCell.getInfo(infoType);
  }

  @Override
  public String getUnitCellInfo(boolean scaled) {
    return unitCell.dumpInfo(false, scaled);
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
  public boolean checkDistance(P3d f1, P3d f2, double distance, double dx,
                               int iRange, int jRange, int kRange,
                               P3d ptOffset) {
    return unitCell.checkDistance(f1, f2, distance, dx, iRange, jRange, kRange,
        ptOffset);
  }

  @Override
  public P3d[] getUnitCellVectors() {
    return unitCell.getUnitCellVectorsD();
  }

  /**
   * @param oabc
   *        [ptorigin, va, vb, vc]
   * @param setRelative
   *        a flag only set true for IsosurfaceMesh
   * @param name
   * @return this SymmetryInterface
   */
  @Override
  public SymmetryInterface getUnitCell(T3d[] oabc, boolean setRelative,
                                       String name) {
    if (oabc == null)
      return null;
    unitCell = UnitCell.fromOABCd(oabc, setRelative);
    if (name != null)
      unitCell.name = name;
    return this;
  }

  @Override
  public SymmetryInterface getUnitCelld(T3d[] oabc, boolean setRelative,
                                       String name) {
    if (oabc == null)
      return null;
    unitCell = UnitCell.fromOABCd(oabc, setRelative);
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
      boolean isOneMolecule = (molecules[moleculeCount
          - 1].firstAtomIndex == modelSet.am[atoms[iAtom0].mi].firstAtomIndex);
      P3d center = new P3d();
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

  private boolean isNotCentroid(P3d center, int n, int[] minmax,
                                boolean centroidPacked) {
    center.scale(1d / n);
    toFractional(center, false);
    // we have to disallow just a tiny slice of atoms due to rounding errors
    // so  -0.000001 is OK, but 0.999991 is not.
    if (centroidPacked)
      return (center.x + 0.000005d <= minmax[0]
          || center.x - 0.000005d > minmax[3]
          || center.y + 0.000005d <= minmax[1]
          || center.y - 0.000005d > minmax[4]
          || center.z + 0.000005d <= minmax[2]
          || center.z - 0.000005d > minmax[5]);

    return (center.x + 0.000005d <= minmax[0] || center.x + 0.00005d > minmax[3]
        || center.y + 0.000005d <= minmax[1] || center.y + 0.00005d > minmax[4]
        || center.z + 0.000005d <= minmax[2]
        || center.z + 0.00005d > minmax[5]);
  }

  // info

  private SymmetryDesc desc;
  private static SymmetryDesc nullDesc;

  private SymmetryDesc getDesc(ModelSet modelSet) {
    if (modelSet == null) {
      return (nullDesc == null
          ? (nullDesc = ((SymmetryDesc) Interface.getInterface(
              "org.jmol.symmetry.SymmetryDesc", null, "modelkit")))
          : nullDesc);
    }
    return (desc == null
        ? (desc = ((SymmetryDesc) Interface.getInterface(
            "org.jmol.symmetry.SymmetryDesc", modelSet.vwr, "eval")))
        : desc).set(modelSet);
  }

  @Override
  public Object getSymmetryInfoAtom(ModelSet modelSet, int iatom, String xyz,
                                    int op, P3d translation, P3d pt, P3d pt2,
                                    String id, int type, double scaleFactor,
                                    int nth, int options) {
    return getDesc(modelSet).getSymopInfo(iatom, xyz, op, translation, pt, pt2,
        id, type, scaleFactor, nth, options);
  }

  @Override
  public Map<String, Object> getSpaceGroupInfo(ModelSet modelSet, String sgName,
                                               int modelIndex, boolean isFull,
                                               double[] cellParams) {
    boolean isForModel = (sgName == null);
    if (sgName == null) {
      Map<String, Object> info = modelSet
          .getModelAuxiliaryInfo(modelSet.vwr.am.cmi);
      if (info != null)
        sgName = (String) info.get("spaceGroup");
    }
    SymmetryInterface cellInfo = null;
    if (cellParams != null) {
      cellInfo = new Symmetry().setUnitCell(cellParams, false);
    }
    return getDesc(modelSet).getSpaceGroupInfo(this, modelIndex, sgName, 0,
        null, null, null, 0, -1, isFull, isForModel, 0, cellInfo, null);
  }

  @Override
  public String fcoord(T3d p) {
    return SymmetryOperation.fcoord(p);
  }

  @Override
  public T3d[] getV0abc(Object def, M4d retMatrix) {
    return (unitCell == null ? null : unitCell.getV0abc(def, retMatrix));
  }

  @Override
  public Qd getQuaternionRotation(String abc) {
    return (unitCell == null ? null : unitCell.getQuaternionRotation(abc));
  }

  @Override
  public P3d getFractionalOrigin() {
    return unitCell.getFractionalOrigin();
  }

  @Override
  public boolean getState(ModelSet ms, int modelIndex, SB commands) {
    T3d pt = getFractionalOffset();
    boolean loadUC = false;
    if (pt != null && (pt.x != 0 || pt.y != 0 || pt.z != 0)) {
      commands.append("; set unitcell ").append(Escape.eP(pt));
      loadUC = true;
    }
    T3d ptm = getUnitCellMultiplier();
    if (ptm != null) {
      commands.append("; set unitcell ")
          .append(SimpleUnitCell.escapeMultiplier(ptm));
      loadUC = true;
    }
    String sg0 = (String) ms.getInfo(modelIndex, "spaceGroupOriginal");
    String sg = (String) ms.getInfo(modelIndex, "spaceGroup");
    if (sg0 != null && sg != null && !sg.equals(sg0)) {
      commands.append("\nMODELKIT SPACEGROUP " + PT.esc(sg));
      loadUC = true;
    }
    return loadUC;
  }

  @Override
  public AtomIndexIterator getIterator(Viewer vwr, Atom atom, BS bsAtoms,
                                       double radius) {
    return ((UnitCellIterator) Interface
        .getInterface("org.jmol.symmetry.UnitCellIterator", vwr, "script"))
            .set(this, atom, vwr.ms.at, bsAtoms, radius);
  }

  @Override
  public boolean toFromPrimitive(boolean toPrimitive, char type, T3d[] oabc,
                                 M3d primitiveToCrystal) {
    if (unitCell == null)
      unitCell = UnitCell.fromOABCd(oabc, false);
    return unitCell.toFromPrimitive(toPrimitive, type, oabc,
        primitiveToCrystal);
  }

  @Override
  public Lst<P3d> generateCrystalClass(P3d pt00) {
    M4d[] ops = getSymmetryOperations();
    Lst<P3d> lst = new Lst<P3d>();
    boolean isRandom = (pt00 == null);
    double rand1 = 0, rand2 = 0, rand3 = 0;
    P3d pt0;
    if (isRandom) {
      rand1 = Math.E;
      rand2 = Math.PI;
      rand3 = Math.log10(2000);
      pt0 = P3d.new3(rand1 + 1, rand2 + 2, rand3 + 3);
    } else {
      pt0 = P3d.newPd(pt00);
    }
    if (ops == null || unitCell == null) {
      lst.addLast(pt0);
    } else {
      unitCell.toFractional(pt0, true); // ignoreOffset
      P3d pt1 = null;
      P3d pt2 = null;
      if (isRandom) {
        pt1 = P3d.new3(rand2 + 4, rand3 + 5, rand1 + 6);
        unitCell.toFractional(pt1, true); // ignoreOffset
        pt2 = P3d.new3(rand3 + 7, rand1 + 8, rand2 + 9);
        unitCell.toFractional(pt2, true); // ignoreOffset
      }
      Bspt bspt = new Bspt(3, 0);
      CubeIterator iter = bspt.allocateCubeIterator();
      P3d pt = new P3d();
      out: for (int i = ops.length; --i >= 0;) {
        ops[i].rotate2(pt0, pt);
        iter.initialize(pt, 0.001f, false);
        if (iter.hasMoreElements())
          continue out;
        P3d ptNew = P3d.newP(pt);
        lst.addLast(ptNew);
        bspt.addTuple(ptNew);
        if (isRandom) {
          if (pt2 != null) {
            ops[i].rotate2(pt2, pt);
            lst.addLast(P3d.newP(pt));
          }
          if (pt1 != null) {
            // pt2 is necessary to distinguish between Cs, Ci, and C1
            ops[i].rotate2(pt1, pt);
            lst.addLast(P3d.newP(pt));
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
    String dataClass = (vwr.getBoolean(T.testflag1) ? "CIPData"
        : "CIPDataTracker");
    CIPData data = ((CIPData) Interface
        .getInterface("org.jmol.symmetry." + dataClass, vwr, "script")).set(vwr,
            bsAtoms);
    data.setRule6Full(vwr.getBoolean(T.ciprule6full));
    cip.getChiralityForAtoms(data);
    vwr.setCursor(GenericPlatform.CURSOR_DEFAULT);
  }

  @Override
  public String[] calculateCIPChiralityForSmiles(Viewer vwr, String smiles)
      throws Exception {
    vwr.setCursor(GenericPlatform.CURSOR_WAIT);
    CIPChirality cip = getCIPChirality(vwr);
    CIPDataSmiles data = ((CIPDataSmiles) Interface
        .getInterface("org.jmol.symmetry.CIPDataSmiles", vwr, "script"))
            .setAtomsForSmiles(vwr, smiles);
    cip.getChiralityForAtoms(data);
    vwr.setCursor(GenericPlatform.CURSOR_DEFAULT);
    return data.getSmilesChiralityArray();
  }

  private CIPChirality getCIPChirality(Viewer vwr) {
    return (cip == null
        ? (cip = ((CIPChirality) Interface
            .getInterface("org.jmol.symmetry.CIPChirality", vwr, "script")))
        : cip);
  }

  /**
   * return a conventional lattice from a primitive
   * 
   * @param latticeType
   *        "A" "B" "C" "R" etc.
   * @return [origin va vb vc]
   */
  @Override
  public T3d[] getConventionalUnitCell(String latticeType,
                                      M3d primitiveToCrystal) {
    return (unitCell == null || latticeType == null ? null
        : unitCell.getConventionalUnitCell(latticeType, primitiveToCrystal));
  }

  @Override
  public Map<String, Object> getUnitCellInfoMap() {
    return (unitCell == null ? null : unitCell.getInfo());
  }

  @Override
  public void setUnitCell(SymmetryInterface uc) {
    unitCell = UnitCell.cloneUnitCell(((Symmetry) uc).unitCell);
  }

  @Override
  public Object findSpaceGroup(Viewer vwr, BS atoms, String opXYZ,
                               boolean asString) {
    return ((SpaceGroupFinder) Interface
        .getInterface("org.jmol.symmetry.SpaceGroupFinder", vwr, "eval"))
            .findSpaceGroup(vwr, atoms, opXYZ, this, asString);
  }

  @Override
  public void setSpaceGroupTo(Object sg) {
    symmetryInfo = null;
    if (sg instanceof SpaceGroup) {
      spaceGroup = (SpaceGroup) sg;
    } else {
      spaceGroup = SpaceGroup.getSpaceGroupFromITAName(sg.toString());
    }
  }

  @Override
  public BS removeDuplicates(ModelSet ms, BS bs) {
    UnitCell uc = this.unitCell;
    Atom[] atoms = ms.at;
    double[] occs = ms.occupancies;
    boolean haveOccupancies = (occs != null);
    P3d pt = new P3d();
    P3d pt2 = new P3d();
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      Atom a = atoms[i];
      pt.setT(a);
      uc.toFractional(pt, false);
      uc.unitizeRnd(pt);
      int type = a.getAtomicAndIsotopeNumber();

      double occ = (haveOccupancies ? occs[i] : 0);
      for (int j = bs.nextSetBit(i + 1); j >= 0; j = bs.nextSetBit(j + 1)) {
        Atom b = atoms[j];
        if (type != b.getAtomicAndIsotopeNumber()
            || (haveOccupancies && occ != occs[j]))
          continue;
        pt2.setT(b);
        uc.toFractional(pt2, false);
        uc.unitizeRnd(pt2);
        if (pt.distanceSquared(pt2) < JC.UC_TOLERANCE2) {
          bs.clear(j);
        }
      }
    }
    return bs;
  }

  @Override
  public Lst<P3d> getEquivPoints(Lst<P3d> pts, P3d pt, String flags) {
    M4d[] ops = getSymmetryOperations();
    return (ops == null || unitCell == null ? null
        : unitCell.getEquivPoints(pt, flags, ops,
            pts == null ? new Lst<P3d>() : pts, 0, 0));
  }

  @Override
  public void getEquivPointList(Lst<P3d> pts, int nIgnored, String flags) {
    M4d[] ops = getSymmetryOperations();
    boolean newPt = (flags.indexOf("newpt") >= 0);
    boolean zapped = (flags.indexOf("zapped") >= 0);
    // we will preserve the points temporarily, then remove them at the end
    int n = pts.size();
    boolean tofractional = (flags.indexOf("tofractional") >= 0);
    // fractionalize all points if necessary
    if (flags.indexOf("fromfractional") < 0) {
      for (int i = 0; i < pts.size(); i++) {
        toFractional(pts.get(i), true);
      }
    }
    // signal to make no changes in points
    flags += ",fromfractional,tofractional";
    int check0 = (nIgnored > 0 ? 0 : n);
    boolean allPoints = (nIgnored == n);
    int n0 = (nIgnored > 0 ? nIgnored : n);
    if (allPoints) {
      nIgnored--;
      n0--;
    }
    if (zapped)
      n0 = 0;
    P3d p0 = (nIgnored > 0 ? pts.get(nIgnored) : null);
    if (ops != null || unitCell != null) {
      for (int i = nIgnored; i < n; i++) {
        unitCell.getEquivPoints(pts.get(i), flags, ops, pts, check0, n0);
      }
    }
    // now remove the starting points, checking to see if perhaps our
    // test point itself has been removed.
    if (!zapped && (pts.size() == nIgnored || pts.get(nIgnored) != p0
        || allPoints || newPt))
      n--;
    for (int i = n - nIgnored; --i >= 0;)
      pts.removeItemAt(nIgnored);
    // final check for removing duplicates
    //    if (nIgnored > 0)
    //      UnitCell.checkDuplicate(pts, 0, nIgnored - 1, nIgnored);

    // and turn these to Cartesians if desired
    if (!tofractional) {
      for (int i = pts.size(); --i >= nIgnored;)
        toCartesian(pts.get(i), true);
    }
  }

  @Override
  public int[] getInvariantSymops(P3d pt, int[] v0) {
    M4d[] ops = getSymmetryOperations();
    if (ops == null)
      return new int[0];
    BS bs = new BS();
    P3d p = new P3d();
    P3d p0 = new P3d();
    int nops = ops.length;
    for (int i = 1; i < nops; i++) {
      p.setT(pt);
      toFractional(p, true);
      // unitize here should take care of all Wyckoff positions
      unitCell.unitize(p);
      p0.setT(p);
      ops[i].rotTrans(p);
      unitCell.unitize(p);
      if (p0.distanceSquared(p) < JC.UC_TOLERANCE2) {
        bs.set(i);
      }
    }
    int[] ret = new int[bs.cardinality()];
    if (v0 != null && ret.length != v0.length)
      return null;
    for (int k = 0, i = 1; i < nops; i++) {
      boolean isOK = bs.get(i);
      if (isOK) {
        if (v0 != null && v0[k] != i + 1)
          return null;
        ret[k++] = i + 1;
      }
    }
    return ret;
  }

  /**
   * @param fracA
   * @param fracB
   * @return matrix
   */
  @Override
  public M4d getTransform(P3d fracA, P3d fracB, boolean best) {
    return getDesc(null).getTransform(unitCell, getSymmetryOperations(), fracA,
        fracB, best);
  }

  static void newPoint(M4d m, P3d atom1, int x, int y, int z, P3d atom2) {
    m.rotTrans2(atom1, atom2);
    atom2.add3(x, y, z);
  }

}
