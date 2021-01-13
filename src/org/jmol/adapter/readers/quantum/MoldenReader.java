package org.jmol.adapter.readers.quantum;

import org.jmol.adapter.smarter.Atom;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;

import java.util.Arrays;
import java.util.Hashtable;

import java.util.Map;

import javajs.util.BS;

import org.jmol.quantum.QS;
import org.jmol.util.Logger;

/**
 * A molecular structure and orbital reader for MolDen files.
 * See http://www.cmbi.ru.nl/molden/molden_format.html
 * 
 * updated by Bob Hanson <hansonr@stolaf.edu> for Jmol 12.0/12.1
 * 
 * adding [spacegroup] [operators] [cell] [cellaxes] for Jmol 14.3.7 
 * 
 * @author Matthew Zwier <mczwier@gmail.com>
 */

public class MoldenReader extends MopacSlaterReader {
  
  protected boolean loadGeometries;
  protected boolean loadVibrations;
  protected boolean vibOnly;
  protected boolean optOnly;
  protected boolean doSort = true;
 
  protected String orbitalType = "";
  protected int modelAtomCount;
  
  private Lst<String> lineBuffer;
  
  @Override
  protected void initializeReader() {
    vibOnly = checkFilterKey("VIBONLY");
    optOnly = checkFilterKey("OPTONLY");
    doSort = !checkFilterKey("NOSORT");
    loadGeometries = !vibOnly && desiredVibrationNumber < 0 && !checkFilterKey("NOOPT");
    loadVibrations = !optOnly && desiredModelNumber < 0 && !checkFilterKey("NOVIB");
    // replace filter with just three MO options
    if (checkFilterKey("ALPHA"))
      filter = "alpha";
    else if (checkFilterKey("BETA"))
      filter = "beta";
    else 
      filter = getFilter("SYM=");
  }

  @Override
  protected boolean checkLine() throws Exception {
    if (!line.contains("["))
      return true;
    line = line.toUpperCase().trim();
    if (!line.startsWith("["))
      return true;
    Logger.info(line);
    if (line.indexOf("[ATOMS]") == 0) {
      readAtoms();
      modelAtomCount = asc.getAtomSetAtomCount(0);
      if (asc.atomSetCount == 1 && moData != null)
        finalizeMOData(moData);
      return false;
    }
    if (line.indexOf("[GTO]") == 0)
      return readGaussianBasis();
    if (line.indexOf("[STO]") == 0)
      return readSlaterBasis();
    if (line.indexOf("[MO]") == 0) 
      return (!doReadMolecularOrbitals || readMolecularOrbitals());
    if (line.indexOf("[FREQ]") == 0)
      return (!loadVibrations || readFreqsAndModes());
    if (line.indexOf("[GEOCONV]") == 0)
      return (!loadGeometries || readGeometryOptimization());
    if (checkOrbitalType(line))
      return true;
    if (checkSymmetry())
      return false;
    return true;
  }

  private boolean checkSymmetry() throws Exception {
    // extension for symmetry
    if (line.startsWith("[SPACEGROUP]")) {
      setSpaceGroupName(rd());
      rd();
      return true;
    }
    if (line.startsWith("[OPERATORS]")) {
      while (rd() != null && line.indexOf("[") < 0)
        if (line.length() > 0) {
          Logger.info("adding operator " + line);
          setSymmetryOperator(line);
        }
      return true;
    }
    if (line.startsWith("[CELL]")) {
      rd();
      Logger.info("setting cell dimensions " + line);
      // ANGS assumed here
      next[0] = 0;
      for (int i = 0; i < 6; i++)
        setUnitCellItem(i, parseFloat());
      rd();
      return true;
    }
    if (line.startsWith("[CELLAXES]")) {
      float[] f = new float[9];
      fillFloatArray(null, 0, f);
      addExplicitLatticeVector(0, f, 0);
      addExplicitLatticeVector(1, f, 3);
      addExplicitLatticeVector(2, f, 6);
      return true;
    }
    return false;
  }

