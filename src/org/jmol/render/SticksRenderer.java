/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2025-01-01 10:38:21 -0600 (Wed, 01 Jan 2025) $
 * $Revision: 22650 $

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

import org.jmol.c.PAL;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.script.T;
import org.jmol.shape.Sticks;
import org.jmol.util.C;
import org.jmol.util.Edge;
import org.jmol.util.GData;
import org.jmol.util.Point3fi;
import org.jmol.viewer.JC;

import javajs.util.A4d;
import javajs.util.BS;
import javajs.util.M3d;
import javajs.util.P3d;
import javajs.util.P3i;
import javajs.util.V3d;

public class SticksRenderer extends FontLineShapeRenderer {

  public static final int FLAG_STRUTS_ONLY = 1;

  private boolean showMultipleBonds;
  private double multipleBondSpacing;
  private double multipleBondRadiusFactor;
  private boolean bondsPerp;
  private boolean useBananas;
  private byte modeMultipleBond;
  private boolean isCartesian;
  //boolean showHydrogens;
  private byte endcaps;

  private boolean ssbondsBackbone;
  private boolean hbondsBackbone;
  private boolean bondsBackbone;
  private boolean hbondsSolid;

  private Atom a, b;
  private Point3fi pa, pb;
  private Bond bond;
  private int xA, yA, zA;
  private int xB, yB, zB;
  private int dx, dy;
  private int mag2d;
  private int bondOrder;
  private boolean wireframeOnly;
  private boolean isAntialiased;
  private boolean slabbing;
  private boolean slabByAtom;

  private final V3d x = new V3d();
  private final V3d y = new V3d();
  private final V3d z = new V3d();
  private final P3d p1 = new P3d();
  private final P3d p2 = new P3d();
  private final BS bsForPass2 = BS.newN(64);
  private boolean isPass2;
  private boolean isPymol;
  private boolean strutsOnly;

  @Override
  protected boolean render() {

    Bond[] bonds = ms.bo;
    if (bonds == null)
      return false;
    isPass2 = vwr.gdata.isPass2;
    if (!isPass2)
      bsForPass2.clearAll();
    slabbing = tm.slabEnabled;
    slabByAtom = vwr.getBoolean(T.slabbyatom);
    endcaps = GData.ENDCAPS_SPHERICAL;
    dashDots = (vwr.getBoolean(T.partialdots) ? sixdots : dashes);
    isCartesian = (exportType == GData.EXPORT_CARTESIAN);
    getMultipleBondSettings(0);
    wireframeOnly = !vwr.checkMotionRendering(T.bonds);
    ssbondsBackbone = vwr.getBoolean(T.ssbondsbackbone);
    hbondsBackbone = vwr.getBoolean(T.hbondsbackbone);
    bondsBackbone = hbondsBackbone | ssbondsBackbone;
    hbondsSolid = vwr.getBoolean(T.hbondssolid);
    isAntialiased = g3d.isAntialiased();
    strutsOnly = ((flags & FLAG_STRUTS_ONLY) != 0);
    boolean needTranslucent = false;
    if (isPass2) {
      for (int i = bsForPass2.nextSetBit(0); i >= 0; i = bsForPass2
          .nextSetBit(i + 1)) {
        bond = bonds[i];
        if (bond != null)
          renderBond(i);
      }
    } else {
      for (int i = ms.bondCount; --i >= 0;) {
        bond = bonds[i];
        if (bond != null && (bond.shapeVisibilityFlags & myVisibilityFlag) != 0
            && renderBond(i)) {
          needTranslucent = true;
          bsForPass2.set(i);
        }
      }
    }
    return needTranslucent;
  }

