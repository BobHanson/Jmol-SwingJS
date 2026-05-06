package org.openscience.jmol.app.plugins;

import java.awt.Container;
import javax.swing.JFrame;
import org.jmol.viewer.Viewer;

public interface JMEJmolI {

  boolean isFrameVisible();
  void setFrameVisible(boolean b);
  public void setViewer(JFrame frame, Viewer vwr, Container parent, String frameType, boolean headless);
  void from3D();  
}
