/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-05-18 08:19:45 -0500 (Fri, 18 May 2007) $
 * $Revision: 7742 $

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

package org.jmol.modelset;



import javajs.util.BS;

import javajs.util.T3;

public class AtomIteratorWithinModelSet extends AtomIteratorWithinModel {
  private BS bsModels;

  public AtomIteratorWithinModelSet(BS bsModels) {
    this.bsModels = bsModels;
  }

  private T3 center;
  private float distance;

  @Override
  public void setCenter(T3 center, float distance) {
    this.center = center;
    this.distance = distance;
    set(0);
  }

  private boolean set(int iModel) {
    if ((modelIndex = bsModels.nextSetBit(iModel)) < 0
        || (cubeIterator = bspf.getCubeIterator(modelIndex)) == null)
      return false;
    setCenter2(center, distance);
    return true;
  }

  @Override
  public boolean hasNext() {
    if (hasNext2())
      return true;
    if (!set(modelIndex + 1))
      return false;
    return hasNext();
  }
}
