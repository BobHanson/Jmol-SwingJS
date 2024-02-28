/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-11-23 12:49:25 -0600 (Fri, 23 Nov 2007) $
 * $Revision: 8655 $
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

package org.jmol.minimize;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.i18n.GT;
import org.jmol.minimize.forcefield.ForceField;
import org.jmol.minimize.forcefield.ForceFieldMMFF;
import org.jmol.minimize.forcefield.ForceFieldUFF;
import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.Bond;
import org.jmol.modelset.ModelSet;
import org.jmol.script.T;
import org.jmol.thread.JmolThread;
import org.jmol.util.BSUtil;
import org.jmol.util.Edge;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.viewer.JmolAsyncException;
import org.jmol.viewer.Viewer;

import javajs.util.AU;
import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.P3d;

public class Minimizer {

  public static int staticID = 0;
  
  public int id;
  public Viewer vwr;
  public Atom[] atoms;
  public Bond[] bonds;
  public int rawBondCount;
  public BS bsAtoms;

  public Lst<MMConstraint> constraints;

  public MinAtom[] minAtoms;
  public MinBond[] minBonds;
  public MinAngle[] minAngles;
  public MinTorsion[] minTorsions;
  
  public BS bsMinFixed;
  private int ac;
  private int bondCount;
  private int[] atomMap; 
 
  private int steps = 50;
  private double crit = 1e-3;

  public String units = "kJ/mol";
  
  private ForceField pFF;
  private String ff = "UFF";
  private BS bsTaint, bsSelected;
  private BS bsFixedDefault;
  private BS bsFixed;

  private boolean modelkitMinimizing;
  private BS bsBasis;
  
  private boolean isSilent;
 
  public Minimizer() {
    id = (++staticID) * 100;
  }

  
  public Minimizer setProperty(String propertyName, Object value) {
    switch ((
        "ff        " + // 0
        "cancel    " + // 10
        "clear     " + // 20
        "constraint" + // 30
        "fixed     " + // 40
        "stop      " + // 50
        "vwr    "
        ).indexOf(propertyName)) {
    case 0:
      // UFF or MMFF
      if (!ff.equals(value)) {
        setProperty("clear", null);
        ff = (String) value;
      }
      break;
    case 10:
      stopMinimization(false);
      break;
    case 20:
      if (minAtoms != null) {
        stopMinimization(false);
        clear();
      }
      break;
    case 30:
      addConstraint((Object[]) value);
      break;
    case 40:
      bsFixedDefault = (BS) value;
      if (bsFixedDefault != null && bsFixedDefault.cardinality() == 0)
        bsFixedDefault = null;
      break;
    case 50:
      stopMinimization(true);
      break;
    case 60:
      vwr = (Viewer) value;
      break;
    }
    return this;
  }

