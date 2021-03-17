/* $RCSfile: ADFReader.java,v $
 * $Author: egonw $
 * $Date: 2004/02/23 08:52:55 $
 * $Revision: 1.3.2.4 $
 *
 * Copyright (C) 2002-2004  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.jmol.adapter.readers.quantum;

import org.jmol.api.JmolAdapter;
import org.jmol.quantum.SlaterData;
import org.jmol.util.Logger;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;

import java.util.Hashtable;

import java.util.Map;

/**
 * 
 * TODO: adf-2007.out causes failure reading basis functions
 * 
 * A reader for ADF output.
 * Amsterdam Density Functional (ADF) is a quantum chemistry program
 * by Scientific Computing & Modelling NV (SCM)
 * (http://www.scm.com/).
 *
 * <p> Molecular coordinates, energies, and normal coordinates of
 * vibrations are read. Each set of coordinates is added to the
 * ChemFile in the order they are found. Energies and vibrations
 * are associated with the previously read set of coordinates.
 *
 * <p> This reader was developed from a small set of
 * example output files, and therefore, is not guaranteed to
 * properly read all ADF output. If you have problems,
 * please contact the author of this code, not the developers
 * of ADF.
 *
 *<p> Added note (Bob Hanson) -- 1/1/2010 -- 
 *    Trying to implement reading of orbitals; ran into the problem
 *    that the atomic Slater description uses Cartesian orbitals,
 *    but the MO refers to spherical orbitals. 
 *
 *
 * @author Bradley A. Smith (yeldar@home.com)
 * @version 1.0
 */
public class AdfReader extends SlaterReader {

  
  private Map<String, SymmetryData> htSymmetries;
  private Lst<SymmetryData> vSymmetries;
  private String energy = null;
  private int nXX = 0;
  private String symLine;
  
  @Override
  protected boolean checkLine() throws Exception {
    
    if (line.indexOf("Irreducible Representations, including subspecies") >= 0) {
      readSymmetries();
      return true;
    }
    if (line.indexOf("S F O s  ***  (Symmetrized Fragment Orbitals)  ***") >= 0) {
      readSlaterBasis(); // Cartesians
      return true;
    }
    if (line.indexOf(" Coordinates (Cartesian, in Input Orientation)") >= 0
        || line.indexOf("G E O M E T R Y  ***") >= 0) {
      if (!doGetModel(++modelNumber, null))
        return checkLastModel();
      readCoordinates();
      return true;
    }
    if (line.indexOf(" ======  Eigenvectors (rows) in BAS representation") >= 0) {
      if (doReadMolecularOrbitals)
        readMolecularOrbitals(PT.getTokens(symLine)[1]);
      return true;
    }
    if (!doProcessLines)
      return true;
    
    if (line.indexOf("Energy:") >= 0) {
      String[] tokens = PT.getTokens(line.substring(line.indexOf("Energy:")));
      energy = tokens[1];
      return true;
    }
    if (line.indexOf("Vibrations") >= 0) {
      readFrequencies();
      return true;
    }
    if (line.indexOf(" === ") >= 0) {
      symLine = line;
      return true;
    }
    if (line.indexOf(" ======  Eigenvectors (rows) in BAS representation") >= 0) {
      readMolecularOrbitals(PT.getTokens(symLine)[1]);
      return true;
    }
    return true;
  }

  /**
   * Reads a set of coordinates
   *
   * @exception Exception  if an I/O error occurs
   */
  private void readCoordinates() throws Exception {

    /*
     * 
 Coordinates (Cartesian)
 =======================

   Atom                      bohr                                 angstrom                 Geometric Variables
                   X           Y           Z              X           Y           Z       (0:frozen, *:LT par.)
 --------------------------------------------------------------------------------------------------------------
   1 XX         .000000     .000000     .000000        .000000     .000000     .000000      0       0       0


OR


 ATOMS
 =====                            X Y Z                    CHARGE
                                (Angstrom)             Nucl     +Core       At.Mass
                       --------------------------    ----------------       -------
    1  Ni              0.0000    0.0000    0.0000     28.00     28.00       57.9353

     * 
     */
    boolean isGeometry = (line.indexOf("G E O M E T R Y") >= 0);
    asc.newAtomSet();
    asc.setAtomSetName("" + energy); // start with an empty name
    discardLinesUntilContains("----");
    int pt0 = (isGeometry ? 2 : 5);
    nXX = 0;
    String[] tokens;
    while (rd() != null && !line.startsWith(" -----")) {
      tokens = getTokens();
      if (tokens.length < 5)
        break;
      String symbol = tokens[1];
      String name = null;
      if (symbol.indexOf(".") >= 0) {
        name = symbol;
        symbol = symbol.substring(0, symbol.indexOf("."));
      }
      if (JmolAdapter.getElementNumber(symbol) < 1)
        nXX++;
      else
        addAtomXYZSymName(tokens, pt0, symbol, name);
    }
  }


