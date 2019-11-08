package org.jmol.adapter.readers.pymol;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolSceneGenerator;
import org.jmol.api.PymolAtomReader;
import org.jmol.atomdata.RadiusData;
import org.jmol.c.VDW;

import javajs.util.BS;
import org.jmol.modelset.Bond;
import org.jmol.modelset.MeasurementData;
import org.jmol.modelset.Text;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Escape;
import org.jmol.util.Font;
import org.jmol.util.Point3fi;

import javajs.util.AU;
import javajs.util.CU;
import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;

import org.jmol.util.Logger;
import javajs.util.P3;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

/**
 * A class to allow manipulation of scenes dissociated from file loading. A
 * "scene" in this context is a distillation of PyMOL information into a
 * Hashtable for easier retrieval using RESTORE SCENE name.
 * 
 */
class PyMOLScene implements JmolSceneGenerator {
  
//accounted for:
//
//  cartoon_color
//  dot_color
//  ellipsoid_color
//  ellipsoid_transparency
//  ellipsoid_scale
//  label_color
//  label_position
//  mesh_color
//  nonbonded_transparency
//  ribbon_color
//  sphere_color
//  sphere_scale
//  sphere_transparency
//  surface_color
//
//TODO:
//    
//  transparency (surface)
//
//special (probably) PyMOL-only:
//
//  ladder_color
//  ladder_mode
//  ladder_radius
//  ring_color
//  ring_mode
//  ring_radius
//  ring_width

  private Viewer vwr;
  private int pymolVersion;

  // filled by PyMOLReader; used to generate the scene
  BS bsHidden = new BS();
  BS bsNucleic = new BS();
  BS bsNonbonded = new BS();
  BS bsLabeled = new BS();
  BS bsHydrogen = new BS();
  BS bsNoSurface = new BS();

  // private -- only needed for file reading

  private Map<Float, BS> htSpacefill = new Hashtable<Float, BS>();
  private Map<String, BS> ssMapAtom = new Hashtable<String, BS>();
  private Lst<Integer> atomColorList = new Lst<Integer>();
  private Map<String, Boolean> occludedObjects = new Hashtable<String, Boolean>();
  private Map<Integer, Text> labels = new Hashtable<Integer, Text>();
  private short[] colixes;
  private JmolObject frameObj;
  private Map<String, PyMOLGroup> groups;
  private Map<Integer, Lst<Object>> objectSettings;

  private void clearReaderData() {
    reader = null;
    colixes = null;
    atomColorList = null;
    objectSettings = null;
    stateSettings = null;
    if (haveScenes)
      return;
    settings = null;
    groups = null;
    labels = null;
    ssMapAtom = null;
    htSpacefill = null;
    htAtomMap = null;
    htMeasures = null;
    htObjectGroups = null;
    htObjectAtoms = null;
    htObjectSettings = null;
    htStateSettings = null;
    htHiddenObjects = null;
    objectInfo = null;
    occludedObjects = null;
    bsHidden = bsNucleic = bsNonbonded = bsLabeled = bsHydrogen = bsNoSurface 
        = bsCartoon = null;
  }

  // private -- needed for processing Scenes

  private BS bsCartoon = new BS();
  private Map<String, BS> htCarveSets = new Hashtable<String, BS>();
  private Map<String, BS> htDefinedAtoms = new Hashtable<String, BS>();
  private Map<String, Boolean> htHiddenObjects = new Hashtable<String, Boolean>();
  private Lst<String> moleculeNames = new Lst<String>();
  private Lst<JmolObject> jmolObjects = new Lst<JmolObject>();
  private Map<String, int[]> htAtomMap = new Hashtable<String, int[]>();
  private Map<String, BS> htObjectAtoms = new Hashtable<String, BS>();
  private Map<String, String> htObjectGroups = new Hashtable<String, String>();
  private Map<String, MeasurementData[]> htMeasures = new Hashtable<String, MeasurementData[]>();
  private Map<String, Map<Integer, Lst<Object>>> htObjectSettings = new Hashtable<String, Map<Integer, Lst<Object>>>();
  private Map<String, Object[]> objectInfo = new Hashtable<String, Object[]>();
  private Lst<Object> settings;
  private Map<String, Map<Integer, Lst<Object>>> htStateSettings = new Hashtable<String, Map<Integer, Lst<Object>>>();
  private Map<Integer, Lst<Object>>  stateSettings;
  private Map<Integer, Lst<Object>> uniqueSettings;
  private Map<Integer, Integer> uniqueList;
  private BS bsUniqueBonds;
  void setUniqueBond(int index, int uniqueID) {
    if (uniqueID < 0)
      return;
    if (uniqueList == null) {
      uniqueList = new Hashtable<Integer, Integer>();
      bsUniqueBonds = new BS();
    }
    uniqueList.put(Integer.valueOf(index), Integer.valueOf(uniqueID));
    bsUniqueBonds.set(index);
  }
  private int bgRgb;
  private int dotColor;
  private int surfaceMode;
  private int surfaceColor;
  private int cartoonColor;
  private int ribbonColor;
  private int sphereColor;
  private int labelFontId;
  private int labelColor;
  private float cartoonTranslucency;
  private float ribbonTranslucency;
  private float labelSize;
  private float meshWidth;
  private float nonbondedSize;
  private float nonbondedTranslucency;
  private float sphereScale;
  private float sphereTranslucency;
  private float stickTranslucency;
  private float transparency;
  private boolean cartoonLadderMode;
  private boolean cartoonRockets;
  private boolean haveNucleicLadder;
  private P3 labelPosition;
  private P3 labelPosition0 = new P3();

  private String objectName;
  private String objectNameID;
  private String objectJmolName;
  private int objectType;
  private BS bsAtoms;
  private boolean objectHidden;

  // during file loading we have a reader, but after that we must rely on data saved by the server

  private PymolAtomReader reader;
  private int[] uniqueIDs;
  private int[] cartoonTypes;
  private int[] sequenceNumbers;
  private boolean[] newChain;
  private float[] radii;

  private int baseModelIndex;
  private int baseAtomIndex;
  private int stateCount;

  String mepList = "";

  boolean doCache;
  private boolean haveScenes;
  
  private BS bsCarve;
  private boolean solventAccessible;
  private BS bsLineBonds = new BS();
  private BS bsStickBonds = new BS();
  private int thisState;
  int currentAtomSetIndex;
  String surfaceInfoName;

  void setStateCount(int stateCount) {
    this.stateCount = stateCount;
  }

  PyMOLScene(PymolAtomReader reader, Viewer vwr, Lst<Object> settings,
      Map<Integer, Lst<Object>> uniqueSettings, int pymolVersion,
      boolean haveScenes, int baseAtomIndex, int baseModelIndex,
      boolean doCache, String filePath) {
    this.reader = reader;
    this.vwr = vwr;
    this.settings = settings;
    this.uniqueSettings = uniqueSettings;
    this.pymolVersion = pymolVersion;
    this.haveScenes = haveScenes;
    this.baseAtomIndex = baseAtomIndex;
    this.baseModelIndex = baseModelIndex;
    this.doCache = doCache;
    this.surfaceInfoName = filePath + "##JmolSurfaceInfo##";
    setVersionSettings();
    settings.trimToSize();
    bgRgb = colorSetting(PyMOL.bg_rgb);
    labelPosition0 = pointSetting(PyMOL.label_position);
  }

  @SuppressWarnings("unchecked")
  private int colorSetting(int i) {
    Lst<Object> pos = PyMOLReader.listAt(settings, i);
    Object o = (pos == null || pos.size() != 3 ? null : pos.get(2));
    if (o == null)
      return (int) PyMOL.getDefaultSetting(i, pymolVersion);
    return (o instanceof Integer ? ((Integer) o).intValue() : CU
        .colorPtToFFRGB(PyMOLReader.pointAt((Lst<Object>) o, 0, ptTemp)));
  }
  
  @SuppressWarnings("unchecked")
  private P3 pointSetting(int i) {
    P3 pt = new P3();
    Lst<Object> pos = PyMOLReader.listAt(settings, i);
    if (pos != null && pos.size() == 3) 
      return PyMOLReader.pointAt((Lst<Object>) pos.get(2), 0, pt);
    return PyMOL.getDefaultSettingPt(i, pymolVersion, pt); 
  }

