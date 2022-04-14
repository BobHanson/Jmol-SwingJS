/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2017-02-12 13:55:04 -0600 (Sun, 12 Feb 2017) $
 * $Revision: 21428 $
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

/**
 *<p>
 * Implements 3D line drawing routines.
 *</p>
 *<p>
 * A number of line drawing routines, most of which are used to
 * implement higher-level shapes. Triangles and cylinders are drawn
 * as a series of lines
 *</p>
 *
 * @author Miguel, miguel@jmol.org and Bob Hanson, hansonr@stolaf.edu
 */

/* rewritten by Bob 7/2006 to fully implement the capabilities of the
 * Cohen-Sutherland algorithm.
 * 
 * added line bitset option for rockets. Rendering times for bonds done this way are a bit slower.
 */

import java.util.Hashtable;
import java.util.Map;

import javajs.util.P3;
import javajs.util.P3i;

import javajs.util.BS;
import org.jmol.util.GData;
import org.jmol.util.Shader;

final class LineRenderer extends PrecisionRenderer {

  private final Graphics3D g3d;
  private final Shader shader;

  LineRenderer(Graphics3D g3d) {
    this.g3d = g3d;
    shader = g3d.shader;
  }

  private BS lineBits;
  private float slope;
  private boolean lineTypeX;
  private int nBits;
  //  private int nCached = 0;
  //  private int nFound = 0;
  //int test = 5;
  private Map<Float, BS> lineCache = new Hashtable<Float, BS>();
  private Float slopeKey;

  void setLineBits(float dx, float dy) {
    // from cylinder
    slope = (dx != 0 ? dy / dx : dy >= 0 ? Float.MAX_VALUE : -Float.MAX_VALUE);
    lineTypeX = (slope <= 1 && slope >= -1);
    nBits = (lineTypeX ? g3d.width : g3d.height);

    // get cached line bits or create new ones

    slopeKey = Float.valueOf(slope);
    if (lineCache.containsKey(slopeKey)) {
      lineBits = lineCache.get(slopeKey);
      //    if (Logger.debugging) {
      //      nFound++;
      //      if (nFound == 1000000)
      //        Logger.debug("nCached/nFound lines: " + nCached + " " + nFound);
      //    }
      return;
    }
    lineBits = BS.newN(nBits);
    dy = Math.abs(dy);
    dx = Math.abs(dx);
    if (dy > dx) {
      float t = dx;
      dx = dy;
      dy = t;
    }
    int twoDError = 0;
    float twoDx = dx + dx, twoDy = dy + dy;
    for (int i = 0; i < nBits; i++) {
      twoDError += twoDy;
      if (twoDError > dx) {
        lineBits.set(i);
        twoDError -= twoDx;
      }
    }
    lineCache.put(slopeKey, lineBits);
  }

  void clearLineCache() {
    lineCache.clear();
    //nCached = 0;
  }

  void plotLineOld(int argbA, int argbB, int xA, int yA, int zA, int xB,
                   int yB, int zB) {
    // primary method for mesh triangle, quadrilateral, hermite, backbone,
    // sticks, and stars
    x1t = xA;
    x2t = xB;
    y1t = yA;
    y2t = yB;
    z1t = zA;
    z2t = zB;
    boolean clipped = true;
    switch (getTrimmedLineImpl()) {
    case VISIBILITY_UNCLIPPED:
      clipped = false;
      break;
    case VISIBILITY_OFFSCREEN:
      return;
    }
    plotLineClippedOld(argbA, argbB, xA, yA, zA, xB - xA, yB - yA, zB - zA,
        clipped, 0, 0);
  }

