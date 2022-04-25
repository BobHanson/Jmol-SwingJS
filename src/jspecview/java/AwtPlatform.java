package jspecview.java;

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

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.jmol.api.GenericFileInterface;
import org.jmol.api.GenericImageDialog;
import org.jmol.api.GenericMenuInterface;
import org.jmol.api.GenericMouseInterface;
import org.jmol.api.GenericPlatform;
import org.jmol.api.PlatformViewer;
import org.jmol.i18n.GT;
import org.jmol.util.Font;

import javajs.util.P3d;
import javajs.util.Rdr;
import jspecview.api.JSVPanel;


//////import netscape.javascript.JSObject;


public class AwtPlatform implements GenericPlatform {

  PlatformViewer vwr;

  @Override
  public void setViewer(PlatformViewer viewer, Object display) {
    this.vwr = viewer;
  }

  ///// Display 

  @Override
  public void convertPointFromScreen(Object display, P3d ptTemp) {
    Display.convertPointFromScreen(display, ptTemp);
  }

  @Override
  public void getFullScreenDimensions(Object display, int[] widthHeight) {
    Display.getFullScreenDimensions(display, widthHeight);
  }

  @Override
  public GenericMenuInterface getMenuPopup(String menuStructure, char type) {
    return null;//
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

//  /**
//   * legacy apps will use this
//   * 
//   * @param g
//   * @param size
//   */
//  @Override
//  public void renderScreenImage(Object g, Object size) {
//    Display.renderScreenImage(vwr, g, size);
//  }

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
  }

  ////// Mouse

  @Override
  public GenericMouseInterface getMouseManager(double ignored, Object jsvp) {
    return new Mouse((JSVPanel) jsvp);
  }

  ////// Image 

  @Override
  public Object allocateRgbImage(int windowWidth, int windowHeight,
                                 int[] pBuffer, int windowSize,
                                 boolean backgroundTransparent,
                                 boolean isImageWrite) {
    return Image.allocateRgbImage(windowWidth, windowHeight, pBuffer,
        windowSize, backgroundTransparent);
  }

  /**
   * could be byte[] (from ZIP file) or String (local file name) or URL
   * 
   * @param data
   * @return image object
   * 
   */
  @Override
  public Object createImage(Object data) {
    return Image.createImage(data);
  }

  @Override
  public void disposeGraphics(Object gOffscreen) {
    Image.disposeGraphics(gOffscreen);
  }

  @Override
  public void drawImage(Object g, Object img, int x, int y, int width,
                        int height, boolean isDTI) {
    Image.drawImage(g, img, x, y, width, height);
  }

  @Override
  public int[] grabPixels(Object imageobj, int width, int height, int[] pixels) {
    return Image.grabPixels(imageobj, width, height, pixels);
  }

  @Override
  public int[] drawImageToBuffer(Object gOffscreen, Object imageOffscreen,
                                 Object imageobj, int width, int height,
                                 int bgcolor) {
    return Image.drawImageToBuffer(gOffscreen, imageOffscreen, imageobj, width,
        height, bgcolor);
  }

  @Override
  public int[] getTextPixels(String text, Font font3d, Object gObj,
                             Object image, int width, int height, int ascent) {
    return Image.getTextPixels(text, font3d, gObj, image, width, height,
        ascent);
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
  public boolean waitForDisplay(Object ignored, Object image)
      throws InterruptedException {
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
  public Object newFont(String fontFace, boolean isBold, boolean isItalic,
                        float fontSize) {
    return AwtFont.newFont(fontFace, isBold, isItalic, fontSize);
  }

  /// misc

  @Override
  public Object getJsObjectInfo(Object[] jsObject, String method,
                                Object[] args) {
//    JSObject DOMNode = (JSObject) jsObject[0];
//    if (method == null) {
//      String namespaceURI = (String) DOMNode.getMember("namespaceURI");
//      String localName = (String) DOMNode.getMember("localName");
//      return "namespaceURI=\"" + namespaceURI + "\" localName=\"" + localName
//          + "\"";
//    }
//    return (args == null ? DOMNode.getMember(method)
//        : DOMNode.call(method, args));
    return null;
  }

  @Override
  public boolean isHeadless() {
    return GraphicsEnvironment.isHeadless();
  }

  @Override
  public boolean isSingleThreaded() {
    return false;
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
      return (!asString ? ret
          : ret instanceof String ? ret
              : new String((byte[]) Rdr
                  .getStreamAsBytes((BufferedInputStream) ret, null)));
    } catch (IOException e) {
      return "" + e;
    }
  }

  @Override
  public String getLocalUrl(String fileName) {
    // not used in JSpecView
    return null;
  }

  @Override
  public GenericImageDialog getImageDialog(String title,
                                           Map<String, GenericImageDialog> imageMap) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean forceAsyncLoad(String filename) {
    return false;
  }

  @Override
  public boolean isJS() {
    return false;
  }

  @Override
  public Object getInChI() {
    // n/a for JSpecView
    return null;
  }

  @Override
  public int confirm(String msg, String msgNo) {
    int ret = JOptionPane.showConfirmDialog(null, GT.$(msg));
    switch (ret) {
    case JOptionPane.OK_OPTION:
      return ret;
    case JOptionPane.CANCEL_OPTION:
      if (/** @j2sNative false && */
      true)
        return ret;
      //$FALL-THROUGH$
    case JOptionPane.NO_OPTION:
    default:
      return (msgNo != null && JOptionPane.showConfirmDialog(null,
          GT.$(msgNo)) == JOptionPane.OK_OPTION ? JOptionPane.NO_OPTION
              : JOptionPane.CANCEL_OPTION);
    }
  }


}
