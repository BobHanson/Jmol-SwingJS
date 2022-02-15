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

import org.jmol.api.SymmetryInterface;
import org.jmol.i18n.GT;
import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.Bond;
import org.jmol.modelset.MeasurementPending;
import org.jmol.modelset.ModelSet;
import org.jmol.script.SV;
import org.jmol.script.ScriptEval;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.Edge;
import org.jmol.util.Elements;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.SimpleUnitCell;
import org.jmol.viewer.ActionManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.MouseState;
import org.jmol.viewer.Viewer;

import javajs.util.AU;
import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.T3;
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

public class ModelKit {
  
  private Viewer vwr;
  
  private ModelKitPopup menu;

  // scripting options
  
  //  { "xtalModeMenu", "mkmode_molecular mkmode_view mkmode_edit" }, 
  public static final String MODE_OPTIONS = ";view;edit;molecular;";
  public static final String SYMMETRY_OPTIONS = ";none;applylocal;retainlocal;applyfull;";
  public static final String UNITCELL_OPTIONS = ";packed;extend;";
  public static final String BOOLEAN_OPTIONS = ";autobond;hidden;showsymopinfo;clicktosetelement;addhydrogen;addhydrogens;";
  public static final String SET_OPTIONS = ";element;";

  ////////////// modelkit state //////////////
  
  final static int STATE_BITS_XTAL /* 0b00000000011*/ = 0x03;
  final static int STATE_MOLECULAR /* 0b00000000000*/ = 0x00;
  final static int STATE_XTALVIEW /* 0b00000000001*/ = 0x01;
  final static int STATE_XTALEDIT /* 0b00000000010*/ = 0x02;

  final static int STATE_BITS_SYM_VIEW /* 0b00000011100*/ = 0x1c;
  final static int STATE_SYM_NONE      /* 0b00000000000*/ = 0x00;
  final static int STATE_SYM_SHOW      /* 0b00000001000*/ = 0x08;

  final static int STATE_BITS_SYM_EDIT   /* 0b00011100000*/ = 0xe0;
  final static int STATE_SYM_APPLYLOCAL  /* 0b00000100000*/ = 0x20;
  final static int STATE_SYM_RETAINLOCAL /* 0b00001000000*/ = 0x40;
  final static int STATE_SYM_APPLYFULL   /* 0b00010000000*/ = 0x80;

  final static int STATE_BITS_UNITCELL   /* 0b11100000000*/ = 0x700;
  final static int STATE_UNITCELL_PACKED /* 0b00000000000*/ = 0x000;
  final static int STATE_UNITCELL_EXTEND /* 0b00100000000*/ = 0x100;

  private static final P3 Pt000 = new P3();

  int state = STATE_MOLECULAR & STATE_SYM_NONE & STATE_SYM_APPLYFULL
      & STATE_UNITCELL_EXTEND; // 0x00
  
  float rotationDeg;

  String atomHoverLabel = "C", bondHoverLabel = GT.$("increase order"), xtalHoverLabel;

  boolean hasUnitCell;
  String[] allOperators;
  int currentModelIndex = -1;
  
  boolean alertedNoEdit;
  
  protected ModelSet lastModelSet;

  String pickAtomAssignType = "C";
  String lastElementType = "C";
  char pickBondAssignType = 'p'; // increment up
  boolean isPickAtomAssignCharge; // pl or mi

  BS bsHighlight = new BS();

  int bondIndex = -1, bondAtomIndex1 = -1, bondAtomIndex2 = -1;

  BS bsRotateBranch;
  int branchAtomIndex;
  boolean isRotateBond;

  int[] screenXY = new int[2]; // for tracking mouse-down on bond

  boolean showSymopInfo = true;
  
  /**
   * when TRUE, add H atoms to C when added to the modelSet. 
   */
  boolean addXtalHydrogens = true;
  
  /**
   * Except for H atoms, do not allow changes to elements just by clicking them. 
   * This protects against doing that inadvertently when editing.
   * 
   */
  boolean clickToSetElement = true; 
  
  /**
   * set to true for proximity-based autobonding (prior to 14.32.4/15.2.4 the default was TRUE
   */
  boolean autoBond = false;
  
  
  P3 centerPoint, spherePoint, viewOffset;
  float centerDistance;
  Object symop;
  int centerAtomIndex = -1, secondAtomIndex = -1, atomIndexSphere = -1;
  String drawData;
  String drawScript;
  int iatom0;

  String bondRotationName = ".modelkitMenu.bondMenu.rotateBondP!RD";
  
  String lastCenter = "0 0 0", lastOffset = "0 0 0";

