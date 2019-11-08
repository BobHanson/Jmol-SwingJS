package org.jmol.api.js;

import java.util.Map;

import javajs.api.js.J2SObjectInterface;

import org.jmol.api.GenericImageDialog;
import org.jmol.api.GenericPlatform;
import org.jmol.viewer.Viewer;

/**
 * methods in JSmol JavaScript accessed in Jmol 
 */
public interface JmolToJSmolInterface extends J2SObjectInterface {
  
  // JSmol.js; J2SApplet.js
  
  Object loadImage(GenericPlatform platform, String echoName, String path,
                   byte[] bytes, Object f);

  // JSmolConsole.js 
  GenericImageDialog consoleGetImageDialog(Viewer vwr,
                                            String title,
                                            Map<String, GenericImageDialog>imageMap);
  
  // JSmolApi.js

  // needs to be moved from JSmolCore.js to JSmolApi.js
  void setCursor(JSmolAppletObject html5Applet, int c);
  
  void playAudio(JSmolAppletObject applet, Map<String, Object> htParams);


  

}
