/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-26 01:48:23 -0500 (Tue, 26 Sep 2006) $
 * $Revision: 5729 $
 *
 * Copyright (C) 2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.adapter.readers.pymol;

import javajs.util.Lst;

import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.adapter.readers.pdb.PdbReader;
import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.Bond;
import org.jmol.adapter.smarter.Structure;
import org.jmol.api.JmolAdapter;
import org.jmol.api.PymolAtomReader;
import org.jmol.c.STR;
import org.jmol.java.BS;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.BoxInfo;
import org.jmol.util.Logger;

import javajs.util.AU;
import javajs.util.BC;
import javajs.util.CU;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.V3;

/**
 * PyMOL PSE (binary Python session) file reader.
 * development started Feb 2013 Jmol 13.1.13
 * reasonably full implementation May 2013 Jmol 13.1.16
 * 
 * PyMOL state --> Jmol model 
 * PyMOL object --> Jmol named atom set, isosurface, CGO, or measurement 
 * PyMOL group --> Jmol named atom set (TODO: add isosurfaces and measures to these?) 
 * PyMOL movie: an initial view and a set of N "frames" 
 * PyMOL frame: references (a) a state, (b) a script, and (c) a view
 * PyMOL scene --> Jmol scene, including view, frame, visibilities, colors
 *
 * using set LOGFILE, we can dump this to a readable form.
 * 
 * trajectories are not supported yet.
 * 
 * Basic idea is as follows: 
 * 
 * 1) Pickle file is read into a Hashtable.
 * 2) Atoms, bonds, and structures are created, as per other readers, from MOLECULE objects
 * 3) Rendering of atoms and bonds is interpreted as JmolObject objects via PyMOLScene 
 * 3) Other objects such as electron density maps, compiled graphical objects, and 
 *    measures are interpreted, creating more JmolObjects
 * 3) JmolObjects are finalized after file reading takes place by a call from ModelLoader
 *    back here to finalizeModelSet(), which runs PyMOLScene.setObjects, which runs JmolObject.finalizeObject.
 * 
 *  TODO: Handle discrete objects, DiscreteAtmToIdx? 
 *     
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 * 
 */

// TODO: PyMOL.OBJECT_SELECTION not being processed

public class PyMOLReader extends PdbReader implements PymolAtomReader {

  private final static int MIN_RESNO = -1000; // minimum allowed residue number

  private static String nucleic = " A C G T U ADE THY CYT GUA URI DA DC DG DT DU ";

  private boolean allowSurface = true;
  private boolean doResize;
  private boolean doCache;
  private boolean isStateScript;
  private boolean sourcePNGJ;

  private int ac0;
  private int ac;
  private int stateCount;
  private int structureCount;

  private boolean isHidden;

  private BS bsStructureDefined = new BS();
  private BS bsBytesExcluded;

  private int[] atomMap;
  private Map<String, BS> ssMapSeq;

  private PyMOLScene pymolScene;

  private P3 xyzMin = P3.new3(1e6f, 1e6f, 1e6f);
  private P3 xyzMax = P3.new3(-1e6f, -1e6f, -1e6f);

  private int nModels;
  private boolean logging;

  private BS[] reps = new BS[PyMOL.REP_JMOL_MAX];

  private boolean isMovie;

  private int pymolFrame;
  private boolean allStates;
  private int totalAtomCount;
  private int pymolVersion;
  private P3[] trajectoryStep;
  private int trajectoryPtr;

  private String objectName;

  private Map<String, Lst<Object>> volumeData;
  private Lst<Lst<Object>> mapObjects;
  private boolean haveMeasurements;
  private int[] frames;
  private Hashtable<Integer, Lst<Object>> uniqueSettings;
  private Atom[] atoms;
  private boolean haveScenes;
  private int baseModelIndex; // preliminary only; may be revised later if load FILES

  private Lst<Object> sceneOrder;

  private int bondCount;

  private boolean haveBinaryArrays = true;

  @Override
  protected void setup(String fullPath, Map<String, Object> htParams, Object reader) {
    isBinary = mustFinalizeModelSet = true;
    setupASCR(fullPath, htParams, reader);
  }

  @Override
  protected void initializeReader() throws Exception {
    baseAtomIndex = ((Integer) htParams.get("baseAtomIndex")).intValue();
    baseModelIndex = ((Integer) htParams.get("baseModelIndex")).intValue();
    asc.setInfo("noAutoBond",
        Boolean.TRUE);
    asc.setCurrentModelInfo("pdbNoHydrogens", Boolean.TRUE);
    asc.setInfo("isPyMOL", Boolean.TRUE);
    if (isTrajectory)
      trajectorySteps = new Lst<P3[]>();

    isStateScript = htParams.containsKey("isStateScript");
    sourcePNGJ = htParams.containsKey("sourcePNGJ");
    doResize = checkFilterKey("DORESIZE");
    allowSurface = !checkFilterKey("NOSURFACE");
    doCache = checkFilterKey("DOCACHE");
    
    // logic is as follows:
    //
    // isStateScript --> some of this is already done for us. For example, everything is
    //                   already colored and scaled, and there is no need to set the perspective. 
    //
    // doCache && sourcePNGJ   --> reading from a PNGJ that was created with DOCACHE filter
    //                         --> no need for caching.
    //
    // !doCache && sourcePNGJ  --> "standard" PNGJ created without caching
    //                         --> ignore the fact that this is from a PNGJ file
    //
    // doCache && !sourcePNGJ  --> we need to cache surfaces
    //
    // !doCache && !sourcePNGJ --> standard PSE loading

    if (doCache && sourcePNGJ)
      doCache = false;
    else if (sourcePNGJ && !doCache)
      sourcePNGJ = false;
    if (doCache)
      bsBytesExcluded = new BS();
    //logging = true; // specifically for Pickle
    super.initializeReader();
  }

  @Override
  public void processBinaryDocument() throws Exception {
    String logFile = vwr.getLogFileName();
    logging = (logFile.length() > 0);
    Logger.info(logging ? "PyMOL (1) file data streaming to " + logFile : "To view raw PyMOL file data, use 'set logFile \"some_filename\" ");

    PickleReader reader = new PickleReader(binaryDoc, vwr);
    Map<String, Object> map = reader.getMap(logging && Logger.debuggingHigh);
    reader = null;
    process(map);
  }

  @Override
  protected void setAdditionalAtomParameters(Atom atom) {
    // override PDBReader settings
  }

  @Override
  protected void finalizeSubclassReader() throws Exception {
    finalizeReaderPDB();
    asc.setTensors();
  }
  /**
   * At the end of the day, we need to finalize all the JmolObjects, set the
   * trajectories, and, if filtered with DOCACHE, cache a streamlined binary
   * file for inclusion in the PNGJ file.
   * 
   */
  @Override
  public void finalizeModelSet() {

    pymolScene.setReaderObjects();
    
    if (haveMeasurements) {
      appendLoadNote(vwr.getMeasurementInfoAsString());
      setLoadNote();
    }
    
    if (haveScenes) {
      String[] scenes = new String[sceneOrder.size()];
      for (int i = scenes.length; --i >= 0;)
        scenes[i] = stringAt(sceneOrder, i);
      vwr.ms.msInfo.put("scenes", scenes);
    }
    
    vwr.ms.setTrajectoryBs(BSUtil.newBitSet2(baseModelIndex,
        vwr.ms.mc));
    if (!isStateScript)
      pymolScene.setFrameObject(0, null);

    // exclude unnecessary named objects

    if (bsBytesExcluded != null) {
      int nExcluded = bsBytesExcluded.cardinality();
      byte[] bytes0 = (byte[]) vwr.fm.getFileAsBytes(filePath, null);
      byte[] bytes = new byte[bytes0.length - nExcluded];
      for (int i = bsBytesExcluded.nextClearBit(0), n = bytes0.length, pt = 0; i < n; i = bsBytesExcluded
          .nextClearBit(i + 1))
        bytes[pt++] = bytes0[i];
      bytes0 = null;
      String fileName = filePath;
      vwr.cachePut(fileName, bytes);
    }
  }

