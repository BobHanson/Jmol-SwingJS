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
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.jmol.api.JSVInterface;

import javajs.util.Lst;
import javajs.util.SB;
import jspecview.api.JSVFileDropper;
import jspecview.api.JSVPanel;
import jspecview.api.JSVTreeNode;
import jspecview.api.PanelListener;
import jspecview.common.JSVFileManager;
import jspecview.common.JSViewer;
import jspecview.common.PanelNode;
import jspecview.common.PeakPickEvent;
import jspecview.common.ScriptToken;
import jspecview.common.SubSpecChangeEvent;
import jspecview.common.ZoomEvent;
import jspecview.export.Exporter;
import jspecview.source.JDXSource;

// BH 1/14/17 moves command checking to JSViewer

/**
 * The Main Class or Entry point of the JSpecView Application.
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class MainFrame extends JFrame
    implements PanelListener {

  public static void main(String args[]) {
    JSpecView.main(args);
  }

  // ------------------------ Program Properties -------------------------

  /**
   * 
   */
  private final static long serialVersionUID = 1L;
  private final static int MAX_RECENT = 10;

  private static final int JMOL_MIN_HEIGHT = 130;

  // ----------------------------------------------------------------------

  JSViewer vwr;
  private JSpecView jsv;

  ApplicationMenu appMenu;
  private AppToolBar toolBar;
  private JTextField commandInput = new JTextField();
  private BorderLayout mainborderLayout = new BorderLayout();
  private JSplitPane mainSplitPane = new JSplitPane();
  private final JPanel nullPanel = new JPanel();
  private JSplitPane sideSplitPane = new JSplitPane();

  //  public JSVTree spectraTree;
  //  public JDXSource              currentSource;
  //  public JmolList<JSVPanelNode> panelNodes;  
  //  public ColorParameters        parameters;
  //  public RepaintManager         repaintManager;
  //  public JSVPanel               selectedPanel;
  //  public JSVMainPanel           viewPanel; // alias for spectrumPanel

  private CommandHistory commandHistory;
  private DisplaySchemesProcessor dsp;
  private Component jmolDisplay;
  private Dimension jmolDimensionOld;
  private JPanel jmolPanel;
  private Dimension jmolDimensionNew = new Dimension(350, 300);
  private Lst<String> recentFilePaths = new Lst<String>();
  private JScrollPane spectraTreeScrollPane;
  private Component mainPanel;
  private JPanel statusPanel = new JPanel();
  private JLabel statusLabel = new JLabel();

  private AwtTree tree; // alias for spectraTree, because here it is visible

  private boolean sidePanelOn;
  private boolean showExitDialog;
  private boolean statusbarOn;
  //private boolean svgForInkscape = false;
  private boolean toolbarOn;

  private int mainSplitPosition = 200;
  private int splitPosition;
  private boolean isHidden;

  private String tempDS;

  ////////////////////// get/set methods

  /**
   * Constructor
   * 
   * @param jsv
   * @param jmolDisplay
   * 
   * @param jmolStatusListener
   */
  public MainFrame(JSpecView jsv, Component jmolDisplay,
      JSVInterface jmolStatusListener) {
    this.jsv = jsv;
    this.vwr = jsv.vwr;
    this.jmolDisplay = jmolDisplay;
    if (jmolDisplay != null) {
      jmolPanel = (JPanel) jmolDisplay.getParent();
    }
    jsv.setStatusListener(jmolStatusListener);
    init();
  }

  void exitJSpecView(boolean withDialog, boolean isClosing) {
    jsv.notifyExitingJspecView(isClosing, withDialog && showExitDialog);
  }

  private boolean isAwake;
  private int jmolFrameHeight, jmolFrameWidth;

  public void awaken(boolean visible) {
    if (!vwr.isEmbedded)
      return;
    System.out.println("MAINFRAME visible/awake" + visible + " " + isAwake + " "
        + jmolDisplay);
    if (isAwake == visible)
      return;
    jsv.vwr.setRecentSimulation(null);
    isAwake = visible;
    if (jmolDisplay != null)
      try {
        Container top = jmolPanel.getTopLevelAncestor();
        if (visible) {
          jmolDimensionOld = new Dimension(0, 0);
          jmolDisplay.getSize(jmolDimensionOld);
          jmolDisplay.setSize(jmolDimensionNew);
          jmolPanel.remove(jmolDisplay);
          if (top.getHeight() > JMOL_MIN_HEIGHT) {
            jmolFrameHeight = top.getHeight();
            jmolFrameWidth = top.getWidth();
            top.setSize(jmolFrameWidth, JMOL_MIN_HEIGHT);
          }
          jmolPanel.add(nullPanel);
          sideSplitPane.setBottomComponent(jmolDisplay);
          sideSplitPane.setDividerLocation(splitPosition);
          sideSplitPane.validate();
          jmolPanel.validate();
          System.out.println("awakened");
        } else {
          sideSplitPane.setBottomComponent(nullPanel);
          splitPosition = sideSplitPane.getDividerLocation();
          jmolPanel.add(jmolDisplay);
          if (top.getHeight() <= JMOL_MIN_HEIGHT) {
            top.setSize(jmolFrameWidth, jmolFrameHeight);
          }
          jmolDisplay.getSize(jmolDimensionNew);
          jmolDisplay.setSize(jmolDimensionOld);
          sideSplitPane.validate();
          jmolPanel.validate();
          System.out.println("sleeping");
        }
      } catch (Exception e) {
        // ignore
        e.printStackTrace();
      }
    setVisible(visible);
  }

  /**
   * Task to do when program starts
   */
  private void init() {
    // initialise MainFrame as a target for the drag-and-drop action
    DropTargetListener dtl = (DropTargetListener) vwr
        .getPlatformInterface("FileDropper");
    ((JSVFileDropper) dtl).set(vwr);
    new DropTarget(this, dtl);
    Class<? extends MainFrame> cl = getClass();
    URL iconURL = cl.getResource("icons/spec16.gif"); // imageIcon
    setIconImage(Toolkit.getDefaultToolkit().getImage(iconURL));
  
    setApplicationProperties(true);
    tempDS = jsv.defaultDisplaySchemeName;
    // initialise Spectra tree
    vwr.spectraTree = tree = new AwtTree(vwr);
    tree.setCellRenderer(new SpectraTreeCellRenderer());
    tree.putClientProperty("JTree.lineStyle", "Angled");
    tree.setShowsRootHandles(true);
    tree.setEditable(false);
    tree.addMouseListener(new MouseListener() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && vwr.selectedPanel != null) {
          vwr.selectedPanel.getPanelData().setZoom(0, 0, 0, 0);
          repaint();
        }
      }

      @Override
      public void mouseEntered(MouseEvent e) {
      }

      @Override
      public void mouseExited(MouseEvent e) {
      }

      @Override
      public void mousePressed(MouseEvent e) {
      }

      @Override
      public void mouseReleased(MouseEvent e) {
      }

    });
    new DropTarget(tree, dtl);

    // Initialise GUI Components
    try {
      jbInit();
    } catch (Exception e) {
      e.printStackTrace();
    }

    setApplicationElements();
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    // When application exits ...
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent we) {
        windowClosing_actionPerformed();
      }

      @Override
      public void windowIconified(WindowEvent we) {
        windowIconified_actionPerformed();
      }
    });
    setSize(JSViewer.DEFAULT_WIDTH, JSViewer.DEFAULT_HEIGHT);

  }

  /**
   * Shows or hides certain GUI elements
   */
  private void setApplicationElements() {
    appMenu.setSelections(sidePanelOn, toolbarOn, statusbarOn,
        vwr.selectedPanel);
    toolBar.setSelections(vwr.selectedPanel);
  }

  /**
   * Sets the preferences or properties of the application that is loaded from a
   * properties file.
   * 
   * @param shouldApplySpectrumDisplaySettings
   */
  private void setApplicationProperties(boolean shouldApplySpectrumDisplaySettings) {

    Properties properties = vwr.properties;
    String recentFilesString = properties.getProperty("recentFilePaths");
    recentFilePaths.clear();
    if (!recentFilesString.equals("")) {
      StringTokenizer st = new StringTokenizer(recentFilesString, ",");
      while (st.hasMoreTokens()) {
        String file = st.nextToken().trim();
        if (file.length() < 100)
          recentFilePaths.addLast(file);
      }
    }
    showExitDialog = Boolean
        .parseBoolean(properties.getProperty("confirmBeforeExit"));

    sidePanelOn = Boolean.parseBoolean(properties.getProperty("showSidePanel"));
    toolbarOn = Boolean.parseBoolean(properties.getProperty("showToolBar"));
    statusbarOn = Boolean.parseBoolean(properties.getProperty("showStatusBar"));

    jsv.setApplicationProperties(shouldApplySpectrumDisplaySettings);
  }

  /**
   * Initializes GUI components
   * 
   * @throws Exception
   */
  private void jbInit() throws Exception {
    toolBar = new AppToolBar(this);
    appMenu = new ApplicationMenu(this);
    appMenu.setRecentMenu(recentFilePaths);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setJMenuBar(appMenu);
    setTitle("JSpecView");
    getContentPane().setLayout(mainborderLayout);
    sideSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
    sideSplitPane.setOneTouchExpandable(true);
    statusLabel.setToolTipText("");
    statusLabel.setHorizontalTextPosition(SwingConstants.LEADING);
    statusLabel.setText("  ");
    statusPanel.setBorder(BorderFactory.createEtchedBorder());
    BorderLayout bl = new BorderLayout();
    bl.setHgap(2);
    bl.setVgap(2);
    statusPanel.setLayout(bl);
    mainSplitPane.setCursor(Cursor.getDefaultCursor());
    mainSplitPane.setOneTouchExpandable(true);
    mainSplitPane.setResizeWeight(0.3);
    getContentPane().add(statusPanel, BorderLayout.SOUTH);
    statusPanel.add(statusLabel, BorderLayout.NORTH);
    statusPanel.add(commandInput, BorderLayout.SOUTH);
    commandHistory = new CommandHistory(vwr, commandInput);
    commandInput.setFocusTraversalKeysEnabled(false);
    commandInput.addKeyListener(new KeyListener() {
      @Override
      public void keyPressed(KeyEvent e) {
        keyPressedEvent(e.getKeyCode(), e.getKeyChar());
      }

      @Override
      public void keyReleased(KeyEvent e) {
      }

      @Override
      public void keyTyped(KeyEvent e) {
      }

    });

    getContentPane().add(toolBar, BorderLayout.NORTH);
    getContentPane().add(mainSplitPane, BorderLayout.CENTER);

    spectraTreeScrollPane = new JScrollPane(tree);
    if (jmolDisplay != null) {
      JSplitPane leftPanel = new JSplitPane();
      BorderLayout bl1 = new BorderLayout();
      leftPanel.setLayout(bl1);
      JPanel jmolDisplayPanel = new JPanel();
      jmolDisplayPanel.setBackground(Color.BLACK);
      leftPanel.add(jmolDisplayPanel, BorderLayout.SOUTH);
      leftPanel.add(spectraTreeScrollPane, BorderLayout.NORTH);
      sideSplitPane.setTopComponent(spectraTreeScrollPane);
      sideSplitPane.setDividerLocation(splitPosition = 300);
      awaken(true);
      mainSplitPane.setLeftComponent(sideSplitPane);
    } else {
      mainSplitPane.setLeftComponent(spectraTreeScrollPane);
    }
    mainPanel = (Component) vwr.mainPanel;
    mainSplitPane.setRightComponent(mainPanel);
  }

  protected void keyPressedEvent(int keyCode, char keyChar) {
    commandHistory.keyPressed(keyCode);
    String ret = vwr.checkCommandLineForTip(keyChar, commandInput.getText(),
        true);
    if (ret != null)
      commandInput.setText(ret);
    commandInput.requestFocusInWindow();
  }

  void setError(boolean isError, boolean isWarningOnly) {
    appMenu.setError(isError, isWarningOnly);
    toolBar.setError(isError, isWarningOnly);
  }

  /**
   * Shows a dialog with the message "Not Yet Implemented"
   */
  void showNotImplementedOptionPane() {
    if (vwr.hasDisplay)
      JOptionPane.showMessageDialog(this, "Not Yet Implemented",
        "Not Yet Implemented", JOptionPane.INFORMATION_MESSAGE);
  }

  @Override
  public void panelEvent(Object eventObj) {
    if (eventObj instanceof PeakPickEvent) {
      vwr.processPeakPickEvent(eventObj, true);
    } else if (eventObj instanceof ZoomEvent) {
      writeStatus(
          "Double-Click highlighted spectrum in menu to zoom out; CTRL+/CTRL- to adjust Y scaling.");
    } else if (eventObj instanceof SubSpecChangeEvent) {
      SubSpecChangeEvent e = (SubSpecChangeEvent) eventObj;
      if (!e.isValid())
        vwr.advanceSpectrumBy(-e.getSubIndex());
    }
  }

  // //////// MENU ACTIONS ///////////

  void setSplitPane(boolean TF) {
    if (TF)
      mainSplitPane.setDividerLocation(200);
    else
      mainSplitPane.setDividerLocation(0);
  }

  void enableToolbar(boolean isEnabled) {
    if (isEnabled)
      getContentPane().add(toolBar, BorderLayout.NORTH);
    else
      getContentPane().remove(toolBar);
    validate();
  }

  void showPreferences() {
    PreferencesDialog pd = new PreferencesDialog(this, vwr, "Preferences", true,
        dsp);
    vwr.properties = pd.getPreferences();
    boolean shouldApplySpectrumDisplaySetting = pd
        .shouldApplySpectrumDisplaySettingsNow();
    // Apply Properties where appropriate
    setApplicationProperties(shouldApplySpectrumDisplaySetting);

    for (int i = vwr.panelNodes.size(); --i >= 0;)
      jsv.siSetPropertiesFromPreferences(vwr.panelNodes.get(i).jsvp,
          shouldApplySpectrumDisplaySetting);

    setApplicationElements();

    dsp.getDisplaySchemes();
    if (jsv.defaultDisplaySchemeName.equals("Current")) {
      vwr.setProperty("defaultDisplaySchemeName", tempDS);
    }
  }

  /**
   * Export spectrum in a given format
   * 
   * @param command
   *        the name of the format to export in
   */
  void exportSpectrumViaMenu(String command) {
    new Exporter().write(vwr, ScriptToken.getTokens(command), false);
  }

  void enableStatus(boolean TF) {
    if (TF)
      getContentPane().add(statusPanel, BorderLayout.SOUTH);
    else
      getContentPane().remove(statusPanel);
    validate();
  }

  protected void windowClosing_actionPerformed() {
    exitJSpecView(true, true);
  }

  protected void windowIconified_actionPerformed() {
    if (jsv.jmol == null)
      return;
    setState(NORMAL);
    isHidden = true;
    setVisible(!isHidden);
    exitJSpecView(false, false);
  }

  /**
   * Tree Cell Renderer for the Spectra Tree
   */
  private class SpectraTreeCellRenderer extends DefaultTreeCellRenderer {
    /**
     * 
     */
    private final static long serialVersionUID = 1L;
    JSVTreeNode node;

    public SpectraTreeCellRenderer() {
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean sel, boolean expanded,
                                                  boolean leaf, int row,
                                                  boolean hasFocus) {

      super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row,
          hasFocus);

      node = (JSVTreeNode) value;
      return this;
    }

    /**
     * Returns a font depending on whether a frame is hidden
     * 
     * @return the tree node that is associated with an internal frame
     */
    @Override
    public Font getFont() {
      return new Font("Dialog",
          (node == null || node.getPanelNode() == null
              || node.getPanelNode().jsvp == null ? Font.BOLD : Font.ITALIC),
          12);
    }

  }

  void updateToolbar(ScriptToken st, boolean tf) {
    if (vwr.selectedPanel != null)
      switch (st) {
      case COORDINATESON:
        toolBar.coordsToggleButton.setSelected(tf);
        break;
      case GRIDON:
        toolBar.gridToggleButton.setSelected(tf);
        break;
      default:
        break;
      }
  }

  void sourceClosed(JDXSource source) {
    appMenu.clearSourceMenu(source);
    setTitle("JSpecView");
    validateAndRepaint(false);
  }

  void setLoading(String fileName, String filePath) {
    appMenu.setCloseMenuItem(fileName);
    setTitle("JSpecView - "
        + (filePath.startsWith(JSVFileManager.SIMULATION_PROTOCOL)
            ? JSVFileManager.getSimulationType(filePath) + " SIMULATION"
            : filePath));
    appMenu.setSourceEnabled(true);
  }

  void updateRecentMenus(String filePath) {
    // ADD TO RECENT FILE PATHS
    if (filePath.length() > 100)
      return;
    if (recentFilePaths.size() >= MAX_RECENT)
      recentFilePaths.removeItemAt(MAX_RECENT - 1);
    if (recentFilePaths.contains(filePath))
      recentFilePaths.removeObj(filePath);
    recentFilePaths.add(0, filePath);
    SB filePaths = new SB();
    int n = recentFilePaths.size();
    for (int index = 0; index < n; index++)
      filePaths.append(", ").append(recentFilePaths.get(index));
    vwr.setProperty("recentFilePaths", (n == 0 ? "" : filePaths.substring(2)));
    appMenu.updateRecentMenus(recentFilePaths);
  }

  void setMenuEnables(PanelNode node,
                             @SuppressWarnings("unused") boolean isSplit) {
    appMenu.setMenuEnables(node);
    toolBar.setMenuEnables(node);
    // if (isSplit) // not sure why we care...
    // commandInput.requestFocusInWindow();

  }

