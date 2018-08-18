/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-01-10 09:19:33 -0600 (Fri, 10 Jan 2014) $
 * $Revision: 19162 $
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.adapter.smarter;

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.P3;
import javajs.util.P3i;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.T3;
import javajs.util.V3;

import org.jmol.api.JmolModulationSet;
import org.jmol.api.SymmetryInterface;
import javajs.util.BS;
import org.jmol.symmetry.Symmetry;
import org.jmol.symmetry.SymmetryOperation;
import org.jmol.util.BSUtil;
import org.jmol.util.SimpleUnitCell;
import org.jmol.util.Tensor;
import org.jmol.util.Vibration;

/**
 * 
 * A class used by AtomSetCollection for building the symmetry of a model and
 * generating new atoms based on that symmetry.
 * 
 */
public class XtalSymmetry {

  private AtomSetCollection asc;

  private AtomSetCollectionReader acr;

  public XtalSymmetry() {
    // for reflection
  }

  public XtalSymmetry set(AtomSetCollectionReader reader) {
    this.acr = reader;
    this.asc = reader.asc;
    getSymmetry();
    return this;
  }

  public SymmetryInterface symmetry;

  SymmetryInterface getSymmetry() {
    return (symmetry == null ?(symmetry = (Symmetry) acr.getInterface("org.jmol.symmetry.Symmetry")) : symmetry);
  }

  SymmetryInterface setSymmetry(SymmetryInterface symmetry) {
    return (this.symmetry = symmetry);
  }

  private float[] unitCellParams = new float[6];
  private float[] baseUnitCell;
  // expands to 26 for cartesianToFractional matrix as array (PDB) and supercell

  private float symmetryRange;
  private boolean doCentroidUnitCell;
  private boolean centroidPacked;
  private float packingError;
  private String filterSymop;

  private void setSymmetryRange(float factor) {
    symmetryRange = factor;
    asc.setInfo("symmetryRange", Float.valueOf(factor));
  }

  private boolean applySymmetryToBonds = false;

  private int[] latticeCells;

  private void setLatticeCells() {

    //    int[] latticeCells, boolean applySymmetryToBonds,
    //  }
    //                       boolean doPackUnitCell, boolean doCentroidUnitCell,
    //                       boolean centroidPacked, String strSupercell,
    //                       P3 ptSupercell) {
    //set when unit cell is determined
    // x <= 555 and y >= 555 indicate a range of cells to load
    // AROUND the central cell 555 and that
    // we should normalize (z = 1) or pack unit cells (z = -1) or not (z = 0)
    // in addition (Jmol 11.7.36) z = -2 does a full 3x3x3 around the designated cells
    // but then only delivers the atoms that are within the designated cells. 
    // Normalization is the moving of the center of mass into the unit cell.
    // Starting with Jmol 12.0.RC23 we do not normalize a CIF file that 
    // is being loaded without {i j k} indicated.

    latticeCells = acr.latticeCells;
    boolean isLatticeRange = (latticeCells[0] <= 555 && latticeCells[1] >= 555 && (latticeCells[2] == 0
        || latticeCells[2] == 1 || latticeCells[2] == -1));
    doNormalize = latticeCells[0] != 0
        && (!isLatticeRange || latticeCells[2] == 1);
    applySymmetryToBonds = acr.applySymmetryToBonds;
    doPackUnitCell = acr.doPackUnitCell;
    doCentroidUnitCell = acr.doCentroidUnitCell;
    centroidPacked = acr.centroidPacked;
    filterSymop = acr.filterSymop;
    //if (acr.strSupercell == null)
      //setSupercellFromPoint(acr.ptSupercell);
  }

  private Lst<float[]> trajectoryUnitCells;
  
  private void setUnitCell(float[] info, M3 matUnitCellOrientation,
                                   P3 unitCellOffset) {
    unitCellParams = new float[info.length];
    //this.unitCellOffset = unitCellOffset;
    for (int i = 0; i < info.length; i++)
      unitCellParams[i] = info[i];
    asc.haveUnitCell = true;
    asc.setCurrentModelInfo("unitCellParams", unitCellParams);
    if (asc.isTrajectory) {
      if (trajectoryUnitCells == null) {
        trajectoryUnitCells = new Lst<float[]>();
        asc.setInfo("unitCells", trajectoryUnitCells);
      }
      trajectoryUnitCells.addLast(unitCellParams);
    }
    asc.setGlobalBoolean(AtomSetCollection.GLOBAL_UNITCELLS);
    getSymmetry().setUnitCell(unitCellParams, false);
    // we need to set the auxiliary info as well, because 
    // ModelLoader creates a new symmetry object.
    if (unitCellOffset != null) {
      symmetry.setOffsetPt(unitCellOffset);
      asc.setCurrentModelInfo("unitCellOffset", unitCellOffset);
    }
    if (matUnitCellOrientation != null) {
      symmetry.initializeOrientation(matUnitCellOrientation);
      asc.setCurrentModelInfo("matUnitCellOrientation",
          matUnitCellOrientation);
    }
  }

  int addSpaceGroupOperation(String xyz, boolean andSetLattice) {
    if (andSetLattice)
      setLatticeCells();
    symmetry.setSpaceGroup(doNormalize);
    return symmetry.addSpaceGroupOperation(xyz, 0);
  }

  public void setLatticeParameter(int latt) {
    symmetry.setSpaceGroup(doNormalize);
    symmetry.setLattice(latt);
  }

  private boolean doNormalize = true;
  private boolean doPackUnitCell = false;

  private SymmetryInterface baseSymmetry;

  private SymmetryInterface sym2;

  SymmetryInterface applySymmetryFromReader(SymmetryInterface readerSymmetry)
      throws Exception {
    asc.setCoordinatesAreFractional(acr.iHaveFractionalCoordinates);
    setUnitCell(acr.unitCellParams, acr.matUnitCellOrientation,
        acr.unitCellOffset);
    setAtomSetSpaceGroupName(acr.sgName);
    setSymmetryRange(acr.symmetryRange);
    if (acr.doConvertToFractional || acr.fileCoordinatesAreFractional) {
      setLatticeCells();
      boolean doApplySymmetry = true;
      if (acr.ignoreFileSpaceGroupName || !acr.iHaveSymmetryOperators) {
        if (!acr.merging || readerSymmetry == null)
          readerSymmetry = acr.getNewSymmetry();
        doApplySymmetry = readerSymmetry.createSpaceGroup(
            acr.desiredSpaceGroupIndex, (acr.sgName.indexOf("!") >= 0 ? "P1"
                : acr.sgName), acr.unitCellParams, acr.modDim);
      } else {
        acr.doPreSymmetry();
        readerSymmetry = null;
      }
      packingError = acr.packingError;
      if (doApplySymmetry) {
        if (readerSymmetry != null)
          setSpaceGroupFrom(readerSymmetry);
        //parameters are counts of unit cells as [a b c]
        
        applySymmetryLattice();
        if (readerSymmetry != null && filterSymop == null)
          setAtomSetSpaceGroupName(readerSymmetry.getSpaceGroupName());
      }
    }
    if (acr.iHaveFractionalCoordinates && acr.merging && readerSymmetry != null) {
      
      // when merging (with appendNew false), we must return cartesians
      Atom[] atoms = asc.atoms;
      for (int i = asc.getLastAtomSetAtomIndex(), n = asc.ac; i < n; i++)
          readerSymmetry.toCartesian(atoms[i], true);
      asc.setCoordinatesAreFractional(false);
      
      // We no longer allow merging of multiple-model files
      // when the file to be appended has fractional coordinates and vibrations
      acr.addVibrations = false;
    }
    return symmetry;
  }

