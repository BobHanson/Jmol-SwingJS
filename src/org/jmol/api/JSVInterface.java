package org.jmol.api;

import java.util.Properties;

/**
 * and interface for JSpecView for the Jmol application
 * 
 */
public interface JSVInterface {
  
  public void exitJSpecView(boolean withDialog, Object frame);
  public void runScript(String script);
  public void saveProperties(Properties properties);
  public void setProperties(Properties properties);  
  public void syncToJmol(String msg);

}
