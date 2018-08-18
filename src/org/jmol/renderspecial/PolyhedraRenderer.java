/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-12 11:05:36 -0500 (Mon, 12 Mar 2007) $
 * $Revision: 7077 $
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
package org.jmol.renderspecial;


import javajs.util.P3;
import javajs.util.P3i;
import javajs.util.T3;
import javajs.util.V3;

import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.render.ShapeRenderer;
import org.jmol.script.T;
import org.jmol.shapespecial.Polyhedra;
import org.jmol.shapespecial.Polyhedron;
import org.jmol.util.C;
import org.jmol.util.GData;
import org.jmol.util.MeshSurface;

public class PolyhedraRenderer extends ShapeRenderer {

  private int drawEdges;
  private boolean isAll;
  private boolean frontOnly, edgesOnly;
  private P3[] screens3f;
  private P3i scrVib;
  private boolean vibs;
  private BS bsSelected;
  private boolean showNumbers;
  private MeshSurface meshSurface;

  @Override
  protected boolean render() {
    Polyhedra polyhedra = (Polyhedra) shape;
    Polyhedron[] polyhedrons = polyhedra.polyhedrons;
    drawEdges = polyhedra.drawEdges;
    bsSelected = (vwr.getSelectionHalosEnabled() ? vwr.bsA() : null);
    g3d.addRenderer(T.triangles);
    vibs = (ms.vibrations != null && tm.vibrationOn);
    showNumbers = vwr.getBoolean(T.testflag3);
    boolean needTranslucent = false;
    for (int i = polyhedra.polyhedronCount; --i >= 0;) 
      if (polyhedrons[i].isValid && render1(polyhedrons[i]))
        needTranslucent = true;
    return needTranslucent;
  }

