/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-11 14:30:16 -0500 (Sun, 11 Mar 2007) $
 * $Revision: 7068 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

import org.jmol.api.JmolRendererInterface;
import org.jmol.api.SymmetryInterface;
import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelsetbio.BasePair;
import org.jmol.modelsetbio.NucleicMonomer;
import org.jmol.script.T;
import org.jmol.util.C;
import org.jmol.util.GData;
import org.jmol.viewer.TransformManager;
import org.jmol.viewer.Viewer;

import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.T3;


/**
 * extends CartoonRenderer for nucleics
 * 
 */
public class NucleicRenderer {

  private boolean cartoonBaseEdges;
  
  private boolean cartoonBlocks;
  private float blockHeight;
    
    

  private boolean cartoonLadders;
  private boolean cartoonRibose;
 
  public NucleicRenderer() {
    // for reflection
  }
  
  //// nucleic acid base rendering
  
  private P3[] rPt, rPt5;
  private P3[] rScr, rScr5;
  private P3 basePt, backbonePt;
  private P3 baseScreen, backboneScreen;
  private P3 ptTemp;
  private Viewer vwr;
  private TransformManager tm;
  private JmolRendererInterface g3d;
  private BioShapeRenderer bsr;
  private short colix;

  private boolean cartoonSteps;
  
  void renderNucleic(BioShapeRenderer renderer) {
    if (this.vwr == null) {
      rPt = new P3[10];
      rScr = new P3[10];
      rPt5 = new P3[5];
      rScr5 = new P3[5];
      backboneScreen = new P3();
      backbonePt = new P3();
      bsr = renderer;
      tm = renderer.vwr.tm;
      vwr = renderer.vwr;
    }
    this.g3d = renderer.g3d;
    T3[] screens = renderer.controlPointScreens;
    T3[] pts = renderer.controlPoints;
    
    // order of checking for TRUE is:
    // Blocks, BaseEdges, Steps, Ladders, Ribose
    cartoonBlocks = vwr.getBoolean(T.cartoonblocks);
    cartoonBaseEdges = vwr.getBoolean(T.cartoonbaseedges);
    cartoonSteps = vwr.getBoolean(T.cartoonsteps);
    cartoonLadders = vwr.getBoolean(T.cartoonladders);
    cartoonRibose = vwr.getBoolean(T.cartoonribose);
    blockHeight = vwr.getFloat(T.cartoonblockheight);
    boolean isTraceAlpha = vwr.getBoolean(T.tracealpha);
    BS bsVisible = bsr.bsVisible;
    for (int i = bsVisible.nextSetBit(0); i >= 0; i = bsVisible
        .nextSetBit(i + 1)) {
      T3 scr = screens[i + 1];
      if (isTraceAlpha) {
        backboneScreen.ave(screens[i], scr);
        backbonePt.ave(pts[i], pts[i + 1]);
      } else {
        backboneScreen.setT(scr);
        backbonePt.setT(pts[i + 1]);
      }
      bsr.renderHermiteConic(i, false, 4);
      colix = bsr.getLeadColix(i);
      if (bsr.setBioColix(colix)) {
        if (cartoonRibose && bsVisible.get(i + 1))
          renderNucleicBaseStep(i, pts[i + 1],  screens[i + 1]);
        else
          renderNucleicBaseStep(i, null, null);
      }
    }
  }

