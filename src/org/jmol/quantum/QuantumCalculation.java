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


import javajs.util.P3d;
import javajs.util.T3d;

import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.util.Escape;
import org.jmol.util.Logger;


abstract public class QuantumCalculation {

  protected boolean doDebug = false;
  protected BS bsExcluded;

  protected double integration = Double.NaN;
  
  public double getIntegration() {
    return integration;
  }
  

  protected final static double bohr_per_angstrom = 1 / 0.52918d;

  protected double[][][] voxelData;
  public double[][][] voxelDataTemp;
  protected int[] countsXYZ;
  
  protected T3d[] points;
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
  
  protected double[] xBohr, yBohr, zBohr;
  protected double[] originBohr = new double[3];
  protected double[] stepBohr = new double[3];
  protected int nX, nY, nZ;
  
  // grid coordinates relative to orbital center in Bohr 
  public double[] X, Y, Z;

  // grid coordinate squares relative to orbital center in Bohr
  public double[] X2, Y2, Z2;

  // range in bohr to consider affected by an atomic orbital
  // this is a cube centered on an atom of side rangeBohr*2
  protected double rangeBohrOrAngstroms = 10; //bohr; about 5 Angstroms
  
  protected double unitFactor = bohr_per_angstrom;

  protected void initialize(int nX, int nY, int nZ, T3d[] points) {
    initialize0(nX, nY ,nZ, points);
  }

  protected void initialize0(int nX, int nY, int nZ, T3d[] points) {
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
    xBohr = new double[nX];
    yBohr = new double[nY];
    zBohr = new double[nZ];

    // grid coordinates relative to orbital center in Bohr 
    X = new double[nX];
    Y = new double[nY];
    Z = new double[nZ];

    // grid coordinate squares relative to orbital center in Bohr
    X2 = new double[nX];
    Y2 = new double[nY];
    Z2 = new double[nZ];
  }

  protected double volume = 1;


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
  protected void setupCoordinates(double[] originXYZ, double[] stepsXYZ,
                                  BS bsSelected, T3d[] xyz, Atom[] atoms,
                                  T3d[] points, boolean renumber) {

    // all coordinates come in as angstroms, not bohr, and are converted here into bohr

    if (atoms == null)
      atoms = (Atom[]) xyz;
    double[] range = new double[] {nX, nY, nZ};
    if (points == null) {
      volume = 1;
      for (int i = 3; --i >= 0;) {
        originBohr[i] = originXYZ[i] * unitFactor;
        stepBohr[i] = stepsXYZ[i] * unitFactor;
        range[i] = (stepsXYZ[i] * range[i]) * unitFactor/bohr_per_angstrom  ;
        volume *= stepBohr[i];
      }
      Logger.info("QuantumCalculation:" 
          + "\n origin = " + Escape.eAD(originXYZ) 
          + "\n range (Ang) = " +  Escape.eAD(range)
          + "\n steps (Bohr)= " + Escape.eAD(stepBohr)
          + "\n origin(Bohr)= " + Escape.eAD(originBohr) 
          + "\n steps = "
          + Escape.eAD(stepBohr) + "\n counts= " + nX + " " + nY + " " + nZ);
    }
    
     // Allowing missing atoms allows for selectively removing
     // atoms from the rendering of an MO. This could allow for
     // a subset of the MO -- one atom's contribution, for example -- to be rendered.
     // Maybe a first time this has ever been done?
    if (atoms == null)
      return; // NciCalculation !promolecular
    qmAtoms = new QMAtom[renumber ? bsSelected.cardinality() : xyz.length];
    boolean isAll = (bsSelected == null);
    int i0 = (isAll ? qmAtoms.length - 1 : bsSelected.nextSetBit(0));
    for (int i = i0, j = 0; i >= 0; i = (isAll ? i - 1 : bsSelected
        .nextSetBit(i + 1)))
      qmAtoms[renumber ? j++ : i] = new QMAtom(i, xyz[i], atoms[i], X, Y, Z,
          X2, Y2, Z2, unitFactor);
  }

  public double processPt(T3d pt) {
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
    points = new P3d[1];
    points[0] = new P3d();
    if (voxelData == null || voxelData == voxelDataTemp) {
      voxelData = voxelDataTemp = new double[1][1][1];
    } else {
      voxelData = new double[1][1][1];
      voxelDataTemp = new double[1][1][1];
    }
    xMin = yMin = zMin = 0;
    initialize(1, 1, 1, points);
  }

  protected abstract void process();
  
  protected void setXYZBohr(T3d[] points) {
    setXYZBohrI(xBohr, 0, nX, points);
    setXYZBohrI(yBohr, 1, nY, points);
    setXYZBohrI(zBohr, 2, nZ, points);
  }

  private void setXYZBohrI(double[] bohr, int i, int n, T3d[] points) {
    if (points != null) {
      double x = 0;
      for (int j = 0; j < n; j++) {
        switch (i) {
        case 0:
          x = (double) points[j].x;
          break;
        case 1:
          x = (double) points[j].y;
          break;
        case 2:
          x = (double) points[j].z;
          break;
        }
        bohr[j] = x * unitFactor;
      }
      return;
    }
    bohr[0] = originBohr[i];
    double inc = stepBohr[i];
    for (int j = 0; ++j < n;)
      bohr[j] = bohr[j - 1] + inc;
  }

  public void setMinMax(int ix) {
    yMax = zMax = (ix < 0 ? xMax : ix + 1);
    yMin = zMin = (ix < 0 ? 0 : ix);    
  }
  
  public abstract void createCube();

}