  public void setSpaceGroupFrom(SymmetryInterface readerSymmetry) {
    getSymmetry().setSpaceGroupFrom(readerSymmetry);
  }

  private void setAtomSetSpaceGroupName(String spaceGroupName) {
    symmetry.setSpaceGroupName(spaceGroupName);
    asc.setCurrentModelInfo("spaceGroup", spaceGroupName + "");
  }

  private void applySymmetryLattice() throws Exception {
    if (!asc.coordinatesAreFractional || symmetry.getSpaceGroup() == null)
      return;
    sym2 = null;
    int maxX = latticeCells[0];
    int maxY = latticeCells[1];
    int maxZ = Math.abs(latticeCells[2]);
    int kcode = latticeCells[3];
    int dim = (int) symmetry.getUnitCellInfoType(SimpleUnitCell.INFO_DIMENSIONS);
    firstAtom = asc.getLastAtomSetAtomIndex();
    BS bsAtoms = asc.bsAtoms;
    if (bsAtoms != null) {
      updateBSAtoms();
      firstAtom = bsAtoms.nextSetBit(firstAtom);
    }
    rminx = rminy = rminz = Float.MAX_VALUE;
    rmaxx = rmaxy = rmaxz = -Float.MAX_VALUE;
    P3 pt0 = null;
    if (acr.latticeType == null)
      acr.latticeType = symmetry.getLatticeType();
    if (acr.isPrimitive) {
      asc.setCurrentModelInfo("isprimitive", Boolean.TRUE);
      if (!"P".equals(acr.latticeType)) {
        asc.setCurrentModelInfo("unitcell_conventional", symmetry.getConventionalUnitCell(acr.latticeType));
      }
    }
    if (acr.latticeType != null)
      asc.setCurrentModelInfo("latticeType", acr.latticeType);


    if (acr.fillRange instanceof String && acr.latticeType != null) {
      
     String type = (String) acr.fillRange; // conventional or primitive
     if (type.equals("conventional")) {
       acr.fillRange = symmetry.getConventionalUnitCell(acr.latticeType);
     } else if (type.equals("primitive")) {
       acr.fillRange = symmetry.getUnitCellVectors();
       symmetry.toFromPrimitive(true, acr.latticeType.charAt(0), (T3[]) acr.fillRange);
     } else {
       acr.fillRange = null;
     }
     if (acr.fillRange != null)
       acr.addJmolScript("unitcell " + type);
    }
    if (acr.fillRange != null) {

      bsAtoms = updateBSAtoms();
      acr.forcePacked = true;
      doPackUnitCell = false;
      minXYZ = new P3i();
      maxXYZ = P3i.new3(1, 1, 1);
      P3[] oabc = new P3[4];
      for (int i = 0; i < 4; i++)
        oabc[i] = P3.newP(((T3[]) acr.fillRange)[i]);
      adjustRangeMinMax(oabc);
      //Logger.info("setting min/max for original lattice to " + minXYZ + " and "
        //  + maxXYZ);
      if (sym2 == null) {
        sym2 = new Symmetry();
        sym2.getUnitCell((T3[]) acr.fillRange, false, null);
      }
      applyAllSymmetry(acr.ms, bsAtoms);
      pt0 = new P3();
      Atom[] atoms = asc.atoms;
      for (int i = asc.ac; --i >= firstAtom; ) {
        pt0.setT(atoms[i]);
        symmetry.toCartesian(pt0, false);
        sym2.toFractional(pt0, false);
        if (acr.fixJavaFloat)
          PT.fixPtFloats(pt0, PT.FRACTIONAL_PRECISION);
        if (!isWithinCell(dtype, pt0, 0, 1, 0, 1, 0, 1, packingError))
          bsAtoms.clear(i);
          
      }
      return;
    }
    P3 offset = null;
    nVib = 0;
    T3 va = null, vb = null, vc = null;
    baseSymmetry = symmetry;
    String supercell = acr.strSupercell;
    T3[] oabc = null;
    boolean isSuper = (supercell != null && supercell.indexOf(",") >= 0);
    if (isSuper) {
      // expand range to accommodate this alternative cell
      // oabc will be cartesian
      oabc = symmetry.getV0abc(supercell);
      if (oabc != null) {
        // set the bounds for atoms in the new unit cell
        // in terms of the old unit cell
        minXYZ = new P3i();
        maxXYZ = P3i.new3(maxX, maxY, maxZ);
        SimpleUnitCell.setMinMaxLatticeParameters(dim, minXYZ, maxXYZ, kcode);

        // base origin for new unit cell
        pt0 = P3.newP(oabc[0]);

        // base vectors for new unit cell
        va = P3.newP(oabc[1]);
        vb = P3.newP(oabc[2]);
        vc = P3.newP(oabc[3]);

        adjustRangeMinMax(oabc);
      }
    }
    int iAtomFirst = asc.getLastAtomSetAtomIndex();
    if (bsAtoms != null)
      iAtomFirst = bsAtoms.nextSetBit(iAtomFirst);
    if (rminx == Float.MAX_VALUE) {
      supercell = null;
      oabc = null;
    } else {
      boolean doPack0 = doPackUnitCell;
      doPackUnitCell = doPack0;//(doPack0 || oabc != null && acr.forcePacked);
      bsAtoms = updateBSAtoms();
      applyAllSymmetry(acr.ms, null);
      doPackUnitCell = doPack0;

      // 2) set all atom coordinates to Cartesians

      Atom[] atoms = asc.atoms;
      int atomCount = asc.ac;
      for (int i = iAtomFirst; i < atomCount; i++) {
        symmetry.toCartesian(atoms[i], true);
        bsAtoms.set(i);
      }

      // 3) create the supercell unit cell

      symmetry = null;
      symmetry = getSymmetry();
      setUnitCell(new float[] { 0, 0, 0, 0, 0, 0, va.x, va.y, va.z, vb.x, vb.y,
          vb.z, vc.x, vc.y, vc.z }, null, offset);
      setAtomSetSpaceGroupName(oabc == null || supercell == null ? "P1"
          : "cell=" + supercell);
      symmetry.setSpaceGroup(doNormalize);
      symmetry.addSpaceGroupOperation("x,y,z", 0);

      // 4) reset atoms to fractional values in this new system

      if (pt0 != null)
        symmetry.toFractional(pt0, true);
      for (int i = iAtomFirst; i < atomCount; i++) {
        symmetry.toFractional(atoms[i], true);
        if (pt0 != null)
          atoms[i].sub(pt0);
      }

      // 5) apply the full lattice symmetry now

      asc.haveAnisou = false;

      // ?? TODO
      asc.setCurrentModelInfo("matUnitCellOrientation", null);

    }
    minXYZ = new P3i();
    maxXYZ = P3i.new3(maxX, maxY, maxZ);
    SimpleUnitCell.setMinMaxLatticeParameters(dim, minXYZ, maxXYZ, kcode);
    if (oabc == null) {
      applyAllSymmetry(acr.ms, bsAtoms);
      return;
    }
    if (acr.forcePacked || doPackUnitCell) {
      // trim atom set based on original unit cell
      Atom[] atoms = asc.atoms;
      BS bs = updateBSAtoms();
      for (int i = bs.nextSetBit(iAtomFirst); i >= 0; i = bs.nextSetBit(i + 1)) {
        if (!isWithinCell(dtype, atoms[i], minXYZ.x, maxXYZ.x, minXYZ.y,
            maxXYZ.y, minXYZ.z, maxXYZ.z, packingError))
          bs.clear(i);
      }
    }

    // but we leave matSupercell, because we might need it for vibrations in CASTEP
  }

