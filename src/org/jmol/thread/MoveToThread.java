/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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

package org.jmol.thread;

import javajs.util.A4;
import javajs.util.M3;
import javajs.util.P3;
import javajs.util.V3;
import org.jmol.viewer.TransformManager;
import org.jmol.viewer.Viewer;

public class MoveToThread extends JmolThread {

  /**
   * @j2sIgnoreSuperConstructor
   * 
   */
  public MoveToThread() {
    aaStepCenter = new V3();
    aaStepNavCenter = new V3();
    aaStep = new A4();
    aaTotal = new A4();
    matrixStart = new M3();
    matrixStartInv = new M3();
    matrixStep = new M3();
    matrixEnd = new M3();
  }

  private boolean isMove;

  ///// MOVETO command parameters:

  private final V3 aaStepCenter;
  private final V3 aaStepNavCenter;
  private final A4 aaStep;
  private final A4 aaTotal;
  private final M3 matrixStart;
  private final M3 matrixStartInv;
  private M3 matrixStep;
  private final M3 matrixEnd;

  private P3 center;
  private P3 navCenter;
  private P3 ptMoveToCenter;

  private Slider zoom;
  private Slider xTrans;
  private Slider yTrans;
  private Slider xNav;
  private Slider yNav;
  private Slider navDepth;
  private Slider cameraDepth;
  private Slider cameraX;
  private Slider cameraY;
  private Slider rotationRadius;
  private Slider pixelScale;

  private int fps;
  private long frameTimeMillis;
  private boolean doEndMove;
  private float fStep;

  ///// common to both:

  private TransformManager transformManager;
  private float floatSecondsTotal;
  private int totalSteps;
  private int iStep;

  ///// MOVE command uses a different set of parameters:

  private int timePerStep;
  private float radiansXStep;
  private float radiansYStep;
  private float radiansZStep;
  private V3 dRot;
  private V3 dTrans;
  private float dZoom;
  private float dSlab;
  private float zoomPercent0;
  private int slab;
  private float transX;
  private float transY;
  //private float transZ;

  @Override
  public int setManager(Object manager, Viewer vwr, Object params) {
    Object[] options = (Object[]) params;
    isMove = (options[0] instanceof V3);
    setViewer(vwr, (isMove ? "moveThread" : "MoveToThread"));
    transformManager = (TransformManager) manager;
    return (isMove ? setManagerMove(options) : setManagerMoveTo(options));
  }

  @Override
  protected void run1(int mode) throws InterruptedException {
    if (isMove)
      run1Move(mode);
    else
      run1MoveTo(mode);
  }
  
  @Override
  public void interrupt() {
    doEndMove = false;
    super.interrupt();
  }

  /**
   * MOVE is a much simpler command. 
   * 
   * @param options (see comment in code)
   * 
   * @return totalSteps
   */
  private int setManagerMove(Object[] options) {
    //  { dRot, dTrans,
    //    [ 
    //      dZoom,
    //      dSlab,
    //      floatSecondsTotal,
    //      fps
    //    ]
    //  }

    dRot = (V3) options[0];
    dTrans = (V3) options[1];
    float[] f = (float[]) options[2];
    dZoom = f[0];
    dSlab = f[1];
    floatSecondsTotal = f[2];
    int fps = (int) f[3];

    slab = transformManager.getSlabPercentSetting();
    transX = transformManager.getTranslationXPercent();
    transY = transformManager.getTranslationYPercent();
    //transZ = transformManager.getTranslationZPercent();

    timePerStep = 1000 / fps;
    totalSteps = (int) (fps * floatSecondsTotal);
    if (totalSteps <= 0)
      totalSteps = 1; // to catch a zero secondsTotal parameter
    float radiansPerDegreePerStep = (float) (1 / TransformManager.degreesPerRadian / totalSteps);
    radiansXStep = radiansPerDegreePerStep * dRot.x;
    radiansYStep = radiansPerDegreePerStep * dRot.y;
    radiansZStep = radiansPerDegreePerStep * dRot.z;
    zoomPercent0 = transformManager.zmPct;
    iStep = 0;
    return totalSteps;
  }

