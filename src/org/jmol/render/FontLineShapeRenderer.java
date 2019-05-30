/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-08-05 16:26:58 -0500 (Sun, 05 Aug 2007) $
 * $Revision: 8032 $
 *
 * Copyright (C) 2002-2006  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.render;


import org.jmol.modelset.TickInfo;
import org.jmol.script.T;
import org.jmol.util.GData;

import org.jmol.awtjs.swing.Font;
import javajs.util.P3;
import javajs.util.P3i;
import javajs.util.PT;

import org.jmol.util.SimpleUnitCell;
import javajs.util.V3;

public abstract class FontLineShapeRenderer extends ShapeRenderer {

  // Axes, Bbcage, Measures, Uccage, also Sticks, Echo, Measures, Labels

  protected float imageFontScaling;
  protected P3 tickA, tickB, tickAs, tickBs;
  protected Font font3d;

  final protected P3i pt0i = new P3i();
  final protected P3i pt2i = new P3i();
  protected final P3i s1 = new P3i();
  protected final P3i s2 = new P3i();

  final protected P3 pointT = new P3();
  final protected P3 pointT2 = new P3();
  final protected P3 pointT3 = new P3();
  final protected V3 vectorT = new V3();
  final protected V3 vectorT2 = new V3();
  final protected V3 vectorT3 = new V3();

  //final Rectangle box = new Rectangle();

  protected TickInfo tickInfo;

  protected boolean draw000 = true;
  protected int width;
  protected byte endcap = GData.ENDCAPS_SPHERICAL;
  protected P3 pt0 = new P3();
  protected P3 pt1 = new P3();

  //protected void clearBox() {
  //  box.setBounds(0, 0, 0, 0);
  //}
  
  protected int getDiameter(int z, int mad10OrPixels) {
    int diameter;
    boolean isMad10 = (mad10OrPixels > 20);
    switch (exportType) {
    case GData.EXPORT_CARTESIAN:
      diameter = (isMad10 ? mad10OrPixels 
          : (int) Math.floor(vwr.tm.unscaleToScreen(z, mad10OrPixels * 2/10f) * 1000));
      break;
    default:
      if (isMad10) {
        // mad
        diameter = (int) vwr.tm.scaleToScreen(z, mad10OrPixels/10); 
      } else {
        // pixels, and that's what we want
        if (g3d.isAntialiased())
          mad10OrPixels += mad10OrPixels;
        diameter = mad10OrPixels;
      }
    }
    return diameter;
  }  

  protected void renderLine(P3 p0, P3 p1, int diameter, 
                            boolean drawTicks) {
    // used by Bbcage, Uccage, and axes
    if (diameter < 0)
      g3d.drawDashedLineBits(8, 4, p0, p1);
    else
      g3d.fillCylinderBits(endcap, diameter, p0, p1);
    if (!drawTicks || tickInfo == null)
      return;
    // AtomA and AtomB molecular coordinates must be set previously
    checkTickTemps();
    tickAs.setT(p0);
    tickBs.setT(p1);
    drawTicks(diameter, true);
  }

  protected void checkTickTemps() {
    if (tickA == null) {
      tickA = new P3();
      tickB = new P3();
      tickAs = new P3();
      tickBs = new P3();
    }
  }

  protected void drawTicks(int diameter, boolean withLabels) {
    if (Float.isNaN(tickInfo.first))
      tickInfo.first = 0;
    drawTicks2(tickInfo.ticks.x, 8, diameter, (!withLabels ? null : tickInfo.tickLabelFormats == null ? 
            new String[] { "%0.2f" } : tickInfo.tickLabelFormats));
    drawTicks2(tickInfo.ticks.y, 4, diameter, null);
    drawTicks2(tickInfo.ticks.z, 2, diameter, null);
  }

