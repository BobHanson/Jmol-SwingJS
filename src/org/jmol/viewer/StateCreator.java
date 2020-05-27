/* $Author: hansonr $
 * $Date: 2010-04-22 13:16:44 -0500 (Thu, 22 Apr 2010) $
 * $Revision: 12904 $
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

package org.jmol.viewer;

import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.JmolDataManager;
import org.jmol.api.JmolModulationSet;
import org.jmol.api.JmolScriptFunction;
import org.jmol.api.SymmetryInterface;
import org.jmol.c.PAL;
import org.jmol.c.STR;
import org.jmol.c.VDW;
import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.Bond;
import org.jmol.modelset.BondSet;
import org.jmol.modelset.Measurement;
import org.jmol.modelset.Model;
import org.jmol.modelset.ModelSet;
import org.jmol.modelset.StateScript;
import org.jmol.modelset.Text;
import org.jmol.modelset.TickInfo;
import org.jmol.script.SV;
import org.jmol.script.T;
import org.jmol.shape.AtomShape;
import org.jmol.shape.Axes;
import org.jmol.shape.Balls;
import org.jmol.shape.Echo;
import org.jmol.shape.FontLineShape;
import org.jmol.shape.Frank;
import org.jmol.shape.Halos;
import org.jmol.shape.Hover;
import org.jmol.shape.Labels;
import org.jmol.shape.Measures;
import org.jmol.shape.Shape;
import org.jmol.shape.Sticks;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.ColorEncoder;
import org.jmol.util.Edge;
import org.jmol.util.Escape;
import org.jmol.util.Font;
import org.jmol.util.GData;
import org.jmol.util.Logger;
import org.jmol.util.Vibration;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.SB;

/**
 * StateCreator handles all aspects of working with the "state" as
 * generally defined, including
 * 
 *  -- creating the state script
 *  
 *  -- general output, including logging
 *  
 *  -- handling undo/redo
 *  
 *  -- processing SYNC directives
 *  
 * 
 * Called by reflection only; all state generation script here, for
 * modularization in JavaScript
 * 
 * 
 * 
 */
public class StateCreator extends JmolStateCreator {

  public StateCreator() {

    // by reflection only!

  }

  private Viewer vwr;

  @Override
  void setViewer(Viewer vwr) {
    this.vwr = vwr;
  }


  /////////////////// creating the state script ////////////////////

  @Override
  String getStateScript(String type, int width, int height) {
    //System.out.println("vwr getStateInfo " + type);
    boolean isAll = (type == null || type.equalsIgnoreCase("all"));
    SB s = new SB();
    SB sfunc = (isAll ? new SB().append("function _setState() {\n") : null);
    if (isAll) {
      s.append(JC.STATE_VERSION_STAMP + Viewer.getJmolVersion() + ";\n");
      if (vwr.isApplet) {
        app(s, "# fullName = " + PT.esc(vwr.fullName));
        app(s, "# documentBase = " + PT.esc(Viewer.appletDocumentBase));
        app(s, "# codeBase = " + PT.esc(Viewer.appletCodeBase));
        s.append("\n");
      }
    }

    GlobalSettings global = vwr.g;
    // window state
    if (isAll || type.equalsIgnoreCase("windowState"))
      s.append(getWindowState(sfunc, width, height));
    //if (isAll)
    //s.append(getFunctionCalls(null)); // removed in 12.1.16; unnecessary in state
    // file state
    if (isAll || type.equalsIgnoreCase("fileState"))
      s.append(getFileState(sfunc));
    // all state scripts (definitions, dataFrames, calculations, configurations,
    // rebonding
    if (isAll || type.equalsIgnoreCase("definedState"))
      s.append(getDefinedState(sfunc, true));
    // numerical values
    if (isAll || type.equalsIgnoreCase("variableState"))
      s.append(getParameterState(global, sfunc)); // removed in 12.1.16; unnecessary in state // ARGH!!!
    if (isAll || type.equalsIgnoreCase("dataState"))
      s.append(getDataState(sfunc));
    // connections, atoms, bonds, labels, echos, shapes
    if (isAll || type.equalsIgnoreCase("modelState"))
      s.append(getModelState(sfunc, true,
          vwr.getBooleanProperty("saveProteinStructureState")));
    // color scheme
    if (isAll || type.equalsIgnoreCase("colorState"))
      s.append(getColorState(vwr.cm, sfunc));
    // frame information
    if (isAll || type.equalsIgnoreCase("frameState"))
      s.append(getAnimState(vwr.am, sfunc));
    // orientation and slabbing
    if (isAll || type.equalsIgnoreCase("perspectiveState"))
      s.append(getViewState(vwr.tm, sfunc));
    // display and selections
    if (isAll || type.equalsIgnoreCase("selectionState"))
      s.append(getSelectionState(vwr.slm, sfunc));
    if (sfunc != null) {
      app(sfunc, "set refreshing true");
      app(sfunc, "set antialiasDisplay " + global.antialiasDisplay);
      app(sfunc, "set antialiasTranslucent " + global.antialiasTranslucent);
      app(sfunc, "set antialiasImages " + global.antialiasImages);
      if (vwr.tm.spinOn)
        app(sfunc, "spin on");
      sfunc.append("}\n\n_setState;\n");
    }
    if (isAll)
      s.appendSB(sfunc);
    return s.toString();
  }

  private String getDataState(SB sfunc) {
    SB commands = new SB();
    boolean haveData = false;
    String atomProps = getAtomicPropertyState(-1, null);
    if (atomProps.length() > 0) {
      haveData = true;
      commands.append(atomProps);
    }
    if (vwr.userVdws != null) {
      String info = vwr.getDefaultVdwNameOrData(0, VDW.USER,
          vwr.bsUserVdws);
      if (info.length() > 0) {
        haveData = true;
        commands.append(info);
      }
    }    
    if (vwr.nmrCalculation != null)
      haveData |= vwr.nmrCalculation.getState(commands);
    if (vwr.dm != null)
      haveData |= vwr.dm.getDataState(this, commands);
    if (!haveData)
      return "";
    
    String cmd = "";
    if (sfunc != null) {
      sfunc.append("  _setDataState;\n");
      cmd = "function _setDataState() {\n";
      commands.append("}\n\n");
    }
    return cmd + commands.toString();
  }

  private String getDefinedState(SB sfunc, boolean isAll) {
    ModelSet ms = vwr.ms;
    int len = ms.stateScripts.size();
    if (len == 0)
      return "";

    boolean haveDefs = false;
    SB commands = new SB();
    String cmd;
    for (int i = 0; i < len; i++) {
      StateScript ss = ms.stateScripts.get(i);
      if (ss.inDefinedStateBlock && (cmd = ss.toString()).length() > 0) {
        app(commands, cmd);
        haveDefs = true;
      }
    }
    if (!haveDefs)
      return "";
    cmd = "";
    if (isAll && sfunc != null) {
      sfunc.append("  _setDefinedState;\n");
      cmd = "function _setDefinedState() {\n\n";
    }
    if (sfunc != null)
      commands.append("\n}\n\n");
    return cmd + commands.toString();
  }

