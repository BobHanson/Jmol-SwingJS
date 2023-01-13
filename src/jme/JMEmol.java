package jme;

// -------------------------------------------------------------------------- 
// 
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
// StringTokenizer
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolAdapterAtomIterator;
import org.jmol.api.JmolAdapterBondIterator;
import org.jmol.util.Edge;
import org.jmol.viewer.PropertyManager;

import javajs.util.P3d;

// --------------------------------------------------------------------------
public class JMEmol {

  JME jme; // parent
  // --- vlastne data pre molekulu
  public int natoms = 0;
  public int nbonds = 0;
  // atom and bond storage starting with value of 20
  int an[] = new int[20];
  int q[] = new int[20];
  double x[] = new double[20];
  double y[] = new double[20];
  int v[][] = new int[20][MAX_BONDS_ON_ATOM + 1];
  int abg[] = new int[20]; // atom background
  String atag[] = new String[20]; // atom tag
  String[] label = new String[20];
  int nh[] = new int[20];
  int nv[] = new int[20];
  int va[] = new int[20];
  int vb[] = new int[20];
  int nasv[] = new int[20];
  int stereob[] = new int[20]; // pre QUERY bond je tu aj type
  int xb[] = new int[20];
  int yb[] = new int[20];
  String btag[] = new String[20]; // bond tag
  int mark[][] = new int[10][2];
  int chain[] = new int[101];
  int nmarked = 0; // obsolote in reaction automarking since 2009.04, still used in other marks
  int maxMark = 0; // updated only in autonumber
  int doColoring = 0; // 1 atom coloring, -1 background coloring

  int a[]; // help variable for deleteGroup, createSmiles, checkMultipart ...
  int btype[]; // SINGLE,DOUBLE,TRIPLE,AROMATIC - len pre smi ?
  int touchedAtom = 0; // nesmu byt static kvoli reaction (multi?)
  int touchedBond = 0;
  int touched_org; // original v rubber banding
  double xorg, yorg; // center of ring in free space, rubber banding
  private boolean linearAdding = false; // pre ACTION_TBU (lepsie ???)
  int nchain; // pomocna variable pre CHAIN (aktualna dlzka pri rubber)
  boolean stopChain = false;
  boolean needRecentering = false;

  // static constants
  static final int SINGLE = 1, DOUBLE = 2, TRIPLE = 3, AROMATIC = 5, QUERY = 9;
  static final int QB_ANY = 11, QB_AROMATIC = 12, QB_RING = 13, QB_NONRING = 14;
  private static final int UP = 1, DOWN = 2, XUP = 3, XDOWN = 4, EZ = 10;
  public static final int RBOND = 25;
  private static final int TOUCH_LIMIT = 50;
  private static final int MAX_BONDS_ON_ATOM = 6;
  static boolean TESTDRAW = false;

  // ----------------------------------------------------------------------------
  JMEmol(JME jme) {
    this.jme = jme;
  }

  private void init() {
    natoms = 0;
    nbonds = 0;
    nmarked = 0;
  }

  // ----------------------------------------------------------------------------
  JMEmol(JMEmol m) {
    // vytvori kopiu molekuly m
    // nekopiruje v[][], nv[], nh[], xb[], yb[] - treba dimenzovat v complete()
    jme = m.jme;
    natoms = m.natoms;
    nbonds = m.nbonds;
    nmarked = m.nmarked;
    an = new int[natoms + 1];
    System.arraycopy(m.an, 0, an, 0, natoms + 1);
    q = new int[natoms + 1];
    System.arraycopy(m.q, 0, q, 0, natoms + 1);
    nh = new int[natoms + 1];
    System.arraycopy(m.nh, 0, nh, 0, natoms + 1);
    abg = new int[natoms + 1];
    System.arraycopy(m.abg, 0, abg, 0, natoms + 1);
    atag = new String[natoms + 1];
    System.arraycopy(m.atag, 0, atag, 0, natoms + 1);
    x = new double[natoms + 1];
    System.arraycopy(m.x, 0, x, 0, natoms + 1);
    y = new double[natoms + 1];
    System.arraycopy(m.y, 0, y, 0, natoms + 1);
    label = new String[natoms + 1];
    System.arraycopy(m.label, 0, label, 0, natoms + 1);
    //nv = new int[natoms+1]; System.arraycopy(m.nv,0,nv,0,natoms+1);
    va = new int[nbonds + 1];
    System.arraycopy(m.va, 0, va, 0, nbonds + 1);
    vb = new int[nbonds + 1];
    System.arraycopy(m.vb, 0, vb, 0, nbonds + 1);
    nasv = new int[nbonds + 1];
    System.arraycopy(m.nasv, 0, nasv, 0, nbonds + 1);
    btag = new String[nbonds + 1];
    System.arraycopy(m.btag, 0, btag, 0, nbonds + 1);
    stereob = new int[nbonds + 1];
    System.arraycopy(m.stereob, 0, stereob, 0, nbonds + 1);
    mark = new int[nmarked + 1][2];
    //System.arraycopy(m.mark,0,mark,0,mark.length); // ??? preco
    for (int i = 1; i <= nmarked; i++) {
      mark[i][0] = m.mark[i][0];
      mark[i][1] = m.mark[i][1];
    }
    // coloring info
    doColoring = m.doColoring;
  }

  // ----------------------------------------------------------------------------
  JMEmol(JME jme, JMEmol mols[], int nmols) {
    // merge molecules do 1 molekuly
    // iba docasne, pouziva sa pri zapise mol file;
    this(jme);
    for (int i = 1; i <= nmols; i++) {
      natoms += mols[i].natoms;
      nbonds += mols[i].nbonds;
      nmarked += mols[i].nmarked;
    }
    an = new int[natoms + 1];
    q = new int[natoms + 1];
    nh = new int[natoms + 1];
    abg = new int[natoms + 1];
    atag = new String[natoms + 1];
    x = new double[natoms + 1];
    y = new double[natoms + 1];
    label = new String[natoms + 1];
    va = new int[nbonds + 1];
    vb = new int[nbonds + 1];
    nasv = new int[nbonds + 1];
    btag = new String[nbonds + 1];
    stereob = new int[nbonds + 1];
    mark = new int[nmarked + 1][2];
    int na = 0, nb = 0, nm = 0, nadd = 0;
    for (int i = 1; i <= nmols; i++) {
      for (int j = 1; j <= mols[i].natoms; j++) {
        na++;
        an[na] = mols[i].an[j];
        x[na] = mols[i].x[j];
        y[na] = mols[i].y[j];
        q[na] = mols[i].q[j];
        nh[na] = mols[i].nh[j];
        abg[na] = mols[i].abg[j];
        atag[na] = mols[i].atag[j];
        label[na] = mols[i].label[j];
      }
      for (int j = 1; j <= mols[i].nbonds; j++) {
        nb++;
        nasv[nb] = mols[i].nasv[j];
        stereob[nb] = mols[i].stereob[j];
        va[nb] = mols[i].va[j] + nadd;
        vb[nb] = mols[i].vb[j] + nadd;
        btag[nb] = mols[i].btag[j];
      }
      for (int j = 1; j <= mols[i].nmarked; j++) { // ??? ok
        nm++;
        mark[nm][0] = mols[i].mark[j][0] + nadd;
        mark[nm][1] = mols[i].mark[j][1];
      }
      nadd = na;
    }
    complete();
    center();
  }

  // ----------------------------------------------------------------------------
  JMEmol(JME jme, JMEmol m, int part) {
    this(jme);
    int newn[] = new int[m.natoms + 1]; // cislovanie stare -> nove
    for (int i = 1; i <= m.natoms; i++) {
      if (m.a[i] != part)
        continue;
      createAtom();
      an[natoms] = m.an[i];
      x[natoms] = m.x[i];
      y[natoms] = m.y[i];
      q[natoms] = m.q[i];
      nh[natoms] = m.nh[i];
      abg[natoms] = m.abg[i]; // !!!
      atag[natoms] = m.atag[i];
      label[natoms] = m.label[i];
      newn[i] = natoms;
    }
    for (int i = 1; i <= m.nbonds; i++) {
      int atom1 = m.va[i];
      int atom2 = m.vb[i];
      if (m.a[atom1] != part && m.a[atom2] != part)
        continue;
      if (m.a[atom1] != part || m.a[atom2] != part) { // musia byt obidve part
        System.err.println("MOL multipart inconsistency - report bug !");
        continue;
      }
      createBond();
      nasv[nbonds] = m.nasv[i];
      stereob[nbonds] = m.stereob[i];
      va[nbonds] = newn[atom1];
      vb[nbonds] = newn[atom2];
      btag[nbonds] = m.btag[i];
    }
    for (int i = 1; i <= m.nmarked; i++) {
      int atom = m.mark[i][0];
      if (atom != part)
        continue;
      nmarked++;
      mark[nmarked][0] = newn[atom];
      mark[nmarked][1] = m.mark[i][1];
    }
    doColoring = m.doColoring;

    complete();
    center();
  }

  // ----------------------------------------------------------------------------
  JMEmol(JME jme, String molecule, boolean hasCoordinates) {
    // processes JME string
    // natoms nbonds (atomic_symbol x y) (va vb nasv)
    // atomic symbols for smiles-non-standard atoms may be in smiles form
    // i.e O-, Fe2+, NH3+ 
    // ak su tam vodiky, berie to cislo (aj H0) inak pre stand. atomy doplni
    // ak su standardne atomy v [] berie ich ako X
    // 2006.09 stereo double bond is -5

    this(jme);

    // vyhodi "" na zaciatku a konci
    if (molecule.startsWith("\""))
      molecule = molecule.substring(1, molecule.length());
    if (molecule.endsWith("\""))
      molecule = molecule.substring(0, molecule.length() - 1);

    //System.err.println("TEST molecule in>"+molecule+"<");

    if (molecule.length() < 1) {
      natoms = 0;
      return;
    }
    try {
      StringTokenizer st = new StringTokenizer(molecule);
      int natomsx = Integer.valueOf(st.nextToken()).intValue();
      int nbondsx = Integer.valueOf(st.nextToken()).intValue();
      // natoms and nbonds filled in createAtom() & createBond()
      //System.err.println("TEST a b >"+natomsx + " " +nbondsx+"<");

      // --- reading basic data for atoms
      for (int i = 1; i <= natomsx; i++) {

        // processing atomic symbol => Xx | Hn | charge | :n
        // symbol je vsetko od zaciatku do H + -
        // ak Xx spozna - spracuje vsetko, ak je to X, berie cely a testuje len :n
        String symbol = st.nextToken();
        createAtom(symbol);

        if (hasCoordinates) {
          x[i] = Double.valueOf(st.nextToken()).doubleValue();
          y[i] = -Double.valueOf(st.nextToken()).doubleValue();
        }
        /*
        // this interferes with relstereo do tags
        if (jme.doTags) {
        String s = st.nextToken();
        if (!s.equals("*")) atag[i] = s; // * means there is no tag
        }
        */
        //System.out.println("TESTa> " + i+" "+an[i]+" "+x[i]+" "+y[i]);
      }
      // --- bonds
      for (int i = 1; i <= nbondsx; i++) {
        createBond();
        va[i] = Integer.valueOf(st.nextToken()).intValue();
        vb[i] = Integer.valueOf(st.nextToken()).intValue();
        nasv[i] = Integer.valueOf(st.nextToken()).intValue();
        // musi premenit nasv -1 up a -2 down (z va na vb) na stereob
        if (nasv[i] == -1) {
          nasv[i] = 1;
          stereob[i] = UP;
        } else if (nasv[i] == -2) {
          nasv[i] = 1;
          stereob[i] = DOWN;
        } else if (nasv[i] == -5) {
          nasv[i] = 2;
          stereob[i] = EZ;
        } // ez stereo
        // query bonds created in query window
        else if (nasv[i] == QB_ANY || nasv[i] == QB_AROMATIC
            || nasv[i] == QB_RING || nasv[i] == QB_NONRING) {
          stereob[i] = nasv[i];
          nasv[i] = QUERY;
        }
        //System.out.println("TESTb> " + i+" "+va[i]+" "+vb[i]+" "+nasv[i]);
        /*
        if (jme.doTags) {
        String s = st.nextToken();
        if (!s.equals("*")) btag[i] = s; // * means there is no tag
        }
        */
      }

      fillFields();

      if (hasCoordinates) {
        scaling();
        center(); // calls findBondCenters() --- vyzaduje dimensionsn
      }
    } // end of try
    catch (Exception e) {
      System.err.println("read mol exception - " + e.getMessage());
      //e.printStackTrace();
      natoms = 0;
      return;
    }
    //valenceState(); // dolezite aj pre call v negrafickom mode
    deleteHydrogens();
    complete(); // este raz, zachytit zmeny
  }

  public JMEmol(JME jme, JmolAdapterAtomIterator atomIterator,
      JmolAdapterBondIterator bondIterator) {
    this(jme);
    init();
    Map<Object, Integer> atomMap = new Hashtable<Object, Integer>();
    while (atomIterator.hasNext()) {
      createAtom();
      atomMap.put(atomIterator.getUniqueID(), Integer.valueOf(natoms));
      P3d pt = atomIterator.getXYZ();
      //System.out.println("atomline"+line);
      x[natoms] = pt.x;
      y[natoms] = -pt.y;
      q[natoms] = atomIterator.getFormalCharge();
      setAtom(natoms, JmolAdapter.getElementSymbol(atomIterator.getElement()));
    }
    while (bondIterator.hasNext()) {
      createBond();
      int i = nbonds;
      va[i] = atomMap.get(bondIterator.getAtomUniqueID1()).intValue();
      vb[i] = atomMap.get(bondIterator.getAtomUniqueID2()).intValue();
      int bo = bondIterator.getEncodedOrder();
      switch (bo) {
      case Edge.BOND_STEREO_NEAR:
        nasv[i] = SINGLE;
        stereob[i] = UP;
        break;
      case Edge.BOND_STEREO_FAR:
        nasv[i] = SINGLE;
        stereob[i] = DOWN;
        break;
      case Edge.BOND_COVALENT_SINGLE:
      case Edge.BOND_AROMATIC_SINGLE:
        nasv[i] = SINGLE;
        break;
      case Edge.BOND_COVALENT_DOUBLE:
      case Edge.BOND_AROMATIC_DOUBLE:
        nasv[i] = DOUBLE;
        break;
      case Edge.BOND_COVALENT_TRIPLE:
        nasv[i] = TRIPLE;
        break;
      case Edge.BOND_AROMATIC:
      case Edge.BOND_STEREO_EITHER:
      default:
        if ((bo & 0x07) != 0)
          nasv[i] = (bo & 0x07);
        break;
      }
    }
    fillFields();
    scaling();
    center(); // calls findBondCenters();
    complete(); // ok

    //valenceState(); // dolezite aj pre call v negrafickom mode
    deleteHydrogens();
    complete(); // este raz, zachytit zmeny
  }


  // ----------------------------------------------------------------------------
  JMEmol(JME jme, String molFile) {
    // MDL mol file
    this(jme);
    if (molFile == null)
      return;
    String line = "";
    //System.err.println("molfile in >>>\n"+molFile);
    // osetrene zacatie s null line
    //char c = molFile.charAt(0);
    String separator = findSeparator(molFile);
    StringTokenizer st = new StringTokenizer(molFile, separator, true);
    //System.out.println(st.countTokens());

    for (int i = 1; i <= 4; i++) {
      line = nextData(st, separator);
      //System.out.println(i+" "+line);
    }
    //System.out.println("ab"+line);
    int natomsx = Integer.valueOf(line.substring(0, 3).trim()).intValue();
    int nbondsx = Integer.valueOf(line.substring(3, 6).trim()).intValue();
    //System.out.println("atoms bonds "+natomsx + " "+nbondsx);
    for (int i = 1; i <= natomsx; i++) {
      createAtom();
      line = nextData(st, separator);
      //System.out.println("atomline"+line);
      x[i] = Double.valueOf(line.substring(0, 10).trim()).doubleValue();
      y[i] = -Double.valueOf(line.substring(10, 20).trim()).doubleValue();
      // symbol 32-34 dolava centrovany (v String 31-33)
      int endsymbol = 34;
      if (line.length() < 34)
        endsymbol = line.length();
      String symbol = line.substring(31, endsymbol).trim();
      //String q = line.substring(36,39);
      setAtom(i, symbol); // sets an[i]

      // atom mapping - 61 - 63 
      if (line.length() >= 62) {
        String s = line.substring(60, 63).trim();
        if (s.length() > 0) {
          // 2007.03 fix not to put there 0
          int mark = Integer.valueOf(s).intValue();
          if (mark > 0) {
            touchedAtom = i;
            jme.currentMark = mark;
            mark();
            touchedAtom = 0; // not to frame atom
          }
        }
      }
      //System.out.println("atom "+i+" "+an[i]+" "+x[i]+" "+y[i]);
    }
    for (int i = 1; i <= nbondsx; i++) {
      createBond();
      line = nextData(st, separator);
      //System.out.println("bond"+line);
      va[i] = Integer.valueOf(line.substring(0, 3).trim()).intValue();
      vb[i] = Integer.valueOf(line.substring(3, 6).trim()).intValue();
      int nasvx = Integer.valueOf(line.substring(6, 9).trim()).intValue();
      if (nasvx == 1)
        nasv[i] = SINGLE;
      else if (nasvx == 2)
        nasv[i] = DOUBLE;
      else if (nasvx == 3)
        nasv[i] = TRIPLE;
      // aromatic ???
      else
        nasv[i] = QUERY;
      int stereo = 0;
      if (line.length() > 11)
        stereo = Integer.valueOf(line.substring(9, 12).trim()).intValue();
      // ??? treba s nasvx
      if (nasvx == SINGLE && stereo == 1) {
        nasv[i] = SINGLE;
        stereob[i] = UP;
      }
      if (nasvx == SINGLE && stereo == 6) {
        nasv[i] = SINGLE;
        stereob[i] = DOWN;
      }
      //System.out.println("bons "+i+" "+va[i]+" "+vb[i]+" "+nasv[i]);
    }
    fillFields();
    scaling();
    center(); // calls findBondCenters();
    complete(); // ok

    // reading charges and other information
    while (st.hasMoreTokens()) {
      if ((line = st.nextToken()) == null)
        break;
      if (line.startsWith("M  END"))
        break;
      if (line.startsWith("M  CHG")) {
        StringTokenizer stq = new StringTokenizer(line);
        stq.nextToken();
        stq.nextToken();
        int ndata = Integer.valueOf(stq.nextToken()).intValue();
        for (int i = 1; i <= ndata; i++) {
          int a = Integer.valueOf(stq.nextToken()).intValue();
          q[a] = Integer.valueOf(stq.nextToken()).intValue();
        }
      }
      if (line.startsWith("M  APO")) { // 2004.05
        StringTokenizer stq = new StringTokenizer(line);
        stq.nextToken();
        stq.nextToken();
        int ndata = Integer.valueOf(stq.nextToken()).intValue();
        for (int i = 1; i <= ndata; i++) {
          int a = Integer.valueOf(stq.nextToken()).intValue();
          int nr = Integer.valueOf(stq.nextToken()).intValue();
          // addinf Rnr to atom a
          touchedAtom = a;
          addBond();
          setAtom(natoms, "R" + nr);
          touchedAtom = 0;
        }
      }
    }

    //valenceState(); // dolezite aj pre call v negrafickom mode
    deleteHydrogens();
    complete(); // este raz, zachytit zmeny
  }

  // ----------------------------------------------------------------------------
  public static String nextData(StringTokenizer st, String separator) {
    // dost tricky, musi uvazit aj bez \n aj sa \n |\n za sebou ...
    // musi osetrit aj lines with zero length (2 x po sebe \n alebo |)
    while (st.hasMoreTokens()) {
      String s = st.nextToken();
      if (s.equals(separator))
        return " ";
      if (!st.nextToken().equals(separator)) { // ukoncujuci separator
        System.err.println("mol file line separator problem!");
      }
      // musi vyhodit z konca pripadne | (napr v appletviewer)
      while (true) {
        char c = s.charAt(s.length() - 1);
        if (c == '|' || c == '\n' || c == '\r') {
          s = s.substring(0, s.length() - 1);
          if (s.length() == 0)
            return " "; // v textboox \r\n ??
        } else
          break;
      }
      return s;
    }
    return null;
  }

  // ----------------------------------------------------------------------------
  public static String findSeparator(String molFile) {
    //if (c=='\t' || c=='\n' || c=='\r' || c=='\f' || c=='|') 
    //  molFile = " " + molFile;
    //StringTokenizer st = new StringTokenizer(molFile,"\t\n\r\f|",true);
    // osetrene aj separator 2x tesne za sebou  
    StringTokenizer st = new StringTokenizer(molFile, "\n", true);
    if (st.countTokens() > 4)
      return "\n";
    st = new StringTokenizer(molFile, "|", true);
    if (st.countTokens() > 4)
      return "|";
    System.err.println("Cannot process mol file, use | as line separator !");
    return null;
  }

  // ----------------------------------------------------------------------------
  //public functions for JMEcml
  public int getAtomCount() {
    return natoms;
  }

  public int getBondCount() {
    return nbonds;
  }

  public double getX(int i) {
    return x[i] * 1.4 / RBOND;
  }

  public double getY(int i) {
    return y[i] * 1.4 / RBOND;
  }

  public void setAtomProperties(double xx, double yy, int ahc, int aq) {
    // vztahuje sa na atom natoms
    x[natoms] = xx;
    y[natoms] = yy;
    setAtomHydrogenCount(natoms, ahc);
    setAtomFormalCharge(natoms, aq);
  }

  public int getHydrogenCount(int i) {
    return nh[i];
  }

  public int getCharge(int i) {
    return q[i];
  }

  public int[] getBondProperties(int i) {
    int[] bd = new int[4];
    bd[0] = va[i];
    bd[1] = vb[i];
    bd[2] = nasv[i];
    bd[3] = stereob[i];
    return bd;
  }

  public void setBondProperties(int bp0, int bp1, int bp2, int bp3) {
    // for actual bond, called after createBond() - so data for nbonds
    va[nbonds] = bp0;
    vb[nbonds] = bp1;
    nasv[nbonds] = bp2;
    stereob[nbonds] = bp3;
  }

  // ----------------------------------------------------------------------------
  public void completeMolecule() {
    // hodit to aj inde
    fillFields();
    //deleteHydrogens(); // ??? treba
    scaling();
    center(); // centering
    complete();
  }

  // ----------------------------------------------------------------------------
  void complete() {
    // dimenzuje tie variables, co sa nedimenzovali v constructor JMEMol(m)
    fillFields();
    int storage = nasv.length;
    xb = new int[storage];
    yb = new int[storage];
    findBondCenters();
    valenceState(); // nh a upravi q
  }

  // ----------------------------------------------------------------------------
  void scaling() {
    // proper scaling (to RBOND)
    double dx, dy, sumlen = 0., scale = 0.;
    for (int i = 1; i <= nbonds; i++) {
      dx = x[va[i]] - x[vb[i]];
      dy = y[va[i]] - y[vb[i]];
      sumlen += Math.sqrt(dx * dx + dy * dy);
    }
    if (nbonds > 0) {
      sumlen = sumlen / nbonds;
      scale = RBOND / sumlen;
    } else if (natoms > 1) { // disconnected structure(s)
      scale = 3. * RBOND / Math
          .sqrt((x[1] - x[2]) * (x[1] - x[2]) + (y[1] - y[2]) * (y[1] - y[2]));
    }
    for (int i = 1; i <= natoms; i++) {
      x[i] *= scale;
      y[i] *= scale;
    }
    //if (jme.dimension == null) jme.dimension = size();
    // cim vacsia scale, tym viac sa molekula zmensuje
  }

