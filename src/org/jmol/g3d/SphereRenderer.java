/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2015-06-06 17:41:37 -0500 (Sat, 06 Jun 2015) $
 * $Revision: 20555 $
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

//import javax.vecmath.Vector4f;  !NO -- requires Vector4d in applet

import javajs.util.M3;
import javajs.util.M4;
import javajs.util.P3;

import org.jmol.util.Shader;



/**
 *<p>
 * Implements high performance rendering of shaded spheres.
 *</p>
 *<p>
 * Drawing spheres quickly is critically important to Jmol.
 * These routines implement high performance rendering of
 * spheres in 3D.
 *</p>
 *<p>
 * If you can think of a faster way to implement this, please
 * let us know.
 *</p>
 *<p>
 * There is a lot of bit-twiddling going on here, which may
 * make the code difficult to understand for non-systems programmers.
 *</p>
 * Ellipsoid code added 4/2008 -- Bob Hanson hansonr@stolaf.edu
 *
 * @author Miguel, miguel@jmol.org
 */
public class SphereRenderer {

  private final Graphics3D g3d;
  private final Shader shader;
  
  SphereRenderer(Graphics3D g3d) {
    this.g3d = g3d;
    shader = g3d.shader;
  }

  private final static int maxOddSizeSphere = 49;
  final static int maxSphereDiameter = 1000;
  final static int maxSphereDiameter2 = maxSphereDiameter * 2;
  private double[] zroot = new double[2];
  private M3 mat;
  private double[] coef;
  private M4 mDeriv;
  private int selectedOctant;
  private int planeShade;
  private int[] zbuf;
  private int width;
  private int height;
  private int depth;
  private int slab;
  private int offsetPbufBeginLine;

  void render(int[] shades, int diameter, int x, int y,
              int z, M3 mat, double[] coef, M4 mDeriv,
              int selectedOctant, P3[] octantPoints) {
    //System.out.println("sphere " + x  + " " + y  + " " + z + " " + diameter);
    if (z == 1)
      return;
    if (diameter > maxOddSizeSphere)
      diameter &= ~1;
    if (g3d.isClippedXY(diameter, x, y))
      return;
    slab = g3d.slab;
    depth = g3d.depth;
    int radius = (diameter + 1) >> 1;
    int minZ = z - radius;
    if (z + radius < slab || minZ > depth)
      return;
    int minX = x - radius;
    int maxX = x + radius;
    int minY = y - radius;
    int maxY = y + radius;    
    shader.nOut = shader.nIn = 0;
    zbuf = g3d.zbuf;
    height = g3d.height;
    width = g3d.width;
    offsetPbufBeginLine = width * y + x;
    Shader sh = shader;
    this.mat = mat;
    if (mat != null) {
      this.coef = coef;
      this.mDeriv = mDeriv;
      this.selectedOctant = selectedOctant;
      if (sh.ellipsoidShades == null)
        sh.createEllipsoidShades();
      if (octantPoints != null) {
        planeShade = -1;
        for (int i = 0; i < 3; i ++) {
          float dx = dxyz[i][0] = octantPoints[i].x - x;
          float dy = dxyz[i][1] = octantPoints[i].y - y;
          float dz = dxyz[i][2] = octantPoints[i].z - z;
          planeShades[i] = sh.getShadeIndex(dx, dy, -dz);
          if (dx == 0 && dy == 0) {
            planeShade = planeShades[i];
            break;
          }
        }
      }
    }
    
    if (mat != null || diameter > Shader.maxSphereCache) {
      renderQuadrant(-1, -1, x, y, z, diameter, shades);
      renderQuadrant(-1, 1, x, y, z, diameter, shades);
      renderQuadrant(1, -1, x, y, z, diameter, shades);
      renderQuadrant(1, 1, x, y, z, diameter, shades);
      if (mat != null) {
        // clear;
        this.mat = null;
        this.coef = null;
        this.mDeriv = null;
      }
    } else {
      int[] ss = sh.sphereShapeCache[diameter - 1];
      if (ss == null) {
        int countSE = 0;
        boolean d = (diameter & 1) != 0;
        float radiusF = diameter / 2.0f;
        float radiusF2 = radiusF * radiusF;
        radius = (diameter + 1) / 2;
        float ys = d ? 0 : 0.5f;
        for (int i = 0; i < radius; ++i, ++ys) {
          float y2 = ys * ys;
          float xs = d ? 0 : 0.5f;
          for (int j = 0; j < radius; ++j, ++xs) {
            float x2 = xs * xs;
            float z2 = radiusF2 - y2 - x2;
            if (z2 >= 0)
              ++countSE;
          }
        }        
        ss = new int[countSE];
        int offset = 0;
        ys = d ? 0 : 0.5f;
        for (int i = 0; i < radius; ++i, ++ys) {
          float y2 = ys * ys;
          float xs = d ? 0 : 0.5f;
          for (int j = 0; j < radius; ++j, ++xs) {
            float x2 = xs * xs;
            float z2 = radiusF2 - y2 - x2;
            if (z2 >= 0) {
              float zs = (float)Math.sqrt(z2);
              int height = (int)zs;
              int shadeIndexSE = sh.getShadeN( xs,  ys, zs, radiusF);
              int shadeIndexSW = sh.getShadeN(-xs,  ys, zs, radiusF);
              int shadeIndexNE = sh.getShadeN( xs, -ys, zs, radiusF);
              int shadeIndexNW = sh.getShadeN(-xs, -ys, zs, radiusF);
              int packed = (height |
                            (shadeIndexSE << 7) |
                            (shadeIndexSW << 13) |
                            (shadeIndexNE << 19) |
                            (shadeIndexNW << 25));
              ss[offset++] = packed;
            }
          }
          ss[offset - 1] |= 0x80000000;
        }
        sh.sphereShapeCache[diameter - 1] = ss;
      }
      if (minX < 0 || maxX >= width || minY < 0 || maxY >= height
          || minZ < slab || z > depth)
        renderSphereClipped(ss, x, y, z, diameter, shades);
      else
        renderSphereUnclipped(ss, z, diameter, shades);
    }
    zbuf = null;
  } 
  
