package org.jmol.adapter.readers.cif;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.adapter.readers.cif.CifReader.Parser;
import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.Bond;
import org.jmol.api.SymmetryInterface;
import org.jmol.symmetry.SymmetryOperation;
import org.jmol.util.Edge;

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
  public static final int LINK_TYPE_SINGLE   = 1;
  public static final int LINK_TYPE_DOUBLE   = 2;
  public static final int LINK_TYPE_TRIPLE   = 3;
  public static final int LINK_TYPE_QUADRUPLE   = 4;
  public static final int LINK_TYPE_QUINTUPLE   = 5;
  public static final int LINK_TYPE_SEXTUPLE    = 6;
  public static final int LINK_TYPE_SEPTUPLE   = 7;
  public static final int LINK_TYPE_OCTUPLE    = 8;
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
      + "SEX"
      + "SEP"
      + "OCT"
      + "ARO"
      + "POL"
      + "PI "
      + "HBO"
      + "VDW"; // also "W" ?

  static int getBondType(String type, int order) {
    type = type.toUpperCase();
    if (type.equals("V"))
      return (order == 0 ? LINK_TYPE_SINGLE : order);
    switch (type.charAt(0)) {
    case 'V':
      return LINK_TYPE_VDW;
    }
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
  private SymmetryInterface sym;
  
  // CifParser supports up to 100 fields
  final private static String[] topolFields = {
      /*0*/"_topol_net_id",
      /*1*/"_topol_net_label",
      /*2*/"_topol_net_special_details",
      /*3*/"_topol_link_id",
      /*4*/"_topol_link_net_id",
      /*5*/"_topol_link_node_id_1",
      /*6*/"_topol_link_node_id_2",
      /*7*/"_topol_link_node_label_1",
      /*8*/"_topol_link_node_label_2",
      /*9*/"_topol_link_atom_label_1",
      /*10*/"_topol_link_atom_label_2",
      /*11*/"_topol_link_symop_1",
      /*12*/"_topol_link_translation_1",
      /*13*/"_topol_link_translation_1_x",
      /*14*/"_topol_link_translation_1_y",
      /*15*/"_topol_link_translation_1_z",
      /*16*/"_topol_link_symop_2",
      /*17*/"_topol_link_translation_2",
      /*18*/"_topol_link_translation_2_x",
      /*19*/"_topol_link_translation_2_y",
      /*20*/"_topol_link_translation_2_z",
      /*21*/"_topol_link_distance",
      /*22*/"_topol_link_type",
      /*23*/"_topol_link_multiplicity",
      /*24*/"_topol_link_voronoi_solidangle",
      /*25*/"_topol_link_order",
      /*26*/"_topol_node_id",
      /*27*/"_topol_node_net_id",
      /*28*/"_topol_node_label",
      /*29*/"_topol_node_atom_label",
      /*30*/"_topol_node_symop",
      /*31*/"_topol_node_translation",
      /*32*/"_topol_node_translation_x",
      /*33*/"_topol_node_translation_y",
      /*34*/"_topol_node_translation_z",
      /*35*/"_topol_node_fract_x",
      /*36*/"_topol_node_fract_y",
      /*37*/"_topol_node_fract_z",
      /*38*/"_topol_node_chemical_formula_sum",
      /*39*/"_topol_atom_id",
      /*40*/"_topol_atom_atom_label",
      /*41*/"_topol_atom_node_id",
      /*42*/"_topol_atom_link_id",
      /*43*/"_topol_atom_symop",
      /*44*/"_topol_atom_translation",
      /*45*/"_topol_atom_translation_x",
      /*46*/"_topol_atom_translation_y",
      /*47*/"_topol_atom_translation_z",
      /*48*/"_topol_atom_fract_x",
      /*49*/"_topol_atom_fract_y",
      /*50*/"_topol_atom_fract_z",
      /*51*/"_topol_atom_element_symbol",
      /*52*/"_topol_link_site_symmetry_symop_1",
      /*53*/"_topol_link_site_symmetry_translation_1_x",
      /*54*/"_topol_link_site_symmetry_translation_1_y",
      /*55*/"_topol_link_site_symmetry_translation_1_z",
      /*56*/"_topol_link_site_symmetry_symop_2",
      /*57*/"_topol_link_site_symmetry_translation_2_x",
      /*58*/"_topol_link_site_symmetry_translation_2_y",
      /*59*/"_topol_link_site_symmetry_translation_2_z",
      /*60*/"_topol_link_site_symmetry_translation_1",
      /*61*/"_topol_link_site_symmetry_translation_2",
  };

  private final static byte topol_net_id = 0;
  private final static byte topol_net_label = 1;
  private final static byte topol_net_special_details = 2;
  private final static byte topol_link_id = 3;
  private final static byte topol_link_net_id = 4;
  private final static byte topol_link_node_id_1 = 5;
  private final static byte topol_link_node_id_2 = 6;
  private final static byte topol_link_node_label_1 = 7;
  private final static byte topol_link_node_label_2 = 8;
  private final static byte topol_link_atom_label_1 = 9;
  private final static byte topol_link_atom_label_2 = 10;
  private final static byte topol_link_symop_1 = 11;
  private final static byte topol_link_translation_1 = 12;
  private final static byte topol_link_translation_1_x = 13;
  private final static byte topol_link_translation_1_y = 14;
  private final static byte topol_link_translation_1_z = 15;
  private final static byte topol_link_symop_2 = 16;
  private final static byte topol_link_translation_2 = 17;
  private final static byte topol_link_translation_2_x = 18;
  private final static byte topol_link_translation_2_y = 19;
  private final static byte topol_link_translation_2_z = 20;
  private final static byte topol_link_distance = 21;
  private final static byte topol_link_type = 22;
  private final static byte topol_link_multiplicity = 23;
  private final static byte topol_link_voronoi_solidangle = 24;
  private final static byte topol_link_order = 25;
  private final static byte topol_node_id = 26;
  private final static byte topol_node_net_id = 27;
  private final static byte topol_node_label = 28;
  private final static byte topol_node_atom_label = 29;
  private final static byte topol_node_symop = 30;
  private final static byte topol_node_translation = 31;
  private final static byte topol_node_translation_x = 32;
  private final static byte topol_node_translation_y = 33;
  private final static byte topol_node_translation_z = 34;
  private final static byte topol_node_fract_x = 35;
  private final static byte topol_node_fract_y = 36;
  private final static byte topol_node_fract_z = 37;
  private final static byte topol_node_chemical_formula_sum = 38;
  private final static byte topol_atom_id = 39;
  private final static byte topol_atom_atom_label = 40;
  private final static byte topol_atom_node_id = 41;
  private final static byte topol_atom_link_id = 42;
  private final static byte topol_atom_symop = 43;
  private final static byte topol_atom_translation = 44;
  private final static byte topol_atom_translation_x = 45;
  private final static byte topol_atom_translation_y = 46;
  private final static byte topol_atom_translation_z = 47;
  private final static byte topol_atom_fract_x = 48;
  private final static byte topol_atom_fract_y = 49;
  private final static byte topol_atom_fract_z = 50;
  private final static byte topol_atom_element_symbol = 51;
  private final static byte topol_link_site_symmetry_symop_1 = 52;
  private final static byte topol_link_site_symmetry_translation_1_x = 53;
  private final static byte topol_link_site_symmetry_translation_1_y = 54;
  private final static byte topol_link_site_symmetry_translation_1_z = 55;
  private final static byte topol_link_site_symmetry_symop_2 = 56;
  private final static byte topol_link_site_symmetry_translation_2_x = 57;
  private final static byte topol_link_site_symmetry_translation_2_y = 58;
  private final static byte topol_link_site_symmetry_translation_2_z = 59;
  private final static byte topol_link_site_symmetry_translation_1 = 60;
  private final static byte topol_link_site_symmetry_translation_2 = 61;

  
  public TopoCifParser() {
  }

  /**
   * filter "TOPOS_TYPES=hb" will only load hydrogen bonds; options include v,
   * vw, and hb
   */
  @Override
  public TopoCifParser setReader(CifReader reader) {
    if (!reader.checkFilterKey("TOPOL")) {
      reader.appendLoadNote("This file has Topology analysis records.\nUse LOAD \"\" {1 1 1} FILTER \"TOPOL\"  to load the topology.");
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

  @Override
  public void ProcessRecord(String key, String data) {
    if (key.startsWith("_topol_net")) {
      processSingleNet(key, data);
    }
  }

  /**
   * A single net may be represented with no loop.
   * Generously allowing legacy format where _topol_net.id was a string.
   * 
   * @param key
   * @param data
   */
  private void processSingleNet(String key, String data) {
    if (key.equals(topolFields[topol_net_id])) {
      int n = reader.parseIntStr(data);
      if (n == Integer.MIN_VALUE) {
        //  legacy net.id was not an integer, now net.label
      } else {
        if (singleNet == null) {
          nets.addLast(singleNet = new TNet(netCount++, n, "Net" + n, null));
        } else {
          singleNet.id = n;
        }
        return;
      }
    } else if (key.equals(topolFields[topol_net_special_details])) {
      if (singleNet == null) {
        nets.addLast(singleNet = new TNet(netCount++, 1, null, data));
      } else {
        singleNet.specialDetails = data;
      }      
      return;
    } else if (!key.equals(topolFields[topol_net_label])) {
      return;
    }
    // net.label
    if (singleNet == null) {
      nets.addLast(singleNet = new TNet(netCount++, 1, data, null));
    } else {
      singleNet.label = data;
    }
  }

  /**
   * Process all nets. Note that the nets list is self-populating with a "Net1"
   * value if there is no TOPOL_NET section.
   * 
   * @throws Exception
   */
  private void processNets() throws Exception {
    while (cifParser.getData()) {
      int id = getInt(getDataValue(topol_net_id));
      if (id < 0)
        id = 0;
      String netLabel = getDataValue(topol_net_label);
      TNet net = getNetFor(id, netLabel);
      net.specialDetails = getDataValue(topol_net_special_details);
      net.line = reader.line;
    }
  }

  private void processLinks() throws Exception {
    while (cifParser.getData()) {
      String type = ("" + getDataValue(topol_link_type)).toLowerCase();
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
            // original, legacy -- net.id value is "net1"
            link.netLabel = field;
          } else {
            link.netID = getInt(field);
          }
          break;
        case topol_link_node_id_1:
          link.nodeIds[0] = getInt(field);
          break;
        case topol_link_node_id_2:
          link.nodeIds[1] = getInt(field);
          break;
        case topol_link_atom_label_1:
        case topol_link_node_label_1: // legacy
            link.atomLabels[0] = field;
          break;
        case topol_link_atom_label_2:
        case topol_link_node_label_2: // legacy
            link.atomLabels[1] = field;
          break;
        case topol_link_site_symmetry_symop_1:
        case topol_link_symop_1:
          link.symops[0] = getInt(field) - 1;
          break;
        case topol_link_site_symmetry_symop_2:
        case topol_link_symop_2:
          link.symops[1] = getInt(field) - 1;
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
    Lst<String> symops = reader.symops;
    int nOps = symops.size();
    ops = new M4[nOps];
    for (int i = 0; i < nOps; i++) {
      ops[i] = SymmetryOperation.getMatrixFromXYZ("!" + symops.get(i)); 
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
    if (reader.doApplySymmetry) {
      reader.applySymmetryAndSetTrajectory();
    }
    return true;
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
    // create the excluded atoms set -- atoms of bsAtoms that are linked 
    BS bsExclude = shiftBits(bsAtoms, bsConnected);
    // If we have a network, remove all unconnected atoms.
    if (bsConnected.cardinality() > 0) {
      reader.asc.bsAtoms = bsAtoms;
      reader.asc.atomSetInfo.put("bsExcludeBonding", bsExclude);
    }
    reader.appendLoadNote("TopoCifParser created " + bsConnected.cardinality() + " nodes and " + nLinks + " links");

    // add auxiliaryInfo.models[i].topology
    Lst<Map<String, Object>> info = new Lst<Map<String, Object>>();
    for (int i = 0, n = links.size(); i < n; i++) {
      info.addLast(links.get(i).getLinkInfo());
    }
    reader.asc.setCurrentModelInfo("topology", info);
    String script = "if (autobond) {delete !connected && !(atomName LIKE '*_Link*' or atomName LIKE '*_Node*')}; display displayed or " + nets.get(0).label + "__*";
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
    for (int pt = 0, i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
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
        if (link != null) {
          if (link.bsAtoms == null)
            link.bsAtoms = new BS();
          link.bsAtoms.set(i0 + a.index);
        }
      }
      bsAtoms.set(a.index);
    }

    boolean checkDistance = reader.doPackUnitCell;
    // finish up with bonds
    Bond[] bonds = reader.asc.bonds;
    for (int i = reader.asc.bondCount; --i >= bc0;) {
      Bond b = bonds[i];
      if (b.order >= TOPOL_GROUP) {
        // associated atoms - don't show this bond
        bonds[i] = null;
      } else if (b.order >= TOPOL_LINK) {
        // adjust link bond order, and add this bond to the link's bsBonds bitset
        b.order -= TOPOL_LINK;
        TLink link = getAssoiatedLinkByIdx(b.order >> LINK_TYPE_BITS);
        if (checkDistance && Math
            .abs(calculateDistance(atoms[b.atomIndex1], atoms[b.atomIndex2])
                - link.distance) >= ERROR_TOLERANCE) {
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

  static boolean isEqualD(T3 p1, T3 p2, double d) {
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

  private float getFloat(String f) {
    return (f == null ? Float.NaN : reader.parseFloatStr(f));
  }

  private class TNet {
    @SuppressWarnings("unused")
    String line;
    int id;
    int nLinks, nNodes;
    String label;
    String specialDetails;

    @SuppressWarnings("unused")
    int idx;
    boolean hasAtoms;

    TNet(int index, int id, String label, String specialDetails) {
      idx = index;
      this.id = id;
      this.label = label;
      this.specialDetails = specialDetails;
    }

    void finalizeNet() {
      String netKey = "," + id + ",";
      if (netNotes.indexOf(netKey) < 0) {
        reader.appendLoadNote(
            "Net " + label 
              + (specialDetails == null ? "" : " '" + specialDetails + "'")
              + " created with " + nLinks + " links and " + nNodes + " nodes.\n"
            + "Use DISPLAY " 
                +  (hasAtoms ? label + "__* to display it without associated atoms\nUse DISPLAY " + label 
                + "_* to display it with its associated atoms" : label + "* to display it"
                +"")
            );
      }
    }
  }

  private class TAtom extends Atom {

    // from CIF data:
    @SuppressWarnings("unused")
    int id;
    String atomLabel;
    int nodeID;
    int linkID;
    int symop = 0;
    private P3 trans = new P3();
    String formula;
    String line;


    // derived
    private boolean isFinalized;
    @SuppressWarnings("unused")
    int idx;
    
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

    void finalizeAtom() throws Exception {
      if (isFinalized)
        return;
      isFinalized = true;
      Atom a = getAtomFromName(atomLabel);
      setElementSymbol(this, formula);
      if (a == null && Float.isNaN(x)) {
        // no associated atom
        throw new Exception("TopoCIFParser.finalizeAtom no atom " + atomLabel + " line=" + line);
      }

      // check for addition to a TNode
      TNode node = null;
      if (nodeID > 0) {
        node = getNodeById(nodeID, -1, null);
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

      // transfer fields and set the symmetry op [tx ty tz]
      if (a != null && Float.isNaN(x)) {
        setTAtom(a, this);
        applySymmetry(this, ops, symop, trans);
      }

      // add this atom to the AtomSetCollection
      atomName = (node != null ? "Node_" + nodeID + "_"
          : link != null ? "Link_" + linkID + "_" : "TAtom_") + atomLabel;
      System.out.println("TAtom adding " + this);
      reader.addCifAtom(this, atomName, null, null);
    }

    private TLink getLinkById(int linkID) {
      for (int i = links.size(); --i >= 0;) {
        TLink l = links.get(i);
        if (l.id == linkID)
          return l;
      }
      return null;
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
      @SuppressWarnings("unused")
      int i = 0;// old transpiler needs this?
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
        net = getNetFor(netID, null);
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
        a = (atom == null ? getAtomFromName(atomLabel) : atom);
        setElementSymbol(this, formula == null ? a.elementSymbol : formula);
        if (a == null && !haveXYZ) {
          // no assigned atom_site
          // no associated atom
          // no defined xyz
          throw new Exception("TopoCIFParser.finalizeNode no atom " + atomLabel
              + " line=" + line);
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
      if ((a != null && a == atom) || !haveXYZ) {
        if (a != this) {
          setTAtom(a, this);
        }
        applySymmetry(this, ops, symop, trans);
      }
      atomName = net.label + "__";
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
   * A class to hold the TOPOL_LINK data item information and transform it as needed. A
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
    int[] nodeIds = new int[2];
    String[] atomLabels = new String[2];
    int[] symops = new int[2];
    P3[] translations = new P3[2];

    int netID;
    String netLabel;
    String type = "";
    int multiplicity;
    int topoOrder;
    float voronoiAngle;
    float cartesianDistance;


    // derived:

    int idx;
    TNet net;
    TNode[] linkNodes = new TNode[2];

    int typeBondOrder;

    Lst<TAtom> tatoms;
    BS bsAtoms;
    BS bsBonds;
    private String line;
    
    public TLink() {
      super();
      @SuppressWarnings("unused")
      int i = 0;
    }

    boolean setLink(int[] t1, int[] t2, String line) {
      this.line = line;
      idx = linkCount++;
      if (nodeIds[1] == 0)
        nodeIds[1] = nodeIds[0];
      typeBondOrder = getBondType(type, topoOrder);
      // only a relative lattice change is necessary here.
      translations[0] = P3.new3(t1[0], t1[1], t1[2]);
      translations[1] = P3.new3(t2[0], t2[1], t2[2]);
      System.out.println("TopoCifParser.setLink " + this);
      return true;
    }

    void addAtom(TAtom atom) {
      if (tatoms == null)
        tatoms = new Lst<TAtom>();
      tatoms.add(atom);
    }

    /**
     * Take all actions prior to applying symmetry. Specifically, create any
     * nodes and atoms
     * 
     * @throws Exception
     */
    void finalizeLink() throws Exception {
      finalizeLinkNode(0);
      finalizeLinkNode(1);
      
      // encode the associated atom sequence number with this link's id.
      
      if (tatoms != null) {
        net.hasAtoms = true;
        for (int i = tatoms.size(); --i >= 0;) {
          TAtom a = tatoms.get(i);
          a.sequenceNumber = -idx - 1;
          a.atomName = net.label + "_" + a.atomName;
        }        
      }

      // set the Bond fields, encoding the order field with "link", id, and typeBondOrder

      order = TOPOL_LINK + (idx << LINK_TYPE_BITS) + typeBondOrder;
      distance = calculateDistance(linkNodes[0], linkNodes[1]);
      if (cartesianDistance != 0 && Math.abs(distance - cartesianDistance) >= ERROR_TOLERANCE)
        System.err.println("Distance error! distance=" + distance + " for " + line);
      System.out.println("link d=" + distance + " " + this + linkNodes[0] + linkNodes[1]);
      
      reader.asc.addBond(this);
    }
    
    /**
     * 
     * @param index 0 or 1
     * @throws Exception
     */
    private void finalizeLinkNode(int index)  throws Exception {

      int id = nodeIds[index];
      String atomLabel = atomLabels[index];
      int op = symops[index];
      P3 trans = translations[index];

      // first check is for a node based on id
      TNode node = getNodeWithSym(id, atomLabel, op, trans);
      TNode node0 = node;
      if (node == null) {
        node = getNodeWithSym(id, atomLabel, -1, null);
      }
      // second check is for an atom_site atom with this label
      Atom atom = (node == null ? getAtomFromName(atomLabel) : null);
      // we now either have a node or an atom_site atom or we have a problem
      if (atom != null) {
        setNet(null);
        node = new TNode(atomCount++, atom, net, op, trans);
      } else if (node != null) {
        setNet(node);
        if (node0 == null)
          node = node.copy();
        node.linkSymop = op;
        node.linkTrans = trans;
      } else {
        throw new Exception(
            "TopoCIFParser.addNodeIfNull no atom or node " + atomLabel + " line=" + line);
      }
      nodes.addLast(node);
      linkNodes[index] = node;
      // check for the same node (but different symmetry, of course)
      if (index == 1 && node == linkNodes[0]) {
        linkNodes[1] = node.copy();
      }
      node.finalizeNode(ops);
      applySymmetry(node, ops, op, trans);
      if (index == 0)
        atomIndex1 = node.index;
      else 
        atomIndex2 = node.index;
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
    
    Map<String, Object> getLinkInfo() {
      Hashtable<String, Object> info = new Hashtable<String, Object>();
      info.put("index", Integer.valueOf(idx + 1));
      if (id > 0)
        info.put("id", Integer.valueOf(id));
      info.put("netID", Integer.valueOf(netID));
      info.put("netLabel", netLabel);
      if (atomLabels[0] != null)
        info.put("atomLabel1", atomLabels[0]);
      if (atomLabels[1] != null)
        info.put("atomLabel2", atomLabels[1]);
      if (nodeIds[0] > 0)
        info.put("nodeId1", Integer.valueOf(nodeIds[0]));
      if (nodeIds[1] > 0)
        info.put("nodeId2", Integer.valueOf(nodeIds[1]));
      info.put("distance", Float.valueOf(cartesianDistance));
      if (!Float.isNaN(distance))
        info.put("distance", Float.valueOf(distance));
      info.put("symops1", Integer.valueOf(symops[0] + 1));
      info.put("symops2", Integer.valueOf(symops[1] + 1));
      info.put("translation1", translations[0]);
      info.put("translation2", translations[1]);
      info.put("multiplicity", Integer.valueOf(multiplicity));
      if (type != null)
        info.put("type", type);
      info.put("voronoiSolidAngle", Float.valueOf(voronoiAngle));
      // derived
      info.put("atomIndex1", Integer.valueOf(i0 + linkNodes[0].index));
      info.put("atomIndex2", Integer.valueOf(i0 + linkNodes[1].index));
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
    TNet n = new TNet(netCount++, id, "Net" + id, null);
    nets.addLast(n);
    return n;
  }

  public Atom getAtomFromName(String atomLabel) {
    return (atomLabel == null ? null : reader.asc.getAtomFromName(atomLabel));
  }

  float calculateDistance(P3 p1, P3 p2) {
    temp1.setT(p1);
    temp2.setT(p2);
    sym.toCartesian(temp1, true);
    sym.toCartesian(temp2, true);
    return temp1.distance(temp2);
  }

  /**
   * Find or create a TNet for this id and label.
   * 
   * @param id or null
   * @param label or null
   * @return a net, never null
   */
  public TNet getNetFor(int id, String label) {
    TNet net = null;
    if (id > 0) {
      net = getNetByID(id);
    } else if (label != null) {
      for (int i = nets.size(); --i >= 0;) {
        TNet n = nets.get(i);
        if (n.label.equals(label)) {
          net = n;
          break;
        }
      }
    }
    if (net == null) {
      net = getNetByID(id < 1 ? 1 : id);
    }
    if (label != null)
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
   * @param op
   * @param trans
   * @return the node, or null if no such node was found
   */
  public TNode getNodeById(int nodeID, int op, P3 trans) {
    for (int i = nodes.size(); --i >= 0;) {
      TNode n = nodes.get(i);
      if (n.id == nodeID && (op < 0 || n.linkSymop == op && n.linkTrans.equals(trans)))
        return n;
    }
    return null;
  }

}

