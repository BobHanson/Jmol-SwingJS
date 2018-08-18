/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-12-13 22:43:17 -0600 (Sat, 13 Dec 2014) $
 * $Revision: 20162 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either"
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
package org.gennbo;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import javajs.util.PT;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jmol.i18n.GT;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.jmolpanel.GuiMap;

class NBOConfig  {

  protected static final String NBO_WEB_SITE = "http://nbo7.chem.wisc.edu";
  protected static final String ARCHIVE_DIR = NBO_WEB_SITE + "/jmol_nborxiv/";

  protected static final String DEFAULT_SCRIPT = "zap;set nbocharges false;set antialiasdisplay;set fontscaling;" +
  		"set bondpicking true;set multipleBondSpacing -0.2; set multipleBondRadiusFactor 0.6;" +
  		"set zoomlarge false;select none;";

  protected static final String INPUT_FILE_EXTENSIONS = "adf;cfi;com;g09;gau;gms;jag;log;mm2;mnd;mol;mp;nw;orc;pqs;qc;vfi;xyz;47";
  protected static final String OUTPUT_FILE_EXTENSIONS = "adf;cfi;gau;gms;jag;mm2;mnd;mp;nw;orc;pqs;qc;mol;xyz;vfi;g09;com";
  protected static final String JMOL_EXTENSIONS = "xyz;mol";
  protected static final String SCRIPT_VIDEO_EXTENSIONS="script";
  protected static final String GIF_VIDEO_EXTENSIONS="gif";
  //  protected static final String RUN_EXTENSIONS = "47;gau;gms";

  protected final static String JMOL_FONT_SCRIPT = ";set fontscaling true;select _H; font label 10 arial plain 0.025;select !_H;font label 10 arial bold 0.025;select none;";

  /**
   * 14 pt M O N O S P A C E D
   */
  final static protected Font listFont = new Font("Monospaced", Font.BOLD, 14);

  /**
   * 16 pt M O N O S P A C E D
   */
  final static protected Font monoFont = new Font("Monospaced", Font.BOLD, 16);

  /**
   * user input box .... -- should be monospace?
   */
  final static protected Font userInputFont = new Font("Arial", Font.PLAIN, 12);

  /**
   * Settings and Help
   */
  final static protected Font settingHelpFont = new Font("Arial", Font.PLAIN,
      14);

  /**
   * Settings and Help
   */
  final static protected Font searchOpListFont = new Font("Arial", Font.BOLD,
      14);

  /**
   * Status
   */
  final static protected Font statusFont = searchOpListFont;

  /**
   * 16 pt Arial plain for search area text
   * 
   */
  final static protected Font searchTextAreaFont = new Font("Arial",
      Font.PLAIN, 16);

  /**
   * arial plain 16
   */
  final static protected Font iconFont = searchTextAreaFont;

  /**
   * 16 pt arial bold
   */
  final static protected Font nboFont = new Font("Arial", Font.BOLD, 16);

  /**
   * 16 pt bold italic
   */
  final static protected Font homeTextFont = new Font("Arial", Font.BOLD
      | Font.ITALIC, 16);

  /**
   * 18 pt Arial bold italic
   * 
   */
  final static protected Font titleFont = new Font("Arial", Font.BOLD
      | Font.ITALIC, 18);

  /**
   * run button 20-pt arial plain
   */
  final static protected Font runButtonFont = new Font("Arial", Font.PLAIN, 20);

  /**
   * MODEL VIEW ....
   */
  final static protected Font topFont = new Font("Arial", Font.BOLD, 20);

  /**
   * 22 pt bold italic
   */
  final static protected Font homeButtonFont = new Font("Arial", Font.BOLD
      | Font.ITALIC, 22);

  /**
   * 26 pt arial bold
   */
  final static protected Font nboFontLarge = new Font("Arial", Font.BOLD, 26);

  /**
   * "NBOPro@Jmol title 26-pt arial bold
   */
  final static protected Font nboProTitleFont = nboFontLarge;

  private static final int MODE_PATH_SERVICE = 0;
  private static final int MODE_PATH_WORKING = 1;

  final static protected Color titleColor = Color.blue;
  
  private static final String NBOPROPERTY_DISPLAY_OPTIONS = "displayOptions";

  
  protected final NBODialog dialog;
  protected final Viewer vwr;
  private final NBOPlugin nboPlugin;
  private final NBOService nboService;

  protected JSlider opacity = new JSlider(0, 10);
  protected JComboBox<Color> colorBox1, colorBox2;
  protected JCheckBox jCheckAtomNum, jCheckSelHalo, jCheckDebugVerbose,
      jCheckNboView, jCheckWireMesh;

