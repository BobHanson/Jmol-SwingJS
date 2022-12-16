/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-05-11 15:47:18 -0500 (Tue, 11 May 2010) $
 * $Revision: 13064 $
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

import java.util.Map;

import org.jmol.api.SC;
import org.jmol.i18n.GT;
import org.jmol.popup.JmolGenericPopup;
import org.jmol.popup.PopupResource;
import org.jmol.script.T;
import org.jmol.viewer.ActionManager;
import org.jmol.viewer.Viewer;

/**
 * An abstract popup class that is instantiated for a given platform and context
 * as one of:
 * 
 * <pre>
 *   -- abstract ModelKitPopup
 *      -- AwtModelKitPopup
 *      -- JSModelKitPopup
 * </pre>
 * 
 */

abstract public class ModelKitPopup extends JmolGenericPopup {

  abstract protected void menuHidePopup(SC popup);

  private static PopupResource bundle = new ModelKitPopupResourceBundle(null,
      null);

  ////////////// modelkit menus //////////////

  private static final int MAX_LABEL = 32;

  /**
   * set by MODELKIT [DISPLAY/HIDE]
   */

  // accessed by ModelKit
  
  ModelKit modelkit;
  boolean hidden = false;
  boolean allowPopup = true;

  String activeMenu;

  // accessed by subclasses
  
  protected SC bondRotationCheckBox, prevBondCheckBox;


  // private
  
  private String bondRotationName = ".modelkitMenu.bondMenu.rotateBondP!RD";

  private boolean haveOperators;

  public ModelKitPopup() {
  }

  //////////////// menu creation and update ///////////////

  @Override
  protected PopupResource getBundle(String menu) {
    return bundle;
  }

  @Override
  public void jpiShow(int x, int y) {
    if (!hidden) {
      updateCheckBoxesForModelKit(null);
      super.jpiShow(x, y);
    }
  }

  @Override
  public void jpiUpdateComputedMenus() {
    htMenus.get(ModelKit.XTAL_MODE).setEnabled(modelkit.setHasUnitCell());
    if (modelkit.checkNewModel()) {
      haveOperators = false;
      updateOperatorMenu();
    }
    updateAllXtalMenuOptions();
  }

  @Override
  protected void appUpdateForShow() {
    jpiUpdateComputedMenus();
  }

  void hidePopup() {
    menuHidePopup(popupMenu);
  }

  public void clearLastModelSet() {
    modelkit.lastModelSet = null;
  }

  protected void updateOperatorMenu() {
    if (haveOperators)
      return;
    haveOperators = true;
    SC menu = htMenus.get("xtalOp!PersistMenu");
    if (menu != null)
      addAllCheckboxItems(menu, modelkit.getAllOperators());
  }

  private void addAllCheckboxItems(SC menu, String[] labels) {
    menuRemoveAll(menu, 0);
    SC subMenu = menu;
    int pt = (labels.length > MAX_LABEL ? 0 : Integer.MIN_VALUE);
    for (int i = 0; i < labels.length; i++) {
      if (pt >= 0 && (pt++ % MAX_LABEL) == 0) {
        String id = "mtsymop" + pt + "Menu";
        subMenu = menuNewSubMenu(
            (i + 1) + "..." + Math.min(i + MAX_LABEL, labels.length),
            menuGetId(menu) + "." + id);
        menuAddSubMenu(menu, subMenu);
        htMenus.put(id, subMenu);
        pt = 1;
      }
      if (i == 0)
        menuEnable(
            menuCreateItem(subMenu, GT.$("none"), "draw sym_* delete", null),
            true);
      String sym = labels[i]; // XYZoriginal
      menuEnable(menuCreateItem(subMenu, sym, sym,
          subMenu.getName() + "." + "mkop_" + (i + 1)), true);
    }

  }

