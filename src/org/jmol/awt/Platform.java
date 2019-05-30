package org.jmol.awt;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.jmol.awtjs.swing.Font;

import javajs.util.P3;
import javajs.util.Rdr;

import javax.swing.JDialog;

import org.jmol.api.GenericFileInterface;
import org.jmol.api.GenericImageDialog;
import org.jmol.api.GenericMenuInterface;
import org.jmol.api.GenericMouseInterface;
import org.jmol.api.GenericPlatform;
import org.jmol.api.Interface;
import org.jmol.api.PlatformViewer;
import org.jmol.viewer.Viewer;

public class Platform implements GenericPlatform {

  PlatformViewer vwr;
  
  @Override
  public void setViewer(PlatformViewer vwr, Object display) {
    this.vwr = vwr;
  }
  
  ///// Display 

  @Override
  public void convertPointFromScreen(Object display, P3 ptTemp) {
    Display.convertPointFromScreen(display, ptTemp);
  }

  @Override
  public void getFullScreenDimensions(Object display, int[] widthHeight) {
    Display.getFullScreenDimensions(display, widthHeight);        
  }
  
  @Override
  public GenericMenuInterface getMenuPopup(String menuStructure, char type) {
    GenericMenuInterface jmolpopup = (GenericMenuInterface) Interface.getOption(
        type == 'j' ? "awt.AwtJmolPopup" : "awt.AwtModelKitPopup", null, null);
    if (jmolpopup != null)
      jmolpopup.jpiInitialize(vwr, menuStructure);
    return jmolpopup;
  }

  @Override
  public boolean hasFocus(Object display) {
    return Display.hasFocus(display);
  }

  @Override
  public String prompt(String label, String data, String[] list,
                       boolean asButtons) {
    return Display.prompt(label, data, list, asButtons);
  }

  /**
   * legacy apps will use this
   * 
   * @param g
   * @param size
   */
  @SuppressWarnings("deprecation")
  @Override
  public void renderScreenImage(Object g, Object size) {
    Display.renderScreenImage(vwr, g, size);
  }

  @Override
  public void requestFocusInWindow(Object display) {
    Display.requestFocusInWindow(display);
  }

  @Override
  public void repaint(Object display) {
    Display.repaint(display);
  }

  @Override
  public void setTransparentCursor(Object display) {
    Display.setTransparentCursor(display);
  }

  @Override
  public void setCursor(int c, Object display) {
    Display.setCursor(c, display);
    if (c == CURSOR_HAND)
      ((Component) display).requestFocus();
  }

  ////// Mouse

  @Override
  public GenericMouseInterface getMouseManager(double privateKey, Object display) {
    return new Mouse(privateKey, vwr, display);
  }

  ////// Image 

  @Override
  public Object allocateRgbImage(int windowWidth, int windowHeight,
                                 int[] pBuffer, int windowSize,
                                 boolean backgroundTransparent, boolean isImageWrite) {
    return Image.allocateRgbImage(windowWidth, windowHeight, pBuffer, windowSize, backgroundTransparent);
  }

  /**
   * could be byte[] (from ZIP file) or String (local file name) or URL
   * @param data 
   * @return image object
   * 
   */
  @Override
  public Object createImage(Object data) {
    return Image.createImage(data, vwr);
  }

  @Override
  public void disposeGraphics(Object gOffscreen) {
    Image.disposeGraphics(gOffscreen);
  }

  @Override
  public void drawImage(Object g, Object img, int x, int y, int width, int height, boolean isDTI) {
    if (isDTI)
      Display.drawImageDTI(g, img, x, y, width, height);
    else
      Display.drawImage(g, img, x, y, width, height);
  }

  @Override
  public int[] grabPixels(Object imageobj, int width, int height, int[] pixels, int startRow, int nRows) {
    return Image.grabPixels(imageobj, width, height, pixels, startRow, nRows); 
  }

  @Override
  public int[] drawImageToBuffer(Object gOffscreen, Object imageOffscreen,
                                 Object imageobj, int width, int height, int bgcolor) {
    return Image.drawImageToBuffer(gOffscreen, imageOffscreen, imageobj, width, height, bgcolor);
  }

  @Override
  public int[] getTextPixels(String text, Font font3d, Object gObj,
                             Object image, int width, int height, int ascent) {
    return Image.getTextPixels(text, font3d, gObj, image, width, height, ascent);
  }

  @Override
  public void flushImage(Object imagePixelBuffer) {
    Image.flush(imagePixelBuffer);
  }

