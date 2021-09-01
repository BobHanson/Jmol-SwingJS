package org.jmol.adapter.readers.cif;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.adapter.readers.cif.CifReader.Parser;
import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.Bond;
import org.jmol.api.SymmetryInterface;
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
  private final static String recognizedTypes = "+v+hb+w+";
  public static final int LINK_TYPE_OTHER = 1;
  public static final int LINK_TYPE_HBOND = 2;
  public static final int LINK_TYPE_VW = 3;
 
  private static double ERROR_TOLERANCE = 0.001;

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
  Lst<Net> nets = new Lst<Net>();

  Net singleNet;
  
  int netCount;
  int linkCount;
  int atomCount;

  private int ac0 = -1, bc0;

  private GenericCifDataParser cifParser;

  /**
   * and indictor that we should abort, and why
   */
  String failed;

  /**
   * symmetry interface for this space group
   */
  SymmetryInterface sym;

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
    if (reader.checkFilterKey("NOTOPOL")) {
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
    if (reader.doApplySymmetry)
      reader.asc.setNoAutoBond();
    i0 = reader.baseAtomIndex;
    b0 = reader.baseBondIndex;
    return this;
  }

  @Override
  public boolean processBlock(String key) throws Exception {
    if (reader == null || !reader.doApplySymmetry || failed != null) {
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
   * Process all nets. Note that tne nets list is self-populating with a "Net1" value if there is no TOPOL_NET section.
   * @throws Exception
   */
  private void processNets() throws Exception {
    nets.addLast(
        new Net(netCount++, getInt(getField(topol_net_id)), getField(topol_net_label)));
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
          link.nodeID1 = getInt(field);
          break;
        case topol_link_node_id_2:
          link.nodeID2 = getInt(field);
          break;
        case topol_link_node_label_1:
        case topol_link_atom_label_1: // 
          if (link.nodeLabel1 == null)
            link.nodeLabel1 = field;
          break;
        case topol_link_atom_label_2:
        case topol_link_node_label_2:
          if (link.nodeLabel2 == null)
            link.nodeLabel2 = field;
          break;
        case topol_link_site_symmetry_symop_1:
        case topol_link_symop_1:
          link.nodeOp1 = getInt(field) - 1;
          break;
        case topol_link_site_symmetry_symop_2:
        case topol_link_symop_2:
          link.nodeOp2 = getInt(field) - 1;
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

  @Override
  public boolean finalizeReader() throws Exception {
    // opportunity to handle anything prior to applying symmetry
    if (reader == null)
      return false;
    cifParser = null;
    reader.applySymmetryToBonds = true;
    // finalize all topol_atom symmetries
    M4[] ops = getOperations();
    for (int i = 0; i < atoms.size(); i++) {
      atoms.get(i).finalizeAtom(ops);
    }
    // we do not have to finalize nodes directly -- finalizeLink will take care of that.
    for (int i = 0; i < links.size(); i++) {
      links.get(i).finalizeLink(ops);
    }
    reader.applySymmetryAndSetTrajectory();
    return true;
  }

  private M4[] getOperations() {
    if (!reader.doApplySymmetry)
      return null;
    reader.applySymmetryToBonds = true;
    sym = reader.asc.getXSymmetry().getSymmetry();
    sym.setFinalOperations(null, null,  0,  0, false, null);
    int nOps = sym.getSpaceGroupOperationCount();
    ops = new M4[nOps];
    for (int i = 0; i < nOps; i++) {
      ops[i] = sym.getSpaceGroupOperationRaw(i);
    }   
    return ops;
  }

  /**
   * Symmetry has been applied. Identify all of the connected atoms and process the group associations
   * 
   */
  @Override
  public void finalizeSymmetry(boolean haveSymmetry) throws Exception {
    if (reader == null || !haveSymmetry || !reader.doApplySymmetry)
      return;
    
    BS bsConnected = new BS();
    BS bsAtoms = new BS();
    int nLinks = processAssociations(bsConnected, bsAtoms);
    bsAtoms.or(bsConnected);

    // If we have a network, remove all unconnected atoms.
    if (bsConnected.cardinality() > 0)
      reader.asc.bsAtoms = bsAtoms;
    reader.appendLoadNote("TopoCifParser created " + bsConnected.cardinality() + " nodes and " + nLinks + " links");

    // add model info
    Lst<Map<String, Object>> info = new Lst<Map<String, Object>>();
    for (int i = 0, n = links.size(); i < n; i++) {
      info.addLast(links.get(i).getInfo());
    }
    reader.asc.setCurrentModelInfo("topology", info);
  }

  /**
   * Find and process all "bonds" associated with all links and nodes. 
   * 
   * BOND_LINK + index indicates linked nodes
   * 
   * BOND_GROUP + index indicates associated nodes
   * 
   * 
   * @param bsConnected to be filled 
   * @param bsAtoms 
   * 
   * @return number of bonds created
   */
  private int processAssociations(BS bsConnected, BS bsAtoms) {

    int nlinks = 0;
    Atom[] atoms = reader.asc.atoms;
   
    // set associations for links and nodes 
    for (int i = reader.asc.ac; --i >= ac0;) {
      Atom a = atoms[i];
      int id = a.sequenceNumber;
      if (id == Integer.MIN_VALUE || id == 0)
        continue;
      if (id > 0) {
        TNode node = getNodeById(id);
        if (node.bsAtoms == null)
          node.bsAtoms = new BS();
        node.bsAtoms.set(i0 + a.index);
      } else {
        TLink link = getLinkById(-id);
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
      if (!isEqualD(atoms[b.atomIndex1], atoms[b.atomIndex2], b.distance)) {
        bonds[i] = null;
        continue;
      }
      if (b.order >= TOPOL_GROUP) {
        // associated atoms - don't show this bond
        bonds[i] = null;
//        b.order = Edge.BOND_PARTIAL01;         
      } else if (b.order >= TOPOL_LINK) {
        // adjust link bond order, and add this bond to the link's bsBonds bitset
        b.order -= TOPOL_LINK;
        int id = b.order >> 2;
        TLink link = getLinkById(id);
        link.bsBonds.set(b0 + i);
        switch (b.order & 3) {
        case LINK_TYPE_VW:
          b.order = Edge.BOND_PARTIAL01;
          break;
        case LINK_TYPE_HBOND:
          b.order = Edge.BOND_H_REGULAR;
          break;
        case LINK_TYPE_OTHER:
          b.order = Edge.BOND_COVALENT_SINGLE;
          break;
        } 
        bsConnected.set(b.atomIndex1);
        bsConnected.set(b.atomIndex2);
        nlinks++;
      }
    }    
    return nlinks;
  }

  static boolean isEqualD(T3 p1, T3 p2, double d) {
    return (Double.isNaN(d) || Math.abs(p1.distance(p2) - d) < ERROR_TOLERANCE);
  }

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
          nets.addLast(singleNet = new Net(netCount++, n, "Net" + n));
        } else {
          singleNet.id = n;
        }
      } else if (key.endsWith("_label")) {
        if (singleNet == null) {
          nets.addLast(singleNet = new Net(netCount++, 1, data));
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
  
  private class Net {
    @SuppressWarnings("unused")
    int idx;
    int id;
    String label;

    Net(int index, int id, String label) {
      idx = index;
      this.id = id;
      this.label = label;
    }

  }

  private class TAtom extends Atom {

    int id;
    String atomLabel;
    int nodeID;
    int linkID;
    int symop = -1;
    P3 trans;
    String formula;

    Atom atom;

    boolean isFinalized;


    int idx;

    String line;
    
    TAtom() {
      super();
    }

    boolean setAtom(int[] a, String line) {
      this.line = line;
      idx = atomCount++;
      trans = P3.new3(a[0], a[1], a[2]);
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
      setElementSymbol();
      if (a == null && Float.isNaN(x)) {
        // no associated atom
        throw new Exception(
            "TopoCIFParser.finalizeAtom no atom " + atomLabel);
      }

      // check for addition to a TNode
      TNode node = null;
      if (nodeID > 0)
        node = getNodeById(nodeID);
      else if (atomLabel != null)
        node = (TNode) getTopo(nodes, atomLabel, 'n');
      if (node != null) {
        node.addAtom(this);
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
        setT(a);
        applySymmetry(ops, symop, trans);
      }
      
      // add this atom to the AtomSetCollection
      atomName = "TAtom_" + atomLabel;
      reader.asc.addAtom(this);
    }

    public void setElementSymbol() {
      String name = atomName;
      if (formula == null) {
        atomName = (atomName == null ? "X" : atomName.substring(atomName.indexOf('_') + 1));
      } else {
        atomName = formula;
      }
      getElementSymbol();
      atomName = name;
    }

    /**
     * Apply the symmetry and translation
     * @param ops
     * @param op
     * @param t
     */
    public void applySymmetry(M4[] ops, int op, T3 t) {
      if (op >= 0) {
        if (op > 1 || t.x != 0 || t.y != 0 || t.z != 0) {
          if (op > 1)
            ops[op].rotTrans(this);
          this.add(t);
        }
      }
    }
  }

  private class TNode extends TAtom {

    int netID;
    String netLabel;
    String label;
    
    Lst<TAtom> tatoms;
    BS bsAtoms = null;
    
    int linkSymop = 0;
    P3 linkTrans = new P3();
    
    TNode() {
      super();
    }

    public TNode(int idx, Atom atom, String netLabel, int op, P3 trans) {
      super();
      this.idx = idx;
      this.label = this.atomName = this.atomLabel = atom.atomName;
      this.formula = atom.getElementSymbol();
      this.netLabel = netLabel;
      this.linkSymop = op;
      this.linkTrans = trans;
      setT(atom);
      this.atom = atom;
    }

    boolean setNode(int[] a, String line) {
      if (!setAtom(a, line)) {        
        return false;
      }
      if (label == null)
        label = atomLabel;
      System.out.println("TopoCifParser.setLink " + this);
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
      if (label == null)
        label = atomLabel;
      // null atomName means no data for that
      Atom a = (atom != null ? atom : atomLabel == null ? null : reader.asc.getAtomFromName(atomLabel));
      boolean haveXYZ = !Float.isNaN(x);
      if (a == null) {
        // no associated atom
        if (!haveXYZ) {
          // no defined xyz
          if (tatoms.size() == 0) {
            // no associated TAtoms
            throw new Exception(
                "TopoCIFParser.finalizeNode no atom " + atomLabel);
          }
          setCentroid(this, tatoms);
          if (tatoms.size() == 1)
            tatoms = null;
        }
        a = this;
      }
      if (a == atom || !haveXYZ) {
        setT(a);
        if (ops != null)  
          applySymmetry(ops, symop, trans);
      }
      String s = (label != null ? label : "Node" + id);
      atomName = "TAtom_" + (atomLabel != null ? atomLabel : s);
      setElementSymbol();
      Net net = getNetFor(netID, netLabel);
      netLabel = net.label;
      netID = net.id;      
      atomName = net.label + "_" + s;
      reader.addCifAtom(this, atomName, null, null);
      //sequenceNumber = id;
      if (tatoms != null) {
        for (int i = tatoms.size(); --i >= 0;) {
          TAtom ta = tatoms.get(i);
          ta.sequenceNumber = id;
//          reader.asc.addNewBondWithOrderA(this, ta, TOPOL_GROUP + id);
        }        
      }
    }

    public String info() {
      return "[node " + idx + " " + label + "/" + atomName + " "
          + super.toString() + "]";
    }

    @Override
    public String toString() {
      return info();
    }

    public TNode copy() {
      return (TNode) clone();
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
    int nodeID1, nodeID2;
    String nodeLabel1, nodeLabel2; 
    int nodeOp1 = -1, nodeOp2 = -1;
    P3 nodeTrans1, nodeTrans2;
    String type = "";
    int multiplicity;
    int topoOrder;
    float voronoiAngle;
    float cartesianDistance;


    // derived:

    int idx;
    TNode node1, node2;
    P3 dt;
    int typeBondOrder;

    Lst<TAtom> tatoms;
    BS bsAtoms = null;
    BS bsBonds = new BS();
    private String line;
    
    public TLink() {
      super();
    }

    boolean setLink(int[] a1, int[] a2, String line) {
      this.line = line;
      Net net = getNetFor(netID, netLabel);
      netLabel = net.label;
      netID = net.id;
      idx = linkCount++;
      typeBondOrder = ("vw".equals(type) || "w".equals(type) ? LINK_TYPE_VW
          : "hb".equals(type) ? LINK_TYPE_HBOND
              : LINK_TYPE_OTHER);
      // only a relative lattice change is necessary here.
      dt = new P3();
      nodeTrans1 = P3.new3(a1[0], a1[1], a1[2]);
      nodeTrans2 = P3.new3(a2[0], a2[1], a2[2]);
      dt.sub2(nodeTrans2, nodeTrans1);
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
      if (nodeID1 > 0)
        info.put("id1", Integer.valueOf(nodeID1));
      if (nodeID2 > 0)
        info.put("id2", Integer.valueOf(nodeID2));
      info.put("distance", Float.valueOf(cartesianDistance));
      info.put("netID", Integer.valueOf(netID));
      info.put("netLabel", netLabel);
      info.put("label1", node1.atomName);
      info.put("label2", node2.atomName);
      if (!Float.isNaN(distance))
        info.put("distance", Float.valueOf(distance));
      info.put("symop1", Integer.valueOf(nodeOp1 + 1));
      info.put("symop2", Integer.valueOf(nodeOp2 + 1));
      info.put("translation1", nodeTrans1);
      info.put("translation2", nodeTrans2);
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
      return "[link " + (idx + 1) + " " + nodeLabel1 + " " + nodeLabel2 + " "
          + distance + " " + type + "]";
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
     * 
     * @throws Exception
     */
    void finalizeLink(M4[] ops) throws Exception {
      node1 = addLinkNodeIfNull(nodeID1, nodeLabel1, nodeOp1, nodeTrans1, netID);
      node2 = addLinkNodeIfNull(nodeID2, nodeLabel2, nodeOp2, nodeTrans2, netID);
      if (node1 == null || node2 == null) {
        Logger.warn("TopoCifParser atom "
            + (node1 == null ? nodeLabel1 : nodeLabel2) + " could not be found");
        return;
      }
      // check for the same node (but different symmetry, then)
      if (node1 == node2) {
        node2 = node1.copy();
      }
      node1.finalizeNode(ops);
      node2.finalizeNode(ops);
      
      // apply symmetry
      node1.applySymmetry(ops, nodeOp1, nodeTrans1);
      node2.applySymmetry(ops, nodeOp2, nodeTrans2);
 
      // set the Bond fields, encoding the order field with "link", id, and typeBondOrder
      atomIndex1 = node1.index;
      atomIndex2 = node2.index;
      distance = node1.distance(node2);            
      order = TOPOL_LINK + (id << 2) + typeBondOrder;

      System.out.println("link " + distance + " " + this);
      
      // encode the associated atom sequence number with this link's id.
      
      if (tatoms != null) {
        TAtom aa = tatoms.get(0);
        aa.finalizeAtom(ops);
        aa.sequenceNumber = -id;
        for (int i = tatoms.size(); --i >= 1;) {
          TAtom a = tatoms.get(i);
          a.finalizeAtom(ops);
          a.sequenceNumber = -id;
          reader.asc.addNewBondWithOrderA(aa, a, TOPOL_GROUP + id);
        }        
      }

      reader.asc.addBond(this);
    }
    
    /**
     * @param id
     * @param nodeLabel
     * @param nodeOp
     * @param nodeTrans
     * @param netID
     * @return the node
     * @throws Exception
     */
    public TNode addLinkNodeIfNull(int id, String nodeLabel, int nodeOp,
                                   P3 nodeTrans, int netID)
        throws Exception {

      // first check is for a node based on id
      TNode node = getNodeWithSym(id, nodeLabel, nodeOp, nodeTrans);
      if (node == null) {
        node = getNodeWithSym(id, nodeLabel, -1, null);
      }
      // second check is for an atom_site atom with this label
      Atom atom = (node == null ? reader.asc.getAtomFromName(nodeLabel) : null);
      // we now either have a node or an atom_site atom or we have a problem
      if (atom != null) {
        node = new TNode(atomCount++, atom, getNetByID(netID).label, nodeOp, nodeTrans);
      } else if (node != null) {
        node = node.copy();
        node.linkSymop = nodeOp;
        node.linkTrans = nodeTrans;
      } else {
        throw new Exception(
            "TopoCIFParser.addNodeIfNull no atom or node " + nodeLabel);
      }
      nodes.addLast(node);
      return node;
    }

  }

  public Net getNetByID(int netID) {
    for (int i = nets.size(); --i >= 0;) {
      Net n = nets.get(i);
      if (n.id == netID)
        return n;
    }
    Net n = new Net(netCount++, netID, "Net" + netID);
    nets.addLast(n);
    return n;
  }

  public TNode getNodeWithSym(int nodeID, String nodeLabel, int op,
                              P3 trans) {
    if (nodeID > 0) 
      return getNodeById(nodeID);
    for (int i = nodes.size(); --i >= 0;) {
      TNode n = nodes.get(i);
      if (n.label.equals(nodeLabel) && (op == -1 || op == n.linkSymop && trans.equals(n.linkTrans)))
        return n;
    }
    return null;
  }

  public Net getNetFor(int netID, String netLabel) {
    Net net = null;
    if (netID > 0) {
      net = getNetByID(netID);
    } else if (netLabel != null) {
      net = (Net) getTopo(nets, netLabel, 'N');
    }
    if (net == null)
      net = getNetByID(1);
  return net;
}

  public TLink getLinkById(int linkID) {
    for (int i = links.size(); --i >= 0;) {
      TLink l = links.get(i);
      if (l.id == linkID)
        return l;
    }
    return null;
  }

  public TNode getNodeById(int nodeID) {
    for (int i = nodes.size(); --i >= 0;) {
      TNode n = nodes.get(i);
      if (n.id == nodeID)
        return n;
    }
    return null;
  }

  static void setCentroid(TAtom a, Lst<TAtom> tatoms) {
    a.x = a.y = a.z = 0;
    int n = tatoms.size();
    for (int i = n; --i >= 0;)
      a.add(tatoms.get(i));
    a.x /= n;
    a.y /= n;
    a.z /= n;
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
        if (((Net) o).label.equals(label))
          return o;
        continue;
      default:
        return null;
      }
    }
    return null;
  }


}

