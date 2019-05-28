/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-05-11 15:47:18 -0500 (Tue, 11 May 2010) $
 * $Revision: 13064 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
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
package org.jmol.modelkit;

import javajs.util.P3;

public class Constraint {

  public final static int TYPE_SYMMETRY = 0;
  public final static int TYPE_DISTANCE = 1;
  public final static int TYPE_ANGLE    = 2;
  public final static int TYPE_DIHEDRAL = 3;
  
  int type;
  
  private String symop;
  private P3[] points;
  private P3 offset;
  private double value;
    
  public Constraint(int type, Object... params) throws IllegalArgumentException {
    this.type = type;
    switch (type) {
    case TYPE_SYMMETRY:
      symop = (String) params[0];
      points = new P3[1];
      offset = (P3) params[1];
      break;
    case TYPE_DISTANCE:
      value = ((Double) params[0]).doubleValue();
      points = new P3[] { (P3) params[1], null };
      break;
    case TYPE_ANGLE:
      value = ((Double) params[0]).doubleValue();
      points = new P3[] { (P3) params[1], (P3) params[2], null };
      break;
    case TYPE_DIHEDRAL:
      value = ((Double) params[0]).doubleValue();
      points = new P3[] { (P3) params[1], (P3) params[2], (P3) params[3], null };
      break;
    default:
      throw new IllegalArgumentException();
    }
    
    
  }
  
}