  void ensureCapacity(int n) {
    atomColorList.ensureCapacity(atomColorList.size() + n);
  }

  void setReaderObjectInfo(String name, int type, String groupName, boolean isHidden,
                     Lst<Object> listObjSettings,
                     Lst<Object> listStateSettings, String ext) {
    objectName = name;
    objectHidden = isHidden;
    objectNameID = (objectName == null ? null : fixName(objectName
        + ext));
    objectSettings = new Hashtable<Integer, Lst<Object>>();
    stateSettings = new Hashtable<Integer, Lst<Object>>();
    if (objectName != null) {
      objectJmolName = getJmolName(name);
      if (groupName != null) {
        htObjectGroups.put(objectName, groupName);
        htObjectGroups.put(objectNameID, groupName);
      }
      objectInfo.put(objectName, new Object[] { objectNameID,
          Integer.valueOf(type) });
      if (htObjectSettings.get(objectName) == null) {
        listToSettings(listObjSettings, objectSettings);
        htObjectSettings.put(objectName, objectSettings);
      }
      if (htStateSettings.get(objectNameID) == null) {
        listToSettings(listStateSettings, stateSettings);
        htStateSettings.put(objectNameID, stateSettings);
      }
    }
    getObjectSettings();
  }

  @SuppressWarnings("unchecked")
  private void listToSettings(Lst<Object> list,
                              Map<Integer, Lst<Object>> objectSettings) {
    if (list != null && list.size() != 0) {
      for (int i = list.size(); --i >= 0;) {
        Lst<Object> setting = (Lst<Object>) list.get(i);
        objectSettings.put((Integer) setting.get(0), setting);
      }
    }
  }

  private void getObjectSettings() {
    transparency = floatSetting(PyMOL.transparency);
    dotColor = (int) floatSetting(PyMOL.dot_color);
    nonbondedSize = floatSetting(PyMOL.nonbonded_size);
    nonbondedTranslucency = floatSetting(PyMOL.nonbonded_transparency);
    sphereScale = floatSetting(PyMOL.sphere_scale);
    cartoonColor = (int) floatSetting(PyMOL.cartoon_color);
    ribbonColor = (int) floatSetting(PyMOL.ribbon_color);
    sphereColor = (int) floatSetting(PyMOL.sphere_color);
    cartoonTranslucency = floatSetting(PyMOL.cartoon_transparency);
    ribbonTranslucency = floatSetting(PyMOL.ribbon_transparency);
    stickTranslucency = floatSetting(PyMOL.stick_transparency);
    sphereTranslucency = floatSetting(PyMOL.sphere_transparency);
    cartoonLadderMode = booleanSetting(PyMOL.cartoon_ladder_mode);
    cartoonRockets = booleanSetting(PyMOL.cartoon_cylindrical_helices);
    surfaceMode = (int) floatSetting(PyMOL.surface_mode);
    surfaceColor = (int) floatSetting(PyMOL.surface_color);
    solventAccessible = booleanSetting(PyMOL.surface_solvent);
    meshWidth = floatSetting(PyMOL.mesh_width);
    String carveSet = stringSetting(PyMOL.surface_carve_selection).trim();
    if (carveSet.length() == 0) {
      bsCarve = null;
    } else {
      bsCarve = htCarveSets.get(carveSet);
      if (bsCarve == null)
        htCarveSets.put(carveSet, bsCarve = new BS());      
    }

    //solventAsSpheres = getBooleanSetting(PyMOL.sphere_solvent); - this is for SA-Surfaces
    labelPosition = new P3();
    try {
      Lst<Object> setting = getObjectSetting(PyMOL.label_position);
      PyMOLReader.pointAt(PyMOLReader.listAt(setting, 2), 0, labelPosition);
    } catch (Exception e) {
      // no problem.
    }
    labelPosition.add(labelPosition0);
    labelColor = (int) floatSetting(PyMOL.label_color);
    labelSize = floatSetting(PyMOL.label_size);
    labelFontId = (int) floatSetting(PyMOL.label_font_id);
  }

  void setAtomInfo(int[] uniqueIDs, int[] cartoonTypes, int[] sequenceNumbers,
                   boolean newChain[], float radii[]) {
    this.uniqueIDs = uniqueIDs;
    this.cartoonTypes = cartoonTypes;
    this.sequenceNumbers = sequenceNumbers;
    this.newChain = newChain;
    this.radii = radii;
  }
  
  ////////////// scene-related methods //////////
  
//  From module/pymol/Viewing.py: 
//  
//  DESCRIPTION
//
//  "scene" makes it possible to save and restore multiple scenes
//  within a single session.  A scene consists of the view, all object
//  activity information, all atom-wise visibility, color,
//  representations, and the global frame index.

  
  /**
   * Set scene object/state-specific global fields and 
   * settings based on the name and state or stored values
   * from when the file was loaded.
   *  
   * @param name 
   * @param istate 
   *  
   */
  private void setSceneObject(String name, int istate) {
    objectName = name;
    objectType = getObjectType(name);
    objectJmolName = getJmolName(name);
    objectNameID = (istate == 0 && objectType != 0 ? getObjectID(name) : objectJmolName + "_"
        + istate);
    bsAtoms = htObjectAtoms.get(name);
    objectSettings = htObjectSettings.get(name);
    stateSettings = htStateSettings.get(name+"_" + istate);
    String groupName = htObjectGroups.get(name);
    objectHidden = (htHiddenObjects.containsKey(name) || groupName != null
        && !groups.get(groupName).visible);
    getObjectSettings();
  }

  /**
   * Build a scene at file reading time. We only implement frame-specific
   * scenes. Creates a map of information that can be used later and
   * will also be a reference to this instance of PyMOLScene, which is an
   * implementation of JmolSceneGenerator.
   * 
   * @param name
   * @param thisScene
   * @param htObjNames
   * @param htSecrets
   */
  @SuppressWarnings("unchecked")
  void buildScene(String name, Lst<Object> thisScene,
                  Map<String, Lst<Object>> htObjNames,
                  Map<String, Lst<Object>> htSecrets) {
    Object frame = thisScene.get(2);
    Map<String, Object> smap = new Hashtable<String, Object>();
    smap.put("pymolFrame", frame);
    smap.put("generator", this);
    smap.put("name", name);
    Lst<Object> view = PyMOLReader.listAt(thisScene, 0);
    if (view != null)
      smap.put("pymolView", getPymolView(view, false));

    // get the overall object visibilities:
    //   {name : [ visFlag, repOn, objVis, color ], ...}
    // As far as I can tell, repOn is not useful, and objVis
    // is only used for measurements.

    Map<String, Object> visibilities = (Map<String, Object>) thisScene.get(1);
    smap.put("visibilities", visibilities);

    // get all subtypes from names (_lines, _sticks, etc.)
    String sname = "_scene_" + name + "_";
    Object[] reps = new Object[PyMOL.REP_LIST.length];
    for (int j = PyMOL.REP_LIST.length; --j >= 0;) {
      Lst<Object> list = htObjNames.get(sname + PyMOL.REP_LIST[j]);
      Lst<Object> data = PyMOLReader.listAt(list, 5);
      if (data != null && data.size() > 0)
        reps[j] = PyMOLReader.listToMap(data);
    }
    smap.put("moleculeReps", reps);

    // there's no real point in getting 
    // get all colors from selector_secrets
    sname = "_!c_" + name + "_";
    Lst<Object> colorection = PyMOLReader.listAt(thisScene, 3);
    int n = colorection.size();
    n -= n % 2;
    // [color/selEntry,color/selEntry,color/selEntry.....]
    // [3, 262,        0, 263,        4, 264,       .....]
    // see layer3/Selector.c SelectorColorectionApply
    Object[] colors = new Object[n / 2];
    for (int j = 0, i = 0; j < n; j += 2) {
      int color = PyMOLReader.intAt(colorection, j);
      Lst<Object> c = htSecrets.get(sname + color);
      if (c != null && c.size() > 1)
        colors[i++] = new Object[] { Integer.valueOf(color), c.get(1) };
    }
    smap.put("colors", colors);
    addJmolObject(T.scene, null, smap).jmolName = name;
  }

