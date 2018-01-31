/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2015-04-30 08:17:17 -0500 (Thu, 30 Apr 2015) $
 * $Revision: 20465 $
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
package org.jmol.g3d;

import org.jmol.api.JmolRendererInterface;
import org.jmol.util.GData;

/**
 * <p>
 * Implements flat circle drawing/filling routines.
 * </p>
 * 
 * @author Miguel, miguel@jmol.org
 */
public final class CircleRenderer implements G3DRenderer {

  private Graphics3D g3d;

  public CircleRenderer() {
    // for reflection
  }

  @Override
  public G3DRenderer set(JmolRendererInterface g3d, GData gdata) {
    try {
      this.g3d = (Graphics3D) g3d;
    } catch (Exception e) {
      // must be export; not a problem
    }
    return this;
  }

  void plotCircleCenteredClipped(int xCenter, int yCenter, int zCenter,
                                 int diameter) {
    Graphics3D g = g3d;
    int c = g.argbCurrent;
    int width = g.width;
    int[] zbuf = g.zbuf;
    Pixelator p = g.pixel;
    // halo only -- simple window clip
    int r = diameter / 2;
    int sizeCorrection = 1 - (diameter & 1);
    int x = r;
    int y = 0;
    int xChange = 1 - 2 * r;
    int yChange = 1;
    int radiusError = 0;
    while (x >= y) {
      g.plotPixelClippedArgb(c, xCenter + x - sizeCorrection, yCenter + y
          - sizeCorrection, zCenter, width, zbuf, p);
      g.plotPixelClippedArgb(c, xCenter + x - sizeCorrection, yCenter - y,
          zCenter, width, zbuf, p);
      g.plotPixelClippedArgb(c, xCenter - x, yCenter + y - sizeCorrection,
          zCenter, width, zbuf, p);
      g.plotPixelClippedArgb(c, xCenter - x, yCenter - y, zCenter, width, zbuf,
          p);

      g.plotPixelClippedArgb(c, xCenter + y - sizeCorrection, yCenter + x
          - sizeCorrection, zCenter, width, zbuf, p);
      g.plotPixelClippedArgb(c, xCenter + y - sizeCorrection, yCenter - x,
          zCenter, width, zbuf, p);
      g.plotPixelClippedArgb(c, xCenter - y, yCenter + x - sizeCorrection,
          zCenter, width, zbuf, p);
      g.plotPixelClippedArgb(c, xCenter - y, yCenter - x, zCenter, width, zbuf,
          p);
      ++y;
      radiusError += yChange;
      yChange += 2;
      if (2 * radiusError + xChange > 0) {
        --x;
        radiusError += xChange;
        xChange += 2;
      }
    }
  }

  void plotCircleCenteredUnclipped(int xCenter, int yCenter, int zCenter,
                                   int diameter) {
    int r = diameter / 2;
    int sizeCorrection = 1 - (diameter & 1);
    int x = r;
    int y = 0;
    int xChange = 1 - 2 * r;
    int yChange = 1;
    int radiusError = 0;
    Graphics3D g = g3d;
    Pixelator p = g.pixel;
    int width = g.width;
    int[] zbuf = g.zbuf;
    int c = g.argbCurrent;

    while (x >= y) {
      g.plotPixelUnclipped(c, xCenter + x - sizeCorrection, yCenter + y
          - sizeCorrection, zCenter, width, zbuf, p);
      g.plotPixelUnclipped(c, xCenter + x - sizeCorrection, yCenter - y,
          zCenter, width, zbuf, p);
      g.plotPixelUnclipped(c, xCenter - x, yCenter + y - sizeCorrection,
          zCenter, width, zbuf, p);
      g.plotPixelUnclipped(c, xCenter - x, yCenter - y, zCenter, width, zbuf, p);

      g.plotPixelUnclipped(c, xCenter + y - sizeCorrection, yCenter + x
          - sizeCorrection, zCenter, width, zbuf, p);
      g.plotPixelUnclipped(c, xCenter + y - sizeCorrection, yCenter - x,
          zCenter, width, zbuf, p);
      g.plotPixelUnclipped(c, xCenter - y, yCenter + x - sizeCorrection,
          zCenter, width, zbuf, p);
      g.plotPixelUnclipped(c, xCenter - y, yCenter - x, zCenter, width, zbuf, p);
      ++y;
      radiusError += yChange;
      yChange += 2;
      if (2 * radiusError + xChange > 0) {
        --x;
        radiusError += xChange;
        xChange += 2;
      }
    }
  }

