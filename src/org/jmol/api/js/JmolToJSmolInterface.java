package org.jmol.api.js;

import java.util.Map;

import javajs.api.js.J2SObjectInterface;

import org.jmol.api.GenericImageDialog;
import org.jmol.awtjs2d.Platform;
import org.jmol.viewer.Viewer;

/**
 * methods in JSmol JavaScript accessed in Jmol 
 */
public interface JmolToJSmolInterface extends J2SObjectInterface {


  // JSmolJSV.js

  Object newGrayScaleImage(Object context, Object image, int width,
                            int height, int[] grayBuffer);

  // JSmol.js

  void repaint(JSmolAppletObject html5Applet, boolean asNewThread);

  void setCanvasImage(Object canvas, int width, int height);

  Object getHiddenCanvas(JSmolAppletObject html5Applet, String string, int w, int h);

  Object loadImage(Platform platform, String echoName, String path,
                    byte[] bytes, Object f);

  Object loadFileAsynchronously(Object fileLoadThread, JSmolAppletObject html5Applet, String fileName, Object appData);

  // JSmolConsole.js 
  GenericImageDialog _consoleGetImageDialog(Viewer vwr,
                                            String title,
                                            Map<String, GenericImageDialog>imageMap);

  // JSmolApi.js
  void resizeApplet(Object html5Applet, int[] dims);

  // needs to be moved from JSmolCore.js to JSmolApi.js
  void setCursor(JSmolAppletObject html5Applet, int c);
  
  void playAudio(JSmolAppletObject applet, Map<String, Object> htParams);

  // JmolCore.js
  boolean isBinaryUrl(String filename);


  

}
