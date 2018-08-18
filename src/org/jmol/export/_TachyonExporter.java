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
import org.jmol.util.GData;
import javajs.util.Lst;
import javajs.util.SB;
import javajs.util.P3;
import javajs.util.M3;
import javajs.util.T3;
import javajs.util.V3;
import org.jmol.viewer.Viewer;

/*
 * see http://jedi.ks.uiuc.edu/~johns/raytracer/papers/tachyon.pdf
 * 
 */

public class _TachyonExporter extends __RayTracerExporter {

  String lighting;
  String phong;
  
  UseTable textures = new UseTable(" ");
  
  public _TachyonExporter() {
    commentChar = "# ";
  }
 
  @Override
  boolean initializeOutput(Viewer vwr, double privateKey, GData gdata, Map<String, Object> params) {
    getLightingInfo();
    return initOutput(vwr, privateKey, gdata, params);    
  }
  
  private void getLightingInfo() {
    lighting = " AMBIENT " + round(gdata.getAmbientPercent() / 100f)
        + " DIFFUSE " + round(gdata.getDiffusePercent()/100f) 
        + " SPECULAR " + round(gdata.getSpecularPercent() / 100f);
    phong = " Phong Plastic 0.5 Phong_size " + gdata.getSpecularExponent();
  }  
  
  /* 
  public String finalizeOutput() {
    if (wasPerspectiveDepth)
      vwr.setPerspectiveDepth(true);
    return super.finalizeOutput();
  }
  */

  @Override
  protected void outputHeader() {
    initVars();
    output("# ******************************************************\n");
    output("# Created by Jmol " + Viewer.getJmolVersion() + "\n");
    output("#\n");
    output("# This script was generated on " + getExportDate() + "\n");
    output("#\n");
    output("# Requires Tachyon version 0.98.7 or newer\n");
    output("#\n");
    output("# Default tachyon rendering command for this scene:\n");
    output("#   tachyon  -aasamples 12 %s -format TARGA -o %s.tga\n");
    output("#\n");
    output("# ******************************************************\n");
    output("\n");
    output(getJmolPerspective());
    output("\n");
    output("Begin_Scene\n");
    output("Resolution " + screenWidth + " " + screenHeight + "\n");
    output("Shader_Mode Medium\n"); // not documented.
    output("  Trans_VMD\n");
    output("  Fog_VMD\n");
    output("End_Shader_Mode\n");
    output("Camera\n");
//    output("  projection ORTHOGRAPHIC\n");
    output("  Zoom 3.0\n");
    output("  Aspectratio 1\n");
    output("  Antialiasing 12\n");
    output("  Raydepth 8\n");
    output("  Center " + triad(screenWidth / 2, screenHeight / 2, 0) + "\n");
    output("  Viewdir 0 0 1\n");
    output("  Updir   0 1 0\n");
    output("End_Camera\n");
    output("Directional_Light Direction " + round(lightSource) + " Color 1 1 1\n");
    output("\n");
    output("Background " + rgbFractionalFromColix(backgroundColix)
        + "\n");
    output("\n");
  }

  @Override
  protected void outputFooter() {
    output("End_Scene\n");
  }

  @Override
  protected void output(T3 pt) {
    output(triad(pt));
  }

  private String triad(float x, float y, float z) {
    return (int) x + " " + (int) (-y) + " " + (int) z;
  }

  private String triad(T3 pt) {
    if (Float.isNaN(pt.x))
      return "0 0 0";
    return triad(pt.x, pt.y, pt.z);
  }

  private String textureCode;
  
  private void outputTextureCode() {
    output(textureCode);
    output("\n");
  }

  private void outputTexture(short colix, boolean useTexDef) {
    outputTexture2(rgbFractionalFromColix(colix), 
        opacityFractionalFromColix(colix), useTexDef);
  }

  private void outputTexture(int argb, boolean useTexDef) {
    outputTexture2(rgbFractionalFromArgb(argb), 
        opacityFractionalFromArgb(argb), useTexDef);
  }
  
  private void outputTexture2(String rgb, String opacity, boolean useTexDef) {
    textureCode = (useTexDef ? textures.getDef("t" + rgb + opacity) : null);
    if (useTexDef && textureCode.startsWith(" "))
      return;
    SB sb = new SB();
    sb.append(lighting);
    sb.append(" Opacity " + opacity);
    sb.append(phong);
    sb.append(" Color " + rgb);
    sb.append(" TexFunc 0\n");
    if (!useTexDef) {
      textureCode = "Texture " + sb;
      return;
    }
    output("TexDef " + textureCode);
    output(sb.toString());
    textureCode = " " + textureCode;
  }

  @Override
  protected void outputCircle(int x, int y, int z, float radius, short colix,
                              boolean doFill) {
    tempV1.set(0,0,-1);
    outputRing(x, y, z, tempV1, radius, colix, doFill);
  }

  private void outputRing(int x, int y, int z, V3 tempV1, float radius,
                          short colix, boolean doFill) {
    outputTexture(colix, true);
    output("Ring Center ");
    output(triad(x, y, z));
    output(" Normal " + triad(tempV1));
    output(" Inner " + round((doFill ? 0 : radius * 0.95)));
    output(" Outer " + round(radius));
    outputTextureCode();
  }

