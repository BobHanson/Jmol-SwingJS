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
 * change  QS.MAX_TYPE_SUPPORTED if you implement this
 * 
 * adds 28 cartesian I orbital contributions
 */
public class DataAdderI implements DataAdder {

  public DataAdderI() {
  }

  // expects Gaussian order:
  
  //  62        8ZZZZZZ    -0.00110   0.00000   0.00000   0.00000   0.00000
  //  63        8YZZZZZ     0.00000   0.00000   0.00000   0.00775   0.00000
  //  64        8YYZZZZ    -0.00719   0.00000   0.00000   0.00000   0.00000
  //  65        8YYYZZZ     0.00000   0.00000   0.00000   0.00125   0.00000
  //  66        8YYYYZZ    -0.00719   0.00000   0.00000   0.00000   0.00000
  //  67        8YYYYYZ     0.00000   0.00000   0.00000   0.00775   0.00000
  //  68        8YYYYYY    -0.00110   0.00000   0.00000   0.00000   0.00000
  //  69        8XZZZZZ     0.00000   0.00000   0.00775   0.00000   0.00000
  //  70        8XYZZZZ     0.00000   0.01341   0.00000   0.00000   0.03025
  //  71        8XYYZZZ     0.00000   0.00000   0.00390   0.00000   0.00000
  //  72        8XYYYZZ     0.00000   0.00390   0.00000   0.00000   0.03999
  //  73        8XYYYYZ     0.00000   0.00000   0.01341   0.00000   0.00000
  //  74        8XYYYYY     0.00000   0.00775   0.00000   0.00000  -0.05819
  //  75        8XXZZZZ    -0.00719   0.00000   0.00000   0.00000   0.00000
  //  76        8XXYZZZ     0.00000   0.00000   0.00000   0.00390   0.00000
  //  77        8XXYYZZ    -0.01680   0.00000   0.00000   0.00000   0.00000
  //  78        8XXYYYZ     0.00000   0.00000   0.00000   0.00390   0.00000
  //  79        8XXYYYY    -0.00719   0.00000   0.00000   0.00000   0.00000
  //  80        8XXXZZZ     0.00000   0.00000   0.00125   0.00000   0.00000
  //  81        8XXXYZZ     0.00000   0.00390   0.00000   0.00000   0.03999
  //  82        8XXXYYZ     0.00000   0.00000   0.00390   0.00000   0.00000
  //  83        8XXXYYY     0.00000   0.00125   0.00000   0.00000  -0.03353
  //  84        8XXXXZZ    -0.00719   0.00000   0.00000   0.00000   0.00000
  //  85        8XXXXYZ     0.00000   0.00000   0.00000   0.01341   0.00000
  //  86        8XXXXYY    -0.00719   0.00000   0.00000   0.00000   0.00000
  //  87        8XXXXXZ     0.00000   0.00000   0.00775   0.00000   0.00000
  //  88        8XXXXXY     0.00000   0.00775   0.00000   0.00000  -0.05819
  //  89        8XXXXXX    -0.00110   0.00000   0.00000   0.00000   0.00000
  
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
