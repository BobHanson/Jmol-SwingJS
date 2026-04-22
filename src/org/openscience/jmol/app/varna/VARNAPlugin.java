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
package org.openscience.jmol.app.varna;

import java.awt.event.ActionEvent;

import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.jmol.c.CBK;
import org.jmol.util.Logger;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.JmolPlugin;

import fr.orsay.lri.varna.applications.VARNAViewer;
import fr.orsay.lri.varna.interfaces.VARNAViewerI;
import fr.orsay.lri.varna.interfaces.VARNAViewerI.VARNACallBack;
import javajs.util.BS;
import swingjs.api.Interface;

public class VARNAPlugin implements JmolPlugin, ActionListener {

  public final static String version = "0.0.1";

  private static final String MY_SCRIPT_ID = JC.SCRIPT_EXT + "FROM_VARNA";

  protected Viewer vwr;

  private VARNAViewerI varna;

  private Map<String, Object> options;

  private JFrame varnaFrame;


  @Override
  public boolean isStarted() {
    return vwr != null;
  }


  @Override
  public void start(JFrame frame, Viewer vwr, Map<String, Object> jmolOptions) {
    this.vwr = vwr;
    if (jmolOptions != null)
      this.options = jmolOptions;
    System.out.println("VARNA Plugin started.");    
  }

  @Override
  public String getWebSite() {
    return null;
  }
  @Override
  public String getName() {
    return "VARNA";
  }
  
  @Override
  public ImageIcon getMenuIcon() {
    return null;//getIcon("nbo7logo20x20");
  }

  @Override
  public String getMenuText() {
    return getName();
  }

  /**
   * @param name  
   * @return icon
   */
  ImageIcon getIcon(String name) {
    return null;
//    return new ImageIcon(this.getClass().getResource("assets/" + name + ".gif"));
  }

  @Override
  public String getVersion() {
    return version;
  }
 
  @Override
  public void setVisible(boolean b) {
    showFrame(b);
    if (b)
      checkDSSR(null);
  }

  private void showFrame(boolean b) {
    if (b && varna == null) {
      varna = (VARNAViewer) Interface
          .getInstance("fr.orsay.lri.varna.applications.VARNAViewer", true);
      return;
    }
    if (varna == null)
      return;
    varnaFrame = (JFrame) varna.notifyCallback(VARNACallBack.GETFRAME, null);
    if (varnaFrame == null) {
      // too early in the process
      System.out.println("VP???");
    } else {
      varnaFrame.setVisible(b);
      if (b)
        varnaFrame.toFront();
    }
  }


  @Override
  public void destroy() {
    if (varna == null)
      return;
    vwr = null;
    varna.notifyCallback(VARNACallBack.DESTROY, null);
    varna = null;
    varnaFrame = null;
  }

  private String mySelectColors;
  @Override
  public void notifyCallback(CBK type, Object[] data) {
    // don't do anything if multiple frames are visible
    if (vwr == null || vwr.am.cmi < 0)
      return;
    String cmd;
    switch (type) {
    default:  
//      System.out.println("Varna plugin callback for " + type + data);
      return;
    case CALCULATION:
//      if ("DSSR".equals(data[2]))
        checkDSSR(null);
      return;
    case LOADSTRUCT:
      if (varna != null && "zapped".equals(data[2]))
        varna.notifyCallback(VARNACallBack.ZAP, data);
      return;
    case SCRIPT:
      //System.out.println("Varna plugin SCRIPT " + data[1] + " " + data[2]);
      return;
    case SYNC:
      cmd = (String) data[1];
      if (!cmd.startsWith("varna:"))
        return;
      if (cmd.equalsIgnoreCase("varna:stop")) {
        destroy();
        return;
      }
      varna.notifyCallback(VARNACallBack.SCRIPT, data);
      return;
    case ANIMFRAME:
      String modelName = (String) data[2];
      System.out.println("VP ANIMFRAME " + data[2]);
      checkDSSR(modelName);
      return;      
    case SELECT:
      BS atoms = (BS) data[1];
      String selectColors = (String) data[5];
      if (selectColors == null ? mySelectColors != null : !selectColors.equals(mySelectColors)) {
        mySelectColors = selectColors;
        data[1] = "varna:setSelectionColors(" + selectColors + ")";
        varna.notifyCallback(VARNACallBack.SCRIPT, data);
      }
      // limit the atoms to the current frame
      atoms.and(vwr.getFrameAtoms());
      HashSet<Integer> set = new HashSet<Integer>();
      for (int i = atoms.nextSetBit(0); i >= 0; i = atoms.nextSetBit(i + 1)) {
        set.add(Integer.valueOf(vwr.ms.at[i].getResno()));
      }
      data[1] = set;
      varna.notifyCallback(VARNACallBack.SELECT, data);
      return;
    }
  }

  /**
   * null modelName for current model
   * @param modelName
   */
  @SuppressWarnings("unchecked")
  private void checkDSSR(String modelName) {
    Map<String, Object> info = vwr.getCurrentModelAuxInfo();
    Map<String, Object> dssrInfo = (info == null ? null : (Map<String, Object>) info.get(JC.INFO_DSSR));
    if (dssrInfo == null || !checkFrame())
      return;
    String m = (modelName == null ? info.get("modelNumberDotted") + ": " + info.get("modelName") : modelName);  
    SwingUtilities.invokeLater(()->{
      varna.notifyCallback(VARNACallBack.SETDSSR, new Object[] { m, dssrInfo });
      showFrame(true);
    });
  }


  private boolean checkFrame() {
    if (varna == null) 
      return false;
    if (varnaFrame == null) {
      varnaFrame = (JFrame) varna.notifyCallback(VARNACallBack.SETPLUGIN, new Object [] { options.get("parentFrame"), this });
    }
    return true;
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    String script = null;
    switch (e.getActionCommand()) {
    case "selectModel":
      // Model 1.1: 1EHZ
      String name = (String) e.getSource();
      int pt = name.indexOf(":");
      if (pt <= 6)
        return;
      name = name.substring(6, pt);
      if (name.equals(vwr.getModelNumberDotted(vwr.am.cmi))) {
        return;
      }
      script = "model " + name + MY_SCRIPT_ID;
      break;
    case "selectBases":
      if (e.getSource() instanceof int[]) {
        int[] resnos = (int[]) e.getSource();
        if (resnos.length == 0) {
          script = "select none";
        } else {
          script = "select model=" + vwr.getModelNumberDotted(vwr.am.cmi)
              + " and resno=" + Arrays.toString(resnos);
        }
      } else {
        // this is a request for information from CLEAR.
        e.setSource(vwr.slm.getSelectionColors());
        script = "select none";
      }
      break;
    }
    if (script != null) {
      if (Logger.debugging)
        System.out.println(script);
      vwr.script(script);
    }

  }


}