  private void renderSphereUnclipped(int[] sphereShape, int z, int diameter, int[] shades) {
    int offsetSphere = 0;
    int evenSizeCorrection = 1 - (diameter & 1);
    int offsetSouthCenter = offsetPbufBeginLine;
    int offsetNorthCenter = offsetSouthCenter - evenSizeCorrection * width;
    int nLines = (diameter + 1) / 2;
    int[] zbuf = this.zbuf;
    int width = this.width;
    Pixelator p = g3d.pixel;
      do {
        int offsetSE = offsetSouthCenter;
        int offsetSW = offsetSouthCenter - evenSizeCorrection;
        int offsetNE = offsetNorthCenter;
        int offsetNW = offsetNorthCenter - evenSizeCorrection;
        int packed;
        do {
          packed = sphereShape[offsetSphere++];
          int zPixel = z - (packed & 0x7F);
          if (zPixel < zbuf[offsetSE])
            p.addPixel(offsetSE, zPixel,
                shades[((packed >> 7) & 0x3F)]);
          if (zPixel < zbuf[offsetSW])
            p.addPixel(offsetSW, zPixel,
                shades[((packed >> 13) & 0x3F)]);
          if (zPixel < zbuf[offsetNE])
            p.addPixel(offsetNE, zPixel,
                shades[((packed >> 19) & 0x3F)]);
          if (zPixel < zbuf[offsetNW])
            p.addPixel(offsetNW, zPixel,
                shades[((packed >> 25) & 0x3F)]);
          ++offsetSE;
          --offsetSW;
          ++offsetNE;
          --offsetNW;
        } while (packed >= 0);
        offsetSouthCenter += width;
        offsetNorthCenter -= width;
      } while (--nLines > 0);
  }

  private final static int SHADE_SLAB_CLIPPED = Shader.SHADE_INDEX_NORMAL - 5;