  @Override
  protected void outputCone(P3 screenBase, P3 screenTip, float radius,
                            short colix, boolean isBarb) {
    
    // as mesh, which uses Cartesian coordinates
    
    tm.unTransformPoint(screenBase, tempP1);
    tm.unTransformPoint(screenTip, tempP2);
    radius = vwr.tm.unscaleToScreen(screenBase.z, radius);
    M3 matRotateScale = getRotationMatrix(tempP1, tempP2, radius);
    export3D.drawSurface(getConeMesh(tempP1, matRotateScale, colix), colix);
  }

  @Override
  protected void outputCylinder(P3 screenA, P3 screenB,
                                      float radius, short colix, boolean withCaps) {
    outputTexture(colix, true);
    output("FCylinder Base ");
    output(triad(screenA));
    output(" Apex ");
    output(triad(screenB));
    output(" Rad " + round(radius));
    outputTextureCode();
    if (withCaps && radius > 1) {
      tempV1.sub2(screenA, screenB);
      outputRing((int) screenA.x, (int) screenA.y, (int) screenA.z, tempV1, radius, colix, true);
      tempV1.scale(-1);
      outputRing((int) screenB.x, (int) screenB.y, (int) screenB.z, tempV1, radius, colix, true);
    }
  }  
  
  @Override
  protected void fillConicalCylinder(P3 screenA, P3 screenB,
                                     int madBond, short colix, byte endcaps) {
    // conic sections not implemented in Tachyon
    int diameter = (int) vwr.tm.scaleToScreen((int) ((screenA.z + screenB.z)/2f), madBond);
    fillCylinderScreenMad(colix, endcaps, diameter, screenA, screenB);
   }


  @Override
  protected void outputCylinderConical(P3 screenA, P3 screenB,
                                       float radius1, float radius2, short colix) {
    //not applicable
  }

  @Override
  protected void outputEllipsoid(P3 center, float radius, double[] coef, short colix) {
    tm.transformPt3f(center, tempP1);
    // no support for ellipsoids -- just draw ball
    outputSphere(tempP1.x, tempP1.y, tempP1.z, radius, colix);
  }

  @Override
  protected void outputSurface(T3[] vertices, T3[] normals,
                               short[] colixes, int[][] indices,
                               short[] polygonColixes, int nVertices,
                               int nPolygons, int nTriangles, BS bsPolygons,
                               int faceVertexMax, short colix,
                               Lst<Short> colorList, Map<Short, Integer> htColixes, P3 offset) {
    if (polygonColixes != null) {
      boolean isAll = (bsPolygons == null);
      int i0 = (isAll ? nPolygons - 1 : bsPolygons.nextSetBit(0));
      for (int i = i0; i >= 0; i = (isAll ? i - 1 : bsPolygons.nextSetBit(i + 1))) {
        setTempVertex(vertices[indices[i][0]], offset, tempP1);
        setTempVertex(vertices[indices[i][1]], offset, tempP2);
        setTempVertex(vertices[indices[i][2]], offset, tempP3);
        tm.transformPt3f(tempP1, tempP1);
        tm.transformPt3f(tempP2, tempP2);
        tm.transformPt3f(tempP3, tempP3);
        outputTriangle(tempP1, tempP2, tempP3, colix);
      }
      return;
    }
    outputTexture(colixes == null ? colix : colixes[0], false);
    output("VertexArray  Numverts " + nVertices + "\nCoords\n");
    for (int i = 0; i < nVertices; i++)
      outputVertex(vertices[i], offset);
    output("\nNormals\n");
    for (int i = 0; i < nVertices; i++) {
      setTempVertex(vertices[i], offset, tempP1);
      output(triad(getScreenNormal(tempP1, normals[i], 10)) + "\n");
    }
    String rgb = (colixes == null ? rgbFractionalFromColix(colix) : null);
    output("\nColors\n");
    for (int i = 0; i < nVertices; i++) {
      output((colixes == null ? rgb : rgbFractionalFromColix(colixes[i])) + "\n");
    }
    outputTextureCode();
    output("\nTriMesh " + nTriangles + "\n");
    boolean isAll = (bsPolygons == null);
    int i0 = (isAll ? nPolygons - 1 : bsPolygons.nextSetBit(0));
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bsPolygons.nextSetBit(i + 1))) {
      output(indices[i][0] + " " + indices[i][1] + " " + indices[i][2] + "\n");
      if (faceVertexMax == 4 && indices[i].length == 4)
        output(indices[i][0] + " " + indices[i][2] + " " + indices[i][3] + "\n");
    }
    output("\nEnd_VertexArray\n");
  }

  @Override
  protected void outputSphere(float x, float y, float z, float radius,
                                  short colix) {

    outputTexture(colix, true);
    output("Sphere Center ");
    output(triad(x, y, z));
    output(" Rad " + round(radius));
    outputTextureCode();
  }

  @Override
  protected void outputTextPixel(int x, int y, int z, int argb) {
    outputTexture(argb, true);
    output("Sphere Center ");
    output(triad(x, y, z));
    output(" Rad 1");
/*
    output("BOX MIN ");
    output(triad(x, y, z));
    output(" MAX ");
    output(triad(x + 1, y - 1, z + 1));
*/    
    outputTextureCode();
  }
  
  @Override
  protected void outputTriangle(T3 ptA, T3 ptB, T3 ptC, short colix) {
    outputTexture(colix, true);
    output("TRI");
    output(" V0 " + triad(ptA));
    output(" V1 " + triad(ptB));
    output(" V2 " + triad(ptC));
    outputTextureCode();
  }

}
