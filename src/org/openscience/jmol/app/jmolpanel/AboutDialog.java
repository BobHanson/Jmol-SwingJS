/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-05-24 21:30:49 -0500 (Tue, 24 May 2016) $
 * $Revision: 21129 $
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


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URL;

import javajs.util.PT;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.jmol.api.JmolViewer;
import org.jmol.i18n.GT;

class AboutDialog extends JDialog implements HyperlinkListener {

  private JmolViewer vwr;
    
  AboutDialog(JFrame fr, JmolViewer vwr) throws IOException {
    super(fr, GT._("About Jmol"), true);
    this.vwr = vwr;
    JScrollPane scroller = new JScrollPane() {
      @Override
      public Dimension getPreferredSize() {
        return new Dimension(750, 650);
      }

      @Override
      public float getAlignmentX() {
        return LEFT_ALIGNMENT;
      }
    };
    JEditorPane html = new JEditorPane();
    html.setContentType("text/html");
    html.setText(PT.rep(GuiMap.getResourceString(this, getClass().getClassLoader()
        .getResource(JmolResourceHandler.getStringX("About.aboutURL")).getPath()),
        "SPLASH", "" + getClass().getResource("about.jpg")));
    html.setEditable(false);
    html.addHyperlinkListener(this);
    scroller.getViewport().add(html);
    JPanel htmlWrapper = new JPanel(new BorderLayout());
    htmlWrapper.setAlignmentX(LEFT_ALIGNMENT);
    htmlWrapper.add(scroller, BorderLayout.CENTER);
    JPanel container = new JPanel();
    container.setLayout(new BorderLayout());
    container.add(htmlWrapper, BorderLayout.CENTER);
    getContentPane().add(container);
    pack();
    Dimension screenSize = getToolkit().getScreenSize();
    Dimension size = getSize();
    setLocation(screenSize.width / 2 - size.width / 2, screenSize.height / 2
        - size.height / 2);
  }

  @Override
  public void hyperlinkUpdate(HyperlinkEvent e) {
    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
      linkActivated(e.getURL());
    }
  }

  /**
   * Opens a web page in an external browser
   * 
   * @param url
   *        the URL to follow
   */
  protected void linkActivated(URL url) {
    vwr.showUrl(url.toString());
  }
}
