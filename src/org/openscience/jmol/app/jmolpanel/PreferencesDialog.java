/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2018-07-22 20:29:48 -0500 (Sun, 22 Jul 2018) $
 * $Revision: 21922 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

import org.jmol.api.JmolAbstractButton;
import org.jmol.i18n.GT;
import org.jmol.script.T;
import org.jmol.util.Elements;
import org.jmol.util.Logger;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Hashtable;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;

import javajs.util.PT;

import javax.swing.JRadioButton;
import javax.swing.BoxLayout;
import javax.swing.JSlider;
import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JCheckBox;
import javax.swing.Box;
import javax.swing.JTabbedPane;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.border.TitledBorder;

public class PreferencesDialog extends JDialog implements ActionListener {

  private boolean autoBond;
  boolean showHydrogens;
  boolean showMeasurements;
  boolean perspectiveDepth;
  boolean showAxes;
  boolean showBoundingBox;
  boolean axesOrientationRasmol;
  boolean openFilePreview;
  boolean clearHistory;
  int fontScale = 1;
  float minBondDistance;
  float bondTolerance;
  short marBond;
  int percentVdwAtom;
  int bondingVersion;

  JButton bButton, pButton, tButton, eButton, vButton;
  private JRadioButton /*pYes, pNo, */ abYes, abNo;
  private JSlider vdwPercentSlider;
  private JSlider bdSlider, bwSlider, btSlider;
  private JCheckBox cH, cM;
  private JCheckBox cbPerspectiveDepth;
  private JCheckBox cbShowAxes, cbShowBoundingBox;
  private JCheckBox cbAxesOrientationRasmol;
  private JCheckBox cbOpenFilePreview;
  private JCheckBox cbClearHistory;
  //  private JCheckBox cbLargeFont;
  private Properties jmolDefaultProperties;
  Properties currentProperties;

  // The actions:

  private PrefsAction prefsAction = new PrefsAction();
  private Map<String, Action> commands;

  final static String[] jmolDefaults = { "jmolDefaults", "true",
      "showHydrogens", "true", "showMeasurements", "true", "perspectiveDepth",
      "true", "showAxes", "false", "showBoundingBox", "false",
      "axesOrientationRasmol", "false", "openFilePreview", "true", "autoBond",
      "true", "percentVdwAtom", "" + JC.DEFAULT_PERCENT_VDW_ATOM, "marBond",
      "" + JC.DEFAULT_BOND_MILLIANGSTROM_RADIUS, "minBondDistance",
      "" + JC.DEFAULT_MIN_BOND_DISTANCE, "bondTolerance",
      "" + JC.DEFAULT_BOND_TOLERANCE, "bondingVersion",
      "" + Elements.RAD_COV_IONIC_OB1_100_1, };

  final static String[] rasmolOverrides = { "jmolDefaults", "false",
      "percentVdwAtom", "0", "marBond", "1", "axesOrientationRasmol", "true", };

  JmolPanel jmol;
  Viewer vwr;
  GuiMap guimap;

  List<Action> actions = new ArrayList<Action>();

  {
    addActions(actions);
  }

