/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-09 19:01:40 -0500 (Mon, 09 Apr 2007) $
 * $Revision: 7365 $

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.jmol.shapebio;

import java.util.Map;

import javajs.util.AU;
import javajs.util.PT;
import javajs.util.V3;

import org.jmol.c.PAL;
import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.ModelSet;
import org.jmol.modelsetbio.AlphaPolymer;
import org.jmol.modelsetbio.BioPolymer;
import org.jmol.modelsetbio.Monomer;
import org.jmol.modelsetbio.NucleicMonomer;
import org.jmol.modelsetbio.NucleicPolymer;
import org.jmol.shape.AtomShape;
import org.jmol.shape.Mesh;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Logger;
import org.jmol.viewer.JC;

public class BioShape extends AtomShape {

  @Override
  public void setProperty(String propertyName, Object value, BS bsSelected) {
    setPropAS(propertyName, value, bsSelected);
    
  }

  public int modelIndex;
  public int modelVisibilityFlags = 0;

  BioShapeCollection shape;
  
  public BioPolymer bioPolymer;
  
  public Mesh[] meshes;
  public boolean[] meshReady;

  public short[] colixesBack;

  public Monomer[] monomers;

  public V3[] wingVectors;
  int[] leadAtomIndices;

  BioShape(BioShapeCollection shape, int modelIndex, BioPolymer bioPolymer) {
    this.shape = shape;
    this.modelIndex = modelIndex;
    this.bioPolymer = bioPolymer;
    isActive = shape.isActive;
    bsSizeDefault = new BS();
    monomerCount = bioPolymer.monomerCount;
    if (monomerCount > 0) {
      colixes = new short[monomerCount];
      paletteIDs = new byte[monomerCount];
      mads = new short[monomerCount + 1];
      monomers = bioPolymer.monomers;
      meshReady = new boolean[monomerCount];
      meshes = new Mesh[monomerCount];
      wingVectors = bioPolymer.getWingVectors();
      leadAtomIndices = bioPolymer.getLeadAtomIndices();
      //Logger.debug("mps assigning wingVectors and leadMidpoints");
    }
  }

  boolean hasBfactorRange = false;
  int bfactorMin, bfactorMax;
  int range;
  float floatRange;

  void calcBfactorRange() {
    bfactorMin = bfactorMax =
      monomers[0].getLeadAtom().getBfactor100();
    for (int i = monomerCount; --i > 0; ) {
      int bfactor =
        monomers[i].getLeadAtom().getBfactor100();
      if (bfactor < bfactorMin)
        bfactorMin = bfactor;
      else if (bfactor > bfactorMax)
        bfactorMax = bfactor;
    }
    range = bfactorMax - bfactorMin;
    floatRange = range;
    hasBfactorRange = true;
  }

  private final static double eightPiSquared100 = 8 * Math.PI * Math.PI * 100;
  /**
   * Calculates the mean positional displacement in milliAngstroms.
   * <p>
   * <a href='http://www.rcsb.org/pdb/lists/pdb-l/200303/000609.html'>
   * http://www.rcsb.org/pdb/lists/pdb-l/200303/000609.html
   * </a>
   * <code>
   * > -----Original Message-----
   * > From: pdb-l-admin@sdsc.edu [mailto:pdb-l-admin@sdsc.edu] On 
   * > Behalf Of Philipp Heuser
   * > Sent: Thursday, March 27, 2003 6:05 AM
   * > To: pdb-l@sdsc.edu
   * > Subject: pdb-l: temperature factor; occupancy
   * > 
   * > 
   * > Hi all!
   * > 
   * > Does anyone know where to find proper definitions for the 
   * > temperature factors 
   * > and the values for occupancy?
   * > 
   * > Alright I do know, that the atoms with high temperature 
   * > factors are more 
   * > disordered than others, but what does a temperature factor of 
   * > a specific 
   * > value mean exactly.
   * > 
   * > 
   * > Thanks in advance!
   * > 
   * > Philipp
   * > 
   * pdb-l: temperature factor; occupancy
   * Bernhard Rupp br@llnl.gov
   * Thu, 27 Mar 2003 08:01:29 -0800
   * 
   * * Previous message: pdb-l: temperature factor; occupancy
   * * Next message: pdb-l: Structural alignment?
   * * Messages sorted by: [ date ] [ thread ] [ subject ] [ author ]
   * 
   * Isotropic B is defined as 8*pi**2<u**2>.
   * 
   * Meaning: eight pi squared =79
   * 
   * so B=79*mean square displacement (from rest position) of the atom.
   * 
   * as u is in Angstrom, B must be in Angstrom squared.
   * 
   * example: B=79A**2
   * 
   * thus, u=sqrt([79/79]) = 1 A mean positional displacement for atom.
   * 
   * 
   * See also 
   * 
   * http://www-structure.llnl.gov/Xray/comp/comp_scat_fac.htm#Atomic
   * 
   * for more examples.
   * 
   * BR
   *</code>
   *
   * @param bFactor100
   * @return ?
   */
  short calcMeanPositionalDisplacement(int bFactor100) {
    return (short)(Math.sqrt(bFactor100/eightPiSquared100) * 1000);
  }

