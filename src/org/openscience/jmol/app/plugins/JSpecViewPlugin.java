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

import org.jmol.api.Interface;
import org.jmol.c.CBK;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.JmolPlugin;
import org.openscience.jmol.app.jmolpanel.JmolResourceHandler;
//import org.openscience.jmol.app.jspecview.JSVJmol;

public class JSpecViewPlugin implements JmolPlugin {

  private JFrame parentFrame;
  @SuppressWarnings("unused")
  private Map<String, Object> options;
  private JSVJmolI app;

  @Override
  public void destroy() {
    // TODO

  }

  @Override
  public String getLicense() {
    // TODO
    return null;
  }

  @Override
  public ImageIcon getMenuIcon() {
    return JmolResourceHandler.getIconX("jspecview.png");
  }

  private final static String openJSpecViewScript="|Open...=sync on; sync . \"" + JC.JSV_SYNC_KEYWORD_PREFIX + "\"";
  private final static String  simulate1HSpectrum="|Simulated 1H Spectrum=sync on; sync . \"H1Simulate:\"";
  private final static String  simulate13CSpectrum="|Simulated 13C Spectrum=sync on; sync . \"C13Simulate:\"";

  @Override
  public String getMenuText() {    
    return "JSpecView" 
        + openJSpecViewScript
        + simulate1HSpectrum
        + simulate13CSpectrum;
  }

  @Override
  public String getName() {
    return "JSpecView";
  }

  @Override
  public String getVersion() {
    // TODO
    return null;
  }

  @Override
  public String getWebSite() {
    // TODO
    return null;
  }

  @Override
  public boolean isStarted() {
    return app != null;
  }

  @Override
  public boolean isVisible() {
    return app != null && app.isVisible();
  }

  @Override
  public void notifyCallback(CBK type, Object[] data) {
    if (app != null)
      app.notifyCallback(type, data);
  }

  @Override
  public void setVisible(boolean b) {
    app.setVisible(b);
  }

  @Override
  public void start(JFrame frame, Viewer vwr, Map<String, Object> jmolOptions, boolean headless) {
    parentFrame = frame;
    if (jmolOptions != null) {
      options = jmolOptions;
    }
    app = (JSVJmolI) Interface.getInterface("org.openscience.jmol.app.jspecview.JSVJmol", vwr, "plugin");
    app.setViewer(vwr, parentFrame);
    System.out.println("JSpecViewPlugin started.");
  }


  @Override
  public Object processRequest(String action, Object value) {
    if (app == null)
      return null;
    switch (action) {
    case JC.PLUGIN_REQUEST_PROPERTY:
      return app.getJSpecViewProperty((String) value);
    }
    return null;
  }

  @Override
  public boolean isHeadless() {
    return false;
  }

}
