/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-04-29 07:22:46 -0500 (Thu, 29 Apr 2010) $
 * $Revision: 12980 $
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
package org.jmol.modelkit;

import java.util.Properties;

import org.jmol.i18n.GT;
import org.jmol.popup.PopupResource;

public class ModelKitPopupResourceBundle extends PopupResource {

  public ModelKitPopupResourceBundle(String menuStructure,
      Properties menuText) {
    super(menuStructure, menuText);
  }

  private final static String MENU_NAME = "modelkitMenu";

  @Override
  public String getMenuName() {
    return MENU_NAME; 
  }

  @Override
  protected void buildStructure(String menuStructure) {
    addItems(menuContents);
    addItems(structureContents);
    if (menuStructure != null)
      setStructure(menuStructure, new GT());
  }
  
  private static String[][] menuContents = {
    { MENU_NAME, "atomMenu bondMenu xtalMenu optionsMenu" },
    { "optionsMenu", "new center addh minimize hmin " +
    		" - undo redo - SIGNEDsaveFile SIGNEDsaveState exit" },
    { "atomMenu" , "assignAtom_XP!CB assignAtom_XxP!CB dragAtomP!CB dragMinimizeP!CB dragMoleculeP!CB dragMinimizeMoleculeP!CB " +
    		"invertStereoP!CB - assignAtom_CP!CB assignAtom_HP!CB assignAtom_NP!CB assignAtom_OP!CB assignAtom_FP!CB assignAtom_ClP!CB assignAtom_BrP!CB " +
    		"_??P!CB _??P!CB _??P!CB " +
    		"moreAtomMenu - assignAtom_plP!CB assignAtom_miP!CB" },
    { "moreAtomMenu", "clearQ - _??P!CB _??P!CB _??P!CB _??P!CB _??P!CB _??P!CB " },
    { "bondMenu", "assignBond_0P!CB assignBond_1P!CB assignBond_2P!CB assignBond_3P!CB - assignBond_pP!CB assignBond_mP!CB - rotateBondP!CB" },
    { "xtalMenu", "xtalModeMenu xtalSelMenu xtalSelOpMenu xtalSymmetryMenu xtalPackingMenu xtalOptionsMenu" },
    { "xtalModeMenu", "mkmode_molecular??P!RD mkmode_view??P!RD mkmode_edit??P!RD" }, 
    { "xtalSelMenu", "mksel_atom??P!RD mksel_position??P!RD" },
    { "xtalSelOpMenu", "mkselop_byop??P!RD xtalOpMenu mkselop_addOffset??P!RD mkselop_atom2??P!RD" },
    { "xtalSymmetryMenu", "mksymmetry_none??P!RD mksymmetry_retainLocal??P!RD mksymmetry_applyLocal??P!RD mksymmetry_applyFull??P!RD" },
    { "xtalPackingMenu", "mkunitcell_extend??P!RD mkunitcell_packed??P!RD" },
    { "xtalOptionsMenu", "mkaddHydrogens??P!CB mkclicktosetelement??P!CB" }

  };
  
  private static String[][] structureContents = {
      { "new" , "zap" },
    { "center" , "zoomto 0 {visible} 0/1.5" },
    { "addh" , "calculate hydrogens {model=_lastframe}" },
    { "minimize" , "minimize" },
    { "hmin" , "delete hydrogens and model=_lastframe; minimize addhydrogens" },
    { "SIGNEDsaveFile", "select visible;write COORD '?jmol.mol'" },
    { "SIGNEDsaveState", "write '?jmol.jpg'" },
    { "clearQ", "clearQ" },
    { "undo" , "!UNDO" },
    { "redo" , "!REDO" },
    { "exit", "set modelkitMode false" },
  };
  
