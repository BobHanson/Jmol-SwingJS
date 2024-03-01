package org.jmol.symmetry;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

import org.jmol.api.SymmetryInterface;
import org.jmol.modelset.Atom;
import org.jmol.util.BSUtil;
import org.jmol.util.SimpleUnitCell;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.T3d;

/**
 * A relatively simple space group finder given a unit cell.
 * The unit cell is used to reduce the options involving hexagonal, tetragonal, and cubic groups.
 * 
 * See https://stokes.byu.edu/iso/findsymform.php for finding the space group
 * given no unit cell information.
 * 
 */
public class SpaceGroupFinder {

  /**
   * maximum allowable supercell
   */
  private static final int MAX_COUNT = 100;


//  /**
//   * maximum allowable distance fx, fy ,fz for atom finder
//   */
//  private static final double SLOP0014 = Math.sqrt(JC.UC_TOLERANCE2);
//
//  /**
//   * tolerance for fractional coord and 
//   */
//  private final static double //SLOP02 = 0.02f, 
//      SLOP001 = 0.001, SLOP0001= 0.0001;  // 0.001 was too tight for labradorite Si // 0.0001 was too tight here. 

  private static int GROUP_COUNT; // 531
  private static int OP_COUNT; // 882
  private static BS[] bsOpGroups;
  private static BS[] bsGroupOps;
  private static String[] groupNames;
  private static String[] opXYZ;
  private static SymmetryOperation[] ops;

  private static BufferedReader rdr = null;

  private SGAtom[] atoms;
  private int nAtoms;
  private P3d pt = new P3d();
  private double slop;

  public SpaceGroupFinder() {
  }

  private class SGAtom extends P3d {
    int typeAndOcc;
    int index;
    String name;

    SGAtom(int type, int index, String name, int occupancy) {
      this.typeAndOcc = type + 1000 * occupancy;
      this.index = index;
      this.name = name;
    }
  }