  private boolean render1(Polyhedron p) {
    if (p.visibilityFlags == 0)
      return false;
    short[] colixes = ((Polyhedra) shape).colixes;
    int iAtom = -1;
    short colix;
    float scale = 1;
    if (p.id == null) {
      iAtom = p.centralAtom.i;
      colix = (colixes == null || iAtom >= colixes.length ? C.INHERIT_ALL
          : colixes[iAtom]);
      colix = C.getColixInherited(colix, p.centralAtom.colixAtom);
    } else {
      colix = p.colix;
      scale = p.scale;
    }
    boolean needTranslucent = false;
    if (C.renderPass2(colix)) {
      needTranslucent = true;
    } else if (!g3d.setC(colix)) {
      return false;
    }
    T3[] vertices = p.vertices;
    if (scale != 1) {
      T3[] v = new T3[vertices.length];
      if (scale < 0) {
        // explode from {0 0 0}
        V3 a = V3.newV(p.center);
        a.scale(-scale - 1);
        for (int i = v.length; --i >= 0;) {
          V3 b = V3.newV(vertices[i]);
          b.add(a);
          v[i] = b;
        }
      } else {
        // enlarge
        for (int i = v.length; --i >= 0;) {
          V3 a = V3.newVsub(vertices[i], p.center);
          a.scaleAdd2(scale, a, p.center);
          v[i] = a;
        }
      }
      vertices = v;
    }

    if (screens3f == null || screens3f.length < vertices.length) {
      screens3f = new P3[vertices.length];
      for (int i = vertices.length; --i >= 0;)
        screens3f[i] = new P3();
    }
    P3[] sc = this.screens3f;
    int[][] planes = p.triangles;
    int[] elemNos = (p.pointScale > 0 ? p.getElemNos() : null);
    for (int i = vertices.length; --i >= 0;) {
      Atom atom = (vertices[i] instanceof Atom ? (Atom) vertices[i] : null);
      P3 v = sc[i];
      if (atom == null) {
        tm.transformPtScrT3(vertices[i], v);
        //      } else if (atom.isVisible(myVisibilityFlag)) {
        //      v.set(atom.sX, atom.sY, atom.sZ);
      } else if (vibs && atom.hasVibration()) {
        scrVib = tm.transformPtVib(atom, ms.vibrations[atom.i]);
        v.set(scrVib.x, scrVib.y, scrVib.z);
      } else {
        tm.transformPt3f(atom, v);
      }
      if (elemNos != null
          && i < elemNos.length
          && g3d.setC(elemNos[i] < 0 ? colix : vwr.cm.setElementArgb(
              elemNos[i], Integer.MAX_VALUE))) {
        g3d.fillSphereBits(
            (int) tm.scaleToScreen((int) v.z, (int) (p.pointScale * 1000)), v);
        g3d.setC(colix);
      }
      if (showNumbers) {
        if (g3d.setC(C.BLACK)) {
          g3d.drawStringNoSlab("" + i, null, (int) v.x, (int) v.y,
              (int) v.z - 30, (short) 0);
          g3d.setC(colix);
        }
      }
    }

    boolean isSelected = (iAtom >= 0 && bsSelected != null && bsSelected
        .get(iAtom));
    isAll = (drawEdges == Polyhedra.EDGES_ALL || isSelected);
    frontOnly = (drawEdges == Polyhedra.EDGES_FRONT);
    edgesOnly = (drawEdges == Polyhedra.EDGES_ONLY);

    // no edges to new points when not collapsed
    //int m = (int) ( Math.random() * 24);
    short[] normixes = p.getNormixes();
    if ((!needTranslucent || g3d.setC(colix)) && !edgesOnly) {
      if (exportType == GData.EXPORT_CARTESIAN && !p.collapsed) {
        if (meshSurface == null)
          meshSurface = new MeshSurface();
        meshSurface.vs = vertices;
        meshSurface.pis = planes;
        meshSurface.pc = planes.length;
        meshSurface.vc = vertices.length;
        g3d.drawSurface(meshSurface, colix);
      } else {
        for (int i = planes.length; --i >= 0;) {
          int[] pl = planes[i];
          try {
            if (!showNumbers
                || g3d.setC((short) (Math.round(Math.random() * 10) + 5)))
              g3d.fillTriangleTwoSided(normixes[i], sc[pl[0]], sc[pl[1]],
                  sc[pl[2]]);
          } catch (Exception e) {
            System.out.println("PolyhedraRendererError");
          }
        }
        //        if (pl[3] >= 0)
        //        g3d.fillTriangleTwoSided(normixes[i], sc[pl[2]], sc[pl[3]], sc[pl[0]]);
      }
    }
    // edges are not drawn translucently ever
    if (isAll || frontOnly || edgesOnly) {
      if (isSelected)
        colix = C.GOLD;
      else if (p.colixEdge != C.INHERIT_ALL)
        colix = p.colixEdge;
      if (g3d.setC(C.getColixTranslucent3(colix, false, 0)))
        for (int i = planes.length; --i >= 0;) {
          int[] pl = planes[i];
          //       if (pl[3] < 0) {
          drawEdges(normixes[i], sc[pl[0]], sc[pl[1]], sc[pl[2]], -pl[3]);
          //          break;
          //        } else {
          //          drawFace(normixes[i], sc[pl[0]], sc[pl[1]], sc[pl[2]], 3);
          //          drawFace(normixes[i], sc[pl[0]], sc[pl[2]], sc[pl[3]], 6);
          //        }

        }
    }
    return needTranslucent;
  }

  private void drawEdges(short normix, P3 a, P3 b, P3 c, int edgeMask) {
    if (isAll || edgesOnly || frontOnly && vwr.gdata.isDirectedTowardsCamera(normix)) {
      int d = (g3d.isAntialiased() ? 6 : 3);
      if ((edgeMask & 1) == 1)
        g3d.fillCylinderBits(GData.ENDCAPS_SPHERICAL, d, a, b);
      if ((edgeMask & 2) == 2)
        g3d.fillCylinderBits(GData.ENDCAPS_SPHERICAL, d, b, c);
      if ((edgeMask & 4) == 4)
        g3d.fillCylinderBits(GData.ENDCAPS_SPHERICAL, d, a, c);
    }
  }


}