  /**
   * The main processor.
   * 
   * @param map
   */
  @SuppressWarnings("unchecked")
  private void process(Map<String, Object> map) {

    pymolVersion = ((Integer) map.get("version")).intValue();
    appendLoadNote("PyMOL version: " + pymolVersion);

    // create settings and uniqueSettings lists
    Lst<Object> settings = fixSettings(getMapList(map, "settings"));
    Lst<Object> lst = listAt(settings, PyMOL.dump_binary);
    haveBinaryArrays = (lst != null  && floatAt(lst, 2) == 1);
    sceneOrder = getMapList(map, "scene_order");
    haveScenes = getFrameScenes(map);
    Lst<Object> file = listAt(settings, PyMOL.session_file);
    if (file != null && file.size() > 2)
      Logger.info("PyMOL session file: " + file.get(2));
    //asc.setAtomSetCollectionAuxiliaryInfo("settings", settings);
    setUniqueSettings(getMapList(map, "unique_settings"));
    pymolScene = new PyMOLScene(this, vwr, settings, uniqueSettings, 
        pymolVersion, haveScenes, baseAtomIndex, baseModelIndex, doCache, filePath);
    //just doing this based on having binaryStrings. haveBinaryArrays = pymolScene.booleanSetting(PyMOL.dump_binary);

    // just log and display some information here
    String logFile = vwr.getLogFileName();
    logging = (logFile.length() > 0);
    Logger.info(logging ? "PyMOL file data streaming to " + logFile : "To view raw PyMOL file data, use 'set logFile \"some_filename\" ");
    Lst<Object> names = getMapList(map, "names");
    for (Map.Entry<String, Object> e : map.entrySet()) {
      String name = e.getKey();
      Logger.info(name);
      if (name.equals("names")) {
        for (int i = 1; i < names.size(); i++) {
          Lst<Object> obj = listAt(names, i);
          Logger.info("  " + stringAt(obj, 0));
        }
      }
    }
    if (logging) {
      if (logging)
        vwr.log("$CLEAR$");
      //String s = map.toString();//.replace('=', '\n');
      for (Map.Entry<String, Object> e : map.entrySet()) {
        String name = e.getKey();
        if (!"names".equals(name)) {
          vwr.log("\n===" + name + "===");
          vwr.log(PT.rep(e.getValue().toString(), "[",
              "\n["));
        }
      }
      vwr.log("\n===names===");
      for (int i = 1; i < names.size(); i++) {
        vwr.log("");
        Lst<Object> list = (Lst<Object>) names.get(i);
        vwr.log(" =" + bytesToString(list.get(0)) + "=");
        try {
          vwr.log(PT.rep(list.toString(), "[", "\n["));
        } catch (Throwable e) {
          //
        }
      }
    }

    // set up additional colors
    // not 100% sure what color clamping is, but this seems to work.
    addColors(getMapList(map, "colors"), pymolScene
        .globalSetting(PyMOL.clamp_colors) != 0);

    // set a few global flags
    allStates = (pymolScene.globalSetting(PyMOL.all_states) != 0);
    pymolFrame = (int) pymolScene.globalSetting(PyMOL.frame);
    // discover totalAtomCount and stateCount:
    getAtomAndStateCount(names);
    pymolScene.setStateCount(stateCount);

    int pymolState = (int) pymolScene.globalSetting(PyMOL.state);
    if (!isMovie)
      pymolScene.setFrameObject(T.frame, (allStates ? Integer.valueOf(-1)
          : Integer.valueOf(pymolState - 1)));
    appendLoadNote("frame=" + pymolFrame + " state=" + pymolState
        + " all_states=" + allStates);

    // resize frame
    if (!isStateScript && doResize) {
      int width = 0, height = 0;
      Lst<Object> main = getMapList(map, "main");
      if (main != null) {
        // not all PSE files have this
        width = intAt(main, 0);
        height = intAt(main, 1);
      }
      String note;
      if (width > 0 && height > 0) {
        note = "PyMOL dimensions width=" + width + " height=" + height;
        asc.setInfo(
            "preferredWidthHeight", new int[] { width, height });
        //Dimension d = 
        vwr.resizeInnerPanel(width, height);
      } else {
        note = "PyMOL dimensions?";
      }
      appendLoadNote(note);
    }
    // PyMOL setting all_states disables movies
    Lst<Object> mov;
    if (!isStateScript && !allStates
        && (mov = getMapList(map, "movie")) != null) {
      int frameCount = intAt(mov, 0);
      if (frameCount > 0)
        processMovie(mov, frameCount);
    }
    if (totalAtomCount == 0)
      asc.newAtomSet();

    if (allStates && desiredModelNumber == Integer.MIN_VALUE) {
      // if all_states and no model number indicated, display all states
    } else if (isMovie) {
      // otherwise, if a movie, load all states
      switch (desiredModelNumber) {
      case Integer.MIN_VALUE:
        break;
      default:
        desiredModelNumber = frames[(desiredModelNumber > 0
            && desiredModelNumber <= frames.length ? desiredModelNumber
            : pymolFrame) - 1];
        pymolScene.setFrameObject(T.frame, Integer
            .valueOf(desiredModelNumber - 1));
        break;
      }
    } else if (desiredModelNumber == 0) {
      // otherwise if you specify model "0", only load the current PyMOL state
      desiredModelNumber = pymolState;
    } else {
      // load only the state you request, or all states, if you don't specify
    }
    int n = names.size();
    
    for (int j = 0; j < stateCount; j++) {
      if (!doGetModel(++nModels, null))
        continue;
      model(nModels);
      pymolScene.currentAtomSetIndex = asc.iSet;
      if (isTrajectory) {
        trajectoryStep = new P3[totalAtomCount];
        trajectorySteps.addLast(trajectoryStep);
        trajectoryPtr = 0;
      }
      for (int i = 1; i < n; i++)
        processObject(listAt(names, i), true, j);
    }
    for (int i = 1; i < n; i++)
      processObject(listAt(names, i), false, 0);
    pymolScene.setReaderObjectInfo(null, 0, null, false, null, null, null);

    // meshes are special objects that depend upon grid map data
    if (mapObjects != null && allowSurface)
      processMeshes(); 

    // trajectories are not supported yet
    if (isTrajectory) {
      appendLoadNote("PyMOL trajectories read: " + trajectorySteps.size());
      asc.finalizeTrajectoryAs(trajectorySteps, null);
    }

    processDefinitions();
    processSelectionsAndScenes(map);
    // no need to render if this is a state script
    pymolScene.finalizeVisibility();
    if (!isStateScript) {
      // same idea as for a Jmol state -- session reinitializes
      vwr.initialize(false, true);
      addJmolScript(pymolScene.getViewScript(getMapList(map, "view"))
          .toString());
    }
    if (ac == 0)
      asc.setInfo("dataOnly",
          Boolean.TRUE);
    pymolScene.offsetObjects();
  }

