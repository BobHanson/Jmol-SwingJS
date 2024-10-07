/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-02-25 11:44:18 -0600 (Sat, 25 Feb 2006) $
 * $Revision: 4528 $
 *
 * Copyright (C) 2005  Miguel, Jmol Development
 *
 * Contact: jmol-developers@lists.sf.net, jmol-developers@lists.sourceforge.net
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
package org.jmol.renderspecial;




import javajs.util.BS;
import org.jmol.render.MeshRenderer;
import org.jmol.script.T;
import org.jmol.shape.Mesh;
import org.jmol.shapespecial.Draw;
import org.jmol.shapespecial.DrawMesh;
import org.jmol.shapespecial.Draw.EnumDrawType;
import org.jmol.util.C;
import org.jmol.util.Font;
import org.jmol.util.GData;
import javajs.util.Lst;

import javajs.util.A4d;
import javajs.util.M3d;
import javajs.util.MeasureD;
import javajs.util.P3d;
import javajs.util.P3i;
import javajs.util.T3d;
import javajs.util.V3d;
import org.jmol.viewer.ActionManager;
import org.jmol.viewer.JC;

public class DrawRenderer extends MeshRenderer {

  private EnumDrawType drawType;
  protected DrawMesh dmesh;

  private P3d[] controlHermites;
  protected P3d pt0 = new P3d();
  protected P3d pt1 = new P3d();
  protected P3d pt2 = new P3d();
  protected final V3d vTemp = new V3d();
  protected final V3d vTemp2 = new V3d();

  @Override
  protected boolean render() {
    /*
     * Each drawn object, draw.meshes[i], may consist of several polygons, one
     * for each MODEL FRAME. Or, it may be "fixed" and only contain one single
     * polygon.
     * 
     */
    needTranslucent = false;
    imageFontScaling = vwr.imageFontScaling;
    Draw draw = (Draw) shape;
    //isPrecision = true;//vwr.tm.perspectiveDepth;
    for (int i = draw.meshCount; --i >= 0;) {
      Mesh mesh = dmesh = (DrawMesh) draw.meshes[i];
      if (mesh == null) {
        return false;
      }
      if (dmesh.thisModelOnly && vwr.am.cmi < 0)
        continue;
      if (mesh.connectedAtoms != null) {
        if (mesh.connectedAtoms[0] < 0)
          continue;
        // bond-bond [ a b   c d  ]
        // bond-atom [ a b   c -1 ]
        // atom-bond [ a -1  c d  ]
        // atom-atom [ a -1  c -1 ]

        mesh.vs = new P3d[4];
        mesh.vc = 4;
        int[] c = mesh.connectedAtoms;
        for (int j = 0; j < 4; j++)
          mesh.vs[j] = (c[j] < 0 ? mesh.vs[j - 1] : vwr.ms.at[c[j]]);
        mesh.recalcAltVertices = true;
      }
      if (renderMesh2(mesh))
        renderInfo();
      if (!isExport 
          && mesh.visibilityFlags != 0
          && (vwr.getDrawHover() || vwr.getPickingMode() == ActionManager.PICKING_DRAW)) {
        if (!g3d.setC(C.getColixTranslucent3(C.GOLD, true, 0.5d)))
          needTranslucent = true;
        else
          renderHandles();
      }
    }
    return needTranslucent;
  }

  @Override
  protected boolean isPolygonDisplayable(int i) {
    return Draw.isPolygonDisplayable(dmesh, i)
        && (dmesh.modelFlags == null || dmesh.bsMeshesVisible.get(i));
  }

