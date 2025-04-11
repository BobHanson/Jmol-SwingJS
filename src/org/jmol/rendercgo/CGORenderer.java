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
package org.jmol.rendercgo;


import org.jmol.renderspecial.DrawRenderer;
import org.jmol.script.T;
import org.jmol.shape.Mesh;
import org.jmol.shapecgo.CGO;
import org.jmol.shapecgo.CGOMesh;
import org.jmol.util.C;
import org.jmol.util.Logger;

import javajs.util.Lst;
import javajs.util.P3d;
import javajs.util.P3i;


/**
 * Something like a PyMOL Compiled Graphical Object, but more interesting!
 * 
 */
public class CGORenderer extends DrawRenderer {

  private CGOMesh cgoMesh;
  private Lst<Object> cmds;
  private P3d pt3 = new P3d();
  private short colix0, colix1, colix2, normix0, normix1, normix2, normix;
  private boolean doColor;
  private int ptNormal;
  private int ptColor;
  
  /**
   * UV mapping Cartesian origin, X, and Y
   */
  private P3d map0, vX, vY;
  /**
   * UV mapping min/max x and y
   */
  private double x0, y0, dx, dy, scaleX, scaleY;
  
  private boolean is2D, is2DPercent, isMapped, isPS;
  private int screenZ;
  private double alpha;
  private int nPts;
  private int glMode;

  
  @Override
  protected boolean render() {
//    isPrecision = true;
    needTranslucent = false;
    imageFontScaling = vwr.imageFontScaling;
    CGO cgo = (CGO) shape;
    for (int i = cgo.meshCount; --i >= 0;)
      render2(mesh = cgoMesh = (CGOMesh) cgo.meshes[i]);
    return needTranslucent;
  }
  
  private void render2(Mesh mesh) {
    diameter = cgoMesh.diameter;
    width = cgoMesh.width;
    cmds = cgoMesh.cmds;
//    if (cmds == null || !cgoMesh.visible || cgoMesh.visibilityFlags == 0)
//      return;
    if (!g3d.setC(cgoMesh.colix)) {
      needTranslucent = true;
      return;
    }
    int n = cmds.size();
    glMode = -1;
    nPts = 0;
    ptNormal = 0;
    ptColor = 0;
    width = cgoMesh.meshWidth / 1000 * 50;
    screenZ = Integer.MAX_VALUE;
    doColor = !mesh.useColix;
    g3d.addRenderer(T.triangles);
    is2D = isMapped = false;
    scaleX = scaleY = 1;

    for (int j = 0; j < n; j++) {
      int type = cgoMesh.getInt(j);
      if (type == CGOMesh.STOP)
        break;
      int len = CGOMesh.getSize(type, is2D);
      if (len < 0) {
        Logger.error("CGO unknown type: " + type);
        return;
      }
      switch (type) {
      default:
        System.out.println("CGO ? " + type);
        break;
      case CGOMesh.SPECIAL:
        break;
      case CGOMesh.DRAW_ARRAYS:
        len = drawArray(j + 1);
        break;
      case CGOMesh.PS_SHOWPAGE:
        // no fill, either
        break;
      case CGOMesh.PS_SETLINEWIDTH:
        diameter = cgoMesh.getInt(j + 1);
        break;
      case CGOMesh.JMOL_DIAMETER:
        width = cgoMesh.getFloat(j + 1);
        break;
      case CGOMesh.JMOL_SCREEN:
        isMapped = false;
        double f = cgoMesh.getFloat(j + 1);
        if (f == 0) {
          is2D = false;
        } else {
          is2DPercent = (f > 0);
          screenZ = (is2DPercent ? tm.zValueFromPercent((int) f) : -(int) f);
          is2D = true;
        }
        break;
      case CGOMesh.JMOL_PS:
        isPS = true;
        //$FALL-THROUGH$
      case CGOMesh.JMOL_UVMAP:
        is2D = isMapped = true;
        map0 = new P3d();
        vX = new P3d();
        vY = new P3d();
        cgoMesh.getPoint(j + 1, map0);
        cgoMesh.getPoint(j + 4, vX);
        vX.sub(map0);
        cgoMesh.getPoint(j + 7, vY);
        vY.sub(map0);
        x0 = cgoMesh.getFloat(j + 10);
        y0 = cgoMesh.getFloat(j + 11);
        dx = cgoMesh.getFloat(j + 12) - x0;
        dy = cgoMesh.getFloat(j + 13) - y0;
        if (isPS)
          break;
        //$FALL-THROUGH$
      case CGOMesh.PS_SCALE:
        scaleX = cgoMesh.getFloat(isPS ? j + 1 : j + 14);
        scaleY = cgoMesh.getFloat(isPS ? j + 2 : j + 15);
        break;
      case CGOMesh.ALPHA:
        alpha = cgoMesh.getFloat(j + 1);
        break;
      case CGOMesh.RESET_NORMAL: // use?
        break;
      case CGOMesh.SIMPLE_LINE:
        // what are the first two parameters? 
        // width and number of points?
        getPoint(j + 2, pt0, pt0i);
        getPoint(j + (is2D ? 4 : 5), pt1, pt1i);
        drawEdge(1, 2, false, pt0, pt1, pt0i, pt1i);
        break;
      case CGOMesh.BEGIN:
        glMode = cgoMesh.getInt(j + 1);
        //$FALL-THROUGH$
      case CGOMesh.PS_NEWPATH:
        nPts = 0;
        break;
      case CGOMesh.PS_CLOSEPATH:
        glMode = CGOMesh.PS_CLOSEPATH;
        break;
      case CGOMesh.PS_STROKE:
        if (glMode != CGOMesh.PS_CLOSEPATH)
          break;
        glMode = CGOMesh.GL_LINE_LOOP;
        //$FALL-THROUGH$
      case CGOMesh.END:
        if (glMode == CGOMesh.GL_LINE_LOOP && nPts >= 3)
          drawEdge(1, 2, true, pt0, pt3, pt0i, pt3i);
        nPts = 0;
        break;
      case CGOMesh.COLOR:
        getColix(true);
        break;
      case CGOMesh.NORMAL:
        normix = getNormix();
        break;
      case CGOMesh.PS_MOVETO:
        nPts = 0;
        //$FALL-THROUGH$
      case CGOMesh.PS_LINETO:
        glMode = CGOMesh.GL_LINE_LOOP;
        //$FALL-THROUGH$
      case CGOMesh.VERTEX:
        processVertex(j);
        break;
      case CGOMesh.SAUSAGE:
        getPoint(j, pt0, pt0i);
        getPoint(j + (is2D ? 2 : 3), pt1, pt1i);
        width = cgoMesh.getFloat(j + 7);
        getColix(true);
        getColix(false); // for now -- ignore second color
        drawEdge(1, 2, false, pt0, pt1, pt0i, pt1i);
        width = 0;
        break;
      case CGOMesh.TRICOLOR_TRIANGLE:
        getPoint(j, pt0, pt0i);
        getPoint(j + (is2D ? 2 : 3), pt1, pt1i);
        getPoint(j + (is2D ? 4 : 6), pt2, pt2i);
        normix0 = getNormix();
        normix1 = getNormix();
        normix2 = getNormix();
        colix0 = getColix(false);
        colix1 = getColix(false);
        colix2 = getColix(false);
        fillTriangle();
        break;
      }
      j += len;
    }
  }

