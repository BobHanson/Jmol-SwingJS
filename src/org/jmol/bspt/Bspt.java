/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2017-11-10 12:45:23 -0600 (Fri, 10 Nov 2017) $
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



import javajs.util.T3;


//import org.jmol.util.Logger;

/**
 *<p>
 * a Binary Space Partitioning Tree
 *</p>
 *<p>
 * The tree partitions n-dimensional space (in our case 3) into little boxes,
 * facilitating searches for things which are *nearby*.
 *</p>
 *<p>
 * For some useful background info, search the web for "bsp tree faq". Our
 * application is somewhat simpler because we are storing points instead of
 * polygons.
 *</p>
 *<p>
 * We are working with three dimensions. For the purposes of the Bspt code these
 * dimensions are stored as 0, 1, or 2. Each node of the tree splits along the
 * next dimension, wrapping around to 0.
 * 
 * <pre>
 * mySplitDimension = (parentSplitDimension + 1) % 3;
 * </pre>
 * 
 * A split value is stored in the node. Values which are <= splitValue are
 * stored down the left branch. Values which are >= splitValue are stored down
 * the right branch. If searchValue == splitValue then the search must proceed
 * down both branches.
 *</p>
 *<p>
 * Planar and crystaline substructures can generate values which are == along
 * one dimension.
 *</p>
 *<p>
 * To get a good picture in your head, first think about it in one dimension,
 * points on a number line. The tree just partitions the points. Now think about
 * 2 dimensions. The first node of the tree splits the plane into two rectangles
 * along the x dimension. The second level of the tree splits the subplanes
 * (independently) along the y dimension into smaller rectangles. The third
 * level splits along the x dimension. In three dimensions, we are doing the
 * same thing, only working with 3-d boxes.
 *</p>
 * 
 * @author Miguel, miguel@jmol.org
 */

public final class Bspt {

  final static int leafCountMax = 2;
  // this corresponds to the max height of the tree
  final static int MAX_TREE_DEPTH = 100;
  int treeDepth;
  int dimMax;
  int index;
  Element eleRoot;

  /**
   * Create a bspt with the specified number of dimensions. For a 3-dimensional
   * tree (x,y,z) call new Bspt(3).
   * 
   * @param dimMax
   * @param index 
   */
  public Bspt(int dimMax, int index) {
    this.dimMax = dimMax;
    this.index = index;
    reset();
  }

  void reset() {
    eleRoot = new Leaf(this, null, 0);
    treeDepth = 1;
  }

  /**
   * Iterate through all of your data points, calling addTuple
   * 
   * @param tuple
   */
  public void addTuple(T3 tuple) {
    eleRoot = eleRoot.addTuple(0, tuple);
  }

  /**
   * prints some simple stats to stdout
   */
  public void stats() {
    //    if (Logger.debugging) {
    //      Logger.debug(
    //          "bspt treeDepth=" + treeDepth +
    //          " count=" + eleRoot.count);
    //    }
  }

//  public void dump() {
//    SB sb = new SB();
//    eleRoot.dump(0, sb);
//    Logger.info(sb.toString());
//  }
//
  //    @Override
  //    public String toString() {
  //     return eleRoot.toString();
  //    }

  public CubeIterator allocateCubeIterator() {
    return new CubeIterator(this);
  }


}
