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

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.Qd;
import javajs.util.SB;
import javajs.util.P3d;
import javajs.util.OC;
import javajs.util.M3d;
import javajs.util.M4d;
import javajs.util.T3d;
import javajs.util.V3d;

import org.jmol.api.JmolRendererInterface;

import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.script.T;
import org.jmol.util.C;
import org.jmol.util.Font;
import org.jmol.util.GData;
import org.jmol.util.Logger;
import org.jmol.util.MeshSurface;
import org.jmol.viewer.StateManager;
import org.jmol.viewer.TransformManager;
import org.jmol.viewer.Viewer;


/*
 * Jmol Export Drivers
 * 
 * ___Exporter
 *     __CartesianExporter
 *         _IdtfExporter
 *         _MayaExporter
 *         _VrmlExporter
 *         _X3dExporter                      
 *     __RayTracerExporter
 *         _PovrayExporter
 *         _TachyonExporter
 *
 * 
 *  org.jmol.export is a package that contains export drivers --
 *  custom interfaces for capturing the information that would normally
 *  go to the screen. 
 *  
 *  The Jmol script command is:
 *  
 *    write [driverName] [filename] 
 *  
 *  For example:
 *  
 *    write VRML "myfile.wrl"
 *    
 *  Or, programmatically:
 *  
 *  String data = org.jmol.viewer.Viewer.generateOutput([Driver])
 *  
 *  where in this case [Driver] is a string such as "Maya" or "Vrml".
 *  
 *  Once a driver is registered in org.jmol.viewer.JmolConstants.EXPORT_DRIVER_LIST,
 *  all that is necessary is to add the appropriate Java class file to 
 *  the org.jmol.export directory with the name _[DriverName]Exporter.java. 
 *  
 *  Jmol will find it using Class.forName().
 *   
 *  This export driver should subclass either __CartesianExporter or __RayTracerExporter.
 *  The difference is that __CartesianExporters use the untransformed XYZ coordinates of the model,
 *  with all distances in milliAngstroms, while __RayTracerExporter uses screen coordinates 
 *  (which may include perspective distortion), with all distances in pixels
 *  In addition, a __RayTracerExporter will clip based on the window size, like the standard graphics.
 *  
 *  The export driver is then responsible for implementing all outstanding abstract methods
 *  of the ___Exporter class. Most of these are of the form outputXXXXX(...). 
 *  
 *  In the renderers, there are occasions when we need to know that we are exporting. 
 *  In those cases ShapeRenderer.exportType will be set and can be tested. 
 *  
 *  Basically, this system is designed to be updated easily by multiple 
 *  developers. The process should be:
 *  
 *   1) Add the Driver name to org.jmol.viewer.JmolConstants.EXPORT_DRIVER_LIST.
 *   2) Copy one of the exporters to create org.jmol.export._[DriverName]Exporter.java
 *   3) Fill out the template with proper calls. 
 *  
 *  Alternatively, Java-savvy users can create their own drivers entirely independently
 *  and place them in org.jmol.export. Setting the script variable "exportDrivers" to
 *  include this driver enables that custom driver. The default value for this variable is:
 *  
 *    exportDrivers = "Maya;Vrml"
 *   
 *  Whatever default drivers are provided with Jmol should be in EXPORT_DRIVER_LIST; setting
 *  
 *    exportDrivers = "Mydriver"
 *    
 *  Disables Maya and Vrml; setting it to   
 *  
 *    exportDrivers = "Maya;Vrml;Mydriver"
 *    
 *  Enables the default Maya and Vrml drivers as well as a user-custom driver, _MydriverExporter.java
 *    
 * Bob Hanson, 7/2007, updated 12/2009
 * 
 */

public abstract class ___Exporter {

  // The following fields and methods are required for instantiation or provide
  // generally useful functionality:

  protected boolean solidOnly; // _STL
  
  protected Viewer vwr;
  protected TransformManager tm;
  protected double privateKey;
  protected JmolRendererInterface export3D;
  protected OC out;
  protected String fileName;
  protected String commandLineOptions;
  
  protected GData gdata;

  protected short backgroundColix;
  protected int screenWidth;
  protected int screenHeight;
  protected int slabZ;
  protected int depthZ;
  protected V3d lightSource;
  protected P3d fixedRotationCenter;
  protected P3d referenceCenter;
  protected P3d cameraPosition;
  protected double cameraDistance;
  protected double apertureAngle;
  protected double scalePixelsPerAngstrom;
  protected double exportScale = 1; // currently VRML and X3D only



