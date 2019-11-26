/*
 * adapted from: http://janocchio.sourceforge.net
 * 
 *  The Janocchio program is (C) 2007 Eli Lilly and Co.
 *  Authors: David Evans and Gary Sharman
 *  Contact : janocchio-users@lists.sourceforge.net.
 *
 *  It is derived in part from Jmol
 *  (C) 2002-2006 The Jmol Development Team
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.
 *  All we ask is that proper credit is given for our work, which includes
 *  - but is not limited to - adding the above copyright notice to the beginning
 *  of your source code files, and to any copyright notice that you may distribute
 *  with programs based on this work.
 *
 *  This program is distributed in the hope that it will be useful, on an 'as is' basis,
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.jmol.quantum;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.BS;
import javajs.util.Lst;

import org.jmol.modelset.Atom;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

/**
 * Class for calculating NOE intensities by full matrix relaxation approach.
 * <p>
 * 
 * create an instance of the class:
 * <p>
 * 
 * NoeMatrix n = new NoeMatrix();
 * <p>
 * 
 * Create the atom list:
 * <p>
 * 
 * n.makeAtomList(x);
 * <p>
 * 
 * where x is the number of atoms (methyls count as 1). add the atoms in turn
 * with:
 * <p>
 * 
 * n.addAtom(x,y,z);
 * <p>
 * 
 * where x y and z are the atom coordinates, or:
 * <p>
 * 
 * n.addMethyl(x,y,z,x1,y1,z1,x2,y2,z2);
 * <p>
 * 
 * which does the same for a methyl. T
 * <p>
 * 
 * hen just call calcNOEs:
 * <p>
 * 
 * double[][] results = n.calcNOEs();
 * <p>
 * 
 * This will need to be in a try statement as this routine throws an exception
 * if the atoms have not been set up properly.
 * 
 * @author YE91009
 * @created 28 February 2007
 */
public class NMRNoeMatrix {
  
  static int id = 0;
  
  public static class NOEParams {

    private boolean noesy;

    
    /**
     * the correlation time in seconds. typical value would be 80E-12
     */
    private double tau;
    private double freq;
    private double tMix;
    private double cutoff;
    private double rhoStar;
    {
      freq = 400 * 2 * Math.PI * 1E6; // freq (entered as MHz)
      tau = 80E-12; //sec (entered as ps)
      tMix = 0.5; // sec
      cutoff = 10; //Ang
      rhoStar = 0.1;
      noesy = true;
    }

    @Override
    public String toString() {
      return "[id=" + id + " freq="+getNMRfreqMHz()+" tau="+tau+" tMix="+tMix+" cutoff="+cutoff+" rhoStar="+rhoStar+" noesy="+noesy+"]";
    }
    
    boolean tainted = true;
    boolean mixingChanged = true;


    public int id; // updated statically

    /**
     * set the correlation time to be used in the NOE calculation
     * 
     * @param t
     */
    public void setCorrelationTimeTauPS(double t) {
      tau = t * 1E-12;
      tainted = true;
    }

    /**
     * sets the mixing time for the NOE experiment
     * 
     * @param t
     *        the mixing time in seconds. Typically 0.5-1.5 seconds for small
     *        molecules
     */
    public void setMixingTimeSec(double t) {
      tMix = t;
      mixingChanged = true;
    }

    /**
     * set the NMR frequency for the NOE simulation
     * 
     * @param f
     *        the frequency in MHz
     */
    public void setNMRfreqMHz(double f) {
      freq = f * 2 * Math.PI * 1E6;
      tainted = true;
    }

    /**
     * sets the cutoff distance beyond which atom interactions are not
     * considered
     * 
     * @param c
     *        the cutoff distance in Angstroms
     */
    public void setCutoffAng(double c) {
      cutoff = c;
      tainted = true;
    }

    public void setRhoStar(double c) {
      rhoStar = c;
      tainted = true;
    }

    /**
     * sets the experiemnt type to NOESY or ROESY
     * 
     * @param b
     *        true for NOESY, flase for ROESY
     */
    public void setNoesy(boolean b) {
      noesy = b;
      tainted = true;
    }

    /**
     * get the correlation time in picoseconds
     * 
     * @return the correlation time in picoseconds
     */
    public double getCorrelationTimeTauPS() {
      return tau;
    }

    /**
     * get the mixing time
     * 
     * @return the mixing time in seconds
     */
    public double getMixingTimeSec() {
      return tMix;
    }

