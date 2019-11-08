package org.jmol.viewer;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javajs.util.DF;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.SB;

import org.jmol.c.CBK;
import org.jmol.c.STR;
import org.jmol.script.SV;
import org.jmol.script.T;
import org.jmol.util.Elements;
import org.jmol.util.Escape;
import org.jmol.util.Logger;

public class GlobalSettings {

  private final Viewer vwr;

  Map<String, Object> htNonbooleanParameterValues;
  Map<String, Boolean> htBooleanParameterFlags;
  Map<String, Boolean> htPropertyFlagsRemoved;
  Map<String, SV> htUserVariables = new Hashtable<String, SV>();
  

  /*
   *  Mostly these are just saved and restored directly from Viewer.
   *  They are collected here for reference and to ensure that no 
   *  methods are written that bypass vwr's get/set methods.
   *  
   *  Because these are not Frame variables, they (mostly) should persist past
   *  a new file loading. There is some question in my mind whether all
   *  should be in this category.
   *  
   */

  GlobalSettings(Viewer vwr, GlobalSettings g, boolean clearUserVariables) {
    this.vwr = vwr;
    htNonbooleanParameterValues = new Hashtable<String, Object>();
    htBooleanParameterFlags = new Hashtable<String, Boolean>();
    htPropertyFlagsRemoved = new Hashtable<String, Boolean>();
    if (g != null) {
      //persistent values not reset with the "initialize" command
      if (!clearUserVariables) {
        setO("_pngjFile", g.getParameter("_pngjFile", false));
        htUserVariables = g.htUserVariables; // 12.3.7, 12.2.7
      }
      
      debugScript = g.debugScript;
      disablePopupMenu = g.disablePopupMenu;
      messageStyleChime = g.messageStyleChime;
      defaultDirectory = g.defaultDirectory;
      autoplayMovie = g.autoplayMovie;
      allowAudio = g.allowAudio;
      allowGestures = g.allowGestures;
      allowModelkit = g.allowModelkit;
      allowMultiTouch = g.allowMultiTouch;
      allowKeyStrokes = g.allowKeyStrokes;
      legacyAutoBonding = g.legacyAutoBonding;
      legacyHAddition = g.legacyHAddition;
      legacyJavaFloat = g.legacyJavaFloat;
      bondingVersion = g.bondingVersion;
      platformSpeed = g.platformSpeed;
      useScriptQueue = g.useScriptQueue;
      //useArcBall = g.useArcBall;
      showTiming = g.showTiming;
      wireframeRotation = g.wireframeRotation;
      testFlag1 = g.testFlag1;
      testFlag2 = g.testFlag2;
      testFlag3 = g.testFlag3;
      testFlag4 = g.testFlag4;
    }
    loadFormat = pdbLoadFormat = JC.databases.get("pdb");
    pdbLoadLigandFormat = JC.databases.get("ligand");
    nmrUrlFormat = JC.databases.get("nmr");
    nmrPredictFormat = JC.databases.get("nmrdb");
    smilesUrlFormat = JC.databases.get("nci") + "/file?format=sdf&get3d=true";
    nihResolverFormat = JC.databases.get("nci");
    pubChemFormat = JC.databases.get("pubchem");
    macroDirectory = JC.defaultMacroDirectory;

    // beyond these six, they are just in the form load =xxx/id

    for (CBK item : CBK.values())
      resetValue(item.name() + "Callback", g);

    // These next are just placeholders so that the math processor
    // knows they are Jmol variables. They are held by other managers.
    // This is NOT recommended, because it is easy to forget they are 
    // here and then not reset them properly. Basically it means that
    // the other manager must ensure that the value changed there is
    // updated here, AND when an initialization occurs, they remain in
    // sync. This is difficult to manage and should be changed.
    // The good news is that this manager is initialized FIRST, so 
    // we really just have to make sure that all these values are definitely
    // also initialized within the managers. 

    setF("cameraDepth", TransformManager.DEFAULT_CAMERA_DEPTH);
    setI("contextDepthMax", 100); // maintained by ScriptEval
    setI("depth", 0); // maintained by TransformManager
    setF("gestureSwipeFactor", ActionManager.DEFAULT_GESTURE_SWIPE_FACTOR);
    setB("hideNotSelected", false); //maintained by the selectionManager
    setI("historyLevel", 0); // maintained by ScriptEval
    setO("hoverLabel", ""); // maintained by the Hover shape
    setB("isKiosk", vwr.isKiosk()); // maintained by Viewer
    setO("logFile", vwr.getLogFileName()); // maintained by Viewer
    setI("logLevel", Logger.getLogLevel());
    setF("mouseWheelFactor", ActionManager.DEFAULT_MOUSE_WHEEL_FACTOR);
    setF("mouseDragFactor", ActionManager.DEFAULT_MOUSE_DRAG_FACTOR);
    setI("navFps", TransformManager.DEFAULT_NAV_FPS);
    setI("navigationDepth", 0); // maintained by TransformManager
    setI("navigationSlab", 0); // maintained by TransformManager
    setI("navX", 0); // maintained by TransformManager
    setI("navY", 0); // maintained by TransformManager
    setI("navZ", 0); // maintained by TransformManager
    setO("pathForAllFiles", "");
    setB("perspectiveDepth", TransformManager.DEFAULT_PERSPECTIVE_DEPTH);
    setI("perspectiveModel", TransformManager.DEFAULT_PERSPECTIVE_MODEL);
    setO("picking", "identify"); // maintained by ActionManager
    setO("pickingStyle", "toggle"); // maintained by ActionManager
    setB("refreshing", true); // maintained by Viewer
    setI("rotationRadius", 0); // maintained by TransformManager
    setI("scaleAngstromsPerInch", 0); // maintained by TransformManager
    setI("scriptReportingLevel", 0); // maintained by ScriptEval
    setB("selectionHalos", false); // maintained by ModelSet
    setB("showaxes", false); // maintained by Axes
    setB("showboundbox", false); // maintained by Bbcage
    setB("showfrank", false); // maintained by Viewer
    setB("showUnitcell", false); // maintained by Uccage
    setI("slab", 100); // maintained by TransformManager
    setB("slabEnabled", false); // maintained by TransformManager     
    setF("slabrange", 0f); // maintained by TransformManager
    setI("spinX", 0); // maintained by TransformManager
    setI("spinY", TransformManager.DEFAULT_SPIN_Y);
    setI("spinZ", 0); // maintained by TransformManager
    setI("spinFps", TransformManager.DEFAULT_SPIN_FPS);
    setF("visualRange", TransformManager.DEFAULT_VISUAL_RANGE);
    setI("stereoDegrees", TransformManager.DEFAULT_STEREO_DEGREES);
    //setI("stateversion", 0); // only set by a saved state being recalled
    setB("syncScript", vwr.sm.syncingScripts);
    setB("syncMouse", vwr.sm.syncingMouse);
    setB("syncStereo", vwr.sm.stereoSync);
    setB("windowCentered", true); // maintained by TransformManager
    setB("zoomEnabled", true); // maintained by TransformManager

    // These next values have no other place than the global Hashtables.
    // This just means that a call to vwr.getXxxxProperty() is necessary.
    // Otherwise, it's the same as if they had a global variable. 
    // It's just an issue of speed of access. Generally, these should only be
    // accessed by the user. 

    setI("_version", JC.versionInt);
    setO("_versionDate", Viewer.getJmolVersion());

    setB("axesWindow", true);
    setB("axesMolecular", false);
    setB("axesPosition", false);
    setB("axesUnitcell", false);
    setI("backgroundModel", 0);
    setB("colorRasmol", false);
    setO("currentLocalPath", "");
    setO("defaultLattice", "{0 0 0}");
    setO("defaultColorScheme", "Jmol");
    setO("defaultDirectoryLocal", "");
    setO("defaults", "Jmol");
    setO("defaultVDW", "Jmol");
    setO("exportDrivers", JC.EXPORT_DRIVER_LIST);
    setI("propertyAtomNumberColumnCount", 0);
    setI("propertyAtomNumberField", 0);
    setI("propertyDataColumnCount", 0);
    setI("propertyDataField", 0);
    setB("undo", true);

    // OK, all of the rest of these are maintained here as global values (below)

    setB("allowEmbeddedScripts", allowEmbeddedScripts);
    setB("allowGestures", allowGestures);
    setB("allowKeyStrokes", allowKeyStrokes);
    setB("allowModelkit", allowModelkit);
    setB("allowMultiTouch", allowMultiTouch);
    setB("allowRotateSelected", allowRotateSelected);
    setB("allowMoveAtoms", allowMoveAtoms);
    setI("animationFps", animationFps);
    setB("antialiasImages", antialiasImages);
    setB("antialiasDisplay", antialiasDisplay);
    setB("antialiasTranslucent", antialiasTranslucent);
    setB("appendNew", appendNew);
    setO("appletProxy", appletProxy);
    setB("applySymmetryToBonds", applySymmetryToBonds);
    setB("atomPicking", atomPicking);
    setO("atomTypes", atomTypes);
    setB("autoBond", autoBond);
    setB("autoFps", autoFps);
    //      setParameterValue("autoLoadOrientation", autoLoadOrientation);
    setI("axesMode", axesMode == T.axesunitcell ? 2
        : axesMode == T.axesmolecular ? 1 : 0);
    setF("axesScale", axesScale);
    setF("axesOffset", axesOffset);
    setB("axesOrientationRasmol", axesOrientationRasmol);
    setF("cartoonBlockHeight", cartoonBlockHeight);
    setB("cartoonBlocks", cartoonBlocks);
    setB("cartoonSteps", cartoonSteps);
    setB("bondModeOr", bondModeOr);
    setB("bondPicking", bondPicking);
    setI("bondRadiusMilliAngstroms", bondRadiusMilliAngstroms);
    setF("bondTolerance", bondTolerance);
    setB("cartoonBaseEdges", cartoonBaseEdges);
    setB("cartoonFancy", cartoonFancy);
    setB("cartoonLadders", cartoonLadders);
    setB("cartoonLadders", cartoonRibose);
    setB("cartoonRockets", cartoonRockets);
    setB("chainCaseSensitive", chainCaseSensitive);
    setB("cipRule6Full", cipRule6Full);
    setI("bondingVersion", bondingVersion);
    setO("dataSeparator", dataSeparator);
    setB("debugScript", debugScript);
    setO("defaultAngleLabel", defaultAngleLabel);
    setF("defaultDrawArrowScale", defaultDrawArrowScale);
    setO("defaultDirectory", defaultDirectory);
    setO("defaultDistanceLabel", defaultDistanceLabel);
    setO("defaultDropScript", defaultDropScript);
    setO("defaultLabelPDB", defaultLabelPDB);
    setO("defaultLabelXYZ", defaultLabelXYZ);
    setO("defaultLoadFilter", defaultLoadFilter);
    setO("defaultLoadScript", defaultLoadScript);
    setB("defaultStructureDSSP", defaultStructureDSSP);
    setO("defaultTorsionLabel", defaultTorsionLabel);
    setF("defaultTranslucent", defaultTranslucent);
    setI("delayMaximumMs", delayMaximumMs);
    setF("dipoleScale", dipoleScale);
    setB("disablePopupMenu", disablePopupMenu);
    setB("displayCellParameters", displayCellParameters);
    setI("dotDensity", dotDensity);
    setI("dotScale", dotScale);
    setB("dotsSelectedOnly", dotsSelectedOnly);
    setB("dotSurface", dotSurface);
    setB("dragSelected", dragSelected);
    setB("drawHover", drawHover);
    setF("drawFontSize", drawFontSize);
    setB("drawPicking", drawPicking);
    setB("dsspCalculateHydrogenAlways", dsspCalcHydrogen);
//    setO("edsUrlFormat", edsUrlFormat);
//    setO("edsUrlFormatDiff", edsUrlFormatDiff);
//    //setParameterValue("edsUrlOptions", edsUrlOptions);
//    setO("edsUrlCutoff", edsUrlCutoff);
    setB("ellipsoidArcs", ellipsoidArcs);
    setB("ellipsoidArrows", ellipsoidArrows);
    setB("ellipsoidAxes", ellipsoidAxes);
    setF("ellipsoidAxisDiameter", ellipsoidAxisDiameter);
    setB("ellipsoidBall", ellipsoidBall);
    setI("ellipsoidDotCount", ellipsoidDotCount);
    setB("ellipsoidDots", ellipsoidDots);
    setB("ellipsoidFill", ellipsoidFill);
    setO("energyUnits", energyUnits);
    //      setParameterValue("_fileCaching", _fileCaching);
    //      setParameterValue("_fileCache", _fileCache);
    setF("exportScale", exportScale);
    setB("fontScaling", fontScaling);
    setB("fontCaching", fontCaching);
    setB("forceAutoBond", forceAutoBond);
    setO("forceField", forceField);
    setB("fractionalRelative", fractionalRelative);
    setF("particleRadius", particleRadius);
    setB("greyscaleRendering", greyscaleRendering);
    setF("hbondsAngleMinimum", hbondsAngleMinimum);
    setF("hbondsDistanceMaximum", hbondsDistanceMaximum);
    setB("hbondsBackbone", hbondsBackbone);
    setB("hbondsRasmol", hbondsRasmol);
    setB("hbondsSolid", hbondsSolid);
    setI("helixStep", helixStep);
    setO("helpPath", helpPath);
    setI("hermiteLevel", hermiteLevel);
    setB("hideNameInPopup", hideNameInPopup);
    setB("hideNavigationPoint", hideNavigationPoint);
    setB("hiddenLinesDashed", hiddenLinesDashed);
    setB("highResolution", highResolutionFlag);
    setF("hoverDelay", hoverDelayMs / 1000f);
    setB("imageState", imageState);
    setI("infoFontSize", infoFontSize);
    setB("isosurfaceKey", isosurfaceKey);
    setB("isosurfacePropertySmoothing", isosurfacePropertySmoothing);
    setI("isosurfacePropertySmoothingPower", isosurfacePropertySmoothingPower);
    setB("jmolInJSpecView", jmolInJSpecView);
    setB("justifyMeasurements", justifyMeasurements);
    setB("legacyAutoBonding", legacyAutoBonding);
    setB("legacyHAddition", legacyHAddition);
    setB("legacyJavaFloat", legacyJavaFloat);
    setF("loadAtomDataTolerance", loadAtomDataTolerance);
    setO("loadFormat", loadFormat);
    setO("loadLigandFormat", pdbLoadLigandFormat);
    setB("logCommands", logCommands);
    setB("logGestures", logGestures);
    setB("measureAllModels", measureAllModels);
    setB("measurementLabels", measurementLabels);
    setO("measurementUnits", measureDistanceUnits);
    setI("meshScale", meshScale);
    setB("messageStyleChime", messageStyleChime);
    setF("minBondDistance", minBondDistance);
    setI("minPixelSelRadius", minPixelSelRadius);
    setI("minimizationSteps", minimizationSteps);
    setB("minimizationRefresh", minimizationRefresh);
    setB("minimizationSilent", minimizationSilent);
    setF("minimizationCriterion", minimizationCriterion);
    setB("modelKitMode", modelKitMode);
    setF("modulationScale", modulationScale);
    setB("monitorEnergy", monitorEnergy);
    setF("multipleBondRadiusFactor", multipleBondRadiusFactor);
    setB("multipleBondBananas", multipleBondBananas);
    setF("multipleBondSpacing", multipleBondSpacing);
    setB("multiProcessor", multiProcessor && (Viewer.nProcessors > 1));
    setB("navigationMode", navigationMode);
    //setParamB("navigateSurface", navigateSurface);
    setB("navigationPeriodic", navigationPeriodic);
    setF("navigationSpeed", navigationSpeed);
    setB("nboCharges", nboCharges);
    setB("noDelay", noDelay);
    setO("nmrPredictFormat", nmrPredictFormat);
    setO("nmrUrlFormat", nmrUrlFormat);
    setB("partialDots", partialDots);
    setB("pdbAddHydrogens", pdbAddHydrogens); // new 12.1.51
    setB("pdbGetHeader", pdbGetHeader); // new 11.5.39
    setB("pdbSequential", pdbSequential); // new 11.5.39
    setI("percentVdwAtom", percentVdwAtom);
    setI("pickingSpinRate", pickingSpinRate);
    setO("pickLabel", pickLabel);
    setI("platformSpeed", platformSpeed);
    setF("pointGroupLinearTolerance", pointGroupLinearTolerance);
    setF("pointGroupDistanceTolerance", pointGroupDistanceTolerance);
    setB("preserveState", preserveState);
    setO("propertyColorScheme", propertyColorScheme);
    setO("quaternionFrame", quaternionFrame);
    setB("rangeSelected", rangeSelected);
    setI("repaintWaitMs", repaintWaitMs);
    setI("ribbonAspectRatio", ribbonAspectRatio);
    setB("ribbonBorder", ribbonBorder);
    setB("rocketBarrels", rocketBarrels);
    setB("saveProteinStructureState", saveProteinStructureState);
    setB("scriptqueue", useScriptQueue);
    setB("selectAllModels", selectAllModels);
    setB("selectHetero", rasmolHeteroSetting);
    setB("selectHydrogen", rasmolHydrogenSetting);
    setF("sheetSmoothing", sheetSmoothing);
    setB("showHiddenSelectionHalos", showHiddenSelectionHalos);
    setB("showHydrogens", showHydrogens);
    setB("showKeyStrokes", showKeyStrokes);
    setB("showMeasurements", showMeasurements);
    setB("showModulationVectors", showModVecs);
    setB("showMultipleBonds", showMultipleBonds);
    setB("showNavigationPointAlways", showNavigationPointAlways);
    setI("showScript", scriptDelay);
    setB("showtiming", showTiming);
    setB("slabByMolecule", slabByMolecule);
    setB("slabByAtom", slabByAtom);
    setB("smartAromatic", smartAromatic);
    setI("smallMoleculeMaxAtoms", smallMoleculeMaxAtoms);
    setO("smilesUrlFormat", smilesUrlFormat);
    setO("macroDirectory", macroDirectory);
    setO("nihResolverFormat", nihResolverFormat);
    setO("pubChemFormat", pubChemFormat);
    setB("showUnitCellDetails", showUnitCellDetails);
    setB("solventProbe", solventOn);
    setF("solventProbeRadius", solventProbeRadius);
    setB("ssbondsBackbone", ssbondsBackbone);
    setF("starWidth", starWidth);
    setB("statusReporting", statusReporting);
    setI("strandCount", strandCountForStrands);
    setI("strandCountForStrands", strandCountForStrands);
    setI("strandCountForMeshRibbon", strandCountForMeshRibbon);
    setF("strutDefaultRadius", strutDefaultRadius);
    setF("strutLengthMaximum", strutLengthMaximum);
    setI("strutSpacing", strutSpacing);
    setB("strutsMultiple", strutsMultiple);
    setB("testFlag1", testFlag1);
    setB("testFlag2", testFlag2);
    setB("testFlag3", testFlag3);
    setB("testFlag4", testFlag4);
    setB("traceAlpha", traceAlpha);
    setB("translucent", translucent);
    setB("twistedSheets", twistedSheets);
    //setB("useArcBall", useArcBall);
    setB("useMinimizationThread", useMinimizationThread);
    setB("useNumberLocalization", useNumberLocalization);
    setB("vectorsCentered", vectorsCentered);
    setF("vectorScale", vectorScale);
    setB("vectorSymmetry", vectorSymmetry);
    setI("vectorTrail", vectorTrail);
    setF("vibrationPeriod", vibrationPeriod);
    setF("vibrationScale", vibrationScale);
    setB("waitForMoveTo", waitForMoveTo);
    setB("wireframeRotation", wireframeRotation);
    setI("zDepth", zDepth);
    setB("zeroBasedXyzRasmol", zeroBasedXyzRasmol);
    setB("zoomHeight", zoomHeight);
    setB("zoomLarge", zoomLarge);
    setI("zShadePower", zShadePower);
    setI("zSlab", zSlab);
  }

