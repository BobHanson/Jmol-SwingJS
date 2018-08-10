/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-08-22 03:13:40 -0500 (Tue, 22 Aug 2006) $
 * $Revision: 5412 $

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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.shape;

import javajs.util.AU;

import org.jmol.atomdata.RadiusData;
import org.jmol.atomdata.RadiusData.EnumType;
import org.jmol.c.PAL;
import org.jmol.c.VDW;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.viewer.JC;

public abstract class AtomShape extends Shape {

  // Balls, BioShape Dots, Ellipsoids, Halos, Labels, Polyhedra, Stars, Vectors

  public short mad = (short)-1;
  public short[] mads;
  public short[] colixes;
  public byte[] paletteIDs;
  public int ac;
  public Atom[] atoms;
  public boolean isActive;
  
  public int monomerCount;
  public BS bsSizeDefault;
  
  @Override
  public void initShape() {
    // nothing  to do  
  }
  
  @Override
  protected void initModelSet() {
    atoms = ms.at;
    ac = ms.ac;
    // in case this is due to "load append"
    if (mads != null)
      mads = AU.arrayCopyShort(mads, ac);
    if (colixes != null)
      colixes = AU.arrayCopyShort(colixes, ac);
    if (paletteIDs != null)
      paletteIDs = AU.arrayCopyByte(paletteIDs, ac);
  }

  @Override
  public int getSize(int atomIndex) {
    return (mads == null ? 0 : mads[atomIndex]);
  }
  
  @Override
  protected void setSize(int   size, BS bsSelected) {
    setSize2(size, bsSelected);
  }

  private RadiusData rd;

  protected void setSize2(int size, BS bsSelected) {
    if (size == 0) {
      setSizeRD(null, bsSelected);
      return;
    }
    if (rd == null)
      rd = new RadiusData(null, size, EnumType.SCREEN, null);
    else
      rd.value = size;
    setSizeRD(rd, bsSelected);
  }

  @Override
  protected void setSizeRD(RadiusData rd, BS bsSelected) {
    // Halos Stars Vectors
    if (atoms == null)  // vector values are ignored if there are none for a model 
      return;
    isActive = true;
    boolean isVisible = (rd != null && rd.value != 0);
    boolean isAll = (bsSelected == null);
    int i0 = (isAll ? ac - 1 : bsSelected.nextSetBit(0));
    if (bsSizeSet == null)
      bsSizeSet = BS.newN(ac);
    if (mads == null && i0 >= 0)
      mads = new short[ac];
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bsSelected.nextSetBit(i + 1)))
      setSizeRD2(i, rd, isVisible);
  }

  protected void setSizeRD2(int i, RadiusData rd, boolean isVisible) {
    Atom atom = atoms[i];
    mads[i] = atom.calculateMad(vwr, rd);
    bsSizeSet.setBitTo(i, isVisible);
    atom.setShapeVisibility(vf, isVisible);
  }

  protected void setPropAS(String propertyName, Object value, BS bs) {
    if ("color" == propertyName) {
      isActive = true;
      short colix = C.getColixO(value);
      byte pid = PAL.pidOf(value);
      int n = checkColixLength(colix, bs.length());
      for (int i = bs.nextSetBit(0); i >= 0 && i < n; i = bs.nextSetBit(i + 1))
        setColixAndPalette(colix, pid, i);
      return;
    }
    if ("params" == propertyName) {
      // PyMOL only
      isActive = true;
      Object[] data = (Object[]) value;
      short[] colixes = (short[]) data[0];
      float[] atrans = (float[]) data[1];
      float[] sizes = (float[]) data[2];
      RadiusData rd = new RadiusData(null, 0, RadiusData.EnumType.FACTOR,
          VDW.AUTO);
      if (bsColixSet == null)
        bsColixSet = new BS();
      if (bsSizeSet == null)
        bsSizeSet = new BS();
      int i0 = bs.nextSetBit(0);
      if (mads == null && i0 >= 0)
        mads = new short[ac];
      
      int n = checkColixLength(colixes == null ? 0 : C.BLACK, bs.length());
      for (int i = i0, pt = 0; i >= 0 && i < n; i = bs.nextSetBit(i + 1), pt++) {
        short colix = (colixes == null ? 0 : colixes[pt]);
        //if (colix == 0)
          //colix = C.INHERIT_ALL;  IS 0 already
        float f = (atrans == null ? 0 : atrans[pt]);
        if (f > 0.01f)
          colix = C.getColixTranslucent3(colix, true, f);
        setColixAndPalette(colix, PAL.UNKNOWN.id, i);
        if (sizes == null)
          continue;
        boolean isVisible = ((rd.value = sizes[pt]) > 0);
        setSizeRD2(i, rd, isVisible);
      }
      return;
    }
    if ("translucency" == propertyName) {
      isActive = true;
      boolean isTranslucent = (value.equals("translucent"));
      checkColixLength(C.BLACK, ac);
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        colixes[i] = C.getColixTranslucent3(colixes[i], isTranslucent,
            translucentLevel);
        if (isTranslucent)
          bsColixSet.set(i);
      }
      return;
    }
    if (propertyName == "deleteModelAtoms") {
      atoms = (Atom[]) ((Object[]) value)[1];
      int[] info = (int[]) ((Object[]) value)[2];
      ac = ms.ac;
      int firstAtomDeleted = info[1];
      int nAtomsDeleted = info[2];
      mads = (short[]) AU.deleteElements(mads, firstAtomDeleted,
          nAtomsDeleted);
      colixes = (short[]) AU.deleteElements(colixes, firstAtomDeleted,
          nAtomsDeleted);
      paletteIDs = (byte[]) AU.deleteElements(paletteIDs,
          firstAtomDeleted, nAtomsDeleted);
      BSUtil.deleteBits(bsSizeSet, bs);
      BSUtil.deleteBits(bsColixSet, bs);
      return;
    }
    setPropS(propertyName, value, bs);
  }

  protected int checkColixLength(short colix, int n) {
    n = Math.min(ac, n);
    if (colix == C.INHERIT_ALL)
      return (colixes == null ? 0 : colixes.length);
    if (colixes == null || n > colixes.length) {
      colixes = AU.ensureLengthShort(colixes, n);
      paletteIDs = AU.ensureLengthByte(paletteIDs, n);
    }
    if (bsColixSet == null)
      bsColixSet = BS.newN(ac);
    return n;
  }
  
  protected void setColixAndPalette(short colix, byte paletteID, int atomIndex) {
    if (colixes == null)
      System.out.println("ATOMSHAPE ERROR");
    colixes[atomIndex] = colix = getColixI(colix, paletteID, atomIndex);
    bsColixSet.setBitTo(atomIndex, colix != C.INHERIT_ALL || shapeID == JC.SHAPE_BALLS);
    paletteIDs[atomIndex] = paletteID;
  }

  @Override
  public void setAtomClickability() {
    if (!isActive)
      return;
    for (int i = ac; --i >= 0;) {
      Atom atom = atoms[i];
      if ((atom.shapeVisibilityFlags & vf) == 0
          || ms.isAtomHidden(i))
        continue;
      atom.setClickable(vf);
    }
  }

  /**
   * @param i  
   * @return script, but only for Measures
   */
  public String getInfoAsString(int i) {
    // only in Measures
    return null;
  }

  @Override
  public String getShapeState() {
   // implemented in StateCreator, not here
   return null;
 }
}
