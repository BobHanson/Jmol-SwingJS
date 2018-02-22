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
 * 
 * adds 21 cartesian H orbital contributions
 */
public class DataAdderH implements DataAdder {

  public DataAdderH() {
  }

  // expects gaussian order:
  
  //  41        7ZZZZZ      0.00000  -0.03588   0.00000   0.00000  -0.17803
  //  42        7YZZZZ      0.00000   0.00000  -0.01125   0.00000   0.00000
  //  43        7YYZZZ      0.00000  -0.00835   0.00000   0.00000  -0.04252
  //  44        7YYYZZ      0.00000   0.00000  -0.00835   0.00000   0.00000
  //  45        7YYYYZ      0.00000  -0.01125   0.00000   0.00000  -0.04439
  //  46        7YYYYY      0.00000   0.00000  -0.03588   0.00000   0.00000
  //  47        7XZZZZ      0.00000   0.00000   0.00000  -0.01125   0.00000
  //  48        7XYZZZ      0.00029   0.00000   0.00000   0.00000   0.00000
  //  49        7XYYZZ      0.00000   0.00000   0.00000  -0.01153   0.00000
  //  50        7XYYYZ      0.00029   0.00000   0.00000   0.00000   0.00000
  //  51        7XYYYY      0.00000   0.00000   0.00000  -0.01125   0.00000
  //  52        7XXZZZ      0.00000  -0.00835   0.00000   0.00000  -0.04252
  //  53        7XXYZZ      0.00000   0.00000  -0.01153   0.00000   0.00000
  //  54        7XXYYZ      0.00000  -0.01153   0.00000   0.00000   0.01674
  //  55        7XXYYY      0.00000   0.00000  -0.00835   0.00000   0.00000
  //  56        7XXXZZ      0.00000   0.00000   0.00000  -0.00835   0.00000
  //  57        7XXXYZ      0.00029   0.00000   0.00000   0.00000   0.00000
  //  58        7XXXYY      0.00000   0.00000   0.00000  -0.00835   0.00000
  //  59        7XXXXZ      0.00000  -0.01125   0.00000   0.00000  -0.04439
  //  60        7XXXXY      0.00000   0.00000  -0.01125   0.00000   0.00000
  //  61        7XXXXX      0.00000   0.00000   0.00000  -0.03588   0.00000
  
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
