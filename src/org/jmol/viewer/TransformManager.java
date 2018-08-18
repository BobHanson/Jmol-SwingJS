/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-12-18 10:29:29 -0600 (Mon, 18 Dec 2006) $
 * $Revision: 6502 $
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
package org.jmol.viewer;

import org.jmol.api.Interface;
import org.jmol.api.JmolNavigatorInterface;
import org.jmol.api.JmolScriptEvaluator;
import org.jmol.c.STER;
import org.jmol.java.BS;
import org.jmol.script.T;
import org.jmol.thread.JmolThread;
import org.jmol.util.Escape;
import org.jmol.util.Point3fi;

import javajs.util.Lst;
import javajs.util.SB;

import org.jmol.util.Logger;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.A4;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.P3i;
import javajs.util.Quat;
import javajs.util.T3;
import javajs.util.V3;
import org.jmol.util.Vibration;

import java.util.Hashtable;

import java.util.Map;

public class TransformManager {

  protected Viewer vwr;

  final static int DEFAULT_SPIN_Y = 30;
  final static int DEFAULT_SPIN_FPS = 30;
  static final int DEFAULT_NAV_FPS = 10;
  public static final float DEFAULT_VISUAL_RANGE = 5;
  public final static int DEFAULT_STEREO_DEGREES = -5;

  public final static int MODE_STANDARD = 0;
  public final static int MODE_NAVIGATION = 1;
  public final static int MODE_PERSPECTIVE_PYMOL = 2;

  static final int DEFAULT_PERSPECTIVE_MODEL = 11;
  static final boolean DEFAULT_PERSPECTIVE_DEPTH = true;
  static final float DEFAULT_CAMERA_DEPTH = 3.0f;

  public JmolThread movetoThread;
  public JmolThread vibrationThread;
  public JmolThread spinThread;

  public final static double degreesPerRadian = 180 / Math.PI;

  protected int perspectiveModel = DEFAULT_PERSPECTIVE_MODEL;
  protected float cameraScaleFactor;
  public float referencePlaneOffset;
  protected float aperatureAngle;
  protected float cameraDistanceFromCenter;
  public float modelCenterOffset;
  public float modelRadius;
  public float modelRadiusPixels;

  public final P3 navigationCenter = new P3();
  public final P3 navigationOffset = new P3();
  public final P3 navigationShiftXY = new P3();
  public float navigationDepthPercent;

  protected final M4 matrixTemp = new M4();
  protected final V3 vectorTemp = new V3();

  public TransformManager() {
  }

  static TransformManager getTransformManager(Viewer vwr, int width,
                                              int height, boolean is4D) {
    TransformManager me = (is4D ? (TransformManager) Interface.getInterface(
        "org.jmol.viewer.TransformManager4D", vwr, "tm")
        : new TransformManager());
    me.vwr = vwr;
    me.setScreenParameters(width, height, true, false, true, true);
    return me;
  }

  /* ***************************************************************
   * GENERAL METHODS
   ***************************************************************/

  void setDefaultPerspective() {
    setCameraDepthPercent(DEFAULT_CAMERA_DEPTH, true);
    setPerspectiveDepth(DEFAULT_PERSPECTIVE_DEPTH);
    setStereoDegrees(DEFAULT_STEREO_DEGREES);
    visualRangeAngstroms = DEFAULT_VISUAL_RANGE;
    setSpinOff();
    setVibrationPeriod(0);
  }

  public void homePosition(boolean resetSpin) {
    // reset, setNavigationMode, setPerspectiveModel
    if (resetSpin)
      setSpinOff();
    setNavOn(false);
    navFps = DEFAULT_NAV_FPS;
    navX = navY = navZ = 0;
    rotationCenterDefault.setT(vwr.getBoundBoxCenter());
    setFixedRotationCenter(rotationCenterDefault);
    rotationRadiusDefault = setRotationRadius(0, true);
    windowCentered = true;
    setRotationCenterAndRadiusXYZ(null, true);
    resetRotation();
    //if (vwr.autoLoadOrientation()) {
    M3 m = (M3) vwr.ms.getInfoM("defaultOrientationMatrix");
    if (m != null)
      setRotation(m);
    //}
    setZoomEnabled(true);
    zoomToPercent(vwr.g.modelKitMode ? 50 : 100);
    zmPct = zmPctSet;
    slabReset();
    resetFitToScreen(true);
    if (vwr.isJmolDataFrame()) {
      fixedRotationCenter.set(0, 0, 0);
    } else {
      if (vwr.g.axesOrientationRasmol)
        matrixRotate.setAsXRotation((float) Math.PI);
    }
    vwr.stm.saveOrientation("default", null);
    if (mode == MODE_NAVIGATION)
      setNavigationMode(true);
  }

  public void setRotation(M3 m) {
    if (m.isRotation())
      matrixRotate.setM3(m);
    else
      resetRotation();
  }

  public void resetRotation() {
    matrixRotate.setScale(1); // no rotations
  }

  void clearThreads() {
    clearVibration();
    clearSpin();
    setNavOn(false);
    stopMotion();
  }

  void clear() {
    fixedRotationCenter.set(0, 0, 0);
    navigating = false;
    slabPlane = null;
    depthPlane = null;
    zSlabPoint = null;
    resetNavigationPoint(true);
  }

  protected boolean haveNotifiedNaN = false;

  public float spinX;

  public float spinY = DEFAULT_SPIN_Y;

  public float spinZ;

  public float spinFps = DEFAULT_SPIN_FPS;
  public float navX;
  public float navY;
  public float navZ;
  public float navFps = Float.NaN;

  public boolean isSpinInternal = false;
  public boolean isSpinFixed = false;
  boolean isSpinSelected = false;
  protected boolean doTransform4D;

  public final P3 fixedRotationOffset = new P3();
  public final P3 fixedRotationCenter = new P3();
  protected final P3 perspectiveOffset = new P3();
  protected final P3 perspectiveShiftXY = new P3();

  private final P3 rotationCenterDefault = new P3();
  private float rotationRadiusDefault;

  public final A4 fixedRotationAxis = new A4();
  public final A4 internalRotationAxis = new A4();
  protected V3 internalTranslation;
  final P3 internalRotationCenter = P3.new3(0, 0, 0);
  private float internalRotationAngle = 0;

  /* ***************************************************************
   * ROTATIONS
   ***************************************************************/

  // this matrix only holds rotations ... no translations
  public final M3 matrixRotate = new M3();

  protected final M3 matrixTemp3 = new M3();
  private final M4 matrixTemp4 = new M4();
  private final A4 axisangleT = new A4();
  private final V3 vectorT = new V3();
  private final V3 vectorT2 = new V3();
  private final P3 pointT2 = new P3();

  public final static int MAXIMUM_ZOOM_PERCENTAGE = 200000;
  private final static int MAXIMUM_ZOOM_PERSPECTIVE_DEPTH = 10000;

  private void setFixedRotationCenter(T3 center) {
    if (center == null)
      return;
    fixedRotationCenter.setT(center);
  }

  void setRotationPointXY(P3 center) {
    P3i newCenterScreen = transformPt(center);
    fixedTranslation.set(newCenterScreen.x, newCenterScreen.y, 0);
  }

  V3 rotationAxis = new V3();
  float rotationRate = 0;

  void spinXYBy(int xDelta, int yDelta, float speed) {
    // from mouse action
    if (xDelta == 0 && yDelta == 0) {
      if (spinThread != null && spinIsGesture)
        clearSpin();
      return;
    }
    clearSpin();
    P3 pt1 = P3.newP(fixedRotationCenter);
    P3 ptScreen = new P3();
    transformPt3f(pt1, ptScreen);
    P3 pt2 = P3.new3(-yDelta, xDelta, 0);
    pt2.add(ptScreen);
    unTransformPoint(pt2, pt2);
    vwr.setInMotion(false);
    rotateAboutPointsInternal(null, pt2, pt1, 10 * speed, Float.NaN, false,
        true, null, true, null, null, null, null);
  }

  //  final V3 arcBall0 = new V3();
  //  final V3 arcBall1 = new V3();
  //  final V3 arcBallAxis = new V3();
  //  final M3 arcBall0Rotation = new M3();

  //  void rotateArcBall(float x, float y, float factor) {
  //    // radius is half the screen pixel count. 
  //    float radius2 = (screenPixelCount >> 2) * screenPixelCount;
  //    x -= fixedTranslation.x;
  //    y -= fixedTranslation.y;
  //    float z = radius2 - x * x - y * y;
  //    z = (z < 0 ? -1 : 1) * (float) Math.sqrt(Math.abs(z));
  //    if (factor == 0) {
  //      // mouse down sets the initial rotation and point on the sphere
  //      arcBall0Rotation.setM3(matrixRotate);
  //      arcBall0.set(x, -y, z);
  //      if (!Float.isNaN(z))
  //        arcBall0.normalize();
  //      return;
  //    }
  //    if (Float.isNaN(arcBall0.z) || Float.isNaN(z))
  //      return;
  //    arcBall1.set(x, -y, z);
  //    arcBall1.normalize();
  //    arcBallAxis.cross(arcBall0, arcBall1);
  //    axisangleT.setVA(arcBallAxis, factor
  //        * (float) Math.acos(arcBall0.dot(arcBall1)));
  //    setRotation(arcBall0Rotation);
  //    rotateAxisAngle2(axisangleT, null);
  //  }

  protected void rotateXYBy(float degX, float degY, BS bsAtoms) {
    // from mouse action
    //if (vwr.getTestFlag(2)) {
    //  rotateXRadians(degY * JC.radiansPerDegree, bsAtoms);
    //  rotateYRadians(degX * JC.radiansPerDegree, bsAtoms);
    //} else {
    rotate3DBall(degX, degY, bsAtoms);
    //}
  }

  void rotateZBy(int zDelta, int x, int y) {
    if (x != Integer.MAX_VALUE && y != Integer.MAX_VALUE)
      resetXYCenter(x, y);
    rotateZRadians((float) (zDelta / degreesPerRadian));
  }

  private void applyRotation(M3 mNew, boolean isInternal, BS bsAtoms,
                             V3 translation, boolean translationOnly, M4 m4) {
    if (bsAtoms == null) {
      matrixRotate.mul2(mNew, matrixRotate);
      return;
    }
    vwr.moveAtoms(m4, mNew, matrixRotate, translation, internalRotationCenter,
        isInternal, bsAtoms, translationOnly);
    if (translation != null) {
      internalRotationCenter.add(translation);
    }
  }

  protected void rotate3DBall(float xDeg, float yDeg, BS bsAtoms) {
    // xDeg and yDeg are calibrated to be 180 degrees for 
    // a full drag across the frame or from top to bottom.

    // Note: We will apply this matrix to the untransformed
    // model coordinates, not their screen counterparts. 
    // Nonetheless, dx and dy are in terms of the screen. 
    // The swapping of dx and dy, and their reversal in sign
    // probably has to do with the fact that we are changing
    // the signs of both screen Y and screen Z in the end.

    if (matrixTemp3.setAsBallRotation(JC.radiansPerDegree, -yDeg, -xDeg))
      applyRotation(matrixTemp3, false, bsAtoms, null, false, null);
  }

  public synchronized void rotateXRadians(float angleRadians, BS bsAtoms) {
    applyRotation(matrixTemp3.setAsXRotation(angleRadians), false, bsAtoms,
        null, false, null);
  }

  public synchronized void rotateYRadians(float angleRadians, BS bsAtoms) {
    applyRotation(matrixTemp3.setAsYRotation(angleRadians), false, bsAtoms,
        null, false, null);
  }

  public synchronized void rotateZRadians(float angleRadians) {
    applyRotation(matrixTemp3.setAsZRotation(angleRadians), false, null, null,
        false, null);
  }