  private Atom a0, a3;


  public ModelKit() {
  }

  //////////////// menu creation and update ///////////////
    
  public void setMenu(ModelKitPopup menu) {
    // from Viewer
    this.menu = menu;
    this.vwr = menu.vwr;
    menu.modelkit = this;
    initializeForModel();
  }

  public void initializeForModel() {
    // from Viewer also
       resetBondFields();
    allOperators = null;
    currentModelIndex = -999;
    iatom0 = 0;
    atomIndexSphere = centerAtomIndex = secondAtomIndex = -1;
    centerPoint = spherePoint = null;
   // no! atomHoverLabel = bondHoverLabel = xtalHoverLabel = null;
    symop = null;
    setDefaultState(setHasUnitCell() ? STATE_XTALVIEW : STATE_MOLECULAR);
    //setProperty("clicktosetelement",Boolean.valueOf(!hasUnitCell));
    //setProperty("addhydrogen",Boolean.valueOf(!hasUnitCell));
  }

  public void showMenu(int x, int y) {

    // from viewer
    
    menu.jpiShow(x, y);
  }

  public String getDefaultModel() {
    
    // from Viewer
    
    return (addXtalHydrogens ? JC.MODELKIT_ZAP_STRING : "1\n\nC 0 0 0\n");
  }

  public void updateMenu() {
    
    // from Viewer
    
    menu.jpiUpdateComputedMenus();
  }

  public void dispose() {
    
    // from Viewer
    
    menu.jpiDispose();
    menu.modelkit = null;
    menu = null;
    vwr = null;
  }
  
  public boolean isPickAtomAssignCharge() {
    
    // from ActionManager
    
    return isPickAtomAssignCharge;
  }
  public boolean isHidden() {
    
    // from FrankReneder
    
    return menu.hidden;
  }

  /**
   * for the thin box on the top left of the window
   * 
   * @return [ "atomMenu" | "bondMenu" | "xtalMenu" | null ]
   */
  public String getActiveMenu() {
    
    // from FrankRender
    
    return menu.activeMenu;
  }

  public int getRotateBondIndex() {

    // from ActionManager
    
    return (getMKState() == STATE_MOLECULAR && isRotateBond ? bondIndex : -1);
  }
    
  /** Get a property of the modelkit.
   * 
   * @param name
   * @return value
   */
  public Object getProperty(String name) {
    
    name = name.toLowerCase().intern();

    if (name == "ismolecular") {
      return Boolean.valueOf(getMKState() == STATE_MOLECULAR);
    }

    if (name == "alloperators") {
      return allOperators;
    }

    if (name == "data") {
      return getinfo();
    }

    
    return setProperty(name, null);
  }
  
