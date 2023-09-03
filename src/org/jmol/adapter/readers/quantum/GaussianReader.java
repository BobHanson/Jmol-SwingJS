/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-12 00:46:22 -0500 (Tue, 12 Sep 2006) $
 * $Revision: 5501 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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

package org.jmol.adapter.readers.quantum;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.V3d;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.SmarterJmolAdapter;
import javajs.util.BS;
import org.jmol.quantum.QS;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Tensor;

/**
 * Reader for Gaussian 94/98/03/09 output files.
 * 
 * 4/11/2009 -- hansonr -- added NBO support as extension of MOReader
 *
 **/
public class GaussianReader extends MOReader {
  
  /**
   * Word index of atomic number in line with atom coordinates in an
   * orientation block.
   */
  private final static int STD_ORIENTATION_ATOMIC_NUMBER_OFFSET = 1;
  
  /** Calculated energy with units (if possible). */
  private String energyString = "";
  /**
   * Type of energy calculated, e.g., E(RB+HF-PW91).
   */
  private String energyKey = "";
  
  /** The number of the calculation being interpreted. */
  protected int calculationNumber = 1;
  
  /**  The scan point, where -1 denotes no scan information. */
  private int scanPoint = -1;
  
  /**
   * The number of equivalent atom sets.
   * <p>Needed to associate identical properties to multiple atomsets
   */
  private int equivalentAtomSets = 0;
  private int stepNumber;

  private int moModelSet = -1;
  protected BS namedSets = new BS();

  private boolean isHighPrecision;
  private boolean haveHighPrecision;
  private boolean allowHighPrecision;
  private boolean orientationInput;

  private String orientation;

  @Override 
  protected void initializeReader() throws Exception {
    allowHighPrecision = !checkAndRemoveFilterKey("NOHP");
    orientation = (checkFilterKey("ORIENTATION:INPUT") ? "Input" 
        : checkFilterKey("ORIENTATION:STANDARD") ? "Standard"
        : null);
    orientationInput = (orientation == "Input");
    appendLoadNote("Orientation:" + (orientation == null ? "ALL" : orientation));
    if (orientation != null)
      orientation += " orientation:";
    super.initializeReader();
  }

  /**
   * Reads a Collection of AtomSets from a BufferedReader.
   * 
   * <p>
   * New AtomSets are generated when an <code>Input</code>,
   * <code>Standard</code> or <code>Z-Matrix</code> orientation is read. The
   * occurence of these orientations seems to depend on (in pseudo-code): <code>
   *  <br>&nbsp;if (opt=z-matrix) Z-Matrix; else Input;
   *  <br>&nbsp;if (!NoSymmetry) Standard;
   * </code> <br>
   * Which means that if <code>NoSymmetry</code> is used with a z-matrix
   * optimization, no other orientation besides <code>Z-Matrix</code> will be
   * present. This is important because <code>Z-Matrix</code> may have dummy
   * atoms while the analysis of the calculation results will not, i.e., the
   * <code>Center Numbers</code> in the z-matrix orientation may be different
   * from those in the population analysis!
   * 
   * <p>
   * Single point or frequency calculations always have an <code>Input</code>
   * orientation. If symmetry is used a <code>Standard</code> will be present
   * too.
   * 
   * @return TRUE to read a new line
   * 
   * @throws Exception
   * 
   **/

