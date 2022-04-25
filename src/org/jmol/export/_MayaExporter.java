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


import javajs.util.BS;

import javajs.util.Lst;
import javajs.util.P3d;
import javajs.util.T3d;


public class _MayaExporter extends __CartesianExporter {
  
  public _MayaExporter() {
    commentChar = "// ";
  }

  /*
   * The Maya exporter was the first exporter -- really very crude
   * and never worked out because the user interest didn't develop
   * past the initial stages. Still, it is important because it
   * represents the first effort, which is much more fulfilled in 
   * other exporters now, and it does produce a basic model 
   * with sticks and balls.
   * 
   * Bob Hanson
   * 
   */
  private int nBalls = 0;
  private int nCyl = 0;
  private String name;
  private String id;

  @Override
  protected void outputHeader() {
    output("//  Maya ASCII 8.5 scene\n");
    output("//  Name: ball_stripped.ma\n");
    //    output("//  CreatedBy: Jmol");
    output("//  Last modified: Thu, Jul 5, 2007 10:25:55 PM\n");
    output("//  Codeset: UTF-8\n");
    output("requires maya \"8.5\";\n");
    output("currentUnit -l centimeter -a degree -t film;\n");
    output("fileInfo \"application\" \"maya\";\n");
    output("fileInfo \"product\" \"Maya Unlimited 8.5\";\n");
    output("fileInfo \"version\" \"8.5\";\n");
    output("fileInfo \"cutIdentifier\" \"200612170012-692032\";\n");
    output("fileInfo \"osv\" \"Mac OS X 10.4.9\";  \n");
  }

  private void addAttr() {
    output(" setAttr -k off \".v\";\n");
    output(" setAttr \".vir\" yes;\n");
    output(" setAttr \".vif\" yes;\n");
    output(" setAttr \".tw\" yes;\n");
    output(" setAttr \".covm[0]\"  0 1 1;\n");
    output(" setAttr \".cdvm[0]\"  0 1 1;\n");
  }

  private void addConnect() {
    output(" connectAttr \"make" + name + ".os\" \"" + id + ".cr\";\n");
    output("connectAttr \"" + id
        + ".iog\" \":initialShadingGroup.dsm\" -na;\n");
  }

  private void setAttr(String attr, double val) {
    output(" setAttr \"." + attr + "\" " + val + ";\n");
  }

  private void setAttr(String attr, int val) {
    output(" setAttr \"." + attr + "\" " + val + ";\n");
  }

  private void setAttr(String attr, T3d pt) {
    output(" setAttr \"." + attr + "\" -type \"double3\" " + pt.x + " "
        + pt.y + " " + pt.z + ";\n");
  }

  @Override
  protected boolean outputCylinder(P3d ptCenter, P3d pt1, P3d pt2, short colix,
                      byte endcaps, double radius, P3d ptX, P3d ptY, boolean checkRadius) {
    if (ptX != null)
      return false;
    nCyl++;
    name = "nurbsCylinder" + nCyl;
    id = "nurbsCylinderShape" + nCyl;
    output(" createNode transform -n \"" + name + "\";\n");
    double length = pt1.distance(pt2);
    tempV1.ave(pt2, pt1);
    setAttr("t", tempV1);
    tempV1.sub(pt1);
    tempV2.setT(tempV1);
    tempV2.normalize();
    double r = tempV1.length();
    double rX = (double) Math.acos(tempV1.y / r) * degreesPerRadian;
    if (tempV1.x < 0)
      rX += 180;
    double rY = (double) Math.atan2(tempV1.x, tempV1.z) * degreesPerRadian;
    tempV2.set(rX, rY, 0);
    setAttr("r", tempV2);
    output(" createNode nurbsSurface -n \"" + id + "\" -p \"" + name
        + "\";\n");
    addAttr();
    output("createNode makeNurbCylinder -n \"make" + name + "\";\n");
    output(" setAttr \".ax\" -type \"double3\" 0 1 0;\n");
    setAttr("r", radius);
    setAttr("s", 4);
    setAttr("hr", length / radius);
    addConnect();
    return true;
  }

  @Override
  protected void outputSphere(P3d pt, double radius, short colix, boolean checkRadius) {
    //String color = rgbFromColix(colix);
    nBalls++;
    name = "nurbsSphere" + nBalls;
    id = "nurbsSphereShape" + nBalls;

    output("createNode transform -n \"" + name + "\";\n");
    setAttr("t", pt);
    output("createNode nurbsSurface -n \"" + id + "\" -p \"" + name
        + "\";\n");
    addAttr();
    output("createNode makeNurbSphere -n \"make" + name + "\";\n");
    output(" setAttr \".ax\" -type \"double3\" 0 1 0;\n");
    setAttr("r", radius);
    setAttr("s", 4);
    setAttr("nsp", 3);
    addConnect();
  }

  // not implemented: 
  
  @Override
  void drawTextPixel(int argb, int x, int y, int z) {
    // override __CartesianExporter
  }

  @Override
  protected void outputTextPixel(P3d pt, int argb) {
  }
  
  @Override
  protected void outputSurface(T3d[] vertices, T3d[] normals,
                                  short[] colixes, int[][] indices,
                                  short[] polygonColixes,
                                  int nVertices, int nPolygons, int nTriangles, BS bsPolygons,
                                  int faceVertexMax, short colix,
                                  Lst<Short> colorList, Map<Short, Integer> htColixes, P3d offset) {
  }

  @Override
  protected void outputTriangle(T3d pt1, T3d pt2, T3d pt3,
                                short colix) {
    // TODO
    
  }

  @Override
  protected void outputCircle(P3d pt1, P3d pt2, double radius,
                              short colix, boolean doFill) {
    // TODO
    
  }

  @Override
  protected void outputCone(P3d ptBase, P3d ptTip, double radius,
                            short colix) {
    // TODO
    
  }

  @Override
  protected void outputEllipsoid(P3d center, P3d[] points, short colix) {
    // TODO
    
  }

  @Override
  protected void outputFace(int[] is, int[] coordMap, int faceVertexMax) {
    // TODO
    
  }

  @Override
  protected void output(T3d pt) {
    // TODO
    
  }

}
