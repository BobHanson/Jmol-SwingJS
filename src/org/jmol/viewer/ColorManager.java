/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2017-10-07 09:28:54 -0500 (Sat, 07 Oct 2017) $
 * $Revision: 21710 $
 *
 * Copyright (C) 2003-2006  Miguel, Jmol Development, www.jmol.org
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

import org.jmol.c.PAL;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.Model;
import org.jmol.modelset.ModelSet;
import org.jmol.modelsetbio.BioModel;
import org.jmol.script.T;
import org.jmol.util.C;
import org.jmol.util.ColorEncoder;
import org.jmol.util.Elements;
import org.jmol.util.GData;
import org.jmol.util.Logger;

import javajs.util.AU;
import javajs.util.BS;


public class ColorManager {

  /*
   * ce is a "master" colorEncoded. It will be used
   * for all atom-based schemes (Jmol, Rasmol, shapely, etc.)
   * and it will be the 
   * 
   * 
   */

  public ColorEncoder ce;
  private Viewer vwr;
  private GData g3d;

  // for atoms -- color CPK:

  private int[] argbsCpk;
  private int[] altArgbsCpk;

  // for properties.

  private double[] colorData;

  ColorManager(Viewer vwr, GData gdata) {
    this.vwr = vwr;
    ce = new ColorEncoder(null, vwr);
    g3d = gdata;
    argbsCpk = PAL.argbsCpk;
    altArgbsCpk = AU.arrayCopyRangeI(JC.altArgbsCpk, 0, -1);
  }

//  void clear() {
//    //causes problems? flushCaches();
//  }

  boolean isDefaultColorRasmol;

  void setDefaultColors(boolean isRasmol) {
    if (isRasmol) {
      isDefaultColorRasmol = true;
      argbsCpk = AU.arrayCopyI(ColorEncoder.getRasmolScale(), -1);
    } else {
      isDefaultColorRasmol = false;
      argbsCpk = PAL.argbsCpk;
    }
    altArgbsCpk = AU.arrayCopyRangeI(JC.altArgbsCpk, 0, -1);
    ce.createColorScheme((isRasmol ? "Rasmol="
        : "Jmol="), true, true);
    for (int i = PAL.argbsCpk.length; --i >= 0;)
      g3d.changeColixArgb(i, argbsCpk[i]);
    for (int i = JC.altArgbsCpk.length; --i >= 0;)
      g3d.changeColixArgb(Elements.elementNumberMax + i,
          altArgbsCpk[i]);
  }

  public short colixRubberband = C.HOTPINK;

  public void setRubberbandArgb(int argb) {
    colixRubberband = (argb == 0 ? 0 : C.getColix(argb));
  }

  /*
   * black or white, whichever contrasts more with the current background
   *
   *
   * @return black or white colix value
   */
  public short colixBackgroundContrast;

  void setColixBackgroundContrast(int argb) {
    colixBackgroundContrast = C.getBgContrast(argb);
  }

  public short getColixBondPalette(Bond bond, int pid) {
    int argb = 0;
    switch (pid) {
    case PAL.PALETTE_ENERGY:
      return ce.getColorIndexFromPalette(bond.getEnergy(),
          -2.5d, -0.5d, ColorEncoder.BWR, false);
    }
    return (argb == 0 ? C.RED : C.getColix(argb));
  }