  private void drawTicks2(float dx, int length,
                         int diameter, String[] formats) {

    if (dx == 0)
      return;
    /*
    boolean isOut = true;
    if (dx < 0) {
      isOut = false;
      dx = -dx;
    }
    */
    if (g3d.isAntialiased())
      length *= 2;
    // perpendicular to line on screen:
    vectorT2.set(tickBs.x, tickBs.y, 0);
    vectorT.set(tickAs.x, tickAs.y, 0);
    vectorT2.sub(vectorT);
    if (vectorT2.length() < 50)
      return;

    float signFactor = tickInfo.signFactor;
    vectorT.sub2(tickB, tickA);
    float d0 = vectorT.length();
    if (tickInfo.scale != null) {
      if (Float.isNaN(tickInfo.scale.x)) { // unitcell
        float a = vwr.getUnitCellInfo(SimpleUnitCell.INFO_A);
        if (!Float.isNaN(a))
          vectorT.set(vectorT.x / a, vectorT.y
              / vwr.getUnitCellInfo(SimpleUnitCell.INFO_B), vectorT.z
              / vwr.getUnitCellInfo(SimpleUnitCell.INFO_C));
      } else {
        vectorT.set(vectorT.x * tickInfo.scale.x, vectorT.y * tickInfo.scale.y,
            vectorT.z * tickInfo.scale.z);
      }
    }
    // d is in scaled units
    float d = vectorT.length() + 0.0001f * dx;
    if (d < dx)
      return;
    float f = dx / d * d0 / d;
    vectorT.scale(f);
    float dz = (tickBs.z - tickAs.z) / (d / dx);
    // TODO: z-value error: ONLY APPROXIMATE
    // vectorT is now the length of the spacing between ticks
    // but we may have an offset.
    d += tickInfo.first;
    float p = ((int) Math.floor(tickInfo.first / dx)) * dx - tickInfo.first;
    pointT.scaleAdd2(p / dx, vectorT, tickA);
    p += tickInfo.first;
    float z = tickAs.z;
    if (diameter < 0)
      diameter = 1;
    vectorT2.set(-vectorT2.y, vectorT2.x, 0);
    vectorT2.scale(length / vectorT2.length());
    P3 ptRef = tickInfo.reference; // not implemented
    if (ptRef == null) {
      pointT3.setT(vwr.getBoundBoxCenter());
      if (vwr.g.axesMode == T.axeswindow) {
        pointT3.add3(1, 1, 1);
      }
    } else {
      pointT3.setT(ptRef);
    }
    tm.transformPtScr(pointT3, pt2i);
    //too annoying! float tx = vectorT2.x * ((ptA.screenX + ptB.screenX) / 2 - pt2.x);
    //float ty = vectorT2.y * ((ptA.screenY + ptB.screenY) / 2 - pt2.y);
    //if (tx + ty < -0.1)
      //vectorT2.scale(-1);
    //if (!isOut)
      //vectorT2.scale(-1);
    boolean horizontal = (Math.abs(vectorT2.x / vectorT2.y) < 0.2);
    boolean centerX = horizontal;
    boolean centerY = !horizontal;
    boolean rightJustify = !centerX && (vectorT2.x < 0);
    boolean drawLabel = (formats != null && formats.length > 0);
    int x, y;
    Object[] val = new Object[1];
    int i = (draw000 ? 0 : -1);
    while (p < d) {
      if (p >= tickInfo.first) {
        pointT2.setT(pointT);
        tm.transformPt3f(pointT2, pointT2);
        drawLine((int) Math.floor(pointT2.x), (int) Math.floor(pointT2.y), (int) z,
            (x = (int) Math.floor(pointT2.x + vectorT2.x)),
            (y = (int) Math.floor(pointT2.y + vectorT2.y)), (int) z, diameter);
        if (drawLabel && (draw000 || p != 0)) {
          val[0] = Float.valueOf((p == 0 ? 0 : p * signFactor));
          String s = PT.sprintf(formats[i % formats.length], "f", val);
          drawString(x, y, (int) z, 4, rightJustify, centerX, centerY,
              (int) Math.floor(pointT2.y), s);
        }
      }
      pointT.add(vectorT);
      p += dx;
      z += dz;
      i++;
    }
  }

  protected int drawLine(int x1, int y1, int z1, int x2, int y2, int z2,
                         int diameter) {
    return drawLine2(x1, y1, z1, x2, y2, z2, diameter);
  }

  protected int drawLine2(int x1, int y1, int z1, int x2, int y2, int z2, int diameter) {
    pt0.set(x1, y1, z1);
    pt1.set(x2, y2, z2);
    if (dotsOrDashes) {
      if (dashDots != null)
        drawDashed(x1, y1, z1, x2, y2, z2, dashDots);
    } else {
      if (diameter < 0) {
        g3d.drawDashedLineBits(8, 4, pt0 , pt1);
        return 1;
      }    
      g3d.fillCylinderBits(GData.ENDCAPS_FLAT, diameter, pt0, pt1);
    }
    return (diameter + 1) / 2;
  }

