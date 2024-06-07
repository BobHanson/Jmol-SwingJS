/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-30 10:16:53 -0500 (Sat, 30 Sep 2006) $
 * $Revision: 5778 $
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
package org.jmol.adapter.readers.xtal;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Atom;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;

import org.jmol.util.Logger;

/**
 * A reader for SHELX output (RES) files. It does not read all information.
 * The list of fields that is read: TITL, REM, END, CELL, SPGR, SFAC
 * 
 * Atom positions and thermal ellipsoids are read.
 *
 * <p>A reader for SHELX files. It currently supports SHELXL.
 *
 * <p>The SHELX format is described on the net:
 * <a href="http://www.msg.ucsf.edu/local/programs/shelxl/ch_07.html">
 * http://www.msg.ucsf.edu/local/programs/shelxl/ch_07.html</a>.
 *
 * modified by Bob Hanson 2006/04 to allow 
 * variant CrystalMaker .cmdf file reading
 * -- but by 2/2010 looks like these ASCII CrystalMaker files aren't used
 * anymore by CrystalMaker, and instead it uses a binary format. 
 * CrystalMaker2.2.3 seems to attempt to read the files, but actually cannot.
 * At least not for the file given at http://www.ch.ic.ac.uk/chemime/
 *  
 */

public class ShelxReader extends AtomSetCollectionReader {

  private String[] sfacElementSymbols;
  private boolean isCmdf;
  String[] tokens;
  
  @Override
  public void initializeReader() {
      setFractionalCoordinates(true);
  }
  
  @Override
  protected boolean checkLine() throws Exception {

    int lineLength ;
    // '=' as last char of line means continue on next line
    while ((lineLength = (line = line.trim()).length()) > 0 
        && line.charAt(lineLength - 1) == '=') 
      line = line.substring(0, lineLength - 1) + rd();
    
    tokens = getTokens();
    if (tokens.length == 0)
      return true;
    String command = tokens[0].toUpperCase();
    if (command.equals("TITL")) {
      if (!doGetModel(++modelNumber, null))
        return checkLastModel();
      sfacElementSymbols = null;
      applySymmetryAndSetTrajectory();
      setFractionalCoordinates(true);
      asc.newAtomSet();
      asc.setAtomSetName(line.substring(4).trim());
      return true;
    }

    if (command.equals("NOTE")) {
      isCmdf = true;
      return true;
    }
    if (!doProcessLines || lineLength < 3)
      return true;

    if (unsupportedRecordTypes.indexOf(";" + command + ";") >= 0)
      return true;
    for (int i = supportedRecordTypes.length; --i >= 0;)
      if (command.equals(supportedRecordTypes[i])) {
        processSupportedRecord(i);
        return true;
      }
    if (!isCmdf)
      assumeAtomRecord();
    return true;
  }

  private final static String unsupportedRecordTypes = 
    ";ZERR;DISP;UNIT;LAUE;REM;MORE;TIME;" +
    "HKLF;OMIT;SHEL;BASF;TWIN;EXTI;SWAT;HOPE;MERG;" +
    "SPEC;RESI;MOVE;ANIS;AFIX;HFIX;FRAG;FEND;EXYZ;" +
    "EXTI;EADP;EQIV;" +
    "CONN;BIND;FREE;" +
    "DFIX;DANG;BUMP;SAME;SADI;CHIV;FLAT;DELU;SIMU;" +
    "DEFS;ISOR;NCSY;SUMP;" +
    "L.S.;CGLS;BLOC;DAMP;STIR;WGHT;FVAR;" +
    "BOND;CONF;MPLA;RTAB;HTAB;LIST;ACTA;SIZE;TEMP;" +
    "WPDB;" +
    "FMAP;GRID;PLAN;MOLE;";

  /**
   * Supported records -- remove from unsupportedRecordTypes if added here
   */
  final private static String[] supportedRecordTypes = { "TITL", "CELL", "SPGR",
      "SFAC", "LATT", "SYMM", "NOTE", "ATOM", "PART", "END" };

  private void processSupportedRecord(int recordIndex) throws Exception {
    //System.out.println(recordIndex+" "+line);
    switch (recordIndex) {
    case 0: // TITL
    case 9: // END
      break;
    case 1: // CELL
      cell();
      break;
    case 2: // SPGR
      setSpaceGroupName(PT.parseTrimmedAt(line, 4));
      break;
    case 3: // SFAC
      parseSfacRecord();
      break;
    case 4: // LATT
      parseLattRecord();
      break;
    case 5: // SYMM
      parseSymmRecord();
      break;
    case 6: // NOTE
      isCmdf = true;
      break;
    case 7: // ATOM
      isCmdf = true;
      processCmdfAtoms();
      break;
    case 8: // PART
      processPartRecord();
      break;
    }
  }
  
