/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-10-21 06:35:41 -0500 (Fri, 21 Oct 2016) $
 * $Revision: 21271 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
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
package org.jmol.popup;

import java.util.Properties;


import org.jmol.i18n.GT;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.util.PT;


public class MainPopupResourceBundle extends PopupResource {

  private final static String MENU_NAME = "popupMenu";

  @Override
  public String getMenuName() {
    return MENU_NAME; 
  }
  
  public MainPopupResourceBundle(String menuStructure, Properties menuText) {
    super(menuStructure, menuText);
  }
  @Override
  protected void buildStructure(String menuStructure) {
    addItems(menuContents);
    addItems(structureContents);
    if (menuStructure != null)
      setStructure(menuStructure, new GT());
  }
    
  private static String Box(String cmd) {
    return "if (showBoundBox or showUnitcell) {"+cmd+"} else {boundbox on;"+cmd+";boundbox off}";
  }

  private static String[][] menuContents = {
    
      {   "@COLOR", "black darkgray lightgray white - red orange yellow green cyan blue indigo violet"},      
      {   "@AXESCOLOR", "gray salmon maroon olive slateblue gold orchid"},
      
      {   MENU_NAME,
          "fileMenu modelSetMenu FRAMESbyModelComputedMenu configurationComputedMenu " +
          "- selectMenuText viewMenu renderMenu colorMenu " +
          "- surfaceMenu FILEUNITMenu " +
          "- sceneComputedMenu zoomMenu spinMenu VIBRATIONMenu spectraMenu FRAMESanimateMenu " +
          "- measureMenu pickingMenu " +
          "- showConsole JSConsole showMenu computationMenu " +
          "- languageComputedMenu aboutMenu" },

      {   "fileMenu", "loadMenu saveMenu exportMenu SIGNEDJAVAcaptureMenuSPECIAL " },
          
      {   "loadMenu", "SIGNEDloadFile SIGNEDloadUrl SIGNEDloadPdb SIGNEDloadMol SIGNEDloadScript - "
          + "reload SIGNEDloadFileUnitCell" },       
      {   "saveMenu", "writeFileTextVARIABLE writeState writeHistory SIGNEDwriteJmol SIGNEDwriteIsosurface "} ,      
      {   "exportMenu", "SIGNEDNOGLwriteGif SIGNEDNOGLwriteJpg SIGNEDNOGLwritePng " +
      		"SIGNEDNOGLwritePngJmol SIGNEDNOGLwritePovray - "
            + "SIGNEDNOGLwriteVrml SIGNEDNOGLwriteX3d SIGNEDNOGLwriteSTL" },
      {   "selectMenuText",
          "hideNotSelectedCB showSelectionsCB - selectAll selectNone invertSelection - elementsComputedMenu SYMMETRYSelectComputedMenu - "
              + "PDBproteinMenu PDBnucleicMenu PDBheteroMenu PDBcarboMenu PDBnoneOfTheAbove" },

      {   "PDBproteinMenu", 
          "PDBaaResiduesComputedMenu - "
              + "allProtein proteinBackbone proteinSideChains - "
              + "polar nonpolar - "
              + "positiveCharge negativeCharge noCharge" },
              
      {   "PDBcarboMenu",
          "PDBcarboResiduesComputedMenu - allCarbo" },

      {   "PDBnucleicMenu",
          "PDBnucleicResiduesComputedMenu - allNucleic nucleicBackbone nucleicBases - DNA RNA - "
              + "atPairs auPairs gcPairs - aaStructureMenu" },
              
      {   "PDBheteroMenu",
          "PDBheteroComputedMenu - allHetero Solvent Water - "
              + "Ligand exceptWater nonWaterSolvent" },

      {   "viewMenu",
          "best front left right top bottom back - axisX axisY axisZ - axisA axisB axisC" },

      {   "renderMenu",
          "renderSchemeMenu - atomMenu labelMenu bondMenu hbondMenu ssbondMenu - "
              + "PDBstructureMenu - [set_axes]Menu [set_boundbox]Menu [set_UNITCELL]Menu - perspectiveDepthCB stereoMenu" },

      {   "renderSchemeMenu",
          "renderCpkSpacefill renderBallAndStick "
              + "renderSticks renderWireframe PDBrenderCartoonsOnly PDBrenderTraceOnly" },
                            
      {   "atomMenu",
          "showHydrogensCB - atomNone - "
              + "atom15 atom20 atom25 atom50 atom75 atom100" },

      {   "bondMenu",
          "bondNone bondWireframe - "
              + "bond100 bond150 bond200 bond250 bond300" },

      {   "hbondMenu",
          "hbondCalc hbondNone hbondWireframe - "
              + "PDBhbondSidechain PDBhbondBackbone - "
              + "hbond100 hbond150 hbond200 hbond250 hbond300" },

      {   "ssbondMenu",
          "ssbondNone ssbondWireframe - "
              + "PDBssbondSidechain PDBssbondBackbone - "
              + "ssbond100 ssbond150 ssbond200 ssbond250 ssbond300" },

      {   "PDBstructureMenu",
          "structureNone - "
              + "backbone cartoon cartoonRockets ribbons rockets strands trace" },

      {   "VIBRATIONvectorMenu",
          "vectorOff vectorOn vibScale20 vibScale05 vector3 vector005 vector01 - "
              + "vectorScale02 vectorScale05 vectorScale1 vectorScale2 vectorScale5" },

      {   "stereoMenu",
          "stereoNone stereoRedCyan stereoRedBlue stereoRedGreen stereoCrossEyed stereoWallEyed" },

      {   "labelMenu",
          "labelNone - " + "labelSymbol labelName labelNumber - "
              + "labelPositionMenu" },

      {   "labelPositionMenu",
          "labelCentered labelUpperRight labelLowerRight labelUpperLeft labelLowerLeft" },

      {   "colorMenu",
          "colorrasmolCB [color_]schemeMenu - [color_atoms]Menu [color_bonds]Menu [color_hbonds]Menu [color_ssbonds]Menu colorPDBStructuresMenu [color_isosurface]Menu"
              + " - [color_labels]Menu [color_vectors]Menu - [color_axes]Menu [color_boundbox]Menu [color_UNITCELL]Menu [color_background]Menu" },

      { "[color_atoms]Menu", "schemeMenu - @COLOR - opaque translucent" },
      { "[color_bonds]Menu", "none - @COLOR - opaque translucent" },
      { "[color_hbonds]Menu", null },
      { "[color_ssbonds]Menu", null },
      { "[color_labels]Menu", null },
      { "[color_vectors]Menu", null },
      { "[color_backbone]Menu", "none - schemeMenu - @COLOR - opaque translucent" },
      { "[color_cartoon]sMenu", null },
      { "[color_ribbon]sMenu", null },
      { "[color_rockets]Menu", null },
      { "[color_strands]Menu", null },
      { "[color_trace]Menu", null },
      { "[color_background]Menu", "@COLOR" },
      { "[color_isosurface]Menu", "@COLOR - opaque translucent" },
      { "[color_axes]Menu", "@AXESCOLOR" },
      { "[color_boundbox]Menu", null },
      { "[color_UNITCELL]Menu", null },



      {   "colorPDBStructuresMenu",
          "[color_backbone]Menu [color_cartoon]sMenu [color_ribbon]sMenu [color_rockets]Menu [color_strands]Menu [color_trace]Menu" },

      {   "schemeMenu",
            "cpk molecule formalcharge partialcharge - altloc#PDB amino#PDB chain#PDB group#PDB monomer#PDB shapely#PDB structure#PDB relativeTemperature#BFACTORS fixedTemperature#BFACTORS property_vxyz#VIBRATION" },

      {   "[color_]schemeMenu", null },

      {   "zoomMenu",
          "zoom50 zoom100 zoom150 zoom200 zoom400 zoom800 - "
              + "zoomIn zoomOut" },

      {   "spinMenu",
          "spinOn spinOff - " + "[set_spin_X]Menu [set_spin_Y]Menu [set_spin_Z]Menu - "
              + "[set_spin_FPS]Menu" },

      {   "VIBRATIONMenu", 
          "vibrationOff vibrationOn vibration20 vibration05 VIBRATIONvectorMenu" },

          {   "spectraMenu", 
          "hnmrMenu cnmrMenu" },

      {   "FRAMESanimateMenu",
          "animModeMenu - play pause resume stop - nextframe prevframe rewind - playrev restart - "
              + "FRAMESanimFpsMenu" },

      {   "FRAMESanimFpsMenu", 
          "animfps5 animfps10 animfps20 animfps30 animfps50" },

      {   "measureMenu",
          "showMeasurementsCB - "
              + "measureOff measureDistance measureAngle measureTorsion PDBmeasureSequence - "
              + "measureDelete measureList - distanceNanometers distanceAngstroms distancePicometers distanceHz" },

      {   "pickingMenu",
          "pickOff pickCenter pickIdent pickLabel pickAtom "
              + "pickMolecule pickElement - dragAtom dragMolecule - pickSpin - modelKitMode - PDBpickChain PDBpickGroup SYMMETRYpickSite" },

      {   "computationMenu",
          "minimize"
              /* calculateVolume*/ },

              
      {   "showMenu",
          "showHistory showFile showFileHeader - "
              + "showOrient showMeasure - "
              + "showSpacegroup showState SYMMETRYshowSymmetry UNITCELLshow - "
              + "showIsosurface showMo - "
              + "modelkit extractMOL" },

      {    "SIGNEDJAVAcaptureMenuSPECIAL", "SIGNEDJAVAcaptureRock SIGNEDJAVAcaptureSpin - SIGNEDJAVAcaptureBegin SIGNEDJAVAcaptureEnd SIGNEDJAVAcaptureOff SIGNEDJAVAcaptureOn SIGNEDJAVAcaptureFpsSPECIAL SIGNEDJAVAcaptureLoopingSPECIAL" },

      { "[set_spin_X]Menu", "s0 s5 s10 s20 s30 s40 s50" },
      { "[set_spin_Y]Menu", null },
      { "[set_spin_Z]Menu", null },
      { "[set_spin_FPS]Menu", null },

      {   "animModeMenu", 
          "onceThrough palindrome loop" },


      {   "surfaceMenu",
          "surfDots surfVDW surfSolventAccessible14 surfSolvent14 surfMolecular surf2MEP surfMEP surfMoComputedMenuText - surfOpaque surfTranslucent surfOff" },

      {   "FILEUNITMenu",
          "SYMMETRYShowComputedMenu FILEMOLload FILEUNITone FILEUNITnine FILEUNITnineRestricted FILEUNITninePoly" },

      {   "[set_axes]Menu", 
          "on off#axes dotted - byPixelMenu byAngstromMenu" },

      { "[set_boundbox]Menu", null },
      { "[set_UNITCELL]Menu", null },

      {   "byPixelMenu", 
          "1p 3p 5p 10p" },

      {   "byAngstromMenu", 
          "10a 20a 25a 50a 100a" },

      {   "aboutMenu", 
          "jmolMenu systemMenu" },
          {   "jmolMenu",
          "APPLETid version date - help - mouse translations jmolorg" },
          {   "systemMenu", 
          "os javaVender javaVersion JAVAprocessors JAVAmemMax JAVAmemTotal" },

  };  
  
