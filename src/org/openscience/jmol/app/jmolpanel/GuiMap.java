/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2019-11-17 22:49:25 -0600 (Sun, 17 Nov 2019) $
 * $Revision: 22002 $
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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.PT;

import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.AbstractButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JCheckBox;

import org.jmol.console.GenericConsole;
import org.jmol.console.JmolButton;
import org.jmol.console.KeyJMenu;
import org.jmol.console.KeyJMenuItem;
import org.jmol.console.KeyJCheckBox;
import org.jmol.console.KeyJCheckBoxMenuItem;
import org.jmol.console.KeyJRadioButtonMenuItem;
import org.jmol.i18n.GT;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

public class GuiMap {

  private static Object language;

  Map<String, Object> map = new Hashtable<String, Object>();

  protected Map<String, String> labels;

  // keys here refer to keys listed in org.openscience.jmol.Properties.Jmol-resources.properties
  // actions are either defined there, as xxxScript=, or by 
  // Actions created in DisplayPanel.java

  private void setupLabels() {
    labels = new Hashtable<String, String>();
    labels.put("macros", GT.$("&Macros"));
    labels.put("idfileMenu", GT.$("&File"));
    labels.put("file", GT.$("&File"));
    labels.put("newwin", GT.$("&New"));
    labels.put("open", GT.$("&Open"));
    labels.put("openTip", GT.$("Open a file."));
    labels.put("openurl", GT.$("Open &URL"));
    labels.put("openpdb", GT.$("&Get PDB"));
    labels.put("openmol", GT.$("Get &MOL"));
    labels.put("reloadScript", GT.$("&Reload"));

    labels.put("spectrumMenu", "&Spectra");

    labels.put("openJSpecViewScript", "JSpecView");
    labels.put("simulate1HSpectrumScript", "Simulated 1H Spectrum");
    labels.put("simulate13CSpectrumScript", "Simulated 13C Spectrum");

    labels.put("editor", GT.$("Scrip&t Editor...")); // new %t 11.7.45
    labels.put("console", GT.$("Conso&le..."));
    labels.put("jconsole", GT.$("Jmol Java &Console"));
    labels.put("atomsetchooser", GT.$("AtomSet&Chooser..."));
    labels.put("saveas", GT.$("&Save As..."));
    labels.put("exportMenu", GT.$("&Export"));
    labels.put("export", GT.$("Export &Image..."));
    labels.put("exportTip", GT.$("Save current view as an image."));
    labels.put("toweb", GT.$("Export to &Web Page..."));
    labels.put("towebTip", GT.$("Export one or more views to a web page."));
    labels.put("povray", GT.$("Render in POV-&Ray..."));
    labels.put("povrayTip", GT.$("Render in POV-Ray"));
    labels.put("write", GT.$("Write &State..."));
    labels.put("writeTip", GT.$("Save current view as a Jmol state script."));
    labels.put("print", GT.$("&Print..."));
    labels.put("printTip", GT.$("Print view."));
    labels.put("close", GT.$("&Close"));
    labels.put("exit", GT.$("E&xit"));
    labels.put("recentFiles", GT.$("Recent &Files..."));
    labels.put("edit", GT.$("&Edit"));
    // labels.put("makecrystal", GT. _("Make crystal..."));
    labels.put("selectall", GT.$("Select &All"));
    labels.put("deselectall", GT.$("Deselect All"));
    labels.put("copyImage", GT.$("Copy &Image"));
    labels.put("copyScript", GT.$("Copy Script"));
    labels.put("prefs", GT.$("Pr&eferences..."));
    labels.put("pasteClipboard", GT.$("&Paste"));
    labels.put("editSelectAllScript", GT.$("Select &All"));
    labels.put("selectMenu", GT.$("&Select"));
    labels.put("selectMenuText", GT.$("&Select"));
    labels.put("selectAllScript", GT.$("&All"));
    labels.put("selectNoneScript", GT.$("&None"));
    labels.put("selectHydrogenScript", GT.$("Hydrogen"));
    labels.put("selectCarbonScript", GT.$("Carbon"));
    labels.put("selectNitrogenScript", GT.$("Nitrogen"));
    labels.put("selectOxygenScript", GT.$("Oxygen"));
    labels.put("selectPhosphorusScript", GT.$("Phosphorus"));
    labels.put("selectSulfurScript", GT.$("Sulfur"));
    labels.put("selectAminoScript", GT.$("Amino"));
    labels.put("selectNucleicScript", GT.$("Nucleic"));
    labels.put("selectWaterScript", GT.$("Water"));
    labels.put("selectHeteroScript", GT.$("Hetero"));
    labels.put("display", GT.$("&Display"));
    labels.put("atomMenu", GT.$("&Atom"));
    labels.put("atomNoneScript", GT.$("&None"));
    labels.put("atom15Script", GT.o(GT.$("{0}% van der Waals"), "15"));
    labels.put("atom20Script", GT.o(GT.$("{0}% van der Waals"), "20"));
    labels.put("atom25Script", GT.o(GT.$("{0}% van der Waals"), "25"));
    labels.put("atom100Script", GT.o(GT.$("{0}% van der Waals"), "100"));
    labels.put("bondMenu", GT.$("&Bond"));
    labels.put("bondNoneScript", GT.$("&None"));
    labels.put("bondWireframeScript", GT.$("&Wireframe"));
    labels.put("bond100Script", GT.o(GT.$("{0} \u00C5"), "0.10"));
    labels.put("bond150Script", GT.o(GT.$("{0} \u00C5"), "0.15"));
    labels.put("bond200Script", GT.o(GT.$("{0} \u00C5"), "0.20"));
    labels.put("labelMenu", GT.$("&Label"));
    labels.put("labelNoneScript", GT.$("&None"));
    labels.put("labelSymbolScript", GT.$("&Symbol"));
    labels.put("labelNameScript", GT.$("&Name"));
    labels.put("labelNumberScript", GT.$("&Number"));
    labels.put("labelCenteredScript", GT.$("&Centered"));
    labels.put("labelUpperRightScript", GT.$("&Upper right"));
    labels.put("vectorMenu", GT.$("&Vector"));
    labels.put("vectorOffScript", GT.$("&None"));
    labels.put("vectorOnScript", GT.$("&On"));
    labels.put("vector3Script", GT.o(GT.$("{0} pixels"), "3"));
    labels.put("vector005Script", GT.o(GT.$("{0} \u00C5"), "0.05"));
    labels.put("vector01Script", GT.o(GT.$("{0} \u00C5"), "0.1"));
    labels.put("vectorScale02Script", GT.o(GT.$("Scale {0}"), "0.2"));
    labels.put("vectorScale05Script", GT.o(GT.$("Scale {0}"), "0.5"));
    labels.put("vectorScale1Script", GT.o(GT.$("Scale {0}"), "1"));
    labels.put("vectorScale2Script", GT.o(GT.$("Scale {0}"), "2"));
    labels.put("vectorScale5Script", GT.o(GT.$("Scale {0}"), "5"));
    labels.put("zoomMenu", GT.$("&Zoom"));
    labels.put("zoom100Script", GT.o(GT.$("{0}%"), "100"));
    labels.put("zoom150Script", GT.o(GT.$("{0}%"), "150"));
    labels.put("zoom200Script", GT.o(GT.$("{0}%"), "200"));
    labels.put("zoom400Script", GT.o(GT.$("{0}%"), "400"));
    labels.put("zoom800Script", GT.o(GT.$("{0}%"), "800"));
    labels.put("perspectiveCheck", GT.$("&Perspective Depth"));
    labels.put("axesCheck", GT.$("A&xes"));
    labels.put("boundboxCheck", GT.$("B&ounding Box"));
    labels.put("hydrogensCheck", GT.$("&Hydrogens"));
    labels.put("vectorsCheck", GT.$("V&ectors"));
    labels.put("measurementsCheck", GT.$("&Measurements"));
    labels.put("resize", GT.$("Resi&ze"));
    labels.put("view", GT.$("&View"));
    labels.put("front", GT.$("&Front"));
    labels.put("top", GT.$("&Top"));
    labels.put("bottom", GT.$("&Bottom"));
    labels.put("right", GT.$("&Right"));
    labels.put("left", GT.$("&Left"));
    labels.put("axisaScript", GT.$("Axis a"));
    labels.put("axisbScript", GT.$("Axis b"));
    labels.put("axiscScript", GT.$("Axis c"));
    labels.put("axisxScript", GT.$("Axis x"));
    labels.put("axisyScript", GT.$("Axis y"));
    labels.put("axiszScript", GT.$("Axis z"));
    labels.put("transform", GT.$("Tr&ansform..."));
    labels.put("definecenter", GT.$("Define &Center"));
    labels.put("tools", GT.$("&Tools"));
    labels.put("gauss", GT.$("&Gaussian..."));
    labels.put("viewMeasurementTable", GT.$("&Measurements") + "...");
    labels.put("distanceUnitsMenu", GT.$("Distance &Units"));
    labels.put("distanceNanometersScript", GT.$("&Nanometers 1E-9"));
    labels.put("distanceAngstromsScript", GT.$("&Angstroms 1E-10"));
    labels.put("distancePicometersScript", GT.$("&Picometers 1E-12"));
    labels.put("distanceHzScript", GT.$("&Hz (NMR J-coupling)"));
    labels.put("animateMenu", GT.$("&Animate..."));
    labels.put("vibrateMenu", GT.$("&Vibrate..."));
    // these three are not implemented:
    labels.put("graph", GT.$("&Graph..."));
    labels.put("chemicalShifts", GT.$("Calculate chemical &shifts..."));
    labels.put("crystprop", GT.$("&Crystal Properties"));
    //
    labels.put("animateOnceScript", GT.$("&Once"));
    labels.put("animateLoopScript", GT.$("&Loop"));
    labels.put("animatePalindromeScript", GT.$("P&alindrome"));
    labels.put("animateStopScript", GT.$("&Stop animation"));
    labels.put("animateRewindScript", GT.$("&Rewind to first frame"));
    labels.put("animateRewindScriptTip", GT.$("Rewind to first frame"));
    labels.put("animateNextScript", GT.$("Go to &next frame"));
    labels.put("animateNextScriptTip", GT.$("Go to next frame"));
    labels.put("animatePrevScript", GT.$("Go to &previous frame"));
    labels.put("animatePrevScriptTip", GT.$("Go to previous frame"));
    labels.put("animateAllScript", GT.$("All &frames"));
    labels.put("animateAllScriptTip", GT.$("All frames"));
    labels.put("animateLastScript", GT.$("Go to &last frame"));
    labels.put("animateLastScriptTip", GT.$("Go to last frame"));
    labels.put("vibrateStartScript", GT.$("Start &vibration"));
    labels.put("vibrateStopScript", GT.$("&Stop vibration"));
    labels.put("vibrateRewindScript", GT.$("&First frequency"));
    labels.put("vibrateNextScript", GT.$("&Next frequency"));
    labels.put("vibratePrevScript", GT.$("&Previous frequency"));
    labels.put("surfaceTool", GT.$("SurfaceTool..."));
    labels.put("surfaceToolTip", GT.$("Control Display of Surfaces"));
    labels.put("help", GT.$("&Help"));
    labels.put("about", GT.$("About Jmol"));
    labels.put("uguide", GT.$("Jmol Wiki"));
    labels.put("whatsnew", GT.$("What's New"));
    labels.put("credits", GT.$("Credits"));
    labels.put("Prefs.showHydrogens", GT.$("Hydrogens"));
    labels.put("Prefs.showMeasurements", GT.$("Measurements"));
    labels.put("Prefs.perspectiveDepth", GT.$("Perspective Depth"));
    labels.put("Prefs.showAxes", GT.$("Axes"));
    labels.put("Prefs.showBoundingBox", GT.$("Bounding Box"));
    labels.put("Prefs.axesOrientationRasmol",
        GT.$("RasMol/Chime compatible axes orientation/rotations"));
    labels.put("Prefs.openFilePreview",
        GT.$("File Preview (requires restarting Jmol)"));
    labels.put("Prefs.clearHistory",
        GT.$("Clear history (requires restarting Jmol)"));
    labels.put("Prefs.largeFont", GT.$("Large Console Font"));
    labels.put("Prefs.isLabelAtomColor", GT.$("Use Atom Color"));
    labels.put("Prefs.isBondAtomColor", GT.$("Use Atom Color"));
    labels.put("rotateScriptTip", GT.$("Rotate molecule."));
    labels.put("pickScriptTip",
        GT.$("Select a set of atoms using SHIFT-LEFT-DRAG."));
    labels
        .put("pickMeasureScriptTip", GT.$("Click atoms to measure distances"));
    labels.put("pickCenterScriptTip", GT.$("Click an atom to center on it"));
    labels
        .put(
            "pickLabelScriptTip",
            GT.$("click an atom to toggle label;DOUBLE-Click a label to set; drag to move"));
    labels.put("homeTip", GT.$("Return molecule to home position."));
    labels.put("modelkitScriptTip", GT.$("Open the model kit."));
    labels.put("JavaConsole.Clear", GT.$("Clear"));
    labels.put("plugins", GT.$("&Plugins"));

    moreLabels(labels);

  }

