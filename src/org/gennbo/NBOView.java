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
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Map;

import javax.swing.SwingConstants;
import javajs.util.PT;
import javajs.util.SB;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.gennbo.NBODialog.HelpBtn;
import org.jmol.awt.AwtColor;
import org.jmol.i18n.GT;
import javajs.util.BS;
import org.jmol.util.C;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.*;
import java.awt.image.*;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.net.URL;

class NBOView {

  protected NBODialog dialog;
  protected Viewer vwr;

  private static final int MODE_VIEW_IMAGE = 13;
  private static final int MODE_VIEW_LIST = 23;
  private static final int MODE_VIEW_VIDEO= 33;

  protected NBOView(NBODialog dialog) {
    this.dialog = dialog;
    this.vwr = dialog.vwr;
  }

  protected final int BASIS_AO = 0;
  protected final int BASIS_PNAO = 1;
  protected final int BASIS_NAO = 2;
  protected final int BASIS_PNHO = 3;
  protected final int BASIS_NHO = 4;
  protected final int BASIS_PNBO = 5;
  protected final int BASIS_NBO = 6;
  protected final int BASIS_PNLMO = 7;
  protected final int BASIS_NLMO = 8;
  protected final int BASIS_MO = 9;

  protected final static String[] basSet = 
    { "AO",  // 0   
      "PNAO", "NAO",  // 1,2 
      "PNHO", "NHO",  // 3,4
      "PNBO", "NBO", "PNLMO", "NLMO", // 5-8 
      "MO" }; // 9

  protected final static int[] basisLabelKey = 
    {//AO   NAO    NHOab     NBOab     MO 
        0,  1, 1,  2, 2,  3, 3, 3, 3,  4, // alpha
        0,  1, 1,  5, 5,  6, 6, 6, 6,  4  // beta   
    };
  
  private String[][] orbitalLabels;
  
  private void clearLabelSet() {
    orbitalLabels = new String[7][];
  }

  private String[] getLabelSet(int ibas, boolean isAlpha) {
    return orbitalLabels[getIbasKey(ibas, isAlpha)];
  }
  
  private int getIbasKey(int ibas, boolean isBeta) {
    return basisLabelKey[ibas + (isBeta ? 10 : 0)];
  }
  private void addBasisLabel(int ikey, String[] tokens) {
    orbitalLabels[ikey] = tokens;
  }
  
  private JScrollPane orbScroll;
  private Box centerBox, bottomBox;
  private Box vecBox;
  private Box planeBox;
  private JRadioButton profileBtn, contourBtn, viewBtn;
  private JRadioButton atomOrient;
  private DefaultListModel<String> alphaList, betaList; // useful for no-NBOServe use?
  
  /**
   * "Jmol" vs. "Atom" perspective chosen
   */
  private boolean jmolView;

  // effectively private, but need to be protected
  // because they are used by private inner classes

  protected OrbitalList orbitals;

  /**
   * The state of the VIEW panel, in terms of what modal dialogs are open; reset
   * to VIEW_STATE_MAIN each time openPanel() is run.
   */
  protected int viewState;

  protected final static int VIEW_STATE_MAIN = 0;
  protected final static int VIEW_STATE_PLANE = 1;
  protected final static int VIEW_STATE_VECTOR = 2;
  protected final static int VIEW_STATE_CAMERA = 3;

  // used in SEARCH and in some cases NBODialg itself:

  protected JComboBox<String> comboBasis1;
  protected JRadioButton alphaSpin, betaSpin;
  protected boolean isNewModel = true;

  // used by SEARCH; cleared by openPanel() using 

  protected String currOrb = "";
  protected int currOrbIndex;

  //NBOServe view settings
  private String[] plVal, vecVal, lineVal;
  
  private String videoInputFileType;
  
  private JRadioButton video;
  
  //dialog box for video
  private JDialog videoDialog;
  
  private NBOFileHandler scriptVideoFileHandler;
  private NBOFileHandler gifVideoFileHandler;
  private final int SCRIPT_MODE = 0;
  private final int GIF_MODE = 1;
  
  private JRadioButton createVideo,saveVideo,playVideo;
  
  private boolean validScriptFilePath, validGifFilePath;
  
  private JComboBox<String> dropDownMenu;
  private int frameRate;
  
  private String scriptVideoPath, gifVideoPath;
  public String savevideo_jobstem;
 
  
  /**
   * reset the arrays of values that will be sent to NBOServe to their default
   * values.
   * 
   */
  protected void setDefaultParameterArrays() {
    plVal = new String[] { "1", "2", "3", "0.5", "0.0", "0.0", "0.0", "-3.0",
        "3.0", "-3.0", "3.0", "25" };
    vecVal = new String[] { "1", "2", "0.5", "-2.0", "2.0", "-1.0", "1.0",
        "100" };
    lineVal = new String[] { "0.03", "0.05", "4", "0.05", "0.05", "0.1", "0.1" };
  }


  private final JTextField[] vectorFields = new JTextField[8];
  private final JTextField[] planeFields = new JTextField[12];
  private final JTextField[] camFields = new JTextField[53];
  private final JTextField[] lineFields = new JTextField[7];

  private String[] camVal = { "6.43", "0.0", "0.0", "50.0", "2.0", "2.0",
      "0.0", "0.60", "1.0", "1.0", "40.0", "0.0", "0.60", "1.0", "1.0", "40.0",
      "0.0", "0.60", "1.0", "1.0", "40.0", "0.0", "0.60", "1.0", "1.0", "40.0",
      "0.5", "1.0", "1.0", "1.0", "0.8", "0.0", "0.0", "1.0", "0.8", "0.4",
      "0.0", "1.0", "1.0", "0.5", "0.5", "0.5", "0.0", "0.7", "1.0", "0.22",
      "0.40", "0.10", "0.05", "0.0316", "0.0001", "0.4000", "1" };

  private String[] camFieldIDs = {
      // 0 - 6
      "1a", "1b", "1c", "1d", "1e", "1f",
      "1g",
      // 7 - 26
      "2a", "2b", "2c", "2d", "2e", "2f", "2g", "2h", "2i", "2j", "2k", "2l",
      "2m", "2n", "2o", "2p", "2q", "2r", "2s", "2t",
      // 27 - 44
      "3a", "3b", "3c", "3d", "3e", "3f", "3g", "3h", "3i", "3j", "3k", "3l",
      "3m", "3n", "3o", "3p", "3q", "3r",
      // 45 - 48
      "4a", "4b", "4c", "4d",
      // 49 - 51
      "5a", "5b", "5c", "6" };

  protected JPanel buildViewPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    viewState = VIEW_STATE_MAIN;
    ///panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    dialog.runScriptQueued("set bondpicking true");

    Box b = createViewSearchJobBox(NBOFileHandler.MODE_VIEW,true);
    if (!dialog.jmolOptionNONBO)
      panel.add(b, BorderLayout.NORTH);

    centerBox = NBOUtil.createTitleBox(" Select Orbital(s) ",
        createSelectOrbitalBox());
    centerBox.setVisible(false);
    centerBox.add(createOrbitalPanel());
    panel.add(centerBox, BorderLayout.CENTER);

    panel.add(createBottomBox(), BorderLayout.SOUTH);

    updateViewSettings();
    frameRate=-1;
    dialog.inputFileHandler.setBrowseEnabled(true);

    //    String fileType = dialog.runScriptNow("print _fileType");
    //    if (fileType.equals("GenNBO")) {
    //      File f = new File(
    //          dialog.runScriptNow("select within(model, visible); print _modelFile"));
    //      dialog.inputFileHandler.setInputFile(NBOFileHandler.newNBOFile(f, "47"));
    //    }

