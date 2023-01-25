/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-08-02 11:48:43 -0500 (Wed, 02 Aug 2006) $
 * $Revision: 5364 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.adapter.readers.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.Bond;
import org.jmol.api.JmolAdapter;
import org.jmol.util.Edge;
import org.jmol.util.Logger;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.PT;

/**
 * A reader for CambridgeSoft CDXML files.
 * 
 * See
 * https://www.cambridgesoft.com/services/documentation/sdk/chemdraw/cdx/IntroCDXML.htm
 * 
 * for the full, detailed specification.
 * 
 * Here we are just looking for simple aspects that could be converted to valid
 * 2D MOL files, SMILES, and InChI.
 * 
 * Fragments (such as CH2CH2OH) and "Nickname"-type fragments such as Ac and Ph,
 * are processed correctly. But their 2D representations are pretty nuts.
 * ChemDraw does not make any attempt to place these in reasonable locations.
 * That said, Jmol's 3D minimization does a pretty fair job, and the default is
 * to do that minimization.
 * 
 * If minimization and addition of H is not desired, use FILTER "NOH" or FILTER
 * "NO3D"
 * 
 * XmlChemDrawReader also serves as the reader for binary CDX files, as
 * CDXReader subclasses this class. See that class for details.
 * 
 * @author hansonr
 * 
 */

public class XmlCdxReader extends XmlReader {

  private double minX = Double.MAX_VALUE;
  private double minY = Double.MAX_VALUE;
  private double minZ = Double.MAX_VALUE;
  private double maxZ = -Double.MAX_VALUE;
  private double maxY = -Double.MAX_VALUE;
  private double maxX = -Double.MAX_VALUE;

  /**
   * setting filter "no3D" ensures the raw 2D structure is returned even though
   * there may be 3D coordinates, since a common option for the xyz attribute is
   * simply the same as 2D with a crude z offset.
   */
  private boolean no3D;

  /**
   * CDNode extends Atom in order to maintain information about fragments,
   * connectivity, and validity
   * 
   */
  class CDNode extends Atom {

    String warning;
    String id;
    int intID;
    boolean isValid = true;
    boolean isConnected;
    boolean isExternalPt;
    String nodeType;

    boolean isFragment; // could also be a Nickname
    /**
     * fragment ID for the fragment containing this node
     */
    String outerFragmentID;

    /**
     * fragment ID of this fragment node
     */
    String innerFragmentID;

    public String text;
    CDNode parentNode;
    /**
     * list of connection bonds, ordered by ID
     */
    Lst<Object[]> orderedConnectionBonds;
    /**
     * for an external point, the actual atom associated with it in the fragment  
     */
    CDNode internalAtom;
    /**
     * for a fragment, the list of external points for a fragment, ordered by sequence in the label
     */
    Lst<CDNode> orderedExternalPoints;

    /**
     * 0x0432 For multicenter attachment nodes or variable attachment nodes a
     * list of IDs of the nodes which are multiply or variably attached to this
     * node array of attachment id values;
     * 
     * for example, in ferrocene, we are attaching to all of the carbon atoms if
     * this node is the special point that indicates that attachment
     */
    private String[] attachments;
    /**
     * 0x0431 BondOrdering An ordering of the bonds to this node used for
     * stereocenters fragments and named alternative groups with more than one
     * attachment.
     * 
     */
    private String[] bondOrdering;
    /**
     * 0x0505 ConnectionOrder An ordered list of attachment points within a
     * fragment.
     * 
     */
    private String[] connectionOrder;

    public boolean hasMultipleAttachments;
    CDNode attachedAtom;
    private boolean isGeneric;

    CDNode(String id, String nodeType, String fragmentID, CDNode parent) {
      this.id = id;
      this.outerFragmentID = fragmentID;
      this.atomSerial = intID = Integer.parseInt(id);
      this.nodeType = nodeType;
      this.parentNode = parent;
      isFragment = "Fragment".equals(nodeType) || "Nickname".equals(nodeType);
      isExternalPt = "ExternalConnectionPoint".equals(nodeType);
      isGeneric = "GenericNickname".equals(nodeType);
    }