  protected void drawString(int x, int y, int z, int radius,
                            boolean rightJustify, boolean centerX,
                            boolean centerY, int yRef, String sVal) {
    if (sVal == null)
      return;
    int width = font3d.stringWidth(sVal);
    int height = font3d.getAscent();
    int xT = x;
    if (rightJustify)
      xT -= radius / 2 + 2 + width;
    else if (centerX)
      xT -= radius / 2 + 2 + width / 2;
    else
      xT += radius / 2 + 2;
    int yT = y;
    if (centerY)
      yT += height / 2;
    else if (yRef == 0 || yRef < y)
      yT += height;
    else
      yT -= radius / 2;
    int zT = z - radius - 2;
    if (zT < 1)
      zT = 1;
    //if (!box.contains(xT, yT) && !box.contains(xT + width, yT)
      //  && !box.contains(xT, yT + height)
        //&& !box.contains(xT + width, yT + height)) {
      g3d.drawString(sVal, font3d, xT, yT, zT, zT, (short) 0);
   //   box.setBounds(xT, yT, width, height);
   // }
  }

  protected final static int[] dashes =   { 12, 0, 0, 2, 5, 7, 10 };
  protected final static int[] hDashes =  { 10, 7, 6, 1, 3, 4, 6, 7, 9 };

  protected final static int[] ndots =  { 0, 3, 1000 };
  protected final static int[] sixdots =  { 12, 3, 6, 1, 3, 5, 7, 9, 11 };
  protected final static int[] fourdots = { 13, 3, 5, 2, 5, 8, 11 };
  protected final static int[] twodots =  { 12, 3, 4, 3, 9 };

  protected short colixA, colixB;
  protected boolean dotsOrDashes;
  protected int[] dashDots;

  protected void drawDashed(int xA, int yA, int zA, int xB, int yB, int zB,
                          int[] array) {
    if (array == null || width < 0)
      return;
    // for sticks and measures
    float f = array[0];
    float dx = xB - xA;
    float dy = yB - yA;
    float dz = zB - zA;
    int n = 0;
    boolean isNdots = (array == ndots);
    boolean isDots = (isNdots || array == sixdots);
    if (isDots) {
      float d2 = (dx * dx + dy * dy)  / (width * width);
      if (isNdots) {
        f = (float) (Math.sqrt(d2) / 1.5);
        n = (int) f + 2;
      } else if (d2 < 8) {
        array = twodots;
      } else if (d2 < 32) {
        array = fourdots;
      }
    }
    int ptS = array[1];
    int ptE = array[2];
    short colixS = colixA;
    short colixE = (ptE == 0 ? colixB : colixA);
    if (n == 0) 
      n = array.length;
    for (int i = 0, pt = 3; pt < n; pt++) {
      i = (isNdots ? i + 1 : array[pt]);
      int xS = (int) Math.floor(xA + dx * i / f);
      int yS = (int) Math.floor(yA + dy * i / f);
      int zS = (int) Math.floor(zA + dz * i / f);
      if (isDots) {
        s1.set(xS, yS, zS);
        if (pt == ptS)
          g3d.setC(colixA);
        else if (pt == ptE)
          g3d.setC(colixB);
        g3d.fillSphereI(width, s1);
        continue;
      }
      if (pt == ptS)
        colixS = colixB;
      i = array[++pt];
      if (pt == ptE)
        colixE = colixB;
      int xE = (int) Math.floor(xA + dx * i / f);
      int yE = (int) Math.floor(yA + dy * i / f);
      int zE = (int) Math.floor(zA + dz * i / f);
      fillCylinder(colixS, colixE, GData.ENDCAPS_FLAT, width, xS, yS, zS,
          xE, yE, zE);
    }
  }
  
  protected boolean asLineOnly;

  protected void fillCylinder(short colixA, short colixB, byte endcaps,
                              int diameter, int xA, int yA, int zA, int xB,
                              int yB, int zB) {
    if (asLineOnly)
      g3d.drawLine(colixA, colixB, xA, yA, zA, xB, yB, zB);
    else
      g3d.fillCylinderXYZ(colixA, colixB, endcaps, 
          (!isExport || mad == 1 ? diameter : mad), 
          xA, yA, zA, xB, yB, zB);
  }




}
