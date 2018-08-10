/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-05-08 12:29:47 -0500 (Sun, 08 May 2016) $
 * $Revision: 21082 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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


import javajs.awt.Font;

import org.jmol.modelset.TickInfo;

public abstract class FontLineShape extends Shape {

  // Axes, Bbcage, Uccage
  
  public TickInfo[] tickInfos = new TickInfo[4];

  public Font font3d;

  @Override
  public void initShape() {
    translucentAllowed = false;
  }

  protected void setPropFLS(String propertyName, Object value) {

    if ("tickInfo" == propertyName) {
      TickInfo t = (TickInfo) value;
      if (t.ticks == null) {
        // null ticks is an indication to delete the tick info
        if (t.type.equals(" "))
          tickInfos[0] = tickInfos[1] = tickInfos[2] = tickInfos[3] = null;
        else
          tickInfos["xyz".indexOf(t.type) + 1] = null;
        return;
      }
      tickInfos["xyz".indexOf(t.type) + 1] = t;
      return;
    }
    if ("font" == propertyName) {
      font3d = (Font) value;
      return;
    }
  }

  @Override
  public String getShapeState() {
    // see StateCreator
    return null;
  }
//
//  protected String getShapeStateFL() {
//    String s = vwr.getFontState(myType, font3d);
//    return (tickInfos == null ? s : vwr.getFontLineShapeState(s, myType, tickInfos));
//  }
}
