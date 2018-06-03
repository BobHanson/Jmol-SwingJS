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
package org.jmol.render;



import javajs.util.AU;
import javajs.util.M4;
import javajs.util.P3;
import javajs.util.P3i;
import javajs.util.P4;
import javajs.util.T3;
import javajs.util.V3;

import org.jmol.api.SymmetryInterface;
import javajs.util.BS;
import org.jmol.script.T;
import org.jmol.shape.Mesh;
import org.jmol.util.C;
import org.jmol.util.GData;
import org.jmol.util.MeshSurface;
import org.jmol.util.SimpleUnitCell;

/**
 * an abstract class subclasssed by BioShapeRenderer, DrawRenderer, and IsosurfaceRenderer
 */
public abstract class MeshRenderer extends ShapeRenderer {

  protected Mesh mesh;
  protected T3[] vertices;
  protected short[] normixes;
  protected P3i[] screens;
  protected P3[] p3Screens;
  protected V3[] transformedVectors;
  protected int vertexCount;
  
  protected float imageFontScaling;
  protected float scalePixelsPerMicron;
  protected int diameter;
  protected float width;
  

  protected boolean isTranslucent;
  protected boolean frontOnly;
  protected boolean isShell;
  protected boolean antialias;
  protected boolean haveBsDisplay;
  protected boolean selectedPolyOnly;
  protected boolean isGhostPass;
  //protected boolean isPrecision; // DRAW, bioshape

  protected P4 thePlane;
  protected P3 latticeOffset = new P3();

  protected final P3 pt1f = new P3();
  protected final P3 pt2f = new P3();

  protected P3i pt1i = new P3i();
  protected P3i pt2i = new P3i();
  protected final P3i pt3i = new P3i();
  protected int exportPass;
  protected boolean needTranslucent;

  /**
   * overridden in BioShapeRenderer, DrawRenderer, and IsosurfaceRenderer
   * @param mesh
   * @return whether we need to show info
   */
  protected boolean renderMesh2(Mesh mesh) {
    this.mesh = mesh;
    if (!setVariables())
      return false;
    if (!doRender)
      return mesh.title != null;
    latticeOffset.set(0, 0, 0);
    if (mesh.modelIndex < 0
        || mesh.lattice == null && mesh.symops == null) {
      for (int i = vertexCount; --i >= 0;)
        if (vertices[i] != null)
          tm.transformPtScr(vertices[i], screens[i]);
      //if (isPrecision) 
        for (int i = vertexCount; --i >= 0;)
          if (vertices[i] != null)
            tm.transformPtScrT3(vertices[i], p3Screens[i]);

      render2(isExport);
    } else {
      P3 vTemp = new P3();
      SymmetryInterface unitcell = mesh.getUnitCell();
      if (unitcell != null) {
        if (mesh.symops != null) {
          if (mesh.symopNormixes == null)
            mesh.symopNormixes = AU.newShort2(mesh.symops.length);
          P3[] verticesTemp = null;
          int max = mesh.symops.length;
          short c = mesh.colix;
          for (int j = max; --j >= 0;) {
            M4 m = mesh.symops[j];
            if (m == null)
              continue;
            if (mesh.colorType == T.symop)
              mesh.colix = mesh.symopColixes[j];
            short[] normals = mesh.symopNormixes[j];
            boolean needNormals = (normals == null);
            verticesTemp = (needNormals ? new P3[vertexCount] : null);
            for (int i = vertexCount; --i >= 0;) {
              vTemp.setT(vertices[i]);
              unitcell.toFractional(vTemp, true);
              m.rotTrans(vTemp);
              unitcell.toCartesian(vTemp, true);
              tm.transformPtScr(vTemp, screens[i]);
              if (needNormals) {
                verticesTemp[i] = vTemp;
                vTemp = new P3();
              }
            }
            if (needNormals)
              normixes = mesh.symopNormixes[j] = mesh.setNormixes(mesh
                  .getNormals(verticesTemp, null));
            else
              normixes = mesh.normixes = mesh.symopNormixes[j];
            render2(isExport);
          }
          mesh.colix = c;
        } else {
          P3i minXYZ = new P3i();
          P3i maxXYZ = P3i.new3((int) mesh.lattice.x, (int) mesh.lattice.y,
              (int) mesh.lattice.z);
          SimpleUnitCell.setMinMaxLatticeParameters((int) unitcell.getUnitCellInfoType(SimpleUnitCell.INFO_DIMENSIONS), minXYZ, maxXYZ, 0);
          for (int tx = minXYZ.x; tx < maxXYZ.x; tx++)
            for (int ty = minXYZ.y; ty < maxXYZ.y; ty++)
              for (int tz = minXYZ.z; tz < maxXYZ.z; tz++) {
                latticeOffset.set(tx, ty, tz);
                unitcell.toCartesian(latticeOffset, false);
                for (int i = vertexCount; --i >= 0;) {
                  vTemp.add2(vertices[i], latticeOffset);
                  tm.transformPtScr(vTemp, screens[i]);
                }
                render2(isExport);
              }
        }
      }
    }

    if (screens != null)
      vwr.freeTempScreens(screens);
    if (p3Screens != null)
      vwr.freeTempPoints(p3Screens);
    return true;
  }

