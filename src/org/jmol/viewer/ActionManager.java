/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-07-31 09:22:19 -0500 (Fri, 31 Jul 2009) $
 * $Revision: 11291 $
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

import java.util.Map;

import org.jmol.api.EventManager;
import org.jmol.api.GenericPlatform;
import org.jmol.api.Interface;
import org.jmol.awtjs.Event;
import org.jmol.i18n.GT;

import javajs.util.AU;
import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.MeasurementPending;
import org.jmol.script.SV;
import org.jmol.script.ScriptEval;
import org.jmol.script.T;
import org.jmol.thread.HoverWatcherThread;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Point3fi;

import javajs.util.P3;
import javajs.util.PT;

import org.jmol.util.Rectangle;
import org.jmol.viewer.binding.Binding;
import org.jmol.viewer.binding.JmolBinding;

public class ActionManager implements EventManager {

  protected Viewer vwr;
  protected boolean haveMultiTouchInput;  
  protected boolean isMultiTouch;
  
  public Binding b;

  private Binding jmolBinding;
  private Binding pfaatBinding;
  private Binding dragBinding;
  private Binding rasmolBinding;
  private Binding predragBinding;
  private int LEFT_CLICKED;
  private int LEFT_DRAGGED;
  
  /**
   * 
   * @param vwr
   * @param commandOptions
   */
  public void setViewer(Viewer vwr, String commandOptions) {
    this.vwr = vwr;
    if (!Viewer.isJS)
      createActions();
    setBinding(jmolBinding = new JmolBinding());
    LEFT_CLICKED = Binding.getMouseAction(1, Binding.LEFT, Event.CLICKED);
    LEFT_DRAGGED = Binding.getMouseAction(1, Binding.LEFT, Event.DRAGGED);
    dragGesture = new Gesture(20, vwr);
  }

  protected Thread hoverWatcherThread;

  public void checkHover() {
    if (zoomTrigger) {
      zoomTrigger = false;
      if (vwr.currentCursor == GenericPlatform.CURSOR_ZOOM)
        vwr.setCursor(GenericPlatform.CURSOR_DEFAULT);
      vwr.setInMotion(false);
      return;
    }
    if (!vwr.getInMotion(true) && !vwr.tm.spinOn && !vwr.tm.navOn
        && !vwr.checkObjectHovered(current.x, current.y)) {
      int atomIndex = vwr.findNearestAtomIndex(current.x, current.y);
      if (atomIndex < 0)
        return;
      boolean isLabel = (apm == PICKING_LABEL && bnd(
          Binding
              .getMouseAction(clickedCount, moved.modifiers, Event.DRAGGED),
          ACTION_dragLabel));
      vwr.hoverOn(atomIndex, isLabel);
    }
  }

  /**
   * 
   * Specific to ActionManagerMT -- for processing SparshUI gestures
   * 
   * @param groupID
   * @param eventType
   * @param touchID
   * @param iData
   * @param pt
   * @param time
   */
  public void processMultitouchEvent(int groupID, int eventType, int touchID, int iData,
                           P3 pt, long time) {
    // see subclass
  }

  /**
   * 
   * @param desc
   * @param name
   */
  void bind(String desc, String name) {
    int jmolAction = getActionFromName(name);
    int mouseAction = Binding.getMouseActionStr(desc);
    if (mouseAction == 0)
      return;
    if (jmolAction >= 0) {
      b.bindAction(mouseAction, jmolAction);
    } else {
      b.bindName(mouseAction, name);
    }
  }

  protected void clearBindings() {
    setBinding(jmolBinding = new JmolBinding());
    pfaatBinding = null;
    dragBinding = null;
    rasmolBinding = null;
  }

  void unbindAction(String desc, String name) {
    if (desc == null && name == null) {
      clearBindings();
      return;
    }
    int jmolAction = getActionFromName(name);
    int mouseAction = Binding.getMouseActionStr(desc);
    if (jmolAction >= 0)
      b.unbindAction(mouseAction, jmolAction);
    else if (mouseAction != 0)
      b.unbindName(mouseAction, name);
    if (name == null)
      b.unbindUserAction(desc);
  }

  //// Gestures
  
  private Gesture dragGesture;

  /*
   * a "Jmol action" is one of these:
   * 
   * A Jmol action is "bound" to a mouse action by the 
   * simple act of concatenating string "jmol action" + \t + "mouse action"
   *  
   * 
   */
  public final static int ACTION_assignNew = 0;
  public final static int ACTION_center = 1;
  public final static int ACTION_clickFrank = 2;
  public final static int ACTION_connectAtoms = 3;
  public final static int ACTION_deleteAtom = 4;
  public final static int ACTION_deleteBond = 5;
  public final static int ACTION_depth = 6;
  public final static int ACTION_dragAtom = 7;
  public final static int ACTION_dragDrawObject = 8;
  public final static int ACTION_dragDrawPoint = 9;
  public final static int ACTION_dragLabel = 10;
  public final static int ACTION_dragMinimize = 11;
  public final static int ACTION_dragMinimizeMolecule = 12;
  public final static int ACTION_dragSelected = 13;
  public final static int ACTION_dragZ = 14;
  public final static int ACTION_multiTouchSimulation = 15;
  public final static int ACTION_navTranslate = 16;
  public final static int ACTION_pickAtom = 17;
  public final static int ACTION_pickIsosurface = 18;
  public final static int ACTION_pickLabel = 19;
  public final static int ACTION_pickMeasure = 20;
  public final static int ACTION_pickNavigate = 21;
  public final static int ACTION_pickPoint = 22;
  public final static int ACTION_popupMenu = 23;
  public final static int ACTION_reset = 24;
  public final static int ACTION_rotate = 25;
  public final static int ACTION_rotateBranch = 26;
  public final static int ACTION_rotateSelected = 27;
  public final static int ACTION_rotateZ = 28;
  public final static int ACTION_rotateZorZoom = 29;
  public final static int ACTION_select = 30;
  public final static int ACTION_selectAndDrag = 31;
  public final static int ACTION_selectAndNot = 32;
  public final static int ACTION_selectNone = 33;
  public final static int ACTION_selectOr = 34;
  public final static int ACTION_selectToggle = 35;
  public final static int ACTION_selectToggleExtended = 36;
  public final static int ACTION_setMeasure = 37;
  public final static int ACTION_slab = 38;
  public final static int ACTION_slabAndDepth = 39;
  public final static int ACTION_slideZoom = 40;
  public final static int ACTION_spinDrawObjectCCW = 41;
  public final static int ACTION_spinDrawObjectCW = 42;
  public final static int ACTION_stopMotion = 43;
  public final static int ACTION_swipe = 44;
  public final static int ACTION_translate = 45;
  public final static int ACTION_wheelZoom = 46;
  public final static int ACTION_count = 47;

  final static String[] actionInfo = new String[ACTION_count];
  final static String[] actionNames = new String[ACTION_count];

  static void newAction(int i, String name, String info) {
    actionInfo[i] = info;
    actionNames[i] = name;
  }

