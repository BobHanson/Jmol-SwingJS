/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-26 23:35:44 -0500 (Fri, 26 Jun 2009) $
 * $Revision: 11131 $
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
package org.openscience.jmol.app.jmolpanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.PropertyResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.jmol.api.Interface;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolScriptManager;
import org.jmol.api.JmolStatusListener;
import org.jmol.awt.FileDropper;
import org.jmol.awt.Platform;
import org.jmol.console.JmolButton;
import org.jmol.console.JmolToggleButton;
import org.jmol.console.KeyJMenu;
import org.jmol.dialog.Dialog;
import org.jmol.i18n.GT;
import org.jmol.script.T;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.HistoryFile;
import org.openscience.jmol.app.Jmol;
import org.openscience.jmol.app.JmolApp;
import org.openscience.jmol.app.JmolPlugin;
import org.openscience.jmol.app.SplashInterface;
import org.openscience.jmol.app.jmolpanel.console.AppConsole;
import org.openscience.jmol.app.jmolpanel.console.ConsoleTextArea;
import org.openscience.jmol.app.jsonkiosk.BannerFrame;
import org.openscience.jmol.app.jsonkiosk.JsonNioClient;
import org.openscience.jmol.app.jsonkiosk.JsonNioServer;
import org.openscience.jmol.app.jsonkiosk.JsonNioService;
import org.openscience.jmol.app.jsonkiosk.KioskFrame;
import org.openscience.jmol.app.surfacetool.SurfaceTool;
import org.openscience.jmol.app.webexport.WebExport;

import javajs.async.AsyncFileChooser;
import javajs.util.JSJSONParser;
import javajs.util.PT;
import jme.JMEJmol;

public class JmolPanel extends JPanel implements SplashInterface, JsonNioClient {

  public static HistoryFile historyFile, pluginFile;

  private static final boolean addPreferencesDialog = !Viewer.isSwingJS;
  private static final boolean addMacrosMenu = !Viewer.isSwingJS;
  private static final boolean allowRecentFiles = !Viewer.isSwingJS;
  private static final boolean addAtomChooser = !Viewer.isSwingJS;
  private static final boolean allowPreferences = !Viewer.isSwingJS;
  private static final boolean allowGaussian = !Viewer.isSwingJS;

  public Viewer vwr;

  protected JmolAdapter modelAdapter;
  public JmolApp jmolApp;
  protected StatusBar status;
  protected int startupWidth, startupHeight;
  private JsonNioServer serverService;

  // Called by NBODialog

  protected String appletContext;
  public PropertyChangeSupport pcs = new PropertyChangeSupport(this);
  public DisplayPanel display;
  protected GaussianDialog gaussianDialog; // not in SwingJS
  protected RecentFilesDialog recentFiles; // not in SwingJS
  public AtomSetChooser atomSetChooser;
  public JFrame frame;
  public SplashInterface splash;
  protected JFrame consoleframe;
  protected JsonNioServer clientService;
  protected int qualityJPG = -1;
  protected int qualityPNG = -1;
  protected String imageType;

  public GuiMap guimap;
  protected ExecuteScriptAction executeScriptAction;
  protected PreferencesDialog preferencesDialog;
  protected StatusListener myStatusListener;
  protected SurfaceTool surfaceTool;
  protected MeasurementTable measurementTable;
  protected JMEJmol jmolJME;

  protected Map<String, Action> commands;
  protected Map<String, JMenuItem> menuItems;
  protected JToolBar toolbar;

  // --- action implementations -----------------------------------

  protected TwoDEditorAction twoDEditorAction = new TwoDEditorAction();
  protected ExportAction exportAction = new ExportAction();
  protected PovrayAction povrayAction = new PovrayAction();
  protected ToWebAction toWebAction = new ToWebAction();
  protected WriteAction writeAction = new WriteAction();
  protected PrintAction printAction = new PrintAction();
  protected CopyImageAction copyImageAction = new CopyImageAction();
  protected CopyScriptAction copyScriptAction = new CopyScriptAction();
  protected SurfaceToolAction surfaceToolAction = new SurfaceToolAction();
  protected PasteClipboardAction pasteClipboardAction = new PasteClipboardAction();
  protected ViewMeasurementTableAction viewMeasurementTableAction = new ViewMeasurementTableAction();

  protected Map<String, Object> vwrOptions;

  protected String windowName;

  protected boolean isPlugin;

  private JMenuBar menubar;

  protected static int numWindows = 0;
  protected static KioskFrame kioskFrame;
  protected static BannerFrame bannerFrame;

  protected static boolean allowJavaConsole = true;

  // Window names for the history file
  protected final static String EDITOR_WINDOW_NAME = "ScriptEditor";
  protected final static String SCRIPT_WINDOW_NAME = "ScriptWindow";
  protected final static String FILE_OPEN_WINDOW_NAME = "FileOpen";
  protected final static String WEB_MAKER_WINDOW_NAME = "JmolWebPageMaker";
  protected final static String SURFACETOOL_WINDOW_NAME = "SurfaceToolWindow";
  protected final static Dimension screenSize = Toolkit.getDefaultToolkit()
      .getScreenSize();

  // these correlate with items xxx in GuiMap.java 
  // that have no associated xxxScript property listed
  // in org.openscience.jmol.Properties.Jmol-resources.properties

  protected static final String newwinAction = "newwin";
  protected static final String openAction = "open";
  protected static final String openurlAction = "openurl";
  protected static final String openpdbAction = "openpdb";
  protected static final String openmolAction = "openmol";
  protected static final String newAction = "new";
  protected static final String twoDEditorActionProperty = "twoDEditor";
  protected static final String exportActionProperty = "export";
  protected static final String closeAction = "close";
  protected static final String exitAction = "exit";
  protected static final String aboutAction = "about";
  protected static final String whatsnewAction = "whatsnew";
  protected static final String creditsAction = "credits";
  protected static final String uguideAction = "uguide";
  protected static final String printActionProperty = "print";
  protected static final String recentFilesAction = "recentFiles";
  protected static final String povrayActionProperty = "povray";
  protected static final String writeActionProperty = "write";
  protected static final String editorAction = "editor";
  protected static final String consoleAction = "console";
  protected static final String toWebActionProperty = "toweb";
  protected static final String atomsetchooserAction = "atomsetchooser";
  protected static final String copyImageActionProperty = "copyImage";
  protected static final String copyScriptActionProperty = "copyScript";
  protected static final String surfaceToolActionProperty = "surfaceTool";
  protected static final String pasteClipboardActionProperty = "pasteClipboard";
  protected static final String gaussianAction = "gauss";
  //  protected static final String nboAction = "nbo";
  protected static final String resizeAction = "resize";

  //protected static final String saveasAction = "saveas";
  //protected static final String vibAction = "vibrate";

  
  
