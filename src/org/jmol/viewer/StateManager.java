 /* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
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


import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.jmol.api.JmolSceneGenerator;
import org.jmol.java.BS;
import org.jmol.modelset.Bond;
import org.jmol.modelset.ModelSet;
import org.jmol.modelset.Orientation;
import org.jmol.script.SV;
import org.jmol.script.T;

import org.jmol.util.BSUtil;
import org.jmol.util.Elements;

import javajs.J2SIgnoreImport;
import javajs.util.SB;

import java.util.Arrays;
import java.util.Map.Entry;

@J2SIgnoreImport({Runtime.class})
public class StateManager {

  /* steps in adding a global variable:
   
   In Viewer:
   
   1. add a check in setIntProperty or setBooleanProperty or setFloat.. or setString...
   2. create new set/get methods
   
   In StateManager
   
   3. create the global.xxx variable
   4. in registerParameter() register it so that it shows up as having a value in math
   
   */

  public final static int OBJ_BACKGROUND = 0;
  public final static int OBJ_AXIS1 = 1;
  public final static int OBJ_AXIS2 = 2;
  public final static int OBJ_AXIS3 = 3;
  public final static int OBJ_BOUNDBOX = 4;
  public final static int OBJ_UNITCELL = 5;
  public final static int OBJ_FRANK = 6;
  public final static int OBJ_MAX = 7;
  private final static String objectNameList = "background axis1      axis2      axis3      boundbox   unitcell   frank      ";

  public static String getVariableList(Map<String, SV> htVariables, int nMax,
                                       boolean withSites, boolean definedOnly) {
    SB sb = new SB();
    // user variables only:
    int n = 0;

    String[] list = new String[htVariables.size()];
    for (Map.Entry<String, SV> entry : htVariables.entrySet()) {
      String key = entry.getKey();
      SV var = entry.getValue();
      if ((withSites || !key.startsWith("site_")) && (!definedOnly || key.charAt(0) == '@'))
        list[n++] = key
            + (key.charAt(0) == '@' ? " " + var.asString() : " = "
                + varClip(key, var.escape(), nMax));
    }
    Arrays.sort(list, 0, n);
    for (int i = 0; i < n; i++)
      if (list[i] != null)
        sb.append("  ").append(list[i]).append(";\n");
    if (n == 0 && !definedOnly)
      sb.append("# --no global user variables defined--;\n");
    return sb.toString();
  }
  
  public static int getObjectIdFromName(String name) {
    if (name == null)
      return -1;
    int objID = objectNameList.indexOf(name.toLowerCase());
    return (objID < 0 ? objID : objID / 11);
  }

  static String getObjectNameFromId(int objId) {
    if (objId < 0 || objId >= OBJ_MAX)
      return null;
    return objectNameList.substring(objId * 11, objId * 11 + 11).trim();
  }

  protected final Viewer vwr;
  protected Map<String, Object> saved = new Hashtable<String, Object>();
  
  private String lastOrientation = "";
  private String lastContext = "";
  private String lastConnections = "";
  private String lastScene = "";
  private String lastSelected = "";
  private String lastState = "";
  private String lastShape = "";
  private String lastCoordinates = "";

  StateManager(Viewer vwr) {
    this.vwr = vwr;
  }
  
  void clear(GlobalSettings global) {
    vwr.setShowAxes(false);
    vwr.setShowBbcage(false);
    vwr.setShowUnitCell(false);
    global.clear();
  }

  /**
   * Reset lighting to Jmol defaults
   * 
   */
  public void resetLighting() {
    vwr.setIntProperty("ambientPercent",  45);
    vwr.setIntProperty("celShadingPower", 10);      
    vwr.setIntProperty("diffusePercent",  84);
    vwr.setIntProperty("phongExponent",   64);
    vwr.setIntProperty("specularExponent", 6); // log2 of phongExponent
    vwr.setIntProperty("specularPercent", 22);
    vwr.setIntProperty("specularPower",   40);
    vwr.setIntProperty("zDepth",           0);
    vwr.setIntProperty("zShadePower",      3);
    vwr.setIntProperty("zSlab",           50);
    
    vwr.setBooleanProperty("specular",    true);
    vwr.setBooleanProperty("celShading", false);
    vwr.setBooleanProperty("zshade",     false);
  }

  void setCrystallographicDefaults() {
    //axes on and mode unitCell; unitCell on; perspective depth off;
    vwr.setAxesMode(T.axesunitcell);
    vwr.setShowAxes(true);
    vwr.setShowUnitCell(true);
    vwr.setBooleanProperty("perspectiveDepth", false);
  }

  private void setCommonDefaults() {
    vwr.setBooleanProperty("perspectiveDepth", true);
    vwr.setFloatProperty("bondTolerance",
        JC.DEFAULT_BOND_TOLERANCE);
    vwr.setFloatProperty("minBondDistance",
        JC.DEFAULT_MIN_BOND_DISTANCE);
    vwr.setIntProperty("bondingVersion", Elements.RAD_COV_IONIC_OB1_100_1);
    vwr.setBooleanProperty("translucent", true);
  }

  void setJmolDefaults() {
    setCommonDefaults();
    vwr.setStringProperty("defaultColorScheme", "Jmol");
    vwr.setBooleanProperty("axesOrientationRasmol", false);
    vwr.setBooleanProperty("zeroBasedXyzRasmol", false);
    vwr.setIntProperty("percentVdwAtom",
        JC.DEFAULT_PERCENT_VDW_ATOM);
    vwr.setIntProperty("bondRadiusMilliAngstroms",
        JC.DEFAULT_BOND_MILLIANGSTROM_RADIUS);
    vwr.setVdwStr("auto");
  }

  void setRasMolDefaults() {
    setCommonDefaults();
    vwr.setStringProperty("defaultColorScheme", "RasMol");
    vwr.setBooleanProperty("axesOrientationRasmol", true);
    vwr.setBooleanProperty("zeroBasedXyzRasmol", true);
    vwr.setIntProperty("percentVdwAtom", 0);
    vwr.setIntProperty("bondRadiusMilliAngstroms", 1);
    vwr.setVdwStr("Rasmol");
  }

  void setPyMOLDefaults() {
    setCommonDefaults();
    vwr.setStringProperty("measurementUnits", "ANGSTROMS");
    vwr.setBooleanProperty("zoomHeight", true);
  }

  private static Object getNoCase(Map<String, Object> saved, String name) {
    for (Entry<String, Object> e : saved.entrySet())
      if (e.getKey().equalsIgnoreCase(name))
        return e.getValue();
   return null;
  }

  public String listSavedStates() {
    String names = "";
    for (String name: saved.keySet())
      names += "\n" + name;
    return names;
  }

  private void deleteSavedType(String type) {
    Iterator<String> e = saved.keySet().iterator();
    while (e.hasNext())
      if (e.next().startsWith(type))
        e.remove();
  }

  public void deleteSaved(String namelike) {
    Iterator<String> e = saved.keySet().iterator();
    while (e.hasNext()) {
      String name = e.next();
      if (name.startsWith(namelike) || name.endsWith("_" + namelike)
          && name.indexOf("_") == name.lastIndexOf("_" + namelike))
        e.remove();
    }
  }
  
  public void saveSelection(String saveName, BS bsSelected) {
    if (saveName.equalsIgnoreCase("DELETE")) {
      deleteSavedType("Selected_");
      return;
    }
    saveName = lastSelected = "Selected_" + saveName;
    saved.put(saveName, BSUtil.copy(bsSelected));
  }

  public boolean restoreSelection(String saveName) {
    String name = (saveName.length() > 0 ? "Selected_" + saveName
        : lastSelected);
    BS bsSelected = (BS) getNoCase(saved, name);
    if (bsSelected == null) {
      vwr.select(new BS(), false, 0, false);
      return false;
    }
    vwr.select(bsSelected, false, 0, false);
    return true;
  }

  public void saveState(String saveName) {
    if (saveName.equalsIgnoreCase("DELETE")) {
      deleteSavedType("State_");
      return;
    }
    saveName = lastState = "State_" + saveName;
    saved.put(saveName, vwr.getStateInfo());
  }

  public String getSavedState(String saveName) {
    String name = (saveName.length() > 0 ? "State_" + saveName : lastState);
    String script = (String) getNoCase(saved, name);
    return (script == null ? "" : script);
  }

  /*  
   boolean restoreState(String saveName) {
   //not used -- more efficient just to run the script 
   String name = (saveName.length() > 0 ? "State_" + saveName
   : lastState);
   String script = (String) getNoCase(saved, name);
   if (script == null)
   return false;
   vwr.script(script + CommandHistory.NOHISTORYATALL_FLAG);
   return true;
   }
   */
  public void saveStructure(String saveName) {
    if (saveName.equalsIgnoreCase("DELETE")) {
      deleteSavedType("Shape_");
      return;
    }
    saveName = lastShape = "Shape_" + saveName;
    saved.put(saveName, vwr.getStructureState());
  }

  public String getSavedStructure(String saveName) {
    String name = (saveName.length() > 0 ? "Shape_" + saveName : lastShape);
    String script = (String) getNoCase(saved, name);
    return (script == null ? "" : script);
  }

  public void saveCoordinates(String saveName, BS bsSelected) {
    if (saveName.equalsIgnoreCase("DELETE")) {
      deleteSavedType("Coordinates_");
      return;
    }
    saveName = lastCoordinates = "Coordinates_" + saveName;
    saved.put(saveName, vwr.getCoordinateState(bsSelected));
  }

  public String getSavedCoordinates(String saveName) {
    String name = (saveName.length() > 0 ? "Coordinates_" + saveName
        : lastCoordinates);
    String script = (String) getNoCase(saved, name);
    return (script == null ? "" : script);
  }

  Orientation getOrientation() {
    return new Orientation(vwr, false, null);
  }

  String getSavedOrientationText(String saveName) {
    Orientation o;
    if (saveName != null) {
      o = getOrientationFor(saveName);
      return (o == null ? "" : o.getMoveToText(true));
    }
    SB sb = new SB();
    for (Entry<String, Object> e : saved.entrySet()) {
      String name = e.getKey();
      if (name.startsWith("Orientation_"))
        sb.append(((Orientation) e.getValue()).getMoveToText(true));
    }
    return sb.toString();
  }

  public void saveScene(String saveName, Map<String, Object> scene) {
    if (saveName.equalsIgnoreCase("DELETE")) {
      deleteSavedType("Scene_");
      return;
    }
    Scene o = new Scene(scene);
    o.saveName = lastScene = "Scene_" + saveName;
    saved.put(o.saveName, o);
  }

  public boolean restoreScene(String saveName, float timeSeconds) {
    Scene o = (Scene) getNoCase(saved, (saveName.length() > 0 ? "Scene_"
        + saveName : lastScene));
    return (o != null && o.restore(vwr, timeSeconds));
  }

  public void saveOrientation(String saveName, float[] pymolView) {
    if (saveName.equalsIgnoreCase("DELETE")) {
      deleteSavedType("Orientation_");
      return;
    }
    Orientation o = new Orientation(vwr, saveName.equalsIgnoreCase("default"), pymolView);
    o.saveName = lastOrientation = "Orientation_" + saveName;
    saved.put(o.saveName, o);
  }
  
  public boolean restoreOrientation(String saveName, float timeSeconds, boolean isAll) {
    Orientation o = getOrientationFor(saveName);
    return (o != null && o.restore(timeSeconds, isAll));
  }

  private Orientation getOrientationFor(String saveName) {
    String name = (saveName.length() > 0 ? "Orientation_" + saveName
        : lastOrientation);    
    return (Orientation) getNoCase(saved, name);
  }

  public void saveContext(String saveName, Object context) {
    if (saveName.equalsIgnoreCase("DELETE")) {
      deleteSavedType("Context_");
      return;
    }
    saved.put((lastContext = "Context_" + saveName), context);
  }

  public Object getContext(String saveName) {
    return saved.get(saveName.length() == 0 ? lastContext : "Context_" + saveName);
  }
  
  public void saveBonds(String saveName) {
    if (saveName.equalsIgnoreCase("DELETE")) {
      deleteSavedType("Bonds_");
      return;
    }
    Connections b = new Connections(vwr);
    b.saveName = lastConnections = "Bonds_" + saveName;
    saved.put(b.saveName, b);
  }

  public boolean restoreBonds(String saveName) {
    vwr.clearModelDependentObjects();
    String name = (saveName.length() > 0 ? "Bonds_" + saveName
        : lastConnections);
    Connections c = (Connections) getNoCase(saved, name);
    return (c != null && c.restore());
  }

  public static String varClip(String name, String sv, int nMax) {
    if (nMax > 0 && sv.length() > nMax)
      sv = sv.substring(0, nMax) + " #...more (" + sv.length()
          + " bytes -- use SHOW " + name + " or MESSAGE @" + name
          + " to view)";
    return sv;
  }
}

