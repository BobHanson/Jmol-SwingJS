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


import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolAdapter;
import org.jmol.quantum.QS;
import org.jmol.util.Logger;

import java.io.IOException;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;

import java.util.Hashtable;

import java.util.Map;

/**
 * A reader for Q-Chem 2.1 and 3.2
 * Q-Chem  is a quantum chemistry program developed
 * by Q-Chem, Inc. (http://www.q-chem.com/)
 *
 * <p> Molecular coordinates, normal coordinates of
 * vibrations and MOs are read.
 * 
 *  <p> In order to get the output required for MO reading
 *  make sure that the $rem block has<p>
 * <code>print_general_basis TRUE<br>  print_orbitals TRUE</code>
 *
 * <p> This reader was developed from only a few
 * output files, and therefore, is not guaranteed to
 * properly read all Q-chem output. If you have problems,
 * please contact the author of this code, not the developers
 * of Q-chem.
 *
 * <p> This is a hacked version of Miguel's GaussianReader
 *
 * @author Rene P.F Kanters (rkanters@richmond.edu)
 * @version 1.1
 * 
 * @author Steven E. Wheeler (swheele2@ccqc.uga.edu)
 * @version 1.0
 * 
*/

public class QchemReader extends MOReader {
 
/** The number of the job being interpreted. */
  private int calculationNumber = 1;
  private boolean isFirstJob = true;

  private MOInfo[] alphas = null;
  private MOInfo[] betas = null;
  private int nBasis = 0;          // # of basis according to qchem

  
  @Override
  protected void initializeReader() {
    energyUnits = "au";   
  }
  
  /**
   * @return true if need to read new line
   * @throws Exception
   * 
   */
  @Override
  protected boolean checkLine() throws Exception {
    if (line.indexOf("Standard Nuclear Orientation") >= 0) {
      readAtoms();
      moData = null; // no MO data for this structure
      return true;
    }
    if (line.indexOf("Total energy") >= 0 
        || line.indexOf("total energy") >= 0 
        || line.indexOf("Energy is") >=0 ){
      if (line.indexOf("Excitation") == -1) readEnergy(); // Don't do excitation energies
      return true;
    }
    if (line.indexOf("Requested basis set is") >= 0) {
      readCalculationType();
      return true;
    }
    if (line.indexOf("VIBRATIONAL FREQUENCIES") >= 0) {
      readFrequencies();
      return true;
    }
    if (line.indexOf("Mulliken Net Atomic Charges") >= 0) {
      readPartialCharges();
      return true;
    }
    if (line.startsWith("Job ") || line.startsWith("Running Job")) {
      if (isFirstJob && line.startsWith("Running"))
        calculationNumber = 0; // qchem 4 always has Running.. also for first job
      calculationNumber++;
      isFirstJob = false;
      moData = null; // start 'fresh'
      return true;
    }
    if (line.indexOf("Basis set in general basis input format") >= 0) {
      if (moData == null) {
        // only read the first basis (not basis2)
        readBasis();
      }
      return true;
    }
    if (moData == null)
      return true;
    if (line.indexOf("Orbital Energies (a.u.) and Symmetries") >= 0) {
      readESym(true);
      return true;
    }
    if (line.indexOf("Orbital Energies (a.u.)") >= 0) {
      readESym(false);
      return true;
    }
    if (line.indexOf("MOLECULAR ORBITAL COEFFICIENTS") >= 0) {
      if (filterMO())
        readQchemMolecularOrbitals();
      return true;
    }
    return checkNboLine();
  }

  private void readCalculationType() {
    calculationType = line.substring(line.indexOf("set is") + 6).trim();
  }


  /* Q-chem 2.1 format:
         Standard Nuclear Orientation (Angstroms)
      I     Atom         X            Y            Z
   ----------------------------------------------------
      1      H       0.000000     0.000000     4.756791
  */

