/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-12-13 22:43:17 -0600 (Sat, 13 Dec 2014) $
 * $Revision: 20162 $
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.gennbo.NBOConfig;
import org.jmol.api.Interface;
import org.jmol.c.CBK;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.JmolPlugin;
import org.openscience.jmol.app.jmolpanel.JmolPanel;
import org.openscience.jmol.app.jmolpanel.JmolResourceHandler;

public class NBOPlugin implements JmolPlugin, ActionListener {

  protected NBOInterface nbo;
  protected Viewer vwr;
  
  public final static String version = "NBO plugin 0.1.2b";
  public final static String license = "NBO(FORTRAN) is licensed independently of this plugin, through the University of Minnesota";

  @Override
  public boolean isStarted() {
    return vwr != null;
  }

  @Override
  public void start(JFrame frame, Viewer vwr, Map<String, Object> jmolOptions, boolean headless) {
    this.vwr = vwr;
    if (getNBOProperty("serverPath", null) == null) {
        vwr.alert("NBOServe has not been installed. See " + getWebSite() + "/new6_css.htm for additional information");
    }
    nbo = (NBOInterface) Interface.getInterface("org.gennbo.NBO", vwr, "plugin");
    nbo.newDialog(this, frame, vwr, jmolOptions);
    System.out.println("NBO Plugin started.");    
    setVisible(true);
  }

  @Override
  public String getWebSite() {
    return NBOInterface.NBO_WEB_SITE;
  }
  
  @Override
  public String getName() {
    return "NBOPro@Jmol";
  }
  
  @Override
  public ImageIcon getMenuIcon() {
    return JmolResourceHandler.getIconX("nbo7logo20x20.gif");
  }

  @Override
  public String getMenuText() {
    return getName();
  }

  public ImageIcon getIcon(String name) {
    return nbo.getIcon(name);
  }

  @Override
  public String getVersion() {
    return version;
  }
 
  @Override
  public String getLicense() {
    return license;
  }
 
  @Override
  public void setVisible(boolean b) {
    if (nbo == null)
      return;
    nbo.setVisible(b);
  }
  
  @Override
  public boolean isVisible() {
    return nbo != null && nbo.isVisible();
  }

  @Override
  public void destroy() {
    if (nbo == null)
      return;
    nbo.close();
    nbo = null;
  }

  @Override
  public void notifyCallback(CBK type, Object[] data) {
    if (nbo == null)
      return;
    nbo.notifyCallback(type, data);
  }

  /**
   * Get an NBO property from Jmol's plugin resources.
   * 
   * @param name
   * @param defaultValue
   * @return the property string or the default value if the key was not found
   * 
   */
  public String getNBOProperty(String name, String defaultValue) {
    return JmolPanel.getPluginOption("NBO", name, defaultValue);
  }

  /**
   * Set an NBO property in Jmol's plugin property file.
   * 
   * @param name
   * @param option
   */
  public void setNBOProperty(String name, String option) {
    if (option == null)
      return;
    JmolPanel.setPluginOption("NBO", name, option.replace('\\', '/'));
  }

  @Override
  public Object processRequest(String action, Object value) {
    return null;
  }

  @Override
  public boolean isHeadless() {
    return false;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();
    String[] data;
    switch (e.getActionCommand()) {
    case "getProperty":
      data = (String[]) o;
      e.setSource(getNBOProperty(data[0], data[1]));
      break;
    case "setProperty":
      data = (String[]) o;
      setNBOProperty(data[0], data[1]);
      break;
    case "version":
      e.setSource(getVersion());
      break;
    case "name":
      e.setSource(getName());
      break;
    }
  }


}
