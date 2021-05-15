/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-16 14:11:08 -0500 (Sat, 16 Sep 2006) $
 * $Revision: 5569 $
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
 * US modifications by Albert DeFusco - adefusco and Bob Hanson 12/2/2008
 * 
 */

package org.jmol.adapter.readers.quantum;


import javajs.util.Lst;
import javajs.util.PT;


import org.jmol.adapter.smarter.Atom;
import org.jmol.util.Logger;
import javajs.util.V3;

public class GamessUSReader extends GamessReader {

  private boolean lowdenCharges;
  /*
  ------------------
  MOLECULAR ORBITALS
  ------------------

  ------------
  EIGENVECTORS
  ------------

  1          2          3          4          5
  -79.9156   -20.4669   -20.4579   -20.4496   -20.4419
  A          A          A          A          A   
  1  C  1  S   -0.000003  -0.000029  -0.000004   0.000011   0.000016
  2  C  1  S   -0.000009   0.000140   0.000001   0.000057   0.000065
  3  C  1  X    0.000007  -0.000241  -0.000022  -0.000010  -0.000061
  4  C  1  Y   -0.000008   0.000017  -0.000027  -0.000010   0.000024
  5  C  1  Z    0.000007   0.000313   0.000009  -0.000002  -0.000001
  6  C  1  S    0.000049   0.000875  -0.000164  -0.000521  -0.000440
  7  C  1  X   -0.000066   0.000161   0.000125   0.000034   0.000406
  8  C  1  Y    0.000042   0.000195  -0.000165  -0.000254  -0.000573
  9  C  1  Z    0.000003   0.000045   0.000052   0.000112  -0.000129
  10  C  1 XX   -0.000010   0.000010  -0.000040   0.000019   0.000045
  11  C  1 YY   -0.000010  -0.000031   0.000000  -0.000003   0.000019
  ...

  6          7          8          9         10
  -20.4354   -20.4324   -20.3459   -20.3360   -11.2242
  A          A          A          A          A   
  1  C  1  S    0.000000  -0.000001   0.000001   0.000000   0.008876
  2  C  1  S   -0.000003   0.000002   0.000003   0.000002   0.000370

  ...
  TOTAL NUMBER OF BASIS SET SHELLS             =  101

  */

  @Override
  protected void initializeReader() throws Exception {
    lowdenCharges = checkAndRemoveFilterKey("CHARGE=LOW");
    super.initializeReader();
  }
  
  /**
   * @return true if need to read new line
   * @throws Exception
   * 
   */
  @Override
  protected boolean checkLine() throws Exception {
    
    if (line.startsWith(" $DATA"))
      return readInputDeck();

    if (line.indexOf("***************") >= 0)
      Logger.info(rd());
    boolean isBohr;
    
    if (line.indexOf("FINAL ENERGY IS") >= 0 || 
        line.indexOf("TOTAL ENERGY = ") >= 0 ||
        line.indexOf("FINAL RHF ENERGY IS") >= 0 ||
        line.indexOf("E(MP2)=") >= 0 ||
        line.indexOf("COUPLED-CLUSTER ENERGY E(CCSD) =") >= 0 ||
        line.indexOf("COUPLED-CLUSTER ENERGY E(   CCSD(T)) =") >= 0)
    {
      readEnergy();
    }
    
    if (line.indexOf("BASIS OPTIONS") >= 0){
      readBasisInfo();
      return true;
    }    
    if (line.indexOf("$CONTRL OPTIONS") >= 0){
      readControlInfo();
      return true;
    }
    if (line.indexOf("ATOMIC BASIS SET") >= 0) {
      readGaussianBasis("SHELL TYPE", "TOTAL");
      return false;
    }
    if ((isBohr = line.indexOf("COORDINATES (BOHR)") >= 0)
        || line.indexOf("COORDINATES OF ALL ATOMS ARE (ANGS)") >= 0) {
      if (!doGetModel(++modelNumber, null))
        return checkLastModel();
      atomNames = new  Lst<String>();
      if (isBohr)
        readAtomsInBohrCoordinates();
      else
        readAtomsInAngstromCoordinates();
      return true;
    }
    if (!doProcessLines)
      return true;
    if (line.indexOf("FREQUENCIES IN CM") >= 0) {
      readFrequencies();
      return true;
    }
    if (line.indexOf("SUMMARY OF THE EFFECTIVE FRAGMENT") >= 0) {
      // We have EFP and we're not afraid to use it!!
      // it would be nice is this information was closer to the ab initio
      // molecule
      readEFPInBohrCoordinates();
      return false;
    }
    if (line.indexOf("  TOTAL MULLIKEN AND LOWDIN ATOMIC POPULATIONS") >= 0) {
      readPartialCharges();
      return false;
    }
    if (line.indexOf("ELECTROSTATIC MOMENTS")>=0){
      readDipoleMoment();
      return true;
    }
    if (line.indexOf("- ALPHA SET -") >= 0)
      alphaBeta = "alpha";
    else if (line.indexOf("- BETA SET -") >= 0)
      alphaBeta = "beta";
    else if  (line.indexOf("  EIGENVECTORS") >= 0
        || line.indexOf("  INITIAL GUESS ORBITALS") >= 0
        || line.indexOf("  MCSCF OPTIMIZED ORBITALS") >= 0
        || line.indexOf("  MCSCF NATURAL ORBITALS") >= 0
        || line.indexOf("  MOLECULAR ORBITALS") >= 0
        && line
            .indexOf("  MOLECULAR ORBITALS LOCALIZED BY THE POPULATION METHOD") < 0) {
      if (!filterMO())
        return true;
      // energies and possibly symmetries
      readMolecularOrbitals(HEADER_GAMESS_ORIGINAL);
      return false;
    }
    if (line.indexOf("EDMISTON-RUEDENBERG ENERGY LOCALIZED ORBITALS") >= 0
        || line.indexOf("  THE PIPEK-MEZEY POPULATION LOCALIZED ORBITALS ARE") >= 0) {
      if (!filterMO())
        return true;
      readMolecularOrbitals(HEADER_NONE);
      return false;
    }
    if (line.indexOf("  NATURAL ORBITALS IN ATOMIC ORBITAL BASIS") >= 0) {
      // the for mat of the next orbitals can change depending on the
      // cistep used. This works for ALDET and GUGA

      // BH to AD: but the GUGA file delivered has only energies?
      if (!filterMO())
        return true;
      readMolecularOrbitals(HEADER_GAMESS_OCCUPANCIES);
      return false;
    }
    return checkNboLine();
  }
  
