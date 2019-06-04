/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-04-29 07:22:46 -0500 (Thu, 29 Apr 2010) $
 * $Revision: 12980 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
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
package jspecview.unused;

import java.util.Properties;

public abstract class PopupResource {

  // Properties to store menu structure and contents
  protected Properties structure, words;

  abstract public String getMenuName();

  protected PopupResource() {
    // when these were defined above, then they were overwritten by
    // setFields operating a second time, probably because MainPopupResourceBundle does not
    // have any fields of its own.
    structure = new Properties();
    words = new Properties();
    buildStructure();
  }
  
  /**
   * 
   * @param title
   * @return menu string -- see MainPopupResourceBundle 
   */
  String getMenuAsText(String title) {
    return null;
  }
    
  abstract protected String[] getWordContents();
  
  abstract protected void buildStructure();
  
  String getStructure(String key) {
    return structure.getProperty(key);
  }

  String getWord(String key) {
    String str = words.getProperty(key);
    return (str == null ? key : str);
  }

  protected void addItems(String[][] itemPairs) {   
    String previous = "";
    for (int i = 0; i < itemPairs.length; i++) {
      String str = itemPairs[i][1];
      if (str == null)
        str = previous;
      previous = str;
      structure.setProperty(itemPairs[i][0], str);
    }
  }
  
}
