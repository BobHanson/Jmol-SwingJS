/* $RCSfile$
 * $Author: migueljmol $
 * $Date: 2006-03-25 09:27:43 -0600 (Sat, 25 Mar 2006) $
 * $Revision: 4696 $

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.script.T;
import org.jmol.shape.Halos;
import org.jmol.util.C;
import org.jmol.util.GData;
import org.jmol.viewer.JC;


public class HalosRenderer extends ShapeRenderer {

  boolean isAntialiased;
  @Override
  protected boolean render() {
    Halos halos = (Halos) shape;
    boolean showOnce = vwr.getShowSelectedOnce();
    boolean selectDisplayTrue = (vwr.getSelectionHalosEnabled() || showOnce);
    boolean showHiddenSelections = (selectDisplayTrue && vwr.getBoolean(T.showhiddenselectionhalos));
    if (halos.mads == null && halos.bsHighlight == null && !selectDisplayTrue)
      return false;
    isAntialiased = g3d.isAntialiased();
    Atom[] atoms = ms.at;
    BS bsSelected = (showOnce && vwr.movableBitSet != null ? vwr.movableBitSet 
        : selectDisplayTrue ? vwr.bsA() : null);
    boolean needTranslucent = false;
    g3d.addRenderer(T.circle);
    for (int i = ms.ac; --i >= 0;) {
      Atom atom = atoms[i];
      if ((atom.shapeVisibilityFlags & Atom.ATOM_INFRAME) == 0)
        continue;
      boolean isHidden = ms.isAtomHidden(i);
      mad = (halos.mads == null ? 0 : halos.mads[i]);
      colix = (halos.colixes == null || i >= halos.colixes.length ? C.INHERIT_ALL
          : halos.colixes[i]);
      if (selectDisplayTrue && bsSelected.get(i)) {
        if (isHidden && !showHiddenSelections)
          continue;
        if (mad == 0)
          mad = -1; // unsized
        if (colix == C.INHERIT_ALL) {
          // don't export selection halos, even to VRML
          if (exportType == GData.EXPORT_CARTESIAN && !g3d.isWebGL())
            continue;
          colix = halos.colixSelection;
        }
        if (colix == C.USE_PALETTE)
          colix = C.GOLD;
        else if (colix == C.INHERIT_ALL)
          colix = C.getColixInherited(colix, atom.colixAtom);
      } else if (isHidden) {
        continue;
      } else {
        colix = C.getColixInherited(colix, atom.colixAtom);
      }
      if (mad != 0) {
        if (render1(atom))
          needTranslucent = true;
      }
      if (!isHidden && halos.bsHighlight != null && halos.bsHighlight.get(i)) {
        mad = -2;
        colix = halos.colixHighlight;
        if (render1(atom))
          needTranslucent = true;
      }       
    }
    return needTranslucent;
  }

  boolean render1(Atom atom) {
    short colixFill = (mad == -2 ? 0 : C.getColixTranslucent3(colix, true, 0.5f));
    boolean needTranslucent = (mad != -2);
    if (!g3d.setC(colix)) {
      needTranslucent = true;
      colix = 0;
      if (colixFill == 0 || !g3d.setC(colixFill))
        return needTranslucent;      
    }
    int z = atom.sZ;
    float d = mad;
    if (d < 0) { //unsized selection
      d = atom.sD;
      if (d == 0) {
        float ellipsemax = (atom.isVisible(JC.SHAPE_ELLIPSOIDS) ? atom.getADPMinMax(true) : 0);
        if (ellipsemax > 0)
          d = vwr.tm.scaleToScreen(z, (int) Math.floor(ellipsemax * 2000));
        if (d == 0) {
          d = (int) vwr.tm.scaleToScreen(z, mad == -2 ? 250 : 500);
        }
      }
    } else {
      d = vwr.tm.scaleToScreen(z, mad);
    }
//    System.out.println(atom + "scaleToScreen(" + z + "," + mad +")=" + d);
    if (isAntialiased)
      d /= 2;
    float more = (d / 2);
    if (mad == -2)
      more /= 2;
    if (more < 8)
      more = 8;
    if (more > 20)
      more = 20;
    d += more;
    if (isAntialiased)
      d *= 2;
    if (d < 1)
      return false;
    g3d.drawFilledCircle(colix, colixFill, (int) Math.floor(d),
        atom.sX, atom.sY, atom.sZ);
    return needTranslucent;
  }  
}
