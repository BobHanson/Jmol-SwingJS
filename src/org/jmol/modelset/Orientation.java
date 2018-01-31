package org.jmol.modelset;

import javajs.util.M3;
import javajs.util.P3;
import javajs.util.PT;

import org.jmol.script.T;
import org.jmol.util.Escape;
import org.jmol.viewer.Viewer;

public class Orientation {

  public String saveName;

  private M3 rotationMatrix = new M3();
  private float xTrans, yTrans;
  private float zoom, rotationRadius;
  private P3 center = new P3();
  private P3 navCenter = new P3();
  private float xNav = Float.NaN;
  private float yNav = Float.NaN;
  private float navDepth = Float.NaN;
  private float cameraDepth = Float.NaN;
  private float cameraX = Float.NaN;
  private float cameraY = Float.NaN;
  private boolean windowCenteredFlag;
  private boolean navigationMode;
  //boolean navigateSurface;
  private String moveToText;

  private float[] pymolView;

  private Viewer vwr;
  
  public Orientation(Viewer vwr, boolean asDefault, float[] pymolView) {
    this.vwr = vwr;
    if (pymolView != null) {
      this.pymolView = pymolView;
      moveToText = "moveTo -1.0 PyMOL " + Escape.eAF(pymolView);
      return;
    }
    vwr.finalizeTransformParameters();
    if (asDefault) {
      M3 rot = (M3) vwr.ms.getInfoM("defaultOrientationMatrix");
      if (rot == null)
        rotationMatrix.setScale(1);
      else
        rotationMatrix.setM3(rot);
    } else {
      vwr.tm.getRotation(rotationMatrix);
    }
    xTrans = vwr.tm.getTranslationXPercent();
    yTrans = vwr.tm.getTranslationYPercent();
    zoom = vwr.tm.getZoomSetting();
    center.setT(vwr.tm.fixedRotationCenter);
    windowCenteredFlag = vwr.tm.isWindowCentered();
    rotationRadius = vwr.getFloat(T.rotationradius);
    navigationMode = vwr.getBoolean(T.navigationmode);
    //navigateSurface = vwr.getNavigateSurface();
    moveToText = vwr.tm.getMoveToText(-1, false);
    if (navigationMode) {
      xNav = vwr.tm.getNavigationOffsetPercent('X');
      yNav = vwr.tm.getNavigationOffsetPercent('Y');
      navDepth = vwr.tm.navigationDepthPercent;
      navCenter = P3.newP(vwr.tm.navigationCenter);
    }
    if (vwr.tm.camera.z != 0) { // PyMOL mode
      cameraDepth = vwr.tm.getCameraDepth();
      cameraX = vwr.tm.camera.x;
      cameraY = vwr.tm.camera.y;
    }
  }

  public String getMoveToText(boolean asCommand) {
    return (asCommand ? "   " + moveToText + "\n  save orientation " 
        + PT.esc(saveName.substring(12)) + ";\n" : moveToText);
  }
  
  public boolean restore(float timeSeconds, boolean isAll) {
    if (isAll) {
      vwr.setBooleanProperty("windowCentered", windowCenteredFlag);
      vwr.setBooleanProperty("navigationMode", navigationMode);
      //vwr.setBooleanProperty("navigateSurface", navigateSurface);
      if (pymolView == null)
        vwr.moveTo(vwr.eval, timeSeconds, center, null, Float.NaN, rotationMatrix, zoom, xTrans,
            yTrans, rotationRadius, navCenter, xNav, yNav, navDepth, cameraDepth, cameraX, cameraY);
      else
        vwr.tm.moveToPyMOL(vwr.eval, timeSeconds, pymolView);
    } else {
      vwr.tm.setRotation(rotationMatrix);
    }
    return true;
  }
}