  public PreferencesDialog(JmolPanel jmol, JFrame f, GuiMap guimap,
      Viewer vwr) {

    super(f, false);
    this.jmol = jmol;
    this.guimap = guimap;
    this.vwr = vwr;

    initializeProperties();

    this.setTitle(GT.$("Preferences"));

    initVariables();
    commands = new Hashtable<String, Action>();

    for (int i = 0; i < actions.size(); i++) {
      Action a = actions.get(i);
      Object name = a.getValue(Action.NAME);
      commands.put((name != null) ? name.toString() : null, a);
    }
    JPanel container = new JPanel();
    container.setLayout(new BorderLayout());

    JTabbedPane tabs = new JTabbedPane();
    JPanel disp = buildDispPanel();
    JPanel atoms = buildAtomsPanel();
    JPanel bonds = buildBondPanel();
    //    JPanel vibrate = buildVibratePanel();
    tabs.addTab(GT.$("Display"), null, disp);
    tabs.addTab(GT.$("Atoms"), null, atoms);
    tabs.addTab(GT.$("Bonds"), null, bonds);

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

    jmolDefaultsButton = new JButton(GT.$("Jmol Defaults"));
    jmolDefaultsButton.addActionListener(this);
    buttonPanel.add(jmolDefaultsButton);

    rasmolDefaultsButton = new JButton(GT.$("RasMol Defaults"));
    rasmolDefaultsButton.addActionListener(this);
    buttonPanel.add(rasmolDefaultsButton);

    //cancelButton = new JButton(GT._("Cancel"));
    //cancelButton.addActionListener(this);
    //buttonPanel.add(cancelButton);

    applyButton = new JButton(GT.$("Apply"));
    applyButton.addActionListener(this);
    buttonPanel.add(applyButton);

    okButton = new JButton(GT.$("OK"));
    okButton.addActionListener(this);
    buttonPanel.add(okButton);
    getRootPane().setDefaultButton(okButton);

    container.add(tabs, BorderLayout.CENTER);
    container.add(buttonPanel, BorderLayout.SOUTH);
    getContentPane().add(container);

    updateComponents();

    pack();
    centerDialog();
  }

  public JPanel buildDispPanel() {

    JPanel disp = new JPanel();
    GridBagLayout gridbag = new GridBagLayout();
    disp.setLayout(gridbag);
    GridBagConstraints constraints;

    JPanel showPanel = new JPanel();
    showPanel.setLayout(new GridLayout(1, 3));
    showPanel.setBorder(new TitledBorder(GT.$("Show All")));
    cH = guimap.newJCheckBox("Prefs.showHydrogens",
        vwr.getBoolean(T.showhydrogens));
    cH.addItemListener(checkBoxListener);
    cM = guimap.newJCheckBox("Prefs.showMeasurements",
        vwr.getBoolean(T.showmeasurements));
    cM.addItemListener(checkBoxListener);
    showPanel.add(cH);
    showPanel.add(cM);

    constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    disp.add(showPanel, constraints);

    JPanel fooPanel = new JPanel();
    fooPanel.setBorder(new TitledBorder(""));
    fooPanel.setLayout(new GridLayout(2, 1));

    cbPerspectiveDepth = guimap.newJCheckBox("Prefs.perspectiveDepth",
        vwr.tm.perspectiveDepth);
    cbPerspectiveDepth.addItemListener(checkBoxListener);
    fooPanel.add(cbPerspectiveDepth);

    cbShowAxes = guimap.newJCheckBox("Prefs.showAxes", vwr.getShowAxes());
    cbShowAxes.addItemListener(checkBoxListener);
    fooPanel.add(cbShowAxes);

    cbShowBoundingBox = guimap.newJCheckBox("Prefs.showBoundingBox",
        vwr.getShowBbcage());
    cbShowBoundingBox.addItemListener(checkBoxListener);
    fooPanel.add(cbShowBoundingBox);

    constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    disp.add(fooPanel, constraints);

    JPanel axesPanel = new JPanel();
    axesPanel.setBorder(new TitledBorder(""));
    axesPanel.setLayout(new GridLayout(1, 1));

    cbAxesOrientationRasmol = guimap.newJCheckBox("Prefs.axesOrientationRasmol",
        vwr.getBoolean(T.axesorientationrasmol));
    cbAxesOrientationRasmol.addItemListener(checkBoxListener);
    axesPanel.add(cbAxesOrientationRasmol);

    constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    disp.add(axesPanel, constraints);

    JPanel otherPanel = new JPanel();
    otherPanel.setBorder(new TitledBorder(""));
    otherPanel.setLayout(new GridLayout(2, 1));

    cbOpenFilePreview = guimap.newJCheckBox("Prefs.openFilePreview",
        openFilePreview);
    cbOpenFilePreview.addItemListener(checkBoxListener);
    otherPanel.add(cbOpenFilePreview);

    cbClearHistory = guimap.newJCheckBox("Prefs.clearHistory", clearHistory);
    cbClearHistory.addItemListener(checkBoxListener);
    otherPanel.add(cbClearHistory);

    //    cbLargeFont =
    //        guimap.newJCheckBox("Prefs.largeFont", largeFont);
    //    cbLargeFont.addItemListener(checkBoxListener);
    //    otherPanel.add(cbLargeFont);

    constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    disp.add(otherPanel, constraints);

    JLabel filler = new JLabel();
    constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.gridheight = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.weightx = 1.0;
    constraints.weighty = 1.0;
    disp.add(filler, constraints);

    return disp;
  }

