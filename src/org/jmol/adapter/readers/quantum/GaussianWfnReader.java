/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-12 00:46:22 -0500 (Tue, 12 Sep 2006) $
 * $Revision: 5501 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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

import org.jmol.adapter.smarter.AtomSetCollectionReader;

/**
 * Reader for Gaussian Wfn files -- not implemented yet
 *
 **/
public class GaussianWfnReader extends AtomSetCollectionReader {
//  int ac = 0;
//  int shellCount = 0;
//  int gaussianCount = 0;
//  Hashtable moData = new Hashtable();
//  List orbitals = new List();

  
  /* I thought perhaps this would be enough, but now I'm not so sure.
   * 
GAUSSIAN             14 MOL ORBITALS    168 PRIMITIVES        6 NUCLEI
  B    1    (CENTRE  1)   0.00000000  2.98988716  0.00000000  CHARGE =  5.0
  B    2    (CENTRE  2)   2.58931823  1.49494358  0.00000000  CHARGE =  5.0
  B    3    (CENTRE  3)   2.58931823 -1.49494358  0.00000000  CHARGE =  5.0
  B    4    (CENTRE  4)   0.00000000 -2.98988716  0.00000000  CHARGE =  5.0
  B    5    (CENTRE  5)  -2.58931823 -1.49494358  0.00000000  CHARGE =  5.0
  B    6    (CENTRE  6)  -2.58931823  1.49494358  0.00000000  CHARGE =  5.0
CENTRE ASSIGNMENTS    1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1
CENTRE ASSIGNMENTS    1  1  1  1  1  1  1  1  2  2  2  2  2  2  2  2  2  2  2  2
CENTRE ASSIGNMENTS    2  2  2  2  2  2  2  2  2  2  2  2  2  2  2  2  3  3  3  3
CENTRE ASSIGNMENTS    3  3  3  3  3  3  3  3  3  3  3  3  3  3  3  3  3  3  3  3
CENTRE ASSIGNMENTS    3  3  3  3  4  4  4  4  4  4  4  4  4  4  4  4  4  4  4  4
CENTRE ASSIGNMENTS    4  4  4  4  4  4  4  4  4  4  4  4  5  5  5  5  5  5  5  5
CENTRE ASSIGNMENTS    5  5  5  5  5  5  5  5  5  5  5  5  5  5  5  5  5  5  5  5
CENTRE ASSIGNMENTS    6  6  6  6  6  6  6  6  6  6  6  6  6  6  6  6  6  6  6  6
CENTRE ASSIGNMENTS    6  6  6  6  6  6  6  6
TYPE ASSIGNMENTS      1  1  1  1  1  1  1  1  1  2  2  2  3  3  3  4  4  4  1  2
TYPE ASSIGNMENTS      3  4  5  6  7  8  9 10  1  1  1  1  1  1  1  1  1  2  2  2
TYPE ASSIGNMENTS      3  3  3  4  4  4  1  2  3  4  5  6  7  8  9 10  1  1  1  1
TYPE ASSIGNMENTS      1  1  1  1  1  2  2  2  3  3  3  4  4  4  1  2  3  4  5  6
TYPE ASSIGNMENTS      7  8  9 10  1  1  1  1  1  1  1  1  1  2  2  2  3  3  3  4
TYPE ASSIGNMENTS      4  4  1  2  3  4  5  6  7  8  9 10  1  1  1  1  1  1  1  1
TYPE ASSIGNMENTS      1  2  2  2  3  3  3  4  4  4  1  2  3  4  5  6  7  8  9 10
TYPE ASSIGNMENTS      1  1  1  1  1  1  1  1  1  2  2  2  3  3  3  4  4  4  1  2
TYPE ASSIGNMENTS      3  4  5  6  7  8  9 10
EXPONENTS  0.2068882D+04 0.3106496D+03 0.7068303D+02 0.1986108D+02 0.6299305D+01
   */

  @Override
  protected void initializeReader() {
    continuing = false;
  }

  /*
   public void initializeReader() throws Exception {
     readHeader();
     readAtoms();
     readBasis();
     readMolecularOrbitals();
   }
*/

   /*
  private int nMo, nPrimitive;
  // GAUSSIAN             14 MOL ORBITALS    168 PRIMITIVES        6 NUCLEI

  private void readHeader() throws Exception {
    String[] tokens = getTokens();
    nMo = parseInt(tokens[1]);
    nPrimitive = parseInt(tokens[4]);
    ac = parseInt(tokens[6]);
  }

  //   B    1    (CENTRE  1)   0.00000000  2.98988716  0.00000000  CHARGE =  5.0

  private void readAtoms() throws Exception {
    asc.newAtomSet();
    String tokens[];
    for (int i = 0; i < ac; i++) {
      tokens = getTokens(readLine()); 
      Atom atom = asc.addNewAtom();
      atom.elementSymbol = tokens[0];
      setAtomCoord(atom, parseFloat(tokens[4]), parseFloat(tokens[5]), parseFloat(tokens[6]));
    }
  }

  private int[] getIntArray(int n, int pt) {
    int[] a = new int[n];
    String[] tokens;
    int nValues = 0;
    while (nValues < n) {
      tokens = getTokens(readLine());
      for (int i = pt; i < tokens.length; i++)
        a[nValues++] = parseInt(tokens[i]);
    }
    return a;
  }
  
  private float[] getFloatArray(int n, int pt) {
    float[] a = new float[n];
    String[] tokens;
    int nValues = 0;
    while (nValues < n) {
      tokens = getTokens(readLine());
      for (int i = pt; i < tokens.length; i++)
        a[nValues++] = parseFloat(tokens[i]);
    }
    return a;
  }
  
  private void readBasis() throws Exception {
    List sdata = new List();
    List gdata = new List();
    gaussianCount = 0;
    shellCount = 0;
    int[] centers = getIntArray(nPrimitive, 2);
    int[] types = getIntArray(nPrimitive, 2);
    float[] exponents = getFloatArray(nPrimitive, 1);
    int lastAtom = -1;
    for (i = 0; i < nPrimitive; i++) {
      what here?
    }
  }  
   
  void readMolecularOrbitals() throws Exception {
  }

 */
}
