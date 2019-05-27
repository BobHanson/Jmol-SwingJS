/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-11-23 12:49:25 -0600 (Fri, 23 Nov 2007) $
 * $Revision: 8655 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.minimize.forcefield;

import javajs.util.Lst;
import javajs.util.PT;

import java.util.Map;

import org.jmol.minimize.MMConstraint;
import org.jmol.minimize.MinAngle;
import org.jmol.minimize.MinAtom;
import org.jmol.minimize.MinBond;
import org.jmol.minimize.MinObject;
import org.jmol.minimize.MinPosition;
import org.jmol.minimize.MinTorsion;

/**
 * @author  Bob Hanson  5/10/12 - 5/15/12
 * 
 */

class CalculationsMMFF extends Calculations {

  final static double FPAR = 143.9325;

  public static final int DA_D = 'D';
  public static final int DA_DA = DA_D + 'A';

  Calculation bondCalc, angleCalc, torsionCalc, oopCalc, vdwCalc, esCalc, sbCalc;
  //PositionCalc posCalc;
  
  ForceFieldMMFF mmff;
  
  CalculationsMMFF(ForceField ff, Map<Object, Object> ffParams, 
      MinAtom[] minAtoms, MinBond[] minBonds, 
      MinAngle[] minAngles, MinTorsion[] minTorsions, MinPosition[] minPositions,
      Lst<MMConstraint> constraints) {
    super(ff, minAtoms, minBonds, minAngles, minTorsions, minPositions, constraints);
    mmff = (ForceFieldMMFF) ff;
    this.ffParams = ffParams;
    bondCalc = new MMFFDistanceCalc().set(this);
    angleCalc = new MMFFAngleCalc().set(this);
    sbCalc = new MMFFSBCalc().set(this);
    torsionCalc = new MMFFTorsionCalc().set(this);
    oopCalc = new MMFFOOPCalc().set(this);
    vdwCalc = new MMFFVDWCalc().set(this);
    esCalc = new MMFFESCalc().set(this);
    //posCalc = new PositionCalc();
  }
  
  @Override
  String getUnits() {
    return "kcal"; 
  }

  @Override
  boolean setupCalculations() {

    Lst<Object[]> calc;

    MMFFDistanceCalc distanceCalc = (MMFFDistanceCalc) new MMFFDistanceCalc().set(this);
    calc = calculations[CALC_DISTANCE] = new  Lst<Object[]>();
    for (int i = 0; i < bondCount; i++)
      distanceCalc.setData(calc, minBonds[i]);

    calc = calculations[CALC_ANGLE] = new  Lst<Object[]>();
    MMFFAngleCalc angleCalc = (MMFFAngleCalc) new MMFFAngleCalc().set(this);
    for (int i = 0; i < angleCount; i++)
      angleCalc.setData(calc, minAngles[i]);

    calc = calculations[CALC_STRETCH_BEND] = new  Lst<Object[]>();
    MMFFSBCalc sbCalc = (MMFFSBCalc) new MMFFSBCalc().set(this);
    for (int i = 0; i < angleCount; i++)
      sbCalc.setData(calc, minAngles[i]);

    calc = calculations[CALC_TORSION] = new  Lst<Object[]>();
    MMFFTorsionCalc torsionCalc = (MMFFTorsionCalc) new MMFFTorsionCalc().set(this);
    for (int i = 0; i < torsionCount; i++)
      torsionCalc.setData(calc, minTorsions[i]);

    calc = calculations[CALC_OOP] = new  Lst<Object[]>();
    // set up the special atom arrays
    MMFFOOPCalc oopCalc = (MMFFOOPCalc) new MMFFOOPCalc().set(this);
    for (int i = 0; i < ac; i++)
      if (isInvertible(minAtoms[i]))
        oopCalc.setData(calc, i);

//    if (minPositions != null) {
//      calc = calculations[CALC_POSITION] = new List<Object[]>();
//      // set up the special atom arrays
//      //PositionCalc posCalc = new PositionCalc();
//      //for (int i = minPositions.length; --i >= 0;)
//      //  posCalc.setData(calc, minPositions[i].data, minPositions[i].ddata);
//    }

    pairSearch(calculations[CALC_VDW] = new  Lst<Object[]>(), new MMFFVDWCalc().set(this),
        calculations[CALC_ES] = new  Lst<Object[]>(), new MMFFESCalc().set(this));

    return true;
  }