  /**
   * Generate the saved scene using file settings preserved here and 
   * scene-specific information including frame, view, colors, visibilities,
   * . Called by StateManager via implemented JmolSceneGenerator.
   * 
   * @param scene
   * 
   */
  @Override
  @SuppressWarnings("unchecked")
  public void generateScene(Map<String, Object> scene) {
    Logger.info("PyMOLScene - generateScene " + scene.get("name"));
    // generateVisibities();
    jmolObjects.clear();
    bsHidden.clearAll();
    occludedObjects.clear();
    htHiddenObjects.clear();
    Integer frame = (Integer) scene.get("pymolFrame");
    thisState  = frame.intValue();
    addJmolObject(T.frame, null, Integer.valueOf(thisState - 1));
    try {
      generateVisibilities((Map<String, Object>) scene.get("visibilities"));
      generateColors((Object[]) scene.get("colors"));
      generateShapes((Object[]) scene.get("moleculeReps"));
      finalizeVisibility();
      offsetObjects();
      finalizeObjects();
    } catch (Exception e) {
      Logger.info("PyMOLScene exception " + e);
        e.printStackTrace();
    }
  }

  /**
   * Set PyMOL "atom-wise" colors -- the colors that are defined
   * initially as element colors but possibly set with the PyMOL 'color'
   * command and are used when representation colors (cartoon, dots, etc.)
   * are not defined (-1). This is the same as Jmol's inherited atom color.
   *  
   * @param colors
   */
  @SuppressWarnings("unchecked")
  private void generateColors(Object[] colors) {
    if (colors == null)
      return;
    // note that colors are for ALL STATES
    for (int i = colors.length; --i >= 0;) {
      Object[] item = (Object[]) colors[i];
      int color = ((Integer) item[0]).intValue();
      int icolor = PyMOL.getRGB(color);
      Lst<Object> molecules = (Lst<Object>) item[1];
      BS bs = getSelectionAtoms(molecules, thisState, new BS());
      addJmolObject(T.atoms, bs, null).argb = icolor;
    }
  }

  /**
   * process the selection sets (sele), (...)
   * 
   * @param selection
   */
  void processSelection(Lst<Object> selection) {
    String id = PyMOLReader.stringAt(selection, 0);
    id = "_" + (id.equals("sele") ? id : "sele_" + id); 
    PyMOLGroup g = getGroup(id);
    getSelectionAtoms(PyMOLReader.listAt(selection, 5), 0, g.bsAtoms);
  }

  /**
   * Add selected atoms to a growing bit set.
   * 
   * @param molecules
   * @param istate
   * @param bs
   * @return bs for convenience
   */
  private BS getSelectionAtoms(Lst<Object> molecules, int istate, BS bs) {
    if (molecules != null)
      for (int j = molecules.size(); --j >= 0;)
        selectAllAtoms(PyMOLReader.listAt(molecules, j), istate, bs);
    return bs;
  }

  /**
   * Collect all the atoms specified by an object state into a bit set.
   * 
   * @param obj
   * @param istate  0 for "all states"
   * @param bs
   */
  private void selectAllAtoms(Lst<Object> obj, int istate, BS bs) {
    String name = PyMOLReader.stringAt(obj, 0);
    setSceneObject(name, istate);
    Lst<Object> atomList = PyMOLReader.listAt(obj, 1);
    int k0 = (istate == 0 ? 1 : istate);
    int k1 = (istate == 0 ? stateCount : istate);
    for (int k = k0; k <= k1; k++) {
      int[] atomMap = htAtomMap.get(fixName(name + "_" + k));
      if (atomMap == null)
        continue;
      getBsAtoms(atomList, atomMap, bs);
    }
  }

  /**
   * Hide everything, then just make visible the sets of 
   * atoms specified in the visibility (i.e. "activity") list within scene_dict.
   * 
   * @param vis
   */
  @SuppressWarnings("unchecked")
  private void generateVisibilities(Map<String, Object> vis) {
    if (vis == null)
      return;
    BS bs = new BS();
    addJmolObject(T.hide, null, null);
    for (Entry<String, PyMOLGroup> e : groups.entrySet())
      e.getValue().visible = true;
    for (Entry<String, Object> e : vis.entrySet()) {
      String name = e.getKey();
      if (name.equals("all"))
        continue;
      Lst<Object> list = (Lst<Object>) e.getValue();
      int tok = (PyMOLReader.intAt(list, 0) == 1 ? T.display : T.hide);
      if (tok == T.hide)
        htHiddenObjects.put(name, Boolean.TRUE);
      switch (getObjectType(name)) {
      case PyMOL.OBJECT_GROUP:
        PyMOLGroup g = groups.get(name);
        if (g != null)
          g.visible = (tok == T.display);
        break;
      }
    }
    setGroupVisibilities();
    for (Entry<String, Object> e : vis.entrySet()) {
      String name = e.getKey();
      if (name.equals("all"))
        continue;
      setSceneObject(name, thisState);
      if (objectHidden)
        continue;
      Lst<Object> list = (Lst<Object>) e.getValue();
      int tok = (objectHidden ? T.hide : T.display);
      bs = null;
      String info = objectJmolName;
      switch (objectType) {
      case 0: // doesn't have selected state
      case PyMOL.OBJECT_GROUP:
        continue;
      case PyMOL.OBJECT_MOLECULE:
        bs = vwr.getDefinedAtomSet(info);
        if (bs.nextSetBit(0) < 0)
          continue;
        break;
      case PyMOL.OBJECT_MEASURE:
        if (tok == T.display) {
          MeasurementData[] mdList = htMeasures.get(name);
          if (mdList != null)
            addMeasurements(mdList, mdList[0].points.size(), null,
                getBS(PyMOLReader.listAt(list, 2)), PyMOLReader.intAt(list, 3), null, true);
        }
        info += "_*";
        break;
      case PyMOL.OBJECT_CGO:
      case PyMOL.OBJECT_MAPMESH:
      case PyMOL.OBJECT_MAPDATA:
        // might need to set color here for these?
        break;
      }
      addJmolObject(tok, bs, info);
    }
  }

  /**
   * Create all Jmol shape objects. 
   * 
   * @param reps
   */
  @SuppressWarnings("unchecked")
  private void generateShapes(Object[] reps) {
    if (reps == null)
      return;
    addJmolObject(T.restrict, null, null).argb = thisState - 1;
    // through all molecules...
    //    for (int m = moleculeNames.size(); --m >= 0;) {
    for (int m = 0; m < moleculeNames.size(); m++) {
      setSceneObject(moleculeNames.get(m), thisState);
      if (objectHidden)
        continue;
      BS[] molReps = new BS[PyMOL.REP_JMOL_MAX];
      for (int i = 0; i < PyMOL.REP_JMOL_MAX; i++)
        molReps[i] = new BS();
      // through all representations...
      for (int i = reps.length; --i >= 0;) {
        Map<String, Lst<Object>> repMap = (Map<String, Lst<Object>>) reps[i];
        Lst<Object> list = (repMap == null ? null : repMap.get(objectName));
        if (list != null)
          selectAllAtoms(list, thisState, molReps[i]);
      }
      createShapeObjects(molReps, true, -1, -1);
    }
  }

  private BS getBS(Lst<Object> list) {
    BS bs = new BS();
    for (int i = list.size(); --i >= 0;)
      bs.set(PyMOLReader.intAt(list, i));
    return bs;
  }

  private void getBsAtoms(Lst<Object> list, int[] atomMap, BS bs) {
    for (int i = list.size(); --i >= 0;)
      bs.set(atomMap[PyMOLReader.intAt(list, i)]);
  }

  private final P3 ptTemp = new P3();

  void setReaderObjects() {
    clearReaderData();
    finalizeObjects();
    if (!haveScenes) {
      uniqueSettings = null;
      bsUniqueBonds = bsStickBonds = bsLineBonds = null;
    }
  }

  /**
   * Finally, we turn each JmolObject into its Jmol equivalent.
   */
  private void finalizeObjects() {
    vwr.setStringProperty("defaults", "PyMOL");
    for (int i = 0; i < jmolObjects.size(); i++) {
      try {
        JmolObject obj = jmolObjects.get(i);
        obj.finalizeObject(this, vwr.ms, mepList, doCache);
      } catch (Exception e) {
        System.out.println(e);
          e.printStackTrace();
      }
    }
    finalizeUniqueBonds();
    jmolObjects.clear();
  }
  
