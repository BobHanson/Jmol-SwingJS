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
package org.gennbo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

import javajs.util.JSJSONParser;
import javajs.util.PT;
import javajs.util.SB;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

class NBORun {

  protected NBODialog dialog;
  private Viewer vwr;

  protected NBORun(NBODialog dialog) {
    this.dialog = dialog;
    this.vwr = dialog.vwr;
  }

  protected static boolean ALLOW_SELECT_ALL = false;

  protected static final String[] RUN_KEYWORD_DESC = {
      "Bonding character of canonical MO's", "Dipole moment analysis",
      "Natural bond-bond polarizability indices",
      "Natural bond critical point analysis",
      "Natural coulomb electrostatics analysis",
      "Natural cluster unit analysis", "Natural poly-electron population analysis",
      "Natural resonance theory analysis",
      "Write files for orbital plotting", "Natural steric analysis" };

  protected static final String[] RUN_KEYWORD_NAMES = { "CMO", "DIPOLE", "NBBP",
      "NBCP", "NCE", "NCU", "NPEPA", "NRT", "PLOT", "STERIC" };

  protected Box editBox;
  protected JRadioButton rbLocal;
  protected JRadioButton[] keywordButtons;
  protected JButton btnRun;
  protected JTextField tfJobName;
  private JPanel myPanel;
  private Box titleBox;

  //protected String file47Keywords;

  protected JPanel buildRunPanel() {
    JPanel panel = myPanel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    dialog.getNewInputFileHandler(NBOFileHandler.MODE_RUN);
    dialog.inputFileHandler.setBrowseEnabled(false);

    panel.add(NBOUtil.createTitleBox(" Select Job ", dialog.new HelpBtn(
        "run_job_help.htm")));
    Box inputBox = NBOUtil.createBorderBox(true);
    inputBox.add(createSourceBox());
    inputBox.add(dialog.inputFileHandler);
    inputBox.setMinimumSize(new Dimension(360, 80));
    inputBox.setPreferredSize(new Dimension(360, 80));
    inputBox.setMaximumSize(new Dimension(360, 80));
    panel.add(inputBox);

    //EDIT////////////////
    panel.add(
        titleBox = NBOUtil.createTitleBox(" Choose $NBO Keywords ", dialog.new HelpBtn(
            "run_keywords_help.htm"))).setVisible(false);
    editBox = NBOUtil.createBorderBox(true);
    editBox.setSize(new Dimension(350, 400));
    tfJobName = new JTextField();
    tfJobName.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doLogJobName(null);
      }
    });
    tfJobName.addFocusListener(new FocusListener() {

      @Override
      public void focusGained(FocusEvent e) {
      }

      @Override
      public void focusLost(FocusEvent e) {
        doLogJobName(null);
      }
    });

    editBox.setVisible(false);
    panel.add(editBox);
    //BOTTOM OPTIONS///////////////

    //    Box box = Box.createHorizontalBox();
    //    box.setSize(new Dimension(250, 50));
    //    box.setAlignmentX(0.5f);
    btnRun = new JButton("Run");
    btnRun.setFont(NBOConfig.runButtonFont);
    btnRun.setVisible(false);
    btnRun.setEnabled(true);
    btnRun.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doRunGenNBOJob("");
      }
    });

    //box.add(btnRun);
    panel.add(btnRun);

    if (dialog.inputFileHandler.tfExt.getText().equals("47"))
      notifyFileLoaded();
    dialog.inputFileHandler.setBrowseEnabled(true);
    hideEditBox();
    return panel;
  }

  /**
   * set up the local/archive/webmo box
   * 
   * @return Box
   */
  private Box createSourceBox() {
    Box box = Box.createHorizontalBox();
    ButtonGroup bg = new ButtonGroup();
    rbLocal = new JRadioButton("Local");
    rbLocal.setSelected(true);
    rbLocal.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doLocalBtn();
      }
    });
    box.add(rbLocal);
    bg.add(rbLocal);
    JRadioButton btn = new JRadioButton("NBOrXiv");
    btn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doArchiveButton();
      }
    });
    box.add(btn);
    bg.add(btn);
    //WebMO is being removed in May 2018 on the request of Professor Frank