  void clear() {
    Iterator<String> e = htUserVariables.keySet().iterator();
    while (e.hasNext()) {
      String key = e.next();
      if (key.charAt(0) == '@' || key.startsWith("site_"))
        e.remove();
    }

    // PER-zap settings made
    vwr.setPicked(-1, false);
    setI("_atomhovered", -1);
    setO("_pickinfo", "");
    setB("selectionhalos", false);
    setB("hidenotselected", false); // to synchronize with selectionManager
    setB("measurementlabels", measurementLabels = true);
    setB("drawHover", drawHover = false);
    vwr.stm.saveScene("DELETE", null);
  }

  int zDepth = 0;
  int zShadePower = 3; // increased to 3 from 1 for Jmol 12.1.49
  int zSlab = 50; // increased to 50 from 0 in Jmol 12.3.6 and Jmol 12.2.6

  boolean slabByMolecule = false;
  boolean slabByAtom = false;

  //file loading

  boolean allowEmbeddedScripts = true;
  public boolean appendNew = true;
  String appletProxy = "";
  boolean applySymmetryToBonds = false; //new 11.1.29
  String atomTypes = "";
  boolean autoBond = true;
  //    boolean autoLoadOrientation = false; // 11.7.30 for Spartan and Sygress/CAChe loading with or without rotation
  // starting with Jmol 12.0.RC10, this setting is ignored, and FILTER "NoOrient" is required if the file
  // is to be loaded without reference to the orientation saved in the file.
  boolean axesOrientationRasmol = false;
  short bondRadiusMilliAngstroms = JC.DEFAULT_BOND_MILLIANGSTROM_RADIUS;
  float bondTolerance = JC.DEFAULT_BOND_TOLERANCE;
  String defaultDirectory = "";
  boolean defaultStructureDSSP = true; // Jmol 12.1.15
  final P3 ptDefaultLattice = new P3();
  public String defaultLoadScript = "";
  public String defaultLoadFilter = "";
  public String defaultDropScript = JC.DEFAULT_DRAG_DROP_SCRIPT;
  //    boolean _fileCaching = false;
  //    String _fileCache = "";
  boolean forceAutoBond = false;
  boolean fractionalRelative = true;// true: {1/2 1/2 1/2} relative to current (possibly offset) unit cell 
  char inlineNewlineChar = '|'; //pseudo static
  String loadFormat, pdbLoadFormat, pdbLoadLigandFormat,
      nmrUrlFormat, nmrPredictFormat, smilesUrlFormat, nihResolverFormat,
      pubChemFormat, macroDirectory;

//  String edsUrlFormat = "http://eds.bmc.uu.se/eds/dfs/%c2%c3/%file/%file.omap";
//  String edsUrlFormatDiff = "http://eds.bmc.uu.se/eds/dfs/%c2%c3/%file/%file_diff.omap";
//  String edsUrlCutoff = "http://eds.bmc.uu.se/eds/dfs/%c2%c3/%file/%file.sfdat";
  // not implemented String edsUrlOptions = "within 2.0 {*}";
  float minBondDistance = JC.DEFAULT_MIN_BOND_DISTANCE;
  int minPixelSelRadius = 6;
  boolean pdbAddHydrogens = false; // true to add hydrogen atoms
  boolean pdbGetHeader = false; // true to get PDB header in auxiliary info
  boolean pdbSequential = false; // true for no bonding check
  int percentVdwAtom = JC.DEFAULT_PERCENT_VDW_ATOM;
  int smallMoleculeMaxAtoms = 40000;
  boolean smartAromatic = true;
  boolean zeroBasedXyzRasmol = false;
  boolean legacyAutoBonding = false;
  public boolean legacyHAddition = false;
  public boolean legacyJavaFloat = false; // float/double issue with crystallographic symmetry before Jmol 14.2.5
  boolean jmolInJSpecView = true;