  void offsetObjects() {
    for (int i = 0, n = jmolObjects.size(); i < n; i++)
      jmolObjects.get(i).offset(baseModelIndex, baseAtomIndex);
  }

  private JmolObject getJmolObject(int id, BS bsAtoms, Object info) {
    if (baseAtomIndex > 0)
      bsAtoms = BSUtil.copy(bsAtoms);
    return new JmolObject(id, objectNameID, bsAtoms, info);
  }

  private JmolObject addJmolObject(int id, BS bsAtoms, Object info) {
    return addObject(getJmolObject(id, bsAtoms, info));
  }

  /**
   * adds depth_cue, fog, and fog_start
   * 
   * @param view
   * @param isViewObj
   * @return 22-element array
   */
  private float[] getPymolView(Lst<Object> view, boolean isViewObj) {
    float[] pymolView = new float[21];
    boolean depthCue = booleanSetting(PyMOL.depth_cue); // 84
    boolean fog = booleanSetting(PyMOL.fog); // 88
    float fog_start = floatSetting(PyMOL.fog_start); // 192

    int pt = 0;
    int i = 0;
    // x-axis
    for (int j = 0; j < 3; j++)
      pymolView[pt++] = PyMOLReader.floatAt(view, i++);
    if (isViewObj)
      i++;
    // y-axis
    for (int j = 0; j < 3; j++)
      pymolView[pt++] = PyMOLReader.floatAt(view, i++);
    if (isViewObj)
      i++;
    // z-axis (not used)
    for (int j = 0; j < 3; j++)
      pymolView[pt++] = PyMOLReader.floatAt(view, i++);
    if (isViewObj)
      i += 5;
    // xTrans, yTrans, -distanceToCenter, center(x,y,z), distanceToSlab, distanceToDepth
    for (int j = 0; j < 8; j++)
      pymolView[pt++] = PyMOLReader.floatAt(view, i++);

    boolean isOrtho = booleanSetting(PyMOL.orthoscopic); // 23
    float fov = floatSetting(PyMOL.field_of_view); // 152

    pymolView[pt++] = (isOrtho ? fov : -fov);
    pymolView[pt++] = (depthCue ? 1 : 0);
    pymolView[pt++] = (fog ? 1 : 0);
    pymolView[pt++] = fog_start;
    return pymolView;
  }

  float globalSetting(int i) {
    Lst<Object> setting = PyMOLReader.listAt(settings, i);
    if (setting != null && setting.size() == 3)
      return ((Number) setting.get(2)).floatValue();
    return PyMOL.getDefaultSetting(i, pymolVersion);
  }

  /**
   * Create a hierarchical list of named groups as generally seen on the PyMOL
   * app's right-hand object menu.
   * 
   * @param object
   * @param parent
   * @param type
   * @param bsAtoms
   * @return group
   */

  PyMOLGroup addGroup(Lst<Object> object, String parent, int type, BS bsAtoms) {
    if (groups == null)
      groups = new Hashtable<String, PyMOLGroup>();
    PyMOLGroup myGroup = getGroup(objectName);
    myGroup.object = object;
    myGroup.objectNameID = objectNameID;
    myGroup.visible = !objectHidden;
    myGroup.type = type;
    if (!myGroup.visible) {
      occludedObjects.put(objectNameID, Boolean.TRUE);
      htHiddenObjects.put(objectName, Boolean.TRUE);
    }
    if (parent != null && parent.length() != 0)
      getGroup(parent).addList(myGroup);
    if (bsAtoms != null)
      myGroup.addGroupAtoms(bsAtoms);
    return myGroup;
  }

  PyMOLGroup getGroup(String name) {
    PyMOLGroup g = groups.get(name);
    if (g == null) {
      groups.put(name, (g = new PyMOLGroup(name)));
      defineAtoms(name, g.bsAtoms);
    }
    return g;
  }

  /**
   * Create group JmolObjects, and set hierarchical visibilities
   */
  void finalizeVisibility() {
    setGroupVisibilities();
    if (groups != null)
      for (int i = jmolObjects.size(); --i >= 0;) {
        JmolObject obj = jmolObjects.get(i);
        if (obj.jmolName != null
            && occludedObjects.containsKey(obj.jmolName))
          obj.visible = false;
      }
    if (!bsHidden.isEmpty())
      addJmolObject(T.hidden, bsHidden, null);
  }

  void setCarveSets(Map<String, Lst<Object>> htObjNames) {
    if (htCarveSets.isEmpty())
      return;
    for (Entry<String, BS> e: htCarveSets.entrySet())
      getSelectionAtoms(PyMOLReader.listAt(htObjNames.get(e.getKey()), 5), 0, e.getValue());
  }

  private void setGroupVisibilities() {
    if (groups == null)
      return;
    Collection<PyMOLGroup> list = groups.values();
    BS bsAll = new BS();
    for (PyMOLGroup g : list) {
      bsAll.or(g.bsAtoms);
      if (g.parent == null) // top
        setGroupVisible(g, true);
      else if (g.list.isEmpty()) // bottom
        g.addGroupAtoms(new BS());
    }
    defineAtoms("all", bsAll);
  }

  private void defineAtoms(String name, BS bs) {
    htDefinedAtoms.put(getJmolName(name), bs);
  }

  private String getJmolName(String name) {
    return "__" + fixName(name);
  }

  /**
   * create all objects for a given molecule or scene
   * @param reps
   * @param allowSurface
   * @param ac0     > 0 for a molecule; -1 for a scene
   * @param ac
   */
  void createShapeObjects(BS[] reps, boolean allowSurface, int ac0,
                          int ac) {
    if (ac >= 0) {
      // initial creation, not just going to this scene
      bsAtoms = BSUtil.newBitSet2(ac0, ac);
      JmolObject jo;
      // from reader
      jo = addJmolObject(T.atoms, bsAtoms, null);
      colixes = AU.ensureLengthShort(colixes, ac);
      for (int i = ac; --i >= ac0;)
        colixes[i] = (short) atomColorList.get(i).intValue();
      jo.setColors(colixes, 0);
      jo.setSize(0);
      jo = addJmolObject(JC.SHAPE_STICKS, bsAtoms, null);
      jo.setSize(0);
    }
    createShapeObject(PyMOL.REP_LINES, reps[PyMOL.REP_LINES]);
    createShapeObject(PyMOL.REP_STICKS, reps[PyMOL.REP_STICKS]);
    fixReps(reps);
    createSpacefillObjects();
    for (int i = 0; i < PyMOL.REP_JMOL_MAX; i++)
      switch (i) {
      case PyMOL.REP_LINES:
      case PyMOL.REP_STICKS:
        continue;
      case PyMOL.REP_MESH:
      case PyMOL.REP_SURFACE:
        // surfaces depend upon global flags
        if (!allowSurface)
          continue;

        //    #define cRepSurface_by_flags       0
        //    #define cRepSurface_all            1
        //    #define cRepSurface_heavy_atoms    2
        //    #define cRepSurface_vis_only       3
        //    #define cRepSurface_vis_heavy_only 4

        switch (surfaceMode) {
        case 0:
          reps[i].andNot(bsNoSurface);
          break;
        case 1:
        case 3:
          break;
        case 2:
        case 4:
          reps[i].andNot(bsHydrogen);
          break;
        }
        //$FALL-THROUGH$
      default:
        createShapeObject(i, reps[i]);
        continue;
      }
    bsAtoms = null;
  }

  void addLabel(int atomIndex, int uniqueID, int atomColor,
                float[] labelPos, String label) {
    int icolor = (int) getUniqueFloatDef(uniqueID, PyMOL.label_color,
        labelColor);
    if (icolor == PyMOL.COLOR_BACK || icolor == PyMOL.COLOR_FRONT) {
      // deal with this later
    } else if (icolor < 0) {
      icolor = atomColor;
    }
    if (labelPos == null) {
      P3 offset = getUniquePoint(uniqueID, PyMOL.label_position, null);
      if (offset == null)
        offset = labelPosition;
      else
        offset.add(labelPosition);
      setLabelPosition(offset, labelPos);
    }
    labels.put(Integer.valueOf(atomIndex), newTextLabel(label, labelPos,
        icolor, labelFontId, labelSize));
  }