  protected void updateAllXtalMenuOptions() {

    //    "mkaddHydrogens??P!CB", "add hydrogens on new atoms",
    //    "mkclicktosetelement??P!CB", "allow clicking to set atom element",
    //    "mksel_atom", "select atom", 
    //    "mksel_position", "select position",
    //    "mkmode_molecular", GT.$("No View/Edit"),
    //    "mksymmetry_none", GT.$("do not apply"),
    //    "mksymmetry_retainLocal", GT.$("retain local"),
    //    "mksymmetry_applyLocal", GT.$("apply local"),
    //    "mksymmetry_applyFull", GT.$("apply full"),
    //    "mkunitcell_extend", GT.$("extend cell"),
    //    "mkunitcell_packed", GT.$("pack cell"),
    //    "mkasymmetricUnit", GT.$("asymmetric unit"),
    //    "mkallAtoms", GT.$("all atoms"),

    // mode
    String text = "";
    switch (modelkit.getMKState()) {
    case ModelKit.STATE_MOLECULAR:
      text = " (not enabled)";
      break;
    case ModelKit.STATE_XTALVIEW:
      text = " (view)";
      break;
    case ModelKit.STATE_XTALEDIT:
      text = " (edit)";
      break;
    }
    setLabel("xtalModePersistMenu", "Crystal Mode: " + text);

    // atom or position
    text = modelkit.getCenterText();
    setLabel("xtalSelPersistMenu",
        "Center: " + (text == null ? "(not selected)" : text));
    // operator
    text = modelkit.getSymopText();
    setLabel("operator", text == null ? "(no operator selected)" : text);

    // editing option
    switch (modelkit.getSymEditState()) {
    case ModelKit.STATE_SYM_NONE:
      text = "do not apply symmetry";
      break;
    case ModelKit.STATE_SYM_RETAINLOCAL:
      text = "retain local symmetry";
      break;
    case ModelKit.STATE_SYM_APPLYLOCAL:
      text = "apply local symmetry";
      break;
    case ModelKit.STATE_SYM_APPLYFULL:
      text = "apply full symmetry";
      break;
    }
    setLabel("xtalEditOptPersistMenu", "Edit option: " + text);

    // packing
    switch (modelkit.getUnitCellState()) {
    case ModelKit.STATE_UNITCELL_PACKED:
      text = "packed";
      break;
    case ModelKit.STATE_UNITCELL_EXTEND:
      text = "unpacked" + (modelkit.viewOffset == null ? "(no view offset)"
          : "(view offset=" + modelkit.viewOffset + ")");
      break;
    }
    setLabel("xtalPackingPersistMenu", "Packing: " + text);

  }

  private void setLabel(String key, String label) {
    menuSetLabel(htMenus.get(key), label);
  }

  /**
   * Set the active menu and request a repaint.
   * 
   * @param name
   * @return activeMenu or null
   */
  public String setActiveMenu(String name) {
    // TODO -- if the hovering is working, this should not be necessary
    String active = (name.indexOf(ModelKit.XTAL_MODE) >= 0 ? ModelKit.XTAL_MODE
        : name.indexOf(ModelKit.ATOM_MODE) >= 0 ? ModelKit.ATOM_MODE
            : name.indexOf(ModelKit.BOND_MODE) >= 0 ? ModelKit.BOND_MODE : null);
//    System.out.println("MKP setActiveMenu " + name + " " + active);
    if (active != null) {
      activeMenu = active;
      if ((active == ModelKit.XTAL_MODE) == (modelkit
          .getMKState() == ModelKit.STATE_MOLECULAR))
        modelkit.setMKState(active == ModelKit.XTAL_MODE ? ModelKit.STATE_XTALVIEW
            : ModelKit.STATE_MOLECULAR);
      vwr.refresh(Viewer.REFRESH_REPAINT, "modelkit");
      if (active == ModelKit.BOND_MODE && prevBondCheckBox == null)
        prevBondCheckBox = htMenus.get("assignBond_pP!RD");
    } else if (name.indexOf(ModelKit.OPTIONS_MODE) >= 0) {
        htMenus.get("undo").setEnabled(vwr.undoMoveAction(T.undomove, T.count) > 0);
        htMenus.get("redo").setEnabled(vwr.undoMoveAction(T.redomove, T.count) > 0);
    }
    //System.out.println("active menu is " + activeMenu + " state=" + getMKState());
    return active;
  }

