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

import org.jmol.viewer.Viewer;
import org.jmol.i18n.GT;
import org.jmol.util.Logger;
import org.openscience.jmol.app.jmolpanel.JmolPanel;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.JComponent;
import javax.swing.InputVerifier;
import java.io.File;
import java.util.Hashtable;
import java.util.Map;

/**
 * A dialog for controling the creation of a povray input file from a
 * Chemframe and a display. The actual leg work of writing the file
 * out is done by PovrayWriter.java.
 * <p>Borrows code from org.openscience.jmol.Vibrate (Thanks!).
 * @author Thomas James Grey (tjg1@ch.ic.ac.uk)
 * @author Matthew A. Meineke (mmeineke@nd.edu)
 */
public class PovrayDialog extends JDialog {

  private transient Viewer vwr;
  
  protected JButton    povrayPathButton;
  //protected JTextField commandLineField;
  protected JButton    goButton;
  protected JTextField saveField;
  protected JTextField savePathLabel;
  private int          outputWidth = -1;
  private int          outputHeight = -1;
  protected JTextField povrayPathLabel;
  
  protected JCheckBox runPovCheck;
  //protected JCheckBox useIniCheck;
  protected JCheckBox allFramesCheck;
  protected JCheckBox antiAliasCheck;
  protected JCheckBox displayWhileRenderingCheck;
  
  //private JCheckBox           imageSizeCheck;
  private JLabel              imageSizeWidth;
  private JFormattedTextField imageSizeTextWidth;
  private JLabel              imageSizeHeight;
  private JFormattedTextField imageSizeTextHeight;
  private JCheckBox	          imageSizeRatioBox;
  private JComboBox<String>           imageSizeRatioCombo;
  
  private JCheckBox outputFormatCheck;
  private JComboBox<String> outputFormatCombo;

  private JCheckBox outputAlphaCheck;
  
  private JCheckBox mosaicPreviewCheck;
  private JLabel    mosaicPreviewStart;
  private JComboBox<String> mosaicPreviewComboStart;
  private JLabel    mosaicPreviewEnd;
  private JComboBox<String> mosaicPreviewComboEnd;

  private String outputExtension = ".png";
  private String outputFileType = "N";


