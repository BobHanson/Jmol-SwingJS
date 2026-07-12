package org.jmol.awt;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.MemoryImageSource;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.jmol.api.PlatformViewer;
import org.jmol.viewer.Viewer;

import javajs.util.P3d;
import javajs.util.PT;

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

  public static String prompt(PlatformViewer vwr, String label, String data, String[] list,
                              boolean asButtons) throws Exception {
    if (Viewer.isJS) {
      if (!asButtons) {
        /**
         * @j2sNative
         * 
         *            var s = (data == null ? alert(label) : prompt(label, data));
         *            return "" + s;
         */
        {
        }
      }
      if (data != null)
        list = PT.split(data, "|");
      // JavaScript async will throw ScriptInterrupt
      String option = ((Viewer) vwr).eval.promptAsync(label, list);
      if (option != null) {
        if (data == null) {
          for (int i = 0; i < list.length; i++)
            if (list[i].equals(data))
              return "" + i;
        }
        return option;
      }
      return "null";
    }
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

  public static void convertPointFromScreen(Object display, P3d ptTemp) {
    Point xyTemp = new Point();
    xyTemp.x = (int) ptTemp.x;
    xyTemp.y = (int) ptTemp.y;
    SwingUtilities.convertPointFromScreen(xyTemp, (Component) display);
    ptTemp.set(xyTemp.x, xyTemp.y, Double.NaN);
  }


}
