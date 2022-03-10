/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-10-02 12:35:03 -0500 (Sun, 02 Oct 2016) $
 * $Revision: 21252 $
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
import javajs.util.P3i;
import javajs.util.V3;

import org.jmol.api.JmolModulationSet;
import org.jmol.modelset.Atom;
import org.jmol.render.ShapeRenderer;
import org.jmol.script.T;
import org.jmol.shape.Shape;
import org.jmol.shapespecial.Vectors;
import org.jmol.util.GData;
import org.jmol.util.Point3fi;
import org.jmol.util.Vibration;

public class VectorsRenderer extends ShapeRenderer {

  private final static float arrowHeadOffset = -0.2f;
  private final Point3fi ptTemp = new Point3fi();
  private final P3 pointVectorStart = new P3();
  private final Point3fi pointVectorEnd = new Point3fi();
  private final P3 pointArrowHead = new P3();
  private final P3 screenVectorStart = new P3();
  private final P3 screenVectorEnd = new P3();
  private final P3 screenArrowHead = new P3();
  private final V3 headOffsetVector = new V3();
  private final P3 pTemp3 = new P3();
  
  
  private int diameter;
  //float headWidthAngstroms;
  private int headWidthPixels;
  private float vectorScale;
  private boolean vectorSymmetry;
  private float headScale;
  private boolean drawShaft;
  private Vibration vibTemp;
  private boolean vectorsCentered;
  private boolean standardVector = true;
  private boolean vibrationOn;
  private boolean drawCap;
  private boolean showModVecs;
  private int vectorTrail;
  private P3 ptTemp4;
  private P3 ptTemp2;


  @Override
  protected boolean render() {
    Vectors vectors = (Vectors) shape;
    if (!vectors.isActive)
      return false;
    short[] mads = vectors.mads;
    if (mads == null)
      return false;
    short[] colixes = vectors.colixes;
    boolean needTranslucent = false;
    vectorScale = vwr.getFloat(T.vectorscale);
    vectorTrail = vwr.getInt(T.vectortrail);
    if (vectorScale < 0)
      vectorScale = 1;
    vectorSymmetry = vwr.getBoolean(T.vectorsymmetry);
    vectorsCentered = vwr.getBoolean(T.vectorscentered);
    showModVecs = vwr.getBoolean(T.showmodvecs);
    vibrationOn = vwr.tm.vibrationOn;
    headScale = arrowHeadOffset;
    if (vectorScale < 0)
      headScale = -headScale;
    boolean haveModulations = false;
    Atom[] atoms = ms.at;
    for (int i = ms.ac; --i >= 0;) {
      Atom atom = atoms[i];
      if (!isVisibleForMe(atom))
        continue;
      JmolModulationSet mod = ms.getModulation(i);
      if (showModVecs && !haveModulations && mod != null)
        haveModulations = true;
      Vibration vib = ms.getVibration(i, false);
      if (vib == null)
        continue;
      // just the vibration, but if it is a spin, it might be modulated
      // issue here is that the "vibration" for an atom may be one of:
      // standard vibration
      // magnetic spin
      // displacement modulation
      // modulated magnetic spin
      // magnetic spin and displacement modulation
      // modulated magnetic spin and displacement modulation
      if (!transform(mads[i], atom, vib, mod))
        continue;
      if (!g3d.setC(Shape.getColix(colixes, i, atom))) {
        needTranslucent = true;
        continue;
      }
      renderVector(atom, vib);
      if (vectorSymmetry) {
        if (vibTemp == null)
          vibTemp = new Vibration();
        vibTemp.setT(vib);
        vibTemp.scale(-1);
        transform(mads[i], atom, vibTemp, null);
        renderVector(atom, vib);
      }
    }
    if (haveModulations)
      for (int i = ms.ac; --i >= 0;) {
        Atom atom = atoms[i];
        if (!isVisibleForMe(atom))
          continue;
        JmolModulationSet mod = ms.getModulation(i);
        if (mod == null)
          continue;
        if (!g3d.setC(Shape.getColix(colixes, i, atom))) {
          needTranslucent = true;
          continue;
        }
        // now we focus on modulations 
        // this may involve a modulated atom or a spin modulation
        if (!transform(mads[i], atom, null, mod))
          continue;
        renderVector(atom, null);
      }

    return needTranslucent;
  }