  /**
   * low-resolution linear z
   * 
   * @param argbA
   * @param argbB
   * @param xA
   * @param yA
   * @param zA
   * @param dxBA
   * @param dyBA
   * @param dzBA
   * @param clipped
   */
  void plotLineDeltaOld(int argbA, int argbB, int xA, int yA, int zA, int dxBA,
                        int dyBA, int dzBA, boolean clipped) {
    // from cylinder -- endcaps open or flat, diameter 1, cone
    // cartoon rockets, low-precision z-buffer
    x1t = xA;
    x2t = xA + dxBA;
    y1t = yA;
    y2t = yA + dyBA;
    z1t = zA;
    z2t = zA + dzBA;
    if (clipped)
      switch (getTrimmedLineImpl()) {
      case VISIBILITY_OFFSCREEN:
        return;
      case VISIBILITY_UNCLIPPED:
        clipped = false;
        break;
      }
    plotLineClippedOld(argbA, argbB, xA, yA, zA, dxBA, dyBA, dzBA, clipped, 0,
        0);
  }

  /**
   * low-precision old-style linear z, even in perspective mode
   * 
   * @param shades1
   * @param shades2
   * @param screenMask
   * @param shadeIndex
   * @param x
   * @param y
   * @param z
   * @param dx
   * @param dy
   * @param dz
   * @param clipped
   */
  void plotLineDeltaAOld(int[] shades1, int[] shades2, int screenMask,
                         int shadeIndex, int x, int y, int z, int dx, int dy,
                         int dz, boolean clipped) {
    // from cylinder -- standard bond with two colors or cone with one color
    x1t = x;
    x2t = x + dx;
    y1t = y;
    y2t = y + dy;
    z1t = z;
    z2t = z + dz;
    if (clipped)
      switch (getTrimmedLineImpl()) {
      case VISIBILITY_OFFSCREEN:
        return;
      case VISIBILITY_UNCLIPPED:
        clipped = false;
      }
    // special shading for bonds
    int[] zbuf = g3d.zbuf;
    int width = g3d.width;
    int runIndex = 0;
    int rise = Integer.MAX_VALUE;
    int run = 1;
    int offset = y * width + x;
    int offsetMax = g3d.bufferSize;
    int shadeIndexUp = (shadeIndex < Shader.SHADE_INDEX_LAST ? shadeIndex + 1
        : shadeIndex);
    int shadeIndexDn = (shadeIndex > 0 ? shadeIndex - 1 : shadeIndex);
    int argb1 = shades1[shadeIndex];
    int argb1Up = shades1[shadeIndexUp];
    int argb1Dn = shades1[shadeIndexDn];
    int argb2 = shades2[shadeIndex];
    int argb2Up = shades2[shadeIndexUp];
    int argb2Dn = shades2[shadeIndexDn];
    int argb = argb1;
    Pixelator p = g3d.pixel;
    if (screenMask != 0) {
      p = g3d.setScreened((screenMask & 1) == 1);
      g3d.currentShadeIndex = 0;
    }
    if (argb != 0 && !clipped && offset >= 0 && offset < offsetMax
        && z < zbuf[offset])
      p.addPixel(offset, z, argb);
    if (dx == 0 && dy == 0)
      return;
    int xIncrement = 1;
    int yOffsetIncrement = width;
    int x2 = x + dx;
    int y2 = y + dy;

    if (dx < 0) {
      dx = -dx;
      xIncrement = -1;
    }
    if (dy < 0) {
      dy = -dy;
      yOffsetIncrement = -width;
    }
    int twoDx = dx + dx, twoDy = dy + dy;

    // the z dimension and the z increment are stored with a fractional
    // component in the bottom 10 bits.
    int zCurrentScaled = z << 10;
    int argbUp = argb1Up;
    int argbDn = argb1Dn;
    if (dy <= dx) {
      int roundingFactor = dx - 1;
      if (dz < 0)
        roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dx;
      int twoDxAccumulatedYError = 0;
      int n1 = Math.abs(x2 - x2t) - 1;
      int n2 = Math.abs(x2 - x1t) - 1;
      for (int n = dx - 1, nMid = n / 2; --n >= n1;) {
        if (n == nMid) {
          argb = argb2;
          if (argb == 0)
            return;
          argbUp = argb2Up;
          argbDn = argb2Dn;
          if (screenMask % 3 != 0) {
            p = g3d.setScreened((screenMask & 2) == 2);
            g3d.currentShadeIndex = 0;
          }
        }
        offset += xIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDxAccumulatedYError += twoDy;
        if (twoDxAccumulatedYError > dx) {
          offset += yOffsetIncrement;
          twoDxAccumulatedYError -= twoDx;
        }
        if (argb != 0 && n < n2 && offset >= 0 && offset < offsetMax
            && runIndex < rise) {
          int zCurrent = zCurrentScaled >> 10;
          if (zCurrent < zbuf[offset]) {
            int rand8 = shader.nextRandom8Bit();
            p.addPixel(offset, zCurrent, rand8 < 85 ? argbDn
                : (rand8 > 170 ? argbUp : argb));
          }
        }
        runIndex = (runIndex + 1) % run;
      }
    } else {
      int roundingFactor = dy - 1;
      if (dz < 0)
        roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dy;
      int twoDyAccumulatedXError = 0;
      int n1 = Math.abs(y2 - y2t) - 1;// + 1;
      int n2 = Math.abs(y2 - y1t) - 1;// - 1;
      for (int n = dy - 1, nMid = n / 2; --n >= n1;) {
        if (n == nMid) {
          argb = argb2;
          if (argb == 0)
            return;
          argbUp = argb2Up;
          argbDn = argb2Dn;
          if (screenMask % 3 != 0) {
            p = g3d.setScreened((screenMask & 2) == 2);
            g3d.currentShadeIndex = 0;
          }
        }
        offset += yOffsetIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDyAccumulatedXError += twoDx;
        if (twoDyAccumulatedXError > dy) {
          offset += xIncrement;
          twoDyAccumulatedXError -= twoDy;
        }
        if (argb != 0 && n < n2 && offset >= 0 && offset < offsetMax
            && runIndex < rise) {
          int zCurrent = zCurrentScaled >> 10;
          if (zCurrent < zbuf[offset]) {
            int rand8 = g3d.shader.nextRandom8Bit();
            p.addPixel(offset, zCurrent, rand8 < 85 ? argbDn
                : (rand8 > 170 ? argbUp : argb));
          }
        }
        runIndex = (runIndex + 1) % run;
      }
    }
  }

