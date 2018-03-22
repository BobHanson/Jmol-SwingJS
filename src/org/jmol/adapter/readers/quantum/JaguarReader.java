/* $RCSfile$
 * $Author: nicove $
 * $Date: 2006-08-30 13:20:20 -0500 (Wed, 30 Aug 2006) $
 * $Revision: 5447 $
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

package org.jmol.adapter.readers.quantum;

import org.jmol.util.Logger;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;

import java.util.Hashtable;

import java.util.Map;

/**
 * Jaguar reader tested for the two samples files in CVS. Both
 * these files were created with Jaguar version 4.0, release 20.
 * MO reader corrected 9/28/11 by Bob Hanson -- reading NORMALIZED set
 * TODO: slight question about application of SQRT(3) in XY XZ YZ set
 *       if that turns out to be an issue, we can multiply coefficients
 */
public class JaguarReader extends MOReader {

  private int moCount = 0;
  private float lumoEnergy = Float.MAX_VALUE;

  /**
   * @return true if need to read new line
   * @throws Exception
   * 
   */
  @Override
  protected boolean checkLine() throws Exception {
    if (line.startsWith(" Input geometry:")
        || line.startsWith(" Symmetrized geometry:")
        || line.startsWith("  final geometry:")) {
      readAtoms();
      return true;
    }
    if (line.startsWith("  Atomic charges from electrostatic potential:")) {
      readCharges();
      return true;
    }
    if (line.startsWith("  number of basis functions....")) {
      moCount = parseIntAt(line, 32);
      return true;
    }
    if (line.startsWith("  basis set:")) {
      moData.put("energyUnits", "");
      moData.put("calculationType", calculationType = line.substring(13).trim());
      if ("sto-3g".equals(calculationType)) {
        Logger.error("STO-3G not supported for Jaguar -- unusual SP basis definition.");
      }
      return true;
    }
    if (line.indexOf("XXXXXShell information") >= 0) { // disabled -- need normalized set
      readUnnormalizedBasis();
      return true;
    }

    if (line.indexOf("Normalized coefficients") >= 0) {
      if (!"sto-3g".equals(calculationType))
      readBasisNormalized();
      return true;
    }
    if (line.startsWith(" LUMO energy:")) {
      lumoEnergy = parseFloatStr(line.substring(13));
      return true;
    }
    if (line.indexOf("final wvfn") >= 0) {
      if (shells != null)
        readJaguarMolecularOrbitals();
      return true;
    }
    if (line.startsWith("  harmonic frequencies in")) {
      readFrequencies();
      continuing = false;
      return false;
    }
    return checkNboLine();
  }

  private void readAtoms() throws Exception {
    // we only take the last set of atoms before the frequencies
    discardPreviousAtoms();
    // start parsing the atoms
    readLines(2);
    while (rd() != null && line.length() >= 60 && line.charAt(2) != ' ') {
      String[] tokens = getTokens();
      String atomName = tokens[0];
      if (atomName.length() < 2)
        return;
      char ch2 = atomName.charAt(1);
      String elementSymbol = (ch2 >= 'a' && ch2 <= 'z' ? atomName.substring(0, 2) : atomName.substring(0, 1));
      addAtomXYZSymName(tokens, 1, elementSymbol, atomName);
    }
  }

  /*

 Atom       C1           H2           H3           H4           O5      
 Charge    0.24969      0.04332     -0.02466     -0.02466     -0.65455
   
 Atom       H6      
 Charge    0.41085
  
   */
  private void readCharges() throws Exception {
   int iAtom = 0;
    while (rd() != null && line.indexOf("sum") < 0) {
      if (line.indexOf("Charge") < 0)
        continue;
      String[] tokens = getTokens();
      for (int i = 1; i < tokens.length; i++)
        asc.atoms[iAtom++].partialCharge = parseFloatStr(tokens[i]);
    }
  }

