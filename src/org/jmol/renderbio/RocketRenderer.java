package org.jmol.renderbio;

import javajs.util.P3d;
import javajs.util.V3d;

import org.jmol.api.JmolRendererInterface;
import org.jmol.c.STR;
import javajs.util.BS;
import org.jmol.modelsetbio.AlphaMonomer;
import org.jmol.modelsetbio.AminoPolymer;
import org.jmol.modelsetbio.Helix;
import org.jmol.modelsetbio.ProteinStructure;
import org.jmol.modelsetbio.Sheet;
import org.jmol.util.GData;
import org.jmol.util.MeshSurface;
import org.jmol.viewer.TransformManager;
import org.jmol.viewer.Viewer;

public class RocketRenderer {
  private boolean tPending;
  private ProteinStructure proteinstructurePending;
  private int startIndexPending;
  private int endIndexPending;

  private V3d vtemp;
  private P3d screenA, screenB, screenC;
  private short colix;
  private short mad;
  private RocketsRenderer rr;
  private Viewer vwr;
  private JmolRendererInterface g3d;
  private TransformManager tm;
  private boolean renderArrowHeads;
  private boolean isRockets;

  public RocketRenderer(){
  }
  
  RocketRenderer set(RocketsRenderer rr) {
    screenA = new P3d();
    screenB = new P3d();
    screenC = new P3d();
    vtemp = new V3d();
    this.rr = rr;
    vwr = rr.vwr;
    tm = rr.vwr.tm;
    isRockets = rr.isRockets;
    return this;
  }

  void renderRockets() {
    // doing the cylinders separately because we want to connect them if we can.

    // Key structures that must render properly
    // include 1crn and 7hvp

    g3d = rr.g3d;
    tPending = false;
    renderArrowHeads = rr.renderArrowHeads;
    BS bsVisible = rr.bsVisible;
    for (int i = bsVisible.nextSetBit(0); i >= 0; i = bsVisible
        .nextSetBit(i + 1)) {
      if (rr.structureTypes[i] == STR.HELIX || isRockets && rr.structureTypes[i] == STR.SHEET) {
        renderSpecialSegment((AlphaMonomer) rr.monomers[i], rr.getLeadColix(i), rr.mads[i]);
      } else if (isRockets) {
        renderPending();
        rr.renderHermiteConic(i, true, 7);
      }
    }
    renderPending();
  }

  private void renderSpecialSegment(AlphaMonomer monomer, short thisColix,
                                      short thisMad) {
    ProteinStructure proteinstructure = monomer.proteinStructure;
    if (tPending) {
      if (proteinstructure == proteinstructurePending && thisMad == mad
          && thisColix == colix
          && proteinstructure.getIndex(monomer) == endIndexPending + 1) {
        ++endIndexPending;
        return;
      }
      renderPending();
    }
    proteinstructurePending = proteinstructure;
    startIndexPending = endIndexPending = proteinstructure.getIndex(monomer);
    colix = thisColix;
    mad = thisMad;
    tPending = true;
  }

  private void renderPending() {
    if (!tPending)
      return;
    P3d[] segments = proteinstructurePending.getSegments();
    boolean renderArrowHead = (renderArrowHeads && endIndexPending == proteinstructurePending.nRes - 1);
    if (proteinstructurePending instanceof Helix) {
      renderPendingRocketSegment(endIndexPending, segments[startIndexPending],
          segments[endIndexPending], segments[endIndexPending + 1],
          renderArrowHead);
    } else if (proteinstructurePending instanceof Sheet 
        && ((Sheet)proteinstructurePending).apolymer instanceof AminoPolymer) {
      renderPendingSheetPlank(segments[startIndexPending],
          segments[endIndexPending], segments[endIndexPending + 1],
          renderArrowHead);
    } else {
      tPending = false;
      return;
    }
    if (rr.renderStruts) {
      rr.adjustStrut(segments, startIndexPending, endIndexPending, proteinstructurePending.monomerIndexFirst);
    }
    tPending = false;
  }