  public void rotateAxisAngle(V3 rotAxis, float radians) {
    axisangleT.setVA(rotAxis, radians);
    rotateAxisAngle2(axisangleT, null);
  }

  private synchronized void rotateAxisAngle2(A4 axisAngle, BS bsAtoms) {
    applyRotation(matrixTemp3.setAA(axisAngle), false, bsAtoms, null, false, null);
  }

  /*
   * *************************************************************** *THE* TWO
   * VIEWER INTERFACE METHODS
   * **************************************************************
   */

  boolean rotateAxisAngleAtCenter(JmolScriptEvaluator eval, P3 rotCenter,
                                  V3 rotAxis, float degreesPerSecond,
                                  float endDegrees, boolean isSpin, BS bsAtoms) {

    // *THE* Viewer FIXED frame rotation/spinning entry point
    if (rotCenter != null)
      moveRotationCenter(rotCenter, true);

    if (isSpin)
      setSpinOff();
    setNavOn(false);

    if (vwr.headless) {
      if (isSpin && endDegrees == Float.MAX_VALUE)
        return false;
      isSpin = false;
    }
    if (Float.isNaN(degreesPerSecond) || degreesPerSecond == 0
        || endDegrees == 0)
      return false;

    if (rotCenter != null) {
      setRotationPointXY(rotCenter);
    }
    setFixedRotationCenter(rotCenter);
    rotationAxis.setT(rotAxis);
    rotationRate = degreesPerSecond;
    if (isSpin) {
      fixedRotationAxis.setVA(rotAxis, degreesPerSecond * JC.radiansPerDegree);
      isSpinInternal = false;
      isSpinFixed = true;
      isSpinSelected = (bsAtoms != null);
      setSpin(eval, true, endDegrees, null, null, bsAtoms, false);
      // fixed spin -- we will wait
      return (endDegrees != Float.MAX_VALUE);
    }
    float radians = endDegrees * JC.radiansPerDegree;
    fixedRotationAxis.setVA(rotAxis, endDegrees);
    rotateAxisAngleRadiansFixed(radians, bsAtoms);
    return true;
  }

  public synchronized void rotateAxisAngleRadiansFixed(float angleRadians,
                                                       BS bsAtoms) {
    // for spinning -- reduced number of radians
    axisangleT.setAA(fixedRotationAxis);
    axisangleT.angle = angleRadians;
    rotateAxisAngle2(axisangleT, bsAtoms);
  }

  /*
   * *************************************************************** INTERNAL
   * ROTATIONS**************************************************************
   */

  /**
   * 
   * @param eval
   * @param point1
   * @param point2
   * @param degreesPerSecond
   * @param endDegrees
   * @param isClockwise
   * @param isSpin
   * @param bsAtoms
   * @param isGesture
   * @param translation
   * @param finalPoints
   * @param dihedralList
   * @param m4 
   * @return true if synchronous so that JavaScript can restart properly
   */
  boolean rotateAboutPointsInternal(JmolScriptEvaluator eval, T3 point1,
                                    T3 point2, float degreesPerSecond,
                                    float endDegrees, boolean isClockwise,
                                    boolean isSpin, BS bsAtoms,
                                    boolean isGesture, V3 translation,
                                    Lst<P3> finalPoints, float[] dihedralList, M4 m4) {

    // *THE* Viewer INTERNAL frame rotation entry point

    if (isSpin)
      setSpinOff();
    setNavOn(false);

    if (dihedralList == null
        && (translation == null || translation.length() < 0.001)
        && (isSpin ? Float.isNaN(degreesPerSecond) || degreesPerSecond == 0
            : endDegrees == 0))
      return false;

    V3 axis = null;
    if (dihedralList == null) {
      axis = V3.newVsub(point2, point1);
      if (isClockwise)
        axis.scale(-1f);
      internalRotationCenter.setT(point1);
      rotationAxis.setT(axis);
      internalTranslation = (translation == null ? null : V3.newV(translation));
    }
    boolean isSelected = (bsAtoms != null);
    if (isSpin) {
      // we need to adjust the degreesPerSecond to match a multiple of the frame rate
      if (dihedralList == null) {
        if (endDegrees == 0)
          endDegrees = Float.NaN;
        if (Float.isNaN(endDegrees)) {
          rotationRate = degreesPerSecond;
        } else {
          int nFrames = (int) (Math.abs(endDegrees)
              / Math.abs(degreesPerSecond) * spinFps + 0.5);
          rotationRate = degreesPerSecond = endDegrees / nFrames * spinFps;
          if (translation != null)
            internalTranslation.scale(1f / nFrames);
        }
        internalRotationAxis.setVA(axis, (Float.isNaN(rotationRate) ? 0
            : rotationRate) * JC.radiansPerDegree);
        isSpinInternal = true;
        isSpinFixed = false;
        isSpinSelected = isSelected;
      } else {
        endDegrees = degreesPerSecond;
      }
      setSpin(eval, true, endDegrees, finalPoints, dihedralList, bsAtoms,
          isGesture);
      return !Float.isNaN(endDegrees);
    }
    float radians = endDegrees * JC.radiansPerDegree;
    internalRotationAxis.setVA(axis, radians);
    rotateAxisAngleRadiansInternal(radians, bsAtoms, m4);
    return false;
  }

  public synchronized void rotateAxisAngleRadiansInternal(float radians,
                                                          BS bsAtoms, M4 m4) {

    // final matrix rotation when spinning or just rotating

    // trick is to apply the current rotation to the internal rotation axis
    // and then save the angle for generating a new fixed point later
    internalRotationAngle = radians;
    vectorT.set(internalRotationAxis.x, internalRotationAxis.y,
        internalRotationAxis.z);
    matrixRotate.rotate2(vectorT, vectorT2);
    axisangleT.setVA(vectorT2, radians);

    // NOW apply that rotation  

    applyRotation(matrixTemp3.setAA(axisangleT), true, bsAtoms,
        internalTranslation, radians > 1e6f, m4);
    if (bsAtoms == null)
      getNewFixedRotationCenter();
  }

  void getNewFixedRotationCenter() {

    /*
     * (1) determine vector offset VectorT () 
     * (2) translate old point so trueRotationPt is at [0,0,0] (old - true)
     * (3) do axisangle rotation of -radians (pointT2)
     * (4) translate back (pointT2 + vectorT)
     * 
     * The new position of old point is the new rotation center
     * set this, rotate about it, and it will APPEAR that the 
     * rotation was about the desired point and axis!
     *  
     */

    // fractional OPPOSITE of angle of rotation
    axisangleT.setAA(internalRotationAxis);
    axisangleT.angle = -internalRotationAngle;
    //this is a fraction of the original for spinning
    matrixTemp4.setToAA(axisangleT);

    // apply this to the fixed center point in the internal frame

    vectorT.setT(internalRotationCenter);
    pointT2.sub2(fixedRotationCenter, vectorT);
    T3 pt = matrixTemp4.rotTrans2(pointT2, new P3());

    // return this point to the fixed frame

    pt.add(vectorT);

    // it is the new fixed rotation center!

    setRotationCenterAndRadiusXYZ(pt, false);
  }

  /* ***************************************************************
   * TRANSLATIONS
   ****************************************************************/
  public final P3 fixedTranslation = new P3();
  public final P3 camera = new P3();
  public final P3 cameraSetting = new P3();

  float xTranslationFraction = 0.5f;
  float yTranslationFraction = 0.5f;
  protected float prevZoomSetting;

  public float previousX;
  public float previousY;

  void setTranslationFractions() {
    xTranslationFraction = fixedTranslation.x / width;
    yTranslationFraction = fixedTranslation.y / height;
  }

  public void centerAt(int x, int y, P3 pt) {
    if (pt == null) {
      translateXYBy(x, y);
      return;
    }
    if (windowCentered)
      vwr.setBooleanProperty("windowCentered", false);
    fixedTranslation.x = x;
    fixedTranslation.y = y;
    setFixedRotationCenter(pt);
  }

  public int percentToPixels(char xyz, float percent) {
    switch (xyz) {
    case 'x':
      return (int) Math.floor(percent / 100 * width);
    case 'y':
      return (int) Math.floor(percent / 100 * height);
    case 'z':
      return (int) Math.floor(percent / 100 * screenPixelCount);
    }
    return 0;
  }

  int angstromsToPixels(float distance) {
    return (int) Math.floor(scalePixelsPerAngstrom * distance);
  }

  void translateXYBy(int xDelta, int yDelta) {
    // mouse action or translate x|y|z x.x nm|angstroms|%
    fixedTranslation.x += xDelta;
    fixedTranslation.y += yDelta;
    setTranslationFractions();
  }

  public void setCamera(float x, float y) {
    cameraSetting.set(x, y, (x == 0 && y == 0 ? 0 : 1));
  }

  public void translateToPercent(char type, float percent) {
    switch (type) {
    case 'x':
      xTranslationFraction = 0.5f + percent / 100;
      fixedTranslation.x = width * xTranslationFraction;
      return;
    case 'y':
      yTranslationFraction = 0.5f + percent / 100;
      fixedTranslation.y = height * yTranslationFraction;
      return;
    case 'z':
      if (mode == MODE_NAVIGATION)
        setNavigationDepthPercent(percent);
      return;
    }
  }

  public float getTranslationXPercent() {
    return (width == 0 ? 0 : (fixedTranslation.x - width / 2f) * 100 / width);
  }

  public float getTranslationYPercent() {
    return (height == 0 ? 0 : (fixedTranslation.y - height / 2f) * 100 / height);
  }

  public String getTranslationScript() {
    String info = "";
    float f = getTranslationXPercent();
    if (f != 0.0)
      info += "translate x " + f + ";";
    f = getTranslationYPercent();
    if (f != 0.0)
      info += "translate y " + f + ";";
    return info;
  }

  String getOrientationText(int type, boolean isBest) {
    switch (type) {
    case T.moveto:
      return getMoveToText(1, false);
    case T.rotation:
      Quat q = getRotationQ();
      if (isBest)
        q = q.inv();
      return q.toString();
    case T.translation:
      SB sb = new SB();
      float d = getTranslationXPercent();
      truncate2(sb, (isBest ? -d : d));
      d = getTranslationYPercent();
      truncate2(sb, (isBest ? -d : d));
      return sb.toString();
    default:
      return getMoveToText(1, true) + "\n#OR\n" + getRotateZyzText(true);

    }
  }

  public Quat getRotationQ() {
    return Quat.newM(matrixRotate);
  }

  Map<String, Object> getOrientationInfo() {
    Map<String, Object> info = new Hashtable<String, Object>();
    info.put("moveTo", getMoveToText(1, false));
    info.put("center", "center " + getCenterText());
    info.put("centerPt", fixedRotationCenter);
    A4 aa = new A4();
    aa.setM(matrixRotate);
    info.put("axisAngle", aa);
    info.put("quaternion", getRotationQ().toPoint4f());
    info.put("rotationMatrix", matrixRotate);
    info.put("rotateZYZ", getRotateZyzText(false));
    info.put("rotateXYZ", getRotateXyzText());
    info.put("transXPercent", Float.valueOf(getTranslationXPercent()));
    info.put("transYPercent", Float.valueOf(getTranslationYPercent()));
    info.put("zoom", Float.valueOf(zmPct));
    info.put("modelRadius", Float.valueOf(modelRadius));
    if (mode == MODE_NAVIGATION) {
      info.put("navigationCenter",
          "navigate center " + Escape.eP(navigationCenter));
      info.put("navigationOffsetXPercent",
          Float.valueOf(getNavigationOffsetPercent('X')));
      info.put("navigationOffsetYPercent",
          Float.valueOf(getNavigationOffsetPercent('Y')));
      info.put("navigationDepthPercent",
          Float.valueOf(navigationDepthPercent));
    }
    return info;
  }

