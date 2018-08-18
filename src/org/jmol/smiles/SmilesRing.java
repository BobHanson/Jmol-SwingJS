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
import org.jmol.util.Edge;

/**
 * Ring of (tentatively) aromatic nodes and edges
 */

class SmilesRing extends BS {

  SmilesRingSet set;
  Lst<Edge> edges;
  BS bsEdgesToCheck;
  boolean isOK;
  int n;

  SmilesRing(int n, BS atoms, Lst<Edge> edges, boolean isOK) {
    this.or(atoms);
    this.edges = edges;
    this.isOK = isOK;
    this.n = n;
  }

  void addEdges(Hashtable<String, SmilesRingSet> htEdgeMap) {
    for (int i = edges.size(); --i >= 0;)
      htEdgeMap.put(getKey(edges.get(i)), set);
  }

  static SmilesRingSet getSetByEdge(Edge edge,
                                           Hashtable<String, SmilesRingSet> htEdgeMap) {
    return htEdgeMap.get(getKey(edge));
  }

  private static String getKey(Edge e) {
    int i = e.getAtomIndex1();
    int j = e.getAtomIndex2();
    return (i < j ? i + "_" + j : j + "_" + i);
  }

}
