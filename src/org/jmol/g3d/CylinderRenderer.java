/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2017-02-12 13:55:04 -0600 (Sun, 12 Feb 2017) $
 * $Revision: 21428 $
 *
 * Copyright (C) 2003-2006  Miguel, Jmol Development, www.jmol.org
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

import javajs.util.AU;
import javajs.util.P3d;
import javajs.util.P3i;

import org.jmol.util.GData;
import org.jmol.util.Shader;

/**
 *<p>
 * Draws shaded cylinders in 3D.
 *</p>
 *<p>
 * Cylinders are used to draw bonds.
 *</p>
 *
 * @author Miguel, miguel@jmol.org
 */
class CylinderRenderer {

  private final Graphics3D g3d;
  private final LineRenderer line3d;
  private final Shader shader;

  CylinderRenderer(Graphics3D g3d) {
    this.g3d = g3d;
    line3d = g3d.line3d;
    shader = g3d.shader;
  }

  private short colixA, colixB;
  private int[] shadesA;
  private int[] shadesB;
  private int xA, yA, zA, xAend, yAend, zAend;
  private int dxB, dyB, dzB;
  private double xAf, yAf, zAf; 
  private double dxBf, dyBf, dzBf;
  private boolean tEvenDiameter;
  //private int evenCorrection;
  private int diameter;
  private byte endcaps;
  private boolean endCapHidden;
  private int xEndcap, yEndcap, zEndcap;
  private int argbEndcap;
  private short colixEndcap;
  private int endcapShadeIndex;

  private double radius, radius2, cosTheta, cosPhi, sinPhi;

  private boolean clipped;

  private int rasterCount;
  private double[][] xyztRaster = new double[][] {new double[32], new double[32], new double[32], new double[32] };
  private int[][] xyzfRaster = new int[][] {new int[32], new int[32], new int[32], new int[32]};


  void renderOld(short colixA, short colixB, int screen, 
              byte endcaps, int diameter, int xa, int ya,
              int za, int xb, int yb, int zb) {
    //0 for colixA or colixB means ignore for this pass
    int r = diameter / 2 + 1;
    //System.out.println("Cyl " + xa + " "  + ya + " "  + za + " "  + xb + " "  + yb + " "  + zb + " " );
    Graphics3D g = this.g3d;
    int codeMinA = g.clipCode3(xa - r, ya - r, za - r);
    int codeMaxA = g.clipCode3(xa + r, ya + r, za + r);
    int codeMinB = g.clipCode3(xb - r, yb - r, zb - r);
    int codeMaxB = g.clipCode3(xb + r, yb + r, zb + r);
    //all bits 0 --> no clipping
    int c = (codeMinA | codeMaxA | codeMinB | codeMaxB);
    clipped = (c != 0);
    //any two bits same in all cases --> fully clipped
    if (c == -1 || (codeMinA & codeMaxB & codeMaxA & codeMinB) != 0)
      return; // fully clipped;
    dxB = xb - xa;
    dyB = yb - ya;
    dzB = zb - za;
    if (diameter <= 1) {
      line3d.plotLineDeltaOld(g.getColorArgbOrGray(colixA), g
          .getColorArgbOrGray(colixB), xa, ya, za, dxB, dyB, dzB,
          clipped);
      return;
    }
    boolean drawBackside = (screen == 0 && (clipped 
        || endcaps == GData.ENDCAPS_FLAT || endcaps == GData.ENDCAPS_NONE));
    this.diameter = diameter;
    this.xA = xa;
    this.yA = ya;
    this.zA = za;
    this.endcaps = endcaps;
    shadesA = g.getShades(this.colixA = colixA);
    shadesB = g.getShades(this.colixB = colixB);
    calcArgbEndcap(true, false);

    // generate base ellipse
    
    calcCosSin(dxB, dyB, dzB);
    calcPoints(3, false);
    interpolate(0, 1, xyzfRaster, xyztRaster);
    interpolate(1, 2, xyzfRaster, xyztRaster);

    int[][] xyzf = xyzfRaster;
    if (endcaps == GData.ENDCAPS_FLAT)
      renderFlatEndcap(true, false, xyzf);
    g.setZMargin(5);
    int width = g.width;
    int[] zbuf = g.zbuf;
    int[] xr = xyzf[0];
    int[] yr = xyzf[1];
    int[] zr = xyzf[2];
    int[] fr = xyzf[3];
    Pixelator p = g.pixel;

    for (int i = rasterCount; --i >= 0;) {
      int fpz = fr[i] >> (8);
      int fpzBack = fpz >> 1;
      int x = xr[i];
      int y = yr[i];
      int z = zr[i];
      if (endCapHidden && argbEndcap != 0) {
        if (clipped) {
          g.plotPixelClippedArgb(argbEndcap, xEndcap + x, yEndcap + y, zEndcap - z
              - 1, width, zbuf, p);
          g.plotPixelClippedArgb(argbEndcap, xEndcap - x, yEndcap - y, zEndcap + z
              - 1, width, zbuf, p);
        } else {
          g.plotPixelUnclipped(argbEndcap, xEndcap + x, yEndcap + y, zEndcap
              - z - 1, width, zbuf, p);
          g.plotPixelUnclipped(argbEndcap, xEndcap - x, yEndcap - y, zEndcap
              + z - 1, width, zbuf, p);
        }
      }
      
      line3d.plotLineDeltaAOld(shadesA, shadesB, screen,
          fpz, xA + x, yA + y, zA - z, dxB, dyB, dzB, clipped);
      if (drawBackside) {
        line3d.plotLineDeltaOld(shadesA[fpzBack], shadesB[fpzBack], xA
            - x, yA - y, zA + z, dxB, dyB, dzB,clipped);
      }
    }
    g.setZMargin(0);
    if (endcaps == GData.ENDCAPS_SPHERICAL)
      renderSphericalEndcaps();
  }

  
  private P3d ptA0, ptB0;
  