  /*
   Vibrations and Normal Modes  ***  (cartesian coordinates, NOT mass-weighted)  ***
   ===========================
   
   The headers on the normal mode eigenvectors below give the Frequency in cm-1
   (a negative value means an imaginary frequency, no output for (almost-)zero frequencies)


   940.906                      1571.351                      1571.351
   ------------------------      ------------------------      ------------------------
   1.XX          .000    .000    .000          .000    .000    .000          .000    .000    .000
   2.N           .000    .000    .115          .008    .067    .000         -.067    .008    .000
   3.H           .104    .180   -.534          .323   -.037   -.231          .580   -.398    .098
   4.H          -.208    .000   -.534          .017   -.757    .030         -.140   -.092   -.249
   5.H           .104   -.180   -.534         -.453   -.131    .201          .485    .378    .151


   ====================================
   */
  /**
   * Reads a set of vibrations.
   *
   * @exception Exception  if an I/O error occurs
   */
  private void readFrequencies() throws Exception {
    rd();
    while (rd() != null) {
      while (rd() != null && line.indexOf(".") < 0
          && line.indexOf("====") < 0) {
      }
      if (line == null || line.indexOf(".") < 0)
        return;
      String[] frequencies = getTokens();
      rd(); // -------- -------- --------
      int iAtom0 = asc.ac;
      int ac = asc.getLastAtomSetAtomCount();
      int frequencyCount = frequencies.length;
      boolean[] ignore = new boolean[frequencyCount];
      for (int i = 0; i < frequencyCount; ++i) {
        ignore[i] = !doGetVibration(++vibrationNumber);
        if (ignore[i])
          continue;
        asc.cloneLastAtomSet();
        asc.setAtomSetFrequency(vibrationNumber, null, null, frequencies[i], null);
      }
      readLines(nXX);
      fillFrequencyData(iAtom0, ac, ac, ignore, true, 0, 0, null, 0, null);
    }
  }
  
  private void readSymmetries() throws Exception {
    /*
 Irreducible Representations, including subspecies
 -------------------------------------------------
 A1
 A2
 B1
 B2
     */
    vSymmetries = new  Lst<SymmetryData>();
    htSymmetries = new Hashtable<String, SymmetryData>();
    rd();
    int index = 0;
    String syms = "";
    while (rd() != null && line.length() > 1)
      syms += line;
    String[] tokens = PT.getTokens(syms);
    for (int i = 0; i < tokens.length; i++) {
      SymmetryData sd = new SymmetryData(index++, tokens[i]);
      htSymmetries.put(tokens[i], sd);
      vSymmetries.addLast(sd);
    }
  }

  private class SymmetryData {
    int index;
    String sym;
    int nSFO;
    int nBF;
    float[][] coefs;
    Map<String, Object>[] mos;
    int[] basisFunctions;
    public SymmetryData(int index, String sym) {
      Logger.info("ADF reader creating SymmetryData " + sym + " " + index);
      this.index = index;
      this.sym = sym;
    }
    
  }
  