  /**
   * Set the active menu based on updating a value -- usually by the user, but
   * also during setup (ignored).
   * 
   */
  @Override
  protected void appUpdateSpecialCheckBoxValue(SC source, String actionCommand,
                                               boolean selected) {
    if (source == null || !selected)
      return;
    String name = source.getName();
    String mode;
//    System.out.println("ModelKitPopup updating for show " + updatingForShow);
    if (!updatingForShow && (mode = setActiveMenu(name)) != null) {
      String text = source.getText();
      // don't turn this into a Java 8 switch -- we need this to still compile in Java 6 for legacy Jmol
      if (mode == ModelKit.BOND_MODE) {
        if (name.equals(bondRotationName)) {
          bondRotationCheckBox = source;
        } else {
          prevBondCheckBox = source;
        }
      }
      modelkit.setHoverLabel(activeMenu, text);
    }
  }

  protected void exitBondRotation() {
    
    // called by subclasses
    
    modelkit.exitBondRotation(prevBondCheckBox == null ? null : prevBondCheckBox.getText());
    vwr.setPickingMode(null, ActionManager.PICKING_ASSIGN_BOND);
    if (bondRotationCheckBox != null)
      bondRotationCheckBox.setSelected(false);
    if (prevBondCheckBox != null)
      prevBondCheckBox.setSelected(true);

  }

  /////////////////// menu execution //////////////

  @Override
  protected boolean appGetBooleanProperty(String name) {
    if (name.startsWith("mk")) {
      return ((Boolean) modelkit.getProperty(name.substring(2))).booleanValue();
    }
    return vwr.getBooleanProperty(name);
  }
  @Override
  public String getUnknownCheckBoxScriptToRun(SC item, String name, String what,
                                              boolean TF) {
    
    // From JmolGenericPopup.appRunSpecialCheckBox when name starts with "mk" or
    // has "??" in it.

    if (name.startsWith("mk")) {
      modelkit.processMKPropertyItem(name, TF);
      return null;
    }
    // must be ?? -- atom setting by user input
    String element = modelkit.getElementFromUser();
    if (element == null)
      return null;
    menuSetLabel(item, element);
    item.setActionCommand("assignAtom_" + element + "P!:??");
    return "set picking assignAtom_" + element;
    
  }

  @Override
  public void menuFocusCallback(String name, String actionCommand,
                                boolean gained) {
    if (gained && !modelkit.processSymop(name, true)) {
      setActiveMenu(name);
    }
  }

  @Override
  public void menuClickCallback(SC source, String script) {
    //action performed
    if (modelkit.processSymop(source.getName(), false))
      return;
    if (script.equals("clearQPersist")) {
      for (SC item : htCheckbox.values()) {
        if (item.getActionCommand().indexOf(":??") < 0)
          continue;
        menuSetLabel(item, "??");
        item.setActionCommand("_??P!:");
      }
      appRunScript("set picking assignAtom_C");
      return;
    }
    // may come back to getScriptForCallback
    doMenuClickCallback(source, script);
  }

  /**
   * Secondary processing of menu item click
   */
  @Override
  protected String getScriptForCallback(SC source, String id, String script) {
    if (script.startsWith("mk")) {
      modelkit.processXtalClick(id, script);
      script = null; // cancels any further processing
    }
    return script;
  }

  @Override
  protected boolean appRunSpecialCheckBox(SC item, String basename,
                                          String script, boolean TF) {
    if (basename.indexOf("assignAtom_Xx") == 0) {
      // drag to bond
      modelkit.resetAtomPickType();
    }
    if (TF && !updatingForShow && basename.indexOf("Bond") < 0) {
      updatingForShow = true;
      exitBondRotation();
      updatingForShow = false;
    }
    return super.appRunSpecialCheckBox(item, basename, script, TF);
  }

  public void updateCheckBoxesForModelKit(@SuppressWarnings("unused") String menuName) {
    String thisAtomType = "assignAtom_" + modelkit.pickAtomAssignType + "P";
    String thisBondType = "assignBond_"+modelkit.pickBondAssignType;
    for (Map.Entry<String, SC> entry : htCheckbox.entrySet()) {
      SC item = entry.getValue();
      String key = item.getActionCommand();
      if (key.startsWith(thisBondType) || key.startsWith(thisAtomType)) {
        updatingForShow = true;
        item.setSelected(false);
        item.setSelected(true);
        updatingForShow = false;
      }
    }
  }

}