  private boolean doRender;
  protected boolean volumeRender;
  protected BS bsPolygons;
  protected boolean isTranslucentInherit;
  protected boolean renderLow;
  protected int meshSlabValue = 100;
  private boolean showTriangles;
  protected boolean forceShowTriangles;  
  
  private boolean setVariables() {
    if (mesh.visibilityFlags == 0)
      return false;
    forceShowTriangles = vwr.getBoolean(T.testflag3);
    showTriangles = forceShowTriangles || mesh.showTriangles;
    if (mesh.bsSlabGhost != null)
      g3d.setC(mesh.slabColix); // forces a second pass
    if (mesh.colorsExplicit)
      g3d.setC((short) C.LAST_AVAILABLE_COLIX);
    isGhostPass = (mesh.bsSlabGhost != null && (isExport ? exportPass == 2
        : vwr.gdata.isPass2));
    isTranslucentInherit = (isGhostPass && C.getColixTranslucent3(mesh.slabColix, false, 0)== C.INHERIT_COLOR);
    isTranslucent = isGhostPass
        || C.renderPass2(mesh.colix);
    if (isTranslucent || volumeRender || mesh.bsSlabGhost != null)
      needTranslucent = true;
    doRender = (setColix(mesh.colix) || mesh.showContourLines);
    if (!doRender || isGhostPass && !(doRender = g3d.setC(mesh.slabColix))) {
      vertices = mesh.vs;
      if (needTranslucent)
        g3d.setC(C.getColixTranslucent3(C.BLACK, true, 0.5f));
      return true;
    }
    if (mesh.isModelConnected)
      mesh.mat4 = ms.am[mesh.modelIndex].mat4;
    vertices = (mesh.scale3d == 0 && mesh.mat4 == null ? mesh.vs : mesh.getOffsetVertices(thePlane));
    if (mesh.lineData == null) {
      // not a draw 
      if ((vertexCount = mesh.vc) == 0)
        return false;
      normixes = mesh.normixes;
      if (normixes == null  && mesh.pc >= 0 || vertices == null)
        return false;
      // this can happen when user switches windows
      // during a surface calculation
      haveBsDisplay = (mesh.bsDisplay != null);
      // mesh.bsSlabDisplay is a temporary slab effect 
      // that is reversible; these are the polygons to display
      selectedPolyOnly = (isGhostPass || mesh.bsSlabDisplay != null);
      bsPolygons = (isGhostPass ? mesh.bsSlabGhost
          : selectedPolyOnly ? mesh.bsSlabDisplay : null);
      
      renderLow = (!isExport && !vwr.checkMotionRendering(T.mesh));
      boolean allowFrontOnly = (!mesh.isTwoSided && !selectedPolyOnly 
          && (meshSlabValue == Integer.MIN_VALUE || meshSlabValue >= 100));
      frontOnly = renderLow || mesh.frontOnly && !tm.slabEnabled && allowFrontOnly;
      isShell = mesh.isShell && allowFrontOnly;
      screens = vwr.allocTempScreens(vertexCount);
      //if (isPrecision)
        p3Screens = vwr.allocTempPoints(vertexCount);
      if (frontOnly || isShell)
        transformedVectors = vwr.gdata.getTransformedVertexVectors();
      if (transformedVectors == null)
        frontOnly = isShell = false;
    }
    return true;
  }

