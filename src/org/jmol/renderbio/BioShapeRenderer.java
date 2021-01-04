/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2017-04-03 23:13:04 -0500 (Mon, 03 Apr 2017) $
 * $Revision: 21475 $
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

import org.jmol.c.STR;
import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelsetbio.CarbohydratePolymer;
import org.jmol.modelsetbio.Monomer;
import org.jmol.modelsetbio.NucleicPolymer;
import org.jmol.modelsetbio.PhosphorusPolymer;
import org.jmol.render.ShapeRenderer;
import org.jmol.script.T;
import org.jmol.shapebio.BioShape;
import org.jmol.shapebio.BioShapeCollection;
import org.jmol.util.C;
import org.jmol.util.GData;

import javajs.api.Interface;
import javajs.util.P3;
import javajs.util.V3;

/**
   * @author Alexander Rose
   * @author Bob Hanson
   * 
 */
abstract class BioShapeRenderer extends ShapeRenderer {

  //ultimately this renderer calls MeshRenderer.render1(mesh)

  private boolean invalidateMesh;
  private boolean invalidateSheets;
  private boolean isTraceAlpha;
  private boolean ribbonBorder = false;
  private boolean haveControlPointScreens;
  float aspectRatio;
  int hermiteLevel;
  private float sheetSmoothing;
  protected boolean cartoonsFancy;

  protected int monomerCount;
  protected Monomer[] monomers;

  protected boolean isNucleic;
  protected boolean isPhosphorusOnly;
  protected boolean isCarbohydrate;
  protected BS bsVisible = new BS();
  protected P3[] ribbonTopScreens;
  protected P3[] ribbonBottomScreens;
  protected P3[] controlPoints;
  protected P3[] controlPointScreens;

  protected int[] leadAtomIndices;
  protected V3[] wingVectors;
  protected short[] mads;
  protected short[] colixes;
  protected short[] colixesBack;
  protected STR[] structureTypes;
  boolean isHighRes;
  
  protected boolean wireframeOnly;
  private boolean needTranslucent;
  BioMeshRenderer meshRenderer;
  BioShape bioShape;
  
  protected abstract void renderBioShape(BioShape bioShape);

  @Override
  protected boolean render() {
    if (shape == null)
      return false;
    setGlobals();
    renderShapes();
    return needTranslucent;
  }

  private void setGlobals() {
    invalidateMesh = false;
    needTranslucent = false;
    g3d.addRenderer(T.hermitelevel);
    boolean TF = (!isExport && !vwr.checkMotionRendering(T.cartoon));

    if (TF != wireframeOnly)
      invalidateMesh = true;
    wireframeOnly = TF;

    TF = (isExport || !wireframeOnly && vwr.getBoolean(T.highresolution));
    if (TF != isHighRes)
      invalidateMesh = true;
    isHighRes = TF;

    TF = !wireframeOnly && (vwr.getBoolean(T.cartoonsfancy) || isExport);
    if (cartoonsFancy != TF) {
      invalidateMesh = true;
      cartoonsFancy = TF;
    }
    int val1 = vwr.getHermiteLevel();
    val1 = (val1 <= 0 ? -val1 : vwr.getInMotion(true) ? 0 : val1);
    if (cartoonsFancy && !wireframeOnly)
      val1 = Math.max(val1, 3); // at least HermiteLevel 3 for "cartoonFancy" and 
    //else if (val1 == 0 && exportType == GData.EXPORT_CARTESIAN)
    //val1 = 5; // forces hermite for 3D exporters
    if (val1 != hermiteLevel)// && val1 != 0)
      invalidateMesh = true;
    hermiteLevel = Math.min(val1, 8);

    int val = vwr.getInt(T.ribbonaspectratio);
    val = Math.min(Math.max(0, val), 20);
    if (cartoonsFancy && val >= 16)
      val = 4; // at most 4 for elliptical cartoonFancy
    if (wireframeOnly || hermiteLevel == 0)
      val = 0;

    if (val != aspectRatio && val != 0 && val1 != 0)
      invalidateMesh = true;
    aspectRatio = val;
    if (aspectRatio > 0) {
      if (meshRenderer == null) {
        meshRenderer = (BioMeshRenderer) Interface
            .getInterface("org.jmol.renderbio.BioMeshRenderer");
        meshRenderer.setViewerG3dShapeID(vwr, shape.shapeID);
      }
      meshRenderer.setup(g3d, vwr.ms, shape);
    }
    TF = vwr.getBoolean(T.tracealpha);
    if (TF != isTraceAlpha)
      invalidateMesh = true;
    isTraceAlpha = TF;

    invalidateSheets = false;
    float fval = vwr.getFloat(T.sheetsmoothing);
    if (fval != sheetSmoothing && isTraceAlpha) {
      sheetSmoothing = fval;
      invalidateMesh = true;
      invalidateSheets = true;
    }
  }
  
