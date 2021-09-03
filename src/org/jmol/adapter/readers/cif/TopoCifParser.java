package org.jmol.adapter.readers.cif;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.adapter.readers.cif.CifReader.Parser;
import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.Bond;
import org.jmol.api.SymmetryInterface;
import org.jmol.symmetry.SymmetryOperation;
import org.jmol.util.Edge;
import org.jmol.util.Logger;

import javajs.api.GenericCifDataParser;
import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.M4;
import javajs.util.P3;
import javajs.util.T3;

/**
 * see https://github.com/COMCIFS/TopoCif
 * 
 * Basic idea:
 * 
 * We have TLinks, TNodes, and TAtoms
 * 
 * TLinks each have two TNodes and may also be associated with bridging TAtom
 * sets.
 * 
 * TNode extends TAtom and may also maintain a list of TAtoms.
 * 
 * TAtoms extend Atom and may have symmetry aspects.
 * 
 * 
 * 
 * 
 * 
 * 
 * @author Bob Hanson hansonr@stolaf.edu 2020.11.17 2021.05.07
 * 
 */
public class TopoCifParser implements Parser {

  final static int TOPOL_LINK = 0x1000000;
  final static int TOPOL_GROUP = 0x2000000;
  final static int TOPOL_NODE = 0x4000000;

  /**
   * types set by filter TOPOSE_TYPES in the format of one or more of {v, vw,
   * hb} separated by "+"; default is v+hb
   */
//  private final static String ` = "+v+hb+w+";
  public static final int LINK_TYPE_SING   = 1;
  public static final int LINK_TYPE_DOUB   = 2;
  public static final int LINK_TYPE_TRIP   = 3;
  public static final int LINK_TYPE_QUAD   = 4;
  public static final int LINK_TYPE_QUIN   = 5;
  public static final int LINK_TYPE_HEX    = 6;
  public static final int LINK_TYPE_HEPT   = 7;
  public static final int LINK_TYPE_OCT    = 8;
  public static final int LINK_TYPE_AROM   = 9;
  public static final int LINK_TYPE_POLY   = 0xA;
  public static final int LINK_TYPE_DELO   = 0xB;
  public static final int LINK_TYPE_PI     = 0xC;
  public static final int LINK_TYPE_HBOND  = 0xD;
  public static final int LINK_TYPE_VDW    = 0xE;
  public static final int LINK_TYPE_OTHER  = 0xF;

  public static String linkTypes =
        "?  "
      + "SIN"
      + "TRI"
      + "QUA"
      + "QUI"
      + "HEX"
      + "HEP"
      + "OCT"
      + "ARO"
      + "POL"
      + "PI "
      + "HBO"
      + "VDW"; // also "W"

  static int getBondType(String type) {
    type = type.toUpperCase();
    if (type.charAt(0) == 'V')
      return (type.length() == 1 ? LINK_TYPE_SING : LINK_TYPE_VDW);
    if (type.length() > 3) 
      type = type.substring(0, 3);
    return Math.max(1, linkTypes.indexOf(type)/3);
  }


  public static final int LINK_TYPE_BITS = 4;

  static double ERROR_TOLERANCE = 0.001;

  /**
   * reader will be null if filter includes TOPOS_IGNORE
   */
  CifReader reader;

  /**
   * list of _topol_node block data
   */
  Lst<TAtom> atoms = new Lst<TAtom>();

  /**
   * list of _topol_node block data
   */
  Lst<TNode> nodes = new Lst<TNode>();

  /**
   * list of _topol_link block data
   */
  private Lst<TLink> links = new Lst<TLink>();

  /**
   * list of _topol_net block data
   */
  Lst<TNet> nets = new Lst<TNet>();

  TNet singleNet;
  
  int netCount;
  int linkCount;
  int atomCount;

  T3 temp1 = new P3(), temp2 = new P3();

  private int ac0 = -1, bc0;

  private GenericCifDataParser cifParser;

  /**
   * and indictor that we should abort, and why
   */
  String failed;

  /**
   * symmetry operations for this space group
   * 
   */
  M4[] ops;

  /**
   * base atom index to be added to any atom bitsets
   */
  int i0;

  /**
   * base bond index to be added to any bond bitsets
   */
  int b0;
  private String allowedTypes;
 
  String netNotes = "";
  