  float getUniqueFloatDef(int id, int key, float defaultValue) {
    Lst<Object> setting;
    if (id <= 0
        || (setting = uniqueSettings.get(Integer.valueOf((id << 10) + key))) == null)
      return defaultValue;
    float v = ((Number) setting.get(2)).floatValue();
    if (Logger.debugging)
      Logger.debug("Pymol unique setting for " + id + ": [" + key + "] = " + v);
    return v;
  }

  @SuppressWarnings("unchecked")
  P3 getUniquePoint(int id, int key, P3 pt) {
    Lst<Object> setting;
    if (id <= 0
        || (setting = uniqueSettings.get(Integer.valueOf((id << 10) + key))) == null)
      return pt;
    pt = new P3();
    PyMOLReader.pointAt((Lst<Object>) setting.get(2), 0, pt);
    Logger.info("Pymol unique setting for " + id + ": " + key + " = " + pt);
    return pt;
  }

  Lst<Object> getObjectSetting(int i) {
    // why label_position only?
    return objectSettings.get(Integer.valueOf(i));
  }

  boolean booleanSetting(int i) {
    return (floatSetting(i) != 0);
  }

  float floatSetting(int i) {
    Lst<Object> setting = getSetting(i);
    if (setting != null && setting.size() == 3)
      return ((Number) setting.get(2)).floatValue();
    return PyMOL.getDefaultSetting(i, pymolVersion);
  }

  String stringSetting(int i) {
      Lst<Object> setting = getSetting(i);
      if (setting != null && setting.size() == 3)
        return PyMOLReader.stringAt(setting, 2);
      return PyMOL.getDefaultSettingS(i, pymolVersion);
  }

  @SuppressWarnings("unchecked")
  private Lst<Object> getSetting(int i) {
    Lst<Object> setting = null;
    if (stateSettings != null)
      setting = stateSettings.get(Integer.valueOf(i));
    if (setting == null && objectSettings != null)
      setting = objectSettings.get(Integer.valueOf(i));
    if (setting == null && i < settings.size())
      setting = (Lst<Object>) settings.get(i);
    return setting;
  }

  float[] setLabelPosition(P3 offset, float[] labelPos) {
    labelPos[0] = 1;
    labelPos[1] = offset.x;
    labelPos[2] = offset.y;
    labelPos[3] = offset.z;
    return labelPos;
  }

  String addCGO(Lst<Object> data, int color) {
    data.addLast(objectName);
    JmolObject jo = addJmolObject(JC.SHAPE_CGO, null, data);
    jo.argb = color;
    jo.translucency = floatSetting(PyMOL.cgo_transparency);
    return fixName(objectName);
  }
  
  boolean addMeasurements(MeasurementData[] mdList, int nCoord,
                          Lst<Object> list, BS bsReps, int color,
                          Lst<Object> offsets, boolean haveLabels) {
    boolean isNew = (mdList == null);
    int n = (isNew ? list.size() / 3 / nCoord : mdList.length);
    if (n == 0)
      return false;
    boolean drawLabel = haveLabels && bsReps.get(PyMOL.REP_LABELS);
    boolean drawDashes = bsReps.get(PyMOL.REP_DASHES);
    float rad = floatSetting(PyMOL.dash_width) / 20;
    if (rad == 0)
      rad = 0.05f;
    if (!drawDashes)
      rad = -0.0005f;
    if (color < 0)
      color = (int) floatSetting(PyMOL.dash_color);
    int c = PyMOL.getRGB(color);
    short colix = C.getColix(c);
    int clabel = (labelColor < 0 ? color : labelColor);
    if (isNew) {
      mdList = new MeasurementData[n];
      htMeasures.put(objectName, mdList);
    }
    BS bs = BSUtil.newAndSetBit(0);
    for (int index = 0, p = 0; index < n; index++) {
      MeasurementData md;
      float[] offset;
      if (isNew) {
        Lst<Object> points = new Lst<Object>();
        for (int i = 0; i < nCoord; i++, p += 3)
          points.addLast(PyMOLReader.pointAt(list, p, new Point3fi()));
        offset = PyMOLReader.floatsAt(PyMOLReader.listAt(offsets, index), 0, new float[7], 7);
        if (offset == null)
          offset = setLabelPosition(labelPosition, new float[7]);
        md = mdList[index] = vwr.newMeasurementData(objectNameID + "_"
            + (index + 1), points);
        md.note = objectName;
      } else {
        md = mdList[index];
        offset = md.text.pymolOffset;
      }
      int nDigits = (int) floatSetting(MEAS_DIGITS[nCoord - 2]);
      String strFormat = nCoord + ": "
          + (drawLabel ? "%0." + (nDigits < 0 ? 1 : nDigits) + "VALUE" : "");
      //strFormat += " -- " + objectNameID + " " + floatSetting(PyMOL.surface_color) + " " + Integer.toHexString(c);
      Text text = newTextLabel(strFormat, offset, clabel,
          (int) floatSetting(PyMOL.label_font_id),
          floatSetting(PyMOL.label_size));
      md.set(T.define, null, null, strFormat, "angstroms", null, false, false, null,
          false, (int) (rad * 2000), colix, text);
      addJmolObject(JC.SHAPE_MEASURES, bs, md);
    }
    return true;
  }

  SB getViewScript(Lst<Object> view) {
    SB sb = new SB();
    float[] pymolView = getPymolView(view, true);
    sb.append(";set translucent " + (globalSetting(PyMOL.transparency_mode) != 2) 
        + ";set zshadePower 1;set traceAlpha "
        + (globalSetting(PyMOL.cartoon_round_helices) != 0));
    boolean rockets = cartoonRockets;
    sb.append(";set cartoonRockets " + rockets);
    if (rockets)
      sb.append(";set rocketBarrels " + rockets);
    sb.append(";set cartoonLadders " + haveNucleicLadder);
    sb.append(";set ribbonBorder "
        + (globalSetting(PyMOL.cartoon_fancy_helices) != 0));
    sb.append(";set cartoonFancy "
        + (globalSetting(PyMOL.cartoon_fancy_helices) == 0));
    String s = "000000" + Integer.toHexString(bgRgb & 0xFFFFFF);
    s = "[x" + s.substring(s.length() - 6) + "]";
    sb.append(";background " + s);
    sb.append(";moveto 0 PyMOL " + Escape.eAF(pymolView));
    sb.append(";save orientation 'default';"); // DO NOT set antialiasDisplay here! It can cause immediate rendering problem in Java
    return sb;
  }

  short getColix(int colorIndex, float translucency) {
    short colix = (colorIndex == PyMOL.COLOR_BACK 
        ? (C.getBgContrast(bgRgb) == C.WHITE ? C.BLACK : C.WHITE)
        : colorIndex == PyMOL.COLOR_FRONT ? C.getBgContrast(bgRgb) : 
          C.getColixO(Integer.valueOf(PyMOL.getRGB(colorIndex))));

    return C.getColixTranslucent3(colix, translucency > 0, translucency);
  }

  void setAtomColor(int atomColor) {
    atomColorList.addLast(Integer.valueOf(getColix(atomColor, 0)));
  }

  void setFrameObject(int type, Object info) {
    if (info != null) {
      frameObj = getJmolObject(type, null, info);
      return;
    }
    if (frameObj == null)
      return;
    frameObj.finalizeObject(this, vwr.ms, null, false);
    frameObj = null;
  }

  private String fixName(String name) {
    char[] chars = name.toLowerCase().toCharArray();
    for (int i = chars.length; --i >= 0;)
      if (!PT.isLetterOrDigit(chars[i]))
        chars[i] = '_';
    return String.valueOf(chars); 
  }

  String getObjectID(String name) {
    return (String) objectInfo.get(name)[0];
  }

  private int getObjectType(String name) {
    Object[] o = objectInfo.get(name);
    return (o == null ? 0 : ((Integer) o[1]).intValue());
  }

  BS setAtomMap(int[] atomMap, int ac0) {
    htAtomMap.put(objectNameID, atomMap);
    BS bsAtoms = htDefinedAtoms.get(objectJmolName);
    if (bsAtoms == null) {
      bsAtoms = BS.newN(ac0 + atomMap.length);
      Logger.info("PyMOL molecule " + objectName);
      htDefinedAtoms.put(objectJmolName, bsAtoms);
      htObjectAtoms.put(objectName, bsAtoms);
      moleculeNames.addLast(objectName);
    }
    return bsAtoms;
  }