  /**
   * Creates a dialog for getting info related to output frames in
   *  povray format.
   * @param f The frame assosiated with the dialog
   * @param vwr The interacting display we are reproducing (source of view angle info etc)
   */
  public PovrayDialog(JFrame f, Viewer vwr) {

    super(f, GT.$("Render in POV-Ray"), true);
    this.vwr = vwr;

    //
    String text = null;
    
    //Take the height and width settings from the JFrame
    int screenWidth = vwr.getScreenWidth();
    int screenHeight = vwr.getScreenHeight();
    setImageDimensions(screenWidth, screenHeight);

    // Event management
    ActionListener updateActionListener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateScreen();
      }
    };
    InputVerifier updateInputVerifier = new InputVerifier() {
      @Override
      public boolean verify(JComponent component) {
        updateScreen();
        return true;
      }
    };
    ItemListener updateItemListener = new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        updateScreen();
      }
    };
    
    //Box: Window
    Box windowBox = Box.createVerticalBox();
    getContentPane().add(windowBox);
    
    //Box: Main
    Box mainBox = Box.createVerticalBox();
    
    //GUI for save name selection
    Box justSavingBox = Box.createVerticalBox();
    text = GT.$("Conversion from Jmol to POV-Ray");
    justSavingBox.setBorder(new TitledBorder(text));
    
    Box saveBox = Box.createHorizontalBox();
    text = GT.$("File Name:");
    saveBox.setBorder(new TitledBorder(text));
    text = GT.$("'caffeine.pov' -> 'caffeine.pov', 'caffeine.pov.ini', 'caffeine.pov.spt'");
    saveBox.setToolTipText(text);
    saveField = new JTextField("Jmol.pov", 20);
    saveField.addActionListener(updateActionListener);
    saveField.setInputVerifier(updateInputVerifier);
    saveBox.add(saveField);
    justSavingBox.add(saveBox);

    //GUI for save path selection
    Box savePathBox = Box.createHorizontalBox();
    text = GT.$("Working Directory");
    savePathBox.setBorder(new TitledBorder(text));
    text = GT.$("Where the .pov files will be saved");
    savePathBox.setToolTipText(text);
    savePathLabel = new JTextField("");
    savePathLabel.setEditable(false);
    savePathLabel.setBorder(null);
    savePathBox.add(savePathLabel);
    text = GT.$("Select");
    JButton savePathButton = new JButton(text);
    savePathButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        showSavePathDialog();
      }
    });
    savePathBox.add(savePathButton);
    justSavingBox.add(savePathBox);
    mainBox.add(justSavingBox);

    //GUI for povray options
    Box povOptionsBox = Box.createVerticalBox();
    text = GT.$("POV-Ray Runtime Options");
    povOptionsBox.setBorder(new TitledBorder(text));
    
    // Run povray option
    Box runPovBox = Box.createHorizontalBox();
    text = GT.$("Run POV-Ray directly");
    runPovCheck = new JCheckBox(text, true);
    text = GT.$("Launch POV-Ray from within Jmol");
    runPovCheck.setToolTipText(text);
    runPovCheck.addItemListener(updateItemListener);
    runPovBox.add(runPovCheck);
    runPovBox.add(Box.createGlue());
    povOptionsBox.add(runPovBox);

    
 /*
    // Use Ini option
    Box useIniBox = Box.createHorizontalBox();
    text = GT._("Use .ini file");
    useIniCheck = new JCheckBox(text, true);
    text = GT._("Save options in a .ini file");
    useIniCheck.setToolTipText(text);
    useIniCheck.addItemListener(updateItemListener);
    useIniBox.add(useIniCheck);
    useIniBox.add(Box.createGlue());
    povOptionsBox.add(useIniBox);

    // Render all frames options
    Box allFramesBox = Box.createHorizontalBox();
    text = GT._("Render all frames");
    allFramesCheck = new JCheckBox(text, false);
    text = GT._("Render each model (not only the currently displayed one)");
    allFramesCheck.setToolTipText(text);
    allFramesCheck.addItemListener(updateItemListener);
    allFramesBox.add(allFramesCheck);
    allFramesBox.add(Box.createGlue());
    povOptionsBox.add(allFramesBox);
    
    // Antialias option
    Box antiAliasBox = Box.createHorizontalBox();
    text = GT._("Turn on POV-Ray anti-aliasing");
    antiAliasCheck = new JCheckBox(text, true);
    text = GT._("Use povray's slower but higher quality anti-aliasing mode");
    antiAliasCheck.setToolTipText(text);
    antiAliasCheck.addItemListener(updateItemListener);
    antiAliasBox.add(antiAliasCheck);
    antiAliasBox.add(Box.createGlue());
    povOptionsBox.add(antiAliasBox);
    
*/
    // Display when rendering option
    Box displayBox = Box.createHorizontalBox();
    text = GT.$("Display While Rendering");
    displayWhileRenderingCheck = new JCheckBox(text, true);
    text = GT.$("Should POV-Ray attempt to display while rendering?");
    displayWhileRenderingCheck.setToolTipText(text);
    displayWhileRenderingCheck.addItemListener(updateItemListener);
    displayBox.add(displayWhileRenderingCheck);
    displayBox.add(Box.createGlue());
    povOptionsBox.add(displayBox);

    // Image size option
    Box imageBox = Box.createHorizontalBox();
    //text = GT._("Image size");
    //imageSizeCheck = new JCheckBox(text, true);
    //text = GT._("Image size");
    //imageSizeCheck.setToolTipText(text);
    //imageSizeCheck.addItemListener(new ItemListener() {
    //  public void itemStateChanged(ItemEvent e) {
    //    imageSizeChanged();
    //    updateCommandLine();
    //  }
    //});
    //imageBox.add(imageSizeCheck);
    imageBox.add(Box.createHorizontalStrut(10));
    Box imageSizeDetailBox = Box.createVerticalBox();
    Box imageSizeXYBox = Box.createHorizontalBox();
    text = GT.$("width:")+" ";
    imageSizeWidth = new JLabel(text);
    text = GT.$("Image width");
    imageSizeWidth.setToolTipText(text);
    imageSizeXYBox.add(imageSizeWidth);
    imageSizeTextWidth = new JFormattedTextField();
    imageSizeTextWidth.setValue(Integer.valueOf(outputWidth));
    imageSizeTextWidth.addPropertyChangeListener("value",
      new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent e) {
          imageSizeChanged();
          updateCommandLine();
        }
      }
    );
    imageSizeXYBox.add(imageSizeTextWidth);
    imageSizeXYBox.add(Box.createHorizontalStrut(10));
    text = GT.$("height:")+" ";
    imageSizeHeight = new JLabel(text);
    text = GT.$("Image height");
    imageSizeHeight.setToolTipText(text);
    imageSizeXYBox.add(imageSizeHeight);
    imageSizeTextHeight = new JFormattedTextField();
    imageSizeTextHeight.setValue(Integer.valueOf(outputHeight));
    imageSizeTextHeight.addPropertyChangeListener("value",
      new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent e) {
          imageSizeChanged();
          updateCommandLine();
        }
      }
    );
    imageSizeXYBox.add(imageSizeTextHeight);
    imageSizeXYBox.add(Box.createGlue());
    imageSizeDetailBox.add(imageSizeXYBox);
    Box imageSizeBox = Box.createHorizontalBox();
    text = GT.$("Fixed ratio : ");
    imageSizeRatioBox = new JCheckBox(text, true);
    text = GT.$("Use a fixed ratio for width:height");
    imageSizeRatioBox.setToolTipText(text);
    imageSizeRatioBox.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        imageSizeChanged();
        updateCommandLine();
      }
    });
    imageSizeBox.add(imageSizeRatioBox);
    imageSizeBox.add(Box.createHorizontalStrut(10));
    imageSizeRatioCombo = new JComboBox<String>();
    text = GT.$("User defined");
    imageSizeRatioCombo.addItem(text);
    text = GT.$("Keep ratio of Jmol window");
    imageSizeRatioCombo.addItem(text);
    text = "4:3";
    imageSizeRatioCombo.addItem(text);
    text = "16:9";
    imageSizeRatioCombo.addItem(text);
    imageSizeRatioCombo.setSelectedIndex(1);
    imageSizeRatioCombo.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        imageSizeChanged();
        updateCommandLine();
      }
    });
    imageSizeBox.add(imageSizeRatioCombo);
    imageSizeBox.add(Box.createGlue());
    imageSizeDetailBox.add(imageSizeBox);
    imageSizeDetailBox.add(Box.createGlue());
    imageBox.add(imageSizeDetailBox);
    imageBox.add(Box.createGlue());
    povOptionsBox.add(imageBox);
    imageSizeChanged();
    
    // Output format option
    Box outputBox = Box.createHorizontalBox();