  @Override
  protected void render2(boolean isExport) {
    drawType = dmesh.drawType;
    diameter = dmesh.diameter;
    width = dmesh.width;
    if (mesh.connectedAtoms != null)
      getConnectionPoints();
    if (mesh.lineData != null) {
      drawLineData(mesh.lineData);
      return;
    }
    int nPoints = vertexCount;
    boolean isArrow = drawType == EnumDrawType.ARROW;
    boolean isCurved = ((isArrow || drawType == EnumDrawType.CURVE
        || drawType == EnumDrawType.ARC) && vertexCount > 2);
    if (width > 0 && isCurved || isArrow) {
      pt1f.set(0, 0, 0);
      int n = (drawType == EnumDrawType.ARC ? 2 : vertexCount);
      for (int i = 0; i < n; i++)
        pt1f.add(vertices[i]);
      pt1f.scale(1d / n);
      tm.transformPtScr(pt1f, pt1i);
      diameter = (int) vwr.tm.scaleToScreen(pt1i.z,
          (int) Math.floor(width * 1000));
      if (diameter == 0)
        diameter = 1;
    }
    if (dmesh.haveXyPoints && drawXYPoints())
        return;
    int tension = 5;
    switch (drawType) {
    case CYLINDER:
      allowDashed = false;
      //$FALL-THROUGH$
    default:
      render2b(false);
      return;
    case CIRCULARPLANE:
      if (dmesh.scale > 0)
        width *= dmesh.scale;
      allowDashed = false; // width may be negative
      render2b(false);
      return;
    case CIRCLE:
      tm.transformPtScr(vertices[0], pt1i);
      if (diameter == 0 && width == 0)
        width = 1.0d;
      if (dmesh.scale > 0)
        width *= dmesh.scale;
      if (width > 0)
        diameter = (int) vwr.tm.scaleToScreen(pt1i.z,
            (int) Math.floor(width * 1000));
      if (diameter > 0 && (mesh.drawTriangles || mesh.fillTriangles)) {
        g3d.addRenderer(T.circle);
        g3d.drawFilledCircle(colix, mesh.fillTriangles ? colix : 0, diameter,
            pt1i.x, pt1i.y, pt1i.z);
      }
      return;
    case LINE_SEGMENT:
      for (int i = 0; i < nPoints - 1; i++)
        drawEdge(i, i + 1, true, vertices[i], vertices[i + 1], screens[i],
            screens[i + 1]);
      return;
    case CURVE:
      break;
    case ARC:
      //renderArrowHead(controlHermites[nHermites - 2], controlHermites[nHermites - 1], false);
      // 
      // {pt1} {pt2} {ptref} {nDegreesOffset, theta, fractionalOffset}
      T3d ptRef = (vertexCount > 2 ? vertices[2] : Draw.randomPoint());
      double nDegreesOffset = (vertexCount > 3 ? vertices[3].x : 0);
      double theta = (vertexCount > 3 ? vertices[3].y : 360);
      if (theta == 0)
        return;
      double fractionalOffset = (vertexCount > 3 ? vertices[3].z : 0);
      nPoints = setArc(vertices[0], vertices[1], ptRef, nDegreesOffset, theta,
          fractionalOffset, dmesh.scale);
      if (dmesh.isVector && !dmesh.noHead) {
        renderArrowWithHead(pt0, pt1, 0.3d, false, false, dmesh.isBarb);
        tm.transformPtScr(pt1f, screens[nPoints - 1]);
        tm.transformPtScrT3(pt1f, p3Screens[nPoints - 1]);
      }
      pt1f.setT(pt2);
      break;
    case ARROW:
      if (!isCurved) {
        renderArrowWithHead(vertices[0], vertices[1], 0, false, true, dmesh.isBarb);
        return;
      }
      int nHermites = 5;
      if (controlHermites == null || controlHermites.length < nHermites + 1) {
        controlHermites = new P3d[nHermites + 1];
      }
      GData.getHermiteList(tension, vertices[vertexCount - 3],
          vertices[vertexCount - 2], vertices[vertexCount - 1],
          vertices[vertexCount - 1], vertices[vertexCount - 1],
          controlHermites, 0, nHermites, true);
      renderArrowWithHead(controlHermites[nHermites - 2],
          controlHermites[nHermites - 1], 0, false, false, dmesh.isBarb);
      break;
    }
    // CURVE ARC ARROW only
    if (diameter == 0)
      diameter = 3;
    if (isCurved) {
      g3d.addRenderer(T.hermitelevel);
      for (int i = 0, i0 = 0; i < nPoints - 1; i++) {
        g3d.fillHermite(tension, diameter, diameter, diameter, p3Screens[i0],
            p3Screens[i], p3Screens[i + 1], p3Screens[i
                + (i == nPoints - 2 ? 1 : 2)]);
        i0 = i;
      }
    } else {
      render2b(false);
    }

  }

