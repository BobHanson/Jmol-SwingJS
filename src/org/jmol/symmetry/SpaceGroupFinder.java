package org.jmol.symmetry;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import org.jmol.api.Interface;
import org.jmol.api.SymmetryInterface;
import org.jmol.modelset.Atom;
import org.jmol.util.BSUtil;
import org.jmol.util.SimpleUnitCell;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.M4d;
import javajs.util.MeasureD;
import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.T3d;
import javajs.util.V3d;

/**
 * A relatively simple space group finder given a unit cell. The unit cell is
 * used to reduce the options involving hexagonal, tetragonal, and cubic groups.
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

  private Viewer vwr;
  private Symmetry uc;

  private SpaceGroup sg;

  private Atom[] cartesians;
  private SGAtom[] atoms;

  private int nAtoms;
  
  private String xyzList;

  private double[] unitCellParams;
  private double slop;

  private boolean isCalcOnly;
  private boolean isAssign;
  private boolean isUnknown;
  private boolean checkSupercell;
  private boolean isSupercell;
  private boolean asString;
  
  private BS bsPoints0;
  private BS bsAtoms;
  private BS targets;

  private T3d origin;
  private T3d[] oabc;
  
  private P3d scaling;
  private P3d pTemp = new P3d();
 
  private int isg; 
  private int groupType = SpaceGroup.TYPE_SPACE; // TODO 
  private boolean isSpecialGroup;

  private V3d vectorBA;

  private V3d vectorBC;

  private P3d zero;

  private boolean isQuery;

  private boolean isTransformOnly;
  
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
   * @param xyzList0
   *        a semicolon-separated list of possible space groups, such as
   *        "-x,-y,-z;x,-y,-z" or a space group ID such as "133:2"; if a list,
   *        prefixing or postfixing the list with "&" or joining with "&"
   *        indicates that a partial match is desired, returning a list of names
   *        of space groups that have at least the specified operations
   * 
   * @param unitCellParams
   * @param origin
   * @param oabc0
   * @param uci
   * @param flags
   *        see JC.SG_* constants only from x = spacegroup("parent") ?
   * @return SpaceGroup or null if isAssign, spacegroup information map if
   */
  @SuppressWarnings("unchecked")
  Object findSpaceGroup(Viewer vwr, final BS atoms0, final String xyzList0,
                        final double[] unitCellParams, final T3d origin,
                        final T3d[] oabc0, final SymmetryInterface uci,
                        final int flags) {
    this.vwr = vwr;
    xyzList = xyzList0;
    this.unitCellParams = unitCellParams;
    this.origin = origin;
    oabc = oabc0;
    uc = (Symmetry) uci;
    isCalcOnly = ((flags & JC.SG_CALC_ONLY) != 0);
    isAssign = ((flags & JC.SG_IS_ASSIGN) != 0);
    checkSupercell = ((flags & JC.SG_CHECK_SUPERCELL) != 0);
    asString = ((flags & JC.SG_AS_STRING) != 0);
    boolean setFromScratch = ((flags & JC.SG_FROM_SCRATCH) != 0);
    double slop0 = uc.getPrecision();
    slop = (!Double.isNaN(slop0) ? slop0
        : unitCellParams != null
            && unitCellParams.length > SimpleUnitCell.PARAM_SLOP
                ? unitCellParams[SimpleUnitCell.PARAM_SLOP]
                : Viewer.isJmolD ? SimpleUnitCell.SLOPDP
                    : SimpleUnitCell.SLOPSP);
    if (Double.isNaN(slop))
      slop = 1E-6;
    cartesians = vwr.ms.at;
    bsPoints0 = new BS();
    if (xyzList == null || isAssign) {
      bsAtoms = BSUtil.copy(atoms0);
      nAtoms = bsAtoms.cardinality();

    }
    isQuery = (xyzList != null && xyzList.indexOf("&") >= 0);
    isTransformOnly = (xyzList != null && xyzList.startsWith(".:"));
    targets = BS.newN(nAtoms);
    // this will be set in checkSupercell
    scaling = P3d.new3(1, 1, 1);
    String name;
    BS basis;
    isUnknown = true;
    // figure out the space group
    boolean isITA = (isTransformOnly || xyzList != null
        && xyzList.toUpperCase().startsWith("ITA/"));
    boolean isHall = (xyzList != null && !isITA
        && (xyzList.startsWith("[") || xyzList.startsWith("Hall:")));

    if (isAssign && isHall
        || !isHall && (isITA || (isITA = checkWyckoffHM()))) {
      isUnknown = false;
      if (isITA) {
        xyzList = PT.rep((isTransformOnly ? xyzList : xyzList.substring(4)), " ", "");
      } else if (xyzList.startsWith("Hall:")) {
        xyzList = xyzList.substring(5);
      } else {
        xyzList = PT.replaceAllCharacters(xyzList, "[]", "");
      }
      sg = setITA(isHall);
      
      if (sg == null)
        return null;
      name = sg.getName();
    } else if (oabc != null || isHall) {
      name = xyzList;
      if (name != null)
        sg = SpaceGroup.createSpaceGroupN(name, isHall);
      isUnknown = (sg == null);
      if (isHall && !isUnknown)
        return sg.dumpInfoObj();
      // still need basis
    } else if (!isQuery && SpaceGroup.isXYZList(xyzList)) {
      // never for isAssign true
      sg = SpaceGroup.findSpaceGroupFromXYZ(xyzList);
      if (sg != null)
        return sg.dumpInfoObj();
    }

    if (setFromScratch) {
      if (sg == null
          && (sg = SpaceGroup.determineSpaceGroupNA(xyzList,
              unitCellParams)) == null
          && (sg = SpaceGroup.createSpaceGroupN(xyzList, false)) == null)
        return null;
      basis = new BS();
      name = sg.asString();
      if (oabc == null) {
        boolean userDefined = (unitCellParams.length == 6);
        // this unit cell is still for the UNTRANSFORMED space group
        uc = setSpaceGroupAndUnitCell(sg, unitCellParams, null, userDefined);
        oabc = uc.getUnitCellVectors();
        if (origin != null)
          oabc[0].setT(origin);
      } else {
        uc = setSpaceGroupAndUnitCell(sg, null, oabc, false);
        // CLEG has saved this for us; now retrieve it
        uc.transformUnitCell(uci.saveOrRetrieveTransformMatrix(null));
      }
    } else {
      // this method is used to build basis as well, 
      // even if we already have the space groupn
      Object ret = findGroupByOperations();
      if (!isAssign || !(ret instanceof SpaceGroup))
        return ret;
      sg = (SpaceGroup) ret;
      name = sg.asString();
      basis = BSUtil.copy(bsAtoms);
      for (int i = targets.nextSetBit(0); i >= 0; i = targets.nextSetBit(i + 1))
        basis.clear(atoms[i].index);
      int nb = basis.cardinality();
      if (!isCalcOnly) {
        String msg = name + (atoms0 == null || nb == 0 ? ""
            : "\nbasis is " + nb + " atom" + (nb == 1 ? "" : "s") + ": "
                + basis);
        System.out.println("SpaceGroupFinder chose " + msg);
        if (asString)
          return msg;
      }
    }

    // supplement SpaceGroup info map with additional information
    Map<String, Object> map = (Map<String, Object>) sg.dumpInfoObj();
    if (uc != null && !isCalcOnly)
      System.out.println("unitcell is " + uc.getUnitCellInfo(true));
    if (!isAssign) {
      BS bs1 = BS.copy(bsPoints0);
      bs1.andNot(targets);
      dumpBasis(bsGroupOps[isg], bs1, bsPoints0);
    }
    map.put("name", name);
    map.put("basis", basis);
    if (isSupercell)
      map.put("supercell", scaling);
    oabc[1].scale(1 / scaling.x);
    oabc[2].scale(1 / scaling.y);
    oabc[3].scale(1 / scaling.z);
    map.put(JC.INFO_UNIT_CELL, oabc);
    if (isAssign)
      map.put("sg", sg);
    return map;
  }


  private boolean checkWyckoffHM() {
    if (xyzList != null && xyzList.indexOf(",") < 0) {
      //could be H-M -- check for odd case
      String clegId = SpaceGroup.convertWyckoffHMCleg(xyzList, null);
      if (clegId != null) {
        xyzList = "ITA/" + clegId;
        return true;
      }
    }
    return false;
  }


  @SuppressWarnings("unchecked")
  private SpaceGroup setITA(boolean isHall) {
    String name = null;
    Map<String, Object> sgdata = null;
    int pt = xyzList.lastIndexOf(":");
    int pt2 = xyzList.indexOf(",");
    boolean hasTransform = (pt > 0 && pt2 > pt);
    String clegId = null;
    // here we have said ITA/40:b  because we want the ITA variation
    // but that is not going to be documented! 
    boolean isJmolCode = (pt > 0 && !hasTransform);
    String transform = null;
    if (hasTransform) {
      name = transform = uc.staticCleanTransform(xyzList.substring(pt + 1));
      xyzList = xyzList.substring(0, pt);
      clegId = xyzList + ":" + transform;
      // why name?
      if (transform.equals("a,b,c")) {
        transform = null;
        hasTransform = false;
      }
    }
    pt = xyzList.indexOf(".");

    if (pt > 0 && (hasTransform || isJmolCode)) {
      xyzList = xyzList.substring(0, pt);
      pt = -1;
    }
    String itano = xyzList;
    if (SpaceGroup.getExplicitSpecialGroupType(itano) > SpaceGroup.TYPE_SPACE) {
      itano = itano.substring(2);
      isSpecialGroup = true;
      pt -= 2;
    }
    boolean isITADotSetting = (pt > 0);
    if (!isJmolCode && !isHall && !hasTransform && !isITADotSetting
        && PT.parseInt(itano) != Integer.MIN_VALUE)
      xyzList += ".1";
    Lst<Object> genPos;
    Map<String, Object> setting = null;
    String itaIndex = xyzList; // may be Hall or jmolId as well
    String t0 = null;
    if (isHall) {
      genPos = (Lst<Object>) uc.getSpaceGroupInfoObj("nameToXYZList",
          "Hall:" + xyzList, false, false);
      if (genPos == null)
        return null;
    } else {
      // get space group
      name = (hasTransform ? transform : itaIndex);// p/2 here for itaIndex?
      if (isTransformOnly) {
        sg = uc.spaceGroup;
        t0 = sg.itaTransform;
        genPos = null;
      } else {
        sg = SpaceGroup.getSpaceGroupFromJmolClegOrITA(vwr,
            hasTransform ? clegId : itaIndex);
        // get reference group data
        Object allSettings = uc.getSpaceGroupJSON("ITA", itaIndex, 0);
        if (allSettings == null || allSettings instanceof String) {
          return null;
        }
        sgdata = (Map<String, Object>) allSettings;
        if (isJmolCode || hasTransform) {
          Lst<Object> its = (Lst<Object>) sgdata.get("its");
          if (its == null)
            return null;
          sgdata = null;
          for (int i = 0, c = its.size(); i < c; i++) {
            setting = (Map<String, Object>) its.get(i);
            if (name.equals(setting.get(hasTransform ? "trm" : "jmolId"))) {
              sgdata = setting;
              break;
            }
          }
          // "more" type, from wp-list, does note contain gp or wpos
          // also no jmolId for special groups
          if (sgdata == null
              || !isSpecialGroup && !sgdata.containsKey("jmolId")) {
            if (isJmolCode) {
              // trying to get an ITA from a Jmol code with ITA like ITA/12:abc
              return null;
            }
            // nonstandard transform
            setting = null;
            if (sgdata != null)
              transform = (String) sgdata.get("trm");
            hasTransform = true;
            sgdata = (Map<String, Object>) its.get(0);
          } else {
            // we have sgdata, and we have a jmolId
            // we can just set this from the sg and the general positions
            // if we also found the space group
            setting = null;
            // done here
            hasTransform = false;
            transform = null;
          }
        } else {
          name = (String) sgdata.get("jmolId");
        }
        //      boolean isKnownToITAButNotToJmol = isJmolCode && (name.indexOf("?") < 0);
        genPos = (Lst<Object>) sgdata.get("gp");
      }
    }
    if (sg != null && transform == null) {
      sg = SpaceGroup.createITASpaceGroup(sg.groupType, genPos, sg);
      return sg;
    }
    sg = SpaceGroup.transformSpaceGroup(groupType, null, sg, genPos,
        (hasTransform ? transform : null), 
         (hasTransform ? new M4d() : null));
    if (sg == null)
      return null;
    name = "";
    if (sg.itaNumber.equals("0")) {
      if (transform == null) {
        transform = (String) sgdata.get("trm");
        String hm = (String) sgdata.get("hm");
        sg.setHMSymbol(hm);
      } else if (isTransformOnly){
        transform = multiplyTransforms(t0, transform);
        sg.setITATableNames(null, itano, null, transform);
      } else {
        sg.setITATableNames(null, itano, null, transform);
      }
      name = null;
      System.out.println("SpaceGroupFinder: new setting: " + sg.asString());
    }
    return sg;
  }

  private String multiplyTransforms(String t0, String t1) {
    M4d m0 = matFor(t0);
    M4d m1 = matFor(t1);
    m1.mul(m0);
    return abcFor(m1);
  }


  private static String abcFor(M4d trm) {
    return SymmetryOperation.getTransformABC(trm, false);
  }

  private M4d matFor(String trm) {
    M4d m = new M4d();
    UnitCell.getMatrixAndUnitCell(null, null, trm, m);
    return m;
  }



  private Object findGroupByOperations() {
    BS bsOps = new BS();
    BS bsGroups = new BS();
    int n = 0;
    int nChecked = 0;
    try {
      if (isUnknown) {
        if (bsOpGroups == null)
          loadData(vwr);
      } else {
        bsGroups.set(0);
      }
      if (xyzList != null) {
        if (isUnknown) {
          Object ret = getGroupsWithOps();
          if (!isAssign || ret == null)
            return ret;
          sg = (SpaceGroup) ret;
        }
        if (oabc == null)
          uc.setUnitCellFromParams(unitCellParams, false, slop);
      }
      SymmetryInterface uc0 = uc;
      if (oabc == null) {
        oabc = uc.getUnitCellVectors();
        uc = (Symmetry) uc.getUnitCellMultiplied();
        if (origin != null && !origin.equals(uc.getCartesianOffset())) {
          oabc[0].setT(origin);
          uc.setCartesianOffset(origin);
        }
      } else {
        uc.getUnitCell(oabc, false, "finder");
      }
      if (isUnknown)
        filterGroups(bsGroups, uc.getUnitCellParams());
      else if (nAtoms == 0) {
        return sg;
      }

      //      withinCell = vwr.ms.getAtoms(T.unitcell, uc);
      //BS extraAtoms = BSUtil.copy(bsAtoms);
      //extraAtoms.andNot(withinCell);
      //      withinCell.and(bsAtoms);
      //int nExtra = extraAtoms.cardinality();

      // 1. Get ALL atoms.

      if (bsAtoms != null) {
        atoms = new SGAtom[bsAtoms.cardinality()];
        System.out.println("bsAtoms = " + bsAtoms);
        for (int p = 0, i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms
            .nextSetBit(i + 1), p++) {
          Atom a = cartesians[i];
          int type = a.getAtomicAndIsotopeNumber();
          (atoms[p] = new SGAtom(type, i, a.getAtomName(), a.getOccupancy100()))
              .setT(toFractional(a, uc));
        }
      }
      BS bsPoints = BSUtil.newBitSet2(0, nAtoms);

      // Look out for tetrgonal aac issue -- abandoned

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
              System.out.println("supercell found; a scaled by 1/" + scaling.x);
            if (scaling.y != 1)
              System.out.println("supercell found; b scaled by 1/" + scaling.y);
            if (scaling.z != 1)
              System.out.println("supercell found; c scaled by 1/" + scaling.z);
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
        }
        bsAtoms.set(atoms[i].index);
      }

      atoms = newAtoms;
      nAtoms = n;
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
      int count = 0;
      for (int iop = bsOps.nextSetBit(1); iop > 0
          && !bsGroups.isEmpty(); iop = bsOps.nextSetBit(iop + 1)) {
        SymmetryOperation op = (sg == null ? getOp(iop)
            : (SymmetryOperation) sg.getOperation(iop));
        if (sg == null) {
          System.out.println("\n" + ++count + " Checking operation " + iop + " " + opXYZ[iop]);// + " " + bsOpGroups[iop]);
          System.out.println("bsGroups = " + bsGroups);
          System.out.println("bsOps = " + bsOps);
          nChecked++;
        }
        boolean isOK = true;
        bsPoints.clearAll();
        bsPoints.or(bsPoints0);
        targeted.clearAll();
 //       boolean allInvariant = true;
        for (int i = bsPoints.nextSetBit(0); i >= 0; i = bsPoints
            .nextSetBit(i + 1)) {
          bsPoints.clear(i);
          int j = findEquiv(iop, op, i, bsPoints, true);
          if (j < 0 && sg == null) {
            System.out.println(
                "failed op " + iop + " for atom " + i + " " + atoms[i].name
                    + " " + atoms[i] + " looking for " + pTemp + "\n" + op);
            isOK = false;
            break;
          }
          if (j >= 0 && i != j) {
//            allInvariant = false;
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
            // this could work if we did not implement it for screw axes and 
//            if (false && !allInvariant) {
//              bsGroups.and(myGroups);
//              // reduce the number of operations to check to only those NOT common to 
//              // all remaining groups;
//              temp1.setBits(1, OP_COUNT);
//              for (int i = bsGroups.nextSetBit(0); i >= 0; i = bsGroups
//                  .nextSetBit(i + 1)) {
//                temp1.and(bsGroupOps[i]);
//              }
//              uncheckedOps.or(temp1);
//              bsOps.andNot(temp1);
//            }
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
      // part 2
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
            BS bs = bsGroupOps[isg];
            opsChecked.and(bs);
            uncheckedOps.and(bs);
            uncheckedOps.andNot(opsChecked);
            uncheckedOps.or(bs);
            uncheckedOps.clear(0);
            bsPoints.or(bsPoints0);
            //System.out.println("test1 bspoints " + bsPoints.cardinality() + " " + bsPoints);
            //System.out.println("test2 targets " + targets.cardinality() + " " + targets);
            //System.out.println("test3 uchop= " + uncheckedOps.cardinality() + " " + uncheckedOps);
            //System.out.println("test4 opsc= " + opsChecked.cardinality() + " " + opsChecked);
            //bsPoints.andNot(targets);
            if (!checkBasis(uncheckedOps, bsPoints)) {
              System.out.println("failed checkBasis");
              isg = 0;
            }
            //System.out.println("test2b targets " + targets.cardinality() + " " + targets);
            //System.out.println("test1b points " + bsPoints.cardinality() + " " + bsPoints);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      bsGroups.clearAll();
    }
    if (sg == null) {
      System.out.println("checked " + nChecked + " operations; now " + n + " groups="
          + bsGroups + " ops=" + bsOps);
      SpaceGroup[] groups = new SpaceGroup[bsGroups.cardinality()];
      for (int p = 0, i = bsGroups.nextSetBit(0); i >= 0; i = bsGroups
          .nextSetBit(i + 1)) {
        SpaceGroup sg = groups[p++] = nameToGroup(groupNames[i]);
        sg.sfIndex = i;
      }
      Arrays.sort(groups, new Comparator<SpaceGroup>() {
        @Override
        public int compare(SpaceGroup sg1, SpaceGroup sg2) {
          // sort 14.x < 15.x and 15.2 < 15.1 
          // so as to target the highest IT number and the lowest set number
          return (sg1.itaNo != sg2.itaNo ? sg1.itaNo - sg2.itaNo : sg2.setNo - sg1.setNo);
        }
      });
      // get highest valid group; the rest are subgroups
      for (int i = groups.length; --i >= 0;) {
        if (checkUnitCell(groups[i]))
          break;
        System.out.println("SpaceGroupFinder unit cell check failed for " + nameToGroup(groupNames[i]));
        n--;
        bsGroups.clear(groups[i].sfIndex);        
      }
      System.out.println("SpaceGroupFinder found " + bsGroups.cardinality() + " possible groups");
// debugging only
//      for (int i = bsGroups.nextSetBit(0); i >= 0; i = bsGroups
//          .nextSetBit(i + 1)) {
//        System.out.println("SpaceGroupFinder found "
//            + nameToGroup(groupNames[i]));
//      }
      isg = bsGroups.length() - 1;
      if (isg < 0)
        return null;
      sg = nameToGroup(groupNames[isg]);
    }
    return sg;
  }


  /**
   * Specifically for monoclinics, transform a, b, and c back to the default "principle axis b" default
   * for the space group and ensure that the transformed unit cell has  angles alpha=90 and gamma=90
   * 
   * @param sg 
   * 
   * @return true if the unit cell is appropriate -- monoclinic only, because
   *              we have already tested for a=b
   */
  private boolean checkUnitCell(SpaceGroup sg) {
    if (sg.itaNo < 3 || sg.itaNo >= 16)
      return true;
    if (zero == null) {
      vectorBA = new V3d();
      vectorBC = new V3d();
      zero = new P3d();
    }
    String trm = sg.getClegId();
    trm = trm.substring(trm.indexOf(":") + 1);
    M4d tr = (M4d) uc.staticConvertOperation("!"+trm, null, null);
    P3d a = P3d.new3(1, 0, 0);
    P3d b = P3d.new3(0, 1, 0);
    P3d c = P3d.new3(0, 0, 1);
    
    tr.rotTrans(a);
    tr.rotTrans(b);
    tr.rotTrans(c);
    uc.toCartesian(a, true);
    uc.toCartesian(b, true);
    uc.toCartesian(c, true);
    double angleAB = MeasureD.computeAngle(a, zero, b, vectorBA, vectorBC, true);
    double angleBC = MeasureD.computeAngle(a, zero, b, vectorBA, vectorBC, true);
    
    return (approx0(angleAB - 90) && approx0(angleBC - 90));
    
    //    modelkit zap spacegroup("10:b,c,a")
    //    tr = matrix("!b,c,a")
    //        a = {1 0 0}
    //        b = {0 1 0}
    //        c = {0 0 1}
    //        print tr
    //        ap = tr * a
    //        bp = tr * b
    //        cp = tr * c
    //        print ap
    //        print bp
    //        print cp
    //        apc = ap.xyz
    //        bpc = bp.xyz
    //        cpc = cp.xyz
    //        print angle(apc {0 0 0} bpc) // 90
    //        print angle(cpc {0 0 0} bpc) // 90
  }


  private static SpaceGroup nameToGroup(String name) {
    int pt = (name.charAt(0) != '0' ?  0 : name.charAt(1) != '0' ? 1 : 2);
    return  SpaceGroup.nameToGroup.get(name.substring(pt));
  }

  private Symmetry setSpaceGroupAndUnitCell(SpaceGroup sg, double[] params,
                                           T3d[] oabc, boolean allowSame) {
    Symmetry sym = (Symmetry) Interface.getSymmetry(vwr, "sgf");
    sym.setSpaceGroupTo(sg);
    if (oabc == null) {
    double[] newParams = new double[6];
    if (!sg.createCompatibleUnitCell(params, newParams, allowSame)) {
      newParams = params;
    }
    sym.setUnitCellFromParams(newParams, false, Double.NaN);
    } else {
      sym.getUnitCell(oabc, false, "modelkit");
    }
    return sym;
  }  

  private void removeDuplicates(BS bs) {
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      SGAtom a = atoms[i];
      for (int j = bs.nextSetBit(0); j < i; j = bs.nextSetBit(j + 1)) {
        SGAtom b = atoms[j];
        if (a.typeAndOcc == b.typeAndOcc
            && a.distanceSquared(b) < JC.UC_TOLERANCE2) {
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

  private boolean checkBasis(BS uncheckedOps, BS bsPoints) {

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
      SymmetryOperation op = getOp(iop);
      bs.or(bsPoints);
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        int j = findEquiv(-1, op, i, bs, false);
        if (j < 0)
          return false;
// for this check we must check all atoms with all operations
//        if (i != j) {
//          j = Math.max(i, j);
//          targets.set(j);
//          bs.clear(j);
//        }
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
    boolean isOrtho = false, isTet = false, isTri = false, isRhombo = false,
        isCubic = false;
    boolean absame = approx001(params[0] - params[1]);
    boolean bcsame = approx001(params[1] - params[2]);
    if (params[3] == 90) { // alpha
      if (params[4] == 90) { // beta
        // really?? 
        if (absame && params[0] != params[1])
          System.out.println("OHOH - very close a,b distance");
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
    } else if (absame && bcsame && approx001(params[3] - params[4])
        && approx001(params[4] - params[5])) {
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
    num = "000" + num;
    num = num.substring(num.length() - 3);
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
   * @return an array of space group IDs if not isAssign and not "="; a
   *         SpaceGroup or null if isAssign; a single space group ID as a string
   *         if "=" and a string starting and ending with "?" if an xyz operator
   *         is of an invalid form.
   */
  private Object getGroupsWithOps() {

    BS groups = new BS();
    if (unitCellParams == null) {
      groups.setBits(0, GROUP_COUNT);
    } else {
      filterGroups(groups, unitCellParams);
    }
    SpaceGroup sgo = null;
    if (!SpaceGroup.isXYZList(xyzList)) {
      // space group name -- here we return a space group if unit cell symmetry is allowed
      sgo = SpaceGroup.determineSpaceGroupNA(xyzList, unitCellParams);
      if (sgo == null)
        return null;
      sgo.checkHallOperators();
      String tableNo = ("00" + sgo.jmolId);
      int pt = tableNo.indexOf(":");
      tableNo = tableNo.substring((pt < 0 ? tableNo.length() : pt) - 3 );
      // check for appropriate unit cell
      for (int i = 0; i < GROUP_COUNT; i++)
        if (groupNames[i].equals(tableNo)) {
          if (!groups.get(i))
            return null;
          if (unitCellParams != null) {
            uc.setUnitCellFromParams(unitCellParams, false, slop);
            if (!checkUnitCell(sgo))
              return null;
          }
          return (sgo);
        }
      return null;
    }
    // xyz list only, possibly starting with "&" for partial match
    boolean isEqual = !isQuery || isAssign;
    String[] ops = PT.split(PT.trim(xyzList.trim().replace('&', ';'), ";="),
        ";");
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
      return (isAssign ? SpaceGroup.createSpaceGroupN(xyzList, true) : null);
    }
    if (isEqual) {
      for (int n = ops.length, i = groups.nextSetBit(0); i >= 0; i = groups
          .nextSetBit(i + 1)) {
        if (bsGroupOps[i].cardinality() == n) {
         sg = nameToGroup(groupNames[i]);
         uc.setUnitCellFromParams(unitCellParams, false, slop);
         if (!checkUnitCell(sg))
           if (isAssign) {
             return SpaceGroup.createSpaceGroupN(groupNames[i], true);
           }
           return SpaceGroup.getInfo(null, groupNames[i], unitCellParams, true,
              false);
        }
      }
      return null;
    }
    // at this point, the group has cardinality of at least 1 
    Lst<String> ret = new Lst<>();
    uc.setUnitCellFromParams(unitCellParams, false, slop);
    for (int i = groups.nextSetBit(0); i >= 0; i = groups
        .nextSetBit(i + 1)) {
      SpaceGroup sg = nameToGroup(groupNames[i]);
      if (checkUnitCell(sg))
        ret.addLast(sg.getClegId());
    }
    return ret;
  }

  P3d toFractional(Atom a, SymmetryInterface uc) {
    pTemp.setT(a);
    uc.toFractional(pTemp, false);
    return pTemp;
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
   * 
   * @param vwr
   * @param uc
   * @param bsPoints
   * @param abc
   *        1==a, 2==b, 3==c
   * @param scaling
   *        set to [na, nb, nc]
   * @return revised unit cell
   */
  public Symmetry checkSupercell(Viewer vwr, Symmetry uc,
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
        pTemp.sub2(b, a);
        switch (abc) {
        case 1:
          if (approx0(f = pTemp.x) || !approx0(pTemp.y) || !approx0(pTemp.z))
            continue;
          break;
        case 2:
          if (approx0(f = pTemp.y) || !approx0(pTemp.x) || !approx0(pTemp.z))
            continue;
          break;
        default:
        case 3:
          if (approx0(f = pTemp.z) || !approx0(pTemp.x) || !approx0(pTemp.y))
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
    (uc = (Symmetry) Interface.getSymmetry(vwr, "sgf")).getUnitCell(oabc, false, "scaled");
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

  @SuppressWarnings("unused")
  private int findEquiv(int iop, SymmetryOperation op,
                        int i, BS bsPoints, boolean andClear) {
    SGAtom a = atoms[i];
    pTemp.setT(a);
    op.rotTrans(pTemp);
    uc.unitize(pTemp);
    if (pTemp.distanceSquared(a) == 0) {
      return i;
    }
    int testiop = -99;
    int type = a.typeAndOcc;
    String name = a.name;

    for (int j = nAtoms; --j >= 0;) {
      SGAtom b = atoms[j];
      if (b.typeAndOcc != type)
        continue;
      double d = b.distance(pTemp);
      //      if (iop == 15 && j == 98 && i == 46)
      //System.out.println("???");
      if (d * d < JC.UC_TOLERANCE2
          || (1 - d) * (1 - d) < JC.UC_TOLERANCE2 
          && latticeShift(pTemp, b)
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
   * This comes from issues with SimpleCell.unitize and our methods here, I
   * think.
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
    // map is created using sg.spt and createmap.xls
    // I then used superfix.exe to remove all tabs. 
    
    if (loadData(null))
      System.out.println("OK");
  }

  private static boolean loadData(Viewer vwr) {
    try {
      // recreated using createmap.xlsx 2024.09.03
      groupNames = getList(vwr, null, "sggroups_ordered.txt");
      GROUP_COUNT = groupNames.length;
      opXYZ = getList(vwr, null, "sgops_ordered.txt");
      OP_COUNT = opXYZ.length;
      String[] map = getList(vwr, new String[OP_COUNT], "sgmap.txt");
      bsGroupOps = new BS[GROUP_COUNT];
      bsOpGroups = new BS[OP_COUNT];
      for (int j = 0; j < GROUP_COUNT; j++)
        bsGroupOps[j] = BS.newN(OP_COUNT);
      
      for (int i = 0; i < OP_COUNT; i++) {
        String m = map[i];
        int n = m.length();
        bsOpGroups[i] = BS.newN(GROUP_COUNT);
        for (int j = 0; j < n; j++) {
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

  private static String[] getList(Viewer vwr, String[] list,
                                  String fileName)
      throws IOException {
    rdr = FileManager.getBufferedReaderForResource(vwr, SpaceGroupFinder.class,
        "org/jmol/symmetry/", "sg/" + fileName);
    if (list == null) {
      Lst<String> l = new Lst<String>();
      String line;
      while ((line = rdr.readLine()) != null) {
//        System.out.println(i++ + " "  + line);
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
  //        if (sg.jmolId == lastname || sg.jmolId[0] == "*" || sg.ita < lat[1] || sg.ita > lat[2]) {
  //          print "dup " + sg.jmolId
  //          continue
  //        }
  //        lastname = sg.jmolId
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
