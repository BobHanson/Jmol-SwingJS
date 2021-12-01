/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2008-09-16 22:39:58 -0500 (Tue, 16 Sep 2008) $
 * $Revision: 9905 $

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
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.modelset;

import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.V3;

import org.jmol.api.SymmetryInterface;
import javajs.util.BS;
import org.jmol.viewer.Viewer;

import org.jmol.modelsetbio.BioModel;


public class Trajectory {
  private Viewer vwr;
  ModelSet ms;
  Lst<P3[]> steps;

  public Trajectory() {
    
  }
  
  Trajectory set(Viewer vwr, ModelSet ms, Lst<P3[]> steps) {
    this.vwr = vwr;
    this.ms = ms;
    this.steps = steps;
    return this;    
  }

  void setUnitCell(int imodel) {
    SymmetryInterface c = ms.getUnitCell(imodel);
    if (c != null && c.getCoordinatesAreFractional() && c.isSupercell()) {
      P3[] list = ms.trajectory.steps.get(imodel);
      for (int i = list.length; --i >= 0;)
        if (list[i] != null)
          c.toSupercell(list[i]);
    }
  }

  /**
   * The user has used the MODEL command to switch to a new set of atom
   * coordinates Or has specified a trajectory in a select, display, or hide
   * command. Assign the coordinates and the model index for this set of atoms
   * 
   * @param modelIndex
   */
  void setModel(int modelIndex) {
    Model[] am = ms.am;
    int baseModelIndex = am[modelIndex].trajectoryBaseIndex;
    am[baseModelIndex].selectedTrajectory = modelIndex;
    // dcd trajectories should cancel PDB crystal data. Not sure why they were there. Somewhat of a hack
    isFractional = !ms.getMSInfoB("ignoreUnitCell");
    setAtomPositions(baseModelIndex, modelIndex, steps.get(modelIndex),
        null, 0,
        (ms.vibrationSteps == null ? null : ms.vibrationSteps.get(modelIndex)), isFractional);    
    int currentModelIndex = vwr.am.cmi;
    if (currentModelIndex >= 0 && currentModelIndex != modelIndex 
        && am[currentModelIndex].fileIndex == am[modelIndex].fileIndex)
      vwr.setCurrentModelIndexClear(modelIndex, false);
  }
  
  boolean isFractional = true;
  
  /**
   * A generic way to set atom positions, possibly from trajectories but also
   * possibly from an array. Takes care of all associated issues of changing
   * coordinates.
   * 
   * @param baseModelIndex
   * @param modelIndex
   * @param t1
   * @param t2
   * @param f
   * @param vibs
   * @param isFractional
   */
  private void setAtomPositions(int baseModelIndex, int modelIndex,
                                P3[] t1, P3[] t2,
                                float f, V3[] vibs,
                                boolean isFractional) {
    BS bs = new BS();
    V3 vib = new V3();
    Model[] am = ms.am;
    Atom[] at = ms.at;
    int iFirst = am[baseModelIndex].firstAtomIndex;
    int iMax = iFirst + ms.getAtomCountInModel(baseModelIndex);
    if (f == 0) {
      for (int pt = 0, i = iFirst; i < iMax && pt < t1.length; i++, pt++) {
        Atom a = at[i];
        if (a == null)
          continue;
        a.mi = (short) modelIndex;
        if (t1[pt] == null)
          continue;
        if (isFractional)
          a.setFractionalCoordTo(t1[pt], true);
        else
          a.setT(t1[pt]);
        if (ms.vibrationSteps != null) {
          if (vibs != null && vibs[pt] != null)
            vib = vibs[pt];
          ms.setVibrationVector(i, vib);
        }
        bs.set(i);
      }
    } else {
      P3 p = new P3();
      int n = Math.min(t1.length, t2.length);
      for (int pt = 0, i = iFirst; i < iMax && pt < n; i++, pt++) {
        Atom a = at[i];
        if (a == null)
          continue;
        a.mi = (short) modelIndex;
        if (t1[pt] == null || t2[pt] == null)
          continue;
        p.sub2(t2[pt], t1[pt]);
        p.scaleAdd2(f, p, t1[pt]);
        if (isFractional)
          a.setFractionalCoordTo(p, true);
        else
          a.setT(p);
        bs.set(i);
      } 
    }
    // Clear the Binary Search so that select within(),
    // isosurface, and dots will work properly
    ms.initializeBspf();
    ms.validateBspfForModel(baseModelIndex, false);
    // Recalculate critical points for cartoons and such
    // note that models[baseModel] and models[modelIndex]
    // point to the same model. So there is only one copy of 
    // the shape business.

    ms.recalculateLeadMidpointsAndWingVectors(baseModelIndex);
    // Recalculate all measures that involve trajectories

    ms.sm.notifyAtomPositionsChanged(baseModelIndex, bs, null);

    if (am[baseModelIndex].hasRasmolHBonds)
      ((BioModel) am[baseModelIndex]).resetRasmolBonds(bs, 2); // setting DSSP version to 2 here
  }

