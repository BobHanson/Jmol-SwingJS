/* $RCSfile$
 * $Author jonathan gutow$
 * $Date Aug 5, 2007 9:19:06 AM $
 * $Revision$
 * Updated May 2016 by Angel Herraez
 * valid for JSmol
 *
 * Copyright (C) 2005-2016  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */
package org.openscience.jmol.app.webexport;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Checkbox;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.Enumeration;

import javajs.util.Lst;
import javajs.util.PT;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jmol.viewer.FileManager;
import org.jmol.viewer.Viewer;
import org.jmol.i18n.GT;
import javajs.util.BS;
import org.jmol.util.Logger;
import org.openscience.jmol.app.jmolpanel.GuiMap;
import org.openscience.jmol.app.jmolpanel.HelpDialog;

/*
 * an abstract class used as the basis for the tabbed panels in WebExport.
 * (PopInJmol and ScriptButtons)
 * 
 */
abstract class WebPanel extends JPanel implements ActionListener,
    ListSelectionListener, ItemListener {

  abstract String getAppletDefs(int i, String html, StringBuilder appletDefs,
                                JmolInstance instance);

  abstract String fixHtml(String html);

  abstract JPanel appletParamPanel(); // should be defined in the code for the
                                      // specific case e.g. ScriptButtons.java

  protected String panelName; // pop_in or script_btn

  // infoFile = "pop_in_instructions";
  // infoFileLocalized = "pop_in_instructions_" + lang + ".html";
  // templateName = "pop_in_template.html";
  // appletTemplateName = "pop_in_template2.html";

  // protected String templateName;
  // protected String infoFile;
  // protected String appletTemplateName;
  // protected String templateImage;

  protected String htmlAppletTemplate;
  protected String listLabel;
  protected String appletInfoDivs;

  protected JSpinner appletSizeSpinnerW;
  protected JSpinner appletSizeSpinnerH;
  protected JSpinner appletSizeSpinnerP;

  private JScrollPane editorScrollPane;
  private JButton saveButton, viewButton, helpButton, addInstanceButton;
  private JButton deleteInstanceButton, showInstanceButton;
  private JTextField remoteAppletPath, localAppletPath, pageAuthorName,
      webPageTitle;
  private JFileChooser fc;
  protected JList<JmolInstance> instanceList;
  protected Widgets theWidgets;
  protected int nWidgets;
  private Checkbox[] widgetCheckboxes;
  protected Viewer vwr;
  private int panelIndex;
  private WebPanel[] webPanels;
  private int errCount;

  private String htmlPath;

  protected WebPanel(Viewer vwr, JFileChooser fc, WebPanel[] webPanels,
      int panelIndex) {
    this.vwr = vwr;
    this.fc = fc;
    this.webPanels = webPanels;
    this.panelIndex = panelIndex;
    theWidgets = new Widgets();
    nWidgets = theWidgets.widgetList.length;
    widgetCheckboxes = new Checkbox[nWidgets];

    // Create the text fields for the path to the Jmol applet, page author(s)
    // name(s) and web page title.
    remoteAppletPath = new JTextField(20);
    remoteAppletPath.addActionListener(this);
    remoteAppletPath.setText(WebExport.getAppletPath(true));
    localAppletPath = new JTextField(20);
    localAppletPath.addActionListener(this);
    localAppletPath.setText(WebExport.getAppletPath(false));
    pageAuthorName = new JTextField(20);
    pageAuthorName.addActionListener(this);
    pageAuthorName.setText(WebExport.getPageAuthorName());
    webPageTitle = new JTextField(20);
    webPageTitle.addActionListener(this);
    webPageTitle.setText(GT.$("A web page with JSmol objects"));
  }

  // Need the panel maker and the action listener.

  JPanel getPanel(int infoWidth, int infoHeight) {

    // For layout purposes, put things in separate panels

    // Create the list and list view to handle the list of
    // Jmol Instances.
    instanceList = new JList<JmolInstance>(new DefaultListModel<JmolInstance>());
    instanceList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    instanceList.setTransferHandler(new ArrayListTransferHandler(this));
    instanceList.setCellRenderer(new InstanceCellRenderer());
    instanceList.setDragEnabled(true);
    instanceList.setPreferredSize(new Dimension(350, 200));
    instanceList.addListSelectionListener(this);

    JScrollPane instanceListView = new JScrollPane(instanceList);
    instanceListView.setPreferredSize(new Dimension(350, 200));
    JPanel instanceSet = new JPanel();
    instanceSet.setLayout(new BorderLayout());
    instanceSet.add(new JLabel(listLabel), BorderLayout.NORTH);
    instanceSet.add(instanceListView, BorderLayout.CENTER);
    instanceSet.add(new JLabel(GT.$("click and drag to reorder")),
        BorderLayout.SOUTH);

    // Create the Instance add button.
    addInstanceButton = new JButton(GT
        .$("Add present Jmol state as instance..."));
    addInstanceButton.addActionListener(this);

    JPanel buttonPanel = new JPanel();
    buttonPanel.setMaximumSize(new Dimension(350, 50));
//    showInstanceButton = new JButton(GT.$("Show selected"));
//    showInstanceButton.addActionListener(this);
    deleteInstanceButton = new JButton(GT.$("Delete selected"));
    deleteInstanceButton.addActionListener(this);
//    buttonPanel.add(showInstanceButton);
    buttonPanel.add(deleteInstanceButton);

    // width height or %width

    JPanel paramPanel = appletParamPanel();
    paramPanel.setMaximumSize(new Dimension(350, 70));

    // Instance selection
    JPanel instanceButtonPanel = new JPanel();
    instanceButtonPanel.add(addInstanceButton);
    instanceButtonPanel.setSize(300, 70);

    JPanel p = new JPanel();
    p.setLayout(new BorderLayout());
    p.add(instanceButtonPanel, BorderLayout.NORTH);
    p.add(buttonPanel, BorderLayout.SOUTH);

    JPanel instancePanel = new JPanel();
    instancePanel.setLayout(new BorderLayout());
    instancePanel.add(instanceSet, BorderLayout.CENTER);
    instancePanel.add(p, BorderLayout.SOUTH);

    JPanel rightPanel = new JPanel();
    rightPanel.setLayout(new BorderLayout());
    rightPanel.setMinimumSize(new Dimension(350, 350));
    rightPanel.setMaximumSize(new Dimension(350, 1000));
    rightPanel.add(paramPanel, BorderLayout.NORTH);
    rightPanel.add(instancePanel, BorderLayout.CENTER);
    rightPanel.setBorder(BorderFactory.createTitledBorder(GT
        .$("JSmol Instances:")));

    JPanel widgetPanel = new JPanel();
    widgetPanel.setMinimumSize(new Dimension(150,150));
    widgetPanel.setLayout(new BoxLayout(widgetPanel,BoxLayout.Y_AXIS));
    widgetPanel.setBorder(BorderFactory.createTitledBorder(GT.$("Select widgets:")));
    for (int i = 0; i<nWidgets;i++){
      widgetCheckboxes[i]=new Checkbox(theWidgets.widgetList[i].name);
      widgetCheckboxes[i].addItemListener(this);
      widgetPanel.add(widgetCheckboxes[i]);
    }
    // Create the overall panel
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());

    JPanel leftPanel = getLeftPanel(infoWidth, infoHeight);
    leftPanel.setMaximumSize(new Dimension(350, 1000));

    // Add everything to this panel.
    panel.add(leftPanel, BorderLayout.WEST);
    panel.add(rightPanel, BorderLayout.CENTER);
    panel.add(widgetPanel, BorderLayout.EAST);

    enableButtons(instanceList);
    return panel;
  }

  protected JList<JmolInstance> getInstanceList() {
    return instanceList;
  }

  
  /**
   * @param w UNUSED
   * @param h UNUSED 
   * @return   left panel
   * 
   */
  private JPanel getLeftPanel(int w, int h) {

    helpButton = new JButton(GT.$("Help/Instructions"));
    helpButton.addActionListener(this);

    String templateImage = panelName + ".png";
    URL pageCartoon = GuiMap.getResource(this, templateImage);
    ImageIcon pageImage = null;
    if (pageCartoon != null) {
      pageImage = new ImageIcon(pageCartoon, GT.$("Cartoon of Page"));
    } else {
      System.err.println("Error Loading Page Cartoon Image " + templateImage);
    }
    JLabel pageCartoonLabel = new JLabel(pageImage);
    JPanel pageCartoonPanel = new JPanel();
    pageCartoonPanel.setLayout(new BorderLayout());
    pageCartoonPanel.setBorder(BorderFactory.createTitledBorder(GT
        .$("Cartoon of Page")
        + ":"));
    pageCartoonPanel.add(pageCartoonLabel);
    // editorScrollPane = getInstructionPane(w, h);

    // Create the save button.
    saveButton = new JButton(GT.$("Save HTML as..."));
    saveButton.addActionListener(this);
    viewButton = new JButton(GT.$("View HTML"));
    viewButton.addActionListener(this);
    JPanel savePanel = new JPanel();
    savePanel.add(saveButton);
    savePanel.add(viewButton);

    // Path to applet panel

    JPanel pathPanel = new JPanel();
    pathPanel.setLayout(new BorderLayout());
    pathPanel.setBorder(BorderFactory.createTitledBorder(GT
        .$("Relative server path to JSmol.min.js:")));
    pathPanel.add(remoteAppletPath, BorderLayout.NORTH);

    JPanel pathPanel2 = new JPanel();
    pathPanel2.setLayout(new BorderLayout());
    pathPanel2.setBorder(BorderFactory.createTitledBorder(GT
        .$("Relative local path to JSmol.min.js:")));
    pathPanel2.add(localAppletPath, BorderLayout.NORTH);

    // Page Author Panel
    JPanel authorPanel = new JPanel();
    authorPanel.setBorder(BorderFactory.createTitledBorder(GT
        .$("Author (your name):")));
    authorPanel.add(pageAuthorName, BorderLayout.NORTH);

    // Page Title Panel
    JPanel titlePanel = new JPanel();
    titlePanel.setLayout(new BorderLayout());
    titlePanel.setBorder(BorderFactory.createTitledBorder(GT
        .$("Browser window title for this web page:")));
    titlePanel.add(webPageTitle, BorderLayout.NORTH);
    titlePanel.add(savePanel, BorderLayout.SOUTH);

    JPanel pathPanels = new JPanel();
    pathPanels.setLayout(new BorderLayout());
    pathPanels.add(pathPanel, BorderLayout.NORTH);
    pathPanels.add(pathPanel2, BorderLayout.SOUTH);
    JPanel settingsPanel = new JPanel();
    settingsPanel.setLayout(new BorderLayout());
    settingsPanel.add(pathPanels, BorderLayout.NORTH);
    settingsPanel.add(authorPanel, BorderLayout.CENTER);
    settingsPanel.add(titlePanel, BorderLayout.SOUTH);

    // Combine previous three panels into one
    JPanel leftpanel = new JPanel();
    leftpanel.setLayout(new BorderLayout());
    // leftpanel.add(editorScrollPane, BorderLayout.CENTER);
    leftpanel.add(helpButton, BorderLayout.NORTH);
    leftpanel.add(pageCartoonPanel, BorderLayout.CENTER);
    leftpanel.add(settingsPanel, BorderLayout.SOUTH);
    return leftpanel;
  }

  int getInfoWidth() {
    return editorScrollPane.getWidth();
  }

  int getInfoHeight() {
    return editorScrollPane.getHeight();
  }

  @Override
  public void itemStateChanged(ItemEvent e) {
    DefaultListModel<JmolInstance> listModel = (DefaultListModel<JmolInstance>) instanceList.getModel();
    int[] list = instanceList.getSelectedIndices();
    if (list.length == 0)
      return; // don't try to update things if there is nothing in the list...
    JmolInstance instance = listModel.get(list[0]);
    Object source = e.getSource();
    int stateChange = e.getStateChange();
    for (int i = 0; i < nWidgets; i++) {
      if (source == widgetCheckboxes[i]) {
        if (stateChange == ItemEvent.SELECTED) {
          instance.addWidget(i);
        }
        if (stateChange == ItemEvent.DESELECTED) {
          instance.deleteWidget(i);
        }
      }
    }
  }
  
  @Override
  public void actionPerformed(ActionEvent e) {

    if (e.getSource() == helpButton) {
      HelpDialog webExportHelp = new HelpDialog(WebExport.getFrame(), GuiMap
          .getHtmlResource(this, panelName + "_instructions"));
      webExportHelp.setVisible(true);
      webExportHelp.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      return;
    }

    if (e.getSource() == remoteAppletPath) {// apparently no events are fired to
                                            // reach this, maybe "enter" does it
      String path = remoteAppletPath.getText();
      WebExport.setAppletPath(path, true);
      return;
    }

    if (e.getSource() == localAppletPath) {// apparently no events are fired to
                                            // reach this, maybe "enter" does it
      String path = localAppletPath.getText();
      WebExport.setAppletPath(path, false);
      return;
    }

    // Handle open button action.
    if (e.getSource() == addInstanceButton) {
      // make dialog to get name for instance
      // create an instance with this name. Each instance is just a container
      // for a string with the Jmol state
      // which contains the full information on the file that is loaded and
      // manipulations done.
      String label = (instanceList.getSelectedIndices().length != 1 ? ""
          : getInstanceName(-1));
      String name = JOptionPane.showInputDialog(GT
          .$("Give the occurrence of JSmol a name:"), label);
      if (name == null || name.length() == 0)
        return;
      DefaultListModel<JmolInstance> listModel = (DefaultListModel<JmolInstance>) instanceList.getModel();
      int width = 300;
      int height = 300;
      if (appletSizeSpinnerH != null) {
        width = ((SpinnerNumberModel) (appletSizeSpinnerW.getModel()))
            .getNumber().intValue();
        height = ((SpinnerNumberModel) (appletSizeSpinnerH.getModel()))
            .getNumber().intValue();
      }
      JmolInstance instance = JmolInstance.getInstance(vwr, name, width,
          height, nWidgets);
      if (instance == null) {
        LogPanel.log(GT
            .$("Error creating new instance containing script(s) and image."));
        return;
      }

      int i;
      for (i = instanceList.getModel().getSize(); --i >= 0;)
        if (getInstanceName(i).equals(instance.name))
          break;
      String m = GT.$("added Instance {0}");
      if (i < 0) {
        i = listModel.getSize();
        listModel.addElement(instance);
        LogPanel.log(GT.o(m, instance.name));
      } else {
        listModel.setElementAt(instance, i);
        LogPanel.log(GT.o(m, instance.name));
      }
      instanceList.setSelectedIndex(i);
      syncLists();
      return;
    }

    if (e.getSource() == deleteInstanceButton) {
      DefaultListModel<JmolInstance> listModel = (DefaultListModel<JmolInstance>) instanceList.getModel();
      // find out which are selected and remove them.
      int[] todelete = instanceList.getSelectedIndices();
      for (int i = 0; i < todelete.length; i++) {
        JmolInstance instance = listModel.get(todelete[i]);
        try {
          instance.delete();
        } catch (IOException err) {
          LogPanel.log(err.getMessage());
        }
      }
      listModel.removeRange(todelete[0], todelete[todelete.length - 1]);
      syncLists();
      return;
    }

    if (e.getSource() == showInstanceButton) {
      DefaultListModel<JmolInstance> listModel = (DefaultListModel<JmolInstance>) instanceList.getModel();
      int[] list = instanceList.getSelectedIndices();
      if (list.length != 1)
        return;
      JmolInstance instance = listModel.get(list[0]);
      vwr.evalStringQuiet(")" + instance.script); // leading paren disabled
                                                      // history
      return;
    }

    if (e.getSource() == viewButton) {
      vwr.showUrl(htmlPath);
    } else if (e.getSource() == saveButton) {
      fc.setDialogTitle(GT
          .$("Select a folder to create or an HTML file to save"));
      int returnVal = fc.showSaveDialog(this);
      if (returnVal != JFileChooser.APPROVE_OPTION)
        return;
      File file = fc.getSelectedFile();
      htmlPath = "file:///" + file.getAbsolutePath().replace('\\', '/');
      String retVal = null;
      errCount = 0;
      try {
        String path = remoteAppletPath.getText();
        WebExport.setAppletPath(path, true);
        path = localAppletPath.getText();
        WebExport.setAppletPath(path, false);
        String authorName = pageAuthorName.getText();
        WebExport.setWebPageAuthor(authorName);
        retVal = fileWriter(file, instanceList);
        viewButton.setEnabled(true);
      } catch (IOException IOe) {
        LogPanel.log(IOe.getMessage());
        errCount+=1;
      }
      if (retVal != null) {
        LogPanel.log(GT.o(GT.$("file {0} created"), retVal));
      } else {
        LogPanel.log(GT.$("Call to FileWriter unsuccessful."));
        errCount+=1;
      }
      if (errCount > 0){
        LogPanel.log(GT.$("Errors occurred during web page creation.  See Log Tab!"));
      }
      return;
    } 
    
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    if (e.getValueIsAdjusting())
      return; // wait until done
    JList<?> whichList = (JList<?>) e.getSource();
    if (whichList.isSelectionEmpty())
      return;// nothing selected
    if (whichList.getMinSelectionIndex() != whichList.getMaxSelectionIndex())
      return;// multiple selection
    int index = whichList.getSelectedIndex();
    JmolInstance instance = (JmolInstance) whichList.getModel().getElementAt(
        index);
    // dimensions must be stored before changing the spinners as that triggers
    // a stateChanged event for the spinners and causes them to update the
    // instance
    // to the value from the previous selected instance.
    int width = instance.width;
    int height = instance.height;
    if (appletSizeSpinnerW != null)
      appletSizeSpinnerW.getModel().setValue(Integer.valueOf(width));
    if (appletSizeSpinnerH != null)
      appletSizeSpinnerH.getModel().setValue(Integer.valueOf(height));
    vwr.evalStringQuiet(")" + instance.script); //leading paren disabled history
    //Set the widget selections to match this instance
    for (int i = 0; i < nWidgets; i++)
      widgetCheckboxes[i].setState(instance.whichWidgets.get(i));
  }

  String getInstanceName(int i) {
    if (i < 0)
      i = instanceList.getSelectedIndex();
    JmolInstance instance = instanceList.getModel()
        .getElementAt(i);
    return (instance == null ? "" : instance.name);
  }

  String fileWriter(File file, JList<JmolInstance> InstanceList) throws IOException { // returns
                                                                          // true
                                                                          // if
                                                                          // successful.
    // JOptionPane.showMessageDialog(null, "Creating directory for data...");
    String datadirPath = file.getPath().replace('\\','/');
    String datadirName = file.getName();
    String fileName;
    if (datadirName.indexOf(".htm") < 0) {
      File f = new File(datadirPath + ".html"); 
      if (f.exists()) {
        datadirName += ".html";
        file = f;
      } else if ((f = new File(datadirPath + ".htm")).exists()) {
        datadirName += ".htm";
        file = f;
      }
    }
    if (datadirName.indexOf(".htm") > 0) {
      fileName = datadirName;
      datadirPath = file.getParent();
      file = new File(datadirPath);
      datadirName = file.getName();
    } else {
      fileName = datadirName + ".html";
    }
    datadirPath = datadirPath.replace('\\', '/');
    fileName = datadirPath + "/" + fileName;
    boolean made_datadir = (file.exists() && file.isDirectory() || file.mkdir());
    DefaultListModel<JmolInstance> listModel = (DefaultListModel<JmolInstance>) InstanceList.getModel();
    LogPanel.log("");
    if (made_datadir) {
      LogPanel.log(GT.o(GT.$("Using directory {0}"), datadirPath));
      LogPanel.log("  " + GT.o(GT.$("adding {0}"), "support.js"));
      try{
      vwr.writeTextFile(datadirPath + "/support.js", 
          GuiMap.getResourceString(this, "support.js"));
      }catch (IOException IOe){
        throw IOe;
      }
      for (int i = 0; i < listModel.getSize(); i++) {
        JmolInstance thisInstance = listModel.getElementAt(i);
        String javaname = thisInstance.javaname;
        String script = thisInstance.script;
        LogPanel.log("  ...jmolApplet" + i);
        LogPanel.log("      ..." + GT.o(GT.$("adding {0}"), javaname + ".png"));
        try {
          thisInstance.movepict(datadirPath);
        } catch (IOException IOe) {
          throw IOe;
        }
        Lst<String> filesToCopy = new Lst<String>();
        Lst<String> filesToCopyUTF = new Lst<String>();
        String localPath = localAppletPath.getText();
        if (localPath.equals(".") || remoteAppletPath.getText().equals(".")) {
          try{
            String jmolJarDirPath=jmolJarPath().substring(0, jmolJarPath().lastIndexOf("/"));
            filesToCopy.addLast(jmolJarDirPath+"/jsmol.zip");
            filesToCopyUTF.addLast(jmolJarDirPath+"/jsmol.zip");
          }catch (java.io.UnsupportedEncodingException e){
            LogPanel.log(GT.$("There was an error in the text encoding so the path to jsmol.zip is unknown."));
          }
        }
        FileManager.getFileReferences(script, filesToCopy, filesToCopyUTF);
        ArrayList<String> copiedFileNames = new  ArrayList<String>();
        int nFiles = filesToCopy.size();
        for (int iFile = 0; iFile < nFiles; iFile++) {
          String name = filesToCopyUTF.get(iFile);
          int pt = name.indexOf("::");
          String type = "";
          if (pt >= 0) {
            type = name.substring(0, pt + 2);
            name = name.substring(pt + 2);
          }
          String newName = copyBinaryFile(name, datadirPath);
          copiedFileNames.add(PT.escUnicode(type + newName.substring(newName.lastIndexOf('/') + 1)));
        }
        script = replaceQuotedStrings(script, filesToCopy, copiedFileNames);
        LogPanel.log("      ..." + GT.o(GT.$("adding {0}"), javaname + ".spt"));
        vwr.writeTextFile(datadirPath + "/" + javaname + ".spt", script);
      }
      String html = GuiMap.getResourceString(this, panelName + "_template");
      html = fixHtml(html);
      String jsStr = "";
      BS whichWidgets = allSelectedWidgets();
      for (int i = 0; i < nWidgets; i++) {
        if (whichWidgets.get(i)) {
          String scriptFileName = theWidgets.widgetList[i]
              .getJavaScriptFileName();
          if (!scriptFileName.equalsIgnoreCase("none")) {
            jsStr += "\n<script src=\"" + scriptFileName
                + "\" type=\"text/javascript\"></script>";
            LogPanel.log("  " + GT.o(GT.$("adding {0}"), scriptFileName));
            vwr.writeTextFile(datadirPath + "/" + scriptFileName + "",
                GuiMap.getResourceString(this, scriptFileName));
          }
          String [] supportFileNames=theWidgets.widgetList[i].getSupportFileNames();
          int nFiles = supportFileNames.length;
          if (nFiles!=0){
            for(int fileN=0;fileN<nFiles;fileN++){
              String inFile = supportFileNames[fileN];
              String outFile = inFile;
              if((inFile.lastIndexOf("/"))!=-1) {
                 outFile = inFile.substring((inFile.lastIndexOf("/")+1));
              }
              URL fileURL = GuiMap.getResource(this, inFile);
              if (fileURL==null){
                LogPanel.log("    "+GT.o(GT.$("Unable to load resource {0}"), inFile));
                errCount+=1;
              }else{
                InputStream is = fileURL.openConnection().getInputStream();
                FileOutputStream os = new FileOutputStream(datadirPath + "/"
                    + outFile);
                int temp = is.read();
                while (temp != -1) {
                  os.write(temp);
                  temp = is.read();
                }
                os.flush();
                os.close();
                LogPanel.log("  " + GT.o(GT.$("adding {0}"), outFile));
              }
            }         
          }
        }
      }
      html=PT.rep(html,"@WIDGETJSFILES@",jsStr);
      appletInfoDivs = "";
      StringBuilder appletDefs = new StringBuilder();
      htmlAppletTemplate = GuiMap.getResourceString(this, panelName
          + "_template2");
      for (int i = 0; i < listModel.getSize(); i++)
        html = getAppletDefs(i, html, appletDefs, listModel
            .getElementAt(i));
      html = PT.rep(html, "@AUTHOR@", GT
          .escapeHTML(pageAuthorName.getText()));
      html = PT.rep(html, "@TITLE@", GT
          .escapeHTML(webPageTitle.getText()));
      html = PT.rep(html, "@REMOTEAPPLETPATH@",
          remoteAppletPath.getText());
      String localPath=localAppletPath.getText();
      if (localPath.contentEquals(".")){
        localPath= "jsmol";
      }
      html = PT.rep(html, "@LOCALAPPLETPATH@",
          localPath);
      html = PT.rep(html, "@DATADIRNAME@", datadirName);
      if (appletInfoDivs.length() > 0)
        appletInfoDivs = "\n<div style='display:none'>\n" + appletInfoDivs
            + "\n</div>\n";
      String str = appletDefs.toString();
      html = PT.rep(html, "@APPLETINFO@", appletInfoDivs);
      html = PT.rep(html, "@APPLETDEFS@", str);
      html = PT.rep(html, "@CREATIONDATA@", GT
          .escapeHTML(WebExport.TimeStamp_WebLink()));
      html = javajs.util.PT
          .rep(
              html,
              "@AUTHORDATA@",
              GT
                  .escapeHTML(GT
                      .$("Based on a template by A. Herr&#x00E1;ez and J. Gutow")));
      html = PT.rep(html, "@LOGDATA@", "<pre>\n"
          + LogPanel.getText() + "\n</pre>\n");
      LogPanel.log("      ..." + GT.o(GT.$("creating {0}"), fileName));
      vwr.writeTextFile(fileName, html);
    } else {
      IOException IOe = new IOException(GT.$("Error creating directory: ")
          + datadirPath);
      throw IOe;
    }
    return fileName;
  }

  public static String replaceQuotedStrings(String s, ArrayList<String> list,
                                            ArrayList<String> newList) {
    int n = list.size();
    for (int i = 0; i < n; i++) {
      String name = list.get(i);
      String newName = newList.get(i);
      if (!newName.equals(name))
        s = PT.rep(s, "\"" + name + "\"", "\"" + newName
            + "\"");
    }
    return s;
  }

  public BS allSelectedWidgets() {
    BS selectedWidgets = BS.newN(nWidgets);
    DefaultListModel<JmolInstance> listModel = (DefaultListModel<JmolInstance>) instanceList.getModel();
    for (int i = 0; i < listModel.getSize(); i++) {
      JmolInstance thisInstance = listModel.getElementAt(i);
      selectedWidgets.or(thisInstance.whichWidgets);
    }
    return selectedWidgets;
  }

  private String copyBinaryFile(String fullPathName, String dataPath) {
    String name = fullPathName.substring(fullPathName.lastIndexOf('/') + 1)
        .replace('|', '_'); // xxx.zip|filename
    if (name.contentEquals("jsmol.zip")){
      LogPanel.log(GT.o(GT.$("copying and unzipping jsmol.zip directory into {0}"),dataPath));
      String tempDP = copyandUnzip(fullPathName,dataPath, name);
      return (tempDP);
    }
    name = dataPath + "/" + name;
    String gzname = name + ".gz";
    File outFile = new File(name);
    File gzoutFile = new File(gzname);
    if (outFile.exists())
      return name;
    if (gzoutFile.exists())
      return gzname;
    try {
      Object ret = vwr.fm.getFileAsBytes(fullPathName, null);
      if (ret instanceof String){
        LogPanel
            .log(GT.o(GT
                .$("Could not find or open:\n{0}\nPlease check that you are using a Jmol.jar that is part of a full Jmol distribution."),
                    fullPathName));
        errCount+=1;
      }else {
        LogPanel.log("      ..."
            + GT.o(GT.$("copying\n{0}\n         to"), fullPathName));
        byte[] data = (byte[]) ret;
        String[] retName = new String[] { name };
        int maxUnzipped = (name.indexOf(".js") >= 0 ? Integer.MAX_VALUE
            : 100000);
        String err = writeFileZipped(retName, data, maxUnzipped);
        if (!retName[0].equals(name))
          LogPanel.log("      ..." + GT.$("compressing large data file to")
              + "\n" + (name = retName[0]));
        LogPanel.log(name);
        if (err != null){
          LogPanel.log(err);
          errCount+=1;
        }
      }
    } catch (Exception e) {
      LogPanel.log(e.getMessage());
      errCount+=1;
    }
    return name;
  }

  private static String writeFileZipped(String[] retName, byte[] data,
                                        int maxUnzipped) {
    String err = null;
    try {
      boolean doCompress = false;
      if (data.length > maxUnzipped) {
        // don't compress binary files of any sort
        // as judged by having a nonASCII byte in first 10 bytes
        doCompress = true;
        for (int i = 0; i < 10; i++)
          if (data[i] < 10)
            doCompress = false;
      }
      if (doCompress) {
        // gzip it
        retName[0] += ".gz";
        GZIPOutputStream gzFile = new GZIPOutputStream(new FileOutputStream(
            retName[0]));
        gzFile.write(data);
        gzFile.flush();
        gzFile.close();
      } else {
        FileOutputStream os = new FileOutputStream(retName[0]);
        os.write(data);
        os.flush();
        os.close();
      }
    } catch (IOException e) {
      err = e.getMessage();
    }
    return err;
  }

  /******
   * Based on code available at Java2s.com
   * 
   * @param fullPathName
   *        String containing path to the zip file being copied and expanded
   * @param dataPath
   *        String containing path to the directory into which the file will be
   *        unzipped
   * @param name
   *        String containing name of the zipfile without the path (e.g.
   *        xxx.zip)
   * @return string containing path to where file copied.
   */
  private String copyandUnzip(String fullPathName, String dataPath, String name) {
    ZipFile zipFile = null;
    try {
      zipFile = new ZipFile(fullPathName);
      Enumeration<? extends ZipEntry> files = zipFile.entries();
      File f = null;
      FileOutputStream fos = null;
      byte[] buffer = new byte[1024];
      while (files.hasMoreElements()) {
        try {
          fos = null;
          ZipEntry entry = files.nextElement();
          if (!entry.getName().endsWith(".htm")
              && !entry.getName().contains("data" + File.separator)
              && !entry.getName().contains(".html")) {//don't need testing files
            InputStream eis = zipFile.getInputStream(entry);
            f = new File(dataPath + File.separator + entry.getName());
            if (entry.isDirectory()) {
              Logger.info("creating directory " + f);
              f.mkdirs();
            } else {
              f.getParentFile().mkdirs();
              f.createNewFile();
              fos = new FileOutputStream(f);
              int bytesRead = 0;
              while ((bytesRead = eis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
              }
            }
          }
        } catch (IOException e) {
          Logger.error(e.getMessage());
          break;
        }
        if (fos != null) {
          try {
            fos.close();
          } catch (IOException e) {
            // ignore
          }
        }
      }
    } catch (IOException e) {
      LogPanel
          .log(GT
              .$("Error encountered while openning zip file. You may not have\n"
                  + "a complete copy of the Jmol distribution.  Check for the file jsmol.zip.\n"
                  + e.getMessage()));
      errCount += 1;
    }
    try {
      zipFile.close();
    } catch (IOException e) {
      //ignore
    }
    return (dataPath);
  }
  
  /*****
   * @return the URL pointing to the Jmol.jar that is running
   */
  private URL jmolJarURL(){
    return(this.getClass().getProtectionDomain().getCodeSource().
        getLocation());
  }
/******
* Returns a string version of the path to Jmol.jar (including the trailing Jmol.jar)
* decoded using the system default text encoding (usually UTF-8).
* @return system text encoding translated string version of the path to Jmol.jar
* @throws java.io.UnsupportedEncodingException if the encoding can't be used to
* decode the URL or the encoding is bad.
*/
  private String jmolJarPath() throws java.io.UnsupportedEncodingException{
    String pathStr;
    pathStr=null;
    URL jarURL = jmolJarURL();
    pathStr=URLDecoder.decode(jarURL.toString(), System.getProperty("file.encoding"));
    pathStr = pathStr.substring(pathStr.indexOf(":")+1, pathStr.length());
    return(pathStr);
  }
  
  void syncLists() {
    DefaultListModel<JmolInstance> model1 = (DefaultListModel<JmolInstance>) instanceList.getModel();
    for (int j = 0; j < webPanels.length; j++) {
      if (j != panelIndex) {
        JList<JmolInstance> list = webPanels[j].instanceList;
        DefaultListModel<JmolInstance> model2 = (DefaultListModel<JmolInstance>) list.getModel();
        model2.clear();
        int n = model1.getSize();
        for (int i = 0; i < n; i++)
          model2.addElement(model1.get(i));
        list.setSelectedIndices(new int[] {});
        webPanels[j].enableButtons(list);
      }
    }
    enableButtons(instanceList);
  }

  void enableButtons(JList<?> list) {
    int nSelected = list.getSelectedIndices().length;
    int nListed = list.getModel().getSize();
    saveButton.setEnabled(nListed > 0);
    viewButton.setEnabled(false);
    deleteInstanceButton.setEnabled(nSelected > 0);
//    showInstanceButton.setEnabled(nSelected == 1);
  }

  class InstanceCellRenderer extends JLabel implements ListCellRenderer<Object> {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
      setText(" " + ((JmolInstance) value).name);
      if (isSelected) {
        setBackground(list.getSelectionBackground());
        setForeground(list.getSelectionForeground());
      } else {
        setBackground(list.getBackground());
        setForeground(list.getForeground());
      }
      setEnabled(list.isEnabled());
      setFont(list.getFont());
      setOpaque(true);
      enableButtons(list);
      return this;
    }
  }

  public static boolean checkOption( Object o) {
    return (o instanceof Boolean && ((Boolean) o).booleanValue()
        || o instanceof Integer && ((Integer) o).intValue() != 0);
  }


}

