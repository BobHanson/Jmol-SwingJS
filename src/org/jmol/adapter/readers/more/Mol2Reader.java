/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-03-15 07:52:29 -0600 (Wed, 15 Mar 2006) $
 * $Revision: 4614 $
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

package org.jmol.adapter.readers.more;

import javajs.util.PT;

import org.jmol.adapter.smarter.Bond;
import org.jmol.adapter.smarter.Atom;

import org.jmol.api.JmolAdapter;

/**
 * A minimal multi-file reader for TRIPOS SYBYL mol2 files.
 *<p>
 * <a href='http://www.tripos.com/data/support/mol2.pdf '>
 * http://www.tripos.com/data/support/mol2.pdf 
 * </a>
 * 
 * see also http://www.tripos.com/mol2/atom_types.html
 * 
 * PDB note:
 * 
 * Note that mol2 format of PDB files is quite minimal. All we
 * get is the PDB atom name, coordinates, residue number, and residue name
 * No chain terminator, not chain designator, no element symbol.
 * 
 * Chains based on numbering reset just labeled A B C D .... Z a b c d .... z
 * 
 *<p>
 */

public class Mol2Reader extends ForceFieldReader {

  private int nAtoms = 0;
  private int ac = 0;
  private boolean isPDB = false;

  @Override
  protected void initializeReader() throws Exception {
    setUserAtomTypes();
  }

  @Override
  public boolean checkLine() throws Exception {
    if (line.equals("@<TRIPOS>MOLECULE")) {
      if (!processMolecule()) {
        return true;
      }
      continuing = !isLastModel(modelNumber);
      return false;
    }
    if (line.length() != 0 && line.charAt(0) == '#') {
      /*
       * Comment lines (starting with '#' as per Tripos spec) may contain an
       * inline Jmol script.
       */
      checkCurrentLineForScript();
    }
    return true;
  }

  private boolean processMolecule() throws Exception {
    /* 4-6 lines:
     ZINC02211856
     55    58     0     0     0
     SMALL
     USER_CHARGES
     2-diethylamino-1-[2-(2-naphthyl)-4-quinolyl]-ethanol

     mol_name
     num_atoms [num_bonds [num_subst [num_feat [num_sets]]]]
     mol_type
     charge_type
     [status_bits
     [mol_comment]]

     */

    isPDB = false;
    String thisDataSetName = rd().trim();
    if (!doGetModel(++modelNumber, thisDataSetName)) {
      return false;
    }

    lastSequenceNumber = Integer.MAX_VALUE;
    chainID = 64; // 'A' - 1;
    rd();
    line += " 0 0 0 0 0 0";
    ac = parseIntStr(line);
    int bondCount = parseInt();
    if (bondCount == 0)
      asc.setNoAutoBond();
    int resCount = parseInt();
    rd();//mol_type
    rd();//charge_type
    //boolean iHaveCharges = (line.indexOf("NO_CHARGES") != 0);
    //optional SYBYL status
    if (rd() != null && (line.length() == 0 || line.charAt(0) != '@')) {
      //optional comment -- but present if comment is present
      if (rd() != null && line.length() != 0 && line.charAt(0) != '@') {
        /* The MOLECULE's comment line may contain an inline Jmol script.
            (But don't expect it to be applied just to this molecule/model/frame.)
            Note: '#' is not needed here, but it is for general comments (out of the MOLECULE data structure), 
            so for consistency we'll allow both 'jmolscript:' as such or preceded by # (spaces are ignored).
            Any comments written before the 'jmolscript:' will be preserved (and added to the model's title).
        */
        if (line.indexOf("jmolscript:") >= 0) {
          checkCurrentLineForScript();
          if (line.equals("#")) {
            line = "";
          }
        }
        if (line.length() != 0) {
          thisDataSetName += ": " + line.trim();
        }
      }
    }
    newAtomSet(thisDataSetName);
    while (line != null && !line.equals("@<TRIPOS>MOLECULE")) {
      if (line.equals("@<TRIPOS>ATOM")) {
        readAtoms(ac);
        asc.setAtomSetName(thisDataSetName);
      } else if (line.equals("@<TRIPOS>BOND")) {
        readBonds(bondCount);
      } else if (line.equals("@<TRIPOS>SUBSTRUCTURE")) {
        readResInfo(resCount);
      } else if (line.equals("@<TRIPOS>CRYSIN")) {
        readCrystalInfo();
      }
      rd();
    }
    nAtoms += ac;
    if (isPDB) {
      setIsPDB();
      setModelPDB(true);
    }
    applySymmetryAndSetTrajectory();
    return true;
  }

  private int lastSequenceNumber = Integer.MAX_VALUE;
  private int chainID = 64; // 'A' - 1

