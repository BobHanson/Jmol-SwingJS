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
import java.util.List;
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
   * ChemDraw can mess up 3D completely with octahedral stereochemistry; setting
   * filter "no3D" ensures the raw 2D structure is returned.
   */
  private boolean no3D;
  
  class CDNode extends Atom {

    String warning;
    String id;
    int intID;
    boolean isValid = true;
    boolean isConnected;
    boolean isFragment;
//    boolean isNickname;
    boolean isExternalPt;
    String nodeType;
    String fragment;
    public String text;
    CDNode parentNode;
    Lst<CDNode> orderedExternalPoints; // ordered by ID
    Lst<Object[]> orderedAttachedBonds;
    CDBond internalBond;
    private String[] attachments;
    public boolean hasMultipleAttachments;
    CDNode attachedAtom;
    private boolean isGeneric;

    CDNode(String id, String nodeType, String fragment, CDNode parent) {
      this.id = id;
      this.fragment = fragment;
      this.atomSerial = intID = Integer.parseInt(id);
      this.nodeType = nodeType;
      this.parentNode = parent;
      isFragment = "Fragment".equals(nodeType) || "Nickname".equals(nodeType);
      isExternalPt = "ExternalConnectionPoint".equals(nodeType);
      isGeneric = "GenericNickname".equals(nodeType);
    }

    void setMultipleAttachments(String[] attachments) {
      this.attachments = attachments;
      hasMultipleAttachments = true;
    }
    /**
     * keep these in order
     * 
     * @param node
     */
    void addExternalPoint(CDNode node) {
      if (orderedExternalPoints == null)
        orderedExternalPoints = new Lst<CDNode>();
      int i = orderedExternalPoints.size();
      while (--i >= 0 && orderedExternalPoints.get(i).intID >= node.intID) {
        // continue;
      }
      orderedExternalPoints.add(++i, node);
    }

    void addAttachedAtom(CDBond bond, int pt) {
      if (orderedAttachedBonds == null)
        orderedAttachedBonds = new Lst<Object[]>();
      int i = orderedAttachedBonds.size();
      while (--i >= 0 && ((Integer) orderedAttachedBonds.get(i)[0]).intValue() > pt) {
        // continue;
      }
      orderedAttachedBonds.add(++i, new Object[] { Integer.valueOf(pt), bond });
    }
    
    void fixAttachments() {
      if (hasMultipleAttachments && attachedAtom != null) {
        int order = Edge.getBondOrderFromString("partial");
        int a1 = attachedAtom.index;
        for (int i = attachments.length; --i >= 0;) {
          Atom a = asc.getAtomFromName(attachments[i]);
          if (a != null)
            asc.addBondNoCheck(new Bond(a1, a.index, order));
        }
      }
      
      if (orderedExternalPoints == null || text == null)
        return;
      int n = orderedExternalPoints.size();
      if (n != orderedAttachedBonds.size()) {
        System.err.println("cannot fix attachments for " + text);
      }
      for (int i = 0; i < n; i++) {
        CDNode a = orderedExternalPoints.get(i);
        CDBond b = (CDBond) orderedAttachedBonds.get(i)[1];
        if (b.atomIndex2 == this.index) {
          b.atomIndex2 = a.index;
        } else {
          b.atomIndex1 = a.index;          
        }
      }
    }
    
    @Override
    public String toString() {
      return id + " " + elementSymbol + " " + elementNumber + " index=" + index + " ext=" + isExternalPt + " frag=" + isFragment + " " + elementSymbol + " " + x + " " + y;
    }

  }

  class CDBond extends Bond {
    String id1, id2;

    CDBond(String id1, String id2, int order) {
      super(asc.getAtomFromName(id1).index, asc.getAtomFromName(id2).index, order);
      this.id1 = id1;
      this.id2 = id2;
    }
    
    CDNode getOtherNode(CDNode a) {
      return (CDNode) asc.atoms[atomIndex1 == a.index ? atomIndex2 : atomIndex1];
    }

    @Override
    public String toString() {
      return super.toString() + " id1=" + id1 + " id2=" + id2;
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
  private String thisFragment;
  private CDNode thisNode;
  private Stack<CDNode> nodes = new Stack<CDNode>();
  private List<CDNode> nostereo = new ArrayList<CDNode>();

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
      fragments.push(thisFragment = id);
      return;
    }

    if ("n".equals(localName)) {
      setNode(id);
      return;
    }

    if ("t".equals(localName)) {
      textBuffer = "";
    }

    if ("s".equals(localName)) {
      setKeepChars(true);
    }

    if ("b".equals(localName)) {
      setBond();
      return;
    }

  }

  @Override
  void processEndElement(String localName) {
    if ("fragment".equals(localName)) {
      thisFragment = fragments.pop();
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
          parent.appendLoadNote("Warning: " + textBuffer + " " + thisNode.warning);
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
   */
  private void setNode(String id) {
    String nodeType = atts.get("nodetype");
    if (asc.bsAtoms == null)
      asc.bsAtoms = new BS();
    if (thisNode != null)
      nodes.push(thisNode);
    if ("_".equals(nodeType)) {
      atom = thisNode = null;
      return;
    }
    atom = thisNode = new CDNode(id, nodeType, thisFragment, thisNode);
    asc.addAtomWithMappedSerialNumber(atom);
    asc.bsAtoms.set(atom.index);

    String w = atts.get("warning");
    if (w != null) {
      thisNode.warning = PT.rep(w, "&apos;", "'");
      thisNode.isValid = (w.indexOf("ChemDraw can't interpret") < 0);
    }

    String element = atts.get("element");
    String s =  atts.get("genericnickname");
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
      thisNode.setMultipleAttachments(PT.split(s.trim(), " "));
    }

    if (Logger.debugging)
      Logger.info(
        "XmlChemDraw id=" + id + " " + element + " " + atom);
  }

  private boolean checkWarningOK(String warning) {
    return (warning == null
        || warning.indexOf("valence") >= 0 
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
   * Display  -- wedges and such
   * 
   * Display2 -- only important here for partial bonds
   * 
   * bonds to multiple attachments are not actually made.
   * 
   */
  private void setBond() {
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
      return;
    }
    CDBond b = (invertEnds ? new CDBond(atom2, atom1, order) : new CDBond(atom1, atom2, order));
    
    
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
      return;
    }
    else if (node2.hasMultipleAttachments) {
      node2.attachedAtom = node1;
      return;
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
      node1.internalBond = b;
      node2.parentNode.addExternalPoint(node2);
    }
    if (node2.isExternalPt) {
      node2.internalBond = b;
      node1.parentNode.addExternalPoint(node1);
    }
    
    asc.addBondNoCheck(b);

  }

  /**
   * Set the 2D or pseudo-3D coordinates of the atoms. ChemDraw 
   * pseudo-3D is just a z-layering of chunks of the molecule. Nothing really useful. 
   * These coordinates are ignored if there are any atoms also with 2D coordinates or
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
   * First fix all the attachments, tying together the atoms identified as ExternalConnectionPoints
   * with atoms of bonds indicating "BeginAttach" or "EndAttach". 
   * 
   * Then flag all unconnected atoms and also remove any wedges or hashes that are
   * associated with bonds to atoms that also have wavy bonds.  
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
   * Adjust the scale to have an average bond length of 1.45 Angstroms. 
   * This is just to get the structure in the range of other structures
   * rather than being huge. 
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
      if (a.isFragment || a.isExternalPt
          || !a.isConnected && (!a.isValid || a.elementNumber == 6 || a.elementNumber == 0)) {
//        System.out.println("removing atom " + a.id + " " + a.nodeType);
        asc.bsAtoms.clear(a.index);
      }
    }
  }


}
