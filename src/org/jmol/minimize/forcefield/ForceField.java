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

package org.jmol.minimize.forcefield;


import java.io.BufferedReader;
import java.io.IOException;

import javajs.util.PT;

import org.jmol.java.BS;
import org.jmol.minimize.MinAngle;
import org.jmol.minimize.MinAtom;
import org.jmol.minimize.MinBond;
import org.jmol.minimize.MinPosition;
import org.jmol.minimize.MinTorsion;
import org.jmol.minimize.Minimizer;
import org.jmol.minimize.Util;
import org.jmol.util.Logger;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JmolAsyncException;
import org.jmol.viewer.Viewer;

abstract public class ForceField {

  // same flags as for openBabel:
  
  // terms
  final static int ENERGY = (1 << 0); //!< all terms
  final static int EBOND = (1 << 1); //!< bond term
  final static int EANGLE = (1 << 2); //!< angle term
  final static int ESTRBND = (1 << 3); //!< strbnd term
  final static int ETORSION = (1 << 4); //!< torsion term
  final static int EOOP = (1 << 5); //!< oop term
  final static int EVDW = (1 << 6); //!< vdw term
  final static int EELECTROSTATIC = (1 << 7); //!< electrostatic term

  // indexes into the int[] array for angles and torsions for MMFF94
  
  public final static int ABI_IJ = 3; // minBond index for IJ in IJK
  public final static int ABI_JK = 4; // minBond index for JK in IJK
  
  public final static int TBI_AB = 4; // minBond index for AB in ABCD
  public final static int TBI_BC = 5; // minBond index for BC in ABCD
  public final static int TBI_CD = 6; // minBond index for CD in ABCD

  // indexes into vRings vector list of rings from SMARTS search in MMFF94
  
  public static final int R3 = 0;
  public static final int R4 = 1;
  public static final int R5 = 2;
  public static final int Raromatic = 3;
  
  public String name;

  Calculations calc;
  
  private double criterion, e0, dE; 
  int currentStep;
  private int stepMax;
  private double[][] coordSaved;  

  int minAtomCount; 
  int minBondCount;

  MinAtom[] minAtoms;
  MinBond[] minBonds;
  MinAngle[] minAngles;
  MinTorsion[] minTorsions;
  MinPosition[] minPositions;
  BS bsFixed;
  
  Minimizer minimizer;

  abstract public void clear();
  abstract public boolean setModel(BS bsElements, int elemnoMax) throws JmolAsyncException;
  
  protected void setModelFields() {   
    this.minAtoms = minimizer.minAtoms;
    this.minBonds = minimizer.minBonds;
    this.minAngles = minimizer.minAngles;
    this.minTorsions = minimizer.minTorsions;
//    this.minPositions = minimizer.minPositions; // not implemented
    this.bsFixed = minimizer.bsMinFixed;
    minAtomCount = minAtoms.length;
    minBondCount = minBonds.length;
  }
  
  public void setConstraints(Minimizer m) {
    this.bsFixed = m.bsMinFixed;
    calc.setConstraints(m.constraints);
    coordSaved = null;
  }
    
  //////////////////////////////////////////////////////////////////////////////////
  //
  // Energy Minimization
  //
  //////////////////////////////////////////////////////////////////////////////////

  ////////////// calculation /////////////////
  
  public void steepestDescentInitialize(int stepMax, double criterion) {
    this.stepMax = stepMax;//1000
    // The criterion must be in the units of the calculation.
    // However, the user is setting this, so they will be in Minimizer units.
    // 
    this.criterion = criterion / toUserUnits(1); //1e-3
    currentStep = 0;
    clearForces();
    calc.setLoggingEnabled(true);
    calc.setLoggingEnabled(stepMax == 0 || Logger.isActiveLevel(Logger.LEVEL_DEBUGHIGH));
    String s = name + " " + calc.getDebugHeader(-1) + "Jmol Minimization Version " + Viewer.getJmolVersion() + "\n";
    calc.appendLogData(s);
    Logger.info(s);
    calc.getConstraintList();
    if (calc.loggingEnabled)
      calc.appendLogData(calc.getAtomList("S T E E P E S T   D E S C E N T"));
    dE = 0;
    calc.setPreliminary(stepMax > 0);
    e0 = energyFull(false, false);
    s = PT.sprintf(" Initial " + name + " E = %10.3f " + minimizer.units + " criterion = %8.6f max steps = " + stepMax, 
        "ff", new Object[] {Float.valueOf(toUserUnits(e0)), Float.valueOf(toUserUnits(criterion)) });
    minimizer.report(s, false);
    calc.appendLogData(s);
  }

