/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-10-08 22:18:02 -0500 (Mon, 08 Oct 2007) $
 * $Revision: 8391 $

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

import java.util.Map;

import org.jmol.util.C;
import org.jmol.util.GData;
import org.jmol.util.Normix;

import javajs.util.M3d;
import javajs.util.M4d;
import javajs.util.P3d;

import javajs.util.PT;
import javajs.util.V3d;
import javajs.util.V3d;
import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.render.ShapeRenderer;
import org.jmol.script.T;
import org.jmol.shapespecial.Ellipsoid;
import org.jmol.shapespecial.Ellipsoids;
import org.jmol.viewer.JC;

final public class EllipsoidsRenderer extends ShapeRenderer {
  
  // final because we are initializing static fields using static{}
  
  private Ellipsoids ellipsoids;

  private boolean[] bGlobals = new boolean[7];
  private boolean[] bOptions = new boolean[7];
  private final String[] OPTS = new String[] { "dots", "arcs", "axes", "fill", "ball", "arrows", "wireframe" };
  private final static int OPT_DOTS = 0;
  private final static int OPT_ARCS = 1;
  private final static int OPT_AXES = 2;
  private final static int OPT_FILL = 3;
  private final static int OPT_BALL = 4;
  private final static int OPT_ARROWS = 5;
  private final static int OPT_WIREFRAME = 6;
  private static final int OPT_COUNT = 7;
  
  private boolean fillArc;
  private boolean isSet;
  
  private int diameter, diameter0;
  private int dotCount, dotScale;
  private int dx;
  private int eigenSignMask = 7;  
  private int iCutout = -1;  
  private int selectedOctant = -1;

  private int[] coords;
  private V3d[] axes = new V3d[] { new V3d(), new V3d(), new V3d() };
  private P3d center;
  private double perspectiveFactor;
  private BS bsTemp = new BS();
  
  private M3d mat = new M3d();
  private M3d mTemp = new M3d();
  private M4d mDeriv = new M4d();
  private M3d matScreenToCartesian = new M3d();
  private M3d matScreenToEllipsoid = new M3d();
  private M3d matEllipsoidToScreen = new M3d();
  
  private final double[] coefs = new double[10];
  private final double[] factoredLengths = new double[3];
  private final P3d[] selectedPoints = new P3d[3];
  private final V3d v1 = new V3d();
  private final V3d v2 = new V3d();
  private final V3d v3 = new V3d();  
  private final P3d pt1 = new P3d();
  private final P3d pt2 = new P3d();
  private final P3d s0 = new P3d();
  private final P3d s1 = new P3d();
  private final P3d s2 = new P3d();

  private final static double toRadians = Math.PI/180d;
  private final static double[] cossin = new double[36];

  static {
    // OK for J2S compilation because this is a final class
    for (int i = 5, pt = 0; i <= 90; i += 5) {
      cossin[pt++] = Math.cos(i * toRadians);
      cossin[pt++] = Math.sin(i * toRadians);
    }
  }

  @Override
  protected boolean render() {
    isSet = false;
    ellipsoids = (Ellipsoids) shape;
    if (!ellipsoids.isActive())
      return false;
    boolean needTranslucent = false;
    if (!isSet)
      isSet = setGlobals();
    if (!ellipsoids.atomEllipsoids.isEmpty())
      needTranslucent |= renderEllipsoids(ellipsoids.atomEllipsoids, false);
    if (!ellipsoids.simpleEllipsoids.isEmpty()) {
      needTranslucent |= renderEllipsoids(ellipsoids.simpleEllipsoids, true);
    }
    coords = null;
    return needTranslucent;
  }

