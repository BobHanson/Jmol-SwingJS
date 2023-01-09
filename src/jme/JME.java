package jme;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import javajs.util.Rdr;

// ----------------------------------------------------------------------------
// ****************************************************************************
public class JME extends JPanel
    implements MouseListener, KeyListener, MouseMotionListener {

  private JFrame myFrame;

  Point aboutBoxPoint = new Point(500, 10);
  Point smilesBoxPoint = new Point(200, 50);
  Point atomxBoxPoint = new Point(150, 420);
  JTextField atomicSymbol = new JTextField("H"); // pouziva sa v JME

  // editor state
  int action;
  int active_an;
  boolean application = false;
  //static String separator = System.getProperties().getProperty("line.separator");
  static String separator = "\n";

  // customization
  static final String version = "2013.01";
  String infoText = "JME Molecular Editor by Peter Ertl, Novartis";
  int sd = 24;
  int arrowWidth = 24 * 2;
  static Color bgColor = Color.lightGray;
  static Color brightColor = bgColor.brighter();
  Font font, fontBold, fontSmall;
  FontMetrics fontMet, fontBoldMet, fontSmallMet;
  int fontSize; // use in depict
  // tieto parametere sa naplnaju v init (aby sa vynulovali pri starte)
  boolean bwMode = false;
  boolean runsmi = false; // traba to, alebo sa automaticky setne v param smi ??
  String depictcgi = null;
  String depictservlet = null;

  boolean canonize = true;
  boolean stereo = true;
  boolean multipart = false; // vzdy aj pri reaction
  boolean xButton = true;
  boolean rButton = false;
  boolean showHydrogens = true;
  boolean query = false;
  boolean reaction = false;
  boolean autoez = false;
  boolean writesmi = false;
  boolean writemi = false;
  boolean writemol = false;
  boolean number = false;
  boolean star = false;
  boolean autonumber = false;
  boolean jmeh = false;
  boolean depict = false;
  boolean depictBorder = false;
  boolean keepHydrogens = true;
  Color canvasBg = Color.white;
  String atomColors = null; // atom coloring
  String atomBgColors = null; // background coloring
  double depictScale = 1.; // ked scaling viacero mols, alebo reaction
  boolean nocenter = false;
  boolean polarnitro = false;
  boolean showAtomNumbers = false; // only when starting with a molecule
  // scaling pri depict, nacitanie molekul (jme + mol)

  static final Color color[] = new Color[23];
  static final String zlabel[] = new String[23];

  // files na nacitanie (2002.06)
  String smiles = null;
  String jmeString = null;
  String molString = null;

  // pouziva v double bufferingu
  Dimension dimension;
  Image topMenu, leftMenu, infoArea, molecularArea;

  // pre repaint()
  boolean doMenu = true; // ci draw menu pri repaint()
  boolean movePossible; // not to move when dragg in menu

  // actions 2 riadky s ACTIONX a atomy s ACTIONA
  static final int ACTIONX = 12;
  static int ACTIONA = 10; // meni sa podla rxButton
  // cislo action urcuje aj polohu buttonu
  // empty buttons (a ACTION_END v aplete) su vyradene v mousePressed()
  // a kreslenie textov v createSquare (neda sa to v jednom ?) 
  static final int ACTION_DELETE = 104;
  static final int ACTION_MARK = 105;
  static final int ACTION_DELGROUP = 106;
  static final int ACTION_SMI = 101;
  static final int ACTION_END = 111;
  static final int ACTION_QRY = 107;
  static final int ACTION_UNDO = 110;
  static final int ACTION_REACP = 109;
  static final int ACTION_CLEAR = 102;
  static final int ACTION_NEW = 103; // using newMolecule
  static final int ACTION_JME = 112;
  static final int ACTION_PGUP = 151;
  static final int ACTION_PGDN = 152;
  static final int ACTION_ROT90 = 156; // webme
  static final int ACTION_CHARGE_PLUS = 157; // webme
  static final int ACTION_CHARGE_MINUS = 158; // webme

  static final int ACTION_CHARGE = 108;
  static final int ACTION_STEREO = 201;
  static final int ACTION_BOND_SINGLE = 202;
  static final int ACTION_BOND_DOUBLE = 203;
  static final int ACTION_BOND_TRIPLE = 204;
  static final int ACTION_CHAIN = 205;
  static final int ACTION_RING_3 = 206;
  static final int ACTION_RING_4 = 207;
  static final int ACTION_RING_5 = 208;
  static final int ACTION_RING_PH = 209;
  static final int ACTION_RING_6 = 210;
  static final int ACTION_RING_7 = 211;
  static final int ACTION_RING_8 = 212;
  static final int ACTION_RING_FURANE = 221; // nema button
  static final int ACTION_RING_3FURYL = 223; // Alt 0
  static final int ACTION_RING_9 = 229; // nema button
  static final int ACTION_TEMPLATE = 230;
  static final int ACTION_GROUP_TBU = 233;
  static final int ACTION_GROUP_NITRO = 234;
  static final int ACTION_GROUP_COO = 235;
  static final int ACTION_GROUP_CF3 = 236;
  static final int ACTION_GROUP_CCL3 = 237;
  static final int ACTION_GROUP_CC = 238;
  static final int ACTION_GROUP_SULFO = 239;
  static final int ACTION_GROUP_COOME = 240;
  static final int ACTION_GROUP_OCOME = 241;
  static final int ACTION_GROUP_CYANO = 242;
  static final int ACTION_GROUP_NME2 = 243;
  static final int ACTION_GROUP_NHSO2ME = 244;
  static final int ACTION_GROUP_CCC = 245;
  static final int ACTION_GROUP_C2 = 246;
  static final int ACTION_GROUP_C3 = 247;
  static final int ACTION_GROUP_C4 = 248;
  static final int ACTION_GROUP_COH = 249;
  static final int ACTION_GROUP_dO = 250; // =O
  static final int ACTION_GROUP_PO3H2 = 251;
  static final int ACTION_GROUP_SO2NH2 = 252;
  static final int ACTION_GROUP_TEMPLATE = 253;
  static final int ACTION_GROUP_CF = 254;
  static final int ACTION_GROUP_CL = 255;
  static final int ACTION_GROUP_CB = 256;
  static final int ACTION_GROUP_CI = 257;
  static final int ACTION_GROUP_CN = 258;
  static final int ACTION_GROUP_CO = 259;
  static final int ACTION_GROUP_MAX = 260; // last+1 len na < test
  static final int ACTION_AN_C = 301;
  static final int ACTION_AN_N = 401;
  static final int ACTION_AN_O = 501;
  static final int ACTION_AN_S = 601;
  static final int ACTION_AN_F = 701;
  static final int ACTION_AN_CL = 801;
  static final int ACTION_AN_BR = 901;
  static final int ACTION_AN_I = 1001;
  static final int ACTION_AN_P = 1101;
  static final int ACTION_AN_X = 1201;
  static final int ACTION_AN_H = 1300;
  static final int ACTION_AN_R = 1301;
  static final int ACTION_AN_R1 = 1302;
  static final int ACTION_AN_R2 = 1303;
  static final int ACTION_AN_R3 = 1304;
  static final int AN_H = 1;
  static final int AN_B = 2;
  static final int AN_C = 3;
  static final int AN_N = 4;
  static final int AN_O = 5;
  static final int AN_SI = 6;
  static final int AN_P = 7;
  static final int AN_S = 8;
  static final int AN_F = 9;
  static final int AN_CL = 10;
  static final int AN_BR = 11;
  static final int AN_I = 12;
  static final int AN_SE = 13;
  static final int AN_X = 18;
  static final int AN_R = 19;
  static final int AN_R1 = 20;
  static final int AN_R2 = 21;
  static final int AN_R3 = 22;

  // info about last action & undo
  int lastAction = 0; // trva len po mouse up
  static final int LA_BOND = 1;
  static final int LA_RING = 2;
  static final int LA_GROUP = 3;
  static final int LA_MOVE = 5;
  static final int LA_FAILED = 9; // failed to create bond or ring
  boolean newMolecule = false; // enable to start new molecule
  int xold, yold; // position of mousePressed, updated in mouseDragged
  boolean afterClear = false; // info pre undo
  boolean mouseShift = false; // kvoli numbering

  MultiBox smilesBox = null, atomxBox = null, aboutBox = null;
  QueryBox queryBox;
  Point point = new Point(20, 200);
  JButton c, n, o, s, p, f, cl, br, i, any, anyec, halogen, aromatic,
  nonaromatic, ring, nonring;
  JButton anyBond, aromaticBond, ringBond, nonringBond, sdBond;
  JComboBox<String> choiced, choiceh;
  boolean dyMode = true;
  String molText = null;
  //JMEmol mol = new JMEmol(this); // sposobovalo problemy v NS
  JMEmol mol;
  int nmols = 0;
  int actualMolecule = 0;
  int saved = 0; // ktora molekula je saved pri multipart
  String template = null; // template as jme string
  JMEmol tmol = null; // template molecule
  JMEmol mols[] = new JMEmol[99]; // when multipart, nealokuje !! 
  JMEmol smol; // save
  //static Color[] psColor;
  static Color[] psColor = new Color[7];
  List<JMEmol> molStack = new ArrayList<JMEmol>();
  int stackPointer = -1;
  boolean doTags = false; // compatibility with JMEPro
  boolean webme = false; // compatibility with JMEPro
  public int[] apointx, apointy, bpointx, bpointy; // coordinates for webme
  boolean revertStereo = false; // down stereo bond (only 1 action)
  boolean relativeStereo = false;
  boolean allHs = false;
  // for key marking 2009.04
  boolean markUsed = true;
  int currentMark = 1;

  // images
  Image infoImage, clearImage, deleteImage, deleterImage, chargeImage;
  Image templatesImage, rtemplatesImage, undoImage, endImage, smiImage,
      smitImage, smartsImage, stereoImage, stereoxImage;
  private boolean embedded;

  // ----------------------------------------------------------------------------

  public JME(JFrame frame, boolean embedded) {
    if (frame != null) {
      myFrame = frame;
      frame.add("Center", this);
      frame.addKeyListener(this);
      application = true;
    }
    this.embedded = embedded;

    mol = new JMEmol(this);
    psColor[0] = Color.gray;
    psColor[1] = new Color(255, 153, 153); // pastel red
    psColor[2] = new Color(255, 204, 102);
    psColor[3] = new Color(255, 255, 153);
    psColor[4] = new Color(102, 255, 255);
    psColor[5] = new Color(51, 204, 255);
    psColor[6] = new Color(255, 153, 255);
    init();
    start();
  }

  // ----------------------------------------------------------------------------
  public static void main(String args[]) {
    JFrame frame = new JFrame("Jmol/JME 2D Molecular Editor");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    newJME(args, frame, false);
  }

  private static JME newJME(String[] args, JFrame frame, boolean embedded) {
    JME jme = new JME(frame, embedded);
    //frame.setSize(24*18,24*16); // urcuje dimensions pre aplikaciu
    int w = 24 * 18;
    int h = 24 * 16;
    frame.setBounds(300, 200, w, h); // urcuje dimensions pre aplikaciu
    if (args.length == 1)
      jme.options(args[0]);
    frame.setVisible(true);//.setVisible(true);
    // po frame.show, aby boli zname dimension
    // reads molecule (from 2008.12)
    String fileName = null;
    for (int i = 0; i < args.length; i++) {
      if ("-embedded".equals(args[i])) {
        jme.embedded = true;
      } else if ("-s".equals(args[i])) {
      } else if (args[i].startsWith("-f")) {
        fileName = args[++i];
      } else if (args[i].startsWith("-o")) {
        jme.options(args[++i]);
      }
    }
    if (fileName != null) {
      jme.dimension = jme.getSize();
      try {
        jme.readMolecule(Rdr.streamToString(new FileInputStream(fileName)));
      } catch (IOException e) {
        System.err.println("File " + fileName + " could not be read");
      }
    }
    return jme;
  }

  // --------------------------------------------------------------------------
  public Color getColor() {
    return bgColor; // it may be used to color other stuff with mi colors
  }

  // --------------------------------------------------------------------------
  public void activateQuery() {
    // v JME menu nastavi na query (ak bolo medzitym ine)
    if (action != JME.ACTION_QRY) {
      action = ACTION_QRY;
      repaint();
    }
  }

  // ----------------------------------------------------------------------------
  private void init() {
    // tu su veci co suvisia s grafikou

    addMouseListener(this);
    addMouseMotionListener(this);

    dimension = getSize(); // potrebne pre centrovanie nacitanej molekuly

    setLayout(null);

    // NS3 ma error vo font metrics (nedava ascent)
    fontSize = 12;
    if (font == null) { // kvoli IE, ktory aj pri Back vola init
      font = new Font("Helvetica", Font.PLAIN, fontSize);
      fontMet = getFontMetrics(font);
    }
    if (fontBold == null) { // kvoli IE, ktory aj pri Back vola init
      fontBold = new Font("Helvetica", Font.BOLD, fontSize);
      fontBoldMet = getFontMetrics(fontBold);
    }

    //int fs = fontSize-1;
    int fs = fontSize;
    if (fontSmall == null) {
      fontSmall = new Font("Helvetica", Font.PLAIN, fs);
      fontSmallMet = getFontMetrics(fontSmall);
    }

    // este aj tu aby sa vzdy iniciovali pre reload
    // ??? urobit to lepsie
    query = false;
    reaction = false;
    autoez = false;
    stereo = true;
    canonize = true;
    xButton = true;
    rButton = false;
    ACTIONA = 10;
    showHydrogens = true;

    //    if (!application) {
    //      try { // chytanie exception kvoli startu z ineho appletu
    //        String options = getParameter("options");
    //        if (options != null)
    //          options(options);
    //        String jme = getParameter("jme");
    //        if (jme != null)
    //          jmeString = jme;
    //        String molf = getParameter("mol");
    //        if (molf != null)
    //          molString = molf;
    //        String dc = getParameter("depictcgi");
    //        if (dc != null) {
    //          depictcgi = dc;
    //          runsmi = true;
    //        } // sets runsmi, pred smi
    //        // v parametroch depictcgi PRED smiles, upravit ????
    //        String s = getParameter("smiles");
    //        if (s != null)
    //          smiles = s;
    //        String mt = getParameter("text");
    //        if (mt != null) {
    //          molText = mt;
    //          repaint();
    //        }
    //        atomColors = getParameter("atomcolors"); // only 1 of these 2
    //        atomBgColors = getParameter("atombg");
    //        String bc = getParameter("depictbg");
    //        if (bc != null && depict)
    //          canvasBg = parseHexColor(bc);
    //
    //        if (showAtomNumbers)
    //          showAtomNumbers();
    //      } catch (Exception e) {
    //        //System.err.println("JME:no parameters");
    //      }
    //    }

    action = ACTION_BOND_SINGLE; // musi to tu but, inak nic
    atomicData();
    validate();
  }

  // ----------------------------------------------------------------------------
  //  private Color parseHexColor(String hex) {
  //    Color c = Color.white;
  //    try {
  //      if (!hex.startsWith("#"))
  //        throw new Exception("bad hex encoding");
  //      int r = Integer.parseInt(hex.substring(1, 3), 16);
  //      int g = Integer.parseInt(hex.substring(3, 5), 16);
  //      int b = Integer.parseInt(hex.substring(5, 7), 16);
  //      c = new Color(r, g, b);
  //      return c;
  //    } catch (Exception e) {
  //      System.err.println("Problems in parsing background color " + hex);
  //      return c;
  //    }
  //  }

  // ----------------------------------------------------------------------------
  public void start() {
    // System.err.println("start");
    // cita molekuly (uz by malo poznat dimension)
    //addNotify(); // ??? nekompatibilne z mipc
    dimension = getSize();
    if (jmeString != null) {
      readMolecule(jmeString);
      // co s coloring multipart a reactions ???
      // only 1 coloring scheme (atoms || bg) may be applied
      if (atomBgColors != null && mol != null)
        mol.setAtomColors(atomBgColors, true);
      if (atomColors != null && mol != null)
        mol.setAtomColors(atomColors, false);
    } else if (molString != null)
      readMolFile(molString); // coloring tam
    //else if (smiles != null) readSmiles(smiles);
    // toto musi byt after vytvotrenie mol, aby bolo dimenzovane
  }

  // ----------------------------------------------------------------------------
  public void stop() {
    //System.err.println("stop");
    if (smilesBox != null)
      smilesBox.dispose();
    if (atomxBox != null)
      atomxBox.dispose();
    if (aboutBox != null)
      aboutBox.dispose();
    if (queryBox != null)
      queryBox.dispose();
    mols = null; // memory leak ?
  }

  // ----------------------------------------------------------------------------
  // --- public functions -------------------------------------------------------
  // ----------------------------------------------------------------------------
  // for JavaScript to establish connection with JME
  public void ping() {
  }

  // ----------------------------------------------------------------------------
  public String smiles() {
    String smiles = Smiles();
    repaint(); // aby ked je chyba v smilesi (stereo) aby sa objavilo info
    return smiles;
  }

  // ----------------------------------------------------------------------------
  public String nonisomericSmiles() {
    boolean originalStereo = stereo;
    stereo = false;
    String smiles = Smiles();
    stereo = originalStereo;
    repaint(); // aby ked je chyba v smilesi, aby sa objavilo info
    return smiles;
  }

  // ----------------------------------------------------------------------------
  String Smiles() {
    String s;
    if (reaction)
      s = partSmiles(1) + ">" + partSmiles(2) + ">" + partSmiles(3);
    else {
      s = partSmiles(0);
      if (s.length() > 0) {
        molStack.add(new JMEmol(mol)); // adding molecule to stack
        // skoci na koniec s molsack pointer
        stackPointer = molStack.size() - 1;
      }
    }
    return s;
  }

  // ----------------------------------------------------------------------------
  String partSmiles(int pp) {
    // vracia multipart smiles, ak pp != 0 (reaction) iba pre tu part
    // neskor pridat sort jednotlivych smilesov (alfanumeric)
    String s = "";
    for (int m = 1; m <= nmols; m++) {
      if (pp > 0) {
        int p = mols[m].reactionPart();
        if (p != pp)
          continue;
      }
      String smiles = mols[m].createSmiles();
      if (smiles.length() > 0) {
        if (s.length() > 0)
          s += ".";
        s += smiles; // ta molekula moze byt empty
      }
    }
    return s;
  }

  // ----------------------------------------------------------------------------
  public void reset() {
    // volane zvonka - vymaze vsetko
    action = ACTION_BOND_SINGLE;
    newMolecule = false;
    nmols = 0;
    //JMEmol.maxMark = 0;
    actualMolecule = 0;
    mol = new JMEmol(this); // treba
    mol.maxMark = 0;
    molText = null;
    depictScale = 1.; // ??? ked depict viac molekul po sebe
    repaint();
  }

  // ----------------------------------------------------------------------------
  public void clear() {
    // zmaze actualMolecule, zmensi pocet molekul, actual bude najvyssia
    action = ACTION_BOND_SINGLE;
    newMolecule = false;

    if (nmols == 0)
      return;
    mol.save();
    afterClear = true;
    for (int i = actualMolecule; i < nmols; i++)
      mols[i] = mols[i + 1];
    nmols--;
    actualMolecule = nmols;
    if (nmols > 0)
      mol = mols[actualMolecule]; // kvoli move
    else {
      mol = new JMEmol(this);
      mol.maxMark = 0;
    }

  }

  // ----------------------------------------------------------------------------
  public String jmeFile() {
    // returns molecule(s) in jme format
    String s = "";
    if (reaction)
      s = partJme(1) + ">" + partJme(2) + ">" + partJme(3);
    else
      s = partJme(0);
    return s;
  }

  // ----------------------------------------------------------------------------
  String partJme(int pp) {
    // vracia multipart jme, ak pp != 0 (reaction) iba pre tu part
    // neskor pridat sort jednotlivych smilesov (alfanumeric)
    String s = "";
    for (int m = 1; m <= nmols; m++) {
      if (pp > 0) {
        int p = mols[m].reactionPart();
        if (p != pp)
          continue;
      }
      String jme = mols[m].createJME();
      if (jme.length() > 0) {
        if (s.length() > 0)
          s += "|";
        s += jme; // ta molekula moze byt empty
      }
    }
    return s;
  }

  // ----------------------------------------------------------------------------
  int[][] getReactionParts() {
    // returns fields of indices for reactants, products and modulators
    int part[][] = new int[4][nmols + 1];
    for (int p = 1; p <= 3; p++) {
      int np = 0;
      for (int m = 1; m <= nmols; m++)
        if (mols[m].reactionPart() == p)
          part[p][++np] = m;
      part[p][0] = np;
    }
    return part;
  }

  // ----------------------------------------------------------------------------
  // makos
  public void readMolecule(String molecule) {
    // spracuva aj multipart a reactions (aj chybu R>P miesto R>>P)
    // input v JME format
    reset();
    int lastReactant = 0, firstProduct = 0;

    StringTokenizer st = new StringTokenizer(molecule, "|>", true);
    boolean isReaction = (molecule.indexOf(">") > -1);
    int rx = 1; // pocita >

    int nt = st.countTokens();
    nmols = 0;
    for (int i = 1; i <= nt; i++) {
      String s = st.nextToken();
      s.trim();
      if (s.equals("|"))
        continue;
      if (s.equals(">")) {
        rx++;
        if (rx == 2)
          lastReactant = nmols;
        else if (rx == 3)
          firstProduct = nmols + 1;
        continue;
      }
      mol = new JMEmol(this, s, true);
      if (mol.natoms == 0) {
        info("ERROR - problems in reading/processing molecule !");
        System.err.println("ERROR while processing\n" + s);
        continue;
      }
      // vsetko ok - preberie ju do editora
      nmols++; // moze byt aj multipart
      actualMolecule = nmols;
      mols[nmols] = mol;
      //newMolecule = false;
      smol = null; // kvoli undo
    }

    // --- chyba v zadani reakcie (zly pocet >)
    if (rx == 2) {
      firstProduct = lastReactant + 1;
      info("ERROR - strange reaction - fixing !");
      System.err
          .println("ERROR - reactant and product should be separated by >>\n");
    } else if (rx > 3) {
      info("ERROR - strange reaction !");
      System.err.println("ERROR - strange reaction !\n");
      return;
    }

    if (nmols > 1 && !isReaction)
      options("multipart");
    if (isReaction && !reaction)
      options("reaction");
    if (!isReaction && reaction)
      options("noreaction");

    if (!isReaction)
      alignMolecules(1, nmols, 0);
    else {
      alignMolecules(1, lastReactant, 1);
      alignMolecules(lastReactant + 1, firstProduct - 1, 2);
      alignMolecules(firstProduct, nmols, 3);
    }
    repaint();
  }

  // ---------------------------------------------------------------------------- 
  // adding template from JavaScript
  // template menu is actually JME string
  public void setTemplate(String t, String name) {
    //clear();
    afterClear = false; // otherwise problems in undo
    tmol = new JMEmol(this, t, true); // defined globally
    tmol.complete();
    // now waiting for atom or free space click
    action = ACTION_GROUP_TEMPLATE;

    //mol.center();
    //nmols = 1; actualMolecule = 1; mols[1] = mol;

    info(name);
    repaint(); // needed to display status line
  }

  // -------------------------------------------------------------------------- 
  void alignMolecules(int m1, int m2, int part) {

    if (nocenter)
      return; // aj pre depict ???
    int nm = m2 - m1 + 1;
    if (nm <= 0 || m1 > nmols || m2 > nmols)
      return;

    double center[] = new double[4];

    int RBOND = JMEmol.RBOND;
    double[] share = new double[99]; // share na 1 mol (used pri posune)
    double sumx = 0., sumy = 0., maxy = 0.;
    for (int i = m1; i <= m2; i++) {
      mols[i].centerPoint(center); // zisti dimenzie
      sumx += center[2];
      sumy += center[3];
      if (center[3] > maxy)
        maxy = center[3];
      share[i] = center[2];
      if (part == 2)
        share[i] = center[3];
    }

    // prida medzery (na oboch stranach a medzi), pri !depict to netreba 
    if (depict) {
      sumx += RBOND * (nm + 1);
      sumy += RBOND * (nm + 1);
      maxy += RBOND; // malo by byt * 2, ale potom su velke okraje
    }

    // niekedy moze byt nulova
    if (dimension.width == 0 || dimension.height == 0)
      dimension = getSize();
    // ??? od tychto nezavisi, ale musia tu byt (ide toto 2x)
    if (dimension.width == 0)
      dimension.width = 400;
    if (dimension.height == 0)
      dimension.height = 300;

    double scalex = 1., scaley = 1.;
    int xsize = dimension.width;
    int ysize = dimension.height;
    if (!depict) {
      xsize -= sd;
      ysize -= 3 * sd;
    } // ???
    if (part == 1 || part == 3)
      xsize = (xsize - arrowWidth) / 2;
    else if (part == 2)
      ysize = ysize / 2;
    if (sumx >= xsize)
      scalex = (xsize) / sumx;
    if (maxy >= ysize)
      scaley = (ysize) / maxy;

    double medzera = 0.;
    if (depict) { // cize == 1.
      depictScale = Math.min(scalex, scaley); // inak dS = 1.
      medzera = RBOND * xsize / sumx;
      if (part == 2)
        medzera = RBOND * ysize / sumy;
    }

    for (int i = m1; i <= m2; i++) {
      if (part == 2)
        share[i] = share[i] * ysize / sumy;
      else
        share[i] = share[i] * xsize / sumx;
    }

    double shiftx = -xsize / 2.;
    double shifty = 0.;
    if (part == 1)
      shiftx = -xsize - arrowWidth / 2.;
    else if (part == 3)
      shiftx = arrowWidth / 2.;
    else if (part == 2) {
      shiftx = 0.;
      shifty = -ysize;
    } // preco nie ..+2*sd ???

    for (int i = m1; i <= m2; i++) {

      // ??? toto sposobuje problemy depictScale = 0  ???
      if (depict) { // pri depicte zmensuje
        for (int a = 1; a <= mols[i].natoms; a++) {
          mols[i].x[a] *= depictScale;
          mols[i].y[a] *= depictScale;
        }
        mols[i].center(); // este raz, teraz uz zmensene
      }

      // pri depict urobi aj medzeru
      if (part == 2)
        shifty += (share[i] / 2. + medzera);
      else // part == 1, 3, or 0 
        shiftx += (share[i] / 2. + medzera);

      for (int a = 1; a <= mols[i].natoms; a++) {
        mols[i].x[a] += shiftx;
        mols[i].y[a] += shifty;
      }

      if (part == 2)
        shifty += share[i] / 2.;
      else
        shiftx += share[i] / 2.;

      if (!depict)
        mols[i].findBondCenters();
    }

  }

  // --------------------------------------------------------------------------
  public String molFile() {
    // creates mol file, multipart sd file or reaction (rxn file)
    String smiles = smiles(); // now, otherwise for multipart cuts them
    String s = "";
    if (reaction) {
      int part[][] = getReactionParts();
      // discarding modulators (if any)
      s += "$RXN" + separator + separator + separator + "JME Molecular Editor"
          + separator;
      s += JMEmol.iformat(part[1][0], 3) + JMEmol.iformat(part[3][0], 3)
          + separator;
      for (int i = 1; i <= part[1][0]; i++)
        s += "$MOL" + separator + mols[part[1][i]].createMolFile(smiles);
      for (int i = 1; i <= part[3][0]; i++)
        s += "$MOL" + separator + mols[part[3][i]].createMolFile(smiles);
    } else { // viac molekul do 1 mol file
      if (nmols > 1)
        mol = new JMEmol(this, mols, nmols);
      s = mol.createMolFile(smiles);
      if (nmols > 1)
        mol = mols[actualMolecule];
    }
    return s;
  }

  // --------------------------------------------------------------------------
  public void readMolFile(String s) {
    reset(); // set nmols = 0
    if (s.startsWith("$RXN")) { // reaction
      reaction = true;
      multipart = true;
      String separator = JMEmol.findSeparator(s);
      StringTokenizer st = new StringTokenizer(s, separator, true);
      String line = "";
      for (int i = 1; i <= 5; i++) {
        line = JMEmol.nextData(st, separator);
      }
      int nr = Integer.valueOf(line.substring(0, 3).trim()).intValue();
      int np = Integer.valueOf(line.substring(3, 6).trim()).intValue();
      JMEmol.nextData(st, separator); // 1. $MOL
      for (int p = 1; p <= nr + np; p++) {
        String m = "";
        while (true) {
          String ns = JMEmol.nextData(st, separator);
          if (ns == null || ns.equals("$MOL"))
            break;
          m += ns + separator;
        }
        //System.err.print("MOLS"+p+separator+m);
        mols[++nmols] = new JMEmol(this, m);
      }
      alignMolecules(1, nr, 1);
      alignMolecules(nr + 1, nr + np, 3);
    } else { // single molecule - ak multipart automaticky urobi multipart
      reaction = false;
      mol = new JMEmol(this, s);

      if (mol == null || mol.natoms == 0) {
        // 2008.12
        //info("ERROR - problems in reading/processing molecule !");
        //System.err.println("ERROR while processing\n"+s);
        return;
      }
      // coloring tu, inak pri multiupart problemy
      if (atomBgColors != null && mol != null)
        mol.setAtomColors(atomBgColors, true);
      if (atomColors != null && mol != null)
        mol.setAtomColors(atomColors, false);
      // ak multipart, urobi viac molekul
      int nparts = mol.checkMultipart(false);
      if (nparts == 1) {
        mols[++nmols] = mol;
      } else {
        multipart = true;
        for (int p = 1; p <= nparts; p++)
          mols[++nmols] = new JMEmol(this, mol, p); // aj vycentruje
      }
      actualMolecule = 1;
      mol = mols[actualMolecule]; // odstrani povodnu multipart mol
      //newMolecule = false;
      smol = null; // kvoli undo
      alignMolecules(1, nparts, 0);
    }

    repaint();
  }

  // --------------------------------------------------------------------------
  // called from JavaScript menu, sets Rgroup
  public void setSubstituent(String s) {
    // substituent menu
    int pressed = -1;
    if (s.equals("Select substituent")) {
      pressed = ACTION_BOND_SINGLE;
      s = "";
    } else if (s.equals("-C(=O)OH"))
      pressed = ACTION_GROUP_COO;
    else if (s.equals("-C(=O)OMe"))
      pressed = ACTION_GROUP_COOME;
    else if (s.equals("-OC(=O)Me"))
      pressed = ACTION_GROUP_OCOME;
    else if (s.equals("-CMe3"))
      pressed = ACTION_GROUP_TBU;
    else if (s.equals("-CF3"))
      pressed = ACTION_GROUP_CF3;
    else if (s.equals("-CCl3"))
      pressed = ACTION_GROUP_CCL3;
    else if (s.equals("-NO2"))
      pressed = ACTION_GROUP_NITRO;
    else if (s.equals("-NMe2"))
      pressed = ACTION_GROUP_NME2;
    else if (s.equals("-SO2-NH2"))
      pressed = ACTION_GROUP_SO2NH2;
    else if (s.equals("-NH-SO2-Me"))
      pressed = ACTION_GROUP_NHSO2ME;
    else if (s.equals("-SO3H"))
      pressed = ACTION_GROUP_SULFO;
    else if (s.equals("-PO3H2"))
      pressed = ACTION_GROUP_PO3H2;
    else if (s.equals("-C#N"))
      pressed = ACTION_GROUP_CYANO;
    else if (s.equals("-C#C-Me"))
      pressed = ACTION_GROUP_CCC;
    else if (s.equals("-C#CH"))
      pressed = ACTION_GROUP_CC;

    if (pressed > 0)
      menuAction(pressed);
    else
      s = "Not known group!";

    info(s);
    repaint();
  }

  // -------------------------------------------------------------------------- 
  public void options(String parameters) {
    parameters = parameters.toLowerCase();
    if (parameters.indexOf("norbutton") > -1)
      rButton = false;
    else if (parameters.indexOf("rbutton") > -1)
      rButton = true;
    if (parameters.indexOf("nohydrogens") > -1)
      showHydrogens = false;
    else if (parameters.indexOf("hydrogens") > -1)
      showHydrogens = true;
    if (parameters.indexOf("keephs") > -1)
      keepHydrogens = true;
    if (parameters.indexOf("removehs") > -1)
      keepHydrogens = false;
    if (parameters.indexOf("noquery") > -1)
      query = false;
    else if (parameters.indexOf("query") > -1)
      query = true;
    if (parameters.indexOf("noreaction") > -1)
      reaction = false;
    else if (parameters.indexOf("reaction") > -1)
      reaction = true;
    if (parameters.indexOf("noautoez") > -1)
      autoez = false;
    else if (parameters.indexOf("autoez") > -1)
      autoez = true;
    if (parameters.indexOf("nostereo") > -1)
      stereo = false;
    else if (parameters.indexOf("stereo") > -1)
      stereo = true;
    if (parameters.indexOf("nocanonize") > -1)
      canonize = false;
    else if (parameters.indexOf("canonize") > -1)
      canonize = true;
    if (parameters.indexOf("nomultipart") > -1)
      multipart = false;
    else if (parameters.indexOf("multipart") > -1)
      multipart = true;
    if (parameters.indexOf("nonumber") > -1) {
      number = false;
      autonumber = false;
    } else if (parameters.indexOf("number") > -1) {
      number = true;
      autonumber = false;
    }
    if (parameters.indexOf("autonumber") > -1) {
      autonumber = true;
      number = true;
    }
    if (parameters.indexOf("star") > -1) {
      star = true;
      number = true;
    }
    if (parameters.indexOf("polarnitro") > -1)
      polarnitro = true;
    if (parameters.indexOf("depict") > -1) {
      depict = true;
      sd = 0;
      molecularArea = null; // pre prechode z depict je ta primala
      // toto len pre norm mols, nie pre reaction !!!
      alignMolecules(1, nmols, 0);
    }
    if (parameters.indexOf("nodepict") > -1) {
      depict = false;
      // musi male molekuly vratit na povodnu velkost
      for (int i = 1; i <= nmols; i++) {
        mols[i].scaling();
        mols[i].center(); // este raz, teraz uz zmensene
      }
      depictScale = 1; // inak kresli mensi font
      // normal font (ak bola mensia molekula) sa nastavi v drawMolecularArea
      sd = 24;
      if (mol != null)
        mol.needRecentering = true;
    }
    if (parameters.indexOf("border") > -1) {
      depictBorder = true;
    }
    // undocumented options
    if (parameters.indexOf("writesmi") > -1)
      writesmi = true;
    if (parameters.indexOf("writemi") > -1)
      writemi = true;
    if (parameters.indexOf("writemol") > -1)
      writemol = true;
    if (parameters.indexOf("nocenter") > -1)
      nocenter = true;
    if (parameters.indexOf("jmeh") > -1)
      jmeh = true;
    if (parameters.indexOf("showan") > -1)
      showAtomNumbers = true;
    //System.out.println(rButton+" "+showHydrogens+" "+query+" "+autoez+" "+stereo+" "+canonize+" "+reaction);

    // zladi options - ake dalsie ???
    if (reaction) {
      number = true;
      multipart = true;
    }
    if (!depict)
      depictBorder = false;
    // positions and actions for X and Rx buttons
    // nove X a R action musia byt > 300
    if (rButton)
      ACTIONA++;
    repaint();
  }

  // --------------------------------------------------------------------------
  public void setText(String text) {
    molText = text;
    repaint();
  }

  // --------------------------------------------------------------------------
  public void showAtomNumbers() {
    // shows canonical atom numbering
    // numbers only actual molecule !!!
    if (mol != null)
      mol.numberAtoms();
  }

  // ----------------------------------------------------------------------------
  public boolean hasPrevious() {
    if (molStack.size() == 0 || stackPointer == 0)
      return false;
    return true;
  }

  // ----------------------------------------------------------------------------
  public void getPreviousMolecule() {
    getFromStack(-1);
  }

  // ----------------------------------------------------------------------------
  void getFromStack(int n) {
    info("");
    clear();
    // musi vytvorit kopiu, nie len brat poiner (lebo je zmeni)
    stackPointer += n;
    mol = new JMEmol(molStack.get(stackPointer));
    mol.complete();
    mol.center();
    nmols = 1;
    actualMolecule = 1;
    mols[1] = mol;
    repaint();
    smol = null; // kvoli undo
  }

  // ----------------------------------------------------------------------------
  // --- end of public functions ------------------------------------------------
  // ----------------------------------------------------------------------------
  @Override
  public void paint(Graphics g) {
    update(g);
    //requestFocus(); // kvoli key action
  }

  // ----------------------------------------------------------------------------
  @Override
  public void update(Graphics g) {
    // pri fill ma rectangle sirku a vysku presne, pri draw o 1 vacsiu    
    //Dimension d = getSize();
    Dimension d = getSize();
    if (dimension == null || (d.width != dimension.width)
        || (d.height != dimension.height) || molecularArea == null
        || infoArea == null) {
      // infoArea v if kvoli problemom s appletviewer
      dimension = d;
      // fix for bob hanson when starting JME very small
      int imagew = d.width - sd;
      int imageh = d.height - sd * 3;
      if (imagew < 1)
        imagew = 1;
      if (imageh < 1)
        imageh = 1;
      molecularArea = createImage(imagew, imageh);
      drawMolecularArea(g);
      if (depict)
        return;

      topMenu = createImage(d.width, sd * 2);
      drawTopMenu(g);
      imageh = d.height - sd * 2;
      if (imageh < 1)
        imageh = 1;
      leftMenu = createImage(sd, imageh);
      drawLeftMenu(g);
      infoArea = createImage(imagew, sd);
      drawInfo(g);
    } else { // robi len cast obrazku
      drawMolecularArea(g);
      if (depict)
        return;
      drawInfo(g); // ???
      if (doMenu) {
        drawTopMenu(g);
        drawLeftMenu(g);
      }
      doMenu = true;
    }
  }

  // ----------------------------------------------------------------------------
  static void atomicData() {
    for (int i = 0; i < 23; i++) {
      color[i] = Color.orange;
      zlabel[i] = "X";
    }
    zlabel[AN_H] = "H";
    color[AN_H] = Color.darkGray;
    zlabel[AN_B] = "B";
    color[AN_B] = Color.orange;
    zlabel[AN_C] = "C";
    color[AN_C] = Color.darkGray;
    zlabel[AN_N] = "N";
    color[AN_N] = Color.blue;
    zlabel[AN_O] = "O";
    color[AN_O] = Color.red;
    zlabel[AN_F] = "F";
    color[AN_F] = Color.magenta;
    zlabel[AN_CL] = "Cl";
    color[AN_CL] = Color.magenta;
    zlabel[AN_BR] = "Br";
    color[AN_BR] = Color.magenta;
    zlabel[AN_I] = "I";
    color[AN_I] = Color.magenta;
    zlabel[AN_S] = "S";
    color[AN_S] = Color.yellow.darker();
    zlabel[AN_P] = "P";
    color[AN_P] = Color.orange;
    zlabel[AN_SI] = "Si";
    color[AN_SI] = Color.darkGray;
    zlabel[AN_SE] = "Se";
    color[AN_SE] = Color.darkGray;
    zlabel[AN_X] = "X";
    color[AN_X] = Color.darkGray;
    zlabel[AN_R] = "R";
    color[AN_R] = Color.darkGray;
    zlabel[AN_R1] = "R1";
    color[AN_R1] = Color.darkGray;
    zlabel[AN_R2] = "R2";
    color[AN_R2] = Color.darkGray;
    zlabel[AN_R3] = "R3";
    color[AN_R3] = Color.darkGray;
  }

  // ----------------------------------------------------------------------------
  void drawMolecularArea(Graphics g) {
    Graphics2D og = (Graphics2D) molecularArea.getGraphics();
    og.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
    int imgWidth = dimension.width - sd;
    int imgHeight = dimension.height - sd * 3;
    og.setColor(canvasBg);
    og.fillRect(0, 0, imgWidth, imgHeight);

    for (int m = 1; m <= nmols; m++)
      mols[m].draw(og);

    if (!depict) {
      // vonkajsi okraj na pravej strane
      og.setColor(bgColor.darker());
      og.drawLine(imgWidth - 1, 0, imgWidth - 1, imgHeight - 1);
      // predel vo farbe backgroundu
      og.setColor(bgColor);
      og.drawLine(imgWidth - 2, 0, imgWidth - 2, imgHeight - 1);
      // svetly okraj dovnutra
      og.setColor(brightColor);
      og.drawLine(imgWidth - 3, 0, imgWidth - 3, imgHeight - 1);
    }

    // sipka ak ide o reaction
    if (reaction) {
      int pWidth = arrowWidth;
      int pStart = (imgWidth - pWidth) / 2;
      int m = arrowWidth / 8; // hrot sipky
      og.setColor(Color.magenta);
      og.drawLine(pStart, imgHeight / 2, pStart + pWidth, imgHeight / 2);
      og.drawLine(pStart + pWidth, imgHeight / 2, pStart + pWidth - m,
          imgHeight / 2 + m);
      og.drawLine(pStart + pWidth, imgHeight / 2, pStart + pWidth - m,
          imgHeight / 2 - m);
    }

    // molText
    if (depict) { // kvoli molText, ale aj depict dalsej molekuly
      // musi robit novy font, lebo v depict moze byt zmeneny
      font = new Font("Helvetica", Font.PLAIN, fontSize);
      fontMet = getFontMetrics(font);
      if (molText != null) {
        int w = fontMet.stringWidth(molText);
        int xstart = (int) Math.round((imgWidth - w) / 2.);
        int ystart = imgHeight - fontSize;
        og.setColor(Color.black);
        og.setFont(font);
        og.drawString(molText, xstart, ystart);
      }
    }

    g.drawImage(molecularArea, sd, sd * 2, this);
  }

  // ----------------------------------------------------------------------------
  void drawTopMenu(Graphics g) {
    Graphics og = topMenu.getGraphics();
    int imgWidth = dimension.width;
    int imgHeight = sd * 2;
    og.setColor(bgColor);
    og.fillRect(0, 0, imgWidth, imgHeight);

    og.setColor(bgColor.darker());
    og.drawLine(imgWidth - 1, 0, imgWidth - 1, imgHeight - 1); // right
    og.drawLine(0, imgHeight - 1, imgWidth - 1 - 2, imgHeight - 1); // bottom

    og.setColor(brightColor);
    og.drawLine(0, 0, imgWidth - 1, 0); // top
    og.drawLine(ACTIONX * sd, 0, ACTIONX * sd, imgHeight - 1); // predel

    for (int i = 1; i <= ACTIONX; i++) {
      createSquare(og, i, 1);
      createSquare(og, i, 2);
    }
    g.drawImage(topMenu, 0, 0, this);
  }

  // ----------------------------------------------------------------------------
  void drawLeftMenu(Graphics g) {
    Graphics og = leftMenu.getGraphics();
    int imgWidth = sd;
    int imgHeight = dimension.height - sd * 2;
    og.setColor(bgColor);
    og.fillRect(0, 0, imgWidth, imgHeight);
    og.setColor(brightColor);
    og.drawLine(0, 0, 0, imgHeight - 1); // left
    og.drawLine(0, ACTIONA * sd, imgHeight - 1, ACTIONA * sd); // predel
    og.setColor(bgColor.darker());
    og.drawLine(imgWidth - 1, 0, imgWidth - 1, imgHeight - 1 - sd); // right
    og.drawLine(0, imgHeight - 1, imgWidth - 1, imgHeight - 1); // bottom
    for (int i = 3; i <= ACTIONA + 2; i++)
      createSquare(og, 1, i);
    g.drawImage(leftMenu, 0, sd * 2, this);
  }

  // ----------------------------------------------------------------------------
  void drawInfo(Graphics g) {
    Graphics og = infoArea.getGraphics();
    int imgWidth = dimension.width - sd;
    int imgHeight = sd;
    og.setColor(bgColor);
    og.fillRect(0, 0, imgWidth, imgHeight);
    og.setColor(brightColor);
    og.drawLine(0, 0, imgWidth - 1 - 2, 0); // top
    og.setColor(bgColor.darker());
    og.drawLine(0, imgHeight - 1, imgWidth - 1, imgHeight - 1); // bottom
    og.drawLine(imgWidth - 1, 0, imgWidth - 1, imgHeight - 1); // right
    og.setFont(fontSmall);
    og.setColor(Color.black);
    if (infoText.startsWith("E"))
      og.setColor(Color.red);
    og.drawString(infoText, 10, 15);
    g.drawImage(infoArea, sd, dimension.height - sd, this);
  }

  // ----------------------------------------------------------------------------
  void menuAction(int pressed) {
    // calling actions after pressing menu button or menu keys
    // called from mousePressed() or keyTyped()
    if (pressed == 0)
      return; // moze to byt ? ano, napr z keyTyped

    int action_old = action;
    action = pressed;
    if (pressed <= 300) { // top menu
      switch (pressed) {
      case ACTION_CLEAR:
        clear();
        break;
      case ACTION_UNDO:
        // zostavaju rovnake settings ako predtym
        action = action_old;
        // undo po new molecule (pri new smol = null) 
        if (smol == null) {
          actualMolecule = nmols; // mohlo sa medzitym zmenit
          clear();
        } else if (afterClear) {
          saved = ++nmols;
          actualMolecule = nmols;
          afterClear = false;
        }
        // undo po standard change (aj po delete s upravenym saved)
        if (smol == null)
          break; // no molecule in undo stack
        mol = new JMEmol(smol);
        //mol = smol.createClone();
        mol.complete();
        mols[saved] = mol;
        break;
      case ACTION_PGDN:
        int ssize = molStack.size();
        action = action_old;
        if (ssize == 0)
          info("No molecules in molstack");
        else if (stackPointer == 0)
          info("Bottom of molstack reached");
        else
          getFromStack(-1);
        break;
      case ACTION_PGUP:
        ssize = molStack.size();
        action = action_old;
        if (ssize == 0)
          info("No molecules in molstack");
        else if (stackPointer == ssize - 1)
          info("Top of molstack reached");
        else
          getFromStack(1);
        break;
      case ACTION_SMI:
        if (smilesBox != null && smilesBox.isVisible()) {
          smilesBoxPoint = smilesBox.getLocationOnScreen();
          smilesBox.dispose();
          smilesBox = null;
        }
        smilesBox = new MultiBox(1, this);
        action = action_old;
        break;
      case ACTION_QRY:
        if (queryBox != null && queryBox.isVisible()) {
          point = queryBox.getLocationOnScreen();
          queryBox.dispose();
          queryBox = null;
        }
        queryBox = new QueryBox(this);

        // stay commented
        //action = action_old;
        break;
      case ACTION_JME:
        if (aboutBox != null && aboutBox.isVisible()) {
          aboutBoxPoint = aboutBox.getLocationOnScreen();
          aboutBox.dispose();
          aboutBox = null;
        }
        aboutBox = new MultiBox(0, this);
        action = action_old;
        break;
      case ACTION_NEW:
        newMolecule = true;
        action = action_old; // ak nie je bond alebo ring, malo by resetnut
        break;
      case ACTION_MARK:
        if (autonumber) {
          // autonumber added in 2009.09
          if (mouseShift) { // automark all atoms, zrusi stare mark
            mouseShift = false; // aby pridavalo cisla
            mol.numberAtoms();
            action = action_old;
          }
        }
        // set na action_mark
        currentMark = 1; // starts from 1
        break;
      case ACTION_END:
        if (embedded) {
          if (myFrame != null)
            myFrame.setVisible(false);
          return;
        }
        System.exit(0);
        break;
      case ACTION_REACP:
        // save ???
        action = action_old;
        int part = mol.reactionPart();
        if (part == 2) {
          info("Copying the agent not possible !");
          break;
        }
        double center[] = new double[4];
        mol.centerPoint(center);

        mol = new JMEmol(mol);
        //mol = mol.createClone(); // ???
        // posunie ju na spravne miesto
        int dx = (int) ((dimension.width - sd) / 2 - center[0]);
        for (int i = 1; i <= mol.natoms; i++)
          mol.x[i] += dx * 2;
        mol.complete();
        mols[++nmols] = mol;
        actualMolecule = nmols;

        break;
      case ACTION_DELETE:
        // 2011.01 if touchedAtom or bond, deletes it 
        if (mol.touchedAtom > 0) {
          mol.save();
          mol.deleteAtom(mol.touchedAtom);
          mol.touchedAtom = 0;
        } else if (mol.touchedBond > 0) {
          mol.save();
          mol.deleteBond(mol.touchedBond);
          mol.touchedBond = 0;
        }
        mol.valenceState(); // to add Hs
        break;
      default: // vsetky co nerobia okamzitu akcion (DEL, templates, +/-, ...)
        break;
      }
    } else { // pressed > 300 (left menu - atoms)
      switch (pressed) {
      case ACTION_AN_C:
        active_an = AN_C;
        break;
      case ACTION_AN_N:
        active_an = AN_N;
        break;
      case ACTION_AN_O:
        active_an = AN_O;
        break;
      case ACTION_AN_F:
        active_an = AN_F;
        break;
      case ACTION_AN_CL:
        active_an = AN_CL;
        break;
      case ACTION_AN_BR:
        active_an = AN_BR;
        break;
      case ACTION_AN_I:
        active_an = AN_I;
        break;
      case ACTION_AN_S:
        active_an = AN_S;
        break;
      case ACTION_AN_P:
        active_an = AN_P;
        break;
      case ACTION_AN_H:
        active_an = AN_H;
        break;
      case ACTION_AN_X:
        if (!webme) {
          if (atomxBox != null && atomxBox.isVisible()) {
            atomxBoxPoint = atomxBox.getLocationOnScreen();
            atomxBox.dispose();
            atomxBox = null;
          }
          if (mol.touchedAtom == 0)
            atomxBox = new MultiBox(2, this);
        }
        active_an = AN_X;
        break;
      case ACTION_AN_R:
        active_an = AN_R;
        break;
      case ACTION_AN_R1:
        active_an = AN_R1;
        break;
      case ACTION_AN_R2:
        active_an = AN_R2;
        break;
      case ACTION_AN_R3:
        active_an = AN_R3;
        break;
      }
      // 2009.09 if touchedAtom, changes it
      if (mol.touchedAtom > 0) {
        // copied, made subroutine !!!
        if (active_an != mol.an[mol.touchedAtom] && active_an != AN_X) {
          mol.save();
          mol.an[mol.touchedAtom] = active_an;
          mol.q[mol.touchedAtom] = 0; // resetne naboj
          mol.nh[mol.touchedAtom] = 0;
        }
        if (active_an == AN_X) {
          // MultiBox not atomxBox (this is static and always available, 
          // needed for key press)
          String xx = atomicSymbol.getText();
          mol.setAtom(mol.touchedAtom, xx);
        }
        mol.valenceState(); // to add Hs
      }
    }
    // repaintuje zbytocne vsetko - zatial nechat
    repaint();
  }

  // ----------------------------------------------------------------------------
  void createSquare(Graphics g, int xpos, int ypos) {
    int square = ypos * 100 + xpos;
    int xstart = (xpos - 1) * sd;
    int ystart = (ypos - 1) * sd;
    if (xpos == 1 && ypos > 2)
      ystart -= (2 * sd); // relative coordinates in leftMenu
    g.setColor(bgColor);
    if (square == action)
      g.fill3DRect(xstart + 1, ystart + 1, sd, sd, false);
    else
      g.fill3DRect(xstart, ystart, sd, sd, true);

    // treba, aby nekreslilo neaktivne buttons
    if (square == ACTION_AN_R && !rButton)
      return;

    if (square == ACTION_END && !application)
      return;
    if (square == ACTION_QRY && !query)
      return;
    if (square == ACTION_STEREO && !stereo)
      return;
    if (square == ACTION_NEW && !multipart)
      return;
    if (square == ACTION_MARK && !(number || autonumber))
      return;
    if (square == ACTION_REACP && !reaction)
      return;

    // draws icon or text in the square
    int m = sd / 4; // margin
    if (ypos < 3) { // top menu squares
      g.setColor(Color.black);
      switch (square) {
      case ACTION_SMI: // smiley face
        if (!bwMode) {
          g.setColor(Color.yellow);
          g.fillOval(xstart + 3, ystart + 3, sd - 6, sd - 6); // head
          g.setColor(Color.black);
        }
        g.drawOval(xstart + 3, ystart + 3, sd - 6, sd - 6); // head
        g.drawArc(xstart + 6, ystart + 6, sd - 12, sd - 12, -35, -110); // mouth
        // oci
        g.fillRect(xstart + 9, ystart + 9, 2, 4);
        g.fillRect(xstart + sd - 10, ystart + 9, 2, 4);
        // jazyk
        if (Math.random() < 0.04) {
          g.setColor(Color.red);
          g.fillRect(xstart + 10, ystart + 18, 4, 4);
        }
        // blink
        if (Math.random() > 0.96) {
          g.setColor(Color.yellow);
          g.fillRect(xstart + sd - 10, ystart + 8, 2, 3);
        }
        break;
      case ACTION_END:
        squareText(g, xstart, ystart, "END");
        break;
      case ACTION_QRY:
        g.setColor(Color.orange);
        g.fillRect(xstart + 4, ystart + 4, sd - 8, sd - 8); // head
        g.setColor(Color.black);
        g.drawRect(xstart + 4, ystart + 4, sd - 8, sd - 8); // head
        g.drawArc(xstart + 6, ystart + 6, sd - 11, sd - 12, -35, -110); // mouth
        g.fillRect(xstart + 9, ystart + 9, 2, 4);
        g.fillRect(xstart + sd - 10, ystart + 9, 2, 4);
        break;
      case ACTION_CHARGE:
        squareText(g, xstart, ystart, "+ /  ");
        g.drawLine(xstart + 15, ystart + 13, xstart + 19, ystart + 13); // better -
        break;
      case ACTION_UNDO:
        //g.drawArc(xstart+6,ystart+6,sd-12,sd-12,270,270); // head
        g.drawArc(xstart + 6, ystart + 7, sd - 12, sd - 14, 270, 270); // head
        g.drawLine(xstart + 6, ystart + 13, xstart + 3, ystart + 10);
        g.drawLine(xstart + 6, ystart + 13, xstart + 9, ystart + 10);
        //squareText(g,xstart,ystart,"UDO");
        break;
      case ACTION_REACP:
        g.drawLine(xstart + m, ystart + sd / 2, xstart + sd - m,
            ystart + sd / 2);
        g.drawLine(xstart + sd - m, ystart + sd / 2, xstart + sd - m * 3 / 2,
            ystart + sd / 2 + m / 2);
        g.drawLine(xstart + sd - m, ystart + sd / 2, xstart + sd - m * 3 / 2,
            ystart + sd / 2 - m / 2);
        break;
      case ACTION_CLEAR:
        g.setColor(Color.white);
        g.fillRect(xstart + 3, ystart + 5, sd - 7, sd - 11);
        g.setColor(Color.black);
        g.drawRect(xstart + 3, ystart + 5, sd - 7, sd - 11);
        //squareText(g,xstart,ystart,"CLR");
        break;
      case ACTION_NEW:
        // special handling (aby boli 2 stvorce on)
        g.setColor(bgColor);
        if (newMolecule)
          g.fill3DRect(xstart + 1, ystart + 1, sd, sd, false);
        g.setColor(Color.black);
        squareText(g, xstart, ystart, "NEW");
        break;
      case ACTION_DELGROUP:
        //squareText(g,xstart,ystart,"D-R");
        g.setColor(Color.red);
        g.drawLine(xstart + 7, ystart + 7, xstart + sd - 7, ystart + sd - 7);
        g.drawLine(xstart + 8, ystart + 7, xstart + sd - 6, ystart + sd - 7);
        g.drawLine(xstart + 7, ystart + sd - 7, xstart + sd - 7, ystart + 7);
        g.drawLine(xstart + 8, ystart + sd - 7, xstart + sd - 6, ystart + 7);
        g.setColor(Color.black);
        g.drawLine(xstart + m, ystart + sd / 2, xstart + 12, ystart + sd / 2);
        squareText(g, xstart + 6, ystart, "R");
        break;
      case ACTION_DELETE:
        //squareText(g,xstart,ystart,"DEL");
        g.setColor(Color.red);
        //g.drawLine(xstart+m,ystart+m,xstart+sd-m,ystart+sd-m);
        //g.drawLine(xstart+m+1,ystart+m,xstart+sd-m+1,ystart+sd-m);
        //g.drawLine(xstart+m,ystart+sd -m,xstart+sd-m,ystart+m);
        //g.drawLine(xstart+m+1,ystart+sd-m,xstart+sd-m+1,ystart+m);
        g.drawLine(xstart + 7, ystart + 7, xstart + sd - 7, ystart + sd - 7);
        g.drawLine(xstart + 8, ystart + 7, xstart + sd - 6, ystart + sd - 7);
        g.drawLine(xstart + 7, ystart + sd - 7, xstart + sd - 7, ystart + 7);
        g.drawLine(xstart + 8, ystart + sd - 7, xstart + sd - 6, ystart + 7);
        g.setColor(Color.black);
        break;
      case ACTION_MARK:
        if (star) {
          // star (not filled yet)
          g.setColor(Color.cyan);
          g.drawLine(xstart + 11, ystart + 5, xstart + 9, ystart + 9);
          g.drawLine(xstart + 9, ystart + 9, xstart + 4, ystart + 9);
          g.drawLine(xstart + 4, ystart + 9, xstart + 8, ystart + 12);
          g.drawLine(xstart + 8, ystart + 12, xstart + 6, ystart + 18);
          g.drawLine(xstart + 6, ystart + 18, xstart + 11, ystart + 15);

          g.drawLine(xstart + 12, ystart + 5, xstart + 14, ystart + 9);
          g.drawLine(xstart + 14, ystart + 9, xstart + 19, ystart + 9);
          g.drawLine(xstart + 19, ystart + 9, xstart + 15, ystart + 12);
          g.drawLine(xstart + 15, ystart + 12, xstart + 17, ystart + 18);
          g.drawLine(xstart + 17, ystart + 18, xstart + 12, ystart + 15);
          g.setColor(Color.black);
        } else
          squareText(g, xstart, ystart, "123");
        break;
      case ACTION_JME:
        //squareText(g,xstart,ystart,"JME");
        //g.drawImage(infoImage,xstart+2,ystart+2,this);
        g.setColor(Color.blue);
        g.fillRect(xstart + 4, ystart + 4, sd - 8, sd - 8);
        g.setColor(Color.black);
        g.drawRect(xstart + 4, ystart + 4, sd - 8, sd - 8);
        squareTextBold(g, xstart + 1, ystart - 1, Color.white, "i");
        break;
      case ACTION_STEREO:
        g.drawLine(xstart + m, ystart + sd / 2, xstart + sd - m,
            ystart + sd / 2 + 2);
        g.drawLine(xstart + m, ystart + sd / 2, xstart + sd - m,
            ystart + sd / 2 - 2);
        g.drawLine(xstart + sd - m, ystart + sd / 2 + 2, xstart + sd - m,
            ystart + sd / 2 - 2);
        break;
      case ACTION_BOND_SINGLE:
        g.drawLine(xstart + m, ystart + sd / 2, xstart + sd - m,
            ystart + sd / 2);
        break;
      case ACTION_BOND_DOUBLE:
        g.drawLine(xstart + m, ystart + sd / 2 - 2, xstart + sd - m,
            ystart + sd / 2 - 2);
        g.drawLine(xstart + m, ystart + sd / 2 + 2, xstart + sd - m,
            ystart + sd / 2 + 2);
        break;
      case ACTION_BOND_TRIPLE:
        g.drawLine(xstart + m, ystart + sd / 2, xstart + sd - m,
            ystart + sd / 2);
        g.drawLine(xstart + m, ystart + sd / 2 - 2, xstart + sd - m,
            ystart + sd / 2 - 2);
        g.drawLine(xstart + m, ystart + sd / 2 + 2, xstart + sd - m,
            ystart + sd / 2 + 2);
        break;
      case ACTION_CHAIN:
        g.drawLine(xstart + m / 2, ystart + m * 2 + m / 3, xstart + m / 2 * 3,
            ystart + m * 2 - m / 3);
        g.drawLine(xstart + m / 2 * 3, ystart + m * 2 - m / 3,
            xstart + m / 2 * 5, ystart + m * 2 + m / 3);
        g.drawLine(xstart + m / 2 * 5, ystart + m * 2 + m / 3,
            xstart + m / 2 * 7, ystart + m * 2 - m / 3);
        break;
      case ACTION_RING_3: // klesnute o 2
        drawRingIcon(g, xstart, ystart + 2, 3);
        break;
      case ACTION_RING_4:
        drawRingIcon(g, xstart, ystart, 4);
        break;
      case ACTION_RING_5:
        drawRingIcon(g, xstart, ystart, 5);
        break;
      case ACTION_RING_PH:
        drawRingIcon(g, xstart, ystart, 1);
        break;
      case ACTION_RING_6:
        drawRingIcon(g, xstart, ystart, 6);
        break;
      case ACTION_RING_7:
        drawRingIcon(g, xstart, ystart, 7);
        break;
      case ACTION_RING_8:
        drawRingIcon(g, xstart, ystart, 8);
        break;
      }
    } else { // ypos >=3 (left menu squares)
      int dan = AN_C;
      // switch nahradene if, lebo ACTION nie su final, kvoli xy
      if (square == ACTION_AN_C)
        dan = AN_C;
      else if (square == ACTION_AN_N)
        dan = AN_N;
      else if (square == ACTION_AN_O)
        dan = AN_O;
      else if (square == ACTION_AN_S)
        dan = AN_S;
      else if (square == ACTION_AN_F)
        dan = AN_F;
      else if (square == ACTION_AN_CL)
        dan = AN_CL;
      else if (square == ACTION_AN_BR)
        dan = AN_BR;
      else if (square == ACTION_AN_I)
        dan = AN_I;
      else if (square == ACTION_AN_P)
        dan = AN_P;
      else if (square == ACTION_AN_X)
        dan = AN_X;
      else if (square == ACTION_AN_R)
        dan = AN_R;

      squareTextBold(g, xstart, ystart, color[dan], zlabel[dan]);
    }
  }

  // --------------------------------------------------------------------------
  void squareText(Graphics g, int xstart, int ystart, String text) {
    g.setFont(fontSmall);
    int hSmall = fontSmallMet.getAscent(); // vyska fontu
    int w = fontSmallMet.stringWidth(text);
    g.drawString(text, xstart + (sd - w) / 2,
        ystart + (sd - hSmall) / 2 + hSmall);
  }

  // --------------------------------------------------------------------------
  void squareTextBold(Graphics g, int xstart, int ystart, Color col,
                      String text) {
    int h = fontBoldMet.getAscent(); // vyska fontu
    int w = fontBoldMet.stringWidth(text);
    g.setFont(fontBold);
    g.setColor(col);
    if (bwMode)
      g.setColor(Color.black);
    g.drawString(text, xstart + (sd - w) / 2, ystart + (sd - h) / 2 + h);
    // poor man's BOLD
    //g.drawString(text,xstart+(sd-w)/2+1,ystart+(sd-h)/2+h);
  }

  // --------------------------------------------------------------------------
  void drawRingIcon(Graphics g, int xstart, int ystart, int n) {
    int m = sd / 4; // margin
    boolean ph = false;
    int xp[] = new int[9];
    int yp[] = new int[9]; // polygon coordinates
    double xcenter = xstart + sd / 2;
    double ycenter = ystart + sd / 2;
    int rc = sd / 2 - m / 2;
    if (n == 1) {
      n = 6;
      ph = true;
    }
    for (int i = 0; i <= n; i++) {
      double uhol = Math.PI * 2. / n * (i - .5);
      xp[i] = (int) (xcenter + rc * Math.sin(uhol));
      yp[i] = (int) (ycenter + rc * Math.cos(uhol));
    }
    g.drawPolygon(xp, yp, n + 1);
    if (ph) { // double bonds in Ph icon
      for (int i = 0; i <= n; i++) {
        double uhol = Math.PI * 2. / n * (i - .5);
        xp[i] = (int) (xcenter + (rc - 3) * Math.sin(uhol));
        yp[i] = (int) (ycenter + (rc - 3) * Math.cos(uhol));
      }
      g.drawLine(xp[0], yp[0], xp[1], yp[1]);
      g.drawLine(xp[2], yp[2], xp[3], yp[3]);
      g.drawLine(xp[4], yp[4], xp[5], yp[5]);
    }
  }

  // ----------------------------------------------------------------------------
  void info(String text) {
    infoText = text;
    // co s doMenu a repaintom
  }

  // ----------------------------------------------------------------------------
  public boolean mouseDown(MouseEvent e, int x, int y) {
    // 02.06 niektotre return true zmenene na false (aby events aj v mipc)
    boolean status = true; // 2206
    if (depict)
      return true;
    xold = x - sd;
    yold = y - 2 * sd; // tu aby bralo aj s modifiers
    //if (e.modifiers > 0) return true; // ??? bug in MS-Windows ??? treba ???
    info(""); // vynuluje info (mozno sa este pri akcii zaplni);
    //int x = e.getX(); int y = e.getY();
    mouseShift = e.isShiftDown(); // kvoli numbering

    movePossible = false;
    if (x < sd || y < sd * 2) { // --- menu pressed

      int xbutton = 0;
      for (int i = 1; i <= ACTIONX; i++)
        if (x < i * sd) {
          xbutton = i;
          break;
        }
      int ybutton = 0;
      for (int i = 1; i <= ACTIONA + 2; i++)
        if (y < i * sd) {
          ybutton = i;
          break;
        }
      if (xbutton == 0 || ybutton == 0)
        return true;

      // vyradene empty buttons
      int action = ybutton * 100 + xbutton;
      if (!application && action == ACTION_END)
        return true;
      if (!query && action == ACTION_QRY)
        return true;
      if (!stereo && action == ACTION_STEREO)
        return true;
      if (!multipart && action == ACTION_NEW)
        return true;
      if (!(number || autonumber) && action == ACTION_MARK)
        return true;
      if (!reaction && action == ACTION_REACP)
        return true;

      menuAction(action);
    } else if (y > dimension.height - sd - 1) { // --- info area clicked
      return true;
    } else { // ---  mouse click in the drawing area  ---------------------------
      movePossible = true;
      x -= sd;
      y -= 2 * sd;

      if (doAction()) {
        return false;
      }

      if (nmols == 0 || newMolecule == true) {

        // free space clicked - new molecule 
        // creating new molecule only on start or when ACTION_NEW is on
        if (action <= ACTION_STEREO)
          return true;
        doNewMoleculeAction(x, y);
        status = false; //2206
      }

      mol.valenceState();
      repaint(); // ciastocne zbytocne repaintuje, ale asi nechat tak

    }
    return status;
  }

  private boolean doAction() {
    if (mol.touchedAtom > 0) {
      // atom clicked
      doMouseAtomAction();
    } else if (mol.touchedBond > 0) {
      // bond clicked
      doMouseBondAction();
    } else {
      return false;
    }
    mol.valenceState();
    repaint();
    return true;
  }

  private void doNewMoleculeAction(int x, int y) {
    nmols++;
    actualMolecule = nmols;
    mols[nmols] = new JMEmol(this);
    mol = mols[nmols];
    newMolecule = false;
    smol = null; // kvoli undo

    if (action >= ACTION_BOND_SINGLE && action <= ACTION_BOND_TRIPLE
        || action == ACTION_CHAIN) {
      mol.createAtom();
      mol.nbonds = 0;
      mol.nv[1] = 0;
      mol.x[1] = x;
      mol.y[1] = y;
      mol.touchedAtom = 1;
      mol.touched_org = 1; // needed for checkNewBond();
      lastAction = LA_BOND;
      mol.addBond();
      // orienting chain
      if (action == ACTION_CHAIN) {
        mol.x[2] = x + JMEmol.RBOND * .866;
        mol.y[2] = y - JMEmol.RBOND * .5;
        mol.chain[0] = 1;
        mol.chain[1] = 2;
        mol.nchain = 1;
      }
    } else if (action >= ACTION_RING_3 && action <= ACTION_RING_9) {
      mol.xorg = x;
      mol.yorg = y;
      lastAction = LA_RING;
      mol.addRing();
    } else if (action > 300) { // adding 1st atom
      mol.createAtom();
      mol.an[1] = active_an;
      mol.nbonds = 0;
      mol.nv[1] = 0;
      mol.x[1] = x;
      mol.y[1] = y;
      mol.touchedAtom = 1;
      if (active_an == AN_X) {
        String xx = atomicSymbol.getText();
        if (xx.length() < 1)
          xx = "X";
        mol.setAtom(1, xx);
      }
    } else if (action == ACTION_TEMPLATE) {
      readMolecule(template);
    } else if (action >= ACTION_GROUP_TBU && action < ACTION_GROUP_MAX) {
      // adding first atom (to which group will be connected)
      mol.createAtom();
      mol.nbonds = 0;
      mol.nv[1] = 0;
      mol.x[1] = x;
      mol.y[1] = y;
      mol.touchedAtom = 1;
      // adding group
      mol.addGroup(true);
    } else {
      System.err.println("error -report fall through bug !");
    }
  }

  private void doMouseBondAction() {
    if (action == ACTION_DELETE) {
      mol.save();
      mol.deleteBond(mol.touchedBond);
      mol.touchedBond = 0;
    } else if (action == ACTION_DELGROUP) {
      mol.save();
      mol.deleteGroup(mol.touchedBond);
      mol.touchedBond = 0;
    } else if (action == ACTION_STEREO) {
      mol.stereoBond(mol.touchedBond);
    } else if (action == ACTION_BOND_SINGLE || action == ACTION_CHAIN) {
      if (mol.nasv[mol.touchedBond] == JMEmol.SINGLE
          && mol.stereob[mol.touchedBond] == 0) //nie pre stereo
        mol.nasv[mol.touchedBond] = JMEmol.DOUBLE;
      else
        mol.nasv[mol.touchedBond] = JMEmol.SINGLE;
      mol.stereob[mol.touchedBond] = 0; // zrusi stereo
    } else if (action == ACTION_BOND_DOUBLE) {
      mol.nasv[mol.touchedBond] = JMEmol.DOUBLE;
      mol.stereob[mol.touchedBond] = 0; // zrusi stereo
    } else if (action == ACTION_BOND_TRIPLE) {
      mol.nasv[mol.touchedBond] = JMEmol.TRIPLE;
      mol.stereob[mol.touchedBond] = 0; // zrusi stereo
    } else if (action >= ACTION_RING_3 && action <= ACTION_RING_9) {
      // fusing ring to bond
      mol.save();
      lastAction = LA_RING; // in addRing may be set to 0
      mol.addRing();
    } else if (action == ACTION_QRY) {
      if (!queryBox.isBondQuery())
        return;
      String bondQuery = queryBox.getSmarts();
      mol.nasv[mol.touchedBond] = JMEmol.QUERY;
      //mol.stereob[mol.touchedBond] = JMEmol.QUERY;
      mol.btag[mol.touchedBond] = bondQuery;
      /*
      if ("~".equals(bondQuery)) mol.stereob[mol.touchedBond] = JMEmol.QB_ANY;
      if (":".equals(bondQuery)) mol.stereob[mol.touchedBond] = JMEmol.QB_AROMATIC;
      if ("@".equals(bondQuery)) mol.stereob[mol.touchedBond] = JMEmol.QB_RING;
      if ("!@".equals(bondQuery)) mol.stereob[mol.touchedBond] = JMEmol.QB_NONRING;
      */
    } else if (action == ACTION_MARK) {
      info("Only atoms may be marked !");
    }
  }

  private boolean doMouseAtomAction() {
    if (action == ACTION_DELETE) {
      mol.save();
      mol.deleteAtom(mol.touchedAtom);
      mol.touchedAtom = 0;
    } else if (action == ACTION_DELGROUP) {
      return true; // do nothing
    } else if (action == ACTION_CHARGE) {
      mol.changeCharge(mol.touchedAtom, 0);
    } else if (action == ACTION_CHARGE_PLUS) {
      mol.changeCharge(mol.touchedAtom, 1);
    } else if (action == ACTION_CHARGE_MINUS) {
      mol.changeCharge(mol.touchedAtom, -1);
    } else if (action == ACTION_BOND_SINGLE || action == ACTION_BOND_DOUBLE
        || action == ACTION_BOND_TRIPLE || action == ACTION_STEREO
        || action == ACTION_CHAIN) {
      mol.save();
      lastAction = LA_BOND; // in addBond may be set to 0
      mol.addBond();
      mol.touched_org = mol.touchedAtom;
      if (action == ACTION_CHAIN) {
        mol.nchain = 1; // pre CHAIN rubberbanding
        mol.chain[1] = mol.natoms;
        mol.chain[0] = mol.touchedAtom;
        mol.touchedBond = 0; // 2005.02
        // mol.avoidTouch(1);
      }
    } else if (action >= ACTION_RING_3 && action <= ACTION_RING_9) {
      mol.save();
      lastAction = LA_RING; // in addRing may be set to 0
      mol.addRing();
    } else if (action == ACTION_TEMPLATE) {
      mol.save();
      //mol.addTemplate(template);
      lastAction = LA_GROUP;
    } else if (action >= ACTION_GROUP_TBU && action < ACTION_GROUP_MAX) {
      mol.save();
      mol.addGroup(false);
      lastAction = LA_GROUP; // may be set to 0
    } else if (action == ACTION_QRY) { // setting atom as query atom
      if (queryBox.isBondQuery())
        return true;
      mol.setAtom(mol.touchedAtom, queryBox.getSmarts());
    } else if (action == ACTION_MARK) {
      mol.mark();
    } else if (action > 300) { // atoms
      if (active_an != mol.an[mol.touchedAtom] || active_an == AN_X) {
        mol.save();
        mol.an[mol.touchedAtom] = active_an;
        mol.q[mol.touchedAtom] = 0; // resetne naboj
        mol.nh[mol.touchedAtom] = 0;
        // special processing pre AN_X, osetrene, ze moze byt aj ""
        if (active_an == AN_X) {
          String xx = atomicSymbol.getText();
          if (xx.length() < 1)
            xx = "X";
          mol.setAtom(mol.touchedAtom, xx);
        }
      }
    }
    return false;
  }

  // ----------------------------------------------------------------------------
  /**
   * @param e
   * @param x
   * @param y
   * @return ignored
   */
  public boolean mouseUp(MouseEvent e, int x, int y) {
    if (depict)
      return true;
    // LA_GROUP netreba, tam nemoze byt atom overlap
    if (lastAction == LA_BOND) {
      if (action == ACTION_CHAIN)
        mol.checkChain();
      else
        mol.checkBond(); // standard bond check 
      mol.findBondCenters(); // zbytocne vela, ale tu kvoli r.b.
    } else if (lastAction == LA_MOVE) {
      // !!! mal by sem este prist check na touched
      mol.findBondCenters();
    }
    if (lastAction > 0) {
      doMenu = false;
      mol.valenceState();
      // nevola sa vzdy ked treba !!!!!
      mol.cleanPolarBonds(); // nie je to privela action ??? nie az pri canon
      repaint(); // napr po zdvojeni vazby ju nakresli
      lastAction = 0;
      afterClear = false;
    }
    return true;
  }

  // ----------------------------------------------------------------------------
  public boolean mouseDrag(MouseEvent e, int x, int y) {
    //public void mouseDragged(MouseEvent e) {
    if (depict)
      return true;
    // rubberBanding possible only after succesfull addition of the bond
    if (!movePossible)
      return true;

    // ??? MS-Win toto volaju aj len pri MouseClick (handled here)
    //int x = e.getX()-sd; int y = e.getY()-sd*2;
    x -= sd;
    y -= sd * 2;
    int movex = (x - xold);
    int movey = (y - yold);
    if (lastAction == LA_RING || lastAction == LA_GROUP
        || lastAction == LA_FAILED)
      return true;
    else if (lastAction == LA_BOND) {
      mol.rubberBanding(x, y);
    } else if (e.isShiftDown() || e.isMetaDown()) {
      mol.rotate(movex);
      lastAction = LA_MOVE;
    } else if (mol.touchedAtom == 0 && mol.touchedBond == 0) {
      mol.move(movex, movey);
      lastAction = LA_MOVE;
    }
    doMenu = false;
    repaint();
    xold = x;
    yold = y;
    return true;
  }

  // ----------------------------------------------------------------------------
  /**
   * @param e
   * @param x
   * @param y
   * @return ignored
   */
  public boolean mouseMove(MouseEvent e, int x, int y) {
    if (depict)
      return true;
    x -= sd;
    y -= sd * 2;

    boolean repaintFlag = false;
    int newActual = 0;
    // necekuje, ci sa nedotyka 2 molekul naraz, ale to by bolo asi zbytocne
    touchLoop: for (int m = 1; m <= nmols; m++) {
      int a = 0, b = 0;
      a = mols[m].testAtomTouch(x, y);
      if (a == 0)
        b = mols[m].testBondTouch(x, y);
      if (a > 0) {
        mols[m].touchedAtom = a;
        mols[m].touchedBond = 0;
        newActual = m;
        repaintFlag = true;
        break touchLoop;
      } else if (b > 0) {
        mols[m].touchedAtom = 0;
        mols[m].touchedBond = b;
        newActual = m;
        repaintFlag = true;
        break touchLoop;
      } else {
        if (mols[m].touchedAtom > 0 || mols[m].touchedBond > 0) {
          mols[m].touchedAtom = 0;
          mols[m].touchedBond = 0;
          repaintFlag = true;
        }
      }
    }

    if (repaintFlag) {
      // vynuluje pripadny predosly touch
      for (int m = actualMolecule + 1; m <= nmols; m++) {
        mols[m].touchedAtom = 0;
        mols[m].touchedBond = 0;
      }
      doMenu = false;
      repaint();
    }

    if (newActual != 0 && newActual != actualMolecule) {
      actualMolecule = newActual;
      mol = mols[actualMolecule];
    }

    return true;
  }

  // ----------------------------------------------------------------------------
  public boolean keyDown(KeyEvent e, int key) {
    if (depict)
      return true;
    info("");
    // key shortcuts
    // treba to disabled, ked input do elementu;
    //if (elementInput) return false;
    //int key = e.getKeyChar();
    int pressed = 0;
    boolean alt = e.getModifiers() == InputEvent.ALT_MASK;
    boolean ctrl = e.getModifiers() == InputEvent.CTRL_MASK;
    char c = (char) key;
    switch (c) {
    //case 'e': case 'E': JMEmol.TESTDRAW = !JMEmol.TESTDRAW; break;
    case 'C':
      pressed = ACTION_AN_C;
      break;
    case 'N':
      pressed = ACTION_AN_N;
      break;
    case 'O':
      pressed = ACTION_AN_O;
      break;
    case 'S':
      pressed = ACTION_AN_S;
      break;
    case 'P':
      pressed = ACTION_AN_P;
      break;
    case 'F':
      pressed = ACTION_AN_F;
      break;
    case 'L':
      pressed = ACTION_AN_CL;
      break;
    case 'B':
      pressed = ACTION_AN_BR;
      break;
    case 'I':
      pressed = ACTION_AN_I;
      break;
    case 'X':
      info(atomicSymbol.getText());
      pressed = ACTION_AN_X;
      active_an = AN_X;
      break;
    case 'H':
      info("H");
      pressed = ACTION_AN_H;
      break;
    case 'R':
      info("R");
      pressed = ACTION_AN_R;
      break; // if (rButton) ?
    case 'T':
      if (action == ACTION_AN_F) {
        pressed = ACTION_GROUP_CF3;
        info("-CF3");
      } else if (action == ACTION_AN_CL) {
        pressed = ACTION_GROUP_CCL3;
        info("-CCl3");
      } else {
        pressed = ACTION_GROUP_TBU;
        info("-tBu");
      }
      break;
    case 'Y':
      pressed = ACTION_GROUP_NITRO;
      info("-NO2");
      break;
    case 'Z':
      if (ctrl) {
        pressed = ACTION_UNDO;
        info("");
      } else {
        pressed = ACTION_GROUP_SULFO;
        info("-SO3H");
      }
      break;
    case 'A':
      pressed = ACTION_GROUP_COO;
      info("-COOH");
      break;
    case 'E':
      pressed = ACTION_GROUP_CC;
      info("-C#CH");
      break;
    case 'U':
      pressed = ACTION_UNDO;
      break;
    case 'Q':
      pressed = ACTION_GROUP_CYANO;
      info("-C#N");
      break;
    //    case 'g': // used for testing
    //      return true;
    case 27:
      pressed = ACTION_BOND_SINGLE;
      break;
    case '-':
      // check here that an was changed and then cancell this -X ??? 
      if (action == ACTION_AN_F) {
        pressed = ACTION_GROUP_CF;
        info("-F");
      } else if (action == ACTION_AN_CL) {
        pressed = ACTION_GROUP_CL;
        info("-Cl");
      } else if (action == ACTION_AN_BR) {
        pressed = ACTION_GROUP_CB;
        info("-Br");
      } else if (action == ACTION_AN_I) {
        pressed = ACTION_GROUP_CI;
        info("-I");
      } else if (action == ACTION_AN_O) {
        pressed = ACTION_GROUP_CO;
        info("-OH");
      } else if (action == ACTION_AN_N) {
        pressed = ACTION_GROUP_CN;
        info("-NH2");
      } else
        pressed = ACTION_BOND_SINGLE;
      break;
    case '=':
      if (action == ACTION_AN_O) {
        pressed = ACTION_GROUP_dO;
        info("=O");
      } else
        pressed = ACTION_BOND_DOUBLE;
      break;
    case '#':
      pressed = ACTION_BOND_TRIPLE;
      break;
    case '0':
      if (action == ACTION_MARK)
        updateMark(0);
      else {
        if (!alt) {
          pressed = ACTION_RING_FURANE;
          info("-Furyl");
        } else {
          pressed = ACTION_RING_3FURYL;
          info("-3-Furyl");
        }
      }
      break;
    case '1':
      if (action == ACTION_MARK)
        updateMark(1);
      else if (action == ACTION_AN_R) {
        info("-R1");
        pressed = ACTION_AN_R1;
      } else {
        int action_old = action;
        menuAction(ACTION_BOND_SINGLE);
        doAction();
        action = action_old;
        return true;
      }
      break;
    case '2':
      if (action == ACTION_MARK) {
        updateMark(2);
      } else if (action == ACTION_AN_R) {
        info("-R2");
        pressed = ACTION_AN_R2;
      } else {
        int action_old = action;
        menuAction(ACTION_BOND_DOUBLE);
        doAction();
        action = action_old;
        return true;
      }
      break;
    case '3':
      if (action == ACTION_MARK) {
        updateMark(3);
      } else if (action == ACTION_AN_R) {
        info("-R3");
        pressed = ACTION_AN_R3;
      } else {
        int action_old = action;
        menuAction(ACTION_BOND_TRIPLE);
        doAction();
        action = action_old;
        return true;
      }
      break;
    case '4':
      if (action == ACTION_MARK)
        updateMark(4);
      else
        pressed = ACTION_RING_4;
      break;
    case '5':
      if (action == ACTION_MARK)
        updateMark(5);
      else
        pressed = ACTION_RING_5;
      break;
    case '6':
      if (action == ACTION_MARK)
        updateMark(6);
      else
        pressed = ACTION_RING_6;
      break;
    case '7':
      if (action == ACTION_MARK)
        updateMark(7);
      else
        pressed = ACTION_RING_7;
      break;
    case '8':
      if (action == ACTION_MARK)
        updateMark(8);
      else
        pressed = ACTION_RING_8;
      break;
    case '9':
      if (action == ACTION_MARK)
        updateMark(9);
      else {
        info("9 ring");
        pressed = ACTION_RING_9;
      }
      break;
    case 'd':
    case 'D':
    case 8:
    case 127:
      pressed = ACTION_DELETE;
      break;
    case 32:
      pressed = ACTION_CHAIN;
      break; // SPACE
    case 1002:
      pressed = ACTION_PGUP;
      break; // PgUp
    case 1003:
      pressed = ACTION_PGDN;
      break; // PgDn
    //default: System.out.println("key "+key); break;
    }
    menuAction(pressed);
    return true;
  }

  // --------------------------------------------------------------------------
  // called when number key clicked and marking active
  // updates actual mark which will be used for marking
  private void updateMark(int n) {
    // need to know when new number and when combination of 2 presses i.e. 12
    if (autonumber) {
      if (n == 0) {
        currentMark = -1;
        info("click marked atom to delete mark");
        repaint(); // updates status line
      }
      return;
    }

    if (markUsed)
      currentMark = n;
    else {
      if (currentMark > -1 && currentMark < 10)
        currentMark = currentMark * 10 + n;
      else
        currentMark = n; // mark cannot be > 99
    }
    markUsed = false;

    if (currentMark == 0) {
      currentMark = -1;
      info("click marked atom to delete mark");
    } else
      info(currentMark + " ");
    repaint(); // updates status line
  }
  // --------------------------------------------------------------------------

  @Override
  public void mouseDragged(MouseEvent e) {
    mouseDrag(e, e.getX(), e.getY());
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    mouseMove(e, e.getX(), e.getY());
  }

  @Override
  public void keyTyped(KeyEvent e) {
    System.out.println(e);

  }

  @Override
  public void keyPressed(KeyEvent e) {
    keyDown(e, e.getKeyCode());
  }

  @Override
  public void keyReleased(KeyEvent e) {
    System.out.println(e);
    // TODO

  }

  @Override
  public void mouseClicked(MouseEvent e) {
    // TODO

  }

  @Override
  public void mousePressed(MouseEvent e) {
    mouseDown(e, e.getX(), e.getY());
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    mouseUp(e, e.getX(), e.getY());
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    // TODO

  }

  @Override
  public void mouseExited(MouseEvent e) {
    // TODO

  }
}

