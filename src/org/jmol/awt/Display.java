package org.jmol.awt;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.MemoryImageSource;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import javajs.util.P3;
import javajs.util.PT;

import org.jmol.api.PlatformViewer;
import org.jmol.viewer.Viewer;

/**
 * methods required by Jmol that access java.awt.Component
 * 
 * private to org.jmol.awt
 * 
 */

class Display {

  /**
   * @param display
   * @param widthHeight
   *   
   */
  static void getFullScreenDimensions(Object display, int[] widthHeight) {
    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    widthHeight[0] = d.width;
    widthHeight[1] = d.height;
  }
  
  static boolean hasFocus(Object display) {
    return ((Component) display).hasFocus();
  }

  static void requestFocusInWindow(Object display) {
    ((Component) display).requestFocusInWindow();
  }

  static void repaint(Object display) {
    ((Component) display).repaint();
  }

  /**
   * legacy apps will use this
   * 
   * @param vwr
   * @param g
   * @param size
   */
  @Deprecated
  static void renderScreenImage(PlatformViewer vwr, Object g, Object size) {
    ((Viewer)vwr).renderScreenImage(g, ((Dimension)size).width, ((Dimension)size).height);
  }

  static void setTransparentCursor(Object display) {
    int[] pixels = new int[1];
    java.awt.Image image = Toolkit.getDefaultToolkit().createImage(
        new MemoryImageSource(1, 1, pixels, 0, 1));
    Cursor transparentCursor = Toolkit.getDefaultToolkit()
        .createCustomCursor(image, new Point(0, 0), "invisibleCursor");
    ((Container) display).setCursor(transparentCursor);
  }

  static void setCursor(int c, Object display) {
    ((Container) display).setCursor(Cursor.getPredefinedCursor(c));
  }

  public static String prompt(String label, String data, String[] list,
                              boolean asButtons) {
    try {
      if (!asButtons)
        return JOptionPane.showInputDialog(label, data);
      if (data != null)
        list = PT.split(data, "|");
      int i = JOptionPane.showOptionDialog(null, label, "Jmol prompt",
          JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
          list, list == null ? "OK" : list[0]);
      // ESCAPE will close the panel with no option selected.
      return (data == null ? "" + i : i == JOptionPane.CLOSED_OPTION ? "null"
          : list[i]);
    } catch (Throwable e) {
      return "null";
    }

  }

  public static void convertPointFromScreen(Object display, P3 ptTemp) {
    Point xyTemp = new Point();
    xyTemp.x = (int) ptTemp.x;
    xyTemp.y = (int) ptTemp.y;
    SwingUtilities.convertPointFromScreen(xyTemp, (Component) display);
    ptTemp.set(xyTemp.x, xyTemp.y, Float.NaN);
  }

  /**
   * 
   * @param g
   * @param img
   * @param x
   * @param y
   * @param width
   *        unused in Jmol proper
   * @param height
   *        unused in Jmol proper
   */
  static void drawImage(Object g, Object img, int x, int y, int width,
                        int height) {
    ((Graphics) g).drawImage((java.awt.Image) img, x, y, width, height, null);
  }

  /**
   * 
   * @param g
   * @param img
   * @param x
   * @param y
   * @param width
   *        unused in Jmol proper
   * @param height
   *        unused in Jmol proper
   */
  static void drawImageDTI(Object g, Object img, int x, int y, int width,
                        int height) {
    ((Graphics) g).drawImage((java.awt.Image) img, x, y, x == 0 ? width >> 1 : width, height, 0, y, width, height, null);
  }


}
