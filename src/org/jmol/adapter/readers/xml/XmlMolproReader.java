/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-12 00:46:22 -0500 (Tue, 12 Sep 2006) $
 * $Revision: 5501 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 * Copyright (C) 2005  Peter Knowles
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

/**
 * A Molpro 2005 reader
 */
public class XmlMolproReader extends XmlMOReader {

//  String[] myAttributes = { "id", "length", "type", //general
//      "x3", "y3", "z3", "elementType", //atoms
//      "name", //variable
//      "groups", "cartesianLength", "primitives", // basisSet and
//      "minL", "maxL", "angular", "contractions", //   basisGroup
//      "wavenumber", "units", // normalCoordinate
//      "minl", "maxl", "angular", // basisSet.basisGroup
//      "href", // association.bases
//      "occupation", "energy", "symmetryID", // orbital 
//  };

  public XmlMolproReader() {  
    // These lists are for the nonstandard paramter order for Molpro. 
    // We used them to create a coefficient map rather than actually
    // moving any coefficients. In this case, we end up with:
    
    // D-spherical: [0, 1, 2, 0, -3]
    // D-cartesian: [0, 0, 0, 0, 0, 0]
    // F-spherical: [2, -1, -1, 3, 0, -2, -1]
    // F-cartesian: [0, 0, 0, 2, -1, -1, 1, 1, -2, 0]
      
    dslist = "d0 d2- d1+ d2+ d1-";
    fclist = "XXX YYY ZZZ XXY XXZ XYY YYZ XZZ YZZ XYZ";
    fslist = "f1+ f1- f0 f3+ f2- f3- f2+";
    
    // For example, Jmol expects:  "d0 d1+ d1- d2+ d2-"
    // but we have here:           "d0 d2- d1+ d2+ d1-"
    // so the relative shifts are: [0, 1,  2,  0,  -3]

    iHaveCoefMaps = true;
  }
  
  //  @Override
  //  protected String[] getDOMAttributes() {
  //    return myAttributes;
  //  }

  @Override
  public void processStartElement(String localName, String nodeName) {
    if (!processing)
      return;
    processStart2(localName);
    if (!processStartMO(localName)) {
      if (localName.equals("normalcoordinate")) {
        setKeepChars(false);
        if (!parent.doGetVibration(++vibrationNumber))
          return;
        try {
          asc.cloneLastAtomSet();
        } catch (Exception e) {
          System.out.println("" + e);
          asc.errorMessage = "Error processing normalCoordinate: "
              + e.getMessage();
          vibrationNumber = 0;
          return;
        }
        if (atts.containsKey("wavenumber")) {
          String wavenumber = atts.get("wavenumber");
          String units = "cm^-1";
          if (atts.containsKey("units")) {
            units = atts.get("units");
            if (units.startsWith("inverseCent"))
              units = "cm^-1";
          }
          asc.setAtomSetFrequency(vibrationNumber, null, null, wavenumber, units);
          setKeepChars(true);
        }
        return;
      }
      if (localName.equals("vibrations")) {
        vibrationNumber = 0;
        return;
      }
    }
  }

  @Override
  void processEndElement(String localName) {
    if (!processEndMO(localName)) {
      if (localName.equals("normalcoordinate")) {
        if (!keepChars)
          return;
        int ac = asc.getLastAtomSetAtomCount();
        int baseAtomIndex = asc.getLastAtomSetAtomIndex();
        tokens = PT.getTokens(chars.toString());
        for (int offset = tokens.length - ac * 3, i = 0; i < ac; i++) {
          asc.addVibrationVector(i + baseAtomIndex,
              parseFloatStr(tokens[offset++]), parseFloatStr(tokens[offset++]),
              parseFloatStr(tokens[offset++]));
        }
        setKeepChars(false);
      }
    }
    processEnd2(localName);
  }

}
