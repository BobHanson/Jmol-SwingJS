package org.jmol.symmetry;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

import org.jmol.api.SymmetryInterface;
import org.jmol.modelset.Atom;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.SimpleUnitCell;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.P3;

public class SpaceGroupFinder {

  private static int GROUP_COUNT; // 530
  private static int OP_COUNT; // 882
  private static BS[] bsOpGroups;
  private static BS[] bsGroupOps;
  private static String[] groupNames;
  private static String[] opXYZ;
  private static SymmetryOperation[] ops;

  private static BufferedReader rdr = null;

  static {

  }

  private SGAtom[] atoms;
  private int nAtoms;
  P3 pt = new P3();

  /**
   * maximum allowable supercell
   */
  private static final int MAX_COUNT = 100;

  public SpaceGroupFinder() {
  }

  private class SGAtom extends P3 {
    int type;
    public int index;

    SGAtom(P3 uxyz, int type, int index) {
      setT(uxyz);
      this.type = type;
      this.index = index;
    }
  }

  public Object findSpaceGroup(Viewer vwr, BS atoms0, SymmetryInterface uc,
                               boolean asString) {
    int isg = 0;
    BS bsAtoms = BSUtil.copy(atoms0);
    BS bsGroups = new BS();
    BS targets = BS.newN(nAtoms);
    BS bsOps = new BS();
    nAtoms = bsAtoms.cardinality();
    int n = 0;
    int nChecked = 0;
    P3 scaling = P3.new3(1, 1, 1);
    try {
      if (bsOpGroups == null)
        loadData(vwr, this);
      uc = uc.getUnitCellMultiplied();
      bsGroups.setBits(0, GROUP_COUNT);
      bsOps.setBits(1, OP_COUNT);
      BS withinCell = vwr.ms.getAtoms(T.unitcell, uc);
      BS extraAtoms = BSUtil.copy(bsAtoms);
      extraAtoms.andNot(withinCell);
      int nExtra = extraAtoms.cardinality();

      // 1. Get ALL atoms.

      atoms = new SGAtom[bsAtoms.cardinality()];
      System.out.println("bsAtoms = " + bsAtoms);
      for (int p = 0, i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms
          .nextSetBit(i + 1), p++) {
        Atom a = vwr.ms.at[i];
        int type = a.getAtomicAndIsotopeNumber();
        pt.setT(a);
        uc.toFractional(pt, false);
        atoms[p] = new SGAtom(pt, type, i);
      }
      BS bsPoints = BSUtil.newBitSet2(0, nAtoms);

      // 2. Check that packing atoms, if any, are complete and only those packed.

      if (nExtra > 0) {
        BS packedAtoms = new BS();
        checkPackingAtoms(bsPoints, extraAtoms, 1, packedAtoms);
        checkPackingAtoms(bsPoints, extraAtoms, 2, packedAtoms);
        checkPackingAtoms(bsPoints, extraAtoms, 3, packedAtoms);
        if (packedAtoms.cardinality() == nExtra)
          bsPoints.andNot(packedAtoms);
        else
          bsPoints.clearAll();
      }

      // 3. Unitize and check for supercells in each direction


      nAtoms = bsPoints.cardinality();
      if (nAtoms > 0) {
        for (int i = bsPoints.nextSetBit(0); i >= 0; i = bsPoints
            .nextSetBit(i + 1)) {
          SimpleUnitCell.unitizeDim(3, atoms[i]);
        }
        uc = checkSupercell(uc, bsPoints, 1, scaling);
        uc = checkSupercell(uc, bsPoints, 2, scaling);
        uc = checkSupercell(uc, bsPoints, 3, scaling);
        if (scaling.x != 1)
          System.out.println("supercell found; a scaled by 1/" + scaling.x);
        if (scaling.y != 1)
          System.out.println("supercell found; b scaled by 1/" + scaling.y);
        if (scaling.z != 1)
          System.out.println("supercell found; c scaled by 1/" + scaling.z);
      }

      // 4. Remove unneeded atoms

      n = bsPoints.cardinality();
      bsAtoms = new BS();
      SGAtom[] newAtoms = new SGAtom[n];
      for (int p = 0, i = bsPoints.nextSetBit(0); i >= 0; i = bsPoints
          .nextSetBit(i + 1)) {
        newAtoms[p++] = atoms[i];
        bsAtoms.set(atoms[i].index);
      }
      atoms = newAtoms;
      nAtoms = n;
      System.out.println("bsAtoms(within cell) = " + bsAtoms);
      bsPoints.clearAll();
      bsPoints.setBits(0, nAtoms);
      BS bsPoints0 = BS.copy(bsPoints);
      BS temp1 = BS.newN(OP_COUNT);
      BS targeted = BS.newN(nAtoms);
      
      if (nAtoms == 0) {
        bsGroups.clearBits(1, GROUP_COUNT);
        bsOps.clearAll();
      }

      // 5. Adaptively iterate over all known operations.

      BS uncheckedOps = BS.newN(OP_COUNT);
      BS opsChecked = BS.newN(OP_COUNT);
      opsChecked.set(0);
      while (bsGroups.cardinality() > 0 && !bsOps.isEmpty()) {
        System.out.println("bsGroups = " + bsGroups);
        System.out.println("bsOps = " + bsOps);
        int iop = bsOps.nextSetBit(1);
        SymmetryOperation op = getOp(iop);
        System.out.println("Checking operation " + iop + " " + opXYZ[iop]);
        nChecked++;
        boolean isOK = true;
        bsPoints.clearAll();
        bsPoints.or(bsPoints0);
        targeted.clearAll();
        for (int i = bsPoints.nextSetBit(0); i >= 0; i = bsPoints
            .nextSetBit(i + 1)) {
          bsPoints.clear(i);
          int j = findEquiv(op, i, bsPoints, pt);
          if (j < 0) {
            isOK = false;
            break;
          }
          if (i != j)
            targeted.set(Math.max(atoms[i].index, atoms[j].index));
        }
        BS myGroups = bsOpGroups[iop];
        bsOps.clear(iop);
        opsChecked.set(iop);
        if (isOK) {
          System.out.println("OK!");
          // iop was found
          targets.or(targeted);
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
      }
      n = bsGroups.cardinality();
      isg = bsGroups.nextSetBit(0);
      if (n == 1 && !asString) {
        uncheckedOps.and(bsGroupOps[isg]);
        uncheckedOps.andNot(opsChecked);
        checkBasis(uncheckedOps, bsPoints, targets);
      }

      
    } catch (Exception e) {
      e.printStackTrace();
      bsGroups.clearAll();
    }
    System.out.println("checked " + nChecked + " operations; now " + n + " "
        + bsGroups + " " + bsOps);
    for (int i = bsGroups.nextSetBit(0); i >= 0; i = bsGroups
        .nextSetBit(i + 1)) {
      System.out.println(SpaceGroup.nameToGroup.get(groupNames[i]));
    }
    if (n != 1)
      return null;
    String name = groupNames[isg];
    uc.setSpaceGroupName(name);
    System.out.println("found " + name);
    SpaceGroup sg = SpaceGroup.nameToGroup.get(name);
    if (asString)
      return sg.toString();
    @SuppressWarnings("unchecked")
    Map<String, Object> map = (Map<String, Object>) sg.dumpInfoObj();       
    BS basis = BSUtil.copy(bsAtoms);
     basis.andNot(targets);    
    System.out.println("basis is " + basis);
    System.out.println("unitcell is " + uc.getUnitCellInfo(true));
    map.put("basis", basis);
    map.put("supercell", scaling);
    float[] params = uc.getUnitCellParams();
    if (Float.isNaN(params[6]))
      System.arraycopy(params, 0, params = new float[6], 0, 6);
    map.put("unitcell", params);
    return map;
  }

  private static SymmetryOperation getOp(int iop) {
    SymmetryOperation op = ops[iop];
    if (op == null) {
      ops[iop] = op = new SymmetryOperation(null, null, 0, iop, false);
      op.setMatrixFromXYZ(opXYZ[iop], 0, false);
      op.doFinalize();
    }
    return op;
  }

  private void checkBasis(BS uncheckedOps, BS bsPoints, BS targets) {
    int n = uncheckedOps.cardinality();
    if (n == 0)
      return;
    BS bs = new BS();
    bs.or(bsPoints);
    System.out.println("finishing check for basis for " + n + " operations");
    for (int iop = uncheckedOps.nextSetBit(0); iop >= 0; iop = uncheckedOps
        .nextSetBit(iop + 1)) {
      if (bs.cardinality() == 0)
        return;
      SymmetryOperation op = getOp(iop);
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        int j = findEquiv(op, i, bsPoints, pt);
        if (i != j) {
          j = Math.max(i, j);
          targets.set(atoms[j].index);
          bs.clear(j);
        }
      }
    }
  }