  private final static int[] MEAS_DIGITS = { PyMOL.label_distance_digits,
      PyMOL.label_angle_digits, PyMOL.label_dihedral_digits };

  private Text newTextLabel(String label, float[] labelOffset, int colorIndex,
                            int fontID, float fontSize) {
    // 0 GLUT 8x13 
    // 1 GLUT 9x15 
    // 2 GLUT Helvetica10 
    // 3 GLUT Helvetica12 
    // 4 GLUT Helvetica18 
    // 5 DejaVuSans
    // 6 DejaVuSans_Oblique
    // 7 DejaVuSans_Bold
    // 8 DejaVuSans_BoldOblique
    // 9 DejaVuSerif
    // 10 DejaVuSerif_Bold
    // 11 DejaVuSansMono
    // 12 DejaVuSansMono_Oblique
    // 13 DejaVuSansMono_Bold
    // 14 DejaVuSansMono_BoldOblique
    // 15 GenR102
    // 16 GenI102
    // 17 DejaVuSerif_Oblique
    // 18 DejaVuSerif_BoldOblique

    String face;
    float factor = 1f;
    switch (fontID) {
    default:
    case 11:
    case 12:
    case 13:
    case 14:
      // 11-14: Jmol doesn't support sansserif mono -- just using SansSerif here
      face = "SansSerif";
      break;
    case 0:
    case 1:
      face = "Monospaced";
      break;
    case 9:
    case 10:
    case 15:
    case 16:
    case 17:
    case 18:
      face = "Serif";
      break;
    }
    String style;
    switch (fontID) {
    default:
      style = "Plain";
      break;
    case 6:
    case 12:
    case 16:
    case 17:
      style = "Italic";
      break;
    case 7:
    case 10:
    case 13:
      style = "Bold";
      break;
    case 8:
    case 14:
    case 18:
      style = "BoldItalic";
      break;
    }
    Font font = vwr.getFont3D(face, style, fontSize == 0 ? 12 : fontSize
        * factor);
    Text t = Text.newLabel(vwr, font, label, getColix(
        colorIndex, 0), (short) 0, 0, 0);
    if (t != null)
      t.pymolOffset = labelOffset;
    return t;
  }

  /**
   * Attempt to adjust for PyMOL versions. See PyMOL layer3.Executive.c
   * 
   */
  private void setVersionSettings() {
    if (pymolVersion < 100) {
      addSetting(PyMOL.movie_fps, 2, Integer.valueOf(0));
      addSetting(PyMOL.label_digits, 2, Integer.valueOf(2));
      addSetting(PyMOL.label_position, 4, new double[] { 1, 1, 0 });
      if (pymolVersion < 99) {
        addSetting(PyMOL.cartoon_ladder_mode, 2, Integer.valueOf(0));
        addSetting(PyMOL.cartoon_tube_cap, 2, Integer.valueOf(0));
        addSetting(PyMOL.cartoon_nucleic_acid_mode, 2, Integer.valueOf(1));
      }
    }
  }

  private void addSetting(int key, int type, Object val) {
    int settingCount = settings.size();
    if (settingCount <= key)
      for (int i = key + 1; --i >= settingCount;)
        settings.addLast(null);
    if (type == 4) {
      double[] d = (double[]) val;
      Lst<Object> list;
      val = list = new Lst<Object>();
      for (int i = 0; i < 3; i++)
        list.addLast(Double.valueOf(d[i]));
    }
    Lst<Object> setting = new Lst<Object>();
    setting.addLast(Integer.valueOf(key));
    setting.addLast(Integer.valueOf(type));
    setting.addLast(val);
    settings.set(key, setting);
  }

  private void fixReps(BS[] reps) {
    htSpacefill.clear();
    bsCartoon.clearAll();
    for (int iAtom = bsAtoms.nextSetBit(0); iAtom >= 0; iAtom = bsAtoms
        .nextSetBit(iAtom + 1)) {
      float rad = 0;
      int uniqueID = (reader == null ? uniqueIDs[iAtom] : reader
          .getUniqueID(iAtom));
      if (reps[PyMOL.REP_SPHERES].get(iAtom)) {
        rad = (reader == null ? radii[iAtom] : reader.getVDW(iAtom))
            * getUniqueFloatDef(uniqueID, PyMOL.sphere_scale, sphereScale);
      } else if (reps[PyMOL.REP_NBSPHERES].get(iAtom)) {
        // Penta_vs_mutants calcium
        rad = nonbondedSize;
      }
      if (rad != 0) {
        Float r = Float.valueOf(rad);
        BS bsr = htSpacefill.get(r);
        if (bsr == null)
          htSpacefill.put(r, bsr = new BS());
        bsr.set(iAtom);
      }
      int cartoonType = (reader == null ? cartoonTypes[iAtom] : reader
          .getCartoonType(iAtom));
      if (reps[PyMOL.REP_CARTOON].get(iAtom)) {
        /*
              -1 => { type=>'skip',       converted=>undef },
               0 => { type=>'automatic',  converted=>1 },
               1 => { type=>'loop',       converted=>1 },
               2 => { type=>'rectangle',  converted=>undef },
               3 => { type=>'oval',       converted=>undef },
               4 => { type=>'tube',       converted=>1 },
               5 => { type=>'arrow',      converted=>undef },
               6 => { type=>'dumbbell',   converted=>undef },
               7 => { type=>'putty',      converted=>1 },

         */

        // 0, 2, 3, 5, 6 are not treated in any special way

        switch (cartoonType) {
        case 1:
        case 4:
          reps[PyMOL.REP_JMOL_TRACE].set(iAtom);
          //$FALL-THROUGH$
        case -1:
          reps[PyMOL.REP_CARTOON].clear(iAtom);
          bsCartoon.clear(iAtom);
          break;
        case 7:
          reps[PyMOL.REP_JMOL_PUTTY].set(iAtom);
          reps[PyMOL.REP_CARTOON].clear(iAtom);
          bsCartoon.clear(iAtom);
          break;
        default:
          bsCartoon.set(iAtom);
        }
      }
    }

    reps[PyMOL.REP_CARTOON].and(bsCartoon);
    cleanSingletons(reps[PyMOL.REP_CARTOON]);
    cleanSingletons(reps[PyMOL.REP_RIBBON]);
    cleanSingletons(reps[PyMOL.REP_JMOL_TRACE]);
    cleanSingletons(reps[PyMOL.REP_JMOL_PUTTY]);
    bsCartoon.and(reps[PyMOL.REP_CARTOON]);
  }

  /**
   * PyMOL does not display cartoons or traces for single-residue runs. This
   * two-pass routine first sets bits in a residue bitset, then it clears out
   * all singletons, and in a second pass all atom bits for not-represented
   * residues are cleared.
   * 
   * @param bs
   */
  private void cleanSingletons(BS bs) {
    if (bs.isEmpty())
      return;
    bs.and(bsAtoms);
    BS bsr = new BS();
    int n = bs.length();
    int pass = 0;
    while (true) {
      for (int i = 0, offset = 0, iPrev = Integer.MIN_VALUE, iSeqLast = Integer.MIN_VALUE, iSeq = Integer.MIN_VALUE; i < n; i++) {
        if (iPrev < 0
            || (reader == null ? newChain[i] : reader.compareAtoms(iPrev, i)))
          offset++;
        iSeq = (reader == null ? sequenceNumbers[i] : reader
            .getSequenceNumber(i));
        if (iSeq != iSeqLast) {
          iSeqLast = iSeq;
          offset++;
        }
        if (pass == 0) {
          if (bs.get(i))
            bsr.set(offset);
        } else if (!bsr.get(offset))
          bs.clear(i);
        iPrev = i;
      }
      if (++pass == 2)
        break;
      BS bsnot = new BS();
      for (int i = bsr.nextSetBit(0); i >= 0; i = bsr.nextSetBit(i + 1))
        if (!bsr.get(i - 1) && !bsr.get(i + 1))
          bsnot.set(i);
      bsr.andNot(bsnot);
    }
  }