  private boolean renderBond(int index) {
    Atom atomA0, atomB0;
    a = atomA0 = bond.atom1;
    b = atomB0 = bond.atom2;

    int order = bond.order & Edge.BOND_RENDER_MASK;

    if (bondsBackbone) {
      if (ssbondsBackbone && (order & Edge.BOND_SULFUR_MASK) != 0) {
        // for ssbonds, always render the sidechain,
        // then render the backbone version
        /*
         mth 2004 04 26
         No, we are not going to do this any more
         render(bond, atomA, atomB);
         */

        a = a.group.getLeadAtomOr(a);
        b = b.group.getLeadAtomOr(b);
      } else if (hbondsBackbone && Edge.isOrderH(order)) {
        a = a.group.getLeadAtomOr(a);
        b = b.group.getLeadAtomOr(b);
      }
    }
    if (!isPass2 && (!a.isVisible(Atom.ATOM_INFRAME_NOTHIDDEN)
        || !b.isVisible(Atom.ATOM_INFRAME_NOTHIDDEN)
        || !g3d.isInDisplayRange(a.sX, a.sY)
        || !g3d.isInDisplayRange(b.sX, b.sY)))
      return false;

    if (slabbing) {
      boolean ba = vwr.gdata.isClippedZ(a.sZ);
      if (ba && vwr.gdata.isClippedZ(b.sZ)
          || slabByAtom && (ba || vwr.gdata.isClippedZ(b.sZ)))
        return false;
    }
    zA = a.sZ;
    zB = b.sZ;
    if (zA == 1 || zB == 1)
      return false;
    colixA = atomA0.colixAtom;
    colixB = atomB0.colixAtom;
    if (((colix = bond.colix) & C.OPAQUE_MASK) == C.USE_PALETTE) {
      colix = (short) (colix & ~C.OPAQUE_MASK);
      colixA = C.getColixInherited(
          (short) (colix | vwr.cm.getColixAtomPalette(atomA0, PAL.CPK.id)),
          colixA);
      colixB = C.getColixInherited(
          (short) (colix | vwr.cm.getColixAtomPalette(atomB0, PAL.CPK.id)),
          colixB);
    } else {
      colixA = C.getColixInherited(colix, colixA);
      colixB = C.getColixInherited(colix, colixB);
    }
    boolean needTranslucent = false;
    boolean skip = false;
    bondOrder = order & Edge.BOND_RENDER_MASK;
    boolean isStrut = (bondOrder == Edge.BOND_STRUT);
    if (strutsOnly != isStrut) {
      if (isStrut) {
        ((Sticks) shape).haveStrutPoints = true;
        if (!isPass2) {
          a.group.unsetStrutPoint();
          b.group.unsetStrutPoint();
        }
      }
      skip = true;
    }
    
    if (!isExport && !isPass2) {
      boolean doA = !C.renderPass2(colixA);
      boolean doB = !C.renderPass2(colixB);
      if (!doA || !doB) {
        if (isStrut || !doA && !doB) {
          skip = true;
        }
        g3d.setC(!doA ? colixB : colixA);
        needTranslucent = true;
      }
    }
    if (skip)
      return needTranslucent;

    // set the rendered bond order

    boolean isPartial = bond.isPartial();
    if (!isPartial) {
      if ((bondOrder & Edge.BOND_SULFUR_MASK) != 0)
        bondOrder &= ~Edge.BOND_SULFUR_MASK;
      if ((bondOrder & Edge.BOND_COVALENT_MASK) != 0) {
        if (!showMultipleBonds
            || (modeMultipleBond == JC.MULTIBOND_NOTSMALL
                && mad > JC.madMultipleBondSmallMaximum)
            || (bondOrder & Edge.BOND_PYMOL_MULT) == Edge.BOND_RENDER_SINGLE) {
          bondOrder = 1;
        }
      }
    }

    pa = a;
    pb = b;

    // set the mask

    int mask = 0;
    switch (bondOrder) {
    case Edge.BOND_STEREO_NEAR:
    case Edge.BOND_STEREO_FAR:
    case Edge.BOND_STEREO_EITHER:
      bondOrder = 1;
      //$FALL-THROUGH$
    case 1:
    case 2:
    case 3:
    case 4:
    case 5:
    case 6:
      break;
    case Edge.BOND_ORDER_UNSPECIFIED:
    case Edge.BOND_AROMATIC_SINGLE:
      bondOrder = 1;
      mask = (order == Edge.BOND_AROMATIC_SINGLE ? 0 : 1);
      break;
    case Edge.BOND_AROMATIC:
    case Edge.BOND_AROMATIC_DOUBLE:
      bondOrder = 2;
      mask = (order == Edge.BOND_AROMATIC ? getAromaticDottedBondMask() : 0);
      break;
    default:
      if ((bondOrder & Edge.BOND_PYMOL_MULT) == Edge.BOND_PYMOL_MULT) {
        getMultipleBondSettings(bondOrder);
        bondOrder &= 3;
        mask = -2;
      } else if (isPartial) {
        bondOrder = Edge.getPartialBondOrder(order);
        mask = Edge.getPartialBondDotted(order);
      } else if (Edge.isOrderH(bondOrder)) {
        bondOrder = 1;
        if (!hbondsSolid)
          mask = -1;
      } else if (isStrut) {
        bondOrder = 1;
        Point3fi p1 = a.group.strutPoint;
        Point3fi p2 = b.group.strutPoint;
        pa = setStrutPoint(p1, pa);
        pb = setStrutPoint(p2, pb);
        zA = pa.sZ;
        zB = pb.sZ;
      }
    }

    xA = pa.sX;
    yA = pa.sY;
    xB = pb.sX;
    yB = pb.sY;

    // set the diameter

    mad = bond.mad;
    if (multipleBondRadiusFactor > 0 && bondOrder > 1)
      mad *= multipleBondRadiusFactor;
    dx = xB - xA;
    dy = yB - yA;
    width = (int) vwr.tm.scaleToScreen((zA + zB) / 2, mad);
    if (wireframeOnly && width > 0)
      width = 1;
    if (!isCartesian) {
      asLineOnly = (width <= 1);
      if (asLineOnly && (isAntialiased)) {
        width = 3;
        asLineOnly = false;
      }
    }

    // draw the bond
    int renderD = (!isExport || mad == 1 ? width : mad);
    switch (mask) {
    case -2:
      drawBond(0);
      getMultipleBondSettings(0);
      break;
    case -1:
      drawDashedCylinder(g3d, xA, yA, zA, xB, yB, zB, hDashes, width, colixA,
          colixB, renderD, asLineOnly, s1);
      break;
    default:
      switch (bondOrder) {
      case 4: {
        bondOrder = 2;
        double f = multipleBondRadiusFactor;
        if (f == 0 && width > 1)
          width = (int) (width * 0.5);
        double m = multipleBondSpacing;
        if (m < 0)
          multipleBondSpacing = 0.30d;
        drawBond(mask);
        bondsPerp = !bondsPerp;
        bondOrder = 2;
        drawBond(mask >> 2);
        bondsPerp = !bondsPerp;
        multipleBondSpacing = m;
        isPymol = false;
      }
        break;
      case 5: {
        bondOrder = 3;
        double f = multipleBondRadiusFactor;
        if (f == 0 && width > 1)
          width = (int) (width * 0.5);
        double m = multipleBondSpacing;
        if (m < 0)
          multipleBondSpacing = 0.20d;
        drawBond(mask);
        bondsPerp = !bondsPerp;
        bondOrder = 2;
        multipleBondSpacing *= 1.5d;
        drawBond(mask >> 3);
        bondsPerp = !bondsPerp;
        multipleBondSpacing = m;
        isPymol = false;
      }
        break;
      case 6: {
        bondOrder = 4;
        double f = multipleBondRadiusFactor;
        if (f == 0 && width > 1)
          width = (int) (width * 0.5);
        double m = multipleBondSpacing;
        if (m < 0)
          multipleBondSpacing = 0.15d;
        drawBond(mask);
        bondsPerp = !bondsPerp;
        bondOrder = 2;
        multipleBondSpacing *= 1.5d;
        drawBond(mask >> 4);
        bondsPerp = !bondsPerp;
        multipleBondSpacing = m;
        isPymol = false;
      }
        break;
      default:
        drawBond(mask);
      }
      break;
    }
    return needTranslucent;
  }