    /**
     * get if NOESY or ROESY was used for simulation
     * 
     * @return true for NOESY, false for ROESY
     */
    public boolean getNoesy() {
      return noesy;
    }

    /**
     * gets the NMR frequency
     * 
     * @return the NMR frequency in MHz
     */
    public double getNMRfreqMHz() {
      return freq / 2 / Math.PI / 1E6;
    }

    /**
     * get the cutoff distance
     * 
     * @return the cutoff in Angstroms
     */
    public double getCutoffAng() {
      return cutoff;
    }

  }

  double[][] eigenValues;
  double[][] eigenVectors;
  double[][] relaxMatrix;
  double[][] noeM;
  double[][] distanceMatrix;
  NOEAtom[] atoms;
  int nHAtoms, atomCounter, i, j, k, m, n, p, q;
  private int[] atomMap;

  /**
   * first index for this frame
   * 
   */
  private int baseIndex = 0;
  private final NOEParams params;

  private NMRNoeMatrix() {
    params = new NOEParams();
  }
  
  public static NMRNoeMatrix createMatrix(Viewer viewer, BS bsMol,
                                          String[] labelArray, NOEParams params) {
    
//    System.out.println("NMRNoeMatrix creation for " + bsMol);
//
    BS bsH = null;
    try {
      bsH = (bsMol.cardinality() == 0 ? new BS() : viewer.getSmartsMatch("[H]",
          bsMol));
    } catch (Exception e1) {
      // not possible - the SMARTS expression is valid.
    }

    // we will fill these two
    Map<Atom, String> labels = new Hashtable<Atom, String>();
    Map<Atom, Integer> indexAtomInMol = new Hashtable<Atom, Integer>();

    Map<String, Lst<Atom>> labelMap = createLabelMapAndIndex(viewer, bsMol,
        labelArray, bsH, labels, indexAtomInMol);
    Lst<Object> hAtoms = createHAtomList(viewer, bsMol, bsH, labels, labelMap);
    NMRNoeMatrix noeMatrix = createNOEMatrix(hAtoms, indexAtomInMol, bsMol.cardinality(),
        bsMol.nextSetBit(0),(params == null ? new NOEParams() : params));
    if (!bsMol.isEmpty()) {
      viewer.getCurrentModelAuxInfo().put("noeMatrix", noeMatrix);
    }
    return noeMatrix;
  }

  public NOEParams getParams() {
    return params;
  }
  
  /**
   * Create noeMatix and indexAtomInNoeMatrix from hAtoms and indexAtomInMol.
   * 
   * @param hAtoms
   * @param indexAtomInMol
   * @param atomCount
   * @param baseIndex
   * @param params 
   * @return NoeMatrix object
   */
  private static NMRNoeMatrix createNOEMatrix(Lst<Object> hAtoms,
                                              Map<Atom, Integer> indexAtomInMol,
                                              int atomCount, int baseIndex,
                                              NOEParams params) {

    int[] map = new int[atomCount];

    int nHAtoms = hAtoms.size();
    NMRNoeMatrix noeMatrix = new NMRNoeMatrix(params);
    noeMatrix.baseIndex = baseIndex;
    noeMatrix.initArrays(nHAtoms);
    for (int i = 0; i < nHAtoms; i++) {
      Object aobj = hAtoms.get(i);
      if (aobj instanceof Atom) {
        Atom a = (Atom) hAtoms.get(i);
        map[(indexAtomInMol.get(a)).intValue()] = i;
        noeMatrix.addAtom(a.x, a.y, a.z);
      } else if (aobj instanceof Lst) {
        @SuppressWarnings("unchecked")
        Lst<Atom> lst = (Lst<Atom>) aobj;
        int nEquiv = lst.size();
        for (int j = 0; j < nEquiv; j++) {
          map[(indexAtomInMol.get(lst.get(j))).intValue()] = i;
        }
        double[] xa = new double[nEquiv];
        double[] ya = new double[nEquiv];
        double[] za = new double[nEquiv];
        for (int j = 0; j < nEquiv; j++) {
          Atom a = lst.get(j);
          xa[j] = a.x;
          ya[j] = a.y;
          za[j] = a.z;
        }
        noeMatrix.addEquiv(xa, ya, za);
      } else {
        Atom[] a = (Atom[]) aobj;
        map[(indexAtomInMol.get(a[0])).intValue()] = i;
        map[(indexAtomInMol.get(a[1])).intValue()] = i;
        map[(indexAtomInMol.get(a[2])).intValue()] = i;
        noeMatrix.addMethyl(a[0].x, a[0].y, a[0].z, a[1].x, a[1].y, a[1].z,
            a[2].x, a[2].y, a[2].z);
      }
    }
    noeMatrix.atomMap = map;
    return noeMatrix;
  }