  public JPanel buildAtomsPanel() {

    JPanel atomPanel = new JPanel(new GridBagLayout());
    GridBagConstraints constraints;

    JPanel sfPanel = new JPanel();
    sfPanel.setLayout(new BorderLayout());
    sfPanel.setBorder(new TitledBorder(GT.$("Default atom size")));
    JLabel sfLabel = new JLabel(GT.$("(percentage of vanDerWaals radius)"),
        SwingConstants.CENTER);
    sfPanel.add(sfLabel, BorderLayout.NORTH);
    vdwPercentSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 100,
        vwr.getInt(T.percentvdwatom));
    vdwPercentSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    vdwPercentSlider.setPaintTicks(true);
    vdwPercentSlider.setMajorTickSpacing(20);
    vdwPercentSlider.setMinorTickSpacing(10);
    vdwPercentSlider.setPaintLabels(true);
    vdwPercentSlider.addChangeListener(new ChangeListener() {

      @Override
      public void stateChanged(ChangeEvent e) {
        rebond();
      }
    });
    sfPanel.add(vdwPercentSlider, BorderLayout.CENTER);
    constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    atomPanel.add(sfPanel, constraints);

    JLabel filler = new JLabel();
    constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.gridheight = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.weightx = 1.0;
    constraints.weighty = 1.0;
    atomPanel.add(filler, constraints);