  private void readSlaterBasis() throws Exception {
    if (vSymmetries == null)
      return;
    int nBF = 0;
    for (int i = 0; i < vSymmetries.size(); i++) {
      SymmetryData sd = vSymmetries.get(i);
      Logger.info(sd.sym);
      discardLinesUntilContains("=== " + sd.sym + " ===");
      if (line == null) {
        Logger.error("Symmetry slater basis section not found: " + sd.sym);
        return;
      }
    /*
                                      === A1 ===
 Nr. of SFOs :   20
 Cartesian basis functions that participate in this irrep (total number =    32) :
      1     2     3     4     5     8    11    14    20    15
     18    30    23    28    31    43    32    44    33    45
     36    48    34    46    42    54    39    51    37    40
     49    52
     */
      sd.nSFO = parseIntAt(rd(), 15); 
      sd.nBF = parseIntAt(rd(), 75);
      String funcList = "";
      while (rd() != null && line.length() > 1)
        funcList += line;
      String[] tokens = PT.getTokens(funcList);
      if (tokens.length != sd.nBF)
        return;
      sd.basisFunctions = new int[tokens.length];
      for (int j = tokens.length; --j >= 0; ) {
        int n = parseIntStr(tokens[j]);
        if (n > nBF)
          nBF = n;
        sd.basisFunctions[j] = n - 1;
      }
    }
    slaterArray = new SlaterData[nBF];
        /*
     (power of) X  Y  Z  R     Alpha  on Atom
                ==========     =====     ==========

 N                                    1
                                  ---------------------------------------------------------------------------
    Core    0  0  0  0     6.380      1
            0  0  0  1     1.500      2
            0  0  0  1     2.500      3
            0  0  0  1     5.150      4
            1  0  0  0     1.000      5

 H                                    2    3
                                  ---------------------------------------------------------------------------
            0  0  0  0     0.690     31   43
            0  0  0  0     0.920     32   44
            0  0  0  0     1.580     33   45
            1  0  0  0     1.250     34   46

       */
    
    // note, however, that these may continue to the next line as in example adf-2007.out
    
    discardLinesUntilContains("(power of)");
    readLines(2);
    while (rd() != null && line.length() > 3 && line.charAt(3) == ' ') {
      String data = line;
      while (rd().indexOf("---") < 0)
        data += line;
      String[] tokens = PT.getTokens(data);
      int nAtoms = tokens.length - 1;
      int[] atomList = new int[nAtoms];
      for (int i = 1; i <= nAtoms; i++)
        atomList[i - 1] = parseIntStr(tokens[i]) - 1;
      rd();
      while (line.length() >= 10) {
        data = line;
        while (rd().length() > 35 && line.substring(0, 35).trim().length() == 0)
          data += line;
        tokens = PT.getTokens(data);
        boolean isCore = tokens[0].equals("Core");
        int pt = (isCore ? 1 : 0);
        int x = parseIntStr(tokens[pt++]);
        int y = parseIntStr(tokens[pt++]);
        int z = parseIntStr(tokens[pt++]);
        int r = parseIntStr(tokens[pt++]);
        float zeta = parseFloatStr(tokens[pt++]);
        for (int i = 0; i < nAtoms; i++) {
          int ptBF = parseIntStr(tokens[pt++]) - 1;
          slaterArray[ptBF] = new SlaterData(atomList[i], x, y, z, r, zeta, 1);
          slaterArray[ptBF].index = ptBF;
        }
      }
    }
  }