  private void renderSphereClipped(int[] sphereShape, int x, int y, int z, int diameter, int[] shades) {
    int w = width;
    int h = height;
    int offsetSphere = 0;
    int evenSizeCorrection = 1 - (diameter & 1);
    int offsetSouthCenter = offsetPbufBeginLine;
    int offsetNorthCenter = offsetSouthCenter - evenSizeCorrection * w;
    int nLines = (diameter + 1) / 2;
    int ySouth = y;
    int yNorth = y - evenSizeCorrection;
    int randu = (x << 16) + (y << 1) ^ 0x33333333;
    int[] sh = shades;
    int[] zb = zbuf;
    Pixelator p = g3d.pixel;
    int sl = slab;
    int de = depth;
    
    do {
      boolean tSouthVisible = ySouth >= 0 && ySouth < h;
      boolean tNorthVisible = yNorth >= 0 && yNorth < h;
      int offsetSE = offsetSouthCenter;
      int offsetSW = offsetSouthCenter - evenSizeCorrection;
      int offsetNE = offsetNorthCenter;
      int offsetNW = offsetNorthCenter - evenSizeCorrection;
      int packed;
      int xEast = x;
      int xWest = x - evenSizeCorrection;
      do {
        boolean tWestVisible = xWest >= 0 && xWest < w;
        boolean tEastVisible = xEast >= 0 && xEast < w;
        packed = sphereShape[offsetSphere++];
        boolean isCore;
        int zOffset = packed & 0x7F;
        int zPixel;
        if (z < sl) {
          // center in front of plane -- possibly show back half
          zPixel = z + zOffset;
          isCore = (zPixel >= sl);
        } else {
          // center is behind, show front, possibly as solid core
          zPixel = z - zOffset;
          isCore = (zPixel < sl);
        }
        if (isCore)
          zPixel = sl;
        if (zPixel >= sl && zPixel <= de) {
          if (tSouthVisible) {
            if (tEastVisible
                && zPixel < zb[offsetSE]) {
              int i = (isCore ? SHADE_SLAB_CLIPPED - 3 + ((randu >> 7) & 0x07)
                  : (packed >> 7) & 0x3F);
              p.addPixel(offsetSE, zPixel, sh[i]);
            }
            if (tWestVisible
                && zPixel < zb[offsetSW]) {
              int i = (isCore ? SHADE_SLAB_CLIPPED - 3 + ((randu >> 13) & 0x07)
                  : (packed >> 13) & 0x3F);
              p.addPixel(offsetSW, zPixel, sh[i]);
            }
          }
          if (tNorthVisible) {
            if (tEastVisible
                && zPixel < zb[offsetNE]) {
              int i = (isCore ? SHADE_SLAB_CLIPPED - 3 + ((randu >> 19) & 0x07)
                  : (packed >> 19) & 0x3F);
              p.addPixel(offsetNE, zPixel, sh[i]);
            }
            if (tWestVisible
                && zPixel < zb[offsetNW]) {
              int i = (isCore ? SHADE_SLAB_CLIPPED - 3 + ((randu >> 25) & 0x07)
                  : (packed >> 25) & 0x3F);
              p.addPixel(offsetNW, zPixel, sh[i]);
            }
          }
        }
        ++offsetSE;
        --offsetSW;
        ++offsetNE;
        --offsetNW;
        ++xEast;
        --xWest;
        if (isCore)
          randu = ((randu << 16) + (randu << 1) + randu) & 0x7FFFFFFF;
      } while (packed >= 0);
      offsetSouthCenter += w;
      offsetNorthCenter -= w;
      ++ySouth;
      --yNorth;
    } while (--nLines > 0);
  }

  //////////  Ellipsoid Code ///////////
  //
  // Bob Hanson, 4/2008
  //
  //////////////////////////////////////