  @Override
  public void finalizeSubclassReader() throws Exception {
    // a hack to make up for Molden's ** limitation in writing shell information
    // assumption is that there is an atom of the same type just prior to the missing one.
    if (!bsBadIndex.isEmpty())
      try {
        int ilast = 0;
        Atom[] atoms = asc.atoms;
        int nAtoms = asc.ac;
        bsAtomOK.set(nAtoms);
        int n = shells.size();
        for (int i = 0; i < n; i++) {
          int iatom = shells.get(i)[0];
          if (iatom != Integer.MAX_VALUE) {
            ilast = atoms[iatom - 1].elementNumber;
            continue;
          }
          for (int j = bsAtomOK.nextClearBit(0); j >= 0; j = bsAtomOK
              .nextClearBit(j + 1)) {
            if (atoms[j].elementNumber == ilast) {
              shells.get(i)[0] = j + 1;
              Logger.info("MoldenReader assigning shells starting with " + i
                  + " for ** to atom " + (j + 1) + " z " + ilast);
              for (; ++i < n && !bsBadIndex.get(i)
                  && shells.get(i)[0] == Integer.MAX_VALUE;)
                shells.get(i)[0] = j + 1;
              i--;
              bsAtomOK.set(j);
              break;
            }
          }
        }
      } catch (Exception e) {
        Logger.error("Molden reader could not assign shells -- abandoning MOs");
        asc.setCurrentModelInfo("moData", null);
      }
    finalizeReaderASCR();
  }
  private void readAtoms() throws Exception {
    /* 
     [Atoms] {Angs|AU|Fractional}
     C     1    6         0.0076928100       -0.0109376700        0.0000000000
     H     2    1         0.0779745600        1.0936027600        0.0000000000
     H     3    1         0.9365572000       -0.7393011000        0.0000000000
     H     4    1         1.1699572800        0.2075167300        0.0000000000
     H     5    1        -0.4338802400       -0.3282176500       -0.9384614500
     H     6    1        -0.4338802400       -0.3282176500        0.9384614500
     */
    String coordUnit = PT.getTokens(line.replace(']', ' '))[1];
    
    boolean isFractional = (coordUnit.indexOf("FRACTIONAL") >= 0);
    boolean isAU = (!isFractional && coordUnit.indexOf("ANGS") < 0);
    if (isAU && coordUnit.indexOf("AU") < 0)
      Logger.error("Molden atom line does not indicate units ANGS, AU, or FRACTIONAL -- AU assumed: " + line); 
    setFractionalCoordinates(isFractional);
    float f = (isAU ? ANGSTROMS_PER_BOHR : 1);
    while (rd() != null && line.indexOf('[') < 0) {    
      String [] tokens = getTokens();
      if (tokens.length < 6)
        continue;
      Atom atom = setAtomCoordScaled(null, tokens, 3, f);
      atom.atomName = tokens[0];
      atom.elementNumber = (short) parseIntStr(tokens[2]);
    }    
  }
  
  private BS bsAtomOK = new BS();
  private BS bsBadIndex = new BS();
  private int[] nSPDF;
  private boolean haveEnergy = true;
  
  boolean readSlaterBasis() throws Exception {
    /*
    1    0    0    0    1             1.5521451600        0.9776767193          
    1    1    0    0    0             1.5521451600        1.6933857512          
    1    0    1    0    0             1.5521451600        1.6933857512          
    1    0    0    1    0             1.5521451600        1.6933857512          
    2    0    0    0    0             1.4738648100        1.0095121222          
    3    0    0    0    0             1.4738648100        1.0095121222          
     */
    
    nCoef = 0;
    while (rd() != null && line.indexOf("[") < 0) {
      String[] tokens = getTokens();
      if (tokens.length < 7)
        continue;
      addSlater(parseIntStr(tokens[0]), parseIntStr(tokens[1]),
          parseIntStr(tokens[2]), parseIntStr(tokens[3]), parseIntStr(tokens[4]),
          parseFloatStr(tokens[5]), parseFloatStr(tokens[6]));
      nCoef++;
    }
    setSlaters(false, false);
    //moData.put("isNormalized", Boolean.TRUE);
    return false;
  }

