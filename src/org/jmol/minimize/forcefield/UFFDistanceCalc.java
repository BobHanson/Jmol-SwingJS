package org.jmol.minimize.forcefield;

import javajs.util.Lst;

class UFFDistanceCalc extends Calculation {

  double r0, kb;

  @Override
  void setData(Lst<Object[]> calc, int ia, int ib, double bondOrder) {
    calcs.parA = (FFParam) calcs.getParameter(calcs.minAtoms[ia].sType);
    calcs.parB = (FFParam) calcs.getParameter(calcs.minAtoms[ib].sType);
    r0 = CalculationsUFF.calculateR0(calcs.parA.dVal[CalculationsUFF.PAR_R], calcs.parB.dVal[CalculationsUFF.PAR_R], calcs.parA.dVal[CalculationsUFF.PAR_XI],
        calcs.parB.dVal[CalculationsUFF.PAR_XI], bondOrder);

    // here we fold the 1/2 into the kij from equation 1a
    // Otherwise, this is equation 6 from the UFF paper.

    kb = CalculationsUFF.KCAL332 * calcs.parA.dVal[CalculationsUFF.PAR_Z] * calcs.parB.dVal[CalculationsUFF.PAR_Z] / (r0 * r0 * r0);
    calc.addLast(new Object[] { new int[] { ia, ib },
        new double[] { r0, kb, bondOrder } });
  }

  @Override
  double compute(Object[] dataIn) {
    getPointers(dataIn);
    r0 = dData[0];
    kb = dData[1];     
    calcs.setPairVariables(this);

    // Er = 0.5 k (r - r0)^2
    
    delta = rab - r0;     // we pre-compute the r0 below
    energy = kb * delta * delta; // 0.5 factor was precalculated

    if (calcs.gradients) {
      dE = 2.0 * kb * delta;
      calcs.addForces(this, 2);
    }
    
    if (calcs.logging)
      calcs.appendLogData(calcs.getDebugLine(Calculations.CALC_DISTANCE, this));
    
    return energy;
  }
}