  private P3d pt;
  private P3i spt;
  
  private void processVertex(int j) {
    if (nPts++ == 0)
      getPoint(j, pt0, pt0i);
    switch (glMode) {
    case -1:
      break;
    case CGOMesh.SPECIAL:          
      // ??
    case CGOMesh.GL_POINTS:
      drawEdge(1, 1, false, pt0, pt0, pt0i, pt0i);
      break;
    case CGOMesh.GL_LINES:
      if (nPts == 2) {
        getPoint(j, pt1, pt1i);
        drawEdge(1, 2, false, pt0, pt1, pt0i, pt1i);
        nPts = 0;
        return;
      }
      break;
    case CGOMesh.GL_LINE_LOOP:
    case CGOMesh.GL_LINE_STRIP:
      if (nPts == 1) {
        if (glMode == CGOMesh.GL_LINE_LOOP) {
          pt3.setT(pt0);
          pt3i.setT(pt0i);
        }
        break;
      }
      getPoint(j, pt1, pt1i);
      pt = pt0;
      pt0 = pt1;
      pt1 = pt;
      spt = pt0i;
      pt0i = pt1i;
      pt1i = spt;
      drawEdge(1, 2, true, pt0, pt1, pt0i, pt1i);
      break;
    case CGOMesh.GL_TRIANGLES:
      switch (nPts) {
      case 1:
        normix1 = normix2 = normix0 = normix;
        colix1 = colix2 = colix0 = colix;
        break;
      case 2:
        getPoint(j, pt1, pt1i);
        break;
      case 3:
        getPoint(j, pt2, pt2i);
        fillTriangle();
        nPts = 0;
        return;
      }
      break;
    case CGOMesh.GL_TRIANGLE_STRIP:
      // v0 v1 v2   v2 v1 v3   v2 v3 v4   v4 v3 v5 ...
      switch (nPts) {
      case 1:
        normix1 = normix2 = normix0 = normix;
        colix1 = colix2 = colix0 = colix;
        break;
      case 2:
        getPoint(j, pt2, pt2i);
        break;
      default:
        if (nPts % 2 == 0) {
          pt = pt0;
          pt0 = pt2;
          spt = pt0i;
          pt0i = pt2i;
        } else {
          pt = pt1;
          pt1 = pt2;
          spt = pt1i;
          pt1i = pt2i;
        }
        pt2 = pt;
        pt2i = spt;
        getPoint(j, pt2, pt2i);
        fillTriangle();
        break;
      }
      break;
    case CGOMesh.GL_TRIANGLE_FAN:
      // v2 v0 v1   v3 v0 v2   v4 v0 v3  ...
      switch (nPts) {
      case 1:
        normix1 = normix2 = normix0 = normix;
        colix1 = colix2 = colix0 = colix;
        pt1.setT(pt0);
        pt1i.setT(pt0i);
        break;
      case 2:
        getPoint(j, pt0, pt0i);
        break;
      default:
        pt2.setT(pt0);
        pt2i.setT(pt0i);
        getPoint(j, pt0, pt0i);
        fillTriangle();
        break;
      }
      break;
    }
  }

  private int drawArray(int j0) {
    // see CGO.cpp
    int[] info = new int[16];
//    info[0] = glMode;
//    info[1] = arrayTypes;
//    info[2] = nArrays;
//    info[3] = nVertices;
//    info[4] = ptVertices;
//    info[5] = ptNormals;
//    info[6] = ptColors;
//    info[7] = ptPicks;
//    info[8] = ptAccess;
//    info[9] = ptTextures;
    int len = cgoMesh.getDrawArrayParams(j0 - 1, info);
    glMode = info[0];
    int nVertices = info[3];
    int ptVertices = info[4];
    int jColors = info[6];
    cgoMesh.clearNormixList();
    cgoMesh.clearColixList();
    ptNormal = 0;
    ptColor = 0;
    nPts = 0;
    for (int v = 0, j = ptVertices - 1; v < nVertices; v++, j += 3) {
      if (jColors > 0) {
        // not handling alpha here
        cgoMesh.addColix(jColors - 1 + v * 4);
        getColix(true);
      }
      processVertex(j);
    }
    return len;
  }

