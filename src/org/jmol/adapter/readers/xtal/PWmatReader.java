package org.jmol.adapter.readers.xtal;

import javajs.util.PT;

import org.jmol.adapter.smarter.AtomSetCollectionReader;

/**
 * 
 * crude PCmat atom.config reader
 * 
 * @author hansonr
 */

public class PWmatReader extends AtomSetCollectionReader {

  private int nAtoms;

  @Override
  protected void initializeReader() throws Exception {
    doApplySymmetry = true;
  }

  @Override
  protected boolean checkLine() throws Exception {
    if (nAtoms == 0) {
      setSpaceGroupName("P1");
      nAtoms = PT.parseInt(line);
      setFractionalCoordinates(true);
      return true;
    }
    if (line.equalsIgnoreCase("lattice vector")) {
      readUnitCell();
    } else if (line.toLowerCase().startsWith("position")) {
      readCoordinates();
    } else {
      continuing = false;
    }
    return true;
  }
  
  private void readUnitCell() throws Exception {
    float[] unitCellData = new float[9];
    fillFloatArray(null, 0, unitCellData);
    addExplicitLatticeVector(0, unitCellData, 0);
    addExplicitLatticeVector(1, unitCellData, 3);
    addExplicitLatticeVector(2, unitCellData, 6);
  }

  private void readCoordinates() throws Exception {
    int i = 0;
    while (rd() != null && i++ < nAtoms) {
      String[] tokens = getTokens();
      addAtomXYZSymName(tokens, 1, null, getElementSymbol(Integer.parseInt(tokens[0])));
    }
  }

}