  /**
   * Create JmolObjects for each shape.
   * 
   * Note that LINES and STICKS are done initially, then all the others are
   * processed.
   * 
   * @param shapeID
   * @param bs
   */
  private void createShapeObject(int shapeID, BS bs) {
    // add more to implement
    if (bs.isEmpty())
      return;
    JmolObject jo = null;
    switch (shapeID) {
    case PyMOL.REP_NONBONDED: // stars
      bs.and(bsNonbonded);
      if (bs.isEmpty())
        return;
      setUniqueObjects(JC.SHAPE_STARS, bs, 0, 0, PyMOL.nonbonded_transparency, nonbondedTranslucency, 0, nonbondedSize, 0.5f);
      break;
    case PyMOL.REP_NBSPHERES:
    case PyMOL.REP_SPHERES:
      setUniqueObjects(JC.SHAPE_BALLS, bs, PyMOL.sphere_color, sphereColor, PyMOL.sphere_transparency, sphereTranslucency, PyMOL.sphere_scale, sphereScale, 1);
      break;
    case PyMOL.REP_ELLIPSOID:
      float ellipsoidTranslucency = floatSetting(PyMOL.ellipsoid_transparency);
      int ellipsoidColor = (int) floatSetting(PyMOL.ellipsoid_color);
      float ellipsoidScale = floatSetting(PyMOL.ellipsoid_scale);
      setUniqueObjects(JC.SHAPE_ELLIPSOIDS, bs, PyMOL.ellipsoid_color, ellipsoidColor, PyMOL.ellipsoid_transparency, ellipsoidTranslucency, PyMOL.ellipsoid_scale, ellipsoidScale, 50);
      break;
    case PyMOL.REP_DOTS:
      setUniqueObjects(JC.SHAPE_DOTS, bs, PyMOL.dot_color, dotColor, 0, 0, PyMOL.sphere_scale, sphereScale, 1);
      break;
    case PyMOL.REP_SURFACE: //   = 2;
      // unique translucency here involves creating ghost surfaces 
      float withinDistance = floatSetting(PyMOL.surface_carve_cutoff);
      jo = addJmolObject(T.isosurface, bs, new Object[] {
          booleanSetting(PyMOL.two_sided_lighting) ? "FULLYLIT" : "FRONTLIT",
          (surfaceMode == 3 || surfaceMode == 4) ? " only" : "", 
              bsCarve, Float.valueOf(withinDistance)});
      jo.setSize(floatSetting(PyMOL.solvent_radius) * (solventAccessible ? -1 : 1));
      jo.translucency = transparency;
      if (surfaceColor >= 0)
        jo.argb = PyMOL.getRGB(surfaceColor);
      jo.modelIndex = currentAtomSetIndex;
      jo.cacheID = surfaceInfoName;
      setUniqueObjects(JC.SHAPE_ISOSURFACE, bs, PyMOL.surface_color, surfaceColor, PyMOL.transparency, transparency, 0, 0, 0);
      break;
    case PyMOL.REP_MESH: //   = 8;
      jo = addJmolObject(T.isosurface, bs, null);
      jo.setSize(floatSetting(PyMOL.solvent_radius));
      jo.translucency = transparency;
      setUniqueObjects(JC.SHAPE_ISOSURFACE, bs, PyMOL.surface_color, surfaceColor, PyMOL.transparency, transparency, 0, 0, 0);      
      break;
    case PyMOL.REP_LABELS: //   = 3;
      bs.and(bsLabeled);
      if (bs.isEmpty())
        return;
      jo = addJmolObject(JC.SHAPE_LABELS, bs, labels);
      break;
    case PyMOL.REP_DASHES:
      // TODO
    case PyMOL.REP_LINES:
      jo = addJmolObject(T.wireframe, bs, null);
      jo.setSize(floatSetting(PyMOL.line_width) / 15);
      int  color = (int) floatSetting(PyMOL.line_color);
      if (color >= 0)
        jo.argb = PyMOL.getRGB(color);
      break;
    case PyMOL.REP_STICKS:
      jo = addJmolObject(JC.SHAPE_STICKS, bs, null);
      jo.setSize(floatSetting(PyMOL.stick_radius) * 2);
      jo.translucency = stickTranslucency;
      int  col = (int) floatSetting(PyMOL.stick_color);
      if (col >= 0)
        jo.argb = PyMOL.getRGB(col);
      break;
    case PyMOL.REP_CARTOON:
      createCartoonObject("H", (cartoonRockets ? PyMOL.cartoon_helix_radius
          : PyMOL.cartoon_oval_length));
      createCartoonObject("S", PyMOL.cartoon_rect_length);
      createCartoonObject("L", PyMOL.cartoon_loop_radius);
      createCartoonObject(" ", PyMOL.cartoon_loop_radius);
      break;
    case PyMOL.REP_JMOL_PUTTY:
      createPuttyObject(bs);
      break;
    case PyMOL.REP_JMOL_TRACE:
      createTraceObject(bs);
      break;
    case PyMOL.REP_RIBBON: // backbone or trace, depending
      createRibbonObject(bs);
      break;
    default:
      Logger.error("Unprocessed representation type " + shapeID);
    }
  }

  private JmolObject setUniqueObjects(int shape, BS bs, int setColor,
                                      int color, int setTrans, float trans,
                                      int setSize, float size, float f) {
    int n = bs.cardinality();
    short[] colixes = (setColor == 0 ? null : new short[n]);
    float[] atrans = (setTrans == 0 ? null : new float[n]);
    float[] sizes = new float[n];
    for (int pt = 0, i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1), pt++) {
      int id = (reader == null ? uniqueIDs[i] : reader.getUniqueID(i));
      if (colixes != null) {
        int c = (int) getUniqueFloatDef(id, setColor, color);
        if (c > 0)
          colixes[pt] = getColix(c, 0);
      }
      if (atrans != null) {
        atrans[pt] = getUniqueFloatDef(id, setTrans, trans);
      }
      sizes[pt] = getUniqueFloatDef(id, setSize, size) * f;
    }
    return addJmolObject(shape, bs, new Object[] { colixes, atrans, sizes });

