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

import org.jmol.api.JmolModulationSet;
import org.jmol.api.SymmetryInterface;
import org.jmol.symmetry.Symmetry;
import org.jmol.symmetry.SymmetryOperation;
import org.jmol.util.BSUtil;
import org.jmol.util.SimpleUnitCell;
import org.jmol.util.Tensor;
import org.jmol.util.Vibration;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.M3d;
import javajs.util.M4d;
import javajs.util.P3;
import javajs.util.P3d;
import javajs.util.P3i;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.T3d;
import javajs.util.V3;
import javajs.util.V3d;

/**
 * 
 * A class used by AtomSetCollection for building the symmetry of a model and
 * generating new atoms based on that symmetry.
 * 
 */
public class XtalSymmetry {

  private final static int PARTICLE_NONE = 0;
  private final static int PARTICLE_CHAIN = 1;
  private final static int PARTICLE_SYMOP = 2;
  private static final double MAX_INTERCHAIN_BOND_2 = 25; // allowing for hydrogen bonds

  private AtomSetCollectionReader acr;
  private AtomSetCollection asc;
  
  private SymmetryInterface baseSymmetry;
  private SymmetryInterface sym2;


  private boolean applySymmetryToBonds;
  private boolean centroidPacked;
  private boolean doCentroidUnitCell;
  private boolean doNormalize = true;
  private boolean doPackUnitCell;
  private boolean latticeOnly;

  private Lst<double[]> trajectoryUnitCells;

  private double[] unitCellParams = new double[6];
  private double[] baseUnitCell;
  private int[] latticeCells;
  private V3[] unitCellTranslations;
  
  // expands to 26 for cartesianToFractional matrix as array (PDB) and supercell

  private double symmetryRange;
  private double packingError;
  private String filterSymop;
  private SB bondsFound = new SB();
  
  private int ndims = 3;
  private int firstAtom;
  private int latticeOp;
  private int noSymmetryCount;
  private int nVib;

  private P3d ptTemp;
  private M3d mTemp;



  /**
   * range minima and maxima -- also usedf for cartesians comparisons
   * 
   */
  private double rminx, rminy, rminz, rmaxx, rmaxy, rmaxz;

  private final P3d ptOffset = new P3d();

  //private P3d unitCellOffset;

  private P3i minXYZ, maxXYZ;
  private P3d minXYZ0, maxXYZ0;

  private boolean checkAll;
  private int bondCount0;



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

  public SymmetryInterface getSymmetry() {
    return (symmetry == null ?(symmetry = (SymmetryInterface) acr.getInterface("org.jmol.symmetry.Symmetry")) : symmetry);
  }

  SymmetryInterface setSymmetry(SymmetryInterface symmetry) {
    return (this.symmetry = symmetry);
  }

  private void setSymmetryRange(float factor) {
    symmetryRange = factor;
    asc.setInfo("symmetryRange", Float.valueOf(factor));
  }

  private void setLatticeCells() {

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
    doPackUnitCell = acr.doPackUnitCell && !applySymmetryToBonds;
    doCentroidUnitCell = acr.doCentroidUnitCell;
    centroidPacked = acr.centroidPacked;
    filterSymop = acr.filterSymop;
    //if (acr.strSupercell == null)
      //setSupercellFromPoint(acr.ptSupercell);
  }