  /*

   Gaussian Functions - Shell information
   
   s    j
   h    c  i       n
   e    o  s       f
   l    n  h       s
   atom       l    t  l  l    h          z              coef            rcoef
   --------    ---  --- -- --  ---     ----------      ----------       ---------
   C1          1    3  0  1    0      71.6168373       0.1543290       2.7078144
   C1          2   -1  0  1    0      13.0450963       0.5353281       2.6188802
   C1          3   -1  0  1    0       3.5305122       0.4446345       0.8161906
   C1          4    2  2  1    1       2.9412494      -0.2956454      -0.4732386
   C1          5   -4  2  1    1       0.6834831       1.1815287       0.6329949
   C1          6    2 -1  2    2       2.9412494       0.2213487       1.2152952
   C1          7   -6 -1  2    2       0.6834831       0.8627064       0.7642102
   C1          8    1  1  1    1       0.2222899       1.0000000       0.2307278
   C1          9    1 -1  2    2       0.2222899       1.0000000       0.2175654


   */
  
  private final static float ROOT3 = 1.73205080756887729f;
  
  private void readUnnormalizedBasis() throws Exception {
    String lastAtom = "";
    int iAtom = 0;
    int[][] sdata = new int[moCount][4];
    Lst<float[]>[] sgdata = AU.createArrayOfArrayList(moCount);
    String[] tokens;
    gaussianCount = 0;

    // trouble is that these can be out of order!

    discardLinesUntilContains("--------");
    while (rd() != null && (tokens = getTokens()).length == 9) {
      int jCont = parseIntStr(tokens[2]);
      if (jCont > 0) {
        if (!tokens[0].equals(lastAtom))
          iAtom++;
        lastAtom = tokens[0];
        int iFunc = parseIntStr(tokens[5]);
        int iType = parseIntStr(tokens[4]);
        if (iType <= 2)
          iType--; // s,p --> 0,1 because SP is 2
        if (sgdata[iFunc] == null) {
          sdata[iFunc][0] = iAtom;
          sdata[iFunc][1] = iType;
          sdata[iFunc][2] = 0; //pointer
          sdata[iFunc][3] = 0; //count
          sgdata[iFunc] = new  Lst<float[]>();
        }
        float factor = 1;//(iType == 3 ? 1.73205080756887729f : 1);
        //System.out.println("slater: " + iAtom + " " + iType + " " + gaussianCount + " " + nGaussians);
        sgdata[iFunc].addLast(new float[] { parseFloatStr(tokens[6]),
            parseFloatStr(tokens[8]) * factor });
        gaussianCount += jCont;
        for (int i = jCont - 1; --i >= 0;) {
          tokens = PT.getTokens(rd());
          sgdata[iFunc].addLast(new float[] { parseFloatStr(tokens[6]),
              parseFloatStr(tokens[8]) * factor });
        }
      }
    }
    float[][] garray = AU.newFloat2(gaussianCount);
    Lst<int[]> sarray = new  Lst<int[]>();
    gaussianCount = 0;
    for (int i = 0; i < moCount; i++)
      if (sgdata[i] != null) {
        int n = sgdata[i].size();
        sdata[i][2] = gaussianCount;
        sdata[i][3] = n;
        for (int j = 0; j < n; j++) {
          garray[gaussianCount++] = sgdata[i].get(j);
        }
        sarray.addLast(sdata[i]);
      }
    moData.put("shells", sarray);
    moData.put("gaussians", garray);
    if (debugging) {
      Logger.debug(sarray.size() + " slater shells read");
      Logger.debug(gaussianCount + " gaussian primitives read");
    }
  }

