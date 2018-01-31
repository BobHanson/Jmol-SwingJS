package org.jmol.adapter.readers.xtal;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Atom;


/**
 * SIESTA
 * http://www.icmab.es/siesta/
 * @author Pieremanuele Canepa, Room 104, FM Group School of Physical Sciences,
 *         Ingram Building, University of Kent, Canterbury, Kent, CT2 7NH United
 *         Kingdom, pc229@kent.ac.uk
 * 
 * @version 1.0
 */

public class SiestaReader extends AtomSetCollectionReader {

  private int noAtoms;

  @Override
  protected void initializeReader() {
    doApplySymmetry = true;
  }

  @Override
  protected boolean checkLine() throws Exception {

    if (line.contains("%block LatticeVectors")) {
      if (doGetModel(++modelNumber, null))
        readCellThenAtomsCartesian();
      return true;
    } else if (line.contains("outcoor: Atomic coordinates")) {
      if (doGetModel(++modelNumber, null))
        readAtomsCartGeomThenCell();
      return true;
    }
    return true;
  }

  private float[] unitCellData = new float[9];

  private void setCell() throws Exception {
    fillFloatArray(null, 0, unitCellData);
    addExplicitLatticeVector(0, unitCellData, 0);
    addExplicitLatticeVector(1, unitCellData, 3);
    addExplicitLatticeVector(2, unitCellData, 6);
  }

  /*  
    AtomicCoordinatesFormat Ang
    %block AtomicCoordinatesAndAtomicSpecies
         6.15000000     7.17000000    15.47800000    2    C    1
         5.92900000     8.49200000    15.02300000    2    C    2
         5.89900000     9.54900000    15.94800000    2    C    3
         6.08300000     9.29200000    17.31300000    2    C    4
         6.34400000     7.98900000    17.76600100    2    C    5
         6.38200000     6.93200000    16.84400000    2    C    6
         5.72400000     8.78800000    13.70100000    3    O    7
         6.10300000     6.10500000    14.59100000    3    O    8
         5.98800000     8.04300000    13.09100000    1    H    9
   */
  private void readCellThenAtomsCartesian() throws Exception {
    // set cell FIRST, then atoms
    newAtomSet();
    setCell();
    discardLinesUntilContains("AtomicCoordinatesFormat Ang");
    rd();
    setFractionalCoordinates(false);
    while (rd() != null
        && line.indexOf("%endblock Atomic") < 0) {
      String[] tokens = getTokens();
      addAtomXYZSymName(tokens, 0, null, tokens[4]);
    }
    noAtoms = asc.ac;
  }

  private void newAtomSet() throws Exception {
    applySymmetryAndSetTrajectory();
    asc.newAtomSet();
    setSpaceGroupName("P1");
    setFractionalCoordinates(false);
  }

  private void readAtomsCartGeomThenCell() throws Exception {
    readLines(1);
    newAtomSet();
    int atom0 = asc.ac;
    for (int i = 0; i < noAtoms; i++) {
      String[] tokens = getTokens();
      Atom atom = asc.addNewAtom();
      atom.atomName = tokens[4];
      float x = parseFloatStr(tokens[0]);
      float y = parseFloatStr(tokens[1]);
      float z = parseFloatStr(tokens[2]);
      atom.set(x, y, z); // will be set after reading unit cell
      rd();
    }
    discardLinesUntilContains("outcell: Unit cell vectors");
    setCell();
    Atom[] atoms = asc.atoms;
    int ac = asc.ac;
    for (int i = atom0; i < ac; i++)
      setAtomCoord(atoms[i]);
    discardLinesUntilContains("siesta: E_KS(eV) = ");
    String[] tokens = getTokens();
    Double energy = Double.valueOf(Double.parseDouble(tokens[3]));
    asc.setAtomSetEnergy("" + energy, energy.floatValue());
    asc.setCurrentModelInfo("Energy", energy);
    asc.setInfo("Energy", energy);
    asc.setAtomSetName("Energy = " + energy + " eV");
  }
}