  private NMRNoeMatrix(NOEParams params) {
    this.params = params;
    params.id = ++id;
  }

  /**
   * calculate the NOESY spectrum at a particular mixing time. Onc eyou have
   * created and built the atom list, this is the only function you need to
   * call.
   * 
   * @exception Exception
   *            Description of the Exception
   * @throws java.lang.Exception
   *         Throws an exception when the atom list has not been created or is
   *         not properly filled with atoms
   */
  public void calcNOEs() throws Exception {
    if (nHAtoms == 0 || atoms == null) {
      noeM = new double[0][0];
      return;
    }
    if (nHAtoms != atomCounter) {
      throw new Exception("Not all atoms have been read in yet!");
    }
    boolean isNew = false;
    if (params.tainted) {
      calcRelaxMatrix();
      Diagonalise();
      isNew = true;
    }
    if (params.tainted || params.mixingChanged) {
      calcNoeMatrix();
      params.mixingChanged = false;
      isNew = true;
    }
    params.tainted = false;
//    if (isNew)
//      System.out.println(toString());
  }

  /**
   * create an empty atom list for subsequent population with atoms
   * 
   * @param n
   *        the number of atoms to be added
   */
  public void initArrays(int n) {
//    System.out.println("NMRNoeMatrix initialized for " + n + " H atoms");
    nHAtoms = n;
    atoms = new NOEAtom[nHAtoms];
    atomCounter = 0;
    relaxMatrix = new double[nHAtoms][nHAtoms];
    eigenValues = new double[nHAtoms][nHAtoms];
    eigenVectors = new double[nHAtoms][nHAtoms];
    noeM = new double[nHAtoms][nHAtoms];
    distanceMatrix = new double[nHAtoms][nHAtoms];
  }

  /**
   * add a proton to the atom list
   * 
   * @param x
   *        the x position of the atom (in Angstroms)
   * @param y
   *        the x position of the atom (in Angstroms)
   * @param z
   *        the z position of the atom (in Angstroms)
   */
  public void addAtom(double x, double y, double z) {
    atoms[atomCounter] = new NOEAtom();
    atoms[atomCounter].x = x;
    atoms[atomCounter].y = y;
    atoms[atomCounter].z = z;
    atoms[atomCounter].methyl = false;
    atomCounter++;
    params.tainted = true;
  }

  /**
   * Add a methyl group to the atom list
   * 
   * @param x
   *        the x position of atom 1 (in Angstroms)
   * @param y
   *        the y position of atom 1 (in Angstroms)
   * @param z
   *        the z position of atom 1 (in Angstroms)
   * @param x1
   *        the x position of atom 2 (in Angstroms)
   * @param y1
   *        the y position of atom 2 (in Angstroms)
   * @param z1
   *        the z position of atom 2 (in Angstroms)
   * @param x2
   *        the x position of atom 3 (in Angstroms)
   * @param y2
   *        the y position of atom 3 (in Angstroms)
   * @param z2
   *        the z position of atom 3 (in Angstroms)
   */
  public void addMethyl(double x, double y, double z, double x1, double y1,
                        double z1, double x2, double y2, double z2) {
    atoms[atomCounter] = new NOEAtom();
    atoms[atomCounter].x = x;
    atoms[atomCounter].y = y;
    atoms[atomCounter].z = z;
    atoms[atomCounter].x1 = x1;
    atoms[atomCounter].y1 = y1;
    atoms[atomCounter].z1 = z1;
    atoms[atomCounter].x2 = x2;
    atoms[atomCounter].y2 = y2;
    atoms[atomCounter].z2 = z2;
    atoms[atomCounter].methyl = true;
    atomCounter++;
    params.tainted = true;
  }

  public void addEquiv(double[] xa, double[] ya, double[] za) {
    atoms[atomCounter] = new NOEAtom();
    atoms[atomCounter].xa = xa;
    atoms[atomCounter].ya = ya;
    atoms[atomCounter].za = za;
    atoms[atomCounter].equiv = true;
    atomCounter++;
    params.tainted = true;
  }