  void plotLineDeltaABitsFloat(int[] shades1, int[] shades2, int shadeIndex, P3 ptA,
                          P3 ptB, int screenMask, boolean clipped) {
    // from cylinder -- cartoonRockets - somewhat higher precision, because
    // particularly for draw, we can have very short distances.
    int x = Math.round(ptA.x);
    int y = Math.round(ptA.y);
    int z = Math.round(ptA.z);
    int bx = Math.round(ptB.x);
    int by = Math.round(ptB.y);
    int bz = Math.round(ptB.z);
    int dx = bx - x;
    int dy = by - y;

    x1t = x;
    x2t = bx;
    y1t = y;
    y2t = by;
    z1t = z;
    z2t = bz;
    if (clipped && getTrimmedLineImpl() == VISIBILITY_OFFSCREEN)
      return;
    // special shading for rockets; somewhat slower than above;
    int[] zbuf = g3d.zbuf;
    int width = g3d.width;
    int runIndex = 0;
    int rise = Integer.MAX_VALUE;
    int run = 1;
    int shadeIndexUp = (shadeIndex < Shader.SHADE_INDEX_LAST ? shadeIndex + 1
        : shadeIndex);
    int shadeIndexDn = (shadeIndex > 0 ? shadeIndex - 1 : shadeIndex);
    int argb1 = shades1[shadeIndex];
    int argb1Up = shades1[shadeIndexUp];
    int argb1Dn = shades1[shadeIndexDn];
    int argb2 = shades2[shadeIndex];
    int argb2Up = shades2[shadeIndexUp];
    int argb2Dn = shades2[shadeIndexDn];
    int offset = y * width + x;
    int offsetMax = g3d.bufferSize;
    int i0, iMid, i1, i2, iIncrement, xIncrement, yOffsetIncrement;
    if (lineTypeX) {
      i0 = x;
      i1 = x1t;
      i2 = x2t;
      iMid = x + dx / 2;
      iIncrement = (dx >= 0 ? 1 : -1);
      xIncrement = iIncrement;
      yOffsetIncrement = (dy >= 0 ? width : -width);
      setRastABFloat(ptA.x, ptA.z, ptB.x, ptB.z);
    } else {
      i0 = y;
      i1 = y1t;
      i2 = y2t;
      iMid = y + dy / 2;
      iIncrement = (dy >= 0 ? 1 : -1);
      xIncrement = (dy >= 0 ? width : -width);
      yOffsetIncrement = (dx >= 0 ? 1 : -1);
      setRastABFloat(ptA.y, ptA.z, ptB.y, ptB.z);
    }
    float zCurrent = z;
    int argb = argb1;
    int argbUp = argb1Up;
    int argbDn = argb1Dn;
    boolean isInWindow = false;
    Pixelator p = g3d.pixel;
    if (screenMask != 0) {
      p = g3d.setScreened((screenMask & 1) == 1);
      g3d.currentShadeIndex = 0;
    }
    // "x" is not necessarily the x-axis.

    //  x----x1t-----------x2t---x2
    //  ------------xMid-----------
    //0-|------------------>-----------w

    // or

    //  x2---x2t-----------x1t----x
    //  ------------xMid-----------    
    //0-------<-------------------|----w

    for (int i = i0, iBits = i0;; i += iIncrement, iBits += iIncrement) {
      if (i == i1)
        isInWindow = true;
      if (i == iMid) {
        argb = argb2;
        if (argb == 0)
          return;
        argbUp = argb2Up;
        argbDn = argb2Dn;
        if (screenMask % 3 != 0) {
          p = g3d.setScreened((screenMask & 2) == 2);
          g3d.currentShadeIndex = 0;
        }
      }
      if (argb != 0 && isInWindow && offset >= 0 && offset < offsetMax
          && runIndex < rise) {
        zCurrent = getZCurrent(a, b, i);
        if (zCurrent < zbuf[offset]) {
          int rand8 = shader.nextRandom8Bit();
          p.addPixel(offset, (int) zCurrent, rand8 < 85 ? argbDn
              : (rand8 > 170 ? argbUp : argb));
        }
      }
      if (i == i2)
        break;
      runIndex = (runIndex + 1) % run;
      offset += xIncrement;
      while (iBits < 0)
        iBits += nBits;
      if (lineBits.get(iBits % nBits)) {
        offset += yOffsetIncrement;
      }
    }
  }