  /**
   * Modify the state by setting a property.
   * 
   * Also can be used for "get" purposes.
   * 
   * @param name
   * @param value to set, or null to simply return the current value
   * @return null or "get" value
   */
  public synchronized Object setProperty(String name, Object value) {

    // from Viewer and CmdExt
    
    try {
      name = name.toLowerCase().intern();
      //    if (value != null)
      //      System.out.println("ModelKitPopup.setProperty " + name + "=" + value);

      // boolean get/set
      
      if (name == "reset") {
//        setProperty("atomType", "C");
//        setProperty("bondType", "p");
        return null;
      }
      
      if (name == "addhydrogen" || name == "addhydrogens") {
        if (value != null)
          addXtalHydrogens = isTrue(value);
        return Boolean.valueOf(addXtalHydrogens);
      }

      if (name == "autobond") {
        if (value != null)
          autoBond = isTrue(value);
        return Boolean.valueOf(autoBond);
      }

      if (name == "clicktosetelement") {
        if (value != null)
          clickToSetElement = isTrue(value);
        return Boolean.valueOf(clickToSetElement);
      }

      if (name == "hidden") {
        if (value != null)
          menu.hidden = isTrue(value);
        return Boolean.valueOf(menu.hidden);
      }

      if (name == "showsymopinfo") {
        if (value != null)
          showSymopInfo = isTrue(value);
        return Boolean.valueOf(showSymopInfo);
      }

      if (name == "symop") {
        setDefaultState(STATE_XTALVIEW);
        if (value != null) {
          symop = value;
          showSymop(symop);
        }
        return symop;
      }

      if (name == "atomtype") {
        if (value != null) {
          pickAtomAssignType = (String) value;
          isPickAtomAssignCharge = (pickAtomAssignType.equals("pl")
              || pickAtomAssignType.equals("mi"));
          if (!isPickAtomAssignCharge && !"X".equals(pickAtomAssignType)) {
            lastElementType = pickAtomAssignType;
          }
        }
        return pickAtomAssignType;
      }

      if (name == "bondtype") {
        if (value != null) {
          String s = ((String) value).substring(0, 1).toLowerCase();
          if (" 012345pm".indexOf(s) > 0)
            pickBondAssignType = s.charAt(0);
          isRotateBond = false;
        }
        return "" + pickBondAssignType;
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

      if (name == "screenxy") {
        if (value != null) {
          screenXY = (int[]) value;
        }
        return screenXY;
      }

      // set only (always returning null):

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

      if (name == "center") {
        setDefaultState(STATE_XTALVIEW);
        centerPoint = (P3) value;
        lastCenter = centerPoint.x + " " + centerPoint.y + " " + centerPoint.z;
        centerAtomIndex = (centerPoint instanceof Atom ? ((Atom) centerPoint).i
            : -1);
        atomIndexSphere = -1;
        secondAtomIndex = -1;
        processAtomClick(centerAtomIndex);
        return null;
      }

      if (name == "scriptassignbond") {
        // from ActionManger only
        appRunScript("modelkit assign bond [{" + value + "}] \""
            + pickBondAssignType + "\"");
        return null;
      }

      // get only, but with a value argument
      
      if (name == "hoverlabel") {
        // no setting of this, only getting
        return getHoverLabel(((Integer) value).intValue());
      }

      // not yet implemented
      
      if (name == "invariant") {
        // not really a model kit issue
        int iatom = (value instanceof BS ? ((BS) value).nextSetBit(0) : -1);
        P3 atom = vwr.ms.getAtom(iatom);
        return (atom == null ? null : vwr.getSymmetryInfo(iatom, null, -1, null, atom, atom, T.array,
            null, 0, 0, 0));
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
          atomIndexSphere = (spherePoint instanceof Atom
              ? ((Atom) spherePoint).i
              : -1);
        }
        return spherePoint;
      }


      if (name == "addconstraint") {
        notImplemented("setProperty: addConstraint");
        return null;
      }

      if (name == "removeconstraint") {
        notImplemented("setProperty: removeConstraint");
        return null;
      }

      if (name == "removeallconstraints") {
        notImplemented("setProperty: removeAllConstraints");
        return null;
      }

      System.err.println("ModelKitPopup.setProperty? " + name + " " + value);

    } catch (Exception e) {
      return "?";
    }

    return null;
  }
  
  public MeasurementPending setBondMeasure(int bi,
                                           MeasurementPending mp) {
    if (branchAtomIndex < 0)
      return null;
    Bond b = vwr.ms.bo[bi];
    Atom a1 = b.atom1;
    Atom a2 = b.atom2;
    a0 = a3 = null;
    if (a1.getCovalentBondCount() == 1 || a2.getCovalentBondCount() == 1)
      return null;
    mp.addPoint((a0 = getOtherAtomIndex(a1, a2)).i, null, true);
    mp.addPoint(a1.i, null, true);
    mp.addPoint(a2.i, null, true);
    mp.addPoint((a3 = getOtherAtomIndex(a2, a1)).i, null, true);
    mp.mad = 50;
    mp.inFront = true;
    //System.out.println(mp);
    return mp;
  }

  /**
   * Actually rotate the bond.
   * 
   * @param deltaX
   * @param deltaY
   * @param x
   * @param y
   * @param forceFull 
   */
  public void actionRotateBond(int deltaX, int deltaY, int x, int y, boolean forceFull) {

    // from Viewer.moveSelection by ActionManager.checkDragWheelAction
    
    if (bondIndex < 0)
      return;
    BS bsBranch = bsRotateBranch;
    Atom atomFix, atomMove;
    ModelSet ms = vwr.ms;
    Bond b = ms.bo[bondIndex];
    if (forceFull) {
      bsBranch = null;
      branchAtomIndex = -1;
    }
    if (bsBranch == null) {
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
        forceFull = true;
      }
      bsRotateBranch = bsBranch;
      bondAtomIndex1 = atomFix.i;
      bondAtomIndex2 = atomMove.i;
    } else {
      atomFix = ms.at[bondAtomIndex1];
      atomMove = ms.at[bondAtomIndex2];
    }
    V3 v1 = V3.new3(atomMove.sX - atomFix.sX, atomMove.sY - atomFix.sY, 0);
    v1.scale(1f/v1.length());
    V3 v2 = V3.new3(deltaX, deltaY, 0);
    v1.cross(v1, v2);
    