  @Override
  protected boolean checkLine() throws Exception {
    if (line.startsWith(" Step number")) {
      equivalentAtomSets = 0;
      stepNumber++;
      // check for scan point information
      int scanPointIndex = line.indexOf("scan point");
      if (scanPointIndex > 0) {
        scanPoint = parseIntAt(line, scanPointIndex + 10);
      } else {
        scanPoint = -1; // no scan point information
      }
      return true;
    }
    if (line.indexOf("-- Stationary point found") > 0) {
      // stationary point, if have scanPoint: need to increment now...
      // to get the initial geometry for the next scan point in the proper
      // place
      if (scanPoint >= 0)
        scanPoint++;
      return true;
    }
    if (orientation == null ? line.indexOf("Input orientation:") >= 0
        ||  line.indexOf("Z-Matrix orientation:") >= 0 || line.indexOf("Standard orientation:") >= 0
        : line.indexOf(orientation) >= 0
        || orientationInput && line.indexOf("Z-Matrix orientation:") >= 0) {
      if (!doGetModel(++modelNumber, null)) {
        return checkLastModel();
      }
      equivalentAtomSets++;
      Logger.info(asc.atomSetCount + " model " + modelNumber + " step "
          + stepNumber + " equivalentAtomSet " + equivalentAtomSets
          + " calculation " + calculationNumber + " scan point " + scanPoint
          + line);
      readAtoms();
      return false;
    }
    if (!doProcessLines)
      return true;
    if (line.startsWith(" Energy=")) {
      setEnergy();
      return true;
    }
    if (line.startsWith(" SCF Done:")) {
      readSCFDone();
      return true;
    }
    if (line.startsWith(" Calculating GIAO")) {
      readCSATensors();
      return false;
    }
    if (line.startsWith(" Total nuclear spin-spin coupling")) {
      readCouplings();
      return false;
    }
    
    if (!orientationInput && line.startsWith(" Harmonic frequencies")) {
      readFrequencies(":", true);
      return true;
    }
    if (line.startsWith(" Total atomic charges:")
        || line.startsWith(" Mulliken atomic charges:")) {
      // NB this only works for the Standard or Input orientation of
      // the molecule since it does not list the values for the
      // dummy atoms in the z-matrix
      readPartialCharges();
      return true;
    }
    if (line.startsWith(" Dipole moment")) {
      readDipoleMoment();
      return true;
    }
    if (line.startsWith(" Standard basis:")
        || line.startsWith(" General basis read from")) {
      energyUnits = "";
      calculationType = line.substring(line.indexOf(":") + 1).trim();
      return true;
    }  else if (line.startsWith(" Basis read from chk")){
      energyUnits = "";
      calculationType = line.substring(line.lastIndexOf("\"") + 1).trim();
      return true;
    }
    if (line.startsWith(" AO basis set")) {
      readBasis();
      return true;
    }
    if (line.indexOf("Molecular Orbital Coefficients") >= 0
        || line.indexOf("Natural Orbital Coefficients") >= 0
        || line.indexOf("Natural Transition Orbitals") >= 0) {
      if (!filterMO())
        return true;
      readMolecularOrbitals();
      Logger.info(orbitals.size() + " molecular orbitals read");
      return true;
    }
    if (line.startsWith(" Normal termination of Gaussian")) {
      ++calculationNumber;
      equivalentAtomSets = 0;
      // avoid next calculation to set the last title string
      return true;
    }
    if (line.startsWith(" Mulliken atomic spin densities:")) {
      getSpinDensities(11);
      return true;
    }
    if (line.startsWith(" Mulliken charges and spin densities:")) {
      getSpinDensities(21);
      return true;
    }
    return checkNboLine();
  }
  
  

  @Override
  public void finalizeSubclassReader() throws Exception {
    if (orientation == null) {
      appendLoadNote("\nUse filter 'orientation:xxx' where 'xxx' is one of: input (includes z-matrix), standard, or ALL");
    } else {      
      appendLoadNote("\nfilter: " + filter);
    }    
}
//   Mulliken atomic spin densities:
//     1
// 1  O    1.086438
// 2  H    -.043219
// 3  H    -.043219
  
  private void getSpinDensities(int pt) throws Exception {
    rd();
    double[] data = new double[asc.getLastAtomSetAtomCount()];
    for (int i = 0; i < data.length; i++)
      data[i] = parseDoubleStr(rd().substring(pt, pt + 10));
    asc.setAtomProperties("spin", data, -1, false);
    appendLoadNote(data.length + " spin densities loaded into model " + (asc.iSet + 1));
  }

  /**
   * Interprets the SCF Done: section.
   * 
   * <p>
   * The energyKey and energyString will be set for further AtomSets that have
   * the same molecular geometry (e.g., frequencies). The energy, convergence,
   * -V/T and S**2 values will be set as properties for the atomSet.
   * 
   * @throws Exception
   *           If an error occurs
   **/
  private void readSCFDone() throws Exception {
    String tokens[] = PT.getTokensAt(line, 11);
    if (tokens.length < 4)
      return;
    energyKey = tokens[0];
    asc.setAtomSetEnergy(tokens[2], parseDoubleStr(tokens[2]));
    energyString = tokens[2] + " " + tokens[3];
    // now set the names for the last equivalentAtomSets
    setNames(energyKey + " = " + energyString,
        namedSets, equivalentAtomSets);
    // also set the properties for them
    setProps(energyKey, energyString,
        equivalentAtomSets);
    tokens = PT.getTokens(rd());
    if (tokens.length > 2) {
      setProps(tokens[0], tokens[2],
          equivalentAtomSets);
      if (tokens.length > 5)
        setProps(tokens[3], tokens[5],
            equivalentAtomSets);
      tokens = PT.getTokens(rd());
    }
    if (tokens.length > 2)
      setProps(tokens[0], tokens[2],
          equivalentAtomSets);
  }
  
  private void setProps(String key, String value, int n) {
    for (int i = asc.iSet; --n >= 0 && i >= 0; --i)
      asc.setAtomSetModelPropertyForSet(key, value, i);
  }

