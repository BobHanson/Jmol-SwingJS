/*  
 *  The Janocchio program is (C) 2007 Eli Lilly and Co.
 *  Authors: David Evans and Gary Sharman
 *  Contact : janocchio-users@lists.sourceforge.net.
 * 
 *  It is derived in part from Jmol 
 *  (C) 2002-2006 The Jmol Development Team
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.
 *  All we ask is that proper credit is given for our work, which includes
 *  - but is not limited to - adding the above copyright notice to the beginning
 *  of your source code files, and to any copyright notice that you may distribute
 *  with programs based on this work.
 *
 *  This program is distributed in the hope that it will be useful, on an 'as is' basis,
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.openscience.jmol.app.janocchio;

import java.awt.Cursor;

import javajs.util.BS;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.UIManager;

import org.jmol.dialog.Dialog;
import org.jmol.i18n.GT;
import org.jmol.util.Logger;
import org.openscience.jmol.app.JmolApp;
import org.openscience.jmol.app.jmolpanel.Splash;

public class Nmr extends JmolApp {

  public static final String VERSION = "0.1.0";

  public static String path;
  static {
    path = Nmr.class.getName();
    path = path.substring(0, path.lastIndexOf(".") + 1);
  }

  private Splash splash;
  NmrPlugin plugin;
  JFrame mainFrame;
  NMR_JmolPanel nmrPanel;

  public boolean isPlugin() { 
    return plugin != null;
  }
  
  public static void main(String[] args) {
    new Nmr(args);
  }
  public Nmr(String[] args, NmrPlugin plugin) {
    super(args);
    this.plugin = plugin;
    init();
  }

  public Nmr(String[] args) {
    super(args);
    init();
  }
  
  private void init() {
    try {
      if (haveDisplay) {
        Dialog.setupUIManager();
        try {
          UIManager.setLookAndFeel(UIManager
              .getCrossPlatformLookAndFeelClassName());
        } catch (Exception exc) {
          System.err.println("Error loading L&F: " + exc);
        }
      }
      mainFrame = new JFrame();
      if (jmolPosition != null) {
        mainFrame.setLocation(jmolPosition);
      }

      if (haveDisplay && splashEnabled) {
        ImageIcon splash_image = NmrResourceHandler.getIconX("splash");
        if (!isSilent)
          Logger.info("splash_image=" + splash_image);
        splash = new Splash(mainFrame, splash_image);
        splash.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        splash.showStatus(GT.$("Creating main window..."));
        splash.showStatus(GT.$("Initializing Swing..."));
      }

      info.put("guimap", new NmrGuiMap());

      if (splash != null)
        splash.showStatus(GT.$("Initializing Jmol..."));

      nmrPanel = new NMR_JmolPanel(this, splash, mainFrame, null,
          startupWidth, startupHeight, info, null);
      if (haveDisplay)
        mainFrame.setVisible(true);

      startViewer(nmrPanel.vwr, splash, false);

      if (haveConsole)
        nmrPanel.getJavaConsole();

      nmrPanel.vwr.script("set measureAllmodels ON;font measurements 18; font labels 18;measure '2:%1.1VALUE %UNITS//hz'");

    } catch (Throwable t) {
      Logger.error("uncaught exception: " + t);
      t.printStackTrace();
    }

  }

//  public static CdkConvertor getCDKConverter() {
//    return (CdkConvertor) Interface
//        .getInterface("org.openscience.jmol.app.janocchio.CdkConvertor");
//
//  }



}
