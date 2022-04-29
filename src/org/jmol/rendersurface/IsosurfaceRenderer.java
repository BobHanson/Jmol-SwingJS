/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-25 09:53:35 -0500 (Wed, 25 Apr 2007) $
 * $Revision: 7491 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
package org.jmol.rendersurface;

import org.jmol.jvxl.data.JvxlCoder;
import org.jmol.jvxl.readers.Parameters;
import org.jmol.render.MeshRenderer;
import org.jmol.script.T;
import org.jmol.shapesurface.Isosurface;
import org.jmol.shapesurface.IsosurfaceMesh;
import org.jmol.util.C;
import org.jmol.util.GData;
import org.jmol.util.MeshSurface;

import javajs.util.Lst;
import org.jmol.util.Normix;
import org.jmol.viewer.JC;

import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.T3d;
import javajs.util.V3d;

public class IsosurfaceRenderer extends MeshRenderer {

  protected boolean iHideBackground;
  protected boolean isBicolorMap;
  protected short backgroundColix;
  protected int nError = 0;
  protected double[] vertexValues;
  protected IsosurfaceMesh imesh;
  private Isosurface isosurface;
  private boolean isNavigationMode;
  private boolean iShowNormals;
  private boolean showNumbers;
  private Boolean showKey;
  private boolean hasColorRange;
  private int meshScale = -1;
  private int mySlabValue;
  private int globalSlabValue;

  @Override
  protected boolean render() {
    return renderIso();
  }

  protected boolean renderIso() {    
    setGlobals();
    for (int i = isosurface.meshCount; --i >= 0;) {
      mesh = imesh = (IsosurfaceMesh) isosurface.meshes[i];
      if (imesh.connectedAtoms != null && !vwr.ms.at[imesh.connectedAtoms[0]].checkVisible())
        continue;
      hasColorRange = false;
      if (renderMeshSlab()) {
        renderInfo();
        if (isExport && isGhostPass) {
          exportPass = 1;
          renderMeshSlab();
          exportPass = 2;
        }
      }
    }
    return needTranslucent;
  }

  private void setGlobals() {
    needTranslucent = false;
    antialias = g3d.isAntialiased(); 
    iShowNormals = vwr.getBoolean(T.testflag4);
    showNumbers = vwr.getBoolean(T.testflag3);
    isosurface = (Isosurface) shape;
    // exporters will do two passes here if there is translucency
    // first pass is #2 (translucent), then #1 (opaque).
    exportPass = (isExport ? 2 : 0); 
    isNavigationMode = vwr.getBoolean(T.navigationmode);
    showKey = (vwr.getBoolean(T.isosurfacekey) ? Boolean.TRUE : null);
    isosurface.keyXy = null;
    meshScale = -1;
    globalSlabValue = vwr.gdata.slab;
    mySlabValue = (isNavigationMode ? (int) tm.getNavigationOffset().z : Integer.MAX_VALUE);
  }

  protected void renderInfo() {
    if (isExport || !hasColorRange || imesh.colorEncoder == null
        || Boolean.TRUE != showKey)
      return;
    showKey = Boolean.FALSE; // once only
    int[] colors = null;
    short[] colixes = null;
    Lst<Object>[] vContours = null;
    int n = 0;
    int type = 0;
    if (imesh.showContourLines) {
      vContours = imesh.getContours();
      if (vContours == null) {
        colixes = imesh.jvxlData.contourColixes;
        if (colixes == null)
          return;
        n = colixes.length;
      } else {
        n = vContours.length;
        type = 1;
      }
    } else {
      colors = imesh.colorEncoder
          .getColorSchemeArray(imesh.colorEncoder.currentPalette);
      n = (colors == null ? 0 : colors.length);
      type = 2;
    }
    if (n < 2)
      return;
    int factor = (antialias ? 2 : 1);
    int height = vwr.getScreenHeight() * factor;
    int dy = height / 2 / (n - 1);
    int y = height / 4 * 3 - dy;
    int x = 10 * factor;
    int dx = 20 * factor;
    isosurface.keyXy = new int[] { x / factor, 0, (x + dx) / factor,
        (y + dy) / factor, dy / factor };
    for (int i = 0; i < n; i++, y -= dy) {
      switch (type) {
      case 0:
        if (!g3d.setC(colixes[i]))
          return;
        break;
      case 1:
        if (!g3d.setC(((short[]) vContours[i].get(JvxlCoder.CONTOUR_COLIX))[0]))
          return;
        break;
      case 2:
        vwr.gdata.setColor(colors[i]);
        break;
      }
      g3d.fillTextRect(x, y, 5, Integer.MIN_VALUE, dx, dy);
    }
    isosurface.keyXy[1] = (y + dy) / factor;
  }
  
