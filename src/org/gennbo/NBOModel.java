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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

import javajs.util.PT;
import javajs.util.SB;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.gennbo.NBOView.OrbitalList;
import org.jmol.i18n.GT;
import org.jmol.util.Elements;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

class NBOModel {

  protected NBODialog dialog;
  private Viewer vwr;

  protected NBOModel(NBODialog dialog) {
    this.dialog = dialog;
    this.vwr = dialog.vwr;
  }

  private final static int MODEL_ACTION_ALTER  = 0;
  private final static int MODEL_ACTION_CLIP   = 1;
  private final static int MODEL_ACTION_FUSE   = 2;
  private final static int MODEL_ACTION_LINK   = 3;
  private final static int MODEL_ACTION_MUTATE = 4;
  private final static int MODEL_ACTION_SWITCH = 5;
  private final static int MODEL_ACTION_TWIST  = 6;
  private final static int MODEL_ACTION_VALUE  = 7;
  private final static int MODEL_ACTION_3CHB   = 8;
  
  private static final int MODEL_ACTION_MAX    = 9;

  private static final int MODEL_ACTION_REBOND = 9;
  private static final int MODEL_ACTION_SYMMETRY = 10;
  private final static int MODEL_ACTION_HBOND   = 11;
  private final static int MODEL_ACTION_VIBRATE = 12;

  static final int MODE_MODEL_EDIT     = 21;
  static final int MODE_MODEL_NEW      = 31; 
  static final int MODE_MODEL_SAVE     = 41;
  static final int MODE_MODEL_TO_NBO   = 51;
  static final int MODE_MODEL_UNDO_REDO= 61;


  
  
  final static String[] MODEL_ACTIONS = { 
      "Alter", "Clip", "Fuse", "Link", "Mutate",
      "Switch", "Twist", "Value", "3chb", "Rebond", 
      "Sym.", "H-Bonds","Vib." };
  
  private static final String[] EDIT_INFO = {
      "Edit nuclear charge, bond length, bond angle, or dihedral angle",
      "Remove bond between two atoms",
      "Delete monovalent atoms and replace with bond",
      "Add bond between two atoms",
      "Replace atom with a new substituent-group",
      "Switch location of two groups",
      "Perform rigid torsional twist about dihedral angle",
      "Value of nuclear charge, bond length, bond angle, and dihedral angle",
      "Create 3-center linkage between two atoms and a ligand",
      "Change bonding symmetry around transition metal",
      "Display point-group symmetry of current model",
      "Show NBOPro6-derived hydrogen bonds",
      "TODO: Add info for Vibrate here"
      };

  private final static int BOX_COUNT_4 = 4, BOX_COUNT_2 = 2, BOX_COUNT_1 = 1;
  private final static int MAX_HISTORY = 5;

  ///  private static final String LOAD_SCRIPT = ";set zoomlarge false;zoomTo 0.5 {*} 0;";

  private NBOFileHandler saveFileHandler;

  private Box innerEditBox;
  private JTextField jtNIHInput, jtLineFormula;
  private JTextField currVal;
  private JComboBox<String> jcSymOps;
  private JButton rebond, vibrate,jbClear;
  private JLabel atomsLabel;
  private Box editComponent;
  private Box inputHeader;
  private Box saveHeader;
  private Component inputComponent;
  private Box saveComponent;
  private Box editHeader;  
  private JTextField[] atomNumBoxes;
  private JLabel valueLabel = new JLabel("");
  
  // only used by private listeners 
  
  protected JTextField editValueTf;
  protected JButton jbApply;
  protected JComboBox<String> jComboSave;
  protected JButton undo, redo;
  protected Stack<String> undoStack, redoStack;

  /**
   * identifies which action button as pressed -- for example, MODEL_ACTION_ALTER
   */
  private int actionID;

  /**
   * encodes number of atoms that can be selected
   */  
  private int boxCount;
  
 /**
   * A model is being loaded into Jmol that NBO does not know about yet
   */
  private boolean notFromNBO;

  /**
   * A flag to indicate that when the next atom is clicked, the selection should be cleared.
   * Set to true each time a non-value option action is processed.
   */
  private boolean resetOnAtomClick;
  private Box innerLinkOptionBox;
  private JRadioButton radLinkBond;
  
  
  /**
   * inputFileType is the option user chooses from the ComboBox of "Input Model"
   * inputFileType can be different from the ext input of "Input Model"
   */
  private String inputFileType;
  
  /**
   * saveFileType is the option user chooses from the ComboBox of "Save Model"
   * saveFileType can be different from the ext input of "Save Model"
   */
  private String saveFileType;
   
  private String vibrateData;
  
  private JScrollPane vibrationScroll;
  
  private String prevJobStem;
  
  protected void setModelNotFromNBO() {
    notFromNBO = true;
  }

  private void showComponents(boolean tf) {
    editHeader.setVisible(tf);
    editComponent.setVisible(tf);
    innerLinkOptionBox.setVisible(false);
    saveHeader.setVisible(tf);
    saveComponent.setVisible(tf);
  }

  void modelSetSaveParametersFromInput(NBOFileHandler nboFileHandler,
                                       String dir, String name, String ext) {
  //changed by fzy from "" to "gau" as gau is the default save file extension
    if (saveFileHandler != null && nboFileHandler != saveFileHandler)
      saveFileHandler.setInput(dir, name,
          PT.isOneOf(ext, NBOConfig.OUTPUT_FILE_EXTENSIONS) ? ext : "gau");  
  }


  protected JPanel buildModelPanel() {
    resetVariables();
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    inputHeader = NBOUtil.createTitleBoxForNBOModel(" Input Model ", dialog.new HelpBtn(
        "model_input_intro_help.htm"));
    panel.add(inputHeader);
    inputComponent = getInputComponent();
    panel.add(inputComponent);
    
    editHeader = getEditHeader();
    panel.add(editHeader).setVisible(false);
    editComponent = getEditComponent();
    panel.add(editComponent).setVisible(false);
    
    saveHeader = NBOUtil.createTitleBox(" Save Model ", dialog.new HelpBtn(
        "model_save_intro_help.htm"));
    panel.add(saveHeader).setVisible(false);
    saveComponent = getSaveComponent();
    panel.add(saveComponent).setVisible(false);
    panel.add(Box.createGlue());
   
    if (vwr.ms.ac > 0) {
      loadModelToNBO(null, false);
    }
    return panel;

  }

  private void resetVariables() {
    actionID = 0;
    boxCount = 0;
    notFromNBO = false;
    showSelectedOnFileLoad = false;
    resetOnAtomClick = true;
    serverMode = 0;
    saveFileType="";
  }

