package org.openscience.jmol.app.plugins;

import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.jmol.c.CBK;
import org.jmol.viewer.Viewer;

public interface NBOInterface {

  String NBO_WEB_SITE = "http://nbo7.chem.wisc.edu";

  void newDialog(ActionListener nboPlugin, JFrame frame, Viewer vwr,
                 Map<String, Object> jmolOptions);
  
  boolean isVisible();

  void setVisible(boolean b);

  void close();

  void notifyCallback(CBK type, Object[] data);

  String getName();

  String getVersion();

  ImageIcon getIcon(String name);

  String getNBOProperty(String key, String def);

  void setNBOProperty(String key, String value);

}