  private void clearForces() {
    for (int i = 0; i < minAtomCount; i++)
      minAtoms[i].force[0] = minAtoms[i].force[1] = minAtoms[i].force[2] = 0; 
  }
  
  //Vector3d dir = new Vector3d();
  public boolean steepestDescentTakeNSteps(int n) {
    if (stepMax == 0)
      return false;
    boolean isPreliminary = true;
    for (int iStep = 1; iStep <= n; iStep++) {
      currentStep++;
      calc.setSilent(true);
      for (int i = 0; i < minAtomCount; i++)
        if (bsFixed == null || !bsFixed.get(i))
          setForcesUsingNumericalDerivative(minAtoms[i], ENERGY);
      linearSearch();
      calc.setSilent(false);

      if (calc.loggingEnabled)
        calc.appendLogData(calc.getAtomList("S T E P    " + currentStep));

      double e1 = energyFull(false, false);
      dE = e1 - e0;
      boolean done = Util.isNear3(e1, e0, criterion);

      if (done || currentStep % 10 == 0 || stepMax <= currentStep) {
        String s = PT.sprintf(name + " Step %-4d E = %10.6f    dE = %8.6f ",
            "Fi", new Object[] {new float[] { (float) e1, (float) (dE), (float) criterion },
            Integer.valueOf(currentStep) });
        minimizer.report(s, false);
        calc.appendLogData(s);
      }
      e0 = e1;
      if (done || stepMax <= currentStep) {
        if (calc.loggingEnabled)
          calc.appendLogData(calc.getAtomList("F I N A L  G E O M E T R Y"));
        if (done) {
          String s = PT.formatStringF(
              "\n    " + name + " STEEPEST DESCENT HAS CONVERGED: E = %8.5f " + minimizer.units + " after " + currentStep + " steps", "f",
              toUserUnits(e1));
          calc.appendLogData(s);
          minimizer.report(s, true);
          Logger.info(s);
        }
        return false;
      }
      //System.out.println(isPreliminary + " " + getNormalizedDE() + " " + currentStep);
      if (isPreliminary && getNormalizedDE() >= 2) {
        // looking back at this after some time, I don't exactly see why I wanted
        // this to stay in preliminary mode unless |DE| >= 2 * crit. 
        // It's hard to ever have |DE| NOT >= 2 * crit -- that would be very close to the criterion.
        // And when that IS the case, why would you want to STAY in preliminary mode? Hmm.
        calc.setPreliminary(isPreliminary = false);
        e0 = energyFull(false, false);
      }
    }
    return true; // continue
  }

  private double getEnergies(int terms, boolean gradients) {
    if ((terms & ENERGY) != 0)
      return energyFull(gradients, true);
    double e = 0.0;
    if ((terms & EBOND) != 0)
      e += energyBond(gradients);
    if ((terms & EANGLE) != 0)
      e += energyAngle(gradients);
    if ((terms & ESTRBND) != 0)
      e += energyStretchBend(gradients);
    if ((terms & EOOP) != 0)
      e += energyOOP(gradients);
    if ((terms & ETORSION) != 0)
      e += energyTorsion(gradients);
    if ((terms & EVDW) != 0)
      e += energyVDW(gradients);
    if ((terms & EELECTROSTATIC) != 0)
      e += energyES(gradients);
    return e;
  }

  //  
  //         f(x + delta) - f(x)
  // f'(x) = ------------------- 
  //                delta
  //
  private void setForcesUsingNumericalDerivative(MinAtom atom, int terms) {
    double delta = 1.0e-5;
    atom.force[0] = -getDE(atom, terms, 0, delta);
    atom.force[1] = -getDE(atom, terms, 1, delta);
    atom.force[2] = -getDE(atom, terms, 2, delta);
    //if (atom.atom.getAtomIndex() == 2)
      //System.out.println(" atom + " + atom.atom.getAtomIndex() + " force=" + atom.force[0] + " " + atom.force[1] + " " + atom.force[2] );
    return;
  }

