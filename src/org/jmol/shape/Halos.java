/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-08-22 03:13:40 -0500 (Tue, 22 Aug 2006) $
 * $Revision: 5412 $

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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.shape;


import javajs.util.BS;

import org.jmol.modelset.Atom;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.viewer.JC;

public class Halos extends AtomShape {

  public short colixSelection = C.USE_PALETTE;

  public BS bsHighlight;
  public Integer atomWarning;
  
  public short colixHighlight = C.RED;

  void initState() {
    translucentAllowed = false;
  }

  @Override
  public void setProperty(String propertyName, Object value, BS bs) {
    if ("translucency" == propertyName)
      return;
    if ("argbSelection" == propertyName) {
      colixSelection = C.getColix(((Integer) value).intValue());
      return;
    }
    if ("argbHighlight" == propertyName) {
      colixHighlight = C.getColix(((Integer) value).intValue());
      return;
    }
    if ("highlight" == propertyName) {
      bsHighlight = (BS) value;
      if (value == null)
        atomWarning = null;
      return;
    }
    if ("warnAtom" == propertyName) {
      atomWarning = (Integer) value;
      return;
    }

    if (propertyName == JC.PROP_DELETE_MODEL_ATOMS) {
      BSUtil.deleteBits(bsHighlight, bs);
      // pass through to AtomShape
    }

    setPropAS(propertyName, value, bs);
  }

  @Override
  public void setModelVisibilityFlags(BS bs) {
    BS bsSelected = (vwr.getSelectionHalosEnabled() ? vwr.bsA() : null);
    Atom[] atoms = ms.at;
    for (int i = ms.ac; --i >= 0;) {
      if (atoms[i] != null)
        atoms[i].setShapeVisibility(vf, bsSelected != null && bsSelected.get(i)
          || mads != null && mads[i] != 0);
    }
  }

//  @Override
//  public String getShapeState() {
//    // not implemented -- see org.jmol.viewer.StateCreator
//    return null;
//  }
//
}