  // CifParser supports up to 100 fields
  final private static String[] topolFields = {
      /*0*/"_topol_net_id",
      /*1*/"_topol_net_label",
      /*2*/"_topol_link_id",
      /*3*/"_topol_link_net_id",
      /*4*/"_topol_link_node_id_1",
      /*5*/"_topol_link_node_id_2",
      /*6*/"_topol_link_node_label_1",
      /*7*/"_topol_link_node_label_2",
      /*8*/"_topol_link_atom_label_1",
      /*9*/"_topol_link_atom_label_2",
      /*10*/"_topol_link_symop_1",
      /*11*/"_topol_link_translation_1",
      /*12*/"_topol_link_translation_1_x",
      /*13*/"_topol_link_translation_1_y",
      /*14*/"_topol_link_translation_1_z",
      /*15*/"_topol_link_symop_2",
      /*16*/"_topol_link_translation_2",
      /*17*/"_topol_link_translation_2_x",
      /*18*/"_topol_link_translation_2_y",
      /*19*/"_topol_link_translation_2_z",
      /*20*/"_topol_link_distance",
      /*21*/"_topol_link_type",
      /*22*/"_topol_link_multiplicity",
      /*23*/"_topol_link_voronoi_solidangle",
      /*24*/"_topol_link_order",
      /*25*/"_topol_node_id",
      /*26*/"_topol_node_net_id",
      /*27*/"_topol_node_label",
      /*28*/"_topol_node_atom_label",
      /*29*/"_topol_node_symop",
      /*30*/"_topol_node_translation",
      /*31*/"_topol_node_translation_x",
      /*32*/"_topol_node_translation_y",
      /*33*/"_topol_node_translation_z",
      /*34*/"_topol_node_fract_x",
      /*35*/"_topol_node_fract_y",
      /*36*/"_topol_node_fract_z",
      /*37*/"_topol_node_chemical_formula_sum",
      /*38*/"_topol_atom_id",
      /*39*/"_topol_atom_atom_label",
      /*40*/"_topol_atom_node_id",
      /*41*/"_topol_atom_link_id",
      /*42*/"_topol_atom_symop",
      /*43*/"_topol_atom_translation",
      /*44*/"_topol_atom_translation_x",
      /*45*/"_topol_atom_translation_y",
      /*46*/"_topol_atom_translation_z",
      /*47*/"_topol_atom_fract_x",
      /*48*/"_topol_atom_fract_y",
      /*49*/"_topol_atom_fract_z",
      /*50*/"_topol_atom_element_symbol",
      /*51*/"_topol_link_site_symmetry_symop_1",
      /*52*/"_topol_link_site_symmetry_translation_1_x",
      /*53*/"_topol_link_site_symmetry_translation_1_y",
      /*54*/"_topol_link_site_symmetry_translation_1_z",
      /*55*/"_topol_link_site_symmetry_symop_2",
      /*56*/"_topol_link_site_symmetry_translation_2_x",
      /*57*/"_topol_link_site_symmetry_translation_2_y",
      /*58*/"_topol_link_site_symmetry_translation_2_z",
      /*59*/"_topol_link_site_symmetry_translation_1",
      /*60*/"_topol_link_site_symmetry_translation_2",
  };

  private final static byte topol_net_id = 0;
  private final static byte topol_net_label = 1;
  private final static byte topol_link_id = 2;
  private final static byte topol_link_net_id = 3;
  private final static byte topol_link_node_id_1 = 4;
  private final static byte topol_link_node_id_2 = 5;
  private final static byte topol_link_node_label_1 = 6;
  private final static byte topol_link_node_label_2 = 7;
  private final static byte topol_link_atom_label_1 = 8;
  private final static byte topol_link_atom_label_2 = 9;
  private final static byte topol_link_symop_1 = 10;
  private final static byte topol_link_translation_1 = 11;
  private final static byte topol_link_translation_1_x = 12;
  private final static byte topol_link_translation_1_y = 13;
  private final static byte topol_link_translation_1_z = 14;
  private final static byte topol_link_symop_2 = 15;
  private final static byte topol_link_translation_2 = 16;
  private final static byte topol_link_translation_2_x = 17;
  private final static byte topol_link_translation_2_y = 18;
  private final static byte topol_link_translation_2_z = 19;
  private final static byte topol_link_distance = 20;
  private final static byte topol_link_type = 21;
  private final static byte topol_link_multiplicity = 22;
  private final static byte topol_link_voronoi_solidangle = 23;
  private final static byte topol_link_order = 24;
  private final static byte topol_node_id = 25;
  private final static byte topol_node_net_id = 26;
  private final static byte topol_node_label = 27;
  private final static byte topol_node_atom_label = 28;
  private final static byte topol_node_symop = 29;
  private final static byte topol_node_translation = 30;
  private final static byte topol_node_translation_x = 31;
  private final static byte topol_node_translation_y = 32;
  private final static byte topol_node_translation_z = 33;
  private final static byte topol_node_fract_x = 34;
  private final static byte topol_node_fract_y = 35;
  private final static byte topol_node_fract_z = 36;
  private final static byte topol_node_chemical_formula_sum = 37;
  private final static byte topol_atom_id = 38;
  private final static byte topol_atom_atom_label = 39;
  private final static byte topol_atom_node_id = 40;
  private final static byte topol_atom_link_id = 41;
  private final static byte topol_atom_symop = 42;
  private final static byte topol_atom_translation = 43;
  private final static byte topol_atom_translation_x = 44;
  private final static byte topol_atom_translation_y = 45;
  private final static byte topol_atom_translation_z = 46;
  private final static byte topol_atom_fract_x = 47;
  private final static byte topol_atom_fract_y = 48;
  private final static byte topol_atom_fract_z = 49;
  private final static byte topol_atom_element_symbol = 50;
  private final static byte topol_link_site_symmetry_symop_1 = 51;
  private final static byte topol_link_site_symmetry_translation_1_x = 52;
  private final static byte topol_link_site_symmetry_translation_1_y = 53;
  private final static byte topol_link_site_symmetry_translation_1_z = 54;
  private final static byte topol_link_site_symmetry_symop_2 = 55;
  private final static byte topol_link_site_symmetry_translation_2_x = 56;
  private final static byte topol_link_site_symmetry_translation_2_y = 57;
  private final static byte topol_link_site_symmetry_translation_2_z = 58;
  private final static byte topol_link_site_symmetry_translation_1 = 59;
  private final static byte topol_link_site_symmetry_translation_2 = 60;

  public TopoCifParser() {
  }

  /**
   * filter "TOPOS_TYPES=hb" will only load hydrogen bonds; options include v,
   * vw, and hb
   */
  @Override
  public TopoCifParser setReader(CifReader reader) {
    if (!reader.checkFilterKey("TOPOL")) {
      reader.appendLoadNote("This file has Topology analysis records. Use LOAD \"\" {1 1 1} FILTER \"TOPOL\"  to load the topology.");
      return this;
    }
    this.reader = reader;
    String types = reader.getFilter("TOPOS_TYPES=");
    if (types == null)
      types = reader.getFilter("TOPOS_TYPE=");
    if (types != null && types.length() > 0) {
      types = "+" + types.toLowerCase() + "+";
      allowedTypes = types;
    }
//    reader.asc.setNoAutoBond();
    i0 = reader.baseAtomIndex;
    b0 = reader.baseBondIndex;
    return this;
  }