  /**
   * 
   * @param vwr
   * @param atoms0
   * @param xyzList
   * @param unitCellParams
   * @param origin TODO
   * @param uc
   * @param asString
   * @param isAssign
   * @param checkSupercell 
   * @return SpaceGroup or null if isAssign, spacegroup information map if
   */
  @SuppressWarnings("unchecked")
  Object findSpaceGroup(Viewer vwr, BS atoms0, String xyzList,
                        double[] unitCellParams, T3d origin,
                        SymmetryInterface uc, boolean asString, boolean isAssign, boolean checkSupercell) {
    double slop = uc.getPrecision();
    this.slop = (!Double.isNaN(slop) ? slop
        : unitCellParams != null  && unitCellParams.length > SimpleUnitCell.PARAM_SLOP ? unitCellParams[SimpleUnitCell.PARAM_SLOP]
            : Viewer.isDoublePrecision ? SimpleUnitCell.SLOPDP
                : SimpleUnitCell.SLOPSP);
    P3d[] oabc = null;
    Atom[] cartesians = vwr.ms.at;
    int isg = 0;
    BS bsGroups = new BS();
    BS bsOps = new BS();
    BS bsAtoms = null;
    BS bsPoints0 = new BS();
    if (xyzList == null || isAssign) {
      bsAtoms = BSUtil.copy(atoms0);
      nAtoms = bsAtoms.cardinality();
    }
    BS targets = BS.newN(nAtoms);
    int n = 0;
    int nChecked = 0;
    // this will be set in checkSupercell
    P3d scaling = P3d.new3(1, 1, 1);
    //    BS withinCell = null;
    SpaceGroup sg = null;

    boolean setNew = (isAssign && xyzList != null && nAtoms == 0
        && (uc.getSpaceGroup() == null
            || "P1".equals(((SpaceGroup) uc.getSpaceGroup()).getName())));

    String name;
    BS basis;
    if (xyzList != null && xyzList.toUpperCase().startsWith("ITA/")) {
      xyzList = PT.rep(xyzList.substring(4), " ", "");
      boolean isJmolCode = (xyzList.indexOf(":") > 0);
      int pt = xyzList.indexOf(".");
      if (!isJmolCode && pt < 0 && PT.parseInt(xyzList) != Integer.MIN_VALUE)
        xyzList += ".1";
      Map<String, Object> sgdata = null;
      Object o = uc.getSpaceGroupJSON(vwr, "ITA", xyzList, 0);
      if (o == null || o instanceof String) {
        return null;
      }
      sgdata = (Map<String, Object>) o;
      if (isJmolCode) {
        name = xyzList;
        Lst<Object> its = (Lst<Object>) sgdata.get("its");
        sgdata = null;
        if (its == null)
          return null;
        for (int i = 0, c = its.size(); i < c; i++) {
          Map<String, Object> setting = (Map<String, Object>) its.get(i);
          if (name.equals(setting.get("itaFull"))) {
            sgdata = setting;
            break;
          }
        }
        if (sgdata == null)
          return null;
      } else {
        name = (String) sgdata.get("itaFull");
      }
      boolean isKnown = (name.indexOf("?") < 0);
      Lst<Object> genPos = (Lst<Object>) sgdata.get("gp");
      xyzList = "";
      for (int i = 0, c = genPos.size(); i < c; i++)
        xyzList += ";" + (String) genPos.get(i);
      xyzList = xyzList.substring(1);
      sg = SpaceGroup.createSpaceGroupN(xyzList);
      sg.intlTableNumber = name;
      SpaceGroup sgjmol = null;
      if (isKnown) {
        sgjmol = SpaceGroup.determineSpaceGroupNA(name, null);
        if (sgjmol != null) {
          sg = sgjmol.cloneInfoTo(sg);
        } else {
          sg.setIntlTableNumberFull(name);
        }
      }
      if (sgjmol == null) {
        String u = (String) sgdata.get("u");
        String tr = (String) sgdata.get("tm");
        sg.intlTableNumberExt = PT.rep(u, " ", "") + ";" + sgdata.get("sg")
            + "(" + tr + ")";
        char axis = u.toLowerCase().charAt(0);
        if (UnitCell.isHexagonalSG(PT.parseInt(sg.intlTableNumber), null)
            && axis != 'r')
          axis = 'h';
        switch (axis) {
        case 'a':
        case 'b':
        case 'c':
        case 'r':
        case 'h':
          sg.axisChoice = axis;
          break;
        }
      }
    }
    boolean isSupercell = false;
    if (setNew) {
      if (sg == null
          && (sg = SpaceGroup.determineSpaceGroupNA(xyzList,
              unitCellParams)) == null
          && (sg = SpaceGroup.createSpaceGroupN(xyzList)) == null)
        return null;
      uc = createCompatibleUnitCell(sg, unitCellParams);
      basis = new BS();
      name = sg.asString();
      oabc = uc.getUnitCellVectors();
      if (origin != null)
        oabc[0].setT(origin);
    } else {
      try {
        if (bsOpGroups == null)
          loadData(vwr, this);
        if (xyzList != null) {
          Object ret = getGroupsWithOps(xyzList, unitCellParams, isAssign);
          if (!isAssign || ret == null)
            return ret;
          sg = (SpaceGroup) ret;
          uc.setUnitCellFromParams(unitCellParams, false, slop);
        }
        oabc = uc.getUnitCellVectors();
        uc = uc.getUnitCellMultiplied();
        if (origin != null) {
          oabc[0].setT(origin);
          uc.setOffsetPt(origin);
        }
        filterGroups(bsGroups, uc.getUnitCellParams());

        //      withinCell = vwr.ms.getAtoms(T.unitcell, uc);
        //BS extraAtoms = BSUtil.copy(bsAtoms);
        //extraAtoms.andNot(withinCell);
        //      withinCell.and(bsAtoms);
        //int nExtra = extraAtoms.cardinality();

        // 1. Get ALL atoms.

        atoms = new SGAtom[bsAtoms.cardinality()];
        System.out.println("bsAtoms = " + bsAtoms);
        for (int p = 0, i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms
            .nextSetBit(i + 1), p++) {
          Atom a = cartesians[i];
          int type = a.getAtomicAndIsotopeNumber();
          (atoms[p] = new SGAtom(type, i, a.getAtomName(), a.getOccupancy100()))
              .setT(toFractional(a, uc));

        }
        BS bsPoints = BSUtil.newBitSet2(0, nAtoms);

        // Look out for tetrgonal aac issue -- abandoned

        SymmetryInterface uc0 = uc;
        //      M3d mtet = new M3d();
        //      uc = checkTetragonal(vwr, uc, mtet);
        //      if (uc == uc0) {
        //        mtet = null;
        //      } else {
        //System.out.println("tetragonoal setting issue detected");
        //      }

        //      // 2. Check that packing atoms, if any, are complete and only those packed. -- abandoned
        // decided this was unnecessary. We can just unitize and remove duplicates

        //      if (nExtra > 0) {
        //        BS packedAtoms = new BS();
        //        checkPackingAtoms(bsPoints, extraAtoms, 1, packedAtoms);
        //        checkPackingAtoms(bsPoints, extraAtoms, 2, packedAtoms);
        //        checkPackingAtoms(bsPoints, extraAtoms, 3, packedAtoms);
        //        if (packedAtoms.cardinality() == nExtra) {
        //          bsPoints.andNot(packedAtoms);
        //        } else {
        //System.out.println("Packing check failed! " + nExtra + " extra atoms " + extraAtoms + ", but " + packedAtoms.cardinality() + " found for " + packedAtoms);
        //          bsPoints.clearAll();
        //        }
        //      }

        // 3. Unitize, remove duplicates, and check for supercells in each direction

        nAtoms = bsPoints.cardinality();
        uc0 = uc;
        if (nAtoms > 0) {
          for (int i = bsPoints.nextSetBit(0); i >= 0; i = bsPoints
              .nextSetBit(i + 1)) {
            uc.unitize(atoms[i]);
          }
          removeDuplicates(bsPoints);
          // really not what we want here
          if (checkSupercell) {
            uc = checkSupercell(vwr, uc, bsPoints, 1, scaling);
            uc = checkSupercell(vwr, uc, bsPoints, 2, scaling);
            uc = checkSupercell(vwr, uc, bsPoints, 3, scaling);
            isSupercell = (uc != uc0);
            if (isSupercell) {
              if (scaling.x != 1)
                System.out
                    .println("supercell found; a scaled by 1/" + scaling.x);
              if (scaling.y != 1)
                System.out
                    .println("supercell found; b scaled by 1/" + scaling.y);
              if (scaling.z != 1)
                System.out
                    .println("supercell found; c scaled by 1/" + scaling.z);
            }
          }
        }

        // 4. Remove unneeded atoms and recalculate fractional position if a supercell

        n = bsPoints.cardinality();
        bsAtoms = new BS();
        SGAtom[] newAtoms = new SGAtom[n];
        for (int p = 0, i = bsPoints.nextSetBit(0); i >= 0; i = bsPoints
            .nextSetBit(i + 1)) {
          SGAtom a = atoms[i];
          newAtoms[p++] = a;
          if (isSupercell) {
            a.setT(toFractional(cartesians[a.index], uc));
            uc.unitize(a);
            //          if (mtet != null)
            //            mtet.rotate(a);
          }
          bsAtoms.set(atoms[i].index);
        }

        atoms = newAtoms;
        nAtoms = n;
        System.out.println("bsAtoms(within cell) = " + bsAtoms);
        bsPoints.clearAll();
        bsPoints.setBits(0, nAtoms);
        bsPoints0 = BS.copy(bsPoints);
        BS temp1 = BS.newN(OP_COUNT);
        BS targeted = BS.newN(nAtoms);

        bsOps.setBits(1, sg == null ? OP_COUNT : sg.getOperationCount());
        if (nAtoms == 0) {
          bsGroups.clearBits(1, GROUP_COUNT);
          bsOps.clearAll();
        }

        // 5. Adaptively iterate over all known operations.

        BS uncheckedOps = BS.newN(OP_COUNT);
        BS opsChecked = BS.newN(OP_COUNT);
        opsChecked.set(0);
        boolean hasC1 = false;
        for (int iop = bsOps.nextSetBit(1); iop > 0
            && !bsGroups.isEmpty(); iop = bsOps.nextSetBit(iop + 1)) {
          SymmetryOperation op = (sg == null ? getOp(iop)
              : (SymmetryOperation) sg.getOperation(iop));
          if (sg == null) {
            System.out
                .println("\nChecking operation " + iop + " " + opXYZ[iop]);
            System.out.println("bsGroups = " + bsGroups);
            System.out.println("bsOps = " + bsOps);
            nChecked++;
          }
          boolean isOK = true;
          bsPoints.clearAll();
          bsPoints.or(bsPoints0);
          targeted.clearAll();
          for (int i = bsPoints.nextSetBit(0); i >= 0; i = bsPoints
              .nextSetBit(i + 1)) {
            bsPoints.clear(i);
            //          if (iop == 15 && i == 46)
            //System.out.println("????");
            int j = findEquiv(uc, iop, op, i, bsPoints, pt, true);
            if (j < 0 && sg == null) {
              System.out.println(
                  "failed op " + iop + " for atom " + i + " " + atoms[i].name
                      + " " + atoms[i] + " looking for " + pt + "\n" + op);
              isOK = false;
              break;
            }
            if (j >= 0 && i != j) {
              targeted.set(j);
            }
          }
          if (sg == null) {
            BS myGroups = bsOpGroups[iop];
            bsOps.clear(iop);
            opsChecked.set(iop);
            if (isOK) {
              if (iop == 1)
                hasC1 = true;
              // iop was found
              targets.or(targeted);
              //System.out.println("targeted=" + targeted);
              //System.out.println("targets=" + targets);
              //reduce the number of possible groups to groups having this operation
              bsGroups.and(myGroups);
              // reduce the number of operations to check to only those NOT common to 
              // all remaining groups;
              temp1.setBits(1, OP_COUNT);
              for (int i = bsGroups.nextSetBit(0); i >= 0; i = bsGroups
                  .nextSetBit(i + 1)) {
                temp1.and(bsGroupOps[i]);
              }
              uncheckedOps.or(temp1);
              bsOps.andNot(temp1);
            } else {
              // iop was not found
              // clear all groups that require this operation
              bsGroups.andNot(myGroups);
              // trim ops to only those needed for compatible groups
              // and retain only operations for groups that have this operation
              temp1.clearAll();
              for (int i = bsGroups.nextSetBit(0); i >= 0; i = bsGroups
                  .nextSetBit(i + 1)) {
                temp1.or(bsGroupOps[i]);
              }
              bsOps.and(temp1);
            }
          } else {
            targets.or(targeted);
          }
        }
        if (sg == null) {
          n = bsGroups.cardinality();
          if (n == 0) {
            bsGroups.set(hasC1 ? 1 : 0);
            n = 1;
            if (hasC1 && !asString) {
              uncheckedOps.clearAll();
              uncheckedOps.set(1);
              opsChecked.clearAll();
              targets.clearAll();
              bsPoints.or(bsPoints0);
            }
          }
          isg = bsGroups.nextSetBit(0);
          if (n == 1) {
            if (isg > 0) {
              opsChecked.and(bsGroupOps[isg]);
              uncheckedOps.and(bsGroupOps[isg]);
              uncheckedOps.andNot(opsChecked);
              uncheckedOps.or(bsGroupOps[isg]);
              uncheckedOps.clear(0);
              bsPoints.or(bsPoints0);
              //System.out.println("test1 bspoints " + bsPoints.cardinality() + " " + bsPoints);
              //System.out.println("test2 targets " + targets.cardinality() + " " + targets);
              //System.out.println("test3 uchop= " + uncheckedOps.cardinality() + " " + uncheckedOps);
              //System.out.println("test4 opsc= " + opsChecked.cardinality() + " " + opsChecked);
              bsPoints.andNot(targets);
              if (!checkBasis(uc, uncheckedOps, bsPoints, targets)) {
                isg = 0;
              }
              //System.out.println("test2b targets " + targets.cardinality() + " " + targets);
              //System.out.println("test1b points " + bsPoints.cardinality() + " " + bsPoints);
            }
            if (isg == 0)
              targets.clearAll();
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        bsGroups.clearAll();
      }

      if (sg == null) {
        System.out.println("checked " + nChecked + " operations; now " + n + " "
            + bsGroups + " " + bsOps);
        for (int i = bsGroups.nextSetBit(0); i >= 0; i = bsGroups
            .nextSetBit(i + 1)) {
          System.out.println(SpaceGroup.nameToGroup.get(groupNames[i]));
        }
        if (n != 1)
          return null;
        sg = SpaceGroup.nameToGroup.get(groupNames[isg]);
      }
      name = sg.asString();
      basis = BSUtil.copy(bsAtoms);
      for (int i = targets.nextSetBit(0); i >= 0; i = targets.nextSetBit(i + 1))
        basis.clear(atoms[i].index);
      int nb = basis.cardinality();
      String msg = name + "\nbasis is " + nb + " atom" + (nb == 1 ? "" : "s")
          + ": " + basis;
      System.out.println(msg);
      if (asString)
        return msg;
    }

    Map<String, Object> map = (Map<String, Object>) sg.dumpInfoObj();
    System.out.println("unitcell is " + uc.getUnitCellInfo(true));
    BS bs1 = BS.copy(bsPoints0);
    bs1.andNot(targets);
    if (!isAssign)
      dumpBasis(bsGroupOps[isg], bs1, bsPoints0);
    map.put("name", name);
    map.put("basis", basis);
    if (isSupercell)
      map.put("supercell", scaling);
    oabc[1].scale(1/scaling.x);
    oabc[2].scale(1/scaling.y);
    oabc[3].scale(1/scaling.z);
    map.put("unitcell", oabc);
    if (isAssign)
      map.put("sg", sg);
    return map;
  }

  private SymmetryInterface createCompatibleUnitCell(SpaceGroup sg,
                                                            double[] params) {
    double[] newParams = new double[6];
    UnitCell.createCompatibleUnitCell(sg, params, newParams, false);
    SymmetryInterface sym = new Symmetry().setUnitCellFromParams(newParams, false, Double.NaN);
    sym.setSpaceGroupTo(sg);
    return sym;
  }

  private void removeDuplicates(BS bs) {
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      SGAtom a = atoms[i];
      for (int j = bs.nextSetBit(0); j < i; j = bs.nextSetBit(j + 1)) {
        SGAtom b = atoms[j];
        if (a.typeAndOcc == b.typeAndOcc && a.distanceSquared(b) < JC.UC_TOLERANCE2) {
          bs.clear(i);
          break;
        }
      }
      
    }
  }

  @SuppressWarnings("unused")
  private void dumpBasis(BS ops, BS bs1, BS bsPoints) {
//System.out.println("----");
//    for (int i = bs1.nextSetBit(0); i >= 0; i = bs1.nextSetBit(i + 1)) {
//      for (int iop = ops.nextSetBit(1); iop >= 0; iop = ops
//          .nextSetBit(iop + 1)) {
//        SymmetryOperation op = getOp(iop);
//        System.out.print(i + ":" + iop + " = ");
//        int i0 = i;
//        int j = -1;
//        while (j != i) {
//          j = findEquiv(-1, op, i0, bsPoints, pt, false);
//          if (j == i)
//            break;
//          System.out.print(" " + j);
//          i0 = j;
//        }
//        System.out.print("\n");
//System.out.println("----");
  }

  private boolean checkBasis(SymmetryInterface uc, BS uncheckedOps, BS bsPoints, BS targets) {
	  
    int n = uncheckedOps.cardinality();
    if (n == 0)
      return true;
    BS bs = new BS();
    bs.or(bsPoints);
    System.out.println("finishing check for basis for " + n + " operations");
    for (int iop = uncheckedOps.nextSetBit(0); iop >= 0; iop = uncheckedOps
        .nextSetBit(iop + 1)) {
//      if (bs.isEmpty())
//        return;
// added
      bs.or(bsPoints);
      SymmetryOperation op = getOp(iop);
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        int j = findEquiv(uc, -1, op, i, bs, pt, false);
        if (j < 0)
          return false;
        if (i != j) {
          j = Math.max(i, j);
          targets.set(j);
          bs.clear(j);
        }
      }
    }
    return true;
  }
//  private boolean checkBasis(BS uncheckedOps, BS bs, BS targets) {
//    int n = uncheckedOps.cardinality();
//    if (n == 0)
//      return true;
//    int[] basis = new int[nAtoms];
//    for (int i = nAtoms; --i >= 0;)
//      basis[i] = Integer.MAX_VALUE;
//System.out.println("finishing check for basis for " + n + " operations");
//    bs.setBits(0, nAtoms);
//System.out.println(
//        "checkBasis " + bs.cardinality() + " " + targets.cardinality() + " " + bs);
//    for (int iop = uncheckedOps.nextSetBit(1); iop >= 0; iop = uncheckedOps
//        .nextSetBit(iop + 1)) {
//      SymmetryOperation op = getOp(iop);
//      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
//        int j = findEquiv(-1, op, i, bs, pt, false);
//        if (j < 0)
//          return false;
//        if (i == 12 || j == 12)
//System.out.println("??" + op + iop + " " + i + " " + j + atoms[i] + atoms[j]);
//          if (i < basis[j]) {
//            basis[j] = i;
//          } else if (j < basis[i]) {
//              basis[i] = j;
//          }
//      }
//    }
//    BS bsnew = new BS();
//    for (int i = 0; i < nAtoms; i++) {
//      if (basis[i] >= i)
//        bsnew.set(i);
//    }
//    bs.and(bsnew);
//    bs.andNot(targets);
//    targets.setBits(0, nAtoms);
//    targets.andNot(bs);
//System.out.println(
//        "checkBasis " + bs.cardinality() + " " + targets.cardinality() + " " + bs);
//    return true;
//  }