  public short getColixAtomPalette(Atom atom, byte pid) {
    int argb = 0;
    int index;
    int id;
    ModelSet modelSet = vwr.ms;
    int modelIndex;
    float lo, hi;
    // we need to use the byte form here for speed
    switch (pid) {
    case PAL.PALETTE_PROPERTY:
      return (colorData == null || atom.i >= colorData.length || Double.isNaN(colorData[atom.i]) ? C.GRAY : 
        ce.getColorIndex(colorData[atom.i]));
    case PAL.PALETTE_NONE:
    case PAL.PALETTE_CPK:
      int[] a = argbsCpk;
      int i = id = atom.getAtomicAndIsotopeNumber();
      if (i >= Elements.elementNumberMax) {
        id = Elements.altElementIndexFromNumber(i);
        if (id > 0) {
          i = id;
          id += Elements.elementNumberMax;
          a = altArgbsCpk;
        } else {
          i = id = Elements.getElementNumber(i);
        }
      }
      // Note that CPK colors can be changed based upon user preference
      // therefore, a changeable colix is allocated in this case
      return g3d.getChangeableColix(id, a[i]);
    case PAL.PALETTE_PARTIAL_CHARGE:
      // This code assumes that the range of partial charges is [-1, 1].
      index = ColorEncoder.quantize4(atom.getPartialCharge(), -1, 1,
          JC.PARTIAL_CHARGE_RANGE_SIZE);
      return g3d.getChangeableColix(JC.PARTIAL_CHARGE_COLIX_RED + index,
          JC.argbsRwbScale[index]);
    case PAL.PALETTE_FORMAL_CHARGE:
      index = atom.getFormalCharge() - Elements.FORMAL_CHARGE_MIN;
      return g3d.getChangeableColix(JC.FORMAL_CHARGE_COLIX_RED + index,
          JC.argbsFormalCharge[index]);
    case PAL.PALETTE_TEMP:
    case PAL.PALETTE_FIXEDTEMP:
      if (pid == PAL.PALETTE_TEMP) {
        lo = vwr.ms.getBfactor100Lo();
        hi = vwr.ms.getBfactor100Hi();
      } else {
        lo = 0;
        hi = 100 * 100; // scaled by 100
      }
      return ce.getColorIndexFromPalette(atom.getBfactor100(), lo, hi,
          ColorEncoder.BWR, false);
    case PAL.PALETTE_STRAIGHTNESS:
      return ce.getColorIndexFromPalette(atom.group.getGroupParameter(T.straightness), -1, 1,
          ColorEncoder.BWR, false);
    case PAL.PALETTE_SURFACE:
      hi = vwr.ms.getSurfaceDistanceMax();
      return ce.getColorIndexFromPalette(atom.getSurfaceDistance100(), 0, hi,
          ColorEncoder.BWR, false);
    case PAL.PALETTE_NUCLEIC:
      id = atom.group.groupID;
      if (id >= JC.GROUPID_NUCLEIC_MAX)
        id = JC.GROUPID_AMINO_MAX
            + "PGCATU".indexOf(Character.toUpperCase(atom.group.group1)) - 1;
      return ce.getColorIndexFromPalette(id, 0, 0, ColorEncoder.NUCLEIC, false);
    case PAL.PALETTE_AMINO:
      return ce.getColorIndexFromPalette(atom.group.groupID, 0, 0,
          ColorEncoder.AMINO, false);
    case PAL.PALETTE_SHAPELY:
      return ce.getColorIndexFromPalette(atom.group.groupID, 0, 0,
          ColorEncoder.SHAPELY, false);
    case PAL.PALETTE_GROUP:
      // vwr.calcSelectedGroupsCount() must be called first ...
      // before we call getSelectedGroupCountWithinChain()
      // or getSelectedGropuIndexWithinChain
      // however, do not call it here because it will get recalculated
      // for each atom
      // therefore, we call it in Eval.colorObject();
      return ce.getColorIndexFromPalette(atom.group.selectedIndex, 0,
          atom.group.chain.selectedGroupCount - 1, ColorEncoder.BGYOR, false);
    case PAL.PALETTE_POLYMER:
      Model m = vwr.ms.am[atom.mi];
      return ce.getColorIndexFromPalette(
          atom.group.getBioPolymerIndexInModel(), 0,
          (m.isBioModel ? ((BioModel) m).getBioPolymerCount() : 0) - 1,
          ColorEncoder.BGYOR, false);
    case PAL.PALETTE_MONOMER:
      // vwr.calcSelectedMonomersCount() must be called first ...
      return ce.getColorIndexFromPalette(atom.group.getSelectedMonomerIndex(),
          0, atom.group.getSelectedMonomerCount() - 1, ColorEncoder.BGYOR,
          false);
    case PAL.PALETTE_MOLECULE:
      return ce.getColorIndexFromPalette(
          modelSet.getMoleculeIndex(atom.i, true), 0,
          modelSet.getMoleculeCountInModel(atom.mi) - 1, ColorEncoder.ROYGB,
          false);
    case PAL.PALETTE_ALTLOC:
      //very inefficient!
      modelIndex = atom.mi;
      return ce
          .getColorIndexFromPalette(
              modelSet.getAltLocIndexInModel(modelIndex, atom.altloc), 0,
              modelSet.getAltLocCountInModel(modelIndex), ColorEncoder.ROYGB,
              false);
    case PAL.PALETTE_INSERTION:
      //very inefficient!
      modelIndex = atom.mi;
      return ce.getColorIndexFromPalette(
          modelSet.getInsertionCodeIndexInModel(modelIndex,
              atom.group.getInsertionCode()), 0,
          modelSet.getInsertionCountInModel(modelIndex), ColorEncoder.ROYGB,
          false);
    case PAL.PALETTE_JMOL:
      id = atom.getAtomicAndIsotopeNumber();
      argb = getJmolOrRasmolArgb(id, T.jmol);
      break;
    case PAL.PALETTE_RASMOL:
      id = atom.getAtomicAndIsotopeNumber();
      argb = getJmolOrRasmolArgb(id, T.rasmol);
      break;
    case PAL.PALETTE_STRUCTURE:
      argb = atom.group.getProteinStructureSubType().getColor();
      break;
    case PAL.PALETTE_CHAIN:
      int chain = atom.getChainID();
      if (ColorEncoder.argbsChainAtom == null) {
        ColorEncoder.argbsChainAtom = getArgbs(T.atoms);
        ColorEncoder.argbsChainHetero = getArgbs(T.hetero);
      }
      chain = ((chain < 0 ? 0 : chain >= 256 ? chain - 256 : chain) & 0x1F)
          % ColorEncoder.argbsChainAtom.length;
      argb = (atom.isHetero() ? ColorEncoder.argbsChainHetero
          : ColorEncoder.argbsChainAtom)[chain];
      break;
    }
    return (argb == 0 ? C.HOTPINK : C.getColix(argb));
  }

