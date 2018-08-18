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


import org.jmol.adapter.smarter.Atom;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;

import java.util.Hashtable;

import java.util.Map;

import org.jmol.api.JmolAdapter;
import org.jmol.util.Logger;

/**
 * Reader for Psi3 output files. -- http://www.psicode.org/
 * 
 * preliminary version:
 *  -- coordinates only
 *  -- final geometry only; not reading steps
 *  -- no charges
 *  -- no frequencies
 *  -- no orbitals (Can't handle irreducible representations here.)
 *  
 *  -- not processing specified model option in LOAD command
 *  
 **/
public class PsiReader extends MOReader {

 /**
   * @return true if need to read new line
   * @throws Exception
   * 
   */
  @Override
  protected boolean checkLine() throws Exception {
    if (line
        .indexOf("-Geometry after Center-of-Mass shift and reorientation (a.u.):") >= 0) {
      readAtoms(true); // initial geometry
      doProcessLines = true;
      return true;
    }
    if (line
        .indexOf("-Unique atoms in the canonical coordinate system (a.u.):") >= 0) {
      readUniqueAtoms();
      doProcessLines = true;
      return true;
    }
    if (!doProcessLines)
      return true;
    if (line.indexOf("New Cartesian Geometry in a.u.") >= 0) {
      readAtoms(false); // replaced with final geometry
      return true;
    }
    if (line.startsWith("  label        = ")) {
      moData.put("calculationType", calculationType = line.substring(17).trim());
      return true;
    }
    if (line.startsWith("molecular orbitals for ")) {
      moData.put("energyUnits", "");
      return true;
    }
    if (line.startsWith("  -BASIS SETS:")) {
      readBasis();
      return true;
    }
    if (line.indexOf("Molecular Orbital Coefficients") >= 0) {
      // note -- no irreducible representation support
      if (filterMO())
        readPsiMolecularOrbitals();
      return true;
    }
    if (line.indexOf("SCF total energy   =") >= 0) {
      readSCFDone();
      return true;
    }
    if (line.indexOf("Normal Modes") >= 0) {
      readFrequencies();
      return true;
    }
    return checkNboLine();
  }

  /**
   * Interprets the SCF Done: section.
   *
   * @throws Exception If an error occurs
   **/
  private void readSCFDone() throws Exception {
    asc.setAtomSetName(line);
  }

  /* atom locations -- preliminary and final
   * 
   -Geometry after Center-of-Mass shift and reorientation (a.u.):
   Center              X                  Y                   Z
   ------------   -----------------  -----------------  -----------------
   OXYGEN     -0.000000000000    -0.129476880255     0.000000000000
   HYDROGEN      1.494186636402     1.027446024483     0.000000000000
   HYDROGEN     -1.494186636402     1.027446024483     0.000000000000
   *
   */
  /*
   New Cartesian Geometry in a.u.
   8.0   0.0000000000   0.0000000000  -0.1223632565
   1.0   0.0000000000   1.3972759189   0.9709968388
   1.0   0.0000000000  -1.3972759189   0.9709968388
   */

  Lst<String> atomNames = new  Lst<String>();
  private void readAtoms(boolean isInitial) throws Exception {
    if (isInitial) {
      asc.newAtomSet();
      asc.setAtomSetName(""); // start with an empty name
      discardLinesUntilContains("----");
    }
    int atomPt = 0;
    while (rd() != null && line.length() > 0) {
      String[] tokens = getTokens(); // get the tokens in the line
      Atom atom = (isInitial ? asc.addNewAtom()
          : asc.atoms[atomPt++]);
      if (isInitial) {
        atomNames.addLast(tokens[0]);
        if (tokens[0].length() <= 2)
          atom.elementNumber = (short) JmolAdapter.getElementNumber(tokens[0]);
      } else {
        atom.elementNumber = (short) parseIntStr(tokens[0]);
      }
      if (atom.elementNumber < 0)
        atom.elementNumber = 0; // dummy atoms have -1 -> 0
      setAtomCoordScaled(atom, tokens, 1, ANGSTROMS_PER_BOHR);
    }
  }

