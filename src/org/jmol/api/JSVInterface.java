package org.jmol.api;

import java.util.Map;
import java.util.Properties;

/**
 * and interface for JSpecView for the Jmol application
 * 
 */
public interface JSVInterface {
  
  void exitJSpecView(boolean withDialog, Object frame);
  void runScript(String script);
  void saveProperties(Properties properties);
  void setProperties(Properties properties);  
  void syncToJmol(String msg);
  Map<String, Object> getJSpecViewProperty(String key);

}
