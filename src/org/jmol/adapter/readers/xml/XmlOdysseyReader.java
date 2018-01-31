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
import org.jmol.api.JmolAdapter;
import javajs.util.P3;
import javajs.util.PT;

/**
 * An Odyssey xodydata reader
 */

public class XmlOdysseyReader extends XmlReader {

  private String modelName = null;
  private String formula = null;
  private String phase = null;
  
//  private String[] myAttributes = { "id", "label", //general 
//      "xyz", "element", "hybrid", //atoms
//      "a", "b", "order", //bond
//      "charge", // group 
//      "entity", // member
//      "box" // boundary
//  };
  private int formalCharge = Integer.MIN_VALUE;

  public XmlOdysseyReader() {
  }

//  @Override
//  protected String[] getDOMAttributes() {
//    return myAttributes;
//  }

  @Override
  protected void processStartElement(String localName, String nodeName) {

    if ("structure".equals(localName)) {
      asc.newAtomSet();
      return;
    }

    if ("atom".equals(localName)) {
      String id = atts.get("id");
      (atom = new Atom()).atomName = atts
          .get(atts.containsKey("label") ? "label" : "id");
      if (id != null && stateScriptVersionInt >= 140400)
        asc.atomSymbolicMap.put(id, atom);
      if (atts.containsKey("xyz")) {
        String xyz = atts.get("xyz");
        String[] tokens = PT.getTokens(xyz);
        atom.set(parseFloatStr(tokens[0]), parseFloatStr(tokens[1]),
            parseFloatStr(tokens[2]));
      }
      if (atts.containsKey("element")) {
        atom.elementSymbol = atts.get("element");
      }
      return;
    }
    if ("bond".equals(localName)) {
      String atom1 = atts.get("a");
      String atom2 = atts.get("b");
      int order = 1;
      if (atts.containsKey("order"))
        order = parseBondToken(atts.get("order"));
      asc.addNewBondFromNames(atom1, atom2, order);
      return;
    }
    if ("group".equals(localName)) {
      String charge = atts.get("charge");
      if (charge != null && charge.indexOf(".") < 0) {
        formalCharge = PT.parseInt(charge);
      }
      return;
    }
    if ("member".equals(localName) && formalCharge != Integer.MIN_VALUE) {
      Atom atom = asc.getAtomFromName(atts.get("entity"));
      if (atom != null)
        atom.formalCharge = formalCharge;
      return;
    }
    if ("boundary".equals(localName)) {
      String[] boxDim = PT.getTokens(atts.get("box"));
      float x = parseFloatStr(boxDim[0]);
      float y = parseFloatStr(boxDim[1]);
      float z = parseFloatStr(boxDim[2]);
      parent.setUnitCellItem(0, x);
      parent.setUnitCellItem(1, y);
      parent.setUnitCellItem(2, z);
      parent.setUnitCellItem(3, 90);
      parent.setUnitCellItem(4, 90);
      parent.setUnitCellItem(5, 90);
      P3 pt = P3.new3(-x / 2, -y / 2, -z / 2);
      //asc.setCurrentModelInfo("periodicOriginXyz", pt);
      Atom[] atoms = asc.atoms;
      for (int i = asc.ac; --i >= 0;) {
        atoms[i].sub(pt);
        parent.setAtomCoord(atoms[i]);
      }
      if (parent.latticeCells[0] == 0)
        parent.latticeCells[0] = parent.latticeCells[1] = parent.latticeCells[2] = 1;
//        parent.setSpaceGroupName("P1");
      return;
    }
    if ("odyssey_simulation".equals(localName)) {
      if (modelName != null && phase != null)
        modelName += " - " + phase;
      if (modelName != null)
        asc.setAtomSetName(modelName);
      if (formula != null)
        asc.setCurrentModelInfo("formula", formula);
    }
    if ("title".equals(localName) || "formula".equals(localName)
        || "phase".equals(localName))
      setKeepChars(true);
  }

  private int parseBondToken(String str) {
    if (str.length() >= 1) {
      switch (str.charAt(0)) {
      case 's':
        return 1;
      case 'd':
        return 2;
      case 't':
        return 3;
      case 'a':
        return JmolAdapter.ORDER_AROMATIC;
      }
      return parseIntStr(str);
    }
    return 1;
  }

  @Override
  void processEndElement(String localName) {
    if ("atom".equals(localName)) {
      if (atom.elementSymbol != null && !Float.isNaN(atom.z)) {
        asc.addAtomWithMappedName(atom);
      }
      atom = null;
      return;
    }
    if ("group".equals(localName)) {
      formalCharge = Integer.MIN_VALUE;
    } else if ("title".equals(localName)) {
      modelName = chars.toString();
    } else if ("formula".equals(localName)) {
      formula = chars.toString();
    } else if ("phase".equals(localName)) {
      phase = chars.toString();
    }
    setKeepChars(false);
  }

}
