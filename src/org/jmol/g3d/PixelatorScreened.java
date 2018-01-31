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

package org.jmol.g3d;

/**
 * A class to create a "screened" translucent effect by 
 * discarding pixels in a checkerboard fashion.
 * 
 */
class PixelatorScreened extends Pixelator {

  /**
   * @param g 
   * @param p0 
   */
  PixelatorScreened(Graphics3D g, Pixelator p0) {
    super(g);
    width = g.width;
    this.p0 = p0;
  }

  /**
  *   @j2sOverride
  */
  @Override
  void addPixel(int offset, int z, int p) {
    if ((offset % width) % 2 == (offset / width) % 2)
      p0.addPixel(offset, z, p);
  }
}