  private void renderNucleicBaseStep(int im, T3 ptPnext, T3 scrPnext) {
    if (bsr.isPhosphorusOnly)
      return;
    NucleicMonomer nucleotide = (NucleicMonomer) bsr.monomers[im];
    short thisMad = bsr.mad = bsr.mads[im];
    if (rScr[0] == null) {
      for (int i = 10; --i >= 0;)
        rScr[i] = new P3();
      for (int i = 5; --i >= 0;)
        rScr5[i] = new P3();
      baseScreen = new P3();
      basePt = new P3();
      rPt[9] = new P3(); // ribose center
    }
    if (cartoonBlocks) {
      renderBlock(nucleotide);
      return;
    }
    if (cartoonBaseEdges) {
      renderLeontisWesthofEdges(nucleotide);
      return;
    }
    if (cartoonSteps) {
      renderSteps(nucleotide, im);
      return;
    }
    nucleotide.getBaseRing6Points(rPt);
    transformPoints(6, rPt, rScr);
    if (!cartoonLadders)
      renderRing6();
    P3 stepScreen;
    P3 stepPt;
    int pt;

    //private final static byte[] ring6OffsetIndexes = {C5, C6, N1, C2, N3, C4};
    //private final static byte[] ring5OffsetIndexes = {C5, N7, C8, N9, C4};
    //private final static byte[] riboseOffsetIndexes = {C1P, C2P, C3P, C4P, O4P, O3P, C5P, O5P};

    boolean hasRing5 = nucleotide.maybeGetBaseRing5Points(rPt5);
    if (hasRing5) {
      if (cartoonLadders) {
        stepScreen = rScr[2]; // N1
        stepPt = rPt[2];
      } else {
        transformPoints(5, rPt5, rScr5);
        renderRing5();
        stepScreen = rScr5[3]; // N9
        stepPt = rPt5[3];
      }
    } else {
      pt = (cartoonLadders ? 4 : 2);
      stepScreen = rScr[pt]; // N3 or N1
      stepPt = rPt[pt];
    }
    short mad = (short) (thisMad > 1 ? thisMad / 2 : thisMad);
    float r = mad / 2000f;
    int w = (int) vwr.tm.scaleToScreen((int) backboneScreen.z, mad);
    if (cartoonLadders || !cartoonRibose)
      g3d.fillCylinderScreen3I(GData.ENDCAPS_SPHERICAL, w, backboneScreen,
          stepScreen, backbonePt, stepPt, r);
    if (cartoonLadders)
      return;
    drawEdges(rScr, rPt, 6);
    if (hasRing5)
      drawEdges(rScr5, rPt5, 5);
    else
      renderEdge(rScr, rPt, 0, 5);
    if (cartoonRibose) {
      baseScreen.setT(stepScreen);
      basePt.setT(stepPt);
      nucleotide.getRiboseRing5Points(rPt);
      P3 c = rPt[9];
      c.set(0, 0, 0);
      for (int i = 0; i < 5; i++)
        c.add(rPt[i]);
      c.scale(0.2f);
      transformPoints(10, rPt, rScr);
      renderRibose();
      renderEdge(rScr, rPt, 2, 5); // C3' - O3'
      renderEdge(rScr, rPt, 3, 6); // C4' - C5' 
      renderEdge(rScr, rPt, 6, 7); // C5' - O5'
      renderEdge(rScr, rPt, 7, 8); // O5' - P'
      renderEdge(rScr, rPt, 0, 4); // C1' - O4'
      renderCyl(rScr[0], baseScreen, rPt[0], basePt); // C1' - N1 or N9
      if (ptPnext != null)
        renderCyl(rScr[5], (P3) scrPnext, rPt[5], (P3) ptPnext); // O3' - P(+1)
      drawEdges(rScr, rPt, 5);
    }
  }
  
  private void renderSteps(NucleicMonomer g, int i) {
      Lst<BasePair> bps = g.getBasePairs();
      Atom atomA = g.getLeadAtom();
      short cA = C.getColixInherited(colix, atomA.colixAtom);
      if (bps != null) {
        boolean checkPass2 = (!bsr.isExport && !vwr.gdata.isPass2);
        Atom[] atoms = vwr.ms.at;
        for (int j = bps.size(); --j >= 0;) {
          int iAtom = bps.get(j).getPartnerAtom(g);
          if (iAtom > i) {
            Atom atomB = atoms[iAtom];
            short cB = C.getColixInherited(colix, atomB.colixAtom);
            if (!checkPass2 || bsr.setBioColix(cA) || bsr.setBioColix(cB))
              bsr.drawSegmentAB(atomA, atomB, cA, cB, 1000);
          }
        }
    }  
  }

  private P3[] scrBox;
  private final int[] triangles = new int[] {
     1, 0, 3, 1, 3, 2, 
     0, 4, 7, 0, 7, 3,
     4, 5, 6, 4, 6, 7,
     5, 1, 2, 5, 2, 6,
     2, 3, 7, 2, 7, 6,
     0, 1, 5, 0, 5, 4
  };

  private void transformPoints(int count, T3[] angstroms, P3[] screens) {
    for (int i = count; --i >= 0;)
      tm.transformPtScrT3(angstroms[i], screens[i]);
  }

  private void drawEdges(P3[] scr, P3[] pt, int n) {
    for (int i = n; --i >= 0; )
      scr[i].z--;
    for (int i = n; --i > 0; )
      renderEdge(scr, pt, i, i - 1);
  }