  /**
   * @param i
   * @param pointStart
   * @param pointBeforeEnd
   *        ignored now that arrow heads protrude beyond end of rocket
   * @param pointEnd
   * @param renderArrowHead
   */
  private void renderPendingRocketSegment(int i, P3d pointStart,
                                          P3d pointBeforeEnd, P3d pointEnd,
                                          boolean renderArrowHead) {
    if (g3d.setC(colix)) {
      tm.transformPt3f(pointStart, screenA);
      tm.transformPt3f((renderArrowHead ? pointBeforeEnd : pointEnd), screenB);
      int zMid = (int) Math.floor((screenA.z + screenB.z) / 2d);
      int diameter = ((int) vwr.tm.scaleToScreen(zMid, mad));
      if (!renderArrowHead || pointStart != pointBeforeEnd)
        g3d.fillCylinderBits(GData.ENDCAPS_FLAT, diameter, screenA, screenB);
      if (renderArrowHead) {
        screenA.sub2(pointEnd, pointBeforeEnd);
        tm.transformPt3f(pointEnd, screenC);
        int coneDiameter = (mad << 1) - (mad >> 1);
        coneDiameter = (int) vwr.tm.scaleToScreen(
            (int) Math.floor(screenB.z), coneDiameter);
        g3d.fillConeScreen3f(GData.ENDCAPS_FLAT, coneDiameter, screenB,
            screenC, false);
      }
      if (startIndexPending == endIndexPending)
        return;
      P3d t = screenB;
      screenB = screenC;
      screenC = t;
    }
  }

  private final static int[][] boxFaces =
  {
    { 0, 1, 3, 2 },
    { 0, 2, 6, 4 },
    { 0, 4, 5, 1 },
    { 7, 5, 4, 6 },
    { 7, 6, 2, 3 },
    { 7, 3, 1, 5 } };

  private final static int[][] arrowHeadFaces =
  {
    { 1, 0, 4 },
    { 2, 3, 5 },
    { 0, 1, 3, 2 },
    { 2, 5, 4, 0 },
    { 1, 4, 5, 3 }
  };
  
  private P3d ptC, ptTip;
  private P3d[] corners, screenCorners;
  private V3d vW, vH;
  private MeshSurface meshSurface;

  private void renderPendingSheetPlank(P3d ptStart, P3d pointBeforeEnd, P3d ptEnd,
                                       boolean renderArrowHead) {
    if (!g3d.setC(colix))
      return;
    if (corners == null) {
      ptC = new P3d();
      ptTip = new P3d();
      vW = new V3d();
      vH = new V3d();
      screenCorners = new P3d[8];
      corners = new P3d[8];
      for (int i = 8; --i >= 0;) {
        corners[i] = new P3d();
        screenCorners[i] = new P3d();
      }
    }
    if (renderArrowHead) {
      setBox(1.25d, 0.333f, pointBeforeEnd);
      ptTip.scaleAdd2(-0.5d, vH, ptEnd);
      for (int i = 4; --i >= 0;) {
        P3d corner = corners[i];
        corner.setT(ptC);
        if ((i & 1) != 0)
          corner.add(vW);
        if ((i & 2) != 0)
          corner.add(vH);
      }
      corners[4].setT(ptTip);
      corners[5].add2(ptTip, vH);
      renderPart(arrowHeadFaces);
      ptEnd = pointBeforeEnd;
    }
    setBox(1d, 0.25d, ptStart);
    vtemp.sub2(ptEnd, ptStart);
    if (vtemp.lengthSquared() == 0)
      return;
    buildBox(ptC, vW, vH, vtemp);
    renderPart(boxFaces);
  }
  
  private void setBox(double w, double h, P3d pt) {
    ((Sheet) proteinstructurePending).setBox(w, h, pt, vW, vH, ptC, mad / 1000d);
  }

  private void buildBox(P3d pointCorner, V3d scaledWidthVector,
                        V3d scaledHeightVector, V3d lengthVector) {
    for (int i = 8; --i >= 0;) {
      P3d corner = corners[i];
      corner.setT(pointCorner);
      if ((i & 1) != 0)
        corner.add(scaledWidthVector);
      if ((i & 2) != 0)
        corner.add(scaledHeightVector);
      if ((i & 4) != 0)
        corner.add(lengthVector);
    }
  }

  private void renderPart(int[][] planes) {
    if (rr.exportType == GData.EXPORT_CARTESIAN) {
      if (meshSurface == null) {
        meshSurface = new MeshSurface();
        meshSurface.vs = corners;
        meshSurface.haveQuads = true;
        meshSurface.vc = corners.length;
      }
      meshSurface.pis = planes;
      meshSurface.pc = planes.length;
      g3d.drawSurface(meshSurface, colix);
    } else {
      for (int i = 8; --i >= 0;)
        tm.transformPt3f(corners[i], screenCorners[i]);
      for (int i = planes.length; --i >= 0;) {
        int[] f = planes[i];
        if (f.length == 3)
          g3d.fillTriangle3f(screenCorners[f[0]], screenCorners[f[1]],
              screenCorners[f[2]], true);
        else
          g3d.fillQuadrilateral(screenCorners[f[0]], screenCorners[f[1]],
              screenCorners[f[2]], screenCorners[f[3]], true);
      }
    }
  }

  
}
