package org.jmol.adapter.readers.cif;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.adapter.readers.cif.CifReader.Parser;
import org.jmol.adapter.smarter.Atom;
import org.jmol.api.SymmetryInterface;
import org.jmol.util.Edge;
import org.jmol.util.Logger;

import javajs.api.GenericCifDataParser;
import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.M4;
import javajs.util.P3;
import javajs.util.T3;
import javajs.util.V3;

/**
 * See https://www.iucr.org/resources/cif/dictionaries/cif_topology
 * 
 * @author Bob Hanson hansonr@stolaf.edu 2020.11.17
 * 
 */
public class TopoCifParser implements Parser {

  private static double ERROR_TOLERANCE = 0.001;

  /**
   * reader will be null if filter includes TOPOS_IGNORE
   */
  private CifReader reader;

  /**
   * list of _topol block data
   */
  private Lst<TopoLink> links = new Lst<TopoLink>();

  /**
   * types set by filter TOPOSE_TYPES in the format of one or more of {v, vw, hb} separated by "+";
   * default is v+hb
   */
  private String allowedTypes = "+v+hb+";

  /**
   * ensures that we only make one bond to any two atoms
   */
  private String bondlist = "";

  final private static String[] topolFields = { 
      /*0*/ "_topol_link_node_label_1",
      /*1*/ "_topol_link_node_label_2", 
      /*2*/ "_topol_link_distance",
      /*3*/ "_topol_link_site_symmetry_symop_1",
      /*4*/ "_topol_link_site_symmetry_translation_1_x",
      /*5*/ "_topol_link_site_symmetry_translation_1_y",
      /*6*/ "_topol_link_site_symmetry_translation_1_z",
      /*7*/ "_topol_link_site_symmetry_symop_2",
      /*8*/ "_topol_link_site_symmetry_translation_2_x",
      /*9*/ "_topol_link_site_symmetry_translation_2_y",
      /*10*/ "_topol_link_site_symmetry_translation_2_z", 
      /*11*/ "_topol_link_type",
      /*12*/ "_topol_link_multiplicity", 
      /*13*/ "_topol_link_voronoi_solidangle", 
      /*14*/ "_topol_link_site_symmetry_translation_1",
      /*15*/ "_topol_link_site_symmetry_translation_2"     
  };

  final private static byte topol_link_node_label_1 = 0;
  final private static byte topol_link_node_label_2 = 1;
  final private static byte topol_link_distance = 2;
  final private static byte topol_link_site_symmetry_symop_1 = 3;
  final private static byte topol_link_site_symmetry_translation_1_x = 4;
  final private static byte topol_link_site_symmetry_translation_1_y = 5;
  final private static byte topol_link_site_symmetry_translation_1_z = 6;
  final private static byte topol_link_site_symmetry_symop_2 = 7;
  final private static byte topol_link_site_symmetry_translation_2_x = 8;
  final private static byte topol_link_site_symmetry_translation_2_y = 9;
  final private static byte topol_link_site_symmetry_translation_2_z = 10;
  final private static byte topol_link_type = 11;
  final private static byte topol_link_multiplicity = 12;
  final private static byte topol_link_voronoi_solidangle = 13;
  // CIF2
  final private static byte topol_link_site_symmetry_translation_1 = 14;
  final private static byte topol_link_site_symmetry_translation_2 = 15;

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
  private class TopoLink {

    int idx;

    Atom a1;
    Atom a2;
    int op1, op2;
    P3 t1, t2, dt;
    String type;
    float voronoiAngle;
    int multiplicity;

    M4 m1, m2;
    P3 p1f, p2f;
    float d;
    int order;

    TopoPrimitive[] primitives;

    public BS symops;

    TopoLink(int index, Atom a1, Atom a2, float d, int op1, int[] t1, int op2,
        int[] t2, int multiplicity, String type, float vAngle) {
      this.idx = index;
      this.a1 = a1;
      this.a2 = a2;
      this.d = d;
      this.op1 = op1 - 1;
      this.op2 = op2 - 1;
      this.type = type;
      this.order = ("vw".equals(type) ? Edge.BOND_PARTIAL01
          : "hb".equals(type) ? Edge.BOND_H_REGULAR
              : Edge.BOND_COVALENT_SINGLE);
      // only a relative lattice change is necessary here.
      this.t1 = P3.new3(t1[0], t1[1], t1[2]);
      this.t2 = P3.new3(t2[0], t2[1], t2[2]);
      dt = P3.new3((t2[0] - t1[0]), (t2[1] - t1[1]), (t2[2] - t1[2]));
      this.multiplicity = multiplicity;
      this.voronoiAngle = vAngle;
    }