  /**
   * Update asc.bsAtoms to include all atoms, or at least all
   * atoms that are still viable from the reader.
   * 
   * @return updated BS
   */
  private BS updateBSAtoms() {
    BS bs = asc.bsAtoms;
    if (bs == null)
      bs = asc.bsAtoms = BSUtil.newBitSet2(0, asc.ac);
    if (bs.nextSetBit(firstAtom) < 0)
      bs.setBits(firstAtom, asc.ac);
    return bs;
  }

  private void adjustRangeMinMax(T3[] oabc) {

    // be sure to add in packing error adjustments 
    // so that we include all needed atoms
    // load "" packed x.x supercell...
    P3 pa = new P3();
    P3 pb = new P3();
    P3 pc = new P3();
    if (acr.forcePacked) {
      pa.setT(oabc[1]);
      pb.setT(oabc[2]);
      pc.setT(oabc[3]);
      pa.scale(packingError);
      pb.scale(packingError);
      pc.scale(packingError);
    }

    // account for lattice specification
    // load "" {x y z} supercell...
    oabc[0].scaleAdd2(minXYZ.x, oabc[1], oabc[0]);
    oabc[0].scaleAdd2(minXYZ.y, oabc[2], oabc[0]);
    oabc[0].scaleAdd2(minXYZ.z, oabc[3], oabc[0]);
    // add in packing adjustment
    oabc[0].sub(pa);
    oabc[0].sub(pb);
    oabc[0].sub(pc);

    // fractionalize and adjust min/max
    P3 pt = P3.newP(oabc[0]);
    symmetry.toFractional(pt, true);
    setSymmetryMinMax(pt);

    // account for lattice specification
    oabc[1].scale(maxXYZ.x - minXYZ.x);
    oabc[2].scale(maxXYZ.y - minXYZ.y);
    oabc[3].scale(maxXYZ.z - minXYZ.z);
    // add in packing adjustment
    oabc[1].scaleAdd2(2, pa, oabc[1]);
    oabc[2].scaleAdd2(2, pb, oabc[2]);
    oabc[3].scaleAdd2(2, pc, oabc[3]);
    // run through six of the corners -- a, b, c, ab, ac, bc
    for (int i = 0; i < 3; i++) {
      for (int j = i + 1; j < 4; j++) {
        pt.add2(oabc[i], oabc[j]);
        if (i != 0)
          pt.add(oabc[0]);
        symmetry.toFractional(pt, false);
        setSymmetryMinMax(pt);
      }
    }
    // bc in the end, so we need abc
    symmetry.toCartesian(pt, false);
    pt.add(oabc[1]);
    symmetry.toFractional(pt, false);
    setSymmetryMinMax(pt);
    // allow for some imprecision
    minXYZ = P3i.new3((int) Math.min(0,Math.floor(rminx + 0.001f)),
        (int) Math.min(0, Math.floor(rminy + 0.001f)), 
        (int) Math.min(0, Math.floor(rminz + 0.001f)));
    maxXYZ = P3i.new3((int) Math.max(1,Math.ceil(rmaxx - 0.001f)),
        (int) Math.max(1,Math.ceil(rmaxy - 0.001f)), 
        (int) Math.max(1,Math.ceil(rmaxz - 0.001f)));
  }

  /**
   * range minima and maxima -- also usedf for cartesians comparisons
   * 
   */
  private float rminx, rminy, rminz, rmaxx, rmaxy, rmaxz;

  private void setSymmetryMinMax(P3 c) {
    if (rminx > c.x)
      rminx = c.x;
    if (rminy > c.y)
      rminy = c.y;
    if (rminz > c.z)
      rminz = c.z;
    if (rmaxx < c.x)
      rmaxx = c.x;
    if (rmaxy < c.y)
      rmaxy = c.y;
    if (rmaxz < c.z)
      rmaxz = c.z;
  }

  private final P3 ptOffset = new P3();

  //private P3 unitCellOffset;

  private P3i minXYZ, maxXYZ;
  private P3 minXYZ0, maxXYZ0;

  public boolean isWithinCell(int dtype, P3 pt, float minX, float maxX,
                              float minY, float maxY, float minZ, float maxZ,
                              float slop) {
    return (pt.x > minX - slop && pt.x < maxX + slop
        && (dtype < 2 || pt.y > minY - slop && pt.y < maxY + slop) 
        && (dtype < 3 || pt.z > minZ - slop && pt.z < maxZ + slop));
  }
  
  //  /**
  //   * A problem arises when converting to JavaScript, because JavaScript numbers are all
  //   * doubles, while here we have floats. So what we do is to multiply by a number that
  //   * is beyond the precision of our data but within the range of floats -- namely, 100000,
  //   * and then integerize. This ensures that both doubles and floats compare the same number.
  //   * Unfortunately, it will break Java reading of older files, so we check for legacy versions. 
  //   * 
  //   * @param dtype
  //   * @param pt
  //   * @param minX
  //   * @param maxX
  //   * @param minY
  //   * @param maxY
  //   * @param minZ
  //   * @param maxZ
  //   * @param slop
  //   * @return  true if within range
  //   */
  //  private boolean xxxisWithinCellInt(int dtype, P3 pt, float minX, float maxX,
  //                              float minY, float maxY, float minZ, float maxZ,
  //                              int slop) {
  //    switch (dtype) {
  //    case 3:
  //      if (Math.round((minZ - pt.z) * 100000) >= slop || Math.round((pt.z - maxZ) * 100000) >= slop)
  //        return false;
  //      //$FALL-THROUGH$
  //    case 2:
  //      if (Math.round((minY - pt.y) * 100000) >= slop || Math.round((pt.y - maxY) * 100000) >= slop)
  //        return false;
  //      //$FALL-THROUGH$
  //    case 1:
  //      if (Math.round((minX - pt.x) * 100000) >= slop || Math.round((pt.x - maxX) * 100000) >= slop)
  //        return false;
  //      break;
  //    }
  //    return true;
  //  }