  public boolean setColix(short colix) {
    if (isGhostPass)
      return true;
    if (volumeRender && !isTranslucent)
      colix = C.getColixTranslucent3(colix, true, 0.8f);
    this.colix = colix;
    if (C.isColixLastAvailable(colix))
      vwr.gdata.setColor(mesh.color);
    return g3d.setC(colix);
  }

  // all of the following methods are overridden in subclasses
  // DO NOT change parameters without first checking for the
  // same method in a subclass.
  
  /**
   * @param i 
   * @return T/F
   * 
   */
  protected boolean isPolygonDisplayable(int i) {
    return true;
  }

  /**
   * Overridden in DrawRenderer and IsosurfaceRenderer
   * @param generateSet
   */
  protected void render2(boolean generateSet) {
    render2b(generateSet);
  }
  
  protected void render2b(boolean generateSet) {
    if (!g3d.setC(isGhostPass ? mesh.slabColix : colix))
      return;
    if (renderLow || mesh.showPoints || mesh.pc <= 0)
      renderPoints(); 
    if (!renderLow && (isGhostPass ? mesh.slabMeshType == T.mesh : mesh.drawTriangles))
      renderTriangles(false, showTriangles, false);
    if (!renderLow && mesh.pc > 0 && (isGhostPass ? mesh.slabMeshType == T.fill : mesh.fillTriangles))
      renderTriangles(true, showTriangles, generateSet);
  }

  protected void renderPoints() {
    if (!mesh.isDrawPolygon || mesh.pc < 0) {
      for (int i = vertexCount; --i >= 0;)
        if (!frontOnly || normixes == null || isVisibleNormix(normixes[i]))
          drawPoint(i, false);
      return;
    }
    int[][] polygonIndexes = mesh.pis;
    BS bsPoints = BS.newN(mesh.vc);
    if (haveBsDisplay) {
      bsPoints.setBits(0, mesh.vc);
      bsPoints.andNot(mesh.bsDisplay);
    }
    for (int i = mesh.pc; --i >= 0;) {
      if (!isPolygonDisplayable(i))
        continue;
      int[] p = polygonIndexes[i];
      if (frontOnly && !isVisibleNormix(normixes[i]))
        continue;
      for (int j = p.length - 1; --j >= 0;) {
        int pt = p[j];
        if (bsPoints.get(pt))
          continue;
        bsPoints.set(pt);
        drawPoint(pt, true);
      }
    }
  }

  private void drawPoint(int pt, boolean forTriangle) {
    if (renderLow && forTriangle) {
      P3i s = screens[pt];
      g3d.drawPixel(s.x, s.y, s.z);
    } else if (mesh.pc >= 0) {
      drawMeshSphere(screens[pt]);
    } else {
      // scaled point drawing
      drawEdge(pt, pt, false, vertices[pt], vertices[pt], screens[pt], null);
    }
  }
  
  private void drawMeshSphere(P3i pt) {
    g3d.fillSphereI(4, pt);
  }

  protected BS bsPolygonsToExport = new BS();