  private boolean drawXYPoints() {
    if (dmesh.isVector) {
      int ptXY = 0;
      // [x y] or [x,y] refers to an xy point on the screen
      // just a Point3f with z = Double.MAX_VALUE
      //  [x y %] or [x,y %] refers to an xy point on the screen
      // as a percent 
      // just a Point3f with z = -Double.MAX_VALUE
      for (int i = 0; i < 2; i++)
        if (vertices[i].z == Double.MAX_VALUE
            || vertices[i].z == -Double.MAX_VALUE)
          ptXY += i + 1;
      if (--ptXY < 2) {
        renderXyArrow(ptXY);
        return true;
      }
    } else if (drawType == Draw.EnumDrawType.POINT || drawType == Draw.EnumDrawType.MULTIPLE){
      renderXyPoint();
      return true;
    }
    return false;
  }

  private int setArc(T3d v1, T3d v2, T3d ptRef, double nDegreesOffset,
                       double theta, double fractionalOffset, double scale) {
    vTemp.sub2(v2, v1);
    // crossing point
    pt1f.scaleAdd2(fractionalOffset, vTemp, v1);
    // define rotational axis
    M3d mat = new M3d().setAA(A4d.newVA(vTemp,
        (nDegreesOffset * Math.PI / 180)));
    // vector to rotate
    vTemp2.sub2(ptRef,
        v1);
    vTemp2.cross(vTemp, vTemp2);
    vTemp2.cross(vTemp2, vTemp);
    vTemp2.normalize();
    vTemp2.scale(scale / 2);
    mat.rotate(vTemp2);
    //control points
    double degrees = theta / 5;
    while (Math.abs(degrees) > 5)
      degrees /= 2;
    int nPoints = (int) Math.round(theta / degrees) + 1;
    while (nPoints < 10) {
      degrees /= 2;
      nPoints = (int) Math.round(theta / degrees) + 1;
    }
    mat.setAA(A4d.newVA(vTemp, (degrees * Math.PI / 180)));
    screens = vwr.allocTempScreens(nPoints);
    p3Screens = vwr.allocTempPoints(nPoints);
    int iBase = nPoints - (dmesh.scale < 2 ? 3 : 3);
    for (int i = 0; i < nPoints; i++) {
      if (i == iBase)
        pt0.setT(pt1);
      pt1.scaleAdd2(1, vTemp2, pt1f);
      if (i == 0)
        pt2.setT(pt1);
      tm.transformPtScr(pt1, screens[i]);
      tm.transformPtScrT3(pt1, p3Screens[i]);
      mat.rotate(vTemp2);
    }
    return nPoints;
  }

  private void getConnectionPoints() {
    // now we screens and any adjustment to positions
    // we need to set the actual control points
    
    
    vertexCount = 3;
    double dmax = Double.MAX_VALUE;
    int i0 = 0;
    int j0 = 0;
    for (int i = 0; i < 2; i++)
      for (int j = 2; j < 4; j++) {
        double d = vertices[i].distance(vertices[j]);
        if (d < dmax) {
          dmax = d;
          i0 = i;
          j0 = j;
        }
      }
    pt0.ave(vertices[0], vertices[1]);
    pt2.ave(vertices[2], vertices[3]);
    pt1.ave(pt0, pt2);
    vertices[3] = P3d.newP(vertices[i0]);
    vertices[3].add(vertices[j0]);
    vertices[3].scale(0.5d);
    vertices[1] = P3d.newP(pt1); 
    vertices[0] = P3d.newP(pt0);
    vertices[2] = P3d.newP(pt2);

    for (int i = 0; i < 4; i++)
      tm.transformPtScr(vertices[i], screens[i]);

    double f = 4 * getArrowScale(); // bendiness
    double endoffset = 0.2d;
    double offsetside = (width == 0 ? 0.1d : width);
    
    pt0.set(screens[0].x, screens[0].y, screens[0].z);
    pt1.set(screens[1].x, screens[1].y, screens[1].z);
    pt2.set(screens[3].x, screens[3].y, screens[3].z);
    double dx = (screens[1].x - screens[0].x) * f;
    double dy = (screens[1].y - screens[0].y) * f;
    
    if (dmax == 0 || MeasureD.computeTorsion(pt2, pt0, P3d.new3(pt0.x, pt0.y, 10000d), pt1, false) > 0) {
      dx = -dx;
      dy = -dy;
    }
    pt2.set(dy, -dx, 0);
    pt1.add(pt2);
    tm.unTransformPoint(pt1, vertices[1]);
    pt2.scale(offsetside);
    vTemp.sub2(vertices[1], vertices[0]);
    vTemp.scale(endoffset); 
    vertices[0].add(vTemp);
    vTemp.sub2(vertices[1], vertices[2]);
    vTemp.scale(endoffset); 
    vertices[2].add(vTemp);
    for (int i = 0; i < 3; i++) {
      tm.transformPtScr(vertices[i], screens[i]);
      if (offsetside != 0) {
        screens[i].x += Math.round(pt2.x);
        screens[i].y += Math.round(pt2.y);
        pt1.set(screens[i].x, screens[i].y, screens[i].z);
        tm.unTransformPoint(pt1 , vertices[i]);
      }
    }
  }

