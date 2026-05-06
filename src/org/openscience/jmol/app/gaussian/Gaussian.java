package org.openscience.jmol.app.gaussian;

import javax.swing.JFrame;

import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.plugins.GaussianI;

public class Gaussian implements GaussianI {

  GaussianDialog dialog;
  
  public Gaussian() {
    // for dynamic loading
  }
  
  @Override
  public void newDialog(JFrame frame, Viewer vwr) {
    dialog = new GaussianDialog(frame, vwr);
  }  

  @Override
  public boolean isVisible() {
    return dialog != null && dialog.isVisible();
  }

  @Override
  public void setVisible(boolean b) {
    if (dialog != null)
      dialog.setVisible(b);
  }

  @Override
  public void updateModel(int i) {
    if (dialog != null)
      dialog.updateModel(i);
  }

  @Override
  public void close() {
    if (dialog != null)
      dialog.dispose();
    dialog = null;
  }

}