  // ----------------------------------------------------------------------------
  public void center() {
    // centers molecule within the window xpix x ypix
    double center[] = new double[4];
    // ak depict => sd = 0
    // berie vonkajsie. nie vnutorne rozmery pri starte z main, preto korekcia
    //int xpix = jme.sd*18 - jme.sd - 20, ypix = jme.sd*16 - jme.sd*3 - 45;
    int xpix = 0, ypix = 0;
    if (jme != null && jme.dimension != null && jme.dimension.width > 0) {
      xpix = jme.dimension.width - jme.sd;
      ypix = jme.dimension.height - jme.sd * 3;
    }
    if (xpix <= 0 || ypix <= 0) {
      needRecentering = true;
      return;
    }

    centerPoint(center);

    int shiftx = xpix / 2 - (int) Math.round(center[0]);
    int shifty = ypix / 2 - (int) Math.round(center[1]);
    if (!jme.nocenter)
      for (int i = 1; i <= natoms; i++) {
        x[i] += shiftx;
        y[i] += shifty;
      }

    findBondCenters();
  }

  // ----------------------------------------------------------------------------
  int testAtomTouch(int xx, int yy) {
    int i, atom;
    double min, dx, dy, rx;

    atom = 0;
    min = TOUCH_LIMIT + 1;
    for (i = 1; i <= natoms; i++) {
      dx = xx - x[i];
      dy = yy - y[i];
      rx = dx * dx + dy * dy;
      if (rx < TOUCH_LIMIT)
        if (rx < min) {
          min = rx;
          atom = i;
        }
    }
    return atom;
  }

  // ----------------------------------------------------------------------------
  int testBondTouch(int xx, int yy) {
    int i, bond;
    double min, dx, dy, rx;

    //if (jme.action == JME.ACTION_CHAIN) return 0; // 2005.02 no bound touch

    bond = 0;
    min = TOUCH_LIMIT + 1;
    for (i = 1; i <= nbonds; i++) {
      dx = xx - xb[i];
      dy = yy - yb[i];
      rx = dx * dx + dy * dy;
      if (rx < TOUCH_LIMIT)
        if (rx < min) {
          min = rx;
          bond = i;
        }
    }
    return bond;
  }

  // ----------------------------------------------------------------------------
  void reset() {
    save();
    natoms = 0;
    nbonds = 0;
    nmarked = 0;
  }

  // ----------------------------------------------------------------------------
  void draw(Graphics g) {
    int atom1, atom2;
    double xa, ya, xb, yb, dx, dy, dd, sina = 1., cosa = 1.;
    double sirka2s, sirka2c;
    double sirka2 = 2., sirka3 = 3.;

    if (needRecentering) {
      center();
      jme.alignMolecules(1, jme.nmols, 0); // !!! nefunguje pre reakcion
      needRecentering = false;
      //System.err.println("DD recenter " + jme.dimension.width);
    }

    if (jme.depictScale != 1.) { // depictScale sa plni len pri depict
      sirka2 *= jme.depictScale;
      sirka3 *= jme.depictScale;
      double xs = 1.0;
      if (jme.depictScale < 0.7)
        xs = 1.2;// aby font nie primaly 
      int fs = (int) (jme.fontSize * jme.depictScale * xs);
      jme.font = new Font("Helvetica", Font.PLAIN, fs);
      jme.fontMet = g.getFontMetrics(jme.font);
    }

    // ked padne, aby aspon ukazalo ramcek
    if (jme.depictBorder) {
      g.setColor(Color.black);
      g.drawRect(0, 0, jme.dimension.width - 1, jme.dimension.height - 1);
    }

    if (natoms == 0)
      return;

    // atom & bond background coloring
    if (doColoring == -1) {
      int cs = (int) Math.round(sirka2 * 12);
      // atoms
      for (int i = 1; i <= natoms; i++) {
        if (abg[i] > 0 && abg[i] < 7) {
          g.setColor(jme.psColor[abg[i]]);
          g.fillOval((int) (x[i] - cs / 2.), (int) (y[i] - cs / 2.), cs, cs);
        }
      }
      // bonds
      // color background for bonds (2 atoms must have the same color)
      for (int i = 1; i <= nbonds; i++) {
        atom1 = va[i];
        atom2 = vb[i];
        if (abg[atom1] == 0)
          continue;
        if (abg[atom1] != abg[atom2])
          continue;
        g.setColor(jme.psColor[abg[atom1]]);
        dx = x[atom2] - x[atom1];
        dy = y[atom2] - y[atom1];
        dd = Math.sqrt(dx * dx + dy * dy);
        if (dd < 1.)
          dd = 1.;
        sina = dy / dd;
        cosa = dx / dd;
        sirka2s = (sirka3 * 3) * sina;
        sirka2c = (sirka3 * 3) * cosa;
        int[] xr = new int[4], yr = new int[4];
        xr[0] = (int) (x[atom1] + sirka2s);
        yr[0] = (int) (y[atom1] - sirka2c);
        xr[1] = (int) (x[atom2] + sirka2s);
        yr[1] = (int) (y[atom2] - sirka2c);
        xr[2] = (int) (x[atom2] - sirka2s);
        yr[2] = (int) (y[atom2] + sirka2c);
        xr[3] = (int) (x[atom1] - sirka2s);
        yr[3] = (int) (y[atom1] + sirka2c);
        g.fillPolygon(xr, yr, 4);
      }
    }

    // bonds
    for (int i = 1; i <= nbonds; i++) {
      g.setColor(Color.black);

      atom1 = va[i];
      atom2 = vb[i];

      if (doColoring == 1) {
        if (abg[atom1] != 0 && abg[atom1] == abg[atom2])
          g.setColor(jme.psColor[abg[atom1]]);
      }

      if (stereob[i] == XUP || stereob[i] == XDOWN) // kvoli spicke vazby 
      {
        int d = atom1;
        atom1 = atom2;
        atom2 = d;
      }

      xa = x[atom1];
      ya = y[atom1];
      xb = x[atom2];
      yb = y[atom2];

      if (nasv[i] != SINGLE || stereob[i] != 0) {
        dx = xb - xa;
        dy = yb - ya;
        dd = Math.sqrt(dx * dx + dy * dy);
        if (dd < 1.)
          dd = 1.;
        sina = dy / dd;
        cosa = dx / dd;
      }
      switch (nasv[i]) {
      case DOUBLE:
        if (stereob[i] >= 10)
          g.setColor(Color.magenta); // E,Z je farebna
        sirka2s = sirka2 * sina;
        sirka2c = sirka2 * cosa;
        g.drawLine((int) Math.round(xa + sirka2s),
            (int) Math.round(ya - sirka2c), (int) Math.round(xb + sirka2s),
            (int) Math.round(yb - sirka2c));
        g.drawLine((int) Math.round(xa - sirka2s),
            (int) Math.round(ya + sirka2c), (int) Math.round(xb - sirka2s),
            (int) Math.round(yb + sirka2c));
        g.setColor(Color.black);
        break;
      case TRIPLE:
        int ixa = (int) Math.round(xa);
        int iya = (int) Math.round(ya);
        int ixb = (int) Math.round(xb);
        int iyb = (int) Math.round(yb);
        g.drawLine(ixa, iya, ixb, iyb);
        int sirka3s = (int) Math.round(sirka3 * sina);
        int sirka3c = (int) Math.round(sirka3 * cosa);
        g.drawLine(ixa + sirka3s, iya - sirka3c, ixb + sirka3s, iyb - sirka3c);
        g.drawLine(ixa - sirka3s, iya + sirka3c, ixb - sirka3s, iyb + sirka3c);
        /*
        g.drawLine((int)Math.round(xa+sirka3s),(int)Math.round(ya-sirka3c),
                   (int)Math.round(xb+sirka3s),(int)Math.round(yb-sirka3c));
        g.drawLine((int)Math.round(xa-sirka3s),(int)Math.round(ya+sirka3c),
                   (int)Math.round(xb-sirka3s),(int)Math.round(yb+sirka3c));
                   */
        break;
      case QUERY:
      case 0: // dotted
        for (int k = 0; k < 10; k++) {
          double xax = xa - (xa - xb) / 10. * k;
          double yax = ya - (ya - yb) / 10. * k;
          g.drawLine((int) Math.round(xax), (int) Math.round(yax),
              (int) Math.round(xax), (int) Math.round(yax));
        }
        // query bond text
        g.setFont(jme.font);
        int h = jme.fontMet.getAscent(); // vyska fontu
        /*
        String z = "?";
        switch (stereob[i]) {
        case QB_ANY: z = "~"; break;
        case QB_AROMATIC: z = ":"; break;
        case QB_RING: z = "@"; break;
        case QB_NONRING: z = "!@"; break;
        }
        */
        // 2007.10 dqp support
        Object o = btag[i];
        String z = "?";
        if (o != null)
          z = (String) o;
        int w = jme.fontMet.stringWidth(z);
        int xstart = (int) Math.round((xa + xb) / 2. - w / 2.);
        int ystart = (int) Math.round((ya + yb) / 2. + h / 2 - 1); // o 1 vyssie
        //g.setColor(Color.white);
        //g.fillRect(xstart-1,ystart-h+2,w+1,h-1);
        g.setColor(Color.magenta);
        g.drawString(z, xstart, ystart);
        g.setColor(Color.black);
        break;
      default: // SINGLE, alebo stereo
        if (stereob[i] == UP || stereob[i] == XUP) {
          sirka2s = sirka3 * sina;
          sirka2c = sirka3 * cosa;
          int[] px = new int[3];
          int[] py = new int[3];
          px[0] = (int) Math.round(xb + sirka2s);
          py[0] = (int) Math.round(yb - sirka2c);
          px[1] = (int) Math.round(xa);
          py[1] = (int) Math.round(ya);
          px[2] = (int) Math.round(xb - sirka2s);
          py[2] = (int) Math.round(yb + sirka2c);
          g.fillPolygon(px, py, 3);
        } else if (stereob[i] == DOWN || stereob[i] == XDOWN) {
          sirka2s = sirka3 * sina;
          sirka2c = sirka3 * cosa;
          for (int k = 0; k < 10; k++) {
            double xax = xa - (xa - xb) / 10. * k;
            double yax = ya - (ya - yb) / 10. * k;
            double sc = k / 10.;
            g.drawLine((int) Math.round(xax + sirka2s * sc),
                (int) Math.round(yax - sirka2c * sc),
                (int) Math.round(xax - sirka2s * sc),
                (int) Math.round(yax + sirka2c * sc));
          }
        } else // normal single bonds 
          g.drawLine((int) Math.round(xa), (int) Math.round(ya),
              (int) Math.round(xb), (int) Math.round(yb));
        break;

      }
      // bond tags
      if (jme.doTags) {
        if (btag[i] != null && btag[i].length() > 0) {
          g.setFont(jme.font);
          int h = jme.fontMet.getAscent(); // vyska fontu
          int w = jme.fontMet.stringWidth(btag[i]);
          int xstart = (int) Math.round((xa + xb) / 2. - w / 2.);
          int ystart = (int) Math.round((ya + yb) / 2. + h / 2 - 1); // o 1 vyssie
          g.setColor(Color.red);
          g.drawString(btag[i], xstart, ystart);
          g.setColor(Color.black);
        }
      }
    }

    // atom labels 
    g.setFont(jme.font);
    int h = jme.fontMet.getAscent(); // vyska fontu
    String[] zz = new String[natoms + 1];
    for (int i = 1; i <= natoms; i++) {
      String z = getAtomLabel(i);
      if (an[i] == JME.AN_C && nv[i] > 0 && q[i] == 0 && !TESTDRAW) {
        zz[i] = z;
        continue;
      }
      // nekresli C na allene (problemy s #C-)
      if (jme.showHydrogens && !TESTDRAW) {
        if (nh[i] > 0)
          z = z + "H";
        if (nh[i] > 1)
          z = z + nh[i];
      }
      // charges
      if (q[i] != 0) {
        if (Math.abs(q[i]) > 1)
          z += Math.abs(q[i]);
        if (q[i] > 0)
          z += "+";
        else
          z += "-";
      }
      if (TESTDRAW)
        z += i;
      zz[i] = z;

      if (z == null || z.length() < 1) {
        z = "*";
        System.err.println("Z error!");
      }

      int w = jme.fontMet.stringWidth(z);
      int xstart = (int) Math.round(x[i] - w / 2.);
      int ystart = (int) Math.round(y[i] + h / 2 - 1); // o 1 vyssie
      g.setColor(jme.canvasBg);
      if (doColoring == -1 && abg[i] != 0)
        g.setColor(jme.psColor[abg[i]]);
      g.fillRect(xstart - 1, ystart - h + 2, w + 1, h - 1);
      if (doColoring == 1) {
        // !!! nefarbi single carbons, co s tym ???
        if (abg[i] != 0)
          g.setColor(jme.psColor[abg[i]]);
        else
          g.setColor(Color.black);
      } else
        g.setColor(jme.color[an[i]]);
      if (jme.bwMode)
        g.setColor(Color.black);
      g.drawString(z, xstart, ystart);
    }

    // marked atoms - islo by to do predosleho loopu zapasovat ???
    for (int k = 1; k <= nmarked; k++) {
      int atom = mark[k][0];
      int w = jme.fontMet.stringWidth(zz[atom]);
      int xstart = (int) Math.round(x[atom] - w / 2.);
      int ystart = (int) Math.round(y[atom] + h / 2 - 1); // o 1 vyssie
      g.setColor(Color.magenta);
      g.drawString(" " + mark[k][1], xstart + w, ystart);
    }

    // tags (povodne to bolo label)
    if (jme.doTags) {
      for (int i = 1; i <= natoms; i++) {
        if (atag[i] == null || atag[i].equals(""))
          continue;
        int w = jme.fontMet.stringWidth(zz[i]);
        int xstart = (int) Math.round(x[i] - w / 2.);
        int ystart = (int) Math.round(y[i] + h / 2 - 1); // o 1 vyssie
        g.setColor(Color.red);
        g.drawString(" " + atag[i], xstart + w, ystart);
      }
    }

    // mark touched bond or atom, or atoms marked to delete
    if ((touchedAtom > 0 || touchedBond > 0) && !jme.webme) {

      g.setColor(jme.action == JME.ACTION_DELETE ? Color.red : Color.blue);

      if (touchedAtom > 0 && jme.action != JME.ACTION_DELGROUP) {
        int w = jme.fontMet.stringWidth(zz[touchedAtom]);
        g.drawRect((int) Math.round(x[touchedAtom] - w / 2. - 1),
            (int) Math.round(y[touchedAtom] - h / 2. - 1), w + 2, h + 2);
      }

      if (touchedBond > 0) {
        atom1 = va[touchedBond];
        atom2 = vb[touchedBond];
        dx = x[atom2] - x[atom1];
        dy = y[atom2] - y[atom1];
        dd = Math.sqrt(dx * dx + dy * dy);
        if (dd < 1.)
          dd = 1.;
        sina = dy / dd;
        cosa = dx / dd;
        sirka2s = (sirka3 + 1) * sina;
        sirka2c = (sirka3 + 1) * cosa;
        int[] px = new int[5];
        int[] py = new int[5];
        px[0] = (int) Math.round(x[atom1] + sirka2s);
        px[1] = (int) Math.round(x[atom2] + sirka2s);
        py[0] = (int) Math.round(y[atom1] - sirka2c);
        py[1] = (int) Math.round(y[atom2] - sirka2c);
        px[3] = (int) Math.round(x[atom1] - sirka2s);
        px[2] = (int) Math.round(x[atom2] - sirka2s);
        py[3] = (int) Math.round(y[atom1] + sirka2c);
        py[2] = (int) Math.round(y[atom2] + sirka2c);
        px[4] = px[0];
        py[4] = py[0]; // bug in 1.0
        if (jme.action != JME.ACTION_DELGROUP) // pri DELGROUP nekresli modro
          g.drawPolygon(px, py, 5);

        markGroup: if (jme.action == JME.ACTION_DELGROUP) {
          // marks atoms with unpleasent fate (suggested by Bernd Rohde)
          if (!isRotatableBond(va[touchedBond], vb[touchedBond]))
            break markGroup;
          int nsub = 0;
          for (int i = 1; i <= natoms; i++)
            if (a[i] > 0)
              nsub++;
          if (nsub > natoms / 2) {
            // revert a[]
            for (int i = 1; i <= natoms; i++)
              if (a[i] > 0)
                a[i] = 0;
              else
                a[i] = 1;
          }
          // framing atoms to delete in red
          g.setColor(Color.red);
          for (int i = 1; i <= natoms; i++)
            if (a[i] > 0) {
              int w = jme.fontMet.stringWidth(zz[i]);
              g.drawRect((int) Math.round(x[i] - w / 2. - 1),
                  (int) Math.round(y[i] - h / 2. - 1), w + 2, h + 2);
            }
        }
      }
    }

    // filling points for webme
    if (jme.webme) {
      jme.apointx = new int[natoms];
      jme.apointy = new int[natoms];
      jme.bpointx = new int[nbonds];
      jme.bpointy = new int[nbonds];
      for (int i = 1; i <= natoms; i++) {
        jme.apointx[i - 1] = (int) Math.round(x[i]);
        jme.apointy[i - 1] = (int) Math.round(y[i]);
      }
      for (int i = 1; i <= nbonds; i++) {
        jme.bpointx[i - 1] = (int) Math.round((x[va[i]] + x[vb[i]]) / 2.);
        jme.bpointy[i - 1] = (int) Math.round((y[va[i]] + y[vb[i]]) / 2.);
      }
    }

  }

  // ----------------------------------------------------------------------------
  void move(int movex, int movey) {
    // move
    // ??? check, aby sa nedostalo do trap, ked je molekula za okrajom
    for (int i = 1; i <= natoms; i++) {
      x[i] += movex;
      y[i] += movey;
    }

    // checking if still visible
    double center[] = new double[4];
    centerPoint(center);
    //musi mysliet aj na moznu rotaciu
    double centerx = center[0];
    double centery = center[1];
    if (centerx > 0 && centerx < jme.dimension.width - jme.sd && centery > 0
        && centery < jme.dimension.height - jme.sd * 3)
      return;

    // molecule out of box, back move
    for (int i = 1; i <= natoms; i++) {
      x[i] -= movex;
      y[i] -= movey;
    }

  }

  // ----------------------------------------------------------------------------
  void rotate(int movex) {
    double center[] = new double[4];

    // get original position
    centerPoint(center);
    double centerx = center[0];
    double centery = center[1];

    // rotation
    double sinu = Math.sin(movex * Math.PI / 180.);
    double cosu = Math.cos(movex * Math.PI / 180.);
    for (int i = 1; i <= natoms; i++) {
      double xx = x[i] * cosu + y[i] * sinu;
      double yy = -x[i] * sinu + y[i] * cosu;
      x[i] = xx;
      y[i] = yy;
    }

    // moving to original position
    centerPoint(center);
    for (int i = 1; i <= natoms; i++) {
      x[i] += centerx - center[0];
      y[i] += centery - center[1];
    }
  }

  // ----------------------------------------------------------------------------
  void centerPoint(double center[]) {
    // returns x and y of the centre of the molecule
    // also x and y dimensions (for depict)
    double minx = 9999., maxx = -9999., miny = 9999., maxy = -9999.;
    for (int i = 1; i <= natoms; i++) {
      if (x[i] < minx)
        minx = x[i];
      if (x[i] > maxx)
        maxx = x[i];
      if (y[i] < miny)
        miny = y[i];
      if (y[i] > maxy)
        maxy = y[i];
    }
    center[0] = (minx + (maxx - minx) / 2.);
    center[1] = (miny + (maxy - miny) / 2.);
    // for scaling in depict
    center[2] = maxx - minx;
    center[3] = maxy - miny;
    if (center[2] < RBOND)
      center[2] = RBOND;
    if (center[3] < RBOND)
      center[3] = RBOND;
  }

  // ----------------------------------------------------------------------------
  void rubberBanding(int xnew, int ynew) {
    // len pre vazby
    int atom;
    double dx, dy, rx, sina, cosa;
    // povodny touchedAtom je ulozeny v touched_org (urobene v mouse_down)

    touchedAtom = 0;

    x[0] = xnew;
    y[0] = ynew; // position of the mouse (? nie totozne s natoms)
    atom = checkTouch(0);
    if (atom > 0 && jme.action != JME.ACTION_CHAIN) { // pri chaine to blblo
      touchedAtom = atom;
      if (atom != touched_org) { // make bond towards atom
        x[natoms] = x[atom];
        y[natoms] = y[atom];
      } else { // this was standard position of the bond
        x[natoms] = xorg;
        y[natoms] = yorg;
      }
    } else {
      if (jme.action == JME.ACTION_CHAIN) {
        // first atom (chain=1) was added in mouseDown
        // ma 4 moznosti: back, flip, add1, add2
        // chain[0] is origin, chain[1] moze len flipnut, nie deletnut
        // miesto add sa moze aj napojit na existing atom

        touchedBond = 0; // 2005.02 (no marked bond)
        // action according to the mouse position
        int last = chain[nchain]; // last atom
        int parent = chain[nchain - 1];
        dx = x[last] - x[parent];
        dy = y[last] - y[parent];
        rx = Math.sqrt(dx * dx + dy * dy);
        if (rx < 1.0)
          rx = 1.0;
        sina = dy / rx;
        cosa = dx / rx;
        double vv = rx / 2. / Math.tan(Math.PI / 6.);
        // moving mouse pos
        double xx = xnew - x[parent];
        double yy = ynew - y[parent];
        double xm = -rx / 2. + xx * cosa + yy * sina; // relativ to "0"
        double ym = yy * cosa - xx * sina; // hore / dolu
        // zistuje poziciu mouse point relativne k trojuholniku
        if (xm < 0.) { // delete this atom
          // special treatment per 1. atom (inak sa vzdy vymaze)
          if (nchain > 1) { // !!!
            deleteAtom(natoms);
            nchain--;
            stopChain = false;
          } else if (natoms == 2) { // first 2 atoms / \ flip (4 positions)
            // up down flip
            if (y[2] - y[1] < 0 && ynew - y[1] > 0)
              y[2] = y[1] + rx / 2.;
            else if (y[2] - y[1] > 0 && ynew - y[1] < 0)
              y[2] = y[1] - rx / 2.;
            // left right flip
            if (x[2] - x[1] < 0 && xnew - x[1] > 0)
              x[2] = x[1] + rx * .866;
            else if (x[2] - x[1] > 0 && xnew - x[1] < 0)
              x[2] = x[1] - rx * .866;
          } else { // skusa flipnut 1. atom (x je vzdy -RBOND !) okolo chain[0]
            if (nv[chain[0]] == 2) { // i.e. moze flipnut
              int ref = v[chain[0]][1];
              if (ref == chain[1])
                ref = v[chain[0]][2];
              // flipne len ked mouse na opacnej strane ref---chain[0] ako ch[1]
              dx = x[chain[0]] - x[ref];
              dy = y[chain[0]] - y[ref];
              rx = Math.sqrt(dx * dx + dy * dy);
              if (rx < 1.0)
                rx = 1.0;
              sina = dy / rx;
              cosa = dx / rx;
              // moving mouse pos
              xx = xnew - x[ref];
              yy = ynew - y[ref];
              double ymm = yy * cosa - xx * sina; // hore / dolu
              // moving chain[1] 
              xx = x[chain[1]] - x[ref];
              yy = y[chain[1]] - y[ref];
              double yc1 = yy * cosa - xx * sina; // hore / dolu
              if (ymm > 0. && yc1 < 0. || ymm < 0. && yc1 > 0.) { // su opacne
                int bd = nbonds;
                touchedAtom = chain[0];
                addBond(); // adds new bond
                deleteBond(bd); // delets old bond
                if (checkTouch(natoms) > 0)
                  stopChain = true;
              }
            }
          }
        } else {
          if (stopChain)
            return;
          // calculates triangle height at this position
          double th = -1.; // mouse too far right
          if (xm < rx * 1.5)
            th = (rx * 1.5 - xm) * vv / (rx * 1.5);
          if (Math.abs(ym) > th) { // mouse above/below trinagle border
            nchain++;
            if (nchain > 100) {
              jme.info("You are too focused on chains, enough of it for now !");
              nchain--;
              return;
            }
            touchedAtom = natoms;
            addBond((int) Math.round(ym));
            chain[nchain] = natoms;
            if (checkTouch(natoms) > 0)
              stopChain = true;
          }
        }

        touchedAtom = 0;
        // when starting from scratch ukazuje dlzku mensiu o 1
        int n = nchain;

        jme.info(n + ""); // napise dlzku do info 
      } // end ACTION_CHAIN

      else { // bond width normal length folows mouse 
        dx = xnew - x[touched_org];
        dy = ynew - y[touched_org];
        rx = Math.sqrt(dx * dx + dy * dy);
        if (rx < 1.0)
          rx = 1.0;
        sina = dy / rx;
        cosa = dx / rx;
        x[natoms] = x[touched_org] + RBOND * cosa;
        y[natoms] = y[touched_org] + RBOND * sina;
      }
    }
  }

