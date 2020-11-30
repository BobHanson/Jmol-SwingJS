/* $RCSfile$
 * $Author: nicove $
 * $Date: 2006-08-30 13:20:20 -0500 (Wed, 30 Aug 2006) $
 * $Revision: 5447 $
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

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;

import java.util.Hashtable;
import java.util.Properties;

import java.util.Map;
import java.util.Map.Entry;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolAdapter;
import javajs.util.BS;
import org.jmol.quantum.QS;
import org.jmol.util.Elements;
import org.jmol.util.Logger;

/**
 * A reader for NWChem 4.6
 * NWChem is a quantum chemistry program developed at
 * Pacific Northwest National Laboratory.
 * See http://www.nwchem-sw.org/index.php/NWChem_Documentation
 * for orbital plotting, one needs to use the following switches:
 * 
 * print "final vectors" "final vectors analysis"
 *
 * <p>AtomSets will be generated for
 * output coordinates in angstroms,
 * energy gradients with vector information of the gradients,
 * and frequencies with an AtomSet for every separate frequency containing
 * vector information of the vibrational mode.
 * <p>Note that the different modules give quite different formatted output
 * so it is not certain that all modules will be properly interpreted.
 * Most testing has been done with the SCF and DFT tasks.
 * 
 **/

public class NWChemReader extends MOReader {

  /**
   * The number of the task begin interpreted.
   * <p>Used for the construction of the 'path' for the atom set.
   */
  private int taskNumber = 1;

  /**
   * The number of equivalent atom sets.
   * <p>Needed to associate identical properties to multiple atomsets
   */
  private int equivalentAtomSets = 0;

  /**
   * The type of energy last calculated.
   */
  private String energyKey = "";
  /**
   * The last calculated energy value.
   */
  private String energyValue = "";

  // need to remember a bit of the state of what was read before...
  private boolean converged;
  private boolean haveEnergy;
  private boolean haveAt;
  private boolean inInput;

  private Lst<String> atomTypes;
  private Map<String, Lst<String>> htMOs = new Hashtable<String, Lst<String>>();

  @Override
  protected void initializeReader() {
    calculationType = "(NWCHEM)"; // normalization is different for NWCHEM
  }

  /**
    * @return true if need to read new line
    * @throws Exception
    * 
    */
  @Override
  protected boolean checkLine() throws Exception {

    
    if (line.trim().startsWith("NWChem")) {
      // currently only keep track of whether I am in the input module or not.
      inInput = (line.indexOf("NWChem Input Module") >= 0);
      if (inInput) {
        checkMOs();
      }
    }

    if (line.startsWith("          Step")) {
      init();
      return true;
    }
    if (line.indexOf("  wavefunction    = ") >= 0) {
      calculationType = line.substring(line.indexOf("=") + 1).trim()
          + "(NWCHEM)";
      moData.put("calculationType", calculationType);
      return true;
    }
    if (line.indexOf("Total") >= 0) {
      readTotal();
      return true;
    }
    if (line.indexOf("@") >= 0) {
      readAtSign();
      return true;
    }
    if (line.startsWith(" Task  times")) {
      init();
      taskNumber++; // starting a new task
      return true;
    }
    if (line.startsWith("      Optimization converged")) {
      converged = true;
      return true;
    }
    if (line.startsWith("      Symmetry information")) {
      readSymmetry();
      return true;
    }
    if (line.indexOf("Output coordinates in ") >= 0) {
      String thisLine = line;
      if (!htMOs.isEmpty())
        checkMOs();
      if (!doGetModel(++modelNumber, null))
        return checkLastModel();
      equivalentAtomSets++;
      readAtoms(thisLine);
      return true;
    }
    if (line.indexOf("Vibrational analysis") >= 0) {
      readFrequencies();
      return true;
    }

    if (!doProcessLines)
      return true;

//    if (line.indexOf("ENERGY GRADIENTS") >= 0) {
//      // abandoned - we don't need to see gradients as vibrations
//      equivalentAtomSets++;
//      readGradients();
//      return true;
//    }

    if (line.startsWith("  Mulliken analysis of the total density")) {
      // only do this if I have read an atom set in this task/step
      if (equivalentAtomSets != 0)
        readPartialCharges();
      return true;
    }

    if (line.contains("Basis \"ao basis\"") && doReadMolecularOrbitals) {
      return readBasis();
    }
    
    if (line.contains("Molecular Orbital Analysis")) {
      if (equivalentAtomSets != 0)
        readMOs();
      return true;
    }
//    if (!readROHFonly && line.contains("Final MO vectors")) {
//      if (equivalentAtomSets == 0)
//        return true;
//      return readMolecularOrbitalVectors();
//    }

    return true;
  }