  @Override
  String getModelState(SB sfunc, boolean isAll, boolean withProteinStructure) {
    SB commands = new SB();
    if (isAll && sfunc != null) {
      sfunc.append("  _setModelState;\n");
      commands.append("function _setModelState() {\n");
    }
    String cmd;

    // connections

    ModelSet ms = vwr.ms;
    Bond[] bonds = ms.bo;
    Model[] models = ms.am;
    int modelCount = ms.mc;

    if (isAll) {

      int len = ms.stateScripts.size();
      for (int i = 0; i < len; i++) {
        StateScript ss = ms.stateScripts.get(i);
        if (!ss.inDefinedStateBlock && (cmd = ss.toString()).length() > 0) {
          app(commands, cmd);
        }
      }

      SB sb = new SB();
      for (int i = 0; i < ms.bondCount; i++)
        if (!models[bonds[i].atom1.mi].isModelKit)
          if (bonds[i].isHydrogen() || (bonds[i].order & Edge.BOND_NEW) != 0) {
            Bond bond = bonds[i];
            int index = bond.atom1.i;
            if (bond.atom1.group.isAdded(index))
              index = -1 - index;
            sb.appendI(index).appendC('\t').appendI(bond.atom2.i).appendC('\t')
                .appendI(bond.order & ~Edge.BOND_NEW).appendC('\t')
                .appendF(bond.mad / 1000f).appendC('\t')
                .appendF(bond.getEnergy()).appendC('\t')
                .append(Edge.getBondOrderNameFromOrder(bond.order))
                .append(";\n");
          }
      if (sb.length() > 0)
        commands.append("data \"connect_atoms\"\n").appendSB(sb)
            .append("end \"connect_atoms\";\n");
      commands.append("\n");
    }

    // bond visibility

    if (ms.haveHiddenBonds) {
      BondSet bs = new BondSet();
      for (int i = ms.bondCount; --i >= 0;)
        if (bonds[i].mad != 0
            && (bonds[i].shapeVisibilityFlags & Bond.myVisibilityFlag) == 0)
          bs.set(i);
      if (bs.isEmpty())
        ms.haveHiddenBonds = false;
      else
        commands.append("  hide ").append(Escape.eBond(bs)).append(";\n");
    }

    // shape construction

    vwr.setModelVisibility();

    // unnecessary. Removed in 11.5.35 -- oops!

    if (withProteinStructure)
      commands.append(ms
          .getProteinStructureState(null, isAll ? T.all : T.state));

    // introduced in 14.4.2
    for (int i = 0; i < modelCount; i++)
      if (models[i].mat4 != null)
        commands.append("  frame orientation " + ms.getModelNumberDotted(i)
            + Escape.matrixToScript(models[i].mat4) + ";\n");

    getShapeStatePriv(commands, isAll, Integer.MAX_VALUE);

    if (isAll) {
      boolean needOrientations = false;
      for (int i = 0; i < modelCount; i++)
        if (models[i].isJmolDataFrame) {
          needOrientations = true;
          break;
        }
      SB sb = new SB();
      for (int i = 0; i < modelCount; i++) {
        Model m = models[i];
        sb.setLength(0);
        String s = (String) ms.getInfo(i, "modelID");
        if (s != null && !s.equals(ms.getInfo(i, "modelID0")))
          sb.append("  frame ID ").append(PT.esc(s)).append(";\n");
        String t = ms.frameTitles[i];
        if (t != null && t.length() > 0)
          sb.append("  frame title ").append(PT.esc(t)).append(";\n");
        if (needOrientations && m.orientation != null
            && !ms.isTrajectorySubFrame(i))
          sb.append("  ").append(m.orientation.getMoveToText(false))
              .append(";\n");
        if (m.frameDelay != 0 && !ms.isTrajectorySubFrame(i))
          sb.append("  frame delay ").appendF(m.frameDelay / 1000f)
              .append(";\n");
        if (m.simpleCage != null) {
          sb.append("  unitcell ")
              .append(Escape.eAP(m.simpleCage.getUnitCellVectors()))
              .append(";\n");
          getShapeStatePriv(sb, isAll, JC.SHAPE_UCCAGE);
        }
        if (sb.length() > 0)
          commands.append("  frame " + ms.getModelNumberDotted(i) + ";\n")
              .appendSB(sb);
      }

      boolean loadUC = false;
      if (ms.unitCells != null) {
        boolean haveModulation = false;
        for (int i = 0; i < modelCount; i++) {
          SymmetryInterface symmetry = ms.getUnitCell(i);
          if (symmetry == null)
            continue;
          sb.setLength(0);
          if (symmetry.getState(sb)) {
            loadUC = true;
            commands.append("  frame ").append(ms.getModelNumberDotted(i))
                .appendSB(sb).append(";\n");
          }
          haveModulation |= (vwr.ms.getLastVibrationVector(i, T.modulation) >= 0);
        }
        if (loadUC)
          vwr.shm.loadShape(JC.SHAPE_UCCAGE); // just in case
        getShapeStatePriv(commands, isAll, JC.SHAPE_UCCAGE);
        if (haveModulation) {
          Map<String, BS> temp = new Hashtable<String, BS>();
          int ivib;
          for (int i = modelCount; --i >= 0;) {
            if ((ivib = vwr.ms.getLastVibrationVector(i, T.modulation)) >= 0)
              for (int j = models[i].firstAtomIndex; j <= ivib; j++) {
                JmolModulationSet mset = ms.getModulation(j);
                if (mset != null)
                  BSUtil.setMapBitSet(temp, j, j, mset.getState());
              }
          }
          commands.append(getCommands(temp, null, "select"));
        }
      }
      commands.append("  set fontScaling " + vwr.getBoolean(T.fontscaling)
          + ";\n");
      //      if (vwr.getBoolean(T.modelkitmode))
      //      commands.append("  set modelKitMode true;\n");
    }
    if (sfunc != null)
      commands.append("\n}\n\n");
    return commands.toString();
  }

  private String getWindowState(SB sfunc, int width, int height) {
    GlobalSettings global = vwr.g;
    SB str = new SB();
    if (sfunc != null) {
      sfunc
          .append("  initialize;\n  set refreshing false;\n  _setWindowState;\n");
      str.append("\nfunction _setWindowState() {\n");
    }
    if (width != 0)
      str.append("# preferredWidthHeight ").appendI(width).append(" ").appendI(
          height).append(";\n");
    str.append("# width ")
        .appendI(width == 0 ? vwr.getScreenWidth() : width).append(
            ";\n# height ").appendI(
            height == 0 ? vwr.getScreenHeight() : height).append(";\n");
    app(str, "stateVersion = " + JC.versionInt);
    app(str, "background " + Escape.escapeColor(global.objColors[0]));
    for (int i = 1; i < StateManager.OBJ_MAX; i++)
      if (global.objColors[i] != 0)
        app(str, StateManager.getObjectNameFromId(i) + "Color = \""
            + Escape.escapeColor(global.objColors[i]) + '"');
    if (global.backgroundImageFileName != null) {
      app(str, "background IMAGE "
          + (global.backgroundImageFileName.startsWith(";base64,") ? "" : "/*file*/")
          + PT.esc(global.backgroundImageFileName));
    }
    str.append(getLightingState(false));
    //app(str, "statusReporting  = " + global.statusReporting);
    if (sfunc != null)
      str.append("}\n\n");
    return str.toString();
  }

  @Override
  String getLightingState(boolean isAll) {
    SB str = new SB();
    GData g = vwr.gdata;
    app(str, "set ambientPercent " + g.getAmbientPercent());
    app(str, "set diffusePercent " + g.getDiffusePercent());
    app(str, "set specular " + g.getSpecular());
    app(str, "set specularPercent " + g.getSpecularPercent());
    app(str, "set specularPower " + g.getSpecularPower());
    int se = g.getSpecularExponent();
    int pe = g.getPhongExponent();
    app(str, (Math.pow(2, se) == pe ? "set specularExponent " + se :  "set phongExponent " + pe));
    app(str, "set celShading " + g.getCel());
    app(str, "set celShadingPower " + g.getCelPower());
    app(str, "set zShadePower " + vwr.g.zShadePower);    
    if (isAll)
      getZshadeState(str, vwr.tm, true);
    return str.toString();
  }

  private String getFileState(SB sfunc) {
    SB commands = new SB();
    if (sfunc != null) {
      sfunc.append("  _setFileState;\n");
      commands.append("function _setFileState() {\n\n");
    }
    if (commands.indexOf("append") < 0
        && vwr.getModelSetFileName().equals(JC.ZAP_TITLE))
      commands.append("  zap;\n");
    appendLoadStates(commands);
    if (sfunc != null)
      commands.append("\n}\n\n");
    return commands.toString();
  }

  private void appendLoadStates(SB cmds) {
    Map<String, Boolean> ligandModelSet = vwr.ligandModelSet;
    if (ligandModelSet != null) {
      for (String key : ligandModelSet.keySet()) {
        String data = (String) vwr.ligandModels.get(key + "_data");
        if (data != null)
          cmds.append("  ").append(
              Escape.encapsulateData("ligand_" + key, data.trim() + "\n", JmolDataManager.DATA_TYPE_STRING));
        data = (String) vwr.ligandModels.get(key + "_file");
        if (data != null)
          cmds.append("  ").append(
              Escape.encapsulateData("file_" + key, data.trim() + "\n", JmolDataManager.DATA_TYPE_STRING));
      }
    }
    SB commands = new SB();
    ModelSet ms = vwr.ms;
    Model[] models = ms.am;
    int modelCount = ms.mc;
    for (int i = 0; i < modelCount; i++) {
      if (ms.isJmolDataFrameForModel(i) || ms.isTrajectorySubFrame(i))
        continue;
      Model m = models[i];
      int pt = commands.indexOf(m.loadState);
      if (pt < 0 || pt != commands.lastIndexOf(m.loadState))
        commands.append(models[i].loadState);
      if (models[i].isModelKit) {
        BS bs = ms.getModelAtomBitSetIncludingDeleted(i, false);
        if (ms.tainted != null) {
          if (ms.tainted[AtomCollection.TAINT_COORD] != null)
            ms.tainted[AtomCollection.TAINT_COORD].andNot(bs);
          if (ms.tainted[AtomCollection.TAINT_ELEMENT] != null)
            ms.tainted[AtomCollection.TAINT_ELEMENT].andNot(bs);
        }
        m.loadScript = new SB();
        getInlineData(commands, vwr.getModelExtract(bs, false, true, "MOL"),
            i > 0, null);
      } else {
        commands.appendSB(m.loadScript);
      }
    }
    String s = commands.toString();
    // add a zap command before the first load command.
    if (s.indexOf("data \"append ") < 0) {
      int i = s.indexOf("load /*data*/");
      int j = s.indexOf("load /*file*/");
      if (j >= 0 && j < i)
        i = j;
      if ((j = s.indexOf("load \"@")) >= 0 && j < i)
        i = j;
      if (i >= 0)
        s = s.substring(0, i) + "zap;" + s.substring(i);
    }
    cmds.append(s);
  }