/*    
    text = GT._("Output format : ");
    outputFormatCheck = new JCheckBox(text, true);
    text = GT._("Select the file format of the output file");
    outputFormatCheck.setToolTipText(text);
    outputFormatCheck.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        outputFormatChanged();
        updateCommandLine();
      }
    });
    outputBox.add(outputFormatCheck);
*/    
    outputBox.add(Box.createHorizontalStrut(10));
    outputFormatCombo = new JComboBox<String>();
    //case 0
    text = GT.$("N - PNG");
    outputFormatCombo.addItem(text);
    //case 1
    text = GT.$("P - PPM");
    outputFormatCombo.addItem(text);
    //case 2
    text = GT.$("C - Compressed Targa-24");
    outputFormatCombo.addItem(text);
    //case 3
    text = GT.$("T - Uncompressed Targa-24");
    outputFormatCombo.addItem(text);
    outputFormatCombo.setSelectedIndex(0);
    outputFormatCombo.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        outputFormatChanged();
        updateCommandLine();
      }
    });
    outputBox.add(outputFormatCombo);
    outputBox.add(Box.createGlue());
    povOptionsBox.add(outputBox);
    outputFormatChanged();

    // Alpha option
    Box alphaBox = Box.createHorizontalBox();
    text = GT.$("Alpha transparency");
    outputAlphaCheck = new JCheckBox(text, false);
    text = GT.$("Output Alpha transparency data");
    outputAlphaCheck.setToolTipText(text);
    outputAlphaCheck.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        updateCommandLine();
      }
    });
    alphaBox.add(outputAlphaCheck);
    alphaBox.add(Box.createGlue());
    povOptionsBox.add(alphaBox);
    
    // Mosaic preview option
    Box mosaicBox = Box.createHorizontalBox();
    text = GT.$("Mosaic preview");
    mosaicPreviewCheck = new JCheckBox(text, false);
    text = GT.$("Render the image in several passes");
    mosaicPreviewCheck.setToolTipText(text);
    mosaicPreviewCheck.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
      	mosaicPreviewChanged();
      	updateCommandLine();
      }
    });
    mosaicBox.add(mosaicPreviewCheck);
    mosaicBox.add(Box.createHorizontalStrut(10));
    text = GT.$("Start size : ");
    mosaicPreviewStart = new JLabel(text);
    text = GT.$("Initial size of the tiles");
    mosaicPreviewStart.setToolTipText(text);
    mosaicBox.add(mosaicPreviewStart);
    mosaicPreviewComboStart = new JComboBox<String>();
    for (int power = 0; power < 8; power++) {
    	mosaicPreviewComboStart.addItem(Integer.toString((int)Math.pow(2, power)));
    }
    mosaicPreviewComboStart.setSelectedIndex(3);
    mosaicPreviewComboStart.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        mosaicPreviewChanged();
        updateCommandLine();
      }
    });
    mosaicBox.add(mosaicPreviewComboStart);
    mosaicBox.add(Box.createHorizontalStrut(10));
    text = GT.$("End size : ");
    mosaicPreviewEnd = new JLabel(text);
    text = GT.$("Final size of the tiles");
    mosaicPreviewEnd.setToolTipText(text);
    mosaicBox.add(mosaicPreviewEnd);
    mosaicPreviewComboEnd = new JComboBox<String>();
    for (int power = 0; power < 8; power++) {
    	mosaicPreviewComboEnd.addItem(Integer.toString((int)Math.pow(2, power)));
    }
    mosaicPreviewComboEnd.setSelectedIndex(0);
    mosaicPreviewComboEnd.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        mosaicPreviewChanged();
        updateCommandLine();
      }
    });
    mosaicBox.add(mosaicPreviewComboEnd);
    mosaicBox.add(Box.createGlue());
    povOptionsBox.add(mosaicBox);
    mosaicPreviewChanged();
  
    //GUI for povray path selection
    Box povrayPathBox = Box.createHorizontalBox();
    text = GT.$("Location of the POV-Ray Executable");
    povrayPathBox.setBorder(new TitledBorder(text));
    text = GT.$("Location of the POV-Ray Executable");
    povrayPathBox.setToolTipText(text);
    povrayPathLabel = new JTextField("");
    povrayPathLabel.setEditable(false);
    povrayPathLabel.setBorder(null);
    povrayPathBox.add(povrayPathLabel);
    text = GT.$("Select");
    povrayPathButton = new JButton(text);
    povrayPathButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        showPovrayPathDialog();
      }
    });
    povrayPathBox.add(povrayPathButton);
    povOptionsBox.add(povrayPathBox);

    //GUI for command selection
    
    /*
    Box commandLineBox = Box.createVerticalBox();
    text = GT._("Command Line to Execute");
    commandLineBox.setBorder(new TitledBorder(text));
    text = GT._("The actual command which will be executed");
    commandLineBox.setToolTipText(text);
    commandLineField = new JTextField(30);
    text = GT._("The actual command which will be executed");
    commandLineField.setToolTipText(text);
    commandLineField.addActionListener(updateActionListener);
    commandLineBox.add(commandLineField);
    povOptionsBox.add(commandLineBox);
*/
    mainBox.add(povOptionsBox);

    //GUI for panel with go, cancel and stop (etc) buttons
    Box buttonBox = Box.createHorizontalBox();
    buttonBox.add(Box.createGlue());
    text = GT.$("Go!");
    goButton = new JButton(text);
    text = GT.$("Save file and possibly launch POV-Ray");
    goButton.setToolTipText(text);
    goButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        goPressed();
      }
    });
    buttonBox.add(goButton);
    text = GT.$("Cancel");
    JButton cancelButton = new JButton(text);
    text = GT.$("Cancel this dialog without saving");
    cancelButton.setToolTipText(text);
    cancelButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        cancelPressed();
      }
    });
    buttonBox.add(cancelButton);
    
    windowBox.add(mainBox);
    windowBox.add(buttonBox);

    getPathHistory();
    updateScreen();
    pack();
    centerDialog();
    setVisible(true);
  }

  /**
   *  Sets the output image dimensions. Setting either to &lt;= 0 will
   *  remove the height and width specification from the commandline- the
   * resulting behaviour depends on povray!
   * @param imageWidth The width of the image.
   * @param imageHeight The height of the image.
   */
  public void setImageDimensions(int imageWidth, int imageHeight) {
    outputWidth = imageWidth;
    outputHeight = imageHeight;
    updateCommandLine();
  }

  /**
   * Save or else launch povray- ie do our thang!
   */
  void goPressed() {

    // File theFile = new.getSelectedFile();
    String basename = saveField.getText();
    String filename = basename;
    String savePath = savePathLabel.getText();
    File theFile = new File(savePath, filename);
    basename = filename = theFile.getAbsolutePath();
    /*
    boolean allFrames = false;
    if (allFramesCheck != null) {
     allFrames = allFramesCheck.isSelected();   
    }
    */
    //int width = outputWidth;
    //int height = outputHeight;
    //if ((imageSizeCheck != null) && (imageSizeCheck.isSelected())) {
    int height = Integer.parseInt(imageSizeTextHeight.getValue().toString());
    int width = Integer.parseInt(imageSizeTextWidth.getValue().toString());
    //}
    Map<String, Object> params = new Hashtable<String, Object>();
    params.put("type", "Povray");
    params.put("fileName", filename);
    params.put("width", Integer.valueOf(width));
    params.put("height", Integer.valueOf(height));
    params.put("params", getINI());
    String data = vwr.generateOutputForExport(params);
    if (data == null)
      return;
    vwr.writeTextFile(filename + ".ini", data);

    // Run Povray if needed
    boolean callPovray = runPovCheck.isSelected();
    if (callPovray) {
      String[] commandLineArgs = null;
      //      if (useIniFile) {
      commandLineArgs = new String[] { povrayPathLabel.getText(),
          filename + ".ini" };
      //      } else {
      //        commandLineArgs = getCommandLineArgs();
      //      }
      try {
        Runtime.getRuntime().exec(commandLineArgs);
      } catch (java.io.IOException e) {
        Logger.errorEx("Caught IOException in povray exec", e);
        Logger.error("CmdLine:");
        for (int i = 0; i < commandLineArgs.length; i++) {
          Logger.error("  <" + commandLineArgs[i] + ">");
        }
      }
    }
    setVisible(false);
    saveHistory();
    dispose();
  }       

  /**
   * Responds to cancel being press- or equivalent eg window closed.
   */
  void cancelPressed() {
    setVisible(false);
    dispose();
  }

  /**
   * Show a file selector when the savePath button is pressed.
   */
  void showSavePathDialog() {

    JFileChooser myChooser = new JFileChooser();
    myChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    int button = myChooser.showDialog(this, GT.$("Select"));
    if (button == JFileChooser.APPROVE_OPTION) {
      java.io.File newFile = myChooser.getSelectedFile();
      String savePath;
      if (newFile.isDirectory()) {
        savePath = newFile.toString();
      } else {
        savePath = newFile.getParent();
      }
      savePathLabel.setText(savePath);
      updateCommandLine();
      pack();
    }
  }

  /**
   * Show a file selector when the savePath button is pressed.
   */
  void showPovrayPathDialog() {

    JFileChooser myChooser = new JFileChooser();
    int button = myChooser.showDialog(this, GT.$("Select"));
    if (button == JFileChooser.APPROVE_OPTION) {
      java.io.File newFile = myChooser.getSelectedFile();
      povrayPathLabel.setText(newFile.toString());
      updateCommandLine();
      pack();
    }
  }

  /**
   * Called when the ImageSize check box is modified 
   */
  void imageSizeChanged() {
  	//if (imageSizeCheck != null) {
  	  boolean selected = true;//imageSizeCheck.isSelected();
  	  boolean enabled = true;//runPovCheck.isSelected() || useIniCheck.isSelected();
  	  boolean ratioSelected = false;
  	  //imageSizeCheck.setEnabled(enabled);
  	  if (imageSizeRatioBox != null) {
  	    ratioSelected = imageSizeRatioBox.isSelected();
  	    imageSizeRatioBox.setEnabled(selected && enabled);
  	  }
  	  if (imageSizeWidth != null) {
  	    imageSizeWidth.setEnabled(selected && enabled);
  	  }
  	  if (imageSizeTextWidth != null) {
  	    imageSizeTextWidth.setEnabled(selected && enabled);
  	  }
  	  if (imageSizeHeight != null) {
  	    imageSizeHeight.setEnabled(selected && !ratioSelected && enabled);
  	  }
  	  if (imageSizeTextHeight != null) {
  	    imageSizeTextHeight.setEnabled(selected && !ratioSelected && enabled);
  	  }
  	  if (imageSizeRatioCombo != null) {
  	  	imageSizeRatioCombo.setEnabled(selected && ratioSelected && enabled);
  	    if ((imageSizeTextWidth != null) && (imageSizeTextHeight != null)) {
          int width = Integer.parseInt(imageSizeTextWidth.getValue().toString());
  	      int height;
  	      switch (imageSizeRatioCombo.getSelectedIndex()) {
  	      case 0: // Free
  	        break;
  	      case 1: // Jmol
  	        height = (int)(((double) width) * outputHeight / outputWidth);
  	        imageSizeTextHeight.setValue(Integer.valueOf(height));
  	        break;
  	      case 2: // 4/3
  	        height = (int)(((double) width) * 3 / 4);
  	        imageSizeTextHeight.setValue(Integer.valueOf(height));
  	        break;
  	      case 3: // 16/9
  	        height = (int)(((double) width) * 9 / 16);
  	        imageSizeTextHeight.setValue(Integer.valueOf(height));
  	        break;
  	      }
  	    }
  	  }
  	//}
  }
  
  /**
   * Called when the OutputFormat check box is modified 
   */
  void outputFormatChanged() {
  	if (outputFormatCheck != null) {
  	  boolean selected = outputFormatCheck.isSelected();
  	  boolean enabled = true;//runPovCheck.isSelected() || useIniCheck.isSelected();
  	  outputFormatCheck.setEnabled(enabled);
  	  if (outputFormatCombo != null) {
  	    outputFormatCombo.setEnabled(selected && enabled);
        switch (outputFormatCombo.getSelectedIndex()) {
        case 0: // PNG
          outputExtension = ".png";
          outputFileType = "N";
          break;
        case 1: // PPM
          outputExtension = ".ppm";
          outputFileType = "P";
          break;
        case 2: // Compressed TARGA
          outputExtension = ".tga";
          outputFileType = "C";
          break;
        case 3: // uncompressed TARGA
          outputExtension = ".tga";
          outputFileType = "T";
          break;
        }        
  	  }
  	}
  }
  
  /**
   * Called when the MosaicPreview check box is modified 
   */

  void mosaicPreviewChanged() {
  	if (mosaicPreviewCheck != null) {
  	  boolean selected = mosaicPreviewCheck.isSelected();
  	  boolean enabled = runPovCheck.isSelected();// || useIniCheck.isSelected();
  	  mosaicPreviewCheck.setEnabled(enabled);
  	  if (mosaicPreviewStart != null) {
  	    mosaicPreviewStart.setEnabled(selected && enabled);
  	  }
  	  if (mosaicPreviewComboStart != null) {
  	    mosaicPreviewComboStart.setEnabled(selected && enabled);
  	  }
  	  if (mosaicPreviewEnd != null) {
  	    mosaicPreviewEnd.setEnabled(selected && enabled);
  	  }
  	  if (mosaicPreviewComboEnd != null) {
  	    mosaicPreviewComboEnd.setEnabled(selected && enabled);
  	  }
  	}
  }

  /**
   * Update screen informations
   */
  protected void updateScreen() {
  	
  	// Call povray ?
  	boolean callPovray = false;
  	if (runPovCheck != null) {
  	  callPovray = runPovCheck.isSelected();
  	}
    String text = null;
    if (callPovray) {
      text = GT.$("Go!");
    } else {
      text = GT.$("Save");
    }
    if (goButton != null) {
      goButton.setText(text);
    }
    
    // Use INI ?
    boolean useIni = true;
/*    
    if (useIniCheck != null) {
      useIni = useIniCheck.isSelected();
    }
*/    
    // Update state
    if (antiAliasCheck != null) {
      antiAliasCheck.setEnabled(callPovray || useIni);
    }
    if (povrayPathButton != null) {
      povrayPathButton.setEnabled(callPovray || useIni);
    }
/*    
    if (displayWhileRenderingCheck != null) {
      displayWhileRenderingCheck.setEnabled(callPovray || useIni);
    }
    if (antiAliasCheck != null) {
      antiAliasCheck.setEnabled(callPovray || useIni);
    }
*/    
    //if (commandLineField != null) {
    //  commandLineField.setEnabled(callPovray && !useIni);
    //}
    
    // Various update
    imageSizeChanged();
    outputFormatChanged();
//    mosaicPreviewChanged();
    
  	// Update command line
    updateCommandLine();
  }
  
  protected void updateCommandLine() {
    //if (commandLineField != null)
     // commandLineField.setText(getCommandLine());
  }
  
  /**
   * Generates a commandline from the options set for povray path
   * etc and sets in the textField.
   * @return command line
   */
  protected String getCommandLine() {

  	// Check fields
  	String basename = null;
  	if (saveField != null) {
  		basename = saveField.getText();
  	}
  	String savePath = null;
  	if (savePathLabel != null) {
  	  savePath = savePathLabel.getText();
  	}
  	String povrayPath = null;
  	if (povrayPathLabel != null) {
  	  povrayPath = povrayPathLabel.getText();
  	}
    if ((savePath == null) ||
        (povrayPath == null) ||
	    (basename == null)) {
      //if (commandLineField != null) {
      //  commandLineField.setText(GT._("null component string"));
      //}
      return "";
    }

    //Append a file separator to the savePath is necessary
    if (!savePath.endsWith(java.io.File.separator)) {
      savePath += java.io.File.separator;
    }

    String commandLine =
      doubleQuoteIfContainsSpace(povrayPath) +
      " +I" + simpleQuoteIfContainsSpace(savePath + basename + ".pov");

    commandLine +=
      " +O" +
      simpleQuoteIfContainsSpace(savePath + basename + outputExtension) +
      " +F" + outputFileType;
    
    // Output alpha options
    if ((outputAlphaCheck != null) && (outputAlphaCheck.isSelected())) {
      commandLine +=
        " +UA";
    }
    
    // Image size options
    //if ((imageSizeCheck != null) && (imageSizeCheck.isSelected())) {
      commandLine +=
        " +H" + imageSizeTextHeight.getValue() +
		" +W" + imageSizeTextWidth.getValue();
    //} else {
    //  if ((outputWidth > 0) && (outputHeight > 0)) {
    //    commandLine +=
	  // 	  " +H" + outputHeight +
		//  " +W" + outputWidth;
    //  }
    //}

    // Anti Alias
//    if ((antiAliasCheck != null) && (antiAliasCheck.isSelected())) {
      commandLine += " +A0.1";
    //}

    // Display while rendering
    if ((displayWhileRenderingCheck != null) &&
        (displayWhileRenderingCheck.isSelected())) {
      commandLine += " +D +P";
    }

    // Animation options
    if ((allFramesCheck != null) && (allFramesCheck.isSelected())) {
      commandLine += " +KFI1";
      commandLine += " +KFF" + vwr.ms.mc;
      commandLine += " +KI1";
      commandLine += " +KF" + vwr.ms.mc;
    }

    // Mosaic preview options
    if ((mosaicPreviewCheck != null) && (mosaicPreviewCheck.isSelected())) {
      commandLine +=
        " +SP" + mosaicPreviewComboStart.getSelectedItem() +
		" +EP" + mosaicPreviewComboEnd.getSelectedItem();
    }
  
    commandLine += " -V"; // turn off verbose messages ... although it is still rather verbose

    return commandLine;
  }

  
  /**
   * Save INI file
   * 
   * @return INI data
   */
  

  private String getINI() {

    StringBuilder data = new StringBuilder();
    // Save path
  	String savePath = savePathLabel.getText();
    if (!savePath.endsWith(java.io.File.separator)) {
      savePath += java.io.File.separator;
    }
    String basename = saveField.getText();
  	
    // Input file
    data.append("Input_File_Name=" + savePath + basename + "\n");

    // Output format options
    data.append("Output_to_File=true\n");
    data.append("Output_File_Type=" + outputFileType + "\n");
    data.append("Output_File_Name=" + savePath + basename + outputExtension + "\n");
    
    // Image size options
    data.append("Height=" + imageSizeTextHeight.getValue() + "\n");
    data.append("Width=" + imageSizeTextWidth.getValue() + "\n");

    // Animation options
    if ((allFramesCheck != null) && (allFramesCheck.isSelected())) {
      data.append("Initial_Frame=1\n");
      data.append("Final_Frame=" + vwr.ms.mc + "\n");
      data.append("Initial_Clock=1\n");
      data.append("Final_Clock=" + vwr.ms.mc + "\n");
    }
    
    // Output alpha options
    if ((outputAlphaCheck != null) && (outputAlphaCheck.isSelected())) {
      data.append("Output_Alpha=true\n");
    }
    
    // Anti Alias
      data.append("Antialias=true\n");
      data.append("Antialias_Threshold=0.1\n");

    // Display while rendering
    if ((displayWhileRenderingCheck != null) &&
        (displayWhileRenderingCheck.isSelected())) {
      data.append("Display=true\n");
      data.append("Pause_When_Done=true\n");
    }

    // Mosaic preview options
    if ((mosaicPreviewCheck != null) && (mosaicPreviewCheck.isSelected())) {
      data.append("Preview_Start_Size=" + mosaicPreviewComboStart.getSelectedItem() + "\n");
      data.append("Preview_End_Size=" + mosaicPreviewComboEnd.getSelectedItem() + "\n");
    }
    
    data.append("Warning_Level=5\n");
    data.append("Verbose=false\n");
    return data.toString();
  }

  /**
   * @return Command line split into arguments
   */