  private double getDE(MinAtom atom, int terms, int i, double delta) {
    // get energy derivative
    atom.coord[i] += delta;
    double e = getEnergies(terms, false);
    atom.coord[i] -= delta;
    //if (atom.atom.getAtomIndex() == 2)
      //System.out.println ((i==0 ? "\n" : "") + "atom 3: " + atom.atom.getInfo() + " " + i + " " + (e - e0) + " " + (Point3f)atom.atom + "{" + atom.coord[0] + " " + atom.coord[1] + " " + atom.coord[2] + "} delta=" + delta);
    return (e - e0) / delta;
  }

/*  
  //  
  //          f(x + 2delta) - 2f(x + delta) + f(x)
  // f''(x) = ------------------------------------
  //                        (delta)^2        
  //
  void getNumericalSecondDerivative(MinAtom atom, int terms, Vector3d dir) {
    double delta = 1.0e-5;
    double e0 = getEnergy(terms, false);
    double dx = getDx2(atom, terms, 0, e0, delta);
    double dy = getDx2(atom, terms, 1, e0, delta);
    double dz = getDx2(atom, terms, 2, e0, delta);
    dir.set(dx, dy, dz);
  }

  private double getDx2(MinAtom atom, int terms, int i,
                                     double e0, double delta) {
    // calculate f(1)    
    atom.coord[i] += delta;
    double e1 = getEnergy(terms, false);
    // calculate f(2)
    atom.coord[i] += delta;
    double e2 = getEnergy(terms, false);
    atom.coord[i] -= 2 * delta;
    return (e2 - 2 * e1 + e0) / (delta * delta);
  }

*/  
  public double energyFull(boolean gradients, boolean isSilent) {
    double energy;

    if (gradients)
      clearForces();

    energy = energyBond(gradients) +
        energyAngle(gradients)
       + energyTorsion(gradients)
       + energyStretchBend(gradients)
       + energyOOP(gradients)
       + energyVDW(gradients)
       + energyES(gradients);

    if (!isSilent && calc.loggingEnabled)      
      calc.appendLogData(PT.sprintf("\nTOTAL %s ENERGY = %8.3f %s/mol\n", 
          "sfs", new Object[] {name, Float.valueOf(toUserUnits(energy)), minimizer.units }));
    return energy;
  }

  /**
   * 
   * @param gradients
   * @return energy
   */
  double energyStretchBend(boolean gradients) {
    return calc.energyStretchBend(gradients); 
  }

  double energyBond(boolean gradients) {
    return calc.energyBond(gradients); 
  }
  
  double energyAngle(boolean gradients) {
    return calc.energyAngle(gradients); 
  }

  double energyTorsion(boolean gradients) {
    return calc.energyTorsion(gradients); 
  }

  double energyOOP(boolean gradients) {
    return calc.energyOOP(gradients); 
  }

//  double energyPosition(boolean gradients) {
//    return calc.energyPos(gradients); 
//  }

  double energyVDW(boolean gradients) {
    return calc.energyVDW(gradients);
  }

  double energyES(boolean gradients) {
    return calc.energyES(gradients);
  }
  
  // linearSearch
  //
  // atom: coordinates of atom at iteration k (x_k)
  // direction: search direction ( d = -grad(x_0) )
  //
  // ALGORITHM:
  // 
  // step = 1
  // for (i = 1 to 100) { max steps = 100
  // e_k = energy(x_k) energy of current iteration
  // x_k = x_k + step * d update coordinates
  // e_k+1 = energy(x_k+1) energy of next iteration
  //   
  // if (e_k+1 < e_k)
  // step = step * 1.2 increase step size
  // if (e_k+1 > e_k) {
  // x_k = x_k - step * d reset coordinates to previous iteration
  // step = step * 0.5 reduce step size
  // }
  // if (e_k+1 == e_k)
  // end convergence criteria reached, stop
  // }