  private Box getEditHeader() {
    Box topBox = Box.createHorizontalBox();
    undo = new JButton("<HTML>&#8592Undo</HTML>");
    redo = new JButton("<HTML>Redo&#8594</HTML>");
    undoStack = new Stack<String>();
    redoStack = new Stack<String>();
    redo.addActionListener(redoAction);
    undo.addActionListener(undoAction);
    topBox.add(undo);
    topBox.add(redo);
    topBox.add(dialog.new HelpBtn("model_edit_intro_help.htm"));
    return NBOUtil.createTitleBox(" Edit Model ", topBox);
  }

  /**
   * adds use elements to main panel
   * 
   * @return use elements
   */
  private Component getInputComponent() {

    Box inputBox = NBOUtil.createBorderBox(true);
    inputBox.setMaximumSize(new Dimension(360, 140));
    inputBox.setPreferredSize(new Dimension(360, 140));
    inputBox.setMinimumSize(new Dimension(360, 140));
    JPanel p2 = new JPanel(new GridLayout(3, 2));
    p2.setMaximumSize(new Dimension(360, 90));
    p2.setPreferredSize(new Dimension(360, 90));
    p2.setMinimumSize(new Dimension(360, 90));

    final JRadioButton jrJmolIn = new JRadioButton("NIH/PubChem/PDB");
    jrJmolIn.setFont(NBOConfig.monoFont);
    final JRadioButton jrLineIn = new JRadioButton("Line Formula");
    jrLineIn.setFont(NBOConfig.monoFont);
    jrLineIn.setSelected(true);
    final JRadioButton jrFileIn = new JRadioButton("File Input");
    jrFileIn.setFont(NBOConfig.monoFont);
    ButtonGroup rg = new ButtonGroup();
    rg.add(jrJmolIn);
    rg.add(jrLineIn);
    rg.add(jrFileIn);
    createInput(jtNIHInput = new JTextField(), jrJmolIn);
    createInput(jtLineFormula = new JTextField(), jrLineIn);
    jtNIHInput.setFont(NBOConfig.userInputFont);
    jtLineFormula.setFont(NBOConfig.userInputFont);
    jtLineFormula.add(new JLabel("line formula"));
    String[] useOps = { "<Select File  Type>", "[.47]   NBO Archive",
        "[.gau]  Gaussian Input", "[.log]  Gaussian Output",
        "[.gms]  GAMESS Input", "[.adf]  ADF Input", "[.jag]  Jaguar Input",
        "[.mm2]  MM2-Input", "[.mnd]  Dewar Type Input",
        "[.mp]   Molpro Input", "[.nw]   NWChem Input", "[.orc]  Orca Input",
        "[.pqs]  PQS Input", "[.qc]   Q-Chem Input", "[.xyz]  XYZ", "[.mol]  MOL",
        "[.cfi]  NBO Cartesian", "[.vfi]  NBO Valence" };
    final JComboBox<String> jComboUse = new JComboBox<String>(useOps);
    jComboUse.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doComboUseAction(jComboUse.getSelectedIndex() > 0 ? jComboUse.getSelectedItem().toString() : null);
      }
    });
    p2.add(jrLineIn);
    p2.add(jtLineFormula);
    p2.add(jrJmolIn);
    p2.add(jtNIHInput);
    p2.add(jrFileIn);
    p2.add(jComboUse);
    addFocusListeners(jComboUse, jrFileIn);
    
    inputBox.add(p2);
    if(dialog.inputFileHandler!=null)
      if(dialog.inputFileHandler.jobStem!=null)
        prevJobStem=dialog.inputFileHandler.jobStem;
      else
        prevJobStem=null;
    else
      prevJobStem=null;
    
    dialog.inputFileHandler = new NBOFileHandler("", "", NBOFileHandler.MODE_MODEL_USE, NBOConfig.INPUT_FILE_EXTENSIONS, dialog) 
    {
    
      @Override
      protected boolean doFileBrowsePressed() {
        String folder = tfDir.getText().trim();
        String name = tfName.getText();
        if (name.length() == 0)
          name = "*";
        String ext = tfExt.getText();
        if (ext.length() == 0)
          ext = "*";
        folder = NBOUtil.getWindowsFullNameFor(folder, name, ext);
        JFileChooser myChooser = new JFileChooser();
        String filter = useExt;
        if (name.equals("*") && !ext.equals("*"))
          filter = ext;
        if (filter.contains(";"))
          myChooser.setFileFilter(new FileNameExtensionFilter(filter, filter
              .split(";")));
        else
          myChooser.setFileFilter(new FileNameExtensionFilter(filter, filter));
        
        myChooser.setFileHidingEnabled(true);
        if (folder.endsWith("/"))
          folder = folder + "*.*";
        if (!folder.equals(""))
          myChooser.setSelectedFile(new File(folder));
        int button = myChooser.showDialog(this, GT.$("Select"));
        
        if(vibrate!=null)
          vibrate.setEnabled(false);
        
        if (button == JFileChooser.APPROVE_OPTION) 
        {
          if (PT.isOneOf(inputFileType, NBOConfig.INPUT_FILE_EXTENSIONS)) 
          {
            File newFile = myChooser.getSelectedFile();
            if (newFile.toString().indexOf(".") < 0) {
              dialog.logError("File not found");
              return false;
            }
            loadModelFromNBO(newFile.getParent(),
                (jobStem = NBOUtil.getJobStem(newFile)), NBOUtil.getExt(newFile));
            dialog.inputFileHandler
                .setInput(fullFilePath, jobStem, NBOUtil.getExt(newFile));
            fullFilePath = newFile.getParent();
            
            //If this is a Gaussian Output [.log] file, send request to NBOServe to check if this file can vibrate
            if(inputFileType.equals("log"))
              postVibrateRequestToNBO(newFile.getParent(),jobStem,NBOUtil.getExt(newFile));
            
                      
            return true;
          }
          else
          {
            dialog.logError("Invalid input file type defined");
          }
        }
        return false;
      }
    };
    
    jComboUse.setSelectedIndex(1);
    if(prevJobStem!=null)
      dialog.inputFileHandler.tfName.setText(prevJobStem);
    // BH adding focus for these as well
    addFocusListeners(dialog.inputFileHandler.tfDir, jrFileIn);
    addFocusListeners(dialog.inputFileHandler.tfExt, jrFileIn);
    addFocusListeners(dialog.inputFileHandler.tfName, jrFileIn);
    addFocusListeners(dialog.inputFileHandler.btnBrowse, jrFileIn);

    inputBox.add(dialog.inputFileHandler);
    inputBox.add(Box.createGlue());
    
    return inputBox;
  }

  protected void doComboUseAction(String item) {
    if (item == null) {
      dialog.inputFileHandler.tfExt.setText("");
      dialog.inputFileHandler.useExt = NBOConfig.INPUT_FILE_EXTENSIONS;
    } else {
      item = item.substring(item.indexOf("[") + 2, item.indexOf("]"));
      dialog.inputFileHandler.tfExt.setText(item);
      inputFileType=item;
      dialog.inputFileHandler.useExt = item;
    }
  }

  private Box getEditComponent() {
    Box editBox = NBOUtil.createBorderBox(false);
    Box actionBox = Box.createVerticalBox();
    

    final JRadioButton[] jrModelActions = new JRadioButton[MODEL_ACTION_MAX];
    ButtonGroup rg = new ButtonGroup();
    for (int i = 0; i < MODEL_ACTION_MAX; i++) {
      jrModelActions[i] = new JRadioButton(MODEL_ACTIONS[i]);
      jrModelActions[i].setToolTipText(EDIT_INFO[i]);
      actionBox.add(jrModelActions[i]);
      rg.add(jrModelActions[i]);
      final int op = i;
      jrModelActions[i].addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          doModelAction(op);
        }
      });
    }

    editBox.add(actionBox);
    Box rightBox = Box.createVerticalBox();
    createInnerEditBox();
    rightBox.add(this.innerEditBox);
    Box lowBox = Box.createHorizontalBox();
    
    JButton sym = new JButton(MODEL_ACTIONS[MODEL_ACTION_SYMMETRY]);
    sym.setToolTipText(EDIT_INFO[MODEL_ACTION_SYMMETRY]);
    sym.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doGetSymmetry();
      }
    });
    
    lowBox.add(sym);
    rebond = new JButton(MODEL_ACTIONS[MODEL_ACTION_REBOND]);
    rebond.setEnabled(false);
    rebond.setToolTipText(EDIT_INFO[MODEL_ACTION_REBOND]);

    rebond.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doModelAction(MODEL_ACTION_REBOND);
      }
    });
    lowBox.add(rebond);
    
    JButton hbond = new JButton(MODEL_ACTIONS[MODEL_ACTION_HBOND]);
    hbond.setToolTipText(EDIT_INFO[MODEL_ACTION_HBOND]);
    hbond.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doGetHBonds();
      }
    });
    lowBox.add(hbond);
    
    vibrate=new JButton(MODEL_ACTIONS[MODEL_ACTION_VIBRATE]);
    vibrate.setEnabled(false);
    vibrate.setToolTipText(EDIT_INFO[MODEL_ACTION_VIBRATE]);
    
    vibrate.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        doVibrate();
      }
    }
    );
    lowBox.add(vibrate);
    
    rightBox.add(lowBox);
    editBox.add(rightBox);
    
    //btns[0].doClick();
    return editBox;
  }

  private void createInnerEditBox() {

    innerEditBox = Box.createVerticalBox();
    innerEditBox.setBorder(BorderFactory.createLoweredBevelBorder());
    innerEditBox.setMaximumSize(new Dimension(275, 200));
    innerEditBox.setAlignmentX(0.5f);
    innerEditBox.setVisible(false);
    Box atBox = Box.createHorizontalBox();
    atBox.add(atomsLabel = new JLabel("")); // "Atoms:"
    atomNumBoxes = new JTextField[4];
    for (int i = 0; i < 4; i++) {
      atomNumBoxes[i] = new JTextField();
      atomNumBoxes[i].setFont(NBOConfig.userInputFont);
      atomNumBoxes[i].setMaximumSize(new Dimension(50, 50));
      atBox.add(atomNumBoxes[i]).setVisible(false);
      final int num = i;
      atomNumBoxes[i].addKeyListener(new KeyListener(){

        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {}

        @Override
        public void keyReleased(KeyEvent e) {
          editValueTf.setText("");
          editValueTf.setEnabled(modelEditGetSelected().length() > 0);
        }
        
      });
      atomNumBoxes[i].addFocusListener(new FocusListener() {
        @Override 
        public void focusGained(FocusEvent arg0) {
          doAtomNumBoxFocus(true, num);
        }

        @Override
        public void focusLost(FocusEvent arg0) {
          doAtomNumBoxFocus(false, 0);
        }
      });
      atomNumBoxes[i].addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          doSetAtomBoxesFromSelection(null, false);
        }
      });
    }

    innerEditBox.add(atBox);

    Box box = Box.createHorizontalBox();
    box.add(new JLabel("Symmetry Type: "));
    jcSymOps = new JComboBox<String>();
    jcSymOps.addItem("<Select Transition Metal>");
    jcSymOps.setMaximumSize(new Dimension(180, 40));
    jcSymOps.setEnabled(false);
    box.add(jcSymOps);
    box.setVisible(false);
    innerEditBox.add(box);
    
    currVal = new JTextField("pick atoms...");
    currVal.setFont(NBOConfig.titleFont);
    currVal.setBackground(new Color(220,220,220));
    currVal.setMinimumSize(new Dimension(250, 40));
    currVal.setPreferredSize(new Dimension(250, 40));
    currVal.setMaximumSize(new Dimension(250, 40));
