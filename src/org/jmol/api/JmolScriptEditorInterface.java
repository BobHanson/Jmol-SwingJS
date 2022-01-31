package org.jmol.api;

import org.jmol.script.ScriptContext;

public interface JmolScriptEditorInterface extends JmolDropEditor {

  void setVisible(boolean b);

  void dispose();
  
  boolean isVisible();

  void notifyContext(ScriptContext property, Object[] data);

  void show(String[] fileText);

  void notify(int msWalltime, Object[] data);

}