  // Most exporters (Maya, X3D, VRML, IDTF) 
  // can manipulate actual 3D data.
  // exportType == Graphics3D.EXPORT_CARTESIAN indicates that and is used:
  // a) to prevent export of the background image
  // b) to prevent export of the backgrounds of labels
  // c) to prevent clipping based on the window size
  // d) for single bonds, just use the XYZ coordinates
  
  // POV-RAY is different -- as EXPORT_RAYTRACER, 
  // it's taken to be a single view image
  // with a limited, clipped window.
  
  int exportType;
  
  final protected static double degreesPerRadian = (double) (180 / Math.PI);

  final protected P3d tempP1 = new P3d();
  final protected P3d tempP2 = new P3d();
  final protected P3d tempP3 = new P3d();
  final protected P3d center = new P3d();
  final protected V3d tempV1 = new V3d();
  final protected V3d tempV2 = new V3d();
  private boolean isWebGL;
  
  public ___Exporter() {
  }

  boolean initializeOutput(Viewer vwr, double privateKey, GData gdata,
                           Map<String, Object> params) {
    return initOutput(vwr, privateKey, gdata, params);
  }

  protected boolean initOutput(Viewer vwr, double privateKey, GData g3d,
                             Map<String, Object> params) {
    this.vwr = vwr;
    tm = vwr.tm;
    isWebGL = params.get("type").equals("JS");
    gdata = g3d;
    this.privateKey = privateKey;
    backgroundColix = vwr.getObjectColix(StateManager.OBJ_BACKGROUND);
    center.setT(tm.fixedRotationCenter);
    exportScale = vwr.getDouble(T.exportscale);
    if (exportScale == 0) {
      exportScale = 10; // default is 1 cm/Angstrom -- 1 : 100,000,000
    }
    Logger.info("__Exporter exportScale: " + exportScale);
    if ((screenWidth <= 0) || (screenHeight <= 0)) {
      screenWidth = vwr.getScreenWidth();
      screenHeight = vwr.getScreenHeight();
    }
    slabZ = g3d.slab;
    depthZ = g3d.depth;
    lightSource = g3d.getLightSource();
    P3d[] cameraFactors = vwr.tm.getCameraFactors();
    referenceCenter = cameraFactors[0];
    cameraPosition = cameraFactors[1];
    fixedRotationCenter = cameraFactors[2];
    cameraDistance = cameraFactors[3].x;
    apertureAngle = cameraFactors[3].y;
    scalePixelsPerAngstrom = cameraFactors[3].z;
    out = (OC) params.get("outputChannel");
    commandLineOptions = (String) params.get("params");
    if (out != null)
      fileName = out.getFileName();
    outputHeader();
    return true;
  }

  abstract protected void outputHeader();

  protected void output(String data) {
    out.append(data);
  }

  protected int getByteCount() {
    return out.getByteCount();
  }

  protected void outputComment(String comment) {
    if (commentChar != null)
      output(commentChar + comment + "\n");
  }
  

  protected static void setTempVertex(T3d pt, T3d offset, T3d ptTemp) {
    ptTemp.setT(pt);
    if (offset != null)
      ptTemp.add(offset);
  }

  protected void outputVertices(T3d[] vertices, int nVertices, T3d offset) {
    for (int i = 0; i < nVertices; i++) {
      if (Double.isNaN(vertices[i].x))
        continue;
      outputVertex(vertices[i], offset);
      output("\n");
    }
  }

  protected void outputVertex(T3d pt, T3d offset) {
    setTempVertex(pt, offset, tempV1);
    output(tempV1);
  }

  abstract protected void output(T3d pt);

  protected void outputJmolPerspective() {
    outputComment(getJmolPerspective());
  }

