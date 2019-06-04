/* $RCSfile$
 * $Author: aherraez $
 * $Date: 2009-01-15 21:00:00 +0100 (Thu, 15 Jan 2009) $
 * $Revision: 7752 $

 *
 * Copyright (C) 2003-2009  The Jmol Development Team
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

/*	Based on _VrmlExporter  by rhanson
		and Help from http://x3dgraphics.com/examples/X3dForWebAuthors/index.html
*/
 
package org.jmol.export;


import java.util.Map;

import javajs.util.A4;
import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.T3;

import javajs.util.BS;

import org.jmol.util.Font;
import org.jmol.util.GData;
import org.jmol.viewer.Viewer;

public class _X3dExporter extends _VrmlExporter {

  public _X3dExporter() {
    super();
    useTable = new UseTable("USE='");
  }
  

  @Override
  protected void outputHeader() {
    output("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
    output("<!DOCTYPE X3D PUBLIC \"ISO//Web3D//DTD X3D 3.1//EN\" \"http://www.web3d.org/specifications/x3d-3.1.dtd\">\n");
    output("<X3D profile='Immersive' version='3.1' "
        + "xmlns:xsd='http://www.w3.org/2001/XMLSchema-instance' "
        + "xsd:noNamespaceSchemaLocation=' http://www.web3d.org/specifications/x3d-3.1.xsd '>"
        + "\n");
    output("<head>\n");
    output("<meta name='title' content="
        + PT.esc(vwr.ms.modelSetName).replace('<', ' ').replace('>',
            ' ').replace('&', ' ') + "/>\n");
    output("<meta name='description' content='Jmol rendering'/>\n");
    output("<meta name='creator' content=' '/>\n");
    output("<meta name='created' content='" + getExportDate() + "'/>\n");
    output("<meta name='generator' content='Jmol " + Viewer.getJmolVersion()
        + ", http://www.jmol.org'/>\n");
    output("<meta name='license' content='http://www.gnu.org/licenses/licenses.html#LGPL'/>\n");
    output("</head>\n");
    output("<Scene>\n");

    output("<NavigationInfo type='EXAMINE'/>\n");
    // puts the vwr into model-rotation mode
    output("<Background skyColor='" + rgbFractionalFromColix(backgroundColix)
        + "'/>\n");
    // next is an approximation only
    float angle = getViewpoint();
    output("<Viewpoint fieldOfView='" + angle);
    output("' position='");
    // remove export scaling for from Viewpoint so on-screen version is good.
    cameraPosition.z *= exportScale;
    output(cameraPosition);
    output("' orientation='");
    output(tempP1);
    output(" " + -viewpoint.angle + "'\n jump='true' description='v1'/>\n");
    output("\n  <!-- \n");
    output(getJmolPerspective());
    output("\n  -->\n\n");
    commentChar = null;
    outputInitialTransform();
  }
  
  @Override
  protected void outputAttrPt(String attr, T3 pt) {
    output(" " + attr + "='" + pt.x + " " + pt.y + " " + pt.z + "'");
  }

  @Override
  protected void pushMatrix() {
    output("<Transform ");
  }

  @Override
  protected void popMatrix() {
    output("</Transform>\n");
  }

  @Override
  protected void outputAttr(String attr, float x, float y, float z) {
    output(" " + attr + "='" + round(x) + " " + round(y) + " " + round(z) + "'");
  }
  
  @Override
  protected void outputRotation(A4 a) {
    output(" rotation='" + a.x + " " + a.y + " " + a.z + " " + a.angle + "'");
  }


  @Override
  protected void outputFooter() {
    useTable = null;
    popMatrix();
    popMatrix();
    output("</Scene>\n");
    output("</X3D>\n");
  }

  @Override
  protected void outputAppearance(short colix, boolean isText) {  
    String def = getDef((isText ? "T" : "") + colix);
    output("<Appearance ");
    if (def.charAt(0) == '_') {
      String color = rgbFractionalFromColix(colix);
      output("DEF='" + def + "'><Material diffuseColor='");
      if (isText)
        output("0 0 0' specularColor='0 0 0' ambientIntensity='0.0' shininess='0.0' emissiveColor='" 
            + color + "'/>");
      else
        output(color + "' transparency='" + translucencyFractionalFromColix(colix) + "'/>" );
    }
    else
      output(def +">");
    output("</Appearance>");
  }
  
  @Override
  protected void outputChildShapeStart() {
    outputShapeStart();
  }

  @Override
  protected void outputShapeStart() {
    output("<Shape>");
    outputFaceSetStart();
  }

  @Override
  protected void outputChildStart() {
    // not used!
  }

  @Override
  protected void outputChildClose() {
    // not used!
  }

  @Override
  protected void outputDefChildFaceSet(String child) {
    if (child != null)
      output("DEF='" + child + "'");
  }

  @Override
  protected void outputFaceSetStart() {
    output("<IndexedFaceSet ");
  }

  @Override
  protected void outputFaceSetClose() {
    output("</IndexedFaceSet>\n");
  }

  @Override
  protected void outputUseChildClose(String child) {
    output(child + "/>");
  }

  @Override
  protected void outputChildShapeClose() {
    outputShapeClose();
  }

  @Override
  protected void outputShapeClose() {
    output("</Shape>\n");
  }

  @Override
  protected void outputCloseTag() {
    output(">\n");
  }


  @Override
  protected void outputTriangle(T3 pt1, T3 pt2, T3 pt3, short colix) {
    // nucleic base
    // cartoons
    output("<Shape>\n");
    output("<IndexedFaceSet solid='false' ");
    output("coordIndex='0 1 2 -1'>");
    output("<Coordinate point='");
    output(pt1);
    output(" ");
    output(pt2);
    output(" ");
    output(pt3);
    output("'/>");
    output("</IndexedFaceSet>\n");
    outputAppearance(colix, false);
    output("\n</Shape>\n");
  }


  @Override
  protected void outputCircle(P3 pt1, P3 pt2, float radius, short colix,
                              boolean doFill) {
    
    // not fixed -- still duplicated in X3d
    if (doFill) {

      // draw filled circle
      pushMatrix();
        output("translation='");
        tempV1.ave(tempP3, pt1);
        output(tempV1);
        output("'><Billboard axisOfRotation='0 0 0'>");
        pushMatrix();
          output ("rotation='1 0 0 1.5708'");
          float height = pt1.distance(pt2);
          outputAttr("scale", radius, height, radius);
          output(">");
          outputCylinderChildScaled(colix, GData.ENDCAPS_FLAT);
        popMatrix();
        output("</Billboard>");
      popMatrix();
      
      return;
    }
    
    // draw a thin torus

    String child = getDef("C" + colix + "_" + radius);
    pushMatrix();
      outputTransRot(tempP3, pt1, 0, 0, 1);
      tempP3.set(1, 1, 1);
      tempP3.scale(radius);
      outputAttr("scale", tempP3.x, tempP3.y, tempP3.z);
      output(">\n<Billboard ");
      if (child.charAt(0) == '_') {
        output("DEF='" + child + "'");
        output(" axisOfRotation='0 0 0'>");
        pushMatrix();
          output("<Shape><Extrusion beginCap='false' convex='false' endCap='false' creaseAngle='1.57'");
          output(" crossSection='");
          float rpd = 3.1415926f / 180;
          float scale = 0.02f / radius;
          for (int i = 0; i <= 360; i += 10) {
            output(round(Math.cos(i * rpd) * scale) + " ");
            output(round(Math.sin(i * rpd) * scale) + " ");
          }
          output("' spine='");
          for (int i = 0; i <= 360; i += 10) {
            output(round(Math.cos(i * rpd)) + " ");
            output(round(Math.sin(i * rpd)) + " 0 ");
          }
          output("'/>");
          outputAppearance(colix, false);
          output("</Shape>");
        popMatrix();
      } else {
        output(child + ">");
      }
      output("</Billboard>\n");
    popMatrix();
  }

  @Override
  protected void outputGeometry(T3[] vertices, T3[] normals,
                              short[] colixes, int[][] indices,
                              short[] polygonColixes,
                              int nVertices, int nPolygons,
                              BS bsPolygons,
                              int faceVertexMax, Lst<Short> colorList, Map<Short, Integer> htColixes, P3 offset) {
    
    output(" creaseAngle='0.5'\n");

    if (polygonColixes != null)
      output(" colorPerVertex='false'\n");

    // coordinates, part 1

    output("coordIndex='\n");
    int[] map = new int[nVertices];
    getCoordinateMap(vertices, map, null);
    outputIndices(indices, map, nPolygons, bsPolygons, faceVertexMax);
    output("'\n");

    // normals, part 1  
    
    Lst<String> vNormals = null;
    if (normals != null) {
      vNormals = new  Lst<String>();
      map = getNormalMap(normals, nVertices, null, vNormals);
      output("  solid='false'\n  normalPerVertex='true'\n  normalIndex='\n");
      outputIndices(indices, map, nPolygons, bsPolygons, faceVertexMax);
      output("'\n");
    }      
    
    map = null;
    
    // colors, part 1
        
    if (colorList != null) {
      output("  colorIndex='\n");
      outputColorIndices(indices, nPolygons, bsPolygons, faceVertexMax, htColixes, colixes, polygonColixes);
      output("'\n");
    }    

    output(">\n");  // closes IndexedFaceSet opening tag
    
    // coordinates, part 2
    
    output("<Coordinate point='\n");
    outputVertices(vertices, nVertices, offset);
    output("'/>\n");

    // normals, part 2

    if (normals != null) {
      output("<Normal vector='\n");
      outputNormals(vNormals);
      vNormals = null;
      output("'/>\n");
    }

    // colors, part 2

    if (colorList != null) {
      output("<Color color='\n");
      outputColors(colorList);
      output("'/>\n");
    }
  }

  @Override
  protected void outputTextPixel(P3 pt, int argb) {
//    // text only
//    String color = rgbFractionalFromArgb(argb);
//    output("<Transform translation='");
//    output(pt);
//    output("'>\n<Shape ");
//    String child = getDef("p" + argb);
//    if (child.charAt(0) == '_') {
//      output("DEF='" + child + "'>");
//      output("<Sphere radius='0.01'/>");
//      output("<Appearance><Material diffuseColor='0 0 0' specularColor='0 0 0'"
//        + " ambientIntensity='0.0' shininess='0.0' emissiveColor='" 
//        + color + "'/></Appearance>'");
//    } else {
//      output(child + ">");
//    }
//    output("</Shape>\n");
//    output("</Transform>\n");
  }

  @Override
  void plotText(int x, int y, int z, short colix, String text, Font font3d) {
//    output("<Transform translation='");
//    output(setFont(x, y, z, colix, text, font3d));
//    output("'>");
//    // These x y z are 3D coordinates of echo or the atom the label is attached
//    // to.
//    output("<Billboard ");
//    if (fontChild.charAt(0) == '_') {
//      output("DEF='" + fontChild + "' axisOfRotation='0 0 0'>"
//        + "<Transform translation='0.0 0.0 0.0'>"
//        + "<Shape>");
//      outputAppearance(colix, true);
//      output("<Text string=" + PT.esc(text) + ">");
//      output("<FontStyle ");
//      String fontstyle = getDef("F" + fontFace + fontStyle);
//      if (fontstyle.charAt(0) == '_') {
//        output("DEF='" + fontstyle + "' size='"+fontSize+"' family='" + fontFace
//            + "' style='" + fontStyle + "'/>");      
//      } else {
//        output(fontstyle + "/>");
//      }
//      output("</Text>");
//      output("</Shape>");
//      output("</Transform>");
//    } else {
//      output(fontChild + ">");
//    }
//    output("</Billboard>\n");
//    output("</Transform>\n");
//
//    /*
//     * Unsolved issues: # Non-label texts: echos, measurements :: need to get
//     * space coordinates, not screen coord. # Font size: not implemented; 0.4A
//     * is hardcoded (resizes with zoom) Java VRML font3d.fontSize = 13.0 size
//     * (numeric), but in angstroms, not pixels font3d.fontSizeNominal = 13.0 #
//     * Label offsets: not implemented; hardcoded to 0.25A in each x,y,z #
//     * Multi-line labels: only the first line is received # Sub/superscripts not
//     * interpreted
//     */
  }

  //  @Override
  //  protected void outputCone(P3 ptBase, P3 ptTip, float radius,
  //                            short colix) {
  //    float height = ptBase.distance(ptTip);
  //    pushMatrix();
  //      outputTransRot(ptBase, ptTip, 0, 1, 0);
  //      outputAttr("scale", radius, height, radius);
  //      outputCloseTag();
  //      outputShapeStart();
  //      String child = getDef("c");
  //      if (child.charAt(0) == '_') {
  //        outputDefChildFaceSet(child);
  //        outputConeGeometry(true);
  //        outputCloseFaceSet();
  //      } else {
  //        outputChildClose(child);
  //      }
  //      outputAppearance(colix, false);
  //      outputShapeClose();
  //    popMatrix();
  //  }
  //
  //  @Override
  //  protected boolean outputCylinder(P3 ptCenter, P3 pt1, P3 pt2,
  //                                short colix, byte endcaps, float radius, P3 ptX, P3 ptY, boolean checkRadius) {
  //    float height = pt1.distance(pt2);
  //    pushMatrix();
  //      if (ptX == null) {
  //        outputTransRot(pt1, pt2, 0, 1, 0);
  //        outputAttr("scale", radius, height, radius);
  //      } else {
  //        outputAttrPt("translation", ptCenter);
  //        outputQuaternionFrame(ptCenter, ptY, pt1, ptX, 2, 2, 2);
  //        pt1.set(0, 0, -0.5f);
  //        pt2.set(0, 0, 0.5f);
  //      }
  //      outputCloseTag();
  //      outputCylinderChildScaled(colix, endcaps);
  //    popMatrix();
  ////    if (endcaps == GData.ENDCAPS_SPHERICAL) {
  ////      outputSphere(pt1, radius * 1.01f, colix, true);
  ////      outputSphere(pt2, radius * 1.01f, colix, true);
  ////    }
  //    return true;
  //  }
  //
  //  @Override
  //  protected void outputCylinderChildScaled(short colix,
  //                                   byte endcaps) {
  //    outputShapeStart();
  //    String child = getDef("C" + "_" + endcaps);
  //    if (child.charAt(0) == '_') {
  //      outputDefChildFaceSet(child);
  //      outputCylinderGeometry(endcaps);
  //      outputCloseFaceSet();
  //    } else {
  //      outputUseChildClose(child);
  //    }
  //    outputAppearance(colix, false);
  //    outputShapeClose();
  //  }

//  @Override
//  protected void outputSurface(T3[] vertices, T3[] normals,
//                               short[] colixes, int[][] indices,
//                               short[] polygonColixes,
//                               int nVertices, int nPolygons, int nTriangles, BS bsPolygons,
//                               int faceVertexMax, short colix,
//                               Lst<Short> colorList, Map<Short, Integer> htColixes, P3 offset) {
//    output("<Shape><IndexedFaceSet \n");
//    outputGeometry(vertices, normals, colixes, indices, polygonColixes,
//        nVertices, nPolygons, bsPolygons, faceVertexMax, colorList,
//        htColixes, offset);
//    output("</IndexedFaceSet>");
//    outputAppearance(colix, false);
//    output("</Shape>\n");
//  }


}
