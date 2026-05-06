package org.openscience.jmol.app.plugins;

import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.jmol.api.Interface;
import org.jmol.c.CBK;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.JmolPlugin;
import org.openscience.jmol.app.jmolpanel.JmolResourceHandler;

public class GaussianPlugin implements JmolPlugin {

  protected GaussianI gaussian; // not in SwingJS

  public GaussianPlugin() {
  }

  @Override
  public void destroy() {
    gaussian = null;
  }

  @Override
  public String getLicense() {
    return null;
  }

  @Override
  public ImageIcon getMenuIcon() {
    return JmolResourceHandler.getIconX("gaussianButton.png");
  }

  @Override
  public String getMenuText() {
    return "Gaussian";
  }

  @Override
  public String getName() {
    return "Gaussian";
  }

  @Override
  public String getVersion() {
    return null;
  }

  @Override
  public String getWebSite() {
    return null;
  }

  @Override
  public boolean isStarted() {
    return (gaussian != null);
  }

  @Override
  public boolean isVisible() {
    return gaussian != null && gaussian.isVisible();
  }

  @Override
  public void setVisible(boolean b) {
    if (gaussian != null)
      gaussian.setVisible(b);
  }

  @Override
  public void start(JFrame frame, Viewer vwr, Map<String, Object> jmolOptions, boolean headless) {
 
    
    gaussian = 
        (GaussianI) Interface
        .getInterface("org.openscience.jmol.app.gaussian.Gaussian", vwr, "plugin");
    gaussian.newDialog(frame, vwr);
  }

  @Override
  public Object processRequest(String action, Object value) {
    // TODO
    return null;
  }
  
  @Override
  public void notifyCallback(CBK type, Object[] data) {
    if (!isStarted())
      return;
    switch (type) {
    case LOADSTRUCT:
      gaussian.updateModel(-2);
      break;
    case PICK:
      gaussian.updateModel(((Integer) data[2]).intValue());
      break;
    case STRUCTUREMODIFIED:
      gaussian.updateModel(-1);
      break;
    default:
      break;
    }
  }
  @Override
  public boolean isHeadless() {
    return false;
  }
  
}