  private Point3fi setStrutPoint(Point3fi p, Point3fi a) {
    if (p.sX == Integer.MIN_VALUE)
      return a;
    P3i pi = vwr.tm.transformPt(p);
    p.sX = pi.x;
    p.sY = pi.y;
    p.sZ = pi.z;
    return p;
  }

  private void getMultipleBondSettings(int pymolBondOrder) {
    isPymol = (pymolBondOrder != 0);
    if (isPymol) {
      bondsPerp = false;
      // scale is encoded in 6 bits starting at the third bit
      double scale = ((pymolBondOrder >> 2) & 0x3F) / 50d;
      showMultipleBonds = (scale != 0);
      int n = (pymolBondOrder & 3);
      // This is a comb calculation where n teeth of width w are
      // separated by a gap of width w/2. This actually looks pretty good!
      // The "teeth" span the width of the bond (mad/1000) * scale
      //
      // |//////  n teeth, each with fractional width fw
      // |//////
      // |        n-1 gaps, each fractional width fw/2
      // |//////
      // |//////
      // 
      // n*fw+(n-1)(fw/2) must fill the scaled span, 
      // so fw = 1/(n + (n-1)/2)*scale = 2/(3n-1)*scale 
      // and w = fw*(mad/1000)
      // thus:      
      multipleBondRadiusFactor = scale * 2 / (3 * n - 1);
      // The center-to-center spacing is (3/2)w  (y, below)
      multipleBondSpacing = multipleBondRadiusFactor * 3 / 2
          * (bond.mad / 1000);
      // And x, the starting offset from center, is (1-fw)/2, scaled appropriately
      // (one minus half the fractional width top and bottom, divided by two).
      // But we can also say for convenience that it is just half the spacing for 
      // a double bond and equal to the spacing for a triple bond. 
      // So the calculation we use below is just (n-1)/2 * (multipleBondSpacing). 
      // But that is not the general case. The general value for the "factional" 
      // offset is (1-fw)/2, which is (1-2/(3n-1))/2 = (3n-3)/(3n-1)/2 = (3/2)(n-1)/(3n-1).
      // For a double bond, this is (3/2)/5 = 3/10, and for a triple bond, it is 
      // (3/2)(2)/8 = 3/8. For a quadruple bond, it would be (3/2)(3)/11 = 9/22. 
      // (which is half of 18/22, which is 1-4/22 = 1-2/11), etc.
    } else {
      useBananas = (vwr.getBoolean(T.multiplebondbananas) && !isPymol);
      // negative spacing is relative, depending upon atom-atom distance;
      // positive spacing is absolute, for fixed in-plane (radiusFactor > 0) or perp-plane (radiusFactor < 0)
      multipleBondSpacing = vwr.getDouble(T.multiplebondspacing);
      // negative radius factor indicates perpendicular fixed double bond
      multipleBondRadiusFactor = vwr.getDouble(T.multiplebondradiusfactor);
      bondsPerp = (useBananas
          || multipleBondSpacing > 0 && multipleBondRadiusFactor < 0);
      if (useBananas)
        multipleBondSpacing = (multipleBondSpacing < 0
            ? -multipleBondSpacing * 0.4d
            : multipleBondSpacing);
      multipleBondRadiusFactor = Math.abs(multipleBondRadiusFactor);
      if (multipleBondSpacing == 0 && isCartesian)
        multipleBondSpacing = 0.2d;
    }
    modeMultipleBond = vwr.g.modeMultipleBond;
    showMultipleBonds = (multipleBondSpacing != 0
        && modeMultipleBond != JC.MULTIBOND_NEVER
        && vwr.getBoolean(T.showmultiplebonds));
  }

