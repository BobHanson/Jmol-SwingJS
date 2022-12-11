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

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.Bond;
import org.jmol.api.JmolAdapter;

import javajs.util.BS;
import javajs.util.PT;

/**
 * An chem3d c3xml reader
 */

public class XmlChemDrawReader extends XmlReader {

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

  static class CDAtom extends Atom {

    String warning;
    String id;
    boolean isValid = true;
    boolean isConnected;
    boolean isFragment;
    boolean isNickname;
    boolean isConnectionPt;
    String nodeType;

    CDAtom(String id, String nodeType) {
      this.id = id;
      this.atomSerial = Integer.parseInt(id);
      this.nodeType = nodeType;
      isFragment = "Fragment".equals(nodeType);
      isNickname = "Nickname".equals(nodeType);
      isConnectionPt = "ExternalConnectionPoint".equals(nodeType);
    }

  }

  static class CDBond extends Bond {
    String id1, id2;

    CDBond(String id1, String id2, int order) {
      this.id1 = id1;
      this.id2 = id2;
      this.order = order;
    }
  }

  public XmlChemDrawReader() {
  }

  @Override
  protected void processXml(XmlReader parent, Object saxReader)
      throws Exception {
    is2D = true;
    no3D = parent.checkFilterKey("NO3D");
    noHydrogens = parent.noHydrogens;
    processXml2(parent, saxReader);
    this.filter = parent.filter;
  }

  @Override
  public void processStartElement(String localName, String nodeName) {
    if ("fragment".equals(localName)) {
      return;
    }

    if ("n".equals(localName)) {
      if (asc.bsAtoms == null)
        asc.bsAtoms = new BS();

      String id = atts.get("id");
      String nodeType = atts.get("nodetype");
      atom = new CDAtom(id, nodeType);

      String warning = atts.get("warning");
      if (warning != null) {
        ((CDAtom) atom).warning = PT.rep(warning, "&apos;", "'");
        ((CDAtom) atom).isValid = (warning
            .indexOf("ChemDraw can't interpret") < 0);
      }

      // temporary only
      String element = atts.get("element");
      atom.elementNumber = (short) (warning != null
          && warning.indexOf("valence") < 0 && warning.indexOf("very close") < 0 ? 0
              : element == null ? 6 : parseIntStr(element));
      element = JmolAdapter.getElementSymbol(atom.elementNumber);
      if (atom.elementNumber == 0) {
        System.err.println("XmlChemDrawReader: Could not read elementSymbol for " + element);
      }
      String isotope = atts.get("isotope");
      if (isotope != null)
        element = isotope + element;
      setElementAndIsotope(atom, element);
      String s = atts.get("charge");
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

      asc.addAtomWithMappedSerialNumber(atom);
      asc.bsAtoms.set(atom.index);
      return;
    }

    if ("s".equals(localName)) {
      if (((CDAtom) atom).warning != null) {
        setKeepChars(true);
      }
    }

    if ("b".equals(localName)) {
      String atom1 = atts.get("b");
      String atom2 = atts.get("e");
      boolean invertEnds = false;
      int order = (atts.containsKey("order") ? parseIntStr(atts.get("order"))
          : 1);
      String buf = atts.get("display");
      if (buf != null) {
        if (buf.equals("WedgeEnd")) {
          invertEnds = true;
          order = JmolAdapter.ORDER_STEREO_NEAR;
        } else if (buf.equals("WedgeBegin")) {
          order = JmolAdapter.ORDER_STEREO_NEAR;
        } else if (buf.equals("Hash") || buf.equals("WedgedHashBegin")) {
          order = JmolAdapter.ORDER_STEREO_FAR;
        } else if (buf.equals("WedgedHashEnd")) {
          invertEnds = true;
          order = JmolAdapter.ORDER_STEREO_FAR;
        }
      }
      asc.addBondNoCheck(new CDBond((invertEnds ? atom2 : atom1),
          (invertEnds ? atom1 : atom2), order));
      return;
    }

  }

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

  @Override
  void processEndElement(String localName) {
    if ("s".equals(localName)) {
      String w = ((CDAtom) atom).warning;
      if (w != null) {
        String group = chars.toString();
        parent.appendLoadNote("Warning: " + group + " " + w);
      }
    }

    setKeepChars(false);
  }

  @Override
  protected void finalizeSubclassReader() throws Exception {
    fixConnections();
    fixInvalidAtoms();
    center();
//    System.out.println("bsAtoms = " + asc.bsAtoms);
    asc.setInfo("minimize3D", Boolean.valueOf(!is2D && !noHydrogens));
    set2D();
    asc.setInfo("is2D", Boolean.valueOf(is2D));
    if (is2D)
      asc.setModelInfoForSet("dimension", "2D", asc.iSet);
    parent.appendLoadNote("ChemDraw CDXML: " + (is2D ? "2D" : "3D"));
  }

  /**
   * Remove all atoms that are ChemDraw-invalid and not connected, presuming
   * them to be extraneous labels of some sort.
   * 
   */
  private void fixConnections() {
    for (int i = 0, n = asc.bondCount; i < n; i++) {
      Bond b = asc.bonds[i];
      if (b == null)
        continue; // bond to nickname

      CDAtom a1 = (CDAtom) asc.getAtomFromName(((CDBond) b).id1);
      CDAtom a2 = (CDAtom) asc.getAtomFromName(((CDBond) b).id2);
      a1.isConnected = true;
      a2.isConnected = true;
      Atom pt = (a1.isFragment || a1.isNickname || a1.isConnectionPt? a1 
          : a2.isFragment || a2.isNickname || a2.isConnectionPt ? a2 : null);
      if (pt != null) {
        // ExternalConnectionPoint
        for (int j = asc.bsAtoms.nextSetBit(0); j >= 0; j = asc.bsAtoms
            .nextSetBit(j + 1)) {
          CDAtom a = (CDAtom) asc.atoms[j];
          if (a.isFragment || a.isNickname)
            continue;
          if (Math.abs(a.x - pt.x) < 0.1d && Math.abs(a.y - pt.y) < 0.1d) {
            if (pt == a1) {
              a1 = a;
            } else {
              a2 = a;
            }
            break;
          }
        }
      }
      b.atomIndex1 = a1.index;
      b.atomIndex2 = a2.index;
    }
  }

  private void center() {
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

  private void fixInvalidAtoms() {
    for (int i = asc.ac; --i >= 0;) {
      CDAtom a = (CDAtom) asc.atoms[i];
      a.atomSerial = Integer.MIN_VALUE;
      if (a.isFragment || a.isNickname || a.isConnectionPt
          || !a.isValid && !a.isConnected) {
        System.out.println("removing atom " + a.id + " " + a.nodeType);
        asc.bsAtoms.clear(a.index);
      }
    }
  }
  
}
