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
package org.gennbo;

import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.jmol.c.CBK;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.JmolPlugin;
import org.openscience.jmol.app.jmolpanel.JmolPanel;

public class NBOPlugin implements JmolPlugin {

  protected NBODialog nboDialog;
  protected Viewer vwr;
  
  public final static String version = "0.1.2b";


  @Override
  public boolean isStarted() {
    return vwr != null;
  }


  @Override
  public void start(JFrame frame, Viewer vwr, Map<String, Object> jmolOptions) {
    this.vwr = vwr;
    if (getNBOProperty("serverPath", null) == null) {
        vwr.alert("NBOServe has not been installed. See " + NBOConfig.NBO_WEB_SITE + "/new6_css.htm for additional information");
    }
    nboDialog = new NBODialog(this, frame, vwr, jmolOptions);
    System.out.println("NBO Plugin started.");    
  }

  @Override
  public String getWebSite() {
    return NBOConfig.NBO_WEB_SITE;
  }
  @Override
  public String getName() {
    return "NBOPro@Jmol";
  }
  
  @Override
  public ImageIcon getMenuIcon() {
    return getIcon("nbo7logo20x20");
  }

  @Override
  public String getMenuText() {
    return getName();
  }

  ImageIcon getIcon(String name) {
    return new ImageIcon(this.getClass().getResource("assets/" + name + ".gif"));
  }

  @Override
  public String getVersion() {
    return version;
  }
 
  @Override
  public void setVisible(boolean b) {
    if (nboDialog == null)
      return;
    nboDialog.setVisible(b);
  }

  @Override
  public void destroy() {
    if (nboDialog == null)
      return;
    nboDialog.close();
    nboDialog = null;
  }

  @Override
  public void notifyCallback(CBK type, Object[] data) {
    if (nboDialog == null)
      return;
    nboDialog.notifyCallback(type, data);
  }

  /**
   * Get an NBO property from Jmol's plugin resources.
   * 
   * @param name
   * @param defaultValue
   * @return the property string or the default value if the key was not found
   * 
   */
  protected String getNBOProperty(String name, String defaultValue) {
    return JmolPanel.getPluginOption("NBO", name, defaultValue);
  }

  /**
   * Set an NBO property in Jmol's plugin property file.
   * 
   * @param name
   * @param option
   */
  protected void setNBOProperty(String name, String option) {
    if (option == null)
      return;
    JmolPanel.setPluginOption("NBO", name, option.replace('\\', '/'));
  }

}
