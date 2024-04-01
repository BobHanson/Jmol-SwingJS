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


import org.jmol.modelset.TickInfo;
import org.jmol.util.Font;

public abstract class FontLineShape extends Shape {

  // Axes, Bbcage, Uccage
  
  public TickInfo[] tickInfos;

  public Font font3d;

  @Override
  public void initShape() {
    translucentAllowed = false;
  }

  protected void setPropFLS(String propertyName, Object value) {

    if ("tickInfo" == propertyName) {
      TickInfo t = (TickInfo) value;
      char type = t.type;
      if (t.ticks == null) {
        // null ticks is an indication to delete the tick info
        if (t.type == ' ') {
          tickInfos = null;
          return;
        }
        if (tickInfos != null) {
          boolean haveTicks = false;
          for (int i = 0; i < 4; i++) {
            if (tickInfos[i] != null && tickInfos[i].type == t.type) {
              tickInfos[i] = null;
            } else {
              haveTicks = true;
            }
          }
          if (!haveTicks)
            tickInfos = null;
        }
        return;
      }
      if (tickInfos == null)
        tickInfos = new TickInfo[4];
      tickInfos["xyz".indexOf(type) + 1] = t;
      return;
    }
    if ("font" == propertyName) {
      font3d = (Font) value;
      return;
    }
  }

  private void checkTickinfos() {
    // TODO
    
  }

  @Override
  public String getShapeState() {
    return null;
  }
}