  private void renderQuadrant(int xSign, int ySign, int x, int y, int z, int diameter, int[] shades) {
    int radius = diameter / 2;
    int t = x + radius * xSign;
    int xStatus = (x < 0 ? -1 : x < width ? 0 : 1)
        + (t < 0 ? -2 : t < width ? 0 : 2);
    if (xStatus == -3 || xStatus == 3)
      return;

    t = y + radius * ySign;
    int yStatus = (y < 0 ? -1 : y < height ? 0 : 1)
        + (t < 0 ? -2 : t < height ? 0 : 2);
    if (yStatus == -3 || yStatus == 3)
      return;

    boolean unclipped = (mat == null && xStatus == 0 && yStatus == 0 
        && z - radius >= slab  && z <= depth);
    if (unclipped)
      renderQuadrantUnclipped(radius, xSign, ySign, z, shades);
    else
      renderQuadrantClipped(radius, xSign, ySign, x, y, z, shades);
  }

  private void renderQuadrantUnclipped(int radius, int xSign, int ySign, int z, int[] s) {
    int r2 = radius * radius;
    int dDivisor = radius * 2 + 1;
    int lineIncrement = (ySign < 0 ? -width : width);
    int ptLine = offsetPbufBeginLine;
    int[] zb = zbuf;
    Pixelator p = g3d.pixel;
    byte[] indexes = shader.sphereShadeIndexes;
    for (int i = 0, i2 = 0; i2 <= r2; 
        i2 += i + (++i),
        ptLine += lineIncrement) {
      int offset = ptLine;
      int s2 = r2 - i2;
      int z0 = z - radius;
      int y8 = ((i * ySign + radius) << 8) / dDivisor;
      for (int j = 0, j2 = 0; j2 <= s2;
           j2 += j + (++j),
           offset += xSign) {
          if (zb[offset] <= z0)
            continue;
          int k = (int)Math.sqrt(s2 - j2);
          z0 = z - k;
          if (zb[offset] <= z0)
            continue;
          int x8 = ((j * xSign + radius) << 8) / dDivisor;
          p.addPixel(offset,z0, s[indexes[((y8 << 8) + x8)]]);
      }
    }
  }

  private final P3 ptTemp = new P3();  
  private final int[] planeShades = new int[3];
  private final float[][] dxyz = new float[3][3];
  
