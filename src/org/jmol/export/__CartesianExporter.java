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

import java.util.Hashtable;
import java.util.Map;

import javajs.awt.Font;
import javajs.util.A4;
import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.P3;
import javajs.util.T3;

import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.util.C;
import org.jmol.util.GData;
import org.jmol.util.Logger;

/*
 * for programs that use the standard 3D coordinates.
 * IDTF, Maya, OBJ, VRML, JS
 * 
 */
abstract public class __CartesianExporter extends ___Exporter {

  protected A4 viewpoint = new A4();
  protected boolean canCapCylinders;
  protected boolean noColor;

  public __CartesianExporter() {
    exportType = GData.EXPORT_CARTESIAN;
    lineWidthMad = 100;
  }

  protected P3 getModelCenter() {
    // "center" is the center of rotation, not
    // necessary the screen center or the center of the model. 
    // When the user uses ALT-CTRL-drag, Jmol is applying an 
    // XY screen translation AFTER the matrix transformation. 
    // Apparently, that's unusual in this business. 
    // (The rotation center is generally directly
    // in front of the observer -- not allowing, for example,
    // holding the model in one's hand at waist level and rotating it.)

    // But there are good reasons to do it the Jmol way. If you don't, then
    // what happens is that the distortion pans over the moving model
    // and you get an odd lens effect rather than the desired smooth
    // panning. So we must approximate.

    return referenceCenter;
  }

  protected P3 getCameraPosition() {

    // used for VRML/X3D only

    P3 ptCamera = new P3();
    P3 pt = P3.new3(screenWidth / 2, screenHeight / 2, 0);
    tm.unTransformPoint(pt, ptCamera);
    ptCamera.sub(center);
    // this is NOT QUITE correct when the model has been shifted with CTRL-ALT
    // because in that case the center of distortion is not the screen center,
    // and these simpler perspective models don't allow for that.
    tempP3.set(screenWidth / 2, screenHeight / 2, cameraDistance
        * scalePixelsPerAngstrom);
    tm.unTransformPoint(tempP3, tempP3);
    tempP3.sub(center);
    ptCamera.add(tempP3);

    //System.out.println(ptCamera + " " + cameraPosition);
    //  return ptCamera;

    return cameraPosition;

  }

  private void setTempPoints(P3 ptA, P3 ptB, boolean isCartesian) {
    if (isCartesian) {
      // really first order -- but actual coord
      tempP1.setT(ptA);
      tempP2.setT(ptB);
    } else {
      tm.unTransformPoint(ptA, tempP1);
      tm.unTransformPoint(ptB, tempP2);
    }
  }

  protected int getCoordinateMap(T3[] vertices, int[] coordMap, BS bsValid) {
    int n = 0;
    for (int i = 0; i < coordMap.length; i++) {
      if (bsValid != null && !bsValid.get(i) || Float.isNaN(vertices[i].x)) {
        if (bsValid != null)
          bsValid.clear(i);
        continue;
      }
      coordMap[i] = n++;
    }
    return n;
  }

  protected int[] getNormalMap(T3[] normals, int nNormals,
                               BS bsValid, Lst<String> vNormals) {
    Map<String, Integer> htNormals = new Hashtable<String, Integer>();
    int[] normalMap = new int[nNormals];
    for (int i = 0; i < nNormals; i++) {
      String s;
      if (bsValid != null && !bsValid.get(i) || Float.isNaN(normals[i].x)){
        if (bsValid != null)
          bsValid.clear(i);
        continue;
      }
      s = getTriad(normals[i]) + "\n";
      if (htNormals.containsKey(s)) {
        normalMap[i] = htNormals.get(s).intValue();
      } else {
        normalMap[i] = vNormals.size();
        vNormals.addLast(s);
        htNormals.put(s, Integer.valueOf(normalMap[i]));
      }
    }
    return normalMap;
  }

