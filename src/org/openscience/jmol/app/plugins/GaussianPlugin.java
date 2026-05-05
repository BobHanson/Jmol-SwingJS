package org.openscience.jmol.app.plugins;

import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.jmol.c.CBK;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.JmolPlugin;
import org.openscience.jmol.app.jmolpanel.GaussianDialog;
import org.openscience.jmol.app.jmolpanel.JmolResourceHandler;

public class GaussianPlugin implements JmolPlugin {

  protected GaussianDialog gaussianDialog; // not in SwingJS

  public GaussianPlugin() {
    gaussianDialog = null;
  }
  @Override
  public void destroy() {
    gaussianDialog = null;
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
    return (gaussianDialog != null);
  }

  @Override
  public boolean isVisible() {
    return gaussianDialog != null && gaussianDialog.isVisible();
  }

  @Override
  public void setVisible(boolean b) {
    if (gaussianDialog != null)
      gaussianDialog.setVisible(b);
  }

  @Override
  public void start(JFrame frame, Viewer vwr, Map<String, Object> jmolOptions) {
    gaussianDialog = new GaussianDialog(frame, vwr);
    // TODO
    
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
      gaussianDialog.updateModel(-2);
      break;
    case PICK:
      gaussianDialog.updateModel(((Integer) data[2]).intValue());
      break;
    case STRUCTUREMODIFIED:
      gaussianDialog.updateModel(-1);
      break;
    default:
      break;
    }
  }
 
  
}