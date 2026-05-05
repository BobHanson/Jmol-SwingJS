package org.openscience.jmol.app;

import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.jmol.c.CBK;
import org.jmol.viewer.Viewer;

public interface JmolPlugin { 
  void destroy();
  String getLicense();
  ImageIcon getMenuIcon();
  String getMenuText();
  String getName();
  String getVersion();
  String getWebSite();
  boolean isStarted();
  boolean isVisible();
  void notifyCallback(CBK type, Object[] data);
  Object processRequest(String action, Object value);
  void setVisible(boolean b);
  void start(JFrame parentFrame, Viewer vwr, Map<String, Object> jmolOptions);
  
}
