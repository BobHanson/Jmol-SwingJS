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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javajs.util.PT;
import javajs.util.SB;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import org.jmol.modelset.Atom;

class NBOSearch extends NBOView {

  protected NBOSearch(NBODialog dialog) {
    super(dialog);
  }

  private final static int KEYWD_WEBHELP = 0;

  private final static int KEYWD_NPA = 1;
  private final static int KEYWD_NBO = 2;
  private final static int KEYWD_BEND = 3;
  private final static int KEYWD_E2PERT = 4;
  private final static int KEYWD_NLMO = 5;

  // Note: The next three are not in the order the buttons are presented
  private final static int KEYWD_NRT = 6;
  private final static int KEYWD_STERIC = 7;
  private final static int KEYWD_CMO = 8;

  private final static int KEYWD_DIPOLE = 9;
  private final static int KEYWD_OPBAS = 10;
  private final static int KEYWD_BAS1BAS2 = 11;

  /**
   * keywords in order of PRESENTATION
   * 
   */
  private final static String[] keywordNames = { "NPA", "NBO", "BEND", "E2",
      "NLMO", "NRT", "STERIC", "CMO", "DIPOLE", "OPBAS", "B1B2" };

  /**
   * @return name of the keyword
   */
  private String getKeyword() {
    return getKeywordName(keywordID);
  }

  /**
   * map button index to NBO keyword numbers.
   */
  private final static int[] btnIndexToNBOKeyword = new int[] { KEYWD_NPA,
      KEYWD_NBO, KEYWD_BEND, KEYWD_E2PERT, KEYWD_NLMO, KEYWD_CMO, KEYWD_NRT,
      KEYWD_STERIC, // these three are shifted 
      KEYWD_DIPOLE, KEYWD_OPBAS, KEYWD_BAS1BAS2 };

  private String getKeywordButtonLabel(int ibtn) {
    return (ibtn == 3 ? "E2PERT" : getKeywordName(btnIndexToNBOKeyword[ibtn]));
  }

  
  private static final int MODE_SEARCH_VALUE       = 14;
  private static final int MODE_SEARCH_LIST_MO     = 24;
  private static final int MODE_SEARCH_ATOM_VALUES = 34;
  private static final int MODE_SEARCH_BOND_VALUES = 44;
  private static final int MODE_SEARCH_LIST        = 54;
  private static final int MODE_SEARCH_LIST_LABEL  = 64;


  /**
   * * does not include "HELP" (0)
   * 
   * @param ikeywd
   * @return name for the specified keyword ID
   */
  private String getKeywordName(int ikeywd) {
    return keywordNames[ikeywd - 1];
  }

  /**
   * Return the NBOServe keyword for a given SEARCH option; allows for a
   * different presentation ordering in Jmol relative to actual numbers in
   * NBOServe
   * 
   * @param index
   *        the button index
   * @return the NBO keyword number for this option
   */
  protected final static int getNboKeywordNumber(int index) {
    return (btnIndexToNBOKeyword[index]);
  }

  protected final static String[] keyW = {
      "NPA    : Atomic and NAO properties",
      "NBO    : Natural Lewis Structure and\n NBO properties",
      "BEND   : NHO directionality and\n bond-bending",
      "E2PERT : 2nd-order energtics of NBO\n donor-acceptor interactions",
      "NLMO   : NLMO properties",
      "CMO    : NBO-based character of canonical\n molecular orbitals",
      "NRT    : Natural Resonance Theory\n weightings and bond orders",
      "STERIC : Total/pairwise contributions\n to steric exchange energy",
      "DIPOLE : L/NL contributions to electric\n dipole moment",
      "OPBAS  : Matrix elements of chosen operator   \n in chosen basis set",
      "B1B2   : Transformation matrix between\n chosen basis sets" };

  protected final static String[] npa = { "NPA Atomic Properties:",
      "  (1) NPA atomic charge", "  (2) NPA atomic spin density",
      "  (3) NEC atomic electron configuration",
      "NPA Molecular Unit Properties:", "  (4) NPA molecular unit charge",
      "  (5) NPA molecular unit spin density", "NAO Orbital Properties:",
      "  (6) NAO label", "  (7) NAO orbital population",
      "  (8) NAO orbital energy", "  (9) NAO orbital spin density",
      "  (10) NMB minimal basis %-accuracy", "Display Options:",
      "  (11) Display atomic charges" }, nbo = { "NBO Orbital Properties:",
      "  (1) NBO orbital label", "  (2) NBO orbital population",
      "  (3) NBO orbital energy", "  (4) NBO ionicity",
      "Natural Lewis Structure Properties:", "  (5) NLS rho(NL)",
      "  (6) NLS %-rho(L)" }, bend = { "NHO Orbital Prperties:",
      "  (1) NHO orbital label", "  (2) NHO orbital population",
      "  (3) NHO orbital energy", "  (4) NHO hybrid composition",
      "  (5) NHO direction angles",
      "  (6) NHO bending deviation from line of centers",
      "  (7) Strongest bending deviation for any NHO" }, e2 = {
      "E2 Values for Selected Donor-Acceptor NBOs:",
      "  (1) E(2) interaction for current d/a NBOs",
      "  (2) Strongest E(2) interaction for current d-NBO",
      "  (3) Strongest E(2) interaction for current a-NBO",
      "  (4) Strongest E(2) interaction for any d/a NBOs",
      "Intermolecular E2 Options:",
      "  (5) Strongest intermolecular E(2) for current unit",
      "  (6) Strongest intermolecular E(2) for any units" }, nlmo = {
      "NLMO Orbital Properties:", "  (1) NLMO orbital label",
      "  (2) NLMO population", "  (3) NLMO orbital energy",
      "  (4) NLMO %-NBO parentage", "NLMO Delocalization Tail Properties:",
      "  (5) NLMO delocalization tail population",
      "  (6) NLMO delocalization tail NBO components" }, nrt = {
      "Atom (A) Properties:", "  (1) atomic valency (total)",
      "  (2) atomic covalency", "  (3) atomic electrovalency",
      "Bond [A-A'] Properties:", "  (4) bond order (total)",
      "  (5) covalent bond order", "  (6) electrovalent bond order",
      "Resonance Structure Properties:", "  (7) RS weighting",
      "  (8) RS rho(NL) (reference structures only)", "Display Options:",
      "  (9) Display NRT atomic valencies", "  (10) Display NRT bond orders" },
      steric = { "Total Steric Exchange Energy (SXE) Estimates:",
          "  (1) Total SXE", "  (2) Sum of pairwise (PW-SXE) contributions",
          "Selected PW-SXE contributions:",
          "  (3) PW-SXE for current d-d' NLMOs",
          "  (4) Strongest PW-SXE for current d NLMO",
          "Intra- and intermolecular options:",
          "  (5) Strongest PW-SXE within current unit",
          "  (6) Strongest PW-SXE within any unit",
          "  (7) Strongest PW-SXE between any units" }, mo = {
          "Character of current MO (c):", "  (1) Current MO energy and type",
          "  (2) Bonding character of current MO",
          "  (3) Nonbonding character of current MO",
          "  (4) Antibonding character of current MO",
          "NBO (n) %-contribution to selected MO (c):",
          "  (5) %-contribution of current NBO to current MO",
          "  (6) Largest %-contribution to current MO from any NBO",
          "  (7) Largest %-contribution of current NBO to any MO" }, dip = {
          "Total Dipole Properties:", "  (1) Total dipole moment",
          "  (2) Total L-type (Lewis) dipole",
          "  (3) Total NL-type (resonance) dipole",
          "Bond [NBO/NLMO] Dipole Properties:",
          "  (4) Dipole moment of current NLMO",
          "  (5) L-type (NBO bond dipole) contribution",
          "  (6) NL-type (resonance dipole) contribution",
          "Molecular Unit Dipole Properties:",
          "  (7) Dipole moment of current molecular unit",
          "  (8) L-type contribution to unit dipole",
          "  (9) NL-type contribution to unit dipole" }, op = {
          "<select an operation>", " S    : overlap (unit) operator",
          " F    : 1e Hamiltonian (Fock/Kohn-Sham) operator",
          " K    : kinetic energy operator",
          " V    : 1e potential (nuclear-electron attraction) operator",
          " DM   : 1e density matrix operator",
          " DIx  : dipole moment operator (x component)",
          " DIy  : dipole moment operator (y component)",
          " DIz  : dipole moment operator (z component)" };

