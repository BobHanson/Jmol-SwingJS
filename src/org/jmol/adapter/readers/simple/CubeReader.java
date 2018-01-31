/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-26 01:48:23 -0500 (Tue, 26 Sep 2006) $
 * $Revision: 5729 $
 *
 * Copyright (C) 2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.adapter.readers.simple;

import javajs.util.PT;

import org.jmol.adapter.smarter.AtomSetCollectionReader;

/**
 * Gaussian cube file format
 * 
 * http://www.cup.uni-muenchen.de/oc/zipse/lv18099/orb_MOLDEN.html
 * this is good because it is source code
 * http://ftp.ccl.net/cca/software/SOURCES/C/scarecrow/gcube2plt.c
 *
 * http://www.nersc.gov/nusers/resources/software/apps/chemistry/gaussian/g98/00000430.htm
 *
 * distances are in Bohrs because we are reading Gaussian cube OUTPUT files
 * not Gaussian cube INPUT files. 
 *
 * Miguel 2005 07 17
 * a negative atom count means
 * that it is molecular orbital (MO) data
 * with MO data, the extra line contains the number
 * of orbitals and the orbital number
 * 
 * these orbitals are interspersed -- all orbital values are
 * given together for each coordinate point.
 * 
 * also used for older JVXL and JVXL+ file format
 * 
 */

public class CubeReader extends AtomSetCollectionReader {
    
  private int ac;
  private boolean isAngstroms = false;
  
  @Override
  public void initializeReader() throws Exception {
    asc.newAtomSet();
    readTitleLines();
    readAtomCountAndOrigin();
    readLines(3);
    readAtoms();
    applySymmetryAndSetTrajectory();
    continuing = false;
  }

  private void readTitleLines() throws Exception {
    if (rd().indexOf("#JVXL") == 0)
      while (rd().indexOf("#") == 0) {
      }
    checkCurrentLineForScript();
    String name = line.trim();
    rd();
    checkCurrentLineForScript();
    asc.setAtomSetName(name + " - " + line.trim());
  }

  private void readAtomCountAndOrigin() throws Exception {
    rd();
    isAngstroms = (line.indexOf("ANGSTROMS") >= 0); //JVXL flag for Angstroms
    String[] tokens = getTokens();
    if (tokens[0].charAt(0) == '+') //Jvxl progressive reader -- ignore and consider negative
      tokens[0] = tokens[0].substring(1);
    ac = Math.abs(parseIntStr(tokens[0]));
  }
  
  private void readAtoms() throws Exception {
    float f = (isAngstroms ? 1 : ANGSTROMS_PER_BOHR);
    for (int i = 0; i < ac; ++i) {
      String[] tokens = PT.getTokens(rd());
      //allowing atomicAndIsotope for JVXL format
      setAtomCoordScaled(null, tokens, 2, f).elementNumber = (short)parseIntStr(tokens[0]);
    }
  }

}
