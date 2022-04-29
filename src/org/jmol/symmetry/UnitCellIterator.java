package org.jmol.symmetry;

import org.jmol.api.AtomIndexIterator;
import org.jmol.api.SymmetryInterface;
import org.jmol.atomdata.RadiusData;
import org.jmol.modelset.Atom;
import org.jmol.modelset.ModelSet;
import org.jmol.util.BoxInfo;
import org.jmol.util.Logger;
import org.jmol.util.Point3fi;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.M4d;
import javajs.util.P3d;
import javajs.util.P3i;
import javajs.util.T3d;

public class UnitCellIterator implements AtomIndexIterator {

  private Atom[] atoms;
  private T3d center;
  private T3d translation;
  private int nFound;
  private double maxDistance2;
  private double distance2;
  private SymmetryInterface unitCell;
  private P3i minXYZ, maxXYZ, t;
  private P3d p;
  private int ipt = Integer.MIN_VALUE;
  private Lst<P3d[]> unitList;
  private boolean done;
  private int nAtoms;
  private int listPt;

  public UnitCellIterator() {
    // for reflection
  }

  /**
   * 
   * @param unitCell
   * @param atom
   * @param atoms
   * @param bsAtoms
   * @param distance
   *        <= 0 indicates that distance will be set later, probably from a
   *        point
   * @return this
   */
  public UnitCellIterator set(SymmetryInterface unitCell, Atom atom,
                              Atom[] atoms, BS bsAtoms, double distance) {
    this.unitCell = unitCell;
    this.atoms = atoms;
    addAtoms(bsAtoms);
    p = new P3d();
    if (distance > 0)
      setCenter(atom, distance);
    return this;
  }

  @Override
  public void setModel(ModelSet modelSet, int modelIndex, int zeroBase,
                       int atomIndex, T3d center, double distance, RadiusData rd) {
    // not implemented for UnitCell iterator
  }

  @Override
  public void setCenter(T3d center, double distance) {
    if (distance == 0)
      return;
    maxDistance2 = distance * distance;
    this.center = center;
    translation = new P3d();
    T3d[] pts = BoxInfo.unitCubePoints;
    P3d min = P3d.new3(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
    P3d max = P3d.new3(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);
    p = new P3d();
    P3d ptC = new P3d();
    ptC.setT(center);
    unitCell.toFractional(ptC, true);
    for (int i = 0; i < 8; i++) {
      p.scaleAdd2(-2d, pts[i], pts[7]);
      p.scaleAdd2(distance, p, center);
      unitCell.toFractional(p, true);
      if (min.x > p.x)
        min.x = p.x;
      if (max.x < p.x)
        max.x = p.x;
      if (min.y > p.y)
        min.y = p.y;
      if (max.y < p.y)
        max.y = p.y;
      if (min.z > p.z)
        min.z = p.z;
      if (max.z < p.z)
        max.z = p.z;
    }
    minXYZ = P3i.new3((int) Math.floor(min.x), (int) Math.floor(min.y),
        (int) Math.floor(min.z));
    maxXYZ = P3i.new3((int) Math.ceil(max.x), (int) Math.ceil(max.y),
        (int) Math.ceil(max.z));
    if (Logger.debugging)
      Logger.info("UnitCellIterator minxyz/maxxyz " + minXYZ + " " + maxXYZ);
    t = P3i.new3(minXYZ.x - 1, minXYZ.y, minXYZ.z);
    nextCell();
  }

  @Override
  public void addAtoms(BS bsAtoms) {
    done = (bsAtoms == null);
    if (done)
      return;
    unitList = new Lst<P3d[]>();
    String cat = "";
    M4d[] ops = unitCell.getSymmetryOperations();
    int nOps = ops.length;
    P3d pt = new P3d();
    P3d pa = new P3d();
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
      Atom a = atoms[i];
      for (int j = 0; j < nOps; j++) {
        if (j > 0) {
          pt.setT(a);
          unitCell.toFractional(pt, false);
          ops[j].rotTrans(pt);
          unitCell.unitize(pt);
          unitCell.toCartesian(pt, false);
          pt.putP(pa);
        } else {
          pa.setT(a);
          unitCell.toUnitCellD(pa, null);
        }
        String key = "_" + (int) (pa.x * 100) + "_" + (int) (pa.y * 100) + "_"
            + (int) (pa.z * 100) + "_";
        if (cat.indexOf(key) >= 0)
          continue;
        cat += key;
        unitList.addLast(new P3d[] { a, pa });
        pa = new P3d();
      }
    }
    nAtoms = unitList.size();
    done = (nAtoms == 0);
    if (Logger.debugging)
      Logger.info("UnitCellIterator " + nAtoms + " unique points found");
  }

  @Override
  public boolean hasNext() {
    while ((ipt < nAtoms || nextCell())) {
      p.add2(unitList.get(listPt = ipt++)[1], translation);
      if ((distance2 = p.distanceSquared(center)) < maxDistance2
          && distance2 > 0.1d) {
        nFound++;
        return true;
      }
    }
    return false;
  }

  private boolean nextCell() {
    if (done)
      return false;
    if (++t.x >= maxXYZ.x) {
      t.x = minXYZ.x;
      if (++t.y >= maxXYZ.y) {
        t.y = minXYZ.y;
        if (++t.z >= maxXYZ.z) {
          done = true;
          ipt = nAtoms;
          return false;
        }
      }
    }
    translation.set(t.x, t.y, t.z);
    unitCell.toCartesian(translation, false);
    ipt = 0;
    return true;
  }

  @Override
  public int next() {
    return (done || ipt < 0 ? -1 : getAtom().i);
  }

  private Atom getAtom() {
    return ((Atom) unitList.get(listPt)[0]);
  }

  @Override
  public double foundDistance2() {
    return (nFound > 0 ? distance2 : Double.MAX_VALUE);
  }

  @Override
  public P3d getPosition() {
    Atom a = getAtom();
    if (Logger.debugging)
      Logger.info("draw ID p_" + nFound + " " + p + " //" + a + " " + t);
    if (this.p.distanceSquared(a) < 0.0001f)
      return a;
    Point3fi p = new Point3fi();
    p.setT(this.p);
    p.i = a.i;
    p.sD = (short) a.getElementNumber();
    return p;
  }

  @Override
  public void release() {
    atoms = null;
    center = null;
    translation = null;
  }
}