  void renderBitsFloat(short colixA, short colixB, int screen, byte endcaps, int diameter, P3d ptA, P3d ptB) {
    Graphics3D g = this.g3d;
// checked already
//    if (diameter == 0 || diameter == 1) {
//      line3d.plotLineBits(g.getColorArgbOrGray(colixA), g.getColorArgbOrGray(colixB), ptA, ptB);
//      return;
//    }
    if (ptA0 == null) {
      ptA0 = new P3d();
      ptB0 = new P3d();
    }
    ptA0.setT(ptA);
    // dipole cross, cartoonRockets, draw mesh nofill or width = -1
    // oops -- problem here if diameter < 0 is that we may have already clipped it!
    int r = diameter / 2 + 1;
    int ixA = (int) Math.round(ptA.x);
    int iyA = (int) Math.round(ptA.y);
    int izA = (int) Math.round(ptA.z);
    int ixB = (int) Math.round(ptB.x);
    int iyB = (int) Math.round(ptB.y);
    int izB = (int) Math.round(ptB.z);
    int codeMinA = g.clipCode3(ixA - r, iyA - r, izA - r);
    int codeMaxA = g.clipCode3(ixA + r, iyA + r, izA + r);
    int codeMinB = g.clipCode3(ixB - r, iyB - r, izB - r);
    int codeMaxB = g.clipCode3(ixB + r, iyB + r, izB + r);
    //all bits 0 --> no clipping
    int c = (codeMinA | codeMaxA | codeMinB | codeMaxB);
    //any two bits same in all cases --> fully clipped
    clipped = (c != 0);
    //any two bits same in all cases --> fully clipped
    if (c == -1 || (codeMinA & codeMaxB & codeMaxA & codeMinB) != 0)
      return;
    dxBf = ptB.x - ptA.x;
    dyBf = ptB.y - ptA.y;
    dzBf = ptB.z - ptA.z;
    if (diameter > 0) {
      this.diameter = diameter;
      this.xAf = ptA.x;
      this.yAf = ptA.y;
      this.zAf = ptA.z;
    }
    boolean drawBackside = (screen == 0 && (clipped 
        || endcaps == GData.ENDCAPS_FLAT || endcaps == GData.ENDCAPS_NONE));
    this.xA = (int) xAf;
    this.yA = (int) yAf;
    this.zA = (int) zAf;
    this.dxB = (int) dxBf;
    this.dyB = (int) dyBf;
    this.dzB = (int) dzBf;

    this.shadesA = g.getShades(this.colixA = colixA);
    this.shadesB = g.getShades(this.colixB = colixB);
    this.endcaps = endcaps;
    calcArgbEndcap(true, true);
    int[][] xyzf = xyzfRaster;
    if (diameter > 0)
      generateBaseEllipsePrecisely(false);
    if (endcaps == GData.ENDCAPS_FLAT)
      renderFlatEndcap(true, true, xyzf);
    line3d.setLineBits(dxBf, dyBf);
    g.setZMargin(5);
    Pixelator p = g.pixel;
    int width = g.width;
    int[] zbuf = g.zbuf;
    int[] xr = xyzf[0];
    int[] yr = xyzf[1];
    int[] zr = xyzf[2];
    int[] fr = xyzf[3];
    for (int i = rasterCount; --i >= 0;) {
      int fpz = fr[i] >> (8);
      int fpzBack = fpz >> 1;
      int x = xr[i];
      int y = yr[i];
      int z = zr[i];
      if (endCapHidden && argbEndcap != 0) {
        if (clipped) {
          g.plotPixelClippedArgb(argbEndcap, xEndcap + x, yEndcap + y, zEndcap
              - z - 1, width, zbuf, p);
          g.plotPixelClippedArgb(argbEndcap, xEndcap - x, yEndcap - y, zEndcap
              + z - 1, width, zbuf, p);
        } else {
          g.plotPixelUnclipped(argbEndcap, xEndcap + x, yEndcap + y, zEndcap
              - z - 1, width, zbuf, p);
          g.plotPixelUnclipped(argbEndcap, xEndcap - x, yEndcap - y, zEndcap
              + z - 1, width, zbuf, p);
        }
      }
      ptA0.set(xA + x, yA + y, zA - z);
      ptB0.setT(ptA0);
      ptB0.x += dxB;
      ptB0.y += dyB;
      ptB0.z += dzB;
      line3d.plotLineDeltaABitsFloat(shadesA, shadesB, fpz, ptA0, ptB0, screen, clipped); 
      if (drawBackside) {
        ptA0.set(xA - x, yA - y, zA + z);
        ptB0.setT(ptA0);
        ptB0.x += dxB;
        ptB0.y += dyB;
        ptB0.z += dzB;
        line3d.plotLineDeltaABitsFloat(shadesA, shadesB, fpzBack, ptA0, ptB0, screen, clipped); 
      }
    }
    g.setZMargin(0);
    if (endcaps == GData.ENDCAPS_SPHERICAL)
      renderSphericalEndcaps();
    this.xAf += dxBf;
    this.yAf += dyBf;
    this.zAf += dzBf;
  }

  
  