  /**
   * Check for the same number of base (coord 0) atoms is the same as the number
   * of packed atoms. If that is not the case, we have P1 symmetry, and we just
   * clear all points.
   * 
   * @param bsPoints
   *        set to empty if there is a problem
   * @param extraAtoms
   * @param packedAtoms
   *        to be filled
   * @param abc
   */
  private void checkPackingAtoms(BS bsPoints, BS extraAtoms, int abc,
                                 BS packedAtoms) {
    int nextra = extraAtoms.cardinality();
    if (nextra == 0)
      return;
    int nbase = 0;
    int npacked = 0;
    float f = 0;
    for (int i = bsPoints.nextSetBit(0); i >= 0; i = bsPoints
        .nextSetBit(i + 1)) {
      SGAtom a = atoms[i];
      boolean isExtra = extraAtoms.get(a.index);
      switch (abc) {
      case 1:
        f = a.x;
        break;
      case 2:
        f = a.y;
        break;
      case 3:
        f = a.z;
        break;
      }
      if (isExtra) {
        if (approx0(f))
          isExtra = false;
        else
          f -= 1;
      }
      if (approx0(f)) {
        if (isExtra) {
          npacked++;
          packedAtoms.set(i);
        } else {
          nbase++;
        }
      }
    }
    if (nbase != npacked) {
      bsPoints.clearAll();
      extraAtoms.clearAll();
    }
  }

