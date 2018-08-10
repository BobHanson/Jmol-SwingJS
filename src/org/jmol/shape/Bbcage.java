/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-05-08 12:29:47 -0500 (Sun, 08 May 2016) $
 * $Revision: 21082 $
 *
 * Copyright (C) 2002-2006  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.shape;


import org.jmol.java.BS;
import org.jmol.viewer.JC;

public class Bbcage extends FontLineShape {

  @Override
  public void setProperty(String propertyName, Object value, BS bs) {
    setPropFLS(propertyName, value);
  }
  
  @Override
  public void initShape() {
    super.initShape();
    font3d = vwr.gdata.getFont3D(JC.AXES_DEFAULT_FONTSIZE);
    myType = "boundBox";
  }

  public boolean isVisible;
  
  @Override
  public void setModelVisibilityFlags(BS bs) {
    BS bboxModels;
    isVisible = (vwr.getShowBbcage() && ((bboxModels = vwr.ms.bboxModels) == null || bs
        .intersects(bboxModels)));
  }
  
//@Override
//public String getShapeState() {
//  // not implemented -- see org.jmol.viewer.StateCreator
//  return null;
//}

}