  boolean modulateOccupancy = true;

  //centering and perspective

  boolean allowRotateSelected = false;
  boolean allowMoveAtoms = false;

  //solvent

  boolean solventOn = false;

  //measurements

  String defaultAngleLabel = "%VALUE %UNITS";
  String defaultDistanceLabel = "%VALUE %UNITS"; //also %_ and %a1 %a2 %m1 %m2, etc.
  String defaultTorsionLabel = "%VALUE %UNITS";
  boolean justifyMeasurements = false;
  boolean measureAllModels = false;

  // minimization  // 11.5.21 03/2008

  int minimizationSteps = 100;
  boolean minimizationRefresh = true;
  boolean minimizationSilent = false;
  float minimizationCriterion = 0.001f;

  //rendering

  int infoFontSize = 20;
  boolean antialiasDisplay = false;
  boolean antialiasImages = true;
  boolean imageState = true;
  boolean antialiasTranslucent = true;
  boolean displayCellParameters = true;
  boolean dotsSelectedOnly = false;
  boolean dotSurface = true;
  int dotDensity = 3;
  int dotScale = 1;
  int meshScale = 1;
  boolean greyscaleRendering = false;
  boolean isosurfaceKey = false;
  boolean isosurfacePropertySmoothing = true;
  int isosurfacePropertySmoothingPower = 7;
  int platformSpeed = 10; // 1 (slow) to 10 (fast)
  public int repaintWaitMs = 1000;
  boolean showHiddenSelectionHalos = false;
  boolean showKeyStrokes = true;
  boolean showMeasurements = true;
  public boolean showTiming = false;
  boolean zoomLarge = true; //false would be like Chime
  boolean zoomHeight = false; // true would be like PyMOL
  String backgroundImageFileName;