  void createActions() {
    if (actionInfo[ACTION_assignNew] != null)
      return;
    // OK for J2S because actionInfo and actionNames are both private
    newAction(ACTION_assignNew, "_assignNew", GT.o(GT.$(
        "assign/new atom or bond (requires {0})"),
        "set picking assignAtom_??/assignBond_?"));
    newAction(ACTION_center, "_center", GT.$("center"));
    newAction(ACTION_clickFrank, "_clickFrank", GT
        .$("pop up recent context menu (click on Jmol frank)"));
    newAction(ACTION_deleteAtom, "_deleteAtom", GT.o(GT.$(
        "delete atom (requires {0})"), "set picking DELETE ATOM"));
    newAction(ACTION_deleteBond, "_deleteBond", GT.o(GT.$(
        "delete bond (requires {0})"), "set picking DELETE BOND"));
    newAction(ACTION_depth, "_depth", GT.o(GT.$(
        "adjust depth (back plane; requires {0})"), "SLAB ON"));
    newAction(ACTION_dragAtom, "_dragAtom", GT.o(GT.$("move atom (requires {0})"),
        "set picking DRAGATOM"));
    newAction(ACTION_dragDrawObject, "_dragDrawObject", GT.o(GT.$(
        "move whole DRAW object (requires {0})"), "set picking DRAW"));
    newAction(ACTION_dragDrawPoint, "_dragDrawPoint", GT.o(GT.$(
        "move specific DRAW point (requires {0})"), "set picking DRAW"));
    newAction(ACTION_dragLabel, "_dragLabel", GT.o(GT.$("move label (requires {0})"),
        "set picking LABEL"));
    newAction(ACTION_dragMinimize, "_dragMinimize", GT.o(GT.$(
        "move atom and minimize molecule (requires {0})"),
        "set picking DRAGMINIMIZE"));
    newAction(ACTION_dragMinimizeMolecule, "_dragMinimizeMolecule", GT.o(GT.$(
        "move and minimize molecule (requires {0})"),
        "set picking DRAGMINIMIZEMOLECULE"));
    newAction(ACTION_dragSelected, "_dragSelected", GT.o(GT.$(
        "move selected atoms (requires {0})"), "set DRAGSELECTED"));
    newAction(ACTION_dragZ, "_dragZ", GT.o(GT.$(
        "drag atoms in Z direction (requires {0})"), "set DRAGSELECTED"));
    newAction(ACTION_multiTouchSimulation, "_multiTouchSimulation", GT
        .$("simulate multi-touch using the mouse)"));
    newAction(ACTION_navTranslate, "_navTranslate", GT.o(GT.$(
        "translate navigation point (requires {0} and {1})"), new String[] {
            "set NAVIGATIONMODE", "set picking NAVIGATE" }));
    newAction(ACTION_pickAtom, "_pickAtom", GT.$("pick an atom"));
    newAction(ACTION_connectAtoms, "_pickConnect", GT.o(GT.$(
        "connect atoms (requires {0})"), "set picking CONNECT"));
    newAction(ACTION_pickIsosurface, "_pickIsosurface", GT.o(GT.$(
        "pick an ISOSURFACE point (requires {0}"), "set DRAWPICKING"));
    newAction(ACTION_pickLabel, "_pickLabel", GT.o(GT.$(
        "pick a label to toggle it hidden/displayed (requires {0})"),
        "set picking LABEL"));
    newAction(
        ACTION_pickMeasure,
        "_pickMeasure",
        GT.o(GT
            .$(
                "pick an atom to include it in a measurement (after starting a measurement or after {0})"),
                "set picking DISTANCE/ANGLE/TORSION"));
    newAction(ACTION_pickNavigate, "_pickNavigate", GT.o(GT.$(
        "pick a point or atom to navigate to (requires {0})"),
        "set NAVIGATIONMODE"));
    newAction(ACTION_pickPoint, "_pickPoint", GT.o(GT
        .$("pick a DRAW point (for measurements) (requires {0}"),
            "set DRAWPICKING"));
    newAction(ACTION_popupMenu, "_popupMenu", GT
        .$("pop up the full context menu"));
    newAction(ACTION_reset, "_reset", GT
        .$("reset (when clicked off the model)"));
    newAction(ACTION_rotate, "_rotate", GT.$("rotate"));
    newAction(ACTION_rotateBranch, "_rotateBranch", GT.o(GT.$(
        "rotate branch around bond (requires {0})"), "set picking ROTATEBOND"));
    newAction(ACTION_rotateSelected, "_rotateSelected", GT.o(GT.$(
        "rotate selected atoms (requires {0})"), "set DRAGSELECTED"));
    newAction(ACTION_rotateZ, "_rotateZ", GT.$("rotate Z"));
    newAction(
        ACTION_rotateZorZoom,
        "_rotateZorZoom",
        GT
            .$("rotate Z (horizontal motion of mouse) or zoom (vertical motion of mouse)"));
    newAction(ACTION_select, "_select", GT.o(GT.$("select an atom (requires {0})"),
        "set pickingStyle EXTENDEDSELECT"));
    newAction(ACTION_selectAndDrag, "_selectAndDrag", GT.o(GT.$(
        "select and drag atoms (requires {0})"), "set DRAGSELECTED"));
    newAction(ACTION_selectAndNot, "_selectAndNot", GT.o(GT.$(
        "unselect this group of atoms (requires {0})"),
        "set pickingStyle DRAG/EXTENDEDSELECT"));
    newAction(ACTION_selectNone, "_selectNone", GT.o(GT.$(
        "select NONE (requires {0})"), "set pickingStyle EXTENDEDSELECT"));
    newAction(ACTION_selectOr, "_selectOr", GT.o(GT.$(
        "add this group of atoms to the set of selected atoms (requires {0})"),
        "set pickingStyle DRAG/EXTENDEDSELECT"));
    newAction(ACTION_selectToggle, "_selectToggle", GT.o(GT.$(
        "toggle selection (requires {0})"),
        "set pickingStyle DRAG/EXTENDEDSELECT/RASMOL"));
    newAction(
        ACTION_selectToggleExtended,
        "_selectToggleOr",
        GT.o(GT
            .$(
                "if all are selected, unselect all, otherwise add this group of atoms to the set of selected atoms (requires {0})"),
                "set pickingStyle DRAG"));
    newAction(ACTION_setMeasure, "_setMeasure", GT
        .$("pick an atom to initiate or conclude a measurement"));
    newAction(ACTION_slab, "_slab", GT.o(GT.$(
        "adjust slab (front plane; requires {0})"), "SLAB ON"));
    newAction(ACTION_slabAndDepth, "_slabAndDepth", GT.o(GT.$(
        "move slab/depth window (both planes; requires {0})"), "SLAB ON"));
    newAction(ACTION_slideZoom, "_slideZoom", GT
        .$("zoom (along right edge of window)"));
    newAction(
        ACTION_spinDrawObjectCCW,
        "_spinDrawObjectCCW",
        GT.o(GT
            .$(
                "click on two points to spin around axis counterclockwise (requires {0})"),
                "set picking SPIN"));
    newAction(ACTION_spinDrawObjectCW, "_spinDrawObjectCW", GT.o(GT.$(
        "click on two points to spin around axis clockwise (requires {0})"),
        "set picking SPIN"));
    newAction(ACTION_stopMotion, "_stopMotion", GT.o(GT.$(
        "stop motion (requires {0})"), "set waitForMoveTo FALSE"));
    newAction(
        ACTION_swipe,
        "_swipe",
        GT
            .$("spin model (swipe and release button and stop motion simultaneously)"));
    newAction(ACTION_translate, "_translate", GT.$("translate"));
    newAction(ACTION_wheelZoom, "_wheelZoom", GT.$("zoom"));
  }

  public static String getActionName(int i) {
    return (i < actionNames.length ? actionNames[i] : null);
  }

  public static int getActionFromName(String name) {
    for (int i = 0; i < actionNames.length; i++)
      if (actionNames[i].equalsIgnoreCase(name))
        return i;
    return -1;
  }

  public String getBindingInfo(String qualifiers) {
    return b.getBindingInfo(actionInfo, actionNames, qualifiers);
  }

  protected void setBinding(Binding newBinding) {
    // overridden in ActionManagerMT
    b = newBinding;
  }

  boolean bnd(int mouseAction, int... jmolActions) {
    for (int i = jmolActions.length; --i >= 0;)
      if (b.isBound(mouseAction, jmolActions[i]))
        return true;
    return false;
  }

  private boolean isDrawOrLabelAction(int a) {
    return (drawMode && bnd(a, ACTION_dragDrawObject, ACTION_dragDrawPoint) 
        || labelMode && bnd(a, ACTION_dragLabel));
  }

  /**
   * picking modes set picking....
   */
  
  private int apm = PICKING_IDENTIFY;
  private int bondPickingMode;

  public final static int PICKING_MK_RESET = -1;
  public final static int PICKING_OFF = 0;
  public final static int PICKING_IDENTIFY = 1;
  public final static int PICKING_LABEL = 2;
  public final static int PICKING_CENTER = 3;
  public final static int PICKING_DRAW = 4;
  public final static int PICKING_SPIN = 5;
  public final static int PICKING_SYMMETRY = 6;
  public final static int PICKING_DELETE_ATOM = 7;
  public final static int PICKING_DELETE_BOND = 8;
  public final static int PICKING_SELECT_ATOM = 9;
  public final static int PICKING_SELECT_GROUP = 10;
  public final static int PICKING_SELECT_CHAIN = 11;
  public final static int PICKING_SELECT_MOLECULE = 12;
  public final static int PICKING_SELECT_POLYMER = 13;
  public final static int PICKING_SELECT_STRUCTURE = 14;
  public final static int PICKING_SELECT_SITE = 15;
  public final static int PICKING_SELECT_MODEL = 16;
  public final static int PICKING_SELECT_ELEMENT = 17;
  public final static int PICKING_MEASURE = 18;
  public final static int PICKING_MEASURE_DISTANCE = 19;
  public final static int PICKING_MEASURE_ANGLE = 20;
  public final static int PICKING_MEASURE_TORSION = 21;
  public final static int PICKING_MEASURE_SEQUENCE = 22;
  public final static int PICKING_NAVIGATE = 23;
  public final static int PICKING_CONNECT = 24;
  public final static int PICKING_STRUTS = 25;
  public final static int PICKING_DRAG_SELECTED = 26;
  public final static int PICKING_DRAG_MOLECULE = 27;
  public final static int PICKING_DRAG_ATOM = 28;
  public final static int PICKING_DRAG_MINIMIZE = 29;
  public final static int PICKING_DRAG_MINIMIZE_MOLECULE = 30; // for docking
  public final static int PICKING_INVERT_STEREO = 31;
  public final static int PICKING_ASSIGN_ATOM = 32;
  public final static int PICKING_ASSIGN_BOND = 33;
  public final static int PICKING_ROTATE_BOND = 34;
  public final static int PICKING_IDENTIFY_BOND = 35;
  public final static int PICKING_DRAG_LIGAND = 36;

  /**
   * picking styles
   */
  public final static int PICKINGSTYLE_SELECT_JMOL = 0;
  public final static int PICKINGSTYLE_SELECT_CHIME = 0;
  public final static int PICKINGSTYLE_SELECT_RASMOL = 1;
  public final static int PICKINGSTYLE_SELECT_PFAAT = 2;
  public final static int PICKINGSTYLE_SELECT_DRAG = 3;
  public final static int PICKINGSTYLE_MEASURE_ON = 4;
  public final static int PICKINGSTYLE_MEASURE_OFF = 5;

  private final static String[] pickingModeNames;
  static {
    pickingModeNames = "off identify label center draw spin symmetry deleteatom deletebond atom group chain molecule polymer structure site model element measure distance angle torsion sequence navigate connect struts dragselected dragmolecule dragatom dragminimize dragminimizemolecule invertstereo assignatom assignbond rotatebond identifybond dragligand".split(" ");
  }
  
  public final static String getPickingModeName(int pickingMode) {
    return (pickingMode < 0 || pickingMode >= pickingModeNames.length ? "off"
        : pickingModeNames[pickingMode]);
  }

  public final static int getPickingMode(String str) {
    for (int i = pickingModeNames.length; --i >= 0;)
      if (str.equalsIgnoreCase(pickingModeNames[i]))
        return i;
    return -1;
  }

  private final static String[] pickingStyleNames;
  
  static {
    pickingStyleNames = "toggle selectOrToggle extendedSelect drag measure measureoff".split(" ");
  }
  
  public final static String getPickingStyleName(int pickingStyle) {
    return (pickingStyle < 0 || pickingStyle >= pickingStyleNames.length ? "toggle"
        : pickingStyleNames[pickingStyle]);
  }

  public final static int getPickingStyleIndex(String str) {
    for (int i = pickingStyleNames.length; --i >= 0;)
      if (str.equalsIgnoreCase(pickingStyleNames[i]))
        return i;
    return -1;
  }

  int getAtomPickingMode() {
    return apm;
  }

  void setPickingMode(int pickingMode) {
    boolean isNew = false;
    switch (pickingMode) {
    case PICKING_MK_RESET: 
      // from  set modelkit OFF only
      isNew = true;
      bondPickingMode = PICKING_IDENTIFY_BOND;
      pickingMode = PICKING_IDENTIFY;
      vwr.setStringProperty("pickingStyle", "toggle");
      vwr.setBooleanProperty("bondPicking", false);
      break;
    case PICKING_IDENTIFY_BOND:
    case PICKING_ROTATE_BOND:
    case PICKING_ASSIGN_BOND:
      vwr.setBooleanProperty("bondPicking", true);
      bondPickingMode = pickingMode;
      return;
    case PICKING_DELETE_BOND:
      bondPickingMode = pickingMode;
      if (vwr.getBondPicking())
        return;
      isNew = true;
      break;
    // if we have bondPicking mode, then we don't set atomPickingMode to this
    }
    isNew |= (apm != pickingMode);
    apm = pickingMode;
    if (isNew)
      resetMeasurement();
  }

