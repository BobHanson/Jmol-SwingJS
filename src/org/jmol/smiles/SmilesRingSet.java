/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-02-04 21:46:46 -0600 (Thu, 04 Feb 2016) $
 * $Revision: 20945 $
 *
 * Copyright (C) 2005  The Jmol Development Team
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

package org.jmol.smiles;

import java.util.Hashtable;

import javajs.util.Lst;

import javajs.util.BS;

/**
 * a list of fused rings
 */

class SmilesRingSet extends Lst<SmilesRing> {

  BS bs = new BS();

  SmilesRingSet() {
  }

  void addSet(SmilesRingSet set, Hashtable<String, SmilesRingSet> htEdgeMap) {
    for (int i = set.size(); --i >= 0;) {
      SmilesRing r = set.get(i);
      addRing(r);
      r.addEdges(htEdgeMap);
    }
  }

  void addRing(SmilesRing ring) {
    addLast(ring);
    ring.set = this;
    bs.or(ring);
  }

  int getElectronCount(int[] eCounts) {
    int eCount = 0;
    for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1))
      eCount += eCounts[j];
    return eCount;
  }
}