  private void setNames(String atomSetName, BS namedSets, int n) {
    for (int i = asc.iSet; --n >= 0 && i >= 0; --i)
      if (namedSets == null || !namedSets.get(i))
        asc.setModelInfoForSet("name", atomSetName, i);
  }

  /**
   * Interpret the Energy= line for non SCF type energy output
   *
   */
  private void setEnergy() {
    String tokens[] = getTokens();
    energyKey = "Energy";
    energyString = tokens[1];
    setNames("Energy = "+tokens[1], namedSets, equivalentAtomSets);
    asc.setAtomSetEnergy(energyString, parseDoubleStr(energyString));
  }
  
  /* GAUSSIAN STRUCTURAL INFORMATION THAT IS EXPECTED
   It looks like sometimes it is possible to have in g03's standard
   orientation section a 'space' for the atomic type, so reading
   the last three tokens as x, y, and z should always work
   */
  
  // GAUSSIAN 04 format
  /*                 Standard orientation:
   ----------------------------------------------------------
   Center     Atomic              Coordinates (Angstroms)
   Number     Number             X           Y           Z
   ----------------------------------------------------------
   1          6           0.000000    0.000000    1.043880
   ##SNIP##    
   ---------------------------------------------------------------------
   */
  
  // GAUSSIAN 98 and 03 format
  /*                    Standard orientation:                         
   ---------------------------------------------------------------------
   Center     Atomic     Atomic              Coordinates (Angstroms)
   Number     Number      Type              X           Y           Z
   ---------------------------------------------------------------------
   1          6             0        0.852764   -0.020119    0.050711
   ##SNIP##
   ---------------------------------------------------------------------
   */
  
  protected void readAtoms() throws Exception {
    asc.newAtomSet();
    // default title : the energy of the previous structure as title
    // this is needed for the last structure in an optimization
    // if energy information is found for this structure the reader
    // will overwrite this setting later.
    haveHighPrecision = false;
    if (energyKey.length() != 0)
      asc.setAtomSetName(energyKey + " = " + energyString);
    asc.setAtomSetEnergy(energyString, parseDoubleStr(energyString));
//  asc.setAtomSetName("Last read atomset.");
    String path = getTokens()[0]; // path = type of orientation
    readLines(4);
    String tokens[];
    while (rd() != null &&
        !line.startsWith(" --")) {
      tokens = getTokens(); // get the tokens in the line
      Atom atom = asc.addNewAtom();
      atom.elementNumber =
        (short)parseIntStr(tokens[STD_ORIENTATION_ATOMIC_NUMBER_OFFSET]);
      if (atom.elementNumber < 0)
        atom.elementNumber = 0; // dummy atoms have -1 -> 0
      setAtomCoordTokens(atom, tokens, tokens.length - 3);
    }
    asc.setAtomSetModelProperty(SmarterJmolAdapter.PATH_KEY,
        "Calculation "+calculationNumber+
        (scanPoint>=0?(SmarterJmolAdapter.PATH_SEPARATOR+"Scan Point "+scanPoint):"")+
        SmarterJmolAdapter.PATH_SEPARATOR+path);
  }

  /* SAMPLE BASIS OUTPUT */
  /*
   * see also http://www.gaussian.com/g_ur/k_gen.htm  -- thank you, Rick Spinney

   Standard basis: VSTO-3G (5D, 7F)
   AO basis set:
   Atom O1       Shell     1 SP   3    bf    1 -     4          0.000000000000          0.000000000000          0.216790088607
   0.5033151319D+01 -0.9996722919D-01  0.1559162750D+00
   0.1169596125D+01  0.3995128261D+00  0.6076837186D+00
   0.3803889600D+00  0.7001154689D+00  0.3919573931D+00
   Atom H2       Shell     2 S   3     bf    5 -     5          0.000000000000          1.424913022638         -0.867160354429
   0.3425250914D+01  0.1543289673D+00
   0.6239137298D+00  0.5353281423D+00
   0.1688554040D+00  0.4446345422D+00
   Atom H3       Shell     3 S   3     bf    6 -     6          0.000000000000         -1.424913022638         -0.867160354429
   0.3425250914D+01  0.1543289673D+00
   0.6239137298D+00  0.5353281423D+00
   0.1688554040D+00  0.4446345422D+00
   There are     3 symmetry adapted basis functions of A1  symmetry.
   There are     0 symmetry adapted basis functions of A2  symmetry.
   There are     1 symmetry adapted basis functions of B1  symmetry.
   There are     2 symmetry adapted basis functions of B2  symmetry.
   
   or
   
    AO basis set in the form of general basis input (Overlap normalization):
      1 0
 S   3 1.00       0.000000000000
      0.1508000000D 01 -0.1754110411D 00
      0.5129000000D 00 -0.4465363900D 00
      0.1362000000D 00  0.1295841966D 01
 S   3 1.00       0.000000000000
      0.2565000000D 01 -0.1043105923D 01
      0.1508000000D 01  0.1331478902D 01
      0.5129000000D 00  0.5613064585D 00
 S   1 1.00       0.000000000000
      0.4170000000D-01  0.1000000000D 01
 P   3 1.00       0.000000000000
      0.4859000000D 01 -0.9457549473D-01
      0.1219000000D 01  0.7434797586D 00
      0.4413000000D 00  0.3668143796D 00
 P   2 1.00       0.000000000000
      0.5725000000D 00 -0.8808640317D-01
      0.8300000000D-01  0.1028397037D 01
 P   1 1.00       0.000000000000
      0.2500000000D-01  0.1000000000D 01
 D   3 1.00       0.000000000000
      0.4195000000D 01  0.4857290090D-01
      0.1377000000D 01  0.5105223094D 00
      0.4828000000D 00  0.5730028106D 00
 D   1 1.00       0.000000000000
      0.1501000000D 00  0.1000000000D 01
 ****
      2 0
...
   */

