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

import javajs.util.Lst;

import org.jmol.adapter.smarter.Atom;
import org.jmol.util.Logger;

/**
 * A reader for Amber Molecular Dynamics topology files --
 * requires subsequent COORD "xxxx.mdcrd" file 
 * 
 *<p>
 * <a href=''>
 *  
 * </a>
 * 
 * PDB note:
 * 
 * Note that topology format does not include chain designations,
 * chain terminator, chain designator, or element symbol.
 * 
 * Chains based on numbering reset just labeled A B C D .... Z a b c d .... z
 * Element symbols based on reasoned guess and properties of hetero groups
 * 
 * In principal we could use average atomic mass.
 * 
 * 
 *<p>
 */

public class MdTopReader extends ForceFieldReader {

  private int nAtoms = 0;
  private int ac = 0;

  @Override
  protected void initializeReader() throws Exception {
    setIsPDB();
    setUserAtomTypes();
  }

  @Override
  protected boolean checkLine() throws Exception {
    if (line.indexOf("%FLAG ") != 0)
      return true;
    line = line.substring(6).trim();
    if (line.equals("POINTERS"))
      getPointers();
    else if (line.equals("ATOM_NAME"))
      getAtomNames();
    else if (line.equals("CHARGE"))
      getCharges();
    else if (line.equals("RESIDUE_LABEL"))
      getResidueLabels();
    else if (line.equals("RESIDUE_POINTER"))
      getResiduePointers();
    else if (line.equals("AMBER_ATOM_TYPE"))
      getAtomTypes();
    else if (line.equals("MASS"))
      getMasses();
    return false;
  }
  
  @Override
  protected void finalizeSubclassReader() throws Exception {
    finalizeReaderASCR();
    Atom[] atoms = asc.atoms;
    Atom atom;
    for (int i = 0; i < ac; i++) {
      atom = atoms[i];
      atom.isHetero = vwr.getJBR().isHetero(atom.group3);
      String atomType = atomTypes[i];
      if (!getElementSymbol(atom, atomType))
        atom.elementSymbol = deducePdbElementSymbol(atom.isHetero,
            atom.atomName, atom.group3);
    }
    Atom[] atoms2 = null;
    if (filter == null) {
      nAtoms = ac;
    } else {
      atoms2 = new Atom[atoms.length];
      nAtoms = 0;
      for (int i = 0; i < ac; i++)
        if (filterAtom(atoms[i], i))
          atoms2[nAtoms++] = atoms[i];
    }
    for (int i = 0, j = 0, k = 0; i < ac; i++) {
      if (filter == null || bsFilter.get(i)) {
        if (k % 100 == 0)
          j++;
        setAtomCoordXYZ(atoms[i], (i % 100) * 2, j * 2, 0);
      }
    }
    if (atoms2 != null) {
      discardPreviousAtoms();
      for (int i = 0; i < nAtoms; i++)
        asc.addAtom(atoms2[i]);
    }
    Logger.info("Total number of atoms used=" + nAtoms);
    setModelPDB(true);
    htParams.put("defaultType", "mdcrd");
  }

  private String[] getDataBlock() throws Exception {
    Lst<String> vdata = new  Lst<String>();
    // for these purposes, we just read the first length
    discardLinesUntilContains("FORMAT");
    int n = getFortranFormatLengths(line.substring(line.indexOf("("))).get(0).intValue();
    int i = 0;
    int len = 0;
    while (true) {
      if (i >= len) {
        if (rd() == null)
          break;
        i = 0;
        len = line.length();
        if (len == 0 || line.indexOf("FLAG") >= 0)
          break;
      }
      vdata.addLast(line.substring(i, i + n).trim());
      i += n;
    }
    return vdata.toArray(new String[vdata.size()]);
  }

