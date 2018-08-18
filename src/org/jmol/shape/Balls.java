/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-05-08 12:29:47 -0500 (Sun, 08 May 2016) $
 * $Revision: 21082 $

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

import org.jmol.atomdata.RadiusData;
import org.jmol.c.PAL;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.util.C;

public class Balls extends AtomShape {

  @Override
  protected void setSize(int size, BS bsSelected) {
    // from {*}.spacefill =... , for state
    if (size == Integer.MAX_VALUE) {
      isActive = true;
      if (bsSizeSet == null)
        bsSizeSet = new BS();
      bsSizeSet.or(bsSelected);
      return;
    }
    setSize2(size, bsSelected);
  }


  @Override
  protected void setSizeRD(RadiusData rd, BS bsSelected) {
    isActive = true;
    if (bsSizeSet == null)
      bsSizeSet = new BS();
    int bsLength = Math.min(atoms.length, bsSelected.length());
    for (int i = bsSelected.nextSetBit(0); i >= 0 && i < bsLength; i = bsSelected
        .nextSetBit(i + 1)) {
      Atom atom = atoms[i];
      atom.setMadAtom(vwr, rd);
      bsSizeSet.set(i);
    }
  }

  @Override
  public void setProperty(String propertyName, Object value, BS bs) {
    if ("color" == propertyName) {
      short colix = C.getColixO(value);
      if (colix == C.INHERIT_ALL)
        colix = C.USE_PALETTE;
      if (bsColixSet == null)
        bsColixSet = new BS();
      byte pid = PAL.pidOf(value);
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        Atom atom = atoms[i];
        atom.colixAtom = getColixA(colix, pid, atom);
        bsColixSet.setBitTo(i, colix != C.USE_PALETTE
            || pid != PAL.NONE.id);
        atom.paletteID = pid;
      }
      return;
    }
    if ("colorValues" == propertyName) {
      int[] values = (int[]) value;
      if (values.length == 0)
        return;
      if (bsColixSet == null)
        bsColixSet = new BS();
      int n = 0;
      Integer color = null;
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        if (n >= values.length)
          return;
        color = Integer.valueOf(values[n++]);
        short colix = C.getColixO(color);
        if (colix == C.INHERIT_ALL)
          colix = C.USE_PALETTE;
        byte pid = PAL.pidOf(color);
        Atom atom = atoms[i];
        atom.colixAtom = getColixA(colix, pid, atom);
        bsColixSet.setBitTo(i, colix != C.USE_PALETTE
            || pid != PAL.NONE.id);
        atom.paletteID = pid;
      }
      return;
    }
    if ("colors" == propertyName) {
      Object[] data = (Object[]) value;
      short[] colixes = (short[]) data[0];
      //float translucency  = ((Float) data[1]).floatValue();
      if (bsColixSet == null)
        bsColixSet = new BS();
      short c;
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        if (i >= colixes.length ||(c = colixes[i]) == C.INHERIT_ALL)
          continue;
        
        atoms[i].colixAtom = c;
        atoms[i].paletteID = PAL.UNKNOWN.id;
        bsColixSet.set(i);
      }
      return;
    }

    if ("translucency" == propertyName) {
      boolean isTranslucent = (((String) value).equals("translucent"));
      if (bsColixSet == null)
        bsColixSet = new BS();
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        atoms[i].setTranslucent(isTranslucent, translucentLevel);
        if (isTranslucent)
          bsColixSet.set(i);
      }
      return;
    }
    if (propertyName.startsWith("ball")) {
      propertyName = propertyName.substring(4).intern();
    }
    setPropAS(propertyName, value, bs);
  }

  @Override
  public void setAtomClickability() {
    BS bsDeleted = vwr.slm.bsDeleted;
    for (int i = ac; --i >= 0;) {
      Atom atom = atoms[i];
      atom.setClickable(0);
      if (bsDeleted != null && bsDeleted.get(i)
          || (atom.shapeVisibilityFlags & vf) == 0
          || ms.isAtomHidden(i))
        continue;
      atom.setClickable(vf);
    }
  }

//  @Override
//  public String getShapeState() {
//    // not implemented -- see org.jmol.viewer.StateCreator
//    return null;
//  }
//
  /*
  boolean checkObjectHovered(int x, int y) {
    //just for debugging
    if (!vwr.getNavigationMode())
      return false;
    vwr.hoverOn(x, y, x + " " + y);
    return true;
  }
  */

}