  char altloc = '\0';

  private void processPartRecord() {
    
    // email exchange with Brian McMahon 22.10.11
    //            see
    //            https://journals.iucr.org/c/issues/2015/01/00/fa3356/ (in the below I
    //            take PART to be the same as
    //            _atom_site_disorder_group):
    //
    //            "The use of PART numbers, introduced in SHELXL93, has proved invaluable
    //            in the refinement of disordered structures. Two atoms are considered to
    //            be bonded if they have the same PART number or if one of them is in
    //            PART 0. The resulting connectivity table is used for the generation of
    //            H atoms (HFIX and AFIX), for setting up restraints such as DELU, SIMU,
    //            RIGU, CHIV, BUMP and SAME, and for generating tables of geometric
    //            parameters (BOND, CONF, HTAB). Usually, most of the atoms are in
    //            PART 0, but, for example, a molecule or side chain dis­ordered over
    //            three positions could use PART 1, PART 2 and PART 3. If the PART
    //            number is negative, bonds are not generated to symmetry-equivalent
    //            atoms. It should be noted that positive PART numbers 1, 2, 3 etc.
    //            correspond to the alternative location indicators A, B, C etc. in
    //            PDB format. However, this notation is difficult to use when there
    //            is a disorder within a disorder."

    // atom.basLoc provides the base altloc "n" of "-n"
    // as symmetry is applied, if basLoc is found, then 
    // the cloned atom is given an incremented altloc
    // this only works with C2 and m; with higher-order symmetry, this
    // will dump all the symmetry-related groups into the same configuration=2


    
    // PART 1 become altloc '1'
    int part = parseIntStr(tokens[1]);
    altloc = (char) (part == 0 ? 0 : '0' + part);
  }

  private boolean isCentroSymmetric;
  
  private void parseLattRecord() throws Exception {
    int latt = parseIntStr(tokens[1]);
    isCentroSymmetric = (latt > 0);
    if (latt ==1 || latt == -1)
      return;
    asc.getXSymmetry().setLatticeParameter(latt);
  }

  boolean haveXYZ;
  
  private void parseSymmRecord() throws Exception {
    if (!haveXYZ) {
      setSymmetryOperator("x,y,z");
      haveXYZ = true;
    }
    setSymmetryOperator(line.substring(4).trim());
  }

  private void cell() throws Exception {
    /* example:
     * CELL   wavelngth    a        b         c       alpha   beta   gamma
     * CELL   1.54184   7.11174  21.71704  30.95857  90.000  90.000  90.000
     * 
     * or CrystalMaker file:
     * 
     * CELL       a        b         c       alpha   beta   gamma
     * CELL   7.11174  21.71704  30.95857  90.000  90.000  90.000
     */

    int ioff = tokens.length - 6;
    if (ioff == 2)
      asc.setInfo("wavelength",
          Double.valueOf(parseDoubleStr(tokens[1])));
    for (int ipt = 0; ipt < 6; ipt++)
      setUnitCellItem(ipt, parseDoubleStr(tokens[ipt + ioff]));
  }

  private void parseSfacRecord() {
    // an SFAC record is one of two cases
    // a simple SFAC record contains element names
    // a general SFAC record contains coefficients for a single element
    boolean allElementSymbols = true;
    for (int i = tokens.length; allElementSymbols && --i >= 1;) {
      String token = tokens[i];
      allElementSymbols = isValidElementSymbolNoCaseSecondChar(token);
    }
    String[] sfacTokens = PT.getTokens(line.substring(4));
    if (allElementSymbols)
      parseSfacElementSymbols(sfacTokens);
    else
      parseSfacCoefficients(sfacTokens);
  }

  private void parseSfacElementSymbols(String[] sfacTokens) {
    if (sfacElementSymbols == null) {
      sfacElementSymbols = sfacTokens;
    } else {
      int oldCount = sfacElementSymbols.length;
      int tokenCount = sfacTokens.length;
      sfacElementSymbols = AU.arrayCopyS(sfacElementSymbols, oldCount + tokenCount);
      for (int i = tokenCount; --i >= 0;)
        sfacElementSymbols[oldCount + i] = sfacTokens[i];
    }
  }
  