  private void readMolecularOrbitals(String sym) throws Exception {
    /*
    ======  Eigenvectors (rows) in BAS representation

    column           1                   2                   3                   4
    row   
    1    2.97448635016195E-01  7.07156589388012E-01  6.86546190383583E-03 -1.61065890134540E-03
    2   -1.38294969376236E-01 -1.62913073678337E-02 -1.31464541737858E-01  5.35848303329039E-01
    3    3.86427624200707E-02  2.84046375688973E-02  3.66872765902448E-02 -2.21326610798233E-01
     */
    SymmetryData sd = htSymmetries.get(sym);
    if (sd == null)
      return;
    int ptSym = sd.index;
    boolean isLast = (ptSym == vSymmetries.size() - 1);
    int n = 0;
    int nBF = slaterArray.length;
    sd.coefs = new float[sd.nSFO][nBF];
    while (n < sd.nBF) {
      rd();
      int nLine = PT.getTokens(rd()).length;
      rd();
      sd.mos = AU.createArrayOfHashtable(sd.nSFO);
      String[][] data = new String[sd.nSFO][];
      fillDataBlock(data, 0);
      for (int j = 1; j < nLine; j++) {
        int pt = sd.basisFunctions[n++];
        for (int i = 0; i < sd.nSFO; i++)
          sd.coefs[i][pt] = parseFloatStr(data[i][j]);
      }
    }
    for (int i = 0; i < sd.nSFO; i++) {
      Map<String, Object> mo = new Hashtable<String, Object>();
      mo.put("coefficients", sd.coefs[i]);
      //System.out.println(i + " " + Escape.escapeArray(sd.coefs[i]));
      mo.put("id", sym + " " + (i + 1));
      sd.mos[i] = mo;
    }
    if (!isLast)
      return;
    int nSym = htSymmetries.size();

    /*
    Scaled ZORA Orbital Energies, per Irrep and Spin:
    =================================================
                      Occup              E (au)              E (eV)       Diff (eV) with prev. cycle
                                                                          (Including levelshift (eV)
                      -----      --------------------        ------       --------------------------
    A
            32        2.000     -0.74325831542570E+00       -20.225               1.82E-09
            33        2.000     -0.45059367525444E+00       -12.261              -5.41E-10
            34        2.000     -0.36437125017094E+00        -9.915              -3.42E-09
            35        2.000     -0.36433628606164E+00        -9.914               1.26E-09
            36        2.000     -0.32374039794317E+00        -8.809              -1.16E-09
            37        2.000     -0.30337693645922E+00        -8.255              -3.54E-09
            38        2.000     -0.30336321188240E+00        -8.255               8.49E-10
            39        2.000     -0.30216236922833E+00        -8.222              -4.25E-09
            40        2.000     -0.30213095951623E+00        -8.221               1.49E-09
            41        2.000     -0.29100668702532E+00        -7.919              -1.44E-09
            42        0.000     -0.11635248827995E+00        -3.166  (      -3.139)
            43        0.000     -0.76614528258569E-01        -2.085  (      -2.058)
            44        0.000     -0.25381641140969E-02        -0.069  (      -0.042)
            45        0.000     -0.23982740441974E-02        -0.065  (      -0.038)
            46        0.000      0.13429238347442E-01         0.365  (       0.393)
            47        0.000      0.52569195633736E-01         1.430  (       1.458)
            48        0.000      0.57620545273285E-01         1.568  (       1.595)
            49        0.000      0.57659418273279E-01         1.569  (       1.596)
            50        0.000      0.17278009002144E+00         4.702  (       4.729)
            51        0.000      0.19150012638228E+00         5.211  (       5.238)
     */
    /*
    Orbital Energies, all Irreps
    ========================================

    Irrep        no.  (spin)   Occup              E (au)                E (eV)
    ---------------------------------------------------------------------------
    A1            1             2.00       -0.18782651837132E+02      -511.1020
    A1            2             2.00       -0.93500325051330E+00       -25.4427
     */
    discardLinesUntilContains(nSym == 1 ? "Orbital Energies, per Irrep"
        : "Orbital Energies, all Irreps");
    readLines(4);
    int pt = (nSym == 1 ? 0 : 1);
    if (nSym == 1)
      sym = rd().trim();
    while (rd() != null && line.length() > 10) {
      line = line.replace('(', ' ').replace(')', ' ');
      String[] tokens = getTokens();
      int len = tokens.length;
      if (nSym > 1)
        sym = tokens[0];
      int moPt = parseIntStr(tokens[pt]);
      // could be spin here?
      float occ = parseFloatStr(tokens[len - 4 + pt]);
      float energy = parseFloatStr(tokens[len - 2 + pt]); // eV
      addMo(sym, moPt, occ, energy);
    }
    int iAtom0 = asc.getLastAtomSetAtomIndex();
    for (int i = 0; i < nBF; i++)
      slaterArray[i].atomNo += iAtom0 + 1;
    setSlaters(true);
    sortOrbitals();
    setMOs("eV");
  }

  private void addMo(String sym, int moPt, float occ, float energy) {
    SymmetryData sd = htSymmetries.get(sym);
    if (sd == null) {
      for (Map.Entry<String, SymmetryData> entry : htSymmetries.entrySet())
        if (entry.getKey().startsWith(sym + ":")) {
          sd = entry.getValue();
          break;
        }
      if (sd == null)
        return;
    }
    Map<String, Object> mo = sd.mos[moPt - 1];
    mo.put("occupancy", Float.valueOf(occ > 2 ? 2 : occ));
    mo.put("energy", Float.valueOf(energy)); //eV
    mo.put("symmetry", sd.sym + "_" + moPt);
    setMO(mo);
  }  
}