  /*
     Gaussian Functions - Normalized coefficients
     
     s
     h    t
     e    y
     l    p    f
     atom       l    e    n          z          rcoef        rmfac     rcoef*rmfac
     --------    ---  ---  ---     ---------    ----------   ----------  -----------
     C1          1    S    1    3047.524880     0.536345     1.000000     0.536345
     C1          2    S    1     457.369518     0.989452     1.000000     0.989452
     C1          3    S    1     103.948685     1.597283     1.000000     1.597283
     C1          4    S    1      29.210155     2.079187     1.000000     2.079187
     C1          5    S    1       9.286663     1.774174     1.000000     1.774174
     C1          6    S    1       3.163927     0.612580     1.000000     0.612580
     C1          7    S    2       7.868272    -0.399556     1.000000    -0.399556
     C1          8    S    2       1.881289    -0.184155     1.000000    -0.184155
     C1          9    S    2       0.544249     0.516390     1.000000     0.516390
     C1         10    X    3       7.868272     1.296082     1.000000     1.296082
                      Y    4                                 1.000000     1.296082
                      Z    5                                 1.000000     1.296082
     C1         11    X    3       1.881289     0.993754     1.000000     0.993754

     */
  private void readBasisNormalized() throws Exception {
// if (true) return;
    String lastAtom = "";
    int iAtom = 0;
    String id;
    int iFunc = 0;
    int iFuncLast = -1;
    Lst<int[]> sarray = new  Lst<int[]>();
    Lst<float[]> gdata = new  Lst<float[]>();
    gaussianCount = 0;
    int[] sdata = null;
    discardLinesUntilContains("--------");
    while (rd() != null && line.length() > 3) {
      String[] tokens = getTokens();
      if (tokens.length == 4) { //continuation
        id = tokens[0];
        continue;
      }
      if (!tokens[0].equals(lastAtom))
        iAtom++;
      lastAtom = tokens[0];
      id = tokens[2];
      int iType = BasisFunctionReader.getQuantumShellTagID(id);
      iFunc = parseIntStr(tokens[3]) - 1;
      int gPtr = gdata.size();
      if (iFunc == iFuncLast) {
        sdata[3]++;
      } else if (iFunc < iFuncLast) {
        // out of order!
        for (int i = gdata.size(); --i >= 0;) {
          if (gdata.get(i)[2] == iFunc) {
            gPtr = i + 1;
            break;            
          }
        }
        for (int i = sarray.size(); --i >= 0;) {
          if (sarray.get(i)[4] == iFunc) {
            sarray.get(i)[3]++;
            while (++i < sarray.size()) {
              sarray.get(i)[2]++;
            }
            break;
          }
        }
      } else {
        sdata = new int[] { iAtom, iType, gaussianCount + 1, 1, iFunc };
        sarray.addLast(sdata);
        iFuncLast = iFunc;
      }
      gaussianCount++;
      float z = parseFloatStr(tokens[4]);
      float rCoef = parseFloatStr(tokens[5]);
      if (id.equals("XX"))
        rCoef *= ROOT3;
      gdata.add(gPtr, new float[] { z, rCoef, iFunc});
    }

    float[][] garray = AU.newFloat2(gaussianCount);
    for (int i = gdata.size(); --i >= 0;)
      garray[i] = gdata.get(i);
    moData.put("shells", shells = sarray);
    moData.put("gaussians", garray);
    if (debugging) {
      Logger.debug(sarray.size() + " slater shells read");
      Logger.debug(gaussianCount + " gaussian primitives read");
    }
    moData.put("isNormalized", Boolean.TRUE);
  }

  /*

   Occupied + virtual Orbitals- final wvfn
   
   ***************************************** 
   
   
   1         2         3         4         5
   eigenvalues-            -20.56138 -11.27642  -1.35330  -0.91170  -0.68016
   1 C1               S    0.00002   0.99583  -0.07294   0.17630   0.01918
   2 C1               S   -0.00028   0.02695   0.13608  -0.34726  -0.03173
   3 C1               X   -0.00014   0.00018   0.02808   0.00925   0.23168
   4 C1               Y    0.00033  -0.00073  -0.09792  -0.06147  -0.12659
   5 C1               Z    0.00003  -0.00013  -0.01570  -0.01416   0.08005

   
   
   */