  private boolean setGlobals() {
    bGlobals[OPT_ARCS] = vwr.getBooleanProperty("ellipsoidArcs");
    bGlobals[OPT_ARROWS] = vwr.getBooleanProperty("ellipsoidArrows");
    bGlobals[OPT_AXES] = vwr.getBooleanProperty("ellipsoidAxes");
    bGlobals[OPT_BALL] = vwr.getBooleanProperty("ellipsoidBall");
    bGlobals[OPT_DOTS] = vwr.getBooleanProperty("ellipsoidDots");
    bGlobals[OPT_FILL] = vwr.getBooleanProperty("ellipsoidFill");
    bGlobals[OPT_WIREFRAME] = !isExport && !vwr.checkMotionRendering(T.ellipsoid);
    diameter0 = (int) Math.round (((Number) vwr.getP("ellipsoidAxisDiameter"))
        .doubleValue() * 1000);    
    M4d m4 = tm.matrixTransform;
    mat.setRow(0, m4.m00, m4.m01, m4.m02);
    mat.setRow(1, m4.m10, m4.m11, m4.m12);
    mat.setRow(2, m4.m20, m4.m21, m4.m22);
    matScreenToCartesian.invertM(mat);
    setLogic();
    return true;
  }

  private void setOptions(String options) {
    for (int i = 0; i < OPT_COUNT; i++)
      bOptions[i] = bGlobals[i];
    if (options != null) {
      options = ";" + options + ";";
      for (int i = 0; i < OPT_COUNT; i++) {
        if (PT.isOneOf(OPTS[i], options))
        bOptions[i] = true;
      else if (PT.isOneOf("no" + OPTS[i], options))
        bOptions[i] = false;
      }
    }
    setLogic();    
  }
  
  private void setLogic() {
    //perspectiveOn = vwr.getPerspectiveDepth();
    /* general logic:
     * 
     * 
     * 1) octant and DOTS are incompatible; octant preferred over dots
     * 2) If not BALL, ARCS, or DOTS, the rendering defaults to AXES
     * 3) If DOTS, then turn off ARCS and FILL
     * 
     * note that FILL serves to provide a cut-out for BALL and a 
     * filling for ARCS
     */

    bOptions[OPT_DOTS] &= !bOptions[OPT_WIREFRAME];
    bOptions[OPT_BALL] &= !bOptions[OPT_WIREFRAME];
    bOptions[OPT_FILL] &= !bOptions[OPT_WIREFRAME];
    fillArc = bOptions[OPT_FILL] && !bOptions[OPT_BALL];
    if (fillArc)
      g3d.addRenderer(T.triangles);

    if (bOptions[OPT_BALL])
      bOptions[OPT_DOTS] = false;
    if (!bOptions[OPT_DOTS] && !bOptions[OPT_ARCS] && !bOptions[OPT_BALL])
      bOptions[OPT_AXES] = true;
    if (bOptions[OPT_DOTS]) {
      bOptions[OPT_ARCS] = false;
      bOptions[OPT_FILL] = false;
      dotScale = vwr.getInt(T.dotscale);
    }

    if (bOptions[OPT_DOTS]) {
      dotCount = ((Integer) vwr.getP("ellipsoidDotCount"))
          .intValue();
      if (coords == null || coords.length != dotCount * 3)
        coords = new int[dotCount * 3];
    }
  }
  
  private boolean renderEllipsoids(Map<?, Ellipsoid> ht, boolean isSimple) {
    boolean needTranslucent = false;
    Atom atom = null;
    for (Ellipsoid ellipsoid: ht.values()) {
      if (!ellipsoid.visible)
        continue;
      if (isSimple) {
        colix = ellipsoid.colix;
      } else {
        atom = ms.at[ellipsoid.tensor.atomIndex1];
        if (atom.sZ <= 1 || !isVisibleForMe(atom))
          continue;
        colix = C.getColixInherited(ellipsoid.colix, atom.colixAtom);
      }
      if (!g3d.setC(colix)) {
        needTranslucent = true;
        continue;
      }
      tm.transformPtScrT3(ellipsoid.center, s0);
      renderOne(ellipsoid);
    }
    return needTranslucent;
  }

