/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-01-06 06:26:15 -0600 (Wed, 06 Jan 2016) $
 * $Revision: 20923 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.JmolModulationSet;
import org.jmol.modelset.Measurement;
import org.jmol.modelset.MeasurementPending;
import org.jmol.script.T;
import org.jmol.shape.Measures;
import org.jmol.util.C;
import org.jmol.util.GData;
import org.jmol.util.Point3fi;
import org.jmol.util.Vibration;

import javajs.util.A4;
import javajs.util.M3;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.P3i;



public class MeasuresRenderer extends LabelsRenderer {

  private boolean doJustify;
  private boolean modulating;
  private short mad0;
  
  /**
   * modulation points, which must be refreshed based on
   * phase of the vibration; keyed on atom index.
   * 
   */
  private Map<Integer, Point3fi> mpts;

  private Measurement m;
  private Point3fi[] p;
  private int count;

  private A4 aaT;
  private M3 matrixT;
  
  @Override
  protected void initRenderer() {
    mpts = new Hashtable<Integer, Point3fi>();
    p = new Point3fi[4];
  }
  
  @Override
  protected boolean render() {
    if (!g3d.checkTranslucent(false))
      return false;
    if (atomPt == null)
      atomPt = new Point3fi();
    Measures measures = (Measures) shape;
    if (measures.ms != ms) {
      System.out.println("!measure wrong modelset!");
      measures.clear();
      return false;    
    }
    doJustify = vwr.getBoolean(T.justifymeasurements);
    modulating = ms.bsModulated != null; 
    // note that this COULD be screen pixels if <= 20. 
    imageFontScaling = vwr.imageFontScaling;
    mad0 = measures.mad;
    font3d =vwr.gdata.getFont3DScaled(measures.font3d, imageFontScaling);
    m = measures.mPending;
    if (!isExport && m != null && (count = m.count)!= 0)
      renderPendingMeasurement();
    if (!vwr.getBoolean(T.showmeasurements))
      return false;
    boolean showMeasurementLabels = vwr.getBoolean(T.measurementlabels);
    measures.setVisibilityInfo();
    for (int i = measures.measurementCount; --i >= 0;) {
      m = measures.measurements.get(i);
      if (!m.isVisible || !m.isValid || (count = m.count) == 1 && m.traceX == Integer.MIN_VALUE)
        continue;
      getPoints();
      colix = m.colix;
      if (colix == 0)
        colix = measures.colix;
      if (colix == 0)
        colix = vwr.cm.colixBackgroundContrast;
      labelColix = m.labelColix;
      if (labelColix == 0)
        labelColix = vwr.cm.colixBackgroundContrast;
      else if (labelColix == -1)
        labelColix = colix;
      g3d.setC(colix);
      colixA = colixB = colix;
      renderMeasurement(showMeasurementLabels);
      //checkAtoms("m3");
    }
    return false;
  }

  private void getPoints() {
    for (int j = count; --j >= 0;) {
      int i = m.getAtomIndex(j + 1);
      Point3fi pt = (i >= 0 && modulating ? getModAtom(i) : m.getAtom(j + 1));
      if (pt.sD < 0) {
        tm.transformPtScr(pt, pt0i);
        pt.sX = pt0i.x;
        pt.sY = pt0i.y;
        pt.sZ = pt0i.z;
      }
      p[j] = pt;
    }
    if (modulating)
      m.refresh(p);
  }

  private Point3fi getModAtom(int i) {
    Integer ii = Integer.valueOf(i);
    Point3fi pt = mpts.get(ii);
    if (pt != null)
      ii = null;
    JmolModulationSet v = ms.getModulation(i);
    if (v == null) {
      pt = ms.at[i];
    } else {
      if (pt == null)
        pt = new Point3fi();
      pt.setT(ms.at[i]);
      if (vwr.tm.vibrationOn)
        vwr.tm.getVibrationPoint((Vibration) v, pt, Float.NaN);
      pt.sD = -1;
    }
    if (ii != null)
      mpts.put(ii, pt);
    return pt;
  }
 
