/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2018-02-22 12:04:47 -0600 (Thu, 22 Feb 2018) $
 * $Revision: 21841 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development
 *
 * Contact: jmol-developers@lists.sf.net, jmol-developers@lists.sf.net
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

import org.jmol.api.JmolSelectionListener;
import org.jmol.c.PAL;
import org.jmol.i18n.GT;
import org.jmol.modelset.ModelSet;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Escape;

import javajs.util.AU;
import javajs.util.BS;

public class SelectionManager {


  public BS bsSubset;
  public BS bsDeleted;
  /**
   * a flag to indicate that labels and fonts should be set to default values;
   * set only by SELECT NONE;
   */
  public Boolean noneSelected;

  final BS bsHidden = new BS();
  final BS bsFixed = new BS();

  private Viewer vwr;
  private JmolSelectionListener[] listeners;
  private final BS bsSelection = new BS();

  SelectionManager(Viewer vwr) {
    this.vwr = vwr;
    listeners = new JmolSelectionListener[0];
    colorer = new SelectionColorer(vwr);
    addListener(colorer);
  }

  void processDeletedModelAtoms(BS bsAtoms) {
    BSUtil.deleteBits(bsDeleted, bsAtoms);
    BSUtil.deleteBits(bsSubset, bsAtoms);
    BSUtil.deleteBits(bsFixed, bsAtoms);
    BSUtil.deleteBits(bsHidden, bsAtoms);
    BS bs = BSUtil.copy(bsSelection);
    BSUtil.deleteBits(bs, bsAtoms);
    setSelectionSet(bs, 0);
    selectionChanged(false);
  }

  // this is a tri-state. the value -1 means unknown
  private final static int TRUE = 1;
  private final static int FALSE = 0;
  private final static int UNKNOWN = -1;
  private int empty = TRUE;

  boolean hideNotSelected;

  void clear() {
    clearSelection(true);
    setSelectionSubset(null);
    hide(null, null, 0, true);
    bsDeleted = null;
    setMotionFixedAtoms(null);
  }

  void display(ModelSet modelSet, BS bs, int addRemove, boolean isQuiet) {
    switch (addRemove) {
    default:
      BS bsNotSubset = (bsSubset == null ? null
          : BSUtil.andNot(BSUtil.copy(bsHidden), bsSubset));
      BS bsAll = modelSet.getModelAtomBitSetIncludingDeleted(-1, false);
      bsHidden.or(bsAll);
      if (bsNotSubset != null) {
        bsHidden.and(bsSubset);
        bsHidden.or(bsNotSubset);
      }
      //$FALL-THROUGH$
    case T.add:
      if (bs != null)
        bsHidden.andNot(bs);
      break;
    case T.remove:
      if (bs != null)
        bsHidden.or(bs);
      break;
    }
    BSUtil.andNot(bsHidden, bsDeleted);
    modelSet.setBsHidden(bsHidden);
    if (!isQuiet)
      vwr.reportSelection(
          GT.i(GT.$("{0} atoms hidden"), bsHidden.cardinality()));
  }

  void hide(ModelSet modelSet, BS bs, int addRemove, boolean isQuiet) {
    BS bsNotSubset = (addRemove == 0 || bsSubset == null ? null
        : BSUtil.andNot(BSUtil.copy(bsHidden), bsSubset));
    setBitSet(bsHidden, bs, addRemove);
    if (bsNotSubset != null)
      bsHidden.or(bsNotSubset);
    if (modelSet != null)
      modelSet.setBsHidden(bsHidden);
    if (!isQuiet)
      vwr.reportSelection(
          GT.i(GT.$("{0} atoms hidden"), bsHidden.cardinality()));
  }

  void setSelectionSet(BS set, int addRemove) {
    setBitSet(bsSelection, set, addRemove);
    empty = UNKNOWN;
  }

  private static void setBitSet(BS bsWhat, BS bs, int addRemove) {
    switch (addRemove) {
    default:
      bsWhat.clearAll();
      //$FALL-THROUGH$
    case T.add:
      if (bs != null)
        bsWhat.or(bs);
      break;
    case T.remove:
      if (bs != null)
        bsWhat.andNot(bs);
      break;
    }
  }

  public BS getHiddenSet() {
    return bsHidden;
  }

  boolean getHideNotSelected() {
    return hideNotSelected;
  }

  void setHideNotSelected(boolean TF) {
    hideNotSelected = TF;
    if (TF)
      selectionChanged(false);
  }

