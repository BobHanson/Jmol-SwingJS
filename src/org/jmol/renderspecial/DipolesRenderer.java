/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-03-16 15:52:18 -0600 (Thu, 16 Mar 2006) $
 * $Revision: 4635 $
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.renderspecial;


import javajs.util.P3;
import javajs.util.V3;

import javajs.util.BS;
import org.jmol.render.ShapeRenderer;
import org.jmol.script.T;
import org.jmol.shapespecial.Dipole;
import org.jmol.shapespecial.Dipoles;
import org.jmol.util.C;
import org.jmol.util.GData;

public class DipolesRenderer extends ShapeRenderer {

  private float dipoleVectorScale;
  private final V3 offset = new V3();
  private final P3[] screens3f = new P3[6];
  private final P3[] points = new P3[6];
  {
    for (int i = 0; i < 6; i++) {
      screens3f[i] = new P3();
      points[i] = new P3();
    }
  }
  private P3 cross0 = new P3();
  private P3 cross1 = new P3();
  
  private final static int cylinderBase = 0;
  private final static int cross = 1;
  private final static int crossEnd = 2;
  private final static int center = 3;
  private final static int arrowHeadBase = 4;
  private final static int arrowHeadTip = 5;

  private int diameter;
  private int headWidthPixels;
  private int crossWidthPixels;
  private float offsetSide;
  private short colixA;
  private short colixB;
  private boolean noCross;

  private final static float arrowHeadOffset = 0.9f;
  private final static float arrowHeadWidthFactor = 2f;
  private final static float crossOffset = 0.1f;
  private final static float crossWidth = 0.04f;


  @Override
  protected boolean render() {
    Dipoles dipoles = (Dipoles) shape;
    dipoleVectorScale = vwr.getFloat(T.dipolescale);
    boolean needTranslucent = false;
    BS vis = vwr.ms.getVisibleSet(false);
    for (int i = dipoles.dipoleCount; --i >= 0;) {
      Dipole dipole = dipoles.dipoles[i];
      if (dipole.visibilityFlags != 0 && 
         (dipole.atoms[0] == null || !ms.isAtomHidden(dipole.atoms[0].i))
         && (dipole.bsMolecule == null || dipole.bsMolecule.intersects(vis))
          && renderDipoleVector(dipole, vis))
        needTranslucent = true;
    }
    return needTranslucent;
  }

  private boolean renderDipoleVector(Dipole dipole, BS vis) {
    mad = dipole.mad;
    offsetSide = dipole.offsetSide;
    noCross = dipole.noCross;
    colixA = (dipole.bond == null ? dipole.colix : C.getColixInherited(
        dipole.colix, dipole.bond.colix));
    colixB = colixA;
    if (dipole.atoms[0] != null) {
      colixA = C.getColixInherited(colixA, dipole.atoms[0].colixAtom);
      colixB = C.getColixInherited(colixB, dipole.atoms[1].colixAtom);
    }
    if (colixA == 0)
      colixA = C.ORANGE;
    if (colixB == 0)
      colixB = C.ORANGE;
    if (dipoleVectorScale < 0) {
      short c = colixA;
      colixA = colixB;
      colixB = c;
    }
    float factor = dipole.offsetAngstroms / dipole.dipoleValue;
    if (dipole.lstDipoles == null)
      return renderVector(dipole.vector, dipole.origin, dipole.center,
          factor, false);
    boolean needTranslucent = false;
    for (int i = dipole.lstDipoles.size(); --i >= 0;) {
      Object[] o = (Object[]) dipole.lstDipoles.get(i);
      V3 v = (V3) o[0];
      P3 origin = (P3) o[1];
      BS bsAtoms = (BS) o[2];
      if (bsAtoms.intersects(vis))
        needTranslucent = renderVector(v, origin, null, dipole.offsetAngstroms, true);
    }
    return needTranslucent;
  }
 
  private boolean renderVector(V3 vector, P3 origin, P3 dcenter, float factor, boolean isGroup) {
    offset.setT(vector);
    if (dcenter == null) {
      if (isGroup) {
        offset.normalize();
        offset.scale(factor);
      } else {
        offset.scale(factor);
        if (dipoleVectorScale < 0)
          offset.add(vector);
      }
      points[cylinderBase].add2(origin, offset);
    } else {
      offset.scale(-0.5f * dipoleVectorScale);
      points[cylinderBase].add2(dcenter, offset);
      if (factor != 0) {
        offset.setT(vector);
        offset.scale(factor);
        points[cylinderBase].add(offset);
      }
    }
    points[cross].scaleAdd2(dipoleVectorScale * crossOffset, vector,
        points[cylinderBase]);
    points[crossEnd].scaleAdd2(dipoleVectorScale * (crossOffset + crossWidth),
        vector, points[cylinderBase]);
    points[center]
        .scaleAdd2(dipoleVectorScale / 2, vector, points[cylinderBase]);
    points[arrowHeadBase].scaleAdd2(dipoleVectorScale * arrowHeadOffset, vector,
        points[cylinderBase]);
    points[arrowHeadTip].scaleAdd2(dipoleVectorScale, vector,
        points[cylinderBase]);

    offset.setT(points[center]);
    offset.cross(offset, vector);
    if (offset.length() == 0) {
      offset.set(points[center].x + 0.2345f, points[center].y + 0.1234f,
          points[center].z + 0.4321f);
      offset.cross(offset, vector);
    }
    offset.scale(offsetSide / offset.length());
    for (int i = 0; i < 6; i++)
      points[i].add(offset);
    for (int i = 0; i < 6; i++)
      tm.transformPtScrT3(points[i], screens3f[i]);
    tm.transformPt3f(points[cross], cross0);
    tm.transformPt3f(points[crossEnd], cross1);
    float d = vwr.tm.scaleToScreen((int) screens3f[center].z, mad);
    diameter = (int) d;
    headWidthPixels = (int) Math.floor(d * arrowHeadWidthFactor);
    if (headWidthPixels < diameter + 5)
      headWidthPixels = diameter + 5;
    crossWidthPixels = headWidthPixels;
    colix = colixA;
    if (colix == colixB) {
      if (!g3d.setC(colix))
        return true;
      g3d.fillCylinderBits(GData.ENDCAPS_FLAT, diameter,
          screens3f[cylinderBase], screens3f[arrowHeadBase]);
      if (!noCross)
        g3d.fillCylinderBits(GData.ENDCAPS_FLAT, crossWidthPixels, cross0,
            cross1);
      g3d.fillConeScreen3f(GData.ENDCAPS_FLAT, headWidthPixels,
          screens3f[arrowHeadBase], screens3f[arrowHeadTip], false);
      return false;
    }
    boolean needTranslucent = false;
    if (g3d.setC(colix)) {
      g3d.fillCylinderBits(GData.ENDCAPS_FLAT, diameter,
          screens3f[cylinderBase], screens3f[center]);
      if (!noCross)
        g3d.fillCylinderBits(GData.ENDCAPS_FLAT, crossWidthPixels, cross0,
            cross1);
    } else {
      needTranslucent = true;
    }
    colix = colixB;
    if (g3d.setC(colix)) {
      g3d.fillCylinderBits(GData.ENDCAPS_FLAT, diameter, screens3f[center],
          screens3f[arrowHeadBase]);
      g3d.fillConeScreen3f(GData.ENDCAPS_FLAT, headWidthPixels,
          screens3f[arrowHeadBase], screens3f[arrowHeadTip], false);
    } else {
      needTranslucent = true;
    }
    return needTranslucent;
  }
 }