  protected void readBasis() throws Exception {
    shells = new  Lst<int[]>();
    Lst<String[]> gdata = new  Lst<String[]>();
    int ac = 0;
    gaussianCount = 0;
    shellCount = 0;
    String lastAtom = "";
    String[] tokens;

    boolean doSphericalD = (calculationType != null && (calculationType
        .indexOf("5D") > 0));
    boolean doSphericalF = (calculationType != null && (calculationType
        .indexOf("7F") > 0));
    boolean doSphericalG = (calculationType != null && (calculationType
        .indexOf("9G") > 0));
    boolean doSphericalH = (calculationType != null && (calculationType
        .indexOf("11H") > 0));
    boolean doSphericalI = (calculationType != null && (calculationType
        .indexOf("13I") > 0));
    boolean doSphericalHighL = (doSphericalG ||doSphericalH ||doSphericalI);
    boolean doSpherical = (doSphericalD ||doSphericalF || doSphericalHighL);
    boolean isGeneral = (line.indexOf("general basis input") >= 0);
    if (isGeneral) {
      while (rd() != null && line.length() > 0) {
        shellCount++;
        tokens = getTokens();
        ac++;
        while (rd().indexOf("****") < 0) {
          int[] slater = new int[4];
          slater[0] = ac;
          tokens = getTokens();
          String oType = tokens[0];
          if (doSphericalF && oType.indexOf("F") >= 0 || doSphericalD
              && oType.indexOf("D") >= 0)
            slater[1] = BasisFunctionReader.getQuantumShellTagIDSpherical(oType);
          else
            slater[1] = BasisFunctionReader.getQuantumShellTagID(oType);

          int nGaussians = parseIntStr(tokens[1]);
          slater[2] = gaussianCount + 1; // or parseInt(tokens[7])
          slater[3] = nGaussians;
          if (debugging)
            Logger.debug("Slater " + shells.size() + " " + Escape.eAI(slater));
          shells.addLast(slater);
          gaussianCount += nGaussians;
          for (int i = 0; i < nGaussians; i++) {
            rd();
            line = PT.rep(line, "D ", "D+");
            tokens = getTokens();
            if (debugging)
              Logger.debug("Gaussians " + (i + 1) + " " + Escape.eAS(tokens, true));
            gdata.addLast(tokens);
          }
        }
      }
    } else {
      while (rd() != null && line.startsWith(" Atom")) {
        shellCount++;
        tokens = getTokens();
        int[] slater = new int[4];
        if (!tokens[1].equals(lastAtom))
          ac++;
        lastAtom = tokens[1];
        slater[0] = ac;
        String oType = tokens[4];
        if (doSpherical && (
            doSphericalF && oType.indexOf("F") >= 0
             || doSphericalD && oType.indexOf("D") >= 0
             || doSphericalHighL && (
                doSphericalG && oType.indexOf("G") >= 0
                || doSphericalH && oType.indexOf("H") >= 0
                || doSphericalI && oType.indexOf("I") >= 0
                )
            ))
          slater[1] = BasisFunctionReader.getQuantumShellTagIDSpherical(oType);
        else
          slater[1] = BasisFunctionReader.getQuantumShellTagID(oType);
        enableShell(slater[1]); // Gaussian is the reference
        int nGaussians = parseIntStr(tokens[5]);
        slater[2] = gaussianCount + 1; // or parseInt(tokens[7])
        slater[3] = nGaussians;
        shells.addLast(slater);
        gaussianCount += nGaussians;
        for (int i = 0; i < nGaussians; i++) {
          gdata.addLast(PT.getTokens(rd()));
        }
      }
    }
    if (ac == 0)
      ac = 1;
    gaussians = AU.newDouble2(gaussianCount);
    for (int i = 0; i < gaussianCount; i++) {
      tokens = gdata.get(i);
      gaussians[i] = new double[tokens.length];
      for (int j = 0; j < tokens.length; j++)
        gaussians[i][j] = (double) parseDoubleStr(tokens[j]);
    }
    Logger.info(shellCount + " slater shells read");
    Logger.info(gaussianCount + " gaussian primitives read");
  }
  
