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
    { "optionsMenu", "new ekey center addh minimize hmin " +
        " - undo redo - SIGNEDsaveFile SIGNEDsaveState exit!Persist" },
    { ModelKit.ATOM_MODE , "assignAtom_XxP!RD dragAtomP!RD dragMinimizeP!RD dragMoleculeP!RD dragMinimizeMoleculeP!RD " +
        "invertStereoP!RD -  assignAtom_XP!RD assignAtom_CP!RD assignAtom_HP!RD assignAtom_NP!RD assignAtom_OP!RD assignAtom_FP!RD assignAtom_ClP!RD assignAtom_BrP!RD " +
        "_??P!RD _??P!RD _??P!RD " +
        "moreAtomMenu assignAtom_plP!RD assignAtom_miP!RD" },
    { "moreAtomMenu", "clearQPersist , _??P!RD _??P!RD _??P!RD _??P!RD _??P!RD _??P!RD " },
    { ModelKit.BOND_MODE, "assignBond_0P!RD assignBond_1P!RD assignBond_2P!RD assignBond_3P!RD assignBond_pP!RD assignBond_mP!RD rotateBondP!RD" },
    { ModelKit.XTAL_MODE, ""
        //+ "xtalModePersistMenu "
        //+ "xtalSelPersistMenu "
        + "xtalSelOpPersistMenu "
        + "operator "
        //+ "xtalPackingPersistMenu "
        //+ "xtalEditOptPersistMenu "
        //+ "xtalOptionsPersistMenu" 
        },
    { "xtalModePersistMenu", "mkmode_molecular mkmode_view mkmode_edit" }, 
    { "xtalSelPersistMenu", "mksel_atom mksel_position" },    
    { "xtalSelOpPersistMenu", "xtalOp!PersistMenu "
        //+ "mkselop_atom2 "
        //+ "mkselop_addOffset" 
        },
    { "xtalEditOptPersistMenu", "mksymmetry_none mksymmetry_retainLocal mksymmetry_applyLocal mksymmetry_applyFull" },

    { "xtalPackingPersistMenu", "mkunitcell_packed mkunitcell_extend" },
    { "xtalOptionsPersistMenu", "mkshowSymopInfoCB mkclicktosetelementCB mkaddHydrogensCB" }

  };
  
  private static String[][] structureContents = {
      { "new" , "zap" },
      { "ekey" , "set elementkey= @{!elementkey}" },
    { "center" , "zoomto 0 {visible} 0/1.5" },
    { "addh" , "calculate hydrogens {model=_lastframe}" },
    { "minimize" , "minimize" },
    { "hmin" , "delete hydrogens and model=_lastframe; minimize addhydrogens" },
    { "SIGNEDsaveFile", "select visible;write COORD '?jmol.mol'" },
    { "SIGNEDsaveState", "write '?jmol.jpg'" },
    { "clearQ", "clearQ" },
    { "undo" , "!undoMove" },
    { "redo" , "!redoMove" },
    { "operator", "" },
    { "exit!Persist", "set modelkitMode false" }
  };
  
  @Override
  protected String[] getWordContents() {
    
    boolean wasTranslating = GT.setDoTranslate(true);
    String[] words = new String[] {
        ModelKit.ATOM_MODE, "<atoms.png>",//GT.$("atoms"),
        "moreAtomMenu", "<dotdotdot.png>",//GT.$("more..."),
        ModelKit.BOND_MODE, "<bonds.png>",//GT.$("bonds"),
        "optionsMenu", "<dotdotdot.png>",//GT.$("atoms"),
        "operator", "(no operator selected)",
        ModelKit.XTAL_MODE, "<xtal.png>",
        "xtalModePersistMenu","mode",
        "xtalSelPersistMenu","select atom or position",
        "xtalSelOpPersistMenu","select operator", 
        "xtalOp!PersistMenu","from list...", 
        "xtalEditOptPersistMenu", "edit options",
        "xtalOptionsPersistMenu", "more options...",
        "xtalPackingPersistMenu", "packing",
        "mkshowSymopInfoCB", "show symmetry operator info",
        "mkaddHydrogensCB", "add hydrogens on new atoms",
        "mkclicktosetelementCB", "allow clicking to set atom element",
        "mkselop_byop", "from list",
        "mkselop_addOffset", "add/remove lattice offset",
        "mkselop_atom2", "to second atom",
        "mksel_atom", "select atom", 
        "mksel_position", "select position",
        "mkmode_molecular", GT.$("disabled"),
        "mkmode_view", GT.$("View"),
        "mkmode_edit", GT.$("Edit"),
        "mksymmetry_none", GT.$("do not apply"),
        "mksymmetry_retainLocal", GT.$("retain local"),
        "mksymmetry_applyLocal", GT.$("apply local"),
        "mksymmetry_applyFull", GT.$("apply full"),
        "mkunitcell_extend", GT.$("this operator"),
        "mkunitcell_packed", GT.$("add translation"),
        "mkasymmetricUnit", GT.$("asymmetric unit"),
        "mkallAtoms", GT.$("all atoms"),
        "new", GT.$("new"),
        "undo", GT.$("undo (CTRL-Z)"),
        "redo", GT.$("redo (CTRL-Y)"),
        "center", GT.$("center"),
        "ekey", GT.$("element key"),
        "addh", GT.$("add hydrogens"),
        "minimize", GT.$("minimize"),
        "hmin", GT.$("fix hydrogens and minimize"),
        "clearQPersist", GT.$("clear"),
        "SIGNEDsaveFile", GT.$("save file"),
        "SIGNEDsaveState", GT.$("save state"),
        "invertStereoP!RD", ModelKit.getText("invStereo"),
        "assignAtom_XP!RD" , ModelKit.getText("delAtom"),
        "assignAtom_XxP!RD" , ModelKit.getText("dragBond"),
        "dragAtomP!RD" , ModelKit.getText("dragAtom"),
        "dragMinimizeP!RD" , ModelKit.getText("dragMinimize"),
        "dragMoleculeP!RD" , ModelKit.getText("dragMolecule"),
        "dragMinimizeMoleculeP!RD" , ModelKit.getText("dragMMol"),
        "assignAtom_CP!RD" , "C",
        "assignAtom_HP!RD" , "H",
        "assignAtom_NP!RD" , "N",
        "assignAtom_OP!RD" , "O",
        "assignAtom_FP!RD" , "F",
        "assignAtom_ClP!RD" , "Cl",
        "assignAtom_BrP!RD" , "Br",
        "_??P!RD", "??",
        "assignAtom_plP!RD", ModelKit.getText("incCharge"),
        "assignAtom_miP!RD", ModelKit.getText("decCharge"),
        "assignBond_0P!RD" , ModelKit.getText("bondTo0"),
        "assignBond_1P!RD", ModelKit.getText("bondTo1"),
        "assignBond_2P!RD", ModelKit.getText("bondTo2"),
        "assignBond_3P!RD", ModelKit.getText("bondTo3"),
        "assignBond_pP!RD", ModelKit.getText("incBond"),
        "assignBond_mP!RD", ModelKit.getText("decBond"),
        "rotateBondP!RD", ModelKit.getText("rotBond"),
        "exit!Persist", GT.$("exit modelkit mode"),
    };
 
    GT.setDoTranslate(wasTranslating);

    return words;
  }

  @Override
  public String getMenuAsText(String title) {
    return getStuctureAsText(title, menuContents, structureContents);
  }

}
