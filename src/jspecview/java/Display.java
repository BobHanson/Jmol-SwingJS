package jspecview.java;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.MemoryImageSource;

import javajs.util.P3;
import javajs.util.PT;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.jmol.api.GenericPlatform;
import org.jmol.api.PlatformViewer;


/**
 * methods required by Jmol that access java.awt.Component
 * 
 * private to jspecview.java
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
   * @param viewer
   * @param g
   * @param size
   */
  static void renderScreenImage(PlatformViewer viewer, Object g, Object size) {
    //((Viewer)viewer).renderScreenImage(g, ((Dimension)size).width, ((Dimension)size).height);
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
    Container d = (Container) display;
    switch (c) {
    case GenericPlatform.CURSOR_HAND:
      c = Cursor.HAND_CURSOR;
      break;
    case GenericPlatform.CURSOR_MOVE:
      c = Cursor.MOVE_CURSOR;
      break;
    case GenericPlatform.CURSOR_ZOOM:
      c = Cursor.N_RESIZE_CURSOR;
      break;
    case GenericPlatform.CURSOR_CROSSHAIR:
      c = Cursor.CROSSHAIR_CURSOR;
      break;
    case GenericPlatform.CURSOR_WAIT:
      c = Cursor.WAIT_CURSOR;
      break;
    default:
      d.setCursor(Cursor.getDefaultCursor());
      return;
    }
    d.setCursor(Cursor.getPredefinedCursor(c));
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
          list, list[0]);
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


}
