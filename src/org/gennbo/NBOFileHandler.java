package org.gennbo;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

  protected static final String EXTENSIONS = "31;32;33;34;35;36;37;38;39;40;41;42;46;nbo";
  protected static final String[] EXT_ARRAY = PT.split(EXTENSIONS, ";");
  
  protected File inputFile;


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
  
  
  protected String useExt;
  protected NBODialog dialog;
  protected boolean canReRun;
  protected boolean isOpenShell;

  protected final static int MODE_MODEL_USE = 1;
  protected final static int MODE_RUN = 2;
  protected final static int MODE_VIEW = 3;
  protected final static int MODE_SEARCH = 4;
  protected final static int MODE_MODEL_SAVE = 5;

  
  public NBOFileHandler(String jobName, String ext, final int mode, String useExt,
      NBODialog dialog) {
    this.dialog = dialog;
    canReRun = true;
    fullFilePath = dialog.getWorkingPath();
    this.useExt = useExt;
    setLayout(new GridBagLayout());
    setMaximumSize(new Dimension(350, 40));
    setPreferredSize(new Dimension(350, 40));
    setMinimumSize(new Dimension(350, 40));
    GridBagConstraints c = new GridBagConstraints();
    boolean extShowTextFields = (mode == MODE_MODEL_SAVE || mode == MODE_MODEL_USE);
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
    tfExt.setText(ext); 
    //    tfExt.addActionListener(new ActionListener() {
    //      @Override
    //      public void actionPerformed(ActionEvent e) {
    //        browsePressed();
    //      }
    //    });
    if (extShowTextFields) {
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
    setInput(fullFilePath, jobName, ext);
  }

  /*
   * Chooser allows multiple jobs selection with *
   */
  protected boolean doFileBrowsePressed() {
    if (dialog.nboService.getWorkingMode() == NBODialog.DIALOG_RUN) {
      int i = JOptionPane.showConfirmDialog(dialog,
          "Warning, changing jobs while running GenNBO can effect output files."
              + "\nContinue anyway?");
      if (i == JOptionPane.NO_OPTION)
        return false;
    }
    
   
//  JFileChooser myChooser = new JFileChooser();
//  myChooser.setFileFilter(new FileNameExtensionFilter(useExt, useExt));
//  myChooser.setFileHidingEnabled(true);
//  myChooser.setMultiSelectionEnabled(true);
//  
//  String folder = tfDir.getText();
//  File[] filesInDir = new File(folder).listFiles();
//  List<File> files = new ArrayList<File>();
//  String name = tfName.getText();
//  
//  if(name.contains("*")){
//    String[] nameComponents = name.split("*");
//    for(File f: filesInDir){
//      for(int i =0; i < nameComponents.length; i++){
//        if(!f.getName().contains(nameComponents[i])){
//           break;
//        }
//      }
//      files.add(f);
//    }
//  }
//  
//  for(File f : files){
//  System.out.println(f.getName());
//  }
//  
// for(File f : files){
//  if (folder.length() > 0)
//    fullFilePath = NBOUtil.getWindowsFullNameFor(folder, f.getName().length() == 0 ? " " : f.getName() + (useExt.equals("47") ? ".47" : ""), null);
//  myChooser.setSelectedFile(new File(fullFilePath));
//  }
// 
// // let chooser remember the previous folder location and thus to save time finding the folder every time
// if (folder != null)
//  myChooser.setCurrentDirectory(new File(folder));
//  int button = myChooser.showDialog(this, GT._("Select"));
//  if (button == JFileChooser.APPROVE_OPTION)
//    return loadSelectedFile(myChooser.getSelectedFile());
// 
// 
 // OLD 
    JFileChooser myChooser = new JFileChooser();
    myChooser.setFileFilter(new FileNameExtensionFilter(useExt, useExt));
    myChooser.setFileHidingEnabled(true);

    String folder = tfDir.getText();
    String name = tfName.getText();
    if (folder.length() > 0)
      fullFilePath = NBOUtil.getWindowsFullNameFor(folder, name.length() == 0 ? " " : name + (useExt.equals("47") ? ".47" : ""), null);
    myChooser.setSelectedFile(new File(fullFilePath));
    int button = myChooser.showDialog(this, GT._("Select"));
    if (button == JFileChooser.APPROVE_OPTION)
      return loadSelectedFile(myChooser.getSelectedFile());
 
    return true;
  }

  protected boolean loadSelectedFile(File selectedFile) {
    dialog.nboService.restartIfNecessary();
    inputFile = selectedFile;
    //if(!useExt.equals("47")&&!useExt.equals("31")&&!useExt.equals("nbo")) 
    //return false;
    if (dialog.dialogMode == NBODialog.DIALOG_MODEL)
      return true;
    if (!useExt.equals("47")) {
      setJobStemAndTextFieldName(NBOUtil.getJobStem(inputFile));
      dialog.modelPanel.loadModelFromNBO(fullFilePath, jobStem, useExt);
      tfExt.setText(useExt);
      return true;
    }
    canReRun = true;
    setInputFile(inputFile);
    dialog.runPanel.doLogJobName(jobStem);
    fullFilePath = inputFile.getParent();
    dialog.saveWorkingPath(fullFilePath.toString());
    return true;
  }

  private void setJobStemAndTextFieldName(String name) {
    tfName.setText(jobStem = name);
  }

  /**
   * Sets up the input file, currently only support for .47/model input file
   * types
   * 
   * @param inputFile
   */
  protected void setInputFile(File inputFile) {
    dialog.logValue("Input file=" + inputFile);
    clearInputFile(false); // clear CURRENT input file's server directory
    this.inputFile = inputFile;
    if (inputFile.getName().indexOf(".") > 0)
      jobStem = NBOUtil.getJobStem(inputFile);
    if (dialog.modelOrigin == NBODialog.ORIGIN_NBO_ARCHIVE)
      clearInputFile(true);
    setInput(inputFile.getParent(), jobStem, useExt);
    if (!NBOUtil.getExt(inputFile).equals("47"))
      return;
    if (NBOUtil.fixPath(inputFile.getParent().toString()).equals(
        dialog.nboService.getServerPath(null))) {
      JOptionPane.showMessageDialog(this,
          "Select a directory that does not contain the NBOServe executable,"
              + "\nor select a new location for your NBOServe executable");
      return;
    }
    fullFilePath = inputFile.getParent();
    boolean canLoad = true;
    boolean isOK = true;
    String msg = "";
    if (dialog.dialogMode != NBODialog.DIALOG_MODEL && dialog.dialogMode != NBODialog.DIALOG_RUN) {
      for (String nn : EXT_ARRAY) {
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
   * @param doAll read the whole thing; else just for keywords (stops at $COORD) 
   * 
   * @return [ pre-keyword params, no-file keywords, post-keyword params, all keywords ]
   */
  protected String[] read47File(boolean doAll) {
    String[] fileData = new String[] { "", "", "", "" };
    String nboKeywords = "";
    SB data = new SB();
    if (!NBOUtil.read47FileBuffered(inputFile, data, doAll))
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
        if (prePost[0].length() > 0)
          params.append(s).append(sep);
        nboKeywords = PT.trim(prePost[1], "\t\r\n ");
        params = postParams;
        if (!doAll)
          break;
        continue;
      }
      params.append(s).append(sep).append("$END").append(sep);
    }
    dialog.logInfo("$NBO: " + nboKeywords, Logger.LEVEL_INFO);
    fileData[0] = fix47File(preParams.toString());
    fileData[1] = NBOUtil.removeNBOFileKeyword(nboKeywords, null);
    fileData[2] = postParams.toString();
    fileData[3] = nboKeywords;
    return fileData;
  }

  /**
   * I cannot remember why this is important -- BH
   * 
   * @param data
   * @return fixed data
   */
  public static String fix47File(String data) {
    return PT.rep(data, "FORMAT=PRECISE", "");     
  }

  public void clear() {
    tfName.setText("");
    tfExt.setText("");
  }

  protected void clearInputFile(boolean andUserDir) {
    if (jobStem.length() == 0)
      return;
    for (String ext : EXT_ARRAY)
      try {
        new File(dialog.nboService.getServerPath(jobStem + "." + ext)).delete();
      } catch (Exception e) {
        // ignore
      }
    if (andUserDir)
      for (String ext : EXT_ARRAY)
        try {
          NBOUtil.newNBOFile(inputFile, ext).delete();
        } catch (Exception e) {
          // ignore
        }
    inputFile = null;
    if (dialog.dialogMode == NBODialog.DIALOG_VIEW)
      dialog.viewPanel.resetView();
  }

  protected void setInput(String dir, String name, String ext) {
    if (dir != null)
      tfDir.setText(dir);
    if (name != null)
      tfName.setText(name);
    if (tfExt != null)
      tfExt.setText(ext);
    if (dir != null && name != null && ext != null) {
      dialog.modelPanel.modelSetSaveParametersFromInput(this, dir, name, ext);
      inputFile = new File(dir + "\\" + name + "." + ext);
    }
  }

  protected String getFileData(String fileName) {
    return dialog.vwr.getAsciiFileOrNull(fileName);
  }

  boolean writeToFile(String fileName, String s) {
    String ret = (s == null ? null : dialog.vwr.writeTextFile(fileName, s));
    return (ret != null && ret.startsWith("OK"));
  }

  public void setBrowseEnabled(boolean b) {
    btnBrowse.setEnabled(b);
  }

  public String getInputFileData(String nn) {
    return getFileData(newNBOFile(nn).toString());
  }

  public File newNBOFile(String nn) {
    return NBOUtil.newNBOFile(inputFile, nn);
  }

  /**
   * Fix a file name to be [A-Z0-9_]+
   * 
   * @param name the name to fix, or if empty, the input file name truncated to a maximum of 10 digits
   * @return fixed name replacing not-allowed characters with '_'
   */
  public String fixJobName(String name) {
    if (name == null || (name = name.trim()).length() == 0)
      name = jobStem;
    for (int i = name.length(); --i >= 0;) {
      char ch = name.charAt(i);
      if (!Character.isLetterOrDigit(ch) && ch != '_')
        name = name.substring(0, i) + "_" + name.substring(i + 1);
    }
    return name;
  }

  

  public String[] update47File(String jobName, String keywords, boolean isRun) {
    if (!useExt.equals("47"))
      return null;
    String fileName47 = inputFile.getAbsolutePath();
    String[] fileData = read47File(true);
    String oldData = (isRun ? getFileData(fileName47) : null);
    String newFileData = 
        fileData[0] 
        + "$NBO\n "
        + "FILE=" + jobName + " " + keywords 
        + "  $END" 
        + sep 
        + fileData[2];
    if (writeToFile(fileName47, newFileData)) {
      if (oldData != null)
        writeToFile(fileName47 + "$", oldData);
      fileData[1] = keywords;
      fileData[3] = "FILE=" + jobName + " " + keywords; 
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
  public boolean checkSwitch47To(String jobName) {
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
      String data = dialog.vwr.getAsciiFileOrNull(inputFile.getAbsolutePath());
      setJobStemAndTextFieldName(jobName);
      setInput(tfDir.getText(), jobName, "47");
      if (data != null)
        this.writeToFile(inputFile.getAbsolutePath(), data);    
    }
    return true;
  }

  /**
   * Delete all files ending with $
   */
  public void removeAllTemporaryRunFiles() {
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
  public void deleteFile(File f) {
    try {
      if (f.exists())
        f.delete();
    } catch (Exception e) {
      System.out.println("Could not delete " + f);
    }
  }

  /**
   * just get the keywords from the .47 file
   * 
   * @return NBO keywords
   */
  public String get47Keywords() {
    return read47File(false)[1];
  }

  /**
   * Checks to see that the .nbo file was created fully and, if not, deletes it,
   * moving its contents to .err$, and deletes all plot files
   * 
   * @param saveErr  write the current .nbo as .err$ file  
   * 
   * @return true if .nbo file is OK
   */
  public boolean checkNBOComplete(boolean saveErr) {
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
  
}