  /**
   * @param ms
   *        modulated structure interface
   * @param bsAtoms
   *        relating to supercells
   * @throws Exception
   */
  private void applyAllSymmetry(MSInterface ms, BS bsAtoms) throws Exception {
    if (asc.ac == 0 || bsAtoms != null && bsAtoms.isEmpty())
      return;
    int n = noSymmetryCount = asc.baseSymmetryAtomCount > 0 ? asc.baseSymmetryAtomCount
        : bsAtoms == null ? asc.getLastAtomSetAtomCount() : asc.ac
            - bsAtoms.nextSetBit(asc.getLastAtomSetAtomIndex());
    asc.setTensors();
    bondCount0 = asc.bondCount;
    finalizeSymmetry(symmetry);
    int operationCount = symmetry.getSpaceGroupOperationCount();
    BS excludedOps = (acr.thisBiomolecule == null ? null : new BS());
    if (excludedOps != null)
      asc.checkSpecial = true;
    dtype = (int) symmetry.getUnitCellInfoType(SimpleUnitCell.INFO_DIMENSIONS);
    SimpleUnitCell.setMinMaxLatticeParameters(dtype, minXYZ, maxXYZ, 0);
    latticeOp = symmetry.getLatticeOp();
    latticeOnly = (asc.checkLatticeOnly && latticeOp >= 0); // CrystalReader
    if (doCentroidUnitCell)
      asc.setInfo("centroidMinMax", new int[] { minXYZ.x, minXYZ.y, minXYZ.z,
          maxXYZ.x, maxXYZ.y, maxXYZ.z, (centroidPacked ? 1 : 0) });
//    if (ptSupercell != null) {
//      asc.setCurrentModelInfo("supercell", ptSupercell);
//      switch (dtype) {
//      case 3:
//        // standard
//        minXYZ.z *= (int) Math.abs(ptSupercell.z);
//        maxXYZ.z *= (int) Math.abs(ptSupercell.z);
//        //$FALL-THROUGH$;
//      case 2:
//        // slab or standard
//        minXYZ.y *= (int) Math.abs(ptSupercell.y);
//        maxXYZ.y *= (int) Math.abs(ptSupercell.y);
//        //$FALL-THROUGH$;
//      case 1:
//        // slab, polymer, or standard
//        minXYZ.x *= (int) Math.abs(ptSupercell.x);
//        maxXYZ.x *= (int) Math.abs(ptSupercell.x);
//      }
//    }
    if (doCentroidUnitCell || doPackUnitCell || symmetryRange != 0
        && maxXYZ.x - minXYZ.x == 1 && maxXYZ.y - minXYZ.y == 1
        && maxXYZ.z - minXYZ.z == 1) {
      // weird Mac bug does not allow   Point3i.new3(minXYZ) !!
      minXYZ0 = P3.new3(minXYZ.x, minXYZ.y, minXYZ.z);
      maxXYZ0 = P3.new3(maxXYZ.x, maxXYZ.y, maxXYZ.z);
      if (ms != null) {
        ms.setMinMax0(minXYZ0, maxXYZ0);
        minXYZ.set((int) minXYZ0.x, (int) minXYZ0.y, (int) minXYZ0.z);
        maxXYZ.set((int) maxXYZ0.x, (int) maxXYZ0.y, (int) maxXYZ0.z);
      }
      switch (dtype) {
      case 3:
        // standard
        minXYZ.z--;
        maxXYZ.z++;
        //$FALL-THROUGH$;
      case 2:
        // slab or standard
        minXYZ.y--;
        maxXYZ.y++;
        //$FALL-THROUGH$;
      case 1:
        // slab, polymer, or standard
        minXYZ.x--;
        maxXYZ.x++;
      }
    }
    int nCells = (maxXYZ.x - minXYZ.x) * (maxXYZ.y - minXYZ.y)
        * (maxXYZ.z - minXYZ.z);
    int nsym = n *  (latticeOnly ? 4 : operationCount);
    int cartesianCount = (asc.checkSpecial || acr.thisBiomolecule != null ? 
        nsym * nCells : symmetryRange > 0 ? nsym  // checking against {1 1 1}
    //        : symmetryRange < 0 ? 1 // checking against symop=1555 set; just a box
        : 1 // not checking
    );
    P3[] cartesians = new P3[cartesianCount];
    Atom[] atoms = asc.atoms;
    for (int i = 0; i < n; i++)
      atoms[firstAtom + i].bsSymmetry = BS.newN(operationCount
          * (nCells + 1));
    int pt = 0;
    int[] unitCells = new int[nCells];
    unitCellTranslations = new V3[nCells];
    int iCell = 0;
    int cell555Count = 0;
    float absRange = Math.abs(symmetryRange);
    boolean checkCartesianRange = (symmetryRange != 0);
    boolean checkRangeNoSymmetry = (symmetryRange < 0);
    boolean checkRange111 = (symmetryRange > 0);
    if (checkCartesianRange) {
      rminx = rminy = rminz = Float.MAX_VALUE;
      rmaxx = rmaxy = rmaxz = -Float.MAX_VALUE;
    }
    // always do the 555 cell first

    // incommensurate symmetry can have lattice centering, resulting in 
    // duplication of operators. There's a bug later on that requires we 
    // only do this with the first atom set for now, at least.
    SymmetryInterface thisSymmetry = symmetry;
    SymmetryInterface lastSymmetry = thisSymmetry;
    checkAll = (latticeOnly || asc.atomSetCount == 1 && asc.checkSpecial && latticeOp >= 0);
    P3 pttemp = null;
    M4 op = thisSymmetry.getSpaceGroupOperation(0);
    if (doPackUnitCell) {
      pttemp = new P3();
      ptOffset.set(0, 0, 0);
    }
    int[] atomMap = (bondCount0 > asc.bondIndex0 && applySymmetryToBonds ? new int[n]
        : null);
    Lst<M4> lstNCS = acr.lstNCS;

    if (lstNCS != null && lstNCS.get(0).m33 == 0) {
      int nOp = thisSymmetry.getSpaceGroupOperationCount();
      int nn = lstNCS.size();
      for (int i = nn; --i >= 0;) {
        M4 m = lstNCS.get(i);
        m.m33 = 1;
        thisSymmetry.toFractionalM(m);
      }
      for (int i = 1; i < nOp; i++) {
        M4 m1 = thisSymmetry.getSpaceGroupOperation(i);
        for (int j = 0; j < nn; j++) {
          M4 m = M4.newM4(lstNCS.get(j));
          m.mul2(m1, m);
          if (doNormalize)
            SymmetryOperation.setOffset(m, atoms, firstAtom,
                noSymmetryCount);
          lstNCS.addLast(m);
        }
      }
    }

    for (int tx = minXYZ.x; tx < maxXYZ.x; tx++)
      for (int ty = minXYZ.y; ty < maxXYZ.y; ty++)
        for (int tz = minXYZ.z; tz < maxXYZ.z; tz++) {
          unitCellTranslations[iCell] = V3.new3(tx, ty, tz);
          unitCells[iCell++] = 555 + tx * 100 + ty * 10 + tz;
          if (tx != 0 || ty != 0 || tz != 0 || cartesians.length == 0)
            continue;

          // base cell only

          for (pt = 0; pt < n; pt++) {
            Atom atom = atoms[firstAtom + pt];
            if (ms != null) {
              thisSymmetry = ms.getAtomSymmetry(atom, this.symmetry);
              if (thisSymmetry != lastSymmetry) {
                if (thisSymmetry.getSpaceGroupOperationCount() == 0)
                  finalizeSymmetry(lastSymmetry = thisSymmetry);
                op = thisSymmetry.getSpaceGroupOperation(0);
              }
            }
            P3 c = P3.newP(atom);
            op.rotTrans(c);
            thisSymmetry.toCartesian(c, false);
            if (doPackUnitCell) {
              thisSymmetry.toUnitCell(c, ptOffset);
              pttemp.setT(c);
              thisSymmetry.toFractional(pttemp, false);
              if (acr.fixJavaFloat)
                PT.fixPtFloats(pttemp, PT.FRACTIONAL_PRECISION);
              // when bsAtoms != null, we are
              // setting it to be correct for a 
              // second unit cell -- the supercell
              if (bsAtoms == null)
                atom.setT(pttemp);
              else if (atom.distance(pttemp) < 0.0001f)
                bsAtoms.set(atom.index);
              else {// not in THIS unit cell
                bsAtoms.clear(atom.index);
                continue;
              }
            }
            if (bsAtoms != null)
              atom.bsSymmetry.clearAll();
            atom.bsSymmetry.set(iCell * operationCount);
            atom.bsSymmetry.set(0);
            if (checkCartesianRange)
              setSymmetryMinMax(c);
            if (pt < cartesianCount)
              cartesians[pt] = c;
          }
          if (checkRangeNoSymmetry) {
            rminx -= absRange;
            rminy -= absRange;
            rminz -= absRange;
            rmaxx += absRange;
            rmaxy += absRange;
            rmaxz += absRange;
          }
          cell555Count = pt = symmetryAddAtoms(0, 0, 0, 0, pt, iCell
              * operationCount, cartesians, ms, excludedOps, atomMap);
        }
    if (checkRange111) {
      rminx -= absRange;
      rminy -= absRange;
      rminz -= absRange;
      rmaxx += absRange;
      rmaxy += absRange;
      rmaxz += absRange;
    }

    // now apply all the translations
    iCell = 0;
    for (int tx = minXYZ.x; tx < maxXYZ.x; tx++)
      for (int ty = minXYZ.y; ty < maxXYZ.y; ty++)
        for (int tz = minXYZ.z; tz < maxXYZ.z; tz++) {
          iCell++;
          if (tx != 0 || ty != 0 || tz != 0)
            pt = symmetryAddAtoms(tx, ty, tz, cell555Count, pt, iCell
                * operationCount, cartesians, ms, excludedOps, atomMap);
        }
    if (iCell * n == asc.ac - firstAtom)
      duplicateAtomProperties(iCell);
    setSymmetryOps();
    asc.setCurrentModelInfo("presymmetryAtomIndex",
        Integer.valueOf(firstAtom));
    asc.setCurrentModelInfo("presymmetryAtomCount", Integer.valueOf(n));
    asc.setCurrentModelInfo("latticeDesignation",
        thisSymmetry.getLatticeDesignation());
    asc.setCurrentModelInfo("unitCellRange", unitCells);
    asc.setCurrentModelInfo("unitCellTranslations", unitCellTranslations);
    baseUnitCell = unitCellParams;
    unitCellParams = new float[6];
    reset();
  }

