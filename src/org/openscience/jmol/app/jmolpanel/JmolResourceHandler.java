/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-02-02 19:52:43 -0600 (Sun, 02 Feb 2014) $
 * $Revision: 19249 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
package org.openscience.jmol.app.jmolpanel;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import javax.swing.ImageIcon;

import org.jmol.i18n.GT;

/**
 * Provides access to resources (for example, strings and images). This class is
 * a singleton which is retrieved by the getInstance method.
 *
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
class JmolResourceHandler {

  private static JmolResourceHandler instance;
  private ResourceBundle stringsResourceBundle;
  private ResourceBundle generalResourceBundle;
  
  public static Object codePath;

  private JmolResourceHandler() {
    String language = "en";
    String country = "";
    String localeString = GT.getLanguage();
//    String localeString = System.getProperty("user.language");
    if (localeString != null) {
      StringTokenizer st = new StringTokenizer(localeString, "_");
      if (st.hasMoreTokens()) {
        language = st.nextToken();
      }
      if (st.hasMoreTokens()) {
        country = st.nextToken();
      }
    }
    Locale locale = new Locale(language, country);
    stringsResourceBundle =
      ResourceBundle.getBundle("org.openscience.jmol.app.jmolpanel.Properties.Jmol", locale);

    try {
      String t = "/org/openscience/jmol/app/jmolpanel/Properties/Jmol-resources.properties";
      generalResourceBundle =
        new PropertyResourceBundle(getClass().getResourceAsStream(t));
    } catch (IOException ex) {
      throw new RuntimeException(ex.toString());
    }
  }

  static void clear() {
    instance = null;  
  }
  
  static JmolResourceHandler getInstance() {
    if (instance == null) {
      instance = new JmolResourceHandler();
    }
    return instance;
  }

  static String getStringX(String key) {
    return getInstance().getString(key);
  }

  static ImageIcon getIconX(String key){ 
    return getInstance().getIcon(key);
  }

  private synchronized ImageIcon getIcon(String key) {

    String resourceName = null;
    try {
      resourceName = getString(key);
    } catch (MissingResourceException e) {
    }

    if (resourceName != null) {
      String imageName = "org/openscience/jmol/app/images/" + resourceName;
      URL imageUrl = this.getClass().getClassLoader().getResource(imageName);
      if (imageUrl != null) {
        String s = imageUrl.toString();
        codePath = s.substring(0, s.indexOf(imageName));

        return new ImageIcon(imageUrl);
      }
      /*
      System.err.println("Warning: unable to load " + resourceName
          + " for icon " + key + ".");
      */
    }
    return null;
  }

  private synchronized String getString(String key) {

    String result = null;
    try {
      result = stringsResourceBundle.getString(key);
    } catch (MissingResourceException e) {
    }
    if (result == null) {
      try {
        result = generalResourceBundle.getString(key);
      } catch (MissingResourceException e) {
      }
    }
    return result != null ? result : key;
  }

  /**
   * A wrapper for easy detection which strings in the
   * source code are localized.
   * @param text Text to translate
   * @return Translated text
   */
  /*private synchronized String translate(String text) {
    StringTokenizer st = new StringTokenizer(text, " ");
    StringXBuilder key = new StringXBuilder();
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

