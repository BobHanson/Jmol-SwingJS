package org.jmol.minimize.forcefield;

import javajs.util.Lst;

//  class PositionCalc extends Calculation {
//
//    @Override
//    double compute(Object[] dataIn) {
//      // TODO
//      return 0;
//    }
//
//    public void setData(List<Object[]> calc, int[] data, double[] ddata) {
//      // TODO
//      
//    }
//    
//  }
//  
  class UFFOOPCalc extends Calculation {

    @Override
    void setData(Lst<Object[]> calc, int ib, int elemNo, double dd) {

      // The original Rappe paper in JACS isn't very clear about the parameters
      // The following was adapted from Towhee

      /*
       *         a
       *         |
       *         b      theta defines the angle of b->c relative to the plane abd
       *        / \     note that we want the theta >= 0. 
       *       c   d
       * 
       *
       *   E = K [ c0 + c1 cos(theta) + c2 cos(2 theta) ]
       * 
       *   But we only allow one or the other, c1 or c2, to be nonzero. 
       *   
       *   trigonal planar species (CH2=CH2, for example), we use
       *    
       *     c0 = 1
       *     c1 = -1
       *     c2 = 0 
       *     
       *   so that the function is 
       *   
       *     E = K [1 - cos(theta)] 
       *     
       *   with a minimum at theta=0 and "barrier" height of K when theta = 90.
       * 
       *   For trigonal pyramidal species (NH3, PX3), we want the minimum at
       *   some particular angle near 90 degrees. If we wanted exactly 90 degrees, 
       *   then we would use 
       *   
       *     c0 = c1 = 0 and c2 = 1
       *     
       *   so that we would have
       *   
       *     E = K cos(2 theta)
       *     
       *   with minimum at theta=90 degrees and a barrier of K at theta=0
       *   
       *   But NH3, PH3, etc. are not exactly at 90 degrees, so we use the known hydride
       *   angle as the basis angle PHI and use the following function instead:
       *   
       *     E = K {  [cos(phi) - cos(theta)]^2 }
       *     
       *   At least, that's what I would do, because then we have a minimum at theta = phi and 
       *   a barrier approx = 0 when E = K, provided  
       *   
       *   
       *   . This works out to:
       *   
       *     E/K = cos(phi)^2 - 2cos(phi)cos(theta) + cos(theta)^2
       *     
       *   Now,  cos(theta)^2 = 1/2 cos(2 theta) + 1/2, so we have:
       *   
       *    E/K = 1/2 + cos(phi)^2 - 2 cos(phi) cos(theta) + 1/2 cos(2 theta)
       *    
       *    giving
       *    
       * [1]  c0 = 1/2 + cos(phi)^2
       *      c1 = -2 cos(phi)
       *      c2 = 1/2
       *      
       *   which has the proper barrier of E = K at theta = 0, considering phi is about 90.
       *   
       *   Oddly enough, the C++ code in OpenBabel uses
       *   
       *      c0 = 4 cos(phi)^2 + cos(2 phi)
       *      c1 = -4 cos(phi)
       *      c2 = 1
       *      
       *   I think this should be a - cos(2 phi) and all coefficients multiplied by
       *   1/2 to be consistent with this analysis. 
       *   Otherwise the barrier is too large at theta=0.
       *   
       *   What happens is we cast this as the following? 
       *   
       *     E = K [ a0 + a1 cos(theta) + a2 cos(theta)^2]
       *     
       *   We get
       *   
       *     E = K [ c0 + c1 cos(theta) + c2 cos(2 theta)]
       *     
       *       = K [ c0 + c1 cos(theta) + c2 (2 cos(theta)^2 - 1) ]
       *       
       *       = K [ (c0 - c2) + c1 cos(theta) + 2 c2 cos(theta)^2]
       *   
       *   so
       *   
       *     ao = (c0 - c2)
       *     a1 = c1
       *     a2 = 2 c2
       *     
       *   And we don't have to take two cos operations. For our three cases then we get:
       *   
       *   
       *   trigonal planar species (no change):
       *    
       *     c0 = 1       a0 =  1
       *     c1 = -1      a1 = -1
       *     c2 = 0       a2 =  0
       *     
       *   NH3/PH3, etc.:
       *   
       *     c0 = 1/2 + cos(phi)^2    a0 = cos(phi)^2
       *     c1 = -2 cos(phi)         a1 = -2 cos(phi)
       *     c2 = 1/2                 a2 = 1
       *     
       *   I have to say I like these better!
       *   
       *      
       */

      
      b = calcs.minAtoms[ib];
      int[] atomList = b.getBondedAtomIndexes();
      a = calcs.minAtoms[ia = atomList[0]];
      c = calcs.minAtoms[ic = atomList[1]];
      d = calcs.minAtoms[id = atomList[2]];

      double a0 = 1.0;
      double a1 = -1.0;
      double a2 = 0.0;
      double koop = CalculationsUFF.KCAL6;
      switch (elemNo) {
      case 6: // carbon could be a carbonyl, which is considerably stronger
        // added b.sType == "C_2+" for cations 12.0.RC9
        // added b.typ "C_2" check for H-connected 12.0.RC13
        if (b.sType == "C_2" && b.hCount > 1
            || b.sType == "C_2+" || a.sType == "O_2" || c.sType == "O_2" || d.sType == "O_2") {
          koop += CalculationsUFF.KCAL44;
          break;
        }/* else if (b.sType.lastIndexOf("R") == 2) 
          koop *= 10; // Bob's idea to force flat aromatic rings. 
           // Who would EVER want otherwise?
*/        break;
      case 7:
      case 8:
        break;
      default:
        koop = CalculationsUFF.KCAL22;
        double phi = Calculations.DEG_TO_RAD;
        switch (elemNo) {
        case 15: // P
          phi *= 84.4339;
          break;
        case 33: // As
          phi *= 86.9735;
          break;
        case 51: // Sb
          phi *= 87.7047;
          break;
        case 83: // Bi     
          phi *= 90.0;
          break;
        }
        double cosPhi = Math.cos(phi);
        a0 = cosPhi * cosPhi;
        a1 = -2.0 * cosPhi;
        a2 = 1.0;
        //
        // same as:
        //
        // E = K [ cos(theta) - cos(phi)]^2
        //
        //phi ~ 90, so c0 ~ 0, c1 ~ 0.5, and E(0) ~ K 
      }

      koop /= 3.0;

      // A-BCD 
      calc.addLast(new Object[] { new int[] { ia, ib, ic, id },
          new double[] { koop, a0, a1, a2, koop * 10 } });

      // C-BDA
      calc.addLast(new Object[] { new int[] { ic, ib, id, ia },
          new double[] { koop, a0, a1, a2, koop * 10 } });

      // D-BAC
      calc.addLast(new Object[] { new int[] { id, ib, ia, ic },
          new double[] { koop, a0, a1, a2, koop * 10 } });
    }

    @Override
    double compute(Object[] dataIn) {

      getPointers(dataIn);
      double koop = (calcs.isPreliminary ? dData[4] : dData[0]);
      double a0 = dData[1];
      double a1 = dData[2];
      double a2 = dData[3];
      calcs.setOopVariables(this, true);
      
      double cosTheta = Math.cos(theta);

      //energy = koop * (c0 + c1 * Math.cos(theta) + c2 * Math.cos(2.0 * theta));
      //
      //using

      energy = koop * (a0 + a1 * cosTheta + a2 * cosTheta * cosTheta);

      if (calcs.gradients) {
        // somehow we already get the -1 from the OOPDeriv -- so we'll omit it here
        // not checked in Java
        dE = koop
            * (a1 * Math.sin(theta) + a2 * 2.0 * Math.sin(theta) * cosTheta);
        calcs.addForces(this, 4);
      }

      if (calcs.logging)
        calcs.appendLogData(calcs.getDebugLine(Calculations.CALC_OOP, this));

      return energy;
    }

  }