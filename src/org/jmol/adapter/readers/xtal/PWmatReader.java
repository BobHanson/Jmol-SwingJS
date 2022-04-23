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
/**
 * The format of pwmat config file specification is now clear, would like to
 * adjust PWmatReader accordingly. The pwmat config file format accommodates
 * conversion from other vendors and their formats.
 * 
 * The first line always has number of atoms, before the number, could be
 * spaces. after the number, there could be comments such as "generated from
 * pwmat". Please ignore those comments, only gets the number from the first
 * line.
 * 
 * After the first line, there could be comments please ignore till reading of
 * letter lattice or lattice vector. The letters are case non-sensitive.
 * 
 * The lattice section consists of 3 lines representing lattice vector. For
 * each line, there could be extra 3 numbers followed, please ignore.
 * 
 * Following lattice section, there comes position section. As long as there
 * is "position" case-insensitive leading the line, that is our position
 * section. The position section consists of total lines of total atom number
 * which is the from the first line.
 * 
 * The lattice section, following by position section, then there are optional
 * sections. The cue is always the words case-insensitive. After the keywords,
 * following by the total lines of total atom number.
 */
  private int nAtoms;

  @Override
  protected void initializeReader() throws Exception {
    doApplySymmetry = true;
  }

  private boolean haveLattice;
  private boolean havePositions;
  
  @Override
  protected boolean checkLine() throws Exception {
    // first line has atom count
    if (nAtoms == 0) {
      setSpaceGroupName("P1");
      nAtoms = PT.parseInt(line);
      setFractionalCoordinates(true);
      return true;
    }
    removeComments();
    String lc = line.toLowerCase().trim();
    if (lc.length() == 0)
      return true;
    // do not read anything unit we get the lattice line
    if (!haveLattice) {
      if (lc.startsWith("lattice")) {
        readUnitCell();
        haveLattice = true;
      }
      return true;
    }
    // and then also have positions
    if (!havePositions) {
      if (lc.startsWith("position")) {
        readCoordinates();
        havePositions = true;
      }
      return true;
    }
    if (!readDataBlock(lc)) {
      continuing = false;
    }      
    return true;
  }
  
  private void readUnitCell() throws Exception {
    // The lattice section consists of 3 lines representing 
    // the lattice vector. For each line, there could be an extra 
    // 3 numbers followed, please ignore.
    
    float[] unitCellData = new float[3];
    addExplicitLatticeVector(0, fillFloatArray(getLine(), 0, unitCellData), 0);
    addExplicitLatticeVector(1, fillFloatArray(getLine(), 0, unitCellData), 0);
    addExplicitLatticeVector(2, fillFloatArray(getLine(), 0, unitCellData), 0);
  }


  private void readCoordinates() throws Exception {
    // Following lattice section is the position section. 
    // As long as there is "position" case-insensitive 
    // leading the line, that is our position section. 
    // The position section consists of N lines. 
    // Atom number is the from the line.
    
    Lst<float[]> constraints = new Lst<float[]>();
    boolean haveConstraints = true;
    int i = 0;
    while (i++ < nAtoms && getLine() != null) {
      String[] tokens = getTokens();
      addAtomXYZSymName(tokens, 1, null,
          getElementSymbol(Integer.parseInt(tokens[0])));
      haveConstraints = (tokens.length >= 7) && haveConstraints;
      if (haveConstraints)
        constraints.addLast(new float[] { Float.parseFloat(tokens[4]),
            Float.parseFloat(tokens[5]), Float.parseFloat(tokens[6]) });
    }
    float[] cx = new float[nAtoms];
    float[] cy = new float[nAtoms];
    float[] cz = new float[nAtoms];
    float[] c = new float[] { 1, 1, 1 };
    for (i = nAtoms; --i >= 0;) {
      if (haveConstraints)
        c = constraints.get(i);
      cx[i] = c[0];
      cy[i] = c[1];
      cz[i] = c[2];
    }
    setVectors("constraints", cx, cy, cz, nAtoms);
  }

  private boolean readDataBlock(String name) throws Exception {
    getLine();
    if (line == null)
      return false;
    String[] tokens = getTokens();
    switch (tokens.length) {
    case 1:
    case 2:
    case 3:
      readItems(name, tokens.length - 1, null);
      return true;
    case 4: // elemno, x,y,z
      readVectors(name, 1, true);
      return true;
    default:
      Logger.error("PWmatReader block " + name.toUpperCase() + " ignored");
      return false;
    }
  }

  private boolean haveMagnetic = false;
  
  private void readItems(String name, int offset, float[] values) throws Exception {
    if (name.equalsIgnoreCase("magnetic"))
      haveMagnetic = true;
    name = "pwm_" + name;
    if (values == null) {
      values = new float[nAtoms];
    } else {
      getLine();
    }
    int n = 0;
    for (int i = 0;;) {
      String[] tokens = getTokens();
      if ((values[i] = Float.parseFloat(tokens[offset])) != 0)
        n++;
      if (++i == nAtoms)
        break;
      getLine();
    }
    setProperties(name, values, n);
  }

  private void setProperties(String name, float[] values, int n) {
    asc.setAtomProperties(name, values, asc.iSet, false);
    Logger.info("PWmatReader: " + name.toUpperCase()  +" processed for " + n + " atoms");
    appendLoadNote("PWmatReader read property_" + name);
  }

  private void readVectors(String name, int offset, boolean haveLine) throws Exception {
    if (!haveLine)
      getLine();
    float[] valuesX = new float[nAtoms];
    float[] valuesY = new float[nAtoms];
    float[] valuesZ = new float[nAtoms];
    int n = 0;
    for (int i = 0;;) {
      String[] tokens = getTokens();
      if ((((valuesX[i] = Float.parseFloat(tokens[offset])) == 0 ? 0 : 1)
          | ((valuesY[i] = Float.parseFloat(tokens[offset + 1])) == 0 ? 0 : 1)
          | ((valuesZ[i] = Float.parseFloat(tokens[offset + 2])) == 0 ? 0 : 1)) != 0)
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

  private void setVectors(String name, float[] valuesX, float[] valuesY,
                          float[] valuesZ, int n) {
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
            float[] af = (float[]) e.getValue();
            float[] af2 = new float[nAtoms];
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
    if (!haveMagnetic && asc.ac > 0) {
      setProperties("pwm_magnetic", new float[asc.ac], nAtoms);
    }
  }

}