    float f = (v1.z > 0 ? 1 : -1);
    float degrees = f * ((int) v2.length()/2 + 1);
    if (!forceFull && a0 != null) {
      // integerize
      float ang0 = Measure.computeTorsion(a0, b.atom1, b.atom2, a3, true);
      float ang1 = Math.round(ang0 + degrees);
      degrees = ang1 - ang0;
    }
    BS bs = BSUtil.copy(bsBranch);
    bs.andNot(vwr.slm.getMotionFixedAtoms());

    vwr.rotateAboutPointsInternal(null, atomFix, atomMove, 0, degrees, false, bs,
        null, null, null, null);
  }

  /**
   * handle a mouse-generated assignNew event
   * 
   * @param pressed
   * @param dragged
   * @param mp
   * @param dragAtomIndex
   * @return true if we should do a refresh now
   */
  public boolean handleAssignNew(MouseState pressed, MouseState dragged,
                                 MeasurementPending mp, int dragAtomIndex) {

    // From ActionManager
    
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
      if (((Atom) mp.getAtom(1)).isBonded((Atom) mp.getAtom(2))) {
        appRunScript("modelkit assign bond " + mp.getMeasurementScript(" ", false) + "'p'");
      } else {
        appRunScript("modelkit connect " + mp.getMeasurementScript(" ", false));
      }
    } else {
      if (atomType.equals("Xx")) {
        atomType = lastElementType;
      }
      if (inRange) {
        String s = "modelkit assign atom ({" + dragAtomIndex + "}) \"" + atomType + "\" true";
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
          assignAtomClick(dragAtomIndex, "X", null);
        } else {
          int x = dragged.x;
          int y = dragged.y;

          if (vwr.antialiased) {
            x <<= 1;
            y <<= 1;
          }

          P3 ptNew = P3.new3(x, y, a.sZ);
          vwr.tm.unTransformPoint(ptNew, ptNew);
          assignAtomClick(dragAtomIndex, atomType, ptNew);
        }
      }
    }
    return true;
  }
  
  public void cmdAssignAtom(int atomIndex, P3 pt, String type, String cmd, boolean isClick) {
    
    // from CmdExt
    
    if (isClick && type.equals("X"))
      vwr.setModelKitRotateBondIndex(-1);
    int ac = vwr.ms.ac;
    Atom atom = (atomIndex < 0 ? null : vwr.ms.at[atomIndex]);
    if (pt == null) {
      if (atomIndex < 0 || atom == null)
        return;
      int mi = atom.mi;
      vwr.sm.setStatusStructureModified(atomIndex, mi, 1, cmd, 1, null);
      // After this next command, vwr.modelSet will be a different instance
      assignAtom(atomIndex, type, autoBond, true, true);
      if (!PT.isOneOf(type, ";Mi;Pl;X;"))
        vwr.ms.setAtomNamesAndNumbers(0, -ac, null);
      vwr.sm.setStatusStructureModified(atomIndex, mi, -1, "OK", 1, null);
      vwr.refresh(Viewer.REFRESH_SYNC_MASK, "assignAtom");
      return;
    }
    BS bs = (atomIndex < 0 ? new BS() : BSUtil.newAndSetBit(atomIndex));
    P3[] pts = new P3[] { pt };
    Lst<Atom> vConnections = new Lst<Atom>();
    int modelIndex = -1;
    if (atom != null) {
      vConnections.addLast(atom);
      modelIndex = atom.mi;
      vwr.sm.setStatusStructureModified(atomIndex, modelIndex, 3, cmd, 1, null);
    }
    try {
      int pickingMode = vwr.acm.getAtomPickingMode();
      boolean wasHidden = menu.hidden;
      boolean isMK = vwr.getBoolean(T.modelkitmode);
      if (!isMK) {
        vwr.setBooleanProperty("modelkitmode", true);
        menu.hidden = true;
        menu.allowPopup = false;
      }
      bs = vwr.addHydrogensInline(bs, vConnections, pts);
      if (!isMK) {
        vwr.setBooleanProperty("modelkitmode", false);
        menu.hidden = wasHidden;
        menu.allowPopup = true;
        vwr.acm.setPickingMode(pickingMode);
        menu.hidePopup();
      }
      int atomIndex2 = bs.nextSetBit(0);
      int state = getMKState();
      setMKState(STATE_MOLECULAR);
      assignAtom(atomIndex2, type, false, atomIndex >= 0, true);
      if (atomIndex >= 0)
        assignAtom(atomIndex, ".", false, true, isClick);
      setMKState(state);
      atomIndex = atomIndex2;
    } catch (Exception ex) {
      //
    }
    vwr.ms.setAtomNamesAndNumbers(0, -ac, null);
    vwr.sm.setStatusStructureModified(atomIndex, modelIndex, -3, "OK", 1, null);
  }

  public void cmdAssignBond(int bondIndex, char type, String cmd) {
    
    // from CmdExt
    
    int modelIndex = -1;
    try {
      if (type == '-')
        type = pickBondAssignType;
      modelIndex = vwr.ms.bo[bondIndex].atom1.mi;
      int ac = vwr.ms.ac;
      vwr.sm.setStatusStructureModified(bondIndex, modelIndex, 2,
          cmd, 1, null);
      BS bsAtoms = assignBond(bondIndex, type);
      vwr.ms.setAtomNamesAndNumbers(0, -ac, null);
      if (bsAtoms == null || type == '0')
        vwr.refresh(Viewer.REFRESH_SYNC_MASK, "setBondOrder");      
      vwr.sm.setStatusStructureModified(bondIndex, modelIndex, -2, "" + type, 1, null);
    } catch (Exception ex) {
      Logger.error("assignBond failed");
      vwr.sm.setStatusStructureModified(bondIndex, modelIndex, -2, "ERROR " + ex, 1, null);
    }
  }

  public void cmdAssignConnect(int index, int index2, char type, String cmd) {

    // from CmdExt
    
    float[][] connections = AU.newFloat2(1);
    connections[0] = new float[] { index, index2 };
    int modelIndex = vwr.ms.at[index].mi;
    vwr.sm.setStatusStructureModified(index, modelIndex, 2, cmd, 1, null);
    vwr.ms.connect(connections);    
    int ac = vwr.ms.ac;
    // note that vwr.ms changes during the assignAtom command 
    assignAtom(index, ".", true, true, false);
    assignAtom(index2, ".", true, true, false);
    vwr.ms.setAtomNamesAndNumbers(0, -ac, null);
    vwr.sm.setStatusStructureModified(index, modelIndex, -2, "OK", 1, null);
    if (type != '1') {
      BS bs = BSUtil.newAndSetBit(index);
      bs.set(index2);
      bs = vwr.getBondsForSelectedAtoms(bs);
      int bondIndex = bs.nextSetBit(0);
      cmdAssignBond(bondIndex, type, cmd);
    }
    vwr.refresh(Viewer.REFRESH_SYNC_MASK, "assignConnect");
  }

  public void assignAtomClick(int atomIndex, String element, P3 ptNew) {

    // from Mouse
    
    vwr.script("modelkit assign atom ({" + atomIndex + "}) \"" + element + "\" "
          + (ptNew == null ? "" : Escape.eP(ptNew)) + " true");
  }


  ///////// from ModelKitPopup /////////
  
  boolean isXtalState() {
    return ((state & STATE_BITS_XTAL) != 0);
  }

  void setMKState(int bits) {
    state = (state & ~STATE_BITS_XTAL) | (hasUnitCell ? bits : STATE_MOLECULAR);
  }

  int getMKState() {
    return state & STATE_BITS_XTAL;
  }

  void setSymEdit(int bits) {
    state = (state & ~STATE_BITS_SYM_EDIT) | bits;
  }

  int getSymEditState() {
    return state & STATE_BITS_SYM_EDIT;
  }

  void setSymViewState(int bits) {
    state = (state & ~STATE_BITS_SYM_VIEW) | bits;
  }

  int getSymViewState() {
    return state & STATE_BITS_SYM_VIEW;
  }

  void setUnitCell(int bits) {
    state = (state & ~STATE_BITS_UNITCELL) | bits;
  }

  int getUnitCellState() {
    return state & STATE_BITS_UNITCELL;
  }

  void exitBondRotation(String text) {
    isRotateBond = false;
    if (text != null)
      bondHoverLabel = text;
    vwr.highlight(null);
    vwr.setPickingMode(null, ActionManager.PICKING_ASSIGN_BOND);
  }

  void resetBondFields() {
    bsRotateBranch = null;
    // do not set bondIndex to -1 here
    branchAtomIndex = bondAtomIndex1 = bondAtomIndex2 = -1;
  }

  void processXtalClick(String id, String action) {
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
      while(action != null)
        action = processSelOpClick(action);
    } else if (action.startsWith("mksymmetry_")) {
      processSymClick(action);
    } else if (action.startsWith("mkunitcell_")) {
      processUCClick(action);
    } else {
      notImplemented("XTAL click " + action);
    }
    menu.updateAllXtalMenuOptions();
  }

  boolean processSymop(String id, boolean isFocus) {
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

  void setDefaultState(int mode) {
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

  String[] getAllOperators() {
    if (allOperators != null)
      return allOperators;
    String data = runScriptBuffered("show symop");
    allOperators = PT.split(data.trim().replace('\t', ' '), "\n");
    return allOperators;
  }

  boolean setHasUnitCell() {
    return hasUnitCell = (vwr.getCurrentUnitCell() != null);
  }

  boolean checkNewModel() {
    boolean isNew = false;
    if (vwr.ms != lastModelSet) {
      lastModelSet = vwr.ms;
      isNew = true;
    }
    currentModelIndex = Math.max(vwr.am.cmi, 0);
    iatom0 = vwr.ms.am[currentModelIndex].firstAtomIndex;
    return isNew;
  }

  String getSymopText() {
    return (symop == null || allOperators == null ? null : symop instanceof Integer ? allOperators[((Integer) symop).intValue() - 1] : symop.toString());
  }

  String getCenterText() {
    return (centerAtomIndex < 0 && centerPoint == null ? null
        : centerAtomIndex >= 0 ? vwr.getAtomInfo(centerAtomIndex) : centerPoint.toString());
  }

  void resetAtomPickType() {
    pickAtomAssignType = lastElementType;
  }

  void setHoverLabel(String activeMenu, String text) {
    if (text == null)
      return;
    if (activeMenu == ModelKitPopup.BOND_MENU) {
      bondHoverLabel = text;
    } else if (activeMenu == ModelKitPopup.ATOM_MENU) {
      atomHoverLabel = text;
    } else if (activeMenu == ModelKitPopup.XTAL_MENU) {
      xtalHoverLabel = atomHoverLabel = text;
    }
  }

  String getElementFromUser() {
    String element = promptUser(GT.$("Element?"), "");
    return (element == null || Elements.elementNumberFromSymbol(element, true) == 0 ? null : element);
  }

  void processMKPropertyItem(String name, boolean TF) {
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

  //////// private methods /////////

  /**
   * Original ModelKitPopup functionality -- assign an atom.
   * 
   * @param atomIndex
   * @param type
   * @param autoBond
   * @param addHsAndBond
   * @param isClick whether this is a click or not
   */
  private void assignAtom(int atomIndex, String type, boolean autoBond,
                          boolean addHsAndBond, boolean isClick) {
    if (isClick) {
      
      if (isVwrRotateBond()) {
        bondAtomIndex1 = atomIndex;
        return;
      }

      if (processAtomClick(atomIndex) || !clickToSetElement
          && vwr.ms.getAtom(atomIndex).getElementNumber() != 1)
        return;

    }
    Atom atom = vwr.ms.at[atomIndex];
    if (atom == null)
      return;
    vwr.ms.clearDB(atomIndex);
    if (type == null)
      type = "C";

    // not as simple as just defining an atom.
    // if we click on an H, and C is being defined,
    // this sprouts an sp3-carbon at that position.

    BS bs = new BS();
    boolean wasH = (atom.getElementNumber() == 1);
    int atomicNumber = (type.equals("Xx") ? 0 : PT.isUpperCase(type.charAt(0))
        ? Elements.elementNumberFromSymbol(type, true)
        : -1);

    // 1) change the element type or charge

    boolean isDelete = false;
    if (atomicNumber >= 0) {
      boolean doTaint = (atomicNumber > 1 || !addHsAndBond);
      vwr.ms.setElement(atom, atomicNumber, doTaint);
      vwr.shm.setShapeSizeBs(JC.SHAPE_BALLS, 0, vwr.rd,
          BSUtil.newAndSetBit(atomIndex));
      vwr.ms.setAtomName(atomIndex, type + atom.getAtomNumber(), doTaint);
      if (vwr.getBoolean(T.modelkitmode))
        vwr.ms.am[atom.mi].isModelKit = true;
      if (!vwr.ms.am[atom.mi].isModelKit || atomicNumber > 1)
        vwr.ms.taintAtom(atomIndex, AtomCollection.TAINT_ATOMNAME);
    } else if (type.toLowerCase().equals("pl")) {
      atom.setFormalCharge(atom.getFormalCharge() + 1);
    } else if (type.toLowerCase().equals("mi")) {
      atom.setFormalCharge(atom.getFormalCharge() - 1);
    } else if (type.equals("X")) {
      isDelete = true;
    } else if (!type.equals(".") || !addXtalHydrogens) {
      return; // uninterpretable
    }

    if (!addHsAndBond)
      return;

    // type = "." is for connect
    
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
      vwr.addHydrogens(bsA, Viewer.MIN_SILENT);
  }

  /** 
   * Original ModelKit functionality -- assign a bond.
   * 
   * @param bondIndex
   * @param type
   * @return bit set of atoms to modify
   */
  private BS assignBond(int bondIndex, char type) {
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
      vwr.addHydrogens(bsAtoms, Viewer.MIN_SILENT);
    return bsAtoms;
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
      resetBondFields();
      return;
    }
    
    bsRotateBranch = null;
    branchAtomIndex = -1;   
    bondIndex = index;
    isRotateBond = isRotate;
    bondAtomIndex1 = vwr.ms.bo[index].getAtomIndex1();
    bondAtomIndex2 = vwr.ms.bo[index].getAtomIndex2();
    menu.setActiveMenu(ModelKitPopup.BOND_MENU);
  }


  /**
   * 
   * @param pressed
   * @param dragged
   * @param countPlusIndices
   * @return true if handled here
   */
  private boolean handleDragAtom(MouseState pressed, MouseState dragged,
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

  private void showSymop(Object symop) {
    secondAtomIndex = -1;
    this.symop = symop;
    showXtalSymmetry();
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

  private Object getinfo() {
    Map<String, Object> info = new Hashtable<String, Object>();
    addInfo(info, "addHydrogens", Boolean.valueOf(addXtalHydrogens));
    addInfo(info, "autobond", Boolean.valueOf(autoBond));
    addInfo(info, "clickToSetElement", Boolean.valueOf(clickToSetElement));
    addInfo(info, "hidden", Boolean.valueOf(menu.hidden));
    addInfo(info, "showSymopInfo", Boolean.valueOf(showSymopInfo));
    addInfo(info, "centerPoint" , centerPoint);
    addInfo(info, "centerAtomIndex", Integer.valueOf(centerAtomIndex));
    addInfo(info, "secondAtomIndex", Integer.valueOf(secondAtomIndex));
    addInfo(info, "symop",  symop);
    addInfo(info, "offset",  viewOffset);
    addInfo(info, "drawData", drawData);
    addInfo(info, "drawScript", drawScript);
    addInfo(info, "isMolecular", Boolean.valueOf(getMKState() == STATE_MOLECULAR));
    return info;
  }

  private void addInfo(Map<String, Object> info, String key, Object value) {
    if (value != null)
      info.put(key, value);
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

  private void processModeClick(String action) {
    processMKPropertyItem(action, false); 
  }

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

  private String processSelOpClick(String action) {
    secondAtomIndex = -1;
    if (action == "mkselop_addoffset") {
      String pos = promptUser(
          "Enter i j k for an offset for viewing the operator - leave blank to clear",
          lastOffset);
      if (pos == null)
        return null;
      lastOffset = pos;
      if (pos.length() == 0 || pos == "none") {
        setProperty("offset", "none");
        return null;
      }
      P3 p = pointFromTriad(pos);
      if (p == null) {
        return action;
      }
      setProperty("offset", p);
    } else if (action == "mkselop_atom2") {
      notImplemented(action);
    }
    return null;
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
   * Called by Viewer.hoverOn to set the special label if desired.
   * 
   * @param atomIndex
   * @return special label or null
   */
  private String getHoverLabel(int atomIndex) {
    int state = getMKState();
    String msg = null;
    switch (state) {
    case STATE_XTALVIEW:
      if (symop == null)
        symop = Integer.valueOf(1);
      msg = "view symop " + symop + " for " + vwr.getAtomInfo(atomIndex);
      break;
    case STATE_XTALEDIT:
      msg = "start editing for " + vwr.getAtomInfo(atomIndex);
      break;
    case STATE_MOLECULAR:
      Atom[] atoms = vwr.ms.at;
      if (isRotateBond) {
        if (atomIndex == bondAtomIndex1 || atomIndex == bondAtomIndex2) {
          msg = "rotate branch " + atoms[atomIndex].getAtomName();
          branchAtomIndex = atomIndex;
          bsRotateBranch = null;
        } else {
          msg = "rotate bond"  + getBondLabel(atoms);
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
          msg = atoms[atomIndex].getAtomName() + ": " + atomHoverLabel;
          vwr.highlight(BSUtil.newAndSetBit(atomIndex));
        }
      } else {
        if (msg == null) {
          switch (bsHighlight.cardinality()) {
          case 0:
            vwr.highlight(BSUtil.newAndSetBit(atomIndex));
            //$FALL-THROUGH$
          case 1:
            if (!isRotateBond)
              menu.setActiveMenu(ModelKitPopup.ATOM_MENU);
            if (atomHoverLabel.indexOf("charge") >= 0) {
              int ch = vwr.ms.at[atomIndex].getFormalCharge();
              ch += (atomHoverLabel.indexOf("increase") >= 0 ? 1 :-1);
              msg = atomHoverLabel + " to " + (ch > 0 ? "+" : "") + ch;
            } else {
              msg = atomHoverLabel;
            }
            msg = atoms[atomIndex].getAtomName() + ": " + msg;
            break;
          case 2:
            msg = bondHoverLabel + getBondLabel(atoms);
            break;
          }
        }
      }
      break;
    }
    return msg;
  }
  
  private String getBondLabel(Atom[] atoms) {
    return " for " + atoms[Math.min(bondAtomIndex1, bondAtomIndex2)].getAtomName() 
     + "-" + atoms[Math.max(bondAtomIndex1, bondAtomIndex2)].getAtomName();
   }

  private Atom getOtherAtomIndex(Atom a1, Atom a2) {
    Bond[] b = a1.bonds;
    Atom a;
    Atom ret = null;
    int zmin = Integer.MAX_VALUE;
    for (int i = -1; ++i < b.length;) {
      if (b[i] != null && b[i].isCovalent() && (a = b[i].getOtherAtom(a1)) != a2 && a.sZ < zmin) {
        zmin = a.sZ;
        ret = a;
      }
    }
    return ret;
  }

  private boolean isVwrRotateBond() {
    return (vwr.acm.getBondPickingMode() == ActionManager.PICKING_ROTATE_BOND);
  }

  private String promptUser(String msg, String def) {
    return vwr.prompt(msg, def, null, false);
  }

  private void appRunScript(String script) {
    vwr.evalStringQuiet(script);
  }

  private String runScriptBuffered(String script) {
    SB sb = new SB();
    try {
      ((ScriptEval) vwr.eval).runBufferedSafely(script, sb);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return sb.toString();
  }

  private static boolean isTrue(Object value) {
    return (Boolean.valueOf(value.toString()) == Boolean.TRUE);
  }

  private static P3 pointFromTriad(String pos) {
    float[] a = PT.parseFloatArray(PT.replaceAllCharacters(pos,  "{,}", " "));
    return (a.length == 3 && !Float.isNaN(a[2]) ? P3.new3(a[0], a[1], a[2]) : null);
  }

  private static void notImplemented(String action) {
    System.err.println("ModelKit.notImplemented(" + action + ")");
  }

  public String cmdAssignSpaceGroup(BS bs) {
    try {
      if (bs != null && bs.isEmpty())
        return "";
      SymmetryInterface uc = vwr.getCurrentUnitCell();
      if (uc == null)
        uc = vwr.getSymTemp()
            .setUnitCell(new float[] { 10, 10, 10, 90, 90, 90 }, false);
      BS bsAtoms = vwr.getThisModelAtoms();
      if (bs == null) {
        bs = SV.getBitSet(vwr.evaluateExpressionAsVariable("{within(unitcell)}"), true);
      }
      if (bs != null)
        bsAtoms.and(bs);
      boolean noAtoms = bsAtoms.isEmpty();
      int mi = (noAtoms ? 0 : vwr.ms.at[bsAtoms.nextSetBit(0)].getModelIndex());
      T3 m = uc.getUnitCellMultiplier();
      if (m != null && m.z == 1) {
        m.z = 0;
      }
      P3 supercell;
      float[] params;
      String name;
      String ita;
      BS basis;
      @SuppressWarnings("unchecked")
      Map<String, Object> sg = (noAtoms ? null
          : (Map<String, Object>) vwr.findSpaceGroup(bsAtoms, null, false));
      if (sg == null) {
        name = "P1";
        supercell = P3.new3(1, 1, 1);
        params = uc.getUnitCellParams();
        ita = "1";
        basis = null;
      } else {
        supercell = (P3) sg.get("supercell");
        params = (float[]) sg.get("unitcell");
        name = (String) sg.get("name");
        ita = (String) sg.get("itaFull");
        basis = (BS) sg.get("basis");
      }
      uc.setUnitCell(params, false);
      uc.setSpaceGroupTo(ita);
      uc.setSpaceGroupName(name);
      vwr.ms.setSpaceGroup(mi, uc, basis);
      P4 pt = SimpleUnitCell.ptToIJK(supercell, 1);
      vwr.ms.setUnitCellOffset(uc, pt, 0);
      return name + " basis=" + basis;
    } catch (Exception e) {
      if (!Viewer.isJS)
        e.printStackTrace();
      return e.getMessage();
    }
  }

}