  @Override
  public void getInlineData(SB loadScript, String strModel, boolean isAppend, String loadFilter) {
    String tag = (isAppend ? "append" : "model") + " inline";
    loadScript.append("load /*data*/ data \"").append(tag).append("\"\n")
        .append(strModel).append("end \"").append(tag)
        .append(loadFilter == null || loadFilter.length() == 0 ? "" : " filter" + PT.esc(loadFilter))
        .append("\";");
  }

  private String getColorState(ColorManager cm, SB sfunc) {
    SB s = new SB();
    int n = getCEState(cm.ce, s);
    //String colors = getColorSchemeList(getColorSchemeArray(USER));
    //if (colors.length() > 0)
    //s.append("userColorScheme = " + colors + ";\n");
    if (n > 0 && sfunc != null)
      sfunc.append("\n  _setColorState\n");
    return (n > 0 && sfunc != null ? "function _setColorState() {\n"
        + s.append("}\n\n").toString() : s.toString());
  }

  private int getCEState(ColorEncoder p, SB s) {
    int n = 0;
    for (Map.Entry<String, int[]> entry : p.schemes.entrySet()) {
      String name = entry.getKey();
      if (name.length() > 0 & n++ >= 0)
        s.append("color \"" + name + "="
            + ColorEncoder.getColorSchemeList(entry.getValue()) + "\";\n");
    }
    return n;
  }

  private String getAnimState(AnimationManager am, SB sfunc) {
    int modelCount = vwr.ms.mc;
    if (modelCount < 2)
      return "";
    SB commands = new SB();
    if (sfunc != null) {
      sfunc.append("  _setFrameState;\n");
      commands.append("function _setFrameState() {\n");
    }
    commands.append("# frame state;\n");
    commands.append("# modelCount ").appendI(modelCount).append(";\n# first ")
        .append(vwr.getModelNumberDotted(0)).append(";\n# last ")
        .append(vwr.getModelNumberDotted(modelCount - 1)).append(";\n");
    if (am.backgroundModelIndex >= 0)
      app(commands,
          "set backgroundModel "
              + vwr.getModelNumberDotted(am.backgroundModelIndex));
    if (vwr.tm.bsFrameOffsets != null) {
      app(commands, "frame align " + Escape.eBS(vwr.tm.bsFrameOffsets));
    } else if (vwr.ms.translations != null) {
      for (int i = modelCount; --i >= 0;) {
        P3 t = (vwr.ms.getTranslation(i));
        if (t != null)
          app(commands, "frame " + vwr.ms.getModelNumberDotted(i) + " align "
              + t);
      }
    }
    app(commands,
        "frame RANGE " + am.getModelSpecial(AnimationManager.FRAME_FIRST) + " "
            + am.getModelSpecial(AnimationManager.FRAME_LAST));
    app(commands, "animation DIRECTION "
        + (am.animationDirection == 1 ? "+1" : "-1"));
    app(commands, "animation FPS " + am.animationFps);
    app(commands, "animation MODE " + T.nameOf(am.animationReplayMode) + " "
        + am.firstFrameDelay + " " + am.lastFrameDelay);
    if (am.morphCount > 0)
      app(commands, "animation MORPH " + am.morphCount);
    boolean showModel = true;
    if (am.animationFrames != null) {
      app(commands, "anim frames " + Escape.eAI(am.animationFrames));
      int i = am.caf;
      app(commands, "frame " + (i + 1));
      showModel = (am.cmi != am.modelIndexForFrame(i));
    }
    if (showModel) {
      String s = am.getModelSpecial(AnimationManager.MODEL_CURRENT);
      app(commands, s.equals("0") ? "frame *" : "model " + s);
    }
    app(commands, "animation "
        + (!am.animationOn ? "OFF" : am.currentDirection == 1 ? "PLAY"
            : "PLAYREV"));
    if (am.animationOn && am.animationPaused)
      app(commands, "animation PAUSE");
    if (sfunc != null)
      commands.append("}\n\n");
    return commands.toString();
  }

  /**
   * note that these are not user variables, only global jmol parameters
   * 
   * @param global
   * @param sfunc
   * @return String
   */
  private String getParameterState(GlobalSettings global, SB sfunc) {
    String[] list = new String[global.htBooleanParameterFlags.size()
        + global.htNonbooleanParameterValues.size()];
    SB commands = new SB();
    boolean isState = (sfunc != null);
    if (isState) {
      sfunc.append("  _setParameterState;\n");
      commands.append("function _setParameterState() {\n\n");
    }
    int n = 0;
    //booleans
    for (String key : global.htBooleanParameterFlags.keySet())
      if (GlobalSettings.doReportProperty(key))
        list[n++] = "set " + key + " "
            + global.htBooleanParameterFlags.get(key);
    for (String key : global.htNonbooleanParameterValues.keySet())
      if (GlobalSettings.doReportProperty(key)) {
        Object value = global.htNonbooleanParameterValues.get(key);
        if (key.charAt(0) == '=') {
          //save as =xxxx if you don't want "set" to be there first
          // (=color [element], =frame ...; set unitcell) -- see Viewer.java
          key = key.substring(1);
        } else {
          // one error here is that defaultLattice is being saved as the
          // escaped string set defaultLattice "{...}", which actually is not read
          // and was being improperly read as "{1 1 1}". 
          // leaving it here as it is, now always setting  {0 0 0}
          // otherwise we will break states
          key = (key.indexOf("default") == 0 ? " " : "") + "set " + key;
          value = Escape.e(value);
        }
        list[n++] = key + " " + value;
      }
    switch (global.axesMode) {
    case T.axesunitcell:
      list[n++] = "set axes unitcell";
      break;
    case T.axesmolecular:
      list[n++] = "set axes molecular";
      break;
    default:
      list[n++] = "set axes window";
    }

    Arrays.sort(list, 0, n);
    for (int i = 0; i < n; i++)
      if (list[i] != null)
        app(commands, list[i]);

    String s = StateManager.getVariableList(global.htUserVariables, 0, false,
        true);
    if (s.length() > 0) {
      commands.append("\n#user-defined atom sets; \n");
      commands.append(s);
    }

    // label defaults

    if (vwr.shm.getShape(JC.SHAPE_LABELS) != null)
      commands
          .append(getDefaultLabelState((Labels) vwr.shm.shapes[JC.SHAPE_LABELS]));

    // structure defaults

    if (global.haveSetStructureList) {
      Map<STR, float[]> slist = global.structureList;
      commands.append("struture HELIX set "
          + Escape.eAF(slist.get(STR.HELIX)));
      commands.append("struture SHEET set "
          + Escape.eAF(slist.get(STR.SHEET)));
      commands.append("struture TURN set "
          + Escape.eAF(slist.get(STR.TURN)));
    }
    if (sfunc != null)
      commands.append("\n}\n\n");
    return commands.toString();
  }

  private String getDefaultLabelState(Labels l) {
    SB s = new SB().append("\n# label defaults;\n");
    app(s, "select none");
    app(s, Shape.getColorCommand("label", l.defaultPaletteID,
        l.defaultColix, l.translucentAllowed));
    app(s, "background label " + Shape.encodeColor(l.defaultBgcolix));
    app(s, "set labelOffset " + JC.getXOffset(l.defaultOffset)
        + " " + (JC.getYOffset(l.defaultOffset)));
    String align = JC.getHorizAlignmentName(l.defaultAlignment);
    app(s, "set labelAlignment " + (align.length() < 5 ? "left" : align));
    String pointer = JC.getPointerName(l.defaultPointer);
    app(s, "set labelPointer "
        + (pointer.length() == 0 ? "off" : pointer));
    if ((l.defaultZPos & JC.LABEL_ZPOS_FRONT) != 0)
      app(s, "set labelFront");
    else if ((l.defaultZPos & JC.LABEL_ZPOS_GROUP) != 0)
      app(s, "set labelGroup");
    app(s, Shape.getFontCommand("label", Font
        .getFont3D(l.defaultFontId)));
    return s.toString();
  }