  private void renderMeasurement(boolean renderLabel) {
    String s = (renderLabel ? m.getString() : null);
    if (s != null) {
      if (s.length() == 0) {
        s = null;
      } else if (m.text != null) {
        m.text.setText(s);
        m.text.colix = labelColix;
      }
    }
    if (m.mad == 0) {
      dotsOrDashes = false;
      mad = mad0;
    } else {
      mad = (short) m.mad;
      //dashDots = hDashes;
      dotsOrDashes = true;
      dashDots = (mad < 0 ? null : ndots);
    }
    switch (count) {
    case 1:
      drawLine(p[0].sX, p[0].sY, p[0].sZ, m.traceX, m.traceY,
          p[0].sZ, mad);
      break;
    case 2:
      renderDistance(s, p[0], p[1]);
      break;
    case 3:
      renderAngle(s, p[0], p[1], p[2]);
      break;
    case 4:
      renderTorsion(s, p[0], p[1], p[2], p[3]);
      break;
    }
    p[0] = p[1] = p[2] = p[3] = null;
  }

  void renderDistance(String s, Point3fi a, Point3fi b) {
   if ((tickInfo = m.tickInfo) != null) {
      drawLine(a.sX, a.sY, a.sZ, b.sX,
          b.sY, b.sZ, mad);
      tickA = a;
      tickB = b;
      if (tickAs == null) {
        tickAs = new P3();
        tickBs = new P3();
      }
      tickAs.set(a.sX, a.sY, a.sZ);
      tickBs.set(b.sX, b.sY, b.sZ);
      // TODO: z-value error: ONLY APPROXIMATE
      drawTicks(mad, s != null);
      return;
    }
    int zA = a.sZ - a.sD - 10;
    int zB = b.sZ - b.sD - 10;
    int radius = drawLine(a.sX, a.sY, zA, b.sX,
        b.sY, zB, mad);
    if (s == null)
      return;
    if (mad > 0)
      radius <<= 1;
    int z = (zA + zB) / 2;
    if (z < 1)
      z = 1;
    int x = (a.sX + b.sX) / 2;
    int y = (a.sY + b.sY) / 2;
    if (m.text == null) {
      g3d.setC(labelColix);
      drawString(x, y, z, radius, doJustify
          && (x - a.sX) * (y - a.sY) > 0, false, false,
          (doJustify ? 0 : Integer.MAX_VALUE), s);
    } else {
      atomPt.ave(a, b);
      atomPt.sX = (a.sX + b.sX) / 2;
      atomPt.sY = (a.sY + b.sY) / 2;
      renderLabelOrMeasure(m.text, s);
    }
  }
                          
  private void renderAngle(String s, Point3fi a, Point3fi b, Point3fi c) {
    int zOffset = b.sD + 10;
    int zA = a.sZ - a.sD - 10;
    int zB = b.sZ - zOffset;
    int zC = c.sZ - c.sD - 10;
    int radius = drawLine(a.sX, a.sY, zA, b.sX,
        b.sY, zB, mad);
    radius += drawLine(b.sX, b.sY, zB, c.sX,
        c.sY, zC, mad);
    if (s == null)
      return;
    radius = (radius + 1) / 2;
    if (m.value > 175) {
      if (m.text == null) {
        int offset = (int) Math.floor(5 * imageFontScaling);
        g3d.setC(labelColix);
        drawString(b.sX + offset, b.sY - offset, zB, radius,
            false, false, false, (doJustify ? 0 : Integer.MAX_VALUE), s);
      } else {
        atomPt.setT(b);
        renderLabelOrMeasure(m.text, s);
      }
      return;
    }
    if (m.isTainted()) {
      float radians = Measure.computeAngle(p[0], p[1], p[2],
          vectorT2, vectorT3, false);
      vectorT.cross(vectorT2, vectorT3);
      m.renderAxis = A4.new4(vectorT.x, vectorT.y, vectorT.z, radians);
      vectorT2.normalize();
      vectorT2.scale(0.5f);
      m.renderArc = P3.newP(vectorT2);
    }
    if (aaT == null) {
      aaT = new A4();
      matrixT = new M3();
    }
    int dotCount = (int) Math.floor((m.renderAxis.angle / (2 * Math.PI)) * 64);
    float stepAngle = m.renderAxis.angle / dotCount;
    aaT.setAA(m.renderAxis);
    int iMid = dotCount / 2;
    for (int i = dotCount; --i >= 0;) {
      aaT.angle = i * stepAngle;
      pointT.setT(m.renderArc);
      matrixT.setAA(aaT).rotate(pointT);
      pointT.add(b);
      // NOTE! Point3i screen is just a pointer 
      //  to tm.transformManager.point3iScreenTemp
      P3i p3i = tm.transformPt(pointT);
      int zArc = p3i.z - zOffset;
      if (zArc < 0)
        zArc = 0;
      g3d.drawPixel(p3i.x, p3i.y, zArc);
      if (i != iMid)
        continue;
      pointT.setT(m.renderArc);
      pointT.scale(1.1f);
      // next line modifies Point3i point3iScreenTemp
      matrixT.rotate(pointT);
      pointT.add(b);
      tm.transformPt(pointT);
      int zLabel = p3i.z - zOffset;
      if (m.text == null) {
        g3d.setC(labelColix);
        drawString(p3i.x, p3i.y, zLabel, radius, p3i.x < b.sX, false,
            false, (doJustify ? b.sY : Integer.MAX_VALUE), s);
      } else {
        atomPt.setT(pointT);
        renderLabelOrMeasure(m.text, s);
      }
    }
  }