  /**
   * Add more labels if desired
   * 
   * @param labels
   */
  protected void moreLabels(Map<String, String> labels) {
    //labels.put("plugins", GT.$("&Plugins"));
  }

  public String getLabel(String key) {
    if (labels == null)
      setupLabels();
    String s = labels.get(key);
    if (s == null || s.length() == 0) {
      System.err.println("GUI key? " + key);
      return key;
    }
    return s;
  }

  public JMenu newJMenu(String key) {
    return new KeyJMenu(key, getLabel(key), map);
  }

  public JMenuItem newJMenuItem(String key) {
    return new KeyJMenuItem(key, getLabel(key), map);
  }

  public JCheckBoxMenuItem newJCheckBoxMenuItem(String key, boolean isChecked) {
    return new KeyJCheckBoxMenuItem(key, getLabel(key), map, isChecked);
  }

  public JRadioButtonMenuItem newJRadioButtonMenuItem(String key) {
    return new KeyJRadioButtonMenuItem(key, getLabel(key), map);
  }

  public JCheckBox newJCheckBox(String key, boolean isChecked) {
    return new KeyJCheckBox(key, getLabel(key), map, isChecked);
  }

  public JButton newJButton(String key) {
    JButton jb = new JmolButton(getLabel(key));
    map.put(key, jb);
    return jb;
  }

