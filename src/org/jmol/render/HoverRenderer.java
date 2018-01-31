/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-12-13 22:43:17 -0600 (Sat, 13 Dec 2014) $
 * $Revision: 20162 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
package org.jmol.render;

import javajs.util.P3;

import org.jmol.modelset.Atom;
import org.jmol.modelset.Text;
import org.jmol.shape.Hover;
import org.jmol.util.Txt;

public class HoverRenderer extends ShapeRenderer {
  
  private float[] tempXY = new float[3];
  private P3 ptTemp;

  @Override
  protected boolean render() {
    // hover rendering always involves translucent pass
    if (tm.isNavigating())
      return false;
    if (ptTemp == null)
      ptTemp = new P3();
    Hover hover = (Hover) shape;
    boolean antialias = g3d.isAntialiased();
    Text text = hover.hoverText;
    String label;
    if (hover.atomIndex >= 0) {
      Atom atom = ms.at[hover.atomIndex];
      label = (hover.specialLabel != null ? hover.specialLabel 
          : hover.atomFormats != null
          && hover.atomFormats[hover.atomIndex] != null ? 
              vwr.ms.getLabeler().formatLabel(vwr, atom, hover.atomFormats[hover.atomIndex], ptTemp)
          : hover.labelFormat != null ? vwr.ms.getLabeler().formatLabel(vwr, atom, fixLabel(atom, hover.labelFormat), ptTemp)
              : null);
      if (label == null)
        return false;
      text.setXYZs(atom.sX, atom.sY, 1, Integer.MIN_VALUE);
    } else if (hover.text != null) {
      label = hover.text;
      text.setXYZs(hover.xy.x, hover.xy.y, 1, Integer.MIN_VALUE);
    } else {
      return true;
    }
    if (vwr != null &&(label.indexOf("%{") >= 0 || label
        .indexOf("@{") >= 0))
      label = Txt.formatText(vwr, label);
    text.setText(label);
    //System.out.println("hoverRenderer " + text.getText());
    TextRenderer.render(text, g3d, 0, antialias ? 2 : 1, false, null, tempXY );
    return true;
  }
  
  String fixLabel(Atom atom, String label) {
    if (label == null)
      return null;
    return (vwr.ms.isJmolDataFrameForModel(atom.mi) 
        && label.equals("%U") ?"%W" : label);
  }
}
