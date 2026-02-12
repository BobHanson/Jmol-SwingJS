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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.jmol.api.JSVInterface;
import org.jmol.api.JmolSyncInterface;
import org.jmol.util.Logger;

import javajs.util.Lst;
import javajs.util.PT;
import jspecview.api.JSVPanel;
import jspecview.api.ScriptInterface;
import jspecview.common.ColorParameters;
import jspecview.common.JSVFileManager;
import jspecview.common.JSVersion;
import jspecview.common.JSViewer;
import jspecview.common.PanelData;
import jspecview.common.PanelNode;
import jspecview.common.Parameters;
import jspecview.common.ScriptToken;
import jspecview.common.Spectrum;
import jspecview.java.AwtFileHelper;
import jspecview.java.AwtMainPanel;
import jspecview.java.AwtPanel;
import jspecview.source.JDXSource;



/**
 * The Main Class or Entry point of the JSpecView Application.
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 * @author Bob Hanson St. Olaf College hansonr@stolaf.edu
 */
public class JSpecView implements JSVInterface, JmolSyncInterface, ScriptInterface {

  private MainFrame mainFrame;
  JSViewer vwr;
  public String defaultDisplaySchemeName;
  private DisplaySchemesProcessor dsp;
  public JmolSyncInterface jmol;
  private JSVPanel prevPanel;
  private JSVInterface statusListener;

  public void setMainFrame(MainFrame mainFrame) {
    this.mainFrame = mainFrame;    
  }

  public boolean isHeadless() {
    return (mainFrame == null);
  }

  /**
   * The entry point constructor, from Jmol's StatusListener or from the AppletPro object (via MainFrame) or from MainFrame itself.
   * 
   * @param hasDisplay
   * @param jmolStatusListener
   */
  public JSpecView(boolean hasDisplay, JSVInterface jmolStatusListener) {
    vwr = new JSViewer(this, false, false, hasDisplay);
    vwr.mainPanel = new AwtMainPanel(new BorderLayout());
    JSVFileManager.setDocumentBase(vwr, null);
    if (hasDisplay) {
      mainFrame = new MainFrame(this, null, (jmolStatusListener == null ? this : jmolStatusListener));
      vwr.setParentFrame(mainFrame);
    } else {
      getDisplaySchemesProcessor(this);
      setApplicationProperties(true);
    }
  }
  
  public void setStatusListener(JSVInterface jmolStatusListener) {
    statusListener = jmolStatusListener;
    getDisplaySchemesProcessor(jmolStatusListener);
  }

  public void getDisplaySchemesProcessor(JSVInterface statusListener) {
    // Initalize application properties with defaults
    // and load properties from file
    Properties properties = vwr.properties = new Properties();
    // sets the list of recently opened files property to be initially empty
    properties.setProperty("recentFilePaths", "");
    properties.setProperty("confirmBeforeExit", "true");
    properties.setProperty("automaticallyOverlay", "false");
    properties.setProperty("automaticallyShowLegend", "false");
    properties.setProperty("useDirectoryLastOpenedFile", "true");
    properties.setProperty("useDirectoryLastExportedFile", "false");
    properties.setProperty("directoryLastOpenedFile", "");
    properties.setProperty("directoryLastExportedFile", "");
    properties.setProperty("showSidePanel", "true");
    properties.setProperty("showToolBar", "true");
    properties.setProperty("showStatusBar", "true");
    properties.setProperty("defaultDisplaySchemeName", "Default");
    properties.setProperty("showGrid", "false");
    properties.setProperty("showCoordinates", "false");
    properties.setProperty("showXScale", "true");
    properties.setProperty("showYScale", "true");
    properties.setProperty("svgForInkscape", "false");
    properties.setProperty("automaticTAConversion", "false");
    properties.setProperty("AtoTSeparateWindow", "false");
    properties.setProperty("automaticallyIntegrate", "false");
    properties.setProperty("integralMinY", "0.1");
    properties.setProperty("integralFactor", "50");
    properties.setProperty("integralOffset", "30");
    properties.setProperty("integralPlotColor", "#ff0000");

    statusListener.setProperties(properties);
    dsp = new DisplaySchemesProcessor();
    if (!dsp.loadDefaultXML()){
      Logger.info("JSpecView: Problem loading Display Scheme");
    }
  }

