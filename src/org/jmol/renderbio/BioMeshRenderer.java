/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-01-04 07:48:02 -0500 (Mon, 04 Jan 2016) $
 * $Revision: 20918 $
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

package org.jmol.renderbio;

import javajs.util.A4;
import javajs.util.M3;
import javajs.util.P3;
import javajs.util.T3;
import javajs.util.V3;

import javajs.util.BS;
import org.jmol.render.MeshRenderer;
import org.jmol.render.ShapeRenderer;
import org.jmol.script.T;
import org.jmol.shape.Mesh;
import org.jmol.shapebio.BioShape;
import org.jmol.util.GData;
import org.jmol.util.Logger;
import org.jmol.util.Normix;

public class BioMeshRenderer extends MeshRenderer {

  private Mesh[] meshes;
  private boolean[] meshReady;
  private BS bsRenderMesh;
  private BioShapeRenderer bsr;
  private boolean doCap0, doCap1;

  @Override
  protected boolean render() {
    // not necessary
    return false;
  }
  
  public void initialize(ShapeRenderer bsr, BioShape bioShape, int monomerCount) {
    this.bsr = (BioShapeRenderer) bsr;
    bsRenderMesh = BS.newN(monomerCount);
    meshReady = bioShape.meshReady;
    meshes = bioShape.meshes;
  }

  private void renderBioMesh(Mesh mesh) {
    if (mesh.normalsTemp != null) {
      mesh.setNormixes(mesh.normalsTemp);
      mesh.normalsTemp = null;
    } else if (mesh.normixes == null) {
      mesh.initialize(T.frontlit, null, null);
    }
    renderMesh2(mesh);
  }

  // Bob Hanson 11/04/2006 - mesh rendering of secondary structure.
  // mesh creation occurs at rendering time, because we don't
  // know what all the options are, and they aren't important,
  // until it gets rendered, if ever.

  public void setFancyRibbon(int i) {
    try {
      if ((meshes[i] == null || !meshReady[i])
          && !createMesh(i, bsr.madBeg, bsr.madMid, bsr.madEnd, bsr.aspectRatio, bsr.isNucleic ? 4 : 7))
        return;
      meshes[i].setColix(bsr.colix);
      meshes[i].setColixBack(bsr.colixBack);
      bsRenderMesh.set(i);
    } catch (Exception e) {
      bsRenderMesh.clear(i);
      meshes[i] = null;
      Logger.error("render mesh error hermiteRibbon: " + e.toString());
      //System.out.println(e.getMessage());
    }
  }

  public void setFancyConic(int i, int tension) {
    try {
      if ((meshes[i] == null || !meshReady[i])
          && !createMesh(i, bsr.madBeg, bsr.madMid, bsr.madEnd, 1, tension))
        return;
      meshes[i].setColix(bsr.colix);
      bsRenderMesh.set(i);
      return;
    } catch (Exception e) {
      bsRenderMesh.clear(i);
      meshes[i] = null;
      Logger.error("render mesh error hermiteConic: " + e.toString());
      //System.out.println(e.getMessage());
    }
  }

  public void setFancyArrowHead(int i) {
    try {
      doCap0 = true;
      doCap1 = false;
      if ((meshes[i] == null || !meshReady[i])
          && !createMesh(i, (int) Math.floor(bsr.madBeg * 1.2),
              (int) Math.floor(bsr.madBeg * 0.6), 0,
              (bsr.aspectRatio == 1 ? bsr.aspectRatio : bsr.aspectRatio / 2), 7))
        return;
      meshes[i].setColix(bsr.colix);
      bsRenderMesh.set(i);
      return;
    } catch (Exception e) {
      bsRenderMesh.clear(i);
      meshes[i] = null;
      Logger.error("render mesh error hermiteArrowHead: " + e.toString());
      //System.out.println(e.getMessage());
    }
  }

  private final static int ABSOLUTE_MIN_MESH_SIZE = 3;
  private final static int MIN_MESH_RENDER_SIZE = 8;

