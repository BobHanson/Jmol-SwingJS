package org.jmol.minimize.forcefield;

import javajs.util.Lst;

import org.jmol.minimize.MinBond;
import org.jmol.minimize.Util;

class UFFTorsionCalc extends Calculation {

void setData(Lst<Object[]> calc, int[] t) {
    double cosNPhi0 = -1; // n * phi0 = 180; max at 0 
    int n = 0;
    double V = 0;
    a = calcs.minAtoms[ia = t[0]];
    b = calcs.minAtoms[ib = t[1]];
    c = calcs.minAtoms[ic = t[2]];
    d = calcs.minAtoms[id = t[3]];
    MinBond bc = c.getBondTo(ib);
    double bondOrder = bc.order;
    if (bc.isAromatic)
      bondOrder = 1.5;
    if (bc.isAmide)
      bondOrder = 1.41;

    calcs.parB = (FFParam) calcs.getParameter(b.sType);
    calcs.parC = (FFParam) calcs.getParameter(c.sType);

    switch (calcs.parB.iVal[0] * calcs.parC.iVal[0]) {
    case 9: // sp3 sp3
      // max at 0; minima at 60, 180, 240
      n = 3; 
      double vi = calcs.parB.dVal[CalculationsUFF.PAR_V];
      double vj = calcs.parC.dVal[CalculationsUFF.PAR_V];

      // exception for (group 6 -- group 6) sp3 atoms
      double viNew = 0;
      switch (b.atom.getElementNumber()) {
      case 8:
        viNew = 2.0;
        break;
      case 16:
      case 34:
      case 52:
      case 84:
        viNew = 6.8;
      }
      if (viNew != 0)
        switch (c.atom.getElementNumber()) {
        case 8:
          // max at 0; minima at 90
          vi = viNew;
          vj = 2.0;
          n = 2; 
          break;
        case 16:
        case 34:
        case 52:
        case 84:
          // max at 0; minima at 90
          vi = viNew;
          vj = 6.8;
          n = 2; 
        }
      V = 0.5 * Calculations.KCAL_TO_KJ * Math.sqrt(vi * vj);
      break;
    case 4: //sp2 sp2
      // max at 90; minima at 0 and 180
      cosNPhi0 = 1; 
      n = 2; 
      V = 0.5 * Calculations.KCAL_TO_KJ * 5.0
          * Math.sqrt(calcs.parB.dVal[CalculationsUFF.PAR_U] * calcs.parC.dVal[CalculationsUFF.PAR_U])
          * (1.0 + 4.18 * Math.log(bondOrder));
      break;
    case 6: //sp2 sp3
      // maximim at 30, 90, 150; minima at 0, 60, 120, 180
      cosNPhi0 = 1;  
      n = 6; 
      // exception for group 6 sp3 attached to non-group 6 sp2
      // maximim at 30, 90, 150; minima at 0, 60, 120, 180
      boolean sp3C = (calcs.parC.iVal[0] == 3); 
      switch ((sp3C ? c : b).atom.getElementNumber()) {
      case 8:
      case 16:
      case 34:
      case 52:
      case 84:
        switch ((sp3C ? b : c).atom.getElementNumber()) {
        case 8:
        case 16:
        case 34:
        case 52:
        case 84:
          break;
        default:
          n = 2;
          cosNPhi0 = -1; 
        }
        break;
      }
      V = 0.5 * Calculations.KCAL_TO_KJ;
    }

    if (Util.isNearZero(V)) // don't bother calcuating this torsion
      return;

    calc.addLast(new Object[] { iData = new int[] { ia, ib, ic, id, n },
        new double[] { V, cosNPhi0 }, isLoggable(4) });
  }

  
  @Override
  double compute(Object[] dataIn) {
     
    getPointers(dataIn);
    int n = iData[4];
    double V = dData[0];
    double cosNPhi0 = dData[1];      
    calcs.setTorsionVariables(this);

    energy = V * (1.0 - cosNPhi0 * Math.cos(theta * n));

    if (calcs.gradients) {
      dE = V * n * cosNPhi0 * Math.sin(n * theta);
      calcs.addForces(this, 4);
    }
    
    if (calcs.logging && dataIn[2] == Boolean.TRUE)
      calcs.appendLogData(calcs.getDebugLine(Calculations.CALC_TORSION, this));
    
    return energy;
  }
}