  void plotLineDeltaABitsInt(int[] shades1, int[] shades2, int shadeIndex, P3i ptA,
                          P3i ptB, int screenMask, boolean clipped) {
    // from cylinder -- cartoonRockets
    int x = ptA.x;
    int y = ptA.y;
    int z = ptA.z;
    int bx = ptB.x;
    int by = ptB.y;
    int bz = ptB.z;
    int dx = bx - x;
    int dy = by - y;

    x1t = x;
    x2t = bx;
    y1t = y;
    y2t = by;
    z1t = z;
    z2t = bz;
    if (clipped && getTrimmedLineImpl() == VISIBILITY_OFFSCREEN)
      return;
    // special shading for rockets; somewhat slower than above;
    int[] zbuf = g3d.zbuf;
    int width = g3d.width;
    int runIndex = 0;
    int rise = Integer.MAX_VALUE;
    int run = 1;
    int shadeIndexUp = (shadeIndex < Shader.SHADE_INDEX_LAST ? shadeIndex + 1
        : shadeIndex);
    int shadeIndexDn = (shadeIndex > 0 ? shadeIndex - 1 : shadeIndex);
    int argb1 = shades1[shadeIndex];
    int argb1Up = shades1[shadeIndexUp];
    int argb1Dn = shades1[shadeIndexDn];
    int argb2 = shades2[shadeIndex];
    int argb2Up = shades2[shadeIndexUp];
    int argb2Dn = shades2[shadeIndexDn];
    int offset = y * width + x;
    int offsetMax = g3d.bufferSize;
    int i0, iMid, i1, i2, iIncrement, xIncrement, yOffsetIncrement;
    if (lineTypeX) {
      i0 = x;
      i1 = x1t;
      i2 = x2t;
      iMid = x + dx / 2;
      iIncrement = (dx >= 0 ? 1 : -1);
      xIncrement = iIncrement;
      yOffsetIncrement = (dy >= 0 ? width : -width);
      setRastAB(ptA.x, ptA.z, ptB.x, ptB.z);
    } else {
      i0 = y;
      i1 = y1t;
      i2 = y2t;
      iMid = y + dy / 2;
      iIncrement = (dy >= 0 ? 1 : -1);
      xIncrement = (dy >= 0 ? width : -width);
      yOffsetIncrement = (dx >= 0 ? 1 : -1);
      setRastAB(ptA.y, ptA.z, ptB.y, ptB.z);
    }
    float zCurrent = z;
    int argb = argb1;
    int argbUp = argb1Up;
    int argbDn = argb1Dn;
    boolean isInWindow = false;
    Pixelator p = g3d.pixel;
    if (screenMask != 0) {
      p = g3d.setScreened((screenMask & 1) == 1);
      g3d.currentShadeIndex = 0;
    }
    // "x" is not necessarily the x-axis.

    //  x----x1t-----------x2t---x2
    //  ------------xMid-----------
    //0-|------------------>-----------w

    // or

    //  x2---x2t-----------x1t----x
    //  ------------xMid-----------    
    //0-------<-------------------|----w

    for (int i = i0, iBits = i0;; i += iIncrement, iBits += iIncrement) {
      if (i == i1)
        isInWindow = true;
      if (i == iMid) {
        argb = argb2;
        if (argb == 0)
          return;
        argbUp = argb2Up;
        argbDn = argb2Dn;
        if (screenMask % 3 != 0) {
          p = g3d.setScreened((screenMask & 2) == 2);
          g3d.currentShadeIndex = 0;
        }
      }
      if (argb != 0 && isInWindow && offset >= 0 && offset < offsetMax
          && runIndex < rise) {
        zCurrent = getZCurrent(a, b, i);
        if (zCurrent < zbuf[offset]) {
          int rand8 = shader.nextRandom8Bit();
          p.addPixel(offset, (int) zCurrent, rand8 < 85 ? argbDn
              : (rand8 > 170 ? argbUp : argb));
        }
      }
      if (i == i2)
        break;
      runIndex = (runIndex + 1) % run;
      offset += xIncrement;
      while (iBits < 0)
        iBits += nBits;
      if (lineBits.get(iBits % nBits)) {
        offset += yOffsetIncrement;
      }
    }
  }

