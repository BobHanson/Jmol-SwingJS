/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-05-13 19:17:06 -0500 (Sat, 13 May 2006) $
 * $Revision: 5114 $
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
package org.jmol.quantum.mo;

import org.jmol.quantum.MOCalculation;



/*
 * NOTE -- THIS CLASS IS INSTANTIATED USING Interface.getOptionInterface
 * NOT DIRECTLY -- FOR MODULARIZATION. NEVER USE THE CONSTRUCTOR DIRECTLY!
 * 
 */

/**
 * 
 * change  QS.MAX_TYPE_SUPPORTED if you implement this

 * adds 15 cartesian G orbital contributions
 */
public class DataAdderG implements DataAdder {

  // expects ordering same as Gaussian:
  
  //  26        6ZZZZ      -0.02806   0.00000   0.00000   0.00000   0.00000
  //  27        6YZZZ       0.00000   0.00000   0.00000  -0.00484   0.00000
  //  28        6YYZZ      -0.00287   0.00000   0.00000   0.00000   0.00000
  //  29        6YYYZ       0.00000   0.00000   0.00000  -0.00484   0.00000
  //  30        6YYYY      -0.02806   0.00000   0.00000   0.00000   0.00000
  //  31        6XZZZ       0.00000   0.00000  -0.00484   0.00000   0.00000
  //  32        6XYZZ       0.00000  -0.00249   0.00000   0.00000  -0.05029
  //  33        6XYYZ       0.00000   0.00000  -0.00249   0.00000   0.00000
  //  34        6XYYY       0.00000  -0.00484   0.00000   0.00000   0.03779
  //  35        6XXZZ      -0.00287   0.00000   0.00000   0.00000   0.00000
  //  36        6XXYZ       0.00000   0.00000   0.00000  -0.00249   0.00000
  //  37        6XXYY      -0.00287   0.00000   0.00000   0.00000   0.00000
  //  38        6XXXZ       0.00000   0.00000  -0.00484   0.00000   0.00000
  //  39        6XXXY       0.00000  -0.00484   0.00000   0.00000   0.03779
  //  40        6XXXX      -0.02806   0.00000   0.00000   0.00000   0.00000

  // NOT the same as Molden, which is:
  
  //  15G: xxxx yyyy zzzz xxxy xxxz yyyx yyyz zzzx zzzy,
  //       xxyy xxzz yyzz xxyz yyxz zzxy
  
  public DataAdderG() {
  }

  @Override
  public boolean addData(MOCalculation calc, boolean havePoints) {
    switch (calc.normType) {
    case MOCalculation.NORM_NONE:
    default:
      return false;
    case MOCalculation.NORM_NBO:
      return false;
    case MOCalculation.NORM_STANDARD:
      return false;
    case MOCalculation.NORM_NWCHEM:
      return false;
    }
    //return true;
  }
}