  private P3i ptA0i = new P3i();
  private P3i ptB0i = new P3i();
  
  void renderBits(short colixA, short colixB, int screen, byte endcaps, int diameter, P3i ptA, P3i ptB) {
    Graphics3D g = this.g3d;
    if (diameter == 0 || diameter == 1) {
      line3d.plotLineBits(g.getColorArgbOrGray(colixA), g.getColorArgbOrGray(colixB), ptA, ptB, 0, 0, false);
      return;
    }
    ptA0i.setT(ptA);
    // dipole cross, cartoonRockets, draw mesh nofill or width = -1
    // oops -- problem here if diameter < 0 is that we may have already clipped it!
    int r = diameter / 2 + 1;
    int ixA = ptA.x;
    int iyA = ptA.y;
    int izA = ptA.z;
    int ixB = ptB.x;
    int iyB = ptB.y;
    int izB = ptB.z;
    int codeMinA = g.clipCode3(ixA - r, iyA - r, izA - r);
    int codeMaxA = g.clipCode3(ixA + r, iyA + r, izA + r);
    int codeMinB = g.clipCode3(ixB - r, iyB - r, izB - r);
    int codeMaxB = g.clipCode3(ixB + r, iyB + r, izB + r);
    //all bits 0 --> no clipping
    int c = (codeMinA | codeMaxA | codeMinB | codeMaxB);
    //any two bits same in all cases --> fully clipped
    clipped = (c != 0);
    //any two bits same in all cases --> fully clipped
    if (c == -1 || (codeMinA & codeMaxB & codeMaxA & codeMinB) != 0)
      return;
    dxBf = ptB.x - ptA.x;
    dyBf = ptB.y - ptA.y;
    dzBf = ptB.z - ptA.z;
    if (diameter > 0) {
      this.diameter = diameter;
      xAf = ptA.x;
      yAf = ptA.y;
      zAf = ptA.z;
    }
    boolean drawBackside = (screen == 0 && (clipped 
        || endcaps == GData.ENDCAPS_FLAT || endcaps == GData.ENDCAPS_NONE));
    xA = (int) xAf;
    yA = (int) yAf;
    zA = (int) zAf;
    dxB = (int) dxBf;
    dyB = (int) dyBf;
    dzB = (int) dzBf;

    shadesA = g.getShades(this.colixA = colixA);
    shadesB = g.getShades(this.colixB = colixB);
    this.endcaps = endcaps;
    calcArgbEndcap(true, true);
    int[][] xyzf = xyzfRaster;
    if (diameter > 0)
      generateBaseEllipsePrecisely(false);
    if (endcaps == GData.ENDCAPS_FLAT)
      renderFlatEndcap(true, true, xyzf);
    line3d.setLineBits(dxBf, dyBf);
    g.setZMargin(5);
    Pixelator p = g.pixel;
    int width = g.width;
    int[] zbuf = g.zbuf;
    int[] xr = xyzf[0];
    int[] yr = xyzf[1];
    int[] zr = xyzf[2];
    int[] fr = xyzf[3];
    for (int i = rasterCount; --i >= 0;) {
      int fpz = fr[i] >> (8);
      int fpzBack = fpz >> 1;
      int x = xr[i];
      int y = yr[i];
      int z = zr[i];
      if (endCapHidden && argbEndcap != 0) {
        if (clipped) {
          g.plotPixelClippedArgb(argbEndcap, xEndcap + x, yEndcap + y, zEndcap
              - z - 1, width, zbuf, p);
          g.plotPixelClippedArgb(argbEndcap, xEndcap - x, yEndcap - y, zEndcap
              + z - 1, width, zbuf, p);
        } else {
          g.plotPixelUnclipped(argbEndcap, xEndcap + x, yEndcap + y, zEndcap
              - z - 1, width, zbuf, p);
          g.plotPixelUnclipped(argbEndcap, xEndcap - x, yEndcap - y, zEndcap
              + z - 1, width, zbuf, p);
        }
      }
      ptA0i.set(xA + x, yA + y, zA - z);
      ptB0i.setT(ptA0i);
      ptB0i.x += dxB;
      ptB0i.y += dyB;
      ptB0i.z += dzB;
      line3d.plotLineDeltaABitsInt(shadesA, shadesB, fpz, ptA0i, ptB0i, screen, clipped); 
      if (drawBackside) {
        ptA0i.set(xA - x, yA - y, zA + z);
        ptB0i.setT(ptA0i);
        ptB0i.x += dxB;
        ptB0i.y += dyB;
        ptB0i.z += dzB;
        line3d.plotLineDeltaABitsInt(shadesA, shadesB, fpzBack, ptA0i, ptB0i, screen, clipped); 
      }
    }
    g.setZMargin(0);
    if (endcaps == GData.ENDCAPS_SPHERICAL)
      renderSphericalEndcaps();
    this.xAf += dxBf;
    this.yAf += dyBf;
    this.zAf += dzBf;
  }