  @Override
  protected String[] getWordContents() {
    
    boolean wasTranslating = GT.setDoTranslate(true);
    String vdw = GT.$("{0}% van der Waals");
    String exm = GT.$("Export {0} 3D model");
    String exi = GT.$("Export {0} image");
    String rld = GT.$("Reload {0}");
    String scl = GT.$("Scale {0}");
    String ang = GT.$("{0} \u00C5");
    String pxl = GT.$("{0} px");
    String[] words = new String[] {
        // note that these are now ordered by value, not be key.
        "cnmrMenu", GT.$("13C-NMR"),
        "hnmrMenu", GT.$("1H-NMR"),
        "aboutMenu", GT.$("About..."),
        "negativeCharge", GT.$("Acidic Residues (-)"),
        "allModelsText", GT.$("All {0} models"),
        "allHetero", GT.$("All PDB \"HETATM\""),
        "Solvent", GT.$("All Solvent"),
        "Water", GT.$("All Water"),
        "selectAll", GT.$("All"),
        "allProtein", null,
        "allNucleic", null,
        "allCarbo", null,
        "altloc#PDB", GT.$("Alternative Location"),
        "amino#PDB", GT.$("Amino Acid"),
        "byAngstromMenu", GT.$("Angstrom Width"),
        "animModeMenu", GT.$("Animation Mode"),
        "FRAMESanimateMenu", GT.$("Animation"),
        "atPairs", GT.$("AT pairs"),
        "atomMenu", GT.$("Atoms"),
        "[color_atoms]Menu", null,
        "atomsText", GT.$("atoms: {0}"),
        "auPairs", GT.$("AU pairs"),
        "[color_axes]Menu", GT.$("Axes"),
        "showAxesCB", null,
        "[set_axes]Menu", null, 
        "axisA", GT.$("Axis a"),
        "axisB", GT.$("Axis b"),
        "axisC", GT.$("Axis c"),
        "axisX", GT.$("Axis x"),
        "axisY", GT.$("Axis y"),
        "axisZ", GT.$("Axis z"),
        "back", GT.$("Back"),
        "proteinBackbone", GT.$("Backbone"),
        "nucleicBackbone", null,
        "backbone", null,
        "[color_backbone]Menu", null,
        "[color_background]Menu", GT.$("Background"),
        "renderBallAndStick", GT.$("Ball and Stick"),
        "nucleicBases", GT.$("Bases"),
        "positiveCharge", GT.$("Basic Residues (+)"),
        "best", GT.$("Best"),
        "biomoleculeText", GT.$("biomolecule {0} ({1} atoms)"),
        "biomoleculesMenuText", GT.$("Biomolecules"),
        "black", GT.$("Black"),
        "blue", GT.$("Blue"),
        "bondMenu", GT.$("Bonds"),
        "[color_bonds]Menu", null,
        "bondsText", GT.$("bonds: {0}"),
        "bottom", GT.$("Bottom"),
        "[color_boundbox]Menu", GT.$("Boundbox"),
        "[set_boundbox]Menu", null,
        "showBoundBoxCB", null,
        "PDBheteroComputedMenu", GT.$("By HETATM"),
        "PDBaaResiduesComputedMenu", GT.$("By Residue Name"),
        "PDBnucleicResiduesComputedMenu", null,
        "PDBcarboResiduesComputedMenu", null,
        "schemeMenu", GT.$("By Scheme"),
        "[color_]schemeMenu", null,
        "hbondCalc", GT.$("Calculate"),
        "SIGNEDJAVAcaptureRock", GT.$("Capture rock"),
        "SIGNEDJAVAcaptureSpin", GT.$("Capture spin"),
        "SIGNEDJAVAcaptureMenuSPECIAL", GT.$("Capture"),
        "PDBcarboMenu", GT.$("Carbohydrate"),
        "cartoonRockets", GT.$("Cartoon Rockets"),
        "PDBrenderCartoonsOnly", GT.$("Cartoon"),
        "cartoon", null,
        "[color_cartoon]sMenu", null,
        "pickCenter", GT.$("Center"),
        "labelCentered", GT.$("Centered"),
        "chain#PDB", GT.$("Chain"),
        "chainsText", GT.$("chains: {0}"),
        "colorChargeMenu", GT.$("Charge"),
        "measureAngle", GT.$("Click for angle measurement"),
        "measureDistance", GT.$("Click for distance measurement"),
        "measureTorsion", GT.$("Click for torsion (dihedral) measurement"),
        "PDBmeasureSequence", GT.$("Click two atoms to display a sequence in the console"),
        "modelSetCollectionText", GT.$("Collection of {0} models"),
        "colorMenu", GT.$("Color"),
//        "optionsMenu", GT.$("Compatibility"),
        "computationMenu", GT.$("Computation"),      
        "configurationMenuText", GT.$("Configurations ({0})"),
        "configurationComputedMenu", GT.$("Configurations"),
        "showConsole", GT.$("Console"),
        "renderCpkSpacefill", GT.$("CPK Spacefill"),
        "stereoCrossEyed", GT.$("Cross-eyed viewing"),
        "showState", GT.$("Current state"),
        "cyan", GT.$("Cyan"),
        "darkgray", GT.$("Dark Gray"),
        "measureDelete", GT.$("Delete measurements"),
        "SIGNEDJAVAcaptureOff", GT.$("Disable capturing"),
        "hideNotSelectedCB", GT.$("Display Selected Only"),
        "distanceAngstroms", GT.$("Distance units Angstroms"),
        "distanceNanometers", GT.$("Distance units nanometers"),
        "distancePicometers", GT.$("Distance units picometers"),
        "distanceHz", GT.$("Distance units hz (NMR J-coupling)"),
        "ssbondMenu", GT.$("Disulfide Bonds"),
        "[color_ssbonds]Menu", null,
        "DNA", GT.$("DNA"),
        "surfDots", GT.$("Dot Surface"),
        "dotted", GT.$("Dotted"),
        "measureOff", GT.$("Double-Click begins and ends all measurements"),
        "cpk", GT.$("Element (CPK)"),
        "elementsComputedMenu", GT.$("Element"),
        "SIGNEDJAVAcaptureEnd", GT.$("End capturing"),
        "exportMenu", GT.$("Export"),
        "extractMOL", GT.$("Extract MOL data"),
        "showFile", GT.$("File Contents"),
        "showFileHeader", GT.$("File Header"),
        "fileMenu", GT.$("File"),
        "formalcharge", GT.$("Formal Charge"),
        "front", GT.$("Front"),
        "gcPairs", GT.$("GC pairs"),
        "gold", GT.$("Gold"),
        "gray", GT.$("Gray"),
        "green", GT.$("Green"),
        "group#PDB", GT.$("Group"),
        "groupsText", GT.$("groups: {0}"),
        "PDBheteroMenu", GT.$("Hetero"),
        "off#axes", GT.$("Hide"), 
        "showHistory", GT.$("History"),
        "hbondMenu", GT.$("Hydrogen Bonds"),
        "[color_hbonds]Menu", null,
        "pickIdent", GT.$("Identity"),
        "indigo", GT.$("Indigo"),
        "none", GT.$("Inherit"),
        "invertSelection", GT.$("Invert Selection"),
        "showIsosurface", GT.$("Isosurface JVXL data"),
        "help", GT.$("Jmol Script Commands") ,
        "pickLabel", GT.$("Label"),
        "labelMenu", GT.$("Labels"),
        "[color_labels]Menu", null,
        "languageComputedMenu", GT.$("Language"),
        "left", GT.$("Left"),
        "Ligand", GT.$("Ligand"),
        "lightgray", GT.$("Light Gray"),
        "measureList", GT.$("List measurements"),
        "loadBiomoleculeText", GT.$("load biomolecule {0} ({1} atoms)"),
        "SIGNEDloadFileUnitCell", GT.$("Load full unit cell"),      
        "loadMenu", GT.$("Load"),
        "loop", GT.$("Loop"),
        "labelLowerLeft", GT.$("Lower Left"),
        "labelLowerRight", GT.$("Lower Right"),
        "mainMenuText", GT.$("Main Menu"),
        "opaque", GT.$("Make Opaque"),
        "surfOpaque", null,
        "translucent", GT.$("Make Translucent"),
        "surfTranslucent", null,
        "maroon", GT.$("Maroon"),
        "measureMenu", GT.$("Measurements"),
        "showMeasure", null,
        "modelMenuText", GT.$("model {0}"),
        "hiddenModelSetText", GT.$("Model information"),
        "modelkit", GT.$("Model kit"),      
        "showModel", GT.$("Model"),
        "FRAMESbyModelComputedMenu", GT.$("Model/Frame"),
        "modelKitMode", GT.$("modelKitMode"),
        "surf2MEP", GT.$("Molecular Electrostatic Potential (range -0.1 0.1)"),
        "surfMEP", GT.$("Molecular Electrostatic Potential (range ALL)"),
        "showMo", GT.$("Molecular orbital JVXL data"),
        "surfMoComputedMenuText", GT.$("Molecular Orbitals ({0})"),
        "surfMolecular", GT.$("Molecular Surface"),
        //"calculateVolume", GT.$("Molecular volume"),   
        "molecule", GT.$("Molecule"),
        "monomer#PDB", GT.$("Monomer"),
        "mouse", GT.$("Mouse Manual") ,
        //    "pickDraw" , GT.$("moves arrows"),
        "nextframe", GT.$("Next Frame"),
        "modelSetMenu", GT.$("No atoms loaded"),
        "exceptWater", GT.$("Nonaqueous HETATM") + " (hetero and not water)",
        "nonWaterSolvent",GT.$("Nonaqueous Solvent") + " (solvent and not water)",
        "PDBnoneOfTheAbove", GT.$("None of the above"),
        "selectNone", GT.$("None"),
        "stereoNone", null,
        "labelNone", null,
        "nonpolar", GT.$("Nonpolar Residues"),
        "PDBnucleicMenu", GT.$("Nucleic"),
        "atomNone", GT.$("Off"),
        "bondNone", null,
        "hbondNone", null,
        "ssbondNone", null,
        "structureNone", null,
        "vibrationOff", null,
        "vectorOff", null,
        "spinOff", null,
        "pickOff", null,
        "surfOff", null,
        "olive", GT.$("Olive"),
        "bondWireframe", GT.$("On"),
        "hbondWireframe", null,
        "ssbondWireframe", null,
        "vibrationOn", null,
        "vectorOn", null,
        "spinOn", null,
        "on", null,
        "SIGNEDloadPdb", GT.$("Get PDB file"),
        "SIGNEDloadMol", GT.$("Get MOL file"),
        "SIGNEDloadFile", GT.$("Open local file"),      
        "SIGNEDloadScript", GT.$("Open script"),      
        "SIGNEDloadUrl", GT.$("Open URL"),      
        "minimize", GT.$("Optimize structure"),      
        "orange", GT.$("Orange"),
        "orchid", GT.$("Orchid"),
        "showOrient", GT.$("Orientation"),
        "palindrome", GT.$("Palindrome"),
        "partialcharge", GT.$("Partial Charge"),
        "pause", GT.$("Pause"),
        "perspectiveDepthCB", GT.$("Perspective Depth"),      
        "byPixelMenu", GT.$("Pixel Width"), 
        "onceThrough", GT.$("Play Once"),
        "play", GT.$("Play"),
        "polar", GT.$("Polar Residues"),
        "polymersText", GT.$("polymers: {0}"),
        "labelPositionMenu", GT.$("Position Label on Atom"),
        "prevframe", GT.$("Previous Frame"),
        "PDBproteinMenu", GT.$("Protein"),
        "colorrasmolCB", GT.$("RasMol Colors"),
        "red", GT.$("Red"),
        "stereoRedBlue", GT.$("Red+Blue glasses"),
        "stereoRedCyan", GT.$("Red+Cyan glasses"),
        "stereoRedGreen", GT.$("Red+Green glasses"),
        "SIGNEDJAVAcaptureOn", GT.$("Re-enable capturing"),
        "FILEUNITninePoly", GT.$("Reload + Polyhedra"),
        "reload", GT.$("Reload"),      
        "restart", GT.$("Restart"),
        "resume", GT.$("Resume"),
        "playrev", GT.$("Reverse"),
        "rewind", GT.$("Rewind"),
        "ribbons", GT.$("Ribbons"),
        "[color_ribbon]sMenu", null,
        "right", GT.$("Right"),
        "RNA", GT.$("RNA"),
        "rockets", GT.$("Rockets"),
        "[color_rockets]Menu", null,
        "salmon", GT.$("Salmon"),
        "writeFileTextVARIABLE", GT.$("Save a copy of {0}"),
        "SIGNEDwriteJmol", GT.$("Save as PNG/JMOL (image+zip)"),      
        "SIGNEDwriteIsosurface", GT.$("Save JVXL isosurface"),      
        "writeHistory", GT.$("Save script with history"),      
        "writeState", GT.$("Save script with state"),      
        "saveMenu", GT.$("Save"),
        "sceneComputedMenu" , GT.$("Scenes"),
        "renderSchemeMenu", GT.$("Scheme"),
        "aaStructureMenu", GT.$("Secondary Structure"),
        "structure#PDB", null,
        "selectMenuText", GT.$("Select ({0})"),
        "pickAtom", GT.$("Select atom"),
        "dragAtom", GT.$("Drag atom"),
        "dragMolecule", GT.$("Drag molecule"),
        "PDBpickChain", GT.$("Select chain"),
        "pickElement", GT.$("Select element"),
        "PDBpickGroup", GT.$("Select group"),
        "pickMolecule", GT.$("Select molecule"),
        "SYMMETRYpickSite", GT.$("Select site"),
//        "selectMenu", GT.$("Select"),
        "showSelectionsCB", GT.$("Selection Halos"),
        "SIGNEDJAVAcaptureFpsSPECIAL", GT.$("Set capture replay rate"),
        "[set_spin_FPS]Menu", GT.$("Set FPS"),
        "FRAMESanimFpsMenu", null,
        "PDBhbondBackbone", GT.$("Set H-Bonds Backbone"),
        "PDBhbondSidechain", GT.$("Set H-Bonds Side Chain"),
        "pickingMenu", GT.$("Set picking"),
        "PDBssbondBackbone", GT.$("Set SS-Bonds Backbone"),
        "PDBssbondSidechain", GT.$("Set SS-Bonds Side Chain"),
        "[set_spin_X]Menu", GT.$("Set X Rate"),
        "[set_spin_Y]Menu", GT.$("Set Y Rate"),
        "[set_spin_Z]Menu", GT.$("Set Z Rate"),
        "shapely#PDB", GT.$("Shapely"),
        "showHydrogensCB", GT.$("Show Hydrogens"),
        "showMeasurementsCB", GT.$("Show Measurements"),
        "SYMMETRYpickSymmetry", GT.$("Show symmetry operation"),
        "showMenu", GT.$("Show"),
        "proteinSideChains", GT.$("Side Chains"),
        "slateblue", GT.$("Slate Blue"),
        "SYMMETRYShowComputedMenu", GT.$("Space Group"),
        "showSpacegroup", null,
        "spectraMenu", GT.$("Spectra"),
        "spinMenu", GT.$("Spin"),
        "pickSpin", null,
        "SIGNEDJAVAcaptureBegin", GT.$("Start capturing"),
        "stereoMenu", GT.$("Stereographic"),
        "renderSticks", GT.$("Sticks"),
        "stop", GT.$("Stop"),
        "strands", GT.$("Strands"),
        "[color_strands]Menu", null,
        "PDBstructureMenu", GT.$("Structures"),
        "colorPDBStructuresMenu", null,
        "renderMenu", GT.$("Style"),
        "[color_isosurface]Menu", GT.$("Surfaces"),
        "surfaceMenu", null,
        "SYMMETRYSelectComputedMenu", GT.$("Symmetry"),
        "SYMMETRYshowSymmetry", null,
        "FILEUNITMenu", null,
        "systemMenu", GT.$("System"),
        "relativeTemperature#BFACTORS", GT.$("Temperature (Relative)"),
        "fixedTemperature#BFACTORS", GT.$("Temperature (Fixed)"),
        "SIGNEDJAVAcaptureLoopingSPECIAL", GT.$("Toggle capture looping"),
        "top", PT.split(GT.$("Top[as in \"view from the top, from above\" - (translators: remove this bracketed part]"), "[")[0],
        "PDBrenderTraceOnly", GT.$("Trace"),
        "trace", null,
        "[color_trace]Menu", null,
        "translations", GT.$("Translations") ,
        "noCharge", GT.$("Uncharged Residues"),
        "[color_UNITCELL]Menu", GT.$("Unit cell"),
        "UNITCELLshow", null,
        "[set_UNITCELL]Menu", null,
        "showUNITCELLCB", null,      
        "labelUpperLeft", GT.$("Upper Left"),
        "labelUpperRight", GT.$("Upper Right"),
        "surfVDW", GT.$("van der Waals Surface"),
        "VIBRATIONvectorMenu", GT.$("Vectors"),
        "property_vxyz#VIBRATION", null,
        "[color_vectors]Menu", null,
        "VIBRATIONMenu", GT.$("Vibration"),
        "viewMenuText", GT.$("View {0}"),
        "viewMenu", GT.$("View"),
        "violet", GT.$("Violet"),
        "stereoWallEyed", GT.$("Wall-eyed viewing"),
        "white", GT.$("White"),
        "renderWireframe", GT.$("Wireframe"),
        "labelName", GT.$("With Atom Name"),
        "labelNumber", GT.$("With Atom Number"),
        "labelSymbol", GT.$("With Element Symbol"),
        "yellow", GT.$("Yellow"),
        "zoomIn", GT.$("Zoom In"),
        "zoomOut", GT.$("Zoom Out"),
        "zoomMenu", GT.$("Zoom"),
        "vector005", GT.o(ang, "0.05"),
        "bond100", GT.o(ang, "0.10"),
        "hbond100", null,
        "ssbond100", null,
        "vector01", null,
        "10a", null,
        "bond150", GT.o(ang, "0.15"),
        "hbond150", null,
        "ssbond150", null,
        "bond200", GT.o(ang, "0.20"),
        "hbond200", null,
        "ssbond200", null,
        "20a", null,
        "bond250", GT.o(ang, "0.25"),
        "hbond250", null,
        "ssbond250", null,
        "25a", null,
        "bond300", GT.o(ang, "0.30"),
        "hbond300", null,
        "ssbond300", null,
        "50a", GT.o(ang, "0.50"),
        "100a", GT.o(ang, "1.0"),
        "1p", GT.i(pxl, 1),
        "10p", GT.i(pxl, 10),
        "3p", GT.i(pxl, 3), 
        "vector3", null,
        "5p", GT.i(pxl, 5),
        "atom100", GT.i(vdw, 100),
        "atom15", GT.i(vdw, 15),
        "atom20", GT.i(vdw, 20),
        "atom25", GT.i(vdw, 25),
        "atom50", GT.i(vdw, 50),
        "atom75", GT.i(vdw, 75),
        "SIGNEDNOGLwriteIdtf", GT.o(exm, "IDTF"),      
        "SIGNEDNOGLwriteMaya", GT.o(exm, "Maya"),      
        "SIGNEDNOGLwriteVrml", GT.o(exm, "VRML"),      
        "SIGNEDNOGLwriteX3d", GT.o(exm, "X3D"),      
        "SIGNEDNOGLwriteSTL", GT.o(exm, "STL"),      
        "SIGNEDNOGLwriteGif", GT.o(exi, "GIF"),    
        "SIGNEDNOGLwriteJpg", GT.o(exi, "JPG"),      
        "SIGNEDNOGLwritePng", GT.o(exi, "PNG"),      
        "SIGNEDNOGLwritePngJmol", GT.o(exi, "PNG+JMOL"),      
        "SIGNEDNOGLwritePovray", GT.o(exi, "POV-Ray"),      
        "FILEUNITnineRestricted", GT.o(GT.$("Reload {0} + Display {1}"), new Object[] { "{444 666 1}", "555" } ),
        "FILEMOLload", GT.o(rld, "(molecular)"),
        "FILEUNITone", GT.o(rld, "{1 1 1}"),
        "FILEUNITnine", GT.o(rld, "{444 666 1}"),
        "vectorScale02", GT.o(scl, "0.2"),
        "vectorScale05", GT.o(scl, "0.5"),
        "vectorScale1", GT.o(scl, "1"),
        "vectorScale2", GT.o(scl, "2"),
        "vectorScale5", GT.o(scl, "5"),
        "surfSolvent14", GT.o(GT.$("Solvent Surface ({0}-Angstrom probe)"), "1.4"),
        "surfSolventAccessible14",GT.o(GT.$("Solvent-Accessible Surface (VDW + {0} Angstrom)"), "1.4"),
        "vibration20", "*2",
        "vibration05", "/2",
        "JAVAmemTotal", "?",
        "JAVAmemMax",null,
        "JAVAprocessors",null,
        "s0", "0",
        "animfps10", "10",
        "s10",null,
        "zoom100", "100%",
        "zoom150", "150%",
        "animfps20", "20",
        "s20",null,
        "zoom200", "200%",
        "animfps30", "30",
        "s30",null,
        "s40", "40",
        "zoom400", "400%",
        "animfps5", "5",
        "s5",null,
        "animfps50", "50",
        "s50",null,
        "zoom50", "50%",
        "zoom800", "800%",
        "JSConsole", GT.$("JavaScript Console"),
        "jmolMenu", "Jmol",
        "date" , JC.date,
        "version", JC.version,
        "javaVender", Viewer.strJavaVendor,
        "javaVersion", Viewer.strJavaVersion,
        "os", Viewer.strOSName,
        "jmolorg","http://www.jmol.org" ,
    };
    GT.setDoTranslate(wasTranslating);
    for (int i = 1, n = words.length; i < n; i += 2)
      if (words[i] == null)
        words[i] = words[i - 2];
    return words;
  }