  private P3[] controlHermites;
  private V3[] wingHermites;
  private P3[] radiusHermites;

  private V3 norm = new V3();
  private final V3 wing = new V3();
  private final V3 wing1 = new V3();
  private final V3 wingT = new V3();
  private final A4 aa = new A4();
  private final P3 pt = new P3();
  private final P3 pt1 = new P3();
  private final P3 ptPrev = new P3();
  private final P3 ptNext = new P3();
  private final M3 mat = new M3();
  private final static int MODE_TUBE = 0;
  private final static int MODE_FLAT = 1;
  private final static int MODE_ELLIPTICAL = 2;
  private final static int MODE_NONELLIPTICAL = 3;

  /**
   * Cartoon meshes are triangulated objects. 
   * 
   * @param i
   * @param madBeg
   * @param madMid
   * @param madEnd
   * @param aspectRatio
   * @param tension 
   * @return true if deferred rendering is required due to normals averaging
   */
  private boolean createMesh(int i, int madBeg, int madMid, int madEnd,
                             float aspectRatio, int tension) {
    bsr.setNeighbors(i);
    P3[] cp = bsr.controlPoints;
    if (cp[i].distanceSquared(cp[bsr.iNext]) == 0)
      return false;

    // options:

    // isEccentric == not just a tube    
    boolean isEccentric = (aspectRatio != 1 && bsr.wingVectors != null);
    // isFlatMesh == using mesh even for hermiteLevel = 0 (for exporters)
    boolean isFlatMesh = (aspectRatio == 0);
    // isElliptical == newer cartoonFancy business
    boolean isElliptical = (bsr.cartoonsFancy || bsr.hermiteLevel >= 6);

    // parameters:

    int nHermites = (bsr.hermiteLevel + 1) * 2 + 1; // 5 for hermiteLevel = 1; 13 for hermitelevel 5
    int nPer = (isFlatMesh ? 4 : (bsr.hermiteLevel + 1) * 4 - 2); // 6 for hermiteLevel 1; 22 for hermiteLevel 5
    float angle = (float) ((isFlatMesh ? Math.PI / (nPer - 1) : 2 * Math.PI
        / nPer));
    Mesh mesh = meshes[i] = new Mesh().mesh1(vwr, "mesh_" + shapeID + "_" + i, (short) 0, i);
    boolean variableRadius = (madBeg != madMid || madMid != madEnd);

    // control points and vectors:

    if (controlHermites == null || controlHermites.length < nHermites + 1) {
      controlHermites = new P3[nHermites + 1];
    }
    GData.getHermiteList(tension, cp[bsr.iPrev],
        cp[i], cp[bsr.iNext], cp[bsr.iNext2],
        cp[bsr.iNext3], controlHermites, 0, nHermites, true);
    // wing hermites determine the orientation of the cartoon
    if (wingHermites == null || wingHermites.length < nHermites + 1) {
      wingHermites = new V3[nHermites + 1];
    }

    wing.setT(bsr.wingVectors[bsr.iPrev]);
    if (madEnd == 0)
      wing.scale(2.0f); //adds a flair to an arrow
    GData.getHermiteList(tension, wing, bsr.wingVectors[i],
        bsr.wingVectors[bsr.iNext], bsr.wingVectors[bsr.iNext2], bsr.wingVectors[bsr.iNext3],
        wingHermites, 0, nHermites, false);
    //    }
    // radius hermites determine the thickness of the cartoon
    float radius1 = madBeg / 2000f;
    float radius2 = madMid / 2000f;
    float radius3 = madEnd / 2000f;
    if (variableRadius) {
      if (radiusHermites == null
          || radiusHermites.length < ((nHermites + 1) >> 1) + 1) {
        radiusHermites = new P3[((nHermites + 1) >> 1) + 1];
      }
      ptPrev.set(radius1, radius1, 0);
      pt.set(radius1, radius2, 0);
      pt1.set(radius2, radius3, 0);
      ptNext.set(radius3, radius3, 0);
      // two for the price of one!
      GData.getHermiteList(4, ptPrev, pt, pt1, ptNext, ptNext,
          radiusHermites, 0, (nHermites + 1) >> 1, true);
    }

    // now create the cartoon polygon

    int nPoints = 0;
    int iMid = nHermites >> 1;
    int kpt1 = (nPer + 2) / 4;
    int kpt2 = (3 * nPer + 2) / 4;
    int mode = (!isEccentric ? MODE_TUBE : isFlatMesh ? MODE_FLAT
        : isElliptical ? MODE_ELLIPTICAL : MODE_NONELLIPTICAL);
    boolean useMat = (mode == MODE_TUBE || mode == MODE_NONELLIPTICAL);
    for (int p = 0; p < nHermites; p++) {
      norm.sub2(controlHermites[p + 1], controlHermites[p]);
      float scale = (!variableRadius ? radius1 : p < iMid ? radiusHermites[p].x
          : radiusHermites[p - iMid].y);
      wing.setT(wingHermites[p]);
      wing1.setT(wing);
      switch (mode) {
      case MODE_FLAT:
        // hermiteLevel = 0 and not exporting
        break;
      case MODE_ELLIPTICAL:
        // cartoonFancy 
        wing1.cross(norm, wing);
        wing1.normalize();
        wing1.scale(wing.length() / aspectRatio);
        break;
      case MODE_NONELLIPTICAL:
        // older nonelliptical hermiteLevel > 0
        wing.scale(2 / aspectRatio);
        wing1.sub(wing);
        break;
      case MODE_TUBE:
        // not helix or sheet
        wing.cross(wing, norm);
        wing.normalize();
        break;
      }
      wing.scale(scale);
      wing1.scale(scale);
      if (useMat) {
        aa.setVA(norm, angle);
        mat.setAA(aa);
      }
      pt1.setT(controlHermites[p]);
      float theta = (isFlatMesh ? 0 : angle);
      for (int k = 0; k < nPer; k++, theta += angle) {
        if (useMat && k > 0)
          mat.rotate(wing);
        switch (mode) {
        case MODE_FLAT:
          wingT.setT(wing1);
          wingT.scale((float) Math.cos(theta));
          break;
        case MODE_ELLIPTICAL:
          wingT.setT(wing1);
          wingT.scale((float) Math.sin(theta));
          wingT.scaleAdd2((float) Math.cos(theta), wing, wingT);
          break;
        case MODE_NONELLIPTICAL:
          wingT.setT(wing);
          if (k == kpt1 || k == kpt2)
            wing1.scale(-1);
          wingT.add(wing1);
          break;
        case MODE_TUBE:
          wingT.setT(wing);
          break;
        }
        pt.add2(pt1, wingT);
        //System.out.println(pt);
        mesh.addV(pt, true);
      }
      if (p > 0) {
        int nLast = (isFlatMesh ? nPer - 1 : nPer);
        for (int k = 0; k < nLast; k++) {
          // draw the triangles of opposing quads congruent, so they won't clip 
          // esp. for high ribbonAspectRatio values 
          int a = nPoints - nPer + k;
          int b = nPoints - nPer + ((k + 1) % nPer);
          int c = nPoints + ((k + 1) % nPer);
          int d = nPoints + k;
          // can happen at arrow point
//          if (wing1.length() == 0 && mesh.vs[c].distanceSquared(mesh.vs[d]) == 0)
//            d = c;
          if (k < nLast / 2)
            mesh.addQuad(a, b, c, d);
          else
            mesh.addQuad(b, c, d, a);
        }
      }
      nPoints += nPer;
    }
    if (!isFlatMesh) {
      int nPointsPreCap = nPoints;
      // copying vertices here so that the caps are not connected to the rest of
      // the mesh preventing light leaking around the sharp edges
      if (doCap0) {
        T3[] vs = mesh.getVertices();
        for (int l = 0; l < nPer; l++)
          mesh.addV(vs[l], true);
        nPoints += nPer;
        for (int k = bsr.hermiteLevel * 2; --k >= 0;)
          mesh.addQuad(nPoints - nPer + k + 2, nPoints - nPer + k + 1, nPoints
              - nPer + (nPer - k) % nPer, nPoints - k - 1);
      }
      if (doCap1) {
        T3[] vs = mesh.getVertices();
        for (int l = 0; l < nPer; l++)
          mesh.addV(vs[nPointsPreCap - nPer + l], true);
        nPoints += nPer;
        for (int k = bsr.hermiteLevel * 2; --k >= 0;)
          mesh.addQuad(nPoints - k - 1, nPoints - nPer + (nPer - k) % nPer,
              nPoints - nPer + k + 1, nPoints - nPer + k + 2);
      }
    }
    meshReady[i] = true;
    adjustCartoonSeamNormals(i, nPer);
    mesh.setVisibilityFlags(1);
    return true;
  }

