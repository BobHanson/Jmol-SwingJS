/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-08-27 21:07:49 -0500 (Sun, 27 Aug 2006) $
 * $Revision: 5420 $
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
package org.jmol.adapter.readers.simple;


import javajs.util.PT;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Atom;
import javajs.util.BS;
import org.jmol.util.Logger;

/**
 * Reads Mopac 93, 6, 7, 2002, or 2009 output files
 *
 * @author Egon Willighagen <egonw@jmol.org>
 */
public class MopacReader extends AtomSetCollectionReader {
    
  private boolean chargesFound = false;
  private boolean haveHeader;
  private int mopacVersion;
  
  @Override
  protected void initializeReader() throws Exception {
    while (mopacVersion == 0) {
      discardLinesUntilContains("MOPAC");
      if (line.indexOf("2009") >= 0)
        mopacVersion = 2009;
      else if (line.indexOf("6.") >= 0)
        mopacVersion = 6;
      else if (line.indexOf("7.") >= 0)
        mopacVersion = 7;
      else if (line.indexOf("93") >= 0)
        mopacVersion = 93;
      else if (line.indexOf("2002") >= 0)
        mopacVersion = 2002;
      else if (line.indexOf("MOPAC2") >= 0)
        mopacVersion = PT.parseInt(line.substring(line.indexOf("MOPAC2") + 5));
    }
    Logger.info("MOPAC version " + mopacVersion);
  }
  
  @Override
  protected boolean checkLine() throws Exception {    
    if (!haveHeader) {
      if (line.trim().equals("CARTESIAN COORDINATES")) {
        processCoordinates();
        asc.setAtomSetName("Input Structure");
        return true;
      }
      haveHeader = line.startsWith(" ---");
      return true;
    }
    if (line.indexOf("TOTAL ENERGY") >= 0) {
      processTotalEnergy();
      return true;
    }
    if (line.indexOf("ATOMIC CHARGES") >= 0) {
      processAtomicCharges();
      return true;
    }
    if (line.trim().equals("CARTESIAN COORDINATES")) {
      processCoordinates();
      return true;
    }
    if (line.indexOf("ORIENTATION OF MOLECULE IN FORCE") >= 0) {
      processCoordinates();
      asc.setAtomSetName("Orientation in Force Field");
      return true;
    }
    if (line.indexOf("NORMAL COORDINATE ANALYSIS") >= 0) {
      readFrequencies();
      return true;
    }
    return true;
  }
    
  void processTotalEnergy() {
    //frameInfo = line.trim();
  }

  /**
   * Reads the section in MOPAC files with atomic charges.
   * These sections look like:
   * <pre>
   *               NET ATOMIC CHARGES AND DIPOLE CONTRIBUTIONS
   * 
   *          ATOM NO.   TYPE          CHARGE        ATOM  ELECTRON DENSITY
   *            1          C          -0.077432        4.0774
   *            2          C          -0.111917        4.1119
   *            3          C           0.092081        3.9079
   * </pre>
   * They are expected to be found in the file <i>before</i> the 
   * cartesian coordinate section.
   * 
   * @throws Exception
   */
void processAtomicCharges() throws Exception {
    readLines(2);
    asc.newAtomSet(); // charges before coords, see JavaDoc
    baseAtomIndex = asc.ac;
    int expectedAtomNumber = 0;
    while (rd() != null) {
      int atomNumber = parseIntStr(line);
      if (atomNumber == Integer.MIN_VALUE) // a blank line
        break;
      ++expectedAtomNumber;
      if (atomNumber != expectedAtomNumber)
        throw new Exception("unexpected atom number in atomic charges");
      Atom atom = asc.addNewAtom();
      atom.elementSymbol = parseToken();
      atom.partialCharge = parseFloat();
    }
    chargesFound = true;
  }
    