  public boolean isSelected(int atomIndex) {
    return (atomIndex >= 0 && bsSelection.get(atomIndex));
  }

  void select(BS bs, int addRemove, boolean isQuiet) {
    if (bs == null) {
      selectAll(true);
      if (!vwr.getBoolean(T.hydrogen))
        excludeSelectionSet(vwr.ms.getAtoms(T.hydrogen, null));
      if (!vwr.getBoolean(T.hetero))
        excludeSelectionSet(vwr.ms.getAtoms(T.hetero, null));
    } else {
      setSelectionSet(bs, addRemove);
      if (!vwr.getBoolean(T.hydrogen))
        excludeSelectionSet(vwr.ms.getAtoms(T.hydrogen, null));
      if (!vwr.getBoolean(T.hetero))
        excludeSelectionSet(vwr.ms.getAtoms(T.hetero, null));

    }
    selectionChanged(false);
    boolean reportChime = vwr.getBoolean(T.messagestylechime);
    if (!reportChime && isQuiet)
      return;
    int n = getSelectionCount();
    if (reportChime)
      vwr.getChimeMessenger().reportSelection(n);
    else if (!isQuiet)
      vwr.reportSelection(GT.i(GT.$("{0} atoms selected"), n));
  }

  void selectAll(boolean isQuiet) {
    int count = vwr.ms.ac;
    empty = (count == 0) ? TRUE : FALSE;
    for (int i = count; --i >= 0;)
      bsSelection.set(i);
    BSUtil.andNot(bsSelection, bsDeleted);
    selectionChanged(isQuiet);
  }

  void clearSelection(boolean isQuiet) {
    setHideNotSelected(false);
    bsSelection.clearAll();
    empty = TRUE;
    selectionChanged(isQuiet);
  }

  public boolean isAtomSelected(int atomIndex) {
    return (
        (bsSubset == null || bsSubset.get(atomIndex))
        && bsDeleted == null || !bsDeleted.get(atomIndex))
        && bsSelection.get(atomIndex);
  }

  public void setSelectedAtom(int atomIndex, boolean TF) {
    if (atomIndex < 0) {
      selectionChanged(true);
      return;
    }
    if (bsSubset != null && !bsSubset.get(atomIndex)
        || bsDeleted != null && bsDeleted.get(atomIndex))
      return;
    bsSelection.setBitTo(atomIndex, TF);
    if (TF)
      empty = FALSE;
    else
      empty = UNKNOWN;
  }

  public void setSelectionSubset(BS bs) {
    bsSubset = bs;
  }

  boolean isInSelectionSubset(int atomIndex) {
    return (atomIndex < 0
        || vwr.am.splitFrame && !vwr.am.isSplitFrameSelectable(atomIndex)
        || bsSubset == null 
        || bsSubset.get(atomIndex));
  }

  void invertSelection() {
    BSUtil.invertInPlace(bsSelection, vwr.ms.ac);
    empty = (bsSelection.length() > 0 ? FALSE : TRUE);
    selectionChanged(false);
  }

  private void excludeSelectionSet(BS setExclude) {
    if (setExclude == null || empty == TRUE)
      return;
    bsSelection.andNot(setExclude);
    empty = UNKNOWN;
  }

  private final BS bsTemp = new BS();

  public int getSelectionCount() {
    if (empty == TRUE)
      return 0;
    empty = TRUE;
    BS bs;
    if (bsSubset == null) {
      bs = bsSelection;
    } else {
      bsTemp.clearAll();
      bsTemp.or(bsSubset);
      bsTemp.and(bsSelection);
      bs = bsTemp;
    }
    int count = bs.cardinality();
    if (count > 0)
      empty = FALSE;
    return count;
  }

  void addListener(JmolSelectionListener listener) {
    for (int i = listeners.length; --i >= 0;)
      if (listeners[i] == listener) {
        listeners[i] = null;
        break;
      }
    int len = listeners.length;
    for (int i = len; --i >= 0;)
      if (listeners[i] == null) {
        listeners[i] = listener;
        return;
      }
    if (listeners.length == 0)
      listeners = new JmolSelectionListener[1];
    else
      listeners = (JmolSelectionListener[]) AU.doubleLength(listeners);
    listeners[len] = listener;
  }

  private void selectionChanged(boolean isQuiet) {
    if (hideNotSelected)
      hide(vwr.ms, BSUtil.copyInvert(bsSelection, vwr.ms.ac), 0, isQuiet);
    if (isQuiet || listeners.length == 0)
      return;
    for (int i = listeners.length; --i >= 0;)
      if (listeners[i] != null)
        listeners[i].selectionChanged(bsSelection);
  }