  private double xTip, yTip, zTip;
  
  void renderConeOld(short colix, byte endcap, int diameter, double xa, double ya,
                  double za, double xtip, double ytip, double ztip, boolean doFill, boolean isBarb) {
    dxBf = (xtip) - (xAf = xa);
    dyBf = (ytip) - (yAf = ya);
    dzBf = (ztip) - (zAf = za);
    xA = (int) Math.floor(xAf);
    yA = (int) Math.floor(yAf);
    zA = (int) Math.floor(zAf);
    dxB = (int) Math.floor(dxBf);
    dyB = (int) Math.floor(dyBf);
    dzB = (int) Math.floor(dzBf);
    xTip = xtip;
    yTip = ytip;
    zTip = ztip;
    shadesA = g3d.getShades(colixA = colix);
    int shadeIndexTip = shader.getShadeIndex(dxB, dyB, -dzB);
    Graphics3D g3d = this.g3d;
    Pixelator p = g3d.pixel;
    int width = g3d.width;
    int[] zbuf = g3d.zbuf;
    g3d.plotPixelClippedArgb(shadesA[shadeIndexTip], (int) xtip,
        (int) ytip, (int) ztip, width, zbuf, p);

    this.diameter = diameter;
    if (diameter <= 1) {
      if (diameter == 1)
        line3d.plotLineDeltaOld(colixA, colixA, xA,
            yA, zA, dxB, dyB, dzB, clipped);
      return;
    }
    this.endcaps = endcap;
    calcArgbEndcap(false, true);
    generateBaseEllipsePrecisely(isBarb);
    if (!isBarb && endcaps == GData.ENDCAPS_FLAT)
      renderFlatEndcap(false, true, xyzfRaster);
    g3d.setZMargin(5);
    double[] xr = xyztRaster[0];
    double[] yr = xyztRaster[1];
    double[] zr = xyztRaster[2];
    int[] fr = xyzfRaster[3];
    int[] sA = shadesA;
    boolean doOpen = (endCapHidden && argbEndcap != 0);
    for (int i = rasterCount; --i >= 0;) {
      double x = xr[i];
      double y = yr[i];
      double z = zr[i];
      int fpz = fr[i] >> (8);
      double xUp = xAf + x, yUp = yAf + y, zUp = zAf - z;
      double xDn = xAf - x, yDn = yAf - y, zDn = zAf + z;
      int argb = sA[0];
      if (doOpen) {
        g3d.plotPixelClippedArgb(argbEndcap, (int) xUp, (int) yUp,
            (int) zUp, width, zbuf, p);
        g3d.plotPixelClippedArgb(argbEndcap, (int) xDn, (int) yDn,
            (int) zDn, width, zbuf, p);
      }
      if (argb != 0) {
        line3d.plotLineDeltaAOld(sA, sA, 0, fpz,
            (int) xUp, (int) yUp, (int) zUp, (int) Math.ceil(xTip - xUp),
            (int) Math.ceil(yTip - yUp), (int) Math.ceil(zTip - zUp), true);
        
        if (doFill) { //rockets, not arrows
          line3d.plotLineDeltaAOld(sA, sA, 0, fpz,
            (int) xUp, (int) yUp + 1, (int) zUp, (int) Math.ceil(xTip - xUp),
            (int) Math.ceil(yTip - yUp) + 1, (int) Math.ceil(zTip - zUp), true);
          line3d.plotLineDeltaAOld(sA, sA, 0, fpz,
            (int) xUp + 1, (int) yUp, (int) zUp, (int) Math.ceil(xTip - xUp) + 1,
            (int) Math.ceil(yTip - yUp), (int) Math.ceil(zTip - zUp), true);
        }    
    
        if (!isBarb && !(endcaps != GData.ENDCAPS_FLAT && dzB > 0)) {
          line3d.plotLineDeltaOld(argb, argb, (int) xDn,
              (int) yDn, (int) zDn, (int) Math.ceil(xTip - xDn), (int) Math
                  .ceil(yTip - yDn), (int) Math.ceil(zTip - zDn), true);
        }
      }
    }
    g3d.setZMargin(0);
  }

