/* Copyright (c) 2002-2013 The University of the West Indies
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

package jspecview.application;

import java.awt.BorderLayout;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;


import javajs.util.CU;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jspecview.common.ColorParameters;
import jspecview.common.JSViewer;
import jspecview.common.ScriptToken;
import jspecview.exception.JSVException;
import jspecview.java.AwtColor;
import jspecview.java.AwtPanel;
import jspecview.java.AwtParameters;
import jspecview.source.JDXReader;
import jspecview.source.JDXSource;
import java.awt.Font;
import javax.swing.UIManager;
import javax.swing.JSeparator;

/**
 * Dialog to change the preferences for the application.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */

public class PreferencesDialog extends JDialog implements ActionListener {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  JPanel contentpanel = new JPanel();
  BorderLayout contentBorderLayout = new BorderLayout();
  JTabbedPane preferencesTabbedPane = new JTabbedPane();
  JPanel generalPanel = new JPanel();
  JPanel displayPanel = new JPanel();
  TitledBorder fontTitledBorder;
  TitledBorder contentTitledBorder;
  JCheckBox confirmExitCheckBox = new JCheckBox();
  JCheckBox statusBarCheckBox = new JCheckBox();
  JCheckBox toolbarCheckBox = new JCheckBox();
  JCheckBox sidePanelCheckBox = new JCheckBox();
  JCheckBox exportDirCheckBox = new JCheckBox();
  JCheckBox openedDirCheckBox = new JCheckBox();
  JCheckBox legendCheckBox = new JCheckBox();
  JCheckBox overlayCheckBox = new JCheckBox();
  JButton clearRecentButton = new JButton();
  JButton cancelButton = new JButton();
  JPanel buttonPanel = new JPanel();
  JButton okButton = new JButton();
  private BorderLayout borderLayout6 = new BorderLayout();
  private DefaultListModel<String> elModel = new DefaultListModel<String>();
  private JPanel topPanel = new JPanel();
  private JPanel displayFontPanel = new JPanel();
  private JPanel colorSchemePanel = new JPanel();
  private JPanel elementPanel = new JPanel();
  private JLabel elementLabel = new JLabel();
  private GridBagLayout gridBagLayout1 = new GridBagLayout();
  private JCheckBox defaultFontCheckBox = new JCheckBox();
  private JComboBox<String> fontComboBox = new JComboBox<String>();
  private GridBagLayout gridBagLayout2 = new GridBagLayout();
  private JPanel colorPanel = new JPanel();
  private JComboBox<String> schemeComboBox = new JComboBox<String>();
  private GridLayout gridLayout1 = new GridLayout();
  private JCheckBox defaultColorCheckBox = new JCheckBox();
  private JButton customButton = new JButton();
  private GridBagLayout gridBagLayout3 = new GridBagLayout();
  private JScrollPane listScrollPane = new JScrollPane();
  JList<String> elementList = new JList<String>();
  private JButton colorButton8 = new JButton();
  private JButton colorButton7 = new JButton();
  private JButton colorButton6 = new JButton();
  private JButton colorButton5 = new JButton();
  private JButton colorButton4 = new JButton();
  private JButton colorButton3 = new JButton();
  private JButton colorButton2 = new JButton();
  private JButton colorButton1 = new JButton();
  JButton currentColorButton = new JButton();
  private JPanel processingPanel = new JPanel();
  private GridBagLayout gridBagLayout4 = new GridBagLayout();
  private JButton saveButton = new JButton();
  private GridBagLayout gridBagLayout5 = new GridBagLayout();
  private JPanel integrationPanel = new JPanel();
  private JPanel absTransPanel = new JPanel();
  private GridBagLayout gridBagLayout6 = new GridBagLayout();
  private TitledBorder integratinTitledBorder;
  private JLabel jLabel1 = new JLabel();
  private JTextField minYTextField = new JTextField();
  private JLabel jLabel2 = new JLabel();
  private JTextField integFactorTextField = new JTextField();
  private JLabel jLabel3 = new JLabel();
  private JLabel jLabel4 = new JLabel();
  private JButton processingCustomButton = new JButton();
  private JCheckBox autoIntegrateCheckBox = new JCheckBox();
  private JTextField integOffsetTextField = new JTextField();
  private TitledBorder absTransTitledBorder;
  private JCheckBox separateWindowCheckBox = new JCheckBox();
  //private ButtonGroup fontButtonGroup = new ButtonGroup();
  private JLabel jLabel5 = new JLabel();
  private JLabel jLabel6 = new JLabel();
  private JLabel jLabel7 = new JLabel();
  private GridBagLayout gridBagLayout7 = new GridBagLayout();
  private JRadioButton TtoARadioButton = new JRadioButton();
  private JRadioButton AtoTRadioButton = new JRadioButton();
  private ButtonGroup conversionButtonGroup = new ButtonGroup();