    return panel;
  }
  
  /**
   * Creates the title blocks for VIEW module with background color for headers.
   * 
   * Instead of creating title box at NBOUtil, we created it in NBOView because we will need to add
   * radio button and action listener to the title box of NBOView (Special case)
   * 
   * @param title
   *        - title for the section
   * @param rightSideComponent
   *        help button, for example
   * @return Box formatted title box
   */
  private Box createTitleBox(String title, Component rightSideComponent) {
    Box box = Box.createVerticalBox();
    JLabel label = new JLabel(title);
    label.setAlignmentX(0);
    label.setBackground(NBOConfig.titleColor);
    label.setForeground(Color.white);
    label.setFont(NBOConfig.titleFont);
    label.setOpaque(true);
    
    video=new JRadioButton("Video");
    video.setHorizontalAlignment(JRadioButton.CENTER);
    video.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent arg0)
      {
        doVideo();
      }
    }
    );
    
    if (rightSideComponent != null) {
      JPanel box2 = new JPanel(new BorderLayout());
      box2.setAlignmentX(0);
      box2.setMaximumSize(new Dimension(360, 25));
      box2.add(label, BorderLayout.WEST);
      box2.add(video,BorderLayout.CENTER);
      box2.add(rightSideComponent, BorderLayout.EAST);
      box.add(box2);
    } else
    {
      box.add(label,BorderLayout.WEST);
      box.add(video,BorderLayout.CENTER);
    }
    
    video.setVisible(false);
    box.setAlignmentX(0.0f);
  
    return box;
  }
  
  public static Box createTitleBoxForVideoMenu(String title, Component rightSideComponent) {
    Box box = Box.createVerticalBox();
    JLabel label = new JLabel(title);
    label.setAlignmentX(0);
    label.setBackground(NBOConfig.titleColor);
    label.setForeground(Color.white);
    label.setFont(NBOConfig.titleFont);
    label.setOpaque(true);
    if (rightSideComponent != null) {
      JPanel box2 = new JPanel(new BorderLayout());
      box2.setAlignmentX(0);
      box2.add(label, BorderLayout.WEST);
      box2.add(rightSideComponent, BorderLayout.EAST);
      box2.setMaximumSize(new Dimension(400, 25));
      box.add(box2);
    } else
      box.add(label,BorderLayout.WEST);
    box.setAlignmentX(0.0f);
  
    return box;
  }
  
  /*
   * Helper function to create file handler for VIDEO
   */
  private NBOFileHandler getFileHandlerForVideo(int mode)
  {
    NBOFileHandler fileHandler=null;
    if(mode==SCRIPT_MODE)
    {
      fileHandler=new NBOFileHandler("","",NBOFileHandler.MODE_VIEW_VIDEO, NBOConfig.SCRIPT_VIDEO_EXTENSIONS, dialog)
      {
        @Override
        protected boolean doFileBrowsePressed()
        {
          String folder = tfDir.getText().trim();
          String name = tfName.getText();
          String ext = tfExt.getText().trim();
          if (name.length() == 0)
            name = "*";
          if (ext.length() == 0)
            ext = "script";
          folder = NBOUtil.getWindowsFullNameFor(folder, name, ext);
          JFileChooser myChooser = new JFileChooser();
          
          useExt = (ext.equals("") ? NBOConfig.SCRIPT_VIDEO_EXTENSIONS : ext);
          //file type must be only be script for now. But the structure of this method is set up such that 
          //it can easily allow other file type other than cmd in the future.
          videoInputFileType="script";
          String filter=useExt;
          myChooser.setFileFilter(new FileNameExtensionFilter(filter, filter));
          myChooser.setFileHidingEnabled(true);
          if (folder.endsWith("/"))
            folder = folder + "*.*";
          if (!folder.equals(""))
            myChooser.setSelectedFile(new File(folder));
          int button = myChooser.showDialog(this, GT.$("Select"));
          if (button == JFileChooser.APPROVE_OPTION) 
          {
            if (PT.isOneOf(videoInputFileType, NBOConfig.SCRIPT_VIDEO_EXTENSIONS)) 
            {
              File newFile = myChooser.getSelectedFile();
              if (newFile.toString().indexOf(".") < 0) 
              {
                dialog.logError("File not found");
                validScriptFilePath=false;
                return false;
              }
              //do something to process the file here
              scriptVideoPath=newFile.getAbsolutePath();
              jobStem = NBOUtil.getJobStem(newFile);
              tfName.setText(jobStem);
              dialog.inputFileHandler.setInput(fullFilePath, jobStem, NBOUtil.getExt(newFile));
              validScriptFilePath=true;
              return true;
            }
            else
            {
              dialog.logError("Invalid input file type defined");
            }
          }
          validScriptFilePath=false;
          return false;
        }
      };
    }
    else if(mode==GIF_MODE)
    {
      fileHandler=new NBOFileHandler("","",NBOFileHandler.MODE_VIEW_VIDEO, NBOConfig.GIF_VIDEO_EXTENSIONS, dialog)
      {
        @Override
        protected boolean doFileBrowsePressed()
        {
          String folder = tfDir.getText().trim();
          String name = tfName.getText();
          String ext = tfExt.getText().trim();
          if (name.length() == 0)
            name = "*";
          if (ext.length() == 0)
            ext = "gif";
          folder = NBOUtil.getWindowsFullNameFor(folder, name, ext);
          JFileChooser myChooser = new JFileChooser();
          
          useExt = (ext.equals("") ? NBOConfig.GIF_VIDEO_EXTENSIONS : ext);
          
          String filter=useExt;
          myChooser.setFileFilter(new FileNameExtensionFilter(filter, filter));
          myChooser.setFileHidingEnabled(true);
          if (folder.endsWith("/"))
            folder = folder + "*.*";
          if (!folder.equals(""))
            myChooser.setSelectedFile(new File(folder));
          int button = myChooser.showDialog(this, GT.$("Select"));
          if (button == JFileChooser.APPROVE_OPTION) 
          {
            if (PT.isOneOf(useExt, NBOConfig.GIF_VIDEO_EXTENSIONS)) 
            {
              File newFile = myChooser.getSelectedFile();
              if (newFile.toString().indexOf(".") < 0) 
              {
                dialog.logError("File not found");
                validGifFilePath=false;
                return false;
              }
              //do something to process the file here
              jobStem = NBOUtil.getJobStem(newFile);
              tfName.setText(jobStem);
              dialog.inputFileHandler.setInput(fullFilePath, jobStem, NBOUtil.getExt(newFile));
              gifVideoPath=newFile.getAbsolutePath();
              validGifFilePath=true;
              return true;
            }
            else
            {
              dialog.logError("Invalid input file type defined. Only gif file is accepted.");
            }
          }
          validGifFilePath=false;
          return false;
        }
      };
    }
    return fileHandler;
  }
  
  private void createDropDownMenuForGIFCreation()
  {
    String[] frameRate= {"Select frames/sec","20","40","60","80","100"};
    dropDownMenu=new JComboBox<String>(frameRate);
    dropDownMenu.setPrototypeDisplayValue("Select frames/sec");
    dropDownMenu.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        doDropDownMenuAction(dropDownMenu.getSelectedIndex()>0? dropDownMenu.getSelectedItem().toString():"-1");
      }
    });
  }
  
  private void doDropDownMenuAction(String num)
  {
    int number=Integer.parseInt(num);
    if(number==-1)
      frameRate=-1;
    else
    {
      frameRate=1/number;
      frameRate=frameRate*1000;
    }
  }
  
  /*
   * create and return VIDEO menu
   */
  public void doVideo()
  {
    if(videoDialog!=null)
    {    
      videoDialog.setVisible(true);
      return;
    }
    
    Box box=createTitleBoxForVideoMenu("Animated GIF Sequence",dialog.new HelpBtn("view_video_help.htm"));
    
    ButtonGroup buttonGroup=new ButtonGroup();
    
    final JLabel scriptFileIn=new JLabel(" Script File");
    scriptFileIn.setFont(NBOConfig.monoFont);
    final JLabel gifFileIn=new JLabel(" GIF File");
    gifFileIn.setFont(NBOConfig.monoFont);
   
   
    validScriptFilePath=false; validGifFilePath=false; 
    
    final JButton goBtn = new JButton("GO ");
    goBtn.setMinimumSize(new Dimension(60, 30));
    goBtn.setMaximumSize(new Dimension(60, 30));
    goBtn.setPreferredSize(new Dimension(60, 30));
    goBtn.setEnabled(false);

    ActionListener goEnableAction = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        goBtn.setEnabled(true);
      }
    };
    
    /////////////////////////////buttons for video menu
    ButtonGroup bg = new ButtonGroup();
    
    createVideo=new JRadioButton("CREATE frames from script");
    createVideo.setSelected(false); 
    createVideo.addActionListener(goEnableAction);
    
    saveVideo=new JRadioButton("SAVE frames to GIF file");
    saveVideo.setSelected(false); 
    saveVideo.addActionListener(goEnableAction);
    
    playVideo=new JRadioButton("PLAY GIF file as animated video");
    playVideo.setSelected(false); 
    playVideo.addActionListener(goEnableAction);
    
    bg.add(createVideo); bg.add(saveVideo); bg.add(playVideo);
    
    /////////////////////////////file handlers for video menu
    
    scriptVideoFileHandler=getFileHandlerForVideo(SCRIPT_MODE);
    scriptVideoFileHandler.tfExt.setText("script");
   
    gifVideoFileHandler=getFileHandlerForVideo(GIF_MODE);
    gifVideoFileHandler.tfExt.setText("gif");
    
    /////////////////////////////panel for storing file handlers
    JPanel fileHandlerPanel=new JPanel(new GridLayout(4,1,0,0));
    fileHandlerPanel.setMaximumSize(new Dimension(400, 175));
    fileHandlerPanel.setPreferredSize(new Dimension(400, 175));
    fileHandlerPanel.setMinimumSize(new Dimension(400, 175));
    fileHandlerPanel.setAlignmentX(0);
    fileHandlerPanel.add(scriptFileIn);
    fileHandlerPanel.add(scriptVideoFileHandler);
    fileHandlerPanel.add(gifFileIn);
    fileHandlerPanel.add(gifVideoFileHandler);
    
    ////////////////////////////////panel for storing radio buttons
    createDropDownMenuForGIFCreation();
    JPanel buttonPanel=new JPanel(new GridLayout(4,1,0,0));
    buttonPanel.setMaximumSize(new Dimension(250, 150));
    buttonPanel.setPreferredSize(new Dimension(250, 150));
    buttonPanel.setMinimumSize(new Dimension(250, 150));
    buttonPanel.setAlignmentX(0);
    buttonPanel.add(createVideo);
    buttonPanel.add(saveVideo);
    buttonPanel.add(dropDownMenu);
    buttonPanel.add(playVideo);
    
    
    JPanel goPanel=new JPanel(new BorderLayout());
    goPanel.setMaximumSize(new Dimension(380, 40));
    goPanel.setMinimumSize(new Dimension(380, 40));
    goPanel.setPreferredSize(new Dimension(380, 40));
    goPanel.setAlignmentX(0);
    goPanel.add(goBtn,BorderLayout.EAST);
    goBtn.setHorizontalAlignment(SwingConstants.RIGHT);
    
    goBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doVideoGoPressed();
      }
    });