  /**
   * Remove possibilities based on unit cells. No attempt is made to permute
   * axes. Rhombohedral groups are never included.
   * 
   * triclinic = 1, 2
   * 
   * monoclinic = 3 - 15
   * 
   * orthorhombic = 16 - 74 (a, b, c, 90, 90, 90)
   * 
   * tetragonal = 75 - 142 (a, a, c, 90, 90, 90)
   * 
   * trigonal = 143 - 194 (a, a, c, 90, 90, 120)
   * 
   * cubic = 195 - 230 (a, a, a, 90, 90, 90)
   * 
   * 
   * @param bsGroups
   * @param params
   */
  private void filterGroups(BS bsGroups, double[] params) {
    boolean isOrtho = false, isTet = false, isTri = false,
        isRhombo = false, isCubic = false;
    boolean absame = approx001(params[0] - params[1]);
    boolean bcsame = approx001(params[1] - params[2]);
    if (params[3] == 90) { // alpha
      if (params[4] == 90) { // beta
        // really?? 
        if (absame && params[0] != params[1]) 
          System.out.println("OHOH"); 
        // stoltzite.cif?? 
        //        _cell_length_a             5.4450502
        //        _cell_length_b             5.44503
        //         _space_group_IT_number     88

        // Tausonite.cif (Ok)
        //       _cell_length_a             5.4784
        //       _cell_length_b             5.4791
        //       _space_group_IT_number     62


        isTri = (absame && approx001(params[5] - 120));
        if (params[5] == 90) { // gamma
          isCubic = (absame && params[1] == params[2]);
          isTet = (!isCubic && absame);
          isOrtho = (!isCubic && !isTet);
        }
      }
    } else if (absame && bcsame
        && approx001(params[3] - params[4])  && approx001(params[4] - params[5])) {
      isRhombo = true;
    }
    bsGroups.setBits(0, 2);
    int i0 = 2, i = 2;
    while (true) {
      i = scanTo(i, "16");
      if (!isOrtho && !isTet && !isTri && !isRhombo && !isCubic)
        break;

      i = scanTo(i, "75");
      if (!isTet && !isTri && !isRhombo && !isCubic)
        break;

      i = scanTo(i, "143");
      if (!isTri && !isRhombo && !isCubic)
        break;

      i0 = i;
      for (;; i++) {
        String g = groupNames[i];
        if (g.indexOf(":r") >= 0) {
          if (!isRhombo)
            continue;
          bsGroups.set(i);
        }
        if (g.startsWith("195")) {
          if (isRhombo)
            return;
          break;
        }
      }
      if (!isCubic)
        break;
      bsGroups.setBits(2, i0);
      i0 = i;
      i = GROUP_COUNT;
      break;
    }
    bsGroups.setBits(i0, i);
  }