  private short getNormix() {
    return (ptNormal >= cgoMesh.nList.size() ? 0 : cgoMesh.nList.get(ptNormal++).shortValue());
  }

  private short getColix(boolean doSet) {
    if (doColor) {
      colix = C.copyColixTranslucency(cgoMesh.colix, cgoMesh.cList.get(
          ptColor++).shortValue());
      if (doSet)
        g3d.setC(colix);
    }
    return colix;
  }

 void getPoint(int i, P3d pt, P3i pti) {
    cgoMesh.getPoint(i + 1, pt);
    if (isMapped) { 
      // The vertex coordinates x and y are map coordinates
      // on a plane with origin map0 and axis vectors vX and vY;
      // x0, dx, y0, dy are scalings on the plane 
      double fx = (pt.x * scaleX - x0) / dx;
      double fy = (pt.y * scaleY - y0) / dy;
      pt.scaleAdd2(fx, vX, map0);
      pt.scaleAdd2(fy, vY, pt);      
    } else if (is2D) {
      pti.x = (is2DPercent ? tm.percentToPixels('x', pt.x) : (int) pt.x);
      pti.y = (is2DPercent ? tm.percentToPixels('y', pt.y) : (int) pt.y);
      pti.z = screenZ;
      tm.unTransformPoint(pt, pt);
      return;
    }
    tm.transformPtScr(pt, pti);
  }

  private void fillTriangle() {
    g3d.fillTriangle3CNBits(pt0, colix0, normix0, pt1, colix1, normix1, pt2,
        colix2, normix2, true);
  }

//  private void drawTriangleRGB(P3i s1, P3i s2, P3i s3, int c1,
//                               int c2, int c3) {
//
//    //g3d.fillCylinderRGB(s1, s2, c1, c2, GData.ENDCAPS_OPEN, width);
//    //g3d.fillCylinderRGB(s2, s3, c2, c3, GData.ENDCAPS_OPEN, width);
//    //g3d.fillCylinderRGB(s3, s1, c3, c1, GData.ENDCAPS_OPEN, width);    
//  }