  private void renderShapes() {

    BioShapeCollection mps = (BioShapeCollection) shape;
    for (int c = mps.bioShapes.length; --c >= 0;) {
      bioShape = mps.getBioShape(c);
      if ((bioShape.modelVisibilityFlags & myVisibilityFlag) == 0)
        continue;
      if (bioShape.monomerCount >= 2 && initializePolymer(bioShape)) {
        if (meshRenderer != null)
          meshRenderer.initBS();    
        isCyclic = bioShape.bioPolymer.isCyclic();
        renderBioShape(bioShape);
        if (meshRenderer != null)
          meshRenderer.renderMeshes();
        freeTempArrays();
      }
    }
  }

  protected boolean setBioColix(short colix) {
    if (g3d.setC(colix))
      return true;
    needTranslucent = true;
    return false;
  }

  private void freeTempArrays() {
    if (haveControlPointScreens)
      vwr.freeTempPoints(controlPointScreens);
    vwr.freeTempEnum(structureTypes);
  }

  private boolean initializePolymer(BioShape bioShape) {
    BS bsDeleted = vwr.slm.bsDeleted;
    if (vwr.ms.isJmolDataFrameForModel(bioShape.modelIndex)) {
      controlPoints = bioShape.bioPolymer.getControlPoints(true, 0, false);
    } else {
      controlPoints = bioShape.bioPolymer.getControlPoints(isTraceAlpha,
          sheetSmoothing, invalidateSheets);
    }
    monomerCount = bioShape.monomerCount;
    monomers = bioShape.monomers;
    reversed = bioShape.bioPolymer.reversed;
    leadAtomIndices = bioShape.bioPolymer.getLeadAtomIndices();

    bsVisible.clearAll();
    boolean haveVisible = false;
    if (invalidateMesh)
      bioShape.falsifyMesh();
    for (int i = monomerCount; --i >= 0;) {
      if ((monomers[i].shapeVisibilityFlags & myVisibilityFlag) == 0
          || ms.isAtomHidden(leadAtomIndices[i]) || bsDeleted != null && bsDeleted.get(leadAtomIndices[i]))
        continue;
      Atom lead = ms.at[leadAtomIndices[i]];
      if (!g3d.isInDisplayRange(lead.sX, lead.sY))
        continue;
      bsVisible.set(i);
      haveVisible = true;
    }
    if (!haveVisible)
      return false;
    ribbonBorder = vwr.getBoolean(T.ribbonborder);

    // note that we are not treating a PhosphorusPolymer
    // as nucleic because we are not calculating the wing
    // vector correctly.
    // if/when we do that then this test will become
    // isNucleic = bioShape.bioPolymer.isNucleic();

    isNucleic = bioShape.bioPolymer instanceof NucleicPolymer;
    isPhosphorusOnly = !isNucleic && bioShape.bioPolymer instanceof PhosphorusPolymer;
    isCarbohydrate = bioShape.bioPolymer instanceof CarbohydratePolymer;
    haveControlPointScreens = false;
    wingVectors = bioShape.wingVectors;
    if (meshRenderer != null)
      meshRenderer.initialize(this, bioShape, monomerCount);
    mads = bioShape.mads;
    colixes = bioShape.colixes;
    colixesBack = bioShape.colixesBack;
    setStructureTypes();
    return true;
  }

  private void setStructureTypes() {
    STR[] types = structureTypes = vwr.allocTempEnum(monomerCount + 1);
    for (int i = monomerCount; --i >= 0;)
      if ((types[i] = monomers[i].getProteinStructureType()) == STR.TURN)
        types[i] = STR.NONE;
    types[monomerCount] = types[monomerCount - 1];
  }

  protected void calcScreenControlPoints() {
    int count = monomerCount + 1;
    P3[] scr = controlPointScreens = vwr.allocTempPoints(count);
    P3[] points = controlPoints;
    for (int i = count; --i >= 0;)
      tm.transformPtScrT3(points[i], scr[i]);
    haveControlPointScreens = true;
  }

