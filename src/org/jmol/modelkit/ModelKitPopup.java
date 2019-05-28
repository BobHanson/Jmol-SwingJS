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

import java.awt.Component;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.jmol.api.PlatformViewer;
import org.jmol.i18n.GT;
import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.Bond;
import org.jmol.modelset.ModelSet;
import org.jmol.popup.AwtSwingComponent;
import org.jmol.popup.AwtSwingPopupHelper;
import org.jmol.popup.JmolGenericPopup;
import org.jmol.popup.PopupResource;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.Edge;
import org.jmol.util.Elements;
import org.jmol.util.Logger;
import org.jmol.viewer.ActionManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.awt.SC;
import javajs.util.BS;
import javajs.util.P3;
import javajs.util.V3;

public class ModelKitPopup extends JmolGenericPopup {

  private boolean hasUnitCell;
  private int state;

  public ModelKitPopup() {
    helper = new AwtSwingPopupHelper(this);
  }
  
  @Override
  public void jpiInitialize(PlatformViewer vwr, String menu) {
    updateMode = UPDATE_NEVER;
    boolean doTranslate = GT.setDoTranslate(true);
    PopupResource bundle = new ModelKitPopupResourceBundle(null, null);
    initialize((Viewer) vwr, bundle, bundle.getMenuName());
    GT.setDoTranslate(doTranslate);
  }
  
  @Override
  public void jpiUpdateComputedMenus() {
    hasUnitCell = vwr.getCurrentUnitCell() != null;
    SC menu = htMenus.get("xtalMenu");
    menu.setEnabled(hasUnitCell);
  }


  @Override
  protected void menuShowPopup(SC popup, int x, int y) {
    try {
      ((JPopupMenu)((AwtSwingComponent)popup).jc).show((Component) vwr.display, x, y);
    } catch (Exception e) {
      // ignore
    }
  }
  