  public void getRotation(M3 m) {
    // hmm ... I suppose that there could be a race condition here
    // if matrixRotate is being modified while this is called
    m.setM3(matrixRotate);
  }

  /* ***************************************************************
   * ZOOM
   ****************************************************************/
  public boolean zoomEnabled = true;
  
  /**
   * zoom percent
   *  
   * zmPct is the current displayed zoom value, AFTER rendering;
   * may not be the same as zmPctSet, particularly if zoom is not enabled
   *  
   */
  public float zmPct = 100;
  
  /**
   * zoom percent setting
   * 
   * the current setting of zoom;
   * may not be the same as zmPct, particularly  if zoom is not enabled
   * 
   */
  float zmPctSet = 100;
  
  public void setZoomHeight(boolean zoomHeight, boolean zoomLarge) {
    this.zoomHeight = zoomHeight;
    scaleFitToScreen(false, zoomLarge, false, true);
  }

  private float zoomRatio;

  /**
   * standard response to user mouse vertical shift-drag
   * 
   * @param pixels
   */
  protected void zoomBy(int pixels) {
    if (pixels > 20)
      pixels = 20;
    else if (pixels < -20)
      pixels = -20;
    float deltaPercent = pixels * zmPctSet / 50;
    if (deltaPercent == 0)
      deltaPercent = (pixels > 0 ? 1 : (deltaPercent < 0 ? -1 : 0));
    zoomRatio = (deltaPercent + zmPctSet) / zmPctSet;
    zmPctSet += deltaPercent;
  }

  void zoomByFactor(float factor, int x, int y) {
    if (factor <= 0 || !zoomEnabled)
      return;
    if (mode != MODE_NAVIGATION) {
      zoomRatio = factor;
      zmPctSet *= factor;
      resetXYCenter(x, y);
    } else if (getNav()) {
      nav.zoomByFactor(factor, x, y);
    }
  }

  public void zoomToPercent(float percentZoom) {
    zmPctSet = percentZoom;
    zoomRatio = 0;
  }

  void translateZBy(int pixels) {
    if (pixels >= screenPixelCount)
      return;
    float sppa = scalePixelsPerAngstrom
        / (1 - pixels * 1.0f / screenPixelCount);
    if (sppa >= screenPixelCount)
      return;
    float newZoomPercent = sppa / scaleDefaultPixelsPerAngstrom * 100f;
    zoomRatio = newZoomPercent / zmPctSet;
    zmPctSet = newZoomPercent;
  }

  private void resetXYCenter(int x, int y) {
    if (x == Integer.MAX_VALUE || y == Integer.MAX_VALUE)
      return;
    if (windowCentered)
      vwr.setBooleanProperty("windowCentered", false);
    P3 pt = new P3();
    transformPt3f(fixedRotationCenter, pt);
    pt.set(x, y, pt.z);
    unTransformPoint(pt, pt);
    fixedTranslation.set(x, y, 0);
    setFixedRotationCenter(pt);
  }

  void zoomByPercent(float percentZoom) {
    float deltaPercent = percentZoom * zmPctSet / 100;
    if (deltaPercent == 0)
      deltaPercent = (percentZoom < 0) ? -1 : 1;
    zoomRatio = (deltaPercent + zmPctSet) / zmPctSet;
    zmPctSet += deltaPercent;
  }

  void setScaleAngstromsPerInch(float angstromsPerInch) {
    // not compatible with perspectiveDepth
    scale3D = (angstromsPerInch > 0);
    if (scale3D)
      scale3DAngstromsPerInch = angstromsPerInch;
    perspectiveDepth = !scale3D;
  }

  /* ***************************************************************
   * SLAB
   ****************************************************************/

  /*
   slab is a term defined and used in rasmol.
   it is a z-axis clipping plane. only atoms behind the slab get rendered.
   100% means:
   - the slab is set to z==0
   - 100% of the molecule will be shown
   50% means:
   - the slab is set to the center of rotation of the molecule
   - only the atoms behind the center of rotation are shown
   0% means:
   - the slab is set behind the molecule
   - 0% (nothing, nada, nil, null) gets shown
   */

  public boolean slabEnabled;
  public boolean zShadeEnabled;

  public boolean internalSlab;

  int slabPercentSetting;
  int depthPercentSetting;
  public int slabValue;
  public int depthValue;

  public int zSlabPercentSetting = 50; // new default for 12.3.6 and 12.2.6
  public int zDepthPercentSetting = 0;
  public P3 zSlabPoint;
  public int zSlabValue;
  public int zDepthValue;

  float slabRange = 0f;

  public void setSlabRange(float value) {
    slabRange = value;
  }

  void setSlabEnabled(boolean slabEnabled) {
    vwr.g.setB("slabEnabled", this.slabEnabled = slabEnabled);
  }

  void setZShadeEnabled(boolean zShadeEnabled) {
    this.zShadeEnabled = zShadeEnabled;
    vwr.g.setB("zShade", zShadeEnabled);
  }

  void setZoomEnabled(boolean zoomEnabled) {
    this.zoomEnabled = zoomEnabled;
    vwr.g.setB("zoomEnabled", zoomEnabled);
  }

  P4 slabPlane = null;
  P4 depthPlane = null;

  public void slabReset() {
    slabToPercent(100);
    depthToPercent(0);
    depthPlane = null;
    slabPlane = null;
    setSlabEnabled(false);
    setZShadeEnabled(false);
    slabDepthChanged();
  }

  public int getSlabPercentSetting() {
    return slabPercentSetting;
  }

  private void slabDepthChanged() {
    vwr.g.setI("slab", slabPercentSetting);
    vwr.g.setI("depth", depthPercentSetting);
    finalizeTransformParameters(); // also sets _slabPlane and _depthPlane
  }

  void slabByPercentagePoints(int percentage) {
    slabPlane = null;
    if (percentage < 0 ? slabPercentSetting <= Math.max(0, depthPercentSetting) 
        : slabPercentSetting >= 100)
      return;
    slabPercentSetting += percentage;
    slabDepthChanged();
    if (depthPercentSetting >= slabPercentSetting)
      depthPercentSetting = slabPercentSetting - 1;
  }

  void depthByPercentagePoints(int percentage) {
    depthPlane = null;
    if (percentage < 0 ? depthPercentSetting <= 0 
        : depthPercentSetting >= Math.min(100, slabPercentSetting))
      return;
    depthPercentSetting += percentage;
    if (slabPercentSetting <= depthPercentSetting)
      slabPercentSetting = depthPercentSetting + 1;
    slabDepthChanged();
  }

  void slabDepthByPercentagePoints(int percentage) {
    slabPlane = null;
    depthPlane = null;
    if (percentage < 0 ? slabPercentSetting <= Math.max(0, depthPercentSetting) 
        : depthPercentSetting >= Math.min(100, slabPercentSetting))
      return;
    slabPercentSetting += percentage;
    depthPercentSetting += percentage;
    slabDepthChanged();
  }

  public void slabToPercent(int percentSlab) {
    slabPlane = null;
    vwr.setFloatProperty("slabRange", 0);
    slabPercentSetting = percentSlab;
    if (depthPercentSetting >= slabPercentSetting)
      depthPercentSetting = slabPercentSetting - 1;
    slabDepthChanged();
  }

  public void depthToPercent(int percentDepth) {
    depthPlane = null;
    vwr.g.setI("depth", percentDepth);
    depthPercentSetting = percentDepth;
    if (slabPercentSetting <= depthPercentSetting)
      slabPercentSetting = depthPercentSetting + 1;
    slabDepthChanged();
  }

  void zSlabToPercent(int percentSlab) {
    zSlabPercentSetting = percentSlab;
    if (zDepthPercentSetting > zSlabPercentSetting)
      zDepthPercentSetting = percentSlab;
  }

  void zDepthToPercent(int percentDepth) {
    zDepthPercentSetting = percentDepth;
    if (zDepthPercentSetting > zSlabPercentSetting)
      zSlabPercentSetting = percentDepth;
  }

  public void slabInternal(P4 plane, boolean isDepth) {
    //also from vwr
    if (isDepth) {
      depthPlane = plane;
      depthPercentSetting = 0;
    } else {
      slabPlane = plane;
      slabPercentSetting = 100;
    }
    slabDepthChanged();
  }

  /**
   * set internal slab or depth from screen-based slab or depth
   * 
   * @param isDepth
   */
  public void setSlabDepthInternal(boolean isDepth) {
    if (isDepth)
      depthPlane = null;
    else
      slabPlane = null;
    finalizeTransformParameters();
    slabInternal(getSlabDepthPlane(isDepth), isDepth);
  }

  private P4 getSlabDepthPlane(boolean isDepth) {
    // the third row of the matrix defines the Z coordinate, which is all we need
    // and, in fact, it defines the plane. How convenient!
    // eval "slab set"  
    if (isDepth) {
      if (depthPlane != null)
        return depthPlane;
    } else if (slabPlane != null) {
        return slabPlane;
    }
    M4 m = matrixTransform;
    P4 plane = P4.new4(-m.m20, -m.m21, -m.m22, -m.m23
        + (isDepth ? depthValue : slabValue));
    return plane;
  }

  /* ***************************************************************
   * PERSPECTIVE
   ****************************************************************/