  private Box optionBox;
  private JButton back;
  private JButton keyWdBtn;
  protected JLabel unitLabel;
  protected JPanel opList;
  protected JRadioButton[] rBtns = new JRadioButton[12];
  protected JRadioButton radioOrbMO, radioOrbNBO;
  protected JComboBox<String> comboSearchOrb1, comboSearchOrb2, comboUnit1,
      comboAtom1, comboAtom2, comboBasis2, comboBasisOperation;

  /**
   * The ID associated with the currently chosen keyword.
   */
  private int keywordID = KEYWD_WEBHELP;

  /**
   * the radio button option selected for a given keyword
   */
  private int optionSelected;

  /**
   * the OPBAS operator currently selected
   */
  private int operator = 1;

  /**
   * set true when an action is going to produce a change that will require
   * relabeling the atoms, specifically NPA option 11/12 and NRT option 9
   */

  private boolean needRelabel;

  private void resetVariables() {
    optionSelected = -1;
    keywordID = 0;
    operator = 1;
  }

  /////////////////////////////////////////////////////////////////

  //  private NBODialog dialog;
  //  private Viewer vwr;

  //  protected NBODialogSearch(NBODialog dialog) {
  //    this.dialog = dialog;
  //    this.vwr = dialog.vwr;
  //  }
  //
  //
  protected JPanel buildSearchPanel() {

    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(createViewSearchJobBox(NBOFileHandler.MODE_SEARCH,false),
        BorderLayout.NORTH);
    //    inputFileHandler.tfName.setText("");

    /////INPUT FILE/////////////

    //    Box inputBox = Box.createHorizontalBox();
    //    inputBox.setAlignmentX(0.0f);
    //    if (inputFileHandler == null)
    //      inputFileHandler = newNBOFileHandler("", "47", 4, "47");
    //    else
    //      inputFileHandler = newNBOFileHandler(inputFileHandler.jobStem, "47", 4,
    //          "47");
    //    inputBox.add(inputFileHandler);
    //    inputBox.setBorder(BorderFactory.createLineBorder(Color.black));
    //    inputBox.setMinimumSize(new Dimension(360, 60));
    //    inputBox.setPreferredSize(new Dimension(360,60));
    //    inputBox.setMaximumSize(new Dimension(360, 60));
    //
    //    Box box1 = createTitleBox(" Select Job ",
    //        new HelpBtn("search_job_help.htm"));
    //    box1.add(inputBox);
    //    
    //    
    //    panel.add(box1, BorderLayout.NORTH);

    back = new JButton("<html>&#8592Back</html>");

    /////ALPHA-BETA SPIN/////////////////
    betaSpin = new JRadioButton("<html>&#x3B2</html>");
    alphaSpin = new JRadioButton("<html>&#x3B1</html>");
    ActionListener spinListener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        doSetSpin();
      }
    };
    alphaSpin.addActionListener(spinListener);
    betaSpin.addActionListener(spinListener);
    alphaSpin.setSelected(true);
    ButtonGroup bg = new ButtonGroup();
    bg.add(alphaSpin);
    bg.add(betaSpin);

    /////////CMO Radio Buttons/////////////////
    bg = new ButtonGroup();
    radioOrbMO = new JRadioButton("MO");
    radioOrbMO.setSelected(true);
    radioOrbMO.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        doSearchCMOSelectMO();
      }
    });
    radioOrbMO.setBackground(null);
    bg.add(radioOrbMO);

    radioOrbNBO = new JRadioButton("NBO");
    radioOrbNBO.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        doSearchCMOSelectNBO();
      }
    });
    radioOrbNBO.setBackground(null);
    bg.add(radioOrbNBO);

    /////SELECT KEYWORD///////////
    Box optionBox2 = Box.createVerticalBox();
    opList = new JPanel();
    comboBasisOperation = new JComboBox<String>(op);
    //    comboBasisOperation.setUI(new StyledComboBoxUI(150, 350));

    comboBasisOperation.setMaximumSize(new Dimension(350, 30));
    comboBasisOperation.setAlignmentX(0.0f);
    comboBasisOperation.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doComboBasisOperationAction();
      }
    });
    comboBasisOperation.setVisible(false);

    optionBox2.add(comboBasisOperation);
    optionBox2.add(opList);
    optionBox2.setBorder(BorderFactory.createLineBorder(Color.black));
    optionBox = Box.createVerticalBox();
    optionBox.setVisible(false);
    Box topBox = Box.createHorizontalBox();
    keyWdBtn = new JButton("<html></html>");

    keyWdBtn.setVisible(false);
    keyWdBtn.setRolloverEnabled(false);
    topBox.add(keyWdBtn);
    topBox.add(back);
    topBox.add(dialog.new HelpBtn("a") {
      @Override
      protected String getHelpPage() {
        return getSearchHelpURL();
      }
    });
    Box box2 = NBOUtil.createTitleBox(" Select Keyword ", topBox);
    back.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        doBack();
      }
    });

    back.setForeground(Color.blue);
    back.setEnabled(false);
    buildHome();

    box2.setAlignmentX(0.0f);
    optionBox.add(box2);
    optionBox2.setAlignmentX(0.0f);
    optionBox.add(optionBox2);
    panel.add(optionBox);

    comboBasis1 = new JComboBox<String>(NBOView.basSet);
    //comboBasis1.setUI(new StyledComboBoxUI(180, -1));

    dialog.inputFileHandler.setBrowseEnabled(true);

    dialog.viewSettingsBox.removeAll();
