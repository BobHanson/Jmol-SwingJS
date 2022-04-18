package org.jmol.adapter.readers.xtal;

import java.util.Map;
import java.util.Map.Entry;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.util.Logger;

import javajs.util.Lst;
import javajs.util.PT;

/**
 * 
 * crude PWmat atom.config reader
 * 
 * http://pwmatus.com/manual
 * 
 * @author hansonr
 */

public class PWmatReader extends AtomSetCollectionReader {

  private int nAtoms;

  public PWmatReader() {
  }
  
  @Override
  protected void initializeReader() throws Exception {
    setHighPrecision();
    doApplySymmetry = true;
  }

  @Override
  protected boolean checkLine() throws Exception {
    if (nAtoms == 0) {
      readComments();
      setSpaceGroupName("P1");
      nAtoms = PT.parseInt(line);
      setFractionalCoordinates(true);
      return true;
    }
    removeComments();
    String lc = line.toLowerCase().trim();
    if (lc.length() == 0)
      return true;
    if (lc.startsWith("lattice")) {
      readUnitCell();
    } else if (lc.startsWith("position")) {
      readCoordinates();
    } else {
      readDataBlock(lc);
    }
    return true;
  }
  
  private void readComments() {
    // TODO
    
  }

  private void readUnitCell() throws Exception {
    // The lattice section consists of 3 lines representing 
    // the lattice vector. For each line, there could be an extra 
    // 3 numbers followed, please ignore.
    
    double[] unitCellData = new double[3];
    addExplicitLatticeVector(0, fillDoubleArray(getLine(), 0, unitCellData), 0);
    addExplicitLatticeVector(1, fillDoubleArray(getLine(), 0, unitCellData), 0);
    addExplicitLatticeVector(2, fillDoubleArray(getLine(), 0, unitCellData), 0);
  }


  private void readCoordinates() throws Exception {
    // Following lattice section is the position section. 
    // As long as there is "position" case-insensitive 
    // leading the line, that is our position section. 
    // The position section consists of N lines. 
    // Atom number is the from the line.
    
    Lst<double[]> constraints = new Lst<double[]>();
    boolean haveConstraints = true;
    int i = 0;
    while (i++ < nAtoms && getLine() != null) {
      String[] tokens = getTokens();
      addAtomXYZSymName(tokens, 1, null,
          getElementSymbol(Integer.parseInt(tokens[0])));
      haveConstraints = (tokens.length >= 7) && haveConstraints;
      if (haveConstraints)
        constraints.addLast(new double[] { Double.parseDouble(tokens[4]),
            Double.parseDouble(tokens[5]), Double.parseDouble(tokens[6]) });
    }
    double[] cx = new double[nAtoms];
    double[] cy = new double[nAtoms];
    double[] cz = new double[nAtoms];
    double[] c = new double[] { 1, 1, 1 };
    for (i = nAtoms; --i >= 0;) {
      if (haveConstraints)
        c = constraints.get(i);
      cx[i] = c[0];
      cy[i] = c[1];
      cz[i] = c[2];
    }
    setVectors("constraints", cx, cy, cz, nAtoms);
  }

  private void readDataBlock(String name) throws Exception {
    getLine();
    String[] tokens = getTokens();
    switch (tokens.length) {
    case 1:
    case 2:
    case 3:
      readItems(name, tokens.length - 1, null);
      break;
    case 4: // elemno, x,y,z
      readVectors(name, 1, true);
      break;
    default:
      Logger.error("PWmatReader block " + name.toUpperCase() + " ignored");
      break;
    }
  }

  private boolean haveMagnetic = false;
  
  private void readItems(String name, int offset, double[] values) throws Exception {
    if (name.equalsIgnoreCase("magnetic"))
      haveMagnetic = true;
    name = "pwm_" + name;
    if (values == null) {
      values = new double[nAtoms];
    } else {
      getLine();
    }
    int n = 0;
    for (int i = 0;;) {
      String[] tokens = getTokens();
      if ((values[i] = Double.parseDouble(tokens[offset])) != 0)
        n++;
      if (++i == nAtoms)
        break;
      getLine();
    }
    setProperties(name, values, asc.iSet, n);
  }

  private void setProperties(String name, double[] values, int iSet, int n) {
    asc.setAtomProperties(name, values, asc.iSet, false);
    Logger.info("PWmatReader: " + name.toUpperCase()  +" processed for " + n + " atoms");
    appendLoadNote("PWmatReader read property_" + name);
  }

  private void readVectors(String name, int offset, boolean haveLine) throws Exception {
    if (!haveLine)
      getLine();
    double[] valuesX = new double[nAtoms];
    double[] valuesY = new double[nAtoms];
    double[] valuesZ = new double[nAtoms];
    int n = 0;
    for (int i = 0;;) {
      String[] tokens = getTokens();
      if ((((valuesX[i] = Double.parseDouble(tokens[offset])) == 0 ? 0 : 1)
          | ((valuesY[i] = Double.parseDouble(tokens[offset + 1])) == 0 ? 0 : 1)
          | ((valuesZ[i] = Double.parseDouble(tokens[offset + 2])) == 0 ? 0 : 1)) != 0)
        n++;
      if (++i == nAtoms)
        break;
      getLine();
    }
    setVectors(name, valuesX, valuesY, valuesZ, n);
  }

  private String getLine() throws Exception {
    rd();
    return removeComments();
  }

  private String removeComments() {
    if (line != null) {
      int pt = line.indexOf("#");
      if (pt >= 0) {
        line = line.substring(0, pt).trim();
      }
    }
    return line;
  }

  private void setVectors(String name, double[] valuesX, double[] valuesY,
                          double[] valuesZ, int n) {
    name = "pwm_" + name;
    asc.setAtomProperties(name + "_x", valuesX, asc.iSet, false);
    asc.setAtomProperties(name + "_y", valuesY, asc.iSet, false);
    asc.setAtomProperties(name + "_z", valuesZ, asc.iSet, false);
    Logger.info("PWmatReader: " + name.toUpperCase() + " processed for " + n
        + " atoms");
    appendLoadNote("PWmatReader read property_" + name + "_x/_y/_z");
    if (name.equals("pwm_magnetic_xyz")) {
      for (int i = 0; i < nAtoms; i++) {
        asc.addVibrationVector(i, valuesX[i], valuesY[i], valuesZ[i]);
      }
      addJmolScript("vectors 0.2;set vectorscentered");
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void applySymmetryAndSetTrajectory() throws Exception {
    super.applySymmetryAndSetTrajectory();
    if (nAtoms != asc.ac) {
      nAtoms = asc.ac;
      Map<String, Object> p = (Map<String, Object>) asc
          .getAtomSetAuxiliaryInfoValue(asc.iSet, "atomProperties");
      if (p != null) {
        Atom[] atoms = asc.atoms;
        for (Entry<String, Object> e : p.entrySet()) {
          String key = e.getKey();
          if (key.startsWith("pwm_")) {
            double[] af = (double[]) e.getValue();
            double[] af2 = new double[nAtoms];
            for (int j = 0; j < nAtoms; j++)
              af2[j] = af[atoms[j].atomSite];
            e.setValue(af2);
          }
        }
      }
    }
  }

  @Override
  protected void finalizeSubclassReader() throws Exception {
    if (!haveMagnetic) {
      setProperties("pwm_magnetic", new double[asc.ac], nAtoms, nAtoms);
    }
  }

}
