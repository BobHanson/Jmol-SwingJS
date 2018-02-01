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


import javajs.util.PT;

import org.jmol.adapter.smarter.Atom;
import javajs.util.BS;

/**
 * An XML reader for Materials Studio .xsd files   http://accelrys.com/products/materials-studio/
 * 
 * Bob Hanson hansonr@stolaf.edu 6/3/09
 * 
 */

public class XmlXsdReader extends XmlReader {

  public XmlXsdReader() {
  }

  private BS bsBackbone = new BS();
  
  private int iChain = -1;
  private int iGroup = 0;
  private int iAtom = 0;
  
//  @Override
//  protected String[] getDOMAttributes() {
//    return new String[] { "ID", //general 
//        "XYZ", "Connections", "Components", "IsBackboneAtom",//Atom3d
//        "Connects", "Type", //Bond
//        "Name", //LinearChain
//    };
//  }

  @Override
  protected void processXml(XmlReader parent,
                            Object saxReader) throws Exception {
    parent.htParams.put("backboneAtoms", bsBackbone);
    processXml2(parent, saxReader);
    asc.atomSymbolicMap.clear(); 
  }

  @Override
  public void processStartElement(String localName, String nodeName) {
    String[] tokens;
    //System.out.println( " " + localName + " " + atts);
    //System.out.println("xmlchem3d: start " + localName);
    if ("molecule".equals(localName)) {
      asc.newAtomSet();
      asc.setAtomSetName(atts.get("name"));      
      return;
    }
    
    if ("linearchain".equals(localName)) {
      iGroup = 0;
      iChain++;
    }

    if ("repeatunit".equals(localName)) {
      iGroup++;
    }

    if ("atom3d".equals(localName)) {
      atom = new Atom();
      atom.elementSymbol = atts.get("components");
      atom.atomName = atts.get("id");
      atom.atomSerial = ++iAtom;
      if (iChain >= 0)
        parent.setChainID(atom, "" + (char) ((iChain - 1)%26 + 'A'));
      atom.group3 = "UNK";
      if (iGroup == 0)
        iGroup = 1;
      atom.sequenceNumber = iGroup;
      String xyz = atts.get("xyz");
      if (xyz != null) {
        tokens = PT.getTokens(xyz.replace(',',' '));
        atom.set(parseFloatStr(tokens[0]), parseFloatStr(tokens[1]), parseFloatStr(tokens[2]));
      }
      boolean isBackbone = "1".equals(atts.get("isbackboneatom"));
      if (isBackbone)
        bsBackbone.set(iAtom);
      return;
    }
    if ("bond".equals(localName)) {
      String[] atoms = PT.split(atts.get("connects"), ",");
      int order = 1;
      if (atts.containsKey("type")) {
        String type = atts.get("type");
        if (type.equals("Double"))
          order = 2;
        else if (type.equals("Triple"))
          order = 3;
      }
      asc.addNewBondFromNames(atoms[0], atoms[1], order);
      return;
    }
  }

  @Override
  void processEndElement(String localName) {
    if ("atom3d".equalsIgnoreCase(localName)) {
      if (atom.elementSymbol != null && !Float.isNaN(atom.z)) {
        parent.setAtomCoord(atom);
        asc.addAtomWithMappedName(atom);
      }
      atom = null;
      return;
    }
    setKeepChars(false);
  }

}