  private boolean approx001(double d) {
    return Math.abs(d) < SimpleUnitCell.SLOP_PARAMS;
  }

  private static int scanTo(int i, String num) {
    for (;; i++) {
      if (groupNames[i].startsWith(num))
        break;
    }
    return i;
  }

  /**
   * Find all space groups that match EXACTLY or contain all of the operations
   * specified. Operation need not be in Jmol-canonical form. For instance,
   * "1-x,-y,-z" will be matched to "-x,-y,-z".
   *
   * 
   * Could be the result of spacegroup(nameOrXYZList, unitcellParametersArray)
   * or MODELKIT ASSIGN SPACEGROUP "nameOrXYZList" other than P1
   * 
   * @param xyzList
   *        a semicolon-separated list of possible space groups, such as
   *        "-x,-y,-z;x,-y,-z" or a space group ID such as "133:2"; if a list,
   *        prefixing or postfixing the list with "&" or joining with "&"
   *        indicates that a partial match is desired, returning a list of names
   *        of space groups that have at least the specified operations
   * @param unitCellParams
   * @param isAssign
   *        from ModelKit
   * @return an array of space group IDs if not isAssign and not "="; a
   *         SpaceGroup or null if isAssign; a single space group ID as a string
   *         if "=" and a string starting and ending with "?" if an xyz operator
   *         is of an invalid form.
   */
  private Object getGroupsWithOps(String xyzList, double[] unitCellParams, boolean isAssign) {
    BS groups = new BS();
    if (unitCellParams == null) {
      groups.setBits(0,  GROUP_COUNT);
    } else {
      filterGroups(groups, unitCellParams);
    }
    SpaceGroup sg = null;
    if (!SpaceGroup.isXYZList(xyzList)) {
      // space group name -- here we return a space group if unit cell symmetry is allowed
      sg = SpaceGroup.determineSpaceGroupNA(xyzList, unitCellParams);
      if (sg == null)
        return null;
      sg.checkHallOperators();
      // check for appropriate unit cell
      for (int i = 0; i < GROUP_COUNT; i++)
        if (groupNames[i].equals(sg.intlTableNumberFull))
          return (groups.get(i) ? sg : null);
      return null;
    }
    // xyz list only, possibly starting with "&" for partial match
    boolean isEqual = xyzList.indexOf("&") < 0 || isAssign;
    String[] ops = PT.split(PT.trim(xyzList.trim().replace('&', ';'), ";="), ";");
    for (int j = ops.length; --j >= 0;) {
      String xyz = ops[j];
      if (xyz == null)// || xyz.indexOf("z") < xyz.lastIndexOf(","))
        return "?" + ops[j] + "?";
      xyz = SymmetryOperation.getJmolCanonicalXYZ(xyz);
      for (int i = opXYZ.length; --i >= 0;) {
        if (opXYZ[i].equals(xyz)) {
          groups.and(bsOpGroups[i]);
          break;
        }
        // check for not found
        if (i == 0)
          groups.clearAll();
      }
    }
    if (groups.isEmpty()) {
      // if assigning, we must assume that this is a non-standard unknown setting 
      // and the unit cell is OK. We just define the space group as is.
      return (isAssign ? SpaceGroup.createSpaceGroupN(xyzList) : null);
    }
    if (isEqual) {
      for (int n = ops.length, i = groups.nextSetBit(0); i >= 0; i = groups
          .nextSetBit(i + 1)) {
        if (bsGroupOps[i].cardinality() == n) {
          if (isAssign) {
            return SpaceGroup.createSpaceGroupN(groupNames[i]);
          }
          return SpaceGroup.getInfo(null, groupNames[i], unitCellParams, true, false);
        } 
      }
      return null;
    }
    // at this point, the group has cardinality of at least 1 
    String[] ret = new String[groups.cardinality()];
    for (int p = 0, i = groups.nextSetBit(0); i >= 0; i = groups
        .nextSetBit(i + 1)) {
      ret[p++] = groupNames[i];
    }
    return ret;
  }

//  private SymmetryInterface checkTetragonal(Viewer vwr, SymmetryInterface uc, M3d mtet) {    
//    double[] params = uc.getUnitCellParams();
//    if (!approx0(params[0] - params[1])
//        && approx0(params[1] - params[2])
//        && approx0(params[3] - 90) && approx0(params[4] - 90)
//        && approx0(params[5] - 90)) {
//      // b==c, so a->c->b->a
//      mtet.setA(new double[] {0, 1, 0, 0, 0, 1, 1, 0, 0});
//      for (int i = 0; i < nAtoms; i++) {
//        mtet.rotate(atoms[i]);
//      }
//      P3[] oabc = uc.getUnitCellVectors();
//      uc = vwr.getSymTemp().getUnitCell(new P3[] {oabc[0], oabc[2], oabc[3], oabc[1]}, false, "permuted");
//    } else if (!approx0(params[0] - params[1])
//        && approx0(params[0] - params[2])
//        && approx0(params[3] - 90) && approx0(params[4] - 90)
//        && approx0(params[5] - 90)) {
//      // a==c, so b->c->a->b
//      mtet.setA(new double[] {1, 0, 0, 0, 0, 1, 0, 1, 0});
//      for (int i = 0; i < nAtoms; i++) {
//        mtet.rotate(atoms[i]);
//      }
//      P3[] oabc = uc.getUnitCellVectors();
//      uc = vwr.getSymTemp().getUnitCell(new P3[] {oabc[0], oabc[1], oabc[3], oabc[2]}, false, "permuted");
//    }
//    return uc;
//  }

