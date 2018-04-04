package org.gennbo;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileFilter;

import javajs.util.PT;
import javajs.util.SB;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jmol.i18n.GT;
import org.jmol.util.Logger;

/**
 * Builds the input file box found in all 4 modules JPanel containing file input
 * box
 */
class NBOFileHandler extends JPanel {

  protected static final String sep = System.getProperty("line.separator");

  private static final String[] PLOT_AND_NBO_FILES = PT.split("31;32;33;34;35;36;37;38;39;40;41;42;46;nbo", ";");

  final static String[] MODEL_OPEN_OPTIONS = { "<Select File Type>", 
    "[.47]   NBO Archive",
    "[.gau]  Gaussian Input", 
    "[.log]  Gaussian Output",
    "[.gms]  GAMESS Input", 
    "[.adf]  ADF Input",
    "[.jag]  Jaguar Input",
    "[.mm2]  MM2-Input", 
    "[.mnd]  Dewar Type Input", 
    "[.mp]   Molpro Input", 
    "[.nw]   NWChem Input", 
    "[.orc]  Orca Input", 
    "[.pqs]  PQS Input", 
    "[.qc]   Q-Chem Input", 
    "[.cfi]  NBO Cartesian", 
    "[.vfi]  NBO Valence", 
    "[.xyz]  XYZ", 
    "[.mol]  MOL"
    };

  final static String[] MODEL_SAVE_OPTIONS = {  "<Select File Type>",
    "Gaussian Input             [.gau]",
    "Gaussian Input (Cartesian) [.gau]",
    "Gaussian Input (z-Matrix)  [.gau]",
    "GAMESS Input               [.gms]",
    "ADF Input                  [.adf]",
    "Jaguar Input               [.jag]",
    "MM2-Input                  [.mm2]",
    "Dewar Type Input           [.mnd]",
    "Molpro Input               [.mp]", 
    "NWChem Input               [.nw]",
    "Orca Input                 [.orc]",
    "PQS Input                  [.pqs]", 
    "Q-Chem Input               [.qc]",
    "NBO Cartesian              [.cfi]",
    "NBO Valence                [.vfi]",
    "XYZ                        [.xyz]",
    "MOL                        [.mol]"
    };

  private static final String GAUSSIAN_EXTENSIONS = ";gau;g09;com;";
  static final String MODEL_SAVE_FILE_EXTENSIONS = ";adf;cfi;gms;jag;mm2;mnd;mol;mp;nw;orc;pqs;qc;vfi;xyz" + GAUSSIAN_EXTENSIONS;
  static final String MODEL_OPEN_FILE_EXTENSIONS = MODEL_SAVE_FILE_EXTENSIONS + "log;47;";
  static final String JMOL_EXTENSIONS = ";xyz;mol;";
  //  protected static final String RUN_EXTENSIONS = "47;gau;gms";

  protected final static int MODE_MODEL_OPEN = 1;
  protected final static int MODE_RUN = 2;
  protected final static int MODE_VIEW = 3;
  protected final static int MODE_SEARCH = 4;
  protected final static int MODE_MODEL_SAVE = 5;

  protected File file47;


  protected JTextField tfDir, tfName, tfExt;
  JButton btnBrowse;
  
  /**
   * working directory for input
   * 
   * caution should be used here; this value in general should not 
   * differ from tfDir except in MODEL, where the user can set that.
   *  
   */
  protected String fullFilePath;
  
  /**
   * file root for input
   * 
   * caution should be used here; this value in general should not 
   * differ from tfName except for Model, where the user can set that.
   *  
   */
  protected String jobStem;
  
  
  private String useExt;
  private NBODialog dialog;
  private boolean canReRun;

  private NBOFileAcceptor fileAcceptor;
  private int dialogMode;
  protected String saveFilter;