  /**
   * Reads the section in MOPAC files with cartesian coordinates. These sections
   * look like:
   * 
   * <pre>
   *           CARTESIAN COORDINATES
   * 
   *     NO.       ATOM         X         Y         Z
   * 
   *      1         C        0.0000    0.0000    0.0000
   *      2         C        1.3952    0.0000    0.0000
   *      3         C        2.0927    1.2078    0.0000
   * </pre>
   * 
   * In a MOPAC2002 file the columns are different:
   * 
   * <pre>
   *          CARTESIAN COORDINATES
   * 
   * NO.       ATOM           X             Y             Z
   * 
   *  1         H        0.00000000    0.00000000    0.00000000
   *  2         O        0.95094500    0.00000000    0.00000000
   *  3         H        1.23995160    0.90598439    0.00000000
   * </pre>
   * 
   * @throws Exception
   */
  void processCoordinates() throws Exception {
    if (!chargesFound) {
      asc.newAtomSet();
      baseAtomIndex = asc.ac;
    } else {
      chargesFound = false;
    }
    Atom[] atoms = asc.atoms;
    int atomNumber;
    while (rd().trim().length() == 0 || line.indexOf("ATOM") >= 0) {
      // skip header lines
    }
    while (line != null) {
      String[] tokens = getTokens();
      if (tokens.length == 0
          || (atomNumber = parseIntStr(tokens[0])) == Integer.MIN_VALUE)
        break;
      Atom atom = atoms[baseAtomIndex + atomNumber - 1];
      if (atom == null)
        atom = asc.addNewAtom(); // if no charges were found first
      atom.atomSerial = atomNumber;
      setAtomCoordTokens(atom, tokens, 2);
      String elementSymbol = tokens[1];
      int atno = parseIntStr(elementSymbol);
      if (atno != Integer.MIN_VALUE)
        elementSymbol = getElementSymbol(atno);
      atom.elementSymbol = elementSymbol;
      rd();
    }
  }
  /**
   * Interprets the Harmonic frequencies section.
   * 
   * <pre>
   *     THE LAST 6 VIBRATIONS ARE THE TRANSLATION AND ROTATION MODES
   *    THE FIRST THREE OF THESE BEING TRANSLATIONS IN X, Y, AND Z, RESPECTIVELY
   *              NORMAL COORDINATE ANALYSIS
   *   
   *       ROOT NO.    1           2           3           4           5           6
   *   
   *              370.51248   370.82204   618.03031   647.68700   647.74806   744.32662
   *     
   *            1   0.00002     0.00001    -0.00002    -0.05890     0.07204    -0.00002
   *            2   0.00001    -0.00006    -0.00001     0.01860     0.13517     0.00000
   *            3   0.00421    -0.11112     0.06838    -0.00002    -0.00003    -0.02449
   *   
   *            4   0.00002     0.00001    -0.00002    -0.04779     0.07977    -0.00001
   *            5  -0.00002     0.00002     0.00001     0.13405    -0.02908     0.00004
   *            6  -0.10448     0.05212    -0.06842    -0.00005    -0.00002    -0.02447
   * </pre>
   * 
   * <p>
   * The vectors are added to a clone of the last read AtomSet. Only the
   * Frequencies are set as properties for each of the frequency type AtomSet
   * generated.
   * 
   * @throws Exception
   *             If an I/O error occurs
   */
  private void readFrequencies() throws Exception {
    
    BS bsOK = new BS();
    int n0 = asc.iSet + 1;
    String[] tokens;

    boolean done = false;
    while (!done && rd() != null
        && line.indexOf("DESCRIPTION") < 0 && line.indexOf("MASS-WEIGHTED") < 0)
      if (line.toUpperCase().indexOf("ROOT") >= 0) {
        discardLinesUntilNonBlank();
        tokens = getTokens();
        if (Float.isNaN(PT.parseFloatStrict(tokens[tokens.length - 1]))) {
          discardLinesUntilNonBlank();
          tokens = getTokens();
        }
        int frequencyCount = tokens.length;
        rd();
        int iAtom0 = asc.ac;
        int ac = asc.getLastAtomSetAtomCount();
        boolean[] ignore = new boolean[frequencyCount];
        float freq1 = PT.parseFloatStrict(tokens[0]);
        boolean ignoreNegative = (freq1 < 0);
        for (int i = 0; i < frequencyCount; ++i) {
          ignore[i] = done || (done = (!ignoreNegative && PT.parseFloatStrict(tokens[i]) < 1))
          || !doGetVibration(++vibrationNumber);
          if (ignore[i])
            continue;  
          bsOK.set(vibrationNumber - 1);
          asc.cloneLastAtomSet();
        }
        fillFrequencyData(iAtom0, ac, ac, ignore, false, 0, 0, null, 2, null);
      }
    String[][] info = new String[vibrationNumber][];
    if (line.indexOf("DESCRIPTION") < 0)
      discardLinesUntilContains("DESCRIPTION");
    while (discardLinesUntilContains("VIBRATION") != null) {
      tokens = getTokens();
      int freqNo = parseIntStr(tokens[1]); 
      tokens[0] = PT.getTokens(rd())[1]; // FREQ
      if (tokens[2].equals("ATOM"))
        tokens[2] = null;
      info[freqNo - 1] = tokens;
      if (freqNo == vibrationNumber)
        break;            
    }
    // some may be missing -- degenerate sets
    for (int i = vibrationNumber - 1; --i >= 0; ) 
      if (info[i] == null)
        info[i] = info[i + 1];
    // now set labels
    for (int i = 0, n = n0; i < vibrationNumber; i++) {
      if (!bsOK.get(i))
        continue;
      asc.iSet = n++;
      asc.setAtomSetFrequency(vibrationNumber, null, info[i][2], info[i][0], null);
    }
  }
}