  private void calcRelaxMatrix() {
    double alpha = 5.6965E10;
    double rho;
    double JValSigma;
    double JValRho;
    double rhoStar = params.rhoStar;
    double freq = params.freq;
    double cutoff2 = params.cutoff * params.cutoff;
    double tau = params.tau;
    if (params.noesy) {
      JValSigma = 6.0 * J(2 * freq, tau) - J(0, tau);
      JValRho = 6.0 * J(2 * freq, tau) + 3.0 * J(freq, tau) + J(0, tau);
    } else {
      JValSigma = 3.0 * J(freq, tau) + 2.0 * J(0, tau);
      JValRho = 3.0 * J(2 * freq, tau) + 4.5 * J(freq, tau) + 2.5 * J(0, tau);
    }
    for (i = 0; i < nHAtoms; i++) {
      rho = 0.0;
      for (j = 0; j < nHAtoms; j++) {
        // double distSqrd = (atoms[i].x-atoms[j].x)*(atoms[i].x-atoms[j].x) + (atoms[i].y-atoms[j].y)*(atoms[i].y-atoms[j].y) + (atoms[i].z-atoms[j].z)*(atoms[i].z-atoms[j].z);
        double distSqrd = distanceSqrd(atoms[i], atoms[j]);
        distanceMatrix[i][j] = Math.sqrt(distSqrd);
        double aOverR6;
        if (distSqrd < cutoff2) {
          aOverR6 = alpha / (distSqrd * distSqrd * distSqrd);
        } else {
          aOverR6 = 0;
        }
        if (i < j) {
          relaxMatrix[i][j] = aOverR6 * JValSigma;
          relaxMatrix[j][i] = relaxMatrix[i][j];
        }
        if (i != j) {
          rho = rho + aOverR6 * JValRho;
        }
      }
      relaxMatrix[i][i] = rho + rhoStar;
    }
  }

  private static double J(double w, double tau) {
    return tau / (1 + (w * w * tau * tau));
  }

  private int sign(double x) {
    if (x < 0) {
      return -1;
    }
    return 1;
  }

  private void calcNoeMatrix() {
    double[] tempEVs = new double[nHAtoms];
    double tMix = params.tMix;
    for (i = 0; i < nHAtoms; i++) {
      tempEVs[i] = Math.exp(-eigenValues[i][i] * tMix);
    }
    for (i = 0; i < nHAtoms; i++) {
      for (j = 0; j <= i; j++) {
        double sum = 0;
        for (k = 0; k < nHAtoms; k++) {
          sum += eigenVectors[i][k] * eigenVectors[j][k] * tempEVs[k];
        }
        noeM[i][j] = sum;
        noeM[j][i] = sum;
      }
    }
  }

  private int Diagonalise() {

    int iter = 0;

    for (int i = 0; i < nHAtoms; i++) {
      for (int z = 0; z < nHAtoms; z++) {
        eigenVectors[i][z] = 0.0;
        eigenValues[i][z] = relaxMatrix[i][z];
      }
    }

    for (int i = 0; i < nHAtoms; i++) {
      eigenVectors[i][i] = 1.0;
    }

    String state = "ITERATING";
    int maxIter = 100000;

    while (state == "ITERATING") {
      double max = maxOffDiag();
      if (max > 0.0) {
        rotate();
        iter++;
        if (iter >= maxIter) {
          state = "STOP";
          System.out.println("maximum iteration reached");
        }
      } else {
        state = "SUCCESS";

      }
    }
    return iter;
  }

  private double maxOffDiag() {
    double max = 0.0;
    for (int i = 0; i < nHAtoms - 1; i++) {
      for (int j = i + 1; j < nHAtoms; j++) {
        double aij = Math.abs(eigenValues[i][j]);
        if (aij > max) {
          max = aij;
          p = i;
          q = j;
        }
      }
    }
    return max;
  }

