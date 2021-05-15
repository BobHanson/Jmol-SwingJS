/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-16 14:11:08 -0500 (Sat, 16 Sep 2006) $
 * $Revision: 5569 $
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

import java.util.Map;

import javajs.util.Lst;
import javajs.util.PT;

import org.jmol.adapter.smarter.Atom;

public class GamessUKReader extends GamessReader {

  @Override
  protected void initializeReader() throws Exception {
    super.initializeReader();
  }
  
  /**
   * @return true if need to read new line
   * @throws Exception
   * 
   */
  @Override
  protected boolean checkLine() throws Exception {
    if (line.indexOf("BASIS OPTIONS") >= 0) {
      readBasisInfo();
      return true;
    }
    if (line.indexOf("$CONTRL OPTIONS") >= 0) {
      readControlInfo();
      return true;
    }
    if (line.indexOf("contracted primitive functions") >= 0) {
      readGaussianBasis(
          "======================================================", "======");
      return false;
    }
    if (line.indexOf("molecular geometry") >= 0) {
      if (!doGetModel(++modelNumber, null))
        return checkLastModel();
      atomNames = new  Lst<String>();
      readAtomsInBohrCoordinates();
      return true;
    }
    if (!doProcessLines)
      return true;
    if (line.indexOf("FREQUENCY_INFO_WOULD_BE_HERE") >= 0) {
      // not implemented for readFrequencies();
      return true;
    }
    if (line.indexOf("SYMMETRY ASSIGNMENT") >= 0) {
      readOrbitalSymmetryAndOccupancy();
      return false;
    }
    if (line.indexOf("- ALPHA SET -") >= 0)
      alphaBeta = "alpha";
    else if (line.indexOf("- BETA SET -") >= 0)
      alphaBeta = "beta";
    else if (line.indexOf("eigenvectors") >= 0) {
      readMolecularOrbitals(HEADER_GAMESS_UK_MO);
      setOrbitalSymmetryAndOccupancy();
      return false;
    }
    return checkNboLine();
  }

  @Override
  protected void readAtomsInBohrCoordinates() throws Exception {
/*
         *     atom   atomic                coordinates                 number of      *
         *            charge       x             y              z       shells         *
         *                                                                             *
         *******************************************************************************
         *                                                                             *
         *                                                                             *
         *    c01       6.0   2.2681106     -1.3191283      0.0000000       3          *
         *                                                                1s  2sp 2sp  *
0         1         2         3         4         5         6         7    
01234567890123456789012345678901234567890123456789012345678901234567890123456789

*/    

    discardLinesUntilContains("*****");
    discardLinesUntilContains("atom");
    discardLinesUntilContains("*****");
    asc.newAtomSet();
    while (rd() != null
        && line.indexOf("*****") < 0) {
      if (line.charAt(14) == ' ')
        continue;
      String[] tokens = getTokens();
      int atomicNumber = (int) parseFloatStr(tokens[2]);
      Atom atom = setAtomCoordScaled(null, tokens, 3, ANGSTROMS_PER_BOHR);
      atom.elementSymbol = getElementSymbol(atomicNumber);
      setAtom(atom, atomicNumber, tokens[1], null);
    }
  }

  /*
   * 


 atom        shell   type  prim       exponents            contraction coefficients
 =================================================================================================================

 c01     


                 1   1s       1     3047.524880       0.536345  (    0.001835  )
                 1   1s       2      457.369518       0.989452  (    0.014037  )
                 1   1s       3      103.948685       1.597283  (    0.068843  )
                 1   1s       4       29.210155       2.079187  (    0.232184  )
                 1   1s       5        9.286663       1.774174  (    0.467941  )
                 1   1s       6        3.163927       0.612580  (    0.362312  )


                 2   2sp      7        7.868272      -0.399556  (   -0.119332  )       1.296082  (    0.068999  )
                 2   2sp      8        1.881289      -0.184155  (   -0.160854  )       0.993754  (    0.316424  )
                 2   2sp      9        0.544249       0.516390  (    1.143456  )       0.495953  (    0.744308  )


                 3   2sp     10        0.168714       0.187618  (    1.000000  )       0.154128  (    1.000000  )

 c02     


                 4   1s      11     3047.524880       0.536345  (    0.001835  )
                 4   1s      12      457.369518       0.989452  (    0.014037  )
                 4   1s      13      103.948685       1.597283  (    0.068843  )
                 4   1s      14       29.210155       2.079187  (    0.232184  )
                 4   1s      15        9.286663       1.774174  (    0.467941  )
                 4   1s      16        3.163927       0.612580  (    0.362312  )


                 5   2sp     17        7.868272      -0.399556  (   -0.119332  )       1.296082  (    0.068999  )
                 5   2sp     18        1.881289      -0.184155  (   -0.160854  )       0.993754  (    0.316424  )
                 5   2sp     19        0.544249       0.516390  (    1.143456  )       0.495953  (    0.744308  )


                 6   2sp     20        0.168714       0.187618  (    1.000000  )       0.154128  (    1.000000  )


   */

