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

import java.util.Hashtable;
import java.util.Map;

import org.jmol.awtjs.swing.SC;
import org.jmol.i18n.GT;
import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.Bond;
import org.jmol.modelset.MeasurementPending;
import org.jmol.modelset.ModelSet;
import org.jmol.popup.JmolGenericPopup;
import org.jmol.popup.PopupResource;
import org.jmol.script.ScriptEval;
import org.jmol.script.ScriptException;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.Edge;
import org.jmol.util.Elements;
import org.jmol.util.Logger;
import org.jmol.viewer.ActionManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.MouseState;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.V3;

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
  
  private boolean hasUnitCell;
  private String[] allOperators;
  private int currentModelIndex = -1;
  
  private boolean alertedNoEdit;


  
  private String atomHoverLabel = "C", bondHoverLabel = GT.$("increase order"), xtalHoverLabel;
  private String activeMenu;
  private ModelSet lastModelSet;

  private String pickAtomAssignType = "C";
  private String pickBondAssignType = "p"; // increment up
  private boolean isPickAtomAssignCharge; // pl or mi

  private BS bsHighlight = new BS();

  private int bondIndex = -1, bondAtomIndex1 = -1, bondAtomIndex2 = -1;

  private BS bsRotateBranch;
  private int branchAtomIndex;
  private boolean isRotateBond;

  private int[] screenXY = new int[2]; // for tracking mouse-down on bond

  private Map<String, Object> mkdata = new Hashtable<String, Object>();
  
  

  private boolean showSymopInfo = true;
  
  /**
   * when TRUE, add H atoms to C when added to the modelSet. 
   */
  private boolean addXtalHydrogens = true;
  
  /**
   * Except for H atoms, do not allow changes to elements just by clicking them. 
   * This protects against doing that inadvertently when editing.
   * 
   */
  private boolean clickToSetElement = true; 
  
  private P3 centerPoint, spherePoint, viewOffset;
  private float centerDistance;
  private Object symop;
  private int centerAtomIndex = -1, secondAtomIndex = -1, atomIndexSphere = -1;
  private String drawData;
  private String drawScript;
  private int iatom0;

  public ModelKitPopup() {
  }

  @Override
  protected void initialize(Viewer vwr, PopupResource bundle, String title) {
   super.initialize(vwr, bundle, title); 
   initializeForModel();
  }

  //////////////// menu creation and update ///////////////
    
  private static final int MAX_LABEL = 32;

  private static PopupResource bundle = new ModelKitPopupResourceBundle(null, null);
  @Override
  protected PopupResource getBundle(String menu) {
    return bundle;
  }

  //  @Override
  //  public void jpiDispose() {
  //    super.jpiDispose();
  //  }

  public void initializeForModel() {
    resetBondFields("init");
    allOperators = null;
    currentModelIndex = -999;
    iatom0 = 0;
    atomIndexSphere = centerAtomIndex = secondAtomIndex = -1;
    centerPoint = spherePoint = null;
   // no! atomHoverLabel = bondHoverLabel = xtalHoverLabel = null;
    hasUnitCell = (vwr.getCurrentUnitCell() != null);
    symop = null;
    setDefaultState(hasUnitCell ? STATE_XTALVIEW : STATE_MOLECULAR);
    //setProperty("clicktosetelement",Boolean.valueOf(!hasUnitCell));
    //setProperty("addhydrogen",Boolean.valueOf(!hasUnitCell));
  }

  @Override
  public void jpiUpdateComputedMenus() {
    hasUnitCell = (vwr.getCurrentUnitCell() != null);
    if (!checkUpdateSymmetryInfo())
      updateAllXtalMenus();
  }

  @Override
  protected void appUpdateForShow() {
    updateAllXtalMenuOptions();
  }

  private boolean checkUpdateSymmetryInfo() {
    htMenus.get("xtalMenu").setEnabled(hasUnitCell);
    boolean isOK = true;
    if (vwr.ms != lastModelSet) {
      lastModelSet = vwr.ms;
      isOK = false;
    } else if (currentModelIndex == -1 || currentModelIndex != vwr.am.cmi) {
      isOK = false;
    }
    currentModelIndex = Math.max(vwr.am.cmi, 0);
    iatom0 = vwr.ms.am[currentModelIndex].firstAtomIndex;
    if (!isOK) {
      allOperators = null;
    }
    return isOK;
  }

  private void updateAllXtalMenus() {
    updateOperatorMenu();
    updateAllXtalMenuOptions();
  }

  private void updateOperatorMenu() {
    if (allOperators != null)
      return;
    String data = runScriptBuffered("show symop");
    allOperators = PT.split(data.trim().replace('\t', ' '), "\n");
    addAllCheckboxItems(htMenus.get("xtalOp!PersistMenu"), allOperators);
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

  private void updateAllXtalMenuOptions() {

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
    switch (getMKState()) {
    case STATE_MOLECULAR:
      text = " (not enabled)";
      break;
    case STATE_XTALVIEW:
      text = " (view)";
      break;
    case STATE_XTALEDIT:
      text = " (edit)";
      break;
    }
    setLabel("xtalModePersistMenu", "Crystal Mode: " + text);
    
    // atom or position
    text = (centerAtomIndex < 0 && centerPoint == null ? "(not selected)"
        : centerAtomIndex >= 0 ? vwr.getAtomInfo(centerAtomIndex) : centerPoint.toString());
    setLabel("xtalSelPersistMenu", "Center: " + text);
    // operator
    text = (symop == null ? "(no operator selected)" : symop instanceof Integer ? allOperators[((Integer) symop).intValue() - 1] : symop.toString());
    setLabel("operator", text);

    // editing option
    switch (getSymEditState()) {
    case STATE_SYM_NONE:
      text = "do not apply symmetry";
      break;
    case STATE_SYM_RETAINLOCAL:
      text = "retain local symmetry";
      break;
    case STATE_SYM_APPLYLOCAL:
      text = "apply local symmetry";
      break;
    case STATE_SYM_APPLYFULL:
      text = "apply full symmetry";
      break;
    }
    setLabel("xtalSymmetryPersistMenu", "Edit option: " + text);

    // packing
    switch (getUnitCellState()) {
    case STATE_UNITCELL_PACKED:
      text = "packed";
      break;
    case STATE_UNITCELL_EXTEND:
      text = "unpacked" + (viewOffset == null ? "(no view offset)" : "(view offset=" + viewOffset + ")");
      break;
    }
    setLabel("xtalPackingPersistMenu", "Packing: " + text);

  }

  private void setLabel(String key, String label) {
    menuSetLabel(htMenus.get(key), label);
  }

  /**
   * for FrankRender -- the thin box on the top left
   * 
   * @return [ "atomMenu" | "bondMenu" | "xtalMenu" | null ]
   */
  public String getActiveMenu() {
    return activeMenu;
  }

  /**
   * Set the active menu and request a repaint.
   * 
   * @param name
   * @return activeMenu or null
   */
  public String setActiveMenu(String name) {
    // TODO -- if the hovering is working, this should not be necessary
    String active = (name.indexOf("xtalMenu") >= 0 ? "xtalMenu"
        : name.indexOf("atomMenu") >= 0 ? "atomMenu"
            : name.indexOf("bondMenu") >= 0 ? "bondMenu" : null);
    if (active != null) {
      activeMenu = active;
      if ((active == "xtalMenu") == (getMKState() == STATE_MOLECULAR))
        setMKState(active == "xtalMenu" ? STATE_XTALVIEW : STATE_MOLECULAR);
      
      
      vwr.refresh(Viewer.REFRESH_REPAINT, "modelkit");
    }
//    System.out.println("active menu is " + activeMenu + " state=" + getMKState());
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
    String name = source.getName();
    if (!updatingForShow && setActiveMenu(name) != null) {
      String text = source.getText();
      if (name.indexOf("Bond") >= 0) {
        bondHoverLabel = text;
      }
      else if (name.indexOf("assignAtom") >= 0)
        atomHoverLabel = text;
      else if (activeMenu == "xtalMenu")
        xtalHoverLabel = atomHoverLabel = text;
    }
  }

  ////////////// modelkit state //////////////
  
  public final static int STATE_BITS_XTAL /* 0b00000000011*/ = 0x03;
  public final static int STATE_MOLECULAR /* 0b00000000000*/ = 0x00;
  public final static int STATE_XTALVIEW /* 0b00000000001*/ = 0x01;
  public final static int STATE_XTALEDIT /* 0b00000000010*/ = 0x02;

  public final static int STATE_BITS_SYM_VIEW /* 0b00000011100*/ = 0x1c;
  public final static int STATE_SYM_NONE      /* 0b00000000000*/ = 0x00;
  public final static int STATE_SYM_SHOW      /* 0b00000001000*/ = 0x08;

  public final static int STATE_BITS_SYM_EDIT   /* 0b00011100000*/ = 0xe0;
  public final static int STATE_SYM_APPLYLOCAL  /* 0b00000100000*/ = 0x20;
  public final static int STATE_SYM_RETAINLOCAL /* 0b00001000000*/ = 0x40;
  public final static int STATE_SYM_APPLYFULL   /* 0b00010000000*/ = 0x80;

  public final static int STATE_BITS_UNITCELL   /* 0b11100000000*/ = 0x700;
  public final static int STATE_UNITCELL_PACKED /* 0b00000000000*/ = 0x000;
  public final static int STATE_UNITCELL_EXTEND /* 0b00100000000*/ = 0x100;

  //  { "xtalModeMenu", "mkmode_molecular mkmode_view mkmode_edit" }, 
  public static final String MODE_OPTIONS = ";view;edit;molecular;";
  public static final String SYMMETRY_OPTIONS = ";none;applylocal;retainlocal;applyfull;";
  public static final String UNITCELL_OPTIONS = ";packed;extend;";
  public static final String BOOLEAN_OPTIONS = ";showsymopinfo;clicktosetelement;addhydrogen;addhydrogens;";
  public static final String SET_OPTIONS = ";element;";
  private static final P3 Pt000 = new P3();

  //  { "xtalSelMenu", "mksel_atom mksel_position" },
  //  { "xtalSelOpMenu", "mkselop_byop xtalOpMenu mkselop_addOffset mkselop_atom2" },

  private int state = STATE_MOLECULAR & STATE_SYM_NONE & STATE_SYM_APPLYFULL
      & STATE_UNITCELL_EXTEND; // 0x00
  private float rotationDeg;
  
  private boolean isXtalState() {
    return ((state & STATE_BITS_XTAL) != 0);
  }

  private void setMKState(int bits) {
    state = (state & ~STATE_BITS_XTAL) | (hasUnitCell ? bits : STATE_MOLECULAR);
  }

  private int getMKState() {
    return state & STATE_BITS_XTAL;
  }

  private void setSymEdit(int bits) {
    state = (state & ~STATE_BITS_SYM_EDIT) | bits;
  }

  private int getSymEditState() {
    return state & STATE_BITS_SYM_EDIT;
  }

  private int getViewState() {
    return state & STATE_BITS_SYM_VIEW;
  }

  private void setSymViewState(int bits) {
    state = (state & ~STATE_BITS_SYM_VIEW) | bits;
  }

  private int getSymViewState() {
    return state & STATE_BITS_SYM_VIEW;
  }

  private void setUnitCell(int bits) {
    state = (state & ~STATE_BITS_UNITCELL) | bits;
  }

  private int getUnitCellState() {
    return state & STATE_BITS_UNITCELL;
  }

  public boolean isPickAtomAssignCharge() {
    return isPickAtomAssignCharge;
  }

  /** Get a property of the modelkit.
   * 
   * @param data a name or an array with [name, value]
   * @return value
   */
  public Object getProperty(Object data) {
    String key = (data instanceof String ? data : ((Object[]) data)[0]).toString();
    Object value = (data instanceof String ? null : ((Object[]) data)[1]);
    return setProperty(key, value); 
  }
  
  /**
   * Modify the state by setting a property -- primarily from CmdExt.modelkit.
   * 
   * Also can be used for "get" purposes.
   * 
   * @param name
   * @param value
   * @return null or "get" value
   */
  public synchronized Object setProperty(String name, Object value) {
    name = name.toLowerCase().intern();
    //    if (value != null)
    //      System.out.println("ModelKitPopup.setProperty " + name + "=" + value);

    // getting only:
    
    if (name == "isMolecular") {
      return Boolean.valueOf(getMKState() == STATE_MOLECULAR);
    }
    
    if (name == "hoverlabel") {
      // no setting of this, only getting
      return getHoverLabel(((Integer) value).intValue());
    }

    if (name == "alloperators") {
      return allOperators;
    }

    if (name == "data") {
      return getData(value == null ? null : value.toString());
    }

    if (name == "invariant") {
      int iatom = (value instanceof BS ? ((BS) value).nextSetBit(0) : -1);
      P3 atom = vwr.ms.getAtom(iatom);
      if (atom == null)
        return null;
      return vwr.getSymmetryInfo(iatom, null, -1, atom, atom, T.array, null, 0,
          0, 0);
    }

    // set only (always returning null):

    if (name == "assignatom") {
      // standard entry point for an atom click in the ModelKit
      Object[] o = ((Object[]) value);
      String type = (String) o[0];
      int[] data = (int[]) o[1];
      int atomIndex = data[0];
      if (isVwrRotateBond()) {
        bondAtomIndex1 = atomIndex;
      } else if (!processAtomClick(data[0]) && (clickToSetElement
          || vwr.ms.getAtom(atomIndex).getElementNumber() == 1))
        assignAtom(atomIndex, type, data[1] >= 0, data[2] >= 0);
      return null;
    }

    if (name == "bondatomindex") {
      int i = ((Integer) value).intValue();
      if (i != bondAtomIndex2)
        bondAtomIndex1 = i;

      bsRotateBranch = null;
      return null;
    }

    if (name == "highlight") {
      if (value == null)
        bsHighlight = new BS();
      else
        bsHighlight = (BS) value;
      return null;
    }
    if (name == "mode") { // view, edit, or molecular
      boolean isEdit = ("edit".equals(value));
      setMKState("view".equals(value) ? STATE_XTALVIEW
          : isEdit ? STATE_XTALEDIT : STATE_MOLECULAR);
      if (isEdit)
        addXtalHydrogens = false;
      return null;
    }

    if (name == "symmetry") {
      setDefaultState(STATE_XTALEDIT);
      name = ((String) value).toLowerCase().intern();
      setSymEdit(name == "applylocal" ? STATE_SYM_APPLYLOCAL
          : name == "retainlocal" ? STATE_SYM_RETAINLOCAL
              : name == "applyfull" ? STATE_SYM_APPLYFULL : 0);
      showXtalSymmetry();
      return null;
    }

    if (name == "unitcell") { // packed or extend
      boolean isPacked = "packed".equals(value);
      setUnitCell(isPacked ? STATE_UNITCELL_PACKED : STATE_UNITCELL_EXTEND);
      viewOffset = (isPacked ? Pt000 : null);
      return null;
    }

    if (name == "symop") {
      setDefaultState(STATE_XTALVIEW);
      if (value != null) {
        symop = value;
        showSymop(symop);
      }
      return symop;
    }

    if (name == "center") {
      setDefaultState(STATE_XTALVIEW);
      P3 centerAtom = (P3) value;
      lastCenter = centerAtom.x + " " + centerAtom.y + " " + centerAtom.z;
      centerAtomIndex = (centerAtom instanceof Atom ? ((Atom) centerAtom).i
          : -1);
      atomIndexSphere = -1;
      secondAtomIndex = -1;
      processAtomClick(centerAtomIndex);
      return null;
    }

    if (name == "scriptassignbond") {
      appRunScript(
          "assign bond [{" + value + "}] \"" + pickBondAssignType + "\"");
      return null;
    }
    // set and get:

    if (name == "assignbond") {
      int[] data = (int[]) value;
      return assignBond(data[0], data[1]);
    }

    if (name == "atomtype") {
      if (value != null) {
        pickAtomAssignType = (String) value;
        isPickAtomAssignCharge = (pickAtomAssignType.equals("pl")
            || pickAtomAssignType.equals("mi"));
      }
      return pickAtomAssignType;
    }

    if (name == "bondtype") {
      if (value != null) {
        pickBondAssignType = ((String) value).substring(0, 1).toLowerCase();
      }
      return pickBondAssignType;
    }

    if (name == "bondindex") {
      if (value != null) {
        setBondIndex(((Integer) value).intValue(), false);
      }
      return (bondIndex < 0 ? null : Integer.valueOf(bondIndex));
    }

    if (name == "rotatebondindex") {
      if (value != null) {
        setBondIndex(((Integer) value).intValue(), true);
      }
      return (bondIndex < 0 ? null : Integer.valueOf(bondIndex));
    }

    if (name == "addhydrogen" || name == "addhydrogens") {
      if (value != null)
        addXtalHydrogens = isTrue(value);
      return Boolean.valueOf(addXtalHydrogens);
    }

    if (name == "clicktosetelement") {
      if (value != null)
        clickToSetElement = isTrue(value);
      return Boolean.valueOf(clickToSetElement);
    }

    if (name == "showsymopinfo") {
      if (value != null)
        showSymopInfo = isTrue(value);
      return Boolean.valueOf(showSymopInfo);
    }

    if (name == "offset") {
      if (value == "none") {
        viewOffset = null;
      } else if (value != null) {
        viewOffset = (value instanceof P3 ? (P3) value
            : pointFromTriad(value.toString()));
        if (viewOffset != null)
          setSymViewState(STATE_SYM_SHOW);
      }
      showXtalSymmetry();
      return viewOffset;
    }

    if (name == "distance") {
      setDefaultState(STATE_XTALEDIT);
      float d = (value == null ? Float.NaN
          : value instanceof Float ? ((Float) value).floatValue()
              : PT.parseFloat((String) value));
      if (!Float.isNaN(d)) {
        notImplemented("setProperty: distance");
        centerDistance = d;
      }
      return Float.valueOf(centerDistance);
    }

    if (name == "point") {
      if (value != null) {
        notImplemented("setProperty: point");
        setDefaultState(STATE_XTALEDIT);
        spherePoint = (P3) value;
        atomIndexSphere = (spherePoint instanceof Atom ? ((Atom) spherePoint).i
            : -1);
      }
      return spherePoint;
    }

    if (name == "screenxy") {
      if (value != null) {
        screenXY = (int[]) value;
      }
      return screenXY;
    }

    if (name == "addconstraint") {
      notImplemented("setProperty: addConstraint");
    }

    if (name == "removeconstraint") {
      notImplemented("setProperty: removeConstraint");
    }

    if (name == "removeallconstraints") {
      notImplemented("setProperty: removeAllConstraints");
    }

    System.err.println("ModelKitPopup.setProperty? " + name + " " + value);

    return null;
  }

  private static boolean isTrue(Object value) {
    return (Boolean.valueOf(value.toString()) == Boolean.TRUE);
  }

  private Object getData(String key) {
    addData("centerPoint" , centerPoint);
    addData("centerAtomIndex", Integer.valueOf(centerAtomIndex));
    addData("secondAtomIndex", Integer.valueOf(secondAtomIndex));
    addData("symop",  symop);
    addData("offset",  viewOffset);
    addData("drawData", drawData);
    addData("drawScript", drawScript);
    return mkdata;
  }

  private void addData(String key, Object value) {
    mkdata.put(key, value == null ? "null" : value);
  }

  /**
   * An atom has been clicked -- handle it. Called from CmdExt.assignAtom
   * from the script created in ActionManager.assignNew from Actionmanager.checkReleaseAction
   * 
   * @param atomIndex
   * @return true if handled
   */
  private boolean processAtomClick(int atomIndex) {
    switch (getMKState()) {
    case STATE_MOLECULAR:
      return isVwrRotateBond();
    case STATE_XTALVIEW:
      centerAtomIndex = atomIndex;
      if (getSymViewState() == STATE_SYM_NONE)
        setSymViewState(STATE_SYM_SHOW);
      showXtalSymmetry();
      return true;
    case STATE_XTALEDIT:
      if (atomIndex == centerAtomIndex)
        return true;
      notImplemented("edit click");
      return false;
    }
    notImplemented("atom click unknown XTAL state");
    return false;
  }

  /**
   * Called by Viewer.hoverOn to set the special label if desired.
   * 
   * @param atomIndex
   * @return special label or null
   */
  private String getHoverLabel(int atomIndex) {
    int state = getMKState();
    if (state != STATE_XTALVIEW && atomIndex >= 0 && !vwr.ms.isAtomInLastModel(atomIndex)) {
      return "Only atoms in the last model may be edited.";
    }
    String msg = null;
    switch (state) {
    case STATE_XTALVIEW:
      if (symop == null)
        symop = Integer.valueOf(1);
      msg = "view symop " + symop + " for "
          + vwr.getAtomInfo(atomIndex);
      break;
    case STATE_XTALEDIT:
      msg = "start editing for " + vwr.getAtomInfo(atomIndex);
      break;
    case STATE_MOLECULAR:
      if (isRotateBond) {
        if (atomIndex == bondAtomIndex1 || atomIndex == bondAtomIndex2) {
          msg = "rotate branch";
          branchAtomIndex = atomIndex;
          bsRotateBranch = null;
        } else {
          msg = "rotate bond";
          bsRotateBranch = null;
          branchAtomIndex = -1;
//          resetBondFields("gethover");
        }
      }  
      if (bondIndex < 0) {
        if (atomHoverLabel.length() <= 2) {
          msg = atomHoverLabel = "Click to change to " + atomHoverLabel
              + " or drag to add " + atomHoverLabel;
        } else {
          msg = atomHoverLabel;
          vwr.highlight(BSUtil.newAndSetBit(atomIndex));
        }
      } else {
        if (msg == null) {
          switch (bsHighlight.cardinality()) {
          case 0:
            vwr.highlight(BSUtil.newAndSetBit(atomIndex));
            //$FALL-THROUGH$
          case 1:
            msg = atomHoverLabel;
            break;
          case 2:
            msg = bondHoverLabel;
            break;
          }
        }
      }
      break;
    }
    return msg;
  }

  private void setDefaultState(int mode) {
    if (!hasUnitCell)
      mode = STATE_MOLECULAR;
    if (!hasUnitCell || isXtalState() != hasUnitCell) {
      setMKState(mode);
      switch (mode) {
      case STATE_MOLECULAR:
        break;
      case STATE_XTALVIEW:
        if (getSymViewState() == STATE_SYM_NONE)
          setSymViewState(STATE_SYM_SHOW);
        break;
      case STATE_XTALEDIT:
        break;
      }
    }
  }

  /////////////////// menu execution //////////////

  @Override
  protected boolean appGetBooleanProperty(String name) {
    if (name.startsWith("mk")) {
      return ((Boolean) getProperty(name.substring(2))).booleanValue();
    }
    return vwr.getBooleanProperty(name);
  }

  /**
   * From JmolGenericPopup.appRunSpecialCheckBox when name starts with "mk" or has "??" in it.
   */
  @Override
  public String getUnknownCheckBoxScriptToRun(SC item, String name, String what,
                                              boolean TF) {
    if (name.startsWith("mk")) {
      processMKPropertyItem(name, TF);
      return null;
    }
    // must be ?? -- atom setting by user input
    String element = promptUser(GT.$("Element?"), "");
    if (element == null || Elements.elementNumberFromSymbol(element, true) == 0)
      return null;
    menuSetLabel(item, element);
    item.setActionCommand("assignAtom_" + element + "P!:??");
    atomHoverLabel = "Click or click+drag for " + element;
    return "set picking assignAtom_" + element;
  }


  private void processMKPropertyItem(String name, boolean TF) {
    // set a property
    // { "xtalOptionsPersistMenu", "mkaddHydrogensCB mkclicktosetelementCB" }
    name = name.substring(2);
    int pt = name.indexOf("_");
    if (pt > 0) {
      setProperty(name.substring(0, pt), name.substring(pt + 1));
    } else {
      setProperty(name, Boolean.valueOf(TF));
    }
  }

  /**
   * Draw the symmetry element
   */
  private void showXtalSymmetry() {
    String script = null;
    switch (getSymViewState()) {
    case STATE_SYM_NONE:
      script = "draw * delete";
      break;
    case STATE_SYM_SHOW:
    default:
      P3 offset = null;
      if (secondAtomIndex >= 0) {
        script = "draw ID sym symop "
            + (centerAtomIndex < 0 ? centerPoint
                : " {atomindex=" + centerAtomIndex + "}")
            + " {atomindex=" + secondAtomIndex + "}";
      } else {
        offset = this.viewOffset;
        if (symop == null)
          symop = Integer.valueOf(1);
        int iatom = (centerAtomIndex >= 0 ? centerAtomIndex
            : centerPoint != null ? -1 : iatom0); // default to first atom
        script = "draw ID sym symop "
            + (symop == null ? "1"
                : symop instanceof String ? "'" + symop + "'"
                    : PT.toJSON(null, symop))
            + (iatom < 0 ? centerPoint : " {atomindex=" + iatom + "}")
            + (offset == null ? "" : " offset " + offset);
      }
      drawData = runScriptBuffered(script);
      drawScript = script;
      drawData = (showSymopInfo
          ? drawData.substring(0, drawData.indexOf("\n") + 1)
          : "");
      appRunScript(
           ";refresh;set echo top right;echo " + drawData.replace('\t', ' ')
          );
      break;
    }
  }

  /**
   * Original ModelKitPopup functionality -- assign an atom.
   * 
   * @param atomIndex
   * @param type
   * @param autoBond
   * @param addHsAndBond
   */
  private void assignAtom(int atomIndex, String type, boolean autoBond,
                          boolean addHsAndBond) {

    vwr.ms.clearDB(atomIndex);
    if (type == null)
      type = "C";

    // not as simple as just defining an atom.
    // if we click on an H, and C is being defined,
    // this sprouts an sp3-carbon at that position.

    Atom atom = vwr.ms.at[atomIndex];
    BS bs = new BS();
    boolean wasH = (atom.getElementNumber() == 1);
    int atomicNumber = (PT.isUpperCase(type.charAt(0)) ? Elements.elementNumberFromSymbol(type, true) : -1);

    // 1) change the element type or charge

    boolean isDelete = false;
    if (atomicNumber > 0) {
      vwr.ms.setElement(atom, atomicNumber, !addHsAndBond);
      vwr.shm.setShapeSizeBs(JC.SHAPE_BALLS, 0, vwr.rd,
          BSUtil.newAndSetBit(atomIndex));
      vwr.ms.setAtomName(atomIndex, type + atom.getAtomNumber(), !addHsAndBond);
      if (vwr.getBoolean(T.modelkitmode))
        vwr.ms.am[atom.mi].isModelKit = true;
      if (!vwr.ms.am[atom.mi].isModelKit)
        vwr.ms.taintAtom(atomIndex, AtomCollection.TAINT_ATOMNAME);
    } else if (type.toLowerCase().equals("pl")) {
      atom.setFormalCharge(atom.getFormalCharge() + 1);
    } else if (type.toLowerCase().equals("mi")) {
      atom.setFormalCharge(atom.getFormalCharge() - 1);
    } else if (type.equals("X")) {
      isDelete = true;
    } else if (!type.equals(".")) {
      return; // uninterpretable
    }

    if (!addHsAndBond)
      return;

    // 2) delete noncovalent bonds and attached hydrogens for that atom.

    vwr.ms.removeUnnecessaryBonds(atom, isDelete);

    // 3) adjust distance from previous atom.

    float dx = 0;
    if (atom.getCovalentBondCount() == 1)
      if (wasH) {
        dx = 1.50f;
      } else if (!wasH && atomicNumber == 1) {
        dx = 1.0f;
      }
    if (dx != 0) {
      V3 v = V3.newVsub(atom, vwr.ms.at[atom.getBondedAtomIndex(0)]);
      float d = v.length();
      v.normalize();
      v.scale(dx - d);
      vwr.ms.setAtomCoordRelative(atomIndex, v.x, v.y, v.z);
    }

    BS bsA = BSUtil.newAndSetBit(atomIndex);

    if (isDelete) {
      vwr.deleteAtoms(bsA, false);      
    }
    if (atomicNumber != 1 && autoBond) {

      // 4) clear out all atoms within 1.0 angstrom
      vwr.ms.validateBspf(false);
      bs = vwr.ms.getAtomsWithinRadius(1.0f, bsA, false, null);
      bs.andNot(bsA);
      if (bs.nextSetBit(0) >= 0)
        vwr.deleteAtoms(bs, false);

      // 5) attach nearby non-hydrogen atoms (rings)

      bs = vwr.getModelUndeletedAtomsBitSet(atom.mi);
      bs.andNot(vwr.ms.getAtomBitsMDa(T.hydrogen, null, new BS()));
      vwr.ms.makeConnections2(0.1f, 1.8f, 1, T.create, bsA, bs, null, false,
          false, 0);

      // 6) add hydrogen atoms

    }
    if (addXtalHydrogens)
      vwr.addHydrogens(bsA, false, true);
  }

  /**
   * Original ModelKit functionality -- assign a bond.
   * 
   * @param bondIndex
   * @param type
   * @return bit set of atoms to modify
   */
  private BS assignBond(int bondIndex, int type) {
    int bondOrder = type - '0';
    Bond bond = vwr.ms.bo[bondIndex];
    vwr.ms.clearDB(bond.atom1.i);
    switch (type) {
    case '0':
    case '1':
    case '2':
    case '3':
    case '4':
    case '5':
      break;
    case 'p':
    case 'm':
      bondOrder = Edge.getBondOrderNumberFromOrder(bond.getCovalentOrder())
          .charAt(0) - '0' + (type == 'p' ? 1 : -1);
      if (bondOrder > 3)
        bondOrder = 1;
      else if (bondOrder < 0)
        bondOrder = 3;
      break;
    default:
      return null;
    }
    BS bsAtoms = new BS();
    try {
      if (bondOrder == 0) {
        BS bs = new BS();
        bs.set(bond.index);
        bsAtoms.set(bond.atom1.i);
        bsAtoms.set(bond.atom2.i);
        vwr.ms.deleteBonds(bs, false);
      } else {
        bond.setOrder(bondOrder | Edge.BOND_NEW);
        if (bond.atom1.getElementNumber() != 1
            && bond.atom2.getElementNumber() != 1) {
          vwr.ms.removeUnnecessaryBonds(bond.atom1, false);
          vwr.ms.removeUnnecessaryBonds(bond.atom2, false);
        }
        bsAtoms.set(bond.atom1.i);
        bsAtoms.set(bond.atom2.i);
      }
    } catch (Exception e) {
      Logger.error("Exception in seBondOrder: " + e.toString());
    }
    if (type != '0' && addXtalHydrogens)
      vwr.addHydrogens(bsAtoms, false, true);
    return bsAtoms;
  }

  private boolean isVwrRotateBond() {
    return (vwr.acm.getBondPickingMode() == ActionManager.PICKING_ROTATE_BOND);
  }

  public int getRotateBondIndex() {
    return (getMKState() == STATE_MOLECULAR && isRotateBond ? bondIndex : -1);
  }
    
  private void resetBondFields(String where) {
    bsRotateBranch = null;
    // do not set bondIndex to -1 here
    branchAtomIndex = bondAtomIndex1 = bondAtomIndex2 = -1;
  }

  /**
   * Set the bond for rotation -- called by Sticks.checkObjectHovered via
   * Viewer.highlightBond.
   * 
   * 
   * @param index
   * @param isRotate
   */
  private void setBondIndex(int index, boolean isRotate) {
    if (!isRotate && isVwrRotateBond()) {
      vwr.setModelKitRotateBondIndex(index);
      return;
    }
    
    boolean haveBond = (bondIndex >= 0);
    if (!haveBond && index < 0)
      return;
    if (index < 0) {
      resetBondFields("setbondindex<0");
      return;
    }
    
    bsRotateBranch = null;
    branchAtomIndex = -1;   
    bondIndex = index;
    isRotateBond = isRotate;
    bondAtomIndex1 = vwr.ms.bo[index].getAtomIndex1();
    bondAtomIndex2 = vwr.ms.bo[index].getAtomIndex2();
    setActiveMenu("bondMenu");
  }


  /**
   * Actually rotate the bond. Called by ActionManager.checkDragWheelAction.
   * 
   * @param deltaX
   * @param deltaY
   * @param x
   * @param y
   */
  public void actionRotateBond(int deltaX, int deltaY, int x, int y, boolean forceFull) {
    
    if (bondIndex < 0)
      return;
    BS bsBranch = bsRotateBranch;
    Atom atomFix, atomMove;
    ModelSet ms = vwr.ms;
    if (forceFull) {
      bsBranch = null;
      branchAtomIndex = -1;
    }
    if (bsBranch == null) {
      Bond b = ms.bo[bondIndex];
      atomMove = (branchAtomIndex == b.atom1.i ? b.atom1 : b.atom2);
      atomFix = (atomMove == b.atom1 ? b.atom2 : b.atom1);
      vwr.undoMoveActionClear(atomFix.i, AtomCollection.TAINT_COORD, true);
      
      if (branchAtomIndex >= 0)       
        bsBranch = vwr.getBranchBitSet(atomMove.i, atomFix.i, true);
      if (bsBranch != null)
        for (int n = 0, i = atomFix.bonds.length; --i >= 0;) {
          if (bsBranch.get(atomFix.getBondedAtomIndex(i)) && ++n == 2) {
            bsBranch = null;
            break;
          }
        }
      if (bsBranch == null) {
        bsBranch = ms.getMoleculeBitSetForAtom(atomFix.i);
      }
      bsRotateBranch = bsBranch;
      bondAtomIndex1 = atomFix.i;
      bondAtomIndex2 = atomMove.i;
    } else {
      atomFix = ms.at[bondAtomIndex1];
      atomMove = ms.at[bondAtomIndex2];
    }
    V3 v1 = V3.new3(atomMove.sX - atomFix.sX, atomMove.sY - atomFix.sY, 0);
    V3 v2 = V3.new3(deltaX, deltaY, 0);
    v1.cross(v1, v2);
    float degrees = (v1.z > 0 ? 1 : -1) * v2.length();

    BS bs = BSUtil.copy(bsBranch);
    bs.andNot(vwr.slm.getMotionFixedAtoms());
    vwr.rotateAboutPointsInternal(null, atomFix, atomMove, 0, degrees, false, bs,
        null, null, null, null);
  }

////////////// more callback methods //////////////
  
  @Override
  public void menuFocusCallback(String name, String actionCommand,
                                boolean gained) {
    if (gained && !processSymop(name, true))
        setActiveMenu(name);
  }

  @Override
  public void menuClickCallback(SC source, String script) {
    doMenuClickCallbackMK(source, script);
  }
    
    public void doMenuClickCallbackMK(SC source, String script) {
      //action performed
      if (processSymop(source.getName(), false))
        return;
      if (script.equals("clearQPersist")) {
        for (SC item : htCheckbox.values()) {
          if (item.getActionCommand().indexOf(":??") < 0)
            continue;
          menuSetLabel(item, "??");
          item.setActionCommand("_??P!:");
          item.setSelected(false);
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
      processXtalClick(id, script);
      script = null; // cancels any further processing
    }
    return script;
  }

  private void processXtalClick(String id, String action) {
    if (processSymop(id, false))
      return;
    action = action.intern();
    if (action.startsWith("mkmode_")) {
      if (!alertedNoEdit && action == "mkmode_edit") {
        alertedNoEdit = true;
        vwr.alert("ModelKit xtal edit has not been implemented");
        return;
      }
      processModeClick(action);
    } else if (action.startsWith("mksel_")) {
      processSelClick(action);
    } else if (action.startsWith("mkselop_")) {
      processSelOpClick(action);
    } else if (action.startsWith("mksymmetry_")) {
      processSymClick(action);
    } else if (action.startsWith("mkunitcell_")) {
      processUCClick(action);
    } else {
      notImplemented("XTAL click " + action);
    }
    updateAllXtalMenuOptions();
  }
  private void processSelOpClick(String action) {
    secondAtomIndex = -1;
    if (action == "mkselop_addoffset") {
      String pos = promptUser("Enter i j k for an offset for viewing the operator - leave blank to clear", lastOffset);
      if (pos == null)
        return;
      lastOffset = pos;
      if (pos.length() == 0 || pos == "none") {
        setProperty("offset", "none");
        return;
      }
      P3 p = pointFromTriad(pos);
      if (p == null) {
        processSelOpClick(action);
      } else {
        setProperty("offset", p);
      }
    } else if (action == "mkselop_atom2") {
      notImplemented(action);
    }
  }

  private boolean processSymop(String id, boolean isFocus) {
    int pt = id.indexOf(".mkop_");
    if (pt >= 0) {
      Object op = symop;
      symop = Integer.valueOf(id.substring(pt + 6));
      showSymop(symop);
      if (isFocus) // temporary only
        symop = op;
      return true;
    }
    return false;
  }

  private void showSymop(Object symop) {
    secondAtomIndex = -1;
    this.symop = symop;
    showXtalSymmetry();
  }

  private void processModeClick(String action) {
    processMKPropertyItem(action, false); 
  }
  
  private String lastCenter = "0 0 0", lastOffset = "0 0 0";

  private void processSelClick(String action) {
    if (action == "mksel_atom") {
      centerPoint = null;
      centerAtomIndex = -1;
      secondAtomIndex = -1;
      // indicate next click is an atom
    } else if (action == "mksel_position") {
      String pos = promptUser("Enter three fractional coordinates", lastCenter);
      if (pos == null)
        return;
      lastCenter = pos;
      P3 p = pointFromTriad(pos);
      if (p == null) {
        processSelClick(action);
        return;
      }
      centerAtomIndex = -Integer.MAX_VALUE;
      centerPoint = p;
      showXtalSymmetry();
    }
  }

  private void processSymClick(String action) {
    if (action == "mksymmetry_none") {
      setSymEdit(STATE_SYM_NONE);
    } else {
      processMKPropertyItem(action, false); 
    }
  }

  private void processUCClick(String action) {
    processMKPropertyItem(action, false);
    showXtalSymmetry();
  }

  /**
   * Called from ActionManager for a drag-drop
   * 
   * @param pressed
   * @param dragged
   * @param index
   * @param countPlusIndices
   * @return true if handled here
   */
  public boolean handleDragAtom(MouseState pressed, MouseState dragged,
                                int[] countPlusIndices) {
    switch (getMKState()) {
    case STATE_MOLECULAR:
      return false;
    case STATE_XTALEDIT:
      if (countPlusIndices[0] > 2)
        return true;
      notImplemented("drag atom for XTAL edit");
      break;
    case STATE_XTALVIEW:
      if (getSymViewState() == STATE_SYM_NONE)
        setSymViewState(STATE_SYM_SHOW);
      switch (countPlusIndices[0]) {
      case 1:
        centerAtomIndex = countPlusIndices[1];
        secondAtomIndex = -1;
        break;
      case 2:
        centerAtomIndex = countPlusIndices[1];
        secondAtomIndex = countPlusIndices[2];
        break;
      }
      showXtalSymmetry();
      return true;
    }
    return true;
  }

  private static P3 pointFromTriad(String pos) {
    float[] a = PT.parseFloatArray(PT.replaceAllCharacters(pos,  "{,}", " "));
    return (a.length == 3 && !Float.isNaN(a[2]) ? P3.new3(a[0], a[1], a[2]) : null);
  }

  private static void notImplemented(String action) {
    System.err.println("ModelKitPopup.notImplemented(" + action + ")");
  }

  private String promptUser(String msg, String def) {
    return vwr.prompt(msg, def, null, false);
  }

  private String runScriptBuffered(String script) {
    SB sb = new SB();
    try {
      System.out.println("MKP\n" + script);
      ((ScriptEval) vwr.eval).runBufferedSafely(script, sb);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return sb.toString();
  }

  /**
   * C
   * 
   * @param pressed
   * @param dragged
   * @param mp
   * @param dragAtomIndex
   * @return true if we should do a refresh now
   */
  public boolean handleAssignNew(MouseState pressed, MouseState dragged,
                                 MeasurementPending mp, int dragAtomIndex) {

    // H C + -, etc.
    // also check valence and add/remove H atoms as necessary?
    boolean inRange = pressed.inRange(ActionManager.XY_RANGE, dragged.x,
        dragged.y);

    if (inRange) {
      dragged.x = pressed.x;
      dragged.y = pressed.y;
    }

    if (handleDragAtom(pressed, dragged, mp.countPlusIndices))
      return true;
    boolean isCharge = isPickAtomAssignCharge;
    String atomType = pickAtomAssignType;
    if (mp.count == 2) {
      vwr.undoMoveActionClear(-1, T.save, true);
      appRunScript("assign connect " + mp.getMeasurementScript(" ", false));
    } else if (atomType.equals("Xx")) {
      return false;
    } else {
      if (inRange) {
        String s = "assign atom ({" + dragAtomIndex + "}) \"" + atomType + "\"";
        if (isCharge) {
          s += ";{atomindex=" + dragAtomIndex + "}.label='%C'; ";
          vwr.undoMoveActionClear(dragAtomIndex,
              AtomCollection.TAINT_FORMALCHARGE, true);
        } else {
          vwr.undoMoveActionClear(-1, T.save, true);
        }
        appRunScript(s);
      } else if (!isCharge) {
        vwr.undoMoveActionClear(-1, T.save, true);
        Atom a = vwr.ms.at[dragAtomIndex];
        if (a.getElementNumber() == 1) {
          vwr.assignAtom(dragAtomIndex, "X", null);
          //          runScript("assign atom ({" + dragAtomIndex + "}) \"X\"");
        } else {
          int x = dragged.x;
          int y = dragged.y;

          if (vwr.antialiased) {
            x <<= 1;
            y <<= 1;
          }

          P3 ptNew = P3.new3(x, y, a.sZ);
          vwr.tm.unTransformPoint(ptNew, ptNew);
          vwr.assignAtom(dragAtomIndex, atomType, ptNew);
        }
      }
    }
    return true;
  }

}
