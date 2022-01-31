package org.jmol.adapter.readers.xtal;

import org.jmol.adapter.smarter.AtomSetCollectionReader;

import javajs.util.PT;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.adapter.smarter.Atom;

/**
 * SIESTA http://www.icmab.es/siesta/
 * 
 * @author Pieremanuele Canepa, Room 104, FM Group School of Physical Sciences,
 *         Ingram Building, University of Kent, Canterbury, Kent, CT2 7NH United
 *         Kingdom, pc229@kent.ac.uk
 * 
 * @version 1.0
 */

public class SiestaReader extends AtomSetCollectionReader {

  private int noAtoms;
  private String acfUnits = "bohr";
  private String[] tokens;

  @Override
  protected void initializeReader() {
    doApplySymmetry = true;
  }

  private final int STATE_UNKNOWN = 0;
  private final int STATE_INPUT = 1;
  private final int STATE_OUTPUT = 2;
  
  private int state = STATE_UNKNOWN;
  private float acfFactor;
  
  
  @Override
  protected boolean checkLine() throws Exception {
    if (line.length() == 0 || line.charAt(0) == '#' || line.indexOf(' ') < 0)
      return true;
    switch (state) {
    case STATE_UNKNOWN:
        if (line.indexOf("Dump of input data file") >= 0) {
          state = STATE_INPUT;
          return true;
        }
        tokens = getTokens();
        if (fixToken(0).equals("numberofspecies")) {
          state = STATE_INPUT;
          return false;
        }
        return true;
    case STATE_INPUT:
      if (line.indexOf("End of input data file") >= 0) {
        state = STATE_OUTPUT;
          return true;
      }
      tokens = getTokens();
       if (tokens[0].equals("%block")) {
         readBlock(fixToken(1));
       } else {
         readValue(fixToken(0));
       }
       return true;
    }
  // output section
  if (line.contains("outcoor: Atomic coordinates")) {
    if (doGetModel(++modelNumber, null))
      readAtomsCartGeomThenCell();
    return true;
  }
  return true;
  }


  private void readValue(String key) throws Exception {
    // Jmol on SourceForge is still in Java 6 :( so no switches for strings
    if (key.equals("latticeconstant")) {
      setCell("latticeconstant");
    } else if (key.equals("atomiccoordinatesformat")) {
      readAtomicCoordinatesFormat();
    }
  }

  private boolean readBlock(String key) throws Exception {
    if (key.equals("latticevectors") || key.equals("latticeparameters"))
      return setCell(key);
    if (key.equals("chemicalspecieslabel"))
      return readSpecies();
    if (key.equals("atomiccoordinatesandatomicspecies")) {
      if (!doGetModel(++modelNumber, null)) {
        skipModel();
        return false;
      }
      return readAtoms();
    }
    discardLinesUntilContains("%endblock");
    return true;
  }

  Map<String, String[]>htSpecies;
  
// %block ChemicalSpeciesLabel
//  1  8  O      # Species index, atomic number, species label
//  2  1  H
// %endblock ChemicalSpeciesLabel

  private boolean readSpecies() throws Exception {
    htSpecies = new Hashtable<String, String[]>();
    while(rdSiesta().indexOf("%") < 0) {
      tokens = getTokens();
      htSpecies.put(tokens[0], tokens);
    }
    return false;
  }

  private String fixToken(int i) {
    return PT.replaceAllCharacters(tokens[i], "_.-","").toLowerCase();
  }
  
  public String rdSiesta() throws Exception {
    String s = rd();
    int pt = s.indexOf("#");
    return (pt < 0 ? s : s.substring(pt)).trim();
  }

  private float getACFValue(float v) {
    if (acfFactor == 0) {
      boolean isScaledCartesian = (acfUnits == "scaledcartesian");
      if (isScaledCartesian)
        acfUnits = latticeUnits;
      acfUnits = PT.rep(acfUnits, "notscaledcartesian", "");
      switch (acfUnits.charAt(0)) {
      default:
      case 'b'://"bohr":
        setFractionalCoordinates(isScaledCartesian);
        acfFactor = (float) (ACF_ANG / ACF_BOHR);
        break;
      case 'm':
        setFractionalCoordinates(isScaledCartesian);
        acfFactor = (float) (ACF_ANG / ACF_M);
        break;
      case 'n'://"nm":
        setFractionalCoordinates(isScaledCartesian);
        acfFactor = (float) (ACF_ANG / ACF_NM);
        break;
      case 'a'://"ang":
        setFractionalCoordinates(isScaledCartesian);
        acfFactor = 1;
        break;
      case 'f'://"fractional":
      case 's'://"scaledbylatticevectors":
        setFractionalCoordinates(true);
        acfFactor = 1;
        break;
      }
      if (isScaledCartesian) {
        acfFactor /= latticeConstant;
        setFractionalCoordinates(true);
      }
    }
    return (acfFactor * v);
  }
  private void readAtomicCoordinatesFormat() {
    acfUnits = tokens[1].toLowerCase().intern();

    
//    AtomicCoordinatesFormat (string): Character string to specify the format of the atomic
//    positions in input. These can be expressed in four forms:
//
//    Bohr or NotScaledCartesianBohr (atomic positions are given directly in Bohr, in
//    cartesian coordinates)
//
//    Ang or NotScaledCartesianAng (atomic positions are given directly in Angstrom, in
//    cartesian coordinates)
//
//    ScaledCartesian (atomic positions are given in cartesian coordinates, in units of the
//    lattice constant)
//
//    Fractional or ScaledByLatticeVectors (atomic positions are given referred to the
//    lattice vectors)
//
//    Default value: NotScaledCartesianBohr



    
  }

