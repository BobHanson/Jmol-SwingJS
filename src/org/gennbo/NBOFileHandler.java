package org.gennbo;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;

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
  protected final static int MODE_VIEW_VIDEO=6;

  
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
    boolean canEditTextFields = (mode == MODE_MODEL_SAVE || mode == MODE_MODEL_USE || mode==MODE_VIEW_VIDEO);

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
    if (canEditTextFields) {
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

  protected boolean doFileBrowsePressed() {
    if (dialog.nboService.isWorking()
        && dialog.statusLab.getText().startsWith("Running")) {
      int i = JOptionPane.showConfirmDialog(dialog,
          "Warning, changing jobs while running GenNBO can effect output files."
              + "\nContinue anyway?");
      if (i == JOptionPane.NO_OPTION)
        return false;
    }
    JFileChooser myChooser = new JFileChooser();
    myChooser.setFileFilter(new FileNameExtensionFilter(useExt, useExt));
    myChooser.setFileHidingEnabled(true);
    String folder = tfDir.getText();
    String name = tfName.getText();
    if (folder.length() > 0)
      fullFilePath = NBOUtil.getWindowsFullNameFor(folder, name.length() == 0 ? " " : name + (useExt.equals("47") ? ".47" : ""), null);
    myChooser.setSelectedFile(new File(fullFilePath));
    int button = myChooser.showDialog(this, GT.$("Select"));
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
    
    //fzy: Professor Frank initially wanted this feature to be added in, but wanted it abandoned later on
    //because it can't work on large files, which is a mystery (I'm not given any such large files to debug on error)
//    dialog.convertUnix2Dos(inputFile.getParent(), NBOUtil.getJobStem(inputFile), NBOUtil.getExt(inputFile));
    
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
    if (dialog.dialogMode != NBODialog.DIALOG_MODEL) {
      for (String x : EXT_ARRAY) {
        File f3 = newNBOFileForExt(x);
        if (!f3.exists() || x.equals("36") && f3.length() == 0) {
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
   * @return [ pre-keyword params, keywords, post-keyword params ]
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
        if(prePost.length>=1)
        {
          if (prePost[0].length() > 0)
          {
            params.append(s).append(sep);
          }
        }
        if(prePost.length>=2)
        {
          nboKeywords = PT.trim(prePost[1], "\t\r\n ");
        }
        params = postParams;
        if (!doAll)
          break;
        continue;
      }
      params.append(s).append(sep).append("$END").append(sep);
    }
    fileData[0] = NBOUtil.fix47File(preParams.toString());
    fileData[1] = NBOUtil.removeNBOFileKeyword(nboKeywords, null);
    fileData[2] = postParams.toString();
    fileData[3] = nboKeywords;
    return fileData;
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
    String ret = dialog.vwr.writeTextFile(fileName, s);
    return (ret != null && ret.startsWith("OK"));
  }

  public void setBrowseEnabled(boolean b) {
    btnBrowse.setEnabled(b);
  }

  public String getInputFile(String name) {
    return getFileData(newNBOFileForExt(name).toString());
  }

  public File newNBOFileForExt(String filenum) {
    return NBOUtil.newNBOFile(inputFile, filenum);
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


  /**
   * update47File will only be called by doRunSaveButton() and doRunGenNBOJob() in NBORun.java
   * If this method is being first called on a given .47 file, we will make a copy of the .47 file,
   * called <filename>.copy. At the end of the module (when we want to switch to another module), we will
   * copy our <filename>.copy to <filename>.47.
   * 
   * @param jobName
   * @param keywords
   * @param isRun
   * @return [
   */
  
  public String[] update47File(String jobName, String keywords, boolean isRun) {
    if (!useExt.equals("47"))
      return null;
    
    String fileName47 = inputFile.getAbsolutePath();
    
    File copyFile47=new File(fileName47+"$");
    FileInputStream originalFileReader=null;
    FileOutputStream copyFileWriter=null;
    if(isRun && !dialog.backupFileExists(jobName))
    {
      try
      {
        if(!copyFile47.exists())
        {
          copyFile47.createNewFile();
          copyFileWriter=new FileOutputStream(copyFile47);
        }
        else
        {
          copyFileWriter=new FileOutputStream(copyFile47,false);
        }
        File originalFile47=new File(fileName47);
        originalFileReader=new FileInputStream(originalFile47);
        //just a threshold to ensure that the original file 47 that we are going to copy isn't corrupted at the first place
//        if(originalFileReader.getChannel().size()>50000)
        copyFileWriter.getChannel().transferFrom(originalFileReader.getChannel(),0,originalFileReader.getChannel().size());
        File47AndFileCopy pair=new File47AndFileCopy(jobName,fileName47,fileName47+"$");
        dialog.insertNewFileCopy(pair);
      }
      catch(IOException ex)
      {
        dialog.logInfo("Could not create copy for file 47. Update to file 47 aborted.", Logger.LEVEL_ERROR);
        return null;
      }
      finally
      {
        try
        {
          if(originalFileReader!=null)
            originalFileReader.close();
          if(copyFileWriter!=null)
            copyFileWriter.close();
        }
        catch(IOException ex)
        {
          dialog.logInfo("Could not close file " + fileName47+"$. Update to file 47 aborted.", Logger.LEVEL_ERROR);
          return null;
        }
      }
    }
    
    String[] fileData = read47File(true);
//    String oldData = (isRun ? getFileData(fileName47) : null);
        
    if (writeToFile(inputFile.getAbsolutePath(), fileData[0] + "$NBO\n "
        + "FILE=" + jobName + " " + keywords + "  $END" + sep + fileData[2])) {
//      if (oldData != null)
//        writeToFile(fileName47 + "$", oldData);
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

}

class File47AndFileCopy
{
  private String jobName;
  private String file47;
  private String filecopy;
  
  File47AndFileCopy(String jobName,String file47,String fileCopy)
  {
    this.jobName=jobName;
    this.file47=file47;
    this.filecopy=fileCopy;
  }
  
  String getJobname()
  {
    return this.jobName;
  }
  
  String getFile47()
  {
    return this.file47;
  }
  
  String getFilecopy()
  {
    return this.filecopy;
  }
}
