package org.jmol.adapter.readers.xtal;

import javajs.util.PT;

import org.jmol.adapter.smarter.AtomSetCollectionReader;

/**
 * 
 * 
 * @author Pieremanuele Canepa, Massachusetts Institute of Technology, 
 *         Room 13-5001 Department of Materials Science and Engineering
 *         Massachusetts Institute of Technology 
 *         77 Massachusetts Avenue
 *         Cambridge, MA 02139 
 * 
 *         http://www.xcrysden.org
 *         
 *         
 *         http://www.xcrysden.org/doc/XSF.html
 * 
 * @version 1.1
 */

public class XcrysdenReader extends AtomSetCollectionReader {

  private int nAtoms;
  private boolean animation = false;
  private float[] unitCellData = new float[9];
  private int animationStep;


  @Override
  protected void initializeReader() throws Exception {
    setFractionalCoordinates(false);
    doApplySymmetry = true;
  }

  @Override
  protected boolean checkLine() throws Exception {
    if (line.startsWith("ATOMS")) {
      doApplySymmetry = false;
      return readCoordinates();
    }
    if (line.contains("ANIMSTEP")) {
      animation = true;
    } else if (line.contains("PRIMVEC")) {
      readUnitCell();
    } else if (line.contains("PRIMCOORD")) {
      return readCoordinates();
    }
    return true;
  }
  
  private void readUnitCell() throws Exception {
    setSymmetry();
    fillFloatArray(null, 0, unitCellData);
    setUnitCell();
  }

  private void setUnitCell() {
    addExplicitLatticeVector(0, unitCellData, 0);
    addExplicitLatticeVector(1, unitCellData, 3);
    addExplicitLatticeVector(2, unitCellData, 6);
  }

  private void setSymmetry() throws Exception {
    applySymmetryAndSetTrajectory();
    asc.newAtomSet();
    setSpaceGroupName("P1");
    setFractionalCoordinates(false);
  }

  /*    
      
      1  -6.465123371  4.133947301 -4.246232206
      1  -0.105791449  0.706840646  8.791095596
      1   8.005285327  0.622155447 -3.685466049
      1   8.014605347  1.820410690  5.171706563
      1   1.652171017  5.246604959 -7.867307077
      1  -6.465593274  5.332914994  4.607381544
      6  -0.916425367  5.375190418 -7.209984663
      6  -4.773254987  4.300512942  6.348687286
  */
  private boolean readCoordinates() throws Exception {
    if (doApplySymmetry) {
      String[] atomStr = PT.getTokens(rd());
      nAtoms = Integer.parseInt(atomStr[0]);
    } else {
      nAtoms = Integer.MAX_VALUE;
    }
    setFractionalCoordinates(false);
    int counter = 0;
    while (counter < nAtoms && rd() != null) {
      String[] tokens = getTokens();
      int an = PT.parseInt(tokens[0]);
      if (an < 0) {
        break;
      }
      line = null;
      addAtomXYZSymName(tokens, 1, null,
          getElementSymbol(an));
      counter++;
    }
    asc.setAtomSetName(
        animation ? "Structure " + (++animationStep) : "Initial coordinates");
    if (line != null)
      setSymmetry();
    return (line == null);
  }

}
