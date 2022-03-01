/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-10-08 12:28:44 -0500 (Sat, 08 Oct 2016) $
 * $Revision: 21258 $

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
import org.jmol.shape.Balls;
import org.jmol.shape.Shape;

public class BallsRenderer extends ShapeRenderer {

  @Override
  protected boolean render() {
    boolean needTranslucent = false;
    if (isExport || vwr.checkMotionRendering(T.atoms)) {
      Atom[] atoms = ms.at;
      short[] colixes = ((Balls) shape).colixes;
      BS bsOK = vwr.shm.bsRenderableAtoms;
      for (int i = bsOK.nextSetBit(0); i >= 0; i = bsOK.nextSetBit(i + 1)) {
        if (atoms == null || atoms[i] == null)
          return false;
        Atom atom = atoms[i];
        if (atom.sD > 0
            && (atom.shapeVisibilityFlags & myVisibilityFlag) != 0) {
          if (g3d.setC(colixes == null ? atom.colixAtom : Shape.getColix(colixes, i, atom))) {
            g3d.drawAtom(atom, 0);
          } else {
            needTranslucent = true;
          }
        }
      }
    }
    return needTranslucent;
  }

}
