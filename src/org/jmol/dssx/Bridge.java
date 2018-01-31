/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.dssx;

import java.util.Map;

import org.jmol.modelset.Atom;
import org.jmol.util.Escape;

/**
 * A class for cataloging ladder bridges in a DSSP calculation
 * 
 * @author hansonr
 * 
 */
class Bridge {
  Atom a, b;
  int[][] ladder;
  boolean isAntiparallel;
  
  Bridge(Atom a, Atom b, Map<int[][], Boolean> htLadders) {
    this.a = a;
    this.b = b;
    ladder = new int[2][2];
    ladder[0][0] = ladder[0][1] = Math.min(a.i, b.i);
    ladder[1][0] = ladder[1][1] = Math.max(a.i, b.i);
    addLadder(htLadders);
  }
  
  boolean addBridge(Bridge bridge,  Map<int[][], Boolean> htLadders) {
    if (bridge.isAntiparallel != isAntiparallel
        || !canAdd(bridge) || !bridge.canAdd(this))
      return false;
    extendLadder(bridge.ladder[0][0], bridge.ladder[1][0]);
    extendLadder(bridge.ladder[0][1], bridge.ladder[1][1]);
    bridge.ladder = ladder;
    if (bridge.ladder != ladder) {
      htLadders.remove(bridge.ladder);
      addLadder(htLadders);
    }
    return true;
  }

  private void addLadder(Map<int[][], Boolean> htLadders) {
    htLadders.put(ladder, (isAntiparallel ? Boolean.TRUE : Boolean.FALSE));
  }

  private boolean canAdd(Bridge bridge) {
    int index1 = bridge.a.i;
    int index2 = bridge.b.i;
    // no crossing of ladder rungs (2WUJ)
    return (isAntiparallel ?
        (index1 >= ladder[0][1] && index2 <= ladder[1][0] 
        || index1 <= ladder[0][0] && index2 >= ladder[1][1]) 
      : (index1 <= ladder[0][0] && index2 <= ladder[1][0] 
        || index1 >= ladder[0][1] && index2 >= ladder[1][1]));
  }

  private void extendLadder(int index1, int index2) {
    if (ladder[0][0] > index1)
      ladder[0][0] = index1;
    if (ladder[0][1] < index1)
      ladder[0][1] = index1;
    if (ladder[1][0] > index2)
      ladder[1][0] = index2;
    if (ladder[1][1] < index2)
      ladder[1][1] = index2;
  }
  
  @Override
  public String toString() {
    return (isAntiparallel ? "a " : "p ") + a + " - " + b + "\t" + Escape.e(ladder);
  }

}