  private void readAtoms(int ac) throws Exception {
    //     1 Cs       0.0000   4.1230   0.0000   Cs        1 RES1   0.0000
    //  1 C1          7.0053   11.3096   -1.5429 C.3       1 <0>        -0.1912
    // free format, but no blank lines
    if (ac == 0)
      return;
    int i0 = asc.ac;
    for (int i = 0; i < ac; ++i) {
      Atom atom = asc.addNewAtom();
      String[] tokens = PT.getTokens(rd());
      String atomType = tokens[5];
      String name = tokens[1];// + '\0' + atomType;
      int pt = atomType.indexOf(".");
      if (pt >= 0) {
        // accepts "." for "no atom type"
        atom.elementSymbol = atomType.substring(0, pt);
      } else {
        atom.atomName = name;
        atom.elementSymbol = atom.getElementSymbol();
      }
      atom.atomName = name + '\0' + atomType;
      atom.set(parseFloatStr(tokens[2]), parseFloatStr(tokens[3]),
          parseFloatStr(tokens[4]));
      // apparently "NO_CHARGES" is not strictly enforced
      //      if (iHaveCharges)
      if (tokens.length > 6) {
        atom.sequenceNumber = parseIntStr(tokens[6]);
        if (atom.sequenceNumber < lastSequenceNumber) {
          if (chainID == 90) //'Z'
            chainID = 96;//'a' - 1;
          chainID++;
        }
        lastSequenceNumber = atom.sequenceNumber;
        setChainID(atom, "" + (char) chainID);
      }
      if (tokens.length > 7)
        atom.group3 = tokens[7];
      if (tokens.length > 8) {
        atom.partialCharge = parseFloatStr(tokens[8]);
        if (atom.partialCharge == (int) atom.partialCharge)
          atom.formalCharge = (int) atom.partialCharge;
      }
    }

    // trying to guess if this is a PDB-type file

    Atom[] atoms = asc.atoms;

    // 1. Does the very first atom have a group name?

    String g3 = atoms[i0].group3;
    if (g3 == null)
      return;
    boolean isPDB = false;

    // 2. If so, is there more than one kind of group?

    if (!g3.equals("UNK") && !g3.startsWith("RES")) {

      for (int i = asc.ac; --i >= i0;)
        if (!g3.equals(atoms[asc.ac - 1].group3)) {
          isPDB = true;
          break;
        }

      // 3. If so, is there an identifiable group name? 

      if (isPDB) {
        isPDB = false;
        for (int i = asc.ac; --i >= i0;) {
          int pt = getPDBGroupLength(atoms[i].group3);
          if (pt == 0 || pt > 3)
            break;
          if (vwr.getJBR().isKnownPDBGroup(g3.substring(0, pt), Integer.MAX_VALUE)) {
            isPDB = this.isPDB = true;
            break;
          }
        }
      }

    }

    // remove group3 entry if not PDB; fix if it is like THR13
    for (int i = asc.ac; --i >= i0;) {
      if (isPDB) {
        g3 = atoms[i].group3;
        g3 = g3.substring(0, getPDBGroupLength(g3));
        atoms[i].isHetero = vwr.getJBR().isHetero(g3);
      } else {
        g3 = null;
      }
      atoms[i].group3 = g3;
    }
  }

  private int getPDBGroupLength(String g3) {
    int pt0 = g3.length();
    int pt = pt0;
    while (--pt > 0 && Character.isDigit(g3.charAt(pt))) {
      // continue
    }
    return ++pt;
  }

  private void readBonds(int bondCount) throws Exception {
    //     6     1    42    1
    // free format, but no blank lines
    for (int i = 0; i < bondCount; ++i) {
      String[] tokens = PT.getTokens(rd());
      int atomIndex1 = parseIntStr(tokens[1]);
      int atomIndex2 = parseIntStr(tokens[2]);
      int order = parseIntStr(tokens[3]);
      if (order == Integer.MIN_VALUE)
        order = (tokens[3].equals("ar") ? JmolAdapter.ORDER_AROMATIC
            : tokens[3].equals("am") ? 1 : JmolAdapter.ORDER_UNSPECIFIED);
      asc.addBond(new Bond(nAtoms + atomIndex1 - 1, nAtoms
          + atomIndex2 - 1, order));
    }
  }

  private void readResInfo(int resCount) throws Exception {
    // free format, but no blank lines
    for (int i = 0; i < resCount; ++i) {
      rd();
      //to be determined -- not implemented
    }
  }

  private void readCrystalInfo() throws Exception {
    //    4.1230    4.1230    4.1230   90.0000   90.0000   90.0000   221     1
    rd();
    String[] tokens = getTokens();
    if (tokens.length < 6)
      return;
    String name = "";
    for (int i = 6; i < tokens.length; i++)
      name += " " + tokens[i];
    if (name == "")
      name = " P1";
    else
      name += " *";
    name = name.substring(1);
    setSpaceGroupName(name);
    if (ignoreFileUnitCell)
      return;
    for (int i = 0; i < 6; i++)
      setUnitCellItem(i, parseFloatStr(tokens[i]));
    Atom[] atoms = asc.atoms;
    for (int i = 0; i < ac; ++i)
      setAtomCoord(atoms[nAtoms + i]);
  }
}
