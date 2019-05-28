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

import java.net.URL;
import java.util.Map.Entry;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import org.jmol.api.PlatformViewer;
import org.jmol.i18n.GT;
import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.Bond;
import org.jmol.modelset.ModelSet;
import org.jmol.popup.AwtSwingComponent;
import org.jmol.popup.GenericSwingPopup;
import org.jmol.popup.JmolGenericPopup;
import org.jmol.popup.JmolSwingPopup;
import org.jmol.popup.PopupResource;
import org.jmol.script.ScriptException;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.Edge;
import org.jmol.util.Elements;
import org.jmol.util.Logger;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.awt.SC;
import javajs.util.BS;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.V3;

abstract public class ModelKitPopup extends JmolSwingPopup {

  protected boolean hasUnitCell;
 
  public ModelKitPopup() {
    System.out.println("hmm");
  }
  
 
  @Override
  protected PopupResource getBundle(String menu) {
    return new ModelKitPopupResourceBundle(null, null);
  }
 
  public void initializeForModel() {
    initializeBondRotation();
    ms = null;
    allOperators = null;
    modelIndex = -999;
  }

  private ModelSet ms;
  String[] allOperators;
  private int modelIndex = -1;
  
  @Override
  public void jpiUpdateComputedMenus() {
    hasUnitCell = vwr.getCurrentUnitCell() != null;
    if (!checkUpdateSymmetryInfo())
      updateAllXtalMenus();
  }

  @Override
  public String menuSetCheckBoxOption(SC item, String name, String what, boolean TF) {
    if (name.startsWith("mk")) {
      int pt = name.indexOf("??");
      name = name.substring(2, pt);
      pt = name.indexOf("_");
      if (pt > 0) {
        setProperty(name.substring(0, pt), name.substring(pt + 1));
      } else {
        setProperty(name, Boolean.valueOf(TF));
      }
      return null;
    }
    
    // atom type
    String element = JOptionPane.showInputDialog(GT.$("Element?"), "");
    if (element == null || Elements.elementNumberFromSymbol(element, true) == 0)
      return null;
    menuSetLabel(item, element);
    
    ((AwtSwingComponent) item).setActionCommand("assignAtom_" + element + "P!:??");
    return "set picking assignAtom_" + element;
  }
  