//    inner.add(goPanel);
    
    Box box2 = Box.createVerticalBox();
    
    box2.add(fileHandlerPanel);
    box2.add(buttonPanel);
    box.add(box2);
    box2.setAlignmentX(0.0f);
    box.add(goPanel);
     
    
    videoDialog=new JDialog(dialog,"Video Menu");
    videoDialog.setSize(400, 450);
    videoDialog.setVisible(true);
    
    videoDialog.add(box);
    centerDialog(videoDialog,200);
  }
  
  
  private void doVideoGoPressed()
  {
    if(createVideo.isSelected())
      createVideo();
    else if(saveVideo.isSelected())
      saveVideo();
    else if(playVideo.isSelected())
      playVideo();
    
    videoDialog.setVisible(false);
  }
  
  private void createVideo()
  {
    if(!validScriptFilePath)
    {
      dialog.logInfo("Error: Invalid path for script file", Logger.LEVEL_ERROR);
      return;
    }
    SB sb = new SB();
    NBOUtil.postAddCmd(sb, "VC "+scriptVideoPath);
    postNBO_v(sb, MODE_VIEW_VIDEO, -1, null,"Sending video create command..", null, null);
  }
  
  class BMP_Files implements Comparable<BMP_Files>
  {
    public File file;
    public String filename;
    
    public BMP_Files(File file,String filename)
    {
      this.file=file;
      this.filename=filename;
    }
    
    public int compareTo(BMP_Files other)
    {
      return this.filename.compareTo(other.filename);
    }
    
  }
  
  
  
  private File[] finder(String directoryName,String jobstem)
  {
    int counter=0,i;
    
    savevideo_jobstem=jobstem;
    
    File dir=new File(directoryName);
    File bmpFiles[]=dir.listFiles(new FilenameFilter()
    {
      @Override
      public boolean accept(File dir, String filename)
      {
        String curr_filename=filename.trim();
        return curr_filename.startsWith(savevideo_jobstem+"_");
      }
    });
    
    //sort the files according to their filename
    ArrayList<BMP_Files> files=new ArrayList<BMP_Files>();
    for(File file:bmpFiles)
    {
      files.add(new BMP_Files(file,file.getName()));
      counter++;
    }
    Collections.sort(files);
    
    File bmps[]=new File[counter];
    for(i=0;i<counter;i++)
    {
      bmps[i]=files.get(i).file;
    }
    
    return bmps;
  }
  
  
  
  private void saveVideo()
  { 
    String folder = gifVideoFileHandler.tfDir.getText().trim();
    String name = gifVideoFileHandler.tfName.getText();
    String ext = gifVideoFileHandler.tfExt.getText().trim();
    
    if(( folder.equals("")||name.equals("")||ext.equals("") ) && !validGifFilePath )
    {
      dialog.logInfo("Error: Invalid path for gif file", Logger.LEVEL_ERROR);
      return;
    }
    if(frameRate==-1)
    {
      dialog.logInfo("Error: Please select a framerate", Logger.LEVEL_ERROR);
      return;
    }
    
    
    File bmpFiles[]=finder(folder,name);
    int timeBetweenFrameInMS=frameRate;
    int i;
    GIFWriter writer=null;
    ImageOutputStream output=null;
    

    int strLength=folder.length();
    if(folder.charAt(strLength-1)!='/')
      folder=folder+"/";
    gifVideoPath=folder+name+"."+ext;
    
    try
    {
      BufferedImage firstImage=ImageIO.read(bmpFiles[0]);
      output=new FileImageOutputStream(new File(gifVideoPath));
      writer=new GIFWriter(output,firstImage.getType(),timeBetweenFrameInMS,true);
      writer.writeToSequence(firstImage);
      for(i=1;i<bmpFiles.length;i++)
      {
        BufferedImage nextImage=ImageIO.read(bmpFiles[i]);
        writer.writeToSequence(nextImage);
      }
      
    }
    catch(IIOException e1)
    {
      dialog.logInfo("Error creating GIF: "+e1, Logger.LEVEL_ERROR);
    }
    catch(IOException e2)
    {
      dialog.logInfo("Error creating GIF: "+e2, Logger.LEVEL_ERROR);
    }
    finally
    {
      //this method of writing is ugly, but it's essential to ensure that the writer and output is close no matter what.
      //improvement can be made here for code aesthetics
      try
      {
        if(writer!=null)
        {
          writer.close();
        }
        if(output!=null)
        {
          output.close();
        }
      }
      catch(IOException ioError)
      {
        dialog.logInfo("Fatal Error: Unable to close gifWriter or output stream", Logger.LEVEL_ERROR);
      }
    }
    dialog.logValue("GIF file has been successfully created at "+gifVideoPath);
  }
  
  private void playVideo()
  {
    String folder = gifVideoFileHandler.tfDir.getText().trim();
    String name = gifVideoFileHandler.tfName.getText();
    String ext = gifVideoFileHandler.tfExt.getText().trim();
    
    if(( folder.equals("")||name.equals("")||ext.equals("") ) && !validGifFilePath )
    {
      dialog.logInfo("Error: Invalid path for gif file", Logger.LEVEL_ERROR);
      return;
    }
    
   
    int strLength=folder.length();
    if(folder.charAt(strLength-1)!='/')
      folder=folder+"/";
    gifVideoPath=folder+name+"."+ext;
    
    File file=new File(gifVideoPath);
    if(!file.exists())
    {
      dialog.logInfo("Error: Invalid path for gif file", Logger.LEVEL_ERROR);
      return;
    }
    
    
    ImageIcon icon = new ImageIcon(gifVideoPath);
    JLabel label = new JLabel(icon);
    
    JFrame f=new JFrame("Animation");
    f.getContentPane().add(label);
    f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    f.pack();
    f.setLocationRelativeTo(null);
    f.setVisible(true);
  }
  
  
  protected Box createViewSearchJobBox(int mode,boolean isNBOView) {
    Box topBox;
    if(isNBOView)
    {
      topBox = createTitleBox(" Select Job ", dialog.new HelpBtn(
          (mode == NBOFileHandler.MODE_SEARCH ? "search" : "view")
              + "_job_help.htm"));
    }
    else
    {
      topBox = NBOUtil.createTitleBox(" Select Job ", dialog.new HelpBtn(
          (mode == NBOFileHandler.MODE_SEARCH ? "search" : "view")
              + "_job_help.htm"));
    }
    Box inputBox = NBOUtil.createBorderBox(true);
    inputBox.setPreferredSize(new Dimension(360, 50));
    inputBox.setMaximumSize(new Dimension(360, 50));
    topBox.add(inputBox);
    dialog.getNewInputFileHandler(mode);
    inputBox.add(Box.createVerticalStrut(5));
    inputBox.add(dialog.inputFileHandler);
    return topBox;
  }

  private Component createBottomBox() {
    bottomBox = NBOUtil.createTitleBox(" Display Type ", dialog.new HelpBtn(
        "view_display_help.htm"));
    JPanel profBox = new JPanel(new GridLayout(2, 3, 0, 0));
    profBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    profBox.setAlignmentX(0.0f);

    ButtonGroup bg = new ButtonGroup();
    profileBtn = new JRadioButton("1D Profile");
    profileBtn.setToolTipText("Produce profile plot from axis parameters");
    bg.add(profileBtn);

    final JButton goBtn = new JButton("GO");
    goBtn.setEnabled(false);

    ActionListener goEnableAction = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        goBtn.setEnabled(true);
      }
    };
    profileBtn.addActionListener(goEnableAction);
    profBox.add(profileBtn);//.setFont(nboFont);

    contourBtn = new JRadioButton("2D Contour");
    contourBtn.setToolTipText("Produce contour plot from plane parameters");
    profBox.add(contourBtn);//.setFont(nboFont);
    contourBtn.addActionListener(goEnableAction);
    bg.add(contourBtn);

    viewBtn = new JRadioButton("3D view");
    viewBtn.addActionListener(goEnableAction);
    bg.add(viewBtn);
    profBox.add(viewBtn);//.setFont(nboFont);
    
    vecBox = Box.createHorizontalBox();
    vecBox.setAlignmentX(0.0f);
    vecBox.setMaximumSize(new Dimension(120, 25));
    profBox.add(vecBox);

    planeBox = Box.createHorizontalBox();
    planeBox.setAlignmentX(0.0f);
    planeBox.setMaximumSize(new Dimension(120, 25));
    profBox.add(planeBox);

    goBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doGoPressed();
      }
    });
    profBox.add(goBtn);
    
    //Added by fzy: set 3D view to default
    viewBtn.setSelected(true);
    goBtn.setEnabled(true);
    
    bottomBox.add(profBox);
    bottomBox.setVisible(false);
    return bottomBox;
  }

  private Component createSelectOrbitalBox() {
    Box horizBox = Box.createHorizontalBox();
    alphaList = betaList = null;
    comboBasis1 = new JComboBox<String>(basSet);
    comboBasis1.setMaximumSize(new Dimension(70, 25));
    //comboBasis1.setUI(new StyledComboBoxUI(180, -1));
    horizBox.add(comboBasis1);
    comboBasis1.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (comboBasis1.isEnabled())
          doSetNewBasis(false, true);
      }
    });
    betaSpin = new JRadioButton("<html>&#x3B2</html>");
    alphaSpin = new JRadioButton("<html>&#x3B1</html>");
    alphaSpin.setSelected(true);
    ButtonGroup spinSelection = new ButtonGroup();
    spinSelection.add(alphaSpin);
    spinSelection.add(betaSpin);
    betaSpin.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        EventQueue.invokeLater(new Runnable() {

          @Override
          public void run() {
            doSetSpin(isAlphaSpin() ? null : "beta");
          }

        });
      }
    });
    alphaSpin.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        EventQueue.invokeLater(new Runnable() {

          @Override
          public void run() {
            doSetSpin(isAlphaSpin() ? "alpha" : null);
          }

        });
      }
    });
    horizBox.add(alphaSpin);
    horizBox.add(betaSpin);
    alphaSpin.setVisible(dialog.isOpenShell());
    betaSpin.setVisible(dialog.isOpenShell());
    horizBox.add(dialog.new HelpBtn("view_orbital_help.htm"));
    return horizBox;
  }

  private Component createOrbitalPanel() {
    JPanel orbPanel = new JPanel(new BorderLayout());
    orbPanel.setBorder(BorderFactory.createLineBorder(Color.black));
    orbScroll = new JScrollPane();
    orbScroll.setMaximumSize(new Dimension(355, 400));
    orbScroll.getViewport().setMinimumSize(new Dimension(250, 400));
    orbScroll
        .setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    orbPanel.add(orbScroll, BorderLayout.CENTER);

    orbPanel.setAlignmentX(0.0f);
    orbPanel.add(new JLabel(
        "click to turn up to 9 orbitals on/off; hold to reverse phase"),
        BorderLayout.SOUTH);
    newOrbitals();
    return orbPanel;
  }

  protected void doGoPressed() {
    int n = orbitals.bsOn.cardinality();
    if (n > 9) {
      vwr.alert("More than 9 orbitals are selected!");
      return;
    }
    if (orbitals.bsOn.isEmpty()) {
      vwr.alert("Pick an orbital to plot.");
      return;
    }
    initializeImage();
    if (profileBtn.isSelected()) {
      createImage1or2D(true);
    } else if (contourBtn.isSelected()) {
      createImage1or2D(false);
    } else if (viewBtn.isSelected())
      createImage3D();
  }

  protected void doSetSpin(String type) {
    if (type != null) {
      dialog.doSetStructure(type);
    }
    if (NBOConfig.nboView) {
      dialog.runScriptQueued("select *;color bonds lightgrey");
    }
    doSetNewBasis(false, false);
  }

  /**
   * Check to see if we have a given plot file and that it is not zero length.
   * If we don't, run gennbo (if available) using the PLOT keyword and return
   * null.
   * 
   * @param fileNum
   *        31-39 or 0 to set to the current value of comboBasis
   * @return a new File object or null
   */
  private File ensurePlotFile(int fileNum) {
    if (fileNum == 0)
      fileNum = 31 + comboBasis1.getSelectedIndex();
    File f = dialog.inputFileHandler.newNBOFileForExt("" + fileNum);
    if (!f.exists() || f.length() == 0) {
      dialog.runPanel.doRunGenNBOJob("PLOT");
      return null;
    }
    return f;
  }

  /**
   * Plane dialog
   */
  protected void doViewPlane() {
    viewPlanePt = 0;
    dialog.runScriptQueued("set bondpicking false");
    viewState = VIEW_STATE_PLANE;
    Box box = NBOUtil.createTitleBox(" Definiton of Plane ", null);
    final JPanel plane = new JPanel(new BorderLayout());
    JPanel labs = new JPanel(new GridLayout(6, 1, 5, 0));
    labs.add(new JLabel("Enter fraction to locate origin:"));
    labs.add(new JLabel("Enter two rotation angles:"));
    labs.add(new JLabel("Enter shift of plane along normal:"));
    labs.add(new JLabel("Enter min and max X values:"));
    labs.add(new JLabel("Enter min and max Y values:"));
    labs.add(new JLabel("Enter number of steps NX:"));
    plane.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(6, 1, 5, 0));
    in.add(planeFields[3]);
    Box bo = Box.createHorizontalBox();
    bo.add(planeFields[4]);
    bo.add(planeFields[5]);
    in.add(bo);
    in.add(planeFields[6]);
    bo = Box.createHorizontalBox();
    bo.add(planeFields[7]);
    bo.add(planeFields[8]);
    in.add(bo);
    bo = Box.createHorizontalBox();
    bo.add(planeFields[9]);
    bo.add(planeFields[10]);
    in.add(bo);
    in.add(planeFields[11]);
    plane.add(in, BorderLayout.CENTER);
    JButton b = new JButton("OK");
    plane.add(b, BorderLayout.SOUTH);
    Box box2 = Box.createVerticalBox();
    box2.setBorder(BorderFactory.createLineBorder(Color.black));
    box2.add(plane);
    box2.setAlignmentX(0.0f);
    box2.setMaximumSize(new Dimension(355, 250));
    box.add(box2);
    final JDialog d = new JDialog(dialog, "Vector definition");
    d.setSize(new Dimension(300, 300));
    d.setVisible(true);
    d.add(box);
    centerDialog(d, 175);
    showSelected(planeFields);
    d.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        dialog.runScriptQueued("select off;set bondpicking true");
        viewState = VIEW_STATE_MAIN;
      }
    });
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        d.dispose();
        dialog.runScriptQueued("select off;set bondpicking true");
        viewState = VIEW_STATE_MAIN;
      }
    });

    plane.setVisible(true);
  }

  private void showSelected(JTextField[] t) {
    String s = "";
    for (int i = t.length; --i >= 0;)
      s += " " + t[i].getText();
    dialog.showSelected(s);
  }

  /**
   * Vector dialog
   */
  protected void doViewAxis() {
    dialog.runScriptQueued("set bondpicking false");
    viewState = VIEW_STATE_VECTOR;
    viewVectorPt = 0;
    Box box = NBOUtil.createTitleBox(" Vector Definition ", null);
    JPanel vect = new JPanel(new BorderLayout());
    JPanel labs = new JPanel(new GridLayout(4, 1, 4, 0));
    //    labs.add(new JLabel("Enter or select two atom numbers:"));
    labs.add(new JLabel("Enter fraction to locate origin:"));
    labs.add(new JLabel("Enter min and max X values:"));
    labs.add(new JLabel("Enter min and max function values:"));
    labs.add(new JLabel("Enter number of steps NX:"));
    vect.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(4, 1, 4, 0));
    //    Box bo = Box.createHorizontalBox();
    //    bo.add(vectorFields[0]);
    //    bo.add(vectorFields[1]);
    //    in.add(bo);
    in.add(vectorFields[2]);
    Box bo = Box.createHorizontalBox();
    bo.add(vectorFields[3]);
    bo.add(vectorFields[4]);
    in.add(bo);
    bo = Box.createHorizontalBox();
    bo.add(vectorFields[5]);
    bo.add(vectorFields[6]);
    in.add(bo);
    in.add(vectorFields[7]);
    vect.add(in, BorderLayout.CENTER);

    JButton b = new JButton("OK");
    Box box2 = Box.createVerticalBox();
    vect.add(b, BorderLayout.SOUTH);
    vect.setAlignmentX(0.0f);
    box2.setAlignmentX(0.0f);
    box.setAlignmentX(0.0f);
    box2.setBorder(BorderFactory.createLineBorder(Color.black));
    box2.add(vect);
    box2.setMaximumSize(new Dimension(355, 250));
    box.add(box2);

    final JDialog d = new JDialog(dialog, "Vector definition");
    d.setSize(new Dimension(300, 300));
    d.setVisible(true);
    d.add(box);
    centerDialog(d, 150);
    showSelected(vectorFields);
    d.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        dialog.runScriptQueued("select off;set bondpicking true");
        viewState = VIEW_STATE_MAIN;
      }
    });
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        d.dispose();
        dialog.runScriptQueued("select off;set bondpicking true");
        viewState = VIEW_STATE_MAIN;
      }
    });
  }

  /**
   * Vector dialog
   */
  protected void doViewLines() {
    Box box = NBOUtil.createTitleBox(" Contour lines ", null);
    JPanel lines = new JPanel(new BorderLayout());
    JPanel labs = new JPanel(new GridLayout(5, 1, 5, 0));
    labs.add(new JLabel("Enter first contour line:"));
    labs.add(new JLabel("Enter contour step size:"));
    labs.add(new JLabel("Enter number of contours:"));
    labs.add(new JLabel("Enter length of dash (cm):"));
    labs.add(new JLabel("Enter length of space (cm):"));
    lines.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(5, 1, 5, 0));
    in.add(lineFields[0]);
    in.add(lineFields[1]);

    in.add(lineFields[2]);
    in.add(lineFields[3]);
    in.add(lineFields[4]);
    lines.add(in, BorderLayout.CENTER);
    Box box2 = Box.createVerticalBox();

    box2.setBorder(BorderFactory.createLineBorder(Color.black));
    box2.add(lines);
    box.add(box2);
    box2.setAlignmentX(0.0f);

    final JDialog d = new JDialog(dialog, "Line settings");
    d.setSize(new Dimension(300, 300));
    d.setVisible(true);
    d.add(box);

    box = NBOUtil.createTitleBox(" Orbital diagram lines ", null);

    lines = new JPanel(new BorderLayout());
    labs = new JPanel(new GridLayout(2, 1, 5, 0));
    labs.add(new JLabel("Enter length of dash (cm):"));
    labs.add(new JLabel("Enter length of space (cm):"));
    lines.add(labs, BorderLayout.WEST);
    in = new JPanel(new GridLayout(2, 1, 5, 0));

    in.add(lineFields[5]);
    in.add(lineFields[6]);
    lines.add(in, BorderLayout.CENTER);
    box2 = Box.createVerticalBox();
    box2.setAlignmentX(0.0f);
    box2.setBorder(BorderFactory.createLineBorder(Color.black));
    box2.add(lines);
    box.add(box2);
    JButton b = new JButton("OK");
    lines.add(b, BorderLayout.SOUTH);
    d.add(box, BorderLayout.SOUTH);
    centerDialog(d, 150);

    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        d.dispose();
        viewState = VIEW_STATE_MAIN;
      }
    });
    lines.setVisible(true);
  }

  private void centerDialog(JDialog d, int h) {
    int x = (dialog.getX() + dialog.getWidth()) / 2 + 150;
    int y = (dialog.getY() + dialog.getHeight()) / 2 - h;
    d.setLocation(x, y);
  }

  /**
   * Camera settings panel
   */
  protected void doCam1() {
    viewState = VIEW_STATE_CAMERA;
    JPanel panel = new JPanel();
    JPanel cam1 = new JPanel();//(dialog, "Camera and Light-Source:", false);
    cam1.setLayout(new BorderLayout());
    cam1.setMinimumSize(new Dimension(350, 200));
    cam1.setVisible(true);
    //centerDialog(cam1);
    cam1.setBorder(BorderFactory.createLineBorder(Color.black));
    Box box = NBOUtil.createTitleBox(" Camera and Light-Source ", null);
    JPanel labs = new JPanel(new GridLayout(5, 1, 5, 0));
    labs.add(new JLabel("Bounding sphere radius"));
    labs.add(new JLabel("Camera distance from screen center:"));
    labs.add(new JLabel("Two rotation angles (about X, Y):"));
    labs.add(new JLabel("Camera view angle:"));
    labs.add(new JLabel("Lighting (RL, UD, BF w.r.t. camera):"));
    cam1.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(5, 1, 5, 0));
    Box bo = Box.createHorizontalBox();
    in.add(camFields[52]);
    in.add(camFields[0]);
    bo.add(camFields[1]);
    bo.add(camFields[2]);
    in.add(bo);
    in.add(camFields[3]);
    bo = Box.createHorizontalBox();
    bo.add(camFields[4]);
    bo.add(camFields[5]);
    bo.add(camFields[6]);
    in.add(bo);
    cam1.add(in, BorderLayout.CENTER);
    cam1.setAlignmentX(0.0f);
    box.add(cam1);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.add(box);
    cam2(panel);
    cam3(panel);
    cam4(panel);
    cam5(panel);
    JScrollPane sp = new JScrollPane();
    sp.setMaximumSize(new Dimension(350, 500));
    sp.getViewport().add(panel);

    final JDialog d = new JDialog(dialog, "Camera parameters");
    d.setSize(new Dimension(360, 500));
    d.setVisible(true);
    d.add(sp, BorderLayout.CENTER);
    centerDialog(d, 250);
    JButton b = new JButton("OK");
    d.add(b, BorderLayout.SOUTH);
    b.setAlignmentX(0.0f);
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        d.dispose();
      }
    });
  }

  private void cam2(JPanel panel) {
    Box box = NBOUtil.createTitleBox(" Surface Optical Parameters: ", null);
    JPanel cam2 = new JPanel(new BorderLayout());
    cam2.setBorder(BorderFactory.createLineBorder(Color.black));
    JPanel labs = new JPanel(new GridLayout(4, 1, 5, 0));
    labs.add(new JLabel("atoms:"));
    labs.add(new JLabel("bonds:"));
    labs.add(new JLabel("H-bonds:"));
    labs.add(new JLabel("orbitals:"));
    cam2.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(4, 1, 5, 0));
    Box bo = Box.createHorizontalBox();
    bo.add(camFields[7]);
    bo.add(camFields[8]);
    bo.add(camFields[9]);
    bo.add(camFields[10]);
    bo.add(camFields[11]);
    in.add(bo);
    bo = Box.createHorizontalBox();
    bo.add(camFields[12]);
    bo.add(camFields[13]);
    bo.add(camFields[14]);
    bo.add(camFields[15]);
    bo.add(camFields[16]);
    in.add(bo);
    bo = Box.createHorizontalBox();
    bo.add(camFields[17]);
    bo.add(camFields[18]);
    bo.add(camFields[19]);
    bo.add(camFields[20]);
    bo.add(camFields[21]);
    in.add(bo);
    bo = Box.createHorizontalBox();
    bo.add(camFields[22]);
    bo.add(camFields[23]);
    bo.add(camFields[24]);
    bo.add(camFields[25]);
    bo.add(camFields[26]);
    in.add(bo);
    cam2.add(in, BorderLayout.CENTER);
    cam2.add(
        new JLabel(
            "                    amb              diff           spec        pow        transp"),
        BorderLayout.NORTH);
    //JButton b = new JButton("OK");
    //cam2.add(b, BorderLayout.SOUTH);
    cam2.setAlignmentX(0.0f);
    box.add(cam2);
    panel.add(box);
    //    b.addActionListener(new ActionListener() {
    //      @Override
    //      public void actionPerformed(ActionEvent e) {
    //        cam2.dispose();
    //        cam3();
    //      }
    //    });
  }

  private void cam3(JPanel panel) {
    JPanel cam3 = new JPanel(new BorderLayout());
    Box box = NBOUtil.createTitleBox(" Color (Blue/Green/Red) Parameters: ",
        null);
    cam3.setBorder(BorderFactory.createLineBorder(Color.black));
    JPanel labs = new JPanel(new GridLayout(6, 1, 5, 0));
    labs.add(new JLabel("light source color:"));
    labs.add(new JLabel("background color:"));
    labs.add(new JLabel("orbital (+ phase) color:"));
    labs.add(new JLabel("orbital (- phase) color:"));
    labs.add(new JLabel("bond color"));
    labs.add(new JLabel("H-Bond color"));
    cam3.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(6, 1, 5, 0));

    Box bo = Box.createHorizontalBox();
    bo.add(camFields[27]);
    bo.add(camFields[28]);
    bo.add(camFields[29]);
    in.add(bo);

    bo = Box.createHorizontalBox();
    bo.add(camFields[30]);
    bo.add(camFields[31]);
    bo.add(camFields[32]);
    in.add(bo);

    bo = Box.createHorizontalBox();
    bo.add(camFields[33]);
    bo.add(camFields[34]);
    bo.add(camFields[35]);
    in.add(bo);

    bo = Box.createHorizontalBox();
    bo.add(camFields[36]);
    bo.add(camFields[37]);
    bo.add(camFields[38]);
    in.add(bo);

    bo = Box.createHorizontalBox();
    bo.add(camFields[39]);
    bo.add(camFields[40]);
    bo.add(camFields[41]);
    in.add(bo);

    bo = Box.createHorizontalBox();
    bo.add(camFields[42]);
    bo.add(camFields[43]);
    bo.add(camFields[44]);
    in.add(bo);

    cam3.add(in, BorderLayout.CENTER);
    cam3.add(
        new JLabel(
            "                                                 Blue               Green             Red"),
        BorderLayout.NORTH);
    cam3.setAlignmentX(0.0f);
    box.add(cam3);
    panel.add(box);

  }

  private void cam4(JPanel panel) {
    JPanel cam4 = new JPanel(new BorderLayout());
    Box box = NBOUtil.createTitleBox(" Atomic and Bond Radii: ", null);
    cam4.setBorder(BorderFactory.createLineBorder(Color.black));
    JPanel labs = new JPanel(new GridLayout(4, 1, 5, 0));
    labs.add(new JLabel("Atomic radius for H:"));
    labs.add(new JLabel("Atomic radius for C:"));
    labs.add(new JLabel("Bond radius:"));
    labs.add(new JLabel("H-bond radius:"));
    cam4.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(4, 1, 5, 0));
    in.add(camFields[45]);
    in.add(camFields[46]);
    in.add(camFields[47]);
    in.add(camFields[48]);
    cam4.add(in, BorderLayout.CENTER);
    cam4.setAlignmentX(0.0f);
    box.add(cam4);
    panel.add(box);
  }

  private void cam5(JPanel panel) {
    JPanel cam5 = new JPanel(new BorderLayout());
    Box box = NBOUtil.createTitleBox(" Contour Parameters: ", null);
    cam5.setBorder(BorderFactory.createLineBorder(Color.black));
    JPanel labs = new JPanel(new GridLayout(3, 1, 5, 0));
    labs.add(new JLabel("Contour value:"));
    labs.add(new JLabel("Contour tolerance:"));
    labs.add(new JLabel("Stepsize:"));
    cam5.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(3, 1, 5, 0));
    in.add(camFields[49]);
    in.add(camFields[50]);
    in.add(camFields[51]);
    cam5.add(in, BorderLayout.CENTER);
    cam5.setAlignmentX(0.0f);
    box.add(cam5);
    panel.add(box);
  }

  //////////////////////// general methods ////////////////////

  private void newOrbitals() {
    if (orbitals != null) {
      orbitals.removeListSelectionListener(orbitals);
      orbitals.removeMouseListener(orbitals);
      orbScroll.getViewport().remove(orbitals);
    }
    orbScroll.getViewport().add(orbitals = new OrbitalList());
  }

  private void updateViewSettings() {
    dialog.viewSettingsBox.removeAll();
    dialog.viewSettingsBox.setLayout(new BorderLayout());
    //Box top = Box.createVerticalBox();
    JLabel lab = new JLabel("Settings:");
    lab.setBackground(Color.black);
    lab.setForeground(Color.white);
    lab.setOpaque(true);
    lab.setFont(NBOConfig.nboFont);
    //top.add(lab);

    Box middle = Box.createVerticalBox();
    Box tmp = Box.createHorizontalBox();
    tmp.add(new JLabel("Orientation: "));
    atomOrient = new JRadioButton("Atoms");
    atomOrient.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        doViewByAtoms();
      }
    });
    tmp.add(atomOrient);

    final JRadioButton jmolOrient = new JRadioButton("Jmol");
    jmolOrient.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        doViewByJmol();
      }
    });
    tmp.add(jmolOrient);

    ButtonGroup bg = new ButtonGroup();
    bg.add(jmolOrient);
    bg.add(atomOrient);

    dialog.viewSettingsBox.add(lab, BorderLayout.NORTH);

    middle.add(tmp);

    tmp = Box.createHorizontalBox();
    final JButton btnVec = new JButton("Axis");
    final JButton btnPla = new JButton("Plane");
    final JButton btnLines = new JButton("Lines");

    btnVec.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doViewAxis();
      }
    });
    btnPla.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doViewPlane();
      }
    });
    btnLines.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doViewLines();
      }
    });

    btnPla.setMargin(null);
    tmp.add(btnVec);
    tmp.add(btnPla);
    tmp.add(btnLines);
    middle.add(tmp);
    dialog.viewSettingsBox.add(middle, BorderLayout.CENTER);

    final JButton btnCam = new JButton("Camera");
    btnCam.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doCam1();
      }
    });
    //set default VIEW orientation to Jmol orientation
    jmolOrient.setSelected(true);
    doViewByJmol();
    
    dialog.viewSettingsBox.add(btnCam, BorderLayout.SOUTH);
    dialog.repaint();
    dialog.revalidate();
  }

  protected void doViewByJmol() {
    //commented out by fzy. planeBox should be showed no matter what the view orientation is
//    planeBox.setVisible(false);
    jmolView = true;
  }

  protected void doViewByAtoms() {
    //commented out by fzy. planeBox should be showed no matter what the view orientation is
//    planeBox.setVisible(true);
    dialog.nboService.restartIfNecessary();
    setDefaultParameterArrays();
    jmolView = false;
  }

  protected File doSetNewBasis(boolean isFileLoading, boolean clearAlphaBeta) {

    if (clearAlphaBeta) {
      alphaList = betaList = null;
    }
    dialog.runScriptQueued("isosurface delete");
    resetCurrentOrbitalClicked();

    if (comboBasis1.getSelectedIndex() == BASIS_MO) {
      if (!dialog.runPanel.cleanNBOKeylist(
          dialog.inputFileHandler.read47File(false)[1], true).contains("CMO")) {
        dialog.runPanel.doRunGenNBOJob("CMO");
        return null;
      }
    }

    if (orbitals == null)
      return null;
    isNewModel = true;
    orbitals.removeAll();

    File f = ensurePlotFile(0);
    if (f == null) {
      // NBOServe is probably sending a job.
      return null;
    }

    boolean isBeta = dialog.isOpenShell() && !isAlphaSpin();

    DefaultListModel<String> list = (isBeta ? betaList : alphaList);
    if (list != null && list.size() > 0) {
      orbitals.setModelList(list, false);
      return null;
    }
    list = new DefaultListModel<String>();
    if (isBeta)
      betaList = list;
    else
      alphaList = list;

    if (isFileLoading) {
      return f;
    }
    
    dialog.logCmd("select " + comboBasis1.getSelectedItem() + " "
        + (isBeta ? "beta" : dialog.isOpenShell() ? "alpha" : ""));
    int ibasis = comboBasis1.getSelectedIndex();
    String[] tokens = this.getLabelSet(ibasis, isBeta);
    if (tokens != null) {
      orbitals.loadList(tokens, list);
      return f;
    }
    if (dialog.jmolOptionNONBO) 
      return f;
    postNBO_v(NBOUtil.postAddCmd(getMetaHeader(true), "LABEL"),
       MODE_VIEW_LIST, getIbasKey(ibasis, isBeta), list, "Getting list", null, null);
    return null;
  }

  /////////////////////// RAW NBOSERVE API ////////////////////

  /**
   * get the standard header for a set of META commands, specifically C_PATH and
   * C_JOBSTEM and I_SPIN; possibly I_BAS_1
   * 
   * @param addBasis
   *        if desired, from comboBasis
   * 
   * @return a new string buffer using javajs.util.SB
   * 
   */
  protected SB getMetaHeader(boolean addBasis) {
    SB sb = new SB();
    NBOUtil.postAddGlobalC(sb, "PATH",
        dialog.inputFileHandler.inputFile.getParent());
    NBOUtil.postAddGlobalC(sb, "JOBSTEM", dialog.inputFileHandler.jobStem);
    if (addBasis)
      NBOUtil.postAddGlobalI(sb, "BAS_1", 1, comboBasis1);
    NBOUtil.postAddGlobalI(sb, "SPIN",
        (!dialog.isOpenShell() ? 0 : isAlphaSpin() ? 1 : -1), null);
    return sb;
  }

  /**
   * add in camera parameters
   * 
   * @param sb
   */
  private void appendCameraParams(SB sb) {
    int n = camFields.length;
    for (int i = 0; i < n; i++) {
//      //if (!camFields[i].getText().equals(camVal[i])) {
//        camVal[i] = camFields[i].getText();
        NBOUtil.postAddGlobalT(sb, "CAMERA_" + camFieldIDs[i], camFields[i]);
      }
  }

  /**
   * add in the SIGN parameter
   * 
   * @param sb
   * @param i
   */
  private void appendOrbitalPhaseSign(SB sb, int i) {
    NBOUtil.postAddGlobal(sb, "SIGN", (orbitals.bsNeg.get(i) ? "-1" : "+1"));
  }

  //contour/profile selected orbital in orbital list
  protected void createImage1or2D(boolean oneD) {

    if (jmolView)
      setJmolView(true);
    else
      setAtomsView();

    if (orbitals.bsOn.cardinality() > 1) {
      createImage1or2DMultiple(oneD);
      return;
    }

    // ? needed ? sendJmolOrientation();

    SB sb = getMetaHeader(true);

    int ind = orbitals.bsOn.nextSetBit(0);

    appendOrbitalPhaseSign(sb, ind);
    appendLineParams(sb);
    if (oneD) {
      appendVectorParams(sb);
    } else {
      appendPlaneParams(sb);
    }
    String cmd = (oneD ? "Profile " : "Contour ") + (ind + 1);
    dialog.logCmd(cmd);
    NBOUtil.postAddCmd(sb, cmd);
    postNBO_v(sb, MODE_VIEW_IMAGE, -1, null,
        (oneD ? "Profiling.." : "Contouring.."), null, null);
  }

  private void appendLineParams(SB sb) {
    for (int i = 0; i < lineFields.length; i++)
      NBOUtil.postAddGlobal(sb, "LINES_" + (char) ('a' + i), lineVal[i] = lineFields[i].getText());
  }

  private void appendVectorParams(SB sb) {
    for (int i = 0; i < vectorFields.length; i++)
      NBOUtil.postAddGlobal(sb, "VECTOR_" + (char) ('a' + i), vecVal[i] = vectorFields[i].getText());
  }

  private void appendPlaneParams(SB sb) {
    for (int i = 0; i < planeFields.length; i++)
       NBOUtil.postAddGlobal(sb, "PLANE_" + (char) ('a' + i), plVal[i] = planeFields[i].getText());
  }

  private void setJmolView(boolean is2D) {

    String key = (is2D ? "a U" : "a V_U");

    SB sb = new SB();
    for (int i = 1; i <= 3; i++) {
      String tmp2 = "";
      for (int j = 1; j <= 3; j++) {
        Object oi = vwr.getProperty("string", "orientationInfo.rotationMatrix["
            + j + "][" + i + "]", null);
        tmp2 += oi.toString() + " ";
      }
      sb.append(key + i + " " + tmp2 + NBOUtil.sep);
    }

    // I do not understand why a LABEL command has to be given here. A bug? 

    postNBO_v(NBOUtil.postAddCmd(getMetaHeader(true), "LABEL"),
        NBOService.MODE_RAW, -1, null, "", "jview.txt", sb.toString());

    postNBO_v(NBOUtil.postAddCmd(new SB(), "JVIEW"), NBOService.MODE_RAW, -1, null,
        "Sending Jmol orientation", null, null);

    //    sb = getMetaHeader(true);
    //    sb.append("CMD LABEL");
    //    nboService.rawCmdNew("v", sb, MODE_RAW, null, "");

  }
  
  private void setAtomsView()
  {
    postNBO_v(NBOUtil.postAddCmd(getMetaHeader(true), "LABEL"),
        NBOService.MODE_RAW, -1, null, "", null,null);
    
    postNBO_v(NBOUtil.postAddCmd(new SB(), "AVIEW"), NBOService.MODE_RAW, -1, null,
        "Sending Jmol orientation", null, null);
  }
  
  protected void createImage1or2DMultiple(boolean oneD) {

    //    sb = getMetaHeader(true);
    //    sb.append("CMD LABEL");
    //    nboService.rawCmdNew("v", sb, MODE_RAW, null, "");
    //    

    SB sb = new SB();
    String msg = (oneD) ? "Profile" : "Contour";
    String profileList = "";
    for (int pt = 0, i = orbitals.bsOn.nextSetBit(0); i >= 0; i = orbitals.bsOn
        .nextSetBit(i + 1)) {
      sb = getMetaHeader(true);
      appendOrbitalPhaseSign(sb, i);
      NBOUtil.postAddCmd(sb, (oneD ? "PROFILE " : "CONTOUR ") + (i + 1));
      msg += " " + (i + 1);
      profileList += " " + (++pt);
      postNBO_v(sb, NBOService.MODE_RAW, -1, null, "Sending " + msg, null, null);
    }
    dialog.logCmd(msg);
    sb = getMetaHeader(false);
    appendLineParams(sb);
    NBOUtil.postAddCmd(sb, "DRAW" + profileList);
    postNBO_v(sb, MODE_VIEW_IMAGE, -1, null, "Drawing...", null, null);
  }

  protected void createImage3D() {
    if(jmolView)
    {
      postNBO_v(NBOUtil.postAddCmd(getMetaHeader(true), "LABEL"),
          NBOService.MODE_RAW, -1, null, "", null, null);
      
      postNBO_v(NBOUtil.postAddCmd(new SB(), "JVIEW"), NBOService.MODE_RAW, -1, null,
          "Sending Jmol orientation", null, null);
    }
    else
    {
      setAtomsView();
    }
    
    SB sb = new SB();
    String tmp = "View";
    String list = "";
    BS bs = orbitals.bsOn;
    for (int pt = 0, i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      sb = getMetaHeader(true);
      appendOrbitalPhaseSign(sb, i);
      NBOUtil.postAddCmd(sb, "PROFILE " + (i + 1));
      postNBO_v(sb, NBOService.MODE_RAW, -1, null, "Sending profile " + (i + 1),
          null, null);
      tmp += " " + (i + 1);
      list += " " + (++pt);
    }
    dialog.logCmd(tmp);
    String jviewData = sb.toString();
    // BH: It turns out it is CRITICAL that if you send camera parameters, you MUST NOT SEND basis information.
    // (no idea why!)
    sb = getMetaHeader(false);
    appendCameraParams(sb);
    NBOUtil.postAddCmd(sb, "VIEW" + list);
    postNBO_v(sb, MODE_VIEW_IMAGE, -1, null, "Raytracing...", null, jviewData);
  }

  private void initializeImage() {
    //testingView = NBOConfig.debugVerbose;
    dialog.runScriptQueued("image close");
    dialog.nboService.restart();
    setDefaultParameterArrays();
    if (jmolView)
      setJmolView(false);
  }

  private int viewVectorPt = 0;
  private int viewPlanePt = 0;

  /**
   * callback notification from Jmol
   * 
   * @param picked
   *        [atomno1, atomno2] or [atomno1, Integer.MIN_VALUE]
   * 
   */
  protected void notifyPick_v(int[] picked) {
    dialog.runScriptQueued("isosurface delete");
    int at1 = picked[0];
    int at2 = picked[1];
    switch (viewState) {
    case VIEW_STATE_VECTOR:
      if (at2 != Integer.MIN_VALUE)
        return;
      vectorFields[viewVectorPt++].setText("" + at1);
      showSelected(vectorFields);
      viewVectorPt = viewVectorPt % 2;
      break;
    case VIEW_STATE_PLANE:
      if (at2 != Integer.MIN_VALUE)
        return;
      planeFields[viewPlanePt++].setText("" + at1);
      showSelected(planeFields);
      viewPlanePt = viewPlanePt % 3;
      break;
    case VIEW_STATE_MAIN:
      if (at2 == Integer.MIN_VALUE) {
        showOrbital(nextOrbitalForAtomPick(at1,
            (DefaultListModel<String>) orbitals.getModel()));
        return;
      }
      switch (comboBasis1.getSelectedIndex()) {
      case BASIS_AO:
      case BASIS_PNAO:
      case BASIS_NAO:
      case BASIS_MO:
        break;
      case BASIS_PNHO:
      case BASIS_NHO: {
        String sat1 = vwr.ms.at[at1 - 1].getElementSymbol() + at1;
        String sat2 = vwr.ms.at[at2 - 1].getElementSymbol() + at2;
        showOrbital(nextOrbitalForBondPick(sat1 + "(" + sat2 + ")", sat2 + "("
            + at1 + ")"));
      }
        break;
      case BASIS_PNBO:
      case BASIS_NBO:
      case BASIS_PNLMO:
      case BASIS_NLMO: {
        String sat1 = vwr.ms.at[at1 - 1].getElementSymbol() + at1;
        String sat2 = vwr.ms.at[at2 - 1].getElementSymbol() + at2;
        showOrbital(nextOrbitalForBondPick(sat1 + "-" + sat2, sat2 + "-" + sat1));
      }
        break;
      }
    }
  }

  private boolean includeRydberg = false; // BH thinking this is not necessary

  /**
   * Determine the next orbital in the cycle for bond picking
   * 
   * @param atomno
   * @param list
   * @return the new index
   */
  protected int nextOrbitalForAtomPick(int atomno,
                                       AbstractListModel<String> list) {
    String at = vwr.ms.at[atomno - 1].getElementSymbol() + atomno + "(";
    int curr = (currOrb.contains(at) ? currOrbIndex : -1);
    for (int i = curr + 1, size = list.getSize(); i < size + curr; i++) {
      int ipt = i % size;
      String str = list.getElementAt(ipt).replaceAll(" ", "");
      if (str.contains(at + "lp)") || str.contains(at + "lv)")
          || includeRydberg && str.contains(at + "ry)")) {
        orbitals.setSelectedIndex(ipt);
        currOrb = str;
        currOrbIndex = ipt;
        return ipt;
      }
    }
    return curr;
  }

  /**
   * check for the next orbital upon bond clicking
   * 
   * @param b1
   * @param b2
   * @return the next orbital index, or -1
   */
  private int nextOrbitalForBondPick(String b1, String b2) {
    int size = orbitals.getModel().getSize();
    int curr = (currOrb.contains(b1) ? currOrbIndex : -1);
    for (int i = curr + 1; i < size + curr; i++) {
      int ipt = i % size;
      String listOrb = orbitals.getModel().getElementAt(ipt);
      String str = listOrb.replace(" ", "");
      if (str.contains(b1) || b2 != null && str.contains(b2)) {
        orbitals.setSelectedIndex(ipt);
        currOrb = str;
        currOrbIndex = ipt;
        return ipt;
      }
    }
    return curr;
  }

  protected void resetCurrentOrbitalClicked() {
    currOrb = "";
    currOrbIndex = -1;
  }

  protected void showOrbital(int i) {
    if (i < 0)
      return;
    orbitals.bsOn.clearAll();
    orbitals.bsNeg.clearAll();
    dialog.runScriptQueued("isosurface * off");
    orbitals.updateIsosurfacesInJmol(i);
  }

  protected void notifyList(AbstractListModel<String> list) {
    if (list != null) {
      orbitals.setLayoutOrientation(JList.VERTICAL_WRAP);
      orbitals.requestFocus();
    }
  }

  @SuppressWarnings("unchecked")
  protected void notifyFileLoaded_v() {
    if (vwr.ms.ac == 0)
      return;
    clearLabelSet();
    video.setVisible(true);
    if(videoDialog!=null)
      videoDialog.dispose();
    centerBox.setVisible(true);
    bottomBox.setVisible(!dialog.jmolOptionNONBO);

    //OLD

    //    if (!newModel) {
    //      String frame = (startingModelCount + modelCount - 1) + ".1";
    //      dialog.runScriptQueued("frame " + frame + ";refesh");
    //      colorMeshes();
    //      dialog.runScriptQueued("var b = {visible}.bonds;select bonds @b;wireframe 0;refresh");
    //      return;
    //    }

    //<--

    // retrieve model from currently loaded model

    Map<String, Object> moData = (Map<String, Object>) vwr
        .getCurrentModelAuxInfo().get("moData");
    String type = comboBasis1.getSelectedItem().toString();
    int ibas = comboBasis1.getSelectedIndex();
    if (type.charAt(0) == 'P')
      type = type.substring(1);
    boolean isBeta = dialog.isOpenShell() && !isAlphaSpin()
        && betaList != null;
    try {
      boolean needab = (getIbasKey(ibas, true) != getIbasKey(ibas, false));
      alphaSpin.setVisible(needab && dialog.isOpenShell()); // old
      betaSpin.setVisible(needab && dialog.isOpenShell()); // old

      setDefaultParameterArrays();

      for (int i = 0; i < planeFields.length; i++)
        planeFields[i] = new JTextField(plVal[i]);
      for (int i = 0; i < vectorFields.length; i++)
        vectorFields[i] = new JTextField(vecVal[i]);
      for (int i = 0; i < lineFields.length; i++)
        lineFields[i] = new JTextField(lineVal[i]);
      for (int i = 0; i < camFields.length; i++)
        camFields[i] = new JTextField(camVal[i]);

      vecBox.removeAll();
      vecBox.add(new JLabel("Axis: "));
      vecBox.add(vectorFields[0]);
      vecBox.add(vectorFields[1]);
      planeBox.removeAll();
      planeBox.add(new JLabel("Plane: "));
      planeBox.add(planeFields[0]);
      planeBox.add(planeFields[1]);
      planeBox.add(planeFields[2]);
      dialog.viewSettingsBox.setVisible(!dialog.jmolOptionNONBO);

      // set list
      DefaultListModel<String> list = (isBeta ? betaList : alphaList);
      if (dialog.jmolOptionNONBO) {
        if (type.startsWith("P"))
          type = type.substring(1);
        if (type.equalsIgnoreCase("NLMO"))
          type = "NBO";
        String[] a = ((Map<String, String[]>) moData.get("nboLabelMap"))
            .get((isBeta ? "beta_" : "") + type);
        list.clear();
        for (int i = 0; i < a.length; i++)
          list.addElement((i + 1) + ". " + a[i] + "   ");
      } else {
        doSetNewBasis(false, true);
      }
      
//      orbitals.setModelList(list, true);
    } catch (NullPointerException e) {
      //not a problem? dialog.log(e.getMessage() + " reading file", 'r');
      e.printStackTrace();
    }
    NBODialog.colorMeshes();
    //    showAtomNums(true);
    //    setBonds(true);

  }

  protected void loadNewFileIfAble() {
    dialog.nboService.restart();
    comboBasis1.setEnabled(false);
    if (comboBasis1.getSelectedIndex() != BASIS_MO)
      comboBasis1.setSelectedIndex(BASIS_PNBO);
    else
      comboBasis1.setSelectedIndex(BASIS_MO);
    comboBasis1.setEnabled(true);
    File f = doSetNewBasis(true, true);
    if (f == null)
      return;
    dialog.loadModelFileQueued(
        f,
        NBOUtil.pathWithoutExtension(f.getAbsolutePath()).equals(
            NBOUtil.pathWithoutExtension(dialog.getJmolFilename())));
  }

  protected void showLewisStructure() {
    dialog.doSetStructure(isAlphaSpin() ? "alpha" : "beta");
  }
  
  protected boolean isAlphaSpin() {
   return alphaSpin.isSelected() || !alphaSpin.isVisible();
  }


  /**
   * Indicate that we have a new model and clear all bit sets.
   * 
   * Called by NBOFileHandler.clearInputFile
   * 
   */
  protected void resetView() {
    isNewModel = true;
    orbitals.clearOrbitals(true);
    clearLabelSet();
  }

   class OrbitalList extends JList<String> implements ListSelectionListener,
      MouseListener, KeyListener {

    protected BS bsOn = new BS();
    protected BS bsNeg = new BS();
    protected BS bsKnown = new BS();

    public OrbitalList() {
      super();
      setLayoutOrientation(JList.VERTICAL_WRAP);
      setVisibleRowCount(-1); // indicates to automatically calculate max number of rows
      setFont(NBOConfig.nboFontLarge);
      //setColorScheme();
      setFont(NBOConfig.listFont);
      setModel(new DefaultListModel<String>() {
        @Override
        public void addElement(String s) {
          s += "   ";
          super.addElement(s);
        }
      });
      setCellRenderer(new ListCellRenderer<String>() {

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list,
                                                      String value, int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
          return renderCell(index);
        }
      });
      addListSelectionListener(this);
      addMouseListener(this);
    }

    public void loadList(String[] lines, DefaultListModel<String> list) {      
      list.clear();
      for (int i = 0; i < lines.length; i++)
        list.addElement(lines[i]);
      setModelList(list, true);
    }

    /**
     * @param list
     * @param isNew
     *        unused
     */
    void setModelList(DefaultListModel<String> list, boolean isNew) {
      setSelectedIndices(new int[0]);
      setModel(list);
      clearOrbitals(true);
      showLewisStructure();
      updateIsosurfacesInJmol(Integer.MIN_VALUE);
    }

    private JLabel cellLabel;
    protected boolean myTurn;
    protected boolean toggled;

    protected Component renderCell(int index) {
      if (cellLabel == null) {
        cellLabel = new JLabel() {
          @Override
          public void setBackground(Color bg) {
            if (myTurn)
              super.setBackground(bg);
          }

        };
        cellLabel.setFont(NBOConfig.listFont);
        //changed my fzy from 180 to 150 on the request of Frank to reduce the width per cell
        cellLabel.setMinimumSize(new Dimension(180, 20));
        cellLabel.setPreferredSize(new Dimension(180, 20));
        cellLabel.setMaximumSize(new Dimension(180, 20));
        cellLabel.setOpaque(true);
      }
      cellLabel.setText(getModel().getElementAt(index));
      myTurn = true;
      Color bgcolor = (!bsOn.get(index) ? Color.WHITE
          : bsNeg.get(index) ? NBOConfig.orbColor2 : NBOConfig.orbColor1);
      cellLabel.setBackground(bgcolor);
      cellLabel.setForeground(getContrastColor(bgcolor));
      myTurn = false;

      return cellLabel;
    }

    private Color getContrastColor(Color bgcolor) {
      return new AwtColor(C.getArgb(C.getBgContrast(bgcolor.getRGB())));
    }

    /**
     * Clear the orbital display bitsets.
     * 
     * @param clearAll
     */
    void clearOrbitals(boolean clearAll) {
      bsKnown.clearAll();
      if (clearAll) {
        bsOn.clearAll();
        bsNeg.clearAll();
      }
    }

    //    /**
    //     * 
    //     */
    //    protected void setLastOrbitalSelection() {
    //      updateIsosurfacesInJmol(-1);
    //    }
    //
    /**
     * Create, display, or hide a given orbital zero-based index. Optionally (in
     * the case of a key release) use the current selection set from Java but
     * otherwise use the bsON bit set.
     * 
     * 
     * @param iClicked
     *        negative: set the current list selection using BsON
     *        Integer.MAX_VALUE: set the current BsON from the list selection
     *        (keyboard drag, for example) otherwise: toggle the specified
     *        orbital index on or off
     * 
     */
    protected void updateIsosurfacesInJmol(int iClicked) {
      DefaultListModel<String> model = (DefaultListModel<String>) getModel();
      boolean isBeta = betaSpin.isSelected();
      String type = comboBasis1.getSelectedItem().toString();
      String script = "select 1.1;";
      if (iClicked == Integer.MAX_VALUE)
        script += updateBitSetFromModel();
      else
        updateModelFromBitSet();
      //System.out.println("update " + bsOn + " " + bsKnown + " " +  iClicked);
      for (int i = 0, n = model.getSize(); i < n; i++) {
        boolean isOn = bsOn.get(i);
        if (i == iClicked || isOn && !bsKnown.get(i)
            || isSelectedIndex(i) != isOn) {
          String id = "mo" + i;
          if (!isOn || bsKnown.get(i)) {
            if (isOn && bsNeg.get(i)) {
              bsKnown.clear(i);
              bsNeg.clear(i);
            }
            bsOn.setBitTo(i, isOn = !isOn);
          }
          boolean isKnown = bsKnown.get(i);
          if (!bsOn.get(i)) {
            // just turn it off
            script += "isosurface mo" + i + " off;";
          } else if (isKnown) {
            // just turn it on
            script += "isosurface mo" + i + " on;";
          } else {
            bsKnown.set(i);
            // create the isosurface
            script += NBOConfig.getJmolIsosurfaceScript(id, type, i + 1,
                isBeta, bsNeg.get(i));
          }
        }
//        if (bsOn.get(i)) {
//          dialog.logCmd("...orbital " + orbitals.getModel().getElementAt(i)
//              + (bsNeg.get(i) ? " [-]" : ""));
//        }
      }
      updateModelFromBitSet();
      dialog.runScriptQueued(script);
      //System.out.println("known" + bsKnown + " on" + bsOn + " neg" + bsNeg + " " + script);
    }

    private String updateBitSetFromModel() {
      int[] a = getSelectedIndices();
      BS bsModel = new BS();
      for (int i = a.length; --i >= 0;)
        bsModel.set(i);
      String script = "";
      for (int i = getModel().getSize(); --i >= 0;) {
        if (bsModel.get(i) != bsOn.get(i)) {
          if (bsOn.get(i)) {
            script += "isosurface mo" + i + " off;";
            bsOn.clear(i);
          } else {
            bsOn.set(i);
          }
        }
      }
      return script;
    }

    private void updateModelFromBitSet() {
      int[] a = new int[bsOn.cardinality()];
      for (int i = bsOn.nextSetBit(0), pt = 0; i >= 0; i = bsOn
          .nextSetBit(i + 1))
        a[pt++] = i;
      try {
        setSelectedIndices(a);
        //System.out.println("on neg " + bsOn + " " + bsNeg + " " + PT.toJSON(null,  a));
      } catch (Exception e) {
        System.out.println("render error " + e);
        // this is due to underlying list changing. Ignore
      }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
    }

    //    protected void setLabel(int i) {
    //      String label0 = getModel().getElementAt(i);
    //      int pt = label0.indexOf('[');
    //      if (pt > 0)
    //        label0 = label0.substring(0, pt);
    //      ((DefaultListModel<String>) getModel()).set(i, label0.trim() + "   ");
    //    }

    @Override
    public void mousePressed(MouseEvent e) {
      killMouseTimer();
      mouseTimer = getMouseTimer();

      //      System.out.println("PRESS" + e);
      //      System.out.println("press " + PT.toJSON(null, getSelectedIndices()) + e.getClickCount());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      //      System.out.println("RELEASE" + e);      
      //      System.out.println("release " + PT.toJSON(null, getSelectedIndices()));
      //boolean toggled = this.toggled;
      killMouseTimer();
      int i = getSelectedIndex();
      //        if (toggled)
      //          this.toggleOrbitalNegation(i);
      toggled = false;
      updateIsosurfacesInJmol(i);
    }

    private void killMouseTimer() {
      if (mouseTimer != null)
        mouseTimer.stop();
      mouseTimer = null;
      toggled = false;
    }

    private final static int DBLCLICK_THRESHOLD_MS = 300;

    private Timer mouseTimer;

    private Timer getMouseTimer() {
      Timer t = new Timer(DBLCLICK_THRESHOLD_MS, new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          doHoldTimerEvent();
        }
      });

      t.setRepeats(false);
      t.start();
      System.out.println("timer started");
      return t;

    }

    protected void doHoldTimerEvent() {
      int i = getSelectedIndex();
      if (bsOn.get(i)) {
        toggled = true;
        // toggle:
        bsNeg.setBitTo(i, !bsNeg.get(i));
        bsKnown.clear(i); // to - just switch colors?
        //        toggleOrbitalNegation(i);
        repaint();
      }

    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
      //      System.out.println("KEYDN" +  orbitals.getSelectedIndex() + " " + e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
      //      System.out.println("KEYUP" + orbitals.getSelectedIndex() + " " + e);
      updateIsosurfacesInJmol(Integer.MAX_VALUE);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

  }

  /**
   * Post a request to NBOServe with a callback to processNBO_s.
   * 
   * @param sb
   *        command data
   * @param mode
   *        type of request
   * @param ikey 
   * @param list
   *        optional list to fill
   * @param statusMessage
   * @param dataFileName
   *        optional
   * @param fileData
   *        optional
   */
  private void postNBO_v(SB sb, final int mode, final int ikey,
                         final DefaultListModel<String> list,
                         String statusMessage, String dataFileName,
                         String fileData) {
    final NBORequest req = new NBORequest();
    req.set(new Runnable() {
      @Override
      public void run() {
        processNBO_v(req, mode, ikey, list);
      }
    }, false, statusMessage, "v_cmd.txt", sb.toString(), dataFileName, fileData);
    //Added by fzy
    if(mode==MODE_VIEW_VIDEO)
      req.isVideoCreate=true;
    dialog.nboService.postToNBO(req);
  }

  /**
   * Process the reply from NBOServe.
   * 
   * 
   * @param req
   * @param mode
   * @param ikey 
   * @param list
   */
  protected void processNBO_v(NBORequest req, int mode, int ikey,
                              DefaultListModel<String> list) {
    String[] lines = req.getReplyLines();
    switch (mode) {
    case MODE_VIEW_LIST:
      addBasisLabel(ikey, lines);
      orbitals.loadList(lines, list);
      break;
    case MODE_VIEW_IMAGE:
      String fname = dialog.inputFileHandler.inputFile.getParent() + "\\"
          + dialog.inputFileHandler.jobStem + ".bmp";
      File f = new File(fname);
      final SB title = new SB();
      String id = "id " + PT.esc(title.toString().trim());
      String script = "image " + id + " close;image id \"\" "
          + PT.esc(f.toString().replace('\\', '/'));
      dialog.runScriptQueued(script);
      break;
    case MODE_VIEW_VIDEO:
      break;
    case NBOService.MODE_RAW:
      break;
    }
  }
  
 
}



