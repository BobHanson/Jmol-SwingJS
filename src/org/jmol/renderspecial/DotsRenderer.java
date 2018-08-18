/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-10-08 12:28:44 -0500 (Sat, 08 Oct 2016) $
 * $Revision: 21258 $
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

package org.jmol.renderspecial;

import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.render.ShapeRenderer;
import org.jmol.script.T;
import org.jmol.shapespecial.Dots;
import org.jmol.util.C;
import org.jmol.util.GData;
import org.jmol.util.Geodesic;

import javajs.util.P3i;
import javajs.util.V3;



public class DotsRenderer extends ShapeRenderer {

  public boolean iShowSolid;
  
  public int screenLevel;
  public int screenDotCount;
  public int[] scrCoords;
  public int[] faceMap = null; // used only by GeoSurface, but set here
  
  private V3 v3temp = new V3();
  private P3i scrTemp = new P3i();

  private int dotScale;
  
  @Override
  protected void initRenderer() {
    screenLevel = Dots.MAX_LEVEL;
    screenDotCount = Geodesic.getVertexCount(Dots.MAX_LEVEL);
    scrCoords = new int[3 * screenDotCount];
  }

  @Override
  protected boolean render() {
    render1((Dots) shape);
    return false;
  }

  protected void render1(Dots dots) {
    //dots.timeBeginExecution = System.currentTimeMillis();
    if (!iShowSolid && !g3d.setC(C.BLACK)) // no translucent for dots
      return;
    int sppa = (int) vwr.getScalePixelsPerAngstrom(true);
    screenLevel = (iShowSolid || sppa > 20 ? 3 : sppa > 10 ? 2 : sppa > 5 ? 1
        : 0);
    if (!iShowSolid)
      screenLevel += vwr.getInt(T.dotdensity) - 3;
    screenLevel = Math.max(Math.min(screenLevel, Dots.MAX_LEVEL), 0);
    screenDotCount = Geodesic.getVertexCount(screenLevel);
    dotScale = vwr.getInt(T.dotscale);
    BS[] maps = dots.ec.getDotsConvexMaps();
    for (int i = dots.ec.getDotsConvexMax(); --i >= 0;) {
      Atom atom = ms.at[i];
      BS map = maps[i];
      if (map == null || !isVisibleForMe(atom)
          || !g3d.isInDisplayRange(atom.sX, atom.sY))
        continue;
      try {
        float radius = dots.ec.getAppropriateRadius(i);
        if (iShowSolid && exportType == GData.EXPORT_CARTESIAN) {
          // for VRML, X3D, and STL, just output the atom;
          // (not showing unclosed surfaces)
          g3d.drawAtom(atom, radius);
          continue;
        }
        int nPoints = 0;
        int j = 0;
        int iDot = Math.min(map.size(), screenDotCount); 
        while (--iDot >= 0) {
          if (!map.get(iDot))
            continue;
          v3temp.scaleAdd2(radius, Geodesic.getVertexVector(iDot), atom);
          tm.transformPtScr(v3temp, scrTemp);
          if (faceMap != null)
            faceMap[iDot] = j;
          scrCoords[j++] = scrTemp.x;
          scrCoords[j++] = scrTemp.y;
          scrCoords[j++] = scrTemp.z;
          ++nPoints;
        }

        if (nPoints != 0)
          renderConvex(C.getColixInherited(dots.colixes[i], atom.colixAtom),
              map, nPoints);
      } catch (Exception e) {
        System.out.println("Dots rendering error");
        System.out.println(e.toString());
        // ignore -- some sort of fluke
      }
    }
    //dots.timeEndExecution = System.currentTimeMillis();
    //Logger.debug("dots rendering time = "+ gs.getExecutionWalltime());
  }
  

  /**
   * generic renderer -- dots and geosurface
   * 
   * @param colix
   * @param map
   * @param nPoints
   */
  protected void renderConvex(short colix, BS map, int nPoints) {
    this.colix = C.getColixTranslucent3(colix, false, 0);
    renderDots(nPoints);
  }

  /**
   * also called by GeoSurface when in motion
   * 
   * @param nPoints
   */
  protected void renderDots(int nPoints) {
    g3d.setC(colix);
    g3d.drawPoints(nPoints, scrCoords, dotScale);
  }
}