  /*

   Molecular Orbital Coefficients
                            1         2         3         4         5
                        (A1)--O   (A1)--O   (B2)--O   (A1)--O   (B1)--O
   EIGENVALUES --     -20.55790  -1.34610  -0.71418  -0.57083  -0.49821
   1 1   O  1S          0.99462  -0.20953   0.00000  -0.07310   0.00000
   2        2S          0.02117   0.47576   0.00000   0.16367   0.00000
   3        2PX         0.00000   0.00000   0.00000   0.00000   0.63927
   4        2PY         0.00000   0.00000   0.50891   0.00000   0.00000
   5        2PZ        -0.00134  -0.09475   0.00000   0.55774   0.00000
   6        3S          0.00415   0.43535   0.00000   0.32546   0.00000
   ...can have...
  16       10PX         0.00000   0.00000   0.00000   0.00000   0.00000

  but:

  105        4S        -47.27845  63.29565-100.44035   1.98362 -51.35328

  also G09  # B3LYP 6-31g sp gfprint pop(full,NO) 

     Natural Orbital Coefficients:
                           1         2         3         4         5
     Eigenvalues --     2.00000   2.00000   2.00000   2.00000   2.00000
   1 1   C  1S          0.03807   0.23941   0.96961   0.01811  -0.04011
   2        2S         -0.00048   0.00095   0.01728   0.01316  -0.02849


   */
  protected void readMolecularOrbitals() throws Exception {
    if (shells == null)
      return;
    Map<String, Object>[] mos = AU.createArrayOfHashtable(5);
    Lst<String>[] data = AU.createArrayOfArrayList(5);
    int nThisLine = 0;
    boolean isNOtype = line.contains("Natural Orbital"); //gfprint pop(full,NO)
    while (rd() != null && line.toUpperCase().indexOf("DENS") < 0) {
      String[] tokens;
      if (line.indexOf("eta Molecular Orbital Coefficients") >= 0) {
        addMOData(nThisLine, data, mos);
        nThisLine = 0;
        if (!filterMO())
          break;
      }
      if (line.indexOf("                    ") == 0) {
        addMOData(nThisLine, data, mos);
        if (isNOtype) {
          tokens = getTokens();
          nThisLine = tokens.length;
          tokens = PT.getTokens(rd());
        } else {
          tokens = PT.getTokens(rd());
          nThisLine = tokens.length;
        }
        for (int i = 0; i < nThisLine; i++) {
          mos[i] = new Hashtable<String, Object>();
          data[i] = new Lst<String>();
          String sym;
          if (isNOtype) {
            mos[i]
                .put("occupancy", Double.valueOf(PT.parseDouble(tokens[i + 2])));
          } else {
            sym = tokens[i];
            mos[i].put("symmetry", sym);
            if (sym.indexOf("O") >= 0)
              mos[i].put("occupancy", Double.valueOf(2));
            else if (sym.indexOf("V") >= 0)
              mos[i].put("occupancy", Double.valueOf(0));
          }
        }
        if (isNOtype)
          continue;
        line = rd().substring(21);
        tokens = getTokens();
        if (tokens.length != nThisLine)
          tokens = getStrings(line, nThisLine, 10);
        for (int i = 0; i < nThisLine; i++) {
          mos[i].put("energy", Double.valueOf(tokens[i]));
          //System.out.println(i + " gaussian energy " + mos[i].get("energy"));
        }
        continue;
      } else if (line.length() < 21
          || (line.charAt(5) != ' ' && !PT.isDigit(line.charAt(5)))) {
        continue;
      }
      try {
        // must fix "7D 0 " to be "7D0  " and "7F 0 " to be "7F0  " Jmol 13.0.RC6
        line = PT.rep(line, " 0 ", "0  ");
        tokens = getTokens();
        String type = tokens[tokens.length - nThisLine - 1].substring(1);
        if (PT.isDigit(type.charAt(0)))
          type = type.substring(1); // "11XX"
        if (!QS.isQuantumBasisSupported(type.charAt(0))
            && "XYZ".indexOf(type.charAt(0)) >= 0)
          type = (type.length() == 2 ? "D" : "F") + type;
        if (!QS.isQuantumBasisSupported(type.charAt(0)))
          continue;
        tokens = getStrings(line.substring(line.length() - 10 * nThisLine),
            nThisLine, 10);
        for (int i = 0; i < nThisLine; i++)
          data[i].addLast(tokens[i]);
      } catch (Exception e) {
        Logger.error("Error reading Gaussian file Molecular Orbitals at line: "
            + line);
        break;
      }
    }
    addMOData(nThisLine, data, mos);
    setMOData(moModelSet != asc.atomSetCount);
    moModelSet = asc.atomSetCount;
  }