  public JmolPanel(JmolApp jmolApp, Splash splash, JFrame frame,
      JmolPanel parent, int startupWidth, int startupHeight,
      Map<String, Object> vwrOptions, Point loc) {
    super(true);
    
    if (vwrOptions == null)
      vwrOptions = new Hashtable<String, Object>();
    this.vwrOptions = vwrOptions;
    this.splash = splash;
    this.jmolApp = jmolApp;
    this.frame = frame;
    if (frame != null && frame.getName() == null)
      frame.setName("JmolApp");
    this.startupWidth = startupWidth;
    this.startupHeight = startupHeight;
    historyFile = jmolApp.historyFile;
    pluginFile = jmolApp.pluginFile;
    numWindows++;

    try {
      if (historyFile != null)
        say("history file is " + historyFile.getFile().getAbsolutePath());
      if (jmolApp.userPropsFile != null)
        say("user properties file is "
            + jmolApp.userPropsFile.getAbsolutePath());
    } catch (Throwable e) {
      // ignore - no historyFile
    }

    setBorder(BorderFactory.createEtchedBorder());
    setLayout(new BorderLayout());
    windowName = getWindowName();
    guimap = createGuiMap();
    executeScriptAction = new ExecuteScriptAction();
    createDisplayAndAddStatusListener();
    setupModelAdapterAndViewer();
    getDialogs();
    getMeasurementTable();
    setCommandHooks();
    status = createStatusBar();
    toolbar = createToolBar();
    if (!jmolApp.haveDisplay)
      return;
    setupDisplay();
    setFrameLocation(loc, parent);
    setIntoFrame();
    setupConsole();
    setupDnD();
    setAtomChooser();
    launchMainFrame();
  }

  /**
   * @return A list of Actions that is understood by the upper level application
   */
  protected List<Action> getFrameActions() {

    Action[] defaultActions = { new NewAction(), new NewwinAction(),
        new OpenAction(), new OpenUrlAction(), new OpenPdbAction(),
        new OpenMolAction(), printAction, exportAction, new CloseAction(),
        new ExitAction(), copyImageAction, copyScriptAction,
        pasteClipboardAction, new AboutAction(), new WhatsNewAction(),
        new CreditsAction(), new UguideAction(), new ConsoleAction(),
        (allowRecentFiles ? new RecentFilesAction() : null), povrayAction,
        writeAction, toWebAction, new ScriptWindowAction(),
        new ScriptEditorAction(),
        (addAtomChooser ? new AtomSetChooserAction() : null),
        viewMeasurementTableAction,
        (allowGaussian ? new GaussianAction() : null), /*new NBOAction(),*/
        new ResizeAction(), surfaceToolAction, twoDEditorAction };

    List<Action> actions = new ArrayList<Action>();
    actions.addAll(Arrays.asList(defaultActions));
    display.addActions(actions);
    if (addPreferencesDialog)
      preferencesDialog.addActions(actions);
    return actions;
  }

  protected String getStringX(String cmd) {
    return JmolResourceHandler.getStringX(cmd);
  }

  protected ImageIcon getIconX(String img) {
    return JmolResourceHandler.getIconX(img);
  }

  protected String getWindowName() {
    return "Jmol";
  }

  protected GuiMap createGuiMap() {
    return new GuiMap();
  }

  protected StatusBar createStatusBar() {
    return new StatusBar(startupWidth);
  }

  /**
   * Create the toolbar. By default this reads the resource file for the
   * definition of the toolbars.
   * 
   * @return The toolbar
   */
  protected JToolBar createToolBar() {
    JToolBar toolbar = newToolbar(PT.getTokens(setMenuKeys("toolbar",
        JmolResourceHandler.getStringX("toolbar"))));
    toolbar
        .setPreferredSize(new Dimension(display.getPreferredSize().width, 25));
    //Action handler implementation would go here.
    toolbar.add(Box.createHorizontalGlue());
    return toolbar;
  }

  protected JToolBar newToolbar(String[] keys) {
    JToolBar toolbar = new JToolBar();
    for (int i = 0; i < keys.length; i++) {
      if (keys[i].equals("-")) {
        toolbar.addSeparator();
      } else {
        toolbar.add(createToolbarButton(keys[i]));
      }
    }
    return toolbar;
  }

  protected void createDisplayAndAddStatusListener() {
    say(GT.$("Initializing 3D display..."));
    display = new DisplayPanel(this);
    vwrOptions.put("display", display);
    JmolStatusListener userStatusListener = (JmolStatusListener) vwrOptions.get("statusListener");
    if (userStatusListener != null && userStatusListener.getClass() == StatusListener.class) {
      // don't pass on Jmol's standard listener as user's
      userStatusListener = null;
    }
    myStatusListener = new StatusListener(this, display);
    myStatusListener.userStatusListener = userStatusListener;
    vwrOptions.put("statusListener", myStatusListener);
  }

  protected void setupModelAdapterAndViewer() {
    if (JmolResourceHandler.codePath != null)
      vwrOptions.put("codePath", JmolResourceHandler.codePath);
    if (modelAdapter != null)
      vwrOptions.put("modelAdapter", modelAdapter);
    say(GT.$("Initializing 3D display...4"));
    vwr = new Viewer(vwrOptions);
    say(GT.$("Initializing 3D display...5"));

    display.setViewer(vwr);
    myStatusListener.setViewer(vwr);
  }

  protected void getDialogs() {
    if (allowPreferences) {
      say(GT.$("Initializing Preferences..."));
      preferencesDialog = new PreferencesDialog(this, frame, guimap, vwr);

    }
    if (allowRecentFiles) {
      say(GT.$("Initializing Recent Files..."));
      recentFiles = new RecentFilesDialog(frame);
    }
  }

  protected void getMeasurementTable() {
    if (jmolApp.haveDisplay) {
      if (measurementTable != null)
        measurementTable.dispose();
      measurementTable = new MeasurementTable(vwr, frame);
    }
  }

  protected void setCommandHooks() {
    if (display != null) {
      List<Action> actions = getFrameActions();
      // install the command table
      say(GT.$("Building Command Hooks..."));
      commands = new Hashtable<String, Action>();
      for (int i = 0; i < actions.size(); i++) {
        Action a = actions.get(i);
        if (a != null) // SwingJS
          commands.put(a.getValue(Action.NAME).toString(), a);
      }
    }
  }

  protected void setupDisplay() {
    if (jmolApp.isKiosk) {
      add("Center", display);
    } else {
      JPanel panel = new JPanel();
      menuItems = new Hashtable<String, JMenuItem>();
      say(GT.$("Building Menubar..."));
      menubar = createMenubar();
      add("North", menubar);
      panel.setLayout(new BorderLayout());
      if (toolbar != null)
        panel.add("North", toolbar);
      JPanel ip = new JPanel();
      ip.setLayout(new BorderLayout());
      ip.add("Center", display);
      panel.add("Center", ip);
      add("Center", panel);
      if (status != null)
        add("South", status);
    }
    say(GT.$("Starting display..."));
    display.start();

  }

  protected void setFrameLocation(Point loc, JmolPanel parent) {
    if (jmolApp.isKiosk) {
      bannerFrame = new BannerFrame(jmolApp.startupWidth, 75);
    } else {
      // prevent new Jmol from covering old Jmol
      if (loc == null) {
        if (parent != null) {
          loc = parent.frame.getLocationOnScreen();
          int maxX = screenSize.width - 50;
          int maxY = screenSize.height - 50;
          loc.x += 40;
          loc.y += 40;
          if (loc.x > maxX || loc.y > maxY)
            loc.setLocation(0, 0);
        } else if (historyFile == null || (loc = historyFile.getWindowPosition(windowName)) == null) {
          return;
        }
      }
      frame.setLocation(loc);
    }
  }