// ****************************************************************************
class MultiBox extends JFrame implements KeyListener {
  JTextField smilesText;
  JME jme; // parent of MultiBox
  // ----------------------------------------------------------------------------

  MultiBox(int box, JME jme) {
    super();
    this.jme = jme;
    setFont(jme.fontSmall);
    setBackground(JME.bgColor);
    setResizable(false);

    addKeyListener(this);
    if (box == 1)
      createSmilesBox(jme.Smiles());
    else if (box == 2)
      createAtomxBox();
    else
      createAboutBox();

    pack();
    setVisible(true);
  }

  // ----------------------------------------------------------------------------
  void createAboutBox() {
    setTitle("about JSME");
    setLayout(new GridLayout(0, 1, 0, 0));
    setFont(jme.fontSmall);
    setBackground(JME.bgColor);
    add(new JLabel("JSME Molecular Editor" + " v" + JME.version,
        SwingConstants.CENTER));
    add(new JLabel("Peter Ertl and Bruno BienFait", SwingConstants.CENTER));
    //add(new JLabel("peter.ertl@novartis.com",Label.CENTER));
    JPanel p = new JPanel();
    JButton b = new JButton("Close");
    b.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        jme.aboutBoxPoint = jme.aboutBox.getLocationOnScreen();
        jme.aboutBox.setVisible(false);
      }

    });
    p.add(b);
    add(p);
    setLocation(jme.aboutBoxPoint);
  }

  // ----------------------------------------------------------------------------
  void createSmilesBox(String smiles) {
    setTitle("SMILES");
    setLayout(new BorderLayout(2, 0)); // 2, 0 gaps
    smilesText = new JTextField(smiles + "     ");
    if (!jme.runsmi)
      smilesText.setEditable(false);
    add("Center", smilesText);
    JPanel p = new JPanel();
    JButton b = new JButton("Close");
    b.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        jme.smilesBoxPoint = jme.smilesBox.getLocationOnScreen();
        jme.smilesBox.setVisible(false);
      }
      
    });
    p.add(b);
    if (jme.runsmi) {
      b = new JButton("Submit");
      p.add(b);
    }
    add("South", p);
    smilesText.setText(smilesText.getText().trim()); // odstrani "      "
    setResizable(true);
    setLocation(jme.smilesBoxPoint);
  }

  // ----------------------------------------------------------------------------
  // sets smiles in smiles box a aj upravi dlzku
  void setSmiles(String smiles) {
    Dimension d = getSize();
    int l = jme.fontSmallMet.stringWidth(smiles) + 30;
    if (l < 150)
      l = 150;
    this.setSize(l, d.height);
    validate();
    smilesText.setText(smiles);
  }

  // ----------------------------------------------------------------------------
  void createAtomxBox() {
    setTitle("nonstandard atom");
    setLayout(new BorderLayout(2, 0)); // 2, 0 gaps
    JPanel p = new JPanel();
    p.add(new JLabel("atomic SMILES", SwingConstants.CENTER));
    add("North", p);
    // 2007.01 fixed bug - frozen xbutton
    String as = "H";
    if (jme.atomicSymbol != null)
      as = jme.atomicSymbol.getText();
    jme.atomicSymbol = new JTextField(as, 8);
    add("Center", jme.atomicSymbol);
    p = new JPanel();
    JButton b = new JButton("Close ");
    b.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        jme.atomxBoxPoint = jme.atomxBox.getLocationOnScreen();
        jme.atomxBox.setVisible(false);
      }

    });
    p.add(b);
    add("South", p);
    setLocation(jme.atomxBoxPoint);
  }
  /**
   * @param e 
   * @param key  
   * @return false
   */
  // ----------------------------------------------------------------------------
  public boolean keyDown(KeyEvent e, int key) {
    // v JME menu nastavi na X (ak bolo medzitym ine) ak tukane zo atomxBox
    if (jme.atomicSymbol == null)
      return false; // nie null iba v atomxBox
    // vracia false, lebo potom by sa nedalo pisat napr v smilesBox
    if (jme.action != JME.ACTION_AN_X) {
      jme.action = JME.ACTION_AN_X;
      jme.active_an = JME.AN_X; // treba
    }
    //JME.repaint(); //can't make static reference ... kvoli ocierneniu X
    return false; // inak sa nedaju pisat pismena do text boxu
  }
  // ----------------------------------------------------------------------------

  @Override
  public void keyTyped(KeyEvent e) {
    // TODO
    
  }

  @Override
  public void keyPressed(KeyEvent e) {
    keyDown(e, e.getKeyCode());
  }

  @Override
  public void keyReleased(KeyEvent e) {
    // TODO
    
  }
}