  /* SAMPLE FREQUENCY OUTPUT */
  /*
   Harmonic frequencies (cm**-1), IR intensities (KM/Mole), Raman scattering
   activities (A**4/AMU), depolarization ratios for plane and unpolarized
   incident light, reduced masses (AMU), force constants (mDyne/A),
   and normal coordinates:
                       1                      2                      3
                      A1                     B2                     B1
   Frequencies --    64.6809                64.9485               203.8241
   Red. masses --     8.0904                 2.2567                 1.0164
   Frc consts  --     0.0199                 0.0056                 0.0249
   IR Inten    --     1.4343                 1.4384                15.8823
   Atom AN      X      Y      Z        X      Y      Z        X      Y      Z
   1   6     0.00   0.00   0.48     0.00  -0.05   0.23     0.01   0.00   0.00
   2   6     0.00   0.00   0.48     0.00  -0.05  -0.23     0.01   0.00   0.00
   3   1     0.00   0.00   0.49     0.00  -0.05   0.63     0.03   0.00   0.00
   4   1     0.00   0.00   0.49     0.00  -0.05  -0.63     0.03   0.00   0.00
   5   1     0.00   0.00  -0.16     0.00  -0.31   0.00    -1.00   0.00   0.00
   6  35     0.00   0.00  -0.16     0.00   0.02   0.00     0.01   0.00   0.00
   ##SNIP##
                      10                     11                     12
                      A1                     B2                     A1
   Frequencies --  2521.0940              3410.1755              3512.0957
   Red. masses --     1.0211                 1.0848                 1.2333
   Frc consts  --     3.8238                 7.4328                 8.9632
   IR Inten    --   264.5877               109.0525                 0.0637
   Atom AN      X      Y      Z        X      Y      Z        X      Y      Z
   1   6     0.00   0.00   0.00     0.00   0.06   0.00     0.00  -0.10   0.00
   2   6     0.00   0.00   0.00     0.00   0.06   0.00     0.00   0.10   0.00
   3   1     0.00   0.01   0.00     0.00  -0.70   0.01     0.00   0.70  -0.01
   4   1     0.00  -0.01   0.00     0.00  -0.70  -0.01     0.00  -0.70  -0.01
   5   1     0.00   0.00   1.00     0.00   0.00   0.00     0.00   0.00   0.00
   6  35     0.00   0.00  -0.01     0.00   0.00   0.00     0.00   0.00   0.00
   
   -------------------
   - Thermochemistry -
   -------------------
   */
  
  
  /**
   * Interprets the Harmonic frequencies section.
   *
   * <p>The vectors are added to a clone of the last read AtomSet.
   * Only the Frequencies, reduced masses, force constants and IR intensities
   * are set as properties for each of the frequency type AtomSet generated.
   * @param mustHave 
   * @param key 
   *
   * @throws Exception If no frequencies were encountered
   **/
  protected void readFrequencies(String key, boolean mustHave) throws Exception {
    discardLinesUntilContains2(key, ":");
    if (line == null && mustHave)
      throw (new Exception("No frequencies encountered"));
    line = rd();
    int ac = asc.getLastAtomSetAtomCount();
    String[][] data = new String[ac][], temp = null;
    int[] atomIndices = new int[ac];
    while (line != null && line.length() > 20 && line.indexOf("Temperature") < 0) {
      // we now have the line with the vibration numbers in them, but don't need it
      String[] symmetries = PT.getTokens(rd());
      discardLinesUntilContains(" Frequencies");
      if (line == null)
        return;
      isHighPrecision = (line.indexOf("---") > 0);
      if (isHighPrecision ? !allowHighPrecision : haveHighPrecision)
        return;
      if (isHighPrecision && !haveHighPrecision) {
        appendLoadNote("high precision vibrational modes enabled. Use filter 'NOHP' to disable.");
        haveHighPrecision = true;
      }
      if (temp == null)
        temp = new String[isHighPrecision ? 3 : 1][0];
      int width = (isHighPrecision ? 22 : 15);
      String[] frequencies = PT.getTokensAt(
          line, width);
      String[] red_masses = PT.getTokensAt(
          discardLinesUntilContains(isHighPrecision ? "Reduced masses" : "Red. masses"), width);
      String[] frc_consts = PT.getTokensAt(
          discardLinesUntilContains(isHighPrecision ? "Force constants" : "Frc consts"), width);
      String[] intensities = PT.getTokensAt(
          discardLinesUntilContains(isHighPrecision ? "IR Intensities" : "IR Inten"), width);
      int iAtom0 = asc.ac;
      int frequencyCount = frequencies.length;
      boolean[] ignore = new boolean[frequencyCount];
      for (int i = 0; i < frequencyCount; ++i) {
        ignore[i] = !doGetVibration(++vibrationNumber);
        if (ignore[i])
          continue;  
        asc.cloneAtomSetWithBonds(true);
        // set the properties
        String name = asc.setAtomSetFrequency(vibrationNumber, "Calculation " + calculationNumber, symmetries[i], frequencies[i], null);
        appendLoadNote("model " + asc.atomSetCount + ": " + name);
        namedSets.set(asc.iSet);
        asc.setAtomSetModelProperty("ReducedMass",
            red_masses[i]+" AMU");
        asc.setAtomSetModelProperty("ForceConstant",
            frc_consts[i]+" mDyne/A");
        asc.setAtomSetModelProperty("IRIntensity",
            intensities[i]+" KM/Mole");
      }
      discardLinesUntilContains(" Atom ");
      if (isHighPrecision) {
        while (true) {
          // one atom at a time.
          fillFrequencyData(iAtom0, 1, ac, ignore, false, 23, 10, null, 9, temp);
          // return of null data[0] indicates we have overrun the data.
          if (temp[0] == null)
            break;
        }
      } else {
        int nLines = 0;
        int nMin = frequencyCount * 3 + 1;
        while (true) {
          // wide format
          fillDataBlockFixed(temp, 0, 0, 0);
          if (temp[0].length < nMin)
            break;
          atomIndices[nLines] = Integer.valueOf(temp[0][0]).intValue() - 1; 
          data[nLines++] = temp[0];
        }
        fillFrequencyData(iAtom0, nLines, ac, ignore, true, 0, 0, atomIndices, 0, data);
      }
    }
  }
  