//    currVal.setEditable(false);
    currVal.setHorizontalAlignment(SwingConstants.CENTER);
    innerEditBox.add(currVal).setVisible(false);
    
    valueLabel = new JLabel();
    valueLabel.setAlignmentX(0.5f);
    innerEditBox.add(valueLabel).setVisible(false);

    editValueTf = new JTextField("Select atoms...");
    editValueTf.setVisible(false);
    editValueTf.setMaximumSize(new Dimension(200, 30));
    editValueTf.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doEditValueTextField();
      }
    });
    editValueTf.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void changedUpdate(DocumentEvent arg0) {
      }

      @Override
      public void insertUpdate(DocumentEvent arg0) {
        if (!editValueTf.getText().equals("")
            && !editValueTf.getText().contains("Select"))
          jbApply.setEnabled(true);
      }

      @Override
      public void removeUpdate(DocumentEvent arg0) {
        if (editValueTf.getText().equals(""))
          jbApply.setEnabled(false);
      }
    });
    innerEditBox.add(editValueTf).setVisible(false);

    innerLinkOptionBox = Box.createHorizontalBox();
    radLinkBond = new JRadioButton("Bond");
    radLinkBond.setSelected(true);
    JRadioButton radLinkDotted = new JRadioButton("Dotted");
    innerLinkOptionBox.add(radLinkBond);
    innerLinkOptionBox.add(radLinkDotted);
    
    ButtonGroup g = new ButtonGroup();
    g.add(radLinkBond);
    g.add(radLinkDotted);
    innerEditBox.add(innerLinkOptionBox).setVisible(false);
    
    Box lowBox = Box.createHorizontalBox();
    jbClear = new JButton("Clear Selected");
    jbClear.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        clearSelected(true);
      }
    });
    jbApply = new JButton("Apply");
    jbApply.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doApply();
      }
    });
    lowBox.add(jbClear).setVisible(false);
    lowBox.add(jbApply).setVisible(false);
    innerEditBox.add(lowBox);

  }

  protected void doLinkDotted() {
    String atomList = modelEditGetSelected();
    String[] atoms = PT.getTokens(atomList);
    if (atoms.length == 2)
      processHBonds("1 " + atomList);
  }

  protected void doAtomNumBoxFocus(boolean isGained, int num) {
    System.out.println("atomnumbfocus" + isGained + " " +num);
    if (!isGained) {
      int atnum = PT.parseInt(atomNumBoxes[num].getText());
      if (atnum > vwr.ms.ac || atnum < 1) {
        atomNumBoxes[num].setText("");
      } else {
        doSetAtomBoxesFromSelection(null, false);
      }
    } else if (num == boxCount - 1) {
      jbApply.setEnabled(modelEditGetSelected().length() > 0);
    }
  }

  protected void doApply() {
    postActionToNBO(actionID);
  }

  protected void doEditValueTextField() {
    postActionToNBO(actionID);
  }

  protected void updateSelected(boolean doPost, boolean setFocus) {
    String selected = modelEditGetSelected();          
    String script = "measure delete;";
    int cnt = selected.split(" ").length;
    editValueTf.setEnabled(cnt > 0);
    editValueTf.setText("");
    if (editValueTf.isVisible())
      editValueTf.requestFocus();
    switch (boxCount) {
    case BOX_COUNT_4:
      String desc = "";
      if (cnt > 1) 
        script += "measure " + selected + " \" \";";
      switch (cnt) {
      case 0:
        currVal.setText("pick atoms...");
        break;
      case 1:
        desc = (actionID == MODEL_ACTION_ALTER ? "atomic number or symbol"
            : "atomic number");
        break;
      case 2:
        desc = "distance";
        break;
      case 3:
      case 4:
        desc = (cnt == 3 ? "angle" : "dihedral angle");
        break;
      }
      valueLabel.setText("(" + desc + ")");
      valueLabel.setVisible(cnt > 0);
      break;
    case BOX_COUNT_2:
      if (cnt == 2) {
        jbApply.setEnabled(true);
        if (editValueTf.isVisible())
          editValueTf.requestFocus();
        else if (setFocus)
          atomNumBoxes[1].requestFocus();
      }
      break;
    case BOX_COUNT_1:
      if (cnt == 1) {
        if (actionID == MODEL_ACTION_REBOND) {
          jcSymOps.removeAllItems();
          jcSymOps.setEnabled(true);
          int atomNumber = PT.parseInt(atomNumBoxes[0].getText());
          if (atomNumber < 1)
            return;
          int val = vwr.ms.at[atomNumber - 1].getValence();
          jbApply.setEnabled(true);
          String[] symlist = getRebondSymList(val);
          if (symlist != null) {
            for (int i = 0; i < symlist.length; i++)
              jcSymOps.addItem(symlist[i]);
            if (currentRebondSymOp > 0)
              jcSymOps.setSelectedIndex(currentRebondSymOp);
            currentRebondSymOp = 0;
          } else {
            jcSymOps.addItem("<Select Transition Metal>");
            jcSymOps.setEnabled(false);
            jbApply.setEnabled(false);
          }
        }
      }
    }
    if (actionID == MODEL_ACTION_ALTER || actionID == MODEL_ACTION_TWIST && cnt == 4) {
      postActionToNBO(MODEL_ACTION_VALUE);
    }
    if (actionID == MODEL_ACTION_LINK) {
      script = "";
    }
    
    
    dialog.runScriptQueued(script);
    editValueTf.setText("");
    editValueTf.setEnabled(selected.length() > 0);
    dialog.showSelected(selected);
    if (actionID == MODEL_ACTION_VALUE || doPost)
      postActionToNBO(actionID);
  }

  private final static String[][] REBOND_LISTS = 
    new String[][]  {  
     { "td", "c3v", "c4v" },            // MX4
     { "c4vo", "c4vi" },                 // MX5
     { "c3vo", "c3vi", "c5vo", "c5vi" }, // MX6
  };
  
  private static String[] getRebondSymList(int val) {
    return  (val - 4 < REBOND_LISTS.length? REBOND_LISTS[val - 4]: null);
  }

  protected String modelEditGetSelected() {
    String s = "";
    for (int j = 0; j < boxCount; j++)
      s += atomNumBoxes[j].getText().trim() + " ";
    return PT.rep(s.trim(), "  ", " ").trim();
  }

  private Box getSaveComponent() {
    Box sBox = NBOUtil.createBorderBox(true);
    final String[] SAVE_OPTIONS = { 
        "<Select File Type>",
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
        "XYZ                        [.xyz]",
        "MOL                        [.mol]",
        "NBO Cartesian              [.cfi]",
        "NBO Valence                [.vfi]"};
    jComboSave = new JComboBox<String>(SAVE_OPTIONS);

    jComboSave.setFont(NBOConfig.monoFont);
    
    jComboSave.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (jComboSave.getSelectedIndex() > 0)
          doComboSaveAction(jComboSave.getSelectedItem().toString());
        else if(jComboSave.getSelectedIndex()==0)
          saveFileType="";
          
      }
    });
    
    sBox.add(jComboSave);
    
    saveFileHandler = new NBOFileHandler("", "", NBOFileHandler.MODE_MODEL_SAVE,
        NBOConfig.OUTPUT_FILE_EXTENSIONS, dialog) {
      
      @Override
      protected boolean doFileBrowsePressed() {
        String folder = tfDir.getText().trim();
        String name = tfName.getText().trim();
        String ext = tfExt.getText().trim();
//        if (!ext.equals("") && !folder.equals("") && !name.equals("")) {
//          if (!PT.isOneOf(ext, NBOConfig.OUTPUT_FILE_EXTENSIONS)) {
//            dialog.alertError("Invalid output extenstion");
//            return false;
//          }
//          if (name.length() == 0)
//            name = "new";
//          File f = new File(NBOUtil.getWindowsFullNameFor(folder, name, ext));
//          if (f.exists()) {
//            int i = JOptionPane.showConfirmDialog(null, "File " + f
//                + " already exists, do you want to overwrite contents?",
//                "Warning", JOptionPane.YES_NO_OPTION);
//            if (i == JOptionPane.NO_OPTION)
//              return false;
//            dialog.inputFileHandler.setInput(folder, name, ext);
//
//          }
//          saveModel(folder, name, ext);
//          dialog.saveWorkingPath(fullFilePath);
//          return true;
//        }
        JFileChooser myChooser = new JFileChooser();
        useExt = (ext.equals("") ? NBOConfig.OUTPUT_FILE_EXTENSIONS : ext);
        myChooser.setFileFilter(new FileNameExtensionFilter(useExt, useExt
            .split(",")));
        myChooser.setFileHidingEnabled(true);
        String savePath = fullFilePath;
        folder = NBOUtil.getWindowsFolderFor(folder, fullFilePath);
        if (name.equals("") && jobStem != null)
          savePath = tfDir.getText()
              + "/"
              + (jobStem.equals("") ? "new.cfi" : jobStem
                  + (ext.contains(";") ? "" : "." + ext));
        else
          savePath = tfDir.getText() + "/" + name + "." + ext;
        myChooser.setSelectedFile(new File(savePath));
        int button = myChooser.showSaveDialog(this);
        if (button == JFileChooser.APPROVE_OPTION) {
          File newFile = myChooser.getSelectedFile();
          ext = NBOUtil.getExt(newFile);
         //user will need to select a valid file type from the combo box (even if they want to customized
         //their own extension for save file)
          if (PT.isOneOf(saveFileType, NBOConfig.OUTPUT_FILE_EXTENSIONS)) {
            if (newFile.exists()) {
              int i = JOptionPane.showConfirmDialog(null, "File " + newFile
                  + " already exists, do you want to overwrite contents?",
                  "Warning", JOptionPane.YES_NO_OPTION);
              if (i == JOptionPane.NO_OPTION)
                return false;
            }
            dialog.inputFileHandler.setInput(folder, name, ext);
            fullFilePath = newFile.getParent();
            saveModel(newFile.getParent(), NBOUtil.getJobStem(newFile), ext);
            dialog.saveWorkingPath(fullFilePath);
          } 
          else
            dialog.logError("Invalid output file type defined");
        }
        return false;
      }

    };
    
    jComboSave.setSelectedIndex(1);
    
    sBox.add(saveFileHandler);
    return sBox;
  }

  protected void doComboSaveAction(String item) {
      String ext = item.substring(item.indexOf("[") + 2, item.indexOf("]"));
      saveFileType=ext;
      saveFileHandler.tfExt.setText(ext);
  }

  private void createInput(final JTextField field, JRadioButton radio) {
    field.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doLoadtModelFromTextBox(field);
      }
    });
    addFocusListeners(field, radio);
  }

  private void addFocusListeners(final JComponent field,
                                   final JRadioButton radio) {
    field.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent arg0) {
        radio.setSelected(true);
      }

      @Override
      public void focusLost(FocusEvent arg0) {
      }
    });
    radio.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent arg0) {
        field.requestFocus();
      }

      @Override
      public void focusLost(FocusEvent arg0) {
      }
    });
  }

  /**
   * edit action selected
   * 
   * @param action
   */
  protected void doModelAction(int action) {
    actionID = action;
    dialog.runScriptQueued("set refreshing true; measurements delete"); // just in case
    clearSelected(true);
    if (action != MODEL_ACTION_LINK) {
      if (action != MODEL_ACTION_CLIP)
        measures = ""; 
      innerLinkOptionBox.setVisible(false);
    }
    switch (action) {
    case MODEL_ACTION_MUTATE:
      boxCount = BOX_COUNT_1;
      setEditBox("Radical name or line formula...");
      break;
    case MODEL_ACTION_REBOND:
      boxCount = BOX_COUNT_1;
      setEditBox(null);
      break;
    case MODEL_ACTION_LINK:
      innerLinkOptionBox.setVisible(true);
      //$FALL-THROUGH$
    case MODEL_ACTION_CLIP:
    case MODEL_ACTION_FUSE:
    case MODEL_ACTION_SWITCH:
    case MODEL_ACTION_3CHB:
      boxCount = BOX_COUNT_2;
      setEditBox(null);
      break;
    case MODEL_ACTION_ALTER:
    case MODEL_ACTION_TWIST:
    case MODEL_ACTION_VALUE:
      boxCount = BOX_COUNT_4;
      setEditBox(null);
      break;
    }
  }

  private void setEditBox(String label) {
    if (label == null)
      label = "Select atom" + (boxCount > 1 ? "s" : "") + "...";
    jbApply.setEnabled(false);
    for (int i = 0; i < 4; i++)
      atomNumBoxes[i].setVisible(i < boxCount);
    atomsLabel.setText(boxCount == 0 ? "" : "Atom" + (boxCount > 1 ? "s" : "") + ":");
    editValueTf.setText(label);
    editValueTf.setEnabled(false);
    jcSymOps.getParent().setVisible(actionID == MODEL_ACTION_REBOND);
    switch (actionID) {
    case MODEL_ACTION_ALTER:
    case MODEL_ACTION_MUTATE:
    case MODEL_ACTION_TWIST:
    case MODEL_ACTION_3CHB:
      editValueTf.setVisible(true);
      break;
    default:
      editValueTf.setVisible(false);
    }

    currVal.setVisible(boxCount == BOX_COUNT_4);
    valueLabel.setVisible(true);//actionID == MODEL_ACTION_ALTER);

    jbApply.setVisible(actionID != MODEL_ACTION_VALUE);
    jbClear.setVisible(true);
    innerEditBox.repaint();
    innerEditBox.revalidate();
  }

  ActionListener redoAction = new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      String curr = redoStack.pop();
      if (redoStack.isEmpty()) {
        redo.setEnabled(false);
      }
      loadModelToNBO(curr, true);
      dialog.logCmd("Redo");
    }
  };

  ActionListener undoAction = new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      String curr = undoStack.pop();
      if (undoStack.isEmpty()) {
        undo.setEnabled(false);
        return;
      }
      String tmp = undoStack.pop();
      loadModelToNBO(tmp, true);
      redoStack.push(curr);
      if (redoStack.size() > MAX_HISTORY)
        redoStack.removeElementAt(MAX_HISTORY);
      dialog.logCmd("Undo");
    }
  };
  private boolean showSelectedOnFileLoad;
  private int currentRebondSymOp;
  private int serverMode;
  
  /**
   * Clear out the text fields
   * @param andShow TODO
   * 
   */
  protected void clearSelected(boolean andShow) {
    for (int i = 0; i < boxCount; i++) {
      atomNumBoxes[i].setText("");
    }
    
    if (currVal != null)
      currVal.setText("");
    if (valueLabel != null)
      valueLabel.setText(" ");
    if (editValueTf != null) {
      editValueTf.setText("Select atoms...");
      editValueTf.setEnabled(false);
      jbApply.setEnabled(false);
    }
    if (andShow)
      updateSelected(false, true);
  }

  /**
   * Apply the selected edit action to a model.
   * 
   * @param actionID
   * 
   */
  protected void postActionToNBO(int actionID) {
    if (actionID == MODEL_ACTION_LINK && !radLinkBond.isSelected()) {
      doLinkDotted();
      return;
    }
    SB sb = new SB();
    String selected = modelEditGetSelected();
    String cmd = MODEL_ACTIONS[actionID].toLowerCase() + " " + selected + " ";
    String val = editValueTf.getText().trim();
    if (actionID == MODEL_ACTION_ALTER && PT.parseInt(val) == Integer.MIN_VALUE) {
      if (val.length() == 0)
        return;
      val = "" + Elements.elementNumberFromSymbol(val, true);
    }
    if (boxCount == BOX_COUNT_4 || boxCount == BOX_COUNT_1)
      cmd += val;
    else if (actionID == MODEL_ACTION_3CHB) {
      if (!val.startsWith(":"))
        cmd += ":";
      cmd += val;
    }
    if (actionID == MODEL_ACTION_REBOND) {
      currentRebondSymOp = jcSymOps.getSelectedIndex();
      cmd += jcSymOps.getItemAt(currentRebondSymOp); // FIX g1 under Model
    }

    //    dialog.runScriptNow("save orientation o2");
    NBOUtil.postAddCmd(sb, cmd);
    dialog.logCmd(cmd);
    jbApply.setEnabled(false);
    resetOnAtomClick = (actionID != MODEL_ACTION_VALUE);
    postNBO(sb, actionID, (actionID == MODEL_ACTION_VALUE ? "Checking value"
        : "Editing model"), null, null);
  }

  /**
   * Post a request for a point group symmetry check.
   */
  protected void doGetSymmetry() {
    String cmd = "symmetry";
    dialog.logCmd(cmd);
    postNBO(NBOUtil.postAddCmd(new SB(), cmd), MODEL_ACTION_SYMMETRY,  "Checking Symmetry", null, null);
  }

  /**
   * Post a request for a point group symmetry check.
   */
  protected void doGetHBonds() {
    String cmd = "hbond";
    dialog.logCmd(cmd);
    postNBO(NBOUtil.postAddCmd(new SB(), cmd), MODEL_ACTION_HBOND,  "Getting Hydrogen Bonds", null, null);
  }
  
  
  protected void doVibrate()
  {
    //JDialog 
    int i,menuLength=0,height;
    dialog.logCmd("vibrate");
    
    String lines[]=vibrateData.split("\\r?\\n");
    
    Box box = NBOUtil.createTitleBox(" Harmonic Vibration Modes ", null);
    JLabel heading=new JLabel("       nu(cm-1) IR inten. sym.");
    heading.setFont(NBOConfig.listFont);
    JPanel mainPanel=new JPanel(new GridLayout(1, 0));
    JPanel vibratePanel=new JPanel(new GridLayout(0,1,0,0));
    List<JRadioButton> list=new ArrayList<JRadioButton>();
    ButtonGroup bg=new ButtonGroup();
    for(String vibrateRow : lines)
    {
      if(vibrateRow.trim().length()>0)
      {
        JRadioButton radiobutton=new JRadioButton(vibrateRow);
        radiobutton.setFont(NBOConfig.listFont);
        radiobutton.setOpaque(true);
        list.add(radiobutton);
        bg.add(radiobutton);
        vibratePanel.add(radiobutton);
        menuLength++;
      }
    }
   
    height=menuLength*30;
    if(height>500)
      height=500;
    
    vibrationScroll=new JScrollPane(vibratePanel);
    vibrationScroll.setMaximumSize(new Dimension(300, height));
//    vibrationScroll.getViewport().setMinimumSize(new Dimension(275, height));
    vibrationScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    mainPanel.add(vibrationScroll);
    
    vibrationScroll.getVerticalScrollBar().setUnitIncrement(25);
    Box box2=Box.createVerticalBox();
    box2.add(heading);
    box2.add(mainPanel);
    box2.setAlignmentX(0.0f);
    box.add(box2);
    
    
    
    final JDialog vibrateDialog=new JDialog(dialog,"Vibration Menu");
  
    vibrateDialog.setMaximumSize(new Dimension(300,height+30));
    vibrateDialog.setMinimumSize(new Dimension(300,height+30));
    vibrateDialog.setPreferredSize(new Dimension(300,height+30));
    
    vibrateDialog.setVisible(true);
    
    vibrateDialog.add(box);
    centerDialog(vibrateDialog,150);
    
  }
  
  private void centerDialog(JDialog d, int h) {
    int x = (dialog.getX() + dialog.getWidth()) / 2 + 150;
    int y = (dialog.getY() + dialog.getHeight()) / 2 - h;
    d.setLocation(x, y);
  }
  
  /**
   * clipped in?
   * 
   * @param textBox
   */
  protected void doLoadtModelFromTextBox(JTextField textBox) {
    String model = textBox.getText().trim();
    if (model.length() == 0)
      return;
    String s = "";
    dialog.inputFileHandler.setInput(null, "", "");
    saveFileHandler.setInput(null, "", "");
    clearSelected(false);
    if (textBox == jtNIHInput) {
      if (model.startsWith("!")) {
        dialog.runScriptQueued(model.substring(1));
        return;
      }
      dialog.modelOrigin = NBODialog.ORIGIN_NIH;
      notFromNBO = true;
      if ("$:=".indexOf(model.charAt(0)) < 0)
        model = "$" + model;
      if (model.startsWith("=")) {
        switch (model.length()) {
        case 5:
          break;
        case 4:
          model = "=" + model; // ligand codes require two == signs, for example ==HEM
          break;
        default:
          // "=" can be the start of may databases if there is a / present
          if (model.indexOf("/") < 0)
            dialog.logError("PDB codes must be of the form XXX for ligands and XXXX for standard PDB entries.");
          break;
        }
      }
      jtLineFormula.setText("");
      saveFileHandler.setInput(null, model, "gau");
      dialog.logCmd("get " + model);
      dialog.iAmLoading = true;
      if (dialog.loadModelFileNow(model) == null) {
        model = (model.charAt(0) == ':' ? "$" : ":") + model.substring(1);
        if (model.startsWith("$=")) {
          dialog.logError("RCSB does not recognize ligand code " + model.substring(2) + ".");
          dialog.iAmLoading = false;
          return;
        }
        dialog.logCmd("get " + model);
        if (dialog.loadModelFileNow(model) == null) {
          dialog.logError("Neither NIH/CIR nor PubChem have recognize this identifier.");
          notFromNBO = false;
          dialog.iAmLoading = false;
        }
      }
    } else {
      dialog.modelOrigin = NBODialog.ORIGIN_LINE_FORMULA;
      SB sb = new SB();
      jtNIHInput.setText("");
      s = "show " + model;
      saveFileHandler.setInput(null, "line", "gau");
      NBOUtil.postAddCmd(sb, s);
      dialog.logCmd(s);
      postNBO(sb, MODE_MODEL_NEW, "model from line input...",
          null, null);
    }
  }

  /**
   * Loads model gotten from Pubchem/NIS databases
   * 
   * @param s
   *        - cfi formatted model string
   * @param undoRedo 
   */

  protected void loadModelToNBO(String s, boolean undoRedo) {
    boolean alsoLoadJmol = true;
    if (s == null) {
      s = dialog.getCFIData();
      alsoLoadJmol = false;
    }
//    if (undoRedo)
//      dialog.runScriptNow("save orientation o2");
    SB sb = new SB();
    NBOUtil.postAddGlobalC(sb, "PATH", dialog.nboService.getServerPath(null) + "/");
    NBOUtil.postAddGlobalC(sb, "ESS", "c");
    NBOUtil.postAddGlobalC(sb, "FNAME", "jmol_outfile");
    NBOUtil.postAddGlobalC(sb, "IN_EXT", "cfi");
    NBOUtil.postAddCmd(sb, "use");
    postNBO(sb, (undoRedo ? MODE_MODEL_UNDO_REDO : MODE_MODEL_TO_NBO), (alsoLoadJmol ? "Loading" : "Sending") + " model to NB", "jmol_outfile.cfi", s);

  }

  /**
   * Loads model from file type after browse
   * 
   * @param path
   * @param fname
   * @param ext
   */
  protected void loadModelFromNBO(String path, String fname, String ext) {
    if (PT.isOneOf(ext, NBOConfig.JMOL_EXTENSIONS)) {
      notFromNBO = true;
      dialog.runScriptQueued("set refreshing false");
      dialog.loadModelFileQueued(new File(path  + "\\" + fname + "." + ext), false);
      dialog.runScriptQueued("set refreshing true");
      return;
    }
    String ess = getEss(inputFileType, true);
    SB sb = new SB();
    if (jtNIHInput != null) {
      jtNIHInput.setText("");
      jtLineFormula.setText("");
    }
    dialog.modelOrigin = NBODialog.ORIGIN_FILE_INPUT;
    NBOUtil.postAddGlobalC(sb, "PATH", path);
    NBOUtil.postAddGlobalC(sb, "ESS", ess);
    NBOUtil.postAddGlobalC(sb, "FNAME", fname);
    NBOUtil.postAddGlobalC(sb, "IN_EXT", ext.toLowerCase());
    NBOUtil.postAddCmd(sb, "use");
    clearSelected(false);
    dialog.logCmd("use." + ess + " " + fname + "." + ext);
    postNBO(sb, MODE_MODEL_NEW, "Loading model from NBO...", null, null);

  }

  /**
   * Save the model either by having Jmol convert it or NBOServe.
   * 
   * @param path
   * @param fname
   * @param ext
   */
  protected void saveModel(String path, String fname, String ext) {
    if (PT.isOneOf(ext, NBOConfig.JMOL_EXTENSIONS)) {
      String s = vwr.getModelExtract("1.1", false, false, ext.toUpperCase());
      String ret = vwr.writeTextFile(path + "\\" + fname + "." + ext, s);
      dialog.logValue(ret);
      return;
    }
    String ess = getEss(saveFileType, false);
    SB sb = new SB();
    
    NBOUtil.postAddGlobalC(sb, "PATH", path);
    NBOUtil.postAddGlobalC(sb, "ESS", ess);
    NBOUtil.postAddGlobalC(sb, "FNAME", fname);
    NBOUtil.postAddGlobalC(sb, "OUT_EXT", ext.toLowerCase());
    NBOUtil.postAddCmd(sb, "save");
    postNBO(sb, MODE_MODEL_SAVE, "Saving model...", null, null);
    dialog.logCmd("save." + ess + " " + fname);
    dialog.logValue("--Model Saved--<br>" + path + "\\" + fname + "." + ext);
  }

  /**
   * @param ext
   *        - extension to convert
   * @param isLoading
   *        - true if "use" (loading), false if saving
   * @return ess code used internally by NBOServie
   */
  private String getEss(String ext, boolean isLoading) {
    ext = ext.toLowerCase();
    if ((ext.equals("gau") || ext.equals("g09") || ext.equals("com")) && !isLoading) {
      if (jComboSave.getSelectedItem().toString().contains("(C")) {
        return ext.charAt(0) + "c";
      } else if (jComboSave.getSelectedItem().toString().contains("(z")) {
        return ext.charAt(0) + "z";
      } else
        return "g";
    } else if (ext.equals("cfi") || ext.equals("vfi") || ext.equals("gau")
        || ext.equals("log") || ext.equals("g09"))
      return "" + ext.charAt(0);
    else if (ext.equals("47"))
      return "a";
    else if (ext.equals("mm2"))
      return "mm";
    else if (ext.equals("com"))
      return "g";
    else
      return ext;
  }

  /**
   * callback notification from Jmol
   * 
   * @param picked
   *        [atomno1, atomno2] or [atomno1, Integer.MIN_VALUE]
   * 
   */
  protected void notifyPick(int[] picked) {
    dialog.runScriptQueued("measure delete;"
        + (resetOnAtomClick ? "select none" : ""));
    if (resetOnAtomClick) {
      clearSelected(false);
    }
    resetOnAtomClick = false;
    if (boxCount == 0)
      return;
    String selected = " " + modelEditGetSelected() + " ";
    int at1 = picked[0];
    int at2 = picked[1];
    if (at2 == Integer.MIN_VALUE) {
      // atom selection
      boolean isSelected = vwr.bsA().get(at1 - 1);
      if (isSelected) {
        selected = PT.rep(selected, " " + at1 + " ", " ").trim();
      } else {
        if (PT.getTokens(selected).length >= boxCount) {
          clearSelected(true);
          selected = "";
        }
        selected += " " + at1;
      }
      doSetAtomBoxesFromSelection(selected, true);
    } else {
      //Bond selection -- just break that into two atom picks
      if (boxCount != BOX_COUNT_2)
        return;
      clearSelected(true);
      notifyPick(new int[] { at1, Integer.MIN_VALUE });
      notifyPick(new int[] { at2, Integer.MIN_VALUE });
    }
  }

  protected void doSetAtomBoxesFromSelection(String selected, boolean setFocus) {
    if (selected == null)
      selected = modelEditGetSelected();
    String[] split = PT.getTokens(selected);
    //System.out.println("setting " + selected);
    for (int i = 0; i < atomNumBoxes.length; i++) {
      atomNumBoxes[i].setText(i >= split.length ? "" : "  " + split[i]);
      //System.out.println("set  i=" + i + " " + atomNumBoxes[i].getText());
    }
    updateSelected(false, setFocus);
    
  }

  private void setCurrentValue(String sval) {
    currVal.setText(sval.length() == 0 ? "pick atoms..." : "current value: " + sval);
  }

  /**
   * callback notification from Jmol
   * 
   */
  protected void notifyFileLoaded() {

    String fileContents = dialog.getCFIData();
    if (notFromNBO) {
      notFromNBO = false;
      loadModelToNBO(fileContents, false);
      return;
    }
    dialog.runScriptQueued(NBOConfig.JMOL_FONT_SCRIPT);
//        + ";select 1.1;"); // NOT rotate best, because these may be symmetry designed
    dialog.doSetStructure(null);
    showComponents(true);
    innerEditBox.setVisible(true);
    if (vwr.ms.ac > 0)
      if (fileContents != null) {
        undoStack.push(fileContents);
        if (undoStack.size() > MAX_HISTORY)
          undoStack.removeElementAt(0);
      }
    undo.setEnabled(undoStack.size() > 1);
    redo.setEnabled(!redoStack.isEmpty());
    // "({1})"
    rebond.setEnabled(dialog.evaluateJmolString("{transitionMetal}")
        .length() > 4);
    if (actionID == MODEL_ACTION_MUTATE) {
      doModelAction(actionID);
    } // else if (actionID == MODEL_ACTION_REBOND && serverMode != MODEL_ACTION_SYMMETRY)
      //doGetSymmetry();
    if (showSelectedOnFileLoad) {
      updateSelected(false, true);
      showSelectedOnFileLoad = false;
    } else {
      dialog.runScriptQueued("select none; select on;refresh");
    }
  }
  