  private boolean readInputDeck() throws Exception {
    readLines(2);
    asc.newAtomSet();
    while (rd().indexOf("$END") < 0) {
      String[] tokens = getTokens();
      if (tokens.length > 4)
        addAtomXYZSymName(tokens, 2, tokens[0], null).elementNumber = (short) parseIntStr(tokens[1]);
    }
    return (continuing = false);
  }

  @Override
  protected void readMolecularOrbitals(int headerType) throws Exception {
    setCalculationType();
    super.readMolecularOrbitals(headerType);
  }
  
  /*

   for H2ORHF, the Z entries are nuclear positions

   MULTIPOLE COORDINATES, ELECTRONIC AND NUCLEAR CHARGES

   X              Y              Z           ELEC.   NUC.
   ZO1       -7.7339870636   0.7855024013   0.0607735878    8.00000    0.0
   ZH2       -6.3592068574   1.8865489098  -0.2204029069    1.00000    0.0
   ZH3       -8.0979273324   0.8550163890   1.8055083920    1.00000    0.0
   O1        -7.7339870636   0.7855024013   0.0607735878   -8.21083    0.0
   H2        -6.3592068574   1.8865489098  -0.2204029069   -0.55665    0.0
   H3        -8.0979273324   0.8550163890   1.8055083920   -0.55665    0.0
   B12       -7.0465971979   1.3360253807  -0.0798150033   -0.33793    0.0
   B13       -7.9159574354   0.8202591203   0.9331406462   -0.33793    0.0

   for H2ODFT
   MULTIPOLE COORDINATES, ELECTRONIC AND NUCLEAR CHARGES

   X              Y              Z           ELEC.   NUC.
   O1         6.7090100309  -3.9975560003  -0.0215951332   -8.22458    8.0
   H2         7.4569069150  -4.5350351179  -1.5490605828   -0.57906    1.0
   H3         7.1548619721  -2.2838340456   0.1923145656   -0.57906    1.0
   B12        7.0829581926  -4.2662958353  -0.7853275496   -0.30866    0.0
   B13        6.9319357212  -3.1406952991   0.0853600246   -0.30866    0.0
   
   */

