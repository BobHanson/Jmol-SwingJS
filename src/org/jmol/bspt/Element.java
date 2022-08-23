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


import javajs.util.T3d;
import javajs.util.T3d;

/**
 * the internal tree is made up of elements ... either Node or Leaf
 *
 * @author Miguel, miguel@jmol.org
 */
abstract class Element {
  Bspt bspt;
  int count;
  abstract Element addTuple(int level, T3d tuple);
  
  //abstract void dump(int level, SB sb);
  
}