  @Override
  public Object getGraphics(Object image) {
    return Image.getGraphics(image);
  }

  @Override
  public int getImageHeight(Object image) {
    return (image == null ? -1 : Image.getHeight(image));
  }

  @Override
  public int getImageWidth(Object image) {
    return (image == null ? -1 : Image.getWidth(image));
  }

  @Override
  public Object getStaticGraphics(Object image, boolean backgroundTransparent) {
    return Image.getStaticGraphics(image, backgroundTransparent);
  }

  @Override
  public Object newBufferedImage(Object image, int w, int h) {
    return Image.newBufferedImage(image, w, h);
  }

  @Override
  public Object newOffScreenImage(int w, int h) {
    return Image.newBufferedImage(w, h);
  }

  @Override
  public boolean waitForDisplay(Object ignored, Object image) throws InterruptedException {
    Image.waitForDisplay(vwr, image);
    return true;
  }

  
  ///// FONT
  
  @Override
  public int fontStringWidth(Font font, String text) {
    return AwtFont.stringWidth(font.getFontMetrics(), text);
  }

  @Override
  public int getFontAscent(Object fontMetrics) {
    return AwtFont.getAscent(fontMetrics);
  }

  @Override
  public int getFontDescent(Object fontMetrics) {
    return AwtFont.getDescent(fontMetrics);
  }

  @Override
  public Object getFontMetrics(Font font, Object graphics) {
    return AwtFont.getFontMetrics(font, graphics);
  }

  @Override
  public Object newFont(String fontFace, boolean isBold, boolean isItalic, float fontSize) {
    return AwtFont.newFont(fontFace, isBold, isItalic, fontSize);
  }

  /// misc

  @Override
  public Object getJsObjectInfo(Object[] jsObject, String method, Object[] args) {
    Object[] obj = new Object[] {null, method, jsObject, args};
   ((Viewer) vwr).sm.cbl.notifyCallback(null, obj);
    return obj[0];
  }

  @Override
  public boolean isHeadless() {
    return GraphicsEnvironment.isHeadless();
  }

  @Override
  public boolean isSingleThreaded() {
    return  Viewer.isSwingJS;
  }

  @Override
  public void notifyEndOfRendering() {
    // N/A
  }

  /**
   * @param p 
   * @return The hosting frame or JDialog.
   */
  static public Window getWindow(Container p) {
    while (p != null) {
      if (p instanceof Frame)
        return (Frame) p;
      else if (p instanceof JDialog)
        return (JDialog) p;
      else if (p instanceof JmolFrame)
        return ((JmolFrame) p).getFrame();
      p = p.getParent();
    }
    return null;
  }

  @Override
  public String getDateFormat(String isoType) {
    if (isoType == null) {
      isoType = "EEE, d MMM yyyy HH:mm:ss Z";
    } else if (isoType.contains("8824")) {
      return "D:" + new SimpleDateFormat("YYYYMMddHHmmssX").format(new Date())
          + "'00'";
    } else if (isoType.contains("8601")) {
      return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date());
    }
    return new SimpleDateFormat(isoType).format(new Date());
  }
//  @Override
//  public String getDateFormat(boolean isoiec8824) {
//    return (isoiec8824 ? "D:"
//        + new SimpleDateFormat("YYYYMMddHHmmssX").format(new Date()) + "'00'"
//        : (new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z"))
//            .format(new Date()));
//  }

  @Override
  public GenericFileInterface newFile(String name) {
    return new AwtFile(name);
  }

  @Override
  public Object getBufferedFileInputStream(String name) {
    return AwtFile.getBufferedFileInputStream(name);
  }

  @Override
  public Object getURLContents(URL url, byte[] outputBytes, String post,
      boolean asString) {
    Object ret = AwtFile.getURLContents(url, outputBytes, post);
    try {
      return (!asString ? ret : ret instanceof String ? ret : new String(
          (byte[]) Rdr.getStreamAsBytes((BufferedInputStream) ret, null)));
    } catch (IOException e) {
      return "" + e;
    }
  }


  @Override
  public String getLocalUrl(String fileName) {
    return AwtFile.getLocalUrl(newFile(fileName));
  }

  @Override
  public GenericImageDialog getImageDialog(String title,
                                        Map<String, GenericImageDialog> imageMap) {
    return Image.getImageDialog(vwr, title, imageMap);
  }

  @Override
  public boolean forceAsyncLoad(String filename) {
    return false;
  }

    
}
