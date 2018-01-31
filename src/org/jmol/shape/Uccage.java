/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-05-08 12:29:47 -0500 (Sun, 08 May 2016) $
 * $Revision: 21082 $
 *
 * Copyright (C) 2003-2006  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.shape;

import org.jmol.java.BS;
import org.jmol.viewer.JC;

public class Uccage extends FontLineShape {

  @Override
  public void setProperty(String propertyName, Object value, BS bs) {
    setPropFLS(propertyName, value);
  }
  
//  @Override
//  public String getShapeState() {
//    if (!ms.haveUnitCells)
//      return "";
//    String st = getShapeStateFL();
//    String s = st;
//    int iAtom = vwr.am.cai;
//    if (iAtom >= 0)
//      s += "  unitcell ({" + iAtom + "});\n"; 
//    SymmetryInterface uc = vwr.getCurrentUnitCell();
//    if (uc != null) { 
//      s += uc.getUnitCellState();
//      s += st; // needs to be after this state as well.
//    }
//    return s;
//  }

  @Override
  public void initShape() {
    super.initShape();
    font3d = vwr.gdata.getFont3D(JC.AXES_DEFAULT_FONTSIZE);
    myType = "unitcell";
  }
}
