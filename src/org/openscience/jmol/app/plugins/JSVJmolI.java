package org.openscience.jmol.app.plugins;

import javax.swing.JFrame;

import org.jmol.c.CBK;
import org.jmol.viewer.Viewer;

public interface JSVJmolI {

  boolean isVisible();

  void notifyCallback(CBK type, Object[] data);

  void setVisible(boolean b);

  void setViewer(Viewer vwr, JFrame parentFrame);

  Object getJSpecViewProperty(String value);

}