  @Override
  public boolean processBlock(String key) throws Exception {
    if (reader == null || failed != null) {
      return false;
    }
    if (ac0 < 0) {
      ac0 = reader.asc.ac;
      bc0 = reader.asc.bondCount;
    }
    if (reader.ucItems != null) {
      reader.allow_a_len_1 = true;
      for (int i = 0; i < 6; i++)
        reader.setUnitCellItem(i, reader.ucItems[i]);
    }
    reader.parseLoopParameters(topolFields);
    cifParser = reader.cifParser;
    if (key.startsWith("_topol_net")) {
      processNets();
    } else if (key.startsWith("_topol_link")) {
      processLinks();
    } else if (key.startsWith("_topol_node")) {
      processNodes();
    } else if (key.startsWith("_topol_atom")) {
      processAtoms();
    }
    return true;
  }

  /**
   * Process all nets. Note that tne nets list is self-populating with a "Net1"
   * value if there is no TOPOL_NET section.
   * 
   * @throws Exception
   */
  private void processNets() throws Exception {
    while (cifParser.getData()) {
      int id = getInt(getField(topol_net_id));
      if (id < 0)
        id = 0;
      String netLabel = getField(topol_net_label);
      TNet net = getNetFor(id, netLabel);
      net.line = reader.line;
    }
  }

  private void processLinks() throws Exception {
    while (cifParser.getData()) {
      String type = ("" + getField(topol_link_type)).toLowerCase();
      if (allowedTypes != null && allowedTypes.indexOf("+" + type + "+") < 0)
        continue;
      TLink link = new TLink();
      link.type = type;
      int[] t1 = new int[3];
      int[] t2 = new int[3];
      int n = cifParser.getColumnCount();
      for (int i = 0; i < n; ++i) {
        int p = reader.fieldProperty(i);
        String field = reader.field;
        switch (p) {
        case topol_link_id:
          link.id = getInt(field);
          break;
        case topol_link_net_id:
          int id = getInt(field);
          if (id == Integer.MIN_VALUE) {
            // original
            link.netLabel = field;
          } else {
            link.netID = getInt(field);
          }
          break;
        case topol_link_node_id_1:
          link.id1 = getInt(field);
          break;
        case topol_link_node_id_2:
          link.id2 = getInt(field);
          break;
        case topol_link_node_label_1:
        case topol_link_atom_label_1: // 
          if (link.label1 == null)
            link.label1 = field;
          break;
        case topol_link_atom_label_2:
        case topol_link_node_label_2:
          if (link.label2 == null)
            link.label2 = field;
          break;
        case topol_link_site_symmetry_symop_1:
        case topol_link_symop_1:
          link.symop1 = getInt(field) - 1;
          break;
        case topol_link_site_symmetry_symop_2:
        case topol_link_symop_2:
          link.symop2 = getInt(field) - 1;
          break;
        case topol_link_order:
          link.topoOrder = getInt(field);
          break;
        case topol_link_site_symmetry_translation_1:
        case topol_link_site_symmetry_translation_1_x:
        case topol_link_site_symmetry_translation_1_y:
        case topol_link_site_symmetry_translation_1_z:
        case topol_link_translation_1:
        case topol_link_translation_1_x:
        case topol_link_translation_1_y:
        case topol_link_translation_1_z:
          t1 = processTranslation(p, t1, field);
          break;
        case topol_link_site_symmetry_translation_2:
        case topol_link_site_symmetry_translation_2_x:
        case topol_link_site_symmetry_translation_2_y:
        case topol_link_site_symmetry_translation_2_z:
        case topol_link_translation_2:
        case topol_link_translation_2_x:
        case topol_link_translation_2_y:
        case topol_link_translation_2_z:
          t2 = processTranslation(p, t2, field);
          break;
        case topol_link_distance:
          link.cartesianDistance = getFloat(field);
          break;
        case topol_link_multiplicity:
          link.multiplicity = getInt(field);
          break;
        case topol_link_voronoi_solidangle:
          link.voronoiAngle = getFloat(field);
        }
      }
      if (!link.setLink(t1, t2, reader.line)) {
        failed = "invalid link! " + link;
        return;
      }
      links.addLast(link);
    }
  }

  private int[] processTranslation(int p, int[] t, String field) {
    switch (p) {
    case topol_link_site_symmetry_translation_1:
    case topol_link_site_symmetry_translation_2:
    case topol_link_translation_1:
    case topol_link_translation_2:
    case topol_node_translation:
    case topol_atom_translation:
      t = Cif2DataParser.getIntArrayFromStringList(field, 3);
      break;
    case topol_link_site_symmetry_translation_1_x:
    case topol_link_site_symmetry_translation_2_x:
    case topol_link_translation_1_x:
    case topol_link_translation_2_x:
    case topol_node_translation_x:
    case topol_atom_translation_x:
      t[0] = getInt(field);
      break;
    case topol_link_site_symmetry_translation_1_y:
    case topol_link_site_symmetry_translation_2_y:
    case topol_link_translation_1_y:
    case topol_link_translation_2_y:
    case topol_node_translation_y:
    case topol_atom_translation_y:
      t[1] = getInt(field);
      break;
    case topol_link_site_symmetry_translation_1_z:
    case topol_link_site_symmetry_translation_2_z:
    case topol_link_translation_1_z:
    case topol_link_translation_2_z:
    case topol_node_translation_z:
    case topol_atom_translation_z:
      t[2] = getInt(field);
      break;
    }
    return t;
  }

