/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-07-14 18:41:50 -0500 (Fri, 14 Jul 2006) $
 * $Revision: 5311 $
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

package org.jmol.adapter.readers.spartan;

import javajs.util.PT;

import org.jmol.adapter.readers.quantum.BasisFunctionReader;
import org.jmol.adapter.smarter.Bond;
import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolAdapter;

import org.jmol.util.Logger;

/*
 * Wavefunction INPUT section reader
 * 
 */

public abstract class SpartanInputReader extends BasisFunctionReader {

  protected int modelAtomCount;
  protected String bondData = "";
  protected String constraints = "";

  protected String readInputRecords() throws Exception {
    int ac0 = asc.ac;
    String modelName = readInputHeader();
    while (rd() != null) {
      String[] tokens = getTokens();
      //charge and spin
      if (tokens.length == 2 && parseIntStr(tokens[0]) != Integer.MIN_VALUE
          && parseIntStr(tokens[1]) >= 0)
        break;
    }
    if (line == null)
      return null;
    readInputAtoms();
    discardLinesUntilContains("ATOMLABELS");
    if (line != null)
      readAtomNames();
    if (modelAtomCount > 1) {
      discardLinesUntilContains("HESSIAN");
      if (line != null)
        readBonds(ac0);
      if (line != null && line.indexOf("BEGINCONSTRAINTS") >= 0)
        readConstraints();
    }
    return modelName;
  }

  private void readConstraints() throws Exception {
    constraints = "";
    while (rd() != null && line.indexOf("END") < 0)
      constraints += (constraints == "" ? "" : "\n") + line;
    rd();
    if (constraints.length() == 0)
      return;
    asc.setCurrentModelInfo("constraints", constraints);
    asc.setAtomSetModelProperty(SmarterJmolAdapter.PATH_KEY, "EnergyProfile");
    asc.setAtomSetModelProperty("Constraint", constraints);
  }

  void readTransform() throws Exception {
    rd();
    String[] tokens = PT.getTokens(rd() + " " + rd());
    //BEGINMOLSTATE
    //MODEL=3~HYDROGEN=1~LABELS=0
    //0.70925283  0.69996750 -0.08369886  0.00000000 -0.70480913  0.70649898 -0.06405880  0.00000000
    //0.01429412  0.10442561  0.99443018  0.00000000  0.00000000  0.00000000  0.00000000  1.00000000
    //ENDMOLSTATE
    setTransform(
        parseFloatStr(tokens[0]), parseFloatStr(tokens[1]), parseFloatStr(tokens[2]),
        parseFloatStr(tokens[4]), parseFloatStr(tokens[5]), parseFloatStr(tokens[6]),
        parseFloatStr(tokens[8]), parseFloatStr(tokens[9]), parseFloatStr(tokens[10])
    );
  }
  
  private String readInputHeader() throws Exception {
    while (rd() != null
        && !line.startsWith(" ")) {}
    rd();
    return line.substring(0, (line + ";").indexOf(";")).trim();
  }
  
  private void readInputAtoms() throws Exception {
    modelAtomCount = 0;
    while (rd() != null
        && !line.startsWith("ENDCART")) {
      String[] tokens = getTokens();
      addAtomXYZSymName(tokens, 1, getElementSymbol(parseIntStr(tokens[0])), null);
      modelAtomCount++;
    }
    if (debugging)
      Logger.debug(asc.ac + " atoms read");
  }

  private void readAtomNames() throws Exception {
    int atom0 = asc.ac - modelAtomCount;
    // note that asc.isTrajectory() gets set onlyAFTER an input is
    // read.
    for (int i = 0; i < modelAtomCount; i++) {
      line = rd().trim();
      String name = line.substring(1, line.length() - 1);
      asc.atoms[atom0 + i].atomName = name;
    }
  }
  
  private void readBonds(int ac0) throws Exception {
    int nAtoms = modelAtomCount;
    /*
     <one number per atom>
     1    2    1
     1    3    1
     1    4    1
     1    5    1
     1    6    1
     1    7    1
     */
    bondData = ""; //used for frequency business
    while (rd() != null && !line.startsWith("ENDHESS")) {
      String[] tokens = getTokens();
      bondData += line + " ";
      if (nAtoms == 0) {
        int sourceIndex = parseIntStr(tokens[0]) - 1 + ac0;
        int targetIndex = parseIntStr(tokens[1]) - 1 + ac0;
        int bondOrder = parseIntStr(tokens[2]);
        if (bondOrder > 0) {
          asc.addBond(new Bond(sourceIndex, targetIndex,
              bondOrder < 4 ? bondOrder : bondOrder == 5 ? JmolAdapter.ORDER_AROMATIC : 1));
        }
      } else {
        nAtoms -= tokens.length;
      }
    }
    rd();
    if (debugging)
      Logger.debug(asc.bondCount + " bonds read");
  }
}