  private int pickingStyle;
  private int pickingStyleSelect = PICKINGSTYLE_SELECT_JMOL;
  private int pickingStyleMeasure = PICKINGSTYLE_MEASURE_OFF;
  private int rootPickingStyle = PICKINGSTYLE_SELECT_JMOL;
  
  
  public String getPickingState() {
    // the pickingMode is not reported in the state. But when we do an UNDO,
    // we want to restore this.
    String script = ";set modelkitMode " + vwr.getBoolean(T.modelkitmode)
        + ";set picking " + getPickingModeName(apm);
    if (apm == PICKING_ASSIGN_ATOM)
      script += "_" + vwr.getModelkitProperty("atomType");
    script += ";";
    if (bondPickingMode != PICKING_OFF)
      script += "set picking " + getPickingModeName(bondPickingMode);
    if (bondPickingMode == PICKING_ASSIGN_BOND)
      script += "_" + vwr.getModelkitProperty("bondType");
    script += ";";
    return script;
  }

  int getPickingStyle() {
    return pickingStyle;
  }

  void setPickingStyle(int pickingStyle) {
    this.pickingStyle = pickingStyle;
    if (pickingStyle >= PICKINGSTYLE_MEASURE_ON) {
      pickingStyleMeasure = pickingStyle;
      resetMeasurement();
    } else {
      if (pickingStyle < PICKINGSTYLE_SELECT_DRAG)
        rootPickingStyle = pickingStyle;
      pickingStyleSelect = pickingStyle;
    }
    rubberbandSelectionMode = false;
    switch (pickingStyleSelect) {
    case PICKINGSTYLE_SELECT_PFAAT:
      if (!b.name.equals("extendedSelect"))
        setBinding(pfaatBinding == null ? pfaatBinding = Binding
            .newBinding(vwr, "Pfaat") : pfaatBinding);
      break;
    case PICKINGSTYLE_SELECT_DRAG:
      if (!b.name.equals("drag"))
        setBinding(dragBinding == null ? dragBinding = Binding
            .newBinding(vwr, "Drag") : dragBinding);
      rubberbandSelectionMode = true;
      break;
    case PICKINGSTYLE_SELECT_RASMOL:
      if (!b.name.equals("selectOrToggle"))
        setBinding(rasmolBinding == null ? rasmolBinding = Binding
            .newBinding(vwr, "Rasmol") : rasmolBinding);
      break;
    default:
      if (b != jmolBinding)
        setBinding(jmolBinding);
    }
    if (!b.name.equals("drag"))
      predragBinding = b;
  }


  
  private final static long MAX_DOUBLE_CLICK_MILLIS = 700;
  protected final static long MININUM_GESTURE_DELAY_MILLISECONDS = 10;
  private final static int SLIDE_ZOOM_X_PERCENT = 98;
  public final static float DEFAULT_MOUSE_DRAG_FACTOR = 1f;
  public final static float DEFAULT_MOUSE_WHEEL_FACTOR = 1.15f;
  public final static float DEFAULT_GESTURE_SWIPE_FACTOR = 1f;


  protected int xyRange = 10; // BH 2019.04.21 was 0

  private float gestureSwipeFactor = DEFAULT_GESTURE_SWIPE_FACTOR;
  protected float mouseDragFactor = DEFAULT_MOUSE_DRAG_FACTOR;
  protected float mouseWheelFactor = DEFAULT_MOUSE_WHEEL_FACTOR;

  void setGestureSwipeFactor(float factor) {
    gestureSwipeFactor = factor;
  }

  void setMouseDragFactor(float factor) {
    mouseDragFactor = factor;
  }

  void setMouseWheelFactor(float factor) {
    mouseWheelFactor = factor;
  }

  protected final MouseState current = new MouseState("current");
  protected final MouseState moved = new MouseState("moved");
  private final MouseState clicked = new MouseState("clicked");
  private final MouseState pressed = new MouseState("pressed");
  private final MouseState dragged = new MouseState("dragged");

  protected void setCurrent(long time, int x, int y, int mods) {
    vwr.hoverOff();
    current.set(time, x, y, mods);
  }

  int getCurrentX() {
    return current.x;
  }

  int getCurrentY() {
    return current.y;
  }

  protected int pressedCount;
  protected int clickedCount;

  private boolean drawMode;
  private boolean labelMode;
  private boolean dragSelectedMode;
  private boolean measuresEnabled = true;
  private boolean haveSelection;

  public void setMouseMode() {
    drawMode = labelMode = false;
    dragSelectedMode = vwr.getDragSelected();
    measuresEnabled = !dragSelectedMode;
    if (!dragSelectedMode)
      switch (apm) {
      default:
        return;
      case PICKING_ASSIGN_ATOM:
        measuresEnabled = !vwr.getModelkit(false).isPickAtomAssignCharge();
        return;
      case PICKING_DRAW:
        drawMode = true;
        // drawMode and dragSelectedMode are incompatible
        measuresEnabled = false;
        break;
      //other cases here?
      case PICKING_LABEL:
        labelMode = true;
        measuresEnabled = false;
        break;
      case PICKING_SELECT_ATOM:
        measuresEnabled = false;
        break;
      case PICKING_MEASURE_DISTANCE:
      case PICKING_MEASURE_SEQUENCE:
      case PICKING_MEASURE_ANGLE:
      case PICKING_MEASURE_TORSION:
        measuresEnabled = false;
        return;
        //break;
      }
    exitMeasurementMode(null);
  }

  protected void clearMouseInfo() {
    // when a second touch is made, this clears all record of first touch
    pressedCount = clickedCount = 0;
    dragGesture.setAction(0, 0);
    exitMeasurementMode(null);
  }

  private boolean hoverActive = false;

  private MeasurementPending mp;
  private int dragAtomIndex = -1;
  
  public void setDragAtomIndex(int iatom) {
    // from label 
    dragAtomIndex = iatom;
    setAtomsPicked(BSUtil.newAndSetBit(iatom), "Label picked for atomIndex = " + iatom);
  }
  

  private boolean rubberbandSelectionMode = false;
  private final Rectangle rectRubber = new Rectangle();

  private boolean isAltKeyReleased = true;
  private boolean keyProcessing;

  protected boolean isMultiTouchClient;
  protected boolean isMultiTouchServer;

  public boolean isMTClient() {
    return isMultiTouchClient;
  }

  public boolean isMTServer() {
    return isMultiTouchServer;
  }

  public void dispose() {
    clear();
  }

  public void clear() {
    startHoverWatcher(false);
    if (predragBinding != null)
      b = predragBinding;
    vwr.setPickingMode(null, PICKING_IDENTIFY);
    vwr.setPickingStyle(null, rootPickingStyle);
    isAltKeyReleased = true;
  }

  synchronized public void startHoverWatcher(boolean isStart) {
    if (vwr.isPreviewOnly)
      return;
    try {
      if (isStart) {
        if (hoverWatcherThread != null)
          return;
        current.time = -1;
        hoverWatcherThread = new HoverWatcherThread(this, current, moved,
            vwr);
      } else {
        if (hoverWatcherThread == null)
          return;
        current.time = -1;
        hoverWatcherThread.interrupt();
        hoverWatcherThread = null;
      }
    } catch (Exception e) {
      // is possible -- seen once hoverWatcherThread.start() had null pointer.
    }
  }

  /**
   * only NONE (-1) is implemented; it just stops the hoverWatcher thread so
   * that the vwr references are all removed
   * 
   * @param modeMouse
   */
  public void setModeMouse(int modeMouse) {
    if (modeMouse == JC.MOUSE_NONE) {
      startHoverWatcher(false);
    }
  }

  /**
   * called by MouseManager.keyPressed
   * 
   * @param key
   * @param modifiers
   * @return true if handled 
   */
  @Override
  public boolean keyPressed(int key, int modifiers) {
    if (keyProcessing)
      return false;
    vwr.hoverOff();
    keyProcessing = true;
    switch (key) {
    case Event.VK_ALT:
      if (dragSelectedMode && isAltKeyReleased)
        vwr.moveSelected(Integer.MIN_VALUE, 0, Integer.MIN_VALUE,
            Integer.MIN_VALUE, Integer.MIN_VALUE, null, false, false);
      isAltKeyReleased = false;
      moved.modifiers |= Binding.ALT;
      break;
    case Event.VK_SHIFT:
      dragged.modifiers |= Binding.SHIFT;
      moved.modifiers |= Binding.SHIFT;
      break;
    case Event.VK_CONTROL:
      moved.modifiers |= Binding.CTRL;
      break;
    case Event.VK_ESCAPE:
      exitMeasurementMode("escape");
      break;
    }
    int action = Binding.LEFT | Binding.SINGLE | Binding.DRAG | moved.modifiers;
    if (!labelMode && !b.isUserAction(action)) {
      checkMotionRotateZoom(action, current.x, 0, 0, false);
    }
    if (vwr.getBoolean(T.navigationmode)) {
      // if (vwr.getBooleanProperty("showKeyStrokes", false))
      // vwr.evalStringQuiet("!set echo bottom left;echo "
      // + (i == 0 ? "" : i + " " + m));
      switch (key) {
      case Event.VK_UP:
      case Event.VK_DOWN:
      case Event.VK_LEFT:
      case Event.VK_RIGHT:
      case Event.VK_SPACE:
      case Event.VK_PERIOD:
        vwr.navigate(key, modifiers);
        break;
      }
    }
    keyProcessing = false;
    return true;
  }

  @Override
  public void keyReleased(int key) {
    switch (key) {
    case Event.VK_ALT:
      if (dragSelectedMode)
        vwr.moveSelected(Integer.MAX_VALUE, 0, Integer.MIN_VALUE,
            Integer.MIN_VALUE, Integer.MIN_VALUE, null, false, false);
      isAltKeyReleased = true;
      moved.modifiers &= ~Binding.ALT;
      break;
    case Event.VK_SHIFT:
      moved.modifiers &= ~Binding.SHIFT;
      break;
    case Event.VK_CONTROL:
      moved.modifiers &= ~Binding.CTRL;
    }
    if (moved.modifiers == 0)
      vwr.setCursor(GenericPlatform.CURSOR_DEFAULT);
    if (!vwr.getBoolean(T.navigationmode))
      return;
    //if (vwr.getBooleanProperty("showKeyStrokes", false))
    //vwr.evalStringQuiet("!set echo bottom left;echo;");
    switch (key) {
    case Event.VK_UP:
    case Event.VK_DOWN:
    case Event.VK_LEFT:
    case Event.VK_RIGHT:
      vwr.navigate(0, 0);
      break;
    }
  }

  @Override
  public void mouseEnterExit(long time, int x, int y, boolean isExit) {
    if (vwr.tm.stereoDoubleDTI)
      x = x << 1;
    setCurrent(time, x, y, 0);
    if (isExit)
      exitMeasurementMode("mouseExit"); //otherwise pending measurement can be left over.
  }

