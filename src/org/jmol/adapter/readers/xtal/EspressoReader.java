package org.jmol.adapter.readers.xtal;

/**
 * Piero Canepa
 * 
 * Quantum Espresso
 * http://www.quantum-espresso.org and http://qe-forge.org/frs/?group_id=10
 * @author Pieremanuele Canepa, Room 104, FM Group School of Physical Sciences,
 *         Ingram Building, University of Kent, Canterbury, Kent, CT2 7NH United
 *         Kingdom, pc229@kent.ac.uk
 * 
 * @version 1.0
 */

import javajs.util.PT;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Atom;

public class EspressoReader extends AtomSetCollectionReader {

  private float[] cellParams;
  private Double totEnergy;
  private boolean endFlag;
 

  @Override
  protected void initializeReader() {
    setSpaceGroupName("P1");
    doApplySymmetry = true;
    // inputOnly = checkFilter("INPUT");
  }

  @Override
  protected boolean checkLine() throws Exception {
    if (line.contains("lattice parameter (a_0)")
        || line.contains("lattice parameter (alat)")) {
      readAparam();
    } else if (line.contains("crystal axes:")) {
      readCellParam(false);
    } else if (line.contains("CELL_PARAMETERS (")) {
      readCellParam(true);
    } else if (line.contains("Cartesian axes")) {
      discardLinesUntilContains("positions (");
      if (doGetModel(++modelNumber, null))
        readAtoms();
    } else if (line.contains("POSITIONS (")) {
      if (doGetModel(++modelNumber, null))
        readAtoms();
    } else if (line.contains("!    total energy")) {
      readEnergy();
    } else if (line.contains("A final scf")) {
      endFlag = true;
    }
    return true;
  }

  private float aPar;

  private void readAparam() throws Exception {
    // lattice parameter (alat)  =       5.3033  a.u.
    aPar = parseFloatStr(getTokens()[4]) * ANGSTROMS_PER_BOHR;
  }

  /*
  crystal axes: (cart. coord. in units of a_0)
            a(1) = (  1.000000  0.000000  0.000000 )  
            a(2) = ( -0.500000  0.866025  0.000000 )  
            a(3) = (  0.000000  0.000000  0.744955 )  

  reciprocal axes: (cart. coord. in units 2 pi/a_0)
            b(1) = (  1.000000  0.577350  0.000000 )  
            b(2) = (  0.000000  1.154701  0.000000 )  
            b(3) = (  0.000000  0.000000  1.342362 )  

   */

  /*  
  CELL_PARAMETERS (alat= 17.62853047)
  1.019135101   0.000000000   0.000000000
  -0.509567550   0.882596887   0.000000000
  0.000000000   0.000000000   0.737221415

   */

  private void readCellParam(boolean andAPar) throws Exception {
    int i0 = (andAPar ? 0 : 3);
    
    /*   in the old version of Espresso optimized cell parameters
    are expressed in Bohr unit
    CELL_PARAMETERS (bohr)                                                                                                                                                                                                                   
    -4.979256769   4.979256769   9.998215946
     4.979256769  -4.979256769   9.998215946
     4.979256769   4.979256769  -9.998215946
    */
    if (line.contains("bohr"))
      aPar = ANGSTROMS_PER_BOHR;

    /*    in the old version of Espresso optimized cell parameters
    are expressed in function of the original aPar stored at the beginning
    CELL_PARAMETERS (alat)
    1.001108280   0.000000000   0.000000000
    0.000000000   1.001108280   0.000000000
    0.000000000   0.000000000   1.301023011
    So we also check for = in the line 
     *
     */

    if (andAPar && line.contains("="))
      aPar = parseFloatStr(line.substring(line.indexOf("=") + 1))
      * ANGSTROMS_PER_BOHR;

    //Can you look at the example HAP_fullopt_40_r1.fullopt from the 2nd model on the representation is correct 
    //The 1st is wrong. 
    // BH: It's just a bad starting geometry, but the program 
    //     very nicely cleans it up in just one step.
    //PC this is not true as it happens for every 0single jobs and even for single SCF calculations

    cellParams = new float[9];
    for (int n = 0, i = 0; n < 3; n++) {
      String[] tokens = PT.getTokens(rd());
      cellParams[i++] = parseFloatStr(tokens[i0]) * aPar;
      cellParams[i++] = parseFloatStr(tokens[i0 + 1]) * aPar;
      cellParams[i++] = parseFloatStr(tokens[i0 + 2]) * aPar;
    }
  }