  /**
   * calculate screen points based on control points and wing positions
   * (cartoon, strand, meshRibbon, and ribbon)
   * 
   * @param offsetFraction
   * @param mads 
   * @return Point3i array THAT MUST BE LATER FREED
   */
  protected P3[] calcScreens(float offsetFraction, short[] mads) {
    int count = controlPoints.length;
    P3[] screens = vwr.allocTempPoints(count);
    if (offsetFraction == 0) {
      for (int i = count; --i >= 0;)
        tm.transformPtScrT3(controlPoints[i], screens[i]);
    } else {
      float offset_1000 = offsetFraction / 1000f;
      for (int i = count; --i >= 0;)
        calc1Screen(controlPoints[i], wingVectors[i],
            (mads[i] == 0 && i > 0 ? mads[i - 1] : mads[i]), offset_1000,
            screens[i]);
    }
    return screens;
  }

  private final P3 pointT = new P3();

  private void calc1Screen(P3 center, V3 vector, short mad,
                           float offset_1000, P3 screen) {
    pointT.scaleAdd2(mad * offset_1000, vector, center);
    tm.transformPtScrT3(pointT, screen);
  }

  protected short getLeadColix(int i) {
    return C.getColixInherited(colixes[i], monomers[i].getLeadAtom()
        .colixAtom);
  }

  protected short getLeadColixBack(int i) {
    return (colixesBack == null || colixesBack.length <= i ? 0 : colixesBack[i]);
  }

  //// cardinal hermite constant cylinder (meshRibbon, strands)

  int iPrev, iNext, iNext2, iNext3;
  int diameterBeg, diameterMid, diameterEnd;
  short madBeg, madMid, madEnd;
  short colixBack;
  private BS reversed;
  private boolean isCyclic;

  void setNeighbors(int i) {
    if (isCyclic) {
      i += monomerCount;
      iPrev = (i - 1) % monomerCount;
      iNext = (i + 1) % monomerCount;
      iNext2 = (i + 2) % monomerCount;
      iNext3 = (i + 3) % monomerCount;
    } else {
      iPrev = Math.max(i - 1, 0);
      iNext = Math.min(i + 1, monomerCount);
      iNext2 = Math.min(i + 2, monomerCount);
      iNext3 = Math.min(i + 3, monomerCount);
    }
  }

  protected boolean setColix(short colix) {
    this.colix = colix;
    return g3d.setC(colix);
  }

  /**
   * set diameters for a bioshape
   * 
   * @param i
   * @param thisTypeOnly true for Cartoon but not MeshRibbon
   * @return true if a mesh is needed
   */
  private boolean setMads(int i, boolean thisTypeOnly) {
    madMid = madBeg = madEnd = mads[i];
    if (isTraceAlpha) {
      if (!thisTypeOnly || structureTypes[i] == structureTypes[iNext]) {
        madEnd = mads[iNext];
        if (madEnd == 0) {
          if (this instanceof TraceRenderer) {
            madEnd = madBeg;
          } else {
            madEnd = madBeg;
          }
        }
        madMid = (short) ((madBeg + madEnd) >> 1);
      }
    } else {
      if (!thisTypeOnly || structureTypes[i] == structureTypes[iPrev])
        madBeg = (short) (((mads[iPrev] == 0 ? madMid : mads[iPrev]) + madMid) >> 1);
      if (!thisTypeOnly || structureTypes[i] == structureTypes[iNext])
        madEnd = (short) (((mads[iNext] == 0 ? madMid : mads[iNext]) + madMid) >> 1);
    }
    diameterBeg = (int) vwr.tm.scaleToScreen((int) controlPointScreens[i].z, madBeg);
    diameterMid = (int) vwr.tm.scaleToScreen(monomers[i].getLeadAtom().sZ,
        madMid);
    diameterEnd = (int) vwr.tm.scaleToScreen((int) controlPointScreens[iNext].z, madEnd);
    boolean doCap0 = (i == iPrev || !bsVisible.get(iPrev) || thisTypeOnly
        && structureTypes[i] != structureTypes[iPrev]);
    boolean doCap1 = (iNext == iNext2 || iNext2 == iNext3 || !bsVisible.get(iNext) || thisTypeOnly
        && structureTypes[i] != structureTypes[iNext]);
    return (aspectRatio > 0 && meshRenderer != null && meshRenderer.check(doCap0, doCap1));
  }

  protected void renderHermiteCylinder(P3[] screens, int i) {
    //strands
    colix = getLeadColix(i);
    if (!setBioColix(colix))
      return;
    setNeighbors(i);
    g3d.drawHermite4(isNucleic ? 4 : 7, screens[iPrev], screens[i],
        screens[iNext], screens[iNext2]);
  }