  private void generateBaseEllipsePrecisely(boolean isBarb) {
    calcCosSin(dxBf, dyBf, dzBf);
    calcPoints(isBarb ? 2 : 3, true);
    interpolatePrecisely(0, 1, xyzfRaster, xyztRaster);
    if (!isBarb)
      interpolatePrecisely(1, 2, xyzfRaster, xyztRaster);
    for (int i = 3; --i >= 0;)
      for (int j = rasterCount; --j >= 0;)
        xyzfRaster[i][j] = (int) Math.floor(xyztRaster[i][j]);
  }

  private void calcPoints(int count, boolean isPrecise) {
    calcRotatedPoint(0f, 0, isPrecise, xyzfRaster, xyztRaster);
    calcRotatedPoint(0.5f, 1, isPrecise, xyzfRaster, xyztRaster);
    if ((rasterCount = count) == 3)
      calcRotatedPoint(1f, 2, isPrecise, xyzfRaster, xyztRaster);
  }

  private void calcCosSin(double dx, double dy, double dz) {
    double mag2d2 = dx * dx + dy * dy;
    if (mag2d2 == 0) {
      cosTheta = 1;
      cosPhi = 1;
      sinPhi = 0;
    } else {
      double mag2d = (double) Math.sqrt(mag2d2);
      double mag3d = (double) Math.sqrt(mag2d2 + dz * dz);
      cosTheta = dz / mag3d;
      cosPhi = dx / mag2d;
      sinPhi = dy / mag2d;
    }
  }