  private boolean renderMeshSlab() {
    volumeRender = (imesh.jvxlData.colorDensity && imesh.jvxlData.allowVolumeRender);
    int thisSlabValue = mySlabValue;
    frontOnly = mesh.frontOnly || shapeID == JC.SHAPE_LCAOCARTOON;
    isShell = mesh.isShell && shapeID != JC.SHAPE_LCAOCARTOON;
    if (!isNavigationMode) {
      meshSlabValue = imesh.jvxlData.slabValue; 
      if (meshSlabValue != Integer.MIN_VALUE  
          && imesh.jvxlData.isSlabbable) {
        P3d[] points = imesh.jvxlData.boundingBox;
        double z0 = Double.MAX_VALUE;
        double z1 = PT.FLOAT_MIN_SAFE;
        for (int i = points.length; --i >= 0;) {
          pt2f.setT(points[i]);
          tm.transformPt3f(pt2f, pt2f);
          if (pt2f.z < z0)
            z0 = pt2f.z;
          if (pt2f.z > z1)
            z1 = pt2f.z;
        }
        thisSlabValue = (int) Math.round(z0 + (z1 - z0) * (100d - meshSlabValue)/100);
        frontOnly &= (meshSlabValue >= 100);
        isShell &= (meshSlabValue >= 100);
      }
    }
    boolean tCover = vwr.gdata.translucentCoverOnly;
    // isShell??
    vwr.gdata.translucentCoverOnly = (frontOnly || !vwr.getBoolean(T.translucent));
    thePlane = imesh.jvxlData.jvxlPlane;
    vertexValues = mesh.vvs;
    boolean isOK;
    if (thisSlabValue != Integer.MAX_VALUE && imesh.jvxlData.isSlabbable) {
      g3d.setSlab(thisSlabValue);
      isOK = renderMesh2(mesh);
      g3d.setSlab(globalSlabValue);
    } else {
      isOK = renderMesh2(mesh);
    }
    vwr.gdata.translucentCoverOnly = tCover;
    return isOK;
  }
  
  @Override
  protected void render2(boolean isExport) {
    if (volumeRender) {
      renderPoints();
      return;
    }
    switch (imesh.dataType) {
    case Parameters.SURFACE_LONEPAIR:
      renderLonePair(false);
      return;
    case Parameters.SURFACE_RADICAL:
      renderLonePair(true);
      return;
    }
    isBicolorMap = imesh.jvxlData.isBicolorMap;
    render2b(isExport);
    if (!g3d.setC(C.BLACK)) // must be 1st pass
      return;
    if (imesh.showContourLines)
      renderContourLines();
  }
  
  private void renderLonePair(boolean isRadical) {
    pt2f.setT(vertices[1]);
    tm.transformPt3f(pt2f, pt2f);
    int r = (int) vwr.tm.scaleToScreen((int)pt2f.z, 100);
    if (r < 1)
      r = 1;
    if (!isRadical) {
      V3d v1 = new V3d();
      V3d v2 = new V3d();
      pt1f.setT(vertices[0]);
      tm.transformPt3f(pt1f, pt1f);
      v1.sub2(pt2f, pt1f);
      v2.set(v1.x, v1.y, v1.z + 1);
      v2.cross(v2,v1);
      v2.normalize();
      double f = vwr.tm.scaleToScreen((int)pt1f.z, 100);
      v2.scale(f);
      pt1f.add2(pt2f, v2);
      pt2f.sub(v2);
      screens[0].set((int) Math.round(pt1f.x),(int) Math.round(pt1f.y),(int) Math.round(pt1f.z));
      g3d.fillSphereI(r, screens[0]);
    }
    screens[1].set((int) Math.round(pt2f.x),(int) Math.round(pt2f.y),(int) Math.round(pt2f.z));
    g3d.fillSphereI(r, screens[1]);
  }
  