  private BS bsTemp;
  private final V3 norml = new V3();

  /**
   * Matches normals for adjacent mesh sections to create a seamless overall
   * mesh. We use temporary normals here. We will convert normals to normixes
   * later.
   * 
   * @param i
   * @param nPer
   */
  void adjustCartoonSeamNormals(int i, int nPer) {
    if (bsTemp == null)
      bsTemp = Normix.newVertexBitSet();
    if (i == bsr.iNext - 1 && bsr.iNext < bsr.monomerCount
        && bsr.monomers[i].getStrucNo() == bsr.monomers[bsr.iNext].getStrucNo()
        && meshReady[i] && meshReady[bsr.iNext]) {
      try {
        V3[] normals2 = meshes[bsr.iNext].getNormalsTemp();
        V3[] normals = meshes[i].getNormalsTemp();
        int normixCount = normals.length;
        if (doCap0)
          normixCount -= nPer;
        for (int j = 1; j <= nPer; ++j) {
          norml.add2(normals[normixCount - j], normals2[nPer - j]);
          norml.normalize();
          normals[normixCount - j].setT(norml);
          normals2[nPer - j].setT(norml);
        }
      } catch (Exception e) {
      }
    }
  }

  public void renderMeshes() {
    if (bsRenderMesh.isEmpty())
      return;
    setColix(bsr.colix);
    for (int i = bsRenderMesh.nextSetBit(0); i >= 0; i = bsRenderMesh
        .nextSetBit(i + 1))
      renderBioMesh(meshes[i]);
  }