//  public void setCursorObject(Object c) {
//    setCursor((Cursor) c);
//  }
//
  void setSelectedPanel(JSVPanel jsvp) {
    if (vwr.selectedPanel != null)
      mainSplitPosition = mainSplitPane.getDividerLocation();
    vwr.mainPanel.setSelectedPanel(vwr, jsvp, vwr.panelNodes);
    vwr.spectraTree.setSelectedPanel(jsv, jsvp);
    validate();
    if (jsvp != null) {
      jsvp.setEnabled(true);
      jsvp.setFocusable(true);
    }
    if (mainSplitPosition != 0)
      mainSplitPane.setDividerLocation(mainSplitPosition);
  }

  void validateAndRepaint(boolean isAll) {
    validate();
    if (isAll)
      repaint();
    else
      vwr.requestRepaint();
  }

  void execHidden(boolean b) {
    isHidden = (jsv.jmol != null && b);
    setVisible(!isHidden);
  }

  void writeStatus(String msg) {
    if (msg == null)
      msg = "Unexpected Error";
    if (msg.length() == 0)
      msg = "Enter a command:";
    statusLabel.setText(msg);
  }

  void setTreeEnabled(boolean b) {
    tree.setEnabled(b);
  }

  void closeMenuItem(JDXSource source) {
    if (source != null)
      appMenu.setCloseMenuItem(JSVFileManager.getTagName(source.getFilePath()));
    boolean isError = (source != null && source.getErrorLog().length() > 0);
    setError(isError,
        (isError && source.getErrorLog().indexOf("Warning") >= 0));
  }

}