//    btn = new JRadioButton("WebMO");
//    btn.addActionListener(new ActionListener() {
//      @Override
//      public void actionPerformed(ActionEvent e) {
//        String url = "http://www.webmo.net/demoserver/cgi-bin/webmo/jobmgr.cgi";
//        try {
//          Desktop.getDesktop().browse(new URI(url));
//        } catch (Exception e1) {
//          dialog.alertError("Could not open WebMO");
//        }
//      }
//    });
//    box.add(btn);
//    bg.add(btn);
    return box;
  }

  protected void doArchiveButton() {
    ArchiveViewer aView = new ArchiveViewer(dialog, NBOConfig.ARCHIVE_DIR);
    aView.setVisible(true);
  }

  protected void doLocalBtn() {
    dialog.inputFileHandler.setBrowseEnabled(true);
    if (dialog.modelOrigin != NBODialog.ORIGIN_NBO_ARCHIVE)
      dialog.inputFileHandler.doFileBrowsePressed();
  }

  protected void addNBOKeylist() {
    if (dialog.inputFileHandler.inputFile == null)
      return;
    Box jobNameOuterBox = Box.createVerticalBox();
    jobNameOuterBox.setSize(new Dimension(250, 75));

    Box selectBox = Box.createHorizontalBox();
    selectBox.setSize(new Dimension(250, 50));

    Box mainBox = Box.createVerticalBox();
    mainBox.setSize(new Dimension(250, 275));

    final JPanel mainMenuOptions = addMenuOption();
    mainBox.add(mainMenuOptions);

    final JPanel mainTextEditor = addTextOption();
    mainBox.add(mainTextEditor);

    editBox.removeAll();
    editBox.add(Box.createRigidArea(new Dimension(350, 0)));
    editBox.add(jobNameOuterBox);
    editBox.add(selectBox);
    editBox.add(mainBox);

    Box jobNameInnerBox = Box.createHorizontalBox();
    jobNameInnerBox.add(new JLabel("Jobname ")).setFont(NBOConfig.nboFont);
    jobNameInnerBox.add(tfJobName).setMaximumSize(new Dimension(150, 30));
    jobNameInnerBox.setAlignmentX(0.5f);
    jobNameOuterBox.add(jobNameInnerBox);
    JLabel lab = new JLabel("(Plot files will be created with this name)");
    lab.setAlignmentX(0.5f);
    jobNameOuterBox.add(lab);

    ButtonGroup bg = new ButtonGroup();
    JRadioButton btnMenuSelect = new JRadioButton("Menu Select");
    bg.add(btnMenuSelect);
    btnMenuSelect.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        mainTextEditor.setVisible(false);
        mainMenuOptions.setVisible(true);
      }
    });

    JRadioButton btnTextEditor = new JRadioButton("Text Editor");
    btnTextEditor.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        mainMenuOptions.setVisible(false);
        doSetKeywordTextPane(getKeywordsFromButtons());
        mainTextEditor.setVisible(true);
      }
    });
    bg.add(btnTextEditor);

    selectBox.add(new JLabel("Keywords:  ")).setFont(NBOConfig.nboFont);
    selectBox.add(btnMenuSelect);
    selectBox.add(btnTextEditor);

    mainTextEditor.setVisible(false);
    mainMenuOptions.setVisible(true);
    btnMenuSelect.doClick();
  }

  private JPanel addTextOption() {
    JPanel textPanel = new JPanel(new BorderLayout());
    textPanel.setPreferredSize(new Dimension(270, 240));
    textPanel.setMaximumSize(new Dimension(270, 240));
    textPanel.setAlignmentX(0.5f);

    keywordTextPane = new JTextPane();
    doSetKeywordTextPane(cleanNBOKeylist(
        dialog.inputFileHandler.read47File(false)[1], true));

    JScrollPane sp = new JScrollPane();
    sp.getViewport().add(keywordTextPane);
    textPanel.add(sp, BorderLayout.CENTER);

    //    keywordTextPane.setCaretPosition(7);
    JButton saveBtn = new JButton("Save Changes");
    saveBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doRunSaveButton();
      }
    });
    textPanel.add(saveBtn, BorderLayout.SOUTH);
    textPanel.setVisible(false);
    return textPanel;
  }

  protected void doRunSaveButton() {
    String s = keywordTextPane.getText();
    String keywords = "";
    String[] tokens = PT.getTokens(PT.rep(PT.rep(s, "$NBO", ""), "$END", "")
        .trim());
    for (int i = 0; i < tokens.length; i++) {
      String x = tokens[i];
      String xuc = x.toUpperCase();
      if (xuc.indexOf("FILE=") < 0) {
        keywords += xuc + " ";
      } else {
        String jobName = x.substring(x.indexOf("=") + 1);
        if (!dialog.inputFileHandler.checkSwitch47To(jobName))
          return;
        tfJobName.setText(jobName);
      }
    }
    String name = tfJobName.getText().trim();
    name = dialog.inputFileHandler.fixJobName(name);
    dialog.inputFileHandler.update47File(name, keywords);
    addNBOKeylist();
    tfJobName.setText(name);
    editBox.repaint();
    editBox.revalidate();
  }

  private JPanel addMenuOption() {
    JPanel menuPanel = new JPanel();
    //width changed by fzy from 270 to 320, height changed by fzy from 240 to 280 
    //to allocate space for new radiobutton NPEPA
    menuPanel.setPreferredSize(new Dimension(320, 280));
    menuPanel.setMaximumSize(new Dimension(320, 280));
    menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
    menuPanel.setBorder(BorderFactory.createLoweredBevelBorder());
    keywordButtons = new JRadioButton[RUN_KEYWORD_NAMES.length];
    String keywords = " " + cleanNBOKeylist(
        dialog.inputFileHandler.read47File(false)[1], false) + " ";
    for (int i = 0; i < keywordButtons.length; i++) {
      keywordButtons[i] = new JRadioButton(RUN_KEYWORD_NAMES[i] + ": " + RUN_KEYWORD_DESC[i]);
      if (NBOUtil.findKeyword(keywords, RUN_KEYWORD_NAMES[i], true) >= 0)
        keywordButtons[i].setSelected(true);
      keywordButtons[i].setAlignmentX(0.0f);
      menuPanel.add(keywordButtons[i]);
      keywordButtons[i].addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          doLogKeywords(null);
        }

      });
    }
    JLabel lab2 = new JLabel("(Select one or more)");
    menuPanel.add(lab2);
    menuPanel.setAlignmentX(0.5f);
    menuPanel.setVisible(true);
    return menuPanel;
  }

  /**
   * Clean parameters and remove all FILE=xxxx
   * 
   * @param params
   * @param setJobNameTextField
   * @return cleaned string, just CAPS, no FILE=xxxx
   */
  protected String cleanNBOKeylist(String params, boolean setJobNameTextField) {
    setJobNameTextField &= (tfJobName != null);
    String[] fname = new String[1];
    params = NBOUtil.removeNBOFileKeyword(params, fname);
    String[] tokens = PT.getTokens(PT.clean(params));
    String tmp = " ";
    for (int i = 0; i < tokens.length; i++)
      tmp = NBOUtil.addNBOKeyword(tmp, tokens[i]);
    if (fname[0] != null && setJobNameTextField)
      tfJobName.setText(fname[0]);
    if (setJobNameTextField && (tfJobName.getText().equals("") || fname[0] == null))
      tfJobName.setText(dialog.inputFileHandler.jobStem);
    return tmp.trim();
  }

  protected void doLogJobName(String name) {
    if (name == null)
      tfJobName.setText(name = tfJobName.getText().trim());
    dialog.logValue("Job: " + name);
  }

  protected void doLogKeywords(String keywords) {
    if (keywords == null)
      keywords = getKeywordsFromButtons();
    dialog.logValue("Keywords: " + keywords);
  }

  JTextPane keywordTextPane;

  protected void doSetKeywordTextPane(String keywords) {
    keywordTextPane.setText(keywords);
  }

  //  protected void removeListParams(List<String> list,
  //                                  DefaultListModel<String> listModel) {
  //    log("Keyword(s) removed:", 'p');
  //    for (String x : list) {
  //      listModel.removeElement(x);
  //      if (file47Keywords.toUpperCase().contains(x.toUpperCase())) {
  //        file47Keywords = file47Keywords.substring(0,
  //            file47Keywords.indexOf(x.toUpperCase()))
  //            + file47Keywords.substring(file47Keywords.indexOf(x.toUpperCase())
  //                + x.length());
  //        log("  " + x, 'i');
  //      }
  //    }
  //  }

  protected void notifyFileLoaded() {
    if (vwr.ms.ac == 0)
    {
      hideEditBox();
      return;
    }
    dialog.doSetStructure("alpha");
    addNBOKeylist();
    for (Component c : myPanel.getComponents())
      c.setVisible(true);
    editBox.getParent().setVisible(true);
    titleBox.setVisible(true);
    editBox.setVisible(true);
    btnRun.setVisible(true);
    doLogKeywords(null);
    dialog.repaint();
    dialog.revalidate();

  }
  
  protected void hideEditBox() {
    titleBox.setVisible(false);
    editBox.setVisible(false);
    btnRun.setVisible(false);
  }
  //  protected void showConfirmationDialog(String st, File newFile, String ext) {
  //    int i = JOptionPane.showConfirmDialog(dialog, st, "Message",
  //        JOptionPane.YES_NO_OPTION);
  //    if (i == JOptionPane.YES_OPTION) {
  //      JDialog d = new JDialog(dialog);
  //      d.setLayout(new BorderLayout());
  //      JTextPane tp = new JTextPane();
  //      d.add(tp, BorderLayout.CENTER);
  //      d.setSize(new Dimension(500, 600));
  //      tp.setText(dialog.inputFileHandler.getFileData(NBOFileHandler.newNBOFile(
  //          newFile, "nbo").toString()));
  //      d.setVisible(true);
  //    }
  //  }
  //
  class ArchiveViewer extends JDialog implements ActionListener {
    private JScrollPane archivePanel;
    private JButton selectAll, download;
    private JCheckBox[] jcLinks;
    private JTextField tfPath;
    private String baseDir;

    public ArchiveViewer(NBODialog d, String url) {
      super(d, "NBO Archive Files");
      GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment()
          .getDefaultScreenDevice();
      int width = gd.getDisplayMode().getWidth() / 2 - 250;
      int height = gd.getDisplayMode().getHeight() / 2 - 120;
      setLocation(width, height);
      setSize(new Dimension(500, 240));
      setLayout(new BorderLayout());
      setResizable(false);
      archivePanel = new JScrollPane();

      archivePanel.setBorder(BorderFactory.createLineBorder(Color.black));
      add(archivePanel, BorderLayout.CENTER);
      String[] links = getLinks(url);
      setLinks(links, null);

      Box bottom = Box.createHorizontalBox();
      tfPath = new JTextField(d.inputFileHandler.fullFilePath);
      bottom.add(new JLabel("  Download to: "));
      bottom.add(tfPath);
      if (ALLOW_SELECT_ALL) {
        selectAll = new JButton("Select All");
        selectAll.addActionListener(this);
        bottom.add(selectAll);
      }
      download = new JButton("Download");
      download.addActionListener(this);
      bottom.add(download);
      add(bottom, BorderLayout.SOUTH);
      
      archivePanel.getHorizontalScrollBar().setUnitIncrement(25);
    }

    /**
     * look for a file "47files" in the directory, which should be a simple list
     * of files.
     * 
     * @author Bob Hanson
     * @param baseDir
     * @return array of fully-elaborated file names
     */
    @SuppressWarnings("unchecked")
    private String[] getLinks(String baseDir) {
      if (!baseDir.endsWith("/"))
        baseDir += "/";
      this.baseDir = baseDir;
      String fileList = dialog.inputFileHandler.getFileData(baseDir
          + "47files.txt");
      String html;
      String sep;
      if (fileList == null) {
        // presumes a raw directory listing from Apache
        html = dialog.inputFileHandler.getFileData(baseDir);
        sep = "<a";
      } else if (fileList.indexOf("{") == 0 || fileList.indexOf("[") == 0) {
        Map<String, Object> map = new JSJSONParser().parseMap(fileList, true);
        ArrayList<String> list = (map == null ? null : (ArrayList<String>) map
            .get("47files"));
        if (list == null || list.size() == 0)
          return new String[0];
        String ext = (list.get(0).indexOf(".47") >= 0 ? "" : ".47");
        String[] a = list.toArray(new String[0]);
        for (int i = 0; i < list.size(); i++)
          a[i] = baseDir + list.get(i) + ext;
        return a;
      } else {
        html = PT.rep(fileList, "\r", "");
        sep = "\n";
      }
      ArrayList<String> files = new ArrayList<String>();
      String[] toks = html.split(sep);
      for (int i = 1; i < toks.length; i++) {
        String file = PT.getQuotedAttribute(toks[i], "href");
        if (file != null && file.endsWith(".47"))
          files.add(file);
      }
      return files.toArray(new String[0]);
    }

    private void setLinks(String[] links, String startsWith) {
      jcLinks = new JCheckBox[links.length];
      JPanel filePanel = new JPanel(new FlowLayout());
      if (startsWith == null)
        startsWith = "";
      ButtonGroup bg = (ALLOW_SELECT_ALL ? null : new ButtonGroup());
      for (int i = 0; i < links.length; i += 6) {
        Box box = Box.createVerticalBox();
        for (int j = 0; j < 6; j++) {
          if (i + j >= jcLinks.length)
            break;
          jcLinks[i + j] = new JCheckBox(links[i + j]);
          if (bg != null)
            bg.add(jcLinks[i + j]);
          jcLinks[i + j].setBackground(Color.white);
          box.add(jcLinks[i + j]);
        }
        filePanel.add(box);
      }
      filePanel.setBackground(Color.white);
      archivePanel.getViewport().add(filePanel);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (e.getSource() == selectAll) {
        boolean didCheck = false;
        for (int i = 0; i < jcLinks.length; i++) {
          if (!jcLinks[i].isSelected()) {
            didCheck = true;
            jcLinks[i].setSelected(true);
          }
        }
        if (!didCheck) {
          for (int i = 0; i < jcLinks.length; i++) {
            jcLinks[i].setSelected(false);
          }
        }
        repaint();
      } else if (e.getSource() == download) {
        String s = null;
        for (int i = 0; i < jcLinks.length; i++) {
          if (!jcLinks[i].isSelected())
            continue;
          s = baseDir + jcLinks[i].getText();
          break;
        }
        if (s == null)
          return;
        setVisible(false);
        if (!retrieveFile(s, tfPath.getText())) {
          setVisible(true);
          return;
        }
      }
    }

  }

  /**
   * Merge buttons with full list of file keywords.
   * 
   * @return new list, including trailing space character.
   */
  protected String getKeywordsFromButtons() {
    String keywords = " "
        + cleanNBOKeylist(dialog.inputFileHandler.read47File(false)[1], false)
        + " ";
    if (keywordButtons == null)
      return keywords;
    for (int i = 0; i < keywordButtons.length; i++) {
      String name = RUN_KEYWORD_NAMES[i];
      if (keywordButtons[i].isSelected())
        keywords = NBOUtil.addNBOKeyword(keywords, name);
      else
        keywords = NBOUtil.removeNBOKeyword(keywords, name);
    }
    return keywords;
  }

  /**
   * get a resource and put it in the specified path
   * 
   * @param s
   * @param path
   * @return true if successful
   */
  public boolean retrieveFile(String s, String path) {
    File f = null;
    if (path == null)
      path = dialog.inputFileHandler.fullFilePath;
    dialog.logCmd("retrieve " + s);

    String name = s.substring(s.lastIndexOf("/") + 1);
    if (path.endsWith("/") || path.endsWith("\\"))
      path += name;
    else
      path += (path.endsWith("\\") ? "\\" : "/") + name;
    f = new File(path);
    if (f.exists()) {
      int j = JOptionPane
          .showConfirmDialog(
              null,
              "File "
                  + f.getAbsolutePath()
                  + " already exists, do you want to overwrite contents, along with its associated .nn and .nbo files?",
              "Warning", JOptionPane.YES_NO_OPTION);
      if (j == JOptionPane.NO_OPTION)
        return false;
    }
    try {
      String fileData = vwr.getAsciiFileOrNull(s);
      if (fileData == null) {
        dialog.logError("Error reading " + s);
        return false;
      }
      if (dialog.inputFileHandler.writeToFile(path, fileData)) {
        dialog.logInfo(f.getName() + " (" + fileData.length() + " bytes)",
            Logger.LEVEL_INFO);
      } else {
        dialog.logError("Error writing to " + f);
      }
    } catch (Throwable e) {
      dialog.alertError("Error reading " + s + ": " + e);
    }
    dialog.modelOrigin = NBODialog.ORIGIN_NBO_ARCHIVE;
    dialog.inputFileHandler.setInputFile(f);
    dialog.modelOrigin = NBODialog.ORIGIN_NBO_ARCHIVE;
    rbLocal.doClick();
    dialog.modelOrigin = NBODialog.ORIGIN_FILE_INPUT;
    return true;
  }

  /**
   * Initiates a gennbo job via NBOServe; called from RUN, VIEW, and SEARCH
   * 
   * Note that there are issues with this method.
   * 
   * @param requiredKeyword
   */
  protected void doRunGenNBOJob(String requiredKeyword) {

    if (dialog.jmolOptionNONBO) {
      dialog.alertRequiresNBOServe();
      return;
    }

    // get the current file47Data and nboKeywords

    String newKeywords = getKeywordsFromButtons();

    //Check the plot file names match job name, warn user otherwise
    dialog.inputFileHandler.jobStem = dialog.inputFileHandler.jobStem.trim();

    // check to see if there is a new job name. 
    if (requiredKeyword.length() > 0 && tfJobName != null) {
      // from another module
      tfJobName.setText(dialog.inputFileHandler.jobStem);
    }
    String jobName = dialog.inputFileHandler.fixJobName(tfJobName == null ? null : tfJobName.getText().trim());

    // BH Q: Would it be reasonable if the NO option is chosen to put that other job name in to the jobStem field, and also copy the .47 file to that? Or use that?
    // Or, would it be better to ask this question immediately upon file loading so that it doesn't come up, and make it so that
    // you always MUST have these two the same?

    if (!dialog.inputFileHandler.checkSwitch47To(jobName))
      return;
    String[] tokens = PT.getTokens(requiredKeyword);
    // trick here is that you CANNOT TAKE OUT A TOKEN because it might have a keyword.
    for (int i = 0; i < tokens.length; i++) {
      String x = tokens[i];
      newKeywords = NBOUtil.addNBOKeyword(newKeywords, x);
    }
    newKeywords = NBOUtil.addNBOKeyword(newKeywords, "PLOT");

    jobName = (jobName.equals("") ? dialog.inputFileHandler.jobStem : jobName);

    String[] fileData = dialog.inputFileHandler.update47File(jobName,
        newKeywords);
    if (fileData == null)
      return;
    SB sb = new SB();
    NBOUtil.postAddGlobalC(sb, "PATH",
        dialog.inputFileHandler.inputFile.getParent());
    NBOUtil.postAddGlobalC(sb, "JOBSTEM", dialog.inputFileHandler.jobStem);
    NBOUtil.postAddGlobalC(sb, "ESS", "gennbo");
    NBOUtil.postAddGlobalC(sb, "LABEL_1", "FILE=" + jobName);

    dialog.logCmd("RUN GenNBO FILE=" + jobName + " "
        + cleanNBOKeylist(fileData[1], false));

    postNBO(sb, "Running GenNBO...");
  }

  /**
   * Post a request to NBOServe with a callback to processNBO.
   * 
   * @param sb
   *        command data
   * @param statusMessage
   */
  private void postNBO(SB sb, String statusMessage) {
    final NBORequest req = new NBORequest();
    req.set(new Runnable() {
      @Override
      public void run() {
        processNBO(req);
      }
    }, true, statusMessage, "r_cmd.txt", sb.toString());
    dialog.nboService.postToNBO(req);
  }

  /**
   * Process the reply from NBOServe for a RUN request
   * 
   * @param req
   */
  protected void processNBO(NBORequest req) {
    dialog.inputFileHandler.setInputFile(dialog.inputFileHandler.inputFile);
  }

}
