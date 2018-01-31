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


import org.jmol.adapter.smarter.Atom;
import org.jmol.api.JmolAdapter;
import javajs.util.P3;

/**
 * 
 * Mopac Archive reader -- presumes "zMatrix" is really Cartesians
 * 
 * use FILTER "NOCENTER" to NOT center atoms in unit cell
 * use CENTROID for complete molecules with centroids within unit cell
 * use PACKED CENTROID for complete molecules with any atoms within unit cell 
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */

public class MopacArchiveReader extends InputReader {


  @Override
  protected void initializeReader() {
    asc.newAtomSet();
    if (!checkFilterKey("NOCENTER"))
      doCentralize = true;
  }
  
  /*
          HEAT OF FORMATION       =      -2179.37980 KCAL/MOL =   -9118.52509 KJ/MOL
          H.o.F. per unit cell    =        -68.10562 KCAL, for  32 unit cells
          TOTAL ENERGY            =      -7442.50807 EV
          ELECTRONIC ENERGY       =   -7684437.23375 EV
          CORE-CORE REPULSION     =    7676994.72567 EV
          NO. OF FILLED LEVELS    =        128
          IONIZATION POTENTIAL    =          9.958165 EV
          HOMO LUMO ENERGIES (EV) =         -9.958  0.439
          MOLECULAR WEIGHT        =       5312.090

   */
  @Override
  protected boolean checkLine() throws Exception {
    if (line.indexOf("=") == 34)
      return getMyValue();
    if (line.indexOf("FINAL GEOMETRY OBTAINED") >= 0)
      return readCoordinates();
    return true;
  }

  private String energyWithUnits;
  private boolean getMyValue() {
    if (line.substring(0, 10).trim().length() != 0)
      return true;
    String key = line.substring(0, 34).trim().replace(' ', '_');
    String value = line.substring(35).trim();
    asc.setCurrentModelInfo(key, value);
    if (line.indexOf("TOTAL ENERGY") >= 0) {
      String[] tokens = getTokens();
      energyWithUnits = " (" + tokens[3] + " " + tokens[4] + ")";
      asc.setAtomSetEnergy(tokens[3], parseFloatStr(tokens[3]));      
    }
    return true;
  }

  /*
          FINAL GEOMETRY OBTAINED                                    CHARGE
MERS=(1,2,2)   GNORM=4
 Lansfordite (MgCO3 5(H2O))

0         1         2         3         4         5         6         7         8
012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
 Mg     0.00407813 +1  -0.10980012 +1  -0.07460042 +1                         0.0000
   */
  
  private boolean readCoordinates() throws Exception {
    rd();
    line = rd().trim();
    asc.setAtomSetName(line + (energyWithUnits == null ? "" : energyWithUnits));
    rd();
    Atom atom = null;
    String sym = null;
    setFractionalCoordinates(false);
    while (rd() != null && line.length() >= 50) {
      vAtoms.addLast(atom = new Atom());
      atom.x = parseFloatRange(line, 5, 18);
      atom.y = parseFloatRange(line, 21, 34);
      atom.z = parseFloatRange(line, 37, 50);
      if (line.length() > 58 && line.charAt(58) != ' ') {
        // internal coordinates
        switch (ac) {
        case 0:
          break;
        case 1:
          atom.sub(vAtoms.get(0));
          break;
        case 2:
          setAtom(atom, 0, 1, 0, atom.x, atom.y, Float.MAX_VALUE);
          break;
        default:
          setAtom(atom, 
              parseIntRange(line, 54, 59) - 1, 
              parseIntRange(line, 60, 65) - 1, 
              parseIntRange(line, 66, 71) - 1, 
              atom.x, atom.y, atom.z);
        }
      }
      sym = line.substring(1,3).trim();
      atom.elementSymbol = sym;
      if (!sym.equals("Tv")) {
        ac++;
        if (line.length() >= 84)
          atom.partialCharge = parseFloatRange(line, 76, 84);
        if (JmolAdapter.getElementNumber(sym) != 0)
          asc.addAtom(atom);
        setAtomCoord(atom);
      }
    }
    if (sym.equals("Tv")) {
      // last element was "Tv" -- translation vector
      setSpaceGroupName("P1");
      int nTv = vAtoms.size() - ac;
      for (int i = nTv; i < 3; i++)
        vAtoms.addLast(new Atom()); 
      float[] xyz = new float[9];
      for (int i = 0; i < 3; i++) {
        int j = i * 3;
        atom = vAtoms.get(ac + i);
        if (!Float.isNaN(atom.x)) {
          xyz[j] = atom.x;
          xyz[j + 1] = atom.y;
          xyz[j + 2] = atom.z;
        }
        addExplicitLatticeVector(i, xyz, j);
      }
      for (int i = ac; --i >= 0;)
        setAtomCoord(vAtoms.get(i));
      P3 ptMax = P3.new3(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
      P3 ptMin = P3.new3(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
      if (doCentralize) {
        for (int i = ac; --i >= 0;) {
          atom = vAtoms.get(i);
          ptMax.x = Math.max(ptMax.x, atom.x);
          ptMax.y = Math.max(ptMax.y, atom.y);
          ptMax.z = Math.max(ptMax.z, atom.z);
          ptMin.x = Math.min(ptMin.x, atom.x);
          ptMin.y = Math.min(ptMin.y, atom.y);
          ptMin.z = Math.min(ptMin.z, atom.z);
        }
        P3 ptCenter = new P3();
        switch (nTv) {
        case 3:
          ptCenter.x = 0.5f;
          //$FALL-THROUGH$
        case 2:
          ptCenter.y = 0.5f;
          //$FALL-THROUGH$
        case 1:
          ptCenter.z = 0.5f;
        }
        ptCenter.scaleAdd2(-0.5f, ptMin, ptCenter);
        ptCenter.scaleAdd2(-0.5f, ptMax, ptCenter);
        for (int i = ac; --i >= 0;)
          vAtoms.get(i).add(ptCenter);
      }
      doCentralize = false;
    }
    return true;
  }
}