  private int[] getArgbs(int tok) {
    return vwr.getJBR().getArgbs(tok);
  }

  private int getJmolOrRasmolArgb(int id, int argb) {
    switch (argb) {
    case T.jmol:
      if (id >= Elements.elementNumberMax)
        break;
      return ce.getArgbFromPalette(id, 0, 0,
          ColorEncoder.JMOL);
    case T.rasmol:
      if (id >= Elements.elementNumberMax)
        break;
      return ce.getArgbFromPalette(id, 0, 0,
          ColorEncoder.RASMOL);
    default:
      return argb;
    }
    return JC.altArgbsCpk[Elements.altElementIndexFromNumber(id)];
  }

  public short getElementColix(int elemNo) {
    // could be isotope,elemno
    int[] a = argbsCpk;
    int i = elemNo;
    if (i > Elements.elementNumberMax) {
      int ialt = Elements.altElementIndexFromNumber(i);
      if (ialt >  0) {
        i = ialt;
        a = altArgbsCpk;
      } else {
        i = Elements.getElementNumber(i);
      }
    }
    return C.getColix(a[i]);
  }
  
  public void setElementArgb(int elemNo, int argb) {
    if (argb == T.jmol && argbsCpk == PAL.argbsCpk)
      return;
    argb = getJmolOrRasmolArgb(elemNo, argb);
    if (argbsCpk == PAL.argbsCpk) {
      argbsCpk = AU.arrayCopyRangeI(PAL.argbsCpk, 0, -1);
      altArgbsCpk = AU.arrayCopyRangeI(JC.altArgbsCpk, 0, -1);
    }
    int id = elemNo;
    if (id < Elements.elementNumberMax) {
      argbsCpk[id] = argb;
    } else {
      id = Elements.altElementIndexFromNumber(elemNo);
      altArgbsCpk[id] = argb;
      id += Elements.elementNumberMax;
    }
    g3d.changeColixArgb(id, argb);
    vwr.setModelkitPropertySafely(JC.MODELKIT_UPDATE_MODEL_KEYS, null);
  }


  ///////////////////  propertyColorScheme ///////////////

  double[] getPropertyColorRange() {
    return (ce.isReversed ? new double[] { ce.hi, ce.lo } : new double[] { ce.lo,
        ce.hi });
  }

  void getPropertyColorRangeF(double[] ret) {
    ret[ce.isReversed ? 0 : 1] = ce.hi;
    ret[ce.isReversed ? 1 : 0] = ce.lo;
  }

  public void setPropertyColorRangeData(double[] data, BS bs) {
    colorData = data;
    ce.currentPalette = ce.createColorScheme(
        vwr.g.propertyColorScheme, true, false);
    ce.hi = -Double.MAX_VALUE;
    ce.lo = Double.MAX_VALUE;
    if (data == null)
      return;
    boolean isAll = (bs == null);
    double min = Double.MAX_VALUE;
    double max = -Double.MAX_VALUE;
    double d;
    int i0 = (isAll ? data.length - 1 : bs.nextSetBit(0));
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bs.nextSetBit(i + 1))) {
      if (Double.isNaN(d = data[i]))
        continue;
      if (d > max)
        max = d;
      if (d < min)
        min = d;
    }    
    setPropertyColorRange(ce.lo = min, ce.hi = max);
  }

  public void setPropertyColorRange(double min, double max) {
    ce.setRange(min, max, min > max);
    if (Logger.debugging)
      Logger.debug("ColorManager: color \""
        + ce.getCurrentColorSchemeName() + "\" range " + min + " "
        + max);
  }

  double[] frange = new double[2];
  
  void setPropertyColorScheme(String colorScheme, boolean isTranslucent,
                              boolean isOverloaded) {
    boolean isReset = (colorScheme.length() == 0);
    if (isReset)
      colorScheme = "="; // reset roygb
    getPropertyColorRangeF(frange);
    ce.currentPalette = ce.createColorScheme(
        colorScheme, true, isOverloaded);
    if (!isReset)
      setPropertyColorRange(frange[0], frange[1]);
    ce.isTranslucent = isTranslucent;
  }

  public String getColorSchemeList(String colorScheme) {
    // isosurface sets ifDefault FALSE so that any default schemes are returned
    int iPt = (colorScheme == null || colorScheme.length() == 0) ? ce.currentPalette
        : ce
            .createColorScheme(colorScheme, true, false);
    return ColorEncoder.getColorSchemeList(ce
        .getColorSchemeArray(iPt));
  }

  public ColorEncoder getColorEncoder(String colorScheme) {
    if (colorScheme == null || colorScheme.length() == 0)
      return ce;
    ColorEncoder c = new ColorEncoder(ce, vwr);
    c.currentPalette = c.createColorScheme(colorScheme, false, true);
    return (c.currentPalette == Integer.MAX_VALUE ? null : c);
  }
}
