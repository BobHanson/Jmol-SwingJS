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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.jmol.c.CBK;
import org.jmol.modelset.Group;
import org.jmol.modelset.Model;
import org.jmol.shape.Balls;
import org.jmol.util.Logger;
import org.jmol.viewer.JC;
import org.jmol.viewer.StatusManager;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.JmolPlugin;

import fr.orsay.lri.varna.applications.VARNAviewer;
import fr.orsay.lri.varna.interfaces.VARNAviewerI;
import fr.orsay.lri.varna.interfaces.VARNAviewerI.VARNACallBack;
import javajs.util.BS;

public class VARNAPlugin implements JmolPlugin, ActionListener {

  public final static String version = "0.0.1";

  private static final String MY_SCRIPT_ID = JC.SCRIPT_EXT + "FROM_VARNA";

  protected Viewer vwr;

  private VARNAviewerI varna;

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
      varna = (VARNAviewer) javajs.api.Interface
          .getInterface("fr.orsay.lri.varna.applications.VARNAviewer");
      return;
    }
    if (varna == null)
      return;
    varnaFrame = (JFrame) varna.notifyCallback(VARNACallBack.GETFRAME, null);
    if (varnaFrame == null) {
      // too early in the process
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
    BS bsAtoms;
    String cmd;
    switch (type) {
    default:
      //      System.out.println("Varna plugin callback for " + type + data);
      return;
    case STRUCTUREMODIFIED:
      if (((Integer) data[1])
          .intValue() != StatusManager.NOTIFY_MOD_ATOM_COLORED)
        return;
      bsAtoms = (BS) data[6];
      if (bsAtoms.isEmpty())
        return;
      Object d1 = data[1];
      data[1] = new Object[] { getModelGroupMap(bsAtoms, VARNACallBack.COLOR),
          colors };
      varna.notifyCallback(VARNACallBack.COLOR, data);
      data[1] = d1;
      return;
    case SERVICE:
      return;
    case CALCULATION:
      //      if (JC.INFO_DSSR.equals(data[2]))
      checkDSSR(null);
      return;
    case LOADSTRUCT:
      if (varna != null && data[7].equals(data[6])) {
        // only zap VARNA if ZAP, or LOAD without APPEND
        varna.notifyCallback(VARNACallBack.ZAP, data);
      }
      return;
    case SCRIPT:
      //System.out.println("Varna plugin SCRIPT " + data[1] + " " + data[2]);
      return;
    case SYNC:
      cmd = (String) data[1];
      if (!cmd.startsWith("varna:"))
        break;
      switch (cmd.substring(6).toLowerCase()) {
      case "stop":
        destroy();
        break;
      // case "start":
      // is handled by JmolPanel
      case "on":
      case "show":
        if (varnaFrame != null)
          varnaFrame.setVisible(true);
        break;
      case "off":
      case "hide":
        if (varnaFrame != null)
          varnaFrame.setVisible(false);
        break;
      default:
        String err = (String) varna.notifyCallback(VARNACallBack.SCRIPT, data);
        data[0] = err;
        break;
      }      
      return;
    case ANIMFRAME:
      String modelName = (String) data[2];
      checkDSSR(modelName);
      return;
    case SELECT:
      bsAtoms = (BS) data[1];
      String selectColors = (String) data[5];
      if (selectColors == null ? mySelectColors != null
          : !selectColors.equals(mySelectColors)) {
        mySelectColors = selectColors;
        data[1] = "varna:setSelectionColors(" + selectColors + ")";
        varna.notifyCallback(VARNACallBack.SCRIPT, data);
      }
      // limit the atoms to the current frame
      data[1] = getModelGroupMap(bsAtoms, VARNACallBack.SELECT);
      varna.notifyCallback(VARNACallBack.SELECT, data);
      data[1] = bsAtoms;
      return;
    }
  }

  private List<Color> colors;
  private List<Integer> colorRGBs;

  private Map<Integer, Map<String, List<Integer>>> getModelGroupMap(BS bsAtoms,
                                                                    VARNACallBack cbk) {
    if (bsAtoms.cardinality() == 0)
      return null;
    boolean setColors = (cbk.equals(VARNACallBack.COLOR));
    if (setColors && colors == null) {
      colors = new ArrayList<Color>();
      colorRGBs = new ArrayList<Integer>();
    }
    short[] colixes = (setColors
        ? ((Balls) vwr.shm.getShape(JC.SHAPE_BALLS)).colixes
        : null);
    List<Group> groups = vwr.ms.getGroupsForAtoms(bsAtoms);
    Map<Integer, Map<String, List<Integer>>> modelMap = new HashMap<>();
    int ngroups = groups.size();
    Map<String, List<Integer>> modelData = null;
    int lastRGB = 1;
    List<Integer> resnos = null;
    List<Integer> colorIndexes = null;
    int nColors = (setColors ? colors.size() : 0);
    for (int i = 0, modelID = -1; i < ngroups; i++) {
      Group g = groups.get(i);
      Model m = g.getModel();
      int id = vwr.getModelFileNumber(m.modelIndex);
      if (id != modelID) {
        modelID = id;
        modelData = new HashMap<>();
        modelMap.put(Integer.valueOf(modelID), modelData);
        resnos = new ArrayList<Integer>();
        modelData.put("resnos", resnos);
        if (setColors) {
          colorIndexes = new ArrayList<Integer>();
          modelData.put("colorIndexes", colorIndexes);
        }
        lastRGB = 1;
      }
      if (setColors) {
        Integer ci;
        int rgb = vwr.shm.getAtomColorRGBShaded(colixes, vwr.ms.at[g.getLeadOrFirstAtomIndex()]);
        if (rgb == lastRGB) {
          colorIndexes.add(null);
        } else {
          lastRGB = rgb;
          Integer rgbI = Integer.valueOf(rgb);
          int ip = colorRGBs.indexOf(rgbI);
          if (ip >= 0) {
            ci = Integer.valueOf(ip);
          } else {
            colors.add(new Color(rgb));
            colorRGBs.add(rgbI);
            ci = Integer.valueOf(nColors++);
          }
          colorIndexes.add(ci);
        }
      }
      resnos.add(Integer.valueOf(g.getResno()));
    }
    return modelMap;
  }

  /**
   * null modelName for current model
   * 
   * @param modelName
   */
  @SuppressWarnings("unchecked")
  private void checkDSSR(String modelName) {
    Map<String, Object> info = vwr.getCurrentModelAuxInfo();
    int modelID = vwr.getModelFileNumber(vwr.am.cmi);
    Map<String, Object> dssrInfo = (info == null ? null
        : (Map<String, Object>) info.get(JC.INFO_DSSR));
    if (dssrInfo == null || !checkFrame())
      return;
    String m = (modelName == null
        ? info.get("modelNumberDotted") + ": " + info.get("modelName")
        : modelName);
    // if this is invoked later, it fouls up scripting after a file load
    varna.notifyCallback(VARNACallBack.SETDSSR,
        new Object[] { m, Integer.valueOf(modelID), dssrInfo });
    SwingUtilities.invokeLater(() -> {
      showFrame(true);
    });
  }

  private boolean checkFrame() {
    if (varna == null)
      return false;
    if (varnaFrame == null) {
      varnaFrame = (JFrame) varna.notifyCallback(VARNACallBack.SETPLUGIN,
          new Object[] { options.get("parentFrame"), this });
    }
    return true;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String jmolScript = null;
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
      jmolScript = "model " + name + MY_SCRIPT_ID;
      break;
    case "selectBases":
      if (e.getSource() instanceof int[]) {
        int[] resnos = (int[]) e.getSource();
        if (resnos.length == 0) {
          jmolScript = "select none";
        } else {
          jmolScript = "select model=" + vwr.getModelNumberDotted(vwr.am.cmi)
              + " and resno=" + Arrays.toString(resnos);
        }
      } else {
        // this is a request for information from CLEAR or set.
        jmolScript = (String) e.getSource();
        e.setSource(vwr.slm.getSelectionColors());
      }
      break;
    }
    if (jmolScript != null) {
      if (Logger.debugging)
        System.out.println(jmolScript);
      script(jmolScript);
    }

  }

  private void script(String script) {
    vwr.evalStringQuiet(script);
  }

}