  private void renderContourLines() {
    // no check here for within distance
    Lst<Object>[] vContours = imesh.getContours();
    if (vContours == null) {
      if (imesh.jvxlData.contourValues != null)
        hasColorRange = true;
      return;
    }
    hasColorRange = (mesh.meshColix == 0);
    for (int i = vContours.length; --i >= 0;) {
      Lst<Object> v = vContours[i];
      if (v.size() < JvxlCoder.CONTOUR_POINTS)
        continue;
      colix = (mesh.meshColix == 0 ? ((short[]) v.get(JvxlCoder.CONTOUR_COLIX))[0]
          : mesh.meshColix);
      if (!g3d.setC(colix))
        return;
      int n = v.size() - 1;
      int diam = getDiameter();
      for (int j = JvxlCoder.CONTOUR_POINTS; j < n; j++) {
        T3d pt1 = (T3d) v.get(j);
        T3d pt2 = (T3d) v.get(++j);
        if (Double.isNaN(pt1.x) || Double.isNaN(pt2.x))
          break;
        tm.transformPtScrT3(pt1, pt1f);
        tm.transformPtScrT3(pt2, pt2f);
        pt1f.z -= 2;
        pt2f.z -= 2;
        if (!antialias && diam == 1) {
          g3d.drawLineAB(pt1f, pt2f);
        } else {
          g3d.fillCylinderBits(GData.ENDCAPS_HIDDEN, diam, pt1f,
              pt2f);
        }
      }
    }
  }
  
  @Override
  protected void renderPoints() {
    try {
      if (volumeRender)
        g3d.volumeRender(true);
      boolean slabPoints = ((volumeRender || mesh.pc == 0) && selectedPolyOnly);
      int incr = imesh.vertexIncrement;
      int diam;
      if (mesh.diameter <= 0) {
        diam = vwr.getInt(T.dotscale);
        frontOnly = isShell = false;
      } else {
        diam = vwr.getScreenDim() / (volumeRender ? 50 : 100);        
      }
      int ptSize = (int) Math.round(Double.isNaN(mesh.volumeRenderPointSize) ? 150 : mesh.volumeRenderPointSize * 1000);
      if (diam < 1)
        diam = 1;
      int cX = (showNumbers ? vwr.getScreenWidth() / 2 : 0);
      int cY = (showNumbers ? vwr.getScreenHeight() / 2 : 0);
      if (showNumbers)
        vwr.gdata.setFontBold("Monospaced", 24);
      for (int i = (!imesh.hasGridPoints || imesh.firstRealVertex < 0 ? 0
          : imesh.firstRealVertex); i < vertexCount; i += incr) {
        if (vertexValues != null && Double.isNaN(vertexValues[i]) || frontOnly
            && !isVisibleNormix(normixes[i]) 
            || imesh.jvxlData.thisSet != null
            && !imesh.jvxlData.thisSet.get(mesh.vertexSets[i]) || !mesh.isColorSolid
            && mesh.vcs != null && !setColix(mesh.vcs[i])
            || haveBsDisplay && !mesh.bsDisplay.get(i)
            || slabPoints && !bsPolygons.get(i))
          continue;
        hasColorRange = true; // maybe
        if (showNumbers && screens[i].z > 10
            && Math.abs(screens[i].x - cX) < 150
            && Math.abs(screens[i].y - cY) < 150) {
          String s = i
              + (mesh.isColorSolid ? "" : " " + mesh.vvs[i]);
          g3d.setC(C.BLACK);
          g3d.drawStringNoSlab(s, null, screens[i].x, screens[i].y,
              screens[i].z - 30, (short) 0);
        }
        if (volumeRender) {
          diam = (int) vwr.tm.scaleToScreen(screens[i].z, ptSize);
          if (diam < 1)
            diam = 1;
          g3d.volumeRender4(diam, screens[i].x, screens[i].y, screens[i].z);
        } else {
          g3d.fillSphereI(diam, screens[i]);
        }
      }
      if (incr == 3) {
        g3d.setC(isTranslucent ? C.getColixTranslucent3(
            C.GRAY, true, 0.5d) : C.GRAY);
        for (int i = 1; i < vertexCount; i += 3)
          g3d.fillCylinder(GData.ENDCAPS_SPHERICAL, diam / 4, screens[i],
              screens[i + 1]);
        g3d.setC(isTranslucent ? C.getColixTranslucent3(
            C.YELLOW, true, 0.5d) : C.YELLOW);
        for (int i = 1; i < vertexCount; i += 3)
          g3d.fillSphereI(diam, screens[i]);

        g3d.setC(isTranslucent ? C.getColixTranslucent3(
            C.BLUE, true, 0.5d) : C.BLUE);
        for (int i = 2; i < vertexCount; i += 3) {
          g3d.fillSphereI(diam, screens[i]);
        }
      }
    } catch (Throwable e) {
      // just in case, need to reset volume rendering
    }
    if (volumeRender)
      g3d.volumeRender(false);
  }

