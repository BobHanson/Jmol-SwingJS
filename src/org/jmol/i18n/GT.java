/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2015-04-05 20:13:49 -0500 (Sun, 05 Apr 2015) $
 * $Revision: 20435 $
 *
 * Copyright (C) 2005  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.i18n;

import java.text.MessageFormat;
//import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javajs.J2SRequireImport;
import javajs.util.PT;

import org.jmol.api.Translator;
import org.jmol.util.Logger; 
import org.jmol.viewer.Viewer;

/**
 * 
 * The language list is now in org.jmol.i18n.Language -- Bob Hanson, 12/16/12
 * 
 * implementing translations in JavaScript
 */

@J2SRequireImport({java.text.MessageFormat.class, org.jmol.i18n.Resource.class, org.jmol.i18n.Language.class, javajs.util.PT.class})
public class GT implements Translator {

  private static boolean ignoreApplicationBundle = false;
  private static GT getTextWrapper;
  private static Language[] languageList;

  private Resource[] resources = null;
  private int resourceCount = 0;

  private boolean doTranslate = true;
  private String language;
  private static boolean allowDebug = false;
  static Viewer vwr;

  public GT() {
   //  
  }
  
  /** used in custom menu translation only **/
  
  @Override
  public String translate(String s) {
    return _(s);
  }
  
  public GT(Viewer vwr, String langCode) {
    /**
     * @j2sIgnore
     * 
     */
    {
      allowDebug = true;      
    }
    
    resources = null;
    resourceCount = 0;
    getTextWrapper = this;
    if (langCode != null && langCode.length() == 0)
      langCode = "none";
    if (langCode != null)
      language = langCode;
    if ("none".equals(language))
      language = null;
    if (language == null)
      language = Resource.getLanguage();
    if (language == null)
      language = "en";

    String la = language;
    String la_co = null;
    String la_co_va = null;
    int i = language.indexOf("_");
    if (i >= 0) {
      la = la.substring(0, i);
      la_co = language;
      if ((i = la_co.indexOf("_", ++i)) >= 0) {
        la_co = la_co.substring(0, i);
        la_co_va = language;
      }
    }

    /*
     * find the best match. In each case, if the match is not found
     * but a variation at the next level higher exists, pick that variation.
     * So, for example, if fr_CA does not exist, but fr_FR does, then 
     * we choose fr_FR, because that is taken as the "base" class for French.
     * 
     * Or, if the language requested is "fr", and there is no fr.po, but there
     * is an fr_FR.po, then return that. 
     * 
     * Thus, the user is informed of which country/variant is in effect,
     * if they want to know. 
     * 
     */
    if ((language = getSupported(la_co_va)) == null
        && (language = getSupported(la_co)) == null
        && (language = getSupported(la)) == null) {
      language = "en";
      System.out.println(language + " not supported -- using en");
      return;
    }
    la_co_va = null;
    la_co = null;
    switch (language.length()) {
    default:
      la_co_va = language;
      la_co = language.substring(0, 5);
      la = language.substring(0, 2);
      break;
    case 5:
      la_co = language;
      la = language.substring(0, 2);
      break;
    case 2:
      la = language;
      break;
    }

    /*
     * Time to determine exactly what .po files we actually have.
     * No need to check a file twice.
     * 
     */

    la_co = getSupported(la_co);
    la = getSupported(la);

    if (la == la_co || "en_US".equals(la))
      la = null;
    if (la_co == la_co_va)
      la_co = null;
    if ("en_US".equals(la_co))
      return;
    if (allowDebug && Logger.debugging)
      Logger.debug("Instantiating gettext wrapper for " + language
          + " using files for language:" + la + " country:" + la_co
          + " variant:" + la_co_va);
    if (!ignoreApplicationBundle)
      addBundles(vwr, "Jmol", la_co_va, null, null);
    addBundles(vwr, "JmolApplet", la_co_va, null, null);
    if (!ignoreApplicationBundle)
      addBundles(vwr, "Jmol", null, la_co, null);
    addBundles(vwr, "JmolApplet", null, la_co, null);
    if (!ignoreApplicationBundle)
      addBundles(vwr, "Jmol", null, null, la);
    addBundles(vwr, "JmolApplet", null, null, la);
  }

