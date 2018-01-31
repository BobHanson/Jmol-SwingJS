/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
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


package org.jmol.symmetry;

/*
 * Bob Hanson 9/2006
 * 
 * references: International Tables for Crystallography Vol. A. (2002) 
 *
 * http://www.iucr.org/iucr-top/cif/cifdic_html/1/cif_core.dic/Ispace_group_symop_operation_xyz.html
 * http://www.iucr.org/iucr-top/cif/cifdic_html/1/cif_core.dic/Isymmetry_equiv_pos_as_xyz.html
 *
 * LATT : http://macxray.chem.upenn.edu/LATT.pdf thank you, Patrick Carroll
 * 
 * Hall symbols:
 * 
 * http://cci.lbl.gov/sginfo/hall_symbols.html
 * 
 * and
 * 
 * http://cci.lbl.gov/cctbx/explore_symmetry.html
 * 
 * (-)L   [N_A^T_1]   [N_A^T_2]   ...  [N_A^T_P]   V(Nx Ny Nz)
 * 
 * lattice types S and T are not supported here
 * 
 * NEVER ACCESS THESE METHODS OUTSIDE OF THIS PACKAGE
 * 
 *
 */

import javajs.util.P3i;

class HallTranslation {
  
  char translationCode = '\0';
  int rotationOrder;
  int rotationShift12ths;
  P3i vectorShift12ths;

  HallTranslation(char translationCode, P3i params) {
    this.translationCode = translationCode;
    if (params != null) {
      if (params.z >= 0) {
        // just a shift
        vectorShift12ths = params;
        return;
      }
      // just a screw axis
      rotationOrder = params.x;
      rotationShift12ths = params.y;
    }
    vectorShift12ths = new P3i();
  }
  
  final static String getHallLatticeEquivalent(int latticeParameter) {
   // SHELX LATT --> Hall term
    char latticeCode = HallTranslation.getLatticeCode(latticeParameter);
    boolean isCentrosymmetric = (latticeParameter > 0);
    return (isCentrosymmetric ? "-" : "") + latticeCode + " 1";
  }
  
  final static int getLatticeIndex(char latt) {
    /*
     * returns lattice code (1-9, including S and T) for a given lattice type
     * 1-7 match SHELX codes
     * 
     */
    for (int i = 1, ipt = 3; i <= nLatticeTypes; i++, ipt+=3)
      if (latticeTranslationData[ipt].charAt(0) == latt)
        return i;
    return 0;
  }
  
  /**
   * 
   * @param latt SHELX index or character
   * @return lattice character P I R F A B C S T or \0
   * 
   */
  final static char getLatticeCode(int latt) {
    if (latt < 0)
      latt = -latt;
    return (latt == 0 ? '\0' : latt > nLatticeTypes ?
        getLatticeCode(getLatticeIndex((char)latt))
        : latticeTranslationData[latt * 3].charAt(0));
  }

  final static String getLatticeDesignation(int latt) {
    boolean isCentrosymmetric = (latt > 0);
    String str = (isCentrosymmetric ? "-" : "");
    if (latt < 0)
      latt = -latt;
    if (latt == 0 || latt > nLatticeTypes)
      return "";
    return str + getLatticeCode(latt) + ": "
        + (isCentrosymmetric ? "centrosymmetric " : "")
        + latticeTranslationData[latt * 3 + 1];
  }  
 
  final static String getLatticeDesignation2(char latticeCode, boolean isCentrosymmetric) {
    int latt = getLatticeIndex(latticeCode);
    if (!isCentrosymmetric)
      latt = - latt;
    return getLatticeDesignation(latt);
  }  
 
  final static String getLatticeExtension(char latt, boolean isCentrosymmetric) {
    /*
     * returns a set of rotation terms that are equivalent to the lattice code
     * 
     */
    for (int i = 1, ipt = 3; i <= nLatticeTypes; i++, ipt += 3)
      if (latticeTranslationData[ipt].charAt(0) == latt)
        return latticeTranslationData[ipt + 2] + (isCentrosymmetric ? " -1" : "");
    return "";
  }

  final static String[] latticeTranslationData = {
    "\0", "unknown",         ""
    ,"P", "primitive",       ""
    ,"I", "body-centered",   " 1n"
    ,"R", "rhombohedral",    " 1r 1r"
    ,"F", "face-centered",   " 1ab 1bc 1ac"
    ,"A", "A-centered",      " 1bc"
    ,"B", "B-centered",      " 1ac"
    ,"C", "C-centered",      " 1ab"
    ,"S", "rhombohedral(S)", " 1s 1s"
    ,"T", "rhombohedral(T)", " 1t 1t"
  };
  
  final static int nLatticeTypes = latticeTranslationData.length/3 - 1;
 

  private static HallTranslation[] hallTranslationTerms;
  
  private synchronized static HallTranslation[] getHallTerms() {
    return (hallTranslationTerms == null ? hallTranslationTerms = new HallTranslation[] {
      new HallTranslation('a', P3i.new3(6, 0, 0))
      , new HallTranslation('b', P3i.new3(0, 6, 0))
      , new HallTranslation('c', P3i.new3(0, 0, 6))
      , new HallTranslation('n', P3i.new3(6, 6, 6))
      , new HallTranslation('u', P3i.new3(3, 0, 0))
      , new HallTranslation('v', P3i.new3(0, 3, 0))
      , new HallTranslation('w', P3i.new3(0, 0, 3))
      , new HallTranslation('d', P3i.new3(3, 3, 3))
      , new HallTranslation('1', P3i.new3(2, 6, -1))
      , new HallTranslation('1', P3i.new3(3, 4, -1))
      , new HallTranslation('2', P3i.new3(3, 8, -1))
      , new HallTranslation('1', P3i.new3(4, 3, -1))
      , new HallTranslation('3', P3i.new3(4, 9, -1))
      , new HallTranslation('1', P3i.new3(6, 2, -1))
      , new HallTranslation('2', P3i.new3(6, 4, -1))
      , new HallTranslation('4', P3i.new3(6, 8, -1))
      , new HallTranslation('5', P3i.new3(6, 10, -1))
      // extension to handle rhombohedral lattice as primitive
      , new HallTranslation('r', P3i.new3(4, 8, 8))
      , new HallTranslation('s', P3i.new3(8, 8, 4))
      , new HallTranslation('t', P3i.new3(8, 4, 8))
    } : hallTranslationTerms);
  }

  static HallTranslation getHallTranslation(char translationCode, int order) {
    HallTranslation ht = null;
    for (int i = getHallTerms().length; --i >= 0;) {
      HallTranslation h = hallTranslationTerms[i];
      if (h.translationCode == translationCode) {
        if (h.rotationOrder == 0 || h.rotationOrder == order) {
          ht = new HallTranslation(translationCode, null);
          ht.translationCode = translationCode;
          ht.rotationShift12ths = h.rotationShift12ths;
          ht.vectorShift12ths = h.vectorShift12ths;
          return ht;
        }
      }
    }
    return ht;
  }
}