  private void calcRotatedPoint(double t, int i, boolean isPrecision,
                                int[][] xyzf, double[][] xyzt) {
    xyzt[3][i] = t;
    double tPI = t * Math.PI;
    double xT = Math.sin(tPI) * cosTheta;
    double yT = Math.cos(tPI);
    double xR = radius * (xT * cosPhi - yT * sinPhi);
    double yR = radius * (xT * sinPhi + yT * cosPhi);
    double z2 = radius2 - (xR * xR + yR * yR);
    double zR = (z2 > 0 ? Math.sqrt(z2) : 0);

    if (isPrecision) {
      xyzt[0][i] = (double) xR;
      xyzt[1][i] = (double) yR;
      xyzt[2][i] = (double) zR;
    } else if (tEvenDiameter) {
      xyzf[0][i] = (int) (xR - 0.5);
      xyzf[1][i] = (int) (yR - 0.5);
      xyzf[2][i] = (int) (zR + 0.5);
    } else {
      xyzf[0][i] = (int) (xR);
      xyzf[1][i] = (int) (yR);
      xyzf[2][i] = (int) (zR + 0.5);
    }
    xyzf[3][i] = shader.getShadeFp8((double) xR, (double) yR, (double) zR);
  }

  private int allocRaster(boolean isPrecision, int[][] xyzf, double[][] xyzt) {
    if (rasterCount >= xyzf[0].length)
    while (rasterCount >= xyzf[0].length) {
      for (int i = 4; --i >= 0;)
        xyzf[i] = AU.doubleLengthI(xyzf[i]);
        xyzt[3] = AU.doubleLengthD(xyzt[3]);
    }
    if (isPrecision)
      while (rasterCount >= xyzt[0].length) {
        for (int i = 3; --i >= 0;)
          xyzt[i] = AU.doubleLengthD(xyzt[i]);
      }
    return rasterCount++;
  }