  void plotFilledCircleCenteredClipped(int xCenter, int yCenter, int zCenter,
                                       int diameter) {
    // for halo only
    int r = diameter / 2;
    int sizeCorrection = 1 - (diameter & 1);
    int x = r;
    int y = 0;
    int xChange = 1 - 2 * r;
    int yChange = 1;
    int radiusError = 0;
    Graphics3D g = g3d;
    int c = g.argbCurrent;
    int width = g.width;
    int height = g.height;
    int[] zbuf = g.zbuf;
    Pixelator p = g.pixel;

    while (x >= y) {
      plotPixelsClipped(c, 2 * x + 1 - sizeCorrection, xCenter - x, yCenter
          + y - sizeCorrection, zCenter, width, height, zbuf, p);
      plotPixelsClipped(c, 2 * x + 1 - sizeCorrection, xCenter - x, yCenter
          - y, zCenter, width, height, zbuf, p);
      plotPixelsClipped(c, 2 * y + 1 - sizeCorrection, xCenter - y, yCenter
          + x - sizeCorrection, zCenter, width, height, zbuf, p);
      plotPixelsClipped(c, 2 * y + 1 - sizeCorrection, xCenter - y, yCenter
          - x, zCenter, width, height, zbuf, p);
      ++y;
      radiusError += yChange;
      yChange += 2;
      if (2 * radiusError + xChange > 0) {
        --x;
        radiusError += xChange;
        xChange += 2;
      }
    }
  }

  private void plotPixelsClipped(int argb, int count, int x, int y, int z, int width,
                         int height, int[] zbuf, Pixelator p) {
    // for circle only; i.e. halo 
    // simple Z/window clip
    if (y < 0 || y >= height || x >= width)
      return;
    if (x < 0) {
      count += x; // x is negative, so this is subtracting -x
      x = 0;
    }
    if (count + x > width)
      count = width - x;
    if (count <= 0)
      return;
    int offsetPbuf = y * width + x;
    int offsetMax = offsetPbuf + count;
    while (offsetPbuf < offsetMax) {
      if (z < zbuf[offsetPbuf])
        p.addPixel(offsetPbuf, z, argb);
      offsetPbuf++;// += step;
    }
  }

  void plotFilledCircleCenteredUnclipped(int xCenter, int yCenter, int zCenter,
                                         int diameter) {
    // for halo only
    int r = diameter / 2;
    int x = r;
    int y = 0;
    int xChange = 1 - 2 * r;
    int yChange = 1;
    int radiusError = 0;
    Graphics3D g = g3d;
    int c = g.argbCurrent;
    int width = g.width;
    int[] zbuf = g.zbuf;
    Pixelator p = g.pixel;
    while (x >= y) {
      g.plotPixelsUnclippedCount(c, 2 * x + 1, xCenter - x, yCenter + y,
          zCenter, width, zbuf, p);
      g.plotPixelsUnclippedCount(c, 2 * x + 1, xCenter - x, yCenter - y,
          zCenter, width, zbuf, p);
      g.plotPixelsUnclippedCount(c, 2 * y + 1, xCenter - y, yCenter + x,
          zCenter, width, zbuf, p);
      g.plotPixelsUnclippedCount(c, 2 * y + 1, xCenter - y, yCenter - x,
          zCenter, width, zbuf, p);
      ++y;
      radiusError += yChange;
      yChange += 2;
      if (2 * radiusError + xChange > 0) {
        --x;
        radiusError += xChange;
        xChange += 2;
      }
    }
  }

}
