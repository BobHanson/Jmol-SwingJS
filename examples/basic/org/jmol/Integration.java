package org.jmol;

/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolViewer;
import org.jmol.util.Logger;
import org.openscience.jmol.app.jmolpanel.console.AppConsole;

/**
 * A example of integrating the Jmol viewer into a java application, with optional console.
 *
 * <p>I compiled/ran this code directly in the examples directory by doing:
 * <pre>
 * javac -classpath ../Jmol.jar Integration.java
 * java -cp .:../Jmol.jar Integration
 * </pre>
 *
 * @author Miguel <miguel@jmol.org>
 */

public class Integration {

  /*
   * Demonstrates a simple way to include an optional console along with the applet.
   * 
   */
  public static void main(String[] argv) {
    JFrame frame = new JFrame("Hello");
    frame.addWindowListener(new ApplicationCloser());
    frame.setSize(410, 700);
    Container contentPane = frame.getContentPane();
    JmolPanel jmolPanel = new JmolPanel();
    jmolPanel.setPreferredSize(new Dimension(400, 400));

    // main panel -- Jmol panel on top

    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(jmolPanel);
    
    // main panel -- console panel on bottom
    
    JPanel panel2 = new JPanel();
    panel2.setLayout(new BorderLayout());
    panel2.setPreferredSize(new Dimension(400, 300));
    AppConsole console = new AppConsole(jmolPanel.viewer, panel2,
        "History State Clear");
    
    // You can use a different JmolStatusListener or JmolCallbackListener interface
    // if you want to, but AppConsole itself should take care of any console-related callbacks
    jmolPanel.viewer.setJmolCallbackListener(console);
    
    panel.add("South", panel2);
    
    contentPane.add(panel);
    frame.setVisible(true);

    // sample start-up script
    
    String strError = jmolPanel.viewer
        .openFile("http://chemapps.stolaf.edu/jmol/docs/examples-11/data/caffeine.xyz");
    //viewer.openStringInline(strXyzHOH);
    if (strError == null)
      jmolPanel.viewer.evalString(strScript);
    else
      Logger.error(strError);
  }

  final static String strXyzHOH = "3\n" 
    + "water\n" 
    + "O  0.0 0.0 0.0\n"
    + "H  0.76923955 -0.59357141 0.0\n" 
    + "H -0.76923955 -0.59357141 0.0\n";

  final static String strScript = "delay; move 360 0 0 0 0 0 0 0 4;";

  static class ApplicationCloser extends WindowAdapter {
    @Override
    public void windowClosing(WindowEvent e) {
      System.exit(0);
    }
  }

  static class JmolPanel extends JPanel {

    JmolViewer viewer;
    
    private final Dimension currentSize = new Dimension();
    
    JmolPanel() {
      viewer = JmolViewer.allocateViewer(this, new SmarterJmolAdapter(), 
          null, null, null, null, null);
    }

    @Override
    public void paint(Graphics g) {
      getSize(currentSize);
      viewer.renderScreenImage(g, currentSize.width, currentSize.height);
    }
  }

}