  P3d toFractional(Atom a, SymmetryInterface uc) {
    pt.setT(a);
    uc.toFractional(pt, false);
    return pt;
  }

  private static SymmetryOperation getOp(int iop) {
    SymmetryOperation op = ops[iop];
    if (op == null) {
      ops[iop] = op = new SymmetryOperation(null, iop, false);
      op.setMatrixFromXYZ(opXYZ[iop], 0, false);
      op.doFinalize();
    }
    return op;
  }

  /**
   * Look for a supercell and adjust lattice down if necessary.

   * @param vwr 
   * @param uc
   * @param bsPoints
   * @param abc
   *        1==a, 2==b, 3==c
   * @param scaling
   *        set to [na, nb, nc]
   * @return revised unit cell
   */
  public SymmetryInterface checkSupercell(Viewer vwr, SymmetryInterface uc,
                                          BS bsPoints, int abc, P3d scaling) {
    if (bsPoints.isEmpty())
      return uc;
    int minF = Integer.MAX_VALUE, maxF = Integer.MIN_VALUE;
    int[] counts = new int[MAX_COUNT + 1];
    int nAtoms = bsPoints.cardinality();
    for (int i = bsPoints.nextSetBit(0); i >= 0; i = bsPoints
        .nextSetBit(i + 1)) {
      SGAtom a = atoms[i];
      int type = a.typeAndOcc;
      SGAtom b;
      double f;
      for (int j = bsPoints.nextSetBit(0); j >= 0; j = bsPoints
          .nextSetBit(j + 1)) {
        if (j == i || (b = atoms[j]).typeAndOcc != type)
          continue;
        pt.sub2(b, a);
        switch (abc) {
        case 1:
          if (approx0(f = pt.x) || !approx0(pt.y) || !approx0(pt.z))
            continue;
          break;
        case 2:
          if (approx0(f = pt.y) || !approx0(pt.x) || !approx0(pt.z))
            continue;
          break;
        default:
        case 3:
          if (approx0(f = pt.z) || !approx0(pt.x) || !approx0(pt.y))
            continue;
          break;
        }
        int n = approxInt(1 / f);
        // must be positive, must be an integer divisor of the number of atoms.
        //System.out.println(n + " " + f + " " + abc + " " + pt + " " + a + " " + b + " " + nAtoms + " " + n + " " + counts[n]);
        if (n == 0 || nAtoms / n != 1d * nAtoms / n || n > MAX_COUNT)
          continue;
        //System.out.println(abc + " " + pt + " " + a + " " + b + " " + nAtoms + " " + n + " " + counts[n]);
        if (n > maxF)
          maxF = n;
        if (n < minF)
          minF = n;
        counts[n]++;
      }
    }
    int n = maxF;
    while (n >= minF) {
      if (counts[n] > 0 && counts[n] == (n - 1) * nAtoms / n) {
        break;
      }
      --n;
    }
    if (n < minF)
      return uc;
    // we have the smallest unit in this direction
    P3d[] oabc = uc.getUnitCellVectors();
    oabc[abc].scale(1d / n);
    switch (abc) {
    case 1:
      scaling.x = n;
      break;
    case 2:
      scaling.y = n;
      break;
    case 3:
      scaling.z = n;
      break;
    }
    uc = vwr.getSymTemp().getUnitCell(oabc, false, "scaled");
    // remove points not within this unitcell
    int f = 0;
    for (int i = bsPoints.nextSetBit(0); i >= 0; i = bsPoints
        .nextSetBit(i + 1)) {
      switch (abc) {
      case 1:
        f = approxInt(n * atoms[i].x);
        break;
      case 2:
        f = approxInt(n * atoms[i].y);
        break;
      case 3:
        f = approxInt(n * atoms[i].z);
        break;
      }
      if (f != 0) {
        atoms[i] = null;
        bsPoints.clear(i);
      }
    }
    nAtoms = bsPoints.cardinality();
    return uc;
  }