  private void parseSfacCoefficients(String[] sfacTokens) {
    double a1 = parseDoubleStr(sfacTokens[1]);
    double a2 = parseDoubleStr(sfacTokens[3]);
    double a3 = parseDoubleStr(sfacTokens[5]);
    double a4 = parseDoubleStr(sfacTokens[7]);
    double c = parseDoubleStr(sfacTokens[9]);
    // element # is these floats rounded to nearest int
    int z = (int) Math.round(a1 + a2 + a3 + a4 + c);
    String elementSymbol = getElementSymbol(z);
    int oldCount = 0;
    if (sfacElementSymbols == null) {
      sfacElementSymbols = new String[1];
    } else {
      oldCount = sfacElementSymbols.length;
      sfacElementSymbols = AU.arrayCopyS(sfacElementSymbols, oldCount + 1);
      sfacElementSymbols[oldCount] = elementSymbol;
    }
    sfacElementSymbols[oldCount] = elementSymbol;
  }

  private void assumeAtomRecord() throws Exception {
    // this line gives an atom, because any line not starting with
    // a SHELX command is an atom
    double x = Double.NaN, y = Double.NaN, z = Double.NaN;
    double occ = 1;
    int elementIndex = -1;
    String atomName = null;
    try {
      atomName = tokens[0];
      elementIndex = parseIntStr(tokens[1]) - 1;
      x = parseDoubleStr(tokens[2])%10;
      y = parseDoubleStr(tokens[3])%10;
      z = parseDoubleStr(tokens[4])%10;
      occ = parseDoubleStr(tokens[5])%10;
    } catch (Exception e) {
      // must have a NaN
    }
    if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
      Logger.error("skipping line " + line);
      return;
    }

    Atom atom = asc.addNewAtom();
    atom.atomName = atomName;
    atom.foccupancy = occ;
    boolean isQPeak = atomName.startsWith("Q");
    if (isQPeak) {
     atom.elementSymbol = "Xx";
    } else  if (sfacElementSymbols != null && elementIndex >= 0
        && elementIndex < sfacElementSymbols.length) {
      atom.elementSymbol = sfacElementSymbols[elementIndex];
    }
    setAtomCoordXYZ(atom, x, y, z);
    atom.altLoc = altloc;

    if (tokens.length == 12) {
      double[] data = new double[8];
      data[0] = parseDoubleStr(tokens[6]); //U11
      data[1] = parseDoubleStr(tokens[7]); //U22
      data[2] = parseDoubleStr(tokens[8]); //U33
      data[3] = parseDoubleStr(tokens[11]); //U12
      data[4] = parseDoubleStr(tokens[10]); //U13
      data[5] = parseDoubleStr(tokens[9]); //U23
      for (int i = 0; i < 6; i++)
        if (Double.isNaN(data[i])) {
          Logger.error("Bad anisotropic Uij data: " + line);
          return;
        }
      asc.setAnisoBorU(atom, data, 8);
      // Ortep Type 8: D = 2pi^2, C = 2, a*b*  
    }
  }

  private void processCmdfAtoms() throws Exception {
    while (rd() != null && line.length() > 10) {
      tokens = getTokens();
      addAtomXYZSymName(tokens, 2, getSymbol(tokens[0]), tokens[1]);
    }
  }

  private String getSymbol(String sym) {
    if (sym == null)
      return "Xx";
    int len = sym.length();
    if (len < 2)
      return sym;
    char ch1 = sym.charAt(1);
    if (ch1 >= 'a' && ch1 <= 'z')
      return sym.substring(0, 2);
    return "" + sym.charAt(0);
  }

  public static boolean isValidElementSymbolNoCaseSecondChar(String str) {
    if (str == null)
      return false;
    int length = str.length();
    if (length == 0)
      return false;
    char chFirst = str.charAt(0);
    if (length == 1)
      return Atom.isValidSym1(chFirst);
    if (length > 2)
      return false;
    char chSecond = str.charAt(1);
    return Atom.isValidSymNoCase(chFirst, chSecond);
  }

  @Override
  public void applySymmetryAndSetTrajectory() throws Exception {
    if (isCentroSymmetric && !ignoreFileSymmetryOperators) {
      asc.getXSymmetry().getSymmetry().addInversion();
      isCentroSymmetric = false;
    }
    applySymTrajASCR();
  }

}