  private void readAtoms() throws Exception {
    asc.newAtomSet();
    setMOData(true);
    dFixed = fFixed = false;
    readLines(2);
    String[] tokens;
    while (rd() != null && !line.startsWith(" --")) {
      tokens = getTokens();
      if (tokens.length < 5)
        continue;
      String symbol = tokens[1];
      if (JmolAdapter.getElementNumber(symbol) > 0)
        addAtomXYZSymName(tokens, 2, symbol, null);
    }
    asc.setAtomSetModelProperty(SmarterJmolAdapter.PATH_KEY,
        "Job " + calculationNumber);
  }
  
  /**
   * Interprets the Harmonic frequencies section.
   * 
   * <p>
   * The vectors are added to a clone of the last read AtomSet. Only the
   * Frequencies, reduced masses, force constants and IR intensities are set as
   * properties for each of the frequency type AtomSet generated.
   * 
   * @throws Exception
   *           If no frequences were encountered
   * @throws IOException
   *           If an I/O error occurs
   **/
  private void readFrequencies() throws Exception, IOException {
    while (rd() != null && line.indexOf("STANDARD") < 0) {
      if (!line.startsWith(" Frequency:"))
        discardLinesUntilStartsWith(" Frequency:");
      String[] frequencies = getTokens();
      int frequencyCount = frequencies.length - 1;
      boolean[] ignore = new boolean[frequencyCount];
      int ac = asc.getLastAtomSetAtomCount();
      int iAtom0 = asc.ac;
      for (int i = 0; i < frequencyCount; ++i) {
        ignore[i] = !doGetVibration(++vibrationNumber);
        if (ignore[i])
          continue;
        asc.cloneLastAtomSet();
        asc.setAtomSetFrequency(vibrationNumber, 
            "Job " + calculationNumber, null, frequencies[i + 1], null);
      }

      // position to start reading the displacement vectors
      discardLinesUntilStartsWith("               X");
      fillFrequencyData(iAtom0, ac, ac, ignore, true, 0, 0, null, 0, null);
      discardLinesUntilBlank();
    }
  }

  private void readPartialCharges() throws Exception {
    readLines(3);
    Atom[] atoms = asc.atoms;
    int ac = asc.getLastAtomSetAtomCount();
    for (int i = 0; i < ac && rd() != null; ++i)
      atoms[i].partialCharge = parseFloatStr(getTokens()[2]);
  }
  
  private void readEnergy() {
    int ac = asc.getLastAtomSetAtomCount();
    String tokens[] = getTokens();
    String energyKey = "E("+tokens[0]+")";
    String energyString = tokens[tokens.length-1]; // value is last one
    asc.setAtomSetEnergy(energyString, parseFloatStr(energyString));
    asc.setAtomSetName(energyKey + " = " + energyString);
    asc.setModelInfoForSet("name", energyKey+" "+energyString, ac);
  }


/* SAMPLE BASIS OUTPUT for a cartesian basis set
 * if using pure the same shells are there, but nbasis is 18 (one less)
 * (because of only 5 d orbitals on O).

Basis set in general basis input format:
-----------------------------------------------------------------------
$basis
O    0
S    6    1.000000
   5.48467170E+03    1.83110000E-03 
   8.25234950E+02    1.39501000E-02 
   1.88046960E+02    6.84451000E-02 
   5.29645000E+01    2.32714300E-01 
   1.68975700E+01    4.70193000E-01 
   5.79963530E+00    3.58520900E-01 
SP   3    1.000000
   1.55396160E+01   -1.10777500E-01   7.08743000E-02 
   3.59993360E+00   -1.48026300E-01   3.39752800E-01 
   1.01376180E+00    1.13076700E+00   7.27158600E-01 
SP   1    1.000000
   2.70005800E-01    1.00000000E+00   1.00000000E+00 
D    1    1.000000
   8.00000000E-01    1.00000000E+00 
****
H    0
S    3    1.000000
   1.87311370E+01    3.34946000E-02 
   2.82539370E+00    2.34726950E-01 
   6.40121700E-01    8.13757330E-01 
S    1    1.000000
   1.61277800E-01    1.00000000E+00 
****
H    0
S    3    1.000000
   1.87311370E+01    3.34946000E-02 
   2.82539370E+00    2.34726950E-01 
   6.40121700E-01    8.13757330E-01 
S    1    1.000000
   1.61277800E-01    1.00000000E+00 
****
$end
-----------------------------------------------------------------------
 There are 8 shells and 19 basis functions

 * Since I don't know beforehand whether or not we use spherical or cartesians
 * I need to keep track of which shell and orbitals is where in the sdata
 * That way when I read the MOs I can see which shell goes where. 
 */