  private static String propertiesFileName = "jspecview.properties";


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
  public void siOpenDataOrFile(Object data, String name, Lst<Spectrum> specs,
                               String url, int firstSpec, int lastSpec,
                               boolean isAppend, String script, String id) {
    boolean isOne = (vwr.currentSource == null);
    switch (name == null && url == null ? JSViewer.FILE_OPEN_ERROR
        : vwr.openDataOrFile(data, name, specs, url, firstSpec, lastSpec,
            isAppend, id)) {
    case JSViewer.FILE_OPEN_OK:
      if (script == null && isOne && vwr.currentSource.isCompoundSource
          && vwr.pd().getSpectrum().isGC())
        script = "VIEW ALL;PEAK GC/MS ID=#1";
      if (script != null)
        runScript(script);
      break;
    case JSViewer.FILE_OPEN_ALREADY:
      if (mainFrame != null) {
        mainFrame.awaken(false);
        mainFrame.awaken(true);
      }
      break;
    case JSViewer.FILE_OPEN_ERROR:
      if (mainFrame != null) {
        mainFrame.awaken(false);
        mainFrame.awaken(true);
        String msg = "There was an error reading " + (name != null ? name : url);
        Logger.info(msg);
        if (vwr.hasDisplay)
          JOptionPane.showMessageDialog(mainFrame, msg);
      }
      break;
    }
    siValidateAndRepaint(false);
  }

  @Override
  public void siSetCurrentSource(JDXSource source) {
    vwr.currentSource = source;
    if (source != null && mainFrame != null)
      mainFrame.closeMenuItem(source);
  }

  private void setError(boolean isError, boolean isWarningOnly) {
    if (mainFrame != null)
      mainFrame.setError(isError, isWarningOnly);
  }

  /**
   * Sets the display properties as specified from the preferences dialog or the
   * properties file
   * 
   * @param jsvp
   *          the display panel
   */
  @Override
  public void siSetPropertiesFromPreferences(JSVPanel jsvp,
      boolean includeMeasures) {
    ColorParameters ds = dsp.getDisplaySchemes().get(defaultDisplaySchemeName);
    jsvp.getPanelData().addListener(mainFrame);
    vwr.parameters.setFor(jsvp, (ds == null ? dsp.getDefaultScheme() : ds),
        includeMeasures);
    vwr.checkAutoIntegrate();
    jsvp.doRepaint(true);
  }


  @Override
  public void siProcessCommand(String script) {
    runScriptNow(script);
  }

  @Override
  public void siSetSelectedPanel(JSVPanel jsvp) {
    if (mainFrame != null)
      mainFrame.setSelectedPanel(jsvp);
    else {
      vwr.mainPanel.setSelectedPanel(vwr, jsvp, vwr.panelNodes);
      vwr.spectraTree.setSelectedPanel(this, jsvp);
      if (jsvp != null) {
        jsvp.setEnabled(true);
        jsvp.setFocusable(true);
      }
    }
  }


  @Override
  public void siSendPanelChange() {
    if (vwr.selectedPanel == prevPanel)
      return;
    prevPanel = vwr.selectedPanel;
    vwr.sendPanelChange();
  }

  @Override
  public void siSyncLoad(String filePath) {
    vwr.closeSource(null);
    siOpenDataOrFile(null, null, null, filePath, -1, -1, false, null, null);
    if (vwr.currentSource == null)
      return;
    if (vwr.panelNodes.get(0).getSpectrum().isAutoOverlayFromJmolClick())
      vwr.execView("*", false);
  }

  @Override
  public void siValidateAndRepaint(boolean isAll) {
    if (mainFrame != null) {
      mainFrame.validateAndRepaint(isAll);
    }
  }

  @Override
  public void siExecHidden(boolean b) {
    if (mainFrame != null) {
      mainFrame.execHidden(b);
    }
  }

  @Override
  public String siLoaded(String value) {
    PanelData pd = vwr.pd();
    return (!pd.getSpectrum().is1D() && pd.getDisplay1D() ?
        "Click on the spectrum and use UP or DOWN keys to see subspectra." : null);
  }

  @Override
  public void siExecScriptComplete(String msg, boolean isOK) {
    vwr.requestRepaint();
    if (msg != null) {
      writeStatus(msg);
      if (msg.length() == 0)
        msg = null;
    }
    // if (msg == null) {
    // commandInput.requestFocus();
    // }
  }

  @Override
  public void siExecSetCallback(ScriptToken st, String value) {
    // applet only
  }

  @Override
  public void siUpdateBoolean(ScriptToken st, boolean TF) {
    if (mainFrame != null)
      mainFrame.updateToolbar(st, TF);
  }

  @Override
  public void siCheckCallbacks(String title) {
    // setMainTitle(title);
  }

  @Override
  public void siNodeSet(PanelNode panelNode) {
    siSetMenuEnables(panelNode, false);
    writeStatus("");
  }