  /* Jmol treatment of perspective   Bob Hanson 12/06
   * 
   * See http://www.stolaf.edu/academics/chemapps/jmol/docs/misc/navigation.pdf
   * 


   DEFAULT SCALE -- (zoom == 100) 

   We start by defining a fixedRotationCenter and a modelRadius that encompasses 
   the model. Then: 

   defaultScalePixelsPerAngstrom = screenPixelCount / (2 * modelRadius)

   where:

   screenPixelCount is 2 less than the larger of height or width when zoomLarge == true
   and the smaller of the two when zoomLarge == false

   modelRadius is a rough estimate of the extent of the molecule.
   This pretty much makes a model span the window.

   This is applied as part of the matrixTransform.
   
   ADDING ZOOM
   
   For zoom, we just apply a zoom factor to the default scaling:
   
   scalePixelsPerAngstrom = zoom * defaultScalePixelsPerAngstrom
   
   
   ADDING PERSPECTIVE
   
   Imagine an old fashioned plate camera. The film surface is in front of the model 
   some distance. Lines of perspective go from the plate (our screen) to infinity 
   behind the model. We define:
   
   cameraDistance  -- the distance of the camera in pixels from the FRONT of the model.
   
   cameraDepth     -- a more scalable version of cameraDistance, 
   measured in multiples of screenPixelCount.
   
   The atom position is transformed into screen-based coordinates as:
   
   Z = modelCenterOffset + atom.z * zoom * defaultScalePixelsPerAngstrom

   where 
   
   modelCenterOffset = cameraDistance + screenPixelCount / 2
   
   Z is thus adjusted for zoom such that the center of the model stays in the same position.     
   Defining the position of a vertical plane p as:
   
   p = (modelRadius + zoom * atom.z) / (2 * modelRadius)

   and using the definitions above, we have:

   Z = cameraDistance + screenPixelCount / 2
   + zoom * atom.z * screenPixelCount / (2 * modelRadius)
   
   or, more simply:      
   
   Z = cameraDistance + p * screenPixelCount
   
   This will prove convenient for this discussion (but is never done in code).
   
   All perspective is, is the multiplication of the x and y coordinates by a scaling
   factor that depends upon this screen-based Z coordinate.
   
   We define:
   
   cameraScaleFactor = (cameraDepth + 0.5) / cameraDepth
   referencePlaneOffset = cameraDistance * cameraScaleFactor
   = (cameraDepth + 0.5) * screenPixelCount
   
   and the overall scaling as a function of distance from the camera is simply:
   
   f = perspectiveFactor = referencePlaneOffset / Z
   
   and thus using c for cameraDepth:

   f = (c + 0.5) * screenPixelCount / Z
   = (c + 0.5) * screenPixelCount / (c * screenPixelCount + p * screenPixelCount)
   
   and we simply have:
   
   f = (c + 0.5) / (c + p)
   
   Thus:

   when p = 0,   (front plane) the scale is cameraScaleFactor.
   when p = 0.5, (midplane) the scale is 1.
   when p = 1,   (rear plane) the scale is (cameraDepth + 0.5) / (cameraDepth + 1)
   
   as p approaches infinity, perspectiveFactor goes to 0; 
   if p goes negative, we ignore it. Those points won't be rendered.

   GRAPHICAL INTERPRETATION 
   
   The simplest way to see what is happening is to consider 1/f instead of f:
   
   1/f = (c + p) / (c + 0.5) = c / (c + 0.5) + p / (c + 0.5)
   
   This is a linear function of p, with 1/f=0 at p = -c, the camera position:
   
   
  
   
   \----------------0----------------/    midplane, p = 0.5, 1/f = 1
    \        model center           /     viewingRange = screenPixelCount
     \                             /
      \                           /
       \                         /
        \-----------------------/   front plane, p = 0, 1/f = c / (c + 0.5)
         \                     /    viewingRange = screenPixelCount / f
          \                   /
           \                 /
            \               /   The distance across is the distance that is viewable
             \             /    for this Z position. Just magnify a model and place its
  ^            \           /     center at 0. Whatever part of the model is within the
  |             \         /      triangle will be viewed, scaling each distance so that
  Z increasing    \       /       it ends up screenWidthPixels wide.
  |               \     /
  |                \   /
                   \ /
  Z = 0              X  camera position, p = -c, 1/f = 0
                       viewingRange = 0

   VISUAL RANGE
   
   We simply define a fixed visual range that can be seen by the observer. 
   That range is set at the referencePlaneOffset. Any point ahead of this plane is not shown. 

   VERSION 10
   
   In Jmol 10.2 there was a much more complicated formula for perspectiveFactor, namely
   (where "c" is the cameraDepth):
   
   cameraScaleFactor(old) = 1 + 0.5 / c + 0.02
   z = cameraDistance + (modelRadius + z0) * scalePixelsPerAngstrom * cameraScaleFactor * zoom

   Note that the zoom was being applied in such a way that changing the zoom also changed the
   model midplane position and that the camera scaling factor was being applied in the 
   matrix transformation. This lead to a very complicated but subtle error in perspective.
   
   This error was noticed by Charles Xie and amounts to only a few percent for the 
   cameraDepth that was fixed at 3 in Jmol 10.0. The error was 0 at the front of the model, 
   2% at the middle, and 3.5% at the back, roughly.

   Fixing this error now allows us to adjust cameraDepth at will and to do proper navigation.

   */

  public boolean perspectiveDepth = true;
  protected boolean scale3D = false;
  protected float cameraDepth = 3f;
  protected float cameraDepthSetting = 3f;
  public float visualRangeAngstroms; // set in stateManager to 5f;
  public float cameraDistance = 1000f; // prevent divide by zero on startup

  /**
   * This method returns data needed by the VRML, X3D, and IDTF/U3D exporters.
   * It also should serve as a valuable resource for anyone adapting Jmol and
   * wanting to know how the Jmol 11+ camera business works.
   * @return a set of camera data
   */
  public P3[] getCameraFactors() {
    aperatureAngle = (float) (Math.atan2(screenPixelCount / 2f,
        referencePlaneOffset) * 2 * 180 / Math.PI);
    cameraDistanceFromCenter = referencePlaneOffset / scalePixelsPerAngstrom;

    P3 ptRef = P3.new3(screenWidth / 2, screenHeight / 2, referencePlaneOffset);
    unTransformPoint(ptRef, ptRef);

    // NOTE: Camera position will be approximate.
    // when the model has been shifted with CTRL-ALT
    // the center of distortion is not the screen center.
    // The simpler perspective model in VRML and U3D 
    // doesn't allow for that. (of course, one could argue,
    // that's because they are more REALISTIC). We do it
    // this way so that visual metrics in the model are preserved 
    // when the model is shifted using CTRL-ALT, and it was found
    // that if you didn't do that, moving the model was very odd
    // in that a fish-eye distortion was present as you moved it.

    // note that navigation mode should be EXACTLY reproduced
    // in these renderers. 

    P3 ptCamera = P3.new3(screenWidth / 2, screenHeight / 2, 0);
    unTransformPoint(ptCamera, ptCamera);
    ptCamera.sub(fixedRotationCenter);
    P3 pt = P3.new3(screenWidth / 2, screenHeight / 2, cameraDistanceFromCenter
        * scalePixelsPerAngstrom);
    unTransformPoint(pt, pt);
    pt.sub(fixedRotationCenter);
    ptCamera.add(pt);

    //        System.out.println("TM no " + navigationOffset + " rpo "
    //            + referencePlaneOffset + " aa " + aperatureAngle + " sppa "
    //            + scalePixelsPerAngstrom + " vr " + visualRange + " sw/vr "
    //            + screenWidth / visualRange + " " + ptRef + " " + fixedRotationCenter);

    return new P3[] {
        ptRef,
        ptCamera,
        fixedRotationCenter,
        P3.new3(cameraDistanceFromCenter, aperatureAngle,
            scalePixelsPerAngstrom) };
  }

  void setPerspectiveDepth(boolean perspectiveDepth) {
    if (this.perspectiveDepth == perspectiveDepth)
      return;
    this.perspectiveDepth = perspectiveDepth;
    vwr.g.setB("perspectiveDepth", perspectiveDepth);
    resetFitToScreen(false);
  }

  public boolean getPerspectiveDepth() {
    return perspectiveDepth;
  }

  /**
   * either as a percent -300, or as a float 3.0 note this percent is of
   * zoom=100 size of model
   * 
   * @param percent
   * @param resetSlab
   */
  public void setCameraDepthPercent(float percent, boolean resetSlab) {
    resetNavigationPoint(resetSlab);
    float screenMultiples = (percent < 0 ? -percent / 100 : percent);
    if (screenMultiples == 0)
      return;
    cameraDepthSetting = screenMultiples;
    vwr.g.setF("cameraDepth", cameraDepthSetting);
    //if (mode == MODE_NAVIGATION)// don't remember why we would do that...
    cameraDepth = Float.NaN;
  }

  public float getCameraDepth() {
    return cameraDepthSetting;
  }


  //  M4 getUnscaledTransformMatrix() {
  //    //for povray only
  //    M4 unscaled = M4.newM4(null);
  //    vectorTemp.setT(fixedRotationCenter);
  //    matrixTemp.setZero();
  //    matrixTemp.setTranslation(vectorTemp);
  //    unscaled.sub(matrixTemp);
  //    matrixTemp.setToM3(matrixRotate);
  //    unscaled.mul2(matrixTemp, unscaled);
  //    return unscaled;
  //  }

  /* ***************************************************************
   * SCREEN SCALING
   ****************************************************************/
  public int width;

  public int height;
  public int screenPixelCount;
  float scalePixelsPerAngstrom;
  public float scaleDefaultPixelsPerAngstrom;
  float scale3DAngstromsPerInch;
  protected boolean antialias;
  private boolean useZoomLarge, zoomHeight;

  int screenWidth, screenHeight;

  private void setScreenParameters0(int screenWidth, int screenHeight,
                                    boolean useZoomLarge, boolean antialias,
                                    boolean resetSlab, boolean resetZoom) {
    if (screenWidth == Integer.MAX_VALUE)
      return;
    this.screenWidth = screenWidth;
    this.screenHeight = screenHeight;
    this.useZoomLarge = useZoomLarge;
    this.antialias = antialias;
    width = (antialias ? screenWidth * 2 : screenWidth);
    height = (antialias ? screenHeight * 2 : screenHeight);
    scaleFitToScreen(false, useZoomLarge, resetSlab, resetZoom);
  }

  void setAntialias(boolean TF) {
    boolean isNew = (antialias != TF);
    antialias = TF;
    width = (antialias ? screenWidth * 2 : screenWidth);
    height = (antialias ? screenHeight * 2 : screenHeight);
    if (isNew)
      scaleFitToScreen(false, useZoomLarge, false, false);
  }

  public float defaultScaleToScreen(float radius) {
    /* 
     * 
     * the presumption here is that the rotation center is at pixel
     * (150,150) of a 300x300 window. modelRadius is
     * a rough estimate of the furthest distance from the center of rotation
     * (but not including pmesh, special lines, planes, etc. -- just atoms)
     * 
     * also that we do not want it to be possible for the model to rotate
     * out of bounds of the applet. For internal spinning I had to turn
     * of any calculation that would change the rotation radius.  hansonr
     * 
     */
    return screenPixelCount / 2f / radius;
  }

  private void resetFitToScreen(boolean andCenter) {
    scaleFitToScreen(andCenter, vwr.g.zoomLarge, true, true);
  }

  void scaleFitToScreen(boolean andCenter, boolean zoomLarge,
                        boolean resetSlab, boolean resetZoom) {
    if (width == 0 || height == 0) {
      screenPixelCount = 1;
    } else {

      // translate to the middle of the screen
      fixedTranslation.set(width * (andCenter ? 0.5f : xTranslationFraction),
          height * (andCenter ? 0.5f : yTranslationFraction), 0);
      setTranslationFractions();
      if (andCenter)
        camera.set(0, 0, 0);
      if (resetZoom)
        resetNavigationPoint(resetSlab);
      // 2005 02 22
      // switch to finding larger screen dimension
      // find smaller screen dimension
      if (zoomHeight)
        zoomLarge = (height > width);
      screenPixelCount = (zoomLarge == (height > width) ? height : width);
      //screenPixelCount = Math.min(height, width, arg1);
    }
    // ensure that rotations don't leave some atoms off the screen
    // note that this radius is to the furthest outside edge of an atom
    // given the current VDW radius setting. it is currently *not*
    // recalculated when the vdw radius settings are changed
    // leave a very small margin - only 1 on top and 1 on bottom
    if (screenPixelCount > 2)
      screenPixelCount -= 2;
    scaleDefaultPixelsPerAngstrom = defaultScaleToScreen(modelRadius);
  }

  public float scaleToScreen(int z, int milliAngstroms) {
    if (milliAngstroms == 0 || z < 2)
      return 0;
    float pixelSize = scaleToPerspective(z, milliAngstroms
        * scalePixelsPerAngstrom / 1000);
    return (pixelSize > 0 ? pixelSize : 1);
  }

  public float unscaleToScreen(float z, float screenDistance) {
    float d = screenDistance / scalePixelsPerAngstrom;
    return (perspectiveDepth ? d / getPerspectiveFactor(z) : d);
  }

  public float scaleToPerspective(int z, float sizeAngstroms) {
    //DotsRenderer only
    //old: return (perspectiveDepth ? sizeAngstroms * perspectiveFactor(z)
    //: sizeAngstroms);

    return (perspectiveDepth ? sizeAngstroms * getPerspectiveFactor(z)
        : sizeAngstroms);

  }

  /* ***************************************************************
   * TRANSFORMATIONS
   ****************************************************************/

  public final M4 matrixTransform = new M4();
  public final M4 matrixTransformInv = new M4();

   protected final P3 fScrPt = new P3();
  protected final P3i iScrPt = new P3i();

  final Point3fi ptVibTemp = new Point3fi();

  public boolean navigating = false;
  public int mode = MODE_STANDARD;
  public int defaultMode = MODE_STANDARD;

  void setNavigationMode(boolean TF) {
    mode = (TF ? MODE_NAVIGATION : defaultMode);
    resetNavigationPoint(true);
  }

  public boolean isNavigating() {
    return navigating || navOn;
  }

