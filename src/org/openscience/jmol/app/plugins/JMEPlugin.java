/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2018-07-22 20:29:48 -0500 (Sun, 22 Jul 2018) $
 * $Revision: 21922 $
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
package org.openscience.jmol.app.plugins;

import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.jmol.api.Interface;
import org.jmol.c.CBK;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.JmolPlugin;
import org.openscience.jmol.app.jmolpanel.JmolResourceHandler;

/**
 * SAME as for legacy Jmol, except for the location of JMEJmol
 * 
 * An extension of JME that adds features of Jmol, such as many more file types
 * for reading, writing of various formats, and substructure searching.
 * 
 * @author hansonr
 *
 */
public class JMEPlugin implements JmolPlugin {

  JMEJmolI jmeJmol;
  private boolean headless;
  
  @Override
  public void destroy() {
    jmeJmol = null;
  }

  @Override
  public String getLicense() {
    return null;
  }

  @Override
  public ImageIcon getMenuIcon() {
    return JmolResourceHandler.getIconX("2dButton.png");
  }

  @Override
  public String getMenuText() {
    return "JME 2D Editor";
  }

  @Override
  public String getName() {
    return "JME";
  }

  @Override
  public String getVersion() {
    // TODO
    return "";
  }

  @Override
  public String getWebSite() {
    // TODO
    return null;
  }

  @Override
  public boolean isStarted() {
    // TODO
    return jmeJmol != null;
  }

  @Override
  public boolean isVisible() {
    // a JPanel
    return jmeJmol != null && jmeJmol.isFrameVisible();
  }

  @Override
  public void notifyCallback(CBK type, Object[] data) {
    // nothing to do here, for now    
  }

  @Override
  public void setVisible(boolean b) {
    if (jmeJmol != null)
      jmeJmol.setFrameVisible(true);
  }

  @Override
  public void start(JFrame parentFrame, Viewer vwr, Map<String, Object> jmolOptions, boolean headless) {
    this.headless = headless;
    if (jmeJmol == null) {
      String classFile = (Viewer.isJmolSwingJS ? "jme.JMEJmol" : "org.openscience.jmol.app.jme.JMEJmol");      
      jmeJmol = (JMEJmolI) Interface.getInterface(classFile, vwr, "plugin");
      jmeJmol.setViewer(null, vwr, (headless ? null : parentFrame), "jmol", headless);
    }
    if (!headless)
      jmeJmol.setFrameVisible(true);
    SwingUtilities.invokeLater(new Runnable() {

      @Override
      public void run() {
        jmeJmol.from3D();
      }      
    });
  }

  @Override
  public Object processRequest(String action, Object value) {
    // TODO
    return null;
  }

  @Override
  public boolean isHeadless() {
    return headless;
  }

}