  /*
# Generated by h2py from gl.h
GL_VERSION_1_1 = 1
GL_ACCUM = 0x0100
GL_LOAD = 0x0101
GL_RETURN = 0x0102
GL_MULT = 0x0103
GL_ADD = 0x0104
GL_NEVER = 0x0200
GL_LESS = 0x0201
GL_EQUAL = 0x0202
GL_LEQUAL = 0x0203
GL_GREATER = 0x0204
GL_NOTEQUAL = 0x0205
GL_GEQUAL = 0x0206
GL_ALWAYS = 0x0207
GL_CURRENT_BIT = 0x00000001
GL_POINT_BIT = 0x00000002
GL_LINE_BIT = 0x00000004
GL_POLYGON_BIT = 0x00000008
GL_POLYGON_STIPPLE_BIT = 0x00000010
GL_PIXEL_MODE_BIT = 0x00000020
GL_LIGHTING_BIT = 0x00000040
GL_FOG_BIT = 0x00000080
GL_DEPTH_BUFFER_BIT = 0x00000100
GL_ACCUM_BUFFER_BIT = 0x00000200
GL_STENCIL_BUFFER_BIT = 0x00000400
GL_VIEWPORT_BIT = 0x00000800
GL_TRANSFORM_BIT = 0x00001000
GL_ENABLE_BIT = 0x00002000
GL_COLOR_BUFFER_BIT = 0x00004000
GL_HINT_BIT = 0x00008000
GL_EVAL_BIT = 0x00010000
GL_LIST_BIT = 0x00020000
GL_TEXTURE_BIT = 0x00040000
GL_SCISSOR_BIT = 0x00080000
GL_ALL_ATTRIB_BITS = 0x000fffff
GL_POINTS = 0x0000
GL_LINES = 0x0001
GL_LINE_LOOP = 0x0002
GL_LINE_STRIP = 0x0003
GL_TRIANGLES = 0x0004
GL_TRIANGLE_STRIP = 0x0005
GL_TRIANGLE_FAN = 0x0006
GL_QUADS = 0x0007
GL_QUAD_STRIP = 0x0008
GL_POLYGON = 0x0009
GL_ZERO = 0
GL_ONE = 1
GL_SRC_COLOR = 0x0300
GL_ONE_MINUS_SRC_COLOR = 0x0301
GL_SRC_ALPHA = 0x0302
GL_ONE_MINUS_SRC_ALPHA = 0x0303
GL_DST_ALPHA = 0x0304
GL_ONE_MINUS_DST_ALPHA = 0x0305
GL_DST_COLOR = 0x0306
GL_ONE_MINUS_DST_COLOR = 0x0307
GL_SRC_ALPHA_SATURATE = 0x0308
GL_TRUE = 1
GL_FALSE = 0
GL_CLIP_PLANE0 = 0x3000
GL_CLIP_PLANE1 = 0x3001
GL_CLIP_PLANE2 = 0x3002
GL_CLIP_PLANE3 = 0x3003
GL_CLIP_PLANE4 = 0x3004
GL_CLIP_PLANE5 = 0x3005
GL_BYTE = 0x1400
GL_UNSIGNED_BYTE = 0x1401
GL_SHORT = 0x1402
GL_UNSIGNED_SHORT = 0x1403
GL_INT = 0x1404
GL_UNSIGNED_INT = 0x1405
GL_FLOAT = 0x1406
GL_2_BYTES = 0x1407
GL_3_BYTES = 0x1408
GL_4_BYTES = 0x1409
GL_DOUBLE = 0x140A
GL_NONE = 0
GL_FRONT_LEFT = 0x0400
GL_FRONT_RIGHT = 0x0401
GL_BACK_LEFT = 0x0402
GL_BACK_RIGHT = 0x0403
GL_FRONT = 0x0404
GL_BACK = 0x0405
GL_LEFT = 0x0406
GL_RIGHT = 0x0407
GL_FRONT_AND_BACK = 0x0408
GL_AUX0 = 0x0409
GL_AUX1 = 0x040A
GL_AUX2 = 0x040B
GL_AUX3 = 0x040C
GL_NO_ERROR = 0
GL_INVALID_ENUM = 0x0500
GL_INVALID_VALUE = 0x0501
GL_INVALID_OPERATION = 0x0502
GL_STACK_OVERFLOW = 0x0503
GL_STACK_UNDERFLOW = 0x0504
GL_OUT_OF_MEMORY = 0x0505
GL_2D = 0x0600
GL_3D = 0x0601
GL_3D_COLOR = 0x0602
GL_3D_COLOR_TEXTURE = 0x0603
GL_4D_COLOR_TEXTURE = 0x0604
GL_PASS_THROUGH_TOKEN = 0x0700
GL_POINT_TOKEN = 0x0701
GL_LINE_TOKEN = 0x0702
GL_POLYGON_TOKEN = 0x0703
GL_BITMAP_TOKEN = 0x0704
GL_DRAW_PIXEL_TOKEN = 0x0705
GL_COPY_PIXEL_TOKEN = 0x0706
GL_LINE_RESET_TOKEN = 0x0707
GL_EXP = 0x0800
GL_EXP2 = 0x0801
GL_CW = 0x0900
GL_CCW = 0x0901
GL_COEFF = 0x0A00
GL_ORDER = 0x0A01
GL_DOMAIN = 0x0A02
GL_CURRENT_COLOR = 0x0B00
GL_CURRENT_INDEX = 0x0B01
GL_CURRENT_NORMAL = 0x0B02
GL_CURRENT_TEXTURE_COORDS = 0x0B03
GL_CURRENT_RASTER_COLOR = 0x0B04
GL_CURRENT_RASTER_INDEX = 0x0B05
GL_CURRENT_RASTER_TEXTURE_COORDS = 0x0B06
GL_CURRENT_RASTER_POSITION = 0x0B07
GL_CURRENT_RASTER_POSITION_VALID = 0x0B08
GL_CURRENT_RASTER_DISTANCE = 0x0B09
GL_POINT_SMOOTH = 0x0B10
GL_POINT_SIZE = 0x0B11
GL_POINT_SIZE_RANGE = 0x0B12
GL_POINT_SIZE_GRANULARITY = 0x0B13
GL_LINE_SMOOTH = 0x0B20
GL_LINE_WIDTH = 0x0B21
GL_LINE_WIDTH_RANGE = 0x0B22
GL_LINE_WIDTH_GRANULARITY = 0x0B23
GL_LINE_STIPPLE = 0x0B24
GL_LINE_STIPPLE_PATTERN = 0x0B25
GL_LINE_STIPPLE_REPEAT = 0x0B26
GL_LIST_MODE = 0x0B30
GL_MAX_LIST_NESTING = 0x0B31
GL_LIST_BASE = 0x0B32
GL_LIST_INDEX = 0x0B33
GL_POLYGON_MODE = 0x0B40
GL_POLYGON_SMOOTH = 0x0B41
GL_POLYGON_STIPPLE = 0x0B42
GL_EDGE_FLAG = 0x0B43
GL_CULL_FACE = 0x0B44
GL_CULL_FACE_MODE = 0x0B45
GL_FRONT_FACE = 0x0B46
GL_LIGHTING = 0x0B50
GL_LIGHT_MODEL_LOCAL_VIEWER = 0x0B51
GL_LIGHT_MODEL_TWO_SIDE = 0x0B52
GL_LIGHT_MODEL_AMBIENT = 0x0B53
GL_SHADE_MODEL = 0x0B54
GL_COLOR_MATERIAL_FACE = 0x0B55
GL_COLOR_MATERIAL_PARAMETER = 0x0B56
GL_COLOR_MATERIAL = 0x0B57
GL_FOG = 0x0B60
GL_FOG_INDEX = 0x0B61
GL_FOG_DENSITY = 0x0B62
GL_FOG_START = 0x0B63
GL_FOG_END = 0x0B64
GL_FOG_MODE = 0x0B65
GL_FOG_COLOR = 0x0B66
GL_DEPTH_RANGE = 0x0B70
GL_DEPTH_TEST = 0x0B71
GL_DEPTH_WRITEMASK = 0x0B72
GL_DEPTH_CLEAR_VALUE = 0x0B73
GL_DEPTH_FUNC = 0x0B74
GL_ACCUM_CLEAR_VALUE = 0x0B80
GL_STENCIL_TEST = 0x0B90
GL_STENCIL_CLEAR_VALUE = 0x0B91
GL_STENCIL_FUNC = 0x0B92
GL_STENCIL_VALUE_MASK = 0x0B93
GL_STENCIL_FAIL = 0x0B94
GL_STENCIL_PASS_DEPTH_FAIL = 0x0B95
GL_STENCIL_PASS_DEPTH_PASS = 0x0B96
GL_STENCIL_REF = 0x0B97
GL_STENCIL_WRITEMASK = 0x0B98
GL_MATRIX_MODE = 0x0BA0
GL_NORMALIZE = 0x0BA1
GL_VIEWPORT = 0x0BA2
GL_MODELVIEW_STACK_DEPTH = 0x0BA3
GL_PROJECTION_STACK_DEPTH = 0x0BA4
GL_TEXTURE_STACK_DEPTH = 0x0BA5
GL_MODELVIEW_MATRIX = 0x0BA6
GL_PROJECTION_MATRIX = 0x0BA7
GL_TEXTURE_MATRIX = 0x0BA8
GL_ATTRIB_STACK_DEPTH = 0x0BB0
GL_CLIENT_ATTRIB_STACK_DEPTH = 0x0BB1
GL_ALPHA_TEST = 0x0BC0
GL_ALPHA_TEST_FUNC = 0x0BC1
GL_ALPHA_TEST_REF = 0x0BC2
GL_DITHER = 0x0BD0
GL_BLEND_DST = 0x0BE0
GL_BLEND_SRC = 0x0BE1
GL_BLEND = 0x0BE2
GL_LOGIC_OP_MODE = 0x0BF0
GL_INDEX_LOGIC_OP = 0x0BF1
GL_COLOR_LOGIC_OP = 0x0BF2
GL_AUX_BUFFERS = 0x0C00
GL_DRAW_BUFFER = 0x0C01
GL_READ_BUFFER = 0x0C02
GL_SCISSOR_BOX = 0x0C10
GL_SCISSOR_TEST = 0x0C11
GL_INDEX_CLEAR_VALUE = 0x0C20
GL_INDEX_WRITEMASK = 0x0C21
GL_COLOR_CLEAR_VALUE = 0x0C22
GL_COLOR_WRITEMASK = 0x0C23
GL_INDEX_MODE = 0x0C30
GL_RGBA_MODE = 0x0C31
GL_DOUBLEBUFFER = 0x0C32
GL_STEREO = 0x0C33
GL_RENDER_MODE = 0x0C40
GL_PERSPECTIVE_CORRECTION_HINT = 0x0C50
GL_POINT_SMOOTH_HINT = 0x0C51
GL_LINE_SMOOTH_HINT = 0x0C52
GL_POLYGON_SMOOTH_HINT = 0x0C53
GL_FOG_HINT = 0x0C54
GL_TEXTURE_GEN_S = 0x0C60
GL_TEXTURE_GEN_T = 0x0C61
GL_TEXTURE_GEN_R = 0x0C62
GL_TEXTURE_GEN_Q = 0x0C63
GL_PIXEL_MAP_I_TO_I = 0x0C70
GL_PIXEL_MAP_S_TO_S = 0x0C71
GL_PIXEL_MAP_I_TO_R = 0x0C72
GL_PIXEL_MAP_I_TO_G = 0x0C73
GL_PIXEL_MAP_I_TO_B = 0x0C74
GL_PIXEL_MAP_I_TO_A = 0x0C75
GL_PIXEL_MAP_R_TO_R = 0x0C76
GL_PIXEL_MAP_G_TO_G = 0x0C77
GL_PIXEL_MAP_B_TO_B = 0x0C78
GL_PIXEL_MAP_A_TO_A = 0x0C79
GL_PIXEL_MAP_I_TO_I_SIZE = 0x0CB0
GL_PIXEL_MAP_S_TO_S_SIZE = 0x0CB1
GL_PIXEL_MAP_I_TO_R_SIZE = 0x0CB2
GL_PIXEL_MAP_I_TO_G_SIZE = 0x0CB3
GL_PIXEL_MAP_I_TO_B_SIZE = 0x0CB4
GL_PIXEL_MAP_I_TO_A_SIZE = 0x0CB5
GL_PIXEL_MAP_R_TO_R_SIZE = 0x0CB6
GL_PIXEL_MAP_G_TO_G_SIZE = 0x0CB7
GL_PIXEL_MAP_B_TO_B_SIZE = 0x0CB8
GL_PIXEL_MAP_A_TO_A_SIZE = 0x0CB9
GL_UNPACK_SWAP_BYTES = 0x0CF0
GL_UNPACK_LSB_FIRST = 0x0CF1
GL_UNPACK_ROW_LENGTH = 0x0CF2
GL_UNPACK_SKIP_ROWS = 0x0CF3
GL_UNPACK_SKIP_PIXELS = 0x0CF4
GL_UNPACK_ALIGNMENT = 0x0CF5
GL_PACK_SWAP_BYTES = 0x0D00
GL_PACK_LSB_FIRST = 0x0D01
GL_PACK_ROW_LENGTH = 0x0D02
GL_PACK_SKIP_ROWS = 0x0D03
GL_PACK_SKIP_PIXELS = 0x0D04
GL_PACK_ALIGNMENT = 0x0D05
GL_MAP_COLOR = 0x0D10
GL_MAP_STENCIL = 0x0D11
GL_INDEX_SHIFT = 0x0D12
GL_INDEX_OFFSET = 0x0D13
GL_RED_SCALE = 0x0D14
GL_RED_BIAS = 0x0D15
GL_ZOOM_X = 0x0D16
GL_ZOOM_Y = 0x0D17
GL_GREEN_SCALE = 0x0D18
GL_GREEN_BIAS = 0x0D19
GL_BLUE_SCALE = 0x0D1A
GL_BLUE_BIAS = 0x0D1B
GL_ALPHA_SCALE = 0x0D1C
GL_ALPHA_BIAS = 0x0D1D
GL_DEPTH_SCALE = 0x0D1E
GL_DEPTH_BIAS = 0x0D1F
GL_MAX_EVAL_ORDER = 0x0D30
GL_MAX_LIGHTS = 0x0D31
GL_MAX_CLIP_PLANES = 0x0D32
GL_MAX_TEXTURE_SIZE = 0x0D33
GL_MAX_PIXEL_MAP_TABLE = 0x0D34
GL_MAX_ATTRIB_STACK_DEPTH = 0x0D35
GL_MAX_MODELVIEW_STACK_DEPTH = 0x0D36
GL_MAX_NAME_STACK_DEPTH = 0x0D37
GL_MAX_PROJECTION_STACK_DEPTH = 0x0D38
GL_MAX_TEXTURE_STACK_DEPTH = 0x0D39
GL_MAX_VIEWPORT_DIMS = 0x0D3A
GL_MAX_CLIENT_ATTRIB_STACK_DEPTH = 0x0D3B
GL_SUBPIXEL_BITS = 0x0D50
GL_INDEX_BITS = 0x0D51
GL_RED_BITS = 0x0D52
GL_GREEN_BITS = 0x0D53
GL_BLUE_BITS = 0x0D54
GL_ALPHA_BITS = 0x0D55
GL_DEPTH_BITS = 0x0D56
GL_STENCIL_BITS = 0x0D57
GL_ACCUM_RED_BITS = 0x0D58
GL_ACCUM_GREEN_BITS = 0x0D59
GL_ACCUM_BLUE_BITS = 0x0D5A
GL_ACCUM_ALPHA_BITS = 0x0D5B
GL_NAME_STACK_DEPTH = 0x0D70
GL_AUTO_NORMAL = 0x0D80
GL_MAP1_COLOR_4 = 0x0D90
GL_MAP1_INDEX = 0x0D91
GL_MAP1_NORMAL = 0x0D92
GL_MAP1_TEXTURE_COORD_1 = 0x0D93
GL_MAP1_TEXTURE_COORD_2 = 0x0D94
GL_MAP1_TEXTURE_COORD_3 = 0x0D95
GL_MAP1_TEXTURE_COORD_4 = 0x0D96
GL_MAP1_VERTEX_3 = 0x0D97
GL_MAP1_VERTEX_4 = 0x0D98
GL_MAP2_COLOR_4 = 0x0DB0
GL_MAP2_INDEX = 0x0DB1
GL_MAP2_NORMAL = 0x0DB2
GL_MAP2_TEXTURE_COORD_1 = 0x0DB3
GL_MAP2_TEXTURE_COORD_2 = 0x0DB4
GL_MAP2_TEXTURE_COORD_3 = 0x0DB5
GL_MAP2_TEXTURE_COORD_4 = 0x0DB6
GL_MAP2_VERTEX_3 = 0x0DB7
GL_MAP2_VERTEX_4 = 0x0DB8
GL_MAP1_GRID_DOMAIN = 0x0DD0
GL_MAP1_GRID_SEGMENTS = 0x0DD1
GL_MAP2_GRID_DOMAIN = 0x0DD2
GL_MAP2_GRID_SEGMENTS = 0x0DD3
GL_TEXTURE_1D = 0x0DE0
GL_TEXTURE_2D = 0x0DE1
GL_FEEDBACK_BUFFER_POINTER = 0x0DF0
GL_FEEDBACK_BUFFER_SIZE = 0x0DF1
GL_FEEDBACK_BUFFER_TYPE = 0x0DF2
GL_SELECTION_BUFFER_POINTER = 0x0DF3
GL_SELECTION_BUFFER_SIZE = 0x0DF4
GL_TEXTURE_WIDTH = 0x1000
GL_TEXTURE_HEIGHT = 0x1001
GL_TEXTURE_INTERNAL_FORMAT = 0x1003
GL_TEXTURE_BORDER_COLOR = 0x1004
GL_TEXTURE_BORDER = 0x1005
GL_DONT_CARE = 0x1100
GL_FASTEST = 0x1101
GL_NICEST = 0x1102
GL_LIGHT0 = 0x4000
GL_LIGHT1 = 0x4001
GL_LIGHT2 = 0x4002
GL_LIGHT3 = 0x4003
GL_LIGHT4 = 0x4004
GL_LIGHT5 = 0x4005
GL_LIGHT6 = 0x4006
GL_LIGHT7 = 0x4007
GL_AMBIENT = 0x1200
GL_DIFFUSE = 0x1201
GL_SPECULAR = 0x1202
GL_POSITION = 0x1203
GL_SPOT_DIRECTION = 0x1204
GL_SPOT_EXPONENT = 0x1205
GL_SPOT_CUTOFF = 0x1206
GL_CONSTANT_ATTENUATION = 0x1207
GL_LINEAR_ATTENUATION = 0x1208
GL_QUADRATIC_ATTENUATION = 0x1209
GL_COMPILE = 0x1300
GL_COMPILE_AND_EXECUTE = 0x1301
GL_CLEAR = 0x1500
GL_AND = 0x1501
GL_AND_REVERSE = 0x1502
GL_COPY = 0x1503
GL_AND_INVERTED = 0x1504
GL_NOOP = 0x1505
GL_XOR = 0x1506
GL_OR = 0x1507
GL_NOR = 0x1508
GL_EQUIV = 0x1509
GL_INVERT = 0x150A
GL_OR_REVERSE = 0x150B
GL_COPY_INVERTED = 0x150C
GL_OR_INVERTED = 0x150D
GL_NAND = 0x150E
GL_SET = 0x150F
GL_EMISSION = 0x1600
GL_SHININESS = 0x1601
GL_AMBIENT_AND_DIFFUSE = 0x1602
GL_COLOR_INDEXES = 0x1603
GL_MODELVIEW = 0x1700
GL_PROJECTION = 0x1701
GL_TEXTURE = 0x1702
GL_COLOR = 0x1800
GL_DEPTH = 0x1801
GL_STENCIL = 0x1802
GL_COLOR_INDEX = 0x1900
GL_STENCIL_INDEX = 0x1901
GL_DEPTH_COMPONENT = 0x1902
GL_RED = 0x1903
GL_GREEN = 0x1904
GL_BLUE = 0x1905
GL_ALPHA = 0x1906
GL_RGB = 0x1907
GL_RGBA = 0x1908
GL_LUMINANCE = 0x1909
GL_LUMINANCE_ALPHA = 0x190A
GL_BITMAP = 0x1A00
GL_POINT = 0x1B00
GL_LINE = 0x1B01
GL_FILL = 0x1B02
GL_RENDER = 0x1C00
GL_FEEDBACK = 0x1C01
GL_SELECT = 0x1C02
GL_FLAT = 0x1D00
GL_SMOOTH = 0x1D01
GL_KEEP = 0x1E00
GL_REPLACE = 0x1E01
GL_INCR = 0x1E02
GL_DECR = 0x1E03
GL_VENDOR = 0x1F00
GL_RENDERER = 0x1F01
GL_VERSION = 0x1F02
GL_EXTENSIONS = 0x1F03
GL_S = 0x2000
GL_T = 0x2001
GL_R = 0x2002
GL_Q = 0x2003
GL_MODULATE = 0x2100
GL_DECAL = 0x2101
GL_TEXTURE_ENV_MODE = 0x2200
GL_TEXTURE_ENV_COLOR = 0x2201
GL_TEXTURE_ENV = 0x2300
GL_EYE_LINEAR = 0x2400
GL_OBJECT_LINEAR = 0x2401
GL_SPHERE_MAP = 0x2402
GL_TEXTURE_GEN_MODE = 0x2500
GL_OBJECT_PLANE = 0x2501
GL_EYE_PLANE = 0x2502
GL_NEAREST = 0x2600
GL_LINEAR = 0x2601
GL_NEAREST_MIPMAP_NEAREST = 0x2700
GL_LINEAR_MIPMAP_NEAREST = 0x2701
GL_NEAREST_MIPMAP_LINEAR = 0x2702
GL_LINEAR_MIPMAP_LINEAR = 0x2703
GL_TEXTURE_MAG_FILTER = 0x2800
GL_TEXTURE_MIN_FILTER = 0x2801
GL_TEXTURE_WRAP_S = 0x2802
GL_TEXTURE_WRAP_T = 0x2803
GL_CLAMP = 0x2900
GL_REPEAT = 0x2901
GL_CLIENT_PIXEL_STORE_BIT = 0x00000001
GL_CLIENT_VERTEX_ARRAY_BIT = 0x00000002
GL_CLIENT_ALL_ATTRIB_BITS = 0xffffffff
GL_POLYGON_OFFSET_FACTOR = 0x8038
GL_POLYGON_OFFSET_UNITS = 0x2A00
GL_POLYGON_OFFSET_POINT = 0x2A01
GL_POLYGON_OFFSET_LINE = 0x2A02
GL_POLYGON_OFFSET_FILL = 0x8037
GL_ALPHA4 = 0x803B
GL_ALPHA8 = 0x803C
GL_ALPHA12 = 0x803D
GL_ALPHA16 = 0x803E
GL_LUMINANCE4 = 0x803F
GL_LUMINANCE8 = 0x8040
GL_LUMINANCE12 = 0x8041
GL_LUMINANCE16 = 0x8042
GL_LUMINANCE4_ALPHA4 = 0x8043
GL_LUMINANCE6_ALPHA2 = 0x8044
GL_LUMINANCE8_ALPHA8 = 0x8045
GL_LUMINANCE12_ALPHA4 = 0x8046
GL_LUMINANCE12_ALPHA12 = 0x8047
GL_LUMINANCE16_ALPHA16 = 0x8048
GL_INTENSITY = 0x8049
GL_INTENSITY4 = 0x804A
GL_INTENSITY8 = 0x804B
GL_INTENSITY12 = 0x804C
GL_INTENSITY16 = 0x804D
GL_R3_G3_B2 = 0x2A10
GL_RGB4 = 0x804F
GL_RGB5 = 0x8050
GL_RGB8 = 0x8051
GL_RGB10 = 0x8052
GL_RGB12 = 0x8053
GL_RGB16 = 0x8054
GL_RGBA2 = 0x8055
GL_RGBA4 = 0x8056
GL_RGB5_A1 = 0x8057
GL_RGBA8 = 0x8058
GL_RGB10_A2 = 0x8059
GL_RGBA12 = 0x805A
GL_RGBA16 = 0x805B
GL_TEXTURE_RED_SIZE = 0x805C
GL_TEXTURE_GREEN_SIZE = 0x805D
GL_TEXTURE_BLUE_SIZE = 0x805E
GL_TEXTURE_ALPHA_SIZE = 0x805F
GL_TEXTURE_LUMINANCE_SIZE = 0x8060
GL_TEXTURE_INTENSITY_SIZE = 0x8061
GL_PROXY_TEXTURE_1D = 0x8063
GL_PROXY_TEXTURE_2D = 0x8064
GL_TEXTURE_PRIORITY = 0x8066
GL_TEXTURE_RESIDENT = 0x8067
GL_TEXTURE_BINDING_1D = 0x8068
GL_TEXTURE_BINDING_2D = 0x8069
GL_VERTEX_ARRAY = 0x8074
GL_NORMAL_ARRAY = 0x8075
GL_COLOR_ARRAY = 0x8076
GL_INDEX_ARRAY = 0x8077
GL_TEXTURE_COORD_ARRAY = 0x8078
GL_EDGE_FLAG_ARRAY = 0x8079
GL_VERTEX_ARRAY_SIZE = 0x807A
GL_VERTEX_ARRAY_TYPE = 0x807B
GL_VERTEX_ARRAY_STRIDE = 0x807C
GL_NORMAL_ARRAY_TYPE = 0x807E
GL_NORMAL_ARRAY_STRIDE = 0x807F
GL_COLOR_ARRAY_SIZE = 0x8081
GL_COLOR_ARRAY_TYPE = 0x8082
GL_COLOR_ARRAY_STRIDE = 0x8083
GL_INDEX_ARRAY_TYPE = 0x8085
GL_INDEX_ARRAY_STRIDE = 0x8086
GL_TEXTURE_COORD_ARRAY_SIZE = 0x8088
GL_TEXTURE_COORD_ARRAY_TYPE = 0x8089
GL_TEXTURE_COORD_ARRAY_STRIDE = 0x808A
GL_EDGE_FLAG_ARRAY_STRIDE = 0x808C
GL_VERTEX_ARRAY_POINTER = 0x808E
GL_NORMAL_ARRAY_POINTER = 0x808F
GL_COLOR_ARRAY_POINTER = 0x8090
GL_INDEX_ARRAY_POINTER = 0x8091
GL_TEXTURE_COORD_ARRAY_POINTER = 0x8092
GL_EDGE_FLAG_ARRAY_POINTER = 0x8093
GL_V2F = 0x2A20
GL_V3F = 0x2A21
GL_C4UB_V2F = 0x2A22
GL_C4UB_V3F = 0x2A23
GL_C3F_V3F = 0x2A24
GL_N3F_V3F = 0x2A25
GL_C4F_N3F_V3F = 0x2A26
GL_T2F_V3F = 0x2A27
GL_T4F_V4F = 0x2A28
GL_T2F_C4UB_V3F = 0x2A29
GL_T2F_C3F_V3F = 0x2A2A
GL_T2F_N3F_V3F = 0x2A2B
GL_T2F_C4F_N3F_V3F = 0x2A2C
GL_T4F_C4F_N3F_V4F = 0x2A2D
GL_EXT_vertex_array = 1
GL_WIN_swap_hint = 1
GL_EXT_bgra = 1
GL_EXT_paletted_texture = 1
GL_VERTEX_ARRAY_EXT = 0x8074
GL_NORMAL_ARRAY_EXT = 0x8075
GL_COLOR_ARRAY_EXT = 0x8076
GL_INDEX_ARRAY_EXT = 0x8077
GL_TEXTURE_COORD_ARRAY_EXT = 0x8078
GL_EDGE_FLAG_ARRAY_EXT = 0x8079
GL_VERTEX_ARRAY_SIZE_EXT = 0x807A
GL_VERTEX_ARRAY_TYPE_EXT = 0x807B
GL_VERTEX_ARRAY_STRIDE_EXT = 0x807C
GL_VERTEX_ARRAY_COUNT_EXT = 0x807D
GL_NORMAL_ARRAY_TYPE_EXT = 0x807E
GL_NORMAL_ARRAY_STRIDE_EXT = 0x807F
GL_NORMAL_ARRAY_COUNT_EXT = 0x8080
GL_COLOR_ARRAY_SIZE_EXT = 0x8081
GL_COLOR_ARRAY_TYPE_EXT = 0x8082
GL_COLOR_ARRAY_STRIDE_EXT = 0x8083
GL_COLOR_ARRAY_COUNT_EXT = 0x8084
GL_INDEX_ARRAY_TYPE_EXT = 0x8085
GL_INDEX_ARRAY_STRIDE_EXT = 0x8086
GL_INDEX_ARRAY_COUNT_EXT = 0x8087
GL_TEXTURE_COORD_ARRAY_SIZE_EXT = 0x8088
GL_TEXTURE_COORD_ARRAY_TYPE_EXT = 0x8089
GL_TEXTURE_COORD_ARRAY_STRIDE_EXT = 0x808A
GL_TEXTURE_COORD_ARRAY_COUNT_EXT = 0x808B
GL_EDGE_FLAG_ARRAY_STRIDE_EXT = 0x808C
GL_EDGE_FLAG_ARRAY_COUNT_EXT = 0x808D
GL_VERTEX_ARRAY_POINTER_EXT = 0x808E
GL_NORMAL_ARRAY_POINTER_EXT = 0x808F
GL_COLOR_ARRAY_POINTER_EXT = 0x8090
GL_INDEX_ARRAY_POINTER_EXT = 0x8091
GL_TEXTURE_COORD_ARRAY_POINTER_EXT = 0x8092
GL_EDGE_FLAG_ARRAY_POINTER_EXT = 0x8093
GL_DOUBLE_EXT = GL_DOUBLE
GL_BGR_EXT = 0x80E0
GL_BGRA_EXT = 0x80E1
GL_COLOR_TABLE_FORMAT_EXT = 0x80D8
GL_COLOR_TABLE_WIDTH_EXT = 0x80D9
GL_COLOR_TABLE_RED_SIZE_EXT = 0x80DA
GL_COLOR_TABLE_GREEN_SIZE_EXT = 0x80DB
GL_COLOR_TABLE_BLUE_SIZE_EXT = 0x80DC
GL_COLOR_TABLE_ALPHA_SIZE_EXT = 0x80DD
GL_COLOR_TABLE_LUMINANCE_SIZE_EXT = 0x80DE
GL_COLOR_TABLE_INTENSITY_SIZE_EXT = 0x80DF
GL_COLOR_INDEX1_EXT = 0x80E2
GL_COLOR_INDEX2_EXT = 0x80E3
GL_COLOR_INDEX4_EXT = 0x80E4
GL_COLOR_INDEX8_EXT = 0x80E5
GL_COLOR_INDEX12_EXT = 0x80E6
GL_COLOR_INDEX16_EXT = 0x80E7
GL_LOGIC_OP = GL_INDEX_LOGIC_OP
GL_TEXTURE_COMPONENTS = GL_TEXTURE_INTERNAL_FORMAT

   */
}