  private boolean checkAll;
  private int bondCount0;

  private int symmetryAddAtoms(int transX, int transY, int transZ,
                               int baseCount, int pt, int iCellOpPt,
                               P3[] cartesians, MSInterface ms, BS excludedOps,
                               int[] atomMap) throws Exception {
    boolean isBaseCell = (baseCount == 0);
    boolean addBonds = (atomMap != null);
    if (doPackUnitCell)
      ptOffset.set(transX, transY, transZ);

    //symmetryRange < 0 : just check symop=1 set
    //symmetryRange > 0 : check against {1 1 1}

    // if we are not checking special atoms, then this is a PDB file
    // and we return all atoms within a cubical volume around the 
    // target set. The user can later use select within() to narrow that down
    // This saves immensely on time.

    float range2 = symmetryRange * symmetryRange;
    boolean checkRangeNoSymmetry = (symmetryRange < 0);
    boolean checkRange111 = (symmetryRange > 0);
    boolean checkSymmetryMinMax = (isBaseCell && checkRange111);
    checkRange111 &= !isBaseCell;
    int nOp = symmetry.getSpaceGroupOperationCount();
    Lst<M4> lstNCS = acr.lstNCS;
    int nNCS = (lstNCS == null ? 0 : lstNCS.size());
    int nOperations = nOp + nNCS;
    nNCS = nNCS / nOp;
    boolean checkSpecial = (nOperations == 1 && !doPackUnitCell ? false
        : asc.checkSpecial);
    boolean checkSymmetryRange = (checkRangeNoSymmetry || checkRange111);
    boolean checkDistances = (checkSpecial || checkSymmetryRange);
    boolean checkOps = (excludedOps != null);
    boolean addCartesian = (checkSpecial || checkSymmetryMinMax);
    BS bsAtoms = (acr.isMolecular ? null : asc.bsAtoms);
    SymmetryInterface symmetry = this.symmetry;
    if (checkRangeNoSymmetry)
      baseCount = noSymmetryCount;
    int atomMax = firstAtom + noSymmetryCount;
    P3 ptAtom = new P3();
    String code = null;
    float d0 = (checkOps ? 0.01f : 0.0001f);
    char subSystemId = '\0';
    int j00 = (bsAtoms == null ? firstAtom : bsAtoms.nextSetBit(firstAtom));
    out: for (int iSym = 0; iSym < nOperations; iSym++) {

      if (isBaseCell && iSym == 0 
          || latticeOnly && iSym > 0 && (iSym % latticeOp) != 0 
          || excludedOps != null && excludedOps.get(iSym))
        continue;

      /* pt0 sets the range of points cross-checked. 
       * If we are checking special positions, then we have to check
       *   all previous atoms. 
       * If we are doing a symmetry range check relative to {1 1 1}, then
       *   we have to check only the base set. (checkRange111 true)
       * If we are doing a symmetry range check on symop=1555 (checkRangeNoSymmetry true), 
       *   then we don't check any atoms and just use the box.
       *    
       */

      int pt0 = firstAtom + (checkSpecial || excludedOps != null ? pt
          : checkRange111 ? baseCount : 0);
      float spinOp = (iSym >= nOp ? 0 : asc.vibScale == 0 ? symmetry
          .getSpinOp(iSym) : asc.vibScale);
      int i0 = Math.max(firstAtom,
          (bsAtoms == null ? 0 : bsAtoms.nextSetBit(0)));
      boolean checkDistance = checkDistances;
      int spt = (iSym >= nOp ? (iSym - nOp) / nNCS : iSym);
      int cpt = spt + iCellOpPt;
      for (int i = i0; i < atomMax; i++) {
        Atom a = asc.atoms[i];
        if (a.ignoreSymmetry || bsAtoms != null && !bsAtoms.get(i))
          continue;

        if (ms == null) {
          symmetry.newSpaceGroupPoint(iSym, a, ptAtom, transX, transY, transZ,
              (iSym >= nOp ? lstNCS.get(iSym - nOp) : null));
        } else {
          symmetry = ms.getAtomSymmetry(a, this.symmetry);
          symmetry.newSpaceGroupPoint(iSym, a, ptAtom, transX, transY, transZ,
              null);
          // COmmensurate structures may use a symmetry operator
          // to changes space groups.
          code = symmetry.getSpaceGroupOperationCode(iSym);
          if (code != null) {
            subSystemId = code.charAt(0);
            symmetry = ms.getSymmetryFromCode(code);
            if (symmetry.getSpaceGroupOperationCount() == 0)
              finalizeSymmetry(symmetry);
          }
        }
        if (acr.fixJavaFloat)
          PT.fixPtFloats(ptAtom, PT.FRACTIONAL_PRECISION);
        P3 c = P3.newP(ptAtom); // cartesian position
        symmetry.toCartesian(c, false);
        if (doPackUnitCell) {
          // note that COmmensurate structures may need 
          // modulation at this point.
          symmetry.toUnitCell(c, ptOffset);
          ptAtom.setT(c);
          symmetry.toFractional(ptAtom, false);
          if (acr.fixJavaFloat)
            PT.fixPtFloats(ptAtom, PT.FRACTIONAL_PRECISION);
          if (!isWithinCell(dtype, ptAtom, minXYZ0.x, maxXYZ0.x, minXYZ0.y,
              maxXYZ0.y, minXYZ0.z, maxXYZ0.z, packingError))
            continue;
        }
        if (checkSymmetryMinMax)
          setSymmetryMinMax(c);
        Atom special = null;
        if (checkDistance) {
          // for range checking, we first make sure we are not out of range
          // for the cartesian
          if (checkSymmetryRange
              && (c.x < rminx || c.y < rminy || c.z < rminz || c.x > rmaxx
                  || c.y > rmaxy || c.z > rmaxz))
            continue;
          float minDist2 = Float.MAX_VALUE;
          // checkAll means we have to check against operations that have
          // just been done; otherwise we can check only to the base set
          int j0 = (checkAll ? asc.ac : pt0);
          String name = a.atomName;
          char id = (code == null ? a.altLoc : subSystemId);
          for (int j = j00; j < j0; j++) {
            if (bsAtoms != null && !bsAtoms.get(j))
              continue;
            P3 pc = cartesians[j - firstAtom];
            if (pc == null)
              continue;
            float d2 = c.distanceSquared(pc);
            //System.out.println(iSym + " " +  i + " " + j + " " + pc + " " + c + " " + d2);
            if (checkSpecial && d2 < d0) {
              /* checkSpecial indicates that we are looking for atoms with (nearly) the
               * same cartesian position.  
               */
              if (checkOps) {
                // if a matching atom is found for a model built
                // from a mix of crystallographic and noncrystallographic 
                // operators, we throw out the entire operation, not just this atom
                excludedOps.set(iSym);
                continue out;
              }
              special = asc.atoms[j];              
              if ((special.atomName == null || special.atomName.equals(name))
                  && special.altLoc == id)
                break;
              special = null;
            }
            if (checkRange111 && j < baseCount && d2 < minDist2)
              minDist2 = d2;
          }
          if (checkRange111 && minDist2 > range2)
            continue;
        }
        if (checkOps) {
          // if we did not find a common atom for the first atom when checking operators,
          // we do not have to check again for any other atom.
          checkDistance = false;
        }
        int atomSite = a.atomSite;
        if (special != null) {
          if (addBonds)
            atomMap[atomSite] = special.index;
          special.bsSymmetry.set(cpt);
          special.bsSymmetry.set(spt);
        } else {
          if (addBonds)
            atomMap[atomSite] = asc.ac;
          Atom atom1 = asc.newCloneAtom(a);
          atom1.setT(ptAtom);
          if (asc.bsAtoms != null)
            asc.bsAtoms.set(atom1.index);
          if (spinOp != 0 && atom1.vib != null) {
            // spinOp is making the correction for spin being a pseudoVector, not a standard vector
            symmetry.getSpaceGroupOperation(iSym).rotate(atom1.vib);
            atom1.vib.scale(spinOp);
          }
          atom1.atomSite = atomSite;
          if (code != null)
            atom1.altLoc = subSystemId;
          atom1.bsSymmetry = BSUtil.newAndSetBit(cpt);
          atom1.bsSymmetry.set(spt);
          if (addCartesian)
            cartesians[pt++] = c;
          Lst<Object> tensors = a.tensors;
          if (tensors != null) {
            atom1.tensors = null;
            for (int j = tensors.size(); --j >= 0;) {
              Tensor t = (Tensor) tensors.get(j);
              if (t == null)
                continue;
              if (nOp == 1)
                atom1.addTensor(t.copyTensor(), null, false);
              else
                addRotatedTensor(atom1, t, iSym, false, symmetry);
            }
          }
        }
      }
      if (addBonds) {
        // Clone bonds
        Bond[] bonds = asc.bonds;
        Atom[] atoms = asc.atoms;
        for (int bondNum = asc.bondIndex0; bondNum < bondCount0; bondNum++) {
          Bond bond = bonds[bondNum];
          Atom atom1 = atoms[bond.atomIndex1];
          Atom atom2 = atoms[bond.atomIndex2];
          if (atom1 == null || atom2 == null)
            continue;
          int iAtom1 = atomMap[atom1.atomSite];
          int iAtom2 = atomMap[atom2.atomSite];
          if (iAtom1 >= atomMax || iAtom2 >= atomMax)
            asc.addNewBondWithOrder(iAtom1, iAtom2, bond.order);
        }
      }
    }
    return pt;
  }