  private int pressAction;
  private int dragAction;
  private int clickAction;

  private void setMouseActions(int count, int buttonMods, boolean isRelease) {
    pressAction = Binding.getMouseAction(count, buttonMods,
        isRelease ? Event.RELEASED : Event.PRESSED);
    dragAction = Binding.getMouseAction(count, buttonMods, Event.DRAGGED);
    clickAction = Binding.getMouseAction(count, buttonMods, Event.CLICKED);
  }

  /**
   * 
   * @param mode
   *        MOVED PRESSED DRAGGED RELEASED CLICKED WHEELED
   * @param time
   * @param x
   * @param y
   * @param count
   * @param buttonMods
   *        LEFT RIGHT MIDDLE WHEEL SHIFT ALT CTRL
   */
  @Override
  public void mouseAction(int mode, long time, int x, int y, int count,
                          int buttonMods) {
    if (!vwr.getMouseEnabled())
      return;
    if (Logger.debuggingHigh && mode != Event.MOVED && vwr.getBoolean(T.testflag1))
      vwr.showString("mouse action: " + mode + " " + buttonMods + " " + Binding.getMouseActionName(Binding.getMouseAction(count, buttonMods, mode), false), false);
    if (vwr.tm.stereoDoubleDTI)
      x = x << 1;
    switch (mode) {
    case Event.MOVED:
      setCurrent(time, x, y, buttonMods);
      moved.setCurrent(current, 0);
      if (mp != null || hoverActive) {
        clickAction = Binding.getMouseAction(clickedCount, buttonMods,
            Event.MOVED);
        checkClickAction(x, y, time, 0);
        return;
      }
      if (isZoomArea(x)) {
        checkMotionRotateZoom(LEFT_DRAGGED, 0, 0, 0, false);
        return;
      }
      if (vwr.currentCursor == GenericPlatform.CURSOR_ZOOM)
        vwr.setCursor(GenericPlatform.CURSOR_DEFAULT);
      return;
    case Event.PRESSED:
      setMouseMode();
      pressedCount = (pressed.check(20, x, y, buttonMods, time,
          MAX_DOUBLE_CLICK_MILLIS) ? pressedCount + 1 : 1);
      if (pressedCount == 1) {
        vwr.checkInMotion(1);
        setCurrent(time, x, y, buttonMods);
      }
      pressAction = Binding.getMouseAction(pressedCount, buttonMods,
          Event.PRESSED);
      vwr.setCursor(GenericPlatform.CURSOR_HAND);
      pressed.setCurrent(current, 1);
      dragged.setCurrent(current, 1);
      vwr.setFocus();
      dragGesture.setAction(dragAction, time);
      checkPressedAction(x, y, time);
      return;
    case Event.DRAGGED:
      setMouseMode();
      setMouseActions(pressedCount, buttonMods, false);
      int deltaX = x - dragged.x;
      int deltaY = y - dragged.y;
      setCurrent(time, x, y, buttonMods);
      dragged.setCurrent(current, -1);
      //if (false && apm != PICKING_ASSIGN_ATOM 
      //    && apm != ACTION_pickMeasure
      //    && apm != PICKING_MEASURE_DISTANCE)
      dragGesture.add(dragAction, x, y, time);
      checkDragWheelAction(dragAction, x, y, deltaX, deltaY, time,
          Event.DRAGGED);
      return;
    case Event.RELEASED:
      setMouseActions(pressedCount, buttonMods, true);
      setCurrent(time, x, y, buttonMods);
      vwr.spinXYBy(0, 0, 0);
      boolean dragRelease = !pressed.check(xyRange, x, y, buttonMods, time,
          Long.MAX_VALUE);
      checkReleaseAction(x, y, time, dragRelease);
      return;
    case Event.WHEELED:
      if (vwr.isApplet && !vwr.hasFocus())
        return;
      setCurrent(time, current.x, current.y, buttonMods);
      checkDragWheelAction(Binding.getMouseAction(0, buttonMods,
          Event.WHEELED), current.x, current.y, 0, y, time, Event.WHEELED);
      return;
    case Event.CLICKED:
      setMouseMode();
      // xyRange was 0 BH 2019.04.21
      clickedCount = (count > 1 ? count : clicked.check(xyRange, 0, 0, buttonMods,
          time, MAX_DOUBLE_CLICK_MILLIS) ? clickedCount + 1 : 1);
      if (clickedCount == 1) {
        setCurrent(time, x, y, buttonMods);
      }
      setMouseActions(clickedCount, buttonMods, false);
      clicked.setCurrent(current, clickedCount);
      vwr.setFocus();
      if (apm != PICKING_SELECT_ATOM
          && bnd(Binding.getMouseAction(1, buttonMods, Event.PRESSED),
              ACTION_selectAndDrag))
        return;
      clickAction = Binding.getMouseAction(clickedCount, buttonMods,
          Event.CLICKED);
      checkClickAction(x, y, time, clickedCount);
      return;
    }
  }

  private void checkPressedAction(int x, int y, long time) {
    int buttonMods = Binding.getButtonMods(pressAction);
    boolean isDragSelectedAction = bnd(
        Binding.getMouseAction(1, buttonMods, Event.PRESSED),
        ACTION_selectAndDrag);
    if (buttonMods != 0) {
      pressAction = vwr.notifyMouseClicked(x, y, pressAction, Event.PRESSED);
      if (pressAction == 0)
        return;
      buttonMods = Binding.getButtonMods(pressAction);
    }
    setMouseActions(pressedCount, buttonMods, false);
    if (Logger.debuggingHigh && vwr.getBoolean(T.testflag1))
      Logger.debug(Binding.getMouseActionName(pressAction, false));

    if (isDrawOrLabelAction(dragAction) && vwr.checkObjectDragged(Integer.MIN_VALUE, 0, x, y, dragAction))
      return;
    checkUserAction(pressAction, x, y, 0, 0, time, Event.PRESSED);
    boolean isBound = false;
    switch (apm) {
    case PICKING_ASSIGN_ATOM:
      isBound = bnd(clickAction, ACTION_assignNew);
      break;
    case PICKING_DRAG_ATOM:
      isBound = bnd(dragAction, ACTION_dragAtom, ACTION_dragZ);
      break;
    case PICKING_DRAG_SELECTED:
    case PICKING_DRAG_LIGAND:
    case PICKING_DRAG_MOLECULE:
      isBound = bnd(dragAction, ACTION_dragAtom, ACTION_dragZ, ACTION_rotateSelected);
      break;
    case PICKING_DRAG_MINIMIZE:
      isBound = bnd(dragAction, ACTION_dragMinimize, ACTION_dragZ);
      break;
    case PICKING_DRAG_MINIMIZE_MOLECULE:
      isBound = bnd(dragAction, ACTION_dragMinimize, ACTION_dragZ, ACTION_rotateSelected);
      break;
    }
    if (isBound) {
      dragAtomIndex = vwr.findNearestAtomIndexMovable(x, y, true);
      if (dragAtomIndex >= 0
          && (apm == PICKING_ASSIGN_ATOM || apm == PICKING_INVERT_STEREO)
          && vwr.ms.isAtomInLastModel(dragAtomIndex)) {
        enterMeasurementMode(dragAtomIndex);
        mp.addPoint(dragAtomIndex, null, false);
      }
      return;
    }
    if (bnd(pressAction, ACTION_popupMenu)) {
      char type = 'j';
      if (vwr.getBoolean(T.modelkitmode)) {
        Map<String, Object> t = vwr.checkObjectClicked(x, y, LEFT_CLICKED);
        type = (t != null && "bond".equals(t.get("type")) ? 'b' : vwr
            .findNearestAtomIndex(x, y) >= 0 ? 'a' : 'm');
      }
      vwr.popupMenu(x, y, type);
      return;
    }
    if (dragSelectedMode) {
      haveSelection = (!isDragSelectedAction || vwr
          .findNearestAtomIndexMovable(x, y, true) >= 0);
      if (haveSelection && bnd(dragAction, ACTION_dragSelected, ACTION_dragZ))
        vwr.moveSelected(Integer.MIN_VALUE, 0, Integer.MIN_VALUE,
            Integer.MIN_VALUE, Integer.MIN_VALUE, null, false, false);
      return;
    }
    //   if (vwr.g.useArcBall)
    //      vwr.rotateArcBall(x, y, 0);
    checkMotionRotateZoom(dragAction, x, 0, 0, true);
  }

