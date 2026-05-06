package org.openscience.jmol.app.plugins;

import javax.swing.JFrame;

import org.jmol.viewer.Viewer;

public interface GaussianI {

  boolean isVisible();

  void setVisible(boolean b);

  void updateModel(int i);

  void newDialog(JFrame frame, Viewer vwr);

  void close();

}
