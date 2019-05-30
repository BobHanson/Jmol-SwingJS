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

import org.jmol.awtjs.swing.Font;
import javajs.util.BS;

import org.jmol.util.GData;

import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.T3;

/**
 * A class to output WebGL graphics. 
 * 
 * Only minimally fleshed out. No text, for instance.
 * 
 * This class demonstrates a way of interacting with JavaScript
 * that other classes in Jmol do not use. The methods here that start
 * with "js" -- jsInitExport, jsEndExport, etc., are all implemented
 * in JSmolGLmol.js -- that is, they are overridden in the JavaScript.
 * 
 * The advantage to this is that this code references just the one
 * line of JavaScript below, which is a static call and serves
 * to extend the prototype. extendJSExporter is in JSmolGLmol.js. 
 * 
 */
public class JSExporter extends __CartesianExporter {

  static {
    /**
     * @j2sNative
     * 
     * Jmol && Jmol.GLmol && Jmol.GLmol.extendJSExporter(org.jmol.export.JSExporter.prototype);
     * 
     */
    {}
    
  }
  
  
  public JSExporter() {
  }

  private Map<String, Boolean> htSpheresRendered = new Hashtable<String, Boolean>();

  private Map<String, Object[]> htObjects = new Hashtable<String, Object[]>();

  Object html5Applet;

  private UseTable useTable;

    ////////////////////// JavaScript will override these /////////////
  
  /**
   * @param applet  
   */
  private void jsInitExport(Object applet) {
    // implemented in JavaScript only    
  }

  /**
   * @param applet  
   */
  private void jsEndExport(Object applet) {
    // implemented in JavaScript only
  }

  /**
   * @param applet  
   * @param id 
   * @param isNew 
   * @param pt1 
   * @param pt2 
   * @param o 
   */
  private void jsCylinder(Object applet, String id, boolean isNew, P3 pt1,
                          P3 pt2, Object[] o) {
    // implemented in JavaScript only
  }
  
  /**
   * @param applet  
   * @param id 
   * @param isNew 
   * @param pt 
   * @param o 
   */
  private void jsSphere(Object applet, String id, boolean isNew, T3 pt,
                        Object[] o) {
    // implemented in JavaScript only
  }

  /**
   * @param applet
   * @param vertices
   * @param normals
   * @param indices
   * @param nVertices
   * @param nPolygons
   * @param nFaces
   * @param bsPolygons
   * @param faceVertexMax
   * @param color
   * @param vertexColors
   * @param polygonColors
   */
  protected void jsSurface(Object applet, T3[] vertices, T3[] normals,
                           int[][] indices, int nVertices, int nPolygons,
                           int nFaces, BS bsPolygons, int faceVertexMax,
                           int color, int[] vertexColors, int[] polygonColors) {
    // implemented in JavaScript only
  }

  /**
   * @param applet
   * @param color
   * @param pt1
   * @param pt2
   * @param pt3
   */
  void jsTriangle(Object applet, int color, T3 pt1, T3 pt2, T3 pt3) {
    // implemented in JavaScript only
  }

  //////////////////// standard exporter methods //////////////////
  
  @Override
  protected void outputHeader() {
    html5Applet = this.vwr.html5Applet;
    useTable = new UseTable("JS");
    htSpheresRendered.clear();
    htObjects.clear();
    jsInitExport(html5Applet);
  }

  @Override
  protected void outputFooter() {
    jsEndExport(html5Applet);
    htSpheresRendered.clear();
    htObjects.clear();
    useTable = null;
  }

@Override
  protected void outputSphere(P3 ptCenter, float radius, short colix,
                              boolean checkRadius) {
    int iRad = Math.round(radius * 100);
    String check = round(ptCenter) + (checkRadius ? " " + iRad : "");
    if (htSpheresRendered.get(check) != null)
      return;
    htSpheresRendered.put(check, Boolean.TRUE);
    boolean found = useTable.getDefRet("S" + colix + "_" + iRad, ret);
    Object[] o;
    if (found)
      o = htObjects.get(ret[0]);
    else
      htObjects.put(ret[0],
          o = new Object[] { getColor(colix), Float.valueOf(radius) });
    jsSphere(html5Applet, ret[0], !found, ptCenter, o);
  }