  @Override
  protected void finalizeSubclassReader() throws Exception {
    checkMOs();
    finalizeReaderASCR();
  }

  private void init() {
    haveEnergy = false;
    haveAt = false;
    converged = false;
    inInput = false;
    equivalentAtomSets = 0;
  }

  /**
   * 
   * @param key
   * @param value
   * @param nAtomSets NOT USED
   */
  private void setEnergies(String key, String value, int nAtomSets) {
    energyKey = key;
    energyValue = value;
    setProps(energyKey, energyValue,
        equivalentAtomSets);
    setNames(energyKey + " = " + energyValue,
        null, equivalentAtomSets);
    asc.setAtomSetEnergy(value, parseFloatStr(value));
    haveEnergy = true;
  }

  private void setEnergy(String key, String value) {
    energyKey = key;
    energyValue = value;
    asc.setAtomSetModelProperty(energyKey, energyValue);
    asc.setAtomSetName(energyKey + " = " + energyValue);
    haveEnergy = true;
  }

  /**
   * Read the symmetry information and set the property.
   * @throws Exception If an error occurs.
   */
  private void readSymmetry() throws Exception {
    String tokens[] = PT.getTokens(readLines(3));
    setProps("Symmetry group name",
        tokens[tokens.length - 1], equivalentAtomSets);
  }

  /**
   * Interpret a line starting with a line with "Total" in it.
   * <p>Determine whether it reports the energy, if so set the property and name(s)
   */
  private void readTotal() {
    String tokens[] = getTokens();
    try {
      if (tokens[2].startsWith("energy")) {
        // in an optimization an energy is reported in a follow up step
        // that energy I don't want so only set the energy once
        if (!haveAt)
          setEnergies("E(" + tokens[1] + ")", tokens[tokens.length - 1],
              equivalentAtomSets);
      }
    } catch (Exception e) {
      // ignore any problems in dealing with the line
    }
  }

