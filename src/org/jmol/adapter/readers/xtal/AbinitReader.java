package org.jmol.adapter.readers.xtal;

/**
 * http://www.abinit.org/
 * 
 * allows filter="input"
 * 
 * @author Pieremanuele Canepa, MIT, 
 *         Department of Material Sciences and Engineering
 * @author Bob Hanson, hansonr@stolaf.edu
 *         
 * 
 * @version 1.0
 */

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollectionReader;

public class AbinitReader extends AtomSetCollectionReader {

  private double[] znucl;
  private boolean inputOnly;

  @Override
  protected void initializeReader() {
    setSpaceGroupName("P1");
    doApplySymmetry = true;
    setFractionalCoordinates(false);
    inputOnly = checkFilterKey("INPUT");
  }

  @Override
  protected boolean checkLine() throws Exception {
    if (line.contains("natom")) {
      readNoatom();
    } else if (line.contains("ntypat") || line.contains("ntype")) {
      readNotypes();
    } else if (line.contains("typat") || line.contains("type")) {
      //read sequence of types
      readTypesequence();
    } else if (line.contains("Pseudopotential")) {
      readAtomSpecies();
    } else if (line.contains("Symmetries :")) {
      readSpaceGroup();
    } else if (line.contains("Real(R)+Recip(G)")) {
      readIntiallattice();
      if (inputOnly)
        continuing = false;
    } else if (line.contains("xcart")) {
      readAtoms();
    }
    return true;
  }

  private int nAtom;

  private void readNoatom() throws Exception {
    String[] tokens = getTokens();
    if (tokens.length <= 2)
      nAtom = parseIntStr(tokens[1]);
  }

  private int nType;

  private void readNotypes() throws Exception {
    String[] tokens = getTokens();
    if (tokens.length <= 2)
      nType = parseIntStr(tokens[1]);
  }

  private double[] typeArray;

  private void readTypesequence() throws Exception {
    fillDoubleArray(line.substring(12),  0, typeArray = new double[nAtom]);
  }

  private void readAtomSpecies() throws Exception {
    znucl = new double[nType];
    //- pspini: atom type   1  psp file is Al.psp
    for (int i = 0; i < nType; i++) { //is this ntype or sequence type ?
      discardLinesUntilContains("zion");
      String[] tokens = getTokens();
      znucl[i] = parseDoubleStr(tokens[tokens[0] == "-" ? 1 : 0]);
    }
  }

  // Symmetries : space group P4/m m m (#123); Bravais tP (primitive tetrag.)
  private void readSpaceGroup() throws Exception {
  }

  double[] cellLattice;

  private void readIntiallattice() throws Exception {
    //    Real(R)+Recip(G) space primitive vectors, cartesian coordinates (Bohr,Bohr^-1):
    //    R(1)= 25.9374361  0.0000000  0.0000000  G(1)=  0.0385543  0.0222593  0.0000000
    //    R(2)=-12.9687180 22.4624785  0.0000000  G(2)=  0.0000000  0.0445187  0.0000000
    //    R(3)=  0.0000000  0.0000000 16.0314917  G(3)=  0.0000000  0.0000000  0.0623772
    //    Unit cell volume ucvol=  9.3402532E+03 bohr^3

    double f = 0;
    cellLattice = new double[9];
    for (int i = 0; i < 9; i++) {
      if (i % 3 == 0) {
        line = rd().substring(6);
        f = parseDoubleStr(line);
      }
      cellLattice[i] = f * ANGSTROMS_PER_BOHR;
      f = parseDouble();      
    }
    applySymmetry();
  }

  private void applySymmetry() throws Exception {
    if (cellLattice == null)
      return;
    setSpaceGroupName("P1");
    for (int i = 0; i < 3; i++)
      addExplicitLatticeVector(i, cellLattice, i * 3);
    Atom[] atoms = asc.atoms;
    int i0 = asc.getAtomSetAtomIndex(asc.iSet);
    if (!iHaveFractionalCoordinates)
      for (int i = asc.ac; --i >= i0;)
        setAtomCoord(atoms[i]);
    applySymmetryAndSetTrajectory();
  }


  //   xcart    0.0000000000E+00  0.0000000000E+00  0.0000000000E+00
  //            2.5542500000E+00  2.5542500000E+00  2.5542500000E+00
  //    xred...
  //    ...
  //    z...
  
  private void readAtoms() throws Exception {
    // Read cartesian coordinates 
    asc.newAtomSet();
    iHaveFractionalCoordinates = false;
    int i0 = asc.ac;
    line = line.substring(12);
    while (line != null && !line.contains("x")) {
      Atom atom = asc.addNewAtom();
      setAtomCoordScaled(atom, getTokens(), 0, ANGSTROMS_PER_BOHR);
      rd();
    }
    discardLinesUntilContains("z");
    if (znucl == null)
      fillDoubleArray(line.substring(12), 0, znucl = new double[nType]);
    Atom[] atoms = asc.atoms;
    for (int i = 0; i < nAtom; i++)
      atoms[i + i0].elementNumber = (short) znucl[(int) typeArray[i] - 1];
    applySymmetry();    
  }

}
