package org.jmol.api;

import org.jmol.util.Vibration;

import javajs.util.T3;
import javajs.util.V3;

public interface JmolModulationSet {

  Object getModulation(char type, T3 t456);

  String getState();

  boolean isEnabled();

  void setModTQ(T3 a, boolean isOn, T3 qtOffset, boolean isQ, float scale);

  float getScale();

  void addTo(T3 a, float scale);

  T3 getModPoint(boolean asEnabled);

  Vibration getVibration(boolean forceNew);

  V3 getV3();

  SymmetryInterface getSubSystemUnitCell();

  void scaleVibration(float m);

  void setMoment();

}