  void plotLineBits(int argbA, int argbB, P3i ptA, P3i ptB, int run, int rise, boolean andClip) {
    // standard, dashed or not dashed
    // measures, axes, bbcage, mesh, cylinders 
    if (ptA.z <= 1 || ptB.z <= 1)
      return;
    boolean zclipped = true;
    x1t = ptA.x;
    y1t = ptA.y;
    z1t = ptA.z;
    x2t = ptB.x;
    y2t = ptB.y;
    z2t = ptB.z;
    switch (getTrimmedLineImpl()) {
    case VISIBILITY_OFFSCREEN:
      return;
    case VISIBILITY_UNCLIPPED:
      zclipped = false;
      break;
    default:
      if (andClip) {
        ptA.set(x1t, y1t, z1t);
        ptB.set(x2t, y2t, z2t);
      }
    }
    int[] zbuf = g3d.zbuf;
    int width = g3d.width;
    int runIndex = 0;
    if (run == 0) {
      rise = Integer.MAX_VALUE;
      run = 1;
    }
    int x = ptA.x;
    int y = ptA.y;
    int z = ptA.z;
    int dx = ptB.x - x;
    int x2 = x + dx;
    int dy = ptB.y - y;
    int y2 = y + dy;
    int offset = y * width + x;
    int offsetMax = g3d.bufferSize;
    //boolean flipflop = (((x ^ y) & 1) != 0);
    //boolean tScreened = tScreened1;
    int argb = argbA;
    Pixelator p = g3d.pixel;
    if (argb != 0 && !zclipped && offset >= 0 && offset < offsetMax
        && z < zbuf[offset])
      p.addPixel(offset, z, argb);

    if (dx == 0 && dy == 0)
      return;
    int xIncrement = 1, yIncrement = 1;
    int yOffsetIncrement = width;

    if (dx < 0) {
      dx = -dx;
      xIncrement = -1;
    }
    if (dy < 0) {
      dy = -dy;
      yOffsetIncrement = -width;
      yIncrement = -1;
    }
    int twoDx = dx + dx, twoDy = dy + dy;

    // the z dimension and the z increment are stored with a fractional
    // component in the bottom 10 bits.
    if (dy <= dx) {
      // using x
      setRastAB(ptA.x, ptA.z, ptB.x, ptB.z);
      int twoDxAccumulatedYError = 0;
      int n1 = Math.abs(x2 - x2t) - 1;
      int n2 = Math.abs(x2 - x1t) - 1;
      for (int n = dx - 1, nMid = n / 2; --n >= n1;) {
        if (n == nMid) {
          argb = argbB;
          if (argb == 0)
            return;
        }
        offset += xIncrement;
        x += xIncrement;
        twoDxAccumulatedYError += twoDy;
        if (twoDxAccumulatedYError > dx) {
          offset += yOffsetIncrement;
          twoDxAccumulatedYError -= twoDx;
        }
        if (argb != 0 && n < n2 && offset >= 0 && offset < offsetMax
            && runIndex < rise) {
          int zCurrent = getZCurrent(a, b, x);
          if (zCurrent < zbuf[offset])
            p.addPixel(offset, zCurrent, argb);
        }
        runIndex = (runIndex + 1) % run;
      }
    } else {
      // using y
      setRastAB(ptA.y, ptA.z, ptB.y, ptB.z);
      int twoDyAccumulatedXError = 0;
      int n1 = Math.abs(y2 - y2t) - 1;
      int n2 = Math.abs(y2 - y1t) - 1;
      for (int n = dy - 1, nMid = n / 2; --n >= n1;) {
        if (n == nMid) {
          argb = argbB;
          if (argb == 0)
            return;
        }
        offset += yOffsetIncrement;
        y += yIncrement;
        twoDyAccumulatedXError += twoDx;
        if (twoDyAccumulatedXError > dy) {
          offset += xIncrement;
          twoDyAccumulatedXError -= twoDy;
        }
        if (argb != 0 && n < n2 && offset >= 0 && offset < offsetMax
            && runIndex < rise) {
          int zCurrent = getZCurrent(a, b, y);
          if (zCurrent < zbuf[offset])
            p.addPixel(offset, zCurrent, argb);
        }
        runIndex = (runIndex + 1) % run;
      }
    }
  }