  private void readBasis() throws Exception {
    // initialize the 'global' variables
    moData = new Hashtable<String, Object>();
    int ac = 1;
    int shellCount = 0;
    int gaussianCount = 0;
    // local variables
    shells = new  Lst<int[]>();
    Lst<String[]> gdata = new  Lst<String[]>();
    String[] tokens;

    discardLinesUntilStartsWith("$basis");
    rd(); // read the atom line
    while (rd() != null) {  // read shell line
      if (line.startsWith("****")) {
        ac++;           // end of basis for an atom
        if (rd() != null && line.startsWith("$end")) break;
        continue; // atom line has been read
      }
      shellCount++;
      int[] slater = new int[4];
      tokens = getTokens();
      slater[0] = ac;
      slater[1] = BasisFunctionReader.getQuantumShellTagID(tokens[0]); // default cartesian
      slater[2] = gaussianCount + 1;
      int nGaussians = parseIntStr(tokens[1]);
      slater[3] = nGaussians;
      shells.addLast(slater);
      gaussianCount += nGaussians;
      for (int i = 0; i < nGaussians; i++) {
        gdata.addLast(PT.getTokens(rd()));
      }
    }
    // now rearrange the gaussians (direct copy from GaussianReader)
    gaussians = AU.newFloat2(gaussianCount);
    for (int i = 0; i < gaussianCount; i++) {
      tokens = gdata.get(i);
      gaussians[i] = new float[tokens.length];
      for (int j = 0; j < tokens.length; j++)
        gaussians[i][j] = parseFloatStr(tokens[j]);
    }
    if (debugging) {
      Logger.debug(shellCount + " slater shells read");
      Logger.debug(gaussianCount + " gaussian primitives read");
    }
    discardLinesUntilStartsWith(" There are");
    tokens = getTokens();
    //nShell = parseInt(tokens[2]);
    nBasis = parseIntStr(tokens[5]);
  }

