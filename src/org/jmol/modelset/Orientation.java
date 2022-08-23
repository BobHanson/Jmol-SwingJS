package org.jmol.modelset;

import javajs.util.M3d;
import javajs.util.P3d;
import javajs.util.PT;

import org.jmol.script.T;
import org.jmol.util.Escape;
import org.jmol.viewer.Viewer;

public class Orientation {

  public String saveName;

  private M3d rotationMatrix = new M3d();
  private double xTrans, yTrans;
  private double zoom, rotationRadius;
  private P3d center = new P3d();
  private P3d navCenter = new P3d();
  private double xNav = Double.NaN;
  private double yNav = Double.NaN;
  private double navDepth = Double.NaN;
  private double cameraDepth = Double.NaN;
  private double cameraX = Double.NaN;
  private double cameraY = Double.NaN;
  private boolean windowCenteredFlag;
  private boolean navigationMode;
  //boolean navigateSurface;
  private String moveToText;

  private double[] pymolView;

  private Viewer vwr;
  
  public Orientation(Viewer vwr, boolean asDefault, double[] pymolView) {
    this.vwr = vwr;
    if (pymolView != null) {
      this.pymolView = pymolView;
      moveToText = "moveTo -1.0 PyMOL " + Escape.eAD(pymolView);
      return;
    }
    vwr.finalizeTransformParameters();
    if (asDefault) {
      M3d rot = (M3d) vwr.ms.getInfoM("defaultOrientationMatrix");
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
    rotationRadius = vwr.getDouble(T.rotationradius);
    navigationMode = vwr.getBoolean(T.navigationmode);
    //navigateSurface = vwr.getNavigateSurface();
    moveToText = vwr.tm.getMoveToText(-1, false);
    if (navigationMode) {
      xNav = vwr.tm.getNavigationOffsetPercent('X');
      yNav = vwr.tm.getNavigationOffsetPercent('Y');
      navDepth = vwr.tm.navigationDepthPercent;
      navCenter = P3d.newP(vwr.tm.navigationCenter);
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
  
  public boolean restore(double timeSeconds, boolean isAll) {
    if (isAll) {
      vwr.setBooleanProperty("windowCentered", windowCenteredFlag);
      vwr.setBooleanProperty("navigationMode", navigationMode);
      //vwr.setBooleanProperty("navigateSurface", navigateSurface);
      if (pymolView == null)
        vwr.moveTo(vwr.eval, timeSeconds, center, null, Double.NaN, rotationMatrix, zoom, xTrans,
            yTrans, rotationRadius, navCenter, xNav, yNav, navDepth, cameraDepth, cameraX, cameraY);
      else
        vwr.tm.moveToPyMOL(vwr.eval, timeSeconds, pymolView);
    } else {
      vwr.tm.setRotation(rotationMatrix);
    }
    return true;
  }
}