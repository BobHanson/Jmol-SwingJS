package org.jmol.minimize.forcefield;

import javajs.util.Lst;

import org.jmol.minimize.MinBond;

class MMFFDistanceCalc extends Calculation {

  final static double FSTRETCH = CalculationsMMFF.FPAR / 2;
  final static double CS = -2.0;
  final static double CS2 = ((7.0/12.0)*(CS * CS));
  
  double r0, kb;
  double delta2;

  void setData(Lst<Object[]> calc, MinBond bond) {
    ia = bond.data[0];
    ib = bond.data[1];
    Object data = calcs.getParameterObj(bond);
    if (data == null)
      return;
    calc.addLast(new Object[] { new int[] { ia, ib },  data });
  }

  @Override
  double compute(Object[] dataIn) {
    
    getPointers(dataIn);
    kb = dData[0];
    r0 = dData[1];
    calcs.setPairVariables(this);
    
    delta = rab - r0; 
    delta2 = delta * delta;
    energy = FSTRETCH * kb * delta2 * (1 + CS * delta + CS2  * (delta2));

    if (calcs.gradients) {
      dE = FSTRETCH * kb * delta * (2 + 3 * CS * delta + 4 * CS2 * delta2);
      calcs.addForces(this, 2);
    }
    
    if (calcs.logging)
      calcs.appendLogData(calcs.getDebugLine(Calculations.CALC_DISTANCE, this));
    
    return energy;
  }
}