  @SuppressWarnings("unchecked")
  private void duplicateAtomProperties(int nTimes) {
    Map<String, Object> p = (Map<String, Object>) asc
        .getAtomSetAuxiliaryInfoValue(-1, "atomProperties");
    if (p != null)
      for (Map.Entry<String, Object> entry : p.entrySet()) {
        String key = entry.getKey();
        Object val = entry.getValue();
        if (val instanceof String) {
          String data = (String) val;
          SB s = new SB();
          for (int i = nTimes; --i >= 0;)
            s.append(data);
          p.put(key, s.toString());
        } else {
          float[] f = (float[]) val;
          float[] fnew = new float[f.length * nTimes];
          for (int i = nTimes; --i >= 0;)
            System.arraycopy(f, 0, fnew, i * f.length, f.length);
        }
      }
  }

  private void finalizeSymmetry(SymmetryInterface symmetry) {
    String name = (String) asc.getAtomSetAuxiliaryInfoValue(-1, "spaceGroup");
    symmetry.setFinalOperations(name, asc.atoms, firstAtom,
        noSymmetryCount, doNormalize, filterSymop);
    if (filterSymop != null || name == null || name.equals("unspecified!"))
      setAtomSetSpaceGroupName(symmetry.getSpaceGroupName());
  }