  void readDipoleMoment() throws Exception {
    //  X=     0.0000    Y=     0.0000    Z=    -1.2917  Tot=     1.2917
    String tokens[] = PT.getTokens(rd());
    if (tokens.length != 8)
      return;
    V3d dipole = V3d.new3((double) parseDoubleStr(tokens[1]),
        (double) parseDoubleStr(tokens[3]), (double) parseDoubleStr(tokens[5]));
    Logger.info("Molecular dipole for model " + asc.atomSetCount
        + " = " + dipole);
    asc.setCurrentModelInfo("dipole", dipole);
  }

  
  /* SAMPLE Mulliken Charges OUTPUT from G98 */
  /*
   Mulliken atomic charges:
   1
   1  C   -0.238024
   2  C   -0.238024
   ###SNIP###
   6  Br  -0.080946
   Sum of Mulliken charges=   0.00000
   */
  
  /**
   * Reads partial charges and assigns them only to the last atom set. 
   * @throws Exception When an I/O error or discardlines error occurs
   */
  // TODO this really should set the charges for the last nOrientations read
  // being careful about the dummy atoms...
  void readPartialCharges() throws Exception {
    rd();
    int ac = asc.ac;
    int i0 = asc.getLastAtomSetAtomIndex();
    Atom[] atoms = asc.atoms;
    for (int i = i0; i < ac; ++i) {
      // first skip over the dummy atoms
      while (atoms[i].elementNumber == 0)
        ++i;
      // assign the partial charge
      double charge = parseDoubleStr(PT.getTokens(rd())[2]);
      atoms[i].partialCharge = charge;
    }
    Logger.info("Mulliken charges found for Model " + asc.atomSetCount);
  }

//  Calculating GIAO nuclear magnetic shielding tensors.
//  SCF GIAO Magnetic shielding tensor (ppm):
//   1  H    Isotropic =    28.9213   Anisotropy =     3.2329
//    XX=    30.8519   YX=     1.2737   ZX=    -0.5281
//    XY=     0.2369   YY=    28.2130   ZY=    -1.9460
//    XZ=     1.2249   YZ=     1.5212   ZZ=    27.6990
//    Eigenvalues:    27.5052    28.1822    31.0766
  