  private String getSelectionState(SelectionManager sm, SB sfunc) {
    SB commands = new SB();
    if (sfunc != null) {
      sfunc.append("  _setSelectionState;\n");
      commands.append("function _setSelectionState() {\n");
    }
    if (vwr.ms.trajectory != null)
      app(commands, vwr.ms.trajectory.getState());
    Map<String, BS> temp = new Hashtable<String, BS>();
    String cmd = null;
    addBs(commands, "hide ", sm.bsHidden);
    addBs(commands, "subset ", sm.bsSubset);
    addBs(commands, "delete ", sm.bsDeleted);
    addBs(commands, "fix ", sm.bsFixed);
    temp.put("-", vwr.slm.getSelectedAtomsNoSubset());
    cmd = getCommands(temp, null, "select");
    if (cmd == null)
      app(commands, "select none");
    else
      commands.append(cmd);
    app(commands, "set hideNotSelected " + sm.hideNotSelected);
    commands.append((String) vwr.getShapeProperty(JC.SHAPE_STICKS,
        "selectionState"));
    if (vwr.getSelectionHalosEnabled())
      app(commands, "SelectionHalos ON");
    if (sfunc != null)
      commands.append("}\n\n");
    return commands.toString();
  }

  private String getViewState(TransformManager tm, SB sfunc) {
    SB commands = new SB();
    String moveToText = tm.getMoveToText(0, false);
    // finalizes transform parameters, in case that has not been done
    if (sfunc != null) {
      sfunc.append("  _setPerspectiveState;\n");
      commands.append("function _setPerspectiveState() {\n");
    }
    app(commands, "set perspectiveModel " + tm.perspectiveModel);
    app(commands, "set scaleAngstromsPerInch "
        + tm.scale3DAngstromsPerInch);
    app(commands, "set perspectiveDepth " + tm.perspectiveDepth);
    app(commands, "set visualRange " + tm.visualRangeAngstroms);
    if (!tm.isWindowCentered())
      app(commands, "set windowCentered false");
    app(commands, "set cameraDepth " + tm.cameraDepth);
    boolean navigating = (tm.mode == TransformManager.MODE_NAVIGATION);
    if (navigating)
      app(commands, "set navigationMode true");
    app(commands, vwr.ms.getBoundBoxCommand(false));
    app(commands, "center " + Escape.eP(tm.fixedRotationCenter));
    commands.append(vwr.getOrientationText(T.name, null, null).toString());

    app(commands, moveToText);
// stereo mode should not be in the state - just a display option
//    if (tm.stereoMode != STER.NONE)
//      app(commands, "stereo "
//          + (tm.stereoColors == null ? tm.stereoMode.getName() : Escape
//              .escapeColor(tm.stereoColors[0])
//              + " " + Escape.escapeColor(tm.stereoColors[1])) + " "
//          + tm.stereoDegrees);
    if (!navigating && !tm.zoomEnabled)
      app(commands, "zoom off");
    commands.append("  slab ").appendI(tm.slabPercentSetting).append(";depth ")
        .appendI(tm.depthPercentSetting).append(
            tm.slabEnabled && !navigating ? ";slab on" : "").append(";\n");
    commands.append("  set slabRange ").appendF(tm.slabRange).append(";\n");
    if (tm.slabPlane != null)
      commands.append("  slab plane ").append(Escape.eP4(tm.slabPlane)).append(
          ";\n");
    if (tm.depthPlane != null)
      commands.append("  depth plane ").append(Escape.eP4(tm.depthPlane))
          .append(";\n");
    getZshadeState(commands, tm, false);
    commands.append(getSpinState(true)).append("\n");
    if (vwr.ms.modelSetHasVibrationVectors() && tm.vibrationOn)
      app(commands, "set vibrationPeriod " + tm.vibrationPeriod
          + ";vibration on");
    boolean slabInternal = (tm.depthPlane != null || tm.slabPlane != null);
    if (navigating) {
      commands.append(tm.getNavigationState());
    } 
    if (!tm.slabEnabled && slabInternal)
      commands.append("  slab off;\n");
    if (sfunc != null)
      commands.append("}\n\n");
    return commands.toString();
  }

  private void getZshadeState(SB s, TransformManager tm, boolean isAll) {
    
    if (isAll) {
      app(s,"set zDepth " + tm.zDepthPercentSetting);
      app(s,"set zSlab " + tm.zSlabPercentSetting);
      if (!tm.zShadeEnabled)
        app(s,"set zShade false");
    }
    if (tm.zShadeEnabled)
      app(s, "set zShade true");
    try {
      if (tm.zSlabPoint != null)
        app(s,"set zSlab " + Escape.eP(tm.zSlabPoint));
    } catch (Exception e) {
      // don't care
    }
  }


  /**
   * @param isAll
   * @return spin state
   */
  @Override
  String getSpinState(boolean isAll) {
    TransformManager tm = vwr.tm;
    String s = "  set spinX " + (int) tm.spinX + "; set spinY "
        + (int) tm.spinY + "; set spinZ " + (int) tm.spinZ + "; set spinFps "
        + (int) tm.spinFps + ";";
    if (!Float.isNaN(tm.navFps))
      s += "  set navX " + (int) tm.navX + "; set navY " + (int) tm.navY
          + "; set navZ " + (int) tm.navZ + "; set navFps " + (int) tm.navFps
          + ";";
    if (tm.navOn)
      s += " navigation on;";
    if (!tm.spinOn)
      return s;
    String prefix = (tm.isSpinSelected ? "\n  select "
        + Escape.eBS(vwr.bsA()) + ";\n  rotateSelected"
        : "\n ");
    if (tm.isSpinInternal) {
      P3 pt = P3.newP(tm.internalRotationCenter);
      pt.sub(tm.rotationAxis);
      s += prefix + " spin " + tm.rotationRate + " "
          + Escape.eP(tm.internalRotationCenter) + " " + Escape.eP(pt);
    } else if (tm.isSpinFixed) {
      s += prefix + " spin axisangle " + Escape.eP(tm.rotationAxis) + " "
          + tm.rotationRate;
    } else {
      s += " spin on";
    }
    return s + ";";
  }

  //// info 

  //// utility methods

  @Override
  String getCommands(Map<String, BS> htDefine, Map<String, BS> htMore,
                            String selectCmd) {
    SB s = new SB();
    String setPrev = getCommands2(htDefine, s, null, selectCmd);
    if (htMore != null)
      getCommands2(htMore, s, setPrev, "select");
    return s.toString();
  }

  private String getCommands2(Map<String, BS> ht, SB s, String setPrev,
                                     String selectCmd) {
    if (ht == null)
      return "";
    for (Map.Entry<String, BS> entry : ht.entrySet()) {
      String key = entry.getKey();
      String set = Escape.eBS(entry.getValue());
      if (set.length() < 5) // nothing selected
        continue;
      set = selectCmd + " " + set;
      if (!set.equals(setPrev))
        app(s, set);
      setPrev = set;
      if (key.indexOf("-") != 0) // - for key means none required
        app(s, key);
    }
    return setPrev;
  }

  private void app(SB s, String cmd) {
    if (cmd.length() != 0)
      s.append("  ").append(cmd).append(";\n");
  }

  private void addBs(SB sb, String key, BS bs) {
    if (bs == null || bs.length() == 0)
      return;
    app(sb, key + Escape.eBS(bs));
  }

  private String getFontState(String myType, Font font3d) {
    int objId = StateManager.getObjectIdFromName(myType
        .equalsIgnoreCase("axes") ? "axis" : myType);
    if (objId < 0)
      return "";
    int mad = vwr.getObjectMad10(objId);
    SB s = new SB().append("\n");
    app(s, myType
        + (mad == 0 ? " off" : mad == 1 ? " on" : mad == -1 ? " dotted"
            : mad < 20 ? " " + mad : " " + (mad / 20000f)));
    if (s.length() < 3)
      return "";
    String fcmd = Shape.getFontCommand(myType, font3d);
    if (fcmd.length() > 0)
      fcmd = "  " + fcmd + ";\n";
    return (s + fcmd);
  }