  private void newAtomSet() throws Exception {
    asc.newAtomSet();
    if (totEnergy != null)
      setEnergy();
  }

  private void setCellParams() throws Exception {
    if (cellParams != null) {
      addExplicitLatticeVector(0, cellParams, 0);
      addExplicitLatticeVector(1, cellParams, 3);
      addExplicitLatticeVector(2, cellParams, 6);
      setSpaceGroupName("P1");
    }
  }

  /*

   some have just atoms...

    site n.     atom                  positions (a_0 units)
        1           Ca  tau(  1) = (   0.5000000  -0.2886751  -0.0018296  )
        2           Ca  tau(  2) = (  -0.5000000   0.2886751   0.3706481  )

   ...some have masses...

     site n.  atom      mass           positions (a_0 units)
        1        Si  28.0800   tau( 1) = (    0.00000    0.00000    0.00000  )
        2        Si  28.0800   tau( 2) = (    0.25000    0.25000    0.25000  )

   ...some just end with a blank line; others end with a short phrase...

      O       -0.088707198  -0.347657305   0.434774168
      O       -0.258950107   0.088707198   0.434774168
      O        0.000000000   0.000000000  -0.214003341
      O        0.000000000   0.000000000   0.286225136
      H        0.000000000   0.000000000  -0.071496337
      H        0.000000000   0.000000000   0.428733409
      End final coordinates
   */

  private void readAtoms() throws Exception {
    // all atom block types are read here -- BH
    newAtomSet();
    boolean isAlat = (line.contains("alat") || line.contains("a_0"));
    // This is when coordinates are like
    //F        3.456262920   8.764752820   1.733918940    0   0   0
    boolean firstStr = (line.contains("site n."));
    boolean isFractional = line.contains("crystal");
    boolean isBohr = line.contains("bohr");
    boolean isAngstrom = line.contains("angstrom");
    
    if (isAlat || isFractional || isAngstrom)
      setCellParams();
    setFractionalCoordinates(isFractional);

    while (rd() != null && line.length() > 45) {
      String[] tokens = getTokens();
      Atom atom = asc.addNewAtom();
      atom.atomName = tokens[(isBohr || tokens.length == 4 || !firstStr ? 0 : 1)];
      int i1 = (isBohr || tokens.length == 4 || !firstStr ? 1
          : tokens.length - 4);
      float x = parseFloatStr(tokens[i1++]);
      float y = parseFloatStr(tokens[i1++]);
      float z = parseFloatStr(tokens[i1++]);
      atom.set(x, y, z);
      if (isBohr) {
        atom.scale(ANGSTROMS_PER_BOHR);
      } else if (isAlat) {
        atom.scale(aPar);
      }
      setAtomCoord(atom);
    }
    applySymmetryAndSetTrajectory();

    //This to avoid error when the class reads the FinalRun task
    /*    A final scf calculation at the relaxed structure.
        The G-vectors are recalculated for the final unit cell
        Results may differ from those at the preceding step*/
    if (endFlag)
      discardLinesUntilContains("Harris-Foulkes estimate");
  }

  //!    total energy              =   -1668.20791579 Ry

  private void readEnergy() throws Exception {
    totEnergy = Double.valueOf(Double.parseDouble(PT.getTokens(line.substring(line
        .indexOf("=") + 1))[0]));
  }

  private void setEnergy() {

    asc.setAtomSetEnergy("" + totEnergy, totEnergy.floatValue());
    asc.setInfo("Energy", totEnergy);
    asc.setAtomSetName("E = " + totEnergy + " Ry");
  }
}