  private void drawBond(int dottedMask) {
    boolean isDashed = (dottedMask & 1) != 0;
    byte endcaps = ((colixA & C.TRANSLUCENT_MASK) == C.TRANSPARENT
        || (colixB & C.TRANSLUCENT_MASK) == C.TRANSPARENT ? GData.ENDCAPS_FLAT
            : this.endcaps);
    if (isCartesian && bondOrder == 1 && !isDashed) {
      // bypass screen rendering and just use the atoms themselves
      g3d.drawBond(pa, pb, colixA, colixB, endcaps, mad, -1);
      return;
    }
    boolean isEndOn = (dx == 0 && dy == 0);
    if (isEndOn && asLineOnly && !isCartesian)
      return;
    int renderD = (!isExport || mad == 1 ? width : mad);
    boolean doFixedSpacing = (bondOrder > 1
        && (isPymol || multipleBondSpacing > 0));
    boolean isPiBonded = doFixedSpacing
        && (vwr.getHybridizationAndAxes(a.i, z, x, "pz") != null
            || vwr.getHybridizationAndAxes(b.i, z, x, "pz") != null)
        && !Double.isNaN(x.x);
    if (isEndOn && !doFixedSpacing) {
      // end-on view
      int space = width / 8 + 3;
      int step = width + space;
      int y = yA - (bondOrder - 1) * step / 2;
      do {
        fillCylinder(g3d, colixA, colixB, endcaps, xA, y, zA, xB, y, zB,
            renderD, asLineOnly);
        y += step;
      } while (--bondOrder > 0);
      return;
    }
    if (bondOrder == 1) {
      if (isDashed)
        drawDashedCylinder(g3d, xA, yA, zA, xB, yB, zB, dashDots, width, colixA,
            colixB, renderD, asLineOnly, s1);
      else
        fillCylinder(g3d, colixA, colixB, endcaps, xA, yA, zA, xB, yB, zB,
            renderD, asLineOnly);
      return;
    }
    if (doFixedSpacing) {
      if (!isPiBonded) // obscure point
        z.setT(P3d.getUnlikely());
      x.sub2(b, a);
      y.cross(x, z);
      y.normalize();
      if (Double.isNaN(y.x)) {
        // in case x and z are parallel (O=C=O)
        z.setT(P3d.getUnlikely());
        y.cross(x, z);
        y.cross(y, x);
        y.normalize();
      }
      if (bondsPerp)
        y.cross(y, x);
      y.scale(multipleBondSpacing);
      x.setT(y);
      x.scale((bondOrder - 1) / 2d);
      if (useBananas) {
        drawBanana(a, b, x, 0);
        switch (bondOrder) {
        case 4:
          drawBanana(a, b, x, 90);
          drawBanana(a, b, x, -90);
          //$FALL-THROUGH$
        case 2:
        default:
          drawBanana(a, b, x, 180);
          break;
        case 3:
          drawBanana(a, b, x, 120);
          drawBanana(a, b, x, -120);
          break;
        }
        return;
      }
      p1.sub2(a, x);
      p2.sub2(b, x);

      while (true) {
        if (isCartesian) {
          // bypass screen rendering and just use the atoms themselves
          g3d.drawBond(p1, p2, colixA, colixB, endcaps, mad, -2);
        } else {
          tm.transformPtScr(p1, s1);
          tm.transformPtScr(p2, s2);
          if (isDashed)
            drawDashedCylinder(g3d, s1.x, s1.y, s1.z, s2.x, s2.y, s2.z,
                dashDots, width, colixA, colixB, renderD, asLineOnly, s1);
          else
            fillCylinder(g3d, colixA, colixB, endcaps, s1.x, s1.y, s1.z, s2.x,
                s2.y, s2.z, renderD, asLineOnly);
        }
        dottedMask >>= 1;
        isDashed = (dottedMask & 1) != 0;
        if (--bondOrder <= 0)
          break;
        p1.add(y);
        p2.add(y);
        stepAxisCoordinates();
      }
      return;
    }
    int dxB = dx * dx;
    int dyB = dy * dy;
    mag2d = Math.round((float) Math.sqrt(dxB + dyB));
    resetAxisCoordinates();
    if (isCartesian && bondOrder == 3) {
      fillCylinder(g3d, colixA, colixB, endcaps, xAxis1, yAxis1, zA, xAxis2,
          yAxis2, zB, renderD, asLineOnly);
      stepAxisCoordinates();
      x.sub2(b, a);
      x.scale(0.05d);
      p1.sub2(a, x);
      p2.add2(b, x);
      g3d.drawBond(p1, p2, colixA, colixB, endcaps, mad, -2);
      stepAxisCoordinates();
      fillCylinder(g3d, colixA, colixB, endcaps, xAxis1, yAxis1, zA, xAxis2,
          yAxis2, zB, renderD, asLineOnly);
      return;
    }
    while (true) {
      if ((dottedMask & 1) != 0)
        drawDashedCylinder(g3d, xAxis1, yAxis1, zA, xAxis2, yAxis2, zB,
            dashDots, width, colixA, colixB, renderD, asLineOnly, s1);
      else
        fillCylinder(g3d, colixA, colixB, endcaps, xAxis1, yAxis1, zA, xAxis2,
            yAxis2, zB, renderD, asLineOnly);
      dottedMask >>= 1;
      if (--bondOrder <= 0)
        break;
      stepAxisCoordinates();
    }
  }