  protected void setIntoFrame() {
    frame.setTitle(windowName);
    frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    frame.setBackground(Color.lightGray);
    frame.setLayout(new BorderLayout());
    frame.add("Center", this);
    frame.pack();
    ImageIcon jmolIcon = JmolResourceHandler.getIconX("icon");
    Image iconImage = jmolIcon.getImage();
    frame.setIconImage(iconImage);
    if (!isPlugin)
      frame.addWindowListener(new AppCloser());
  }

  protected void setupConsole() {
    say(GT.$("Initializing Script Window..."));
    vwr.getProperty("DATA_API", "getAppConsole", Boolean.TRUE);
    AppConsole console = getConsole();
    if (console != null) {
      if (console.jcd != null && historyFile != null) {
        historyFile.repositionWindow(SCRIPT_WINDOW_NAME, console.jcd, 200, 100,
            !jmolApp.isKiosk);
      }
      // this just causes problems
      //c = (Component) vwr.getProperty("DATA_API","getScriptEditor", null);
      //if (c != null)
      //historyFile.repositionWindow(EDITOR_WINDOW_NAME, c, 150, 50);

      console.setStatusListener(myStatusListener);
    }
  }

  protected void setupDnD() {
    say(GT.$("Setting up Drag-and-Drop..."));
    new FileDropper(myStatusListener, vwr, null);
  }

  protected void setAtomChooser() {
    // it's important to set this up first, even though it consumes some memory
    // otherwise, loading a new model in a script that sets the vibration or vector parameters
    // can appear to skip those -- they aren't skipped, but creating the atomSetChooser
    // will run scripts as it loads.
    /**
     * sorry - no JTree yet
     * @j2sNative
     */
    {
    atomSetChooser = new AtomSetChooser(vwr, frame);
    pcs.addPropertyChangeListener(chemFileProperty, atomSetChooser);
    }
  }

  protected void launchMainFrame() {
    say(GT.$("Launching main frame..."));
  }

  @Deprecated
  protected static void getJavaConsole(Jmol jmol) {
    jmol.getJavaConsole();
  }

