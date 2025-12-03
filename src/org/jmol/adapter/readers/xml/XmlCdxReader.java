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

import java.util.Map;

import org.jmol.adapter.readers.xml.CDXMLParser.CDBond;
import org.jmol.adapter.readers.xml.CDXMLParser.CDNode;
import org.jmol.adapter.readers.xml.CDXMLParser.CDXReaderI;
import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.Bond;
import org.jmol.api.JmolAdapter;
import org.jmol.util.Edge;

import javajs.util.BS;

/**
 * A reader for CambridgeSoft CDXML files.
 * 
 * 
 * revvity site:
 * 
 * 
 * https://support.revvitysignals.com/hc/en-us/articles/4408233129748-Where-is-the-ChemDraw-SDK-located
 * 
 * Their link:
 * 
 * https://web.archive.org/web/20221209095323/https://www.cambridgesoft.com/services/documentation/sdk/chemdraw/cdx/
 * 
 * WayBack machine Overview:
 * 
 * https://web.archive.org/web/20240000000000*
 * /https://www.cambridgesoft.com/services/documentation/sdk/chemdraw/cdx
 * 
 * Partial archives:
 * 
 * https://web.archive.org/web/20160911235313/http://www.cambridgesoft.com/services/documentation/sdk/chemdraw/cdx/index.htm
 * 
 * https://web.archive.org/web/20160310081515/http://www.cambridgesoft.com/services/documentation/sdk/chemdraw/cdx/
 * 
 * https://web.archive.org/web/20100503174209/http://www.cambridgesoft.com/services/documentation/sdk/chemdraw/cdx/
 * 
 * Unfortunately, there appears to be no single archive that has all the images,
 * and so some of those are missing.
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
 * @author hansonr@stolaf.edu
 * 
 */

public class XmlCdxReader extends XmlReader implements CDXReaderI {

  /**
   * setting filter "no3D" ensures the raw 2D structure is returned even though
   * there may be 3D coordinates, since a common option for the xyz attribute is
   * simply the same as 2D with a crude z offset.
   */
  private boolean no3D;
  
  private CDXMLParser parser;

  public boolean isCDX;

    public XmlCdxReader() {      
      parser = new CDXMLParser(this);
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

  @Override
  public void processStartElement(String localName, String nodeName) {
    parser.processStartElement(localName, atts);
  }
  
  @Override
  void processEndElement(String localName) {
    parser.processEndElement(localName, chars.toString());
  }

  @Override
  public void setKeepChars(boolean TF) {
    super.setKeepChars(TF);
  }
  /**
   * Fix connections to Fragments and Nicknames, adjust stereochemistry for wavy
   * displays, flag invalid atoms, and adjust the scale to something more
   * molecular. Finalize the 2D/3D business.
   * 
   */
  @Override
  protected void finalizeSubclassReader() throws Exception {
    parser.finalizeParsing();
    createMolecule();
  }


  
  // implementation-specific
  @Override
  public int getBondOrder(String key) {
    switch (key) {
    case "1":
    case "single":
      return JmolAdapter.ORDER_COVALENT_SINGLE;
    case "2":
    case "double":
      return JmolAdapter.ORDER_COVALENT_DOUBLE;
    case "3":
    case "triple":
      return JmolAdapter.ORDER_COVALENT_TRIPLE;
    case "up":
      return JmolAdapter.ORDER_STEREO_NEAR;
    case "down":
      return JmolAdapter.ORDER_STEREO_FAR;
    case "either":
      return JmolAdapter.ORDER_STEREO_EITHER;
    case "null":
      return Edge.BOND_ORDER_NULL;
    case "delocalized":
      return JmolAdapter.ORDER_AROMATIC;
    default:
    case "partial":
      return Edge.getBondOrderFromString(key);
    }
  }

  @Override
  public void handleCoordinates(Map<String, String> atts) {
    boolean hasXYZ = (atts.containsKey("xyz"));
    boolean hasXY = (atts.containsKey("p"));
    if (hasXYZ && (!no3D || !hasXY)) {
      // probably hasXY must be true; hedging here
      is2D = false;
      parser.setAtom("xyz", atts);
    } else if (atts.containsKey("p")) {
      parser.setAtom("p", atts);
    }
  }
  
  public void createMolecule() {
    BS bs = parser.bsAtoms;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      CDNode atom = parser.getAtom(i);
      Atom a = new Atom();
      a.set(atom.x, atom.y, atom.z);
      a.atomSerial = atom.intID;
      a.elementNumber = (short) atom.elementNumber;
      a.formalCharge = atom.formalCharge;
      String element = getElementSymbol(atom.elementNumber);
      if (atom.isotope != null)
        element = atom.isotope + element; //13C
      setElementAndIsotope(a, element);
      asc.addAtom(a);
    }

    bs = parser.bsBonds;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      CDBond bond = parser.getBond(i);
      Bond b = new Bond(bond.atomIndex1, bond.atomIndex2, bond.order);
      asc.addBondNoCheck(b);
    }
    parent.appendLoadNote((isCDX ? "CDX: " : "CDXML: ") + (is2D ? "2D" : "3D"));
    asc.setInfo("minimize3D", Boolean.valueOf(!is2D && !noHydrogens));
    asc.setInfo("is2D", Boolean.valueOf(is2D));
    if (is2D) {
      optimize2D = !noHydrogens && !noMinimize;
      asc.setModelInfoForSet("dimension", "2D", asc.iSet);
      set2D();
    }
  }

  @Override
  public void warn(String warning) {
    parent.appendLoadNote(warning);
  }
}