  private void linearSearch() {

    //double alpha = 0.0; // Scale factor along direction vector
    double step = 0.23;
    double trustRadius = 0.3; // don't move further than 0.3 Angstroms
    double trustRadius2 = trustRadius * trustRadius;

    double e1 = energyFull(false, true);

    for (int iStep = 0; iStep < 10; iStep++) {
      saveCoordinates();
      for (int i = 0; i < minAtomCount; ++i)
        if (bsFixed == null || !bsFixed.get(i)) {
          double[] force = minAtoms[i].force;
          double[] coord = minAtoms[i].coord;
          double f2 = (force[0] * force[0] + force[1] * force[1] + force[2]
              * force[2]);
          if (f2 > trustRadius2 / step / step) {
            f2 = trustRadius / Math.sqrt(f2) / step;
            // if (i == 2)
            //System.out.println("atom 3: force/coord " + force[0] + " " +
            // force[1] + " " + force[2] + "/" + coord[0] + " " + coord[1] + " "
            // + coord[2] + " " + f2);
            force[0] *= f2;
            force[1] *= f2;
            force[2] *= f2;
          }
          /*
           * if (i == 2) f.println("#atom 3; draw " + "{" + coord[0] + " " +
           * coord[1] + " " + coord[2] + "} " + "{" + (coord[0] + force[0]) +
           * " " + (coord[1] + force[1]) + " " + (coord[2] + force[2]) +"}" );
           */for (int j = 0; j < 3; ++j) {
            if (Util.isFinite(force[j])) {
              double tempStep = force[j] * step;
              if (tempStep > trustRadius)
                coord[j] += trustRadius;
              else if (tempStep < -trustRadius)
                coord[j] -= trustRadius;
              else
                coord[j] += tempStep;
            }
          }
        }

      double e2 = energyFull(false, true);

      //System.out.println("step is " + step + " " + (e2 < e1) + " " + e1 + " "
      // + e2);
      if (Util.isNear3(e2, e1, 1.0e-3))
        break;
      if (e2 > e1) {
        step *= 0.1;
        restoreCoordinates();
      } else if (e2 < e1) {
        e1 = e2;
        //alpha += step;
        step *= 2.15;
        if (step > 1.0)
          step = 1.0;
      }
    }
    //System.out.println("alpha = " + alpha);
  }

  private void saveCoordinates() {
    if (coordSaved == null)
      coordSaved = new double[minAtomCount][3];
    for (int i = 0; i < minAtomCount; i++) 
      for (int j = 0; j < 3; j++)
        coordSaved[i][j] = minAtoms[i].coord[j];
  }
  
  private void restoreCoordinates() {
    for (int i = 0; i < minAtomCount; i++) 
      for (int j = 0; j < 3; j++)
        minAtoms[i].coord[j] = coordSaved[i][j];
  }
  
  public boolean detectExplosion() {
    for (int i = 0; i < minAtomCount; i++) {
      MinAtom atom = minAtoms[i];
      for (int j = 0; j < 3; j++)
        if (!Util.isFinite(atom.coord[j]))
          return true;
    }
    for (int i = 0; i < minBondCount; i++) {
      MinBond bond = minBonds[i];
      if (Util.distance2(minAtoms[bond.data[0]].coord,
          minAtoms[bond.data[1]].coord) > 900.0)
        return true;
    }
    return false;
  }

  public int getCurrentStep() {
    return currentStep;
  }

  public double getEnergy() {
    return e0;
  }
  
  public String getAtomList(String title) {
    return calc.getAtomList(title);
  }

  public double getEnergyDiff() {
    return dE;
  }

  public String getLogData() {
    return calc.getLogData();
  }
  
  double getNormalizedDE() {
    return Math.abs(dE/criterion);
  }

  public float toUserUnits(double energy) {
    return toUnits(energy, calc.getUnits());
  }
  
  private float toUnits(double energy, String units) {
    return (float) (units.equalsIgnoreCase(minimizer.units) ? energy : energy
        * (minimizer.units.equals("kJ") ? Calculations.KCAL_TO_KJ
            : 1 / Calculations.KCAL_TO_KJ));
  }

  public void log(String s) {
    calc.appendLogData(s);
  }

  protected BufferedReader getBufferedReader(String resourceName) throws IOException {
    return FileManager.getBufferedReaderForResource(minimizer.vwr, this,
        "org/jmol/minimize/forcefield/", "data/" + resourceName);
  }

}