  //atoms and bonds

  boolean partialDots = false;
  boolean bondModeOr = false;
  boolean hbondsBackbone = false;
  float hbondsAngleMinimum = 90f;
  float hbondsDistanceMaximum = 3.25f;
  boolean hbondsRasmol = true; // 12.0.RC3
  boolean hbondsSolid = false;
  public byte modeMultipleBond = JC.MULTIBOND_NOTSMALL;
  boolean showHydrogens = true;
  boolean showMultipleBonds = true;
  boolean ssbondsBackbone = false;
  float multipleBondSpacing = -1; // 0.35?
  float multipleBondRadiusFactor = 0; // 0.75?
  boolean multipleBondBananas = false;
  boolean nboCharges = true;
  
  //secondary structure + Rasmol

  boolean cartoonBaseEdges = false;
  boolean cartoonRockets = false;
  float cartoonBlockHeight = 0.5f;
  boolean cartoonBlocks = false;
  boolean cartoonSteps = false;
  boolean cartoonFancy = false;
  boolean cartoonLadders = false;
  boolean cartoonRibose = false;
  boolean chainCaseSensitive = false;
  boolean cipRule6Full = false;
  int hermiteLevel = 0;
  boolean highResolutionFlag = false;
  public boolean rangeSelected = false;
  boolean rasmolHydrogenSetting = true;
  boolean rasmolHeteroSetting = true;
  int ribbonAspectRatio = 16;
  boolean ribbonBorder = false;
  boolean rocketBarrels = false;
  float sheetSmoothing = 1; // 0: traceAlpha on alphas for helix, 1 on midpoints
  boolean traceAlpha = true;
  boolean translucent = true;
  boolean twistedSheets = false;