  private String[] ret = new String[1];

  @Override
  protected boolean outputCylinder(P3 ptCenter, P3 pt1, P3 pt2, short colix,
                                   byte endcaps, float radius, P3 ptX, P3 ptY,
                                   boolean checkRadius) {
    // ptX and ptY are ellipse major and minor axes
    // not implemented yet
    if (ptX != null)
      return false;
    float length = pt1.distance(pt2);
    boolean found = useTable.getDefRet(
        "C" + colix + "_" + Math.round(length * 100) + "_" + radius + "_"
            + endcaps, ret);
    Object[] o;
    if (found)
      o = htObjects.get(ret[0]);
    else
      htObjects.put(
          ret[0],
          o = new Object[] { getColor(colix), Float.valueOf(length),
              Float.valueOf(radius) });
    jsCylinder(html5Applet, ret[0], !found, pt1, pt2, o);
    return true;
  }

  @Override
  protected void outputCircle(P3 pt1, P3 pt2, float radius, short colix,
                              boolean doFill) {
    // TODO Auto-generated method stub
  }

  @Override
  protected void outputEllipsoid(P3 center, P3[] points, short colix) {
    // TODO Auto-generated method stub
  }

  @Override
  protected void outputCone(P3 ptBase, P3 ptTip, float radius, short colix) {
    outputCylinder(null, ptBase, ptTip, colix, GData.ENDCAPS_NONE, radius,
        null, null, false);
  }

  private Integer getColor(short colix) {
    return Integer.valueOf(gdata.getColorArgbOrGray(colix));
  }

  @Override
  protected void outputSurface(T3[] vertices, T3[] normals,
                               short[] vertexColixes, int[][] indices,
                               short[] polygonColixes, int nVertices,
                               int nPolygons, int nTriangles, BS bsPolygons,
                               int faceVertexMax, short colix,
                               Lst<Short> colorList,
                               Map<Short, Integer> htColixes, P3 offset) {
    int[] vertexColors = getColors(vertexColixes);
    int[] polygonColors = getColors(polygonColixes);
    jsSurface(html5Applet, vertices, normals, indices, nVertices, nPolygons, nTriangles,
        bsPolygons, faceVertexMax, gdata.getColorArgbOrGray(colix), vertexColors,
        polygonColors);
  }

  @Override
  protected void outputTriangle(T3 pt1, T3 pt2, T3 pt3, short colix) {
    jsTriangle(html5Applet, gdata.getColorArgbOrGray(colix), pt1, pt2, pt3);
  }

  @Override
  protected void outputTextPixel(P3 pt, int argb) {
    // TODO

  }
  
  @Override
  protected void outputFace(int[] is, int[] coordMap, int faceVertexMax) {
    // n/a
  }

  @Override
  protected void output(T3 pt) {
    // used by some exporters to output a list of vertices
    // n/a
  }

  /////////////// called by Export3D ////////////
  
  @Override
  void plotImage(int x, int y, int z, Object image, short bgcolix, int width,
                 int height) {
    // TODO
  }
  
  @Override
  void plotText(int x, int y, int z, short colix, String text, Font font3d) {
    // TODO -- not sure how to handle z exactly. 
    // These are screen coordinates. You have to use
    // vwr.unTransformPoint(pointScreen, pointAngstroms) 
    // to return that to actual coordinates.
  }



  /////////////// private methods ///////////////
  
  private int[] getColors(short[] colixes) {
    if (colixes == null)
      return null;
    int[] colors = new int[colixes.length];
    for (int i = colors.length; --i >= 0;) {
      colors[i] = gdata.getColorArgbOrGray(colixes[i]);
    }
    return colors;
  }


}