  /**
   * Closes the <code>JDXSource</code> specified by source
   * 
   * @param source
   *          the <code>JDXSource</code>
   */
  @Override
  public void siSourceClosed(JDXSource source) {
    setError(false, false);
    if (mainFrame != null)
      mainFrame.sourceClosed(source);
  }

  @Override
  public void siSetLoaded(String fileName, String filePath) {
    if (mainFrame != null)
      mainFrame.setLoading(fileName, filePath);
  }

  @Override
  public void siUpdateRecentMenus(String filePath) {
    if (mainFrame != null)
      mainFrame.updateRecentMenus(filePath);
  }

  @Override
  public void siSetMenuEnables(PanelNode node, boolean isSplit) {
    if (mainFrame != null) {
      mainFrame.setMenuEnables(node, isSplit);
    }
  }

  @Override
  public JSVPanel siGetNewJSVPanel(Spectrum spec) {
    return (spec == null ? null : AwtPanel.getPanelOne(vwr, spec));
  }

  @Override
  public JSVPanel siGetNewJSVPanel2(Lst<Spectrum> specs) {
    return AwtPanel.getPanelMany(vwr, specs);
  }

  @Override
  public void siExecTest(String value) {
    System.out.println(PT.toJSON(null, vwr.getPropertyAsJavaObject(value)));
    //syncScript("Jmol sending to JSpecView: jmolApplet_object__5768809713073075__JSpecView: <PeakData file=\"file:/C:/jmol-dev/workspace/Jmol-documentation/script_documentation/examples-12/jspecview/acetophenone.jdx\" index=\"31\" type=\"13CNMR\" id=\"6\" title=\"carbonyl ~200\" peakShape=\"multiplet\" model=\"acetophenone\" atoms=\"1\" xMax=\"199\" xMin=\"197\"  yMax=\"10000\" yMin=\"0\" />");
  }

  @Override
  public void siNewWindow(boolean isSelected, boolean fromFrame) {
    // not implemented for MainFrame
  }

  @Override
  public void repaint() {
    if (mainFrame != null)
      mainFrame.repaint();
  }

  @SuppressWarnings("deprecation")
  @Override
  public void setCursor(int id) {
    if (mainFrame != null)
      mainFrame.setCursor(id);
  }

  @Override
  public boolean isSigned() {
    return true;
  }

  public void setApplicationProperties(boolean shouldApplySpectrumDisplaySettings) {
    Properties properties = vwr.properties;
    vwr.interfaceOverlaid = Boolean.parseBoolean(properties
        .getProperty("automaticallyOverlay"));
    vwr.autoShowLegend = Boolean.parseBoolean(properties
        .getProperty("automaticallyShowLegend"));
    AwtFileHelper fh = (AwtFileHelper) vwr.fileHelper; 
    fh.useDirLastOpened = Boolean.parseBoolean(properties
        .getProperty("useDirectoryLastOpenedFile"));
    fh.useDirLastExported = Boolean.parseBoolean(properties
        .getProperty("useDirectoryLastExportedFile"));
    fh.dirLastOpened = properties.getProperty("directoryLastOpenedFile");
    fh.dirLastExported = properties.getProperty("directoryLastExportedFile");

    // Initialise DisplayProperties
    defaultDisplaySchemeName = properties
        .getProperty("defaultDisplaySchemeName");

    if (shouldApplySpectrumDisplaySettings) {
      vwr.parameters.setBoolean(ScriptToken.GRIDON, Parameters.isTrue(properties
          .getProperty("showGrid")));
      vwr.parameters.setBoolean(ScriptToken.COORDINATESON, Parameters
          .isTrue(properties.getProperty("showCoordinates")));
      vwr.parameters.setBoolean(ScriptToken.XSCALEON, Parameters.isTrue(properties
          .getProperty("showXScale")));
      vwr.parameters.setBoolean(ScriptToken.YSCALEON, Parameters.isTrue(properties
          .getProperty("showYScale")));
    }

    // TODO: Need to apply Properties to all panels that are opened
    // and update coordinates and grid CheckBoxMenuItems

    // Processing Properties
    vwr.setIRmode(properties.getProperty("automaticTAConversion"));
    try {
      vwr.autoIntegrate = Boolean.parseBoolean(properties
          .getProperty("automaticallyIntegrate"));
      vwr.parameters.integralMinY = parseDoubleSafely(properties
          .getProperty("integralMinY"), vwr.parameters.integralMinY);
      vwr.parameters.integralRange = parseDoubleSafely(properties
          .getProperty("integralRange"),vwr.parameters.integralRange);
      vwr.parameters.integralOffset = parseDoubleSafely(properties
          .getProperty("integralOffset"), vwr.parameters.integralOffset);
      vwr.parameters.set(null, ScriptToken.INTEGRALPLOTCOLOR, properties
          .getProperty("integralPlotColor"));
    } catch (Exception e) {
      System.err.println("Bad PropertyValue ");
      e.printStackTrace();
      // bad property value
    }
  }