// ****************************************************************************
class QueryBox extends JFrame {
  JTextField text;
  Color bgc = JME.bgColor;
  boolean isBondQuery = false;
  JME jme; // reference to parent
  // --- JButtony etc su definovane ako static aby sa zachovala ich hodnota po
  //     novom stlacenie QRY (aby sa window dostalo hore)
  // ----------------------------------------------------------------------------

  QueryBox(JME jme) {
    super("Atom/Bond Query");
    this.jme = jme;
    setLayout(new GridLayout(0, 1));

    setFont(jme.fontSmall);
    setBackground(bgc);

    JPanel p1 = new JPanel();
    p1.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 1));
    p1.add(new JLabel("Atom type :"));

    //boolean first = (any == null); // caused problems
    boolean first = true;

    if (first) {
      jme.any = new JButton("Any");
      jme.anyec = new JButton("Any except C");
      jme.halogen = new JButton("Halogen");
    }
    p1.add(jme.any);
    p1.add(jme.anyec);
    p1.add(jme.halogen);
    add(p1);

    JPanel p2 = new JPanel();
    p2.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 1));
    p2.add(new JLabel("Or select one or more from the list :",
        SwingConstants.LEFT));
    add(p2);

    JPanel p3 = new JPanel();
    //p3.setLayout(new GridLayout(1,0));
    p3.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 1));
    if (first) {
      jme.c = new JButton("C");
      jme.n = new JButton("N");
      jme.o = new JButton("O");
      jme.s = new JButton("S");
      jme.p = new JButton("P");
      jme.f = new JButton("F");
      jme.cl = new JButton("Cl");
      jme.br = new JButton("Br");
      jme.i = new JButton("I");
    }
    p3.add(jme.c);
    p3.add(jme.n);
    p3.add(jme.o);
    p3.add(jme.s);
    p3.add(jme.p);
    p3.add(jme.f);
    p3.add(jme.cl);
    p3.add(jme.br);
    p3.add(jme.i);
    add(p3);

    JPanel p4 = new JPanel();
    p4.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 1));
    if (first) {
      jme.choiceh = new JComboBox<String>();
      jme.choiceh.addItem("Any");
      jme.choiceh.addItem("0");
      jme.choiceh.addItem("1");
      jme.choiceh.addItem("2");
      jme.choiceh.addItem("3");
    }
    p4.add(new JLabel("Number of hydrogens :  "));
    p4.add(jme.choiceh);
    add(p4);

    JPanel p5 = new JPanel();
    p5.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 1));
    if (first) {
      jme.choiced = new JComboBox<String>();
      jme.choiced.addItem("Any");
      jme.choiced.addItem("0");
      jme.choiced.addItem("1");
      jme.choiced.addItem("2");
      jme.choiced.addItem("3");
      jme.choiced.addItem("4");
      jme.choiced.addItem("5");
      jme.choiced.addItem("6");
    }

    p5.add(new JLabel("Number of connections :", SwingConstants.LEFT));
    p5.add(jme.choiced);
    p5.add(new JLabel(" (H's don't count.)", SwingConstants.LEFT));
    add(p5);

    JPanel p6 = new JPanel();
    p6.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 1));
    p6.add(new JLabel("Atom is :"));
    if (first)
      jme.aromatic = new JButton("Aromatic");
    p6.add(jme.aromatic);
    if (first)
      jme.nonaromatic = new JButton("Nonaromatic");
    p6.add(jme.nonaromatic);
    if (first)
      jme.ring = new JButton("Ring");
    p6.add(jme.ring);
    if (first)
      jme.nonring = new JButton("Nonring");
    p6.add(jme.nonring);
    add(p6);

    JPanel p9 = new JPanel();
    p9.setBackground(getBackground().darker());
    p9.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 1));
    p9.add(new JLabel("Bond is :"));
    if (first)
      jme.anyBond = new JButton("Any");
    p9.add(jme.anyBond);
    if (first)
      jme.aromaticBond = new JButton("Aromatic");
    p9.add(jme.aromaticBond);
    // if (first) sdBond = new JButton("- or ="); p9.add(sdBond);
    if (first)
      jme.ringBond = new JButton("Ring");
    p9.add(jme.ringBond);
    if (first)
      jme.nonringBond = new JButton("Nonring");
    p9.add(jme.nonringBond);
    add(p9);

    JPanel p8 = new JPanel();
    p8.setLayout(new FlowLayout(FlowLayout.CENTER, 3, 1));
    //p8.add(new JLabel("Query :"));
    if (first)
      text = new JTextField("*", 20);
    p8.add(text);
    p8.add(new JButton("Reset"));
    p8.add(new JButton("Close"));
    add(p8);
    setResizable(false);

    if (first) { // musi sa to explicitne, inak nemaju vsetky bgc 
      resetAtomList();
      resetAtomType();
      resetBondType();
      jme.aromatic.setBackground(bgc);
      jme.nonaromatic.setBackground(bgc);
      jme.ring.setBackground(bgc);
      jme.nonring.setBackground(bgc);
      jme.choiceh.setBackground(bgc);
      jme.choiced.setBackground(bgc);
      changeColor(jme.any);
    }
    pack();
    setLocation(jme.point);
    setVisible(true);
  }

  // ----------------------------------------------------------------------------
  @Override
  public boolean action(Event e, Object arg) {
    if (arg.equals("Close")) {
      jme.point = getLocationOnScreen();
      setVisible(false);
    } else if (arg.equals("Reset")) {
      resetAll();
      changeColor(jme.any); // Any on
      doSmarts();
    } else if (e.target instanceof JButton) {
      resetBondType(); // set to any ???
      if (e.target == jme.any) {
        resetAtomList();
        resetAtomType();
      } else if (e.target == jme.anyec) {
        resetAtomList();
        resetAtomType();
      } else if (e.target == jme.halogen) {
        resetAtomList();
        resetAtomType();
      } else if (e.target == jme.ring) {
        jme.nonring.setBackground(bgc);
      } else if (e.target == jme.nonring) {
        jme.ring.setBackground(bgc);
        jme.aromatic.setBackground(bgc);
      } else if (e.target == jme.aromatic) {
        jme.nonaromatic.setBackground(bgc);
        jme.nonring.setBackground(bgc);
      } else if (e.target == jme.nonaromatic) {
        jme.aromatic.setBackground(bgc);
      } else if (e.target == jme.anyBond || e.target == jme.aromaticBond
          || e.target == jme.ringBond || e.target == jme.nonringBond) {
        resetAll();
        isBondQuery = true;
      } else { // atom z listu pressed (moze by aj posledny vynulovany
        resetAtomType();
      }
      changeColor((JButton) (e.target));
      doSmarts();
    } else if (e.target instanceof JComboBox) {
      resetBondType();
      JComboBox<?> choice = (JComboBox<?>) (e.target);
      if (choice.getSelectedIndex() == 0)
        choice.setBackground(bgc);
      else
        choice.setBackground(Color.orange);
      doSmarts();
    }

    // v JME menu nastavi na query (ak bolo medzitym ine)
    if (jme.action != JME.ACTION_QRY) {
      jme.action = JME.ACTION_QRY;
      jme.repaint();
    }

    return true;
  }

  // ----------------------------------------------------------------------------
  private void resetAll() {
    resetAtomList();
    resetAtomType();
    jme.choiceh.setSelectedIndex(0);
    jme.choiced.setSelectedIndex(0);
    jme.aromatic.setBackground(bgc);
    jme.nonaromatic.setBackground(bgc);
    jme.ring.setBackground(bgc);
    jme.nonring.setBackground(bgc);
    jme.choiceh.setBackground(bgc);
    jme.choiced.setBackground(bgc);
    resetBondType(); // also sets isBondQuery to false
  }

  // ----------------------------------------------------------------------------
  private void resetAtomList() {
    jme.c.setBackground(bgc);
    jme.n.setBackground(bgc);
    jme.o.setBackground(bgc);
    jme.s.setBackground(bgc);
    jme.p.setBackground(bgc);
    jme.f.setBackground(bgc);
    jme.cl.setBackground(bgc);
    jme.br.setBackground(bgc);
    jme.i.setBackground(bgc);
  }

  // ----------------------------------------------------------------------------
  private void resetAtomType() {
    jme.any.setBackground(bgc);
    jme.anyec.setBackground(bgc);
    jme.halogen.setBackground(bgc);
  }

  // ----------------------------------------------------------------------------
  private void resetBondType() {
    jme.anyBond.setBackground(bgc);
    jme.aromaticBond.setBackground(bgc);
    //sdBond.setBackground(bgc);
    jme.ringBond.setBackground(bgc);
    jme.nonringBond.setBackground(bgc);
    isBondQuery = false;
  }

  // ----------------------------------------------------------------------------
  private void changeColor(JButton b) {
    if (b.getBackground() == bgc)
      b.setBackground(Color.orange);
    else
      b.setBackground(bgc);
  }

  // ----------------------------------------------------------------------------
  private void doSmarts() {
    String smarts = "";
    boolean showaA = false;

    // basic atom type
    if (jme.any.getBackground() != bgc) {
      smarts = "*";
      showaA = true;
    } else if (jme.anyec.getBackground() != bgc) {
      smarts = "!#6";
      showaA = true;
    } else if (jme.halogen.getBackground() != bgc) {
      jme.f.setBackground(Color.orange);
      jme.cl.setBackground(Color.orange);
      jme.br.setBackground(Color.orange);
      jme.i.setBackground(Color.orange);
      smarts = "F,Cl,Br,I";
    } else {
      boolean ar = jme.aromatic.getBackground() != bgc;
      boolean nar = jme.nonaromatic.getBackground() != bgc;
      if (jme.c.getBackground() != bgc) {
        if (ar)
          smarts += "c,";
        else if (nar)
          smarts += "C,";
        else
          smarts += "#6,";
      }
      if (jme.n.getBackground() != bgc) {
        if (ar)
          smarts += "n,";
        else if (nar)
          smarts += "N,";
        else
          smarts += "#7,";
      }
      if (jme.o.getBackground() != bgc) {
        if (ar)
          smarts += "o,";
        else if (nar)
          smarts += "O,";
        else
          smarts += "#8,";
      }
      if (jme.s.getBackground() != bgc) {
        if (ar)
          smarts += "s,";
        else if (nar)
          smarts += "S,";
        else
          smarts += "#16,";
      }
      if (jme.p.getBackground() != bgc) {
        if (ar)
          smarts += "p,";
        else if (nar)
          smarts += "P,";
        else
          smarts += "#15,";
      }
      if (jme.f.getBackground() != bgc)
        smarts += "F,";
      if (jme.cl.getBackground() != bgc)
        smarts += "Cl,";
      if (jme.br.getBackground() != bgc)
        smarts += "Br,";
      if (jme.i.getBackground() != bgc)
        smarts += "I,";
      //if (h.getBackground() != bgc) smarts += "H,";
      if (smarts.endsWith(","))
        smarts = smarts.substring(0, smarts.length() - 1);
      if (smarts.length() < 1 && !isBondQuery) { // napr pri vynulovani listu
        if (ar)
          smarts = "a";
        else if (nar)
          smarts = "A";
        else {
          jme.any.setBackground(Color.orange);
          smarts = "*";
        }
      }
    }

    // atomic properties
    String ap = "";
    if (showaA && jme.aromatic.getBackground() != bgc)
      ap += ";a";
    if (showaA && jme.nonaromatic.getBackground() != bgc)
      ap += ";A";
    if (jme.ring.getBackground() != bgc)
      ap += ";R";
    if (jme.nonring.getBackground() != bgc)
      ap += ";!R";
    // zjednodusenie (mieso *;r len r ...)
    if (jme.any.getBackground() != bgc && ap.length() > 0)
      smarts = ap.substring(1, ap.length());
    else
      smarts += ap;

    // hydrogens and number of bonds
    int nh = jme.choiceh.getSelectedIndex();
    if (nh > 0) {
      nh--;
      smarts += ";H" + nh;
    }
    int nd = jme.choiced.getSelectedIndex();
    if (nd > 0) {
      nd--;
      smarts += ";D" + nd;
    }

    // bond type
    if (jme.anyBond.getBackground() != bgc)
      smarts = "~";
    if (jme.aromaticBond.getBackground() != bgc)
      smarts = ":";
    //if (sdBond.getBackground() != bgc) smarts = "-,=";
    if (jme.ringBond.getBackground() != bgc)
      smarts = "@";
    if (jme.nonringBond.getBackground() != bgc)
      smarts = "!@";

    text.setText(smarts);
  }

  // --------------------------------------------------------------------------
  boolean isBondQuery() {
    return isBondQuery;
  }

  // --------------------------------------------------------------------------
  String getSmarts() {
    return text.getText();
  }
  // --------------------------------------------------------------------------
}
// ****************************************************************************