    //      case PyMOL.REP_DOTS:
    //      addJmolObject(JC.SHAPE_DOTS, bs, null).rd = new RadiusData(null,
    //        value1, RadiusData.EnumType.FACTOR, EnumVdw.AUTO);
    //      case PyMOL.REP_NONBONDED:
    //      addJmolObject(JC.SHAPE_STARS, bs, null).rd = new RadiusData(null,
    //        f / 2, RadiusData.EnumType.FACTOR, EnumVdw.AUTO);
  }

  /**
   * Create a BALLS JmolObject for each radius.
   * 
   */
  private void createSpacefillObjects() {
    for (Map.Entry<Float, BS> e : htSpacefill.entrySet()) {
      float r = e.getKey().floatValue();
      BS bs = e.getValue();
      addJmolObject(T.atoms, bs, null).rd = new RadiusData(null, r,
          RadiusData.EnumType.ABSOLUTE, VDW.AUTO);
    }
    htSpacefill.clear();
  }

  /**
   * trace, or cartoon in the case of cartoon ladders.
   * 
   * @param bs
   */
  private void createTraceObject(BS bs) {
    checkNucleicObject(bs, true);
    if (bs.isEmpty())
      return;
    float r = floatSetting(PyMOL.cartoon_tube_radius);
    JmolObject jo = setUniqueObjects(JC.SHAPE_TRACE, bs,
        PyMOL.cartoon_color, cartoonColor, 0, 0, 0, 0, 0);
    jo.setSize(r * 2);
    jo.translucency = cartoonTranslucency;
  }

  private void checkNucleicObject(BS bs, boolean isTrace) {
    JmolObject jo;
    BS bsNuc = BSUtil.copy(bsNucleic);
    bsNuc.and(bs);
    if (!bsNuc.isEmpty()) {
      if (isTrace && cartoonLadderMode)
        haveNucleicLadder = true;
      // we will just use cartoons for ladder mode
      jo = addJmolObject(JC.SHAPE_CARTOON, bsNuc, null);
      jo.translucency = cartoonTranslucency;
      jo.setSize(floatSetting(PyMOL.cartoon_tube_radius) * 2);
      bs.andNot(bsNuc);
    }
  }

  /**
   * "Putty" shapes scaled in a variety of ways.
   * 
   * @param bs
   */
  private void createPuttyObject(BS bs) {
    float[] info = new float[] { floatSetting(PyMOL.cartoon_putty_quality),
        floatSetting(PyMOL.cartoon_putty_radius),
        floatSetting(PyMOL.cartoon_putty_range),
        floatSetting(PyMOL.cartoon_putty_scale_min),
        floatSetting(PyMOL.cartoon_putty_scale_max),
        floatSetting(PyMOL.cartoon_putty_scale_power),
        floatSetting(PyMOL.cartoon_putty_transform) };
    addJmolObject(T.trace, bs, info).translucency = cartoonTranslucency;
  }

  /**
   * PyMOL "ribbons" could be Jmol backbone or trace, depending upon the value
   * of PyMOL.ribbon_sampling.
   * 
   * @param bs
   */
  private void createRibbonObject(BS bs) {
    // 2ace: 0, 1 ==> backbone  // r rpc w 0.0 1.3 3.0 too small
    // fig8: 0, 1 ==> backbone // r rpc w 0.0 1.0 3.0  OK
    // casp: 0, 1 ==> backbone // r rpc w 0.0 1.3 3.0  too small
    // NLG3_AchE: 0, 1 ==> backbone  //r rpc w 0.0 1.3 4.0 too small 
    // NLG3_HuAChE: 0, 10 ==> trace
    // tach: 0, 10 ==> trace
    // tah-lev: 0, 10 ==> trace
    // 496: -1, 1 ==> backbone  // r rpc 0.0 1.3 3.0 too small
    // kinases: -1, 1 ==> backbone
    // 443_D1: -1, 1 ==> backbone
    // 476Rainbow_New: 10, 8 ==> trace

    //float smoothing = getFloatSetting(PyMOL.ribbon_smooth);
    boolean isTrace = (floatSetting(PyMOL.ribbon_sampling) > 1);
    float r = floatSetting(PyMOL.ribbon_radius) * 2;
    float rayScale = floatSetting(PyMOL.ray_pixel_scale);
    if (r == 0)
      r = floatSetting(PyMOL.ribbon_width)
          * (isTrace ? 1 : (rayScale <= 1 ? 0.5f : rayScale)) * 0.1f;
    JmolObject jo = setUniqueObjects((isTrace ? JC.SHAPE_TRACE : JC.SHAPE_BACKBONE), bs,
        PyMOL.ribbon_color, ribbonColor, 0, 0, 0, 0, 0);
    jo.setSize(r);
    jo.translucency = ribbonTranslucency;
  }

  private void createCartoonObject(String key, int sizeID) {
    BS bs = BSUtil.copy(ssMapAtom.get(key));
    if (bs == null)
      return;
    bs.and(bsCartoon);
    if (bs.isEmpty())
      return;
    if (key.equals(" ")) {
      checkNucleicObject(bs, false);
      if (bs.isEmpty())
        return;
    }
    JmolObject jo = setUniqueObjects(JC.SHAPE_CARTOON, bs, PyMOL.cartoon_color, cartoonColor,
        0, 0, 0, 0, 0);
    jo.setSize(floatSetting(sizeID) * 2);
    jo.translucency = cartoonTranslucency;
  }

  private JmolObject addObject(JmolObject obj) {
    jmolObjects.addLast(obj);
    return obj;
  }

  /**
   * Iterate through groups, setting visibility flags.
   * 
   * @param g
   * @param parentVis
   */
  private void setGroupVisible(PyMOLGroup g, boolean parentVis) {
    boolean vis = parentVis && g.visible;
    if (vis)
      return;
    g.visible = false;
    occludedObjects.put(g.objectNameID, Boolean.TRUE);
    htHiddenObjects.put(g.name, Boolean.TRUE);
    switch (g.type) {
    case PyMOL.OBJECT_MOLECULE:
      bsHidden.or(g.bsAtoms);
      break;
    default:
      // a group?
      g.occluded = true;
      break;
    }
    for (PyMOLGroup gg : g.list.values()) {
      setGroupVisible(gg, vis);
    }
  }

  BS getSSMapAtom(String ssType) {
    BS bs = ssMapAtom.get(ssType);
    if (bs == null)
      ssMapAtom.put(ssType, bs = new BS());
    return bs;
  }

  Map<String, Object> setAtomDefs() {
    setGroupVisibilities();
    Map<String, Object> defs = new Hashtable<String, Object>();
    for (Entry<String, BS> e: htDefinedAtoms.entrySet()) {
      BS bs = e.getValue();
      if (!bs.isEmpty())
        defs.put(e.getKey(), bs);
    }
    addJmolObject(T.define, null, defs);
    return defs;
  }

  boolean needSelections() {
    return haveScenes || !htCarveSets.isEmpty();
  }

  void setUniqueBonds(BS bsBonds, boolean isSticks) {
    if (isSticks) {
      bsStickBonds.or(bsBonds);
      bsStickBonds.andNot(bsLineBonds);
    } else {
      bsLineBonds.or(bsBonds);
      bsLineBonds.andNot(bsStickBonds);
    }
  }

  private void finalizeUniqueBonds() {
    if (uniqueList == null)
      return;
    int bondCount = vwr.ms.bondCount;
    Bond[] bonds = vwr.ms.bo;
    for (int i = bsUniqueBonds.nextSetBit(0); i >= 0; i = bsUniqueBonds.nextSetBit(i + 1)) {
      float rad = Float.NaN;
      int id = uniqueList.get(Integer.valueOf(i)).intValue();
      if (bsLineBonds.get(i)) {
        rad = getUniqueFloatDef(id, PyMOL.line_width, Float.NaN) / 30;
      } else if (bsStickBonds.get(i)) {
        rad = getUniqueFloatDef(id, PyMOL.stick_radius, Float.NaN);
      }
      int c = (int) getUniqueFloatDef(id, PyMOL.stick_color, Integer.MAX_VALUE);
      if (c != Integer.MAX_VALUE)
        c =  PyMOL.getRGB(c);
      float valence = getUniqueFloatDef(id, PyMOL.valence, Float.NaN);
      float t = getUniqueFloatDef(id, PyMOL.stick_transparency, Float.NaN);
      if (i < 0 || i >= bondCount)
        return;
      Bond b = bonds[i];
      setBondParameters(b, thisState - 1, rad, valence, c, t);
    }
  }

  /**
   * used in PyMOL reader to set unique bond settings and for valence
   * 
   * @param modelIndex
   * @param b
   * @param rad
   * @param pymolValence  1 for "show multiple bonds"
   * @param argb
   * @param trans
   */
  void setBondParameters(Bond b, int modelIndex, float rad, float pymolValence,
                             int argb, float trans) {
    if (modelIndex >= 0 && b.atom1.mi != modelIndex)
      return; 
    if (!Float.isNaN(rad))
      b.mad = (short) (rad * 2000);
    short colix = b.colix;
    if (argb != Integer.MAX_VALUE)
      colix = C.getColix(argb);
    if (!Float.isNaN(trans))
      b.colix = C.getColixTranslucent3(colix, trans != 0, trans);
    else if (b.colix != colix)
      b.colix = C.copyColixTranslucency(b.colix, colix);
    if (pymolValence == 1)
      b.order |= JmolAdapter.ORDER_PYMOL_MULT;
    else if (pymolValence == 0)
      b.order |= JmolAdapter.ORDER_PYMOL_SINGLE;
    // could also by NaN, meaning we don't adjust it.
  }


  void addMesh(int tok, Lst<Object> obj, String objName, boolean isMep) {
    JmolObject jo = addJmolObject(tok, null, obj);
    setSceneObject(objName, -1);
    int meshColor = (int) floatSetting(PyMOL.mesh_color);
    if (meshColor < 0)
      meshColor = PyMOLReader.intAt(PyMOLReader.listAt(obj, 0), 2);
    if (!isMep) {
      jo.setSize(meshWidth);
      jo.argb = PyMOL.getRGB(meshColor);
    }
    jo.translucency = transparency;
    jo.cacheID = surfaceInfoName;
  }

  JmolObject addIsosurface(String objectName) {
    JmolObject jo = addJmolObject(T.isosurface, null, objectName);
    jo.cacheID = surfaceInfoName;
    return jo;
  }

}