/*
 * Reference to https://stackoverflow.com/questions/16649620/is-there-a-way-to-create-one-gif-image-from-multiple-images-in-java
 */
class GIFWriter
{
  private ImageWriter gifWriter;
  private ImageWriteParam imageWriteParam;
  private IIOMetadata imageMetadata;
  
  /**
   * Creates a new GifSequenceWriter
   * 
   * @param outputStream the ImageOutputStream to be written to
   * @param imageType one of the imageTypes specified in BufferedImage
   * @param timeBetweenFramesInMS the time between frames in miliseconds
   * @param loopContinuous indicates if the gif should loop repeatedly
   * @throws IIOException if no gif ImageWriters are found
   */
  public GIFWriter(ImageOutputStream outputStream, int imageType, int timeBetweenFramesInMS, boolean loopContinuous) throws IIOException, IOException
  {
    int loop;
    gifWriter=getWriter();
    imageWriteParam=gifWriter.getDefaultWriteParam();
    ImageTypeSpecifier imageTypeSpecifier=ImageTypeSpecifier.createFromBufferedImageType(imageType);
    imageMetadata=gifWriter.getDefaultImageMetadata(imageTypeSpecifier, imageWriteParam);
    
    String metadataFormatName=imageMetadata.getNativeMetadataFormatName();
    IIOMetadataNode root=(IIOMetadataNode)imageMetadata.getAsTree(metadataFormatName);
    IIOMetadataNode graphicsControlExtensionNode=getNode(root,"GraphicControlExtension");
    
    graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
    graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
    graphicsControlExtensionNode.setAttribute("transparentColorFlag","FALSE");
    graphicsControlExtensionNode.setAttribute("delayTime",Integer.toString(timeBetweenFramesInMS/10));
    graphicsControlExtensionNode.setAttribute("transparentColorIndex","0");
    
    IIOMetadataNode appExtensionsNode = getNode(root,"ApplicationExtensions");
    IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");

    child.setAttribute("applicationID", "NETSCAPE");
    child.setAttribute("authenticationCode", "2.0");

    if(loopContinuous)
      loop=0;
     else
      loop=1;
    
    child.setUserObject( new byte[] { 0x1, (byte) (loop & 0xFF), (byte)((loop >> 8) & 0xFF)} );
    appExtensionsNode.appendChild(child);
    
    imageMetadata.setFromTree(metadataFormatName, root);
    
    gifWriter.setOutput(outputStream);
    gifWriter.prepareWriteSequence(null);
  }
  