  public static Language[] getLanguageList(GT gt) {
    if (languageList == null) {
      if (gt == null)
        gt = getTextWrapper();
      gt.createLanguageList();
    }
    return languageList;
  }

  public static String getLanguage() {
    return getTextWrapper().language;
  }

  public static void ignoreApplicationBundle() {
    ignoreApplicationBundle = true;
  }

  /**
   * 
   * @param TF
   * @return  initial setting of GT.doTranslate
   * 
   */
  public static boolean setDoTranslate(boolean TF) {
    boolean b = getDoTranslate();
    getTextWrapper().doTranslate = TF;
    return b;
  }

  public static boolean getDoTranslate() {
    return getTextWrapper().doTranslate;
  }

  public static String _(String string) {
    return getTextWrapper().getString(string);
  }

  public static String o(String s, Object o) {
    if (o instanceof Object[]) {
      if (((Object[]) o).length != 1)
        return MessageFormat.format(s, (Object[]) o);
      o = ((Object[]) o)[0];
    }
    return PT.rep(s, "{0}", o.toString());
  }

  public static String i(String s, int n) {
    return PT.rep(s, "{0}", "" + n);
  }

  public static String escapeHTML(String msg) {
    char ch;
    for (int i = msg.length(); --i >= 0;)
      if ((ch = msg.charAt(i)) > 0x7F) {
        msg = msg.substring(0, i) + "&#" + ((int) ch) + ";"
            + msg.substring(i + 1);
      }
    return msg;
  }

  private static GT getTextWrapper() {
    return (getTextWrapper == null ? getTextWrapper = new GT(null, null)
        : getTextWrapper);
  }

  synchronized private void createLanguageList() {
    boolean wasTranslating = doTranslate;
    doTranslate = false;
    languageList = Language.getLanguageList();
    doTranslate = wasTranslating;
  }

  private static Map<String, String> htLanguages = new Hashtable<String, String>();

  private String getSupported(String code) {
    if (code == null)
      return null;
    String s = htLanguages.get(code);
    if (s != null)
      return (s.length() == 0 ? null : s);
    s = Language.getSupported(getLanguageList(this), code);
    htLanguages.put(code, (s == null ? "" : s));
    return s;
  }

  private void addBundles(Viewer vwr, String type, String la_co_va, String la_co, String la) {
    try {
      String className = "org.jmol.translation." + type + ".";
      if (la_co_va != null)
        addBundle(vwr, className, la_co_va);
      if (la_co != null)
        addBundle(vwr, className, la_co);
      if (la != null)
        addBundle(vwr, className, la);
    } catch (Exception exception) {
      if (allowDebug)
        Logger.errorEx("Some exception occurred!", exception);
      resources = null;
      resourceCount = 0;
    }
  }

  private void addBundle(Viewer vwr, String className, String name) {
    Resource resource = Resource.getResource(vwr, className, name);    
    if (resource != null) {
      if (resources == null) {
        resources = new Resource[8];
        resourceCount = 0;
      }
      resources[resourceCount] = resource;
      resourceCount++;
      if (allowDebug)Logger.debug("GT adding " + className);
    }
  }

  private String getString(String s) {
    String trans = null;
    if (doTranslate) {
      // order was incorrect -- should be:
      // la_co_va(app)  la_co_va(applet)  la_co(app)  la_co(applet) la(app)  la(applet)
      for (int bundle = 0; bundle < resourceCount; bundle++) {
        trans = resources[bundle].getString(s);
        if (trans != null) {
          s = trans;
          break;
        }
      }
      if (resourceCount > 0 && trans == null && allowDebug && Logger.debugging)
        Logger.debug("No trans, using default: " + s);

    }
    if (trans == null) {
      if (s.startsWith("["))
        s = s.substring(s.indexOf("]") + 1);
      else if (s.endsWith("]"))
        s = s.substring(0, s.indexOf("["));
    }
    return s;
  }

}