  //misc

  boolean autoplayMovie = true;
  boolean allowAudio = true; // once turned off, cannot be turned back on
  boolean allowGestures = false;
  boolean allowModelkit = true;
  boolean allowMultiTouch = true; // but you still need to set the parameter multiTouchSparshUI=true
  boolean allowKeyStrokes = false;
  
  boolean hiddenLinesDashed = false;
  
  int animationFps = 10;
  boolean atomPicking = true;
  boolean autoFps = false;
  public int axesMode = T.axeswindow;
  float axesScale = 2;
  float axesOffset = 0;
  float starWidth = 0.05f;
  boolean bondPicking = false;
  String dataSeparator = "~~~";
  boolean debugScript = false;
  float defaultDrawArrowScale = 0.5f;
  String defaultLabelXYZ = "%a";
  String defaultLabelPDB = "%m%r";
  float defaultTranslucent = 0.5f;
  int delayMaximumMs = 0;
  float dipoleScale = 1f;
  float drawFontSize = 14f;
  boolean disablePopupMenu = false;
  boolean dragSelected = false;
  boolean drawHover = false;
  boolean drawPicking = false;
  boolean dsspCalcHydrogen = true;
  public String energyUnits = "kJ";
  float exportScale = 0f;
  String helpPath = JC.DEFAULT_HELP_PATH;
  boolean fontScaling = false;
  boolean fontCaching = true;
  String forceField = "MMFF";
  int helixStep = 1;
  boolean hideNameInPopup = false;
  int hoverDelayMs = 500;
  float loadAtomDataTolerance = 0.01f;
  public boolean logCommands = false;
  public boolean logGestures = false;
  public String measureDistanceUnits = "nanometers";
  boolean measurementLabels = true;
  boolean messageStyleChime = false;
  boolean monitorEnergy = false;
  public float modulationScale = 1;
  boolean multiProcessor = true;
  float particleRadius = 20;
  int pickingSpinRate = 10;
  String pickLabel = "";
  float pointGroupDistanceTolerance = 0.2f;
  float pointGroupLinearTolerance = 8.0f;
  public boolean preserveState = true;
  String propertyColorScheme = "roygb";
  String quaternionFrame = "p"; // was c prior to Jmol 11.7.47
  boolean saveProteinStructureState = true;
  boolean showModVecs = false;
  boolean showUnitCellDetails = true;
  float solventProbeRadius = 1.2f;
  int scriptDelay = 0;
  boolean selectAllModels = true;
  boolean statusReporting = true;
  int strandCountForStrands = 5;
  int strandCountForMeshRibbon = 7;
  int strutSpacing = 6;
  float strutLengthMaximum = 7.0f;
  float strutDefaultRadius = JC.DEFAULT_STRUT_RADIUS;
  boolean strutsMultiple = false; //on a single position    
  //boolean useArcBall = false;
  boolean useMinimizationThread = true;
  boolean useNumberLocalization = true;
  public boolean useScriptQueue = true;
  public boolean waitForMoveTo = true; // Jmol 11.9.24
  /**
   * 
   * ensures that ScriptManager.allowJSThreads is false
   * so that ScriptManager.useThreads() returns false; 
   * 
   * Jmol 14.21.1 
   * 
   */
  public boolean noDelay = false;
  float vectorScale = 1f;
  boolean vectorSymmetry = false; // Jmol 12.3.2
  boolean vectorsCentered = false; // Jmol 14.1.14
  int vectorTrail = 0; // Jmol 14.4.4
  float vibrationPeriod = 1f;
  float vibrationScale = 1f;
  boolean wireframeRotation = false;

