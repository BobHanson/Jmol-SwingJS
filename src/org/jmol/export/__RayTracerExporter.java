/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-05-18 15:41:42 -0500 (Fri, 18 May 2007) $
 * $Revision: 7752 $

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

package org.jmol.export;


import java.util.Map;

import javajs.util.M3;
import javajs.util.M4;
import javajs.util.P3;
import javajs.util.T3;

import org.jmol.modelset.Atom;
import org.jmol.util.GData;
import org.jmol.viewer.Viewer;

/*
 * for PovRay and Tachyon Exporters, which use screen coordinates
 * 
 * 
 */

abstract class __RayTracerExporter extends ___Exporter {

  protected boolean isSlabEnabled;
  protected int minScreenDimension;
  protected boolean wasPerspective;
  
  public __RayTracerExporter() {
    exportType = GData.EXPORT_RAYTRACER;
    lineWidthMad = 2;
  }

  @Override
  protected boolean initOutput(Viewer vwr, double privateKey, GData g3d,
                               Map<String, Object> params) {
    wasPerspective = vwr.tm.perspectiveDepth;
    if (super.initOutput(vwr, privateKey, g3d, params)) {
      vwr.tm.perspectiveDepth = false;
      if (wasPerspective)
        vwr.shm.finalizeAtoms(null, false);
      return true;
    }
    return false; 
  }
    
  @Override
  protected String finalizeOutput2() {
    vwr.tm.perspectiveDepth = wasPerspective;
    return super.finalizeOutput2();    
  }
  
  @Override
  protected void outputVertex(T3 pt, T3 offset) {
    setTempVertex(pt, offset, tempP1);
    tm.transformPt3f(tempP1, tempP1);
    output(tempP1);
  }

  abstract protected void outputCircle(int x, int y, int z, float radius, short colix,
                                       boolean doFill);

  abstract protected void outputCylinder(P3 screenA, P3 screenB, float radius,
                                         short colix, boolean withCaps);
             
  abstract protected void outputCylinderConical(P3 screenA,
                                                P3 screenB, float radius1,
                                                float radius2, short colix);

  abstract protected void outputEllipsoid(P3 center, float radius, double[] coef, short colix);
  
  abstract protected void outputSphere(float x, float y, float z, float radius,
                                    short colix);
  
  abstract protected void outputTextPixel(int x, int y, int z, int argb);
  
  abstract protected void outputTriangle(T3 ptA, T3 ptB, T3 ptC, short colix);

  abstract protected void outputCone(P3 screenBase, P3 screenTip, float radius,
                                     short colix, boolean isBarb);

  protected P3 getScreenNormal(T3 pt, T3 normal, float factor) {
    if (Float.isNaN(normal.x)) {
      tempP3.set(0, 0, 0);
      return tempP3;
    }
    tempP1.add2(pt, normal);
    tm.transformPt3f(pt, tempP2);
    tm.transformPt3f(tempP1, tempP3);
    tempP3.sub(tempP2);
    tempP3.scale(factor);
    return tempP3;
  }

  protected void initVars() {
    isSlabEnabled = tm.slabEnabled;
    minScreenDimension = Math.min(screenWidth, screenHeight);
  }

  // called by Export3D:

  @Override
  void drawAtom(Atom atom, float radius) {
    outputSphere(atom.sX, atom.sY, atom.sZ, atom.sD / 2f, atom.colixAtom);
  }

  @Override
  void drawCircle(int x, int y, int z,
                         int diameter, short colix, boolean doFill) {
    //draw circle
    float radius = diameter / 2f;
    outputCircle(x, y, z, radius, colix, doFill);
  }

  @Override
  boolean drawEllipse(P3 ptAtom, P3 ptX, P3 ptY,
                      short colix, boolean doFill) {
    // IDTF only for now
    return false;
  }

  @Override
  void drawPixel(short colix, int x, int y, int z, int scale) {
    //measures, meshRibbon, dots
    outputSphere(x, y, z, 0.75f * scale, colix);
  }

  @Override
  void drawTextPixel(int argb, int x, int y, int z) {
    outputTextPixel(x, y, fixScreenZ(z), argb);
  }
    
