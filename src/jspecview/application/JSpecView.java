/* Copyright (C) 2002-2012  The JSpecView Development Team
 *
 * Contact: robert.lancashire@uwimona.edu.jm
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

// CHANGES to 'MainFrame.java' - Main Application GUI
// University of the West Indies, Mona Campus
//
// 20-06-2005 kab - Implemented exporting JPG and PNG image files from the application
//                - Need to sort out JSpecViewFileFilters for Save dialog to include image file extensions
// 21-06-2005 kab - Adjusted export to not prompt for spectrum when exporting JPG/PNG
// 24-06-2005 rjl - Added JPG, PNG file filters to dialog
// 30-09-2005 kab - Added command-line support
// 30-09-2005 kab - Implementing Drag and Drop interface (new class)
// 10-03-2006 rjl - Added Locale overwrite to allow decimal points to be recognised correctly in Europe
// 25-06-2007 rjl - Close file now checks to see if any remaining files still open
//                - if not, then remove a number of menu options
// 05-07-2007 cw  - check menu options when changing the focus of panels
// 06-07-2007 rjl - close imported file closes spectrum and source and updates directory tree
// 06-11-2007 rjl - bug in reading displayschemes if folder name has a space in it
//                  use a default scheme if the file can't be found or read properly,
//                  but there will still be a problem if an attempt is made to
//                  write out a new scheme under these circumstances!
// 23-07-2011 jak - altered code to support drawing scales and units separately
// 21-02-2012 rmh - lots of additions  -  integrated into Jmol

package jspecview.application;

import java.awt.Component;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import jspecview.common.JSVersion;
//import jspecview.unused.Test;

import org.jmol.api.JSVInterface;
import org.jmol.util.Logger;



/**
 * The Main Class or Entry point of the JSpecView Application.
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 * @author Bob Hanson St. Olaf College hansonr@stolaf.edu
 */
public class JSpecView implements JSVInterface {

  private MainFrame mainFrame;

  //  ------------------------ Program Properties -------------------------

  public static void main(String args[]) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
    }

		//new Test();

		Logger.info("JSpecView Application " + JSVersion.VERSION);
    JSpecView jsv = new JSpecView();
    jsv.mainFrame = new MainFrame(null, jsv);

    if (args.length > 0) {
      // check for command-line arguments
      if (args.length == 2 && args[0].equalsIgnoreCase("-script"))
        jsv.mainFrame.runScriptNow(args[1]);
      else
        for (int i = 0; i < args.length; i++) {
          System.out.println("JSpecView is attempting to open " + args[i]);
          jsv.mainFrame.vwr.openFile(args[i], false);
        }
    }
    jsv.mainFrame.setVisible(true);
    //if (args.length == 0)
      //jsv.mainFrame.showFileOpenDialog();
  }

  private static String propertiesFileName = "jspecview.properties";


  /**
   * for the applet, this is queued
   */
  @Override
	public void runScript(String script) {
    mainFrame.runScriptNow(script);
  }

  @Override
	public void setProperties(Properties properties) {
    try {
      FileInputStream fileIn = new FileInputStream(propertiesFileName);
      properties.load(fileIn);
    } catch (Exception e) {
    }
  }

  @Override
	public void saveProperties(Properties properties) {
    // Write out current properties
    try {
      FileOutputStream fileOut = new FileOutputStream(propertiesFileName);
      properties.store(fileOut, "JSpecView Application Properties");
    } catch (Exception e) {
    }
  }

  @Override
	public void exitJSpecView(boolean withDialog, Object frame) {
    if (withDialog
        && JOptionPane.showConfirmDialog((Component)frame, "Exit JSpecView?",
            "Exit", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION)
      return;
    System.exit(0);
  }

  @Override
	public void syncToJmol(String peak) {
    // ignore -- this is the stand-alone app
    // will use JmolSyncInterface.syncScript() instead
  }
}
