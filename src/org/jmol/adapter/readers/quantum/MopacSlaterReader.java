/* $RCSfile$
 * $Author: egonw $
 * $Date: 2006-03-18 15:59:33 -0600 (Sat, 18 Mar 2006) $
 * $Revision: 4652 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.adapter.readers.quantum;

/**
 * 
 * @author hansonr <hansonr@stolaf.edu>
 */
abstract class MopacSlaterReader extends SlaterReader {

  protected final static float MIN_COEF = 0.0001f;  // sufficient?  
  protected int[] atomicNumbers;
  
  final private static int [] sphericalDValues = new int[] { 
    // MOPAC2007 graphf output data order
    0, -2, 0, //dx2-y2
    1,  0, 1, //dxz
   -2,  0, 0, //dz2
    0,  1, 1, //dyz
    1,  1, 0, //dxy
  };

  /**
   * We have the type as a string and need to translate that
   * to exponents for x, y, z, and r. No F here.
   * 
   * @param iAtom
   * @param atomicNumber
   * @param type
   * @param zeta
   * @param coef
   */
  protected void createSphericalSlaterByType(int iAtom, int atomicNumber,
                                             String type, float zeta, float coef) {
    int pt = "S Px Py Pz  Dx2-y2Dxz Dz2 Dyz Dxy".indexOf(type);
           // 0 2  5  8   12    18  22  26  30
    switch (pt) {
    case 0: // s
      addSlater(iAtom + 1, 0, 0, 0, getNPQs(atomicNumber) - 1, zeta, coef);
      return;
    case 2: // Px
    case 5: // Py
    case 8: // Pz
      addSlater(iAtom + 1, pt == 2 ? 1 : 0, pt == 5 ? 1 : 0, pt == 8 ? 1 : 0,
          getNPQp(atomicNumber) - 2, zeta, coef);
      return;
    }
    pt = (pt >> 2) * 3 - 9;   // 12->0, 18->3, 22->6, 26->9, 30->12
    addSlater(iAtom + 1, sphericalDValues[pt++], sphericalDValues[pt++],
        sphericalDValues[pt++], getNPQd(atomicNumber) - 3, zeta,
        coef);
  }  

  /**
   * overrides method in SlaterReader to allow for MOPAC's treatment of 
   * the radial exponent differently depending upon position in
   * the periodic table -- noble gases and transition metals
   * and for the fact that these are spherical functions (5D, not 6D)
   * 
   * ignores any F orbitals.
   * 
   * @param ex
   * @param ey
   * @param ez
   * @param er
   * @param zeta
   * @return scaling factor
   */
  @Override
  protected double scaleSlater(int ex, int ey, int ez, int er, double zeta) {
    if (ex >= 0 && ey >= 0) {
      // no need for special attention here
      return super.scaleSlater(ex, ey, ez, er, zeta);
    }
    int el = Math.abs(ex + ey + ez);
    if (el == 3) {
      return 0; // not set up for spherical f
    }

    // A negative zeta means this is contracted, so 
    // there are not as many molecular orbital 
    // coefficients as there are slaters. For example, 
    // an atom's s orbital might have one coefficient
    // for a set of three slaters -- the contracted set.

    return getSlaterConstDSpherical(el + er + 1, Math.abs(zeta), ex, ey);
  }

  /*
   * Sincere thanks to Jimmy Stewart, MrMopac@att.net for these constants
   * 
   */

  ///////////// MOPAC CALCULATION SLATER CONSTANTS //////////////

  // see http://openmopac.net/Downloads/Mopac_7.1source.zip|src_modules/parameters_C.f90

  //H                                                             He
  //Li Be                                          B  C  N  O  F  Ne
  //Na Mg                                          Al Si P  S  Cl Ar
  //K  Ca Sc          Ti V  Cr Mn Fe Co Ni Cu Zn   Ga Ge As Se Br Kr
  //Rb Sr Y           Zr Nb Mo Tc Ru Rh Pd Ag Cd   In Sn Sb Te I  Xe
  //Cs Ba La Ce-Lu    Hf Ta W  Re Os Ir Pt Au Hg   Tl Pb Bi Po At Rn
  //Fr Ra Ac Th-Lr    ?? ?? ?? ??

  private final static int[] principalQuantumNumber = new int[] { 0, 
      1, 1, //  2
      2, 2, 2, 2, 2, 2, 2, 2, // 10
      3, 3, 3, 3, 3, 3, 3, 3, // 18
      4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, // 36
      5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, // 54
      6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
      6, 6, 6, 6, 6, 6, 6, 6, // 86
  };

  private final static int[] npqd = new int[] { 0,
    0, 3, // 2
    0, 0, 0, 0, 0, 0, 0, 3, // 10
    3, 3, 3, 3, 3, 3, 3, 4, // 18
    3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 5, // 36
    4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 6, // 54
    5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
    5, 6, 6, 6, 6, 6, 6, 7, // 86
  };

  private final static int getNPQ(int atomicNumber) {
    return (atomicNumber < principalQuantumNumber.length ? 
        principalQuantumNumber[atomicNumber] : 0);
  }

  /**
   * for S orbitals, MOPAC adds 1 to n for noble gases other than helium
   * 
   * @param atomicNumber
   * @return  adjusted principal quantum number
   */
  private final static int getNPQs(int atomicNumber) {
    int n = getNPQ(atomicNumber);
    switch (atomicNumber) {
    case 10:
    case 18:
    case 36:
    case 54:
    case 86:
      return n + 1;
    default:
      return n;        
    }
  }

  /**
   * for P orbitals, MOPAC adds 1 to n for helium only
   * 
   * @param atomicNumber
   * @return  adjusted principal quantum number
   */
  private final static int getNPQp(int atomicNumber) {
    int n = getNPQ(atomicNumber);
    switch (atomicNumber) {
    case 2:
      return n + 1;
    default:
      return n;        
    }
  }

  /**
   * for D orbitals, MOPAC adds 1 to n for noble gases 
   * but subtracts 1 from n for transition metals
   * 
   * @param atomicNumber
   * @return  adjusted principal quantum number
   */
  private final static int getNPQd(int atomicNumber) {
    return (atomicNumber < npqd.length ? npqd[atomicNumber] : 0);
  }

}