  protected String commentChar;
  protected String getJmolPerspective() {
    if (commentChar == null)
      return "";
    SB sb = new SB();
    sb.append(commentChar).append("Jmol perspective:");
    sb.append("\n").append(commentChar).append("screen width height dim: " + screenWidth + " " + screenHeight + " " + vwr.getScreenDim());
    sb.append("\n").append(commentChar).append("perspectiveDepth: " + vwr.tm.perspectiveDepth);
    sb.append("\n").append(commentChar).append("cameraDistance(angstroms): " + cameraDistance);
    sb.append("\n").append(commentChar).append("aperatureAngle(degrees): " + apertureAngle);
    sb.append("\n").append(commentChar).append("scalePixelsPerAngstrom: " + scalePixelsPerAngstrom);
    sb.append("\n").append(commentChar).append("light source: " + lightSource);
    sb.append("\n").append(commentChar).append("lighting: " + vwr.getLightingState().replace('\n', ' '));
    sb.append("\n").append(commentChar).append("center: " + center);
    sb.append("\n").append(commentChar).append("rotationRadius: " + vwr.getDouble(T.rotationradius));
    sb.append("\n").append(commentChar).append("boundboxCenter: " + vwr.getBoundBoxCenter());
    sb.append("\n").append(commentChar).append("translationOffset: " + tm.getTranslationScript());
    sb.append("\n").append(commentChar).append("zoom: " + vwr.tm.zmPct);
    sb.append("\n").append(commentChar).append("moveto command: " + vwr.getOrientation(T.moveto, null, null));
    sb.append("\n");
    return sb.toString();
  }

  protected void outputFooter() {
    // implementation-specific
  }

  protected String finalizeOutput() {
    return finalizeOutput2();
  }
  
  protected String finalizeOutput2() {
    outputFooter();
    if (out == null)
      return null;
    String ret = out.closeChannel();
    if (fileName == null)
      return ret;
    if (ret != null) {
      Logger.info(ret);
      return "ERROR EXPORTING FILE: " + ret;
    }
    return "OK " + out.getByteCount() + " " + export3D.getExportName() + " " + fileName ;
  }

  protected String getExportDate() {
    return vwr.apiPlatform.getDateFormat(null);
  }

  protected String rgbFractionalFromColix(short colix) {
    return rgbFractionalFromArgb(gdata.getColorArgbOrGray(colix));
  }

  protected String getTriadC(T3d t) {
    return  getTriad(t);
  }
  
  protected String getTriad(T3d t) {
    return round(t.x) + " " + round(t.y) + " " + round(t.z); 
  }
  
  final private P3d tempC = new P3d();

  protected String rgbFractionalFromArgb(int argb) {
    int red = (argb >> 16) & 0xFF;
    int green = (argb >> 8) & 0xFF;
    int blue = argb & 0xFF;
    tempC.set(red == 0 ? 0 : (red + 1)/ 256d, 
        green == 0 ? 0 : (green + 1) / 256d, 
        blue == 0 ? 0 : (blue + 1) / 256d);
    return getTriadC(tempC);
  }

  protected static String translucencyFractionalFromColix(short colix) {
    return round(C.getColixTranslucencyFractional(colix));
  }

  protected static String opacityFractionalFromColix(short colix) {
    return round(1 - C.getColixTranslucencyFractional(colix));
  }

  protected static String opacityFractionalFromArgb(int argb) {
    int opacity = (argb >> 24) & 0xFF;
    return round(opacity == 0 ? 0 : (opacity + 1) / 256d);
  }

  protected static String round(double number) { // AH
    String s;
    return (number == 0 ? "0" : number == 1 ? "1" : (s = ""
        + (Math.round(number * 1000d) / 1000d)).startsWith("0.") ? s
        .substring(1) : s.startsWith("-0.") ? "-" + s.substring(2) : 
          s.endsWith(".0") ? s.substring(0, s.length() - 2) : s);
  }

  protected static String round(T3d pt) {
    return round(pt.x) + " " + round(pt.y) + " " + round(pt.z);
  }
  
