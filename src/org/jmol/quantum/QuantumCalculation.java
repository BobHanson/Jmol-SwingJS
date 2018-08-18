/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-05-13 19:17:06 -0500 (Sat, 13 May 2006) $
 * $Revision: 5114 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.quantum;


import javajs.util.P3;
import javajs.util.T3;

import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.util.Escape;
import org.jmol.util.Logger;


abstract public class QuantumCalculation {

  protected boolean doDebug = false;
  protected BS bsExcluded;

  protected float integration = Float.NaN;
  
  public float getIntegration() {
    return integration;
  }
  

  protected final static float bohr_per_angstrom = 1 / 0.52918f;

  protected float[][][] voxelData;
  public float[][][] voxelDataTemp;
  protected int[] countsXYZ;
  
  protected T3[] points;
  public int xMin, xMax, yMin, yMax, zMin, zMax;

  protected QMAtom[] qmAtoms;
  protected int atomIndex;
  protected QMAtom thisAtom;
  protected int firstAtomOffset;

  // absolute grid coordinates in Bohr
  // these values may change if the reader
  // is switching between reading surface points 
  // and getting values for them during a 
  // progressive calculation.
  
  protected float[] xBohr, yBohr, zBohr;
  protected float[] originBohr = new float[3];
  protected float[] stepBohr = new float[3];
  protected int nX, nY, nZ;
  
  // grid coordinates relative to orbital center in Bohr 
  public float[] X, Y, Z;

  // grid coordinate squares relative to orbital center in Bohr
  public float[] X2, Y2, Z2;

  // range in bohr to consider affected by an atomic orbital
  // this is a cube centered on an atom of side rangeBohr*2
  protected float rangeBohrOrAngstroms = 10; //bohr; about 5 Angstroms
  
  protected float unitFactor = bohr_per_angstrom;

  protected void initialize(int nX, int nY, int nZ, T3[] points) {
    initialize0(nX, nY ,nZ, points);
  }

  protected void initialize0(int nX, int nY, int nZ, T3[] points) {
    if (points != null) {
      this.points = points;
      nX = nY = nZ = points.length;
    }
    
    this.nX = xMax = nX;
    this.nY = yMax = nY;
    this.nZ = zMax = nZ;
    
    if (xBohr != null && xBohr.length >= nX)
      return;
    
    // absolute grid coordinates in Bohr
    xBohr = new float[nX];
    yBohr = new float[nY];
    zBohr = new float[nZ];

    // grid coordinates relative to orbital center in Bohr 
    X = new float[nX];
    Y = new float[nY];
    Z = new float[nZ];

    // grid coordinate squares relative to orbital center in Bohr
    X2 = new float[nX];
    Y2 = new float[nY];
    Z2 = new float[nZ];
  }

  protected float volume = 1;


  /**
   * 
   * @param originXYZ
   * @param stepsXYZ
   * @param bsSelected
   * @param xyz
   *        full T3[] array -- may be transformed coordinates of Atom[]
   * @param atoms
   *        for debugging only -- full Atom[] array
   * @param points
   * @param renumber
   */
  protected void setupCoordinates(float[] originXYZ, float[] stepsXYZ,
                                  BS bsSelected, T3[] xyz, Atom[] atoms,
                                  T3[] points, boolean renumber) {

    // all coordinates come in as angstroms, not bohr, and are converted here into bohr

    if (atoms == null)
      atoms = (Atom[]) xyz;
    if (points == null) {
      volume = 1;
      for (int i = 3; --i >= 0;) {
        originBohr[i] = originXYZ[i] * unitFactor;
        stepBohr[i] = stepsXYZ[i] * unitFactor;
        volume *= stepBohr[i];
      }
      Logger.info("QuantumCalculation:" + "\n origin = "
          + Escape.eAF(originXYZ) + "\n steps = " + Escape.eAF(stepsXYZ)
          + "\n origin(Bohr)= " + Escape.eAF(originBohr) + "\n steps(Bohr)= "
          + Escape.eAF(stepBohr) + "\n counts= " + nX + " " + nY + " " + nZ);
    }
    
     // Allowing missing atoms allows for selectively removing
     // atoms from the rendering of an MO. This could allow for
     // a subset of the MO -- one atom's contribution, for example -- to be rendered.
     // Maybe a first time this has ever been done?
      

    qmAtoms = new QMAtom[renumber ? bsSelected.cardinality() : xyz.length];
    boolean isAll = (bsSelected == null);
    int i0 = (isAll ? qmAtoms.length - 1 : bsSelected.nextSetBit(0));
    for (int i = i0, j = 0; i >= 0; i = (isAll ? i - 1 : bsSelected
        .nextSetBit(i + 1)))
      qmAtoms[renumber ? j++ : i] = new QMAtom(i, xyz[i], atoms[i], X, Y, Z,
          X2, Y2, Z2, unitFactor);
  }

  public float processPt(T3 pt) {
    // implementation from QuantumCalculationInterface for NCI and MO calculations
    // it IS called.
    doDebug = false;
    if (points == null || nX != 1)
      initializeOnePoint();
    points[0].setT(pt);
    voxelData[0][0][0] = voxelDataTemp[0][0][0] = 0;
    setXYZBohr(points);
    processPoints();
    //System.out.println("qc pt=" + pt + " " + voxelData[0][0][0]);
    return voxelData[0][0][0];
  }


  protected void processPoints() {
    process();
  }

  protected void initializeOnePoint() {
    initializeOnePointQC();
  }

  protected void initializeOnePointQC() {
    points = new P3[1];
    points[0] = new P3();
    if (voxelData == null || voxelData == voxelDataTemp) {
      voxelData = voxelDataTemp = new float[1][1][1];
    } else {
      voxelData = new float[1][1][1];
      voxelDataTemp = new float[1][1][1];
    }
    xMin = yMin = zMin = 0;
    initialize(1, 1, 1, points);
  }

  protected abstract void process();
  
  protected void setXYZBohr(T3[] points) {
    setXYZBohrI(xBohr, 0, nX, points);
    setXYZBohrI(yBohr, 1, nY, points);
    setXYZBohrI(zBohr, 2, nZ, points);
  }

  private void setXYZBohrI(float[] bohr, int i, int n, T3[] points) {
    if (points != null) {
      float x = 0;
      for (int j = 0; j < n; j++) {
        switch (i) {
        case 0:
          x = points[j].x;
          break;
        case 1:
          x = points[j].y;
          break;
        case 2:
          x = points[j].z;
          break;
        }
        bohr[j] = x * unitFactor;
      }
      return;
    }
    bohr[0] = originBohr[i];
    float inc = stepBohr[i];
    for (int j = 0; ++j < n;)
      bohr[j] = bohr[j - 1] + inc;
  }

  public void setMinMax(int ix) {
    yMax = zMax = (ix < 0 ? xMax : ix + 1);
    yMin = zMin = (ix < 0 ? 0 : ix);    
  }
  
  public abstract void createCube();

}