package org.openscience.jmol.app;

import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.jmol.c.CBK;
import org.jmol.viewer.Viewer;

public interface JmolPlugin { 
  void start(JFrame frame, Viewer vwr, Map<String, Object> jmolOptions);
  void destroy();
  String getVersion();
  String getName();
  void setVisible(boolean b);
  void notifyCallback(CBK type, Object[] data);
  ImageIcon getMenuIcon();
  String getMenuText();
  boolean isStarted();
  String getWebSite();

}
