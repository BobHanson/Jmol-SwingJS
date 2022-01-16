package org.jmol.api;

import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.OC;

public interface JmolWriter {

  public void set(Viewer vwr, OC out, Object[] data);
  public String write(BS bs);

}
