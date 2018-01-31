package org.jmol.minimize.forcefield;

import javajs.util.Lst;

import org.jmol.minimize.MinAngle;

class MMFFAngleCalc extends Calculation {

  void setData(Lst<Object[]> calc, MinAngle angle) {
    Object data = calcs.getParameterObj(angle);
    if (data == null)
      return;
    calc.addLast(new Object[] { angle.data, data, angle.key });      
  }

  final static double CB = -0.4 * Calculations.DEG_TO_RAD;
  
  @Override
  double compute(Object[] dataIn) {
    
    key = (Integer) dataIn[2];

    getPointers(dataIn);
    double ka = dData[0];
    double t0 = dData[1];
    calcs.setAngleVariables(this);

    double dt = (theta * Calculations.RAD_TO_DEG - t0);

    // could have problems here for very distorted structures.

    if (t0 == 180) {
      energy = CalculationsMMFF.FPAR * ka * (1 + Math.cos(theta));
      if (calcs.gradients)
        dE = -CalculationsMMFF.FPAR * ka * Math.sin(theta);
    } else {
      energy = 0.021922 * ka * Math.pow(dt, 2) * (1 + CB * dt); // 0.043844/2
      if (calcs.gradients)
        dE = 0.021922 * ka * dt * (2 + 3 * CB * dt);
    }
    if (calcs.gradients)
      calcs.addForces(this, 3);
    
    if (calcs.logging)
      calcs.appendLogData(calcs.getDebugLine(Calculations.CALC_ANGLE, this));
    
    return energy;
  }

}