  private void readAtSign() throws Exception {
    if (line.charAt(2) == 'S') {
      // skip over the line with the --- in it
      if (readLines(2) == null)
        return;
    }
    String tokens[] = getTokens();
    if (!haveEnergy) { // if didn't already have the energies, set them now
      setEnergies("E", tokens[2], equivalentAtomSets);
    } else {
      // @ comes after gradients, so 'reset' the energies for additional
      // atom sets that may be been parsed.
      setEnergies(energyKey, energyValue, equivalentAtomSets);
    }
    setProps("Step", tokens[1],
        equivalentAtomSets);
    haveAt = true;
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

  // NWChem Output coordinates
  /*
    Output coordinates in angstroms (scale by  1.889725989 to convert to a.u.)

    No.       Tag          Charge          X              Y              Z
   ---- ---------------- ---------- -------------- -------------- --------------
      1 O                    8.0000     0.00000000     0.00000000     0.14142136
      2 H                    1.0000     0.70710678     0.00000000    -0.56568542
      3 H                    1.0000    -0.70710678     0.00000000    -0.56568542

        Atomic Mass 
  */

  /**
   * Reads the output coordinates section into a new AtomSet.
   * @throws Exception If an error occurs.
   **/
  private void readAtoms(String thisLine) throws Exception {
    float scale = (thisLine.indexOf("angstroms") < 0 ? ANGSTROMS_PER_BOHR : 1);
    readLines(3); // skip blank line, titles and dashes
    String tokens[];
    haveEnergy = false;
    asc.newAtomSet();
    asc.setAtomSetModelProperty(SmarterJmolAdapter.PATH_KEY, "Task "
        + taskNumber
        + (inInput ? SmarterJmolAdapter.PATH_SEPARATOR + "Input"
            : SmarterJmolAdapter.PATH_SEPARATOR + "Geometry"));
    atomTypes = new  Lst<String>();
    while (rd() != null && line.length() > 0) {
      tokens = getTokens(); // get the tokens in the line
      if (tokens.length < 6)
        break; // if don't have enough of them: done
      String name = fixTag(tokens[1]);
      setAtomCoordScaled(null, tokens, 3, scale).atomName = name;
      atomTypes.addLast(name);
    }
    // only if was converged, use the last energy for the name and properties
    if (converged) {
      setEnergy(energyKey, energyValue);
      asc.setAtomSetModelProperty("Step", "converged");
    } else if (inInput) {
      asc.setAtomSetName("Input");
    }
  }

  // NWChem Gradients output
  // The 'atom' is really a Tag (as above)
  /*
                           UHF ENERGY GRADIENTS

      atom               coordinates                        gradient
                   x          y          z           x          y          z
     1 O       0.000000   0.000000   0.267248    0.000000   0.000000  -0.005967
     2 H       1.336238   0.000000  -1.068990   -0.064647   0.000000   0.002984
     3 H      -1.336238   0.000000  -1.068990    0.064647   0.000000   0.002984

  */
  // NB one could consider removing the previous read structure since that
  // must have been the input structure for the optimizition?
  /**
   * Reads the energy gradients section into a new AtomSet.
   * 
   * <p>
   * One could consider not adding a new AtomSet for this, but just adding the
   * gradient vectors to the last AtomSet read (if that was indeed the same
   * nuclear arrangement).
   * 
   * @throws Exception
   *         If an error occurs.
   **/
  private void readGradients() throws Exception {
    readLines(3); // skip blank line, titles and dashes
    String tokens[];
    asc.newAtomSet();
    if (equivalentAtomSets > 1) {
      Properties p = (Properties) asc.getAtomSetAuxiliaryInfoValue(
          asc.iSet - 1, "modelProperties");
      if (p != null)
        asc.setCurrentModelInfo("modelProperties", p.clone());
    }
    asc.setAtomSetModelProperty("vector", "gradient");
    asc.setAtomSetModelProperty(SmarterJmolAdapter.PATH_KEY, "Task "
        + taskNumber + SmarterJmolAdapter.PATH_SEPARATOR + "Gradients");
    float f = ANGSTROMS_PER_BOHR;
    while (rd() != null && line.length() > 0) {
      tokens = getTokens(); // get the tokens in the line
      if (tokens.length < 8)
        break; // make sure I have enough tokens
      Atom atom = setAtomCoordScaled(null, tokens, 2, f);
      atom.atomName = fixTag(tokens[1]);

      // Keep gradients in a.u. (larger value that way)
      // need to multiply with -1 so the direction is in the direction the
      // atom needs to move to lower the energy
      asc.addVibrationVector(atom.index, -parseFloatStr(tokens[5]),
          -parseFloatStr(tokens[6]), -parseFloatStr(tokens[7]));
    }
  }

  // SAMPLE FREQUENCY OUTPUT
  // First the structure. The atom column has real element names (not the tags)
  // units of X Y and Z in a.u.
  /*
   ---------------------------- Atom information ----------------------------
   atom    #        X              Y              Z            mass
   --------------------------------------------------------------------------
   O        1  9.5835700E-02  3.1863970E-07  0.0000000E+00  1.5994910E+01
   H        2 -9.8328438E-01  1.5498085E+00  0.0000000E+00  1.0078250E+00
   H        3 -9.8328460E-01 -1.5498088E+00  0.0000000E+00  1.0078250E+00
   --------------------------------------------------------------------------

   */
  // NB another header but with subhead (Frequencies expressed in cm-1)
  // is in the output before this....
  /*
   -------------------------------------------------
   NORMAL MODE EIGENVECTORS IN CARTESIAN COORDINATES
   -------------------------------------------------
   (Projected Frequencies expressed in cm-1)

   1           2           3           4           5           6
   
   P.Frequency        0.00        0.00        0.00        0.00        0.00        0.00
   
   1     0.03302     0.00000     0.00000     0.00000    -0.02102     0.23236
   2     0.08894     0.00000     0.00000     0.00000     0.22285     0.00752
   3     0.00000     0.00000     0.25004     0.00000     0.00000     0.00000
   4     0.52206     0.00000     0.00000     0.00000    -0.33418     0.13454
   5     0.42946     0.00000     0.00000     0.00000     0.00480    -0.06059
   6     0.00000     0.99611     0.00000     0.00000     0.00000     0.00000
   7    -0.45603     0.00000     0.00000     0.00000     0.29214     0.33018
   8     0.42946     0.00000     0.00000     0.00000     0.00480    -0.06059
   9     0.00000     0.00000     0.00000     0.99611     0.00000     0.00000

   7           8           9
   
   P.Frequency     1484.76     3460.15     3551.50
   
   1    -0.06910    -0.04713     0.00000
   2     0.00000     0.00000    -0.06994
   3     0.00000     0.00000     0.00000
   4     0.54837     0.37401    -0.38643
   5     0.39688    -0.58189     0.55498
   6     0.00000     0.00000     0.00000
   7     0.54837     0.37402     0.38641
   8    -0.39688     0.58191     0.55496
   9     0.00000     0.00000     0.00000



   ----------------------------------------------------------------------------
   Normal Eigenvalue ||    Projected Derivative Dipole Moments (debye/angs)
   Mode   [cm**-1]  ||      [d/dqX]             [d/dqY]           [d/dqZ]
   ------ ---------- || ------------------ ------------------ -----------------
   1        0.000 ||       0.159               2.123             0.000
   2        0.000 ||       0.000               0.000             2.480
   3        0.000 ||       0.000               0.000            -0.044
   4        0.000 ||       0.000               0.000             2.480
   5        0.000 ||      -0.101              -0.015             0.000
   6        0.000 ||       1.116              -0.303             0.000
   7     1484.764 ||       2.112               0.000             0.000
   8     3460.151 ||       1.877               0.000             0.000
   9     3551.497 ||       0.000               3.435             0.000
   ----------------------------------------------------------------------------



   
   
   ----------------------------------------------------------------------------
   Normal Eigenvalue ||           Projected Infra Red Intensities
   Mode   [cm**-1]  || [atomic units] [(debye/angs)**2] [(KM/mol)] [arbitrary]
   ------ ---------- || -------------- ----------------- ---------- -----------
   1        0.000 ||    0.196398           4.531       191.459      10.742
   2        0.000 ||    0.266537           6.149       259.833      14.578
   3        0.000 ||    0.000084           0.002         0.081       0.005
   4        0.000 ||    0.266537           6.149       259.833      14.578
   5        0.000 ||    0.000452           0.010         0.441       0.025
   6        0.000 ||    0.057967           1.337        56.509       3.170
   7     1484.764 ||    0.193384           4.462       188.520      10.577
   8     3460.151 ||    0.152668           3.522       148.828       8.350
   9     3551.497 ||    0.511498          11.801       498.633      27.976
   ----------------------------------------------------------------------------
   */

  /**
   * Reads the AtomSet and projected frequencies in the frequency section.
   *
   * <p>Attaches the vibration vectors of the projected frequencies to
   * duplicates of the atom information in the frequency section.
   * @throws Exception If an error occurs.
   **/
  private void readFrequencies() throws Exception {
    int firstFrequencyAtomSetIndex = asc.atomSetCount;
    int firstVibrationNumber = vibrationNumber;
    String path = "Task " + taskNumber + SmarterJmolAdapter.PATH_SEPARATOR
        + "Frequencies";

    // position myself to read the atom information, i.e., structure
    discardLinesUntilContains("Atom information");
    readLines(2);
    asc.newAtomSet();
    String tokens[];
    while (rd() != null && line.indexOf("---") < 0) {
      tokens = getTokens();
      setAtomCoordScaled(null, tokens, 2, ANGSTROMS_PER_BOHR).atomName = fixTag(tokens[0]);
    }

    discardLinesUntilContains("(Projected Frequencies expressed in cm-1)");
    readLines(3); // step over the line with the numbers

    boolean firstTime = true;
    BS bsIgnore = new BS();
    while (rd() != null && line.indexOf("P.Frequency") >= 0) {
      tokens = PT.getTokensAt(line, 12);
      int frequencyCount = tokens.length;
      int iAtom0 = asc.ac;
      int ac = asc.getLastAtomSetAtomCount();
      if (firstTime)
        iAtom0 -= ac;
      //System.out.println("freq "+ firstTime + " " + iAtom0 + " " + ac);
      boolean[] ignore = new boolean[frequencyCount];
      // clone the last atom set nFreq-1 times the first time, later nFreq times.

      // assign the frequency values to each atomset's name and property
      for (int i = 0; i < frequencyCount; ++i) {
        ignore[i] = (tokens[i].equals("0.00") || !doGetVibration(++vibrationNumber));
        if (ignore[i]) {
          bsIgnore.set(vibrationNumber);
          continue;
        }
        if (!firstTime)
          asc.cloneLastAtomSet();
        firstTime = false;
        asc.setAtomSetFrequency(vibrationNumber, path, null, tokens[i], null);
      }
      readLines(1);
      fillFrequencyData(iAtom0, ac, ac, ignore, false, 0, 0, null, 0, null);
      readLines(3);
    }

    // now set the names and properties of the atomsets associated with
    // the frequencies
    // NB this is not always there: try/catch and possibly set freq value again  
    try {
      discardLinesUntilContains("Infra Red Intensities");
      readLines(2);
      int idx = firstFrequencyAtomSetIndex;
      for (int i = firstVibrationNumber; i < vibrationNumber; ++i) {
        if (rd() == null)
          return;
        if (bsIgnore.get(i))
          continue;
        tokens = getTokens();
        int iset = asc.iSet;
        asc.iSet = idx++;
        asc.setAtomSetModelProperty("IRIntensity", tokens[3] + " au");
        asc.iSet = iset;
      }
    } catch (Exception e) {
      System.out.println("nwchem infra red issue" + e);
      // If exception was thrown, don't do anything here...
    }
  }

  /**
   * Reads partial charges and assigns them only to the last atom set. 
   * @throws Exception When an I/O error or discardlines error occurs
   */
  void readPartialCharges() throws Exception {
    String tokens[];
    readLines(4);
    int ac = asc.ac;
    int i0 = asc.getLastAtomSetAtomIndex();
    Atom[] atoms = asc.atoms;
    for (int i = i0; i < ac; ++i) {
      // first skip over the dummy atoms (not sure whether that really is needed..)
      while (atoms[i].elementNumber == 0)
        ++i;
      do {
        // assign the partial charge
        if (rd() == null || line.length() < 3)
          return;
        tokens = getTokens();
      } while (tokens[0].indexOf(".") >= 0);
      atoms[i].partialCharge = parseIntStr(tokens[2]) - parseFloatStr(tokens[3]);
    }
  }

  /**
   * Returns a modified identifier for a tag, so that the element can be
   * determined from it in the {@link Atom}.
   *<p>
   * The result is that a tag that started with Bq (case insensitive) will be
   * renamed to have the Bq removed and '-Bq' appended to it. <br>
   * A tag consisting only of Bq (case insensitive) will return X. This can
   * happen in a frequency analysis.
   * 
   * @param tag
   *          the tag to be modified
   * @return a possibly modified tag
   **/
  private String fixTag(String tag) {
    // make sure that Bq's are not interpreted as boron
    if (tag.equalsIgnoreCase("bq"))
      return "X";
    if (tag.toLowerCase().startsWith("bq"))
      tag = tag.substring(2) + "-Bq";
    return "" + Character.toUpperCase(tag.charAt(0))
        + (tag.length() == 1 ? "" : "" + Character.toLowerCase(tag.charAt(1)));
  }

  int nBasisFunctions;

  /*

                        Basis "ao basis" -> "ao basis" (cartesian)
                        -----
    C (Carbon)
    ----------
              Exponent  Coefficients 
         -------------- ---------------------------------------------------------
    1 S  6.66500000E+03  0.000692
    1 S  1.00000000E+03  0.005329
    1 S  2.28000000E+02  0.027077
    1 S  6.47100000E+01  0.101718
    1 S  2.10600000E+01  0.274740
    1 S  7.49500000E+00  0.448564
    1 S  2.79700000E+00  0.285074
    1 S  5.21500000E-01  0.015204

    2 S  6.66500000E+03 -0.000146
    2 S  1.00000000E+03 -0.001154
    2 S  2.28000000E+02 -0.005725
    2 S  6.47100000E+01 -0.023312
    2 S  2.10600000E+01 -0.063955
    2 S  7.49500000E+00 -0.149981
    2 S  2.79700000E+00 -0.127262
    2 S  5.21500000E-01  0.544529

    3 S  1.59600000E-01  1.000000

    4 P  9.43900000E+00  0.038109
    4 P  2.00200000E+00  0.209480
    4 P  5.45600000E-01  0.508557

    5 P  1.51700000E-01  1.000000

    6 D  5.50000000E-01  1.000000

    F (Fluorine)
    ------------
              Exponent  Coefficients 
         -------------- ---------------------------------------------------------
    1 S  1.47100000E+04  0.000721
    1 S  2.20700000E+03  0.005553
    1 S  5.02800000E+02  0.028267
    1 S  1.42600000E+02  0.106444
    1 S  4.64700000E+01  0.286814
    1 S  1.67000000E+01  0.448641
    1 S  6.35600000E+00  0.264761
    1 S  1.31600000E+00  0.015333

    2 S  1.47100000E+04 -0.000165
    2 S  2.20700000E+03 -0.001308
    2 S  5.02800000E+02 -0.006495
    2 S  1.42600000E+02 -0.026691
    2 S  4.64700000E+01 -0.073690
    2 S  1.67000000E+01 -0.170776
    2 S  6.35600000E+00 -0.112327
    2 S  1.31600000E+00  0.562814

    3 S  3.89700000E-01  1.000000

    4 P  2.26700000E+01  0.044878
    4 P  4.97700000E+00  0.235718
    4 P  1.34700000E+00  0.508521

    5 P  3.47100000E-01  1.000000

    6 D  1.64000000E+00  1.000000

    H (Hydrogen)
    ------------
              Exponent  Coefficients 
         -------------- ---------------------------------------------------------
    1 S  1.30100000E+01  0.019685
    1 S  1.96200000E+00  0.137977
    1 S  4.44600000E-01  0.478148

    2 S  1.22000000E-01  1.000000

    3 P  7.27000000E-01  1.000000



   Summary of "ao basis" -> "ao basis" (cartesian)

   * 
   */

  private static String DS_LIST = "d2-   d1-   d0    d1+   d2+";
  private static String FS_LIST = "f3-   f2-   f1-   f0    f1+   f2+   f3+";
//  private static String FS_LIST = "f3+   f2+   f1+   f0    f1-   f2-   f3-";
  private static String DC_LIST = "DXX   DXY   DXZ   DYY   DYZ   DZZ";
  private static String FC_LIST = "XXX   XXY   XXZ   XYY   XYZ   XZZ   YYY   YYZ   YZZ   ZZZ";

  private boolean readBasis() throws Exception {
    // F only; G, H, I not enabled. Just set getDFMap to enable those
    gaussianCount = 0;
    shellCount = 0;
    nBasisFunctions = 0;
    boolean isD6F10 = (line.indexOf("cartesian") >= 0);
    if (isD6F10) {
      getDFMap("DC", DC_LIST, QS.DC, QS.CANONICAL_DC_LIST, 3);
      getDFMap("FC", FC_LIST, QS.FC, QS.CANONICAL_FC_LIST, 3);
    } else {
      getDFMap("DS", DS_LIST, QS.DS, QS.CANONICAL_DS_LIST, 2);
      getDFMap("FS", FS_LIST, QS.FS, QS.CANONICAL_FS_LIST, 2);
    }
    shells = new  Lst<int[]>();
    Map<String, Lst<Lst<Object[]>>> atomInfo = new Hashtable<String, Lst<Lst<Object[]>>>();
    String atomSym = null;
    Lst<Lst<Object[]>> atomData = null;
    Lst<Object[]> shellData = null;
    while (line != null) {
      int nBlankLines = 0;
      while (line.length() < 3 || line.charAt(2) == ' ') {
        shellData = new  Lst<Object[]>();
        rd();
        if (line.length() < 3)
          nBlankLines++;
      }
      if (nBlankLines >= 2)
        break;
      if (parseIntStr(line) == Integer.MIN_VALUE) {
        // next atom type
        atomSym = getTokens()[0];
        if (atomSym.length() > 2)
          atomSym = JmolAdapter.getElementSymbol(Elements.elementNumberFromName(atomSym));
        atomData = new  Lst<Lst<Object[]>>();
        atomInfo.put(atomSym, atomData);
        rd();
        rd();
        continue;
      }
      while (line != null && line.length() > 3) {
        String[] tokens = getTokens();
        Object[] o = new Object[] { tokens[1],
            new float[] { parseFloatStr(tokens[2]), parseFloatStr(tokens[3]) } };
        shellData.addLast(o);
        rd();
      }
      atomData.addLast(shellData);
    }

    int nD = (isD6F10 ? 6 : 5);
    int nF = (isD6F10 ? 10 : 7);
    Lst<float[]> gdata = new  Lst<float[]>();
    for (int i = 0; i < atomTypes.size(); i++) {
      atomData = atomInfo.get(atomTypes.get(i));
      int nShells = atomData.size();
      for (int ishell = 0; ishell < nShells; ishell++) {
        shellCount++;
        shellData = atomData.get(ishell);
        int nGaussians = shellData.size();
        String type = (String) shellData.get(0)[0];
        switch (type.charAt(0)) {
        case 'S':
          nBasisFunctions += 1;
          break;
        case 'P':
          nBasisFunctions += 3;
          break;
        case 'D':
          nBasisFunctions += nD;
          break;
        case 'F':
          nBasisFunctions += nF;
          break;
        }
        int[] slater = new int[4];
        slater[0] = i + 1;
        slater[1] = (isD6F10 ? BasisFunctionReader.getQuantumShellTagID(type)
            : BasisFunctionReader.getQuantumShellTagIDSpherical(type));
        slater[2] = gaussianCount + 1;
        slater[3] = nGaussians;
        shells.addLast(slater);
        for (int ifunc = 0; ifunc < nGaussians; ifunc++)
          gdata.addLast((float[]) shellData.get(ifunc)[1]);
        gaussianCount += nGaussians;
      }
    }
    gaussians = AU.newFloat2(gaussianCount);
    for (int i = 0; i < gaussianCount; i++)
      gaussians[i] = gdata.get(i);
    Logger.info(gaussianCount + " Gaussians read");
    return true;
  }

  /*
   * 
                       ROHF Final Molecular Orbital Analysis
                       -------------------------------------

  Vector    9  Occ=2.000000D+00  E=-1.152419D+00  Symmetry=a1
              MO Center=  1.0D-19,  2.0D-16, -4.4D-01, r^2= 2.3D+00
   Bfn.  Coefficient  Atom+Function         Bfn.  Coefficient  Atom+Function  
  ----- ------------  ---------------      ----- ------------  ---------------
    77      0.180031   6 C  s                47      0.180031   4 C  s         
    32      0.178250   3 C  s                92      0.178250   7 C  s         
    62      0.177105   5 C  s                 2      0.174810   1 C  s         

  Vector   10  Occ=2.000000D+00  E=-1.030565D+00  Symmetry=b2
              MO Center=  2.1D-18,  1.8D-17, -3.6D-01, r^2= 2.9D+00
   Bfn.  Coefficient  Atom+Function         Bfn.  Coefficient  Atom+Function  
  ----- ------------  ---------------      ----- ------------  ---------------
    92      0.216716   7 C  s                32     -0.216716   3 C  s         
    47     -0.205297   4 C  s                77      0.205297   6 C  s         

  ...
  Vector   39  Occ=0.000000D+00  E= 6.163870D-01  Symmetry=b2
              MO Center= -9.0D-31,  2.4D-14, -1.7D-01, r^2= 5.7D+00
   Bfn.  Coefficient  Atom+Function         Bfn.  Coefficient  Atom+Function  
  ----- ------------  ---------------      ----- ------------  ---------------
    68      2.509000   5 C  py               39     -2.096777   3 C  pz        
    99      2.096777   7 C  pz               54     -1.647235   4 C  pz        
    84      1.647235   6 C  pz                8     -1.004675   1 C  py        
    98     -0.870389   7 C  py               38     -0.870389   3 C  py        
    83      0.691421   6 C  py               53      0.691421   4 C  py        


  center of mass
   * 
   */

  
  int moCount;
  
  // get just the LAST MO definition of each type.
  private boolean readMOs() throws Exception {
    Lst<String> lines = new Lst<String>();
    htMOs.put(line, lines);
    lines.addLast(line);
    int nblank = 0;
    while (nblank != 2 && rd() != null) {
      lines.addLast(line);
      if (line.length() < 2)
        nblank++;
      else
        nblank = 0;
    }
    return true;
  }

  private void checkMOs() throws Exception {
    if (shells == null)
      return;
    for (Entry<String, Lst<String>> entry : htMOs.entrySet()) {
      line = entry.getKey();
      alphaBeta = line.substring(0, line.indexOf("Final")).trim() + " ";
      int moCount = 0;
      if (!filterMO())
        continue;
      Lst<String> list = entry.getValue();
      int n = list.size();
      Logger.info(line);
      for (int i = 3; i < n; i++) {
        while (i < n && ((line = list.get(i)).length() < 2
            || line.charAt(1) != 'V'))
          i++;
        if (i == n)
          break;
        line = line.replace('=', ' ');
        //  Vector    9  Occ=2.000000D+00  E=-1.152419D+00  Symmetry=a1
        String[] tokens = getTokens();
        float occupancy = parseFloatStr(tokens[3]);
        float energy = parseFloatStr(tokens[5]);
        String symmetry = (tokens.length > 7 ? tokens[7] : null);
        Map<String, Object> mo = new Hashtable<String, Object>();
        mo.put("occupancy", Float.valueOf(occupancy));
        mo.put("energy", Float.valueOf(energy));
        if (symmetry != null)
          mo.put("symmetry", symmetry);
        float[] coefs = null;
        setMO(mo);
        mo.put("type", alphaBeta + (++moCount));
        coefs = new float[nBasisFunctions];
        mo.put("coefficients", coefs);
        i += 3;
        //    68      2.509000   5 C  py               39     -2.096777   3 C  pz        
        while ((line = list.get(++i)) != null && line.length() > 3) {
          tokens = getTokens();
          coefs[parseIntStr(tokens[0]) - 1] = parseFloatStr(tokens[1]);
          int pt = tokens.length / 2;
          if (pt == 5 || pt == 6)
            coefs[parseIntStr(tokens[pt]) - 1] = parseFloatStr(tokens[pt + 1]);
        }
      }
    }
    energyUnits = "a.u.";
    setMOData(true);
    //shells = null;
    htMOs.clear();
  }

  /*
   * 
   * OLD format? I do not have any examples of these files.

                                 Final MO vectors
                                 ----------------


  global array: scf_init: MOs[1:80,1:80],  handle: -1000 

            1           2           3           4           5           6  
       ----------- ----------- ----------- ----------- ----------- -----------
   1       0.98048    -0.02102     0.00000    -0.00000    -0.00000     0.06015
  ...
  80       0.00006    -0.00005    -0.00052     0.00006     0.00000     0.01181

            7           8           9          10          11          12  
       ----------- ----------- ----------- ----------- ----------- -----------
   1      -0.00000    -0.00000     0.02285    -0.00000    -0.00000     0.00000
   
   */
//  private boolean readMolecularOrbitalVectors() throws Exception {
//
//    if (shells == null)
//      return true;
//    Map<String, Object>[] mos = null;
//    List<String>[] data = null;
//    int iListed = 0;
//    int ptOffset = -1;
//    int fieldSize = 0;
//    int nThisLine = 0;
//    readLines(5);
//    boolean isBeta = false;
//    boolean betaOnly = !filterMO();
//    while (readLine() != null) {
//      if (parseIntStr(line) != iListed + 1) {
//        if (line.indexOf("beta") < 0)
//          break;
//        alphaBeta = "beta ";
//        if (!filterMO())
//          break;
//        isBeta = true;
//        iListed = 0;
//        readLine();
//        continue;
//      }
//      
//      readLine();
//      String[] tokens = getTokens();
//      if (debugging) {
//        Logger.debug(tokens.length + " --- " + line);
//      }
//      nThisLine = tokens.length;
//      ptOffset = 6;
//      fieldSize = 12;
//      mos = ArrayUtil.createArrayOfHashtable(nThisLine);
//      data = ArrayUtil.createArrayOfArrayList(nThisLine);
//      for (int i = 0; i < nThisLine; i++) {
//        mos[i] = new Hashtable<String, Object>();
//        data[i] = new  List<String>();
//      }
//
//      while (readLine() != null && line.length() > 0)
//        for (int i = 0, pt = ptOffset; i < nThisLine; i++, pt += fieldSize)
//          data[i].addLast(line.substring(pt, pt + fieldSize).trim());
//
//      for (int iMo = 0; iMo < nThisLine; iMo++) {
//        float[] coefs = new float[data[iMo].size()];
//        int iCoeff = 0;
//        while (iCoeff < coefs.length) {
//          coefs[iCoeff] = parseFloatStr(data[iMo].get(iCoeff));
//          iCoeff++;
//        }
//        mos[iMo].put("coefficients", coefs);
//        mos[iMo].put("type", alphaBeta + " "            + (++iListed));
//        ++moCount;
//        Map<String, Object> mo = (moInfo == null ? null : moInfo.get(Integer
//            .valueOf(isBeta ? -iListed : iListed)));
//        if (mo != null)
//          mos[iMo].putAll(mo);
//        if (!betaOnly || isBeta)
//          setMO(mos[iMo]);
//      }
//      line = "";
//    }
//    energyUnits = "a.u.";
//    setMOData(false);
//    return true;
//  }

  /*
------------------------------------------------------------
EAF file 0: "./CeO2-ECP-RHF.aoints.0" size=19922944 bytes
------------------------------------------------------------
               write      read    awrite     aread      wait
               -----      ----    ------     -----      ----
     calls:       38        15         0       555       555
   data(b): 1.99e+07  7.86e+06  0.00e+00  2.91e+08
   time(s): 5.03e-02  3.82e-03  0.00e+00  1.17e-01  5.40e-05
rate(mb/s): 3.96e+02  2.06e+03  0.00e+00* 2.49e+03*
------------------------------------------------------------
* = Effective rate.  Full wait time used for read and write.

   */
  
  private boolean purging;
  @Override
  public String rd() throws Exception {
    RL();
    if (!purging && line != null && line.startsWith("--")) {
      purging = true;
      if (rd().indexOf("EAF") == 0) {
        rd();
        discardLinesUntilStartsWith("--");
        purging = false;
        return rd();
      };
      discardLinesUntilStartsWith("*");
      rd();
      purging = false;
      RL();
    }
    return line;
 }


}