  //Rotates matrix matA through theta in pq-plane to set matA.re[p][q] = 0
  //Rotation stored in matrix matR whose columns are eigenvectors of matA
  private void rotate() {
    // d = cot 2*theta, t = tan theta, c = cos theta, s = sin theta
    double d = (eigenValues[p][p] - eigenValues[q][q])
        / (2.0 * eigenValues[p][q]);
    double t = sign(d) / (Math.abs(d) + Math.sqrt(d * d + 1));
    double c = 1.0 / Math.sqrt(t * t + 1);
    double s = t * c;
    eigenValues[p][p] += t * eigenValues[p][q];
    eigenValues[q][q] -= t * eigenValues[p][q];
    eigenValues[p][q] = eigenValues[q][p] = 0.0;
    for (int k = 0; k < nHAtoms; k++) {// Transform eigenvalues
      if (k != p && k != q) {
        double akp = c * eigenValues[k][p] + s * eigenValues[k][q];
        double akq = -s * eigenValues[k][p] + c * eigenValues[k][q];
        eigenValues[k][p] = eigenValues[p][k] = akp;
        eigenValues[k][q] = eigenValues[q][k] = akq;
      }
    }
    for (int k = 0; k < nHAtoms; k++) {// Store eigenvectors
      double rkp = c * eigenVectors[k][p] + s * eigenVectors[k][q];
      double rkq = -s * eigenVectors[k][p] + c * eigenVectors[k][q];
      eigenVectors[k][p] = rkp;
      eigenVectors[k][q] = rkq;
    }
  }

  static NumberFormat nf = NumberFormat.getInstance();
  static {
    nf.setMinimumFractionDigits(4);
    nf.setMaximumFractionDigits(4);
  }
  
  @Override
  public String toString() {
    StringBuffer sb;
    sb = new StringBuffer();
    for (i = 0; i < nHAtoms; i++) {
      for (j = 0; j < nHAtoms; j++) {
        sb.append(nf.format(noeM[i][j]) + "\t");

      }
      sb.append("\n");
    }
    sb.append(params.toString());
    return sb.toString();
  }

  public String toStringNormRow() {
    StringBuffer sb;
    NumberFormat nf = NumberFormat.getInstance();
    nf.setMinimumFractionDigits(4);
    nf.setMaximumFractionDigits(4);
    sb = new StringBuffer();
    for (i = 0; i < nHAtoms; i++) {
      for (j = 0; j < nHAtoms; j++) {
        double val = noeM[i][j] / noeM[i][i];
        sb.append(nf.format(val) + "\t");

      }
      sb.append("\n");
    }
    return sb.toString();
  }