  private void setUnitCell(double[] info, M3d matUnitCellOrientation,
                                   P3 unitCellOffset) {
    unitCellParams = new double[info.length];
    //this.unitCellOffset = unitCellOffset;
    for (int i = 0; i < info.length; i++)
      unitCellParams[i] = info[i];
    asc.haveUnitCell = true;
    asc.setCurrentModelInfo("unitCellParams", unitCellParams);
    if (asc.isTrajectory) {
      if (trajectoryUnitCells == null) {
        trajectoryUnitCells = new Lst<double[]>();
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
    getSymmetry().setSpaceGroupTo(readerSymmetry.getSpaceGroup());
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
    /* kcode:  Generally the multiplier is just {ijk ijk scale}, but when we have
    *        1iiijjjkkk 1iiijjjkkk scale, doubles lose kkk due to Java double
    *        precision issues so we use P4d {1iiijjjkkk 1iiijjjkkk scale,
    *        1kkkkkk}. Here, our offset -- initially 0 or 1 from the uccage
    *        renderer, but later -500 or -499 -- tells us which code we are
    *        looking at, the first one or the second one.
    */
    int kcode = latticeCells[3];
    int dim = (int) symmetry
        .getUnitCellInfoType(SimpleUnitCell.INFO_DIMENSIONS);
    firstAtom = asc.getLastAtomSetAtomIndex();
    BS bsAtoms = asc.bsAtoms;
    if (bsAtoms != null) {
      updateBSAtoms();
      firstAtom = bsAtoms.nextSetBit(firstAtom);
    }
    rminx = rminy = rminz = Double.MAX_VALUE;
    rmaxx = rmaxy = rmaxz = -Double.MAX_VALUE;
    P3d pt0 = null;
    if (acr.latticeType == null)
      acr.latticeType = "" + symmetry.getLatticeType();
    if (acr.isPrimitive) {
      asc.setCurrentModelInfo("isprimitive", Boolean.TRUE);
      if (!"P".equals(acr.latticeType) || acr.primitiveToCrystal != null) {
        asc.setCurrentModelInfo("unitcell_conventional", symmetry
            .getConventionalUnitCell(acr.latticeType, acr.primitiveToCrystal));
      }
    }
    if (acr.latticeType != null) {
      asc.setCurrentModelInfo("latticeType", acr.latticeType);
      if (acr.fillRange instanceof String) {

        String type = (String) acr.fillRange; // conventional or primitive
        if (type.equals("conventional")) {
          acr.fillRange = symmetry.getConventionalUnitCell(acr.latticeType,
              acr.primitiveToCrystal);
        } else if (type.equals("primitive")) {
          acr.fillRange = symmetry.getUnitCellVectors();
          symmetry.toFromPrimitive(true, acr.latticeType.charAt(0),
              (T3d[]) acr.fillRange, acr.primitiveToCrystal);
        } else {
          acr.fillRange = null;
        }
        if (acr.fillRange != null)
          acr.addJmolScript("unitcell " + type);
      }
    }
    if (acr.fillRange != null) {

      bsAtoms = updateBSAtoms();
      acr.forcePacked = true;
      doPackUnitCell = false;
      minXYZ = new P3i();
      maxXYZ = P3i.new3(1, 1, 1);
      P3d[] oabc = new P3d[4];
      for (int i = 0; i < 4; i++)
        oabc[i] = P3d.newP(((T3d[]) acr.fillRange)[i]);
      adjustRangeMinMax(oabc);
      //Logger.info("setting min/max for original lattice to " + minXYZ + " and "
      //  + maxXYZ);
      if (sym2 == null) {
        sym2 = new Symmetry();
        sym2.getUnitCelld((T3d[]) acr.fillRange, false, null);
      }
      applyAllSymmetry(acr.ms, bsAtoms);
      pt0 = new P3d();
      Atom[] atoms = asc.atoms;
      for (int i = asc.ac; --i >= firstAtom;) {
        pt0.setT(atoms[i]);
        symmetry.toCartesian(pt0, false);
        sym2.toFractional(pt0, false);
        if (acr.fixJavaDouble)
          PT.fixPtDoubles(pt0, PT.FRACTIONAL_PRECISION);
        if (!isWithinCell(ndims, pt0, 0, 1, 0, 1, 0, 1, packingError))
          bsAtoms.clear(i);

      }
      return;
    }
    P3 offset = null;
    nVib = 0;
    T3d va = null, vb = null, vc = null;
    baseSymmetry = symmetry;
    String supercell = acr.strSupercell;
    T3d[] oabc = null;
    boolean isSuper = (supercell != null && supercell.indexOf(",") >= 0);
    if (isSuper) {
      // expand range to accommodate this alternative cell
      // oabc will be cartesian
      oabc = symmetry.getV0abc(supercell);
      if (oabc != null) {
        // set the bounds for atoms in the new unit cell
        // in terms of the old unit cell
        setMinMax(dim, kcode, maxX, maxY, maxZ);
        // base origin for new unit cell
        pt0 = P3d.newP(oabc[0]);

        // base vectors for new unit cell
        va = P3d.newP(oabc[1]);
        vb = P3d.newP(oabc[2]);
        vc = P3d.newP(oabc[3]);

        adjustRangeMinMax(oabc);
      }
    }
    int iAtomFirst = asc.getLastAtomSetAtomIndex();
    if (bsAtoms != null)
      iAtomFirst = bsAtoms.nextSetBit(iAtomFirst);
    if (rminx == Double.MAX_VALUE) {
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
      setUnitCell(new double[] { 0, 0, 0, 0, 0, 0, va.x, va.y, va.z, vb.x, vb.y,
          vb.z, vc.x, vc.y, vc.z }, null, offset);
      setAtomSetSpaceGroupName(
          oabc == null || supercell == null ? "P1" : "cell=" + supercell);
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
    setMinMax(dim, kcode, maxX, maxY, maxZ);
    if (oabc == null) {
      applyAllSymmetry(acr.ms, bsAtoms);
      if (!applySymmetryToBonds || !acr.doPackUnitCell)
        return;
      // fall through if packed and there are bonds, that is, when we did not shift atoms
      // so we reset to the original min and max and then trim.
      setMinMax(dim, kcode, maxX, maxY, maxZ);
    }
    if (acr.forcePacked || acr.doPackUnitCell) {
      trimToUnitCell(iAtomFirst);
    }

    // but we leave matSupercell, because we might need it for vibrations in CASTEP
  }

  private void setMinMax(int dim, int kcode, int maxX, int maxY, int maxZ) {
    minXYZ = new P3i();
    maxXYZ = P3i.new3(maxX, maxY, maxZ);
    SimpleUnitCell.setMinMaxLatticeParameters(dim, minXYZ, maxXYZ, kcode);
  }

  private void trimToUnitCell(int iAtomFirst) {
    // trim atom set based on current min/max
    Atom[] atoms = asc.atoms;
    BS bs = updateBSAtoms();
    for (int i = bs.nextSetBit(iAtomFirst); i >= 0; i = bs
        .nextSetBit(i + 1)) {
      if (!isWithinCell(ndims, atoms[i], minXYZ.x, maxXYZ.x, minXYZ.y,
          maxXYZ.y, minXYZ.z, maxXYZ.z, packingError))
        bs.clear(i);
    }
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

  private void adjustRangeMinMax(T3d[] oabc) {

    // be sure to add in packing error adjustments 
    // so that we include all needed atoms
    // load "" packed x.x supercell...
    P3d pa = new P3d();
    P3d pb = new P3d();
    P3d pc = new P3d();
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
    P3d pt = P3d.newP(oabc[0]);
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

  private void setSymmetryMinMax(P3d c) {
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

  public boolean isWithinCell(int ndims, P3d pt, double minX, double maxX,
                              double minY, double maxY, double minZ, double maxZ,
                              double slop) {
    return (pt.x > minX - slop && pt.x < maxX + slop
        && (ndims < 2 || pt.y > minY - slop && pt.y < maxY + slop) 
        && (ndims < 3 || pt.z > minZ - slop && pt.z < maxZ + slop));
  }
  
  //  /**
  //   * A problem arises when converting to JavaScript, because JavaScript numbers are all
  //   * doubles, while here we have doubles. So what we do is to multiply by a number that
  //   * is beyond the precision of our data but within the range of doubles -- namely, 100000,
  //   * and then integerize. This ensures that both doubles and doubles compare the same number.
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
  //  private boolean xxxisWithinCellInt(int dtype, P3d pt, double minX, double maxX,
  //                              double minY, double maxY, double minZ, double maxZ,
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
    int n = noSymmetryCount = asc.baseSymmetryAtomCount > 0
        ? asc.baseSymmetryAtomCount
        : bsAtoms == null ? asc.getLastAtomSetAtomCount()
            : asc.ac - bsAtoms.nextSetBit(asc.getLastAtomSetAtomIndex());
    asc.setTensors();
    applySymmetryToBonds = acr.applySymmetryToBonds;
    doPackUnitCell = acr.doPackUnitCell && !applySymmetryToBonds;
    bondCount0 = asc.bondCount;
    ndims = (int) symmetry.getUnitCellInfoType(SimpleUnitCell.INFO_DIMENSIONS);
    finalizeSymmetry(symmetry);
    int operationCount = symmetry.getSpaceGroupOperationCount();
    BS excludedOps = (acr.thisBiomolecule == null ? null : new BS());
    if (excludedOps != null)
      asc.checkSpecial = true;
    SimpleUnitCell.setMinMaxLatticeParameters(ndims, minXYZ, maxXYZ, 0);
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
    if (doCentroidUnitCell || acr.doPackUnitCell
        || symmetryRange != 0 && maxXYZ.x - minXYZ.x == 1
            && maxXYZ.y - minXYZ.y == 1 && maxXYZ.z - minXYZ.z == 1) {
      // weird Mac bug does not allow   Point3i.new3(minXYZ) !!
      minXYZ0 = P3d.new3(minXYZ.x, minXYZ.y, minXYZ.z);
      maxXYZ0 = P3d.new3(maxXYZ.x, maxXYZ.y, maxXYZ.z);
      if (ms != null) {
        ms.setMinMax0(minXYZ0, maxXYZ0);
        minXYZ.set((int) minXYZ0.x, (int) minXYZ0.y, (int) minXYZ0.z);
        maxXYZ.set((int) maxXYZ0.x, (int) maxXYZ0.y, (int) maxXYZ0.z);
      }
      switch (ndims) {
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
    int nsym = n * (latticeOnly ? 4 : operationCount);
    int cartesianCount = (asc.checkSpecial || acr.thisBiomolecule != null
        ? nsym * nCells
        : symmetryRange > 0 ? nsym // checking against {1 1 1}
            //        : symmetryRange < 0 ? 1 // checking against symop=1555 set; just a box
            : 1 // not checking
    );
    P3d[] cartesians = new P3d[cartesianCount];
    Atom[] atoms = asc.atoms;
    for (int i = 0; i < n; i++)
      atoms[firstAtom + i].bsSymmetry = BS.newN(operationCount * (nCells + 1));
    int pt = 0;
    unitCellTranslations = new V3[nCells];
    int iCell = 0;
    int cell555Count = 0;
    double absRange = Math.abs(symmetryRange);
    boolean checkCartesianRange = (symmetryRange != 0);
    boolean checkRangeNoSymmetry = (symmetryRange < 0);
    boolean checkRange111 = (symmetryRange > 0);
    if (checkCartesianRange) {
      rminx = rminy = rminz = Double.MAX_VALUE;
      rmaxx = rmaxy = rmaxz = -Double.MAX_VALUE;
    }
    // incommensurate symmetry can have lattice centering, resulting in 
    // duplication of operators. There's a bug later on that requires we 
    // only do this with the first atom set for now, at least.
    SymmetryInterface sym = symmetry;
    SymmetryInterface lastSymmetry = sym;
    checkAll = (latticeOnly
        || asc.atomSetCount == 1 && asc.checkSpecial && latticeOp >= 0);
    Lst<M4d> lstNCS = acr.lstNCS;
    if (lstNCS != null && lstNCS.get(0).m33 == 0) {
      int nOp = sym.getSpaceGroupOperationCount();
      int nn = lstNCS.size();
      for (int i = nn; --i >= 0;) {
        M4d m = lstNCS.get(i);
        m.m33 = 1;
        sym.toFractionalM(m);
      }
      for (int i = 1; i < nOp; i++) {
        M4d m1 = sym.getSpaceGroupOperation(i);
        for (int j = 0; j < nn; j++) {
          M4d m = M4d.newM4(lstNCS.get(j));
          m.mul2(m1, m);
          if (doNormalize && noSymmetryCount > 0)
            SymmetryOperation.normalizeOperationToCentroid(3, m, atoms, firstAtom, noSymmetryCount);
          lstNCS.addLast(m);
        }
      }
    }

    // always do the 555 cell first, just operation 0 -- but for incommensurate systems this may not be x,y,z

    P3d pttemp = null;
    M4d op = sym.getSpaceGroupOperation(0);
    if (doPackUnitCell) {
      pttemp = new P3d();
      ptOffset.set(0, 0, 0);
    }
    int[] atomMap = (bondCount0 > asc.bondIndex0 && applySymmetryToBonds
        ? new int[n]
        : null);
    int[] unitCells = new int[nCells];

    for (int tx = minXYZ.x; tx < maxXYZ.x; tx++) {
      for (int ty = minXYZ.y; ty < maxXYZ.y; ty++) {
        for (int tz = minXYZ.z; tz < maxXYZ.z; tz++) {
          unitCellTranslations[iCell] = V3.new3(tx, ty, tz);
          unitCells[iCell++] = 555 + tx * 100 + ty * 10 + tz;
          if (tx != 0 || ty != 0 || tz != 0 || cartesians.length == 0)
            continue;

          // base cell only
          for (pt = 0; pt < n; pt++) {
            Atom atom = atoms[firstAtom + pt];
            if (ms != null) {
              // incommensurately modulated structure
              sym = ms.getAtomSymmetry(atom, this.symmetry);
              if (sym != lastSymmetry) {
                if (sym.getSpaceGroupOperationCount() == 0)
                  finalizeSymmetry(lastSymmetry = sym);
                op = sym.getSpaceGroupOperation(0);
              }
            }
            P3d c = P3d.newP(atom);
            op.rotTrans(c);
            sym.toCartesian(c, false);
            if (doPackUnitCell) {

              // 555 cell 
              sym.toUnitCellRnd(c, ptOffset);
              pttemp.setT(c);
              sym.toFractional(pttemp, false);
              if (acr.fixJavaDouble)
                PT.fixPtDoubles(pttemp, PT.FRACTIONAL_PRECISION);
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
          cell555Count = pt = symmetryAddAtoms(0, 0, 0, 0, pt,
              iCell * operationCount, cartesians, ms, excludedOps, atomMap);
        }
      }
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
            pt = symmetryAddAtoms(tx, ty, tz, cell555Count, pt,
                iCell * operationCount, cartesians, ms, excludedOps, atomMap);
        }
    if (iCell * n == asc.ac - firstAtom)
      duplicateAtomProperties(iCell);
    setSymmetryOps();
    asc.setCurrentModelInfo("presymmetryAtomIndex", Integer.valueOf(firstAtom));
    asc.setCurrentModelInfo("presymmetryAtomCount", Integer.valueOf(n));
    asc.setCurrentModelInfo("latticeDesignation", sym.getLatticeDesignation());
    asc.setCurrentModelInfo("unitCellRange", unitCells);
    asc.setCurrentModelInfo("unitCellTranslations", unitCellTranslations);
    baseUnitCell = unitCellParams;
    unitCellParams = new double[6];
    reset();
  }

  private int symmetryAddAtoms(int transX, int transY, int transZ,
                               int baseCount, int pt, int iCellOpPt,
                               P3d[] cartesians, MSInterface ms, BS excludedOps,
                               int[] atomMap)
      throws Exception {
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

    double range2 = symmetryRange * symmetryRange;
    boolean checkRangeNoSymmetry = (symmetryRange < 0);
    boolean checkRange111 = (symmetryRange > 0);
    boolean checkSymmetryMinMax = (isBaseCell && checkRange111);
    checkRange111 &= !isBaseCell;
    int nOp = symmetry.getSpaceGroupOperationCount();
    Lst<M4d> lstNCS = acr.lstNCS;
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
    SymmetryInterface sym = symmetry;
    if (checkRangeNoSymmetry)
      baseCount = noSymmetryCount;
    int atomMax = firstAtom + noSymmetryCount;
    P3d pttemp = new P3d();
    String code = null;
    double d0 = (checkOps ? 0.01f : 0.0001f);
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
      int spinOp = (iSym >= nOp ? 0
          : asc.vibScale == 0 ? sym.getSpinOp(iSym) : asc.vibScale);
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
          sym.newSpaceGroupPoint(a, iSym,
              (iSym >= nOp ? lstNCS.get(iSym - nOp) : null), transX, transY,
              transZ, pttemp);
        } else {
          sym = ms.getAtomSymmetry(a, this.symmetry);
          sym.newSpaceGroupPoint(a, iSym, null, transX, transY, transZ, pttemp);
          // COmmensurate structures may  a symmetry operator
          // to changes space groups.
          code = sym.getSpaceGroupOperationCode(iSym);
          if (code != null) {
            subSystemId = code.charAt(0);
            sym = ms.getSymmetryFromCode(code);
            if (sym.getSpaceGroupOperationCount() == 0)
              finalizeSymmetry(sym);
          }
        }
        if (acr.fixJavaDouble)
          PT.fixPtDoubles(pttemp, PT.FRACTIONAL_PRECISION);
        P3d c = P3d.newP(pttemp); // cartesian position
        sym.toCartesian(c, false);
        if (doPackUnitCell) {
          sym.toUnitCellRnd(c, ptOffset);
          pttemp.setT(c);
          sym.toFractional(pttemp, false);
          if (acr.fixJavaDouble)
            PT.fixPtDoubles(pttemp, PT.FRACTIONAL_PRECISION);
          if (!isWithinCell(ndims, pttemp, minXYZ0.x, maxXYZ0.x, minXYZ0.y,
              maxXYZ0.y, minXYZ0.z, maxXYZ0.z, packingError))
            continue;
        }
        if (checkSymmetryMinMax)
          setSymmetryMinMax(c);
        Atom special = null;
        if (checkDistance) {
          // for range checking, we first make sure we are not out of range
          // for the cartesian
          if (checkSymmetryRange && (c.x < rminx || c.y < rminy || c.z < rminz
              || c.x > rmaxx || c.y > rmaxy || c.z > rmaxz))
            continue;
          double minDist2 = Double.MAX_VALUE;
          // checkAll means we have to check against operations that have
          // just been done; otherwise we can check only to the base set
          int j0 = (checkAll ? asc.ac : pt0);
          String name = a.atomName;
          char id = (code == null ? a.altLoc : subSystemId);
          for (int j = j00; j < j0; j++) {
            if (bsAtoms != null && !bsAtoms.get(j))
              continue;
            P3d pc = cartesians[j - firstAtom];
            if (pc == null)
              continue;
            double d2 = c.distanceSquared(pc);
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
          Atom atom1 = a.copyTo(pttemp, asc);
          if (asc.bsAtoms != null)
            asc.bsAtoms.set(atom1.index);
          if (spinOp != 0 && atom1.vib != null) {
            // spinOp is making the correction for spin being a pseudoVector, not a standard vector
            sym.getSpaceGroupOperation(iSym).rotate(atom1.vib);
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
                addRotatedTensor(atom1, t, iSym, false, sym);
            }
          }
        }
      }
      if (addBonds) {
        // Clone bonds
        Bond[] bonds = asc.bonds;
        Atom[] atoms = asc.atoms;
        String key;
        for (int bondNum = asc.bondIndex0; bondNum < bondCount0; bondNum++) {
          Bond bond = bonds[bondNum];
          Atom atom1 = atoms[bond.atomIndex1];
          Atom atom2 = atoms[bond.atomIndex2];
          if (atom1 == null || atom2 == null
              || atom2.atomSetIndex < atom1.atomSetIndex)
            continue;

          int ia1 = atomMap[atom1.atomSite];
          int ia2 = atomMap[atom2.atomSite];
          //          double d = 0;
          if (ia1 > ia2) {
            int i = ia1;
            ia1 = ia2;
            ia2 = i;
          }
          if (ia1 != ia2 && (ia1 >= atomMax || ia2 >= atomMax)
              && bondsFound.indexOf(key = "-" + ia1 + "," + ia2) < 0) {
            bondsFound.append(key);
            asc.addNewBondWithOrder(ia1, ia2, bond.order);//.distance = d;
          }
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
          double[] f = (double[]) val;
          double[] fnew = new double[f.length * nTimes];
          for (int i = nTimes; --i >= 0;)
            System.arraycopy(f, 0, fnew, i * f.length, f.length);
        }
      }
  }

  private void finalizeSymmetry(SymmetryInterface symmetry) {
    String name = (String) asc.getAtomSetAuxiliaryInfoValue(-1, "spaceGroup");
    symmetry.setFinalOperations(ndims, name, asc.atoms, firstAtom,
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

  public T3d getOverallSpan() {
    return (maxXYZ0 == null ? V3d.new3(maxXYZ.x - minXYZ.x, maxXYZ.y - minXYZ.y,
        maxXYZ.z - minXYZ.z) : V3d.newVsub(maxXYZ0, minXYZ0));
  }

  M4d mident;
  
  @SuppressWarnings("unchecked")
  public void applySymmetryBio(Map<String, Object> thisBiomolecule,
                               boolean applySymmetryToBonds, String filter) {
    Lst<M4d> biomts = (Lst<M4d>) thisBiomolecule.get("biomts");
    int len = biomts.size();
    // this was a bad idea - biomt can be just the identity matrix, in which case this is just a selection set -- 7OJP
    //    if (len < 2)
    //      return;
    if (mident == null) {
      mident = new M4d();
      mident.setIdentity();
    }
    acr.lstNCS = null; // disable NCS
    setLatticeCells();
    int[] lc = (latticeCells != null && latticeCells[0] != 0 ? new int[3]
        : null);
    if (lc != null)
      for (int i = 0; i < 3; i++)
        lc[i] = latticeCells[i];
    latticeCells = null;

    String bmChains = acr.getFilterWithCase("BMCHAINS");
    int fixBMChains = (bmChains == null ? -1
        : bmChains.length() < 2 ? 0 : PT.parseInt(bmChains.substring(1)));
    if (fixBMChains == Integer.MIN_VALUE) {
      fixBMChains = -(int) bmChains.charAt(1);
    }
    int particleMode = (filter.indexOf("BYCHAIN") >= 0 ? PARTICLE_CHAIN
        : filter.indexOf("BYSYMOP") >= 0 ? PARTICLE_SYMOP : PARTICLE_NONE);
    doNormalize = false;
    Lst<String> biomtchains = (Lst<String>) thisBiomolecule.get("chains");
    // Q: Why this? I think each biomt MUST have a chain
    //    if (biomtchains.get(0).equals(biomtchains.get(1)))
    //      biomtchains = null;
    symmetry = null;
    // it's not clear to me why you would do this:
    //if (!Double.isNaN(unitCellParams[0])) // PDB can do this; 
    //setUnitCell(unitCellParams, null, unitCellOffset);
    getSymmetry().setSpaceGroup(doNormalize);
    //symmetry.setUnitCell(null);
    addSpaceGroupOperation("x,y,z", false);
    String name = (String) thisBiomolecule.get("name");
    setAtomSetSpaceGroupName(acr.sgName = name);
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
      // ??
      len = Math.min(len,
          PT.parseInt(filter.substring(filter.indexOf("#<") + 2)) - 1);
      filter = PT.rep(filter, "#<", "_<");
    }

    int maxChain = 0;
    for (int iAtom = firstAtom; iAtom < atomMax; iAtom++) {
      atoms[iAtom].bsSymmetry = new BS(); //TODO was set 0, but this is not necessarily true now
      int chainID = atoms[iAtom].chainID;
      if (chainID > maxChain)
        maxChain = chainID;
    }
    BS bsAtoms = asc.bsAtoms;
    int[] atomMap = (addBonds ? new int[asc.ac] : null);
    // allow for filtering BIOMT number
    // len >= 2, so I don't know what is going on here -- no chains? 
    for (int imt = (biomtchains == null ? 1 : 0); imt < len; imt++) {
      if (filter.indexOf("!#") >= 0) {
        if (filter.indexOf("!#" + (imt + 1) + ";") >= 0)
          continue;
      } else if (filter.indexOf("#") >= 0
          && filter.indexOf("#" + (imt + 1) + ";") < 0) {
        continue;
      }
      M4d mat = biomts.get(imt);
      boolean notIdentity = !mat.equals(mident);

      // if asym_id is given, that is what is being referred to, not author chains
      // we just set bsAtoms to match
      String chains = (biomtchains == null ? null : biomtchains.get(imt));
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

      int lastID = -1, id;
      boolean skipping = false;
      for (int iAtom = firstAtom; iAtom < atomMax; iAtom++) {
        if (bsAtoms != null) {
          skipping = !bsAtoms.get(iAtom);
        } else if (chains != null 
            && (id = atoms[iAtom].chainID) != lastID) {
          skipping = (chains
              .indexOf(":" + acr.vwr.getChainIDStr(lastID = id) + ";") < 0);
        }
        if (skipping)
          continue;
        try {
          int atomSite = atoms[iAtom].atomSite;
          Atom atom1;
          if (addBonds)
            atomMap[atomSite] = asc.ac;
          atom1 = asc.newCloneAtom(atoms[iAtom]);
          atom1.bondingRadius = imt; // temporary only -- to distinguish transforms
          asc.atomSymbolicMap.put("" + atom1.atomSerial, atom1);
          if (asc.bsAtoms != null)
            asc.bsAtoms.set(atom1.index);
          atom1.atomSite = atomSite;
          if (notIdentity)
            mat.rotTrans(atom1);
          atom1.bsSymmetry = BSUtil.newAndSetBit(imt);
        } catch (Exception e) {
          asc.errorMessage = "appendAtomCollection error: " + e;
        }
      }
      // not the identity, which is always the first entry (set by Jmol in reader)
      // symmetry always has first operation x,y,z
      if (notIdentity)
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
    if (biomtchains != null) {
      if (asc.bsAtoms == null)
        asc.bsAtoms = BSUtil.newBitSet2(0, asc.ac);
      asc.bsAtoms.clearBits(firstAtom, atomMax);
      if (particleMode == PARTICLE_NONE) {
        // check for BMCHAINS filter 
        if (fixBMChains != -1) {
          boolean assignABC = (fixBMChains != 0);
          BS bsChains = (assignABC ? new BS() : null);
          atoms = asc.atoms;
          int firstNew = 0;
          if (assignABC) {
            // tested for 1k28 and 1auy
            //For PDB and mmCIF, convert chains for symmetry-generated atoms to unique
            // ids. Three options:
            // bmChains or bmChains=0: append symmetry operator number to chain ID
            // bmChains=q: start with 'q'
            // bmChains= 3 : start with last chain ID + 3
            firstNew = (fixBMChains < 0 ? Math.max(-fixBMChains, maxChain + 1)
                : Math.max(maxChain + fixBMChains, 0 + 'A'));
            bsChains.setBits(0, firstNew - 1);
            bsChains.setBits(1 + 'Z', 0 + 'a');
            bsChains.setBits(1 + 'z', 200);
          }
          BS[] bsAll = (asc.structureCount == 1 ? asc.structures[0].bsAll
              : null);
          Map<String, Integer> chainMap = new Hashtable<String, Integer>();
          Map<Integer, Integer> knownMap = new Hashtable<Integer, Integer>();
          Map<Integer, int[]> knownAtomMap = (bsAll == null ? null
              : new Hashtable<Integer, int[]>());
          int[] lastKnownAtom = null;
          for (int i = atomMax, n = asc.ac; i < n; i++) {
            int ic = atoms[i].chainID;
            int isym = atoms[i].bsSymmetry.nextSetBit(0);
            String ch0 = acr.vwr.getChainIDStr(ic);
            String ch = (isym == 0 ? ch0 : ch0 + isym);
            Integer known = chainMap.get(ch);
            if (known == null) {
              if (assignABC && isym != 0) {
                int pt = (firstNew < 200 ? bsChains.nextClearBit(firstNew)
                    : 200);
                if (pt < 200) {
                  bsChains.set(pt);
                  // have A-Z or a-z
                  known = Integer
                      .valueOf(acr.vwr.getChainID("" + (char) pt, true));
                  firstNew = pt;
                } else {
                  // 1auy will do this
                }
              }
              if (known == null)
                known = Integer.valueOf(acr.vwr.getChainID(ch, true));
              if (ch != ch0) {
                knownMap.put(known, Integer.valueOf(ic));
                if (bsAll != null) {
                  if (lastKnownAtom != null)
                    lastKnownAtom[1] = i;
                  knownAtomMap.put(known, lastKnownAtom = new int[] { i, n });
                }
              }
              chainMap.put(ch, known);
            }
            atoms[i].chainID = known.intValue();
          }
          if (asc.structureCount > 0) {
            // update structures
            Structure[] strucs = asc.structures;
            int nStruc = asc.structureCount;
            for (Entry<Integer, Integer> e : knownMap.entrySet()) {
              Integer known = e.getKey();
              int ch1 = known.intValue();
              int ch0 = e.getValue().intValue();
              for (int i = 0; i < nStruc; i++) {
                Structure s = strucs[i];
                if (s.bsAll != null) {
                  // MMTFReader processes bsAll[] in addStructureSymmetry()
                } else if (s.startChainID == s.endChainID) {
                  if (s.startChainID == ch0) {
                    Structure s1 = s.clone();
                    s1.startChainID = s1.endChainID = ch1;
                    asc.addStructure(s1);
                  }
                } else {
                  System.err.println(
                      "XtalSymmetry not processing biomt chain structure "
                          + acr.vwr.getChainIDStr(ch0) + " to "
                          + acr.vwr.getChainIDStr(ch1));
                  // TODO now what??
                }
              }

            }
          }
        }

        // clean interchain CONECT 
        Lst<int[]> vConnect = (Lst<int[]>) asc.getAtomSetAuxiliaryInfoValue(-1,
            "PDB_CONECT_bonds");
        if (!addBonds && vConnect != null) {
          for (int i = vConnect.size(); --i >= 0;) {
            int[] bond = vConnect.get(i);
            Atom a = asc.getAtomFromName("" + bond[0]);
            Atom b = asc.getAtomFromName("" + bond[1]);
            // bondingRadius here just being used for BIOMT 
            if (a != null && b != null && a.bondingRadius != b.bondingRadius
                && (bsAtoms == null
                    || bsAtoms.get(a.index) && bsAtoms.get(b.index))
                && a.distanceSquared(b) > MAX_INTERCHAIN_BOND_2) {
              vConnect.removeItemAt(i);
              System.out.println("long interchain bond removed for @"
                  + a.atomSerial + "-@" + b.atomSerial);
            }
          }
        }
      }
      // reset bondingRadius to NaN
      for (int i = atomMax, n = asc.ac; i < n; i++) {
        asc.atoms[i].bondingRadius = Float.NaN;
      }

    }
    noSymmetryCount = atomMax - firstAtom;
    asc.setCurrentModelInfo("presymmetryAtomIndex", Integer.valueOf(firstAtom));
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

  public Tensor addRotatedTensor(Atom a, Tensor t, int iSym, boolean reset,
                                 SymmetryInterface symmetry) {
    if (ptTemp == null) {
      ptTemp = new P3d();
      mTemp = new M3d();
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
      if (Double.isNaN(a.bfactor))
        a.bfactor = a.anisoBorU[7] * 100f;
      // prevent multiple additions
      a.anisoBorU = null;
    }
  }

  public void setTimeReversal(int op, int timeRev) {
    symmetry.setTimeReversal(op, timeRev);
  }

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
          v = (Vibration) v.clone(); // this could be a modulation set
          sym.toCartesianF(v, true);
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
    double[] params = getBaseSymmetry().getUnitCellParams();
    P3 ptScale = P3.new3(1 / (float) params[0], 1 / (float) params[1], 1 / (float) params[2]);
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
  public void finalizeUnitCell(P3d ptSupercell) {
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