    public void setInnerFragmentID(String id) {
      innerFragmentID = id;
    }

    void setBondOrdering(String[] bondOrdering) {
      this.bondOrdering = bondOrdering;
    }

    void setConnectionOrder(String[] connectionOrder) {
      this.connectionOrder = connectionOrder;
    }

    void setMultipleAttachments(String[] attachments) {
      this.attachments = attachments;
      hasMultipleAttachments = true;
    }

    /**
     * keep these in order
     * 
     * @param externalPoint
     */
    void addExternalPoint(CDNode externalPoint) {
      if (orderedExternalPoints == null)
        orderedExternalPoints = new Lst<CDNode>();
      int i = orderedExternalPoints.size();
      while (--i >= 0 && orderedExternalPoints.get(i).intID >= externalPoint.internalAtom.intID) {
        // continue;
      }
      orderedExternalPoints.add(++i, externalPoint);
    }

    public void setInternalAtom(CDNode a) {
      internalAtom = a;
      if (parentNode == null) {
        // hmm
      } else {
        parentNode.addExternalPoint(this);
      }
    }

    void addAttachedAtom(CDBond bond, int pt) {
      if (orderedConnectionBonds == null)
        orderedConnectionBonds = new Lst<Object[]>();
      int i = orderedConnectionBonds.size();
      while (--i >= 0
          && ((Integer) orderedConnectionBonds.get(i)[0]).intValue() > pt) {
        // continue;
      }
      orderedConnectionBonds.add(++i, new Object[] { Integer.valueOf(pt), bond });
    }

    void fixAttachments() {
      if (hasMultipleAttachments && attachedAtom != null) {
        // something like Ferrocene
        int order = Edge.getBondOrderFromString("partial");
        int a1 = attachedAtom.index;
        for (int i = attachments.length; --i >= 0;) {
          CDNode a = (CDNode) objectsByID.get(attachments[i]);
          if (a != null)
            asc.addBondNoCheck(new Bond(a1, a.index, order));
        }
      }

      if (orderedExternalPoints == null || text == null)
        return;
      // fragments and Nicknames
      int n = orderedExternalPoints.size();
      if (n != orderedConnectionBonds.size()) {
        System.err.println(
            "XmlCdxReader cannot fix attachments for fragment " + text);
        return;
      }
      System.out.println(
          "XmlCdxReader attaching fragment " + outerFragmentID + " " + text);
      if (bondOrdering == null) {
        bondOrdering = new String[n];
        for (int i = 0; i < n; i++) {
          bondOrdering[i] = ((CDBond)orderedConnectionBonds.get(i)[1]).id;
        }
      }
      if (connectionOrder == null) {
        connectionOrder = new String[n];      
        for (int i = 0; i < n; i++) {
          connectionOrder[i] = orderedExternalPoints.get(i).id;
        }
      }
      
        for (int i = 0; i < n; i++) {
          CDBond b = (CDBond) objectsByID.get(bondOrdering[i]);
          CDNode pt = (CDNode) objectsByID.get(connectionOrder[i]);
          // When there is 
          CDNode a = pt.internalAtom;
//          System.out.println("internal a->pt " 
//          + V3d.newVsub(pt, a) + "\n" + a + "\n" + pt);
          updateExternalBond(b, a);
        }
//      
//      // fallback to original id-ordered plan; probably n == 1
//      for (int i = 0; i < n; i++) {
//        CDNode a = orderedExternalPoints.get(i);
//        CDBond b = (CDBond) orderedAttachedBonds.get(i)[1];
//        updateExternalBond(b, a);
//      }
    }

