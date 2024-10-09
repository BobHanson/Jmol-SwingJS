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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.jmol.api.JmolScriptEvaluator;
import org.jmol.api.SymmetryInterface;
import org.jmol.i18n.GT;
import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.Bond;
import org.jmol.modelset.MeasurementPending;
import org.jmol.modelset.ModelSet;
import org.jmol.script.ScriptEval;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.Edge;
import org.jmol.util.Elements;
import org.jmol.util.Font;
import org.jmol.util.Logger;
import org.jmol.util.SimpleUnitCell;
import org.jmol.util.Vibration;
import org.jmol.viewer.ActionManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.MouseState;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.M3d;
import javajs.util.M4d;
import javajs.util.MeasureD;
import javajs.util.P3d;
import javajs.util.P4d;
import javajs.util.PT;
import javajs.util.Qd;
import javajs.util.SB;
import javajs.util.T3d;
import javajs.util.V3d;

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

    final static int TYPE_NONE = 0;
    //    public final static int TYPE_DISTANCE = 1; // not implemented
    //    public final static int TYPE_ANGLE    = 2; // not implemented
    //    public final static int TYPE_DIHEDRAL = 3; // not implemented
    final static int TYPE_VECTOR = 4;
    final static int TYPE_PLANE = 5;
    final static int TYPE_LOCKED = 6;
    final static int TYPE_GENERAL = 7;

    int type;

    private P3d pt;
    private P3d offset;
    private P4d plane;
    private V3d unitVector;

    //    // not used to date
    //    private P3d[] points;
    //    private double value;

    Constraint(P3d pt, int type, Object[] params)
        throws IllegalArgumentException {
      this.pt = pt;
      this.type = type;
      switch (type) {
      case TYPE_NONE:
      case TYPE_GENERAL:
      case TYPE_LOCKED:
        break;
      case TYPE_VECTOR:
        offset = (P3d) params[0];
        unitVector = V3d.newV((T3d) params[1]);
        unitVector.normalize();
        break;
      case TYPE_PLANE:
        plane = (P4d) params[0];
        break;
      //      case TYPE_SYMMETRY:
      //        symop = (String) params[0];
      //        points = new P3d[1];
      //        offset = (P3d) params[1];
      //        break;
      //      case TYPE_DISTANCE:
      //        // not implemented
      //        value = ((Double) params[0]).doubleValue();
      //        points = new P3d[] { (P3d) params[1], null };
      //        break;
      //      case TYPE_ANGLE:
      //        // not implemented
      //        value = ((Double) params[0]).doubleValue();
      //        points = new P3d[] { (P3d) params[1], (P3d) params[2], null };
      //        break;
      //      case TYPE_DIHEDRAL:
      //        // not implemented
      //        value = ((Double) params[0]).doubleValue();
      //        points = new P3d[] { (P3d) params[1], (P3d) params[2], (P3d) params[3], null };
      //        break;
      default:
        throw new IllegalArgumentException();
      }
    }

    /**
     * 
     * @param ptOld
     * @param ptNew
     *        new point, possibly with x set to NaN
     * @param allowProjection
     *        if false: this is just a test of the atom already being on the
     *        element; in which case ptNew.x will be set to NaN if the test
     *        fails; if true: this is not a test and if the setting is allowed,
     *        set ptNew.x to NaN.
     * 
     */
    void constrain(P3d ptOld, P3d ptNew, boolean allowProjection) {
      V3d v = new V3d();
      P3d p = P3d.newP(ptOld);
      double d = 0;
      switch (type) {
      case TYPE_NONE:
        return;
      case TYPE_GENERAL:
        return;
      case TYPE_LOCKED:
        ptNew.x = Double.NaN;
        return;
      case TYPE_VECTOR:
        if (pt == null) { // generic constraint 
          d = MeasureD.projectOntoAxis(p, offset, unitVector, v);
          if (d * d >= JC.UC_TOLERANCE2) {
            ptNew.x = Double.NaN;
            break;
          }
        }
        d = MeasureD.projectOntoAxis(ptNew, offset, unitVector, v);
        break;
      //      case TYPE_LATTICE_FACE:
      case TYPE_PLANE:
        if (pt == null) { // generic constraint 
          if (Math.abs(MeasureD.getPlaneProjection(p, plane, v, v)) > 0.01d) {
            ptNew.x = Double.NaN;
            break;
          }
        }
        d = MeasureD.getPlaneProjection(ptNew, plane, v, v);
        ptNew.setT(v);
        break;
      }
      if (!allowProjection && Math.abs(d) > 1e-10) {
        ptNew.x = Double.NaN;
      }
    }

  }

  private abstract static class Key {
    
    ModelKit mk;
    int nAtoms;
    protected int modelIndex;
    protected BS bsAtoms;

    protected Key(ModelKit mk, int modelIndex) {
      this.mk = mk;
      bsAtoms = mk.vwr.getModelUndeletedAtomsBitSet(modelIndex);
      nAtoms = (bsAtoms == null ? 0 : bsAtoms.cardinality());
      this.modelIndex = modelIndex;
    }

    protected void setVisibilityFlags() {
      BS bs = mk.vwr.getVisibleFramesBitSet();
      mk.vwr.shm.getShape(JC.SHAPE_DRAW).setModelVisibilityFlags(bs);
      mk.vwr.shm.getShape(JC.SHAPE_ECHO).setModelVisibilityFlags(bs);
    }

    protected void createDrawEcho(int x, int y, String key, String label, int color,
                                Font font) {
      label = PT.replaceAllCharacters(label, "\"'\n\r\t", "_");
      String text = label;
      if (text.startsWith("~_"))
        text = text.substring(2);
      mk.vwr.shm.setShapeProperties(JC.SHAPE_DRAW,
          new Object[] { "init", "elementKey" },
          new Object[] { "thisID", key + "d_" + label },
          new Object[] { "diameter", Double.valueOf(2) },
          new Object[] { "modelIndex", Integer.valueOf(modelIndex) },
          new Object[] { "points", Integer.valueOf(0) },
          new Object[] { "coord", P3d.new3(x, y, -Double.MAX_VALUE) },
          new Object[] { "set", null }, 
          new Object[] { "color", Integer.valueOf(color) },
          new Object[] { "thisID", null });
      mk.vwr.shm.setShapeProperties(JC.SHAPE_ECHO,
          new Object[] { "thisID", null },
          new Object[] { "target", key + "e_" + label },
          new Object[] { "model", Integer.valueOf(modelIndex) },
          new Object[] { "xypos", P3d.new3(x+1, y-1, -Double.MAX_VALUE) },
          new Object[] { "text", text }, 
          new Object[] { "font", font },
          new Object[] { "color", Integer.valueOf(JC.COLOR_CONTRAST) },
          new Object[] { "thisID", null });
    }

    protected static char isKeyOn(ModelKit mk, int modelIndex, char type) {
      Object[] data = new Object[] { getKey(modelIndex, type) + "*", null };
      mk.vwr.shm.getShapePropertyData(JC.SHAPE_ECHO, "checkID", data);
      if (data[1] == null)
        return '\0';
      String key = (String) data[0];
      return (key.indexOf("_E_") >= 0 ? 'E' : key.indexOf("_L_") >= 0 ? 'L' : '\0');
    }

    /**
     * Triggered by a MODELKIT OFF in a state script, set there by StateCreator
     * when there is an ECHO for _!_elkey*. Just checks for DRAW objects and sets
     * haveElementKeys and bsElementKeyModels appropriately.
     * @param mk 
     */
    protected static void updateKeyFromStateScript(ModelKit mk) {
      for (int i = mk.vwr.ms.mc; --i >= 0;) {
        if (isKeyOn(mk, i, '\0') != '\0') {
          mk.bsKeyModels.set(i);
          mk.haveKeys = true;
        }
      }
    }

    /**
     * Create an element or label key for the specified model or all models. In the case of
     * a specific model, only create the key if it does not yet exist in
     * elementKeyModelList.
     * @param mk 
     * 
     * @param modelIndex
     *        the specified model, or -1 to recreate or delete all keys
     * 
     * @param isOn
     */
    protected static void setKey(ModelKit mk, int modelIndex, boolean isOn) {
      if (isOn && (modelIndex >= 0 && mk.bsKeyModels.get(modelIndex)))
        return;
      clearKey(mk, modelIndex);
      if (!isOn || modelIndex < 0)
        return;
      switch (mk.keyType) {
      case 'E':
        new EKey(mk, modelIndex).draw();
        break;
      case 'L':
        new LKey(mk, modelIndex).draw();
        break;
      default:
        mk.haveKeys = false;
        return;
      }
      mk.bsKeyModels.set(modelIndex);
      mk.haveKeys = true;
    }
    
    /**
     * Deletes the DRAW object for this or all models and adjusts haveKeys
     * appropriately.
     * 
     * @param mk 
     * 
     * @param modelIndex
     *        -1 for all models
     */
    protected static void clearKey(ModelKit mk, int modelIndex) {
      if (!mk.haveKeys)
        return;
      String key = getKey(modelIndex, '\0') + "*";
      Object[][] val = new Object[][] { { "thisID", key }, { "delete", null } };
      mk.vwr.shm.setShapeProperties(JC.SHAPE_DRAW, val);
      mk.vwr.shm.setShapeProperties(JC.SHAPE_ECHO, val);
      switch (modelIndex) {
      case -2:
        break;
      case -1:
        mk.bsKeyModels.clearAll();
        break;
      default:
        mk.bsKeyModels.clear(modelIndex);
        break;
      }
      mk.haveKeys = !mk.bsKeyModels.isEmpty();
    }

    /**
     * Element count, symbol, or color has changed for one or more models.
     * Recreate the element key provided it already exists.
     * @param mk 
     * 
     * @param bsAtoms
     */
    protected static void updateKey(ModelKit mk, BS bsAtoms) {
      if (bsAtoms == null) {
        updateModelKey(mk, mk.vwr.am.cmi, true);
        return;
      }
      if (bsAtoms.cardinality() == 1) {
        updateModelKey(mk, mk.vwr.ms.at[bsAtoms.nextSetBit(0)].mi, true);
        return;
      }
      for (int i = mk.vwr.ms.mc; --i >= 0;) {
        if (mk.vwr.ms.am[i].bsAtoms.intersects(bsAtoms)) {
          updateModelKey(mk, i, true);
        }
      }
    }

    /**
     * Update the element keys for the given models, possibly forcing new keys if
     * none exist yet.
     * @param mk 
     * 
     * @param bsModels
     * @param forceNew
     *        for example, when we have a frame resize
     */
    protected static void updateModelKeys(ModelKit mk, BS bsModels, boolean forceNew) {
      if (bsModels == null)
        bsModels = BSUtil.newBitSet2(0, mk.vwr.ms.mc);
      for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels
          .nextSetBit(i + 1)) {
        updateModelKey(mk, i, forceNew);
      }
    }

    /**
     * Only set a model's element key if it is already on or if SET ELEMENTKEYS ON
     * has been issued. "ON" is defined as "present as a draw object"
     * @param mk 
     * 
     * @param modelIndex
     *        the specified model; modelIndex < 0 is ignored
     * @param forceNew
     */
    protected static void updateModelKey(ModelKit mk, int modelIndex, boolean forceNew) {
      if (doUpdateKey(mk, modelIndex)) {
        if (forceNew)
          clearKey(mk, modelIndex);
        setKey(mk, modelIndex, true);
      }
    }

    protected static boolean doUpdateKey(ModelKit mk, int modelIndex) {
      return modelIndex >= 0 //
          && !mk.vwr.ms.isJmolDataFrameForModel(modelIndex) //
          && !mk.bsKeyModelsOFF.get(modelIndex) //
          && (mk.keyType != '\0' 
              || mk.bsKeyModels.get(modelIndex)
              || (mk.keyType = isKeyOn(mk, modelIndex, '\0')) != '\0');
    }

    /**
     * Turn on or off automatic all-model element keys, from
     * 
     * SET ELEMENTKEYS ON/OFF
     * 
     * ON here will also clear all bsElementKeyModelsOFF overrides.
     * @param mk 
     *
     * @param isOn
     */
    protected static void setKeys(ModelKit mk, boolean isOn) {
      if (isOn) {
        clearKeysOFF(mk);
      }
      clearKey(mk, -1);
      if (isOn) {
        // false here because we have already cleared all already
        updateModelKeys(mk, null, false);
      }
    }

    private static void clearKeysOFF(ModelKit mk) {
      mk.bsKeyModelsOFF.clearAll();
    }



  }
  /**
   * A class to use just temporarily to create an element key for a model.
   */
  private static class EKey extends Key {
    BS bsElements = new BS();
    String[][] elementStrings = new String[120][10];
    int[][] colors = new int[120][10];
    int[] isotopeCounts = new int[120];
    
    EKey(ModelKit mk, int modelIndex) {
      super(mk, modelIndex);
      if (nAtoms == 0)
        return;
      Atom[] a = mk.vwr.ms.at;
      for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms
          .nextSetBit(i + 1)) {
        String elem = a[i].getElementSymbol();
        int elemno = a[i].getElementNumber();
        int color = a[i].atomPropertyInt(T.color);
        int j = 0;
        int niso = isotopeCounts[elemno];
        for (; j < niso; j++) {
          if (elementStrings[elemno][j].equals(elem)) {
            if (colors[elemno][j] != color) {
              // different colors for the same element and isotope
              nAtoms = 0;
              return;
            }
            break;
          }
        }
        if (j < niso) {
          continue;
        }
        bsElements.set(elemno);
        isotopeCounts[elemno]++;
        elementStrings[elemno][j] = elem;
        colors[elemno][j] = color;
      }
    }

    void draw() {
      if (nAtoms == 0)
        return;
      String key = getKey(modelIndex, 'E');
      int h = mk.vwr.getScreenHeight();
      int f = h / 20;
      int n = bsElements.cardinality();
      int dy = 5;
      if (n * f > 0.9d * h) {
        f = (int) (0.9d * h / n);
        dy = (int) (90d / n);
      }
      Font font = mk.vwr.getFont3D("SansSerif", "Bold", f);
      int x = 98 - 75 * f * 3 / h;
      for (int y = 90, elemno = bsElements.nextSetBit(
          0); elemno >= 0; elemno = bsElements.nextSetBit(elemno + 1)) {
        n = isotopeCounts[elemno];
        if (n == 0)
          continue;
        String[] elem = elementStrings[elemno];
        for (int j = 0; j < n; j++) {
          String label = elem[j];
          int color = colors[elemno][j];
          createDrawEcho(x, y, key, label, color, font);
          y -= dy;
        }
      }
      setVisibilityFlags();
    }

  }

  /**
   * A class to use just temporarily to create a label key for a model.
   */
  private static class LKey extends Key {
    TreeMap<String, Integer> labels;
    
    LKey(ModelKit mk, int modelIndex) {
       super(mk, modelIndex);
      if (nAtoms == 0)
        return;
      Atom[] a = mk.vwr.ms.at;
      labels = new TreeMap<>();
      Lst<Integer> colorList = new Lst<>();
      for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms
          .nextSetBit(i + 1)) {
        String label = (String) mk.vwr.shm.getShapePropertyIndex(JC.SHAPE_LABELS, "label", i);
        if (label == null) {
          // any missing label results in no label assignments
          nAtoms = 0;
          break;
        }
        if (label.length() == 0)
          continue;
        if (label.charAt(0) < 'a')
          label = "~_" + label;

        int color = a[i].atomPropertyInt(T.color);  
        Integer iColor = labels.get(label);
        if (iColor == null) {
          iColor = Integer.valueOf(color);
          if (colorList.contains(iColor)) {
            nAtoms = 0;
            break;
          }
          colorList.addLast(iColor);
        } else if (iColor.intValue() != color) {
          nAtoms = 0;
          break;
        }          
        labels.put(label, iColor);
      }
      if (nAtoms == 0) {
        labels = null;
      }
    }

    void draw() {
      if (nAtoms == 0)
        return;
      String key = getKey(modelIndex, 'L');
      int h = mk.vwr.getScreenHeight();
      int f = h * 20 / 400;
      int n = labels.size();
      int dy = 5;
      if (n * f > 0.9d * h) {
        f = (int) (0.9d * h / n);
        dy = (int) (90d / n);
      }
      Iterator<String> ik = labels.keySet().iterator();
      int wmax = 0;
      while (ik.hasNext()) {
        String label = ik.next();
        int w = label.length();
        if (label.startsWith("~_"))
          w -= 2;
        if (w > wmax)
          wmax = w;
      }
      int x = 98 - 75 * f * wmax / h;

      Font font = mk.vwr.getFont3D("SansSerif", "Bold", f);
      Iterator<Entry<String, Integer>> it = labels.entrySet().iterator();
      for (int y = 90; it.hasNext();) {
        Entry<String, Integer> e = it.next();
        String label = e.getKey();
        int color = e.getValue().intValue();
        createDrawEcho(x, y, key, label, color, font);
        y -= dy;
      }
      setVisibilityFlags();
    }
    
  }
  
  private static class WyckoffModulation extends Vibration {

    private final static int wyckoffFactor = 10;

    static void setVibrationMode(ModelKit mk, Object value) {
      Atom[] atoms = mk.vwr.ms.at;
      BS bsAtoms = mk.vwr.getThisModelAtoms();
      if (("off").equals(value)) {
        // remove all WyckoffModulations
        for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms
            .nextSetBit(i + 1)) {
          Vibration v = atoms[i].getVibrationVector();
          if (v != null && v.modDim != Vibration.TYPE_WYCKOFF)
            continue;
          mk.vwr.ms.setVibrationVector(i, ((WyckoffModulation) v).oldVib);
        }
      } else if (("wyckoff").equals(value)) {
        for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms
            .nextSetBit(i + 1)) {
          Vibration v = atoms[i].getVibrationVector();
          if (v != null && v.modDim != Vibration.TYPE_WYCKOFF)
            continue;
          SymmetryInterface sym = mk.getSym(i);
          WyckoffModulation wv = null;
          if (sym != null) {
            Constraint c = mk.setConstraint(sym, i, GET_CREATE);
            if (c.type != Constraint.TYPE_LOCKED)
              wv = new WyckoffModulation(sym, c, atoms[i], mk.getBasisAtom(i));
          }
          mk.vwr.ms.setVibrationVector(i, wv);
        }
      }
      mk.vwr.setVibrationPeriod(Double.NaN);
    }

    private Vibration oldVib;
    private Atom atom = null;

    private double t;
    private Atom baseAtom = null;

    private P3d pt0 = new P3d();
    private P3d ptf = new P3d();

    private SymmetryInterface sym;

    private Constraint c;

    private WyckoffModulation(SymmetryInterface sym, Constraint c, Atom atom,
        Atom baseAtom) {
      setType(Vibration.TYPE_WYCKOFF);
      this.sym = sym;
      this.c = c;
      this.atom = atom;
      this.baseAtom = baseAtom;
      this.x = 1; // just a placeholder to trigger a non-zero vibration is present
    }

    @Override
    public T3d setCalcPoint(T3d pt, T3d t456, double scale,
                            double modulationScale) {
      // one reset per cycle, only for the base atoms
      Vibration v = baseAtom.getVibrationVector();
      if (v == null || v.modDim != TYPE_WYCKOFF)
        return pt;
      WyckoffModulation wv = ((WyckoffModulation) v);
      if (sym == null)
        return pt;
      M4d m = null;
      if (wv.atom != atom) {
        m = getTransform(sym, wv.atom, atom);
        if (m == null)
          return pt;
      }
      if (wv.t != t456.x && ((int) (t456.x * 10)) % 2 == 0) {
        if (c.type != Constraint.TYPE_LOCKED) {
          wv.setPos(sym, c, scale);
        }
        wv.t = t456.x;
      }
      if (m == null)
        pt.setT(wv.ptf);
      else
        m.rotTrans2(wv.ptf, pt);
      sym.toCartesian(pt, false);
      return pt;
    }

    private void setPos(SymmetryInterface sym, Constraint c, double scale) {
      x = (Math.random() - 0.5d) / wyckoffFactor * scale;
      y = (Math.random() - 0.5d) / wyckoffFactor * scale;
      z = (Math.random() - 0.5d) / wyckoffFactor * scale;
      // apply this random walk to the base atom and constrain
      pt0.setT(atom);
      ptf.setT(pt0);
      ptf.add(this);
      c.constrain(pt0, ptf, true);
      sym.toFractional(ptf, false);
    }
  }

  private static Constraint locked = new Constraint(null, Constraint.TYPE_LOCKED, null);
  //private static Constraint none = new Constraint(null, Constraint.TYPE_NONE, null);

  final static int STATE_MOLECULAR /* 0b00000000000*/ = 0x00;

  final static int STATE_XTALVIEW /* 0b00000000001*/ = 0x01;

  ////////////// modelkit state //////////////

  final static int STATE_XTALEDIT /* 0b00000000010*/ = 0x02;
  final static int STATE_BITS_XTAL /* 0b00000000011*/ = 0x03;
  final static int STATE_BITS_SYM_VIEW /* 0b00000011100*/ = 0x1c;
  final static int STATE_SYM_NONE /* 0b00000000000*/ = 0x00;

  final static int STATE_SYM_SHOW /* 0b00000001000*/ = 0x08;
  final static int STATE_BITS_SYM_EDIT /* 0b00011100000*/ = 0xe0;
  final static int STATE_SYM_APPLYLOCAL /* 0b00000100000*/ = 0x20;

  final static int STATE_SYM_RETAINLOCAL /* 0b00001000000*/ = 0x40;
  final static int STATE_SYM_APPLYFULL /* 0b00010000000*/ = 0x80;
  final static int STATE_BITS_UNITCELL /* 0b11100000000*/ = 0x700;
  final static int STATE_UNITCELL_PACKED /* 0b00000000000*/ = 0x000;

  final static int STATE_UNITCELL_EXTEND /* 0b00100000000*/ = 0x100;
  final static String OPTIONS_MODE = "optionsMenu";
  final static String XTAL_MODE = "xtalMenu";

  final static String BOND_MODE = "bondMenu";
  final static String ATOM_MODE = "atomMenu";
  private static final P3d Pt000 = new P3d();

  private static final P4d plane001 = P4d.new4(0, 0, 1, 0);
  
  private static int GET = 0;

  static int GET_CREATE = 1;

  private static int GET_DELETE = 2;

  static String getText(String key) {
    switch (("invSter delAtom dragBon dragAto dragMin dragMol dragMMo incChar decChar "
        // 72..............................................120
        + "rotBond bondTo0 bondTo1 bondTo2 bondTo3 incBond decBond")
            .indexOf(key.substring(0, 7))) {
    case 0:
      return GT.$("invert ring stereochemistry");
    case 8:
      return GT.$("delete atom");
    case 16:
      return GT.$("drag to bond");
    case 24:
      return GT.$("drag atom");
    case 32:
      return GT.$("drag atom (and minimize)");
    case 40:
      return GT.$("drag molecule (ALT to rotate)");
    case 48:
      return GT.$("drag and minimize molecule (docking)");
    case 56:
      return GT.$("increase charge");
    case 64:
      return GT.$("decrease charge");
    case 72:
      return GT.$("rotate bond");
    case 80:
      return GT.$("delete bond");
    case 88:
      return GT.$("single");
    case 96:
      return GT.$("double");
    case 104:
      return GT.$("triple");
    case 112:
      return GT.$("increase order");
    case 120:
      return GT.$("decrease order");
    }
    return key;
  }

  static M4d getTransform(SymmetryInterface sym, Atom a, Atom b) {
    P3d fa = P3d.newP(a);
    sym.toFractional(fa, false);
    P3d fb = P3d.newP(b);
    sym.toFractional(fb, false);
    return sym.getTransform(fa, fb, true);
  }

  protected static String getKey(int modelIndex, char ext) {
    return JC.MODELKIT_ELEMENT_KEY_ID
        + (modelIndex < 0 ? "" : modelIndex + "_")
        + (ext == '\0' ? "" : ext + "_");
  }

  private static boolean isTrue(Object value) {
    return (Boolean.valueOf(value.toString()) == Boolean.TRUE);
  }

  /**
   * Convert key sequence from ActionManager into an element symbol.
   * 
   * Element is (char 1) (char 2) or just (char 1)
   * 
   * @param key
   *        (char 2 << 8) + (char 1), all caps
   * @return valid element symbol or null
   */
  private static String keyToElement(int key) {
    int ch1 = (key & 0xFF);
    int ch2 = (key >> 8) & 0xFF;
    String element = "" + (char) ch1
        + (ch2 == 0 ? "" : ("" + (char) ch2).toLowerCase());
    int n = Elements.elementNumberFromSymbol(element, true);
    return (n == 0 ? null : element);
  }

  private static void notImplemented(String action) {
    System.err.println("ModelKit.notImplemented(" + action + ")");
  }

  private static P3d pointFromTriad(String pos) {
    double[] a = PT.parseDoubleArray(PT.replaceAllCharacters(pos, "{,}", " "));
    return (a.length == 3 && !Double.isNaN(a[2]) ? P3d.new3(a[0], a[1], a[2])
        : null);
  }

  protected Viewer vwr;

  private ModelKitPopup menu;

  int state = STATE_MOLECULAR & STATE_SYM_NONE & STATE_SYM_APPLYFULL
      & STATE_UNITCELL_EXTEND; // 0x00

  private String atomHoverLabel = "C", bondHoverLabel = GT.$("increase order");

  private String[] allOperators;

  private int currentModelIndex = -1;

  protected ModelSet lastModelSet;

  private String lastElementType = "C";

  private final BS bsHighlight = new BS();

  private int bondIndex = -1, bondAtomIndex1 = -1, bondAtomIndex2 = -1;

  private BS bsRotateBranch;

  private int branchAtomIndex;

  /**
   * settable property maintained here
   */
  private int[] screenXY = new int[2]; // for tracking mouse-down on bond

  private boolean isPickAtomAssignCharge; // pl or mi
  /**
   * set true when bond rotation is active
   */
  private boolean isRotateBond;
  /**
   * a settable property value; not implemented
   */
  private boolean showSymopInfo = true;

  /**
   * A value set by the popup menu; questionable design
   */
  private boolean hasUnitCell;

  /**
   * alerting that ModelKit crystal editing mode has not been implemented --
   * this is no longer necessary.
   */
  private boolean alertedNoEdit;
  /**
   * set to TRUE after rotation has been turned off in order to turn highlight
   * off in viewer.hoverOff()
   */
  private boolean wasRotating;

  /**
   * when TRUE, add H atoms to C when added to the modelSet.
   */
  private boolean addHydrogens = true;
  /**
   * Except for H atoms, do not allow changes to elements just by clicking them.
   * This protects against doing that inadvertently when editing.
   * 
   */
  private boolean clickToSetElement = true;
  /**
   * set to true for proximity-based autobonding (prior to 14.32.4/15.2.4 the
   * default was TRUE
   */
  private boolean autoBond = false;

  private P3d centerPoint;

  String pickAtomAssignType = "C";

  char pickBondAssignType = 'p'; // increment up

  P3d viewOffset;

  private double centerDistance;

  private Object symop;

  private int centerAtomIndex = -1, secondAtomIndex = -1;

  private String drawData;

  private String drawScript;

  private int iatom0;

  private String lastCenter = "0 0 0", lastOffset = "0 0 0";

  private Atom a0, a3;

  Constraint constraint;

  private Constraint[] atomConstraints;

  private Atom[] minBasisAtoms;

  private SymmetryInterface[] modelSyms;
  //private BS atomLatticeConstraints;
  //private Constraint[] latticeConstraints = new Constraint[6];

  // for minimization
  private BS minBasis, minBasisFixed, minBasisModelAtoms;

  private int minBasisModel;

  private BS minSelectionSaved;

  private BS minTempFixed;

  private BS minTempModelAtoms;

  /**
   * from SET ELEMENTKEY ON/OFF or SET LABELKEY ON/OFF; 
   * TRUE to automatically set element keys for all
   * model s; off to turn them off
   */
  protected char keyType;

  /**
   * a bitset indicating the presence of element keys for models; clearing a bit
   * will cause a new key to be produced.
   * 
   */
  final protected BS bsKeyModels = new BS();

  /**
   * tracks models for which the element key has been explicitly set OFF,
   * overriding global SET elementKey ON
   */
  final protected BS bsKeyModelsOFF = new BS();

  protected boolean haveKeys;

  public ModelKit() {
    // for dynamic class loading in Java and JavaScript
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
  public void actionRotateBond(int deltaX, int deltaY, int x, int y,
                               boolean forceFull) {

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
    if (forceFull)
      bsRotateBranch = null;
    V3d v1 = V3d.new3(atomMove.sX - atomFix.sX, atomMove.sY - atomFix.sY, 0);
    v1.scale(1d / v1.length());
    V3d v2 = V3d.new3(deltaX, deltaY, 0);
    v1.cross(v1, v2);

    double f = (v1.z > 0 ? 1 : -1);
    double degrees = f * ((int) v2.length() / 2 + 1);
    if (!forceFull && a0 != null) {
      // integerize
      double ang0 = MeasureD.computeTorsion(a0, b.atom1, b.atom2, a3, true);
      double ang1 = (int) Math.round(ang0 + degrees);
      degrees = ang1 - ang0;
    }
    BS bs = BSUtil.copy(bsBranch);
    bs.andNot(vwr.slm.getMotionFixedAtoms());

    vwr.rotateAboutPointsInternal(null, atomFix, atomMove, 0, degrees, false,
        bs, null, null, null, null, true, null);
   }

  /**
   * 
   * Only for the current model
   * 
   * @param sg
   * @param bsLocked
   */
  public void addLockedAtoms(SymmetryInterface sg, BS bsLocked) {
    if (vwr.am.cmi < 0 || bsLocked.cardinality() == 0)
      return;
    BS bsm = vwr.getThisModelAtoms();
    int i0 = bsLocked.nextSetBit(0);
    if (sg == null && (sg = getSym(i0)) == null)
      return;
    for (int i = bsm.nextSetBit(0); i >= 0; i = bsm.nextSetBit(i + 1)) {
      if (setConstraint(sg, i, GET_CREATE).type == Constraint.TYPE_LOCKED) {
        bsLocked.set(i);
      }
    }
  }

  /**
   * Something has changed atom positions. Check to see that this is allowed
   * and, if it is, move all equivalent atoms appropriately. If it is not, then
   * revert.
   * 
   * @param bsFixed
   * @param bsAtoms
   * @param apos0
   *        original positions
   * @return number of atoms moved, possibly 0
   */
  public int checkMovedAtoms(BS bsFixed, BS bsAtoms, P3d[] apos0) {
    int i0 = bsAtoms.nextSetBit(0);
    int n = bsAtoms.cardinality();
    P3d[] apos = new P3d[n];
    try {
      Atom[] atoms = vwr.ms.at;
      // reset atoms
      for (int ip = 0, i = i0; i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
        apos[ip++] = P3d.newP(atoms[i]);
        atoms[i].setT(apos0[i]);
      }
      // b) dissect the molecule into independent sets of nonequivalent positions
      int maxSite = 0;
      for (int i = i0; i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
        int s = vwr.ms.at[i].getAtomSite();
        if (s > maxSite)
          maxSite = s;
      }
      int[] sites = new int[maxSite];
      P3d p1 = new P3d();
      // c) attempt all changes to make sure that they are allowed for every atom - any locking nullifies.
      //    no 
      BS bsModelAtoms = vwr.getModelUndeletedAtomsBitSet(vwr.ms.at[i0].mi);
      BS bsMoved = new BS();
      for (int ip = 0, i = i0; i >= 0; i = bsAtoms.nextSetBit(i + 1), ip++) {
        p1.setT(apos[ip]);
        int s = vwr.ms.at[i].getAtomSite() - 1;
        if (sites[s] == 0) {
          sites[s] = i + 1;
          bsMoved = moveConstrained(i, bsFixed, bsModelAtoms, p1, true, false,
              bsMoved);
          if (bsMoved == null) {
            n = 0;
            break;
          }
        }
      }
      return (n != 0 && checkAtomPositions(apos0, apos, bsAtoms) ? n : 0);
    } finally {
      if (n == 0) {
        vwr.ms.restoreAtomPositions(apos0);
        bsAtoms.clearAll();
      } else {
        updateDrawAtomSymmetry(JC.PROP_ATOMS_MOVED, bsAtoms);
      }
    }
  }

  /**
   * MODELKIT SET options for syntax checking.
   * 
   * @param type
   *        'M' menu, 'S' symmetry 'U' unit cell, 'B' boolean
   * @param key
   * @return true if the type exists
   */
  public boolean checkOption(char type, String key) {
    // only for use internally -- not for MODELKIT SET
    String check = null;
    switch (type) {
    case 'M':
      // MODELKIT MODE ....
      check = ";view;edit;molecular;";
      break;
    case 'S':
      // MODELKIT 
      check = ";none;applylocal;retainlocal;applyfull;";
      break;
    case 'U':
      // MODELKIT UNITCELL ...
      check = ";packed;extend;";
      break;
    case 'B':
      // MODELKIT (set) but not MODELKIT SET
      check = ";key;elementkey;labelkey;autobond;hidden;showsymopinfo;clicktosetelement;addhydrogen;addhydrogens;";
      break;
    }
    return (check != null && PT.isOneOf(key.toLowerCase(), check));
  }

  public void clearAtomConstraints() {
    modelSyms = null;
    minBasisAtoms = null;
    if (atomConstraints != null) {
      for (int i = atomConstraints.length; --i >= 0;)
        atomConstraints[i] = null;
    }
  }

  /**
   * From Mouse or handleAtomOrBondBicked
   * 
   * @param atomIndex
   *        initiating atom clicked or dragged from
   * @param element
   *        chemical symbol or "X" for delete, or one of the pickAssignTypes,
   *        such as increase/decrease charge
   * @param ptNew
   *        if dragged to a new location to create a bond
   */
  public void clickAssignAtom(int atomIndex, String element, P3d ptNew) {
    int n = addAtomType(element, new P3d[] { (ptNew == null ? null : ptNew) },
        BSUtil.newAndSetBit(atomIndex), "", null, "click");
    if (n > 0) // do we really want this???
      vwr.setPickingMode("dragAtom", 0);
  }

  /**
   * 
   * MODELKIT ADD @3 ...
   * 
   * MODELKIT ADD _C wyckoff <[a-zAG]
   * 
   * MODELKIT ADD C <point> | <array of points>
   * 
   * this model only
   * 
   * @param type
   *        <element Symbol> | "_"<element Symbol> | <element symbol>":"<Wyckoff
   *        letter [a-zAG]
   * @param pts
   *        one or more new points, may be null
   * @param bsAtoms
   *        the atoms to process, presumably from different sites
   * @param packing
   *        "packed" or ""
   * @param cmd
   *        the command generating this call
   * @return the number of atoms added
   */
  public int cmdAssignAddAtoms(String type, P3d[] pts, BS bsAtoms,
                               String packing, String cmd) {
    if (type.startsWith("_"))
      type = type.substring(1);
    return Math.abs(addAtomType(type, pts, bsAtoms, packing, null, cmd));
  }

  /**
   * A versatile method that allows changing element, setting charge, setting
   * position, adding or deleting an atom via
   * 
   * MODELKIT ASSIGN ATOM
   * 
   * @param bs
   *        may be -1
   * @param pt
   *        a Cartesian position for a new atom or when moving an atom to a new
   *        position
   * @param type
   *        one of: an element symbol, "X" (delete), "Mi" (decrement charge),
   *        "Pl" (increment charge), "." (from connect; just adding hydrogens)
   * @param cmd
   *        reference command given; may be null
   * 
   */
  public void cmdAssignAtom(BS bs, P3d pt, String type, String cmd) {
    if (pt != null && bs != null && bs.cardinality() > 1)
      bs = BSUtil.newAndSetBit(bs.nextSetBit(0));
    if (type.startsWith("_"))
      type = type.substring(1);
    assignAtomNoAddedSymmetry(pt, -1, bs, type, (pt != null), cmd, 0);
  }

  public void cmdAssignBond(int bondIndex, char type, String cmd) {
    assignBondAndType(bondIndex, getBondOrder(type, vwr.ms.bo[bondIndex]), type,
        cmd);
  }

  /**
   * @param index
   * @param index2
   * @param type
   * @param cmd
   */
  public void cmdAssignConnect(int index, int index2, char type, String cmd) {
    // from CmdExt
    Atom[] atoms = vwr.ms.at;
    Atom a, b;
    if (index < 0 || index2 < 0 || index >= atoms.length
        || index2 >= atoms.length || (a = atoms[index]) == null
        || (b = atoms[index2]) == null)
      return;
    int state = getMKState();
    try {
      Bond bond = null;
      if (type != '1') {
        BS bs = new BS();
        bs.set(index);
        bs.set(index2);
        bs = vwr.getBondsForSelectedAtoms(bs);
        bond = vwr.ms.bo[bs.nextSetBit(0)];
      }
      int bondOrder = getBondOrder(type, bond);
      BS bs1 = vwr.ms.getSymmetryEquivAtoms(BSUtil.newAndSetBit(index), null,
          null);
      BS bs2 = vwr.ms.getSymmetryEquivAtoms(BSUtil.newAndSetBit(index2), null,
          null);
      connectAtoms(a.distance(b), bondOrder, bs1, bs2);
      if (vwr.getOperativeSymmetry() == null) {
        bond = a.getBond(b);
        if (bond != null) {
          bs1.or(bs2);
          assignBond(bond.index, Edge.BOND_COVALENT_SINGLE, bs1);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      // ignore?
    } finally {
      setMKState(state);
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
    bs = vwr.ms.getSymmetryEquivAtoms(bs, null, null);
    if (!bs.isEmpty()) {
      vwr.deleteAtoms(bs, false);
    }
    return bs.cardinality();
  }

  public int cmdAssignMoveAtoms(BS bsSelected, int iatom, P3d p, P3d[] pts,
                                boolean allowProjection, boolean isMolecule) {
    return assignMoveAtoms(bsSelected, iatom, p, pts, allowProjection, isMolecule, true);
  }

  public int assignMoveAtoms(BS bsSelected, int iatom, P3d p, P3d[] pts,
                              boolean allowProjection, boolean isMolecule,
                              boolean isCommand) {
    SymmetryInterface sym = getSym(iatom);
    int n;
    if (sym != null) {
      if (addHydrogens)
        vwr.ms.addConnectedHAtoms(vwr.ms.at[iatom], bsSelected);
      n = assignMoveAtoms(sym, bsSelected, null, null, iatom, p, pts,
          allowProjection, isMolecule);
      if (n == 0 || bsSelected.isEmpty()) {
        vwr.showString("could not move atom!", false);
        if (!isCommand)
          vwr.warnAtom(iatom);
      } else if (!isCommand){
        vwr.warnAtom(-1);
      }
    } else {
      n = vwr.moveAtomWithHydrogens(iatom, addHydrogens ? 1 : 0, 0, 0, p, null);
    }
    return n;
  }

  /**
   * Minimize a unit cell with full symmetry constraints.
   * 
   * The model will be loaded with a 27-cell packing, but only the basis atoms
   * themselves will be loaded.
   * 
   * @param eval
   * 
   * @param bsBasis
   * @param steps
   * @param crit
   * @param rangeFixed
   * @param flags
   * @throws Exception
   */
  public void cmdMinimize(JmolScriptEvaluator eval, BS bsBasis, int steps,
                          double crit, double rangeFixed, int flags)
      throws Exception {
    boolean wasAppend = vwr.getBoolean(T.appendnew);
    try {
      vwr.setBooleanProperty("appendNew", true);
      minimizeXtal(eval, bsBasis, steps, crit, rangeFixed, flags);
    } finally {
      vwr.setBooleanProperty("appendNew", wasAppend);
    }
  }

  public int cmdRotateAtoms(BS bsAtoms, P3d[] points, double endDegrees) {
    return rotateAtoms(bsAtoms, points, endDegrees);
  }

  public void dispose() {

    // from Viewer

    menu.jpiDispose();
    menu.modelkit = null;
    menu = null;
    vwr = null;
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

  public String getDefaultModel() {

    // from Viewer

    return (addHydrogens ? JC.MODELKIT_ZAP_STRING : "1\n\nC 0 0 0\n");
  }

  /**
   * Get a property of the modelkit.
   * 
   * @param name
   * @return value
   */
  public Object getProperty(String name) {

    name = name.toLowerCase().intern();

    if (name == JC.MODELKIT_EXISTS)
      return Boolean.TRUE;

    if (name == JC.MODELKIT_CONSTRAINT) {
      return constraint;
    }

    if (name == JC.MODELKIT_ISMOLECULAR) {
      return Boolean.valueOf(getMKState() == STATE_MOLECULAR);
    }

    if (name.startsWith(JC.MODELKIT_MODEL_KEY)) {
      int model = Integer.parseInt(name.substring(JC.MODELKIT_MODEL_KEY.length()));
      return Boolean.valueOf(Key.isKeyOn(this, model, '\0') != '\0');
    }
    if (name == JC.MODELKIT_KEY) {
      return Boolean.valueOf(Key.isKeyOn(this, vwr.am.cmi, '\0') != '\0');
    }

    if (name == JC.MODELKIT_ELEMENT_KEY) {
      return Boolean.valueOf(Key.isKeyOn(this, vwr.am.cmi, 'E') == 'E');
    }

    if (name == JC.MODELKIT_LABEL_KEY) {
      return Boolean.valueOf(Key.isKeyOn(this, vwr.am.cmi, 'L') == 'L');
    }

    if (name == JC.MODELKIT_MINIMIZING)
      return Boolean.valueOf(minBasis != null);

    if (name == JC.MODELKIT_ALLOPERATORS) {
      return allOperators;
    }

    if (name == JC.MODELKIT_DATA) {
      return getinfo();
    }

    return setProperty(name, null);
  }

  public int getRotateBondIndex() {

    // from ActionManager

    return (getMKState() == STATE_MOLECULAR && isRotateBond ? bondIndex : -1);
  }

  public SymmetryInterface getSym(int iatom) {
    int modelIndex = vwr.ms.at[iatom].mi;
    if (modelSyms == null || modelIndex >= modelSyms.length) {
      modelSyms = new SymmetryInterface[vwr.ms.mc];
      for (int imodel = modelSyms.length; --imodel >= 0;) {
        SymmetryInterface sym = vwr.ms.getUnitCell(imodel);
        if (sym == null || sym.getSymmetryOperations() != null)
          modelSyms[imodel] = sym;
      }
    }
    return (iatom < 0 ? null : modelSyms[modelIndex]);
  }

  /**
   * handle a mouse-generated assignNew event
   * 
   * @param pressed
   * @param dragged
   * @param mp
   * @param dragAtomIndex
   * @param key
   *        from a key press
   * @return true if we should do a refresh now
   */
  public boolean handleAssignNew(MouseState pressed, MouseState dragged,
                                 MeasurementPending mp, int dragAtomIndex,
                                 int key) {

    // From ActionManager

    // H C + -, etc.
    // also check valence and add/remove H atoms as necessary?
    boolean inRange = pressed.inRange(ActionManager.XY_RANGE, dragged.x,
        dragged.y);
    if (mp != null && handleAtomDragging(mp.countPlusIndices))
      return true;
    String atomType = (key < 0 ? pickAtomAssignType : keyToElement(key));
    if (atomType == null)
      return false;
    int x = (inRange ? pressed.x : dragged.x);
    int y = (inRange ? pressed.y : dragged.y);
    if (vwr.antialiased) {
      x <<= 1;
      y <<= 1;
    }
    return handleAtomOrBondPicked(x, y, mp, dragAtomIndex, atomType, inRange);
  }

  public boolean hasConstraint(int iatom, boolean ignoreGeneral,
                               boolean addNew) {
    Constraint c = setConstraint(getSym(iatom), iatom,
        addNew ? GET_CREATE : GET);
    return (c != null && (!ignoreGeneral || c.type != Constraint.TYPE_GENERAL));
  }

  /**
   * From menu opening and Not clear this is a good idea. It's possible for this
   * to be set only once, when the file is loaded
   * 
   * @param isZap
   */
  private void initializeForModel(boolean isZap) {
    // from ZAP or from menu opening
    resetBondFields();
    allOperators = null;
    currentModelIndex = -999;
    iatom0 = 0;
    centerAtomIndex = secondAtomIndex = -1;
    centerPoint = null;
    // no! atomHoverLabel = bondHoverLabel = xtalHoverLabel = null;
    symop = null;
    setDefaultState(
        //setHasUnitCell() ? STATE_XTALVIEW);
        STATE_MOLECULAR);
    //setProperty("clicktosetelement",Boolean.valueOf(!hasUnitCell));
    //setProperty("addhydrogen",Boolean.valueOf(!hasUnitCell));
    if (isZap) {
      if (keyType != '\0') {
        Key.updateModelKey(this, vwr.am.cmi, true);
      }
      bsKeyModels.clearAll();
      bsKeyModelsOFF.clearAll();
    }
  }

  public boolean isHidden() {

    // from FrankReneder

    return menu.hidden;
  }

  public boolean isPickAtomAssignCharge() {

    // from ActionManager

    return isPickAtomAssignCharge;
  }

  public void minimizeEnd(BS bsBasis2, boolean isEnd) {
    minimizeXtalEnd(bsBasis2, isEnd);
    vwr.refresh(Viewer.REFRESH_REPAINT, "modelkit minimize");
  }

  public int moveMinConstrained(int iatom, P3d p, BS bsAtoms) {
    BS bsMoved = moveConstrained(iatom, null, bsAtoms, p, true, true, null);
    return (bsMoved == null ? 0 : bsMoved.cardinality());
  }

  public MeasurementPending setBondMeasure(int bi, MeasurementPending mp) {
    if (branchAtomIndex < 0)
      return null;
    Bond b = vwr.ms.bo[bi];
    Atom a1 = b.atom1;
    Atom a2 = b.atom2;
    a0 = a3 = null;
    if (a1.getCovalentBondCount() == 1 || a2.getCovalentBondCount() == 1)
      return null;
    mp.addPoint((a0 = getNearestBondedAtom(a1, a2)).i, null, true);
    mp.addPoint(a1.i, null, true);
    mp.addPoint(a2.i, null, true);
    mp.addPoint((a3 = getNearestBondedAtom(a2, a1)).i, null, true);
    mp.mad = 50;
    mp.inFront = true;
    return mp;
  }

  public void setMenu(ModelKitPopup menu) {
    // from Viewer
    this.menu = menu;
    this.vwr = menu.vwr;
    menu.modelkit = this;
    initializeForModel(false);
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

    // from ModelKit, Viewer, and CmdExt

    try {

      if (vwr == null) // clearing
        return null;

      key = key.toLowerCase().intern();

      // test first due to frequency

      if (key == JC.MODELKIT_HOVERLABEL) {
        // from hoverOn no setting of this, only getting
        return getHoverLabel(((Integer) value).intValue());
      }

      // set only

      if (key == JC.MODELKIT_INITIALIZE_MODEL) {
        // from ZAP only
        initializeForModel(true);
        return null;
      }

      if (key == "atomset") {
        addAtomSet((String) value);
        return null;
      }

      if (key == JC.PROP_ATOMS_MOVED) {
        if (drawAtomSymmetry != null) {
          updateDrawAtomSymmetry(key, ((BS[]) value)[0]);
        }
        return null;
      }
      if (key == JC.MODELKIT_UPDATE_MODEL_KEYS) {
        BS bsModels = (value == null ? null : ((BS[]) value)[1]);
        if (haveKeys) {
          if (value != null && bsModels == null) {
            if (keyType != 'L')
              return null;
            // labels only
            bsModels = vwr.ms.getModelBS(((BS[]) value)[0], false);
          }
          Key.updateModelKeys(this, bsModels, true);
        }
        if (drawAtomSymmetry != null && bsModels != null) {
          updateDrawAtomSymmetry(JC.PROP_ATOMS_DELETED, ((BS[]) value)[0]);
        }
        return null;
      }

      if (key == JC.MODELKIT_UDPATE_KEY_STATE) {
        Key.updateKeyFromStateScript(this);
        return null;
      }

      if (key == JC.MODELKIT_UPDATE_ATOM_KEYS) {
        Key.updateKey(this, (BS) value);
        return null;
      }

      if (key == JC.MODELKIT_SET_ELEMENT_KEY) {
        // exclusively from SET elementKeys...
        keyType = 'E';
        Key.setKeys(this, isTrue(value));
        
        return null;
      }
      if (key == JC.MODELKIT_SET_LABEL_KEY) {
        keyType = 'L';
        // exclusively from SET elementKeys...
        Key.setKeys(this, isTrue(value));
        return null;
      }

      if (key == JC.MODELKIT_FRAME_RESIZED) {
        Key.clearKey(this, -2);
        Key.updateModelKeys(this, null, true);
        return null;
      }

      if (key == JC.MODELKIT_KEY || key == JC.MODELKIT_ELEMENT_KEY) {
        // modelkit set elementkey(s), set key
        int mi = vwr.am.cmi;
        boolean isOn = isTrue(value);
        bsKeyModelsOFF.setBitTo(mi, !isOn);
        bsKeyModels.setBitTo(mi, false);// force new, for whatever reason
        Key.setKey(this, mi, isOn);
        return isOn ? "true" : "false";
      }

      if (key == JC.MODELKIT_BRANCH_ATOM_PICKED) {
        if (isRotateBond && !vwr.acm.isHoverable())
          setBranchAtom(((Integer) value).intValue(), true);
        return null;
      }

      if (key == JC.MODELKIT_BRANCH_ATOM_DRAGGED) {
        if (isRotateBond)
          setBranchAtom(((Integer) value).intValue(), true);
        return null;
      }

      if (key == JC.MODELKIT_HIDEMENU) {
        menu.hidePopup();
        return null;
      }

      if (key == JC.MODELKIT_CONSTRAINT) {
        constraint = null;
        clearAtomConstraints();
        Object[] o = (Object[]) value;
        if (o != null) {
          P3d v1 = (P3d) o[0];
          P3d v2 = (P3d) o[1];
          P4d plane = (P4d) o[2];
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
      if (key == JC.MODELKIT_RESET) {
        //        setProperty("atomType", "C");
        //        setProperty("bondType", "p");
        return null;
      }

      if (key == JC.MODELKIT_ATOMPICKINGMODE) {
        if (PT.isOneOf((String) value, ";identify;off;")) {
          exitBondRotation(null);
          vwr.setBooleanProperty("bondPicking", false);
          vwr.acm.exitMeasurementMode("modelkit");
        }
        if (JC.MODELKIT_DRAGATOM.equals(value)) {
          setHoverLabel(ATOM_MODE, getText("dragAtom"));
        }
        return null;
      }

      if (key == JC.MODELKIT_BONDPICKINGMODE) {
        if (value.equals(JC.MODELKIT_DELETE_BOND)) {
          exitBondRotation(getText("bondTo0"));
        } else if (value.equals("identifybond")) {
          exitBondRotation("");
        }
        return null;
      }

      if (key == JC.MODELKIT_ROTATE_BOND_ATOM_INDEX) {
        int i = ((Integer) value).intValue();
        if (i != bondAtomIndex2)
          bondAtomIndex1 = i;

        bsRotateBranch = null;
        return null;
      }

      if (key == JC.MODELKIT_BONDINDEX) {
        if (value != null) {
          setBondIndex(((Integer) value).intValue(), false);
        }
        return (bondIndex < 0 ? null : Integer.valueOf(bondIndex));
      }

      if (key == JC.MODELKIT_ROTATEBONDINDEX) {
        if (value != null) {
          setBondIndex(((Integer) value).intValue(), true);
        }
        return (bondIndex < 0 ? null : Integer.valueOf(bondIndex));
      }

      if (key == JC.MODELKIT_HIGHLIGHT) {
        bsHighlight.clearAll();
        if (value != null)
          bsHighlight.or((BS) value);
        return null;
      }

      if (key == JC.MODELKIT_MODE) { // view, edit, or molecular
        boolean isEdit = ("edit".equals(value));
        setMKState("view".equals(value) ? STATE_XTALVIEW
            : isEdit ? STATE_XTALEDIT : STATE_MOLECULAR);
        if (isEdit)
          addHydrogens = false;
        return null;
      }

      if (key == JC.MODELKIT_SYMMETRY) {
        setDefaultState(STATE_XTALEDIT);
        key = ((String) value).toLowerCase().intern();
        setSymEdit(key == "applylocal" ? STATE_SYM_APPLYLOCAL
            : key == "retainlocal" ? STATE_SYM_RETAINLOCAL
                : key == "applyfull" ? STATE_SYM_APPLYFULL : 0);
        showXtalSymmetry();
        return null;
      }

      if (key == JC.MODELKIT_UNITCELL) { // packed or extend
        boolean isPacked = "packed".equals(value);
        setUnitCell(isPacked ? STATE_UNITCELL_PACKED : STATE_UNITCELL_EXTEND);
        viewOffset = (isPacked ? Pt000 : null);
        return null;
      }

      if (key == JC.MODELKIT_CENTER) {
        setDefaultState(STATE_XTALVIEW);
        centerPoint = (P3d) value;
        lastCenter = centerPoint.x + " " + centerPoint.y + " " + centerPoint.z;
        centerAtomIndex = (centerPoint instanceof Atom ? ((Atom) centerPoint).i
            : -1);
        secondAtomIndex = -1;
        clickProcessAtom(centerAtomIndex);
        return null;
      }

      if (key == JC.MODELKIT_ASSIGN_BOND) {
        // from ActionManger only
        cmdAssignBond(((Integer) value).intValue(), pickBondAssignType,
            "click");
        return null;
      }

      // boolean get/set

      if (key == JC.MODELKIT_ADDHYDROGEN || key == JC.MODELKIT_ADDHYDROGENS) {
        if (value != null)
          addHydrogens = isTrue(value);
        return Boolean.valueOf(addHydrogens);
      }

      if (key == JC.MODELKIT_AUTOBOND) {
        if (value != null)
          autoBond = isTrue(value);
        return Boolean.valueOf(autoBond);
      }

      if (key == JC.MODELKIT_CLICKTOSETELEMENT) {
        if (value != null)
          clickToSetElement = isTrue(value);
        return Boolean.valueOf(clickToSetElement);
      }

      if (key == JC.MODELKIT_HIDDEN) {
        if (value != null) {
          menu.hidden = isTrue(value);
          if (menu.hidden)
            menu.hidePopup();
          vwr.setBooleanProperty("modelkitMode", true);
        }
        return Boolean.valueOf(menu.hidden);
      }

      if (key == JC.MODELKIT_SHOWSYMOPINFO) {
        if (value != null)
          showSymopInfo = isTrue(value);
        return Boolean.valueOf(showSymopInfo);
      }

      if (key == JC.MODELKIT_SYMOP) {
        setDefaultState(STATE_XTALVIEW);
        if (value != null) {
          // get only, but with a value argument

          if (key == "hoverlabel") {
            // from hoverOn no setting of this, only getting
            return getHoverLabel(((Integer) value).intValue());
          }
          symop = value;
          showSymop(symop);
        }
        return symop;
      }

      if (key == JC.MODELKIT_ATOMTYPE) {
        wasRotating = isRotateBond;
        isRotateBond = false;
        if (value != null) {
          pickAtomAssignType = (String) value;
          isPickAtomAssignCharge = (pickAtomAssignType.equalsIgnoreCase("pl")
              || pickAtomAssignType.equalsIgnoreCase("mi"));
          if (isPickAtomAssignCharge) {
            setHoverLabel(ATOM_MODE,
                getText(pickAtomAssignType.equalsIgnoreCase("mi") ? "decCharge"
                    : "incCharge"));
          } else if ("X".equals(pickAtomAssignType)) {
            setHoverLabel(ATOM_MODE, getText("delAtom"));
          } else if (pickAtomAssignType.equals("Xx")) {
            setHoverLabel(ATOM_MODE, getText("dragBond"));
          } else {
            setHoverLabel(ATOM_MODE, "Click or click+drag to bond or for a new "
                + pickAtomAssignType);
            lastElementType = pickAtomAssignType;
          }
        }
        return pickAtomAssignType;
      }

      if (key == JC.MODELKIT_BONDTYPE) {
        if (value != null) {
          String s = ((String) value).substring(0, 1).toLowerCase();
          if (" 0123456pm".indexOf(s) > 0) {
            pickBondAssignType = s.charAt(0);
            setHoverLabel(BOND_MODE,
                getText(pickBondAssignType == 'm' ? "decBond"
                    : pickBondAssignType == 'p' ? "incBond" : "bondTo" + s));
          }
          isRotateBond = false;
        }
        return "" + pickBondAssignType;
      }

      if (key == JC.MODELKIT_OFFSET) {
        if (value == "none") {
          viewOffset = null;
        } else if (value != null) {
          viewOffset = (value instanceof P3d ? (P3d) value
              : pointFromTriad(value.toString()));
          if (viewOffset != null)
            setSymViewState(STATE_SYM_SHOW);
        }
        showXtalSymmetry();
        return viewOffset;
      }

      if (key == JC.MODELKIT_SCREENXY) {
        if (value != null) {
          screenXY = (int[]) value;
          vwr.acm.exitMeasurementMode("modelkit");
        }
        return screenXY;
      }

      // not yet implemented

      if (key == JC.MODELKIT_INVARIANT) {
        // not really a model kit issue
        int iatom = (value instanceof BS ? ((BS) value).nextSetBit(0) : -1);
        P3d atom = vwr.ms.getAtom(iatom);
        return (atom == null ? null
            : vwr.getSymmetryInfo(iatom, null, -1, null, atom, atom, T.array,
                null, 0, 0, 0, null));
      }

      if (key == JC.MODELKIT_DISTANCE) {
        setDefaultState(STATE_XTALEDIT);
        double d = (value == null ? Double.NaN
            : value instanceof Double ? ((Number) value).doubleValue()
                : PT.parseDouble((String) value));
        if (!Double.isNaN(d)) {
          notImplemented("setProperty: distance");
          centerDistance = d;
        }
        return Double.valueOf(centerDistance);
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

      if (key == JC.MODELKIT_VIBRATION) {
        WyckoffModulation.setVibrationMode(this, value);
        return null;
      }

      System.err.println("ModelKit.setProperty? " + key + " " + value);

    } catch (Exception e) {
      return "?";
    }

    return null;
  }

  public void showMenu(int x, int y) {

    // from viewer

    menu.jpiShow(x, y);
  }

  public void updateMenu() {

    // from Viewer

    menu.jpiUpdateComputedMenus();
  }

  public boolean wasRotating() {
    boolean b = wasRotating;
    wasRotating = false;
    return b;
  }

  protected boolean checkNewModel() {
    boolean isNew = false;
    if (vwr.ms != lastModelSet) {
      lastModelSet = vwr.ms;
      isNew = true;
    }
    currentModelIndex = Math.max(vwr.am.cmi, 0);
    iatom0 = vwr.ms.am[currentModelIndex].firstAtomIndex;
    return isNew;
  }

  protected void clickProcessXtal(String id, String action) {
    if (processSymop(id, false))
      return;
    action = action.intern();
    if (action.startsWith("mkmode_")) {
      if (!alertedNoEdit && action == "mkmode_edit") {
        alertedNoEdit = true;
        vwr.alert("ModelKit xtal edit has not been implemented");
        return;
      }
      clickProcessMode(action);
    } else if (action.startsWith("mksel_")) {
      clickProcessSel(action);
    } else if (action.startsWith("mkselop_")) {
      while (action != null)
        action = clickProcessSelOp(action);
    } else if (action.startsWith("mksymmetry_")) {
      clickProcessSym(action);
    } else if (action.startsWith("mkunitcell_")) {
      clickProcessUC(action);
    } else {
      notImplemented("XTAL click " + action);
    }
    menu.updateAllXtalMenuOptions();
  }

  protected void exitBondRotation(String text) {
    wasRotating = isRotateBond;
    isRotateBond = false;
    if (text != null)
      bondHoverLabel = text;
    vwr.highlight(null);
  }

  protected String[] getAllOperators() {
    if (allOperators != null)
      return allOperators;
    String data = runScriptBuffered("show symop");
    allOperators = PT.split(data.trim().replace('\t', ' '), "\n");
    return allOperators;
  }

  protected Atom getBasisAtom(int iatom) {
    if (minBasisAtoms == null) {
      minBasisAtoms = new Atom[vwr.ms.ac + 10];
    }
    if (minBasisAtoms.length < iatom + 10) {
      Atom[] a = new Atom[vwr.ms.ac + 10];
      System.arraycopy(minBasisAtoms, 0, a, 0, minBasisAtoms.length);
      minBasisAtoms = a;
    }
    Atom b = minBasisAtoms[iatom];
    return (b == null
        ? (minBasisAtoms[iatom] = vwr.ms.getBasisAtom(iatom, false))
        : b);
  }

  protected String getCenterText() {
    return (centerAtomIndex < 0 && centerPoint == null ? null
        : centerAtomIndex >= 0 ? vwr.getAtomInfo(centerAtomIndex)
            : centerPoint.toString());
  }

  protected String getElementFromUser() {
    String element = promptUser(GT.$("Element?"), "");
    return (element == null
        || Elements.elementNumberFromSymbol(element, true) == 0 ? null
            : element);
  }

  protected int getMKState() {
    return state & STATE_BITS_XTAL;
  }

  protected int getSymEditState() {
    return state & STATE_BITS_SYM_EDIT;
  }

  protected String getSymopText() {
    return (symop == null || allOperators == null ? null
        : symop instanceof Integer
            ? allOperators[((Integer) symop).intValue() - 1]
            : symop.toString());
  }

  protected int getSymViewState() {
    return state & STATE_BITS_SYM_VIEW;
  }

  protected int getUnitCellState() {
    return state & STATE_BITS_UNITCELL;
  }

  protected boolean isXtalState() {
    return ((state & STATE_BITS_XTAL) != 0);
  }

  protected void processMKPropertyItem(String name, boolean TF) {
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

  protected boolean processSymop(String id, boolean isFocus) {
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

  protected void resetAtomPickType() {
    setProperty(JC.MODELKIT_ATOMTYPE, lastElementType);
  }

  /**
   * This constraint will be set for the site only.
   * 
   * @param sym
   * @param ia
   * @param mode
   *        GET, GET_CREATE, or GET_DELETE
   * @return a Constraint, or possibly null if not createNew
   */
  protected Constraint setConstraint(SymmetryInterface sym, int ia, int mode) {
    if (ia < 0)
      return null;
    Atom a = getBasisAtom(ia);
    int iatom = a.i;
    Constraint ac = (atomConstraints != null && iatom < atomConstraints.length
        ? atomConstraints[iatom]
        : null);
    if (ac != null || mode != GET_CREATE) {
      if (ac != null && mode == GET_DELETE) {
        atomConstraints[iatom] = null;
      }
      return ac;
    }
    if (sym == null)
      return addConstraint(iatom,
          new Constraint(a, Constraint.TYPE_NONE, null));
    // plane and frieze adds an (x,y,0) plane constraint
    boolean isPlanar = (sym.getDimensionality() == 2);
    
    // the first atom is the site atom
    // these may be special cases such as (y+1/2,x+1/2,z)
    // where y = x + 1/2. For example {0.3,0.8,0}->{1.3,0.8,0} == {0.3 0.8 0.0}
    // glide-planes and screw axes do not create invariant points in general,
    // but there are special cases like this. 
    int[] ops = sym.getInvariantSymops(a, null);
    if (Logger.debugging)
      System.out.println("MK.getConstraint atomIndex=" + iatom + " symops="
          + Arrays.toString(ops));
    // if no invariant operators, this is a general position
    if (ops.length == 0 && !isPlanar)
      return addConstraint(iatom,
          new Constraint(a, Constraint.TYPE_GENERAL, null));
    // we need only work with the first plane or line or point
    P4d plane1 = null;
    Object[] line1 = null;
    for (int i = ops.length + (isPlanar ? 1 : 0); --i >= 0;) {
      Object[] line2 = null;
      Object c = (i == ops.length ? plane001 : sym.getSymmetryInfoAtom(vwr.ms, iatom, null, ops[i], null, a,
          null, JC.MODELKIT_INVARIANT, T.array, 0, -1, 0, null));
      if (c instanceof String) {
        // this would be a special glide plane, for instance
        continue;
      } else if (c instanceof P4d) {
        // check plane - first is the constraint; second is almost(?) certainly not parallel
        P4d plane = (P4d) c;
        if (plane1 == null) {
          plane1 = plane;
          continue;
        }
        // note that the planes cannot be parallel, because a point cannot be
        // invariant on two parallel planes.
        Lst<Object> line = MeasureD.getIntersectionPP(plane1, plane);
        if (line == null || line.size() == 0) {
          // not possible?
          return locked;
        }
        line2 = new Object[] { line.get(0), line.get(1) };
      } else if (c instanceof P3d) {
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
          T3d v1 = (T3d) line1[1];
          if (Math.abs(v1.dot((T3d) line2[1])) < 0.999d)
            return locked;
        }
        if (plane1 != null) {
          if (Math.abs(plane1.dot((T3d) line2[1])) > 0.001d)
            return locked;
        }
      }
    }
    if (line1 != null) {
      // translate line to be through this atom
      line1[0] = P3d.newP(a);
    }
    return addConstraint(iatom, //true ? locked : 
        line1 != null ? new Constraint(a, Constraint.TYPE_VECTOR, line1)
            : plane1 != null
                ? new Constraint(a, Constraint.TYPE_PLANE,
                    new Object[] { plane1 })
                : new Constraint(a, Constraint.TYPE_GENERAL, null));
  }

  protected boolean setHasUnitCell() {
    return hasUnitCell = (vwr.getOperativeSymmetry() != null);
  }

  protected void setHoverLabel(String mode, String text) {
    if (text == null)
      return;
    if (mode == BOND_MODE) {
      bondHoverLabel = text;
    } else if (mode == ATOM_MODE) {
      atomHoverLabel = text;
    } else if (mode == XTAL_MODE) {
      atomHoverLabel = text;
    }
  }

  protected void setMKState(int bits) {
    state = (state & ~STATE_BITS_XTAL) | (hasUnitCell ? bits : STATE_MOLECULAR);
  }

  /**
   * Entry point from clickAssignAtom or cmdAssignAddAtoms
   * 
   * @param type
   *        <element Symbol> | <element symbol>":"<Wyckoff letter [a-zAG]
   * @param pts
   * @param bsAtoms
   * @param packing
   * @param opsCtr
   * @param cmd
   * @return number of atoms added
   */
  private int addAtomType(String type, P3d[] pts, BS bsAtoms, String packing,
                          M4d[] opsCtr, String cmd) {
    SymmetryInterface sym = vwr.getOperativeSymmetry();
    int ipt = type.indexOf(":");
    String wyckoff = (ipt > 0 && ipt == type.length() - 2
        ? type.substring(ipt + 1)
        : null);
    if (wyckoff != null) {
      type = type.substring(0, ipt);
      if (sym != null) {
        Object o = sym.getWyckoffPosition(vwr, pts == null ? null : pts[0], wyckoff);
        if ("L".equals(wyckoff)) {
          Object[] oa  = (Object[]) o;
          P3d[] allPts = (P3d[]) oa[0];
          String elements = (String) oa[1];
          pts = new P3d[1];
          int n = 0;
          int opt = allPts.length - 1;
          for (int i = 0, ept = opt * 2; i < allPts.length; i++, ept -= 2) {
            pts[0] = allPts[i];
            String el = (i == 0 ? type : elements.substring(ept, ept + 2).trim());
            n += addAtoms(el, 
                pts, bsAtoms, packing, opsCtr, cmd);
          }
          return n;
        }
        if (!(o instanceof P3d))
          return 0;
        pts = new P3d[] { (P3d) o };
      }
    }
    return addAtoms(type, pts, bsAtoms, packing, opsCtr, cmd);
  }

  /**
   * The full-blown command with all options, called by addAtomType or
   * cmdAssignSpaceGroupPacked
   * 
   * @param type
   *        <element Symbol> | <element symbol>":"<Wyckoff letter [a-zAG]
   * @param pts
   * @param bsAtoms
   * @param packing
   * @param opsCtr
   * @param cmd
   * @return 0 if nothing added; -n if added and no symmetry; n if added with
   *         symmetry
   */
  private int addAtoms(String type, P3d[] pts, BS bsAtoms, String packing,
                       M4d[] opsCtr, String cmd) {
    try {
      vwr.pushHoldRepaintWhy("modelkit");
      SymmetryInterface sym = vwr.getOperativeSymmetry();
      if (type != null) {
        int ipt = type.indexOf(":");
        String wyckoff = (ipt > 0 && ipt == type.length() - 2
            ? type.substring(ipt + 1)
            : null);
        if (wyckoff != null) {
          type = type.substring(0, ipt);
          if (sym != null) {
            Object o = sym.getWyckoffPosition(vwr, null, wyckoff);
            if (!(o instanceof P3d))
              return 0;
            pts = new P3d[] { (P3d) o };
          }
        }
      }
      boolean isPoint = (bsAtoms == null);
      int atomIndex = (isPoint ? -1 : bsAtoms.nextSetBit(0));
      if (!isPoint && atomIndex < 0 || sym == null && type == null)
        return 0;
      int n = 0;
      if (sym == null) {
        // when no symmetry, this is just a way to add multiple points at the same time. 
        if (isPoint) {
          for (int i = 0; i < pts.length; i++)
            assignAtomNoAddedSymmetry(pts[i], -1, null, type, true, cmd, -1);
          n = -pts.length;
        } else {
          assignAtomNoAddedSymmetry(pts[0], atomIndex, null, type, true, cmd,
              -1);
          n = -1;
        }
      } else {
        // handle equilivalent positions
        n = addAtomsWithSymmetry(sym, bsAtoms, type, atomIndex, isPoint, pts,
            packing, opsCtr);
      }
      return n;
    } catch (Exception e) {
      e.printStackTrace();
      return 0;
    } finally {
      vwr.popHoldRepaint("modelkit");
    }
  }

  /**
   * 
   * Add atoms with or without packing, but always with consideration of
   * equivalent positions.
   * 
   * 
   * must have symmetry; must be this model
   * 
   * @param sym
   * @param bsAtoms
   *        model atoms to check for identity of atom at the specified location
   * @param type
   * @param atomIndex
   * @param isPoint
   * @param pts
   * @param packing
   *        "packed" or ""
   * @param opsCtr
   *        augmented operator set that includes lost translations for subgroups
   * @return number of atoms added
   */
  private int addAtomsWithSymmetry(SymmetryInterface sym, BS bsAtoms,
                                   String type, int atomIndex, boolean isPoint,
                                   P3d[] pts, String packing, M4d[] opsCtr) {
    BS bsM = vwr.getThisModelAtoms();
    int n = bsM.cardinality();
    if (n == 0)
      packing = "zapped;" + packing;
    String stype = "" + type;
    Lst<P3d> points = new Lst<P3d>();
    int site = 0;
    P3d pf = null;
    if (pts != null && pts.length == 1 && pts[0] != null) {
      pf = P3d.newP(pts[0]);
      sym.toFractional(pf, false);
      if (sym.getDimensionality() == 2)
        pf.z = 0;
      isPoint = true;
    }
    // set element type from the atom at this position already, if there is one
    for (int i = bsM.nextSetBit(0); i >= 0; i = bsM.nextSetBit(i + 1)) {
      P3d p = P3d.newP(vwr.ms.at[i]);
      sym.toFractional(p, false);
      if (pf != null && pf.distanceSquared(p) < JC.UC_TOLERANCE2) {
        site = vwr.ms.at[i].getAtomSite();
        if (type == null || pts == null)
          type = vwr.ms.at[i].getElementSymbolIso(true);
      }
      points.addLast(p);
    }
    int nInitial = points.size();
    packing = "fromfractional;tocartesian;" + packing;
    if (isPoint) {

      // MODELKIT ASSIGN ATOM ....
      // new atom at a single point,
      // but there could be equivalent atoms. 
      // note that if there are occupancies at the same spot
      // even if "a point", we need to move all of them.

      BS bsEquiv = (bsAtoms == null ? null
          : vwr.ms.getSymmetryEquivAtoms(bsAtoms, null, null));
      for (int i = 0; i < pts.length; i++) {
        assignAtoms(P3d.newP(pts[i]), atomIndex, bsEquiv, stype, true, // newPoint
            null, // cmd
            false, site, sym, points, packing, null);
      }
    } else {

      // MODELKIT SPACEGROUP 

      // Go through site-by-site. There is no need to check
      // every atom of a given site, since any one will produce
      // the closed set of all the others. But we do need to 
      // add all symmetry-equivalent atoms for each site.

      BS sites = new BS();
      for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms
          .nextSetBit(i + 1)) {
        Atom a = vwr.ms.at[i];
        site = a.getAtomSite();
        if (sites.get(site))
          continue;
        sites.set(site);
        stype = (type == null ? a.getElementSymbolIso(true) : stype);

        // now assign atoms with consideration for equivalent atoms. 

        assignAtoms(P3d.newP(a), -1, null, stype, false, null, false, site, sym,
            points, packing, opsCtr);

        // If we don't have augmnted operations, we can just
        // remove the new atoms from the list, because there won't be any
        // cross-over between Wyckoff positions.
        // 
        // But if we have augmented operations, then we need
        // to convert these back to fractional so that they
        // can be checked for duplicate positions. 
        // (They were converted to Cartesians in order to 
        // load them into the structure.)

        if (opsCtr == null) {
          for (int j = points.size(); --j >= nInitial;)
            points.removeItemAt(j);
          // no change in nInitial
        } else {
          for (int j = points.size(); --j >= nInitial;) {
            // these were converted to Cartesians.
            P3d p = points.get(j);
            sym.toFractional(p, false);
          }
          // include these now for duplication checking
          nInitial = points.size();
        }
      }
    }
    return vwr.getThisModelAtoms().cardinality() - n;
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

  private void addInfo(Map<String, Object> info, String key, Object value) {
    if (value != null)
      info.put(key, value);
  }

  /**
   * Add all partially occupied atoms near each atom in a bitset
   * 
   * @param bsSelected
   *        bitset to add atom index to if it is partially occupied.
   */
  private void addOccupiedAtomsToBitset(BS bsSelected) {
    BS bs = new BS();
    for (int iatom = bsSelected.nextSetBit(0); iatom >= 0; iatom = bsSelected
        .nextSetBit(iatom + 1)) {
      // pick up occupancies
      Atom a = vwr.ms.at[iatom];
      if (vwr.ms.getOccupancyFloat(a.i) == 100) {
        bsSelected.set(a.i);
      } else {
        bs.clearAll();
        vwr.ms.getAtomsWithin(0.0001d, a, bs, a.mi);
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
          // passing over another atom
          if (vwr.ms.getOccupancyFloat(i) == 100) {
            bs.clear(i);
            bsSelected.clear(i);
          }
        }
        bsSelected.or(bs);
      }
    }
  }

  private void appRunScript(String script) {
    vwr.runScript(script);
  }

  /**
   * Just a conduit for debugging and keeping one's sanity.
   * 
   * 
   * @param pt
   * @param atomIndex
   * @param bs
   * @param type
   * @param newPoint
   * @param cmd
   * @param site
   */
  private void assignAtomNoAddedSymmetry(P3d pt, int atomIndex, BS bs,
                                         String type, boolean newPoint,
                                         String cmd,
                                         // strictly internal, for crystal work:
                                         int site) {
    assignAtoms(pt, atomIndex, bs, type, newPoint, cmd, false, site, null, null,
        null, null);
  }

  /**
   * The penultimate target method.
   * 
   * Change element, charge, and deleting an atom by clicking on it or via the
   * MODELKIT ASSIGN ATOM command or from packing with MODELKIT SPACEGROUP ...
   * PACKED
   * 
   * <pre>
   * pt     atomIndex  bs
   * null   i          bs  ASSIGN ATOM @1 "N"
   * 
   * pt    -1        null  ASSIGN ATOM "N" {x,y,z}
   * 
   * pt    -1          bs  ADD ATOM @1 "N" {x,y,z} (add with bonding)
   * 
   * pt     i          bsEquiv  SPACEGROUP ... PACKED
   * </pre>
   * 
   * @param pt
   * @param atomIndex
   * @param bs
   * @param type
   * @param newPoint
   * @param cmd
   *        passed on to notifications
   * @param isClick
   * @param site
   * @param sym
   *        a SymmetryInterface being passed on from addAtoms, or null; will be
   *        set if null
   * @param points
   * @param packing
   * @param opsCtr
   *        when packing a subgroup or setting with expanded unit cell
   *        (deteriminent > 1), this is the expanded operation set that includes
   *        the lost translations
   */
  private void assignAtoms(P3d pt, int atomIndex, BS bs, String type,
                           boolean newPoint, String cmd, boolean isClick,
                           // strictly internal, for crystal work:
                           int site, SymmetryInterface sym, Lst<P3d> points,
                           String packing, M4d[] opsCtr) {
    if (sym == null)
      sym = vwr.getOperativeSymmetry();
    boolean haveAtomByIndex = (atomIndex >= 0);
    boolean isMultipleAtoms = (bs != null && bs.cardinality() > 1);
    int nIgnored = 0;
    int np = 0;
    if (!haveAtomByIndex)
      atomIndex = (bs == null ? -1 : bs.nextSetBit(0));
    Atom atom = (atomIndex < 0 ? null : vwr.ms.at[atomIndex]);
    double bd = (pt != null && atom != null ? pt.distance(atom) : -1);
    if (points != null) {
      np = nIgnored = points.size();
      sym.toFractional(pt, false);
      if (pt.z != 0 && sym.getDimensionality() == 2)
        pt.z = 0;
      points.addLast(pt);
      if (newPoint && haveAtomByIndex)
        nIgnored++;
      // this will convert points to the needed equivalent points
      sym.getEquivPointList(points, nIgnored,
          packing + (newPoint && atomIndex < 0 ? "newpt" : ""), opsCtr);
    }
    BS bsEquiv = (atom == null ? null
        : sym != null ? vwr.ms.getSymmetryEquivAtoms(bs, sym, null)
            : bs == null || bs.cardinality() == 0
                ? BSUtil.newAndSetBit(atomIndex)
                : bs);
    BS bs0 = (bsEquiv == null ? null
        : sym == null ? BSUtil.newAndSetBit(atomIndex) : BSUtil.copy(bsEquiv));
    int mi = (atom == null ? vwr.am.cmi : atom.mi);
    int ac = vwr.ms.ac;
    int state = getMKState();
    boolean isDelete = type.equals("X");
    try {
      if (isDelete) {
        if (isClick) {
          setProperty(JC.MODELKIT_ROTATEBONDINDEX, Integer.valueOf(-1));
        }
        setConstraint(null, atomIndex, GET_DELETE);
      }
      if (pt == null && points == null) {
        // no new position
        // just assigning a charge, deleting an atom, or changing its element 
        if (atom == null)
          return;
        vwr.sm.setStatusStructureModified(atomIndex, mi,
            Viewer.MODIFY_ASSIGN_ATOM, cmd, 1, bsEquiv);
        for (int i = bsEquiv.nextSetBit(0); i >= 0; i = bsEquiv
            .nextSetBit(i + 1)) {
          assignAtom(i, type, autoBond, sym == null, isClick);
        }
        if (!PT.isOneOf(type, ";Mi;Pl;X;"))
          vwr.ms.setAtomNamesAndNumbers(atomIndex, -ac, null, true);
        vwr.sm.setStatusStructureModified(atomIndex, mi,
            -Viewer.MODIFY_ASSIGN_ATOM, "OK", 1, bsEquiv);
        vwr.refresh(Viewer.REFRESH_SYNC_MASK, "assignAtom");
        Key.updateKey(this, null);
        return;
      }

      // have pt or points -- assumes at most a SINGLE atom here
      // if pt != null, then it is what needs to be duplicated
      // if bs != null, then we have atoms to connect as well 

      setMKState(STATE_MOLECULAR);

      // set up the pts array for equivalent points, which will be passed
      // to vwr.addHydrogenAtoms along with information about site and element type

      P3d[] pts;
      if (points == null) {
        pts = new P3d[] { pt };
      } else {
        pts = new P3d[Math.max(0, points.size() - np)];
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
          if (!isMultipleAtoms) {
            vConnections.addLast(atom);
            isConnected = true;
          } else if (sym != null) {
            P3d p = P3d.newP(atom);
            sym.toFractional(p, false);
            bs.or(bsEquiv);
            Lst<P3d> list = sym.getEquivPoints(null, p, packing);
            for (int j = 0, n = list.size(); j < n; j++) {
              for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
                if (vwr.ms.at[i].distanceSquared(list.get(j)) < 0.001d) {
                  vConnections.addLast(vwr.ms.at[i]);
                  bs.clear(i);
                }
              }
            }
          }
          isConnected = (vConnections.size() == pts.length);
          if (isConnected) {
            double d = Double.MAX_VALUE;
            for (int i = pts.length; --i >= 0;) {
              double d1 = vConnections.get(i).distance(pts[i]);
              if (d == Double.MAX_VALUE)
                d1 = d;
              else if (Math.abs(d1 - d) > 0.001d) {
                // this did not work
                isConnected = false;
                break;
              }
            }
          }
          if (!isConnected) {
            vConnections.clear();
          }
          vwr.sm.setStatusStructureModified(atomIndex, mi,
              Viewer.MODIFY_SET_COORD, cmd, 1, null);
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
      if (!isMK && sym == null) {
        vwr.setBooleanProperty("modelkitmode", true);
        menu.hidden = true;
        menu.allowPopup = false;
      }

      // now add the "hydrogens" aka new atoms
      // using vwr.addHydrogensInline, which works through a merge using ModelLoader

      Map<String, Object> htParams = new Hashtable<String, Object>();
      if (site > 0)
        htParams.put("fixedSite", Integer.valueOf(site));
      htParams.put("element", type);
      bs = vwr.addHydrogensInline(bs, vConnections, pts, htParams);
      if (bd > 0 && !isConnected && vConnections.isEmpty()) {
        connectAtoms(bd, 1, bs0, bs);
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

      // we have the new atom, now assign it

      int atomIndexNew = bs.nextSetBit(0);
      if (points == null) {
        // new single atom
        assignAtom(atomIndexNew, type, false, atomIndex >= 0 && sym == null,
            true);
        if (atomIndex >= 0) {
          boolean doAutobond = (sym == null && !"H".equals(type));
          assignAtom(atomIndex, ".", false, doAutobond, isClick);
        }
        vwr.ms.setAtomNamesAndNumbers(atomIndexNew, -ac, null, true);
        vwr.sm.setStatusStructureModified(atomIndexNew, mi,
            -Viewer.MODIFY_SET_COORD, "OK", 1, bs);
        return;
      }
      // potentially many new atoms here, from MODELKIT ADD 
      if (atomIndexNew >= 0) {
        for (int i = atomIndexNew; i >= 0; i = bs.nextSetBit(i + 1)) {
          assignAtom(i, type, false, false, true);
          // ensure that site is tainted
          vwr.ms.setSite(vwr.ms.at[i], -1, false);
          vwr.ms.setSite(vwr.ms.at[i], site, true);
        }
        vwr.ms.updateBasisFromSite(mi);
      }
      int firstAtom = vwr.ms.am[mi].firstAtomIndex;
      vwr.ms.setAtomNamesAndNumbers(firstAtom, -ac, null, true);
      vwr.sm.setStatusStructureModified(-1, mi, -3, "OK", 1, bs);
      Key.updateModelKey(this, mi, true);
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      setMKState(state);
    }
  }

  /**
   * Do the actual assignment of a single atom, possibly with bonding, possibly
   * deletion or charge increment/decerement.
   * 
   * This is the terminal method in the series:
   * 
   * <pre>
   *     
   *        clickAssignAtom        cmdAssignAddAtoms
   *                 |                      |
   *                 +----> AddAtomType <---+       cmdAssignSpaceGroupPacked
   *                            |                          |
   *       cmdAssignAtom        + ----> addAtoms <---------+     
   *              |                         |                        
   *        assignAtomNoAddedSymmetry  <----+---->   assignAtomWithSymmetry
   *                     |                                    |
   *                     +-----------> assignAtoms <----------+    
   *                                        |
   *                                        +--------> assignAtom    
   *                                                   ==========
   *
   * </pre>
   * 
   * @param atomIndex
   *        the atom clicked
   * @param type
   * @param autoBond
   *        an older idea whereby atoms are bonded automatically, for example,
   *        when a cyclohexane ring closes; but this caused unexpected action,
   *        like making cyclopropane rings, so it was abandoned as a default and
   *        not recommended
   * @param addHsAndBond
   *        standard operation for non-xtal systems
   * @param isClick
   *        whether this is a click or not
   * @return atomicNumber or -1
   */
  private int assignAtom(int atomIndex, String type, boolean autoBond,
                         boolean addHsAndBond, boolean isClick) {

    if (isClick) {
      if (vwr.isModelkitPickingRotateBond()) {
        bondAtomIndex1 = atomIndex;
        return -1;
      }
      if (clickProcessAtom(atomIndex) || !clickToSetElement
          && vwr.ms.getAtom(atomIndex).getElementNumber() != 1)
        return -1;
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
    // added P as first char here allows setpicking assignatom_P to work
    int atomicNumber = ("PPlMiX".indexOf(type) > 0 ? -1
        : type.equals("Xx") ? 0
            : PT.isUpperCase(type.charAt(0))
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
    } else if (!type.equals(".") || !addHydrogens) {
      return -1; // uninterpretable
    }

    if (!addHsAndBond && !isDelete)
      return atomicNumber;

    // type = "." is for connect

    // 2) delete noncovalent bonds and attached hydrogens for that atom.

    if (!wasH)
      vwr.ms.removeUnnecessaryBonds(atom, isDelete);

    // 3) adjust distance from previous atom.

    double dx = 0;
    if (atom.getCovalentBondCount() == 1) {
      if (atomicNumber == 1) {
        dx = 1.0d;
      } else {
        dx = 1.5d;
      }
    }
    if (dx != 0) {
      V3d v = V3d.newVsub(atom, vwr.ms.at[atom.getBondedAtomIndex(0)]);
      double d = v.length();
      v.normalize();
      v.scale(dx - d);
      vwr.ms.setAtomCoordRelative(atomIndex, v.x, v.y, v.z);
    }

    BS bsA = BSUtil.newAndSetBit(atomIndex);

    if (isDelete) {
      vwr.deleteAtoms(bsA, false);
    }
    if (atomicNumber != 1 && autoBond) {

      // we no longer do this 

      // 4) clear out all atoms within 1.0 angstrom
      vwr.ms.validateBspf(false);
      bs = vwr.ms.getAtomsWithinRadius(1.0d, bsA, false, null, null);
      bs.andNot(bsA);
      if (bs.nextSetBit(0) >= 0)
        vwr.deleteAtoms(bs, false);

      // 5) attach nearby non-hydrogen atoms (rings)

      bs = vwr.getModelUndeletedAtomsBitSet(atom.mi);
      bs.andNot(vwr.ms.getAtomBitsMDa(T.hydrogen, null, new BS()));
      vwr.ms.makeConnections2(0.1d, 1.8d, 1, T.create, bsA, bs, null, false,
          false, 0, null);

      // 6) add hydrogen atoms

    }

    if (addHydrogens)
      vwr.addHydrogens(bsA, Viewer.MIN_SILENT);
    return atomicNumber;
  }

  /**
   * Original ModelKit functionality -- assign a bond.
   * 
   * @param bondIndex
   * @param bondOrder
   * @param bsAtoms
   * @return bit set of atoms to modify
   */
  private boolean assignBond(int bondIndex, int bondOrder, BS bsAtoms) {
    Bond bond = vwr.ms.bo[bondIndex];
    vwr.ms.clearDB(bond.atom1.i);
    if (bondOrder < 0)
      return false;
    try {
      boolean a1H = (bond.atom1.getElementNumber() == 1);
      boolean isH = (a1H || bond.atom2.getElementNumber() == 1);
      if (isH && bondOrder > 1) {
        vwr.deleteAtoms(BSUtil.newAndSetBit(a1H ? bond.atom1.i : bond.atom2.i),
            false);
        return true;
      }
      if (bondOrder == 0) {
        vwr.deleteBonds(BSUtil.newAndSetBit(bond.index));
      } else {
        bond.setOrder(bondOrder | Edge.BOND_NEW);
        if (!isH) {
          vwr.ms.removeUnnecessaryBonds(bond.atom1, false);
          vwr.ms.removeUnnecessaryBonds(bond.atom2, false);
        }
      }
    } catch (Exception e) {
      Logger.error("Exception in seBondOrder: " + e.toString());
    }
    if (bondOrder != 0 && addHydrogens)
      vwr.addHydrogens(bsAtoms, Viewer.MIN_SILENT);
    return true;
  }

  private void assignBondAndType(int bondIndex, int bondOrder, char type,
                                 String cmd) {

    // from CmdExt

    int modelIndex = -1;
    int state = getMKState();
    try {
      setMKState(STATE_MOLECULAR);
      Atom a1 = vwr.ms.bo[bondIndex].atom1;
      modelIndex = a1.mi;
      int ac = vwr.ms.ac;
      BS bsAtoms = BSUtil.newAndSetBit(a1.i);
      bsAtoms.set(vwr.ms.bo[bondIndex].atom2.i);
      vwr.sm.setStatusStructureModified(bondIndex, modelIndex,
          Viewer.MODIFY_ASSIGN_BOND, cmd, 1, bsAtoms);
      //boolean ok = 
      assignBond(bondIndex, bondOrder, bsAtoms);
      vwr.ms.setAtomNamesAndNumbers(a1.i, -ac, null, true);
      //fails to refresh in JavaScript if we don't do this here      if (!ok || type == '0')
      vwr.refresh(Viewer.REFRESH_SYNC_MASK, "setBondOrder");
      vwr.sm.setStatusStructureModified(bondIndex, modelIndex,
          -Viewer.MODIFY_ASSIGN_BOND, "" + type, 1, bsAtoms);
    } catch (Exception ex) {
      Logger.error("assignBond failed");
      vwr.sm.setStatusStructureModified(bondIndex, modelIndex,
          -Viewer.MODIFY_ASSIGN_BOND, "ERROR " + ex, 1, null);
    } finally {
      setMKState(state);
    }
  }

  /**
   * Move atom iatom to a new position.
   * 
   * @param iatom
   * @param pt
   * @param bsFixed
   * @param bsModelAtoms
   * @param bsMoved
   * @return number of atoms moved
   */
  private int assignMoveAtom(int iatom, P3d pt, BS bsFixed, BS bsModelAtoms,
                             BS bsMoved) {
    // check to see if a constraint has stopped this changae
    if (Double.isNaN(pt.x) || iatom < 0)
      return 0;
    // check that this is an atom in the current model set.
    // must be an atom in the current model set
    BS bs = BSUtil.newAndSetBit(iatom);
    if (bsModelAtoms == null)
      bsModelAtoms = vwr.getThisModelAtoms();
    bs.and(bsModelAtoms);
    if (bs.isEmpty())
      return 0;
    int state = getMKState();
    setMKState(STATE_MOLECULAR);
    int n = 0;
    try {
      // check for locked atoms
      SymmetryInterface sym = getSym(iatom);
      BS bseq = new BS();
      vwr.ms.getSymmetryEquivAtomsForAtom(iatom, null, bsModelAtoms, bseq);
      if (setConstraint(sym, bseq.nextSetBit(0),
          GET_CREATE).type == Constraint.TYPE_LOCKED) {
        return 0;
      }
      if (bsFixed != null && !bsFixed.isEmpty())
        bseq.andNot(bsFixed);
      n = bseq.cardinality();
      if (n == 0) {
        return 0;
      }
      // checking here that the new point has not moved to a special position
      Atom a = vwr.ms.at[iatom];
      int[] v0 = sym.getInvariantSymops(a, null);
      int[] v1 = sym.getInvariantSymops(pt, v0);
      if ((v1 == null) != (v0 == null) || !Arrays.equals(v0, v1))
        return 0;
      P3d[] points = new P3d[n];
      // If this next call fails, then we have a serious problem. 
      // An operator was not found for one of the atoms that transforms it
      // into its presumed symmetry-equivalent atom
      int ia0 = bseq.nextSetBit(0);
      if (!fillPointsForMove(sym, bseq, ia0, a, pt, points)) {
        return 0;
      }
      bsMoved.or(bseq);
      int mi = vwr.ms.at[ia0].mi;
      vwr.sm.setStatusStructureModified(ia0, mi, Viewer.MODIFY_SET_COORD,
          "dragatom", n, bseq);
      for (int k = 0, ia = bseq.nextSetBit(0); ia >= 0; ia = bseq
          .nextSetBit(ia + 1)) {
        P3d p = points[k++];
        vwr.ms.setAtomCoord(ia, p.x, p.y, p.z);
      }
      vwr.sm.setStatusStructureModified(ia0, mi, -Viewer.MODIFY_SET_COORD,
          "dragatom", n, bseq);
      return n;
    } catch (Exception e) {
      System.err.println("Modelkit err" + e);
      return 0;
    } finally {
      setMKState(state);
      if (n > 0) {
        updateDrawAtomSymmetry(JC.PROP_ATOMS_MOVED, bsMoved);
      }

    }
  }

  /**
   * Move all atoms that are equivalent to this atom PROVIDED that they have the
   * same symmetry-invariant properties.
   * 
   * @param sym
   * @param bsSelected
   *        could be a single atom or a molecule, probably not an occupational
   *        partner
   * @param bsFixed
   * @param bsModelAtoms
   * @param iatom
   *        atom index
   * @param p
   *        new position for this atom, which may be modified
   * @param pts
   *        a set of points to drive each individual atom to; either from this
   *        method or from minimization
   * @param allowProjection
   *        always true here
   * @param isMolecule
   * @return number of atoms moved
   */
  private int assignMoveAtoms(SymmetryInterface sym, BS bsSelected, BS bsFixed,
                              BS bsModelAtoms, int iatom, P3d p, P3d[] pts,
                              boolean allowProjection, boolean isMolecule) {
    if (sym == null)
      sym = getSym(iatom);
    int npts = bsSelected.cardinality();
    if (npts == 0)
      return 0;
    int n = 0;
    int i0 = bsSelected.nextSetBit(0);
    if (bsFixed == null)
      bsFixed = vwr.getMotionFixedAtoms(sym, null);
    if (bsModelAtoms == null)
      bsModelAtoms = vwr.getModelUndeletedAtomsBitSet(vwr.ms.at[i0].mi);
    if (pts != null) {
      // must be a minimization or back here from below
      if (npts != pts.length)
        return 0;
      BS bs = new BS();
      for (int ip = 0, i = bsSelected.nextSetBit(0); i >= 0; i = bsSelected
          .nextSetBit(i + 1)) {
        // circle back into the method with each atom
        bs.clearAll();
        bs.set(i);
        n += assignMoveAtoms(sym, bs, bsFixed, bsModelAtoms, i, pts[ip++], null,
            true, isMolecule);
      }
      return n;
    }

    // no points here, but very possibly a molecule

    int nAtoms = bsSelected.cardinality();
    if (bsSelected.intersects(bsFixed)) {
      // abort - fixed or multiple atoms and not P1
      p.x = Float.NaN;
      return 0;
    }

    // a) add in any occupationally identical atoms so as not to separate occupational components

    addOccupiedAtomsToBitset(bsSelected);
    nAtoms = bsSelected.cardinality();

    // b) check for just one atom

    if (nAtoms == 1 && !isMolecule) {
      BS bsMoved = moveConstrained(iatom, bsFixed, bsModelAtoms, p, true,
          allowProjection, null);
      return (bsMoved == null ? 0 : bsMoved.cardinality());
    }

    // c) check that we can move this particular atom and get the change

    P3d p1 = P3d.newP(p);
    p.x = Float.NaN; // set "handled"
    if (moveConstrained(iatom, bsFixed, bsModelAtoms, p1, false, true,
        null) == null) {
      return 0;
    }
    V3d vrel = V3d.newV(p1);
    vrel.sub(vwr.ms.at[iatom]);

    P3d[] apos0 = vwr.ms.saveAtomPositions();

    // d) if drag-molecule, ensure we have the largest representative molecules for all the sites

    BS bsAll = BSUtil.copy(bsSelected);
    if (isMolecule) {
      BS bsTest = BSUtil.copy(bsModelAtoms);
      bsTest.andNot(bsSelected);
      BS bsSites = new BS();
      for (int i = bsSelected.nextSetBit(0); i >= 0; i = bsSelected
          .nextSetBit(i + 1)) {
        bsSites.set(vwr.ms.at[i].getAtomSite());
      }
      for (int i = bsTest.nextSetBit(0); i >= 0; i = bsTest.nextSetBit(i + 1)) {
        if (bsSites.get(vwr.ms.at[i].getAtomSite())) {
          BS bs = vwr.ms.getMoleculeBitSetForAtom(i);
          n = bs.cardinality();
          if (n > nAtoms) {
            nAtoms = n;
            bsTest.andNot(bs);
            bsAll = bs;
          }
        }
      }
      if (!bsAll.equals(bsSelected))
        vwr.select(bsAll, false, 0, true);
    }
    P3d[] apos = new P3d[bsAll.cardinality()];

    // e) dissect the molecule into independent sets of nonequivalent positions

    int maxSite = 0;
    for (int i = bsAll.nextSetBit(0); i >= 0; i = bsAll.nextSetBit(i + 1)) {
      int s = vwr.ms.at[i].getAtomSite();
      if (s > maxSite)
        maxSite = s;
    }
    int[] sites = new int[maxSite];
    pts = new P3d[maxSite];

    // f) preview all changes to make sure that they are allowed for every atom - any locking nullifies.

    BS bsMoved = new BS();
    for (int ip = 0, i = bsAll.nextSetBit(0); i >= 0; i = bsAll
        .nextSetBit(i + 1), ip++) {
      p1.setT(vwr.ms.at[i]);
      p1.add(vrel);
      apos[ip] = P3d.newP(p1);
      int s = vwr.ms.at[i].getAtomSite() - 1;
      if (sites[s] == 0) {
        if (moveConstrained(i, bsFixed, bsModelAtoms, p1, false, true,
            bsMoved) == null) {
          return 0;
        }
        p1.sub(vwr.ms.at[i]);
        p1.sub(vrel);
        pts[s] = P3d.newP(p1);
        // this should be {0 0 0}, meaning all atom sites were moved the same distance but I have not tested it
        //        System.out.println("P1-pd"  + p1);
        sites[s] = i + 1;
      }
    }

    // g) carry out the changes. This next part ensures that translationally symmetry-related
    //    atoms are also moved. (As in graphite layers top and bottom, moving top also moves bottom.)

    bsMoved.clearAll();
    for (int i = sites.length; --i >= 0;) {
      int ia = sites[i] - 1;
      if (ia >= 0) {
        p1.setT(vwr.ms.at[ia]);
        p1.add(vrel);
        if (moveConstrained(ia, bsFixed, bsModelAtoms, p1, true, true,
            bsMoved) == null) {
          bsMoved = null;
          break;
        }
      }
    }
    n = (bsMoved == null ? 0 : bsMoved.cardinality());

    // h) check that all selected atoms have been moved appropriately

    if (n == 0) {
      vwr.ms.restoreAtomPositions(apos0);
      return 0;
    }
    return (checkAtomPositions(apos0, apos, bsAll) ? n : 0);
  }

  /**
   * Check that atom positions are moved appropriately (within 0.0001 Angstrom)
   * and if not, revert all positions.
   * 
   * @param apos0
   * @param apos
   * @param bs
   * @return true if OK
   */
  private boolean checkAtomPositions(P3d[] apos0, P3d[] apos, BS bs) {
    boolean ok = true;
    for (int ip = 0, i = bs.nextSetBit(0); i >= 0; i = bs
        .nextSetBit(i + 1), ip++) {
      if (vwr.ms.at[i].distanceSquared(apos[ip]) > 0.00000001d) {
        ok = false;
        break;
      }
    }
    if (ok)
      return true;
    vwr.ms.restoreAtomPositions(apos0);
    return false;
  }

  /**
   * An atom has been clicked -- handle it. Called from CmdExt.assignAtom from
   * the script created in ActionManager.assignNew from
   * Actionmanager.checkReleaseAction
   * 
   * @param atomIndex
   * @return true if handled
   */
  private boolean clickProcessAtom(int atomIndex) {
    switch (getMKState()) {
    case STATE_MOLECULAR:
      return vwr.isModelkitPickingRotateBond();
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

  private void clickProcessMode(String action) {
    processMKPropertyItem(action, false);
  }

  private void clickProcessSel(String action) {
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
      P3d p = pointFromTriad(pos);
      if (p == null) {
        clickProcessSel(action);
        return;
      }
      centerAtomIndex = -Integer.MAX_VALUE;
      centerPoint = p;
      showXtalSymmetry();
    }
  }

  private String clickProcessSelOp(String action) {
    secondAtomIndex = -1;
    if (action == "mkselop_addoffset") {
      String pos = promptUser(
          "Enter i j k for an offset for viewing the operator - leave blank to clear",
          lastOffset);
      if (pos == null)
        return null;
      lastOffset = pos;
      if (pos.length() == 0 || pos == "none") {
        setProperty(JC.MODELKIT_OFFSET, "none");
        return null;
      }
      P3d p = pointFromTriad(pos);
      if (p == null) {
        return action;
      }
      setProperty(JC.MODELKIT_OFFSET, p);
    } else if (action == "mkselop_atom2") {
      notImplemented(action);
    }
    return null;
  }

  private void clickProcessSym(String action) {
    if (action == "mksymmetry_none") {
      setSymEdit(STATE_SYM_NONE);
    } else {
      processMKPropertyItem(action, false);
    }
  }

  private void clickProcessUC(String action) {
    processMKPropertyItem(action, false);
    showXtalSymmetry();
  }

  private void connectAtoms(double bd, int bondOrder, BS bs1, BS bs2) {
    vwr.makeConnections((bd - 0.01d), (bd + 0.01d), bondOrder, T.modifyorcreate,
        bs1, bs2, new BS(), false, false, 0);
  }

  /**
   * Find the operator that transforms fractional point fa to one of its
   * symmetry-equivalent points, and then also transform pt by that same matrix.
   * Optionally, save the transformed points in a compact array.
   * 
   * @param sg
   * @param bseq
   * @param i0
   *        // basis atom index
   * @param a
   * @param pt
   * @param points
   * @return false if there is a failure to find a transform
   */
  private boolean fillPointsForMove(SymmetryInterface sg, BS bseq, int i0,
                                    P3d a, P3d pt, P3d[] points) {
    double d = a.distance(pt);
    P3d fa = P3d.newP(a);
    P3d fb = P3d.newP(pt);
    sg.toFractional(fa, false);
    sg.toFractional(fb, false);
    for (int k = 0, i = i0; i >= 0; i = bseq.nextSetBit(i + 1)) {
      P3d p = P3d.newP(vwr.ms.at[i]);
      P3d p0 = P3d.newP(p);
      sg.toFractional(p, false);
      M4d m = sg.getTransform(fa, p, false);
      if (m == null) {
        return false;
      }
      P3d p2 = P3d.newP(fb);
      m.rotTrans(p2);
      sg.toCartesian(p2, false);
      if (Math.abs(d - p0.distance(p2)) > 0.001d)
        return false;
      points[k++] = p2;
    }
    fa.setT(points[0]);
    sg.toFractional(fa, false);
    // check for sure that all new positions are also OK
    for (int k = points.length; --k >= 0;) {
      fb.setT(points[k]);
      sg.toFractional(fb, false);
      M4d m = sg.getTransform(fa, fb, false);
      if (m == null) {
        //        m = sg.getTransform(fa, fb, true);
        return false;
      }
      for (int i = points.length; --i > k;) {
        if (points[i].distance(points[k]) < 0.1d)
          return false;
      }
    }
    return true;
  }

  private String getBondLabel(Atom[] atoms) {
    return atoms[Math.min(bondAtomIndex1, bondAtomIndex2)].getAtomName() + "-"
        + atoms[Math.max(bondAtomIndex1, bondAtomIndex2)].getAtomName();
  }

  private int getBondOrder(char type, Bond bond) {
    if (type == '-')
      type = pickBondAssignType;
    int bondOrder = type - '0';
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
      return -1;
    }
    return bondOrder;
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
      //      boolean isBondedAtom = (atomIndex == bondAtomIndex1
      //          || atomIndex == bondAtomIndex2);
      if (isRotateBond) {
        setBranchAtom(atomIndex, false);
        msg = (branchAtomIndex >= 0
            ? "rotate branch " + atoms[atomIndex].getAtomName()
            : "rotate bond for " + getBondLabel(atoms));
      }
      if (bondIndex < 0// || atomIndex >= 0 && !isBondedAtom
      ) {
        if (atomHoverLabel.length() <= 2) {
          msg = atomHoverLabel = "Click to change to " + atomHoverLabel
              + " or drag to add " + atomHoverLabel;
        } else {
          msg = atoms[atomIndex].getAtomName() + ": " + atomHoverLabel;
          vwr.highlight(BSUtil.newAndSetBit(atomIndex));
        }
      } else {
        if (msg == null) {
          switch (isRotateBond ? bsHighlight.cardinality()
              : atomIndex >= 0 ? 1 : -1) {
          case 0:
            vwr.highlight(BSUtil.newAndSetBit(atomIndex));
            //$FALL-THROUGH$
          case 1:
          case 2:
            Atom a = vwr.ms.at[atomIndex];
            if (!isRotateBond) {
              menu.setActiveMenu(ATOM_MODE);
              if (vwr.acm
                  .getAtomPickingMode() == ActionManager.PICKING_IDENTIFY)
                return null;
            }
            if (atomHoverLabel.indexOf("charge") >= 0) {
              int ch = a.getFormalCharge();
              ch += (atomHoverLabel.indexOf("increase") >= 0 ? 1 : -1);
              msg = atomHoverLabel + " to " + (ch > 0 ? "+" : "") + ch;
            } else {
              msg = atomHoverLabel;
            }
            msg = atoms[atomIndex].getAtomName() + ": " + msg;
            break;
          case -1:
            msg = (bondHoverLabel.length() == 0 ? "" : bondHoverLabel + " for ")
                + getBondLabel(atoms);
            break;
          }
        }
      }
      break;
    }
    return msg;
  }

  private Object getinfo() {
    Map<String, Object> info = new Hashtable<String, Object>();
    addInfo(info, "addHydrogens", Boolean.valueOf(addHydrogens));
    addInfo(info, "autobond", Boolean.valueOf(autoBond));
    addInfo(info, "clickToSetElement", Boolean.valueOf(clickToSetElement));
    addInfo(info, "hidden", Boolean.valueOf(menu.hidden));
    addInfo(info, "showSymopInfo", Boolean.valueOf(showSymopInfo));
    addInfo(info, "centerPoint", centerPoint);
    addInfo(info, "centerAtomIndex", Integer.valueOf(centerAtomIndex));
    addInfo(info, "secondAtomIndex", Integer.valueOf(secondAtomIndex));
    addInfo(info, "symop", symop);
    addInfo(info, "offset", viewOffset);
    addInfo(info, "drawData", drawData);
    addInfo(info, "drawScript", drawScript);
    addInfo(info, "isMolecular",
        Boolean.valueOf(getMKState() == STATE_MOLECULAR));
    return info;
  }

  private static Atom getNearestBondedAtom(Atom a1, Atom butNotThis) {
    Bond[] b = a1.bonds;
    Atom a2;
    Atom ret = null;
    int zmin = Integer.MAX_VALUE;
    for (int i = a1.getBondCount(); --i >= 0;) {
      if (b[i] != null && b[i].isCovalent()
          && (a2 = b[i].getOtherAtom(a1)) != butNotThis && a2.sZ < zmin) {
        zmin = a2.sZ;
        ret = a2;
      }
    }
    return ret;
  }

  /**
   * 
   * @param countPlusIndices
   * @return false only if the state is MOLECULAR
   */
  private boolean handleAtomDragging(int[] countPlusIndices) {
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

  private boolean handleAtomOrBondPicked(int x, int y, MeasurementPending mp,
                                         int dragAtomIndex, String atomType,
                                         boolean inRange) {
    boolean isCharge = isPickAtomAssignCharge;
    if (mp != null && mp.count == 2) {
      // bond
      vwr.undoMoveActionClear(-1, T.save, true);
      Atom a = (Atom) mp.getAtom(1);
      Atom b = (Atom) mp.getAtom(2);
      Bond bond = a.getBond(b);
      if (bond == null) {
        cmdAssignConnect(a.i, b.i, '1', "click");
      } else {
        cmdAssignBond(bond.index, 'p', "click");
      }
    } else {
      // atom
      if (atomType.equals("Xx")) {
        atomType = lastElementType;
      }
      if (inRange) {
        vwr.undoMoveActionClear(dragAtomIndex,
            AtomCollection.TAINT_FORMALCHARGE, true);
      } else {
        vwr.undoMoveActionClear(-1, T.save, true);
      }
      Atom a = vwr.ms.at[dragAtomIndex];
      boolean wasH = (a != null && a.getElementNumber() == 1);
      clickAssignAtom(dragAtomIndex, atomType, null);
      if (isCharge) {
        appRunScript("{atomindex=" + dragAtomIndex + "}.label='%C'");
      } else {
        vwr.undoMoveActionClear(-1, T.save, true);
        if (a == null || inRange)
          return false;
        if (wasH) {
          clickAssignAtom(dragAtomIndex, "X", null);
        } else {
          P3d ptNew = P3d.new3(x, y, a.sZ);
          vwr.tm.unTransformPoint(ptNew, ptNew);
          clickAssignAtom(dragAtomIndex, atomType, ptNew);
        }
      }
    }
    // no update necessary here?
    return true;
  }

  /**
   * The idea here is to load the model with {444 666 1} cells in order to
   * create a range around the unit cell that will be locked. Hard to say if
   * this will really work.
   * 
   * @param eval
   * @param bsBasis
   * @param steps
   * @param crit
   * @param rangeFixed
   * @param flags
   * @throws Exception
   */
  private void minimizeXtal(JmolScriptEvaluator eval, BS bsBasis, int steps,
                            double crit, double rangeFixed, int flags)
      throws Exception {
    // original structure
    minBasisModel = vwr.am.cmi;
    minSelectionSaved = vwr.bsA();
    vwr.am.setFrame(minBasisModel);
    minBasis = bsBasis;
    minBasisFixed = vwr.getMotionFixedAtoms(null, null);
    minBasisModelAtoms = vwr.getModelUndeletedAtomsBitSet(minBasisModel);

    String cif = vwr.getModelExtract(bsBasis, false, false, "cif");
    // TODO what about Async exception?
    int tempModelIndex = vwr.ms.mc;
    Map<String, Object> htParams = new Hashtable<String, Object>();
    htParams.put("eval", eval);
    htParams.put("lattice", P3d.new3(444, 666, 1));
    htParams.put("fileData", cif);
    htParams.put("loadScript", new SB());
    if (vwr.loadModelFromFile(null, "<temp>", null, null, true, htParams, null,
        null, 0, " ") != null)
      return;
    BS bsBasis2 = BSUtil.copy(vwr.ms.am[tempModelIndex].bsAsymmetricUnit);
    minTempModelAtoms = vwr.getModelUndeletedAtomsBitSet(tempModelIndex);
    // new structure
    vwr.am.setFrame(tempModelIndex);
    minTempFixed = BSUtil.copy(minTempModelAtoms);
    minTempFixed.andNot(bsBasis2);
    vwr.getMotionFixedAtoms(null, minTempFixed);
    vwr.minimize(eval, steps, crit, BSUtil.copy(bsBasis2), minTempFixed,
        minTempModelAtoms, rangeFixed, flags & ~Viewer.MIN_MODELKIT);
  }

  private void minimizeXtalEnd(BS bsBasis2, boolean isEnd) {
    if (minBasis == null)
      return; // not a new structure
    if (bsBasis2 != null) {
      P3d[] pts = new P3d[bsBasis2.cardinality()];
      for (int p = 0, j = minBasis.nextSetBit(0), i = bsBasis2
          .nextSetBit(0); i >= 0; i = bsBasis2
              .nextSetBit(i + 1), j = minBasis.nextSetBit(j + 1)) {
        pts[p++] = P3d.newP(vwr.ms.at[i].getXYZ());
      }
      BS bs = BSUtil.copy(minBasis);
      bs.andNot(minBasisFixed);
      assignMoveAtoms(null, bs, minBasisFixed, minBasisModelAtoms,
          minBasis.nextSetBit(0), null, pts, true, false);
    }
    if (isEnd) {
      minSelectionSaved = null;
      minBasis = null;
      minBasisFixed = null;
      minTempFixed = null;
      minTempModelAtoms = null;
      minBasisModelAtoms = null;
      minBasisAtoms = null;
      modelSyms = null;
      vwr.deleteModels(vwr.ms.mc - 1, null);
      vwr.setSelectionSet(minSelectionSaved);
      vwr.setCurrentModelIndex(minBasisModel);
    }
  }

  /**
   * This is the main method from viewer.moveSelected.
   * 
   * @param iatom
   * @param bsFixed
   * @param bsModelAtoms
   * @param ptNew
   *        new "projected" position set here; x set to NaN if handled here
   * @param doAssign
   *        allow for exit with setting ptNew but not creating atoms
   * @param allowProjection
   * @param bsMoved
   *        initial moved, or null
   * @return number of atoms moved or checked
   */
  private BS moveConstrained(int iatom, BS bsFixed, BS bsModelAtoms, P3d ptNew,
                             boolean doAssign, boolean allowProjection,
                             BS bsMoved) {
    SymmetryInterface sym = getSym(iatom);
    if (sym == null) {
      // molecular crystals loaded without packed or centroid will not have operations
      return null;
    }
    if (bsMoved == null)
      bsMoved = BSUtil.newAndSetBit(iatom);
    Atom a = vwr.ms.at[iatom];
    Constraint c = constraint;
    M4d minv = null;
    if (c == null) {
      c = setConstraint(sym, iatom, GET_CREATE);
      if (c.type == Constraint.TYPE_LOCKED) {
        iatom = -1;
      } else {
        // transform the shift to the basis
        Atom b = getBasisAtom(iatom);
        if (a != b) {
          M4d m = getTransform(sym, a, b);
          if (m == null) {
            System.err.println(
                "ModelKit - null matrix for " + iatom + " " + a + " to " + b);
            iatom = -1;
          } else {
            if (!doAssign) {
              minv = M4d.newM4(m);
              minv.invert();
            }
            iatom = b.i;
            P3d p = P3d.newP(ptNew);
            sym.toFractional(p, false);
            m.rotTrans(p);
            sym.toCartesian(p, false);
            ptNew.setT(p);
          }
        }
        if (iatom >= 0)
          c.constrain(b, ptNew, allowProjection);
      }
    } else {
      c.constrain(a, ptNew, allowProjection);
    }
    if (iatom >= 0 && !Double.isNaN(ptNew.x)) {
      if (!doAssign) {
        if (minv != null) {
          P3d p = P3d.newP(ptNew);
          sym.toFractional(p, false);
          minv.rotTrans(p);
          sym.toCartesian(p, false);
          ptNew.setP(p);
        }
        return bsMoved;
      }
      if (assignMoveAtom(iatom, ptNew, bsFixed, bsModelAtoms, bsMoved) == 0)
        bsMoved = null;
    }
    ptNew.x = Double.NaN; // indicate handled
    return bsMoved;
  }

  private String promptUser(String msg, String def) {
    return vwr.prompt(msg, def, null, false);
  }

  private void resetBondFields() {
    bsRotateBranch = null;
    // do not set bondIndex to -1 here
    branchAtomIndex = bondAtomIndex1 = bondAtomIndex2 = -1;
  }

  private int rotateAtoms(BS bsAtoms, P3d[] points, double endDegrees) {
    P3d center = points[0];
    P3d p = new P3d();
    int i0 = bsAtoms.nextSetBit(0);
    SymmetryInterface sg = getSym(i0);
    // (1) do not allow any locked atoms; skip equivalent positions
    BS bsAU = new BS();
    BS bsToMove = new BS();
    for (int i = i0; i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
      int bai = getBasisAtom(i).i;
      if (bsAU.get(bai)) {
        continue;
      }
      if (setConstraint(sg, bai, GET_CREATE).type == Constraint.TYPE_LOCKED) {
        return 0;
      }
      bsAU.set(bai);
      bsToMove.set(i);
    }
    // (2) save all atom positions in case we need to reset
    int nAtoms = bsAtoms.cardinality();
    P3d[] apos0 = vwr.ms.saveAtomPositions();
    // (3) get all new points and ensure that they are allowed
    M3d m = Qd.newVA(V3d.newVsub(points[1], points[0]), endDegrees).getMatrix();
    V3d vt = new V3d();
    P3d[] apos = new P3d[nAtoms];
    for (int ip = 0, i = i0; i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
      Atom a = vwr.ms.at[i];
      p = apos[ip++] = P3d.newP(a);
      vt.sub2(p, center);
      m.rotate(vt);
      p.add2(center, vt);
      setConstraint(sg, i, GET_CREATE).constrain(a, p, false);
      if (Double.isNaN(p.x))
        return 0;
    }
    // (4) move all symmetry-equivalent atoms
    nAtoms = 0;
    BS bsFixed = vwr.getMotionFixedAtoms(sg, null); // includes a callback to ModelKit
    BS bsModelAtoms = vwr.getModelUndeletedAtomsBitSet(vwr.ms.at[i0].mi);
    BS bsMoved = new BS();
    for (int ip = 0, i = i0; i >= 0; i = bsToMove.nextSetBit(i + 1), ip++) {
      nAtoms += assignMoveAtom(i, apos[ip], bsFixed, bsModelAtoms, bsMoved);
    }
    // (5) check to see that all equivalent atoms have been placed where they should be

    BS bs = BSUtil.copy(bsAtoms);
    bs.andNot(bsToMove);
    return (checkAtomPositions(apos0, apos, bs) ? nAtoms : 0);
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

  /**
   * Set the bond for rotation -- called by Sticks.checkObjectHovered via
   * Viewer.highlightBond.
   * 
   * 
   * @param index
   * @param isRotate
   */
  private void setBondIndex(int index, boolean isRotate) {
    if (!isRotate && vwr.isModelkitPickingRotateBond()) {
      setProperty(JC.MODELKIT_ROTATEBONDINDEX, Integer.valueOf(index));
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
    menu.setActiveMenu(BOND_MODE);
  }

  /**
   * @param atomIndex
   * @param isClick
   */
  private void setBranchAtom(int atomIndex, boolean isClick) {
    boolean isBondedAtom = (atomIndex == bondAtomIndex1
        || atomIndex == bondAtomIndex2);
    if (isBondedAtom) {
      branchAtomIndex = atomIndex;
      bsRotateBranch = null;
    } else {
      bsRotateBranch = null;
      branchAtomIndex = -1;
    }
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

  private void setSymEdit(int bits) {
    state = (state & ~STATE_BITS_SYM_EDIT) | bits;
  }

  private void setSymViewState(int bits) {
    state = (state & ~STATE_BITS_SYM_VIEW) | bits;
  }

  private void setUnitCell(int bits) {
    state = (state & ~STATE_BITS_UNITCELL) | bits;
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
      P3d offset = null;
      if (secondAtomIndex >= 0) {
        script = "draw ID sym symop "
            + (centerAtomIndex < 0 ? centerPoint
                : " {atomindex=" + centerAtomIndex + "}")
            + " {atomindex=" + secondAtomIndex + "}";
      } else {
        offset = viewOffset;
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
          ";refresh;set echo top right;echo " + drawData.replace('\t', ' '));
      break;
    }
  }

  /**
   * Transform the atoms to fractional coordinate, set the unit cell to a new
   * cell, and then transform them back to Cartesians.
   * 
   * @param sym
   * @param oabc
   * @param ucname
   * @return true if this is a "reset" with no atoms or sym == null
   */
  public boolean transformAtomsToUnitCell(SymmetryInterface sym, T3d[] oabc,
                                          String ucname) {
    BS bsAtoms = vwr.getThisModelAtoms();
    int n = bsAtoms.cardinality();
    boolean isReset = (sym == null || n == 0);
    if (!isReset) {
      Atom[] a = vwr.ms.at;
      P3d[] fxyz = getFractionalCoordinates(sym, bsAtoms);
      vwr.ms.setModelCagePts(-1, oabc, ucname);
      sym = vwr.getCurrentUnitCell();
      for (int j = bsAtoms.nextSetBit(0), k = 0; j >= 0; j = bsAtoms
          .nextSetBit(j + 1), k++) {
        a[j].setT(fxyz[k]);
        vwr.toCartesianUC(sym, a[j], false);
      }
      vwr.ms.setTaintedAtoms(bsAtoms, AtomCollection.TAINT_COORD);
    }
    return isReset;
  }

  /**
   * Grab the fractional coordinates of all atoms in a model (typically). Unit
   * cell offset is accounted for.
   * 
   * @param sym
   * @param bsAtoms
   * @return an array of fractional coordinates.
   */
  private P3d[] getFractionalCoordinates(SymmetryInterface sym, BS bsAtoms) {
    int n = bsAtoms.cardinality();
    Atom[] a = vwr.ms.at;
    P3d[] fxyz = new P3d[n];
    for (int j = bsAtoms.nextSetBit(0), k = 0; j >= 0; j = bsAtoms
        .nextSetBit(j + 1), k++) {
      fxyz[k] = P3d.newP(a[j]);
      vwr.toFractionalUC(sym, fxyz[k], false);
    }
    return fxyz;
  }

  /**
   * A class to maintain the connection between drawn symmetry business and sets
   * of atoms. This allows them to be adjusted on the fly.
   * 
   */
  private class DrawAtomSet {
    BS bsAtoms;
    String cmd;
    String id;

    DrawAtomSet(BS bs, String id, String cmd) {
      bsAtoms = bs;
      this.cmd = cmd;
      this.id = id;
    }

  }

  private Lst<DrawAtomSet> drawAtomSymmetry;

  private synchronized void updateDrawAtomSymmetry(String mode, BS atoms) {
    if (drawAtomSymmetry == null)
      return;
    String cmd = "";
    for (int i = drawAtomSymmetry.size(); --i >= 0;) {
      DrawAtomSet a = drawAtomSymmetry.get(i);
      if (mode == JC.PROP_DELETE_MODEL_ATOMS
          ? atoms.get(a.bsAtoms.nextSetBit(0))
          : atoms.intersects(a.bsAtoms)) {
        switch (mode) {
        case JC.PROP_DELETE_MODEL_ATOMS:
        case JC.PROP_ATOMS_DELETED:
          drawAtomSymmetry.removeItemAt(i);
          break;
        case JC.PROP_ATOMS_MOVED:
          try {
            if (!checkDrawID(a.id)) {
              drawAtomSymmetry.removeItemAt(i);
            } else {
              cmd += a.cmd + JC.SCRIPT_QUIET + "\n";
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
          break;
        }
        if (drawAtomSymmetry.size() == 0)
          drawAtomSymmetry = null;
      }
      if (cmd.length() > 0)
        vwr.evalStringGUI(cmd);
    }
  }

  private boolean checkDrawID(String id) {
    Object[] o = new Object[] { id + "*", null };
    boolean exists = vwr.shm.getShapePropertyData(JC.SHAPE_DRAW, "checkID", o);
    return (exists && o[1] != null);
  }

  /**
   * from set picking symop
   * 
   * @param a1
   * @param a2
   */
  public void drawSymop(int a1, int a2) {
    String s = "({" + a1 + "}) ({" + a2 + "}) ";
    String cmd = "draw ID 'sym' symop " + s;
    vwr.evalStringGUI(cmd);
  }

  public void addAtomSet(String data) {
    String[] tokens = PT.split(data, "|");
    String id = tokens[0];
    clearAtomSets(id);
    int a1 = PT.parseInt(tokens[1]);
    int a2 = PT.parseInt(tokens[2]);
    String cmd = tokens[3];
    BS bs = BSUtil.newAndSetBit(a1);
    bs.set(a2);
    if (drawAtomSymmetry == null) {
      drawAtomSymmetry = new Lst<DrawAtomSet>();
    }
    drawAtomSymmetry.addLast(new DrawAtomSet(bs, id, cmd));
  }

  private void clearAtomSets(String id) {
    if (drawAtomSymmetry == null)
      return;
    for (int i = drawAtomSymmetry.size(); --i >= 0;) {
      DrawAtomSet a = drawAtomSymmetry.get(i);
      if (a.id.equals(id)) {
        drawAtomSymmetry.remove(i);
        return;
      }
    }
  }

  /**
   * @param id
   * @param ucLattice
   * @param swidth
   */
  public void drawUnitCell(String id, T3d ucLattice, String swidth) {
    SymmetryInterface sym = vwr.getOperativeSymmetry();
    if (sym == null)
      return;
    SymmetryInterface uc = vwr.getSymTemp()
        .getUnitCell(sym.getUnitCellVectors(), false, "draw");
    uc.setOffsetPt(ucLattice);
    P3d[] cellRange = { new P3d(), new P3d() };
    String s = "";
    if (id == null)
      id = "uclat";
    Object[][] val = new Object[][] { { "thisID", id + "*" },
        { "delete", null } };
    vwr.shm.setShapeProperties(JC.SHAPE_DRAW, val);

    SimpleUnitCell.getCellRange(ucLattice, cellRange);
    for (int p = 1, x = (int) cellRange[0].x; x < cellRange[1].x; x++) {
      for (int y = (int) cellRange[0].y; y < cellRange[1].y; y++) {
        for (int z = (int) cellRange[0].z; z < cellRange[1].z; z++, p++) {
          s += "\ndraw ID " + PT.esc(id + "_" + p) + " " + swidth
              + " unitcell \"a,b,c;" + x + "," + y + "," + z + "\"";
        }
      }
    }
    s += getDrawAxes(id, swidth);
    appRunScript(s);
  }

  public void drawAxes(String id, String swidth) {
    String s = getDrawAxes(id, swidth);
    if (s.length() > 0)
      appRunScript(s);
  }

  private String getDrawAxes(String id, String swidth) {
    SymmetryInterface sym = vwr.getOperativeSymmetry();
    if (sym == null)
      return "";
    if (vwr.g.axesMode != T.axesunitcell || vwr.shm
        .getShapePropertyIndex(JC.SHAPE_AXES, "axesTypeXY", 0) == Boolean.TRUE)
      return "";
    if (id == null)
      id = "uca";
    if (swidth.indexOf(".") > 0)
      swidth += "05";
    P3d origin = (P3d) vwr.shm.getShapePropertyIndex(JC.SHAPE_AXES,
        "originPoint", 0);
    P3d[] axisPoints = (P3d[]) vwr.shm.getShapePropertyIndex(JC.SHAPE_AXES,
        "axisPoints", 0);
    String s = "";
    String[] colors = new String[] { "red", "green", "blue" };
    int nDim = sym.getDimensionality();
    int per = sym.getPeriodicity();
    boolean shiftA = (per == 0x4 && nDim == 3); // rod group
    boolean shiftB = (shiftA || per == 0x1 && nDim == 2); // rod or frieze
    boolean shiftC = (per == 0x1 && nDim == 2 || per == 0x3 && nDim == 3); // frieze or layer
    P3d[] pts = axisPoints;
    P3d[] opts = null;
    if (shiftA || shiftB || shiftC) {
      pts = new P3d[] { new P3d(), new P3d(), new P3d() };
      opts = new P3d[] { new P3d(), new P3d(), new P3d() };
      if (shiftA) {
        opts[0].sub2(origin, axisPoints[0]);
        opts[0].scale(0.5d);
        pts[0].sub2(origin, opts[0]);
      }
      if (shiftB) {
        opts[1].sub2(origin, axisPoints[1]);
        opts[1].scale(0.5d);
        pts[1].sub2(origin, opts[1]);
      }
      if (shiftC) {
        opts[2].sub2(origin, axisPoints[2]);
        opts[2].scale(0.5d);
        pts[2].sub2(origin, opts[2]);
      }
    }
    for (int i = 0, a = JC.AXIS_A; i < 3; i++, a++) {
      s += "\ndraw ID " + PT.esc(id + "_axis_" + JC.axisLabels[a]) + " "
          + swidth + " line "
          + (opts == null 
          || i == 0 && !shiftA
          || i == 1 && !shiftB ? origin : opts[i])
          + " " + pts[i] + " color " + colors[i];
    }
    return s;
  }

  public String drawSymmetry(String thisId, boolean isSymop, int iatom,
                             String xyz, int iSym, P3d trans, P3d center,
                             P3d target, int intScale, int nth, int options,
                             int[] opList, boolean isModelkit) {
    String s = null;
    if (options != 0) {
      // options is T.offset, and target is an {i j k} offset from cell 555
      Object o = vwr.getSymmetryInfo(iatom, xyz, iSym, trans, center, target,
          T.point, null, intScale / 100d, nth, options, opList);
      if (o instanceof P3d)
        target = (P3d) o;
      else
        s = "";
    }
    if (thisId == null)
      thisId = (isSymop ? "sym" : "sg");
    if (s == null)
      s = (String) vwr.getSymmetryInfo(iatom, xyz, iSym, trans, center, target,
          T.draw, thisId, intScale / 100, nth, options, opList);
    if (s != null) {
      s = "draw ID \"" + (isSymop ? "sg" : "sym") + "*\" delete;" + s;
      s = "draw ID \"" + thisId + "*\" delete;" + s;
    }
    if (isModelkit)
      s += ";draw ID sg_xes axes 0.05;";
    return s;
  }


  /**
   * MODELKIT SPACEGROUP
   * 
   * @param transform
   * @param sb
   * 
   * @param isPacked
   * @param doDraw
   * @param cmd
   */
  public void cmdAssignSpaceGroup(String transform, SB sb, String cmd,
                                  boolean isPacked, boolean doDraw) {
    clearAtomConstraints();
    if (isPacked) {
      int n;
      if (doDraw) {
        n = cmdAssignAddAtoms("N:G", null, null, "packed", cmd);
      } else {
        BS bsModelAtoms = vwr.getThisModelAtoms();
        n = packAssignedSpaceGroup(bsModelAtoms, transform, cmd);
      }
      sb.append("\n").append(GT.i(GT.$("{0} atoms added"), n));
    }
    if (doDraw) {
      String s = drawSymmetry("sg", false, -1, null, Integer.MAX_VALUE, null,
          null, null, 0, -2, 0, null, true);
      appRunScript(s);
    }
  }

  /**
   * 
   * (final part, the packing)
   * 
   * This is a rather complicated process, involving temporarily adding
   * "centering-like" operations that propagate atoms from one former unit cell
   * to others in a klassengleiche (crystal class-preserving) transformation in
   * which lattice translations have been removed, leading to a sort of general
   * position splitting.
   *
   * augment the operations with ones derived from checking the scope of the
   * tranformation.
   * 
   * add all the atoms
   * 
   * set all the model atom basis and symmetry issues
   * 
   * the space group itself is unchanged here.
   * 
   * @param bsAtoms
   * @param transform
   * @param cmd
   * @return the number of new atoms
   */
  private int packAssignedSpaceGroup(BS bsAtoms, String transform, String cmd) {
    SymmetryInterface sym = vwr.getOperativeSymmetry();
    if (sym == null)
      return 0;
    M4d[] opsCtr = (M4d[]) sym.getSpaceGroupInfoObj("opsCtr", transform, false,
        false);
    int n0 = bsAtoms.cardinality();
    addAtoms(null, null, bsAtoms, "packed", opsCtr, cmd);
    vwr.ms.setSpaceGroup(vwr.am.cmi, sym, new BS());
    return vwr.getThisModelAtoms().cardinality() - n0;
  }
   
}