  private double distanceSqrd(NOEAtom a, NOEAtom b) {

    NOEAtom atom1, atom2;
    double d, d1, d2, d3;
    double prod12, prod13, prod23;
    double d15, d25, d35;

    if (b.methyl && !a.methyl) {//exchange i and j atoms
      atom1 = b;
      atom2 = a;
    } else if (b.equiv && !a.equiv) {
      atom1 = b;
      atom2 = a;
    } else {
      atom1 = a;
      atom2 = b;
    }

    if (atom1.methyl) {// methyl group detected in atom i : use Tropp model
      double a2x, a2y, a2z;
      if (atom2.methyl) {// case of two methyl groups average coords of methyl j
        a2x = (atom2.x + atom2.x1 + atom2.x2) / 3.0;
        a2y = (atom2.y + atom2.y1 + atom2.y2) / 3.0;
        a2z = (atom2.z + atom2.z1 + atom2.z2) / 3.0;
      } else if (atom2.equiv) {//average coords of equivs
        a2x = 0.0;
        a2y = 0.0;
        a2z = 0.0;
        for (int j = 0; j < atom2.xa.length; j++) {
          a2x += atom2.xa[j] / atom2.xa.length;
          a2y += atom2.ya[j] / atom2.xa.length;
          a2z += atom2.za[j] / atom2.xa.length;
        }
      } else {//use normal coords
        a2x = atom2.x;
        a2y = atom2.y;
        a2z = atom2.z;
      }
      double x1 = atom1.x - a2x;
      double y1 = atom1.y - a2y;
      double z1 = atom1.z - a2z;
      double x2 = atom1.x1 - a2x;
      double y2 = atom1.y1 - a2y;
      double z2 = atom1.z1 - a2z;
      double x3 = atom1.x2 - a2x;
      double y3 = atom1.y2 - a2y;
      double z3 = atom1.z2 - a2z;

      d1 = (x1 * x1) + (y1 * y1) + (z1 * z1);
      d2 = (x2 * x2) + (y2 * y2) + (z2 * z2);
      d3 = (x3 * x3) + (y3 * y3) + (z3 * z3);

      d15 = d1 * d1 * Math.sqrt(d1);
      d25 = d2 * d2 * Math.sqrt(d2);
      d35 = d3 * d3 * Math.sqrt(d3);

      prod12 = x1 * x2 + y1 * y2 + z1 * z2;
      prod13 = x1 * x3 + y1 * y3 + z1 * z3;
      prod23 = x2 * x3 + y2 * y3 + z2 * z3;

      d = (2 * d1 * d1) / (d15 * d15);
      d += ((3 * (prod12 * prod12)) - (d1 * d2)) / (d15 * d25);
      d += ((3 * (prod13 * prod13)) - (d1 * d3)) / (d15 * d35);
      d += ((3 * (prod12 * prod12)) - (d2 * d1)) / (d25 * d15);
      d += (2 * d2 * d2) / (d25 * d25);
      d += ((3 * (prod23 * prod23)) - (d2 * d3)) / (d25 * d35);
      d += ((3 * (prod13 * prod13)) - (d3 * d1)) / (d35 * d15);
      d += ((3 * (prod23 * prod23)) - (d3 * d2)) / (d35 * d25);
      d += (2 * d3 * d3) / (d35 * d35);
      //System.err.println("Hello "  + Math.pow(d/18,(-1.0/6.0)));
      return (Math.pow(d / 18.0, -1.0 / 3.0));
    } else if (atom1.equiv) {// equivalent atom - do r6 averaging
      if (atom2.equiv) {
        double dd = 0.0;
        for (int i = 0; i < atom1.xa.length; i++) {
          for (int j = 0; j < atom2.xa.length; j++) {
            double x1 = atom1.xa[i] - atom2.xa[j];
            double y1 = atom1.ya[i] - atom2.ya[j];
            double z1 = atom1.za[i] - atom2.za[j];
            dd += Math.pow((x1 * x1) + (y1 * y1) + (z1 * z1), -3.0);
          }
        }
        return Math.pow(dd / (atom1.xa.length * atom2.xa.length), -1.0 / 3.0);
      }
      double dd = 0.0;
      for (int i = 0; i < atom1.xa.length; i++) {
        double x1 = atom1.xa[i] - atom2.x;
        double y1 = atom1.ya[i] - atom2.y;
        double z1 = atom1.za[i] - atom2.z;
        dd += Math.pow((x1 * x1) + (y1 * y1) + (z1 * z1), -3.0);
      }
      return Math.pow(dd / atom1.xa.length, -1.0 / 3.0);
    } else {// normal distance for non equivalent C-H and C-H2 groups
      double x1 = atom1.x - atom2.x;
      double y1 = atom1.y - atom2.y;
      double z1 = atom1.z - atom2.z;
      return (x1 * x1) + (y1 * y1) + (z1 * z1);
    }
  }

  private void doItAll(File file) {

    System.out.println("starting");
    readAtomsFromFile(file);
    relaxMatrix = new double[nHAtoms][nHAtoms];
    eigenValues = new double[nHAtoms][nHAtoms];
    eigenVectors = new double[nHAtoms][nHAtoms];
    noeM = new double[nHAtoms][nHAtoms];
    System.out.println("read atoms: " + Integer.toString(nHAtoms));
    calcRelaxMatrix();
    System.out.println("built matrix");
    System.out.println("total iterations = " + Integer.toString(Diagonalise()));
    System.out.println("diagonalised matrix");
    calcNoeMatrix();
    System.out.println("calculated NOE matrix");
    System.out.println(toString());
    System.out.println("");
    System.out.println(toStringNormRow());
    try {
      calcNOEs();
    } catch (Exception e) {
      System.out.println(e.toString());
    }
    //    System.out.println(params.getNMRfreqMHz());
    //    System.out.println(params.getCorrelationTimeTauPS());
  }