  // ----------------------------------------------------------------------------
  void checkChain() {
    // called from mouseUp after finishing chain
    if (stopChain) { // if overlap, then last added atom
      int n = checkTouch(natoms);
      // adding bond natoms - natoms-1 to n
      if (nv[n] < MAX_BONDS_ON_ATOM) { // if ==, no error message
        // making bond n - nchain-1
        createBond();
        int parent = chain[nchain - 1];
        va[nbonds] = n;
        vb[nbonds] = parent;
        v[n][++nv[n]] = parent;
        v[parent][++nv[parent]] = n;
      }
      deleteAtom(natoms);
    }
    stopChain = false;
  }

  // ----------------------------------------------------------------------------
  private int checkTouch(int atom) {
    // checking touch of atom with other atoms
    double dx, dy, rx;
    double min = TOUCH_LIMIT + 1;
    int touch = 0;
    for (int i = 1; i < natoms; i++) { // < natoms ???
      if (atom == i)
        continue;
      dx = x[atom] - x[i];
      dy = y[atom] - y[i];
      rx = dx * dx + dy * dy;
      if (rx < TOUCH_LIMIT)
        if (rx < min) {
          min = rx;
          touch = i;
        }
    }
    return touch;
  }

  // ----------------------------------------------------------------------------
  void avoidTouch(int from) {
    // checking atom overlap and moving atoms away from each other
    // moving always atom with the higher number 
    // called after GROUP or CHAIN
    if (from == 0)
      from = natoms; // checks last from atoms

    for (int i = natoms; i > natoms - from; i--) {
      int n = checkTouch(i);
      if (n == 0)
        continue;
      // moving i away from n
      x[i] += 6;
      y[i] += 6;
      //      }
    }
  }

  // ----------------------------------------------------------------------------
  void deleteAtom(int delatom) {
    int i, j, k, atom1, atom2;

    // actualises bonds
    j = 0;
    for (i = 1; i <= nbonds; i++) {
      atom1 = va[i];
      atom2 = vb[i];
      if (atom1 != delatom && atom2 != delatom) {
        j++;
        va[j] = atom1;
        if (atom1 > delatom)
          va[j]--;
        vb[j] = atom2;
        if (atom2 > delatom)
          vb[j]--;
        nasv[j] = nasv[i];
        stereob[j] = stereob[i];
        xb[j] = xb[i];
        yb[j] = yb[i];
        btag[j] = btag[i];
      }
    }
    nbonds = j;

    for (i = delatom; i < natoms; i++) {
      an[i] = an[i + 1];
      q[i] = q[i + 1];
      x[i] = x[i + 1];
      y[i] = y[i + 1];
      nh[i] = nh[i + 1];
      abg[i] = abg[i + 1];
      atag[i] = atag[i + 1];
      nv[i] = nv[i + 1];
      label[i] = label[i + 1];
      for (j = 1; j <= nv[i]; j++)
        v[i][j] = v[i + 1][j];
    }
    natoms--;
    if (natoms == 0) {
      jme.clear();
      return;
    }

    // updating nv[] and v[][]
    // updating also nh on neighbors (added in Oct 04 to fix canonisation)
    for (i = 1; i <= natoms; i++) {
      k = 0;
      for (j = 1; j <= nv[i]; j++) {
        atom1 = v[i][j];
        if (atom1 == delatom) {
          nh[i]++;
          continue;
        } // added nh[i]++ 10.04
        if (atom1 > delatom)
          atom1--;
        v[i][++k] = atom1;
      }
      nv[i] = k;
    }

    // updating marked atoms
    iloop: for (i = 1; i <= nmarked; i++)
      if (mark[i][0] == delatom) {
        for (j = i; j < nmarked; j++) {
          mark[j][0] = mark[j + 1][0];
          mark[j][1] = mark[j + 1][1];
        }
        nmarked--;
        break iloop;
      }
    for (i = 1; i <= nmarked; i++)
      if (mark[i][0] > delatom)
        mark[i][0]--;
  }

  // ----------------------------------------------------------------------------
  void deleteBond(int delbond) {
    // deletes bond between atoms delat1 and delat2
    int i, k, atom1, atom2;

    atom1 = va[delbond];
    atom2 = vb[delbond];

    for (i = delbond; i < nbonds; i++) {
      va[i] = va[i + 1];
      vb[i] = vb[i + 1];
      nasv[i] = nasv[i + 1];
      stereob[i] = stereob[i + 1];
      xb[i] = xb[i + 1];
      yb[i] = yb[i + 1];
      btag[i] = btag[i + 1];
    }
    nbonds--;

    // updating nv[] and v[][]
    k = 0;
    for (i = 1; i <= nv[atom1]; i++)
      if (v[atom1][i] != atom2)
        v[atom1][++k] = v[atom1][i];
    nv[atom1] = k;
    k = 0;
    for (i = 1; i <= nv[atom2]; i++)
      if (v[atom2][i] != atom1)
        v[atom2][++k] = v[atom2][i];
    nv[atom2] = k;

    // deleting lonely atom(s)
    if (atom1 < atom2) {
      k = atom1;
      atom1 = atom2;
      atom2 = k;
    }
    if (nv[atom1] == 0)
      deleteAtom(atom1);
    if (nv[atom2] == 0)
      deleteAtom(atom2);
  }

  // ----------------------------------------------------------------------------
  void deleteGroup(int bond) {

    // need to mark atoms to delete for WebME
    if (jme.webme) {
      if (!isRotatableBond(va[touchedBond], vb[touchedBond]))
        return;
      int nsub = 0;
      for (int i = 1; i <= natoms; i++)
        if (a[i] > 0)
          nsub++;
      if (nsub > natoms / 2) {
        // revert a[]
        for (int i = 1; i <= natoms; i++)
          if (a[i] > 0)
            a[i] = 0;
          else
            a[i] = 1;
      }
    }

    // group to delete has already a[] marked (done in draw())
    if (a[va[bond]] > 0 && a[vb[bond]] > 0) {
      jme.info("Removal of substituent not possible.");
      return;
    }

    while (true) {
      int atd = 0;
      for (int i = natoms; i >= 1; i--)
        if (a[i] > 0 && i > atd) {
          atd = i;
        }
      if (atd == 0)
        break;
      deleteAtom(atd);
      a[atd] = 0;
    }
  }

  // ----------------------------------------------------------------------------
  // called after delete atom/bond/group
  // when >NH+< remove charge, so it will be -N< and not -NH+<
  void backCations(int atom) {
    for (int i = 1; i <= nv[atom]; i++) {
      int j = v[atom][i];
      if (q[j] > 0)
        q[j]--;
    }
  }

  // ----------------------------------------------------------------------------
  void backCations(int atom1, int atom2) {
    if (q[atom1] > 0)
      q[atom1]--;
    if (q[atom2] > 0)
      q[atom2]--;
  }

  // ----------------------------------------------------------------------------
  void flipGroup(int atom) {
    // flip group on this atom
    if (nv[atom] < 2)
      return;
  }

  // ----------------------------------------------------------------------------
  void stereoBond(int bond) {
    // alebo vola z drawingArea.mouseDown s (touchBond) a nasv je rozna,
    // alebo z completeBond, vtedy je nasv vzdy 1 
    // robi to inteligente, presmykuje medzi 4, len kde je to mozne
    // v stereob je uschovane aj querytype ked ide o QUERY bond
    if (nasv[bond] == SINGLE) {
      // UP a DOWN daju hrot na va[], XUP, XDOWN na vb[]
      int atom1 = va[bond];
      int atom2 = vb[bond];
      if (nv[atom1] < 2 && nv[atom2] < 2) { // <=2 nemoze byt kvoli allenu
        stereob[bond] = 0;
        jme.info("Stereomarking meaningless on this bond !");
        return;
      }
      // atom1 - stary, atom2 - novy atom
      if (jme.webme) {
        // handling webme (up / down templates)
        // just switching up/xup and down/xdown
        if (!jme.revertStereo) {
          if (stereob[bond] == UP)
            stereob[bond] = XUP;
          else if (stereob[bond] == XUP)
            stereob[bond] = UP;
          else {
            if (nv[atom2] <= nv[atom1])
              stereob[bond] = UP;
            else
              stereob[bond] = XUP;
          }
        } else {
          if (stereob[bond] == DOWN)
            stereob[bond] = XDOWN;
          else if (stereob[bond] == XDOWN)
            stereob[bond] = DOWN;
          else {
            if (nv[atom2] <= nv[atom1])
              stereob[bond] = DOWN;
            else
              stereob[bond] = XDOWN;
          }
        }
      }

      // standard editor stuff
      else {
        switch (stereob[bond]) {
        case 0: // aby bol hrot spravne (nie na nerozvetvenom) 
          // UP dava normalne hrot na va[]
          if (nv[atom2] <= nv[atom1])
            stereob[bond] = UP;
          else
            stereob[bond] = XUP;
          break;
        case UP:
          stereob[bond] = DOWN;
          break;
        case DOWN:
          if (nv[atom2] > 2)
            stereob[bond] = XUP;
          else
            stereob[bond] = UP;
          break;
        case XUP:
          stereob[bond] = XDOWN;
          break;
        case XDOWN:
          if (nv[atom1] > 2)
            stereob[bond] = UP;
          else
            stereob[bond] = XUP;
          break;
        }
      }
    } else if (nasv[bond] == DOUBLE) {
      if (stereob[bond] == EZ)
        stereob[bond] = 0;
      else
        stereob[bond] = EZ;
    } else {
      jme.info("Stereomarking allowed only on single and double bonds!");
    }
  }

  // ----------------------------------------------------------------------------
  // returns stereo atom to which this bond points
  int getStereoAtom(int bond) {
    // UP a DOWN daju hrot na va[], XUP, XDOWN na vb[]
    switch (stereob[bond]) {
    case UP:
    case DOWN:
      return va[bond];
    case XUP:
    case XDOWN:
      return vb[bond];
    }
    return 0;
  }

  // ----------------------------------------------------------------------------
  void addBond() {
    addBond(0);
  }

  // ----------------------------------------------------------------------------
  void addBond(int up) {
    // pridava atom a jeho koordinaty
    int i, atom1, atom3;
    double dx, dy, rx, sina, cosa, xx, yy;

    createAtom();
    switch (nv[touchedAtom]) {
    case 0:
      x[natoms] = x[touchedAtom] + rbond() * .866;
      y[natoms] = y[touchedAtom] + rbond() * .5;
      break;
    case 1:
      atom1 = v[touchedAtom][1];
      atom3 = 0; // reference, aby to slo rovno
      if (nv[atom1] == 2) {
        if (v[atom1][1] == touchedAtom)
          atom3 = v[atom1][2];
        else
          atom3 = v[atom1][1];
      }
      dx = x[touchedAtom] - x[atom1];
      dy = y[touchedAtom] - y[atom1];
      rx = Math.sqrt(dx * dx + dy * dy);
      if (rx < 0.001)
        rx = 0.001;
      sina = dy / rx;
      cosa = dx / rx;
      xx = rx + rbond() * Math.cos(Math.PI / 3.);
      yy = rbond() * Math.sin(Math.PI / 3.);
      // checking for allene -N=C=S, X#C-, etc
      // chain je ako linear !
      i = bondIdentity(touchedAtom, atom1);
      if ((nasv[i] == TRIPLE) || jme.action == JME.ACTION_BOND_TRIPLE
          || (!isSingle(i) && (jme.action == JME.ACTION_BOND_DOUBLE
              || jme.action == JME.ACTION_BOND_TRIPLE))
          || linearAdding) // linearAdding pre ACTION_TBU
      {
        xx = rx + rbond();
        yy = 0.;
      }
      if (atom3 > 0) // to keep growing chain linear
        if (((y[atom3] - y[atom1]) * cosa - (x[atom3] - x[atom1]) * sina) > 0.)
          yy = -yy;
      // flip bond to other site
      if (up > 0 && yy < 0.)
        yy = -yy;
      else if (up < 0 && yy > 0.)
        yy = -yy;

      x[natoms] = x[atom1] + xx * cosa - yy * sina;
      y[natoms] = y[atom1] + yy * cosa + xx * sina;
      break;

    case 2:
      double[] newPoint = new double[2];
      addPoint(touchedAtom, rbond(), newPoint);
      x[natoms] = newPoint[0];
      y[natoms] = newPoint[1];
      break;

    case 3:
    case 4:
    case 5:
      // postupne skusa linearne predlzenie vsetkych vazieb z act_a
      for (i = 1; i <= nv[touchedAtom]; i++) {
        atom1 = v[touchedAtom][i];
        dx = x[touchedAtom] - x[atom1];
        dy = y[touchedAtom] - y[atom1];
        rx = Math.sqrt(dx * dx + dy * dy);
        if (rx < 0.001)
          rx = 0.001;
        x[natoms] = x[touchedAtom] + rbond() * dx / rx;
        y[natoms] = y[touchedAtom] + rbond() * dy / rx;
        // teraz testuje ci sa nedotyka
        if (checkTouch(natoms) == 0 || i == nv[touchedAtom])
          break;
      }
      break;

    default: // error
      natoms--;
      jme.info("Are you trying to draw an hedgehog ?");
      jme.lastAction = JME.LA_FAILED; //aby nevolalo checkNewBond
      return;
    //break;
    }
    completeBond();

    xorg = x[natoms];
    yorg = y[natoms]; // used after moving, when moving !OK

    // for (i=1;i<=natoms;i++)
    //   System.out.println(i+" "+nv[i]+" "+x[i]+" "+y[i]); 

  }

  // ----------------------------------------------------------------------------
  // necessary to add "smaller" bonds in scaled molecule bt WebME
  double rbond() {
    return RBOND * jme.depictScale;
  }

  // ----------------------------------------------------------------------------
  void completeBond() {
    // info pre novy atom a bond
    nv[natoms] = 1;
    nv[touchedAtom]++;
    createBond();
    nasv[nbonds] = SINGLE; // automaticky aj pre CHAIN
    if (jme.action == JME.ACTION_BOND_DOUBLE)
      nasv[nbonds] = DOUBLE;
    if (jme.action == JME.ACTION_BOND_TRIPLE)
      nasv[nbonds] = TRIPLE;
    va[nbonds] = touchedAtom;
    vb[nbonds] = natoms;
    // creating new bond with stereo tool
    if (jme.action == JME.ACTION_STEREO)
      stereoBond(nbonds);

    // toto moze byt este v check bond vyhodene
    v[natoms][1] = touchedAtom;
    v[touchedAtom][nv[touchedAtom]] = natoms;
    xb[nbonds] = (int) Math.round((x[touchedAtom] + x[natoms]) / 2.);
    yb[nbonds] = (int) Math.round((y[touchedAtom] + y[natoms]) / 2.);
  }

  // ----------------------------------------------------------------------------
  void checkBond() {
    // check ci sa novo pridany atom neprekryva s nejakym starym
    // natoms bol posledne pridany atom, bol pridany k touchedAtom

    int i, atom, atom1, atom2;

    // check for touch of end of new bond with some atom
    atom = checkTouch(natoms);
    if (atom == 0)
      return;

    // skutocne sa dotyka atomu atom
    natoms--;

    // check ci to iba nezvysi nasobnost zdvojenim existujucej vazby
    for (i = 1; i < nbonds; i++) {
      atom1 = va[i];
      atom2 = vb[i];
      if ((atom1 == atom && atom2 == touched_org)
          || (atom1 == touched_org && atom2 == atom)) {
        nbonds--;
        nv[touched_org]--;
        if (nasv[i] < TRIPLE) {
          nasv[i]++;
          stereob[i] = 0;
        } // stereo zrusi
        else
          jme.info("Maximum allowed bond order is 3 !");
        return;
      }
    }

    if (nv[atom] == MAX_BONDS_ON_ATOM) {
      nbonds--;
      nv[touched_org]--;
      jme.info("Not possible connection !");
      return;
    }

    // zmeni vazbove data na touched_org a atom
    vb[nbonds] = atom;
    v[atom][++nv[atom]] = touched_org;
    v[touched_org][nv[touched_org]] = atom;
    xb[nbonds] = (int) Math.round((x[touched_org] + x[atom]) / 2.);
    yb[nbonds] = (int) Math.round((y[touched_org] + y[atom]) / 2.);

  }

  // ----------------------------------------------------------------------------
  void addGroup(boolean emptyCanvas) {
    //
    touched_org = touchedAtom;
    int nadded = 0;
    if (jme.action == JME.ACTION_GROUP_TBU
        || jme.action == JME.ACTION_GROUP_CCL3
        || jme.action == JME.ACTION_GROUP_CF3
        || jme.action == JME.ACTION_GROUP_SULFO
        || jme.action == JME.ACTION_GROUP_PO3H2
        || jme.action == JME.ACTION_GROUP_SO2NH2) {
      addBond();
      touchedAtom = natoms;
      linearAdding = true; // pre addAtom
      addBond();
      linearAdding = false;
      touchedAtom = natoms - 1;
      addBond();
      touchedAtom = natoms - 2;
      addBond();
      if (jme.action == JME.ACTION_GROUP_CCL3) {
        an[natoms] = JME.AN_CL;
        an[natoms - 1] = JME.AN_CL;
        an[natoms - 2] = JME.AN_CL;
      }
      if (jme.action == JME.ACTION_GROUP_CF3) {
        an[natoms] = JME.AN_F;
        an[natoms - 1] = JME.AN_F;
        an[natoms - 2] = JME.AN_F;
      }
      if (jme.action == JME.ACTION_GROUP_SULFO) {
        an[natoms] = JME.AN_O;
        an[natoms - 1] = JME.AN_O;
        an[natoms - 2] = JME.AN_O;
        an[natoms - 3] = JME.AN_S;
        nasv[nbonds] = DOUBLE;
        nasv[nbonds - 1] = DOUBLE;
      }
      if (jme.action == JME.ACTION_GROUP_SO2NH2) {
        an[natoms] = JME.AN_O;
        an[natoms - 1] = JME.AN_O;
        an[natoms - 2] = JME.AN_N;
        an[natoms - 3] = JME.AN_S;
        nasv[nbonds] = DOUBLE;
        nasv[nbonds - 1] = DOUBLE;
      }
      if (jme.action == JME.ACTION_GROUP_PO3H2) {
        an[natoms] = JME.AN_O;
        an[natoms - 1] = JME.AN_O;
        an[natoms - 2] = JME.AN_O;
        an[natoms - 3] = JME.AN_P;
        nasv[nbonds] = DOUBLE;
      }
      nadded = 4;
    } else if (jme.action == JME.ACTION_GROUP_NHSO2ME) {
      addBond();
      an[natoms] = JME.AN_N;
      touchedAtom = natoms;
      addBond();
      an[natoms] = JME.AN_S;
      touchedAtom = natoms;
      linearAdding = true;
      addBond();
      linearAdding = false;
      touchedAtom = natoms - 1;
      addBond();
      an[natoms] = JME.AN_O;
      nasv[nbonds] = DOUBLE;
      touchedAtom = natoms - 2;
      addBond();
      an[natoms] = JME.AN_O;
      nasv[nbonds] = DOUBLE;
      nadded = 5;
    } else if (jme.action == JME.ACTION_GROUP_NITRO) {
      addBond();
      an[natoms] = JME.AN_N;
      touchedAtom = natoms;
      addBond();
      an[natoms] = JME.AN_O;
      nasv[nbonds] = DOUBLE;
      touchedAtom = natoms - 1;
      addBond();
      an[natoms] = JME.AN_O;
      nasv[nbonds] = DOUBLE;
      nadded = 3;
    } else if (jme.action == JME.ACTION_GROUP_COO) {
      addBond();
      touchedAtom = natoms;
      addBond();
      an[natoms] = JME.AN_O;
      touchedAtom = natoms - 1;
      addBond();
      an[natoms] = JME.AN_O;
      nasv[nbonds] = DOUBLE;
      nadded = 3;
    } else if (jme.action == JME.ACTION_GROUP_COOME) {
      addBond();
      touchedAtom = natoms;
      addBond();
      an[natoms] = JME.AN_O;
      touchedAtom = natoms;
      addBond();
      touchedAtom = natoms - 2;
      addBond();
      an[natoms] = JME.AN_O;
      nasv[nbonds] = DOUBLE;
      nadded = 4;
    } else if (jme.action == JME.ACTION_GROUP_OCOME) {
      addBond();
      an[natoms] = JME.AN_O;
      touchedAtom = natoms;
      addBond();
      touchedAtom = natoms;
      addBond();
      touchedAtom = natoms - 1;
      addBond();
      nasv[nbonds] = DOUBLE;
      an[natoms] = JME.AN_O;
      nadded = 4;
    } else if (jme.action == JME.ACTION_GROUP_NME2) {
      addBond();
      an[natoms] = JME.AN_N;
      touchedAtom = natoms;
      addBond();
      touchedAtom = natoms - 1;
      addBond();
      nadded = 3;
    } else if (jme.action == JME.ACTION_GROUP_CC) {
      addBond();
      touchedAtom = natoms;
      linearAdding = true; // pre addAtom
      addBond();
      nasv[nbonds] = TRIPLE;
      linearAdding = false; // pre addAtom
      nadded = 2;
    } else if (jme.action == JME.ACTION_GROUP_COH) { // -C=O (fixed 2005.04
      addBond();
      touchedAtom = natoms;
      addBond();
      nasv[nbonds] = DOUBLE;
      an[natoms] = JME.AN_O;
      nadded = 2;
    } else if (jme.action == JME.ACTION_GROUP_dO) { // =O
      addBond();
      nasv[nbonds] = DOUBLE;
      an[natoms] = JME.AN_O;
      nadded = 1;
    } else if (jme.action == JME.ACTION_GROUP_CCC) {
      addBond();
      touchedAtom = natoms;
      linearAdding = true; // pre addAtom
      addBond();
      touchedAtom = natoms;
      nasv[nbonds] = TRIPLE;
      addBond();
      linearAdding = false; // pre addAtom
      nadded = 3;
    } else if (jme.action == JME.ACTION_GROUP_CYANO) {
      addBond();
      touchedAtom = natoms;
      linearAdding = true; // pre addAtom
      addBond();
      nasv[nbonds] = TRIPLE;
      an[natoms] = JME.AN_N;
      linearAdding = false; // pre addAtom
      nadded = 2;
    } else if (jme.action == JME.ACTION_GROUP_CF) {
      addBond();
      an[natoms] = JME.AN_F;
      nadded = 1;
    } else if (jme.action == JME.ACTION_GROUP_CL) {
      addBond();
      an[natoms] = JME.AN_CL;
      nadded = 1;
    } else if (jme.action == JME.ACTION_GROUP_CB) {
      addBond();
      an[natoms] = JME.AN_BR;
      nadded = 1;
    } else if (jme.action == JME.ACTION_GROUP_CI) {
      addBond();
      an[natoms] = JME.AN_I;
      nadded = 1;
    } else if (jme.action == JME.ACTION_GROUP_CN) {
      addBond();
      an[natoms] = JME.AN_N;
      nadded = 1;
    } else if (jme.action == JME.ACTION_GROUP_CO) {
      addBond();
      an[natoms] = JME.AN_O;
      nadded = 1;
    } else if (jme.action == JME.ACTION_GROUP_C2) {
      addBond();
      touchedAtom = natoms;
      addBond();
      nadded = 2;
    } else if (jme.action == JME.ACTION_GROUP_C3) {
      addBond();
      touchedAtom = natoms;
      addBond();
      touchedAtom = natoms;
      addBond();
      nadded = 3;
    } else if (jme.action == JME.ACTION_GROUP_C4) {
      addBond();
      touchedAtom = natoms;
      addBond();
      touchedAtom = natoms;
      addBond();
      touchedAtom = natoms;
      addBond();
      nadded = 4;
    } else if (jme.action == JME.ACTION_GROUP_TEMPLATE) {
      // 2008.1 adding defined tamplate
      addGroupTemplate(emptyCanvas);
      nadded = 4; // ????
    }

    avoidTouch(nadded); // 2009.2, predtym 4

    touchedAtom = touched_org;
    if (emptyCanvas)
      touchedAtom = 0;
  }

