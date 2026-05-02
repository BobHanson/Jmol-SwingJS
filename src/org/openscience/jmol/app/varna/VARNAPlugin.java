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
import org.jmol.modelset.Atom;
import org.jmol.modelset.Group;
import org.jmol.modelset.Model;
import org.jmol.shape.Balls;
import org.jmol.util.Logger;
import org.jmol.viewer.JC;
import org.jmol.viewer.StatusManager;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.JmolPlugin;

import fr.orsay.lri.varna.applications.VARNAViewer;
import fr.orsay.lri.varna.interfaces.VARNAViewerI;
import fr.orsay.lri.varna.interfaces.VARNAViewerI.VARNACallBack;

import javajs.util.BS;

/**
 * The Jmol VARNA plugin receives standard callbacks from Jmol, selecting ones
 * that are either generally useful or are that are targeting this plugin with a
 * string starting with "varna:".
 * 
 * All communication is with the VARNAViewer class, which is part of the
 * VARNA-SwingJS code. VARNAViewer subclasses VARNA (formerly "VARNAGUI").
 * 
 * The entire operation is designed to work headlessly, optionally with a
 * JFrame, and optionally with a parent JFrame.
 * 
 * The information received from Jmol relating to changes to model, selection,
 * and atom colors is received by VARNAPlugin using the single method in the
 * JmolPlugin interface:
 * 
 * Object notifyCallback(VARNACallBack type, Object[] data)
 * 
 * and is passed on to VARNAViewer usng the VARNAViewerI interface, with the
 * identical method.
 * 
 * Note that this interface allows a return value. For example, an error message
 * from VARNA script execution is passed back to Jmol and displayed to the user
 * using Viewer.showString().
 * 
 * Messages coming from VARNAViewer take the form of an ActionPerformed() call
 * to the VARNAPLugin, which implements ActionListener. This interface also
 * allows for return values. These are simply placed in the source field of the
 * ActionEvent. Thus, communication between VARNAViewer and VARNAPlugin can
 * continue back and forth as many times as necessary, just using setSource()
 * and getSource().
 * 
 * VARNAPlugin can access all public components of Jmol, mustly through Viewer,
 * which it holds a reference to. Likewise, VARNAViewer can access all of VARNA,
 * primarily through its reference to VARNAapp. (VARNAapp is a consolidation of
 * fields and methods that formerly were duplicated in VARNAGUI, VARNAEditor,
 * VARNAcmd, among other applications.)
 * 
 * @author Bob Hanson
 */
public class VARNAPlugin implements JmolPlugin, ActionListener {

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
    System.out.println("VARNAPlugin started.");
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
    return VARNAViewerI.version;
  }

  @Override
  public String getLicense() {
    return VARNAViewerI.license;
  }

  @Override
  public void setVisible(boolean b) {
    showFrame(b);
    if (b)
      checkDSSR(null);
  }

  private void showFrame(boolean b) {
    if (b && varna == null) {
      varna = (VARNAViewerI) javajs.api.Interface
          .getInterface("fr.orsay.lri.varna.applications.VARNAViewer");
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
    case ANIMFRAME:
    case CALCULATION:
    case HOVER:
    case LOADSTRUCT:
    case SELECT:
    case STRUCTUREMODIFIED:
    case SYNC:
      break;
    default:
      return;
    }

    switch (type) {
    default:
      return;
    case ANIMFRAME:
      System.out.println("VARNAPlugin Jmol >> " + type + " " + data[2]);
      String modelName = (String) data[2];
      int modelCount = ((int[]) data[1])[1];
      if (varna != null && modelCount == 0) {
        // only zap VARNA if ZAP, or LOAD without APPEND
        varna.notifyCallback(VARNACallBack.ZAP, data);
      } else {
        checkDSSR(modelName);
      }
      return;
    case CALCULATION:
      //      if (JC.INFO_DSSR.equals(data[2]))
      System.out.println("VARNAPlugin Jmol >> " + type);
      checkDSSR(null);
      return;
    case HOVER:
      System.out.println("VARNAPlugin Jmol >> " + type);
      if (data[1] != null) {
        // hover ON
        Atom a = vwr.ms.at[((Integer)data[2]).intValue()];
        int resno = a.group.getResno();
        int modelID = vwr.getModelFileNumber(a.getModelIndex());
        data[0] = new int[] { modelID, resno };
      }
      varna.notifyCallback(VARNACallBack.HOVER, data);
      return;
    case LOADSTRUCT:
      System.out.println("VARNAPlugin Jmol >> " + type + " " + data[2]);
      return;
    case SELECT:
      System.out.println("VARNAPlugin Jmol >> " + type + " " + data[1]);
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
    case STRUCTUREMODIFIED:
      if (((Integer) data[1])
          .intValue() != StatusManager.NOTIFY_MOD_ATOM_COLORED)
        return;
      bsAtoms = (BS) data[6];
      System.out.println("VARNAPlugin Jmol >> " + type + " " + data[6]);
      if (bsAtoms.isEmpty())
        return;
      Object d1 = data[1];
      data[1] = new Object[] { getModelGroupMap(bsAtoms, VARNACallBack.COLOR),
          colors };
      varna.notifyCallback(VARNACallBack.COLOR, data);
      data[1] = d1;
      break;
    case SYNC:
      System.out.println("VARNAPlugin Jmol >> " + type + " " + data[1]);
      cmd = (String) data[1];
      if (!cmd.startsWith("varna:"))
        break;
      switch (cmd.substring(6).toLowerCase()) {
      case "stop":
        destroy();
        break;
      case "start":
        break;
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
    System.out.println("VARNAPlugin VARNA >> " + e.getActionCommand());
    switch (e.getActionCommand()) {
    case VARNAViewerI.ACTION_HOVER:
      int[] modelResno = (int[]) e.getSource();
      Group g = null;
      if (modelResno != null) {
        g = vwr.ms.getGroupForResno(modelResno[0], modelResno[1]);
      }
      vwr.hoverOnPtr((g == null ? -1 : g.getLeadOrFirstAtomIndex()), false, true);
      break;
    case VARNAViewerI.ACTION_SELECT_MODEL:
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
    case VARNAViewerI.ACTION_SELECT_BASES:
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