  public synchronized void finalizeTransformParameters() {
    haveNotifiedNaN = false;
    fixedRotationOffset.setT(fixedTranslation);
    camera.setT(cameraSetting);
    internalSlab = slabEnabled && (slabPlane != null || depthPlane != null);
    float newZoom = getZoomSetting();
    if (zmPct != newZoom) {
      zmPct = newZoom;
      if (!vwr.g.fontCaching)
        vwr.gdata.clearFontCache();
    }
    calcCameraFactors();
    calcTransformMatrix();
    if (mode == MODE_NAVIGATION)
      calcNavigationPoint();
    else
      calcSlabAndDepthValues();
  }

  public float getZoomSetting() {
    if (zmPctSet < 5)
      zmPctSet = 5;
    if (zmPctSet > MAXIMUM_ZOOM_PERCENTAGE)
      zmPctSet = MAXIMUM_ZOOM_PERCENTAGE;
    return (zoomEnabled || mode == MODE_NAVIGATION ? zmPctSet : 100);
  }

  /**
   * sets slab and depth, possibly using visual range considerations for setting
   * the slab-clipping plane. (slab on; slab 0)
   * 
   * superceded in navigation mode
   * 
   */

  public void calcSlabAndDepthValues() {
    if (slabRange < 1)
      slabValue = zValueFromPercent(slabPercentSetting);
    else
      slabValue = (int) Math.floor(modelCenterOffset * slabRange
          / (2 * modelRadius) * (zmPctSet / 100));
    depthValue = zValueFromPercent(depthPercentSetting);
    if (zSlabPercentSetting == zDepthPercentSetting) {
      zSlabValue = slabValue;
      zDepthValue = depthValue;
    } else {
      zSlabValue = zValueFromPercent(zSlabPercentSetting);
      zDepthValue = zValueFromPercent(zDepthPercentSetting);
    }
    if (zSlabPoint != null) {
      try {
        transformPt3f(zSlabPoint, pointT2);
        zSlabValue = (int) pointT2.z;
      } catch (Exception e) {
        // don't care
      }
    }
    vwr.g.setO("_slabPlane", Escape.eP4(getSlabDepthPlane(false)));
    vwr.g.setO("_depthPlane", Escape.eP4(getSlabDepthPlane(true)));
    if (slabEnabled)
      return;
    slabValue = 0;
    depthValue = Integer.MAX_VALUE;
  }

  public int zValueFromPercent(int zPercent) {
    return (int) Math.floor((1 - zPercent / 50f) * modelRadiusPixels
        + modelCenterOffset);
  }

  public synchronized void calcTransformMatrix() {

    matrixTransform.setIdentity();

    // first, translate the coordinates back to the center
    
    vectorTemp.sub2(frameOffset, fixedRotationCenter);
    matrixTransform.setTranslation(vectorTemp);

    // multiply by angular rotations
    // this is *not* the same as  matrixTransform.mul(matrixRotate);
    matrixTemp.setToM3(stereoFrame ? matrixStereo : matrixRotate);
    matrixTransform.mul2(matrixTemp, matrixTransform);
    // scale to screen coordinates
    matrixTemp.setIdentity();
    matrixTemp.m00 = matrixTemp.m11 = matrixTemp.m22 = scalePixelsPerAngstrom;
    // negate y (for screen) and z (for zbuf)
    matrixTemp.m11 = matrixTemp.m22 = -scalePixelsPerAngstrom;

    matrixTransform.mul2(matrixTemp, matrixTransform);
    //z-translate to set rotation center at midplane (Nav) or front plane (V10)
    matrixTransform.m23 += modelCenterOffset;
    try {
      matrixTransformInv.setM4(matrixTransform).invert();
    } catch (Exception e) {
      System.out.println("ERROR INVERTING matrixTransform!");
      // ignore -- this is a Mac issue on applet startup
    }
    // note that the image is still centered at 0, 0 in the xy plane

    //System.out.println("TM matrixTransform " + matrixTransform);
  }

  public void rotatePoint(T3 pt, T3 ptRot) {
    matrixRotate.rotate2(pt, ptRot);
    ptRot.y = -ptRot.y;
  }

  protected void getScreenTemp(T3 ptXYZ) {
    matrixTransform.rotTrans2(ptXYZ, fScrPt);
  }

  public void transformPtScr(T3 ptXYZ, P3i pointScreen) {
    pointScreen.setT(transformPt(ptXYZ));
  }

  public void transformPtScrT3(T3 ptXYZ, T3 pointScreen) {
    transformPt(ptXYZ);
    // note that this point may be returned as z=1 if the point is 
    // past the camera or slabbed internally
    pointScreen.setT(fScrPt);
  }

  public void transformPt3f(T3 ptXYZ, P3 screen) {
    applyPerspective(ptXYZ, ptXYZ);
    screen.setT(fScrPt);
  }

  public void transformPtNoClip(T3 ptXYZ, T3 pointScreen) {
    applyPerspective(ptXYZ, null);
    pointScreen.setT(fScrPt);
  }

  /**
   * CAUTION! returns a POINTER TO A TEMPORARY VARIABLE
   * 
   * @param ptXYZ
   * @return POINTER TO point3iScreenTemp
   */
  public synchronized P3i transformPt(T3 ptXYZ) {
    return applyPerspective(ptXYZ, internalSlab ? ptXYZ : null);
  }

  /**
   * @param ptXYZ
   * @param v
   * @return POINTER TO TEMPORARY VARIABLE (caution!) point3iScreenTemp
   */
  public P3i transformPtVib(P3 ptXYZ, Vibration v) {
    ptVibTemp.setT(ptXYZ);
    return applyPerspective(getVibrationPoint(v, ptVibTemp, Float.NaN), ptXYZ);
  }
  
  /**
   * return 
   * @param v
   * @param pt temporary value; also returned
   * @param scale
   * @return pt
   */
  public T3 getVibrationPoint(Vibration v, T3 pt, float scale) {
    return v.setCalcPoint(pt, vibrationT,
        (Float.isNaN(scale) ? vibrationScale : scale), vwr.g.modulationScale);
  }

  public synchronized P3i transformPt2D(T3 ptXyp) {
    // axes position [50 50]
    // just does the processing for [x y] and [x y %]
    if (ptXyp.z == -Float.MAX_VALUE) {
      iScrPt.x = (int) Math.floor(ptXyp.x / 100 * screenWidth);
      iScrPt.y = (int) Math
          .floor((1 - ptXyp.y / 100) * screenHeight);
    } else {
      iScrPt.x = (int) ptXyp.x;
      iScrPt.y = (screenHeight - (int) ptXyp.y);
    }
    if (antialias) {
      iScrPt.x <<= 1;
      iScrPt.y <<= 1;
    }
    matrixTransform.rotTrans2(fixedRotationCenter, fScrPt);
    iScrPt.z = (int) fScrPt.z;
    return iScrPt;
  }

  /**
   * adjusts the temporary point for perspective and offsets
   * 
   * @param ptXYZ
   * @param ptRef
   * @return temporary point!!!
   * 
   */
  private P3i applyPerspective(T3 ptXYZ, T3 ptRef) {

    getScreenTemp(ptXYZ);
    //System.out.println(point3fScreenTemp);

    // fixedRotation point is at the origin initially

    float z = fScrPt.z;

    // this could easily go negative -- behind the screen --
    // but we don't care. In fact, that just makes it easier,
    // because it means we won't render it.
    // we should probably assign z = 0 as "unrenderable"

    if (Float.isNaN(z)) {
      if (!haveNotifiedNaN && Logger.debugging)
        Logger.debug("NaN seen in TransformPoint");
      haveNotifiedNaN = true;
      z = fScrPt.z = 1;
    } else if (z <= 0) {
      // just don't let z go past 1 BH 11/15/06
      z = fScrPt.z = 1;
    }

    // x and y are moved inward (generally) relative to 0, which
    // is either the fixed rotation center or the navigation center

    // at this point coordinates are centered on rotation center

    switch (mode) {
    case MODE_NAVIGATION:
      // move nav center to 0; refOffset = Nav - Rot
      fScrPt.x -= navigationShiftXY.x;
      fScrPt.y -= navigationShiftXY.y;
      break;
    case MODE_PERSPECTIVE_PYMOL:
      fScrPt.x += perspectiveShiftXY.x;
      fScrPt.y += perspectiveShiftXY.y;
      break;
    }
    if (perspectiveDepth) {
      // apply perspective factor
      float factor = getPerspectiveFactor(z);
      fScrPt.x *= factor;
      fScrPt.y *= factor;
    }
    switch (mode) {
    case MODE_NAVIGATION:
      fScrPt.x += navigationOffset.x;
      fScrPt.y += navigationOffset.y;
      break;
    case MODE_PERSPECTIVE_PYMOL:
      fScrPt.x -= perspectiveShiftXY.x;
      fScrPt.y -= perspectiveShiftXY.y;
      //$FALL-THROUGH$
    case MODE_STANDARD:
      fScrPt.x += fixedRotationOffset.x;
      fScrPt.y += fixedRotationOffset.y;
      break;
    }
    if (Float.isNaN(fScrPt.x) && !haveNotifiedNaN) {
      if (Logger.debugging)
        Logger.debug("NaN found in transformPoint ");
      haveNotifiedNaN = true;
    }

    iScrPt.set((int) fScrPt.x, (int) fScrPt.y,
        (int) fScrPt.z);

    if (ptRef != null && xyzIsSlabbedInternal(ptRef))
      fScrPt.z = iScrPt.z = 1;
    return iScrPt;
  }

  public boolean xyzIsSlabbedInternal(T3 ptRef) {
    return (slabPlane != null
        && ptRef.x * slabPlane.x + ptRef.y * slabPlane.y + ptRef.z
            * slabPlane.z + slabPlane.w > 0 || depthPlane != null
        && ptRef.x * depthPlane.x + ptRef.y * depthPlane.y + ptRef.z
            * depthPlane.z + depthPlane.w < 0);
  }

  final protected P3 untransformedPoint = new P3();

  /* ***************************************************************
   * move/moveTo support
   ****************************************************************/

  void move(JmolScriptEvaluator eval, V3 dRot, float dZoom, V3 dTrans,
            float dSlab, float floatSecondsTotal, int fps) {

    movetoThread = (JmolThread) Interface.getOption("thread.MoveToThread", vwr,
        "tm");
    movetoThread.setManager(this, vwr, new Object[] { dRot, dTrans,
        new float[] { dZoom, dSlab, floatSecondsTotal, fps } });
    if (floatSecondsTotal > 0)
      movetoThread.setEval(eval);
    movetoThread.run();
  }

  protected final P3 ptTest1 = new P3();
  protected final P3 ptTest2 = new P3();
  protected final P3 ptTest3 = new P3();
  protected final A4 aaTest1 = new A4();
  protected final M3 matrixTest = new M3();

  public boolean isInPosition(V3 axis, float degrees) {
    if (Float.isNaN(degrees))
      return true;
    aaTest1.setVA(axis, (float) (degrees / degreesPerRadian));
    ptTest1.set(4.321f, 1.23456f, 3.14159f);
    getRotation(matrixTest);
    matrixTest.rotate2(ptTest1, ptTest2);
    matrixTest.setAA(aaTest1).rotate2(ptTest1, ptTest3);
    return (ptTest3.distance(ptTest2) < 0.1);
  }

