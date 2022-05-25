package org.jmol.api;

import org.jmol.util.Vibration;

import javajs.util.T3d;
import javajs.util.V3d;

public interface JmolModulationSet {

  /**
   * 
   * @param type
   * @param t456
   * @param occ100 true for vibration visualization
   * @return point or (for occupancy) value
   */
  Object getModulation(char type, T3d t456, boolean occ100);

  String getState();

  boolean isEnabled();

  void setModTQ(T3d a, boolean isOn, T3d qtOffset, boolean isQ, double scale);

  double getScale();

  void addTo(T3d a, double scale);

  T3d getModPoint(boolean asEnabled);

  Vibration getVibration(boolean forceNew);

  V3d getV3();

  SymmetryInterface getSubSystemUnitCell();

  void scaleVibration(double m);

  void setMoment();


}