  /**
   * Recent PyMOL files may not have all settings. For now, we just add null
   * values;
   * 
   * @param settings
   * @return settings
   */
  private Lst<Object> fixSettings(Lst<Object> settings) {
    int n = settings.size();
    for (int i = 0; i < n; i++) {
      @SuppressWarnings("unchecked")
      int i2 = intAt((Lst<Object>) settings.get(i), 0);
      if (i2 == -1) {
        Logger.info("PyMOL reader adding null setting #" + i);
        settings.set(i, new Lst<Object>()); // PyMOL 1.7 needs this
      } else {
        while (i < i2) {
          Logger.info("PyMOL reader adding null setting #" + i);
          settings.add(i++, new Lst<Object>());
          n++;
        }
      }
    }
    return settings;
  }

  /**
   * remove all scenes that do not define a frame.
   * @param map
   * @return  true if there are scenes that define a frame
   */
  @SuppressWarnings("unchecked")
  private boolean getFrameScenes(Map<String, Object> map) {
    if (sceneOrder == null)
      return false;
    Map<String, Object> scenes = (Map<String, Object>) map.get("scene_dict");
    for (int i = 0; i < sceneOrder.size(); i++) {
      String name = stringAt(sceneOrder, i);
      Lst<Object> thisScene = getMapList(scenes, name);
      if (thisScene == null || thisScene.get(2) == null)
        sceneOrder.removeItemAt(i--);
    }
    return (sceneOrder != null && sceneOrder.size() != 0);
  }

  /**
   * Create uniqueSettings from the "unique_settings" map item.
   * This will be used later in processing molecule objects.
   * 
   * @param list
   * @return max id
   */
  @SuppressWarnings("unchecked")
  private int setUniqueSettings(Lst<Object> list) {
    uniqueSettings = new Hashtable<Integer, Lst<Object>>();
    int max = 0;
    if (list != null && list.size() != 0) {
      for (int i = list.size(); --i >= 0;) {
        Lst<Object> atomSettings = (Lst<Object>) list.get(i);
        int id = intAt(atomSettings, 0);
        if (id > max)
          max = id;
        Lst<Object> mySettings = (Lst<Object>) atomSettings.get(1);
        for (int j = mySettings.size(); --j >= 0;) {
          Lst<Object> setting = (Lst<Object>) mySettings.get(j);
          int uid = (id << 10) + intAt(setting, 0);
          uniqueSettings.put(Integer.valueOf(uid), setting);
          //System.out.println("PyMOL unique setting " + id + " " + setting);
        }
      }
    }
    return max;
  }

  private final P3 ptTemp = new P3();

  /**
   * Add new colors from the main "colors" map object.
   * Not 100% clear how color clamping works.
   * 
   * @param colors
   * @param isClamped
   */
  private void addColors(Lst<Object> colors, boolean isClamped) {
    if (colors == null || colors.size() == 0)
      return;
    // note, we are ignoring lookup-table colors
    for (int i = colors.size(); --i >= 0;) {
      Lst<Object> c = listAt(colors, i);
      PyMOL.addColor((Integer) c.get(1), isClamped ? colorSettingClamped(c, ptTemp)
          : getColorPt(c.get(2), ptTemp));
    }
  }

  /**
   * Look through all named objects for molecules, counting
   * atoms and also states; see if trajectories are compatible (experimental).
   *  
   * @param names
   */
  private void getAtomAndStateCount(Lst<Object> names) {
    int n = 0;
    for (int i = 1; i < names.size(); i++) {
      Lst<Object> execObject = listAt(names, i);
      int type = intAt(execObject, 4);
      if (!checkObject(execObject))
        continue;
      if (type == PyMOL.OBJECT_MOLECULE) {
        Lst<Object> pymolObject = listAt(execObject, 5);
        Lst<Object> states = listAt(pymolObject, 4);
        int ns = states.size();
        if (ns > stateCount)
          stateCount = ns;
        int nAtoms, nBonds;
        if (haveBinaryArrays) {
          nBonds = ((byte[]) listAt(pymolObject, 6).get(1)).length / 20;
          nAtoms = ((byte[]) listAt(pymolObject, 7).get(1)).length / 120;
          n += nAtoms;
        } else {
          nBonds = listAt(pymolObject, 6).size();
          nAtoms = listAt(pymolObject, 7).size();
        }
        System.out.println("Object " + objectName + " nBonds=" + nBonds + ", nAtoms = " + nAtoms);
        for (int j = 0; j < ns; j++) {
          Lst<Object> state = listAt(states, j);
          Lst<Object> idxToAtm = listAt(state, 3);
          if (idxToAtm == null) {
            isTrajectory = false;
          } else {
            int m = idxToAtm.size();
            n += m;
            if (isTrajectory && m != nAtoms)
              isTrajectory = false;
          }
        }
      }
    }
    totalAtomCount = n;
    Logger.info("PyMOL total atom count = " + totalAtomCount);
    Logger.info("PyMOL state count = " + stateCount);
  }

  private boolean checkObject(Lst<Object> execObject) {
    objectName = stringAt(execObject, 0);
    isHidden = (intAt(execObject, 2) != 1);
    return (objectName.indexOf("_") != 0);
  }

  /**
   * Create a JmolObject that will represent the movie.
   * For now, only process unscripted movies without views.
   * 
   * @param mov
   * @param frameCount
   */
  private void processMovie(Lst<Object> mov, int frameCount) {
    Map<String, Object> movie = new Hashtable<String, Object>();
    movie.put("frameCount", Integer.valueOf(frameCount));
    movie.put("currentFrame", Integer.valueOf(pymolFrame - 1));
    boolean haveCommands = false, haveViews = false, haveFrames = false;
    Lst<Object> list = listAt(mov, 4);
    for (int i = list.size(); --i >= 0;)
      if (intAt(list, i) != 0) {
        frames = new int[list.size()];
        for (int j = frames.length; --j >= 0;)
          frames[j] = intAt(list, j) + 1;
        movie.put("frames", frames);
        haveFrames = true;
        break;
      }
    Lst<Object> cmds = listAt(mov, 5);
    String cmd;
    for (int i = cmds.size(); --i >= 0;)
      if ((cmd = stringAt(cmds, i)) != null && cmd.length() > 1) {
        cmds = fixMovieCommands(cmds);
        if (cmds != null) {
          movie.put("commands", cmds);
          haveCommands = true;
          break;
        }
      }
    Lst<Object> views = listAt(mov, 6);
    Lst<Object> view;
    for (int i = views.size(); --i >= 0;)
      if ((view = listAt(views, i)) != null && view.size() >= 12
          && view.get(1) != null) {
        haveViews = true;
        views = fixMovieViews(views);
        if (views != null) {
          movie.put("views", views);
          break;
        }
      }
    appendLoadNote("PyMOL movie frameCount = " + frameCount);
    if (haveFrames && !haveCommands && !haveViews) {
      // simple animation
      isMovie = true;
      pymolScene.setReaderObjectInfo(null, 0, null, false, null, null, null);
      pymolScene.setFrameObject(T.movie, movie);
    } else {
      //isMovie = true;  for now, no scripted movies
      //pymol.put("movie", movie);
    }
  }

  /**
   * Could implement something here that creates a Jmol view.
   * 
   * @param views
   * @return new views
   */
  private static Lst<Object> fixMovieViews(Lst<Object> views) {
    // TODO -- PyMOL to Jmol views
    return views;
  }

