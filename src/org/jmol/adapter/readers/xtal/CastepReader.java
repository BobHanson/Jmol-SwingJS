/* $RCSfile$
 * $Author: hansonr $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

/* 
 * Copyright (C) 2009  Joerg Meyer, FHI Berlin
 *
 * Contact: meyer@fhi-berlin.mpg.de
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

package org.jmol.adapter.readers.xtal;

import javajs.util.DF;
import javajs.util.Lst;
import javajs.util.M4;
import javajs.util.PT;


import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Atom;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import javajs.util.P3;
import org.jmol.util.Tensor;
import javajs.util.V3;


/**
 * CASTEP (http://www.castep.org) .cell file format relevant section of .cell
 * file are included as comments below
 * 
 * preliminary .castep, .phonon frequency reader 
 * -- hansonr@stolaf.edu 9/2011 
 * -- Many thanks to Keith Refson for his assistance with this implementation 
 * -- atom's mass is encoded as bfactor 
 * -- FILTER options include 
 *      "q=n" where n is an integer 
 *      "q={1/4 1/4 0}" 
 *      "q=ALL"
 * -- for non-simple fractions, you must use the exact form of the wavevector description: 
 * -- load "xxx.phonon" FILTER "q=(-0.083333 0.083333 0.500000) 
 * -- for simple fractions, you can also just specify SUPERCELL {a b c} where 
 *    the number of cells matches a given wavevector  -- SUPERCELL {4 4 1}, for example 
 * 
 * note: following was never implemented?
 * 
 * -- following this with ".1" ".2" etc. gives first, second, third, etc. occurance: 
 * -- load "xxx.phonon" FILTER "q=1.3" .... 
 * -- load "xxx.phonon" FILTER "{0 0 0}.3" ....  
 * 
 * 
 * @author Joerg Meyer, FHI Berlin 2009 (meyer@fhi-berlin.mpg.de)
 * @version 1.2
 */

public class CastepReader extends AtomSetCollectionReader {

  private static final float RAD_TO_DEG = (float) (180.0 / Math.PI);

  private String[] tokens;

  private boolean isPhonon;
  private boolean isTS;
  private boolean isOutput;
  private boolean isCell;

  private float a, b, c, alpha, beta, gamma;
  private V3[] abc = new V3[3];
  
  private int ac;
  private P3[] atomPts;

  private boolean havePhonons = false;
  private String lastQPt;
  private int qpt2;
  private V3 desiredQpt;
  private String desiredQ;

  private String chargeType = "MULL";

  private boolean isAllQ;

  private boolean haveCharges;

  private String tsType;

  @Override
  public void initializeReader() throws Exception {
    if (filter != null) {
      chargeType = getFilter("CHARGE=");
      if (chargeType != null && chargeType.length() > 4)
        chargeType = chargeType.substring(0, 4);
      filter = filter.replace('(', '{').replace(')', '}');
      filter = PT.rep(filter, "  ", " ");
      isAllQ = checkFilterKey("Q=ALL");
      tsType = getFilter("TSTYPE=");
      if (!isAllQ && filter.indexOf("{") >= 0)
        setDesiredQpt(filter.substring(filter.indexOf("{")));
      filter = PT.rep(filter, "-PT", "");
    }
    continuing = readFileData();
  }

