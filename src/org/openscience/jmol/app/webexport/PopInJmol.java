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

import java.util.Hashtable;
import java.util.Map;

import javajs.util.PT;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jmol.i18n.GT;
import org.jmol.viewer.Viewer;

class PopInJmol extends WebPanel implements ChangeListener {

  PopInJmol(Viewer vwr, JFileChooser fc, WebPanel[] webPanels,
      int panelIndex) {
    super(vwr, fc, webPanels, panelIndex);
    panelName = "pop_in";
    listLabel = GT._("These names will be used as filenames used by JSmol");
    // description = "Create a web page with images that convert to live JSmol
    // objects when a user clicks a link";
  }

  @Override
  JPanel appletParamPanel() {
    // Create the appletSize spinner so the user can decide how big
    // the JSmol object should be.
    SpinnerNumberModel appletSizeModelW = new SpinnerNumberModel(WebExport
        .getPopInWidth(), // initial value
        50, // min
        1000, // max
        25); // step size
    SpinnerNumberModel appletSizeModelH = new SpinnerNumberModel(WebExport
        .getPopInHeight(), // initial value
        50, // min
        1000, // max
        25); // step size
    appletSizeSpinnerW = new JSpinner(appletSizeModelW);
    appletSizeSpinnerW.addChangeListener(this);
    appletSizeSpinnerH = new JSpinner(appletSizeModelH);
    appletSizeSpinnerH.addChangeListener(this);

    // panel to hold spinner and label
    JPanel appletSizeWHPanel = new JPanel();
    appletSizeWHPanel.add(new JLabel(GT._("JSmol width:")));
    appletSizeWHPanel.add(appletSizeSpinnerW);
    appletSizeWHPanel.add(new JLabel(GT._("height:")));
    appletSizeWHPanel.add(appletSizeSpinnerH);
    return (appletSizeWHPanel);
  }

  @Override
  String fixHtml(String html) {
    String s = "";
    int nApplets = getInstanceList().getModel().getSize();
    for (int i=0;i<nApplets;i++){
      String javaname = getInstanceList().getModel().getElementAt(i).javaname;
      s+="   var jmolInfo"+i+"=jmolInfo;\n";
      s+="   jmolInfo"+i+".coverImage=\""+javaname+".png\";\n";
      s+="   jmolInfo"+i+".coverScript=\"javascript revealPopinWidgets("+i+");\";\n";
      s+="   jmolInfo"+i+".script=\"load "+javaname+".spt\";\n";
      s+="   $(\"#Jmol"+i+"\").html(Jmol.getAppletHtml(\"jmolApplet"+i+"\",jmolInfo"+i+"));\n";
      
    }
    html = javajs.util.PT.rep(html,"@APPLETINITIALIZATION@",s);
    return html;
  }

  @Override
  String getAppletDefs(int i, String html, StringBuilder appletDefs,
                       JmolInstance instance) {
    String divClass = (i % 2 == 0 ? "floatRight" : "floatLeft");
    String name = instance.name;
    String javaname = instance.javaname;
    int JmolSizeW = instance.width;
    int JmolSizeH = instance.height;
    String widgetDefs = "";
    if (!instance.whichWidgets.isEmpty()) {
      widgetDefs += "<div id=\"JmolCntl" + i + "\">";
      for (int j = 0; j < nWidgets; j++) {
        if (instance.whichWidgets.get(j)) {
          widgetDefs += "\n<div class=\"widgetItemPopin\">" 
              + theWidgets.widgetList[j].getJavaScript(i, instance) 
              + "</div>\n"; //each widget in one line
        }
      }
      widgetDefs += "</div>";
   }
    String s = htmlAppletTemplate;
    s = PT.rep(s, "@CLASS@", "" + divClass);
    s = PT.rep(s, "@I@", "" + i);
    s = PT.rep(s, "@WIDTH@", "" + JmolSizeW);
    s = PT.rep(s, "@HEIGHT@", "" + JmolSizeH);
    s = PT.rep(s, "@NAME@", "&#x201C;" + GT.escapeHTML(name) + "&#x201D;");
    s = PT.rep(s, "@APPLETNAME@", GT.escapeHTML(javaname));
    s = PT.rep(s, "@LEFTWIDGETS@", "");// no left widgets
                                                         // for now
    s = PT.rep(s, "@RIGHTWIDGETS@", widgetDefs);
    appletDefs.append(s);
    return html;
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    if (e.getSource() == appletSizeSpinnerW
        || e.getSource() == appletSizeSpinnerH) {
      int width = ((SpinnerNumberModel) (appletSizeSpinnerW.getModel()))
          .getNumber().intValue();
      int height = ((SpinnerNumberModel) (appletSizeSpinnerH.getModel()))
          .getNumber().intValue();
      WebExport.setPopInDim(width, height);
      JList<JmolInstance> whichList = getInstanceList();
      int[] list = whichList.getSelectedIndices();
      if (list.length != 1)// may want to make this work on multiple selections
        return;
      int index = whichList.getSelectedIndex();
      JmolInstance instance = whichList.getModel().getElementAt(
          index);
      instance.width = width;
      instance.height = height;
      Map<String, Object> params = new Hashtable<String, Object>();
      params.put("fileName", instance.pictFile);
      params.put("type", "PNG");
      params.put("quality", Integer.valueOf(2));
      params.put("width", Integer.valueOf(width));
      params.put("height", Integer.valueOf(height));
      vwr.outputToFile(params);
      return;
    }

    if (e.getSource() == appletSizeSpinnerP) {
      int percent = ((SpinnerNumberModel) (appletSizeSpinnerP.getModel()))
          .getNumber().intValue();
      WebExport.setScriptButtonPercent(percent);
      return;
    }
  }
}