    return atomPanel;
  }

  @SuppressWarnings("unchecked")
  private Dictionary<Object, Object> getJSliderLabelTable(JSlider slider) {
    return slider.getLabelTable();
  }

  public JPanel buildBondPanel() {

    JPanel bondPanel = new JPanel();
    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    bondPanel.setLayout(gridbag);
    c.fill = GridBagConstraints.BOTH;
    c.weightx = 1.0;
    c.weighty = 1.0;

    // Automatic calculation of bonds upon molecule load
    JPanel autobondPanel = new JPanel();
    autobondPanel.setLayout(new BoxLayout(autobondPanel, BoxLayout.Y_AXIS));
    autobondPanel.setBorder(new TitledBorder(GT.$("Compute Bonds")));
    ButtonGroup abGroup = new ButtonGroup();
    abYes = new JRadioButton(GT.$("Automatically"));
    abNo = new JRadioButton(GT.$("Don't Compute Bonds"));
    abGroup.add(abYes);
    abGroup.add(abNo);
    autobondPanel.add(abYes);
    autobondPanel.add(abNo);
    autobondPanel.add(Box.createVerticalGlue());
    abYes.setSelected(vwr.getBoolean(T.autobond));
    abYes.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        vwr.setBooleanProperty("autoBond", true);
        currentProperties.put("autoBond", "" + "true");
      }
    });

    abNo.setSelected(!vwr.getBoolean(T.autobond));
    abNo.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        vwr.setBooleanProperty("autoBond", false);
        currentProperties.put("autoBond", "" + "false");
      }
    });

    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(autobondPanel, c);
    bondPanel.add(autobondPanel);

    JPanel bwPanel = new JPanel();
    bwPanel.setLayout(new BorderLayout());
    bwPanel.setBorder(new TitledBorder(GT.$("Default Bond Radius")));
    JLabel bwLabel = new JLabel(GT.$("(Angstroms)"), SwingConstants.CENTER);
    bwPanel.add(bwLabel, BorderLayout.NORTH);

    bwSlider = new JSlider(0, 250, vwr.getMadBond() / 2);
    bwSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    bwSlider.setPaintTicks(true);
    bwSlider.setMajorTickSpacing(50);
    bwSlider.setMinorTickSpacing(25);
    bwSlider.setPaintLabels(true);
    for (int i = 0; i <= 250; i += 50) {
      String label = "" + (1000 + i);
      label = "0." + label.substring(1);
      Dictionary<Object, Object> labelTable = getJSliderLabelTable(bwSlider);
      labelTable.put(Integer.valueOf(i),
          new JLabel(label, SwingConstants.CENTER));
      bwSlider.setLabelTable(labelTable);
    }
    bwSlider.addChangeListener(new ChangeListener() {

      @Override
      public void stateChanged(ChangeEvent e) {
        rebond();
      }
    });

    bwPanel.add(bwSlider, BorderLayout.SOUTH);

    c.weightx = 0.0;
    gridbag.setConstraints(bwPanel, c);
    bondPanel.add(bwPanel);

    // Bond Tolerance Slider
    JPanel btPanel = new JPanel();
    btPanel.setLayout(new BorderLayout());
    btPanel.setBorder(new TitledBorder(
        GT.$("Bond Tolerance - sum of two covalent radii + this value")));
    JLabel btLabel = new JLabel(GT.$("(Angstroms)"), SwingConstants.CENTER);
    btPanel.add(btLabel, BorderLayout.NORTH);

    btSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 100,
        (int) (100 * vwr.getFloat(T.bondtolerance)));
    btSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    btSlider.setPaintTicks(true);
    btSlider.setMajorTickSpacing(20);
    btSlider.setMinorTickSpacing(10);
    btSlider.setPaintLabels(true);
    Dictionary<Object, Object> labelTable = getJSliderLabelTable(btSlider);
    labelTable.put(Integer.valueOf(0),
        new JLabel("0.0", SwingConstants.CENTER));
    btSlider.setLabelTable(labelTable);
    labelTable.put(Integer.valueOf(20),
        new JLabel("0.2", SwingConstants.CENTER));
    btSlider.setLabelTable(labelTable);
    labelTable.put(Integer.valueOf(40),
        new JLabel("0.4", SwingConstants.CENTER));
    btSlider.setLabelTable(labelTable);
    labelTable.put(Integer.valueOf(60),
        new JLabel("0.6", SwingConstants.CENTER));
    btSlider.setLabelTable(labelTable);
    labelTable.put(Integer.valueOf(80),
        new JLabel("0.8", SwingConstants.CENTER));
    btSlider.setLabelTable(labelTable);
    labelTable.put(Integer.valueOf(100),
        new JLabel("1.0", SwingConstants.CENTER));
    btSlider.setLabelTable(labelTable);

    btSlider.addChangeListener(new ChangeListener() {

      @Override
      public void stateChanged(ChangeEvent e) {
        rebond();
      }
    });
    btPanel.add(btSlider);

    c.weightx = 0.0;
    gridbag.setConstraints(btPanel, c);
    bondPanel.add(btPanel);

    // minimum bond distance slider
    JPanel bdPanel = new JPanel();
    bdPanel.setLayout(new BorderLayout());
    bdPanel.setBorder(new TitledBorder(GT.$("Minimum Bonding Distance")));
    JLabel bdLabel = new JLabel(GT.$("(Angstroms)"), SwingConstants.CENTER);
    bdPanel.add(bdLabel, BorderLayout.NORTH);

    bdSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 100,
        (int) (100 * vwr.getFloat(T.minbonddistance)));
    bdSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    bdSlider.setPaintTicks(true);
    bdSlider.setMajorTickSpacing(20);
    bdSlider.setMinorTickSpacing(10);
    bdSlider.setPaintLabels(true);
    labelTable = getJSliderLabelTable(bdSlider);
    labelTable.put(Integer.valueOf(0),
        new JLabel("0.0", SwingConstants.CENTER));
    bdSlider.setLabelTable(labelTable);
    labelTable.put(Integer.valueOf(20),
        new JLabel("0.2", SwingConstants.CENTER));
    bdSlider.setLabelTable(labelTable);
    labelTable.put(Integer.valueOf(40),
        new JLabel("0.4", SwingConstants.CENTER));
    bdSlider.setLabelTable(labelTable);
    labelTable.put(Integer.valueOf(60),
        new JLabel("0.6", SwingConstants.CENTER));
    bdSlider.setLabelTable(labelTable);
    labelTable.put(Integer.valueOf(80),
        new JLabel("0.8", SwingConstants.CENTER));
    bdSlider.setLabelTable(labelTable);
    labelTable.put(Integer.valueOf(100),
        new JLabel("1.0", SwingConstants.CENTER));
    bdSlider.setLabelTable(labelTable);

    bdSlider.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        rebond();
      }
    });
    bdPanel.add(bdSlider);

    c.weightx = 0.0;
    gridbag.setConstraints(bdPanel, c);
    bondPanel.add(bdPanel);

    return bondPanel;
  }

  protected void centerDialog() {

    Dimension screenSize = this.getToolkit().getScreenSize();
    Dimension size = this.getSize();
    screenSize.height = screenSize.height / 2;
    screenSize.width = screenSize.width / 2;
    size.height = size.height / 2;
    size.width = size.width / 2;
    int y = screenSize.height - size.height;
    int x = screenSize.width - size.width;
    this.setLocation(x, y);
  }

  public void ok() {
    save();
    dispose();
  }

  public void cancel() {
    updateComponents();
    dispose();
  }

  private void updateComponents() {
    // Display panel
    cH.setSelected(vwr.getBoolean(T.showhydrogens));
    cM.setSelected(vwr.getBoolean(T.showmeasurements));

    cbPerspectiveDepth.setSelected(vwr.tm.perspectiveDepth);
    cbShowAxes.setSelected(vwr.getShowAxes());
    cbShowBoundingBox.setSelected(vwr.getShowBbcage());

    cbAxesOrientationRasmol
        .setSelected(vwr.getBoolean(T.axesorientationrasmol));

    cbOpenFilePreview.setSelected(openFilePreview);
    cbClearHistory.setSelected(clearHistory);
    //cbLargeFont.setSelected(largeFont);

    // Atom panel controls: 
    vdwPercentSlider.setValue(vwr.getInt(T.percentvdwatom));

    // Bond panel controls:
    abYes.setSelected(vwr.getBoolean(T.autobond));
    bwSlider.setValue(vwr.getMadBond() / 2);
    bdSlider.setValue((int) (100 * vwr.getFloat(T.minbonddistance)));
    btSlider.setValue((int) (100 * vwr.getFloat(T.bondtolerance)));

  }

  private void apply() {
    rebond();
    save();
    vwr.refresh(Viewer.REFRESH_SYNC_MASK, "PreferencesDialog:apply()");
  }

  void save() {
    try {
      FileOutputStream fileOutputStream = new FileOutputStream(
          jmol.jmolApp.userPropsFile);
      currentProperties.store(fileOutputStream, "Jmol");
      fileOutputStream.close();
    } catch (Exception e) {
      Logger.errorEx("Error saving preferences", e);
    }
  }

  void initializeProperties() {
    jmolDefaultProperties = new Properties(System.getProperties());
    for (int i = jmolDefaults.length; (i -= 2) >= 0;) {
      jmolDefaultProperties.put(jmolDefaults[i], jmolDefaults[i + 1]);
    }
    currentProperties = new Properties(jmolDefaultProperties);
    try {
      BufferedInputStream bis = new BufferedInputStream(
          new FileInputStream(jmol.jmolApp.userPropsFile), 1024);
      currentProperties.load(bis);
      bis.close();
    } catch (Exception e2) {
    }
    //    System.setProperties(currentProperties);
  }

  void resetDefaults(String[] overrides) {
    currentProperties = new Properties(jmolDefaultProperties);
    //System.setProperties(currentProperties);
    if (overrides != null) {
      for (int i = overrides.length; (i -= 2) >= 0;)
        currentProperties.put(overrides[i], overrides[i + 1]);
    }
    initVariables();
    vwr.refresh(Viewer.REFRESH_SYNC_MASK, "PreferencesDialog:resetDefaults()");
    updateComponents();
  }

  void rebond() {
    percentVdwAtom = vdwPercentSlider.getValue();
    vwr.setIntProperty("PercentVdwAtom", percentVdwAtom);
    currentProperties.put("percentVdwAtom", "" + percentVdwAtom);

    bondTolerance = btSlider.getValue() / 100f;
    vwr.setFloatProperty("bondTolerance", bondTolerance);
    currentProperties.put("bondTolerance", "" + bondTolerance);

    minBondDistance = bdSlider.getValue() / 100f;
    vwr.setFloatProperty("minBondDistance", minBondDistance);
    currentProperties.put("minBondDistance", "" + minBondDistance);

    marBond = (short) bwSlider.getValue();
    vwr.setIntProperty("bondRadiusMilliAngstroms", marBond);
    currentProperties.put("marBond", "" + marBond);

    vwr.rebond();
    vwr.refresh(Viewer.REFRESH_SYNC_MASK, "PreferencesDialog:rebond()");
  }

  void initVariables() {

    autoBond = getBoolean("autoBond");
    showHydrogens = getBoolean("showHydrogens");
    //showVectors = getBoolean("showVectors");
    showMeasurements = getBoolean("showMeasurements");
    perspectiveDepth = getBoolean("perspectiveDepth");
    showAxes = getBoolean("showAxes");
    showBoundingBox = getBoolean("showBoundingBox");
    axesOrientationRasmol = getBoolean("axesOrientationRasmol");
    openFilePreview = Boolean
        .parseBoolean(currentProperties.getProperty("openFilePreview", "true"));
    clearHistory = getBoolean("clearHistory");

    minBondDistance = Float.parseFloat(getProp("minBondDistance"));
    bondTolerance = Float.parseFloat(getProp("bondTolerance"));
    marBond = Short.parseShort(getProp("marBond"));
    percentVdwAtom = Integer.parseInt(getProp("percentVdwAtom"));
    bondingVersion = Integer.parseInt(getProp("bondingVersion"));
    fontScale = Math.max(PT.parseInt("" + getProp("consoleFontScale")), 0) % 5;

    if (getBoolean("jmolDefaults"))
      vwr.setStringProperty("defaults", "Jmol");
    else
      vwr.setStringProperty("defaults", "RasMol");

    vwr.setIntProperty("percentVdwAtom", percentVdwAtom);
    vwr.setIntProperty("bondRadiusMilliAngstroms", marBond);
    vwr.setIntProperty("bondingVersion", bondingVersion);
    vwr.setFloatProperty("minBondDistance", minBondDistance);
    vwr.setFloatProperty("BondTolerance", bondTolerance);
    vwr.setBooleanProperty("autoBond", autoBond);
    vwr.setBooleanProperty("showHydrogens", showHydrogens);
    vwr.setBooleanProperty("showMeasurements", showMeasurements);
    vwr.setBooleanProperty("perspectiveDepth", perspectiveDepth);
    vwr.setBooleanProperty("showAxes", showAxes);
    vwr.setBooleanProperty("showBoundBox", showBoundingBox);
    vwr.setBooleanProperty("axesOrientationRasmol", axesOrientationRasmol);

    jmol.updateConsoleFont();

  }

  private String getProp(String key) {
    return currentProperties.getProperty(key);
  }

  private boolean getBoolean(String key) {
    return Boolean.parseBoolean(getProp(key));
  }

  class PrefsAction extends AbstractAction {

    public PrefsAction() {
      super("prefs");
      this.setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      setVisible(true);
    }
  }

  public void addActions(List<Action> list) {
    list.add(prefsAction);
  }

  protected Action getAction(String cmd) {
    return commands.get(cmd);
  }

  ItemListener checkBoxListener = new ItemListener() {

    //Component c;
    //AbstractButton b;

    @Override
    public void itemStateChanged(ItemEvent e) {

      JmolAbstractButton cb = (JmolAbstractButton) e.getSource();
      String key = cb.getKey();
      boolean isSelected = cb.isSelected();
      String strSelected = isSelected ? "true" : "false";
      if (key.equals("Prefs.showHydrogens")) {
        showHydrogens = isSelected;
        vwr.setBooleanProperty("showHydrogens", showHydrogens);
        currentProperties.put("showHydrogens", strSelected);
      } else if (key.equals("Prefs.showMeasurements")) {
        showMeasurements = isSelected;
        vwr.setBooleanProperty("showMeasurements", showMeasurements);
        currentProperties.put("showMeasurements", strSelected);
      } else if (key.equals("Prefs.perspectiveDepth")) {
        perspectiveDepth = isSelected;
        vwr.setBooleanProperty("perspectiveDepth", perspectiveDepth);
        currentProperties.put("perspectiveDepth", strSelected);
      } else if (key.equals("Prefs.showAxes")) {
        showAxes = isSelected;
        vwr.setBooleanProperty("showAxes", isSelected);
        vwr.refresh(Viewer.REFRESH_SYNC_MASK, "pref.showAxes");
        currentProperties.put("showAxes", strSelected);
      } else if (key.equals("Prefs.showBoundingBox")) {
        showBoundingBox = isSelected;
        vwr.setBooleanProperty("showBoundBox", isSelected);
        vwr.refresh(Viewer.REFRESH_SYNC_MASK, "pref.showBoundingBox");
        currentProperties.put("showBoundingBox", strSelected);
      } else if (key.equals("Prefs.axesOrientationRasmol")) {
        axesOrientationRasmol = isSelected;
        vwr.setBooleanProperty("axesOrientationRasmol", isSelected);
        currentProperties.put("axesOrientationRasmol", strSelected);
      } else if (key.equals("Prefs.openFilePreview")) {
        openFilePreview = isSelected;
        currentProperties.put("openFilePreview", strSelected);
      } else if (key.equals("Prefs.clearHistory")) {
        clearHistory = isSelected;
        currentProperties.put("clearHistory", strSelected);
        if (JmolPanel.historyFile != null)
          JmolPanel.historyFile.addProperty("clearHistory", strSelected);
        //      } else if (key.equals("Prefs.fontScale")) {
        //        setFontScale(strSelected);
      }
    }
  };

  private JButton applyButton;
  private JButton jmolDefaultsButton;
  private JButton rasmolDefaultsButton;
  private JButton cancelButton;
  private JButton okButton;

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == applyButton) {
      apply();
    } else if (event.getSource() == jmolDefaultsButton) {
      resetDefaults(null);
    } else if (event.getSource() == rasmolDefaultsButton) {
      resetDefaults(rasmolOverrides);
    } else if (event.getSource() == cancelButton) {
      cancel();
    } else if (event.getSource() == okButton) {
      ok();
    }
  }

  public void setFontScale(int scale) {
    fontScale = (scale == Integer.MIN_VALUE ? fontScale
        : scale < 0 ? fontScale + 1 : scale) % 5;
    currentProperties.put("consoleFontScale", "" + fontScale);
    save();
    jmol.updateConsoleFont();
  }

}