  /**
   * Could possibly implement something here that interprets PyMOL script commands.
   * 
   * @param cmds
   * @return new cmds
   */
  private static Lst<Object> fixMovieCommands(Lst<Object> cmds) {
    // TODO -- PyMOL to Jmol commands
    return cmds;
  }
  
  /**
   * The main object processor. Not implemented: ALIGNMENT, CALLBACK, SLICE,
   * SURFACE
   * 
   * @param execObject
   * @param moleculeOnly
   * @param iState
   */
  @SuppressWarnings("unchecked")
  private void processObject(Lst<Object> execObject, boolean moleculeOnly,
                             int iState) {
    if (execObject == null)
      return;
    int type = intAt(execObject, 4);
    Lst<Object> startLen = (Lst<Object>) execObject
        .get(execObject.size() - 1);
    if ((type == PyMOL.OBJECT_MOLECULE) != moleculeOnly || !checkObject(execObject))
      return;
    Lst<Object> pymolObject = listAt(execObject, 5);
    Lst<Object> stateSettings = null;
    if (type == PyMOL.OBJECT_MOLECULE) {
      Lst<Object> states = listAt(pymolObject, 4);
      Lst<Object> state = listAt(states, iState);
      Lst<Object> idxToAtm = listAt(state, 3);
      if (iState > 0 && (idxToAtm == null || idxToAtm.size() == 0))
        return;
      stateSettings = listAt(state, 7);
    } else   if (iState > 0) {
        return;
    }
    
    Logger.info("PyMOL model " + (nModels) + " Object " + objectName
        + (isHidden ? " (hidden)" : " (visible)"));
    if (!isHidden && !isMovie && !allStates) {
      if (pymolFrame > 0 && pymolFrame != nModels) {
        pymolFrame = nModels;
        allStates = true;
        pymolScene.setFrameObject(T.frame, Integer.valueOf(-1));
      }
    }
    Lst<Object> objectHeader = listAt(pymolObject, 0);
    String parentGroupName = (execObject.size() < 8 ? null : stringAt(
        execObject, 6));
    if (" ".equals(parentGroupName))
      parentGroupName = null;
    pymolScene.setReaderObjectInfo(objectName, type, parentGroupName, isHidden, listAt(objectHeader, 8), stateSettings, (moleculeOnly ? "_" + (iState + 1) : ""));
    BS bsAtoms = null;
    boolean doExclude = (bsBytesExcluded != null);
    String msg = null;
    switch (type) {
    default:
      msg = "" + type;
      break;
    case PyMOL.OBJECT_SELECTION:
      pymolScene.processSelection(execObject);
      break;
    case PyMOL.OBJECT_MOLECULE:
      doExclude = false;
      bsAtoms = processMolecule(pymolObject, iState);
      break;
    case PyMOL.OBJECT_MEASURE:
      doExclude = false;
      processMeasure(pymolObject);
      break;
    case PyMOL.OBJECT_MAPMESH:
    case PyMOL.OBJECT_MAPDATA:
      processMap(pymolObject, type == PyMOL.OBJECT_MAPMESH, false);
      break;
    case PyMOL.OBJECT_GADGET:
      processGadget(pymolObject);
      break;
    case PyMOL.OBJECT_GROUP:
        if (parentGroupName == null)
          parentGroupName = ""; // force creation
      break;
    case PyMOL.OBJECT_CGO:
      msg = "CGO";
      processCGO(pymolObject);
      break;

      // unimplemented:
      
    case PyMOL.OBJECT_ALIGNMENT:
      msg = "ALIGNEMENT";
      break;
    case PyMOL.OBJECT_CALCULATOR:
      msg = "CALCULATOR";
      break;
    case PyMOL.OBJECT_CALLBACK:
      msg = "CALLBACK";
      break;
    case PyMOL.OBJECT_SLICE:
      msg = "SLICE";
      break;
    case PyMOL.OBJECT_SURFACE:
      msg = "SURFACE";
      break;
    }
    if (parentGroupName != null || bsAtoms != null)
      pymolScene.addGroup(execObject, parentGroupName, type, bsAtoms);
    if (doExclude) {
      int i0 = intAt(startLen, 0);
      int len = intAt(startLen, 1);
      bsBytesExcluded.setBits(i0, i0 + len);
      Logger.info("cached PSE file excludes PyMOL object type " + type
          + " name=" + objectName + " len=" + len);
    }
    if (msg != null)
      Logger.error("Unprocessed object type " + msg + " " + objectName);
  }

  /**
   * Create a CGO JmolObject, just passing on key information. 
   * 
   * @param pymolObject
   */
  private void processCGO(Lst<Object> pymolObject) {
    if (isStateScript)
      return;
    if (isHidden)
      return;
    Lst<Object> data = sublistAt(pymolObject, 2, 0);
    int color = PyMOL.getRGB(intAt(listAt(pymolObject, 0), 2));
    String name = pymolScene.addCGO(data, color);
    if (name != null)
      appendLoadNote("CGO " + name);
  }

  /**
   * Only process _e_pot objects -- which we need for color settings 
   * @param pymolObject
   */
  private void processGadget(Lst<Object> pymolObject) {
    if (objectName.endsWith("_e_pot"))
      processMap(pymolObject, true, true);
  }

  /**
   * Create mapObjects and volumeData; create an ISOSURFACE JmolObject.
   * 
   * @param pymolObject
   * @param isObject
   * @param isGadget 
   */
  private void processMap(Lst<Object> pymolObject, boolean isObject, boolean isGadget) {
    if (isObject) {
      if (sourcePNGJ)
        return;
      if (isHidden && !isGadget)
        return; // for now
      if (mapObjects == null)
        mapObjects = new Lst<Lst<Object>>();
      mapObjects.addLast(pymolObject);
    } else {
      if (volumeData == null)
        volumeData = new Hashtable<String, Lst<Object>>();
      volumeData.put(objectName, pymolObject);
      if (!isHidden && !isStateScript)
        pymolScene.addIsosurface(objectName);
    }
    pymolObject.addLast(objectName);
  }



  /**
   * Create a MEASURE JmolObject.
   * 
   * @param pymolObject
   */
  private void processMeasure(Lst<Object> pymolObject) {
    if (isStateScript)
      return;
    if (isHidden)
      return; // will have to reconsider this if there is a movie, though
    Logger.info("PyMOL measure " + objectName);
    Lst<Object> measure = sublistAt(pymolObject, 2, 0);
    int pt;
    int nCoord = (measure.get(pt = 1) instanceof Lst<?> ? 2 : measure
        .get(pt = 4) instanceof Lst<?> ? 3
        : measure.get(pt = 6) instanceof Lst<?> ? 4 : 0);
    if (nCoord == 0)
      return;
    Lst<Object> setting = listAt(pymolObject, 0);
    BS bsReps = getBsReps(listAt(setting, 3));
    Lst<Object> list = listAt(measure, pt);
    Lst<Object> offsets = listAt(measure, 8);
    boolean haveLabels = (measure.size() > 8);
    int color = intAt(setting, 2);
    if (pymolScene.addMeasurements(null, nCoord, list, bsReps, color, offsets, haveLabels))
      haveMeasurements = true;    
  }

