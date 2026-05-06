package org.gennbo;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.jmol.c.CBK;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.plugins.NBOInterface;

public class NBO implements NBOInterface {

  protected NBODialog dialog;
  private ActionListener plugin;

  public NBO() {
    // dynamic loading
  }

  @Override
  public void newDialog(ActionListener plugin, JFrame frame, Viewer vwr,
                        Map<String, Object> jmolOptions) {
    this.plugin = plugin;
    dialog = new NBODialog(this, frame, vwr, jmolOptions);
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
  public void close() {
    if (dialog != null)
      dialog.close();
    dialog = null;
  }

  @Override
  public void notifyCallback(CBK type, Object[] data) {
    if (dialog == null)
      return;
    dialog.notifyCallback(type, data);
  }

  private Object action(String type, Object param) {
    ActionEvent e = new ActionEvent(param, 0, type);
    plugin.actionPerformed(e);
    return e.getSource();
  }

  @Override
  public String getName() {
    return (String) action("name", "");
  }
  
  @Override
  public String getVersion() {
    return (String) action("version", "");
  }

  @Override
  public ImageIcon getIcon(String name) {
    return new ImageIcon(this.getClass().getResource("assets/" + name + ".gif"));
  }

  @Override
  public String getNBOProperty(String key, String def) {
    return (String) action("getProperty", new String[] {key, def});
  }

  @Override
  public void setNBOProperty(String key, String value) {
    action("setProperty", new String[] {key, value});
    
  }
  
  
  
}
