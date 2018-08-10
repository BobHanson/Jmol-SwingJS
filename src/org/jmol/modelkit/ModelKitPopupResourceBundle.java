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
    { MENU_NAME, "atomMenu bondMenu optionsMenu" },
    { "optionsMenu", "new center addh minimize hmin " +
    		" - undo redo - SIGNEDsaveFile SIGNEDsaveState exit" },
    { "atomMenu" , "assignAtom_XP!CB assignAtom_XxP!CB dragAtomP!CB dragMinimizeP!CB dragMoleculeP!CB dragMinimizeMoleculeP!CB " +
    		"invertStereoP!CB - assignAtom_CP!CB assignAtom_HP!CB assignAtom_NP!CB assignAtom_OP!CB assignAtom_FP!CB assignAtom_ClP!CB assignAtom_BrP!CB " +
    		"_??P!CB _??P!CB _??P!CB " +
    		"moreAtomMenu - assignAtom_PlP!CB assignAtom_MiP!CB" },
    { "moreAtomMenu", "clearQ - _??P!CB _??P!CB _??P!CB _??P!CB _??P!CB _??P!CB " },
    { "bondMenu", "assignBond_0P!CB assignBond_1P!CB assignBond_2P!CB assignBond_3P!CB - assignBond_pP!CB assignBond_mP!CB - rotateBondP!CB" },
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
        "atomMenu", "<atoms.png>",//GT._("atoms"),
        "moreAtomMenu", "<dotdotdot.png>",//GT._("more..."),
        "bondMenu", "<bonds.png>",//GT._("bonds"),
        "optionsMenu", "<dotdotdot.png>",//GT._("atoms"),
        "new", GT._("new"),
        "undo", GT._("undo (CTRL-Z)"),
        "redo", GT._("redo (CTRL-Y)"),
        "center", GT._("center"),
        "addh", GT._("add hydrogens"),
        "minimize", GT._("minimize"),
        "hmin", GT._("fix hydrogens and minimize"),
        "clearQ", GT._("clear"),
        "SIGNEDsaveFile", GT._("save file"),
        "SIGNEDsaveState", GT._("save state"),
        "invertStereoP!CB", GT._("invert ring stereochemistry"),
        "assignAtom_XP!CB" , GT._("delete atom"),
        "assignAtom_XxP!CB" , GT._("drag to bond"),
        "dragAtomP!CB" , GT._("drag atom"),
        "dragMinimizeP!CB" , GT._("drag atom (and minimize)"),
        "dragMoleculeP!CB" , GT._("drag molecule (ALT to rotate)"),
        "dragMinimizeMoleculeP!CB" , GT._("drag and minimize molecule (docking)"),
        "assignAtom_CP!CB" , "C",
        "assignAtom_HP!CB" , "H",
        "assignAtom_NP!CB" , "N",
        "assignAtom_OP!CB" , "O",
        "assignAtom_FP!CB" , "F",
        "assignAtom_ClP!CB" , "Cl",
        "assignAtom_BrP!CB" , "Br",
        "_??P!CB", "??",
        "assignAtom_PlP!CB", GT._("increase charge"),
        "assignAtom_MiP!CB", GT._("decrease charge"),
        "assignBond_0P!CB" , GT._("delete bond"),
        "assignBond_1P!CB", GT._("single"),
        "assignBond_2P!CB", GT._("double"),
        "assignBond_3P!CB", GT._("triple"),
        "assignBond_pP!CB", GT._("increase order"),
        "assignBond_mP!CB", GT._("decrease order"),
        "rotateBondP!CB", GT._("rotate bond (SHIFT-DRAG)"),
        "exit", GT._("exit modelkit mode"),
    };
 
    GT.setDoTranslate(wasTranslating);

    return words;
  }

  @Override
  public String getMenuAsText(String title) {
    return getStuctureAsText(title, menuContents, structureContents);
  }

}