    /**
     * Replace the fragment connection (to this fragment node) in bond b with
     * the internal atom a.
     * 
     * @param bond2f
     * @param intAtom
     */
    private void updateExternalBond(CDBond bond2f, CDNode intAtom) {
      if (bond2f.atomIndex2 == index) {
        bond2f.atomIndex2 = intAtom.index;
//        System.out.println("other to intpt " 
//            + V3d.newVsub(asc.atoms[bond2f.atomIndex1], asc.atoms[bond2f.atomIndex2])
//            + "\n" + asc.atoms[bond2f.atomIndex2] + "\n" + asc.atoms[bond2f.atomIndex1]);
      } else if (bond2f.atomIndex1 == index) {
        bond2f.atomIndex1 = intAtom.index;
//        System.out.println("other to intpt " 
//            + V3d.newVsub(asc.atoms[bond2f.atomIndex2], asc.atoms[bond2f.atomIndex1])
//            + "\n" + asc.atoms[bond2f.atomIndex1] + "\n" + asc.atoms[bond2f.atomIndex2]);
//
      } else {
        System.err
            .println("XmlCdxReader attachment failed! " + intAtom + " " + bond2f);
      }
      
    }

    @Override
    public String toString() {
      return "[CDNode " + id + " " + elementSymbol + " " + elementNumber + " index=" + index
          + " ext=" + isExternalPt + " frag=" + isFragment + " " + elementSymbol
          + " " + x + " " + y +"]";
    }

  }

  class CDBond extends Bond {
    String id, id1, id2;

    CDBond(String id, String id1, String id2, int order) {
      super(((CDNode) objectsByID.get(id1)).index,
          ((CDNode) objectsByID.get(id2)).index, order);
      this.id = id;
      this.id1 = id1;
      this.id2 = id2;
    }

    CDNode getOtherNode(CDNode a) {
      return (CDNode) asc.atoms[atomIndex1 == a.index ? atomIndex2
          : atomIndex1];
    }

    @Override
    public String toString() {
      return "[CDBond " + id + " id1=" + id1 + " id2=" + id2 + super.toString() + "]";
    }

  }

  public XmlCdxReader() {
  }

  @Override
  protected void processXml(XmlReader parent, Object saxReader)
      throws Exception {
    is2D = true;
    if (parent == null) {
      processXml2(this, saxReader);
      parent = this;
    } else {
      no3D = parent.checkFilterKey("NO3D");
      noHydrogens = parent.noHydrogens;
      processXml2(parent, saxReader);
      this.filter = parent.filter;
    }
  }

  private Stack<String> fragments = new Stack<String>();
  private String thisFragmentID;
  private CDNode thisNode;
  private Stack<CDNode> nodes = new Stack<CDNode>();
  private List<CDNode> nostereo = new ArrayList<CDNode>();
  Map<String, Object> objectsByID = new HashMap<String, Object>();

  /**
   * temporary holder of style chunks within text objects
   */
  private String textBuffer;

  /**
   * true when this reader is being used after CDX conversion
   */
  public boolean isCDX;

  @Override
  public void processStartElement(String localName, String nodeName) {
    String id = atts.get("id");
    if ("fragment".equals(localName)) {
      objectsByID.put(id, setFragment(id));
      return;
    }

    if ("n".equals(localName)) {
      objectsByID.put(id, setNode(id));
      return;
    }

    if ("b".equals(localName)) {
      objectsByID.put(id, setBond(id));
      return;
    }

    if ("t".equals(localName)) {
      textBuffer = "";
    }

    if ("s".equals(localName)) {
      setKeepChars(true);
    }

  }

  private CDNode setFragment(String id) {
    fragments.push(thisFragmentID = id);
    CDNode fragmentNode = (thisNode == null || !thisNode.isFragment ? null
        : thisNode);
    if (fragmentNode != null) {
      fragmentNode.setInnerFragmentID(id);
    }
    String s = atts.get("connectionorder");
    if (s != null) {
      System.out.println(id + " ConnectionOrder is " + s);
      thisNode.setConnectionOrder(PT.split(s.trim(), " "));
    }
    return fragmentNode;
  }