  private void renderOne(Ellipsoid e) {
    center = e.center;
    // for extremely flat ellipsoids, we need at least some length
    int maxPt = 2;
    double maxLen = 0;
    for (int i = 3; --i >= 0;) {
      double f = factoredLengths[i] = Math.max(e.getLength(i), 0.02f);
      if (f > maxLen) {
        maxLen = f;
        maxPt = i;
      }        
    }
    V3d[] axesd = e.tensor.eigenVectors;
    axesd[0].putP(axes[0]);
    axesd[1].putP(axes[1]);
    axesd[2].putP(axes[2]);
    setMatrices();
    setAxes(maxPt);
    if (g3d.isClippedXY(dx + dx, (int) s0.x, (int) s0.y))
      return;
    eigenSignMask = e.tensor.eigenSignMask;
    setOptions(e.options);
    diameter = (int) vwr.tm.scaleToScreen((int) s0.z, bOptions[OPT_WIREFRAME] ? 1 : diameter0);
    if (e.tensor.isIsotropic) {
      renderBall();
      return;
    }
    if (bOptions[OPT_BALL]) {
      renderBall();
      if (bOptions[OPT_ARCS] || bOptions[OPT_AXES]) {
        g3d.setC(vwr.cm.colixBackgroundContrast);
        //setAxes(atom, 1.0d);
        if (bOptions[OPT_AXES])
          renderAxes();
        if (bOptions[OPT_ARCS])
          renderArcs();
        g3d.setC(colix);
      }
    } else {
      if (bOptions[OPT_AXES])
        renderAxes();
      if (bOptions[OPT_ARCS])
        renderArcs();      
    }
    if (bOptions[OPT_DOTS])
      renderDots();
    if (bOptions[OPT_ARROWS])
      renderArrows();
  }

  private void setMatrices() {

    // Create a matrix that transforms cartesian coordinates
    // into ellipsoidal coordinates, where in that system we 
    // are drawing a sphere. 
    
    for (int i = 0; i < 3; i++) {
      v1.setT(axes[i]);
      v1.scale(factoredLengths[i]);
      mat.setColumnV(i, v1);
    }
    mat.invertM(mat);
    // make this screen coordinates to ellisoidal coordinates
    matScreenToEllipsoid.mul2(mat, matScreenToCartesian);
    matEllipsoidToScreen.invertM(matScreenToEllipsoid);
    perspectiveFactor = vwr.tm.scaleToPerspective((int) s0.z, 1.0d);
    matScreenToEllipsoid.scale(1d/perspectiveFactor);
  }
  
  private final static V3d[] unitAxisVectors = {
    JC.axisNX, JC.axisX, 
    JC.axisNY, JC.axisY, 
    JC.axisNZ, JC.axisZ };

  private final P3d[] screens = new P3d[38];
  private final P3d[] points = new P3d[6];
  {
    for (int i = 0; i < points.length; i++)
      points[i] = new P3d();
    for (int i = 0; i < screens.length; i++)
      screens[i] = new P3d();
  }

  private static int[] axisPoints = {-1, 1, -2, 2, -3, 3};
  
  // octants are sets of three axisPoints references in proper rotation order
  // axisPoints[octants[i]] indicates the axis and direction (pos/neg)

  private static int[] octants = {
    5, 0, 3,
    5, 2, 0, //arc
    4, 0, 2,
    4, 3, 0, //arc
    5, 2, 1, 
    5, 1, 3, //arc
    4, 3, 1, 
    4, 1, 2  //arc
  };

  private void setAxes(int maxPt) {
    for (int i = 0; i < 6; i++) {
      int iAxis = axisPoints[i];
      int i012 = Math.abs(iAxis) - 1;
      points[i].scaleAdd2(factoredLengths[i012] * (iAxis < 0 ? -1 : 1),
          axes[i012], center);
      pt1.setT(unitAxisVectors[i]);
      //pt1.scale(f);

      matEllipsoidToScreen.rotate(pt1);
      screens[i].set(Math.round(s0.x + pt1.x * perspectiveFactor), Math
          .round(s0.y + pt1.y * perspectiveFactor), Math.round(pt1.z + s0.z));
      screens[i + 32].set(Math.round(s0.x + pt1.x * perspectiveFactor * 1.05d),
          Math.round(s0.y + pt1.y * perspectiveFactor * 1.05d), Math
              .round(pt1.z * 1.05d + s0.z));
    }
    dx = 2 + (int) vwr.tm.scaleToScreen((int) s0.z, (int) Math
        .round((Double.isNaN(factoredLengths[maxPt]) ? 1.0d
            : factoredLengths[maxPt]) * 1000));
  }

