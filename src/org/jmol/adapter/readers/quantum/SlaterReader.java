/* $RCSfile$
 * $Author: egonw $
 * $Date: 2006-03-18 15:59:33 -0600 (Sat, 18 Mar 2006) $
 * $Revision: 4652 $
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
package org.jmol.adapter.readers.quantum;


import org.jmol.quantum.SlaterData;
import org.jmol.util.Logger;

import javajs.util.Lst;
import java.util.Arrays;
import java.util.Comparator;

import java.util.Map;

/**
 * 
 * @author hansonr <hansonr@stolaf.edu>
 */
abstract class SlaterReader extends MOReader {

  /*
   * -- this abstract superclass is cartesian bases only (s, p, d, f)
   * 
   * -- MopacReader overrides this for spherical bases (s, p, d)
   * 
   */

  protected boolean scaleSlaters = true;

  /**
   * 
   * We build two data structures for each slater: 
   * 
   * int[] slaterInfo[] = {iatom, a, b, c, d}
   * double[] slaterData[] = {zeta, coef}
   * 
   * where
   * 
   *  psi = (coef)(x^a)(y^b)(z^c)(r^d)exp(-zeta*r)
   * 
   *  Mopac: a == -2 ==> z^2 ==> (coef)(2z^2-x^2-y^2)(r^d)exp(-zeta*r)
   *    and: b == -2 ==> (coef)(x^2-y^2)(r^d)exp(-zeta*r)
   *    
   * @param iAtom now 1-based
   * @param a
   * @param b
   * @param c
   * @param d
   * @param zeta
   * @param coef
   */
  protected final SlaterData addSlater(int iAtom, int a, int b, int c, int d, 
                        double zeta, double coef) {
    //System.out.println ("SlaterReader " + slaters.size() + ": " + iAtom + " " + a + " " + b +  " " + c + " " + d + " " + zeta + " " + coef);
    SlaterData sd = new SlaterData(iAtom, a, b, c, d, zeta, coef);
    getSlaters().addLast(sd);
    return sd;
  }

  protected Lst<SlaterData> getSlaters() {
    return (slaters == null ? slaters = new Lst<SlaterData>() : slaters);
  }
  
  protected void addSlater(SlaterData sd, int n) {
    sd.index = n;
    getSlaters().addLast(sd);    
  }

  /**
   * after the vectors intinfo and floatinfo are completed, we
   * @param doSort TODO
   */
  protected final void setSlaters(boolean doSort) {
    if (slaters == null || slaters.size() == 0)
      return;
    if (slaterArray == null) {
      int nSlaters = slaters.size();
      slaterArray = new SlaterData[nSlaters];
      for (int i = 0; i < slaterArray.length; i++) 
        slaterArray[i] = slaters.get(i);
    }
    if (scaleSlaters)
      for (int i = 0; i < slaterArray.length; i++) {
        SlaterData sd = slaterArray[i];
        sd.coef *= scaleSlater(sd.x, sd.y, sd.z, sd.r, sd.zeta);
        if (debugging) {
          Logger.debug("SlaterReader " + i + ": " + sd.atomNo + " " + sd.x + " " + sd.y +  " " + sd.z + " " + sd.r + " " + sd.zeta + " " + sd.coef);
        }
      }
    if (doSort) {
      Arrays.sort(slaterArray, new SlaterSorter());
      int[] pointers = new int[slaterArray.length];      
      for (int i = 0; i < slaterArray.length; i++)
        pointers[i] = slaterArray[i].index;
      sortOrbitalCoefficients(pointers);
    }
    moData.put("slaters", slaterArray);
    asc.setCurrentModelInfo("moData", moData);
  }

  class SlaterSorter implements Comparator<SlaterData> {
    @Override
    public int compare(SlaterData sd1, SlaterData sd2) {
      return ( sd1.atomNo < sd2.atomNo ? -1 : sd1.atomNo > sd2.atomNo ? 1 : 0);
    }    
  }

  protected final void setMOs(String units) {
    moData.put("mos", orbitals);
    moData.put("energyUnits", units);
    finalizeMOData(moData);
  }

  /**
   * sorts coefficients by atomic number for speed later
   * 
   * @param pointers
   */
  protected void sortOrbitalCoefficients(int[] pointers) {
    // now sort the coefficients as well
    for (int i = orbitals.size(); --i >= 0; ) {
      Map<String, Object> mo = orbitals.get(i);
      double[] coefs = (double[]) mo.get("coefficients");
      double[] sorted = new double[pointers.length];
      for (int j = 0; j < pointers.length; j++) {
        int k = pointers[j];
        if (k < coefs.length)
          sorted[j] = coefs[k];
      }
      mo.put("coefficients", sorted);
    }
  }
  