  /* SAMPLE BASIS OUTPUT */
  /*
   -BASIS SETS:

   -Basis set on unique center 1:
   ( (S (  5484.67166000     0.00183107)
   (   825.23494600     0.01395017)
   (   188.04695800     0.06844508)
   (    52.96450000     0.23271434)
   (    16.89757040     0.47019290)
   (     5.79963534     0.35852085) )
   (S (    15.53961625    -0.11077755)
   (     3.59993359    -0.14802626)
   (     1.01376175     1.13076701) )
   (S (     0.27000582     1.00000000) )
   (P (    15.53961625     0.07087427)
   (     3.59993359     0.33975284)
   (     1.01376175     0.72715858) )
   (P (     0.27000582     1.00000000) )
   (D (     0.80000000     1.00000000) )
   )

   -Basis set on unique center 2:
   ( (S (    18.73113696     0.03349460)
   (     2.82539437     0.23472695)
   (     0.64012169     0.81375733) )
   (S (     0.16127776     1.00000000) )
   (P (     1.10000000     1.00000000) )
   )

   */

  
  Lst<Lst<int[]>> shellsByUniqueAtom = new  Lst<Lst<int[]>>();
  void readBasis() throws Exception {
    Lst<String[]> gdata = new  Lst<String[]>();
    //ac = -1;
    gaussianCount = 0;
    shellCount = 0;
    String[] tokens;
    int[] slater = null;
    Lst<int[]> slatersByUniqueAtom = null;
    rd();
    while (rd() != null && line.startsWith("   -Basis set on")) {
      slatersByUniqueAtom = new  Lst<int[]>();
      int nGaussians = 0;
      while (rd() != null && !line.startsWith("       )")) {
        line = line.replace('(', ' ').replace(')',' ');
        tokens = getTokens();
        int ipt = 0;
        switch (tokens.length) {
        case 3:
          if (slater != null) {
            slatersByUniqueAtom.addLast(slater);
          }
          ipt = 1;
          slater = new int[3];
          slater[0] = BasisFunctionReader.getQuantumShellTagID(tokens[0]);
          slater[1] = gaussianCount;
          shellCount++;
          break;
        case 2:
          break;
        }
        nGaussians++;
        gdata.addLast(new String[] { tokens[ipt], tokens[ipt + 1] });
        slater[2] = nGaussians;
      }
      if (slater != null) {
        slatersByUniqueAtom.addLast(slater);
      }
      shellsByUniqueAtom.addLast(slatersByUniqueAtom);
      gaussianCount += nGaussians;
      rd();
    }
    float[][] garray = AU.newFloat2(gaussianCount);
    for (int i = 0; i < gaussianCount; i++) {
      tokens = gdata.get(i);
      garray[i] = new float[tokens.length];
      for (int j = 0; j < tokens.length; j++)
        garray[i][j] = parseFloatStr(tokens[j]);
    }
    moData.put("gaussians", garray);
    if (debugging) {
      Logger.debug(shellCount + " slater shells read");
      Logger.debug(gaussianCount + " gaussian primitives read");
    }
  }

  /*
   *        Center              X                  Y                   Z
    ------------   -----------------  -----------------  -----------------
          OXYGEN      0.000000000000    -0.000000000000    -0.129476880255
        HYDROGEN      0.000000000000     1.494186636402     1.027446024483

 */
  
  Map<String, Integer> uniqueAtomMap = new Hashtable<String, Integer>();
  private void readUniqueAtoms() throws Exception {
    Lst<int[]> sdata = new  Lst<int[]>();
    discardLinesUntilContains("----");
    int n = 0;
    while (rd() != null && line.length() > 0) {
      String[] tokens = getTokens(); // get the tokens in the line
      uniqueAtomMap.put(tokens[0], Integer.valueOf(n++));
    }
    int ac = atomNames.size();
    for (int i = 0; i < ac; i++) {
      String atomType = atomNames.get(i);
      int iUnique = uniqueAtomMap.get(atomType).intValue();
      Lst<int[]> slaters = shellsByUniqueAtom.get(iUnique);
      if (slaters == null) {
        Logger.error("slater for atom " + i + " atomType " + atomType
            + " was not found in listing. Ignoring molecular orbitals");
        return;
      }
      for (int j = 0; j < slaters.size(); j++) {
        int[] slater = slaters.get(j);
        sdata.addLast(new int[] { i + 1, slater[0], slater[1], slater[2] });
        //System.out.println(atomType + " " + i + " " + slater[0] + " " + slater[1] + " "+ slater[2]);
          
      }
    }
    moData.put("shells", sdata);

  }
  