  /*
  FORMAT(12i6)  NATOM,  NTYPES, NBONH,  MBONA,  NTHETH, MTHETA,
                NPHIH,  MPHIA,  NHPARM, NPARM,  NEXT,   NRES,
                NBONA,  NTHETA, NPHIA,  NUMBND, NUMANG, NPTRA,
                NATYP,  NPHB,   IFPERT, NBPER,  NGPER,  NDPER,
                MBPER,  MGPER,  MDPER,  IFBOX,  NMXRS,  IFCAP
    NATOM  : total number of atoms 
    NTYPES : total number of distinct atom types
    NBONH  : number of bonds containing hydrogen
    MBONA  : number of bonds not containing hydrogen
    NTHETH : number of angles containing hydrogen
    MTHETA : number of angles not containing hydrogen
    NPHIH  : number of dihedrals containing hydrogen
    MPHIA  : number of dihedrals not containing hydrogen
    NHPARM : currently not used
    NPARM  : currently not used
    NEXT   : number of excluded atoms
    NRES   : number of residues
    NBONA  : MBONA + number of constraint bonds
    NTHETA : MTHETA + number of constraint angles
    NPHIA  : MPHIA + number of constraint dihedrals
    NUMBND : number of unique bond types
    NUMANG : number of unique angle types
    NPTRA  : number of unique dihedral types
    NATYP  : number of atom types in parameter file, see SOLTY below
    NPHB   : number of distinct 10-12 hydrogen bond pair types
    IFPERT : set to 1 if perturbation info is to be read in
    NBPER  : number of bonds to be perturbed
    NGPER  : number of angles to be perturbed
    NDPER  : number of dihedrals to be perturbed
    MBPER  : number of bonds with atoms completely in perturbed group
    MGPER  : number of angles with atoms completely in perturbed group
    MDPER  : number of dihedrals with atoms completely in perturbed groups
    IFBOX  : set to 1 if standard periodic box, 2 when truncated octahedral
    NMXRS  : number of atoms in the largest residue
    IFCAP  : set to 1 if the CAP option from edit was specified

  %FLAG POINTERS                                                                  
  %FORMAT(10I8)                                                                   
     37300      16   29669    6234   12927    6917   28267    6499       0       0
     87674    9013    6234    6917    6499      47     101      41      31       1
         0       0       0       0       0       0       0       1      24       0
         0

  0         1         2         3         4         5         6         7         
  01234567890123456789012345678901234567890123456789012345678901234567890123456789

     */
    private void getPointers() throws Exception {
      String[] tokens = getDataBlock();
      ac = parseIntStr(tokens[0]);
      boolean isPeriodic = (tokens[27].charAt(0) != '0');
      if (isPeriodic) {
        Logger.info("Periodic type: " + tokens[27]);
        htParams.put("isPeriodic", Boolean.TRUE);
      }
      Logger.info("Total number of atoms read=" + ac);
      htParams.put("templateAtomCount", Integer.valueOf(ac));
      for (int i = 0; i < ac; i++) 
        asc.addAtom(new Atom());
    }

  private String[] atomTypes;
  private void getAtomTypes() throws Exception {
    atomTypes = getDataBlock();
  }

  /*
%FLAG CHARGE                                                                    
%FORMAT(5E16.8)                                                                 
  5.66713530E-01  4.24397367E+00  4.24397367E+00  4.24397367E+00  4.68313110E-01
  1.87871913E+00  3.43490355E+00  3.88134990E-01 -6.77869560E+00  1.72565181E+00
  1.72565181E+00  1.72565181E+00 -7.05203010E-01  3.66268230E-01  3.66268230E-01

   */
  private void getCharges() throws Exception {
    String[] data = getDataBlock();
    if (data.length != ac)
      return;
    Atom[] atoms = asc.atoms;
    for (int i = ac; --i >= 0;)
      atoms[i].partialCharge = parseFloatStr(data[i]);
  }

  private void getResiduePointers() throws Exception {
    String[] resPtrs = getDataBlock();
    Logger.info("Total number of residues=" + resPtrs.length);
    int pt1 = ac;
    int pt2;
    Atom[] atoms = asc.atoms;
    for (int i = resPtrs.length; --i >= 0;) {
      int ptr = pt2 = parseIntStr(resPtrs[i]) - 1;
      while (ptr < pt1) {
        if (group3s != null)
          atoms[ptr].group3 = group3s[i];
        atoms[ptr++].sequenceNumber = i + 1;
      }
      pt1 = pt2;
    }
  }

  String[] group3s;
  
  private void getResidueLabels() throws Exception {
    group3s = getDataBlock();
  }

  /*
  %FLAG ATOM_NAME                                                                 
  %FORMAT(20a4)                                                                   
  N   H1  H2  H3  CA  HA  CB  HB  CG2 HG21HG22HG23CG1 HG12HG13CD1 HD11HD12HD13C   
  O   N   H   CA  HA  CB  HB2 HB3 SG  HG  C   O   N   H   CA  HA  CB  HB  CG1 HG11
  HG12HG13CG2 HG21HG22HG23C   O   N   H   CA  HA  CB  HB2 HB3 CG  CD1 HD1 CE1 HE1 
  CZ  OH  HH  CE2 HE2 CD2 HD2 C   O   N   H   CA  HA  CB  HB  CG1 HG11HG12HG13CG2 
  */
 
  private void getAtomNames() throws Exception {
    String[] names = getDataBlock();
    Atom[] atoms = asc.atoms;
    for (int i = 0; i < ac; i++)
      atoms[i].atomName = names[i];
  }

  private void getMasses() throws Exception {
    /*    float[] data = new float[ac];
        readLine();
        getTokensFloat(getDataBlock(), data, ac);
    */
  }


}