  /**
   * @j2sOverride
   */
  @Override
  public void menuClickCallback(SC source, String script) {
    if (script.equals("clearQ")) {
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
    processClickCallback(source, script);
  }

  @Override
  protected Object getImageIcon(String fileName) {
    String imageName = "org/jmol/modelkit/images/" + fileName;
    URL imageUrl = this.getClass().getClassLoader().getResource(imageName);
    return (imageUrl == null ? null : new ImageIcon(imageUrl));
  }

  // xtal model kit only
  
  public final static int STATE_BITS_XTAL       /* 0b00000000011*/ = 0x03;
  public final static int STATE_MOLECULAR       /* 0b00000000000*/ = 0x00;
  public final static int STATE_XTALVIEW        /* 0b00000000001*/ = 0x01;
  public final static int STATE_XTALEDIT        /* 0b00000000010*/ = 0x02;
  
  public final static int STATE_BITS_SYM_VIEW   /* 0b00000011100*/ = 0x1c;
  public final static int STATE_SYM_NOOFFSET    /* 0b00000000000*/ = 0x00;
  public final static int STATE_SYM_OFFSET      /* 0b00000001000*/ = 0x08;

  public final static int STATE_BITS_SYM_EDIT   /* 0b00011100000*/ = 0xe0;
  public final static int STATE_SYM_APPLYFULL   /* 0b00000000000*/ = 0x00;
  public final static int STATE_SYM_APPLYLOCAL  /* 0b00000100000*/ = 0x20;
  public final static int STATE_SYM_RETAINLOCAL /* 0b00001000000*/ = 0x40;

  public final static int STATE_BITS_UNITCELL    /* 0b11100000000*/ = 0x700; 
  public final static int STATE_UNITCELL_EXTEND  /* 0b00000000000*/ = 0x000;
  public final static int STATE_UNITCELL_PACKED  /* 0b00100000000*/ = 0x100;

  public static final String MODE_OPTIONS     = ";view;edit;molecular;";
  public static final String SYMMETRY_OPTIONS = ";none;applylocal;retainlocal;applyfull;";
  public static final String UNITCELL_OPTIONS = ";packed;extend;";
  public static final String BOOLEAN_OPTIONS  = ";allowelementchange;addhydrogen;addhydrogens;";
  public static final String SET_OPTIONS     = ";element;";
  private static final int MAX_LABEL = 32;

  private int state = STATE_MOLECULAR & STATE_SYM_NOOFFSET & STATE_SYM_APPLYFULL & STATE_UNITCELL_EXTEND; // 0x00

  public P3 centerAtom, sphereAtom, viewOffset;
  
  public double centerDistance;
  
  public Object symop;

  private boolean addHydrogens = true;
  private int centerAtomIndex = -1, atomIndexSphere = -1;
  private boolean clickToSetElement = false;  

  
  private boolean isXtalState() {
    return ((state & STATE_BITS_XTAL) != 0);
  }

  private void setXtalState(int bits) {
    state = (state & ~STATE_BITS_XTAL) | bits;
  }

  private int getXtalState() {
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

  private void setSymView(int bits) {
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

  public synchronized Object setProperty(String name, Object value) {
    name = name.toLowerCase().intern();
    System.out.println("ModelKitPopup " + name + "=" + value + " " + this);

    if (name == "addhydrogen" || name == "addhydrogens") {
      addHydrogens = (value == Boolean.TRUE);
      return null;
    }

    if (name == "clicktosetelement") {
      clickToSetElement = (value == Boolean.TRUE);
    }

    if (name == "mode") { // view, edit, or molecular
      boolean isEdit = ("edit".equals(value));
      setXtalState("view".equals(value) ? STATE_XTALVIEW
          : isEdit ? STATE_XTALEDIT : STATE_MOLECULAR);
      if (isEdit)
        addHydrogens = false;
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
      viewOffset = (isPacked ? new P3() : null);
      return null;
    }

    if (name == "symop") {
      symop = value;
      setDefaultState(STATE_XTALVIEW);
      showXtalSymmetry();
      return null;
    }

    if (name == "center") {
      centerAtom = (P3) value;
      centerAtomIndex = (centerAtom instanceof Atom ? ((Atom) centerAtom).i
          : -1);
      atomIndexSphere = -1;
      processXtalState(centerAtomIndex);
      return null;
    }

    if (name == "offset") {
      viewOffset = (value instanceof P3 ? (P3) value : null);
      setSymView(viewOffset == null ? STATE_SYM_NOOFFSET : STATE_SYM_OFFSET);
      showXtalSymmetry();
      return null;
    }

    if (name == "distance") {
      setDefaultState(STATE_XTALEDIT);
      centerDistance = ((Float) value).doubleValue();
      return null;
    }

    if (name == "point") {
      setDefaultState(STATE_XTALEDIT);
      sphereAtom = (P3) value;
      atomIndexSphere = (sphereAtom instanceof Atom ? ((Atom) sphereAtom).i
          : -1);
      return null;
    }

    if (name == "assignatom") {
      // standard entry point for an atom click in the ModelKit
      Object[] o = ((Object[]) value);
      String type = (String) o[0];
      int[] data = (int[]) o[1];
      int atomIndex = data[0];
      if (!processXtalState(data[0])
          && (clickToSetElement || atomIndex != centerAtomIndex))
        assignAtom(atomIndex, type, data[1] >= 0, data[2] >= 0);
      return null;
    }

    if (name == "assignbond") {
      int[] data = (int[]) value;
      return assignBond(data[0], data[1]);
    }

    if (name == "addConstraint") {
      // TODO
    }

    if (name == "removeConstraint") {
      // TODO
    }

    if (name == "removeAllConstraints") {
      // TODO
    }

    System.err.println("ModelKitPopup.setProperty? " + name + " " + value);

    return null;
  }

  private void setDefaultState(int mode) {
    if (!isXtalState())
      setXtalState(mode);
  }

  /**
   * atom has been clicked
   * 
   * @param index
   * @return true if handled
   */
  private boolean processXtalState(int index) {
    switch (getXtalState()) {
    case STATE_XTALVIEW:
      centerAtomIndex = index;
      showXtalSymmetry();
      return true;
    case STATE_XTALEDIT:
      if (index == centerAtomIndex)
        return true;
      // TODO do distance measure here.
      return false;
    default:
      return false;
    }
  }

  private void showXtalSymmetry() {
    String script = null;

    if (centerAtomIndex < 0)
      return;
    switch (getSymViewState()) {
    case 0:
      script = "draw * delete";
      break;
    case STATE_SYM_OFFSET:
      break;
    default:
      break;
    }
    if (script == null) {
      script = "draw ID sym symop " 
          + (symop instanceof String ? "'" + symop + "'"
              : PT.toJSON(null, symop)) 
          + " {atomindex=" + centerAtomIndex + "}"
          + (viewOffset == null ? "" : " offset " + viewOffset);
    }
    System.out.println("ModelKitPopup script=" + script);
    appRunScript(script);
  }

  
  /////////////// action methods //////////////
  
  
  private String pickAtomAssignType = "C";
  private char pickBondAssignType = 'p';
  private boolean isPickAtomAssignCharge; // pl or mi

  public boolean isPickAtomAssignCharge() {
    return isPickAtomAssignCharge;
  }
  
  public String getAtomPickingType() {
    return pickAtomAssignType;
  }
  
  public char getBondPickingType() {
    return pickBondAssignType;
  }
  
  public void setAtomPickingOption(String option) {
      pickAtomAssignType = option;
      isPickAtomAssignCharge = (option.equals("pl") || option.equals("mi"));
  }

  public void setBondPickingOption(String option) {
      pickBondAssignType = Character.toLowerCase(option.charAt(0));
  }

  private void assignAtom(int atomIndex, String type, boolean autoBond, boolean addHsAndBond) {
        
    vwr.ms.clearDB(atomIndex);
    if (type == null)
      type = "C";

    // not as simple as just defining an atom.
    // if we click on an H, and C is being defined,
    // this sprouts an sp3-carbon at that position.

    Atom atom = vwr.ms.at[atomIndex];
    BS bs = new BS();
    boolean wasH = (atom.getElementNumber() == 1);
    int atomicNumber = Elements.elementNumberFromSymbol(type, true);

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
    } else if (type.equals("Pl")) {
      atom.setFormalCharge(atom.getFormalCharge() + 1);
    } else if (type.equals("Mi")) {
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
      vwr.ms.makeConnections2(0.1f, 1.8f, 1, T.create, bsA, bs, null, false, false, 0);

      // 6) add hydrogen atoms

    }
    if (addHydrogens)
      vwr.addHydrogens(bsA, false, true);
  }

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
    if (type != '0' && addHydrogens )
      vwr.addHydrogens(bsAtoms, false, true);
    return bsAtoms;
  }
  
  private int rotateBondIndex = -1;

  public void setRotateBondIndex(int index) {
    boolean haveBond = (rotateBondIndex >= 0);
    if (!haveBond && index < 0)
      return;
    rotatePrev1 = -1;
    bsRotateBranch = null;
    if (index == Integer.MIN_VALUE)
      return;
    rotateBondIndex = index;
    vwr.highlightBond(index, false);

  }

  public int getRotateBondIndex() {
    return rotateBondIndex;
  }

  private int rotatePrev1 = -1;
  private int rotatePrev2 = -1;
  private BS bsRotateBranch;

  public void actionRotateBond(int deltaX, int deltaY, int x, int y) {
    // called by actionManager
    if (rotateBondIndex < 0)
      return;
    BS bsBranch = bsRotateBranch;
    Atom atom1, atom2;
    ModelSet ms = vwr.ms;
    if (bsBranch == null) {
      Bond b = ms.bo[rotateBondIndex];
      atom1 = b.atom1;
      atom2 = b.atom2;
      vwr.undoMoveActionClear(atom1.i, AtomCollection.TAINT_COORD, true);
      P3 pt = P3.new3(x, y, (atom1.sZ + atom2.sZ) / 2);
      vwr.tm.unTransformPoint(pt, pt);
      if (atom2.getCovalentBondCount() == 1
          || pt.distance(atom1) < pt.distance(atom2)
              && atom1.getCovalentBondCount() != 1) {
        Atom a = atom1;
        atom1 = atom2;
        atom2 = a;
      }
      if (Measure.computeAngleABC(pt, atom1, atom2, true) > 90
          || Measure.computeAngleABC(pt, atom2, atom1, true) > 90) {
        bsBranch = vwr.getBranchBitSet(atom2.i, atom1.i, true);
      }
      if (bsBranch != null)
        for (int n = 0, i = atom1.bonds.length; --i >= 0;) {
          if (bsBranch.get(atom1.getBondedAtomIndex(i)) && ++n == 2) {
            bsBranch = null;
            break;
          }
        }
      if (bsBranch == null) {
        bsBranch = ms.getMoleculeBitSetForAtom(atom1.i);
      }
      bsRotateBranch = bsBranch;
      rotatePrev1 = atom1.i;
      rotatePrev2 = atom2.i;
    } else {
      atom1 = ms.at[rotatePrev1];
      atom2 = ms.at[rotatePrev2];
    }
    V3 v1 = V3.new3(atom2.sX - atom1.sX, atom2.sY - atom1.sY, 0);
    V3 v2 = V3.new3(deltaX, deltaY, 0);
    v1.cross(v1, v2);
    float degrees = (v1.z > 0 ? 1 : -1) * v2.length();

    BS bs = BSUtil.copy(bsBranch);
    bs.andNot(vwr.slm.getMotionFixedAtoms());
    vwr.rotateAboutPointsInternal(null, atom1, atom2, 0, degrees, false, bs, null,
        null, null, null);
  }

  public void initializeBondRotation() {
    bsRotateBranch = null;
    rotatePrev1 = rotateBondIndex = -1;
  }
  
  /////////// building xtal menu //////////////////
  
  private boolean checkUpdateSymmetryInfo() {
    htMenus.get("xtalMenu").setEnabled(hasUnitCell);
    if (!hasUnitCell) {
      ms = null;
      allOperators = null;
      modelIndex = -1;
      return true;
    }
    boolean isOK = true;
    if (vwr.ms != ms) {
      ms = vwr.ms;
      isOK = false;      
    } else if (modelIndex == -1 || modelIndex != vwr.am.cmi){
      isOK = false;      
      modelIndex = vwr.am.cmi;
    }
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
    SB sb = new SB();
    try {
      vwr.eval.runScriptBuffer("show symop", sb, true);
    } catch (ScriptException e) {
      // ignore
    }
    allOperators = PT.split(sb.toString().trim(), "\n");
    addAllCheckboxItems(htMenus.get("xtalOpMenu"), allOperators);
  }

  private void addAllCheckboxItems(SC menu, String[] labels) {
    menuRemoveAll(menu, 0);
    if (labels.length <= MAX_LABEL) {
      for (int i = 0; i < allOperators.length; i++) {
        String id = "mkop" + i;
        String basename = id;
        String entry = allOperators[i];
        menuCreateCheckboxItem(menu, entry, basename, id, false, false);
      }
    } else {

    }

  }

  private void updateAllXtalMenuOptions() {
    for(Entry<String, SC> entry: htMenus.entrySet()) {
      String key = entry.getKey();
      
      if (key.startsWith("mk")) {
        
        
        
        
      }
      
    }
  }

  @Override
  protected void appUpdateForShow() {
    // n/a
  }

  @Override
  protected void appUpdateSpecialCheckBoxValue(SC source, String actionCommand,
                                               boolean selected) {
    // n/a?
  }


}