  @Override
  void processEndElement(String localName) {
    if ("fragment".equals(localName)) {
      thisFragmentID = fragments.pop();
      return;
    }
    if ("n".equals(localName)) {
      thisNode = (nodes.size() == 0 ? null : nodes.pop());
      return;
    }
    if ("s".equals(localName)) {
      textBuffer += chars.toString();
    }

    if ("t".equals(localName)) {
      if (thisNode == null) {
        System.out.println("XmlChemDrawReader unassigned text: " + textBuffer);
      } else {
        thisNode.text = textBuffer;
        if (atom.elementNumber == 0) {
          System.err.println(
              "XmlChemDrawReader: Problem with \"" + textBuffer + "\"");
        }
        if (thisNode.warning != null)
          parent.appendLoadNote(
              "Warning: " + textBuffer + " " + thisNode.warning);
      }
      textBuffer = "";
    }

    setKeepChars(false);
  }

  /**
   * Set the atom information. Reading:
   * 
   * NodeType, Warning. Element, Isotope, Charge, xyz, p
   * 
   * 3D coordinates xyz is only used if there are no 2D p coordinates. This may
   * not be possible. I don't know. These aren't real 3D coordinates, just
   * enhanced z values.
   * 
   * @param id
   * @return thisNode
   */
  private CDNode setNode(String id) {
    String nodeType = atts.get("nodetype");
    if (asc.bsAtoms == null)
      asc.bsAtoms = new BS();
    if (thisNode != null)
      nodes.push(thisNode);
    if ("_".equals(nodeType)) {
      // internal Jmol code for ignored node
      atom = thisNode = null;
      return null;
    }

    atom = thisNode = new CDNode(id, nodeType, thisFragmentID, thisNode);
    asc.addAtomWithMappedSerialNumber(atom);
    asc.bsAtoms.set(atom.index);

    String w = atts.get("warning");
    if (w != null) {
      thisNode.warning = PT.rep(w, "&apos;", "'");
      thisNode.isValid = (w.indexOf("ChemDraw can't interpret") < 0);
    }

    String element = atts.get("element");
    String s = atts.get("genericnickname");
    if (s != null) {
      element = s;
    }

    atom.elementNumber = (short) (!checkWarningOK(w) ? 0
        : element == null ? 6 : parseIntStr(element));
    element = JmolAdapter.getElementSymbol(atom.elementNumber);
    s = atts.get("isotope");
    if (s != null)
      element = s + element;
    setElementAndIsotope(atom, element);

    s = atts.get("charge");
    if (s != null) {
      atom.formalCharge = parseIntStr(s);
    }

    boolean hasXYZ = (atts.containsKey("xyz"));
    boolean hasXY = (atts.containsKey("p"));
    if (hasXYZ && (!no3D || !hasXY)) {
      // probably hasXY must be true; hedging here
      is2D = false;
      setAtom("xyz");
    } else if (atts.containsKey("p")) {
      setAtom("p");
    }

    s = atts.get("attachments");
    if (s != null) {
      System.out.println(id + " Attachments is " + s);
      thisNode.setMultipleAttachments(PT.split(s.trim(), " "));
    }

    s = atts.get("bondordering");
    if (s != null) {
      System.out.println(id + " BondOrdering is " + s);
      thisNode.setBondOrdering(PT.split(s.trim(), " "));
    }

    if (Logger.debugging)
      Logger.info("XmlChemDraw id=" + id + " " + element + " " + atom);

    return thisNode;
  }

  private boolean checkWarningOK(String warning) {
    return (warning == null || warning.indexOf("valence") >= 0
        || warning.indexOf("very close") >= 0
        || warning.indexOf("two identical colinear bonds") >= 0);
  }

