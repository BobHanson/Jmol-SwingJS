/*  
 *  The Janocchio program is (C) 2007 Eli Lilly and Co.
 *  Authors: David Evans and Gary Sharman
 *  Contact : janocchio-users@lists.sourceforge.net.
 * 
 *  It is derived in part from Jmol 
 *  (C) 2002-2006 The Jmol Development Team
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.
 *  All we ask is that proper credit is given for our work, which includes
 *  - but is not limited to - adding the above copyright notice to the beginning
 *  of your source code files, and to any copyright notice that you may distribute
 *  with programs based on this work.
 *
 *  This program is distributed in the hope that it will be useful, on an 'as is' basis,
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.openscience.jmol.app.janocchio;

import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import javax.swing.ImageIcon;

/**
 * Provides access to resources (for example, strings and images). This class is
 * a singleton which is retrieved by the getInstance method.
 * 
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
public class NmrResourceHandler {

  private static NmrResourceHandler instance;
  private ResourceBundle stringsResourceBundle;

  //  private ResourceBundle generalResourceBundle;

  private NmrResourceHandler() {
    String language = "en";
    String country = "";
    String localeString = System.getProperty("user.language");
    if (localeString != null) {
      StringTokenizer st = new StringTokenizer(localeString, "_");
      if (st.hasMoreTokens()) {
        language = st.nextToken();
      }
      if (st.hasMoreTokens()) {
        country = st.nextToken();
      }
    }
    stringsResourceBundle = ResourceBundle.getBundle(Nmr.path
        + "Properties.Nmr", new Locale(language, country));
  }

  public static NmrResourceHandler getInstance() {
    return (instance == null ? (instance = new NmrResourceHandler()) : instance);
  }

  private synchronized ImageIcon getIcon(String key) {

    String resourceName = null;
    try {
      resourceName = getString(key);
    } catch (MissingResourceException e) {
    }

    if (resourceName != null) {
      String imageName = Nmr.path.replace('.', '/') + "images/" + resourceName;
      URL imageUrl = Nmr.class.getClassLoader().getResource(imageName);
      if (imageUrl != null) {
        return new ImageIcon(imageUrl);
      }
    }
    return null;
  }

  public static String getStringX(String key) {
    return getInstance().getString(key);
  }

  public static ImageIcon getIconX(String key) {
    return getInstance().getIcon(key);
  }

  private synchronized String getString(String key) {
    String result = null;
    try {
      result = stringsResourceBundle.getString(key);
    } catch (MissingResourceException e) {
    }
    return result == null ? key : result;
  }

  /**
   * A wrapper for easy detection which strings in the source code are
   * localized.
   * 
   * @param text
   *        Text to translate
   * @return Translated text
   */
  /*private synchronized String translate(String text) {
    StringTokenizer st = new StringTokenizer(text, " ");
    StringBuffer key = new StringBuffer();
    while (st.hasMoreTokens()) {
      key.append(st.nextToken());
      if (st.hasMoreTokens()) {
        key.append("_");
      }
    }
    String translatedText = getString(key.toString());
    return (translatedText != null) ? translatedText : text;
  }*/

}