  private int xAxis1, yAxis1, xAxis2, yAxis2, dxStep, dyStep;

  private void resetAxisCoordinates() {
    int space = mag2d >> 3;
    if (multipleBondSpacing != -1 && multipleBondSpacing < 0)
      space *= -multipleBondSpacing;
    int step = width + space;
    dxStep = step * dy / mag2d;
    dyStep = step * -dx / mag2d;
    xAxis1 = xA;
    yAxis1 = yA;
    xAxis2 = xB;
    yAxis2 = yB;
    int f = (bondOrder - 1);
    xAxis1 -= dxStep * f / 2;
    yAxis1 -= dyStep * f / 2;
    xAxis2 -= dxStep * f / 2;
    yAxis2 -= dyStep * f / 2;
  }

  private void stepAxisCoordinates() {
    xAxis1 += dxStep;
    yAxis1 += dyStep;
    xAxis2 += dxStep;
    yAxis2 += dyStep;
  }

  private int getAromaticDottedBondMask() {
    Atom atomC = b.findAromaticNeighbor(a.i);
    if (atomC == null)
      return 1;
    int dxAC = atomC.sX - xA;
    int dyAC = atomC.sY - yA;
    return ((dx * dyAC - dy * dxAC) < 0 ? 2 : 1);
  }

  private M3d rot;
  private A4d a4;

  private void drawBanana(Atom a, Atom b, V3d x, int deg) {
    g3d.addRenderer(T.hermitelevel);
    vectorT.sub2(b, a);
    if (rot == null) {
      rot = new M3d();
      a4 = new A4d();
    }
    a4.setVA(vectorT, (deg * Math.PI / 180));
    rot.setAA(a4);
    pointT.setT(a);
    pointT3.setT(b);
    pointT2.ave(a, b);
    rot.rotate2(x, vectorT);
    pointT2.add(vectorT);
    tm.transformPtScrT3(a, pointT);
    tm.transformPtScrT3(pointT2, pointT2);
    tm.transformPtScrT3(b, pointT3);
    int w = Math.max(width, 1);
    g3d.setC(colixA);
    g3d.fillHermite(5, w, w, w, pointT, pointT, pointT2, pointT3);
    g3d.setC(colixB);
    g3d.fillHermite(5, w, w, w, pointT, pointT2, pointT3, pointT3);
  }

}
