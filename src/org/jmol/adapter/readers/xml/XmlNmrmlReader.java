/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-16 09:53:18 -0500 (Sat, 16 Sep 2006) $
 * $Revision: 5561 $
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

import java.io.BufferedReader;
import java.io.IOException;

/**
 * A reader for the structure part of an nmrML file
 * 
 *  
 */

public class XmlNmrmlReader extends XmlCmlReader {

  public XmlNmrmlReader() {
  }

  /**
   * fixing unterminated cv element and unclosed chemicalShiftStandard start
   * element
   * 
   * not fixing trailing '"' after bond elements
   * 
   * Just two times through this before we are done.
   * 
   * First read will be only 64 bytes long and ignored; second will be around
   * 9000 bytes and will be handled.
   * 
   * 
   * @param reader
   * @return repaired reader
   * @throws IOException
   */
  @Override
  protected BufferedReader previewXML(BufferedReader reader) throws IOException {
    return new BufferedReader(reader) {
      
      boolean checked = false;      
      @Override
      public int read(char cbuf[], int off, int len) throws IOException {
        int n = super.read(cbuf, off, len);
        if (!checked && len > 1000) {
          int i = 200;
          while (++i < 1000) {
            // fixing <cv .......> missing "/" before ">"
            if (cbuf[i] == '<' && cbuf[i + 1] == 'c' && cbuf[i + 2] == 'v') {
              while (++i < 500) {
                if (cbuf[i] == '>' && cbuf[i - 1] == '"') {
                  cbuf[i - 1] = '/';
                  cbuf[i - 2] = '"';
                  break;
                }
              }
              break;
            }
          }
          if (i >= 500)
            i = 200;
          while (++i < 1000) {
            // fixing <chemicalShiftStandard  name="tms"  <chemicalShiftStandard>
            if (cbuf[i] == '<' && cbuf[i + 1] == 'c' && cbuf[i + 2] == 'h') {
              while (++i < 1000) {
                if (cbuf[i] == '>')
                  break;
                if (cbuf[i] == '<') {
                  cbuf[i - 1] = '>';
                  break;
                }
              }
              break;
            }
          }
          // not fixing trailing '"' in 
          //    <bond atomRefs="1 2" order="2"/>"
          checked = true;
        }
        return n;
      }

    };
  }


  @Override
  protected void processStart2(String name) {
    name = toCML(name);
    name = name.toLowerCase();
    switch (name) {
    case "atom":
      atts.put("x3", atts.remove("x"));
      atts.put("y3", atts.remove("y"));
      atts.put("z3", atts.remove("z"));
      break;
    case "bond":
      atts.put("atomrefs2", atts.remove("atomrefs"));
      break;
    }
    super.processStart2(name);
  }

  @Override
  public void processEnd2(String name) {
    super.processEnd2(toCML(name));
  }
  private static String toCML(String name) {
    name = name.toLowerCase();
    switch (name) {
    case "structure":
      name = "molecule";
      break;
    case "atomlist":
      name = "atomarray";
      break;
    case "bondlist":
      name = "bondarray";
      break;
    }
    return name;
  }

}
