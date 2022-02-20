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

import javajs.util.AU;
import javajs.util.Lst;
import java.util.Hashtable;

import java.util.Map;

import org.jmol.i18n.GT;
import javajs.util.BS;
import org.jmol.minimize.forcefield.ForceField;
import org.jmol.minimize.forcefield.ForceFieldMMFF;
import org.jmol.minimize.forcefield.ForceFieldUFF;
import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.Bond;
import org.jmol.modelset.ModelSet;
import org.jmol.thread.JmolThread;
import org.jmol.util.BSUtil;
import org.jmol.util.Escape;
import org.jmol.util.Edge;
import org.jmol.util.Logger;

import org.jmol.script.T;
import org.jmol.viewer.JmolAsyncException;
import org.jmol.viewer.Viewer;

public class Minimizer {

  public Viewer vwr;
  public Atom[] atoms;
  public Bond[] bonds;
  public int rawBondCount;
  
  public MinAtom[] minAtoms;
  public MinBond[] minBonds;
  public MinAngle[] minAngles;
  public MinTorsion[] minTorsions;
  public MinPosition[] minPositions;
  
  public BS bsMinFixed;
  private int ac;
  private int bondCount;
  private int[] atomMap; 
 
  public double[] partialCharges;
  
  private int steps = 50;
  private double crit = 1e-3;

  public String units = "kJ/mol";
  
  private ForceField pFF;
  private String ff = "UFF";
  private BS bsTaint, bsSelected;
  public BS bsAtoms;
  private BS bsFixedDefault;
  private BS bsFixed;
  
  public Lst<MMConstraint> constraints;
  
  private boolean isSilent;
 
