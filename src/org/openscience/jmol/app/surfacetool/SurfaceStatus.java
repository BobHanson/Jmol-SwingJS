/* $RCSfile$
 * $Jonathan Gutow$
 * $July 28, 2011$
 * $Revision$
 *
 * Copyright (C) 2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.openscience.jmol.app.surfacetool;

import org.jmol.shape.Mesh;
import org.jmol.util.C;

/**
 * Class that holds the surface status information that the surface tool needs.
 */
public class SurfaceStatus {
  String id;
  int kind;
  int color;
  boolean fillOn;
  int translucency;//as of 12.2 0  to 7, 0 = opaque, 7 = off.
  boolean meshOn;
  int meshColor;
  int meshTranslucency;//as of 12.2 0 to 7, 0 = opaque, 7 = off.  Ignored for now.
  int lighting;
  boolean frontonly;
  boolean show; //TODO keep track of this too...
  boolean beenSliced;
  boolean capOn;
  boolean ghostOn;
  boolean foundDuringLastSync;
  Slice slice;
  private boolean isShell;

  /**
   * @param m
   *        (mesh) the mesh representing the surface
   * @param type
   *        (int) representing the type of surface (eg. pmesh, isosurface...)
   */
  public SurfaceStatus(Mesh m, int type) {
    id = m.thisID;
    kind = type;
    color = m.color;
    fillOn = m.fillTriangles;
    translucency = C.getColixTranslucencyLevel(m.colix);
    meshOn = m.drawTriangles;
    meshColor = C.getArgb(m.meshColix);
    //TODO deal with mesh transparency
    meshTranslucency = 0; //ignored for now    
    lighting = m.lighting;
    frontonly = m.frontOnly;
    isShell = m.isShell;
    beenSliced = false;
    capOn = false;
    ghostOn = false;
    foundDuringLastSync = true;
    slice = new Slice();
  }

  public void updateExisting(Mesh m) {//updates data taken from Mesh...
    color = m.color;
    fillOn = m.fillTriangles;
    translucency = C.getColixTranslucencyLevel(m.colix);
    meshOn = m.drawTriangles;
    meshColor = C.getArgb(m.meshColix);
    //TODO deal with mesh transparency
    meshTranslucency = 0; //ignored for now    
    lighting = m.lighting;
    frontonly = m.frontOnly;
    isShell = m.isShell;
    foundDuringLastSync = true;
  }
}
