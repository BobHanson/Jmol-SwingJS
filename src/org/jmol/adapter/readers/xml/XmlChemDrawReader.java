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

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.Bond;
import org.jmol.api.JmolAdapter;
import org.jmol.util.Logger;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.PT;

/**
 * An chem3d c3xml reader
 */

public class XmlChemDrawReader extends XmlReader {

  boolean optimize2D;
  private double minX = Double.MAX_VALUE;
  private double minY = Double.MAX_VALUE;
  private double minZ = Double.MAX_VALUE;
  private double maxZ = -Double.MAX_VALUE;
  private double maxY = -Double.MAX_VALUE;
  private double maxX = -Double.MAX_VALUE;
  private boolean is3D;
  
  private Lst<Object[]> bonds = new Lst<Object[]>();
  private Atom warningAtom;
  
  public XmlChemDrawReader() {
  }

  @Override
  protected void processXml(XmlReader parent, Object saxReader)
      throws Exception {
    optimize2D = checkFilterKey("2D");
    processXml2(parent, saxReader);
    this.filter = parent.filter;
  }

  @Override
  public void processStartElement(String localName, String nodeName) {
    if ("fragment".equals(localName)) {
      //nah      asc.newAtomSet();
      return;
    }

    if ("n".equals(localName)) {
      if (asc.bsAtoms == null)
        asc.bsAtoms = new BS();
      
      String nodeType = atts.get("nodetype");

      if ("Fragment".equals(nodeType))
        return;
      
      boolean isNickname = "Nickname".equals(nodeType);
      boolean isConnectionPt = "ExternalConnectionPoint".equals(nodeType);
      
      String warning = atts.get("warning");

      atom = new Atom();
      atom.atomName = atts.get("id");
      String element = atts.get("element");
      atom.elementNumber = (short) (warning != null ? 0 : element == null ? 6
          : Integer.parseInt(element));
      element = JmolAdapter.getElementSymbol(atom.elementNumber);
      String isotope = atts.get("isotope");
      if (isotope != null)
        element = isotope + element;
      setElementAndIsotope(atom, element);
      String s = atts.get("charge");
      if (s != null) {
        atom.formalCharge = Integer.parseInt(s);
      }
      if (atts.containsKey("xyz")) {
        is3D = true;
        setAtom("xyz");
      } else if (atts.containsKey("p")) {
        setAtom("p");
      }

      asc.addAtomWithMappedName(atom);
      
      if (warning != null) {
        atom.atomName = PT.rep(warning, "&apos;", "'");
        warningAtom = atom;
      } else {
        warningAtom = null;
      }

      //  ......................O --- Nickname
      //  ExternalConnectionPoint --- TBS
      if (!isConnectionPt && !isNickname) {
        asc.bsAtoms.set(atom.index);
      }
      return;
    }
    
    if ("s".equals(localName)) {
      if (warningAtom != null) {
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
      bonds.addLast(new Object[] {(invertEnds ? atom2 : atom1),
          (invertEnds ? atom1 : atom2), Integer.valueOf(order) });
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
      if (warningAtom != null) {
        String group = chars.toString();
        warningAtom.atomName += ": " + group;
        parent.appendLoadNote("Warning: " + warningAtom.atomName);
        warningAtom = null;
      }
    }
    
    setKeepChars(false);
  }

  @Override
  protected void finalizeSubclassReader() throws Exception {
    fixConnections();
    center();
    System.out.println("bsAtoms = " + asc.bsAtoms);
    asc.setInfo("minimize3D", Boolean.valueOf(is3D));
    set2D();
    asc.setInfo("is2D", Boolean.valueOf(!is3D));
    if (!is3D)
      asc.setModelInfoForSet("dimension", "2D", asc.iSet);
    parent.appendLoadNote("ChemDraw CDXML: " + (is3D ? "3D" : "2D"));
  }

  private void fixConnections() {
    for (int i = 0, n = bonds.size(); i < n; i++) {
      Object[] o = bonds.get(i);
      Bond b = asc.addNewBondFromNames((String) o[0],(String) o[1], ((Integer) o[2]).intValue());
      if (b == null)
        continue; // bond to nickname
      
      Atom a1 = asc.atoms[b.atomIndex1];
      Atom a2 = asc.atoms[b.atomIndex2];
      Atom pt = (!asc.bsAtoms.get(b.atomIndex1) ? a1 : !asc.bsAtoms.get(b.atomIndex2) ? a2 : null);
      if (pt == null)
        continue;
      for (int j = asc.bsAtoms.nextSetBit(0); j >= 0; j = asc.bsAtoms.nextSetBit(j + 1)) {
        Atom a = asc.atoms[j];
        if (Math.abs(a.x - pt.x) < 0.1d && Math.abs(a.y - pt.y) < 0.1d) {
          if (pt == a1) {
            b.atomIndex1 = (a1 = a).index;
          } else {
            b.atomIndex2 = (a2 = a).index;
          }
          break;
        }
      }
      b.distance = a1.distance(a2);
    }
  }

  private void center() {
    if (minX > maxX)
      return;
    double sum = 0;
    int n = 0;
    if (is3D) {
      for (int i = asc.bondCount; --i >= 0;) {
        if (asc.atoms[asc.bonds[i].atomIndex1].elementNumber > 1
            && asc.atoms[asc.bonds[i].atomIndex2].elementNumber > 1) {
          sum += asc.bonds[i].distance;
          n++;
        }
      }
    }
    double f = 1;
    if (sum > 0) {
      f = 1.45d * n / sum;
    }

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
}