  /**
   * input an array of colixes; returns a Vector for the color list and a
   * HashTable for correlating the colix with a specific color index
   * @param i00 
   * @param colixes
   * @param nVertices
   * @param bsSelected
   * @param htColixes
   * @return Vector and HashTable
   */
  protected Lst<Short> getColorList(int i00, short[] colixes, int nVertices,
                                BS bsSelected, Map<Short, Integer> htColixes) {
    int nColix = 0;
    Lst<Short> list = new  Lst<Short>();
    boolean isAll = (bsSelected == null);
    int i0 = (isAll ? nVertices - 1 : bsSelected.nextSetBit(0));
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bsSelected.nextSetBit(i + 1))) {
      Short color = Short.valueOf(colixes[i]);
      if (!htColixes.containsKey(color)) {
        list.addLast(color);
        htColixes.put(color, Integer.valueOf(i00 + nColix++));
      }
    }
    return list;
  }

  protected static MeshSurface getConeMesh(P3d centerBase, M3d matRotateScale, short colix) {
    MeshSurface ms = new MeshSurface();
    int ndeg = 10;
    int n = 360 / ndeg;
    ms.colix = colix;
    ms.vs = new P3d[ms.vc = n + 1];
    ms.pis = AU.newInt2(ms.pc = n);
    for (int i = 0; i < n; i++)
      ms.pis[i] = new int[] {i, (i + 1) % n, n };
    double d = ndeg / 180. * Math.PI; 
    for (int i = 0; i < n; i++) {
      double x = (double) (Math.cos(i * d));
      double y = (double) (Math.sin(i * d));
      ms.vs[i] = P3d.new3(x, y, 0);
    }
    ms.vs[n] = P3d.new3(0, 0, 1);
    if (matRotateScale != null) {
      ms.normals = new V3d[ms.vc];
      for (int i = 0; i < ms.vc; i++) {
        matRotateScale.rotate(ms.vs[i]);
        ms.normals[i] = V3d.newV(ms.vs[i]);
        ms.normals[i].normalize();
        ms.vs[i].add(centerBase);
      }
    }
    return ms;
  }

  protected M3d getRotationMatrix(P3d pt1, P3d pt2, double radius) {    
    M3d m = new M3d();
    M3d m1;
    if (pt2.x == pt1.x && pt2.y == pt1.y) {
      m1 = M3d.newM3((M3d) null);
      if (pt1.z > pt2.z) // 180-degree rotation about X
        m1.m11 = m1.m22 = -1;
    } else {
      tempV1.sub2(pt2, pt1);
      tempV2.set(0, 0, 1);
      tempV2.cross(tempV2, tempV1);
      tempV1.cross(tempV1, tempV2);
      Qd q = Qd.getQuaternionFrameV(tempV2, tempV1, null, false);
      m1 = q.getMatrix();
    }
    m.m00 = radius;
    m.m11 = radius;
    m.m22 = pt2.distance(pt1);
    m1.mul(m);
    return m1;
  }

  protected M3d getRotationMatrix(P3d pt1, P3d ptZ, double radius, P3d ptX, P3d ptY) {    
    M3d m = new M3d();
    m.m00 = ptX.distance(pt1) * radius;
    m.m11 = ptY.distance(pt1) * radius;
    m.m22 = ptZ.distance(pt1) * 2;
    Qd q = Qd.getQuaternionFrame(pt1, ptX, ptY);
    M3d m1 = q.getMatrix();
    m1.mul(m);
    return m1;
  }

  // The following methods are called by a variety of shape renderers and 
  // Export3D, replacing methods in org.jmol.g3d. More will be added as needed. 

  abstract void drawAtom(Atom atom, double radius);

  abstract void drawCircle(int x, int y, int z,
                                   int diameter, short colix, boolean doFill);  //draw circle 

  abstract boolean drawEllipse(P3d ptAtom, P3d ptX, P3d ptY,
                             short colix, boolean doFill);

  void drawSurface(MeshSurface meshSurface, short colix) {
    int nVertices = meshSurface.vc;
    if (nVertices == 0)
      return;
    int nTriangles = 0;
    int nPolygons = meshSurface.pc;
    BS bsPolygons = meshSurface.bsPolygons;
    int faceVertexMax = (meshSurface.haveQuads ? 4 : 3);
    int[][] indices = meshSurface.pis;
    boolean isAll = (bsPolygons == null);
    int i0 = (isAll ? nPolygons - 1 : bsPolygons.nextSetBit(0));
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bsPolygons.nextSetBit(i + 1)))
      nTriangles += (faceVertexMax == 4 && indices[i].length == 4 ? 2 : 1);
    if (nTriangles == 0)
      return;

    T3d[] vertices = meshSurface.getVertices();
    T3d[] normals = meshSurface.normals;

    boolean colorSolid = (colix != 0);
    short[] colixes = (colorSolid ? null : meshSurface.vcs);
    short[] polygonColixes = (colorSolid ? meshSurface.pcs : null);

    Map<Short, Integer> htColixes = null;
    Lst<Short> colorList = null;
    if (!isWebGL) {
      htColixes = new Hashtable<Short, Integer>();
      if (polygonColixes != null)
        colorList = getColorList(0, polygonColixes, nPolygons, bsPolygons,
            htColixes);
      else if (colixes != null)
        colorList = getColorList(0, colixes, nVertices, null, htColixes);
    }
    outputSurface(vertices, normals, colixes, indices, polygonColixes,
        nVertices, nPolygons, nTriangles, bsPolygons, faceVertexMax, colix,
        colorList, htColixes, meshSurface.offset);
  }

  /**
   * @param vertices      generally unique vertices [0:nVertices)
   * @param normals       one per vertex
   * @param colixes       one per vertex, or null
   * @param indices       one per triangular or quad polygon;
   *                      may have additional elements beyond vertex indices if faceVertexMax = 3
   *                      triangular if faceVertexMax == 3; 3 or 4 if face VertexMax = 4
   * @param polygonColixes face-based colixes
   * @param nVertices      vertices[nVertices-1] is last vertex
   * @param nPolygons     indices[nPolygons - 1] is last polygon
   * @param nTriangles        number of triangular faces required
   * @param bsPolygons    number of polygons (triangles or quads)   
   * @param faceVertexMax (3) triangles only, indices[][i] may have more elements
   *                      (4) triangles and quads; indices[][i].length determines 
   * @param colix         overall (solid) color index
   * @param colorList     list of unique color IDs
   * @param htColixes     map of color IDs to colorList
   * @param offset 
   * 
   */
  protected void outputSurface(T3d[] vertices, T3d[] normals,
                                short[] colixes, int[][] indices,
                                short[] polygonColixes,
                                int nVertices, int nPolygons, int nTriangles, BS bsPolygons,
                                int faceVertexMax, short colix, Lst<Short> colorList,
                                Map<Short, Integer> htColixes, P3d offset) {
    // not implemented in _ObjExporter
  }

  abstract void drawPixel(short colix, int x, int y, int z, int scale); //measures
  
  abstract void drawTextPixel(int argb, int x, int y, int z);

  //rockets and dipoles
  abstract void fillConeScreen(short colix, byte endcap, int screenDiameter, 
                         P3d screenBase, P3d screenTip, boolean isBarb);
  
  abstract void drawCylinder(P3d atom1, P3d atom2, short colix1, short colix2,
                             byte endcaps, int madBond, int bondOrder);

  abstract void fillCylinderScreenMad(short colix, byte endcaps, int diameter, 
                                        P3d screenA, P3d screenB);

  abstract void fillCylinderScreen(short colix, byte endcaps, int screenDiameter, 
                                   P3d screenA, P3d screenB, P3d ptA, P3d ptB, double radius);

  abstract void fillEllipsoid(P3d center, P3d[] points, short colix, 
                              int x, int y, int z, int diameter,
                              M3d toEllipsoidal, double[] coef,
                              M4d deriv, P3d[] octantPoints);

  void drawFilledCircle(short colixRing, short colixFill, int diameter, int x, int y, int z) {
    if (colixRing != 0)
      drawCircle(x, y, z, diameter, colixRing, false);
    if (colixFill != 0)
      drawCircle(x, y, z, diameter, colixFill, true);
  }

  //rockets:
  abstract void fillSphere(short colix, int diameter, P3d pt);
  
  //cartoons, rockets, polyhedra:
  protected abstract void fillTriangle(short colix, T3d ptA0, T3d ptB0, T3d ptC0, boolean twoSided);
  
  
  private int nText;
  private int nImage;
  public short lineWidthMad;

  protected int fixScreenZ(int z) {
    return (z <= 3 ? z + (int) tm.cameraDistance : z);
  }

  void plotImage(int x, int y, int z, Object image, short bgcolix, int width,
                 int height) {
    outputComment("start image " + (++nImage));
    gdata.plotImage(x, y, z, image, export3D, bgcolix, width, height);
    outputComment("end image " + nImage);
  }

  void plotText(int x, int y, int z, short colix, String text, Font font3d) {
  	
    // trick here is that we use Jmol's standard g3d package to construct
    // the bitmap, but then output to jmolRenderer, which returns control
    // here via drawPixel.
    outputComment("start text " + (++nText) + ": " + text);
    gdata.plotText(x, y, z, gdata.getColorArgbOrGray(colix), 0, text, font3d, export3D);
    outputComment("end text " + nText + ": " + text);
  }

}