  private static double parseDoubleSafely(String sval, double defVal) {
    return (sval == null ? defVal : Double.parseDouble(sval));
  }


  public static void main(String args[]) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
    }

    Logger.info("JSpecView Application " + JSVersion.VERSION);


    boolean noDisplay = GraphicsEnvironment.isHeadless();
    boolean autoexit = noDisplay;

    int n = args.length;
    
    // check for command-line arguments  "file" "file" "file" -script "xxxx" -nodisplay -exit
    // IN THAT ORDER

    if (n > 0 && args[n - 1].equalsIgnoreCase("-exit")) {
        autoexit = true;
        n--;
    }
    if (n > 0 && args[n - 1].equalsIgnoreCase("-nodisplay")) {
      noDisplay = autoexit = true;
        n--;
    }

    if (autoexit)
      System.out.println("JSpecview running headless");


    if (noDisplay)
      System.out.println("JSpecview has no display");

    JSpecView jsv = new JSpecView(!noDisplay, null);


    if (n >= 2) {
      if (n == 2 && args[0].equalsIgnoreCase("-script")) {
        String script = args[1];
        System.out.println("JSpecView is running script " + script);
        jsv.vwr.runScriptNow(args[1]);
      } else {
        for (int i = 0; i < args.length; i++) {
          System.out.println("JSpecView is attempting to open " + args[i]);
          jsv.vwr.openFile(args[i], false);
        }
      }
    }
    
    if (noDisplay || autoexit)
      exitNow();
    jsv.mainFrame.setVisible(true);
  }

  /**
   * called by Jmol's StatusListener to register itself, indicating to JSpecView
   * that it needs to synchronize with it
   */
  @Override
  public void register(String appletID, JmolSyncInterface jmolStatusListener) {
    jmol = jmolStatusListener;
    vwr.isEmbedded = true;
  }

  @Override
  public Map<String, Object> getJSpecViewProperty(String key) {
    return vwr.getPropertyAsJavaObject(key);
  }

  // JSVAppInterface

  @Override
  public synchronized void syncScript(String peakScript) {
    if (mainFrame != null)
      mainFrame.setTreeEnabled(false);
    vwr.syncScript(peakScript);
    if (mainFrame != null)
      mainFrame.setTreeEnabled(false);
  }

  // //////////////////////// script commands from JSViewer /////////////////


  /**
   * ScriptInterface requires this. In the applet, this would be queued
   */
  @Override
  public void runScript(String script) {
    // if (advancedApplet != null)
    // advancedApplet.runScript(script);
    // else
    runScriptNow(script);
  }


  /**
   * Writes a message to the status bar
   * 
   * @param msg
   *        the message
   */
  @Override
  public void writeStatus(String msg) {
    if (mainFrame != null)
      mainFrame.writeStatus(msg);
  }

  // /////////// JSApp/MainFrame ScriptInterface ////////////

  @Override
  public synchronized void syncToJmol(String msg) {
    if (jmol != null) { // MainFrame --> embedding application
      Logger.info("JSV>Jmol " + msg);
      jmol.syncScript(msg);
      return;
    }
  }

  @Override
  public boolean runScriptNow(String script) {
    return vwr.runScriptNow(script);
  }

  /**
   * From MainFrame only
   * 
   * @param isClosing
   * @param askFirst
   */
  public void notifyExitingJspecView(boolean isClosing, boolean askFirst) {
    // statusListener could be THIS
    statusListener.saveProperties(vwr.properties);
    if (isClosing) {
      statusListener.exitJSpecView(isClosing, mainFrame);
      // if here, we did not exit, but we should at least hide
      if (mainFrame != null)
        mainFrame.awaken(false);
    }
  }

  /**
   * StatusListener (Jmol will not actually close; we will here if stand-alone)
   */
  @Override
  public void exitJSpecView(boolean withDialog, Object frame) {
    if (withDialog && vwr.hasDisplay
        && JOptionPane.showConfirmDialog((Component) frame, "Exit JSpecView?",
            "Exit", JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION)
      return;
    dsp.getDisplaySchemes().remove("Current");
    exitNow();
  }

  private static void exitNow() {
    System.out.println("JSpecView exit");
    System.exit(0);
  }

}
