/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-08-16 14:28:57 -0500 (Thu, 16 Aug 2007) $
 * $Revision: 8103 $
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
/*package org.openscience.jmol.app.webexport;


import org.jmol.export.history.HistoryFile;
import org.jmol.i18n.GT;

public class WebMaker extends JPanel {

  // not currently implementable
  
  private final static String WEB_MAKER_WINDOW_NAME = "JmolWebPageMaker";

  public static void main(String[] args) {
    System.out
        .println("Jmol_Web_Page_Maker is running as a standalone application");
    //Schedule a job for the event-dispatching thread:
    //creating and showing this application's GUI.
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        WebExport.createAndShowGUI(null, historyFile, WEB_MAKER_WINDOW_NAME);
      }
    });
  }
  
  static HistoryFile historyFile;
  
  static HistoryFile getHistoryFile() {
    return historyFile;
  }

  static {
    if (System.getProperty("javawebstart.version") != null) {

      // If the property is found, Jmol is running with Java Web Start. To fix
      // bug 4621090, the security manager is set to null.
      System.setSecurityManager(null);
    }
    if (System.getProperty("user.home") == null) {
      System.err.println(
          GT._("Error starting Jmol: the property 'user.home' is not defined."));
      System.exit(1);
    }
    File ujmoldir = new File(new File(System.getProperty("user.home")),
                      ".jmol");
    ujmoldir.mkdirs();
    historyFile = new HistoryFile(new File(ujmoldir, "history"),
        "Jmol's persistent values");
  }
}
*/