/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-07-13 14:51:46 -0500 (Wed, 13 Jul 2016) $
 * $Revision: 19253 $
 *
 * Copyright (C) 2002-2006  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.viewer;

import java.awt.Cursor;
import java.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.swing.SwingUtilities;

import org.jmol.adapter.readers.quantum.NBOParser;
import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.AtomIndexIterator;
import org.jmol.api.GenericMenuInterface;
import org.jmol.api.GenericMouseInterface;
import org.jmol.api.GenericPlatform;
import org.jmol.api.Interface;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolAnnotationParser;
import org.jmol.api.JmolAppConsoleInterface;
import org.jmol.api.JmolCallbackListener;
import org.jmol.api.JmolDataManager;
import org.jmol.api.JmolInChI;
import org.jmol.api.JmolJSpecView;
import org.jmol.api.JmolNMRInterface;
import org.jmol.api.JmolPropertyManager;
import org.jmol.api.JmolRendererInterface;
import org.jmol.api.JmolRepaintManager;
import org.jmol.api.JmolScriptEditorInterface;
import org.jmol.api.JmolScriptEvaluator;
import org.jmol.api.JmolScriptFunction;
import org.jmol.api.JmolScriptManager;
import org.jmol.api.JmolSelectionListener;
import org.jmol.api.JmolStatusListener;
import org.jmol.api.JmolViewer;
import org.jmol.api.PlatformViewer;
import org.jmol.api.SmilesMatcherInterface;
import org.jmol.api.SymmetryInterface;
import org.jmol.api.js.JSmolAppletObject;
import org.jmol.api.js.JmolToJSmolInterface;
import org.jmol.atomdata.AtomData;
import org.jmol.atomdata.AtomDataServer;
import org.jmol.atomdata.RadiusData;
import org.jmol.atomdata.RadiusData.EnumType;
import org.jmol.awtjs.Event;
import org.jmol.c.FIL;
import org.jmol.c.STER;
import org.jmol.c.STR;
import org.jmol.c.VDW;
import org.jmol.i18n.GT;
import org.jmol.minimize.Minimizer;
import org.jmol.modelkit.ModelKit;
import org.jmol.modelkit.ModelKitPopup;
import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.Bond;
import org.jmol.modelset.LabelToken;
import org.jmol.modelset.Measurement;
import org.jmol.modelset.MeasurementData;
import org.jmol.modelset.MeasurementPending;
import org.jmol.modelset.Model;
import org.jmol.modelset.ModelSet;
import org.jmol.modelset.Orientation;
import org.jmol.modelset.StateScript;
import org.jmol.modelsetbio.BioResolver;
import org.jmol.script.SV;
import org.jmol.script.ScriptContext;
import org.jmol.script.ScriptEval;
import org.jmol.script.T;
import org.jmol.thread.TimeoutThread;
import org.jmol.util.BSUtil;
import org.jmol.util.BoxInfo;
import org.jmol.util.C;
import org.jmol.util.CommandHistory;
import org.jmol.util.Elements;
import org.jmol.util.Escape;
import org.jmol.util.Font;
import org.jmol.util.GData;
import org.jmol.util.JmolMolecule;
import org.jmol.util.Logger;
import org.jmol.util.Node;
import org.jmol.util.Parser;
import org.jmol.util.Rectangle;
import org.jmol.util.TempArray;
import org.jmol.util.Triangulator;
import org.jmol.viewer.binding.Binding;

import javajs.api.GenericCifDataParser;
import javajs.util.AU;
import javajs.util.BS;
import javajs.util.CU;
import javajs.util.DF;
import javajs.util.JSJSONParser;
import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.OC;
import javajs.util.P3;
import javajs.util.P3i;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.Quat;
import javajs.util.Rdr;
import javajs.util.SB;
import javajs.util.T3;
import javajs.util.V3;
import javajs.util.ZipTools;

/*
 * 
 * ****************************************************************
 * The JmolViewer can be used to render client molecules. Clients implement the
 * JmolAdapter. JmolViewer uses this interface to extract information from the
 * client data structures and render the molecule to the supplied
 * java.awt.Component
 * 
 * The JmolViewer runs on Java 1.5+ virtual machines. The 3d graphics rendering
 * package is a software implementation of a z-buffer. It does not use Java3D
 * and does not use Graphics2D from Java 1.2.
 * 
 * public here is a test for applet-applet and JS-applet communication the idea
 * being that applet.getProperty("jmolViewer") returns this Viewer object,
 * allowing direct inter-process access to public methods.
 * 
 * e.g.
 * 
 * applet.getProperty("jmolApplet").getFullPathName()
 * 
 * 
 * This vwr can also be used with JmolData.jar, which is a 
 * frameless version of Jmol that can be used to batch-process
 * scripts from the command line. No shapes, no labels, no export
 * to JPG -- just raw data checking and output. 
 * 
 * 
 * NOSCRIPTING option: 2/2013
 * 
 * This option provides a smaller load footprint for JavaScript JSmol 
 * and disallows:
 * 
 *   scripting
 *   modelKitMode
 *   slabbing of read JVXL files
 *   calculate hydrogens
 *   
 * 
 * ****************************************************************
 */

public class Viewer extends JmolViewer
    implements AtomDataServer, PlatformViewer {

  public final static boolean nullDeletedAtoms = true; // Jmol 15.2+

  static public boolean isSwingJS = /** @j2sNative true|| */
      false;

  public boolean testAsync;// = true; // testing only

  @Override
  protected void finalize() throws Throwable {
    if (Logger.debugging)
      Logger.debug("vwr finalize " + this);
    super.finalize();
  }

  // these are all private now so we are certain they are not
  // being accesed by any other classes

  public boolean autoExit = false;
  public boolean haveDisplay = false;

  static public boolean isJS; // note that we now allow (isJS and !isApplet) 
  public boolean isApplet, isJNLP, isWebGL;

  public boolean isSingleThreaded;
  public boolean queueOnHold = false;

  public String fullName = "";
  public static String appletDocumentBase = "";
  public static String appletCodeBase = "";
  public static String appletIdiomaBase;

  public static String jsDocumentBase = "";

  public enum ACCESS {
    NONE, READSPT, ALL
  }

  public Object compiler;
  public Map<String, Object> definedAtomSets;
  public ModelSet ms;
  public FileManager fm;

  public boolean isSyntaxAndFileCheck = false;
  public boolean isSyntaxCheck = false;
  public boolean listCommands = false;
  boolean mustRender = false;

  public String htmlName = "";
  public String appletName = "";

  public int tryPt;

  private String insertedCommand = "";

  public void setInsertedCommand(String strScript) {
    insertedCommand = strScript;
  }

  public GData gdata;
  public JSmolAppletObject html5Applet; // j2s only - TODO: More explicit references of this 
  public static JmolToJSmolInterface jmolObject;

  public ActionManager acm;
  public AnimationManager am;
  public ColorManager cm;
  JmolDataManager dm;
  public ShapeManager shm;
  public SelectionManager slm;
  JmolRepaintManager rm;
  public GlobalSettings g;
  public StatusManager sm;
  public TransformManager tm;

  public static String strJavaVendor = "Java: "
      + System.getProperty("java.vendor", "j2s");
  public static String strOSName = System.getProperty("os.name", "");
  public static String strJavaVersion = "Java "
      + System.getProperty("java.version", "");

  String syncId = "";
  String logFilePath = "";

  private boolean allowScripting;
  public boolean isPrintOnly;
  public boolean isSignedApplet = false;
  private boolean isSignedAppletLocal = false;
  private boolean isSilent;
  private boolean multiTouch;
  public boolean noGraphicsAllowed;
  private boolean useCommandThread = false;

  private String commandOptions;
  public Map<String, Object> vwrOptions;
  public Object display;
  private JmolAdapter modelAdapter;
  private ACCESS access;
  private CommandHistory commandHistory;

  public ModelManager mm;
  public StateManager stm;
  private JmolScriptManager scm;
  public JmolScriptEvaluator eval;
  private TempArray tempArray;

  public boolean allowArrayDotNotation;
  public boolean async;
  public Object executor;

  private static String version_date;

  public static String getJmolVersion() {
    return (version_date == null
        ? version_date = (JC.version + "  " + JC.date).trim()
        : version_date);
  }

  /**
   * old way...
   * 
   * @param display
   * @param modelAdapter
   * @param fullName
   * @param documentBase
   * @param codeBase
   * @param commandOptions
   * @param statusListener
   * @param implementedPlatform
   * @return JmolViewer object
   */
  protected static JmolViewer allocateViewer(Object display,
                                             JmolAdapter modelAdapter,
                                             String fullName, URL documentBase,
                                             URL codeBase,
                                             String commandOptions,
                                             JmolStatusListener statusListener,
                                             GenericPlatform implementedPlatform) {

    Map<String, Object> info = new Hashtable<String, Object>();
    info.put("display", display);
    info.put("adapter", modelAdapter);
    info.put("statusListener", statusListener);
    info.put("platform", implementedPlatform);
    info.put("options", commandOptions);
    info.put("fullName", fullName);
    info.put("documentBase", documentBase);
    info.put("codeBase", codeBase);
    return new Viewer(info);
  }

  final Dimension dimScreen;
  final Lst<String> actionStates;
  final Lst<String> actionStatesRedo;
  VDW defaultVdw;

  public RadiusData rd;
  public Map<Object, Object> chainMap;
  private Lst<String> chainList;
  private String errorMessage;
  private String errorMessageUntranslated;
  private double privateKey;
  private boolean dataOnly;
  public boolean isJSNoAWT;

  /**
   * new way...
   * 
   * @param info
   *        "display" "adapter" "statusListener" "platform" "options" "fullName"
   *        "documentBase" "codeBase" "multiTouch" [options] "noGraphics"
   *        "printOnly" "previewOnly" "debug" "applet" "signedApplet"
   *        "appletProxy" "useCommandThread" "platform" [option]
   *        "backgroundTransparent" "exit" "listCommands" "check" "checkLoad"
   *        "silent" "access:READSPT" "access:NONE" "menuFile"
   *        "headlessMaxTimeMs" "headlessImage" "isDataOnly" "async"
   **/

  public Viewer(Map<String, Object> info) {
    commandHistory = new CommandHistory();
    dimScreen = new Dimension(0, 0);
    rd = new RadiusData(null, 0, null, null);
    defaultVdw = VDW.JMOL;
    localFunctions = new Hashtable<String, JmolScriptFunction>();
    privateKey = Math.random();
    actionStates = new Lst<String>();
    actionStatesRedo = new Lst<String>();
    chainMap = new Hashtable<Object, Object>();
    chainList = new Lst<String>();
    info.put("isJava", Boolean.TRUE);
    setOptions(info);
  }

  public boolean haveAccess(ACCESS a) {
    // disables WRITE, LOAD file:/, set logFile 
    // command line -g and -w options ARE available for final writing of image
    return access == a;
  }

  @Override
  public JmolAdapter getModelAdapter() {
    return (modelAdapter == null ? modelAdapter = new SmarterJmolAdapter()
        : modelAdapter);
  }

  @Override
  public BS getSmartsMatch(String smarts, BS bsSelected) throws Exception {
    if (bsSelected == null)
      bsSelected = bsA();
    return getSmilesMatcher().getSubstructureSet(smarts, ms.at, ms.ac,
        bsSelected, JC.SMILES_TYPE_SMARTS);
  }

  public BS getSmartsMatchForNodes(String smarts, Node[] atoms)
      throws Exception {
    return getSmilesMatcher().getSubstructureSet(smarts, atoms, atoms.length,
        null, JC.SMILES_TYPE_SMARTS);
  }

  /**
   * 
   * 
   * @param smilesOrSmarts
   * @param bsSelected
   * @param flags
   *        can be bitwise OR of JC.SMILES_* options, in particular,
   * 
   *        JC.SMILES_TYPE_SMARTS, JC.SMILES_TYPE_SMILES, and
   *        JC.SMILES_MAP_UNIQUE
   * 
   * @return map
   * @throws Exception
   */
  public int[][] getSmartsMap(String smilesOrSmarts, BS bsSelected, int flags)
      throws Exception {
    if (bsSelected == null)
      bsSelected = bsA();
    if (flags == 0)
      flags = JC.SMILES_TYPE_SMARTS;
    return getSmilesMatcher().getCorrelationMaps(smilesOrSmarts, ms.at, ms.ac,
        bsSelected, flags);
  }

  @SuppressWarnings({ "unchecked", "null", "unused" })
  public void setOptions(Map<String, Object> info) {

    vwrOptions = info;
    boolean isApp = info.containsKey("isApp");
    boolean isJava = info.containsKey("isJava");
    ThreadGroup group = Thread.currentThread().getThreadGroup();
    Map<String, Object> j2s_viewerOptions = (isApp ? null :
    /** @j2sNative group.秘html5Applet._viewerOptions || */
        null);
    if (j2s_viewerOptions != null) {
      if (isJava)
        j2s_viewerOptions.remove("display");
      vwrOptions.putAll(j2s_viewerOptions);
    }
    isApp |= info.containsKey("main");
    // use allocateViewer
    if (Logger.debugging) {
      Logger.debug("Viewer constructor " + this);
    }

    display = info.get("display");
    modelAdapter = (JmolAdapter) info.get("adapter");
    JmolStatusListener statusListener = (JmolStatusListener) info
        .get("statusListener");
    fullName = (String) info.get("fullName");
    if (fullName == null)
      fullName = "";
    Object o = info.get("codePath");
    if (o == null)
      o = "../java/";
    appletCodeBase = o.toString();
    appletIdiomaBase = appletCodeBase.substring(0,
        appletCodeBase.lastIndexOf("/", appletCodeBase.length() - 2) + 1)
        + "idioma";
    o = info.get("documentBase");
    appletDocumentBase = (o == null ? "" : o.toString());
    o = info.get("options");
    commandOptions = (o == null ? "" : o.toString());

    if (checkOption2("debug", "-debug"))
      Logger.setLogLevel(Logger.LEVEL_DEBUG);
    isJNLP = checkOption2("isJNLP", "-jnlp");
    if (isJNLP)
      Logger.info("setting JNLP mode TRUE");

    isSignedApplet = !isApp
        && (isJNLP || checkOption2("signedApplet", "-signed"));
    isApplet = !isApp && (isSignedApplet || checkOption2("applet", "-applet"));
    if (isApplet && info.containsKey("maximumSize"))
      setMaximumSize(((Integer) info.get("maximumSize")).intValue());
    allowScripting = !checkOption2("noscripting", "-noscripting");
    int i = fullName.indexOf("__");
    htmlName = (i < 0 ? fullName : fullName.substring(0, i));
    appletName = PT.split(htmlName + "_", "_")[0];
    syncId = (i < 0 ? "" : fullName.substring(i + 2, fullName.length() - 2));
    access = (checkOption2("access:READSPT", "-r") ? ACCESS.READSPT
        : checkOption2("access:NONE", "-R") ? ACCESS.NONE : ACCESS.ALL);
    isPreviewOnly = info.containsKey("previewOnly");
    if (isPreviewOnly)
      info.remove("previewOnly"); // see FilePreviewPanel
    isPrintOnly = checkOption2("printOnly", "-p");
    dataOnly = checkOption2("isDataOnly", "\0");
    autoExit = checkOption2("exit", "-x");
    o = info.get("platform");
    String platform = "unknown";
    if (o == null) {
      o = (commandOptions.contains("platform=")
          ? commandOptions.substring(commandOptions.indexOf("platform=") + 9)
          : "org.jmol.awt.Platform");
      // note that this must be the last option if give in commandOptions
    }
    if (o instanceof String) {
      platform = (String) o;
      if (platform == "")
        platform = (
        // could do this, but so far unnecessary isSwingJS ? "org.jmol.awtsw.Platform" : 
        "org.jmol.awt.Platform");
      isWebGL = (platform.indexOf(".awtjs.") >= 0);
      isJS = isSwingJS || isWebGL || (platform.indexOf(".awtjs2d.") >= 0);
      async = !dataOnly && !autoExit
          && (testAsync || isJS && info.containsKey("async"));
      JSmolAppletObject applet = null;
      String javaver = "?";

      JmolToJSmolInterface jmol = null;
      /**
       * @j2sNative
       * 
       *            if(self.Jmol) { jmol = Jmol; applet =
       *            Jmol._applets[this.htmlName.split("_object")[0]]; javaver =
       *            Jmol._version; applet && (applet._viewer = this); }
       * 
       * 
       */
      {
        javaver = null;
      }
      if (javaver != null) {
        jmolObject = jmol;
        html5Applet = applet;
        strJavaVersion = javaver;
        strJavaVendor = "Java2Script " + (isWebGL ? "(WebGL)" : "(HTML5)");
      }
      o = Interface.getInterface(platform, this, "setOptions");
    }
    apiPlatform = (GenericPlatform) o;
    isSingleThreaded = apiPlatform.isSingleThreaded();
    noGraphicsAllowed = checkOption2("noDisplay", "-n");
    headless = apiPlatform.isHeadless();
    haveDisplay = (isWebGL
        || display != null && !noGraphicsAllowed && !headless && !dataOnly);
    noGraphicsAllowed &= (display == null);
    headless |= noGraphicsAllowed;
    isJSNoAWT = isJS && isApplet && !isJava;
    if (haveDisplay) {
      mustRender = true;
      multiTouch = checkOption2("multiTouch", "-multitouch");
      if (isJSNoAWT) {
        /**
         * @j2sNative
         * 
         *            if (!this.isWebGL) this.display =
         *            document.getElementById(this.display);
         */
        {
        }
      }
    } else {
      display = null;
    }
    apiPlatform.setViewer(this, display);
    o = info.get("graphicsAdapter");
    if (o == null && !isWebGL)
      o = Interface.getOption("g3d.Graphics3D", this, "setOptions");
    gdata = (o == null && (isWebGL || isJSNoAWT || !isJS) ? new GData()
        : (GData) o);
    // intentionally throw an error here to restart the JavaScript async process
    gdata.initialize(this, apiPlatform);

    stm = new StateManager(this);
    cm = new ColorManager(this, gdata);
    sm = new StatusManager(this);
    boolean is4D = info.containsKey("4DMouse");
    tm = TransformManager.getTransformManager(this, Integer.MAX_VALUE, 0, is4D);
    slm = new SelectionManager(this);
    if (haveDisplay) {
      // must have language by now, as ActionManager uses GT.$()
      acm = (multiTouch
          ? (ActionManager) Interface.getOption("multitouch.ActionManagerMT",
              null, null)
          : new ActionManager());
      acm.setViewer(this,
          commandOptions + "-multitouch-" + info.get("multiTouch"));
      mouse = apiPlatform.getMouseManager(privateKey, display);
      if (multiTouch && !checkOption2("-simulated", "-simulated"))
        apiPlatform.setTransparentCursor(display);
    }
    mm = new ModelManager(this);
    shm = new ShapeManager(this);
    tempArray = new TempArray();
    am = new AnimationManager(this);
    o = info.get("repaintManager");
    if (o == null)
      o = Interface.getOption("render.RepaintManager", this, "setOptions");
    if (isJS || o != null && !o.equals(""))
      (rm = (JmolRepaintManager) o).set(this, shm);
    // again we through a JS error if in async mode
    ms = new ModelSet(this, null);
    initialize(true, false);
    fm = new FileManager(this);

    definedAtomSets = new Hashtable<String, Object>();
    setJmolStatusListener(statusListener);
    if (isApplet) {
      Logger.info("vwrOptions: \n" + Escape.escapeMap(vwrOptions));
      // Java only, because Signed applet can't find correct path when local.
      String path = (String) vwrOptions.get("documentLocation");
      if (!isJS && path != null && path.startsWith("file:/")) {
        path = path.substring(0,
            path.substring(0, (path + "?").indexOf("?")).lastIndexOf("/"));
        Logger.info("setting current directory to " + path);
        cd(path);
      }
      path = appletDocumentBase;
      i = path.indexOf("#");
      if (i >= 0)
        path = path.substring(0, i);
      i = path.lastIndexOf("?");
      if (i >= 0)
        path = path.substring(0, i);
      i = path.lastIndexOf("/");
      if (i >= 0)
        path = path.substring(0, i);
      jsDocumentBase = path;
      fm.setAppletContext(appletDocumentBase);
      String appletProxy = (String) info.get("appletProxy");
      if (appletProxy != null)
        setStringProperty("appletProxy", appletProxy);
      if (isSignedApplet) {
        logFilePath = PT.rep(appletCodeBase, "file://", "");
        logFilePath = PT.rep(logFilePath, "file:/", "");
        if (logFilePath.indexOf("//") >= 0)
          logFilePath = null;
        else
          isSignedAppletLocal = true;
      } else if (!isJS) {
        logFilePath = null;
      }
      new GT(this, (String) info.get("language"));
      // deferred here so that language is set
      if (isJS && haveDisplay)
        acm.createActions();
    } else {
      // not an applet -- used to pass along command line options
      gdata.setBackgroundTransparent(
          checkOption2("backgroundTransparent", "-b"));
      isSilent = checkOption2("silent", "-i");
      if (isSilent)
        Logger.setLogLevel(Logger.LEVEL_WARN); // no info, but warnings and
      if (headless && !isSilent)
        Logger.info("Operating headless display=" + display
            + " nographicsallowed=" + noGraphicsAllowed);
      // errors
      isSyntaxAndFileCheck = checkOption2("checkLoad", "-C");
      isSyntaxCheck = isSyntaxAndFileCheck || checkOption2("check", "-c");
      listCommands = checkOption2("listCommands", "-l");
      cd(".");
      if (headless) {
        headlessImageParams = (Map<String, Object>) info.get("headlessImage");
        o = info.get("headlistMaxTimeMs");
        if (o == null)
          o = Integer.valueOf(60000);
        setTimeout("" + Math.random(), ((Integer) o).intValue(), "exitJmol");
      }
    }
    useCommandThread = !isJS && !headless
        && checkOption2("useCommandThread", "-threaded");
    setStartupBooleans();
    setIntProperty("_nProcessors", nProcessors);
    /*
     * Logger.info("jvm11orGreater=" + jvm11orGreater + "\njvm12orGreater=" +
     * jvm12orGreater + "\njvm14orGreater=" + jvm14orGreater);
     */
    if (!isSilent) {
      Logger.info(JC.copyright + "\nJmol Version: " + getJmolVersion()
          + "\njava.vendor: " + strJavaVendor + "\njava.version: "
          + strJavaVersion + "\nos.name: " + strOSName + "\nAccess: " + access
          + "\nmemory: " + getP("_memory") + "\nprocessors available: "
          + nProcessors + "\nuseCommandThread: " + useCommandThread
          + (!isApplet ? ""
              : "\nappletId:" + htmlName
                  + (isSignedApplet ? " (signed)" : "")));
    }
    zap(false, true, false); // here to allow echos
    g.setO("language", GT.getLanguage());
    g.setO("_hoverLabel", hoverLabel);
    stm.setJmolDefaults();
    // this code will be shared between Jmol 14.0 and 14.1
    Elements.covalentVersion = Elements.RAD_COV_BODR_2014_02_22;
    allowArrayDotNotation = true;
    if (allowScripting)
      getScriptManager();
  }

  // //////////// screen/image methods ///////////////

  // final Rectangle rectClip = new Rectangle();

  private int maximumSize = Integer.MAX_VALUE;

  private void setMaximumSize(int x) {
    maximumSize = Math.max(x, 100);
  }

  /**
   * A graphics from a "slave" stereo display that has been synchronized with
   * this this applet.
   */
  private Object gRight;

  /**
   * A flag to indicate that THIS is the right-side panel of a pair of synced
   * applets running a left-right stereo display (that would be piped into a
   * dual-image polarized projector system such as GeoWall).
   * 
   */
  private boolean isStereoSlave;

  public void setStereo(boolean isStereoSlave, Object gRight) {
    this.isStereoSlave = isStereoSlave;
    this.gRight = gRight;
  }

  public float imageFontScaling = 1;

  public String getMenu(String type) {
    getPopupMenu();
    if (type.equals("\0")) {
      popupMenu(dimScreen.width - 120, 0, 'j');
      return "OK";
    }
    return (jmolpopup == null ? ""
        : jmolpopup.jpiGetMenuAsString(
            "Jmol version " + getJmolVersion() + "|_GET_MENU|" + type));
  }

  @Override
  public int[] resizeInnerPanel(int width, int height) {
    if (!autoExit && haveDisplay)
      return sm.resizeInnerPanel(width, height);
    setScreenDimension(width, height);
    return new int[] { dimScreen.width, dimScreen.height };
  }

  @Override
  public void setScreenDimension(int width, int height) {
    // There is a bug in Netscape 4.7*+MacOS 9 when comparing dimension objects
    // so don't try dim1.equals(dim2)
    height = Math.min(height, maximumSize);
    width = Math.min(width, maximumSize);
    if (tm.stereoDoubleFull)
      width = (width + 1) / 2;
    if (dimScreen.width == width && dimScreen.height == height)
      return;
    resizeImage(width, height, false, false, true);
  }

  void resizeImage(int width, int height, boolean isImageWrite,
                   boolean isExport, boolean isReset) {
    if (!isImageWrite && creatingImage)
      return;
    boolean wasAntialiased = antialiased;
    antialiased = (isReset
        ? g.antialiasDisplay && checkMotionRendering(T.antialiasdisplay)
        : isImageWrite && !isExport ? g.antialiasImages : false);
    if (!isExport && !isImageWrite
        && (width > 0 || wasAntialiased != antialiased))
      setShapeProperty(JC.SHAPE_LABELS, "clearBoxes", null);
    imageFontScaling = (antialiased ? 2f : 1f)
        * (isReset || tm.scale3D || width <= 0 ? 1
            : (g.zoomLarge == (height > width) ? height : width) * 1f
                / getScreenDim());
    if (width > 0) {
      dimScreen.width = width;
      dimScreen.height = height;
      if (!isImageWrite) {
        g.setI("_width", width);
        g.setI("_height", height);
        //        setStatusResized(width, height);
      }
    } else {
      width = (dimScreen.width == 0 ? dimScreen.width = 500 : dimScreen.width);
      height = (dimScreen.height == 0 ? dimScreen.height = 500
          : dimScreen.height);
    }
    tm.setScreenParameters(width, height,
        isImageWrite || isReset ? g.zoomLarge : false, antialiased, false,
        false);
    gdata.setWindowParameters(width, height, antialiased);
    if (width > 0 && !isImageWrite)
      setStatusResized(width, height);
  }

  @Override
  public int getScreenWidth() {
    return dimScreen.width;
  }

  @Override
  public int getScreenHeight() {
    return dimScreen.height;
  }

  public int getScreenDim() {
    return (g.zoomLarge == (dimScreen.height > dimScreen.width)
        ? dimScreen.height
        : dimScreen.width);
  }

  public void setWidthHeightVar() {
    g.setI("_width", dimScreen.width);
    g.setI("_height", dimScreen.height);
  }

  public int getBoundBoxCenterX() {
    // used by axes renderer
    return dimScreen.width / 2;
  }

  public int getBoundBoxCenterY() {
    return dimScreen.height / 2;
  }

  private boolean updateWindow(int width, int height) {
    if (!refreshing || creatingImage)
      return (refreshing ? false : !isJSNoAWT);
    if (isTainted || tm.slabEnabled)
      setModelVisibility();
    isTainted = false;
    if (rm != null) {
      if (width != 0)
        setScreenDimension(width, height);
    }
    return true;
  }

  /**
   * 
   * @param isDouble
   * @param isImageWrite
   * @return a java.awt.Image in the case of standard Jmol; an int[] in the case
   *         of Jmol-Android a canvas in the case of JSmol
   */
  private Object getImage(boolean isDouble, boolean isImageWrite) {
    Object image = null;
    try {
      beginRendering(isDouble, isImageWrite);
      render();
      gdata.endRendering();
      image = gdata.getScreenImage(isImageWrite);
    } catch (Error er) {
      gdata.getScreenImage(isImageWrite);
      handleError(er, false);
      setErrorMessage("Error during rendering: " + er, null);
    } catch (Exception e) {
      System.out.println("render error" + e);
    }
    return image;
  }

  private void beginRendering(boolean isDouble, boolean isImageWrite) {
    gdata.beginRendering(tm.getStereoRotationMatrix(isDouble), g.translucent,
        isImageWrite, !checkMotionRendering(T.translucent));
  }

  public boolean antialiased;

  private void render() {
    if (mm.modelSet == null || !mustRender || !refreshing && !creatingImage
        || rm == null)
      return;
    boolean antialias2 = antialiased && g.antialiasTranslucent;
    int[] navMinMax = shm.finalizeAtoms(tm.bsSelectedAtoms, true);
    if (isWebGL) {
      rm.renderExport(gdata, ms, jsParams);
      notifyViewerRepaintDone();
      return;
    }
    rm.render(gdata, ms, true, navMinMax);
    if (gdata.setPass2(antialias2)) {
      tm.setAntialias(antialias2);
      rm.render(gdata, ms, false, null);
      tm.setAntialias(antialiased);
    }
  }

  /**
   * 
   * @param graphic
   *        In JavaScript/HTML5, a Canvas.Context2d
   * @param img
   * @param x
   * @param y
   * @param isDTI
   *        DTI format -- scrunch width by factor of two
   */
  private void drawImage(Object graphic, Object img, int x, int y,
                         boolean isDTI) {
    if (graphic != null && img != null) {
      apiPlatform.drawImage(graphic, img, x, y, dimScreen.width,
          dimScreen.height, isDTI);
    }
    gdata.releaseScreenImage();
  }

  public Object getScreenImage() {
    return getScreenImageBuffer(null, true);
  }

  /**
   * Image.getJpgImage, ImageCreator.clipImage, getImageBytes,
   * Viewer.renderScreenImageStereo
   */
  @Override
  public Object getScreenImageBuffer(Object g, boolean isImageWrite) {
    if (isWebGL)
      return (isImageWrite
          ? apiPlatform.allocateRgbImage(0, 0, null, 0, false, true)
          : null);
    boolean isDouble = tm.stereoDoubleFull || tm.stereoDoubleDTI;
    boolean isBicolor = tm.stereoMode.isBiColor();
    boolean mergeImages = (g == null && isDouble);
    Object imageBuffer;
    if (isBicolor) {
      beginRendering(true, isImageWrite);
      render();
      gdata.endRendering();
      gdata.snapshotAnaglyphChannelBytes();
      beginRendering(false, isImageWrite);
      render();
      gdata.endRendering();
      gdata.applyAnaglygh(tm.stereoMode, tm.stereoColors);
      imageBuffer = gdata.getScreenImage(isImageWrite);
    } else {
      imageBuffer = getImage(isDouble, isImageWrite);
    }
    Object imageBuffer2 = null;
    if (mergeImages) {
      imageBuffer2 = apiPlatform.newBufferedImage(imageBuffer,
          (tm.stereoDoubleDTI ? dimScreen.width : dimScreen.width << 1),
          dimScreen.height);
      g = apiPlatform.getGraphics(imageBuffer2);
    }
    if (g == null)
      return imageBuffer;
    if (isDouble) {
      if (tm.stereoMode == STER.DTI) {
        drawImage(g, imageBuffer, dimScreen.width >> 1, 0, true);
        imageBuffer = getImage(false, false);
        drawImage(g, imageBuffer, 0, 0, true);
        g = null;
      } else {
        drawImage(g, imageBuffer, dimScreen.width, 0, false);
        imageBuffer = getImage(false, false);
      }
    }
    if (g != null)
      drawImage(g, imageBuffer, 0, 0, false);
    return (mergeImages ? imageBuffer2 : imageBuffer);
  }

  public synchronized Object evalStringWaitStatusQueued(String returnType,
                                                        String strScript,
                                                        String statusList,
                                                        boolean isQuiet,
                                                        boolean isQueued) {

    if (isJS && isApplet) {
      if (strScript.indexOf("JSCONSOLE") == 0) {
        jmolObject.showInfo(html5Applet, strScript.indexOf("CLOSE") < 0);
        if (strScript.indexOf("CLEAR") >= 0)
          jmolObject.clearConsole(html5Applet);
        return null;
      }

    }
    return (getScriptManager() == null ? null
        : scm.evalStringWaitStatusQueued(returnType, strScript, statusList,
            isQuiet, isQueued));
  }

  void popupMenu(int x, int y, char type) {
    if (!haveDisplay || !refreshing || isPreviewOnly || g.disablePopupMenu)
      return;
    switch (type) {
    case 'j':
      try {
        setCursor(Cursor.WAIT_CURSOR);
        // can throw error if not present; that's ok
        SwingUtilities.invokeLater(new Runnable() {

          @Override
          public void run() {
            getPopupMenu();
            jmolpopup.jpiShow(x, y);
            setCursor(Cursor.DEFAULT_CURSOR);
          }

        });
      } catch (Throwable e) {
        // no Swing -- tough luck!
        Logger.info(e.toString());
        g.disablePopupMenu = true;
      }
      break;
    case 'a':
    case 'b':
    case 'm':
      // atom, bond, or main -- ignored
      if (getModelkit(true) == null) {
        return;
      }
      modelkit.showMenu(x,y);
      break;
    }
  }

  public ModelKit getModelkit(boolean andShow) {
	    if (modelkit == null) {
	      (modelkit = (ModelKit) Interface
	          .getInterface("org.jmol.modelkit.ModelKit", this, "script"))
	              .setMenu((ModelKitPopup) apiPlatform.getMenuPopup(null, 'm'));
	    } else if (andShow) {
	      modelkit.updateMenu();
	    }
	    return modelkit;
	  }

  Object getPopupMenu() {
    if (g.disablePopupMenu)
      return null;
    if (jmolpopup == null) {
      jmolpopup = (allowScripting ? apiPlatform.getMenuPopup(menuStructure, 'j')
          : null);
      if (jmolpopup == null) {
        if (!async)
          g.disablePopupMenu = true;
        return null;
      } 
    }
    if (isJSNoAWT)
      checkMenuUpdate();
    return jmolpopup.jpiGetMenuAsObject();
  }

  @Override
  public void setMenu(String fileOrText, boolean isFile) {
    if (isFile)
      Logger
          .info("Setting menu " + (fileOrText.length() == 0 ? "to Jmol defaults"
              : "from file " + fileOrText));
    if (fileOrText.length() == 0)
      fileOrText = null;
    else if (isFile)
      fileOrText = getFileAsString3(fileOrText, false, null);
    getProperty("DATA_API", "setMenu", fileOrText);
    sm.setCallbackFunction("menu", fileOrText);
  }

  // // JavaScript callback methods for the applet

  /*
  * 
  * animFrameCallback echoCallback (defaults to messageCallback) errorCallback
  * evalCallback hoverCallback loadStructCallback measureCallback (defaults to
  * messageCallback) messageCallback (no local version) minimizationCallback
  * pickCallback resizeCallback scriptCallback (defaults to messageCallback)
  * syncCallback
  */

  /*
  * aniframeCallback is called:
  * 
  * -- each time a frame is changed -- whenever the animation state is changed
  * -- whenever the visible frame range is changed
  * 
  * jmolSetCallback("animFrameCallback", "myAnimFrameCallback") function
  * myAnimFrameCallback(frameNo, fileNo, modelNo, firstNo, lastNo) {}
  * 
  * frameNo == the current frame in fileNo == the current file number, starting
  * at 1 modelNo == the current model number in the current file, starting at 1
  * firstNo == flag1 * (the first frame of the set, in file * 1000000 + model
  * notation) lastNo == flag2 * (the last frame of the set, in file * 1000000 +
  * model notation)
  * 
  * where flag1 = 1 if animationDirection > 1 or -1 otherwise where flag2 = 1
  * if currentDirection > 1 or -1 otherwise
  * 
  * RepaintManager.setStatusFrameChanged RepaintManager.setAnimationOff
  * RepaintManager.setCurrentModelIndex RepaintManager.clearAnimation
  * RepaintManager.rewindAnimation RepaintManager.setAnimationLast
  * RepaintManager.setAnimationRelative RepaintManager.setFrameRangeVisible
  * Viewer.setCurrentModelIndex Eval.file Eval.frame Eval.load
  * Viewer.createImage (when creating movie frames with the WRITE FRAMES
  * command) Viewer.initializeModel
  */

  private int prevFrame = Integer.MIN_VALUE;
  private float prevMorphModel;

  /**
   * @param isVib
   * @param doNotify
   *        ignored; not implemented
   */
  void setStatusFrameChanged(boolean isVib, boolean doNotify) {
    if (isVib) {
      // force reset (reading vibrations)
      prevFrame = Integer.MIN_VALUE;
    }
    tm.setVibrationPeriod(Float.NaN);
    int firstIndex = am.firstFrameIndex;
    int lastIndex = am.lastFrameIndex;

    boolean isMovie = am.isMovie;
    int modelIndex = am.cmi;
    if (firstIndex == lastIndex && !isMovie)
      modelIndex = firstIndex;
    int frameID = getModelFileNumber(modelIndex);
    int currentFrame = am.cmi;
    int fileNo = frameID;
    int modelNo = frameID % 1000000;
    int firstNo = (isMovie ? firstIndex : getModelFileNumber(firstIndex));
    int lastNo = (isMovie ? lastIndex : getModelFileNumber(lastIndex));

    String strModelNo;
    if (isMovie) {
      strModelNo = "" + (currentFrame + 1);
    } else if (fileNo == 0) {
      strModelNo = getModelNumberDotted(firstIndex);
      if (firstIndex != lastIndex)
        strModelNo += " - " + getModelNumberDotted(lastIndex);
      if (firstNo / 1000000 == lastNo / 1000000)
        fileNo = firstNo;
    } else {
      strModelNo = getModelNumberDotted(modelIndex);
    }
    if (fileNo != 0)
      fileNo = (fileNo < 1000000 ? 1 : fileNo / 1000000);

    if (!isMovie) {
      g.setI("_currentFileNumber", fileNo);
      g.setI("_currentModelNumberInFile", modelNo);
    }
    float currentMorphModel = am.currentMorphModel;
    g.setI("_currentFrame", currentFrame);
    g.setI("_morphCount", am.morphCount);
    g.setF("_currentMorphFrame", currentMorphModel);
    g.setI("_frameID", frameID);
    g.setI("_modelIndex", modelIndex);
    g.setO("_modelNumber", strModelNo);
    g.setO("_modelName", (modelIndex < 0 ? "" : getModelName(modelIndex)));
    String title = (modelIndex < 0 ? "" : ms.getModelTitle(modelIndex));
    g.setO("_modelTitle", title == null ? "" : title);
    g.setO("_modelFile",
        (modelIndex < 0 ? "" : ms.getModelFileName(modelIndex)));
    g.setO("_modelType",
        (modelIndex < 0 ? "" : ms.getModelFileType(modelIndex)));

    if (currentFrame == prevFrame && currentMorphModel == prevMorphModel)
      return;
    prevFrame = currentFrame;
    prevMorphModel = currentMorphModel;

    String entryName = getModelName(currentFrame);
    if (isMovie) {
      entryName = "" + (entryName == "" ? currentFrame + 1 : am.caf + 1) + ": "
          + entryName;
    } else {
      String script = "" + getModelNumberDotted(currentFrame);
      if (!entryName.equals(script))
        entryName = script + ": " + entryName;
    }
    // there was a point where I thought frameNo and currentFrame
    // might be different. 
    sm.setStatusFrameChanged(fileNo, modelNo,
        (am.animationDirection < 0 ? -firstNo : firstNo),
        (am.currentDirection < 0 ? -lastNo : lastNo), currentFrame,
        currentMorphModel, entryName);
    if (doHaveJDX())
      getJSV().setModel(modelIndex);
    if (isJS && isApplet)
      updateJSView(modelIndex, -1);
  }

  // interaction with JSpecView

  private boolean haveJDX;
  private JmolJSpecView jsv;

  private boolean doHaveJDX() {
    // once-on, never off
    return (haveJDX
        || (haveJDX = getBooleanProperty("_JSpecView".toLowerCase())));
  }

  JmolJSpecView getJSV() {
    if (jsv == null) {
      jsv = (JmolJSpecView) Interface.getOption("jsv.JSpecView", this,
          "script");
      jsv.setViewer(this);
    }
    return jsv;
  }

  /**
   * get the model designated as "baseModel" in a JCamp-MOL file for example,
   * the model used for bonding for an XYZVIB file or the model used as the base
   * model for a mass spec file. This might then allow pointing off a peak in
   * JSpecView to switch to the model that is involved in HNMR or CNMR
   * 
   * @param modelIndex
   * 
   * @return modelIndex
   */

  public int getJDXBaseModelIndex(int modelIndex) {
    if (!doHaveJDX())
      return modelIndex;
    return getJSV().getBaseModelIndex(modelIndex);
  }

  public Object getJspecViewProperties(Object myParam) {
    // from getProperty("JSpecView...")
    Object o = sm.getJspecViewProperties("" + myParam);
    if (o != null)
      haveJDX = true;
    return o;
  }

  /*
  * echoCallback is one of the two main status reporting mechanisms. Along with
  * scriptCallback, it outputs to the console. Unlike scriptCallback, it does
  * not output to the status bar of the application or applet. If
  * messageCallback is enabled but not echoCallback, these messages go to the
  * messageCallback function instead.
  * 
  * jmolSetCallback("echoCallback", "myEchoCallback") function
  * myEchoCallback(app, message, queueState) {}
  * 
  * queueState = 1 -- queued queueState = 0 -- not queued
  * 
  * serves:
  * 
  * Eval.instructionDispatchLoop when app has -l flag
  * ForceField.steepestDescenTakeNSteps for minimization done
  * Viewer.setPropertyError Viewer.setBooleanProperty error
  * Viewer.setFloatProperty error Viewer.setIntProperty error
  * Viewer.setStringProperty error Viewer.showString adds a Logger.warn()
  * message Eval.showString calculate, cd, dataFrame, echo, error, getProperty,
  * history, isosurface, listIsosurface, pointGroup, print, set, show, write
  * ForceField.steepestDescentInitialize for initial energy
  * ForceField.steepestDescentTakeNSteps for minimization update
  * Viewer.showParameter
  */

  public void scriptEcho(String strEcho) {
    if (!Logger.isActiveLevel(Logger.LEVEL_INFO))
      return;
    if (isJS && !isSwingJS)
      System.out.println(strEcho);
    sm.setScriptEcho(strEcho, isScriptQueued());
    if (listCommands && strEcho != null && strEcho.indexOf("$[") == 0)
      Logger.info(strEcho);
  }

  private boolean isScriptQueued() {
    return scm != null && scm.isScriptQueued();
  }

  /*
  * errorCallback is a special callback that can be used to identify errors
  * during scripting and file i/o, and also indicate out of memory conditions
  * 
  * jmolSetCallback("errorCallback", "myErrorCallback") function
  * myErrorCallback(app, errType, errMsg, objectInfo, errMsgUntranslated) {}
  * 
  * errType == "Error" or "ScriptException" errMsg == error message, possibly
  * translated, with added information objectInfo == which object (such as an
  * isosurface) was involved errMsgUntranslated == just the basic message
  * 
  * Viewer.notifyError Eval.runEval on Error and file loading Exceptions
  * Viewer.handleError Eval.runEval on OOM Error Viewer.createModelSet on OOM
  * model initialization Error Viewer.getImage on OOM rendering Error
  */
  public void notifyError(String errType, String errMsg,
                          String errMsgUntranslated) {
    g.setO("_errormessage", errMsgUntranslated);
    sm.notifyError(errType, errMsg, errMsgUntranslated);
  }

  /*
  * evalCallback is a special callback that evaluates expressions in JavaScript
  * rather than in Jmol.
  * 
  * Viewer.jsEval Eval.loadScriptFileInternal Eval.Rpn.evaluateScript
  * Eval.script
  */

  public String jsEval(String strEval) {
    return "" + sm.jsEval(strEval);
  }

  public SV jsEvalSV(String strEval) {
    return SV.getVariable(isJS ? sm.jsEval(strEval) : jsEval(strEval));
  }

  /*
  * loadStructCallback indicates file load status.
  * 
  * jmolSetCallback("loadStructCallback", "myLoadStructCallback") function
  * myLoadStructCallback(fullPathName, fileName, modelName, errorMsg, ptLoad)
  * {}
  * 
  * ptLoad == JmolConstants.FILE_STATUS_NOT_LOADED == -1 ptLoad == JmolConstants.FILE_STATUS_ZAPPED == 0
  * ptLoad == JmolConstants.FILE_STATUS_CREATING_MODELSET == 2 ptLoad ==
  * JmolConstants.FILE_STATUS_MODELSET_CREATED == 3 ptLoad == JmolConstants.FILE_STATUS_MODELS_DELETED == 5
  * 
  * Only -1 (error loading), 0 (zapped), and 3 (model set created) messages are
  * passed on to the callback function. The others can be detected using
  * 
  * set loadStructCallback "jmolscript:someFunctionName"
  * 
  * At the time of calling of that method, the jmolVariable _loadPoint gives
  * the value of ptLoad. These load points are also recorded in the status
  * queue under types "fileLoaded" and "fileLoadError".
  * 
  * Viewer.setFileLoadStatus Viewer.createModelSet (2, 3)
  * Viewer.createModelSetAndReturnError (-1, 1, 4) Viewer.deleteAtoms (5)
  * Viewer.zap (0)
  */
  private void setFileLoadStatus(FIL ptLoad, String fullPathName,
                                 String fileName, String modelName,
                                 String strError, Boolean isAsync) {
    setErrorMessage(strError, null);
    g.setI("_loadPoint", ptLoad.getCode());
    boolean doCallback = (ptLoad != FIL.CREATING_MODELSET);
    if (doCallback)
      setStatusFrameChanged(false, false);
    sm.setFileLoadStatus(fullPathName, fileName, modelName, strError,
        ptLoad.getCode(), doCallback, isAsync);
    if (doCallback) {
      //       setStatusFrameChanged(false, true); // ensures proper title in JmolFrame but then we miss the file name
      if (doHaveJDX())
        getJSV().setModel(am.cmi);
      if (isJS && isApplet)
        updateJSView(am.cmi, -2);
    }

  }

  public String getZapName() {
    return (g.modelKitMode ? JC.MODELKIT_ZAP_TITLE : JC.ZAP_TITLE);
  }

  /*
  * measureCallback reports completed or pending measurements. Pending
  * measurements are measurements that the user has started but has not
  * completed -- this call comes when the user hesitates with the mouse over an
  * atom and the "rubber band" is showing
  * 
  * jmolSetCallback("measureCallback", "myMeasureCallback") function
  * myMeasureCallback(strMeasure, intInfo, status) {}
  * 
  * intInfo == (see below) status == "measurePicked" (intInfo == the number of
  * atoms in the measurement) "measureComplete" (intInfo == the current number
  * measurements) "measureDeleted" (intInfo == the index of the measurement
  * deleted or -1 for all) "measurePending" (intInfo == number of atoms picked
  * so far)
  * 
  * strMeasure:
  * 
  * For "set picking MEASURE ..." each time the user clicks an atom, a message
  * is sent to the pickCallback function (see below), and if the picking is set
  * to measure distance, angle, or torsion, then after the requisite number of
  * atoms is picked and the pick callback message is sent, a call is also made
  * to measureCallback with a string that indicates the measurement, such as:
  * 
  * Angle O #9 - Si #7 - O #2 : 110.51877
  * 
  * Under default conditions, when picking is not set to MEASURE, then
  * measurement reports are sent when the measure is completed, deleted, or
  * pending. These reports are in a psuedo array form that can be parsed more
  * easily, involving the atoms and measurement with units, for example:
  * 
  * [Si #3, O #8, Si #7, 60.1 <degrees mark>]
  * 
  * Viewer.setStatusMeasuring Measures.clear Measures.define
  * Measures.deleteMeasurement Measures.pending actionManager.atomPicked
  */

  public void setStatusMeasuring(String status, int intInfo, String strMeasure,
                                 float value) {

    // status           intInfo 

    // measureCompleted index
    // measurePicked    atom count
    // measurePending   atom count
    // measureDeleted   -1 (all) or index
    // measureSequence  -2
    sm.setStatusMeasuring(status, intInfo, strMeasure, value);
  }

  /*
  * minimizationCallback reports the status of a currently running
  * minimization.
  * 
  * jmolSetCallback("minimizationCallback", "myMinimizationCallback") function
  * myMinimizationCallback(app, minStatus, minSteps, minEnergy, minEnergyDiff)
  * {}
  * 
  * minStatus is one of "starting", "calculate", "running", "failed", or "done"
  * 
  * Viewer.notifyMinimizationStatus Minimizer.endMinimization
  * Minimizer.getEnergyonly Minimizer.startMinimization
  * Minimizer.stepMinimization
  */

  public void notifyMinimizationStatus() {
    Object step = getP("_minimizationStep");
    String ff = (String) getP("_minimizationForceField");
    sm.notifyMinimizationStatus((String) getP("_minimizationStatus"),
        step instanceof String ? Integer.valueOf(0) : (Integer) step,
        (Float) getP("_minimizationEnergy"),
        (step.toString().equals("0") ? Float.valueOf(0)
            : (Float) getP("_minimizationEnergyDiff")),
        ff);
  }

  /*
  * pickCallback returns information about an atom, bond, or DRAW object that
  * has been picked by the user.
  * 
  * jmolSetCallback("pickCallback", "myPickCallback") function
  * myPickCallback(strInfo, iAtom, map) {}
  * 
  * iAtom == the index of the atom picked or -2 for a draw object or -3 for a
  * bond
  * 
  * strInfo depends upon the type of object picked:
  * 
  * atom (iAtom>=0): a string determinied by the PICKLABEL parameter, which if "" delivers
  * the atom identity along with its coordinates
  * 
  * bond (iAtom==-3): ["bond", bondIdentityString (quoted), x, y, z] where the coordinates
  * are of the midpoint of the bond
  * 
  * draw (iAtom==-2): ["draw", ID(quoted), pickedModel, pickedVertex, x, y, z,
  * title(quoted)]
  * 
  * isosurface (iAtom==-4): ["isosurface", ID(quoted), pickedModel, pickedVertex, x, y, z,
  * title(quoted)]
  * 
  * map:
  * 
  * atom: null
  * 
  * bond: {pt, index, modelIndex, modelNumberDotted, type, strInfo}
  * 
  * Draw, isosurface: {pt, modelIndex, modelNumberDotted, id, vertex, type}
  * 
  * Viewer.setStatusAtomPicked Draw.checkObjectClicked (set picking DRAW)
  * Sticks.checkObjectClicked (set bondPicking TRUE; set picking IDENTIFY)
  * actionManager.atomPicked (set atomPicking TRUE; set picking IDENTIFY)
  * actionManager.queueAtom (during measurements)
  * 
  */

  public void setStatusAtomPicked(int atomIndex, String info,
                                  Map<String, Object> map, boolean andSelect) {
    // andSelect is never true
    if (andSelect)
      setSelectionSet(BSUtil.newAndSetBit(atomIndex));
    if (info == null) {
      info = g.pickLabel;
      info = (info.length() == 0
          ? getAtomInfoXYZ(atomIndex, g.messageStyleChime)
          : ms.getAtomInfo(atomIndex, info, ptTemp));
    }
    setPicked(atomIndex, false);
    if (atomIndex < 0) {
      Measurement m = getPendingMeasurement();
      if (m != null)
        info = info.substring(0, info.length() - 1) + ",\"" + m.getString()
            + "\"]";
    }
    g.setO("_pickinfo", info);
    sm.setStatusAtomPicked(atomIndex, info, map);
    if (atomIndex < 0)
      return;
    int syncMode = sm.getSyncMode();
    if (syncMode == StatusManager.SYNC_DRIVER && doHaveJDX())
      getJSV().atomPicked(atomIndex);
    if (isJS && isApplet)
      updateJSView(ms.at[atomIndex].mi, atomIndex);
  }

  @Override
  public Object getProperty(String returnType, String infoType,
                            Object paramInfo) {
    // accepts a BitSet paramInfo
    // return types include "JSON", "String", "readable", and anything else
    // returns the Java object.
    // Jmol 11.7.45 also uses this method as a general API
    // for getting and returning script data from the console and editor

    if (!"DATA_API".equals(returnType))
      return getPropertyManager().getProperty(returnType, infoType, paramInfo);

    switch (("scriptCheck........." // 0
        + "consoleText........." // 20
        + "scriptEditor........" // 40
        + "scriptEditorState..." // 60
        + "getAppConsole......." // 80
        + "getScriptEditor....." // 100
        + "setMenu............." // 120
        + "spaceGroupInfo......" // 140
        + "disablePopupMenu...." // 160
        + "defaultDirectory...." // 180
        + "getPopupMenu........" // 200
        + "shapeManager........" // 220
        + "getPreference......." // 240
    ).indexOf(infoType)) {

    case 0:
      return scriptCheckRet((String) paramInfo, true);
    case 20:
      return (appConsole == null ? "" : appConsole.getText());
    case 40:
      showEditor((String[]) paramInfo);
      return null;
    case 60:
      scriptEditorVisible = ((Boolean) paramInfo).booleanValue();
      return null;
    case 80:
      if (isKiosk) {
        appConsole = null;
      } else if (paramInfo instanceof JmolAppConsoleInterface) {
        appConsole = (JmolAppConsoleInterface) paramInfo;
      } else if (paramInfo != null && !((Boolean) paramInfo).booleanValue()) {
        appConsole = null;
      } else if (appConsole == null && paramInfo != null
          && ((Boolean) paramInfo).booleanValue()) {
        if (isJSNoAWT) {
          appConsole = (JmolAppConsoleInterface) Interface
              .getOption("consolejs.AppletConsole", this, "script");
        } else// if (isSwingJS) {
        // no applet console yet for SwingJS -- need DefaultStyledDocument
        //} else 
        {
          for (int i = 0, n = isSwingJS ? 1 : 4; i < n
              && appConsole == null; i++) {
            appConsole = (
            //  isApplet
            //  ? (JmolAppConsoleInterface) Interface
            //      .getOption("console.AppletConsole", null, null)
            //  : 
            (JmolAppConsoleInterface) Interface.getInterface(
                "org.openscience.jmol.app.jmolpanel.console.AppConsole", null,
                null));
            if (appConsole == null)
              try {
                System.out.println("Viewer can't start appConsole");
                /**
                 * @j2sNative break;
                 */
                Thread.currentThread().wait(100);
              } catch (InterruptedException e) {
                //
              }
          }

        }
        if (appConsole != null)
          appConsole.start(this);
      }
      scriptEditor = (isJS || appConsole == null ? null
          : appConsole.getScriptEditor());
      return appConsole;
    case 100:
      if (appConsole == null && paramInfo != null
          && ((Boolean) paramInfo).booleanValue()) {
        getProperty("DATA_API", "getAppConsole", Boolean.TRUE);
        scriptEditor = (appConsole == null ? null
            : appConsole.getScriptEditor());
      }
      return scriptEditor;
    case 120:
      if (jmolpopup != null)
        jmolpopup.jpiDispose();
      jmolpopup = null;
      return menuStructure = (String) paramInfo;
    case 140:
      return getSymTemp().getSpaceGroupInfo(ms, null, -1, false, null);
    case 160:
      g.disablePopupMenu = true; // no false here, because it's a
      // one-time setting
      return null;
    case 180:
      return g.defaultDirectory;
    case 200:
      if (paramInfo instanceof String)
        return getMenu((String) paramInfo);
      return getPopupMenu();
    case 220:
      return shm.getProperty(paramInfo);
    case 240:
      return sm.syncSend("getPreference", paramInfo, 1);
    }
    Logger.error("ERROR in getProperty DATA_API: " + infoType);
    return null;
  }

  public int notifyMouseClicked(int x, int y, int action, int mode) {
    // change y to 0 at bottom
    int modifiers = Binding.getButtonMods(action);
    int clickCount = Binding.getClickCount(action);
    g.setI("_mouseX", x);
    g.setI("_mouseY", dimScreen.height - y);
    g.setI("_mouseAction", action);
    g.setI("_mouseModifiers", modifiers);
    g.setI("_clickCount", clickCount);
    return sm.setStatusClicked(x, dimScreen.height - y, action, clickCount,
        mode);
  }

  private OutputManager outputManager;

  private OutputManager getOutputManager() {
    if (outputManager != null)
      return outputManager;
    return (outputManager = (OutputManager) Interface.getInterface(
        "org.jmol.viewer.OutputManager" + (isJSNoAWT ? "JS" : "Awt"), this,
        "file")).setViewer(this, privateKey);
  }

  //  private GenericZipTools jzt;
  //
  //  public GenericZipTools getJzt() {
  //    return (jzt == null
  //        ? jzt = (GenericZipTools) Interface.getInterface("javajs.util.ZipTools",
  //            this, "zip")
  //        : jzt);
  //  }
  //

  public void readFileAsMap(BufferedInputStream bis, Map<String, Object> map,
                            String name) {
    ZipTools.readFileAsMap(bis, map, name);
  }

  public String getZipDirectoryAsString(String fileName) {
    Object t = fm.getBufferedInputStreamOrErrorMessageFromName(fileName,
        fileName, false, false, null, false, true);
    return ZipTools.getZipDirectoryAsStringAndClose((BufferedInputStream) t);
  }

  /**
   * @return byte[] image, or null and an error message
   */
  @Override
  public byte[] getImageAsBytes(String type, int width, int height, int quality,
                                String[] errMsg) {
    return getOutputManager().getImageAsBytes(type, width, height, quality,
        errMsg);
  }

  @Override
  public void releaseScreenImage() {
    gdata.releaseScreenImage();
  }

  public void setDisplay(Object canvas) {
    // used by JSmol/HTML5 when a canvas is resized
    display = canvas;
    apiPlatform.setViewer(this, canvas);
  }

  public MeasurementData newMeasurementData(String id, Lst<Object> points) {
    return ((MeasurementData) Interface
        .getInterface("org.jmol.modelset.MeasurementData", this, "script"))
            .init(id, this, points);
  }

  private JmolDataManager getDataManager() {
    return (dm == null
        ? (dm = ((JmolDataManager) Interface
            .getInterface("org.jmol.viewer.DataManager", this, "script"))
                .set(this))
        : dm);
  }

  private JmolScriptManager getScriptManager() {
    if (allowScripting && scm == null) {
      scm = (JmolScriptManager) Interface
          .getInterface("org.jmol.script.ScriptManager", this, "setOptions");
      if (isJS && scm == null)
        throw new NullPointerException();
      if (scm == null) {
        allowScripting = false;
        return null;
      }
      eval = scm.setViewer(this);
      if (useCommandThread)
        scm.startCommandWatcher(true);
    }
    return scm;
  }

  private boolean checkOption2(String key1, String key2) {
    return (vwrOptions.containsKey(key1)
        && !vwrOptions.get(key1).toString().equals("false")
        || commandOptions.indexOf(key2) >= 0);
  }

  public boolean isPreviewOnly;

  /**
   * determined by GraphicsEnvironment.isHeadless() from java
   * -Djava.awt.headless=true
   * 
   * disables command threading
   * 
   * disables DELAY, TIMEOUT, PAUSE, LOOP, GOTO, SPIN <rate>, ANIMATION ON
   * 
   * turns SPIN <rate> <end> into just ROTATE <end>
   */

  public boolean headless;

  private void setStartupBooleans() {
    setBooleanProperty("_applet", isApplet);
    setBooleanProperty("_JSpecView".toLowerCase(), false);
    setBooleanProperty("_signedApplet", isSignedApplet);
    setBooleanProperty("_headless", headless);
    setStringProperty("_restrict", "\"" + access + "\"");
    setBooleanProperty("_useCommandThread", useCommandThread);
  }

  public String getExportDriverList() {
    return (haveAccess(ACCESS.ALL)
        ? (String) g.getParameter("exportDrivers", true)
        : "");
  }

  /**
   * end of life for this viewer
   */
  @Override
  public void dispose() {
    gRight = null;
    if (mouse != null) {
      acm.dispose();
      mouse.dispose();
      mouse = null;
    }
    clearScriptQueue();
    clearThreads();
    haltScriptExecution();
    if (scm != null)
      scm.clear(true);
    gdata.destroy();
    if (jmolpopup != null)
      jmolpopup.jpiDispose();
    if (modelkit != null)
      modelkit.dispose();
    try {
      if (appConsole != null) {
        appConsole.dispose();
        appConsole = null;
      }
      if (scriptEditor != null) {
        scriptEditor.dispose();
        scriptEditor = null;
      }
    } catch (Exception e) {
      // ignore -- Disposal was interrupted only in Eclipse
    }
  }

  public void reset(boolean includingSpin) {
    // Eval.reset()
    // initializeModel
    ms.calcBoundBoxDimensions(null, 1);
    axesAreTainted = true;
    tm.homePosition(includingSpin);
    if (ms.setCrystallographicDefaults())
      stm.setCrystallographicDefaults();
    else
      setAxesMode(T.axeswindow);
    prevFrame = Integer.MIN_VALUE;
    if (!tm.spinOn)
      setSync();
  }

  @Override
  public void homePosition() {
    evalString("reset spin");
  }

  /*
   * final Hashtable imageCache = new Hashtable();
   * 
   * void flushCachedImages() { imageCache.clear();
   * GData.flushCachedColors(); }
   */

  // ///////////////////////////////////////////////////////////////
  // delegated to StateManager
  // ///////////////////////////////////////////////////////////////

  public void initialize(boolean clearUserVariables, boolean isPyMOL) {
    g = new GlobalSettings(this, g, clearUserVariables);
    setStartupBooleans();
    setWidthHeightVar();
    if (haveDisplay) {
      g.setB("_is2D", isJS && !isWebGL);
      g.setB("_multiTouchClient", acm.isMTClient());
      g.setB("_multiTouchServer", acm.isMTServer());
    }
    cm.setDefaultColors(false);
    setObjectColor("background", "black");
    setObjectColor("axis1", "red");
    setObjectColor("axis2", "green");
    setObjectColor("axis3", "blue");

    // transfer default global settings to managers and g3d

    am.setAnimationOn(false);
    am.setAnimationFps(g.animationFps);
    sm.playAudio(null);
    sm.allowStatusReporting = g.statusReporting;
    setBooleanProperty("antialiasDisplay",
        (isPyMOL ? true : g.antialiasDisplay));
    stm.resetLighting();
    tm.setDefaultPerspective();
  }

  void saveModelOrientation() {
    ms.saveModelOrientation(am.cmi, stm.getOrientation());
  }

  void restoreModelOrientation(int modelIndex) {
    Orientation o = ms.getModelOrientation(modelIndex);
    if (o != null)
      o.restore(-1, true);
  }

  void restoreModelRotation(int modelIndex) {
    Orientation o = ms.getModelOrientation(modelIndex);
    if (o != null)
      o.restore(-1, false);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to TransformManager
  // ///////////////////////////////////////////////////////////////

  /**
   * This method is only called by JmolGLmol applet._refresh();
   * 
   * @return enough data to update a WebGL view
   * 
   */
  @SuppressWarnings("unused")
  public Object getGLmolView() {
    TransformManager tm = this.tm;
    T3 center = tm.fixedRotationCenter;
    Quat q = tm.getRotationQ();
    float xtrans = tm.xTranslationFraction;
    float ytrans = tm.yTranslationFraction;
    float scale = tm.scalePixelsPerAngstrom;
    float zoom = tm.zmPctSet;
    float cd = tm.cameraDistance;
    float pc = tm.screenPixelCount;
    boolean pd = tm.perspectiveDepth;
    int width = tm.width;
    int height = tm.height;

    /**
     * @j2sNative
     * 
     *            return { center:center, quaternion:q, xtrans:xtrans,
     *            ytrans:ytrans, scale:scale, zoom:zoom, cameraDistance:cd,
     *            pixelCount:pc, perspective:pd, width:width, height:height };
     */
    {
      return null;
    }
  }

  public void setRotationRadius(float angstroms, boolean doAll) {
    if (doAll)
      angstroms = tm.setRotationRadius(angstroms, false);
    // only set the rotationRadius if this is NOT a dataframe
    if (ms.setRotationRadius(am.cmi, angstroms))
      g.setF("rotationRadius", angstroms);
  }

  public void setCenterBitSet(BS bsCenter, boolean doScale) {
    // Eval
    // setCenterSelected
    if (isJmolDataFrame())
      return;
    tm.setNewRotationCenter(
        (BSUtil.cardinalityOf(bsCenter) > 0 ? ms.getAtomSetCenter(bsCenter)
            : null),
        doScale);
  }

  public void setNewRotationCenter(P3 center) {
    // eval CENTER command
    if (!isJmolDataFrame())
      tm.setNewRotationCenter(center, true);
  }

  void navigate(int keyWhere, int modifiers) {
    if (isJmolDataFrame())
      return;
    tm.navigateKey(keyWhere, modifiers);
    if (!tm.vibrationOn && keyWhere != 0)
      refresh(REFRESH_REPAINT, "Viewer:navigate()");
  }

  public void move(JmolScriptEvaluator eval, V3 dRot, float dZoom, V3 dTrans,
                   float dSlab, float floatSecondsTotal, int fps) {
    // from Eval
    tm.move(eval, dRot, dZoom, dTrans, dSlab, floatSecondsTotal, fps);
    moveUpdate(floatSecondsTotal);
  }

  public void moveTo(JmolScriptEvaluator eval, float floatSecondsTotal,
                     P3 center, V3 rotAxis, float degrees, M3 rotationMatrix,
                     float zoom, float xTrans, float yTrans,
                     float rotationRadius, P3 navCenter, float xNav, float yNav,
                     float navDepth, float cameraDepth, float cameraX,
                     float cameraY) {
    // from StateManager -- -1 for time --> no repaint
    if (!haveDisplay)
      floatSecondsTotal = 0;
    setTainted(true);
    tm.moveTo(eval, floatSecondsTotal, center, rotAxis, degrees, rotationMatrix,
        zoom, xTrans, yTrans, rotationRadius, navCenter, xNav, yNav, navDepth,
        cameraDepth, cameraX, cameraY);
  }

  public void moveUpdate(float floatSecondsTotal) {
    if (floatSecondsTotal > 0)
      requestRepaintAndWait("moveUpdate");
    else if (floatSecondsTotal == 0)
      setSync();
  }

  public void navigatePt(P3 center) {
    // isosurface setHeading
    tm.setNavigatePt(center);
    setSync();
  }

  public void navigateAxis(V3 rotAxis, float degrees) {
    // isosurface setHeading
    tm.navigateAxis(rotAxis, degrees);
    setSync();
  }

  public void navTranslatePercent(float x, float y) {
    if (isJmolDataFrame())
      return;
    tm.navTranslatePercentOrTo(0, x, y);
    setSync();
  }

  void zoomBy(int pixels) {
    // MouseManager.mouseSinglePressDrag
    //if (mouseEnabled)
    tm.zoomBy(pixels);
    refresh(REFRESH_SYNC, sm.syncingMouse ? "Mouse: zoomBy " + pixels : "");
  }

  void zoomByFactor(float factor, int x, int y) {
    // MouseManager.mouseWheel
    //if (mouseEnabled)
    tm.zoomByFactor(factor, x, y);
    refresh(REFRESH_SYNC,
        !sm.syncingMouse ? ""
            : "Mouse: zoomByFactor " + factor
                + (x == Integer.MAX_VALUE ? "" : " " + x + " " + y));
  }

  void rotateXYBy(float degX, float degY) {
    // mouseSinglePressDrag
    //if (mouseEnabled)
    tm.rotateXYBy(degX, degY, null);
    refresh(REFRESH_SYNC,
        sm.syncingMouse ? "Mouse: rotateXYBy " + degX + " " + degY : "");
  }

  public void spinXYBy(int xDelta, int yDelta, float speed) {
    //if (mouseEnabled)
    tm.spinXYBy(xDelta, yDelta, speed);
    if (xDelta == 0 && yDelta == 0)
      return;
    refresh(REFRESH_SYNC,
        sm.syncingMouse
            ? "Mouse: spinXYBy " + xDelta + " " + yDelta + " " + speed
            : "");
  }

  public void rotateZBy(int zDelta, int x, int y) {
    // mouseSinglePressDrag
    //if (mouseEnabled)
    tm.rotateZBy(zDelta, x, y);
    refresh(REFRESH_SYNC,
        sm.syncingMouse
            ? "Mouse: rotateZBy " + zDelta
                + (x == Integer.MAX_VALUE ? "" : " " + x + " " + y)
            : "");
  }

  void rotateSelected(float deltaX, float deltaY, BS bsSelected) {
    // bsSelected null comes from sync. 
    if (isJmolDataFrame())
      return;
    //if (mouseEnabled) {
    // "true" in setMovableBitSet call is necessary to implement set allowMoveAtoms
    tm.rotateXYBy(deltaX, deltaY, setMovableBitSet(bsSelected, true));
    refreshMeasures(true);
    //}
    //TODO: note that sync may not work with set allowRotateSelectedAtoms
    refresh(REFRESH_SYNC,
        sm.syncingMouse ? "Mouse: rotateMolecule " + deltaX + " " + deltaY
            : "");
  }

  public BS movableBitSet;

  private BS setMovableBitSet(BS bsSelected, boolean checkMolecule) {
    if (bsSelected == null)
      bsSelected = bsA();
    bsSelected = BSUtil.copy(bsSelected);
    BSUtil.andNot(bsSelected, getMotionFixedAtoms());
    if (checkMolecule && !g.allowMoveAtoms)
      bsSelected = ms.getMoleculeBitSet(bsSelected);
    return movableBitSet = bsSelected;
  }

  public void translateXYBy(int xDelta, int yDelta) {
    // mouseDoublePressDrag, mouseSinglePressDrag
    //if (mouseEnabled)
    tm.translateXYBy(xDelta, yDelta);
    refresh(REFRESH_SYNC,
        sm.syncingMouse ? "Mouse: translateXYBy " + xDelta + " " + yDelta : "");
  }

  @Override
  public void rotateFront() {
    // deprecated
    tm.resetRotation();
    refresh(REFRESH_REPAINT, "Viewer:rotateFront()");
  }

  public void translate(char xyz, float x, char type, BS bsAtoms) {
    int xy = (type == '\0' ? (int) x
        : type == '%' ? tm.percentToPixels(xyz, x)
            : tm.angstromsToPixels(x * (type == 'n' ? 10f : 1f)));
    if (bsAtoms != null) {
      if (xy == 0)
        return;
      tm.setSelectedTranslation(bsAtoms, xyz, xy);
    } else {
      switch (xyz) {
      case 'X':
      case 'x':
        if (type == '\0')
          tm.translateToPercent('x', x);
        else
          tm.translateXYBy(xy, 0);
        break;
      case 'Y':
      case 'y':
        if (type == '\0')
          tm.translateToPercent('y', x);
        else
          tm.translateXYBy(0, xy);
        break;
      case 'Z':
      case 'z':
        if (type == '\0')
          tm.translateToPercent('z', x);
        else
          tm.translateZBy(xy);
        break;
      }
    }
    refresh(REFRESH_REPAINT, "Viewer:translate()");
  }

  void slabByPixels(int pixels) {
    // MouseManager.mouseSinglePressDrag
    tm.slabByPercentagePoints(pixels);
    refresh(REFRESH_SYNC_MASK, "slabByPixels");
  }

  void depthByPixels(int pixels) {
    // MouseManager.mouseDoublePressDrag
    tm.depthByPercentagePoints(pixels);
    refresh(REFRESH_SYNC_MASK, "depthByPixels");

  }

  void slabDepthByPixels(int pixels) {
    // MouseManager.mouseSinglePressDrag
    tm.slabDepthByPercentagePoints(pixels);
    refresh(REFRESH_SYNC_MASK, "slabDepthByPixels");
  }

  //  @Override
  //  public M4 getUnscaledTransformMatrix() {
  //    // unused
  //    return tm.getUnscaledTransformMatrix();
  //  }

  public void finalizeTransformParameters() {
    // FrameRenderer
    // InitializeModel

    tm.finalizeTransformParameters();
    gdata.setSlabAndZShade(tm.slabValue, tm.depthValue,
        (tm.zShadeEnabled ? tm.zSlabValue : Integer.MAX_VALUE), tm.zDepthValue,
        g.zShadePower);
  }

  public float getScalePixelsPerAngstrom(boolean asAntialiased) {
    return tm.scalePixelsPerAngstrom
        * (asAntialiased || !antialiased ? 1f : 0.5f);
  }

  public void setSpin(String key, int value) {
    // Eval
    if (!PT.isOneOf(key, ";x;y;z;fps;X;Y;Z;FPS;"))
      return;
    int i = "x;y;z;fps;X;Y;Z;FPS".indexOf(key);
    switch (i) {
    case 0:
      tm.setSpinXYZ(value, Float.NaN, Float.NaN);
      break;
    case 2:
      tm.setSpinXYZ(Float.NaN, value, Float.NaN);
      break;
    case 4:
      tm.setSpinXYZ(Float.NaN, Float.NaN, value);
      break;
    case 6:
    default:
      tm.setSpinFps(value);
      break;
    case 10:
      tm.setNavXYZ(value, Float.NaN, Float.NaN);
      break;
    case 12:
      tm.setNavXYZ(Float.NaN, value, Float.NaN);
      break;
    case 14:
      tm.setNavXYZ(Float.NaN, Float.NaN, value);
      break;
    case 16:
      tm.setNavFps(value);
      break;
    }
    g.setI((i < 10 ? "spin" : "nav") + key, value);
  }

  public String getSpinState() {
    return getStateCreator().getSpinState(false);
  }

  /**
   * 
   * @param type
   * @param name
   * @param bs
   * @return String or Quat or P3[]
   */
  public Object getOrientationText(int type, String name, BS bs) {
    switch (type) {
    case T.volume:
    case T.unitcell:
    case T.best:
    case T.x:
    case T.y:
    case T.z:
    case T.quaternion:
      if (bs == null)
        bs = bsA();
      if (bs.isEmpty())
        return (type == T.volume ? "0"
            : type == T.unitcell ? null : new Quat());
      Object q = ms.getBoundBoxOrientation(type, bs);
      return (name == "best" && type != T.volume
          ? ((Quat) q).div(tm.getRotationQ())
          : q);
    case T.name:
      return stm.getSavedOrientationText(name);
    default:
      return tm.getOrientationText(type, name == "best");
    }
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to ColorManager
  // ///////////////////////////////////////////////////////////////

  public float[] getCurrentColorRange() {
    return cm.getPropertyColorRange();
  }

  private void setDefaultColors(boolean isRasmol) {
    cm.setDefaultColors(isRasmol);
    g.setB("colorRasmol", isRasmol);
    g.setO("defaultColorScheme", (isRasmol ? "rasmol" : "jmol"));
  }

  public void setElementArgb(int elementNumber, int argb) {
    // Eval
    g.setO("=color " + Elements.elementNameFromNumber(elementNumber),
        Escape.escapeColor(argb));
    cm.setElementArgb(elementNumber, argb);
  }

  @Override
  public void setVectorScale(float scale) {
    g.setF("vectorScale", scale);
    g.vectorScale = scale;
  }

  @Override
  public void setVibrationScale(float scale) {
    // Eval
    // public legacy in JmolViewer
    tm.setVibrationScale(scale);
    g.vibrationScale = scale;
    // because this is public:
    g.setF("vibrationScale", scale);
  }

  @Override
  public void setVibrationPeriod(float period) {
    // Eval
    tm.setVibrationPeriod(period);
    period = Math.abs(period);
    g.vibrationPeriod = period;
    // because this is public:
    g.setF("vibrationPeriod", period);
  }

  void setObjectColor(String name, String colorName) {
    if (colorName == null || colorName.length() == 0)
      return;
    setObjectArgb(name, CU.getArgbFromString(colorName));
  }

  public void setObjectVisibility(String name, boolean b) {
    int objId = StateManager.getObjectIdFromName(name);
    if (objId >= 0) {
      setShapeProperty(objId, "display", b ? Boolean.TRUE : Boolean.FALSE);
    }

  }

  public void setObjectArgb(String name, int argb) {
    int objId = StateManager.getObjectIdFromName(name);
    if (objId < 0) {
      if (name.equalsIgnoreCase("axes")) {
        setObjectArgb("axis1", argb);
        setObjectArgb("axis2", argb);
        setObjectArgb("axis3", argb);
      }
      return;
    }
    g.objColors[objId] = argb;
    switch (objId) {
    case StateManager.OBJ_BACKGROUND:
      gdata.setBackgroundArgb(argb);
      cm.setColixBackgroundContrast(argb);
      break;
    }
    g.setO(name + "Color", Escape.escapeColor(argb));
  }

  public void setBackgroundImage(String fileName, Object image) {
    g.backgroundImageFileName = fileName;
    gdata.setBackgroundImage(image);
  }

  public short getObjectColix(int objId) {
    int argb = g.objColors[objId];
    return (argb == 0 ? cm.colixBackgroundContrast : C.getColix(argb));
  }

  // for historical reasons, leave these two:

  @Override
  public void setColorBackground(String colorName) {
    setObjectColor("background", colorName);
  }

  @Override
  public int getBackgroundArgb() {
    return g.objColors[(StateManager.OBJ_BACKGROUND)];
  }

  /**
   * input here is a JC.SHAPE_xxxx identifier
   * 
   * @param iShape
   * @param name
   * @param mad10
   */
  public void setObjectMad10(int iShape, String name, int mad10) {
    int objId = StateManager
        .getObjectIdFromName(name.equalsIgnoreCase("axes") ? "axis" : name);
    if (objId < 0)
      return;
    if (mad10 == -2 || mad10 == -4) { // turn on if not set "showAxes = true"
      int m = mad10 + 3;
      mad10 = getObjectMad10(objId);
      if (mad10 == 0)
        mad10 = m;
    }
    g.setB("show" + name, mad10 != 0);
    g.objStateOn[objId] = (mad10 != 0);
    if (mad10 == 0)
      return;
    g.objMad10[objId] = mad10;
    setShapeSize(iShape, mad10, null); // just loads it
  }

  /**
   * 
   * @param objId
   * @return mad10
   */
  public int getObjectMad10(int objId) {
    return (g.objStateOn[objId] ? g.objMad10[objId] : 0);
  }

  public void setPropertyColorScheme(String scheme, boolean isTranslucent,
                                     boolean isOverloaded) {
    g.propertyColorScheme = scheme;
    if (scheme.startsWith("translucent ")) {
      isTranslucent = true;
      scheme = scheme.substring(12).trim();
    }
    cm.setPropertyColorScheme(scheme, isTranslucent, isOverloaded);
  }

  public String getLightingState() {
    return getStateCreator().getLightingState(true);
  }

  public P3 getColorPointForPropertyValue(float val) {
    // x = {atomno=3}.partialcharge.color
    return CU.colorPtFromInt(gdata.getColorArgbOrGray(cm.ce.getColorIndex(val)),
        null);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to SelectionManager
  // ///////////////////////////////////////////////////////////////

  public boolean hasSelected;
  
  public void select(BS bs, boolean isGroup, int addRemove, boolean isQuiet) {
    // Eval, ActionManager
    if (isGroup)
      bs = getUndeletedGroupAtomBits(bs);
    slm.select(bs, addRemove, isQuiet);
    shm.setShapeSizeBs(JC.SHAPE_STICKS, Integer.MAX_VALUE, null, null);
    hasSelected = true;
  }

  @Override
  public void setSelectionSet(BS set) {
    selectStatus(set, false, 0, true, true);
  }

  public void selectBonds(BS bs) {
    shm.setShapeSizeBs(JC.SHAPE_STICKS, Integer.MAX_VALUE, null, bs);
  }

  public void displayAtoms(BS bs, boolean isDisplay, boolean isGroup,
                           int addRemove, boolean isQuiet) {
    // Eval
    if (isGroup)
      bs = getUndeletedGroupAtomBits(bs);
    if (isDisplay)
      slm.display(ms, bs, addRemove, isQuiet);
    else
      slm.hide(ms, bs, addRemove, isQuiet);
  }

  private BS getUndeletedGroupAtomBits(BS bs) {
    bs = ms.getAtoms(T.group, bs);
    BSUtil.andNot(bs, slm.bsDeleted);
    return bs;
  }

  void reportSelection(String msg) {
    if (selectionHalosEnabled)
      setTainted(true);
    if (isScriptQueued() || g.debugScript)
      scriptStatus(msg);
  }

  private void clearAtomSets() {
    slm.setSelectionSubset(null);
    definedAtomSets.clear();
    if (haveDisplay)
      acm.exitMeasurementMode("clearAtomSets");
  }

  public BS getDefinedAtomSet(String name) {
    Object o = definedAtomSets.get(name.toLowerCase());
    return (o instanceof BS ? (BS) o : new BS());
  }

  @Override
  public void selectAll() {
    // initializeModel
    slm.selectAll(false);
  }

  @Override
  public void clearSelection() {
    // not used in this project; in jmolViewer interface, though
    slm.clearSelection(true);
    g.setB("hideNotSelected", false);
  }

  public BS bsA() {
    return slm.getSelectedAtoms();
  }

  @Override
  public void addSelectionListener(JmolSelectionListener listener) {
    slm.addListener(listener);
  }

  @Override
  public void removeSelectionListener(JmolSelectionListener listener) {
    slm.addListener(listener);
  }

  BS getAtomBitSetEval(JmolScriptEvaluator eval, Object atomExpression) {
    return (allowScripting
        ? getScriptManager().getAtomBitSetEval(eval, atomExpression)
        : new BS());
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to Mouse (part of the apiPlatform system), 
  // ///////////////////////////////////////////////////////////////

  /**
   * either org.jmol.awt.Mouse or org.jmol.awtjs2d.Mouse
   */
  private GenericMouseInterface mouse;

  public void processTwoPointGesture(float[][][] touches) {
    mouse.processTwoPointGesture(touches);
  }

  public boolean processMouseEvent(int id, int x, int y, int modifiers,
                                   long time) {
    // also used for JavaScript from jQuery
    return mouse.processEvent(id, x, y, modifiers, time);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to ActionManager
  // ///////////////////////////////////////////////////////////////

  public Rectangle getRubberBandSelection() {
    return (haveDisplay ? acm.getRubberBand() : null);
  }

  public boolean isBound(int mouseAction, int jmolAction) {
    return (haveDisplay && acm.bnd(mouseAction, jmolAction));

  }

  public int getCursorX() {
    return (haveDisplay ? acm.getCurrentX() : 0);
  }

  public int getCursorY() {
    return (haveDisplay ? acm.getCurrentY() : 0);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to FileManager
  // ///////////////////////////////////////////////////////////////

  public String getDefaultDirectory() {
    return g.defaultDirectory;
  }

  public String getLocalUrl(String fileName) {
    return apiPlatform.getLocalUrl(fileName);
  }

  public String getFileAsString(String fileName) {
    return getAsciiFileOrNull(fileName);
  }

  @Override
  public BufferedInputStream getBufferedInputStream(String fullPathName) {
    // used by some JVXL readers, also OutputManager.writeZipFile and ScriptManager.openFileAsync
    return fm.getBufferedInputStream(fullPathName);
  }

  public Map<String, Object> setLoadParameters(Map<String, Object> htParams,
                                               boolean isAppend) {
    if (htParams == null)
      htParams = new Hashtable<String, Object>();
    htParams.put("vwr", this);
    if (g.atomTypes.length() > 0)
      htParams.put("atomTypes", g.atomTypes);
    if (!htParams.containsKey("lattice"))
      htParams.put("lattice", g.ptDefaultLattice);
    if (g.applySymmetryToBonds)
      htParams.put("applySymmetryToBonds", Boolean.TRUE);
    if (g.pdbGetHeader)
      htParams.put("getHeader", Boolean.TRUE);
    if (g.pdbSequential)
      htParams.put("isSequential", Boolean.TRUE);
    if (g.legacyJavaFloat)
      htParams.put("legacyJavaFloat", Boolean.TRUE);
    htParams.put("stateScriptVersionInt",
        Integer.valueOf(stateScriptVersionInt));
    if (!htParams.containsKey("filter")) {
      String filter = g.defaultLoadFilter;
      if (filter.length() > 0)
        htParams.put("filter", filter);
    }
    boolean merging = (isAppend && !g.appendNew && ms.ac > 0);
    htParams.put("baseAtomIndex", Integer.valueOf(isAppend ? ms.ac : 0));
    htParams.put("baseBondIndex", Integer.valueOf(isAppend ? ms.bondCount : 0));
    htParams.put("baseModelIndex",
        Integer.valueOf(ms.ac == 0 ? 0 : ms.mc + (merging ? -1 : 0)));
    if (merging)
      htParams.put("merging", Boolean.TRUE);
    return htParams;
  }

  // //////////////// methods that open a file to create a model set ///////////

  //  *indicates when a refresh is made (external apps and applets only)
  //  
  //  external apps only 
  //    via loadInline(List)*
  //      createModelSetAndReturnError
  //  
  //  openDOM, openReader, openFile, openFiles
  //    via loadModelFromFileRepaint*
  //      createModelSetAndReturnError
  //  
  //  loadInLine(String) via loadInLineScriptRepaint*
  //  FileDropper (string drop) via openStringInline*
  //    via openStringInlineParamsAppend
  //      createModelSetAndReturnError
  //
  //  external apps, applet only, via loadInline(String[])*
  //    via openStringsInlineParamsAppend
  //      createModelSetAndReturnError
  //
  //  script LOAD
  //    via loadModelFromFile
  //      createModelSetAndReturnError
  //      
  //  script CALCULATE HYDROGENS, PLOT, ZAP (modelkit)
  //    via openStringInlineParamsAppend
  //      createModelSetAndReturnError
  //  
  //  script LOAD DATA via loadFileFull and loadInlineScript
  //    openStringsInlineParamsAppend
  //      createModelSetAndReturnError

  /**
   * opens a file as a model, a script, or a surface via the creation of a
   * script that is queued \t at the beginning disallows script option - used by
   * JmolFileDropper and JmolPanel file-open actions - sets up a script to load
   * the file.
   * 
   * Called from (JSmolCore.js)Jmol.$appEvent(,,"drop").reader.onloadend()
   * 
   * @param fileName
   * @param flags
   *        1=pdbCartoons, 2=no scripting, 4=append, 8=fileOpen, 16=fileDropped
   * 
   */
  @Override
  public void openFileAsyncSpecial(String fileName, int flags) {
    getScriptManager().openFileAsync(fileName, flags, false);
  }

  public void openFileDropped(String fname, boolean checkDims) {
    getScriptManager().openFileAsync(fname, JmolScriptManager.FILE_DROPPED,
        checkDims);
  }

  /**
   * 
   * for JmolSimpleViewer -- external applications only (and no-script
   * JavaScript)
   * 
   * @param fileName
   * @return null or error
   */
  @Override
  public String openFile(String fileName) {
    zap(true, true, false);
    return loadModelFromFileRepaint(null, fileName, null, null);
  }

  /**
   * for JmolSimpleViewer -- external applications only
   * 
   * @param fileNames
   * @return null or error
   */
  @Override
  public String openFiles(String[] fileNames) {
    zap(true, true, false);
    return loadModelFromFileRepaint(null, null, fileNames, null);
  }

  /**
   * Opens the file, given an already-created reader.
   * 
   * @param fullPathName
   * @param fileName
   *        name without path or can just be null
   * @param reader
   *        could be Reader, BufferedInputStream, or byte[]
   * @return null or error message
   */
  @Override
  public String openReader(String fullPathName, String fileName,
                           Object reader) {
    zap(true, true, false);
    return loadModelFromFileRepaint(fullPathName, fileName, null, reader);
  }

  /**
   * applet DOM method -- does not preserve state
   * 
   * @param DOMNode
   * @return null or error
   * 
   */
  @Override
  public String openDOM(Object DOMNode) {
    // applet.loadDOMNode
    zap(true, true, false);
    return loadModelFromFileRepaint("?", "?", null, DOMNode);
  }

  /**
   * 
   * for JmolSimpleViewer -- external applications only (and no-script
   * JavaScript)
   * 
   * @param fullPathName
   * @param fileName
   * @param fileNames
   * @param reader
   * @return error message or null
   * 
   */
  private String loadModelFromFileRepaint(String fullPathName, String fileName,
                                          String[] fileNames, Object reader) {
    String ret = loadModelFromFile(fullPathName, fileName, fileNames, reader,
        false, null, null, null, 0, " ");
    refresh(REFRESH_REPAINT, "loadModelFromFileRepaint");
    return ret;
  }

  /**
   * Used by the ScriptEvaluator LOAD command to open one or more files. Now
   * necessary for EVERY load of a file, as loadScript must be passed to the
   * ModelLoader.
   * 
   * @param fullPathName
   *        may be null; used only when reader != null
   * @param fileName
   *        must not be null
   * @param fileNames
   *        when present, reader is ignored
   * @param reader
   *        may be a Reader, BufferedReader, byte[], or BufferedInputStream
   * @param isAppend
   * @param htParams
   * @param loadScript
   * @param sOptions
   * @param tokType
   * @param filecat
   *        + or null, -, or space
   * @return null or error
   */
  public String loadModelFromFile(String fullPathName, String fileName,
                                  String[] fileNames, Object reader,
                                  boolean isAppend,
                                  Map<String, Object> htParams, SB loadScript,
                                  SB sOptions, int tokType, String filecat) {
    if (htParams == null)
      htParams = setLoadParameters(null, isAppend);
    if (tokType != T.nada)
      htParams.put("dataType", T.nameOf(tokType));
    if (filecat != " ")
      htParams.put("concatenate", Boolean.TRUE);
    Object atomSetCollection;
    String[] saveInfo = fm.getFileInfo();

    // testing only reader = fm.getFileAsBytes(fileName,  null);
    if (fileNames != null) {

      // 1) a set of file names

      if (loadScript == null) {
        loadScript = new SB().append("load files");
        for (int i = 0; i < fileNames.length; i++)
          loadScript.append(i == 0 || filecat == null ? " " : filecat)
              .append("/*file*/$FILENAME" + (i + 1) + "$");
        if (sOptions.length() > 0)
          loadScript.append(" /*options*/ ").append(sOptions.toString());
      }
      long timeBegin = System.currentTimeMillis();

      atomSetCollection = fm.createAtomSetCollectionFromFiles(fileNames,
          setLoadParameters(htParams, isAppend), isAppend);
      long ms = System.currentTimeMillis() - timeBegin;
      Logger.info("openFiles(" + fileNames.length + ") " + ms + " ms");
      fileNames = (String[]) htParams.get("fullPathNames");
      String[] fileTypes = (String[]) htParams.get("fileTypes");
      String s = loadScript.toString();
      for (int i = 0; i < fileNames.length; i++) {
        String fname = fileNames[i];
        if (fileTypes != null && fileTypes[i] != null)
          fname = fileTypes[i] + "::" + fname;
        s = PT.rep(s, "$FILENAME" + (i + 1) + "$",
            PT.esc(FileManager.fixDOSName(fname)));
      }

      loadScript = new SB().append(s);

    } else if (reader == null) {

      // 2) a standard, single file 

      if (loadScript == null)
        loadScript = new SB().append("load /*file*/$FILENAME$");

      atomSetCollection = openFileFull(fileName, isAppend, htParams,
          loadScript);

    } else if (reader instanceof Reader || reader instanceof BufferedInputStream
        || AU.isAB(reader)) {

      // 3) a file reader, BufferedInputStream, or byte[] (not used by Jmol) 

      atomSetCollection = fm.createAtomSetCollectionFromReader(fullPathName,
          fileName, reader, setLoadParameters(htParams, isAppend));

    } else {

      // 4) a DOM reader (could be used by Jmol) 

      atomSetCollection = fm.createAtomSetCollectionFromDOM(reader,
          setLoadParameters(htParams, isAppend));

    }

    // OK, the file has been read and is now closed.

    if (tokType != 0) { // all we are doing is reading atom data
      fm.setFileInfo(saveInfo);
      return loadAtomDataAndReturnError(atomSetCollection, tokType);
    }

    if (htParams.containsKey("isData"))
      return (String) atomSetCollection;

    // now we fix the load script (possibly) with the full path name
    if (loadScript != null && !(atomSetCollection instanceof String)) {
      String fname = (String) htParams.get("fullPathName");
      if (fname == null)
        fname = "";
      // may have been modified.
      if (htParams.containsKey("loadScript"))
        loadScript = (SB) htParams.get("loadScript");
      htParams.put("loadScript",
          loadScript = new SB().append(javajs.util.PT.rep(loadScript.toString(),
              "$FILENAME$", PT.esc(FileManager.fixDOSName(fname)))));
    }

    // and finally to create the model set...

    return createModelSetAndReturnError(atomSetCollection, isAppend, loadScript,
        htParams);
  }

  Map<String, Object> ligandModels;
  Map<String, Boolean> ligandModelSet;

  public void setLigandModel(String key, String data) {
    if (ligandModels == null)
      ligandModels = new Hashtable<String, Object>();
    ligandModels.put(key, data);
  }

  /**
   * obtain CIF data for a ligand for purposes of adding hydrogens or for any
   * other purpose in terms of saving a data set for a file in a state
   * 
   * @param id
   *        unique key; if null, clear "bad" entries from the set.
   * @param prefix
   * @param suffix
   *        or fileName
   * @param terminator
   *        Only save to this if not null
   * @return a ligand model or a string if just file data or null
   */
  public Object getLigandModel(String id, String prefix, String suffix,
                               String terminator) {
    // getLigandModel(id, m40File, "_file", "----"));
    // getLigandModel(group3, "ligand_", "_data", null);
    if (id == null) {
      if (ligandModelSet != null) {
        Iterator<Map.Entry<String, Object>> e = ligandModels.entrySet()
            .iterator();
        while (e.hasNext()) {
          Entry<String, Object> entry = e.next();
          if (entry.getValue() instanceof Boolean)
            e.remove();
        }
      }
      return null;
    }
    id = id.replace('\\', '/');
    boolean isLigand = prefix.equals("ligand_");
    id = (id.indexOf("/cif") >= 0 ? id
        : isLigand ? id.toUpperCase() : id.substring(id.lastIndexOf("/") + 1));
    if (ligandModelSet == null)
      ligandModelSet = new Hashtable<String, Boolean>();
    ligandModelSet.put(id, Boolean.TRUE);
    if (ligandModels == null)
      ligandModels = new Hashtable<String, Object>();
    int pngPt = id.indexOf("|");
    if (pngPt >= 0)
      id = id.substring(id.indexOf("|") + 1);
    Object model = (terminator == null ? ligandModels.get(id) : null);
    String data;
    String fname = null;
    if (model instanceof Boolean)
      return null;
    if (model == null && (terminator == null || pngPt >= 0))
      model = ligandModels.get(id + suffix);
    boolean isError = false;
    boolean isNew = (model == null);
    if (isNew) {
      String s;
      if (isLigand) {
        fname = (String) setLoadFormat("#" + id, '#', false);
        if (fname.length() == 0)
          return null;
        scriptEcho("fetching " + fname);
        s = getFileAsString3(fname, false, null);
      } else {
        scriptEcho("fetching " + prefix);
        s = getFileAsString3(prefix, false, null);
        int pt = (terminator == null ? -1 : s.indexOf(terminator));
        if (pt >= 0)
          s = s.substring(0, pt);
      }
      isError = (s.indexOf("java.") == 0);
      model = s;
      if (!isError)
        ligandModels.put(id + suffix, model);
    }
    if (!isLigand) {
      if (!isNew)
        scriptEcho(prefix + " loaded from cache");
      return model;
    }
    // process ligand business

    if (!isError && model instanceof String) {
      data = (String) model;
      // TODO: check for errors in reading file
      if (data.length() != 0) {
        Map<String, Object> htParams = new Hashtable<String, Object>();
        htParams.put("modelOnly", Boolean.TRUE);
        model = getModelAdapter().getAtomSetCollectionReader("ligand", null,
            Rdr.getBR(data), htParams);
        isError = (model instanceof String);
        if (!isError) {
          model = getModelAdapter().getAtomSetCollection(model);
          isError = (model instanceof String);
          if (fname != null && !isError)
            scriptEcho((String) getModelAdapter()
                .getAtomSetCollectionAuxiliaryInfo(model).get("modelLoadNote"));
        }
      }
    }
    if (isError) {
      scriptEcho(model.toString());
      ligandModels.put(id, Boolean.FALSE);
      return null;
    }
    return model;
  }

  /**
   * 
   * does NOT repaint
   * 
   * @param fileName
   * @param isAppend
   * @param htParams
   * @param loadScript
   *        only necessary for string reading
   * @return an AtomSetCollection or a String (error)
   */
  private Object openFileFull(String fileName, boolean isAppend,
                              Map<String, Object> htParams, SB loadScript) {
    if (fileName == null)
      return null;
    if (fileName.equals("String[]")) {
      // no reloading of string[] or file[] data -- just too complicated
      return null;
    }
    Object atomSetCollection;
    String msg = "openFile(" + fileName + ")";
    Logger.startTimer(msg);
    htParams = setLoadParameters(htParams, isAppend);
    boolean isLoadVariable = fileName.startsWith("@");
    boolean haveFileData = (htParams.containsKey("fileData"));
    if (fileName.indexOf('$') == 0)
      htParams.put("smilesString", fileName.substring(1));
    boolean isString = (fileName.equals("string")
        || fileName.equals(JC.MODELKIT_ZAP_TITLE));
    String strModel = null;
    if (haveFileData) {
      strModel = (String) htParams.get("fileData");
      if (htParams.containsKey("isData")) {
        Object o = loadInlineScript(strModel, '\0', isAppend, htParams);
        lastData = (g.preserveState ? getDataManager().createFileData(strModel)
            : null);
        return o;
      }
    } else if (isString) {
      strModel = ms.getInlineData(-1);
      if (strModel == null)
        if (g.modelKitMode)
          strModel = JC.MODELKIT_ZAP_STRING;
        else
          return "cannot find string data";
      if (loadScript != null)
        htParams.put("loadScript",
            loadScript = new SB().append(PT.rep(loadScript.toString(),
                "/*file*/$FILENAME$", "/*data*/data \"model inline\"\n"
                    + strModel + "end \"model inline\"")));
    }
    if (strModel != null) {
      if (!isAppend)
        zap(true, false/*true*/, false);
      if (!isLoadVariable && (!haveFileData || isString))
        getStateCreator().getInlineData(loadScript, strModel, isAppend, 
            (Integer) htParams.get("appendToModelIndex"),
            g.defaultLoadFilter);
      atomSetCollection = fm.createAtomSetCollectionFromString(strModel,
          htParams, isAppend);
    } else {

      // if the filename has a "?" at the beginning, we don't zap, 
      // because the user might cancel the operation.

      atomSetCollection = fm.createAtomSetCollectionFromFile(fileName, htParams,
          isAppend);
    }
    Logger.checkTimer(msg, false);
    return atomSetCollection;
  }

  /**
   * only used by file dropper.
   */

  @Override
  public String openStringInline(String strModel) {
    // JmolSimpleViewer; JmolFileDropper inline string event
    String ret = openStringInlineParamsAppend(strModel, null, false);
    refresh(REFRESH_REPAINT, "openStringInline");
    return ret;
  }

  /**
   * from Applet and external applications only
   */

  @Override
  public String loadInline(String strModel) {
    // jmolViewer interface
    return loadInlineScriptRepaint(strModel, g.inlineNewlineChar, false);
  }

  /**
   * external apps only
   * 
   */

  @Override
  public String loadInline(String strModel, char newLine) {
    // JmolViewer interface
    return loadInlineScriptRepaint(strModel, newLine, false);
  }

  /**
   * used by applet and console
   */

  @Override
  public String loadInlineAppend(String strModel, boolean isAppend) {
    // JmolViewer interface
    return loadInlineScriptRepaint(strModel, '\0', isAppend);
  }

  private String loadInlineScriptRepaint(String strModel, char newLine,
                                         boolean isAppend) {
    String ret = loadInlineScript(strModel, newLine, isAppend, null);
    refresh(REFRESH_REPAINT, "loadInlineScript");
    return ret;
  }

  /**
   * external apps only
   * 
   */

  @Override
  public String loadInline(String[] arrayModels) {
    // JmolViewer interface
    return loadInline(arrayModels, false);
  }

  /**
   * external apps and applet only
   * 
   */
  @Override
  public String loadInline(String[] arrayModels, boolean isAppend) {
    // JmolViewer interface
    // Eval data
    // loadInline
    if (arrayModels == null || arrayModels.length == 0)
      return null;
    String ret = openStringsInlineParamsAppend(arrayModels,
        new Hashtable<String, Object>(), isAppend);
    refresh(REFRESH_REPAINT, "loadInline String[]");
    return ret;
  }

  /**
   * External applications only; does not preserve state -- intentionally!
   * 
   * @param arrayData
   * @param isAppend
   * @return null or error string
   * 
   */
  @Override
  public String loadInline(java.util.List<Object> arrayData, boolean isAppend) {
    // NO STATE SCRIPT -- HERE WE ARE TRYING TO CONSERVE SPACE

    // loadInline
    if (arrayData == null || arrayData.size() == 0)
      return null;
    if (!isAppend)
      zap(true, false/*true*/, false);
    Lst<Object> list = new Lst<Object>();
    for (int i = 0; i < arrayData.size(); i++)
      list.addLast(arrayData.get(i));
    Object atomSetCollection = fm.createAtomSeCollectionFromArrayData(list,
        setLoadParameters(null, isAppend), isAppend);
    String ret = createModelSetAndReturnError(atomSetCollection, isAppend, null,
        new Hashtable<String, Object>());
    refresh(REFRESH_REPAINT, "loadInline");
    return ret;
  }

  /**
   * used by loadInline and openFileFull
   * 
   * @param strModel
   * @param newLine
   * @param isAppend
   * @param htParams
   * @return null or error message
   */
  private String loadInlineScript(String strModel, char newLine,
                                  boolean isAppend,
                                  Map<String, Object> htParams) {
    if (strModel == null || strModel.length() == 0)
      return null;
    strModel = fixInlineString(strModel, newLine);
    if (newLine != 0)
      Logger.info("loading model inline, " + strModel.length()
          + " bytes, with newLine character " + (int) newLine + " isAppend="
          + isAppend);
    if (Logger.debugging)
      Logger.debug(strModel);
    String datasep = getDataSeparator();
    int i;
    if (datasep != null && datasep != "" && (i = strModel.indexOf(datasep)) >= 0
        && strModel.indexOf("# Jmol state") < 0) {
      int n = 2;
      while ((i = strModel.indexOf(datasep, i + 1)) >= 0)
        n++;
      String[] strModels = new String[n];
      int pt = 0, pt0 = 0;
      for (i = 0; i < n; i++) {
        pt = strModel.indexOf(datasep, pt0);
        if (pt < 0)
          pt = strModel.length();
        strModels[i] = strModel.substring(pt0, pt);
        pt0 = pt + datasep.length();
      }
      return openStringsInlineParamsAppend(strModels, htParams, isAppend);
    }
    return openStringInlineParamsAppend(strModel, htParams, isAppend);
  }

  public static String fixInlineString(String strModel, char newLine) {
    // only if first character is "|" do we consider "|" to be new line
    int i;
    if (strModel.indexOf("\\/n") >= 0) {
      // the problem is that when this string is passed to Jmol
      // by the web page <embed> mechanism, browsers differ
      // in how they handle CR and LF. Some will pass it,
      // some will not.
      strModel = PT.rep(strModel, "\n", "");
      strModel = PT.rep(strModel, "\\/n", "\n");
      newLine = 0;
    }
    if (newLine != 0 && newLine != '\n') {
      boolean repEmpty = (strModel.indexOf('\n') >= 0);
      int len = strModel.length();
      for (i = 0; i < len && strModel.charAt(i) == ' '; ++i) {
      }
      if (i < len && strModel.charAt(i) == newLine)
        strModel = strModel.substring(i + 1);
      if (repEmpty)
        strModel = PT.rep(strModel, "" + newLine, "");
      else
        strModel = strModel.replace(newLine, '\n');
    }
    return strModel;
  }

  /**
   * Only used for adding hydrogen atoms and adding the model kit methane model;
   * not part of the public interface.
   * 
   * @param strModel
   * @param htParams
   * @param isAppend
   * @return null or error string
   * 
   */
  public String openStringInlineParamsAppend(String strModel,
                                             Map<String, Object> htParams,
                                             boolean isAppend) {
    // loadInline, openStringInline

    String type = getModelAdapter().getFileTypeName(Rdr.getBR(strModel));
    if (type == null)
      return "unknown file type";
    if (type.equals("spt")) {
      return "cannot open script inline";
    }

    htParams = setLoadParameters(htParams, isAppend);
    SB loadScript = (SB) htParams.get("loadScript");
    boolean isLoadCommand = htParams.containsKey("isData");
    if (loadScript == null)
      loadScript = new SB();
    if (!isAppend)
      zap(true, false/*true*/, false);
    if (!isLoadCommand)
      getStateCreator().getInlineData(loadScript, strModel, isAppend,
          (Integer) htParams.get("appendToModelIndex"), g.defaultLoadFilter);
    Object atomSetCollection = fm.createAtomSetCollectionFromString(strModel,
        htParams, isAppend);
    return createModelSetAndReturnError(atomSetCollection, isAppend, loadScript,
        htParams);
  }

  /**
   * opens multiple files inline; does NOT repaint
   * 
   * @param arrayModels
   * @param htParams
   * @param isAppend
   * @return null or error message
   */
  private String openStringsInlineParamsAppend(String[] arrayModels,
                                               Map<String, Object> htParams,
                                               boolean isAppend) {
    // loadInline
    SB loadScript = new SB();
    if (!isAppend)
      zap(true, false/*true*/, false);
    Object atomSetCollection = fm.createAtomSeCollectionFromStrings(arrayModels,
        loadScript, setLoadParameters(htParams, isAppend), isAppend);
    return createModelSetAndReturnError(atomSetCollection, isAppend, loadScript,
        htParams);
  }

  public char getInlineChar() {
    // used by the ScriptEvaluator DATA command
    return g.inlineNewlineChar;
  }

  String getDataSeparator() {
    // used to separate data files within a single DATA command
    return (String) g.getParameter("dataseparator", true);
  }

  ////////// create the model set ////////////

  /**
   * finally(!) we are ready to create the "model set" from the "atom set
   * collection" - does NOT repaint
   * 
   * @param atomSetCollection
   * @param isAppend
   * @param loadScript
   *        if null, then some special method like DOM; turn of preserveState
   * @param htParams
   * @return errMsg
   */
  private String createModelSetAndReturnError(Object atomSetCollection,
                                              boolean isAppend, SB loadScript,
                                              Map<String, Object> htParams) {

    Logger.startTimer("creating model");

    String fullPathName = fm.getFullPathName(false);
    String fileName = fm.getFileName();
    String errMsg;
    if (loadScript == null) {
      setBooleanProperty("preserveState", false);
      loadScript = new SB().append("load \"???\"");
    }
    if (atomSetCollection instanceof String) {
      errMsg = (String) atomSetCollection;
      setFileLoadStatus(FIL.NOT_LOADED, fullPathName, null, null, errMsg, null);
      if (displayLoadErrors && !isAppend && !errMsg.equals("#CANCELED#")
          && !errMsg.startsWith(JC.READER_NOT_FOUND))
        zapMsg(errMsg);
      return errMsg;
    }
    if (isAppend)
      clearAtomSets();
    else if (g.modelKitMode && !fileName.equals("Jmol Model Kit"))
      setModelKitMode(false);
    setFileLoadStatus(FIL.CREATING_MODELSET, fullPathName, fileName, null, null,
        null);

    // null fullPathName implies we are doing a merge
    pushHoldRepaintWhy("createModelSet");
    setErrorMessage(null, null);
    try {
      BS bsNew = new BS();
      mm.createModelSet(fullPathName, fileName, loadScript, atomSetCollection,
          bsNew, isAppend);
      if (bsNew.cardinality() > 0) {
        // is a 2D dataset, as from JME
        String jmolScript = (String) ms.getInfoM("jmolscript");
        if (ms.getMSInfoB("doMinimize")) {
          try {
            JmolScriptEvaluator eval = (JmolScriptEvaluator) htParams
                .get("eval");
            BS stereo = getAtomBitSet("_C & connected(3) & !connected(double)");
            stereo.and(bsNew);
            if (stereo.nextSetBit(0) >= 0) {
              bsNew.or(addHydrogens(stereo,MIN_NO_RANGE | MIN_SILENT | MIN_QUICK));
            }
            minimize(eval, Integer.MAX_VALUE, 0, bsNew, null, 0,
                MIN_ADDH | MIN_NO_RANGE | MIN_SILENT | MIN_QUICK);
          } catch (Exception e) {
          }
        } else {
          addHydrogens(bsNew, MIN_SILENT | MIN_QUICK);
        }
        // no longer necessary? -- this is the JME/SMILES data:
        if (jmolScript != null)
          ms.msInfo.put("jmolscript", jmolScript);
      }
      initializeModel(isAppend);
      // if (global.modelkitMode &&
      // (modelSet.modelCount > 1 || modelSet.models[0].isPDB()))
      // setBooleanProperty("modelkitmode", false);

    } catch (Error er) {
      handleError(er, true);
      errMsg = getShapeErrorState();
      errMsg = ("ERROR creating model: " + er
          + (errMsg.length() == 0 ? "" : "|" + errMsg));
      zapMsg(errMsg);
      setErrorMessage(errMsg, null);
    }
    popHoldRepaint("createModelSet " + JC.REPAINT_IGNORE);
    errMsg = getErrorMessage();

    setFileLoadStatus(FIL.CREATED, fullPathName, fileName, ms.modelSetName,
        errMsg, (Boolean) htParams.get("async"));
    if (isAppend) {
      selectAll();
      setTainted(true);
      axesAreTainted = true;
    }
    atomSetCollection = null;
    Logger.checkTimer("creating model", false);
    System.gc();
    return errMsg;
  }

  /**
   * 
   * or just apply the data to the current model set
   * 
   * @param atomSetCollection
   * @param tokType
   * @return error or null
   */
  private String loadAtomDataAndReturnError(Object atomSetCollection,
                                            int tokType) {
    if (atomSetCollection instanceof String)
      return (String) atomSetCollection;
    setErrorMessage(null, null);
    try {
      String script = mm.createAtomDataSet(atomSetCollection, tokType);
      switch (tokType) {
      case T.xyz:
        if (script != null)
          runScriptCautiously(script);
        break;
      case T.vibration:
        setStatusFrameChanged(true, false);
        break;
      case T.vanderwaals:
        shm.deleteVdwDependentShapes(null);
        break;
      }
    } catch (Error er) {
      handleError(er, true);
      String errMsg = getShapeErrorState();
      errMsg = ("ERROR adding atom data: " + er
          + (errMsg.length() == 0 ? "" : "|" + errMsg));
      zapMsg(errMsg);
      setErrorMessage(errMsg, null);
      setParallel(false);
    }
    return getErrorMessage();
  }

  ////////// File-related methods ////////////

  public String getCurrentFileAsString(String state) {
    String filename = fm.getFullPathName(false);
    if (filename.equals("string") || filename.equals(JC.MODELKIT_ZAP_TITLE))
      return ms.getInlineData(am.cmi);
    if (filename.equals("String[]"))
      return filename;
    if (filename == "JSNode")
      return "<DOM NODE>";
    //    filename = mm.getModelSetPathName();
    //    if (filename == null)
    //      return null;
    return getFileAsString4(filename, -1, true, false, false, state);
  }

  /**
   * 
   * @param filename
   * @return String[2] where [0] is fullpathname and [1] is error message or
   *         null
   */
  public String[] getFullPathNameOrError(String filename) {
    String[] data = new String[2];
    fm.getFullPathNameOrError(filename, false, data);
    return data;
  }

  public String getFileAsString3(String name, boolean checkProtected,
                                 String state) {
    return getFileAsString4(name, -1, false, false, checkProtected, state);
  }

  public String getFileAsString4(String name, int nBytesMax,
                                 boolean doSpecialLoad, boolean allowBinary,
                                 boolean checkProtected, String state) {
    if (name == null)
      return getCurrentFileAsString(state);
    String[] data = new String[] { name, null };
    // ignore error completely
    fm.getFileDataAsString(data, nBytesMax, doSpecialLoad, allowBinary,
        checkProtected);
    return data[1];
  }

  public String getAsciiFileOrNull(String name) {
    String[] data = new String[] { name, null };
    return (fm.getFileDataAsString(data, -1, false, false, false) ? data[1]
        : null);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to ModelManager
  // ///////////////////////////////////////////////////////////////

  public void autoCalculate(int tokProperty, String dataType) {
    switch (tokProperty) {
    case T.surfacedistance:
      ms.getSurfaceDistanceMax();
      break;
    case T.straightness:
      ms.calculateStraightnessAll();
      break;
    case T.dssr:
      ms.calculateDssrProperty(dataType);
    }
  }

  // This was just the sum of the atomic volumes, not considering overlap
  // It was never documented.
  // Removed in Jmol 13.0.RC4

  //  public float getVolume(BitSet bs, String type) {
  //    // Eval.calculate(), math function volume({atomExpression},"type")
  //    if (bs == null)
  //      bs = getSelectionSet();
  //    EnumVdw vType = EnumVdw.getVdwType(type);
  //    if (vType == null)
  //      vType = EnumVdw.AUTO;
  //    return modelSet.calculateVolume(bs, vType);
  //  }

  public void calculateStraightness() {
    ms.haveStraightness = false;
    ms.calculateStraightnessAll();
  }

  public P3[] calculateSurface(BS bsSelected, float envelopeRadius) {
    if (bsSelected == null)
      bsSelected = bsA();
    if (envelopeRadius == Float.MAX_VALUE || envelopeRadius == -1)
      ms.addStateScript(
          "calculate surfaceDistance "
              + (envelopeRadius == Float.MAX_VALUE ? "FROM" : "WITHIN"),
          null, bsSelected, null, "", false, true);
    return ms.calculateSurface(bsSelected, envelopeRadius);
  }

  public Map<STR, float[]> getStructureList() {
    return g.getStructureList();
  }

  public void setStructureList(float[] list, STR type) {
    // none, turn, sheet, helix
    g.setStructureList(list, type);
    ms.setStructureList(getStructureList());
  }

  /**
   * 
   * @param bsAtoms
   * @param asDSSP
   * @param setStructure
   *        to actually change structures
   * @param version
   * @return structure string from DSSP
   */
  public String calculateStructures(BS bsAtoms, boolean asDSSP,
                                    boolean setStructure, int version) {
    // Eval
    if (bsAtoms == null)
      bsAtoms = bsA();
    return ms.calculateStructures(bsAtoms, asDSSP, !am.animationOn,
        g.dsspCalcHydrogen, setStructure, version);
  }

  private JmolAnnotationParser annotationParser, dssrParser;

  public JmolAnnotationParser getAnnotationParser(boolean isDSSR) {
    return (isDSSR
        ? (dssrParser == null
            ? (dssrParser = (JmolAnnotationParser) Interface
                .getOption("dssx.DSSR1", this, "script"))
            : dssrParser)
        : (annotationParser == null
            ? (annotationParser = (JmolAnnotationParser) Interface
                .getOption("dssx.AnnotationParser", this, "script"))
            : annotationParser));
  }

  @Override
  public AtomIndexIterator getSelectedAtomIterator(BS bsSelected,
                                                   boolean isGreaterOnly,
                                                   boolean modelZeroBased,
                                                   boolean isMultiModel) {
    return ms.getSelectedAtomIterator(bsSelected, isGreaterOnly, modelZeroBased,
        false, isMultiModel);
  }

  @Override
  public void setIteratorForAtom(AtomIndexIterator iterator, int atomIndex,
                                 float distance) {
    ms.setIteratorForAtom(iterator, -1, atomIndex, distance, null);
  }

  @Override
  public void setIteratorForPoint(AtomIndexIterator iterator, int modelIndex,
                                  T3 pt, float distance) {
    ms.setIteratorForPoint(iterator, modelIndex, pt, distance);
  }

  @Override
  public void fillAtomData(AtomData atomData, int mode) {
    atomData.programInfo = "Jmol Version " + getJmolVersion();
    atomData.fileName = fm.getFileName();
    ms.fillAtomData(atomData, mode);
  }

  public StateScript addStateScript(String script, boolean addFrameNumber,
                                    boolean postDefinitions) {
    // calculate
    // configuration
    // plot
    // rebond
    // setPdbConectBonding
    return ms.addStateScript(script, null, null, null, null, addFrameNumber,
        postDefinitions);
  }

  private Minimizer minimizer;
  private SmilesMatcherInterface smilesMatcher;

  public Minimizer getMinimizer(boolean createNew) {
    return (minimizer == null && createNew
        ? (minimizer = (Minimizer) Interface
            .getInterface("org.jmol.minimize.Minimizer", this, "script"))
                .setProperty("vwr", this)
        : minimizer);
  }

  public SmilesMatcherInterface getSmilesMatcher() {
    return (smilesMatcher == null
        ? (smilesMatcher = (SmilesMatcherInterface) Interface
            .getInterface("org.jmol.smiles.SmilesMatcher", this, "script"))
        : smilesMatcher);
  }

  public void clearModelDependentObjects() {
    setFrameOffsets(null, false);
    stopMinimization();
    minimizer = null;
    smilesMatcher = null;
  }

  public void zap(boolean notify, boolean resetUndo, boolean zapModelKit) {
    clearThreads();
    if (mm.modelSet == null) {
      mm.zap();
    } else {
      //setBooleanProperty("appendNew", true);
      ligandModelSet = null;
      clearModelDependentObjects();
      fm.clear();
      clearRepaintManager(-1);
      am.clear();
      tm.clear();
      slm.clear();
      hasSelected = true;
      clearAllMeasurements();
      clearMinimization();
      gdata.clear();
      mm.zap();
      if (scm != null)
        scm.clear(false);
      if (nmrCalculation != null)
        getNMRCalculation().setChemicalShiftReference(null, 0);

      if (haveDisplay) {
        mouse.clear();
        clearTimeouts();
        acm.clear();
      }
      stm.clear(g);
      tempArray.clear();
      chainMap.clear();
      chainList.clear();
      chainCaseSpecified = false;
      //cm.clear();
      definedAtomSets.clear();
      lastData = null;
      if (dm != null)
        dm.clear();
      setBooleanProperty("legacyjavafloat", false);
      if (resetUndo) {
        if (zapModelKit)
          g.removeParam("_pngjFile");
        if (zapModelKit && g.modelKitMode) {
          loadDefaultModelKitModel(null);
        }
        undoClear();
      }
      System.gc();
    }
    initializeModel(false);
    if (notify) {
      setFileLoadStatus(FIL.ZAPPED, null,
          (resetUndo ? "resetUndo" : getZapName()), null, null, null);
    }
    if (Logger.debugging)
      Logger.checkMemory();
  }

  private void loadDefaultModelKitModel(Map<String, Object> htParams) {
    openStringInlineParamsAppend(getModelkit(false).getDefaultModel(), htParams, true);
    setRotationRadius(5.0f, true);
    setStringProperty("picking", "assignAtom_C");
    setStringProperty("picking", "assignBond_p");
  }

  private void zapMsg(String msg) {
    zap(true, true, false);
    echoMessage(msg);
  }

  void echoMessage(String msg) {
    int iShape = JC.SHAPE_ECHO;
    shm.loadShape(iShape);
    setShapeProperty(iShape, "font", getFont3D("SansSerif", "Plain", 20));
    setShapeProperty(iShape, "target", "error");
    setShapeProperty(iShape, "text", msg);
  }

  private void initializeModel(boolean isAppend) {
    clearThreads();
    if (isAppend) {
      am.initializePointers(1);
      return;
    }
    reset(true);
    selectAll();
    if (modelkit != null)
      modelkit.initializeForModel();
    movingSelected = false;
    slm.noneSelected = Boolean.FALSE;
    setHoverEnabled(true);
    setSelectionHalosEnabled(false);
    tm.setCenter();
    am.initializePointers(1);
    setBooleanProperty("multipleBondBananas", false);
    if (!ms.getMSInfoB("isPyMOL")) {
      clearAtomSets();
      setCurrentModelIndex(0);
    }
    setBackgroundModelIndex(-1);
    setFrankOn(getShowFrank());
    startHoverWatcher(true);
    setTainted(true);
    finalizeTransformParameters();
  }

  public void startHoverWatcher(boolean tf) {
    if (tf && inMotion || !haveDisplay
        || tf && (!hoverEnabled && !sm.haveHoverCallback() || am.animationOn))
      return;
    acm.startHoverWatcher(tf);
  }

  @Override
  public String getModelSetPathName() {
    return mm.modelSetPathName;
  }

  @Override
  public String getModelSetFileName() {
    return (mm.fileName == null ? getZapName() : mm.fileName);
  }

  public String getUnitCellInfoText() {
    SymmetryInterface c = getCurrentUnitCell();
    return (c == null ? "not applicable" : c.getUnitCellInfo());
  }

  public float getUnitCellInfo(int infoType) {
    SymmetryInterface symmetry = getCurrentUnitCell();
    return (symmetry == null ? Float.NaN
        : symmetry.getUnitCellInfoType(infoType));
  }

  /**
   * 
   * convert string abc;offset or M3 or M4 to origin and three vectors -- a, b,
   * c. The string can be preceded by ! for "reverse of". For example,
   * "!a-b,-5a-5b,-c;7/8,0,1/8" offset is optional, but it still needs a
   * semicolon: "a/2,b/2,c;"
   * @param iModel 
   * 
   * @param def
   *        a string or an M3 or M4
   * @return vectors [origin a b c]
   */
  public T3[] getV0abc(int iModel, Object def) {
    SymmetryInterface uc = (iModel < 0 ? getCurrentUnitCell() : getUnitCell(iModel));
    return (uc == null ? null : uc.getV0abc(def));
  }

  public SymmetryInterface getCurrentUnitCell() {
      int iAtom = am.getUnitCellAtomIndex(); 
      return (iAtom >= 0 ? ms.getUnitCellForAtom(iAtom) : getUnitCell(am.cmi));
    }

    private SymmetryInterface getUnitCell(int m) {
      if (m >= 0)
        return ms.getUnitCell(m);
      BS models = getVisibleFramesBitSet();
      SymmetryInterface ucLast = null;
      for (int i = models.nextSetBit(0); i >= 0; i = models.nextSetBit(i + 1)) {
        SymmetryInterface uc = ms.getUnitCell(i);
        if (uc == null)
          continue;
        if (ucLast == null) {
          ucLast = uc;
          continue;
        }
        if (!ucLast.unitCellEquals(uc))
          return null;
      }
      return ucLast;
    }


  public void getPolymerPointsAndVectors(BS bs, Lst<P3[]> vList) {
    ms.getPolymerPointsAndVectors(bs, vList, g.traceAlpha, g.sheetSmoothing);
  }

  public String getHybridizationAndAxes(int atomIndex, V3 z, V3 x,
                                        String lcaoType) {
    return ms.getHybridizationAndAxes(atomIndex, 0, z, x, lcaoType, true, true,
        false);
  }

  public BS getAllAtoms() {
    return getModelUndeletedAtomsBitSet(-1);
  }

  public BS getFrameAtoms() {
    return getModelUndeletedAtomsBitSetBs(getVisibleFramesBitSet());
  }

  @Override
  public BS getVisibleFramesBitSet() {
    BS bs = BSUtil.copy(am.bsVisibleModels);
    if (ms.trajectory != null)
      ms.trajectory.selectDisplayed(bs);
    return bs;

  }

  public BS getModelUndeletedAtomsBitSet(int modelIndex) {
    return slm.excludeAtoms(
        ms.getModelAtomBitSetIncludingDeleted(modelIndex, true), false);
  }

  public BS getModelUndeletedAtomsBitSetBs(BS bsModels) {
    return slm.excludeAtoms(ms.getModelAtomBitSetIncludingDeletedBs(bsModels),
        false);
  }

  @Override
  public P3 getBoundBoxCenter() {
    return ms.getBoundBoxCenter(am.cmi);
  }

  public void calcBoundBoxDimensions(BS bs, float scale) {
    ms.calcBoundBoxDimensions(bs, scale);
    axesAreTainted = true;
  }

  @Override
  public V3 getBoundBoxCornerVector() {
    return ms.getBoundBoxCornerVector();
  }

  @Override
  public Properties getModelSetProperties() {
    return ms.modelSetProperties;
  }

  @Override
  public Properties getModelProperties(int modelIndex) {
    return ms.am[modelIndex].properties;
  }

  public Model getModelForAtomIndex(int iatom) {
    return ms.am[ms.at[iatom].mi];
  }

  @Override
  public Map<String, Object> getModelSetAuxiliaryInfo() {
    return ms.getAuxiliaryInfo(null);
  }

  @Override
  public int getModelNumber(int modelIndex) {
    return (modelIndex < 0 ? modelIndex : ms.getModelNumber(modelIndex));
  }

  public int getModelFileNumber(int modelIndex) {
    return (modelIndex < 0 ? 0 : ms.modelFileNumbers[modelIndex]);
  }

  @Override
  public String getModelNumberDotted(int modelIndex) {
    // must not return "all" for -1, because this could be within a frame RANGE
    return modelIndex < 0 ? "0" : ms.getModelNumberDotted(modelIndex);
  }

  @Override
  public String getModelName(int modelIndex) {
    return ms.getModelName(modelIndex);
  }

  public boolean modelHasVibrationVectors(int modelIndex) {
    return (ms.getLastVibrationVector(modelIndex, T.vibration) >= 0);
  }

  public BS getBondsForSelectedAtoms(BS bsAtoms) {
    // eval
    return ms.getBondsForSelectedAtoms(bsAtoms,
        g.bondModeOr || BSUtil.cardinalityOf(bsAtoms) == 1);
  }

  public boolean frankClicked(int x, int y) {
    // bottom right Jmol logo
    return !g.disablePopupMenu && getShowFrank() && shm.checkFrankclicked(x, y);
  }

  public boolean frankClickedModelKit(int x, int y) {
    // top left indicator
    return !g.disablePopupMenu && g.modelKitMode && x >= 0 && y >= 0 && x < 40
        && y < 26*4; // See FrankRenderer
  }

  @Override
  public int findNearestAtomIndex(int x, int y) {
    return findNearestAtomIndexMovable(x, y, false);
  }

  public int findNearestAtomIndexMovable(int x, int y, boolean mustBeMovable) {
    return (!g.atomPicking ? -1
        : ms.findNearestAtomIndex(x, y,
            mustBeMovable ? slm.getMotionFixedAtoms() : null,
            g.minPixelSelRadius));
  }

  /**
   * absolute or relative to origin of UNITCELL {x y z}
   * @param pt
   * @param ignoreOffset
   *        TODO
   */
  public void toCartesian(T3 pt, boolean ignoreOffset) {
    toCartesianUC(null, pt, ignoreOffset);
  }

  public void toCartesianUC(SymmetryInterface unitCell, T3 pt, boolean ignoreOffset) {
    if (unitCell == null)
      unitCell = getCurrentUnitCell();
    if (unitCell != null) {
      unitCell.toCartesian(pt, ignoreOffset);
      if (!g.legacyJavaFloat)
        PT.fixPtFloats(pt, PT.CARTESIAN_PRECISION);
    }
  }

  /**
   * 
   * @param pt
   * @param ignoreOffset
   *        set true for relative to {0 0 0}; otherwise relative to origin of
   *        UNITCELL {x y z}
   */
  public void toFractional(T3 pt, boolean ignoreOffset) {
    toFractionalUC(null, pt, ignoreOffset);
  }

  public void toFractionalUC(SymmetryInterface unitCell, T3 pt, boolean ignoreOffset) {
    if (unitCell == null)
      unitCell = getCurrentUnitCell();
    if (unitCell != null) {
      unitCell.toFractional(pt, ignoreOffset);
      if (!g.legacyJavaFloat)
        PT.fixPtFloats(pt, PT.FRACTIONAL_PRECISION);
    }
  }

  /**
   * relative to origin without regard to UNITCELL {x y z}
   * 
   * @param pt
   * @param offset
   */
  public void toUnitCell(P3 pt, P3 offset) {
    SymmetryInterface unitCell = getCurrentUnitCell();
    if (unitCell != null)
      unitCell.toUnitCell(pt, offset);
  }

  public void setCurrentCage(String isosurfaceId) {
    Object[] data = new Object[] { isosurfaceId, null };
    shm.getShapePropertyData(JC.SHAPE_ISOSURFACE, "unitCell", data);
    ms.setModelCage(am.cmi, (SymmetryInterface) data[1]);
  }

  public void addUnitCellOffset(P3 pt) {
    SymmetryInterface unitCell = getCurrentUnitCell();
    if (unitCell == null)
      return;
    pt.add(unitCell.getCartesianOffset());
  }

  public void setAtomData(int type, String name, String coordinateData,
                          boolean isDefault) {
    // DATA "xxxx"
    // atom coordinates may be moved here 
    //  but this is not included as an atomMovedCallback
    ms.setAtomData(type, name, coordinateData, isDefault);
    if (type == AtomCollection.TAINT_COORD)
      checkCoordinatesChanged();
    refreshMeasures(true);
  }

  @Override
  public void setCenterSelected() {
    // depricated
    setCenterBitSet(bsA(), true);
  }

  void setApplySymmetryToBonds(boolean TF) {
    g.applySymmetryToBonds = TF;
  }

  @Override
  public void setBondTolerance(float bondTolerance) {
    g.setF("bondTolerance", bondTolerance);
    g.bondTolerance = bondTolerance;
  }

  @Override
  public void setMinBondDistance(float minBondDistance) {
    // PreferencesDialog
    g.setF("minBondDistance", minBondDistance);
    g.minBondDistance = minBondDistance;
  }

  public BS getAtomsNearPt(float distance, P3 coord) {
    BS bs = new BS();
    ms.getAtomsWithin(distance, coord, bs, -1);
    return bs;
  }

  /**
   * given a set of atoms, a subset of atoms to test, two atoms that start the
   * branch, and whether or not to allow the branch to cycle back on
   * itself,deliver the set of atoms constituting this branch.
   * 
   * @param atomIndex
   * @param atomIndexNot
   * @param allowCyclic
   * @return bitset for this branch
   */
  public BS getBranchBitSet(int atomIndex, int atomIndexNot,
                            boolean allowCyclic) {
    if (atomIndex < 0 || atomIndex >= ms.ac)
      return new BS();
    return JmolMolecule.getBranchBitSet(ms.at, atomIndex,
        getModelUndeletedAtomsBitSet(ms.at[atomIndex].mi), null, atomIndexNot,
        allowCyclic, true);
  }

  @Override
  public BS getElementsPresentBitSet(int modelIndex) {
    return ms.getElementsPresentBitSet(modelIndex);
  }

  String getFileHeader() {
    return ms.getFileHeader(am.cmi);
  }

  Object getFileData() {
    return ms.getFileData(am.cmi);
  }

  public Map<String, Object> getCifData(int modelIndex) {
    return readCifData(ms.getModelFileName(modelIndex),
        ms.getModelFileType(modelIndex).toUpperCase());
  }

  public Map<String, Object> readCifData(String fileName, String type) {
    String fname = (fileName == null ? ms.getModelFileName(am.cmi) : fileName);
    if (type == null && fname != null
        && fname.toUpperCase().indexOf("BCIF") >= 0) {
      BufferedInputStream is = fm.getBufferedInputStream(fname);
      try {
        return ((javajs.util.MessagePackReader) Interface
            .getInterface("javajs.util.MessagePackReader", this, "script"))
                .getMapForStream(is);
      } catch (Exception e) {
        e.printStackTrace();
        return new Hashtable<String, Object>();
      }
    }
    String data = (fileName == null || fileName.length() == 0
        ? getCurrentFileAsString("script")
        : getFileAsString3(fileName, false, null));
    if (data == null || data.length() < 2)
      return null;
    BufferedReader rdr = Rdr.getBR(data);
    if (type == null)
      type = getModelAdapter().getFileTypeName(rdr);
    return (type == null ? null : readCifData(null, rdr, type));

  }

  @Override
  public Map<String, Object> readCifData(String fileName,
                                         Object rdrOrStringData, String type) {
    if (rdrOrStringData == null)
      rdrOrStringData = getFileAsString(fileName);
    BufferedReader rdr = (rdrOrStringData instanceof BufferedReader
        ? (BufferedReader) rdrOrStringData
        : Rdr.getBR((String) rdrOrStringData));
    return Rdr.readCifData((GenericCifDataParser) Interface.getInterface(
        ("Cif2".equals(type) ? "org.jmol.adapter.readers.cif.Cif2DataParser"
            : "javajs.util.CifDataParser"),
        this, "script"), rdr);
  }

  JmolStateCreator jsc;

  public JmolStateCreator getStateCreator() {
    if (jsc == null)
      (jsc = (JmolStateCreator) Interface
          .getInterface("org.jmol.viewer.StateCreator", this, "script"))
              .setViewer(this);
    return jsc;
  }

  public String getWrappedStateScript() {
    return (String) getOutputManager().getWrappedState(null, null, null, null);
  }

  @Override
  public String getStateInfo() {
    return getStateInfo3(null, 0, 0);
  }

  public String getStateInfo3(String type, int width, int height) {
    return (g.preserveState
        ? getStateCreator().getStateScript(type, width, height)
        : "");
  }

  public String getStructureState() {
    return getStateCreator().getModelState(null, false, true);
  }

  public String getCoordinateState(BS bsSelected) {
    return getStateCreator().getAtomicPropertyState(AtomCollection.TAINT_COORD,
        bsSelected);
  }

  public void setCurrentColorRange(String label) {
    float[] data = (float[]) getDataObj(label, null,
        JmolDataManager.DATA_TYPE_AF);
    BS bs = (data == null ? null
        : (BS) ((Object[]) getDataObj(label, null,
            JmolDataManager.DATA_TYPE_UNKNOWN))[2]);
    if (bs != null && g.rangeSelected)
      bs.and(bsA());
    cm.setPropertyColorRangeData(data, bs);
  }

  private Object[] lastData;

  /**
   * A general-purpose data storage method. Note that matchFieldCount and
   * dataFieldCount should both be positive or both be negative.
   * 
   * @param key
   * 
   *        a simple key name for the data, starting with "property_" if
   *        user-defined
   * 
   * @param data
   * 
   *        data[0] -- label
   * 
   *        data[1] -- string or float[] or float[][] or float[][][]
   * 
   *        data[2] -- selection bitset or int[] atomMap when field > 0
   * 
   *        data[3] -- arrayDepth
   *        0(String),1(float[]),2(float[][]),3(float[][][]) or -1 to indidate
   *        that it is set by data type
   * 
   *        data[4] -- Boolean.TRUE == saveInState
   * 
   * @param dataType
   * 
   *        see JmolDataManager interface
   * 
   * @param matchField
   * 
   *        if positive, data must match atomNo in this column
   * 
   *        if 0, no match column
   * 
   * @param matchFieldColumnCount
   *        if positive, this number of characters in match column if 0,
   *        reference is to tokens, not characters
   * 
   * @param dataField
   * 
   *        if positive, column containing the data
   * 
   *        if 0, values are a simple list; clear the data
   * 
   *        if Integer.MAX_VALUE, values are a simple list; don't clear the data
   * 
   *        if Integer.MIN_VALUE, have one SINGLE data value for all selected
   *        atoms
   * 
   * @param dataFieldColumnCount
   * 
   *        if positive, this number of characters in data column
   * 
   *        if 0, reference is to tokens, not characters
   */
  public void setData(String key, Object[] data, int dataType, int matchField,
                      int matchFieldColumnCount, int dataField,
                      int dataFieldColumnCount) {
    getDataManager().setData(key, lastData = data, dataType, ms.ac, matchField,
        matchFieldColumnCount, dataField, dataFieldColumnCount);
  }

  /**
   * Retrieve a data object
   * 
   * @param key
   * 
   * @param bsSelected
   * 
   *        selected atoms; for DATA_AF only
   * 
   * @param dataType
   * 
   *        see JmolDataManager interface
   * 
   * @return data object
   * 
   *         data[0] -- label (same as key)
   * 
   *         data[1] -- string or float[] or float[][] or float[][][]
   * 
   *         data[2] -- selection bitset or int[] atomMap when field > 0
   * 
   *         data[3] -- arrayDepth
   *         0(String),1(float[]),2(float[][]),3(float[][][]) or -1 to indicate
   *         that it is set by data type
   * 
   *         data[4] -- Boolean.TRUE == saveInState
   */
  public Object getDataObj(String key, BS bsSelected, int dataType) {
    return (key == null && dataType == JmolDataManager.DATA_TYPE_LAST ? lastData
        : getDataManager().getData(key, bsSelected, dataType));
  }

  //  public float getDataFloatAt(String label, int atomIndex) {
  //    return getDataManager().getDataFloatAt(label, atomIndex);
  //  }

  // boolean autoLoadOrientation() {
  // return true;//global.autoLoadOrientation; 12.0.RC10
  // }

  public int autoHbond(BS bsFrom, BS bsTo, boolean onlyIfHaveCalculated) {
    if (bsFrom == null)
      bsFrom = bsTo = bsA();
    // bsTo null --> use DSSP method further developed 
    // here to give the "defining" Hbond set only
    return ms.autoHbond(bsFrom, bsTo, onlyIfHaveCalculated);
  }

  /*
   * ****************************************************************************
   * delegated to MeasurementManager
   * **************************************************************************
   */

  public String getDefaultMeasurementLabel(int nPoints) {
    switch (nPoints) {
    case 2:
      return g.defaultDistanceLabel;
    case 3:
      return g.defaultAngleLabel;
    default:
      return g.defaultTorsionLabel;
    }
  }

  @Override
  public int getMeasurementCount() {
    int count = getShapePropertyAsInt(JC.SHAPE_MEASURES, "count");
    return count <= 0 ? 0 : count;
  }

  @Override
  public String getMeasurementStringValue(int i) {
    return "" + shm.getShapePropertyIndex(JC.SHAPE_MEASURES, "stringValue", i);
  }

  public String getMeasurementInfoAsString() {
    return (String) getShapeProperty(JC.SHAPE_MEASURES, "infostring");
  }

  @Override
  public int[] getMeasurementCountPlusIndices(int i) {
    return (int[]) shm.getShapePropertyIndex(JC.SHAPE_MEASURES,
        "countPlusIndices", i);
  }

  void setPendingMeasurement(MeasurementPending mp) {
    // from MouseManager
    shm.loadShape(JC.SHAPE_MEASURES);
    setShapeProperty(JC.SHAPE_MEASURES, "pending", mp);
  }

  public MeasurementPending getPendingMeasurement() {
    return (MeasurementPending) getShapeProperty(JC.SHAPE_MEASURES, "pending");
  }

  public void clearAllMeasurements() {
    // Eval only
    setShapeProperty(JC.SHAPE_MEASURES, "clear", null);
  }

  @Override
  public void clearMeasurements() {
    // depricated but in the API -- use "script" directly
    // see clearAllMeasurements()
    evalString("measures delete");
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to AnimationManager
  // ///////////////////////////////////////////////////////////////

  public void setAnimation(int tok) {
    switch (tok) {
    case T.playrev:
      am.reverseAnimation();
      //$FALL-THROUGH$
    case T.play:
    case T.resume:
      if (!am.animationOn)
        am.resumeAnimation();
      return;
    case T.pause:
      if (am.animationOn && !am.animationPaused)
        am.pauseAnimation();
      return;
    case T.next:
      am.setAnimationNext();
      return;
    case T.prev:
      am.setAnimationPrevious();
      return;
    case T.first:
    case T.rewind:
      am.rewindAnimation();
      return;
    case T.last:
      am.setAnimationLast();
      return;
    }
  }

  @Override
  public void setAnimationFps(int fps) {
    am.setAnimationFps(fps);
  }

  private void setAnimationMode(String mode) {
    if (mode.equalsIgnoreCase("once")) {
      am.setAnimationReplayMode(T.once, 0, 0);
    } else if (mode.equalsIgnoreCase("loop")) {
      am.setAnimationReplayMode(T.loop, 1, 1);
    } else if (mode.startsWith("pal")) {
      am.setAnimationReplayMode(T.palindrome, 1, 1);
    }
  }

  public void setAnimationOn(boolean animationOn) {
    // Eval
    boolean wasAnimating = am.animationOn;
    if (animationOn == wasAnimating)
      return;
    am.setAnimationOn(animationOn);
  }

  public void setAnimationRange(int modelIndex1, int modelIndex2) {
    am.setAnimationRange(modelIndex1, modelIndex2);
  }

  public void defineAtomSets(Map<String, Object> info) {
    definedAtomSets.putAll(info);
  }

  public void setAnimDisplay(BS bs) {
    am.setDisplay(bs);
    if (!am.animationOn)
      am.morph(am.currentMorphModel + 1);
  }

  public void setCurrentModelIndex(int modelIndex) {
    // Eval
    // initializeModel
    if (modelIndex == Integer.MIN_VALUE) {
      // just forcing popup menu update
      prevFrame = Integer.MIN_VALUE;
      setCurrentModelIndexClear(am.cmi, true);
      return;
    }
    am.setModel(modelIndex, true);
  }

  public String getTrajectoryState() {
    return (ms.trajectory == null ? "" : ms.trajectory.getState());
  }

  public void setFrameOffsets(BS bsAtoms, boolean isFull) {
    tm.bsFrameOffsets = null;
    if (isFull)
      clearModelDependentObjects();
    else
      tm.bsFrameOffsets = bsAtoms;
    tm.frameOffsets = ms.getFrameOffsets(bsAtoms, isFull);
  }

  public void setCurrentModelIndexClear(int modelIndex,
                                        boolean clearBackground) {
    // Eval
    // initializeModel
    am.setModel(modelIndex, clearBackground);
  }

  public boolean haveFileSet() {
    return (ms.mc > 1 && getModelNumber(Integer.MAX_VALUE) > 2000000);
  }

  public void setBackgroundModelIndex(int modelIndex) {
    am.setBackgroundModelIndex(modelIndex);
    g.setO("backgroundModel", ms.getModelNumberDotted(modelIndex));
  }

  void setFrameVariables() {
    g.setO("animationMode", T.nameOf(am.animationReplayMode));
    g.setI("animationFps", am.animationFps);
    g.setO("_firstFrame", am.getModelSpecial(-1));
    g.setO("_lastFrame", am.getModelSpecial(1));
    g.setF("_animTimeSec", am.getAnimRunTimeSeconds());
    g.setB("_animMovie", am.isMovie);
  }

  private int motionEventNumber;
  private boolean inMotion;

  public boolean getInMotion(boolean includeAnim) {
    return (inMotion || includeAnim && am.animationOn);
  }

  @Override
  public int getMotionEventNumber() {
    return motionEventNumber;
  }

  @Override
  public void setInMotion(boolean inMotion) {
    if (this.inMotion ^ inMotion) {
      this.inMotion = inMotion;
      resizeImage(0, 0, false, false, true); // for antialiasdisplay
      if (inMotion) {
        startHoverWatcher(false);
        ++motionEventNumber;
      } else {
        startHoverWatcher(true);
        refresh(REFRESH_SYNC_MASK, "vwr setInMotion " + inMotion);
      }
    }
  }

  private boolean refreshing = true;

  private void setRefreshing(boolean TF) {
    refreshing = TF;
  }

  public boolean getRefreshing() {
    return refreshing;
  }

  @Override
  public void pushHoldRepaint() {
    pushHoldRepaintWhy(null);
  }

  /**
   * 
   * @param why
   */
  public void pushHoldRepaintWhy(String why) {
    if (rm != null)
      rm.pushHoldRepaint(why);
  }

  @Override
  public void popHoldRepaint(String why) {
    if (rm != null) {
      rm.popHoldRepaint(why.indexOf(JC.REPAINT_IGNORE) < 0, why);
    }
  }

  public final static int REFRESH_REPAINT = 1;
  public final static int REFRESH_SYNC = 2;
  public final static int REFRESH_SYNC_MASK = REFRESH_REPAINT | REFRESH_SYNC;
  public final static int REFRESH_REPAINT_NO_MOTION_ONLY = 6;
  public final static int REFRESH_SEND_WEBGL_NEW_ORIENTATION = 7;

  /**
   * initiate a repaint/update sequence if it has not already been requested.
   * invoked whenever any operation causes changes that require new rendering.
   * 
   * The repaint/update sequence will only be invoked if (a) no repaint is
   * already pending and (b) there is no hold flag set in repaintManager.
   * 
   * Sequence is as follows:
   * 
   * 1) RepaintManager.refresh() checks flags and then calls Viewer.repaint()
   * 
   * 2) Viewer.repaint() invokes display.repaint(), provided display is not null
   * (headless)
   * 
   * 3) The system responds with an invocation of Jmol.update(Graphics g), which
   * we are routing through Jmol.paint(Graphics g).
   * 
   * 4) Jmol.update invokes Viewer.setScreenDimensions(size), which makes the
   * necessary changes in parameters for any new window size.
   * 
   * 5) Jmol.update invokes Viewer.renderScreenImage(g, size, rectClip)
   * 
   * 6) Viewer.renderScreenImage checks object visibility, invokes render1 to do
   * the actual creation of the image pixel map and send it to the screen, and
   * then invokes repaintView()
   * 
   * 7) Viewer.repaintView() invokes RepaintManager.repaintDone(), to clear the
   * flags and then use notify() to release any threads holding on wait().
   * 
   * @param mode
   * 
   *        REFRESH_REPAINT: ONLY do a repaint -- no syncing
   * 
   *        REFRESH_SYNC: mouse motion requiring synchronization -- not going
   *        through Eval so we bypass Eval and mainline on the other vwr! Also
   *        called from j2sApplet.js
   * 
   *        REFRESH_REPAINT_SYNC_MASK: same as REFRESH_REPAINT, but not WebGL
   * 
   *        REFRESH_NO_MOTION_ONLY: refresh only if not in motion
   * 
   *        REFRESH_SEND_WEBGL_NEW_ORIENTATION: send WebGL a "new orientation"
   *        command at the end of a script using html5applet._refresh()
   * 
   * 
   * @param strWhy
   *        debugging or for passing mouse command when using REFRESH_SYNC
   * 
   */
  @Override
  public void refresh(int mode, String strWhy) {

    if (rm == null || !refreshing
        || mode == REFRESH_REPAINT_NO_MOTION_ONLY && getInMotion(true)
        || !isWebGL && mode == REFRESH_SEND_WEBGL_NEW_ORIENTATION)
      return;
    if (isWebGL) {
      switch (mode) {
      case REFRESH_REPAINT:
      case REFRESH_SYNC:
      case REFRESH_SEND_WEBGL_NEW_ORIENTATION:
        tm.finalizeTransformParameters();

        if (html5Applet == null)
          return;
        html5Applet._refresh();
        if (mode == REFRESH_SEND_WEBGL_NEW_ORIENTATION)
          return;
        break;
      }
    } else {
      rm.repaintIfReady("refresh " + mode + " " + strWhy);
    }
    // Q: Why this, since all of these % 3 != 0
    if (/*mode % REFRESH_SYNC_MASK != 0 && */ sm.doSync())
      sm.setSync(mode == REFRESH_SYNC ? strWhy : null);
  }

  public void requestRepaintAndWait(String why) {
    // called by moveUpdate from move, moveTo, navigate,
    // navTranslate
    // called by ScriptEvaluator "refresh" command
    // called by AnimationThread run()
    // called by TransformationManager move and moveTo
    // called by TransformationManager11 navigate, navigateTo
    if (rm == null)
      return;
    if (!haveDisplay) {
      setModelVisibility();
      shm.finalizeAtoms(null, true);
      return;
    }
    rm.requestRepaintAndWait(why);
    setSync();
  }

  public void clearShapeRenderers() {
    clearRepaintManager(-1);
  }

  public boolean isRepaintPending() {
    return (rm == null ? false : rm.isRepaintPending());
  }

  @Override
  public void notifyViewerRepaintDone() {
    if (rm != null)
      rm.repaintDone();
    am.repaintDone();
  }

  private boolean axesAreTainted = false;

  public boolean areAxesTainted() {
    boolean TF = axesAreTainted;
    axesAreTainted = false;
    return TF;
  }

  @Override
  public String generateOutputForExport(Map<String, Object> params) {
    return (noGraphicsAllowed || rm == null ? null
        : getOutputManager().getOutputFromExport(params));
  }

  private void clearRepaintManager(int iShape) {
    if (rm != null)
      rm.clear(iShape);
  }

  /**
   * JmolViewer interface uses this, but that is all
   */
  @Override
  public void renderScreenImage(Object g, int width, int height) {
    renderScreenImageStereo(g, false, width, height);
  }

  public void renderScreenImageStereo(Object gLeft, boolean checkStereoSlave,
                                      int width, int height) {
    // from paint/update event
    // gRight is for second stereo applet
    // when this is the stereoSlave, no rendering occurs through this applet
    // directly, only from the other applet.
    // this is for relatively specialized geoWall-type installations

    //      Jmol repaint/update system for the application:
    //      
    //      threads invoke vwr.refresh()
    //       
    //        --> repaintManager.refresh()
    //        --> vwr.repaint() 
    //        --> display.repaint()
    //        --> OS event queue calls Applet.paint() 
    //        --> vwr.renderScreenImage() 
    //        --> vwr.notifyViewerRepaintDone() 
    //        --> repaintManager.repaintDone()
    //        --> which sets repaintPending false and does notify();

    if (updateWindow(width, height)) {
      if (!checkStereoSlave || gRight == null) {
        getScreenImageBuffer(gLeft, false);
      } else {
        drawImage(gRight, getImage(true, false), 0, 0, tm.stereoDoubleDTI);
        drawImage(gLeft, getImage(false, false), 0, 0, tm.stereoDoubleDTI);
      }
    }
    if (captureParams != null
        && Boolean.FALSE != captureParams.get("captureEnabled")) {
      captureParams.remove("imagePixels");
      //showString(transformManager.matrixRotate.toString(), false);
      long t = ((Long) captureParams.get("endTime")).longValue();
      if (t > 0 && System.currentTimeMillis() + 50 > t)
        captureParams.put("captureMode", "end");
      processWriteOrCapture(captureParams);
    }
    notifyViewerRepaintDone();
  }

  public Map<String, Object> captureParams;
  private Map<String, Object> jsParams;

  /**
   * for JavaScript only
   * 
   */
  public void updateJS() {
    if (isWebGL) {
      if (jsParams == null) {
        jsParams = new Hashtable<String, Object>();
        jsParams.put("type", "JS");
      }
      if (updateWindow(0, 0))
        render();
      notifyViewerRepaintDone();
    } else {
      if (isStereoSlave)
        return;
      // getGraphics returns a canvas context2d
      renderScreenImageStereo(apiPlatform.getGraphics(null), true, 0, 0);
    }
  }

  /**
   * File has been loaded or model has been changed or atom picked. This is a
   * call to Jmol.View for view sets (new in Jmol 14.1.8)
   * 
   * @param imodel
   * @param iatom
   * 
   */
  private void updateJSView(int imodel, int iatom) {
    if (html5Applet == null)
      return;
    @SuppressWarnings("unused")
    JSmolAppletObject applet = this.html5Applet;
    boolean doViewPick = true;
    /**
     * @j2sNative
     * 
     *            doViewPick = (applet != null && applet._viewSet != null);
     * 
     */
    {
    }
    if (doViewPick)
      html5Applet._atomPickedCallback(imodel, iatom);
  }

  // ///////////////////////////////////////////////////////////////
  // routines for script support
  // ///////////////////////////////////////////////////////////////

  @Override
  public String evalFile(String strFilename) {
    // from JmolApp and test suite only
    return (allowScripting && getScriptManager() != null
        ? scm.evalFile(strFilename)
        : null);
  }

  public String getInsertedCommand() {
    String s = insertedCommand;
    insertedCommand = "";
    if (Logger.debugging && s != "")
      Logger.debug("inserting: " + s);
    return s;
  }

  @Override
  public String script(String strScript) {
    // JmolViewer -- just an alias for evalString
    return evalStringQuietSync(strScript, false, true);
  }

  @Override
  public String evalString(String strScript) {
    // JmolSimpleViewer
    return evalStringQuietSync(strScript, false, true);
  }

  @Override
  public String evalStringQuiet(String strScript) {
    // JmolViewer 
    return evalStringQuietSync(strScript, true, true);
  }

  public String evalStringQuietSync(String strScript, boolean isQuiet,
                                    boolean allowSyncScript) {

    return (getScriptManager() == null ? null
        : scm.evalStringQuietSync(strScript, isQuiet, allowSyncScript));
  }

  public void clearScriptQueue() {
    if (scm != null)
      scm.clearQueue();
  }

  private void setScriptQueue(boolean TF) {
    g.useScriptQueue = TF;
    if (!TF)
      clearScriptQueue();
  }

  @Override
  public boolean checkHalt(String str, boolean isInsert) {
    return (scm != null && scm.checkHalt(str, isInsert));
  }

  // / direct no-queue use:

  @Override
  public String scriptWait(String strScript) {
    return (String) evalWait("JSON", strScript,
        "+scriptStarted,+scriptStatus,+scriptEcho,+scriptTerminated");
  }

  @Override
  public Object scriptWaitStatus(String strScript, String statusList) {
    // null statusList will return a String 
    //  -- output from PRINT/MESSAGE/ECHO commands or an error message
    // otherwise, specific status messages will be created as a Java object
    return evalWait("object", strScript, statusList);
  }

  private Object evalWait(String returnType, String strScript,
                          String statusList) {
    //can't do waitForQueue in JavaScript and then wait for the queue:
    if (getScriptManager() == null)
      return null;
    scm.waitForQueue();
    boolean doTranslateTemp = GT.setDoTranslate(false);
    Object ret = evalStringWaitStatusQueued(returnType, strScript, statusList,
        false, false);
    GT.setDoTranslate(doTranslateTemp);
    return ret;
  }

  public void exitJmol() {
    if (isApplet && !isJNLP)
      return;
    if (headlessImageParams != null) {
      try {
        if (headless)
          outputToFile(headlessImageParams);
      } catch (Exception e) {
        //
      }
    }

    if (Logger.debugging)
      Logger.debug("exitJmol -- exiting");
    System.out.flush();
    System.exit(0);
  }

  private Object scriptCheckRet(String strScript, boolean returnContext) {
    return (getScriptManager() == null ? null
        : scm.scriptCheckRet(strScript, returnContext));
  }

  @Override
  public synchronized Object scriptCheck(String strScript) {
    return scriptCheckRet(strScript, false);
  }

  @Override
  public boolean isScriptExecuting() {
    return (eval != null && eval.isExecuting());
  }

  @Override
  public void haltScriptExecution() {
    if (eval != null) {
      eval.haltExecution();
      eval.stopScriptThreads();
    }
    setStringPropertyTok("pathForAllFiles", T.pathforallfiles, "");
    clearTimeouts();
  }

  public void pauseScriptExecution() {
    if (eval != null)
      eval.pauseExecution(true);
  }

  String resolveDatabaseFormat(String fileName) {
    return (hasDatabasePrefix(fileName)
        || fileName.indexOf(JC.legacyResolver) >= 0
            ? (String) setLoadFormat(fileName, fileName.charAt(0), false)
            : fileName);
  }

  public static boolean hasDatabasePrefix(String fileName) {
    return (fileName.length() != 0 && isDatabaseCode(fileName.charAt(0)));
  }

  public static boolean isDatabaseCode(char ch) {
    return (ch == '*' // PDBE
        || ch == '$' // NCI resolver
        || ch == '=' // RCSB model or ligand
        || ch == ':' // PubChem
    );
  }

  /**
   * Jmol will either specify a type or look for it in the first character,
   * making sure it is found using isDatabaseCode() first. Starting with Jmol
   * 13.1.13, we allow a generalized search using =xxx= where xxx is a known or
   * user-specified database designation.
   * 
   * @param name
   * @param type
   *        a character to distinguish the type of file, '?' means we are just
   *        doing an isosurface check
   * @param withPrefix
   * @return String or String[]
   */
  public Object setLoadFormat(String name, char type, boolean withPrefix) {
    String format = null;
    String id = name.substring(1);
    switch (type) {
    case 'c': // cache:local//... legacyResolver....
      return name;
    case 'h':
      // legacy resolver https://
      checkCIR(false);
      return g.nihResolverFormat
          + name.substring(name.indexOf("/structure") + 10);
    case '=':
      if (name.startsWith("==")) {
        id = id.substring(1);
        type = '#';
      } else if (id.indexOf("/") > 0) {
        // =xxxx/....
        try {
          int pt = id.indexOf("/");
          String database = id.substring(0, pt);
          id = JC.resolveDataBase(database, id.substring(pt + 1), null);
          if (id != null && id.startsWith("'"))
            id = evaluateExpression(id).toString();
          return (id == null || id.length() == 0 ? name : id);
        } catch (Exception e) {
          return name;
        }
      } else {
        if (id.endsWith(".mmtf")) {
          id = id.substring(0, id.indexOf(".mmtf"));
          return JC.resolveDataBase("mmtf", id.toUpperCase(), null);
        }
        format = g.loadFormat;
      }
      //$FALL-THROUGH$
    case '#': // ligand
      if (format == null)
        format = g.pdbLoadLigandFormat;
      return JC.resolveDataBase(null, id, format);
    case '*':
      // European Bioinformatics Institute
      int pt = name.lastIndexOf("/");
      if (name.startsWith("*dom/")) {
        //  *dom/.../.../.../xxxx
        id = name.substring(pt + 1);
        format = (pt > 4 ? name.substring(5) : "mappings");
        return PT.rep(JC.resolveDataBase("map", id, null), "%TYPE", format);
      } else if (name.startsWith("*val/")) {
        //  *val/.../.../.../xxxx
        id = name.substring(pt + 1);
        format = (pt > 4 ? name.substring(5) : "validation/outliers/all");
        return PT.rep(JC.resolveDataBase("map", id, null), "%TYPE", format);
      } else if (name.startsWith("*rna3d/")) {
        //  *rna3d/.../.../.../xxxx
        id = name.substring(pt + 1);
        format = (pt > 6 ? name.substring(6) : "loops");
        return PT.rep(JC.resolveDataBase("rna3d", id, null), "%TYPE", format);
      } else if (name.startsWith("*dssr--")) {
        id = name.substring(pt + 1);
        id = JC.resolveDataBase("dssr", id, null);
        return id + "%20" + PT.rep(name.substring(5, pt), " ", "%20");
      } else if (name.startsWith("*dssr/")) {
        id = name.substring(pt + 1);
        return JC.resolveDataBase("dssr", id, null);
      } else if (name.startsWith("*dssr1/")) {
        id = name.substring(pt + 1);
        return JC.resolveDataBase("dssr1", id, null);
      }
      // these are processed in SmarterJmolAdapter
      String pdbe = "pdbe";
      if (id.length() == 5 && id.charAt(4) == '*') {
        pdbe = "pdbe2";
        id = id.substring(0, 4);
      }
      return JC.resolveDataBase(pdbe, id, null);
    case ':': // PubChem
      format = g.pubChemFormat;
      if (id.equals("")) {
        try {
          id = "smiles:" + getOpenSmiles(bsA());
        } catch (Exception e) {
          // oh well.
        }
      }
      String fl = id.toLowerCase();
      int fi = Integer.MIN_VALUE;
      try {
        fi = Integer.parseInt(id);
      } catch (Exception e) {
        //
      }
      if (fi != Integer.MIN_VALUE) {
        id = "cid/" + fi;
      } else {
        if (fl.startsWith("smiles:")) {
          format += "?POST?smiles=" + id.substring(7);
          id = "smiles";
        } else if (id.startsWith("cid:") || id.startsWith("inchikey:")
            || id.startsWith("cas:")) {
          id = id.replace(':', '/');
        } else {
          if (fl.startsWith("name:"))
            id = id.substring(5);
          id = "name/" + PT.escapeUrl(id);
        }
      }
      return PT.formatStringS(format, "FILE", id);
    case '$':
      checkCIR(false);
      if (name.equals("$")) {
        try {
          id = getOpenSmiles(bsA());
        } catch (Exception e) {
          // oh well...
        }
      } else if (name.startsWith("$$")) {
        // 2D version
        id = id.substring(1);
        if (id.length() == 0) {
          try {
            id = getOpenSmiles(bsA());
          } catch (Exception e) {
          }
        }
        //http://cactus.nci.nih.gov/chemical/structure/C%28O%29CCC/file?format=sdf
        format = PT.rep(g.smilesUrlFormat, "get3d=true", "get3d=false");
        return PT.formatStringS(format, "FILE", PT.escapeUrl(id));
      }
      //$FALL-THROUGH$
    case 'M':
    case 'N':
    case '2':
    case 'I':
    case 'K':
    case 'S':
    case 'T':
    case '/':
      id = PT.escapeUrl(id);
      switch (type) {
      case 'M':
      case 'N':
        format = g.nihResolverFormat + "/%FILE/names";
        break;
      case '2':
        format = g.nihResolverFormat + "/%FILE/image";
        break;
      case 'I':
      case 'T':
        format = g.nihResolverFormat + "/%FILE/stdinchi";
        break;
      case 'K':
        format = g.nihResolverFormat + "/%FILE/inchikey";
        break;
      case 'S':
        format = g.nihResolverFormat + "/%FILE/stdinchikey";
        break;
      case '/':
        format = g.nihResolverFormat + "/%FILE/";
        break;
      default:
        format = g.smilesUrlFormat;
        break;
      }
      return (withPrefix ? "MOL3D::" : "")
          + PT.formatStringS(format, "FILE", id);
    case '?': // check only
    case '-': // localized version using the PDBe density server and box....
    case '_': // isosurface "=...", but we code that type as '_'
      // now *xxxx or =xxxx   --  both go to EBI instead of Uppsala, 
      // as Uppsala is being decommissioned in 2018
      boolean isDiff = id.startsWith("*") || id.startsWith("=");
      if (isDiff)
        id = id.substring(1);
      String ciftype = null;
      pt = id.indexOf(".");
      if (pt >= 0) {
        ciftype = id.substring(pt + 1);
        id = id.substring(0, pt);
      }
      boolean checkXray = id.startsWith("density");
      if (checkXray)
        id = "em" + id.substring(7);
      if (id.equals("emdb") || id.equals("em"))
        id += "/";
      if (id.startsWith("em/"))
        id = "emdb" + id.substring(2);
      if (id.startsWith("emdb/")) {
        // *emdb/9357
        // *emdb/=6nef
        // *emdb/=, *emdb/, *emdb
        id = id.substring(5);
        if (id.length() == 0)
          id = "=";
        else if (id.startsWith("*"))
          id = "=" + id.substring(1);
        String emdext = "#-sigma=10";
        if (id.startsWith("=")) {
          id = (id.equals("=") ? getPdbID()
              : id.substring(1));
          if (id == null || type == '?')
            return id;
          String q = JC.resolveDataBase("emdbquery", id, null);
          String data = (String) fm.cacheGet(q, false);
          if (data == null) {
              showString("retrieving " + q, false);
            data = getFileAsString(q);
            if (data == null) {
              showString("EM retrieve failed for " + id, false);
              if (!checkXray)
                return null;
              data = "FAILED";
            } else {
              showString(data, false);
            }
            fm.cachePut(q, data);
          }
          pt = data.indexOf("EMD-");
          if (pt >= 0) {
            id = data.substring(pt + 4);
            pt = id.indexOf('\n');
            if (pt > 0)
              id = id.substring(0, pt);
            pt = id.indexOf(",");
            if (pt > 0) {
              emdext = "#-cutoff=" + id.substring(pt + 1);
              id = id.substring(0, pt);
            }
          } else {
            if (!checkXray)
              return null;
            emdext = null;
          }
        }
        if (emdext != null)
          return JC.resolveDataBase("emdbmap" + (type == '-' ? "server" : ""), id,
              null) + emdext;
      }
      id = JC.resolveDataBase(
          (isDiff ? "pdbemapdiff" : "pdbemap") + (type == '-' ? "server" : ""),
          id, null);
      if ("cif".equals(ciftype)) {
        id = id.replace("bcif", "cif");
      }
      break;
    }
    return id;
  }

  boolean cirChecked;

  /**
   * Check to see if the resolver is working
   * 
   * @param forceCheck
   */
  private void checkCIR(boolean forceCheck) {
    if (cirChecked && !forceCheck)
      return;
    try {
      g.removeParam("_cirStatus");
      Map<String, Object> m = getModelSetAuxiliaryInfo();
      m.remove("cirInfo");
      Map<String, Object> map = parseJSONMap(
          getFileAsString(g.resolverResolver));
      m.put("cirInfo", map);
      ms.msInfo = m;
      String s = (String) map.get("status");
      g.setO("_cirStatus", s);
      g.setCIR((String) map.get("rfc6570Template"));
      System.out.println("Viewer.checkCIR _.cirInfo.status = " + s);
    } catch (Throwable t) {
      System.out.println(
          "Viewer.checkCIR failed at " + g.resolverResolver + ": " + t);
    }
    cirChecked = true;
  }

  public String getStandardLabelFormat(int type) {
    switch (type) {
    default:
    case 0: // standard
      return LabelToken.STANDARD_LABEL;
    case 1:
      return g.defaultLabelXYZ;
    case 2:
      return g.defaultLabelPDB;
    }
  }

  public P3[] getAdditionalHydrogens(BS bsAtoms, Lst<Atom> vConnections,
                                     int flags) {
    if (bsAtoms == null)
      bsAtoms = bsA();
    int[] nTotal = new int[1];
    P3[][] pts = ms.calculateHydrogens(bsAtoms, nTotal, vConnections, flags);
    P3[] points = new P3[nTotal[0]];
    for (int i = 0, pt = 0; i < pts.length; i++)
      if (pts[i] != null)
        for (int j = 0; j < pts[i].length; j++)
          points[pt++] = pts[i][j];
    return points;
  }

  @Override
  public void setMarBond(short marBond) {
    g.bondRadiusMilliAngstroms = marBond;
    g.setI("bondRadiusMilliAngstroms", marBond);
    setShapeSize(JC.SHAPE_STICKS, marBond * 2, BSUtil.setAll(ms.ac));
  }

  private int hoverAtomIndex = -1;
  private String hoverText, hoverLabel = "%U";
  private boolean hoverEnabled = true;

  public void setHoverLabel(String strLabel) {
    shm.loadShape(JC.SHAPE_HOVER);
    setShapeProperty(JC.SHAPE_HOVER, "label", strLabel);
    setHoverEnabled(strLabel != null);
    g.setO("_hoverLabel", hoverLabel = strLabel);
    if (!hoverEnabled && !sm.haveHoverCallback())
      startHoverWatcher(false);
  }

  private void setHoverEnabled(boolean tf) {
    hoverEnabled = tf;
    g.setB("_hoverEnabled", tf);
  }

  /*
   * hoverCallback reports information about the atom being hovered over.
   * 
   * jmolSetCallback("hoverCallback", "myHoverCallback") function
   * myHoverCallback(strInfo, iAtom) {}
   * 
   * strInfo == the atom's identity, including x, y, and z coordinates iAtom ==
   * the index of the atom being hovered over
   * 
   * Viewer.setStatusAtomHovered Hover.setProperty("target") Viewer.hoverOff
   * Viewer.hoverOn
   */
  void hoverOn(int atomIndex, boolean isLabel) {
    g.removeParam("_objecthovered");
    g.setI("_atomhovered", atomIndex);
    g.setO("_hoverLabel", hoverLabel);
    g.setUserVariable("hovered",
        SV.getVariable(BSUtil.newAndSetBit(atomIndex)));
    if (sm.haveHoverCallback())
      sm.setStatusAtomHovered(atomIndex, getAtomInfoXYZ(atomIndex, false));
    if (!hoverEnabled || eval != null && isScriptExecuting()
        || atomIndex == hoverAtomIndex || g.hoverDelayMs == 0
        || !slm.isInSelectionSubset(atomIndex))
      return;
    String label = (isLabel ? GT.$("Drag to move label")
        : g.modelKitMode && modelkit != null
            ? (String) modelkit.setProperty("hoverLabel",
                Integer.valueOf(atomIndex))
            : null);

    shm.loadShape(JC.SHAPE_HOVER);
    if (label != null
        && (!isLabel || ms.at[atomIndex].isVisible(JC.VIS_LABEL_FLAG))) {
      setShapeProperty(JC.SHAPE_HOVER, "specialLabel", label);
    }
    setShapeProperty(JC.SHAPE_HOVER, "text", hoverText = null);
    setShapeProperty(JC.SHAPE_HOVER, "target",
        Integer.valueOf(hoverAtomIndex = atomIndex));
    refresh(REFRESH_SYNC_MASK, "hover on atom");
  }

  /**
   * Hover over an arbitrary point.
   * 
   * @param x
   * @param y
   * @param text
   * @param id
   *        optional id to set _objecthovered to
   * @param pt
   *        optional pt to set "hovered" to
   */
  public void hoverOnPt(int x, int y, String text, String id, T3 pt) {
    // from draw for drawhover on
    if (eval != null && isScriptExecuting())
      return;
    g.setO("_hoverLabel", text);
    if (id != null && pt != null) {
      g.setO("_objecthovered", id);
      g.setI("_atomhovered", -1);
      g.setUserVariable("hovered", SV.getVariable(pt));
      if (sm.haveHoverCallback())
        sm.setStatusObjectHovered(id, text, pt);
    }
    if (!hoverEnabled)
      return;
    shm.loadShape(JC.SHAPE_HOVER);
    setShapeProperty(JC.SHAPE_HOVER, "xy", P3i.new3(x, y, 0));
    setShapeProperty(JC.SHAPE_HOVER, "target", null);
    setShapeProperty(JC.SHAPE_HOVER, "specialLabel", null);
    setShapeProperty(JC.SHAPE_HOVER, "text", text);
    hoverAtomIndex = -1;
    hoverText = text;
    refresh(REFRESH_SYNC_MASK, "hover on point");
  }

  void hoverOff() {
    try {
      if (g.modelKitMode
          && acm.getBondPickingMode() != ActionManager.PICKING_ROTATE_BOND)
        highlight(null);
      if (!hoverEnabled)
        return;
      boolean isHover = (hoverText != null || hoverAtomIndex >= 0);
      if (hoverAtomIndex >= 0) {
        setShapeProperty(JC.SHAPE_HOVER, "target", null);
        hoverAtomIndex = -1;
      }
      if (hoverText != null) {
        setShapeProperty(JC.SHAPE_HOVER, "text", null);
        hoverText = null;
      }
      setShapeProperty(JC.SHAPE_HOVER, "specialLabel", null);
      if (isHover)
        refresh(REFRESH_SYNC_MASK, "hover off");
    } catch (Exception e) {
      // ignore
    }
  }

  @Override
  public void setDebugScript(boolean debugScript) {
    g.debugScript = debugScript;
    g.setB("debugScript", debugScript);
    if (eval != null)
      eval.setDebugging();
  }

  void clearClickCount() {
    setTainted(true);
  }

  public int currentCursor = GenericPlatform.CURSOR_DEFAULT;

  public void setCursor(int cursor) {
    if (isKiosk || currentCursor == cursor || multiTouch || !haveDisplay)
      return;
    apiPlatform.setCursor(currentCursor = cursor, display);
  }

  public void setPickingMode(String strMode, int pickingMode) {
    if (!haveDisplay)
      return;
    showSelected = false;
    String option = null;
    if (strMode != null) {
      int pt = strMode.indexOf("_");
      if (pt >= 0) {
        option = strMode.substring(pt + 1);
        strMode = strMode.substring(0, pt);
      }
      pickingMode = ActionManager.getPickingMode(strMode);
    }
    if (pickingMode < 0)
      pickingMode = ActionManager.PICKING_IDENTIFY;
    acm.setPickingMode(pickingMode);
    g.setO("picking",
        ActionManager.getPickingModeName(acm.getAtomPickingMode()));
    if (option == null || option.length() == 0)
      return;
    option = Character.toUpperCase(option.charAt(0))
        + (option.length() == 1 ? "" : option.substring(1, 2));
    switch (pickingMode) {
    case ActionManager.PICKING_ASSIGN_ATOM:
      getModelkit(false).setProperty("atomType", option);
      break;
    case ActionManager.PICKING_ASSIGN_BOND:
      getModelkit(false).setProperty("bondType", option);
      break;
    default:
      Logger.error("Bad picking mode: " + strMode + "_" + option);
    }
  }

  public int getPickingMode() {
    return (haveDisplay ? acm.getAtomPickingMode() : 0);
  }

  void setPickingStyle(String style, int pickingStyle) {
    if (!haveDisplay)
      return;
    if (style != null)
      pickingStyle = ActionManager.getPickingStyleIndex(style);
    if (pickingStyle < 0)
      pickingStyle = ActionManager.PICKINGSTYLE_SELECT_JMOL;
    acm.setPickingStyle(pickingStyle);
    g.setO("pickingStyle",
        ActionManager.getPickingStyleName(acm.getPickingStyle()));
  }

  public boolean getDrawHover() {
    return haveDisplay && g.drawHover;
  }

  private P3 ptTemp;

  public String getAtomInfo(int atomOrPointIndex) {
    if (ptTemp == null)
      ptTemp = new P3();
    // only for MeasurementTable and actionManager
    return (atomOrPointIndex >= 0
        ? ms.getAtomInfo(atomOrPointIndex, null, ptTemp)
        : (String) shm.getShapePropertyIndex(JC.SHAPE_MEASURES, "pointInfo",
            -atomOrPointIndex));
  }

  private String getAtomInfoXYZ(int atomIndex, boolean useChimeFormat) {
    Atom atom = ms.at[atomIndex];
    if (useChimeFormat)
      return getChimeMessenger().getInfoXYZ(atom);
    if (ptTemp == null)
      ptTemp = new P3();
    return atom.getIdentityXYZ(true, ptTemp);
  }

  // //////////////status manager dispatch//////////////

  private void setSync() {
    if (sm.doSync())
      sm.setSync(null);
  }

  @Override
  public void setJmolCallbackListener(JmolCallbackListener listener) {
    sm.cbl = listener;
  }

  @Override
  public void setJmolStatusListener(JmolStatusListener listener) {
    sm.cbl = sm.jsl = listener;
  }

  public Lst<Lst<Lst<Object>>> getStatusChanged(String statusNameList) {
    return (statusNameList == null ? null
        : sm.getStatusChanged(statusNameList));
  }

  public boolean menuEnabled() {
    return (!g.disablePopupMenu && getPopupMenu() != null);
  }

  public boolean setStatusDragDropped(int mode, int x, int y, String fileName) {
    if (mode == 0) {
      g.setO("_fileDropped", fileName);
      g.setUserVariable("doDrop", SV.vT);
    }
    boolean handled = sm.setStatusDragDropped(mode, x, y, fileName);
    return (!handled || getP("doDrop").toString().equals("true"));
  }

  /*
   * resizeCallback is called whenever the applet gets a resize notification
   * from the browser
   * 
   * jmolSetCallback("resizeCallback", "myResizeCallback") function
   * myResizeCallback(width, height) {}
   */

  public void setStatusResized(int width, int height) {
    sm.setStatusResized(width, height);
  }

  /*
   * scriptCallback is the primary way to monitor script status. In addition, it
   * serves to for passing information to the user over the status line of the
   * browser as well as to the console. Note that console messages are also sent
   * by echoCallback. If messageCallback is enabled but not scriptCallback,
   * these messages go to the messageCallback function instead.
   * 
   * jmolSetCallback("scriptCallback", "myScriptCallback") function
   * myScriptCallback(app, status, message, intStatus, errorMessageUntranslated)
   * {}
   * 
   * intStatus == -2 script start -- message is the script itself intStatus == 0
   * general messages during script execution; translated error message may be
   * present intStatus >= 1 script termination message; translated and
   * untranslated message may be present value is time for execution in
   * milliseconds
   * 
   * Eval.defineAtomSet -- compilation bug indicates problem in JmolConstants
   * array Eval.instructionDispatchLoop -- debugScript messages
   * Eval.logDebugScript -- debugScript messages Eval.pause -- script execution
   * paused message Eval.runEval -- "Script completed" message Eval.script --
   * Chime "script <exiting>" message Eval.scriptStatusOrBuffer -- various
   * messages for Eval.checkContinue (error message) Eval.connect Eval.delete
   * Eval.hbond Eval.load (logMessages message) Eval.message Eval.runEval (error
   * message) Eval.write (error reading file) Eval.zap (error message)
   * FileManager.createAtomSetCollectionFromFile "requesting..." for Chime-like
   * compatibility actionManager.atomPicked
   * "pick one more atom in order to spin..." for example
   * Viewer.evalStringWaitStatus -- see above -2, 0 only if error, >=1 at
   * termination Viewer.reportSelection "xxx atoms selected"
   */

  public void scriptStatus(String strStatus) {
    setScriptStatus(strStatus, "", 0, null);
  }

  public void scriptStatusMsg(String strStatus, String statusMessage) {
    setScriptStatus(strStatus, statusMessage, 0, null);
  }

  public void setScriptStatus(String strStatus, String statusMessage,
                              int msWalltime,
                              String strErrorMessageUntranslated) {
    sm.setScriptStatus(strStatus, statusMessage, msWalltime,
        strErrorMessageUntranslated);
  }

  /*
   * syncCallback traps script synchronization messages and allows for
   * cancellation (by returning "") or modification
   * 
   * jmolSetCallback("syncCallback", "mySyncCallback") function
   * mySyncCallback(app, script, appletName) { ...[modify script here]... return
   * newScript }
   * 
   * StatusManager.syncSend Viewer.setSyncTarget Viewer.syncScript
   */

  @Override
  public void showUrl(String urlString) {
    // applet.Jmol
    // app Jmol
    // StatusManager
    if (urlString == null)
      return;
    if (urlString.indexOf(":") < 0) {
      String base = fm.getAppletDocumentBase();
      if (base == "")
        base = fm.getFullPathName(false);
      if (base.indexOf("/") >= 0) {
        base = base.substring(0, base.lastIndexOf("/") + 1);
      } else if (base.indexOf("\\") >= 0) {
        base = base.substring(0, base.lastIndexOf("\\") + 1);
      }
      urlString = base + urlString;
    }
    Logger.info("showUrl:" + urlString);
    sm.showUrl(urlString);
  }

  /**
   * an external applet or app with class that extends org.jmol.jvxl.MeshCreator
   * might execute:
   * 
   * org.jmol.viewer.Viewer vwr = applet.getViewer(); vwr.setMeshCreator(this);
   * 
   * then that class's updateMesh(String id) method will be called whenever a
   * mesh is rendered.
   * 
   * @param meshCreator
   */
  public void setMeshCreator(Object meshCreator) {
    shm.loadShape(JC.SHAPE_ISOSURFACE);
    setShapeProperty(JC.SHAPE_ISOSURFACE, "meshCreator", meshCreator);
  }

  public void showConsole(boolean showConsole) {
    if (!haveDisplay)
      return;
    // Eval
    try {
      if (appConsole == null && showConsole)
        getConsole();
      appConsole.setVisible(true);
    } catch (Throwable e) {
      // no console for this client... maybe no Swing
    }
  }

  public JmolAppConsoleInterface getConsole() {
    getProperty("DATA_API", "getAppConsole", Boolean.TRUE);
    return appConsole;
  }

  @Override
  public Object getParameter(String key) {
    return getP(key);
  }

  public Object getP(String key) {
    return g.getParameter(key, true);
  }

  public Object getPOrNull(String key) {
    return g.getParameter(key, false);
  }

  public void unsetProperty(String key) {
    key = key.toLowerCase();
    if (key.equals("all") || key.equals("variables"))
      fm.setPathForAllFiles("");
    g.unsetUserVariable(key);
  }

  @Override
  public void notifyStatusReady(boolean isReady) {
    System.out.println(
        "Jmol applet " + fullName + (isReady ? " ready" : " destroyed"));
    if (!isReady)
      dispose();
    sm.setStatusAppletReady(fullName, isReady);
  }

  @Override
  public boolean getBooleanProperty(String key) {
    key = key.toLowerCase();
    if (g.htBooleanParameterFlags.containsKey(key))
      return g.htBooleanParameterFlags.get(key).booleanValue();
    // special cases
    if (key.endsWith("p!")) {
      if (acm == null)
        return false;
      String s = acm.getPickingState().toLowerCase();
      key = key.substring(0, key.length() - 2) + ";";
      return (s.indexOf(key) >= 0);
    }
    if (key.equalsIgnoreCase("executionPaused"))
      return (eval != null && eval.isPaused());
    if (key.equalsIgnoreCase("executionStepping"))
      return (eval != null && eval.isStepping());
    if (key.equalsIgnoreCase("haveBFactors"))
      return (ms.getBFactors() != null);
    if (key.equalsIgnoreCase("colorRasmol"))
      return cm.isDefaultColorRasmol;
    if (key.equalsIgnoreCase("frank"))
      return getShowFrank();
    if (key.equalsIgnoreCase("spinOn"))
      return tm.spinOn;
    if (key.equalsIgnoreCase("isNavigating"))
      return tm.isNavigating();
    if (key.equalsIgnoreCase("showSelections"))
      return selectionHalosEnabled;
    if (g.htUserVariables.containsKey(key)) {
      SV t = g.getUserVariable(key);
      if (t.tok == T.on)
        return true;
      if (t.tok == T.off)
        return false;
    }
    Logger.error("vwr.getBooleanProperty(" + key + ") - unrecognized");
    return false;
  }

  @Override
  public int getInt(int tok) {
    switch (tok) {
    case T.animationfps:
      return am.animationFps;
    case T.dotdensity:
      return g.dotDensity;
    case T.dotscale:
      return g.dotScale;
    case T.helixstep:
      return g.helixStep;
    case T.infofontsize:
      return g.infoFontSize;
    case T.labelpointerwidth:
      return g.labelPointerWidth;
    case T.meshscale:
      return g.meshScale;
    case T.minpixelselradius:
      return g.minPixelSelRadius;
    case T.percentvdwatom:
      return g.percentVdwAtom;
    case T.pickingspinrate:
      return g.pickingSpinRate;
    case T.ribbonaspectratio:
      return g.ribbonAspectRatio;
    case T.showscript:
      return g.scriptDelay;
    case T.minimizationmaxatoms:
      return g.minimizationMaxAtoms;
    case T.smallmoleculemaxatoms:
      return g.smallMoleculeMaxAtoms;
    case T.strutspacing:
      return g.strutSpacing;
    case T.vectortrail:
      return g.vectorTrail;
    }
    Logger.error("viewer.getInt(" + T.nameOf(tok) + ") - not listed");
    return 0;
  }

  // special cases:

  public int getDelayMaximumMs() {
    return (haveDisplay ? g.delayMaximumMs : 1);
  }

  public int getHermiteLevel() {
    return (tm.spinOn && g.hermiteLevel > 0 ? 0 : g.hermiteLevel);
  }

  public int getHoverDelay() {
    return (g.modelKitMode ? 20 : g.hoverDelayMs);

  }

  @Override
  public boolean getBoolean(int tok) {
    switch (tok) {
    case T.nbocharges:
      return g.nboCharges;
    case T.hiddenlinesdashed:
      return g.hiddenLinesDashed;
    case T.pdb:
      return ms.getMSInfoB("isPDB");
    case T.autoplaymovie:
      return g.autoplayMovie;
    case T.allowaudio:
      return !headless && g.allowAudio;
    case T.allowgestures:
      return g.allowGestures;
    case T.allowmultitouch:
      return g.allowMultiTouch;
    case T.allowrotateselected:
      return g.allowRotateSelected;
    case T.appendnew:
      return g.appendNew;
    case T.applysymmetrytobonds:
      return g.applySymmetryToBonds;
    case T.atompicking:
      return g.atomPicking;
    case T.autobond:
      return g.autoBond;
    case T.autofps:
      return g.autoFps;
    case T.axesorientationrasmol:
      return g.axesOrientationRasmol;
    case T.cartoonsteps:
      return g.cartoonSteps;
    case T.cartoonblocks:
      return g.cartoonBlocks;
    case T.checkcir:
      return g.checkCIR;
    case T.bondmodeor:
      return g.bondModeOr;
    case T.cartoonbaseedges:
      return g.cartoonBaseEdges;
    case T.cartoonsfancy:
      return g.cartoonFancy;
    case T.cartoonladders:
      return g.cartoonLadders;
    case T.cartoonribose:
      return g.cartoonRibose;
    case T.cartoonrockets:
      return g.cartoonRockets;
    case T.chaincasesensitive:
      return g.chainCaseSensitive || chainCaseSpecified;
    case T.ciprule6full:
      return g.cipRule6Full;
    case T.debugscript:
      return g.debugScript;
    case T.defaultstructuredssp:
      return g.defaultStructureDSSP;
    case T.disablepopupmenu:
      return g.disablePopupMenu;
    case T.displaycellparameters:
      return g.displayCellParameters;
    case T.dotsurface:
      return g.dotSurface;
    case T.dotsselectedonly:
      return g.dotsSelectedOnly;
    case T.drawpicking:
      return g.drawPicking;
    case T.fontcaching:
      return g.fontCaching;
    case T.fontscaling:
      return g.fontScaling;
    case T.forceautobond:
      return g.forceAutoBond;
    case T.fractionalrelative:
      return false;//g.fractionalRelative;
    case T.greyscalerendering:
      return g.greyscaleRendering;
    case T.hbondsbackbone:
      return g.hbondsBackbone;
    case T.hbondsrasmol:
      return g.hbondsRasmol;
    case T.hbondssolid:
      return g.hbondsSolid;
    case T.hetero:
      return g.rasmolHeteroSetting;
    case T.hidenameinpopup:
      return g.hideNameInPopup;
    case T.highresolution:
      return g.highResolutionFlag;
    case T.hydrogen:
      return g.rasmolHydrogenSetting;
    case T.isosurfacekey:
      return g.isosurfaceKey;
    case T.jmolinjspecview:
      return g.jmolInJSpecView;
    case T.justifymeasurements:
      return g.justifyMeasurements;
    case T.legacyautobonding:
      // aargh -- BitSet efficiencies in Jmol 11.9.24, 2/3/2010, meant that
      // state files created before that that use select BONDS will select the
      // wrong bonds. 
      // reset after a state script is read
      return g.legacyAutoBonding;
    case T.legacyhaddition:
      // aargh -- Some atoms missed before Jmol 13.1.17
      return g.legacyHAddition;
    case T.legacyjavafloat:
      return g.legacyJavaFloat;
    case T.loggestures:
      return g.logGestures;
    case T.measureallmodels:
      return g.measureAllModels;
    case T.measurementlabels:
      return g.measurementLabels;
    case T.messagestylechime:
      return g.messageStyleChime;
    case T.modelkitmode:
      return g.modelKitMode;
    case T.multiplebondbananas:
      return g.multipleBondBananas;
    case T.navigationmode:
      return g.navigationMode;
    case T.navigationperiodic:
      return g.navigationPeriodic;
    case T.partialdots:
      return g.partialDots;
    case T.pdbaddhydrogens:
      return g.pdbAddHydrogens;
    case T.pdbsequential:
      return g.pdbSequential;
    case T.preservestate:
      return g.preserveState;
    case T.refreshing:
      return refreshing;
    case T.ribbonborder:
      return g.ribbonBorder;
    case T.rocketbarrels:
      return g.rocketBarrels;
    case T.nodelay:
      return g.noDelay;
    case T.selectallmodels:
      return g.selectAllModels;
    case T.showhiddenselectionhalos:
      return g.showHiddenSelectionHalos;
    case T.showhydrogens:
      return g.showHydrogens;
    case T.showmeasurements:
      return g.showMeasurements;
    case T.showmodvecs:
      return g.showModVecs;
    case T.showmultiplebonds:
      return g.showMultipleBonds;
    case T.showtiming:
      return g.showTiming;
    case T.showunitcelldetails:
      return g.showUnitCellDetails;
    case T.slabbyatom:
      return g.slabByAtom;
    case T.slabbymolecule:
      return g.slabByMolecule;
    case T.smartaromatic:
      return g.smartAromatic;
    case T.solventprobe:
      return g.dotSolvent;
    case T.ssbondsbackbone:
      return g.ssbondsBackbone;
    case T.strutsmultiple:
      return g.strutsMultiple;
    case T.testflag1:
      // CIPChirality -- turns off tracking (skip creation of _M.CIPInfo for speed tests)
      // no PNGJ caching
      // debug mouse actions
      return g.testFlag1;
    case T.testflag2:
      // no load processing (jmolscript or 2D file load minimization)
      // passed to MOCalcuation, but not used
      // nciCalculation special params.testFlag = 2 "absolute" calc.
      // GIF reducedColors
      // plug-in use variable
      return g.testFlag2;
    case T.testflag3:
      // isosurface numbers
      // polyhedra numbers
      // pmesh triangles
      return g.testFlag3;
    case T.testflag4:
      // isosurface normals
      return g.testFlag4;

    case T.tracealpha:
      return g.traceAlpha;
    case T.translucent:
      return g.translucent;
    case T.twistedsheets:
      return g.twistedSheets;
    case T.vectorscentered:
      return g.vectorsCentered;
    case T.vectorsymmetry:
      return g.vectorSymmetry;
    case T.waitformoveto:
      return g.waitForMoveTo;
    case T.zerobasedxyzrasmol:
      return g.zeroBasedXyzRasmol;
    }
    Logger.error("viewer.getBoolean(" + T.nameOf(tok) + ") - not listed");
    return false;
  }

  // special cases:

  public boolean allowEmbeddedScripts() {
    return (g.allowEmbeddedScripts && !isPreviewOnly);
  }

  boolean getDragSelected() {
    return (g.dragSelected && !g.modelKitMode);
  }

  boolean getBondsPickable() {
    return (g.bondPicking || g.modelKitMode
        && getModelkitProperty("isMolecular") == Boolean.TRUE);
  }

  public boolean useMinimizationThread() {
    return (g.useMinimizationThread && !autoExit);
  }

  @Override
  public float getFloat(int tok) {
    switch (tok) {
    case T.atoms:
      return g.particleRadius;
    case T.axesoffset:
      return g.axesOffset;
    case T.axesscale:
      return g.axesScale;
    case T.bondtolerance:
      return g.bondTolerance;
    case T.defaulttranslucent:
      return g.defaultTranslucent;
    case T.defaultdrawarrowscale:
      return g.defaultDrawArrowScale;
    case T.dipolescale:
      return g.dipoleScale;
    case T.drawfontsize:
      return g.drawFontSize;
    case T.exportscale:
      return g.exportScale;
    case T.hbondsangleminimum:
      return g.hbondsAngleMinimum;
    case T.hbondhxdistancemaximum:
      return g.hbondHXDistanceMaximum;
    case T.hbondnodistancemaximum:
      return g.hbondNODistanceMaximum;
    case T.loadatomdatatolerance:
      return g.loadAtomDataTolerance;
    case T.minbonddistance:
      return g.minBondDistance;
    case T.modulation:
      return g.modulationScale;
    case T.multiplebondspacing:
      return g.multipleBondSpacing;
    case T.multiplebondradiusfactor:
      return g.multipleBondRadiusFactor;
    case T.navigationspeed:
      return g.navigationSpeed;
    case T.pointgroupdistancetolerance:
      return g.pointGroupDistanceTolerance;
    case T.pointgrouplineartolerance:
      return g.pointGroupLinearTolerance;
    case T.rotationradius:
      return tm.modelRadius;
    case T.sheetsmoothing:
      return g.sheetSmoothing;
    case T.solventproberadius:
      return g.solventProbeRadius;
    case T.starwidth:
      return g.starWidth;
    case T.strutdefaultradius:
      return g.strutDefaultRadius;
    case T.strutlengthmaximum:
      return g.strutLengthMaximum;
    case T.vectorscale:
      return g.vectorScale;
    case T.vibrationperiod:
      return g.vibrationPeriod;
    case T.cartoonblockheight:
      // 14.11.0
      return g.cartoonBlockHeight;
    }
    Logger.error("viewer.getFloat(" + T.nameOf(tok) + ") - not listed");
    return 0;
  }

  @Override
  public void setStringProperty(String key, String value) {
    if (value == null || key == null || key.length() == 0)
      return;
    if (key.charAt(0) == '_') {
      g.setO(key, value);
      return;
    }
    int tok = T.getTokFromName(key);
    switch (T.getParamType(tok)) {
    case T.booleanparam:
      setBooleanPropertyTok(key, tok, SV.newV(T.string, value).asBoolean());
      break;
    case T.intparam:
      setIntPropertyTok(key, tok, SV.newV(T.string, value).asInt());
      break;
    case T.floatparam:
      setFloatPropertyTok(key, tok, PT.parseFloat(value));
      break;
    default:
      setStringPropertyTok(key, tok, value);
    }
  }

  private void setStringPropertyTok(String key, int tok, String value) {
    switch (tok) {
    // 14.29.54 new
    case T.macrodirectory:
      g.macroDirectory = value = (value == null || value.length() == 0
          ? JC.defaultMacroDirectory
          : value);
      macros = null;
      break;
    // 14.4.10 new
    case T.nihresolverformat:
      g.nihResolverFormat = value;
      break;
    // removed for Jmol 14.29.1
    //    // 14.3.10 (forgot to add these earlier)
    //    case T.edsurlcutoff:
    //      g.edsUrlCutoff = value;
    //      break;
    //    case T.edsurlformat:
    //      g.edsUrlFormat = value;
    //      break;
    //    // 14.3.10 new
    //    case T.edsurlformatdiff:
    //      g.edsUrlFormatDiff = value;
    //      break;
    // 13.3.6
    case T.animationmode:
      setAnimationMode(value);
      return;
    case T.nmrpredictformat:
      // 13.3.4
      g.nmrPredictFormat = value;
      break;
    case T.defaultdropscript:
      // 13.1.2
      // for File|Open and Drag/drop
      g.defaultDropScript = value;
      break;

    case T.pathforallfiles:
      // 12.3.29
      value = fm.setPathForAllFiles(value);
      break;
    case T.energyunits:
      // 12.3.26
      setUnits(value, false);
      return;
    case T.forcefield:
      // 12.3.25
      g.forceField = value = ("UFF".equalsIgnoreCase(value) ? "UFF"
          : "UFF2D".equalsIgnoreCase(value) ? "UFF2D"
              : "MMFF2D".equalsIgnoreCase(value) ? "MMFF2D" : "MMFF");
      minimizer = null;
      break;
    case T.nmrurlformat:
      // 12.3.3
      g.nmrUrlFormat = value;
      break;
    case T.measurementunits:
      setUnits(value, true);
      return;
    case T.loadligandformat:
      // /12.1.51//
      g.pdbLoadLigandFormat = value;
      break;
    // 12.1.50
    case T.defaultlabelpdb:
      g.defaultLabelPDB = value;
      break;
    case T.defaultlabelxyz:
      g.defaultLabelXYZ = value;
      break;
    case T.defaultloadfilter:
      // 12.0.RC10
      g.defaultLoadFilter = value;
      break;
    case T.logfile:
      value = getOutputManager().setLogFile(value);
      if (value == null)
        return;
      break;
    case T.filecachedirectory:
      // 11.9.21
      // not implemented -- application only -- CANNOT BE SET BY STATE
      // global.fileCacheDirectory = value;
      break;
    case T.atomtypes:
      // 11.7.7
      g.atomTypes = value;
      break;
    case T.currentlocalpath:
      // /11.6.RC15
      break;
    case T.picklabel:
      // /11.5.42
      g.pickLabel = value;
      break;
    case T.quaternionframe:
      // /11.5.39//
      if (value.length() == 2 && value.startsWith("R"))
        // C, P -- straightness from Ramachandran angles
        g.quaternionFrame = value.substring(0, 2);
      else
        g.quaternionFrame = "" + (value.toLowerCase() + "p").charAt(0);
      if (!PT.isOneOf(g.quaternionFrame, JC.allowedQuaternionFrames))
        g.quaternionFrame = "p";
      ms.haveStraightness = false;
      break;
    case T.defaultvdw:
      // /11.5.11//
      setVdwStr(value);
      return;
    case T.language:
      // /11.1.30//
      // fr cs en none, etc.
      // also serves to change language for callbacks and menu
      new GT(this, value);
      String language = GT.getLanguage();
      modelkit = null;
      if (jmolpopup != null) {
        jmolpopup.jpiDispose();
        jmolpopup = null;
        getPopupMenu();
      }
      sm.setCallbackFunction("language", language);
      value = GT.getLanguage();
      break;
    case T.loadformat:
      // /11.1.22//
      g.loadFormat = value;
      break;
    case T.backgroundcolor:
      // /11.1///
      setObjectColor("background", value);
      return;
    case T.axis1color:
      setObjectColor("axis1", value);
      return;
    case T.axis2color:
      setObjectColor("axis2", value);
      return;
    case T.axis3color:
      setObjectColor("axis3", value);
      return;
    case T.boundboxcolor:
      setObjectColor("boundbox", value);
      return;
    case T.unitcellcolor:
      setObjectColor("unitcell", value);
      return;
    case T.propertycolorscheme:
      setPropertyColorScheme(value, false, false);
      break;
    case T.hoverlabel:
      // a special label for selected atoms
      shm.loadShape(JC.SHAPE_HOVER);
      setShapeProperty(JC.SHAPE_HOVER, "atomLabel", value);
      break;
    case T.defaultdistancelabel:
      // /11.0///
      g.defaultDistanceLabel = value;
      break;
    case T.defaultanglelabel:
      g.defaultAngleLabel = value;
      break;
    case T.defaulttorsionlabel:
      g.defaultTorsionLabel = value;
      break;
    case T.defaultloadscript:
      g.defaultLoadScript = value;
      break;
    case T.appletproxy:
      fm.setAppletProxy(value);
      break;
    case T.defaultdirectory:
      if (value == null)
        value = "";
      value = value.replace('\\', '/');
      g.defaultDirectory = value;
      break;
    case T.helppath:
      g.helpPath = value;
      break;
    case T.defaults:
      if (!value.equalsIgnoreCase("RasMol") && !value.equalsIgnoreCase("PyMOL"))
        value = "Jmol";
      setDefaultsType(value);
      break;
    case T.defaultcolorscheme:
      // only two are possible: "jmol" and "rasmol"
      setDefaultColors(value.equalsIgnoreCase("rasmol"));
      return;
    case T.picking:
      setPickingMode(value, 0);
      return;
    case T.pickingstyle:
      setPickingStyle(value, 0);
      return;
    case T.dataseparator:
      // just saving this
      break;
    default:
      if (key.toLowerCase().endsWith("callback")) {
        sm.setCallbackFunction(key,
            (value.length() == 0 || value.equalsIgnoreCase("none") ? null
                : value));
        break;
      }
      if (!g.htNonbooleanParameterValues.containsKey(key.toLowerCase())) {
        g.setUserVariable(key, SV.newV(T.string, value));
        return;
      }
      // a few String parameters may not be tokenized. Save them anyway.
      // for example, defaultDirectoryLocal
      break;
    }
    g.setO(key, value);
  }

  @Override
  public void setFloatProperty(String key, float value) {
    if (Float.isNaN(value) || key == null || key.length() == 0)
      return;
    if (key.charAt(0) == '_') {
      g.setF(key, value);
      return;
    }
    int tok = T.getTokFromName(key);
    switch (T.getParamType(tok)) {
    case T.strparam:
      setStringPropertyTok(key, tok, "" + value);
      break;
    case T.booleanparam:
      setBooleanPropertyTok(key, tok, value != 0);
      break;
    case T.intparam:
      setIntPropertyTok(key, tok, (int) value);
      break;
    default:
      setFloatPropertyTok(key, tok, value);
    }
  }

  private void setFloatPropertyTok(String key, int tok, float value) {
    switch (tok) {
    case T.cartoonblockheight:
      // 14.11.0
      g.cartoonBlockHeight = value;
      break;
    case T.modulationscale:
      // 14.0.1
      ms.setModulation(null, false, null, false);
      g.modulationScale = value = Math.max(0.1f, value);
      ms.setModulation(null, true, null, false);
      break;
    case T.particleradius:
      // 13.3.9
      g.particleRadius = Math.abs(value);
      break;
    case T.drawfontsize:
      // 13.3.6
      g.drawFontSize = value;
      shm.setShapePropertyBs(JC.SHAPE_DRAW, "font", null, null);
      break;
    case T.exportscale:
      // 13.1.19
      g.exportScale = value;
      break;
    case T.starwidth:
      // 13.1.15
      g.starWidth = value;
      break;
    case T.multiplebondradiusfactor:
      // 12.1.11
      g.multipleBondRadiusFactor = value;
      break;
    case T.multiplebondspacing:
      // 12.1.11
      g.multipleBondSpacing = value;
      break;
    case T.slabrange:
      tm.setSlabRange(value);
      break;
    case T.minimizationcriterion:
      g.minimizationCriterion = value;
      break;
    case T.gestureswipefactor:
      if (haveDisplay)
        acm.setGestureSwipeFactor(value);
      break;
    case T.mousedragfactor:
      if (haveDisplay)
        acm.setMouseDragFactor(value);
      break;
    case T.mousewheelfactor:
      if (haveDisplay)
        acm.setMouseWheelFactor(value);
      break;
    case T.strutlengthmaximum:
      // 11.9.21
      g.strutLengthMaximum = value;
      break;
    case T.strutdefaultradius:
      g.strutDefaultRadius = value;
      break;
    case T.navx:
      // 11.7.47
      setSpin("X", (int) value);
      break;
    case T.navy:
      setSpin("Y", (int) value);
      break;
    case T.navz:
      setSpin("Z", (int) value);
      break;
    case T.navfps:
      if (Float.isNaN(value))
        return;
      setSpin("FPS", (int) value);
      break;
    case T.loadatomdatatolerance:
      g.loadAtomDataTolerance = value;
      break;
    case T.hbondsangleminimum:
      // 11.7.9
      g.hbondsAngleMinimum = value;
      break;
    case T.hbondhxdistancemaximum:
      // 14.31.33
      g.hbondHXDistanceMaximum = value;
      break;
    case T.hbondnodistancemaximum:
      // 11.7.9
      g.hbondNODistanceMaximum = value;
      break;
    case T.pointgroupdistancetolerance:
      // 11.6.RC2//
      g.pointGroupDistanceTolerance = value;
      break;
    case T.pointgrouplineartolerance:
      g.pointGroupLinearTolerance = value;
      break;
    case T.ellipsoidaxisdiameter:
      g.ellipsoidAxisDiameter = value;
      break;
    case T.spinx:
      // /11.3.52//
      setSpin("x", (int) value);
      break;
    case T.spiny:
      setSpin("y", (int) value);
      break;
    case T.spinz:
      setSpin("z", (int) value);
      break;
    case T.spinfps:
      setSpin("fps", (int) value);
      break;
    case T.defaultdrawarrowscale:
      // /11.3.17//
      g.defaultDrawArrowScale = value;
      break;
    case T.defaulttranslucent:
      // /11.1///
      g.defaultTranslucent = value;
      break;
    case T.axesoffset:
      setAxesScale(tok, value);
      break;
    case T.axesscale:
      setAxesScale(tok, value);
      break;
    case T.visualrange:
      tm.visualRangeAngstroms = value;
      refresh(REFRESH_REPAINT, "set visualRange");
      break;
    case T.navigationdepth:
      setNavigationDepthPercent(value);
      break;
    case T.navigationspeed:
      g.navigationSpeed = value;
      break;
    case T.navigationslab:
      tm.setNavigationSlabOffsetPercent(value);
      break;
    case T.cameradepth:
      tm.setCameraDepthPercent(value, false);
      refresh(REFRESH_REPAINT, "set cameraDepth");
      // transformManager will set global value for us;
      return;
    case T.rotationradius:
      setRotationRadius(value, true);
      return;
    case T.hoverdelay:
      g.hoverDelayMs = (int) (value * 1000);
      break;
    case T.sheetsmoothing:
      // /11.0///
      g.sheetSmoothing = value;
      break;
    case T.dipolescale:
      value = checkFloatRange(value, -10, 10);
      g.dipoleScale = value;
      break;
    case T.stereodegrees:
      tm.setStereoDegrees(value);
      break;
    case T.vectorscale:
      // public -- no need to set
      setVectorScale(value);
      return;
    case T.vibrationperiod:
      // public -- no need to set
      setVibrationPeriod(value);
      return;
    case T.vibrationscale:
      // public -- no need to set
      setVibrationScale(value);
      return;
    case T.bondtolerance:
      setBondTolerance(value);
      return;
    case T.minbonddistance:
      setMinBondDistance(value);
      return;
    case T.scaleangstromsperinch:
      tm.setScaleAngstromsPerInch(value);
      break;
    case T.solventproberadius:
      value = checkFloatRange(value, 0, 10);
      g.solventProbeRadius = value;
      break;
    default:
      if (!g.htNonbooleanParameterValues.containsKey(key.toLowerCase())) {
        g.setUserVariable(key, SV.newF(value));
        return;
      }
    }
    g.setF(key, value);
  }

  @Override
  public void setIntProperty(String key, int value) {
    if (value == Integer.MIN_VALUE || key == null || key.length() == 0)
      return;
    if (key.charAt(0) == '_') {
      g.setI(key, value);
      return;
    }
    int tok = T.getTokFromName(key);
    switch (T.getParamType(tok)) {
    case T.strparam:
      setStringPropertyTok(key, tok, "" + value);
      break;
    case T.booleanparam:
      setBooleanPropertyTok(key, tok, value != 0);
      break;
    case T.floatparam:
      setFloatPropertyTok(key, tok, value);
      break;
    default:
      setIntPropertyTok(key, tok, value);
    }
  }

  private void setIntPropertyTok(String key, int tok, int value) {
    switch (tok) {
    case T.labelpointerwidth:
      // 14.32.15
      g.labelPointerWidth = value;
      break;
    case T.minimizationmaxatoms:
      // 14.30.0
      g.minimizationMaxAtoms = value;
      break;
    case T.infofontsize:
      g.infoFontSize = Math.max(0, value);
      break;
    case T.contextdepthmax:
    case T.historylevel:
    case T.scriptreportinglevel:
      value = eval.setStatic(tok, value);
      break;
    case T.vectortrail:
      g.vectorTrail = value;
      break;
    case T.bondingversion:
      // 14.1.11
      value = (value == 0 ? Elements.RAD_COV_IONIC_OB1_100_1
          : Elements.RAD_COV_BODR_2014_02_22);
      g.bondingVersion = Elements.bondingVersion = value;
      break;
    case T.celshadingpower:
      // 13.3.9
      gdata.setCelPower(value);
      break;
    case T.ambientocclusion:
      // 13.3.9
      gdata.setAmbientOcclusion(value);
      break;
    case T.platformspeed:
      // 13.3.4
      g.platformSpeed = Math.min(Math.max(value, 0), 10); // 0 could mean "adjust as needed"
      break;
    case T.meshscale:
      // 12.3.29
      g.meshScale = value;
      break;
    case T.minpixelselradius:
      // 12.2.RC6
      g.minPixelSelRadius = value;
      break;
    case T.isosurfacepropertysmoothingpower:
      // 12.1.11
      g.isosurfacePropertySmoothingPower = value;
      break;
    case T.repaintwaitms:
      // 12.0.RC4
      g.repaintWaitMs = value;
      break;
    case T.smallmoleculemaxatoms:
      // 12.0.RC3
      g.smallMoleculeMaxAtoms = value;
      break;
    case T.minimizationsteps:
      g.minimizationSteps = value;
      break;
    case T.strutspacing:
      // 11.9.21
      g.strutSpacing = value;
      break;
    case T.phongexponent:
      // 11.9.13
      value = checkIntRange(value, 0, 1000);
      gdata.setPhongExponent(value);
      break;
    case T.helixstep:
      // 11.8.RC3
      g.helixStep = value;
      ms.haveStraightness = false;
      break;
    case T.dotscale:
      // 12.0.RC25
      g.dotScale = value;
      break;
    case T.dotdensity:
      // 11.6.RC2//
      g.dotDensity = value;
      break;
    case T.delaymaximumms:
      // 11.5.4//
      g.delayMaximumMs = value;
      break;
    case T.loglevel:
      // /11.3.52//
      Logger.setLogLevel(value);
      Logger.info("logging level set to " + value);
      g.setI("logLevel", value);
      if (eval != null)
        eval.setDebugging();
      return;
    case T.axesmode:
      setAxesMode(value == 2 ? T.axesunitcell
          : value == 1 ? T.axesmolecular : T.axeswindow);
      return;
    case T.strandcount:
      // /11.1///
      setStrandCount(0, value);
      return;
    case T.strandcountforstrands:
      setStrandCount(JC.SHAPE_STRANDS, value);
      return;
    case T.strandcountformeshribbon:
      setStrandCount(JC.SHAPE_MESHRIBBON, value);
      return;
    case T.perspectivemodel:
      // abandoned in 13.1.10
      //setPerspectiveModel(value);
      return;
    case T.showscript:
      g.scriptDelay = value;
      break;
    case T.specularpower:
      if (value < 0)
        value = checkIntRange(value, -10, -1);
      else
        value = checkIntRange(value, 0, 100);
      gdata.setSpecularPower(value);
      break;
    case T.specularexponent:
      value = checkIntRange(-value, -10, -1);
      gdata.setSpecularPower(value);
      break;
    case T.bondradiusmilliangstroms:
      setMarBond((short) value);
      // public method -- no need to set
      return;
    case T.specular:
      setBooleanPropertyTok(key, tok, value == 1);
      return;
    case T.specularpercent:
      value = checkIntRange(value, 0, 100);
      gdata.setSpecularPercent(value);
      break;
    case T.diffusepercent:
      value = checkIntRange(value, 0, 100);
      gdata.setDiffusePercent(value);
      break;
    case T.ambientpercent:
      value = checkIntRange(value, 0, 100);
      gdata.setAmbientPercent(value);
      break;
    case T.zdepth:
      tm.zDepthToPercent(value);
      break;
    case T.zslab:
      tm.zSlabToPercent(value);
      break;
    case T.depth:
      tm.depthToPercent(value);
      break;
    case T.slab:
      tm.slabToPercent(value);
      break;
    case T.zshadepower:
      g.zShadePower = value = Math.max(value, 0);
      break;
    case T.ribbonaspectratio:
      g.ribbonAspectRatio = value;
      break;
    case T.pickingspinrate:
      g.pickingSpinRate = (value < 1 ? 1 : value);
      break;
    case T.animationfps:
      setAnimationFps(value);
      return;
    case T.percentvdwatom:
      setPercentVdwAtom(value);
      break;
    case T.hermitelevel:
      g.hermiteLevel = value;
      break;
    case T.ellipsoiddotcount: // 11.5.30
    case T.propertyatomnumbercolumncount:
    case T.propertyatomnumberfield: // 11.6.RC16
    case T.propertydatacolumncount:
    case T.propertydatafield: // 11.1.31
      // just save in the hashtable, not in global
      break;
    default:
      // stateversion is not tokenized
      if (!g.htNonbooleanParameterValues.containsKey(key)) {
        g.setUserVariable(key, SV.newI(value));
        return;
      }
    }
    g.setI(key, value);
  }

  private static int checkIntRange(int value, int min, int max) {
    return (value < min ? min : value > max ? max : value);
  }

  private static float checkFloatRange(float value, float min, float max) {
    return (value < min ? min : value > max ? max : value);
  }

  @Override
  public void setBooleanProperty(String key, boolean value) {
    if (key == null || key.length() == 0)
      return;
    if (key.charAt(0) == '_') {
      g.setB(key, value);
      return;
    }
    int tok = T.getTokFromName(key);
    switch (T.getParamType(tok)) {
    case T.strparam:
      setStringPropertyTok(key, tok, "");
      break;
    case T.intparam:
      setIntPropertyTok(key, tok, value ? 1 : 0);
      break;
    case T.floatparam:
      setFloatPropertyTok(key, tok, value ? 1 : 0);
      break;
    default:
      setBooleanPropertyTok(key, tok, value);
    }
  }

  private void setBooleanPropertyTok(String key, int tok, boolean value) {
    boolean doRepaint = true;
    switch (tok) {
    case T.checkcir:
      // 14.31.40
      g.checkCIR = value;
      if (value) {
        checkCIR(true);
      }
      break;
    case T.ciprule6full:
      // 14.29.14
      g.cipRule6Full = value;
      break;
    case T.autoplaymovie:
      // 14.29.2
      g.autoplayMovie = value;
      break;
    case T.allowaudio:
      // 14.29.2
      value = false;
      // cannot be set TRUE once set FALSE
      g.allowAudio = value;
      break;
    case T.nodelay:
      // 14.21.1
      g.noDelay = value;
      break;
    case T.nbocharges:
      // 14.8.2
      g.nboCharges = value;
      break;
    case T.hiddenlinesdashed:
      // 14.5.1
      g.hiddenLinesDashed = value;
      break;
    case T.multiplebondbananas:
      // 14.3.15
      g.multipleBondBananas = value;
      break;
    case T.modulateoccupancy:
      // 12.0.RC6
      g.modulateOccupancy = value;
      break;
    case T.legacyjavafloat:
      // 14.3.5
      g.legacyJavaFloat = value;
      break;
    case T.showmodvecs:
      // 14.3.5
      g.showModVecs = value;
      break;
    case T.showunitcelldetails:
      // 14.1.16
      g.showUnitCellDetails = value;
      break;
    case T.fractionalrelative:
      // REMOVED in 14.1.16
      // an odd quantity -- relates specifically to scripts commands
      // using fx, fy, fz, fxyz, ux, uy, uz, uxyz, and cell=
      // It should never have been set in state, as it has nothing to do with the state 
      // Its use with cell= was never documented.
      // It makes no sense to change the unit cell and not change the 
      // meaning of fx, fy, fz, fxyz, ux, uy, uz, uxyz, and cell=
      // Its presence caused unitcell [{origin} {a} {b} {c}] to fail.

      //g.fractionalRelative = value;
      doRepaint = false;
      break;
    case T.vectorscentered:
      // 14.1.15
      g.vectorsCentered = value;
      break;
    case T.cartoonblocks:
      // 14.11.0
      g.cartoonBlocks = value;
      break;
    case T.cartoonsteps:
      // 14.1.14
      g.cartoonSteps = value;
      break;
    case T.cartoonribose:
      // 14.1.8
      g.cartoonRibose = value;
      //      if (value && getBoolean(T.cartoonbaseedges))
      //        setBooleanPropertyTok("cartoonBaseEdges", T.cartoonbaseedges, false);
      break;
    case T.ellipsoidarrows:
      // 13.1.17 TRUE for little points on ellipsoids showing sign of 
      // eigenvalues (in --> negative; out --> positive)
      g.ellipsoidArrows = value;
      break;
    case T.translucent:
      // 13.1.17 false -> translucent objects are opaque among themselves (Pymol transparency_mode 2)
      g.translucent = value;
      break;
    case T.cartoonladders:
      // 13.1.15
      g.cartoonLadders = value;
      break;
    case T.twistedsheets:
      boolean b = g.twistedSheets;
      g.twistedSheets = value;
      if (b != value)
        checkCoordinatesChanged();
      break;
    case T.celshading:
      // 13.1.13
      gdata.setCel(value);
      break;
    case T.cartoonsfancy:
      // 12.3.7
      g.cartoonFancy = value;
      break;
    case T.showtiming:
      // 12.3.6
      g.showTiming = value;
      break;
    case T.vectorsymmetry:
      // 12.3.2
      g.vectorSymmetry = value;
      break;
    case T.isosurfacekey:
      // 12.2.RC5
      g.isosurfaceKey = value;
      break;
    case T.partialdots:
      // Jmol 12.1.46
      g.partialDots = value;
      break;
    case T.legacyautobonding:
      g.legacyAutoBonding = value;
      break;
    case T.defaultstructuredssp:
      g.defaultStructureDSSP = value;
      break;
    case T.dsspcalchydrogen:
      g.dsspCalcHydrogen = value;
      break;
    case T.allowmodelkit:
      // 11.12.RC15
      g.allowModelkit = value;
      if (!value)
        setModelKitMode(false);
      break;
    case T.modelkitmode:
      setModelKitMode(value);
      break;
    case T.multiprocessor:
      // 12.0.RC6
      g.multiProcessor = value && (nProcessors > 1);
      break;
    case T.monitorenergy:
      // 12.0.RC6
      g.monitorEnergy = value;
      break;
    case T.hbondsrasmol:
      // 12.0.RC3
      g.hbondsRasmol = value;
      break;
    case T.minimizationrefresh:
      g.minimizationRefresh = value;
      break;
    case T.minimizationsilent:
      // 12.0.RC5
      g.minimizationSilent = value;
      break;
    //case T.usearcball:
    //g.useArcBall = value;
    //break;
    case T.iskiosk:
      // 11.9.29
      // 12.2.9, 12.3.9: no false here, because it's a one-time setting
      if (value) {
        isKiosk = true;
        g.disablePopupMenu = true;
        if (display != null)
          apiPlatform.setTransparentCursor(display);
      }
      break;
    // 11.9.28
    case T.waitformoveto:
      g.waitForMoveTo = value;
      break;
    case T.logcommands:
      g.logCommands = true;
      break;
    case T.loggestures:
      g.logGestures = true;
      break;
    case T.allowmultitouch:
      // 11.9.24
      g.allowMultiTouch = value;
      break;
    case T.preservestate:
      // 11.9.23
      g.preserveState = value;
      ms.setPreserveState(value);
      undoClear();
      break;
    case T.strutsmultiple:
      // 11.9.23
      g.strutsMultiple = value;
      break;
    case T.filecaching:
      // 11.9.21
      // not implemented -- application only -- CANNOT BE SET BY STATE
      break;
    case T.slabbyatom:
      // 11.9.19
      g.slabByAtom = value;
      break;
    case T.slabbymolecule:
      // 11.9.18
      g.slabByMolecule = value;
      break;
    case T.saveproteinstructurestate:
      // 11.9.15
      g.saveProteinStructureState = value;
      break;
    case T.allowgestures:
      g.allowGestures = value;
      break;
    case T.imagestate:
      // 11.8.RC6
      g.imageState = value;
      break;
    case T.useminimizationthread:
      // 11.7.40
      g.useMinimizationThread = value;
      break;
    // case Token.autoloadorientation:
    // // 11.7.30; removed in 12.0.RC10 -- use FILTER "NoOrient"
    // global.autoLoadOrientation = value;
    // break;
    case T.allowkeystrokes:
      // 11.7.24
      //      if (g.disablePopupMenu)
      //        value = false;
      g.allowKeyStrokes = value;
      break;
    case T.dragselected:
      // 11.7.24
      g.dragSelected = value;
      showSelected = false;
      break;
    case T.showkeystrokes:
      g.showKeyStrokes = value;
      break;
    case T.fontcaching:
      // 11.7.10
      g.fontCaching = value;
      break;
    case T.atompicking:
      // 11.6.RC13
      g.atomPicking = value;
      break;
    case T.bondpicking:
      // 11.6.RC13
      highlight(null);
      g.bondPicking = value;
      break;
    case T.selectallmodels:
      // 11.5.52
      g.selectAllModels = value;
      if (value)
        slm.setSelectionSubset(null);
      else
        am.setSelectAllSubset(false);
      break;
    case T.messagestylechime:
      // 11.5.39
      g.messageStyleChime = value;
      break;
    case T.pdbsequential:
      g.pdbSequential = value;
      break;
    case T.pdbaddhydrogens:
      g.pdbAddHydrogens = value;
      break;
    case T.pdbgetheader:
      g.pdbGetHeader = value;
      break;
    case T.ellipsoidaxes:
      g.ellipsoidAxes = value;
      break;
    case T.ellipsoidarcs:
      g.ellipsoidArcs = value;
      break;
    case T.ellipsoidball:
      g.ellipsoidBall = value;
      break;
    case T.ellipsoiddots:
      g.ellipsoidDots = value;
      break;
    case T.ellipsoidfill:
      g.ellipsoidFill = value;
      break;
    case T.fontscaling:
      // 11.5.4
      g.fontScaling = value;
      break;
    case T.syncmouse:
      // 11.3.56
      setSyncTarget(0, value);
      break;
    case T.syncscript:
      setSyncTarget(1, value);
      break;
    case T.wireframerotation:
      // 11.3.55
      g.wireframeRotation = value;
      break;
    case T.isosurfacepropertysmoothing:
      // 11.3.46
      g.isosurfacePropertySmoothing = value;
      break;
    case T.drawpicking:
      // 11.3.43
      g.drawPicking = value;
      break;
    case T.antialiasdisplay:
      // 11.3.36
    case T.antialiastranslucent:
    case T.antialiasimages:
      setAntialias(tok, value);
      break;
    case T.smartaromatic:
      // 11.3.29
      g.smartAromatic = value;
      break;
    case T.applysymmetrytobonds:
      // 11.1.29
      setApplySymmetryToBonds(value);
      break;
    case T.appendnew:
      // 11.1.22
      g.appendNew = value;
      break;
    case T.autofps:
      g.autoFps = value;
      break;
    case T.usenumberlocalization:
      // 11.1.21
      DF.setUseNumberLocalization(g.useNumberLocalization = value);
      break;
    case T.showfrank:
    case T.frank:
      key = "showFrank";
      setFrankOn(value);
      // 11.1.20
      break;
    case T.solvent:
      key = "solventProbe";
      g.dotSolvent = value;
      break;
    case T.solventprobe:
      g.dotSolvent = value;
      break;
    case T.allowrotateselected:
      // 11.1.14
      g.allowRotateSelected = value;
      break;
    case T.allowmoveatoms:
      // 12.1.21
      //setBooleanProperty("allowRotateSelected", value);
      //setBooleanProperty("dragSelected", value);
      g.allowMoveAtoms = value;
      showSelected = false;
      break;
    case T.showscript:
      // /11.1.13///
      setIntPropertyTok("showScript", tok, value ? 1 : 0);
      return;
    case T.allowembeddedscripts:
      // /11.1///
      g.allowEmbeddedScripts = value;
      break;
    case T.navigationperiodic:
      g.navigationPeriodic = value;
      break;
    case T.zshade:
      tm.setZShadeEnabled(value);
      return;
    case T.drawhover:
      if (haveDisplay)
        g.drawHover = value;
      break;
    case T.navigationmode:
      setNavigationMode(value);
      break;
    case T.navigatesurface:
      // was experimental; abandoned in 13.1.10
      return;//global.navigateSurface = value;
    //break;
    case T.hidenavigationpoint:
      g.hideNavigationPoint = value;
      break;
    case T.shownavigationpointalways:
      g.showNavigationPointAlways = value;
      break;
    case T.refreshing:
      // /11.0///
      setRefreshing(value);
      break;
    case T.jmolinjspecview:
      g.jmolInJSpecView = value;
      break;
    case T.justifymeasurements:
      g.justifyMeasurements = value;
      break;
    case T.ssbondsbackbone:
      g.ssbondsBackbone = value;
      break;
    case T.hbondsbackbone:
      g.hbondsBackbone = value;
      break;
    case T.hbondssolid:
      g.hbondsSolid = value;
      break;
    case T.specular:
      gdata.setSpecular(value);
      break;
    case T.slabenabled:
      // Eval.slab
      tm.setSlabEnabled(value); // refresh?
      return;
    case T.zoomenabled:
      tm.setZoomEnabled(value);
      return;
    case T.highresolution:
      g.highResolutionFlag = value;
      break;
    case T.tracealpha:
      g.traceAlpha = value;
      break;
    case T.zoomlarge:
      g.zoomLarge = value;
      tm.setZoomHeight(g.zoomHeight, value);
      break;
    case T.zoomheight:
      g.zoomHeight = value;
      tm.setZoomHeight(value, g.zoomLarge);
      break;
    case T.languagetranslation:
      GT.setDoTranslate(value);
      break;
    case T.hidenotselected:
      slm.setHideNotSelected(value);
      break;
    case T.scriptqueue:
      setScriptQueue(value);
      break;
    case T.dotsurface:
      g.dotSurface = value;
      break;
    case T.dotsselectedonly:
      g.dotsSelectedOnly = value;
      break;
    case T.selectionhalos:
      setSelectionHalosEnabled(value);
      break;
    case T.selecthydrogen:
      g.rasmolHydrogenSetting = value;
      break;
    case T.selecthetero:
      g.rasmolHeteroSetting = value;
      break;
    case T.showmultiplebonds:
      g.showMultipleBonds = value;
      break;
    case T.showhiddenselectionhalos:
      g.showHiddenSelectionHalos = value;
      break;
    case T.windowcentered:
      tm.setWindowCentered(value);
      break;
    case T.displaycellparameters:
      g.displayCellParameters = value;
      break;
    case T.testflag1:
      g.testFlag1 = value;
      break;
    case T.testflag2:
      g.testFlag2 = value;
      break;
    case T.testflag3:
      g.testFlag3 = value;
      break;
    case T.testflag4:
      jmolTest();
      g.testFlag4 = value;
      break;
    case T.ribbonborder:
      g.ribbonBorder = value;
      break;
    case T.cartoonbaseedges:
      g.cartoonBaseEdges = value;
      //      if (value && getBoolean(T.cartoonribose))
      //        setBooleanPropertyTok("cartoonRibose", T.cartoonribose, false);
      break;
    case T.cartoonrockets:
      g.cartoonRockets = value;
      break;
    case T.rocketbarrels:
      g.rocketBarrels = value;
      break;
    case T.greyscalerendering:
      gdata.setGreyscaleMode(g.greyscaleRendering = value);
      break;
    case T.measurementlabels:
      g.measurementLabels = value;
      break;
    case T.axeswindow:
    case T.axesmolecular:
    case T.axesunitcell:
      setAxesMode(tok);
      return;
    case T.axesorientationrasmol:
      // public; no need to set here
      setAxesOrientationRasmol(value);
      return;
    case T.colorrasmol:
      setStringPropertyTok("defaultcolorscheme", T.defaultcolorscheme,
          value ? "rasmol" : "jmol");
      return;
    case T.debugscript:
      setDebugScript(value);
      return;
    case T.perspectivedepth:
      setPerspectiveDepth(value);
      return;
    case T.autobond:
      // public - no need to set
      setAutoBond(value);
      return;
    case T.showaxes:
      setShowAxes(value);
      return;
    case T.showboundbox:
      setShowBbcage(value);
      return;
    case T.showhydrogens:
      setShowHydrogens(value);
      return;
    case T.showmeasurements:
      setShowMeasurements(value);
      return;
    case T.showunitcell:
      setShowUnitCell(value);
      return;
    case T.bondmodeor:
      doRepaint = false;
      g.bondModeOr = value;
      break;
    case T.zerobasedxyzrasmol:
      doRepaint = false;
      g.zeroBasedXyzRasmol = value;
      reset(true);
      break;
    case T.rangeselected:
      doRepaint = false;
      g.rangeSelected = value;
      break;
    case T.measureallmodels:
      doRepaint = false;
      g.measureAllModels = value;
      break;
    case T.statusreporting:
      doRepaint = false;
      // not part of the state
      sm.allowStatusReporting = value;
      break;
    case T.chaincasesensitive:
      doRepaint = false;
      g.chainCaseSensitive = value;
      break;
    case T.hidenameinpopup:
      doRepaint = false;
      g.hideNameInPopup = value;
      break;
    case T.disablepopupmenu:
      doRepaint = false;
      g.disablePopupMenu = value;
      break;
    case T.forceautobond:
      doRepaint = false;
      g.forceAutoBond = value;
      break;
    default:
      if (!g.htBooleanParameterFlags.containsKey(key.toLowerCase())) {
        g.setUserVariable(key, SV.getBoolean(value));
        return;
      }
    }
    g.setB(key, value);
    if (doRepaint)
      setTainted(true);
  }

  /*
   * public void setFileCacheDirectory(String fileOrDir) { if (fileOrDir ==
   * null) fileOrDir = ""; global._fileCache = fileOrDir; }
   * 
   * String getFileCacheDirectory() { if (!global._fileCaching) return null;
   * return global._fileCache; }
   */

  private void setModelKitMode(boolean value) {
    if (acm == null || !allowScripting)
      return;
    if (value || g.modelKitMode) {
      setPickingMode(null, value ? ActionManager.PICKING_ASSIGN_BOND
          : ActionManager.PICKING_IDENTIFY);
      setPickingMode(null, value ? ActionManager.PICKING_ASSIGN_ATOM
          : ActionManager.PICKING_IDENTIFY);
    }
    boolean isChange = (g.modelKitMode != value);
    g.modelKitMode = value;
    g.setB("modelkitmode", value); // in case there is a callback before this completes
    highlight(null);
    if (value) {
      setNavigationMode(false);
      selectAll();
      // setShapeProperty(JmolConstants.SHAPE_LABELS, "color", "RED");
      setStringProperty("picking", "assignAtom_C");
      setStringProperty("picking", "assignBond_p");
      if (!isApplet)
        popupMenu(10, 0, 'm'); // was 0?
      if (isChange)
        sm.setStatusModelKit(1);
      g.modelKitMode = true;
      if (ms.ac == 0)
        zap(false, true, true);
      else if (am.cmi >= 0 && getModelUndeletedAtomsBitSet(am.cmi).isEmpty()) {
        Map<String, Object> htParams = new Hashtable<String, Object>();
        htParams.put("appendToModelIndex", Integer.valueOf(am.cmi));
        loadDefaultModelKitModel(htParams);
      }
    } else {
      acm.setPickingMode(ActionManager.PICKING_MK_RESET);
      setStringProperty("pickingStyle", "toggle");
      setBooleanProperty("bondPicking", false);
      if (isChange) {
        sm.setStatusModelKit(0);
      }
    }
  }

  public void setSmilesString(String s) {
    if (s == null)
      g.removeParam("_smilesString");
    else
      g.setO("_smilesString", s);
  }

  public void removeUserVariable(String key) {
    g.removeUserVariable(key);
    if (key.endsWith("callback"))
      sm.setCallbackFunction(key, null);
  }

  private void jmolTest() {
    /*
     * Vector v = new Vector(); Vector m = new Vector(); v.add(m);
     * m.add("MODEL     2");m.add(
     * "HETATM    1 H1   UNK     1       2.457   0.000   0.000  1.00  0.00           H  "
     * );m.add(
     * "HETATM    2 C1   UNK     1       1.385   0.000   0.000  1.00  0.00           C  "
     * );m.add(
     * "HETATM    3 C2   UNK     1      -1.385  -0.000   0.000  1.00  0.00           C  "
     * ); v.add(new String[] { "MODEL     2",
     * "HETATM    1 H1   UNK     1       2.457   0.000   0.000  1.00  0.00           H  "
     * ,
     * "HETATM    2 C1   UNK     1       1.385   0.000   0.000  1.00  0.00           C  "
     * ,
     * "HETATM    3 C2   UNK     1      -1.385  -0.000   0.000  1.00  0.00           C  "
     * , }); v.add(new String[] {"3","testing","C 0 0 0","O 0 1 0","N 0 0 1"} );
     * v.add("3\ntesting\nC 0 0 0\nO 0 1 0\nN 0 0 1\n"); loadInline(v, false);
     */
  }

  public void showParameter(String key, boolean ifNotSet, int nMax) {
    String sv = "" + g.getParameterEscaped(key, nMax);
    if (ifNotSet || sv.indexOf("<not defined>") < 0)
      showString(key + " = " + sv, false);
  }

  public void showString(String str, boolean isPrint) {
    if (!isJS && isScriptQueued() && (!isSilent || isPrint)
        && !"\0".equals(str)) {
      Logger.warn(str); // warn here because we still want to be be able to turn this off
    }
    scriptEcho(str);
  }

  public String getAllSettings(String prefix) {
    return getStateCreator().getAllSettings(prefix);
  }

  public String getBindingInfo(String qualifiers) {
    return (haveDisplay ? acm.getBindingInfo(qualifiers) : "");
  }

  // ////// flags and settings ////////

  public int getIsosurfacePropertySmoothing(boolean asPower) {
    // Eval
    return (asPower ? g.isosurfacePropertySmoothingPower
        : g.isosurfacePropertySmoothing ? 1 : 0);
  }

  public void setNavigationDepthPercent(float percent) {
    tm.setNavigationDepthPercent(percent);
    refresh(REFRESH_REPAINT, "set navigationDepth");
  }

  public boolean getShowNavigationPoint() {
    if (!g.navigationMode/* || !tm.canNavigate()*/)
      return false;
    return (tm.isNavigating() && !g.hideNavigationPoint
        || g.showNavigationPointAlways || getInMotion(true));
  }

  @Override
  public void setPerspectiveDepth(boolean perspectiveDepth) {
    // setBooleanProperty
    // stateManager.setCrystallographicDefaults
    // app preferences dialog
    tm.setPerspectiveDepth(perspectiveDepth);
  }

  @Override
  public void setAxesOrientationRasmol(boolean TF) {
    // app PreferencesDialog
    // stateManager
    // setBooleanproperty
    /*
     * *************************************************************** RasMol
     * has the +Y axis pointing down And rotations about the y axis are
     * left-handed setting this flag makes Jmol mimic this behavior
     * 
     * All versions of Jmol prior to 11.5.51 incompletely implement this flag.
     * All versions of Jmol between 11.5.51 and 12.2.4 incorrectly implement this flag.
     * Really all it is just a flag to tell Eval to flip the sign of the Z
     * rotation when specified specifically as "rotate/spin Z 30".
     * 
     * In principal, we could display the axis opposite as well, but that is
     * only aesthetic and not at all justified if the axis is molecular.
     * **************************************************************
     */
    g.setB("axesOrientationRasmol", TF);
    g.axesOrientationRasmol = TF;
    reset(true);
  }

  private void setAxesScale(int tok, float val) {
    val = checkFloatRange(val, -100, 100);
    if (tok == T.axesoffset)
      g.axesOffset = val;
    else
      g.axesScale = val;
    axesAreTainted = true;
  }

  void setAxesMode(int mode) {
    g.axesMode = mode;
    axesAreTainted = true;
    switch (mode) {
    case T.axesunitcell:
      // stateManager
      // setBooleanproperty
      g.removeParam("axesmolecular");
      g.removeParam("axeswindow");
      g.setB("axesUnitcell", true);
      mode = 2;
      break;
    case T.axesmolecular:
      g.removeParam("axesunitcell");
      g.removeParam("axeswindow");
      g.setB("axesMolecular", true);
      mode = 1;
      break;
    case T.axeswindow:
      g.removeParam("axesunitcell");
      g.removeParam("axesmolecular");
      g.setB("axesWindow", true);
      mode = 0;
    }
    g.setI("axesMode", mode);
  }

  private boolean selectionHalosEnabled = false;

  public boolean getSelectionHalosEnabled() {
    return selectionHalosEnabled;
  }

  public void setSelectionHalosEnabled(boolean TF) {
    if (selectionHalosEnabled == TF)
      return;
    g.setB("selectionHalos", TF);
    shm.loadShape(JC.SHAPE_HALOS);
    selectionHalosEnabled = TF;
  }

  public boolean getShowSelectedOnce() {
    boolean flag = showSelected;
    showSelected = false;
    return flag;
  }

  private void setStrandCount(int type, int value) {
    value = checkIntRange(value, 0, 20);
    switch (type) {
    case JC.SHAPE_STRANDS:
      g.strandCountForStrands = value;
      break;
    case JC.SHAPE_MESHRIBBON:
      g.strandCountForMeshRibbon = value;
      break;
    default:
      g.strandCountForStrands = value;
      g.strandCountForMeshRibbon = value;
      break;
    }
    g.setI("strandCount", value);
    g.setI("strandCountForStrands", g.strandCountForStrands);
    g.setI("strandCountForMeshRibbon", g.strandCountForMeshRibbon);
  }

  public int getStrandCount(int type) {
    return (type == JC.SHAPE_STRANDS ? g.strandCountForStrands
        : g.strandCountForMeshRibbon);
  }

  public void setNavigationMode(boolean TF) {
    g.navigationMode = TF;
    tm.setNavigationMode(TF);
  }

  @Override
  public void setAutoBond(boolean TF) {
    // setBooleanProperties
    g.setB("autobond", TF);
    g.autoBond = TF;
  }

  public int[] makeConnections(float minDistance, float maxDistance, int order,
                               int connectOperation, BS bsA, BS bsB, BS bsBonds,
                               boolean isBonds, boolean addGroup,
                               float energy) {
    // eval
    clearModelDependentObjects();
    // removed in 12.3.2 and 12.2.1; cannot remember why this was important
    // we aren't removing atoms, just bonds. So who cares in terms of measurements?
    // clearAllMeasurements(); // necessary for serialization (??)
    clearMinimization();
    return ms.makeConnections(minDistance, maxDistance, order, connectOperation,
        bsA, bsB, bsBonds, isBonds, addGroup, energy);
  }

  @Override
  public void rebond() {
    // PreferencesDialog
    rebondState(false);
  }

  public void rebondState(boolean isStateScript) {
    // Eval CONNECT
    clearModelDependentObjects();
    ms.deleteAllBonds();
    boolean isLegacy = isStateScript && g.legacyAutoBonding;
    ms.autoBondBs4(null, null, null, null, getMadBond(), isLegacy);
    addStateScript((isLegacy
        ? "set legacyAutoBonding TRUE;connect;set legacyAutoBonding FALSE;"
        : "connect;"), false, true);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to stateManager
  // ///////////////////////////////////////////////////////////////

  @Override
  public void setPercentVdwAtom(int value) {
    g.setI("percentVdwAtom", value);
    g.percentVdwAtom = value;
    rd.value = value / 100f;
    rd.factorType = EnumType.FACTOR;
    rd.vdwType = VDW.AUTO;
    shm.setShapeSizeBs(JC.SHAPE_BALLS, 0, rd, null);
  }

  @Override
  public short getMadBond() {
    return (short) (g.bondRadiusMilliAngstroms * 2);
  }

  @Override
  public void setShowHydrogens(boolean TF) {
    // PreferencesDialog
    // setBooleanProperty
    g.setB("showHydrogens", TF);
    g.showHydrogens = TF;
  }

  public void setShowBbcage(boolean value) {
    setObjectMad10(JC.SHAPE_BBCAGE, "boundbox", (short) (value ? -4 : 0));
    g.setB("showBoundBox", value);
  }

  public boolean getShowBbcage() {
    return getObjectMad10(StateManager.OBJ_BOUNDBOX) != 0;
  }

  public void setShowUnitCell(boolean value) {
    setObjectMad10(JC.SHAPE_UCCAGE, "unitcell", (short) (value ? -2 : 0));
    g.setB("showUnitCell", value);
  }

  public boolean getShowUnitCell() {
    return getObjectMad10(StateManager.OBJ_UNITCELL) != 0;
  }

  public void setShowAxes(boolean value) {
    setObjectMad10(JC.SHAPE_AXES, "axes", (short) (value ? -2 : 0));
    g.setB("showAxes", value);
  }

  public boolean getShowAxes() {
    return getObjectMad10(StateManager.OBJ_AXIS1) != 0;
  }

  public boolean frankOn = true;
  public boolean noFrankEcho = true; // set when Echo bottom right renders

  @Override
  public void setFrankOn(boolean TF) {
    if (isPreviewOnly)
      TF = false;
    frankOn = TF;
    setObjectMad10(JC.SHAPE_FRANK, "frank", (short) (TF ? 1 : 0));
  }

  public boolean getShowFrank() {
    if (isPreviewOnly || isApplet && creatingImage)
      return false;
    // Java remote signed applet only?
    return (isSignedApplet && !isSignedAppletLocal && !isJS || frankOn);
  }

  @Override
  public void setShowMeasurements(boolean TF) {
    // setbooleanProperty
    g.setB("showMeasurements", TF);
    g.showMeasurements = TF;
  }

  public void setUnits(String units, boolean isDistance) {
    // stateManager
    // Eval
    g.setUnits(units);
    if (isDistance) {
      g.setUnits(units);
      setShapeProperty(JC.SHAPE_MEASURES, "reformatDistances", null);
    } else {

    }
  }

  @Override
  public void setRasmolDefaults() {
    setDefaultsType("RasMol");
  }

  @Override
  public void setJmolDefaults() {
    setDefaultsType("Jmol");
  }

  private void setDefaultsType(String type) {
    if (type.equalsIgnoreCase("RasMol")) {
      stm.setRasMolDefaults();
      return;
    }
    if (type.equalsIgnoreCase("PyMOL")) {
      stm.setPyMOLDefaults();
      return;
    }
    stm.setJmolDefaults();
    setIntProperty("bondingVersion", Elements.RAD_COV_IONIC_OB1_100_1);
    shm.setShapeSizeBs(JC.SHAPE_BALLS, 0, rd, getAllAtoms());
  }

  private void setAntialias(int tok, boolean TF) {
    boolean isChanged = false;
    switch (tok) {
    case T.antialiasdisplay:
      isChanged = (g.antialiasDisplay != TF);
      g.antialiasDisplay = TF;
      break;
    case T.antialiastranslucent:
      isChanged = (g.antialiasTranslucent != TF);
      g.antialiasTranslucent = TF;
      break;
    case T.antialiasimages:
      g.antialiasImages = TF;
      return;
    }
    if (isChanged) {
      resizeImage(0, 0, false, false, true); // for antialiasdisplay
      refresh(REFRESH_SYNC_MASK, "Viewer:setAntialias()");
    }
    //    resizeImage(0, 0, false, false, true);
  }

  // //////////////////////////////////////////////////////////////
  // temp manager
  // //////////////////////////////////////////////////////////////

  public P3[] allocTempPoints(int size) {
    // rockets cartoons renderer only
    return tempArray.allocTempPoints(size);
  }

  public void freeTempPoints(P3[] tempPoints) {
    // rockets, cartoons render only
    tempArray.freeTempPoints(tempPoints);
  }

  public P3i[] allocTempScreens(int size) {
    // mesh and mps
    return tempArray.allocTempScreens(size);
  }

  public void freeTempScreens(P3i[] tempScreens) {
    tempArray.freeTempScreens(tempScreens);
  }

  public STR[] allocTempEnum(int size) {
    // mps renderer
    return tempArray.allocTempEnum(size);
  }

  public void freeTempEnum(STR[] temp) {
    tempArray.freeTempEnum(temp);
  }

  // //////////////////////////////////////////////////////////////
  // font stuff
  // //////////////////////////////////////////////////////////////
  public Font getFont3D(String fontFace, String fontStyle, float fontSize) {
    return gdata.getFont3DFSS(fontFace, fontStyle, fontSize);
  }

  // //////////////////////////////////////////////////////////////
  // Access to atom properties for clients
  // //////////////////////////////////////////////////////////////

  public Quat[] getAtomGroupQuaternions(BS bsAtoms, int nMax) {
    return ms.getAtomGroupQuaternions(bsAtoms, nMax, getQuaternionFrame());
  }

  // //////////////////////////////////////////////////////////////
  // stereo support
  // //////////////////////////////////////////////////////////////

  public void setStereoMode(int[] twoColors, STER stereoMode, float degrees) {
    setFloatProperty("stereoDegrees", degrees);
    setBooleanProperty("greyscaleRendering", stereoMode.isBiColor());
    if (twoColors != null)
      tm.setStereoMode2(twoColors);
    else
      tm.setStereoMode(stereoMode);
  }

  // //////////////////////////////////////////////////////////////
  //
  // //////////////////////////////////////////////////////////////

  // /////////////// getProperty /////////////

  public boolean scriptEditorVisible;

  public JmolAppConsoleInterface appConsole;
  private JmolScriptEditorInterface scriptEditor;
  GenericMenuInterface jmolpopup;
  private ModelKit modelkit;
  private Map<String, Object> headlessImageParams;

  public String getChimeInfo(int tok) {
    return getPropertyManager().getChimeInfo(tok, bsA());
  }

  public String getModelFileInfo() {
    return getPropertyManager().getModelFileInfo(getVisibleFramesBitSet());
  }

  public String getModelFileInfoAll() {
    return getPropertyManager().getModelFileInfo(null);
  }

  public void showEditor(String[] file_text) {
    JmolScriptEditorInterface scriptEditor = (JmolScriptEditorInterface) getProperty(
        "DATA_API", "getScriptEditor", Boolean.TRUE);
    if (scriptEditor == null)
      return;
    scriptEditor.show(file_text);
  }

  JmolPropertyManager pm;

  private JmolPropertyManager getPropertyManager() {
    if (pm == null)
      (pm = (JmolPropertyManager) Interface
          .getInterface("org.jmol.viewer.PropertyManager", this, "prop"))
              .setViewer(this);
    return pm;
  }

  // ////////////////////////////////////////////////

  boolean isTainted = true;

  public void setTainted(boolean TF) {
    isTainted = axesAreTainted = (TF && (refreshing || creatingImage));
  }

  Map<String, Object> checkObjectClicked(int x, int y, int modifiers) {
    return shm.checkObjectClicked(x, y, modifiers, getVisibleFramesBitSet(),
        g.drawPicking);
  }

  public boolean checkObjectHovered(int x, int y) {
    return (x >= 0 && shm != null && shm.checkObjectHovered(x, y,
        getVisibleFramesBitSet(), getBondsPickable()));
  }

  boolean checkObjectDragged(int prevX, int prevY, int x, int y, int action) {
    int iShape = 0;
    switch (getPickingMode()) {
    case ActionManager.PICKING_LABEL:
      iShape = JC.SHAPE_LABELS;
      break;
    case ActionManager.PICKING_DRAW:
      iShape = JC.SHAPE_DRAW;
      break;
    }
    if (shm.checkObjectDragged(prevX, prevY, x, y, action,
        getVisibleFramesBitSet(), iShape)) {
      refresh(REFRESH_REPAINT, "checkObjectDragged");
      if (iShape == JC.SHAPE_DRAW)
        scriptEcho((String) getShapeProperty(JC.SHAPE_DRAW, "command"));
      return true;
    }
    return false;
  }

  public boolean rotateAxisAngleAtCenter(JmolScriptEvaluator eval, P3 rotCenter,
                                         V3 rotAxis, float degreesPerSecond,
                                         float endDegrees, boolean isSpin,
                                         BS bsSelected) {
    // Eval: rotate FIXED
    boolean isOK = tm.rotateAxisAngleAtCenter(eval, rotCenter, rotAxis,
        degreesPerSecond, endDegrees, isSpin, bsSelected);
    if (isOK)
      setSync();
    return isOK;
  }

  public boolean rotateAboutPointsInternal(JmolScriptEvaluator eval, P3 point1,
                                           P3 point2, float degreesPerSecond,
                                           float endDegrees, boolean isSpin,
                                           BS bsSelected, V3 translation,
                                           Lst<P3> finalPoints,
                                           float[] dihedralList, M4 m4) {
    // Eval: rotate INTERNAL

    if (eval == null)
      eval = this.eval;

    if (headless) {
      if (isSpin && endDegrees == Float.MAX_VALUE)
        return false;
      isSpin = false;
    }

    boolean isOK = tm.rotateAboutPointsInternal(eval, point1, point2,
        degreesPerSecond, endDegrees, false, isSpin, bsSelected, false,
        translation, finalPoints, dihedralList, m4);
    if (isOK)
      setSync();
    return isOK;
  }

  public void startSpinningAxis(T3 pt1, T3 pt2, boolean isClockwise) {
    // Draw.checkObjectClicked ** could be difficult
    // from draw object click
    if (tm.spinOn || tm.navOn) {
      tm.setSpinOff();
      tm.setNavOn(false);
      return;
    }
    tm.rotateAboutPointsInternal(null, pt1, pt2, g.pickingSpinRate,
        Float.MAX_VALUE, isClockwise, true, null, false, null, null, null,
        null);
  }

  public V3 getModelDipole() {
    return ms.getModelDipole(am.cmi);
  }

  public V3 calculateMolecularDipole(BS bsAtoms) throws Exception {
    try {
      return ms.calculateMolecularDipole(am.cmi, bsAtoms);
    } catch (JmolAsyncException e) {
      if (eval != null)
        eval.loadFileResourceAsync(e.getFileName());
      return null;
    }
  }

  public void setDefaultLattice(P3 p) {
    // Eval -- handled separately
    if (!Float.isNaN(p.x + p.y + p.z))
      g.ptDefaultLattice.setT(p);
    g.setO("defaultLattice", Escape.eP(p));
  }

  public P3 getDefaultLattice() {
    return g.ptDefaultLattice;
  }

  /**
   * 
   * V3000, SDF, JSON, CD, XYZ, XYZVIB, XYZRN, CML, PDB, PQR
   * 
   * @param atomExpression
   * @param doTransform
   * @param isModelKit
   * @param type
   * @return full file data
   * 
   */
  public String getModelExtract(Object atomExpression, boolean doTransform,
                                boolean isModelKit, String type) {
    return getPropertyManager().getModelExtract(getAtomBitSet(atomExpression),
        doTransform, isModelKit, type, false);
  }

  @Override
  public String getData(String atomExpression, String type) {
    // from GaussianDialog
    return getModelFileData(atomExpression, type, true);
  }

  /**
   * @param atomExpression
   *        -- will be wrapped in { } and evaluated
   * @param type
   *        -- lower case means "atom data only; UPPERCASE returns full file
   *        data
   * @param allTrajectories
   * @return full or atom-only data formatted as specified
   */
  public String getModelFileData(String atomExpression, String type,
                                 boolean allTrajectories) {
    return getPropertyManager().getAtomData(atomExpression, type,
        allTrajectories);
  }

  public String getModelCml(BS bs, int nAtomsMax, boolean addBonds,
                            boolean doTransform) {
    return getPropertyManager().getModelCml(bs, nAtomsMax, addBonds,
        doTransform, false);
  }

  public String getPdbAtomData(BS bs, OC out, boolean asPQR,
                               boolean doTransform) {
    return getPropertyManager().getPdbAtomData(bs == null ? bsA() : bs, out,
        asPQR, doTransform, false);
  }

  public boolean isJmolDataFrame() {
    return ms.isJmolDataFrameForModel(am.cmi);
  }

  public void setFrameTitle(int modelIndex, String title) {
    ms.setFrameTitle(BSUtil.newAndSetBit(modelIndex), title);
  }

  public void setFrameTitleObj(Object title) {
    shm.loadShape(JC.SHAPE_ECHO);
    ms.setFrameTitle(getVisibleFramesBitSet(), title);
  }

  public String getFrameTitle() {
    return ms.getFrameTitle(am.cmi);
  }

  public void setAtomProperty(BS bs, int tok, int iValue, float fValue,
                              String sValue, float[] values, String[] list) {
    if (tok == T.vanderwaals)
      shm.deleteVdwDependentShapes(bs);
    clearMinimization();
    ms.setAtomProperty(bs, tok, iValue, fValue, sValue, values, list);
    switch (tok) {
    case T.atomx:
    case T.atomy:
    case T.atomz:
    case T.fracx:
    case T.fracy:
    case T.fracz:
    case T.unitx:
    case T.unity:
    case T.unitz:
    case T.element:
      refreshMeasures(true);
    }
  }

  public void checkCoordinatesChanged() {
    // note -- use of save/restore coordinates cannot 
    // track connected objects
    ms.recalculatePositionDependentQuantities(null, null);
    refreshMeasures(true);
  }

  public void setAtomCoords(BS bs, int tokType, Object xyzValues) {
    if (bs.isEmpty())
      return;
    Atom atom = ms.at[bs.nextSetBit(0)];
    int n = bs.cardinality();
    sm.setStatusStructureModified(atom.i, atom.mi, 3, "setAtomCoords", n, bs);
    ms.setAtomCoords(bs, tokType, xyzValues);
    checkMinimization();
    sm.setStatusAtomMoved(bs);
    sm.setStatusStructureModified(atom.i, atom.mi, -3, "OK", n, bs);
  }

  public void setAtomCoordsRelative(T3 offset, BS bs) {
    // Eval
    if (bs == null)
      bs = bsA();
    if (bs.isEmpty())
      return;
    boolean doNotify = (offset.lengthSquared() != 0);
    Atom atom = ms.at[bs.nextSetBit(0)];
    int n = bs.cardinality();
    if (doNotify) {
      sm.setStatusStructureModified(atom.i, atom.mi, 3, "setAtomCoords", n, bs);
    }
    ms.setAtomCoordsRelative(offset, bs);
    checkMinimization();
    if (doNotify) {
      sm.setStatusAtomMoved(bs);
      sm.setStatusStructureModified(atom.i, atom.mi, -3, "OK", n, bs);
    }
  }

  public void invertAtomCoord(P3 pt, P4 plane, BS bs, int ringAtomIndex, boolean isClick) {
    // Eval
    if (ringAtomIndex >= 0) {
      // invert ring [r50 here just sets the max ring size to 50
      bs = getAtomBitSet(
          "connected(atomIndex=" + ringAtomIndex + ") and !within(SMARTS,'[r50,R]')");
      int nb = bs.cardinality();
      switch (nb) {
      case 0:
      case 1:
        // not enough non-ring atoms
        return;
      case 2:
        break;
      case 3:
      case 4:
        // three or four are not in a ring. So let's find the shortest two
        // branches and invert them.
        int[] lengths = new int[nb];
        int[] points = new int[nb];
        int ni = 0;
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1), ni++) {
          lengths[ni] = getBranchBitSet(i, ringAtomIndex, true).cardinality();
          points[ni] = i;
        }
        for (int j = 0; j < nb - 2; j++) {
          int max = Integer.MIN_VALUE;
          int imax = 0;
          for (int i = 0; i < nb; i++)
            if (lengths[i] >= max && bs.get(points[i])) {
              imax = points[i];
              max = lengths[i];
            }
          bs.clear(imax);
        }
      }
      if (isClick && bs.cardinality() > 0)
        undoMoveActionClear(ringAtomIndex, AtomCollection.TAINT_COORD, true);
    }    
    int n = bs.cardinality();
    if (n == 0)
      return;
    Atom atom = ms.at[bs.nextSetBit(0)];
    sm.setStatusStructureModified(atom.i, atom.mi, 3, "invertAtomCoords", n, bs);
    ms.invertSelected(pt, plane, ringAtomIndex, bs);
    checkMinimization();
    sm.setStatusAtomMoved(bs);
    sm.setStatusStructureModified(atom.i, atom.mi, -3, "OK", n, bs);
    if (isClick)
      setStatusAtomPicked(ringAtomIndex, "inverted: " + Escape.eBS(bs), null,
          false);
  }

  public void invertSelected(P3 pt, P4 plane, int iAtom, BS bsAtoms) {
    // Eval
    if (bsAtoms == null)
      bsAtoms = bsA();
    if (bsAtoms.cardinality() == 0)
      return;
    ms.invertSelected(pt, plane, iAtom, bsAtoms);
    checkMinimization();
    sm.setStatusAtomMoved(bsAtoms);
  }

  public void moveAtoms(M4 m4, M3 mNew, M3 rotation, V3 translation, P3 center,
                        boolean isInternal, BS bsAtoms,
                        boolean translationOnly) {
    // from TransformManager exclusively
    if (bsAtoms.isEmpty())
      return;
    ms.moveAtoms(m4, mNew, rotation, translation, bsAtoms, center, isInternal,
        translationOnly);
    checkMinimization();
    sm.setStatusAtomMoved(bsAtoms);
  }

  private boolean movingSelected;
  private boolean showSelected;

  public void moveSelected(int deltaX, int deltaY, int deltaZ, int x, int y,
                           BS bsSelected, boolean isTranslation,
                           boolean asAtoms, int modifiers) {
    // called by actionManager
    // cannot synchronize this -- it's from the mouse and the event queue
    if (deltaZ == 0)
      return;
    if (x == Integer.MIN_VALUE)
      setModelKitRotateBondIndex(Integer.MIN_VALUE);
    if (isJmolDataFrame())
      return;
    if (deltaX == Integer.MIN_VALUE) {
      showSelected = true;
      movableBitSet = setMovableBitSet(null, !asAtoms);
      shm.loadShape(JC.SHAPE_HALOS);
      refresh(REFRESH_REPAINT_NO_MOTION_ONLY, "moveSelected");
      return;
    }
    if (deltaX == Integer.MAX_VALUE) {
      if (!showSelected)
        return;
      showSelected = false;
      movableBitSet = null;
      refresh(REFRESH_REPAINT_NO_MOTION_ONLY, "moveSelected");
      return;
    }
    if (movingSelected)
      return;
    movingSelected = true;
    stopMinimization();
    // note this does not sync with applets
    if (x != Integer.MIN_VALUE && modelkit != null
        && modelkit.getProperty("rotateBondIndex") != null) {
      modelkit.actionRotateBond(deltaX, deltaY, x, y,
          (modifiers & Event.VK_SHIFT) != 0);
    } else {
      bsSelected = setMovableBitSet(bsSelected, !asAtoms);
      if (!bsSelected.isEmpty()) {
        if (isTranslation) {
          P3 ptCenter = ms.getAtomSetCenter(bsSelected);
          tm.finalizeTransformParameters();
          float f = (g.antialiasDisplay ? 2 : 1);
          P3i ptScreen = tm.transformPt(ptCenter);
          P3 ptScreenNew;
          if (deltaZ != Integer.MIN_VALUE)
            ptScreenNew = P3.new3(ptScreen.x, ptScreen.y,
                ptScreen.z + deltaZ + 0.5f);
          else
            ptScreenNew = P3.new3(ptScreen.x + deltaX * f + 0.5f,
                ptScreen.y + deltaY * f + 0.5f, ptScreen.z);
          P3 ptNew = new P3();
          tm.unTransformPoint(ptScreenNew, ptNew);
          // script("draw ID 'pt" + Math.random() + "' " + Escape.escape(ptNew));
          ptNew.sub(ptCenter);
          setAtomCoordsRelative(ptNew, bsSelected);
        } else {
          tm.rotateXYBy(deltaX, deltaY, bsSelected);
        }
      }
    }
    refresh(REFRESH_SYNC, ""); // should be syncing here
    movingSelected = false;
  }

  /**
   * from Sticks
   * 
   * @param index
   * @param closestAtomIndex
   * @param x
   * @param y
   */
  public void highlightBond(int index, int closestAtomIndex, int x, int y) {//, String msg) {
    if (!hoverEnabled)
      return;
    BS bs = null;
    if (index >= 0) {
      Bond b = ms.bo[index];
      int i = b.atom2.i;
//      if (!ms.isAtomInLastModel(i))
//        return;
      bs = BSUtil.newAndSetBit(i);
      bs.set(b.atom1.i);
    }
    highlight(bs);
    setModelkitProperty("bondIndex", Integer.valueOf(index));
    setModelkitProperty("screenXY", new int[] { x, y });
    String text = (String) setModelkitProperty("hoverLabel",
        Integer.valueOf(-2 - index));
    if (text != null)
      hoverOnPt(x, y, text, null, null);
    //    hoverOn(closestAtomIndex, false);
    refresh(REFRESH_SYNC_MASK, "highlightBond");
  }

  public int atomHighlighted = -1;

  public void highlight(BS bs) {
    atomHighlighted = (bs != null && bs.cardinality() == 1 ? bs.nextSetBit(0)
        : -1);

    if (bs == null) {
      setCursor(GenericPlatform.CURSOR_DEFAULT);
    } else {
      shm.loadShape(JC.SHAPE_HALOS);
      setCursor(GenericPlatform.CURSOR_HAND);
    }
    setModelkitProperty("highlight", bs);
    setShapeProperty(JC.SHAPE_HALOS, "highlight", bs);
  }

  public void refreshMeasures(boolean andStopMinimization) {
    setShapeProperty(JC.SHAPE_MEASURES, "refresh", null);
    if (andStopMinimization)
      stopMinimization();
  }

  /**
   * fills an array with data -- if nX < 0 and this would involve JavaScript,
   * then this reads a full set of Double[][] in one function call. Otherwise it
   * reads the values using individual function calls, which each return Double.
   * 
   * If the functionName begins with "file:" then data are read from a file
   * specified after the colon. The sign of nX is not relevant in that case. The
   * file may contain mixed numeric and non-numeric values; the non-numeric
   * values will be skipped by Parser.parseFloatArray
   * 
   * @param functionName
   * @param nX
   * @param nY
   * @return nX by nY array of floating values
   */
  public float[][] functionXY(String functionName, int nX, int nY) {
    String data = null;
    if (functionName.indexOf("file:") == 0)
      data = getFileAsString3(functionName.substring(5), false, null);
    else if (functionName.indexOf("data2d_") != 0)
      return sm.functionXY(functionName, nX, nY);
    nX = Math.abs(nX);
    nY = Math.abs(nY);
    float[][] fdata;
    if (data == null) {
      fdata = (float[][]) getDataObj(functionName, null,
          JmolDataManager.DATA_TYPE_AFF);
      if (fdata != null)
        return fdata;
      data = "";
    }
    fdata = new float[nX][nY];
    float[] f = new float[nX * nY];
    Parser.parseStringInfestedFloatArray(data, null, f);
    for (int i = 0, n = 0; i < nX; i++)
      for (int j = 0; j < nY; j++)
        fdata[i][j] = f[n++];
    return fdata;
  }

  public float[][][] functionXYZ(String functionName, int nX, int nY, int nZ) {
    String data = null;
    if (functionName.indexOf("file:") == 0)
      data = getFileAsString3(functionName.substring(5), false, null);
    else if (functionName.indexOf("data3d_") != 0)
      return sm.functionXYZ(functionName, nX, nY, nZ);
    nX = Math.abs(nX);
    nY = Math.abs(nY);
    nZ = Math.abs(nZ);
    float[][][] xyzdata;
    if (data == null) {
      xyzdata = (float[][][]) getDataObj(functionName, null,
          JmolDataManager.DATA_TYPE_AFF);
      if (xyzdata != null)
        return xyzdata;
      data = "";
    }
    xyzdata = new float[nX][nY][nZ];
    float[] f = new float[nX * nY * nZ];
    Parser.parseStringInfestedFloatArray(data, null, f);
    for (int i = 0, n = 0; i < nX; i++)
      for (int j = 0; j < nY; j++)
        for (int k = 0; k < nZ; k++)
          xyzdata[i][j][k] = f[n++];
    return xyzdata;
  }

  @Override
  public String extractMolData(String what) {
    if (what == null) {
      int i = am.cmi;
      if (i < 0 || ms.ac == 0)
        return null;
      what = getModelNumberDotted(i);
    }
    return getModelExtract(what, true, false, "V2000");
  }

  /**
   * 
   * @param type
   *        C13 or H1
   * @return null
   */
  public String getNMRPredict(String type) {
    type = type.toUpperCase();
    if (type.equals("H") || type.equals("1H") || type.equals(""))
      type = "H1";
    else if (type.equals("C") || type.equals("13C"))
      type = "C13";
    if (!type.equals("NONE")) {
      if (!type.equals("C13") && !type.equals("H1"))
        return "Type must be H1 or C13";
      String molFile = getModelExtract("selected", true, false, "V2000");
      int pt = molFile.indexOf("\n");
      if (pt < 0)
        return null;
      molFile = "Jmol " + version_date + molFile.substring(pt);
      if (isApplet) {
        //TODO -- can do this if connected
        showUrl(g.nmrUrlFormat + molFile);
        return "opening " + g.nmrUrlFormat;
      }
    }
    syncScript("true", "*", 0);
    syncScript(type + "Simulate:", ".", 0);
    return "sending request to JSpecView";
  }

  public void getHelp(String what) {
    if (g.helpPath.indexOf("?") < 0) {
      if (what.length() > 0 && what.indexOf("?") != 0)
        what = "?search=" + PT.rep(what, " ", "%20");
      what += (what.length() == 0 ? "?ver=" : "&ver=") + JC.majorVersion;
    } else {
      what = "&" + what;
    }
    showUrl(g.helpPath + what);
  }

  public String getChemicalInfo(String smiles, String info, BS bsAtoms) {
    info = info.toLowerCase();
    char type = '/';
    switch (";inchi;inchikey;stdinchi;stdinchikey;name;image;drawing;names;"
        .indexOf(";" + info + ";")) {
    //       0     6        15       24          36   41    47      55
    //       0         1         2         3         4         5
    //       0123456789012345678901234567890123456789012345678901234567890
    case 0: // inchi
      type = 'I';
      break;
    case 6: // inchikey
      type = 'K';
      break;
    case 15: // stdinchi
      type = 'T';
      break;
    case 24: // stdinchikey
      type = 'S';
      break;
    case 36: // name
      type = 'M';
      break;
    case 41: // image
    case 47: // drawing
      type = '2';
      break;
    case 55: // names
      type = 'N';
      break;
    }
    String s = (String) setLoadFormat("_" + smiles, type, false);
    if (type == '2') {
      fm.loadImage(s, "\1" + smiles, false);
      return s;
    }
    if (type == '/') {
      if (PT.isOneOf(info, JC.CACTUS_FILE_TYPES))
        s += "file?format=" + info;
      else
        s += PT.rep(info, " ", "%20");
    }
    s = getFileAsString4(s, -1, false, false, false, "file");
    if (type == 'M' && s.indexOf("\n") > 0)
      s = s.substring(0, s.indexOf("\n"));
    else if (info.equals("jme"))
      s = getPropertyManager().fixJMEFormalCharges(bsAtoms, s);
    return s;
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to stateManager
  // ///////////////////////////////////////////////////////////////

  /*
   * Moved from the consoles to vwr, since this could be of general interest,
   * it's more a property of Eval/Viewer, and the consoles are really just a
   * mechanism for getting user input and sending results, not saving a history
   * of it all. Ultimately I hope to integrate the mouse picking and possibly
   * periodic updates of position into this history to get a full history. We'll
   * see! BH 9/2006
   */

  /**
   * Adds one or more commands to the command history
   * 
   * @param command
   *        the command to add
   */
  public void addCommand(String command) {
    if (autoExit || !haveDisplay || !getPreserveState())
      return;
    commandHistory.addCommand(PT.replaceAllCharacters(command, "\r\n\t", " "));
  }

  public void pushState() {
    if (autoExit || !haveDisplay || !getPreserveState())
      return;
    commandHistory.pushState(getStateInfo());
  }

  public void popState() {
    if (autoExit || !haveDisplay || !getPreserveState())
      return;
    String state = commandHistory.popState();
    if (state != null)
      evalStringQuiet(state);
  }

  /**
   * Removes one command from the command history
   * 
   * @return command removed
   */
  public String removeCommand() {
    return commandHistory.removeCommand();
  }

  /**
   * Options include: ; all n == Integer.MAX_VALUE ; n prev n >= 1 ; next n ==
   * -1 ; set max to -2 - n n <= -3 ; just clear n == -2 ; clear and turn off;
   * return "" n == 0 ; clear and turn on; return "" n == Integer.MIN_VALUE;
   * 
   * @param howFarBack
   *        number of lines (-1 for next line)
   * @return one or more lines of command history
   */
  @Override
  public String getSetHistory(int howFarBack) {
    return commandHistory.getSetHistory(howFarBack);
  }

  public String historyFind(String cmd, int dir) {
    return commandHistory.find(cmd, dir);
  }

  public void setHistory(String fileName) {
    commandHistory.getSetHistory(Integer.MIN_VALUE);
    commandHistory
        .addCommand(getFileAsString4(fileName, -1, false, false, true, null));
  }

  // ///////////////////////////////////////////////////////////////
  // image and file export
  // ///////////////////////////////////////////////////////////////

  public OC getOutputChannel(String localName, String[] fullPath) {
    return getOutputManager().getOutputChannel(localName, fullPath);
  }

  @Override
  public String writeTextFile(String fileName, String data) {
    Map<String, Object> params = new Hashtable<String, Object>();
    params.put("fileName", fileName);
    params.put("type", "txt");
    params.put("text", data);
    return outputToFile(params);
  }

  /**
   * 
   * @param text
   *        null here clips image; String pastes text
   * 
   * @return "OK image to clipboard: [width] * [height] or "OK text to
   *         clipboard: [length]
   */
  @Override
  public String clipImageOrPasteText(String text) {
    if (!haveAccess(ACCESS.ALL))
      return "no";
    return getOutputManager().clipImageOrPasteText(text);
  }

  @Override
  public String getClipboardText() {
    if (!haveAccess(ACCESS.ALL))
      return "no";
    try {
      return getOutputManager().getClipboardText();
    } catch (Error er) {
      // unsigned applet will not have this interface
      return GT.$("clipboard is not accessible -- use signed applet");
    }
  }

  public boolean creatingImage;

  /**
   * 
   * from eval write command only includes option to write set of files
   * 
   * @param params
   * @return message starting with "OK" or an error message
   */
  public String processWriteOrCapture(Map<String, Object> params) {
    return getOutputManager().processWriteOrCapture(params);
  }

  public Object createZip(String fileName, String type,
                          Map<String, Object> params) {
    String state = getStateInfo();
    Object data = params.get("data");
    if (fileName != null)
      params.put("fileName", fileName);
    params.put("type", type);
    params.put("text", state);
    if (data instanceof String[])
      params.put("scripts", data);
    else if (data instanceof Lst)
      params.put("imageData", data);
    return getOutputManager().outputToFile(params);
  }

  @Override
  public String outputToFile(Map<String, Object> params) {
    return getOutputManager().outputToFile(params);
  }

  private void setSyncTarget(int mode, boolean TF) {
    switch (mode) {
    case 0:
      sm.syncingMouse = TF;
      break;
    case 1:
      sm.syncingScripts = TF;
      break;
    case 2:
      sm.syncSend(TF ? SYNC_GRAPHICS_MESSAGE : SYNC_NO_GRAPHICS_MESSAGE, "*",
          0);
      if (Float.isNaN(tm.stereoDegrees))
        setFloatProperty("stereoDegrees",
            TransformManager.DEFAULT_STEREO_DEGREES);
      if (TF) {
        setBooleanProperty("_syncMouse", false);
        setBooleanProperty("_syncScript", false);
      }
      return;
    }
    // if turning both off, sync the orientation now
    if (!sm.syncingScripts && !sm.syncingMouse)
      setSync();
  }

  public final static String SYNC_GRAPHICS_MESSAGE = "GET_GRAPHICS";
  public final static String SYNC_NO_GRAPHICS_MESSAGE = "SET_GRAPHICS_OFF";

  @Override
  public void syncScript(String script, String applet, int port) {
    sm.syncScript(script, applet, port);
  }

  @Override
  public int getModelIndexFromId(String id) {
    // from JSpecView peak pick and model "ID"
    return ms.getModelIndexFromId(id);
  }

  public void setSyncDriver(int mode) {
    sm.setSyncDriver(mode);
  }

  public void setProteinType(STR type, BS bs) {
    ms.setProteinType(bs == null ? bsA() : bs, type);
  }

  public int getVanderwaalsMar(int i) {
    return (defaultVdw == VDW.USER ? userVdwMars[i]
        : Elements.getVanderwaalsMar(i, defaultVdw));
  }

  public int getVanderwaalsMarType(int atomicAndIsotopeNumber, VDW type) {
    if (type == null)
      type = defaultVdw;
    else
      switch (type) {
      case AUTO:
      case AUTO_BABEL:
      case AUTO_JMOL:
      case AUTO_RASMOL:
        if (defaultVdw != VDW.AUTO)
          type = defaultVdw;
        break;
      default:
        break;
      }
    if (type == VDW.USER && bsUserVdws == null)
      type = VDW.JMOL;
    return (type == VDW.USER ? userVdwMars[atomicAndIsotopeNumber & 127]
        : Elements.getVanderwaalsMar(atomicAndIsotopeNumber, type));
  }

  void setVdwStr(String name) {
    VDW type = VDW.getVdwType(name);
    if (type == null)
      type = VDW.AUTO;
    // only allowed types here are VDW_JMOL, VDW_BABEL, VDW_RASMOL, VDW_USER, VDW_AUTO
    switch (type) {
    case JMOL:
    case BABEL:
    case RASMOL:
    case AUTO:
    case USER:
      break;
    default:
      type = VDW.JMOL;
    }
    if (type != defaultVdw && type == VDW.USER && bsUserVdws == null)
      setUserVdw(defaultVdw);
    defaultVdw = type;
    g.setO("defaultVDW", type.getVdwLabel());
  }

  BS bsUserVdws;
  float[] userVdws;
  int[] userVdwMars;

  void setUserVdw(VDW mode) {
    userVdwMars = new int[Elements.elementNumberMax];
    userVdws = new float[Elements.elementNumberMax];
    bsUserVdws = new BS();
    if (mode == VDW.USER)
      mode = VDW.JMOL;
    for (int i = 1; i < Elements.elementNumberMax; i++) {
      userVdwMars[i] = Elements.getVanderwaalsMar(i, mode);
      userVdws[i] = userVdwMars[i] / 1000f;
    }
  }

  public String getDefaultVdwNameOrData(int mode, VDW type, BS bs) {
    // called by getDataState and via Viewer: Eval.calculate,
    // Eval.show, StateManager.getLoadState, Viewer.setDefaultVdw
    switch (mode) {
    case Integer.MIN_VALUE:
      // iMode Integer.MIN_VALUE -- just the name
      return defaultVdw.getVdwLabel();
    case Integer.MAX_VALUE:
      // iMode = Integer.MAX_VALUE -- user, only selected
      if ((bs = bsUserVdws) == null)
        return "";
      type = VDW.USER;
      break;
    }
    if (type == null || type == VDW.AUTO)
      type = defaultVdw;
    if (type == VDW.USER && bsUserVdws == null)
      setUserVdw(defaultVdw);

    return getDataManager().getDefaultVdwNameOrData(type, bs);
  }

  public int deleteAtoms(BS bsAtoms, boolean fullModels) {
    int atomIndex = (bsAtoms == null ? -1 : bsAtoms.nextSetBit(0));
    if (atomIndex < 0)
      return 0;
    clearModelDependentObjects();
    Atom a = ms.at[atomIndex];
    if (a == null)
      return 0;
    int mi = a.mi;
    if (fullModels) {
      return deleteModels(mi, bsAtoms);
    }
    sm.setStatusStructureModified(atomIndex, a.mi, 4,
        "deleting atoms " + bsAtoms, bsAtoms.cardinality(), bsAtoms);
    ms.deleteAtoms(bsAtoms);
    int n = slm.deleteAtoms(bsAtoms);
    setTainted(true);
    sm.setStatusStructureModified(atomIndex, mi, -4, "OK", n, bsAtoms);
    return n;
  }

  /**
   * called by ZAP {atomExpression} when atoms are present or the command is
   * specific for a model, such as ZAP 2.1
   * 
   * @param modelIndex
   * @param bsAtoms
   * @return number of atoms deleted
   */
  public int deleteModels(int modelIndex, BS bsAtoms) {
    BS bsModels = (bsAtoms == null ? BSUtil.newAndSetBit(modelIndex)
        : ms.getModelBS(bsAtoms, false));
    clearModelDependentObjects();
    // fileManager.addLoadScript("zap " + Escape.escape(bs));
    bsAtoms = getModelUndeletedAtomsBitSetBs(bsModels);
    int n = bsAtoms.cardinality();
    int currentModel = am.cmi;
    setCurrentModelIndexClear(0, false);
    am.setAnimationOn(false);
    BS bsD0 = BSUtil.copy(slm.bsDeleted);
    BS bsDeleted = ms.deleteModels(bsModels);
    if (bsDeleted == null) {
      setCurrentModelIndexClear(currentModel, false);
      return 0;
    }
    sm.setStatusStructureModified(-1, modelIndex, 5,
        "deleting model " + getModelNumberDotted(modelIndex), n, bsAtoms);
    slm.processDeletedModelAtoms(bsDeleted);
    if (eval != null)
      eval.deleteAtomsInVariables(bsDeleted);
    setAnimationRange(0, 0);
    clearRepaintManager(-1);
    am.clear();
    am.initializePointers(1);
    setCurrentModelIndexClear(ms.mc > 1 ? -1 : 0, ms.mc > 1);
    hoverAtomIndex = -1;
    setFileLoadStatus(FIL.DELETED, null, null, null, null, null);
    refreshMeasures(true);
    if (bsD0 != null)
      bsDeleted.andNot(bsD0);
    n = BSUtil.cardinalityOf(bsDeleted);
    sm.setStatusStructureModified(-1, modelIndex, -5, "OK", n, bsDeleted);
    return n;
  }

  public void deleteBonds(BS bsDeleted) {
    int modelIndex = ms.bo[bsDeleted.nextSetBit(0)].atom1.mi;
    int n = bsDeleted.cardinality();
    if (n == 0)
      return;
    sm.setStatusStructureModified(-1, modelIndex, 2, "delete bonds " + Escape.eBond(bsDeleted), bsDeleted.cardinality(), bsDeleted);
    ms.deleteBonds(bsDeleted, false);
    sm.setStatusStructureModified(-1, modelIndex, -2, "OK", bsDeleted.cardinality(), bsDeleted);
  }

  public void deleteModelAtoms(int modelIndex, int firstAtomIndex, int nAtoms,
                               BS bsModelAtoms) {
    // called from ModelCollection.deleteModel
    int n = bsModelAtoms.cardinality();
    if (n == 0)
      return; 
    sm.setStatusStructureModified(-1, modelIndex, 1,
        "delete atoms " + Escape.eBS(bsModelAtoms), n, bsModelAtoms);
    BSUtil.deleteBits(tm.bsFrameOffsets, bsModelAtoms);
    getDataManager().deleteModelAtoms(firstAtomIndex, nAtoms, bsModelAtoms);
    sm.setStatusStructureModified(-1, modelIndex, -1, "OK", n, bsModelAtoms);
  }

  public char getQuaternionFrame() {
    return g.quaternionFrame.charAt(g.quaternionFrame.length() == 2 ? 1 : 0);
  }

  /**
   * 
   * NOTE: This method is called from within a j2sNative block in
   * awtjs2d.Platform.java as well as from FileManager.loadImage
   * 
   * @param image
   *        could be a byte array
   * @param nameOrError
   * @param echoName
   *        if this is an echo rather than the background
   * @param sco
   *        delivered in JavaScript from Platform.java
   * @return false
   */
  public boolean loadImageData(Object image, String nameOrError,
                               String echoName, Object sco) {
    ScriptContext sc = (ScriptContext) sco;
    if (image == null && nameOrError != null)
      scriptEcho(nameOrError);
    if (echoName == null) {
      setBackgroundImage((image == null ? null : nameOrError), image);
    } else if (echoName.startsWith("\1")) {
      sm.showImage(echoName, image);
    } else if (echoName.startsWith("\0")) {
      if (image != null) {
        setWindowDimensions(new float[] { apiPlatform.getImageWidth(image),
            apiPlatform.getImageHeight(image) });
      }
    } else {
      shm.loadShape(JC.SHAPE_ECHO);
      setShapeProperty(JC.SHAPE_ECHO, "text", nameOrError);
      if (image != null)
        setShapeProperty(JC.SHAPE_ECHO, "image", image);
    }
    if (isJS && sc != null) {
      sc.mustResumeEval = true;
      eval.resumeEval(sc);
    }
    return false;
  }

  public String cd(String dir) {
    if (dir == null) {
      dir = ".";
    } else if (dir.length() == 0) {
      setStringProperty("defaultDirectory", "");
      dir = ".";
    }
    dir = fm.getDefaultDirectory(
        dir + (dir.equals("=") ? "" : dir.endsWith("/") ? "X.spt" : "/X.spt"));
    if (dir.length() > 0)
      setStringProperty("defaultDirectory", dir);
    String path = fm.getFilePath(dir + "/", true, false);
    if (path.startsWith("file:/"))
      FileManager.setLocalPath(this, dir, false);
    return dir;
  }

  // //// Error handling

  public String setErrorMessage(String errMsg, String errMsgUntranslated) {
    errorMessageUntranslated = errMsgUntranslated;
    if (errMsg != null)
      eval.stopScriptThreads();
    return (errorMessage = errMsg);
  }

  @Override
  public String getErrorMessage() {
    return errorMessage;
  }

  @Override
  public String getErrorMessageUn() {
    return errorMessageUntranslated == null ? errorMessage
        : errorMessageUntranslated;
  }

  private int currentShapeID = -1;
  private String currentShapeState;

  public void setShapeErrorState(int shapeID, String state) {
    currentShapeID = shapeID;
    currentShapeState = state;
  }

  public String getShapeErrorState() {
    if (currentShapeID < 0)
      return "";
    shm.releaseShape(currentShapeID);
    clearRepaintManager(currentShapeID);
    return JC.getShapeClassName(currentShapeID, false) + " "
        + currentShapeState;
  }

  public void handleError(Error er, boolean doClear) {
    // almost certainly out of memory; could be missing Jar file
    try {
      if (doClear)
        zapMsg("" + er); // get some breathing room
      undoClear();
      if (Logger.getLogLevel() == 0)
        Logger.setLogLevel(Logger.LEVEL_INFO);
      setCursor(GenericPlatform.CURSOR_DEFAULT);
      setBooleanProperty("refreshing", true);
      fm.setPathForAllFiles("");
      Logger.error("vwr handling error condition: " + er + "  ");
      notifyError("Error", "doClear=" + doClear + "; " + er, "" + er);
    } catch (Throwable e1) {
      try {
        Logger.error("Could not notify error " + er + ": due to " + e1);
      } catch (Throwable er2) {
        // tough luck.
      }
    }
  }

  // / User-defined functions

  final static Map<String, JmolScriptFunction> staticFunctions = new Hashtable<String, JmolScriptFunction>();

  Map<String, JmolScriptFunction> localFunctions;

  public Map<String, JmolScriptFunction> getFunctions(boolean isStatic) {
    return (isStatic ? staticFunctions : localFunctions);
  }

  public void removeFunction(String name) {
    name = name.toLowerCase();
    JmolScriptFunction function = getFunction(name);
    if (function == null)
      return;
    staticFunctions.remove(name);
    localFunctions.remove(name);
  }

  public JmolScriptFunction getFunction(String name) {
    if (name == null)
      return null;
    JmolScriptFunction function = (isStaticFunction(name) ? staticFunctions
        : localFunctions).get(name);
    return (function == null || function.geTokens() == null ? null : function);
  }

  static boolean isStaticFunction(String name) {
    return name.startsWith("static_");
  }

  public boolean isFunction(String name) {
    return (isStaticFunction(name) ? staticFunctions : localFunctions)
        .containsKey(name);
  }

  public void clearFunctions() {
    staticFunctions.clear();
    localFunctions.clear();
  }

  public void addFunction(JmolScriptFunction function) {
    String name = function.getName();
    (isStaticFunction(name) ? staticFunctions : localFunctions).put(name,
        function);
  }

  public String getFunctionCalls(String selectedFunction) {
    return getStateCreator().getFunctionCalls(selectedFunction);
  }

  /**
   * Simple method to ensure that the image creator (which writes files) was in
   * fact opened by this vwr and not by some manipulation of the applet. When
   * the image creator is used it requires both a vwr object and that vwr's
   * private key. But the private key is private, so it is not possible to
   * create a useable image creator without working through a vwr's own methods.
   * Bob Hanson, 9/20/2009
   * 
   * @param privateKey
   * @return true if privateKey matches
   * 
   */

  public boolean checkPrivateKey(double privateKey) {
    return privateKey == this.privateKey;
  }

  public void bindAction(String desc, String name) {
    if (haveDisplay)
      acm.bind(desc, name);
  }

  public void unBindAction(String desc, String name) {
    if (haveDisplay)
      acm.unbindAction(desc, name);
  }

  public int calculateStruts(BS bs1, BS bs2) {
    return ms.calculateStruts(bs1 == null ? bsA() : bs1,
        bs2 == null ? bsA() : bs2);
  }

  /**
   * This flag if set FALSE:
   * 
   * 1) turns UNDO off for the application 2) turns history off 3) prevents
   * saving of inlinedata for later LOAD "" commands 4) turns off the saving of
   * changed atom properties 5) does not guarantee accurate state representation
   * 6) disallows generation of the state
   * 
   * It is useful in situations such as web sites where memory is an issue and
   * there is no need for such.
   * 
   * 
   * @return TRUE or FALSE
   */
  public boolean getPreserveState() {
    return (g.preserveState && scm != null);
  }

  boolean isKiosk;

  boolean isKiosk() {
    return isKiosk;
  }

  public boolean hasFocus() {
    return (haveDisplay && (isKiosk || apiPlatform.hasFocus(display)));
  }

  public void setFocus() {
    if (haveDisplay && !apiPlatform.hasFocus(display))
      apiPlatform.requestFocusInWindow(display);
  }

  void stopMinimization() {
    if (minimizer != null) {
      minimizer.setProperty("stop", null);
    }
  }

  void clearMinimization() {
    if (minimizer != null)
      minimizer.setProperty("clear", null);
  }

  public String getMinimizationInfo() {
    return (minimizer == null ? "" : (String) minimizer.getProperty("log", 0));
  }

  private void checkMinimization() {
    refreshMeasures(true);
    if (!g.monitorEnergy)
      return;
    try {
      minimize(null, 0, 0, getFrameAtoms(), null, 0, MIN_SILENT);
    } catch (Exception e) {
    }
    echoMessage(getP("_minimizationForceField") + " Energy = "
        + getP("_minimizationEnergy"));
  }

  public static final int MIN_SILENT = 1;
  public static final int MIN_HAVE_FIXED = 2;
  public static final int MIN_QUICK = 4;
  public static final int MIN_ADDH = 8;
  public static final int MIN_NO_RANGE = 16;

  /**
   * 
   * @param eval
   * @param steps
   *        Integer.MAX_VALUE --> use defaults
   * @param crit
   *        -1 --> use defaults
   * @param bsSelected
   * @param bsFixed
   * @param rangeFixed
   * @param flags
   * @throws Exception
   */
  public void minimize(JmolScriptEvaluator eval, int steps, float crit,
                       BS bsSelected, BS bsFixed, float rangeFixed, int flags)
      throws Exception {

    boolean isSilent = (flags & MIN_SILENT) == MIN_SILENT;
    boolean isQuick = (flags & MIN_QUICK) == MIN_QUICK;
    boolean hasRange = (flags & MIN_NO_RANGE) == 0;
    boolean addHydrogen = (flags & MIN_ADDH) == MIN_ADDH;
    // We only work on atoms that are in frame

    String ff = g.forceField;
    BS bsInFrame = getFrameAtoms();
    if (bsSelected == null)
      bsSelected = getModelUndeletedAtomsBitSet(getVisibleFramesBitSet().nextSetBit(0));
    else if (!isQuick)
      bsSelected.and(bsInFrame);
    if (isQuick) {
      getAuxiliaryInfoForAtoms(bsSelected).put("dimension", "3D");
      bsInFrame = bsSelected;
    }

    if (rangeFixed <= 0)
      rangeFixed = JC.MINIMIZE_FIXED_RANGE;

    // we allow for a set of atoms to be fixed, 
    // but that is only used by default

    BS bsMotionFixed = BSUtil
        .copy(bsFixed == null ? slm.getMotionFixedAtoms() : bsFixed);
    boolean haveFixed = (bsMotionFixed.cardinality() > 0);
    if (haveFixed)
      bsSelected.andNot(bsMotionFixed);

    // We always fix any atoms that
    // are in the visible frame set and are within 5 angstroms
    // and are not already selected

    BS bsNearby = (hasRange ? new BS()
        : ms.getAtomsWithinRadius(rangeFixed, bsSelected, true, null));
    bsNearby.andNot(bsSelected);
    if (haveFixed) {
      bsMotionFixed.and(bsNearby);
    } else {
      bsMotionFixed = bsNearby;
    }
    bsMotionFixed.and(bsInFrame);
    flags |= ((haveFixed ? MIN_HAVE_FIXED : 0)
        | (getBooleanProperty("minimizationSilent") ? MIN_SILENT : 0));
    if (isQuick && getBoolean(T.testflag2))
      return;
    if (isQuick) {
      {
        // carry out a preliminary UFF no-hydrogen calculation
        // to clean up benzene rings and amides.
        try {
          if (!isSilent)
            Logger.info("Minimizing " + bsSelected.cardinality() + " atoms");
          getMinimizer(true).minimize(steps, crit, bsSelected, bsMotionFixed,
              flags, "UFF");
        } catch (Exception e) {
          Logger.error("Minimization error: " + e.toString());
          e.printStackTrace();
        }

      }
    }
    if (addHydrogen) {
      BS bsH = addHydrogens(bsSelected, flags);
      if (!isQuick)
        bsSelected.or(bsH);
    }

    int n = bsSelected.cardinality();
    if (ff.equals("MMFF") && n > g.minimizationMaxAtoms) {
      scriptStatusMsg(
          "Too many atoms for minimization (" + n + ">" + g.minimizationMaxAtoms
              + "); use 'set minimizationMaxAtoms' to increase this limit",
          "minimization: too many atoms");
      return;
    }
    try {

      if (!isSilent)
        Logger.info("Minimizing " + bsSelected.cardinality() + " atoms");
      getMinimizer(true).minimize(steps, crit, bsSelected, bsMotionFixed, flags,
          (isQuick ? "MMFF" : ff));
      if (isQuick) {
        g.forceField = "MMFF";
        setHydrogens(bsSelected);
        showString("Minimized by Jmol",  false);
      }
    } catch (JmolAsyncException e) {
      if (eval != null)
        eval.loadFileResourceAsync(e.getFileName());
    } catch (Exception e) {
      Logger.error("Minimization error: " + e.toString());
      e.printStackTrace();
    }
  }

  private void setHydrogens(BS bsAtoms) {
    int[] nTotal = new int[1];
    P3[][] hatoms = ms.calculateHydrogens(bsAtoms, nTotal, null,
        AtomCollection.CALC_H_IGNORE_H | AtomCollection.CALC_H_QUICK);
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
      P3[] pts = hatoms[i];
      if (pts == null || pts.length == 0)
        continue;
      Atom a = ms.at[i];
      Bond[] b = a.bonds;
      for (int j = 0, pt = 0, n = a.getBondCount(); j < n; j++) {
        Atom h = b[j].getOtherAtom(a);
        if (h.getAtomicAndIsotopeNumber() == 1) {
          P3 p = pts[pt++];
          ms.setAtomCoord(h.i, p.x, p.y, p.z);
        }
      }
    }
    ms.resetMolecules();
  }

  public void setMotionFixedAtoms(BS bs) {
    slm.setMotionFixedAtoms(bs);
  }

  public BS getMotionFixedAtoms() {
    return slm.getMotionFixedAtoms();
  }

  //  void rotateArcBall(int x, int y, float factor) {
  //    tm.rotateArcBall(x, y, factor);
  //    refresh(REFRESH_SYNC, sm.syncingMouse ? "Mouse: rotateArcBall " + x + " "
  //        + y + " " + factor : "");
  //  }

  void getAtomicPropertyState(SB commands, byte type, BS bs, String name,
                              float[] data) {
    getStateCreator().getAtomicPropertyStateBuffer(commands, type, bs, name,
        data);
  }

  public P3[][] getCenterAndPoints(Lst<Object[]> atomSets, boolean addCenter) {
    return ms.getCenterAndPoints(atomSets, addCenter);
  }

  public String writeFileData(String fileName, String type, int modelIndex,
                              Object[] parameters) {
    return getOutputManager().writeFileData(fileName, type, modelIndex,
        parameters);
  }

  public String getPdbData(int modelIndex, String type, BS bsAtoms,
                           Object[] parameters, OC oc, boolean getStructure) {
    // plot command
    return getPropertyManager().getPdbData(modelIndex, type,
        bsAtoms == null ? bsA() : bsAtoms, parameters, oc, getStructure);
  }

  public BS getGroupsWithin(int nResidues, BS bs) {
    return ms.getGroupsWithin(nResidues, bs);
  }

  // parallel processing

  public static int nProcessors = 1;

  static {
    /**
     * @j2sIgnore
     * 
     */
    {
      nProcessors = Runtime.getRuntime().availableProcessors();
    }

  }

  public boolean displayLoadErrors = true;

  /**
   * 
   * @param shapeID
   * @param madOrMad10
   *        for axes, unitcell, and boundbox 10*mad; otherwise milliangstrom
   *        diameter
   * @param bsSelected
   */
  public void setShapeSize(int shapeID, int madOrMad10, BS bsSelected) {
    // might be atoms or bonds
    if (bsSelected == null)
      bsSelected = bsA();
    shm.setShapeSizeBs(shapeID, madOrMad10, null, bsSelected);
  }

  public void setShapeProperty(int shapeID, String propertyName, Object value) {
    // Eval, BondCollection, StateManager, local
    if (shapeID >= 0)
      shm.setShapePropertyBs(shapeID, propertyName, value, null);
  }

  public Object getShapeProperty(int shapeType, String propertyName) {
    return shm.getShapePropertyIndex(shapeType, propertyName,
        Integer.MIN_VALUE);
  }

  private int getShapePropertyAsInt(int shapeID, String propertyName) {
    Object value = getShapeProperty(shapeID, propertyName);
    return value == null || !(value instanceof Integer) ? Integer.MIN_VALUE
        : ((Integer) value).intValue();
  }

  public void setModelVisibility() {
    if (shm != null) // necessary for file chooser
      shm.setModelVisibility();
  }

  public void resetShapes(boolean andCreateNew) {
    shm.resetShapes();
    if (andCreateNew) {
      shm.loadDefaultShapes(ms);
      clearRepaintManager(-1);
    }
  }

  private boolean isParallel;

  public boolean setParallel(boolean TF) {
    return (isParallel = g.multiProcessor && TF);
  }

  public boolean isParallel() {
    return g.multiProcessor && isParallel;
  }

  void undoClear() {
    actionStates.clear();
    actionStatesRedo.clear();
  }

  /**
   * 
   * @param action
   *        Token.undo or Token.redo
   * @param n
   *        number of steps to go back/forward; 0 for all; -1 for clear; -2 for
   *        clear BOTH
   * 
   */
  public void undoMoveAction(int action, int n) {
    getStateCreator().undoMoveAction(action, n);
  }

  public void undoMoveActionClear(int taintedAtom, int type,
                                  boolean clearRedo) {
    // called by actionManager
    if (g.preserveState)
      getStateCreator().undoMoveActionClear(taintedAtom, type, clearRedo);
  }

  protected void moveAtomWithHydrogens(int atomIndex, int deltaX, int deltaY,
                                       int deltaZ, BS bsAtoms) {
    // called by actionManager
    stopMinimization();
    if (bsAtoms == null) {
      Atom atom = ms.at[atomIndex];
      bsAtoms = BSUtil.newAndSetBit(atomIndex);
      Bond[] bonds = atom.bonds;
      if (bonds != null)
        for (int i = 0; i < bonds.length; i++) {
          Atom atom2 = bonds[i].getOtherAtom(atom);
          if (atom2.getElementNumber() == 1)
            bsAtoms.set(atom2.i);
        }
    }
    moveSelected(deltaX, deltaY, deltaZ, Integer.MIN_VALUE, Integer.MIN_VALUE,
        bsAtoms, true, true, 0);
  }

  public boolean isModelPDB(int i) {
    return ms.am[i].isBioModel;
  }

  @Override
  public void deleteMeasurement(int i) {
    setShapeProperty(JC.SHAPE_MEASURES, "delete", Integer.valueOf(i));
  }

  @Override
  public String getSmiles(BS bs) throws Exception {
    return getSmilesOpt(bs, -1, -1,
        (bs == null && Logger.debugging ? JC.SMILES_GEN_ATOM_COMMENT : 0)
        ,
        null);
  }

  @Override
  public String getOpenSmiles(BS bs) throws Exception {
    return getSmilesOpt(bs, -1, -1,
        JC.SMILES_TYPE_OPENSMILES
            | (bs == null && Logger.debugging ? JC.SMILES_GEN_ATOM_COMMENT : 0)
            ,
        "/openstrict///");
  }

  public String getBioSmiles(BS bs) throws Exception {
    return getSmilesOpt(bs, -1, -1,
        JC.SMILES_GEN_BIO_ALLOW_UNMATCHED_RINGS
            | JC.SMILES_GEN_BIO_COV_CROSSLINK | JC.SMILES_GEN_BIO_COMMENT
            | (Logger.debugging ? JC.SMILES_GEN_ATOM_COMMENT : 0),
        null);
  }

  /**
   * returns the SMILES string for a sequence or atom set does not include
   * attached protons on groups
   * 
   * @param bsSelected
   *        selected atom set or null for current or specified range
   * @param index1
   *        when bsSeleced == null, first atomIndex or -1 for current
   * @param index2
   *        when bsSeleced == null, end atomIndex or -1 for current
   * @param flags
   *        see JC.SMILES_xxxx
   * @param options
   *        e.g. /strict,open/
   * @return SMILES string
   * @throws Exception
   */
  public String getSmilesOpt(BS bsSelected, int index1, int index2, int flags,
                             String options)
      throws Exception {
    String bioComment = ((flags
        & JC.SMILES_GEN_BIO_COMMENT) == JC.SMILES_GEN_BIO_COMMENT
            ? getJmolVersion() + " " + getModelName(am.cmi)
            : options);
    Atom[] atoms = ms.at;
    if (bsSelected == null) {
      if (index1 < 0 || index2 < 0) {
        bsSelected = bsA();
      } else {
        if ((flags & JC.SMILES_GEN_BIO) == JC.SMILES_GEN_BIO) {
          if (index1 > index2) {
            int i = index1;
            index1 = index2;
            index2 = i;
          }
          index1 = atoms[index1].group.firstAtomIndex;
          index2 = atoms[index2].group.lastAtomIndex;
        }
        bsSelected = new BS();
        bsSelected.setBits(index1, index2 + 1);
      }
    }
    flags |= (isModel2D(bsSelected) ? JC.SMILES_2D : 0);
    SmilesMatcherInterface sm = getSmilesMatcher();
    if (JC.isSmilesCanonical(options)) {
      String smiles = sm.getSmiles(atoms, ms.ac, bsSelected, "/noAromatic/",
          flags);
      return getChemicalInfo(smiles, "smiles", null).trim();
    }
    return sm.getSmiles(atoms, ms.ac, bsSelected, bioComment, flags);
  }

  private boolean isModel2D(BS bs) {
    Model m = getModelForAtomIndex(bs.nextSetBit(0));
    return (m != null && "2D".equals(m.auxiliaryInfo.get("dimension")));
  }

  public void alert(String msg) {
    prompt(msg, null, null, true);
  }

  public String prompt(String label, String data, String[] list,
                       boolean asButtons) {
    return (isKiosk ? "null"
        : apiPlatform.prompt(label, data, list, asButtons));
  }

  /**
   * Ask for new file name when saving or opening a file in Java and saving a file in JavaScript.
   * JavaScript use of FileReader goes through loadFileAsync
   * 
   * @param type
   * @param fileName
   * @param params
   * @return new file name
   */
  public String dialogAsk(String type, String fileName,
                          Map<String, Object> params) {
    /**
     * @j2sNative
     * 
     *            return prompt(type, fileName);
     * 
     */
    {
      // may have #NOCARTOONS#; and/or "#APPEND#; prepended
      return (isKiosk || !haveAccess(ACCESS.ALL) ? null
          : sm.dialogAsk(type, fileName, params));
    }
  }

  public int stateScriptVersionInt = Integer.MAX_VALUE;

  private JmolRendererInterface jsExporter3D;

  public JmolRendererInterface initializeExporter(Map<String, Object> params) {
    boolean isJS = params.get("type").equals("JS");
    if (isJS) {
      if (jsExporter3D != null) {
        jsExporter3D.initializeOutput(this, privateKey, params);
        return jsExporter3D;
      }
    } else {
      String fileName = (String) params.get("fileName");
      String[] fullPath = (String[]) params.get("fullPath");
      OC out = getOutputChannel(fileName, fullPath);
      if (out == null)
        return null;
      params.put("outputChannel", out);
    }
    JmolRendererInterface export3D = (JmolRendererInterface) Interface
        .getOption("export.Export3D", this, "export");
    if (export3D == null)
      return null;
    Object exporter = export3D.initializeExporter(this, privateKey, gdata,
        params);
    if (isJS && exporter != null)
      jsExporter3D = export3D;
    return (exporter == null ? null : export3D);
  }

  public boolean getMouseEnabled() {
    return refreshing && !creatingImage;
  }

  @Override
  public void calcAtomsMinMax(BS bs, BoxInfo boxInfo) {
    ms.calcAtomsMinMax(bs, boxInfo);
  }

  /**
   * used in autocompletion in console using TAB
   * 
   * @param map
   * @param c
   */
  @SuppressWarnings("unchecked")
  public void getObjectMap(Map<String, ?> map, char c) {
    switch (c) {
    case '{':
      if (getScriptManager() != null) {
        Map<String, Object> m = (Map<String, Object>) map;
        if (definedAtomSets != null)
          m.putAll(definedAtomSets);
        T.getTokensType(m, T.predefinedset);
      }
      return;
    case '$':
    case '0':
      shm.getObjectMap(map, c == '$');
      return;
    }
  }

  public void setPicked(int atomIndex, boolean andReset) {
    SV pickedSet = null;
    SV pickedList = null;
    if (atomIndex >= 0) {
      if (andReset)
        setPicked(-1, false);
      g.setI("_atompicked", atomIndex);
      pickedSet = (SV) g.getParam("picked", true);
      pickedList = (SV) g.getParam("pickedList", true);
    }
    if (pickedSet == null || pickedSet.tok != T.bitset) {
      pickedSet = SV.newV(T.bitset, new BS());
      pickedList = SV.getVariableList(new Lst<Object>());
      g.setUserVariable("picked", pickedSet);
      g.setUserVariable("pickedList", pickedList);
    }
    if (atomIndex < 0)
      return;
    SV.getBitSet(pickedSet, false).set(atomIndex);
    SV p = pickedList.pushPop(null, null);
    // don't allow double click
    if (p.tok == T.bitset)
      pickedList.pushPop(null, p);
    if (p.tok != T.bitset || !((BS) p.value).get(atomIndex))
      pickedList.pushPop(null,
          SV.newV(T.bitset, BSUtil.newAndSetBit(atomIndex)));
  }

  /**
   * Run a script using the script function script("xxxxxx") using direct script
   * tokens for script ( "xxxxxxx" )
   *
   */
  @Override
  public String runScript(String script) {
    return "" + evaluateExpression(new T[][] { new T[] { T.tokenScript,
        T.tokenLeftParen, SV.newS(script), T.tokenRightParen } });
  }

  /**
   * formerly runScript(), this method really can ONLY be called by the viewer
   * being run from an already-running script. If it is invoked by a separate
   * thread, it can wreak havoc on any queued thread, since they are not thread
   * safe.
   * 
   * @param script
   * @return output of the script.
   */
  @Override
  public String runScriptCautiously(String script) {
    // from isosurface reading JVXL file with slab
    SB outputBuffer = new SB();
    try {
      if (getScriptManager() == null)
        return null;
      eval.runScriptBuffer(script, outputBuffer, false);
    } catch (Exception e) {
      return eval.getErrorMessage();
    }
    return outputBuffer.toString();
  }

  public void setFrameDelayMs(long millis) {
    ms.setFrameDelayMs(millis, getVisibleFramesBitSet());
  }

  public BS getBaseModelBitSet() {
    return ms.getModelAtomBitSetIncludingDeleted(getJDXBaseModelIndex(am.cmi),
        true);
  }

  public Map<String, Object> timeouts;

  public void clearTimeouts() {
    if (timeouts != null)
      TimeoutThread.clear(timeouts);
  }

  public void setTimeout(String name, int mSec, String script) {
    if (!haveDisplay || headless || autoExit)
      return;
    if (name == null) {
      clearTimeouts();
      return;
    }
    if (timeouts == null) {
      timeouts = new Hashtable<String, Object>();
    }
    TimeoutThread.setTimeout(this, timeouts, name, mSec, script);
  }

  public void triggerTimeout(String name) {
    if (!haveDisplay || timeouts == null)
      return;
    TimeoutThread.trigger(timeouts, name);
  }

  public void clearTimeout(String name) {
    setTimeout(name, 0, null);
  }

  public String showTimeout(String name) {
    return (haveDisplay ? TimeoutThread.showTimeout(timeouts, name) : "");
  }

  public float[] getOrCalcPartialCharges(BS bsSelected, BS bsIgnore)
      throws JmolAsyncException {
    if (bsSelected == null)
      bsSelected = bsA();
    bsSelected = BSUtil.copy(bsSelected);
    BSUtil.andNot(bsSelected, bsIgnore);
    BSUtil.andNot(bsSelected, ms.bsPartialCharges);
    if (!bsSelected.isEmpty())
      calculatePartialCharges(bsSelected);
    return ms.getPartialCharges();
  }

  public void calculatePartialCharges(BS bsSelected) throws JmolAsyncException {
    if (bsSelected == null || bsSelected.isEmpty())
      bsSelected = getFrameAtoms();
    if (bsSelected.isEmpty())
      return;
    //    // this forces an array if it does not exist 
    //    setAtomProperty(BSUtil.newAndSetBit(pt), T.partialcharge, 0, 1f, null,
    //        null, null);
    Logger.info("Calculating MMFF94 partial charges for "
        + bsSelected.cardinality() + " atoms");
    getMinimizer(true).calculatePartialCharges(ms, bsSelected, null);
  }

  public void setCurrentModelID(String id) {
    int modelIndex = am.cmi;
    if (modelIndex >= 0)
      ms.setInfo(modelIndex, "modelID", id);
  }

  public void cacheClear() {
    // script: reset cache
    fm.cacheClear();
    ligandModelSet = null;
    ligandModels = null;
    ms.clearCache();
  }

  /**
   * JSInterface -- allows saving files in memory for later retrieval
   * 
   * @param key
   * @param data
   * 
   */

  public void cachePut(String key, Object data) {
    // PyMOL reader and isosurface
    // HTML5/JavaScript load ?  and  script ? 
    Logger.info("Viewer cachePut " + key);
    fm.cachePut(key, data);
  }

  public int cacheFileByName(String fileName, boolean isAdd) {
    // cache command in script
    if (fileName == null) {
      cacheClear();
      return -1;
    }
    return fm.cacheFileByNameAdd(fileName, isAdd);
  }

  public void clearThreads() {
    if (eval != null)
      eval.stopScriptThreads();
    stopMinimization();
    tm.clearThreads();
    setAnimationOn(false);
  }

  public ScriptContext getEvalContextAndHoldQueue(JmolScriptEvaluator eval) {
    if (eval == null || !(isJS || testAsync))
      return null;
    eval.pushContextDown(ScriptEval.CONTEXT_HOLD_QUEUE);
    ScriptContext sc = eval.getThisContext();
    sc.setMustResume();
    sc.isJSThread = true;
    queueOnHold = true;
    return sc;
  }

  public String getDefaultPropertyParam(int propertyID) {
    return getPropertyManager().getDefaultPropertyParam(propertyID);
  }

  public int getPropertyNumber(String name) {
    return getPropertyManager().getPropertyNumber(name);
  }

  public boolean checkPropertyParameter(String name) {
    return getPropertyManager().checkPropertyParameter(name);
  }

  public Object extractProperty(Object property, Object args, int pt) {
    return getPropertyManager().extractProperty(property, args, pt, null,
        false);
  }

  //// requiring ScriptEvaluator:

  public BS addHydrogens(BS bsAtoms, int flags) {
    boolean isSilent = ((flags & MIN_SILENT) == MIN_SILENT);
    boolean isQuick = ((flags & MIN_QUICK) == MIN_QUICK);
    boolean doAll = (bsAtoms == null);
    if (bsAtoms == null)
      bsAtoms = getModelUndeletedAtomsBitSet(
          getVisibleFramesBitSet().length() - 1);
    BS bsB = new BS();
    if (bsAtoms.isEmpty())
      return bsB;
//    if (!ms.isAtomInLastModel(bsAtoms.nextSetBit(0)))
//      return bsB;
    Lst<Atom> vConnections = new Lst<Atom>();
    P3[] pts = getAdditionalHydrogens(bsAtoms, vConnections,
        flags | (doAll ? AtomCollection.CALC_H_DOALL : 0));
    boolean wasAppendNew = false;
    wasAppendNew = g.appendNew;
    if (pts.length > 0) {
      clearModelDependentObjects();
      try {
        bsB = (isQuick ? ms.addHydrogens(vConnections, pts)
            : addHydrogensInline(bsAtoms, vConnections, pts));
      } catch (Exception e) {
        System.out.println(e.toString());
        // ignore
      }
      if (wasAppendNew)
        g.appendNew = true;
    }
    if (!isSilent)
      scriptStatus(GT.i(GT.$("{0} hydrogens added"), pts.length));
    return bsB;
  }

  public BS addHydrogensInline(BS bsAtoms, Lst<Atom> vConnections, P3[] pts)
      throws Exception {
    if (getScriptManager() == null)
      return null;
    return scm.addHydrogensInline(bsAtoms, vConnections, pts);
  }

  @Override
  public float evalFunctionFloat(Object func, Object params, float[] values) {
    return (getScriptManager() == null ? 0
        : eval.evalFunctionFloat(func, params, values));
  }

  public boolean evalParallel(ScriptContext context,
                              ShapeManager shapeManager) {
    displayLoadErrors = false;
    boolean isOK = getScriptManager() != null && eval.evalParallel(context,
        (shapeManager == null ? this.shm : shapeManager));
    displayLoadErrors = true;
    return isOK;
  }

  /**
   * synchronized here trapped the eventQueue; see also
   * evaluateExpressionAsVariable
   * 
   */
  @Override
  public Object evaluateExpression(Object stringOrTokens) {
    return (getScriptManager() == null ? null
        : eval.evaluateExpression(stringOrTokens, false, false));
  }

  @Override
  public SV evaluateExpressionAsVariable(Object stringOrTokens) {
    return (getScriptManager() == null ? null
        : (SV) eval.evaluateExpression(stringOrTokens, true, false));
  }

  public BS getAtomBitSet(Object atomExpression) {
    // SMARTS searching
    // getLigandInfo
    // used in interaction with JSpecView
    // used for set picking SELECT

    if (atomExpression instanceof BS)
      return slm.excludeAtoms((BS) atomExpression, false);
    getScriptManager();
    return getAtomBitSetEval(eval, atomExpression);
  }

  public ScriptContext getScriptContext(String why) {
    return (getScriptManager() == null ? null : eval.getScriptContext(why));
  }

  public String getAtomDefs(Map<String, Object> names) {
    Lst<String> keys = new Lst<String>();
    for (Map.Entry<String, ?> e : names.entrySet())
      if (e.getValue() instanceof BS)
        keys.addLast("{" + e.getKey() + "} <"
            + ((BS) e.getValue()).cardinality() + " atoms>\n");
    int n = keys.size();
    String[] k = new String[n];
    keys.toArray(k);
    Arrays.sort(k);
    SB sb = new SB();
    for (int i = 0; i < n; i++)
      sb.append(k[i]);
    return sb.append("\n").toString();
  }

  public void setCGO(Lst<Object> info) {
    shm.loadShape(JC.SHAPE_CGO);
    shm.setShapePropertyBs(JC.SHAPE_CGO, "setCGO", info, null);
  }

  public void setModelSet(ModelSet modelSet) {
    this.ms = mm.modelSet = modelSet;
  }

  public String setObjectProp(String id, int tokCommand) {
    // for PyMOL session scene setting
    getScriptManager();
    if (id == null)
      id = "*";
    return (eval == null ? null : eval.setObjectPropSafe(id, tokCommand));
  }

  public void setDihedrals(float[] dihedralList, BS[] bsBranches, float rate) {
    if (bsBranches == null)
      bsBranches = ms.getBsBranches(dihedralList);
    ms.setDihedrals(dihedralList, bsBranches, rate);
  }

  private boolean chainCaseSpecified;

  /**
   * Create a unique integer for any chain string. Note that if there are any
   * chains that are more than a single character, chainCaseSensitive is
   * automatically set TRUE
   * 
   * 
   * @param id
   *        < 256 is just the character of a single-character upper-case chain
   *        id, upper or lower case query;
   * 
   *        >= 256 < 300 is lower case found in structure
   * 
   * @param isAssign
   *        from a file reader, not a select query
   * 
   * @return i
   */
  public int getChainID(String id, boolean isAssign) {
    // if select :a and there IS chain "a" in a structure,
    // then we return that id, and all is good. Chain selectivity
    // is inforced, and we will find it.
    Integer iboxed = (Integer) chainMap.get(id);
    if (iboxed != null)
      return iboxed.intValue();
    int i = id.charAt(0);
    if (id.length() > 1) {
      i = 300 + chainList.size();
    } else if ((isAssign || chainCaseSpecified) && 97 <= i && i <= 122) { // lower case
      i += 159; // starts at 256
    }
    if (i >= 256) {
    	iboxed = (Integer) chainMap.get(id);
        if (iboxed != null)
          return iboxed.intValue();
      //this will force chainCaseSensitive when it is necessary
      chainCaseSpecified |= isAssign;
      chainList.addLast(id);
    }
    // if select :a and there is NO chain "a" in the structure,
    // there still might be an "A" and we cannot check for 
    // chain case sensitivity yet, as we are parsing the script,
    // not processing it. So we just store this one as 97-122.

    iboxed = Integer.valueOf(i);
    chainMap.put(iboxed, id);
    chainMap.put(id, iboxed);
    return i;
  }

  public String getChainIDStr(int id) {
    return (String) chainMap.get(Integer.valueOf(id));
  }

  public Boolean getScriptQueueInfo() {
    return (scm != null && scm.isQueueProcessing() ? Boolean.TRUE
        : Boolean.FALSE);
  }

  JmolNMRInterface nmrCalculation;

  public JmolNMRInterface getNMRCalculation() {
    return (nmrCalculation == null
        ? (nmrCalculation = (JmolNMRInterface) Interface
            .getOption("quantum.NMRCalculation", this, "script"))
                .setViewer(this)
        : nmrCalculation);
  }

  public String getDistanceUnits(String s) {
    if (s == null)
      s = getDefaultMeasurementLabel(2);
    int pt = s.indexOf("//");
    return (pt < 0 ? g.measureDistanceUnits : s.substring(pt + 2));
  }

  public int calculateFormalCharges(BS bs) {
    return ms.fixFormalCharges(bs == null ? bsA() : bs);
  }

  public void setModulation(BS bs, boolean isOn, P3 t1, boolean isQ) {
    if (isQ)
      g.setO("_modt", Escape.eP(t1));
    ms.setModulation(bs == null ? getAllAtoms() : bs, isOn, t1, isQ);
    refreshMeasures(true);
  }

  public void checkInMotion(int state) {
    switch (state) {
    case 0: // off
      setTimeout("_SET_IN_MOTION_", 0, null);
      break;
    case 1: // start 1-second timer (by default)
      if (!inMotion)
        setTimeout("_SET_IN_MOTION_", g.hoverDelayMs * 2, "!setInMotion");
      break;
    case 2: // trigger, from a timeout thread
      setInMotion(true);
      refresh(REFRESH_SYNC_MASK, "timeoutThread set in motion");
      break;
    }
  }

  /**
   * check motion for rendering during mouse movement, spin, vibration, and
   * animation
   * 
   * @param tok
   * @return TRUE if allowed
   */
  public boolean checkMotionRendering(int tok) {
    if (!getInMotion(true) && !tm.spinOn && !tm.vibrationOn && !am.animationOn)
      return true;
    if (g.wireframeRotation)
      return false;
    int n = 0;
    switch (tok) {
    case T.bonds:
    case T.atoms:
      n = 2;
      break;
    case T.ellipsoid:
      n = 3;
      break;
    case T.geosurface:
      n = 4;
      break;
    case T.cartoon:
      n = 5;
      break;
    case T.mesh:
      n = 6;
      break;
    case T.translucent:
      n = 7;
      break;
    case T.antialiasdisplay:
      n = 8;
      break;
    }
    return g.platformSpeed >= n;
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to JmolFileAdapter
  // ///////////////////////////////////////////////////////////////

  public OC openExportChannel(double privateKey, String fileName,
                              boolean asWriter)
      throws IOException {
    return getOutputManager().openOutputChannel(privateKey, fileName, asWriter,
        false);
  }

  /*default*/String logFileName;

  @Override
  public void log(String data) {
    if (data != null)
      getOutputManager().logToFile(data);
  }

  public String getLogFileName() {
    return (logFileName == null ? "" : logFileName);
  }

  public String getCommands(Map<String, BS> htDefine, Map<String, BS> htMore,
                            String select) {
    return getStateCreator().getCommands(htDefine, htMore, select);
  }

  public boolean allowCapture() {
    return !isApplet || isSignedApplet;
  }

  public T[] compileExpr(String expr) {
    Object o = (getScriptManager() == null ? null
        : eval.evaluateExpression(expr, false, true));
    return (o instanceof T[] ? (T[]) o : new T[] { T.o(T.string, expr) });
  }

  public boolean checkSelect(Map<String, SV> h, T[] value) {
    return getScriptManager() != null && eval.checkSelect(h, value);
  }

  public String getAnnotationInfo(SV d, String match, int type) {
    return getAnnotationParser(type == T.dssr).getAnnotationInfo(this, d, match,
        type, am.cmi);
  }

  public Lst<Float> getAtomValidation(String type, Atom atom) {
    return getAnnotationParser(false).getAtomValidation(this, type, atom);
  }

  void dragMinimizeAtom(final int iAtom) {
    stopMinimization();
    BS bs = (getMotionFixedAtoms().isEmpty()
        ? ms.getAtoms((ms.isAtomPDB(iAtom) ? T.group : T.molecule),
            BSUtil.newAndSetBit(iAtom))
        : BSUtil.setAll(ms.ac));
    try {
      minimize(null, Integer.MAX_VALUE, 0, bs, null, 0, 0);
    } catch (Exception e) {
      if (!async)
        return;
      final Viewer me = this;
      @SuppressWarnings("unused")
      Runnable r = new Runnable() {
        @Override
        public void run() {
          me.dragMinimizeAtom(iAtom);
        }
      };
      /**
       * @j2sNative
       * 
       *            setTimeout(function(){r.run()}, 100);
       * 
       */
      {
      }
    }
  }

  BioResolver jbr;

  public BioResolver getJBR() {
    return (jbr == null
        ? jbr = ((BioResolver) Interface
            .getInterface("org.jmol.modelsetbio.BioResolver", this, "file"))
                .setViewer(this)
        : jbr);
  }

  public void checkMenuUpdate() {
    if (jmolpopup != null)
      jmolpopup.jpiUpdateComputedMenus();
  }

  private JmolChimeMessenger jcm;

  public JmolChimeMessenger getChimeMessenger() {
    return (jcm == null
        ? jcm = ((JmolChimeMessenger) Interface
            .getInterface("org.jmol.viewer.ChimeMessenger", this, "script"))
                .set(this)
        : jcm);
  }

  public Map<String, Object> getAuxiliaryInfoForAtoms(Object atomExpression) {
    return ms
        .getAuxiliaryInfo(ms.getModelBS(getAtomBitSet(atomExpression), false));
  }

  private JSJSONParser jsonParser;

  private JSJSONParser getJSJSONParser() {
    return (jsonParser == null
        ? jsonParser = (JSJSONParser) Interface
            .getInterface("javajs.util.JSJSONParser", this, "script")
        : jsonParser);
  }

  public Object parseJSON(String str) {
    return (str == null ? null
        : (str = str.trim()).startsWith("{") ? parseJSONMap(str)
            : parseJSONArray(str));
  }

  public Map<String, Object> parseJSONMap(String jsonMap) {
    return getJSJSONParser().parseMap(jsonMap, true);
  }

  @SuppressWarnings("unchecked")
  public Lst<Object> parseJSONArray(String jsonArray) {
    return (Lst<Object>) getJSJSONParser().parse(jsonArray, true);
  }

  /**
   * Retrieve a Symmetry object, possibly re-using an old one.
   * 
   * @return org.jmol.symmetry.Symmetry object
   */
  public SymmetryInterface getSymTemp() {
    return Interface.getSymmetry(this, "ms");
  }

  public void setWindowDimensions(float[] dims) {
    resizeInnerPanel((int) dims[0], (int) dims[1]);
  }

  private Triangulator triangulator;

  public Triangulator getTriangulator() {
    return (triangulator == null
        ? (triangulator = (Triangulator) Interface.getUtil("Triangulator", this,
            "script"))
        : triangulator);
  }

  public Map<String, Object> getCurrentModelAuxInfo() {
    return (am.cmi >= 0 ? ms.getModelAuxiliaryInfo(am.cmi) : null);
  }

  public void startNBO(String options) {
    Map<String, Object> htParams = new Hashtable<String, Object>();
    htParams.put("service", "nbo");
    htParams.put("action", "showPanel");
    htParams.put("options", options);
    sm.processService(htParams);
  }

  /**
   * startup -U nbo option
   * 
   * @param plugin
   */
  public void startPlugin(String plugin) {

    // for now, just NBO; need a way to bootstrap this

    if ("nbo".equalsIgnoreCase(plugin))
      startNBO("all");
  }

  private NBOParser nboParser;

  public void connectNBO(String type) {
    if (am.cmi < 0)
      return;
    getNBOParser().connectNBO(am.cmi, type);
  }

  private NBOParser getNBOParser() {
    return (nboParser == null ? nboParser = ((NBOParser) Interface.getInterface(
        "org.jmol.adapter.readers.quantum.NBOParser", this, "script")).set(this)
        : nboParser);
  }

  public String getNBOAtomLabel(Atom atom) {
    return getNBOParser().getNBOAtomLabel(atom);
  }

  public String calculateChirality(BS bsAtoms) {
    if (bsAtoms == null)
      bsAtoms = bsA();
    return ms.calculateChiralityForAtoms(bsAtoms, true);
  }

  public BS[] getSubstructureSetArray(String pattern, BS bsSelected, int flags)
      throws Exception {
    return getSmilesMatcher().getSubstructureSetArray(pattern, ms.at, ms.ac,
        bsSelected, null, flags);
  }

  public BS[] getSubstructureSetArrayForNodes(String pattern, Node[] nodes,
                                              int flags)
      throws Exception {
    return getSmilesMatcher().getSubstructureSetArray(pattern, nodes,
        nodes.length, null, null, flags);
  }

  public Node[] getSmilesAtoms(String smiles) throws Exception {
    return getSmilesMatcher().getAtoms(smiles);
  }

  public String[] calculateChiralityForSmiles(String smiles) {
    try {
      return Interface.getSymmetry(this, "ms")
          .calculateCIPChiralityForSmiles(this, smiles);
    } catch (Exception e) {
      return null;
    }
  }

  public String getPdbID() {
    return (ms.getInfo(am.cmi, "isPDB") == Boolean.TRUE
        ? (String) ms.getInfo(am.cmi, "pdbID")
        : null);
  }

  /**
   * get a value from the current model's Model.auxiliaryInfo
   * 
   * @param key
   * @return value, or null if there is no SINGLE current model
   */
  public Object getModelInfo(String key) {
    return ms.getInfo(am.cmi, key);
  }

  public void notifyScriptEditor(int msWalltime, Object[] data) {
    if (scriptEditor != null) {
      scriptEditor.notify(msWalltime, data);
    }
  }

  public void sendConsoleMessage(String msg) {
    if (appConsole != null)
      appConsole.sendConsoleMessage(msg);
  }

  /**
   * Get a ModelKit property.
   * 
   * @param name
   * @return value
   */
  public Object getModelkitProperty(String name) {
    return (modelkit == null ? null : modelkit.getProperty(name));
  }

  public Object setModelkitProperty(String key, Object value) {
    return (modelkit == null ? null : modelkit.setProperty(key, value));
  }

  /**
   * A general method for retrieving symmetry information with full capability
   * of the symop() scripting function.
   * 
   * @param iatom
   *        atom index specifying the model set and used for pt1 if that is null
   *        and also for matching element type.
   * @param xyz
   *        the desired Jones-Faithful representation of the symmetry operation
   *        or null
   * @param iOp
   *        the desired symmetry operation [1-n] or 0
   * @param translation [i j k] translational addition to symop
   * @param pt1
   *        the starting point, or null if to use iatom or otherwise unnecessary
   * @param pt2
   *        the target point, if this is a point-to-point determination, or the
   *        offset if not and options is nonzero
   * @param type
   *        a token type such as T.list or T.array
   * @param desc
   *        if type == T.nada (0), a name evaluating to a type, or one of the
   *        special names: "info", "description", "matrix", "axispoint", or
   *        "time" (as in time-reversal); otherwise, if type == T.draw, the root
   *        id given to a returned DRAW command set
   * @param scaleFactor
   *        if nonzero and type == T.draw, a scaling factor to be applied to the
   *        rotational vector
   * @param nth
   *        in the case of a point-to-point determination, the nth matching
   *        operator, or 0 for "all"
   * @param options
   *        if nonzero, a option, currently just T.offset, indicating that pt1
   *        is an {i j k} offset from cell 555
   * @return string, Object[], or Lst<Object[]>
   */
  public Object getSymmetryInfo(int iatom, String xyz, int iOp, P3 translation, P3 pt1,
                                P3 pt2, int type, String desc,
                                float scaleFactor, int nth, int options) {
    try {
      return getSymTemp().getSymmetryInfoAtom(ms, iatom, xyz, iOp, translation, pt1,
          pt2, desc, type, scaleFactor, nth, options);
    } catch (Exception e) {
      System.out.println("Exception in Viewer.getSymmetryInfo: " + e);
      if (!isJS)
        e.printStackTrace();
      return null;
    }
  }

  /**
   * 
   * @param i
   *        Integer.MIN_VALUE initializes the bond index
   */
  public void setModelKitRotateBondIndex(int i) {
    if (modelkit != null) {
      modelkit.setProperty("rotateBondIndex", Integer.valueOf(i));
    }
  }

  private Map<String, Object> macros;

  /**
   * retrieve macros.json from the directory
   * 
   * @param key
   * @return the macro path
   */
  @SuppressWarnings("unchecked")
  public String getMacro(String key) {
    if (macros == null || macros.isEmpty()) {
      try {
        String s = getAsciiFileOrNull(g.macroDirectory + "/macros.json");
        macros = (Map<String, Object>) parseJSON(s);
      } catch (Exception e) {
        macros = new Hashtable<String, Object>();
      }
      //    {
      //        aflow:{path:"https://chemapps.stolaf.edu/jmol/macros/AFLOW.spt", title:"AFLOW macros"},
      //        bz:{path:"https://chemapps.stolaf.edu/jmol/macros/bz.spt", title: "Brillouin Zone/Wigner-Seitz macros"},
      //        topology:{path:"https://chemapps.stolaf.edu/jmol/macros/topology.spt", title: "Topology CIF macros"},
      //        topond:{path:"https://chemapps.stolaf.edu/jmol/macros/topond.spt", title: "CRYSTAL/TOPOND macros"},
      //        crystal:{path:"https://chemapps.stolaf.edu/jmol/macros/crystal.spt", title: "CRYSTAL macros"}
      //     }; 
    }
    if (key == null) {
      SB s = new SB();
      for (String k : macros.keySet()) {
        Object a = macros.get(k);
        s.append(k).append("\t").appendO(a).append("\n");
      }
      return s.toString();
    }
    key = key.toLowerCase();
    return macros.containsKey(key)
        ? ((Map<String, Object>) macros.get(key)).get("path").toString()
        : null;
  }

  private int consoleFontScale = 1;

  public int getConsoleFontScale() {
    return consoleFontScale;
  }

  public void setConsoleFontScale(int scale) {
    consoleFontScale = scale;
  }

  public int confirm(String msg, String msgNo) {
    return apiPlatform.confirm(msg, msgNo);
  }

  /**
   * Run a script asynchronously, adding the GUI flag to indicate that we should
   * fire the SELECT callback at the end if there is one.
   * 
   * @param script
   */
  public void evalStringGUI(String script) {
    evalStringQuiet(script + JC.SCRIPT_GUI);
  }

  /**
   * "SELECT" starting with comma triggers the SELECT callback from a SELECT command.
   * GUI scripts also trigger this if a select command has been given and the 
   * last select command given did not start with comma.
   * 
   * @param bs
   * @param isGroup
   * @param addRemove
   * @param isQuiet
   * @param reportStatus
   */
  public void selectStatus(BS bs, boolean isGroup, int addRemove, boolean isQuiet,
                           boolean reportStatus) {
    select(bs, isGroup, addRemove, isQuiet);
    if (reportStatus) {
      setStatusSelect(bs);
    }
  }

  /**
   * Make the SelectCallback call and reset the hasSelected value to false. 
   * This method is called by SELECT , ... or by a GUI script command completion
   * or an atom selection using the mouse.
   * 
   * 
   * @param bs
   */
  public void setStatusSelect(BS bs) {
    hasSelected = false;
    sm.setStatusSelect(bs == null ? bsA() : bs);
  }

  /**
   * WASM inchi must be initialized asynchronously
   * 
   * @param cmd
   * @return cmd
   */
  public String wasmInchiHack(String cmd) {
    if (Viewer.isJS
        && (cmd.indexOf("inchi") >= 0 || cmd.indexOf("INCHI") >= 0)
        || cmd.indexOf("TAUTOMER") >= 0 || cmd.indexOf("tautomer") >= 0) {
      getInchi(null, null, null);
    }
    return cmd;
  }

  /**
   * Get an InChI or InChIKey for a set of atoms or MOL data.
   * 
   * @param atoms
   * @param molData null, or MOL data, or a database $ or : call, or SMILES, or "InChI=...." to retrieve the key
   * @param options
   * @return InChI or InChIKey
   */
  public String getInchi(BS atoms, String molData, String options) {
    try {
      JmolInChI inch = this.apiPlatform.getInChI();
      if (atoms == null && molData == null) {
        // JavaScript initialization only
        return "";
      }
      if (molData != null) {
        if (molData.startsWith("$") || molData.startsWith(":")) {
          molData = getFileAsString4(molData, -1, false, false, true, "script");
        } else if (!molData.startsWith("InChI=") && molData.indexOf(" ") < 0) {
          // assume SMILES
          molData = getFileAsString4("$" + molData, -1, false, false, true,
              "script");
        }
      }
      return inch.getInchi(this, atoms, molData, options);
    } catch (Throwable t) {
      return "";
    }
  }


}