  /**
   * Process the bond tags. We only look at the following attributes:
   * 
   * B beginning atom (atom1)
   * 
   * E ending atom (atom2)
   * 
   * BeginAttach associates atom1 with a fragment
   * 
   * EndAttach associates atom2 with a fragment
   * 
   * Order -- the bond order
   * 
   * Display -- wedges and such
   * 
   * Display2 -- only important here for partial bonds
   * 
   * bonds to multiple attachments are not actually made.
   * 
   * @param id
   * @return the bond
   * 
   */
  private CDBond setBond(String id) {
    String atom1 = atts.get("b");
    String atom2 = atts.get("e");
    String a = atts.get("beginattach");
    int beginAttach = (a == null ? 0 : parseIntStr(a));
    a = atts.get("endattach");
    int endAttach = (a == null ? 0 : parseIntStr(a));
    String s = atts.get("order");
    String disp = atts.get("display");
    String disp2 = atts.get("display2");
    int order = Edge.BOND_ORDER_NULL;
    boolean invertEnds = false;
    if (disp == null) {
      if (s == null) {
        order = 1;
      } else if (s.equals("1.5")) {
        order = JmolAdapter.ORDER_AROMATIC;
      } else {
        if (s.indexOf(".") > 0 && !"Dash".equals(disp2)) {
          // partial only works with "dash" setting for second line
          s = s.substring(0, s.indexOf("."));
        }
        order = Edge.getBondOrderFromString(s);
      }
    } else if (disp.equals("WedgeBegin")) {
      order = JmolAdapter.ORDER_STEREO_NEAR;
    } else if (disp.equals("Hash") || disp.equals("WedgedHashBegin")) {
      order = JmolAdapter.ORDER_STEREO_FAR;
    } else if (disp.equals("WedgeEnd")) {
      invertEnds = true;
      order = JmolAdapter.ORDER_STEREO_NEAR;
    } else if (disp.equals("WedgedHashEnd")) {
      invertEnds = true;
      order = JmolAdapter.ORDER_STEREO_FAR;
    } else if (disp.equals("Wavy")) {
      order = JmolAdapter.ORDER_STEREO_EITHER;
    }
    if (order == Edge.BOND_ORDER_NULL) {
      // dative, ionic, hydrogen, threecenter
      System.err.println("XmlChemDrawReader ignoring bond type " + s);
      return null;
    }
    CDBond b = (invertEnds ? new CDBond(id, atom2, atom1, order)
        : new CDBond(id, atom1, atom2, order));

    CDNode node1 = (CDNode) asc.atoms[b.atomIndex1];
    CDNode node2 = (CDNode) asc.atoms[b.atomIndex2];

    if (order == JmolAdapter.ORDER_STEREO_EITHER) {
      if (!nostereo.contains(node1))
        nostereo.add(node1);
      if (!nostereo.contains(node2))
        nostereo.add(node2);
    }

    if (node1.hasMultipleAttachments) {
      node1.attachedAtom = node2;
      return b;
    } else if (node2.hasMultipleAttachments) {
      node2.attachedAtom = node1;
      return b;
    }

    if (node1.isFragment && beginAttach == 0)
      beginAttach = 1;
    if (node2.isFragment && endAttach == 0)
      endAttach = 1;
    if (beginAttach > 0) {
      (invertEnds ? node2 : node1).addAttachedAtom(b, beginAttach);
    }
    if (endAttach > 0) {
      (invertEnds ? node1 : node2).addAttachedAtom(b, endAttach);
    }
    if (node1.isExternalPt) {
      node1.setInternalAtom(node2);
    }
    if (node2.isExternalPt) {
      node2.setInternalAtom(node1);
    }

    asc.addBondNoCheck(b);

    return b;
  }

  /**
   * Set the 2D or pseudo-3D coordinates of the atoms. ChemDraw pseudo-3D is
   * just a z-layering of chunks of the molecule. Nothing really useful. These
   * coordinates are ignored if there are any atoms also with 2D coordinates or
   * for FILTER "NO3D". So, pretty much, the z coordinates are never used.
   * 
   * @param key
   */
  private void setAtom(String key) {
    String xyz = atts.get(key);
    String[] tokens = PT.getTokens(xyz);
    double x = parseDoubleStr(tokens[0]);
    double y = -parseDoubleStr(tokens[1]);
    double z = (key == "xyz" ? parseDoubleStr(tokens[2]) : 0);
    if (x < minX)
      minX = x;
    if (x > maxX)
      maxX = x;
    if (y < minY)
      minY = y;
    if (y > maxY)
      maxY = y;
    if (z < minZ)
      minZ = z;
    if (z > maxZ)
      maxZ = z;
    atom.set(x, y, z);
  }