  private boolean readGaussianBasis() throws Exception {
    /* 
     [GTO]
       1 0
      s   10 1.00
       0.8236000000D+04  0.5309998617D-03
       0.1235000000D+04  0.4107998930D-02
       0.2808000000D+03  0.2108699451D-01
       0.7927000000D+02  0.8185297868D-01
       0.2559000000D+02  0.2348169388D+00
       0.8997000000D+01  0.4344008869D+00
       0.3319000000D+01  0.3461289099D+00
       0.9059000000D+00  0.3937798974D-01
       0.3643000000D+00 -0.8982997660D-02
       0.1285000000D+00  0.2384999379D-02
      s   10 1.00
     */
    shells = new  Lst<int[]>();
    Lst<float[]> gdata = new  Lst<float[]>();
    int atomIndex = 0;
    int gaussianPtr = 0;
    nCoef = 0;
    nSPDF = new int[12];
    discardLinesUntilNonBlank();
    while (line != null
        && !((line = line.trim()).length() == 0 || line.charAt(0) == '[')) {
      // First, expect the number of the atomic center
      // The 0 following the atom index is now optional
      String[] tokens = getTokens();

      // Molden may have ** in place of the atom index when there are > 99 atoms
      atomIndex = parseIntStr(tokens[0]) - 1;
      if (atomIndex == Integer.MAX_VALUE) {
        bsBadIndex.set(shells.size());
      } else {
        bsAtomOK.set(atomIndex);
      }
      // Next is a sequence of shells and their primitives
      while (rd() != null && (line = line.trim()).length() > 0 && line.charAt(0) != '[') {
        // Next line has the shell label and a count of the number of primitives
        tokens = getTokens();
        String shellLabel = tokens[0].toUpperCase();
        int type = BasisFunctionReader.getQuantumShellTagID(shellLabel);
        int nPrimitives = parseIntStr(tokens[1]);
        int[] slater = new int[4];
        nSPDF[type]++;
        slater[0] = atomIndex + 1;
        slater[1] = type;
        slater[2] = gaussianPtr + 1;
        slater[3] = nPrimitives;
        int n = getDfCoefMaps()[type].length;
        //System.out.println("adding " + n + " coefficients type " + JmolAdapter.getQuantumShellTag(type) + " for atom " + atomIndex);
        nCoef += n;
        for (int ip = nPrimitives; --ip >= 0;) {
          // Read ip primitives, each containing an exponent and one (s,p,d,f)
          // or two (sp) contraction coefficient(s)
          String[] primTokens = PT.getTokens(rd());
          int nTokens = primTokens.length;
          float orbData[] = new float[nTokens];

          for (int d = 0; d < nTokens; d++)
            orbData[d] = parseFloatStr(primTokens[d]);
          gdata.addLast(orbData);
          gaussianPtr++;
        }
        shells.addLast(slater);
      }
      if (line.length() > 0 && line.charAt(0) == '[')
        break;
      rd();
    }

    float[][] garray = AU.newFloat2(gaussianPtr);
    for (int i = 0; i < gaussianPtr; i++) {
      garray[i] = gdata.get(i);
    }
    moData.put("shells", shells);
    moData.put("gaussians", garray);
    Logger.info(shells.size() + " slater shells read");
    Logger.info(garray.length + " gaussian primitives read");
    Logger.info(nCoef + " MO coefficients expected for orbital type " + orbitalType);
    asc.setCurrentModelInfo("moData", moData);
    return false;
  }
 

  private boolean readMolecularOrbitals() throws Exception {
    /*
      [MO]
       Ene=     -11.5358
       Spin= Alpha
       Occup=   2.000000
         1   0.99925949663
         2  -0.00126378192
         3   0.00234724545
     [and so on]
       110   0.00011350764
       Ene=      -1.3067
       Spin= Alpha
       Occup=   1.984643
         1  -0.00865451496
         2   0.79774685891
         3  -0.01553604903
     */

    while (checkOrbitalType(rd())) {
      //
    }
    if (orbitalType == "") {
      createLineBuffer();
    }
     
    fixOrbitalType();
    // TODO we are assuming Jmol-canonical order for orbital coefficients.
    // see BasisFunctionReader
    // TODO no check here for G orbitals

    String[] tokens = getMoTokens(line);
    while (tokens != null && tokens.length > 0 && tokens[0].indexOf('[') < 0) {
      Map<String, Object> mo = new Hashtable<String, Object>();
      Lst<String> data = new Lst<String>();
      float energy = Float.NaN;
      float occupancy = Float.NaN;
      String symmetry = null;
      String key;
      while (parseIntStr(key = tokens[0]) == Integer.MIN_VALUE) {
        if (key.startsWith("Ene")) {
          energy = parseFloatStr(tokens[1]);
        } else if (key.startsWith("Occup")) {
          occupancy = parseFloatStr(tokens[1]);
        } else if (key.startsWith("Sym")) {
          symmetry = tokens[1];
        } else if (key.startsWith("Spin")) {
          alphaBeta = tokens[1].toLowerCase();
        }
        tokens = getMoTokens(null);
      }
      int pt = 0;
      int offset = 0;
      while (tokens != null && tokens.length > 0
          && parseIntStr(tokens[0]) != Integer.MIN_VALUE) {
        if (tokens.length != 2)
          throw new Exception("invalid MO coefficient specification");
        // tokens[0] is the function number, and tokens[1] is the coefficient
        int i = parseIntStr(tokens[0]);
        if (pt == 0 && i == nCoef + 1 && "beta".equals(alphaBeta)) {
          // in case beta starts with ncoef + 1 (CRYSTAL output)
          offset = -nCoef;
          }
        i += offset;
        while (i > ++pt)
          data.addLast("0");
        data.addLast(tokens[1]);
        tokens = getMoTokens(null);
      }
      if (orbitalType.equals("") && data.size() < nCoef) {
        Logger.info("too few orbital coefficients for 6D");
        checkOrbitalType("[5D]");
      }
      while (++pt <= nCoef) {
        data.addLast("0");
      }
      
      float[] coefs = new float[nCoef];
      for (int i = data.size(); --i >= 0;)
        coefs[i] = parseFloatStr(data.get(i));
      String l = line;
      line = "" + symmetry;
      if (filterMO()) {
        mo.put("coefficients", coefs);
        if (Float.isNaN(energy)) {
          haveEnergy = false;
        } else {
          mo.put("energy", Float.valueOf(energy));
        }
        if (!Float.isNaN(occupancy))
          mo.put("occupancy", Float.valueOf(occupancy));
        if (symmetry != null)
          mo.put("symmetry", symmetry);
        if (alphaBeta.length() > 0)
          mo.put("type", alphaBeta);
        setMO(mo);
        if (debugging) {
          Logger.debug(coefs.length + " coefficients in MO " + orbitals.size());
        }
      }
      line = l;
    }
    if (debugging)
      Logger.debug("read " + orbitals.size() + " MOs");
    setMOs("");// Molden does not specify units.
    if (haveEnergy && doSort)
      sortMOs();
    
    return false;
  }
  