  protected void renderTriangles(boolean fill, boolean iShowTriangles,
                                 boolean generateSet) {
    g3d.addRenderer(T.triangles);
    int[][] polygons = mesh.pis;
    colix = (isGhostPass ? mesh.slabColix : mesh.colix);
    // vertexColixes are only isosurface properties of IsosurfaceMesh, not Mesh
    if (isTranslucentInherit)
      colix = C.copyColixTranslucency(mesh.slabColix, mesh.colix);
    g3d.setC(colix);
    // isShell???
    if (generateSet) {
      if (frontOnly && fill)
        frontOnly = false;
      bsPolygonsToExport.clearAll();
    }
    for (int i = mesh.pc; --i >= 0;) {
      if (!isPolygonDisplayable(i))
        continue;
      int[] polygon = polygons[i];
      int iA = polygon[0];
      int iB = polygon[1];
      int iC = polygon[2];
      if (iShowTriangles)
        setColix((short) (Math.round(Math.random() * 10) + 5));
      if (haveBsDisplay
          && (!mesh.bsDisplay.get(iA) || !mesh.bsDisplay.get(iB) || !mesh.bsDisplay
              .get(iC)))
        continue;
      if (iB == iC) {
        // line or point
        drawEdge(iA, iB, fill, vertices[iA], vertices[iB], screens[iA],
            screens[iB]);
        continue;
      }
      int check;
      if (mesh.isDrawPolygon) {
        short normix = normixes[i];
        if (frontOnly && !isVisibleNormix(normix))
          continue;
        if (fill) {
          //if (isPrecision)
            g3d.fillTriangle3CNBits(p3Screens[iA], colix, normix,
                p3Screens[iB], colix, normix, p3Screens[iC], colix, normix, true);
          //else
            //g3d.fillTriangle3CN(screens[iA], colix, normix, screens[iB], colix,
            //    normix, screens[iC], colix, normix);
          continue;
        }
        check = polygon[MeshSurface.P_CHECK];
        if (iShowTriangles)
          check = 7;
        if ((check & 1) == 1)
          drawEdge(iA, iB, true, vertices[iA], vertices[iB], screens[iA],
              screens[iB]);
        if ((check & 2) == 2)
          drawEdge(iB, iC, true, vertices[iB], vertices[iC], screens[iB],
              screens[iC]);
        if ((check & 4) == 4)
          drawEdge(iA, iC, true, vertices[iA], vertices[iC], screens[iA],
              screens[iC]);
        continue;
      }
      short nA = normixes[iA];
      short nB = normixes[iB];
      short nC = normixes[iC];
      check = (frontOnly || isShell ? checkFront(nA, nB, nC) : 7);
      if (fill && check != 7)
        continue;
      switch (polygon.length) {
      case 3:
        // simple triangle
        if (fill) {
          if (generateSet) {
            bsPolygonsToExport.set(i);
            continue;
          }
          //if (isPrecision)
            g3d.fillTriangle3CNBits(p3Screens[iA], colix, nA, p3Screens[iB],
                colix, nB, p3Screens[iC], colix, nC, false);
          //else
            //g3d.fillTriangle3CN(screens[iA], colix, nA, screens[iB], colix, nB,
              //  screens[iC], colix, nC);
          continue;
        }
//        if (isPrecision)
          drawTriangleBits(p3Screens[iA], colix, p3Screens[iB], colix,
              p3Screens[iC], colix, check, 1);
        //else
        //  drawTriangle(screens[iA], colix, screens[iB], colix, screens[iC],
        //      colix, check, 1);
        continue;
      case 4:
        // simple quad -- DRAW only (isPrecision)
        int iD = polygon[3];
        short nD = normixes[iD];
        if (frontOnly && (check != 7 || !isVisibleNormix(nD)))
          continue;
        if (fill) {
          if (generateSet) {
            bsPolygonsToExport.set(i);
            continue;
          }
          //if (isPrecision) {
          g3d.fillTriangle3CNBits(p3Screens[iA], colix, nA, p3Screens[iB],
              colix, nB, p3Screens[iC], colix, nC, false);
          g3d.fillTriangle3CNBits(p3Screens[iA], colix, nA, p3Screens[iC],
              colix, nC, p3Screens[iD], colix, nD, false);
//          } else {
//            g3d.fillTriangle3CN(screens[iA], colix, nA, screens[iB],
//                colix, nB, screens[iC], colix, nC);
//            g3d.fillTriangle3CN(screens[iA], colix, nA, screens[iC],
//                colix, nC, screens[iD], colix, nD);
//            
//          }
          continue;
        }
        vwr.gdata.drawQuadrilateralBits(g3d, colix, p3Screens[iA], p3Screens[iB],
            p3Screens[iC], p3Screens[iD]);
      }
    }
    if (generateSet)
      exportSurface(colix);
  }

  protected boolean isVisibleNormix(short normix) {
    return (normix < 0 || transformedVectors[normix].z >= 0);
  }