  private void processNodes() throws Exception {
    while (cifParser.getData()) {
      TNode node = new TNode();
      int[] t = new int[3];
      int n = cifParser.getColumnCount();
      for (int i = 0; i < n; ++i) {
        int p = reader.fieldProperty(i);
        String field = reader.field;
        switch (p) {
        case topol_node_id:
          node.id = getInt(field);
          break;
        case topol_node_label:
          node.label = field;
          break;
        case topol_node_net_id:
          node.netID = getInt(field);
          break;
        case topol_node_atom_label:
          node.atomLabel = field;
          break;
        case topol_node_symop:
          node.symop = getInt(field) - 1;
          break;
        case topol_node_translation:
        case topol_node_translation_x:
        case topol_node_translation_y:
        case topol_node_translation_z:
          t = processTranslation(p, t, field);
          break;
        case topol_node_chemical_formula_sum:
          node.formula = field;
          break;
        case topol_node_fract_x:
          node.x = getFloat(field);
          break;
        case topol_node_fract_y:
          node.y = getFloat(field);
          break;
        case topol_node_fract_z:
          node.z = getFloat(field);
          break;
        }
      }
      if (node.setNode(t, reader.line))
        nodes.addLast(node);
    }
  }

  private void processAtoms() throws Exception {
    while (cifParser.getData()) {
      TAtom atom = new TAtom();
      int[] t = new int[3];
      int n = cifParser.getColumnCount();
      for (int i = 0; i < n; ++i) {
        int p = reader.fieldProperty(i);
        String field = reader.field;
        switch (p) {
        case topol_atom_id:
          atom.id = getInt(field);
          break;
        case topol_atom_atom_label:
          atom.atomLabel = field;
          break;
        case topol_atom_node_id:
          atom.nodeID = getInt(field);
          break;
        case topol_atom_link_id:
          atom.linkID = getInt(field);
          break;
        case topol_atom_symop:
          atom.symop = getInt(field) - 1;
          break;
        case topol_atom_translation:
        case topol_atom_translation_x:
        case topol_atom_translation_y:
        case topol_atom_translation_z:
          t = processTranslation(p, t, field);
          break;
        case topol_atom_fract_x:
          atom.x = getFloat(field);
          break;
        case topol_atom_fract_y:
          atom.y = getFloat(field);
          break;
        case topol_atom_fract_z:
          atom.z = getFloat(field);
          break;
        case topol_atom_element_symbol:
          atom.formula = field;
          break;
        }
      }
      if (atom.setAtom(t, reader.line))
        atoms.addLast(atom);
    }
  }

  /**
   * PRIOR to symmetry application, process all internal symop/translation aspects.
   */
  @Override
  public boolean finalizeReader() throws Exception {
    // opportunity to handle anything prior to applying symmetry
    if (reader == null || reader.symops == null)
      return false;
    cifParser = null;
    reader.applySymmetryToBonds = true;
    // finalize all topol_atom symmetries
    M4[] ops = getOperations();
    for (int i = 0; i < atoms.size(); i++) {
      atoms.get(i).finalizeAtom(ops);
    }
    
    SymmetryInterface sym = reader.getSymmetry();
    // we do not have to finalize nodes directly -- finalizeLink will take care of that.
    for (int i = 0; i < links.size(); i++) {
      links.get(i).finalizeLink(ops, sym);
    }
    if (reader.doApplySymmetry)
      reader.applySymmetryAndSetTrajectory();
    return true;
  }

  private M4[] getOperations() {
    Lst<String> symops = reader.symops;
    int nOps = symops.size();
    ops = new M4[nOps];
    for (int i = 0; i < nOps; i++) {
      ops[i] = SymmetryOperation.getMatrixFromXYZ("!" + symops.get(i)); 
    }   
    return ops;
  }

  /**
   * Symmetry has been applied. Identify all of the connected atoms and process the group associations
   * 
   */
  @Override
  public void finalizeSymmetry(boolean haveSymmetry) throws Exception {
    if (reader == null || !haveSymmetry || links.size() == 0)
      return;
    
    BS bsConnected = new BS(); // atoms that are linked
    BS bsAtoms = new BS(); // atoms that are associated or connected;
    int nLinks = processAssociations(bsConnected, bsAtoms);
    bsAtoms.or(bsConnected);
    // create the excluded atoms set -- atoms of bsAtoms that are linked 
    BS bsExclude = new BS();
    for (int pt = 0, i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
      while (bsAtoms.get(i)) {
        bsExclude.setBitTo(pt++, bsConnected.get(i++));
      }
    }
    // If we have a network, remove all unconnected atoms.
    if (bsConnected.cardinality() > 0) {
      reader.asc.bsAtoms = bsAtoms;
      reader.asc.atomSetInfo.put("bsExcludeBonding", bsExclude);
    }
    reader.appendLoadNote("TopoCifParser created " + bsConnected.cardinality() + " nodes and " + nLinks + " links");

    // add model info
    Lst<Map<String, Object>> info = new Lst<Map<String, Object>>();
    for (int i = 0, n = links.size(); i < n; i++) {
      info.addLast(links.get(i).getInfo());
    }
    reader.asc.setCurrentModelInfo("topology", info);
    String script = "display displayed or " + nets.get(0).label + "__*";
    reader.addJmolScript(script);
    for (int i = 0; i < nets.size(); i++) {
      nets.get(i).setNote();
    }
  }

