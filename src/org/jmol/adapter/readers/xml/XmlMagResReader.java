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



import org.jmol.util.Logger;

/**
 * 
 * 
 */

public class XmlMagResReader extends XmlReader {

//  private String[] myAttributes = new String[]{ /* XML tag attributes of interest here */};
  
  public XmlMagResReader() {
  }
  
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
      Logger.debug("xmlmagres: start " + localName);

    if (!parent.continuing)
      return;

    if ("calculation".equals(localName)) {
      setKeepChars(true);
      return;
    }        
    if ("atoms".equals(localName)) {
      setKeepChars(true);
      return;
    }

  }

  @Override
  void processEndElement(String localName) {

    if (debugging) 
      Logger.debug("xmlmagres: end " + localName);

    while (true) {

      if ("calculation".equals(localName)) {
        // process calculation data here
        break;
      }
      
      if (!parent.doProcessLines)
        break;

      if ("atoms".equals(localName)) {
        // process atom data here
        break;
      }
      
      return;
    }
    setKeepChars(false);
  }

}