  private void setSymmetryOps() {
    int operationCount = symmetry.getSpaceGroupOperationCount();
    if (operationCount > 0) {
      String[] symmetryList = new String[operationCount];
      for (int i = 0; i < operationCount; i++)
        symmetryList[i] = "" + symmetry.getSpaceGroupXyz(i, doNormalize);
      asc.setCurrentModelInfo("symmetryOperations", symmetryList);
      asc.setCurrentModelInfo("symmetryOps",
          symmetry.getSymmetryOperations());
    }
    asc.setCurrentModelInfo("symmetryCount",
        Integer.valueOf(operationCount));
    asc.setCurrentModelInfo("latticeType", acr.latticeType == null ? "P" : acr.latticeType);
    asc.setCurrentModelInfo("intlTableNo", symmetry.getIntTableNumber());
    if (acr.sgName == null || acr.sgName.indexOf("?") >= 0 || acr.sgName.indexOf("!") >= 0)
      setAtomSetSpaceGroupName(acr.sgName = symmetry.getSpaceGroupName());
  }

  public T3 getOverallSpan() {
    return (maxXYZ0 == null ? V3.new3(maxXYZ.x - minXYZ.x, maxXYZ.y - minXYZ.y,
        maxXYZ.z - minXYZ.z) : V3.newVsub(maxXYZ0, minXYZ0));
  }
  
  private int dtype = 3;
  private V3[] unitCellTranslations;
  private int latticeOp;
  private boolean latticeOnly;
  private int noSymmetryCount;
  private int firstAtom;

  private final static int PARTICLE_NONE = 0;
  private final static int PARTICLE_CHAIN = 1;
  private final static int PARTICLE_SYMOP = 2;

  @SuppressWarnings("unchecked")
  public void applySymmetryBio(Map<String, Object> thisBiomolecule,
                               boolean applySymmetryToBonds, String filter) {
    Lst<M4> biomts = (Lst<M4>) thisBiomolecule.get("biomts");
    if (biomts.size() < 2)
      return;
    acr.lstNCS = null; // disable NCS
    setLatticeCells();
    int[] lc = (latticeCells != null && latticeCells[0] != 0 ? new int[3] : null);
    if (lc != null)
      for (int i = 0; i < 3; i++)
        lc[i] = latticeCells[i];
    latticeCells = null;
    int particleMode = (filter.indexOf("BYCHAIN") >= 0 ? PARTICLE_CHAIN
        : filter.indexOf("BYSYMOP") >= 0 ? PARTICLE_SYMOP : PARTICLE_NONE);
    doNormalize = false;
    Lst<String> biomtchains = (Lst<String>) thisBiomolecule.get("chains");
    if (biomtchains.get(0).equals(biomtchains.get(1)))
      biomtchains = null;
    symmetry = null;
    // it's not clear to me why you would do this:
    //if (!Float.isNaN(unitCellParams[0])) // PDB can do this; 
      //setUnitCell(unitCellParams, null, unitCellOffset);
    getSymmetry().setSpaceGroup(doNormalize);
    //symmetry.setUnitCell(null);
    addSpaceGroupOperation("x,y,z", false);
    String name = (String) thisBiomolecule.get("name");
    setAtomSetSpaceGroupName(acr.sgName = name);
    int len = biomts.size();
    this.applySymmetryToBonds = applySymmetryToBonds;
    bondCount0 = asc.bondCount;
    firstAtom = asc.getLastAtomSetAtomIndex();
    int atomMax = asc.ac;
    Map<Integer, BS> ht = new Hashtable<Integer, BS>();
    int nChain = 0;
    Atom[] atoms = asc.atoms;
    boolean addBonds = (bondCount0 > asc.bondIndex0 && applySymmetryToBonds);
    switch (particleMode) {
    case PARTICLE_CHAIN:
      for (int i = atomMax; --i >= firstAtom;) {
        Integer id = Integer.valueOf(atoms[i].chainID);
        BS bs = ht.get(id);
        if (bs == null) {
          nChain++;
          ht.put(id, bs = new BS());
        }
        bs.set(i);
      }
      asc.bsAtoms = new BS();
      for (int i = 0; i < nChain; i++) {
        asc.bsAtoms.set(atomMax + i);
        Atom a = new Atom();
        a.set(0, 0, 0);
        a.radius = 16;
        asc.addAtom(a);
      }
      int ichain = 0;
      for (Entry<Integer, BS> e : ht.entrySet()) {
        Atom a = atoms[atomMax + ichain++];
        BS bs = e.getValue();
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
          a.add(atoms[i]);
        a.scale(1f / bs.cardinality());
        a.atomName = "Pt" + ichain;
        a.chainID = e.getKey().intValue();
      }
      firstAtom = atomMax;
      atomMax += nChain;
      addBonds = false;
      break;
    case PARTICLE_SYMOP:
      asc.bsAtoms = new BS();
      asc.bsAtoms.set(atomMax);
      Atom a = atoms[atomMax] = new Atom();
      a.set(0, 0, 0);
      for (int i = atomMax; --i >= firstAtom;)
        a.add(atoms[i]);
      a.scale(1f / (atomMax - firstAtom));
      a.atomName = "Pt";
      a.radius = 16;
      asc.addAtom(a);
      firstAtom = atomMax++;
      addBonds = false;
      break;
    }
    Map<String, BS> assemblyIdAtoms = (Map<String, BS>) thisBiomolecule
        .get("asemblyIdAtoms");
    if (filter.indexOf("#<") >= 0) {
      len = Math.min(len,
          PT.parseInt(filter.substring(filter.indexOf("#<") + 2)) - 1);
      filter = PT.rep(filter, "#<", "_<");
    }
    for (int iAtom = firstAtom; iAtom < atomMax; iAtom++)
      atoms[iAtom].bsSymmetry = BSUtil.newAndSetBit(0);
    BS bsAtoms = asc.bsAtoms;
    int[] atomMap = (addBonds ? new int[asc.ac] : null);
    for (int i = (biomtchains == null ? 1 : 0); i < len; i++) {
      if (filter.indexOf("!#") >= 0) {
        if (filter.indexOf("!#" + (i + 1) + ";") >= 0)
          continue;
      } else if (filter.indexOf("#") >= 0
          && filter.indexOf("#" + (i + 1) + ";") < 0) {
        continue;
      }
      M4 mat = biomts.get(i);
      String chains = (biomtchains == null ? null : biomtchains.get(i));
      if (chains != null && assemblyIdAtoms != null) {
        // must use label_asym_id, not auth_asym_id // bug fix 11/18/2015 
        bsAtoms = new BS();
        for (Entry<String, BS> e : assemblyIdAtoms.entrySet())
          if (chains.indexOf(":" + e.getKey() + ";") >= 0)
            bsAtoms.or(e.getValue());
        if (asc.bsAtoms != null)
          bsAtoms.and(asc.bsAtoms);
        chains = null;
      }
      for (int iAtom = firstAtom; iAtom < atomMax; iAtom++) {
        if (bsAtoms != null
            && !bsAtoms.get(iAtom)
            || chains != null
            && chains.indexOf(":" + acr.vwr.getChainIDStr(atoms[iAtom].chainID)
                + ";") < 0)
          continue;
        try {
          int atomSite = atoms[iAtom].atomSite;
          Atom atom1;
          if (addBonds)
            atomMap[atomSite] = asc.ac;
          atom1 = asc.newCloneAtom(atoms[iAtom]);
          if (asc.bsAtoms != null)
            asc.bsAtoms.set(atom1.index);
          atom1.atomSite = atomSite;
          mat.rotTrans(atom1);
          atom1.bsSymmetry = BSUtil.newAndSetBit(i);
        } catch (Exception e) {
          asc.errorMessage = "appendAtomCollection error: " + e;
        }
      }
      if (i > 0) {
        symmetry.addBioMoleculeOperation(mat, false);
        if (addBonds) {
          // Clone bonds
          for (int bondNum = asc.bondIndex0; bondNum < bondCount0; bondNum++) {
            Bond bond = asc.bonds[bondNum];
            int iAtom1 = atomMap[atoms[bond.atomIndex1].atomSite];
            int iAtom2 = atomMap[atoms[bond.atomIndex2].atomSite];
//            if (iAtom1 >= atomMax || iAtom2 >= atomMax)
            asc.addNewBondWithOrder(iAtom1, iAtom2, bond.order);
          }
        }
      }
    }
    if (biomtchains != null) {
      if (asc.bsAtoms == null)
        asc.bsAtoms = BSUtil.newBitSet2(0, asc.ac);
      asc.bsAtoms.clearBits(firstAtom, atomMax);
    }

    noSymmetryCount = atomMax - firstAtom;
    asc.setCurrentModelInfo("presymmetryAtomIndex",
        Integer.valueOf(firstAtom));
    asc.setCurrentModelInfo("presymmetryAtomCount",
        Integer.valueOf(noSymmetryCount));
    asc.setCurrentModelInfo("biosymmetryCount", Integer.valueOf(len));
    asc.setCurrentModelInfo("biosymmetry", symmetry);
    finalizeSymmetry(symmetry);
    setSymmetryOps();
    reset();
    //TODO: need to clone bonds
  }