  // Jmol/NBO visual settings

  protected static Color orbColor1, orbColor2, backgroundColor;
  protected static String orbColorJmol1, orbColorJmol2;
  protected static float opacityOp;
  protected static boolean nboView;
  protected static boolean useWireMesh;
  protected static boolean showAtNum;
  protected static boolean debugVerbose;

  protected NBOConfig(NBODialog dialog) {
    this.dialog = dialog;
    this.vwr = dialog.vwr;
    this.nboPlugin = dialog.nboPlugin;
    this.nboService = dialog.nboService;
  }

  /**
   * Creates a dialog for getting info related to output frames in nbo format.
   * 
   * @param settingsPanel
   * 
   * @return settings panel
   */
  @SuppressWarnings("unchecked")
  protected JPanel buildSettingsPanel(JPanel settingsPanel) {

    settingsPanel.removeAll();
    checkNBOStatus();
    String viewOpts = getOrbitalDisplayOptions();

    settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
    addPathSetting(settingsPanel, MODE_PATH_SERVICE);
    addPathSetting(settingsPanel, MODE_PATH_WORKING);

    //Settings

    JButton jbDefaults = new JButton("Set Defaults");
    jbDefaults.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        doSetDefaults(false);
      }

    });
    settingsPanel.add(NBOUtil.createTitleBox(" Settings ", jbDefaults));
    jCheckAtomNum = new JCheckBox("Show Atom Numbers");//.setAlignmentX(0.5f);
    jCheckAtomNum.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doSettingAtomNums();
      }
    });
    showAtNum = true;
    jCheckAtomNum.setSelected(true);
    Box settingsBox = NBOUtil.createBorderBox(true);
    settingsBox.add(jCheckAtomNum);

    jCheckSelHalo = new JCheckBox("Show selection halos on atoms");
    jCheckSelHalo.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doSettingShowHalos();
      }
    });
    jCheckSelHalo.doClick();
    settingsBox.add(jCheckSelHalo);

    jCheckWireMesh = new JCheckBox("Use wire mesh for orbital display");

    settingsBox.add(jCheckWireMesh);
    Color[] colors = { Color.red, Color.orange, Color.yellow, Color.green,
        Color.cyan, Color.blue, Color.magenta, };

    JPanel displayOps = new JPanel(new GridLayout(1, 4));
    JLabel label = new JLabel("     (+) color: ");
    displayOps.add(label);
    colorBox1 = new JComboBox<Color>(colors);
    colorBox1.setRenderer(new ColorRenderer());
    colorBox1.setSelectedItem(orbColor1);
    displayOps.add(colorBox1);
    colorBox1.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        orbColor1 = ((Color) colorBox1.getSelectedItem());
        setOrbitalDisplayOptions();
      }
    });

    displayOps.add(new JLabel("     (-) color: "));
    colorBox2 = new JComboBox<Color>(colors);
    colorBox2.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        orbColor2 = ((Color) colorBox2.getSelectedItem());
        setOrbitalDisplayOptions();
      }
    });
    colorBox2.setSelectedItem(orbColor2);
    colorBox2.setRenderer(new ColorRenderer());

    displayOps.add(colorBox2);
    displayOps.setAlignmentX(0.0f);
    settingsBox.add(displayOps);
    settingsBox.add(Box.createRigidArea(new Dimension(10, 10)));

    //Opacity slider///////////////////
    opacity.setMajorTickSpacing(1);
    opacity.setPaintTicks(true);
    Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
    for (int i = 0; i <= 10; i++)
      labelTable.put(new Integer(i), new JLabel(i == 10 ? "1" : "0." + i));
    opacity.setPaintLabels(true);
    opacity.setLabelTable(labelTable);
    opacity.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        opacityOp = (float) opacity.getValue() / 10;
        setOrbitalDisplayOptions();
      }
    });
    Box opacBox = Box.createHorizontalBox();
    opacBox.add(new JLabel("Orbital opacity:  "));
    opacBox.setAlignmentX(0.0f);
    opacBox.add(opacity);
    settingsBox.add(opacBox);

    jCheckWireMesh.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doSettingWireMesh();
      }
    });
    if (useWireMesh)
      jCheckWireMesh.setSelected(true);

    jCheckNboView = new JCheckBox("Emulate NBO View");
    jCheckNboView.setSelected(true);
    //    settingsBox.add(jCheckNboView);
    jCheckNboView.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doSetDefaults(!jCheckNboView.isSelected());
      }
    });
    jCheckDebugVerbose = new JCheckBox("Verbose Debugging");
    settingsBox.add(jCheckDebugVerbose);
    jCheckDebugVerbose.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        debugVerbose = ((JCheckBox) e.getSource()).isSelected();
      }
    });
    if (!"nboView".equals(viewOpts) && !"default".equals(viewOpts)) {
      jCheckNboView.doClick();
    } else {
      opacity.setValue((int) (opacityOp * 10));
    }
    settingsBox.setBorder(BorderFactory.createLineBorder(Color.black));
    settingsPanel.add(settingsBox);
    return settingsPanel;
  }

  protected void doSettingShowHalos() {
    dialog.runScriptQueued("select " + jCheckSelHalo.isSelected());
  }

  protected void doSettingWireMesh() {
    useWireMesh = !useWireMesh;
    opacity.setValue(0);
  }

  protected void doSettingAtomNums() {
    showAtNum = !showAtNum;
    dialog.doSetStructure(null);
  }

  protected void doSetDefaults(boolean isJmol) {
    nboPlugin.setNBOProperty(NBOPROPERTY_DISPLAY_OPTIONS, "default");
    getOrbitalDisplayOptions();
    opacity.setValue((int) (opacityOp * 10));
    colorBox1.setSelectedItem(orbColor1);
    colorBox2.setSelectedItem(orbColor2);

    jCheckWireMesh.setSelected(useWireMesh);
    //jCheckWireMesh.doClick();
    jCheckAtomNum.setSelected(true);
    //jCheckAtomNum.doClick();
    jCheckSelHalo.setSelected(true);
    //jCheckSelHalo.doClick();
    jCheckDebugVerbose.setSelected(false);
    //jCheckDebugVerbose.doClick();
    jCheckNboView.setSelected(false);
    //jCheckNboView.doClick();

    if (isJmol) {
      if (!dialog.jmolOptionNONBO)
        dialog.runScriptQueued("background gray;set defaultcolors Jmol;refresh;");
    } else {
      if (jCheckWireMesh.isSelected())
        jCheckWireMesh.doClick();
      colorBox1.setSelectedItem(Color.cyan);
      colorBox2.setSelectedItem(Color.yellow);
      opacity.setValue(7);
      try {
        String atomColors = "";
        atomColors = GuiMap.getResourceString(this,
            "org/gennbo/assets/atomColors.txt");
        dialog.runScriptQueued(atomColors + ";refresh");
      } catch (IOException e) {
        dialog.logError("atomColors.txt not found");
      }
      nboView = true;
    }
    dialog.updatePanelSettings();
  }

  protected void setOrbitalDisplayOptions() {
    orbColorJmol1 = "[" + orbColor1.getRed() + " " + orbColor1.getGreen() + " "
        + orbColor1.getBlue() + "]";
    orbColorJmol2 = "[" + orbColor2.getRed() + " " + orbColor2.getGreen() + " "
        + orbColor2.getBlue() + "]";
    dialog.updatePanelSettings();
    if (!nboView)
      nboPlugin.setNBOProperty(NBOPROPERTY_DISPLAY_OPTIONS, orbColor1.getRGB()
          + "," + orbColor2.getRGB() + "," + opacityOp + "," + useWireMesh);
  }

  private String getOrbitalDisplayOptions() {
    String options = (dialog.jmolOptionNONBO ? "jmol" : nboPlugin.getNBOProperty(
        NBOPROPERTY_DISPLAY_OPTIONS, "default"));
    if (options.equals("default") || options.equals("nboView")) {
      orbColor1 = Color.cyan;
      orbColor2 = Color.yellow;
      opacityOp = 0.7f;
      useWireMesh = false;
    } else if (options.equals("jmol")) {
      orbColor1 = Color.blue;
      orbColor2 = Color.red;
      opacityOp = 1f;
      useWireMesh = true;
    } else {
      // color1, color2, useMesh
      String[] toks = options.split(",");
      orbColor1 = new Color(Integer.parseInt(toks[0]));
      orbColor2 = new Color(Integer.parseInt(toks[1]));
      opacityOp = Float.parseFloat(toks[2]);
      useWireMesh = toks[3].contains("true");
    }
    return options;
  }

  /**
   * return a script ISOSURFACE ID id COLOR color1 color2 NBO/MO n [optional
   * BETA] [display options] also used in VIEW
   * 
   * @param id
   *        ISOSURFACE ID
   * @param type
   *        NBO type
   * @param orbitalNumber
   *        1-based
   * @param isBeta
   * @param isNegative
   * @return Jmol ISOSURFACE command string
   */
  protected static String getJmolIsosurfaceScript(String id, String type,
                                           int orbitalNumber, boolean isBeta,
                                           boolean isNegative) {
    return ";select 1.1;isosurface ID \""
        + id
        + "\" color "
        + (isNegative ? orbColorJmol1 + " "
            + orbColorJmol2 : orbColorJmol2
            + " " + orbColorJmol1) + " cutoff 0.0316 NBO "
        + type
        + " "
        + orbitalNumber
        + (isBeta && !type.equals("MO") ? " beta" : "") // AO?
        + " frontonly "
        + (useWireMesh ? " mesh nofill" : " nomesh fill translucent "
            + (1 - opacityOp)) + ";select none;";
  }


  private void addPathSetting(JPanel panel, final int mode) {
    //GUI for NBO path selection
    String title = "";
    String path = "";
    switch (mode) {
    case MODE_PATH_SERVICE:
      title = " NBOPro Executables Folder ";
      path = nboService.getServerPath(null);
      break;
    case MODE_PATH_WORKING:
      title = " User Work Folder ";
      path = dialog.getWorkingPath();
      break;
    }
    final JTextField tfPath = new JTextField(path);
    tfPath.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        doSetNBOPath(tfPath, mode);
      }

    });
    panel.add(NBOUtil.createTitleBox(title, null));
    Box serverBox = NBOUtil.createBorderBox(true);
    serverBox.add(createPathBox(tfPath, mode));
    serverBox.setMaximumSize(new Dimension(350, 50));
    panel.add(serverBox);

  }

  protected void doSetNBOPath(JTextField tf, int mode) {
    String path = PT.rep(tf.getText(), "\\", "/");
    if (path.length() == 0)
      return;
    switch (mode) {
    case MODE_PATH_SERVICE:
      nboService.setServerPath(path);
      dialog.log("NBOServe location changed changed:<br> " + path, 'b');
      connect();
      break;
    case MODE_PATH_WORKING:
      if ((path + "/").equals(nboService.getServerPath(null))) {
        vwr.alert("The working directory may not be the same as the directory containing NBOServe");
        return;
      }
      nboPlugin.setNBOProperty("workingPath", path);
      dialog.log("Working path directory changed:<br> " + path, 'b');
      break;
    }
  }

  protected boolean connect() {
    if (!nboService.haveGenNBO())
      return false;
    boolean isOK = dialog.checkEnabled();
    if (isOK)
      dialog.icon.setText("Connected");
    //appendOutputWithCaret(isOK ? "NBOServe successfully connected" : "Could not connect",'p');
    return isOK;
  }


  private void checkNBOStatus() {
    if (dialog.jmolOptionNONBO)
      return;
    if (nboService.restartIfNecessary()) {
    } else {
      String s = "Could not connect to NBOServe. Check to make sure its path is correct. \nIf it is already running from another instance of Jmol, you must first close that instance.";
      dialog.alertError(s);
    }

  }

  /**
   * Create a horizontal box with a text field, a
   * 
   * @param tf
   * @param mode
   *        MODE_PATH_SERVICE or MODE_PATH_WORKING
   * @return a box
   */
  private Box createPathBox(final JTextField tf, final int mode) {
    Box box = Box.createHorizontalBox();
    box.add(tf);
    JButton b = new JButton("Browse");
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doPathBrowseClicked(tf, tf.getText(), mode);
      }
    });
    box.add(b);
    return box;
  }

  /**
   * Show a file selector for choosing NBOServe from config.
   * 
   * @param tf
   * @param fname
   * @param mode
   */
  protected void doPathBrowseClicked(final JTextField tf, String fname, int mode) {
    JFileChooser myChooser = new JFileChooser();
    myChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    myChooser.setSelectedFile(new File(fname + "/ "));
    int button = myChooser.showDialog(dialog, GT.$("Select"));
    if (button == JFileChooser.APPROVE_OPTION) {
      File newFile = myChooser.getSelectedFile();
      tf.setText(PT.rep(newFile.toString(), "\\", "/"));
      tf.postActionEvent();
    }
  }

  @SuppressWarnings("rawtypes")
  class ColorRenderer extends JButton implements ListCellRenderer {

    boolean b = false;

    public ColorRenderer() {
      setOpaque(true);
    }

    @Override
    public void setBackground(Color bg) {
      if (!b)
        return;
      super.setBackground(bg);
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
      b = true;
      setText(" ");
      setBackground((Color) value);
      b = false;
      return this;
    }

  }

}
