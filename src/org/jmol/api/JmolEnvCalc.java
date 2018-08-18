package org.jmol.api;

import org.jmol.atomdata.AtomDataServer;
import org.jmol.atomdata.RadiusData;
import javajs.util.BS;

import javajs.util.P3;

public interface JmolEnvCalc {

  JmolEnvCalc set(AtomDataServer vwr, int ac, short[] mads);

  P3[] getPoints();

  BS getBsSurfaceClone();

  void calculate(RadiusData rd, float maxRadius, BS bsSelected,
                 BS bsIgnore, boolean disregardNeighbors,
                 boolean onlySelectedDots, boolean isSurface,
                 boolean multiModel);
}