  private final static int VISIBILITY_UNCLIPPED = 0;
  private final static int VISIBILITY_CLIPPED = 1;
  private final static int VISIBILITY_OFFSCREEN = 2;

  private int x1t, y1t, z1t, x2t, y2t, z2t; // trimmed

  /**
   * <p>
   * Cohen-Sutherland line clipping used to check visibility.
   * </p>
   * <p>
   * Note that this routine is only used for visibility checking. To avoid
   * integer rounding errors which cause cracking to occur in 'solid' surfaces,
   * the lines are actually drawn from their original end-points.
   * 
   * The nuance is that this algorithm doesn't just deliver a boolean. It
   * delivers the trimmed line. Although we need to start the raster loop at the
   * origin for good surfaces, we can save lots of time by saving the known
   * endpoints as globals variables. -- Bob Hanson 7/06
   * </p>
   * 
   * @return Visibility (see VISIBILITY_... constants);
   */

  private int getTrimmedLineImpl() { // formerly "visibilityCheck()"

    int cc1 = g3d.clipCode3(x1t, y1t, z1t);
    int cc2 = g3d.clipCode3(x2t, y2t, z2t);
    int c = (cc1 | cc2);
    if ((cc1 | cc2) == 0)
      return VISIBILITY_UNCLIPPED;
    if (c == -1)
      return VISIBILITY_OFFSCREEN;
    int xLast = g3d.xLast;
    int yLast = g3d.yLast;
    int slab = g3d.slab;
    int depth = g3d.depth;
    do {
      if ((cc1 & cc2) != 0)
        return VISIBILITY_OFFSCREEN;

      float dx = x2t - x1t;
      float dy = y2t - y1t;
      float dz = z2t - z1t;
      if (cc1 != 0) { //cohen-sutherland line clipping
        if ((cc1 & GData.xLT) != 0) {
          y1t += (int) ((-x1t * dy) / dx);
          z1t += (int) ((-x1t * dz) / dx);
          x1t = 0;
        } else if ((cc1 & GData.xGT) != 0) {
          y1t += (int) (((xLast - x1t) * dy) / dx);
          z1t += (int) (((xLast - x1t) * dz) / dx);
          x1t = xLast;
        } else if ((cc1 & GData.yLT) != 0) {
          x1t += (int) ((-y1t * dx) / dy);
          z1t += (int) ((-y1t * dz) / dy);
          y1t = 0;
        } else if ((cc1 & GData.yGT) != 0) {
          x1t += (int) (((yLast - y1t) * dx) / dy);
          z1t += (int) (((yLast - y1t) * dz) / dy);
          y1t = yLast;
        } else if ((cc1 & GData.zLT) != 0) {
          x1t += (int) (((slab - z1t) * dx) / dz);
          y1t += (int) (((slab - z1t) * dy) / dz);
          z1t = slab;
        } else // must be zGT
        {
          x1t += (int) (((depth - z1t) * dx) / dz);
          y1t += (int) (((depth - z1t) * dy) / dz);
          z1t = depth;
        }

        cc1 = g3d.clipCode3(x1t, y1t, z1t);
      } else {
        if ((cc2 & GData.xLT) != 0) {
          y2t += (int) ((-x2t * dy) / dx);
          z2t += (int) ((-x2t * dz) / dx);
          x2t = 0;
        } else if ((cc2 & GData.xGT) != 0) {
          y2t += (int) (((xLast - x2t) * dy) / dx);
          z2t += (int) (((xLast - x2t) * dz) / dx);
          x2t = xLast;
        } else if ((cc2 & GData.yLT) != 0) {
          x2t += (int) ((-y2t * dx) / dy);
          z2t += (int) ((-y2t * dz) / dy);
          y2t = 0;
        } else if ((cc2 & GData.yGT) != 0) {
          x2t += (int) (((yLast - y2t) * dx) / dy);
          z2t += (int) (((yLast - y2t) * dz) / dy);
          y2t = yLast;
        } else if ((cc2 & GData.zLT) != 0) {
          x2t += (int) (((slab - z2t) * dx) / dz);
          y2t += (int) (((slab - z2t) * dy) / dz);
          z2t = slab;
        } else // must be zGT
        {
          x2t += (int) (((depth - z2t) * dx) / dz);
          y2t += (int) (((depth - z2t) * dy) / dz);
          z2t = depth;
        }
        cc2 = g3d.clipCode3(x2t, y2t, z2t);
      }
    } while ((cc1 | cc2) != 0);
    return VISIBILITY_CLIPPED;
  }