  private void renderBlock(NucleicMonomer g) {
    Atom atomA = g.getLeadAtom();
    short cA = colix;
      if (scrBox == null) {
        scrBox = new P3[8];
        for (int j = 0; j < 8; j++)
          scrBox[j] = new P3();
      }
      P3[] oxyz = g.getDSSRFrame(vwr);
      P3[] box = g.dssrBox;
      float lastHeight = g.dssrBoxHeight;
      boolean isPurine = g.isPurine();
      if (box == null || lastHeight != blockHeight) {
        g.dssrBoxHeight = blockHeight;
        if (box == null) {
        box = new P3[8];
        for (int j = 8; --j >= 0;)
          box[j] = new P3();
        g.dssrBox = box;
        }
        SymmetryInterface uc = vwr.getSymTemp().getUnitCell(oxyz, false, null);
        if (ptTemp == null)
          ptTemp = new P3();
        ptTemp.setT(oxyz[0]);
        uc.toFractional(ptTemp, true);
        uc.setOffsetPt(P3.new3(ptTemp.x - 2.25f, ptTemp.y + 5f, ptTemp.z
            - blockHeight / 2));
        float x = 4.5f;
        float y = (isPurine ? -4.5f : -3f);
        float z = blockHeight;
        uc.toCartesian(box[0] = P3.new3(0, 0, 0), false);
        uc.toCartesian(box[1] = P3.new3(x, 0, 0), false);
        uc.toCartesian(box[2] = P3.new3(x, y, 0), false);
        uc.toCartesian(box[3] = P3.new3(0, y, 0), false);
        uc.toCartesian(box[4] = P3.new3(0, 0, z), false);
        uc.toCartesian(box[5] = P3.new3(x, 0, z), false);
        uc.toCartesian(box[6] = P3.new3(x, y, z), false);
        uc.toCartesian(box[7] = P3.new3(0, y, z), false);
      }
      for (int j = 0; j < 8; j++)
        vwr.tm.transformPt3f(box[j], scrBox[j]);      
      for (int j = 0; j < 36;)
        g3d.fillTriangle3f(scrBox[triangles[j++]], scrBox[triangles[j++]], scrBox[triangles[j++]], false);
      Atom atomB = g.getC1P();
      Atom atomC = g.getN0();
      if (atomB != null && atomC != null) {
        bsr.drawSegmentAB(atomA, atomB, cA, cA, 1000);
        bsr.drawSegmentAB(atomB, atomC, cA, cA, 1000);
      }
  }
  
  private void renderLeontisWesthofEdges(NucleicMonomer nucleotide) {
    //                Nasalean L, Strombaugh J, Zirbel CL, and Leontis NB in 
    //                Non-Protein Coding RNAs, 
    //                Nils G. Walter, Sarah A. Woodson, Robert T. Batey, Eds.
    //                Chapter 1, p 6.
    // http://books.google.com/books?hl=en&lr=&id=se5JVEqO11AC&oi=fnd&pg=PR11&dq=Non-Protein+Coding+RNAs&ots=3uTkn7m3DA&sig=6LzQREmSdSoZ6yNrQ15zjYREFNE#v=onepage&q&f=false

    if (!nucleotide.getEdgePoints(rPt))
      return;
    transformPoints(6, rPt, rScr);
    renderTriangle(rScr, rPt, 2, 3, 4, true);
    renderEdge(rScr, rPt, 0, 1);
    renderEdge(rScr, rPt, 1, 2);
    boolean isTranslucent = C.isColixTranslucent(colix);
    float tl = C.getColixTranslucencyLevel(colix);
    short colixSugarEdge = C.getColixTranslucent3(C.RED, isTranslucent,
        tl);
    short colixWatsonCrickEdge = C.getColixTranslucent3(C.GREEN,
        isTranslucent, tl);
    short colixHoogsteenEdge = C.getColixTranslucent3(C.BLUE,
        isTranslucent, tl);
    g3d.setC(colixSugarEdge);
    renderEdge(rScr, rPt, 2, 3);
    g3d.setC(colixWatsonCrickEdge);
    renderEdge(rScr, rPt, 3, 4);
    g3d.setC(colixHoogsteenEdge);
    renderEdge(rScr, rPt, 4, 5);
  }

  private void renderEdge(P3[] scr, P3[] pt, int i, int j) {
    renderCyl(scr[i], scr[j], pt[i], pt[j]);
  }

  private void renderCyl(P3 s1, P3 s2, P3 p1, P3 p2) {
    g3d.fillCylinderScreen3I(GData.ENDCAPS_SPHERICAL, 3, s1, s2, p1, p2, 0.005f);
  }

  /**
   * 
   * @param scr 
   * @param pt 
   * @param i 
   * @param j 
   * @param k 
   * @param doShade    if shade was not calculated previously;
   */
  private void renderTriangle(P3[] scr, P3[] pt, int i, int j, int k, boolean doShade) {
    g3d.fillTriangle3i(scr[i], scr[j], scr[k], pt[i], pt[j], pt[k], doShade);
  }

  private void renderRing6() {
    renderTriangle(rScr, rPt, 0, 2, 4, true);
    renderTriangle(rScr, rPt, 0, 1, 2, false);
    renderTriangle(rScr, rPt, 0, 4, 5, false);
    renderTriangle(rScr, rPt, 2, 3, 4, false);
  }

  private void renderRing5() {
    renderTriangle(rScr5, rPt5, 0, 1, 2, false);
    renderTriangle(rScr5, rPt5, 0, 2, 3, false);
    renderTriangle(rScr5, rPt5, 0, 3, 4, false);
  }  

  private void renderRibose() {
    renderTriangle(rScr, rPt, 0, 1, 9, true);
    renderTriangle(rScr, rPt, 1, 2, 9, true);
    renderTriangle(rScr, rPt, 2, 3, 9, true);
    renderTriangle(rScr, rPt, 3, 4, 9, true);
    renderTriangle(rScr, rPt, 4, 0, 9, true);
  }


}
