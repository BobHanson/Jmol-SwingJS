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
package org.jmol.popup;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Properties;

import javajs.util.SB;

import org.jmol.api.Translator;

public abstract class PopupResource {

  // Properties to store menu structure and contents
  protected Properties structure, words;

  abstract public String getMenuName();

  protected PopupResource(String menuStructure, Properties menuText) {
    // when these were defined above, then they were overwritten by
    // setFields operating a second time, probably because MainPopupResourceBundle does not
    // have any fields of its own.
    structure = new Properties();
    words = new Properties();
    buildStructure(menuStructure);
    localize(menuStructure != null, menuText);
  }
     
  abstract protected String[] getWordContents();
  
  abstract protected void buildStructure(String menuStructure);
  
  String getStructure(String key) {
    return structure.getProperty(key);
  }

  String getWord(String key) {
    String str = words.getProperty(key);
    return (str == null ? key : str);
  }

  protected void setStructure(String slist, Translator gt) {
    BufferedReader br = new BufferedReader(new StringReader(slist));
    String line;
    int pt;
    try {
      while ((line = br.readLine()) != null) {
        if (line.length() == 0 || line.charAt(0) == '#')
          continue;
        pt = line.indexOf("=");
        if (pt < 0) {
          pt = line.length();
          line += "=";
        }
        String name = line.substring(0, pt).trim();
        String value = line.substring(pt + 1).trim();
        String label = null;
        if ((pt = name.indexOf("|")) >= 0) {
          label = name.substring(pt + 1).trim();
          name = name.substring(0, pt).trim();
        }
        if (name.length() == 0)
          continue;
        if (value.length() > 0)
          structure.setProperty(name, value);
        if (label != null && label.length() > 0)
          words.setProperty(name, (gt == null ? label : gt.translate(label)));
        /* note that in this case we are using a variable in 
         * the GT.$() method. That's because all standard labels
         * have been preprocessed already, so any standard label
         * will be translated. Any other label MIGHT be translated
         * if by chance that word or phrase appears in some other
         * GT.$() call somewhere else in Jmol. Otherwise it will not
         * be translated by this call, because it hasn't been 
         * internationalized. 
         */
      }
    } catch (Exception e) {
      //
    }
    try {
      br.close();
    } catch (Exception e) {
    }
  }
  
  protected void addItems(String[][] itemPairs) {   
    String previous = "";
    for (int i = 0; i < itemPairs.length; i++) {
      String[] pair = itemPairs[i];
      String str = pair[1];
      if (str == null)
        str = previous;
      previous = str;
      structure.setProperty(pair[0], str);
    }
  }
  
  /**
   * 
   * @param haveUserMenu NOT USED
   * @param menuText
   */
  private void localize(boolean haveUserMenu, Properties menuText) {
    String[] wordContents = getWordContents();
    for (int i = 0; i < wordContents.length; i++) {
      String item = wordContents[i++];
      String word = words.getProperty(item);
      if (word == null)
        word = wordContents[i];
      words.setProperty(item, word);
      // save a few names for later
      if (menuText != null && item.indexOf("Text") >= 0)
        menuText.setProperty(item, word);
    }
  }

  abstract public String getMenuAsText(String title);

  protected String getStuctureAsText(String title, String[][] menuContents,
                                     String[][] structureContents) {
      return "# " + getMenuName() + ".mnu " + title + "\n\n" +
          "# Part I -- Menu Structure\n" +
          "# ------------------------\n\n" +
          dumpStructure(menuContents) + "\n\n" +
          "# Part II -- Key Definitions\n" +
          "# --------------------------\n\n" +
          dumpStructure(structureContents) + "\n\n" +
          "# Part III -- Word Translations\n" +
          "# -----------------------------\n\n" +
          dumpWords();
  }

  private String dumpWords() {
   String[] wordContents = getWordContents();
   SB s = new SB();
   for (int i = 0; i < wordContents.length; i++) {
     String key = wordContents[i++];
     if (structure.getProperty(key) == null)
       s.append(key).append(" | ").append(wordContents[i]).appendC('\n');
   }
   return s.toString();
  }

  private String dumpStructure(String[][] items) {
   String previous = "";
   SB s = new SB();
   for (int i = 0; i < items.length; i++) {
     String key = items[i][0];
     String label = words.getProperty(key);
     if (label != null)
       key += " | " + label;
     s.append(key).append(" = ")
      .append(items[i][1] == null ? previous : (previous = items[i][1]))
      .appendC('\n');
   }
   return s.toString();
  }
}