  private boolean approx0(double f) {
    return (Math.abs(f) < slop);
  }

  private int approxInt(double finv) {
//  int i = (int) Math.round (finv); // was 
  int i = (int) (finv + slop);
  return (approx0(finv - i) ? i : 0);
}


//  private static boolean approx000(double f) {
//    return (Math.abs(f) < SLOP0001);
//  }
//
//  private static boolean approx0014(double f) {
//    return (Math.abs(f) < SLOP0014);
//  }
//
//  private boolean approx01(double f) {
//    return (Math.abs(f) < SLOP02);
//  }
//
  @SuppressWarnings("unused")
  private int findEquiv(SymmetryInterface uc, int iop, SymmetryOperation op, int i, BS bsPoints,
                        P3d pt, boolean andClear) {
    SGAtom a = atoms[i];
    pt.setT(a);
    op.rotTrans(pt);
    uc.unitize(pt);
    if (pt.distanceSquared(a) == 0) {
      return i;
    }
    int testiop = -99;
    int type = a.typeAndOcc;
    String name = a.name;
    
    for (int j = nAtoms; --j >= 0;) {
      SGAtom b = atoms[j];
      if (b.typeAndOcc != type)
        continue;
      double d = b.distance(pt);
//      if (iop == 15 && j == 98 && i == 46)
//System.out.println("???");
      if (d * d < JC.UC_TOLERANCE2 
          || (1 - d) * (1 - d) < JC.UC_TOLERANCE2
          && latticeShift(pt, b)
          ) { // this is a SQUARE
        if (andClear) {
          j = Math.max(i, j);
          if (i != j)
            bsPoints.clear(j);
        }
        return j;
      }
    }
    return -1;
  }