  @Override
  protected String fixShellTag(String tag) {
    // 1s --> S; 2sp --> SP
    return tag.substring(1).toUpperCase();
  }


  /*
   * 
   ======================================================================================
       SYMMETRY ASSIGNMENT
   ======================================================================================
   E level    m.o.     symmetry           orbital         orbital  degeneracy  occupancy
          energy (a.u.)    energy (e.v)
   ======================================================================================
   1      1 -  1      1 a'        -11.23478130       -305.7169       1        2.000000
   2      2 -  2      2 a'        -11.23422058       -305.7017       1        2.000000
...
   ======================================================================================

   */
  private Lst<String> symmetries;
  private Lst<Float> occupancies;
   
   private void readOrbitalSymmetryAndOccupancy() throws Exception {
     readLines(4);
     symmetries = new  Lst<String>();
     occupancies = new  Lst<Float>();
     while (rd() != null && line.indexOf("====") < 0) {
       String[] tokens = PT.getTokens(line.substring(20));
       symmetries.addLast(tokens[0] + " " + tokens[1]);
       occupancies.addLast(Float.valueOf(parseFloatStr(tokens[5])));
     }
   }

   private void setOrbitalSymmetryAndOccupancy() {
     // why do some of the orbitals not get shown in the coefficient list?
     if (symmetries.size() < orbitals.size())
       return;
     for (int i = orbitals.size(); --i >= 0; ) {
       Map<String, Object> mo = orbitals.get(i);
       mo.put("symmetry", symmetries.get(i));
       mo.put("occupancy", occupancies.get(i));
     }
   }

  /*


                                                  ------------
                                                  eigenvectors
                                                  ------------


                  -11.2348 -11.2342 -11.2342 -11.2330 -11.2330 -11.2324  -1.1572  -1.0176  -1.0174  -0.8237


                      1        2        3        4        5        6        7        8        9       10


    1  1  c s       0.4086  -0.4980   0.2906  -0.4982   0.2846   0.4047   0.0929   0.0647  -0.1121  -0.0874
    2  1  c s       0.0095  -0.0123   0.0072  -0.0131   0.0075   0.0115  -0.1825  -0.1300   0.2251   0.1786
    3  1  c x       0.0000   0.0001  -0.0002   0.0002  -0.0001  -0.0002   0.0550  -0.0359  -0.0541   0.0983
    4  1  c y       0.0000  -0.0002  -0.0002  -0.0001   0.0001   0.0001  -0.0318  -0.0956  -0.0359   0.0649
    5  1  c z       0.0000   0.0000   0.0000   0.0000   0.0000   0.0000   0.0000   0.0000   0.0000   0.0000
    6  1  c s      -0.0029   0.0057  -0.0035   0.0121  -0.0069  -0.0225  -0.1075  -0.0967   0.1676   0.1557
    7  1  c x      -0.0004   0.0000   0.0011  -0.0030   0.0006   0.0081   0.0068  -0.0054   0.0003   0.0464
    8  1  c y       0.0002   0.0010   0.0012   0.0006  -0.0023  -0.0047  -0.0039  -0.0060  -0.0054  -0.0008
    9  1  c z       0.0000   0.0000   0.0000   0.0000   0.0000   0.0000   0.0000   0.0000   0.0000   0.0000
...

                   -0.8237  -0.7135  -0.6381  -0.6197  -0.5880  -0.5878  -0.5024  -0.4877  -0.4877  -0.3346


                     11       12       13       14       15       16       17       18       19       20


    1  1  c s      -0.0504   0.0094  -0.0564   0.0000  -0.0097  -0.0168   0.0000  -0.0019  -0.0033   0.0000
    2  1  c s       0.1031  -0.0189   0.1224   0.0000   0.0232   0.0403   0.0000   0.0048   0.0083   0.0000
...
 end of closed shell scf at       1.14 seconds

 --------------------------------------------------------------------------------------------------------



 ================================================================================

   */


}