  private void checkDragWheelAction(int dragWheelAction, int x, int y,
                                    int deltaX, int deltaY, long time, int mode) {
    int buttonmods = Binding.getButtonMods(dragWheelAction);
    if (buttonmods != 0) {
      int newAction = vwr.notifyMouseClicked(x, y,
          Binding.getMouseAction(pressedCount, buttonmods, mode), mode); // why was this "-pressedCount"? passing to user?
      if (newAction == 0)
        return;
      if (newAction > 0)
        dragWheelAction = newAction;
    }

    if (isRubberBandSelect(dragWheelAction)) {
      calcRectRubberBand();
      vwr.refresh(Viewer.REFRESH_SYNC_MASK, "rubberBand selection");
      return;
    }

    if (checkUserAction(dragWheelAction, x, y, deltaX, deltaY, time, mode))
      return;

    if (vwr.g.modelKitMode && vwr.getModelkit(false).getRotateBondIndex() >= 0) {
      if (bnd(dragWheelAction, ACTION_rotateBranch)) {
        vwr.moveSelected(deltaX, deltaY, Integer.MIN_VALUE, x, y, null, false,
            false);
        return;
      }
      if (!bnd(dragWheelAction, ACTION_rotate))
        vwr.setRotateBondIndex(-1);
    }
    
    BS bs = null;
    if (dragAtomIndex >= 0 && apm != PICKING_LABEL) {
      
      switch (apm) {
      case PICKING_DRAG_SELECTED:
        dragSelected(dragWheelAction, deltaX, deltaY, true);
        return;
      case PICKING_DRAG_LIGAND:
      case PICKING_DRAG_MOLECULE:
      case PICKING_DRAG_MINIMIZE_MOLECULE:
        bs = vwr.ms.getAtoms(T.molecule, BSUtil.newAndSetBit(dragAtomIndex));
        if (apm == PICKING_DRAG_LIGAND)
          bs.and(vwr.getAtomBitSet("ligand"));
        //$FALL-THROUGH$
      case PICKING_DRAG_ATOM:
      case PICKING_DRAG_MINIMIZE:
        if (dragGesture.getPointCount() == 1)
          vwr.undoMoveActionClear(dragAtomIndex, AtomCollection.TAINT_COORD,
              true);
        setMotion(GenericPlatform.CURSOR_MOVE, true);
        if (bnd(dragWheelAction, ACTION_rotateSelected)) {
          vwr.rotateSelected(getDegrees(deltaX, true),
              getDegrees(deltaY, false), bs);
        } else {
          switch (apm) {
          case PICKING_DRAG_LIGAND:
          case PICKING_DRAG_MOLECULE:
          case PICKING_DRAG_MINIMIZE_MOLECULE:
            vwr.select(bs, false, 0, true);
            break;
          }
          vwr.moveAtomWithHydrogens(
              dragAtomIndex,
              deltaX,
              deltaY,
              (bnd(dragWheelAction, ACTION_dragZ) ? -deltaY : Integer.MIN_VALUE),
              bs);
        }
        // NAH! if (atomPickingMode == PICKING_DRAG_MINIMIZE_MOLECULE && (dragGesture.getPointCount() % 5 == 0))
        //  minimize(false);
        return;
      }
    }

    if (dragAtomIndex >= 0 && mode == Event.DRAGGED
        && bnd(clickAction, ACTION_assignNew) && apm == PICKING_ASSIGN_ATOM) {
      int nearestAtomIndex = vwr.findNearestAtomIndexMovable(x, y, false);
      if (nearestAtomIndex >= 0) {
        if (mp != null) {
          mp.setCount(1);
        } else if (measuresEnabled) {
          enterMeasurementMode(nearestAtomIndex);
        }
        addToMeasurement(nearestAtomIndex, null, true);
        mp.colix = C.MAGENTA;
      } else if (mp != null) {
        mp.setCount(1);
        mp.colix = C.GOLD;
      }
      if (mp == null)
        return;
      if (vwr.antialiased) {
        x <<= 1;
        y <<= 1;
      }

      mp.traceX = x;
      mp.traceY = y;
      vwr.refresh(Viewer.REFRESH_SYNC_MASK, "assignNew");
      return;
    }

    if (!drawMode && !labelMode && bnd(dragWheelAction, ACTION_translate)) {
      vwr.translateXYBy(deltaX, deltaY);
      return;
    }
    if (dragSelectedMode && haveSelection
        && bnd(dragWheelAction, ACTION_dragSelected, ACTION_rotateSelected)) {
      // we will drag atoms and either rotate or translate them
      // possibly just the atoms or possibly their molecule (decided in Viewer)
      int iatom = vwr.bsA().nextSetBit(0);
      if (iatom < 0)
        return;
      if (dragGesture.getPointCount() == 1)
        vwr.undoMoveActionClear(iatom, AtomCollection.TAINT_COORD, true);
      else
        vwr.moveSelected(Integer.MAX_VALUE, 0, Integer.MIN_VALUE,
            Integer.MIN_VALUE, Integer.MIN_VALUE, null, false, false);
      dragSelected(dragWheelAction, deltaX, deltaY, false);
      return;
    }

    if (isDrawOrLabelAction(dragWheelAction)) {
      setMotion(GenericPlatform.CURSOR_MOVE, true);
      if (vwr.checkObjectDragged(dragged.x, dragged.y, x, y, dragWheelAction)) {
        return;
      }
    }
    if (checkMotionRotateZoom(dragWheelAction, x, deltaX, deltaY, true)) {
      if (vwr.tm.slabEnabled && bnd(dragWheelAction,ACTION_slabAndDepth))
        vwr.slabDepthByPixels(deltaY);
      else
        vwr.zoomBy(deltaY);
      return;
    }
    if (bnd(dragWheelAction, ACTION_rotate)) {
      //      if (vwr.g.useArcBall)
      //        vwr.rotateArcBall(x, y, mouseDragFactor);
      //      else
      vwr.rotateXYBy(getDegrees(deltaX, true), getDegrees(deltaY, false));
      return;
    }
    if (bnd(dragWheelAction, ACTION_rotateZorZoom)) {
      if (deltaX == 0 && Math.abs(deltaY) > 1) {
        // if (deltaY < 0 && deltaX > deltaY || deltaY > 0 && deltaX < deltaY)
        setMotion(GenericPlatform.CURSOR_ZOOM, true);
        vwr.zoomBy(deltaY + (deltaY > 0 ? -1 : 1));
      } else if (deltaY == 0 && Math.abs(deltaX) > 1) {
        // if (deltaX < 0 && deltaY > deltaX || deltaX > 0 && deltaY < deltaX)
        setMotion(GenericPlatform.CURSOR_MOVE, true);
        vwr.rotateZBy(-deltaX + (deltaX > 0 ? 1 : -1), Integer.MAX_VALUE,
            Integer.MAX_VALUE);
      }
      return;
    } 
    if (vwr.tm.slabEnabled) {
      if (bnd(dragWheelAction, ACTION_depth)) {
        vwr.depthByPixels(deltaY);
        return;
      }
      if (bnd(dragWheelAction, ACTION_slab)) {
        vwr.slabByPixels(deltaY);
        return;
      }
      if (bnd(dragWheelAction, ACTION_slabAndDepth)) {
        vwr.slabDepthByPixels(deltaY);
        return;
      }
    }
    if (bnd(dragWheelAction, ACTION_wheelZoom)) {
      zoomByFactor(deltaY, Integer.MAX_VALUE, Integer.MAX_VALUE);
      return;
    } 
    if (bnd(dragWheelAction, ACTION_rotateZ)) {
      setMotion(GenericPlatform.CURSOR_MOVE, true);
      vwr.rotateZBy(-deltaX, Integer.MAX_VALUE, Integer.MAX_VALUE);
      return;
    }
  }

  /**
   * change actual coordinates of selected atoms from set dragSeleted TRUE or
   * set PICKING DRAGSELECTED
   * 
   * Basically, set dragSelected adds new functionality to Jmol with alt-drag
   * and alt-shift drag, and set picking dragSelected replaces the standard
   * mouse drag with a move action and also adds rotate and z-shift options.
   * 
   * set dragSelected also allows other picking types, such as set picking SELECT,
   * which uses double-click to start rotating/moving another molecule. 
   * 
   * @param a
   * @param deltaX
   * @param deltaY
   * @param isPickingDrag
   */
  private void dragSelected(int a, int deltaX, int deltaY, boolean isPickingDrag) {

    // see footnotes below for ^, $, #, and *
    //
    // settings:^    set picking dragSelected             set dragSelected 
    //
    // move:#                 drag                          alt-shift-drag
    // rotate:#*          alt-drag                                alt-drag
    // z-shift:#        shift-drag                                  (n/a)
    // 
    // double-click:$  (starts measurement)       (sets selected if set picking SELECT)
    //
    // # all actions involve whole molecules unless   set allowMoveAtoms TRUE
    // ^ set picking dragSelected overrules set dragSelected
    // * rotate requires   set allowRotateSelected TRUE
    // $ set dragSelected allows setting of a new molecule with double-click when    set picking SELECT
    // $ set picking dragSelected allows measurements with double-click, as usual

    setMotion(GenericPlatform.CURSOR_MOVE, true);
    if (bnd(a, ACTION_rotateSelected) && vwr.getBoolean(T.allowrotateselected))
      vwr.rotateSelected(getDegrees(deltaX, true), getDegrees(deltaY, false),
          null);
    else
      vwr.moveSelected(
          deltaX,
          deltaY,
          (isPickingDrag && bnd(a, ACTION_dragZ) ? -deltaY : Integer.MIN_VALUE),
          Integer.MIN_VALUE, Integer.MIN_VALUE, null, true, false);
  }


  private void checkReleaseAction(int x, int y, long time, boolean dragRelease) {
    if (Logger.debuggingHigh && vwr.getBoolean(T.testflag1))
      Logger.debug(Binding.getMouseActionName(pressAction, false));
    vwr.checkInMotion(0);
    vwr.setInMotion(false);
    vwr.setCursor(GenericPlatform.CURSOR_DEFAULT);
    dragGesture.add(dragAction, x, y, time);
    if (dragRelease)
      vwr.setRotateBondIndex(Integer.MIN_VALUE);
    if (dragAtomIndex >= 0) {
      if (apm == PICKING_DRAG_MINIMIZE
          || apm == PICKING_DRAG_MINIMIZE_MOLECULE)
        minimize(true);
    }
    if (apm == PICKING_ASSIGN_ATOM
        && bnd(clickAction, ACTION_assignNew)) {
      if (mp == null || dragAtomIndex < 0)
        return;
      assignNew(x, y);
      return;
    }
    dragAtomIndex = -1;
    boolean isRbAction = isRubberBandSelect(dragAction);
    if (isRbAction)
      selectRb(clickAction);
    rubberbandSelectionMode = (b.name.equals("drag"));
    rectRubber.x = Integer.MAX_VALUE;
    if (dragRelease) {
      vwr.notifyMouseClicked(x, y, Binding.getMouseAction(pressedCount, 0,
          Event.RELEASED), Event.RELEASED);
    }
    if (isDrawOrLabelAction(dragAction)) {
      vwr.checkObjectDragged(Integer.MAX_VALUE, 0, x, y, dragAction);
      return;
    }
    if (haveSelection && dragSelectedMode && bnd(dragAction, ACTION_dragSelected))
      vwr.moveSelected(Integer.MAX_VALUE, 0, Integer.MIN_VALUE,
          Integer.MIN_VALUE, Integer.MIN_VALUE, null, false, false);

    if (dragRelease
        && checkUserAction(pressAction, x, y, 0, 0, time, Event.RELEASED))
      return;

    if (vwr.getBoolean(T.allowgestures)) {
      if (bnd(dragAction, ACTION_swipe)) {
        float speed = getExitRate();
        if (speed > 0)
          vwr.spinXYBy(dragGesture.getDX(4, 2), dragGesture.getDY(4, 2),
              speed * 30 * gestureSwipeFactor);
        if (vwr.g.logGestures)
          vwr.log("$NOW$ swipe " + dragGesture + " " + speed);
        return;
      }

    }
  }

