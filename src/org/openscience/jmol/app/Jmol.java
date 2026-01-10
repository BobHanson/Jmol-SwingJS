/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-03-20 17:22:16 -0500 (Thu, 20 Mar 2014) $
 * $Revision: 19476 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.openscience.jmol.app;

import java.awt.Cursor;
import java.awt.Point;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.UIManager;

import org.jmol.dialog.Dialog;
import org.jmol.i18n.GT;
import org.jmol.util.Logger;
import org.openscience.jmol.app.jmolpanel.JmolPanel;
import org.openscience.jmol.app.jmolpanel.JmolResourceHandler;
import org.openscience.jmol.app.jmolpanel.Splash;
import org.openscience.jmol.app.jsonkiosk.JsonNioService;
import org.openscience.jmol.app.jsonkiosk.KioskFrame;

import javajs.util.MeasureD;

/**
 * This application is produced in both Java and JavaScript using 
 * java2script/SwingJS.

 */
public class Jmol extends JmolPanel {

  static {
    /**
     *  @j2sNative
     *  
     *  self.Jmol || (Jmol = self.J2S); 
     *  Jmol._isSwingJS = true; Jmol._isAWTjs = true;
     */
    
  }
  
  public Jmol(JmolApp jmolApp, Splash splash, JFrame frame, Jmol parent, int startupWidth,
      int startupHeight, Map<String, Object> vwrOptions, Point loc) {
    super(jmolApp, splash, frame, parent, startupWidth, startupHeight, vwrOptions, loc);
  }

  public static void main(String[] args) {
    startJmol(new JmolApp(args));
  }

  protected static void startJmol(JmolApp jmolApp) {

    // moved here from JmolPanel Jmol 14.29.56
    
    Dialog.setupUIManager();

    JFrame jmolFrame;

    if (jmolApp.isKiosk) {
      if (jmolApp.startupWidth < 100 || jmolApp.startupHeight < 100) {
        jmolApp.startupWidth = screenSize.width;
        jmolApp.startupHeight = screenSize.height - 75;
      }
      jmolFrame = kioskFrame = new KioskFrame(0, 75, jmolApp.startupWidth,
          jmolApp.startupHeight, null);
    } else {
      jmolFrame = new JFrame();
    }

    // now pass these to vwr

    Jmol jmol = null;

    try {
      if (jmolApp.jmolPosition != null) {
        jmolFrame.setLocation(jmolApp.jmolPosition);
      }

      jmol = getJmol(jmolApp, jmolFrame);

      // scripts are read and files are loaded now
      jmolApp.startViewer(jmol.vwr, jmol.splash, false);

    } catch (Throwable t) {
      Logger.error("uncaught exception: " + t);
      t.printStackTrace();
    }

    if (jmolApp.haveJavaConsole  && allowJavaConsole )
      jmol.getJavaConsole();

    if (jmolApp.isKiosk) {
      kioskFrame.setPanel(jmol);
      bannerFrame.setLabel("click below and type exitJmol[enter] to quit");
      jmol.vwr.script("set allowKeyStrokes;set zoomLarge false;");
    }
    if (jmolApp.port > 0) {
      try {
        jmol.clientService = getJsonNioServer();
        jmol.clientService.startService(jmolApp.port, jmol, jmol.vwr, "-1", JsonNioService.VERSION);
        //        JsonNioService service2 = new JsonNioService();
        //        service2.startService(jmolApp.port, jmol, null, "-2");
        //        service2.sendMessage(null, "test", null);
      } catch (Throwable e) {
        e.printStackTrace();
        if (bannerFrame != null) {
          bannerFrame.setLabel("could not start NIO service on port "
              + jmolApp.port);
        }
        if (jmol.clientService != null)
          jmol.clientService.close();
      }

    }
  }
  
  public static Jmol getJmol(JFrame baseframe, 
                             int width, int height, Map<String, Object> vwrOptions) {
    JmolApp jmolApp = new JmolApp(new String[] {});
    jmolApp.startupHeight = height;
    jmolApp.startupWidth = width;
    jmolApp.info = (vwrOptions == null ? new Hashtable<String, Object>() : vwrOptions);
    return getJmol(jmolApp, baseframe);
  }
  
  public static Jmol getJmol(JmolApp jmolApp, JFrame frame) {

    Splash splash = null;
    if (jmolApp.haveDisplay && jmolApp.splashEnabled) {
      ImageIcon splash_image = JmolResourceHandler.getIconX("splash");
      if (!jmolApp.isSilent)
        Logger.info("splash_image=" + splash_image);
      splash = new Splash(frame, splash_image);
      splash.setCursor(new Cursor(Cursor.WAIT_CURSOR));
      splash.showStatus(GT.$("Creating main window..."));
      splash.showStatus(GT.$("Initializing Swing..."));
    }
    if (jmolApp.haveDisplay)
      try {
        UIManager.setLookAndFeel(UIManager
            .getCrossPlatformLookAndFeelClassName());
      } catch (Exception exc) {
        System.err.println("Error loading L&F: " + exc);
      }

    if (splash != null)
      splash.showStatus(GT.$("Initializing Jmol..."));

    Jmol window = new Jmol(jmolApp, splash, frame, null, jmolApp.startupWidth,
        jmolApp.startupHeight, jmolApp.info, null);
    if (jmolApp.haveDisplay)
      frame.setVisible(true);
    return window;
  }

}