  BS getModelsSelected() {
    BS bsModels = new BS();
    for (int i = ms.mc; --i >= 0;) {
      int t = ms.am[i].selectedTrajectory; 
      if (t >= 0) {
        bsModels.set(t);
        i = ms.am[i].trajectoryBaseIndex; //skip other trajectories
      }
    }
    return bsModels;
  }

  void morph(int m1, int m2, float f) {
    if (f == 0) {
      ms.setTrajectory(m1);
      return;
    }
    if (f == 1) {
      ms.setTrajectory(m2);
      return;
    }
    int baseModelIndex = ms.am[m1].trajectoryBaseIndex;
    ms.am[baseModelIndex].selectedTrajectory = m1;
    setAtomPositions(baseModelIndex, m1, steps.get(m1),
        steps.get(m2), f, (ms.vibrationSteps == null ? null
            : ms.vibrationSteps.get(m1)), true);
    int m = vwr.am.cmi;
    if (m >= 0 && m != m1 && ms.am[m].fileIndex == ms.am[m1].fileIndex)
      vwr.setCurrentModelIndexClear(m1, false);
  }

  void fixAtom(Atom a) {
    int m = a.mi;
    boolean isFrac = (ms.unitCells != null && ms.unitCells[m]
        .getCoordinatesAreFractional());
    P3 pt = steps.get(m)[a.i - ms.am[m].firstAtomIndex];
    pt.set(a.x, a.y, a.z);
    if (isFrac)
      ms.unitCells[m].toFractional(pt, true);
  }

  public void getFractional(Atom a, P3 ptTemp) {
    a.setFractionalCoordPt(ptTemp, steps.get(a.mi)[a.i
        - ms.am[a.mi].firstAtomIndex], true);
  }

  public String getState() {
    String s = "";
    for (int i = ms.mc; --i >= 0;) {
      int t = ms.am[i].selectedTrajectory;
      if (t >= 0) {
        s = " or " + ms.getModelNumberDotted(t) + s;
        i = ms.am[i].trajectoryBaseIndex; //skip other trajectories
      }
    }
    return (s.length() > 0 ? s = "set trajectory {" + s.substring(4) + "}" : "");
  }

  public boolean hasMeasure(int[] measure) {
    if (measure != null) {
      int atomIndex;
      for (int i = 1, count = measure[0]; i <= count; i++)
        if ((atomIndex = measure[i]) >= 0
            && ms.am[ms.at[atomIndex].mi].isTrajectory)
          return true;
    }
    return false;
  }

  /**
   * Remove trajectories that are not currently displayed from the visible
   * frames bitset.
   * 
   * when a trajectory is selected, the atom's modelIndex is switched to that of
   * the selected trajectory even though the underlying model itself is not
   * changed.
   * 
   * @param bs
   */
  public void selectDisplayed(BS bs) {
    Atom a;
    for (int i = ms.mc; --i >= 0;) {
      if (ms.am[i].isTrajectory && ((a = ms.at[ms.am[i].firstAtomIndex]) == null || a.mi != i))
        bs.clear(i);
    }
  }
  
  /**
   * set bits for all trajectories associated with this model
   * 
   * @param modelIndex
   * @param bs
   */

  public void getModelBS(int modelIndex, BS bs) {    
    int iBase = ms.am[modelIndex].trajectoryBaseIndex;
    for (int i = ms.mc; --i >= iBase;)
      if (ms.am[i].trajectoryBaseIndex == iBase)
        bs.set(i);
  }

  /**
   * set bits for all base models only
   * 
   * @param bsModels
   */
  public void setBaseModels(BS bsModels) {
    for (int i = ms.mc; --i >= 0; )
      if (bsModels.get(i) && ms.am[i].isTrajectory)
        bsModels.set(ms.am[i].trajectoryBaseIndex);    
  }


}