  // window

  boolean hideNavigationPoint = false;
  boolean navigationMode = false;
  //boolean navigateSurface = false;
  boolean navigationPeriodic = false;
  float navigationSpeed = 5;
  boolean showNavigationPointAlways = false;
  String stereoState = null;
  boolean modelKitMode = false;

  // special persistent object characteristics -- bbcage, uccage, axes:

  int[] objColors = new int[StateManager.OBJ_MAX];
  boolean[] objStateOn = new boolean[StateManager.OBJ_MAX];
  int[] objMad10 = new int[StateManager.OBJ_MAX];

  boolean ellipsoidAxes = false;
  boolean ellipsoidDots = false;
  boolean ellipsoidArcs = false;
  boolean ellipsoidArrows = false;
  boolean ellipsoidFill = false;
  boolean ellipsoidBall = true;

  int ellipsoidDotCount = 200;
  float ellipsoidAxisDiameter = 0.02f;

  //testing

  boolean testFlag1 = false;
  boolean testFlag2 = false;
  boolean testFlag3 = false;
  boolean testFlag4 = false;

  //controlled access:

  void setUnits(String units) {
    String mu = measureDistanceUnits;
    String eu = energyUnits;
    if (units.equalsIgnoreCase("angstroms"))
      measureDistanceUnits = "angstroms";
    else if (units.equalsIgnoreCase("nanometers")
        || units.equalsIgnoreCase("nm"))
      measureDistanceUnits = "nanometers";
    else if (units.equalsIgnoreCase("picometers")
        || units.equalsIgnoreCase("pm"))
      measureDistanceUnits = "picometers";
    else if (units.equalsIgnoreCase("bohr") || units.equalsIgnoreCase("au"))
      measureDistanceUnits = "au";
    else if (units.equalsIgnoreCase("vanderwaals")
        || units.equalsIgnoreCase("vdw"))
      measureDistanceUnits = "vdw";
    else if (units.toLowerCase().endsWith("hz")
        || units.toLowerCase().endsWith("khz"))
      measureDistanceUnits = units.toLowerCase();
    else if (units.equalsIgnoreCase("kj"))
      energyUnits = "kJ";
    else if (units.equalsIgnoreCase("kcal"))
      energyUnits = "kcal";
    if (!mu.equalsIgnoreCase(measureDistanceUnits))
      setO("measurementUnits", measureDistanceUnits);
    else if (!eu.equalsIgnoreCase(energyUnits))
      setO("energyUnits", energyUnits);
  }

  boolean isJmolVariable(String key) {
    return key.charAt(0) == '_'
        || htNonbooleanParameterValues.containsKey(key = key.toLowerCase())
        || htBooleanParameterFlags.containsKey(key)
        || unreportedProperties.indexOf(";" + key + ";") >= 0;
  }

  private void resetValue(String name, GlobalSettings g) {
    setO(name, g == null ? "" : (String) g.getParameter(name, true));
  }

  public void setB(String name, boolean value) {
    name = name.toLowerCase();
    if (htNonbooleanParameterValues.containsKey(name))
      return; // don't allow setting boolean of a numeric
    htBooleanParameterFlags.put(name, value ? Boolean.TRUE : Boolean.FALSE);
  }

  void setI(String name, int value) {
    if (value != Integer.MAX_VALUE)
      setO(name, Integer.valueOf(value));
  }

  public void setF(String name, float value) {
    if (!Float.isNaN(value))
      setO(name, Float.valueOf(value));
  }

  public void setO(String name, Object value) {
    name = name.toLowerCase();
    if (value == null || htBooleanParameterFlags.containsKey(name))
      return; // don't allow setting string of a boolean
    htNonbooleanParameterValues.put(name, value);
  }

  public void removeParam(String key) {
    // used by resetError to remove _errorMessage
    // used by setSmilesString to remove _smilesString
    // used by setAxesModeMolecular to remove axesUnitCell
    //   and either axesWindow or axesMolecular
    // used by setAxesModeUnitCell to remove axesMolecular
    //   and either remove axesWindow or axesUnitCell

    key = key.toLowerCase();
    if (htBooleanParameterFlags.containsKey(key)) {
      htBooleanParameterFlags.remove(key);
      if (!htPropertyFlagsRemoved.containsKey(key))
        htPropertyFlagsRemoved.put(key, Boolean.FALSE);
      return;
    }
    if (htNonbooleanParameterValues.containsKey(key))
      htNonbooleanParameterValues.remove(key);
  }

  public SV setUserVariable(String key, SV var) {
    if (var != null) {
      key = key.toLowerCase();
      htUserVariables.put(key, var.setName(key));
    }
    return var;
  }

  void unsetUserVariable(String key) {
    if (key.equals("all") || key.equals("variables")) {
      htUserVariables.clear();
      Logger.info("all user-defined variables deleted");
    } else if (htUserVariables.containsKey(key)) {
      Logger.info("variable " + key + " deleted");
      htUserVariables.remove(key);
    }
  }

  void removeUserVariable(String key) {
    htUserVariables.remove(key);
  }

  SV getUserVariable(String name) {
    if (name == null)
      return null;
    name = name.toLowerCase();
    return htUserVariables.get(name);
  }

