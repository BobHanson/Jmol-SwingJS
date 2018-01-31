package org.jmol.api;

import org.jmol.viewer.Viewer;

public interface JmolAppConsoleInterface {

  public void setVisible(boolean b);

  public void sendConsoleEcho(String strEcho);

  public void sendConsoleMessage(String strInfo);

  public JmolScriptEditorInterface getScriptEditor();

  public void start(Viewer vwr);

  public void zap();

  public void dispose();

  public String getText();

  public Object newJMenu(String key);

  public Object newJMenuItem(String key);


}