  private void appendTickInfo(String myType, SB sb, TickInfo t) {
    sb.append("  ");
    sb.append(myType);
    addTickInfo(sb, t, false);
    sb.append(";\n");
  }

  private static void addTickInfo(SB sb, TickInfo tickInfo, boolean addFirst) {
    sb.append(" ticks ").append(tickInfo.type).append(" ").append(
        Escape.eP(tickInfo.ticks));
    boolean isUnitCell = (tickInfo.scale != null && Float
        .isNaN(tickInfo.scale.x));
    if (isUnitCell)
      sb.append(" UNITCELL");
    if (tickInfo.tickLabelFormats != null)
      sb.append(" format ")
          .append(Escape.eAS(tickInfo.tickLabelFormats, false));
    if (!isUnitCell && tickInfo.scale != null)
      sb.append(" scale ").append(Escape.eP(tickInfo.scale));
    if (addFirst && !Float.isNaN(tickInfo.first) && tickInfo.first != 0)
      sb.append(" first ").appendF(tickInfo.first);
    if (tickInfo.reference != null) // not implemented
      sb.append(" point ").append(Escape.eP(tickInfo.reference));
  }

  private String getMeasurementState(Measures shape) {

    Lst<Measurement> mList = shape.measurements;
    int measurementCount = shape.measurementCount;
    Font font3d = Measures.font3d;
    TickInfo ti = shape.defaultTickInfo;
    SB commands = new SB();
    app(commands, "measures delete");
    for (int i = 0; i < measurementCount; i++) {
      Measurement m = mList.get(i);
      boolean isProperty = (m.property != null);
      if (isProperty && Float.isNaN(m.value))
        continue;
      int count = m.count;
      SB sb = new SB().append("measure");
      if (m.thisID != null)
        sb.append(" ID ").append(PT.esc(m.thisID));
      if (m.mad != 0)
        sb.append(" radius ").appendF(
            m.thisID == null || m.mad > 0 ? m.mad / 2000f : 0);
      if (m.colix != 0)
        sb.append(" color ").append(Escape.escapeColor(C.getArgb(m.colix)));
      if (m.text != null) {
        if (m.text.font != null)
          sb.append(" font ").append(m.text.font.getInfo());
        if (m.text.align != JC.TEXT_ALIGN_NONE)
          sb.append(" align ").append(JC.getHorizAlignmentName(m.text.align));
        if (m.text.pymolOffset != null)
          sb.append(" offset ").append(Escape.eAF(m.text.pymolOffset));
      }
      TickInfo tickInfo = m.tickInfo;
      if (tickInfo != null)
        addTickInfo(sb, tickInfo, true);
      for (int j = 1; j <= count; j++)
        sb.append(" ").append(m.getLabel(j, true, true));
      if (isProperty)
        sb.append(" " + m.property + " value " + (Float.isNaN(m.value) ? 0f : m.value))
        .append(" " + PT.esc(m.getString()));
      //sb.append("; # " + shape.getInfoAsString(i));
      app(commands, sb.toString());
    }
    app(commands, Shape.getFontCommand("measures", font3d));
    int nHidden = 0;
    Map<String, BS> temp = new Hashtable<String, BS>();
    BS bs = BS.newN(measurementCount);
    for (int i = 0; i < measurementCount; i++) {
      Measurement m = mList.get(i);
      if (m.isHidden) {
        nHidden++;
        bs.set(i);
      }
      if (shape.bsColixSet != null && shape.bsColixSet.get(i))
        BSUtil.setMapBitSet(temp, i, i, Shape.getColorCommandUnk("measure",
            m.colix, shape.translucentAllowed));
        
    }
    if (nHidden > 0)
      if (nHidden == measurementCount)
        app(commands, "measures off; # lines and numbers off");
      else
        for (int i = 0; i < measurementCount; i++)
          if (bs.get(i))
            BSUtil.setMapBitSet(temp, i, i, "measure off");
    if (ti != null) {
      commands.append(" measure ");
      addTickInfo(commands, ti, true);
      commands.append(";\n");
    }
    if (shape.mad >= 0)
      commands.append(" set measurements ").appendF(shape.mad / 2000f).append(";\n");
    String s = getCommands(temp, null, "select measures");
    if (s != null && s.length() != 0) {
      commands.append(s);
      app(commands, "select measures ({null})");
    }

    return commands.toString();
  }

  private Map<String, BS> temp = new Hashtable<String, BS>();
  private Map<String, BS> temp2 = new Hashtable<String, BS>();
  private Map<String, BS> temp3 = new Hashtable<String, BS>();

  private void getShapeStatePriv(SB commands, boolean isAll, int iShape) {
    Shape[] shapes = vwr.shm.shapes;
    if (shapes == null)
      return;
    int i;
    int imax;
    if (iShape == Integer.MAX_VALUE) {
      i = 0;
      imax = JC.SHAPE_MAX;
    } else {
      imax = (i = iShape) + 1;
    }
    for (; i < imax; ++i) {
      Shape shape = shapes[i];
      if (shape != null
          && (isAll || i >= JC.SHAPE_MIN_SECONDARY
              && i < JC.SHAPE_MAX_SECONDARY)) {
        String cmd = getShapeState(shape);
        if (cmd != null && cmd.length() > 1)
          commands.append(cmd);
      }
    }
    commands.append("  select *;\n");
  }

  private String getBondState(Sticks shape) {
    BS bsOrderSet = shape.bsOrderSet;
    boolean reportAll = shape.reportAll;
    clearTemp();
    ModelSet modelSet = vwr.ms;
    boolean haveTainted = false;
    Bond[] bonds = modelSet.bo;
    int bondCount = modelSet.bondCount;
    short r;

    if (reportAll || shape.bsSizeSet != null) {
      int i0 = (reportAll ? bondCount - 1 : shape.bsSizeSet.nextSetBit(0));
      for (int i = i0; i >= 0; i = (reportAll ? i - 1 : shape.bsSizeSet
          .nextSetBit(i + 1)))
        BSUtil.setMapBitSet(temp, i, i, "wireframe "
            + ((r = bonds[i].mad) == 1 ? "on" : "" + PT.escF(r / 2000f)));
    }
    if (reportAll || bsOrderSet != null) {
      int i0 = (reportAll ? bondCount - 1 : bsOrderSet.nextSetBit(0));
      for (int i = i0; i >= 0; i = (reportAll ? i - 1 : bsOrderSet
          .nextSetBit(i + 1))) {
        Bond bond = bonds[i];
        if (reportAll || (bond.order & Edge.BOND_NEW) == 0)
          BSUtil.setMapBitSet(temp, i, i, "bondOrder "
              + Edge.getBondOrderNameFromOrder(bond.order));
      }
    }
    if (shape.bsColixSet != null)
      for (int i = shape.bsColixSet.nextSetBit(0); i >= 0; i = shape.bsColixSet
          .nextSetBit(i + 1)) {
        short colix = bonds[i].colix;
        if ((colix & C.OPAQUE_MASK) == C.USE_PALETTE)
          BSUtil.setMapBitSet(temp, i, i, Shape.getColorCommand("bonds",
              PAL.CPK.id, colix, shape.translucentAllowed));
        else
          BSUtil.setMapBitSet(temp, i, i, Shape.getColorCommandUnk("bonds",
              colix, shape.translucentAllowed));
      }

    String s = getCommands(temp, null, "select BONDS") + "\n"
        + (haveTainted ? getCommands(temp2, null, "select BONDS") + "\n" : "");
    clearTemp();
    return s;
  }

  private void clearTemp() {
    temp.clear();
    temp2.clear();
  }