  String getParameterEscaped(String name, int nMax) {
    name = name.toLowerCase();
    if (htNonbooleanParameterValues.containsKey(name)) {
      Object v = htNonbooleanParameterValues.get(name);
      return StateManager.varClip(name, Escape.e(v), nMax);
    }
    if (htBooleanParameterFlags.containsKey(name))
      return htBooleanParameterFlags.get(name).toString();
    if (htUserVariables.containsKey(name))
      return htUserVariables.get(name).escape();
    if (htPropertyFlagsRemoved.containsKey(name))
      return "false";
    return "<not defined>";
  }

  /**
   * 
   * strictly a getter
   * 
   * @param name
   * @param nullAsString
   *        returns "" if not found
   * @return a Integer, Float, String, BitSet, or Variable, or null
   */
  Object getParameter(String name, boolean nullAsString) {
    Object v = getParam(name, false);
    return (v == null && nullAsString ? "" : v);
  }

  /**
   * 
   * 
   * @param name
   * @param doSet
   * @return a new variable if possible, but null if "_xxx"
   * 
   */
  public SV getAndSetNewVariable(String name, boolean doSet) {
    if (name == null || name.length() == 0)
      name = "x";
    Object v = getParam(name, true);
    return (v == null && doSet && name.charAt(0) != '_' ? setUserVariable(name,
        SV.newV(T.string, "")) : SV.getVariable(v));
  }

  Object getParam(String name, boolean asVariable) {
    name = name.toLowerCase();
    if (name.equals("_memory")) {
      float bTotal = 0;
      float bFree = 0;
      /**
       * @j2sIgnore
       * 
       */
      {
        Runtime runtime = Runtime.getRuntime();
        bTotal = runtime.totalMemory() / 1000000f;
        bFree = runtime.freeMemory() / 1000000f;
      }
      String value = DF.formatDecimal(bTotal - bFree, 1) + "/"
          + DF.formatDecimal(bTotal, 1);
      htNonbooleanParameterValues.put("_memory", value);
    }
    if (htNonbooleanParameterValues.containsKey(name))
      return htNonbooleanParameterValues.get(name);
    if (htBooleanParameterFlags.containsKey(name))
      return htBooleanParameterFlags.get(name);
    if (htPropertyFlagsRemoved.containsKey(name))
      return Boolean.FALSE;
    if (htUserVariables.containsKey(name)) {
      SV v = htUserVariables.get(name);
      return (asVariable ? v : SV.oValue(v));
    }
    return null;
  }

  public String getVariableList() {
    return StateManager.getVariableList(htUserVariables, 0, true, false);
  }

  // static because we don't plan to be changing these
  Map<STR, float[]> structureList = new Hashtable<STR, float[]>();

  {
    structureList.put(STR.TURN, new float[] { // turn
        30, 90, -15, 95, });
    structureList.put(STR.SHEET, new float[] { // sheet
        -180, -10, 70, 180, -180, -45, -180, -130, 140, 180, 90, 180, });
    structureList.put(STR.HELIX, new float[] { // helix
        -160, 0, -100, 45, });
  }

  boolean haveSetStructureList;
//  private String[] userDatabases;

  public int bondingVersion = Elements.RAD_COV_IONIC_OB1_100_1;

  public void setStructureList(float[] list, STR type) {
    haveSetStructureList = true;
    structureList.put(type, list);
  }

  public Map<STR, float[]> getStructureList() {
    return structureList;
  }

  static boolean doReportProperty(String name) {
    return (name.charAt(0) != '_' && unreportedProperties.indexOf(";" + name
        + ";") < 0);
  }

  final private static String unreportedProperties =
  //these are handled individually in terms of reporting for the state
  //NOT EXCLUDING the load state settings, because although we
  //handle these specially for the CURRENT FILE, their current
  //settings won't be reflected in the load state, which is determined
  //earlier, when the file loads. 
  //
  //place any parameter here you do NOT want to have in the state
  //
  // _xxxxx variables are automatically exempt
  //
  (";ambientpercent;animationfps"
      + ";antialiasdisplay;antialiasimages;antialiastranslucent;appendnew;axescolor"
      + ";axesposition;axesmolecular;axesorientationrasmol;axesunitcell;axeswindow;axis1color;axis2color"
      + ";axis3color;backgroundcolor;backgroundmodel;bondsymmetryatoms;boundboxcolor;cameradepth"
      + ";bondingversion;ciprule6full;contextdepthmax;debug;debugscript;defaultlatttice;defaults;defaultdropscript;diffusepercent;"
      + ";exportdrivers;exportscale"
      + ";_filecaching;_filecache;fontcaching;fontscaling;forcefield;language"
      + ";hbondsDistanceMaximum;hbondsangleminimum" // added Jmol 14.24.2
      + ";jmolinjspecview;legacyautobonding;legacyhaddition;legacyjavafloat"
      + ";loglevel;logfile;loggestures;logcommands;measurestylechime"
      + ";loadformat;loadligandformat;macrodirectory;mkaddhydrogens"
      + ";smilesurlformat;pubchemformat;nihresolverformat;edsurlformat;edsurlcutoff;multiprocessor;navigationmode;"
      + ";nodelay;pathforallfiles;perspectivedepth;phongexponent;perspectivemodel;platformspeed"
      + ";preservestate;refreshing;repaintwaitms;rotationradius;selectallmodels"
      + ";showaxes;showaxis1;showaxis2;showaxis3;showboundbox;showfrank;showtiming;showunitcell"
      + ";slabenabled;slab;slabrange;depth;zshade;zshadepower;specular;specularexponent;specularpercent"
      + ";celshading;celshadingpower;specularpower;stateversion"
      + ";statusreporting;stereo;stereostate;vibrationperiod"
      + ";unitcellcolor;visualrange;windowcentered;zerobasedxyzrasmol;zoomenabled;mousedragfactor;mousewheelfactor"
      //    saved in the hash table but not considered part of the state:
      + ";scriptqueue;scriptreportinglevel;syncscript;syncmouse;syncstereo"
      + ";defaultdirectory;currentlocalpath;defaultdirectorylocal"
      //    more settable Jmol variables    
      + ";ambient;bonds;colorrasmol;diffuse;fractionalrelative;frank;hetero;hidenotselected"
      + ";hoverlabel;hydrogen;languagetranslation;measurementunits;navigationdepth;navigationslab"
      + ";picking;pickingstyle;propertycolorschemeoverload;radius;rgbblue;rgbgreen;rgbred"
      + ";scaleangstromsperinch;selectionhalos;showscript;showselections;solvent;strandcount"
      + ";spinx;spiny;spinz;spinfps;navx;navy;navz;navfps;"
      + CBK.getNameList()
      + ";undo;atompicking;drawpicking;bondpicking;pickspinrate;picklabel"
      + ";modelkitmode;autoplaymovie;allowaudio;allowgestures;allowkeystrokes;allowmultitouch;allowmodelkit"
      //  oops these were in some state scripts but should not have been
      + ";dodrop;hovered;historylevel;imagestate;iskiosk;useminimizationthread"
      + ";showkeystrokes;saveproteinstructurestate;testflag1;testflag2;testflag3;testflag4"
      // removed in Jmol 14.29.18
      + ";selecthetero;selecthydrogen;"
      
      + ";")
      .toLowerCase();