  private void renderBall() {
    setSelectedOctant();
    // get equation and differential
    Ellipsoid.getEquationForQuadricWithCenter(s0.x, s0.y, s0.z, 
        matScreenToEllipsoid, v1, mTemp, coefs, mDeriv);
    g3d.fillEllipsoid(center, points, (int) s0.x, (int) s0.y, (int) s0.z, dx + dx, matScreenToEllipsoid,
        coefs, mDeriv, selectedOctant, selectedOctant >= 0 ? selectedPoints : null);
  }

  private void renderArrows() {
    for (int i = 0; i < 6; i += 2) {
      int pt = (i == 0 ? 1 : i);
      fillConeScreen(screens[i], screens[i + 1], (eigenSignMask & pt) != 0);
    }
  }
  private void fillConeScreen(P3d p1, P3d p2, boolean isPositive) {
    if (diameter == 0)
      return;
    double diam = (diameter == 0 ? 1 : diameter) * 8;
    v1.set(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z);
    v1.normalize();
    v1.scale(diam);
    s1.setT(p1);
    s2.setT(p1);
    if (isPositive) {
      s2.x -= (int) v1.x; 
      s2.y -= (int) v1.y; 
      s2.z -= (int) v1.z; 
    } else {
      s1.x -= (int) v1.x; 
      s1.y -= (int) v1.y; 
      s1.z -= (int) v1.z; 
    }
    g3d.fillConeScreen3f(GData.ENDCAPS_FLAT, (int) diam, s1, s2, false); 
    s1.setT(p2);
    s2.setT(p2);
    if (isPositive) {
      s2.x += (int) v1.x; 
      s2.y += (int) v1.y; 
      s2.z += (int) v1.z; 
    } else {
      s1.x += (int) v1.x; 
      s1.y += (int) v1.y; 
      s1.z += (int) v1.z; 
    }
    g3d.fillConeScreen3f(GData.ENDCAPS_FLAT, (int) diam, s1, s2, false); 
  }

  private void renderAxes() {
    if (bOptions[OPT_BALL] && bOptions[OPT_FILL]) {
      g3d.fillCylinderBits(GData.ENDCAPS_FLAT, diameter, s0,
          selectedPoints[0]);
      g3d.fillCylinderBits(GData.ENDCAPS_FLAT, diameter, s0,
          selectedPoints[1]);
      g3d.fillCylinderBits(GData.ENDCAPS_FLAT, diameter, s0,
          selectedPoints[2]);
      return;
    }

//    if (Logger.debugging) {
//      g3d.setColix(GData.RED);
//      g3d.fillCylinder(GData.ENDCAPS_FLAT, diameter, screens[0],
//          screens[1]);
//      g3d.setColix(GData.GREEN);
//      g3d.fillCylinder(GData.ENDCAPS_FLAT, diameter, screens[2],
//          screens[3]);
//      g3d.setColix(GData.BLUE);
//      g3d.fillCylinder(GData.ENDCAPS_FLAT, diameter, screens[4],
//          screens[5]);
//      g3d.setColix(colix);
//    } else {
    if (bOptions[OPT_BALL]) {
      g3d.fillCylinderBits(GData.ENDCAPS_FLAT, diameter, screens[32],
          screens[33]);
      g3d.fillCylinderBits(GData.ENDCAPS_FLAT, diameter, screens[34],
          screens[35]);
      g3d.fillCylinderBits(GData.ENDCAPS_FLAT, diameter, screens[36],
          screens[37]);
//    }
    } else {
      g3d.fillCylinderBits(GData.ENDCAPS_FLAT, diameter, screens[0],
          screens[1]);
      g3d.fillCylinderBits(GData.ENDCAPS_FLAT, diameter, screens[2],
          screens[3]);
      g3d.fillCylinderBits(GData.ENDCAPS_FLAT, diameter, screens[4],
          screens[5]);
    }

  }
  private void renderDots() {
    for (int i = 0; i < coords.length;) {
      double fx = Math.random();
      double fy = Math.random();
      fx *= (Math.random() > 0.5 ? -1 : 1);
      fy *= (Math.random() > 0.5 ? -1 : 1);
      double fz = Math.sqrt(1 - fx * fx - fy * fy);
      if (Double.isNaN(fz))
        continue;
      fz = (Math.random() > 0.5 ? -1 : 1) * fz;
      pt1.scaleAdd2(fx * factoredLengths[0], axes[0], center);
      pt1.scaleAdd2(fy * factoredLengths[1], axes[1], pt1);
      pt1.scaleAdd2(fz * factoredLengths[2], axes[2], pt1);
      tm.transformPtScrT3(pt1, s1);
      coords[i++] = (int) s1.x;
      coords[i++] = (int) s1.y;
      coords[i++] = (int) s1.z;
    }
    g3d.drawPoints(dotCount, coords, dotScale);
  }

