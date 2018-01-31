package org.jmol.api;

import java.util.Map;

import org.jmol.modelset.ModelSet;
import org.jmol.util.GData;
import org.jmol.viewer.ShapeManager;
import org.jmol.viewer.Viewer;

public interface JmolRepaintManager {

  void set(Viewer vwr, ShapeManager shapeManager);

  boolean isRepaintPending();

  void popHoldRepaint(boolean andRepaint, String why);

  boolean repaintIfReady(String why);

  void pushHoldRepaint(String why);

  void repaintDone();

  void requestRepaintAndWait(String why);

  void clear(int iShape);

  void render(GData gdata, ModelSet modelSet, boolean isFirstPass, int[] navMinMax);

  String renderExport(GData gdata, ModelSet modelSet, Map<String, Object> params);

}
