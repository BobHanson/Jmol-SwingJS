/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-30 10:16:53 -0500 (Sat, 30 Sep 2006) $
 * $Revision: 5778 $
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
package org.jmol.adapter.readers.xtal;

import javajs.util.PT;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Atom;

/**
 * A reader for Wein2k DFT files.  
 * 
 * http://www.wien2k.at/reg_user/textbooks/usersguide.pdf
 * 
 * Bob Hanson hansonr@stolaf.edu 5/14/2009
 *   
 */

public class Wien2kReader extends AtomSetCollectionReader {

  private boolean isrhombohedral;
  private char latticeCode = '\0';
  private boolean doSymmetry = true;
  
  @Override
  public void initializeReader() throws Exception {
    doSymmetry = !sgName.equals("none");
    setFractionalCoordinates(true);
    asc.setCollectionName(rd());
    readUnitCell();
    readAtoms();
    readSymmetry();
    readEmbeddedScript();
    continuing = false;
  }

  /* from HallInfo:
   * 
    final static String[] latticeTranslationData = {
    "\0", "unknown",         ""
    ,"P", "primitive",       ""
    ,"I", "body-centered",   " 1n"
    ,"R", "rhombohedral",    " 1r 1r"
    ,"F", "face-centered",   " 1ab 1bc 1ac"
    ,"A", "A-centered",      " 1bc"
    ,"B", "B-centered",      " 1ac"
    ,"C", "C-centered",      " 1ab"
    ,"S", "rhombohedral(S)", " 1s 1s"
    ,"T", "rhombohedral(T)", " 1t 1t"
  };
  
  from Wien2k user manual:
  
  P all primitive lattices except hexagonal (trigonal lattice is also supported)
    [a sin(gamma), a cos(gamma), 0], [0, b, 0], [0, 0, c]
  F face-centered 
    [a/2, b/2, 0], [a/2, 0, c/2], [0, b/2, c/2]
  B body-centered 
    [a/2, -b/2, c/2],[a/2, b/2, -c/2], [-a/2, b/2, c/2]
  CXY C-base-centered (orthorhombic only) 
    [a/2, -b/2, 0], [a/2, b/2, 0], [0, 0, c]
  CYZ A-base-centered (orthorhombic only) 
    [a, 0, 0], [0, -b/2, c/2], [0, b/2, c/2]
  CXZ B-base-centered (orthorh. and monoclinic symmetry)
    [a sin(gamma)/2, a cos(gamma)/2, -c/2], [0, b, 0], [a sin(gamma)/2, a cos(gamma)/2, c/2]
  R rhombohedral [a/sqrt(3)/2, -a/2, c/3],[a/sqrt(3)/2, a/2, c/3],[-a/sqrt(3), 0, c/3]
  H hexagonal [sqrt(3)a/2, -a/2, 0],[0, a, 0],[0, 0, c]


   * 
   */
  private void readUnitCell() throws Exception {    
    rd();
    isrhombohedral = ((latticeCode = line.charAt(0)) == 'R');
    //CXY --> "C" SHELX  // x+1/2, y+1/2, z
    if (line.startsWith("CYZ"))
      latticeCode = 'A'; // x,y+1/2,z+1/2
    else if (line.startsWith("CXZ"))
      latticeCode = 'B'; // provided gamma is 90 -- x+1/2,y,z+1/2
    else if (line.startsWith("B"))
      latticeCode = 'I'; // x+1/2,y+1/2,z+1/2
    if (latticeCode != 'R' && latticeCode != 'H')
      asc.getXSymmetry().setLatticeParameter(latticeCode);
    if (line.length() > 32) {
      String name = line.substring(32).trim();
      if (name.indexOf(" ") >= 0)
        name = name.substring(name.indexOf(" ") + 1);
      if (name.indexOf("_") >= 0)
        name = name.substring(name.indexOf("_") + 1);
      setSpaceGroupName(name);
    }
    float factor = (rd().toLowerCase().indexOf("ang") >= 0 ? 1f : ANGSTROMS_PER_BOHR);
    rd();
    float a = parseFloatRange(line, 0,10) * factor;
    float b = parseFloatRange(line, 10,20) * factor;
    float c = parseFloatRange(line, 20,30) * factor;
    int l = line.length();
    float alpha = (l >= 40 ? parseFloatRange(line, 30,40) : 0);
    float beta = (l >= 50 ? parseFloatRange(line, 40,50) : 0);
    float gamma = (l >= 60 ? parseFloatRange(line, 50,60) : 0);
    if (isrhombohedral) {
      float ar = (float) Math.sqrt(a * a /3 + c * c / 9) ;
      alpha = beta = gamma = (float) (Math.acos( (2*c * c  - 3 * a * a) 
          / (2 * c * c + 6 * a * a)) * 180f / Math.PI);
      a = b = c = ar;
    }
    if (Float.isNaN(alpha) || alpha == 0)
      alpha = 90;
    if (Float.isNaN(beta) || beta == 0)
      beta = 90;
    if (Float.isNaN(gamma) || gamma == 0)
      gamma = 90; 
    setUnitCell(a, b, c, alpha, beta, gamma);  
  }
 