  private void drawTriangleBits(P3 screenA, short colixA, P3 screenB, short colixB,
                                P3 screenC, short colixC, int check, int diam) {
    if (!antialias && diam == 1) {
     vwr.gdata.drawTriangleBits(g3d, screenA, colixA, screenB, colixB, screenC, colixC,
          check);
      return;
    }
    if (antialias)
      diam <<= 1;
    if ((check & 1) == 1)
      g3d.fillCylinderBits2(colixA, colixB, GData.ENDCAPS_HIDDEN, diam, screenA, screenB);
    if ((check & 2) == 2)
      g3d.fillCylinderBits2(colixB, colixC, GData.ENDCAPS_HIDDEN, diam, screenB, screenC);
    if ((check & 4) == 4)
      g3d.fillCylinderBits2(colixA, colixC, GData.ENDCAPS_HIDDEN, diam, screenA, screenC);
  }

  protected void drawTriangle(P3i screenA, short colixA, P3i screenB,
                              short colixB, P3i screenC, short colixC,
                              int check, int diam) {
    if (!antialias && diam == 1) {
      g3d.drawTriangle3C(screenA, colixA, screenB, colixB, screenC, colixC,
          check);
      return;
    }
    if (antialias)
      diam <<= 1;
    if ((check & 1) == 1)
      g3d.fillCylinderXYZ(colixA, colixB, GData.ENDCAPS_HIDDEN, diam, screenA.x,
          screenA.y, screenA.z, screenB.x, screenB.y, screenB.z);
    if ((check & 2) == 2)
      g3d.fillCylinderXYZ(colixB, colixC, GData.ENDCAPS_HIDDEN, diam, screenB.x,
          screenB.y, screenB.z, screenC.x, screenC.y, screenC.z);
    if ((check & 4) == 4)
      g3d.fillCylinderXYZ(colixA, colixC, GData.ENDCAPS_HIDDEN, diam, screenA.x,
          screenA.y, screenA.z, screenC.x, screenC.y, screenC.z);
  }

  protected int checkFront(short nA, short nB, short nC) {
    int check = 7;
    if (transformedVectors[nA].z < 0)
      check ^= 1;
    if (transformedVectors[nB].z < 0)
      check ^= 2;
    if (transformedVectors[nC].z < 0)
      check ^= 4;
    return check;
  }

  protected void drawEdge(int iA, int iB, boolean fill, T3 vA, T3 vB, P3i sA,
                          P3i sB) {
    byte endCap = (iA != iB && !fill ? GData.ENDCAPS_NONE : width < 0
        || width == -0.0 || iA != iB && isTranslucent ? GData.ENDCAPS_FLAT
        : GData.ENDCAPS_SPHERICAL);
    if (width == 0) {
      if (diameter == 0)
        diameter = (mesh.diameter > 0 ? mesh.diameter : iA == iB ? 7 : 3);
      if (exportType == GData.EXPORT_CARTESIAN) {
        pt1f.ave(vA, vB);
        tm.transformPtScr(pt1f, pt1i);
      }
      if (iA == iB) {
//        if (isPrecision) {
          pt1f.set(sA.x, sA.y, sA.z);
          g3d.fillSphereBits(diameter, pt1f);
//        } else {
//          g3d.fillSphereI(diameter, pt1i);
//        }
        return;
      }
//      if (!isPrecision) {
//        g3d.fillCylinder(endCap, diameter, sA, sB);
//        return;
//      }
    } else {
      pt1f.ave(vA, vB);
      tm.transformPtScr(pt1f, pt1i);
      int mad = (int) Math.floor(Math.abs(width) * 1000);
      diameter = (int) (vwr.tm.scaleToScreen(pt1i.z, mad));
    }
    if (diameter == 0)
      diameter = 1;
    tm.transformPt3f(vA, pt1f);
    tm.transformPt3f(vB, pt2f);
    g3d.fillCylinderBits(endCap, diameter, pt1f, pt2f);
  }

  protected void exportSurface(short colix) {
    mesh.normals = mesh.getNormals(vertices, null);
    mesh.bsPolygons = bsPolygonsToExport;
    mesh.offset = latticeOffset;
    g3d.drawSurface(mesh, colix);
    mesh.normals = null;
    mesh.bsPolygons = null;
  }

}
