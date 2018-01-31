/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2006  The Jmol Development Team
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

package org.jmol.navigate;



import org.jmol.api.JmolNavigatorInterface;
import org.jmol.api.JmolScriptEvaluator;
import org.jmol.script.T;
import org.jmol.thread.JmolThread;
import org.jmol.util.Escape;
import org.jmol.util.GData;

import javajs.awt.event.Event;
import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.P3;
import javajs.util.T3;
import javajs.util.V3;
import org.jmol.viewer.JC;
import org.jmol.viewer.TransformManager;
import org.jmol.viewer.Viewer;

/**
 * Navigator is a user input mechanism that utilizes the keypad to drive through
 * the model.
 * 
 * It is created by reflection only from org.jmol.viewer.TransformManager
 * 
 * 
 */
public final class Navigator extends JmolThread implements
    JmolNavigatorInterface {

  public Navigator() {
    super();
    // for Reflection from TransformManager.java
  }

  @Override
  public void set(TransformManager tm, Viewer vwr) {
    this.tm = tm;
    setViewer(vwr, "navigator");
  }

  private TransformManager tm;

  private int nHits;
  private int multiplier = 1;
  private boolean isPathGuide;
  private P3[] points;
  private P3[] pointGuides;
  private int frameTimeMillis;
  private float floatSecondsTotal;
  private V3 axis;
  private float degrees;
  private P3 center;
  private float depthPercent;
  private float xTrans;
  private float yTrans;
  private float depthStart;
  private float depthDelta;
  private float xTransStart;
  private float xTransDelta;
  private float yTransStart;
  private float yTransDelta;
  private float degreeStep;
  private P3 centerStart;
  private int totalSteps;
  private V3 aaStepCenter;
  private boolean isNavTo;

  private int iStep;

  private Lst<Object[]> navigationList;

  private int iList;

  private boolean isStep;

  @Override
  public void navigateList(JmolScriptEvaluator eval, Lst<Object[]> list) {
    // needs testing in Jmol/Java
    // still not correct for JS
    setEval(eval);
    navigationList = list;
    iList = 0;
    isStep = false;
    run();
  }

  private void nextList(int i, P3 ptTemp) {
    Object[] o = navigationList.get(i);
    float seconds = ((Float) o[1]).floatValue();
    int tok = ((Integer) o[0]).intValue();
    switch (tok) {
    case T.point:
      P3 pt = (P3) o[2];
      if (seconds == 0) {
        tm.setNavigatePt(pt);
        vwr.moveUpdate(0);
        return;
      }
      navigateTo(seconds, null, Float.NaN, pt, Float.NaN, Float.NaN, Float.NaN);
      break;
    case T.path:
      P3[] path = (P3[]) o[2];
      float[] theta = (float[]) o[3];
      int indexStart = ((int[]) o[4])[0];
      int indexEnd = ((int[]) o[4])[1];
      navigate(seconds, null, path, theta, indexStart, indexEnd);
      break;
    case T.trace:
      /*
       * follows a path guided by orientation and offset vectors (as Point3fs)
       */
      P3[][] pathGuide = ((P3[][]) o[2]);
      navigate(seconds, pathGuide, null, null, 0, Integer.MAX_VALUE);
      break;
    case T.rotate:
      V3 rotAxis = (V3) o[2];
      float degrees = ((Float) o[3]).floatValue();
      if (seconds == 0) {
        navigateAxis(rotAxis, degrees);
        vwr.moveUpdate(0);
        return;
      }
      navigateTo(seconds, rotAxis, degrees, null, Float.NaN, Float.NaN,
          Float.NaN);
      break;
    case T.translate:
    case T.percent: 
      if (tok == T.translate) {
        tm.transformPt3f((P3) o[2], ptTemp);        
      } else {
        ptTemp.x = ((Float) o[2]).floatValue();
        ptTemp.y = ((Float) o[3]).floatValue();
        setNavPercent(ptTemp);
      }
      if (seconds == 0) {
        navTranslatePercentOrTo(-1, ptTemp.x, ptTemp.y);
        vwr.moveUpdate(0);
        return;
      }
      navigateTo(seconds, null, Float.NaN, null, Float.NaN, ptTemp.x, ptTemp.y);
      break;
    case T.depth:
      float percent = ((Float) o[2]).floatValue();
      navigateTo(seconds, null, Float.NaN, null, percent, Float.NaN, Float.NaN);
      break;
    }
  }

  private void setNavPercent(P3 pt1) {
    tm.transformPt3f(tm.navigationCenter, tm.navigationOffset);
    float x = pt1.x;
    float y = pt1.y;
    if (!Float.isNaN(x))
      x = tm.width * x / 100f
          + (Float.isNaN(y) ? tm.navigationOffset.x : (tm.width / 2f));
    if (!Float.isNaN(y))
      y = tm.height * y / 100f
          + (Float.isNaN(x) ? tm.navigationOffset.y : tm.getNavPtHeight());
    pt1.x = x;
    pt1.y = y;
  }

  @Override
  public void navigateTo(float seconds, V3 axis, float degrees,
                         P3 center, float depthPercent, float xTrans,
                         float yTrans) {
    /*
     * Orientation o = vwr.getOrientation(); if (!Float.isNaN(degrees) &&
     * degrees != 0) navigate(0, axis, degrees); if (center != null) {
     * navigate(0, center); } if (!Float.isNaN(xTrans) || !Float.isNaN(yTrans))
     * navTranslatePercent(-1, xTrans, yTrans); if (!Float.isNaN(depthPercent))
     * setNavigationDepthPercent(depthPercent); Orientation o1 =
     * vwr.getOrientation(); o.restore(0, true);
     * o1.restore(floatSecondsTotal, true);
     */

    floatSecondsTotal = seconds;
    this.axis = axis;
    this.degrees = degrees;
    this.center = center;
    this.depthPercent = depthPercent;
    this.xTrans = xTrans;
    this.yTrans = yTrans;
    setupNavTo();
    isStep = true;
    run();
  }

  @Override
  public void navigate(float seconds, P3[][] pathGuide, P3[] path,
                       float[] theta, int indexStart, int indexEnd) {
    //this.theta = theta;
    floatSecondsTotal = seconds;
    setupNav(seconds, pathGuide, path, indexStart, indexEnd);
    isStep = true;
    run();
  }

  @Override
  protected void run1(int mode) throws InterruptedException {
    P3 ptTemp = new P3();
    while (isJS || vwr.isScriptExecuting())
      switch (mode) {
      case INIT:
        if (isStep) {
          targetTime = startTime;
          iStep = 0;
          mode = (totalSteps <= 0 && isNavTo ? CHECK1 : MAIN);
          break;
        }
        mode = CHECK2;
        break;
      case CHECK2:
        nextList(iList, ptTemp);
        return;
      case MAIN:
        if (stopped || iStep >= totalSteps) {
          mode = FINISH;
          break;
        }
        doNavStep(iStep++);
        vwr.requestRepaintAndWait("navigatorThread");
        int sleepTime = (int) (targetTime - System.currentTimeMillis());
        if (!runSleep(sleepTime, MAIN))
          return;
        mode = MAIN;
        break;
      case CHECK1:
        if (!runSleep((int) (floatSecondsTotal * 1000) - 30, FINISH))
          return;
        mode = FINISH;
        break;
      case FINISH:
        if (isNavTo) {
          // if (center != null)
          // navigate(0, center);
          if (!Float.isNaN(xTrans) || !Float.isNaN(yTrans))
            navTranslatePercentOrTo(-1, xTrans, yTrans);
          if (!Float.isNaN(depthPercent))
            setNavigationDepthPercent(depthPercent);
        }
        vwr.setInMotion(false);
        vwr.moveUpdate(floatSecondsTotal);
        if (!stopped && ++iList < navigationList.size()) {
          mode = CHECK2;
          break;
        }
        resumeEval();
        return;
      }
  }

  private void doNavStep(int iStep) {
    if (!isNavTo) {
      tm.setNavigatePt(points[iStep]);
      if (isPathGuide) {
        alignZX(points[iStep], points[iStep + 1], pointGuides[iStep]);
      }
      targetTime += frameTimeMillis;
      return;
    }
    tm.navigating = true;
    float fStep = (iStep + 1f) / totalSteps;
    if (!Float.isNaN(degrees))
      tm.navigateAxis(this.axis, degreeStep);
    if (center != null) {
      centerStart.add(aaStepCenter);
      tm.setNavigatePt(centerStart);
    }
    if (!Float.isNaN(xTrans) || !Float.isNaN(yTrans)) {
      float x = Float.NaN;
      float y = Float.NaN;
      if (!Float.isNaN(xTrans))
        x = xTransStart + xTransDelta * fStep;
      if (!Float.isNaN(yTrans))
        y = yTransStart + yTransDelta * fStep;
      navTranslatePercentOrTo(-1, x, y);
    }

    if (!Float.isNaN(depthPercent)) {
      setNavigationDepthPercent(depthStart + depthDelta * fStep);
    }
    tm.navigating = false;
    targetTime += frameTimeMillis;
  }

  private void setupNavTo() {
    isNavTo = true;
    if (!vwr.haveDisplay)
      floatSecondsTotal = 0;
    int fps = 30;
    totalSteps = (int) (floatSecondsTotal * fps) - 1;
    if (floatSecondsTotal > 0)
      vwr.setInMotion(true);
    if (degrees == 0)
      degrees = Float.NaN;
    if (totalSteps > 0) {
      frameTimeMillis = 1000 / fps;
      depthStart = tm.navigationDepthPercent;
      depthDelta = depthPercent - depthStart;
      xTransStart = tm.navigationOffset.x;
      xTransDelta = xTrans - xTransStart;
      yTransStart = tm.navigationOffset.y;
      yTransDelta = yTrans - yTransStart;
      degreeStep = degrees / (totalSteps + 1);
      aaStepCenter = V3.newVsub(center == null ? tm.navigationCenter : center,
          tm.navigationCenter);
      aaStepCenter.scale(1f / (totalSteps + 1));
      centerStart = P3.newP(tm.navigationCenter);
    }
  }

  private void setupNav(float seconds, P3[][] pathGuide, P3[] path,
                        int indexStart, int indexEnd) {
    isNavTo = false;
    if (seconds <= 0) // PER station
      seconds = 2;
    if (!vwr.haveDisplay)
      seconds = 0;
    isPathGuide = (pathGuide != null);
    int nSegments = Math.min(
        (isPathGuide ? pathGuide.length : path.length) - 1, indexEnd);
    if (!isPathGuide)
      while (nSegments > 0 && path[nSegments] == null)
        nSegments--;
    nSegments -= indexStart;
    if (nSegments < 1)
      return;
    int nPer = (int) Math.floor(10 * seconds); // ?
    int nSteps = nSegments * nPer + 1;
    points = new P3[nSteps + 2];
    pointGuides = new P3[isPathGuide ? nSteps + 2 : 0];
    for (int i = 0; i < nSegments; i++) {
      int iPrev = Math.max(i - 1, 0) + indexStart;
      int pt = i + indexStart;
      int iNext = Math.min(i + 1, nSegments) + indexStart;
      int iNext2 = Math.min(i + 2, nSegments) + indexStart;
      int iNext3 = Math.min(i + 3, nSegments) + indexStart;
      if (isPathGuide) {
        GData.getHermiteList(7, pathGuide[iPrev][0], pathGuide[pt][0],
            pathGuide[iNext][0], pathGuide[iNext2][0], pathGuide[iNext3][0],
            points, i * nPer, nPer + 1, true);
        GData.getHermiteList(7, pathGuide[iPrev][1], pathGuide[pt][1],
            pathGuide[iNext][1], pathGuide[iNext2][1], pathGuide[iNext3][1],
            pointGuides, i * nPer, nPer + 1, true);
      } else {
        GData.getHermiteList(7, path[iPrev], path[pt], path[iNext],
            path[iNext2], path[iNext3], points, i * nPer, nPer + 1, true);
      }
    }
    vwr.setInMotion(true);
    frameTimeMillis = (int) (1000 / tm.navFps);
    totalSteps = nSteps;
  }

  /**
   * brings pt0-pt1 vector to [0 0 -1], then rotates about [0 0 1] until
   * ptVectorWing is in xz plane
   * 
   * @param pt0
   * @param pt1
   * @param ptVectorWing
   */
  private void alignZX(P3 pt0, P3 pt1, P3 ptVectorWing) {
    P3 pt0s = new P3();
    P3 pt1s = new P3();
    M3 m = tm.matrixRotate;
    m.rotate2(pt0, pt0s);
    m.rotate2(pt1, pt1s);
    V3 vPath = V3.newVsub(pt0s, pt1s);
    V3 v = V3.new3(0, 0, 1);
    float angle = vPath.angle(v);
    v.cross(vPath, v);
    if (angle != 0)
      tm.navigateAxis(v, (float) (angle * TransformManager.degreesPerRadian));
    m.rotate2(pt0, pt0s);
    P3 pt2 = P3.newP(ptVectorWing);
    pt2.add(pt0);
    P3 pt2s = new P3();
    m.rotate2(pt2, pt2s);
    vPath.sub2(pt2s, pt0s);
    vPath.z = 0; // just use projection
    v.set(-1, 0, 0); // puts alpha helix sidechain above
    angle = vPath.angle(v);
    if (vPath.y < 0)
      angle = -angle;
    v.set(0, 0, 1);
    if (angle != 0)
      tm.navigateAxis(v, (float) (angle * TransformManager.degreesPerRadian));
    //    if (vwr.getNavigateSurface()) {
    //      // set downward viewpoint 20 degrees to horizon
    //      v.set(1, 0, 0);
    //      tm.navigateAxis(v, 20);
    //    }
    m.rotate2(pt0, pt0s);
    m.rotate2(pt1, pt1s);
    m.rotate2(ptVectorWing, pt2s);
  }

  @Override
  public void zoomByFactor(float factor, int x, int y) {
    float navZ = tm.navZ;
    if (navZ > 0) {
      navZ /= factor;
      if (navZ < 5)
        navZ = -5;
      else if (navZ > 200)
        navZ = 200;
    } else if (navZ == 0) {
      navZ = (factor < 1 ? 5 : -5);
    } else {
      navZ *= factor;
      if (navZ > -5)
        navZ = 5;
      else if (navZ < -200)
        navZ = -200;
    }
    tm.navZ = navZ;

    /*    float range = visualRange / factor;
        System.out.println(navZ);
        
        if (vwr.getNavigationPeriodic())
          range = Math.min(range, 0.8f * modelRadius);      
        visualRange = range;  
    */
  }

  @Override
  public void calcNavigationPoint() {
    // called by finalize
    calcNavigationDepthPercent();
    if (!tm.navigating && tm.navMode != TransformManager.NAV_MODE_RESET) {
      // rotations are different from zoom changes
      if (tm.navigationDepthPercent < 100 && tm.navigationDepthPercent > 0
          && !Float.isNaN(tm.previousX)
          && tm.previousX == tm.fixedTranslation.x
          && tm.previousY == tm.fixedTranslation.y
          && tm.navMode != TransformManager.NAV_MODE_ZOOMED)
        tm.navMode = TransformManager.NAV_MODE_NEWXYZ;
      else
        tm.navMode = TransformManager.NAV_MODE_NONE;
    }
    switch (tm.navMode) {
    case TransformManager.NAV_MODE_RESET:
      // simply place the navigation center front and center and recalculate
      // modelCenterOffset
      tm.navigationOffset.set(tm.width / 2f, tm.getNavPtHeight(),
          tm.referencePlaneOffset);
      tm.zoomFactor = Float.MAX_VALUE;
      tm.calcCameraFactors();
      tm.calcTransformMatrix();
      newNavigationCenter();
      break;
    case TransformManager.NAV_MODE_NONE:
    case TransformManager.NAV_MODE_ZOOMED:
      // update fixed rotation offset and find the new 3D navigation center
      tm.fixedRotationOffset.setT(tm.fixedTranslation);
      newNavigationCenter();
      break;
    case TransformManager.NAV_MODE_NEWXY:
      // redefine the navigation center based on its old screen position
      newNavigationCenter();
      break;
    case TransformManager.NAV_MODE_IGNORE:
    case TransformManager.NAV_MODE_NEWXYZ:
      // must just be (not so!) simple navigation
      // navigation center will initially move
      // but we center it by moving the rotation center instead
      T3 pt1 = tm.matrixTransform.rotTrans2(tm.navigationCenter, new P3());
      float z = pt1.z;
      tm.matrixTransform.rotTrans2(tm.fixedRotationCenter, pt1);
      tm.modelCenterOffset = tm.referencePlaneOffset + (pt1.z - z);
      tm.calcCameraFactors();
      tm.calcTransformMatrix();
      break;
    case TransformManager.NAV_MODE_NEWZ:
      // just untransform the offset to get the new 3D navigation center
      tm.navigationOffset.z = tm.referencePlaneOffset;
      //System.out.println("nav_mode_newz " + navigationOffset);
      tm.unTransformPoint(tm.navigationOffset, tm.navigationCenter);
      break;
    }
    tm.matrixTransform.rotTrans2(tm.navigationCenter, tm.navigationShiftXY);
    if (vwr.getBoolean(T.navigationperiodic)) {
      // TODO
      // but if periodic, then the navigationCenter may have to be moved back a
      // notch
      P3 pt = P3.newP(tm.navigationCenter);
      vwr.toUnitCell(tm.navigationCenter, null);
      // presuming here that pointT is still a molecular point??
      if (pt.distance(tm.navigationCenter) > 0.01) {
        tm.matrixTransform.rotTrans2(tm.navigationCenter, pt);
        float dz = tm.navigationShiftXY.z - pt.z;
        // the new navigation center determines the navigationZOffset
        tm.modelCenterOffset += dz;
        tm.calcCameraFactors();
        tm.calcTransformMatrix();
        tm.matrixTransform
            .rotTrans2(tm.navigationCenter, tm.navigationShiftXY);
      }
    }
    tm.transformPt3f(tm.fixedRotationCenter, tm.fixedTranslation);
    tm.fixedRotationOffset.setT(tm.fixedTranslation);
    tm.previousX = tm.fixedTranslation.x;
    tm.previousY = tm.fixedTranslation.y;
    tm.transformPt3f(tm.navigationCenter, tm.navigationOffset);
    tm.navigationOffset.z = tm.referencePlaneOffset;
    tm.navMode = TransformManager.NAV_MODE_NONE;
    calcNavSlabAndDepthValues();
  }

  private void calcNavSlabAndDepthValues() {
    tm.calcSlabAndDepthValues();
    if (tm.slabEnabled) {
      tm.slabValue = (tm.mode == TransformManager.MODE_NAVIGATION ? -100 : 0)
          + (int) (tm.referencePlaneOffset - tm.navigationSlabOffset);
      if (tm.zSlabPercentSetting == tm.zDepthPercentSetting)
        tm.zSlabValue = tm.slabValue;
    }

    //    if (Logger.debugging)
    //      Logger.debug("\n" + "\nperspectiveScale: " + referencePlaneOffset
    //          + " screenPixelCount: " + screenPixelCount + "\nmodelTrailingEdge: "
    //          + (modelCenterOffset + modelRadiusPixels) + " depthValue: "
    //          + depthValue + "\nmodelCenterOffset: " + modelCenterOffset
    //          + " modelRadiusPixels: " + modelRadiusPixels + "\nmodelLeadingEdge: "
    //          + (modelCenterOffset - modelRadiusPixels) + " slabValue: "
    //          + slabValue + "\nzoom: " + zoomPercent + " navDepth: "
    //          + ((int) (100 * getNavigationDepthPercent()) / 100f)
    //          + " visualRange: " + visualRange + "\nnavX/Y/Z/modelCenterOffset: "
    //          + navigationOffset.x + "/" + navigationOffset.y + "/"
    //          + navigationOffset.z + "/" + modelCenterOffset + " navCenter:"
    //          + navigationCenter);
  }

  /**
   * We do not want the fixed navigation offset to change, but we need a new
   * model-based equivalent position. The fixed rotation center is at a fixed
   * offset as well. This means that the navigationCenter must be recalculated
   * based on its former offset in the new context. We have two points,
   * N(navigation) and R(rotation). We know where they ARE:
   * fixedNavigationOffset and fixedRotationOffset. From these we must derive
   * navigationCenter.
   */
  private void newNavigationCenter() {

    // Point3f fixedRotationCenter, Point3f navigationOffset,

    // Point3f navigationCenter) {

    // fixedRotationCenter, navigationOffset, navigationCenter
    tm.mode = tm.defaultMode;
    // get the rotation center's Z offset and move X and Y to 0,0
    P3 pt = new P3();
    tm.transformPt3f(tm.fixedRotationCenter, pt);
    pt.x -= tm.navigationOffset.x;
    pt.y -= tm.navigationOffset.y;
    // unapply the perspective as if IT were the navigation center
    float f = -tm.getPerspectiveFactor(pt.z);
    pt.x /= f;
    pt.y /= f;
    pt.z = tm.referencePlaneOffset;
    // now untransform that point to give the center that would
    // deliver this fixedModel position
    tm.matrixTransformInv.rotTrans2(pt, tm.navigationCenter);
    tm.mode = TransformManager.MODE_NAVIGATION;
  }

  @Override
  public void setNavigationOffsetRelative() {//boolean navigatingSurface) {
  //    if (navigatingSurface) {
  //      navigateSurface(Integer.MAX_VALUE);
  //      return;
  //    }
    if (tm.navigationDepthPercent < 0 && tm.navZ > 0 || tm.navigationDepthPercent > 100
        && tm.navZ < 0) {
      tm.navZ = 0;
    }
    tm.rotateXRadians(JC.radiansPerDegree * -.02f * tm.navY, null);
    tm.rotateYRadians(JC.radiansPerDegree * .02f * tm.navX, null);
    P3 pt = tm.navigationCenter;
    P3 pts = new P3();
    tm.transformPt3f(pt, pts);
    pts.z += tm.navZ;
    tm.unTransformPoint(pts, pt);
    tm.setNavigatePt(pt);
  }

  @Override
  public void navigateKey(int keyCode, int modifiers) {
    // 0 0 here means "key released"
    String key = null;
    float value = 0;
    if (tm.mode != TransformManager.MODE_NAVIGATION)
      return;
    if (keyCode == 0) {
      nHits = 0;
      multiplier = 1;
      if (!tm.navigating)
        return;
      tm.navigating = false;
      return;
    }
    nHits++;
    if (nHits % 10 == 0)
      multiplier *= (multiplier == 4 ? 1 : 2);
    //boolean navigateSurface = vwr.getNavigateSurface();
    boolean isShiftKey = ((modifiers & Event.SHIFT_MASK) > 0);
    boolean isAltKey = ((modifiers & Event.ALT_MASK) > 0);
    boolean isCtrlKey = ((modifiers & Event.CTRL_MASK) > 0);
    float speed = vwr.getFloat(T.navigationspeed) * (isCtrlKey ? 10 : 1);
    // race condition vwr.cancelRendering();
    switch (keyCode) {
    case Event.VK_PERIOD:
      tm.navX = tm.navY = tm.navZ = 0;
      tm.homePosition(true);
      return;
    case Event.VK_SPACE:
      if (!tm.navOn)
        return;
      tm.navX = tm.navY = tm.navZ = 0;
      return;
    case Event.VK_UP:
      if (tm.navOn) {
        if (isAltKey) {
          tm.navY += multiplier;
          value = tm.navY;
          key = "navY";
        } else {
          tm.navZ += multiplier;
          value = tm.navZ;
          key = "navZ";
        }
        break;
      }
      //      if (navigateSurface) {
      //        navigateSurface(Integer.MAX_VALUE);
      //        break;
      //      }
      if (isShiftKey) {
        tm.navigationOffset.y -= 2 * multiplier;
        tm.navMode = TransformManager.NAV_MODE_NEWXY;
        break;
      }
      if (isAltKey) {
        tm.rotateXRadians(JC.radiansPerDegree * -.2f * multiplier,
            null);
        tm.navMode = TransformManager.NAV_MODE_NEWXYZ;
        break;
      }
      tm.modelCenterOffset -= speed
          * (vwr.getBoolean(T.navigationperiodic) ? 1 : multiplier);
      tm.navMode = TransformManager.NAV_MODE_NEWZ;
      break;
    case Event.VK_DOWN:
      if (tm.navOn) {
        if (isAltKey) {
          tm.navY -= multiplier;
          value = tm.navY;
          key = "navY";
        } else {
          tm.navZ -= multiplier;
          value = tm.navZ;
          key = "navZ";
        }
        break;
      }
      //      if (navigateSurface) {
      //        navigateSurface(-2 * multiplier);
      //        break;
      //      }
      if (isShiftKey) {
        tm.navigationOffset.y += 2 * multiplier;
        tm.navMode = TransformManager.NAV_MODE_NEWXY;
        break;
      }
      if (isAltKey) {
        tm.rotateXRadians(JC.radiansPerDegree * .2f * multiplier,
            null);
        tm.navMode = TransformManager.NAV_MODE_NEWXYZ;
        break;
      }
      tm.modelCenterOffset += speed
          * (vwr.getBoolean(T.navigationperiodic) ? 1 : multiplier);
      tm.navMode = TransformManager.NAV_MODE_NEWZ;
      break;
    case Event.VK_LEFT:
      if (tm.navOn) {
        tm.navX -= multiplier;
        value = tm.navX;
        key = "navX";
        break;
      }
      //      if (navigateSurface) {
      //        break;
      //      }
      if (isShiftKey) {
        tm.navigationOffset.x -= 2 * multiplier;
        tm.navMode = TransformManager.NAV_MODE_NEWXY;
        break;
      }
      tm.rotateYRadians(JC.radiansPerDegree * 3 * -.2f * multiplier,
          null);
      tm.navMode = TransformManager.NAV_MODE_NEWXYZ;
      break;
    case Event.VK_RIGHT:
      if (tm.navOn) {
        tm.navX += multiplier;
        value = tm.navX;
        key = "navX";
        break;
      }
      //      if (navigateSurface) {
      //        break;
      //      }
      if (isShiftKey) {
        tm.navigationOffset.x += 2 * multiplier;
        tm.navMode = TransformManager.NAV_MODE_NEWXY;
        break;
      }
      tm.rotateYRadians(JC.radiansPerDegree * 3 * .2f * multiplier,
          null);
      tm.navMode = TransformManager.NAV_MODE_NEWXYZ;
      break;
    default:
      tm.navigating = false;
      tm.navMode = TransformManager.NAV_MODE_NONE;
      return;
    }
    if (key != null)
      vwr.g.setF(key, value);
    tm.navigating = true;
    tm.finalizeTransformParameters();
  }

  //  private void navigateSurface(int dz) {
  //    if (vwr.isRepaintPending())
  //      return;
  //    vwr.setShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "navigate",
  //        Integer.valueOf(dz == Integer.MAX_VALUE ? 2 * multiplier : dz));
  //    vwr.requestRepaintAndWait();
  //  }

  @Override
  public void setNavigationDepthPercent(float percent) {
    // navigation depth 0 # place user at rear plane of the model
    // navigation depth 100 # place user at front plane of the model

    vwr.g.setF("navigationDepth", percent);
    tm.calcCameraFactors(); // current
    tm.modelCenterOffset = tm.referencePlaneOffset - (1 - percent / 50)
        * tm.modelRadiusPixels;
    tm.calcCameraFactors(); // updated
    tm.navMode = TransformManager.NAV_MODE_ZOOMED;
  }

  private void calcNavigationDepthPercent() {
    tm.calcCameraFactors(); // current
    tm.navigationDepthPercent = (tm.modelRadiusPixels == 0 ? 50
        : 50 * (1 + (tm.modelCenterOffset - tm.referencePlaneOffset)
            / tm.modelRadiusPixels));
  }

  @Override
  public String getNavigationState() {
    return "# navigation state;\nnavigate 0 center "
        + Escape.eP(tm.navigationCenter)
        + ";\nnavigate 0 translate " + tm.getNavigationOffsetPercent('X') + " "
        + tm.getNavigationOffsetPercent('Y') + ";\nset navigationDepth "
        + tm.navigationDepthPercent + ";\nset navigationSlab "
        + getNavigationSlabOffsetPercent() + ";\n\n";
  }

  private float getNavigationSlabOffsetPercent() {
    tm.calcCameraFactors(); // current
    return 50 * tm.navigationSlabOffset / tm.modelRadiusPixels;
  }

  @Override
  public void navigateAxis(V3 rotAxis, float degrees) {
    if (degrees == 0)
      return;
    tm.rotateAxisAngle(rotAxis,
        (float) (degrees / TransformManager.degreesPerRadian));
    tm.navMode = TransformManager.NAV_MODE_NEWXYZ;
    tm.navigating = true;
    tm.finalizeTransformParameters();
    tm.navigating = false;
  }

  @Override
  public void navTranslatePercentOrTo(float seconds, float x, float y) {
    // from MoveToThread and Viewer
    // if either is Float.NaN, then the other is RELATIVE to current
    // seconds < 0 means "to (x,y)"; >= 0 mean "to (x%, y%)"

    P3 pt1 = P3.new3(x, y, 0);
    if (seconds >= 0)
      setNavPercent(pt1);
    if (!Float.isNaN(pt1.x))
      tm.navigationOffset.x = pt1.x;
    if (!Float.isNaN(pt1.y))
      tm.navigationOffset.y = pt1.y;
    tm.navMode = TransformManager.NAV_MODE_NEWXY;
    tm.navigating = true;
    tm.finalizeTransformParameters();
    tm.navigating = false;
  }

  @Override
  protected void oops(Exception e) {
    super.oops(e);
    tm.navigating = false;
  }


}
