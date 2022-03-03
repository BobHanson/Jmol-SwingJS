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

import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.V3;

public class Constraint {

  public final static int TYPE_SYMMETRY = 0;
  public final static int TYPE_DISTANCE = 1;
  public final static int TYPE_ANGLE    = 2;
  public final static int TYPE_DIHEDRAL = 3;
  public final static int TYPE_VECTOR = 4;
  public final static int TYPE_PLANE = 5;
  public final static int TYPE_LOCKED = 6;

  int type;
  
  private String symop;
  private P3[] points;
  private P3 offset;
  private P4 plane;
  private V3 unitVector;
  private double value;
    
  public Constraint(int type, Object[] params) throws IllegalArgumentException {
    this.type = type;
    switch (type) {
    case TYPE_LOCKED:
      break;
    case TYPE_VECTOR:
      offset = (P3) params[0];
      unitVector = V3.newVsub((P3) params[1], offset);
      unitVector.normalize();
      break;
    case TYPE_PLANE:
      this.plane = (P4) params[0];
      break;
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

  public void constrain(P3 ptOld, P3 ptNew) {
    V3 v = new V3();
    P3 p = P3.newP(ptOld);
    switch (type) {
    case TYPE_LOCKED:
      ptNew.x = Float.NaN;
      return;
    case TYPE_VECTOR:
      Measure.projectOntoAxis(p, offset, unitVector, v);
      if (p.distanceSquared(ptOld) > JC.UC_TOLERANCE2) {
        ptNew.x = Float.NaN;
      } else {
        Measure.projectOntoAxis(ptNew, offset, unitVector, v);
      }
      break;
    case TYPE_PLANE:
      if (Math.abs(Measure.getPlaneProjection(p, plane, v, v)) > 0.01f) {
        ptNew.x = Float.NaN;
      } else {
        Measure.getPlaneProjection(ptNew, plane, v, v);
        ptNew.setT(v);
      }
      break;
    }
  }
  
}