  /**
   * Find and process all "bonds" associated with all links and nodes. 
   * 
   * BOND_LINK + index indicates linked nodes
   * 
   * BOND_GROUP + index indicates associated nodes
   * 
   * 
   * @param bsConnected prevent Jmol from adding bonds to this atom 
   * @param bsAtoms  allow Jmol to add bonds to these atoms, inclusively
   * 
   * @return number of bonds created
   */
  private int processAssociations(BS bsConnected, BS bsAtoms) {

    int nlinks = 0;
    Atom[] atoms = reader.asc.atoms;
   
    // set associations for links and nodes 
    for (int i = reader.asc.ac; --i >= ac0;) {
      Atom a = atoms[i];
      int idx = a.sequenceNumber;
      if (idx == Integer.MIN_VALUE || idx == 0)
        continue;
      if (idx > 0) {
        TNode node = getAssociatedNodeByIdx(idx - 1);
        if (node.bsAtoms == null)
          node.bsAtoms = new BS();
        node.bsAtoms.set(i0 + a.index);
      } else {
        TLink link = getAssoiatedLinkByIdx(-idx - 1);
        if (link.bsAtoms == null)
          link.bsAtoms = new BS();
        link.bsAtoms.set(i0 + a.index);
      }
      bsAtoms.set(a.index);
    }  

    // finish up with bonds
    Bond[] bonds = reader.asc.bonds;
    for (int i = reader.asc.bondCount; --i >= bc0;) {
      Bond b = bonds[i];
//      if (!isEqualD(atoms[b.atomIndex1], atoms[b.atomIndex2], b.distance)) {
//        bonds[i] = null;
//        bsAtoms.set(b.atomIndex1);
//        bsAtoms.set(b.atomIndex2);
//        continue;
//      }
      if (b.order >= TOPOL_GROUP) {
        // associated atoms - don't show this bond
        bonds[i] = null;
      } else if (b.order >= TOPOL_LINK) {
        // adjust link bond order, and add this bond to the link's bsBonds bitset
        b.order -= TOPOL_LINK;
        int id = b.order >> LINK_TYPE_BITS;
        TLink link = getLinkById(id);
        link.bsBonds.set(b0 + i);
        switch (b.order & 0xF) {
        default:
          b.order = Edge.BOND_COVALENT_SINGLE;
          break;
        case LINK_TYPE_DOUB:
          b.order = Edge.BOND_COVALENT_DOUBLE;
          break;
        case LINK_TYPE_TRIP:
          b.order = Edge.BOND_COVALENT_TRIPLE;
          break;
        case LINK_TYPE_QUAD:
          b.order = Edge.BOND_COVALENT_QUADRUPLE;
          break;
        case LINK_TYPE_QUIN:
          b.order = Edge.BOND_COVALENT_QUINTUPLE;
          break;
        case LINK_TYPE_HEX:
          b.order = Edge.BOND_COVALENT_sextuple;
          break;
        case LINK_TYPE_POLY:
          b.order = Edge.BOND_COVALENT_SINGLE;
          break;
        case LINK_TYPE_DELO:
        case LINK_TYPE_PI:
          b.order = Edge.BOND_AROMATIC;
          break;
        case LINK_TYPE_HBOND:
          b.order = Edge.BOND_H_REGULAR;
          break;
        case LINK_TYPE_VDW:
          b.order = Edge.BOND_PARTIAL01;
          break;
        } 
        bsConnected.set(b.atomIndex1);
        bsConnected.set(b.atomIndex2);
        nlinks++;
      }
    }    
    return nlinks;
  }

//  static boolean isEqualD(T3 p1, T3 p2, double d) {
//    return (Double.isNaN(d) || Math.abs(p1.distance(p2) - d) < ERROR_TOLERANCE);
//  }
//
  private String getField(byte field) {
    String f = reader.getField(field);
    return ("\0".equals(f) ? null : f);
  }

  private int getInt(String f) {
    return (f == null ? Integer.MIN_VALUE : reader.parseIntStr(f));
  }

  private float getFloat(String f) {
    return (f == null ? Float.NaN : reader.parseFloatStr(f));
  }

  @Override
  public void ProcessRecord(String key, String data) {
    if (key.startsWith("_topol_net")) {
      if (key.endsWith("_id")) {
        int n = Integer.parseInt(data);
        if (singleNet == null) {
          nets.addLast(singleNet = new TNet(netCount++, n, "Net" + n));
        } else {
          singleNet.id = n;
        }
      } else if (key.endsWith("_label")) {
        if (singleNet == null) {
          nets.addLast(singleNet = new TNet(netCount++, 1, data));
        } else {
          singleNet.label = data;
        }
      }
    }
  }