class ArrayListTransferHandler extends TransferHandler {
  DataFlavor localArrayListFlavor, serialArrayListFlavor;
  String localArrayListType = DataFlavor.javaJVMLocalObjectMimeType
      + ";class=java.util.ArrayList";
  JList<?> source = null;
  int[] sourceIndices = null;
  int addIndex = -1; // Location where items were added
  int addCount = 0; // Number of items added
  WebPanel webPanel;

  ArrayListTransferHandler(WebPanel webPanel) {
    this.webPanel = webPanel;
    try {
      localArrayListFlavor = new DataFlavor(localArrayListType);
    } catch (ClassNotFoundException e) {
      System.out
          .println("ArrayListTransferHandler: unable to create data flavor");
    }
    serialArrayListFlavor = new DataFlavor(ArrayList.class, "ArrayList");
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean importData(JComponent c, Transferable t) {
    if (sourceIndices == null || !canImport(c, t.getTransferDataFlavors())) {
      return false;
    }
    JList<?> target = null;
    Lst<?> alist = null;
    try {
      target = (JList<?>) c;
      if (hasLocalArrayListFlavor(t.getTransferDataFlavors())) {
        alist = (Lst<?>) t.getTransferData(localArrayListFlavor);
      } else if (hasSerialArrayListFlavor(t.getTransferDataFlavors())) {
        alist = (Lst<?>) t.getTransferData(serialArrayListFlavor);
      } else {
        return false;
      }
    } catch (UnsupportedFlavorException ufe) {
      Logger.error("importData: unsupported data flavor");
      return false;
    } catch (IOException ioe) {
      Logger.error("importData: I/O exception");
      return false;
    }

    // At this point we use the same code to retrieve the data
    // locally or serially.

    // We'll drop at the current selected index.
    int targetIndex = target.getSelectedIndex();

    // Prevent the user from dropping data back on itself.
    // For example, if the user is moving items #4,#5,#6 and #7 and
    // attempts to insert the items after item #5, this would
    // be problematic when removing the original items.
    // This is interpreted as dropping the same data on itself
    // and has no effect.
    if (source.equals(target)) {
      // System.out.print("checking indices index TO: " + targetIndex + "
      // FROM:");
      // for (int i = 0; i < sourceIndices.length;i++)
      // System.out.print(" "+sourceIndices[i]);
      //System.out.println("");
      if (targetIndex >= sourceIndices[0]
          && targetIndex <= sourceIndices[sourceIndices.length - 1]) {
        //System.out.println("setting indices null : " + targetIndex + " " +
        // sourceIndices[0] + " " + sourceIndices[sourceIndices.length - 1]);
        sourceIndices = null;
        return true;
      }
    }

    DefaultListModel<?> listModel = (DefaultListModel<?>) target.getModel();
    int max = listModel.getSize();
    if (targetIndex < 0) {
      targetIndex = max;
    } else {
      if (sourceIndices[0] < targetIndex)
        targetIndex++;
      if (targetIndex > max) {
        targetIndex = max;
      }
    }
    addIndex = targetIndex;
    addCount = alist.size();
    for (int i = 0; i < alist.size(); i++) {
      ((DefaultListModel<Object>) listModel).add(targetIndex++,
          objectOf(listModel, alist.get(i)));
    }
    return true;
  }

  private static Object objectOf(DefaultListModel<?> listModel, Object objectName) {
    if (objectName instanceof String) {
      String name = (String) objectName;
      Object o;
      for (int i = listModel.size(); --i >= 0;)
        if (!((o = listModel.get(i)) instanceof String)
            && o.toString().equals(name))
          return listModel.get(i);
    }
    return objectName;
  }

  @Override
  protected void exportDone(JComponent c, Transferable data, int action) {
    //System.out.println("action="+action + " " + addCount + " " +
    // sourceIndices);
    if ((action == MOVE) && (sourceIndices != null)) {
      DefaultListModel<?> model = (DefaultListModel<?>) source.getModel();

      // If we are moving items around in the same list, we
      // need to adjust the indices accordingly since those
      // after the insertion point have moved.
      if (addCount > 0) {
        for (int i = 0; i < sourceIndices.length; i++) {
          if (sourceIndices[i] > addIndex) {
            sourceIndices[i] += addCount;
          }
        }
      }
      for (int i = sourceIndices.length - 1; i >= 0; i--)
        model.remove(sourceIndices[i]);
      ((JList<?>) c).setSelectedIndices(new int[] {});
      if (webPanel != null)
        webPanel.syncLists();
    }
    sourceIndices = null;
    addIndex = -1;
    addCount = 0;
  }

  private boolean hasLocalArrayListFlavor(DataFlavor[] flavors) {
    if (localArrayListFlavor == null) {
      return false;
    }

    for (int i = 0; i < flavors.length; i++) {
      if (flavors[i].equals(localArrayListFlavor)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasSerialArrayListFlavor(DataFlavor[] flavors) {
    if (serialArrayListFlavor == null) {
      return false;
    }

    for (int i = 0; i < flavors.length; i++) {
      if (flavors[i].equals(serialArrayListFlavor)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean canImport(JComponent c, DataFlavor[] flavors) {
    if (hasLocalArrayListFlavor(flavors)) {
      return true;
    }
    if (hasSerialArrayListFlavor(flavors)) {
      return true;
    }
    return false;
  }

  @Override
  protected Transferable createTransferable(JComponent c) {
    if (c instanceof JList<?>) {
      source = (JList<?>) c;
      sourceIndices = source.getSelectedIndices();
      java.util.List<?> values = source.getSelectedValuesList();
      if (values == null || values.size() == 0) {
        return null;
      }
      Lst<String> alist = new Lst<String>();
      for (int i = 0; i < values.size(); i++) {
        Object o = values.get(i);
        String str = o.toString();
        if (str == null)
          str = "";
        alist.addLast(str);
      }
      return new ArrayListTransferable(alist);
    }
    return null;
  }

  @Override
  public int getSourceActions(JComponent c) {
    return COPY_OR_MOVE;
  }

  class ArrayListTransferable implements Transferable {
    Lst<String> data;

    ArrayListTransferable(Lst<String> alist) {
      data = alist;
    }

    @Override
    public Object getTransferData(DataFlavor flavor)
        throws UnsupportedFlavorException {
      if (!isDataFlavorSupported(flavor)) {
        throw new UnsupportedFlavorException(flavor);
      }
      return data;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
      return new DataFlavor[] { localArrayListFlavor, serialArrayListFlavor };
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
      if (localArrayListFlavor.equals(flavor)) {
        return true;
      }
      if (serialArrayListFlavor.equals(flavor)) {
        return true;
      }
      return false;
    }
  }
}
