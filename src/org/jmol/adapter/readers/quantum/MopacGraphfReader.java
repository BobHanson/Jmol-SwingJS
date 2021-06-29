/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-08-27 21:07:49 -0500 (Sun, 27 Aug 2006) $
 * $Revision: 5420 $
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

import java.util.Hashtable;
import java.util.Map;

import javajs.util.AU;
import javajs.util.Lst;

import org.jmol.adapter.smarter.Atom;

/**
 * Reads Mopac 2007 GRAPHF output files
 *
 * @author Bob Hanson <hansonr@stolaf.edu>
 * 
 */
public class MopacGraphfReader extends MopacSlaterReader {

  private int ac;

  private int nCoefficients;

  @Override
  protected void initializeReader() {
    alphaBeta = "alpha";
  }

  @Override
  protected boolean checkLine() throws Exception {
    readAtoms();
    if (doReadMolecularOrbitals) {
      readSlaterBasis();
      readMolecularOrbitals(false);
      if (readKeywords())
        readMolecularOrbitals(true);
    }
    continuing = false;
    return false;
  }

  private void readAtoms() throws Exception {
    asc.newAtomSet();
    ac = parseIntStr(line);
    atomicNumbers = new int[ac];
    for (int i = 0; i < ac; i++) {
      rd();
      atomicNumbers[i] = parseIntRange(line, 0, 4);
      Atom atom = asc.addNewAtom();
      setAtomCoordXYZ(atom, parseFloatRange(line, 4, 17),
          parseFloatRange(line, 17, 29), parseFloatRange(line, 29, 41));
      if (line.length() > 41)
        atom.partialCharge = parseFloatStr(line.substring(41));
      atom.elementSymbol = getElementSymbol(atomicNumbers[i]);
      //System.out.println(atom.elementSymbol + " " + atom.x + " " + atom.y + " " + atom.z);
    }
  }

  /*
   *  see http://openmopac.net/manual/graph.html
   *  
   *  <code>
   Block 1, 1 line: Number of atoms (5 characters), plain text: "MOPAC-Graphical data"
   Block 2, 1 line per atom: Atom number (4 characters), Cartesian coordinates (3 sets of 12 characters)
   Block 3, 1 line per atom: Orbital exponents for "s", "p", and "d" Slater orbitals. (3 sets of 11 characters)
   Block 4, number of orbitals squared, All the molecular orbital coefficients in the order M.O. 1, M.O.2, etc. (5 data per line, 15 characters per datum, FORTRAN format: 5d15.8)
   Block 4, inverse-square-root of overlap matrix, (number of orbitals*(number of orbitals+1))/2.
   4 MOPAC-Graphical data
   
   8    0.0000000   0.0000000   0.0000000
   6    1.2108153   0.0000000   0.0000000
   1    1.7927832   0.9304938   0.0000000
   1    1.7927832  -0.9304938   0.0000000
  
   
   0         1         2         3         4
   01234567890123456789012345678901234567890
   
  
   5.4217510  2.2709600  0.0000000
   2.0475580  1.7028410  0.0000000
   1.2686410  0.0000000  0.0000000
   1.2686410  0.0000000  0.0000000
   </code>
   */

  private void readSlaterBasis() throws Exception {
    /*
     * We have two data structures for each slater, using the WebMO format: 
     * 
     * <code>
     * int[] slaterInfo[] = {iatom, a, b, c, d}
     * float[] slaterData[] = {zeta, coef}
     * 
     * where
     * 
     *  psi = (coef)(x^a)(y^b)(z^c)(r^d)exp(-zeta*r)
     * 
     * except: a == -2 ==> z^2 ==> (coef)(2z^2-x^2-y^2)(r^d)exp(-zeta*r)
     *    and: b == -2 ==> (coef)(x^2-y^2)(r^d)exp(-zeta*r)
     * </code>
     */
    nCoefficients = 0;
    getSlaters();
    float[] values = new float[3];
    for (int iAtom = 0; iAtom < ac; iAtom++) {
      getTokensFloat(rd(), values, 3);
      int atomicNumber = atomicNumbers[iAtom];
      createMopacSlaters(iAtom, atomicNumber, values, true);
    }
    nCoefficients = slaters.size();
    setSlaters(false);
  }

