/* $RCSfile$
 * $Author jonathan gutow$
 * $Date Aug 5, 2007 9:19:06 AM $
 * $Revision$
 * Updated May 2016 by Angel Herraez
 *
 * Copyright (C) 2005-2016  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */
package org.openscience.jmol.app.webexport;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.Properties;

import javajs.util.PT;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileSystemView;

import org.jmol.viewer.Viewer;
import org.jmol.i18n.GT;
import org.openscience.jmol.app.HistoryFile;
import org.openscience.jmol.app.jmolpanel.GuiMap;

public class WebExport extends JPanel implements WindowListener {

  final static String chemappsPath = "http://chemapps.stolaf.edu/jmol/jsmol"; 

  private static boolean showMoleculesAndOrbitals = false; //not implemented

  //run status
  private final static int STAND_ALONE = 0;
  private final static int IN_JMOL = 1;

  private static int runStatus = IN_JMOL; //assume running inside Jmol

  private static HistoryFile historyFile;

  private static WebPanel[] webPanels;
  static WebExport webExport;
  private static JFrame webFrame;
  private static String windowName;

  private WebExport(Viewer vwr, HistoryFile hFile) {
    super(new BorderLayout());

    historyFile = hFile;
    remoteAppletPath = historyFile.getProperty("webMakerAppletPath", chemappsPath);
    localAppletPath = historyFile.getProperty("webMakerLocalAppletPath", chemappsPath);
    pageAuthorName = historyFile.getProperty("webMakerPageAuthorName",
        GT._("Jmol Web Page Maker"));
    popInWidth=PT.parseInt(historyFile.getProperty("webMakerPopInWidth", "300"));
    popInHeight=PT.parseInt(historyFile.getProperty("webMakerPopInHeight", "300"));
    scriptButtonPercent = PT.parseInt(historyFile.getProperty(
        "webMakerScriptButtonPercent", "60"));

    //Define the tabbed pane
    JTabbedPane mainTabs = new JTabbedPane();

    //Create file chooser
    JFileChooser fc = new JFileChooser();

    webPanels = new WebPanel[2];
    
    if (runStatus != STAND_ALONE) {
      //Add tabs to the tabbed pane

      JPanel introPanel = new JPanel();
      String introFileName = "WebExportIntro";
      URL url = GuiMap.getHtmlResource(this, introFileName);
      if (url == null) {
        System.err.println(GT.o(GT._("Couldn't find file: {0}"), introFileName+".html"));
      }
      JEditorPane intro = new JEditorPane();
      if (url != null) {
        try {
          intro.setPage(url);
        } catch (IOException e) {
          System.err.println("Attempted to read a bad URL: " + url);
        }
      }
      intro.setEditable(false);
      JScrollPane introPane = new JScrollPane(intro);
      introPane.setMaximumSize(new Dimension(450, 350));
      introPane.setPreferredSize(new Dimension(400, 300));
      introPanel.setLayout(new BorderLayout());
      introPanel.add(introPane);
      introPanel.setMaximumSize(new Dimension(450, 350));
      introPanel.setPreferredSize(new Dimension(400, 300));

      mainTabs.add(GT._("Introduction"), introPanel);

      webPanels[0] = new PopInJmol(vwr, fc, webPanels, 0);
      webPanels[1] = new ScriptButtons(vwr, fc, webPanels, 1);

      int w = Integer.parseInt(historyFile.getProperty("webMakerInfoWidth",
          "300"));
      int h = Integer.parseInt(historyFile.getProperty("webMakerInfoHeight",
          "350"));

      mainTabs.addTab(GT._("Pop-In Jmol"), webPanels[0].getPanel(w, h));
      mainTabs.addTab(GT._("ScriptButton Jmol"), webPanels[1].getPanel(w, h));

      // Uncomment to activate the test panel
      //    Test TestCreator = new Test((Viewer)vwr);
      //    JComponent Test = TestCreator.Panel();
      //    Maintabs.addTab("Tests",Test);
    }

    showMoleculesAndOrbitals = (runStatus == STAND_ALONE || WebPanel.checkOption(vwr.getP("webMakerAllTabs")));
    if (showMoleculesAndOrbitals) {
      //mainTabs.addTab("Orbitals", (new Orbitals()).getPanel());
      //mainTabs.addTab("Molecules", (new Molecules()).getPanel());
    }

    //The LogPanel should always be the last one

    mainTabs.addTab(GT._("Log"), LogPanel.getPanel());

    //Add the tabbed pane to this panel
    add(mainTabs);

    //Create the small log

    add(LogPanel.getMiniPanel(), BorderLayout.SOUTH);

    //Uncomment the following line to use scrolling tabs.
    //tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

  }

  static String TimeStamp_WebLink() {
	  return GT.o(GT._("Page skeleton and JavaScript generated by the Export to Web module of {0} on {1}."),
          new String[] {" <a href=\"http://jmol.sourceforge.net\">Jmol " + Viewer.getJmolVersion() + "</a> ",
          DateFormat.getDateInstance().format(new Date()) });
	      //Specify medium verbosity on the date and time
  }
  
  /*
   * Create the GUI and show it.  For thread safety,
   * this method should be invoked from the
   * event-dispatching thread.
   */

  public static void dispose() {
    webFrame.dispose();
    webFrame = null;
  }
  