  // since the orbital coefficients don't show the symmetry, I will read them here
  /* 
   * sample output for an unrestricted calculation
   * 
  --------------------------------------------------------------
             Orbital Energies (a.u.) and Symmetries
  --------------------------------------------------------------
  Warning : Irrep of orbital(   1) could not be determined
  ....
  Warning : Irrep of orbital(  86) could not be determined
  
  Alpha MOs, Unrestricted
  -- Occupied --                  
  -10.446 -10.446 -10.446 -10.446 -10.412 -10.412  -1.100  -0.998
  0 xxx   0 xxx   0 xxx   0 xxx   0 xxx   0 xxx   1 A1g   1 E1u                
  ....
  -0.611  -0.571  -0.569  -0.512  -0.479
  1 A2u   1 E2g   1 E2g   1 E1g   1 E1g                                        
  -- Virtual --                   
  -0.252  -0.226  -0.102  -0.076  -0.049  -0.039  -0.015  -0.006
  1 E2u   1 E2u   2 A1g   1 B2g   0 xxx   0 xxx   2 E2g   2 E2g                
  ....
  4.427
  5 B1u                                                                        
  Warning : Irrep of orbital(   1) could not be determined
  ....
  Warning : Irrep of orbital(  57) could not be determined
  
  Beta MOs, Unrestricted
  -- Occupied --                  
  -10.442 -10.442 -10.441 -10.441 -10.413 -10.413  -1.088  -0.978
  ....
  -0.577  -0.569  -0.566  -0.473
  1 A2u   2 E2g   2 E2g   1 E1g                                                
  -- Virtual --                   
  -0.416  -0.214  -0.211  -0.100  -0.053  -0.047  -0.039  -0.008
  1 E1g   1 E2u   1 E2u   3 A1g   1 B2g   0 xxx   0 xxx   3 E2g                
  ....
  4.100   4.433
  10 E2g   6 B1u                                                                
  --------------------------------------------------------------
  

   * 
   * For a restricted open shell
   * 
  --------------------------------------------------------------
             Orbital Energies (a.u.) and Symmetries
  --------------------------------------------------------------
  Warning : Irrep of orbital(   1) could not be determined
  ....
  Warning : Irrep of orbital(  86) could not be determined
  
  Alpha MOs, Restricted
  -- Doubly Occupied --           
  -10.446 -10.446 -10.445 -10.445 -10.413 -10.413  -1.099  -0.996
  0 xxx   0 xxx   0 xxx   0 xxx   0 xxx   0 xxx   1 A1g   1 E1u                
  ....
  -0.609  -0.572  -0.570  -0.481
  1 A2u   2 E2g   2 E2g   1 E1g                                                
  -- Singly Occupied (Occupied) --
  -0.507
  1 E1g                                                                        
  -- Virtual --                   
  -0.248  -0.230  -0.102  -0.076  -0.049  -0.039  -0.015  -0.007
  1 E2u   1 E2u   2 A1g   1 B2g   0 xxx   0 xxx   3 E2g   3 E2g                
  ....
  4.427
  6 B1u                                                                        
  
  Beta MOs, Restricted
  -- Doubly Occupied --           
  -10.443 -10.443 -10.442 -10.442 -10.413 -10.413  -1.088  -0.980
  0 xxx   0 xxx   0 xxx   0 xxx   0 xxx   0 xxx   1 A1g   1 E1u                
  ....
  -0.578  -0.569  -0.566  -0.470
  1 A2u   2 E2g   2 E2g   1 E1g                                                
  -- Singly Occupied (Vacant) --  
  -0.421
  1 E1g                                                                        
  -- Virtual --                   
  -0.215  -0.212  -0.100  -0.055  -0.047  -0.038  -0.008  -0.004
  1 E2u   1 E2u   2 A1g   1 B2g   0 xxx   0 xxx   3 E2g   3 E2g                
  ....
  4.433
  6 B1u                                                                        
  --------------------------------------------------------------

   * 
   * For a restricted one : only need to read the alpha ones....
   * 
  --------------------------------------------------------------
             Orbital Energies (a.u.) and Symmetries
  --------------------------------------------------------------
  
  Alpha MOs, Restricted
  -- Occupied --                  
  -10.187 -10.187 -10.187 -10.186 -10.186 -10.186  -0.847  -0.740
  1 A1g   1 E1u   1 E1u   1 E2g   1 E2g   1 B1u   2 A1g   2 E1u                
  ....
  -0.360  -0.340  -0.340  -0.247  -0.246
  1 A2u   3 E2g   3 E2g   1 E1g   1 E1g                                        
  -- Virtual --                   
  0.004   0.004   0.091   0.145   0.145   0.165   0.182   0.182
  1 E2u   1 E2u   4 A1g   4 E1u   4 E1u   1 B2g   4 E2g   4 E2g                
  ....
  4.668
  10 B1u                                                                        
  
  Beta MOs, Restricted
  -- Occupied --                  
  -10.187 -10.187 -10.187 -10.186 -10.186 -10.186  -0.847  -0.740
  1 A1g   1 E1u   1 E1u   1 E2g   1 E2g   1 B1u   2 A1g   2 E1u                
  ....
  -0.360  -0.340  -0.340  -0.247  -0.246
  1 A2u   3 E2g   3 E2g   1 E1g   1 E1g                                        
  -- Virtual --                   
  0.004   0.004   0.091   0.145   0.145   0.165   0.182   0.182
  1 E2u   1 E2u   4 A1g   4 E1u   4 E1u   1 B2g   4 E2g   4 E2g                
  ....
  4.668
  10 B1u                                                                        
  --------------------------------------------------------------

   * 
   */
  private void readESym(boolean haveSym) throws Exception {
    alphas = new MOInfo[nBasis];
    betas = new MOInfo[nBasis];
    MOInfo[] moInfos;
    int ne = 0; // number of electrons for a particular series of orbitals
    boolean readBetas = false;

    discardLinesUntilStartsWith(" Alpha");
    String[] tokens = getTokens(); // initialize tokens for later as well
    moInfos = alphas;
    for (int e = 0; e < 2; e++) { // do for A and B electrons
      int nMO = 0;
      while (rd() != null) { // will break out of loop
        if (line.startsWith(" -- ")) {
          ne = 0;
          if (line.indexOf("Vacant") < 0) {
            if (line.indexOf("Occupied") > 0)
              ne = 1;
          }
          rd();
        }
        if (line.startsWith(" -------")) {
          e = 2; // pretend I did read beta whether it happened or not
          break; // done....
        }
        int nOrbs = getTokens().length;
        if (nOrbs == 0 || line.startsWith(" Warning")) {
          discardLinesUntilStartsWith(" Beta"); // now the beta ones.
          readBetas = true;
          moInfos = betas;
          break;
        }
        if (haveSym)
          tokens = PT.getTokens(rd());
        for (int i = 0, j = 0; i < nOrbs; i++, j += 2) {
          MOInfo info = new MOInfo();
          info.ne = ne;
          if (haveSym)
            info.moSymmetry = tokens[j] + tokens[j + 1] + " ";
          moInfos[nMO] = info;
          nMO++;
        }
      }
    }
    if (!readBetas)
      betas = alphas; // no beta symmetry info: Restricted no sym
  }

/* Restricted orbitals cartesian see H2O-B3LYP-631Gd.out:
 * 
                        RESTRICTED (RHF) MOLECULAR ORBITAL COEFFICIENTS
                         1         2         3         4         5         6
 eigenvalues:        -19.138    -0.998    -0.517    -0.372    -0.291     0.063
   1  O     s        0.99286  -0.20950   0.00000  -0.08810   0.00000   0.10064
   2  O     s        0.02622   0.46921   0.00000   0.17726   0.00000  -0.11929
   3  O     px       0.00000   0.00000   0.51744   0.00000   0.00000   0.00001
   4  O     py       0.00000   0.00000   0.00000   0.00000   0.64458   0.00000
   5  O     pz      -0.00110  -0.12769   0.00000   0.55181   0.00000   0.28067
   6  O     s        0.01011   0.43952   0.00000   0.41043   0.00000  -1.25784
   7  O     px       0.00000   0.00000   0.26976   0.00000   0.00000   0.00001
   8  O     py       0.00000   0.00000   0.00000   0.00000   0.50605   0.00000
   9  O     pz       0.00000  -0.06065   0.00000   0.37214   0.00000   0.47747
  10  O     dxx     -0.00777   0.01878   0.00000   0.00088   0.00000   0.04509
  11  O     dxy      0.00000   0.00000   0.00000   0.00000   0.00000   0.00000
  12  O     dyy     -0.00772  -0.01094   0.00000  -0.00026   0.00000   0.05804
  13  O     dxz      0.00000   0.00000  -0.04127   0.00000   0.00000   0.00000
  14  O     dyz      0.00000   0.00000   0.00000   0.00000  -0.03544   0.00000
  15  O     dzz     -0.00775   0.01607   0.00000  -0.05242   0.00000   0.02731
  16  H 1   s        0.00037   0.13914  -0.23744  -0.14373   0.00000   0.09628
  17  H 1   s       -0.00103   0.00645  -0.14196  -0.11428   0.00000   0.96908
  18  H 2   s        0.00037   0.13914   0.23744  -0.14373   0.00000   0.09627
  19  H 2   s       -0.00103   0.00645   0.14195  -0.11428   0.00000   0.96905
                         7         8         9        10
 eigenvalues:          0.148     0.772     0.861     0.891
   1  O     s        0.00000   0.00000   0.03777   0.00000
....

 * and for pure d, H2O-B3LYP-631Gd_pure.out:
                        RESTRICTED (RHF) MOLECULAR ORBITAL COEFFICIENTS
                         1         2         3         4         5         6
 eigenvalues:        -19.130    -0.997    -0.516    -0.371    -0.290     0.065
   1  O     s        0.99505  -0.21173   0.00000  -0.08338   0.00000   0.08960
   2  O     s        0.02790   0.46512   0.00000   0.18751   0.00000  -0.15229
   3  O     px       0.00000   0.00000   0.51708   0.00000   0.00000   0.00001
   4  O     py       0.00000   0.00000   0.00000   0.00000   0.64424   0.00000
   5  O     pz      -0.00169  -0.12726   0.00000   0.55128   0.00000   0.28063
   6  O     s       -0.01316   0.46728   0.00000   0.34668   0.00000  -1.09467
   7  O     px       0.00000   0.00000   0.26985   0.00000   0.00000   0.00001
   8  O     py       0.00000   0.00000   0.00000   0.00000   0.50641   0.00000
   9  O     pz       0.00261  -0.06385   0.00000   0.37987   0.00000   0.46045
  10  O     d 1      0.00000   0.00000   0.00000   0.00000   0.00000   0.00000
  11  O     d 2      0.00000   0.00000   0.00000   0.00000  -0.03550   0.00000
  12  O     d 3     -0.00004   0.00813   0.00000  -0.03535   0.00000  -0.01590
  13  O     d 4      0.00000   0.00000  -0.04131   0.00000   0.00000   0.00000
  14  O     d 5     -0.00027   0.01732   0.00000   0.00046   0.00000  -0.00725
  15  H 1   s        0.00029   0.13911  -0.23753  -0.14352   0.00000   0.09663
  16  H 1   s        0.00298   0.00119  -0.14211  -0.10113   0.00000   0.93864
  17  H 2   s        0.00029   0.13911   0.23753  -0.14352   0.00000   0.09663
  18  H 2   s        0.00298   0.00119   0.14210  -0.10113   0.00000   0.93860

 * section finishes with an empty line containing only a space.
 * 
 * Since I could not determine from the basis information whether a shell
 * was cartesian or pure, I need to check this from the first time I go
 * through the AO's used
 */

