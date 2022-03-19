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

import java.util.Arrays;
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
import javajs.util.M4;
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
  
  private static class Constraint {

    public final static int TYPE_NONE     = 0;
    public final static int TYPE_DISTANCE = 1; // not implemented
    public final static int TYPE_ANGLE    = 2; // not implemented
    public final static int TYPE_DIHEDRAL = 3; // not implemented
    public final static int TYPE_VECTOR   = 4;
    public final static int TYPE_PLANE    = 5;
    public final static int TYPE_LOCKED   = 6;
    public final static int TYPE_GENERAL  = 7;
//    public final static int TYPE_SYMMETRY = 8;

    int type;
    
    private P3 pt;
    private P3[] points;
    private P3 offset;
    private P4 plane;
    private V3 unitVector;
    private double value;
      
    public Constraint(P3 pt, int type, Object[] params) throws IllegalArgumentException {
      this.pt = pt;
      this.type = type;
      switch (type) {
      case TYPE_NONE:
      case TYPE_GENERAL:
      case TYPE_LOCKED:
        break;
      case TYPE_VECTOR:
        offset = (P3) params[0];
        unitVector = V3.newV((T3) params[1]);
        unitVector.normalize();
        break;
      case TYPE_PLANE:
        plane = (P4) params[0];
        break;
//      case TYPE_SYMMETRY:
//        symop = (String) params[0];
//        points = new P3[1];
//        offset = (P3) params[1];
//        break;
      case TYPE_DISTANCE:
        // not implemented
        value = ((Double) params[0]).doubleValue();
        points = new P3[] { (P3) params[1], null };
        break;
      case TYPE_ANGLE:
        // not implemented
        value = ((Double) params[0]).doubleValue();
        points = new P3[] { (P3) params[1], (P3) params[2], null };
        break;
      case TYPE_DIHEDRAL:
        // not implemented
        value = ((Double) params[0]).doubleValue();
        points = new P3[] { (P3) params[1], (P3) params[2], (P3) params[3], null };
        break;
      default:
        throw new IllegalArgumentException();
      }      

    }

    
    public void constrain(P3 ptOld, P3 ptNew) {
      V3 v = new V3();
      P3 p = P3.newP(ptOld);
      switch (type) {
      case TYPE_NONE:
        return;
      case TYPE_GENERAL:
        return;
      case TYPE_LOCKED:
        ptNew.x = Float.NaN;
        return;
      case TYPE_VECTOR:
        if (pt == null) { // generic constraint 
          Measure.projectOntoAxis(p, offset, unitVector, v);
          if (p.distanceSquared(ptOld) >= JC.UC_TOLERANCE2) {
            ptNew.x = Float.NaN;
            return;
          }
        }
        Measure.projectOntoAxis(ptNew, offset, unitVector, v);
        break;
      case TYPE_PLANE:
        if (pt == null) { // generic constraint 
          if (Math.abs(Measure.getPlaneProjection(p, plane, v, v)) > 0.01f) {
            ptNew.x = Float.NaN;
            return;
          }
        }
        Measure.getPlaneProjection(ptNew, plane, v, v);
        ptNew.setT(v);
        break;
      }
    }
    
  }

  static Constraint locked = new Constraint(null, Constraint.TYPE_LOCKED, null);
  static Constraint none = new Constraint(null, Constraint.TYPE_NONE, null);

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
  
  final static int STATE_MOLECULAR /* 0b00000000000*/ = 0x00;
  final static int STATE_XTALVIEW /* 0b00000000001*/ = 0x01;
  final static int STATE_XTALEDIT /* 0b00000000010*/ = 0x02;
  final static int STATE_BITS_XTAL /* 0b00000000011*/ = 0x03;

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

  private Constraint constraint;


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
    setDefaultState(
        //setHasUnitCell() ? STATE_XTALVIEW);
         STATE_MOLECULAR);
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

    if (name == "constraint") {
      return constraint;
    }

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
   * @param key
   * @param value
   *        to set, or null to simply return the current value
   * @return null or "get" value
   */
  public synchronized Object setProperty(String key, Object value) {

    // from Viewer and CmdExt

    try {
      key = key.toLowerCase().intern();

      // boolean get/set

      if (key == "constraint") {
        constraint = null;
        clearAtomConstraints();
        Object[] o = (Object[]) value;
        if (o != null) {
          P3 v1 = (P3) o[0];
          P3 v2 = (P3) o[1];
          P4 plane = (P4) o[2];
          if (v1 != null && v2 != null) {
            constraint = new Constraint(null, Constraint.TYPE_VECTOR,
                new Object[] { v1, v2 });
          } else if (plane != null) {
            constraint = new Constraint(null, Constraint.TYPE_PLANE,
                new Object[] { plane });
          } else if (v1 != null)
            constraint = new Constraint(null, Constraint.TYPE_LOCKED, null);
        }
        // no message
        return null;
      }
      if (key == "reset") {
        //        setProperty("atomType", "C");
        //        setProperty("bondType", "p");
        return null;
      }

      if (key == "addhydrogen" || key == "addhydrogens") {
        if (value != null)
          addXtalHydrogens = isTrue(value);
        return Boolean.valueOf(addXtalHydrogens);
      }

      if (key == "autobond") {
        if (value != null)
          autoBond = isTrue(value);
        return Boolean.valueOf(autoBond);
      }

      if (key == "clicktosetelement") {
        if (value != null)
          clickToSetElement = isTrue(value);
        return Boolean.valueOf(clickToSetElement);
      }

      if (key == "hidden") {
        if (value != null) {
          menu.hidden = isTrue(value);
          vwr.setBooleanProperty("modelkitMode", true);
        }
        return Boolean.valueOf(menu.hidden);
      }

      if (key == "showsymopinfo") {
        if (value != null)
          showSymopInfo = isTrue(value);
        return Boolean.valueOf(showSymopInfo);
      }

      if (key == "symop") {
        setDefaultState(STATE_XTALVIEW);
        if (value != null) {
          symop = value;
          showSymop(symop);
        }
        return symop;
      }

      if (key == "atomtype") {
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

      if (key == "bondtype") {
        if (value != null) {
          String s = ((String) value).substring(0, 1).toLowerCase();
          if (" 012345pm".indexOf(s) > 0)
            pickBondAssignType = s.charAt(0);
          isRotateBond = false;
        }
        return "" + pickBondAssignType;
      }

      if (key == "bondindex") {
        if (value != null) {
          setBondIndex(((Integer) value).intValue(), false);
        }
        return (bondIndex < 0 ? null : Integer.valueOf(bondIndex));
      }

      if (key == "rotatebondindex") {
        if (value != null) {
          setBondIndex(((Integer) value).intValue(), true);
        }
        return (bondIndex < 0 ? null : Integer.valueOf(bondIndex));
      }

      if (key == "offset") {
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

      if (key == "screenxy") {
        if (value != null) {
          screenXY = (int[]) value;
        }
        return screenXY;
      }

      // set only (always returning null):

      if (key == "bondatomindex") {
        int i = ((Integer) value).intValue();
        if (i != bondAtomIndex2)
          bondAtomIndex1 = i;

        bsRotateBranch = null;
        return null;
      }

      if (key == "highlight") {
        if (value == null)
          bsHighlight = new BS();
        else
          bsHighlight = (BS) value;
        return null;
      }

      if (key == "mode") { // view, edit, or molecular
        boolean isEdit = ("edit".equals(value));
        setMKState("view".equals(value) ? STATE_XTALVIEW
            : isEdit ? STATE_XTALEDIT : STATE_MOLECULAR);
        if (isEdit)
          addXtalHydrogens = false;
        return null;
      }

      if (key == "symmetry") {
        setDefaultState(STATE_XTALEDIT);
        key = ((String) value).toLowerCase().intern();
        setSymEdit(key == "applylocal" ? STATE_SYM_APPLYLOCAL
            : key == "retainlocal" ? STATE_SYM_RETAINLOCAL
                : key == "applyfull" ? STATE_SYM_APPLYFULL : 0);
        showXtalSymmetry();
        return null;
      }

      if (key == "unitcell") { // packed or extend
        boolean isPacked = "packed".equals(value);
        setUnitCell(isPacked ? STATE_UNITCELL_PACKED : STATE_UNITCELL_EXTEND);
        viewOffset = (isPacked ? Pt000 : null);
        return null;
      }

      if (key == "center") {
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

      if (key == "scriptassignbond") {
        // from ActionManger only
        appRunScript("modelkit assign bond [{" + value + "}] \""
            + pickBondAssignType + "\"");
        return null;
      }

      // get only, but with a value argument

      if (key == "hoverlabel") {
        // no setting of this, only getting
        return getHoverLabel(((Integer) value).intValue());
      }

      // not yet implemented

      if (key == "invariant") {
        // not really a model kit issue
        int iatom = (value instanceof BS ? ((BS) value).nextSetBit(0) : -1);
        P3 atom = vwr.ms.getAtom(iatom);
        return (atom == null ? null
            : vwr.getSymmetryInfo(iatom, null, -1, null, atom, atom, T.array,
                null, 0, 0, 0));
      }

      if (key == "distance") {
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

      if (key == "point") {
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

      if (key == "addconstraint") {
        notImplemented("setProperty: addConstraint");
        return null;
      }

      if (key == "removeconstraint") {
        notImplemented("setProperty: removeConstraint");
        return null;
      }

      if (key == "removeallconstraints") {
        notImplemented("setProperty: removeAllConstraints");
        return null;
      }

      System.err.println("ModelKitPopup.setProperty? " + key + " " + value);

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
    return hasUnitCell = (vwr.getOperativeSymmetry() != null);
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
   * @param bsAtoms 
   * @return atomicNumber or -1
   */
  private int assignAtom(int atomIndex, String type, boolean autoBond,
                          boolean addHsAndBond, boolean isClick, BS bsAtoms) {
    
    if (isClick) {  
      if (isVwrRotateBond()) {
        bondAtomIndex1 = atomIndex;
        return -1;
      }

      if (processAtomClick(atomIndex) || !clickToSetElement
          && vwr.ms.getAtom(atomIndex).getElementNumber() != 1)
        return -1;

    }
    
    if (bsAtoms != null) {
      int n = -1;
      for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
        n = assignAtom(i, type, autoBond, addHsAndBond, isClick, null);
      }
      return n;
    }
    Atom atom = vwr.ms.at[atomIndex];
    if (atom == null)
      return -1;
    vwr.ms.clearDB(atomIndex);
    if (type == null)
      type = "C";

    // not as simple as just defining an atom.
    // if we click on an H, and C is being defined,
    // this sprouts an sp3-carbon at that position.

    BS bs = new BS();
    boolean wasH = (atom.getElementNumber() == 1);
    int atomicNumber = (type.equals("X") ? -1 : type.equals("Xx") ? 0 : PT.isUpperCase(type.charAt(0))
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
      return -1; // uninterpretable
    }

    if (!addHsAndBond)
      return atomicNumber;

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
    return atomicNumber;
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
   * Assign a given space group, currently only "P1"
   * @param bs atoms in the set defining the space group
   * @param name "P1" or "1" or ignored
   * @return
   */
  public String cmdAssignSpaceGroup(BS bs, String name) {
    boolean isP1 = (name.equalsIgnoreCase("P1") || name.equals("1"));
    clearAtomConstraints();
    try {
      if (bs != null && bs.isEmpty())
        return "";
      SymmetryInterface uc = vwr.getOperativeSymmetry();
      if (uc == null)
        uc = vwr.getSymTemp()
            .setUnitCell(new float[] { 10, 10, 10, 90, 90, 90 }, false);
      // limit the atoms to 
      BS bsAtoms = vwr.getThisModelAtoms();
      BS bsCell = (isP1 ? bsAtoms : SV.getBitSet(vwr.evaluateExpressionAsVariable("{within(unitcell)}"), true));
      if (bs == null) {
        bs = bsAtoms;
      }
      if (bs != null) {
        bsAtoms.and(bs);
        if (!isP1)
          bsAtoms.and(bsCell);
      }
      boolean noAtoms = bsAtoms.isEmpty();
      int mi = (noAtoms ? 0 : vwr.ms.at[bsAtoms.nextSetBit(0)].getModelIndex());
      T3 m = uc.getUnitCellMultiplier();
      if (m != null && m.z == 1) {
        m.z = 0;
      }
      P3 supercell;
      P3[] oabc;
      String ita;
      BS basis;
      @SuppressWarnings("unchecked")
      Map<String, Object> sg = (noAtoms || isP1 ? null
          : (Map<String, Object>) vwr.findSpaceGroup(bsAtoms, null, false));
      if (sg == null) {
        name = "P1";
        supercell = P3.new3(1, 1, 1);
        oabc = uc.getUnitCellVectors();
        ita = "1";
        basis = null;
      } else {
        supercell = (P3) sg.get("supercell");
        oabc = (P3[]) sg.get("unitcell");
        name = (String) sg.get("name");
        ita = (String) sg.get("itaFull");
        basis = (BS) sg.get("basis");
      }
      uc.getUnitCell(oabc,  false, null);
      uc.setSpaceGroupTo(ita);
      uc.setSpaceGroupName(name);
      if (basis == null)
        basis = uc.removeDuplicates(vwr.ms, bsAtoms);
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

  /**
   * Delete all atoms that are equivalent to this atom.
   * 
   * @param bs
   * @return number of deleted atoms
   */
  public int cmdAssignDeleteAtoms(BS bs) {
    clearAtomConstraints();
    bs.and(vwr.getThisModelAtoms());
    bs = vwr.ms.getSymmetryEquivAtoms(bs);
    if (!bs.isEmpty())
      vwr.deleteAtoms(bs, false);
    return bs.cardinality();
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
    vwr.runScript(script);
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

  //// from CmdExt
  
  /**
   * A versatile method that allows changing element, setting charge, setting
   * position, adding or deleting an atom by clicking or dragging or via the MODELKIT
   * ASSIGN ATOM command.
   * 
   * @param atomIndex may be -1
   * @param pt a Cartesian position for a new atom or when moving an atom to a new position 
   * @param type one of: an element symbol, "X" (delete), "Mi" (decrement charge), "Pl" (increment charge), "." (from connect; just adding hydrogens)
   * @param cmd reference command given; may be null
   * @param isClick if this is a user-generated click event
   * 
   */
  public void cmdAssignAtom(int atomIndex, P3 pt, String type, String cmd,
                            boolean isClick) {
    // single atom - clicked or from ASSIGN or MODELKIT ASSIGN command
    BS bs = (atomIndex < 0 ? null : BSUtil.newAndSetBit(atomIndex));
    assignAtoms(pt, (pt != null), -1, type, cmd, isClick, bs, 0, 0, null, null, null);
  }

  /**
   * Change element, charge, and deleting an atom by clicking on it or via the
   * MODELKIT ASSIGN ATOM command.
   * 
   * null n bs    ASSIGN ATOM @1 "N"
   * 
   * pt -1 null   ASSIGN ATOM "N" {x,y,z}
   * 
   * pt -1 bs     ADD ATOM @1 "N" {x,y,z}
   * 
   * 
   * 
   * 
   * 
   * @param pt
   * @param type
   * @param cmd
   * @param isClick
   * @param bs
   * @param atomicNo
   * @param site
   * @param uc
   *        a SymmetryInterface or null
   * @param points
   * @param packing
   */
  private void assignAtoms(P3 pt, boolean newPoint, int atomIndex, String type, String cmd,
                            boolean isClick,
                           // strictly internal, for crystal work:
                           BS bs, int atomicNo, int site, SymmetryInterface uc,
                           Lst<P3> points, String packing) {
    boolean haveAtom = (atomIndex >= 0);
    if (bs == null)
      bs = new BS();
    int nIgnored = 0;
    int np = 0;
    if (!haveAtom)
      atomIndex = bs.nextSetBit(0);
    Atom atom = (atomIndex < 0 ? null : vwr.ms.at[atomIndex]);
    float bd = (pt != null && atom != null ? pt.distance(atom) : -1);
    if (points != null) {
      np = nIgnored = points.size();
      uc.toFractional(pt, true);
      points.addLast(pt);
      if (newPoint && haveAtom)
        nIgnored++;
      uc.getEquivPointList(points, nIgnored, packing + (newPoint && atomIndex < 0 ? "newpt" : ""));
    }
    BS bsEquiv = (atom == null ? null : vwr.ms.getSymmetryEquivAtoms(bs));
    BS bs0 = BSUtil.copy(bsEquiv);
    int mi = (atom == null ? vwr.am.cmi : atom.mi);
    int ac = vwr.ms.ac;
    int state = getMKState();
    boolean isDelete = type.equals("X");
    boolean isXtal = (vwr.getOperativeSymmetry() != null);
    try {
      if (isDelete) {
        if (isClick) {
          vwr.setModelKitRotateBondIndex(-1);
        }
        getConstraint(null, atomIndex, GET_DELETE);
      }
      if (pt == null && points == null) {
        // no new position
        // just assigning a charge, deleting an atom, or changing its element 
        if (atom == null)
          return;
        vwr.sm.setStatusStructureModified(atomIndex, mi, 1, cmd, 1, bsEquiv);
        assignAtom(atomIndex, type, autoBond, !isXtal, true, bsEquiv);
        if (!PT.isOneOf(type, ";Mi;Pl;X;"))
          vwr.ms.setAtomNamesAndNumbers(atomIndex, -ac, null, true);
        vwr.sm.setStatusStructureModified(atomIndex, mi, -1, "OK", 1, bsEquiv);
        vwr.refresh(Viewer.REFRESH_SYNC_MASK, "assignAtom");
        return;
      }

      // have pt or points -- assumes at most a SINGLE atom here
      // if pt != null, then it is what needs to be duplicated
      // if bs != null, then we have atoms to connect as well 

      setMKState(STATE_MOLECULAR);

      // set up the pts array for 

      P3[] pts;
      if (points == null) {
        pts = new P3[] { pt };
      } else {
        pts = new P3[Math.max(0, points.size() - np)];
        for (int i = pts.length; --i >= 0;) {
          pts[i] = points.get(np + i);
        }
      }

      // connections list for the new atoms

      Lst<Atom> vConnections = new Lst<Atom>();
      boolean isConnected = false;

      if (site == 0) {

        // set the site to last-site + 1 if this is not an atom
        // and set up the connections

        if (atom != null) {
          if (bs.cardinality() <= 1) {
            vConnections.addLast(atom);
            isConnected = true;
          } else if (uc != null) {
            P3 p = P3.newP(atom);
            uc.toFractional(p, true);
            bs.or(bsEquiv);
            Lst<P3> list = uc.getEquivPoints(null, p, packing);
            for (int j = 0, n = list.size(); j < n; j++) {
              for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
                if (vwr.ms.at[i].distanceSquared(list.get(j)) < 0.001f) {
                  vConnections.addLast(vwr.ms.at[i]);
                  bs.clear(i);
                }
              }
            }
          }
          isConnected = (vConnections.size() == pts.length);
          if (isConnected) {
            float d = Float.MAX_VALUE;
            for (int i = pts.length; --i >= 0;) {
              float d1 = vConnections.get(i).distance(pts[i]);
              if (d == Float.MAX_VALUE)
                d1 = d;
              else if (Math.abs(d1 - d) > 0.001f) {
                // this did not work
                isConnected = false;
                break;
              }
            }
          }
          if (!isConnected) {
            vConnections.clear();
          }
          vwr.sm.setStatusStructureModified(atomIndex, mi, 3, cmd, 1, null);
        }
        if (pt != null || points != null) {
          BS bsM = vwr.getThisModelAtoms();
          for (int i = bsM.nextSetBit(0); i >= 0; i = bsM.nextSetBit(i + 1)) {
            int as = vwr.ms.at[i].getAtomSite();
            if (as > site)
              site = as;
          }
          site++;
        }
      }

      // save globals
      int pickingMode = vwr.acm.getAtomPickingMode();
      boolean wasHidden = menu.hidden;
      boolean isMK = vwr.getBoolean(T.modelkitmode);
      if (!isMK) {
        vwr.setBooleanProperty("modelkitmode", true);
        menu.hidden = true;
        menu.allowPopup = false;
      }

      // now add the hydrogens
      Map<String, Object> htParams = new Hashtable<String, Object>();
      if (site > 0)
        htParams.put("fixedSite", Integer.valueOf(site));
//      // TODO idea not implemented
//      if (type != null)
//        htParams.put("fixedElement", type);
      bs = vwr.addHydrogensInline(bs, vConnections, pts, htParams);
      if (bd > 0 && !isConnected && vConnections.isEmpty()) {
        appRunScript("connect " + (bd - 0.1f) + " " + (bd + 0.01f) + " " + bs0 + " " + bs);
      }

      // bs now points to the new atoms

      // restore globals
      if (!isMK) {
        vwr.setBooleanProperty("modelkitmode", false);
        menu.hidden = wasHidden;
        menu.allowPopup = true;
        vwr.acm.setPickingMode(pickingMode);
        menu.hidePopup();
      }

      int atomIndexNew = bs.nextSetBit(0);
      if (points == null) {
        // new single atom
        assignAtom(atomIndexNew, type, false, atomIndex >= 0 && !isXtal, true,
            null);
        if (atomIndex >= 0)
          assignAtom(atomIndex, ".", false, !isXtal, isClick, null);
        vwr.ms.setAtomNamesAndNumbers(atomIndexNew, -ac, null, true);
        vwr.sm.setStatusStructureModified(atomIndexNew, mi, -3, "OK", 1, bs);
        return;
      }
      // potentially many new atoms here, from MODELKIT ADD 
      if (atomIndexNew >= 0) {
        for (int i = atomIndexNew; i >= 0; i = bs.nextSetBit(i + 1)) {
          assignAtom(i, type, false, false, true, null);
          vwr.ms.setSite(vwr.ms.at[i], site, true);
        }
        vwr.ms.updateBasisFromSite(atomIndexNew);
      }
      int firstAtom = vwr.ms.am[mi].firstAtomIndex;
      if (atomicNo >= 0) {
        atomicNo = Elements.elementNumberFromSymbol(type, true);
        BS bsM = vwr.getThisModelAtoms();
        for (int i = bsM.nextSetBit(0); i >= 0; i = bsM.nextSetBit(i + 1)) {
          if (vwr.ms.at[i].getAtomSite() == site)
            vwr.ms.setElement(vwr.ms.at[i], atomicNo, true);
        }
      }
      vwr.ms.setAtomNamesAndNumbers(firstAtom, -ac, null, true);
      vwr.sm.setStatusStructureModified(-1, mi, -3, "OK", 1, bs);
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      setMKState(state);
    }
  }

  public void cmdAssignBond(int bondIndex, char type, String cmd) {
    
    // from CmdExt
    
    int modelIndex = -1;
    int state = getMKState();
    try {
      setMKState(STATE_MOLECULAR);
      if (type == '-')
        type = pickBondAssignType;
      Atom a1 = vwr.ms.bo[bondIndex].atom1;
      modelIndex = a1.mi;
      int ac = vwr.ms.ac;
      vwr.sm.setStatusStructureModified(bondIndex, modelIndex, Viewer.MODIFY_MAKE_BOND,
          cmd, 1, null);
      BS bsAtoms = assignBond(bondIndex, type);
      vwr.ms.setAtomNamesAndNumbers(a1.i, -ac, null, true);
      if (bsAtoms == null || type == '0')
        vwr.refresh(Viewer.REFRESH_SYNC_MASK, "setBondOrder");      
      vwr.sm.setStatusStructureModified(bondIndex, modelIndex, -Viewer.MODIFY_MAKE_BOND, "" + type, 1, null);
    } catch (Exception ex) {
      Logger.error("assignBond failed");
      vwr.sm.setStatusStructureModified(bondIndex, modelIndex, -2, "ERROR " + ex, 1, null);
    } finally {
      setMKState(state);
    }
  }

  public void cmdAssignConnect(int index, int index2, char type, String cmd) {
    // TODO: connect equivalent atoms?
    // from CmdExt
    int state = getMKState();
    try {
      float[][] connections = AU.newFloat2(1);
      connections[0] = new float[] { index, index2 };
      int modelIndex = vwr.ms.at[index].mi;
      vwr.sm.setStatusStructureModified(index, modelIndex, Viewer.MODIFY_MAKE_BOND, cmd, 1, null);
      vwr.ms.connect(connections);
      int ac = vwr.ms.ac;
      // note that vwr.ms changes during the assignAtom command 
      assignAtom(index, ".", true, true, false, null);
      assignAtom(index2, ".", true, true, false, null);
      vwr.ms.setAtomNamesAndNumbers(index, -ac, null, true);
      vwr.sm.setStatusStructureModified(index, modelIndex, -Viewer.MODIFY_MAKE_BOND, "OK", 1, null);
      if (type != '1') {
        BS bs = BSUtil.newAndSetBit(index);
        bs.set(index2);
        bs = vwr.getBondsForSelectedAtoms(bs);
        int bondIndex = bs.nextSetBit(0);
        cmdAssignBond(bondIndex, type, cmd);
      }
      vwr.refresh(Viewer.REFRESH_SYNC_MASK, "assignConnect");
    } catch (Exception e) {
      // ignore?
    } finally {
      setMKState(state);
    }
  }

  public void assignAtomClick(int atomIndex, String element, P3 ptNew) {

    // from Mouse -- run it through the MODELKIT ASSIGN ATOM command
    
    
    appRunScript("modelkit "
        + (vwr.getOperativeSymmetry() == null ?  "assign atom" : "ADD")
        + " ({" + atomIndex + "}) \"" + element + "\" "
          + (ptNew == null ? "" : Escape.eP(ptNew)) + " true");
  }


  /**
   * MODELKIT ADD @3 ...
   * 
   * @param type
   * @param pt the new point
   * @param bsAtoms the atoms to process, presumably from different sites
   * @param packing  "packed" or ""
   * @param cmd the command generating this call
   * @param isClick 
   * @return the number of atoms added
   */
  public int cmdAssignAddAtoms(String type, P3 pt, BS bsAtoms, String packing,
                               String cmd, boolean isClick) {
    try {
      vwr.pushHoldRepaintWhy("modelkit");
      boolean isPoint = (bsAtoms == null);
      boolean isConnected = (pt != null && !isPoint);
      int atomIndex = (isPoint ? -1 : bsAtoms.nextSetBit(0));
      if (!isPoint && atomIndex < 0)
        return 0;
      SymmetryInterface uc = vwr.getOperativeSymmetry();
      if (uc == null) {
        if (isPoint)
          assignAtoms(pt, true, -1, type, cmd, false, null, 1, -1, null, null, "");
        return (isPoint ? 1 : 0);
      }
      BS bsM = vwr.getThisModelAtoms();
      int n = bsM.cardinality();
      if (n == 0)
        packing = "zapped;" + packing;
      String stype = "" + type;
      P3 pf = null;
      isPoint = (pt != null);
      if (isPoint) {
        pf = P3.newP(pt);
        uc.toFractional(pf, true);
      }
      Lst<P3> list = new Lst<P3>();
      int atomicNo = -1;
      int site = 0;
      for (int i = bsM.nextSetBit(0); i >= 0; i = bsM.nextSetBit(i + 1)) {
          P3 p = P3.newP(vwr.ms.at[i]);
          uc.toFractional(p, true);
          if (pf != null && pf.distanceSquared(p) < JC.UC_TOLERANCE2) {
            site = vwr.ms.at[i].getAtomSite();
            if (type == null)
              type = vwr.ms.at[i].getElementSymbolIso(true);
          }
          list.addLast(p);
      }
      int nIgnored = list.size();
      packing = "fromfractional;tocartesian;" + packing;
      if (type != null)
        atomicNo = Elements.elementNumberFromSymbol(type, true);
      if (isPoint) {
        // new atom, but connected to an current atom (multiple versions
        BS bsEquiv = (bsAtoms == null ? null : vwr.ms.getSymmetryEquivAtoms(bsAtoms));
        assignAtoms(P3.newP(pt), true, atomIndex, stype, null, false, bsEquiv, atomicNo, site, uc,
            list, packing);
      } else {
        // not a new point
        BS sites = new BS();
        for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms
            .nextSetBit(i + 1)) {
          Atom a = vwr.ms.at[i];
          site = a.getAtomSite();
          if (sites.get(site))
            continue;
          sites.set(site);
          stype = (type == null ? a.getElementSymbolIso(true) : stype);
          assignAtoms(P3.newP(a), false, -1, stype, null, false, null, atomicNo, site, uc,
              list, packing);
          for (int j = list.size(); --j >= nIgnored;)
            list.removeItemAt(j);
        }
      }
      if (isClick) {
        vwr.setPickingMode("dragAtom", 0);
      }
      n = vwr.getThisModelAtoms().cardinality() - n;
      return n;
    } catch (Exception e) {
      e.printStackTrace();
      return 0;
    } finally {
      vwr.popHoldRepaint("modelkit");
    }
  }


  /**
   * Move all atoms that are equivalent to this atom PROVIDED that they have the
   * same symmetry-invariant properties.
   * 
   * @param iatom
   *        atom index
   * @param p
   *        new position for this atom, which may be modified
   * @return number of atoms moved
   */
  public int cmdAssignMoveAtom(int iatom, P3 p) { 
    BS bsFixed = vwr.getMotionFixedAtoms();
    BS bs = new BS();
    boolean checkOcc = false;
    // pick up occupancies
    Atom a = vwr.ms.at[iatom];
    if (!checkOcc || vwr.ms.getOccupancyFloat(a.i) == 100) {
      bs.set(a.i);
    } else { 
      vwr.getAtomsNearPt(0.0001f, a, bs);
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        // passing over another atom
        if (vwr.ms.getOccupancyFloat(i) == 100)
          bs.clear(i);
      }
    }
    int n = 0;
    boolean isOccSet = (bs.cardinality() > 1);
    if ((n = constrain(iatom, p, bsFixed, !isOccSet)) == 0 || Float.isNaN(p.x) || !isOccSet)
      return n;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      iatom = (constraint == null ? vwr.ms.getBasisAtom(i).i : i);
      n += assignMoveAtom(iatom, p, null);
    }
    return n;
  }

  public int assignMoveAtom(int iatom, P3 pt, BS bsFixed) {
    // check to see if a constraint has stopped this changae
    if (Float.isNaN(pt.x) || iatom < 0)
      return 0;
    if (iatom < 0)
      return 0;
    // check that this is an atom in the current model set.
    // must be an atom in the current model set
    BS bs = BSUtil.newAndSetBit(iatom);
    bs.and(vwr.getThisModelAtoms());
    if (bs.isEmpty())
      return 0;
    int state = getMKState();
    setMKState(STATE_MOLECULAR);
    try {
      // check for locked atoms
      BS bseq = vwr.ms.getSymmetryEquivAtoms(bs);
      SymmetryInterface sg = vwr.getCurrentUnitCell();
      if (getConstraint(sg, bseq.nextSetBit(0), GET_CREATE).type == Constraint.TYPE_LOCKED) {
        return 0;
      }
      if (bsFixed != null)
        bseq.andNot(bsFixed);
      int n = bseq.cardinality();
      if (n == 0) {
        return 0;
      }
      // checking here that the new point has not moved to a special position
      Atom a = vwr.ms.at[iatom];
      int[] v0 = sg.getInvariantSymops(a, null);
      int[] v1 = sg.getInvariantSymops(pt, v0);
      if ((v1 == null) != (v0 == null) || !Arrays.equals(v0, v1))
        return 0;
      P3[] points = new P3[n];
      // If this next call fails, then we have a serious problem. 
      // An operator was not found for one of the atoms that transforms it
      // into its presumed symmetry-equivalent atom
      int ia0 = bseq.nextSetBit(0);
      if (!fillPointsForMove(sg, bseq, ia0, a, pt, points)) {
        return 0;
      }
      int mi = vwr.ms.at[ia0].mi;
      vwr.sm.setStatusStructureModified(ia0, mi, Viewer.MODIFY_SET_COORD,
          "dragatom", n, bseq);
      for (int k = 0, ia = bseq.nextSetBit(0); ia >= 0; ia = bseq
          .nextSetBit(ia + 1)) {
        P3 p = points[k++];
        vwr.ms.setAtomCoord(ia, p.x, p.y, p.z);
      }
      vwr.sm.setStatusStructureModified(ia0, mi, -Viewer.MODIFY_SET_COORD,
          "dragatom", n, bseq);
      return n;
    } finally {
      setMKState(state);
    }
  }
  
  /**
   * Find the operator that transforms fractional point fa to one of its
   * symmetry-equivalent points, and then also transform pt by that same matrix.
   * Optionally, save the transformed points in a compact array.
   * 
   * @param sg
   * @param bseq
   * @param i0
   * @param a
   * @param pt
   * @param points
   * @return false if there is a failure to find a transform
   */
  private boolean fillPointsForMove(SymmetryInterface sg, BS bseq, int i0,
                                    P3 a, P3 pt, P3[] points) {
    float d = a.distance(pt);
    P3 fa = P3.newP(a);
    P3 fb = P3.newP(pt);
    sg.toFractional(fa, true);
    sg.toFractional(fb, true);
    for (int k = 0, i = i0; i >= 0; i = bseq.nextSetBit(i + 1)) {
      P3 p = P3.newP(vwr.ms.at[i]);
      sg.toFractional(p, true);
      M4 m = sg.getTransform(fa, p, false);
      if (m == null) {
//        m = sg.getTransform(fa, p, true);
        return false;
      }
      P3 p2 = P3.newP(fb);
      m.rotTrans(p2);
      sg.toCartesian(p2, true);
      if (Math.abs(d - vwr.ms.at[i].distance(p2)) > 0.001f)
        return false;
      points[k++] = p2;
    }
    fa.setT(points[0]);
    sg.toFractional(fa, true);
    // check for sure that all new positions are also OK
    for (int k = points.length; --k >= 0;) {
      fb.setT(points[k]);
      sg.toFractional(fb, true);
      M4 m = sg.getTransform(fa, fb, false);
      if (m == null) {
//        m = sg.getTransform(fa, fb, true);
        return false;
      }
      for (int i = points.length; --i > k;) {
        if (points[i].distance(points[k]) < 0.1f)
          return false;
      }
    }
    return true;
  }

  public void clearAtomConstraints() {
    if (atomConstraints != null) {
      for (int i = atomConstraints.length; --i >= 0;)
        atomConstraints[i] = null;        
    }
  }
  
  public boolean hasConstraint(int iatom, boolean ignoreGeneral, boolean addNew) {
    Constraint c = getConstraint(vwr.getOperativeSymmetry(), iatom, addNew ? GET_CREATE : GET); 
    return (c != null && (!ignoreGeneral || c.type != Constraint.TYPE_GENERAL));
  }

  /**
   * This is the main method from viewer.moveSelected.
   * 
   * @param iatom
   * @param ptNew
   * @param bsFixed
   * @param doAssign allow for exit with setting ptNew but not creating atoms
   * @return number of atoms moved
   */
  public int constrain(int iatom, P3 ptNew, BS bsFixed, boolean doAssign) {
    int n = 0;
    SymmetryInterface sym;
    if (iatom < 0 || (sym = vwr.getOperativeSymmetry()) == null) {
      // molecular crystals loaded without packed or centroid will not have operations
      return 0;
    }
    if (bsFixed != null)
      bsFixed = vwr.getMotionFixedAtoms();
    Atom a = vwr.ms.at[iatom];
    Constraint c = constraint;
    if (c == null) {
      c = getConstraint(sym, iatom, GET_CREATE);
      if (c.type == Constraint.TYPE_LOCKED) {
        iatom = -1;
      } else {
        // transform the shift to the basis
        Atom b = vwr.ms.getBasisAtom(iatom);
        P3 fa = P3.newP(a);
        sym.toFractional(fa, true);
        P3 fb = P3.newP(b);
        sym.toFractional(fb, true);
        M4 m = sym.getTransform(fa, fb, true);
        if (m == null) {
          System.err.println(
              "ModelKit - null matrix for " + iatom + " " + a + " to " + b);
          iatom = -1;
        } else {
          sym.toFractional(ptNew, true);
          m.rotTrans(ptNew);
          sym.toCartesian(ptNew, true);
          c.constrain(b, ptNew);
          iatom = b.i;
        }
      }
    } else {
      c.constrain(vwr.ms.at[iatom], ptNew);
    }
    if (iatom >= 0 && !Float.isNaN(ptNew.x)) {
      if (!doAssign)
        return 1;
      n = assignMoveAtom(iatom, ptNew, null);
    }
    ptNew.x = Float.NaN; // indicate handled
    return n;
  }

  private Constraint[] atomConstraints;

  private static int GET = 0;
  private static int GET_CREATE = 1;
  private static int GET_DELETE = 2;
  
  /**
   * This constraint will be set for the site only.
   * 
   * @param sym 
   * @param ia 
   * @param mode GET, GET_CREATE, or GET_DELETE
   * @return a Constraint, or possibly null if not createNew
   */
  private Constraint getConstraint(SymmetryInterface sym, int ia, int mode) {
    if (ia < 0)
      return null;
    Atom a = vwr.ms.getBasisAtom(ia);
    int iatom = a.i;
    Constraint ac = (atomConstraints != null && iatom < atomConstraints.length ? atomConstraints[iatom] : null);
    if (ac != null || mode != GET_CREATE) {
      if (ac != null && mode == GET_DELETE) {
        atomConstraints[iatom] = null;
      }
      return ac;
    }
    if (sym == null)
      return addConstraint(iatom, new Constraint(a, Constraint.TYPE_NONE, null));
    // the first atom is the site atom
    int[] ops = sym.getInvariantSymops(a, null);
    // if no invariant operators, this is a general position
    if (ops.length == 0)
      return addConstraint(iatom, new Constraint(a, Constraint.TYPE_GENERAL, null));
    // we need only work with the first plane or line or point
    P4 plane1 = null;
    Object[] line1 = null;
    for (int i = ops.length; --i >= 0;) {
      Object[] line2 = null;
      Object c = sym.getSymmetryInfoAtom(vwr.ms, iatom, null, ops[i], null, a, null, "invariant", T.array, 0, -1, 0);
      if (c instanceof String) {
        // this would be a translation
        return locked;
      } else if (c instanceof P4) {
        // check plane - first is the constraint; second is almost(?) certainly not parallel
        P4 plane = (P4) c;
        if (plane1 == null) {
          plane1 = plane;
          continue;
        }
        // note that the planes cannot be parallel, because a point cannot be
        // invariant on two parallel planes.
        Lst<Object> line = Measure.getIntersectionPP(plane1, plane);
          if (line == null || line.size() == 0) {
            // not possible?
            return locked;
          }
          line2 = new Object[] { line.get(0), line.get(1) };
      } else if (c instanceof P3) {
        return locked;
      } else {        
        // check line
        // [p3, p3]
        line2 = (Object[]) c;
      }
      if (line2 != null) {
        // add the intersection
        if (line1 == null) {
          line1 = line2;
        } else {
          T3 v1 = (T3) line1[1];
          if (Math.abs(v1.dot((T3) line2[1])) < 0.999f)
            return locked;
//          V3 v = V3.newVsub((T3) line1[0], (T3) line2[0]);
//          if (v.lengthSquared() != 0 && Math.abs(v.dot(v1)) > 0.999f)
//            return locked;
        }
      }
    }
    return addConstraint(iatom, plane1 != null ? new Constraint(a, Constraint.TYPE_PLANE, new Object[] { plane1 })
        : line1 != null ? new Constraint(a, Constraint.TYPE_VECTOR, line1)
            :  new Constraint(a, Constraint.TYPE_GENERAL, null));
  }

  private Constraint addConstraint(int iatom, Constraint c) {
    if (c == null) {
      if (atomConstraints != null && atomConstraints.length > iatom) {
        atomConstraints[iatom] = null;
      }
      return null;
    }

    if (atomConstraints == null) {
      atomConstraints = new Constraint[vwr.ms.ac + 10];
    } 
    if (atomConstraints.length < iatom + 10) {
      Constraint[] a = new Constraint[vwr.ms.ac + 10];
      System.arraycopy(atomConstraints, 0, a, 0, atomConstraints.length);
      atomConstraints = a;
    }
    return atomConstraints[iatom] = c;
  }

  public void addLockedAtoms(BS bs) {
    SymmetryInterface sg = vwr.getOperativeSymmetry();
    if (sg == null)
      return;
    BS bsm = vwr.getThisModelAtoms();
    for (int i = bsm.nextSetBit(0); i >= 0; i = bsm.nextSetBit(i + 1)) {
      if (getConstraint(sg, i, GET_CREATE).type == Constraint.TYPE_LOCKED) {
        bs.set(i);
      }
    }
  }


}
