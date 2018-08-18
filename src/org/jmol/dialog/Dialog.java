/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2008-03-05 17:47:26 -0600 (Wed, 05 Mar 2008) $
 * $Revision: 9055 $
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
package org.jmol.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import org.jmol.api.JmolAppAPI;
import org.jmol.api.JmolDialogInterface;
import org.jmol.i18n.GT;
import org.jmol.util.Logger;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.Viewer;

public class Dialog extends JPanel implements JmolDialogInterface {

  String[] extensions = new String[10];
  String choice;
  String extension;
  
  // static so that they can be shared among instances
  
  static private int defaultChoice;
  static int qualityJPG = 75;
  static int qualityPNG = 2;
  
  private JSlider qSliderJPEG, qSliderPNG;
  private JComboBox<String> cb;

  JPanel qPanelJPEG, qPanelPNG;
  static JFileChooser imageChooser;
  static JFileChooser saveChooser;

  public Dialog() {
  }

  private static FileChooser openChooser;
  private FilePreview openPreview;

  @Override
  public String getOpenFileNameFromDialog(Map<String, Object> vwrOptions,
                                          Viewer vwr,
                                          String fileName, JmolAppAPI jmolApp,
                                          String windowName,
                                          boolean allowAppend) {

    if (openChooser == null) {
      openChooser = new FileChooser();
      Object temp = UIManager.get("FileChooser.fileNameLabelText");
      UIManager.put("FileChooser.fileNameLabelText", GT.$("File or URL:"));
      getXPlatformLook(openChooser);
      UIManager.put("FileChooser.fileNameLabelText", temp);
    }
    if (openPreview == null
        && (vwr.isApplet || Boolean.valueOf(
            System.getProperty("openFilePreview", "true")).booleanValue())) {
      openPreview = new FilePreview(vwr, openChooser, allowAppend, vwrOptions);
    }

    if (jmolApp != null) {
      Dimension dim = jmolApp.getHistoryWindowSize(windowName);
      if (dim != null)
        openChooser.setDialogSize(dim);
      Point loc = jmolApp.getHistoryWindowPosition(windowName);
      if (loc != null)
        openChooser.setDialogLocation(loc);
    }

    openChooser.resetChoosableFileFilters();
    if (openPreview != null)
      openPreview.setPreviewOptions(allowAppend);

    if (fileName != null) {
      int pt = fileName.lastIndexOf(".");
      String sType = fileName.substring(pt + 1);
      if (pt >= 0 && sType.length() > 0)
        openChooser.addChoosableFileFilter(new TypeFilter(sType));
      if (fileName.indexOf(".") == 0)
        fileName = "Jmol" + fileName;
      if (fileName.length() > 0)
        openChooser.setSelectedFile(new File(fileName));
    }
    //System.out.println("fileName for dialog: " + fileName);
    if (fileName == null || fileName.indexOf(":") < 0 && fileName.indexOf("/") != 0) {
      File dir = (File) FileManager.getLocalDirectory(vwr, true);
      //System.out.println("directory for dialog: " + dir.getAbsolutePath());
      openChooser.setCurrentDirectory(dir);
    }
    File file = null;
    if (openChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
      file = openChooser.getSelectedFile();


    if (file == null)
      return closePreview();
    
    if (jmolApp != null)
      jmolApp.addHistoryWindowInfo(windowName, openChooser.getDialog(), null);

    String url = vwr.getLocalUrl(file.getAbsolutePath());
    if (url != null) {
      fileName = url;
    } else {
      FileManager.setLocalPath(vwr, file.getParent(), true);
      fileName = file.getAbsolutePath();
    }
    if (fileName.startsWith("/"))
      fileName = "file://" + fileName; // for Macs
    boolean doCartoons = (jmolApp == null || allowAppend && openPreview != null && openPreview.isCartoonsSelected());
    boolean doAppend = (allowAppend && !FileManager.isScriptType(fileName) 
        && openPreview != null && openPreview.isAppendSelected());
    closePreview();
    return (doCartoons ? "" : "#NOCARTOONS#;") + (doAppend ? "#APPEND#;" : "") + fileName;
  }

  String closePreview() {
    if (openPreview != null)
      openPreview.doUpdatePreview(null);
    return null;
  }
  
  @Override
  public String getSaveFileNameFromDialog(Viewer vwr, String fileName,
                                          String type) {
    if (saveChooser == null) {
      saveChooser = new JFileChooser();
      getXPlatformLook(saveChooser);
    }
    saveChooser.setCurrentDirectory((File) FileManager.getLocalDirectory(vwr, true));
    File file = null;
    saveChooser.resetChoosableFileFilters();
    if (fileName != null) {
      int pt = fileName.lastIndexOf(".");
      String sType = fileName.substring(pt + 1);
      if (pt >= 0 && sType.length() > 0)
        saveChooser.addChoosableFileFilter(new TypeFilter(sType));
      if (fileName.equals("*"))
        fileName = vwr.getModelSetFileName();
      if (fileName.indexOf(".") == 0)
        fileName = "Jmol" + fileName;
      file = new File(fileName);
    }
    if (type != null)
      saveChooser.addChoosableFileFilter(new TypeFilter(type));
    saveChooser.setSelectedFile(file);
    if ((file = showSaveDialog(this, saveChooser, file)) == null)
      return null;
    FileManager.setLocalPath(vwr, file.getParent(), true);
    return file.getAbsolutePath();
  }

  
  @Override
  public String getImageFileNameFromDialog(Viewer vwr, String fileName,
                                           String type, String[] imageChoices,
                                           String[] imageExtensions,
                                           int qualityJPG0, int qualityPNG0) {
    if (qualityJPG0 < 0 || qualityJPG0 > 100)
      qualityJPG0 = qualityJPG;
    if (qualityPNG0 < 0)
      qualityPNG0 = qualityPNG;
    if (qualityPNG0 > 9)
      qualityPNG0 = 2;
    qualityJPG = qualityJPG0;
    qualityPNG = qualityPNG0;
    if (extension == null)
      extension = "jpg";
    
    if (imageChooser == null) {
      imageChooser = new JFileChooser();
      getXPlatformLook(imageChooser);
    }
    imageChooser.setCurrentDirectory((File) FileManager.getLocalDirectory(vwr, true));
    imageChooser.resetChoosableFileFilters();
    File file = null;
    if (fileName == null) {
      fileName = vwr.getModelSetFileName();
      if (fileName.indexOf("?") >= 0)
        fileName = fileName.substring(0, fileName.indexOf("?"));
      String pathName = imageChooser.getCurrentDirectory().getPath();
      if (fileName != null && pathName != null) {
        int extensionStart = fileName.lastIndexOf('.');
        if (extensionStart != -1) {
          fileName = fileName.substring(0, extensionStart) + "."
              + extension;
        }
        file = new File(pathName, fileName);
      }
    } else {
      if (fileName.indexOf(".") == 0)
        fileName = "Jmol" + fileName;
      file = new File(fileName);
      type = fileName.substring(fileName.lastIndexOf(".") + 1);
      for (int i = 0; i < imageExtensions.length; i++)
        if (type.equals(imageChoices[i])
            || type.toLowerCase().equals(imageExtensions[i])) {
          type = imageChoices[i];
          break;
        }
    }
    createExportPanel(imageChoices, imageExtensions, type);
    imageChooser.setSelectedFile(initialFile = file);
    if ((file = showSaveDialog(this, imageChooser, file)) == null)
      return null;
    qualityJPG = qSliderJPEG.getValue();
    qualityPNG = qSliderPNG.getValue();
    if (cb.getSelectedIndex() >= 0)
      defaultChoice = cb.getSelectedIndex();
    FileManager.setLocalPath(vwr, file.getParent(), true);
    return file.getAbsolutePath();
  }

  File initialFile;

  private void createExportPanel(String[] choices,
                          String[] extensions, String type) {
    imageChooser.setAccessory(this);
    setLayout(new BorderLayout());
    if (type == null || type.equals("JPG"))
      type = "JPEG";
    for (defaultChoice = choices.length; --defaultChoice >= 1;)
      if (choices[defaultChoice].equals(type))
        break;
    extension = extensions[defaultChoice];
    choice = choices[defaultChoice];
    this.extensions = extensions;
    imageChooser.resetChoosableFileFilters();
    imageChooser.addChoosableFileFilter(new TypeFilter(extension));
    JPanel cbPanel = new JPanel();
    cbPanel.setLayout(new FlowLayout());
    cbPanel.setBorder(new TitledBorder(GT.$("Image Type")));
    cb = new JComboBox<String>();
    for (int i = 0; i < choices.length; i++) {
      cb.addItem(choices[i]);
    }
    cbPanel.add(cb);
    cb.setSelectedIndex(defaultChoice);
    cb.addItemListener(new ExportChoiceListener());
    add(cbPanel, BorderLayout.NORTH);

    JPanel qPanel2 = new JPanel();
    qPanel2.setLayout(new BorderLayout());

    qPanelJPEG = new JPanel();
    qPanelJPEG.setLayout(new BorderLayout());
    qPanelJPEG.setBorder(new TitledBorder(GT.i(GT.$("JPEG Quality ({0})"),
        qualityJPG)));
    qSliderJPEG = new JSlider(SwingConstants.HORIZONTAL, 50, 100, qualityJPG);
    qSliderJPEG.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    qSliderJPEG.setPaintTicks(true);
    qSliderJPEG.setMajorTickSpacing(10);
    qSliderJPEG.setPaintLabels(true);
    qSliderJPEG.addChangeListener(new QualityListener(true, qSliderJPEG));
    qPanelJPEG.add(qSliderJPEG, BorderLayout.SOUTH);
    qPanel2.add(qPanelJPEG, BorderLayout.NORTH);

    qPanelPNG = new JPanel();
    qPanelPNG.setLayout(new BorderLayout());
    qPanelPNG
        .setBorder(new TitledBorder(GT.i(GT.$("PNG Compression  ({0})"), qualityPNG)));
    qSliderPNG = new JSlider(SwingConstants.HORIZONTAL, 0, 9, qualityPNG);
    qSliderPNG.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    qSliderPNG.setPaintTicks(true);
    qSliderPNG.setMajorTickSpacing(2);
    qSliderPNG.setPaintLabels(true);
    qSliderPNG.addChangeListener(new QualityListener(false, qSliderPNG));
    qPanelPNG.add(qSliderPNG, BorderLayout.SOUTH);
    qPanel2.add(qPanelPNG, BorderLayout.SOUTH);
    add(qPanel2, BorderLayout.SOUTH);
  }

  public class QualityListener implements ChangeListener {

    private boolean isJPEG;
    private JSlider slider;

    public QualityListener(boolean isJPEG, JSlider slider) {
      this.isJPEG = isJPEG;
      this.slider = slider;
    }

    @Override
    public void stateChanged(ChangeEvent arg0) {
      int value = slider.getValue();
      if (isJPEG) {
        qualityJPG = value;
        qPanelJPEG
            .setBorder(new TitledBorder(GT.i(GT.$("JPEG Quality ({0})"), value)));
      } else {
        qualityPNG = value;
        qPanelPNG.setBorder(new TitledBorder(GT.i(GT.$("PNG Quality ({0})"), value)));
      }
    }
  }

  public class ExportChoiceListener implements ItemListener {
    @Override
    @SuppressWarnings("unchecked")
    public void itemStateChanged(ItemEvent e) {

      JComboBox<String> source = (JComboBox<String>) e.getSource();
      File selectedFile = imageChooser.getSelectedFile();
      if (selectedFile == null)
        selectedFile = initialFile;
      File newFile = null;
      String name;
      String newExt = extensions[source.getSelectedIndex()];
      if ((name = selectedFile.getName()) != null
          && name.endsWith("." + extension)) {
        name = name.substring(0, name.length() - extension.length());
        name += newExt;
        initialFile = newFile = new File(selectedFile.getParent(), name);
      }
      extension = newExt;
      imageChooser.resetChoosableFileFilters();
      imageChooser.addChoosableFileFilter(new TypeFilter(extension));
      if (newFile != null)
        imageChooser.setSelectedFile(newFile);
      choice = (String) source.getSelectedItem();
    }
  }

  /* (non-Javadoc)
   * @see org.jmol.export.JmolImageTyperInterface#getType()
   */
  @Override
  public String getType() {
    return choice;
  }

  /* (non-Javadoc)
   * @see org.jmol.export.JmolImageTyperInterface#getQuality(java.lang.String)
   */
  @Override
  public int getQuality(String sType) {
    return (sType.equals("JPEG") || sType.equals("JPG") ? qualityJPG : sType
        .equals("PNG") ? qualityPNG : -1);
  }

  private static boolean doOverWrite(JFileChooser chooser, File file) {
    Object[] options = { GT.$("Yes"), GT.$("No") };
    int opt = JOptionPane.showOptionDialog(chooser, GT.o(GT.$(
        "Do you want to overwrite file {0}?"), file.getAbsolutePath()), GT
        .$("Warning"), JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
        null, options, options[0]);
    return (opt == 0);
  }

  private File showSaveDialog(Component c,
                                     JFileChooser chooser, File file) {
    while (true) {
      if (chooser.showSaveDialog(c) != JFileChooser.APPROVE_OPTION)
        return null;
      if (cb != null && cb.getSelectedIndex() >= 0)
        defaultChoice = cb.getSelectedIndex();
      if ((file = chooser.getSelectedFile()) == null || !file.exists()
          || doOverWrite(chooser, file))
        return file;
    }
  }

  public static class TypeFilter extends FileFilter {

    String thisType;

    TypeFilter(String type) {
      thisType = type.toLowerCase();
    }

    @Override
    public boolean accept(File f) {
      if (f.isDirectory() || thisType == null) {
        return true;
      }
      String ext = f.getName();
      int pt = ext.lastIndexOf(".");
      return (pt >= 0 && ext.substring(pt + 1).toLowerCase().equals(thisType));
    }

    @Override
    public String getDescription() {
      return thisType.toUpperCase() + " (*." + thisType + ")";
    }

  }

  static boolean haveTranslations = false;

  @Override
  public void setupUI(boolean forceNewTranslation) {
    if (forceNewTranslation || !haveTranslations)
      setupUIManager();
    haveTranslations = true;
  }

  /**
   * Setup the UIManager (for i18n) 
   */

  public static void setupUIManager() {

    // FileChooser strings
    UIManager.put("FileChooser.acceptAllFileFilterText", GT.$("All Files"));
    UIManager.put("FileChooser.cancelButtonText", GT.$("Cancel"));
    UIManager.put("FileChooser.cancelButtonToolTipText", GT
        .$("Abort file chooser dialog"));
    UIManager.put("FileChooser.detailsViewButtonAccessibleName", GT
        .$("Details"));
    UIManager.put("FileChooser.detailsViewButtonToolTipText", GT.$("Details"));
    UIManager.put("FileChooser.directoryDescriptionText", GT.$("Directory"));
    UIManager.put("FileChooser.directoryOpenButtonText", GT.$("Open"));
    UIManager.put("FileChooser.directoryOpenButtonToolTipText", GT
        .$("Open selected directory"));
    UIManager.put("FileChooser.fileAttrHeaderText", GT.$("Attributes"));
    UIManager.put("FileChooser.fileDateHeaderText", GT.$("Modified"));
    UIManager.put("FileChooser.fileDescriptionText", GT.$("Generic File"));
    UIManager.put("FileChooser.fileNameHeaderText", GT.$("Name"));
    UIManager.put("FileChooser.fileNameLabelText", GT.$("File Name:"));
    UIManager.put("FileChooser.fileSizeHeaderText", GT.$("Size"));
    UIManager.put("FileChooser.filesOfTypeLabelText", GT.$("Files of Type:"));
    UIManager.put("FileChooser.fileTypeHeaderText", GT.$("Type"));
    UIManager.put("FileChooser.helpButtonText", GT.$("Help"));
    UIManager
        .put("FileChooser.helpButtonToolTipText", GT.$("FileChooser help"));
    UIManager.put("FileChooser.homeFolderAccessibleName", GT.$("Home"));
    UIManager.put("FileChooser.homeFolderToolTipText", GT.$("Home"));
    UIManager.put("FileChooser.listViewButtonAccessibleName", GT.$("List"));
    UIManager.put("FileChooser.listViewButtonToolTipText", GT.$("List"));
    UIManager.put("FileChooser.lookInLabelText", GT.$("Look In:"));
    UIManager.put("FileChooser.newFolderErrorText", GT
        .$("Error creating new folder"));
    UIManager.put("FileChooser.newFolderAccessibleName", GT.$("New Folder"));
    UIManager
        .put("FileChooser.newFolderToolTipText", GT.$("Create New Folder"));
    UIManager.put("FileChooser.openButtonText", GT.$("Open"));
    UIManager.put("FileChooser.openButtonToolTipText", GT
        .$("Open selected file"));
    UIManager.put("FileChooser.openDialogTitleText", GT.$("Open"));
    UIManager.put("FileChooser.saveButtonText", GT.$("Save"));
    UIManager.put("FileChooser.saveButtonToolTipText", GT
        .$("Save selected file"));
    UIManager.put("FileChooser.saveDialogTitleText", GT.$("Save"));
    UIManager.put("FileChooser.saveInLabelText", GT.$("Save In:"));
    UIManager.put("FileChooser.updateButtonText", GT.$("Update"));
    UIManager.put("FileChooser.updateButtonToolTipText", GT
        .$("Update directory listing"));
    UIManager.put("FileChooser.upFolderAccessibleName", GT.$("Up"));
    UIManager.put("FileChooser.upFolderToolTipText", GT.$("Up One Level"));

    // OptionPane strings
    UIManager.put("OptionPane.cancelButtonText", GT.$("Cancel"));
    UIManager.put("OptionPane.noButtonText", GT.$("No"));
    UIManager.put("OptionPane.okButtonText", GT.$("OK"));
    UIManager.put("OptionPane.yesButtonText", GT.$("Yes"));
  }
  
  private static boolean isMac = System.getProperty("os.name", "").startsWith("Mac");
  
  private static void getXPlatformLook(JFileChooser fc) {
    if (isMac) {
      LookAndFeel lnf = UIManager.getLookAndFeel();
      // JFileChooser on Mac OS X with the native L&F doesn't work well.
      // If the native L&F of Mac is selected, disable it for the file chooser
      if (lnf.isNativeLookAndFeel()) {
        try {
          UIManager.setLookAndFeel(UIManager
              .getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
          System.out.println(e.getMessage());
        }
        fc.updateUI();
        try {
          UIManager.setLookAndFeel(lnf);
        } catch (UnsupportedLookAndFeelException e) {
          System.out.println(e.getMessage());
        }
      }
    } else {
      fc.updateUI();
    }
  }

  protected String[] imageChoices = { "JPEG", "PNG", "GIF", "PPM" };
  protected String[] imageExtensions = { "jpg", "png", "gif", "ppm" };
  protected String outputFileName;
  protected String dialogType;
  protected String inputFileName;
  protected Viewer vwr;
  protected int qualityJ = -1;
  protected int qualityP = -1;
  protected String imageType;
  
  @Override
  public void setImageInfo(int qualityJPG, int qualityPNG, String imageType) {
    qualityJ = qualityJPG;
    qualityP = qualityPNG;
    this.imageType = imageType;
  }
  
  @Override
  public String getFileNameFromDialog(Viewer v, String dType, String iFileName) {
    this.vwr = v;
    this.dialogType = dType;
    this.inputFileName = iFileName;
    outputFileName = null;
    try {
      SwingUtilities.invokeAndWait(new Runnable() {
        @Override
        public void run() {
          if (dialogType.equals("Load")) {
            // may have #NOCARTOONS#; and/or "#APPEND#; prepended
            outputFileName = getOpenFileNameFromDialog(
                vwr.vwrOptions, vwr, inputFileName, null, null, false);
            return; 
          }
          if (dialogType.equals("Save")) {
            outputFileName = getSaveFileNameFromDialog(vwr,
                inputFileName, null);
            return;
          }
          if (dialogType.startsWith("Save Image")) {
            outputFileName = getImageFileNameFromDialog(vwr,
                inputFileName, imageType, imageChoices, imageExtensions,
                qualityJ, qualityP);
            return;
          }
          outputFileName = null;
        }
      });
    } catch (Exception e) {
      Logger.error(e.getMessage());
    }
    return outputFileName;
  }

}