  private float[][] invMatrix;

  private boolean isNewFormat;
  private Lst<float[]> orbitalData;
  private Lst<String> orbitalInfo;

  private void readMolecularOrbitals(boolean isBeta) throws Exception {

    // read mo coefficients

    //  (5 data per line, 15 characters per datum, FORTRAN format: 5d15.8)

    if (isBeta)
      alphaBeta = "beta";
    float[][] list = null;
    if (rd() == null)
      return;
    isNewFormat = (line.indexOf("ORBITAL") >= 0);
    if (isNewFormat) {
      orbitalData = new Lst<float[]>();
      if (line.length() > 10)
        orbitalInfo = new Lst<String>();
    } else {
      list = new float[nCoefficients][nCoefficients];
    }
    for (int iMo = 0; iMo < nCoefficients; iMo++) {
      if (iMo != 0)
        rd();
      float[] data;
      if (isNewFormat) {
        if (line == null || line.indexOf("ORBITAL") < 0
            || line.indexOf("ORBITAL_LIST") >= 0)
          break;
        orbitalData.addLast(data = new float[nCoefficients]);
        if (orbitalInfo != null)
          orbitalInfo.addLast(line);
        rd();
      } else {
        data = list[iMo];
      }
      fillFloatArray(line, 15, data);
    }
    if (invMatrix == null) {
      if (isNewFormat && line.indexOf("MATRIX") < 0)
        rd();
      // read lower triangle of symmetric inverse sqrt matrix and multiply
      invMatrix = AU.newFloat2(nCoefficients);
      for (int iMo = 0; iMo < nCoefficients; iMo++)
        fillFloatArray(null, 15, invMatrix[iMo] = new float[iMo + 1]);
    }
    nOrbitals = (orbitalData == null ? nCoefficients : orbitalData.size());
    if (orbitalData != null) {
      list = AU.newFloat2(nOrbitals);
      for (int i = nOrbitals; --i >= 0;)
        list[i] = orbitalData.get(i);
    }
    float[][] list2 = new float[nOrbitals][nCoefficients];
    for (int i = 0; i < nOrbitals; i++)
      for (int j = 0; j < nCoefficients; j++) {
        for (int k = 0; k < nCoefficients; k++)
          list2[i][j] += (list[i][k]
              * (k >= j ? invMatrix[k][j] : invMatrix[j][k]));
        if (Math.abs(list2[i][j]) < MIN_COEF)
          list2[i][j] = 0;
      }
    /*
     System.out.println("MO coefficients: ");
     for (int i = 0; i < nCoefficients; i++) {
     System.out.print((i + 1) + ": ");
     for (int j = 0; j < nCoefficients; j++)
     System.out.print(" " + list2[i][j]);
     System.out.println();
     }
     */

    // read MO energies and occupancies, and fill "coefficients" element
    if (isNewFormat && orbitalInfo == null && line != null
        && line.indexOf("ORBITAL_LIST") < 0)
      rd();
    float[] values = new float[2];
    for (int iMo = 0; iMo < nOrbitals; iMo++) {
      Map<String, Object> mo = new Hashtable<String, Object>();
      if (orbitalInfo != null) {
        line = orbitalInfo.get(iMo);
        String[] tokens = getTokens();
        mo.put("energy", Float.valueOf(parseFloatStr(tokens[3])));
        mo.put("occupancy", Float.valueOf(parseFloatStr(tokens[1])));
      } else if (rd() != null) {
        getTokensFloat(line, values, 2);
        mo.put("energy", Float.valueOf(values[0]));
        mo.put("occupancy", Float.valueOf(values[1]));
      }
      mo.put("coefficients", list2[iMo]);
      if (isBeta)
        mo.put("type", "beta");
      line = "\n";
      if (filterMO())
        setMO(mo);
    }
    setMOs("eV");
  }

  private boolean readKeywords() throws Exception {
    if (rd() == null || line.indexOf(" Keywords:") < 0)
      return false;
    moData.put("calculationType", calculationType = line.substring(11).trim());
    boolean isUHF = (line.indexOf("UHF") >= 0);
    if (isUHF) {
      for (int i = orbitals.size(); --i >= 0;) {
        orbitals.get(i).put("type", "alpha");
      }
    }
    return isUHF;
  }

}