  @Override
  public String menuSetCheckBoxOption(SC item, String name, String what, boolean TF) {
    if (name.startsWith("mk")) {
      int pt = name.indexOf("??");
      name = name.substring(2, pt);
      jpiSetProperty(name.substring(pt + 2), Boolean.valueOf(TF));
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

  @Override
  public void menuClickCallback(SC source, String script) {
    if (script.equals("clearQ")) {
      for (SC item : htCheckbox.values()) {
        if (item.getActionCommand().indexOf(":??") < 0)
          continue;        
        menuSetLabel(item, "??");
        item.setActionCommand("_??P!:");
        item.setSelected(false);
        //item.setArmed(false);
      }
      script = "set picking assignAtom_C";
    }
    super.menuClickCallback(source, script);  
  }

  @Override
  protected Object getImageIcon(String fileName) {
    String imageName = "org/jmol/modelkit/images/" + fileName;
    URL imageUrl = this.getClass().getClassLoader().getResource(imageName);
    return (imageUrl == null ? null : new ImageIcon(imageUrl));
  }

  @Override
  public void menuFocusCallback(String name, String actionCommand, boolean b) {
    // n/a
  }

  // xtal model kit only
  
  public final static int STATE_BITS_VIEWEDIT   = 0b00000011;
  public final static int STATE_NOVIEWEDIT      = 0b00000000;
  public final static int STATE_VIEW            = 0b00000001;
  public final static int STATE_EDIT            = 0b00000010;
  
  public final static int STATE_BITS_SYM        = 0b00011100;
  public final static int STATE_SYM_NONE        = 0b00000000;
  public final static int STATE_SYM_UNITIZE     = 0b00001100;
  public final static int STATE_SYM_APPLYLOCAL  = 0b00000100; // edit only
  public final static int STATE_SYM_RETAINLOCAL = 0b00001000; // edit only
  public final static int STATE_SYM_APPLYFULL   = 0b00010000;

  public final static int STATE_BITS_PACKING    = 0b01100000;
  public final static int STATE_PACK_UC         = 0b00100000;
  public final static int STATE_PACK_EXTEND     = 0b01000000;

  public P3 centerAtom, sphereAtom, offset;
  
  public double coreDistance;
  
  public String symop;

  private boolean addhydrogens = true;
  private int centerAtomIndex = -1, atomIndexSphere = -1;  

  
  private boolean isXtalState() {
    return ((state & STATE_BITS_VIEWEDIT) != 0);
  }

  private void setViewEdit(int bits) {
    state = (state & ~STATE_BITS_VIEWEDIT) | bits;
  }

  private int getViewState() {
    return state & STATE_BITS_VIEWEDIT;
  }

  private void setSym(int bits) {
    state = (state & ~STATE_BITS_SYM) | bits;
  }

  private int getSymState() {
    return state & STATE_BITS_SYM;
  }

  private void setPacking(int bits) {
    state = (state & ~STATE_BITS_PACKING) | bits;
  }

//  { "xtalSymmetryMenu", "mknoSymmetry??P!RD mkretainLocal??P!RD mkapplyLocal??P!RD mkapplyFull??P!RD" },
//  { "xtalPackingMenu", "mkextendCell??P!RD mkpackCell??P!RD" },
//  { "xtalOptionsMenu", "mkallAtoms??P!RD mkasymmetricUnit??P!RD mkallowElementReplacement??P!CB" }


  @Override
  public Object jpiSetProperty(String name, Object value) {
    name = name.toLowerCase().intern();
    System.out.println("ModelKitPopup " + name + "=" + value + " " + this);
    if (name == "noviewedit") {
      setViewEdit(STATE_NOVIEWEDIT);
      return null;
    }
    if (name == "view") {
      setViewEdit(STATE_VIEW);
      return null;
    }
    if (name == "edit") {
      setViewEdit(STATE_EDIT);
      return null;
    }

    if (name == "symop") {
      symop = (String) value;
      showXtalSymmetry();
      return null;
    }
    if (name == "atom1") {
      centerAtom = (P3) value;
      centerAtomIndex = (centerAtom instanceof Atom ? ((Atom) centerAtom).i : -1);
      atomIndexSphere = -1;
      return null;
    }
    if (name == "atom2") {
      sphereAtom = (P3) value;
      atomIndexSphere = (sphereAtom instanceof Atom ? ((Atom) sphereAtom).i : -1);
      return null;
    }
    if (name == "offset") {
      offset = (P3) value;
      showXtalSymmetry();
      return null;
    }
    
    if (name == "nosymmetry") {
      setSym(STATE_SYM_NONE);
      showXtalSymmetry();
      return null;
    }
    if (name == "applylocal") {
      setSym(STATE_SYM_APPLYLOCAL);
      return null;
    }
    if (name == "retainlocal") {
      setSym(STATE_SYM_RETAINLOCAL);
    }
    if (name == "applyfull") {
      setSym(STATE_SYM_APPLYFULL);
      return null;
    }

    if (name == "pack") {
      setPacking(STATE_PACK_UC);
      return null;
    }
    if (name == "extend") {
      setPacking(STATE_PACK_EXTEND);
      return null;
    }
    if (name == "addhydrogen" || name == "addhydrogens") {
      addhydrogens = (value == Boolean.TRUE);
      System.out.println("ad " + addhydrogens + " " + value + " " + value.getClass().getName());
      return null;
    }

    if (name == "assignatom") {
      Object[] o = ((Object[]) value);
      String type = (String) o[0];
      int[] data = (int[]) o[1];
      if (!processXtalState(data[0]))
        assignAtom(data[0], type, data[1] >= 0, data[2] >= 0);
      return null;
    }
    

    if (name == "assignbond") {
      int[] data = (int[]) value;
      return assignBond(data[0],  data[1]);
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

  private boolean processXtalState(int index) {
    switch (getViewState()) {
    case STATE_VIEW:
      centerAtomIndex = index;
      showXtalSymmetry();
      return true;
    case STATE_EDIT:
      if (index == centerAtomIndex)
        return true;
      // TODO do distance measure here.
      return false;
    default:
      return false;
    }
  }

  private void showXtalSymmetry() {
    String symop = this.symop;
    String script = null;
    switch (getSymState()) {
    case STATE_SYM_NONE:
      script = "draw delete";
      break;
    case STATE_SYM_UNITIZE:
      symop = vwr.getSymop(centerAtomIndex, symop, "unit");
      break;
    default:
      if (offset != null)
        symop = vwr.getSymop(centerAtomIndex, symop, offset);
    }
    if (script == null)
      script = "draw ID sym symop '" + symop + "' {atomindex=" + centerAtomIndex + "}";
    vwr.script(script);    
  }

  public void assignAtom(int atomIndex, String type, boolean autoBond, boolean addHsAndBond) {
    
    
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
    if (addhydrogens)
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
    if (type != '0' && addhydrogens )
      vwr.addHydrogens(bsAtoms, false, true);
    return bsAtoms;
  }
  

}