  private void reset() {
    asc.coordinatesAreFractional = false;
    asc.setCurrentModelInfo("hasSymmetry", Boolean.TRUE);
    asc.setGlobalBoolean(AtomSetCollection.GLOBAL_SYMMETRY);
  }

  private P3 ptTemp;
  private M3 mTemp;

  public Tensor addRotatedTensor(Atom a, Tensor t, int iSym, boolean reset,
                                 SymmetryInterface symmetry) {
    if (ptTemp == null) {
      ptTemp = new P3();
      mTemp = new M3();
    }
    return a.addTensor(((Tensor) acr.getInterface("org.jmol.util.Tensor"))
        .setFromEigenVectors(
            symmetry.rotateAxes(iSym, t.eigenVectors, ptTemp, mTemp),
            t.eigenValues, t.isIsotropic ? "iso" : t.type, t.id, t), null,
        reset);
  }

  void setTensors() {
    int n = asc.ac;
    for (int i = asc.getLastAtomSetAtomIndex(); i < n; i++) {
      Atom a = asc.atoms[i];
      if (a.anisoBorU == null)
        continue;
      // getTensor will return correct type
      a.addTensor(symmetry.getTensor(acr.vwr, a.anisoBorU), null, false);
      if (Float.isNaN(a.bfactor))
        a.bfactor = a.anisoBorU[7] * 100f;
      // prevent multiple additions
      a.anisoBorU = null;
    }
  }

  public void setTimeReversal(int op, int timeRev) {
    symmetry.setTimeReversal(op, timeRev);
  }

  private int nVib;

  public int setSpinVectors() {
    // return spin vectors to cartesians
    if (nVib > 0 || asc.iSet < 0 || !acr.vibsFractional)
      return nVib; // already done
    int i0 = asc.getAtomSetAtomIndex(asc.iSet);
    SymmetryInterface sym = getBaseSymmetry();
    for (int i = asc.ac; --i >= i0;) {
      Vibration v = (Vibration) asc.atoms[i].vib;
      if (v != null) {
        if (v.modDim > 0) {
          ((JmolModulationSet) v).setMoment();
        } else {
          //System.out.println("xytalsym v=" + v + "  "+ i + "  ");
          v = (Vibration) v.clone(); // this could be a modulation set
          sym.toCartesian(v, true);
          asc.atoms[i].vib = v;
        }
        nVib++;
      }
    }
    return nVib;
  }

  /**
   * magCIF files have moments expressed as Bohr magnetons along
   * the cryrstallographic axes. These have to be "fractionalized" in order
   * to be properly handled by symmetry operations, then, in the end, turned
   * into Cartesians.
   * 
   * It is not clear to me at all how this would be handled if there are subsystems.
   * This method must be run PRIOR to applying symmetry and thus prior to creation of 
   * modulation sets.
   * 
   */
  public void scaleFractionalVibs() {
    float[] params = getBaseSymmetry().getUnitCellParams();
    P3 ptScale = P3.new3(1 / params[0], 1 / params[1], 1 / params[2]);
    int i0 = asc.getAtomSetAtomIndex(asc.iSet);
    for (int i = asc.ac; --i >= i0;) {
      Vibration v = (Vibration) asc.atoms[i].vib;
      if (v != null) {
        v.scaleT(ptScale);
      }
    }
  }

  /**
   * Get the symmetry that was in place prior to any supercell business
   * @return base symmetry
   */
  public SymmetryInterface getBaseSymmetry() {
    return (baseSymmetry == null ? symmetry : baseSymmetry);
  }
  
  /**
   * Ensure that ModelLoader sets up the supercell unit cell.
   * 
   * @param ptSupercell
   */
  public void finalizeUnitCell(P3 ptSupercell) {
    if (ptSupercell != null && baseUnitCell != null) {
      baseUnitCell[22] = Math.max(1, (int) ptSupercell.x);
      baseUnitCell[23] = Math.max(1, (int) ptSupercell.y);
      baseUnitCell[24] = Math.max(1, (int) ptSupercell.z);
    }
  }

  
//  static {
//    System.out.println(.01999998f);
//    System.out.println(1.01999998f);
//    System.out.println(2.01999998f);
//    System.out.println(9910.01999998f);
//    System.out.println(Math.round(100000*.01999998f));
//    System.out.println(Math.round(100000*.020000000000015));
//    System.out.println(Math.round(100000*-.01999998f));
//    System.out.println(Math.round(100000*-.020000000000015));
//    System.out.println(.01999998f+ Integer.MAX_VALUE);
//  }

}
