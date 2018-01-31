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

import javajs.util.M4;

class HallRotation {
  String rotCode;
  //int order;
  M4 seitzMatrix = new M4();
  M4 seitzMatrixInv = new M4();
  
  private HallRotation (String code, String matrixData) {
    rotCode = code;
    //order = code.charAt(0) - '0';
    float[] data = new float[16];
    float[] dataInv = new float[16];
    data[15] = dataInv[15] = 1f;
    
    for (int i = 0, ipt = 0; ipt < 11; i++) {
      int value = 0;
      switch(matrixData.charAt(i)) {
      case ' ':
        ipt++;
        continue;
      case '+':
      case '1':
        value = 1;
        break;
      case '-':
        value = -1;
        break;
      }
      data[ipt] = value;
      dataInv[ipt] = -value; 
      ipt++;
    }
    seitzMatrix.setA(data);
    seitzMatrixInv.setA(dataInv);
  }
  
  final static HallRotation lookup(String code) {
    for (int i = getHallTerms().length; --i >= 0;)
      if (hallRotationTerms[i].rotCode.equals(code))
        return hallRotationTerms[i];
    return null;
  }
  
  private static HallRotation[] hallRotationTerms;
  
  private synchronized static HallRotation[] getHallTerms() {
    // in matrix definitions, "+" = 1; "-" = -1;
    // just a compact way of indicating a 3x3
    return (hallRotationTerms == null ? hallRotationTerms = new HallRotation[] {
        new HallRotation("1_"   , "+00 0+0 00+")
      , new HallRotation("2x"   , "+00 0-0 00-")
      , new HallRotation("2y"   , "-00 0+0 00-")
      , new HallRotation("2z"   , "-00 0-0 00+")
      , new HallRotation("2'"   , "0-0 -00 00-") //z implied
      , new HallRotation("2\""  , "0+0 +00 00-") //z implied
      , new HallRotation("2x'"  , "-00 00- 0-0")
      , new HallRotation("2x\"" , "-00 00+ 0+0")
      , new HallRotation("2y'"  , "00- 0-0 -00")
      , new HallRotation("2y\"" , "00+ 0-0 +00")
      , new HallRotation("2z'"  , "0-0 -00 00-")
      , new HallRotation("2z\"" , "0+0 +00 00-")
      , new HallRotation("3x"   , "+00 00- 0+-")
      , new HallRotation("3y"   , "-0+ 0+0 -00")
      , new HallRotation("3z"   , "0-0 +-0 00+")
      , new HallRotation("3*"   , "00+ +00 0+0")
      , new HallRotation("4x"   , "+00 00- 0+0")
      , new HallRotation("4y"   , "00+ 0+0 -00")
      , new HallRotation("4z"   , "0-0 +00 00+")
      , new HallRotation("6x"   , "+00 0+- 0+0")
      , new HallRotation("6y"   , "00+ 0+0 -0+")
      , new HallRotation("6z"   , "+-0 +00 00+")
    } : hallRotationTerms);
  }
}