  protected void outputIndices(int[][] indices, int[] map, int nPolygons,
                               BS bsPolygons, int faceVertexMax) {
    // called from IDtf, Vrml, Xed when outputting a surface
    boolean isAll = (bsPolygons == null);
    int i0 = (isAll ? nPolygons - 1 : bsPolygons.nextSetBit(0));
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bsPolygons.nextSetBit(i + 1)))
      outputFace(indices[i], map, faceVertexMax);
  }

  // called from IDtf, Vrml, Xed when outputting a surface
  protected abstract void outputFace(int[] is, int[] coordMap, int faceVertexMax);

  abstract protected void outputCircle(P3 pt1, P3 pt2, float radius,
                                       short colix, boolean doFill);

  abstract protected void outputCone(P3 ptBase, P3 ptTip,
                                     float radius, short colix);

  abstract protected boolean outputCylinder(P3 ptCenter, P3 pt1,
                                            P3 pt2, short colix1,
                                            byte endcaps, float radius,
                                            P3 ptX, P3 ptY, boolean checkRadius);

  abstract protected void outputEllipsoid(P3 center, P3[] points,
                                          short colix);

  abstract protected void outputSphere(P3 ptCenter, float f, short colix, boolean checkRadius);

  abstract protected void outputTextPixel(P3 pt, int argb);

  abstract protected void outputTriangle(T3 pt1, T3 pt2, T3 pt3,
                                         short colix);

  // these are called by Export3D:

  ///////////////////////// called by Export3D ////////////////
  
  @Override
  void plotText(int x, int y, int z, short colix, String text, Font font3d) {
    // over-ridden in VRML and X3D
    // trick here is that we use Jmol's standard g3d package to construct
    // the bitmap, but then output to jmolRenderer, which returns control
    // here via drawPixel.
    gdata.plotText(x, y, z, gdata.getColorArgbOrGray(colix), 0, text,
        font3d, export3D);
  }

  @Override
  void plotImage(int x, int y, int z, Object image, short bgcolix, int width,
                 int height) {
    // not implemented in VRML
    //    gdata.plotImage(x, y, z, image, jmolRenderer, bgcolix, width, height);
  }

  @Override
  void drawAtom(Atom atom, float radius) {
    if (Logger.debugging)
      outputComment("atom " + atom);
    short colix = atom.colixAtom;
    outputSphere(atom, radius == 0 ? atom.madAtom / 2000f : radius, colix, C.isColixTranslucent(colix));
  }

  @Override
  void drawCircle(int x, int y, int z, int diameter, short colix, boolean doFill) {
    // draw circle
    tempP3.set(x, y, z);
    tm.unTransformPoint(tempP3, tempP1);
    float radius = vwr.tm.unscaleToScreen(z, diameter) / 2;
    tempP3.set(x, y, z + 1);
    tm.unTransformPoint(tempP3, tempP3);
    outputCircle(tempP1, tempP3, radius, colix, doFill);
  }

  @Override
  boolean drawEllipse(P3 ptCenter, P3 ptX, P3 ptY, short colix,
                      boolean doFill) {
    tempV1.sub2(ptX, ptCenter);
    tempV2.sub2(ptY, ptCenter);
    tempV2.cross(tempV1, tempV2);
    tempV2.normalize();
    tempV2.scale(doFill ? 0.002f : 0.005f);
    tempP1.sub2(ptCenter, tempV2);
    tempP2.add2(ptCenter, tempV2);
    return outputCylinder(ptCenter, tempP1, tempP2, colix,
        doFill ? GData.ENDCAPS_FLAT : GData.ENDCAPS_NONE, 1.01f, ptX,
        ptY, true);
  }

  @Override
  void drawPixel(short colix, int x, int y, int z, int scale) {
    //measures, meshRibbon, dots
    tempP3.set(x, y, z);
    tm.unTransformPoint(tempP3, tempP1);
    outputSphere(tempP1, 0.02f * scale, colix, true);
  }

  @Override
  void drawTextPixel(int argb, int x, int y, int z) {
    // text only - HLine and VLine and plotImagePixel
    tempP3.set(x, y, z);
    tm.unTransformPoint(tempP3, tempP1);
    outputTextPixel(tempP1, argb);
  }

  @Override
  void fillConeScreen(short colix, byte endcap, int screenDiameter,
                      P3 screenBase, P3 screenTip, boolean isBarb) {
    tm.unTransformPoint(screenBase, tempP1);
    tm.unTransformPoint(screenTip, tempP2);
    float radius = vwr.tm.unscaleToScreen(screenBase.z, screenDiameter) / 2;
    if (radius < 0.05f)
      radius = 0.05f;
    outputCone(tempP1, tempP2, radius, colix);
  }

  /**
   * bond order -1 -- single bond, Cartesian
   * bond order -2 -- multiple bond, Cartesian 
   */
  @Override
  void drawCylinder(P3 ptA, P3 ptB, short colix1, short colix2,
                    byte endcaps, int mad, int bondOrder) {
    setTempPoints(ptA, ptB, bondOrder < 0);
    float radius = mad / 2000f;
    if (Logger.debugging)
      outputComment("bond " + ptA + " " + ptB);
    if (colix1 == colix2 || noColor) {
      outputCylinder(null, tempP1, tempP2, colix1, endcaps, radius, null, null, bondOrder != -1);
    } else {
      tempV2.ave(tempP2, tempP1);
      tempP3.setT(tempV2);
      if (solidOnly && endcaps == GData.ENDCAPS_NONE)
        endcaps = GData.ENDCAPS_FLAT;
      else if (canCapCylinders && endcaps == GData.ENDCAPS_SPHERICAL)
        endcaps = (solidOnly ? GData.ENDCAPS_FLAT_TO_SPHERICAL : GData.ENDCAPS_OPEN_TO_SPHERICAL);
      outputCylinder(null, tempP3, tempP1, colix1,
          (endcaps == GData.ENDCAPS_SPHERICAL ? GData.ENDCAPS_NONE
              : endcaps), radius, null, null, true);
      outputCylinder(null, tempP3, tempP2, colix2,
          (endcaps == GData.ENDCAPS_SPHERICAL ? GData.ENDCAPS_NONE
              : endcaps), radius, null, null, true);
      if (endcaps == GData.ENDCAPS_SPHERICAL) {
        outputSphere(tempP1, radius * 1.01f, colix1, bondOrder != -2);
        outputSphere(tempP2, radius * 1.01f, colix2, bondOrder != -2);
      }
    }
  }

  @Override
  void fillCylinderScreenMad(short colix, byte endcaps, int mad,
                             P3 screenA, P3 screenB) {
    float radius = mad / 2000f;
    setTempPoints(screenA, screenB, false);
    outputCylinder(null, tempP1, tempP2, colix, endcaps, radius, null, null, true);
  }

  @Override
  void fillCylinderScreen(short colix, byte endcaps, int screenDiameter,
                          P3 screenA, P3 screenB, P3 ptA, P3 ptB, float radius) {
    if (ptA != null) {
      drawCylinder(ptA, ptB, colix, colix, endcaps, Math.round(radius * 2000f), -1);
      return;
    }    
    // vectors, polyhedra
    // was (int) in older version
    int mad = Math.round(vwr.tm.unscaleToScreen((screenA.z + screenB.z) / 2,
        screenDiameter) * 1000);
    fillCylinderScreenMad(colix, endcaps, mad, screenA, screenB);
  }

  @Override
  void fillEllipsoid(P3 center, P3[] points, short colix, int x,
                     int y, int z, int diameter, M3 toEllipsoidal,
                     double[] coef, M4 deriv, P3[] octantPoints) {
    outputEllipsoid(center, points, colix);
  }

  @Override
  void fillSphere(short colix, int diameter, P3 pt) {
    tm.unTransformPoint(pt, tempP1);
    outputSphere(tempP1, vwr.tm.unscaleToScreen(pt.z, diameter) / 2, colix, true);
  }

  @Override
  protected void fillTriangle(short colix, T3 ptA, T3 ptB, T3 ptC,
                              boolean twoSided) {
    
    // fillTriangleTwoSided
    //   for Polyhedra (collapsed)

    // fillQuadrilateral
    //   for Rockets (boxes)
    
    // fillTriangle3CNBits
    //   for GeoSurfaceRenderer (not exported), Draw polygons, Ellipsoid arc fill 
    
    // fillTriangle3i
    //   for Cartoon nucleic bases
    
    tm.unTransformPoint(ptA, tempP1);
    tm.unTransformPoint(ptB, tempP2);
    tm.unTransformPoint(ptC, tempP3);
    if (solidOnly) {
      outputSolidPlate(tempP1, tempP2, tempP3, colix);
    } else {
      outputTriangle(tempP1, tempP2, tempP3, colix);
      if (twoSided)
        outputTriangle(tempP1, tempP3, tempP2, colix);
    }
  }

  /**
   * @param tempP1  
   * @param tempP2  
   * @param tempP3  
   */
  protected void outputSolidPlate(P3 tempP1, P3 tempP2, P3 tempP3, short colix) {
    // VRML/STL only
  }

  protected M4 sphereMatrix = new M4();

  protected void setSphereMatrix(T3 center, float rx, float ry, float rz,
                                 A4 a, M4 sphereMatrix) {
    if (a != null) {
      M3 m = new M3();
      m.m00 = rx;
      m.m11 = ry;
      m.m22 = rz;
      M3 mq = new M3().setAA(a);
      mq.mul(m);
      sphereMatrix.setToM3(mq);
    } else {
      sphereMatrix.setIdentity();
      sphereMatrix.m00 = rx;
      sphereMatrix.m11 = ry;
      sphereMatrix.m22 = rz;
    }
    sphereMatrix.m03 = center.x;
    sphereMatrix.m13 = center.y;
    sphereMatrix.m23 = center.z;
    sphereMatrix.m33 = 1;
  }
  

}