  /**
   * Fix connections to Fragments and Nicknames, adjust stereochemistry for wavy
   * displays, flag invalid atoms, and adjust the scale to something more
   * molecular. Finalize the 2D/3D business.
   * 
   */
  @Override
  protected void finalizeSubclassReader() throws Exception {
    fixConnections();
    fixInvalidAtoms();
    centerAndScale();
    parent.appendLoadNote((isCDX ? "CDX: " : "CDXML: ") + (is2D ? "2D" : "3D"));
    asc.setInfo("minimize3D", Boolean.valueOf(!is2D && !noHydrogens));
    asc.setInfo("is2D", Boolean.valueOf(is2D));
    if (is2D) {
      optimize2D = !noHydrogens && !noMinimize;
      asc.setModelInfoForSet("dimension", "2D", asc.iSet);
      set2D();
    }
    // parent will be null for 
  }

  /**
   * First fix all the attachments, tying together the atoms identified as
   * ExternalConnectionPoints with atoms of bonds indicating "BeginAttach" or
   * "EndAttach".
   * 
   * Then flag all unconnected atoms and also remove any wedges or hashes that
   * are associated with bonds to atoms that also have wavy bonds.
   */
  private void fixConnections() {

    // fix attachments for fragments

    for (int i = asc.ac; --i >= 0;) {
      CDNode a = (CDNode) asc.atoms[i];
      if (a.isFragment || a.hasMultipleAttachments)
        a.fixAttachments();
    }

    // indicate all atoms that are connected

    for (int i = 0, n = asc.bondCount; i < n; i++) {
      Bond b = asc.bonds[i];
      if (b == null) {
        continue; // bond to nickname
      }
      CDNode a1 = (CDNode) asc.atoms[b.atomIndex1];
      CDNode a2 = (CDNode) asc.atoms[b.atomIndex2];
      a1.isConnected = true;
      a2.isConnected = true;
      if (nostereo.contains(a1) != nostereo.contains(a2)) {
        // wavy line, so no stereo bonds here
        b.order = 1;
      }
    }
  }

  /**
   * Adjust the scale to have an average bond length of 1.45 Angstroms. This is
   * just to get the structure in the range of other structures rather than
   * being huge.
   * 
   */
  private void centerAndScale() {
    if (minX > maxX)
      return;
    double sum = 0;
    int n = 0;
    double lenH = 1;
    for (int i = asc.bondCount; --i >= 0;) {
      Atom a1 = asc.atoms[asc.bonds[i].atomIndex1];
      Atom a2 = asc.atoms[asc.bonds[i].atomIndex2];
      double d = a1.distance(a2);
      if (a1.elementNumber > 1 && a2.elementNumber > 1) {
        sum += d;
        n++;
      } else {
        lenH = d;
      }
    }
    double f = (sum > 0 ? 1.45d * n / sum : lenH > 0 ? 1 / lenH : 1);
    // in case somehow ChemDraw uses Cartesians.
    if (f > 0.5)
      f = 1;

    double cx = (maxX + minX) / 2;
    double cy = (maxY + minY) / 2;
    double cz = (maxZ + minZ) / 2;
    for (int i = asc.ac; --i >= 0;) {
      Atom a = asc.atoms[i];
      a.x = (a.x - cx) * f;
      a.y = (a.y - cy) * f;
      a.z = (a.z - cz) * f;
    }
  }

  /**
   * Remove fragment, external point, or invalid unconnected nodes (including
   * unconnected carbon nodes, which can arise from deletions (in my experience)
   * and are then not noticed because they have no associated text.
   */
  private void fixInvalidAtoms() {
    for (int i = asc.ac; --i >= 0;) {
      CDNode a = (CDNode) asc.atoms[i];
      a.atomSerial = Integer.MIN_VALUE;
      if (a.isFragment || a.isExternalPt || !a.isConnected
          && (!a.isValid || a.elementNumber == 6 || a.elementNumber == 0)) {
        //        System.out.println("removing atom " + a.id + " " + a.nodeType);
        asc.bsAtoms.clear(a.index);
      }
    }
  }

}