  private String getShapeState(Shape shape) {
    String s;
    switch (shape.shapeID) {
    case JC.SHAPE_AXES:
      s = getAxesState((Axes) shape);
      break;
    case JC.SHAPE_UCCAGE:
      if (!vwr.ms.haveUnitCells)
        return "";
      String st = s = getFontLineShapeState((FontLineShape) shape);
      int iAtom = vwr.am.cai;
      if (iAtom >= 0)
        s += "  unitcell ({" + iAtom + "});\n"; 
      SymmetryInterface uc = vwr.getCurrentUnitCell();
      if (uc != null) { 
        s += uc.getUnitCellState();
        s += st; // needs to be after this state as well.
      }
      break;
    case JC.SHAPE_BBCAGE:
      s = getFontLineShapeState((FontLineShape) shape);
      break;
    case JC.SHAPE_FRANK:
      s = getFontState(shape.myType, ((Frank) shape).baseFont3d);
      break;
    case JC.SHAPE_MEASURES:
      s = getMeasurementState((Measures) shape);
      break;
    case JC.SHAPE_STARS:
    case JC.SHAPE_VECTORS:
      s = getAtomShapeState((AtomShape) shape);
      break;
    case JC.SHAPE_STICKS:
      s = getBondState((Sticks) shape);
      break;
    case JC.SHAPE_ECHO:
      Echo es = (Echo) shape;
      SB sb = new SB();
      sb.append("\n  set echo off;\n");
      for (Text t : es.objects.values()) {
        sb.append(getTextState(t));
        if (t.hidden)
          sb.append("  set echo ID ").append(PT.esc(t.target))
              .append(" hidden;\n");
      }
      s = sb.toString();
      break;
    case JC.SHAPE_HALOS:
      Halos hs = (Halos) shape;
      s = getAtomShapeState(hs)
          + (hs.colixSelection == C.USE_PALETTE ? ""
              : hs.colixSelection == C.INHERIT_ALL ? "  color SelectionHalos NONE;\n"
                  : Shape.getColorCommandUnk("selectionHalos",
                      hs.colixSelection, hs.translucentAllowed) + ";\n");
      if (hs.bsHighlight != null)
        s += "  set highlight "
            + Escape.eBS(hs.bsHighlight)
            + "; "
            + Shape.getColorCommandUnk("highlight", hs.colixHighlight,
                hs.translucentAllowed) + ";\n";
      break;
    case JC.SHAPE_HOVER:
      clearTemp();
      Hover h = (Hover) shape;
      if (h.atomFormats != null)
        for (int i = vwr.ms.ac; --i >= 0;)
          if (h.atomFormats[i] != null)
            BSUtil.setMapBitSet(temp, i, i,
                "set hoverLabel " + PT.esc(h.atomFormats[i]));
      s = "\n  hover " + PT.esc((h.labelFormat == null ? "" : h.labelFormat))
          + ";\n" + getCommands(temp, null, "select");
      clearTemp();
      break;
    case JC.SHAPE_LABELS:
      Labels l = (Labels) shape;
      if (!l.isActive || l.bsSizeSet == null)
        return "";
      clearTemp();
      for (int i = l.bsSizeSet.nextSetBit(0); i >= 0; i = l.bsSizeSet
          .nextSetBit(i + 1)) {
        Text t = l.getLabel(i);
        String cmd = "label ";
        if (t == null) {
          cmd += PT.esc(l.formats[i]);
        } else {
          cmd += PT.esc(t.textUnformatted);
          if (t.pymolOffset != null)
            cmd += ";set labelOffset " + Escape.eAF(t.pymolOffset);
        }
        BSUtil.setMapBitSet(temp, i, i, cmd);
        if (l.bsColixSet != null && l.bsColixSet.get(i))
          BSUtil.setMapBitSet(temp2, i, i, Shape.getColorCommand("label",
              l.paletteIDs[i], l.colixes[i], l.translucentAllowed));
        if (l.bsBgColixSet != null && l.bsBgColixSet.get(i))
          BSUtil.setMapBitSet(temp2, i, i,
              "background label " + Shape.encodeColor(l.bgcolixes[i]));
        Text text = l.getLabel(i);
        float sppm = (text != null ? text.scalePixelsPerMicron : 0);
        if (sppm > 0)
          BSUtil.setMapBitSet(temp2, i, i, "set labelScaleReference "
              + (10000f / sppm));
        if (l.offsets != null && l.offsets.length > i) {
          int offsetFull = l.offsets[i];
          BSUtil.setMapBitSet(
              temp2,
              i,
              i,
              "set "
                  + (JC.isOffsetAbsolute(offsetFull) ? "labelOffsetAbsolute "
                      : "labelOffset ") + JC.getXOffset(offsetFull) + " "
                  + JC.getYOffset(offsetFull));
          String align = JC.getHorizAlignmentName(offsetFull >> 2);
          String pointer = JC.getPointerName(offsetFull);
          if (pointer.length() > 0)
            BSUtil.setMapBitSet(temp2, i, i, "set labelPointer " + pointer);
          if ((offsetFull & JC.LABEL_ZPOS_FRONT) != 0)
            BSUtil.setMapBitSet(temp2, i, i, "set labelFront");
          else if ((offsetFull & JC.LABEL_ZPOS_GROUP) != 0)
            BSUtil.setMapBitSet(temp2, i, i, "set labelGroup");
          // labelAlignment must come last, so we put it in a separate hash
          // table
          if (align.length() > 0)
            BSUtil.setMapBitSet(temp3, i, i, "set labelAlignment " + align);
        }

        if (l.mads != null && l.mads[i] < 0)
          BSUtil.setMapBitSet(temp2, i, i, "set toggleLabel");
        if (l.bsFontSet != null && l.bsFontSet.get(i))
          BSUtil.setMapBitSet(temp2, i, i,
              Shape.getFontCommand("label", Font.getFont3D(l.fids[i])));
      }
      s = getCommands(temp, temp2, "select")
          + getCommands(null, temp3, "select");
      temp3.clear();
      clearTemp();
      break;
    case JC.SHAPE_BALLS:
      clearTemp();
      int ac = vwr.ms.ac;
      Atom[] atoms = vwr.ms.at;
      Balls balls = (Balls) shape;
      short[] colixes = balls.colixes;
      byte[] pids = balls.paletteIDs;
      float r = 0;
      for (int i = 0; i < ac; i++) {
        if (shape.bsSizeSet != null && shape.bsSizeSet.get(i)) {
          if ((r = atoms[i].madAtom) < 0)
            BSUtil.setMapBitSet(temp, i, i, "Spacefill on");
          else
            BSUtil.setMapBitSet(temp, i, i, "Spacefill " + PT.escF(r / 2000f));
        }
        if (shape.bsColixSet != null && shape.bsColixSet.get(i)) {
          byte pid = atoms[i].paletteID;
          if (pid != PAL.CPK.id || C.isColixTranslucent(atoms[i].colixAtom))
            BSUtil.setMapBitSet(temp, i, i, Shape.getColorCommand("atoms", pid,
                atoms[i].colixAtom, shape.translucentAllowed));
          if (colixes != null && i < colixes.length)
            BSUtil.setMapBitSet(temp2, i, i, Shape.getColorCommand("balls",
                pids[i], colixes[i], shape.translucentAllowed));
        }
      }
      s = getCommands(temp, temp2, "select");
      clearTemp();
      break;
    default:
      s = shape.getShapeState();
      break;
    }
    return s;
  }

  private String getFontLineShapeState(FontLineShape shape) {
    String s = getFontState(shape.myType, shape.font3d);
    if (shape.tickInfos == null)
      return s;
    boolean isOff = (s.indexOf(" off") >= 0);
    SB sb = new SB();
    sb.append(s);
    for (int i = 0; i < 4; i++)
      if (shape.tickInfos[i] != null)
        appendTickInfo(shape.myType, sb, shape.tickInfos[i]);
    if (isOff)
      sb.append("  " + shape.myType + " off;\n");
    return sb.toString();
  }
  
  private String getAxesState(Axes axes) {
    SB sb = new SB();
    sb.append(getFontLineShapeState(axes));
    sb.append("  axes scale ").appendF(vwr.getFloat(T.axesscale)).append(";\n"); 
    if (axes.fixedOrigin != null)
      sb.append("  axes center ")
          .append(Escape.eP(axes.fixedOrigin)).append(";\n");
    P3 axisXY = axes.axisXY;
    if (axisXY.z != 0)
      sb.append("  axes position [")
          .appendI((int) axisXY.x).append(" ")
          .appendI((int) axisXY.y).append(" ")
          .append(axisXY.z < 0 ? " %" : "").append("];\n");
    String[] labels = axes.labels;
    if (labels != null) {
      sb.append("  axes labels ");
      for (int i = 0; i < labels.length; i++)
        if (labels[i] != null)
          sb.append(PT.esc(labels[i])).append(" ");
      sb.append(";\n");
    }
    if (axes.axisType != null) {
      sb.append("  axes type " + PT.esc(axes.axisType));
    }
    return sb.toString();
  }