  public boolean moveToPyMOL(JmolScriptEvaluator eval, float floatSecondsTotal,
                             float[] pymolView) {
    // PyMOL matrices are inverted (row-based)
    M3 m3 = M3.newA9(pymolView);
    m3.invert();
    float cameraX = pymolView[9];
    float cameraY = -pymolView[10];
    float pymolDistanceToCenter = -pymolView[11];
    P3 center = P3.new3(pymolView[12], pymolView[13], pymolView[14]);
    float pymolDistanceToSlab = pymolView[15]; // <=0 to ignore
    float pymolDistanceToDepth = pymolView[16];
    float fov = pymolView[17];
    boolean isOrtho = (fov >= 0);
    setPerspectiveDepth(!isOrtho);

    // note that set zoomHeight is required for proper zooming

    // calculate Jmol camera position, which is in screen widths,
    // and is from the front of the screen, not the center.
    //
    //               |--screen height--| 1 unit
    //                       |-rotrad -| 
    //                       o        /
    //                       |       /
    //                       |theta /
    //                       |     /
    // pymolDistanceToCenter |    /
    //                       |   /
    //                       |  /
    //                       | / theta = fov/2
    //                       |/
    //

    // we convert fov to rotation radius
    float theta = Math.abs(fov) / 2;
    float tan = (float) Math.tan(theta * Math.PI / 180);
    float rotationRadius = pymolDistanceToCenter * tan;

    // Jmol camera units are fraction of screen size (height in this case)
    float jmolCameraToCenter = 0.5f / tan;
    float cameraDepth = jmolCameraToCenter - 0.5f;

    // other units are percent; this factor is 100% / (2*rotationRadius)
    float f = 50 / rotationRadius;

    if (pymolDistanceToSlab > 0) {
      int slab = 50 + (int) ((pymolDistanceToCenter - pymolDistanceToSlab) * f);
      int depth = 50 + (int) ((pymolDistanceToCenter - pymolDistanceToDepth) * f);
      // could animate these? Does PyMOL?
      setSlabEnabled(true);
      slabToPercent(slab);
      depthToPercent(depth);
      if (pymolView.length == 21) {
        // from PSE file load only -- 
        boolean depthCue = (pymolView[18] != 0);
        boolean fog = (pymolView[19] != 0);
        float fogStart = pymolView[20];
        // conversion to Jmol zShade, zSlab, zDepth
        setZShadeEnabled(depthCue);
        if (depthCue) {
          if (fog) {
            vwr.setIntProperty("zSlab",
                (int) Math.min(100, slab + fogStart * (depth - slab)));
          } else {
            vwr.setIntProperty("zSlab", (int) ((slab + depth) / 2f));
          }
          vwr.setIntProperty("zDepth", depth);
        }
      }
    }
    moveTo(eval, floatSecondsTotal, center, null, 0, m3, 100, Float.NaN,
        Float.NaN, rotationRadius, null, Float.NaN, Float.NaN, Float.NaN,
        cameraDepth, cameraX, cameraY);
    return true;
  }

  // from Viewer
  void moveTo(JmolScriptEvaluator eval, float floatSecondsTotal, P3 center,
              T3 rotAxis, float degrees, M3 matrixEnd, float zoom,
              float xTrans, float yTrans, float newRotationRadius,
              P3 navCenter, float xNav, float yNav, float navDepth,
              float cameraDepth, float cameraX, float cameraY) {
    if (matrixEnd == null) {
      matrixEnd = new M3();
      V3 axis = V3.newV(rotAxis);
      if (Float.isNaN(degrees)) {
        matrixEnd.m00 = Float.NaN;
      } else if (degrees < 0.01f && degrees > -0.01f) {
        // getRotation(matrixEnd);
        matrixEnd.setScale(1);
      } else {
        if (axis.x == 0 && axis.y == 0 && axis.z == 0) {
          // invalid ... no rotation
          /*
           * why were we then sleeping? int sleepTime = (int) (floatSecondsTotal
           * * 1000) - 30; if (sleepTime > 0) { try { Thread.sleep(sleepTime); }
           * catch (InterruptedException ie) { } }
           */
          return;
        }
        A4 aaMoveTo = new A4();
        aaMoveTo.setVA(axis, (float) (degrees / degreesPerRadian));
        matrixEnd.setAA(aaMoveTo);
      }
    }
    if (cameraX == cameraSetting.x)
      cameraX = Float.NaN;
    if (cameraY == cameraSetting.y)
      cameraY = Float.NaN;
    if (cameraDepth == this.cameraDepth)
      cameraDepth = Float.NaN;
    if (!Float.isNaN(cameraX))
      xTrans = cameraX * 50 / newRotationRadius / width * screenPixelCount;
    if (!Float.isNaN(cameraY))
      yTrans = cameraY * 50 / newRotationRadius / height * screenPixelCount;
    float pixelScale = (center == null ? scaleDefaultPixelsPerAngstrom
        : defaultScaleToScreen(newRotationRadius));
    if (floatSecondsTotal <= 0) {
      setAll(center, matrixEnd, navCenter, zoom, xTrans, yTrans,
          newRotationRadius, pixelScale, navDepth, xNav, yNav, cameraDepth,
          cameraX, cameraY);
      vwr.moveUpdate(floatSecondsTotal);
      vwr.finalizeTransformParameters();
      return;
    }

    try {
      if (movetoThread == null)
        movetoThread = (JmolThread) Interface.getOption("thread.MoveToThread",
            vwr, "tm");
      int nSteps = movetoThread.setManager(this, vwr, new Object[] {
          center,
          matrixEnd,
          navCenter,
          new float[] { floatSecondsTotal, zoom, xTrans, yTrans,
              newRotationRadius, pixelScale, navDepth, xNav, yNav, cameraDepth,
              cameraX, cameraY } });
      if (nSteps <= 0 || vwr.g.waitForMoveTo) {
        if (nSteps > 0)
          movetoThread.setEval(eval);
        movetoThread.run();
        if (!vwr.isSingleThreaded)
          movetoThread = null;
      } else {
        movetoThread.start();
      }
    } catch (Exception e) {
      // ignore
    }
  }

  public void setAll(P3 center, M3 m, P3 navCenter, float zoom, float xTrans,
                     float yTrans, float rotationRadius, float pixelScale,
                     float navDepth, float xNav, float yNav, float cameraDepth,
                     float cameraX, float cameraY) {
    if (!Float.isNaN(m.m00))
      setRotation(m);
    if (center != null)
      moveRotationCenter(center, !windowCentered);
    if (navCenter != null && mode == MODE_NAVIGATION)
      navigationCenter.setT(navCenter);
    if (!Float.isNaN(cameraDepth))
      setCameraDepthPercent(cameraDepth, false);
    if (!Float.isNaN(cameraX) && !Float.isNaN(cameraY))
      setCamera(cameraX, cameraY);
    if (!Float.isNaN(zoom))
      zoomToPercent(zoom);
    if (!Float.isNaN(rotationRadius))
      modelRadius = rotationRadius;
    if (!Float.isNaN(pixelScale))
      scaleDefaultPixelsPerAngstrom = pixelScale;
    if (!Float.isNaN(xTrans) && !Float.isNaN(yTrans)) {
      translateToPercent('x', xTrans);
      translateToPercent('y', yTrans);
    }

    if (mode == MODE_NAVIGATION) {
      if (!Float.isNaN(xNav) && !Float.isNaN(yNav))
        navTranslatePercentOrTo(0, xNav, yNav);
      if (!Float.isNaN(navDepth))
        setNavigationDepthPercent(navDepth);
    }
  }

  public void stopMotion() {
    movetoThread = null;
    //setSpinOff();// trouble here with Viewer.checkHalt
  }

  String getRotationText() {
    axisangleT.setM(matrixRotate);
    float degrees = (float) (axisangleT.angle * degreesPerRadian);
    SB sb = new SB();
    vectorT.set(axisangleT.x, axisangleT.y, axisangleT.z);
    if (degrees < 0.01f)
      return "{0 0 1 0}";
    vectorT.normalize();
    vectorT.scale(1000);
    sb.append("{");
    truncate0(sb, vectorT.x);
    truncate0(sb, vectorT.y);
    truncate0(sb, vectorT.z);
    truncate2(sb, degrees);
    sb.append("}");
    return sb.toString();
  }

  public String getMoveToText(float timespan, boolean addComments) {
    finalizeTransformParameters();
    SB sb = new SB();
    sb.append("moveto ");
    if (addComments)
      sb.append("/* time, axisAngle */ ");
    sb.appendF(timespan);
    sb.append(" ").append(getRotationText());
    if (addComments)
      sb.append(" /* zoom, translation */ ");
    truncate2(sb, zmPctSet);
    truncate2(sb, getTranslationXPercent());
    truncate2(sb, getTranslationYPercent());
    sb.append(" ");
    if (addComments)
      sb.append(" /* center, rotationRadius */ ");
    sb.append(getCenterText());
    sb.append(" ").appendF(modelRadius);
    sb.append(getNavigationText(addComments));
    if (addComments)
      sb.append(" /* cameraDepth, cameraX, cameraY */ ");
    truncate2(sb, cameraDepth);
    truncate2(sb, cameraSetting.x);
    truncate2(sb, cameraSetting.y);
    sb.append(";");
    return sb.toString();
  }

  private String getCenterText() {
    return Escape.eP(fixedRotationCenter);
  }

  private String getRotateXyzText() {
    SB sb = new SB();
    float m20 = matrixRotate.m20;
    float rY = -(float) (Math.asin(m20) * degreesPerRadian);
    float rX, rZ;
    if (m20 > .999f || m20 < -.999f) {
      rX = -(float) (Math.atan2(matrixRotate.m12, matrixRotate.m11) * degreesPerRadian);
      rZ = 0;
    } else {
      rX = (float) (Math.atan2(matrixRotate.m21, matrixRotate.m22) * degreesPerRadian);
      rZ = (float) (Math.atan2(matrixRotate.m10, matrixRotate.m00) * degreesPerRadian);
    }
    sb.append("reset");
    sb.append(";center ").append(getCenterText());
    if (rX != 0) {
      sb.append("; rotate x");
      truncate2(sb, rX);
    }
    if (rY != 0) {
      sb.append("; rotate y");
      truncate2(sb, rY);
    }
    if (rZ != 0) {
      sb.append("; rotate z");
      truncate2(sb, rZ);
    }
    sb.append(";");
    addZoomTranslationNavigationText(sb);
    return sb.toString();
  }

  private void addZoomTranslationNavigationText(SB sb) {
    if (zmPct != 100) {
      sb.append(" zoom");
      truncate2(sb, zmPct);
      sb.append(";");
    }
    float tX = getTranslationXPercent();
    if (tX != 0) {
      sb.append(" translate x");
      truncate2(sb, tX);
      sb.append(";");
    }
    float tY = getTranslationYPercent();
    if (tY != 0) {
      sb.append(" translate y");
      truncate2(sb, tY);
      sb.append(";");
    }
    if (modelRadius != rotationRadiusDefault || modelRadius == 10) {
      // after ZAP;load APPEND   we need modelRadius, which is 10
      sb.append(" set rotationRadius");
      truncate2(sb, modelRadius);
      sb.append(";");
    }
    if (mode == MODE_NAVIGATION) {
      sb.append("navigate 0 center ").append(Escape.eP(navigationCenter));
      sb.append(";navigate 0 translate");
      truncate2(sb, getNavigationOffsetPercent('X'));
      truncate2(sb, getNavigationOffsetPercent('Y'));
      sb.append(";navigate 0 depth ");
      truncate2(sb, navigationDepthPercent);
      sb.append(";");
    }
  }

