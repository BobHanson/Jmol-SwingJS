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

import java.util.Map;

import org.jmol.minimize.MMConstraint;
import org.jmol.minimize.MinAngle;
import org.jmol.minimize.MinAtom;
import org.jmol.minimize.MinBond;
import org.jmol.minimize.MinObject;
import org.jmol.minimize.MinPosition;
import org.jmol.minimize.MinTorsion;

/*
 * Java implementation by Bob Hanson 3/2008
 * based on OpenBabel code in C++ by Tim Vandermeersch 
 * and Geoffrey Hutchison, with permission.
 *    
 * Original comments:
 * 
 * http://towhee.sourceforge.net/forcefields/uff.html
 * http://rdkit.org/
 * http://franklin.chm.colostate.edu/mmac/uff.html
 *(for the last, use the Wayback Machine: http://www.archive.org/
 * As well, the main UFF paper:
 * Rappe, A. K., et. al.; J. Am. Chem. Soc. (1992) 114(25) p. 10024-10035.
 */

class CalculationsUFF extends Calculations {

  final static double KCAL332 = KCAL_TO_KJ * 332.0637;
  final static double KCAL644 = 644.12 * KCAL_TO_KJ;
  final static double KCAL6 = 6.0 * KCAL_TO_KJ;
  final static double KCAL22 = 22.0 * KCAL_TO_KJ;
  final static double KCAL44 = 44.0 * KCAL_TO_KJ;

  final static int PAR_R = 0; // covalent radius
  final static int PAR_THETA = 1; // covalent angle
  final static int PAR_X = 2; // nonbond distance
  final static int PAR_D = 3; // nonbond energy
  final static int PAR_ZETA = 4; // nonbond scale   -- not used
  final static int PAR_Z = 5; // effective charge
  final static int PAR_V = 6; // sp3 torsional barrier parameter
  final static int PAR_U = 7; // sp2 torsional barrier parameter
  final static int PAR_XI = 8; // GMP electronegativity
  final static int PAR_HARD = 9; // not used?
  final static int PAR_RADIUS = 10; // not used?

  Calculation bondCalc, angleCalc, torsionCalc, oopCalc, vdwCalc;

  //PositionCalc posCalc;
  //ESCalc esCalc;

  CalculationsUFF(ForceField ff, Map<Object, Object> ffParams,
      MinAtom[] minAtoms, MinBond[] minBonds, MinAngle[] minAngles,
      MinTorsion[] minTorsions, MinPosition[] minPositions,
      Lst<MMConstraint> constraints) {
    super(ff, minAtoms, minBonds, minAngles, minTorsions, minPositions,
        constraints);
    this.ffParams = ffParams;
    bondCalc = new UFFDistanceCalc().set(this);
    angleCalc = new UFFAngleCalc().set(this);
    torsionCalc = new UFFTorsionCalc().set(this);
    oopCalc = new UFFOOPCalc().set(this);
    vdwCalc = new UFFVDWCalc().set(this);
    //posCalc = new PositionCalc();
    //esCalc = new ESCalc();
  }

  @Override
  String getUnits() {
    return "kJ";
  }

  @Override
  boolean setupCalculations() {

    Lst<Object[]> calc;

    Calculation distanceCalc = new UFFDistanceCalc().set(this);
    calc = calculations[CALC_DISTANCE] = new Lst<Object[]>();
    for (int i = 0; i < bondCount; i++) {
      MinBond bond = minBonds[i];
      double bondOrder = bond.order;
      if (bond.isAromatic)
        bondOrder = 1.5;
      if (bond.isAmide)
        bondOrder = 1.41;
      distanceCalc.setData(calc, bond.data[0], bond.data[1], bondOrder);
    }

    calc = calculations[CALC_ANGLE] = new Lst<Object[]>();
    UFFAngleCalc angleCalc = (UFFAngleCalc) new UFFAngleCalc().set(this);
    for (int i = minAngles.length; --i >= 0;)
      angleCalc.setData(calc, minAngles[i].data);

    calc = calculations[CALC_TORSION] = new Lst<Object[]>();
    UFFTorsionCalc torsionCalc = (UFFTorsionCalc) new UFFTorsionCalc()
        .set(this);
    for (int i = minTorsions.length; --i >= 0;)
      torsionCalc.setData(calc, minTorsions[i].data);

    calc = calculations[CALC_OOP] = new Lst<Object[]>();
    // set up the special atom arrays
    Calculation oopCalc = new UFFOOPCalc().set(this);
    int elemNo;
    for (int i = 0; i < ac; i++) {
      MinAtom a = minAtoms[i];
      if (a.nBonds == 3 && isInvertible(elemNo = a.atom.getElementNumber()))
        oopCalc.setData(calc, i, elemNo, 0);
    }

    //    if (minPositions != null) {
    //      calc = calculations[CALC_POSITION] = new List<Object[]>();
    //      // set up the special atom arrays
    //      //PositionCalc posCalc = new PositionCalc();
    //      //for (int i = minPositions.length; --i >= 0;)
    //      //  posCalc.setData(calc, minPositions[i].data, minPositions[i].ddata);
    //    }

    // Note that while the UFF paper mentions an electrostatic term,
    // it does not actually use it. Both Towhee and the UFF FAQ
    // discourage the use of electrostatics with UFF.

    pairSearch(calculations[CALC_VDW] = new Lst<Object[]>(),
        new UFFVDWCalc().set(this), null, null);
    return true;
  }

  private static boolean isInvertible(int n) {
    switch (n) {
    case 6: // C
    case 7: // N
    case 8: // O
    case 15: // P
    case 33: // As
    case 51: // Sb
    case 83: // Bi
      return true;
    default:
      return false;// no inversion term for this element
    }
  }

  static double calculateR0(double ri, double rj, double chiI, double chiJ,
                            double bondorder) {
    // precompute the equilibrium geometry
    // From equation 3
    double rbo = -0.1332 * (ri + rj) * Math.log(bondorder);
    // From equation 4

    double dchi = Math.sqrt(chiI) - Math.sqrt(chiJ);
    double ren = ri * rj * dchi * dchi / (chiI * ri + chiJ * rj);
    // From equation 2
    // NOTE: See http://towhee.sourceforge.net/forcefields/uff.html
    // There is a typo in the published paper
    return (ri + rj + rbo - ren);
  }

  @Override
  double compute(int iType, Object[] dataIn) {

    switch (iType) {
    case CALC_DISTANCE:
      return bondCalc.compute(dataIn);
    case CALC_ANGLE:
      return angleCalc.compute(dataIn);
    case CALC_TORSION:
      return torsionCalc.compute(dataIn);
    case CALC_OOP:
      return oopCalc.compute(dataIn);
    case CALC_VDW:
      return vdwCalc.compute(dataIn);
      //case CALC_POSITION:
      //return posCalc.compute(dataIn);
      //case CALC_ES:
      //return esCalc.compute(dataIn);
    }
    return 0.0;
  }

  @Override
  String getDebugHeader(int iType) {
    switch (iType) {
    case -1:
      return "Universal Force Field -- "
          + "Rappe, A. K., et. al.; J. Am. Chem. Soc. (1992) 114(25) p. 10024-10035\n";
    default:
      return getDebugHeader2(iType);
    }
  }

  @Override
  protected Object getParameterObj(MinObject o) {
    // n/a
    return null;
  }

}