//  protected void showConfirmationDialog(String st, File newFile, String ext) {
//    int i = JOptionPane.showConfirmDialog(null, st, "Warning",
//        JOptionPane.YES_NO_OPTION);
//    if (i == JOptionPane.YES_OPTION)
//      saveModel(newFile.getParent(), NBOFileHandler.getJobStem(newFile), ext);
//  }

  /**
   * Post a request to NBOServe with a callback to processNBO_s.
   * 
   * @param sb
   *        command data
   * @param mode 
   * @param statusMessage
   * @param fileName  optional
   * @param fileData  optional
   */
  private void postNBO(SB sb, final int mode, String statusMessage, String fileName, String fileData) {
    final NBORequest req = new NBORequest();
    serverMode = mode;
    req.set(new Runnable() {
      @Override
      public void run() {
        processNBO(mode, req);
      }
    }, false, statusMessage, "m_cmd.txt", sb.toString(), fileName, fileData);
    dialog.nboService.postToNBO(req);
  }

  /**
   * Process the reply from NBOServe for a MODEL request
   * 
   * @param mode
   * @param req
   */
  protected void processNBO(int mode, NBORequest req) {
    String s = req.getReply();
    boolean doClear = true;
    switch (mode) {
    case MODEL_ACTION_ALTER:
      // using quaternion analysis to reorient the structure even though it has been messed up.
      dialog.runScriptQueued("z = show('zoom');set refreshing false;x = {*}.xyz.all;load " + s + NBOConfig.JMOL_FONT_SCRIPT
          + ";compare {*} @x rotate translate 0;script inline @z;set refreshing true");
      break;
    case MODEL_ACTION_TWIST:
    case MODEL_ACTION_REBOND:
      doClear = false;
      //$FALL-THROUGH$
    case MODEL_ACTION_CLIP:
    case MODEL_ACTION_FUSE:
    case MODEL_ACTION_LINK:
    case MODEL_ACTION_MUTATE:
    case MODEL_ACTION_SWITCH:
    case MODEL_ACTION_3CHB:
    case MODE_MODEL_NEW:
    case MODE_MODEL_EDIT:
      if (s.length() == 0) {
        dialog.logError("Return message from NBOServe was empty.");
        return;
      }
      s += NBOConfig.JMOL_FONT_SCRIPT;
// backing off from this so we can see what NBO6 does and maybe not need to do this
//      if (mode == MODE_MODEL_EDIT)
//        s = "set refreshing off;save orientation o4;load " + s
//            + ";restore orientation o4;set refreshing on";
//      else
        s = ";load " + s;
      if  (doClear)
        clearSelected(false);
      else
        showSelectedOnFileLoad = true;
      dialog.loadModelDataQueued(s);
      break;
    case MODE_MODEL_SAVE:
      break;
    case MODE_MODEL_TO_NBO:
      s = "load " + s  + NBOConfig.JMOL_FONT_SCRIPT + ";set refreshing true;";
//      s = "set refreshing off;save orientation o3;load " + s
//          + ";restore orientation o3;set refreshing on";
      dialog.loadModelDataQueued(s);
      break;
    case MODE_MODEL_UNDO_REDO:
      dialog.loadModelDataQueued(
      //    "set refreshing false;load " +
      s + NBOConfig.JMOL_FONT_SCRIPT
          //+ ";restore orientation o2;set refreshing true"
      );
      break;
    case MODEL_ACTION_VALUE:
      String sval = s.trim();
      dialog.logValue(sval);
      setCurrentValue(sval);
      break;
    case MODEL_ACTION_HBOND:
      measures = "";
      processHBonds(s);
      break;
    case MODEL_ACTION_SYMMETRY:
      // do not reorient the return in this case
      String symmetry = s.substring(0, s.indexOf("\n"));
      dialog.logValue(symmetry);
      // fix issue with blank first line
      s = s.substring(s.indexOf("\n") + 1);
      s = "set refreshing false;load " + s + NBOConfig.JMOL_FONT_SCRIPT
          + ";set refreshing true";
      showSelectedOnFileLoad = true;
      dialog.loadModelDataQueued(s);
      break;
    case MODEL_ACTION_VIBRATE:
      if(!s.equals("no data"))
      {
        vibrate.setEnabled(true);
        vibrateData=s;
      }
      break;
    }
  }

  /**
   * first number is number of bonds
   * 
   * @param s
   */
  private void processHBonds(String s) {
    float[] atomList = PT.parseFloatArray(s);
    if (atomList.length == 0 || atomList[0] == 0)
      return;
    String script = "";
    for (int i = atomList.length % 2; i < atomList.length; i += 2) {
      int a1 = (int) atomList[i];
      int a2 = (int) atomList[i + 1];
      String a1a2 = " @" + a1 + " @" + a2;
      script += "measure ID m" + ("" + Math.random()).substring(2) + a1a2
          + " radius 0.1 ' ';";
      dialog.logValue(dialog.evaluateJmolString("@" + a1 + ".atomName + " + "'-' + @" + a2 + ".atomName + ' d='+ distance(" + a1a2 + ")"));
    }
    measures += script;
    dialog.runScriptQueued(measures);
  }
  
  private String measures = "";
  
  
  private void postVibrateRequestToNBO(String path, String fname, String ext)
  {
    String vibrateCommand="vibrate="+fname+"."+ext;
    
    SB sb = new SB();
    NBOUtil.postAddCmd(sb, vibrateCommand);
    postNBO(sb, MODEL_ACTION_VIBRATE, "Checking Vibration", null, null);
  }
  
}