  // ----------------------------------------------------------------------------
  void addRing() {
    // adding ring atoms
    // (bonds are added in completeRing)
    int atom1, atom2, atom3, revert;
    double dx, dy, rx, sina, cosa, xx, yy;
    double diel, rc, uhol, xstart, ystart;
    int returnTouch = -1; // stopka pridavanie

    int nmembered = 6;
    switch (jme.action) {
    case JME.ACTION_RING_3:
      nmembered = 3;
      break;
    case JME.ACTION_RING_4:
      nmembered = 4;
      break;
    case JME.ACTION_RING_5:
    case JME.ACTION_RING_FURANE:
    case JME.ACTION_RING_3FURYL:
      nmembered = 5;
      break;
    case JME.ACTION_RING_6:
    case JME.ACTION_RING_PH:
      nmembered = 6;
      break;
    case JME.ACTION_RING_7:
      nmembered = 7;
      break;
    case JME.ACTION_RING_8:
      nmembered = 8;
      break;
    case JME.ACTION_RING_9:
      nmembered = 9;
      break;
    }

    diel = Math.PI * 2. / nmembered;
    rc = Math.sqrt(rbond() * rbond() / 2. / (1. - Math.cos(diel)));

    if (touchedAtom > 0) {
      // --- adding ring at the end of the bond
      if (nv[touchedAtom] < 2) {
        addRingToBond(nmembered, diel, rc);
      } else {
        if (!jme.mouseShift) {
          // adding bond and ring
          returnTouch = touchedAtom;
          addBond();
          touchedAtom = natoms;
          addRingToBond(nmembered, diel, rc);
        } else {
          // checking ci moze robit spiro
          if (jme.action == JME.ACTION_RING_PH
              || jme.action == JME.ACTION_RING_FURANE
              || jme.action == JME.ACTION_RING_3FURYL) {
            jme.info("ERROR - cannot add aromatic spiro ring !");
            jme.lastAction = JME.LA_FAILED; //aby nevolalo checkNewRing
            return;
          }
          for (int i = 1; i <= nv[touchedAtom]; i++) {
            int bo = nasv[bondIdentity(touchedAtom, v[touchedAtom][i])];
            if (i > 2 || bo != SINGLE) {
              jme.info("ERROR - spiro ring not possible here !");
              jme.lastAction = JME.LA_FAILED; //aby nevolalo checkNewRing
              return;
            }
          }
          // ---  adding spiro ring
          double[] newPoint = new double[2];
          addPoint(touchedAtom, rc, newPoint);
          dx = x[touchedAtom] - newPoint[0];
          dy = y[touchedAtom] - newPoint[1];
          rx = Math.sqrt(dx * dx + dy * dy);
          if (rx < 0.001)
            rx = 0.001;
          sina = dy / rx;
          cosa = dx / rx;
          for (int i = 1; i <= nmembered; i++) {
            createAtom();
            uhol = diel * i + Math.PI * .5;
            x[natoms] = newPoint[0]
                + rc * (Math.sin(uhol) * cosa - Math.cos(uhol) * sina);
            y[natoms] = newPoint[1]
                + rc * (Math.cos(uhol) * cosa + Math.sin(uhol) * sina);
          }
        }
      }
    }

    // fusing ring
    else if (touchedBond > 0) {
      atom1 = va[touchedBond];
      atom2 = vb[touchedBond];
      // hlada ref. atom atom3
      atom3 = 0;
      if (nv[atom1] == 2) {
        if (v[atom1][1] != atom2)
          atom3 = v[atom1][1];
        else
          atom3 = v[atom1][2];
      } else if (nv[atom2] == 2) {
        if (v[atom2][1] != atom1)
          atom3 = v[atom2][1];
        else
          atom3 = v[atom2][2];
        revert = atom1;
        atom1 = atom2;
        atom2 = revert; // atom3 on atom1
      }
      if (atom3 == 0) // no clear reference atom
        if (v[atom1][1] != atom2)
          atom3 = v[atom1][1];
        else
          atom3 = v[atom1][2];

      dx = x[atom2] - x[atom1];
      dy = y[atom2] - y[atom1];
      rx = Math.sqrt(dx * dx + dy * dy);
      if (rx < 0.001)
        rx = 0.001;
      sina = dy / rx;
      cosa = dx / rx;
      xx = rx / 2.;
      yy = rc * Math.sin((Math.PI - diel) * .5);
      revert = 1;
      if (((y[atom3] - y[atom1]) * cosa - (x[atom3] - x[atom1]) * sina) > 0.) {
        yy = -yy;
        revert = 0;
      }
      xstart = x[atom1] + xx * cosa - yy * sina;
      ystart = y[atom1] + yy * cosa + xx * sina;
      for (int i = 1; i <= nmembered; i++) {
        createAtom();
        uhol = diel * (i + .5) + Math.PI * revert;
        x[natoms] = xstart
            + rc * (Math.sin(uhol) * cosa - Math.cos(uhol) * sina);
        y[natoms] = ystart
            + rc * (Math.cos(uhol) * cosa + Math.sin(uhol) * sina);
        // next when fusing to the "long" bond
        if (revert == 1) {
          if (i == nmembered) {
            x[natoms] = x[atom1];
            y[natoms] = y[atom1];
          }
          if (i == nmembered - 1) {
            x[natoms] = x[atom2];
            y[natoms] = y[atom2];
          }
        } else {
          if (i == nmembered - 1) {
            x[natoms] = x[atom1];
            y[natoms] = y[atom1];
          }
          if (i == nmembered) {
            x[natoms] = x[atom2];
            y[natoms] = y[atom2];
          }
        }
      }
    }

    // new ring in free space
    else {
      double helpv = 0.5;
      if (nmembered == 6)
        helpv = 0.;
      for (int i = 1; i <= nmembered; i++) {
        createAtom();
        uhol = diel * (i - helpv);
        x[natoms] = xorg + rc * Math.sin(uhol);
        y[natoms] = yorg + rc * Math.cos(uhol);
      }
    }

    completeRing(nmembered);
    // a aby to bolo uz po mouse down OK
    checkRing(nmembered);
    // po check ring (inak nerobi avoid), pri stopke pridavani
    if (returnTouch > -1)
      touchedAtom = returnTouch;
  }

  // ----------------------------------------------------------------------------
  void addRingToBond(int nmembered, double diel, double rc) {
    double sina, cosa, dx, dy, rx, uhol;
    int atom1 = 0;
    if (nv[touchedAtom] == 0) {
      sina = 0.;
      cosa = 1.;
    } else {
      atom1 = v[touchedAtom][1];
      dx = x[touchedAtom] - x[atom1];
      dy = y[touchedAtom] - y[atom1];
      rx = Math.sqrt(dx * dx + dy * dy);
      if (rx < 0.001)
        rx = 0.001;
      sina = dy / rx;
      cosa = dx / rx;
    }
    double xstart = x[touchedAtom] + rc * cosa;
    double ystart = y[touchedAtom] + rc * sina;
    for (int i = 1; i <= nmembered; i++) {
      createAtom();
      uhol = diel * i - Math.PI * .5;
      x[natoms] = xstart + rc * (Math.sin(uhol) * cosa - Math.cos(uhol) * sina);
      y[natoms] = ystart + rc * (Math.cos(uhol) * cosa + Math.sin(uhol) * sina);
    }
  }

  // ----------------------------------------------------------------------------
  void completeRing(int nmembered) {
    // adding bonds between ring atoms

    int i, atom = 0, atom3;
    for (i = 1; i <= nmembered; i++) {
      createBond();
      nasv[nbonds] = 1;
      atom = natoms - nmembered + i;
      nv[atom] = 2;
      va[nbonds] = atom;
      vb[nbonds] = atom + 1;
    }
    vb[nbonds] = natoms - nmembered + 1;

    // alternating double bonds for phenyl and furane template
    // 2007.12 fixed problematic adding
    if (jme.action == JME.ACTION_RING_PH) {
      nasv[nbonds - 4] = DOUBLE;
      nasv[nbonds - 2] = DOUBLE;
      nasv[nbonds - 0] = DOUBLE;
      if (touchedBond > 0) {
        if (isSingle(touchedBond)) {
          // fancy stuff - fusing two phenyls by single bond
          atom3 = 0;
          if (nv[va[touchedBond]] > 1) {
            atom3 = v[va[touchedBond]][1];
            atom = va[touchedBond];
            if (atom3 == vb[touchedBond])
              atom3 = v[va[touchedBond]][2];
          }
          if (atom3 == 0 && nv[vb[touchedBond]] > 1) {
            atom3 = v[vb[touchedBond]][1];
            atom = vb[touchedBond];
            if (atom3 == vb[touchedBond])
              atom3 = v[vb[touchedBond]][2];
          }
          if (atom3 > 0)
            // checking if bond atom3-atom is multiple
            for (i = 1; i <= nbonds; i++)
              if ((va[i] == atom3 && vb[i] == atom)
                  || (va[i] == atom && vb[i] == atom3)) {
                if (!isSingle(i)) {
                  nasv[nbonds - 4] = SINGLE;
                  nasv[nbonds - 2] = SINGLE;
                  nasv[nbonds - 0] = SINGLE;
                  nasv[nbonds - 5] = DOUBLE;
                  nasv[nbonds - 3] = DOUBLE;
                  nasv[nbonds - 1] = TRIPLE;
                }
                break;
              }
        } else {
          nasv[nbonds - 4] = SINGLE;
          nasv[nbonds - 2] = SINGLE;
          nasv[nbonds - 0] = SINGLE;
          nasv[nbonds - 5] = DOUBLE;
          nasv[nbonds - 3] = DOUBLE;
          nasv[nbonds - 1] = DOUBLE;
        }
      }
    } else if (jme.action == JME.ACTION_RING_FURANE
        || jme.action == JME.ACTION_RING_3FURYL) {
      // fused pridava celkom inteligente (akurat ze jed dolava O je dolu)
      // treba to zmenit na hore ??? (asi nie)
      // zatial nefixuje C+ after fusing
      // 2008.11 fixed furane adding (opposite double bond)
      if (touchedBond > 0) {
        if (nasv[touchedBond] == SINGLE) {
          // nned to check whether it is not =C-C= bond
          boolean isConjugated = false;
          for (i = 1; i <= nv[va[touchedBond]]; i++) {
            int ax = v[va[touchedBond]][i];
            if (nasv[bondIdentity(va[touchedBond], ax)] > SINGLE) {
              isConjugated = true;
              break;
            }
          }
          for (i = 1; i <= nv[vb[touchedBond]]; i++) {
            int ax = v[vb[touchedBond]][i];
            if (nasv[bondIdentity(vb[touchedBond], ax)] > SINGLE) {
              isConjugated = true;
              break;
            }
          }
          if (!isConjugated)
            nasv[touchedBond] = DOUBLE;
        }
        nasv[nbonds - 4] = DOUBLE;
        an[natoms - 2] = JME.AN_O;
      } else if (touchedAtom > 0) {
        if (jme.action == JME.ACTION_RING_FURANE) {
          nasv[nbonds - 4] = SINGLE;
          nasv[nbonds - 2] = SINGLE;
          nasv[nbonds - 1] = SINGLE;
          nasv[nbonds - 3] = DOUBLE;
          nasv[nbonds - 0] = DOUBLE;
          an[natoms - 1] = JME.AN_O;
        } else {
          nasv[nbonds - 3] = SINGLE;
          nasv[nbonds - 2] = SINGLE;
          nasv[nbonds - 0] = SINGLE;
          nasv[nbonds - 4] = DOUBLE;
          nasv[nbonds - 1] = DOUBLE;
          an[natoms - 2] = JME.AN_O;
        }
      } else { // new furane ring
        nasv[nbonds - 3] = SINGLE;
        nasv[nbonds - 2] = SINGLE;
        nasv[nbonds - 0] = SINGLE;
        nasv[nbonds - 4] = DOUBLE;
        nasv[nbonds - 1] = DOUBLE;
        an[natoms - 2] = JME.AN_O;
      }
    }

  }

  // ----------------------------------------------------------------------------
  void checkRing(int nmembered) {
    // checks if newly created ring doesn't touch with some atoms
    // ak touchedAtom > 0 (pridavanie k stopke) robi avoid atoms
    // doplni v[][], xa[], xb[]
    // va[], vb[], nv[] bolo uz zmenene v add_raw_bond()

    int i, j, k, atom, atom1, atom2, ratom, rbond, noldbonds, noldatoms;
    int parent[] = new int[natoms + 1];
    double dx, dy, rx, min;

    for (i = 1; i <= nmembered; i++) {
      ratom = natoms - nmembered + i;
      rbond = nbonds - nmembered + i;
      v[ratom][1] = ratom - 1;
      v[ratom][2] = ratom + 1;
      atom1 = va[rbond];
      atom2 = vb[rbond];
      xb[rbond] = (int) Math.round((x[atom1] + x[atom2]) / 2.);
      yb[rbond] = (int) Math.round((y[atom1] + y[atom2]) / 2.);
    }
    v[natoms - nmembered + 1][1] = natoms;
    v[natoms][2] = natoms - nmembered + 1;

    // zistuje, ci sa nejake nove atomy dotykaju so starymi
    for (i = natoms - nmembered + 1; i <= natoms; i++) {
      parent[i] = 0;
      min = TOUCH_LIMIT + 1;
      atom = 0;
      for (j = 1; j <= natoms - nmembered; j++) {
        dx = x[i] - x[j];
        dy = y[i] - y[j];
        rx = dx * dx + dy * dy;
        if (rx < TOUCH_LIMIT)
          if (rx < min) {
            min = rx;
            atom = j;
          }
      }
      if (atom > 0) // dotyk noveho atomu i so starym atomom atom
        if (touchedAtom == 0 || atom == touchedAtom) // ked stopka len ten 1
          parent[i] = atom;
    }

    // robi nove vazby
    noldbonds = nbonds - nmembered;
    bloop: for (i = noldbonds + 1; i <= noldbonds + nmembered; i++) {
      atom1 = va[i];
      atom2 = vb[i];
      if (parent[atom1] > 0 && parent[atom2] > 0) {
        // ak parenty nie su viazane urobi medzi nimi novu vazbu
        for (k = 1; k <= noldbonds; k++) {
          if ((va[k] == parent[atom1] && vb[k] == parent[atom2])
              || (vb[k] == parent[atom1] && va[k] == parent[atom2]))
            continue bloop;
        }
        createBond();
        nasv[nbonds] = nasv[i];
        va[nbonds] = parent[atom1];
        v[parent[atom1]][++nv[parent[atom1]]] = parent[atom2];
        vb[nbonds] = parent[atom2];
        v[parent[atom2]][++nv[parent[atom2]]] = parent[atom1];
        xb[nbonds] = (int) Math.round((x[va[nbonds]] + x[vb[nbonds]]) / 2.);
        yb[nbonds] = (int) Math.round((y[va[nbonds]] + y[vb[nbonds]]) / 2.);
      } else if (parent[atom1] > 0) {
        createBond();
        nasv[nbonds] = nasv[i];
        va[nbonds] = parent[atom1];
        v[parent[atom1]][++nv[parent[atom1]]] = atom2;
        vb[nbonds] = atom2;
        v[atom2][++nv[atom2]] = parent[atom1];
        xb[nbonds] = (int) Math.round((x[va[nbonds]] + x[vb[nbonds]]) / 2.);
        yb[nbonds] = (int) Math.round((y[va[nbonds]] + y[vb[nbonds]]) / 2.);
      } else if (parent[atom2] > 0) {
        createBond();
        nasv[nbonds] = nasv[i];
        va[nbonds] = parent[atom2];
        v[parent[atom2]][++nv[parent[atom2]]] = atom1;
        vb[nbonds] = atom1;
        v[atom1][++nv[atom1]] = parent[atom2];
        xb[nbonds] = (int) Math.round((x[va[nbonds]] + x[vb[nbonds]]) / 2.);
        yb[nbonds] = (int) Math.round((y[va[nbonds]] + y[vb[nbonds]]) / 2.);
      }
    }

    // nakoniec vyhodi atomy, co maju parentov
    noldatoms = natoms - nmembered;
    for (i = natoms; i > noldatoms; i--) {
      if (parent[i] > 0) {
        deleteAtom(i);
        // 2007.12 checking 5-nasobnost u C
        if (an[parent[i]] == JME.AN_C) {
          int sum = 0;
          for (j = 1; j <= nv[parent[i]]; j++) {
            int a2 = v[parent[i]][j];
            for (k = 1; k <= nbonds; k++) {
              if ((va[k] == parent[i] && vb[k] == a2)
                  || (va[k] == a2 && vb[k] == parent[i]))
                sum += nasv[k];
            }
          }
          if (sum > 4) {
            // zmeni nove vazby na single
            // more intelligent and keep double bond ???
            for (k = noldbonds + 1; k <= noldbonds + nmembered; k++)
              nasv[k] = SINGLE;
          }
        }
      }

    }

    // if stopka avoid
    if (touchedAtom > 0)
      avoidTouch(nmembered);

  }

  // ----------------------------------------------------------------------------
  private void addPoint(int touchedAtom, double rbond, double[] newPoint) {
    // adding new atom to source with two bonds already
    // called when creating new bond or ring center
    double dx, dy, rx, sina, cosa, xx, yy, xpoint, ypoint;
    int atom1 = v[touchedAtom][1];
    int atom2 = v[touchedAtom][2];
    dx = x[atom2] - x[atom1];
    dy = -(y[atom2] - y[atom1]);
    rx = Math.sqrt(dx * dx + dy * dy);
    if (rx < 0.001)
      rx = 0.001;
    sina = dy / rx;
    cosa = dx / rx;
    // vzd. act_a od priamky atom1-atom2
    double vzd = Math.abs((y[touchedAtom] - y[atom1]) * cosa
        + (x[touchedAtom] - x[atom1]) * sina);
    if (vzd < 1.0) { // perpendicular to linear moiety
      dx = x[touchedAtom] - x[atom1];
      dy = y[touchedAtom] - y[atom1];
      rx = Math.sqrt(dx * dx + dy * dy);
      if (rx < 0.001)
        rx = 0.001;
      xx = rx;
      yy = rbond;
      sina = dy / rx;
      cosa = dx / rx;
      newPoint[0] = x[atom1] + xx * cosa - yy * sina;
      newPoint[1] = y[atom1] + yy * cosa + xx * sina;
    } else { // da do stredu tych 2 vazieb a oproti nim
      xpoint = (x[atom1] + x[atom2]) / 2.;
      ypoint = (y[atom1] + y[atom2]) / 2.;
      dx = x[touchedAtom] - xpoint;
      dy = y[touchedAtom] - ypoint;
      rx = Math.sqrt(dx * dx + dy * dy);
      if (rx < 0.001)
        rx = 0.001;
      newPoint[0] = x[touchedAtom] + rbond * dx / rx;
      newPoint[1] = y[touchedAtom] + rbond * dy / rx;
    }
  }

  // ----------------------------------------------------------------------------
  // adding template store in jme.tmol to clicked atom
  // connection by atom marked by :1
  // emptyCanvas indicates that (artificial) touchedAtom should be deleted
  void addGroupTemplate(boolean emptyCanvas) {
    // finding mark:1 in template
    // qw
    int mark1 = 0;
    JMEmol tmol = jme.tmol;
    for (int k = 1; k <= tmol.nmarked; k++) {
      int atom = tmol.mark[k][0];
      if (tmol.mark[k][1] == 1)
        mark1 = atom;
    }
    int nn = natoms;

    // getting dummy point in original molecule
    int source = touchedAtom;
    addBond();
    double x1 = x[natoms];
    double y1 = y[natoms];
    deleteAtom(natoms); // deletes also bond
    double dx1 = x[source] - x1;
    double dy1 = y[source] - y1;
    double r = Math.sqrt(dx1 * dx1 + dy1 * dy1);
    double sina = dy1 / r;
    double cosa = dx1 / r;

    for (int i = 1; i <= tmol.natoms; i++) {
      createAtom();
      an[natoms] = tmol.an[i];
      q[natoms] = tmol.q[i];
      nh[natoms] = tmol.nh[i];
      x[natoms] = tmol.x[i];
      y[natoms] = tmol.y[i];
    }
    for (int i = 1; i <= tmol.nbonds; i++) {
      createBond();
      va[nbonds] = tmol.va[i] + nn;
      vb[nbonds] = tmol.vb[i] + nn;
      nasv[nbonds] = tmol.nasv[i];
    }

    // adding dummy to the template
    complete();
    touchedAtom = mark1 + nn;
    addBond();
    double x2 = x[natoms];
    double y2 = y[natoms];
    deleteAtom(natoms);
    double dx2 = x[mark1 + nn] - x2;
    double dy2 = y[mark1 + nn] - y2;
    r = Math.sqrt(dx2 * dx2 + dy2 * dy2);
    double sinb = dy2 / r;
    double cosb = dx2 / r;
    for (int i = nn + 1; i <= natoms; i++) {
      // move point2 to zero
      x[i] -= x2;
      y[i] -= y2;
      // rotation to be parallel with x axis
      double xx = x[i] * cosb + y[i] * sinb;
      double yy = y[i] * cosb - x[i] * sinb;
      x[i] = xx;
      y[i] = yy;
      // rotating parallel with connecting bond
      // -cosa - opposite direction
      xx = -x[i] * cosa + y[i] * sina;
      yy = -y[i] * cosa - x[i] * sina;
      x[i] = xx;
      y[i] = yy;
      // move towards point1
      x[i] += x[source];
      y[i] += y[source];
    }

    // adding connecting bond
    createBond();
    va[nbonds] = source;
    vb[nbonds] = mark1 + nn;
    complete();

    //for (int i=1;i<=natoms;i++)
    //  System.out.println(i+" "+an[i]);
    //for (int i=1;i<=nbonds;i++)
    //  System.out.println(i+" "+va[i]+" "+vb[i]+" "+nasv[i]);

    if (emptyCanvas) {
      deleteAtom(source);
      center();
    }
  }