  private void readCSATensors() throws Exception {
    rd();
    rd();
    while (line != null && line.indexOf("Isotropic") >= 0) {
      int iatom = parseIntAt(line,  0);
      String[] data = (rd() + rd() + rd()).split("=");
      addTensor(iatom, data);
      if (rd() != null && line.indexOf("Eigen") >= 0)
        rd();
    }
    appendLoadNote("NMR shift tensors are available for model=" + (asc.iSet + 1) + "\n using \"ellipsoids set 'csa'.");

  }

  private void addTensor(int iatom, String[] data) {
    int i0 = asc.getLastAtomSetAtomIndex();
    double[][] a = new double[3][3];
    for (int i = 0, p = 1; i < 3; i++) {
      for (int j = 0; j < 3; j++, p++) {
        a[i][j] = parseDoubleStr(data[p]); // XX YX ZX  XY YY ZY XZ YZ ZZ
      }
    }
    Tensor t = new Tensor().setFromAsymmetricTensor(a, "csa", "csa" + iatom);
    asc.atoms[i0 + iatom - 1].addTensor(t,  "csa",  false);
    System.out.println("calc Tensor " + t 
        + "calc isotropy=" + t.getInfo("isotropy") 
        + " anisotropy=" + t.getInfo("anisotropy") + "\n");
   
  }
  
  //    Total nuclear spin-spin coupling K (Hz): 
  //      1             2             3             4             5
  //  1  0.000000D+00
  //  2  0.158415D+02  0.000000D+00
  //  3 -0.187537D+00  0.169443D+02  0.000000D+00
  //  4 -0.318756D+00  0.151998D+02 -0.694557D-01  0.000000D+00
  //  5  0.536278D+00  0.491163D+01  0.463273D+00 -0.369586D+00  0.000000D+00
  //  6  0.716597D-01  0.955897D-01  0.473142D-01  0.117953D-01  0.198491D+02
  //  7 -0.833251D-01  0.547105D+00  0.491859D-01 -0.194792D+00  0.257557D+02
  //  8 -0.267188D+00  0.602187D+01  0.100268D-01  0.927527D-01 -0.348110D+02
  //  9 -0.468360D+00  0.147669D+02 -0.374664D+00 -0.642673D+00 -0.954001D-01
  //      6             7             8             9
  //  6  0.000000D+00
  //  7  0.154135D+01  0.000000D+00
  //  8  0.613165D+00 -0.455484D+00  0.000000D+00
  //  9  0.918779D-01  0.146931D-01 -0.143995D+01  0.000000D+00
  //  Total nuclear spin-spin coupling J (Hz): 
  //      1             2             3             4             5
  //  1  0.000000D+00
  //  2  0.124307D+03  0.000000D+00
  //  3 -0.585116D+01  0.132960D+03  0.000000D+00
  //  4 -0.994517D+01  0.119271D+03 -0.216701D+01  0.000000D+00
  //  5  0.420812D+01  0.969322D+01  0.363526D+01 -0.290011D+01  0.000000D+00
  //  6  0.223578D+01  0.750083D+00  0.147620D+01  0.368013D+00  0.155754D+03
  //  7 -0.259974D+01  0.429308D+01  0.153460D+01 -0.607750D+01  0.202103D+03
  //  8  0.113055D+01 -0.640836D+01 -0.424260D-01 -0.392462D+00  0.370452D+02
  //  9 -0.146128D+02  0.115875D+03 -0.116895D+02 -0.200514D+02 -0.748595D+00
  //      6             7             8             9
  //  6  0.000000D+00
  //  7  0.480901D+02  0.000000D+00
  //  8 -0.259447D+01  0.192728D+01  0.000000D+00
  //  9  0.286659D+01  0.458424D+00  0.609282D+01  0.000000D+00

  private void readCouplings() throws Exception {
    String type = (line.indexOf(" K ") >= 0 ? "K" : "J");
    //int i0 = asc.getLastAtomSetAtomIndex();
    int n = asc.getLastAtomSetAtomCount();
    double[][] data = new double[n][n];
    int k0 = 0;
    while (true) {
      rd();
      for (int i = k0; i < n; i++) {
        rd();
        String[] tokens = getTokens();
        for (int j = 1, nj = tokens.length; j < nj; j++) {
          double v = parseDoubleStr(tokens[j]);
          data[i][k0 + j - 1] = data[k0 + j - 1][i] = v;
        }
      }
      k0 += 5;
      if (k0 >= n)
        break;
    }
    System.out.println(data);
    asc.setModelInfoForSet("NMR_" + type + "_couplings", data, asc.iSet);
    if (type == "J") {
      asc.setAtomProperties("J", data, asc.iSet, false);
      appendLoadNote(
          "NMR J Couplings saved for model=" + (asc.iSet + 1) + " as property_J;\n use set measurementUnits \"+hz\" to measure them.");
    }
  }
  
}