  @Override
  public void findNearestAtomIndex(int xMouse, int yMouse, Atom[] closest, BS bsNot) {
    bioPolymer.findNearestAtomIndex(xMouse, yMouse, closest, mads,
        shape.vf, bsNot);
  }
  
  void setMad(short mad, BS bsSelected, float[] values) {
    if (monomerCount < 2)
      return;
    isActive = true;
    if (bsSizeSet == null)
      bsSizeSet = new BS();
    int flag = shape.vf;
    boolean setRingVis = (flag == JC.VIS_CARTOON_FLAG && bioPolymer instanceof NucleicPolymer);
    for (int i = monomerCount; --i >= 0; ) {
      int leadAtomIndex = leadAtomIndices[i];
      if (bsSelected.get(leadAtomIndex)) {
        if (values != null && leadAtomIndex < values.length) {
          if (Float.isNaN(values[leadAtomIndex]))
            continue;
          mad = (short) (values[leadAtomIndex] * 2000);
        }
        boolean isVisible = ((mads[i] = getMad(i, mad)) > 0);
        bsSizeSet.setBitTo(i, isVisible);
        monomers[i].setShapeVisibility(flag, isVisible);
        // this is necessary in case we are 
        shape.atoms[leadAtomIndex].setShapeVisibility(flag, isVisible);
        if (setRingVis)
          ((NucleicMonomer) monomers[i]).setRingsVisible(isVisible);
        falsifyNearbyMesh(i);
      }
    }
    if (monomerCount > 1)
      mads[monomerCount] = mads[monomerCount - 1];
  }

  private short getMad(int groupIndex, short mad) {
    bsSizeDefault.setBitTo(groupIndex, mad == -1 || mad == -2);
    if (mad >= 0)
      return mad;      
    switch (mad) {
    case -1: // trace on
    case -2: // trace structure
      if (mad == -1 && shape.madOn >= 0)
        return shape.madOn;
      switch (monomers[groupIndex].getProteinStructureType()) {
      case SHEET:
      case HELIX:
        return shape.madHelixSheet;
      case DNA:
      case RNA:
        return shape.madDnaRna;
      default:
        return shape.madTurnRandom;
      }
    case -3: // trace temperature
      {
        if (! hasBfactorRange)
          calcBfactorRange();
        Atom atom = monomers[groupIndex].getLeadAtom();
        int bfactor100 = atom.getBfactor100(); // scaled by 1000
        int scaled = bfactor100 - bfactorMin;
        if (range == 0)
          return (short)0;
        float percentile = scaled / floatRange;
        if (percentile < 0 || percentile > 1)
          Logger.error("Que ha ocurrido? " + percentile);
        return (short)((1750 * percentile) + 250);
      }
    case -4: // trace displacement
      {
        Atom atom = monomers[groupIndex].getLeadAtom();
        return // double it ... we are returning a diameter
          (short)(2 * calcMeanPositionalDisplacement(atom.getBfactor100()));
      }
    }
    Logger.error("unrecognized setMad(" + mad + ")");
    return 0;
  }

  public void falsifyMesh() {
    if (meshReady == null)
      return;
    for (int i = 0; i < monomerCount; i++)
      meshReady[i] = false;
  }
   
  private void falsifyNearbyMesh(int index) {
    if (meshReady == null)
      return;
    meshReady[index] = false;
    if (index > 0)
      meshReady[index - 1] = false;
    if (index < monomerCount - 1)
      meshReady[index + 1] = false;
  }    

  void setColixBS(short colix, byte pid, BS bsSelected) {
    isActive = true;
    if (bsColixSet == null)
      bsColixSet = BS.newN(monomerCount);
    for (int i = monomerCount; --i >= 0;) {
      int atomIndex = leadAtomIndices[i];
      if (bsSelected.get(atomIndex)) {
        colixes[i] = shape.getColixI(colix, pid, atomIndex);
        if (colixesBack != null && colixesBack.length > i)
          colixesBack[i] = C.INHERIT_ALL;
        paletteIDs[i] = pid;
        bsColixSet.setBitTo(i, colixes[i] != C.INHERIT_ALL);
      }
    }
  }
  
  void setColixBack(short colix, BS bsSelected) {
    if (colixesBack == null)
      colixesBack = new short[colixes.length];
    if (colixesBack.length < colixes.length)
      colixesBack = AU.ensureLengthShort(colixesBack, colixes.length);
    for (int i = monomerCount; --i >= 0;)
      if (bsSelected.get(leadAtomIndices[i]))
        colixesBack[i] = colix;
  }
  