  private void readAtoms() throws Exception {

    // format (4X,I4,4X,F10.8,3X,F10.8,3X,F10.8)
    
    rd();
    while (line != null && (line.indexOf("ATOM") == 0 || !doSymmetry && line.indexOf(":") == 8)) {
      int thisAtom = asc.ac;
      addAtom();
      if (rd().indexOf("MULT=") == 10)
        for (int i = parseIntRange(line, 15,18); --i >= 0; ) { 
          rd();
          if (!doSymmetry)
            addAtom();
        }
      // format (A10,5X,I5,5X,F10.8,5X,F10.5,5X,F5.2)
      
      String atomName = line.substring(0, 10);
      String sym = atomName.substring(0,2).trim();
      if (sym.length() == 2 && PT.isDigit(sym.charAt(1)))
        sym = sym.substring(0, 1);
      atomName = PT.rep(atomName, " ", "");
      int n = 0;
      for (int i = asc.ac; --i >= thisAtom; ) {
        Atom atom = asc.atoms[i];
        atom.elementSymbol = sym;
        atom.atomName = atomName + "_" + (n++);
      }
      while (rd() != null && line.indexOf("ATOM") < 0 && line.indexOf("SYMMETRY") < 0) {
      }      
    }
    // return with "SYMMETRY" line in buffer
  }

  private void addAtom() {
    float a = parseFloatRange(line, 12,22);
    float b = parseFloatRange(line, 25,35);
    float c = parseFloatRange(line, 38,48);
/*    if (false && isrhombohedral) {
      float ar = a;
      float br = b;
      float cr = c;
      a = ar * 2 / 3 - br * 1 / 3 - cr * 1 / 3;
      b = ar * 1 / 3 + br * 1 / 3 - cr * 2 / 3;
      c = ar * 1 / 3 + br * 1 / 3 + cr * 1 / 3;        
    }
*/    Atom atom = asc.addNewAtom();
    setAtomCoordXYZ(atom, a, b, c);
  }
  
  private void readSymmetry() throws Exception {
    if (line.indexOf("SYMMETRY") < 0)
      return;
    int n = parseIntRange(line, 0, 4);
    for (int i = n; --i >= 0;) {
      String xyz = getJones() + "," + getJones() + "," + getJones();
      if (doSymmetry)
        setSymmetryOperator(xyz);
      rd();
    }   
  }
  
  private final String cxyz = " x y z";
  private String getJones() throws Exception {
    rd();
    String xyz = "";
    // 1 0 0 0.0000000
    float trans = parseFloatStr(line.substring(6));
    for (int i = 0; i < 6; i++) {
      if (line.charAt(i) == '-')
        xyz += "-";
      if (line.charAt(++i) == '1') {
        xyz += cxyz.charAt(i);
        if (trans > 0)
          xyz += "+";
        if (trans != 0)
          xyz += trans;
      }
    }
    return xyz;
  }
  
  private void readEmbeddedScript() throws Exception {
    while (line != null) {
      checkCurrentLineForScript();
      rd();
    }
  }
}
