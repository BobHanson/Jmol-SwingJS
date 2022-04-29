/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-10-08 12:28:44 -0500 (Sat, 08 Oct 2016) $
 * $Revision: 21258 $
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

import org.jmol.api.SymmetryInterface;
import org.jmol.script.T;
import org.jmol.shape.Axes;
import org.jmol.util.GData;
import org.jmol.viewer.StateManager;

import javajs.util.P3d;


public class AxesRenderer extends CageRenderer {

  private final static String[] axisLabels = { "+X", "+Y", "+Z", null, null, null, 
                                  "a", "b", "c", 
                                  "X", "Y", "Z", null, null, null,
                                  "X", null, "Z", null, "(Y)", null};

  private final P3d originScreen = new P3d();
  
  private short[] colixes = new short[3];

  private P3d pt000;

  private final static String[] axesTypes = {"a", "b", "c", "x", "y", "z"};

  @Override
  protected void initRenderer() {
    endcap = GData.ENDCAPS_FLAT; 
    draw000 = false;
  }

  @Override
  protected boolean render() {
    Axes axes = (Axes) shape;
    int mad10 = vwr.getObjectMad10(StateManager.OBJ_AXIS1);
    boolean isXY = (axes.axisXY.z != 0);
    // no translucent axes
    if (mad10 == 0 || !g3d.checkTranslucent(false))
      return false;
    if (isXY ? exportType == GData.EXPORT_CARTESIAN
        : tm.isNavigating() && vwr.getBoolean(T.navigationperiodic))
      return false;
    // includes check here for background model present
    int modelIndex = vwr.am.cmi;
    if (ms.isJmolDataFrameForModel(modelIndex)
        && !ms.getJmolFrameType(modelIndex).equals("plot data"))
      return false;
    boolean isUnitCell = (vwr.g.axesMode == T.axesunitcell);
    SymmetryInterface unitcell = null;
    if (isUnitCell && (unitcell = vwr.getCurrentUnitCell()) == null && modelIndex < 0)
      return false;
    imageFontScaling = vwr.imageFontScaling;
    if (vwr.areAxesTainted())
      axes.reinitShape();
    font3d = vwr.gdata.getFont3DScaled(axes.font3d, imageFontScaling);
    isUnitCell &= (ms.unitCells != null);
    String axisType = (isUnitCell ? axes.axisType : null);
    boolean isabcxyz = (isXY && isUnitCell && axes.axes2 != null);
    isPolymer = isUnitCell && unitcell.isPolymer();
    isSlab = isUnitCell && unitcell.isSlab();

    double scale = axes.scale;
    if (isabcxyz) {
      // both abc and xyz
      render1(axes, mad10, false, axisType, isUnitCell, 2, null);
      vwr.setBooleanProperty("axesmolecular", true);
      axes.initShape();
      render1(axes, mad10, true, null, false, scale, axes.axes2);
      vwr.setBooleanProperty("axesunitcell", true);
    } else {
      render1(axes, mad10, isXY, axisType, isUnitCell, scale, null);
    }
    return true;
  }
  