  private boolean dFixed = false;
  private boolean fFixed = false;

  // all we do here is list the orbital types found in the file
  // in the order that corresponds to Jmol's order. getDFMap will set up the array. 
  
  private static String DC_LIST = QS.CANONICAL_DC_LIST;
  private static String DS_LIST = "D3    D4    D2    D5    D1";
  private static String FC_LIST = QS.CANONICAL_FC_LIST;
  private static String FS_LIST = "F4    F5    F3    F6    F2    F7    F1";
  
  private void readQchemMolecularOrbitals() throws Exception {

    /* 
     * Jmol:   XX, YY, ZZ, XY, XZ, YZ 
     * qchem: dxx, dxy, dyy, dxz, dyz, dzz : VERIFIED
     * Jmol:   d0, d1+, d1-, d2+, d2-
     * qchem: d 1=d2-, d 2=d1-, d 3=d0, d 4=d1+, d 5=d2+
     * Jmol:   XXX, YYY, ZZZ, XYY, XXY, XXZ, XZZ, YZZ, YYZ, XYZ
     * qchem: fxxx, fxxy, fxyy, fyyy, fxxz, fxyz, fyyz, fxzz, fyzz, fzzz
     * Jmol:   f0, f1+, f1-, f2+, f2-, f3+, f3-
     * qchem: f 1=f3-, f 2=f2-, f 3=f1-, f 4=f0, f 5=f1+, f 6=f2+, f 7=f3+
     * 
     */
    String orbitalType = getTokens()[0]; // is RESTRICTED or ALPHA
    alphaBeta = "A"; // (orbitalType.equals("RESTRICTED") ? "" : "A");
    readMOs(orbitalType.equals("RESTRICTED"), alphas);
    if (orbitalType.equals("ALPHA")) { // we also have BETA orbitals....
      discardLinesUntilContains("BETA");
      alphaBeta = "B";
      readMOs(false, betas);
    }
    boolean isOK = true;
    if (dList.length() > 0) {
      if (dSpherical) 
        isOK = getDFMap("DS", dList, QS.DS, DS_LIST, 2);
      else
        isOK = getDFMap("DC", dList, QS.DC, DC_LIST, 3);
      if (!isOK) {
        Logger.error("atomic orbital order is unrecognized -- skipping reading of MOs. dList=" + dList);
        shells = null;
      }
    }
    if (fList.length() > 0) {
      if (fSpherical) 
        isOK = getDFMap("FS", fList, QS.FS, FS_LIST, 2);
      else
        isOK = getDFMap("FC", fList, QS.FC, FC_LIST, 3);
      if (!isOK) {
        Logger.error("atomic orbital order is unrecognized -- skipping reading of MOs. fList=" + fList);
        shells = null;
      }
    }
    setMOData(shells == null);
    shells = null; // clears mo data upon next model loading
  }

