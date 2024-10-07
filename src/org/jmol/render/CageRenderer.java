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


import org.jmol.shape.Bbcage;
import org.jmol.shape.FontLineShape;
import org.jmol.util.BoxInfo;

import javajs.util.BS;
import javajs.util.MeasureD;
import javajs.util.P3d;

abstract class CageRenderer extends FontLineShapeRenderer {

  // Bbcage and Uccage

  protected final P3d[] p3Screens = new P3d[8];
  {
    for (int i = 8; --i >= 0; )
      p3Screens[i] = new P3d();
  }

  protected char[] tickEdges;
  
  protected boolean isSlab;
  protected boolean isPolymer;
  protected int periodicity = 0x7;
  protected int nDims = 3;
  protected BS bsPeriod; 
  protected P3d pt = new P3d();

  protected P3d[] vvert, vvertA;

  /**
   * Uccage only
   * 
   * @param vertices  
   * @param scale 
   */
  protected void setPeriodicity(P3d[] vertices, double scale){}
  
  protected void renderCage(int mad, P3d[] vertices, int[][] faces,
                        P3d[] axisPoints, int firstLine, int allowedEdges0,
                        int allowedEdges1, double scale) {
    //clearBox();
    g3d.setC(colix);
    FontLineShape fls = (FontLineShape) shape;
    boolean hiddenLines = (faces != null);
    
    imageFontScaling = vwr.imageFontScaling;
    font3d = vwr.gdata.getFont3DScaled(fls.font3d, imageFontScaling);

    double zSum = 0;
    for (int i = 8; --i >= 0;) {
      pt.setT(vertices[i]);
      if (scale != 1) {
        pt.sub(vertices[0]);
        pt.scaleAdd2(scale, pt, vertices[0]);
      }
      tm.transformPtNoClip(pt, p3Screens[i]);
      zSum += p3Screens[i].z;
    }
    setPeriodicity(vertices, scale);
    BS bsSolid = null;
    if (hiddenLines) {
      // bsSolid marks all points on faces that are front-facing
      // lines to all other points should be dashed 
      bsSolid = new BS();
      for (int i = 12; --i >= 0;) {
        int[] face = faces[i];
        MeasureD.getNormalThroughPoints(p3Screens[face[0]], p3Screens[face[1]], p3Screens[face[2]], pt1, pt);
        if (pt1.z <= 0) {
          bsSolid.set(face[0]);
          bsSolid.set(face[1]);
          bsSolid.set(face[2]);
        }
      }
    }
    int diameter = getDiameter((int) Math.floor(zSum / 8), mad);
    int axisPt = 2;
    char edge = 0;
    allowedEdges0 &= (isPolymer ? 0x1 : isSlab ? 0x55 : 0xFF);
    allowedEdges1 &= (isPolymer ? 0x10 : isSlab ? 0x55 : 0xFF);
    setBSPeriod();
    for (int i = firstLine * 2; i < 24; i += 2) {
      if (bsPeriod != null && !bsPeriod.get(i))
        continue;
      int d = diameter;
      int edge0 = BoxInfo.edges[i];
      int edge1 = BoxInfo.edges[i + 1];
      if (hiddenLines && (!bsSolid.get(edge0) || !bsSolid.get(edge1)))
        d = -Math.abs(diameter);
      if (axisPoints != null && edge0 == 0)
        tm.transformPtNoClip(axisPoints[axisPt--], p3Screens[0]);
      if ((allowedEdges0 & (1 << edge0)) == 0 
        || (allowedEdges1 & (1 << edge1)) == 0)
        continue;
      boolean drawTicks = (fls.tickInfos != null && (edge = tickEdges[i >> 1]) != 0);
      if (drawTicks) {
        checkTickTemps();
        tickA.setT(vertices[edge0]);
        tickB.setT(vertices[edge1]);
        double start = 0;
        if (shape instanceof Bbcage)
          switch (edge) {
          case 'x':
            start = tickA.x;
            break;
          case 'y':
            start = tickA.y;
            break;
          case 'z':
            start = tickA.z;
            break;
          }
        tickInfo = fls.tickInfos["xyz".indexOf(edge) + 1];
        if (tickInfo == null)
          tickInfo = fls.tickInfos[0];
        if (tickInfo == null)
          drawTicks = false;
        else
          tickInfo.first = start;
      }
      renderCageLine(i, edge0, edge1, d, drawTicks);
    }
  }

  /**
   * @param i  
   * @param edge0 
   * @param edge1 
   * @param d 
   * @param drawTicks 
   */
  protected void renderCageLine(int i, int edge0, int edge1, int d, boolean drawTicks) {
    P3d p1 = p3Screens[edge0];
    P3d p2 = p3Screens[edge1];
    renderLine(p1, p2, d, drawTicks);
  }

  protected void setBSPeriod() {}

  protected boolean shiftA, shiftB, shiftC, shifting;
  
  protected void setShifts() {
    shiftA = (nDims == 3 && periodicity == 0x4); // rod
    shiftB = shiftA || (nDims == 2 && periodicity == 0x1); // rod or frieze
    shiftC = (nDims == 3 && periodicity == 0x3); // layer
    shifting = (shiftA || shiftB || shiftC);
  }
}