  /**
   * Look for true {1 0 0}, {0 1 0}, {0 0 1} difference.
   * 
   * This comes from issues with SimpleCell.unitize and our methods here, I think.
   * 
   * @param a
   * @param b
   * @return true if a lattice shift
   */
  private boolean latticeShift(P3d a, P3d b) {
    boolean is1 = (approx0(Math.abs(a.x - b.x) - 1)
        || approx0(Math.abs(a.y - b.y) - 1)
        || approx0(Math.abs(a.z - b.z) - 1));
    if (is1) {
      //System.out.println("was 1 for " + a + b); 
      // Heulandite, Jalpaite, Letovicite, Merrillite, 
      // Schreyerite, Supierite, Shigaite, Tobermorite
    }
    return is1;
  }

  public static void main(String[] args) {
    if (loadData(null, new SpaceGroupFinder()))
      System.out.println("OK");
  }

  private static boolean loadData(Viewer vwr, Object me) {
    try {
      groupNames = getList(vwr, me, null, "sggroups_ordered.txt");
      GROUP_COUNT = groupNames.length;
      opXYZ = getList(vwr, me, null, "sgops_ordered.txt");
      OP_COUNT = opXYZ.length;
      String[] map = getList(vwr, me, new String[OP_COUNT], "sgmap.txt");
      bsGroupOps = new BS[GROUP_COUNT];
      bsOpGroups = new BS[OP_COUNT];
      for (int j = 0; j < GROUP_COUNT; j++)
        bsGroupOps[j] = BS.newN(OP_COUNT);
      for (int i = 0; i < OP_COUNT; i++) {
        String m = map[i];
        bsOpGroups[i] = BS.newN(GROUP_COUNT);
        for (int j = 0; j < GROUP_COUNT; j++) {
          if (m.charAt(j) == '1') {
            bsGroupOps[j].set(i);
            bsOpGroups[i].set(j);
          }
        }
      }
      ops = new SymmetryOperation[OP_COUNT];
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      if (rdr != null)
        try {
          rdr.close();
        } catch (IOException e) {
        }
    }
  }

