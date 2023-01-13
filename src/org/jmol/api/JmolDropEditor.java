package org.jmol.api;

public interface JmolDropEditor {

  void loadFile(String fname);
  /**
   * Deprecated. 
   * 
   * Designed to be used for dropping a script into the AppConsole or ScriptEditor
   * in order to open it in the ScriptEditor, but that was never implemented.
   * 
   *
   * @param script
   */
  @Deprecated
  void loadContent(String script);

}