  /*
   * This method returns GIF Image Writer using ImageIO.getImageWritersBySuffix("gif")
   * 
   * @return: GIF ImageWriter
   * @throws IIOException if error finding available GIF image writers
   */
  private static ImageWriter getWriter() throws IIOException
  {
    Iterator<ImageWriter> iterator=ImageIO.getImageWritersBySuffix("gif");
    if(!iterator.hasNext())
    {
      throw new IIOException("Error finding GIF Image Writer");
    }
    else
    {
      return iterator.next();
    }
  }
  /*
   * Create and return a new child node if the requested node doesn't exist, else, return existing child node
   * 
   * @param root: the IIOMetadataNode to search for child node
   * @param nodeName: name of child node
   * 
   * @return: return existing child node if it exists, else return a new node with name nodeName
   */
  private static IIOMetadataNode getNode(IIOMetadataNode root,String nodeName)
  {
    int i;
    int numNodes=root.getLength();
    for(i=0;i<numNodes;i++)
    {
      if(root.item(i).getNodeName().compareToIgnoreCase(nodeName)==0)
      {
        return (IIOMetadataNode) root.item(i);
      }
    }
    IIOMetadataNode newNode=new IIOMetadataNode(nodeName);
    root.appendChild(newNode);
    return newNode;
  }
  
  public void writeToSequence(RenderedImage image)throws IOException
  {
    gifWriter.writeToSequence(new IIOImage(image,null,imageMetadata), imageWriteParam);
  }
  
  /*
   * Closes GIFWriter object (finishes off the GIF). 
   * Note: This method doesn't close underlying stream
   */
  public void close() throws IOException
  {
    gifWriter.endWriteSequence();
  }
  
}
////