//    String file = vwr.getProperty("String", "filename", null).toString();
//    String ext = NBOUtil.getExt(new File(file));
//
//    if (PT.isOneOf(ext, NBOFileHandler.EXTENSIONS))
//      notifyFileLoaded_s();

    return panel;
  }

  protected void doSearchCMOSelectNBO() {
    showOrbJmol("PNBO", comboSearchOrb1.getSelectedIndex(), "cmo");
  }

  protected void doSearchCMOSelectMO() {
    showOrbJmol("MO", comboSearchOrb2.getSelectedIndex(), "cmo");
  }

  protected void doComboBasisOperationAction() {
    operator = comboBasisOperation.getSelectedIndex();
    if (operator > 0)
      changeKey(getBasisOperationsOPBAS());
  }

  protected void buildHome() {
    resetVariables();
    resetCurrentOrbitalClicked();
    opList.removeAll();
    comboBasisOperation.setVisible(false);
    keyWdBtn.setVisible(false);
    opList.setLayout(new GridBagLayout());
    opList.setBackground(Color.white);
    keywordID = KEYWD_WEBHELP;
    dialog.viewSettingsBox.setVisible(false);
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;
    for (int i = 0; i < keywordNames.length * 2; i += 2) {
      c.gridy = i;
      c.gridx = 0;
      c.gridwidth = 1;

      final int index = i / 2;
      JButton btn = new JButton(getKeywordButtonLabel(index));
      btn.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
          doKeywordClicked(index);
        }
      });
      opList.add(btn, c);
      c.gridx = 1;
      String st = keyW[index].substring(keyW[index].indexOf(":") + 1);
      JTextArea jt = new JTextArea(st);
      jt.setBackground(null);
      jt.setFont(NBOConfig.searchTextAreaFont);
      jt.setEditable(false);
      opList.add(jt, c);
      c.gridy = i + 1;
      c.gridx = 0;
      c.gridwidth = 2;
      JSeparator sp = new JSeparator(SwingConstants.HORIZONTAL);
      sp.setForeground(Color.BLACK);
      sp.setSize(350, 10);
      opList.add(sp, c);
    }
    opList.repaint();
    opList.revalidate();
    back.setEnabled(false);
  }

  protected void doKeywordClicked(int index) {
    if (dialog.nboService.isWorking()) {
      vwr.alert("Please wait for NBOServe to finish working");
      return;
    }
    keywordClicked(getNboKeywordNumber(index));
  }

  protected String getSearchHelpURL() {
    return (keywordID == KEYWD_WEBHELP ? "search_help.htm" : getKeyword()
        .equals("E2") ? "search_e2pert_help.htm" : "search_" + getKeyword()
        + "_help.htm");
  }

  protected void doSetSpin() {
    showLewisStructure();
    switch (keywordID) {
    case KEYWD_NBO:
    case KEYWD_BEND:
    case KEYWD_DIPOLE:
      postListRequest("o", comboSearchOrb1);
      break;
    case KEYWD_E2PERT:
      postListRequest("d nbo", comboSearchOrb1);
      postListRequest("a nbo", comboSearchOrb2);
      break;
    case KEYWD_NLMO:
      postListRequest("o", comboSearchOrb2);
      break;
    case KEYWD_STERIC:
      postListRequest("d", comboSearchOrb1);
      postListRequest("d'", comboSearchOrb2);
      break;
    case KEYWD_CMO:
      postListRequest("n", comboSearchOrb1);
      if (radioOrbMO.isSelected())
        radioOrbMO.doClick();
      break;
    case KEYWD_OPBAS:
    case KEYWD_BAS1BAS2:
      setBasisForOPBASorB1B2(0);
      break;
    }
  }

  protected void doBack() {
    if (keywordID == KEYWD_NRT && comboUnit1.getModel().getSize() > 0)
      comboUnit1.setSelectedIndex(0);
    SB script = new SB();
    if (needRelabel) {
      showLewisStructure();
      script
          .append("measurements off;select add {*}.bonds; color bonds lightgrey; select none;");
    }
    script.append("isosurface delete; select off;refresh");
    dialog.runScriptQueued(script.toString());
    buildHome();
  }

  private void changeKey(final String[] s) {

    secondPick = true;
    back.setEnabled(true);
    dialog.viewSettingsBox.setVisible(!dialog.jmolOptionNONBO);
    keyWdBtn.setText("<html><font color=black>" + getKeyword()
        + "</font></html>");
    keyWdBtn.setVisible(true);
    dialog.runScriptQueued("isosurface delete;refresh");
    opList.removeAll();

    ButtonGroup btnGroup = new ButtonGroup();

    opList.setLayout(new BoxLayout(opList, BoxLayout.Y_AXIS));
    if (keywordID == KEYWD_OPBAS) {
      comboBasisOperation.setVisible(true);
      opList.add(comboBasisOperation);
    }

    for (int i = 0; i < s.length; i++) {
      if (!s[i].trim().startsWith("(")) {
        JLabel lab = new JLabel(s[i]);
        lab.setFont(NBOConfig.searchOpListFont);
        lab.setForeground(Color.blue);
        opList.add(lab);
      } else {
        final int num = Integer.parseInt(s[i].substring(s[i].indexOf("(") + 1,
            s[i].indexOf(")"))) - 1;
        rBtns[num] = new JRadioButton(s[i].substring(s[i].indexOf(')') + 1));
        rBtns[num].addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent arg0) {
            doGetSearchValue(num + 1);
          }
        });
        rBtns[num].setBackground(null);
        opList.add(rBtns[num]);
        btnGroup.add(rBtns[num]);
      }
      opList.add(new JSeparator());
    }

    opList.add(Box.createRigidArea(new Dimension(0, (16 - s.length) * 20)));

    opList.repaint();
    opList.revalidate();

  }

  protected void showMessage() {
    JOptionPane.showMessageDialog(dialog,
        "Error getting lists, an error may have occured during run");
  }

  protected void keywordClicked(int index) throws IllegalArgumentException {
    isNewModel = false;
    keywordID = index;
    resetCurrentOrbitalClicked();
    switch (index) {
    case KEYWD_NPA:
      load(31, false);
      comboAtom2 = null;
      //changed by fzy from PNAO to NAO
      comboBasis1.setSelectedIndex(BASIS_NAO); // WAS BASIS_NAO ? 
      setKeyword(new String[] { "b", "a", "o PNAO", "u" }, new String[] {
          "Basis: ", "Atom: ", "Orbital: ", "Unit: " });
      changeKey(npa);
      break;
    case KEYWD_NBO:
      load(36, true);
      //changed by fzy from PNBO to NBO
      comboBasis1.setSelectedIndex(BASIS_NBO);
      setKeyword(new String[] { "b", "o PNBO" }, new String[] { "Basis: ",
          "Orbital: " });
      changeKey(nbo);
      break;
    case KEYWD_NLMO:
      load(38, true);
    //changed by fzy from PNLMO to NLMO
      comboBasis1.setSelectedIndex(BASIS_NLMO);
      setKeyword(new String[] { "b", "o PNLMO" }, new String[] { "Basis: ",
          "Orbital: " });
      changeKey(nlmo);
      break;
    case KEYWD_BEND:
      load(34, true);
    //changed by fzy from PNHO to NHO
      comboBasis1.setSelectedIndex(BASIS_NHO);
      setKeyword(new String[] { "b", "o PNHO" }, new String[] { "Basis: ",
          "Orbital: " });
      changeKey(bend);
      break;
    case KEYWD_NRT:
      dialog.runScriptQueued("set bondpicking true");
      if (dialog.isOpenShell())
        setKeyword("s a a' rs".split(" "), new String[] { "Spin: ", "Atom A: ",
            "Atom A': ", "Res Struct: " });
      else
        setKeyword("a a' rs".split(" "), new String[] { "Atom A: ",
            "Atom A': ", "Res Struct: " });
      changeKey(nrt);
      break;
    case KEYWD_E2PERT:
      load(36, true);
    //changed by fzy from PNBO to NBO
      comboBasis1.setSelectedIndex(BASIS_NBO);
      setKeyword(new String[] { "b", "d nbo", "a nbo", "u" }, new String[] {
          "Basis: ", "d-NBO: ", "a-NBO:", "Unit: " });
      changeKey(e2);
      break;
    case KEYWD_STERIC:
      load(38, true);
    //changed by fzy from PNLMO to NLMO
      comboBasis1.setSelectedIndex(BASIS_NLMO);
      setKeyword(new String[] { "b", "d nlmo", "d' nlmo", "u" }, new String[] {
          "Basis: ", "d-NLMO: ", "d'-NLMO:", "Unit: " });
      changeKey(steric);
      break;
    case KEYWD_CMO:
      load(40, true);
      comboBasis1.setSelectedIndex(BASIS_MO);
      if (!dialog.runPanel.cleanNBOKeylist(
          dialog.inputFileHandler.read47File(false)[1], true).contains("CMO")) {
        dialog.runPanel.doRunGenNBOJob("CMO");
        return;
      }
      setKeyword(new String[] { "b", "c cmo", "n" }, new String[] { "Basis: ",
          "MO: ", "NBO:" });
      changeKey(mo);
      break;
    case KEYWD_DIPOLE:
      load(38, true);
    //changed by fzy from PNLMO to NLMO
      comboBasis1.setSelectedIndex(BASIS_NLMO);
      setKeyword(new String[] { "b", "o", "u" }, new String[] { "Basis: ",
          "Orbital: ", "Unit:" });
      changeKey(dip);
      break;
    case KEYWD_OPBAS:
      load(31, true);
      dialog.viewSettingsBox.removeAll();
      comboBasis1 = new JComboBox<String>(NBOView.basSet);
      //comboBasis1.setUI(new StyledComboBoxUI(180, -1));
      comboBasis1.setEditable(false);
      comboBasisOperation.requestFocus();
      setKeyword("b1 r c".split(" "), new String[] { "Basis:", "Row:",
          "Column:" });
      changeKey(new String[] {});
      break;
    case KEYWD_BAS1BAS2:
      dialog.runScriptQueued("set bondpicking true");
      setKeyword("b1 b2 r c".split(" "), new String[] { "Basis 1:", "Basis 2:",
          "Row:", "Column:" });
      getBasisOperationsB1B2();
      break;
    }
    dialog.repaint();
    dialog.revalidate();

    if (index == KEYWD_OPBAS)
      comboBasisOperation.showPopup();
  }

  private void load(int nn, boolean withBondPicking) {
    dialog.iAmLoading = true;
    if (dialog.loadModelFileNow(
        PT.esc(dialog.inputFileHandler.newNBOFileForExt("" + nn).toString().replace('\\', '/'))
        + (withBondPicking ? ";set bondpicking true" : "")) == null)
      dialog.iAmLoading = false;
  }

  /**
   * A generalized method for retrieving items from NBO with specific labels.
   * 
   * @param items
   *        Data items coded for use in meta commands
   * @param labels
   *        labels for these
   */
  protected void setKeyword(final String[] items, final String[] labels) {
    resetCurrentOrbitalClicked();
    dialog.viewSettingsBox.removeAll();
    dialog.viewSettingsBox.setLayout(new BorderLayout());
    JPanel outerListPanel = new JPanel(new GridLayout(labels.length, 1));
    JPanel innerListPanel = new JPanel(new GridLayout(labels.length, 1));
    for (int i = 0; i < labels.length; i++) {
      final String key = items[i].split(" ")[0];
      outerListPanel.add(new JLabel(labels[i]));
      if (key.equals("b") || key.equals("s")) {
        Box b = Box.createHorizontalBox();

        if (keywordID == KEYWD_CMO) {
          b.add(radioOrbMO);
          b.add(radioOrbNBO);
        } else if (key.equals("b")) {
          String str = peeify(comboBasis1.getSelectedItem().toString());
          b.add(new JLabel(str));
          //runScriptQueued("NBO TYPE " + str + ";MO TYPE " + str);
          b.add(Box.createRigidArea(new Dimension(20, 0)));
        }
        b.add(alphaSpin);
        b.add(betaSpin);
        //}
        innerListPanel.add(b);
      } else if (PT.isOneOf(items[i],
          "o PNBO;o PNLMO;r;d nlmo;n;d nbo;o PNHO;o")) {
        comboSearchOrb1 = new JComboBox<String>(
            new DefaultComboBoxModel<String>());
        setComboSearchOrbDefaultAction(key);
        innerListPanel.add(comboSearchOrb1);
        
        //fzy
        //TODO: INCOMPLETE
        if(items[i]=="d nbo")
        {
          ButtonGroup donorButtonGroup=new ButtonGroup();
          JRadioButton donorPositive = new JRadioButton("+");
          donorPositive.setSelected(true);
          donorPositive.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {

            }
          });
          donorPositive.setBackground(null);
          JRadioButton donorNegative = new JRadioButton("-");
          donorNegative.setSelected(true);
          donorNegative.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {

            }
          });
          donorNegative.setBackground(null);
          
          donorButtonGroup.add(donorPositive);
          donorButtonGroup.add(donorNegative);
        }
        postListRequest(key, comboSearchOrb1);
      } else if (PT.isOneOf(items[i], "c;d' nlmo;a nbo;c cmo;o PNAO")) {
        comboSearchOrb2 = new JComboBox<String>(
            new DefaultComboBoxModel<String>());
        postListRequest(key, comboSearchOrb2);
        if(items[i]=="a nbo")
        {
          
        }
        setComboSearchOrb2DefaultAction(key, items[i]);
        innerListPanel.add(comboSearchOrb2);
      } else if (key.equals("u")) {
        comboUnit1 = new JComboBox<String>(new DefaultComboBoxModel<String>());
        postListRequest(key, comboUnit1);
        unitLabel = new JLabel();
        Box box = Box.createHorizontalBox();
        box.add(comboUnit1);
        box.add(unitLabel);
        innerListPanel.add(box);
        unitLabel.setVisible(false);
      } else if (key.equals("rs")) {
        comboUnit1 = new JComboBox<String>(new DefaultComboBoxModel<String>());
        postListRequest("r", comboUnit1);
        innerListPanel.add(comboUnit1);
      } else if (key.equals("a")) {
        comboAtom1 = new JComboBox<String>(new DefaultComboBoxModel<String>());
        postListRequest("a", comboAtom1);
        comboAtom1.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            doSearchAtom1Action(comboAtom1.getSelectedIndex(),
                comboAtom2 == null ? -1 : comboAtom2.getSelectedIndex());
          }
        });
        innerListPanel.add(comboAtom1);
      } else if (key.equals("a'")) {
        comboAtom2 = new JComboBox<String>(new DefaultComboBoxModel<String>());
        postListRequest("a", comboAtom2);
        comboAtom2.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (comboAtom1.getSelectedIndex() > 0
                && comboAtom2.getSelectedIndex() > 0)
              dialog.runScriptQueued("select on @"
                  + (comboAtom1.getSelectedIndex()) + ",@"
                  + (comboAtom2.getSelectedIndex()));
          }
        });
        innerListPanel.add(comboAtom2);
      } else if (key.equals("b1")) {
        // OPBAS and B1B2
        Box b = Box.createHorizontalBox();
        b.add(comboBasis1);
        comboBasis1.setSelectedIndex(BASIS_AO);
        comboBasis1.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            setBasisForOPBASorB1B2(1);
          }
        });
        if (dialog.isOpenShell()) {
          //b.add(Box.createRigidArea(new Dimension(20,0)));
          b.add(alphaSpin);
          b.add(betaSpin);
        }
        innerListPanel.add(b);
      } else if (key.equals("b2")) {
        comboBasis2 = new JComboBox<String>(NBOView.basSet);
        //comboBasis2.setUI(new StyledComboBoxUI(180, -1));
        comboBasis2.setSelectedIndex(1);
        comboBasis2.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            setBasisForOPBASorB1B2(2);
          }
        });
        innerListPanel.add(comboBasis2);
      }
    }
    dialog.logCmd(getKeyword() + " Search Results:");
    JLabel lab = new JLabel("Settings");
    lab.setFont(NBOConfig.nboFont);

    lab.setOpaque(true);
    lab.setBackground(Color.black);
    lab.setForeground(Color.white);
    dialog.viewSettingsBox.add(lab, BorderLayout.NORTH);
    dialog.viewSettingsBox.add(outerListPanel, BorderLayout.WEST);
    dialog.viewSettingsBox.add(innerListPanel, BorderLayout.CENTER);
  }

  protected void doSearchAtom1Action(int atomno1, int atomno2) {
    if (atomno1 <= 0 || atomno2 <= 0)
      return;
    if (comboAtom2 == null)
      dialog.runScriptQueued("select on @" + atomno1);
    else
      dialog.runScriptQueued("select on @" + atomno1 + ", @" + atomno2);
  }

  private void setComboSearchOrbDefaultAction(final String key) {
    comboSearchOrb1.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (comboSearchOrb1.getSelectedIndex() > 0)
          doSearchOrb1Action(key);
      }
    });
  }

  //  private void setComboSearchOrbsActionForB1B2() {
  //    if (false) {
  //    comboSearchOrb
  //        .removeActionListener(comboSearchOrb.getActionListeners()[0]);
  //    comboSearchOrb.addActionListener(new ActionListener() {
  //      @Override
  //      public void actionPerformed(ActionEvent e) {
  //        showOrbJmol(comboBasis.getSelectedItem().toString(),
  //            comboSearchOrb.getSelectedIndex(), "b1");
  //      }
  //    });
  //    comboSearchOrb2.removeActionListener(comboSearchOrb2
  //        .getActionListeners()[0]);
  //    comboSearchOrb2.addActionListener(new ActionListener() {
  //      @Override
  //      public void actionPerformed(ActionEvent e) {
  //        showOrbJmol((searchKeywordNumber == KEYWD_OPBAS ? comboBasis
  //            : comboBasis2).getSelectedItem().toString(), comboSearchOrb2
  //            .getSelectedIndex(), "b2");
  //      }
  //      
  //    });
  //    }
  //}

  protected void doSearchOrb1Action(String key) {
    checkOptionClickForOrbitalSelection();
    if (key.equals("n")) {
      showOrbJmol("NBO", comboSearchOrb1.getSelectedIndex(), key);
      radioOrbNBO.doClick();
    } else
      showOrbJmol(comboBasis1.getSelectedItem().toString(),
          comboSearchOrb1.getSelectedIndex(), key);
  }

  private void setComboSearchOrb2DefaultAction(final String key,
                                               final String item) {
    comboSearchOrb2.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doComboSearchOrb2Click(key, item);
      }
    });
  }

  protected void doComboSearchOrb2Click(String key, String item) {
    int index2 = (comboSearchOrb2.isEnabled() ? comboSearchOrb2
        .getSelectedIndex() : 0);
    if (index2 <= 0)
      return;
    checkOptionClickForOrbitalSelection();
    if (keywordID == KEYWD_BAS1BAS2) {
      showOrbJmol(comboBasis2.getSelectedItem().toString(), index2, "b2");
    } else if (key.equals("a")) {
      showOrbJmol("NBO", comboSearchOrb1.getModel().getSize() + (index2 - 1),
          key);
    } else {
      showOrbJmol(comboBasis1.getSelectedItem().toString(), index2, key);
    }
    if (item.equals("c cmo")) {
      radioOrbMO.doClick();
    }
  }

  /**
   * Reload the combo boxes for OPBAS or BAS1BAS2
   * 
   * @param iBasis unused 
   */
  protected void setBasisForOPBASorB1B2(int iBasis) {
//    resetCurrentOrbitalClicked();
//    if (comboBasis2 == null)
//      return;
    postListRequest("r", comboSearchOrb1);
    postListRequest("c", comboSearchOrb2);
    if (keywordID == KEYWD_OPBAS)
      getBasisOperationsOPBAS();
    else
      getBasisOperationsB1B2();
  }

  private String[] getBasisOperationsOPBAS() {
    String operator = comboBasisOperation.getSelectedItem().toString()
        .trim().split(" ")[0];
    return new String[] { "Current [r(ow),c(ol)] matrix element",
        "  (1) current <r|" + operator + "|c> value",
        "Extremal off-diagonal values for current r orbital:",
        "  (2) max <r|" + operator + "|*c> value for current r",
        "  (3) min <r|" + operator + "|*c> value for current r",
        "Extremal off-diagonal values for current c orbital:",
        "  (4) max <*r|" + operator + "|c> value for current c",
        "  (5) min <*r|" + operator + "|c> value for current c",
        "Extremal off-diagonal values for any [*r,*c] orbitals:",
        "  (6) max <*r|" + operator + "|*c> value for any *r,*c",
        "  (7) min <*r|" + operator + "|*c> value for any *r,*c" };
  }

  protected void getBasisOperationsB1B2() {
    String b1 = comboBasis1.getSelectedItem().toString();
    String b2 = comboBasis2.getSelectedItem().toString();
    changeKey(new String[] { "Current r(ow),c(olumn) matrix element:",
        "  (1) current <" + b1 + "(r)|" + b2 + "(c)> value",
        "Extremal off-diagonal values for current r orbital:",
        "  (2) max <" + b1 + "(r)|" + b2 + "(*c)> value for current r",
        "  (3) min <" + b1 + "(r)|" + b2 + "(*c)> value for current r",
        "Extremal off-diagonal values for current c orbital:",
        "  (4) max <" + b1 + "(*r)|" + b2 + "(c)> value for current c",
        "  (5) min <" + b1 + "(*r)|" + b2 + "(c)> value for current c",
        "Extremal off-diagonal values for any (*r,*c) orbitals:",
        "  (6) max <" + b1 + "(*r)|" + b2 + "(*c)> value for any *r,*c",
        "  (7) min <" + b1 + "(*r)|" + b2 + "(*c)> value for any *r,*c" });
  }

  /**
   * add "P" except to MO and AO
   * 
   * @param str
   * @return trimmed and P-prepended label
   */
  private String peeify(String str) {
    str = str.trim();
    //commented out by fzy. 
    //KEYWD NBO should have basis NBO, not PNBO
    //KEYWD NLMO should have basis NLMO, not PNLMO
    //KEYWD NPA should have basis NAO, not PNAO
    //KEYWD BEND should have basis NHO, not PNHO
//    return (!str.equals("MO") && !str.equals("AO") && str.charAt(0) != 'P' ? "P"
//        + str
//        : str);
    return str;
  }

  /**
   * The main method that is called to set up the post event to NBO.
   * 
   * @param op  one-based index of the radio buttons for this SEARCH option
   */
  protected void doGetSearchValue(int op) {
    optionSelected = op - 1;
    // check orbital is selected
    JComboBox<String> orb1 = comboSearchOrb1;
    JComboBox<String> orb2 = null, atom1 = null, atom2 = null, unit1 = null;
    String labelOrb1 = "ORB_1", labelOrb2 = "ORB_2", labelAtom1 = "ATOM_1", labelAtom2 = "ATOM_2", labelUnit1 = "UNIT_1";
    int offset1 = 0, offset2 = 0;

    final SB sb = getMetaHeader(false);
    NBOUtil.postAddGlobalI(sb, "KEYWORD", keywordID, null);
    boolean isLabelAtom = false;
    boolean isLabelBonds = false;

    // generally an offset is 1 because meta commands are 1-based, 
    // but if a combobox has a <select ...> in position 1, then the offset will be 0

    switch (keywordID) {
    case KEYWD_NPA:
      orb1 = comboSearchOrb2;
      unit1 = comboUnit1;
      if (op < 6 || op > 10) {
        // only 6-10 use orbital 1
        orb1 = null;
        if (op > 10) {
          isLabelAtom = true;
          op = 12;
        } else if (op <= 3) {
          // ops 1-3 use atom1
          atom1 = comboAtom1;
        }
      }
      break;
    case KEYWD_NBO:
    case KEYWD_BEND:
    case KEYWD_NLMO:
      // just orb1
      break;
    case KEYWD_E2PERT:
      labelOrb1 = "d_NBO_1";
      labelOrb2 = "a_NBO";
      orb2 = comboSearchOrb2;
      ComboBoxModel<String> x = orb1.getModel();
      System.out.println(x.getSize());
      offset2 = orb1.getModel().getSize() - 1;
      break;
    case KEYWD_NRT:
      orb1 = null;
      switch (op) {
      case 4:
      case 5:
      case 6:
        atom2 = comboAtom2;
        //$FALL-THROUGH$
      case 1:
      case 2:
      case 3:
        atom1 = comboAtom1;
        break;
      case 9:
        isLabelAtom = true;
        break;
      case 10:
        isLabelBonds = true;
        break;
      case 7:
      case 8:
        doShowResonanceStructure(comboUnit1.getSelectedIndex());
        break;
      default:
        break;
      }
      unit1 = comboUnit1;
      labelUnit1 = "RES_STR";
      break;
    case KEYWD_STERIC:
      labelOrb1 = "d_NBO_1";
      labelOrb2 = "d_NBO_2";
      orb2 = comboSearchOrb2;
      unit1 = comboUnit1;
      switch (op) {
      case 1:
      case 2:
      case 3:
        break;
      default:
        orb1 = null;
        //$FALL-THROUGH$
      case 4:
        orb2 = null;
        break;
      }
      break;
    case KEYWD_CMO:
      // op 7 requires only an NBO selection
      labelOrb1 = "NBO";
      if (op != 5 && op != 7) 
        orb1 = null;      
      // only  ops 5 and 7 require an NBO selection
      labelOrb2 = "CMO";
      orb2 = (op == 7 ? null : comboSearchOrb2);
      break;
    case KEYWD_DIPOLE:
      unit1 = comboUnit1;
      break;
    case KEYWD_OPBAS:
      labelOrb1 = "ROW";
      labelOrb2 = "COLUMN";
      orb2 = comboSearchOrb2;
      NBOUtil.postAddGlobalI(sb, "OPERATOR", operator, null);
      NBOUtil.postAddGlobalI(sb, "BAS_1", 1, comboBasis1);
      break;
    case KEYWD_BAS1BAS2:
      labelOrb1 = "ROW";
      labelOrb2 = "COLUMN";
      orb2 = comboSearchOrb2;
      NBOUtil.postAddGlobalI(sb, "BAS_1", 1, comboBasis1);
      NBOUtil.postAddGlobalI(sb, "BAS_2", 1, comboBasis2);
      break;
    }

    JComboBox<String> cb = null;
    boolean isOK = (
           (atom1 == null || (cb = atom1).getSelectedIndex() > 0)
        && (atom2 == null || (cb = atom2).getSelectedIndex() > 0)
        && (orb1 == null || (cb = orb1).getSelectedIndex() > 0)
        && (orb2 == null || (cb = orb2).getSelectedIndex() > 0));
    
    //fzy
    //This checking for "ok" doesn't work for e2pert. In particular, 
    //e2pert's strongest E2 Interaction for current d-NBO needs user to input only donor
    //e2pert's strongest E2 Interaction for current a-NBO needs user to input only acceptor
    //e2pert's strongest E2 Interaction for any d/a NBO no need any user input
    if(keywordID==KEYWD_E2PERT)
    {
      switch(optionSelected)
      {
        //strongest E2 Interaction for current d-NBO
        case 1:
          if(orb1!=null&&(cb = orb1).getSelectedIndex() > 0)
            isOK=true;
          break;
        //strongest E2 Interaction for current a-NBO
        case 2:
          if(orb2!=null&&(cb = orb2).getSelectedIndex() > 0)
            isOK=true;
          break;
        //strongest E2 Interaction for any d/a NBO
        case 3:
          isOK=true;
          break;
      }
    }
  //fzy
    //This checking for "ok" doesn't work for B1B2. In particular, 
    //B1B2's max/min <AO,PNO> pair for current r needs user to input only r
    //B1B2's max/min <AO,PNO> pair for current c needs user to input only c
    //B1B2's max/min <AO,PNO> pair for any r/c doesn't require any user input
    if(keywordID==KEYWD_BAS1BAS2)
    {
      switch(optionSelected)
      {
        // max/min <AO,PNO> pair for current r 
        case 1:
        case 2:
          if(orb1!=null&&(cb = orb1).getSelectedIndex() > 0)
            isOK=true;
          break;
        //max/min <AO,PNO> pair for current c 
        case 3:
        case 4:
          if(orb2!=null&&(cb = orb2).getSelectedIndex() > 0)
            isOK=true;
          break;
        //max/min <AO,PNO> pair for any r/c
        case 5:
        case 6:
          isOK=true;
          break;
      }
    }
    
    if (!isOK) {
      dialog.logError(cb.getItemAt(0));
      return;
    }
    if (orb1 != null)
      NBOUtil.postAddGlobalI(sb, labelOrb1, offset1, orb1);
    if (orb2 != null)
      NBOUtil.postAddGlobalI(sb, labelOrb2, offset2, orb2);
    if (atom1 != null)
      NBOUtil.postAddGlobalI(sb, labelAtom1, 0, atom1);
    if (atom2 != null)
      NBOUtil.postAddGlobalI(sb, labelAtom2, 0, atom2);
    if (unit1 != null)
      NBOUtil.postAddGlobalI(sb, labelUnit1, 1, unit1);
    NBOUtil.postAddGlobalI(sb, "OPT_" + getKeyword(), op, null);

    if (needRelabel) {
      dialog
          .runScriptQueued("select add {*}.bonds; color bonds lightgrey; select none; measurements off");
      showLewisStructure();
      needRelabel = false;
    }
    if (isLabelAtom) {
      needRelabel = true;
      postNBO_s(sb, MODE_SEARCH_ATOM_VALUES, null, "Getting labels", false);
    } else if (isLabelBonds) {
      needRelabel = true;
      dialog
          .runScriptQueued("select add {*}.bonds; color bonds [170,170,170]; select none");
      postNBO_s(sb, MODE_SEARCH_BOND_VALUES, null,
          "Getting bonds list", false);
    } else {
      postNBO_s(sb, MODE_SEARCH_VALUE, null, "Getting value...", true);
    }
  }

  private boolean secondPick = true;

  /**
   * Runs when list is finished being sent by nboServe -E2PERT: orbital
   * numbering of second list offset by length of first -NPA, STERIC, DIPOLE:
   * checks for more than 1 unit -NRT: gets res structure info
   * 
   * @param cb
   */
  protected void processReturnedSearchList(JComboBox<String> cb) {
    DefaultComboBoxModel<String> list = (DefaultComboBoxModel<String>) cb
        .getModel();
    switch (keywordID) {
    case KEYWD_E2PERT:
      if (cb == comboSearchOrb2) {
        //Relabel a-nbo to correct orbital number
        int offset = comboSearchOrb1.getModel().getSize() - 1; // list includes "<select an orbital>"
        int sz = cb.getModel().getSize();
        comboSearchOrb2.setEnabled(false);
        //        ActionListener listener = comboSearchOrb2.getActionListeners()[0];
        //        comboSearchOrb2.removeActionListener(listener);
        for (int i = 1; i < sz; i++) {
          String s = list.getElementAt(i);
          list.removeElementAt(i);
          s = "   " + (offset + i) + s.substring(s.indexOf("."));
          list.insertElementAt(s, i);
        }
        comboSearchOrb2.setEnabled(true);
        //        comboSearchOrb2.addActionListener(listener);
      }
      break;
    case KEYWD_NPA:
    case KEYWD_STERIC:
    case KEYWD_DIPOLE:
      if (cb == comboUnit1)
        if (list.getSize() == 1) 
        {
          comboUnit1.setVisible(false);
          unitLabel.setVisible(true);
          if((list.getElementAt(0).length())>6)
          {
            unitLabel.setText(list.getElementAt(0).substring(6)); // "unit:"
          }
        } else 
        {
          unitLabel.setVisible(false);
          comboUnit1.setVisible(true);
        }
      break;
    case KEYWD_NRT:
      if (cb == comboUnit1) {
        changeKey(nrt);
        //Parsing RS list here ensures RS list will be in .nbo file
        comboUnit1.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            doShowResonanceStructure(comboUnit1.getSelectedIndex());
          }
        });
        doShowResonanceStructure(0);
      }
      break;
    case KEYWD_OPBAS:
    case KEYWD_BAS1BAS2:
      if (cb == comboBasis2) {
        //setComboSearchOrbsActionForB1B2();
        comboSearchOrb1.setSelectedIndex(0);
        comboSearchOrb2.setSelectedIndex(0);

      }
    }
  }

  protected void doShowResonanceStructure(int index) {
    dialog.doSearchSetResStruct((isAlphaSpin() ? "rsa" : "rsb"), index);
  }

  /**
   * 
   * @param type
   * @param i
   * @param id
   */
  protected void showOrbJmol(String type, int i, String id) {
    if (i <= 0) {
      dialog.runScriptQueued("select visible;isosurface delete");
      dialog.logError("Select an orbital.");
      return;
    }
    id = fixID(id);
// Changes by fzy to fix the bug of Jmol unable to show both acceptor and donor
//    dialog.runScriptQueued("select visible;isosurface delete;"
//        + NBOConfig.getJmolIsosurfaceScript(id, peeify(type), i,
//            betaSpin.isSelected(), false));
    dialog.runScriptQueued("select visible;"
        + NBOConfig.getJmolIsosurfaceScript(id, peeify(type), i,
            betaSpin.isSelected(), false));
  }

  /**
   * Fix ID for isosurface command purposes only
   * 
   * @param id
   * @return id with quotes replaced by underscore
   * 
   */
  private String fixID(String id) {
    return PT.replaceAllCharacters(id, "'\"", "_");
  }

  /**
   * Click the current radio option button in certain cases after an orbital is
   * selected.
   */
  protected void checkOptionClickForOrbitalSelection() {
    if (optionSelected < 0)
      return;
    boolean doClick = false;
    switch (keywordID) {
    default:
    case KEYWD_NRT:
    case KEYWD_OPBAS:
    case KEYWD_BAS1BAS2:
      break;
    case KEYWD_NPA:
      doClick = (optionSelected > 4 && optionSelected < 10);
      break;
    case KEYWD_NBO:
      doClick = (optionSelected < 4);
      break;
    case KEYWD_BEND:
      doClick = (optionSelected < 6);
      break;
    case KEYWD_E2PERT:
      doClick = (optionSelected < 3);
      break;
    case KEYWD_NLMO:
      doClick = true;
      break;
    case KEYWD_STERIC:
      doClick = (optionSelected > 1 && optionSelected < 4);
      break;
    case KEYWD_CMO:
      doClick = true;
      break;
    case KEYWD_DIPOLE:
      doClick = (optionSelected > 2 && optionSelected < 6);
      break;
    }
    if (doClick)
      rBtns[optionSelected].doClick();
  }

  /**
   * callback notification from Jmol
   * 
   * @param picked
   *        [atomno1, atomno2] or [atomno1, Integer.MIN_VALUE]
   * 
   */
  protected void notifyPick_s(int[] picked) {
    dialog.runScriptQueued("isosurface delete");
    int at1 = picked[0];
    int at2 = picked[1];
    if (at2 == Integer.MIN_VALUE) {
      // single-atom pick
      switch (keywordID) {
      case KEYWD_NBO:
      case KEYWD_BEND:
      case KEYWD_NLMO:
      case KEYWD_E2PERT:
        showOrbital(nextOrbitalForAtomPick(at1,
            (DefaultComboBoxModel<String>) comboSearchOrb1.getModel()));
        return;
      case KEYWD_CMO:
        showOrbital(nextOrbitalForAtomPick(at1,
            (DefaultComboBoxModel<String>) comboSearchOrb2.getModel()));
        return;
      case KEYWD_NPA:
      case KEYWD_NRT:
      case KEYWD_STERIC:
      case KEYWD_DIPOLE:
      case KEYWD_OPBAS:
      case KEYWD_BAS1BAS2:
        break;
      }
      if (comboAtom1 != null && comboAtom2 == null) {
        comboAtom1.setSelectedIndex(at1);
        if (optionSelected >= 0 && optionSelected < 3)
          rBtns[optionSelected].doClick();
      } else if (comboAtom1 != null && comboAtom2 != null) {
        secondPick = !secondPick;
        if (secondPick)
          comboAtom2.setSelectedIndex(at1);
        else
          comboAtom1.setSelectedIndex(at1);
      }
      return;
    }

    switch (keywordID) {
    case KEYWD_NBO:
    case KEYWD_NLMO:
    case KEYWD_DIPOLE:
    case KEYWD_CMO:
      comboSearchOrb1.setSelectedIndex(pickBondNBO(at1, at2, comboSearchOrb1));
      break;
    case KEYWD_BEND:
      comboSearchOrb1.setSelectedIndex(pickBondNHO(at1, at2, comboSearchOrb1));
      break;
    case KEYWD_NRT:
      this.comboAtom1.setSelectedIndex(at1);
      this.comboAtom2.setSelectedIndex(at2);
      if (optionSelected > 2 && optionSelected < 6)
        rBtns[optionSelected].doClick();
      return;
    case KEYWD_E2PERT:
    case KEYWD_STERIC:
      String bond = at1 + "-" + at2;
      String str = comboSearchOrb1.getSelectedItem().toString()
          .replace(" ", "");
      if (str.contains(bond)) {
        comboSearchOrb1.setSelectedIndex(pickBondNBO(at1, at2, comboSearchOrb1));
        return;
      }
      str = comboSearchOrb2.getSelectedItem().toString().replace(" ", "");
      if (str.contains(bond)) {
        comboSearchOrb2.setSelectedIndex(pickBondNBO(at1, at2, comboSearchOrb2));
        return;
      }
      secondPick = !secondPick;
      if (secondPick)
        comboSearchOrb2.setSelectedIndex(pickBondNBO(at1, at2, comboSearchOrb2));
      else
        comboSearchOrb1.setSelectedIndex(pickBondNBO(at1, at2, comboSearchOrb1));
      break;
    case KEYWD_OPBAS:
    case KEYWD_BAS1BAS2:
      JComboBox<String> tmpBas = comboBasis1;
      if (keywordID == KEYWD_BAS1BAS2) {
        if (!secondPick)
          tmpBas = comboBasis2;
      }
      switch (tmpBas.getSelectedIndex()) {
      case BASIS_AO:
      case BASIS_PNAO:
      case BASIS_NAO:
      case BASIS_MO:
      default:
        secondPick = !secondPick;
        break;
      case BASIS_PNHO:
      case BASIS_NHO:
        secondPick = !secondPick;
        if (secondPick)
          comboSearchOrb2
              .setSelectedIndex(pickBondNHO(at1, at2, comboSearchOrb2));
        else
          comboSearchOrb1
              .setSelectedIndex(pickBondNHO(at1, at2, comboSearchOrb1));
        break;
      case BASIS_PNBO:
      case BASIS_NBO:
      case BASIS_PNLMO:
      case BASIS_NLMO:
        bond = at1 + "-" + at2;
        str = comboSearchOrb1.getSelectedItem().toString().replace(" ", "");
        if (str.contains(bond)) {
          comboSearchOrb1
              .setSelectedIndex(pickBondNBO(at1, at2, comboSearchOrb1));
          return;
        }
        str = comboSearchOrb2.getSelectedItem().toString().replace(" ", "");
        if (str.contains(bond)) {
          comboSearchOrb2
              .setSelectedIndex(pickBondNBO(at1, at2, comboSearchOrb2));
          return;
        }
        secondPick = !secondPick;
        if (secondPick)
          comboSearchOrb2
              .setSelectedIndex(pickBondNBO(at1, at2, comboSearchOrb2));
        else
          comboSearchOrb1
              .setSelectedIndex(pickBondNBO(at1, at2, comboSearchOrb1));
        break;
      }
    }
  }

  /**
   * An NBO type bond was clicked
   * 
   * @param at1
   * @param at2
   * @param cb
   * @return the orbital index, one-based
   */
  private int pickBondNBO(int at1, int at2, JComboBox<String> cb) {
    return selectOnOrb(at1 + "-" + at2, null, cb);
  }

  /**
   * An NHO type bond was clicked
   * 
   * @param at1
   * @param at2
   * @param cb
   * @return the orbital index, one-based
   */
  private int pickBondNHO(int at1, int at2, JComboBox<String> cb) {
    return selectOnOrb(at1 + "(" + at2 + ")", at2 + "(" + at1 + ")", cb);
  }

  /**
   * find next orbital from clicking a bond. Note that these lists have [select
   * an orbital] at the top, so the number returned will be the same as the
   * index on the list
   * 
   * @param b1
   * @param b2
   * @param cb
   * @return an orbital index -- one-based
   */
  protected int selectOnOrb(String b1, String b2, JComboBox<String> cb) {
    DefaultComboBoxModel<String> list = (DefaultComboBoxModel<String>) cb
        .getModel();
    int size = list.getSize();
    int curr = (currOrb.contains(b1) ? currOrbIndex : 0);
    for (int i = curr + 1; i < size + curr; i++) {
      int ipt = i % size;
      String str = list.getElementAt(ipt).replace(" ", "");
      if (str.contains(b1) || b2 != null && str.contains(b2)) {
        list.setSelectedItem(list.getElementAt(ipt));
        currOrb = str;
        currOrbIndex = ipt;
        return ipt;
      }
    }
    return curr;
  }

  /**
   * callback notification that Jmol has loaded a model while SEARCH was active
   * 
   */
  protected void notifyFileLoaded_s() {
    if (vwr.ms.ac == 0)
      return;
    dialog.runScriptQueued("isosurface delete");

    optionSelected = -1;
    if (dialog.isOpenShell()) {
      alphaSpin.setVisible(true);
      betaSpin.setVisible(true);
      showLewisStructure();
    } else {
      alphaSpin.setVisible(false);
      betaSpin.setVisible(false);
      dialog.doSetStructure("alpha");
    }

    optionBox.setVisible(true);
    if (keywordID > 0 && isNewModel) {
      keywordClicked(keywordID);
    }
  }