/*
  private String[] getCommandLineArgs() {
  	
    //Parsing command line
    String commandLine = commandLineField.getText();
    List vector = new List();
    int begin = 0;
    int end = 0;
    int doubleQuoteCount = 0;
    while (end < commandLine.length()) {
      if (commandLine.charAt(end) == '\"') {
        doubleQuoteCount++;
      }
      if (Character.isSpaceChar(commandLine.charAt(end))) {
        while ((begin < end) &&
               (Character.isSpaceChar(commandLine.charAt(begin)))) {
          begin++;
        }
        if (end > begin + 1) {
          if (doubleQuoteCount % 2 == 0) {
            vector.add(commandLine.substring(begin, end));
            begin = end;
          }
        }
      }
      end++;
    }
    while ((begin < end) &&
           (Character.isSpaceChar(commandLine.charAt(begin)))) {
      begin++;
    }
    if (end > begin + 1) {
      vector.add(commandLine.substring(begin, end));
    }
    
    //Construct result
    String[] args = new String[vector.size()];
    for (int pos = 0; pos < vector.size(); pos++) {
      args[pos] = vector.get(pos).toString();
      if ((args[pos].charAt(0) == '\"') &&
          (args[pos].charAt(args[pos].length() - 1) == '\"')) {
        args[pos] = args[pos].substring(1, args[pos].length() - 1);
      }
    }
    return args;
  }
*/  
  /**
   * Centers the dialog on the screen.
   */
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

  /**
   * Listener for responding to dialog window events.
   */
  class PovrayWindowListener extends WindowAdapter {

    /**
     * Closes the dialog when window closing event occurs.
     * @param e Event
     */
    @Override
    public void windowClosing(WindowEvent e) {
      cancelPressed();
      setVisible(false);
      dispose();
    }
  }

  /**
   * Just recovers the path settings from last session.
   */
  private void getPathHistory() {

    java.util.Properties props = JmolPanel.historyFile.getProperties();
    if (povrayPathLabel != null) {
      String povrayPath = props.getProperty("povrayPath",
        System.getProperty("user.home"));
      if (povrayPath != null) {
        povrayPathLabel.setText(povrayPath);
      }
    }
    if (savePathLabel != null) {
      String savePath = props.getProperty("povraySavePath",
        System.getProperty("user.home"));
      if (savePath != null) {
        savePathLabel.setText(savePath);
      }
    }
  }

  /**
   * Just saves the path settings from this session.
   */
  private void saveHistory() {
    java.util.Properties props = new java.util.Properties();
    props.setProperty("povrayPath", povrayPathLabel.getText());
    props.setProperty("povraySavePath", savePathLabel.getText());
    JmolPanel.historyFile.addProperties(props);
  }

  String doubleQuoteIfContainsSpace(String str) {
    for (int i = str.length(); --i >= 0; )
      if (str.charAt(i) == ' ')
        return "\"" + str + "\"";
    return str;
  }

  String simpleQuoteIfContainsSpace(String str) {
    for (int i = str.length(); --i >= 0; )
      if (str.charAt(i) == ' ')
        return "\'" + str + "\'";
    return str;
  }
}