  protected void readEFPInBohrCoordinates() throws Exception {
    //it's really too bad that the EFP information is nowhere near
    //the ab initio molecule for single-point runs.

    int acInFirstModel = asc.ac;
    //should only contain the $DATA card
    discardLinesUntilContains("MULTIPOLE COORDINATES");

    rd(); // blank line
    rd(); // X              Y              Z           ELEC.   NUC.
    //at least for FRAGNAME=H2ORHF, the atoms come out as ZO1, ZH2, ZH3
    //Z stands for nuclear position.
    while (rd() != null && line.length() >= 72) {
      String atomName = line.substring(1, 2);
      //Z is perhaps not officially deprecated, but the newer
      //H2ODFT potential doesn't use it.
      //It does however put the nuclear charge in the last column
      if (atomName.charAt(0) == 'Z')
        atomName = line.substring(2, 3);
      else if (parseFloatRange(line, 67, 73) == 0)
        continue;
      float x = parseFloatRange(line, 8, 25);
      float y = parseFloatRange(line, 25, 40);
      float z = parseFloatRange(line, 40, 56);
      if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
        break;
      Atom atom = asc.addNewAtom();
      atom.atomName = atomName + (++acInFirstModel);
      setAtomCoordXYZ(atom, x * ANGSTROMS_PER_BOHR, y * ANGSTROMS_PER_BOHR, z * ANGSTROMS_PER_BOHR);
      atomNames.addLast(atomName);
    }
  }
  
  
  @Override
  protected void readAtomsInBohrCoordinates() throws Exception {
/*
 ATOM      ATOMIC                      COORDINATES (BOHR)
           CHARGE         X                   Y                   Z
 C           6.0     3.9770911639       -2.7036584676       -0.3453920672

0         1         2         3         4         5         6         7    
01234567890123456789012345678901234567890123456789012345678901234567890123456789

*/    

    rd(); // discard one line
    String atomName;
    asc.newAtomSet();
    int n = 0;
    while (rd() != null
        && (atomName = parseTokenRange(line, 1, 11)) != null) {
      float x = parseFloatRange(line, 17, 37);
      float y = parseFloatRange(line, 37, 57);
      float z = parseFloatRange(line, 57, 77);
      if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
        break;
      Atom atom = asc.addNewAtom();
      setAtomCoordXYZ(atom, x * ANGSTROMS_PER_BOHR, y * ANGSTROMS_PER_BOHR, z * ANGSTROMS_PER_BOHR);
      int atomicNumber = parseIntRange(line, 11, 14);
      atom.elementSymbol = getElementSymbol(atomicNumber);
      setAtom(atom, atomicNumber, atom.elementSymbol + (++n), atomName);
    }
  }
  
  private void readAtomsInAngstromCoordinates() throws Exception {
    rd(); 
    rd(); // discard two lines
    String atomName;
    asc.newAtomSet();
/*    
       COORDINATES OF ALL ATOMS ARE (ANGS)
   ATOM   CHARGE       X              Y              Z
 ------------------------------------------------------------
 C           6.0   2.1045861621  -1.4307145508  -0.1827736240

0         1         2         3         4         5         6    
0123456789012345678901234567890123456789012345678901234567890

*/
    int n = 0;
    while (rd() != null
        && (atomName = parseTokenRange(line, 1, 11)) != null) {
      float x = parseFloatRange(line, 16, 31);
      float y = parseFloatRange(line, 31, 46);
      float z = parseFloatRange(line, 46, 61);
      if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
        break;
      Atom atom = asc.addNewAtom();
      setAtomCoordXYZ(atom, x, y, z);
      int atomicNumber = parseIntRange(line, 11, 14);
      atom.elementSymbol = getElementSymbol(atomicNumber);
      setAtom(atom, atomicNumber, atom.elementSymbol + (++n), atomName);
    }
    
    /*
    During optimization, this will immediately appear after the
    ab initio molecule
    
    COORDINATES OF FRAGMENT MULTIPOLE CENTERS (ANGS)
    MULTIPOLE NAME        X              Y              Z
    ------------------------------------------------------------
    FRAGNAME=H2ORHF
    ZO1              -4.1459482636   0.4271933699   0.0417242924
    ZH2              -3.4514529072   1.0596960013  -0.0504444399
    ZH3              -4.5252917848   0.5632659571   0.8952236761
    
    or for H2ODFT
    
    COORDINATES OF FRAGMENT MULTIPOLE CENTERS (ANGS)
    MULTIPOLE NAME        X              Y              Z
    ------------------------------------------------------------
    FRAGNAME=H2ODFT
    O1                3.5571448937  -2.1158335714  -0.0044768463
    H2                3.9520351868  -2.4002052098  -0.8132245708
    H3                3.7885802785  -1.2074436330   0.1057222304
    
    */
    
    // Now is the time to read Effective Fragments (EFP)
    if (line.indexOf("COORDINATES OF FRAGMENT MULTIPOLE CENTERS (ANGS)") >= 0) {
         rd(); // MULTIPONE NAME         X ...
        rd(); // ------------------------ ...
        rd(); // FRAGNAME=
        
        //at least for FRAGNAME=H2ORHF, the atoms come out as ZO1, ZH2, ZH3
        while (rd() != null
        && (atomName = parseTokenRange(line, 1, 2)) != null) {
              if (parseTokenRange(line,1,2).equals("Z")) //Z means nuclear position
                    atomName = parseTokenRange(line, 2, 3);
              else if (parseTokenRange(line,1,9).equals("FRAGNAME"))//Z is a deprecated requirement
                  continue;
              else
                    atomName = parseTokenRange(line, 1, 2); 
              float x = parseFloatRange(line, 16, 31);
              float y = parseFloatRange(line, 31, 46);
              float z = parseFloatRange(line, 46, 61);
              if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
                    break;
              Atom atom = asc.addNewAtom();
              atom.atomName = atomName + (++n);
              setAtomCoordXYZ(atom, x, y, z);
              atomNames.addLast(atomName);
        } 
          
    }
  }
  /*
   * 
   ATOMIC BASIS SET
   ----------------
   THE CONTRACTED PRIMITIVE FUNCTIONS HAVE BEEN UNNORMALIZED
   THE CONTRACTED BASIS FUNCTIONS ARE NOW NORMALIZED TO UNITY

   SHELL TYPE PRIMITIVE    EXPONENT          CONTRACTION COEFFICIENTS

   C         


   1   S    1           172.2560000       .061766907377
   1   S    2            25.9109000       .358794042852
   1   S    3             5.5333500       .700713083689

   2   L    4             3.6649800      -.395895162119       .236459946619
   2   L    5              .7705450      1.215834355681       .860618805716

   OR:

   SHELL TYPE PRIM    EXPONENT          CONTRACTION COEFFICIENTS

   C         

   1   S    1      71.616837    2.707814 (  0.154329) 
   1   S    2      13.045096    2.618880 (  0.535328) 
   1   S    3       3.530512    0.816191 (  0.444635) 

   2   L    4       2.941249   -0.160017 ( -0.099967)     0.856045 (  0.155916) 
   2   L    5       0.683483    0.214036 (  0.399513)     0.538304 (  0.607684) 
   2   L    6       0.222290    0.161536 (  0.700115)     0.085276 (  0.391957) 

   */
  
