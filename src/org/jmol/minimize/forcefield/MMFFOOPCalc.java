package org.jmol.minimize.forcefield;

import javajs.util.Lst;

class MMFFOOPCalc extends Calculation {

  final static double FOOPD = 0.043844 * Calculations.RAD_TO_DEG;
  final static double FOOP = FOOPD / 2 * Calculations.RAD_TO_DEG;

  int[] list = new int[4];
  
  void setData(Lst<Object[]> calc, int i) {
    if (calcs.minAtoms[i].nBonds != 3)
      return;// should not be possible...
    int[] indices = calcs.minAtoms[i].getBondedAtomIndexes();
    // our calculation is for first, not last, relative to plane of others, 
    list[0] = indices[2];
    list[1] = i;
    list[2] = indices[1];
    list[3] = indices[0];
    double koop = ((CalculationsMMFF)calcs).mmff.getOutOfPlaneParameter(list);
    if (koop == 0)
      return;
    double[] dk = new double[] { koop };
    calc.addLast(new Object[] { new int[] { indices[0], i, indices[1], indices[2] },  dk });
    calc.addLast(new Object[] { new int[] { indices[1], i, indices[2], indices[0] },  dk });
    calc.addLast(new Object[] { new int[] { indices[2], i, indices[0], indices[1] },  dk });
  }

  @Override
  double compute(Object[] dataIn) {
    
    getPointers(dataIn);
    calcs.setOopVariables(this, false);
    double koop = dData[0];
    
    energy = FOOP * koop * theta * theta; // theta in radians
    
    if (calcs.gradients) {
      dE = FOOPD * koop * theta;
      calcs.addForces(this, 4);
    }

    if (calcs.logging)
      calcs.appendLogData(calcs.getDebugLine(Calculations.CALC_OOP, this));

    return energy;
  }
}