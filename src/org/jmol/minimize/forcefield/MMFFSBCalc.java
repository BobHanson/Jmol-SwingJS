package org.jmol.minimize.forcefield;

import javajs.util.Lst;

import org.jmol.minimize.MinAngle;

class MMFFSBCalc extends Calculation {
  
  void setData(Lst<Object[]> calc, MinAngle angle) {
    // not applicable for linear types
    if (calcs.isLinear(angle.data[1]))
      return;
    double[] data = (double[]) calcs.getParameter(angle.sbKey);
    double[] datakat0 = (double[]) calcs.getParameterObj(angle);
    double[] dataij = (double[]) calcs.getParameter(calcs.minBonds[angle.data[ForceField.ABI_IJ]]);
    double[] datajk = (double[]) calcs.getParameter(calcs.minBonds[angle.data[ForceField.ABI_JK]]);
    if (data == null || datakat0 == null || dataij == null || datajk == null)
      return;
    double theta0 = datakat0[1];
    double r0ij = dataij[1];
    double r0jk = datajk[1];
    calc.addLast(new Object[] { angle.data, new double[] { data[0], theta0, r0ij }, angle.sbKey });
    calc.addLast(new Object[] { new int[] {angle.data[2], angle.data[1], angle.data[0]}, 
        new double[] { data[1], theta0, r0jk }, angle.sbKey  });
  }

  @Override
  double compute(Object[] dataIn) {

    key = (Integer) dataIn[2];

    getPointers(dataIn);
    double k = 2.51210 * dData[0];
    double t0 = dData[1];
    double r0_ab = dData[2];

    calcs.setPairVariables(this);
    calcs.setAngleVariables(this);
    double dr_ab = rab - r0_ab;
    delta = theta * Calculations.RAD_TO_DEG - t0;
    // equation 5
    energy = k * dr_ab * delta;

    if (calcs.logging)
      calcs.appendLogData(calcs.getDebugLine(Calculations.CALC_STRETCH_BEND, this));
    
    if (calcs.gradients) {
      dE = k * dr_ab;
      calcs.addForces(this, 3);
      calcs.setPairVariables(this);
      dE = k * delta;
      calcs.addForces(this, 2);        
    }
    
    return energy;
  }
}