  ColorParameters currentDS = (ColorParameters) new AwtParameters().setName("Current");
  private DisplaySchemesProcessor dsp;
  private AwtPanel previewPanel = null;
  private String defaultDSName = "";
  private JLabel jLabel8 = new JLabel();
  private JLabel jLabel9 = new JLabel();
  //private JColorChooser cc = new JColorChooser();
  private JCheckBox AutoConvertCheckBox = new JCheckBox();
  //private boolean clearRecentFiles = false;
  JButton plotColorButton = new JButton();
  private JPanel colorPanel1 = new JPanel();
  private JButton procColorButton8 = new JButton();
  private JButton procColorButton7 = new JButton();
  private JButton procColorButton6 = new JButton();
  private JButton procColorButton5 = new JButton();
  private JButton procColorButton4 = new JButton();
  private JButton procColorButton3 = new JButton();
  private JButton procColorButton2 = new JButton();
  private GridLayout gridLayout2 = new GridLayout();
  private JButton procColorButton1 = new JButton();

  private JCheckBox gridCheckBox = new JCheckBox();
  private JCheckBox coordinatesCheckBox = new JCheckBox();
  private JCheckBox scaleXCheckBox = new JCheckBox();
  private JCheckBox svgForInkscapeCheckBox = new JCheckBox();
  private JPanel applicationPanel;
  private JPanel uiPanel;
  private final JPanel spectrumPanel = new JPanel();
  private final JCheckBox spectrumDisplayApplyNowCheckBox = new JCheckBox("Apply to currently opened spectra");

  
  private Properties preferences;
  private boolean isSpectrumDisplayApplyNowEnabled = false;
  private final JCheckBox scaleYCheckBox = new JCheckBox();
  private final JPanel panel = new JPanel();
  private final JButton deleteButton = new JButton("Delete Scheme");
  
  /**
   * Initialises the <code>PreferencesDialog</code>
   * @param frame the the parent frame
   * @param viewer 
   * @param title the title
   * @param modal the modality
   * @param dsp an instance of <code>DisplaySchemesProcessor</code>
   */
  public PreferencesDialog(Frame frame, JSViewer viewer, String title, boolean modal,
                           DisplaySchemesProcessor dsp) {
    super(frame, title, modal);
    setSize(new Dimension(480, 571));
    
    setLocationRelativeTo(frame);

    preferences = viewer.properties;
    this.dsp = dsp;

    elModel.addElement("Title");
    elModel.addElement("Plot");
    elModel.addElement("Scale");
    elModel.addElement("Units");
    elModel.addElement("Coordinates");
    elModel.addElement("PlotArea");
    elModel.addElement("Background");
    elModel.addElement("Grid");



    try {
      jbInit();      
      if(dsp != null){
        initDisplayTab(viewer);
      }
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }

    initProcessingTab();
    initGeneralTab();

    elementList.addListSelectionListener(new ElementListSelectionListener());
    elementList.getSelectionModel().setSelectionInterval(0, 0);

    setVisible(true);
  }
  