  private int ptLineBuf, bufLen;
  @Override
  public String rd() throws Exception {
    if (++ptLineBuf < bufLen) {
      return line = lineBuffer.get(ptLineBuf);
    } 
    if (bufLen > 0) {
      lineBuffer = null;
      bufLen = -1;
      return null;
    }
    return super.rd();
  }

  private void createLineBuffer() throws Exception {
    if (lineBuffer != null)
      return;
    lineBuffer = new Lst<String>();
    String l0 = line;
    // read to end of file
    while (super.rd() != null) {
      if (!line.contains("[") || !checkOrbitalType(line)) {
        lineBuffer.addLast(line);
      } 
    }
    bufLen = lineBuffer.size();
    ptLineBuf = -1;
    line = l0;
  }

  @SuppressWarnings("unchecked")
  private void sortMOs() {
    Object[] list = orbitals.toArray(new Object[orbitals.size()]);
    Arrays.sort(list, new MOEnergySorter());
    orbitals.clear();
    for (int i = 0; i < list.length; i++)
      orbitals.addLast((Map<String, Object>)list[i]);
  }

  private String[] getMoTokens(String line) throws Exception {
    return (line == null && (line = rd()) == null ? null : PT.getTokens(line.replace('=',' ')));
  }

  private boolean checkOrbitalType(String line) {
    if (line.length() > 3 && "5D 6D 7F 10 9G 15 11 21".indexOf(line.substring(1,3)) >= 0) {
      if (orbitalType.indexOf(line) >= 0)
        return true;
      if (line.indexOf("G") >= 0 || line.indexOf("H") >= 0 || line.indexOf("I") >= 0)
        appendLoadNote("Unsupported orbital type ignored: " + line);
      orbitalType += line;
      Logger.info("Orbital type set to " + orbitalType);
      fixOrbitalType();
      return true;
    }
    return false;
  }

  private void fixOrbitalType() {
    if (orbitalType.contains("5D")) {
      fixSlaterTypes(QS.DC, QS.DS);
      fixSlaterTypes(QS.FC, QS.FS);
      fixSlaterTypes(QS.GC, QS.GS);
      fixSlaterTypes(QS.HC, QS.HS);
    } 
    if (orbitalType.contains("10F")) {
      fixSlaterTypes(QS.FS, QS.FC);
      fixSlaterTypes(QS.GS, QS.GC);
      fixSlaterTypes(QS.HS, QS.HC);
    } 
    if (orbitalType.contains("15G")) {
      fixSlaterTypes(QS.GS, QS.GC);
      fixSlaterTypes(QS.HS, QS.HC);
    } 
  }