  String dList = "";
  String fList = "";
  boolean dSpherical = false;
  boolean fSpherical = false;
  
  private int readMOs(boolean restricted, MOInfo[] moInfos) throws Exception {
    Map<String, Object>[] mos = AU.createArrayOfHashtable(6); // max 6 MO's per line
    float[][] mocoef = AU.newFloat2(6); // coefficients for each MO
    int[] moid = new int[6]; // mo numbers
    String[] tokens, energy;
    int nMOs = 0;

    while (rd().length() > 2) {
      tokens = getTokens();
      int nMO = tokens.length; // number of MO columns
      energy = PT.getTokens(rd().substring(13));
      for (int i = 0; i < nMO; i++) {
        moid[i] = parseIntStr(tokens[i]) - 1;
        mocoef[i] = new float[nBasis];
        mos[i] = new Hashtable<String, Object>();
      }
      for (int i = 0, pt = 0; i < nBasis; i++) {
        tokens = PT.getTokens(rd());
        String s = line.substring(12, 17).trim(); // collect the shell labels
        char ch = s.charAt(0);
        switch (ch) {
        case 'd':
          s = s.substring(s.length() - 3).toUpperCase();
          if (s.startsWith("D ")) {
            if (!dFixed)
              fixSlaterTypes(QS.DC, QS.DS);
            s = "D" + s.charAt(2);
            dSpherical = true;
          }
          if (dList.indexOf(s) < 0)
            dList += s + " ";
          dFixed = true;
          break;
        case 'f':
          s = s.substring(s.length() - 3).toUpperCase();
          if (s.startsWith("F ")) {
            if (!fFixed)
              fixSlaterTypes(QS.FC, QS.FS);
            s = "F" + s.charAt(2);
            fSpherical = true;
          }
          if (fList.indexOf(s) < 0)
            fList += s + " ";
          fFixed = true;
          break;
        default:
          if (!QS.isQuantumBasisSupported(ch))
            continue;
          break;
        }
        for (int j = tokens.length - nMO, k = 0; k < nMO; j++, k++)
          mocoef[k][pt] = parseFloatStr(tokens[j]);
        pt++;
      }
      // we have all the info we need 
      for (int i = 0; i < nMO; i++) {
        MOInfo moInfo = moInfos[moid[i]];
        mos[i].put("energy", Float.valueOf(energy[i]));
        mos[i].put("coefficients", mocoef[i]);
        String label = alphaBeta;
        int ne = moInfo.ne;
        if (restricted)
          ne = alphas[moid[i]].ne + betas[moid[i]].ne;
        mos[i].put("occupancy", Float.valueOf(ne));
        switch (ne) {
        case 2:
          label = "AB";
          break;
        case 1:
          break;
        case 0:
          if (restricted)
            label = "V";
          else
            label = "V" + label; // keep spin information for the orbital
          break;
        }
        mos[i].put("symmetry", moInfo.moSymmetry + label + "("
            + (moid[i] + 1) + ")");
        orbitals.addLast(mos[i]);
      }
      nMOs += nMO;
    }
    return nMOs;
  }
  
  // inner class moInfo for storing occupancy and symmetry info from the
  // orbital energies and symmetrys block
  protected class MOInfo {
    protected int ne = 0;      // 0 or 1
    protected String moSymmetry = "";
  }
}