    Map<String, Object> getInfo() {
      Hashtable<String, Object> info = new Hashtable<String, Object>();
      info.put("label1", a1.atomName);
      info.put("label2", a2.atomName);
      info.put("distance", Float.valueOf(d));
      info.put("symop1", Integer.valueOf(op1 + 1));
      info.put("symop2", Integer.valueOf(op2 + 1));
      info.put("t1", t1);
      info.put("t2", t2);
      info.put("multiplicity", Integer.valueOf(multiplicity));
      info.put("type", type);
      info.put("voronoiSolidAngle", Float.valueOf(voronoiAngle));
      // derived
      info.put("atomIndex1", Integer.valueOf(a1.index));
      info.put("atomIndex2", Integer.valueOf(a2.index));
      info.put("index", Integer.valueOf(idx));
      info.put("op1", m1);
      info.put("op2", m2);
      info.put("dt", dt);
      info.put("primitive1", p1f);
      info.put("primitive2", p2f);
      int[] ops = new int[symops.cardinality()];
      for (int p = 0, i = symops.nextSetBit(0); i >= 0 ; i = symops.nextSetBit(i + 1)) {
        ops[p++] = i + 1;
      }
      info.put("primitiveSymops", ops);
      info.put("order", Integer.valueOf(order));
      return info;
    }

    /**
     * Primitives are set by doing the operations and adding dt to the second
     * point's position only. This works because we are going to unitize this
     * anyway, so only the relative final lattice translation for point 1 and
     * point 2 is needed.
     * 
     * @param sym
     * @param operations
     */
    void setPrimitives(SymmetryInterface sym, M4[] operations) {
      int nOps = operations.length;
      p1f = P3.new3(a1.x, a1.y, a1.z);
      p2f = P3.new3(a2.x, a2.y, a2.z);
      (m1 = operations[op1]).rotTrans(p1f);
      (m2 = operations[op2]).rotTrans(p2f);
      p2f.add(dt);
      primitives = new TopoPrimitive[nOps];
      for (int j = 0; j < nOps; j++) {
        TopoPrimitive prim = new TopoPrimitive(this, j + 1, sym, operations[j]);
        if (!prim.isValid)
          continue;
        primitives[j] = prim;
      }
    }

    public String info() {
      return "[link " + idx + " " + a1.atomName + " " + a2.atomName + " " + d + " " + type + "]";
    }
    
    @Override
    public String toString() {
      return info();
    }
  }

  /**
   * A class to hold the result of a "primitive" operation on a given link with
   * a given operator. The result includes two points and a vector. The first
   * point is "unitized" by translation back into the original unit cell. The
   * second point is displaced from that point by the appropriate vector. All
   * values are fractional coordinates.
   * 
   */
  private class TopoPrimitive {
    P3 p1u, p2u;
    V3 v12f;
    boolean isValid;
    int symop;

    TopoPrimitive(TopoLink link, int symop, SymmetryInterface sym, M4 op) {
      this.symop = symop;
      P3 p1 = new P3(), p2 = new P3();
      p1.setT(link.p1f);
      p2.setT(link.p2f);
      op.rotTrans(p1);
      op.rotTrans(p2);
      p1u = P3.newP(p1);
      sym.unitize(p1u);
      p2u = P3.newP(p2);
      p2u.add(V3.newVsub(p1u, p1));
      v12f = V3.newVsub(p2u, p1u);
      p1.setT(p1u);
      p2.setT(p2u);
      sym.toCartesian(p1, true);
      sym.toCartesian(p2, true);
      isValid = isEqualD(p1, p2, link.d);
//      if (!isValid)
//        System.out.println(p1 + " " + p2 + " " + p1.distance(p2) + " " + link.d + "\n" + op);
    }

    public String info() {
      return "op=" + symop + " pt=" + p1u + " v=" + v12f;
    }
    
    @Override
    public String toString() {
      return info();
    }
  }

  public TopoCifParser() {
    debugging = Logger.isActiveLevel(Logger.LEVEL_DEBUG);
  }

  /**
   * filter "TOPOS_TYPES=hb" will only load hydrogen bonds; options include v,
   * vw, and hb
   */
  @Override
  public TopoCifParser setReader(CifReader reader) {
    if (reader.checkFilterKey("TOPOS_IGNORE")) {
      return this;
    }
    this.reader = reader;
    String types = reader.getFilter("TOPOS_TYPES");
    if (types != null && types.length() > 1)
      types = "+" + types.substring(1).toLowerCase() + "+";
    if (reader.doApplySymmetry)
      reader.asc.setNoAutoBond();
    return this;
  }

  private int index;

  private boolean debugging;