  @Override
  protected String fixShellTag(String tag) {
    return tag;
  }


  /*
  TOTAL MULLIKEN AND LOWDIN ATOMIC POPULATIONS
ATOM         MULL.POP.    CHARGE          LOW.POP.     CHARGE
1 O             8.000000    0.000000         8.000000    0.000000
2 O             8.000000    0.000000         8.000000    0.000000


*/

  /**
   * @throws Exception
   */
  void readPartialCharges() throws Exception {
    String tokens[]=null;
    String searchstr = (lowdenCharges ? "LOW.POP."
            : "MULL.POP.");
    while (rd() != null && ("".equals(line.trim())||line.indexOf("ATOM") >= 0)) {
      tokens = getTokens();      
    }
    int poploc = 0;
    for (; ++poploc < tokens.length; )
      if (searchstr.equals(tokens[poploc]))
        break;
    if (++poploc >= tokens.length || !"CHARGE".equals(tokens[poploc++]))
      return; // Not as expected don't read
    Atom[] atoms = asc.atoms;
    int startAtom = asc.getLastAtomSetAtomIndex();
    int endAtom = asc.ac;
    for (int i = startAtom; i < endAtom && rd() != null; ++i)
      atoms[i].partialCharge = parseFloatStr(PT.getTokens(prevline)[poploc]);
  }
 /*
           ---------------------
          ELECTROSTATIC MOMENTS
          ---------------------

 POINT   1           X           Y           Z (BOHR)    CHARGE
                 0.000000    0.000000   -0.020735        0.00 (A.U.)
         DX          DY          DZ         /D/  (DEBYE)
     0.000000    0.000000   -1.449162    1.449162
  */
  void readDipoleMoment() throws Exception {
    String tokens[] = null;
    rd();
    while (line != null && ("".equals(line.trim()) || line.indexOf("DX") < 0)) {
      rd();
    }
    tokens = getTokens();
    if (tokens.length != 5)
      return;
    if ("DX".equals(tokens[0]) && "DY".equals(tokens[1])
        && "DZ".equals(tokens[2])) {
      tokens = PT.getTokens(rd());
      V3 dipole = V3.new3(parseFloatStr(tokens[0]),
          parseFloatStr(tokens[1]), parseFloatStr(tokens[2]));
      Logger.info("Molecular dipole for model "
          + asc.atomSetCount + " = " + dipole);
      asc.setCurrentModelInfo("dipole", dipole);
    }
  }
}