  private void setDesiredQpt(String s) {
    desiredQpt = new V3();
    desiredQ = "";
    float num = 1;
    float denom = 1;
    int ipt = 0;
    int xyz = 0;
    boolean useSpace = (s.indexOf(',') < 0);
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
      case '{':
        ipt = i + 1;
        num = 1;
        denom = 1;
        break;
      case '/':
        num = parseFloatStr(s.substring(ipt, i));
        ipt = i + 1;
        denom = 0;
        break;
      case ',':
      case ' ':
      case '}':
        if (c == '}')
          desiredQ = s.substring(0, i + 1);
        else if ((c == ' ') != useSpace)
          break;
        if (denom == 0) {
          denom = parseFloatStr(s.substring(ipt, i));
        } else {
          num = parseFloatStr(s.substring(ipt, i));
        }
        num /= denom;
        switch (xyz++) {
        case 0:
          desiredQpt.x = num;
          break;
        case 1:
          desiredQpt.y = num;
          break;
        case 2:
          desiredQpt.z = num;
          break;
        }
        denom = 1;
        if (c == '}')
          i = s.length();
        ipt = i + 1;
        break;
      }
    }
    Logger.info("Looking for q-pt=" + desiredQpt);
  }

  private boolean readFileData() throws Exception {
    while (tokenizeCastepCell() > 0)
      if (tokens.length >= 2 && tokens[0].equalsIgnoreCase("%BLOCK")) {
        Logger.info(line);
        /*
        %BLOCK LATTICE_ABC
        ang
        16.66566792 8.33283396  16.82438907
        90.0    90.0    90.0
        %ENDBLOCK LATTICE_ABC
        */
        if (tokens[1].equalsIgnoreCase("LATTICE_ABC")) {
          readLatticeAbc();
          continue;
        }
        /*
        %BLOCK LATTICE_CART
        ang
        16.66566792 0.0   0.0
        0.0   8.33283396  0.0
        0.0   0.0   16.82438907
        %ENDBLOCK LATTICE_CART
        */
        if (tokens[1].equalsIgnoreCase("LATTICE_CART")) {
          readLatticeCart();
          continue;
        }

        /* coordinates are set immediately */
        /*
        %BLOCK POSITIONS_FRAC
        Pd         0.0 0.0 0.0
        %ENDBLOCK POSITIONS_FRAC
        */
        if (tokens[1].equalsIgnoreCase("POSITIONS_FRAC")) {
          setFractionalCoordinates(true);
          readPositionsFrac();
          continue;
        }
        /*
        %BLOCK POSITIONS_ABS
        ang
        Pd         0.00000000         0.00000000       0.00000000 
        %ENDBLOCK POSITIONS_ABS
        */
        if (tokens[1].equalsIgnoreCase("POSITIONS_ABS")) {
          setFractionalCoordinates(false);
          readPositionsAbs();
          continue;
        }
      }
    if (isPhonon || isOutput || isTS) {
      if (isPhonon) {
        isTrajectory = (desiredVibrationNumber <= 0);
        asc.allowMultiple = false;
      }
      return true; // use checkLine
    }
    return false;
  }

  @Override
  protected boolean checkLine() throws Exception {
    // only for .phonon, castep output, or other BEGIN HEADER type files
    if (isOutput) {
      if (line.contains("Real Lattice(A)")) {
        readOutputUnitCell();
      } else if (line.contains("Fractional coordinates of atoms")) {
        if (doGetModel(++modelNumber, null)) {
          readOutputAtoms();
        }
      } else if (doProcessLines && 
          (line.contains("Atomic Populations (Mulliken)") 
              || line.contains("Hirshfield Charge (e)"))) {
        readOutputCharges();
      } else if (doProcessLines && line.contains("Born Effective Charges")) {
        readOutputBornChargeTensors();
      } else if (line.contains("Final energy ")) { // not "Final energy, E"
        readEnergy(3, null);
      } else if (line.contains("Dispersion corrected final energy*")) {
        readEnergy(5, null);
      } else if (line.contains("Total energy corrected")) {
        readEnergy(8, null);
      }
      return true;
    }

    // phonon only from here
    if (line.contains("<-- E")) {
      readPhononTrajectories();
      return true;
    }
    if (line.indexOf("Unit cell vectors") == 1) {
      readPhononUnitCell();
      return true;
    }
    if (line.indexOf("Fractional Co-ordinates") >= 0) {
      readPhononFractionalCoord();
      return true;
    }
    if (line.indexOf("q-pt") >= 0) {
      readPhononFrequencies();
      return true;
    }
    return true;
  }

  /*
        Real Lattice(A)                      Reciprocal Lattice(1/A)
   2.6954645   2.6954645   0.0000000        1.1655107   1.1655107  -1.1655107
   2.6954645   0.0000000   2.6954645        1.1655107  -1.1655107   1.1655107
   0.0000000   2.6954645   2.6954645       -1.1655107   1.1655107   1.1655107
   */

  private void readOutputUnitCell() throws Exception {
    applySymmetryAndSetTrajectory();
    asc.newAtomSetClear(false);
    setFractionalCoordinates(true);
    abc = read3Vectors(false);
    setLatticeVectors();
  }

  /*
            x  Element    Atom        Fractional coordinates of atoms  x
            x            Number           u          v          w      x
            x----------------------------------------------------------x
            x  Si           1         0.000000   0.000000   0.000000   x
            x  Si           2         0.250000   0.250000   0.250000   x

   */
  private void readOutputAtoms() throws Exception {
    readLines(2);
    while (rd().indexOf("xxx") < 0) {
      Atom atom = new Atom();
      tokens = getTokens();
      atom.elementSymbol = tokens[1];
      atom.atomName = tokens[1] + tokens[2];
      asc.addAtomWithMappedName(atom);
      setAtomCoordTokens(atom, tokens, 3);
    }

  }

  private void readEnergy(int pt, String prefix) throws Exception {
    if (isTrajectory)
      applySymmetryAndSetTrajectory();
    tokens = getTokens();
    try {
      Double energy = Double.valueOf(Double.parseDouble(tokens[pt]));
      asc.setAtomSetName(prefix + "Energy = " + energy + " eV");
      asc.setAtomSetEnergy("" + energy, energy.floatValue());
      asc.setCurrentModelInfo("Energy", energy);
    } catch (Exception e) {
      appendLoadNote("CASTEP Energy could not be read: " + line);
    }

// this change, 4/16/2013, broke the optimization reader
// one should never start a new atom set without actually
// adding new atoms. The reader will apply symmetry in the
// finalization stage. 
//
//    /*    
//    is better to do this also here
//    in case the output is only a 
//    geometry optimization and not 
//    both volume and geometry
//     */
//    applySymmetryAndSetTrajectory();
//    asc.newAtomSetClear(false);
//    setLatticeVectors();
  }
  
  private void readPhononTrajectories() throws Exception {
    if (!isTS) // force this only for .phonon, not .ts
      isTrajectory = (desiredVibrationNumber <= 0);
    if (isTrajectory)
      asc.setTrajectory();
    doApplySymmetry = true;
    while (line != null && line.contains("<-- E")) {
      boolean skip = (isTS && tsType != null && prevline.indexOf(tsType) < 0);
      if (!skip) {
        asc.newAtomSetClear(false);
        if (isTS)
          readEnergy(0, PT.getTokens(prevline + " -")[0] + " ");
        discardLinesUntilContains("<-- h");
        setSpaceGroupName("P1");
        abc = read3Vectors(true);
        setLatticeVectors();
        setFractionalCoordinates(false);
        discardLinesUntilContains("<-- R");
        while (line != null && line.contains("<-- R")) {
          tokens = getTokens();
          setAtomCoordScaled(null, tokens, 2, ANGSTROMS_PER_BOHR).elementSymbol = tokens[0];
          rd();
        }
        applySymmetryAndSetTrajectory();
      }
      discardLinesUntilContains("<-- E");
    }
  }

  @Override
  protected void finalizeSubclassReader() throws Exception {
    if (isPhonon || isOutput || isTS) {
      isTrajectory = false;
    } else {
      doApplySymmetry = true;
      setLatticeVectors();
      int nAtoms = asc.ac;
      /*
       * this needs to be run either way (i.e. even if coordinates are already
       * fractional) - to satisfy the logic in AtomSetCollectionReader()
       */
      for (int i = 0; i < nAtoms; i++)
        setAtomCoord(asc.atoms[i]);
    }
    finalizeReaderASCR();
  }

  private void setLatticeVectors() {
    if (abc[0] == null) {
      setUnitCell(a, b, c, alpha, beta, gamma);
      return;
    }
    float[] lv = new float[3];
    for (int i = 0; i < 3; i++) {
      lv[0] = abc[i].x;
      lv[1] = abc[i].y;
      lv[2] = abc[i].z;
      addExplicitLatticeVector(i, lv, 0);
    }
  }

  private void readLatticeAbc() throws Exception {
    if (tokenizeCastepCell() == 0)
      return;
    float factor = readLengthUnit(tokens[0]);
    if (tokens.length >= 3) {
      a = parseFloatStr(tokens[0]) * factor;
      b = parseFloatStr(tokens[1]) * factor;
      c = parseFloatStr(tokens[2]) * factor;
    } else {
      Logger
          .warn("error reading a,b,c in %BLOCK LATTICE_ABC in CASTEP .cell file");
      return;
    }

    if (tokenizeCastepCell() == 0)
      return;
    if (tokens.length >= 3) {
      alpha = parseFloatStr(tokens[0]);
      beta = parseFloatStr(tokens[1]);
      gamma = parseFloatStr(tokens[2]);
    } else {
      Logger
          .warn("error reading alpha,beta,gamma in %BLOCK LATTICE_ABC in CASTEP .cell file");
    }
  }

  private void readLatticeCart() throws Exception {
    if (tokenizeCastepCell() == 0)
      return;
    float factor = readLengthUnit(tokens[0]);
    float x, y, z;
    for (int i = 0; i < 3; i++) {
      if (tokens.length >= 3) {
        x = parseFloatStr(tokens[0]) * factor;
        y = parseFloatStr(tokens[1]) * factor;
        z = parseFloatStr(tokens[2]) * factor;
        abc[i] = V3.new3(x, y, z);
      } else {
        Logger.warn("error reading coordinates of lattice vector "
            + Integer.toString(i + 1)
            + " in %BLOCK LATTICE_CART in CASTEP .cell file");
        return;
      }
      if (tokenizeCastepCell() == 0)
        return;
    }
    a = abc[0].length();
    b = abc[1].length();
    c = abc[2].length();
    alpha = (abc[1].angle(abc[2]) * RAD_TO_DEG);
    beta = (abc[2].angle(abc[0]) * RAD_TO_DEG);
    gamma = (abc[0].angle(abc[1]) * RAD_TO_DEG);
  }

  private void readPositionsFrac() throws Exception {
    if (tokenizeCastepCell() == 0)
      return;
    readAtomData(1.0f);
  }

  private void readPositionsAbs() throws Exception {
    if (tokenizeCastepCell() == 0)
      return;
    float factor = readLengthUnit(tokens[0]);
    readAtomData(factor);
  }

  /*
     to be kept in sync with Utilities/io.F90
  */
  private final static String[] lengthUnitIds = { "bohr", "m", "cm", "nm",
      "ang", "a0" };

  private final static float[] lengthUnitFactors = { ANGSTROMS_PER_BOHR, 1E10f,
      1E8f, 1E1f, 1.0f, ANGSTROMS_PER_BOHR };

  private float readLengthUnit(String units) throws Exception {
    float factor = 1.0f;
    for (int i = 0; i < lengthUnitIds.length; i++)
      if (units.equalsIgnoreCase(lengthUnitIds[i])) {
        factor = lengthUnitFactors[i];
        tokenizeCastepCell();
        break;
      }
    return factor;
  }

  private void readAtomData(float factor) throws Exception {
    do {
      if (tokens.length >= 4) {
        Atom atom = asc.addNewAtom();
        int pt = tokens[0].indexOf(":");
        if (pt >= 0) {
          atom.elementSymbol = tokens[0].substring(0, pt);
          atom.atomName = tokens[0];
        } else {
          atom.elementSymbol = tokens[0];
        }
        
        atom.set(parseFloatStr(tokens[1]), parseFloatStr(tokens[2]),
            parseFloatStr(tokens[3]));
        atom.scale(factor);
      } else {
        Logger.warn("cannot read line with CASTEP atom data: " + line);
      }
    } while (tokenizeCastepCell() > 0
        && !tokens[0].equalsIgnoreCase("%ENDBLOCK"));
  }

  private int tokenizeCastepCell() throws Exception {
    while (rd() != null) {
      if ((line = line.trim()).length() == 0 || line.startsWith("#")
          || line.startsWith("!"))
        continue;
      if (!isCell) {
        if (line.startsWith("%")) {
          isCell = true;
          break;
        }
        if (line.startsWith("LST")) {
          isTS = true;
          Logger.info("reading CASTEP .ts file");
          return -1;
        }
        if (line.startsWith("BEGIN header")) {
          isPhonon = true;
          Logger.info("reading CASTEP .phonon file");
          return -1;
        }
        if (line.contains("CASTEP")) {
          isOutput = true;
          Logger.info("reading CASTEP .castep file");
          return -1;
        }
      }
      break;
    }
    return (line == null ? 0 : (tokens = getTokens()).length);
  }

  /*
                   Born Effective Charges
                   ----------------------
   O       1        -5.27287    -0.15433     1.86524
                    -0.32884    -1.78984     0.13678
                     1.81939     0.06085    -1.80221
   */
  private void readOutputBornChargeTensors() throws Exception {
    if (rd().indexOf("--------") < 0)
      return;
    Atom[] atoms = asc.atoms;
    appendLoadNote("Ellipsoids: Born Charge Tensors");
    while (rd().indexOf('=') < 0)
      getTensor(atoms[readOutputAtomIndex()], line.substring(12));
  }


  private int readOutputAtomIndex() {
    tokens = getTokens();
    return asc.getAtomIndex(tokens[0] + tokens[1]);
  }

  private void getTensor(Atom atom, String line0) throws Exception {
    float[] data = new float[9];
    double[][] a = new double[3][3];
    fillFloatArray(line0, 0, data);
    Logger.info("tensor " +  atom.atomName 
        + "\t" +Escape.eAF(data)); 
    for (int p = 0, i = 0; i < 3; i++)
      for (int j = 0; j < 3; j++)
        a[i][j] = data[p++];
    atom.addTensor((new Tensor()).setFromAsymmetricTensor(a, "charge", atom.atomName + " " + line0), null, false);
    if (!haveCharges)
      appendLoadNote("Ellipsoids set \"charge\": Born Effective Charges");
    haveCharges = true;
  }

  /*
     Hirshfeld Analysis
     ------------------
Species   Ion     Hirshfeld Charge (e)
======================================
  H        1                 0.05
...
  O        6                -0.24
  O        7                -0.25
  O        8                -0.25
======================================

  or
  
  Atomic Populations (Mulliken)
  -----------------------------
Species   Ion     s      p      d      f     Total  Charge (e)
==============================================================
  O        1     1.86   4.84   0.00   0.00   6.70    -0.70
..
  Ti       3     2.23   6.33   2.12   0.00  10.67     1.33
  Ti       4     2.23   6.33   2.12   0.00  10.67     1.33
==============================================================

*/

  /**
   * read Mulliken or Hirshfield charges
   * @throws Exception 
   */
  private void readOutputCharges() throws Exception {
    if (line.toUpperCase().indexOf(chargeType ) < 0)
      return; 
    Logger.info("reading charges: " + line);
    readLines(2);
    boolean haveSpin = (line.indexOf("Spin") >= 0);
    rd();
    Atom[] atoms = asc.atoms;
    float[] spins = (haveSpin ? new float[atoms.length] : null);
    if (spins != null)
      for (int i = 0; i < spins.length; i++)
        spins[i] = 0;
    while (rd() != null && line.indexOf('=') < 0) {
      int index = readOutputAtomIndex();
      float charge = parseFloatStr(tokens[haveSpin ? tokens.length - 2 : tokens.length - 1]);
      atoms[index].partialCharge = charge;
      if (haveSpin)
        spins[index] = parseFloatStr(tokens[tokens.length - 1]);
    }
    if (haveSpin)
      asc.setAtomProperties("spin", spins, -1, false);
    
  }


  //////////// phonon code ////////////

  /*
  Unit cell vectors (A)
     0.000000    1.819623    1.819623
     1.819623    0.000000    1.819623
     1.819623    1.819623    0.000000
  Fractional Co-ordinates
      1     0.000000    0.000000    0.000000   B        10.811000
      2     0.250000    0.250000    0.250000   N        14.006740
    */
  private void readPhononUnitCell() throws Exception {
    abc = read3Vectors(line.indexOf("bohr") >= 0);
    setSpaceGroupName("P1");
    setLatticeVectors();
  }

  private void readPhononFractionalCoord() throws Exception {
    setFractionalCoordinates(true);
    while (rd() != null && line.indexOf("END") < 0) {
      tokens = getTokens();
      addAtomXYZSymName(tokens, 1, tokens[4], null).bfactor = parseFloatStr(tokens[5]); // mass, actually
    }
    ac = asc.ac;
    // we collect the atom points, because any supercell business
    // will trash those, and we need the originals
    atomPts = new P3[ac];
    Atom[] atoms = asc.atoms;
    for (int i = 0; i < ac; i++)
      atomPts[i] = P3.newP(atoms[i]);
  }

  /*
     q-pt=    1    0.000000  0.000000  0.000000      1.000000    1.000000  0.000000  0.000000
       1      58.268188              0.0000000                                  
       2      58.268188              0.0000000                                  
       3      58.292484              0.0000000                                  
       4    1026.286406             13.9270643                                  
       5    1026.286406             13.9270643                                  
       6    1262.072445             13.9271267                                  
                        Phonon Eigenvectors
  Mode Ion                X                                   Y                                   Z
   1   1 -0.188759409143  0.000000000000      0.344150676582  0.000000000000     -0.532910085817  0.000000000000
   1   2 -0.213788416373  0.000000000000      0.389784162147  0.000000000000     -0.603572578624  0.000000000000
   2   1 -0.506371267280  0.000000000000     -0.416656077168  0.000000000000     -0.089715190073  0.000000000000
   2   2 -0.573514781701  0.000000000000     -0.471903590472  0.000000000000     -0.101611191184  0.000000000000
   3   1  0.381712598768  0.000000000000     -0.381712598812  0.000000000000     -0.381712598730  0.000000000000
   3   2  0.433161430960  0.000000000000     -0.433161431010  0.000000000000     -0.433161430917  0.000000000000
   4   1  0.431092607594  0.000000000000     -0.160735361462  0.000000000000      0.591827969056  0.000000000000
   4   2 -0.380622988260  0.000000000000      0.141917473232  0.000000000000     -0.522540461492  0.000000000000
   5   1  0.434492641457  0.000000000000      0.590583470288  0.000000000000     -0.156090828832  0.000000000000
   5   2 -0.383624967478  0.000000000000     -0.521441660837  0.000000000000      0.137816693359  0.000000000000
   6   1  0.433161430963  0.000000000000     -0.433161430963  0.000000000000     -0.433161430963  0.000000000000
   6   2 -0.381712598770  0.000000000000      0.381712598770  0.000000000000      0.381712598770  0.000000000000
   */

  private void readPhononFrequencies() throws Exception {
    tokens = getTokens();
    V3 v = new V3();
    V3 qvec = V3.new3(parseFloatStr(tokens[2]), parseFloatStr(tokens[3]),
        parseFloatStr(tokens[4]));
    String fcoord = getFractionalCoord(qvec);
    String qtoks = "{" + tokens[2] + " " + tokens[3] + " " + tokens[4] + "}";
    if (fcoord == null)
      fcoord = qtoks;
    else
      fcoord = "{" + fcoord + "}";
    boolean isOK = isAllQ;
    boolean isSecond = (tokens[1].equals(lastQPt));
    qpt2 = (isSecond ? qpt2 + 1 : 1);

    lastQPt = tokens[1];
    //TODO not quite right: can have more than two options. 
    if (!isOK && checkFilterKey("Q=")) {
      // check for an explicit q=n or q={1/4 1/2 1/4}
      if (desiredQpt != null) {
        v.sub2(desiredQpt, qvec);
        if (v.length() < 0.001f)
          fcoord = desiredQ;
      }
      isOK = (checkFilterKey("Q=" + fcoord + "." + qpt2 + ";")
          || checkFilterKey("Q=" + lastQPt + "." + qpt2 + ";") 
          || !isSecond && checkFilterKey("Q=" + fcoord + ";") 
          || !isSecond && checkFilterKey("Q=" + lastQPt + ";"));
      if (!isOK)
        return;
    }
    boolean isGammaPoint = (qvec.length() == 0);
    float nx = 1, ny = 1, nz = 1;
    if (ptSupercell != null && !isOK && !isSecond) {
      matSupercell = new M4();
      matSupercell.m00 = ptSupercell.x;
      matSupercell.m11 = ptSupercell.y;
      matSupercell.m22 = ptSupercell.z;
      matSupercell.m33 = 1;
      Logger.info("Using supercell \n" + matSupercell);
      nx = ptSupercell.x;
      ny = ptSupercell.y;
      nz = ptSupercell.z;
      // only select corresponding phonon vector 
      // relating to this supercell -- one that has integral dot product
      float dx = (qvec.x == 0 ? 1 : qvec.x) * nx;
      float dy = (qvec.y == 0 ? 1 : qvec.y) * ny;
      float dz = (qvec.z == 0 ? 1 : qvec.z) * nz;
      if ((nx != 1 || ny != 1 || nz != 1) && isGammaPoint || !isInt(dx) || !isInt(dy) || !isInt(dz))
        return;
      isOK = true;
    }
    if (ptSupercell == null || !havePhonons)
      appendLoadNote(line);
    if (!isOK && isSecond)
      return;
    if (!isOK && (ptSupercell == null) == !isGammaPoint)
      return;
    if (havePhonons && !isAllQ)
      return;
    havePhonons = true;
    String qname = "q=" + lastQPt + " " + fcoord;
    applySymmetryAndSetTrajectory();
    if (isGammaPoint)
      qvec = null;
    Lst<Float> freqs = new  Lst<Float>();
    while (rd() != null && line.indexOf("Phonon") < 0) {
      tokens = getTokens();
      freqs.addLast(Float.valueOf(parseFloatStr(tokens[1])));
    }
    rd();
    int frequencyCount = freqs.size();
    float[] data = new float[8];
    V3 t = new V3();
    asc.setCollectionName(qname);
    for (int i = 0; i < frequencyCount; i++) {
      if (!doGetVibration(++vibrationNumber)) {
        for (int j = 0; j < ac; j++)
          rd();
        continue;
      }
      if (desiredVibrationNumber <= 0) {
        if (!isTrajectory) {
          cloneLastAtomSet(ac, atomPts);
          applySymmetryAndSetTrajectory();
        }
      }
      symmetry = asc.getSymmetry();
      int iatom = asc.getLastAtomSetAtomIndex();
      float freq = freqs.get(i).floatValue();
      Atom[] atoms = asc.atoms;
      int aCount = asc.ac;
      for (int j = 0; j < ac; j++) {
        fillFloatArray(null, 0, data);
        for (int k = iatom++; k < aCount; k++)
          if (atoms[k].atomSite == j) {
            t.sub2(atoms[k], atoms[atoms[k].atomSite]);
            // for supercells, fractional coordinates end up
            // in terms of the SUPERCELL and need to be transformed.
            // TODO: UNTESTED
            if (matSupercell != null)
              matSupercell.rotTrans(t);
            setPhononVector(data, atoms[k], t, qvec, v);
            asc.addVibrationVectorWithSymmetry(k, v.x, v.y, v.z, true);
          }
      }
      if (isTrajectory)
        asc.setTrajectory();
      asc.setAtomSetFrequency(null, null, "" + freq, null);
      asc.setAtomSetName(DF.formatDecimal(freq, 2)
          + " cm-1 " + qname);
    }
  }

  private M4 matSupercell;

  private String getFractionalCoord(V3 qvec) {
    return (symmetry != null && isInt(qvec.x * 12) 
        && isInt(qvec.y * 12) && isInt(qvec.z * 12) ? symmetry.fcoord(qvec)
        : null);
  }

  private static boolean isInt(float f) {
    return (Math.abs(f - Math.round(f)) < 0.001f);
  }

  private static final double TWOPI = Math.PI * 2;

  /**
   * transform complex vibration vector to a real vector by applying the
   * appropriate translation, storing the results in v
   * 
   * @param data
   *        from .phonon line parsed for floats
   * @param atom
   * @param rTrans
   *        translation vector in unit fractional coord
   * @param qvec
   *        q point vector
   * @param v
   *        return vector
   */
  private void setPhononVector(float[] data, Atom atom, V3 rTrans,
                               V3 qvec, V3 v) {
    // complex[r/i] vx = data[2/3], vy = data[4/5], vz = data[6/7]
    if (qvec == null) {
      v.set(data[2], data[4], data[6]);
    } else {
      // from CASTEP ceteprouts.pm:
      //  $phase = $qptx*$$sh[0] + $qpty*$$sh[1] + $qptz*$$sh[2];
      //  $cosph = cos($twopi*$phase); $sinph = sin($twopi*$phase); 
      //  push @$pertxo, $cosph*$$pertx_r[$iat] - $sinph*$$pertx_i[$iat];
      //  push @$pertyo, $cosph*$$perty_r[$iat] - $sinph*$$perty_i[$iat];
      //  push @$pertzo, $cosph*$$pertz_r[$iat] - $sinph*$$pertz_i[$iat];

      double phase = qvec.dot(rTrans);
      double cosph = Math.cos(TWOPI * phase);
      double sinph = Math.sin(TWOPI * phase);
      v.x = (float) (cosph * data[2] - sinph * data[3]);
      v.y = (float) (cosph * data[4] - sinph * data[5]);
      v.z = (float) (cosph * data[6] - sinph * data[7]);
    }
    v.scale((float) Math.sqrt(1 / atom.bfactor)); // mass stored in bfactor
  }

}