  Object getAllVariables() {
    Map<String, Object> map = new Hashtable<String, Object>();
    map.putAll(htBooleanParameterFlags);
    map.putAll(htNonbooleanParameterValues);
    map.putAll(htUserVariables);
    return map;
  }

  /**
   * these settings are determined when the file is loaded and are kept even
   * though they might later change. So we list them here and ALSO let them be
   * defined in the settings. 10.9.98 missed this.
   * 
   * @param htParams
   * 
   * @return script command
   */
  String getLoadState(Map<String, Object> htParams) {

    // some commands register flags so that they will be 
    // restored in a saved state definition, but will not execute
    // now so that there is no chance any embedded scripts or
    // default load scripts will run and slow things down.
    SB str = new SB();
    app(str, "set allowEmbeddedScripts false");
    if (allowEmbeddedScripts)
      setB("allowEmbeddedScripts", true);
    app(str, "set appendNew " + appendNew);
    app(str, "set appletProxy " + PT.esc(appletProxy));
    app(str, "set applySymmetryToBonds " + applySymmetryToBonds);
    if (atomTypes.length() > 0)
      app(str, "set atomTypes " + PT.esc(atomTypes));
    app(str, "set autoBond " + autoBond);
    //    appendCmd(str, "set autoLoadOrientation " + autoLoadOrientation);
    if (axesOrientationRasmol)
      app(str, "set axesOrientationRasmol true");
    app(str, "set bondRadiusMilliAngstroms " + bondRadiusMilliAngstroms);
    app(str, "set bondTolerance " + bondTolerance);
    app(str, "set defaultLattice " + Escape.eP(ptDefaultLattice));
    app(str, "set defaultLoadFilter " + PT.esc(defaultLoadFilter));
    app(str, "set defaultLoadScript \"\"");
    if (defaultLoadScript.length() > 0)
      setO("defaultLoadScript", defaultLoadScript);
    app(str, "set defaultStructureDssp " + defaultStructureDSSP);
    String sMode = vwr.getDefaultVdwNameOrData(Integer.MIN_VALUE, null, null);
    app(str, "set defaultVDW " + sMode);
    if (sMode.equals("User"))
      app(str, vwr.getDefaultVdwNameOrData(Integer.MAX_VALUE, null, null));
    app(str, "set forceAutoBond " + forceAutoBond);
    app(str, "#set defaultDirectory " + PT.esc(defaultDirectory));
    app(str, "#set loadFormat " + PT.esc(loadFormat));
    app(str, "#set loadLigandFormat " + PT.esc(pdbLoadLigandFormat));
    app(str, "#set smilesUrlFormat " + PT.esc(smilesUrlFormat));
    app(str, "#set nihResolverFormat " + PT.esc(nihResolverFormat));
    app(str, "#set pubChemFormat " + PT.esc(pubChemFormat));
//    app(str, "#set edsUrlFormat " + PT.esc(edsUrlFormat));
//    app(str, "#set edsUrlFormatDiff " + PT.esc(edsUrlFormatDiff));
//    app(str, "#set edsUrlCutoff " + PT.esc(edsUrlCutoff));
    //    if (autoLoadOrientation)
    //      appendCmd(str, "set autoLoadOrientation true");
    app(str, "set bondingVersion " + bondingVersion);
    app(str, "set legacyAutoBonding " + legacyAutoBonding);
    app(str, "set legacyAutoBonding " + legacyAutoBonding);
    app(str, "set legacyHAddition " + legacyHAddition);
    app(str, "set legacyJavaFloat " + legacyJavaFloat);
    app(str, "set minBondDistance " + minBondDistance);
    // these next two might be part of a 2D->3D operation
    app(str, "set minimizationCriterion  " + minimizationCriterion);
    app(str, "set minimizationSteps  " + minimizationSteps);
    // Jmol 14.3.15 introduces bananas, but this setting should not carry through from one model to the next
    app(str, "set multipleBondBananas false");
    app(str,
        "set pdbAddHydrogens "
            + (htParams != null
                && htParams.get("pdbNoHydrogens") != Boolean.TRUE ? pdbAddHydrogens
                : false));
    app(str, "set pdbGetHeader " + pdbGetHeader);
    app(str, "set pdbSequential " + pdbSequential);
    app(str, "set percentVdwAtom " + percentVdwAtom);
    app(str, "set smallMoleculeMaxAtoms " + smallMoleculeMaxAtoms);
    app(str, "set smartAromatic " + smartAromatic);
    if (zeroBasedXyzRasmol)
      app(str, "set zeroBasedXyzRasmol true");
    return str.toString();
  }

  private void app(SB s, String cmd) {
    if (cmd.length() == 0)
      return;
    s.append("  ").append(cmd).append(";\n");
  }

}
