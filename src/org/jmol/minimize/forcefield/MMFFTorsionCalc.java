package org.jmol.minimize.forcefield;

import javajs.util.Lst;

import org.jmol.minimize.MinTorsion;

class MMFFTorsionCalc extends Calculation {

    void setData(Lst<Object[]> calc, MinTorsion t) {
      if (calcs.isLinear(t.data[1]) || calcs.isLinear(t.data[2]))
        return;
      Object data = calcs.getParameterObj(t);
      if (data == null)
        return;
      calc.addLast(new Object[] { t.data, data, t.key });
    }
    
    @Override
    double compute(Object[] dataIn) {
      
      key = (Integer) dataIn[2];

      getPointers(dataIn);
      double v1 = dData[0];
      double v2 = dData[1];
      double v3 = dData[2];
      
      calcs.setTorsionVariables(this);

      // use one single cosine calculation 
      
      double cosTheta = Math.cos(theta);
      double cosTheta2 = cosTheta * cosTheta;
      
      energy = 0.5 * (v1 * (1 + cosTheta)
          + v2 * (2 - 2 * cosTheta2)
          + v3 * (1 + cosTheta * (4 * cosTheta2 - 3)));

/*          
        energy = 0.5 * (v1 * (1.0 + Math.cos(theta)) 
            + v2 * (1 - Math.cos(2 * theta)) 
            + v3 * (1 + Math.cos(3 * theta)));
*/
      if (calcs.gradients) {
        double sinTheta = Math.sin(theta);        
        dE = 0.5 * (-v1 * sinTheta 
            + 4 * v2 * sinTheta * cosTheta 
            + 3 * v3 * sinTheta * (1 - 4 * cosTheta2));
/*
        dE = 0.5 * (-v1 * sinTheta 
        + 2 * v2 * Math.sin(2 * theta) 
        - 3 * v3 * Math.sin(3 * theta));
*/        
        calcs.addForces(this, 4);
      }
      
      if (calcs.logging)
        calcs.appendLogData(calcs.getDebugLine(Calculations.CALC_TORSION, this));
      
      return energy;
    }
  }