  @Override
  protected String[] getWordContents() {
    
    boolean wasTranslating = GT.setDoTranslate(true);
    String[] words = new String[] {
        "atomMenu", "<atoms.png>",//GT.$("atoms"),
        "moreAtomMenu", "<dotdotdot.png>",//GT.$("more..."),
        "bondMenu", "<bonds.png>",//GT.$("bonds"),
        "optionsMenu", "<dotdotdot.png>",//GT.$("atoms"),
        "xtalMenu", "<xtal.png>",
        "xtalModeMenu","mode",
        "xtalSelMenu","select atom or position",
        "xtalSelOpMenu","select operator", 
        "xtalOpMenu","operators:", 
        "xtalSymmetryMenu", "symmetry",
        "xtalOptionsmenu", "options",
        "xtalPackingMenu", "packing",
        "mkaddHydrogens??P!CB", "add hydrogens on new atoms",
        "mkclicktosetelement??P!CB", "allow clicking to set atom element",
        "mkselop_byop??P!RD", "from list",
        "mkselop_addOffset??P!RD", "add lattice offset",
        "mkselop_atom2??P!RD", "to second atom",
        "mksel_atom??P!RD", "select atom", 
        "mksel_position??P!RD", "select position",
        "mkmode_molecular??P!RD", GT.$("No View/Edit"),
        "mkmode_view??P!RD", GT.$("View"),
        "mkmode_edit??P!RD", GT.$("Edit"),
        "mksymmetry_none??P!RD", GT.$("do not apply"),
        "mksymmetry_retainLocal??P!RD", GT.$("retain local"),
        "mksymmetry_applyLocal??P!RD", GT.$("apply local"),
        "mksymmetry_applyFull??P!RD", GT.$("apply full"),
        "mkunitcell_extend??P!RD", GT.$("extend cell"),
        "mkunitcell_packed??P!RD", GT.$("pack cell"),
        "mkasymmetricUnit??P!RD", GT.$("asymmetric unit"),
        "mkallAtoms??P!RD", GT.$("all atoms"),
        "new" , "zap",
        "new", GT.$("new"),
        "undo", GT.$("undo (CTRL-Z)"),
        "redo", GT.$("redo (CTRL-Y)"),
        "center", GT.$("center"),
        "addh", GT.$("add hydrogens"),
        "minimize", GT.$("minimize"),
        "hmin", GT.$("fix hydrogens and minimize"),
        "clearQ", GT.$("clear"),
        "SIGNEDsaveFile", GT.$("save file"),
        "SIGNEDsaveState", GT.$("save state"),
        "invertStereoP!CB", GT.$("invert ring stereochemistry"),
        "assignAtom_XP!CB" , GT.$("delete atom"),
        "assignAtom_XxP!CB" , GT.$("drag to bond"),
        "dragAtomP!CB" , GT.$("drag atom"),
        "dragMinimizeP!CB" , GT.$("drag atom (and minimize)"),
        "dragMoleculeP!CB" , GT.$("drag molecule (ALT to rotate)"),
        "dragMinimizeMoleculeP!CB" , GT.$("drag and minimize molecule (docking)"),
        "assignAtom_CP!CB" , "C",
        "assignAtom_HP!CB" , "H",
        "assignAtom_NP!CB" , "N",
        "assignAtom_OP!CB" , "O",
        "assignAtom_FP!CB" , "F",
        "assignAtom_ClP!CB" , "Cl",
        "assignAtom_BrP!CB" , "Br",
        "_??P!CB", "??",
        "assignAtom_plP!CB", GT.$("increase charge"),
        "assignAtom_miP!CB", GT.$("decrease charge"),
        "assignBond_0P!CB" , GT.$("delete bond"),
        "assignBond_1P!CB", GT.$("single"),
        "assignBond_2P!CB", GT.$("double"),
        "assignBond_3P!CB", GT.$("triple"),
        "assignBond_pP!CB", GT.$("increase order"),
        "assignBond_mP!CB", GT.$("decrease order"),
        "rotateBondP!CB", GT.$("rotate bond (SHIFT-DRAG)"),
        "exit", GT.$("exit modelkit mode"),
    };
 
    GT.setDoTranslate(wasTranslating);

    return words;
  }

  @Override
  public String getMenuAsText(String title) {
    return getStuctureAsText(title, menuContents, structureContents);
  }

}