 private void interpolate(int iLower, int iUpper, 
                          int[][] xyzf, double[][] xyzt) {
   int[] x = xyzf[0];
   int[] y = xyzf[1];
    
    int dx = x[iUpper] - x[iLower];
    if (dx < 0)
      dx = -dx;
    int dy = y[iUpper] - y[iLower];
    if (dy < 0)
      dy = -dy;
    if ((dx + dy) <= 1)
      return;
    int iMid = allocRaster(false, xyzf, xyzt);
    x = xyzf[0];
    y = xyzf[1];
    int[] f = xyzf[3];
    double tLower = xyzt[3][iLower];
    double tUpper = xyzt[3][iUpper];
    for (int j = 4; --j >= 0;) {
      double tMid = (tLower + tUpper) / 2;
      calcRotatedPoint(tMid, iMid, false, xyzf, xyzt);
      if ((x[iMid] == x[iLower])
          && (y[iMid] == y[iLower])) {
        f[iLower] = (f[iLower] + f[iMid]) >>> 1;
        tLower = tMid;
      } else if ((x[iMid] == x[iUpper])
          && (y[iMid] == y[iUpper])) {
        f[iUpper] = (f[iUpper] + f[iMid]) >>> 1;
        tUpper = tMid;
      } else {
        interpolate(iLower, iMid, xyzf, xyzt);
        interpolate(iMid, iUpper, xyzf, xyzt);
        return;
      }
    }
    x[iMid] = x[iLower];
    y[iMid] = y[iUpper];
  }

  private void interpolatePrecisely(int iLower, int iUpper, int[][] xyzf, double[][] xyzt) {
    double[] x = xyzt[0];
    double[] y = xyzt[1];
    
    int dx = (int) Math.floor(x[iUpper])
        - (int) Math.floor(x[iLower]);
    if (dx < 0)
      dx = -dx;
    double dy = (int) Math.floor(y[iUpper])
        - (int) Math.floor(y[iLower]);
    if (dy < 0)
      dy = -dy;
    if ((dx + dy) <= 1)
      return;
    double[] t = xyzt[3];
    double tLower = t[iLower];
    double tUpper = t[iUpper];
    int iMid = allocRaster(true, xyzf, xyzt);
    x = xyzt[0];
    y = xyzt[1];
    t = xyzt[3];
    int[] f = xyzf[3];
    for (int j = 4; --j >= 0;) {
      double tMid = (tLower + tUpper) / 2;
      calcRotatedPoint(tMid, iMid, true, xyzf, xyzt);
      if (((int) Math.floor(x[iMid]) == (int) Math
          .floor(x[iLower]))
          && ((int) Math.floor(y[iMid]) == (int) Math
              .floor(y[iLower]))) {
        f[iLower] = (f[iLower] + f[iMid]) >>> 1;
        tLower = tMid;
      } else if (((int) Math.floor(x[iMid]) == (int) Math
          .floor(x[iUpper]))
          && ((int) Math.floor(y[iMid]) == (int) Math
              .floor(y[iUpper]))) {
        f[iUpper] = (f[iUpper] + f[iMid]) >>> 1;
        tUpper = tMid;
      } else {
        interpolatePrecisely(iLower, iMid, xyzf, xyzt);
        interpolatePrecisely(iMid, iUpper, xyzf, xyzt);
        return;
      }
    }
    x[iMid] = x[iLower];
    y[iMid] = y[iUpper];
  }