  /**
   * 
   * sorts orbitals by energy rather than by symmetry
   * so that we can use "MO HOMO" "MO HOMO - 1" "MO LUMO"
   * 
   */
  
  @SuppressWarnings("unchecked")
  protected void sortOrbitals() {
    Map<String, Object>[] array = orbitals.toArray(new Map[0]);
    Arrays.sort(array, new OrbitalSorter());
    orbitals.clear();
    for (int i = 0; i < array.length; i++)
      orbitals.addLast(array[i]);    
  }
  
  class OrbitalSorter implements Comparator<Map<String, Object>> {
    @Override
    public int compare(Map<String, Object> mo1, Map<String, Object> mo2) {
      double e1 = ((Number) mo1.get("energy")).doubleValue();
      double e2 = ((Number) mo2.get("energy")).doubleValue();
      return ( e1 < e2 ? -1 : e2 < e1 ? 1 : 0);
    }    
  }

  ///////////// orbital scaling //////////////////
  
  /**
   * Perform implementation-specific scaling.
   * This method is subclassed in MopacSlaterReader
   * to handle spherical slaters
   * 
   * @param ex
   * @param ey
   * @param ez
   * @param er
   * @param zeta
   * @return scaling factor
   */
  protected double scaleSlater(int ex, int ey, int ez, int er, double zeta) {
    int el = ex + ey + ez;
    switch (el) {
    case 0: //S
    case 1: //P
      ez = -1; // no need for ex, ey, ez in that case
      break;
    }
    // A negative zeta means this is contracted, so 
    // there are not as many molecular orbital 
    // coefficients as there are slaters. For example, 
    // an atom's s orbital might have one coefficient
    // for a set of three slaters -- the contracted set.
    return getSlaterConstCartesian(el + er + 1, 
        Math.abs(zeta), el, ex, ey, ez);
  }

  /**
   * Sincere thanks to Miroslav Kohout (DGRID) for helping me get this right
   *   -- Bob Hanson, 1/5/2010
   *   
   * slater scaling based on zeta, n, l, and x y z exponents.
   * 
   * sqrt[(2zeta)^(2n + 1) * f  / 4pi / (2n)!]
   * 
   * where 
   * 
   * f = (2 el + 1)!! / (2 ex - 1)!! / (2 ey - 1)!! / (2 ez - 1)!!
   *     
   * The double factorials are precalculated.
   * 
   * @param f
   * @param zeta
   * @param n
   * @return scaled exponent
   */
  protected static double fact(double f, double zeta, int n) {
    return Math.pow(2 * zeta, n + 0.5) * Math.sqrt(f * _1_4pi / fact_2n[n]);
  }

  protected final static double _1_4pi = 0.25 / Math.PI;

  // (2n)!        
  protected final static double[] fact_2n = new double[] {
    1, 2, 24, 720, 40320, 3628800, 479001600 };
  //0  1  2    3     4       5          6
  //0! 2! 4!  6!     8! *9*10^    *11*12^                                                      x   0  1  2  3   4
  // (2x - 1)!!   double factorial                        s  p  d   f
  private final static double[] dfact2 = new double[] { 1, 1, 3, 15, 105 };
  
  /**
   *  scales slater using double factorials involving 
   *  quantum number n, l, and xyz exponents. fact2[x] is (2x - 1)!!
   *  Since x!! = 1 for x = 1, 0 or -1, we can just ignore this
   *  part for s and p orbitals, where x, y, and z are all 0 or 1.
   *  
   *  7!! = 105
   *  5!! = 15
   *  3!! = 3
   *  
   *  Numerators/4pi:
   *  
   *  all d orbitals:     fact2[3] = (2*2 + 1)!! = 5!! = 15/4pi 
   *  all f orbitals:     fact2[4] = (2*3 + 1)!! = 7!! = 105/4pi
   *  
   *  Denominators:
   *  
   *  dxy, dyz, dxz all are 1 giving 15/4pi
   *  dx2, dy2, and dz2 all have one "2", giving 15/3!!/4pi or 5/4pi
   *   
   * @param n
   * @param zeta
   * @param el
   * @param ex
   * @param ey
   * @param ez
   * @return scaled exponent
   */
  protected final static double getSlaterConstCartesian(int n, double zeta,
                                                       int el, int ex, int ey,
                                                       int ez) {
    
    // f = (2 el + 1)!! / (2 ex - 1)!! / (2 ey - 1)!! / (2 ez - 1)!!

    double f = ez < 0 ? dfact2[el + 1] : dfact2[el + 1] / dfact2[ex] / dfact2[ey] / dfact2[ez];
    return fact(f, zeta, n);
  }

}
