///*  
// *  The Janocchio program is (C) 2007 Eli Lilly and Co.
// *  Authors: David Evans and Gary Sharman
// *  Contact : janocchio-users@lists.sourceforge.net.
// * 
// *  It is derived in part from Jmol 
// *  (C) 2002-2006 The Jmol Development Team
// *
// *  This program is free software; you can redistribute it and/or
// *  modify it under the terms of the GNU Lesser General Public License
// *  as published by the Free Software Foundation; either version 2.1
// *  of the License, or (at your option) any later version.
// *  All we ask is that proper credit is given for our work, which includes
// *  - but is not limited to - adding the above copyright notice to the beginning
// *  of your source code files, and to any copyright notice that you may distribute
// *  with programs based on this work.
// *
// *  This program is distributed in the hope that it will be useful, on an 'as is' basis,
// *  WITHOUT ANY WARRANTY; without even the implied warranty of
// *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// *  GNU Lesser General Public License for more details.
// *
// *  You should have received a copy of the GNU Lesser General Public License
// *  along with this program; if not, write to the Free Software
// *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// */
//package org.openscience.jmol.app.janocchio; import javax.swing.*;
//
//import java.awt.BorderLayout;
//import java.awt.Dimension;
//import java.awt.FlowLayout;
//import java.awt.GridBagConstraints;
//import java.awt.GridBagLayout;
//import java.awt.GridLayout;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.awt.event.ItemEvent;
//import java.awt.event.ItemListener;
//import java.io.BufferedInputStream;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.util.Hashtable;
//import java.util.Properties;
//
//import AbstractAction;
//import Action;
//import Box;
//import BoxLayout;
//import ButtonGroup;
//import JButton;
//import JCheckBox;
//import JDialog;
//import JFrame;
//import JLabel;
//import JPanel;
//import JRadioButton;
//import JSlider;
//import JTabbedPane;
//import SwingConstants;
//import border.TitledBorder;
//import javax.swing.event.ChangeEvent;
//import javax.swing.event.ChangeListener;
//
//import org.jmol.api.JmolViewer;
//import org.jmol.i18n.GT;
//import org.openscience.jmol.app.jmolpanel.NmrGuiMap;
//
//public class NmrPreferencesDialog {//extends JDialog implements ActionListener {
//
//	private boolean autoBond;
//	boolean showHydrogens;
//	boolean showMeasurements;
//	boolean perspectiveDepth;
//	boolean showAxes;
//	boolean showBoundingBox;
//	boolean axesOrientationRasmol;
//	boolean openFilePreview;
//	boolean clearConsoleButton;
//	float minBondDistance;
//	float bondTolerance;
//	short marBond;
//	int percentVdwAtom;
//	JButton bButton, pButton, tButton, eButton, vButton;
//	private JRadioButton /* pYes, pNo, */ abYes, abNo;
//	private JSlider vdwPercentSlider;
//	private JSlider bdSlider, bwSlider, btSlider;
//	private JCheckBox cH, cM;
//	private JCheckBox cbPerspectiveDepth;
//	private JCheckBox cbShowAxes, cbShowBoundingBox;
//	private JCheckBox cbAxesOrientationRasmol;
//	private JCheckBox cbOpenFilePreview;
//	private JCheckBox cbClearConsoleButton;
//	private Properties originalSystemProperties;
//	private Properties jmolDefaultProperties;
//	Properties currentProperties;
//
//	// The actions:
//
//	private PrefsAction prefsAction = new PrefsAction();
//	private Hashtable commands;
//
//	final static String[] jmolDefaults = { "jmolDefaults", "true", "showHydrogens", "true", "showMeasurements", "true",
//			"perspectiveDepth", "true", "showAxes", "false", "showBoundingBox", "false", "axesOrientationRasmol",
//			"false", "openFilePreview", "true", "percentVdwAtom", "20", "autoBond", "true", "marBond", "150",
//			"minBondDistance", "0.40", "bondTolerance", "0.45", };
//
//	final static String[] rasmolOverrides = { "jmolDefaults", "false", "percentVdwAtom", "0", "marBond", "1",
//			"axesOrientationRasmol", "true", };
//
//	JmolViewer viewer;
//	NmrGuiMap guimap;
//
//	public NmrPreferencesDialog(JFrame f, NmrGuiMap guimap, JmolViewer viewer) {
//
//		super(f, false);
//		this.guimap = guimap;
//		this.viewer = viewer;
//
//		initializeProperties();
//
//		this.setTitle(GT._("Preferences"));
//
//		initVariables();
//		commands = new Hashtable();
//		Action[] actions = getActions();
//		for (int i = 0; i < actions.length; i++) {
//			Action a = actions[i];
//			commands.put(a.getValue(Action.NAME), a);
//		}
//		JPanel container = new JPanel();
//		container.setLayout(new BorderLayout());
//
//		JTabbedPane tabs = new JTabbedPane();
//		JPanel disp = buildDispPanel();
//		JPanel atoms = buildAtomsPanel();
//		JPanel bonds = buildBondPanel();
//		// JPanel vibrate = buildVibratePanel();
//		tabs.addTab(GT._("Display"), null, disp);
//		tabs.addTab(GT._("Atoms"), null, atoms);
//		tabs.addTab(GT._("Bonds"), null, bonds);
//
//		JPanel buttonPanel = new JPanel();
//		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
//
//		jmolDefaultsButton = new JButton(GT._("Jmol Defaults"));
//		jmolDefaultsButton.addActionListener(this);
//		buttonPanel.add(jmolDefaultsButton);
//
//		rasmolDefaultsButton = new JButton(GT._("RasMol Defaults"));
//		rasmolDefaultsButton.addActionListener(this);
//		buttonPanel.add(rasmolDefaultsButton);
//
//		cancelButton = new JButton(GT._("Cancel"));
//		cancelButton.addActionListener(this);
//		buttonPanel.add(cancelButton);
//
//		applyButton = new JButton(GT._("Apply"));
//		applyButton.addActionListener(this);
//		buttonPanel.add(applyButton);
//
//		okButton = new JButton(GT._("OK"));
//		okButton.addActionListener(this);
//		buttonPanel.add(okButton);
//		getRootPane().setDefaultButton(okButton);
//
//		container.add(tabs, BorderLayout.CENTER);
//		container.add(buttonPanel, BorderLayout.SOUTH);
//		getContentPane().add(container);
//
//		updateComponents();
//
//		pack();
//		centerDialog();
//	}
//
//	public JPanel buildDispPanel() {
//
//		JPanel disp = new JPanel();
//		GridBagLayout gridbag = new GridBagLayout();
//		disp.setLayout(gridbag);
//		GridBagConstraints constraints;
//
//		JPanel showPanel = new JPanel();
//		showPanel.setLayout(new GridLayout(1, 3));
//		showPanel.setBorder(new TitledBorder(GT._("Show All")));
//		cH = guimap.newJCheckBox("Prefs.showHydrogens", viewer.getShowHydrogens());
//		cH.addItemListener(checkBoxListener);
//		cM = guimap.newJCheckBox("Prefs.showMeasurements", viewer.getShowMeasurements());
//		cM.addItemListener(checkBoxListener);
//		showPanel.add(cH);
//		showPanel.add(cM);
//
//		constraints = new GridBagConstraints();
//		constraints.gridwidth = GridBagConstraints.REMAINDER;
//		constraints.fill = GridBagConstraints.HORIZONTAL;
//		constraints.weightx = 1.0;
//		disp.add(showPanel, constraints);
//
//		JPanel fooPanel = new JPanel();
//		fooPanel.setBorder(new TitledBorder(""));
//		fooPanel.setLayout(new GridLayout(2, 1));
//
//		cbPerspectiveDepth = guimap.newJCheckBox("Prefs.perspectiveDepth", viewer.getPerspectiveDepth());
//		cbPerspectiveDepth.addItemListener(checkBoxListener);
//		fooPanel.add(cbPerspectiveDepth);
//
//		cbShowAxes = guimap.newJCheckBox("Prefs.showAxes", viewer.getShowAxes());
//		cbShowAxes.addItemListener(checkBoxListener);
//		fooPanel.add(cbShowAxes);
//
//		cbShowBoundingBox = guimap.newJCheckBox("Prefs.showBoundingBox", viewer.getShowBbcage());
//		cbShowBoundingBox.addItemListener(checkBoxListener);
//		fooPanel.add(cbShowBoundingBox);
//
//		constraints = new GridBagConstraints();
//		constraints.gridwidth = GridBagConstraints.REMAINDER;
//		constraints.fill = GridBagConstraints.HORIZONTAL;
//		constraints.weightx = 1.0;
//		disp.add(fooPanel, constraints);
//
//		JPanel axesPanel = new JPanel();
//		axesPanel.setBorder(new TitledBorder(""));
//		axesPanel.setLayout(new GridLayout(1, 1));
//
//		cbAxesOrientationRasmol = guimap.newJCheckBox("Prefs.axesOrientationRasmol", viewer.getAxesOrientationRasmol());
//		cbAxesOrientationRasmol.addItemListener(checkBoxListener);
//		axesPanel.add(cbAxesOrientationRasmol);
//
//		constraints = new GridBagConstraints();
//		constraints.gridwidth = GridBagConstraints.REMAINDER;
//		constraints.fill = GridBagConstraints.HORIZONTAL;
//		constraints.weightx = 1.0;
//		disp.add(axesPanel, constraints);
//
//		JPanel otherPanel = new JPanel();
//		otherPanel.setBorder(new TitledBorder(""));
//		otherPanel.setLayout(new GridLayout(2, 1));
//
//		cbOpenFilePreview = guimap.newJCheckBox("Prefs.openFilePreview", openFilePreview);
//		cbOpenFilePreview.addItemListener(checkBoxListener);
//		otherPanel.add(cbOpenFilePreview);
//
//		cbClearConsoleButton = guimap.newJCheckBox("Prefs.clearConsoleButton", clearConsoleButton);
//		cbClearConsoleButton.addItemListener(checkBoxListener);
//		otherPanel.add(cbClearConsoleButton);
//
//		constraints = new GridBagConstraints();
//		constraints.gridwidth = GridBagConstraints.REMAINDER;
//		constraints.fill = GridBagConstraints.HORIZONTAL;
//		constraints.weightx = 1.0;
//		disp.add(otherPanel, constraints);
//
//		JLabel filler = new JLabel();
//		constraints = new GridBagConstraints();
//		constraints.gridwidth = GridBagConstraints.REMAINDER;
//		constraints.gridheight = GridBagConstraints.REMAINDER;
//		constraints.fill = GridBagConstraints.BOTH;
//		constraints.weightx = 1.0;
//		constraints.weighty = 1.0;
//		disp.add(filler, constraints);
//
//		return disp;
//	}
//
//	public JPanel buildAtomsPanel() {
//
//		JPanel atomPanel = new JPanel(new GridBagLayout());
//		GridBagConstraints constraints;
//
//		JPanel sfPanel = new JPanel();
//		sfPanel.setLayout(new BorderLayout());
//		sfPanel.setBorder(new TitledBorder(GT._("Default atom size")));
//		JLabel sfLabel = new JLabel(GT._("(percentage of vanDerWaals radius)"), SwingConstants.CENTER);
//		sfPanel.add(sfLabel, BorderLayout.NORTH);
//		vdwPercentSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, viewer.getPercentVdwAtom());
//		vdwPercentSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
//		vdwPercentSlider.setPaintTicks(true);
//		vdwPercentSlider.setMajorTickSpacing(20);
//		vdwPercentSlider.setMinorTickSpacing(10);
//		vdwPercentSlider.setPaintLabels(true);
//		vdwPercentSlider.addChangeListener(new ChangeListener() {
//
//			public void stateChanged(ChangeEvent e) {
//
//				JSlider source = (JSlider) e.getSource();
//				percentVdwAtom = source.getValue();
//				viewer.setIntProperty("PercentVdwAtom", percentVdwAtom);
//				currentProperties.put("percentVdwAtom", "" + percentVdwAtom);
//			}
//		});
//		sfPanel.add(vdwPercentSlider, BorderLayout.CENTER);
//		constraints = new GridBagConstraints();
//		constraints.gridwidth = GridBagConstraints.REMAINDER;
//		constraints.fill = GridBagConstraints.HORIZONTAL;
//		constraints.weightx = 1.0;
//		atomPanel.add(sfPanel, constraints);
//
//		JLabel filler = new JLabel();
//		constraints = new GridBagConstraints();
//		constraints.gridwidth = GridBagConstraints.REMAINDER;
//		constraints.gridheight = GridBagConstraints.REMAINDER;
//		constraints.fill = GridBagConstraints.BOTH;
//		constraints.weightx = 1.0;
//		constraints.weighty = 1.0;
//		atomPanel.add(filler, constraints);
//
//		return atomPanel;
//	}
//
//	public JPanel buildBondPanel() {
//
//		JPanel bondPanel = new JPanel();
//		GridBagLayout gridbag = new GridBagLayout();
//		GridBagConstraints c = new GridBagConstraints();
//		bondPanel.setLayout(gridbag);
//		c.fill = GridBagConstraints.BOTH;
//		c.weightx = 1.0;
//		c.weighty = 1.0;
//
//		JPanel autobondPanel = new JPanel();
//		autobondPanel.setLayout(new BoxLayout(autobondPanel, BoxLayout.Y_AXIS));
//		autobondPanel.setBorder(new TitledBorder(GT._("Compute Bonds")));
//		ButtonGroup abGroup = new ButtonGroup();
//		abYes = new JRadioButton(GT._("Automatically"));
//		abNo = new JRadioButton(GT._("Don't Compute Bonds"));
//		abGroup.add(abYes);
//		abGroup.add(abNo);
//		autobondPanel.add(abYes);
//		autobondPanel.add(abNo);
//		autobondPanel.add(Box.createVerticalGlue());
//		abYes.setSelected(viewer.getAutoBond());
//		abYes.addActionListener(new ActionListener() {
//
//			public void actionPerformed(ActionEvent e) {
//				viewer.setBooleanProperty("autoBond", true);
//			}
//		});
//
//		abNo.addActionListener(new ActionListener() {
//
//			public void actionPerformed(ActionEvent e) {
//				viewer.setBooleanProperty("autoBond", false);
//			}
//		});
//
//		c.gridwidth = GridBagConstraints.REMAINDER;
//		gridbag.setConstraints(autobondPanel, c);
//		bondPanel.add(autobondPanel);
//
//		JPanel bwPanel = new JPanel();
//		bwPanel.setLayout(new BorderLayout());
//		bwPanel.setBorder(new TitledBorder(GT._("Default Bond Radius")));
//		JLabel bwLabel = new JLabel(GT._("(Angstroms)"), SwingConstants.CENTER);
//		bwPanel.add(bwLabel, BorderLayout.NORTH);
//
//		bwSlider = new JSlider(0, 250, viewer.getMadBond() / 2);
//		bwSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
//		bwSlider.setPaintTicks(true);
//		bwSlider.setMajorTickSpacing(50);
//		bwSlider.setMinorTickSpacing(25);
//		bwSlider.setPaintLabels(true);
//		for (int i = 0; i <= 250; i += 50) {
//			String label = "" + (1000 + i);
//			label = "0." + label.substring(1);
//			bwSlider.getLabelTable().put(new Integer(i), new JLabel(label, SwingConstants.CENTER));
//			bwSlider.setLabelTable(bwSlider.getLabelTable());
//		}
//		bwSlider.addChangeListener(new ChangeListener() {
//
//			public void stateChanged(ChangeEvent e) {
//
//				JSlider source = (JSlider) e.getSource();
//				marBond = (short) source.getValue();
//				viewer.setIntProperty("bondRadiusMilliAngstroms", marBond);
//				currentProperties.put("marBond", "" + marBond);
//			}
//		});
//
//		bwPanel.add(bwSlider, BorderLayout.SOUTH);
//
//		c.weightx = 0.0;
//		gridbag.setConstraints(bwPanel, c);
//		bondPanel.add(bwPanel);
//
//		// Bond Tolerance Slider
//		JPanel btPanel = new JPanel();
//		btPanel.setLayout(new BorderLayout());
//		btPanel.setBorder(new TitledBorder(GT._("Bond Tolerance - sum of two covalent radii + this value")));
//		JLabel btLabel = new JLabel(GT._("(Angstroms)"), SwingConstants.CENTER);
//		btPanel.add(btLabel, BorderLayout.NORTH);
//
//		btSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, (int) (100 * viewer.getBondTolerance()));
//		btSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
//		btSlider.setPaintTicks(true);
//		btSlider.setMajorTickSpacing(20);
//		btSlider.setMinorTickSpacing(10);
//		btSlider.setPaintLabels(true);
//		btSlider.getLabelTable().put(new Integer(0), new JLabel("0.0", SwingConstants.CENTER));
//		btSlider.setLabelTable(btSlider.getLabelTable());
//		btSlider.getLabelTable().put(new Integer(20), new JLabel("0.2", SwingConstants.CENTER));
//		btSlider.setLabelTable(btSlider.getLabelTable());
//		btSlider.getLabelTable().put(new Integer(40), new JLabel("0.4", SwingConstants.CENTER));
//		btSlider.setLabelTable(btSlider.getLabelTable());
//		btSlider.getLabelTable().put(new Integer(60), new JLabel("0.6", SwingConstants.CENTER));
//		btSlider.setLabelTable(btSlider.getLabelTable());
//		btSlider.getLabelTable().put(new Integer(80), new JLabel("0.8", SwingConstants.CENTER));
//		btSlider.setLabelTable(btSlider.getLabelTable());
//		btSlider.getLabelTable().put(new Integer(100), new JLabel("1.0", SwingConstants.CENTER));
//		btSlider.setLabelTable(btSlider.getLabelTable());
//
//		btSlider.addChangeListener(new ChangeListener() {
//
//			public void stateChanged(ChangeEvent e) {
//
//				JSlider source = (JSlider) e.getSource();
//				bondTolerance = source.getValue() / 100f;
//				viewer.setFloatProperty("bondTolerance", bondTolerance);
//				currentProperties.put("bondTolerance", "" + bondTolerance);
//				viewer.rebond();
//			}
//		});
//		btPanel.add(btSlider);
//
//		c.weightx = 0.0;
//		gridbag.setConstraints(btPanel, c);
//		bondPanel.add(btPanel);
//
//		// minimum bond distance slider
//		JPanel bdPanel = new JPanel();
//		bdPanel.setLayout(new BorderLayout());
//		bdPanel.setBorder(new TitledBorder(GT._("Minimum Bonding Distance")));
//		JLabel bdLabel = new JLabel(GT._("(Angstroms)"), SwingConstants.CENTER);
//		bdPanel.add(bdLabel, BorderLayout.NORTH);
//
//		bdSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, (int) (100 * viewer.getMinBondDistance()));
//		bdSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
//		bdSlider.setPaintTicks(true);
//		bdSlider.setMajorTickSpacing(20);
//		bdSlider.setMinorTickSpacing(10);
//		bdSlider.setPaintLabels(true);
//		bdSlider.getLabelTable().put(new Integer(0), new JLabel("0.0", SwingConstants.CENTER));
//		bdSlider.setLabelTable(bdSlider.getLabelTable());
//		bdSlider.getLabelTable().put(new Integer(20), new JLabel("0.2", SwingConstants.CENTER));
//		bdSlider.setLabelTable(bdSlider.getLabelTable());
//		bdSlider.getLabelTable().put(new Integer(40), new JLabel("0.4", SwingConstants.CENTER));
//		bdSlider.setLabelTable(bdSlider.getLabelTable());
//		bdSlider.getLabelTable().put(new Integer(60), new JLabel("0.6", SwingConstants.CENTER));
//		bdSlider.setLabelTable(bdSlider.getLabelTable());
//		bdSlider.getLabelTable().put(new Integer(80), new JLabel("0.8", SwingConstants.CENTER));
//		bdSlider.setLabelTable(bdSlider.getLabelTable());
//		bdSlider.getLabelTable().put(new Integer(100), new JLabel("1.0", SwingConstants.CENTER));
//		bdSlider.setLabelTable(bdSlider.getLabelTable());
//
//		bdSlider.addChangeListener(new ChangeListener() {
//
//			public void stateChanged(ChangeEvent e) {
//
//				JSlider source = (JSlider) e.getSource();
//				minBondDistance = source.getValue() / 100f;
//				viewer.setFloatProperty("minBondDistance", minBondDistance);
//				currentProperties.put("minBondDistance", "" + minBondDistance);
//				viewer.rebond();
//			}
//		});
//		bdPanel.add(bdSlider);
//
//		c.weightx = 0.0;
//		gridbag.setConstraints(bdPanel, c);
//		bondPanel.add(bdPanel);
//
//		return bondPanel;
//	}
//
//	protected void centerDialog() {
//
//		Dimension screenSize = this.getToolkit().getScreenSize();
//		Dimension size = this.getSize();
//		screenSize.height = screenSize.height / 2;
//		screenSize.width = screenSize.width / 2;
//		size.height = size.height / 2;
//		size.width = size.width / 2;
//		int y = screenSize.height - size.height;
//		int x = screenSize.width - size.width;
//		this.setLocation(x, y);
//	}
//
//	public void ok() {
//		save();
//		dispose();
//	}
//
//	public void cancel() {
//		updateComponents();
//		dispose();
//	}
//
//	private void updateComponents() {
//		// Display panel
//		cH.setSelected(viewer.getShowHydrogens());
//		cM.setSelected(viewer.getShowMeasurements());
//
//		cbPerspectiveDepth.setSelected(viewer.getPerspectiveDepth());
//		cbShowAxes.setSelected(viewer.getShowAxes());
//		cbShowBoundingBox.setSelected(viewer.getShowBbcage());
//
//		cbAxesOrientationRasmol.setSelected(viewer.getAxesOrientationRasmol());
//
//		cbOpenFilePreview.setSelected(openFilePreview);
//		cbClearConsoleButton.setSelected(clearConsoleButton);
//
//		// Atom panel controls:
//		vdwPercentSlider.setValue(viewer.getPercentVdwAtom());
//
//		// Bond panel controls:
//		abYes.setSelected(viewer.getAutoBond());
//		bwSlider.setValue(viewer.getMadBond() / 2);
//		bdSlider.setValue((int) (100 * viewer.getMinBondDistance()));
//		btSlider.setValue((int) (100 * viewer.getBondTolerance()));
//
//	}
//
//	private void save() {
//		try {
//			FileOutputStream fileOutputStream = new FileOutputStream(Jmol.UserPropsFile);
//			currentProperties.store(fileOutputStream, "Jmol");
//			fileOutputStream.close();
//		} catch (Exception e) {
//			System.out.println("Error saving preferences" + e);
//		}
//		viewer.refresh(0, "PreferencesDialog:save()");
//	}
//
//	void initializeProperties() {
//		originalSystemProperties = System.getProperties();
//		jmolDefaultProperties = new Properties(originalSystemProperties);
//		for (int i = jmolDefaults.length; (i -= 2) >= 0;)
//			jmolDefaultProperties.put(jmolDefaults[i], jmolDefaults[i + 1]);
//		currentProperties = new Properties(jmolDefaultProperties);
//		try {
//			FileInputStream fis2 = new FileInputStream(Jmol.UserPropsFile);
//			currentProperties.load(new BufferedInputStream(fis2, 1024));
//			fis2.close();
//		} catch (Exception e2) {
//		}
//		System.setProperties(currentProperties);
//	}
//
//	void resetDefaults(String[] overrides) {
//		currentProperties = new Properties(jmolDefaultProperties);
//		System.setProperties(currentProperties);
//		if (overrides != null) {
//			for (int i = overrides.length; (i -= 2) >= 0;)
//				currentProperties.put(overrides[i], overrides[i + 1]);
//		}
//		initVariables();
//		viewer.refresh(0, "PreferencesDialog:resetDefaults()");
//		updateComponents();
//	}
//
//	void initVariables() {
//
//		autoBond = Boolean.getBoolean("autoBond");
//		showHydrogens = Boolean.getBoolean("showHydrogens");
//		// showVectors = Boolean.getBoolean("showVectors");
//		showMeasurements = Boolean.getBoolean("showMeasurements");
//		perspectiveDepth = Boolean.getBoolean("perspectiveDepth");
//		showAxes = Boolean.getBoolean("showAxes");
//		showBoundingBox = Boolean.getBoolean("showBoundingBox");
//		axesOrientationRasmol = Boolean.getBoolean("axesOrientationRasmol");
//		openFilePreview = Boolean.valueOf(System.getProperty("openFilePreview", "true")).booleanValue();
//		clearConsoleButton = Boolean.getBoolean("clearConsoleButton");
//
//		minBondDistance = new Float(currentProperties.getProperty("minBondDistance")).floatValue();
//		bondTolerance = new Float(currentProperties.getProperty("bondTolerance")).floatValue();
//		marBond = Short.parseShort(currentProperties.getProperty("marBond"));
//		percentVdwAtom = Integer.parseInt(currentProperties.getProperty("percentVdwAtom"));
//
//		/*
//		 * if (Boolean.getBoolean("jmolDefaults")) viewer.setStringProperty("defaults",
//		 * "Jmol"); else viewer.setStringProperty("defaults", "RasMol");
//		 */
//		viewer.setIntProperty("percentVdwAtom", percentVdwAtom);
//		viewer.setIntProperty("bondRadiusMilliAngstroms", marBond);
//		viewer.setFloatProperty("minBondDistance", minBondDistance);
//		viewer.setFloatProperty("BondTolerance", bondTolerance);
//		viewer.setBooleanProperty("autoBond", autoBond);
//		viewer.setBooleanProperty("showHydrogens", showHydrogens);
//		viewer.setBooleanProperty("showMeasurements", showMeasurements);
//		viewer.setBooleanProperty("perspectiveDepth", perspectiveDepth);
//		viewer.setBooleanProperty("showAxes", showAxes);
//		viewer.setBooleanProperty("showBoundBox", showBoundingBox);
//		viewer.setBooleanProperty("axesOrientationRasmol", axesOrientationRasmol);
//	}
//
//	class PrefsAction extends AbstractAction {
//
//		public PrefsAction() {
//			super("prefs");
//			this.setEnabled(true);
//		}
//
//		public void actionPerformed(ActionEvent e) {
//			show();
//		}
//	}
//
//	public Action[] getActions() {
//		Action[] defaultActions = { prefsAction };
//		return defaultActions;
//	}
//
//	protected Action getAction(String cmd) {
//		return (Action) commands.get(cmd);
//	}
//
//	ItemListener checkBoxListener = new ItemListener() {
//
//		// Component c;
//		// AbstractButton b;
//
//		public void itemStateChanged(ItemEvent e) {
//
//			JCheckBox cb = (JCheckBox) e.getSource();
//			String key = guimap.getKey(cb);
//			boolean isSelected = cb.isSelected();
//			String strSelected = isSelected ? "true" : "false";
//			if (key.equals("Prefs.showHydrogens")) {
//				showHydrogens = isSelected;
//				viewer.setBooleanProperty("showHydrogens", showHydrogens);
//				currentProperties.put("showHydrogens", strSelected);
//			} else if (key.equals("Prefs.showMeasurements")) {
//				showMeasurements = isSelected;
//				viewer.setBooleanProperty("showMeasurements", showMeasurements);
//				currentProperties.put("showMeasurements", strSelected);
//			} else if (key.equals("Prefs.perspectiveDepth")) {
//				perspectiveDepth = isSelected;
//				viewer.setBooleanProperty("perspectiveDepth", perspectiveDepth);
//				currentProperties.put("perspectiveDepth", strSelected);
//			} else if (key.equals("Prefs.showAxes")) {
//				showAxes = isSelected;
//				viewer.setBooleanProperty("showAxes", isSelected);
//				currentProperties.put("showAxes", strSelected);
//			} else if (key.equals("Prefs.showBoundingBox")) {
//				showBoundingBox = isSelected;
//				viewer.setBooleanProperty("showBoundBox", isSelected);
//				currentProperties.put("showBoundingBox", strSelected);
//			} else if (key.equals("Prefs.axesOrientationRasmol")) {
//				axesOrientationRasmol = isSelected;
//				viewer.setBooleanProperty("axesOrientationRasmol", isSelected);
//				currentProperties.put("axesOrientationRasmol", strSelected);
//			} else if (key.equals("Prefs.openFilePreview")) {
//				openFilePreview = isSelected;
//				currentProperties.put("openFilePreview", strSelected);
//			} else if (key.equals("Prefs.clearConsoleButton")) {
//				clearConsoleButton = isSelected;
//				currentProperties.put("clearConsoleButton", strSelected);
//			}
//		}
//	};
//
//	private JButton applyButton;
//	private JButton jmolDefaultsButton;
//	private JButton rasmolDefaultsButton;
//	private JButton cancelButton;
//	private JButton okButton;
//
//	public void actionPerformed(ActionEvent event) {
//		if (event.getSource() == applyButton) {
//			save();
//		} else if (event.getSource() == jmolDefaultsButton) {
//			resetDefaults(null);
//		} else if (event.getSource() == rasmolDefaultsButton) {
//			resetDefaults(rasmolOverrides);
//		} else if (event.getSource() == cancelButton) {
//			cancel();
//		} else if (event.getSource() == okButton) {
//			ok();
//		}
//	}
//
//}
