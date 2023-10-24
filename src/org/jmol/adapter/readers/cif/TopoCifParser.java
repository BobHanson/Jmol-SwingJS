package org.jmol.adapter.readers.cif;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.adapter.readers.cif.CifReader.Parser;
import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.Bond;
import org.jmol.api.JmolAdapter;
import org.jmol.api.SymmetryInterface;
import org.jmol.symmetry.SymmetryOperation;
import org.jmol.util.Edge;
import org.jmol.util.JmolMolecule;

import javajs.api.GenericCifDataParser;
import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.M4d;
import javajs.util.P3d;
import javajs.util.T3d;

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
  public static final int LINK_TYPE_GENERIC_LINK = 0;
  public static final int LINK_TYPE_SINGLE = 1;
  public static final int LINK_TYPE_DOUBLE = 2;
  public static final int LINK_TYPE_TRIPLE = 3;
  public static final int LINK_TYPE_QUADRUPLE = 4;
  public static final int LINK_TYPE_QUINTUPLE = 5;
  public static final int LINK_TYPE_SEXTUPLE = 6;
  public static final int LINK_TYPE_SEPTUPLE = 7;
  public static final int LINK_TYPE_OCTUPLE = 8;
  public static final int LINK_TYPE_AROM = 9;
  public static final int LINK_TYPE_POLY = 0xA;
  public static final int LINK_TYPE_DELO = 0xB;
  public static final int LINK_TYPE_PI = 0xC;
  public static final int LINK_TYPE_HBOND = 0xD;
  public static final int LINK_TYPE_VDW = 0xE;
  public static final int LINK_TYPE_OTHER = 0xF; // Special bond

  // officially v pi hb vw sb . (no bond)
  public static String linkTypes = "?  " 
      + "SIN" + "DOU" + "TRI" + "QUA" + "QUI" + "SEX" + "SEP" + "OCT" // 1-8
      + "ARO" + "POL" + "DEL" // 9-11
      + "PI " + "HBO" + "VDW"; //12-14 

  static int getBondType(String type, int order) {
    if (type == null)
      return LINK_TYPE_GENERIC_LINK;
    type = type.toUpperCase();
    if (type.equals("V"))
      return (order == 0 ? LINK_TYPE_SINGLE : order);
    if (type.equals("sb"))
      type = "?";
    switch (type.charAt(0)) {
    case 'V':
      return LINK_TYPE_VDW;
    }
    if (type.length() > 3)
      type = type.substring(0, 3);
    // defaults to SINGLE
    return Math.max(1, linkTypes.indexOf(type) / 3);
  }

  public static final int LINK_TYPE_BITS = 4;

  static double ERROR_TOLERANCE = 0.001;

  /**
   * reader will be null if filter includes TOPOS_IGNORE
   */
  CifReader reader;

  /**
   * list of TOPOL_ATOM loop data
   */
  Lst<TAtom> atoms = new Lst<TAtom>();

  /**
   * list of TOPOL_NODE loop data
   */
  Lst<TNode> nodes = new Lst<TNode>();

  /**
   * list of TOPOL_LINK loop data
   */
  Lst<TLink> links = new Lst<TLink>();

  /**
   * list of TOPOL_NET loop or single data item data
   */
  Lst<TNet> nets = new Lst<TNet>();

  /**
   * storage for a single net from a non-looped data item
   */
  TNet singleNet;

  int netCount;
  int linkCount;
  int atomCount;

  T3d temp1 = new P3d(), temp2 = new P3d();

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
  M4d[] ops;

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
  private SymmetryInterface sym;
  String selectedNet;

  // CifParser supports up to 100 fields
  final private static String[] topolFields = {
      /*0*/"_topol_net_id",
      /*1*/"_topol_net_label",
      /*2*/"_topol_net_special_details",
      /*3*/"_topol_link_id",
      /*4*/"_topol_link_net_id",
      /*5*/"_topol_link_node_id_1",
      /*6*/"_topol_link_node_id_2",
      /*7*/"_topol_link_symop_id_1",
      /*8*/"_topol_link_translation_1",
      /*9*/"_topol_link_translation_1_x",
      /*10*/"_topol_link_translation_1_y",
      /*11*/"_topol_link_translation_1_z",
      /*12*/"_topol_link_symop_id_2",
      /*13*/"_topol_link_translation_2",
      /*14*/"_topol_link_translation_2_x",
      /*15*/"_topol_link_translation_2_y",
      /*16*/"_topol_link_translation_2_z",
      /*17*/"_topol_link_distance",
      /*18*/"_topol_link_type",
      /*19*/"_topol_link_multiplicity",
      /*20*/"_topol_link_voronoi_solidangle",
      /*21*/"_topol_link_order",
      /*22*/"_topol_node_id",
      /*23*/"_topol_node_net_id",
      /*24*/"_topol_node_label",
      /*25*/"_topol_node_symop_id",
      /*26*/"_topol_node_translation",
      /*27*/"_topol_node_translation_x",
      /*28*/"_topol_node_translation_y",
      /*29*/"_topol_node_translation_z",
      /*30*/"_topol_node_fract_x",
      /*31*/"_topol_node_fract_y",
      /*32*/"_topol_node_fract_z",
      /*33*/"_topol_atom_id",
      /*34*/"_topol_atom_atom_label",
      /*35*/"_topol_atom_node_id",
      /*36*/"_topol_atom_link_id",
      /*37*/"_topol_atom_symop_id",
      /*38*/"_topol_atom_translation",
      /*39*/"_topol_atom_translation_x",
      /*40*/"_topol_atom_translation_y",
      /*41*/"_topol_atom_translation_z",
      /*42*/"_topol_atom_fract_x",
      /*43*/"_topol_atom_fract_y",
      /*44*/"_topol_atom_fract_z",
      /*45*/"_topol_atom_element_symbol",
      /*46*/"_topol_link_site_symmetry_symop_1",
      /*47*/"_topol_link_site_symmetry_translation_1_x",
      /*48*/"_topol_link_site_symmetry_translation_1_y",
      /*49*/"_topol_link_site_symmetry_translation_1_z",
      /*50*/"_topol_link_site_symmetry_symop_2",
      /*51*/"_topol_link_site_symmetry_translation_2_x",
      /*52*/"_topol_link_site_symmetry_translation_2_y",
      /*53*/"_topol_link_site_symmetry_translation_2_z",
      /*54*/"_topol_link_site_symmetry_translation_1",
      /*55*/"_topol_link_site_symmetry_translation_2",
      /*56*/"_topol_link_node_label_1",
      /*57*/"_topol_link_node_label_2",
      /*58*/"_topol_link_atom_label_1",
      /*59*/"_topol_link_atom_label_2",
  };

  private final static byte topol_net_id = 0;
  private final static byte topol_net_label = 1;
  private final static byte topol_net_special_details = 2;
  private final static byte topol_link_id = 3;
  private final static byte topol_link_net_id = 4;
  private final static byte topol_link_node_id_1 = 5;
  private final static byte topol_link_node_id_2 = 6;
  private final static byte topol_link_symop_id_1 = 7;
  private final static byte topol_link_translation_1 = 8;
  private final static byte topol_link_translation_1_x = 9;
  private final static byte topol_link_translation_1_y = 10;
  private final static byte topol_link_translation_1_z = 11;
  private final static byte topol_link_symop_id_2 = 12;
  private final static byte topol_link_translation_2 = 13;
  private final static byte topol_link_translation_2_x = 14;
  private final static byte topol_link_translation_2_y = 15;
  private final static byte topol_link_translation_2_z = 16;
  private final static byte topol_link_distance = 17;
  private final static byte topol_link_type = 18;
  private final static byte topol_link_multiplicity = 19;
  private final static byte topol_link_voronoi_solidangle = 20;
  private final static byte topol_link_order = 21;
  private final static byte topol_node_id = 22;
  private final static byte topol_node_net_id = 23;
  private final static byte topol_node_label = 24;
  private final static byte topol_node_symop_id = 25;
  private final static byte topol_node_translation = 26;
  private final static byte topol_node_translation_x = 27;
  private final static byte topol_node_translation_y = 28;
  private final static byte topol_node_translation_z = 29;
  private final static byte topol_node_fract_x = 30;
  private final static byte topol_node_fract_y = 31;
  private final static byte topol_node_fract_z = 32;
  private final static byte topol_atom_id = 33;
  private final static byte topol_atom_atom_label = 34;
  private final static byte topol_atom_node_id = 35;
  private final static byte topol_atom_link_id = 36;
  private final static byte topol_atom_symop_id = 37;
  private final static byte topol_atom_translation = 38;
  private final static byte topol_atom_translation_x = 39;
  private final static byte topol_atom_translation_y = 40;
  private final static byte topol_atom_translation_z = 41;
  private final static byte topol_atom_fract_x = 42;
  private final static byte topol_atom_fract_y = 43;
  private final static byte topol_atom_fract_z = 44;
  private final static byte topol_atom_element_symbol = 45;
  private final static byte topol_link_site_symmetry_symop_1_DEPRECATED = 46;
  private final static byte topol_link_site_symmetry_translation_1_x_DEPRECATED = 47;
  private final static byte topol_link_site_symmetry_translation_1_y_DEPRECATED = 48;
  private final static byte topol_link_site_symmetry_translation_1_z_DEPRECATED = 49;
  private final static byte topol_link_site_symmetry_symop_2_DEPRECATED = 50;
  private final static byte topol_link_site_symmetry_translation_2_x_DEPRECATED = 51;
  private final static byte topol_link_site_symmetry_translation_2_y_DEPRECATED = 52;
  private final static byte topol_link_site_symmetry_translation_2_z_DEPRECATED = 53;
  private final static byte topol_link_site_symmetry_translation_1_DEPRECATED = 54;
  private final static byte topol_link_site_symmetry_translation_2_DEPRECATED = 55;
  private final static byte topol_link_node_label_1_DEPRECATED = 56;
  private final static byte topol_link_node_label_2_DEPRECATED = 57;

  public TopoCifParser() {
  }

  /**
   * filter "TOPOS_TYPES=hb" will only load hydrogen bonds; options include v,
   * vw, and hb
   */
  @Override
  public TopoCifParser setReader(CifReader reader) {
    if (!reader.checkFilterKey("TOPOL")) {
      reader.appendLoadNote(
          "This file has Topology analysis records.\nUse LOAD \"\" {1 1 1} FILTER \"TOPOL\"  to load the topology.");
      return this;
    }
    this.reader = reader;
    String net = reader.getFilter("TOPOLNET=");
    selectedNet = net;
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

  /**
   * process _topol_node.id 1
   * 
   */
  @Override
  public void ProcessRecord(String key, String data) throws Exception {
    if (reader == null || failed != null) {
      return;
    }
    int pt = key.indexOf(".");
    if (pt < 0) {
      // _topol_*_ --> _topol_*.
      pt = key.indexOf('_',key.indexOf('_',1) + 1);
      if (pt < 0)
        return;
      key = key.substring(0, pt) + '.' + key.substring(pt + 1);
    }
    processBlock(key);
  }

  @Override
  public boolean processBlock(String key) throws Exception {
    if (reader == null || failed != null) {
      return false;
    }
    if (ac0 < 0) {
      reader.asc.firstAtomToBond = reader.asc.getAtomSetAtomIndex(reader.asc.iSet);
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
    } else {
      return false;
    }
    return true;
  }

  /**
   * Process all nets. Note that the nets list is self-populating with a "Net1"
   * value if there is no TOPOL_NET section.
   * 
   * @throws Exception
   */
  private void processNets() throws Exception {
    while (cifParser.getData()) {
      String id = getDataValue(topol_net_id);
      String netLabel = getDataValue(topol_net_label);
      if (id == null)
        id = "" + (netCount + 1);
      TNet net = getNetFor(id, netLabel, true);
      net.specialDetails = getDataValue(topol_net_special_details);
      net.line = reader.line;
    }
  }

  private void processLinks() throws Exception {
    while (cifParser.getData()) {
      String t = getDataValue(topol_link_type);
      String type = (t == null ? null : t.toLowerCase());
      if (allowedTypes != null
          && (type == null || allowedTypes.indexOf("+" + type + "+") < 0))
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
          link.id = field;
          break;
        case topol_link_net_id:
          link.netID = field;
          break;
        case topol_link_node_id_1:
          link.nodeIds[0] = field;
          break;
        case topol_link_node_id_2:
          link.nodeIds[1] = field;
          break;
        case topol_link_node_label_1_DEPRECATED: // legacy
          link.nodeLabels[0] = field;
          break;
        case topol_link_node_label_2_DEPRECATED: // legacy
          link.nodeLabels[1] = field;
          break;
        case topol_link_site_symmetry_symop_1_DEPRECATED:
        case topol_link_symop_id_1:
          link.symops[0] = getInt(field) - 1;
          break;
        case topol_link_site_symmetry_symop_2_DEPRECATED:
        case topol_link_symop_id_2:
          link.symops[1] = getInt(field) - 1;
          break;
        case topol_link_order:
          link.topoOrder = getInt(field);
          break;
        case topol_link_site_symmetry_translation_1_DEPRECATED:
        case topol_link_site_symmetry_translation_1_x_DEPRECATED:
        case topol_link_site_symmetry_translation_1_y_DEPRECATED:
        case topol_link_site_symmetry_translation_1_z_DEPRECATED:
        case topol_link_translation_1:
        case topol_link_translation_1_x:
        case topol_link_translation_1_y:
        case topol_link_translation_1_z:
          t1 = processTranslation(p, t1, field);
          break;
        case topol_link_site_symmetry_translation_2_DEPRECATED:
        case topol_link_site_symmetry_translation_2_x_DEPRECATED:
        case topol_link_site_symmetry_translation_2_y_DEPRECATED:
        case topol_link_site_symmetry_translation_2_z_DEPRECATED:
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
          node.id = field;
          break;
        case topol_node_label:
          node.label = field;
          break;
        case topol_node_net_id:
          node.netID = field;
          break;
        case topol_node_symop_id:
          node.symop = getInt(field) - 1;
          break;
        case topol_node_translation:
        case topol_node_translation_x:
        case topol_node_translation_y:
        case topol_node_translation_z:
          t = processTranslation(p, t, field);
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
          atom.id = field;
          break;
        case topol_atom_atom_label:
          atom.atomLabel = field;
          break;
        case topol_atom_node_id:
          atom.nodeID = field;
          break;
        case topol_atom_link_id:
          atom.linkID = field;
          break;
        case topol_atom_symop_id:
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
          atom.elementSymbol = field;
          break;
        }
      }
      if (atom.setAtom(t, reader.line))
        atoms.addLast(atom);
    }
  }

  private int[] processTranslation(int p, int[] t, String field) {
    switch (p) {
    case topol_link_site_symmetry_translation_1_DEPRECATED:
    case topol_link_site_symmetry_translation_2_DEPRECATED:
    case topol_link_translation_1:
    case topol_link_translation_2:
    case topol_node_translation:
    case topol_atom_translation:
      t = Cif2DataParser.getIntArrayFromStringList(field, 3);
      break;
    case topol_link_site_symmetry_translation_1_x_DEPRECATED:
    case topol_link_site_symmetry_translation_2_x_DEPRECATED:
    case topol_link_translation_1_x:
    case topol_link_translation_2_x:
    case topol_node_translation_x:
    case topol_atom_translation_x:
      t[0] = getInt(field);
      break;
    case topol_link_site_symmetry_translation_1_y_DEPRECATED:
    case topol_link_site_symmetry_translation_2_y_DEPRECATED:
    case topol_link_translation_1_y:
    case topol_link_translation_2_y:
    case topol_node_translation_y:
    case topol_atom_translation_y:
      t[1] = getInt(field);
      break;
    case topol_link_site_symmetry_translation_1_z_DEPRECATED:
    case topol_link_site_symmetry_translation_2_z_DEPRECATED:
    case topol_link_translation_1_z:
    case topol_link_translation_2_z:
    case topol_node_translation_z:
    case topol_atom_translation_z:
      t[2] = getInt(field);
      break;
    }
    return t;
  }

  /**
   * PRIOR to symmetry application, process all internal symop/translation
   * aspects.
   */
  @Override
  public boolean finalizeReader() throws Exception {
    // opportunity to handle anything prior to applying symmetry
    if (reader == null || reader.symops == null)
      return false;
    cifParser = null;
    reader.applySymmetryToBonds = true;
    // finalize all topol_atom symmetries
    Lst<String> symops = reader.symops;
    int nOps = symops.size();
    ops = new M4d[nOps];
    for (int i = 0; i < nOps; i++) {
      ops[i] = SymmetryOperation.getMatrixFromXYZ("!" + symops.get(i), true);
    }
    for (int i = 0; i < atoms.size(); i++) {
      atoms.get(i).finalizeAtom();
    }
    // sym is used only to allow conversion to Cartesian coordinates for the link distance finalization
    sym = reader.getSymmetry();
    // we do not have to finalize nodes directly -- finalizeLink will take care of that.
    for (int i = 0; i < links.size(); i++) {
      links.get(i).finalizeLink();
    }
    for (int i = links.size(); --i >= 0;) {
      if (!links.get(i).finalized)
        links.removeItemAt(i);
    }

    if (reader.doApplySymmetry) {
      reader.applySymmetryAndSetTrajectory();
    }
    if (selectedNet != null)
      selectNet();
    return true;
  }

  private void selectNet() {
    TNet net = getNetFor(null, selectedNet, false);
    if (net == null) {
      net = getNetFor(selectedNet, null, false);
    }
    if (net == null)
      return;
    BS bsAtoms = reader.asc.getBSAtoms(-1);
    Atom[] atoms = reader.asc.atoms;
    for (int i = reader.asc.ac; --i >= 0;) {
      Atom a = atoms[i];
      if (!(a instanceof TPoint) || ((TPoint) a).getNet() != net) {
        bsAtoms.clear(i);
      }
    }
  }

  /**
   * Symmetry has been applied. Identify all of the connected atoms and process
   * the group associations
   * 
   */
  @Override
  public void finalizeSymmetry(boolean haveSymmetry) throws Exception {
    if (reader == null || !haveSymmetry || links.size() == 0)
      return;

    BS bsConnected = new BS(); // atoms that are linked
    BS bsAtoms = new BS(); // atoms that are associated or connected;
    int nLinks = processAssociations(bsConnected, bsAtoms);
    // create the excluded atoms set -- atoms of bsAtoms that are linked 
    BS bsExclude = shiftBits(bsAtoms, bsConnected);
    // If we have a network, remove all unconnected atoms.
    if (!bsConnected.isEmpty()) {
      reader.asc.bsAtoms = bsAtoms;
      reader.asc.atomSetInfo.put("bsExcludeBonding", bsExclude);
    }
    reader.appendLoadNote("TopoCifParser created " + bsConnected.cardinality()
        + " nodes and " + nLinks + " links");

    // add auxiliaryInfo.models[i].topology
    Lst<Map<String, Object>> info = new Lst<Map<String, Object>>();
    for (int i = 0, n = links.size(); i < n; i++) {
      info.addLast(links.get(i).getLinkInfo());
    }
    reader.asc.setCurrentModelInfo("topology", info);
    String script = ""
        + "if (autobond) {delete !connected && !(atomName LIKE '*_Link*' or atomName LIKE '*_Node*')}; "
        + "display displayed or " + nets.get(0).label + "__*";
    reader.addJmolScript(script);
    for (int i = 0; i < nets.size(); i++) {
      nets.get(i).finalizeNet();
    }
  }

  /**
   * Shift bits to the left to account for missing atoms in the final atom list.
   * 
   * @param bsAtoms
   * @param bs
   * @return shifted bitset
   */
  static BS shiftBits(BS bsAtoms, BS bs) {
    BS bsNew = new BS();
    for (int pt = 0, i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms
        .nextSetBit(i + 1)) {
      while (bsAtoms.get(i)) {
        bsNew.setBitTo(pt++, bs.get(i++));
      }
    }
    return bsNew;
  }

  /**
   * Find and process all "bonds" associated with all links and nodes. This
   * method runs AFTER generation of all the symmetry-related atoms.
   * 
   * BOND_LINK + index indicates linked nodes
   * 
   * BOND_GROUP + index indicates associated nodes
   * 
   * 
   * @param bsConnected
   *        prevent Jmol from adding bonds to this atom
   * @param bsAtoms
   *        allow Jmol to add bonds to these atoms, inclusively
   * 
   * @return number of bonds created
   */
  private int processAssociations(BS bsConnected, BS bsAtoms) {

    int nlinks = 0;
    BS bsAtoms0 = reader.asc.bsAtoms;
    Atom[] atoms = reader.asc.atoms;

    // set associations for links and nodes 
    for (int i = reader.asc.ac; --i >= ac0;) {
      Atom a = atoms[i];
      if (bsAtoms0 != null && !bsAtoms0.get(i))
        continue;
      int idx = (a instanceof TAtom ? ((TAtom) a).idx1 : 0);
      if (idx == Integer.MIN_VALUE || idx == 0)
        continue;
      if (idx > 0) {
        TNode node = getAssociatedNodeByIdx(idx - 1);
        if (node.bsAtoms == null)
          node.bsAtoms = new BS();
        node.bsAtoms.set(i0 + a.index);
      } else {
        TLink link = getAssoiatedLinkByIdx(-idx - 1);
        if (link != null) {
          if (link.bsAtoms == null)
            link.bsAtoms = new BS();
          link.bsAtoms.set(i0 + a.index);
        }
      }
      bsAtoms.set(a.index);
    }

    boolean checkDistance = reader.doPackUnitCell;
    double distance;
    // finish up with bonds
    Bond[] bonds = reader.asc.bonds;
    for (int i = reader.asc.bondCount; --i >= bc0;) {
      Bond b = bonds[i];
      if (b.order >= TOPOL_GROUP) {
        // associated atoms - don't show this bond
        bonds[i] = null;
      } else if (b.order >= TOPOL_LINK) {
        // adjust link bond order, and add this bond to the link's bsBonds bitset
        if (bsAtoms0 != null
            && (!bsAtoms0.get(b.atomIndex1) || !bsAtoms0.get(b.atomIndex2))) {
          bonds[i] = null;
          continue;
        }
        b.order -= TOPOL_LINK;
        TLink link = getAssoiatedLinkByIdx(b.order >> LINK_TYPE_BITS);

        if (checkDistance
            && Math.abs((distance = calculateDistance(atoms[b.atomIndex1],
                atoms[b.atomIndex2])) - link.distance) >= ERROR_TOLERANCE) {
          System.err.println("Distance error! removed! distance=" + distance
              + " for " + link + link.linkNodes[0] + link.linkNodes[1]);
          bonds[i] = null;
          continue;
        }
        if (link.bsBonds == null)
          link.bsBonds = new BS();
        link.bsBonds.set(b0 + i);
        switch (b.order & 0xF) {
        default:
          b.order = Edge.BOND_COVALENT_SINGLE;
          break;
        case LINK_TYPE_DOUBLE:
          b.order = Edge.BOND_COVALENT_DOUBLE;
          break;
        case LINK_TYPE_TRIPLE:
          b.order = Edge.BOND_COVALENT_TRIPLE;
          break;
        case LINK_TYPE_QUADRUPLE:
          b.order = Edge.BOND_COVALENT_QUADRUPLE;
          break;
        case LINK_TYPE_QUINTUPLE:
          b.order = Edge.BOND_COVALENT_QUINTUPLE;
          break;
        case LINK_TYPE_SEXTUPLE:
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

    bsAtoms.or(bsConnected);
    if (bsAtoms0 != null)
      bsAtoms.and(bsAtoms0);

    for (int i = nodes.size(); --i >= 0;) {
      TNode node = nodes.get(i);
      if (node.bsAtoms != null) {
        node.bsAtoms = shiftBits(bsAtoms, node.bsAtoms);
      }
    }

    for (int i = links.size(); --i >= 0;) {
      TLink link = links.get(i);
      if (link.bsAtoms != null) {
        link.bsAtoms = shiftBits(bsAtoms, link.bsAtoms);
      }
    }

    return nlinks;
  }

  static boolean isEqualD(T3d p1, T3d p2, double d) {
    return (Double.isNaN(d) || Math.abs(p1.distance(p2) - d) < ERROR_TOLERANCE);
  }

  /**
   * Read the data value.
   * 
   * @param key
   * @return the value or null if does not exist or is '.' or '?'
   */
  private String getDataValue(byte key) {
    String f = reader.getField(key);
    return ("\0".equals(f) ? null : f);
  }

  private int getInt(String f) {
    return (f == null ? Integer.MIN_VALUE : reader.parseIntStr(f));
  }

  private double getFloat(String f) {
    return (f == null ? Double.NaN : reader.parseDoubleStr(f));
  }

  private interface TPoint {

    TNet getNet();
  }

  private class TNet {
    @SuppressWarnings("unused")
    String line;
    String id;
    int nLinks, nNodes;
    String label;
    String specialDetails;

    int idx;
    boolean hasAtoms;

    TNet(int index, String id, String label, String specialDetails) {
      idx = index;
      this.id = id;
      this.label = label;
      this.specialDetails = specialDetails;
    }

    void finalizeNet() {
      if (id == null)
        id = "" + (idx + 1);
      if (selectedNet != null && !label.equalsIgnoreCase(selectedNet)
          && !id.equalsIgnoreCase(selectedNet))
        return;
      String netKey = "," + id + ",";
      if (netNotes.indexOf(netKey) < 0) {
        reader
            .appendLoadNote("Net " + label
                + (specialDetails == null ? "" : " '" + specialDetails + "'")
                + " created from " + nLinks + " links and " + nNodes
                + " nodes.\n" + "Use DISPLAY "
                + (hasAtoms ? label
                    + "__* to display it without associated atoms\nUse DISPLAY "
                    + label + "_* to display it with its associated atoms"
                    : label + "* to display it" + ""));
      }
    }
  }

  private class TAtom extends Atom implements TPoint {

    // from CIF data:
    @SuppressWarnings("unused")
    String id;
    String atomLabel;
    String nodeID;
    String linkID;
    int symop = 0;
    private P3d trans = new P3d();
    String line;

    // derived
    private boolean isFinalized;
    @SuppressWarnings("unused")
    int idx;
    TNet net;
    public int idx1;

    TAtom() {
      super();
      @SuppressWarnings("unused")
      int i = 0;// old transpiler hack
    }

    TAtom getTClone() {
      try {
        TAtom ta = (TAtom) clone();
        ta.idx = atomCount++;
        return ta;
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }

    @Override
    public TNet getNet() {
      return net;
    }

    boolean setAtom(int[] a, String line) {
      this.line = line;
      if (Double.isNaN(x) != Double.isNaN(y) || Double.isNaN(y) != Double.isNaN(z))
        return false;
      idx = atomCount++;
      if (Double.isNaN(x)) {
        trans = P3d.new3(a[0], a[1], a[2]);
      } else {
        symop = 0;
      }
      atomName = atomLabel;
      return true;
    }

    void finalizeAtom() throws Exception {
      if (isFinalized)
        return;
      isFinalized = true;
      Atom a = getAtomFromName(atomLabel);
      setElementSymbol(this, elementSymbol);
      if (a == null && Double.isNaN(x)) {
        // no associated atom
        throw new Exception("_topol_atom: no atom " + atomLabel
            + " line=" + line);
      }

      // check for addition to a TNode
      TNode node = null;
      if (nodeID != null) {
        node = findNode(nodeID, -1, null);
      }

      // check for addition to a TLink
      TLink link = null;
      if (linkID != null) {
        link = getLinkById(linkID);
      }

      if (node == null && link == null) {
        System.out.println("TAtom " + this + " ignored");
        return;
      }

      // transfer fields and set the symmetry op [tx ty tz]
      if (a != null && Double.isNaN(x)) {
        setTAtom(a, this);
        applySymmetry(this, ops, symop, trans);
      }

      // add this atom to the AtomSetCollection
      atomName = atomLabel;
//System.out.println("TAtom adding " + this);
      
      if (node != null) {
        node.addAtom(this);
      }
      TAtom ta = this;
      if (link != null)
        ta = link.addAtom(this);
      reader.addCifAtom(this, atomName, null, null);
      if (ta != this)
        reader.addCifAtom(ta, atomName, null, null);
    }

    private TLink getLinkById(String linkID) {
      for (int i = links.size(); --i >= 0;) {
        TLink l = links.get(i);
        if (l.id.equalsIgnoreCase(linkID))
          return l;
      }
      return null;
    }

    @Override
    public String toString() {
      return line + " " + super.toString();
    }

  }

  static String getMF(Lst<TAtom> tatoms) {
    int n = tatoms.size();
    if (n < 2)
      return (n == 0 ? "" : tatoms.get(0).elementSymbol);
    int[] atNos = new int[n];
    for (int i = 0; i < n; i++) {
      atNos[i] = JmolAdapter
          .getElementNumber(tatoms.get(i).getElementSymbol());
    }
    JmolMolecule m = new JmolMolecule();
    m.atNos = atNos;
    return m.getMolecularFormula(false, null, false);
  }

  static void setTAtom(Atom a, Atom b) {
    b.setT(a);
    b.formalCharge = a.formalCharge;
    b.bondingRadius = a.bondingRadius;
  }

  /**
   * 
   * @param a
   *        TNode or TAtom
   * @param sym
   */
  static void setElementSymbol(Atom a, String sym) {
    String name = a.atomName;
    if (sym == null) {
      a.atomName = (a.atomName == null ? "X"
          : a.atomName.substring(a.atomName.indexOf('_') + 1));
    } else {
      a.atomName = sym;
    }
    a.getElementSymbol();
    a.atomName = name;
  }

  /**
   * Apply the symmetry and translation
   * 
   * @param a
   *        TNode or TAtom
   * @param ops
   * @param op
   * @param t
   */
  static void applySymmetry(Atom a, M4d[] ops, int op, T3d t) {
    if (op >= 0) {
      if (op >= 1 || t.x != 0 || t.y != 0 || t.z != 0) {
        if (op >= 1)
          ops[op].rotTrans(a);
        a.add(t);
      }
    }
  }

  final static P3d ZERO = new P3d();
  private class TNode extends Atom implements TPoint {

    public String id;
    public String atomLabel;
    String netID;
    String label;
    int symop = 0;
    P3d trans = new P3d();

    Lst<TAtom> tatoms;
    BS bsAtoms = null;

    int linkSymop = 0;
    P3d linkTrans = new P3d();
    TNet net;
    private boolean isFinalized;
    int idx;
    private Atom atom; // legacy
    private String line;
    private String mf;

    TNode() {
      super();
      @SuppressWarnings("unused")
      int i = 0;// old transpiler needs this?
    }

    /**
     * Constructor from TLink
     * 
     * @param idx
     * @param atom
     * @param net
     * @param op
     * @param trans
     */
    TNode(int idx, Atom atom, TNet net, int op, P3d trans) {
      super();
      this.idx = idx;
      this.atom = atom;
      this.net = net;
      this.linkSymop = op;
      this.linkTrans = trans;
      this.label = this.atomName = this.atomLabel = atom.atomName;
      this.elementSymbol = atom.elementSymbol;
//      this.formula = atom.getElementSymbol();
      setTAtom(atom, this);
    }
    
    public String getMolecularFormula() {
      return (mf == null ? (mf = getMF(tatoms)) : mf);
    }


    @Override
    public TNet getNet() {
      return net;
    }

    boolean setNode(int[] a, String line) {
      this.line = line;
      if (tatoms == null) {
        if (Double.isNaN(x) != Double.isNaN(y)
            || Double.isNaN(y) != Double.isNaN(z))
          return false;
        idx = atomCount++;
        if (Double.isNaN(x)) {
          trans = P3d.new3(a[0], a[1], a[2]);
        } else {
          symop = 0;
        }
//        if (formula != null && formula.indexOf(" ") < 0) {
//          atomName = formula;
//          getElementSymbol();
//          if (!formula.equals(elementSymbol))
//            elementSymbol = "Z";
//          atomName = null;
//        }
      }
      return true;
    }

    void addAtom(TAtom atom) {
      if (tatoms == null)
        tatoms = new Lst<TAtom>();
      atom.atomName = "Node_" + atom.nodeID + "_" + atom.atomLabel;
      tatoms.addLast(atom);
    }

    void finalizeNode(M4d[] ops) throws Exception {
      if (isFinalized)
        return;
      isFinalized = true;
      if (net == null)
        net = getNetFor(netID, null, true);
      boolean haveXYZ = !Double.isNaN(x);
      Atom a;
      if (tatoms == null) {
        a = null;
//        a = (atom == null ? getAtomFromName(atomLabel) : atom);
        if (!haveXYZ) {
          // no assigned atom_site
          // no associated atom
          // no defined xyz
          throw new Exception("_topol_node no atom " + atomLabel
              + " line=" + line);
        }
//        setElementSymbol(this, a.elementSymbol);
      } else {
        if (Double.isNaN(x))
          setCentroid();
        if (tatoms.size() == 1) {
          TAtom ta = tatoms.get(0);
          elementSymbol = ta.elementSymbol;
          atomLabel = ta.atomLabel;
          formalCharge = ta.formalCharge;
          tatoms = null;
        } else {
          net.hasAtoms = true;
          elementSymbol = "Xx";
          for (int i = tatoms.size(); --i >= 0;) {
            TAtom ta = tatoms.get(i);
            ta.idx1 = idx + 1;
            if (ta.atomName == null || !ta.atomName.startsWith(net.label + "_"))
              ta.atomName = net.label + "_" + ta.atomName;
            ta.net = net;
          }
        }
        a = this;
      }
      if ((a != null && a == atom) || !haveXYZ) {
        if (a != this) {
          setTAtom(a, this);
        }
        applySymmetry(this, ops, symop, trans);
      }
      atomName = net.label.replace(' ', '_') + "__";
      if (label != null && label.startsWith(atomName)) {
        atomName = "";
      }
      atomName += (label != null ? label
          : atomLabel != null ? atomLabel : "Node_" + id);
      addNode();
    }

    private void addNode() {
      reader.addCifAtom(this, atomName, null, null);
      net.nNodes++;
      if (tatoms != null && tatoms.size() > 1)
        reader.appendLoadNote("_topos_node " + id + " " + atomName + " has formula " + getMolecularFormula());
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
      return "[node idx=" + idx + " id=" + id + " " + label + "/" + atomName + " "
          + super.toString() + "]";
    }

    @Override
    public String toString() {
      return info();
    }

    public TNode copy() {
      TNode node = (TNode) clone();
      node.idx = atomCount++;
      if (node.isFinalized)
        node.addNode();
      if (tatoms != null) {
        node.tatoms = new Lst<TAtom>();
        for (int i = 0, n = tatoms.size(); i < n; i++) {
          TAtom ta = tatoms.get(i).getTClone();
          node.tatoms.addLast(ta);
          reader.addCifAtom(ta, ta.atomName, null, null);
        }
      }
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
   * A class to hold the TOPOL_LINK data item information and transform it as
   * needed. A key field is the primitives array of TopoPrimitives. These
   * structures allow us to create a set of "primitive" operation results that
   * operate specifically on the links themselves. Rather than showing those
   * links (as with the Jmol script commented at the end of this class), we
   * choose to first create the standard Jmol atom set using, for example, load
   * hcb.cif PACKED or load xxx.cif {1 1 1} or load xxx.cif {444 666 1}, etc.
   * Then we match those atoms with link edges by unitizing the atom back to its
   * unit cell 555 site and then comparing with the primitive associated with a
   * given operator.
   * 
   */
  private class TLink extends Bond {

    String id;
    String[] nodeIds = new String[2];
    String[] nodeLabels = new String[2];
    int[] symops = new int[2];
    P3d[] translations = new P3d[2];

    String netID;
    String netLabel;
    String type = "";
    int multiplicity;
    int topoOrder;
    double voronoiAngle;
    double cartesianDistance;

    // derived:

    int idx;
    TNet net;
    TNode[] linkNodes = new TNode[2];

    int typeBondOrder;

    Lst<TAtom> tatoms;
    BS bsAtoms;
    BS bsBonds;
    private String line;
    boolean finalized;
    private String mf;

    public TLink() {
      super(0,0,0);
      @SuppressWarnings("unused")
      int i = 0;
    }

    boolean setLink(int[] t1, int[] t2, String line) {
      this.line = line;
      idx = linkCount++;
      if (nodeIds[1] == null)
        nodeIds[1] = nodeIds[0];
      typeBondOrder = getBondType(type, topoOrder);
      // only a relative lattice change is necessary here.
      translations[0] = P3d.new3(t1[0], t1[1], t1[2]);
      translations[1] = P3d.new3(t2[0], t2[1], t2[2]);
      System.out.println("TopoCifParser.setLink " + this);
      return true;
    }

    TAtom addAtom(TAtom atom) {
      if (tatoms == null)
        tatoms = new Lst<TAtom>();
      if (atom.nodeID != null) {
        atom = atom.getTClone();
        atom.nodeID = null;
      }
      atom.atomName = "Link_" + atom.linkID + "_" + atom.atomLabel;
      tatoms.addLast(atom);
      return atom;
    }

    /**
     * Take all actions prior to applying symmetry. Specifically, create any
     * nodes and atoms
     * 
     * @throws Exception
     */
    void finalizeLink() throws Exception {
      netID = (nodeIds[0] == null ? null : findNode(nodeIds[0], -1, null).netID);
      if (netID == null && netLabel == null) {
        if (nets.size() > 0)
          net = nets.get(0);
        else
          net = getNetFor(null, null, true);
      } else {
        net = getNetFor(netID, netLabel, true);
      }
      netLabel = net.label;
      net.nLinks++;
      if (selectedNet != null) {
        if (!selectedNet.equalsIgnoreCase(net.label)
            && !selectedNet.equalsIgnoreCase(net.id)) {
          return;
        }
      }
      finalizeLinkNode(0);
      finalizeLinkNode(1);

      // encode the associated atom sequence number with this link's id.

      if (tatoms != null) {
        int n = tatoms.size();
        net.hasAtoms = true;
        for (int i = n; --i >= 0;) {
          TAtom a = tatoms.get(i);
          a.idx1 = -idx - 1;
          a.atomName = netLabel + "_" + a.atomName;
          a.net = net;
          //          a.assocDist = new double[] { calculateDistance(linkNodes[0], a), calculateDistance(linkNodes[1], a) };
        }
        if (n >= 0) {
          mf = getMF(tatoms);
          reader.appendLoadNote("_topos_link " + id + " for net " + netLabel + " has formula " + mf);
        }
      }

      // set the Bond fields, encoding the order field with "link", id, and typeBondOrder

      order = TOPOL_LINK + (idx << LINK_TYPE_BITS) + typeBondOrder;
      distance = calculateDistance(linkNodes[0], linkNodes[1]);
      if (cartesianDistance != 0
          && Math.abs(distance - cartesianDistance) >= ERROR_TOLERANCE)
        System.err
            .println("Distance error! distance=" + distance + " for " + line);
      System.out.println(
          "link d=" + distance + " " + this + linkNodes[0] + linkNodes[1]);

      reader.asc.addBond(this);
      finalized = true;
    }

//    public String getMolecularFormula() {
//      return (mf == null ? (mf = getMF(tatoms)) : mf);
//    }

    /**
     * 
     * @param index
     *        0 or 1
     * @throws Exception
     */
    private void finalizeLinkNode(int index) throws Exception {

      String id = nodeIds[index];
      String atomLabel = nodeLabels[index];
      int op = symops[index];
      P3d trans = translations[index];

      // first check is for a node based on id
      TNode node = getNodeWithSym(id, atomLabel, op, trans);
      TNode node0 = node;
      if (node == null && id != null) {
        node = getNodeWithSym(id, null, -1, null);
      }
      // second check is for an atom_site atom with this label
      Atom atom = (node == null && atomLabel != null ? getAtomFromName(atomLabel) : null);
      // we now either have a node or an atom_site atom or we have a problem
      if (atom != null) {
        node = new TNode(atomCount++, atom, net, op, trans);
      } else if (node != null) {
        if (node0 == null)
          node = node.copy();
        node.linkSymop = op;
        node.linkTrans = trans;
        nodeLabels[index] = node.atomName; 
      } else {
        throw new Exception("_topol_link: no atom or node "
            + atomLabel + " line=" + line);
      }
      nodes.addLast(node);
      linkNodes[index] = node;
      // check for the same node (but different symmetry, of course)
      if (index == 1 && node == linkNodes[0]) {
        linkNodes[1] = node.copy();
      }
      node.finalizeNode(ops);
      if (node0 == null)
        applySymmetry(node, ops, op, trans);
      if (index == 0) {
        atomIndex1 = node.index;
      } else {
        atomIndex2 = node.index;
      }
    }

//    private void setNet(TNode node) {
//      if (net != null)
//        return;
//      net = (node == null ? getNetFor(netID, netLabel, true) : node.net);
//    }

    /**
     * Find a node that already matches this id and symmetry
     * 
     * @param nodeID
     * @param nodeLabel
     * @param op
     *        a symmetry operation [9...N-1] or -1 to ignore op and trans
     * @param trans
     *        the translation, ignored if op < 0
     * @return found node or null
     */
    private TNode getNodeWithSym(String nodeID, String nodeLabel, int op,
                                 P3d trans) {
      if (nodeID != null)
        return findNode(nodeID, op, trans);
      for (int i = nodes.size(); --i >= 0;) {
        TNode n = nodes.get(i);
        if (n.label.equals(nodeLabel)
            && (op == -1 && n.linkSymop == 0 && n.linkTrans.equals(ZERO)|| op == n.linkSymop && trans.equals(n.linkTrans)))
          return n;
      }
      return null;
    }

    Map<String, Object> getLinkInfo() {
      Hashtable<String, Object> info = new Hashtable<String, Object>();
      info.put("index", Integer.valueOf(idx + 1));
      if (id != null)
        info.put("id", id);
      info.put("netID", net.id);
      info.put("netLabel", net.label);
      if (nodeLabels[0] != null)
        info.put("nodeLabel1", nodeLabels[0]);
      if (nodeLabels[1] != null)
        info.put("nodeLabel2", nodeLabels[1]);
      if (nodeIds[0] != null)
        info.put("nodeId1", nodeIds[0]);
      if (nodeIds[1] != null)
        info.put("nodeId2", nodeIds[1]);
      info.put("distance", Double.valueOf(cartesianDistance));
      if (!Double.isNaN(distance))
        info.put("distance", Double.valueOf(distance));
      info.put("symops1", Integer.valueOf(symops[0] + 1));
      info.put("symops2", Integer.valueOf(symops[1] + 1));
      info.put("translation1", translations[0]);
      info.put("translation2", translations[1]);
      info.put("multiplicity", Integer.valueOf(multiplicity));
      if (type != null)
        info.put("type", type);
      info.put("voronoiSolidAngle", Double.valueOf(voronoiAngle));
      // derived
      info.put("atomIndex1", Integer.valueOf(i0 + linkNodes[0].index));
      info.put("atomIndex2", Integer.valueOf(i0 + linkNodes[1].index));
      if (bsAtoms != null && !bsAtoms.isEmpty())
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

  }

  /**
   * Find or create a net with this netID, giving it a default name "Net"+id
   * 
   * @param id
   * @return net, never null
   */
  public TNet getNetByID(String id) {
    for (int i = nets.size(); --i >= 0;) {
      TNet n = nets.get(i);
      if (n.id.equalsIgnoreCase(id))
        return n;
    }
    TNet n = new TNet(netCount++, id, "Net" + id, null);
    nets.addLast(n);
    return n;
  }

  public Atom getAtomFromName(String atomLabel) {
    return (atomLabel == null ? null : reader.asc.getAtomFromName(atomLabel));
  }

  double calculateDistance(P3d p1, P3d p2) {
    temp1.setT(p1);
    temp2.setT(p2);
    sym.toCartesian(temp1, true);
    sym.toCartesian(temp2, true);
    return temp1.distance(temp2);
  }

  /**
   * Find or create a TNet for this id and label.
   * 
   * @param id
   *        or null
   * @param label
   *        or null
   * @param forceNew
   *        true to create a new net
   * @return a net, or null if not forceNew and not found
   */
  public TNet getNetFor(String id, String label, boolean forceNew) {
    TNet net = null;
    if (id != null) {
      net = getNetByID(id);
      if (net != null && label != null && forceNew)
        net.label = label;
    } else if (label != null) {
      for (int i = nets.size(); --i >= 0;) {
        TNet n = nets.get(i);
        if (n.label.equalsIgnoreCase(label)) {
          net = n;
          break;
        }
      }
    }
    if (net == null) {
      if (!forceNew)
        return null;
      net = getNetByID(id == null ? "1" : id);
    }
    if (net != null && label != null && forceNew)
      net.label = label;
    return net;
  }

  /**
   * Find the node for this TAtom.
   * 
   * @param idx
   * @return the node or null
   */
  TNode getAssociatedNodeByIdx(int idx) {
    for (int i = nodes.size(); --i >= 0;) {
      TNode n = nodes.get(i);
      if (n.idx == idx)
        return n;
    }
    return null;
  }

  /**
   * Find the link for this TAtom.
   * 
   * @param idx
   * @return the link or null
   */
  TLink getAssoiatedLinkByIdx(int idx) {
    for (int i = links.size(); --i >= 0;) {
      TLink l = links.get(i);
      if (l.idx == idx)
        return l;
    }
    return null;
  }

  /**
   * Called from TLink and TAtom to find a node with the given symmetry.
   * 
   * @param nodeID
   * @param op match for linkSymop
   * @param trans match for linkTrans
   * @return the node, or null if no such node was found
   */
  public TNode findNode(String nodeID, int op, P3d trans) {
    for (int i = nodes.size(); --i >= 0;) {
      TNode n = nodes.get(i);
      if (n.id.equals(nodeID)
          && (op < 0 && n.linkSymop == 0 && n.linkTrans.equals(ZERO) || n.linkSymop == op && n.linkTrans.equals(trans)))
        return n;
    }
    return null;
  }

}