  @Override
  public String getAtomShapeState(AtomShape shape) {
    // called also by Polyhedra
    if (!shape.isActive)
      return "";
    clearTemp();
    String type = JC.shapeClassBases[shape.shapeID];
    boolean isVector = (shape.shapeID == JC.SHAPE_VECTORS);
    int mad;
    if (shape.bsSizeSet != null)
      for (int i = shape.bsSizeSet.nextSetBit(0); i >= 0; i = shape.bsSizeSet
          .nextSetBit(i + 1))
        BSUtil.setMapBitSet(temp, i, i, type
            + " " + ((mad = shape.mads[i]) < 0 ? (isVector && mad < -1 ? "" + -mad :  "on") : PT.escF(mad / 2000f)));
    if (shape.bsColixSet != null)
      for (int i = shape.bsColixSet.nextSetBit(0); i >= 0; i = shape.bsColixSet
          .nextSetBit(i + 1))
        BSUtil.setMapBitSet(temp2, i, i, Shape.getColorCommand(type,
            shape.paletteIDs[i], shape.colixes[i], shape.translucentAllowed));
    String s = getCommands(temp, temp2, "select");
    clearTemp();
    return s;
  }

  private String getTextState(Text t) {
    SB s = new SB();
    String text = t.text;
    if (text == null || !t.isEcho || t.target.equals("error"))
      return "";
    //set echo top left
    //set echo myecho x y
    //echo .....
    boolean isImage = (t.image != null);
    //    if (isDefine) {
    String strOff = null;
    String echoCmd = "set echo ID " + PT.esc(t.target);
    switch (t.valign) {
    case JC.ECHO_XY:
      if (t.movableXPercent == Integer.MAX_VALUE
          || t.movableYPercent == Integer.MAX_VALUE) {
        strOff = (t.movableXPercent == Integer.MAX_VALUE ? t.movableX + " "
            : t.movableXPercent + "% ")
            + (t.movableYPercent == Integer.MAX_VALUE ? t.movableY + ""
                : t.movableYPercent + "%");
      } else {
        strOff = "[" + t.movableXPercent + " " + t.movableYPercent + "%]";
      }
      //$FALL-THROUGH$
    case JC.ECHO_XYZ:
      if (strOff == null)
        strOff = Escape.eP(t.xyz);
      s.append("  ").append(echoCmd).append(" ").append(strOff);
      if (t.align != JC.TEXT_ALIGN_LEFT)
        s.append(";  ").append(echoCmd).append(" ").append(
            JC.getHorizAlignmentName(t.align));
      break;
    default:
      s.append("  set echo ").append(JC.getEchoName(t.valign)).append(" ")
          .append(JC.getHorizAlignmentName(t.align));
    }
    if (t.movableZPercent != Integer.MAX_VALUE)
      s.append(";  ").append(echoCmd).append(" depth ").appendI(
          t.movableZPercent);
    if (isImage)
      s.append("; ").append(echoCmd).append(" IMAGE /*file*/");
    else
      s.append("; echo ");
    s.append(PT.esc(text)); // was textUnformatted, but that is not really the STATE
    s.append(";\n");
    if (isImage && t.imageScale != 1)
      s.append("  ").append(echoCmd).append(" scale ").appendF(t.imageScale)
          .append(";\n");
    if (t.script != null)
      s.append("  ").append(echoCmd).append(" script ").append(
          PT.esc(t.script)).append(";\n");
    if (t.modelIndex >= 0)
      s.append("  ").append(echoCmd).append(" model ").append(
          vwr.getModelNumberDotted(t.modelIndex)).append(";\n");
    if (t.pointerPt != null) {
      s.append("  ").append(echoCmd).append(" point ").append(
          t.pointerPt instanceof Atom ? "({" + ((Atom) t.pointerPt).i
              + "})" : Escape.eP(t.pointerPt)).append(";\n");
    }
    if (t.pymolOffset != null) {
      s.append("  ").append(echoCmd).append(" offset ").append(
          Escape.escapeFloatA(t.pymolOffset, true)).append(";\n");
    }
    //    }
    //isDefine and target==top: do all
    //isDefine and target!=top: just start
    //!isDefine and target==top: do nothing
    //!isDefine and target!=top: do just this
    //fluke because top is defined with default font
    //in initShape(), so we MUST include its font def here
    //    if (isDefine != target.equals("top"))
    //      return s.toString();
    // these may not change much:
    t.appendFontCmd(s);
    s.append("; color echo");
    if (C.isColixTranslucent(t.colix))
      s.append(C.getColixTranslucencyLabel(t.colix));
    s.append(" ").append(C.getHexCode(t.colix));
    if (t.bgcolix != 0) {
      s.append("; color echo background ");
      if (C.isColixTranslucent(t.bgcolix))
        s.append(C.getColixTranslucencyLabel(t.bgcolix)).append(" ");
      s.append(C.getHexCode(t.bgcolix));
    }
    s.append(";\n");
    return s.toString();
  }

  @Override
  String getAllSettings(String prefix) {
    GlobalSettings g = vwr.g;
    SB commands = new SB();
    String[] list = new String[g.htBooleanParameterFlags.size()
        + g.htNonbooleanParameterValues.size() + g.htUserVariables.size()];
    //booleans
    int n = 0;
    String _prefix = "_" + prefix;
    for (String key : g.htBooleanParameterFlags.keySet()) {
      if (prefix == null || key.indexOf(prefix) == 0
          || key.indexOf(_prefix) == 0)
        list[n++] = (key.indexOf("_") == 0 ? key + " = " : "set " + key + " ")
            + g.htBooleanParameterFlags.get(key);
    }
    //save as _xxxx if you don't want "set" to be there first
    for (String key : g.htNonbooleanParameterValues.keySet()) {
      if (key.charAt(0) != '@'
          && (prefix == null || key.indexOf(prefix) == 0 || key
              .indexOf(_prefix) == 0)) {
        Object value = g.htNonbooleanParameterValues.get(key);
        if (value instanceof String)
          value = chop(PT.esc((String) value));
        list[n++] = (key.indexOf("_") == 0 ? key + " = " : "set " + key + " ")
            + value;
      }
    }
    for (String key : g.htUserVariables.keySet()) {
      if (prefix == null || key.indexOf(prefix) == 0) {
        SV value = g.htUserVariables.get(key);
        String s = value.escape();
        list[n++] = key + " " + (key.startsWith("@") ? "" : "= ")
            + (value.tok == T.string ? chop(PT.esc(s)) : s);
      }
    }
    Arrays.sort(list, 0, n);
    for (int i = 0; i < n; i++)
      if (list[i] != null)
        app(commands, list[i]);
    commands.append("\n");
    return commands.toString();
  }

  private static String chop(String s) {
    int len = s.length();
    if (len < 512)
      return s;
    SB sb = new SB();
    String sep = "\"\\\n    + \"";
    int pt = 0;
    for (int i = 72; i < len; pt = i, i += 72) {
      while (s.charAt(i - 1) == '\\')
        i++;
      sb.append((pt == 0 ? "" : sep)).append(s.substring(pt, i));
    }
    sb.append(sep).append(s.substring(pt, len));
    return sb.toString();
  }

  @Override
  String getFunctionCalls(String f) {
    if (f == null)
      f = "";
    SB s = new SB();
    int pt = f.indexOf("*");
    boolean isGeneric = (pt >= 0);
    boolean isStatic = (f.indexOf("static_") == 0);
    boolean namesOnly = (f.equalsIgnoreCase("names") || f
        .equalsIgnoreCase("static_names"));
    if (namesOnly)
      f = "";
    if (isGeneric)
      f = f.substring(0, pt);
    f = f.toLowerCase();
    if (isStatic || f.length() == 0)
      addFunctions(s, Viewer.staticFunctions, f, isGeneric, namesOnly);
    if (!isStatic || f.length() == 0)
      addFunctions(s, vwr.localFunctions, f, isGeneric, namesOnly);
    return s.toString();
  }

  private void addFunctions(SB s, Map<String, JmolScriptFunction> ht, String selectedFunction,
                            boolean isGeneric, boolean namesOnly) {
    String[] names = new String[ht.size()];
    int n = 0;
    for (String name : ht.keySet())
      if (selectedFunction.length() == 0 && !name.startsWith("_")
          || name.equalsIgnoreCase(selectedFunction) || isGeneric
          && name.toLowerCase().indexOf(selectedFunction) == 0)
        names[n++] = name;
    Arrays.sort(names, 0, n);
    for (int i = 0; i < n; i++) {
      JmolScriptFunction f = ht.get(names[i]);
      s.append(namesOnly ? f.getSignature() : f.toString());
      s.appendC('\n');
    }
  }

  private static boolean isTainted(BS[] tainted, int atomIndex, int type) {
    return (tainted != null && tainted[type] != null && tainted[type]
        .get(atomIndex));
  }

  @Override
  String getAtomicPropertyState(int taintWhat, BS bsSelected) {
    if (!vwr.g.preserveState)
      return "";
    BS bs;
    SB commands = new SB();
    for (int type = 0; type < AtomCollection.TAINT_MAX; type++)
      if (taintWhat < 0 || type == taintWhat)
        if ((bs = (bsSelected != null ? bsSelected : vwr
            .ms.getTaintedAtoms(type))) != null)
          getAtomicPropertyStateBuffer(commands, type, bs, null, null);
    return commands.toString();
  }