//
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

  ////////////////////////// SEARCH POSTS TO NBO ///////////////////

  /**
   * 
   * @param cmd_basis
   *        a possibly space-separated set of CMD and
   * @param cb
   */
  private void postListRequest(String cmd_basis, JComboBox<String> cb) {
    int mode = MODE_SEARCH_LIST;
    SB sb = getMetaHeader(false);
    String cmd;
    if (keywordID == KEYWD_OPBAS || keywordID == KEYWD_BAS1BAS2) {
      cmd = "LABEL";
      JComboBox<String> tmpBas = ((cmd_basis.startsWith("c") && keywordID == KEYWD_BAS1BAS2) ? comboBasis2
          : comboBasis1);
      NBOUtil.postAddGlobalI(sb, "BAS_1", 1, tmpBas);
      mode = MODE_SEARCH_LIST_LABEL;
    } else {
      NBOUtil.postAddGlobalI(sb, "BAS_1", 1, comboBasis1);
      NBOUtil.postAddGlobalI(sb, "KEYWORD", keywordID, null);
      cmd = cmd_basis.split(" ")[0];
    }
    NBOUtil.postAddCmd(sb, cmd);
    if (keywordID == KEYWD_CMO && cmd_basis.equals("c_cmo"))
      mode = MODE_SEARCH_LIST_MO;
    postNBO_s(sb, mode, cb, "Getting list " + cmd, false);
  }

  /**
   * Post a request to NBOServe with a callback to processNBO.
   * 
   * @param sb
   *        command data
   * @param mode
   *        type of request
   * @param cb
   *        optional JComboBox to fill
   * @param statusMessage
   * @param isGetValue 
   */
  private void postNBO_s(SB sb, final int mode, final JComboBox<String> cb,
                         String statusMessage, boolean isGetValue) {
    final NBORequest req = new NBORequest();
    req.set(new Runnable() {
      @Override
      public void run() {
        processNBO(req, mode, cb);
      }
    }, isGetValue, statusMessage, (mode == MODE_SEARCH_LIST_LABEL ? "v_cmd.txt" : "s_cmd.txt"), sb.toString());
    dialog.nboService.postToNBO(req);
  }

  /**
   * Do something in response to a reply that includes a * by an orbital's name,
   * indicating that it is a maximum value -- perhaps display it? Briefly?
   * 
   * @param line
   */
  protected void showMax(String line) {
    //BH not implemented?  
  }

  /**
   * Process the reply from NBOServe.
   * 
   * @param req
   * @param mode
   * @param cb
   */
  protected void processNBO(NBORequest req, int mode, JComboBox<String> cb) {
    String[] lines = req.getReplyLines();
    String line;
    DefaultComboBoxModel<String> list;
    switch (mode) {
    case MODE_SEARCH_VALUE:
      line = lines[0];
      if (dialog.isOpenShell()) {
        String spin = (isAlphaSpin() ? "&uarr;" : "&darr;");
        int ind = line.indexOf(')') + 1;
        line = line.substring(0, ind) + spin + line.substring(ind);
      }
      dialog.logValue(" " + line);
      if (line.contains("*"))
        showMax(line);
      break;
    case MODE_SEARCH_LIST_LABEL:
    case MODE_SEARCH_LIST:
      list = (DefaultComboBoxModel<String>) cb.getModel();
      list.removeAllElements();
      if (cb == comboAtom1 || cb == comboAtom2)
        list.addElement("<select an atom>");
      else if (cb == comboSearchOrb1 || cb == comboSearchOrb2)
        list.addElement("<select an orbital>");
      for (int i = 0; i < lines.length; i++)
        list.addElement(lines[i]);
      processReturnedSearchList(cb);
      break;
    case MODE_SEARCH_LIST_MO:
      list = (DefaultComboBoxModel<String>) cb.getModel();
      for (int i = 0; i < lines.length; i++)
        list.addElement("  " + PT.rep(PT.rep(lines[i], "MO ", ""), " ", ".  "));
      break;
    case MODE_SEARCH_BOND_VALUES:
    case MODE_SEARCH_ATOM_VALUES:
      int i0 = -1;
      for (int i = lines.length; --i >= 0 && lines[i].indexOf("END") < 0;) {
        i0 = i;
      }
      SB sbLog = new SB();
      SB sb = new SB();
      for (int i = i0, pt = 1; i < lines.length; i++, pt++)
        if (!processSearchLabel(sbLog, sb, lines[i], pt, mode))
           break;
      dialog.log(sbLog.toString(), 'b');
      dialog.runScriptQueued(sb.toString() + ";select none;");
      break;
    }
  }

  private boolean processSearchLabel(SB sbLog, SB sb, String line, int count, int mode) {
    switch (mode) {
    case MODE_SEARCH_ATOM_VALUES:
      float v = PT.parseFloat(line);
      if (Float.isNaN(v)) {
        System.out
            .println("SEARCH: atomic value list processing ended unexpectedly!");
        return false;
      }
      Atom atom = vwr.ms.at[count - 1];
      sb.append(";select @" + count + ";set fontscaling off;label "
          + NBOUtil.round(v, 3) + ";set fontscaling on;");
      sb.append(";font label " + (atom.getElementNumber() == 1 ? 6 : 10) + ";");
      line = atom.getAtomName() + " " + line.trim();
      break;
    case MODE_SEARCH_BOND_VALUES:
      String[] toks = PT.getTokens(line);
      float order = Float.NaN;
      int atom1 = 0, atom2 = 0;
      if (toks.length == 3) {
        atom1 = PT.parseInt(toks[0]);
        atom2 = PT.parseInt(toks[1]);
        order = PT.parseFloat(toks[2]);
      }
      if (Float.isNaN(order)) {
        System.out
            .println("SEARCH: bond value list processing ended unexpectedly!");
        return false;
      }
      if (order < 0.01)
        return true;
      sb.append("font measures 20; measure id 'm" + atom1 + "_" + atom2 + "' @"
          + atom1 + " @" + atom2 + " radius 0.0 \"" + NBOUtil.round(order, 4)
          + "\";");
      line = vwr.ms.at[atom1 - 1].getAtomName() + " "
          + vwr.ms.at[atom2 - 1].getAtomName() + " " + order;
      break;
    }
    sbLog.append(line.trim() + "<br>");
    return true;
  }

}
