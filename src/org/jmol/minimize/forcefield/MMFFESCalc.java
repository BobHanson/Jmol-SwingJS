package org.jmol.minimize.forcefield;

import javajs.util.Lst;

class MMFFESCalc extends Calculation {

  private static final double BUFF = 0.05;

  @Override
  void setData(Lst<Object[]> calc, int ia, int ib, double d) {
    if (calcs.minAtoms[ia].partialCharge == 0 || calcs.minAtoms[ib].partialCharge == 0)
      return;
    calc.addLast(new Object[] { new int[] { ia, ib }, new double[] {
         calcs.minAtoms[ia].partialCharge, calcs.minAtoms[ib].partialCharge, 
         (calcs.minAtoms[ia].bs14.get(ib) ? 249.0537 : 332.0716) }
    });
  }

  @Override
  double compute(Object[] dataIn) {
    getPointers(dataIn);
    double f = dData[0] * dData[1] * dData[2];
    calcs.setPairVariables(this);
    double d = rab + BUFF;
    energy = f / d; // DIEL = 1 here
    
    if (calcs.gradients) {
      dE = -energy / d;
      calcs.addForces(this, 2);
    }

    if (calcs.logging && Math.abs(energy) > 20)
      calcs.appendLogData(calcs.getDebugLine(Calculations.CALC_ES, this));

    return energy;
  }
}