  @Override
  protected void renderTriangles(boolean fill, boolean iShowTriangles,
                                 boolean isExport) {
    g3d.addRenderer(T.triangles);
    int[][] polygonIndexes = mesh.pis;
    colix = (isGhostPass ? mesh.slabColix
        : !fill && mesh.meshColix != 0 ? mesh.meshColix : mesh.colix);
    short[] vertexColixes = (!fill && mesh.meshColix != 0 ? null
        : mesh.vcs);
    if (isTranslucentInherit)
      colix = C.copyColixTranslucency(mesh.slabColix, mesh.colix);
    g3d.setC(colix);
    boolean generateSet = isExport;
    // isShell???
    if (generateSet) {
      if (frontOnly && fill)
        frontOnly = false;
      bsPolygonsToExport.clearAll();
    }
    if (exportType == GData.EXPORT_CARTESIAN) {
      frontOnly = false;
    }
    boolean colorSolid = (isGhostPass && (!isBicolorMap)
        || vertexColixes == null || mesh.isColorSolid);
    boolean noColor = (isGhostPass && !isBicolorMap || vertexColixes == null || !fill
        && mesh.meshColix != 0);
    boolean isPlane = (imesh.jvxlData.jvxlPlane != null);
    short colix = this.colix;
    if (isPlane && !colorSolid && !fill && mesh.fillTriangles) {
      colorSolid = true;
      colix = C.BLACK;
    }
    /*  only an idea -- causes flickering
        if (isPlane && colorSolid) {
          g3d.setNoisySurfaceShade(screens[polygonIndexes[0][0]], 
              screens[polygonIndexes[mesh.polygonCount / 2][1]], screens[polygonIndexes[mesh.polygonCount - 1][2]]);
        }
    */
    boolean colorArrayed = (colorSolid && mesh.pcs != null);
    if (colorArrayed && !fill && mesh.fillTriangles)
      colorArrayed = false;
    short[] contourColixes = imesh.jvxlData.contourColixes;
    // two-sided means like a plane, with no front/back distinction

    hasColorRange = !colorSolid && !isBicolorMap;
    int diam = getDiameter();
    int i0 = 0;
    for (int i = mesh.pc; --i >= i0;) {
      int[] polygon = polygonIndexes[i];
      if (polygon == null || selectedPolyOnly && !bsPolygons.get(i))
        continue;
      int iA = polygon[0];
      int iB = polygon[1];
      int iC = polygon[2];
      if (imesh.jvxlData.thisSet != null && mesh.vertexSets != null
          && !imesh.jvxlData.thisSet.get(mesh.vertexSets[iA]))
        continue;
      if (haveBsDisplay
          && (!mesh.bsDisplay.get(iA) || !mesh.bsDisplay.get(iB) || !mesh.bsDisplay
              .get(iC)))
        continue;
      short nA = normixes[iA];
      short nB = normixes[iB];
      short nC = normixes[iC];
      int check = (frontOnly || isShell ? checkFront(nA, nB, nC) : 7);
      if (fill && check == 0)
        continue;
      short colixA, colixB, colixC;
      if (colorSolid) {
        if (colorArrayed && i < mesh.pcs.length) {
          short c = mesh.pcs[i];
          if (c == 0)
            continue;
          colix = c;
        }
        if (iShowTriangles)
          colix = (short) (Math.round(Math.random()*10)+ 5);
        colixA = colixB = colixC = colix;
      } else {
        colixA = vertexColixes[iA];
        colixB = vertexColixes[iB];
        colixC = vertexColixes[iC];
        if (isBicolorMap) {
          if (colixA != colixB || colixB != colixC)
            continue;
          if (isGhostPass) {
            colixA = colixB = colixC = C.copyColixTranslucency(mesh.slabColix,
                colixA);
          }
        }
      }
      if (fill) {
        if (generateSet) {
          bsPolygonsToExport.set(i);
          continue;
        }
        if (iB == iC) {
          setColix(colixA);
          if (iA == iB)
            g3d.fillSphereI(diam, screens[iA]);
          else
            g3d.fillCylinder(GData.ENDCAPS_SPHERICAL, diam, screens[iA],
                screens[iB]);
        //} else if (iShowTriangles) {
          //g3d.fillTriangle(screens[iA], colixA, nA, screens[iB], colixB, nB,
            //  screens[iC], colixC, nC, 0.1d);
        } else if (mesh.colorsExplicit) {
            vwr.gdata.setColor(polygon[MeshSurface.P_EXPLICIT_COLOR]);
            colixA = C.copyColixTranslucency(mesh.colix, (short) C.LAST_AVAILABLE_COLIX); 
            g3d.setC(colixA);
          g3d.fillTriangle3CN(screens[iA], colixA, nA, screens[iB], colixA, nB,
              screens[iC], colixA, nC);
        } else {
          if (isTranslucentInherit && vertexColixes != null) {
            colixA = C.copyColixTranslucency(mesh.slabColix, vertexColixes[iA]);
            colixB = C.copyColixTranslucency(mesh.slabColix, vertexColixes[iB]);
            colixC = C.copyColixTranslucency(mesh.slabColix, vertexColixes[iC]);
          }
          g3d.fillTriangle3CN(screens[iA], colixA, nA, screens[iB], colixB, nB,
              screens[iC], colixC, nC);
        }
        if (iShowNormals)
          renderNormals();
      } else {
        // mesh only
        // check: 1 (ab) | 2(bc) | 4(ac)
        check &= polygon[MeshSurface.P_CHECK];
        if (check == 0)
          continue;
        if (iShowTriangles)
          check = 7;
        pt1i.setT(screens[iA]);
        pt2i.setT(screens[iB]);
        pt3i.setT(screens[iC]);
        pt1i.z -= 2;
        pt2i.z -= 2;
        pt3i.z -= 2;
        if (noColor) {
        } else if (colorArrayed) {
          g3d.setC(mesh.fillTriangles ? C.BLACK : contourColixes[polygon[MeshSurface.P_CONTOUR]
              % contourColixes.length]);
        } else {
          drawTriangle(pt1i, colixA, pt2i, colixB, pt3i, colixC, check, diam);
          continue;
        }
        drawTriangle(pt1i, colix, pt2i, colix, pt3i, colix, check, diam);
      }
    }
    if (generateSet)
      exportSurface(colorSolid ? colix : 0);
  }

  private int getDiameter() {
    int diam;
    if (mesh.diameter <= 0) {
      diam = (meshScale < 0 ? meshScale = vwr.getInt(T.meshscale)
          : meshScale);
      if (antialias)
        diam *= 2;
    } else {
      diam = vwr.getScreenDim() / 100;
    }
    if (diam < 1)
      diam = 1;
    return diam;
  }

  private void renderNormals() {
    if (!g3d.setC(C.copyColixTranslucency(mesh.colix, C.WHITE)))
      return;
    vwr.gdata.setFontBold("Monospaced", 24);
    V3d[] vertexVectors = Normix.getVertexVectors();
    for (int i = vertexCount; --i >= 0;) {
      if (vertexValues != null && Double.isNaN(vertexValues[i]))
        continue;
      pt1f.setT(vertices[i]);
      short n = mesh.normixes[i];
      // -n is an intensity2sided and does not correspond to a true normal
      // index
      if (n >= 0) {
        pt2f.scaleAdd2(0.3d, vertexVectors[n], pt1f);
        tm.transformPtScrT3(pt2f, pt2f);
        pt1f.set(screens[i].x, screens[i].y, screens[i].z);
        g3d.drawLineAB(pt1f, pt2f);
      }
    }
  }

}