  void setColixes(short[] atomColixes, BS bsSelected) {
    isActive = true;
    if (bsColixSet == null)
      bsColixSet = BS.newN(monomerCount);
    for (int i = monomerCount; --i >= 0;) {
      int atomIndex = leadAtomIndices[i];
      if (bsSelected.get(atomIndex) && i < colixes.length && atomIndex < atomColixes.length) {
        colixes[i] = shape.getColixI(atomColixes[atomIndex], PAL.UNKNOWN.id, atomIndex);
        if (colixesBack != null && i < colixesBack.length)
          colixesBack[i] = C.INHERIT_ALL;
        paletteIDs[i] = PAL.UNKNOWN.id;
        bsColixSet.set(i);
      }
    }
  }
  
  public void setParams(Object[] data, int[] atomMap, BS bsSelected) {
    if (monomerCount == 0)
      return;
    // only implemented for simple colixes, really.
    short[] c = (short[]) data[0];
    // would have to do something like this here as well;
    float[] atrans = (float[]) data[1];
    //float[] sizes = (float[]) data[2];

    isActive = true;
    if (bsColixSet == null)
      bsColixSet = BS.newN(monomerCount);
    int n = atomMap.length;
    for (int i = monomerCount; --i >= 0;) {
      int atomIndex = leadAtomIndices[i];
      if (bsSelected.get(atomIndex) && i < colixes.length && atomIndex < n) {
        int pt = atomMap[atomIndex];
        short colix = (c == null ? C.INHERIT_ALL : c[pt]);
        float f = (atrans == null ? 0 : atrans[pt]);
        if (f > 0.01f)
          colix = C.getColixTranslucent3(colix, true, f);
        colixes[i] = shape.getColixI(colix, PAL.UNKNOWN.id, atomIndex);
        if (colixesBack != null && i < colixesBack.length)
          colixesBack[i] = 0;
        paletteIDs[i] = PAL.UNKNOWN.id;
        bsColixSet.set(i);
      }
    }    
  }

  void setTranslucent(boolean isTranslucent, BS bsSelected, float translucentLevel) {
    isActive = true;
    if (bsColixSet == null)
      bsColixSet = BS.newN(monomerCount);
    for (int i = monomerCount; --i >= 0; )
      if (bsSelected.get(leadAtomIndices[i])) {
        colixes[i] = C.getColixTranslucent3(colixes[i], isTranslucent, translucentLevel);
        if (colixesBack != null && colixesBack.length > i)
          colixesBack[i] = C.getColixTranslucent3(colixesBack[i], isTranslucent, translucentLevel);
        bsColixSet.setBitTo(i, colixes[i] != C.INHERIT_ALL);
    }
  }

  @Override
  public void setAtomClickability() {
    if (!isActive || wingVectors == null || monomerCount == 0)
      return;
    boolean setRingsClickable = (bioPolymer instanceof NucleicPolymer && shape.shapeID == JC.SHAPE_CARTOON);
    boolean setAlphaClickable = (bioPolymer instanceof AlphaPolymer || shape.shapeID != JC.SHAPE_ROCKETS);
    ModelSet ms = monomers[0].chain.model.ms;
    for (int i = monomerCount; --i >= 0;) {
      if (mads[i] <= 0)
        continue;
      int iAtom = leadAtomIndices[i];
      if (ms.isAtomHidden(iAtom))
        continue;
      if (setAlphaClickable)
        ms.at[iAtom].setClickable(JC.ALPHA_CARBON_VISIBILITY_FLAG);
      if (setRingsClickable)
        ((NucleicMonomer) monomers[i]).setRingsClickable();
    }
  }

  void getBioShapeState(String type, boolean translucentAllowed,
                               Map<String, BS> temp, Map<String, BS> temp2) {
    if (monomerCount > 0) {
      if (!isActive || bsSizeSet == null && bsColixSet == null)
        return;
      for (int i = 0; i < monomerCount; i++) {
        int atomIndex1 = monomers[i].firstAtomIndex;
        int atomIndex2 = monomers[i].lastAtomIndex;
        if (bsSizeSet != null
            && (bsSizeSet.get(i) || bsColixSet != null && bsColixSet.get(i))) {//shapes MUST have been set with a size
          if (bsSizeDefault.get(i)) {
            BSUtil.setMapBitSet(temp, atomIndex1, atomIndex2,
                type + (bsSizeSet.get(i) ? " on" : " off"));
          } else {
            BSUtil.setMapBitSet(temp, atomIndex1, atomIndex2,
                type + " " + PT.escF(mads[i] / 2000f));
          }
        }
        if (bsColixSet == null || !bsColixSet.get(i))
          continue;
        String s = getColorCommand(type, paletteIDs[i], colixes[i],
            translucentAllowed);
        if (colixesBack != null && colixesBack.length > i
            && colixesBack[i] != C.INHERIT_ALL)
          s += " " + C.getHexCode(colixesBack[i]);
        BSUtil.setMapBitSet(temp2, atomIndex1, atomIndex2, s);
      }
    }
  }

  @Override
   public String getShapeState() {
    // implemented in BioShapeCollection, not here
    return null;
  }

}