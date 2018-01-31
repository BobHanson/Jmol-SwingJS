package org.jmol.minimize.forcefield;

import javajs.util.Lst;

class UFFVDWCalc extends Calculation {
    
    @Override
    void setData(Lst<Object[]> calc, int ia, int ib, double dd) {
      a = calcs.minAtoms[ia];
      b = calcs.minAtoms[ib];
      
      FFParam parA = (FFParam) calcs.getParameter(a.sType);
      FFParam parB = (FFParam) calcs.getParameter(b.sType);

      double Xa = parA.dVal[CalculationsUFF.PAR_X];
      double Da = parA.dVal[CalculationsUFF.PAR_D];
      if (parB == null || parB.dVal == null)
        System.out.println("OHOH");
      double Xb = parB.dVal[CalculationsUFF.PAR_X];
      double Db = parB.dVal[CalculationsUFF.PAR_D];

      //this calculations only need to be done once for each pair, 
      //we do them now and save them for later use
      double Dab = Calculations.KCAL_TO_KJ * Math.sqrt(Da * Db);

      // 1-4 scaling
      // This isn't mentioned in the UFF paper, but is common for other methods
      //       if (a.IsOneFour(b))
      //         kab *= 0.5;

      // Xab is xij in equation 20 -- the expected vdw distance
      double Xab = Math.sqrt(Xa * Xb);
      calc.addLast(new Object[] {
          new int[] { ia, ib },
          new double[] { Xab, Dab } });
    }

    @Override
    double compute(Object[] dataIn) {

      getPointers(dataIn);
      double Xab = dData[0];
      double Dab = dData[1];
      
      calcs.setPairVariables(this);
      
      // Evdw = Dab [(Xab/r)^12 - 2(Xab/r)^6]      Lennard-Jones
      //      = Dab (Xab/r)^6[(Xab/r)^6 - 2]
      
      double term = Xab / rab;
      double term6 = term * term * term;
      term6 *= term6;
      energy = Dab * term6 * (term6 - 2.0);

      if (calcs.gradients) {
        dE = Dab * 12.0 * (1.0 - term6) * term6 * term / Xab; // unchecked
        calcs.addForces(this, 2);
      }
      
      if (calcs.logging)
        calcs.appendLogData(calcs.getDebugLine(Calculations.CALC_VDW, this));
      
      return energy;
    }
  } 
/*
  
  class ESCalc extends PairCalc {

    @Override
    void setData(List<Object[]> calc, int ia, int ib) {
      a = minAtoms[ia];
      b = minAtoms[ib];
      double qq = KCAL332 * partialCharges[ia]
          * partialCharges[ib];
      if (qq != 0)
        calc.add(new Object[] {
            new int[] { ia, ib },
            new double[] { qq } });
    }

    @Override
    double compute(Object[] dataIn) {      
      getPointers(dataIn);
      double qq = dData[0];
      setPairVariables(this);

      energy = qq / rab;

      if (gradients) {
        dE = -qq / (rab * rab);
        addForces(this, 2);
      }
      
      if (logging)
        appendLogData(getDebugLine(CALC_ES, this));
      
      return energy;
    }
  }

*/  
  ///////// REPORTING /////////////