  /**
   * Create everything necessary to generate a molecule in Jmol.
   * 
   * @param pymolObject
   * @param iState
   * @return atom set only if this is a trajectory.
   */
  private BS processMolecule(Lst<Object> pymolObject, int iState) {
    Lst<Object> states = listAt(pymolObject, 4);
    Lst<Object> state = listAt(states, iState);
    Lst<Object> idxToAtm;
    Lst<Object> coords;
    Lst<Object> labelPositions;
    int[] idxArray = null;
    float[] coordsArray = null;
    float[] labelArray = null;
    int nBonds = intAt(pymolObject, 2);
    int nAtoms = intAt(pymolObject, 3);
    int n = nAtoms;
    if (haveBinaryArrays && AU.isAB(state.get(3))) {
      idxToAtm = coords = labelPositions = null;
      idxArray = new int[nAtoms];
      coordsArray = new float[nAtoms * 3];
      fillFloatArrayFromBytes((byte[]) state.get(2), coordsArray);
      fillIntArrayFromBytes((byte[]) state.get(3), idxArray);
      byte[] b = (byte[]) state.get(8);
      if (b != null) {
        labelArray = new float[nAtoms * 7];
        fillFloatArrayFromBytes(b, labelArray);
      }

    } else {
      coords = listAt(state, 2);
      idxToAtm = listAt(state, 3);
      // atomToIdx = listAt(state, 4);
      
      labelPositions = listAt(state, 8);
      if (idxToAtm != null)
        n = idxToAtm.size();
    }
    if (n == 0)
      return null;

    ac = ac0 = asc.ac;
    if (nAtoms == 0)
      return null;
    ssMapSeq = new Hashtable<String, BS>();
    if (iState == 0)
      processMolCryst(listAt(pymolObject, 10));
    Lst<Bond> bonds = getBondList(listAt(pymolObject, 6));
    Lst<Object> pymolAtoms = listAt(pymolObject, 7);
    atomMap = new int[nAtoms];
    BS bsAtoms = pymolScene.setAtomMap(atomMap, ac0);
    for (int i = 0; i < PyMOL.REP_JMOL_MAX; i++)
      reps[i] = BS.newN(1000);

    // TODO: Implement trajectory business here.

    if (iState == 0 || !isTrajectory) {
      pymolScene.ensureCapacity(n);
      String[] lexStr = null;
      byte[] atomArray = null;
      int[] vArray = null;
      if (haveBinaryArrays) {
        int ver = intAt(pymolAtoms,  0);
        atomArray = (byte[])pymolAtoms.get(1);
        lexStr = getLexStr((byte[])pymolAtoms.get(2));
        System.out.println("PyMOL atom dump version " + ver);
        vArray = (haveBinaryArrays ? PyMOL.getVArray(ver) : null);
      }
      for (int idx = 0; idx < n; idx++) {
        P3 a = addAtom(pymolAtoms, (idxToAtm != null ? intAt(idxToAtm, idx)
            : idxArray != null ? idxArray[idx] : idx),
            atomArray, vArray, lexStr, idx, coords,
            coordsArray, labelPositions, labelArray, bsAtoms, iState);
        if (a != null)
          trajectoryStep[trajectoryPtr++] = a;
      }
    }
    addBonds(bonds);
    addMolStructures();
    atoms = asc.atoms;
    if (!isStateScript)
      createShapeObjects();
    ssMapSeq = null;

    Logger.info("reading " + (ac - ac0) + " atoms and " + nBonds + " bonds");
    Logger.info("----------");
    return bsAtoms;
  }

  private String[] getLexStr(byte[] lex) {
    int pt = 0;
    int n = BC.bytesToInt(lex, pt, false);
    int[] index = new int[n];
    int imax = 0;
    for (int i = 0; i < n; i++) {
      pt += 4;
      int ipt = index[i] = BC.bytesToInt(lex, pt, false);
      if (ipt > imax)
        imax = ipt;
    }
    String[] tokens = new String[imax + 1];
    tokens[0] = " ";
    pt += 4;
    for (int i = 0; i < n; i++) {
      String s = tokens[index[i]] = getCStr(lex, pt);
      pt += s.length() + 1;
    }
    return tokens;
  }
  