  @Override
  void getAtomicPropertyStateBuffer(SB commands, int type, BS bs,
                                           String label, float[] fData) {
    if (!vwr.g.preserveState)
      return;
    // see setAtomData()
    SB s = new SB();
    String dataLabel = (label == null ? AtomCollection.userSettableValues[type]
        : label)
        + " set";
    int n = 0;
    boolean isDefault = (type == AtomCollection.TAINT_COORD);
    Atom[] atoms = vwr.ms.at;
    BS[] tainted = vwr.ms.tainted;
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        if (atoms[i].isDeleted())
          continue;
        s.appendI(i + 1).append(" ").append(atoms[i].getElementSymbol())
            .append(" ").append(atoms[i].getInfo().replace(' ', '_')).append(
                " ");
        switch (type) {
        case AtomCollection.TAINT_MAX:
          if (i < fData.length) // when data are appended, the array may not
            // extend that far
            s.appendF(fData[i]);
          break;
        case AtomCollection.TAINT_ATOMNO:
          s.appendI(atoms[i].getAtomNumber());
          break;
        case AtomCollection.TAINT_CHAIN:
          s.append(atoms[i].getChainIDStr());
          break;
        case AtomCollection.TAINT_RESNO:
          s.appendI(atoms[i].group.getResno());
          break;
        case AtomCollection.TAINT_SEQID:
          s.appendI(atoms[i].getSeqID());
          break;
        case AtomCollection.TAINT_ATOMNAME:
          s.append(atoms[i].getAtomName());
          break;
        case AtomCollection.TAINT_ATOMTYPE:
          s.append(atoms[i].getAtomType());
          break;
        case AtomCollection.TAINT_COORD:
          if (isTainted(tainted, i, AtomCollection.TAINT_COORD))
            isDefault = false;
          s.appendF(atoms[i].x).append(" ").appendF(atoms[i].y).append(" ")
              .appendF(atoms[i].z);
          break;
        case AtomCollection.TAINT_VIBRATION:
          Vibration v = atoms[i].getVibrationVector();
          if (v == null)
            s.append("0 0 0");
          else if (Float.isNaN(v.modScale))
            s.appendF(v.x).append(" ").appendF(v.y).append(" ").appendF(v.z);
          else
            s.appendF(PT.FLOAT_MIN_SAFE).append(" ").appendF(PT.FLOAT_MIN_SAFE).append(" ").appendF(v.modScale);
          break;
        case AtomCollection.TAINT_ELEMENT:
          s.appendI(atoms[i].getAtomicAndIsotopeNumber());
          break;
        case AtomCollection.TAINT_FORMALCHARGE:
          s.appendI(atoms[i].getFormalCharge());
          break;
        case AtomCollection.TAINT_BONDINGRADIUS:
          s.appendF(atoms[i].getBondingRadius());
          break;
        case AtomCollection.TAINT_OCCUPANCY:
          s.appendI(atoms[i].getOccupancy100());
          break;
        case AtomCollection.TAINT_PARTIALCHARGE:
          s.appendF(atoms[i].getPartialCharge());
          break;
        case AtomCollection.TAINT_TEMPERATURE:
          s.appendF(atoms[i].getBfactor100() / 100f);
          break;
        case AtomCollection.TAINT_VALENCE:
          s.appendI(atoms[i].getValence());
          break;
        case AtomCollection.TAINT_VANDERWAALS:
          s.appendF(atoms[i].getVanderwaalsRadiusFloat(vwr, VDW.AUTO));
          break;
        }
        s.append(" ;\n");
        ++n;
      }
    if (n == 0)
      return;
    if (isDefault)
      dataLabel += "(default)";
    commands.append("\n  DATA \"" + dataLabel + "\"\n").appendI(n).append(
        " ;\nJmol Property Data Format 1 -- Jmol ").append(
        Viewer.getJmolVersion()).append(";\n");
    commands.appendSB(s);
    commands.append("  end \"" + dataLabel + "\";\n");
  }

  
  /////////////////////////////////  undo/redo functions /////////////////////
  
  
  @Override
  void undoMoveAction(int action, int n) {
    switch (action) {
    case T.undomove:
    case T.redomove:
      switch (n) {
      case -2:
        vwr.undoClear();
        break;
      case -1:
        (action == T.undomove ? vwr.actionStates : vwr.actionStatesRedo)
            .clear();
        break;
      case 0:
        n = Integer.MAX_VALUE;
        //$FALL-THROUGH$
      default:
        if (n > MAX_ACTION_UNDO)
          n = (action == T.undomove ? vwr.actionStates
              : vwr.actionStatesRedo).size();
        for (int i = 0; i < n; i++)
          undoMoveActionClear(0, action, true);
      }
      break;
    }
  }

  @Override
  void undoMoveActionClear(int taintedAtom, int type, boolean clearRedo) {
    // called by actionManager
    if (!vwr.g.preserveState)
      return;
    int modelIndex = (taintedAtom >= 0 ? vwr.ms.at[taintedAtom].mi
        : vwr.ms.mc - 1);
    //System.out.print("undoAction " + type + " " + taintedAtom + " modelkit?"
    //    + modelSet.models[modelIndex].isModelkit());
    //System.out.println(" " + type + " size=" + actionStates.size() + " "
    //    + +actionStatesRedo.size());
    switch (type) {
    case T.redomove:
    case T.undomove:
      // from MouseManager
      // CTRL-Z: type = 1 UNDO
      // CTRL-Y: type = -1 REDO
      vwr.stopMinimization();
      String s = "";
      Lst<String> list1;
      Lst<String> list2;
      switch (type) {
      default:
      case T.undomove:
        list1 = vwr.actionStates;
        list2 = vwr.actionStatesRedo;
        break;
      case T.redomove:
        list1 = vwr.actionStatesRedo;
        list2 = vwr.actionStates;
        if (vwr.actionStatesRedo.size() == 1)
          return;
        break;
      }
      if (list1.size() == 0 || undoWorking)
        return;
      undoWorking = true;
      list2.add(0, list1.removeItemAt(0));
      s = vwr.actionStatesRedo.get(0);
      if (type == T.undomove && list2.size() == 1) {
        // must save current state, coord, etc.
        // but this destroys actionStatesRedo
        int[] pt = new int[] { 1 };
        type = PT.parseIntNext(s, pt);
        taintedAtom = PT.parseIntNext(s, pt);
        undoMoveActionClear(taintedAtom, type, false);
      }
      //System.out.println("redo type = " + type + " size=" + actionStates.size()
      //    + " " + +actionStatesRedo.size());
      if (vwr.ms.am[modelIndex].isModelKit
          || s.indexOf("zap ") < 0) {
        if (Logger.debugging)
          vwr.log(s);
        vwr.evalStringQuiet(s);
      } else {
        // if it's not modelkit mode and we are trying to do a zap, then ignore
        // and clear all action states.
        vwr.actionStates.clear();
      }
      break;
    default:
      if (undoWorking && clearRedo)
        return;
      undoWorking = true;
      BS bs;
      SB sb = new SB();
      sb.append("#" + type + " " + taintedAtom + " " + (new Date()) + "\n");
      if (taintedAtom >= 0) {
        bs = vwr.getModelUndeletedAtomsBitSet(modelIndex);
        vwr.ms.taintAtoms(bs, type);
        sb.append(getAtomicPropertyState(-1, null));
      } else {
        bs = vwr.getModelUndeletedAtomsBitSet(modelIndex);
        sb.append("zap ");
        sb.append(Escape.eBS(bs)).append(";");
        getInlineData(sb, vwr.getModelExtract(bs, false, true,
            "MOL"), true, null);
        sb.append("set refreshing false;").append(
            vwr.acm.getPickingState()).append(
            vwr.tm.getMoveToText(0, false)).append(
            "set refreshing true;");

      }
      if (clearRedo) {
        vwr.actionStates.add(0, sb.toString());
        vwr.actionStatesRedo.clear();
      } else {
        vwr.actionStatesRedo.add(1, sb.toString());
      }
      if (vwr.actionStates.size() == MAX_ACTION_UNDO) {
        vwr.actionStates.removeItemAt(MAX_ACTION_UNDO - 1);
      }
    }
    undoWorking = !clearRedo;
  }

  private boolean undoWorking = false;
  private final static int MAX_ACTION_UNDO = 100;
  

}