  private static String[] getList(Viewer vwr, Object me, String[] list,
                                  String fileName)
      throws IOException {
    rdr = FileManager.getBufferedReaderForResource(vwr, me,
        "org/jmol/symmetry/", "sg/" + fileName);
    if (list == null) {
      Lst<String> l = new Lst<String>();
      String line;
      while ((line = rdr.readLine()) != null) {
        if (line.length() > 0) {
          l.addLast(line);
        }
      }
      l.toArray(list = new String[l.size()]);
    } else {
      for (int i = 0; i < list.length; i++)
        list[i] = rdr.readLine();
    }
    rdr.close();
    return list;
  }
  
// sg.spt - for generating the data files 
// this list was then processed by Excel pivot tables to get the binary listing
// sorted by 
//      x = spacegroup("all")
//      types = {"all":[1, 230]}
//
//      for (name in types) {
//      lat = types[name]
//
//      a = ""
//      lastname =""
//      for (var sg in x.spaceGroupInfo) {
//        if (sg.itaFull == lastname || sg.itaFull[0] == "*" || sg.ita < lat[1] || sg.ita > lat[2]) {
//          print "dup " + sg.itaFull
//          continue
//        }
//        lastname = sg.itaFull
//        var s = lastname.split(":")
//        var post = (s.length == 1 ? "" : ":" + s[2]) 
//        var pre = "\n\"" + s[1].format("%03s")+post + "\"\t\""
//        var list = sg.operationsXYZ
//        var n = sg.operationCount
//        for (var i = 1; i <= n; i++){
//          a +=  pre +  list[i] + "\""
//        }
//      }
//
//      write var a @{"c:/temp/sg_"+name}
//      }
//

  // load =ams/corkite 1 packed
  // 160:h HM:R 3 m:h Hall:R 3 -2"

  // load =ams/cordierite 1 packed
  // 66 HM:C c c m Hall:-C 2 2c
  
  
}