  /**
   * Create a new instance of an file open or save JPanel
   * 
   * @param jobName
   * @param mode
   * @param dialog
   * @param acceptor
   */
  NBOFileHandler(String jobName, final int mode, NBODialog dialog,
      NBOFileAcceptor acceptor) {
    dialogMode = mode;
    this.useExt = (mode == MODE_MODEL_SAVE ? "" : "47");
    this.dialog = dialog;
    fileAcceptor = acceptor;
    canReRun = true;
    fullFilePath = dialog.getWorkingPath();
    setLayout(new GridBagLayout());
    setMaximumSize(new Dimension(350, 40));
    setPreferredSize(new Dimension(350, 40));
    setMinimumSize(new Dimension(350, 40));
    GridBagConstraints c = new GridBagConstraints();
    boolean showExtensionField = (mode == MODE_MODEL_SAVE || mode == MODE_MODEL_OPEN);
    boolean canEditTextFields = false;
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.BOTH;
    (tfDir = new JTextField()).setPreferredSize(new Dimension(110, 20));
    tfDir.setEditable(canEditTextFields);
    //    tfDir.addActionListener(new ActionListener() {
    //      @Override
    //      public void actionPerformed(ActionEvent e) {
    //
    //        browsePressed();
    //      }
    //    });
    tfDir.setText(fullFilePath);
    add(tfDir, c);
    c.gridx = 1;
    (tfName = new JTextField()).setPreferredSize(new Dimension(120, 20));
    tfName.setEditable(canEditTextFields);
    //    tfName.addActionListener(new ActionListener() {
    //      @Override
    //      public void actionPerformed(ActionEvent e) {
    //        //        if(mode == MODEL)
    //        //          showWorkpathDialogM(null,null);
    //        browsePressed();
    //      }
    //    });
    add(tfName, c);
    c.gridx = 0;
    c.gridy = 1;
    add(new JLabel("         folder"), c);
    c.gridx = 1;
    add(new JLabel("          name"), c);
    c.gridx = 2;
    c.gridy = 0;
    (tfExt = new JTextField()).setPreferredSize(new Dimension(40, 20));
    tfExt.setEditable(canEditTextFields);
    tfExt.setText(useExt);
    //    tfExt.addActionListener(new ActionListener() {
    //      @Override
    //      public void actionPerformed(ActionEvent e) {
    //        browsePressed();
    //      }
    //    });
    if (showExtensionField) {
      add(tfExt, c);
      c.gridy = 1;
      add(new JLabel("  ext"), c);
    }
    c.gridx = 3;
    c.gridy = 0;
    c.gridheight = 2;
    btnBrowse = new JButton(mode == MODE_MODEL_SAVE ? "Save" : "Browse");
    btnBrowse.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        doFileBrowsePressed();
      }
    });
    add(btnBrowse, c);
    setJobStemAndTextFieldName(jobName);
    setTextFields(fullFilePath, jobName, useExt);
  }

  /**
   * Open a JDialog containing a JFileChooser
   *  
   * @return false if aborting
   * 
   */
  protected boolean doFileBrowsePressed() {
    if (dialog.nboService.getWorkingMode() == NBODialog.DIALOG_RUN) {
      int i = JOptionPane.showConfirmDialog(dialog,
          "Warning, changing jobs while running GenNBO can effect output files."
              + "\nContinue anyway?");
      if (i == JOptionPane.NO_OPTION)
        return false;
    }
    final JFileChooser myChooser = new NBOFileChooser();
    String folder = tfDir.getText().trim();
    String name = tfName.getText();
    String ext = tfExt.getText();
    File newFile;
    if (folder.length() > 0)
      fullFilePath = NBOUtil.getWindowsFullNameFor(folder,
          name.length() == 0 ? " " : name + (useExt.equals("47") ? ".47" : ""),
          null);
    switch (dialogMode) {
    case MODE_MODEL_OPEN:
      if (ext.length() == 0)
        ext = "*";
      String path = NBOUtil.getWindowsFullNameFor(folder, name, ext);
      setFilterForDialog(myChooser, MODE_MODEL_OPEN, ext);
      if (path.endsWith("/"))
        path += "*.*";
      if (!path.equals(""))
        myChooser.setSelectedFile(new File(path));
      if (myChooser.showDialog(this, GT._("Select")) != JFileChooser.APPROVE_OPTION)
        break;
      newFile = myChooser.getSelectedFile();
      if (newFile.toString().indexOf(".") < 0) {
        dialog.logError("File not found");
        return false;
      }
      fullFilePath = newFile.getParent();
      jobStem = NBOUtil.getJobStem(newFile);
      ext = NBOUtil.getExt(newFile);
      if (!PT.isOneOf(ext, MODEL_OPEN_FILE_EXTENSIONS)) {
        dialog.logError("Invalid file extension");
        break;
      }
      setTextFields(fullFilePath, jobStem, ext);
      dialog.saveWorkingPath(fullFilePath);
      fileAcceptor
          .acceptFile(MODE_MODEL_OPEN, fullFilePath, jobStem, ext, null);
      return true;
    case MODE_MODEL_SAVE:
      myChooser.addPropertyChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          if (evt.getPropertyName().equals("fileFilterChanged")) {
            saveFilter = myChooser.getFileFilter().getDescription();
          }
        }

      });
      useExt = ext = (ext.equals("") ? "cfi" : ext);
      setFilterForDialog(myChooser, MODE_MODEL_SAVE,
          fileAcceptor.getDefaultFilterOption());
      String savePath = NBOUtil.getWindowsFolderFor(folder, fullFilePath);
      if (name.equals("") && jobStem != null)
        savePath = tfDir.getText()
            + "/"
            + (jobStem.equals("") ? "new.cfi" : jobStem
                + (ext.contains(";") ? "" : "." + ext));
      else
        savePath = tfDir.getText() + "/" + name + "." + ext;
      myChooser.setSelectedFile(new File(savePath));
      if (myChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
        break;
      newFile = myChooser.getSelectedFile();
      fullFilePath = newFile.getParent();
      jobStem = NBOUtil.getJobStem(newFile);
      ext = NBOUtil.getExt(newFile);
      boolean isAll = (saveFilter.indexOf("(") < 0);
      ext = (isAll ? ext 
          : saveFilter.indexOf(",") < 0 ? saveFilter.substring(saveFilter.indexOf("(*") + 3,
              saveFilter.length() - 1)
          : PT.isOneOf(ext, ";gau;g09;com;") ? ext 
          : "gau"
          );
      newFile = new File(fullFilePath, jobStem + "." + ext);
      if (!PT.isOneOf(ext, MODEL_SAVE_FILE_EXTENSIONS)) {
        dialog.logError("Invalid file extension");
        break;
      }
      if (newFile.exists()
          && JOptionPane.showConfirmDialog(null, "File " + newFile
              + " already exists, do you want to overwrite contents?",
              "Warning", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
        return false;
      dialog.inputFileHandler.setTextFields(fullFilePath, jobStem, ext);
      dialog.saveWorkingPath(fullFilePath);
      fileAcceptor.acceptFile(MODE_MODEL_SAVE, fullFilePath,
          NBOUtil.getJobStem(newFile), ext,
          (isAll ? null : saveFilter.substring(0, saveFilter.indexOf("(*"))));
      return true;
    default:
      myChooser.setFileFilter(new FileNameExtensionFilter(useExt, useExt));
      myChooser.setSelectedFile(new File(fullFilePath));
      if (myChooser.showDialog(this, GT._("Select")) != JFileChooser.APPROVE_OPTION)
        break;
      return loadSelectedFile(myChooser.getSelectedFile());
    }
    return false;
  }

  /**
   * Set the file filters for the file dialog
   * 
   * @param chooser
   * @param mode
   * @param defaultExt
   */
  private static void setFilterForDialog(JFileChooser chooser, int mode,
                                String defaultExt) {
    FileNameExtensionFilter defaultFilter = null;
    if (defaultExt != null && PT.isOneOf(defaultExt, GAUSSIAN_EXTENSIONS))
        defaultExt = "gau";
    boolean isOpen = (mode == MODE_MODEL_OPEN);
    String[] options = (isOpen ? MODEL_OPEN_OPTIONS
        : MODEL_SAVE_OPTIONS);
    String allFiles = (isOpen ? MODEL_OPEN_FILE_EXTENSIONS : MODEL_SAVE_FILE_EXTENSIONS);
    String[] allExt = allFiles.substring(1, allFiles.length() - 1).split(";"); 
    FileNameExtensionFilter[] filters = new FileNameExtensionFilter[options.length - 1];
    for (int i = 1; i < options.length; i++) {
      String option = options[i];
      int pt = option.indexOf("[");
      int pt2 = option.indexOf("]");
      String label;
      if (pt == 0) {
        label = option.substring(pt2 + 1).trim();
      } else {
        label = option.substring(0, pt).trim();
      }
      String ext = option.substring(pt + 1, pt2).trim().substring(1);
      String[] a = (ext.equals("gau") ? new String[] { "gau", "g09", "com" }
          : new String[] { ext });
      String ext2 = (ext.equals("gau") ? " (*.gau, *.g09, *.com)" : " (*." + ext + ")");
      filters[i - 1] = new FileNameExtensionFilter(label + ext2, a);
      if (defaultFilter == null
          && (ext.equals(defaultExt) || label.indexOf(defaultExt) >= 0))
        defaultFilter = filters[i - 1];
    }
    for (int i = 0; i < filters.length; i++)
      chooser.addChoosableFileFilter(filters[i]);
    chooser.addChoosableFileFilter(new FileNameExtensionFilter("All NBOPro-" + (isOpen ? "Readable" : "Writable") + " Types", allExt));
    chooser.setAcceptAllFileFilterUsed(true);
    chooser.setMultiSelectionEnabled(false);
    chooser.setFileHidingEnabled(true);
    if (defaultFilter != null)
      chooser.setFileFilter(defaultFilter);
  }

  /**
   * standard load of file after user selection
   * @param selectedFile
   * @return true for convenience only
   */
  protected boolean loadSelectedFile(File selectedFile) {
    dialog.nboService.restartIfNecessary();
    file47 = selectedFile;
    if (dialog.dialogMode == NBODialog.DIALOG_MODEL)
      return true;
    if (!useExt.equals("47")) {
      setJobStemAndTextFieldName(NBOUtil.getJobStem(file47));
      dialog.modelPanel.loadModelFromNBO(fullFilePath, jobStem, useExt);
      tfExt.setText(useExt);
      return true;
    }
    canReRun = true;
    setFile47(file47);
    dialog.runPanel.doLogJobName(jobStem);
    fullFilePath = file47.getParent();
    dialog.saveWorkingPath(fullFilePath);
    return true;
  }

  private void setJobStemAndTextFieldName(String name) {
    tfName.setText(jobStem = name);
  }

  /**
   * Sets up the input file, currently only support for .47/model input file
   * types
   * 
   * @param file47
   */
  protected void setFile47(File file47) {
    dialog.logValue("Input file=" + file47);
    deletePlotAndNBOFiles(false); // clear CURRENT input file's server directory
    this.file47 = file47;
    if (file47.getName().indexOf(".") > 0)
      jobStem = NBOUtil.getJobStem(file47);
    if (dialog.modelOrigin == NBODialog.ORIGIN_NBO_ARCHIVE)
      deletePlotAndNBOFiles(true);
    setTextFields(file47.getParent(), jobStem, useExt);
    if (!NBOUtil.getExt(file47).equals("47"))
      return;
    if (NBOUtil.fixPath(file47.getParent().toString()).equals(
        dialog.nboService.getServerPath(null))) {
      JOptionPane.showMessageDialog(this,
          "Select a directory that does not contain the NBOServe executable,"
              + "\nor select a new location for your NBOServe executable");
      return;
    }
    fullFilePath = file47.getParent();
    boolean canLoad = true;
    boolean isOK = true;
    String msg = "";
    if (dialog.dialogMode != NBODialog.DIALOG_MODEL && dialog.dialogMode != NBODialog.DIALOG_RUN) {
      for (String nn : PLOT_AND_NBO_FILES) {
        File f3 = newNBOFile(nn);
        if (!f3.exists() || nn.equals("36") && f3.length() == 0) {
          msg = "file " + f3 + " is missing or zero length";
          // BH: But this means all  || f3.length() == 0) {
          isOK = false;
          break;
        }
      }
    }
    if (!isOK) {
      if (dialog.dialogMode != NBODialog.DIALOG_RUN) {
        if (canReRun) {
          canReRun = false;
          dialog.runPanel.doRunGenNBOJob("PLOT");
        } else {
          dialog.alertError("Error occurred during run: " + msg);
        }
        return;
      }
      canLoad = false;
    }
    if (canLoad) {
      // this will trigger setViewerBasis and doSetNewBasis and a file load 
      dialog.loadNewFile(new File(fullFilePath + "/" + jobStem + ".47"));
    } else if (dialog.dialogMode == NBODialog.DIALOG_RUN) {
      dialog.modelPanel.loadModelFromNBO(fullFilePath, jobStem, useExt);
      setJobStemAndTextFieldName(jobStem);
      tfExt.setText("47");
    }
  }
  
  /**
   * Read input parameters from .47 file
   * 
   * @param doAll
   *        read the whole thing; else just for keywords (stops at $COORD)
   * 
   * @return NBOFileData47
   */
  private NBOFileData47 read47File(boolean doAll) {
    NBOFileData47 fileData = new NBOFileData47();
    String allKeywords = "";
    SB data = new SB();
    if (!NBOUtil.read47FileBuffered(file47, data, doAll))
      return fileData;
    String s = PT.trim(data.toString(), "\t\r\n ");
    String[] tokens = PT.split(s, "$END");
    if (tokens.length == 0)
      return fileData;
    SB preParams = new SB();
    SB postParams = new SB();
    SB params = preParams;
    // ignore everything after the last $END token
    for (int i = 0, n = tokens.length; i < n; i++) {
      s = PT.trim(tokens[i], "\t\r\n ");
      if (params == preParams && s.indexOf("$NBO") >= 0) {
        String[] prePost = PT.split(s, "$NBO");
        if (prePost[0].length() > 0) {
          params.append(s).append(sep);
          allKeywords = PT.trim(prePost[1], "\t\r\n ");
        }
        params = postParams;
        if (!doAll)
          break;
        continue;
      }
      params.append(s).append(sep).append("$END").append(sep);
    }
    dialog.logInfo("$NBO: " + allKeywords, Logger.LEVEL_INFO);
    return fileData.set(PT.rep(preParams.toString(), "FORMAT=PRECISE", ""),
        allKeywords, postParams.toString());
  }

  protected void deletePlotAndNBOFiles(boolean andUserDir) {
    if (jobStem.length() == 0)
      return;
    for (String ext : PLOT_AND_NBO_FILES)
      try {
        new File(dialog.nboService.getServerPath(jobStem + "." + ext)).delete();
      } catch (Exception e) {
        // ignore
      }
    if (andUserDir)
      for (String ext : PLOT_AND_NBO_FILES)
        try {
          NBOUtil.newNBOFile(file47, ext).delete();
        } catch (Exception e) {
          // ignore
        }
    file47 = null;
    if (dialog.dialogMode == NBODialog.DIALOG_VIEW)
      dialog.viewPanel.resetView();
  }

  protected void setTextFields(String dir, String name, String ext) {
    if (dir != null)
      tfDir.setText(dir);
    if (name != null)
      tfName.setText(name);
    if (tfExt != null)
      tfExt.setText(ext);
    if (dir != null && name != null && ext != null) {
      dialog.modelPanel.modelSetSaveParametersFromInput(this, dir, name, ext);
      file47 = new File(dir + "\\" + name + "." + ext);
    }
  }

  protected String getFileData(String fileName) {
    return dialog.vwr.getAsciiFileOrNull(fileName);
  }

  boolean writeToFile(String fileName, String s) {
    String ret = (s == null ? null : dialog.vwr.writeTextFile(fileName, s));
    return (ret != null && ret.startsWith("OK"));
  }

  void setBrowseEnabled(boolean b) {
    btnBrowse.setEnabled(b);
  }

  String getInputFileData(String nn) {
    return getFileData(newNBOFile(nn).toString());
  }

  File newNBOFile(String nn) {
    return NBOUtil.newNBOFile(file47, nn);
  }

  /**
   * Fix a file name to be [A-Z0-9_]+
   * 
   * @param name the name to fix, or if empty, the input file name truncated to a maximum of 10 digits
   * @return fixed name replacing not-allowed characters with '_'
   */
  String fixJobName(String name) {
    if (name == null || (name = name.trim()).length() == 0)
      name = jobStem;
    for (int i = name.length(); --i >= 0;) {
      char ch = name.charAt(i);
      if (!Character.isLetterOrDigit(ch) && ch != '_')
        name = name.substring(0, i) + "_" + name.substring(i + 1);
    }
    return name;
  }

  /**
   * 
   * @param jobName
   * @param keywords
   * @param isRun
   * @return [
   */
  
  NBOFileData47 update47File(String jobName, String keywords, boolean isRun) {
    if (!useExt.equals("47"))
      return null;
    String fileName47 = file47.getAbsolutePath();
    NBOFileData47 fileData = read47File(true);
    String oldData = (isRun ? getFileData(fileName47) : null);
    String newFileData = 
        fileData.preParams 
        + "$NBO\n "
        + "FILE=" + jobName + " " + keywords 
        + "  $END" 
        + sep 
        + fileData.postKeywordData;
    if (writeToFile(fileName47, newFileData)) {
      if (oldData != null)
        writeToFile(fileName47 + "$", oldData);
      fileData.noFileKeywords = keywords;
      fileData.allKeywords = "FILE=" + jobName + " " + keywords; 
      dialog.runPanel.doLogJobName(jobName);
      dialog.runPanel.doLogKeywords(keywords);
      return fileData;
    }
    dialog.logInfo("Could not create " + fileName47, Logger.LEVEL_ERROR);
    return null;
  }


  /**
   * Check to see if the jobName (from file= or tfJobName) and the jobStem (loaded file stem)
   * are the same, and if not, warn the user and offer them the opportunity to bail now.
   * In no case do we allow saving to a new .nbo file with a different name from the .47 file.
   * 
   * @param jobName
   * @return true if user agrees or does not have to; false to bail
   */
  boolean checkSwitch47To(String jobName) {
    if (!jobName.equals(jobStem)) {
      int i = JOptionPane
          .showConfirmDialog(
              null,
              "Note: Plot files are being created with name \""
                  + jobName
                  + "\", which does not match your file name \""
                  + jobStem
                  + "\"\nTo continue, we must create a new .47 file \""
                  + jobName
                  + ".47\" so that all files related to this job are under the same name. Continue?",
              "Warning", JOptionPane.YES_NO_OPTION);
      if (i != JOptionPane.YES_OPTION)
        return false;
      String data = dialog.vwr.getAsciiFileOrNull(file47.getAbsolutePath());
      setJobStemAndTextFieldName(jobName);
      setTextFields(tfDir.getText(), jobName, "47");
      if (data != null)
        this.writeToFile(file47.getAbsolutePath(), data);    
    }
    return true;
  }

  /**
   * Delete all files ending with $
   */
  void removeAllTemporaryRunFiles() {
    File[] files = new File(fullFilePath).listFiles(new FileFilter() {
      @Override
      public boolean accept(File theFile) {
        if (theFile.isFile()) {
          return theFile.getName().endsWith("$");
        }
        return false;
      }
    });
    for (File f : files) 
      deleteFile(f);
  }

  /**
   * delete a file
   * 
   * @param f
   */
  void deleteFile(File f) {
    try {
      if (f.exists())
        f.delete();
    } catch (Exception e) {
      System.out.println("Could not delete " + f);
    }
  }

  /**
   * just get the keywords from the .47 file, without FILE=xxxx 
   * 
   * @return NBO keywords
   */
  String get47KeywordsNoFile() {
    return read47File(false).noFileKeywords;
  }

  /**
   * Checks to see that the .nbo file was created fully and, if not, deletes it,
   * moving its contents to .err$, and deletes all plot files
   * 
   * @param saveErr  write the current .nbo as .err$ file  
   * 
   * @return true if .nbo file is OK
   */
  boolean checkNBOComplete(boolean saveErr) {
    deleteFile(newNBOFile("err$"));
    String data = getInputFileData("nbo");
    boolean isOK = data != null && data.contains("NBO analysis completed");
    if (!isOK) {
      deleteFile(newNBOFile("nbo"));
      if (saveErr && data != null)
        writeToFile(newNBOFile("err$").getAbsolutePath(), data);
      writeToFile(newNBOFile("47").getAbsolutePath(), getInputFileData("47$"));
      deletePlotFiles(fullFilePath);
    }
    deleteFile(newNBOFile("47$"));
    deleteServerFiles();
    return isOK;
  }

  /**
   * 
   * Delete .3*, .nbo, and .4* in server directory
   * 
   */
  private void deleteServerFiles() {
    deletePlotFiles(null);   
  }

  /**
   * Delete .3*, .nbo, and .4* (other than .47 when path is not null)
   * @param path optional user directory or null for cleaning the server directory
   */
  void deletePlotFiles(String path) {
    final boolean isServerPath = (path == null);
      File[] files = new File(isServerPath ? dialog.nboService.getServerPath(null) : path).listFiles(new FileFilter() {
      @Override
      public boolean accept(File theFile) {
        if (theFile.isFile()) {
          String name = theFile.getName();
          return (name.startsWith(jobStem + ".3") 
              || name.startsWith(jobStem + ".4")
              && (isServerPath || name.indexOf(".47") < 0))
              || name.equals(jobStem + ".nbo");
        }
        return false;
      }
      });
      for (File f : files) 
        deleteFile(f);
    }

  /**
   * 
   * Get the proper code for the C_ESS record
   * 
   * @param ext
   *        - extension to convert
   * @param saveType
   *        - the combo box description, indicating Cartesian or z-matrix
   * @return one of g, gc, gz, c, v, l, a, or mm if recognhized or ext if not
   */
  static String getEss(String ext, String saveType) {
    ext = ext.toLowerCase();
    return (PT.isOneOf(ext,  GAUSSIAN_EXTENSIONS) ?
        (saveType != null && saveType.contains("Cartesian") ? "gc" : saveType.contains("z-matrix") ? "gz" : "g")
          : ext.equals("cfi") || ext.equals("vfi")|| ext.equals("log") ? ext.substring(0, 1)
            : ext.equals("47") ? "a" 
            : ext.equals("mm2") ? "mm" 
            : ext);
  }

  private class NBOFileChooser extends JFileChooser {

    NBOFileChooser() {
      super();
    }

  }
}
