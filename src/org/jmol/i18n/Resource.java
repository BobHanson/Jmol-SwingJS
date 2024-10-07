package org.jmol.i18n;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.jmol.translation.PO;
import org.jmol.util.Logger;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.Viewer;

import javajs.util.PT;
import javajs.util.Rdr;

class Resource {

  private ResourceBundle resource;
  private Map<String, String> resourceMap;
  
  @SuppressWarnings("unchecked")
  private Resource(Object resource, String className) {
    if (className == null)
      this.resourceMap = (Map<String, String>) resource;
    else
      this.resource = (ResourceBundle) resource;
  }

  static Resource getResource(Viewer vwr, String className, String name) {
    try {
      BufferedReader br = FileManager.getBufferedReaderForResource(vwr,
          PO.class, "org/jmol/translation/",
          (className.indexOf("Applet") >= 0 ? "JmolApplet/" : "Jmol/") + name
              + ".po");
      String[] data = new String[1];
      Rdr.readAllAsString(br, Integer.MAX_VALUE, false, data, 0);
      String poData = data[0];
      return getResourceFromPO(poData);
    } catch (IOException e) {
      return null;
    }
  }

  String getString(String string) {
    try {
      return (resource == null ? resourceMap.get(string) : resource
          .getString(string));
    } catch (Exception e) {
      return null;
    }
  }

  static String getLanguage() {
    String language = null;
    /**
     * @j2sNative
     * 
     *            language = Jmol.featureDetection.getDefaultLanguage().replace(/-/g,'_');
     * 
     */
    {
      Locale locale = Locale.getDefault();
      if (locale != null) {
        language = locale.getLanguage();
        if (locale.getCountry() != null) {
          language += "_" + locale.getCountry();
          if (locale.getVariant() != null && locale.getVariant().length() > 0)
            language += "_" + locale.getVariant();
        }
      }
    }
    return language;
  }

  /**
   * 
   * applet only -- Simple reading of .po file; necessary for
   * JavaScript; works in Java as well and avoids all those 
   * signed applets.
   * 
   * @param data
   * @return JmolResource
   * 
   */
  static Resource getResourceFromPO(String data) {
    /*
    #: org/jmol/console/GenericConsole.java:94
    msgid "press CTRL-ENTER for new line or paste model data and press Load"
    msgstr ""
    "pulsa Ctrl+Intro para una línea nueva o pega datos de un modelo y luego "
    "pulsa Cargar"
       */
    if (data == null || data.length() == 0)
      return null;
    Map<String, String> map = new Hashtable<String, String>();
    try {
      String[] lines = PT.split(data, "\n");
      int mode = 0;
      String msgstr = "";
      String msgid = "";
      for (int i = 0; i < lines.length; i++) {
        String line = lines[i];
        if (line.length() <= 2) {
          if (mode == 2 && msgstr.length() != 0 && msgid.length() != 0)
            map.put(msgid, msgstr);
          mode = 0;
        } else if (line.indexOf("msgid") == 0) {
          mode = 1;
          msgid = fix(line);
        } else if (line.indexOf("msgstr") == 0) {
          mode = 2;
          msgstr = fix(line);
        } else if (mode == 1) {
          msgid += fix(line);
        } else if (mode == 2) {
          msgstr += fix(line);
        }
      }
    } catch (Exception e) {
    }
    Logger.info(map.size() + " translations loaded");
    return (map.size() == 0 ? null : new Resource(map, null));
  }

  static String fix(String line) {
    if (line.indexOf("\\\"") >= 0)
      line = PT.rep(line, "\\\"", "\"");
    return PT.rep(
        line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\"")), "\\n",
        "\n");
  }

}