  private void renderArcs() {
    if (g3d.drawEllipse(center, points[0], points[2], fillArc, bOptions[OPT_WIREFRAME])) {
      g3d.drawEllipse(center, points[2], points[5], fillArc, bOptions[OPT_WIREFRAME]);
      g3d.drawEllipse(center, points[5], points[0], fillArc, bOptions[OPT_WIREFRAME]);
      return;
    }
    for (int i = 1, pt = 3; i < 8; i += 2, pt += 6) {
    //if (i == 3 || i == 7) 
    	renderArc(octants[pt], octants[pt + 1]);
      renderArc(octants[pt + 1], octants[pt + 2]);
      renderArc(octants[pt + 2], octants[pt]);
    }
  }
  
  private void renderArc(int ptA, int ptB) {
    v1.sub2(points[ptA], center);
    v2.sub2(points[ptB], center);
    double d1 = v1.length();
    double d2 = v2.length();
    v1.normalize();
    v2.normalize();
    v3.cross(v1, v2);
    pt1.setT(points[ptA]);
    s1.setT(screens[ptA]);
    short normix = Normix.get2SidedNormix(v3, bsTemp);
    if (!fillArc && !bOptions[OPT_WIREFRAME])
      screens[6].setT(s1);
    for (int i = 0, pt = 0; i < 18; i++, pt += 2) {
      pt2.scaleAdd2(cossin[pt] * d1, v1, center);
      pt2.scaleAdd2(cossin[pt + 1] * d2, v2, pt2);
      tm.transformPtScrT3(pt2, s2);
      if (fillArc) {
//       colix = (short) Math.ceil(Math.random()  * 20);
//        if (i < 4 && i > 1)
        g3d.fillTriangle3CNBits(s0, colix, normix, s1, colix, normix, s2, colix,
            normix, true);
      }
      else if (bOptions[OPT_WIREFRAME])
        g3d.fillCylinderBits(GData.ENDCAPS_FLAT, diameter, s1, s2);
      else
        screens[i + 7].setT(s2);
      pt1.setT(pt2);
      s1.setT(s2);
    }
    if (!fillArc && !bOptions[OPT_WIREFRAME]) {
      g3d.addRenderer(T.hermitelevel);
      for (int i = 0; i < 18; i++) {
        g3d.fillHermite(5, diameter, diameter, diameter, 
            screens[i == 0 ? i + 6 : i + 5], 
            screens[i + 6], 
            screens[i + 7], 
            screens[i == 17 ? i + 7 : i + 8]);
      }
    }
  }


  private void setSelectedOctant() {
    int zMin = Integer.MAX_VALUE;
    selectedOctant = -1;
    iCutout = -1;
    if (bOptions[OPT_FILL]) {
      for (int i = 0; i < 8; i++) {
        int ptA = octants[i * 3];
        int ptB = octants[i * 3 + 1];
        int ptC = octants[i * 3 + 2];
        int z = (int) (screens[ptA].z + screens[ptB].z + screens[ptC].z);
        if (z < zMin) {
          zMin = z;
          iCutout = i;
        }
      }
      //TODO -- adjust x and y for perspective?
      s1.setT(selectedPoints[0] = screens[octants[iCutout * 3]]);
      s1.add(selectedPoints[1] = screens[octants[iCutout * 3 + 1]]);
      s1.add(selectedPoints[2] = screens[octants[iCutout * 3 + 2]]);
      s1.scaleAdd2(-3, s0, s1);
      pt1.set(s1.x, s1.y, s1.z);
      matScreenToEllipsoid.rotate(pt1);
        int i = 0;
        if (pt1.x < 0)
          i |= 1;
        if (pt1.y < 0)
          i |= 2;
        if (pt1.z < 0)
          i |= 4;
       selectedOctant = i;
    }
  }  
}