class Scene {
  protected String  saveName;
  private Map<String, Object> scene;
  
  protected Scene(Map<String, Object> scene) {
    this.scene = scene;
  }

  protected boolean restore(Viewer vwr, float timeSeconds) {
    JmolSceneGenerator gen = (JmolSceneGenerator) scene.get("generator");
    if (gen != null)
      gen.generateScene(scene);
    float[] pv = (float[]) scene.get("pymolView");
    return (pv != null && vwr.tm.moveToPyMOL(vwr.eval, timeSeconds, pv));
  }
}

class Connections {

  protected String saveName;
  protected int bondCount;
  protected Connection[] connections;
  private Viewer vwr;

  protected Connections(Viewer vwr) {
    ModelSet modelSet = vwr.ms;
    if (modelSet == null)
      return;
    this.vwr = vwr;
    bondCount = modelSet.bondCount;
    connections = new Connection[bondCount + 1];
    Bond[] bonds = modelSet.bo;
    for (int i = bondCount; --i >= 0;) {
      Bond b = bonds[i];
      connections[i] = new Connection(b.atom1.i, b.atom2.i, b
          .mad, b.colix, b.order, b.getEnergy(), b.shapeVisibilityFlags);
    }
  }

  protected boolean restore() {
    ModelSet modelSet = vwr.ms;
    if (modelSet == null)
      return false;
    modelSet.deleteAllBonds();
    for (int i = bondCount; --i >= 0;) {
      Connection c = connections[i];
      int ac = modelSet.ac;
      if (c.atomIndex1 >= ac || c.atomIndex2 >= ac)
        continue;
      Bond b = modelSet.bondAtoms(modelSet.at[c.atomIndex1],
          modelSet.at[c.atomIndex2], c.order, c.mad, null, c.energy, false, true);
      b.colix = c.colix;
      b.shapeVisibilityFlags = c.shapeVisibilityFlags;
    }
    for (int i = modelSet.bondCount; --i >= 0;)
        modelSet.bo[i].index = i;
    vwr.setShapeProperty(JC.SHAPE_STICKS, "reportAll", null);
    return true;
  }
}

class Connection {
  protected int atomIndex1;
  protected int atomIndex2;
  protected short mad;
  protected short colix;
  protected int order;
  protected float energy;
  protected int shapeVisibilityFlags;

  protected Connection(int atom1, int atom2, short mad, short colix, int order, float energy,
      int shapeVisibilityFlags) {
    atomIndex1 = atom1;
    atomIndex2 = atom2;
    this.mad = mad;
    this.colix = colix;
    this.order = order;
    this.energy = energy;
    this.shapeVisibilityFlags = shapeVisibilityFlags;
  }
}