  /*

 molecular orbitals for irrep A1 

           1           2           3           4           5           6           7           8           9          10

    1   0.9947150   0.2114006  -0.0714069  -0.0988706   0.0407654  -0.0174813   0.0803902   0.0272910   0.0420888  -0.0137728
    2   0.0209638  -0.4782149   0.1593362   0.0624875  -0.2899886  -0.9045706   1.3308498   0.2975508   0.5216148   0.1514479
    3   0.0040475  -0.4381721   0.3284674   1.3270493   0.5227552   1.6922720  -3.3203402  -0.8173616  -1.2076145  -0.1468854
    4   0.0014148  -0.0758964  -0.5492784   0.2325008  -0.4621264   0.6520880   0.4590942   0.0837639   0.2357725   0.0564790
    5  -0.0003139  -0.0342955  -0.3909115   0.5170726   0.2725637  -0.7911736  -1.0744616  -0.5012396  -0.5803915  -0.4266790
    6  -0.0038273  -0.0012515   0.0091957  -0.0673908  -0.1939281  -0.3373102   0.2803627  -0.2459803   1.0424623   0.0313366
    7  -0.0038656  -0.0077122  -0.0010027  -0.0515324   0.2220243  -0.3053820   0.6039044  -0.2638198  -0.4852683   0.7410745
    8  -0.0038406  -0.0088777  -0.0368443  -0.0388375   0.0155930  -0.3518008   0.3307708   0.7906442  -0.0434744  -0.8839197
    9   0.0001758  -0.1328286  -0.1466865  -0.0640810   0.6955775   0.2660928   0.3491620   0.2305190   0.5703285   0.0726513
   10  -0.0002766  -0.0150091  -0.1032895  -1.0056171  -0.6399829  -0.2872141   0.6990829   0.1301674   0.0565621   0.0475849
   11   0.0002289   0.0197834   0.0149388   0.0024602   0.1701635  -0.0368803   0.0121333  -0.1874604   0.1623632  -0.4377729
   12   0.0001721   0.0128358  -0.0049166   0.0019170   0.0577778  -0.0519996  -0.1235310   0.2576273   0.1764125   0.6440154

      -20.5681443  -1.3194670  -0.5611060   0.2025392   1.0516162   1.1303012   1.4076901   1.8176092   2.5120325   3.2940311

        2.0000000   2.0000000   2.0000000   0.0000000   0.0000000   0.0000000   0.0000000   0.0000000   0.0000000   0.0000000

          11          12

    1   0.2259298  -0.4061472
    2  -0.1101846   0.3157444
    3  -2.5963980   2.6139959
    4  -0.4038191  -0.3747028
    5  -0.8263162  -0.0747695
    6   1.1326884  -1.1809845
    7  -0.0958127  -1.8747295
    8   0.2457677  -1.5566384
    9   0.7541966   0.5435187
   10   0.5236998  -0.2796098
   11  -0.7683741  -0.4264278
   12  -0.4724594  -0.3672793

        3.6206477   4.0830283

        0.0000000   0.0000000

 molecular orbitals for irrep A2 

           1           2

    1  -0.6754549  -0.8616489
    2  -0.3633895   0.6868267

        1.7998064   2.9292018

        0.0000000   0.0000000

 molecular orbitals for irrep B1 

           1           2           3           4

    1  -0.6401716  -0.9625381  -0.0024268   0.0102944
    2  -0.5007057   1.0339235  -0.2062399  -0.3965559
    3  -0.0266517   0.0151565   0.7809915  -0.7188309
    4  -0.0192662   0.0039871   0.3130397   0.7405846

       -0.4943889   1.1690877   1.9237355   2.9383072

        2.0000000   0.0000000   0.0000000   0.0000000

 molecular orbitals for irrep B2 

           1           2           3           4           5           6           7

    1   0.4962934   0.3417920   0.1841270  -0.8946398  -0.0831261   0.3628566   0.6012427
    2   0.2859073   0.8303785   0.2276947   1.7225933   0.5021038  -0.3977076   1.2754987
    3   0.0338300   0.0308523  -0.2679660   0.0987811  -0.0769386  -0.7572994   1.4477069
    4   0.2344526  -0.0622036  -0.7797987  -0.1310535  -0.1493691   0.4866326  -1.0857157
    5   0.1419344  -1.3226295   0.7157855  -0.8638581  -0.0763895  -0.2110304  -0.4835887
    6  -0.0114336   0.0119594  -0.0491207   0.1703041  -0.4911720   0.2970473   0.7840565
    7  -0.0180772   0.0052447  -0.1008887   0.1067037   0.5623551   0.2835558   0.6708437

       -0.6811047   0.2930006   0.9860211   1.2953253   2.5293580   2.7157483   3.8700721

        2.0000000   0.0000000   0.0000000   0.0000000   0.0000000   0.0000000   0.0000000

Orbital energies (a.u.):

  Doubly occupied orbitals
   1A1    -20.568144     2A1     -1.319467     1B2     -0.681105  
   3A1     -0.561106     1B1     -0.494389  


  Unoccupied orbitals
   4A1      0.202539     2B2      0.293001     3B2      0.986021  
   5A1      1.051616     6A1      1.130301     2B1      1.169088  
   4B2      1.295325     7A1      1.407690     1A2      1.799806  
   8A1      1.817609     3B1      1.923735     9A1      2.512033  
   5B2      2.529358     6B2      2.715748     2A2      2.929202  
   4B1      2.938307    10A1      3.294031    11A1      3.620648  
   7B2      3.870072    12A1      4.083028  


   */
  void readPsiMolecularOrbitals() throws Exception {
    
    //TODO: This reader will fail for G orbitals
    //TODO: No way to check order
    
    Map<String, Object>[] mos = AU.createArrayOfHashtable(5);
    Lst<String>[] data = AU.createArrayOfArrayList(5);
    int nThisLine = 0;
    while (rd() != null && line.toUpperCase().indexOf("DENS") < 0) {
      String[] tokens = getTokens();
      int ptData = (line.charAt(5) == ' ' ? 2 : 4);
      if (line.indexOf("                    ") == 0) {
        addMOData(nThisLine, data, mos);
        nThisLine = tokens.length;
        tokens = PT.getTokens(rd());
        for (int i = 0; i < nThisLine; i++) {
          mos[i] = new Hashtable<String, Object>();
          data[i] = new  Lst<String>();
          mos[i].put("symmetry", tokens[i]);
        }
        tokens = getStrings(rd().substring(21), nThisLine, 10);
        for (int i = 0; i < nThisLine; i++) {
          mos[i].put("energy", Float.valueOf(tokens[i]));
        }
        continue;
      }
      try {
        for (int i = 0; i < nThisLine; i++) {
          data[i].addLast(tokens[i + ptData]);
        }
      } catch (Exception e) {
        Logger.error("Error reading Psi3 file molecular orbitals at line: "
            + line);
        break;
      }
    }
    addMOData(nThisLine, data, mos);
    moData.put("mos", orbitals);
    finalizeMOData(moData);
  }

  private void readFrequencies() throws Exception {
    rd();
    int ac = asc.getLastAtomSetAtomCount();
    String tokens[];
    while (rd() != null && line.indexOf("Frequency") >= 0) {
      tokens = getTokens();
      int iAtom0 = asc.ac;
      boolean[] ignore = new boolean[1];
      if (!doGetVibration(++vibrationNumber))
        continue;
      asc.cloneLastAtomSet();
      asc.setAtomSetFrequency(vibrationNumber, null, null, tokens[1], null);
      readLines(2);
      fillFrequencyData(iAtom0, ac, ac, ignore, true, 0, 0, null, 0, null);
      rd();
    }
  }


}