  protected void renderHermiteConic(int i, boolean thisTypeOnly, int tension) {
    //cartoons, rockets, trace
    setNeighbors(i);
    colix = getLeadColix(i);
    if (!setBioColix(colix))
      return;
    if (setMads(i, thisTypeOnly) || isExport) {
      meshRenderer.setFancyConic(i, tension);
      return;
    }
    if (diameterBeg == 0 && diameterEnd == 0 || wireframeOnly)
      g3d.drawLineAB(controlPointScreens[i], controlPointScreens[iNext]);
    else {
      g3d.fillHermite(isNucleic ? 4 : 7, diameterBeg, diameterMid, diameterEnd,
          controlPointScreens[iPrev], controlPointScreens[i],
          controlPointScreens[iNext], controlPointScreens[iNext2]);
    }
  }

  /**
   * 
   * @param doFill
   * @param i
   * @param thisTypeOnly true for Cartoon but not MeshRibbon
   */
  protected void renderHermiteRibbon(boolean doFill, int i, boolean thisTypeOnly) {
    // cartoons and meshRibbon

    setNeighbors(i);
    short c0 = colix = getLeadColix(i);
    if (!setBioColix(colix))
      return;
    short cb = colixBack = getLeadColixBack(i);
    if (doFill && (aspectRatio != 0 || isExport)) {
      if (setMads(i, thisTypeOnly) || isExport) {
        meshRenderer.setFancyRibbon(i);
        return;
      }
    }
    boolean isReversed = reversed.get(i);
    if (isReversed && colixBack != 0) {
      setColix(colixBack);
      cb = c0;
    }
    g3d.drawHermite7(doFill, ribbonBorder, (isReversed ? -1 : 1) * (isNucleic ? 4 : 7),
        ribbonTopScreens[iPrev], ribbonTopScreens[i], ribbonTopScreens[iNext],
        ribbonTopScreens[iNext2], ribbonBottomScreens[iPrev],
        ribbonBottomScreens[i], ribbonBottomScreens[iNext],
        ribbonBottomScreens[iNext2], (int) aspectRatio, cb);
    if (isReversed && colixBack != 0) {
      setColix(c0);
      cb = colixBack;
    }
  }

  //// cardinal hermite (box or flat) arrow head (cartoon)

  private final P3 screenArrowTop = new P3();
  private final P3 screenArrowTopPrev = new P3();
  private final P3 screenArrowBot = new P3();
  private final P3 screenArrowBotPrev = new P3();

  protected void renderHermiteArrowHead(int i) {
    // cartoons only
    colix = getLeadColix(i);
    if (!setBioColix(colix))
      return;
    colixBack = getLeadColixBack(i);
    setNeighbors(i);
    if (setMads(i, false) || isExport) {
      meshRenderer.setFancyArrowHead(i);
      return;
    }

    P3 cp = controlPoints[i];
    V3 wv = wingVectors[i];
    calc1Screen(cp, wv, madBeg, .0007f, screenArrowTop);
    calc1Screen(cp, wv, madBeg, -.0007f, screenArrowBot);
    calc1Screen(cp, wv, madBeg, 0.001f, screenArrowTopPrev);
    calc1Screen(cp, wv, madBeg, -0.001f, screenArrowBotPrev);
    g3d.drawHermite7(true, ribbonBorder, isNucleic ? 4 : 7, screenArrowTopPrev,
        screenArrowTop, controlPointScreens[iNext],
        controlPointScreens[iNext2], screenArrowBotPrev, screenArrowBot,
        controlPointScreens[iNext], controlPointScreens[iNext2],
        (int) aspectRatio, colixBack);
    g3d.setC(colix);
    if (ribbonBorder && aspectRatio == 0) {
      g3d.fillCylinderBits(GData.ENDCAPS_SPHERICAL,
          3,  
          screenArrowTop, screenArrowBot);
    }
  }
  
  protected void drawSegmentAB(Atom atomA, Atom atomB, short colixA, short colixB, float max) {
    int xA = atomA.sX, yA = atomA.sY, zA = atomA.sZ;
    int xB = atomB.sX, yB = atomB.sY, zB = atomB.sZ;
    int mad = this.mad;
    if (max == 1000)
      mad = mad >> 1;
    if (mad < 0) {
      g3d.drawLine(colixA, colixB, xA, yA, zA, xB, yB, zB);
    } else {
      int width = (int) (isExport ? mad : vwr.tm.scaleToScreen((zA + zB) / 2,
          mad));
      g3d.fillCylinderXYZ(colixA, colixB, GData.ENDCAPS_SPHERICAL, width, xA,
          yA, zA, xB, yB, zB);
    }
  }  

}