  private void drawLineData(Lst<P3d[]> lineData) {
    if (diameter == 0)
      diameter = 3;
    for (int i = lineData.size(); --i >= 0;) {
      P3d[] pts = lineData.get(i);
      tm.transformPtScr(pts[0], pt1i);
      tm.transformPtScr(pts[1], pt2i);
      drawEdge(-1, -2, true, pts[0], pts[1], pt1i, pt2i);
    }
  }

  private void renderXyPoint() {
    // new in Jmol 14.5
    int f = (g3d.isAntialiased() ? 2 : 1);
    pt0.setT(vertices[0]);
    if (diameter == 0)
      diameter = (int) width;
    if (pt0.z == -Double.MAX_VALUE) {
      pt0.x *= vwr.tm.width / 100d;
      pt0.y *= vwr.tm.height / 100d;
      diameter = (int) (diameter * vwr.getScreenDim() / 100d);
    }
    diameter *= f;
    pt1i.set((int) (pt0.x), (int) (vwr.tm.height - pt0.y), (int) vwr.tm.cameraDistance);
    g3d.fillSphereI(diameter, pt1i);
  }

  private void renderXyArrow(int ptXY) {
    // only 0 or 1 here; so ptXYZ is 1 or 0
    int ptXYZ = 1 - ptXY;
    P3d[] arrowPt = new P3d[2];
    arrowPt[ptXYZ] = pt1;
    arrowPt[ptXY] = pt0;
    // set up (0,0,0) to ptXYZ in real and screen coordinates
    pt0.set(screens[ptXY].x, screens[ptXY].y, screens[ptXY].z);
    tm.rotatePoint(vertices[ptXYZ], pt1);
    pt1.z *= -1;
    double zoomDimension = vwr.getScreenDim();
    double scaleFactor = zoomDimension / 20d;
    pt1.scaleAdd2(dmesh.scale * scaleFactor, pt1, pt0);
    if (diameter == 0)
      diameter = 1;
    if (diameter < 0)
      g3d.drawDashedLineBits(8, 4, pt0, pt1);
    else
      g3d.fillCylinderBits(GData.ENDCAPS_FLAT, diameter, pt0, pt1);
    renderArrowWithHead(pt0, pt1, 0, true, false, false);
  }

  private final P3d pt0d = new P3d();
  protected P3i pt0i = new P3i();
  private P3d s0d;
  private P3d s1f;
  private P3d s2f;

  private void renderArrowWithHead(T3d pt1, T3d pt2, double factor2,
                               boolean isTransformed, boolean withShaft,
                               boolean isBarb) {
    if (dmesh.noHead && !withShaft)
      return;
    if (s0d == null) {
      s0d = new P3d();
      s1f = new P3d();
      s2f = new P3d();
    }
    double fScale = getArrowScale();
    if (isTransformed)
      fScale *= 40;
    if (factor2 > 0)
      fScale *= factor2;

    pt0d.setT(pt1);
    pt2f.setT(pt2);
    double d = pt0d.distance(pt2f);
    if (d == 0)
      return;
    double headWidth = (width > 0 && !dmesh.noHead ? width * 3 : 0);
    vTemp.sub2(pt2f, pt0d); // total length
    vTemp.normalize(); // unit length
    
    vTemp.scale(Math.min(headWidth == 0 ? fScale : headWidth*1.5, fScale) / 5); // 1/10
    if (!withShaft)
      pt2f.add(vTemp);
    vTemp.scale(5);
    double len = vTemp.length();
    if (len > headWidth * 1.5)
      vTemp.scale(headWidth * 1.5/len);
    ///  pt0====pt1>>>>pt2
    pt1f.sub2(pt2f, vTemp);
    if (isTransformed) {
      s1f.setT(pt1f);
      s2f.setT(pt2f);
    } else {
      tm.transformPtScrT3(pt2f, s2f);
      tm.transformPtScrT3(pt1f, s1f);
      tm.transformPtScrT3(pt0d, s0d);
    }
    if (s2f.z == 1 || s1f.z == 1) //slabbed
      return;
    int headDiameter;
    if (diameter > 0) {
      headDiameter = diameter * 3;
    } else {
      vTemp.set(s2f.x - s1f.x, s2f.y - s1f.y, s2f.z - s1f.z);
      headDiameter = (int) Math.round(vTemp.length() * .5d);
      diameter = headDiameter / 5;
    }
    if (diameter < 1)
      diameter = 1;
    if (headDiameter > 2 && !dmesh.noHead)
      g3d.fillConeScreen3f(GData.ENDCAPS_FLAT, headDiameter, s1f, s2f,
          isBarb);
    if (withShaft)
      g3d.fillCylinderScreen3I(GData.ENDCAPS_FLAT, diameter, s0d, s1f, null, null, 0);
  }