  public void initBS() {
    bsRenderMesh.clearAll();
  }

  public boolean check(boolean doCap0, boolean doCap1) {
    this.doCap0 = doCap0;
    this.doCap1 = doCap1;
    return (exportType == GData.EXPORT_CARTESIAN 
        || checkDiameter(bsr.diameterBeg)
        || checkDiameter(bsr.diameterMid) 
        || checkDiameter(bsr.diameterEnd));  
    }

  private boolean checkDiameter(int d) {
    return (bsr.isHighRes && d > ABSOLUTE_MIN_MESH_SIZE || d >= MIN_MESH_RENDER_SIZE);
  }

  /*
  private void dumpPoint(Point3f pt, short color) {
    Point3i pt1 = tm.transformPoint(pt);
    g3d.fillSphereCentered(color, 20, pt1);
  }

  private void dumpVector(Point3f pt, Vector3f v, short color) {
    Point3f p1 = new Point3f();
    Point3i pt1 = new Point3i();
    Point3i pt2 = new Point3i();
    p1.add(pt, v);
    pt1.set(tm.transformPoint(pt));
    pt2.set(tm.transformPoint(p1));
    System.out.print("draw pt" + ("" + Math.random()).substring(3, 10) + " {"
        + pt.x + " " + pt.y + " " + pt.z + "} {" + p1.x + " " + p1.y + " "
        + p1.z + "}" + ";" + " ");
    g3d.fillCylinder(color, GData.ENDCAPS_FLAT, 2, pt1.x, pt1.y, pt1.z,
        pt2.x, pt2.y, pt2.z);
    g3d.fillSphereCentered(color, 5, pt2);
  }
  */


}