  @Override
  void fillConeScreen(short colix, byte endcap, int screenDiameter, P3 screenBase,
                P3 screenTip, boolean isBarb) {
    outputCone(screenBase, screenTip, screenDiameter / 2f, colix, isBarb);
  }

  @Override
  void drawCylinder(P3 screenA, P3 screenB, short colix1,
                           short colix2, byte endcaps, int madBond,
                           int bondOrder) {
    // from drawBond and fillCylinder here
    if (colix1 == colix2) {
      fillConicalCylinder(screenA, screenB, madBond, colix1, endcaps);
    } else {
      tempV2.ave(screenB, screenA);
      tempP1.setT(tempV2);
      fillConicalCylinder(screenA, tempP1, madBond, colix1, endcaps);
      fillConicalCylinder(tempP1, screenB, madBond, colix2, endcaps);
    }
    if (endcaps != GData.ENDCAPS_SPHERICAL)
      return;
    
    float radius = vwr.tm.scaleToScreen((int) screenA.z, madBond) / 2f;
    if (radius <= 1)
      return;
    outputSphere(screenA.x, screenA.y, screenA.z, radius, colix1);
    radius = vwr.tm.scaleToScreen((int) screenB.z, madBond) / 2f;
    if (radius <= 1)
      return;
    outputSphere(screenB.x, screenB.y, screenB.z, radius, colix2);

  }

  /**
   * 
   * @param screenA
   * @param screenB
   * @param madBond
   * @param colix
   * @param endcaps
   */
  protected void fillConicalCylinder(P3 screenA, P3 screenB,
                                    int madBond, short colix, 
                                    byte endcaps) {
    float radius1 = vwr.tm.scaleToScreen((int) screenA.z, madBond) / 2f;
    if (radius1 == 0)
      return;
    if (radius1 < 1)
      radius1 = 1;
    if (screenA.distance(screenB) == 0) {
      outputSphere(screenA.x, screenA.y, screenA.z, radius1, colix);
      return;
    }
    float radius2 = vwr.tm.scaleToScreen((int) screenB.z, madBond) / 2f;
    if (radius2 == 0)
      return;
    if (radius2 < 1)
      radius2 = 1;
    outputCylinderConical(screenA, screenB, radius1, radius2, colix);
  }

  @Override
  void fillCylinderScreenMad(short colix, byte endcaps, int diameter, 
                               P3 screenA, P3 screenB) {
    if (diameter == 0)
      return;
    if (diameter < 1)
      diameter = 1;
    float radius = diameter / 2f;
    if (screenA.distance(screenB) == 0) {
      outputSphere(screenA.x, screenA.y, screenA.z, radius, colix);
      return;
    }
    outputCylinder(screenA, screenB, radius, colix, endcaps == GData.ENDCAPS_FLAT);
    if (endcaps != GData.ENDCAPS_SPHERICAL || radius <= 1)
      return;
    outputSphere(screenA.x, screenA.y, screenA.z, radius, colix);
    outputSphere(screenB.x, screenB.y, screenB.z, radius, colix);

  }

  @Override
  void fillCylinderScreen(short colix, byte endcaps, int screenDiameter, P3 screenA, 
                                 P3 screenB, P3 ptA, P3 ptB, float radius) {
          // vectors, polyhedra
    fillCylinderScreenMad(colix, endcaps, screenDiameter, screenA, screenB);
  }

  @Override
  void fillSphere(short colix, int diameter, P3 pt) {
    outputSphere(pt.x, pt.y, pt.z, diameter / 2f, colix);
  }
  
  @Override
  protected void fillTriangle(short colix, T3 ptA, T3 ptB, T3 ptC, boolean twoSided) {
    outputTriangle(ptA, ptB, ptC, colix);
  }

  @Override
  void fillEllipsoid(P3 center, P3[] points, short colix, int x,
                       int y, int z, int diameter, M3 toEllipsoidal,
                       double[] coef, M4 deriv, P3[] octantPoints) {
    float radius = diameter / 2f;
    if (radius == 0)
      return;
    if (radius < 1)
      radius = 1;
    outputEllipsoid(center, radius, coef, colix); 
  }

}
