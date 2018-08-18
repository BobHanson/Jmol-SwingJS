/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-19 12:18:17 -0500 (Mon, 19 Mar 2007) $
 * $Revision: 7171 $
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

import javajs.util.P3;

import javajs.util.BS;
import org.jmol.script.T;
import org.jmol.shapespecial.GeoSurface;
import org.jmol.util.C;
import org.jmol.util.Geodesic;


/*
 * A simple geodesic surface renderer that turns into just dots
 * if the model is rotated.
 *
 * Bob Hanson, 3/2007
 * 
 * see org.jmol.viewer.DotsRenderer
 */

public class GeoSurfaceRenderer extends DotsRenderer {
  
  private boolean requireTranslucent;

  @Override
  protected boolean render() {
    GeoSurface gs = (GeoSurface) shape;
    iShowSolid = !(!vwr.checkMotionRendering(T.geosurface) && gs.ec.getDotsConvexMax() > 100);
    if (!iShowSolid && !g3d.setC(C.BLACK))
      return false;
    // need to hide inner parts if translucent
    boolean tCover = vwr.gdata.translucentCoverOnly;
    if (iShowSolid)
      vwr.gdata.translucentCoverOnly = true;
    g3d.addRenderer(T.triangles);
    if (iShowSolid && faceMap == null)
      faceMap = new int[screenDotCount];
    render1(gs);
    vwr.gdata.translucentCoverOnly = tCover;
    return requireTranslucent;
  }
  
 @Override
protected void renderConvex(short colix, BS visibilityMap, int nPoints) {
    this.colix = colix;
    if (iShowSolid) {
      if (g3d.setC(colix))       
        renderSurface(visibilityMap);
      else
        requireTranslucent = true;
      return;
    }
    renderDots(nPoints);
  }
  
  private P3 facePt1 = new P3();
  private P3 facePt2 = new P3();
  private P3 facePt3 = new P3();
  
  private void renderSurface(BS points) {
    if (faceMap == null)
      return;
    short[] faces = Geodesic.getFaceVertexes(screenLevel);
    int[] coords = scrCoords;
    short p1, p2, p3;
    int mapMax = points.size();
    //Logger.debug("geod frag "+mapMax+" "+dotCount);
    if (screenDotCount < mapMax)
      mapMax = screenDotCount;
    // TODO use a mesh for this
    for (int f = 0; f < faces.length;) {
      p1 = faces[f++];
      p2 = faces[f++];
      p3 = faces[f++];
      if (p1 >= mapMax || p2 >= mapMax || p3 >= mapMax || !points.get(p1)
          || !points.get(p2) || !points.get(p3))
        continue;
      facePt1.set(coords[faceMap[p1]], coords[faceMap[p1] + 1],
          coords[faceMap[p1] + 2]);
      facePt2.set(coords[faceMap[p2]], coords[faceMap[p2] + 1],
          coords[faceMap[p2] + 2]);
      facePt3.set(coords[faceMap[p3]], coords[faceMap[p3] + 1],
          coords[faceMap[p3] + 2]);
      g3d.fillTriangle3CNBits(facePt1, colix, p1, facePt2, colix, p2, facePt3,
          colix, p3, false);
    }
  }  
}