  int deleteAtoms(BS bs) {
    BS bsNew = BSUtil.copy(bs);
    if (bsDeleted == null) {
      bsDeleted = bsNew;
    } else {
      bsNew.andNot(bsDeleted);
      bsDeleted.or(bs);
    }
    bsHidden.andNot(bsDeleted);
    bsSelection.andNot(bsDeleted);
    return bsNew.cardinality();
  }

  /**
   * @param asCopy 
   * @return bsSelection or a subset of that
   */
  BS getSelectedAtoms(boolean asCopy) {
    BS bs = (asCopy || bsSubset != null ? BSUtil.copy(bsSelection) : bsSelection);
    if (bsSubset == null)
      return bs;
    bs.and(bsSubset);
    return bs;
  }

  BS getSelectedAtomsNoSubset() {
    return BSUtil.copy(bsSelection);
  }

  public BS excludeAtoms(BS bs, boolean ignoreSubset) {
    if (bsDeleted != null)
      bs.andNot(bsDeleted);
    if (!ignoreSubset && bsSubset != null)
      (bs = BSUtil.copy(bs)).and(bsSubset);
    return bs;
  }

  void setMotionFixedAtoms(BS bs) {
    bsFixed.clearAll();
    if (bs != null)
      bsFixed.or(bs);
  }

  public BS getMotionFixedAtoms() {
    return bsFixed;
  }

  private SelectionColorer colorer;

  private static class SelectionColorer implements JmolSelectionListener {

    protected boolean enabled;

    private Object colorSelected;
    private Object colorUnselected;
    private BS bsColorSelected, bsColorUnselected;
    private String translucency;
    private Double translucentLevel;
    private Viewer vwr;

    protected SelectionColorer(Viewer vwr) {
      this.vwr = vwr;
    }

    /**
     * only if not disabled by
     * NULL NULL
     */
    @Override
    public void selectionChanged(BS selection) {
      if (!enabled)
        return;
      BS bsAll = vwr.getAllAtoms();      
      BSUtil.copy2(selection, bsColorSelected);
      BSUtil.copy2(bsAll, bsColorUnselected);
      bsColorUnselected.andNot(bsColorSelected);
      vwr.shm.setShapePropertyBs(JC.SHAPE_BALLS, "color", colorSelected,
          bsColorSelected);
      vwr.shm.setShapePropertyBs(JC.SHAPE_BALLS, "color", colorUnselected,
          bsColorUnselected);
      if (translucency != null) {
        vwr.shm.setShapePropertyBs(JC.SHAPE_BALLS, "translucency", translucency,
            bsColorUnselected);
        vwr.shm.setShapePropertyBs(JC.SHAPE_BALLS, "translucentLevel",
            translucentLevel, bsColorUnselected);
      }
    }

    private String colorString(Object c, boolean isTranslucent) {
      // note that this does not report translucency
      if (c == null || c == PAL.NONE)
        return "NULL";
      int ic = C.getShadedRGB(((Integer) c).intValue(), (isTranslucent ? translucentLevel.doubleValue() : 0));
      return "#" + Escape.getHexColorFromRGB(ic);
    }

    void set(Integer colorSelected, Integer colorUnselected,
                    String translucency, double translucentLevel) {
      if (bsColorSelected == null) {
        bsColorSelected = new BS();
        bsColorUnselected = new BS();
      }
      this.colorSelected = (colorSelected == null ? PAL.NONE : colorSelected);
      this.colorUnselected = (colorUnselected == null ? PAL.NONE
          : colorUnselected);
      if (translucentLevel == Double.MAX_VALUE) {
        this.translucency = null;
      } else {
        this.translucency = translucency;
        this.translucentLevel = Double.valueOf(translucentLevel);
      }
      enabled = true;
    }

    @Override
    public String toString() {
      return colorString(colorSelected, false) + " "
          + colorString(colorUnselected, translucency != null);

    }

  }

  public void setSelectionColors(Integer colorSelected, Integer colorUnselected,
                                 String translucency, double translucentLevel) {
    colorer.set(colorSelected, colorUnselected, translucency, translucentLevel);
    vwr.selectStatus(getSelectedAtoms(true), false, T.nada, false, true);
    colorer.enabled = (colorSelected != null || colorUnselected != null);
  }

  public String getSelectionColors() {
    return (colorer.enabled ? colorer.toString() : "NULL NULL");
  }

}