  private boolean readFreqsAndModes() throws Exception {
    String[] tokens;
    Lst<String> frequencies = new  Lst<String>();
 //   BitSet bsOK = new BitSet();
 //   int iFreq = 0;
    while (rd() != null && line.indexOf('[') < 0) {
      String f = getTokens()[0];
//      bsOK.set(iFreq++, parseFloatStr(f) != 0);
      frequencies.addLast(f);
    }
    int nFreqs = frequencies.size();
    skipTo("[FR-COORD]");
    if (!vibOnly)
      readAtomSet("frequency base geometry", true, true);
    skipTo("[FR-NORM-COORD]");
    boolean haveVib = false;
    for (int nFreq = 0; nFreq < nFreqs; nFreq++) {
      skipTo("vibration");
// RPFK: if the frequency was given, the mode should be read (even when 0.0)
//      if (!bsOK.get(nFreq) || !doGetVibration(++vibrationNumber)) 
//        continue;
      doGetVibration(++vibrationNumber);
      if (haveVib)
        asc.cloneLastAtomSet();
      haveVib = true;
      asc.setAtomSetFrequency(vibrationNumber, null, null, "" + Double.valueOf(frequencies.get(nFreq)), null);
      int i0 = asc.getLastAtomSetAtomIndex();
      for (int i = 0; i < modelAtomCount; i++) {
        tokens = PT.getTokens(rd());
        asc.addVibrationVector(i + i0,
            parseFloatStr(tokens[0]) * ANGSTROMS_PER_BOHR,
            parseFloatStr(tokens[1]) * ANGSTROMS_PER_BOHR,
            parseFloatStr(tokens[2]) * ANGSTROMS_PER_BOHR);
      }
    }
    return true;
  }
  
  /*
[GEOCONV]
energy
-.75960756002000E+02
-.75961091052100E+02
-.75961320555300E+02
-.75961337317300E+02
-.75961338487700E+02
-.75961338493500E+02
max-force
0.15499000000000E-01
0.11197000000000E-01
0.50420000000000E-02
0.15350000000000E-02
0.42000000000000E-04
0.60000000000000E-05
[GEOMETRIES] XYZ
     3

 o  0.00000000000000E+00 0.00000000000000E+00 -.36565628831562E+00
 h  -.77567072215814E+00 0.00000000000000E+00 0.18282805096053E+00
 h  0.77567072215814E+00 0.00000000000000E+00 0.18282805096053E+00

  */
  private boolean readGeometryOptimization() throws Exception {
    Lst<String> energies = new  Lst<String>();
    rd(); // energy
    while (rd() != null 
        && line.indexOf("force") < 0)
      energies.addLast("" + Double.valueOf(line.trim()));
    skipTo("[GEOMETRIES] XYZ");
    int nGeom = energies.size();
    int firstModel = (optOnly || desiredModelNumber >= 0 ? 0 : 1);
    modelNumber = firstModel; // input model counts as model 1; vibrations do not count
    boolean haveModel = false;
    if (desiredModelNumber == 0 || desiredModelNumber == nGeom)
      desiredModelNumber = nGeom; 
    else if (asc.atomSetCount > 0)
      finalizeMOData(moData);
    for (int i = 0; i < nGeom; i++) {
      readLines(2);
      if (doGetModel(++modelNumber, null)) {
        readAtomSet("Step " + (modelNumber - firstModel) + "/" + nGeom + ": " + energies.get(i), false, 
            !optOnly || haveModel);
        haveModel = true;
      } else {
        readLines(modelAtomCount);
      }
    }
    return true;
  }

  private void skipTo(String key) throws Exception {
    key = key.toUpperCase();
    if (line == null || !line.toUpperCase().contains(key))
//      discardLinesUntilContains(key);
      while (rd() != null && line.toUpperCase().indexOf(key) < 0) {
      }
    
  }

  private void readAtomSet(String atomSetName, boolean isBohr, boolean asClone) throws Exception {
    if (asClone && desiredModelNumber < 0)
      asc.cloneFirstAtomSet(0);
    float f = (isBohr ? ANGSTROMS_PER_BOHR : 1);
    asc.setAtomSetName(atomSetName);
    if (asc.ac == 0) {
      while (rd() != null && line.indexOf('[') < 0) {    
        String [] tokens = getTokens();
        if (tokens.length == 4)
          setAtomCoordScaled(null, tokens, 1, f).atomName = tokens[0];
      }    
      modelAtomCount = asc.getLastAtomSetAtomCount();
      return;
    }
    Atom[] atoms = asc.atoms;
    int i0 = asc.getLastAtomSetAtomIndex();
    for (int i = 0; i < modelAtomCount; i++)
      setAtomCoordScaled(atoms[i + i0], PT.getTokens(rd()), 1, f);
  }
}