  public static WebExport createAndShowGUI(Viewer vwr,
                                           HistoryFile historyFile, String wName) {

    if (vwr == null)
      runStatus = STAND_ALONE;

    //Create and set up the window.
    if (webFrame != null) {
      webFrame.setVisible(true);
      webFrame.toFront();
      return webExport;
    }
    webFrame = new JFrame(GT._("Jmol Web Page Maker"));
    //Set title bar icon
    String imageName = "org/openscience/jmol/app/images/icon.png";
    URL imageUrl = vwr.getClass().getClassLoader().getResource(imageName);
    ImageIcon jmolIcon = new ImageIcon(imageUrl);
    webFrame.setIconImage(jmolIcon.getImage());
    windowName = wName;
    historyFile.repositionWindow(windowName, webFrame, 700, 400, true);
    if (runStatus == STAND_ALONE) {
      //Make sure we have nice window decorations.
      JFrame.setDefaultLookAndFeelDecorated(true);
      JDialog.setDefaultLookAndFeelDecorated(true);
      webFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    } else {
      webFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    //Create and set up the content pane.
    webExport = new WebExport(vwr, historyFile);
    webExport.setOpaque(true); //content panes must be opaque
    webFrame.setContentPane(webExport);
    webFrame.addWindowListener(webExport);

    //Display the window.
    webFrame.pack();
    webFrame.setVisible(true);
    if (runStatus == STAND_ALONE) {
      //LogPanel.Log("Jmol_Web_Page_Maker is running as a standalone application");
    } else {
      //LogPanel.Log("Jmol_Web_Page_Maker is running as a plug-in");
    }

    return webExport;
  }

  public static void saveHistory() {
    if (historyFile == null)
      return;
    historyFile.addWindowInfo(windowName, webFrame, null);
    //    prop.setProperty("webMakerInfoWidth", "" + webPanels[0].getInfoWidth());
    //    prop.setProperty("webMakerInfoHeight", "" + webPanels[0].getInfoHeight());
    prop.setProperty("webMakerAppletPath", remoteAppletPath);
    prop.setProperty("webMakerLocalAppletPath", localAppletPath);
    prop.setProperty("webMakerPageAuthorName", pageAuthorName);
    historyFile.addProperties(prop);
  }

  static String remoteAppletPath, localAppletPath;
  
  static String getAppletPath(boolean isRemote) {
    return (isRemote ? remoteAppletPath : localAppletPath);
  }

  static Properties prop = new Properties();

  static void setAppletPath(String path, boolean isRemote) {
    if (path == null)
      path = chemappsPath;
    if (isRemote) {
      remoteAppletPath = path;
      prop.setProperty("webMakerAppletPath", remoteAppletPath);
      historyFile.addProperties(prop);
    } else {
      localAppletPath = path;
      prop.setProperty("webMakerLocalAppletPath", localAppletPath);
      historyFile.addProperties(prop);
    }
  }

  static String pageAuthorName;

  static String getPageAuthorName() {
    return pageAuthorName;
  }

  static void setWebPageAuthor(String pageAuthor) {
    if (pageAuthor == null)
      pageAuthor = GT._("Jmol Web Page Maker");
    pageAuthorName = pageAuthor;
    prop.setProperty("webMakerPageAuthorName", pageAuthorName);
    historyFile.addProperties(prop);
  }

  static int popInWidth;
  static int popInHeight;
 
  static void setPopInDim(int appletWidth, int appletHeight) {
    if (appletWidth<25||appletWidth>3000)
      appletWidth = 300;
    if (appletHeight<25||appletHeight>3000)
      appletHeight = 300;
    popInWidth=appletWidth;
    popInHeight=appletHeight;
    prop.setProperty("webMakerPopInWidth", ""+appletWidth);
    prop.setProperty("webMakerPopInHeight", ""+appletHeight);
    historyFile.addProperties(prop);
  }
  
  static int getPopInWidth(){
    return popInWidth;
  }
  
  static int getPopInHeight(){
    return popInHeight;
  }
  
  static int scriptButtonPercent;
  
  static void setScriptButtonPercent(int percent){
    if (percent <10 || percent > 90)
      percent = 60;
    scriptButtonPercent = percent;
    prop.setProperty("webMakerScriptButtonPercent", ""+percent);
    historyFile.addProperties(prop);
  }
  
  static int getScriptButtonPercent(){
    return scriptButtonPercent;
  }
  
  static JFrame getFrame() {
    return webFrame;
  }

  /* Window event code for cleanup*/
  @Override
  public void windowClosing(WindowEvent e) {
  }

  @Override
  public void windowClosed(WindowEvent e) {
    //cleanUp(); Should do this, but then states during a session loose their .png files if window is closed.
  }

  @Override
  public void windowOpened(WindowEvent e) {
  }

  @Override
  public void windowIconified(WindowEvent e) {
  }

  @Override
  public void windowDeiconified(WindowEvent e) {
  }

  @Override
  public void windowActivated(WindowEvent e) {
  }

  @Override
  public void windowDeactivated(WindowEvent e) {
  }

  /**
   * @param e  
   */
  public void windowGainedFocus(WindowEvent e) {
  }

  /**
   * @param e  
   */
  public void windowLostFocus(WindowEvent e) {
  }

  /**
   * @param e  
   */
  public void windowStateChanged(WindowEvent e) {
  }

  public static void cleanUp() {
    //gets rid of scratch files.
    FileSystemView Directories = FileSystemView.getFileSystemView();
    File homedir = Directories.getHomeDirectory();
    String homedirpath = homedir.getPath();
    String scratchpath = homedirpath + "/.jmol_WPM";
    File scratchdir = new File(scratchpath);
    if (scratchdir.exists()) {
      File[] dirListing = null;
      dirListing = scratchdir.listFiles();
      for (int i = 0; i < (dirListing.length); i++) {
        dirListing[i].delete();
      }
    }
    saveHistory();//force save of history.
  }
}
