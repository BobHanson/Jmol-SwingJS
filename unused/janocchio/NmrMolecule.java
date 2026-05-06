/*  
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
 *
 * distanceJMolecule.java
 *
 * Created on 03 April 2006, 17:05
 *
 */

package org.openscience.jmol.app.janocchio;

import java.util.Vector;

import javajs.util.BS;

import org.jmol.modelset.Atom;
import org.jmol.quantum.NMRCalculation;
import org.jmol.quantum.NMRNoeMatrix;

public class NmrMolecule {

  final protected NMR_Viewer viewer;
  final NMR_JmolPanel nmrPanel;
  final private BS bsMol;
  final private String[] labelArray;

  final protected Vector<Double> distances = new Vector<Double>();
  final protected Vector<Double> couples = new Vector<Double>();
  final protected Vector<DihedralCouple> couplesWhole = new Vector<DihedralCouple>();
  protected NMRNoeMatrix noeMatrix;
  protected String CHequation = "was";


  /**
   * 
   * @param nmrPanel
   * @param bsMol
   * @param labelArray
   *        optional array of atom labels, one per atom in the current frame
   * @param forNOE
   */
  public NmrMolecule(NMR_JmolPanel nmrPanel, BS bsMol,
      String[] labelArray, boolean forNOE) {
    this.nmrPanel = nmrPanel;
    viewer = (NMR_Viewer) nmrPanel.vwr;
    this.bsMol = bsMol;
    this.labelArray = labelArray;
    addAtomstoMatrix();

  }

  protected DihedralCouple getDihedralCouple(Atom[] atoms) {
    return new DihedralCouple(atoms, false);
  }

  NMRNoeMatrix.NOEParams params = new NMRNoeMatrix.NOEParams();
  /**
   * Generate noeMatrix and map fields for DistanceJMolecule.
   * 
   */
  protected void addAtomstoMatrix() {
    noeMatrix = NMRNoeMatrix.createMatrix(viewer, bsMol, labelArray, params);
    viewer.setFrameModelInfo("noeMatrix", noeMatrix);
    try {
      noeMatrix.calcNOEs();
    } catch (Exception e) {
    }
    System.out.println("NMRMolecule saved \n" + noeMatrix.toString());
  }

  public Vector<Double> getDistances() {
    return distances;
  }

  public Vector<Double> getCouples() {
    return couples;
  }

  public void calcNOEs() {

    try {
      noeMatrix.calcNOEs();
    } catch (Exception e) {
      System.out.println(e.toString());
    }
  }

  // Adds couple to the list that will be output. USed in command line tool cdkReade
  public void addCouple(Atom[] atoms) {
    DihedralCouple dihe = getDihedralCouple(atoms);
    double jvalue = dihe.getJvalue();
    if (Double.isNaN(jvalue))
      return;
    couples.add(new Double(jvalue));
    couplesWhole.add(dihe);
  }

  public void addJmolCouple(int a1, int a2, int a3, int a4) {
    addCouple(new Atom[] { viewer.getAtomAt(a1),
        viewer.getAtomAt(a2), viewer.getAtomAt(a3),
        viewer.getAtomAt(a4) });
  }

  // Returns couple does not add to output list. Used in NMR viewer applet 
  public double[] calcCouple(Atom[] atoms) {
    DihedralCouple dc = getDihedralCouple(atoms);
    double jvalue = dc.getJvalue();
    return (Double.isNaN(jvalue) ? null
        : new double[] { dc.getTheta(), jvalue });
  }

  public double[] calcJmolCouple(int a1, int a2, int a3, int a4) {
    return calcCouple(new Atom[] { viewer.getAtomAt(a1),
        viewer.getAtomAt(a2), viewer.getAtomAt(a3),
        viewer.getAtomAt(a4) });
  }

  public void setCorrelationTimeTauPS(double t) {
    params.setCorrelationTimeTauPS(t);
  }

  /**
   * sets the mixing time for the NOE experiment
   * 
   * @param t
   *        the mixing time in seconds. Typically 0.5-1.5 seconds for small
   *        molecules
   */
  public void setMixingTimeSec(double t) {
    params.setMixingTimeSec(t);
  }

  /**
   * set the NMR frequency for the NOE simulation
   * 
   * @param f
   *        the frequency in MHz
   */
  public void setNMRfreqMHz(double f) {
    params.setNMRfreqMHz(f);
  }

  /**
   * sets the cutoff distance beyond which atom interactions are not considered
   * 
   * @param c
   *        the cutoff distance in Angstroms
   */
  public void setCutoffAng(double c) {
    params.setCutoffAng(c);
  }

  public void setRhoStar(double c) {
    params.setRhoStar(c);
  }

  public void setNoesy(boolean b) {
    params.setNoesy(b);
  }

  public void setCHequation(String eq) {
    this.CHequation = eq;
  }

  public double getJmolNoe(int a, int b) {
    return noeMatrix.getJmolNoe(a, b);
  }

  /**
   * Add using Jmol atom index
   * 
   * @param a 
   * @param b
   */
  public void addJmolDistance(int a, int b) {

    distances.add(new Double(noeMatrix.getJmolDistance(a, b)));

  }

  /**
   * 
   * Calc using Jmol atom index
   * @param a 
   * @param b 
   * @return NOE-based distance, averaged for equivalent H atoms
   */
  public double getJmolDistance(int a, int b) {
    return noeMatrix.getJmolDistance(a, b);
  }

  class DihedralCouple {

    final protected double[] data;

    DihedralCouple(Atom[] atoms, boolean forNOE) {
      String CHEquation = nmrPanel.coupleTable.CHequation;
      data = NMRCalculation.calc2or3JorNOE(viewer, atoms, CHEquation,
          forNOE ? NMRCalculation.MODE_CALC_NOE : NMRCalculation.MODE_CALC_J);
    }

    public double getTheta() {
      return data[0];
    }

    public double getJvalue() {
      return data[1];
    }

  }

}