  @Override
  public void processBlock(String key) throws Exception {
    if (reader == null)
      return;
    if (reader.ucItems != null) {
      reader.allow_a_len_1 = true;
      for (int i = 0; i < 6; i++)
        reader.setUnitCellItem(i, reader.ucItems[i]);
    }
    reader.parseLoopParameters(topolFields);
    while (reader.cifParser.getData()) {
      index++;
      String name1 = null, name2 = null;
      float d = 0;
      int op1 = 1, op2 = 1;
      int[] t1 = new int[3];
      int[] t2 = new int[3];
      String type = "v";
      int multiplicity;
      float angle = 0;

      type = reader.getField(topol_link_type);
      if (allowedTypes.indexOf("+" + type + "+") < 0)
        continue;

      Atom a1 = null, a2 = null;
      if ((a1 = reader.asc.getAtomFromName(
          name1 = reader.getField(topol_link_node_label_1))) == null
          || (a2 = reader.asc.getAtomFromName(
              name2 = reader.getField(topol_link_node_label_2))) == null) {
        Logger.warn("TopoCifParser atom " + (a1 == null ? name1 : name2)
            + " could not be found");
        continue;
      }

      d = getFloat(topol_link_distance);
      if (!(d > 0)) { // includes NaN
        Logger.warn("TopoCifParser invalid distance");
        continue;
      }

      multiplicity = getInt(topol_link_multiplicity);
      angle = getFloat(topol_link_voronoi_solidangle);
      op1 = getInt(topol_link_site_symmetry_symop_1);
      op2 = getInt(topol_link_site_symmetry_symop_2);

      String field;
      field = reader.getField(topol_link_site_symmetry_translation_1);
      if (field.length() > 1) {
        t1 = Cif2DataParser.getIntArrayFromStringList(field, 3);
      } else {
        t1[0] = getInt(topol_link_site_symmetry_translation_1_x);
        t1[1] = getInt(topol_link_site_symmetry_translation_1_y);
        t1[2] = getInt(topol_link_site_symmetry_translation_1_z);
      }
      field = reader.getField(topol_link_site_symmetry_translation_2);
      if (field.length() > 1) {
        t2 = Cif2DataParser.getIntArrayFromStringList(field, 3);
      } else {
        t2[0] = getInt(topol_link_site_symmetry_translation_2_x);
        t2[1] = getInt(topol_link_site_symmetry_translation_2_y);
        t2[2] = getInt(topol_link_site_symmetry_translation_2_z);
      }
      
      links.addLast(new TopoLink(index, a1, a2, d, op1, t1, op2, t2,
          multiplicity, type, angle));
    }
  }

  @Override
  public boolean finalizeReader() throws Exception {
    // opportunity to handle anything prior to applying symmetry
    if (reader == null)
      return false;
    reader.applySymmetryAndSetTrajectory();
    return true;
  }

  /**
   * Create a list of cartesians and then set the bonds.
   * 
   */
  @Override
  public void finalizeSymmetry(boolean haveSymmetry) throws Exception {
    if (reader == null || !haveSymmetry || !reader.doApplySymmetry)
      return;
    SymmetryInterface sym = reader.asc.getXSymmetry().getBaseSymmetry();
    int nOps = sym.getSpaceGroupOperationCount();
    M4[] operations = new M4[nOps];
    for (int i = 0; i < nOps; i++) {
      operations[i] = sym.getSpaceGroupOperationRaw(i);
    }
    P3[] carts = new P3[reader.asc.ac];
    Atom[] atoms = reader.asc.atoms;
    for (int i = reader.asc.ac; --i >= 0;) {
      carts[i] = P3.newP(atoms[i]);
      sym.toCartesian(carts[i], true);
    }
    int n = 0;
    BS bsConnected = new BS();
    int nLinks = links.size();
    for (int i = 0; i < nLinks; i++) {
      TopoLink link = links.get(i);
      link.setPrimitives(sym, operations);
      n += setBonds(i, atoms, carts, link, sym, nOps, bsConnected);
    }

    // If we have a network, remove all unconnected atoms.
    if (bsConnected.cardinality() > 0)
      reader.asc.bsAtoms = bsConnected;
    reader.appendLoadNote("TopoCifParser read " + nLinks + " links; created "
        + n + " edges and " + bsConnected.cardinality() + " nodes");

    // add model info
    Lst<Map<String, Object>> info = new Lst<Map<String, Object>>();
    for (int i = 0; i < nLinks; i++) {
      info.addLast(links.get(i).getInfo());
    }
    reader.asc.setCurrentModelInfo("topology", info);
  }

