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

import javajs.util.PT;

/**
 * An chem3d c3xml reader
 */

public class XmlChemDrawReader extends XmlReader {

  boolean optimize2D;
  private float minX = Float.MAX_VALUE;
  private float minY = Float.MAX_VALUE;
  private float maxY = -Float.MAX_VALUE;
  private float maxX = -Float.MAX_VALUE;

  public XmlChemDrawReader() {
  }

  
  @Override
  protected void processXml(XmlReader parent,
                            Object saxReader) throws Exception {
    optimize2D = checkFilterKey("2D");
    processXml2(parent, saxReader);
    this.filter = parent.filter;
    set2D();
  }

  @Override
  public void processStartElement(String localName, String nodeName) {
    String[] tokens;
    if ("fragment".equals(localName)) {
//nah      asc.newAtomSet();
      return;
    }

    if ("n".equals(localName)) {
      String type = atts.get("type");
      if (type != null && !type.equals("unspecified")
          && !type.equals("eLement")) {
        System.err.println("XmlChemDrawReader Unsupported type: " + type);
        return;
      }
      atom = new Atom();
      atom.atomName = atts.get("id");
      String element = atts.get("element");
      short elNo = (short) (element == null ? 6 : Integer.parseInt(element));
      element = JmolAdapter.getElementSymbol(elNo);
      String isotope = atts.get("isotope");
      if (isotope != null)
        element = isotope + element;
      setElementAndIsotope(atom, element);
      String s = atts.get("charge");
      if (s != null) {
        atom.formalCharge = Integer.parseInt(s);
      }
      float x = 0, y = 0;
      if (atts.containsKey("p")) {
        String xy = atts.get("p");
        tokens = PT.getTokens(xy);
        x = parseFloatStr(tokens[0]);
        y = -parseFloatStr(tokens[1]);
        if (x < minX)
          minX = x;
        if (x > maxX)
          maxX = x;
        if (y < minY)
          minY = y;
        if (y > maxY)
          maxY = y;
      }
      
      atom.set(x, y, 0);
      asc.addAtomWithMappedName(atom);
      return;
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
      if (invertEnds) {
        asc.addNewBondFromNames(atom2, atom1, order);
      } else {
        asc.addNewBondFromNames(atom1, atom2, order);
      }
      return;
    }

  }

  @Override
  void processEndElement(String localName) {
    setKeepChars(false);
  }

  @Override
  protected void finalizeSubclassReader() throws Exception {
    center();
    asc.setModelInfoForSet("dimension", "2D", asc.iSet);
  }


  private void center() {
    if (minX > maxX)
      return;
    float cx = (maxX + minX)/2;
    float cy = (maxY + minY)/2;
    for (int i = asc.ac; --i >= 0;) {
      Atom a = asc.atoms[i];
      a.x -= cx;
      a.y -= cy;
    }
      
    
  }
}