  // ----------------------------------------------------------------------------
  void createAtom() {
    // creating new atom with AN_C
    // allocating memory if necessary
    natoms++;
    if (natoms > an.length - 1) {
      int storage = an.length + 10;
      int n_an[] = new int[storage];
      System.arraycopy(an, 0, n_an, 0, an.length);
      an = n_an;
      int n_q[] = new int[storage];
      System.arraycopy(q, 0, n_q, 0, q.length);
      q = n_q;

      int n_nh[] = new int[storage];
      System.arraycopy(nh, 0, n_nh, 0, nh.length);
      nh = n_nh;

      int n_abg[] = new int[storage];
      System.arraycopy(abg, 0, n_abg, 0, abg.length);
      abg = n_abg;

      String n_atag[] = new String[storage];
      System.arraycopy(atag, 0, n_atag, 0, atag.length);
      atag = n_atag;

      String n_label[] = new String[storage];
      System.arraycopy(label, 0, n_label, 0, label.length);
      label = n_label;

      double n_x[] = new double[storage];
      System.arraycopy(x, 0, n_x, 0, x.length);
      x = n_x;
      double n_y[] = new double[storage];
      System.arraycopy(y, 0, n_y, 0, y.length);
      y = n_y;
      int n_v[][] = new int[storage][MAX_BONDS_ON_ATOM + 1];
      System.arraycopy(v, 0, n_v, 0, v.length);
      v = n_v;
      int n_nv[] = new int[storage];
      System.arraycopy(nv, 0, n_nv, 0, nv.length);
      nv = n_nv;

    }
    an[natoms] = JME.AN_C;
    q[natoms] = 0;
    abg[natoms] = 0;
    atag[natoms] = null;
    nh[natoms] = 0;
  }

  // ----------------------------------------------------------------------------
  void createAtom(String symbol) {
    // parses SMILES-like atomic label and set atom parameters
    createAtom(); // sets natoms
    setAtom(natoms, symbol);
  }

  // ----------------------------------------------------------------------------
  void setAtom(int atom, String symbol) {
    // volane pri spracovavani mol alebo jme z createAtom
    // alebo pri kliknuti na X atom x x boxu
    // aj po query

    // if in [] forces this valence state as AN_X, 2004.01
    if (symbol.startsWith("[") && symbol.endsWith("]")) {
      symbol = symbol.substring(1, symbol.length() - 1);
      an[atom] = JME.AN_X;
      label[atom] = symbol;
      nh[atom] = 0;
      return;
    }

    if (symbol.length() < 1)
      System.err.println("Error - null atom !");

    // ak je tam , alebo ; ide o query aj ked zacina so znamym symbolom
    boolean isQuery = false;
    if (symbol.indexOf(",") > -1)
      isQuery = true;
    if (symbol.indexOf(";") > -1)
      isQuery = true;
    if (symbol.indexOf("#") > -1)
      isQuery = true;
    if (symbol.indexOf("!") > -1)
      isQuery = true;
    int dpos = symbol.indexOf(":"); // marking 
    int hpos = symbol.indexOf("H");
    int qpos = Math.max(symbol.indexOf("+"), symbol.indexOf("-"));

    // spracuje label a odsekne ju
    if (dpos > -1) {
      String smark = symbol.substring(dpos + 1);
      // fixed in 2010.01
      //maxMark = Integer.valueOf(smark).intValue() - 1; // v mark() je ++
      //makos
      try {
        jme.currentMark = Integer.parseInt(smark);
      } catch (Exception e) {
        jme.currentMark = 0;
      }
      touchedAtom = atom; // kvoli mark()
      mark();
      // odsekne z konca :label
      symbol = symbol.substring(0, dpos);
      touchedAtom = 0;
    }

    atomProcessing: {
      if (isQuery) {
        label[atom] = symbol;
        an[atom] = JME.AN_X;
        nh[atom] = 0;
        break atomProcessing;
      }

      // skusa, ci to je standard atom
      String as = symbol;

      // testuje > 0 nie > -1 (aby pokrylo H a zaciatok atomu s + -
      if (hpos > 0)
        as = symbol.substring(0, hpos);
      else if (qpos > 0)
        as = symbol.substring(0, qpos);

      an[atom] = checkAtomicSymbol(as); // as & symbol su rozdielne/
      if (an[atom] == JME.AN_X)
        label[atom] = as;

      symbol += " "; // aby netrebalo stale checkovat koniec

      // number of hydrogens (moze but aj H0)
      int nhs = 0;
      if (hpos > 0) { // > 0, nie -1
        nhs = 1;
        char c = symbol.charAt(++hpos);
        if (c >= '0' && c <= '9')
          nhs = c - '0';
      }
      if (an[atom] == JME.AN_X) {
        nh[atom] = nhs;
      }
      // co ostatne atomy ??? force ich

      // charge
      int charge = 0;
      if (qpos > 0) {
        char c = symbol.charAt(qpos++);
        if (c == '+')
          charge = 1;
        else if (c == '-')
          charge = -1;
        if (charge != 0) {
          c = symbol.charAt(qpos++);
          if (c >= '0' && c <= '9')
            c *= c - '0';
          else {
            while (c == '+') {
              charge++;
              c = symbol.charAt(qpos++);
            }
            while (c == '-') {
              charge--;
              c = symbol.charAt(qpos++);
            }
          }
        }
      }
      q[atom] = charge;
    }
  }

  // ----------------------------------------------------------------------------
  void setAtomHydrogenCount(int atom, int nh) {
    // upravuje to len pre X atomy !
    if (an[atom] == JME.AN_X) {
      label[atom] += "H";
      if (nh > 1)
        label[atom] += nh;
    }
  }

  // ----------------------------------------------------------------------------
  void setAtomFormalCharge(int atom, int nq) {
    q[atom] = nq; // setne aj pre X atomy
    // musi byt volane po setAtomHydrogenCount
    /*
    if (an[atom] == JME.AN_X) {
      if (nq > 0) label[atom] += "+"; else label[atom] += "-";
      if (Math.abs(nq) > 1) label[atom] += Math.abs(nq);
    }
    */
  }

  // ----------------------------------------------------------------------------
  void setAtomColors(String s, boolean bg) {
    doColoring = 1;
    if (bg)
      doColoring = -1; // whether atoms or background

    StringTokenizer st = new StringTokenizer(s, ",");
    int atom, color;
    try {
      while (st.hasMoreTokens()) {
        atom = Integer.valueOf(st.nextToken()).intValue();
        color = Integer.valueOf(st.nextToken()).intValue();
        setAtomColoring(atom, color);
      }
    } catch (Exception e) {
      System.err.println("Error in atom coloring");
      e.printStackTrace();
    }
  }

  // ----------------------------------------------------------------------------
  public void setAtomColoring(int atom, int n) {
    if (n < 0 || n > 6)
      n = 0; // default color gray 
    abg[atom] = n;
  }

  // ----------------------------------------------------------------------------
  void createBond() {
    // creates new bonds (standard SINGLE)
    // allocating memory if necessary
    nbonds++;
    if (nbonds > nasv.length - 1) {
      int storage = nasv.length + 10;
      int n_va[] = new int[storage];
      System.arraycopy(va, 0, n_va, 0, va.length);
      va = n_va;
      int n_vb[] = new int[storage];
      System.arraycopy(vb, 0, n_vb, 0, vb.length);
      vb = n_vb;
      int n_nasv[] = new int[storage];
      System.arraycopy(nasv, 0, n_nasv, 0, nasv.length);
      nasv = n_nasv;
      int n_stereob[] = new int[storage];
      System.arraycopy(stereob, 0, n_stereob, 0, stereob.length);
      stereob = n_stereob;
      int n_xb[] = new int[storage];
      System.arraycopy(xb, 0, n_xb, 0, xb.length);
      xb = n_xb;
      int n_yb[] = new int[storage];
      System.arraycopy(yb, 0, n_yb, 0, yb.length);
      yb = n_yb;

      String n_btag[] = new String[storage];
      System.arraycopy(btag, 0, n_btag, 0, btag.length);
      btag = n_btag;
    }
    nasv[nbonds] = SINGLE;
    stereob[nbonds] = 0;
    btag[nbonds] = null;
  }

  // ----------------------------------------------------------------------------
  void findBondCenters() {
    for (int i = 1; i <= nbonds; i++) {
      int atom1 = va[i];
      int atom2 = vb[i];
      xb[i] = (int) Math.round((x[atom1] + x[atom2]) / 2.);
      yb[i] = (int) Math.round((y[atom1] + y[atom2]) / 2.);
    }
  }

  // ----------------------------------------------------------------------------
  void save() {
    // saving current molecule 
    // ??? only after serious changes (not after changing atom or bond type)
    jme.smol = new JMEmol(this);
    jme.smol.complete();
    jme.saved = jme.actualMolecule;
  }

  // ----------------------------------------------------------------------------
  boolean isRotatableBond(int a1, int a2) {
    // function tests if bond between atoms a1 and a2 is rotatable
    // (i.e. returns false if bonds is in a cycle, or atoms a1 and a2
    // are not bonded)
    // atoms on the a1-side (including a1) will have set flag a[] > 0

    int i, j, poradie = 1;
    a = new int[natoms + 1];
    for (i = 1; i <= natoms; i++)
      a[i] = 0; //treba to, alebo automatic pri new ???
    a[a1] = poradie;
    for (i = 1; i <= nv[a1]; i++)
      if (v[a1][i] != a2)
        a[v[a1][i]] = ++poradie;

    boolean ok = false;
    while (true) {
      for (i = 1; i <= natoms; i++) {
        ok = false;
        if (a[i] > 0 && i != a1)
          for (j = 1; j <= nv[i]; j++) {
            if (a[v[i][j]] == 0) {
              a[v[i][j]] = ++poradie;
              ok = true;
            }
          }
        if (ok)
          break;
      }
      if (!ok)
        break;
    }

    return (a[a2] == 0);
  }

  // ----------------------------------------------------------------------------
  void findRingBonds(boolean isRingBond[]) {
    // modifikuje a[]
    for (int i = 1; i <= nbonds; i++)
      if (isRotatableBond(va[i], vb[i]))
        isRingBond[i] = false;
      else
        isRingBond[i] = true;
  }

  // ----------------------------------------------------------------------------
  boolean isInRing(int atom, boolean isRingBond[]) {
    for (int i = 1; i <= nv[atom]; i++) {
      if (isRingBond[bondIdentity(atom, v[atom][i])])
        return true;
    }
    return false;
  }

  // ----------------------------------------------------------------------------
  void findAromatic(boolean isAromatic[], boolean isRingBond[]) {
    // two pass

    btype = new int[nbonds + 1];
    boolean pa[] = new boolean[natoms + 1]; // possible aromatic

    for (int i = 1; i <= natoms; i++) {
      pa[i] = false;
      isAromatic[i] = false;
      if (!isInRing(i, isRingBond))
        continue;
      if (nv[i] + nh[i] > 3)
        continue; // >X< nemoze byt aromaticky (ako s nabojmi?)
      switch (an[i]) {
      case JME.AN_C:
      case JME.AN_N:
      case JME.AN_P:
      case JME.AN_O:
      case JME.AN_S:
      case JME.AN_SE:
        pa[i] = true;
        break;
      case JME.AN_X:
        pa[i] = true; // malo by zistovat, ci je naozaj mozne
        break;
      }

    }

    // 2. prechod, ide po ring vazbach a cekuje, zaroven plni aj btype[]
    // ignoruje stereo !!!
    for (int b = 1; b <= nbonds; b++) {
      if (isSingle(b))
        btype[b] = SINGLE;
      else if (isDouble(b))
        btype[b] = DOUBLE;
      else if (nasv[b] == TRIPLE)
        btype[b] = TRIPLE;
      else
        System.err.println("problems in findAromatic " + nasv[b]);
    }
    bondloop: for (int b = 1; b <= nbonds; b++) {
      if (!isRingBond[b])
        continue;
      int atom1 = va[b];
      int atom2 = vb[b];
      if (!pa[atom1] || !pa[atom2])
        continue;

      // loop cez molekulu len po pa[] atomoch
      boolean a[] = new boolean[natoms + 1]; // plni na false
      for (int i = 1; i <= nv[atom1]; i++) {
        int atom = v[atom1][i];
        if (atom != atom2 && pa[atom])
          a[atom] = true;
      }

      boolean ok = false;
      while (true) {
        for (int i = 1; i <= natoms; i++) {
          ok = false;
          if (a[i] && pa[i] && i != atom1) {
            for (int j = 1; j <= nv[i]; j++) {
              int atom = v[i][j];
              if (atom == atom2) { // bond b je v aromatickom kruhu
                isAromatic[atom1] = true;
                isAromatic[atom2] = true;
                btype[b] = AROMATIC;
                continue bondloop;
              }
              if (!a[atom] && pa[atom]) {
                a[atom] = true;
                ok = true;
              }
            }
          }
          if (ok)
            break;
        }
        if (!ok)
          break;
      }

    } // --- bondloop

  }

  // ----------------------------------------------------------------------------
  void canonize() {
    // #1 atom will be simplest
    boolean ok;
    int a[] = new int[natoms + 1]; // current ranking
    int aold[] = new int[natoms + 1];
    long d[] = new long[natoms + 1];
    // primes
    long prime[] = new long[natoms + 2]; //+2 primes return minimum 2 values
    prime = generatePrimes(natoms);

    // seeds
    for (int i = 1; i <= natoms; i++) {
      int xbo = 1; // product of bond orders
      for (int j = 1; j <= nbonds; j++) { // efektivnejsie ako s bond identity
        if (va[j] == i || vb[j] == i)
          xbo *= btype[j]; // 1,2,3 alebo 5 (AROMATIC)
      }
      int xan = an[i];
      if (xan == JME.AN_X) {
        String zlabel = label[i];
        // nekanonizuje query, ale to nevadi
        int c1 = zlabel.charAt(0) - 'A' + 1; // +1 aby sa nekrylo z h.AN
        int c2 = 0;
        if (zlabel.length() > 1)
          c2 = zlabel.charAt(1) - 'a';
        if (c1 < 0)
          c1 = 0;
        if (c2 < 0)
          c2 = 0; // pre qry - zostava visiet pri #
        xan = c1 * 28 + c2;
      }
      int qq = 0;
      if (q[i] < -2)
        qq = 1;
      else if (q[i] == -2)
        qq = 2;
      else if (q[i] == -1)
        qq = 3;
      else if (q[i] == 1)
        qq = 4;
      else if (q[i] == 2)
        qq = 5;
      else if (q[i] > 2)
        qq = 6;

      // (x musi byt maximum+1)
      int xx = 1;
      d[i] = xbo;
      xx *= 126;
      d[i] += nh[i] * xx;
      xx *= 7;
      d[i] += qq * xx;
      xx *= 7;
      d[i] += xan * xx;
      xx *= 783; // 27*28+26+1
      d[i] += nv[i] * xx;
    }

    int breaklevel = 0;
    while (true) {
      // sorting
      if (canonsort(a, d))
        break;
      // comparing with aold[]
      ok = false;
      for (int i = 1; i <= natoms; i++)
        if (a[i] != aold[i]) {
          aold[i] = a[i];
          ok = true;
        }

      if (ok) { // cize ci sa pohlo dopredu
        for (int i = 1; i <= natoms; i++) {
          d[i] = 1;
          for (int j = 1; j <= nv[i]; j++)
            d[i] *= prime[a[v[i][j]]];
        }
        breaklevel = 0;
      } else { // musi break degeneraciu
        if (breaklevel > 0) { // just random breaking
          for (int i = 1; i <= natoms; i++)
            d[i] = 1;
          bd: for (int i = 1; i <= natoms - 1; i++)
            for (int j = i + 1; j <= natoms; j++)
              if (a[i] == a[j]) {
                d[i] = 2;
                break bd;
              }
        } else { // skusa inteligente
          for (int i = 1; i <= natoms; i++) {
            d[i] = 1;
            // co E,Z,stereovazby, je to OK ???
            for (int j = 1; j <= nv[i]; j++) {
              int atom = v[i][j];
              d[i] *= an[atom] * btype[bondIdentity(i, atom)];
            }
          }
          breaklevel = 1;
        }
      }
      canonsort(a, d);
      for (int i = 1; i <= natoms; i++)
        d[i] = aold[i] * natoms + a[i];
    }

    // reordering atoms podla a[]
    for (int i = 1; i <= natoms; i++)
      aold[i] = a[i];
    // [0] used as a swap space
    for (int s = 1; s <= natoms; s++) {
      for (int i = 1; i <= natoms; i++) {
        if (aold[i] == s) { // changes s and i
          // changes s and i
          an[0] = an[i];
          q[0] = q[i];
          x[0] = x[i];
          y[0] = y[i];
          nv[0] = nv[i];
          an[i] = an[s];
          q[i] = q[s];
          x[i] = x[s];
          y[i] = y[s];
          nv[i] = nv[s];
          an[s] = an[0];
          q[s] = q[0];
          x[s] = x[0];
          y[s] = y[0];
          nv[s] = nv[0];
          aold[i] = aold[s];
          aold[s] = s;
          label[0] = label[i];
          label[i] = label[s];
          label[s] = label[0];
          abg[0] = abg[i];
          abg[i] = abg[s];
          abg[s] = abg[0];
          atag[0] = atag[i];
          atag[i] = atag[s];
          atag[s] = atag[0];
          nh[0] = nh[i];
          nh[i] = nh[s];
          nh[s] = nh[0];
          break;
        }
      }
    }

    // marked atoms
    for (int i = 1; i <= nmarked; i++)
      mark[i][0] = a[mark[i][0]]; //mark[][1] zostane
    //System.out.println("mrk1 "+mark[1][0]+" "+mark[2][0]+" "+mark[3][0]);

    // canonization of bonds (pozor na stereo !)
    for (int i = 1; i <= nbonds; i++) {
      va[i] = a[va[i]];
      vb[i] = a[vb[i]];
      if (va[i] > vb[i]) {
        int du = va[i];
        va[i] = vb[i];
        vb[i] = du;
        if (stereob[i] == UP)
          stereob[i] = XUP;
        else if (stereob[i] == DOWN)
          stereob[i] = XDOWN;
        else if (stereob[i] == XUP)
          stereob[i] = UP;
        else if (stereob[i] == XDOWN)
          stereob[i] = DOWN;
      }
    }

    // sorting bonds according to va & vb
    // ez ????
    for (int i = 1; i < nbonds; i++) {
      int minva = natoms;
      int minvb = natoms;
      int b = 0;
      for (int j = i; j <= nbonds; j++) {
        if (va[j] < minva) {
          minva = va[j];
          minvb = vb[j];
          b = j;
        } else if (va[j] == minva && vb[j] < minvb) {
          minvb = vb[j];
          b = j;
        }
      }
      // changes i-th and b-th bond
      int du;
      du = va[i];
      va[i] = va[b];
      va[b] = du;
      du = vb[i];
      vb[i] = vb[b];
      vb[b] = du;
      du = nasv[i];
      nasv[i] = nasv[b];
      nasv[b] = du;
      du = stereob[i];
      stereob[i] = stereob[b];
      stereob[b] = du;
      String ds = btag[i];
      btag[i] = btag[b];
      btag[b] = ds;
    }
    // btype sa znici, ale neskor v createSmiles sa znovu vypocita

    //fillFields();
    //findBondCenters();
    complete();

  }

  // ----------------------------------------------------------------------------
  boolean canonsort(int a[], long d[]) {
    // v d[] su podla coho sa triedi, vysledok do a[] (1 1 1 2 3 3 ...)
    // pozor ! d[] nesmie byt 0
    // returns true ak nth = natoms (cize ziadna degeneracia)
    long min = 0;
    int nth = 0;
    int ndone = 0;
    while (true) {
      nth++;
      for (int i = 1; i <= natoms; i++)
        if (d[i] > 0) {
          min = d[i];
          break;
        }
      for (int i = 1; i <= natoms; i++)
        if (d[i] > 0 && d[i] < min)
          min = d[i];
      for (int i = 1; i <= natoms; i++)
        if (d[i] == min) {
          a[i] = nth;
          d[i] = 0;
          ndone++;
        }
      if (ndone == natoms)
        break;
    }
    return (nth == natoms);
  }

  // ----------------------------------------------------------------------------
  void cleanPolarBonds() {
    // changing [X+]-[Y-] into X=Y (such as non-symmetric nitro bonds)
    // changing [X+]=[Y+] into X-Y (such as C+=C+ after fusing )
    // key polarnitro added since version 2002.05
    for (int i = 1; i <= nbonds; i++) {
      int atom1 = va[i];
      int atom2 = vb[i];

      if ((q[atom1] == 1 && q[atom2] == -1)
          || (q[atom1] == -1 && q[atom2] == 1)) {
        if (nasv[i] == SINGLE || nasv[i] == DOUBLE) { // tu nie E,Z

          // exceptions
          // not doing this by polarnitro set (since 2002.05)
          if (an[atom1] != JME.AN_C && an[atom2] != JME.AN_C && jme.polarnitro)
            continue;
          // moved here 2011.10
          if (an[atom1] == JME.AN_H || an[atom2] == JME.AN_H)
            continue;
          if (an[atom1] == JME.AN_B || an[atom2] == JME.AN_B)
            continue;
          /*
          // not [H+]-[B-] in boranes (2005.02)
          if (an[atom1] == JME.AN_H || an[atom2] == JME.AN_H) continue; // 
          // not [N+] [B-] 2011.10
          if ((an[atom1]==JME.AN_B && an[atom2]==JME_AN.N) || (an[atom1]==JME.AN_N && an[atom2]==JME.AN_B)) continue;
          */

          // not between halogenes
          if (an[atom1] == JME.AN_F || an[atom1] == JME.AN_CL
              || an[atom1] == JME.AN_BR || an[atom1] == JME.AN_I
              || an[atom2] == JME.AN_F || an[atom2] == JME.AN_CL
              || an[atom2] == JME.AN_BR || an[atom2] == JME.AN_I)
            continue; // 2005.10

          //System.err.println("CPB1  "+atom1+" "+atom2);
          q[atom1] = 0;
          q[atom2] = 0;
          nasv[i]++;
          valenceState();
        }
      }

      // ??? nie, aspon 1 z nich musi byt C, (inak >N+=N+< meni na >NH+-NH+<) 
      //if (q[atom1]==1 && q[atom2]==1 && (an[atom1]==h.C || an[atom2]==h.C)) {
      if (q[atom1] == 1 && q[atom2] == 1) {
        if (nasv[i] == DOUBLE)
          nasv[i] = SINGLE;
        else if (nasv[i] == TRIPLE)
          nasv[i] = DOUBLE;
        //System.err.println("CPB2");
        valenceState();
      }

      // this fixes rare WebME problems (Jun 09)
      // how this affects normal JME editing ?
      if (nasv[i] == 4)
        nasv[i] = 1;
    }
  }