  public Minimizer() {
  }

  
  public Minimizer setProperty(String propertyName, Object value) {
    switch (("ff        " + "cancel    " + "clear     " + "constraint"
        +    "fixed     " + "stop      " + "vwr    ").indexOf(propertyName)) {
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
                          int flags, String ff)
      throws JmolAsyncException {
    isSilent = ((flags & Viewer.MIN_SILENT) == Viewer.MIN_SILENT);
    isQuick = (ff.indexOf("2D") >= 0
        || (flags & Viewer.MIN_QUICK) == Viewer.MIN_QUICK);
    boolean haveFixed = ((flags
        & Viewer.MIN_HAVE_FIXED) == Viewer.MIN_HAVE_FIXED);
    BS bsXx = ((flags & Viewer.MIN_XX) == Viewer.MIN_XX ? new BS() : null);
    Object val;
    if (crit <= 0) {
      val = vwr.getP("minimizationCriterion");
      if (val != null && val instanceof Float)
        crit = ((Float) val).floatValue();
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

      // if the user indicated minimize ... FIX ... or we don't have any defualt,
      // use the bsFixed coming in here, which is set to "nearby and in frame" in that case. 
      // and if something is fixed, then AND it with "nearby and in frame" as well.
      if (!haveFixed && bsFixedDefault != null)
        bsFixed.and(bsFixedDefault);
      if (minimizationOn)
        return false;
      ForceField pFF0 = pFF;
      getForceField(ff);
      if (pFF == null) {
        Logger.error(GT.o(GT.$("Could not get class for force field {0}"), ff));
        return false;
      }
      Logger.info("minimize: initializing " + pFF.name + " (steps = " + steps
          + " criterion = " + crit + ")"
                + " silent=" + isSilent 
                + " quick=" + isQuick 
                + " fixed=" + haveFixed 
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
            Logger.info("minimize: Ignoring Xx for atomIndex=" + i);
          } else {
            bsXx.set(i);
            Logger.info("minimize: Setting Xx to fluorine for atomIndex=" + i);
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
      if ((!sameAtoms || !BSUtil.areEqual(bsFixed, this.bsFixed))
          && !setupMinimization()) {
        clear();
        return false;
      }
      if (steps > 0) {
        bsTaint = BSUtil.copy(bsAtoms);
        BSUtil.andNot(bsTaint, bsFixed);
        vwr.ms.setTaintedAtoms(bsTaint, AtomCollection.TAINT_COORD);
      }
      if (bsFixed != null)
        this.bsFixed = bsFixed;
      setAtomPositions();

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
    double value = ((Float) o[1]).doubleValue();
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
    partialCharges = null;
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

  private boolean setupMinimization() throws JmolAsyncException {

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
      minAtoms[pt] = new MinAtom(pt, atom, new double[] { atom.x, atom.y,
          atom.z }, ac);
      minAtoms[pt].sType = atom.getAtomName();
    }

    Logger.info(GT.i(GT.$("{0} atoms will be minimized."), ac));
    Logger.info("minimize: getting bonds...");
    bonds = vwr.ms.bo;
    rawBondCount = vwr.ms.bondCount;
    getBonds();
    Logger.info("minimize: getting angles...");
    getAngles();
    Logger.info("minimize: getting torsions...");
    getTorsions();
    return setModel(bsElements);
  }
  
  private boolean setModel(BS bsElements) throws JmolAsyncException {
    if (!pFF.setModel(bsElements, elemnoMax)) {
      //pFF.log("could not setup force field " + ff);
      Logger.error(GT.o(GT.$("could not setup force field {0}"), ff));
      if (ff.startsWith("MMFF")) {
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
    bsMinFixed = null;
    if (bsFixed != null) {
      bsMinFixed = new BS();
      for (int i = bsAtoms.nextSetBit(0), pt = 0; i >= 0; i = bsAtoms
          .nextSetBit(i + 1), pt++)
        if (bsFixed.get(i))
          bsMinFixed.set(pt);
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
      int bondOrder = bond.getCovalentOrder();
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
/*            System.out.println (" " 
                + minAtoms[ia].getIdentity() + " -- " 
                + minAtoms[ib].getIdentity() + " -- " 
                + minAtoms[ic].getIdentity());
*/
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
/*
            System.out.println ("a " 
                + minAtoms[ic].getIdentity() + " -- " 
                + minAtoms[ia].getIdentity() + " -- " 
                + minAtoms[ib].getIdentity());
*/            
          }
      }
    }
    minAngles = vAngles.toArray(new MinAngle[vAngles.size()]);
    Logger.info(minAngles.length + " angles");
  }

  public void getTorsions() {
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
              minAtoms[Math.min(ia, id)].bs14.set(Math.max(ia, id));
/*            System.out.println("t " + minAtoms[ia].getIdentity() + " -- "
                + minAtoms[ib].getIdentity() + " -- "
                + minAtoms[ic].getIdentity() + " -- "
                + minAtoms[id].getIdentity());
*/
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
            minAtoms[Math.min(ic, id)].bs14.set(Math.max(ic, id));
/*            System.out.println("t " + minAtoms[ic].getIdentity() + " -- "
                + minAtoms[ib].getIdentity() + " -- "
                + minAtoms[ia].getIdentity() + " -- "
                + minAtoms[id].getIdentity());
*/
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
    //Logger.info("minimize: forcefield = " + pFF);
    return pFF;
  }
  
  /* ***************************************************************
   * Minimization thead support
   ****************************************************************/

  private boolean minimizationOn;
  
  public boolean minimizationOn() {
    return minimizationOn;
  }

  private MinimizationThread minimizationThread;
  
  
  public JmolThread getThread() {
    return minimizationThread;
  }

  private void setMinimizationOn(boolean minimizationOn) {
    this.minimizationOn = minimizationOn;
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
    pFF.steepestDescentInitialize(steps, crit);      
    vwr.setFloatProperty("_minimizationEnergyDiff", 0);
    reportEnergy();
    vwr.setStringProperty("_minimizationStatus", "calculate");
    vwr.notifyMinimizationStatus();
  }
  
  private void reportEnergy() {
    vwr.setFloatProperty("_minimizationEnergy", pFF.toUserUnits(pFF.getEnergy()));
  }

  
  public boolean startMinimization() {
   try {
      Logger.info("minimize: startMinimization");
      vwr.setIntProperty("_minimizationStep", 0);
      vwr.setStringProperty("_minimizationStatus", "starting");
      vwr.setFloatProperty("_minimizationEnergy", 0);
      vwr.setFloatProperty("_minimizationEnergyDiff", 0);
      vwr.notifyMinimizationStatus();
      vwr.stm.saveCoordinates("minimize", bsTaint);
      pFF.steepestDescentInitialize(steps, crit);
      reportEnergy();
      saveCoordinates();
    } catch (Exception e) {
      Logger.error("minimization error vwr=" + vwr + " pFF = " + pFF);
      return false;
    }
    minimizationOn = true;
    return true;
  }

  
  public boolean stepMinimization() {
    if (!minimizationOn)
      return false;
    boolean doRefresh = (!isSilent && vwr.getBooleanProperty("minimizationRefresh"));
    vwr.setStringProperty("_minimizationStatus", "running");
    boolean going = pFF.steepestDescentTakeNSteps(1);
    int currentStep = pFF.getCurrentStep();
    vwr.setIntProperty("_minimizationStep", currentStep);
    reportEnergy();
    vwr.setFloatProperty("_minimizationEnergyDiff", pFF.toUserUnits(pFF.getEnergyDiff()));
    vwr.notifyMinimizationStatus();
    if (doRefresh) {
      updateAtomXYZ();
      vwr.refresh(Viewer.REFRESH_SYNC_MASK, "minimization step " + currentStep);
    }
    return going;
  }

  
  public void endMinimization() {
    updateAtomXYZ();
    setMinimizationOn(false);
    if (pFF == null) {
      System.out.println("pFF was null");
    } else {
      boolean failed = pFF.detectExplosion();
      if (failed)
        restoreCoordinates();
      vwr.setIntProperty("_minimizationStep", pFF.getCurrentStep());
      reportEnergy();
      vwr.setStringProperty("_minimizationStatus", (failed ? "failed" : "done"));
      vwr.notifyMinimizationStatus();
      vwr.refresh(Viewer.REFRESH_SYNC_MASK, "minimize:done"
          + (failed ? " EXPLODED" : "OK"));
    }
    Logger.info("minimize: endMinimization");
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
    updateAtomXYZ();
  }

  public void stopMinimization(boolean coordAreOK) {
    if (!minimizationOn)
      return;
    setMinimizationOn(false);
    if (coordAreOK)
      endMinimization();
    else
      restoreCoordinates();
  }
  
  void updateAtomXYZ() {
    if (steps <= 0)
      return;
    for (int i = 0; i < ac; i++) {
      MinAtom minAtom = minAtoms[i];
      Atom atom = minAtom.atom;
      atom.x = (float) minAtom.coord[0];
      atom.y = (float) minAtom.coord[1];
      atom.z = (float) minAtom.coord[2];
    }
    vwr.refreshMeasures(false);
  }

  private void minimizeWithoutThread() {
    //for batch operation
    if (!startMinimization())
      return;
    while (stepMinimization()) {
    }
    endMinimization();
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

}