  private String getRotateZyzText(boolean iAddComment) {
    SB sb = new SB();
    M3 m = (M3) vwr.ms.getInfoM("defaultOrientationMatrix");
    if (m == null) {
      m = matrixRotate;
    } else {
      m = M3.newM3(m);
      m.invert();
      m.mul2(matrixRotate, m);
    }
    float m22 = m.m22;
    float rY = (float) (Math.acos(m22) * degreesPerRadian);
    float rZ1, rZ2;
    if (m22 > .999f || m22 < -.999f) {
      rZ1 = (float) (Math.atan2(m.m10, m.m11) * degreesPerRadian);
      rZ2 = 0;
    } else {
      rZ1 = (float) (Math.atan2(m.m21, -m.m20) * degreesPerRadian);
      rZ2 = (float) (Math.atan2(m.m12, m.m02) * degreesPerRadian);
    }
    if (rZ1 != 0 && rY != 0 && rZ2 != 0 && iAddComment)
      sb.append("#Follows Z-Y-Z convention for Euler angles\n");
    sb.append("reset");
    sb.append(";center ").append(getCenterText());
    if (rZ1 != 0) {
      sb.append("; rotate z");
      truncate2(sb, rZ1);
    }
    if (rY != 0) {
      sb.append("; rotate y");
      truncate2(sb, rY);
    }
    if (rZ2 != 0) {
      sb.append("; rotate z");
      truncate2(sb, rZ2);
    }
    sb.append(";");
    addZoomTranslationNavigationText(sb);
    return sb.toString();
  }

  static private void truncate0(SB sb, float val) {
    sb.appendC(' ');
    sb.appendI(Math.round(val));
  }

  static private void truncate2(SB sb, float val) {
    sb.appendC(' ');
    sb.appendF(Math.round(val * 100) / 100f);
  }

  /* ***************************************************************
   * Spin support
   ****************************************************************/

  void setSpinXYZ(float x, float y, float z) {
    if (!Float.isNaN(x))
      spinX = x;
    if (!Float.isNaN(y))
      spinY = y;
    if (!Float.isNaN(z))
      spinZ = z;
    if (isSpinInternal || isSpinFixed)
      clearSpin();
  }

  void setSpinFps(int value) {
    if (value <= 0)
      value = 1;
    else if (value > 50)
      value = 50;
    spinFps = value;
  }

  public void setNavXYZ(float x, float y, float z) {
    if (!Float.isNaN(x))
      navX = x;
    if (!Float.isNaN(y))
      navY = y;
    if (!Float.isNaN(z))
      navZ = z;
  }

  private void clearSpin() {
    setSpinOff();
    setNavOn(false);
    isSpinInternal = false;
    isSpinFixed = false;
    //back to the Chime defaults
  }

  public boolean spinOn;

  public boolean navOn;

  private boolean spinIsGesture;

  public void setSpinOn() {
    setSpin(null, true, Float.MAX_VALUE, null, null, null, false);
  }

  public void setSpinOff() {
    setSpin(null, false, Float.MAX_VALUE, null, null, null, false);
  }

  private void setSpin(JmolScriptEvaluator eval, boolean spinOn,
                       float endDegrees, Lst<P3> endPositions,
                       float[] dihedralList, BS bsAtoms, boolean isGesture) {
    if (navOn && spinOn)
      setNavOn(false);
    if (this.spinOn == spinOn)
      return;
    this.spinOn = spinOn;
    vwr.g.setB("_spinning", spinOn);
    if (spinOn) {
      if (spinThread == null) {
        spinThread = (JmolThread) Interface.getOption("thread.SpinThread", vwr,
            "tm");
        spinThread.setManager(this, vwr,
            new Object[] { Float.valueOf(endDegrees), endPositions,
                dihedralList, bsAtoms, isGesture ? Boolean.TRUE : null });
        spinIsGesture = isGesture;
        if ((Float.isNaN(endDegrees) || endDegrees == Float.MAX_VALUE || !vwr.g.waitForMoveTo)) {
          spinThread.start();
        } else {
          spinThread.setEval(eval);
          spinThread.run();
        }
      }
    } else if (spinThread != null) {
      spinThread.reset();
      spinThread = null;
    }
  }

  public void setNavOn(boolean navOn) {
    if (Float.isNaN(navFps))
      return;
    boolean wasOn = this.navOn;
    if (navOn && spinOn)
      setSpin(null, false, 0, null, null, null, false);
    this.navOn = navOn;
    vwr.g.setB("_navigating", navOn);
    if (!navOn)
      navInterrupt();
    if (navOn) {
      if (navX == 0 && navY == 0 && navZ == 0)
        navZ = 1;
      if (navFps == 0)
        navFps = 10;
      if (spinThread == null) {
        spinThread = (JmolThread) Interface.getOption("thread.SpinThread", vwr,
            "tm");
        spinThread.setManager(this, vwr, null);
        spinThread.start();
      }
    } else if (wasOn) {
      if (spinThread != null) {
        spinThread.interrupt();
        spinThread = null;
      }
    }
  }

  public boolean vibrationOn;
  float vibrationPeriod;
  public int vibrationPeriodMs;
  private float vibrationScale;
  private P3 vibrationT = new P3();

  // only vibrationT.x is used for vibration; modulation options not implemented
  // but they could be implemented as a "slice"

  void setVibrationScale(float scale) {
    vibrationScale = scale;
  }

  /**
   * sets the period of vibration -- period > 0: sets the period and turns
   * vibration on -- period < 0: sets the period but does not turn vibration on
   * -- period = 0: sets the period to zero and turns vibration off -- period
   * Float.NaN: uses current setting (frame change)
   * 
   * @param period
   */
  public void setVibrationPeriod(float period) {
    if (Float.isNaN(period)) {
      // NaN -- new frame check
      period = vibrationPeriod;
    } else if (period == 0) {
      vibrationPeriod = 0;
      vibrationPeriodMs = 0;
    } else {
      vibrationPeriod = Math.abs(period);
      vibrationPeriodMs = (int) (vibrationPeriod * 1000);
      if (period > 0)
        return;
      period = -period;
    }
    setVibrationOn(period > 0
        && (vwr.ms.getLastVibrationVector(vwr.am.cmi, 0) >= 0));
  }

  public void setVibrationT(float t) {
    vibrationT.x = t;
    if (vibrationScale == 0)
      vibrationScale = vwr.g.vibrationScale;
  }

  boolean isVibrationOn() {
    return vibrationOn;
  }

  private void setVibrationOn(boolean vibrationOn) {
    if (!vibrationOn) {
      if (vibrationThread != null) {
        vibrationThread.interrupt();
        vibrationThread = null;
      }
      this.vibrationOn = false;
      vibrationT.x = 0;
      return;
    }
    if (vwr.ms.mc < 1) {
      this.vibrationOn = false;
      vibrationT.x = 0;
      return;
    }
    if (vibrationThread == null) {
      vibrationThread = (JmolThread) Interface.getOption(
          "thread.VibrationThread", vwr, "tm");
      vibrationThread.setManager(this, vwr, null);
      vibrationThread.start();
    }
    this.vibrationOn = true;
  }

  private void clearVibration() {
    setVibrationOn(false);
    vibrationScale = 0;
  }

  STER stereoMode = STER.NONE;
  int[] stereoColors;
  boolean stereoDoubleDTI, stereoDoubleFull;

  void setStereoMode2(int[] twoColors) {
    stereoMode = STER.CUSTOM;
    stereoColors = twoColors;
  }

  void setStereoMode(STER stereoMode) {
    stereoColors = null;
    this.stereoMode = stereoMode;
    stereoDoubleDTI = (stereoMode == STER.DTI);
    stereoDoubleFull = (stereoMode == STER.DOUBLE);
  }

  float stereoDegrees = Float.NaN; // set in state manager
  float stereoRadians;

  void setStereoDegrees(float stereoDegrees) {
    this.stereoDegrees = stereoDegrees;
    stereoRadians = stereoDegrees * JC.radiansPerDegree;
  }

  boolean stereoFrame;

  protected final M3 matrixStereo = new M3();

  synchronized M3 getStereoRotationMatrix(boolean stereoFrame) {
    this.stereoFrame = stereoFrame;
    if (!stereoFrame)
      return matrixRotate;
    matrixTemp3.setAsYRotation(-stereoRadians);
    matrixStereo.mul2(matrixTemp3, matrixRotate);
    return matrixStereo;
  }

  /////////// rotation center ////////////

  //from Frame:

  public boolean windowCentered;

  public boolean isWindowCentered() {
    return windowCentered;
  }

  void setWindowCentered(boolean TF) {
    windowCentered = TF;
    resetNavigationPoint(true);
  }

  public float setRotationRadius(float angstroms, boolean doAll) {
    angstroms = (modelRadius = (angstroms <= 0 ? vwr.ms.calcRotationRadius(
        vwr.am.cmi, fixedRotationCenter, true) : angstroms));
    if (doAll)
      vwr.setRotationRadius(angstroms, false);
    return angstroms;
  }

  private void setRotationCenterAndRadiusXYZ(T3 newCenterOfRotation,
                                             boolean andRadius) {
    resetNavigationPoint(false);
    if (newCenterOfRotation == null) {
      setFixedRotationCenter(rotationCenterDefault);
      modelRadius = rotationRadiusDefault;
      return;
    }
    setFixedRotationCenter(newCenterOfRotation);
    if (andRadius && windowCentered)
      modelRadius = vwr.ms.calcRotationRadius(vwr.am.cmi, fixedRotationCenter, true);
  }

  void setNewRotationCenter(P3 center, boolean doScale) {
    // once we have the center, we need to optionally move it to 
    // the proper XY position and possibly scale
    if (center == null)
      center = rotationCenterDefault;
    if (windowCentered) {
      translateToPercent('x', 0);
      translateToPercent('y', 0);///CenterTo(0, 0);
      setRotationCenterAndRadiusXYZ(center, true);
      if (doScale)
        resetFitToScreen(true);
    } else {
      moveRotationCenter(center, true);
    }
  }

  // from Viewer:

  public void moveRotationCenter(P3 center, boolean toXY) {
    setRotationCenterAndRadiusXYZ(center, false);
    if (toXY)
      setRotationPointXY(fixedRotationCenter);
  }

  void setCenter() {
    setRotationCenterAndRadiusXYZ(fixedRotationCenter, true);
  }

  public void setCenterAt(int relativeTo, P3 pt) {
    P3 pt1 = P3.newP(pt);
    switch (relativeTo) {
    case T.absolute:
      break;
    case T.average:
      pt1.add(vwr.ms.getAverageAtomPoint());
      break;
    case T.boundbox:
      pt1.add(vwr.getBoundBoxCenter());
      break;
    default:
      pt1.setT(rotationCenterDefault);
      break;
    }
    setRotationCenterAndRadiusXYZ(pt1, true);
    resetFitToScreen(true);
  }

  /* ***************************************************************
   * Navigation support
   ****************************************************************/

  final P3 frameOffset = new P3();
  P3[] frameOffsets;
  public BS bsFrameOffsets;

  void setFrameOffset(int modelIndex) {
    if (frameOffsets == null || modelIndex < 0
        || modelIndex >= frameOffsets.length)
      frameOffset.set(0, 0, 0);
    else
      frameOffset.setT(frameOffsets[modelIndex]);
  }

  /////////// Allow during-rendering mouse operations ///////////

  BS bsSelectedAtoms;
  P3 ptOffset = new P3();

  void setSelectedTranslation(BS bsAtoms, char xyz, int xy) {
    this.bsSelectedAtoms = bsAtoms;
    switch (xyz) {
    case 'X':
    case 'x':
      ptOffset.x += xy;
      break;
    case 'Y':
    case 'y':
      ptOffset.y += xy;
      break;
    case 'Z':
    case 'z':
      ptOffset.z += xy;
      break;
    }
  }

  /////////////////////////// old TransfomManager11 //////////////////// 

  final public static int NAV_MODE_IGNORE = -2;
  final public static int NAV_MODE_ZOOMED = -1;
  final public static int NAV_MODE_NONE = 0;
  final public static int NAV_MODE_RESET = 1;
  final public static int NAV_MODE_NEWXY = 2;
  final public static int NAV_MODE_NEWXYZ = 3;
  final public static int NAV_MODE_NEWZ = 4;

  public int navMode = NAV_MODE_RESET;
  public float zoomFactor = Float.MAX_VALUE;