  private void checkClickAction(int x, int y, long time, int clickedCount) {
    // points are always picked up first, then atoms
    // so that atom picking can be superceded by draw picking
    // Binding.MOVED is used for some vwr methods.
    if (clickedCount > 0) {
      if (checkUserAction(clickAction, x, y, 0, 0, time, Binding.CLICK))
        return;
      clickAction = vwr.notifyMouseClicked(x, y, clickAction, Binding.CLICK);
      if (clickAction == 0)
        return;
    }
    if (Logger.debuggingHigh  && vwr.getBoolean(T.testflag1))
      Logger.debug(Binding.getMouseActionName(clickAction, false));
    if (bnd(clickAction, ACTION_clickFrank)) {
      if (vwr.frankClicked(x, y)) {
        vwr.popupMenu(-x, y, 'j');
        return;
      }
      if (vwr.frankClickedModelKit(x, y)) {
        vwr.popupMenu(10, 0, 'm');
        return;
      }
    }
    Point3fi nearestPoint = null;
    boolean isBond = false;
    boolean isIsosurface = false;
    Map<String, Object> map = null;
    // t.tok will let us know if this is an atom or a bond that was clicked
    if (!drawMode) {
      map = vwr.checkObjectClicked(x, y, clickAction);
      if (map != null) {
        if (labelMode) {
          pickLabel(((Integer)map.get("atomIndex")).intValue());
          return;
        }
        isBond = "bond".equals(map.get("type"));
        isIsosurface = "isosurface".equals(map.get("type"));
        nearestPoint = getPoint(map);
      }
    }
    if (isBond)
      clickedCount = 1;

    if (nearestPoint != null && Float.isNaN(nearestPoint.x))
      return;
    int nearestAtomIndex = findNearestAtom(x, y, nearestPoint, clickedCount > 0);

    if (clickedCount == 0 && apm != PICKING_ASSIGN_ATOM) {
      // mouse move
      if (mp == null)
        return;
      if (nearestPoint != null
          || mp.getIndexOf(nearestAtomIndex) == 0)
        mp.addPoint(nearestAtomIndex, nearestPoint, false);
      if (mp.haveModified)
        vwr.setPendingMeasurement(mp);
      vwr.refresh(Viewer.REFRESH_SYNC_MASK, "measurementPending");
      return;
    }
    setMouseMode();

    if (bnd(clickAction, ACTION_stopMotion)) {
      vwr.tm.stopMotion();
      // continue checking --- no need to exit here
    }

    if (vwr.getBoolean(T.navigationmode)
        && apm == PICKING_NAVIGATE
        && bnd(clickAction, ACTION_pickNavigate)) {
      vwr.navTranslatePercent(x * 100f / vwr.getScreenWidth() - 50f, y
          * 100f / vwr.getScreenHeight() - 50f);
      return;
    }

    // bond change by clicking on a bond
    // bond deletion by clicking a bond
    if (isBond) {
      if (bnd(clickAction, bondPickingMode == PICKING_ROTATE_BOND
          || bondPickingMode == PICKING_ASSIGN_BOND ? ACTION_assignNew
          : ACTION_deleteBond)) {
        bondPicked(((Integer) map.get("index")).intValue());
        return;
      }
    } else if (isIsosurface) {
      return;
    } else {
      if (apm != PICKING_ASSIGN_ATOM && mp != null
          && bnd(clickAction, ACTION_pickMeasure)) {
        atomOrPointPicked(nearestAtomIndex, nearestPoint);
        if (addToMeasurement(nearestAtomIndex, nearestPoint, false) == 4)
          toggleMeasurement();
        return;
      }

      if (bnd(clickAction, ACTION_setMeasure)) {
        if (mp != null) {
          addToMeasurement(nearestAtomIndex, nearestPoint, true);
          toggleMeasurement();
        } else if (!drawMode && !labelMode && !dragSelectedMode
            && measuresEnabled) {
          enterMeasurementMode(nearestAtomIndex);
          addToMeasurement(nearestAtomIndex, nearestPoint, true);
        }
        atomOrPointPicked(nearestAtomIndex, nearestPoint);
        return;
      }
    }
    if (isSelectAction(clickAction)) {
      // TODO: in drawMode the binding changes
      if (!isIsosurface)
        atomOrPointPicked(nearestAtomIndex, nearestPoint);
      return;
    }
    if (bnd(clickAction, ACTION_reset)) {
      if (nearestAtomIndex < 0)
        reset();
      return;
    }
  }

  private void pickLabel(int iatom) {
    String label = vwr.ms.at[iatom].atomPropertyString(vwr,  T.label);
    if (pressedCount == 2) {
      label = vwr.apiPlatform.prompt("Set label for atomIndex=" + iatom, label, null, false);
      if (label != null) {
        vwr.shm.setAtomLabel(label, iatom);
        vwr.refresh(Viewer.REFRESH_REPAINT, "label atom");
      }
    } else {
      setAtomsPicked(BSUtil.newAndSetBit(iatom), "Label picked for atomIndex = " + iatom + ": " + label);
    }
  }

  private boolean checkUserAction(int mouseAction, int x, int y, int deltaX,
                                  int deltaY, long time, int mode) {
    if (!b.isUserAction(mouseAction))
      return false;
    boolean passThrough = false;
    Object obj;
    Map<String, Object> ht = b.getBindings();
    String mkey = mouseAction + "\t";
    for (String key : ht.keySet()) {
      if (key.indexOf(mkey) != 0 || !AU.isAS(obj = ht.get(key)))
        continue;
      String script = ((String[]) obj)[1];
      P3 nearestPoint = null;
      if (script.indexOf("_ATOM") >= 0) {
        int iatom = findNearestAtom(x, y, null, true);
        script = PT.rep(script, "_ATOM", "({"
            + (iatom >= 0 ? "" + iatom : "") + "})");
        if (iatom >= 0)
          script = PT.rep(script, "_POINT", Escape.eP(vwr
              .ms.at[iatom]));
      }
      if (!drawMode
          && (script.indexOf("_POINT") >= 0 || script.indexOf("_OBJECT") >= 0 || script
              .indexOf("_BOND") >= 0)) {
        Map<String, Object> t = vwr.checkObjectClicked(x, y, mouseAction);
        if (t != null && (nearestPoint = (P3) t.get("pt")) != null) {
          boolean isBond = t.get("type").equals("bond");
          if (isBond)
            script = PT.rep(script, "_BOND", "[{"
                + t.get("index") + "}]");
          script = PT.rep(script, "_POINT", Escape
              .eP(nearestPoint));
          script = PT.rep(script, "_OBJECT", Escape
              .escapeMap(t));
        }
        script = PT.rep(script, "_BOND", "[{}]");
        script = PT.rep(script, "_OBJECT", "{}");
      }
      script = PT.rep(script, "_POINT", "{}");
      script = PT.rep(script, "_ACTION", "" + mouseAction);
      script = PT.rep(script, "_X", "" + x);
      script = PT.rep(script, "_Y", ""
          + (vwr.getScreenHeight() - y));
      script = PT.rep(script, "_DELTAX", "" + deltaX);
      script = PT.rep(script, "_DELTAY", "" + deltaY);
      script = PT.rep(script, "_TIME", "" + time);
      script = PT.rep(script, "_MODE", "" + mode);
      if (script.startsWith("+:")) {
        passThrough = true;
        script = script.substring(2);
      }
      vwr.evalStringQuiet(script);
    }
    return !passThrough;
  }

  /**
   * 
   * @param mouseAction
   * @param x
   * @param deltaX
   * @param deltaY
   * @param isDrag
   * @return TRUE if motion was a zoom
   */
  private boolean checkMotionRotateZoom(int mouseAction, int x, int deltaX,
                                        int deltaY, boolean isDrag) {
    boolean isSlideZoom = bnd(mouseAction, ACTION_slideZoom) && isZoomArea(pressed.x);
    boolean isRotateXY = bnd(mouseAction, ACTION_rotate);
    boolean isRotateZorZoom = bnd(mouseAction, ACTION_rotateZorZoom);
    if (!isSlideZoom && !isRotateXY && !isRotateZorZoom)
      return false;
    boolean isZoom = (isRotateZorZoom && (deltaX == 0 || Math.abs(deltaY) > 5 * Math
        .abs(deltaX)));
    int cursor = (isZoom || isZoomArea(moved.x)
        || bnd(mouseAction, ACTION_wheelZoom) ? GenericPlatform.CURSOR_ZOOM
        : isRotateXY || isRotateZorZoom ? GenericPlatform.CURSOR_MOVE : bnd(
            mouseAction, ACTION_center) ? GenericPlatform.CURSOR_HAND
            : GenericPlatform.CURSOR_DEFAULT);
    setMotion(cursor, isDrag);
    return (isZoom || isSlideZoom);
  }

  private float getExitRate() {
    long dt = dragGesture.getTimeDifference(2);
    return (isMultiTouch ? (dt > (MININUM_GESTURE_DELAY_MILLISECONDS << 3) ? 0 :
      dragGesture.getSpeedPixelsPerMillisecond(2, 1)) 
      : (dt > MININUM_GESTURE_DELAY_MILLISECONDS ? 0 : dragGesture
        .getSpeedPixelsPerMillisecond(4, 2)));
  }

  private boolean isRubberBandSelect(int action) {
    // drag and wheel and release
    action = action & ~Binding.DRAG | Binding.CLICK;
    return (rubberbandSelectionMode
        && bnd(action, ACTION_selectToggle, ACTION_selectOr, ACTION_selectAndNot));
  }

  Rectangle getRubberBand() {
    return (rubberbandSelectionMode && rectRubber.x != Integer.MAX_VALUE ? rectRubber
        : null);
  }

  private void calcRectRubberBand() {
    int factor = (vwr.antialiased ? 2 : 1);
    if (current.x < pressed.x) {
      rectRubber.x = current.x * factor;
      rectRubber.width = (pressed.x - current.x) * factor;
    } else {
      rectRubber.x = pressed.x * factor;
      rectRubber.width = (current.x - pressed.x) * factor;
    }
    if (current.y < pressed.y) {
      rectRubber.y = current.y * factor;
      rectRubber.height = (pressed.y - current.y) * factor;
    } else {
      rectRubber.y = pressed.y * factor;
      rectRubber.height = (current.y - pressed.y) * factor;
    }
  }

  /**
   * Transform a screen pixel change to an angular change
   * such that a full sweep of the dimension (up to 500 pixels)
   * corresponds to 180 degrees of rotation.
   *  
   * @param delta
   * @param isX
   * @return desired scaled rotation, in degrees
   */
  protected float getDegrees(float delta, boolean isX) {
    return delta / Math.min(500, isX ? vwr.getScreenWidth() 
        : vwr.getScreenHeight()) * 180 * mouseDragFactor;
  }

  private boolean isZoomArea(int x) {
    return x > vwr.getScreenWidth() * (vwr.tm.stereoDoubleFull || vwr.tm.stereoDoubleDTI ? 2 : 1)
        * SLIDE_ZOOM_X_PERCENT / 100f;
  }

  private Point3fi getPoint(Map<String, Object> t) {
    Point3fi pt = new Point3fi();
    pt.setT((P3) t.get("pt"));
    pt.mi = (short) ((Integer) t.get("modelIndex")).intValue();
    return pt;
  }