  /**
   * Initialises GUI components
   * @throws Exception
   */
  void jbInit() throws Exception {
    fontTitledBorder = new TitledBorder("");
    contentTitledBorder = new TitledBorder("");
    integratinTitledBorder = new TitledBorder("");
    absTransTitledBorder = new TitledBorder("");
    contentpanel.setLayout(contentBorderLayout);
    fontTitledBorder.setTitle("Font");
    fontTitledBorder.setTitleJustification(2);
    contentTitledBorder.setTitle("Content");
    contentTitledBorder.setTitleJustification(2);
    gridCheckBox.setToolTipText("");
    gridCheckBox.setText("Show grid");
    coordinatesCheckBox.setText("Show coordinates");
    scaleXCheckBox.setText("Show X scale");
    svgForInkscapeCheckBox.setText("SVG export for Inkscape");
    generalPanel.setBorder(BorderFactory.createEtchedBorder());
    statusBarCheckBox.setText("Show status bar");
    toolbarCheckBox.setText("Show toolbar");
    exportDirCheckBox.setText("Remember directory of last exported file");
    openedDirCheckBox.setText("Remember directory of last opened file");
    legendCheckBox.setText("Automatically show legend when spectra are overlaid");
    confirmExitCheckBox.setText("Confirm before exiting");
    sidePanelCheckBox.setText("Show side panel");
    
    applicationPanel = new JPanel();
    applicationPanel.setLayout(null);
    applicationPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Application", TitledBorder.CENTER, TitledBorder.TOP, null, Color.BLACK));
    applicationPanel.setBounds(12, 5, 437, 157);
    generalPanel.add(applicationPanel);
        
    uiPanel = new JPanel();
    uiPanel.setLayout(null);
    uiPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "User Interface", TitledBorder.CENTER, TitledBorder.TOP, null, Color.BLACK));
    uiPanel.setBounds(12, 164, 437, 101);
    generalPanel.add(uiPanel);
       
    confirmExitCheckBox.setBounds(6, 19, 133, 23);
    applicationPanel.add(confirmExitCheckBox);
    openedDirCheckBox.setBounds(6, 45, 211, 23);
    applicationPanel.add(openedDirCheckBox);
    exportDirCheckBox.setBounds(6, 71, 219, 23);
    applicationPanel.add(exportDirCheckBox);
    svgForInkscapeCheckBox.setBounds(6, 97, 143, 23);
    applicationPanel.add(svgForInkscapeCheckBox);
    clearRecentButton.setBounds(6, 127, 119, 23);
    applicationPanel.add(clearRecentButton);
    clearRecentButton.setText("Clear Recent Files");
    overlayCheckBox.setBounds(6, 150, 241, 23); 
    spectrumPanel.add(overlayCheckBox);
    legendCheckBox.setBounds(6, 126, 291, 23);
    spectrumPanel.add(legendCheckBox);
    gridCheckBox.setBounds(6, 22, 73, 23);
    spectrumPanel.add(gridCheckBox);
    scaleXCheckBox.setBounds(6, 48, 97, 23);
    spectrumPanel.add(scaleXCheckBox);
    coordinatesCheckBox.setBounds(142, 22, 111, 23);
    spectrumPanel.add(coordinatesCheckBox);
    
   
    clearRecentButton.addActionListener(new java.awt.event.ActionListener() {
      @Override
			public void actionPerformed(ActionEvent e) {
        clearRecentButton_actionPerformed(e);
      }
    });
    
    sidePanelCheckBox.setBounds(6, 21, 103, 23);
    uiPanel.add(sidePanelCheckBox);
    toolbarCheckBox.setBounds(6, 47, 89, 23);
    uiPanel.add(toolbarCheckBox);
    statusBarCheckBox.setBounds(6, 73, 114, 23);
    uiPanel.add(statusBarCheckBox);
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(new java.awt.event.ActionListener() {
      @Override
			public void actionPerformed(ActionEvent e) {
        cancelButton_actionPerformed(e);
      }
    });
    okButton.setText("OK");
    okButton.addActionListener(new java.awt.event.ActionListener() {
      @Override
			public void actionPerformed(ActionEvent e) {
        okButton_actionPerformed(e);
      }
    });
    displayPanel.setLayout(borderLayout6);
    gridBagLayout4.rowWeights = new double[]{0.0, 1.0};
    gridBagLayout4.columnWeights = new double[]{0.0, 0.0, 1.0};
    topPanel.setLayout(gridBagLayout4);
    elementPanel.setBorder(BorderFactory.createEtchedBorder());
    elementPanel.setLayout(gridBagLayout1);
    colorSchemePanel.setBorder(BorderFactory.createEtchedBorder());
    colorSchemePanel.setLayout(gridBagLayout3);
    displayFontPanel.setBorder(BorderFactory.createEtchedBorder());
    displayFontPanel.setLayout(gridBagLayout2);
    elementLabel.setText("Element:");
    defaultFontCheckBox.setText("Use Default");
    defaultFontCheckBox.addActionListener(new java.awt.event.ActionListener() {
      @Override
			public void actionPerformed(ActionEvent e) {
        defaultFontCheckBox_actionPerformed(e);
      }
    });
    colorPanel.setLayout(gridLayout1);
    schemeComboBox.setMaximumSize(new Dimension(200, 21));
    schemeComboBox.addActionListener(new java.awt.event.ActionListener() {
      @Override
			public void actionPerformed(ActionEvent e) {
        schemeComboBox_actionPerformed(e);
      }
    });
    gridLayout1.setHgap(2);
    gridLayout1.setRows(2);
    gridLayout1.setVgap(2);
    defaultColorCheckBox.setText("Use Default");
    defaultColorCheckBox.addActionListener(new java.awt.event.ActionListener() {
      @Override
			public void actionPerformed(ActionEvent e) {
        defaultColorCheckBox_actionPerformed(e);
      }
    });
    customButton.setText("Custom...");
    customButton.addActionListener(new java.awt.event.ActionListener() {
      @Override
			public void actionPerformed(ActionEvent e) {
        customButton_actionPerformed(e);
      }
    });
    elementList.setToolTipText("");
    elementList.setModel(elModel);
    elementList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    elementList.setVisibleRowCount(4);
    listScrollPane.setMinimumSize(new Dimension(125, 110));
    listScrollPane.setPreferredSize(new Dimension(125, 110));
    addColorButton(colorButton1, Color.black);
    addColorButton(colorButton2, Color.white);
    addColorButton(colorButton3, Color.gray);
    addColorButton(colorButton4, Color.blue);
    addColorButton(colorButton5, Color.red);
    addColorButton(colorButton6, new Color(0, 0, 64));
    addColorButton(colorButton7, new Color(0, 92, 0));
    addColorButton(colorButton8, Color.magenta);
    colorButton3.setText(" ");
        currentColorButton.setBorder(BorderFactory.createLoweredBevelBorder());
    currentColorButton.setMaximumSize(new Dimension(50, 11));
    currentColorButton.setMinimumSize(new Dimension(50, 11));
    currentColorButton.setPreferredSize(new Dimension(50, 11));
    currentColorButton.setMnemonic('0');
    processingPanel.setLayout(gridBagLayout5);
    integrationPanel.setLayout(gridBagLayout6);
    integrationPanel.setBorder(integratinTitledBorder);
    integratinTitledBorder.setTitle("Integration");
    integratinTitledBorder.setTitleJustification(2);
    jLabel1.setText("Integral Factor");
    jLabel2.setText("Minimum Y");
    jLabel3.setText("Integral Offset");
    jLabel4.setText("Plot Color");
    processingCustomButton.setPreferredSize(new Dimension(87, 21));
    processingCustomButton.setText("Custom...");
    processingCustomButton.addActionListener(new java.awt.event.ActionListener() {
      @Override
			public void actionPerformed(ActionEvent e) {
        processingCustomButton_actionPerformed(e);
      }
    });
    autoIntegrateCheckBox.setText("Automatically Integrate HNMR Spectra");
    minYTextField.setMinimumSize(new Dimension(40, 21));
    minYTextField.setPreferredSize(new Dimension(40, 21));
    integFactorTextField.setMinimumSize(new Dimension(40, 21));
    integFactorTextField.setPreferredSize(new Dimension(40, 21));
    integOffsetTextField.setMinimumSize(new Dimension(40, 21));
    integOffsetTextField.setPreferredSize(new Dimension(40, 21));
    absTransPanel.setBorder(absTransTitledBorder);
    absTransPanel.setLayout(gridBagLayout7);
    absTransTitledBorder.setTitle("Absorbance/Transmittance");
    absTransTitledBorder.setTitleJustification(2);
    separateWindowCheckBox.setEnabled(false);
    separateWindowCheckBox.setText("Show converted Spectrum in a separate window");
    jLabel5.setText("%");
    jLabel6.setText("%");
    jLabel7.setText("%");
    TtoARadioButton.setSelected(true);
    TtoARadioButton.setText("Transmittance to Absorbance");
    AtoTRadioButton.setText("Absorbance to Transmittance");
    colorPanel.setBorder(BorderFactory.createRaisedBevelBorder());
    jLabel8.setText("Color Scheme:");
    jLabel9.setText("Font:");
    fontComboBox.addActionListener(new java.awt.event.ActionListener() {
      @Override
			public void actionPerformed(ActionEvent e) {
        fontComboBox_actionPerformed(e);
      }
    });
    AutoConvertCheckBox.setToolTipText("");
    AutoConvertCheckBox.setText("Automatically Convert");
    plotColorButton.setBackground(Color.green);
    plotColorButton.setBorder(null);
    plotColorButton.setPreferredSize(new Dimension(30, 21));
    colorPanel1.setBorder(BorderFactory.createRaisedBevelBorder());
    colorPanel1.setLayout(gridLayout2);
    
    addProcColorButton(procColorButton1, Color.black);
    addProcColorButton(procColorButton2, Color.white);
    addProcColorButton(procColorButton3, Color.gray);
    addProcColorButton(procColorButton4, Color.blue);
    addProcColorButton(procColorButton5, Color.red);
    addProcColorButton(procColorButton6, new Color(0, 0, 64));
    addProcColorButton(procColorButton7, new Color(0, 92, 0));
    addProcColorButton(procColorButton8, Color.magenta);
    
    gridLayout2.setHgap(2);
    gridLayout2.setRows(2);
    gridLayout2.setVgap(2);

    displayFontPanel.add(fontComboBox,                    new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0));
    displayFontPanel.add(defaultFontCheckBox,                  new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(10, 5, 0, 0), 0, 0));
    displayFontPanel.add(jLabel9,      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 5));
    getContentPane().add(contentpanel);
    contentpanel.add(preferencesTabbedPane, BorderLayout.CENTER);
    preferencesTabbedPane.add(generalPanel,   "General");
    preferencesTabbedPane.add(displayPanel,     "Display Scheme");
    displayPanel.add(topPanel,  BorderLayout.NORTH);
    topPanel.add(elementPanel,    new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 5, 5), 13, 0));
    elementPanel.add(listScrollPane,          new GridBagConstraints(0, 1, 1, 1, 0.0, 1.0
            ,GridBagConstraints.NORTH, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
    listScrollPane.setViewportView(elementList);
    elementPanel.add(elementLabel,           new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0));
    colorSchemePanel.add(schemeComboBox,          new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0));
    colorSchemePanel.add(defaultColorCheckBox,  new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
    colorSchemePanel.add(customButton,         new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
    colorSchemePanel.add(colorPanel,         new GridBagConstraints(0, 2, 2, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 5), 0, 0));
    colorSchemePanel.add(currentColorButton,        new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), -16, 0));
    colorSchemePanel.add(jLabel8,    new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 5));
    topPanel.add(colorSchemePanel,    new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 10, 5, 5), 8, 17));
    topPanel.add(displayFontPanel,    new GridBagConstraints(2, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 10, 5, 1), 8, 36));
    preferencesTabbedPane.add(processingPanel,  "Processing");
    processingPanel.add(integrationPanel,            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 10, 0), 0, 30));
    contentpanel.add(buttonPanel,  BorderLayout.SOUTH);
    buttonPanel.add(okButton, null);
    buttonPanel.add(cancelButton, null);
    generalPanel.setLayout(null);
    spectrumPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Default Spectrum Display Settings", TitledBorder.CENTER, TitledBorder.TOP, null, new Color(0, 0, 0)));
    spectrumPanel.setBounds(12, 266, 437, 184);
    
    generalPanel.add(spectrumPanel);
    spectrumPanel.setLayout(null);
    overlayCheckBox.setText("Show compound files as overlaid if possible");
    
    JLabel lblTheFollowingProperties = new JLabel("The spectrum display properties above will apply for new spectra");
    lblTheFollowingProperties.setFont(new Font("Tahoma", Font.ITALIC, 11));
    lblTheFollowingProperties.setForeground(Color.DARK_GRAY);
    lblTheFollowingProperties.setBounds(6, 74, 389, 14);
    spectrumPanel.add(lblTheFollowingProperties);
    spectrumDisplayApplyNowCheckBox.setFont(new Font("Tahoma", Font.ITALIC, 11));
    spectrumDisplayApplyNowCheckBox.setForeground(Color.DARK_GRAY);
    spectrumDisplayApplyNowCheckBox.setBounds(6, 87, 220, 23);
    
    spectrumPanel.add(spectrumDisplayApplyNowCheckBox);
    scaleYCheckBox.setText("Show Y scale");
    scaleYCheckBox.setSelected(false);
    scaleYCheckBox.setBounds(142, 48, 105, 23);
    
    spectrumPanel.add(scaleYCheckBox);
    
    JSeparator separator = new JSeparator();
    separator.setBounds(6, 117, 421, 2);
    spectrumPanel.add(separator);
    
    GridBagConstraints gbc_panel = new GridBagConstraints();
    gbc_panel.gridwidth = 5;
    gbc_panel.fill = GridBagConstraints.BOTH;
    gbc_panel.gridx = 0;
    gbc_panel.gridy = 1;
    topPanel.add(panel, gbc_panel);
    panel.add(saveButton);
    saveButton.setText("Save Scheme");
    deleteButton.addActionListener(new ActionListener() {
    	@Override
			public void actionPerformed(ActionEvent arg0) {
    		deleteButton_actionPerformed(arg0);
    	}
    });
    
    panel.add(deleteButton);
    saveButton.addActionListener(new java.awt.event.ActionListener() {
      @Override
			public void actionPerformed(ActionEvent e) {
        saveButton_actionPerformed(e);
      }
    });
    processingPanel.add(absTransPanel,      new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    integrationPanel.add(jLabel2,                              new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 10, 0), 0, 0));
    integrationPanel.add(jLabel1,                  new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 10, 0), 0, 0));
    integrationPanel.add(autoIntegrateCheckBox,                   new GridBagConstraints(0, 5, 4, 1, 0.0, 1.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(10, 5, 0, 0), 0, 0));
    integrationPanel.add(minYTextField,                     new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 50, 0, 0), 0, 0));
    integrationPanel.add(integFactorTextField,              new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 50, 0, 0), 0, 0));
    integrationPanel.add(jLabel3,           new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
    integrationPanel.add(integOffsetTextField,            new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 50, 0, 0), 0, 0));
    integrationPanel.add(jLabel5,         new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
    integrationPanel.add(jLabel6,        new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
    integrationPanel.add(jLabel7,        new GridBagConstraints(2, 2, 1, 2, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
    integrationPanel.add(colorPanel1,            new GridBagConstraints(2, 3, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    integrationPanel.add(jLabel4,    new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
    integrationPanel.add(processingCustomButton,        new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
    integrationPanel.add(plotColorButton,                new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5, 50, 0, 0), 0, 0));
    absTransPanel.add(separateWindowCheckBox,        new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 147, 0));
    absTransPanel.add(TtoARadioButton,     new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
    absTransPanel.add(AtoTRadioButton,      new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
    absTransPanel.add(AutoConvertCheckBox,   new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
    conversionButtonGroup.add(TtoARadioButton);
    conversionButtonGroup.add(AtoTRadioButton);
  }

  private void addProcColorButton(JButton btn, Color c) {
    colorPanel1.add(btn, null);
    btn.setBackground(c);
    btn.setBorder(BorderFactory.createLoweredBevelBorder());
    btn.setName("pcb");
    btn.addActionListener(this);
    btn.setText(" ");
    btn.setMaximumSize(new Dimension(20, 20));
    btn.setMinimumSize(new Dimension(20, 20));
    btn.setPreferredSize(new Dimension(20, 20));
	}

	private void addColorButton(JButton btn, Color c) {    
    colorPanel.add(btn, null);
    btn.setBackground(c);
    btn.setBorder(BorderFactory.createLoweredBevelBorder());
    btn.setName("cb");
    btn.addActionListener(this);
	}
	

	/**
   * Initialise the Display Tab, where display schemes are created or set
   * @param viewer 
   */
  private void initDisplayTab(JSViewer viewer) {
    TreeMap<String, ColorParameters> displaySchemes = dsp.getDisplaySchemes();

    defaultDSName = preferences.getProperty("defaultDisplaySchemeName");

    // load names of schemes in schemeComboBox
    for (String key : displaySchemes.keySet())
      schemeComboBox.addItem(key);

    // load names of fonts in fontComboBox
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    String[] allFontNames = ge.getAvailableFontFamilyNames();
    for (int i = 0; i < allFontNames.length; i++)
      fontComboBox.addItem(allFontNames[i]);

    schemeComboBox.setSelectedItem(defaultDSName);

    // init preview panel
    try {
      JDXSource source = JDXReader.createJDXSourceFromStream(getClass()
          .getResourceAsStream("resources/sample.jdx"), false, false, Float.NaN);

      previewPanel = AwtPanel.getPanelOne(viewer, source.getSpectra().get(0));
      previewPanel.getPanelData().setBoolean(ScriptToken.ENABLEZOOM, false);
      previewPanel.getPanelData().setBoolean(ScriptToken.GRIDON, true);
      previewPanel.getPanelData().setBoolean(ScriptToken.TITLEON, true);
      previewPanel.getPanelData().setBoolean(ScriptToken.COORDINATESON, true);
    } catch (JSVException ex) {
      ex.printStackTrace();
    } catch (Exception ioe) {
      ioe.printStackTrace();
      return;
    }

    if (previewPanel != null)
      displayPanel.add(previewPanel, BorderLayout.CENTER);
    else {
      displayPanel.add(new JButton("Error Loading Sample File!"),
          BorderLayout.CENTER);
    }

    schemeComboBox.setSelectedItem(currentDS.name);
    fontComboBox.setSelectedItem(currentDS.displayFontName);

    repaint();
  }

  /**
   * Initalises the precesing tab, where integration properties and
   * transmittance/Adsorbance properties are set
   */
  private void initProcessingTab(){

    minYTextField.setText(preferences.getProperty("integralMinY"));
    integFactorTextField.setText(preferences.getProperty("integralFactor"));
    integOffsetTextField.setText(preferences.getProperty("integralOffset"));
    plotColorButton.setBackground(
        new Color(CU.getArgbFromString(preferences.getProperty("integralPlotColor"))));
    autoIntegrateCheckBox.setSelected(
       Boolean.parseBoolean(preferences.getProperty("automaticallyIntegrate")));
    String autoConvert =
        preferences.getProperty("automaticTAConversion");
    if(autoConvert.equals("TtoA")){
      TtoARadioButton.setSelected(true);
      autoIntegrateCheckBox.setSelected(true);
    }else if(autoConvert.equals("AtoT")){
      AtoTRadioButton.setSelected(true);
      autoIntegrateCheckBox.setSelected(true);
    }else{
      autoIntegrateCheckBox.setSelected(false);
    }

    separateWindowCheckBox.setSelected(
        Boolean.parseBoolean(preferences.getProperty("AtoTSeparateWindow")));

  }

  /**
   * Intialises the general tab, where general properties of the application
   * are set
   */
  private void initGeneralTab(){    
    
    confirmExitCheckBox.setSelected(
        Boolean.parseBoolean(preferences.getProperty("confirmBeforeExit")));
    openedDirCheckBox.setSelected(
        Boolean.parseBoolean(preferences.getProperty("useDirectoryLastOpenedFile")));
    exportDirCheckBox.setSelected(
        Boolean.parseBoolean(preferences.getProperty("useDirectoryLastExportedFile")));
    svgForInkscapeCheckBox.setSelected(
        Boolean.parseBoolean(preferences.getProperty("svgForInkscape")));       
    overlayCheckBox.setSelected(
        Boolean.parseBoolean(preferences.getProperty("automaticallyOverlay")));   
    legendCheckBox.setSelected(
        Boolean.parseBoolean(preferences.getProperty("automaticallyShowLegend")));    
    gridCheckBox.setSelected(
        Boolean.parseBoolean(preferences.getProperty("showGrid")));   
    coordinatesCheckBox.setSelected(
        Boolean.parseBoolean(preferences.getProperty("showCoordinates")));   
    scaleXCheckBox.setSelected(
        Boolean.parseBoolean(preferences.getProperty("showXScale")));
    scaleYCheckBox.setSelected(
            Boolean.parseBoolean(preferences.getProperty("showYScale")));
    sidePanelCheckBox.setSelected(
        Boolean.parseBoolean(preferences.getProperty("showSidePanel")));    
    toolbarCheckBox.setSelected(
        Boolean.parseBoolean(preferences.getProperty("showToolBar")));
    statusBarCheckBox.setSelected(
    		Boolean.parseBoolean(preferences.getProperty("showStatusBar")));

    
   
  }

  /**
   * Listener for the element list
   */
  class ElementListSelectionListener implements ListSelectionListener{

    /**
     * Sets the color of the currentColorButton to the color of the selected
     * element in the list
     * 
     * @param lse
     *        the ListSelectionEvent
     */
    @Override
		@SuppressWarnings("unchecked")
		public void valueChanged(ListSelectionEvent lse) {
      currentColorButton.setBackground((Color) currentDS
          .getElementColor(ScriptToken.getScriptToken(
          		((JList<String>)lse.getSource()).getSelectedValue())));
    }
  }

	/**
	 * Sets the color of the selected element on the current color button
	 * 
	 * @param ae
	 *          the ActionEvent
	 */
	@Override
	public void actionPerformed(ActionEvent ae) {
		JButton button = (JButton) ae.getSource();
		Color color = button.getBackground();
		if (button.getName().equals("cb")) {
			currentColorButton.setBackground(color);
			String element = elementList.getSelectedValue();
			setCurrentColor(element, color);
			currentDS.name = "Current";
			updatePreviewPanel();
		} else {
			plotColorButton.setBackground(color);
		}
	}
  /**
   * disposes the dialog when the cancel button is pressed
   * @param e the ActionEvent
   */
  void cancelButton_actionPerformed(ActionEvent e) {
    dispose();
  }

  public void setCurrentColor(String element, Color color) {
    currentDS.setColor(ScriptToken.getScriptToken(element + "COLOR"), new AwtColor(color.getRGB()));
	}

	/**
   * Updates the preferences that were set to the <code>Properties</code> passed
   * in the constructor
   * @param e the ActionEvent
   */
  void okButton_actionPerformed(ActionEvent e) {
    //preferences = new Properties();

    // rewrite and call setProperty method instead of the put method

    // General tab
    preferences.setProperty(
        "confirmBeforeExit", Boolean.toString(confirmExitCheckBox.isSelected()));
    preferences.setProperty(
        "automaticallyOverlay", Boolean.toString(overlayCheckBox.isSelected()));
    preferences.setProperty(
        "automaticallyShowLegend", Boolean.toString(legendCheckBox.isSelected()));
    preferences.setProperty(
        "useDirectoryLastOpenedFile", Boolean.toString(openedDirCheckBox.isSelected()));
    preferences.setProperty(
        "useDirectoryLastExportedFile", Boolean.toString(exportDirCheckBox.isSelected()));
    preferences.setProperty(
        "showSidePanel", Boolean.toString(sidePanelCheckBox.isSelected()));
    preferences.setProperty(
        "showToolBar", Boolean.toString(toolbarCheckBox.isSelected()));
    preferences.setProperty(
        "showStatusBar", Boolean.toString(statusBarCheckBox.isSelected()));
    preferences.setProperty(
        "showGrid", Boolean.toString(gridCheckBox.isSelected()));
    preferences.setProperty(
        "showCoordinates", Boolean.toString(coordinatesCheckBox.isSelected()));    
    preferences.setProperty(
    	"showXScale", Boolean.toString(scaleXCheckBox.isSelected()));
    preferences.setProperty(
    	"showYScale", Boolean.toString(scaleYCheckBox.isSelected()));
    preferences.setProperty(
        "svgForInkscape", Boolean.toString(svgForInkscapeCheckBox.isSelected()));
    
    isSpectrumDisplayApplyNowEnabled = spectrumDisplayApplyNowCheckBox.isSelected();
    
    // Processing tab
    preferences.setProperty("automaticallyIntegrate", Boolean.toString(autoIntegrateCheckBox.isSelected()));

    boolean autoTACovert = AutoConvertCheckBox.isSelected();
    if(autoTACovert){
      if(TtoARadioButton.isSelected())
        preferences.setProperty("automaticTAConversion", "TtoA");
      else
        preferences.setProperty("automaticTAConversion", "AtoT");
    }else{
      preferences.setProperty("automaticTAConversion", "false");
    }

    preferences.setProperty(
        "AtoTSeparateWindow", Boolean.toString(separateWindowCheckBox.isSelected()));
    preferences.setProperty("integralMinY", minYTextField.getText());
    preferences.setProperty("integralFactor", integFactorTextField.getText());
    preferences.setProperty("integralOffset", integOffsetTextField.getText());
    preferences.setProperty("integralPlotColor",
    		CU.toRGBHexString(new AwtColor(plotColorButton.getBackground().getRGB())));

    // Display Schemes Tab
    preferences.setProperty("defaultDisplaySchemeName", currentDS.name);
//    System.out.println(currentDS.name);

    //TreeMap<String,DisplayScheme> dispSchemes;
    if(currentDS.name.equals("Current")){
      //@SuppressWarnings("unchecked")
      TreeMap<String, ColorParameters> dispSchemes = dsp.getDisplaySchemes();
      dispSchemes.put("Current", currentDS);
    }

    dispose();
  }

  /**
   * Returns the preferences (<code>Properties</code> Object)
   * @return the preferences (<code>Properties</code> Object)
   */
  public Properties getPreferences(){
    return preferences;
  }

  /**
   * Shows a Color Dialog and updates the currentColorButton and the preview
   * panel accordingly
   * @param e the ActionEvent
   */
  void customButton_actionPerformed(ActionEvent e) {
    Color color = JColorChooser.showDialog(this, "Choose Color", Color.BLACK);
    if(color != null){
      currentColorButton.setBackground(color);
      setCurrentColor(elementList.getSelectedValue(), color);
      currentDS.name = "Current";
      updatePreviewPanel();
    }
  }
  
  /**
   * Delete a DisplayScheme to file
   * @param e the ActionEvent
   */
  void deleteButton_actionPerformed(ActionEvent e) {
	 
	 int option = JOptionPane.showConfirmDialog(this, "Do you really want to delete '" + currentDS.name + "'? This cannot be undone.",
			  "Delete Scheme", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
	 if(option == JOptionPane.YES_OPTION){
		 TreeMap<String, ColorParameters> dispSchemes = dsp.getDisplaySchemes();

		 dispSchemes.remove(currentDS.name);
	
		 int selectedIndex = schemeComboBox.getSelectedIndex();
		 schemeComboBox.removeItemAt(selectedIndex);
		 
		 try{
		 	dsp.store();
		 }
		 catch (Exception ex) {
		     JOptionPane.showMessageDialog(this, "There was an error deleting the Display Scheme",
		                                    "Error Deleting Scheme", JOptionPane.ERROR_MESSAGE);
		 }
		 
	 }
  }

  /**
   * Saves a new DisplayScheme to file
   * @param e the ActionEvent
   */
  void saveButton_actionPerformed(ActionEvent e) {
    // Prompt for Scheme Name
    String input = "";
    while(input != null && input.equals(""))
      input = JOptionPane.showInputDialog(this, "Enter the Name of the Display Scheme",
                                "Display Scheme Name", JOptionPane.PLAIN_MESSAGE);
    if(input == null)
      return;

    currentDS.name = input;
    boolean isdefault = defaultFontCheckBox.isSelected();
    if(!isdefault){
      String fontName = (String)fontComboBox.getSelectedItem();
      currentDS.displayFontName = fontName;
    }

    TreeMap<String, ColorParameters> dispSchemes = dsp.getDisplaySchemes();

    dispSchemes.put(input, currentDS);

    try {
      dsp.store();
      boolean found = false;
      // add if not already in combobox
      for (int i=0; i < schemeComboBox.getItemCount(); i++) {
        String item = schemeComboBox.getItemAt(i);
        if(item.equals(input)){
          found = true;
          break;
        }
      }

      if(!found)
        schemeComboBox.addItem(input);
      schemeComboBox.setSelectedItem(input);
    }
    catch (Exception ex) {
      JOptionPane.showMessageDialog(this, "There was an error saving the Display Scheme",
                                    "Error Saving Scheme", JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Updates the preview panel and the font panel with the DisplayScheme
   * chosen
   * @param e the ActionEvent
   */
  @SuppressWarnings("unchecked")
	void schemeComboBox_actionPerformed(ActionEvent e) {
    JComboBox<String> schemeCB = (JComboBox<String>)e.getSource();
    String schemeName = (String)schemeCB.getSelectedItem();
    ColorParameters ds = null;

    TreeMap<String, ColorParameters> schemes = dsp.getDisplaySchemes();
    for (Map.Entry<String, ColorParameters> entry: schemes.entrySet()) {
      ds = entry.getValue();
      if(ds.name.equals(schemeName)){
        currentDS = ds.copy();
        break;
      }
    }
    elementList.getSelectionModel().setSelectionInterval(0, 0);

    // Update Selected Font
    String fontName = currentDS.displayFontName;
    fontComboBox.setSelectedItem(fontName);
    currentDS.name = ds.name;
    updatePreviewPanel();
  }

  /**
   * Updates the preview panel with the values chosen in the dialog
   */
  void updatePreviewPanel(){
    if(previewPanel != null){
      previewPanel.setColorOrFont(currentDS, null);
      repaint();
    }
  }

  /**
   * Changes the font of the current DisplayScheme
   * @param e the ActionEvent
   */
  @SuppressWarnings("unchecked")
	void fontComboBox_actionPerformed(ActionEvent e) {
    String fontName = (String)((JComboBox<String>)e.getSource()).getSelectedItem();
    currentDS.displayFontName = fontName;
    currentDS.name = "Current";
    updatePreviewPanel();
  }

  /**
   * Sets the font of the current DisplayScheme to the the system default
   * @param e ActionEvent
   */
  void defaultFontCheckBox_actionPerformed(ActionEvent e) {
    JCheckBox cb = (JCheckBox)e.getSource();
    if(cb.isSelected()){
      fontComboBox.setSelectedItem("Default");
      fontComboBox.setEnabled(false);
      currentDS.displayFontName = "Default";
      currentDS.name = "Current";
      updatePreviewPanel();
    }else{
      fontComboBox.setEnabled(true);
    }
  }

  /**
   * Sets the current DisplayScheme to the default
   * @param e the ActionEvent
   */
  void defaultColorCheckBox_actionPerformed(ActionEvent e) {
    JCheckBox cb = (JCheckBox)e.getSource();
    if(cb.isSelected()){
      schemeComboBox.setSelectedItem("Default");
      schemeComboBox.setEnabled(false);
      customButton.setEnabled(false);
      saveButton.setEnabled(false);

      colorButton1.setEnabled(false);
      colorButton2.setEnabled(false);
      colorButton3.setEnabled(false);
      colorButton4.setEnabled(false);
      colorButton5.setEnabled(false);
      colorButton6.setEnabled(false);
      colorButton7.setEnabled(false);
      colorButton8.setEnabled(false);

      updatePreviewPanel();
    }else{
      schemeComboBox.setEnabled(true);
      colorPanel.setEnabled(true);
      customButton.setEnabled(true);
      saveButton.setEnabled(true);

      colorButton1.setEnabled(true);
      colorButton2.setEnabled(true);
      colorButton3.setEnabled(true);
      colorButton4.setEnabled(true);
      colorButton5.setEnabled(true);
      colorButton6.setEnabled(true);
      colorButton7.setEnabled(true);
      colorButton8.setEnabled(true);
    }
  }


  /* ---------------------------------------------------------------------*/

  /**
   * Retruns the current Display Scheme
   * @return the current Display Scheme
   */
  public ColorParameters getSelectedDisplayScheme(){
    return currentDS;
  }

  /**
   * Clears the list of recently opened files
   * @param e the ActionEvent
   */
  void clearRecentButton_actionPerformed(ActionEvent e) {
    int option = JOptionPane.showConfirmDialog(this, "Recent File Paths will be cleared!",
                                  "Warning", JOptionPane.OK_CANCEL_OPTION,
                                  JOptionPane.WARNING_MESSAGE);
    if(option == JOptionPane.OK_OPTION)
      preferences.setProperty("recentFilePaths", "");
  }

  /**
   * Shows Color dialog to set the color of the integral plot
   * @param e ActionEvent
   */
  void processingCustomButton_actionPerformed(ActionEvent e) {
    Color color = JColorChooser.showDialog(this, "Choose Color", Color.BLACK);
    if(color != null)
      plotColorButton.setBackground(color);
  }

	public boolean shouldApplySpectrumDisplaySettingsNow() {
		return isSpectrumDisplayApplyNowEnabled;
	}

}