  private static String[][] structureContents = {
    {"jmolorg", "show url \"http://www.jmol.org\"" },
    {"help", "help" },
    {"mouse", "show url \"http://wiki.jmol.org/index.php/Mouse_Manual\""}, 
    {"translations", "show url \"http://wiki.jmol.org/index.php/Internationalisation\""}, 
      { "colorrasmolCB", ""},
      { "hideNotSelectedCB", "set hideNotSelected true | set hideNotSelected false; hide(none)" },
      { "perspectiveDepthCB", ""},
      { "showAxesCB", "set showAxes true | set showAxes false;set axesMolecular" },
      { "showBoundBoxCB", ""},
      { "showHydrogensCB", ""},
      { "showMeasurementsCB", ""},
      { "showSelectionsCB", ""},
      { "showUNITCELLCB", ""},

      { "selectAll", "SELECT all" },
      { "selectNone", "SELECT none" },
      { "invertSelection", "SELECT not selected" },
   
      { "allProtein", "SELECT protein" },
      { "proteinBackbone", "SELECT protein and backbone" },
      { "proteinSideChains", "SELECT protein and not backbone" },
      { "polar", "SELECT protein and polar" },
      { "nonpolar", "SELECT protein and not polar" },
      { "positiveCharge", "SELECT protein and basic" },
      { "negativeCharge", "SELECT protein and acidic" },
      { "noCharge", "SELECT protein and not (acidic,basic)" },
      { "allCarbo", "SELECT carbohydrate" },

      { "allNucleic", "SELECT nucleic" },
      { "DNA", "SELECT dna" },
      { "RNA", "SELECT rna" },
      { "nucleicBackbone", "SELECT nucleic and backbone" },
      { "nucleicBases", "SELECT nucleic and not backbone" },
      { "atPairs", "SELECT a,t" },
      { "gcPairs", "SELECT g,c" },
      { "auPairs", "SELECT a,u" },
      { "A", "SELECT a" },
      { "C", "SELECT c" },
      { "G", "SELECT g" },
      { "T", "SELECT t" },
      { "U", "SELECT u" },

      { "allHetero", "SELECT hetero" },
      { "Solvent", "SELECT solvent" },
      { "Water", "SELECT water" },
      // same as ligand    { "exceptSolvent", "SELECT hetero and not solvent" },
      { "nonWaterSolvent", "SELECT solvent and not water" },
      { "exceptWater", "SELECT hetero and not water" },
      { "Ligand", "SELECT ligand" },

      // not implemented    { "Lipid", "SELECT lipid" },
      { "PDBnoneOfTheAbove", "SELECT not(hetero,protein,nucleic,carbohydrate)" },

      { "best", "rotate best -1.0" },
      { "front", Box( "moveto 2.0 front;delay 1" ) },
      { "left", Box( "moveto 1.0 front;moveto 2.0 left;delay 1"  ) },
      { "right", Box( "moveto 1.0 front;moveto 2.0 right;delay 1"  ) },
      { "top", Box( "moveto 1.0 front;moveto 2.0 top;delay 1"  ) },
      { "bottom", Box( "moveto 1.0 front;moveto 2.0 bottom;delay 1"  ) },
      { "back", Box( "moveto 1.0 front;moveto 2.0 back;delay 1"  ) },
      { "axisA", "moveto axis a"},
      { "axisB", "moveto axis b"},
      { "axisC", "moveto axis c"},
      { "axisX", "moveto axis x"},
      { "axisY", "moveto axis y"},
      { "axisZ", "moveto axis z"},

      { "renderCpkSpacefill", "restrict bonds not selected;select not selected;spacefill 100%;color cpk" },
      { "renderBallAndStick", "restrict bonds not selected;select not selected;spacefill 23%AUTO;wireframe 0.15;color cpk" },
      { "renderSticks", "restrict bonds not selected;select not selected;wireframe 0.3;color cpk" },
      { "renderWireframe", "restrict bonds not selected;select not selected;wireframe on;color cpk" },
      { "PDBrenderCartoonsOnly", "restrict bonds not selected;select not selected;cartoons on;color structure" },
      { "PDBrenderTraceOnly", "restrict bonds not selected;select not selected;trace on;color structure" },

      { "atomNone", "cpk off" },
      { "atom15", "cpk 15%" },
      { "atom20", "cpk 20%" },
      { "atom25", "cpk 25%" },
      { "atom50", "cpk 50%" },
      { "atom75", "cpk 75%" },
      { "atom100", "cpk on" },

      { "bondNone", "wireframe off" },
      { "bondWireframe", "wireframe on" },
      { "bond100", "wireframe .1" },
      { "bond150", "wireframe .15" },
      { "bond200", "wireframe .2" },
      { "bond250", "wireframe .25" },
      { "bond300", "wireframe .3" },

      { "hbondCalc", "hbonds calculate" },
      { "hbondNone", "hbonds off" },
      { "hbondWireframe", "hbonds on" },
      { "PDBhbondSidechain", "set hbonds sidechain" },
      { "PDBhbondBackbone", "set hbonds backbone" },
      { "hbond100", "hbonds .1" },
      { "hbond150", "hbonds .15" },
      { "hbond200", "hbonds .2" },
      { "hbond250", "hbonds .25" },
      { "hbond300", "hbonds .3" },

      { "ssbondNone", "ssbonds off" },
      { "ssbondWireframe", "ssbonds on" },
      { "PDBssbondSidechain", "set ssbonds sidechain" },
      { "PDBssbondBackbone", "set ssbonds backbone" },
      { "ssbond100", "ssbonds .1" },
      { "ssbond150", "ssbonds .15" },
      { "ssbond200", "ssbonds .2" },
      { "ssbond250", "ssbonds .25" },
      { "ssbond300", "ssbonds .3" },

      { "structureNone",
          "backbone off;cartoons off;ribbons off;rockets off;strands off;trace off;" },
      { "backbone", "restrict not selected;select not selected;backbone 0.3" },
      { "cartoon", "restrict not selected;select not selected;set cartoonRockets false;cartoons on" },
      { "cartoonRockets", "restrict not selected;select not selected;set cartoonRockets;cartoons on" },
      { "ribbons", "restrict not selected;select not selected;ribbons on" },
      { "rockets", "restrict not selected;select not selected;rockets on" },
      { "strands", "restrict not selected;select not selected;strands on" },
      { "trace", "restrict not selected;select not selected;trace 0.3" },

      { "vibrationOff", "vibration off" },
      { "vibrationOn", "vibration on" },
      { "vibration20", "vibrationScale *= 2" },
      { "vibration05", "vibrationScale /= 2" },

      { "vectorOff", "vectors off" },
      { "vectorOn", "vectors on" },
      { "vector3", "vectors 3" },
      { "vector005", "vectors 0.05" },
      { "vector01", "vectors 0.1" },
      { "vectorScale02", "vector scale 0.2" },
      { "vectorScale05", "vector scale 0.5" },
      { "vectorScale1", "vector scale 1" },
      { "vectorScale2", "vector scale 2" },
      { "vectorScale5", "vector scale 5" },

      { "stereoNone", "stereo off" },
      { "stereoRedCyan", "stereo redcyan 3" },
      { "stereoRedBlue", "stereo redblue 3" },
      { "stereoRedGreen", "stereo redgreen 3" },
      { "stereoCrossEyed", "stereo -5" },
      { "stereoWallEyed", "stereo 5" },

      { "labelNone", "label off" },
      { "labelSymbol", "label %e" },
      { "labelName", "label %a" },
      { "labelNumber", "label %i" },

      { "labelCentered", "set labeloffset 0 0" },
      { "labelUpperRight", "set labeloffset 4 4" },
      { "labelLowerRight", "set labeloffset 4 -4" },
      { "labelUpperLeft", "set labeloffset -4 4" },
      { "labelLowerLeft", "set labeloffset -4 -4" },

      { "zoom50", "zoom 50" },
      { "zoom100", "zoom 100" },
      { "zoom150", "zoom 150" },
      { "zoom200", "zoom 200" },
      { "zoom400", "zoom 400" },
      { "zoom800", "zoom 800" },
      { "zoomIn", "move 0 0 0 40 0 0 0 0 1" },
      { "zoomOut", "move 0 0 0 -40 0 0 0 0 1" },

      { "spinOn", "spin on" },
      { "spinOff", "spin off" },

      { "s0", "0" },
      { "s5", "5" },
      { "s10", "10" },
      { "s20", "20" },
      { "s30", "30" },
      { "s40", "40" },
      { "s50", "50" },

      { "onceThrough", "anim mode once#" },
      { "palindrome", "anim mode palindrome#" },
      { "loop", "anim mode loop#" },
      { "play", "anim play#" },
      { "pause", "anim pause#" },
      { "resume", "anim resume#" },
      { "stop", "anim off#" },
      
      { "nextframe", "frame next#" },
      { "prevframe", "frame prev#" },
      { "playrev", "anim playrev#" },
      
      { "rewind", "anim rewind#" },
      { "restart", "anim on#" },
      
      { "animfps5", "anim fps 5#" },
      { "animfps10", "anim fps 10#" },
      { "animfps20", "anim fps 20#" },
      { "animfps30", "anim fps 30#" },
      { "animfps50", "anim fps 50#" },

      { "measureOff", "set pickingstyle MEASURE OFF; set picking OFF" },
      { "measureDistance",
          "set pickingstyle MEASURE; set picking MEASURE DISTANCE" },
      { "measureAngle", "set pickingstyle MEASURE; set picking MEASURE ANGLE" },
      { "measureTorsion",
          "set pickingstyle MEASURE; set picking MEASURE TORSION" },
      { "PDBmeasureSequence",
          "set pickingstyle MEASURE; set picking MEASURE SEQUENCE" },
      { "measureDelete", "measure delete" },
      { "measureList", "console on;show measurements" },
      { "distanceNanometers", "select *; set measure nanometers" },
      { "distanceAngstroms", "select *; set measure angstroms" },
      { "distancePicometers", "select *; set measure picometers" },
      { "distanceHz", "select *; set measure hz" },

      { "pickOff", "set picking off" },
      { "pickCenter", "set picking center" },
      //    { "pickDraw" , "set picking draw" },
      { "pickIdent", "set picking ident" },
      { "pickLabel", "set picking label" },
      { "pickAtom", "set picking atom" },
      { "dragAtom", "set picking dragAtom" },
      { "dragMolecule", "set picking dragMolecule" },
      { "PDBpickChain", "set picking chain" },
      { "pickElement", "set picking element" },
      { "modelKitMode", "set modelKitMode" },
      { "PDBpickGroup", "set picking group" },
      { "pickMolecule", "set picking molecule" },
      { "SYMMETRYpickSite", "set picking site" },
      { "pickSpin", "set picking spin" },
      { "SYMMETRYpickSymmetry", "set picking symmetry" },

      { "showConsole", "console" },
      { "JSConsole", "JSCONSOLE" },
      { "showFile", "console on;show file" },
      { "showFileHeader", "console on;getProperty FileHeader" },
      { "showHistory", "console on;show history" },
      { "showIsosurface", "console on;show isosurface" },
      { "showMeasure", "console on;show measure" },
      { "showMo", "console on;show mo" },
      { "showModel", "console on;show model" },
      { "showOrient", "console on;show orientation" },
      { "showSpacegroup", "console on;show spacegroup" },
      { "showState", "console on;show state" },
      
      { "reload", "load \"\"" },
      { "SIGNEDloadPdb", JC.getMenuScript("openPDB") },      
      { "SIGNEDloadMol", JC.getMenuScript("openMOL") },      
      { "SIGNEDloadFile", "load ?" },      
      { "SIGNEDloadUrl", "load http://?" },      
      { "SIGNEDloadFileUnitCell", "load ? {1 1 1}" },      
      { "SIGNEDloadScript", "script ?.spt" },      
      
      { "SIGNEDJAVAcaptureRock", "animation mode loop;capture '?Jmol.gif' rock y 10"},      
      { "SIGNEDJAVAcaptureSpin", "animation mode loop;capture '?Jmol.gif' spin y"},      
      { "SIGNEDJAVAcaptureBegin", "capture '?Jmol.gif'" },      
      { "SIGNEDJAVAcaptureEnd", "capture ''"},      
      { "SIGNEDJAVAcaptureOff", "capture off"},      
      { "SIGNEDJAVAcaptureOn", "capture on"},      
      { "SIGNEDJAVAcaptureFpsSPECIAL", "animation fps @{0+prompt('Capture replay frames per second?', animationFPS)};prompt 'animation FPS ' + animationFPS"},      
      { "SIGNEDJAVAcaptureLoopingSPECIAL", "animation mode @{(animationMode=='ONCE' ? 'LOOP':'ONCE')};prompt 'animation MODE ' + animationMode"},
      
      
      { "writeFileTextVARIABLE", "if (_applet && !_signedApplet) { console;show file } else { write file \"?FILE?\"}" },      
      { "writeState", "if (_applet && !_signedApplet) { console;show state } else { write state \"?FILEROOT?.spt\"}" },      
      { "writeHistory", "if (_applet && !_signedApplet) { console;show history } else { write history \"?FILEROOT?.his\"}" },     
      { "SIGNEDwriteJmol", "write PNGJ \"?FILEROOT?.png\"" },      
      { "SIGNEDwriteIsosurface", "write isosurface \"?FILEROOT?.jvxl\"" },      
      { "SIGNEDNOGLwriteGif", "write image \"?FILEROOT?.gif\"" },      
      { "SIGNEDNOGLwriteJpg", "write image \"?FILEROOT?.jpg\"" },      
      { "SIGNEDNOGLwritePng", "write image \"?FILEROOT?.png\"" },      
      { "SIGNEDNOGLwritePngJmol", "write PNGJ \"?FILEROOT?.png\"" },      
      { "SIGNEDNOGLwritePovray", "write POVRAY \"?FILEROOT?.pov\"" },      
      { "SIGNEDNOGLwriteVrml", "write VRML \"?FILEROOT?.wrl\"" },      
      { "SIGNEDNOGLwriteX3d", "write X3D \"?FILEROOT?.x3d\"" },      
      { "SIGNEDNOGLwriteSTL", "write STL \"?FILEROOT?.stl\"" },      
      { "SIGNEDNOGLwriteIdtf", "write IDTF \"?FILEROOT?.idtf\"" },      
      { "SIGNEDNOGLwriteMaya", "write MAYA \"?FILEROOT?.ma\"" },       
      { "SYMMETRYshowSymmetry", "console on;show symmetry" },
      { "UNITCELLshow", "console on;show unitcell" },
      { "extractMOL", "console on;getproperty extractModel \"visible\" " },
      
       { "minimize", "minimize" },    
       { "modelkit", "set modelkitmode" },    
      //  { "calculateVolume", "console on;print \"Volume = \" + {*}.volume() + \" Ang^3\"" },     
      
      { "surfDots", "dots on" },
      { "surfVDW", "isosurface delete resolution 0 solvent 0 translucent" },
      { "surfMolecular", "isosurface delete resolution 0 molecular translucent" },
      { "surfSolvent14",
          "isosurface delete resolution 0 solvent 1.4 translucent" },
      { "surfSolventAccessible14",
          "isosurface delete resolution 0 sasurface 1.4 translucent" },
      { "surfMEP",
          "isosurface delete resolution 0 vdw color range all map MEP translucent" },
      { "surf2MEP",
          "isosurface delete resolution 0 vdw color range -0.1 0.1 map MEP translucent" },
      { "surfOpaque", "mo opaque;isosurface opaque" },
      { "surfTranslucent", "mo translucent;isosurface translucent" },
      { "surfOff", "mo delete;isosurface delete;var ~~sel = {selected};select *;dots off;select ~~sel" },
      { "FILEMOLload",
      "save orientation;load \"\";restore orientation;center" },
      { "FILEUNITone",
          "save orientation;load \"\" {1 1 1} ;restore orientation;center" },
      { "FILEUNITnine",
          "save orientation;load \"\" {444 666 1} ;restore orientation;center" },
      { "FILEUNITnineRestricted",
          "save orientation;load \"\" {444 666 1} ;restore orientation; unitcell on; display cell=555;center visible;zoom 200" },
      { "FILEUNITninePoly",
          "save orientation;load \"\" {444 666 1} ;restore orientation; unitcell on; display cell=555; polyhedra 4,6 (displayed);center (visible);zoom 200" },

      { "1p", "on" },
      { "3p", "3" },
      { "5p", "5" },
      { "10p", "10" },

      { "10a", "0.1" },
      { "20a", "0.20" },
      { "25a", "0.25" },
      { "50a", "0.50" },
      { "100a", "1.0" },
  };
  

  @Override
  public String getMenuAsText(String title) {
    return getStuctureAsText(title, menuContents, structureContents);
  }
  
}
