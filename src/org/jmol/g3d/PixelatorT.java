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
 * Handle pixels in Pass 2 - Translucency
 * 
 */
class PixelatorT extends Pixelator {
  PixelatorT(Graphics3D graphics3d) {
    super(graphics3d);
  }

  /**
   * @param offset  
   * @param z 
   */
  @Override
  void clearPixel(int offset, int z) {
    // ignore in pass 2
  }
  
  /**
  *   @j2sOverride
  */
  @Override
  void addPixel(int offset, int z, int p) {
    int zT = g.zbufT[offset];
    if (z < zT) {
      // new in front -- merge old translucent with opaque
      // if (zT != Integer.MAX_VALUE)
      int argb = g.pbufT[offset];
      if (!g.translucentCoverOnly && argb != 0 && zT - z > g.zMargin)
        pb[offset] = Graphics3D.mergeBufferPixel(pb[offset], argb, g.bgcolor);
      g.zbufT[offset] = z;
      g.pbufT[offset] = p & g.translucencyMask;
    } else if (z == zT) {
    } else if (!g.translucentCoverOnly && z - zT > g.zMargin) {
        // oops-out of order
        pb[offset] = Graphics3D.mergeBufferPixel(pb[offset], p & g.translucencyMask, g.bgcolor);
    }
  }
}