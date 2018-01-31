/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-22 14:12:46 -0500 (Sun, 22 Oct 2006) $
 * $Revision: 5999 $
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

import org.jmol.util.Logger;

/**
 * 
 * QuantumEspresso XML reader
 * 
 * @author hansonr
 * 
 */

public class XmlQEReader extends XmlReader {
  
  public XmlQEReader() {
  }
  
  private float a;
  private float b;
  private float c;
  //private float alpha;
  //private float beta;
  //private float gamma;
  
//  private String[] myAttributes = { "SPECIES", "TAU" };
//  
//  @Override
//  protected String[] getDOMAttributes() {
//    return myAttributes;
//  }

  @Override
  protected void processXml(XmlReader parent,
                            Object saxReader) throws Exception {
    parent.doProcessLines = true;
    processXml2(parent, saxReader);
  }

  @Override
  public void processStartElement(String localName, String nodeName) {
    if (debugging)
      Logger.debug("xmlqe: start " + localName);

    if (!parent.continuing)
      return;

    if ("number_of_atoms".equals(localName)
        || "cell_dimensions".equals(localName)
        || "at".equals(localName)) {
      setKeepChars(true);
      return;
    }

    if (localName.startsWith("atom.")) {
      parent.setAtomCoordScaled(null, PT.getTokens(atts.get("tau")), 0,
          ANGSTROMS_PER_BOHR).elementSymbol = atts.get("species").trim();
    }
    if ("structure".equals(localName)) {
      if (!parent.doGetModel(++parent.modelNumber, null)) {
        parent.checkLastModel();
        return;
      }
      parent.setFractionalCoordinates(true);
      asc.doFixPeriodic = true;
      asc.newAtomSet();
      return;
    }
    if (!parent.doProcessLines)
      return;

  }

  @Override
  void processEndElement(String localName) {

    if (debugging)
      Logger.debug("xmlqe: end " + localName);

    while (true) {

      if (!parent.doProcessLines)
        break;

//      if ("NUMBER_OF_ATOMS".equals(localName)) {
//        ac = parseIntStr(chars.toString());
//        break;
//      }

      if ("cell_dimensions".equals(localName)) {
        parent.setFractionalCoordinates(true);
        float[] data = getTokensFloat(chars.toString(), null, 6);
        a = data[0];
        b = (data[1] == 0 ? a : data[1]);
        c = (data[2] == 0 ? a : data[2]);
        //alpha = (data[3] == 0 ? 90 : data[3]);
        //beta = (data[4] == 0 ? 90 : data[4]);
        //gamma = (data[5] == 0 ? 90 : data[5]);
        break;
      }

      if ("at".equals(localName)) {
        // probably wrong -- only cubic
        float[] m = getTokensFloat(chars.toString(), null, 9);
        for (int i = 0; i < 9; i += 3) {
          m[i] *= a;
          m[i + 1] *= b;
          m[i + 2] *= c;
        }
        parent.addExplicitLatticeVector(0, m, 0);
        parent.addExplicitLatticeVector(1, m, 3);
        parent.addExplicitLatticeVector(2, m, 6);
        break;
      }

      if ("geometry_info".equals(localName)) {
        try {
          parent.applySymmetryAndSetTrajectory();
        } catch (Exception e) {
          // TODO
        }
        break;
      }

      return;
    }
    setKeepChars(false);
  }

}