  @Override
  public Object clone() {
    try {
      return super.clone();
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }
  
  private class TNet {
    @SuppressWarnings("unused")
    String line;
    @SuppressWarnings("unused")
    int idx;
    int id;
    int nLinks, nNodes;
    String label;
    public boolean hasAtoms;

    TNet(int index, int id, String label) {
      idx = index;
      this.id = id;
      this.label = label;
    }

    void setNote() {
      String netKey = "," + id + ",";
      if (netNotes.indexOf(netKey) < 0) {
        reader.appendLoadNote(
            "Net " + label + " created with " + nLinks + " links and " + nNodes + " nodes.\n"
            + "Use DISPLAY " +  (hasAtoms ? label + "__* to display it without associated atoms or " + label 
                + "_* to display it with its associated atoms" : label + "** to display it"
                +"")
            );
      }
    }
  }

  private class TAtom extends Atom {

    @SuppressWarnings("unused")
    int id;
    
    String atomLabel;
    int nodeID;
    int linkID;
    int symop = 0;
    private P3 trans = new P3();
    String formula;

    Atom atom;
    private boolean isFinalized;
    @SuppressWarnings("unused")
    int idx;

    @SuppressWarnings("unused")
    String line;
    
    TAtom() {
      super();
    }

    boolean setAtom(int[] a, String line) {
      this.line = line;
      if (Float.isNaN(x) != Float.isNaN(y) || Float.isNaN(y) != Float.isNaN(z)) 
        return false;
      idx = atomCount++;
      if (Float.isNaN(x)) {
        trans = P3.new3(a[0], a[1], a[2]);        
      } else {
        symop = 0;
      }
      if (formula != null && formula.indexOf(" ") < 0) {
        atomName = formula;
        getElementSymbol();
        if (!formula.equals(elementSymbol))
          elementSymbol = "Z";
      }
      atomName = atomLabel;
      return true;
    }

    void finalizeAtom(M4[] ops) throws Exception {
      if (isFinalized)
        return;
      isFinalized = true;
      Atom a = atom;
      if (a == null && atomLabel != null) {
        a = reader.asc.getAtomFromName(atomLabel);
      }
      setElementSymbol(this, formula);
      if (a == null && Float.isNaN(x)) {
        // no associated atom
        throw new Exception("TopoCIFParser.finalizeAtom no atom " + atomLabel + " line=" + line);
      }

      // check for addition to a TNode
      TNode node = null;
      if (nodeID > 0) {
        node = getNodeById(nodeID, -1, null);
        //      else if (atomLabel != null)
        //        node = (TNode) getTopo(nodes, atomLabel, 'n');
        if (node != null) {
          node.addAtom(this);
        }
      }

      // check for addition to a TLink
      TLink link = null;
      if (linkID > 0) {
        link = getLinkById(linkID);
        if (link != null)
          link.addAtom(this);
      }
      if (node == null && link == null) {
        System.out.println("TAtom " + this + " ignored");
        return;
      }

      // set the symmetry op [tx ty tz]
      if (a != null && (a == atom || Float.isNaN(x))) {
        setTAtom(a, this);
        applySymmetry(this, ops, symop, trans);
      }

      // add this atom to the AtomSetCollection
//      if (node != null && node.atomLabel == null) {
//        node.atomLabel = node.atomName = node.netLabel + "__" + atomName;
//        if (node.label == null)
//          node.label = node.atomLabel;
//      }
      atomName = (node != null ? "Node_" + nodeID + "_"
          : link != null ? "Link_" + linkID + "_" : "TAtom_") + atomLabel;
      System.out.println("TAtom adding " + this);
      reader.asc.addAtom(this);
    }

    @Override
    public String toString() {
      return line + " " + super.toString();
    }
  }

  static void setTAtom(Atom a, Atom b) {
    b.setT(a);
    b.formalCharge = a.formalCharge;
    b.bondRadius = a.bondRadius;
  }


  /**
   * 
   * @param a TNode or TAtom
   * @param formula
   */
  static void setElementSymbol(Atom a, String formula) {
    String name = a.atomName;
    if (formula == null) {
      a.atomName = (a.atomName == null ? "X" : a.atomName.substring(a.atomName.indexOf('_') + 1));
    } else {
      a.atomName = formula;
    }
    a.getElementSymbol();
    a.atomName = name;
  }

  /**
   * Apply the symmetry and translation
   * @param a TNode or TAtom
   * @param ops
   * @param op
   * @param t
   */
  static void applySymmetry(Atom a, M4[] ops, int op, T3 t) {
    if (op >= 0) {
      if (op > 1 || t.x != 0 || t.y != 0 || t.z != 0) {
        if (op > 1)
          ops[op].rotTrans(a);
        a.add(t);
      }
    }
  }
  
  private class TNode extends Atom {

    public int id;
    public String formula;
    public String atomLabel;
    int netID;
    String netLabel;
    String label;
    int symop = 0;
    P3 trans = new P3();
    
    Lst<TAtom> tatoms;
    BS bsAtoms = null;
    
    int linkSymop = 0;
    P3 linkTrans = new P3();
    TNet net;
    private boolean isFinalized;
    int idx;
    private Atom atom;
    private String line;
    
    TNode() {
      super();
    }

    /**
     * Constructor from TLink
     * @param idx
     * @param atom
     * @param net
     * @param op
     * @param trans
     */
    TNode(int idx, Atom atom, TNet net, int op, P3 trans) {
      super();
      this.idx = idx;
      this.atom = atom;
      this.net = net;
      this.linkSymop = op;
      this.linkTrans = trans;
      this.label = this.atomName = this.atomLabel = atom.atomName;
      this.formula = atom.getElementSymbol();
      setTAtom(atom, this);
    }

    boolean setNode(int[] a, String line) {
      this.line = line;
      if (tatoms == null) {        
        if (Float.isNaN(x) != Float.isNaN(y) || Float.isNaN(y) != Float.isNaN(z)) 
          return false;
        idx = atomCount++;
        if (Float.isNaN(x)) {
          trans = P3.new3(a[0], a[1], a[2]);        
        } else {
          symop = 0;
        }
        if (formula != null && formula.indexOf(" ") < 0) {
          atomName = formula;
          getElementSymbol();
          if (!formula.equals(elementSymbol))
            elementSymbol = "Z";
          atomName = null;
        }
      }
      if (net == null)
        net = getNetFor(netID, netLabel);
      netLabel = net.label;
      netID = net.id;      
      return true;
    }

    void addAtom(TAtom atom) {
      if (tatoms == null)
        tatoms = new Lst<TAtom>();
      tatoms.add(atom);
    }

    void finalizeNode(M4[] ops) throws Exception {
      if (isFinalized)
        return;
      isFinalized = true;
      boolean haveXYZ = !Float.isNaN(x);
      Atom a;
      if (tatoms == null) {
        a = (atom != null ? atom
            : atomLabel == null ? null : getAtomFromName(atomLabel));
        setElementSymbol(this, formula == null ? a.elementSymbol : formula);
        if (a == null && !haveXYZ) {
          // no assigned atom_site
          // no associated atom
          // no defined xyz
          throw new Exception(
              "TopoCIFParser.finalizeNode no atom " + atomLabel + " line=" + line);
        }
      } else {
        setCentroid();
        if (tatoms.size() == 1) {
          TAtom ta = tatoms.get(0);
          setElementSymbol(ta, ta.elementSymbol);
          atomLabel = ta.atomLabel;
          formalCharge = ta.formalCharge;
          tatoms = null;
        } else {
          net.hasAtoms = true;
          elementSymbol = "Xx";
          for (int i = tatoms.size(); --i >= 0;) {
            TAtom ta = tatoms.get(i);
            ta.sequenceNumber = idx + 1;
            if (ta.atomName == null || !ta.atomName.startsWith(net.label + "_"))
              ta.atomName = net.label + "_" + ta.atomName;
          }
        }
        a = this;
      }
      if (a == atom || !haveXYZ) {
        if (a != this) {
          setTAtom(a, this);
        }
        applySymmetry(this, ops, symop, trans);
      }
      atomName = netLabel + "__";
      if (label != null && label.startsWith(atomName)) {
        atomName = "";
      }
      atomName += (label != null ? label : atomLabel != null ? atomLabel : "Node_" + id);      
      reader.addCifAtom(this, atomName, null, null);
      net.nNodes++;
    }

    private void setCentroid() {
      x = y = z = 0;
      int n = tatoms.size();
      for (int i = n; --i >= 0;)
        add(tatoms.get(i));
      x /= n;
      y /= n;
      z /= n;
    }


    public String info() {
      return "[node " + id + " " + label + "/" + atomName + " " 
          + super.toString() + "]";
    }

    @Override
    public String toString() {
      return info();
    }

    public TNode copy() {
      TNode node = (TNode) clone();
      return node;
    }
    
    @Override
    public Object clone() {
      try {
        return super.clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }


  }

  /**
   * A class to hold the _topol block information and transform it as needed. A
   * key field is the primitives array of TopoPrimitives. These structures allow
   * us to create a set of "primitive" operation results that operate
   * specifically on the links themselves. Rather than showing those links (as
   * with the Jmol script commented at the end of this class), we choose to
   * first create the standard Jmol atom set using, for example, load hcb.cif
   * PACKED or load xxx.cif {1 1 1} or load xxx.cif {444 666 1}, etc. Then we
   * match those atoms with link edges by unitizing the atom back to its unit
   * cell 555 site and then comparing with the primitive associated with a given
   * operator.
   * 
   */
  private class TLink extends Bond {

    int id;
    int netID;
    String netLabel;
    int id1, id2;
    String label1, label2; 
    int symop1, symop2;
    P3 trans1, trans2;
    String type = "";
    int multiplicity;
    int topoOrder;
    float voronoiAngle;
    float cartesianDistance;


    // derived:

    int idx;
    TNet net;
    TNode node1, node2;

    int typeBondOrder;

    Lst<TAtom> tatoms;
    BS bsAtoms = null;
    BS bsBonds = new BS();
    @SuppressWarnings("unused")
    private String line;
    
    public TLink() {
      super();
    }

    boolean setLink(int[] t1, int[] t2, String line) {
      this.line = line;
      idx = linkCount++;
      if (id2 == 0)
        id2 = id1;
      typeBondOrder = getBondType(type);
      // only a relative lattice change is necessary here.
      trans1 = P3.new3(t1[0], t1[1], t1[2]);
      trans2 = P3.new3(t2[0], t2[1], t2[2]);
      System.out.println("TopoCifParser.setLink " + this);
      return true;
    }

    void addAtom(TAtom atom) {
      if (tatoms == null)
        tatoms = new Lst<TAtom>();
      tatoms.add(atom);
    }

    Map<String, Object> getInfo() {
      Hashtable<String, Object> info = new Hashtable<String, Object>();
      info.put("index", Integer.valueOf(idx + 1));
      if (id > 0)
        info.put("id", Integer.valueOf(id));
      if (id1 > 0)
        info.put("id1", Integer.valueOf(id1));
      if (id2 > 0)
        info.put("id2", Integer.valueOf(id2));
      info.put("distance", Float.valueOf(cartesianDistance));
      info.put("netID", Integer.valueOf(netID));
      info.put("netLabel", netLabel);
      info.put("label1", node1.atomName);
      info.put("label2", node2.atomName);
      if (!Float.isNaN(distance))
        info.put("distance", Float.valueOf(distance));
      info.put("symop1", Integer.valueOf(symop1 + 1));
      info.put("symop2", Integer.valueOf(symop2 + 1));
      info.put("translation1", trans1);
      info.put("translation2", trans2);
      info.put("multiplicity", Integer.valueOf(multiplicity));
      if (type != null)
        info.put("type", type);
      info.put("voronoiSolidAngle", Float.valueOf(voronoiAngle));
      // derived
      info.put("atomIndex1", Integer.valueOf(i0 + node1.index));
      info.put("atomIndex2", Integer.valueOf(i0 + node2.index));
      if (bsAtoms != null && bsAtoms.cardinality() > 0)
        info.put("representedAtoms", bsAtoms);
      info.put("topoOrder", Integer.valueOf(topoOrder));
      info.put("order", Integer.valueOf(typeBondOrder));
      return info;
    }

    String info() {
      return "[link " + line + " : " + distance + "]";
    }

    @Override
    public String toString() {
      return info();
    }

    /**
     * Take all actions prior to applying symmetry. Specifically, create any
     * nodes and atoms
     * 
     * @param ops
     * @param sym 
     * 
     * @throws Exception
     */
    void finalizeLink(M4[] ops, SymmetryInterface sym) throws Exception {
      node1 = addLinkNodeIfNull(id1, label1, symop1, trans1);
      node2 = addLinkNodeIfNull(id2, label2, symop2, trans2);
      if (node1 == null || node2 == null) {
        Logger.warn("TopoCifParser atom "
            + (node1 == null ? label1 : label2) + " could not be found");
        return;
      }
      // check for the same node (but different symmetry, then)
      if (node1 == node2) {
        node2 = node1.copy();
      }
      node1.finalizeNode(ops);
      node2.finalizeNode(ops);
      applySymmetry(node1, ops, symop1, trans1);
      applySymmetry(node2, ops, symop2, trans2);
      

      // set the Bond fields, encoding the order field with "link", id, and typeBondOrder
      atomIndex1 = node1.index;
      atomIndex2 = node2.index;
      order = TOPOL_LINK + (id << LINK_TYPE_BITS) + typeBondOrder;

      temp1.setT(node1);
      temp2.setT(node2);
      sym.toCartesian(temp1, true);
      sym.toCartesian(temp2, true);
      distance = temp1.distance(temp2);
      if (cartesianDistance != 0 && Math.abs(distance - cartesianDistance) >= ERROR_TOLERANCE)
        System.err.println("Distance error! distance=" + distance + " for " + line);
      System.out.println("link d=" + distance + " " + this + node1 + node2);
      
      // encode the associated atom sequence number with this link's id.
      
      if (tatoms != null) {
//        TAtom aa = tatoms.get(0);
//        aa.finalizeAtom(ops);
//        aa.sequenceNumber = -idx - 1;
//        aa.atomName = net.label + "_" + aa.atomName;
        for (int i = tatoms.size(); --i >= 0;) {
          TAtom a = tatoms.get(i);
          a.finalizeAtom(ops);
          a.sequenceNumber = -idx - 1;
          a.atomName = net.label + "_" + a.atomName;
//          reader.asc.addNewBondWithOrderA(aa, a, TOPOL_GROUP + id);
        }        
      }

      reader.asc.addBond(this);
    }
    
    /**
     * @param id
     * @param nodeLabel
     * @param nodeOp
     * @param nodeTrans
     * @return the node
     * @throws Exception
     */
    private TNode addLinkNodeIfNull(int id, String nodeLabel, int nodeOp,
                                   P3 nodeTrans)
        throws Exception {

      // first check is for a node based on id
      TNode node = getNodeWithSym(id, nodeLabel, nodeOp, nodeTrans);
      if (node == null) {
        node = getNodeWithSym(id, nodeLabel, -1, null);
      }
      // second check is for an atom_site atom with this label
      Atom atom = (node == null ? getAtomFromName(nodeLabel) : null);
      // we now either have a node or an atom_site atom or we have a problem
      if (atom != null) {
        setNet(null);
        node = new TNode(atomCount++, atom, net, nodeOp, nodeTrans);
      } else if (node != null) {
        setNet(node);
        node = node.copy();
        node.linkSymop = nodeOp;
        node.linkTrans = nodeTrans;
      } else {
        throw new Exception(
            "TopoCIFParser.addNodeIfNull no atom or node " + nodeLabel + " line=" + line);
      }
      nodes.addLast(node);
      return node;
    }

    private void setNet(TNode node) {
      if (net != null)
        return;
      net = (node == null ? getNetFor(netID, netLabel) : node.net);
      net.nLinks++;
      netLabel = net.label;
      netID = net.id;
    }

    /**
     * Find a node that already matches this id and symmetry
     * @param nodeID
     * @param nodeLabel
     * @param op a symmetry operation [9...N-1] or -1 to ignore op and trans
     * @param trans the translation, ignored if op < 0
     * @return found node or null
     */
    private TNode getNodeWithSym(int nodeID, String nodeLabel, int op,
                                P3 trans) {
      if (nodeID > 0) 
        return getNodeById(nodeID, op, trans);
      for (int i = nodes.size(); --i >= 0;) {
        TNode n = nodes.get(i);
        if (n.label.equals(nodeLabel) && (op == -1 || op == n.linkSymop && trans.equals(n.linkTrans)))
          return n;
      }
      return null;
    }
  }

  /**
   * Find or create a net with this netID, giving it a default name "Net"+id
   * @param id
   * @return net, never null
   */
  public TNet getNetByID(int id) {
    for (int i = nets.size(); --i >= 0;) {
      TNet n = nets.get(i);
      if (n.id == id)
        return n;
    }
    TNet n = new TNet(netCount++, id, "Net" + id);
    nets.addLast(n);
    return n;
  }

  public Atom getAtomFromName(String name) {
    name = name.substring(name.lastIndexOf("_") + 1);
    return reader.asc.getAtomFromName(name);
  }

  /**
   * Find or create a TNet.
   * 
   * @param id
   * @param label
   * @return a net
   */
  public TNet getNetFor(int id, String label) {
    TNet net = null;
    if (id > 0) {
      net = getNetByID(id);
    } else if (label != null) {
      net = (TNet) getTopo(nets, label, 'N');
    }
    if (net == null) {
      net = getNetByID(id < 1 ? 1 : id);
      if (label != null)
        net.label = label;
    }
  return net;
}

  TLink getAssoiatedLinkByIdx(int idx) {
    for (int i = links.size(); --i >= 0;) {
      TLink l = links.get(i);
      if (l.idx == idx)
        return l;
    }
    return null;
  }

  TNode getAssociatedNodeByIdx(int idx) {
    for (int i = nodes.size(); --i >= 0;) {
      TNode n = nodes.get(i);
      if (n.idx == idx)
        return n;
    }
    return null;
  }

  public TLink getLinkById(int linkID) {
    for (int i = links.size(); --i >= 0;) {
      TLink l = links.get(i);
      if (l.id == linkID)
        return l;
    }
    return null;
  }

  public TNode getNodeById(int nodeID, int op, P3 trans) {
    for (int i = nodes.size(); --i >= 0;) {
      TNode n = nodes.get(i);
      if (n.id == nodeID && (op < 0 || n.linkSymop == op && n.trans.equals(trans)))
        return n;
    }
    return null;
  }

  public Object getTopo(Lst<?> l, String label, char type) {
    for (int i = 0; i < l.size(); i++) {
      Object o = l.get(i);
      switch (type) {
      case 'a':
        if (((TAtom) o).atomLabel.equals(label))
          return o;
        continue;
      case 'n':
        String s = ((TNode) o).label;
        if (s != null && s.equals(label))
          return o;
        continue;
      case 'N':
        if (((TNet) o).label.equals(label))
          return o;
        continue;
      default:
        return null;
      }
    }
    return null;
  }


}