  /**
   * MOVETO is a more complex command. 
   * 
   * @param options (see comment in code)
   * 
   * @return totalSteps
   */
  private int setManagerMoveTo(Object[] options) {
    //  { center, matrixEnd, navCenter,
    //    { 
    //    0 floatSecondsTotal
    //    1 zoom,
    //    2 xTrans,
    //    3 yTrans,
    //    4 newRotationRadius, 
    //    5 pixelScale, 
    //    6 navDepth,
    //    7 xNav,
    //    8 yNav,
    //    9 cameraDepth,
    //   10 cameraX,
    //   11 cameraY 
    //   }
    // }
    center = (P3) options[0];
    matrixEnd.setM3((M3) options[1]);
    float[] f = (float[]) options[3];
    ptMoveToCenter = (center == null ? transformManager.fixedRotationCenter
        : center);
    floatSecondsTotal = f[0];
    zoom = newSlider(transformManager.zmPct, f[1]);
    xTrans = newSlider(transformManager.getTranslationXPercent(), f[2]);
    yTrans = newSlider(transformManager.getTranslationYPercent(), f[3]);
    rotationRadius = newSlider(transformManager.modelRadius,
        (center == null || Float.isNaN(f[4]) ? transformManager.modelRadius
            : f[4] <= 0 ? vwr.ms.calcRotationRadius(vwr.am.cmi, center, false) : f[4]));
    pixelScale = newSlider(transformManager.scaleDefaultPixelsPerAngstrom, f[5]);
    if (f[6] != 0) {
      navCenter = (P3) options[2];
      navDepth = newSlider(transformManager.navigationDepthPercent, f[6]);
      xNav = newSlider(transformManager.getNavigationOffsetPercent('X'), f[7]);
      yNav = newSlider(transformManager.getNavigationOffsetPercent('Y'), f[8]);
    }
    cameraDepth = newSlider(transformManager.getCameraDepth(), f[9]);
    cameraX = newSlider(transformManager.camera.x, f[10]);
    cameraY = newSlider(transformManager.camera.y, f[11]);
    transformManager.getRotation(matrixStart);
    matrixStartInv.invertM(matrixStart);
    matrixStep.mul2(matrixEnd, matrixStartInv);
    aaTotal.setM(matrixStep);
    fps = 30;
    totalSteps = (int) (floatSecondsTotal * fps);
    frameTimeMillis = 1000 / fps;
    targetTime = System.currentTimeMillis();
    aaStepCenter.sub2(ptMoveToCenter, transformManager.fixedRotationCenter);
    aaStepCenter.scale(1f / totalSteps);
    if (navCenter != null
        && transformManager.mode == TransformManager.MODE_NAVIGATION) {
      aaStepNavCenter.sub2(navCenter, transformManager.navigationCenter);
      aaStepNavCenter.scale(1f / totalSteps);
    }
    iStep = 0;
    return totalSteps;
  }

  private Slider newSlider(float start, float value) {
    return (Float.isNaN(value) || value == Float.MAX_VALUE ? null : new Slider(
        start, value));
  }

  private void run1Move(int mode) throws InterruptedException {
    while (true)
      switch (mode) {
      case INIT:
        if (floatSecondsTotal > 0)
          vwr.setInMotion(true);
        mode = MAIN;
        break;
      case MAIN:
        if (stopped || iStep >= totalSteps) {
          mode = FINISH;
          break;
        }
        iStep++;
        if (dRot.x != 0)
          transformManager.rotateXRadians(radiansXStep, null);
        if (dRot.y != 0)
          transformManager.rotateYRadians(radiansYStep, null);
        if (dRot.z != 0)
          transformManager.rotateZRadians(radiansZStep);
        if (dZoom != 0)
          transformManager.zoomToPercent(zoomPercent0 + dZoom * iStep
              / totalSteps);
        if (dTrans.x != 0)
          transformManager.translateToPercent('x', transX + dTrans.x * iStep
              / totalSteps);
        if (dTrans.y != 0)
          transformManager.translateToPercent('y', transY + dTrans.y * iStep
              / totalSteps);
        if (dTrans.z != 0)
          transformManager.translateToPercent('z', /*transZ + */dTrans.z
              * iStep / totalSteps);
        if (dSlab != 0)
          transformManager.slabToPercent((int) Math.floor(slab + dSlab * iStep
              / totalSteps));
        if (iStep == totalSteps) {
          mode = FINISH;
          break;
        }
        int timeSpent = (int) (System.currentTimeMillis() - startTime);
        int timeAllowed = iStep * timePerStep;
        if (timeSpent < timeAllowed) {
          vwr.requestRepaintAndWait("moveThread");
          if (!isJS && !vwr.isScriptExecuting()) {
            mode = FINISH;
            break;
          }
          timeSpent = (int) (System.currentTimeMillis() - startTime);
          sleepTime = timeAllowed - timeSpent;
          if (!runSleep(sleepTime, MAIN))
            return;
        }
        break;
      case FINISH:
        if (floatSecondsTotal > 0)
          vwr.setInMotion(false);
        resumeEval();
        return;
      }
  }

