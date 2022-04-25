package org.jmol.api;



import javajs.util.Lst;
import javajs.util.V3d;
import org.jmol.viewer.TransformManager;
import org.jmol.viewer.Viewer;

public interface JmolNavigatorInterface extends Runnable {

  void set(TransformManager transformManager, Viewer vwr);

//  void navigateTo(double floatSecondsTotal, V3 axis, double degrees,
//                  P3 center, double depthPercent, double xTrans, double yTrans);
//
//  void navigate(double seconds, P3[][] pathGuide, P3[] path,
//                double[] theta, int indexStart, int indexEnd);

  void zoomByFactor(double factor, int x, int y);

  void calcNavigationPoint();

  void setNavigationOffsetRelative();//boolean navigatingSurface);

  void navigateKey(int keyCode, int modifiers);

  void navigateList(JmolScriptEvaluator eval, Lst<Object[]> list);

  void navigateAxis(V3d rotAxis, double degrees);

  void setNavigationDepthPercent(double percent);

  String getNavigationState();

  void navTranslatePercentOrTo(double seconds, double x, double y);

  void interrupt();


}
