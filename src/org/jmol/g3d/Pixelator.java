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
 * 
 */
class Pixelator {
  protected final Graphics3D g;
  Pixelator p0;
  protected int[] zb, pb;
  int width;
  int bgcolor;


  /**
   * @param graphics3d
   */
  Pixelator(Graphics3D graphics3d) {
    g = graphics3d;
    bgcolor = g.bgcolor;
    setBuf();
  }

  void setBuf() {
    zb = g.zbuf;
    pb = g.pbuf;
    
  }

  void clearPixel(int offset, int z) {
    // first-pass only; ellipsoids
    if (zb[offset] > z)
      zb[offset] = Integer.MAX_VALUE;
  }
  
  void addPixel(int offset, int z, int p) {
//    if (offset == 5) {
//     System.out.println("pixelator " + Integer.toHexString(p) + " " + z);
//     System.out.println("---");
//    }
    zb[offset] = z;
    pb[offset] = p;
  }

  public void addImagePixel(byte shade, int tLog, int offset, int z, int argb,
                            int bgargb) {
//    if (zb != g.zbuf)
//      System.out.println("OH");
    if (z < zb[offset]) {
      switch (shade) {
      case 0:
        return;
      case 8:
        addPixel(offset, z, argb);
        return;
      default:
        // shade is a log of translucency, so adding two is equivalent to
        // multiplying them. Works like a charm! - BH 
        shade += tLog;
        if (shade <= 7) {
          int p = pb[offset];
          if (bgargb != 0)
            p = Graphics3D.mergeBufferPixel(p, bgargb, bgargb);
          p = Graphics3D.mergeBufferPixel(p, (argb & 0xFFFFFF)
              | (shade << 24), bgcolor);
          addPixel(offset, z, p);
        }
      }
    }
  }

}