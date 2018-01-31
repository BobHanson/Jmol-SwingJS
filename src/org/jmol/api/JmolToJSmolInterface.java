package org.jmol.api;

import java.util.Map;

import javajs.api.GenericImageDialog;
import javajs.api.JmolObjectInterface;

import org.jmol.awtjs2d.Platform;
import org.jmol.viewer.Viewer;

/**
 * methods in JSmol JavaScript accessed in Jmol 
 */
public interface JmolToJSmolInterface extends JmolObjectInterface {


  // JSmol.js

  Object _newGrayScaleImage(Object context, Object image, int width,
                            int height, int[] grayBuffer);

  void _repaint(Object html5Applet, boolean asNewThread);

  void _setCanvasImage(Object canvas, int width, int height);

  void _setCursor(Object html5Applet, int c);
  
  Object _getHiddenCanvas(Object html5Applet, String string, int w, int h);

  Object _loadImage(Platform platform, String echoName, String path,
                    byte[] bytes, Object f);

  Object _loadFileAsynchronously(Object fileLoadThread, Object html5Applet, String fileName, Object appData);

  // JSmolConsole.js 
  GenericImageDialog _consoleGetImageDialog(Viewer vwr,
                                            String title,
                                            Map<String, GenericImageDialog>imageMap);

  // JSmolApi.js
  void resizeApplet(Object html5Applet, int[] dims);

  // JmolCore.js
  boolean _isBinaryUrl(String filename);

  

}