  private void renderFlatEndcap(boolean isCylinder, boolean isPrecise, int[][] xyzf) {
    int xT, yT, zT;
    if (isPrecise) {
      if (dzBf == 0 || colixEndcap == 0 || !g3d.setC(colixEndcap))
        return;
      double xTf = xAf;
      double yTf = yAf;
      double zTf = zAf;
      if (isCylinder && dzBf < 0) {
        xTf += dxBf;
        yTf += dyBf;
        zTf += dzBf;
      }
      xT = (int) xTf;
      yT = (int) yTf;
      zT = (int) zTf;
    } else {
      if (dzB == 0 || colixEndcap == 0 || !g3d.setC(colixEndcap))
        return;
      xT = xAend;
      yT = yAend;
      zT = zAend;
      if (isCylinder && dzB < 0) {
        xT += dxB;
        yT += dyB;
        zT += dzB;
      }
    }

    int yMin = xyzf[1][0];
    int yMax = xyzf[1][0];
    int zXMin = 0, zXMax = 0;

    // findMinMaxY

    int[] xr = xyzf[0];
    int[] yr = xyzf[1];
    int[] zr = xyzf[2];
    
    for (int i = rasterCount; --i > 0;) {
      int y = yr[i];
      if (y < yMin)
        yMin = y;
      else if (y > yMax)
        yMax = y;
      else {
        y = -y;
        if (y < yMin)
          yMin = y;
        else if (y > yMax)
          yMax = y;
      }
    }

    for (int y = yMin; y <= yMax; ++y) {
      // findMinMaxX
      int xMin = Integer.MAX_VALUE;
      int xMax = Integer.MIN_VALUE;
      for (int i = rasterCount; --i >= 0;) {
        if (yr[i] == y) {
          int x = xr[i];
          if (x < xMin) {
            xMin = x;
            zXMin = zr[i];
          }
          if (x > xMax) {
            xMax = x;
            zXMax = zr[i];
          }
        }
        if (yr[i] == -y) { // 0 will run through here too
          int x = -xr[i];
          if (x < xMin) {
            xMin = x;
            zXMin = -zr[i];
          }
          if (x > xMax) {
            xMax = x;
            zXMax = -zr[i];
          }
        }
      }
      int count = xMax - xMin + 1;
      g3d.setColorNoisy(endcapShadeIndex);
      g3d.plotPixelsClippedRaster(count, xT + xMin, yT + y, zT - zXMin - 1, zT
          - zXMax - 1, null, null);
    }
  }

  private void renderSphericalEndcaps() {
    if (colixA != 0 && g3d.setC(colixA))
      g3d.fillSphereXYZ(diameter, xA, yA, zA + 1);
    if (colixB != 0 && g3d.setC(colixB))
      g3d.fillSphereXYZ(diameter, xA + dxB, yA + dyB, zA + dzB + 1);
  }

  private void calcArgbEndcap(boolean tCylinder, boolean isFloat) {
    tEvenDiameter = ((diameter & 1) == 0);
    radius = diameter / 2.0f;
    radius2 = radius * radius;
    endCapHidden = false;
    double dzf = (isFloat ? dzBf : (double) dzB);
    if (endcaps == GData.ENDCAPS_SPHERICAL || dzf == 0)
      return;
    xEndcap = xAend = xA;
    yEndcap = yAend = yA;
    zEndcap = zAend = zA;
    int[] shadesEndcap;
    double dxf = (isFloat ? dxBf : (double) dxB);
    double dyf = (isFloat ? dyBf : (double) dyB);
    if (dzf >= 0 || !tCylinder) {
      endcapShadeIndex = shader.getShadeIndex(-dxf, -dyf, dzf);
      if (colixA == 0) {
        xAend += dxB/2;
        yAend += dyB/2;
        zAend += dzB/2;
        colixEndcap = colixB;
      } else {
        colixEndcap = colixA;
      }
    } else {
      endcapShadeIndex = shader.getShadeIndex(dxf, dyf, -dzf);
      if (colixB == 0) {
        colixEndcap = colixA;
        xAend -= dxB/2;
        yAend -= dyB/2;
        zAend -= dzB/2;
      } else {
        colixEndcap = colixB;
        xEndcap += dxB;
        yEndcap += dyB;
        zEndcap += dzB;
      }
    }
    shadesEndcap = (colixEndcap == colixA ? shadesA : shadesB);
    // limit specular glare on endcap
    if (endcapShadeIndex > Shader.SHADE_INDEX_NOISY_LIMIT)
      endcapShadeIndex = Shader.SHADE_INDEX_NOISY_LIMIT;
    argbEndcap = shadesEndcap[endcapShadeIndex];
    endCapHidden = (endcaps == GData.ENDCAPS_HIDDEN);
  }

}
