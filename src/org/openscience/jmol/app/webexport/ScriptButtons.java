/* $RCSfile$
 * $Author jonathan gutow$
 * $Date Aug 5, 2007 9:19:06 AM $
 * $Revision$
 * Updated Dec. 2015 by Angel Herraez
 * valid for JSmol
 *
 * Copyright (C) 2005-2016  The Jmol Development Team
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
package org.openscience.jmol.app.webexport;


import javajs.util.PT;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.jmol.i18n.GT;
import org.jmol.viewer.Viewer;

class ScriptButtons extends WebPanel {

  ScriptButtons(Viewer vwr, JFileChooser fc, WebPanel[] webPanels,
      int panelIndex) {
    super(vwr, fc, webPanels, panelIndex);
    panelName = "script_btn";
    listLabel = GT._("These names will be used for button labels");
    //description = "Create a web page containing a text and button pane that scrolls next to a resizable Jmol applet";
  }

  @Override
  JPanel appletParamPanel() {
    SpinnerNumberModel appletSizeModel = new SpinnerNumberModel(WebExport.getScriptButtonPercent(), //initial value
        20, //min
        100, //max
        5); //step size
    appletSizeSpinnerP = new JSpinner(appletSizeModel);
    //panel to hold spinner and label
    JPanel appletSizePPanel = new JPanel();
    appletSizePPanel.add(new JLabel(GT._("% of window for JSmol width:")));
    appletSizePPanel.add(appletSizeSpinnerP);
    return (appletSizePPanel);
  }

  @Override
  String fixHtml(String html) {
    int size = ((SpinnerNumberModel) (appletSizeSpinnerP.getModel()))
        .getNumber().intValue();
    int appletheightpercent = 100;
    int nbuttons = getInstanceList().getModel().getSize();
    if (!allSelectedWidgets().isEmpty())
      appletheightpercent = 85;
    html = PT.rep(html, "@WIDTHPERCENT@", "" + size);
    html = PT.rep(html, "@LEFTPERCENT@", "" + (100 - size));
    html = PT.rep(html, "@NBUTTONS@", "" + nbuttons);
    html = PT.rep(html, "@HEIGHT@", "" + appletheightpercent);
    html = PT.rep(html, "@BOTTOMPERCENT@", "" + (100 - appletheightpercent));
    return html;
  }

  @Override
  String getAppletDefs(int i, String html, StringBuilder appletDefs,
                       JmolInstance instance) {
    /* The widgets are built as hidden divs in the scrolling region,
      upon page load their html is copied into variables,
      and then when a button is pressed its set of widgets is read from the variable 
      and filled into the common widget area below the JSmol panel.
      Note: widget code must be removed from the original place to avoid duplicate IDs.
    */
    String name = instance.name;
    String buttonname = instance.javaname;
    String widgetDefs = "";
    int row = 0;
    if (!instance.whichWidgets.isEmpty()) {
      if (instance.whichWidgets.get(3)) {
        //special:  widgetList[3] is AnimationWidget, taller than the others, put it to the right
        widgetDefs += "<div class=\"widgetItemAnim\"> "
        + theWidgets.widgetList[3].getJavaScript(0, instance)
        + "</div>";
      }
      widgetDefs += "<table><tbody><tr>";
      for (int j = 0; j < nWidgets; j++) {
        if (j==3) { continue; }
        if (instance.whichWidgets.get(j)) {
          if (row == 3) {
            widgetDefs += "</tr><tr>";
            row = 0;
          }
          widgetDefs += "<td class=\"widgetItemScBtn\">"
              + theWidgets.widgetList[j].getJavaScript(0, instance)
                  //does nothing? .replace("'", "\'")
                  + "</td>";
          row = row + 1;
        }
      }
      widgetDefs += "</tr></tbody></table>";
    }
    if (i == 0) {
      html = PT.rep(html, "@APPLETNAME0@", GT.escapeHTML(buttonname));
    }
    String s = htmlAppletTemplate;
    s = PT.rep(s, "@APPLETNAME0@", GT.escapeHTML(buttonname));
    s = PT.rep(s, "@NAME@", "&#x201C;" + GT.escapeHTML(name) + "&#x201D;");
    s = PT.rep(s, "@LABEL@", GT.escapeHTML(name));
    s = PT.rep(s, "@I@", ""+i);
    s = PT.rep(s, "@WIDGETSTR@", widgetDefs);
    appletDefs.append(s);
    return html;
  }
}
