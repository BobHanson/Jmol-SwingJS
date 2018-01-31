/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.i18n;

public class Language {

  /**
   * This is the place to put the list of supported languages. It is accessed
   * by JmolPopup to create the menu list. Note that the names are in GT._
   * even though we set doTranslate false. That ensures that the language name
   * IN THIS LIST is untranslated, but it provides the code xgettext needs in
   * order to provide the list of names that will need translation by translators
   * (the .po files). Later, in JmolPopup.updateLanguageMenu(), GT._() is used
   * again to create the actual, localized menu item name.
   *
   * list order:
   * 
   * The order presented here is the order in which the list will be presented in the 
   * popup menu. In addition, the order of variants is significant. In all cases, place
   * common-language entries in the following order:
   * 
   * la_co_va
   * la_co
   * la
   * 
   * In addition, there really is no need for "la" by itself. Every translator introduces
   * a bias from their originating country. It would be perfectly fine if we had NO "la"
   * items, and just la_co. Thus, we could have just:
   * 
   * pt_BR
   * pt_PT
   * 
   * In this case, the "default" language translation should be entered LAST.
   * 
   * If a user selects pt_ZQ, the code below will find (a) that we don't support pt_ZQ, 
   * (b) that we don't support pt_ZQ_anything, (c) that we don't support pt, and, finally,
   * that we do support pt_PT, and it will select that one, returning to the user the message
   * that language = "pt_PT" instead of pt_ZQ.
   *  
   * For that matter, we don't even need anything more than 
   * 
   * la_co_va
   * 
   * because the algorithm will track that down from anything starting with la, and in all cases
   * find the closest match. 
   * 
   * Introduced in Jmol 11.1.34 
   * Author Bob Hanson May 7, 2007
   * @return  list of codes and untranslated names
   */

  public static Language[] getLanguageList() {
    return new Language[] {
        new Language("ar",    GT._("Arabic"),               "العربية",              false),
        new Language("ast",   GT._("Asturian"),             "Asturian",             false),
        new Language("az",    GT._("Azerbaijani"),          "azərbaycan dili",      false),
        new Language("bs",    GT._("Bosnian"),              "bosanski jezik",       false),
        new Language("ca",    GT._("Catalan"),              "Català",               true),
        new Language("cs",    GT._("Czech"),                "Čeština",              true),
        new Language("da",    GT._("Danish"),               "Dansk",                true),
        new Language("de",    GT._("German"),               "Deutsch",              true),
        new Language("el",    GT._("Greek"),                "Ελληνικά",             false),
        new Language("en_AU", GT._("Australian English"),   "Australian English",   false),
        new Language("en_GB", GT._("British English"),      "British English",      true),
        new Language("en_US", GT._("American English"),     "American English",     true), // global default for "en" will be "en_US"
        new Language("es",    GT._("Spanish"),              "Español",              true),
        new Language("et",    GT._("Estonian"),             "Eesti",                false),
        new Language("eu",    GT._("Basque"),               "Euskara",              true),
        new Language("fi",    GT._("Finnish"),              "Suomi",                true),
        new Language("fo",    GT._("Faroese"),              "Føroyskt",             false),
        new Language("fr",    GT._("French"),               "Français",             true),
        new Language("fy",    GT._("Frisian"),              "Frysk",                false),
        new Language("gl",    GT._("Galician"),             "Galego",               false),
        new Language("hr",    GT._("Croatian"),             "Hrvatski",             false),
        new Language("hu",    GT._("Hungarian"),            "Magyar",               true),
        new Language("hy",    GT._("Armenian"),             "Հայերեն",               false),
        new Language("id",    GT._("Indonesian"),           "Indonesia",            true),
        new Language("it",    GT._("Italian"),              "Italiano",             true),
        new Language("ja",    GT._("Japanese"),             "日本語",               true),
        new Language("jv",    GT._("Javanese"),             "Basa Jawa",            false),
        new Language("ko",    GT._("Korean"),               "한국어",               true),
        new Language("ms",    GT._("Malay"),                "Bahasa Melayu",        true),
        new Language("nb",    GT._("Norwegian Bokmal"),     "Norsk Bokmål",         false),
        new Language("nl",    GT._("Dutch"),                "Nederlands",           true),
        new Language("oc",    GT._("Occitan"),              "Occitan",              false),
        new Language("pl",    GT._("Polish"),               "Polski",               false),
        new Language("pt",    GT._("Portuguese"),           "Português",            false),
        new Language("pt_BR", GT._("Brazilian Portuguese"), "Português brasileiro", true),
        new Language("ru",    GT._("Russian"),              "Русский",              true),
        new Language("sl",    GT._("Slovenian"),            "Slovenščina",          false),
        new Language("sr",    GT._("Serbian"),              "српски језик",         false),
        new Language("sv",    GT._("Swedish"),              "Svenska",              true),
        new Language("ta",    GT._("Tamil"),                "தமிழ்",                 false),
        new Language("te",    GT._("Telugu"),               "తెలుగు",                  false),
        new Language("tr",    GT._("Turkish"),              "Türkçe",               true),
        new Language("ug",    GT._("Uyghur"),               "Uyƣurqə",              false),
        new Language("uk",    GT._("Ukrainian"),            "Українська",           true),
        new Language("uz",    GT._("Uzbek"),                "O'zbek",               false),
        new Language("zh_CN", GT._("Simplified Chinese"),   "简体中文",              true),
        new Language("zh_TW", GT._("Traditional Chinese"),  "繁體中文",              true),
      };
  }
  
  public final String code;
  public final String language;
  public final String nativeLanguage;
  public boolean display;

  /**
   * @param code Language code (see ISO 639-1 for the values)
   * @param language Language name in English (see ISO 639-1 for the values)
   * @param nativeLanguage Language name in its own language (see ISO 639-1 for the values)
   * @param display True if this language has a good percentage of translations done
   * 
   * {@link "http://en.wikipedia.org/wiki/List_of_ISO_639-1_codes"}
   */
  private Language(String code, String language, String nativeLanguage, boolean display) {
    this.code = code;
    this.language = language;
    this.nativeLanguage = nativeLanguage;
    this.display = display;
  }

  static String getSupported(Language[] list, String code) {
    for (int i = list.length; --i >= 0;)
      if (list[i].code.equalsIgnoreCase(code))
        return list[i].code;
      for (int i = list.length; --i >= 0;)
        if (list[i].code.startsWith(code))
          return list[i].code;
    return null;
  }

}