  private void skipModel() throws Exception {
    discardLinesUntilContains("%endblock AtomicCoordinatesAndAtomicSpecies");
  }

  private float[] unitCellVectors, unitCellParamsS;
  private float latticeConstant = 1;
  private String latticeUnits;

  private boolean setCell(String key) throws Exception {
    if (key.equals("latticevectors")) {
      unitCellVectors = new float[9];
      fillFloatArray(null, 0, unitCellVectors);
    } else if (key.equals("latticeconstant")) {
     String[] tokens = getTokens();
      latticeConstant = this.parseFloatStr(tokens[1]);
      latticeUnits = tokens[2].toLowerCase();
    } else if (key.equals("latticeparameters")) {
      unitCellParamsS = new float[6];
      fillFloatArray(line.substring(line.indexOf("ters") + 4), 0, unitCellParamsS);
    }
    return true;
  }

  
  private final static double ACF_M = 1/1e-10;
  private final static double ACF_NM = 1e-9/1e-10;
  private final static double ACF_ANG = 1e-10/1e-10;
  private final static double ACF_BOHR = 0.529177E-10/1E-10;//, // default
    
  
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
  private boolean readAtoms() throws Exception {
    newAtomSet();
    if (unitCellVectors != null) {
      addExplicitLatticeVector(0, unitCellVectors, 0);
      addExplicitLatticeVector(1, unitCellVectors, 3);
      addExplicitLatticeVector(2, unitCellVectors, 6);
    } else if (unitCellParamsS != null) {
      setUnitCell(unitCellParamsS[0] * latticeConstant,
          unitCellParamsS[1] * latticeConstant,
          unitCellParamsS[2] * latticeConstant,
          unitCellParamsS[3],
          unitCellParamsS[4],
          unitCellParamsS[5]);
    }
  
    // see https://github.com/id23cat/siesta-3.1/blob/master/Src/fdf/fdf.f
    
//    .  'length', 'm       ', 1.D0,
//    .  'length', 'nm      ', 1.D-9,
//    .  'length', 'Ang     ', 1.D-10,
//    .  'length', 'Bohr    ', 0.529177D-10, // default
//    
    while (rdSiesta() != null && line.indexOf("%endblock Atomic") < 0) {
      String[] tokens = getTokens();
      String[] species = (htSpecies == null ? new String[] { null, null, tokens[4] } : htSpecies.get(tokens[3]));
      String name = species[2];
      String sym = (species[1] == null ? name : getElementSymbol(parseIntStr(species[1])));
      addAtomXYZSymName(tokens, 0, sym, name);
    }
    noAtoms = asc.ac;
    return true;
  }
  
  
  @Override
  public void setAtomCoordXYZ(Atom atom, float x, float y, float z) {
    super.setAtomCoordXYZ(atom, getACFValue(x), getACFValue(y), getACFValue(z));
  }

  //  AtomicCoordinatesFormat (string): Character string to specify the format of the atomic
  //  positions in input. These can be expressed in four forms:
  //
  //  Bohr or NotScaledCartesianBohr (atomic positions are given directly in Bohr, in
  //  cartesian coordinates)
  //
  //  Ang or NotScaledCartesianAng (atomic positions are given directly in Angstrom, in
  //  cartesian coordinates)
  //
  //  ScaledCartesian (atomic positions are given in cartesian coordinates, in units of the
  //  lattice constant)
  //
  //  Fractional or ScaledByLatticeVectors (atomic positions are given referred to the
  //  lattice vectors)
  //
  //  Default value: NotScaledCartesianBohr
  //  
  //  
  //
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
      rdSiesta();
    }
    discardLinesUntilContains("outcell: Unit cell vectors");
    setCell("vectors");
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