  private void renderQuadrantClipped(int radius, int xSign, int ySign, int x, int y, int z, int[] shades) {
    boolean isEllipsoid = (mat != null);
    boolean checkOctant = (selectedOctant >= 0);
    int r2 = radius * radius;
    int dDivisor = radius * 2 + 1;
    int lineIncrement = (ySign < 0 ? -width : width);
    int ptLine = offsetPbufBeginLine;
    int randu = (x << 16) + (y << 1) ^ 0x33333333;
    int y8 = 0;
    int iShade = 0;
    Pixelator p = g3d.pixel;
    int z1 = 0;
    int h = height;
    int w = width;
    int x0 = x;
    int[] zb = zbuf;
    float[][] xyz = dxyz;
    int y0 = y;
    int z0 = z;
    int sl = slab;
    int de = depth;
    P3 pt = ptTemp;
    double[] c = coef;
    double[] rt = zroot;
    int oct = selectedOctant;
    Shader s = shader;
    int[] pl = planeShades;
    byte[] indexes = s.sphereShadeIndexes;
    int ps = planeShade;
    M3 m = mat;

    for (int i = 0, i2 = 0, yC = y; i2 <= r2; i2 += i + (++i), ptLine += lineIncrement, yC += ySign) {
      if (yC < 0) {
        if (ySign < 0)
          return;
        continue;
      }
      if (yC >= h) {
        if (ySign > 0)
          return;
        continue;
      }
      int s2 = r2 - (isEllipsoid ? 0 : i2);
      if (!isEllipsoid) {
        y8 = ((i * ySign + radius) << 8) / dDivisor;
      }
      randu = ((randu << 16) + (randu << 1) + randu) & 0x7FFFFFFF;
      int xC = x0;
      for (int j = 0, j2 = 0, iRoot = -1, mode = 1, offset = ptLine; j2 <= s2; j2 += j + (++j), offset += xSign, xC += xSign) {
        if (xC < 0) {
          if (xSign < 0)
            break;
          continue;
        }
        if (xC >= w) {
          if (xSign > 0)
            break;
          continue;
        }
        int zPixel;
        if (isEllipsoid) {
          /* simple quadratic formula for:
           * 
           * c0 x^2 + c1 y^2 + c2 z^2 + c3 xy + c4 xz + c5 yz + c6 x + c7 y + c8 z - 1 = 0 
           * 
           * or:
           * 
           * c2 z^2 + (c4 x + c5 y + c8)z + (c0 x^2 + c1 y^2 + c3 xy + c6 x + c7 y - 1) = 0
           * 
           * so:
           * 
           *  z = -(b/2a) +/- sqrt( (b/2a)^2 - c/a )
           */

          double b_2a = (c[4] * xC + c[5] * yC + c[8]) / c[2] / 2;
          double c_a = (c[0] * xC * xC + c[1] * yC * yC + c[3] * xC * yC + c[6]
              * xC + c[7] * yC - 1)
              / c[2];
          double f = b_2a * b_2a - c_a;
          if (f < 0) {
            if (iRoot >= 0) {
              // done for this line
              break;
            }
            continue;
          }
          f = Math.sqrt(f);
          rt[0] = (-b_2a - f);
          rt[1] = (-b_2a + f);
          iRoot = (z0 < sl ? 1 : 0);
          if ((zPixel = (int) rt[iRoot]) == 0)
            zPixel = z0;
          mode = 2;
          z1 = zPixel;
          if (checkOctant) {
            pt.set(xC - x0, yC - y0, zPixel - z0);
            m.rotate(pt);
            int thisOctant = 0;
            if (pt.x < 0)
              thisOctant |= 1;
            if (pt.y < 0)
              thisOctant |= 2;
            if (pt.z < 0)
              thisOctant |= 4;
            if (thisOctant == oct) {
              if (ps >= 0) {
                iShade = ps;
              } else {
                int iMin = 3;
                float dz;
                float zMin = Float.MAX_VALUE;
                for (int ii = 0; ii < 3; ii++) {
                  if ((dz = xyz[ii][2]) == 0)
                    continue;
                  float ptz = z0
                      + (-xyz[ii][0] * (xC - x) - xyz[ii][1]
                          * (yC - y0)) / dz;
                  if (ptz < zMin) {
                    zMin = ptz;
                    iMin = ii;
                  }
                }
                if (iMin == 3) {
                  iMin = 0;
                  zMin = z0;
                }
                rt[0] = zMin;
                iShade = pl[iMin];
              }
              zPixel = (int) rt[0];
              mode = 3;
              // another option: show back only
              //iRoot = 1;
              //zPixel = (int) zroot[iRoot];
            }
            boolean isCore = (z0 < sl ? zPixel >= sl : zPixel < sl);
            if (isCore) {
              z1 = zPixel = sl;
              mode = 0;
            }
          }
          if (zPixel < sl || zPixel > de || zb[offset] <= z1)
            continue;
        } else {
          int zOffset = (int) Math.sqrt(s2 - j2);
          zPixel = z0 + (z0 < sl ? zOffset : -zOffset);
          boolean isCore = (z0 < sl ? zPixel >= sl : zPixel < sl);
          if (isCore) {
            zPixel = sl;
            mode = 0;
          }
          if (zPixel < sl || zPixel > de || zb[offset] <= zPixel)
            continue;
        }
        switch (mode) {
        case 0: //core
          iShade = (SHADE_SLAB_CLIPPED - 3 + ((randu >> 8) & 0x07));
          randu = ((randu << 16) + (randu << 1) + randu) & 0x7FFFFFFF;
          mode = 1;
          break;
        case 2: //ellipsoid
          iShade = s.getEllipsoidShade(xC, yC, (float) rt[iRoot],
              radius, mDeriv);
          break;
        case 3: //ellipsoid fill
          p.clearPixel(offset, z1);
          break;
        default: //sphere
          int x8 = ((j * xSign + radius) << 8) / dDivisor;
          iShade = indexes[(y8 << 8) + x8];
          break;
        }
        p.addPixel(offset, zPixel, shades[iShade]);
      }
      randu = ((randu + xC + yC) | 1) & 0x7FFFFFFF;
    }
  }

}
