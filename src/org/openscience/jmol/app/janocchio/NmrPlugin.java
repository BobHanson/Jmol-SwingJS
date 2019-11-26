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

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.jmol.c.CBK;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.JmolPlugin;

public class NmrPlugin implements JmolPlugin {

  Nmr nmrApp;
  Viewer jmolAppViewer;
  boolean started;
  private String jmolUnits;

  static String notification = "The Janocchio plugin is experimental; it has not been fully tested. Double-click and drag to create measurements in Hz.";
  
  @Override
  public void start(JFrame frame, Viewer vwr, Map<String, Object> jmolOptions) {
    jmolAppViewer = vwr;
    startApp();
  }

  private void startApp() {
    jmolUnits = (String) jmolAppViewer.evaluateExpression("measurementUnits");
    nmrApp = new Nmr(new String[] { "--Plugin", "-g" + jmolAppViewer.getScreenWidth() + "x"
        + jmolAppViewer.getScreenHeight() }, this);
    nmrApp.mainFrame.addWindowListener(new WindowListener() {

      @Override
      public void windowOpened(WindowEvent e) {
        if (notification != null) {
          JOptionPane.showMessageDialog(nmrApp.mainFrame, notification);
          nmrApp.nmrPanel.coupleTable.setCHequation("none");
        }
        notification = null;
      }

      @Override
      public void windowClosing(WindowEvent e) {
        transferStateToJmol();
        nmrApp = null;
        started = false;
      }

      @Override
      public void windowClosed(WindowEvent e) {
      }

      @Override
      public void windowIconified(WindowEvent e) {
      }

      @Override
      public void windowDeiconified(WindowEvent e) {
      }

      @Override
      public void windowActivated(WindowEvent e) {
      }

      @Override
      public void windowDeactivated(WindowEvent e) {
      }

    });
    started = true;
  }

  private void transferStateFromJmol() {
    String state = jmolAppViewer.getStateInfo();
    nmrApp.nmrPanel.vwr.script(state);
    jmolAppViewer.script("select none;set measurementUnits HZ");
  }

  void transferStateToJmol() {
    jmolAppViewer.script(nmrApp.nmrPanel.vwr.getStateInfo());
    jmolAppViewer.script("select off none;set measurementUnits " + jmolUnits);
  }

  @Override
  public void destroy() {
    nmrApp.nmrPanel.frame.dispose();
    started = false;
  }

  @Override
  public String getVersion() {
    return Nmr.VERSION;
  }

  @Override
  public String getName() {
    return "Janocchio";
  }

  @Override
  public void setVisible(boolean b) {
    if (!b) {
      transferStateToJmol();
    }
    if (started) {
      transferStateFromJmol();
      nmrApp.nmrPanel.frame.setVisible(b);
      //    } else if (b) {
      //      startApp();
    }
  }

  @Override
  public void notifyCallback(CBK type, Object[] data) {
    // TODO    
  }

  @Override
  public ImageIcon getMenuIcon() {
    return null;
  }

  @Override
  public String getMenuText() {
    return "Janocchio NMR";
  }

  @Override
  public boolean isStarted() {
    return started;
  }

  @Override
  public String getWebSite() {
    return "http://janocchio.sourceforge.net/index.html";
  }

}