  private void run1MoveTo(int mode) throws InterruptedException {
    while (true)
      switch (mode) {
      case INIT:
        if (totalSteps > 0)
          vwr.setInMotion(true);
        mode = MAIN;
        break;
      case MAIN:
        if (stopped || ++iStep >= totalSteps) {
          mode = FINISH;
          break;
        }
        doStepTransform();
        doEndMove = true;
        targetTime += frameTimeMillis;
        currentTime = System.currentTimeMillis();
        boolean doRender = (currentTime < targetTime);
        if (!doRender && isJS) {
          // JavaScript will be slow anyway -- make sure we render
          targetTime = currentTime;
          doRender = true;
        }
        if (doRender)
          vwr.requestRepaintAndWait("movetoThread");
        if (transformManager.movetoThread == null
            || !transformManager.movetoThread.name.equals(name) || !isJS
            && eval != null && !vwr.isScriptExecuting()) {
          stopped = true;
          break;
        }
        currentTime = System.currentTimeMillis();
        int sleepTime = (int) (targetTime - currentTime);
        if (!runSleep(sleepTime, MAIN))
          return;
        mode = MAIN;
        break;
      case FINISH:
        if (totalSteps <= 0 || doEndMove && !stopped)
          doFinalTransform();
        if (totalSteps > 0)
          vwr.setInMotion(false);
        vwr.moveUpdate(floatSecondsTotal);
        if (transformManager.movetoThread != null && !stopped) {
          transformManager.movetoThread = null;
          vwr.finalizeTransformParameters();
        }
        resumeEval();
        return;
      }
  }

  private void doStepTransform() {
    if (!Float.isNaN(matrixEnd.m00)) {
      transformManager.getRotation(matrixStart);
      matrixStartInv.invertM(matrixStart);
      matrixStep.mul2(matrixEnd, matrixStartInv);
      aaTotal.setM(matrixStep);
      aaStep.setAA(aaTotal);
      aaStep.angle /= (totalSteps - iStep);
      if (aaStep.angle == 0)
        matrixStep.setScale(1);
      else
        matrixStep.setAA(aaStep);
      matrixStep.mul(matrixStart);
    }
    fStep = iStep / (totalSteps - 1f);
    if (center != null)
      transformManager.fixedRotationCenter.add(aaStepCenter);
    if (navCenter != null
        && transformManager.mode == TransformManager.MODE_NAVIGATION) {
      P3 pt = P3.newP(transformManager.navigationCenter);
      pt.add(aaStepNavCenter);
      transformManager.setNavigatePt(pt);
    }
    setValues(matrixStep, null, null);
  }

  private void doFinalTransform() {
    fStep = -1;
    setValues(matrixEnd, center, navCenter);
  }

  private void setValues(M3 m, P3 center, P3 navCenter) {
    transformManager.setAll(center, m, navCenter, getVal(zoom), getVal(xTrans),
        getVal(yTrans), getVal(rotationRadius), getVal(pixelScale),
        getVal(navDepth), getVal(xNav), getVal(yNav), getVal(cameraDepth),
        getVal(cameraX), getVal(cameraY));
  }

  private float getVal(Slider s) {
    return (s == null ? Float.NaN : s.getVal(fStep));
  }

  private class Slider {
    float start;
    float delta;
    float value;

    Slider(float start, float value) {
      this.start = start;
      this.value = value;
      this.delta = value - start;
    }

    float getVal(float fStep) {
      return (fStep < 0 ? value : start + fStep * delta);
    }

  }

}
