package org.jmol.minimize.forcefield;

import javajs.util.Lst;

class MMFFVDWCalc extends Calculation {
  
  @Override
  void setData(Lst<Object[]> calc, int ia, int ib, double dd) {
    a = calcs.minAtoms[ia];
    b = calcs.minAtoms[ib];
    double[] dataA = (double[]) calcs.getParameter(a.vdwKey);
    double[] dataB = (double[]) calcs.getParameter(b.vdwKey);
    if (dataA == null || dataB == null)
      return;
    
    double alpha_a = dataA[0]; 
    double N_a = dataA[1]; 
    double A_a = dataA[2]; 
    double G_a = dataA[3]; 
    int DA_a = (int) dataA[4];
    
    double alpha_b = dataB[0]; 
    double N_b = dataB[1]; 
    double A_b = dataB[2]; 
    double G_b = dataB[3]; 
    int DA_b = (int) dataB[4]; 
    
    double rs_aa = A_a * Math.pow(alpha_a, 0.25);
    double rs_bb = A_b * Math.pow(alpha_b, 0.25);
    double gamma = (rs_aa - rs_bb) / (rs_aa + rs_bb);
    double rs = 0.5 * (rs_aa + rs_bb);
    if (DA_a != CalculationsMMFF.DA_D && DA_b != CalculationsMMFF.DA_D)
      rs *= (1.0 + 0.2 * (1.0 - Math.exp(-12.0 * gamma * gamma)));
    double eps = ((181.16 * G_a * G_b * alpha_a * alpha_b) 
        / (Math.sqrt(alpha_a / N_a) + Math.sqrt(alpha_b / N_b))) * Math.pow(rs, -6.0);

    if(DA_a + DA_b == CalculationsMMFF.DA_DA) {
      rs *= 0.8;
      eps *= 0.5;
    }
    calc.addLast(new Object[] { new int[] {ia, ib}, new double[] { rs, eps } });
  }

  @Override
  double compute(Object[] dataIn) {
    getPointers(dataIn);
    calcs.setPairVariables(this);
    double rs = dData[0];
    double eps = dData[1];
    double r_rs = rab / rs;
    double f1 = 1.07 / (r_rs + 0.07);
    double f2 = 1.12 / (Math.pow(r_rs, 7) + 0.12);
    
    energy = eps * Math.pow(f1, 7)  * (f2 - 2);
    
    if (calcs.gradients) {
      // dE = eps ( 7(f1^6)df1(f2-2) + (f1^7)df2 )
      // dE = eps f1^6 ( 7df1(f2-2) + f1(df2) )
      // df1/dr = -1.07 / (r_rs + 0.07)^2 * 1/rs 
      //        = -f1^2 / 1.07 * 1/rs
      // df2/dr = -1.12 / (r_rs^7 + 0.12)^2 * 7(r_rs)^6 * 1/rs 
      //        = -f2^2 / 1.12 * 7(r_rs)^6 * 1/rs
      // dE = -7 eps f1^7 / rs ( (f2-2)(f1 /1.07) + f2^2 / 1.12 * r_rs^6
      dE = -7 * eps * Math.pow(f1, 7) /rs 
          * (f1 / 1.07 * (f2 - 2) + f2 * f2 * Math.pow(r_rs, 6));
      calcs.addForces(this, 2);
    }

    if (calcs.logging && Math.abs(energy) > 0.1)
      calcs.appendLogData(calcs.getDebugLine(Calculations.CALC_VDW, this));

    return energy;
  } 
}