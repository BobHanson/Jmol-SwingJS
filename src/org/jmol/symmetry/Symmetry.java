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

import java.io.BufferedReader;
import java.util.Hashtable;
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
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.JSJSONParser;
import javajs.util.Lst;
import javajs.util.M3d;
import javajs.util.M4d;
import javajs.util.Matrix;
import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.Qd;
import javajs.util.Rdr;
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

  private static SymmetryDesc nullDesc;
  private static Map<String, Object> aflowStructures;
  private static Map<String, Object>[] itaData;
  private static Map<String, Object>[] itaSubData;
  private static Map<String, Object>[] planeData, layerData, rodData, friezeData;
  // TODO: plane and subperiodic subgroup info
  private static Map<String, Object>[] planeSubData, layerSubData, rodSubData, friezeSubData;
  private static Lst<Object> allDataITA, allPlaneData, allLayerData, allRodData, allFriezeData;
  private static WyckoffFinder wyckoffFinder;
  private static CLEG clegInstance;
  
  public SpaceGroup spaceGroup;
  public UnitCell unitCell;
  public boolean isBio;

  PointGroup pointGroup;
  CIPChirality cip;

  private SymmetryInfo symmetryInfo;
  private SymmetryDesc desc;
  private M4d transformMatrix;
 
  @Override
  public String[] getSymopList(boolean doNormalize) {
    int n = spaceGroup.operationCount;
    String[] list = new String[n];
    for (int i = 0; i < n; i++)
      list[i] = "" + getSpaceGroupXyz(i, doNormalize);
    return list;
  }

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
  public SymmetryInterface setPointGroup(Viewer vwr, SymmetryInterface siLast,
                                         T3d center, T3d[] atomset, BS bsAtoms,
                                         boolean haveVibration,
                                         double distanceTolerance,
                                         double linearTolerance, int maxAtoms,
                                         boolean localEnvOnly) {
    pointGroup = PointGroup.getPointGroup(
        siLast == null ? null : ((Symmetry) siLast).pointGroup, center, atomset,
        bsAtoms, haveVibration, distanceTolerance, linearTolerance, maxAtoms,
        localEnvOnly, vwr.getBoolean(T.symmetryhermannmauguin),
        vwr.getScalePixelsPerAngstrom(false));
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
    symmetryInfo = null;
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

  @SuppressWarnings("unchecked")
  @Override
  public Object getSpaceGroupInfoObj(String name, Object params, boolean isFull,
                                     boolean addNonstandard) {
    boolean isNumOrTrm = false;
    switch (name) {
    case "list":
      // from spacegroup(n, "list")
      return getSpaceGroupList((Integer) params);
    case "opsCtr":
      return spaceGroup.getOpsCtr((String) params);
    case "itaTransform":
    case "itaNumber":
      isNumOrTrm = true;
      //$FALL-THROUGH$
    case "nameToXYZList": 
    case "itaIndex":
    case "hmName":
      SpaceGroup sg = null;
      if (params != null) {
        String s = (String) params;
        if (s.endsWith("'")) {
          // Jmol-specific Wyckoff type names get cleg
          s = SpaceGroup.convertWyckoffHMCleg(s, null);
          if (isNumOrTrm && s != null) {
            int pt = s.indexOf(":");
            return ("itaNumber".equals(name) ? s.substring(0, pt) : s.substring(pt + 1));
          }
          return null;
        }
        if (s.length() > 1 && s.charAt(1) == '/') {
          // get first item if nnn and not nnn.m
          if (s.indexOf('.') < 0  && s.indexOf(":") < 0 && PT.isDigit(s.charAt(2))) {
            s += ".";
          }
          Map<String, Object> info = (Map<String, Object>) getSpaceGroupJSON(vwr, "ITA", s, 0);
          switch (info == null ? "" : name) {
          case "itaData":
            return info;
          case "hmName":
            return info.get("hm");
          case "nameToXYZList":
            return info.get("gp");
          case "itaIndex":
            return "" + info.get("sg") + "." + info.get("set");
          case "itaTransform":
            return info.get("trm");
          case "itaNumber": // as String here
            return "" + info.get("sg");
          }
          return null;
          
        }
        sg = SpaceGroup.determineSpaceGroupN(s);
        if (sg == null && "nameToXYZList".equals(name))
          sg = SpaceGroup.createSpaceGroupN(s, true);
      } else if (spaceGroup != null) {
        sg = spaceGroup;
      } else if (symmetryInfo != null) {
        sg = symmetryInfo.getDerivedSpaceGroup();
      }
      switch (sg == null ? "" : name) {
      case "hmName":
        return sg.getHMName();
      case "nameToXYZList":
        Lst<Object> genPos = new Lst<Object>();
        sg.setFinalOperations();
        for (int i = 0, n = sg.getOperationCount(); i < n; i++) {
          genPos.addLast(((SymmetryOperation) sg.getOperation(i)).xyz);
        }
        return genPos;
      case "itaIndex":
        return sg.getItaIndex();
      case "itaTransform":
        return sg.itaTransform;
      case "itaNumber":
        return sg.itaNumber;
      }
      return null;
    default:
      return SpaceGroup.getInfo(spaceGroup, name, (double[]) params, isFull,
          addNonstandard);
    }
  }

  @SuppressWarnings("unchecked")
  private String getSpaceGroupList(Integer sg0) {
    SB sb = new SB();
    Lst<Object> list = (Lst<Object>) getSpaceGroupJSON(vwr, "ITA", "ALL", 0);
    for (int i = 0, n = list.size(); i < n; i++) {
      Map<String, Object> map = (Map<String, Object>) list.get(i);
      Integer sg = (Integer) map.get("sg");
      if (sg0 == null || sg.equals(sg0))
        sb.appendO(sg).appendC('.').appendO(map.get("set"))
          .appendC('\t').appendO(map.get("hm")).appendC('\t').appendO(map.get("sg")).appendC(':').appendO(map.get("trm")).appendC('\n');
    }
    return sb.toString();
  }

  @Override
  public Object getLatticeDesignation() {
    return spaceGroup.getShelxLATTDesignation();
  }

  @Override
  public void setFinalOperations(int dim, String name, P3d[] atoms,
                                 int iAtomFirst, int noSymmetryCount,
                                 boolean doNormalize, String filterSymop) {
    if (name != null && (name.startsWith("bio") || name.indexOf(" *(") >= 0)) // filter SYMOP
      spaceGroup.setName(name);
    boolean doCalculate = "unspecified!".equals(name);
    if (doCalculate)
      filterSymop = "calculated";
    if (filterSymop != null) {
      Lst<SymmetryOperation> lst = new Lst<SymmetryOperation>();
      lst.addLast(spaceGroup.matrixOperations[0]);
      for (int i = 1; i < spaceGroup.operationCount; i++)
        if (doCalculate || filterSymop.contains(" " + (i + 1) + " "))
          lst.addLast(spaceGroup.matrixOperations[i]);
      spaceGroup = SpaceGroup.createSpaceGroup(-1,
          name + " *(" + filterSymop.trim() + ")", lst, -1);
    }
    spaceGroup.setFinalOperationsForAtoms(dim, atoms, iAtomFirst,
        noSymmetryCount, doNormalize);
  }

  @Override
  public M4d getSpaceGroupOperation(int i) {
    return (spaceGroup == null || spaceGroup.matrixOperations == null // bio 
        || i >= spaceGroup.matrixOperations.length ? null
            : spaceGroup.finalOperations == null ? spaceGroup.matrixOperations[i]
                : spaceGroup.finalOperations[i]);
  }

  @Override
  public String getSpaceGroupXyz(int i, boolean doNormalize) {
    return spaceGroup.getXyz(i, doNormalize);
  }

  @Override
  public void newSpaceGroupPoint(P3d pt, int i, M4d o, int transX, int transY,
                                 int transZ, P3d retPoint) {
    if (o == null && spaceGroup.finalOperations == null) {
      SymmetryOperation op = spaceGroup.matrixOperations[i];
      // temporary spacegroups don't have to have finalOperations
      if (!op.isFinalized)
        op.doFinalize();
      o = op;
    }
    SymmetryOperation.rotateAndTranslatePoint(
        (o == null ? spaceGroup.finalOperations[i] : o), pt, transX, transY,
        transZ, retPoint);
  }

  @Override
  public V3d[] rotateAxes(int iop, V3d[] axes, P3d ptTemp, M3d mTemp) {
    return (iop == 0 ? axes
        : spaceGroup.finalOperations[iop].rotateAxes(axes, unitCell, ptTemp,
            mTemp));
  }

  @Override
  public int getSpinOp(int op) {
    return spaceGroup.matrixOperations[op].getMagneticOp();
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
    return (spaceGroup.finalOperations == null ? spaceGroup.matrixOperations
        : spaceGroup.finalOperations)[iop].rsvs;
  }

  @Override
  public int getSiteMultiplicity(P3d pt) {
    return spaceGroup.getSiteMultiplicity(pt, unitCell);
  }

  @Override
  public String getSpaceGroupName() {
    return (spaceGroup != null ? spaceGroup.getName()
        : symmetryInfo != null ? symmetryInfo.sgName
            : unitCell != null && unitCell.name.length() > 0
                ? "cell=" + unitCell.name
                : "");
  }

  @Override
  public String geCIFWriterValue(String type) {
    return (spaceGroup == null ? null : spaceGroup.getCIFWriterValue(type, this));
  }

  @Override
  public char getLatticeType() {
    return (symmetryInfo != null ? symmetryInfo.latticeType
        : spaceGroup == null ? 'P' : spaceGroup.latticeType);
  }

  @Override
  public String getIntTableNumber() {
    return (symmetryInfo != null ? symmetryInfo.intlTableNo
        : spaceGroup == null ? null : spaceGroup.itaNumber);
  }

  @Override
  public String getIntTableIndex() {
    return (symmetryInfo != null ? symmetryInfo.intlTableIndexNdotM
        : spaceGroup == null ? null : spaceGroup.getItaIndex());
  }

  @Override
  public String getIntTableTransform() {
    return (symmetryInfo != null ? symmetryInfo.intlTableTransform
        : spaceGroup == null ? null : spaceGroup.itaTransform);
  }

  @Override
  public String getSpaceGroupClegId() {
    return (symmetryInfo != null ? symmetryInfo.getClegId() : spaceGroup.getClegId());
  }

  @Override
  public String getSpaceGroupJmolId() {
    return (symmetryInfo != null ? symmetryInfo.intlTableJmolId
        : spaceGroup == null ? null : spaceGroup.jmolId);
  }

  @Override
  public boolean getCoordinatesAreFractional() {
    return symmetryInfo == null || symmetryInfo.coordinatesAreFractional;
  }

  @Override
  public int[] getCellRange() {
    return symmetryInfo == null ? null : symmetryInfo.cellRange;
  }

  /**
   * When information is desired about the space group, we use SymmetryInfo.
   * 
   */
  @Override
  public String getSymmetryInfoStr() {
    if (symmetryInfo != null)
      return symmetryInfo.infoStr;
    if (spaceGroup == null)
      return "";
    (symmetryInfo = new SymmetryInfo()).setSymmetryInfoFromModelkit(spaceGroup);
    return symmetryInfo.infoStr;
  }

  @Override
  public int getSpaceGroupOperationCount() {
    return (symmetryInfo != null && symmetryInfo.symmetryOperations != null ? // null here for PDB 
        symmetryInfo.symmetryOperations.length
        : spaceGroup != null ? (spaceGroup.finalOperations != null
            ? spaceGroup.finalOperations.length 
            : spaceGroup.operationCount) : 0);
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
  public int getAdditionalOperationsCount() {
    return (symmetryInfo != null && symmetryInfo.symmetryOperations != null
        && symmetryInfo.getAdditionalOperations() != null
            ? symmetryInfo.additionalOperations.length
            : spaceGroup != null && spaceGroup.finalOperations != null
                ? spaceGroup.getAdditionalOperationsCount()
                : 0);
  }

  @Override
  public M4d[] getAdditionalOperations() {
    if (symmetryInfo != null)
      return symmetryInfo.getAdditionalOperations();
    getSymmetryOperations();
    return spaceGroup.getAdditionalOperations();
  }

  @Override
  public boolean isSimple() {
    return (spaceGroup == null
        && (symmetryInfo == null || symmetryInfo.symmetryOperations == null));
  }

  // UnitCell methods

  @Override
  public boolean haveUnitCell() {
    return (unitCell != null);
  }

  @Override
  public SymmetryInterface setUnitCellFromParams(double[] unitCellParams,
                                                 boolean setRelative,
                                                 double slop) {
    if (unitCellParams == null)
      unitCellParams = new double[] { 1, 1, 1, 90, 90, 90 };
    unitCell = UnitCell.fromParams(unitCellParams, setRelative, slop);
    return this;
  }

  @Override
  public boolean unitCellEquals(SymmetryInterface uc2) {
    return ((Symmetry) (uc2)).unitCell.isSameAs(unitCell.getF2C());
  }

  @Override
  public boolean isSymmetryCell(SymmetryInterface sym) {
    UnitCell uc = ((Symmetry) (sym)).unitCell;
    double[][] myf2c = (!uc.isStandard() ? null 
        : (symmetryInfo != null ? symmetryInfo.spaceGroupF2C 
            : unitCell.getF2C()));
    boolean ret = uc.isSameAs(myf2c);
    if (symmetryInfo != null) {
      if (symmetryInfo.setIsCurrentCell(ret)) {
        setUnitCellFromParams(symmetryInfo.spaceGroupF2CParams, false,
            Double.NaN);
      }
    }
    return ret;
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

  @Override
  public void initializeOrientation(M3d mat) {
    unitCell.initOrientation(mat);
  }

  @Override
  public void unitize(T3d ptFrac) {
    unitCell.unitize(ptFrac);
  }

  @Override
  public void toUnitCell(T3d pt, T3d offset) {
    unitCell.toUnitCell(pt, offset);
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
  public void toCartesian(T3d pt, boolean ignoreOffset) {
    if (!isBio)
      unitCell.toCartesian(pt, ignoreOffset);
  }

  @Override
  public double[] getUnitCellParams() {
    return unitCell.getUnitCellParams();
  }

  @Override
  public double[] getUnitCellAsArray(boolean vectorsOnly) {
    return unitCell.getUnitCellAsArray(vectorsOnly);
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
  public P3d getFractionalOffset(boolean onlyIfFractional) {
    P3d offset = unitCell.getFractionalOffset();
    return (onlyIfFractional 
        && offset != null
        && offset.x == (int) offset.x
        && offset.y == (int) offset.y 
        && offset.z == (int) offset.z ? null : offset);
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

  /**
   * Note, this has no origin shift.
   */
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
  public P3d[] getCanonicalCopyTrimmed(P3d frac, double scale) {
    return unitCell.getCanonicalCopyTrimmed(frac, scale);
  }


  @Override
  public double getUnitCellInfoType(int infoType) {
    return unitCell.getInfo(infoType);
  }

  @Override
  public String getUnitCellInfo(boolean scaled) {
    return (unitCell == null ? null : unitCell.dumpInfo(false, scaled));
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
  public P3d[] getUnitCellVectors() {
    return unitCell.getUnitCellVectors();
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
      boolean isOneMolecule = (molecules[moleculeCount
          - 1].firstAtomIndex == modelSet.am[atoms[iAtom0].mi].firstAtomIndex);
      P3d center = new P3d();
      double packing = minmax[6] / 100d;
      boolean centroidPacked = (packing != 0);
      nextMol: for (int i = moleculeCount; --i >= 0
          && bsAtoms.get(molecules[i].firstAtomIndex);) {
        BS bs = molecules[i].atomList;
        center.set(0, 0, 0);
        int n = 0;
        for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1)) {
          if (isOneMolecule || centroidPacked) {
            center.setT(atoms[j]);
            if (isNotCentroid(center, 1, minmax, packing)) {
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
        if (centroidPacked || n > 0 && isNotCentroid(center, n, minmax, 0))
          bsDelete.or(bs);
      }
      return bsDelete;
    } catch (Exception e) {
      return null;
    }
  }

  private boolean isNotCentroid(P3d center, int n, int[] minmax,
                                double packing) {
    center.scale(1d / n);
    toFractional(center, false);
    // we have to disallow just a tiny slice of atoms due to rounding errors
    // so  -0.000001 is OK, but 0.999991 is not.
    if (packing != 0) {
      double d = (packing > 0 ? packing : 0.000005d);
      return (center.x + d <= minmax[0]
          || center.y + d <= minmax[1]
          || center.z + d <= minmax[2]
          || center.x - d > minmax[3]
          || center.y - d > minmax[4]
          || center.z - d > minmax[5]);
    }
    // I think this was a bug, but we are going to leave it. Why the two values?
    return (center.x + 0.000005d <= minmax[0] 
        || center.y + 0.000005d <= minmax[1] 
        || center.z + 0.000005d <= minmax[2]
        || center.x + 0.00005d > minmax[3]
        || center.y + 0.00005d > minmax[4]
        || center.z + 0.00005d > minmax[5]);
  }

  // info

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
                                    int nth, int options, int[] opList) {
    return getDesc(modelSet).getSymopInfo(iatom, xyz, op, translation, pt, pt2,
        id, type, scaleFactor, nth, options, opList);
  }

  @Override
  public Map<String, Object> getSpaceGroupInfo(ModelSet modelSet, String sgName,
                                               int modelIndex, boolean isFull,
                                               double[] cellParams) {
    boolean isForModel = (sgName == null);
    if (sgName == null) {
      if (modelIndex < 0)
        modelIndex = vwr.am.cmi;
      Map<String, Object> info = modelSet.getModelAuxiliaryInfo(modelIndex);
      if (info != null)
        sgName = (String) info.get(JC.INFO_SPACE_GROUP);
    }
    SymmetryInterface cellInfo = null;
    if (cellParams != null) {
      cellInfo = new Symmetry().setUnitCellFromParams(cellParams, false,
          Double.NaN);
    }
    return getDesc(modelSet).getSpaceGroupInfo(this, modelIndex, sgName, 0,
        null, null, null, 0, -1, isFull, isForModel, 0, cellInfo, null);
  }

  @Override
  public T3d[] getV0abc(Object def, M4d retMatrix) {
    return (def instanceof T3d[] ? (T3d[]) def
        : UnitCell.getMatrixAndUnitCell(unitCell, def, retMatrix));
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
    boolean isAssigned = (ms.getInfo(modelIndex, JC.INFO_SPACE_GROUP_ASSIGNED) != null);
    T3d pt = getFractionalOffset(false);
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
    String sg = (String) ms.getInfo(modelIndex, JC.INFO_SPACE_GROUP);
    if (isAssigned && sg != null) {
      int ipt = sg.indexOf("#");
      if (ipt >= 0)
        sg = sg.substring(ipt + 1);
      // first one may not be read, but it is important to have it
      // in case there is an issue with assigning the spacegroup
      String cmd = "\n UNITCELL "
          + Escape.e(ms.getUnitCell(modelIndex).getUnitCellVectors());
      commands.append(cmd);
      commands.append("\n MODELKIT SPACEGROUP " + PT.esc(sg));
      commands.append(cmd);
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
      unitCell = UnitCell.fromOABC(oabc, false);
    return unitCell.toFromPrimitive(toPrimitive, type, oabc,
        primitiveToCrystal);
  }

  @Override
  public Lst<P3d> generateCrystalClass(P3d pt00) {
    if (symmetryInfo == null || !symmetryInfo.isCurrentCell)
      return null;
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
      pt0 = P3d.newP(pt00);
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
      for (int j = lst.size(); --j >= 0;) {
        pt = lst.get(j);
        if (isRandom)
          pt.scale(0.5f);
        unitCell.toCartesian(pt, true); // ignoreOffset
      }
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

  @Override
  public Map<String, Object> getUnitCellInfoMap() {
    return (unitCell == null ? null : unitCell.getInfo());
  }

  @Override
  public void setUnitCell(SymmetryInterface uc) {
    unitCell = UnitCell.cloneUnitCell(((Symmetry) uc).unitCell);
  }

  @Override
  public Object findSpaceGroup(Viewer vwr, BS atoms, String xyzList,
                               double[] unitCellParams, T3d origin, T3d[] oabc, int flags) {
    return ((SpaceGroupFinder) Interface
        .getInterface("org.jmol.symmetry.SpaceGroupFinder", vwr, "eval"))
            .findSpaceGroup(vwr, atoms, xyzList, unitCellParams, origin, oabc, this, flags);
  }

  @Override
  public void setSpaceGroupName(String name) {
    symmetryInfo = null;
    if (spaceGroup != null)
      spaceGroup.setName(name);
  }

  @Override
  public void setSpaceGroupTo(Object sg) {
    symmetryInfo = null;
    if (sg instanceof SpaceGroup) {
      spaceGroup = (SpaceGroup) sg;
    } else {
      spaceGroup = SpaceGroup.getSpaceGroupFromJmolClegOrITA(vwr, sg.toString());
    }
  }

  @Override
  public BS removeDuplicates(ModelSet ms, BS bs, boolean highPrec) {
    UnitCell uc = this.unitCell;
    Atom[] atoms = ms.at;
    double[] occs = ms.occupancies;
    boolean haveOccupancies = (occs != null);
    P3d[] unitized = new P3d[bs.length()];
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      P3d pt = unitized[i] = P3d.newP(atoms[i]);
      uc.toFractional(pt, false);
      if (highPrec)
        uc.unitizeRnd(pt);
      else
        uc.unitize(pt);
    }
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      Atom a = atoms[i];
      P3d pt = unitized[i];
      int type = a.getAtomicAndIsotopeNumber();
      double occ = (haveOccupancies ? occs[i] : 0);
      for (int j = bs.nextSetBit(i + 1); j >= 0; j = bs.nextSetBit(j + 1)) {
        Atom b = atoms[j];
        if (type != b.getAtomicAndIsotopeNumber()
            || (haveOccupancies && occ != occs[j]))
          continue;
        P3d pt2 = unitized[j];
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
            pts == null ? new Lst<P3d>() : pts, 0, 0, 0, getPeriodicity()));
  }

  @Override
  public int getPeriodicity() {
    return (spaceGroup == null ? 0x7 : spaceGroup.periodicity);
  }

  @Override
  public int getDimensionality() {
    return (spaceGroup == null ? 3 : spaceGroup.nDim);
  }

  @Override
  public void getEquivPointList(Lst<P3d> pts, int nInitial, String flags, M4d[] opsCtr) {
    M4d[] ops = (opsCtr == null ? getSymmetryOperations() : opsCtr);
    boolean newPt = (flags.indexOf("newpt") >= 0);
    boolean zapped = (flags.indexOf("zapped") >= 0);
    // we will preserve the points temporarily, then remove them at the end
    int n = pts.size();
    boolean tofractional = (flags.indexOf("tofractional") >= 0);
    // fractionalize all points if necessary
    if (flags.indexOf("fromfractional") < 0) {
      for (int i = 0; i < pts.size(); i++) {
        toFractional(pts.get(i), false); // was true in SwingJS
      }
    }
    // signal to make no changes in points
    flags += ",fromfractional,tofractional";
    int check0 = (nInitial > 0 ? 0 : n);
    boolean allPoints = (nInitial == n);
    int n0 = (nInitial > 0 ? nInitial : n);
    if (allPoints) {
      nInitial--;
      n0--;
    }
    if (zapped)
      n0 = 0;
    P3d p0 = (nInitial > 0 ? pts.get(nInitial) : null);
    int dup0 = (opsCtr == null ? n0 : check0);
    if (ops != null || unitCell != null) {
      for (int i = nInitial; i < n; i++) {
        unitCell.getEquivPoints(pts.get(i), flags, ops, pts, check0, n0, dup0, getPeriodicity());
      }
    }
    // now remove the starting points, checking to see if perhaps our
    // test point itself has been removed.
    if (!zapped && (pts.size() == nInitial || pts.get(nInitial) != p0
        || allPoints || newPt))
      n--;
    for (int i = n - nInitial; --i >= 0;)
      pts.removeItemAt(nInitial);
    // final check for removing duplicates
    //    if (nIgnored > 0)
    //      UnitCell.checkDuplicate(pts, 0, nIgnored - 1, nIgnored);

    // and turn these to Cartesians if desired
    if (!tofractional) {
      for (int i = pts.size(); --i >= nInitial;)
        toCartesian(pts.get(i), false);
    }
  }

  @Override
  public int[] getInvariantSymops(P3d pt, int[] v0) {
    SymmetryOperation[] ops = getSymmetryOperations();
    if (ops == null)
      return new int[0];
    BS bs = new BS();
    P3d p = new P3d();
    P3d p0 = new P3d();
    int nops = ops.length;
    for (int i = 1; i < nops; i++) {
      p.setT(pt);
      unitCell.toFractional(p, false);
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

  @Override
  public Object getWyckoffPosition(Viewer vwr, P3d p, String letter) {
    if (unitCell == null)
      return "";
    SpaceGroup sg = spaceGroup;
    if (sg == null && symmetryInfo != null) {
      sg = SpaceGroup.determineSpaceGroupN(symmetryInfo.sgName);
      if (sg == null) {
        String id = getSpaceGroupJmolId(); 
        if (id == null)
          id = getSpaceGroupClegId();
        sg = SpaceGroup.getSpaceGroupFromJmolClegOrITA(vwr, id);
      }
    }
    if (sg == null || sg.itaNumber == null) {
      // maybe an unusual setting
      return "?";
    }
    if (p == null) {
      // attempt to make these not very close to any special position
      // this point tested in every standard setting and found to be excellent
      p = P3d.new3(0.53d, 0.20d, 0.16d);
    } else if (!"L".equals(letter)){
      p = P3d.newP(p);
      unitCell.toFractional(p, false);
      unitCell.unitize(p);
    }
    
    try {
      WyckoffFinder w = getWyckoffFinder().getWyckoffFinder(vwr,
          sg);
      boolean withMult = (letter != null && letter.charAt(0) == 'M');
      if (withMult) {
        letter = (letter.length() == 1 ? null : letter.substring(1));
      }
      int mode = (letter == null ? WyckoffFinder.WYCKOFF_RET_LABEL
          : "L".equals(letter) ? WyckoffFinder.WYCKOFF_RET_ALL_ARRAY 
          : letter.equalsIgnoreCase("coord") ? WyckoffFinder.WYCKOFF_RET_COORD
              : letter.equalsIgnoreCase("coords")
                  ? WyckoffFinder.WYCKOFF_RET_COORDS
                  : letter.endsWith("*") ? (int) letter.charAt(0) : 0);
      if (mode != 0) {
        return (w == null ? "?" : w.getInfo(unitCell, p, mode, withMult, vwr.is2D()));
      }
      if (w.findPositionFor(p, letter) == null)
        return null;
      unitCell.toCartesian(p, false);
      return p;
    } catch (Exception e) {
      e.printStackTrace();
      return (letter == null ? "?" : null);
    }
  }

  private WyckoffFinder getWyckoffFinder() {
    if (wyckoffFinder == null) {
      wyckoffFinder = (WyckoffFinder) Interface
          .getInterface("org.jmol.symmetry.WyckoffFinder", null, "symmetry");
    }
    return wyckoffFinder;
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

  @Override
  public boolean isWithinUnitCell(P3d pt, double x, double y, double z) {
    return unitCell.isWithinUnitCell(x, y, z, pt);
  }

  @Override
  public boolean checkPeriodic(P3d pt) {
    return unitCell.checkPeriodic(pt);
  }

  @Override
  public Object staticConvertOperation(String xyz, M4d matrix) {
    return (matrix == null ? SymmetryOperation.stringToMatrix(xyz) : SymmetryOperation.getXYZFromMatrixFrac(matrix, false, false, false, true));
  }


  /**
   * Retrieve subgroup information for a space group. Returns:
   * 
   * <pre>
   * 
   * values are 1-based so that "0" has special meaning, "-" means ignored; "MnV" is Integer.MIN_VALUE
   * 
   * itaFrom  itaTo  index1  index2
   *    n      MnV     -       -      return map for group n, contents of sub_n.json
   *    n       0      0       0      return int[][] of critical information 
   *    n       0      m      MnV     return map map.subgroups[m]
   *    n       0     MnV      -      return int[] array of list of valid super>>sub 
   *    n       0      m       t      return string transform map.subgroups[m].trm[t]
   *    n1      n2    MnV      -      return list map.subgroups.select("WHERE subgroup=n2")
   *    n1      n2     m      MnV     return map map.subgroups.select("WHERE subgroup=n2")[m]
   *    n1      n2     m       t      return string transform map.subgroups.select("WHERE subgroup=n2")[m].trm[t]
   * 
   * </pre>
   * Critical information array is:
   * 
   *  [ isub, ntrm, subIndex, idet, trType ]
   *  
   *  isub: subgroupNumber
   *  ntrm: transformation count
   *  subIndex: index of this group-subgroup relationship
   *  idet:  determinant if determinant >= 1; -1/determinant if determinant < 1
   *  trType: 1 translationengeliechen, 3 klassengleichen "ct" loss of centering translation, 4 klassengleichen "eu" enlarged unit cell
   *
   * 
   * @param vwr
   * @param itaFrom
   *        group ITA number
   * @param itaTo
   *        subgroup ITA number
   * @param index1
   *        for a specific index or Integer.MIN_VALUE for all itaFrom; itaTo
   *        ignored
   * @param index2
   *        Integer.MIN_VALUE for all, or an index for a specific transform
   * @return Map, List, or String with conjugation class removed (first two
   *         characters "a:......")
   */
  @SuppressWarnings("unchecked")
  @Override
  public Object getSubgroupJSON(Viewer vwr, int itaFrom, int itaTo, int index1,
                                int index2) {
    if (vwr == null)
      vwr = this.vwr;
    boolean allSubsMap = (itaTo < 0);
    boolean asIntArray = (itaTo == 0 && index1 == 0);
    boolean asSSIntArray = (itaTo == 0 && index1 < 0);
    boolean isIndexMap = (itaTo == 0 && index1 > 0 && index2 < 0);
    boolean isIndexTStr = (itaTo == 0 && index1 > 0 && index2 > 0);
    boolean isWhereList = (itaTo > 0 && index1 < 0);
    boolean isWhereMap = (itaTo > 0 && index1 > 0 && index2 < 0);
    boolean isWhereTStr = (itaTo > 0 && index1 > 0 && index2 > 0);
    try {
      Map<String, Object> o = (Map<String, Object>) getSpaceGroupJSON(vwr,
          "subgroups", "map", itaFrom);
      int ithis = 0;
      if (o != null) {
        if (allSubsMap)
          return o;
        if (asIntArray || asSSIntArray) {
          Lst<Object> list = (Lst<Object>) o.get("subgroups");
          int n = list.size();
          int[][] groups = (asIntArray ? new int[n][] : null);
          BS bs = (asSSIntArray ? new BS() : null);
          for (int i = n; --i >= 0;) {
            o = (Map<String, Object>) list.get(i);
            int isub = ((Integer) o.get("subgroup")).intValue();
            if (asSSIntArray) {
              bs.set(isub);
              continue;
            }            
            int subIndex = ((Integer) o.get("subgroupIndex")).intValue();
            int trType = "k".equals(o.get("trType")) ? 2 : 1;
            String subType = (trType == 1 ? (String) o.get("trSubtype") : "");
            double det = ((Number) o.get("det")).doubleValue();
            int idet = (int)(det < 1 ? -1/det : det);
            if (subType.equals("ct"))
              trType = 3;
            else if (subType.equals("eu"))
              trType = 4;
            int ntrm = ((Lst<Object>) o.get("trm")).size();
            groups[i] = new int[] { isub, ntrm, subIndex, idet, trType };
          }
          if (asSSIntArray) {
            int[] a = new int[bs.cardinality()];
            for (int p = 0, i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
              a[p++] = i;
            }
            return a;
          }
          return groups;
          
        }
        Lst<Object> list = (Lst<Object>) o.get("subgroups");
        int i0 = 0;
        int n = list.size();
        if (isIndexMap || isIndexTStr) {
          if (index1 > n) {
            throw new ArrayIndexOutOfBoundsException(
                "no map.subgroups[" + index1 + "]!");
          }
          i0 = index1 - 1;
          if (isIndexMap)
            return list.get(i0);
          n = index1;
        }
        Lst<Map<String, Object>> whereList = (isWhereList ? new Lst<>() : null);
        for (int i = i0; i < n; i++) {
          o = (Map<String, Object>) list.get(i);
          int isub = ((Integer) o.get("subgroup")).intValue();
          if (!isIndexTStr && isub != itaTo)
            continue;
          if (++ithis == index1) {
            if (isWhereMap)
              return o;
          } else if (isWhereTStr) {
            continue;
          }
          if (isWhereList) {
            whereList.addLast(o);
            continue;
          }
          Lst<Object> trms = (Lst<Object>) o.get("trms");
          n = trms.size();
          if (index2 < 1 || index2 > n)
            return null;
          return ((Map<String, Object>)trms.get(index2 - 1)).get("trm");
        }
        if (isWhereList && !whereList.isEmpty()) {
          return whereList;
        }
      }
      if (index1 == 0)
        return null;
      if (isWhereTStr && ithis > 0) {
        throw new ArrayIndexOutOfBoundsException(
            "only " + ithis +" maximal subgroup information for " + itaFrom + ">>" + itaTo + "!");
      }

      throw new ArrayIndexOutOfBoundsException(
          "no maximal subgroup information for " + itaFrom + ">>" + itaTo + "!");
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object getSpaceGroupJSON(Viewer vwr, String name, String data,
                                  int index) {
    boolean isSetting = name.equals("setting");
    boolean isSettings = name.equals("settings");
    boolean isAFLOW = name.equalsIgnoreCase("AFLOW");
    boolean isSubgroups = !isSettings && name.equals("subgroups");
    boolean isThis = ((isSetting || isSettings || isSubgroups)
        && index == Integer.MIN_VALUE);
    String s0 = (!isSettings && !isSetting && !isSubgroups? name
        : isThis ? getSpaceGroupName() : "" + index);
    try {
      int itno;
      int specialType = (data == null ? SpaceGroup.TYPE_SPACE : SpaceGroup.getExplicitSpecialGroupType(data));
      if (specialType > SpaceGroup.TYPE_SPACE)
        data = data.substring(2); 
      String tm = null;
      boolean isTM, isInt;
      String sgname;
      if (isSetting && data == null || isSettings || isSubgroups) {
        isTM = false;
        isInt = true;
        sgname = (isSetting ? data : null);
        if (isThis) {
          // special TODO
          itno = PT.parseInt(getIntTableNumber());
          if (isSetting || isSettings) {
            if (spaceGroup == null) {
              SpaceGroup sg = symmetryInfo.getDerivedSpaceGroup();
              if (sg == null)
                return new Hashtable<String, Object>();
              sgname = sg.jmolId;
            } else {
              sgname = getSpaceGroupClegId();
              if (isSetting) {
                tm = sgname.substring(sgname.indexOf(":") + 1);
              }  else if (isSettings) {
                index = 0;
              }
            }
          }
        } else {
          itno = index;
        }
      } else {
        if (!isAFLOW)
          index = 0;
        sgname = data;
        // tm allow for both 4(a,b,...) and 4:a,b,..., or, technically, 4(a,b,....
        int pt = sgname.indexOf("(");
        if (pt < 0)
          pt = sgname.indexOf(":");
        isTM = (pt >= 0 && sgname.indexOf(",") > pt);
        if (isTM) {
          tm = sgname.substring(pt + 1,
              sgname.length() - (sgname.endsWith(")") ? 1 : 0));
          sgname = sgname.substring(0, pt);
          isThis = true;
        }
        itno = (sgname.equalsIgnoreCase("ALL") ? 0 : PT.parseInt(sgname));
        isInt = (itno != Integer.MIN_VALUE);
        pt = sgname.indexOf('.');
        if (!isTM && isInt && index == 0 && pt > 0) {
          index = PT.parseInt(sgname.substring(pt + 1));
          sgname = sgname.substring(0, pt);
        }
      }
      
      
      if (isInt && (itno > SpaceGroup.getMax(specialType) || (isSettings || isSetting ? itno < 1 : itno < 0)))
        throw new ArrayIndexOutOfBoundsException(itno);
      if (isSubgroups) {
        // special TODO
        if (itaSubData == null)
          itaSubData = new Map[230];
        Map<String, Object> resource = itaSubData[itno - 1];
        if (resource == null)
          itaSubData[itno - 1] = resource = (Map<String, Object>) getResource(
              vwr, "sg/json/sub_" + itno + ".json");
        if (resource != null) {
          return resource;
        }
      } else if (isSetting || isSettings || name.equalsIgnoreCase("ITA")) {
        
        if (itno == 0) {
          return getAllITAData(vwr, specialType, true);
        }
        Map<String, Object> resource = getITResource(vwr, specialType, itno, data);
        if (resource != null) {
          if (index == 0 && tm == null)
            return (isSettings ? resource.get("its") : resource);
          Lst<Object> its = (Lst<Object>) resource.get("its");
          if (its != null) {
            if (isSettings && !isThis) {
              return its;
            }
            int n = its.size();
            int i0 = (isSetting ? Math.max(index, 1) : isInt && !isThis ? index : n);
            if (i0 > n)
              return null;
            if (isSetting)
              return its.get(i0 - 1);
            Map<String, Object> map = null;
            for (int i = i0; --i >= 0;) {
              map = (Map<String, Object>) its.get(i);
              if (i == index - 1 || 
                  (tm == null ? sgname.equals(map.get("jmolId")) : tm.equals(map.get("trm")))) {
                if (!map.containsKey("more")) {
                  return map;
                }
                break;
              } 
              map = null;
            }
            if (map != null) {
              // "more" was found -- this is a minimal Wyckoff-only setting
              return SpaceGroup.fillMoreData(vwr, map, data, itno, (Map<String, Object>) its.get(0));
            }
            // TODO: create entries for unregistered settings?
          }
        }
      } else if (isAFLOW && tm == null) {
        if (aflowStructures == null)
          aflowStructures = (Map<String, Object>) getResource(vwr,
              "sg/json/aflow_structures.json");
        if (itno == 0)
          return aflowStructures;
        if (itno == Integer.MIN_VALUE) {
          Lst<String> start = null;
          if (sgname.endsWith("*")) {
            start = new Lst<>();
            sgname = sgname.substring(0, sgname.length() - 1);
          }
          for (int j = 1; j <= 230; j++) {
            Lst<Object> list = (Lst<Object>) aflowStructures.get("" + j);
            for (int i = 0, n = list.size(); i < n; i++) {
              String id = (String) list.get(i);
              if (start != null && id.startsWith(sgname)) {
                start.addLast("=aflowlib/" + j + "." + (i + 1) + "\t" + id);
              } else if (id.equalsIgnoreCase(sgname)) {
                return j + "." + (i + 1);
              }
            }
          }
          return (start != null && start.size() > 0 ? start : null);
        }
        Lst<Object> adata = (Lst<Object>) aflowStructures.get("" + sgname);
        if (index <= adata.size()) {
          return (index == 0 ? adata : adata.get(index - 1));
        }
      }
      if (isThis)
        return new Hashtable<String, Object>();
      throw new IllegalArgumentException(s0);
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  @SuppressWarnings("unchecked")
  static Map<String, Object> getITResource(Viewer vwr, int type, int itno,
                                           String hmName) {
    if (type == SpaceGroup.TYPE_SPACE) {
      if (itaData == null)
        itaData = new Map[230];
      Map<String, Object> resource = itaData[itno - 1];
      if (resource == null)
        itaData[itno - 1] = resource = (Map<String, Object>) getResource(vwr,
            "sg/json/ita_" + itno + ".json");
      return resource;
    }
    Map<String, Object>[] data = (Map<String, Object>[]) getAllITAData(vwr,
        type, false);
    if (itno > 0)
      return data[itno - 1];
    // match HM name
    for (int i = 0, n = data.length; i < n; i++) {
      Lst<Object> list = (Lst<Object>) data[i].get("its");
      for (int j = list.size(); --j >= 0;) {
        Map<String, Object> map = (Map<String, Object>) list.get(j);
        String hm = (String) map.get("hm");
        if (SpaceGroup.hmEquals(hm, hmName, type))
          return map;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  static Object getAllITAData(Viewer vwr, int type, boolean isAll) {
    switch (type) {
    case SpaceGroup.TYPE_SPACE:
      if (allDataITA == null)
        allDataITA = (Lst<Object>) getResource(vwr, "sg/json/ita_all.json");
      return allDataITA;
    default:
      String name = "sg/json/it" + (type == SpaceGroup.TYPE_PLANE ? "a" : "e")
          + "_all_" + SpaceGroup.getSpecialGroupName(type) + ".json";
      switch (type) {
      case SpaceGroup.TYPE_PLANE:
        if (allPlaneData == null) {
          allPlaneData = (Lst<Object>) getResource(vwr, name);
          planeData = createSpecialData(type, allPlaneData);
        }
        return (isAll ? allPlaneData : planeData);
      case SpaceGroup.TYPE_LAYER:
        if (allLayerData == null) {
          allLayerData = (Lst<Object>) getResource(vwr, name);
          layerData = createSpecialData(type, allLayerData);
        }
        return (isAll ? allLayerData : layerData);
      case SpaceGroup.TYPE_ROD:
        if (allRodData == null) {
          allRodData = (Lst<Object>) getResource(vwr, name);
          rodData = createSpecialData(type, allRodData);
        }
        return (isAll ? allRodData : rodData);
      case SpaceGroup.TYPE_FRIEZE:
        if (allFriezeData == null) {
          allFriezeData = (Lst<Object>) getResource(vwr, name);
          friezeData = createSpecialData(type, allFriezeData);
        }
        return (isAll ? allFriezeData : friezeData);
      }
    }
    return null;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private static Map<String, Object>[] createSpecialData(int type, Lst<Object> data) {
    int n = SpaceGroup.getMax(type);
    Map<String, Object>[] list = new Map[n];
    for (int i = 0; i < n; i++) {
      list[i] = new Hashtable<String, Object>();
      list[i].put("sg", Integer.valueOf(i + 1));
      list[i].put("its", new Lst<Map<String, Object>>());
    }
    for (int i = 0, nd = data.size(); i < nd; i++) {
      Map<String, Object> map = (Map<String, Object>) data.get(i);
      int sg = ((Integer) map.get("sg")).intValue();
      ((Lst<Map<String, Object>>)list[sg - 1].get("its")).addLast(map);
    }
    for (int i = 0; i < n; i++) {
      list[i].put("n", Integer.valueOf(((Lst) list[i].get("its")).size()));
    }
    return list;    
  }

  private static Object getResource(Viewer vwr, String resource) {
    try {
      BufferedReader r = FileManager.getBufferedReaderForResource(vwr, Symmetry.class,
          "org/jmol/symmetry/", resource);
      String[] data = new String[1];
      if (Rdr.readAllAsString(r, Integer.MAX_VALUE, false, data, 0)) {
        return new JSJSONParser().parse(data[0], true);
      }
    } catch (Throwable e) {
      System.err.println(e.getMessage());
    }
    return null;
  }

  @Override
  public double getCellWeight(P3d pt) {
    return unitCell.getCellWeight(pt);
  }

  @Override
  public double getPrecision() {
    return (unitCell == null ? Double.NaN : unitCell.getPrecision());
  }

  @Override
  public boolean fixUnitCell(double[] params) {
    return spaceGroup.createCompatibleUnitCell(params, null, true);
  }


  @Override
  public String staticGetTransformABC(Object transform, boolean normalize) {
    return SymmetryOperation.getTransformABC(transform, normalize);
  }
  

  /**
   * Called from SpaceGroupFinder only.
   * 
   * @param origin
   */
  void setCartesianOffset(T3d origin) {
    unitCell.setCartesianOffset(origin);
  }

  /**
   * Set space group and unit cell from the auxiliary info generated by
   * XtalSymmetry specific to a given model.
   * 
   * Only called by ModelLoader.
   * 
   * @param ms
   * @param modelIndex
   * @param unitCellParams
   * 
   */
  @SuppressWarnings("unchecked")
  public void setSymmetryInfoFromFile(ModelSet ms, int modelIndex,
                                      double[] unitCellParams) {
    Map<String, Object> modelAuxiliaryInfo = ms
        .getModelAuxiliaryInfo(modelIndex);
    symmetryInfo = new SymmetryInfo();
    double[] params = symmetryInfo.setSymmetryInfoFromFile(modelAuxiliaryInfo,
        unitCellParams);
    if (params != null) {
      setUnitCellFromParams(params, modelAuxiliaryInfo.containsKey("jmolData"),
          Double.NaN);
      unitCell.moreInfo = (Lst<String>) modelAuxiliaryInfo
          .get("moreUnitCellInfo");
      modelAuxiliaryInfo.put("infoUnitCell", getUnitCellAsArray(false));
      setOffsetPt((T3d) modelAuxiliaryInfo.get(JC.INFO_UNIT_CELL_OFFSET));
      M3d matUnitCellOrientation = (M3d) modelAuxiliaryInfo
          .get("matUnitCellOrientation");
      if (matUnitCellOrientation != null)
        initializeOrientation(matUnitCellOrientation);
      String s = symmetryInfo.strSUPERCELL;
      if (s != null) {
        T3d[] oabc = unitCell.getUnitCellVectors();
        oabc[0] = new P3d();
        ms.setModelCagePts(modelIndex, oabc, "conventional");
      }
      if (Logger.debugging)
        Logger.debug("symmetryInfos[" + modelIndex + "]:\n"
            + unitCell.dumpInfo(true, true));
    }
  }

  public void transformUnitCell(M4d trm) {
    if (trm == null) {
      trm = UnitCell.toTrm(spaceGroup.itaTransform, null);
    }
    M4d trmInv = M4d.newM4(trm);
    trmInv.invert();
    P3d[] oabc = getUnitCellVectors();
    for (int i = 1; i <= 3; i++) {
      toFractional(oabc[i], true);
      trmInv.rotate(oabc[i]);
      toCartesian(oabc[i], true);
    }
    P3d o = new P3d();
    trm.getTranslation(o);
    toCartesian(o, true);
    oabc[0].add(o);
    unitCell = UnitCell.fromOABC(oabc, false);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object getITASettingValue(Viewer vwr, String itaIndex, String key) {
      Object o = getSpaceGroupJSON(vwr, "ITA", itaIndex, 0);
      return (o instanceof Map ? ( (Map<String, Object>)o).get(key) : o);
  }

  @Override
  public String staticCleanTransform(String tr) {
    return SymmetryOperation.getTransformABC(UnitCell.toTrm(tr, null), true);
  }
  
  @Override
  public M4d replaceTransformMatrix(M4d trm) {
      M4d trm0 = transformMatrix;
      transformMatrix = trm;
      return trm0;
  }

  @Override
  public String getUnitCellDisplayName() {
    String name = (spaceGroup != null ? spaceGroup.getDisplayName()
        : symmetryInfo != null ? symmetryInfo.getDisplayName(this) : null);
    return (name.length() > 0 ? name : null);
  }

  @Override
  public String staticToRationalXYZ(P3d fPt, String sep) {
    String s = SymmetryOperation.fcoord(fPt, sep);
    return (",".equals(sep) ? s : "("+ s + ")");
  }

  @Override
  public int getFinalOperationCount() {
    setFinalOperations(3, null, null, -1, -1, false, null);
    return spaceGroup.getOperationCount();
  }

  @Override
  public Object convertTransform(String transform, M4d trm) {
    if (transform == null) {
      return staticGetTransformABC(trm, false);
    }
    if (transform.equals("xyz")) {
      return (trm == null ? null : SymmetryOperation.getXYZFromMatrix(trm, false, false, false));
    }
    if (trm == null)
      trm = new M4d();
    UnitCell.getMatrixAndUnitCell(null, transform, trm);
    return trm;
  }

  @Override
  public M4d staticGetMatrixTransform(String cleg) {
    return getCLEGInstance().getMatrixTransform(vwr, cleg);
  }


  @Override
  public String staticTransformSpaceGroup(BS bs, String cleg, Object paramsOrUC,
                                          SB sb) {
    return getCLEGInstance().transformSpaceGroup(vwr, bs, cleg, paramsOrUC, sb);
  }


  private CLEG getCLEGInstance() {
    if (clegInstance == null) {
      clegInstance = (CLEG) Interface
          .getInterface("org.jmol.symmetry.CLEG", null, "symmetry");
    }
    return clegInstance;
  }
  
  private Viewer vwr = null;
  
  /**
   * for the vwr.getSymTemp() only
   * 
   * @param vwr
   */
  @Override
  public SymmetryInterface setViewer(Viewer vwr) {
    this.vwr = vwr;
    return this;
  }

  @Override
  public P3d getUnitCellCenter() {
    return unitCell.getCenter(getPeriodicity());
  }

  private static SpecialGroupFactory groupFactory;
  
  /**
   * Called from SpaceGroup to get a special group
   * 
   * @return singleton SpecialGroupFactory instance
   */
  static SpecialGroupFactory getSGFactory() {
    if (groupFactory == null) {
      groupFactory = (SpecialGroupFactory) Interface
          .getInterface("org.jmol.symmetry.SpecialGroupFactory", null, "symmetry");
    }
    return groupFactory;
  }
  
}
