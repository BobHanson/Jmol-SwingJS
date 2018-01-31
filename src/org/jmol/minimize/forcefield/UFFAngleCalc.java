package org.jmol.minimize.forcefield;

import javajs.util.Lst;

import org.jmol.minimize.MinBond;

class UFFAngleCalc extends Calculation {

  void setData(Lst<Object[]> calc, int[] angle) {
    a = calcs.minAtoms[ia = angle[0]];
    b = calcs.minAtoms[ib = angle[1]];
    c = calcs.minAtoms[ic = angle[2]];
    double preliminaryMagnification = (a.sType == "H_" && c.sType == "H_" ? 10 : 1);
    calcs.parA = (FFParam) calcs.getParameter(a.sType);
    calcs.parB = (FFParam) calcs.getParameter(b.sType);
    calcs.parC = (FFParam) calcs.getParameter(c.sType);

    int coordination = calcs.parB.iVal[0]; // coordination of central atom

    double zi = calcs.parA.dVal[CalculationsUFF.PAR_Z];
    double zk = calcs.parC.dVal[CalculationsUFF.PAR_Z];
    double theta0 = calcs.parB.dVal[CalculationsUFF.PAR_THETA];
    double cosT0 = Math.cos(theta0);
    double sinT0 = Math.sin(theta0);
    double c0, c1, c2;
    switch (coordination) {
    case 1:
    case 2:
    case 4:
    case 6:
      c0 = c1 = c2 = 0;
      break;
    default:  
      c2 = 1.0 / (4.0 * sinT0 * sinT0);
      c1 = -4.0 * c2 * cosT0;
      c0 = c2 * (2.0 * cosT0 * cosT0 + 1.0);
    }

    // Precompute the force constant
    MinBond bond = a.getBondTo(ib);
    double bondorder = bond.order;
    if (bond.isAromatic)
      bondorder = 1.5;
    if (bond.isAmide)
      bondorder = 1.41;
    rab = CalculationsUFF.calculateR0(calcs.parA.dVal[CalculationsUFF.PAR_R], calcs.parB.dVal[CalculationsUFF.PAR_R], calcs.parA.dVal[CalculationsUFF.PAR_XI], calcs.parB.dVal[CalculationsUFF.PAR_XI], bondorder);

    bond = c.getBondTo(ib);
    bondorder = bond.order;
    if (bond.isAromatic)
      bondorder = 1.5;
    if (bond.isAmide)
      bondorder = 1.41;
    double rbc = CalculationsUFF.calculateR0(calcs.parB.dVal[CalculationsUFF.PAR_R], calcs.parC.dVal[CalculationsUFF.PAR_R], 
        calcs.parB.dVal[CalculationsUFF.PAR_XI], calcs.parC.dVal[CalculationsUFF.PAR_XI], bondorder);
    double rac = Math.sqrt(rab * rab + rbc * rbc - 2.0 * rab * rbc * cosT0);

    // Equation 13 from paper -- corrected by Towhee
    // Note that 1/(rij * rjk) cancels with rij*rjk in eqn. 13
    double ka = (CalculationsUFF.KCAL644) * (zi * zk / (Math.pow(rac, 5.0)))
        * (3.0 * rab * rbc * (1.0 - cosT0 * cosT0) - rac * rac * cosT0);
    calc.addLast(new Object[] {
        new int[] { ia, ib, ic, coordination },
        new double[] { ka, theta0 * Calculations.RAD_TO_DEG, c0 - c2, c1, 2 * c2, preliminaryMagnification * ka } });
  }

  @Override
  double compute(Object[] dataIn) {
    
    getPointers(dataIn);
    int coordination = iData[3];
    double ka = (calcs.isPreliminary ? dData[5] : dData[0]);
    double a0 = dData[2];
    double a1 = dData[3];
    double a2 = dData[4];
    calcs.setAngleVariables(this);

    //problem here for square planar cis or trans
    if ((coordination == 4 || coordination == 6) && 
        (theta > 2.35619 || theta < 0.785398)) // 135o, 45o
      coordination = 1;
    double cosT = Math.cos(theta);
    double sinT = Math.sin(theta);
    switch (coordination) {
    case 0: //constraint
    case 1: //sp
      energy = ka * (1.0 + cosT) * (1.0 + cosT) / 4.0;
      break;
    case 2: //sp2
       //(1 + 4cos(theta) + 4cos(theta)^2)/9 
      energy = ka * (1.0  + (4.0 * cosT) * (1.0 + cosT)) / 9.0;
      break;
    case 4: //dsp2
    case 6: //d2sp3
      energy = ka * cosT * cosT;
      break;
    default:
      // 
      energy = ka * (a0 + a1 * cosT + a2 * cosT * cosT);
    }

    if (calcs.gradients) {
      // da = dTheta/dx * dE/dTheta
      switch (coordination) {
      case 0: //constraint
      case 1:
        dE = -0.5 * ka * sinT * (1 + cosT);
        break;
      case 2:
        dE = -4.0 * sinT * ka * (1.0 - 2.0 * cosT)/9.0;
        break;
      case 4:
      case 6:
        dE = -ka * sinT * cosT;
        break;
      default:
        dE = -ka * (a1 * sinT - 2.0 * a2 * cosT * sinT);
      }
      calcs.addForces(this, 3);
    }
    
    if (calcs.logging)
      calcs.appendLogData(calcs.getDebugLine(Calculations.CALC_ANGLE, this));
    
    return energy;
  }
}