  // ----------------------------------------------------------------------------
  void fillFields() {
    // fills helper fields v[][], nv[], ??? vzdy allocates memory

    int storage = an.length; // lebo pridava memory po skokoch
    v = new int[storage][MAX_BONDS_ON_ATOM + 1];
    nv = new int[storage];

    for (int i = 1; i <= natoms; i++)
      nv[i] = 0;
    for (int i = 1; i <= nbonds; i++) {
      if (nv[va[i]] < MAX_BONDS_ON_ATOM) // 2002.08 predtym <=
        v[va[i]][++nv[va[i]]] = vb[i];
      if (nv[vb[i]] < MAX_BONDS_ON_ATOM)
        v[vb[i]][++nv[vb[i]]] = va[i];
    }
  }

  // ----------------------------------------------------------------------------
  int checkMultipart(boolean removeSmall) {
    // group prislusnost da do a[]
    int nparts = 0;
    boolean ok = false;
    a = new int[natoms + 1];

    while (true) {
      for (int j = 1; j <= natoms; j++)
        if (a[j] == 0) {
          a[j] = ++nparts;
          ok = true;
          break;
        }
      if (!ok)
        break;
      while (ok) {
        ok = false;
        for (int j = 1; j <= nbonds; j++) {
          int atom1 = va[j];
          int atom2 = vb[j];
          if (a[atom1] > 0 && a[atom2] == 0) {
            a[atom2] = nparts;
            ok = true;
          } else if (a[atom2] > 0 && a[atom1] == 0) {
            a[atom1] = nparts;
            ok = true;
          }
        }
      }
    }
    if (nparts < 2 || !removeSmall)
      return nparts;

    // najde najvacsiu
    int size[] = new int[nparts + 1];
    for (int i = 1; i <= natoms; i++)
      size[a[i]]++;
    int max = 0, largest = 1;
    for (int i = 1; i <= nparts; i++)
      if (size[i] > max) {
        max = size[i];
        largest = i;
      }
    // removing smaller part(s)
    for (int i = natoms; i >= 1; i--)
      if (a[i] != largest)
        deleteAtom(i);

    center(); // aby sa nedostalo do trap za okraj
    jme.info("Smaller part(s) removed !");
    return 1;
  }

  // ----------------------------------------------------------------------------
  String createSmiles() {
    int[] con1 = new int[natoms + 10]; // well a little bit too much memory
    int[] con2 = new int[natoms + 10]; // but the code is much cleaner than Vector
    // v niektorych exotoch je naozaj viac con ako atomov
    int[] branch = new int[natoms + 1];
    int[] candidate = new int[MAX_BONDS_ON_ATOM + 1];
    int[] parent = new int[natoms + 1];
    boolean[] isAromatic = new boolean[natoms + 1];
    boolean[] isRingBond = new boolean[nbonds + 1];
//    boolean leftBranch[] = new boolean[natoms + 1];
    int nconnections = 0;

    if (natoms == 0)
      return "";
    checkMultipart(true);

    boolean noQueryBonds = true;
    for (int b = 1; b <= nbonds; b++) {
      if (nasv[b] == QUERY) {
        noQueryBonds = false;
        break;
      }
    }
    // asi to treba takto komplikovane
    // btype RING_NONAROMATIC sa nepouziva ! (len aromatic)
    if (jme.canonize && noQueryBonds) {
      deleteHydrogens();
      cleanPolarBonds();
      findRingBonds(isRingBond);
      findAromatic(isAromatic, isRingBond); // naplni btype
      canonize(); // btype[] sa tu znici
      valenceState();
      // prec
      findRingBonds(isRingBond); // v canonize sa to prehadze, dat to tam ?
      findAromatic(isAromatic, isRingBond); // znovy vypoicta btype[]
    } else { // to treba pre stereochemiu
      findRingBonds(isRingBond);
      btype = new int[nbonds + 1]; // inak to plni vo findAromatic
      for (int i = 1; i <= nbonds; i++)
        btype[i] = nasv[i];
    }

    int atom = 1; // zacina sa najjednoduchsim
    a = new int[natoms + 1]; // defined globally, fills with 0

    // recursive marching through molecule, path stored int the field a[]
    int step = 1;
    a[atom] = step;
    int nbranch = 0;
    while (true) {
      int ncandidates = 0;
      for (int i = 1; i <= nv[atom]; i++) {
        int atomx = v[atom][i];
        if (a[atomx] > 0) { // hlada ring closure
          if (a[atomx] > a[atom])
            continue;
          if (atomx == parent[atom])
            continue;
          // naslo ring connection (musi ceknut ci uz to nie je zname)
          boolean newcon = true;
          for (int k = 1; k <= nconnections; k++)
            if (con1[k] == atom && con2[k] == atomx
                || con1[k] == atomx && con2[k] == atom) {
              newcon = false;
              break;
            }
          if (newcon) {
            nconnections++;
            con1[nconnections] = atom;
            con2[nconnections] = atomx;
          }
        } else
          candidate[++ncandidates] = atomx;
      }
      // teraz vetvenie podla poctu este nespracovanych susedov
      if (ncandidates == 0) {
        // nenaslo musi sa vratit o 1 branch nizsie (alebo ukoncit)
        if (step == natoms)
          break;
        atom = branch[nbranch--];
      } else if (ncandidates == 1) {
        parent[candidate[1]] = atom;
        atom = candidate[1];
        a[atom] = ++step;
      } else { // 2 a viac kandidatov 
        branch[++nbranch] = atom;
        // musi volit z viacerych moznych pokracovani
        int atomnew = 0;
        // hlada nering branch
        for (int i = 1; i <= ncandidates; i++) {
          int b = bondIdentity(candidate[i], atom);
          //zmenene oproti mgpj
          //if (btype[b]==h.AROMATIC || btype[b]==h.RING_NONAROMATIC) continue;
          if (isRingBond[b])
            continue;
          atomnew = candidate[i];
          break;
        }
        // ring branch = or # (aby potom closing bolo -)
        if (atomnew == 0) { // nenaslo nonring branch 
          for (int i = 1; i <= ncandidates; i++) {
            int b = bondIdentity(candidate[i], atom); // pozor nasv[], btype[]
            //if (isDouble(nasv[b]) || nasv[b]==TRIPLE) // !!! 
            if (btype[b] == DOUBLE || btype[b] == TRIPLE) {
              atomnew = candidate[i];
              break;
            }
          }
        }
        // nenaslo multiple ring bond, zoberie teda prve mozne (ring - or :)
        if (atomnew == 0)
          atomnew = candidate[1];
        parent[atomnew] = atom;
        atom = atomnew;
        a[atom] = ++step; // nemoze byt koniec, lebo viac kandidatov
      }
    }
    //for (int i=1;i<=natoms;i++) System.out.println(i+" "+a[i]+" "+an[i]);
    //for (int i=1;i<=nconnections;i++) System.out.println(i+" "+con1[i]+" "+con2[i]);

    // -------- 2. priechod
    parent = new int[natoms + 1]; // zmenene
    int aa[] = new int[natoms + 1]; // nove poradie
    boolean leftBracket[] = new boolean[natoms + 1];
    boolean rightBracket[] = new boolean[natoms + 1];

    nbranch = 0;
    step = 0;
    int atomold = 0;

    for (int i = 1; i <= natoms; i++)
      if (a[i] == 1) {
        atom = i;
        break;
      } // moze byt != 1?

    loopTwo: while (true) { // hlavny loop - kym neurobi vsetky atomy

      // adding bond
      if (atomold > 0)
        parent[atom] = atomold;

      // prida atom
      aa[++step] = atom; // poradie
      a[atom] = 0;

      // hlada dalsi atom
      // pri vetveni ide podla najnizsieho a[], ak viac moznosti urobi branch
      // musi naplnit atom (current) aj atomold (predosly s nim spojeny)
      int atomnew, ncandidates;
      while (true) {
        atomnew = 0;
        ncandidates = 0;
        int min = natoms + 1;
        cs1: for (int i = 1; i <= nv[atom]; i++) {
          int atomx = v[atom][i];
          // checknut ci atomx nie je s atom connected cislom
          for (int j = 1; j <= nconnections; j++)
            if (con1[j] == atomx && con2[j] == atom
                || con1[j] == atom && con2[j] == atomx)
              continue cs1;
          if (a[atomx] > 0) {
            ncandidates++;
            if (a[atomx] < min) {
              atomnew = atomx;
              min = a[atomx];
            }
          }
        }
        if (atomnew == 0) {
          // koniec branchu, alebo uzavrety kruh
          if (nbranch == 0)
            break loopTwo; // koniec 2. priechodu
          rightBracket[atom] = true;
          atom = branch[nbranch--];
          // musi ist znovu do loopu najst dalsi nespracovany atom
        } else
          break; // vnutorneho while loopu
      }

      atomold = atom;
      atom = atomnew;
      if (ncandidates > 1) {
        branch[++nbranch] = atomold;
        leftBracket[atom] = true;
      }

    } // end of 2. priechodu

    // identification of stereo atoms
    int slashBond[] = new int[nbonds + 1]; // info about / or \ bonds (1,0,or -1)
    int slimak[] = new int[natoms + 1]; // info about @ or @@ (1,0,or -1)
    if (jme.stereo)
      smilesStereo(aa, parent, slashBond, slimak, isRingBond, con1, con2,
          nconnections);

    // -------- vlastne vytvaranie SMILESu
    // poradie ako sa vytvara je ulozene v aa[] a parent[]
    boolean queryMode = false; // dqp support
    // all X atoms takes as query? how to improve this ?
    // 2009.04 reverted, caused problems c1:c:[Ir]:c:c:c:1
    // uncomment for dqp
    //for (int i=1;i<=natoms;i++) 
    //  if (an[i] == JME.AN_X && !label[i].equals("H")) queryMode = true;

    StringBuffer smiles = new StringBuffer("");
    int ax[] = new int[natoms + 1]; // kvoli connections
    for (int i = 1; i <= natoms; i++)
      ax[aa[i]] = i; // ax[i] - kolky sa robi atom i
    for (int i = 1; i <= natoms; i++) {
      atom = aa[i];

      if (leftBracket[atom])
        smiles.append("(");
      if (parent[i] > 0)
        smilesAddBond(atom, parent[atom], smiles, slashBond, queryMode);
      smilesAddAtom(atom, smiles, isAromatic[atom], slimak);

      for (int j = 1; j <= nconnections; j++) {
        if (con1[j] == atom || con2[j] == atom) {
          // pridava len 1. vazbu na connection
          int atom2 = con2[j];
          if (atom2 == atom)
            atom2 = con1[j];
          if (ax[atom] < ax[atom2])
            smilesAddBond(con1[j], con2[j], smiles, slashBond, queryMode);
          if (j > 9)
            smiles.append("%");
          smiles.append(new Integer(j).toString());
        }
      }

      if (rightBracket[atom])
        smiles.append(")");
    }

    return smiles.toString();
  }

  // ----------------------------------------------------------------------------
  private void smilesAddAtom(int atom, StringBuffer smiles, boolean isAromatic,
                             int slimak[]) {
    String z = "X";

    boolean bracket = false;
    if (q[atom] != 0)
      bracket = true;
    if (slimak[atom] != 0)
      bracket = true;
    int lmark = -1;
    for (int i = 1; i <= nmarked; i++)
      if (mark[i][0] == atom) {
        lmark = mark[i][1];
        break;
      }
    if (lmark > -1)
      bracket = true;
    if (jme.allHs)
      bracket = true;
    if (jme.star && abg[atom] > 0) {
      bracket = true;
      lmark = 1;
    }

    switch (an[atom]) {
    case JME.AN_B:
      z = "B";
      break;
    case JME.AN_C:
      if (isAromatic)
        z = "c";
      else
        z = "C";
      // tu aromaticitu ???
      break;
    case JME.AN_N:
      if (isAromatic) {
        z = "n";
        if (nh[atom] > 0)
          bracket = true;
      } else
        z = "N";
      break;
    case JME.AN_O:
      if (isAromatic)
        z = "o";
      else
        z = "O";
      break;
    case JME.AN_P:
      if (isAromatic) {
        z = "p";
        if (nh[atom] > 0)
          bracket = true;
      } else
        z = "P";
      break;
    case JME.AN_S:
      if (isAromatic)
        z = "s";
      else
        z = "S";
      break;
    case JME.AN_SE:
      if (isAromatic)
        z = "se";
      else
        z = "Se";
      bracket = true;
      break;
    case JME.AN_SI:
      z = "Si";
      bracket = true;
      break;
    case JME.AN_F:
      z = "F";
      break;
    case JME.AN_CL:
      z = "Cl";
      break;
    case JME.AN_BR:
      z = "Br";
      break;
    case JME.AN_I:
      z = "I";
      break;
    case JME.AN_H:
      z = "H";
      bracket = true;
      break;
    case JME.AN_R:
      z = "R";
      bracket = true;
      break;
    case JME.AN_R1:
      z = "R1";
      bracket = true;
      break;
    case JME.AN_R2:
      z = "R2";
      bracket = true;
      break;
    case JME.AN_R3:
      z = "R3";
      bracket = true;
      break;
    case JME.AN_X:
      bracket = true;
      z = label[atom];
      // special pre query - * a A
      if (z.equals("*") || z.equals("a") || z.equals("A"))
        bracket = false;
      break;
    }

    if (bracket) {
      z = "[" + z;
      if (slimak[atom] == 1)
        z += "@";
      else if (slimak[atom] == -1)
        z += "@@";
      if (nh[atom] == 1)
        z += "H";
      else if (nh[atom] > 1)
        z += "H" + nh[atom];
      if (q[atom] != 0) {
        if (q[atom] > 0)
          z += "+";
        else
          z += "-";
        if (Math.abs(q[atom]) > 1)
          z += Math.abs(q[atom]);
      }
      if (lmark > -1)
        z += ":" + lmark;
      z += "]";
    }

    smiles.append(z);
  }

  // ----------------------------------------------------------------------------
  private void smilesAddBond(int atom1, int atom2, StringBuffer smiles,
                             int slashBond[], boolean queryMode) {
    // adds bond to SMILES
    int b = bondIdentity(atom1, atom2);
    if (btype[b] != AROMATIC && isDouble(b))
      smiles.append("=");
    else if (nasv[b] == TRIPLE)
      smiles.append("#");
    else if (nasv[b] == QUERY) {
      /*
      String z = "?";
      switch (stereob[b]) {
        case QB_ANY: z = "~"; break;
        case QB_AROMATIC: z = ":"; break;
        case QB_RING: z = "@"; break;
        case QB_NONRING: z = "!@"; break;
      }
      */
      // 2007.10 dqp support
      String z = "?";
      Object o = btag[b];
      if (o != null)
        z = (String) o;
      smiles.append(z);
    } else if (btype[b] == AROMATIC && queryMode)
      smiles.append(":");
    // stereo   
    else if (slashBond[b] == 1)
      smiles.append("/");
    else if (slashBond[b] == -1)
      smiles.append("\\");

  }

  // ----------------------------------------------------------------------------
  private void smilesStereo(int aa[], int parent[], int slashBond[],
                            int slimak[], boolean isRingBond[], int con1[],
                            int con2[], int nconnections) {
    // --- identifikuje stereocentra, plni slashBond[] a slimak[]
    // (c) Peter Ertl - April 1999

    // pozor v aa[1] je ktory atom sa robi ako 1. (nie kolky sa robi atom 1)
    int ax[] = new int[natoms + 1];
    for (int i = 1; i <= natoms; i++)
      ax[aa[i]] = i; // ax[i] - kolky sa robi atom i 

    // E,Z bonds
    // nesmie ist v poradi od 1 do nbonds, lebo si poprepisuje slash[]
    // preto to je robene v poradi kreacie smilesu
    boolean doneEZ[] = new boolean[nbonds + 1]; // kvoli connections
    for (int i = 1; i <= natoms; i++) {
      int atom1 = aa[i]; // aa[] nie ax[] !
      int atom2 = parent[atom1];
      int bi = bondIdentity(atom1, atom2);
      if (bi == 0)
        continue;
      stereoEZ(bi, ax, slashBond, isRingBond);
      doneEZ[bi] = true;
    }
    // teraz este mozne spoje (malo pravdepodobne, ale pre istotu)
    for (int i = 1; i <= nbonds; i++) {
      if (!doneEZ[i])
        stereoEZ(i, ax, slashBond, isRingBond);
    }
    doneEZ = null;

    // C4 stereocentra && allene
    iloop: for (int i = 1; i <= natoms; i++) {
      if (nv[i] < 2 || nv[i] > 4)
        continue;
      int nstereo = 0, doubleBonded = 0;
      for (int j = 1; j <= nv[i]; j++) {
        int bi = bondIdentity(i, v[i][j]);
        if (btype[bi] == AROMATIC)
          continue iloop;
        if (nasv[bi] == SINGLE && upDownBond(bi, i) != 0)
          nstereo++;
        if (nasv[bi] == DOUBLE)
          doubleBonded = v[i][j];
      }
      if (nstereo == 0)
        continue;

      if (doubleBonded > 0) // allene, e.g. =C<stereo
        stereoAllene(i, ax, slimak, parent, con1, con2, nconnections);
      else // --- C4 stereo
        stereoC4(i, parent, ax, con1, con2, nconnections, slimak);
    }
  }

  // ----------------------------------------------------------------------------
  private void stereoC4(int atom, int parent[], int ax[], int con1[],
                        int con2[], int nconnections, int slimak[]) {

    // najde 4 referencne atomy (v poradi v akom su v SMILESe)
    int ref[] = new int[4]; // 0 - atom z ktoreho sa pozera
    int refx[] = new int[4]; // ci je up, dpwn, 0 

    identifyNeighbors(atom, ax, parent, con1, con2, nconnections, ref);

    // reference bonds + help variables
    int nup = 0, ndown = 0;
    int up = 0, down = 0, marked = 0, nonmarked = 0; // jediny markovany / nem.
    for (int i = 0; i < 4; i++) {
      if (ref[i] <= 0)
        continue;
      int bi = bondIdentity(atom, ref[i]);
      refx[i] = upDownBond(bi, atom);
      if (refx[i] > 0) {
        nup++;
        up = ref[i];
        marked = ref[i];
      } else if (refx[i] < 0) {
        ndown++;
        down = ref[i];
        marked = ref[i];
      } else
        nonmarked = ref[i];
    }
    int nstereo = nup + ndown;

    //for (int i=0;i<4;i++) System.out.println(atom+" "+ref[i]+" "+refx[i]);

    int ox[]; // ref[] ordered clockwise
    int t[] = new int[4]; // rohy tetraedra, transformacia t[] urci @ || @@ 
    int stereoRef = 0; // ci t[0] je up || down
    if (nv[atom] == 3) {
      if ((nup == 1 && ndown == 1) || (nstereo == 3 && nup > 0 && ndown > 0)) {
        jme.info("Error in C3H stereospecification !");
        return;
      }
      int refAtom = ref[0];
      if (nstereo == 1)
        refAtom = marked;
      else if (nstereo == 2)
        refAtom = nonmarked;
      ox = C4order(atom, refAtom, ref);
      t[0] = marked;
      t[1] = -1;
      t[2] = ox[2];
      t[3] = ox[1];
      if (nup > 0)
        stereoRef = 1;
      else
        stereoRef = -1;
    }

    else if (nv[atom] == 4) {
      if (nstereo == 1) {
        ox = C4order(atom, marked, ref);
        t[0] = ox[0];
        t[1] = ox[3];
        t[2] = ox[2];
        t[3] = ox[1];
        if (nup > 0)
          stereoRef = 1;
        else
          stereoRef = -1;
      } else { // 2,3,4 stereobonds
        int refAtom = ref[0];
        if (nonmarked > 1)
          refAtom = nonmarked;
        if (nup == 1)
          refAtom = up;
        else if (ndown == 1)
          refAtom = down;
        ox = C4order(atom, refAtom, ref);
        int box[] = new int[4]; // up,down info for ox[] atoms
        for (int i = 0; i < 4; i++) {
          int bi = bondIdentity(atom, ox[i]);
          box[i] = upDownBond(bi, atom);
        }

        if (nstereo == 4) {
          if (nup == 0 || ndown == 0) {
            jme.info("Error in C4 stereospecification !");
            return;
          } else if (nup == 1 || ndown == 1) { // 3/1 ta 1 je stereoRef
            t[0] = ox[0];
            t[1] = ox[3];
            t[2] = ox[2];
            t[3] = ox[1];
            stereoRef = box[0]; // up || down
          } else { // 2/2 - premeni na 2/0
            for (int i = 0; i < 4; i++)
              if (box[i] == -1)
                box[i] = 0;
            nstereo = 2;
          }
        } else if (nstereo == 3) {
          if (nup == 3 || ndown == 3) {
            // ref je single
            t[0] = ox[0];
            t[1] = ox[3];
            t[2] = ox[2];
            t[3] = ox[1];
            if (nup > 0)
              stereoRef = -1;
            else
              stereoRef = 1; // opacne ???
          } else { // 2,1,nonmarked, zmeni to na 2,0
            int d = 0;
            if (nup == 1) {
              d = 1;
              nup = 1;
            } else {
              d = -1;
              ndown = -1;
            }
            for (int i = 0; i < 4; i++)
              if (box[i] == d)
                box[i] = 0;
            nstereo = 2;
          }
        }
        if (nstereo == 2) { // if (nie else), lebo berie aj 4 a 3 zmenene na 2
          // 3 moznosti
          if (nup == 1 && ndown == 1) {
            // down nepocita (musi ju vyhodit z ox)
            if (ox[1] == down) {
              ox[1] = ox[2];
              ox[2] = ox[3];
            } else if (ox[2] == down) {
              ox[2] = ox[3];
            }
            t[0] = up;
            t[1] = down;
            t[2] = ox[2];
            t[3] = ox[1];
            stereoRef = 1;
          } else {
            // zistuje, ci markovane nie su vedla seba (to nesmie byt)
            if ((box[0] == box[1]) || (box[1] == box[2])) {
              jme.info("Error in C4 stereospecification ! 2/0r");
              return;
            }
            if (box[0] != 0) {
              t[0] = ox[0];
              t[1] = ox[2];
              t[2] = ox[1];
              t[3] = ox[3];
            } else {
              t[0] = ox[1];
              t[1] = ox[3];
              t[2] = ox[2];
              t[3] = ox[0];
            }
            if (nup > 1)
              stereoRef = 1;
            else
              stereoRef = -1; // ???
          }
        }
      }
    }

    stereoTransformation(t, ref);

    if (t[2] == ref[2])
      slimak[atom] = 1;
    else if (t[2] == ref[3])
      slimak[atom] = -1;
    else
      jme.info("Error in stereoprocessing ! - t30");

    slimak[atom] *= stereoRef;
  }