  private String getCStr(byte[] lex, int pt) {
    try {
      byte[] a = aTemp;
      int apt = 0;
      byte b = 0;
      while ((b = lex[pt++]) != 0) {
        if (apt >= a.length)
          a = aTemp = AU.doubleLengthByte(a);
        a[apt++] = b;
      }
      return new String(AU.arrayCopyByte(a, apt), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return null;
    }
  }

  byte[] aTemp = new byte[16];


  /**
   * Pick up the crystal data.
   * 
   * @param cryst
   */
  private void processMolCryst(Lst<Object> cryst) {
    if (cryst == null || cryst.size() == 0)
      return;
    Lst<Object> l = sublistAt(cryst, 0, 0);
    Lst<Object> a = sublistAt(cryst, 0, 1);
    setUnitCell(floatAt(l, 0), floatAt(l, 1), floatAt(l, 2), floatAt(a, 0),
        floatAt(a, 1), floatAt(a, 2));
    setSpaceGroupName(stringAt(cryst, 1));
  }

  /**
   * Create the bond set.
   * 
   * @param bonds
   * @return list of bonds
   */
  private Lst<Bond> getBondList(Lst<Object> bonds) {
    boolean asSingle = !pymolScene.booleanSetting(PyMOL.valence);
    byte[] b = null;
    int[] vArray = null;
    int n = bonds.size();
    int len = 0;
    if (haveBinaryArrays && n == 2) {
      int ver = intAt(bonds, 0);
      System.out.println("PyMOL bond dump version " + ver);
      vArray  = PyMOL.getVArrayB(ver);
      b = (byte[]) bonds.get(1);
      len = vArray[PyMOL.LEN];
      n = b.length / len;
    }
    Lst<Bond> bondList = new Lst<Bond>();
    bondList.ensureCapacity(n);
    int ia, ib, order, uid = -1;
    for (int i = 0, apt = 0; i < n; i++) {
      if (haveBinaryArrays) {
        ia = BC.bytesToInt(b, apt + vArray[PyMOL.BATOM1], false);
        ib = BC.bytesToInt(b, apt + vArray[PyMOL.BATOM2], false);
        uid = (b[apt + vArray[PyMOL.BHASSETTING]] == 0 ? -1: BC.bytesToInt(b, apt + vArray[PyMOL.BUNIQUEID], false));
        order = b[apt + vArray[PyMOL.BORDER]];
        apt += len;
      } else {
        Lst<Object> lst = listAt(bonds, i);
        ia = intAt(lst, 0);
        ib = intAt(lst, 1);
        order = intAt(lst, 2);
        uid = (lst.size() > 6 && intAt(lst, 6) != 0 ? intAt(lst, 5) : -1);
      }
      if (order < 1 || order > 3)
        order = 1;
      order |= (asSingle || order == 1 ? JmolAdapter.ORDER_PYMOL_SINGLE
          : JmolAdapter.ORDER_PYMOL_MULT);
      Bond bond = new Bond(ia, ib, order);
      bond.uniqueID = uid;
      bondList.addLast(bond);
    }
    return bondList;
  }
  private void fillIntArrayFromBytes(byte[] b, int[] array) {
    for (int i = 0, pt = 0; i < b.length; i += 4)
      array[pt++] = BC.bytesToInt(b, i, false);
  }

  private void fillFloatArrayFromBytes(byte[] b, float[] array) {
    try {
      for (int i = 0, pt = 0; i < b.length; i += 4)
        array[pt++] = BC.bytesToFloat(b, i, false);
    } catch (Exception e) {
      // not possible?
    }
  }

  

  // [0] Int        resv
  // [1] String     chain
  // [2] String     alt
  // [3] String     resi
  // [4] String     segi
  // [5] String     resn
  // [6] String     name
  // [7] String     elem
  // [8] String     textType
  // [9] String     label
  // [10] String    ssType
  // [11] Int       hydrogen
  // [12] Int       customType
  // [13] Int       priority
  // [14] Float     b-factor
  // [15] Float     occupancy
  // [16] Float     vdw
  // [17] Float     partialCharge
  // [18] Int       formalCharge
  // [19] Int       hetatm
  // [20] List      reps
  // [21] Int       color pointer
  // [22] Int       id
  // [23] Int       cartoon type
  // [24] Int       flags
  // [25] Int       bonded
  // [26] Int       chemFlag
  // [27] Int       geom
  // [28] Int       valence
  // [29] Int       masked
  // [30] Int       protekted
  // [31] Int       protons
  // [32] Int       unique_id
  // [33] Int       stereo
  // [34] Int       discrete_state
  // [35] Float     elec_radius
  // [36] Int       rank
  // [37] Int       hb_donor
  // [38] Int       hb_acceptor
  // [39] Int       atomic_color
  // [40] Int       has_setting
  // [41] Float     U11
  // [42] Float     U22
  // [43] Float     U33
  // [44] Float     U12
  // [45] Float     U13
  // [46] Float     U23


  /**
   * @param pymolAtoms
   *        list of atom details
   * @param apt
   *        array pointer into pymolAtoms
   * @param atomArray 
   * @param vArray 
   * @param lexStr 
   * @param icoord
   *        array pointer into coords (/3)
   * @param coords
   *        coordinates array
   * @param coordArray 
   * @param labelPositions
   * @param labelArray 
   * @param bsState
   *        this state -- Jmol atomIndex
   * @param iState
   * @return true if successful
   * 
   */
  private P3 addAtom(Lst<Object> pymolAtoms, int apt, 
                     byte[] atomArray, int[] vArray, String[] lexStr,
                     int icoord,
                     Lst<Object> coords, float[] coordArray, 
                     Lst<Object> labelPositions, float[] labelArray,
                     BS bsState, int iState) {
    atomMap[apt] = -1;
    String chainID, altLoc, group3, name, sym, label, ssType, resi, insCode = null;
    float bfactor, occupancy, radius, partialCharge;
    int seqNo, intReps, formalCharge, atomColor, serNo, cartoonType, flags, uniqueID = -1;
    boolean isHetero, bonded;
    float[] anisou = null;
    BS bsReps = null;
    if (haveBinaryArrays) {
      int vpt;
      int pt = apt * vArray[PyMOL.LEN];
      seqNo = atomInt(atomArray, pt, vArray[PyMOL.RESV]);
      chainID = atomStr(atomArray, pt, vArray[PyMOL.CHAIN], lexStr);
      resi =  atomStr(atomArray, pt, vArray[PyMOL.RESI], lexStr);
      group3 = atomStr(atomArray, pt, vArray[PyMOL.RESN], lexStr);
      if (group3.length() > 3)
        group3 = group3.substring(0, 3);
      name = atomStr(atomArray, pt, vArray[PyMOL.NAME], lexStr);
      sym = atomStr(atomArray, pt, vArray[PyMOL.ELEM], lexStr);
      label = atomStr(atomArray, pt, vArray[PyMOL.LABEL], lexStr);
      ssType = atomStr(atomArray, pt, vArray[PyMOL.SSTYPE], null);
      altLoc = atomStr(atomArray, pt, vArray[PyMOL.ALTLOC], null);
      if ((vpt = vArray[PyMOL.INSCODE]) == 0) {
        resi = atomStr(atomArray, pt, vArray[PyMOL.RESI], null);
      } else {
        byte b = atomArray[pt + vpt];
        insCode = (b == 0 ? " " :"" + (char) b);        
      }
      bfactor = atomFloat(atomArray, pt, vArray[PyMOL.BFACTOR]);
      occupancy = atomFloat(atomArray, pt, vArray[PyMOL.OCCUPANCY]);
      radius= atomFloat(atomArray, pt, vArray[PyMOL.VDW]);
      partialCharge = atomFloat(atomArray, pt, vArray[PyMOL.PARTIALCHARGE]);
      formalCharge = atomArray[pt + vArray[PyMOL.FORMALCHARGE]];
      if (formalCharge > 125)
        formalCharge -= 512;
      intReps = atomInt(atomArray, pt, vArray[PyMOL.VISREP]);
//      System.out.println(apt + " " + pt + " " + intReps);
      atomColor = atomInt(atomArray, pt, vArray[PyMOL.COLOR]);
      serNo = atomInt(atomArray, pt, vArray[PyMOL.ID]);
      cartoonType = atomInt(atomArray, pt, vArray[PyMOL.CARTOON]);
      flags = atomInt(atomArray, pt, vArray[PyMOL.FLAGS]);
      uniqueID = atomInt(atomArray, pt, vArray[PyMOL.UNIQUEID]);
      if (uniqueID == 0)
        uniqueID = -1;
      anisou = new float[8];
      if ((vpt = vArray[PyMOL.ANISOU]) > 0)
        for (int i = 0; i < 6; i++)
          anisou[i] = BC.bytesToShort(atomArray, pt + vpt + (i << 1), false);
      bonded = atomBool(atomArray, pt, vArray[PyMOL.BONDED], vArray[PyMOL.BONMASK]);
      isHetero = atomBool(atomArray, pt, vArray[PyMOL.HETATM], vArray[PyMOL.HETMASK]);
    } else {
      Lst<Object> a = listAt(pymolAtoms, apt);
      seqNo = intAt(a, 0); // may be negative
      chainID = stringAt(a, 1); // may be more than one char.
      altLoc = stringAt(a, 2);
      resi = stringAt(a, 3);
      group3 = stringAt(a, 5);
      name = stringAt(a, 6);
      sym = stringAt(a, 7);
      label = stringAt(a, 9);
      ssType = stringAt(a, 10).substring(0, 1);
      bfactor = floatAt(a, 14);
      occupancy = floatAt(a, 15);
      radius = floatAt(a, 16);
      partialCharge = floatAt(a, 17);
      formalCharge = intAt(a, 18);
      isHetero = (intAt(a, 19) != 0);
      bsReps = getBsReps(listAt(a, 20));
      intReps = (bsReps == null ? intAt(a, 20) : 0); // Pymol 1.8      
      atomColor = intAt(a, 21);
      serNo = intAt(a, 22);
      cartoonType = intAt(a, 23);
      flags = intAt(a, 24);
      bonded = (intAt(a, 25) != 0);
      uniqueID = (a.size() > 40 && intAt(a, 40) == 1 ? intAt(a, 32) : -1);
      if (a.size() > 46)
        anisou = floatsAt(a, 41, new float[8], 6);
    }
    if (insCode == null) {
      int len = resi.length();
      char ch = (len > 0 ? resi.charAt(len - 1) : ' ');
      insCode = (PT.isDigit(ch) ? " "  : "" + ch);
    }

    if (group3.length() > 3)
      group3 = group3.substring(0, 3);
    if (group3.equals(" "))
      group3 = "UNK";
    if (sym.equals("A"))
      sym = "C";
    int ichain = vwr.getChainID(chainID, true);
    Atom atom = processAtom(new Atom(), name, altLoc.charAt(0), group3, 
        ichain, seqNo, insCode.charAt(0), isHetero, sym);
    if (!filterPDBAtom(atom, fileAtomIndex++))
      return null;
    float x, y, z;
    icoord *= 3;
    if (haveBinaryArrays) {
      x = coordArray[icoord];
      y = coordArray[++icoord];
      z = coordArray[++icoord];
    } else {
      x = floatAt(coords, icoord);
      y = floatAt(coords, ++icoord);
      z = floatAt(coords, ++icoord);
    }
    BoxInfo.addPointXYZ(x, y, z, xyzMin, xyzMax, 0);
    if (isTrajectory && iState > 0)
      return null;
    boolean isNucleic = (nucleic.indexOf(group3) >= 0);
    if (bsState != null)
      bsState.set(ac);

    if (seqNo >= MIN_RESNO
        && (!ssType.equals(" ") || name.equals("CA") || isNucleic)) {
      BS bs = ssMapSeq.get(ssType);
      if (bs == null)
        ssMapSeq.put(ssType, bs = new BS());
      bs.set(seqNo - MIN_RESNO);
      ssType += ichain;
      bs = ssMapSeq.get(ssType);
      if (bs == null)
        ssMapSeq.put(ssType, bs = new BS());
      bs.set(seqNo - MIN_RESNO);
    }
    atom.bfactor = bfactor;
    atom.foccupancy = occupancy;
    atom.radius = radius;
    if (atom.radius == 0)
      atom.radius = 1;
    atom.partialCharge = partialCharge;
    // repurposing vib; leaving Z = Float.NaN to disable actual vibrations
    atom.vib = V3.new3(uniqueID, cartoonType, Float.NaN);    
    if (anisou != null && anisou[0] != 0)
      asc.setAnisoBorU(atom, anisou, 12);
    pymolScene.setAtomColor(atomColor);
    processAtom2(atom, serNo, x, y, z, formalCharge);

    // set pymolScene bit sets and create labels

    if (!bonded)
      pymolScene.bsNonbonded.set(ac);
    if (!label.equals(" ")) {
      pymolScene.bsLabeled.set(ac);
      float[] labelPos = new float[7];
      if (labelArray != null) {
        for (int i = 0; i < 7; i++)
          labelPos[i] = labelArray[apt * 7  + i];
      } else {
        Lst<Object> labelOffset = listAt(labelPositions, apt);
        if (labelOffset != null) {
          for (int i = 0; i < 7; i++)
          labelPos[i] = floatAt(labelOffset, i);
        }
      }
      pymolScene.addLabel(ac, uniqueID, atomColor, labelPos, label);
    }
    if (isHidden)
      pymolScene.bsHidden.set(ac);
    if (isNucleic)
      pymolScene.bsNucleic.set(ac);
    for (int i = 0; i < PyMOL.REP_MAX; i++)
      if (bsReps == null ? ((intReps &  (1<<i)) != 0) : bsReps.get(i))
        reps[i].set(ac);
    if (atom.elementSymbol.equals("H"))
      pymolScene.bsHydrogen.set(ac);
    if ((flags & PyMOL.FLAG_NOSURFACE) != 0)
      pymolScene.bsNoSurface.set(ac);
    atomMap[apt] = ac++;
    return null;
  }

  private boolean atomBool(byte[] atomArray, int pt, int offset, int mask) {
    return ((atomArray[pt + offset] & mask) != 0);
  }

  private float atomFloat(byte[] atomArray, int pt, int offset) {
    try {
      return BC.bytesToFloat(atomArray, pt + offset, false);
    } catch (Exception e) {
      return 0;
    }
  }

  private String atomStr(byte[] atomArray, int pt, int offset, String[] lexStr) {
    if (offset < 0)
      return  lexStr[BC.bytesToInt(atomArray, pt - offset, false)];
    String s = getCStr(atomArray, pt + offset);
    return (s.length() == 0 ? " " : s);
  }

  private int atomInt(byte[] atomArray, int pt, int offset) {
    return BC.bytesToInt(atomArray, pt + offset, false);
  }

  private void addBonds(Lst<Bond> bonds) {
    int n = bonds.size();
    for (int i = 0; i < n; i++) {
      Bond bond = bonds.get(i);
      bond.atomIndex1 = atomMap[bond.atomIndex1];
      bond.atomIndex2 = atomMap[bond.atomIndex2];
      if (bond.atomIndex1 < 0 || bond.atomIndex2 < 0)
        continue;
      pymolScene.setUniqueBond(bondCount++, bond.uniqueID);
      asc.addBond(bond);
    }
  }

  private void addMolStructures() {
    addMolSS("H", STR.HELIX);
    addMolSS("S", STR.SHEET);
    addMolSS("L", STR.TURN);
    addMolSS(" ", STR.NONE);
  }

  /**
   * Secondary structure definition.
   * 
   * @param ssType
   * @param type
   */
  private void addMolSS(String ssType, STR type) {
    if (ssMapSeq.get(ssType) == null)
      return;
    int istart = -1;
    int iend = -1;
    int ichain = 0;
    Atom[] atoms = asc.atoms;
    BS bsSeq = null;
    BS bsAtom = pymolScene.getSSMapAtom(ssType);
    int n = ac + 1;
    int seqNo = -1;
    int thischain = 0;
    int imodel = -1;
    int thisModel = -1;
    for (int i = ac0; i < n; i++) {
      if (i == ac) {
        thischain = 0;
      } else {
        seqNo = atoms[i].sequenceNumber;
        thischain = atoms[i].chainID;
        thisModel = atoms[i].atomSetIndex;
      }
      if (thischain != ichain || thisModel != imodel) {
        ichain = thischain;
        imodel = thisModel;
        bsSeq = ssMapSeq.get(ssType + thischain);
        --i; // replay this one
        if (istart < 0)
          continue;
      } else if (bsSeq != null && seqNo >= MIN_RESNO
          && bsSeq.get(seqNo - MIN_RESNO)) {
        iend = i;
        if (istart < 0)
          istart = i;
        continue;
      } else if (istart < 0) {
        continue;
      }
      if (type != STR.NONE) {
        int pt = bsStructureDefined.nextSetBit(istart);
        if (pt >= 0 && pt <= iend)
          continue;
        bsStructureDefined.setBits(istart, iend + 1);
        Structure structure = new Structure(imodel, type, type,
            type.toString(), ++structureCount, type == STR.SHEET ? 1
                : 0, null);
        Atom a = atoms[istart];
        Atom b = atoms[iend];
        int i0 = asc.getAtomSetAtomIndex(thisModel);
	        //System.out.println("addstruc " + i0 + " " + istart + " " + iend + " " + a.atomName + " " + b.atomName + " " + a.atomSerial + " " + b.atomSerial);
        structure.set(a.chainID, a.sequenceNumber, a.insertionCode, b.chainID,
            b.sequenceNumber, b.insertionCode, istart - i0, iend - i0);
        asc.addStructure(structure);
      }
      bsAtom.setBits(istart, iend + 1);
      istart = -1;
    }
  }

  /**
   * Create JmolObjects for all the molecular shapes; not executed for a state
   * script.
   * 
   */
  private void createShapeObjects() {
    pymolScene.createShapeObjects(reps, allowSurface && !isHidden, ac0, ac);
  }

  ////// end of molecule-specific JmolObjects //////

  ///// final processing /////

  /**
   * Create mesh or mep JmolObjects. Caching the volumeData, because it will be
   * needed by org.jmol.jvxl.readers.PyMOLMeshReader
   * 
   */
  private void processMeshes() {
    String fileName = vwr.fm.getFilePath(pymolScene.surfaceInfoName, true, false);
    vwr.cachePut(fileName, volumeData);
    for (int i = mapObjects.size(); --i >= 0;) {
      Lst<Object> obj = mapObjects.get(i);
      String objName = obj.get(obj.size() - 1).toString();
      boolean isMep = objName.endsWith("_e_pot");
      String mapName;
      int tok;
      if (isMep) {
        // a hack? for electrostatics2.pse
        // _e_chg (surface), _e_map (volume data), _e_pot (gadget)
        tok = T.mep;
        String root = objName.substring(0, objName.length() - 3);
        mapName = root + "map";
        String isosurfaceName = pymolScene.getObjectID(root + "chg");
        if (isosurfaceName == null)
          continue;
        obj.addLast(isosurfaceName);
        pymolScene.mepList += ";" + isosurfaceName + ";";
      } else {
        tok = T.mesh;
        mapName = stringAt(sublistAt(obj, 2, 0), 1);
      }
      Lst<Object> surface = volumeData.get(mapName);
      if (surface == null)
        continue;
      obj.addLast(mapName);
      volumeData.put(objName, obj);
      volumeData.put("__pymolSurfaceData__", obj);
      if (!isStateScript)
        pymolScene.addMesh(tok, obj, objName, isMep);
      appendLoadNote("PyMOL object " + objName + " references map " + mapName);
    }
  }


  /**
   * Create a JmolObject that will define atom sets based on PyMOL objects
   * 
   */
  private void processDefinitions() {
    String s = vwr.getAtomDefs(pymolScene.setAtomDefs());
    if (s.length() > 2)
      s = s.substring(0, s.length() - 2);
    appendLoadNote(s);
  }

  /**
   * A PyMOL scene consists of one or more of: view frame visibilities, by
   * object colors, by color reps, by type currently just extracts viewpoint
   * 
   * @param map
   */
  @SuppressWarnings("unchecked")
  private void processSelectionsAndScenes(Map<String, Object> map) {
    if (!pymolScene.needSelections())
      return;
    Map<String, Lst<Object>> htObjNames = listToMap(getMapList(
        map, "names"));
    if (haveScenes) {
      Map<String, Object> scenes = (Map<String, Object>) map.get("scene_dict");
      finalizeSceneData();
      Map<String, Lst<Object>> htSecrets = listToMap(getMapList(map, "selector_secrets"));
      for (int i = 0; i < sceneOrder.size(); i++) {
        String name = stringAt(sceneOrder, i);
        Lst<Object> thisScene = getMapList(scenes, name);
        if (thisScene == null)
          continue;
        pymolScene.buildScene(name, thisScene, htObjNames, htSecrets);
        appendLoadNote("scene: " + name);
      }
    }
    pymolScene.setCarveSets(htObjNames);
  }

  ////////////////// set the rendering ////////////////

  /**
   * Make sure atom uniqueID (vectorX) and cartoonType (vectorY) are made
   * permanent
   */
  private void finalizeSceneData() {
    int[] cartoonTypes = new int[ac];
    int[] uniqueIDs = new int[ac];
    int[] sequenceNumbers = new int[ac];
    boolean[] newChain = new boolean[ac];
    float[] radii = new float[ac];
    int lastAtomChain = Integer.MIN_VALUE;
    int lastAtomSet = Integer.MIN_VALUE;
    for (int i = 0; i < ac; i++) {
      cartoonTypes[i] = getCartoonType(i);
      uniqueIDs[i] = getUniqueID(i);
      sequenceNumbers[i] = getSequenceNumber(i);
      radii[i] = getVDW(i);
      if (lastAtomChain != atoms[i].chainID
          || lastAtomSet != atoms[i].atomSetIndex) {
        newChain[i] = true;
        lastAtomChain = atoms[i].chainID;
        lastAtomSet = atoms[i].atomSetIndex;
      }
    }
    pymolScene.setAtomInfo(uniqueIDs, cartoonTypes, sequenceNumbers,
        newChain, radii);
  }

  // generally useful static methods

  static int intAt(Lst<Object> list, int i) {
    // PyMOL 1.7.5 may simply have a null list for a setting
    return (list == null ? -1 : ((Number) list.get(i)).intValue());
  }

  static P3 pointAt(Lst<Object> list, int i, P3 pt) {
    pt.set(floatAt(list, i++), floatAt(list, i++), floatAt(list, i));
    return pt;
  }

  static float[] floatsAt(Lst<Object> a, int pt, float[] data, int len) {
    if (a == null)
      return null;
    for (int i = 0; i < len; i++)
      data[i] = floatAt(a, pt++);
    return data;
  }

  static float floatAt(Lst<Object> list, int i) {
    return (list == null || i >= list.size() ? 0 : ((Number) list.get(i)).floatValue());
  }

  @SuppressWarnings("unchecked")
  static Lst<Object> listAt(Lst<Object> list, int i) {
    if (list == null || i >= list.size())
      return null;
    Object o = list.get(i);
    return (o instanceof Lst<?> ? (Lst<Object>) o : null);
  }
  
  @SuppressWarnings("unchecked")
  public static Lst<Object> sublistAt(Lst<Object> mesh, int... pt) {
    for (int i = 0; i < pt.length; i++)
      mesh = (Lst<Object>) mesh.get(pt[i]);
    return mesh;
  }



  /**
   * return a map of lists of the type: [ [name1,...], [name2,...], ...]
   * 
   * @param list
   * @return Hashtable
   */
  static Map<String, Lst<Object>> listToMap(Lst<Object> list) {
    Hashtable<String, Lst<Object>> map = new Hashtable<String, Lst<Object>>();
    for (int i = list.size(); --i >= 0;) {
      Lst<Object> item = listAt(list, i);
      if (item != null && item.size() > 0)
        map.put(stringAt(item, 0), item);
    }
    return map;
  }

  static String stringAt(Lst<Object> list, int i) {
    byte[] a = (byte[]) list.get(i);
    return (a.length == 0 ? " " : bytesToString(a));
  }

  static String bytesToString(Object object) {
    try {
      return new String((byte[]) object, "UTF-8");
    } catch (Exception e) {
      return object.toString();
    }
  }

  static int colorSettingClamped(Lst<Object> c, P3 ptTemp) {
    return getColorPt(c.get(c.size() < 6 || intAt(c, 4) == 0 ? 2 : 5), ptTemp);
  }

  @SuppressWarnings("unchecked")
  static int getColorPt(Object o, P3 ptTemp) {
    return (o == null ? 0 : o instanceof Integer ? ((Integer) o).intValue() : CU
        .colorPtToFFRGB(PyMOLReader.pointAt((Lst<Object>) o, 0, ptTemp)));
  }

  @SuppressWarnings("unchecked")
  private static Lst<Object> getMapList(Map<String, Object> map, String key) {
    return (Lst<Object>) map.get(key);
  }

  private static BS getBsReps(Lst<Object> list) {
    if (list == null)
      return null;
    BS bsReps = new BS();
    int n = Math.min(list.size(), PyMOL.REP_MAX);
    for (int i = 0; i < n; i++) {
      if (intAt(list, i) == 1)
        bsReps.set(i);
    }
    return bsReps;
  }

  /// PymolAtomReader interface
  
  @Override
  public int getUniqueID(int iAtom) {
    return (int) atoms[iAtom].vib.x;
  }

  @Override
  public int getCartoonType(int iAtom) {
    return (int) atoms[iAtom].vib.y;
  }

  @Override
  public float getVDW(int iAtom) {
    return atoms[iAtom].radius;
  }

  @Override
  public int getSequenceNumber(int iAtom) {
    return atoms[iAtom].sequenceNumber;
  }

  @Override
  public boolean compareAtoms(int iPrev, int i) {
    return atoms[iPrev].chainID != atoms[i].chainID;
  }

}