  private boolean transform(short mad, Atom atom, Vibration vib,
                            JmolModulationSet mod2) {
    boolean isMod = (vib == null || vib.modDim >= 0);
    boolean isSpin = (!isMod && vib.modDim == Vibration.TYPE_SPIN);
    if (vib == null)
      vib = (Vibration) mod2;
    drawCap = true;
    if (!isMod) {
      float len = vib.length();
      // to have the vectors move when vibration is turned on
      if (Math.abs(len * vectorScale) < 0.01)
        return false;
      standardVector = true;
      drawShaft = (0.1 + Math.abs(headScale / len) < Math.abs(vectorScale));
      headOffsetVector.setT(vib);
      headOffsetVector.scale(headScale / len);
    }
    ptTemp.setT(atom);
    JmolModulationSet mod = atom.getModulation();
    if (vibrationOn && mod != null)
      vwr.tm.getVibrationPoint((Vibration) mod, ptTemp, 1);
    if (isMod) {
      standardVector = false;
      drawShaft = true;
      mod = (JmolModulationSet) vib;
      pointVectorStart.setT(ptTemp);
      pointVectorEnd.setT(ptTemp);
      if (mod.isEnabled()) {
        if (vibrationOn) {
          vwr.tm.getVibrationPoint(vib, pointVectorEnd, Float.NaN);
        }
        mod.addTo(pointVectorStart, Float.NaN);
      } else {
        mod.addTo(pointVectorEnd, 1);
      }
      headOffsetVector.sub2(pointVectorEnd, pointVectorStart);
      float len = headOffsetVector.length();
      drawCap = (len + arrowHeadOffset > 0.001f);
      drawShaft = (len > 0.01f);
      headOffsetVector.scale(headScale / headOffsetVector.length());
    } else if (vectorsCentered || isSpin) {
      standardVector = false;
      //     Vibration v;
      //     if (mod2 == null || !mod2.isEnabled()) {
      //       v = vib; 
      //      } else {
      //        v = vibTemp;
      //        vibTemp.set(0,  0,  0);
      //        v.setTempPoint(vibTemp, null, 1, vwr.g.modulationScale);
      //        vwr.tm.getVibrationPoint(vib, v, Float.NaN);
      //      }
      pointVectorEnd.scaleAdd2(0.5f * vectorScale, vib, ptTemp);
      pointVectorStart.scaleAdd2(-0.5f * vectorScale, vib, ptTemp);
    } else {
      pointVectorEnd.scaleAdd2(vectorScale, vib, ptTemp);
      pointArrowHead.add2(pointVectorEnd, headOffsetVector);
      if (vibrationOn) {
        P3i screen = tm.transformPtVib(pointVectorEnd, vib);
        screenVectorEnd.set(screen.x, screen.y, screen.z);
        screen = tm.transformPtVib(pointArrowHead, vib);
        screenArrowHead.set(screen.x, screen.y, screen.z);
      } else {
        tm.transformPtScrT3(pointVectorEnd, screenVectorEnd);
        tm.transformPtScrT3(pointArrowHead, screenArrowHead);
      }
    }

    if (!standardVector) {
      tm.transformPtScrT3(pointVectorEnd, screenVectorEnd);
      tm.transformPtScrT3(pointVectorStart, screenVectorStart);
      if (drawCap)
        pointArrowHead.add2(pointVectorEnd, headOffsetVector);
      else
        pointArrowHead.setT(pointVectorEnd);
      tm.transformPtScrT3(pointArrowHead, screenArrowHead);
    }
    diameter = (int) (mad < 0 ? -mad : mad < 1 ? 1 : vwr.tm.scaleToScreen(
        (int) screenVectorEnd.z, mad));
    headWidthPixels = diameter << 1;
    if (headWidthPixels < diameter + 2)
      headWidthPixels = diameter + 2;
    
    return true;
  }
  
  private void renderVector(Atom atom, Vibration vib) {
    if (vib != null && vectorTrail > 0) {// show trace
      if (ptTemp4 == null) {
        ptTemp4 = new P3();
        ptTemp2 = new P3();
      }
      int d = Math.max(1, diameter >> 2);
      P3[] pts = vib.addTracePt(vectorTrail, vibrationOn ? pointVectorEnd : null);
      tm.transformPtScrT3(atom, ptTemp4);
      if (pts != null)
        for (int i = pts.length, p = vectorTrail; --i >= 0;) {
          P3 pt = pts[--p];
          if (pt == null)
            break;
          tm.transformPtScrT3(pt, ptTemp2);
          g3d.fillCylinderBits(GData.ENDCAPS_FLAT, d, ptTemp4, ptTemp2);
        }
    }

    if (drawShaft) {
      pTemp3.set(atom.sX, atom.sY, atom.sZ);
      if (standardVector)
        g3d.fillCylinderBits(GData.ENDCAPS_FLAT, diameter, pTemp3, screenArrowHead);
      else 
        g3d.fillCylinderBits(GData.ENDCAPS_FLAT, diameter, screenVectorStart, screenArrowHead);
    }
    if (drawCap)
      g3d.fillConeScreen3f(GData.ENDCAPS_FLAT, headWidthPixels, screenArrowHead,
          screenVectorEnd, false);
  }
}