  // ----------------------------------------------------------------------------
  private void identifyNeighbors(int atom, int ax[], int parent[], int con1[],
                                 int con2[], int nconnections, int ref[]) {
    // naplna ref[] od 0 
    // urci sesedov atom-u v tom istom poradi ako su v SMILESe, uvazuje aj con
    int nref = -1;
    if (parent[atom] > 0)
      ref[++nref] = parent[atom];
    for (int i = 1; i <= nconnections; i++) { // poradie connections ako v SMILESe
      if (con1[i] == atom)
        ref[++nref] = con2[i];
      if (con2[i] == atom)
        ref[++nref] = con1[i];
    }
    for (int i = nref + 1; i < nv[atom]; i++) {
      int min = natoms + 1;
      jloop: for (int j = 1; j <= nv[atom]; j++) {
        int atomx = v[atom][j];
        for (int k = 0; k < i; k++)
          if (atomx == ref[k])
            continue jloop;
        if (ax[atomx] < min) {
          min = ax[atomx];
          ref[i] = atomx;
        }
      }
    }
    // teraz su zotriedne od 0 do 3, prip od 0 do 2 ak H
    // H ref atom markovany ako -1
    if (parent[atom] == 0 && nh[atom] > 0) { // nikdy nenastane, vzdy ma parent
      ref[3] = ref[2];
      ref[2] = ref[1];
      ref[1] = ref[0];
      ref[0] = -1;
      System.out.println("stereowarning #7");
    } else if (nh[atom] > 0) {
      ref[3] = ref[2];
      ref[2] = ref[1];
      ref[1] = -1;
    }
  }

  // ----------------------------------------------------------------------------
  int[] C4order(int center, int ref0, int ref[]) {
    // vrati clockwise poradie ostatnych atomov okolo center, relativne k ref0
    // vrati 2, alebo 3 atomy ako ox[1], ox[2] (ox[3])
    // v ox[0] vrati ref0
    int ox[] = new int[4];
    double dx, dy, rx;

    // referencny uhol x-axis - center - ref0
    dx = x[ref0] - x[center];
    dy = y[ref0] - y[center];
    rx = Math.sqrt(dx * dx + dy * dy);
    if (rx < 0.001)
      rx = 0.001;
    double sin0 = dy / rx;
    double cos0 = dx / rx;

    // ostatne atomy otaca clockwise o uhol sin0, cos0
    // dx = x * cosa + y * sina
    // dy = y * cosa - x * sina

    // naplna p[]
    int p[] = new int[4];
    for (int i = 0; i < 4; i++) {
      if (ref[i] == ref0 || ref[i] <= 0)
        continue; // moze byt -1 ?? treba to
      if (p[1] == 0) {
        p[1] = ref[i];
        continue;
      }
      if (p[2] == 0) {
        p[2] = ref[i];
        continue;
      }
      if (p[3] == 0) {
        p[3] = ref[i];
        continue;
      }
    }

    double sin[] = new double[4], cos[] = new double[4];
    for (int i = 1; i <= 3; i++) {
      if (i == 3 && p[3] == 0)
        continue;
      dx = (x[p[i]] - x[center]) * cos0 + (y[p[i]] - y[center]) * sin0;
      dy = (y[p[i]] - y[center]) * cos0 - (x[p[i]] - x[center]) * sin0;
      rx = Math.sqrt(dx * dx + dy * dy);
      if (rx < 0.001)
        rx = 0.001;
      sin[i] = dy / rx;
      cos[i] = dx / rx;
    }

    // teraz zoradi p[1] - p[3] podla velkosti (najmensi bude 1, potom 2 ...)
    // 6 moznych kombinacii
    int c12 = compareAngles(sin[1], cos[1], sin[2], cos[2]);
    if (p[3] > 0) {
      int c23 = compareAngles(sin[2], cos[2], sin[3], cos[3]);
      int c13 = compareAngles(sin[1], cos[1], sin[3], cos[3]);
      if (c12 > 0 && c23 > 0) {
        ox[1] = p[1];
        ox[2] = p[2];
        ox[3] = p[3];
      } else if (c13 > 0 && c23 < 0) {
        ox[1] = p[1];
        ox[2] = p[3];
        ox[3] = p[2];
      } else if (c12 < 0 && c13 > 0) {
        ox[1] = p[2];
        ox[2] = p[1];
        ox[3] = p[3];
      } else if (c23 > 0 && c13 < 0) {
        ox[1] = p[2];
        ox[2] = p[3];
        ox[3] = p[1];
      } else if (c13 < 0 && c12 > 0) {
        ox[1] = p[3];
        ox[2] = p[1];
        ox[3] = p[2];
      } else if (c23 < 0 && c12 < 0) {
        ox[1] = p[3];
        ox[2] = p[2];
        ox[3] = p[1];
      }
    }
    // porovnanie len 2 uhlov (2 and 3)
    else {
      if (c12 > 0) {
        ox[1] = p[1];
        ox[2] = p[2];
      } else {
        ox[1] = p[2];
        ox[2] = p[1];
      }
    }
    //System.out.println("center = "+center+" ref = "+ref0+" order "+ox[1]+" "+ox[2]+" "+ox[3]);
    ox[0] = ref0;
    return ox;
  }

  // ----------------------------------------------------------------------------
  private void stereoTransformation(int t[], int ref[]) {
    //System.out.println(t[0]+" "+t[1]+" "+t[2]+" "+t[3]+" --- ");
    int d = 0;
    if (ref[0] == t[1]) // 0,1  2,3 
    {
      d = t[0];
      t[0] = t[1];
      t[1] = d;
      d = t[2];
      t[2] = t[3];
      t[3] = d;
    } else if (ref[0] == t[2]) // 0,2  1,3 
    {
      d = t[2];
      t[2] = t[0];
      t[0] = d;
      d = t[1];
      t[1] = t[3];
      t[3] = d;
    } else if (ref[0] == t[3]) // 0,3  1,2 
    {
      d = t[3];
      t[3] = t[0];
      t[0] = d;
      d = t[1];
      t[1] = t[2];
      t[2] = d;
    }

    if (ref[1] == t[2]) // 1,2  2,3
    {
      d = t[1];
      t[1] = t[2];
      t[2] = d;
      d = t[2];
      t[2] = t[3];
      t[3] = d;
    } else if (ref[1] == t[3]) // 1,3  2,3
    {
      d = t[1];
      t[1] = t[3];
      t[3] = d;
      d = t[2];
      t[2] = t[3];
      t[3] = d;
    }
  }

  // ----------------------------------------------------------------------------
  private void stereoEZ(int bond, int ax[], int slashBond[],
                        boolean isRingBond[]) {

    if (nasv[bond] != DOUBLE || btype[bond] == AROMATIC)
      return;
    if (!(stereob[bond] == EZ || (jme.autoez && !isRingBond[bond])))
      return;

    int atom1 = va[bond], atom2 = vb[bond];
    // vyhodi =CH2, =O, =P-<, ...
    if (nv[atom1] < 2 || nv[atom2] < 2 || nv[atom1] > 3 || nv[atom2] > 3)
      return;
    if (ax[atom1] > ax[atom2]) {
      int d = atom1;
      atom1 = atom2;
      atom2 = d;
    }

    int ref1 = 0, ref11 = 0, ref12 = 0;
    boolean ref1x = false; // ze nie je pred, ale za atom1
    for (int j = 1; j <= nv[atom1]; j++) {
      int atomx = v[atom1][j];
      if (atomx == atom2)
        continue;
      if (ref11 == 0)
        ref11 = atomx;
      else
        ref12 = atomx;
    }
    if (ref12 > 0 && ax[ref11] > ax[ref12]) {
      int d = ref11;
      ref11 = ref12;
      ref12 = d;
    }
    int bi = bondIdentity(atom1, ref11);
    if (slashBond[bi] != 0) {
      ref1 = ref11;
    } else if (nasv[bi] == SINGLE && btype[bi] != AROMATIC)
      ref1 = ref11;
    if (ref1 == 0) {
      bi = bondIdentity(atom1, ref12);
      if (slashBond[bi] != 0)
        ref1 = ref12;
      else if (nasv[bi] == SINGLE && btype[bi] != AROMATIC)
        ref1 = ref12;
    }
    if (ax[ref1] > ax[atom1])
      ref1x = true;

    // berie prazdnu (treba ref2x ???)
    int ref2 = 0, ref21 = 0, ref22 = 0;
    for (int j = 1; j <= nv[atom2]; j++) {
      int atomx = v[atom2][j];
      if (atomx == atom1)
        continue;
      if (ref21 == 0)
        ref21 = atomx;
      else
        ref22 = atomx;
    }
    // to dava 21 s vacsim ax[] (na rozdiel id 12 s mensim ax[])
    if (ref22 > 0 && ax[ref21] < ax[ref22]) {
      int d = ref21;
      ref21 = ref22;
      ref22 = d;
    }
    bi = bondIdentity(atom2, ref21);
    if (nasv[bi] == SINGLE && btype[bi] != AROMATIC && slashBond[bi] == 0)
      ref2 = ref21;
    if (ref2 == 0) {
      bi = bondIdentity(atom2, ref22);
      if (nasv[bi] == SINGLE && btype[bi] != AROMATIC)
        ref2 = ref22; // ref2x netreba
    }

    if (ref1 == 0 || ref2 == 0)
      return; // vyhadzuje aj allene vazby

    double dx = x[atom2] - x[atom1];
    double dy = y[atom2] - y[atom1];
    double rx = Math.sqrt(dx * dx + dy * dy);
    if (rx < 0.001)
      rx = 0.001;
    double sina = dy / rx;
    double cosa = dx / rx;
    double y1 = (y[ref1] - y[atom1]) * cosa - (x[ref1] - x[atom1]) * sina;
    double y2 = (y[ref2] - y[atom1]) * cosa - (x[ref2] - x[atom1]) * sina;
    if (Math.abs(y1) < 2 || Math.abs(y2) < 2) {
      jme.info("Not unique E/Z geometry !");
      return;
    }
    int b1 = bondIdentity(ref1, atom1);
    int b2 = bondIdentity(ref2, atom2);
    int newSlash = 1;
    if (slashBond[b1] == 0) {
      // ceknut, ci nove stereo neprotireci uz exisujucej na ref1
      for (int j = 1; j <= nv[ref1]; j++) {
        int atomx = v[ref1][j];
        if (atomx == atom1)
          continue;
        bi = bondIdentity(ref1, atomx);
        if (slashBond[bi] != 0) {
          // zalezi od toho ci ta co uz je ma ax[atomx] <> ax[ref1] 
          if (ax[atomx] > ax[ref1])
            newSlash = -slashBond[bi];
          else
            newSlash = slashBond[bi]; // lebo ax[atom] < ax[ref1] ???
          break;
        }
      }
      slashBond[b1] = newSlash;
    }
    if (slashBond[b2] != 0) {
      System.err.println("E/Z internal error !");
      return; // prepisuje slash
    }
    // to aj zalezi, ci u tej prvej ref je pred, alebo za atomom (v smilese)
    if ((y1 > 0 && y2 > 0) || (y1 < 0 && y2 < 0))
      slashBond[b2] = -slashBond[b1];
    else
      slashBond[b2] = slashBond[b1];
    if (ref1x)
      slashBond[b2] = -slashBond[b2]; // ref1 je za atomom
  }

  // ----------------------------------------------------------------------------
  private int compareAngles(double sina, double cosa, double sinb,
                            double cosb) {
    // returns 1 if a < b (clockwise) -1 a > b, 0 ak a = b
    int qa = 0, qb = 0; // kvadrant
    if (sina >= 0. && cosa >= 0.)
      qa = 1;
    else if (sina >= 0. && cosa < 0.)
      qa = 2;
    else if (sina < 0. && cosa < 0.)
      qa = 3;
    else if (sina < 0. && cosa >= 0.)
      qa = 4;
    if (sinb >= 0. && cosb >= 0.)
      qb = 1;
    else if (sinb >= 0. && cosb < 0.)
      qb = 2;
    else if (sinb < 0. && cosb < 0.)
      qb = 3;
    else if (sinb < 0. && cosb >= 0.)
      qb = 4;
    if (qa < qb)
      return 1;
    else if (qa > qb)
      return -1;
    // su v rovnakom kvadrante
    switch (qa) {
    case 1:
    case 4:
      return (sina < sinb ? 1 : -1);
    case 2:
    case 3:
      return (sina > sinb ? 1 : -1);
    }
    System.err.println("stereowarning #31");
    return 0;
  }

  // ----------------------------------------------------------------------------
  private int upDownBond(int bond, int atom) {
    // zistuje, ci stereo bond je relevantna k atomu (ci na nom je hrot vazby)
    // ci ide hore 1, dolu -1, alebo nie je stereo
    // pri UP a DOWN je hrot na va[bond], pri XUP a XDOWN na vb[bond]
    int sb = stereob[bond];
    if (sb < 1 || sb > 4)
      return 0;
    if (sb == UP && va[bond] == atom)
      return 1;
    if (sb == DOWN && va[bond] == atom)
      return -1;
    if (sb == XUP && vb[bond] == atom)
      return 1;
    if (sb == XDOWN && vb[bond] == atom)
      return -1;
    return 0;
  }

  // ----------------------------------------------------------------------------
  /**
   * @param i 
   * @param ax 
   * @param slimak 
   * @param parent 
   * @param con1 
   * @param con2 
   * @param nconnections  
   */
  private void stereoAllene(int i, int ax[], int slimak[], int parent[],
                            int con1[], int con2[], int nconnections) {
    double dx, dy, rx, sina, cosa;

    int nal = 1; // kolko ich je v chaine
    int ala = i; // ala - actually processed atom
    int al[] = new int[natoms + 1];
    al[1] = i;
    while (true) { // loop po allene chain po double vazbe
      boolean ok = false;
      for (int j = 1; j <= nv[ala]; j++) {
        int atomx = v[ala][j];
        if (atomx == al[1] || atomx == al[nal - 1])
          continue;
        int bi = bondIdentity(ala, atomx);
        if (nasv[bi] == DOUBLE && btype[bi] != AROMATIC) {
          al[++nal] = atomx;
          ala = atomx;
          ok = true;
          break;
        }
      }
      if (!ok)
        break;
    }
    if (nal % 2 == 0)
      return; // parny allene
    if (nv[al[nal]] < 2 || nv[al[nal]] > 3)
      return; // nv[al[1]] uz done
    // allene definovany start=center=end, aspon na al[1] stereob
    int start = al[1];
    int center = al[(nal + 1) / 2];
    int end = al[nal];

    int ref11 = 0, ref12 = 0, ref21 = 0, ref22 = 0, ref1 = 0, ref2 = 0;
    boolean ref1x = false, ref2x = false; // ci je 1. alebo 2. na atome
    // co s >n=C=n< - tam sa neda definovat stereo, lebo nie su ref.

    // ref. at start - ta musi byt aspon 1 stereo (inak by to tu nebolo)
    for (int j = 1; j <= nv[start]; j++) {
      int atomx = v[start][j];
      int bi = bondIdentity(start, atomx);
      if (nasv[bi] != SINGLE || btype[bi] == AROMATIC)
        continue;
      if (ref11 == 0)
        ref11 = atomx;
      else
        ref12 = atomx;
    }
    if (ax[ref12] > 0 && ax[ref11] > ax[ref12]) {
      int d = ref11;
      ref11 = ref12;
      ref12 = d;
    }
    ref1 = ref11;
    if (ref1 == 0) {
      ref1 = ref12;
      ref1x = true;
    }

    // reference at end
    for (int j = 1; j <= nv[end]; j++) {
      int atomx = v[end][j];
      int bi = bondIdentity(end, atomx);
      if (nasv[bi] != SINGLE || btype[bi] == AROMATIC)
        continue;
      if (ref21 == 0)
        ref21 = atomx;
      else
        ref22 = atomx;
    }
    if (ax[ref22] > 0 && ax[ref21] > ax[ref22]) {
      int d = ref21;
      ref21 = ref22;
      ref22 = d;
    }
    ref2 = ref21;
    if (ref2 == 0) {
      ref2 = ref22;
      ref2x = true;
    }

    // na  start moze byt 1 alebo 2 stereo (ak 2 => opacne), na end nesmie byt
    int ref11x = upDownBond(bondIdentity(start, ref11), start);
    int ref12x = upDownBond(bondIdentity(start, ref12), start);
    int ref21x = upDownBond(bondIdentity(end, ref21), end);
    int ref22x = upDownBond(bondIdentity(end, ref22), end);

    if (Math.abs(ref11x + ref12x) > 1 || ref21x != 0 || ref22x != 0) {
      jme.info("Bad stereoinfo on allene !");
      return;
    }

    // vlastne nam treba len 1 (up alebo down) a 3 (resp opak 4 ak je 3 H)
    // urcuje  poziciu ref2
    dx = x[al[nal - 1]] - x[end];
    dy = y[al[nal - 1]] - y[end];
    rx = Math.sqrt(dx * dx + dy * dy);
    if (rx < 0.001)
      rx = 0.001;
    sina = dy / rx;
    cosa = dx / rx;
    double y2 = (y[ref2] - y[al[nal - 1]]) * cosa
        - (x[ref2] - x[al[nal - 1]]) * sina;

    // teraz to bude dost complikovane
    if (y2 > 0)
      slimak[center] = 1;
    else
      slimak[center] = -1;
    if (ref1x)
      slimak[center] *= -1;
    if (ref2x)
      slimak[center] *= -1;
    if (ref1 == ref11 && ref11x < 0)
      slimak[center] *= -1;
    if (ref1 == ref12 && ref12x < 0)
      slimak[center] *= -1;
    if (ax[ref1] > ax[ref2])
      slimak[center] *= -1;
  }

  // ----------------------------------------------------------------------------
  String createJME() {
    // cislovanie nezavisle od smilesu
    // parameter jmeh urcuje pridavanie H
    String s = "" + natoms + " " + nbonds;
    double scale = 1.4 / RBOND;
    for (int i = 1; i <= natoms; i++) {
      String z = getAtomLabel(i);
      //if (jme.jmeh && an[i] != JME.AN_C && nh[i] > 0) {
      if (jme.jmeh && nh[i] > 0) { // aj pre C
        z += "H";
        if (nh[i] > 1)
          z += nh[i];
      }
      // naboje
      if (q[i] != 0) {
        if (q[i] > 0)
          z += "+";
        else
          z += "-";
        if (Math.abs(q[i]) > 1)
          z += Math.abs(q[i]);
      }

      // mapping
      int lmark = -1;
      for (int j = 1; j <= nmarked; j++)
        if (mark[j][0] == i) {
          lmark = mark[j][1];
          break;
        }
      if (jme.star && abg[i] > 0)
        lmark = 1;
      if (lmark > -1)
        z += ":" + lmark;

      // inverts y coordinate
      s += " " + z + " " + fformat(x[i] * scale, 0, 2) + " "
          + fformat(-y[i] * scale, 0, 2);
    }
    for (int i = 1; i <= nbonds; i++) {
      int a1 = va[i], a2 = vb[i], nas = nasv[i];
      if (stereob[i] == UP)
        nas = -1;
      else if (stereob[i] == DOWN)
        nas = -2;
      else if (stereob[i] == XUP) {
        nas = -1;
        int d = a1;
        a1 = a2;
        a2 = d;
      } else if (stereob[i] == XDOWN) {
        nas = -2;
        int d = a1;
        a1 = a2;
        a2 = d;
      } else if (stereob[i] == EZ) {
        nas = -5;
      } // 2006.09
      // query (typ is stored in stereob)
      if (nasv[i] == QUERY)
        nas = stereob[i];
      s += " " + a1 + " " + a2 + " " + nas;
    }
    return s;
  }

  // --------------------------------------------------------------------------
  String createMolFile(String title) {
    if (natoms == 0)
      return ""; // 2008.12

//    int nradicals = 0;
//    int[] radical = new int[natoms + 1];
//
    String s = "";
    //header
    s = title;
    if (s.length() > 79)
      s = s.substring(0, 76) + "...";
    s += JME.separator;
    // since 2006.01 added one space to header line (two newlines causes problems in tokenizer)
    s += PropertyManager.getSDFDateLine("JME " + JME.version, true);
    s += "JME " + JME.version + " " + new Date() + JME.separator;
    //counts line
    s += iformat(natoms, 3) + iformat(nbonds, 3);
    s += "  0  0  0  0  0  0  0  0999 V2000" + JME.separator;
    // atoms
    double scale = 1.4 / RBOND; // ??? co je standard scale ???
    double ymax = -Double.MAX_VALUE;
    double xmin = Double.MAX_VALUE;
    for (int i = 1; i <= natoms; i++) { // to start at 0,0
      if (y[i] > ymax)
        ymax = y[i];
      if (x[i] < xmin)
        xmin = x[i];
    }
    for (int i = 1; i <= natoms; i++) {
      // inverts y coordinate to match ISISDraw coord system
      s += fformat((x[i] - xmin) * scale, 10, 4)
          + fformat((ymax - y[i]) * scale, 10, 4) + fformat(0.0, 10, 4);
      String z = getAtomLabel(i);

      if (z.length() == 1)
        z += "  ";
      else if (z.length() == 2)
        z += " ";
      else if (z.length() > 3)
        z = "Q  "; // query ??? 
      s += " " + z;

      // isotope, charge
      int charge = 0;
      if (q[i] > 0 && q[i] < 4)
        charge = 4 - q[i];
      else if (q[i] < 0 && q[i] > -4)
        charge = 4 - q[i];
      z = " 0" + iformat(charge, 3) + "  0  0  0  0  0  0  0";
      // adding atom mapping (if any)
      int lmark = -1;
      for (int j = 1; j <= nmarked; j++)
        if (mark[j][0] == i) {
          lmark = mark[j][1];
          break;
        }
      if (lmark > -1)
        z += iformat(lmark, 3);
      else
        z += "  0";
      s += z + "  0  0" + JME.separator;

    }
    // bonds
    // 2012.05 added stereo flag for marked EZ stereo bonds
    // TODO
    for (int i = 1; i <= nbonds; i++) {
      int nas = nasv[i];
      if (isSingle(i))
        nas = 1;
      else if (isDouble(i))
        nas = 2;
      String bonds = iformat(va[i], 3) + iformat(vb[i], 3);
      int stereo = 0;
      if (nasv[i] == SINGLE && stereob[i] == UP)
        stereo = 1;
      else if (nasv[i] == SINGLE && stereob[i] == DOWN)
        stereo = 6;
      // XUP & XDOWN prevratia poradie atomov va a vb (kvoli ostremu koncu)
      if (nasv[i] == SINGLE && stereob[i] == XUP) {
        stereo = 1;
        bonds = iformat(vb[i], 3) + iformat(va[i], 3);
      }
      if (nasv[i] == SINGLE && stereob[i] == XDOWN) {
        stereo = 6;
        bonds = iformat(vb[i], 3) + iformat(va[i], 3);
      }
      s += bonds + iformat(nas, 3) + iformat(stereo, 3) + "  0  0  0"
          + JME.separator;
    }

    // charges on standard atoms
    for (int i = 1; i <= natoms; i++)
      if (q[i] != 0) {
        s += "M  CHG  1" + iformat(i, 4) + iformat(q[i], 4) + JME.separator;
      }

    // radical (X atoms)
    /*
    for (int i=1;i<=nradicals;i++) {
      if (an[i] == JME.AN_X) {
        s += "M  RAD  1" + iformat(radical[i],4) + iformat(2,4) + JME.separator;
      }
    }
    */

    s += "M  END" + JME.separator;
    return s;
  }