  private void readAtomsFromFile(File file) {
    atoms = new NOEAtom[200];
    nHAtoms = 0;
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(file));
      br.readLine();
      System.out.println("found file");
      while (true) {
        //br.readLine();
        String[] linetokens = br.readLine().split("\\s+");
        // DAE changed to match particular H atom types in mm files with no atom names
        if (linetokens[1].matches("41") || linetokens[1].matches("44")) {
          atoms[nHAtoms] = new NOEAtom();
          atoms[nHAtoms].x = Double.valueOf(linetokens[14]).doubleValue();
          atoms[nHAtoms].y = Double.valueOf(linetokens[15]).doubleValue();
          atoms[nHAtoms].z = Double.valueOf(linetokens[16]).doubleValue();
          atoms[nHAtoms].methyl = false;
          nHAtoms++;
        }
      }
    } catch (Exception e) {
      System.out.println(e.toString());
    } finally {
      if (br != null)
        try {
          br.close();
        } catch (IOException e) {
        }
    }

  }

  public static void main(String args[]) {
    NOEParams params = new NOEParams();
    params.setNMRfreqMHz(500);
    params.setCorrelationTimeTauPS(80.0);
    params.setMixingTimeSec(0.5);
    params.setCutoffAng(10.0);
    params.setRhoStar(0.1);
    params.setNoesy(true);
    new NMRNoeMatrix(params).doItAll(new File(args[0]));
  }

  static class NOEAtom {

    double x, y, z, x1, y1, z1, x2, y2, z2;
    double[] xa, ya, za;

    boolean methyl;
    boolean equiv;

  }

  private static Map<String, Lst<Atom>> createLabelMapAndIndex(Viewer viewer,
                                                               BS bsMol,
                                                               String[] labelArray,
                                                               BS bsH,
                                                               Map<Atom, String> labels,
                                                               Map<Atom, Integer> indexAtomInMol) {

    Map<String, Lst<Atom>> labelMap = new Hashtable<String, Lst<Atom>>();
    for (int pt = 0, i = bsMol.nextSetBit(0); i >= 0; i = bsMol
        .nextSetBit(i + 1), pt++) {
      Atom a = viewer.ms.at[i];
      indexAtomInMol.put(a, Integer.valueOf(pt));
      if (labelArray != null) {
        String label = labelArray[pt];
        if (labelArray[pt] == null) {
          labels.put(a, "");
          // but no labelMap is necessary;
        } else {
          Lst<Atom> lst = labelMap.get(label);
          if (lst == null) {
            labelMap.put(label, lst = new Lst<Atom>());
          } else {
            bsH.clear(i);
          }
          lst.addLast(a);
          labels.put(a, label);
        }
      }
    }
    return labelMap;
  }

  private static Lst<Object> createHAtomList(Viewer viewer, BS bsMol, BS bsH,
                                             Map<Atom, String> labels,
                                             Map<String, Lst<Atom>> labelMap) {
    /**
     * 
     * Create hAtoms list, which can have three possible element types:
     * 
     * Atom a hydrogen
     * 
     * Atom[3] a methyl group
     * 
     * Lst<Atom> otherwise noted as identical by their label
     * 
     */

    Lst<Object> hAtoms = new Lst<Object>();
    try {
      // find and group all methyl groups -- simple unique SMARTS map here:
      if (!bsMol.isEmpty()) {
        int[][] methyls = viewer.getSmartsMap("C({[H]})({[H]}){[H]}", bsMol,
            JC.SMILES_TYPE_SMARTS | JC.SMILES_MAP_UNIQUE);
        for (int i = methyls.length; --i >= 0;) {
          Atom[] methyl = new Atom[3];
          for (int j = 0; j < 3; j++) {
            int pt = methyls[i][j];
            methyl[j] = viewer.ms.at[pt];
            bsH.clear(pt);
          }
          hAtoms.addLast(methyl);
        }
      }
    } catch (Exception e) {
      // not possible
    }

    // 
    for (int i = bsH.nextSetBit(0); i >= 0; i = bsH.nextSetBit(i + 1)) {
      Atom a = viewer.ms.at[i];
      String label = labels.get(a);
      Lst<Atom> atoms = (label == null ? null : labelMap.get(labels.get(a)));
      if (atoms != null && atoms.size() > 1) {
        hAtoms.addLast(atoms);
      } else {
        hAtoms.addLast(a);
      }
    }
    return hAtoms;
  }

  public double getJmolDistance(int a, int b) {
    return getDistance(atomMap[a - baseIndex], atomMap[b - baseIndex]);
  }

  private double getDistance(int i, int j) {
    return (i < 0 || j < 0 || i >= nHAtoms ? Double.NaN : distanceMatrix[i][j]);
  }

  public double getJmolNoe(int a, int b) {
    return getNoe(atomMap[a - baseIndex], atomMap[b - baseIndex]);
  }

  private double getNoe(int i, int j) {
    return (i < 0 || j < 0 || i >= nHAtoms ? Double.NaN : noeM[i][j]);
  }
 
}