  @Override
  protected boolean isLinear(int i) {
    return MinAtom.isLinear(minAtoms[i]);
  }

  private static boolean isInvertible(MinAtom a) {
    
    // defined for typeB = 2, 3, 8, 10, 17, 26, 30, 37, 39, 40, 41, 43, 
    // 45, 49, 54, 55, 56, 57, 58, 63, 64, 67, 69, 73, 78, 80, 81, 82
    // but is 0 for 8 (amines), 17 (sulfoxides), 
    // 26 (PD3), 43 (N-S), 73 (O-S(=O)R, 82 (N-oxide) 
    // that is, just the planar ones:
    // 2, 3, 10, 30, 37, 39, 40, 41, 
    // 45, 49, 54, 55, 56, 57, 58, 63, 
    // 64, 67, 69, 78, 80, 81
    switch (a.ffType) {
    default:
      return false;
    case 2:
    case 3:
    case 10:
    case 30:
    case 37:
    case 39:
    case 40:
    case 41:
    case 45:
    case 49:
    case 54:
    case 55:
    case 56:
    case 57:
    case 58:
    case 63:
    case 64:
    case 67:
    case 69:
    case 78:
    case 80:
    case 81:
      return true;
    }
  }

  @Override
  double compute(int iType, Object[] dataIn) {

    switch (iType) {
    case CALC_DISTANCE:
      return bondCalc.compute(dataIn);
    case CALC_ANGLE:
      return angleCalc.compute(dataIn);
    case CALC_STRETCH_BEND:
      return sbCalc.compute(dataIn);
    case CALC_TORSION:
      return torsionCalc.compute(dataIn);
    case CALC_OOP:
      return oopCalc.compute(dataIn);
    case CALC_VDW:
      return vdwCalc.compute(dataIn);
    case CALC_ES:
      return esCalc.compute(dataIn);
    //case CALC_POSITION:
      //return posCalc.compute(dataIn);
    }
    return 0.0;
  }

  @Override
  Object getParameterObj(MinObject a) {
    return (a.key == null || a.ddata != null ? a.ddata : ffParams.get(a.key));
  }

  
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
  
  
  
  ///////// REPORTING /////////////
  
  @Override
  String getDebugHeader(int iType) {
    switch (iType){
    case -1:
      return  "MMFF94 Force Field -- " +
          "T. A. Halgren, J. Comp. Chem. 5 & 6 490-519ff (1996).\n";
    case CALC_TORSION:
      return 
           "\nT O R S I O N A L (" + minTorsions.length + " torsions)\n\n"
           +"      ATOMS           ATOM TYPES          TORSION\n"
           +"  I   J   K   L   I     J     K     L      ANGLE       V1       V2       V3     ENERGY\n"
           +"--------------------------------------------------------------------------------------\n";
    default:
      return getDebugHeader2(iType);
    }
  }

  @Override
  String getDebugLine(int iType, Calculation c) {
    float energy = ff.toUserUnits(c.energy);
    switch (iType) {
    case CALC_ANGLE:
    case CALC_STRETCH_BEND:
      return PT.sprintf(
          "%11s  %-5s %-5s %-5s  %8.3f  %8.3f     %8.3f   %8.3f", 
          "ssssFI", new Object[] { MinObject.decodeKey(c.key), minAtoms[c.ia].sType, minAtoms[c.ib].sType, 
              minAtoms[c.ic].sType,
          new float[] { (float)(c.theta * RAD_TO_DEG), (float) c.dData[1] /*THETA0*/, 
              (float)c.dData[0]/*Kijk*/, energy },
          new int[] { minAtoms[c.ia].atom.getAtomNumber(), minAtoms[c.ib].atom.getAtomNumber(),
              minAtoms[c.ic].atom.getAtomNumber()} });
      case CALC_TORSION:
        return PT.sprintf(
              "%15s  %-5s %-5s %-5s %-5s  %8.3f %8.3f %8.3f %8.3f %8.3f", 
              "sssssF", new Object[] { MinObject.decodeKey(c.key), 
                 minAtoms[c.ia].sType, minAtoms[c.ib].sType, 
                 minAtoms[c.ic].sType, minAtoms[c.id].sType, 
            new float[] { (float) (c.theta * RAD_TO_DEG), (float) c.dData[0]/*v1*/, (float) c.dData[1]/*v2*/, (float) c.dData[2]/*v3*/, 
              energy } });
      default:
        return getDebugLineC(iType, c);
    }
  }


}