  private final P3d ptTemp = new P3d();
  private void render1(Axes axes,int mad10, boolean isXY,
                       String axisType, boolean isUnitCell, double scale, String labels2) {
    boolean isDataFrame = vwr.isJmolDataFrame();
    pt000 = (isDataFrame ? pt0 : axes.originPoint);
    int nPoints = 6;
    int labelPtr = 0;
    if (isUnitCell) {
      nPoints = 3;
      labelPtr = 6;
    } else if (isXY) {
      nPoints = 3;
      labelPtr = 9;
    } else if (vwr.g.axesMode == T.axeswindow) {
      nPoints = 6;
      labelPtr = (vwr.getBoolean(T.axesorientationrasmol) ? 15 : 9);
    }
    if (axes.labels != null) {
      if (nPoints != 3)
        nPoints = (axes.labels.length < 6 ? 3 : 6);
      labelPtr = -1;
    }
    int slab = vwr.gdata.slab;
    int diameter = mad10;
    boolean drawTicks = false;
    ptTemp.setT(originScreen);
    boolean checkAxisType = (labels2 == null && axisType != null && (isXY || vwr.getDouble(T.axesoffset) != 0 || axes.fixedOrigin != null));
    if (isXY) {
      if (mad10 >= 20) {
        // width given in angstroms as mAng.
        // max out at 500
        diameter = (mad10 > 500 ? 3 : mad10 / 200);
        if (diameter == 0)
          diameter = 2;
      }
      if (g3d.isAntialiased())
        diameter += diameter;
      g3d.setSlab(0);
      ptTemp.setT(axes.axisXY);
      pt0i.setT(tm.transformPt2D(ptTemp));
      if (ptTemp.x < 0) {
        // window origin
        int offx = (int) ptTemp.x;
        int offy = (int) ptTemp.x;
        // offset is from {0 0 0}
        pointT.setT(pt000);
        for (int i = 0; i < 3; i++)
          pointT.add(axes.getAxisPoint(i, false, ptTemp)); 
        pt0i.setT(tm.transformPt(pt000));
        pt2i.scaleAdd(-1, pt0i, tm.transformPt(pointT));
        if (pt2i.x < 0)
          offx = -offx;
        if (pt2i.y < 0)
          offy = -offy;
        pt0i.x += offx;
        pt0i.y += offy;
      }
      ptTemp.set(pt0i.x, pt0i.y, pt0i.z);
      double zoomDimension = vwr.getScreenDim();
      double scaleFactor = zoomDimension / 10d * scale;
      if (g3d.isAntialiased())
        scaleFactor *= 2;
      if (isUnitCell && isXY)
        scaleFactor /= 2;
      for (int i = 0; i < 3; i++) {
        P3d pt = p3Screens[i];
        tm.rotatePoint(axes.getAxisPoint(i, false, pointT), pt);
        pt.z *= -1;
        pt.scaleAdd2(scaleFactor, pt, ptTemp);
      }
    } else {
      // !isXY
      drawTicks = (axes.tickInfos != null);
      if (drawTicks) {
        checkTickTemps();
        tickA.setT(pt000);
      }
      tm.transformPtNoClip(pt000, ptTemp);
      diameter = getDiameter((int) ptTemp.z, mad10);
      for (int i = nPoints; --i >= 0;)
        tm.transformPtNoClip(axes.getAxisPoint(i, !isDataFrame, pointT), p3Screens[i]);
    }
    double xCenter = ptTemp.x;
    double yCenter = ptTemp.y;
    colixes[0] = vwr.getObjectColix(StateManager.OBJ_AXIS1);
    colixes[1] = vwr.getObjectColix(StateManager.OBJ_AXIS2);
    colixes[2] = vwr.getObjectColix(StateManager.OBJ_AXIS3);
    boolean showOrigin = (!isXY && nPoints == 3 && (scale == 2 || isUnitCell));
    for (int i = nPoints; --i >= 0;) {
      if (labels2 != null && i >= labels2.length()
          || checkAxisType && !axisType.contains(axesTypes[i])
          || exportType != GData.EXPORT_CARTESIAN && 
          (Math.abs(xCenter - p3Screens[i].x)
              + Math.abs(yCenter - p3Screens[i].y) <= 2)
          && (!(showOrigin = false)) // setting showOrigin here
      ) {
        continue;
      }
      colix = colixes[i % 3];
      g3d.setC(colix);
      String label = (labels2 != null ? labels2.substring(i, i + 1) 
          : axes.labels == null ? axisLabels[i + labelPtr]
          : i < axes.labels.length ? axes.labels[i] : null);
      if (label != null && label.length() > 0)
        renderLabel(label, p3Screens[i].x, p3Screens[i].y, p3Screens[i].z,
            xCenter, yCenter);
      if (drawTicks) {
        tickInfo = axes.tickInfos[(i % 3) + 1];
        if (tickInfo == null)
          tickInfo = axes.tickInfos[0];
        if (tickInfo != null) {
          tickB.setT(axes.getAxisPoint(i, isDataFrame || isUnitCell, pointT));
          tickInfo.first = 0;
          tickInfo.signFactor = (i % 6 >= 3 ? -1 : 1);
        }
      }      
      int d = (isSlab && i == 2 || isPolymer && i > 0 ? -4 : diameter);
      renderLine(ptTemp, p3Screens[i], d , drawTicks && tickInfo != null);
    }
    if (showOrigin) { // a b c [orig]
      String label0 = (axes.labels == null || axes.labels.length == 3
          || axes.labels[3] == null ? "0" : axes.labels[3]);
      if (label0 != null && label0.length() != 0) {
        colix = vwr.cm.colixBackgroundContrast;
        g3d.setC(colix);
        renderLabel(label0, xCenter, yCenter, ptTemp.z, xCenter, yCenter);
      }
    }
    if (isXY)
      g3d.setSlab(slab);
  }

  private void renderLabel(String str, double x, double y, double z, double xCenter, double yCenter) {
    int strAscent = font3d.getAscent();
    int strWidth = font3d.stringWidth(str);
    double dx = x - xCenter;
    double dy = y - yCenter;
    if ((dx != 0 || dy != 0)) {
      double dist = Math.sqrt(dx * dx + dy * dy);
      dx = (strWidth * 0.75d * dx / dist);
      dy = (strAscent * 0.75d * dy / dist);
      x += dx;
      y += dy;
    }
    double xStrBaseline = Math.floor(x - strWidth / 2d);
    double yStrBaseline = Math.floor(y + strAscent / 2d);
    g3d.drawString(str, font3d, (int) xStrBaseline, (int) yStrBaseline, (int) z, (int) z, (short) 0);
  }
}