  /**
   * low-resolution linear z-buffer (old style)
   * 
   * @param argb1
   * @param argb2
   * @param x
   * @param y
   * @param z
   * @param dx
   * @param dy
   * @param dz
   * @param clipped
   * @param run
   * @param rise
   */
  private void plotLineClippedOld(int argb1, int argb2, int x, int y, int z,
                                  int dx, int dy, int dz, boolean clipped,
                                  int run, int rise) {
    // standard, dashed or not dashed -- isosurface mesh
    int[] zbuf = g3d.zbuf;
    int width = g3d.width;
    int runIndex = 0;
    if (run == 0) {
      rise = Integer.MAX_VALUE;
      run = 1;
    }
    int offset = y * width + x;
    int offsetMax = g3d.bufferSize;
    //boolean flipflop = (((x ^ y) & 1) != 0);
    //boolean tScreened = tScreened1;
    int argb = argb1;
    Pixelator p = g3d.pixel;
    if (argb != 0 && !clipped && offset >= 0 && offset < offsetMax
        && z < zbuf[offset])
      p.addPixel(offset, z, argb);
    if (dx == 0 && dy == 0)
      return;
    int xIncrement = 1;
    int yOffsetIncrement = width;

    int x2 = x + dx;
    int y2 = y + dy;

    if (dx < 0) {
      dx = -dx;
      xIncrement = -1;
    }
    if (dy < 0) {
      dy = -dy;
      yOffsetIncrement = -width;
    }
    int twoDx = dx + dx, twoDy = dy + dy;

    // the z dimension and the z increment are stored with a fractional
    // component in the bottom 10 bits.
    int zCurrentScaled = z << 10;
    if (dy <= dx) {
      int roundingFactor = dx - 1;
      if (dz < 0)
        roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dx;
      int twoDxAccumulatedYError = 0;
      int n1 = Math.abs(x2 - x2t) - 1;
      int n2 = Math.abs(x2 - x1t) - 1;
      for (int n = dx - 1, nMid = n / 2; --n >= n1;) {
        if (n == nMid) {

          //          tScreened = tScreened2;

          argb = argb2;
          if (argb == 0)
            return;
        }
        offset += xIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDxAccumulatedYError += twoDy;
        if (twoDxAccumulatedYError > dx) {
          offset += yOffsetIncrement;
          twoDxAccumulatedYError -= twoDx;
          //          flipflop = !flipflop;
        }
        if (argb != 0 && n < n2 && offset >= 0 && offset < offsetMax
            && runIndex < rise) {
          int zCurrent = zCurrentScaled >> 10;
          if (zCurrent < zbuf[offset])
            p.addPixel(offset, zCurrent, argb);
        }
        runIndex = (runIndex + 1) % run;
      }
    } else {
      int roundingFactor = dy - 1;
      if (dz < 0)
        roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dy;
      int twoDyAccumulatedXError = 0;
      int n1 = Math.abs(y2 - y2t) - 1;
      int n2 = Math.abs(y2 - y1t) - 1;
      for (int n = dy - 1, nMid = n / 2; --n >= n1;) {
        if (n == nMid) {
          //          tScreened = tScreened2;
          argb = argb2;
          if (argb == 0)
            return;
        }
        offset += yOffsetIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDyAccumulatedXError += twoDx;
        if (twoDyAccumulatedXError > dy) {
          offset += xIncrement;
          twoDyAccumulatedXError -= twoDy;
        }
        if (argb != 0 && n < n2 && offset >= 0 && offset < offsetMax
            && runIndex < rise) {
          int zCurrent = zCurrentScaled >> 10;
          if (zCurrent < zbuf[offset])
            p.addPixel(offset, zCurrent, argb);
        }
        runIndex = (runIndex + 1) % run;
      }
    }
  }

}