  private void renderTorsion(String s, Point3fi a, Point3fi b, Point3fi c, Point3fi d) {
    int zA = a.sZ - a.sD - 10;
    int zB = b.sZ - b.sD - 10;
    int zC = c.sZ - c.sD - 10;
    int zD = d.sZ - d.sD - 10;
    int radius = drawLine(a.sX, a.sY, zA, b.sX,
        b.sY, zB, mad);
    radius += drawLine(b.sX, b.sY, zB, c.sX,
        c.sY, zC, mad);
    radius += drawLine(c.sX, c.sY, zC, d.sX,
        d.sY, zD, mad);
    if (s == null)
      return;
    int zLabel = (zA + zB + zC + zD) / 4;
    radius /= 3;
    if (m.text == null) {
      g3d.setC(labelColix);
      drawString((a.sX + b.sX + c.sX + d.sX) / 4,
          (a.sY + b.sY + c.sY + d.sY) / 4, zLabel, radius, false, false, false,
          (doJustify ? 0 : Integer.MAX_VALUE), s);
    } else {
      atomPt.add2(a, b);
      atomPt.add(c);
      atomPt.add(d);
      atomPt.scale(0.25f);
      renderLabelOrMeasure(m.text, s);
    }
  }

  private void renderPendingMeasurement() {
    try {
      getPoints();
    } catch (Exception e) {
      ((Measures) shape).mPending = null;
      return;
    }
    boolean renderLabel = (m.traceX == Integer.MIN_VALUE);
    g3d.setC(labelColix = (renderLabel ? vwr.cm.colixRubberband
        : count == 2 ? C.MAGENTA : C.GOLD));
    if (((MeasurementPending) m).haveTarget) {
      renderMeasurement(renderLabel);
      return;
    }    
    Point3fi atomLast = p[count - 1];
    if (count > 1)
      renderMeasurement(false);
    int lastZ = atomLast.sZ - atomLast.sD - 10;
    int x = vwr.getCursorX();
    int y = vwr.getCursorY();
    if (g3d.isAntialiased()) {
      x <<= 1;
      y <<= 1;
    }
    drawLine(atomLast.sX, atomLast.sY, lastZ, x, y, lastZ, mad);
  }  
 
  //TODO: I think the 20 here is the cutoff for pixels -- check this
  @Override
  protected int drawLine(int x1, int y1, int z1, int x2, int y2, int z2,
                         int mad) {
    // small numbers refer to pixels already? 
    int diameter = (int) (mad >= 20 && exportType != GData.EXPORT_CARTESIAN ?
      vwr.tm.scaleToScreen((z1 + z2) / 2, mad) : mad);
    if (dotsOrDashes && (dashDots == null || dashDots == ndots))
      width = diameter;
    System.out.println("measuresrend " + x1 + " " + y1 + " " + x2 + " " + y2);
    return drawLine2(x1, y1, z1, x2, y2, z2, diameter);
  }
}