  // ----------------------------------------------------------------------------
  // Molfile V3000 - 2006.09
  String createExtendedMolFile(String smiles) {

//    int nradicals = 0;
//    int[] radical = new int[natoms + 1];
    // finding whether molecule is chiral
    int chiral = 0;
    for (int i = 1; i <= nbonds; i++)
      if (stereob[i] != 0) {
        chiral = 1;
        break;
      }

    String mv30 = "M  V30 ";
    String s = "";
    //header
    s = smiles;
    if (s.length() > 79)
      s = s.substring(0, 76) + "...";
    s += JME.separator;
    s += "JME " + JME.version + " " + new Date() + JME.separator + " "
        + JME.separator;
    //counts line
    s += "  0  0  0  0  0  0  0  0  0  0999 V3000" + JME.separator;
    s += mv30 + "BEGIN CTAB" + JME.separator;
    // at the end should be chiral flag 1 or 0
    s += mv30 + "COUNTS " + natoms + " " + nbonds + " 0 0 " + chiral
        + JME.separator;
    s += mv30 + "BEGIN ATOM" + JME.separator;
    // atoms
    double scale = 1.4 / RBOND; // ??? co je standard scale ???
    double ymax = -Double.MAX_VALUE;
    double xmin = Double.MAX_VALUE;
    for (int i = 1; i <= natoms; i++) { // to start at 0,0
      if (y[i] > ymax)
        ymax = y[i];
      if (x[i] < xmin)
        xmin = x[i];
    }
    for (int i = 1; i <= natoms; i++) {
      s += mv30;
      // inverts y coordinate to match ISISDraw coord system
      String z = getAtomLabel(i);
      s += i + " " + z;

      int m = 0;
      int lmark = -1;
      for (int j = 1; j <= nmarked; j++)
        if (mark[j][0] == i) {
          lmark = mark[j][1];
          break;
        }
      if (lmark > -1)
        m = lmark; // ignores 0 mark

      s += " " + fformat((x[i] - xmin) * scale, 0, 4) + " "
          + fformat((ymax - y[i]) * scale, 0, 4) + " " + fformat(0.0, 0, 4)
          + " " + m;
      if (q[i] != 0)
        s += " CHG=" + q[i];
      // JME curremtly ignoring isotopes & radicals
      //if (q[i] != 0) s+ " MASS="+q[i];

      s += JME.separator;

    }
    s += mv30 + "END ATOM" + JME.separator;
    s += mv30 + "BEGIN BOND" + JME.separator;
    // bonds
    for (int i = 1; i <= nbonds; i++) {
      s += mv30 + i;
      int nas = nasv[i];
      if (isSingle(i))
        nas = 1;
      else if (isDouble(i))
        nas = 2;

      String bonds = va[i] + " " + vb[i];
      int stereo = 0;
      if (nasv[i] == SINGLE && stereob[i] == UP)
        stereo = 1;
      else if (nasv[i] == SINGLE && stereob[i] == DOWN)
        stereo = 3;
      // XUP & XDOWN prevratia poradie atomov va a vb (kvoli ostremu koncu)
      if (nasv[i] == SINGLE && stereob[i] == XUP) {
        stereo = 1;
        bonds = vb[i] + " " + va[i];
      }
      if (nasv[i] == SINGLE && stereob[i] == XDOWN) {
        stereo = 3;
        bonds = vb[i] + " " + va[i];
      }

      s += " " + bonds + " " + nas;
      if (stereo != 0)
        s += " CFG=" + stereo;

      s += JME.separator;
    }
    s += mv30 + "END BOND" + JME.separator;

    // stereo collections
    // MDLV30/STEABS /STERACn /STERELn
    ArrayList<Integer> abs = new ArrayList<Integer>();
    ArrayList<ArrayList<Integer>> orlists = new ArrayList<ArrayList<Integer>>();
    ArrayList<ArrayList<Integer>> mixlists = new ArrayList<ArrayList<Integer>>();
    for (int i = 0; i < 10; i++) {
      orlists.add(null);
      mixlists.add(null);
    }
    for (int i = 1; i <= natoms; i++) {
      if (atag[i] == null || atag[i].length() == 0)
        continue;
      if (atag[i].equals("abs"))
        abs.add(new Integer(i));
      else if (atag[i].startsWith("mix")) {
        int n = Integer.parseInt(atag[i].substring(3));
        ArrayList<Integer> o = null;
        if (mixlists.size() > n)
          o = mixlists.get(n);
        ArrayList<Integer> l = (o == null ? new ArrayList<Integer>() : o);
        l.add(new Integer(i));
        mixlists.set(n, l);
      } else if (atag[i].startsWith("or")) {
        int n = Integer.parseInt(atag[i].substring(2));
        ArrayList<Integer> o = null;
        if (orlists.size() > n)
          o = orlists.get(n);
        ArrayList<Integer> l = (o == null ? new ArrayList<Integer>() : o);
        l.add(new Integer(i));
        orlists.set(n, l);
      }
    }
    s += addCollection("MDLV30/STEABS", abs, mv30);
    if (orlists.size() > 0)
      for (int i = 1; i < orlists.size(); i++)
        s += addCollection("MDLV30/STEREL" + i, orlists.get(i),
            mv30);
    if (mixlists.size() > 0)
      for (int i = 1; i < mixlists.size(); i++)
        s += addCollection("MDLV30/STERAC" + i, mixlists.get(i),
            mv30);

    s += mv30 + "END CTAB" + JME.separator;
    s += "M  END" + JME.separator;
    return s;
  }

  // ----------------------------------------------------------------------------
  String addCollection(String name, ArrayList<Integer> list, String mv30) {
    if (list == null || list.size() == 0)
      return "";
    String s = "";
    s += mv30 + "BEGIN COLLECTION" + JME.separator;
    s += mv30 + name + " [ATOMS=(" + list.size();
    for (Iterator<Integer> i = list.iterator(); i.hasNext();)
      s += " " + i.next();
    s += ")]" + JME.separator;
    s += mv30 + "END COLLECTION" + JME.separator;
    return s;
  }

  // ----------------------------------------------------------------------------
  String getAtomLabel(int i) {
    String z = jme.zlabel[an[i]]; // aj C kvoli ramcekom 
    if (an[i] == JME.AN_X)
      z = label[i];
    return z;
  }

  // ----------------------------------------------------------------------------
  static String iformat(int number, int len) {
    Integer n = new Integer(number);
    String s = n.toString();
    if (s.length() > len)
      s = "?";
    String space = "";
    for (int i = 1; i <= len - s.length(); i++)
      space += " ";
    s = space + s;
    return s;
  }

  // ----------------------------------------------------------------------------
  static String fformat(double number, int len, int dec) {
    // este pridat zmensovanie dec, ked dlzka nestaci
    if (dec == 0)
      return iformat((int) number, len);
    if (Math.abs(number) < 0.0009)
      number = 0.; // 2012 fix 1.0E-4
    number = (int) Math.round(number * Math.pow(10., dec))
        / (Math.pow(10., dec));
    String s = new Double(number).toString(); // this sometimes return 1.0E-4
    int dotpos = s.indexOf('.');
    if (dotpos < 0) {
      s += ".";
      dotpos = s.indexOf('.');
    }
    int slen = s.length();
    for (int i = 1; i <= dec - slen + dotpos + 1; i++)
      s += "0";
    if (len == 0)
      return s;
    if (s.length() > len)
      s = "?";
    String space = "";
    for (int i = 1; i <= len - s.length(); i++)
      space += " ";
    s = space + s;
    return s;
  }

  // ----------------------------------------------------------------------------
  static int checkAtomicSymbol(String s) {
    if (s.equals("C"))
      return JME.AN_C;
    else if (s.equals("B"))
      return JME.AN_B;
    else if (s.equals("N"))
      return JME.AN_N;
    else if (s.equals("O"))
      return JME.AN_O;
    else if (s.equals("P"))
      return JME.AN_P;
    else if (s.equals("S"))
      return JME.AN_S;
    else if (s.equals("F"))
      return JME.AN_F;
    else if (s.equals("Cl"))
      return JME.AN_CL;
    else if (s.equals("Br"))
      return JME.AN_BR;
    else if (s.equals("I"))
      return JME.AN_I;
    else if (s.equals("H"))
      return JME.AN_H;
    else if (s.equals("Se"))
      return JME.AN_SE;
    else if (s.equals("Si"))
      return JME.AN_SI; // 2007.02
    else if (s.equals("R"))
      return JME.AN_R;
    else if (s.equals("R1"))
      return JME.AN_R1;
    else if (s.equals("R2"))
      return JME.AN_R2;
    else if (s.equals("R3"))
      return JME.AN_R3;
    else
      return JME.AN_X;
  }

  // ----------------------------------------------------------------------------
  void deleteHydrogens() {
    if (jme.keepHydrogens)
      return;
    iloop: for (int i = natoms; i >= 1; i--) {
      int parent = v[i][1];
      if (an[i] == JME.AN_H && nv[i] == 1 && q[i] == 0 && an[parent] != JME.AN_H
          && an[parent] < JME.AN_X) { // X R R1 R2 R3 
        // zistuje ci je H marked
        for (int j = 1; j <= nmarked; j++)
          if (mark[j][0] == i)
            continue iloop;
        int bi = bondIdentity(i, parent);
        if (nasv[bi] == SINGLE) {
          if (stereob[bi] == 0 || !jme.stereo)
            deleteAtom(i);
        }
      }
    }
  }

  // ----------------------------------------------------------------------------
  int bondIdentity(int atom1, int atom2) {
    for (int i = 1; i <= nbonds; i++) {
      if (va[i] == atom1 && vb[i] == atom2)
        return i;
      if (va[i] == atom2 && vb[i] == atom1)
        return i;
    }
    return 0;
  }

  // ----------------------------------------------------------------------------
  boolean isSingle(int bond) {
    return (nasv[bond] == SINGLE);
  }

  // ----------------------------------------------------------------------------
  private boolean isDouble(int bond) {
    return (nasv[bond] == DOUBLE);
  }

  // ----------------------------------------------------------------------------
  void valenceState() {
    for (int i = 1; i <= natoms; i++) {
      atomValenceState(i);
    }
  }

  // ----------------------------------------------------------------------------
  void atomValenceState(int i) {
    int sbo = sumBondOrders(i); // sum bond orders to nonhydrogen atoms
    if (sbo == -1) {
      nh[i] = 0;
      return;
    } // query bond
    switch (an[i]) {
    // added 2005.02
    case JME.AN_H: // -[H+]- allowed (as in boranes) 
      if (sbo == 2)
        q[i] = 1;
      else
        q[i] = 0;
      nh[i] = 0;
      break;
    // B changed in 2005.02
    case JME.AN_B: // BH2+ BH3 BH4- BH5 BH6+ BH7++
      if (sbo == 3 || sbo == 5) {
        nh[i] = 0;
        q[i] = 0;
      } else if (sbo < 3) { // onlu here charge switch +/0 possible
        nh[i] = 3 - sbo - q[i];
      } else if (sbo == 4) {
        q[i] = -1;
        nh[i] = 0;
      } else if (sbo > 5) {
        q[i] = sbo - 5;
        nh[i] = 0;
      }
      break;
    case JME.AN_C:
    case JME.AN_SI: // Si since 2007.02
      if (sbo < 4) { // special treatment of carbocations and anions
        if (q[i] > 0)
          nh[i] = 2 - sbo + q[i];
        else if (q[i] < 0)
          nh[i] = 2 - sbo - q[i];
        else
          nh[i] = 4 - sbo;
      } else { // sbo >= 4
        q[i] = sbo - 4;
        nh[i] = 4 - sbo + q[i];
      }
      break;
    case JME.AN_N:
    case JME.AN_P:
      if (sbo < 3)
        nh[i] = 3 - sbo + q[i];
      //else if (sbo == 3) {if(q[i] < 0) q[i] = 0; nh[i] = 3 - sbo + q[i];}
      // else if (sbo == 3) {q[i] = 0; nh[i] = 3 - sbo;} // 2002.12
      // 2004.04 >[NH+]- sbo 3 charge 1
      else if (sbo == 3) {
        if (q[i] < 0) {
          q[i] = 0;
          nh[i] = 0;
        } else if (q[i] > 0)
          nh[i] = q[i];
        else
          nh[i] = 3 - sbo; // 2002.12
      } else if (sbo == 4) {
        q[i] = 1;
        nh[i] = 0;
      } // nh=0 fix 2002.08
      // january 2002, pri sbo > 4, da vodiky na 0
      // 10.2005 XF6+ changed to XF6-
      else if (sbo == 6) {
        q[i] = -1;
        nh[i] = 0;
      } else {
        q[i] = sbo - 5;
        nh[i] = 0;
      } // i.e. 5 && > 6 ???

      break;
    case JME.AN_O: // -[O-] -O- =O -[O+]< >[O2+]< ...
      //if (sbo == 2 && q[i] < 0) q[i] = 0;
      //if (sbo == 2) q[i] = 0; // 2002.12
      if (sbo == 2) {
        if (q[i] < 0) {
          q[i] = 0;
          nh[i] = 0;
        } else if (q[i] > 0)
          nh[i] = q[i];
        else
          nh[i] = 2 - sbo;
      }
      if (sbo > 2)
        q[i] = sbo - 2;
      nh[i] = 2 - sbo + q[i];
      break;
    case JME.AN_S:
    case JME.AN_SE: // Se rozoznava kvoli aromaticite
      // multibond handling nh zmenene v 2002.12
      if (sbo < 2)
        nh[i] = 2 - sbo + q[i];
      //else if (sbo == 2) {if(q[i] < 0) q[i] = 0; nh[i] = 2 - sbo + q[i];}
      //else if (sbo == 2) {q[i] = 0; nh[i] = 2 - sbo;} // 2002.12
      else if (sbo == 2) {
        if (q[i] < 0) {
          q[i] = 0;
          nh[i] = 0;
        } else if (q[i] > 0)
          nh[i] = q[i];
        else
          nh[i] = 2 - sbo;
      } else if (sbo == 3) {
        // >S- ma byt S+, -S= ma byt SH
        if (nv[i] == 2) {
          q[i] = 0;
          nh[i] = 1;
        } else {
          q[i] = 1;
          nh[i] = 0;
        }
      } else if (sbo == 4) {
        q[i] = 0;
        nh[i] = 0;
      } else if (sbo == 5) {
        q[i] = 0;
        nh[i] = 1;
      } else {
        q[i] = sbo - 6;
        nh[i] = 0;
      }
      break;
    case JME.AN_F:
    case JME.AN_CL:
    case JME.AN_BR:
    case JME.AN_I:
      if (sbo >= 1)
        q[i] = sbo - 1;
      nh[i] = 1 - sbo + q[i];
      // potialto org, odteraz zmena kvoli superhalogenom
      if (sbo > 2) {
        q[i] = 0;
        nh[i] = 0;
      }
      break;
    case JME.AN_R:
    case JME.AN_X:
      nh[i] = 0;
      break;
    }
    if (nh[i] < 0)
      nh[i] = 0; // to be sure

    // 2006.09 removes tag if not stereo atom
    if (jme.relativeStereo && atag[i] != null && atag[i].length() > 0) {
      boolean ok = false;
      for (int j = 1; j <= nv[i]; j++) {
        int bond = bondIdentity(i, v[i][j]);
        if (i == va[bond] && (stereob[bond] == UP || stereob[bond] == DOWN)) {
          ok = true;
          break;
        }
        if (i == vb[bond] && (stereob[bond] == XUP || stereob[bond] == XDOWN)) {
          ok = true;
          break;
        }
      }
      if (!ok)
        atag[i] = "";
    }
  }

  // ----------------------------------------------------------------------------
  void changeCharge(int atom, int type) {
    // click with +/- on atom
    // 2002.05 --- pridana moznost C+ 
    // 2005.03 --- pridany type, moze byt 0 1 -1
    String np = "Charge change not possible on ";

    // for  webme
    if (type == 1) {
      q[atom]++;
      return;
    } else if (type == -1) {
      q[atom]--;
      return;
    }

    // standard jme behaviour
    int sbo = sumBondOrders(atom);
    if (sbo == -1) { // query
      if (type == 0) {
        if (q[atom] == 0)
          q[atom] = 1;
        else if (q[atom] == 1)
          q[atom] = -1;
        else if (q[atom] == -1)
          q[atom] = 0;
      }
    }
    switch (an[atom]) {
    case JME.AN_B:
      if (sbo > 2)
        jme.info(np + "this boron !");
      if (q[atom] == 0)
        q[atom] = 1;
      else if (q[atom] == 1)
        q[atom] = 0;
      break;
    case JME.AN_C:
      //case JME.AN_SI:
      if (sbo > 3)
        jme.info(np + "this carbon !");
      else if (sbo < 4) {
        if (q[atom] == 0)
          q[atom] = -1;
        else if (q[atom] == -1)
          q[atom] = 1;
        else if (q[atom] == 1)
          q[atom] = 0;
      }
      break;
    case JME.AN_N:
    case JME.AN_P:
      if (sbo > 3)
        jme.info(np + "multibonded N or P !");
      else if (sbo == 3 && q[atom] == 0)
        q[atom] = 1;
      else if (sbo == 3 && q[atom] == 1)
        q[atom] = 0;
      else if (sbo < 3 && q[atom] == 0)
        q[atom] = 1;
      else if (sbo < 3 && q[atom] == 1)
        q[atom] = -1;
      else if (sbo < 3 && q[atom] == -1)
        q[atom] = 0;
      break;
    case JME.AN_O: // -[O-] -O- =O -[O+]< >[O2+]< ...
    case JME.AN_S: // asi sa na multivalent vykaslat
    case JME.AN_SE:
      if (sbo > 2)
        jme.info(np + "multibonded O or S !");
      else if (sbo == 2 && q[atom] == 0)
        q[atom] = 1;
      else if (sbo == 2 && q[atom] == 1)
        q[atom] = 0;
      else if (sbo < 2 && q[atom] == 0)
        q[atom] = -1;
      else if (sbo < 2 && q[atom] == -1)
        q[atom] = 1;
      else if (sbo < 2 && q[atom] == 1)
        q[atom] = 0;
      break;
    case JME.AN_F:
    case JME.AN_CL:
    case JME.AN_BR:
    case JME.AN_I:
      if (sbo == 0 && q[atom] == 0)
        q[atom] = -1;
      else if (sbo == 0 && q[atom] == -1)
        q[atom] = 0;
      else
        jme.info(np + "the halogen !");
      break;
    case JME.AN_X:
      jme.info("Use X button to change charge on the X atom !");
      break;
    }

  }

  // ----------------------------------------------------------------------------
  int sumBondOrders(int atom) {
    // sum of bond orders k nonhydrogen atoms
    int sbo = 0;
    for (int i = 1; i <= nv[atom]; i++) {
      int bond = bondIdentity(atom, v[atom][i]);
      if (isSingle(bond))
        sbo += 1;
      else if (isDouble(bond))
        sbo += 2;
      else if (nasv[bond] == TRIPLE)
        sbo += 3;
      else if (nasv[bond] == QUERY)
        return -1; // query bond
      //else System.out.println("bond "+bond+" inconsistent info!"+nasv[bond]);
    }
    return sbo;
  }

  // ----------------------------------------------------------------------------
  // 2009.04 updated to take number also from keys (not only autonumbering)
  // in JME is currentMark and markUsed
  // 2009.09 added autonumber for compatibility with previous versions
  // 2010.01 - unified number + autonumber
  // maxMark is updated only in autonumber
  public void mark() {
    // markuje touchedAtom
    jme.markUsed = true;

    // star marking of a part of a molecules
    if (jme.star) {
      doColoring = -1;
      if (abg[touchedAtom] == 0)
        abg[touchedAtom] = 4;
      else
        abg[touchedAtom] = 0;
      return;
    }

    // normal number marking
    // checking whether this atom marked
    for (int i = 1; i <= nmarked; i++) {
      if (touchedAtom == mark[i][0]) {
        if (jme.currentMark == -1) {
          // removing this marking
          for (int j = i; j < nmarked; j++) {
            mark[j][0] = mark[j + 1][0];
            mark[j][1] = mark[j + 1][1];
          }
          nmarked--;
        } else {
          // replacing the current mark
          int n = jme.currentMark;
          if (jme.autonumber) {
            if (!jme.mouseShift)
              maxMark++;
            n = maxMark;
          }
          mark[i][1] = n;
        }
        return; // done
      }
    }

    // atom without mark clicked, need to check for storage
    int storage = mark.length;
    if (++nmarked > storage - 1) {
      int n_m[][] = new int[storage + 5][2];
      System.arraycopy(mark, 0, n_m, 0, mark.length);
      mark = n_m;
    }
    mark[nmarked][0] = touchedAtom;
    int n = jme.currentMark;
    if (jme.autonumber) {
      if (!jme.mouseShift)
        maxMark++;
      n = maxMark;
    }
    mark[nmarked][1] = n;

  }

  // ----------------------------------------------------------------------------
  public void numberAtoms() {
    nmarked = 0; // vymaze marky
    maxMark = 0; // vymaze marky
    createSmiles(); // canonize
    for (int i = 1; i <= natoms; i++) {
      touchedAtom = i;
      mark();
    }
    touchedAtom = 0;
  }

  // ----------------------------------------------------------------------------
  public void setLabel(int n, String s) {
    if (label == null || label.length < natoms + 1)
      label = new String[natoms + 1];
    label[n] = s;
  }

  // ----------------------------------------------------------------------------
  public int reactionPart() {
    // returns 1 if reactant, 2 agent, 3 product
    double center[] = new double[4];
    centerPoint(center);
    int xpix = jme.dimension.width;
    if (!jme.depict)
      xpix -= jme.sd;
    if (center[0] < xpix / 2 - jme.arrowWidth / 2)
      return 1;
    else if (center[0] > xpix / 2 + jme.arrowWidth / 2)
      return 3;
    else
      return 2;
  }

  // ----------------------------------------------------------------------------
  private static long[] generatePrimes(int n) {
    /*
    Prime Number Generator
    code by Mark Chamness (modified slightly by Peter Ertl)
    This subroutine calculates first n prime numbers (starting with 2)
    It stores the first 100 primes it generates. Then it evaluates the rest
    based on those up to prime[100] squared
    */
    int npn;
    long[] pn = new long[n + 2];
    int[] prime = new int[100];
    int test = 5, index = 0;
    int num = 0;
    boolean check = true;
    prime[0] = 3;
    pn[1] = 2;
    pn[2] = 3;
    npn = 2;
    if (n < 3)
      return pn; // very rear case
    while (test < (prime[num] * prime[num])) {
      index = 0;
      check = true;
      while (check == true && index <= num
          && test >= (prime[index] * prime[index])) {
        if (test % prime[index] == 0)
          check = false;
        else
          index++;
      }
      if (check == true) {
        pn[++npn] = test;
        if (npn >= n)
          return pn;
        if (num < (prime.length - 1)) {
          num++;
          prime[num] = test;
        }
      }
      test += 2;
    }
    System.err.println("ERROR - Prime Number generator failed !");
    return pn;
  }
  // ----------------------------------------------------------------------------


}
// ----------------------------------------------------------------------------