  /**
   * Look for a supercell and adjust lattice down if necessary.
   * 
   * @param uc
   * @param bsPoints
   * @param abc
   *        1==a, 2==b, 3==c
   * @param scaling
   *        set to [na, nb, nc]
   * @return revised unit cell
   */
  public SymmetryInterface checkSupercell(SymmetryInterface uc, BS bsPoints,
                                          int abc, P3 scaling) {
    if (bsPoints.cardinality() == 0)
      return uc;
    int minF = Integer.MAX_VALUE, maxF = Integer.MIN_VALUE;
    int[] counts = new int[MAX_COUNT + 1];
    for (int i = bsPoints.nextSetBit(0); i >= 0; i = bsPoints
        .nextSetBit(i + 1)) {
      SGAtom a = atoms[i];
      int type = a.type;
      SGAtom b;
      float f;
      for (int j = bsPoints.nextSetBit(0); j >= 0; j = bsPoints
          .nextSetBit(j + 1)) {
        if (j == i || (b = atoms[j]).type != type)
          continue;
        pt.sub2(b, a);
        switch (abc) {
        case 1:
          f = pt.x;
          if (f <= 0.0001f || !approx0(pt.y) || !approx0(pt.z))
            continue;
          break;
        case 2:
          f = pt.y;
          if (f <= 0.0001f || !approx0(pt.x) || !approx0(pt.z))
            continue;
          break;
        default:
        case 3:
          f = pt.z;
          if (f <= 0.0001f || !approx0(pt.x) || !approx0(pt.y))
            continue;
          break;
        }
        int n = approxInt(1 / f);
        // must be positive
        if (n == 0 || nAtoms / n != 1f * nAtoms / n || n > MAX_COUNT)
          continue;
        if (n > maxF)
          maxF = n;
        if (n < minF)
          minF = n;
        counts[n]++;
      }
    }
    for (int n = maxF; n >= minF; n--) {
      if (counts[n] > 0 && counts[n] == (n - 1) * nAtoms / n) {
        P3[] oabc = uc.getUnitCellVectors();
        oabc[abc].scale(1f / n);
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
        uc = uc.getUnitCell(oabc, false, "scaled");

        for (int i = bsPoints.nextSetBit(0); i >= 0; i = bsPoints
            .nextSetBit(i + 1)) {
          float f;
          switch (abc) {
          case 1:
            f = approxInt(n * atoms[i].x);
            if (f == 0) {
              atoms[i].x *= n;
              continue;
            }
            break;
          case 2:
            f = approxInt(n * atoms[i].y);
            if (f == 0) {
              atoms[i].y *= n;
              continue;
            }
            break;
          case 3:
            f = approxInt(n * atoms[i].x);
            if (f == 0) {
              atoms[i].z *= n;
              continue;
            }
            break;
          }
          atoms[i] = null;
          bsPoints.clear(i);
        }
        break;
      }
    }
    nAtoms = bsPoints.cardinality();
    return uc;
  }

  private boolean approx0(float f) {
    return (Math.abs(f) < 0.0001f);
  }

  private int approxInt(float finv) {
    int i = (int) (finv + 0.0001f);
    return (Math.abs(finv - i) < 0.0001f ? i : 0);
  }

  private int findEquiv(SymmetryOperation op, int i, BS bsPoints, P3 pt) {
    SGAtom a = atoms[i];
    pt.setT(a);
    op.rotTrans(pt);
    SimpleUnitCell.unitizeDim(3, pt);
    if (pt.distanceSquared(a) == 0) {
      return i;
    }
    int type = a.type;
    for (int j = nAtoms; --j >= 0;) {
      SGAtom b = atoms[j];
      float d = b.distanceSquared(pt);
      if (d < 0.001f) {
        bsPoints.clear(j);
        return (b.type == type ? j : -1);
      }
    }
    return -1;
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


}