  private int findNearestAtom(int x, int y, Point3fi nearestPoint,
                              boolean isClicked) {
    int index = (drawMode || nearestPoint != null ? -1 : vwr
        .findNearestAtomIndexMovable(x, y, false));
    return (index >= 0 && (isClicked || mp == null)
        && !vwr.slm.isInSelectionSubset(index) ? -1 : index);
  }

  private boolean isSelectAction(int action) {
    return (bnd(action, ACTION_pickAtom)
        || !drawMode
        && !labelMode
        && apm == PICKING_IDENTIFY
        && bnd(action, ACTION_center)
        || dragSelectedMode
        && bnd(dragAction, ACTION_rotateSelected, ACTION_dragSelected) 
        || bnd(action, ACTION_pickPoint, ACTION_selectToggle, ACTION_selectAndNot,
            ACTION_selectOr, ACTION_selectToggleExtended, ACTION_select));
  }

  //////////// specific actions ////////////////

  private MeasurementPending measurementQueued;
  public boolean zoomTrigger;

  private void enterMeasurementMode(int iAtom) {
    vwr.setPicked(iAtom, true);
    vwr.setCursor(GenericPlatform.CURSOR_CROSSHAIR);
    vwr.setPendingMeasurement(mp = getMP());
    measurementQueued = mp;
  }

  private MeasurementPending getMP() {
    return ((MeasurementPending) Interface
        .getInterface("org.jmol.modelset.MeasurementPending", vwr, "mouse")).set(vwr.ms);
  }

  private int addToMeasurement(int atomIndex, Point3fi nearestPoint,
                               boolean dblClick) {
    if (atomIndex == -1 && nearestPoint == null || mp == null) {
      exitMeasurementMode(null);
      return 0;
    }
    int measurementCount = mp.count;
    if (mp.traceX != Integer.MIN_VALUE && measurementCount == 2)
      mp.setCount(measurementCount = 1);
    return (measurementCount == 4 && !dblClick ? measurementCount
        : mp.addPoint(atomIndex, nearestPoint, true));
  }

  private void resetMeasurement() {
    // doesn't reset the measurement that is being picked using
    // double-click, just the one using set picking measure.
    exitMeasurementMode(null);
    measurementQueued = getMP();
  }

  void exitMeasurementMode(String refreshWhy) {
    if (mp == null)
      return;
    vwr.setPendingMeasurement(mp = null);
    vwr.setCursor(GenericPlatform.CURSOR_DEFAULT);
    if (refreshWhy != null)
      vwr.refresh(Viewer.REFRESH_SYNC_MASK, refreshWhy);
  }

  private void getSequence() {
    int a1 = measurementQueued.getAtomIndex(1);
    int a2 = measurementQueued.getAtomIndex(2);
    if (a1 < 0 || a2 < 0)
      return;
    try {
      String sequence = vwr.getSmilesOpt(null, a1, a2, JC.SMILES_GEN_BIO, null);
      vwr.setStatusMeasuring("measureSequence", -2, sequence, 0);
    } catch (Exception e) {
      Logger.error(e.toString());
    }
  }

  private void minimize(boolean dragDone) {
    int iAtom = dragAtomIndex;
    if (dragDone)
      dragAtomIndex = -1;
    vwr.dragMinimizeAtom(iAtom);
  }

  private int queueAtom(int atomIndex, Point3fi ptClicked) {
    int n = measurementQueued.addPoint(atomIndex, ptClicked, true);
    if (atomIndex >= 0)
      vwr.setStatusAtomPicked(atomIndex, "Atom #" + n + ":"
          + vwr.getAtomInfo(atomIndex), null, false);
    return n;
  }

  protected void setMotion(int cursor, boolean inMotion) {
    switch (vwr.currentCursor) {
    case GenericPlatform.CURSOR_WAIT:
      break;
    default:
      vwr.setCursor(cursor);
    }
    if (inMotion)
      vwr.setInMotion(true);
  }

  protected void zoomByFactor(int dz, int x, int y) {
    if (dz == 0)
      return;
    setMotion(GenericPlatform.CURSOR_ZOOM, true);
    vwr.zoomByFactor((float) Math.pow(mouseWheelFactor, dz), x, y);
    moved.setCurrent(current, 0);
    vwr.setInMotion(true);
    zoomTrigger = true;
    startHoverWatcher(true);
  }

  
  /// methods that utilize vwr.script

  private void runScript(String script) {
    vwr.script(script);
  }

  private void atomOrPointPicked(int atomIndex, Point3fi ptClicked) {
    // atomIndex < 0 is off structure.
    // if picking spin or picking symmetry is on, then 
    // we need to enter this method to process those events.
    if (atomIndex < 0) {
      resetMeasurement(); // for set picking measure only
      if (bnd(clickAction, ACTION_selectNone)) {
        runScript("select none");
        return;
      }
      if (apm != PICKING_SPIN
          && apm != PICKING_SYMMETRY)
        return;
    }
    int n = 2;
    switch (apm) {
    case PICKING_DRAG_ATOM:
      // this is done in mouse drag, not mouse release
    case PICKING_DRAG_MINIMIZE:
      return;
    case PICKING_OFF:
      return;
    case PICKING_STRUTS:
    case PICKING_CONNECT:
    case PICKING_DELETE_BOND:
      boolean isDelete = (apm == PICKING_DELETE_BOND);
      boolean isStruts = (apm == PICKING_STRUTS);
      if (!bnd(clickAction, (isDelete ? ACTION_deleteBond
          : ACTION_connectAtoms)))
        return;
      if (measurementQueued == null || measurementQueued.count == 0
          || measurementQueued.count > 2) {
        resetMeasurement();
        enterMeasurementMode(atomIndex);
      }
      addToMeasurement(atomIndex, ptClicked, true);
      if (queueAtom(atomIndex, ptClicked) != 2)
        return;
      String cAction = (isDelete
          || measurementQueued.isConnected(vwr.ms.at, 2) ? " DELETE"
          : isStruts ? "STRUTS" : "");
      runScript("connect " + measurementQueued.getMeasurementScript(" ", true)
          + cAction);
      resetMeasurement();
      return;
    case PICKING_MEASURE_TORSION:
      n++;
      //$FALL-THROUGH$
    case PICKING_MEASURE_ANGLE:
      n++;
      //$FALL-THROUGH$
    case PICKING_MEASURE:
    case PICKING_MEASURE_DISTANCE:
    case PICKING_MEASURE_SEQUENCE:
      if (!bnd(clickAction, ACTION_pickMeasure))
        return;
      if (measurementQueued == null || measurementQueued.count == 0
          || measurementQueued.count > n) {
        resetMeasurement();
        enterMeasurementMode(atomIndex);
      }
      addToMeasurement(atomIndex, ptClicked, true);
      queueAtom(atomIndex, ptClicked);
      int i = measurementQueued.count;
      if (i == 1)
        vwr.setPicked(atomIndex, true);
      if (i < n)
        return;
      if (apm == PICKING_MEASURE_SEQUENCE) {
        getSequence();
      } else {
        vwr.setStatusMeasuring("measurePicked", n, measurementQueued
            .getStringDetail(), measurementQueued.value);
        if (apm == PICKING_MEASURE
            || pickingStyleMeasure == PICKINGSTYLE_MEASURE_ON) {
          runScript("measure "
              + measurementQueued.getMeasurementScript(" ", true));
        }
      }
      resetMeasurement();
      return;
    }
    int mode = (mp != null
        && apm != PICKING_IDENTIFY ? PICKING_IDENTIFY
        : apm);
    switch (mode) {
    case PICKING_CENTER:
      if (!bnd(clickAction, ACTION_pickAtom))
        return;
      if (ptClicked == null) {
        zoomTo(atomIndex);
      } else {
        runScript("zoomTo " + Escape.eP(ptClicked));
      }
      return;
    case PICKING_SPIN:
    case PICKING_SYMMETRY:
      if (bnd(clickAction, ACTION_pickAtom))
        checkTwoAtomAction(ptClicked, atomIndex);
    }
    if (ptClicked != null)
      return;
    // atoms only here:
    BS bs;
    switch (mode) {
    case PICKING_IDENTIFY:
      if (!drawMode && !labelMode && bnd(clickAction, ACTION_center))
        zoomTo(atomIndex);
      else if (bnd(clickAction, ACTION_pickAtom))
        vwr.setStatusAtomPicked(atomIndex, null, null, false);
      return;
    case PICKING_LABEL:
      if (bnd(clickAction, ACTION_pickLabel)) {
        runScript("set labeltoggle {atomindex=" + atomIndex + "}");
        pickLabel(atomIndex);
      }
      return;
    case PICKING_INVERT_STEREO:
      if (bnd(clickAction, ACTION_assignNew)) {
        vwr.invertRingAt(atomIndex, true);
        vwr.setStatusAtomPicked(atomIndex, "invert stereo for atomIndex=" + atomIndex, null, false);
      }
      return;
    case PICKING_DELETE_ATOM:
      if (bnd(clickAction, ACTION_deleteAtom)) {
        bs = BSUtil.newAndSetBit(atomIndex);
        vwr.deleteAtoms(bs, false);
        vwr.setStatusAtomPicked(atomIndex, "deleted: " + Escape.eBS(bs), null, false);
      }
      return;
    }
    // set picking select options:
    String spec = "atomindex=" + atomIndex;
    switch (apm) {
    default:
      return;
    case PICKING_SELECT_ATOM:
      selectAtoms(spec);
      break;
    case PICKING_SELECT_GROUP:
      selectAtoms("within(group, " + spec + ")");
      break;
    case PICKING_SELECT_CHAIN:
      selectAtoms("within(chain, " + spec + ")");
      break;
    case PICKING_SELECT_POLYMER:
      selectAtoms("within(polymer, " + spec + ")");
      break;
    case PICKING_SELECT_STRUCTURE:
      selectAtoms("within(structure, " + spec + ")");
      break;
    case PICKING_SELECT_MOLECULE:
      selectAtoms("within(molecule, " + spec + ")");
      break;
    case PICKING_SELECT_MODEL:
      selectAtoms("within(model, " + spec + ")");
      break;
    // only the next two use VISIBLE (as per the documentation)
    case PICKING_SELECT_ELEMENT:
      selectAtoms("visible and within(element, " + spec + ")");
      break;
    case PICKING_SELECT_SITE:
      selectAtoms("visible and within(site, " + spec + ")");
      break;
    }
    vwr.clearClickCount();
    vwr.setStatusAtomPicked(atomIndex, null, null, false);
  }

