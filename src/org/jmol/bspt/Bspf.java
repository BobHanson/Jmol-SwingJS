/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-04-10 22:47:52 -0500 (Sun, 10 Apr 2016) $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.bspt;




import javajs.util.AU;
import javajs.util.P3;

import org.jmol.java.BS;

/**
 * A Binary Space Partitioning Forest
 *<p>
 * This is simply an array of Binary Space Partitioning Trees identified
 * by indexes
 *
 * @author Miguel, miguel@jmol.org
*/

public final class Bspf {

  int dimMax;
  public Bspt[] bspts;
  public boolean isValid = false;
  boolean[] bsptsValid;
  
  public void validateModel(int i, boolean isValid) {
    bsptsValid[i] = isValid;
  }

  public boolean isInitializedIndex(int bsptIndex) {
    return bspts.length > bsptIndex && bspts[bsptIndex] != null
        && bsptsValid[bsptIndex];
  }
  
  CubeIterator[] cubeIterators;
  
  public Bspf(int dimMax) {
    this.dimMax = dimMax;
    bspts = new Bspt[1];
    bsptsValid = new boolean[1];
    cubeIterators = new CubeIterator[0];
  }

  public void addTuple(int bsptIndex, P3 tuple) {
    if (bsptIndex >= bspts.length) {
      bspts = (Bspt[]) AU.arrayCopyObject(bspts, bsptIndex + 1);
      bsptsValid = AU.arrayCopyBool(bsptsValid, bsptIndex + 1);
    }
    Bspt bspt = bspts[bsptIndex];
    if (bspt == null) {
      bspt = bspts[bsptIndex] = new Bspt(dimMax, bsptIndex);
    }
    bspt.addTuple(tuple);
  }

  public void stats() {
    for (int i = 0; i < bspts.length; ++i)
      if (bspts[i] != null)
        bspts[i].stats();
  }

  
//  public void dump() {
//    for (int i = 0; i < bspts.length; ++i) {
//      Logger.info(">>>>\nDumping bspt #" + i + "\n>>>>");
//      bspts[i].dump();
//    }
//    Logger.info("<<<<");
//  }
  
  /**
   * @param bsptIndex
   *        a model index
   * @return either a cached or a new CubeIterator
   * 
   */
  public CubeIterator getCubeIterator(int bsptIndex) {
    if (bsptIndex < 0)
      return getNewCubeIterator(-1 - bsptIndex);
    if (bsptIndex >= cubeIterators.length)
      cubeIterators = (CubeIterator[]) AU.arrayCopyObject(cubeIterators,
          bsptIndex + 1);
    if (cubeIterators[bsptIndex] == null && bspts[bsptIndex] != null)
      cubeIterators[bsptIndex] = getNewCubeIterator(bsptIndex);
    cubeIterators[bsptIndex].set(bspts[bsptIndex]);
    return cubeIterators[bsptIndex];
  }

  public CubeIterator getNewCubeIterator(int bsptIndex) {
      return bspts[bsptIndex].allocateCubeIterator();
  }

  public synchronized void initialize(int modelIndex, P3[] atoms, BS modelAtomBitSet) {
    if (bspts[modelIndex] != null)
      bspts[modelIndex].reset();
    for (int i = modelAtomBitSet.nextSetBit(0); i >= 0; i = modelAtomBitSet.nextSetBit(i + 1))
      addTuple(modelIndex, atoms[i]);
    bsptsValid[modelIndex] = true;
  }

}