  private void readJaguarMolecularOrbitals() throws Exception {
    String[][] dataBlock = new String[moCount][];
    rd();
    rd();
    rd();
    int nMo = 0;
    while (line != null) {
      rd();
      rd();
      rd();
      if (line == null || line.indexOf("eigenvalues-") < 0)
        break;
      String[] eigenValues = getTokens();
      int n = eigenValues.length - 1;
      fillDataBlock(dataBlock, 0);
      float occ = 2;
      for (int iOrb = 0; iOrb < n; iOrb++) {
        float[] coefs = new float[moCount];
        Map<String, Object> mo = new Hashtable<String, Object>();
        float energy = parseFloatStr(eigenValues[iOrb + 1]);
        mo.put("energy", Float.valueOf(energy));
        if (Math.abs(energy - lumoEnergy) < 0.0001) {
          moData.put("HOMO", Integer.valueOf(nMo));
          lumoEnergy = Float.MAX_VALUE;
          occ = 0;
        }
        //TODO: SOMO? 
        mo.put("occupancy", Float.valueOf(occ));
        nMo++;
        for (int i = 0, pt =0; i < moCount; i++) {
          //String type = dataBlock[i][2];
          //char ch = type.charAt(0);
          //if (!isQuantumBasisSupported(ch))
            //continue;
          coefs[pt++] = parseFloatStr(dataBlock[i][iOrb + 3]);
        }
        mo.put("coefficients", coefs);
        setMO(mo);
      }
    }
    moData.put("mos", orbitals);
    finalizeMOData(moData);
  }

  /* A block without symmetry, looks like:

   harmonic frequencies in cm**-1, IR intensities in km/mol, and normal modes:
   
   frequencies  1350.52  1354.79  1354.91  1574.28  1577.58  3047.10  3165.57
   intensities    14.07    13.95    13.92     0.00     0.00     0.00    25.19
   C1   X     0.00280 -0.11431  0.01076 -0.00008 -0.00001 -0.00028 -0.00406
   C1   Y    -0.00528  0.01062  0.11423 -0.00015 -0.00001 -0.00038  0.00850
   C1   Z     0.11479  0.00330  0.00502 -0.00006  0.00000  0.00007 -0.08748
   
   With symmetry:
   
   harmonic frequencies in cm**-1, IR intensities in km/mol, and normal modes:
   
   frequencies  1352.05  1352.11  1352.16  1574.91  1574.92  3046.33  3164.52
   symmetries   B3       B1       B3       A        A        A        B1      
   intensities    14.01    14.00    14.00     0.00     0.00     0.00    25.06
   C1   X     0.08399 -0.00233 -0.07841  0.00000  0.00000  0.00000 -0.01133
   C1   Y     0.06983 -0.05009  0.07631 -0.00001  0.00000  0.00000 -0.00283
   C1   Z     0.03571  0.10341  0.03519  0.00001  0.00000  0.00001 -0.08724
   */

  private void readFrequencies() throws Exception {
    int ac = asc.getLastAtomSetAtomCount();
    discardLinesUntilStartsWith("  frequencies ");
    while (line != null && line.startsWith("  frequencies ")) {
      int iAtom0 = asc.ac;
      String[] frequencies = getTokens();
      int frequencyCount = frequencies.length - 1;
      boolean[] ignore = new boolean[frequencyCount];
      // skip to "intensity" or "force" line
      String[] symmetries = null;
      String[] intensities = null;
      while (line != null && line.charAt(2) != ' ') {
        if (line.indexOf("symmetries") >= 0)
          symmetries = getTokens();
        else if (line.indexOf("intensities") >= 0)
          intensities = getTokens();
        rd();
      }
      for (int i = 0; i < frequencyCount; i++) {
        ignore[i] = !doGetVibration(++vibrationNumber);
        if (ignore[i]) 
          continue;
        asc.cloneFirstAtomSet(0);
        asc.setAtomSetFrequency(vibrationNumber, null, symmetries == null ? null : symmetries[i + 1], frequencies[i + 1], null);
        if (intensities != null)
          asc.setAtomSetModelProperty("IRIntensity",
              intensities[i + 1] + " km/mol");
      }
      haveLine = true;
      fillFrequencyData(iAtom0, ac, ac, ignore, false, 0, 0, null, 0, null);
      rd();
      rd();
    }
  }
  
  private boolean haveLine;

  @Override
  public String rd() throws Exception {
    if (!haveLine)
      return super.rd();
    haveLine = false;
    return line;
  }
}