  public float navigationSlabOffset;

  protected void setNavFps(int navFps) {
    this.navFps = navFps;
  }

  /**
   * sets all camera and scale factors needed by the specific perspective model
   * instantiated
   * 
   */
  public void calcCameraFactors() {
    // (m) model coordinates
    // (s) screen coordinates = (m) * screenPixelsPerAngstrom
    // (p) plane coordinates = (s) / screenPixelCount

    if (Float.isNaN(cameraDepth)) {
      cameraDepth = cameraDepthSetting;
      zoomFactor = Float.MAX_VALUE;
    }

    // reference point where p=0
    cameraDistance = cameraDepth * screenPixelCount; // (s)

    // distance from camera to midPlane of model (p=0.5)
    // the factor to apply based on screen Z
    referencePlaneOffset = cameraDistance + screenPixelCount / 2f; // (s)

    // conversion factor Angstroms --> pixels
    // so that "full window" is visualRange
    scalePixelsPerAngstrom = (scale3D && !perspectiveDepth
        && mode != MODE_NAVIGATION ? 72 / scale3DAngstromsPerInch
        * (antialias ? 2 : 1) : screenPixelCount / visualRangeAngstroms); // (s/m)

    if (mode != MODE_NAVIGATION)
      mode = (camera.z == 0 ? MODE_STANDARD : MODE_PERSPECTIVE_PYMOL);
    // still not 100% certain why we have to do this, but H115W.PinM.PSE requires it
    perspectiveShiftXY.set(camera.z == 0 ? 0 : camera.x
        * scalePixelsPerAngstrom / screenWidth * 100, camera.z == 0 ? 0
        : camera.y * scalePixelsPerAngstrom / screenHeight * 100, 0);

    // model radius in pixels
    modelRadiusPixels = modelRadius * scalePixelsPerAngstrom; // (s)


    // model center offset for zoom 100
    float offset100 = (2 * modelRadius) / visualRangeAngstroms * referencePlaneOffset; // (s)

    //    System.out.println("sppA " + scalePixelsPerAngstrom + " pD " +
    //     perspectiveDepth + " s3dspi " + scale3DAngstromsPerInch + " " 
    //     + " spC " + screenPixelCount + " vR " + visualRange
    //     + " sDPPA " + scaleDefaultPixelsPerAngstrom);

    if (mode == MODE_NAVIGATION) {
      calcNavCameraFactors(offset100);
      return;
    }
    // nonNavigation mode -- to match Jmol 10.2 at midplane (caffeine.xyz)
    // flag that we have left navigation mode
    zoomFactor = Float.MAX_VALUE;
    // we place the model at the referencePlaneOffset offset and then change
    // the scale
    modelCenterOffset = referencePlaneOffset;
    // now factor the scale by distance from camera and zoom
    if (!scale3D || perspectiveDepth)
      scalePixelsPerAngstrom *= (modelCenterOffset / offset100) * zmPct / 100; // (s/m)

    
    // so that's sppa = (spc / vR) * rPO * (vR / 2) / mR * rPO = spc/2/mR

    modelRadiusPixels = modelRadius * scalePixelsPerAngstrom; // (s)

    //    System.out.println("transformman zoom scalppa modelrad " + zoomPercent + " " +
    //     scalePixelsPerAngstrom + " " + modelRadiusPixels + " " + visualRange 
    //     + " -- "+ vwr.dimScreen.width+ "  "+ vwr.dimScreen.height);
    //    System.out.println("modelCenterOffset " + modelCenterOffset + " " + modelRadius);
  }

  private void calcNavCameraFactors(float offset100) {
    if (zoomFactor == Float.MAX_VALUE) {
      // entry point
      if (zmPct > MAXIMUM_ZOOM_PERSPECTIVE_DEPTH)
        zmPct = MAXIMUM_ZOOM_PERSPECTIVE_DEPTH;
      // screen offset to fixed rotation center
      modelCenterOffset = offset100 * 100 / zmPct;
    } else if (prevZoomSetting != zmPctSet) {
      if (zoomRatio == 0) // scripted change zoom xxx
        modelCenterOffset = offset100 * 100 / zmPctSet;
      else
        // fractional change by script or mouse
        modelCenterOffset += (1 - zoomRatio) * referencePlaneOffset;
      navMode = NAV_MODE_ZOOMED;
    }
    prevZoomSetting = zmPctSet;
    zoomFactor = modelCenterOffset / referencePlaneOffset;
    // infinite or negative value means there is no corresponding non-navigating
    // zoom setting
    zmPct = (zoomFactor == 0 ? MAXIMUM_ZOOM_PERSPECTIVE_DEPTH : offset100
        / modelCenterOffset * 100);

  }

  /**
   * calculate the perspective factor based on z
   * 
   * @param z
   * @return perspectiveFactor
   */
  public float getPerspectiveFactor(float z) {
    return (z <= 0 ? referencePlaneOffset : referencePlaneOffset / z);
  }

  public void unTransformPoint(T3 screenPt, T3 coordPt) {
    // mostly for exporters and navigation mode
    // but also for translate selected, assign atom, 
    // 
    untransformedPoint.setT(screenPt);
    switch (mode) {
    case MODE_NAVIGATION:
      untransformedPoint.x -= navigationOffset.x;
      untransformedPoint.y -= navigationOffset.y;
      break;
    case MODE_PERSPECTIVE_PYMOL:
      fScrPt.x += perspectiveShiftXY.x;
      fScrPt.y += perspectiveShiftXY.y;
      //$FALL-THROUGH$
    case MODE_STANDARD:
      untransformedPoint.x -= fixedRotationOffset.x;
      untransformedPoint.y -= fixedRotationOffset.y;
    }
    if (perspectiveDepth) {
      float factor = getPerspectiveFactor(untransformedPoint.z);
      untransformedPoint.x /= factor;
      untransformedPoint.y /= factor;
    }
    switch (mode) {
    case MODE_NAVIGATION:
      untransformedPoint.x += navigationShiftXY.x;
      untransformedPoint.y += navigationShiftXY.y;
      break;
    case MODE_PERSPECTIVE_PYMOL:
      untransformedPoint.x -= perspectiveShiftXY.x;
      untransformedPoint.y -= perspectiveShiftXY.y;
      break;
    }
    matrixTransformInv.rotTrans2(untransformedPoint, coordPt);
  }

//  boolean canNavigate() {
//    return true;
//  }

  /**
   * something has arisen that requires resetting of the navigation point.
   * 
   * @param doResetSlab
   */
  protected void resetNavigationPoint(boolean doResetSlab) {
    if (zmPct < 5 && mode != MODE_NAVIGATION) {
      perspectiveDepth = true;
      mode = MODE_NAVIGATION;
      return;
    }
    if (mode == MODE_NAVIGATION) {
      navMode = NAV_MODE_RESET;
      slabPercentSetting = 0;
      perspectiveDepth = true;
    } else if (doResetSlab) {
      slabPercentSetting = 100;
    }
    vwr.setFloatProperty("slabRange", 0);
    if (doResetSlab) {
      setSlabEnabled(mode == MODE_NAVIGATION);
    }
    zoomFactor = Float.MAX_VALUE;
    zmPctSet = zmPct;
  }

  /**
   * scripted entry point for navigation
   * 
   * @param pt
   */
  public void setNavigatePt(P3 pt) {
    // from MoveToThread
    navigationCenter.setT(pt);
    navMode = NAV_MODE_NEWXYZ;
    navigating = true;
    finalizeTransformParameters();
    navigating = false;
  }

  void setNavigationSlabOffsetPercent(float percent) {
    vwr.g.setF("navigationSlab", percent);
    calcCameraFactors(); // current
    navigationSlabOffset = percent / 50 * modelRadiusPixels;
  }

  public P3 getNavigationOffset() {
    transformPt3f(navigationCenter, navigationOffset);
    return navigationOffset;
  }

  public float getNavPtHeight() {
    //boolean navigateSurface = vwr.getNavigateSurface();
    return height / 2f;//(navigateSurface ? 1f : 2f);
  }

  public float getNavigationOffsetPercent(char XorY) {
    getNavigationOffset();
    if (width == 0 || height == 0)
      return 0;
    return (XorY == 'X' ? (navigationOffset.x - width / 2f) * 100f / width
        : (navigationOffset.y - getNavPtHeight()) * 100f / height);
  }

  protected String getNavigationText(boolean addComments) {
    String s = (addComments ? " /* navigation center, translation, depth */ "
        : " ");
    if (mode != MODE_NAVIGATION)
      return s + "{0 0 0} 0 0 0";
    getNavigationOffset();
    return s + Escape.eP(navigationCenter) + " "
        + getNavigationOffsetPercent('X') + " "
        + getNavigationOffsetPercent('Y') + " " + navigationDepthPercent;
  }

  void setScreenParameters(int screenWidth, int screenHeight,
                           boolean useZoomLarge, boolean antialias,
                           boolean resetSlab, boolean resetZoom) {
    P3 pt = (mode == MODE_NAVIGATION ? P3.newP(navigationCenter) : null);
    P3 ptoff = P3.newP(navigationOffset);
    ptoff.x = ptoff.x / width;
    ptoff.y = ptoff.y / height;
    setScreenParameters0(screenWidth, screenHeight, useZoomLarge, antialias,
        resetSlab, resetZoom);
    if (pt != null) {
      navigationCenter.setT(pt);
      navTranslatePercentOrTo(-1, ptoff.x * width, ptoff.y * height);
      setNavigatePt(pt);
    }
  }

  //////////////  optional navigation support ///////////////////////

  private JmolNavigatorInterface nav;

  private void navInterrupt() {
    if (nav != null)
      nav.interrupt();
  }

  private boolean getNav() {
    if (nav != null)
      return true;
    nav = (JmolNavigatorInterface) Interface.getOption("navigate.Navigator",
        vwr, "tm");
    if (nav == null)
      return false;
    nav.set(this, vwr);
    return true;
  }

  public void navigateList(JmolScriptEvaluator eval, Lst<Object[]> list) {
    if (getNav())
      nav.navigateList(eval, list);
  }

  /**
   * scripted entry point for navigation
   * 
   * @param rotAxis
   * @param degrees
   */
  public void navigateAxis(V3 rotAxis, float degrees) {
    if (getNav())
      nav.navigateAxis(rotAxis, degrees);
  }

  public void setNavigationOffsetRelative() {//boolean navigatingSurface) {
    if (getNav())
      nav.setNavigationOffsetRelative();//navigatingSurface);
  }

  /**
   * entry point for keyboard-based navigation
   * 
   * @param keyCode
   *        0 indicates key released
   * @param modifiers
   *        shift,alt,ctrl
   */
  synchronized void navigateKey(int keyCode, int modifiers) {
    if (getNav())
      nav.navigateKey(keyCode, modifiers);
  }

  /**
   * sets the position of the navigation offset relative to the model (50%
   * center; 0% rear, 100% front; can be <0 or >100)
   * 
   * @param percent
   */
  public void setNavigationDepthPercent(float percent) {
    if (getNav())
      nav.setNavigationDepthPercent(percent);
  }

  /**
   * seconds < 0 means "to (x,y)"; >= 0 mean "to (x%, y%)"
   * 
   * @param seconds
   * @param x
   * @param y
   */
  public void navTranslatePercentOrTo(float seconds, float x, float y) {
    if (getNav())
      nav.navTranslatePercentOrTo(seconds, x, y);
  }

  /**
   * All the magic happens here. all navigation effects go through this method
   * 
   */
  protected void calcNavigationPoint() {
    if (getNav())
      nav.calcNavigationPoint();
  }

  /**
   * 
   * @return the script that defines the current navigation state
   * 
   */
  protected String getNavigationState() {
    return (mode == MODE_NAVIGATION && getNav() ? nav.getNavigationState() : "");
  }

}