  public boolean minimize(int steps, double crit, BS bsSelected, BS bsFixed,
                          BS bsBasis, int flags, String ff)
      throws JmolAsyncException {
    id++;
    isSilent = ((flags & Viewer.MIN_SILENT) == Viewer.MIN_SILENT);
    isQuick = (ff.indexOf("2D") >= 0
        || (flags & Viewer.MIN_QUICK) == Viewer.MIN_QUICK);
    modelkitMinimizing = (bsBasis != null && vwr.getModelkitPropertySafely("minimizing") == Boolean.TRUE);
    if (bsBasis != null) {
      if (bsFixed == null)
        bsFixed = new BS();
      vwr.getMotionFixedAtoms(null, bsFixed);
      bsBasis.andNot(bsFixed);
      bsFixed.or(bsSelected);
      bsFixed.andNot(bsBasis);
      if (bsBasis.isEmpty()) {
        report(" symmetry-based minimization failed -- all atoms are fully constrained", false);
        return false;
      }
      int n = bsBasis.cardinality();
      report(" symmetry-based minimization for " + n + " atom" + (n == 1 ? "" : "s"), false);
    }
    this.bsBasis = bsBasis;
    trustRadius = (bsBasis == null ? 0.3 : 0.01);
    boolean haveFixed = ((flags
        & Viewer.MIN_HAVE_FIXED) == Viewer.MIN_HAVE_FIXED);
    BS bsXx = ((flags & Viewer.MIN_XX) == Viewer.MIN_XX ? new BS() : null);
    Object val;
    if (crit <= 0) {
      val = vwr.getP("minimizationCriterion");
      if (val != null && val instanceof Double)
        crit = ((Double) val).doubleValue();
    }
    this.crit = Math.max(crit, 0.0001);
    if (steps == Integer.MAX_VALUE) {
      val = vwr.getP("minimizationSteps");
      if (val != null && val instanceof Integer)
        steps = ((Integer) val).intValue();
    }
    this.steps = steps;
    try {
      setEnergyUnits();

      // if the user indicated minimize ... FIX ... or we don't have any default,
      // use the bsFixed coming in here, which is set to "nearby and in frame" in that case. 
      // and if something is fixed, then AND it with "nearby and in frame" as well.
      if (!haveFixed && bsFixedDefault != null)
        bsFixed.and(bsFixedDefault);
      if (minimizing)
        return false;
      ForceField pFF0 = pFF;
      getForceField(ff);
      if (pFF == null) {
        Logger.error(GT.o(GT.$("Could not get class for force field {0}"), ff));
        return false;
      }
      Logger.info("minimize: " + id + " initializing " + pFF.name + " (steps = " + steps
          + " criterion = " + crit + ")"
                + " silent=" + isSilent 
                + " quick=" + isQuick 
                + " fixed=" + haveFixed
                + " bsSelected=" + bsSelected
                + " bsFixed=" + bsFixed
                + " bsFixedDefault=" + bsFixedDefault
                + " Xx=" + (bsXx != null)
              + " ...");
      if (bsSelected.nextSetBit(0) < 0) {
        Logger.error(GT.$("No atoms selected -- nothing to do!"));
        return false;
      }
      atoms = vwr.ms.at;
      bsAtoms = BSUtil.copy(bsSelected);
      
      for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms
          .nextSetBit(i + 1)) {
        if (atoms[i].getElementNumber() == 0) {
          if (bsXx == null) {
            bsAtoms.clear(i);
            Logger.info("minimize: " + id + " Ignoring Xx for atomIndex=" + i);
          } else {
            bsXx.set(i);
            Logger.info("minimize: " + id + " Setting Xx to fluorine for atomIndex=" + i);
            atoms[i].setAtomicAndIsotopeNumber(9); // Xx -> fluorine
          }
        }
      }
      if (bsFixed != null)
        bsAtoms.or(bsFixed);
      ac = bsAtoms.cardinality();

      boolean sameAtoms = BSUtil.areEqual(bsSelected, this.bsSelected);
      this.bsSelected = bsSelected;
      if (pFF0 != null && pFF != pFF0)
        sameAtoms = false;
      if (!sameAtoms)
        pFF.clear();
      boolean isSame = (sameAtoms && BSUtil.areEqual(bsFixed, this.bsFixed));
      if (!setupMinimization(bsFixed, isSame)) {
        clear();
        return false;
      }
      if (steps > 0) {
        bsTaint = BSUtil.copy(bsAtoms);
        BSUtil.andNot(bsTaint, bsFixed);
        vwr.ms.setTaintedAtoms(bsTaint, AtomCollection.TAINT_COORD);
      }
      if (constraints != null)
        for (int i = constraints.size(); --i >= 0;)
          constraints.get(i).set(steps, bsAtoms, atomMap);

      pFF.setConstraints(this);

      // minimize and store values

      if (steps <= 0)
        getEnergyOnly();
      else if (isSilent || !vwr.useMinimizationThread())
        minimizeWithoutThread();
      else
        setMinimizationOn(true);
    } finally {
      if (bsXx != null && !bsXx.isEmpty()) {
        for (int i = bsXx.nextSetBit(0); i >= 0; i = bsXx.nextSetBit(i + 1)) {
          atoms[i].setAtomicAndIsotopeNumber(0);
        }
      }
    }
    return true;
  }
  
  /**
   * @param propertyName 
   * @param param  
   * @return Object
   */
  public Object getProperty(String propertyName, int param) {
    if (propertyName.equals("log")) {
      return (pFF == null ? "" : pFF.getLogData());
    }
    if (propertyName.equals("fixed")) {
      return bsFixedDefault;
    }
    return null;
  }
  
  private Map<String, MMConstraint> constraintMap;
  private int elemnoMax;
  private boolean isQuick;

  /**
   * 
   * @param o [ [natoms a1 a2 a3...] value ]
   */
  private void addConstraint(Object[] o) {
    if (o == null)
      return;
    int[] indexes = (int[]) o[0];
    int nAtoms = indexes[0];
    if (nAtoms == 0) {
      constraints = null;
      return;
    }
    double value = ((Double) o[1]).doubleValue();
    if (constraints == null) {
      constraints = new  Lst<MMConstraint>();
      constraintMap = new Hashtable<String, MMConstraint>();
    }
    if (indexes[1] > indexes[nAtoms]) {
        AU.swapInt(indexes, 1, nAtoms);
        if (nAtoms == 4)
          AU.swapInt(indexes, 2, 3);
    }
    String id = Escape.eAI(indexes);
    MMConstraint c = constraintMap.get(id);
    if (c == null) {
      c = new MMConstraint(indexes, value);
    } else {
      c.value = value; // just set target value
      return;
    }
    constraintMap.put(id, c);
    constraints.addLast(c);
  }
    
  private void clear() {
    setMinimizationOn(false);
    ac = 0;
    bondCount = 0;
    atoms = null;
    bonds = null;
    rawBondCount = 0;
    minAtoms = null;
    minBonds = null;
    minAngles = null;
    minTorsions = null;
    coordSaved = null;
    atomMap = null;
    bsTaint = null;
    bsAtoms = null;
    bsFixed = null;
    bsFixedDefault = null;
    bsMinFixed = null;
    bsSelected = null;
    constraints = null;
    constraintMap = null;
    pFF = null;
  }
  
  private void setEnergyUnits() {
    String s = vwr.g.energyUnits;
    units = (s.equalsIgnoreCase("kcal") ? "kcal" : "kJ");
  }

  private boolean setupMinimization(BS bsFixed, boolean isSame)
      throws JmolAsyncException {
    if (isSame) {
      setAtomPositions();
      return true;
    }
    coordSaved = null;
    atomMap = new int[atoms.length];
    minAtoms = new MinAtom[ac];
    elemnoMax = 0;
    BS bsElements = new BS();
    for (int i = bsAtoms.nextSetBit(0), pt = 0; i >= 0; i = bsAtoms
        .nextSetBit(i + 1), pt++) {
      Atom atom = atoms[i];
      atomMap[i] = pt;
      int atomicNo = atoms[i].getElementNumber();
      elemnoMax = Math.max(elemnoMax, atomicNo);
      bsElements.set(atomicNo);
      minAtoms[pt] = new MinAtom(pt, atom,
          new double[] { atom.x, atom.y, atom.z }, ac);
      minAtoms[pt].sType = atom.getAtomName();
    }
    if (bsFixed != null)
      this.bsFixed = bsFixed;
    Logger.info(GT.i(GT.$("{0} atoms will be minimized."), ac));
    Logger.info("minimize:  " + id + " getting bonds...");
    bonds = vwr.ms.bo;
    rawBondCount = vwr.ms.bondCount;
    getBonds();
    Logger.info("minimize:  " + id + " getting angles...");
    getAngles();
    Logger.info("minimize:  " + id + " getting torsions...");
    getTorsions(ff.startsWith("MMFF"));
    return setModel(bsElements);
  }
  
  private boolean setModel(BS bsElements) throws JmolAsyncException {
    if (!pFF.setModel(bsElements, elemnoMax)) {
      //pFF.log("could not setup force field " + ff);
      Logger.error(GT.o(GT.$("could not setup force field {0}"), ff));
      if (ff.startsWith("MMFF")) {
        report(" MMFF not applicable", false);
        getForceField("UFF");
        //pFF.log("could not setup force field " + ff);
        return setModel(bsElements);        
      }
      return false;
    }
    return true;
  }

  private void setAtomPositions() {
    for (int i = 0; i < ac; i++)
      minAtoms[i].set();
    if (bsFixed == null || bsFixed.cardinality() == 0) {
      bsMinFixed = null;
    } else {
      bsMinFixed = new BS();
      for (int i = 0; i < ac; i++) {
        if (bsFixed.get(minAtoms[i].atom.i))
          bsMinFixed.set(i);
      }
    }
  }

  private void getBonds() {
    Lst<MinBond> bondInfo = new  Lst<MinBond>();
    bondCount = 0;
    int i1, i2;
    for (int i = 0; i < rawBondCount; i++) {
      Bond bond = bonds[i];
      if (!bsAtoms.get(i1 = bond.atom1.i)
          || !bsAtoms.get(i2 = bond.atom2.i))
        continue;
      if (i2 < i1) {
        int ii = i1;
        i1 = i2;
        i2 = ii;
      }
      int bondOrder = (bond.isPartial() ? 0 : bond.getCovalentOrder());
      switch (bondOrder) {
      case 0:
        // hydrogen bond
        continue;
      case 1:
      case 2:
      case 3:
        break;
      case Edge.BOND_AROMATIC:
        bondOrder = 5;
        break;
      default:
        bondOrder = 1;
      }
      bondInfo.addLast(new MinBond(i, bondCount++, atomMap[i1], atomMap[i2], bondOrder, 0, null));
    }
    minBonds = new MinBond[bondCount];
    for (int i = 0; i < bondCount; i++) {
      MinBond bond = minBonds[i] = bondInfo.get(i);
      int atom1 = bond.data[0];
      int atom2 = bond.data[1];
      minAtoms[atom1].addBond(bond, atom2);
      minAtoms[atom2].addBond(bond, atom1);
    }
    for (int i = 0; i < ac; i++)
      minAtoms[i].getBondedAtomIndexes();
  }

  public void getAngles() {
    Lst<MinAngle> vAngles = new  Lst<MinAngle>();
    int[] atomList;
    int ic;
    for (int i = 0; i < bondCount; i++) {
      MinBond bond = minBonds[i];
      int ia = bond.data[0];
      int ib = bond.data[1];
      if (minAtoms[ib].nBonds > 1) {
        atomList = minAtoms[ib].getBondedAtomIndexes();
        for (int j = atomList.length; --j >= 0;)
          if ((ic = atomList[j]) > ia) {
            vAngles.addLast(new MinAngle(new int[] { ia, ib, ic, i,
                minAtoms[ib].getBondIndex(j)}));
            minAtoms[ia].bsVdw.clear(ic);
          }
      }
      if (minAtoms[ia].nBonds > 1) {
        atomList = minAtoms[ia].getBondedAtomIndexes();
        for (int j = atomList.length; --j >= 0;)
          if ((ic = atomList[j]) < ib && ic > ia) {
            vAngles
                .addLast(new MinAngle(new int[] { ic, ia, ib, minAtoms[ia].getBondIndex(j),
                    i}));
            minAtoms[ic].bsVdw.clear(ib);
          }
      }
    }
    minAngles = vAngles.toArray(new MinAngle[vAngles.size()]);
    Logger.info(minAngles.length + " angles");
  }

  public void getTorsions(boolean isMMFF) {
    Lst<MinTorsion> vTorsions = new  Lst<MinTorsion>();
    int id;
    // extend all angles a-b-c by one, but only
    // when when c > b or a > b
    for (int i = minAngles.length; --i >= 0;) {
      int[] angle = minAngles[i].data;
      int ia = angle[0];
      int ib = angle[1];
      int ic = angle[2];
      int[] atomList;
      if (ic > ib && minAtoms[ic].nBonds > 1) {
        atomList = minAtoms[ic].getBondedAtomIndexes();
        for (int j = 0; j < atomList.length; j++) {
          id = atomList[j];
          if (id != ia && id != ib) {
            vTorsions.addLast(new MinTorsion(new int[] { ia, ib, ic, id, 
                angle[ForceField.ABI_IJ], angle[ForceField.ABI_JK],
                minAtoms[ic].getBondIndex(j) }));
            if (isMMFF)
              minAtoms[Math.min(ia, id)].bs14.set(Math.max(ia, id));
          }
        }
      }
      if (ia > ib && minAtoms[ia].nBonds != 1) {
        atomList = minAtoms[ia].getBondedAtomIndexes();
        for (int j = 0; j < atomList.length; j++) {
          id = atomList[j];
          if (id != ic && id != ib) {
            vTorsions.addLast(new MinTorsion(new int[] { ic, ib, ia, id, 
                angle[ForceField.ABI_JK], angle[ForceField.ABI_IJ],
                minAtoms[ia].getBondIndex(j) }));
            if (isMMFF)
              minAtoms[Math.min(ic, id)].bs14.set(Math.max(ic, id));
          }
        }
      }
    }
    minTorsions = vTorsions.toArray(new MinTorsion[vTorsions.size()]);
    Logger.info(minTorsions.length + " torsions");
  }
  
  ///////////////////////////// minimize //////////////////////

  public ForceField getForceField(String ff) throws JmolAsyncException {
    if (ff.startsWith("MMFF"))
      ff = "MMFF";
    if (pFF == null || !ff.equals(this.ff) || (pFF.name.indexOf("2D") >= 0) != isQuick) {
      if (ff.equals("MMFF")) {
        pFF = new ForceFieldMMFF(this, isQuick);
      } else {
        // default to UFF
        pFF = new ForceFieldUFF(this, isQuick);
        ff = "UFF";
      }
      this.ff = ff;
      if (!isQuick)
        vwr.setStringProperty("_minimizationForceField", ff);
    }
    report(" forcefield is " + ff, false);
    pFF.setNth(vwr.getInt(T.minimizationreportsteps));
    return pFF;
  }
  
  /* ***************************************************************
   * Minimization thead support
   ****************************************************************/

  private boolean minimizing;
  
  public boolean minimizationOn() {
    return minimizing;
  }

  private MinimizationThread minimizationThread;
  private double trustRadius = 0.3;
  
  
  public JmolThread getThread() {
    return minimizationThread;
  }

  private void setMinimizationOn(boolean minimizationOn) {
    this.minimizing = minimizationOn;
    if (!minimizationOn) {
      if (minimizationThread != null) {
        minimizationThread = null;
      }
      return;
    }
    if (minimizationThread == null) {
      minimizationThread = new MinimizationThread();
      minimizationThread.setManager(this, vwr, null);
      minimizationThread.start();
    }
  }

  private void getEnergyOnly() {
    if (pFF == null || vwr == null)
      return;
    pFF.steepestDescentInitialize(steps, crit, trustRadius);      
    vwr.setFloatProperty("_minimizationEnergyDiff", 0);
    reportEnergy();
    vwr.setStringProperty("_minimizationStatus", "calculate");
    vwr.notifyMinimizationStatus();
    if (bsBasis != null) {
      //report(pFF.getLogData(), true);
      // to delete this model
      vwr.getModelkit(false).minimizeEnd(null, true);
    }

  }
  
  private void reportEnergy() {
    vwr.setFloatProperty("_minimizationEnergy", pFF.toUserUnits(pFF.getEnergy()));
  }

  
  public boolean startMinimization() {
   try {
      Logger.info("minimize:  " + id + " startMinimization");
      vwr.setIntProperty("_minimizationStep", 0);
      vwr.setStringProperty("_minimizationStatus", "starting");
      vwr.setFloatProperty("_minimizationEnergy", 0);
      vwr.setFloatProperty("_minimizationEnergyDiff", 0);
      vwr.notifyMinimizationStatus();
      vwr.stm.saveCoordinates("minimize", bsTaint);
      pFF.steepestDescentInitialize(steps, crit, trustRadius);
      reportEnergy();
      saveCoordinates();
    } catch (Exception e) {
      //e.printStackTrace();
      Logger.error("minimization error vwr=" + vwr + " pFF = " + pFF);
      return false;
    }
    minimizing = true;
    return true;
  }

  
  public boolean stepMinimization() {
    if (!minimizing)
      return false;
    boolean doRefresh = (!isSilent && vwr.getBooleanProperty("minimizationRefresh"));
    vwr.setStringProperty("_minimizationStatus", "running");
    boolean going = pFF.steepestDescentTakeNSteps(1, bsBasis != null);
    int currentStep = pFF.getCurrentStep();
    vwr.setIntProperty("_minimizationStep", currentStep);
    if (doRefresh) {
      vwr.refresh(Viewer.REFRESH_SYNC_MASK, "minimization step " + currentStep);
    }
    reportEnergy();
    vwr.setFloatProperty("_minimizationEnergyDiff", pFF.toUserUnits(pFF.getEnergyDiff()));
    vwr.notifyMinimizationStatus();
    if (doRefresh) {
      if (!modelkitMinimizing)
        updateAtomXYZ(false);
      vwr.refresh(3, "minimization step " + currentStep);
    }
    return going;
  }

  
  public void endMinimization(boolean normalFinish) {
    System.out.println("minimization: " + id + " end minimizing=" + minimizing + " normal=" + normalFinish);
    if (!minimizing)
      return;
    setMinimizationOn(false);
    if (pFF == null) {
      System.out.println("pFF was null");
    } else {
      boolean failed = pFF.detectExplosion();
      if (failed)
        restoreCoordinates();
      else
        updateAtomXYZ(true);
      vwr.setIntProperty("_minimizationStep", pFF.getCurrentStep());
      reportEnergy();
      vwr.setStringProperty("_minimizationStatus", (failed ? "failed" : normalFinish ? "done" : "stopped"));
      vwr.notifyMinimizationStatus();
      vwr.refresh(Viewer.REFRESH_SYNC_MASK, "minimize:done"
          + (failed ? " EXPLODED" : "OK"));
    }
    Logger.info("minimize:  " + id + " endMinimization complete");
  }

  double[][] coordSaved;
  
  private void saveCoordinates() {
    if (coordSaved == null)
      coordSaved = new double[ac][3];
    for (int i = 0; i < ac; i++) 
      for (int j = 0; j < 3; j++)
        coordSaved[i][j] = minAtoms[i].coord[j];
  }
  
  private void restoreCoordinates() {
    if (coordSaved == null)
      return;
    for (int i = 0; i < ac; i++) 
      for (int j = 0; j < 3; j++)
        minAtoms[i].coord[j] = coordSaved[i][j];
    updateAtomXYZ(true);
  }

  public void stopMinimization(boolean coordAreOK) {
    if (!minimizing)
      return;
    if (coordAreOK)
      endMinimization(false);
    else
      restoreCoordinates();
    setMinimizationOn(false);
  }
  
  private P3d p = new P3d();

  public void updateAtomXYZ(boolean isEnd) {
    if (steps <= 0 || pFF != null && pFF.getCurrentStep() == 0)
      return;
    if (!modelkitMinimizing) {
      for (int i = 0; i < ac; i++) {
        MinAtom minAtom = minAtoms[i];
        if (bsFixed == null || !bsFixed.get(minAtom.atom.i))
          minAtom.atom.set(minAtom.coord[0], minAtom.coord[1], minAtom.coord[2]);
      }
      isEnd = true;
    } else {
      Atom a;
      // only accept the minimization for the basis atoms (which won't be fixed)
      // ModelKit will transfer the corresponding symmetry changes to the other atoms
      boolean doUpdateMinAtoms = false;
      MinAtom minAtom = minAtoms[0];
      for (int i = 0; i < ac; i++) {
        minAtom = minAtoms[i];
        if (bsMinFixed != null && bsMinFixed.get(i))
          continue;
        a = minAtom.atom;
        p.set(minAtom.coord[0], minAtom.coord[1], minAtom.coord[2]);
        
        if (vwr.getModelkit(false).moveMinConstrained(a.i, p, bsAtoms) > 0) {
          doUpdateMinAtoms = true;
        }
      }
      // now transfer back all atom coordinates
      if (doUpdateMinAtoms) {
        for (int i = 0; i < ac; i++) {
          minAtom = minAtoms[i];
          minAtom.coord[0] = (a = minAtom.atom).x;
          minAtom.coord[1] = a.y;
          minAtom.coord[2] = a.z;
        }
      }
      vwr.getModelkit(false).minimizeEnd(bsBasis, isEnd);
    }
    if (isEnd) {
        //report(pFF.getLogData(), true);
      vwr.refreshMeasures(false);
    }
  }

  private void minimizeWithoutThread() {
    //for batch operation
    if (!startMinimization())
      return;
    while (stepMinimization()) {
    }
    endMinimization(true);
  }
  
  public void report(String msg, boolean isEcho) {
    if (isSilent)
      Logger.info(msg);
    else if (isEcho)
      vwr.showString(msg, false);
    else
      vwr.scriptEcho(msg);    
  }

  
  public void calculatePartialCharges(ModelSet ms, BS bsAtoms, BS bsReport) throws JmolAsyncException {
    ForceFieldMMFF ff = new ForceFieldMMFF(this, false);
    ff.setArrays(ms.at, bsAtoms, ms.bo, ms.bondCount, true, true);
    vwr.setAtomProperty(bsAtoms, T.atomtype, 0, 0, null, null,
        ff.getAtomTypeDescriptions());
   
    vwr.setAtomProperty(bsReport == null ? bsAtoms : bsReport, T.partialcharge, 0, 0, null,
        ff.getPartialCharges(), null);
  }


  public String getForceFieldUsed() {
    return (pFF == null ? null : pFF.name);
  }

  public Boolean isLoggable(int[] iData, int n) {
    if (bsBasis == null)
      return Boolean.TRUE;
    if (iData == null)
      return bsBasis.get(minAtoms[n].atom.i) ? Boolean.TRUE : Boolean.FALSE;
    for (int i = 0; i < n; i++) {
      if (bsBasis.get(minAtoms[iData[i]].atom.i))
        return Boolean.TRUE;
    }
    return Boolean.FALSE;
  }

  @Override
  public String toString() {
    return "[minimizer " + id + " step " + (pFF == null ? 0 : pFF.getCurrentStep()) + " atoms=" + ac + "]";
  }
}