  public Object get(String key) {
    return map.get(key);
  }

  public void setSelected(String key, boolean b) {
    ((AbstractButton) get(key)).setSelected(b);
  }

  public void setEnabled(String key, boolean b) {
    ((AbstractButton) get(key)).setEnabled(b);
  }

  public void updateLabels() {
    boolean doTranslate = GT.setDoTranslate(true);
    setupLabels();
    GenericConsole.setAbstractButtonLabels(map, labels);
    GT.setDoTranslate(doTranslate);
  }

  public static String translate(String str) {
    if (translations == null || !GT.getLanguage().equals(language))
      setTranslations();
    language = GT.getLanguage();
    for (int i = 0; i < translations.length; i += 2) {
      String t = translations[i];
      if (str.indexOf(t) >= 0) {
        String s = translations[i + 1];
        if (s.equals("see Jmol-resources.properties"))
          s = JmolResourceHandler.getStringX(t);
        str = PT.rep(str, t, s);
      }
    }
    return str;
  }

  public static URL getResource(Object object, String fileName) {
    return getResource(object, fileName, true);
  }

  public static URL getHtmlResource(Object object, String root) {
    String lang = GT.getLanguage();
    String fileName = root + "_" + lang + ".html";
    URL url = getResource(object, fileName, false);
    if (url == null && lang.length() == 5) {
      fileName = root + "_" + lang.substring(0, 2) + ".html";
      url = getResource(object, fileName, false);
    }
    if (url == null) {
      fileName = root + ".html";
      url = getResource(object, fileName, true);
    }
    return url;
  }

