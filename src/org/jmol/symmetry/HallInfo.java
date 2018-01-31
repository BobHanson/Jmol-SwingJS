/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
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


package org.jmol.symmetry;

/*
 * Bob Hanson 9/2006
 * 
 * references: International Tables for Crystallography Vol. A. (2002) 
 *
 * http://www.iucr.org/iucr-top/cif/cifdic_html/1/cif_core.dic/Ispace_group_symop_operation_xyz.html
 * http://www.iucr.org/iucr-top/cif/cifdic_html/1/cif_core.dic/Isymmetry_equiv_pos_as_xyz.html
 *
 * LATT : http://macxray.chem.upenn.edu/LATT.pdf thank you, Patrick Carroll
 * 
 * Hall symbols:
 * 
 * http://cci.lbl.gov/sginfo/hall_symbols.html
 * 
 * and
 * 
 * http://cci.lbl.gov/cctbx/explore_symmetry.html
 * 
 * (-)L   [N_A^T_1]   [N_A^T_2]   ...  [N_A^T_P]   V(Nx Ny Nz)
 * 
 * lattice types S and T are not supported here
 * 
 * NEVER ACCESS THESE METHODS OUTSIDE OF THIS PACKAGE
 * 
 *
 */


import org.jmol.util.Logger;

import javajs.util.SB;
import javajs.util.P3i;

class HallInfo {
  
  String hallSymbol;
  String primitiveHallSymbol;
  char latticeCode = '\0';
  String latticeExtension;
  boolean isCentrosymmetric;
  int nRotations;
  HallRotationTerm[] rotationTerms = new HallRotationTerm[16];
  P3i vector12ths;
  String vectorCode;
  
  HallInfo(String hallSymbol) {
    try {
      String str = this.hallSymbol = hallSymbol.trim();
      str = extractLatticeInfo(str);
      if (HallTranslation.getLatticeIndex(latticeCode) == 0)
        return;
      latticeExtension = HallTranslation.getLatticeExtension(latticeCode,
          isCentrosymmetric);
      str = extractVectorInfo(str) + latticeExtension;
      if (Logger.debugging)
        Logger.debug("Hallinfo: " + hallSymbol + " " + str);
      int prevOrder = 0;
      char prevAxisType = '\0';
      primitiveHallSymbol = "P";
      while (str.length() > 0 && nRotations < 16) {
        str = extractRotationInfo(str, prevOrder, prevAxisType);
        HallRotationTerm r = rotationTerms[nRotations - 1];
        prevOrder = r.order;
        prevAxisType = r.axisType;
        primitiveHallSymbol += " " + r.primitiveCode;
      }
      primitiveHallSymbol += vectorCode;
    } catch (Exception e) {
      Logger.error("Invalid Hall symbol "  + e);
      nRotations = 0;
    }
  }
  
  String dumpInfo() {
    SB sb =  new SB();
    sb.append("\nHall symbol: ").append(hallSymbol)
        .append("\nprimitive Hall symbol: ").append(primitiveHallSymbol)
        .append("\nlattice type: ").append(getLatticeDesignation());
    for (int i = 0; i < nRotations; i++) {
      sb.append("\n\nrotation term ").appendI(i + 1).append(rotationTerms[i].dumpInfo(vectorCode));
    }
    return sb.toString();
  }

/*  
  String getCanonicalSeitzList() {
    String[] list = new String[nRotations];
    for (int i = 0; i < nRotations; i++)
      list[i] = SymmetryOperation.dumpSeitz(rotationTerms[i].seitzMatrix12ths);
    Arrays.sort(list, 0, nRotations);
    String s = "";
    for (int i = 0; i < nRotations; i++)
      s += list[i];
    s = s.replace('\t',' ').replace('\n',';');
    return s;
  }
*/
  private String getLatticeDesignation() {    
    return HallTranslation.getLatticeDesignation2(latticeCode, isCentrosymmetric);
  }  
   
  private String extractLatticeInfo(String name) {
    int i = name.indexOf(" ");
    if (i < 0)
      return "";
    String term = name.substring(0, i).toUpperCase();
    latticeCode = term.charAt(0);
    if (latticeCode == '-') {
      isCentrosymmetric = true;
      latticeCode = term.charAt(1);
    }
    return name.substring(i + 1).trim();
  } 
  
  private String extractVectorInfo(String name) {
    // (nx ny nz)  where n is 1/12 of the edge. 
    // also allows for (nz), though that is not standard
    vector12ths = new P3i();
    vectorCode = "";
    int i = name.indexOf("(");
    int j = name.indexOf(")", i);
    if (i > 0 && j > i) {
      String term = name.substring(i + 1, j);
      vectorCode = " (" + term + ")";
      name = name.substring(0, i).trim();
      i = term.indexOf(" ");
      if (i >= 0) {
        vector12ths.x = Integer.parseInt(term.substring(0, i));
        term = term.substring(i + 1).trim();
        i = term.indexOf(" ");
        if (i >= 0) {
          vector12ths.y = Integer.parseInt(term.substring(0, i));
          term = term.substring(i + 1).trim();
        }
      }
      vector12ths.z = Integer.parseInt(term);
    }
    return name;
  }
  
  private String extractRotationInfo(String name, int prevOrder, char prevAxisType) {
    int i = name.indexOf(" ");
    String code;
    if (i >= 0) {
      code = name.substring(0, i);
      name = name.substring(i + 1).trim();
    } else {
      code = name;
      name = "";
    }
    rotationTerms[nRotations] = new HallRotationTerm(this, code, prevOrder, prevAxisType);
    nRotations++;
    return name;
  }  
}