  public void getJavaConsole() {
    if (!jmolApp.haveConsole || !allowJavaConsole)
      return;
    // Adding console frame to grab System.out & System.err
    consoleframe = new JFrame(GT.$("Jmol Java Console"));
    consoleframe.setIconImage(frame.getIconImage());
    try {
      final ConsoleTextArea consoleTextArea = new ConsoleTextArea(true);
      consoleTextArea.setFont(Font.decode("monospaced"));
      consoleframe.getContentPane().add(new JScrollPane(consoleTextArea),
          java.awt.BorderLayout.CENTER);
      JButton buttonClear = guimap.newJButton("JavaConsole.Clear");
      buttonClear.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          consoleTextArea.setText("");
        }
      });
      consoleframe.getContentPane().add(buttonClear,
          java.awt.BorderLayout.SOUTH);
    } catch (IOException e) {
      JTextArea errorTextArea = new JTextArea();
      errorTextArea.setFont(Font.decode("monospaced"));
      consoleframe.getContentPane().add(new JScrollPane(errorTextArea),
          java.awt.BorderLayout.CENTER);
      errorTextArea.append(GT.$("Could not create ConsoleTextArea: ") + e);
    }

    Point location = frame.getLocation();
    Dimension size = frame.getSize();

    // String name = CONSOLE_WINDOW_NAME;     

    //Dimension consoleSize = historyFile.getWindowSize(name);
    //Point consolePosition = historyFile.getWindowPosition(name);
    //if (consoleSize != null && consolePosition != null) {
    //  location = consolePosition;
    //  size = consoleSize;
    //} else {
    location.y += size.height;
    size.height = 200;
    //}
    if (size.height < 200 || size.height > 800)
      size.height = 200;
    if (size.width < 300 || size.width > 800)
      size.width = 300;
    if (location.y < 0 || location.y + size.height > screenSize.height)
      location.y = screenSize.height - size.height;
    if (location.x < 0 || location.x + size.width > screenSize.width)
      location.x = 0;
    consoleframe.setBounds(location.x, location.y, size.width, size.height);

    //Boolean consoleVisible = historyFile.getWindowVisibility(name);
    //if ((consoleVisible != null) && (consoleVisible.equals(Boolean.TRUE))) {
    //consoleframe.setVisible(true);
    // }

  }

  @Override
  public void showStatus(String message) {
    splash.showStatus(message);
  }

  protected void report(String str) {
    if (jmolApp.isSilent)
      return;
    Logger.info(str);
  }

  protected void say(String message) {
    if (jmolApp.haveDisplay)
      if (splash == null) {
        report(message);
      } else {
        splash.showStatus(message);
      }
  }

  /**
   * To shutdown when run as an application. This is a fairly lame
   * implementation. A more self-respecting implementation would at least check
   * to see if a save was needed.
   */
  protected class AppCloser extends WindowAdapter {

    public AppCloser() {
    }

    @Override
    public void windowClosing(WindowEvent e) {
      doClose(false);
    }
  }

  protected boolean doClose(boolean saveSize) {
    if (numWindows == 1
        && vwr.ms.ac > 0
        && JOptionPane.showConfirmDialog(frame, GT.$("Exit Jmol?"), "Exit",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION)
      return false;
    dispose(frame, saveSize);
    return true;
  }

  Map<String, JmolPlugin> plugins = new Hashtable<String, JmolPlugin>();

  void dispose(JFrame f, boolean saveSize) {
    // Save window positions and status in the history
    if (jmolJME != null)
      jmolJME.dispose();
    if (webExport != null)
      WebExport.cleanUp();
    if (saveSize)
      saveWindowSizes();
    if (clientService != null) {
      clientService.close();
      clientService = null;
    }
    if (serverService != null) {
      serverService.close();
      serverService = null;
    }

    for (Entry<String, JmolPlugin> e : plugins.entrySet()) {
      try {
        e.getValue().destroy();
      } catch (Throwable err) {
        // ignore
      }
    }
    plugins.clear();

    if (numWindows <= 1) {
      // Close Jmol
      report(GT.$("Closing Jmol..."));
      // pluginManager.closePlugins();
      System.exit(0);
    } else {
      numWindows--;
      vwr.dispose();
      try {
        f.dispose();
      } catch (Exception e) {
        Logger.error("frame disposal exception");
        // ignore
      }
    }
  }

  protected void saveWindowSizes() {
    if (historyFile == null)
      return;
    if (frame != null) {
      historyFile.addWindowInfo(windowName, frame, null, display.dimSize);
    }
    AppConsole console = getConsole();
    if (console != null && console.jcd != null)
      historyFile.addWindowInfo(SCRIPT_WINDOW_NAME, console.jcd, null);
    Component c = (Component) vwr.getProperty("DATA_API", "getScriptEditor",
        null);
    if (c != null)
      historyFile.addWindowInfo(EDITOR_WINDOW_NAME, c, null);
    if (historyFile.getProperty("clearHistory", "false").equals("true"))
      historyFile.clear();
  }

  /**
   * This is the hook through which all menu items are created. It registers the
   * result with the menuitem hashtable so that it can be fetched with
   * getMenuItem().
   * 
   * @param cmd
   * @return Menu item created
   * @see #getMenuItem
   */
  public JMenuItem createMenuItem(String cmd) {

    JMenuItem mi;
    if (cmd.endsWith("Check")) {
      mi = guimap.newJCheckBoxMenuItem(cmd, false);
    } else {
      mi = guimap.newJMenuItem(cmd);
    }

    ImageIcon f = getIconX(cmd + "Image");
    if (f != null) {
      mi.setHorizontalTextPosition(SwingConstants.RIGHT);
      mi.setIcon(f);
    }

    if (cmd.endsWith("Script")) {
      mi.setActionCommand(getStringX(cmd));
      mi.addActionListener(executeScriptAction);
    } else {
      mi.setActionCommand(cmd);
      Action a = getAction(cmd);
      if (a != null) {
        mi.addActionListener(a);
        a.addPropertyChangeListener(new ActionChangedListener(mi));
        mi.setEnabled(a.isEnabled());
      } else {
        mi.setEnabled(false);
      }
    }
    menuItems.put(cmd, mi);
    return mi;
  }

  /**
   * Fetch the menu item that was created for the given command.
   * 
   * @param cmd
   *        Name of the action.
   * @return item created for the given command or null if one wasn't created.
   */
  protected JMenuItem getMenuItem(String cmd) {
    return menuItems.get(cmd);
  }

  /**
   * Fetch the action that was created for the given command.
   * 
   * @param cmd
   *        Name of the action.
   * @return The action
   */
  protected Action getAction(String cmd) {
    return commands.get(cmd);
  }

  /**
   * Create a button to go inside of the toolbar. By default this will load an
   * image resource. The image filename is relative to the classpath (including
   * the '.' directory if its a part of the classpath), and may either be in a
   * JAR file or a separate file.
   * 
   * @param key
   *        The key in the resource file to serve as the basis of lookups.
   * @return Button
   */
  protected AbstractButton createToolbarButton(String key) {

    ImageIcon ii = JmolResourceHandler.getIconX(key + "Image");
    boolean isHoldButton = (key.startsWith("animatePrev") || key
        .startsWith("animateNext"));
    AbstractButton b = (isHoldButton ? new AnimButton(ii,
        JmolResourceHandler.getStringX(key)) : new JmolButton(ii));
    String isToggleString = JmolResourceHandler.getStringX(key + "Toggle");
    if (isToggleString != null) {
      boolean isToggle = Boolean.valueOf(isToggleString).booleanValue();
      if (isToggle) {
        b = new JmolToggleButton(ii);
        if (key.equals("rotateScript")) {
          display.buttonRotate = b;
        }
        if (key.equals("modelkitScript")) {
          display.buttonModelkit = b;
        }
        display.toolbarButtonGroup.add(b);
        String isSelectedString = JmolResourceHandler.getStringX(key
            + "ToggleSelected");
        if (isSelectedString != null) {
          boolean isSelected = Boolean.valueOf(isSelectedString).booleanValue();
          b.setSelected(isSelected);
        }
      }
    }
    b.setRequestFocusEnabled(false);
    b.setMargin(new Insets(1, 1, 1, 1));

    Action a = null;
    String actionCommand = null;
    if (isHoldButton) {

    } else if (key.endsWith("Script")) {
      actionCommand = JmolResourceHandler.getStringX(key);
      a = executeScriptAction;
    } else {
      actionCommand = key;
      a = getAction(key);
    }
    if (a != null) {
      b.setActionCommand(actionCommand);
      b.addActionListener(a);
      a.addPropertyChangeListener(new ActionChangedListener(b));
      b.setEnabled(a.isEnabled());
    } else {
      b.setEnabled(isHoldButton);
    }
    //System.out.println("JP key cmd " + key + " " + actionCommand + " " + b.isEnabled());
    String tip = guimap.getLabel(key + "Tip");
    if (tip != null) {
      guimap.map.put(key + "Tip", b);
      b.setToolTipText(tip);
    }

    return b;
  }

  /**
   * Create the menubar for the app. By default this pulls the definition of the
   * menu from the associated resource file.
   * 
   * @return Menubar
   */
  protected JMenuBar createMenubar() {
    JMenuBar mb = new JMenuBar();
    addNormalMenuBar(mb);
    addPluginMenu(mb);
    if (addMacrosMenu)
      addMacrosMenu(mb);
    // The Plugin Menu
    // if (pluginManager != null) {
    //     mb.add(pluginManager.getMenu());
    // }
    // The Help menu, right aligned
    //mb.add(Box.createHorizontalGlue());
    addHelpMenuBar(mb);
    return mb;
  }

  JMenu pluginMenu;

  protected void addPluginMenu(JMenuBar mb) {
    pluginMenu = guimap.newJMenu("plugins");
    mb.add(pluginMenu);
    boolean isOK = false;
    try {
      PropertyResourceBundle bundle = new PropertyResourceBundle(
          getClass().getResourceAsStream(
              "/org/openscience/jmol/app/plugins/plugin.properties"));
      isOK = bundle.getKeys().hasMoreElements();
    } catch (IOException e1) {
      // ignore
    }
    if (isOK) {
      pluginMenu.addMenuListener(new MenuListener() {

        @Override
        public void menuSelected(MenuEvent e) {
          setPlugins();
        }

        @Override
        public void menuDeselected(MenuEvent e) {
          // TODO

        }

        @Override
        public void menuCanceled(MenuEvent e) {
          // TODO

        }

      });
    }
    pluginMenu.setEnabled(isOK);
  }

  boolean pluginsSet = false;
  
  void setPlugins() {
    if (pluginsSet)
      return;
    pluginsSet = true;
    try {
      PropertyResourceBundle bundle = new PropertyResourceBundle(getClass()
          .getResourceAsStream(
              "/org/openscience/jmol/app/plugins/plugin.properties"));
      Enumeration<String> keys = bundle.getKeys();
      while (keys.hasMoreElements()) {
        final String key = keys.nextElement();
        JmolPlugin p = plugins.get(key);
        if (p != null)
          continue;
        String path = bundle.getString(key);
        if (path == null | path.length() == 0 || path.indexOf("disabled") >= 0)
          continue;
        try {
          p = getAndRegisterPlugin(key, path);
          if (p != null) {
            String text = p.getMenuText();
            ImageIcon icon = p.getMenuIcon();
            if (text == null)
              text = key;
            JMenuItem item = new JMenuItem(text);
            if (icon != null) {
              item.setHorizontalTextPosition(SwingConstants.RIGHT);
              item.setIcon(icon);
            }
            pluginMenu.add(item);
            item.addActionListener(new ActionListener() {

              @Override
              public void actionPerformed(ActionEvent e) {
                showPlugin(key, null, null);
              }

            });
          }
        } catch (Exception e) {
          System.out.println("Cannot create plugin " + key + " " + path);
        }
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex.toString());
    }
    pluginMenu.setEnabled(pluginMenu.getPopupMenu().getComponentCount() > 0);
  }
  protected void addMacrosMenu(JMenuBar menuBar) {
    // ok, here needs to be added the funny stuff
    JMenu macroMenu = guimap.newJMenu("macros");
    File macroDir = new File(System.getProperty("user.home")
        + System.getProperty("file.separator") + ".jmol"
        + System.getProperty("file.separator") + "macros");
    report("User macros dir: " + macroDir);
    report("       exists: " + macroDir.exists());
    report("  isDirectory: " + macroDir.isDirectory());
    if (macroDir.exists() && macroDir.isDirectory()) {
      File[] macros = macroDir.listFiles();
      for (int i = 0; i < macros.length; i++) {
        // loop over these files and load them
        String macroName = macros[i].getName();
        if (macroName.endsWith(".macro")) {
          if (Logger.debugging) {
            Logger.debug("Possible macro found: " + macroName);
          }
          FileInputStream macro = null;
          try {
            macro = new FileInputStream(macros[i]);
            Properties macroProps = new Properties();
            macroProps.load(macro);
            String macroTitle = macroProps.getProperty("Title");
            String macroScript = macroProps.getProperty("Script");
            JMenuItem mi = new JMenuItem(macroTitle);
            mi.setActionCommand(macroScript);
            mi.addActionListener(executeScriptAction);
            macroMenu.add(mi);
          } catch (IOException exception) {
            System.err.println("Could not load macro file: ");
            System.err.println(exception);
          } finally {
            if (macro != null) {
              try {
                macro.close();
              } catch (IOException e) {
                // Nothing
              }
            }
          }
        }
      }
    }
    menuBar.add(macroMenu);
  }

  protected void addNormalMenuBar(JMenuBar menuBar) {
    String[] menuKeys = PT.getTokens(setMenuKeys("menubar",
        JmolResourceHandler.getStringX("menubar")));
    for (int i = 0; i < menuKeys.length; i++) {
      if (menuKeys[i].equals("-")) {
        menuBar.add(Box.createHorizontalGlue());
      } else {
        JMenu m = createMenu(menuKeys[i]);
        if (m != null)
          menuBar.add(m);
      }
    }
  }

  protected void addHelpMenuBar(JMenuBar menuBar) {
    JMenu m = createMenu("help");
    if (m != null) {
      menuBar.add(m);
    }
  }

  /**
   * Create a menu for the app. By default this pulls the definition of the menu
   * from the associated resource file.
   * 
   * @param key
   * @return Menu created
   */
  public JMenu createMenu(String key) {

    // Get list of items from resource file:
    String[] itemKeys = PT.getTokens(setMenuKeys(key,
        JmolResourceHandler.getStringX(key)));
    // Get label associated with this menu:
    JMenu menu = guimap.newJMenu(key);
    ImageIcon f = JmolResourceHandler.getIconX(key + "Image");
    if (f != null) {
      menu.setHorizontalTextPosition(SwingConstants.RIGHT);
      menu.setIcon(f);
    }
    KeyJMenu jmenu = (KeyJMenu) menu;
    jmenu.itemKeys = itemKeys;
    menu.addMenuListener(new MenuListener() {
      @Override
      public void menuSelected(MenuEvent e) {
        jmenu.createItemKeys(JmolPanel.this);
        switch (key) {
        case "display":
          guimap.setSelected("perspectiveCheck", vwr.tm.perspectiveDepth);
          guimap.setSelected("hydrogensCheck", vwr.getBoolean(T.showhydrogens));
          guimap.setSelected("measurementsCheck",
              vwr.getBoolean(T.showmeasurements));
          guimap.setSelected("axesCheck", vwr.getShowAxes());
          guimap.setSelected("boundboxCheck", vwr.getShowBbcage());
          break;
        case "spectrumMenu":
          guimap.setEnabled("openJSpecViewScript", !vwr.getBoolean(T.pdb));
          guimap.setEnabled("simulate1HSpectrumScript", !vwr.getBoolean(T.pdb));
          guimap.setEnabled("simulate13CSpectrumScript",
              !vwr.getBoolean(T.pdb));
          break;
        }
      }

      @Override
      public void menuDeselected(MenuEvent e) {
      }

      @Override
      public void menuCanceled(MenuEvent e) {
      }
    });
    return menu;
  }

  /**
   * opportunity to adjust menus for a custom application
   * 
   * @param key
   * @param tokens
   * @return possibly changed token string
   */
  protected String setMenuKeys(String key, String tokens) {
    return tokens;
  }

  protected static class ActionChangedListener implements
      PropertyChangeListener {

    AbstractButton button;

    public ActionChangedListener(AbstractButton button) {
      super();
      this.button = button;
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {

      String propertyName = e.getPropertyName();
      if (e.getPropertyName().equals(Action.NAME)) {
        String text = (String) e.getNewValue();
        if (button.getText() != null) {
          button.setText(text);
        }
      } else if (propertyName.equals("enabled")) {
        Boolean enabledState = (Boolean) e.getNewValue();
        button.setEnabled(enabledState.booleanValue());
      }
    }
  }

  public class CloseAction extends AbstractAction {
    public CloseAction() {
      super(closeAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (!doClose(true)) {
        vwr.script("zap");
      }
    }
  }

  public class ConsoleAction extends AbstractAction {

    public ConsoleAction() {
      super("jconsole");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (consoleframe != null)
        consoleframe.setVisible(true);
    }

  }

  public class AboutAction extends AbstractAction {

    public AboutAction() {
      super(aboutAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      try {
        AboutDialog ad = new AboutDialog(frame, vwr);
        ad.setVisible(true);
      } catch (Exception ee) {
        Logger.error(ee.getMessage());
      }
    }

  }

  public class WhatsNewAction extends AbstractAction {

    public WhatsNewAction() {
      super(whatsnewAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      WhatsNewDialog wnd = new WhatsNewDialog(frame);
      wnd.setVisible(true);
    }
  }

  public class CreditsAction extends AbstractAction {

    public CreditsAction() {
      super(creditsAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      (new CreditsDialog(frame)).setVisible(true);
    }
  }

  public class GaussianAction extends AbstractAction {
    public GaussianAction() {
      super(gaussianAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (gaussianDialog == null)
        gaussianDialog = new GaussianDialog(frame, vwr);
      gaussianDialog.setVisible(true);
    }
  }

  //  public class NBOAction extends AbstractAction {
  //    public NBOAction() {
  //      super(nboAction);
  //    }
  //
  //    @Override
  //    public void actionPerformed(ActionEvent arg0) {
  //      startNBO(null);
  //    }
  //  }

  public class NewwinAction extends AbstractAction {

    public NewwinAction() {
      super(newwinAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      doNew();
    }

  }

  void doNew() {
    JFrame newFrame = new JFrame();
    new Jmol(jmolApp, null, newFrame, (Jmol) JmolPanel.this, startupWidth,
        startupHeight, vwrOptions, null);
    newFrame.setVisible(true);
  }

  /**
   * @param item
   */
  void setMenuNBO(JMenu item) {
    // no longer used - causes delay in hovering over NBO menu item
    //    Component[] nodes = item.getMenuComponents();
    //    
    //    for (int i = nodes.length; --i >= 0;) {
    //      String text = ((JMenuItem) nodes[i]).getText();
    //      nodes[i].setEnabled(text.equals("Config"));
    //    }
    //    getNBOService();
    //    if (!nboService.restartIfNecessary()) {
    //      return;
    //    }
    //    if (nboDialog == null)
    //      nboDialog = new NBODialog(frame, vwr, nboService);
    //    // individual nodes here
    //    nodes[1].setEnabled(true); // model
    //    nodes[2].setEnabled(true);//vwr.ms.at.length > 0); // run
    //    //boolean viewOK = "gennbo".equals(vwr.ms.getInfo(vwr.am.cmi, "fileType"));
    //    nodes[3].setEnabled(true); // view    
    //    nodes[4].setEnabled(true); // search
  }

  /**
   * @param jmolOptions
   *        e.g. NOZAP;VIEWER unused
   */
  void startNBO(Map<String, Object> jmolOptions) {

    showPlugin("NBO", "org.gennbo.NBOPlugin", jmolOptions);
  }

  void showPlugin(String name, String path, Map<String, Object> jmolOptions) {
    try {
      JmolPlugin p = getAndRegisterPlugin(name, path);
      if (!p.isStarted())
        p.start(frame, vwr, jmolOptions);
      p.setVisible(true);
    } catch (Throwable e) {
      System.out.println("Error creating plugin " + name);
      e.printStackTrace();
    }
  }

  protected JmolPlugin getAndRegisterPlugin(String name, String path) {
    JmolPlugin p = plugins.get(name);
    if (p == null) {
      plugins.put(name,
          p = (JmolPlugin) Interface.getInterface(path, vwr, "plugin"));
    }
    return p;
  }

  public static Object getInstanceWithParams(String name, Class<?>[] classes,
                                             Object... params) {
    try {
      Class<?> cl = Class.forName(name);
      return cl.getConstructor(classes).newInstance(params);
    } catch (Exception e) {
      return null;
    }
  }

  public class UguideAction extends AbstractAction {

    public UguideAction() {
      super(uguideAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      vwr.showUrl(JmolResourceHandler.getStringX("UGuide.wikiURL"));
      //showHelp(new HelpDialog(frame)).setVisible(true);
    }
  }

  public class PasteClipboardAction extends AbstractAction {

    public PasteClipboardAction() {
      super(pasteClipboardActionProperty);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      vwr.loadInlineAppend(vwr.getClipboardText(), false);
    }
  }

  /**
   * An Action to copy the current image into the clipboard.
   */
  public class CopyImageAction extends AbstractAction {

    public CopyImageAction() {
      super(copyImageActionProperty);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      vwr.clipImageOrPasteText(null);
    }
  }

  public class CopyScriptAction extends AbstractAction {

    public CopyScriptAction() {
      super(copyScriptActionProperty);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      vwr.clipImageOrPasteText((String) vwr.getProperty("string", "stateInfo",
          null));
    }
  }

  public class PrintAction extends AbstractAction {

    public PrintAction() {
      super(printActionProperty);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      print();
    }

  }

  /**
   * added print command, so that it can be used by RasmolScriptHandler
   **/
  public void print() {

    PrinterJob job = PrinterJob.getPrinterJob();
    job.setPrintable(display);
    if (job.printDialog()) {
      try {
        job.print();
      } catch (PrinterException e) {
        Logger.errorEx("Error while printing", e);
      }
    }
  }

  public class OpenAction extends NewAction {

    public OpenAction() {
      super(openAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      openFile();
    }
  }

  public class OpenUrlAction extends NewAction {

    String title;
    String prompt;

    public OpenUrlAction() {
      super(openurlAction);
      title = GT.$("Open URL");
      prompt = GT.$("Enter URL of molecular model");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      String url = JOptionPane.showInputDialog(frame, prompt, title,
          JOptionPane.PLAIN_MESSAGE);
      if (url != null) {
        if (url.indexOf("://") < 0) {
          if (url.length() == 4 && url.indexOf(".") < 0)
            url = "=" + url;
          if (!url.startsWith("="))
            url = "https://" + url;
        }
        vwr.openFileAsync(url);
      }
    }
  }

  public class OpenPdbAction extends NewAction {

    public OpenPdbAction() {
      super(openpdbAction);
      script = JC.getMenuScript("openPDB");
    }
  }

  public class OpenMolAction extends NewAction {

    public OpenMolAction() {
      super(openmolAction);
      script = JC.getMenuScript("openMOL");
    }

  }

  public class NewAction extends AbstractAction {

    protected String script;

    public NewAction() {
      super(newAction);
    }

    public NewAction(String nm) {
      super(nm);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (script == null)
        revalidate();
      else
        vwr.script(script);
    }
  }

  public class ExitAction extends AbstractAction {

    public ExitAction() {
      super(exitAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      saveWindowSizes();
      System.exit(0);
    }
  }

  public class TwoDEditorAction extends AbstractAction {

    public TwoDEditorAction() {
      super(twoDEditorActionProperty);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (jmolJME == null) {
        jmolJME = new JMEJmol();
        jmolJME.setViewer(null, vwr, getTopLevelAncestor(), "jmol");
      }
      jmolJME.setFrameVisible(true);
      jmolJME.from3D();
    }

  }


  final static String[] imageChoices = { "JPEG", "PNG", "GIF", "PPM", "PDF" };
  final static String[] imageExtensions = { "jpg", "png", "gif", "ppm", "pdf" };

  public class ExportAction extends AbstractAction {

    public ExportAction() {
      super(exportActionProperty);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

      Dialog sd = new Dialog();
      String fileName = sd.getImageFileNameFromDialog(vwr, null, imageType,
          imageChoices, imageExtensions, qualityJPG, qualityPNG);
      if (fileName == null)
        return;
      qualityJPG = sd.getQuality("JPG");
      qualityPNG = sd.getQuality("PNG");
      String sType = imageType = sd.getType();
      if (sType == null) {
        // file type changer was not touched
        sType = fileName;
        int i = sType.lastIndexOf(".");
        if (i < 0)
          return; // make no assumptions - require a type by extension
        sType = sType.substring(i + 1).toUpperCase();
      }
      if (fileName.indexOf(".") < 0)
        fileName += "."
            + (sType.equalsIgnoreCase("JPEG") ? "jpg" : sType.toLowerCase());
      Map<String, Object> params = new Hashtable<String, Object>();
      params.put("fileName", fileName);
      params.put("type", sType);
      params.put("quality", Integer.valueOf(sd.getQuality(sType)));
      String msg = vwr.outputToFile(params);
      Logger.info(msg);
    }

  }

  public class RecentFilesAction extends AbstractAction {

    public RecentFilesAction() {
      super(recentFilesAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      /**
       * @j2sNative
       * 
       */
      {
        recentFiles.setVisible(true);
        String selection = recentFiles.getFile();
        if (selection == null || selection.length() == 0)
          return;
        if (selection.endsWith(" (*)"))
          vwr.openFileAsyncSpecial(
              selection.substring(0, selection.length() - 4), JmolScriptManager.NO_AUTOPLAY | JmolScriptManager.PDB_CARTOONS);
        else
          vwr.openFileAsyncSpecial(selection, JmolScriptManager.NO_AUTOPLAY);
      }
    }
  }

  public class ScriptWindowAction extends AbstractAction {

    public ScriptWindowAction() {
      super(consoleAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      AppConsole console = getConsole();
      if (console != null) {
        console.setVisible(true);
      }
    }
  }

  public class ScriptEditorAction extends AbstractAction {

    public ScriptEditorAction() {
      super(editorAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      Component c = (Component) vwr.getProperty("DATA_API", "getScriptEditor",
          null);
      if (c != null)
        c.setVisible(true);
    }
  }

  public class AtomSetChooserAction extends AbstractAction {
    public AtomSetChooserAction() {
      super(atomsetchooserAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      atomSetChooser.setVisible(true);
    }
  }

  public class PovrayAction extends AbstractAction {

    public PovrayAction() {
      super(povrayActionProperty);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      new PovrayDialog(frame, vwr);
    }

  }

  public class WriteAction extends AbstractAction {

    public WriteAction() {
      super(writeActionProperty);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      String fileName = (new Dialog()).getSaveFileNameFromDialog(vwr, null,
          "SPT");
      if (fileName != null) {
        Map<String, Object> params = new Hashtable<String, Object>();
        params.put("fileName", fileName);
        params.put("type", "SPT");
        params.put("text", vwr.getStateInfo());
        String msg = vwr.outputToFile(params);
        Logger.info(msg);
      }
    }
  }

  /**
   * 
   * Starting with Jmol 11.8.RC5, this is just informational if type == null and
   * null is returned, then it means "Jmol, you handle it"
   * 
   * @param fileName
   * @param type
   * @param text_or_bytes
   * @param quality
   * @return null (you do it) or a message starting with OK or an error message
   */
  String createImageStatus(String fileName, String type, Object text_or_bytes,
                           int quality) {
    if (fileName != null && text_or_bytes != null)
      return null; // "Jmol, you do it."
    String msg = fileName;
    if (msg != null && !msg.startsWith("OK") && status != null) {
      setStatus(StatusBar.STATUS_COORD, GT.$("IO Exception:"));
      setStatus(StatusBar.STATUS_TEXT, msg);
    }
    return msg;
  }

  WebExport webExport;

  private TouchHandler touchHandler;

  void createWebExport() {
    webExport = WebExport.createAndShowGUI(vwr, historyFile,
        WEB_MAKER_WINDOW_NAME);
  }

  public class ToWebAction extends AbstractAction {

    public ToWebAction() {
      super(toWebActionProperty);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          createWebExport();
        }
      });
    }
  }

  public class ViewMeasurementTableAction extends AbstractAction {

    public ViewMeasurementTableAction() {
      super("viewMeasurementTable");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      measurementTable.activate();
    }
  }

  void createSurfaceTool() {
    //TODO check to see if it already exists, if so bring to front.
    if (surfaceTool != null) {
      surfaceTool.toFront();
    } else {
      surfaceTool = new SurfaceTool(vwr, historyFile, SURFACETOOL_WINDOW_NAME,
          true);
    }
  }

  public class SurfaceToolAction extends AbstractAction {

    public SurfaceToolAction() {
      super(surfaceToolActionProperty);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          createSurfaceTool();
        }
      });
    }
  }

  /**
   * Returns a new File referenced by the property 'user.dir', or null if the
   * property is not defined.
   * 
   * @return a File to the user directory
   */
  public static File getUserDirectory() {
    String dir = System.getProperty("user.dir");
    return dir == null ? null : new File(System.getProperty("user.dir"));
  }

  void openFile() {
    int flags0 = JmolScriptManager.NO_AUTOPLAY | JmolScriptManager.PDB_CARTOONS; // cartoons+fileOpen
    if (Viewer.isJS) {
      AsyncFileChooser chooser = new AsyncFileChooser();
      chooser.showOpenDialog(frame, new Runnable() {

        @Override
        public void run() {
          vwr.openFileAsyncSpecial(chooser.getSelectedFile().toString(), flags0);
        }
      }, null);
      
    } else {
      String fileName = (new Dialog()).getOpenFileNameFromDialog(vwrOptions,
          vwr, null, jmolApp, FILE_OPEN_WINDOW_NAME, true);
      if (fileName == null)
        return;
      int flags = flags0;
      if (fileName.startsWith("#NOCARTOONS#;")) {
        flags &= ~JmolScriptManager.PDB_CARTOONS;
        fileName = fileName.substring(13);
      }
      if (fileName.startsWith("#APPEND#;")) {
        fileName = fileName.substring(9);
        flags |= JmolScriptManager.IS_APPEND;
      }
      vwr.openFileAsyncSpecial(fileName, flags);
    }
  }

  static final String chemFileProperty = "chemFile";

  void notifyFileOpen(String fullPathName, String title) {
    if (fullPathName == null || !fullPathName.equals("String[]")) {
      int pt = (fullPathName == null ? -1 : fullPathName.lastIndexOf("|"));
      if (pt > 0)
        fullPathName = fullPathName.substring(0, pt);
      if (recentFiles != null)
        recentFiles.notifyFileOpen(fullPathName);
      if (title.indexOf(JC.ADD_HYDROGEN_TITLE) >= 0)
        title = "<modelkit>";
      frame.setTitle(title);
    }
    if (atomSetChooser == null && addAtomChooser) {
      atomSetChooser = new AtomSetChooser(vwr, frame);
      pcs.addPropertyChangeListener(chemFileProperty, atomSetChooser);
    }
    pcs.firePropertyChange(chemFileProperty, null, null);
  }

  public class ExecuteScriptAction extends AbstractAction {
    public ExecuteScriptAction() {
      super("executeScriptAction");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      String script = e.getActionCommand();
      if (script.indexOf("#showMeasurementTable") >= 0)
        measurementTable.activate();
      //      vwr.script("set picking measure distance;set pickingstyle measure");
      vwr.evalStringQuiet(script);
    }
  }

  public class ResizeAction extends AbstractAction {
    public ResizeAction() {
      super(resizeAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      resizeInnerPanel(null);
    }
  }

  int[] resizeInnerPanel(String data) {
    int width = vwr.getScreenWidth();
    int height = vwr.getScreenHeight();
    String info = width + " " + height;
    if (data == null) {
      data = info;
    } else {
      int pt = data.indexOf("preferredWidthHeight ");
      int pt2 = data.indexOf(";", pt + 1);
      if (pt >= 0 && pt2 > pt)
        data = data.substring(pt + 21, pt2).trim();
      if (data.equals(info))
        return new int[] { width, height };
    }
    info = JOptionPane.showInputDialog(GT.$("width height?"), data);
    if (info == null)
      return new int[] { width, height };
    double[] dims = new double[2];
    int n = Parser.parseStringInfestedDoubleArray(info.replace(',', ' '), null,
        dims);
    if (n < 2)
      return new int[] { width, height };
    resizeDisplay((int) dims[0], (int) dims[1]);
    return new int[] { (int) dims[0], (int) dims[1] };
  }

  void resizeDisplay(int width, int height) {
    Dimension d = new Dimension(width, height);
    //display.setSize(width, height);
    display.setJmolSize(d);
    d = new Dimension(width, 30);
    if (status != null)
      status.setPreferredSize(d);
    if (toolbar != null)
      toolbar.setPreferredSize(d);
    menubar.setPreferredSize(new Dimension(width, menubar.getHeight()));  
    Platform.getWindow(this).pack();
    d = new Dimension(width, height);
    System.out.println("resizeDisplay " + display.getSize(d));
  }

  void updateLabels() {
    if (atomSetChooser != null) {
      atomSetChooser.dispose();
      atomSetChooser = null;
    }
    if (gaussianDialog != null) {
      gaussianDialog.dispose();
      gaussianDialog = null;
    }

    boolean doTranslate = GT.setDoTranslate(true);
    getDialogs();
    getMeasurementTable();
    GT.setDoTranslate(doTranslate);
    guimap.updateLabels();
  }

  ////////// JSON/NIO SERVICE //////////

  @Override
  public void nioClosed(JsonNioServer jns) {
    if (bannerFrame != null) {
      vwr.scriptWait("delay 2");
      bannerFrame.dispose();
      vwr.dispose();
      // would not nec. have to close this....
      System.exit(0);
    }
    if (jns.equals(clientService))
      clientService = null;
    else if (jns.equals(serverService))
      serverService = null;

  }
  
  public final static int OUTSOCKET = JsonNioServer.OUTSOCKET;

  void sendNioSyncRequest(Object data, int port, String strInfo) {
    if (serverService == null && port == OUTSOCKET)
      return;
    try {
      if (port < 0) {
        // initialize server and possibly a client, if a test command is also given
        if (serverService != null && "STOP".equalsIgnoreCase(strInfo)) {
          serverService.close();
        } else if (serverService == null) {
          serverService = getJsonNioServer();
          if (serverService != null)
            serverService.startService(port, this, vwr, "-JmolNioServer", JsonNioService.VERSION);
        }
        if (serverService != null && serverService.getPort() == -port
            && strInfo != null) {
          if (clientService == null) {
            clientService = getJsonNioServer();
            if (clientService != null)
              clientService.startService(-port, this, vwr, "-JmolNioClient(self)",  JsonNioService.VERSION);
          }
          if (clientService != null)
            clientService.sendToJmol(-port, strInfo);
        }
        return;
      }        
      if ("STOP".equalsIgnoreCase(strInfo))
        strInfo = "{\"type\":\"quit\"}";
      if (clientService == null && serverService != null) {
        // just an internal test, before any client is started
        if (data != null) {
          serverService.reply(port, data);
        } else if (port == OUTSOCKET) {
           serverService.reply(port, strInfo);
        } else if (serverService.getPort() == port) {
          serverService.sendToJmol(port, strInfo);
        }
        return;
      }
      // sync 30000 ..... 
      if (clientService == null) {
        clientService = getJsonNioServer();
        if (clientService != null)
          clientService.startService(port, this, vwr, "-JmolNioClient",  JsonNioService.VERSION);
      }
      if (clientService != null) {
        if (data == null)
          clientService.sendToJmol(port, strInfo);
        else
          clientService.reply(port, data);
      }
    } catch (IOException e) {
      // TODO
    }
  }

  public static JsonNioServer getJsonNioServer() {
    return (JsonNioServer) Interface.getInterface(
        "org.openscience.jmol.app.jsonkiosk.JsonNioService", null, null);
  }

  public class AnimButton extends JmolButton implements MouseListener {

    protected String script;

    public AnimButton(ImageIcon ii, String script) {
      super(ii);
      this.script = script;
      addMouseListener(this);
    }

    protected long lastPressTime;

    @Override
    public void mousePressed(MouseEvent e) {
      vwr.evalStringQuiet(script);
      long t = System.currentTimeMillis();
      if (t - lastPressTime > jmolApp.autoAnimationDelay * 2000
          && jmolApp.autoAnimationDelay > 0) // 0.2 s
        vwr.evalStringQuiet("timeout '__animBtn' OFF;animation_running = true; delay "
            + jmolApp.autoAnimationDelay
            + "; if(animation_running){timeout '__animBtn' -200 \""
            + script
            + "\"}");
      lastPressTime = t;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      if (jmolApp.autoAnimationDelay > 0)
        vwr.evalStringQuiet("animation_running = false; timeout '__animBtn' OFF");
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

  }

  public AppConsole getConsole() {
    return   (AppConsole) vwr.getProperty("DATA_API", "getAppConsole", null);
  }
  
  public void updateConsoleFont() {
    AppConsole console = getConsole();
    if (console != null)
      console.updateFontSize();
  }

  public Object getPreference(String key) {
    return (preferencesDialog == null ? null
        : preferencesDialog.currentProperties.get(key));
  }

  public static String getJmolProperty(String key, String defaultValue) {
    return (historyFile == null ? defaultValue : historyFile.getProperty(key,
        defaultValue));
  }

  public static void setPluginOption(String pluginName, String key, String value) {
    if (pluginFile == null)
      return;
    pluginFile.addProperty(pluginName + "_" + key, value);
    pluginFile.save();
  }

  public static String getPluginOption(String pluginName, String key,
                                       String defaultValue) {
    return (pluginFile == null ? defaultValue : pluginFile.getProperty(
        pluginName + "_" + key, defaultValue));
  }

  public static void addJmolProperties(Properties props) {
    if (historyFile != null)
      historyFile.addProperties(props);
  }

  public static void addJmolProperty(String key, String value) {
    if (historyFile != null)
      historyFile.addProperty(key, value);
  }

  public void setStatus(int pos, String msg) {
    if (status != null)
      status.setStatus(pos, msg);
  }

  /**
   * Deprecated -- use addJmolWindowInnerInfo(name,window,Dimension inner)
   * 
   * @param name
   * @param window
   * @param border
   */
  @Deprecated
  public static void addJmolWindowInfo(String name, Component window,
                                       Point border) {
    if (historyFile != null)
      historyFile.addWindowInfo(name, window, border, null);

  }

  public static void addJmolWindowInnerInfo(String name, Component window,
                                            Dimension inner) {
    if (historyFile != null)
      historyFile.addWindowInnerInfo(name, window, inner);

  }

  @Override
  public synchronized void processNioMessage(byte[] packet) throws Exception {
    String msg = new String(packet);
    if (Logger.debugging)
      Logger.debug(msg);
    Map<String, Object> json = new JSJSONParser().parseMap(msg, false);
    switch ("" + json.get("type")) {
    case "reply":
      break;
    case "quit":
      vwr.evalString("exitjmol");
      break;
    case "command":
      vwr.evalString((String) json.get("command"));
      break;
    case "move":
      switch (JsonNioService.getString(json, "style")) {
      case "rotate":
        break;
      case "translate":
      case "zoom":
        //        if (!params.isPaused)
        //          pauseScript(true);
        break;
      }
      //$FALL-THROUGH$
    case "sync":
    case "touch":
      if (touchHandler == null)
        touchHandler = new TouchHandler();
      touchHandler.nioSync(vwr, json);
      break;
    }
  }

  @Override
  public void serverCycle() {
    if (touchHandler == null)
      return;
    touchHandler.checkPaused(vwr);
  }

  public boolean isServer() {
    return serverService != null;
  }


}