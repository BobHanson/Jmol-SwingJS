/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-05-08 13:17:03 -0500 (Sun, 08 May 2016) $
 * $Revision: 21087 $
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

package org.jmol.shape;

import org.jmol.java.BS;
import org.jmol.modelset.Text;

import javajs.awt.Font;
import javajs.util.AU;
import javajs.util.P3i;

import org.jmol.util.C;
import org.jmol.viewer.JC;

public class Hover extends TextShape {

  private final static String FONTFACE = "SansSerif";
  private final static String FONTSTYLE = "Plain";
  private final static int FONTSIZE = 12;

  public Text hoverText;
  public int atomIndex = -1;
  public P3i xy;
  public String text;
  public String labelFormat = "%U";
  public String[] atomFormats;
  public String specialLabel;

  @Override
  public void initShape() {
    isHover = true;
    Font font3d = vwr.gdata.getFont3DFSS(FONTFACE, FONTSTYLE, FONTSIZE);
    short bgcolix = C.getColixS("#FFFFC3"); // 255, 255, 195
    short colix = C.BLACK;
    currentObject = hoverText = Text.newLabel(vwr, font3d, null, colix, bgcolix, JC.TEXT_ALIGN_LEFT, 0);
    hoverText.adjustForWindow = true;
  }

  @Override
  public void setProperty(String propertyName, Object value, BS bsSelected) {

    //if (Logger.debugging) {
      //Logger.debug("Hover.setProperty(" + propertyName + "," + value + ")");
    //}

    if ("target" == propertyName) {
      if (value == null)
        atomIndex = -1;
      else {
        atomIndex = ((Integer) value).intValue();
      }
      return;
    }

    if ("text" == propertyName) {
      text = (String) value;
      if (text != null && text.length() == 0)
        text = null;
      return;
    }

    if ("specialLabel" == propertyName) {
      specialLabel = (String) value;
      return;
    }

    if ("atomLabel" == propertyName) {
      String text = (String) value;
      if (text != null && text.length() == 0)
        text = null;
      int count = vwr.ms.ac;
      if (atomFormats == null || atomFormats.length < count)
        atomFormats = new String[count];
      for (int i = bsSelected.nextSetBit(0); i >= 0; i = bsSelected.nextSetBit(i + 1))
        atomFormats[i] = text;
      return;
    }

    if ("xy" == propertyName) {
      xy = (P3i) value;
      return;
    }

    if ("label" == propertyName) {
      labelFormat = (String) value;
      if (labelFormat != null && labelFormat.length() == 0)
        labelFormat = null;
      return;
    }

    if (propertyName == "deleteModelAtoms") {
      if (atomFormats != null) {
        int firstAtomDeleted = ((int[])((Object[])value)[2])[1];
        int nAtomsDeleted = ((int[])((Object[])value)[2])[2];
        atomFormats = (String[]) AU.deleteElements(atomFormats, firstAtomDeleted, nAtomsDeleted);
      }
      atomIndex = -1;
      return;
    }
    
    setPropTS(propertyName, value, null);

  }

//  @Override
//  public String getShapeState() {
//    // not implemented -- see org.jmol.viewer.StateCreator
//    return null;
//  }
}