  /**
   * @param object
   *        UNUSED
   * @param fileName
   * @param flagError
   * @return URL
   */
  public static URL getResource(Object object, String fileName,
                                boolean flagError) {
    URL url = null;
    if (fileName.indexOf("/org/") > 0)
      fileName = fileName.substring(fileName.indexOf("/org/") + 1);
    if (!fileName.contains("/"))
      fileName = "org/openscience/jmol/app/webexport/html/" + fileName;
    try {
      if ((url = ClassLoader.getSystemResource(fileName)) == null && flagError)
        System.err.println("Couldn't find file: " + fileName);
    } catch (Exception e) {
      System.err.println("Exception " + e.getMessage() + " in getResource "
          + fileName);
    }
    return url;
  }

  public static String getResourceString(Object object, String name)
      throws IOException {
    URL url = (name.indexOf(".") >= 0 ? getResource(object, name)
        : getHtmlResource(object, name));
    if (url == null) {
      throw new FileNotFoundException("Error loading resource " + name);
    }
    StringBuilder sb = new StringBuilder();
    try {
      //turns out from the Jar file
      // it's a sun.net.www.protocol.jar.JarURLConnection$JarURLInputStream
      // and within Eclipse it's a BufferedInputStream
      //LogPanel.log(name + " : " + url.getContent().toString());
      BufferedReader br = new BufferedReader(new InputStreamReader(
          (InputStream) url.getContent()));
      String line;
      while ((line = br.readLine()) != null)
        sb.append(line).append("\n");
      br.close();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
    return translate(sb.toString());
  }

  private static String[] translations;

  /**
   * allows for web page material to be internationalized, inserting
   * language-specific code, as for WebExport, or by inserting boiler-plate
   * information, as for About_xx.html
   * 
   */
  private static void setTranslations() {
    // for all templates and JmolPopIn.js
    translations = new String[] {
        "GT_JmolPopIn.js_TOGETA3DMODEL",
        GT.escapeHTML(GT.o(
            GT.$("To get a 3-D model you can manipulate, click {0}here{1}. Download time may be significant the first time the applet is loaded."),
            new String[] { "<a href=\"HREF\">", "</a>" })),

        "GT_pop_in_template.html_INSERTTITLE",
        GT.escapeHTML(GT.$("Insert the page TITLE here.")),
        "GT_pop_in_template.html_INSERTINTRO",
        GT.escapeHTML(GT.$("Insert the page INTRODUCTION here.")),

        "GT_pop_in_template2.html_INSERTCAPTION",
        GT.escapeHTML(GT.o(
            GT.$("CLICK IMAGE TO ACTIVATE 3D <br/> Insert a caption for {0} here."),
            "@NAME@")),
        "GT_pop_in_template2.html_INSERTADDITIONAL",
        GT.escapeHTML(GT.o(
            GT.$("Insert additional explanatory text here. Long text will wrap around Jmol model {0}."),
            "@NAME@")),

        "GT_script_btn_template.html_INSERT",
        GT.escapeHTML(GT.$("Insert your TITLE and INTRODUCTION here.")),
        "GT_script_btn_template.html_LOADING",
        GT.escapeHTML(GT.o(
            GT.$("Once the molecule file is fully loaded, the image at right will become live.  At that time the \"activate 3-D\" icon {0} will disappear."),
            new String[] { "<img id=\"make_live_icon\" src=\"\" height=\"15px\" />" })),
        "GT_script_btn_template.html_VIEWAGAIN",
        GT.escapeHTML(GT
            .$("You may look at any of these intermediate views again by clicking on the appropriate button.")),
        "GT_script_btn_template.html_JAVACAPABLE",
        GT.escapeHTML(GT
            .$("If your browser/OS combination is Java capable, you will get snappier performance if you <a href=\"?use=JAVA\">use Java</a>")),
        "GT_script_btn_template2.html_BUTTONINFO",
        GT.escapeHTML(GT.o(
            GT.$("The button {0} will appear below.  Insert information for {0} here and below."),
            "@NAME@")),
        "GT_script_btn_template2.html_MORE",
        GT.escapeHTML(GT.o(GT.$("Insert more information for {0} here."),
            "@NAME@")), "About.html#version",
        "<p><b>Jmol " + Viewer.getJmolVersion() + "</b></p>",
        "About.html#splash", "see Jmol-resources.properties",
        "About.html#weblinks", "see Jmol-resources.properties",
        "About.html#libraries", "see Jmol-resources.properties" };
  }

}