  /**
   * Find all bonds associated with this link by matching unitized atom
   * positions to unitized "primitive" operations on the link.
   * 
   * @param index
   * @param atoms
   * @param carts
   * @param link
   * @param sym
   * @param nOps
   * @param bsConnected
   * @return number of bonds created
   */
  private int setBonds(int index, Atom[] atoms, P3[] carts, TopoLink link,
                       SymmetryInterface sym, int nOps, BS bsConnected) {

    int nbonds = 0;

    // the two sites, meaning initial asymmetric unit indices

    BS bs1 = new BS();
    BS bs2 = new BS();
    for (int i = reader.asc.ac; --i >= 0;) {
      Atom a = atoms[i];
      if (a.atomSite == link.a1.atomSite) {
        bs1.set(i);
      }
      if (a.atomSite == link.a2.atomSite) {
        bs2.set(i);
      }
    }

    P3 pa = new P3();
    BS bsym = new BS();

    // For each atom in set 1, determine the 
    for (int i1 = bs1.nextSetBit(0); i1 >= 0; i1 = bs1.nextSetBit(i1 + 1)) {
      Atom at1 = atoms[i1];

      // determine which operators are consistent with atom1
      // by checking for the same unitized position for the atom as for the 
      // primitive for this operation

      bsym.clearAll();
      for (int i = 0; i < nOps; i++) {
        TopoPrimitive prim = link.primitives[i];
        if (prim == null)
          continue;
        pa.setT(at1);
        sym.unitize(pa);
        if (isEqualD(pa, prim.p1u, 0)) {
          if (debugging)
            Logger.debug("TopoCifParser " + link.info() + " primitive: " + prim.info());
          bsym.set(i);
        }
      }
      link.symops = bsym;
      
      // now loop through all atom2 possibilities

      for (int i2 = bs2.nextSetBit(0); i2 >= 0; i2 = bs2.nextSetBit(i2 + 1)) {

        Atom at2 = atoms[i2];

        // check first for same atom or wrong distance

        if (i1 == i2 || !isEqualD(carts[i1], carts[i2], link.d))
          continue;

        // now for each symmetry operation, check if the vector atom1->atom2 
        // is the same as the primitive vector for this operation

        V3 va12 = V3.newVsub(at2, at1);
        for (int i = bsym.nextSetBit(0); i >= 0; i = bsym.nextSetBit(i + 1)) {
          if (!isEqualD(va12, link.primitives[i].v12f, 0))
            continue;
          String key = "," + at1.index + "," + at2.index + ",";
          if (bondlist.indexOf(key) >= 0)
            continue;
          bondlist += key + at1.index + ",";
          nbonds++;
          if (debugging)
              Logger.debug(nbonds + " " + at1 + " " + at2 + " "+ at1.index + " " + at2.index);
          reader.asc.addNewBondWithOrderA(at1, at2, link.order);
          bsConnected.set(at1.index);
          bsConnected.set(at2.index);
        }
      }
    }
    return nbonds;
  }

  static boolean isEqualD(T3 p1, T3 p2, double d) {
    return Math.abs(p1.distance(p2) - d) < ERROR_TOLERANCE;
  }

  private int getInt(byte field) {
    return reader.parseIntStr(reader.getField(field));
  }

  private float getFloat(byte field) {
    return reader.parseFloatStr(reader.getField(field));
  }

}

// original test script topocif.spt, which also works, but uses DRAW instead:
//
//var s1xyz = symops[s1]
//var s2xyz = symops[s2]
//
//// ...get list of all atoms of type a1 in the asymmetric unit
//var atoms = {atomName=a1 and symop=1}
//
//
//
//// ...identify two base atoms, and their positions based on the _tx_ty_tz symmetries
//var atom1 = atoms[1]
//var atom2 = {atomName=a2}[1]
//
////print [a1 a2 s1 t1 s2 t2 d type mult "atoms" atoms "atom1" atom1 "atom2" atom2]
//
//var p1 = getTopoPosition(atom1, s1xyz, t1)
//var p2 = getTopoPosition(atom2, s2xyz, t2)
//
//
//
//// ...now loop through the atoms of type a1...
//for (var j = 1; j <= atoms.length; j++) {
//
// // ...determine the lattice offset from the base atom1
// var a = atoms[j]
// var latticeOffset = a.xyz - atom1.xyz
//
// // ...now loop through all the symmetry operators...
// for (var k = 1; k <= nsym; k++) {
//
//   // ... apply the symmetry to both p1 and p2, including lattice offset
//   var p1k = symop(symops[k], p1) + latticeOffset
//   var p2k = symop(symops[k], p2) + latticeOffset
//   draw ID @{"tb" + i + "_" + j + "_" + k} @p1k @p2k
//
// }
// 
//}
//}