  private void assignNew(int x, int y) {
    
    if (vwr.antialiased) {
      x <<= 1;
      y <<= 1;
    }

    // H C + -, etc.
    // also check valence and add/remove H atoms as necessary?
    boolean inRange = pressed.inRange(xyRange, dragged.x, dragged.y);
    if (inRange) {
      dragged.x = pressed.x;
      dragged.y = pressed.y;
    }
    if (vwr.getModelkit(false).handleDragAtom(pressed, dragged, mp.countPlusIndices))
      return;
    boolean isCharge = vwr.getModelkit(false).isPickAtomAssignCharge();
    String atomType = (String) vwr.getModelkitProperty("atomType");
    if (mp.count == 2) {
      vwr.undoMoveActionClear(-1, T.save, true);
      runScript("assign connect "
          + mp.getMeasurementScript(" ", false));
    } else if (atomType.equals("Xx")) {
      exitMeasurementMode("bond dropped");
    } else {
      if (inRange) {
        String s = "assign atom ({" + dragAtomIndex + "}) \""
            + atomType + "\"";
        if (isCharge) {
          s += ";{atomindex=" + dragAtomIndex + "}.label='%C'; ";
          vwr.undoMoveActionClear(dragAtomIndex,
              AtomCollection.TAINT_FORMALCHARGE, true);
        } else {
          vwr.undoMoveActionClear(-1, T.save, true);
        }
        runScript(s);
      } else if (!isCharge) {
        vwr.undoMoveActionClear(-1, T.save, true);
        Atom a = vwr.ms.at[dragAtomIndex];
        if (a.getElementNumber() == 1) {
          vwr.assignAtom(dragAtomIndex, "X", null);
//          runScript("assign atom ({" + dragAtomIndex + "}) \"X\"");
        } else {
          P3 ptNew = P3.new3(x, y, a.sZ);
          vwr.tm.unTransformPoint(ptNew, ptNew);
          vwr.assignAtom(dragAtomIndex, atomType, ptNew);
//          runScript("assign atom ({" + dragAtomIndex + "}) \""
//              + atomType + "\" " + Escape.eP(ptNew));
        }
      }
    }
    exitMeasurementMode(null);
  }

  private void bondPicked(int index) {    
    if (bondPickingMode == PICKING_ASSIGN_BOND)
      vwr.undoMoveActionClear(-1, T.save, true);
    
    switch (bondPickingMode) {
    case PICKING_ASSIGN_BOND:
      runScript("assign bond [{" + index + "}] \"" + vwr.getModelkitProperty("bondType")
          + "\"");
      break;
    case PICKING_ROTATE_BOND:
        vwr.setRotateBondIndex(index);
      break;
    case PICKING_DELETE_BOND:
      vwr.deleteBonds(BSUtil.newAndSetBit(index));
    }
  }

  private void checkTwoAtomAction(Point3fi ptClicked, int atomIndex) {
    boolean isSpin = (apm == PICKING_SPIN);
    if (vwr.tm.spinOn || vwr.tm.navOn
        || vwr.getPendingMeasurement() != null) {
      resetMeasurement();
      if (vwr.tm.spinOn)
        runScript("spin off");
      return;
    }
    if (measurementQueued.count >= 2)
      resetMeasurement();
    int queuedAtomCount = measurementQueued.count;
    if (queuedAtomCount == 1) {
      if (ptClicked == null) {
        if (measurementQueued.getAtomIndex(1) == atomIndex)
          return;
      } else {
        if (measurementQueued.getAtom(1).distance(ptClicked) == 0)
          return;
      }
    }
    if (atomIndex >= 0 || ptClicked != null)
      queuedAtomCount = queueAtom(atomIndex, ptClicked);
    if (queuedAtomCount < 2) {
      if (isSpin)
        vwr.scriptStatus(queuedAtomCount == 1 ? GT
            .$("pick one more atom in order to spin the model around an axis")
            : GT.$("pick two atoms in order to spin the model around an axis"));
      else
        vwr
            .scriptStatus(queuedAtomCount == 1 ? GT
                .$("pick one more atom in order to display the symmetry relationship")
                : GT
                    .$("pick two atoms in order to display the symmetry relationship between them"));
      return;
    }
    String s = measurementQueued.getMeasurementScript(" ", false);
    if (isSpin)
      runScript("spin" + s + " " + vwr.getInt(T.pickingspinrate));
    else
      runScript("draw symop " + s + ";show symop " + s);
  }

  private void reset() {
    runScript("!reset");
  }

  private boolean selectionWorking = false;

  private void selectAtoms(String item) {
    if (mp != null || selectionWorking)
      return;
    selectionWorking = true;
    String s = (rubberbandSelectionMode
        || bnd(clickAction, ACTION_selectToggle) ? "selected and not ("
        + item + ") or (not selected) and " : bnd(clickAction,
        ACTION_selectAndNot) ? "selected and not " : bnd(clickAction,
        ACTION_selectOr) ? "selected or " : clickAction == 0
        || bnd(clickAction, ACTION_selectToggleExtended) ? "selected tog "
        : bnd(clickAction, ACTION_select) ? "" : null);
    if (s != null) {
      s += "(" + item + ")";
      try {
        BS bs = vwr.getAtomBitSetEval(null, s);
        setAtomsPicked(bs, "selected: " + Escape.eBS(bs));
        vwr.refresh(Viewer.REFRESH_SYNC_MASK, "selections set");
      } catch (Exception e) {
        // ignore
      }
    }
    selectionWorking = false;
  }

  private void setAtomsPicked(BS bs, String msg) {
    vwr.select(bs, false, 0, false);
    vwr.setStatusAtomPicked(-1, msg, null, false);
  }

  private void selectRb(int action) {
    BS bs = vwr.ms.findAtomsInRectangle(rectRubber);
    if (bs.length() > 0) {
      String s = Escape.eBS(bs);
      if (bnd(action, ACTION_selectOr))
        runScript("selectionHalos on;select selected or " + s);
      else if (bnd(action, ACTION_selectAndNot))
        runScript("selectionHalos on;select selected and not " + s);
      else
        // ACTION_selectToggle
        runScript("selectionHalos on;select selected tog " + s);
    }
    vwr.refresh(Viewer.REFRESH_SYNC_MASK, "mouseReleased");
  }

  private void toggleMeasurement() {
    if (mp == null)
      return;
    int measurementCount = mp.count;
    if (measurementCount >= 2 && measurementCount <= 4)
      runScript("!measure "
          + mp.getMeasurementScript(" ", true));
    exitMeasurementMode(null);
  }

  private void zoomTo(int atomIndex) {
    runScript("zoomTo (atomindex=" + atomIndex + ")");
    vwr.setStatusAtomPicked(atomIndex, null, null, false);
  }

  @Override
  public boolean keyTyped(int keyChar, int modifiers) {
    return false;
  }

  public boolean userActionEnabled(int action) {
    return vwr.isFunction(getActionName(action).toLowerCase());
  }

  /**
   * If the user has created a function to handle this action, 
   * run it and cancel action processing if that function returns an explicit FALSE;
   * 
   * @param action
   * @param params
   * @return true to continue with the standard action
   */
  public boolean userAction(int action, Object[] params) {
    if (!userActionEnabled(action))
        return false;
    SV result = ScriptEval.runUserAction(getActionName(action), params, vwr);
    return !SV.vF.equals(result);
  }

}

class MotionPoint {
  int index;
  int x;
  int y;
  long time;

  void set(int index, int x, int y, long time) {
    this.index = index;
    this.x = x;
    this.y = y;
    this.time = time;
  }

  @Override
  public String toString() {
    return "[x = " + x + " y = " + y + " time = " + time + " ]";
  }
}

class Gesture {
  private int action;
  MotionPoint[] nodes;
  private int ptNext;
  private long time0;
  private Viewer vwr;

  public Gesture(int nPoints, Viewer vwr) {
    this.vwr = vwr;
    nodes = new MotionPoint[nPoints];
    for (int i = 0; i < nPoints; i++)
      nodes[i] = new MotionPoint();
  }

  void setAction(int action, long time) {
    this.action = action;
    ptNext = 0;
    time0 = time;
    for (int i = 0; i < nodes.length; i++)
      nodes[i].index = -1;
  }

  int add(int action, int x, int y, long time) {
    this.action = action;
    getNode(ptNext).set(ptNext, x, y, time - time0);
    ptNext++;
    return ptNext;
  }

  public long getTimeDifference(int nPoints) {
    nPoints = getPointCount2(nPoints, 0);
    if (nPoints < 2)
      return 0;
    MotionPoint mp1 = getNode(ptNext - 1);
    MotionPoint mp0 = getNode(ptNext - nPoints);
    return mp1.time - mp0.time;
  }

  public float getSpeedPixelsPerMillisecond(int nPoints, int nPointsPrevious) {
    nPoints = getPointCount2(nPoints, nPointsPrevious);
    if (nPoints < 2)
      return 0;
    MotionPoint mp1 = getNode(ptNext - 1 - nPointsPrevious);
    MotionPoint mp0 = getNode(ptNext - nPoints - nPointsPrevious);
    float dx = ((float) (mp1.x - mp0.x)) / vwr.getScreenWidth() * 360;
    float dy = ((float) (mp1.y - mp0.y)) / vwr.getScreenHeight() * 360;
    return (float) Math.sqrt(dx * dx + dy * dy) / (mp1.time - mp0.time);
  }

  int getDX(int nPoints, int nPointsPrevious) {
    nPoints = getPointCount2(nPoints, nPointsPrevious);
    if (nPoints < 2)
      return 0;
    MotionPoint mp1 = getNode(ptNext - 1 - nPointsPrevious);
    MotionPoint mp0 = getNode(ptNext - nPoints - nPointsPrevious);
    return mp1.x - mp0.x;
  }

  int getDY(int nPoints, int nPointsPrevious) {
    nPoints = getPointCount2(nPoints, nPointsPrevious);
    if (nPoints < 2)
      return 0;
    MotionPoint mp1 = getNode(ptNext - 1 - nPointsPrevious);
    MotionPoint mp0 = getNode(ptNext - nPoints - nPointsPrevious);
    return mp1.y - mp0.y;
  }

  int getPointCount() {
    return ptNext;
  }

  private int getPointCount2(int nPoints, int nPointsPrevious) {
    if (nPoints > nodes.length - nPointsPrevious)
      nPoints = nodes.length - nPointsPrevious;
    int n = nPoints + 1;
    for (; --n >= 0;)
      if (getNode(ptNext - n - nPointsPrevious).index >= 0)
        break;
    return n;
  }

  MotionPoint getNode(int i) {
    return nodes[(i + nodes.length + nodes.length) % nodes.length];
  }

  @Override
  public String toString() {
    if (nodes.length == 0)
      return "" + this;
    return Binding.getMouseActionName(action, false) + " nPoints = " + ptNext
        + " " + nodes[0];
  }
}