  private double getArrowScale() {
    double fScale = (dmesh.isScaleSet ? dmesh.scale : 0);
    if (fScale == 0)
      fScale = vwr.getDouble(T.defaultdrawarrowscale) * (dmesh.connectedAtoms == null ? 1d : 0.5d);
    if (fScale <= 0)
      fScale = 0.5d;
    return fScale;
  }

  private final BS bsHandles = new BS();
  private boolean haveNotifiedHandles;
  
  private void renderHandles() {
    int diameter = (int) Math.round(10 * imageFontScaling);
    switch (drawType) {
    case NONE:
      return;
    default:
      short colixFill = C.getColixTranslucent3(C.GOLD, true,
          0.5d);
      bsHandles.clearAll();
      g3d.addRenderer(T.circle);
      for (int i = dmesh.pc; --i >= 0;) {
        if (!isPolygonDisplayable(i))
          continue;
        int[] vertexIndexes = dmesh.pis[i];
        if (vertexIndexes == null)
          continue;
        for (int j = (dmesh.isDrawPolygon ? 3 : vertexIndexes.length - 1); --j >= 0;) {
          int k = vertexIndexes[j];
          if (bsHandles.get(k))
            continue;
          bsHandles.set(k);
          if (k >= screens.length) {
            if (!haveNotifiedHandles) {
              System.out.println("DrawRender handles k >= screens.length " + k + " ");
              haveNotifiedHandles = true;
            }
          } else {
            g3d.drawFilledCircle(C.GOLD, colixFill, diameter,
              screens[k].x, screens[k].y, screens[k].z);
          }
        }
      }
      break;
    }
  }

  private void renderInfo() {
    if (isExport || mesh.title == null || vwr.getDrawHover()
        || !g3d.setC(vwr.cm.colixBackgroundContrast))
      return;
    Font f0 = (Font) vwr.shm.getShapePropertyIndex(JC.SHAPE_DRAW, "font", -1);
    Font f = f0;
    int lastFID = -1;
    boolean haveFont = false;
    for (int i = dmesh.pc; --i >= 0;)
      if (isPolygonDisplayable(i)) {
        //just the first line of the title -- nothing fancy here.
        if (!haveFont || dmesh.fontID != lastFID) {
          f = (Font) vwr.shm.getShapePropertyIndex(JC.SHAPE_DRAW, "font", i);
          if (f == null)
            f = f0;
          lastFID = f.fid;
          vwr.gdata.setFont(imageFontScaling == 1 ? f : vwr.gdata.getFont3DFSS(f.fontFace, f.fontStyle, f.fontSize * imageFontScaling));
          haveFont = true;
        }
        String s = mesh.title[i < mesh.title.length ? i : mesh.title.length - 1];
        int pt = 0;
        if (s.length() > 1 && s.charAt(0) == '>') {
          pt = dmesh.pis[i].length - 1;
          s = s.substring(1);
          if (drawType == EnumDrawType.ARC)
            pt1f.setT(pt2f);
        }
        if (drawType != EnumDrawType.ARC)
          pt1f.setT(vertices[dmesh.pis[i][pt]]);
        tm.transformPtScr(pt1f, pt1i);
        int offset = (int) Math.round(5 * imageFontScaling);
        if (dmesh.titleColor != null)
          vwr.gdata.setColor(dmesh.titleColor.intValue());
        g3d.drawString(s, null, pt1i.x + offset, pt1i.y - offset, pt1i.z,
            pt1i.z, (short) 0);
        break;
      }
  }

}
