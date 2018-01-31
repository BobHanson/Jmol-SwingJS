/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-11-23 12:49:25 -0600 (Fri, 23 Nov 2007) $
 * $Revision: 8655 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.jmol.minimize.forcefield;

import java.io.BufferedReader;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;

import org.jmol.api.SmilesMatcherInterface;
import org.jmol.java.BS;
import org.jmol.minimize.MinAngle;
import org.jmol.minimize.MinAtom;
import org.jmol.minimize.MinBond;
import org.jmol.minimize.MinObject;
import org.jmol.minimize.MinTorsion;
import org.jmol.minimize.Minimizer;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.util.BSUtil;
import org.jmol.util.Elements;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.viewer.JmolAsyncException;

/**
 * MMFF94 implementation 5/14/2012
 * 
 * - fully validated for atom types and charges
 * - reasonably well validated for energies (see below)
 * 
 * - TODO: add UFF for preliminary/backup calculation
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 *
 * Java implementation by Bob Hanson 5/2012
 * based loosely on chemKit code by Kyle Lutz and OpenBabel code by Tim Vandermeersch
 * but primarily from what is described in 
 *   
 *    T. A. Halgren; "Merck Molecular Force Field. V. Extension of MMFF94 
 *      Using Experimental Data, Additional Computational Data, 
 *      and Empirical Rules", J. Comp. Chem. 5 & 6 616-641 (1996).
 *
 * Parameter files are clipped from the original Wiley FTP site supplemental material:
 * 
 * ftp://ftp.wiley.com/public/journals/jcc/suppmat/17/490/MMFF-I_AppendixB.ascii
 * 
 * Original work, as listed at http://towhee.sourceforge.net/forcefields/mmff94.html:
 * 
 *    T. A. Halgren; "Merck Molecular Force Field. I. Basis, Form, Scope, 
 *      Parameterization, and Performance of MMFF94", J. Comp. Chem. 5 & 6 490-519 (1996).
 *    T. A. Halgren; "Merck Molecular Force Field. II. MMFF94 van der Waals 
 *      and Electrostatic Parameters for Intermolecular Interactions", 
 *      J. Comp. Chem. 5 & 6 520-552 (1996).
 *    T. A. Halgren; "Merck Molecular Force Field. III. Molecular Geometries and 
 *      Vibrational Frequencies for MMFF94", J. Comp. Chem. 5 & 6 553-586 (1996).
 *    T. A. Halgren; R. B. Nachbar; "Merck Molecular Force Field. IV. 
 *      Conformational Energies and Geometries for MMFF94", J. Comp. Chem. 5 & 6 587-615 (1996).
 *    T. A. Halgren; "Merck Molecular Force Field. V. Extension of MMFF94 
 *      Using Experimental Data, Additional Computational Data, 
 *      and Empirical Rules", J. Comp. Chem. 5 & 6 616-641 (1996).
 *    T. A. Halgren; "MMFF VII. Characterization of MMFF94, MMFF94s, 
 *      and Other Widely Available Force Fields for Conformational Energies 
 *      and for Intermolecular-Interaction Energies and Geometries", 
 *      J. Comp. Chem. 7 730-748 (1999).
 *      
 * Validation carried out using MMFF94_opti.log and MMFF94_dative.mol2 (or MMFF94_hypervalent.mol2) 
 * including 761 models using org/jmol/minimize/forcefield/mmff/validate/checkmm.spt (checkAllEnergies)
 * 
 * All typical compounds validate. The following 7  
 * structures do not validate to within 0.1 kcal/mol total energy;
 * 

version=12.3.26_dev

# code: adding empirical rules to MMFF94 calculation
#
# checkmm.spt;checkAllEnergies
#
# checking calculated energies for 761 models
# 1 COMKAQ     E=   -7.3250003   Eref=  -7.6177    diff=  0.2926998
# 2 DUVHUX10   E=   64.759995    Eref=  64.082855  diff=  0.6771393
# 3 FORJIF     E=   35.978       Eref=  35.833878  diff=  0.14412308
# 4 JADLIJ     E=   25.104       Eref=  24.7038    diff=  0.4001999
# 5 PHOSLA10   E=   111.232994   Eref=  112.07078  diff=  0.8377838
# 6 PHOSLB10   E=   -93.479004   Eref=  -92.64081  diff=  0.8381958
#
# for 761 atoms, 6 have energy differences outside the range -0.1 to 0.1 
# with a standard deviation of 0.05309403
#
# a comment about empirical bond parameter calculation:
#
#    // Well, guess what? As far as I can tell, in Eqn 18 on page 625, 
#    // the reduction term and delta are zero. 
#   
#    // -- at least in the program run that is at the validation site:
#    //  OPTIMOL: Molecular and Macromolecular Optimization Package 17-Nov-98 16:01:23
#    // SGI double-precision version ... Updated 5/6/98
#    // 
#    // This calculation is run only for the following three structures. In each case the
#    // reported validation values and values from Jmol 12.3.26_dev are shown. Clearly 
#    // the r0 calculated and final energies are very good. subtracting off 0.008 from 
#    // r0 would certainly not give the reported values. Something is odd there.
#    //
#    //             bond      red*     r0(here/valid)  kb(here/valid)  Etotal(here/valid)
#    //            ---------------------------------------------------------------------------------------
#    // OHWM1       H1-O1     0.03      0.978/0.978       7.510/7.51   -21.727/-21.72690
#    // ERULE_03    Si1-P1    0.0       2.223/2.224       1.614/1.609   -2.983/ -2.93518
#    // ERULE_06    N1-F1     0.0       1.381/1.379       5.372/5.438    1.582/  1.58172
#    //
#    // *reduction and delta terms not used in Jmol's calculation
#
#


COMKAQ 
 -- BATCHMIN ignores 1 of 5-membered ring torsions for a 1-oxo-2-oxa-bicyclo[3.2.0]heptane
 -- MMFF94_bmin.log: WARNING - Conformational Energies May Not Be Accurate

DUVHUX10
 -- BATCHMIN ignores 5-membered ring issue for S-S-containing ring
 -- MMFF94_bmin.log: WARNING - Conformational Energies May Not Be Accurate
 
FORJIF
 -- BATCHMIN misses four standard 5-membered C-C ring bonds 
 -- MMFF94_bmin.log: WARNING - Conformational Energies May Not Be Accurate

JADLIJ
 -- BATCHMIN ignores 5-membered ring for S (note, however, this is not the case in BODKOU)
 -- MMFF94_bmin.log: WARNING - Conformational Energies May Not Be Accurate

PHOSLA10
 -- BATCHMIN ignores all 5-membered ring torsions in ring with P
 -- (note, however, this is not the case in CUVGAB)
 -- MMFF94_bmin.log: WARNING - Conformational Energies May Not Be Accurate

PHOSLB10
 -- BATCHMIN ignores all 5-membered ring torsions in ring with P
 -- (note, however, this is not the case in CUVGAB)
 -- MMFF94_bmin.log: WARNING - Conformational Energies May Not Be Accurate

OHMW1
 -- H2O complexed with hydroxide OH(-)
 -- I don't understand (a) why the OH(-) bond has mltb=1, and even with that
    I am not getting the correct ro/kb for that bond from empirical rules. 
    Still working on that.... 
 
 */


public class ForceFieldMMFF extends ForceField {

  
  //private boolean useEmpiricalRules = true;
  
  private static final int A4_VDW = 122;
  private static final int A4_BNDK = 123;
  private static final int A4_CHRG = 124;
  private static final int A4_SB = 125;
  private static final int A4_SBDEF = 126;
  private static final int KEY_SBDEF = 0;
  private static final int KEY_PBCI = 0;
  private static final int KEY_VDW = 0;
  private static final int KEY_BNDK = 0;
  private static final int KEY_OOP = 6;
  private static final int TYPE_PBCI = 0x1;
  private static final int TYPE_VDW = 0x11;
  private static final int TYPE_BNDK = 0x222;
  private static final int TYPE_CHRG = 0x22;
  // the following are bit flags indicating in the 0xF range 
  // which atoms might have default parameter values
  private static final int TYPE_BOND = 0x3;    //    0011
  private static final int TYPE_ANGLE = 0x5;   //    0101
  private static final int TYPE_SB = 0x15;     // 01 0101
  private static final int TYPE_SBDEF = 0x25;  // 10 0101
  private static final int TYPE_TORSION = 0x9; // 00 1001
  private static final int TYPE_OOP = 0xD;     // 00 1101;
  

  private static Lst<AtomType> atomTypes;
  private static Map<Object, Object> ffParams;

  private int[] rawAtomTypes;
  private int[] rawBondTypes;
  private float[] rawMMFF94Charges; // calculated here
  
  public String[] getAtomTypeDescriptions() {
    return getAtomTypeDescs(rawAtomTypes);
  }

  public float[] getPartialCharges() {
    return rawMMFF94Charges;
  }

  /*
   * from SMARTS search when calculating partial charges:
   * 
   * vRings[0] list of 3-membered rings
   * vRings[1] list of 4-membered rings
   * vRings[2] list of all 5-membered rings
   * vRings[3] list of aromatic 5-membered and 6-membered rings
   */
  private Lst<BS>[] vRings;
  
  public ForceFieldMMFF(Minimizer m) throws JmolAsyncException {
    this.minimizer = m;
    this.name = "MMFF";
    getParameters();
  }
  
  @Override
  public void clear() {
    // not same atoms?
    // TODO
    
  }

  @Override
  public boolean setModel(BS bsElements, int elemnoMax) {
    Minimizer m = minimizer;
    if (!setArrays(m.atoms, m.bsAtoms, m.bonds, m.rawBondCount, false, false))
      return false;  
    setModelFields();
    if (!fixTypes())
      return false;
    calc = new CalculationsMMFF(this, ffParams, minAtoms, minBonds, 
        minAngles, minTorsions, minPositions, minimizer.constraints);
    calc.setLoggingEnabled(true);
    return calc.setupCalculations();
  }

  public boolean setArrays(Atom[] atoms, BS bsAtoms, Bond[] bonds,
                        int rawBondCount, boolean doRound, boolean allowUnknowns) {
    Minimizer m = minimizer;
    // these are original atom-index-based, not minAtom-index based. 

    vRings = AU.createArrayOfArrayList(4);
    rawAtomTypes = setAtomTypes(atoms, bsAtoms, m.vwr.getSmilesMatcher(),
        vRings, allowUnknowns);
    if (rawAtomTypes == null)
      return false;
    rawBondTypes = setBondTypes(bonds, rawBondCount, bsAtoms);
    rawMMFF94Charges = calculatePartialCharges(bonds, rawBondTypes, atoms,
        rawAtomTypes, bsAtoms, doRound);
    return true;
  }
  private final static String names = "END.BCI.CHG.ANG.NDK.OND.OOP.TBN.FSB.TOR.VDW.";
  private final static int[] types = {0, TYPE_PBCI, TYPE_CHRG, TYPE_ANGLE, TYPE_BNDK, TYPE_BOND, TYPE_OOP, TYPE_SB, TYPE_SBDEF, TYPE_TORSION, TYPE_VDW };
  
  private void getParameters() throws JmolAsyncException {
    if (ffParams != null)
      return;
    getAtomTypes();
    Hashtable<Object, Object> data = new Hashtable<Object, Object>();
    String resourceName = "mmff94.par.txt";
    if (Logger.debugging)
      Logger.debug("reading data from " + resourceName);
    BufferedReader br = null;
    String line = null;
    try {
      br = getBufferedReader(resourceName);
      int pt = 0;
      int dataType = 0;
      while (true) {
        while ((pt = (line = br.readLine()).indexOf(".PAR")) < 0) {}
        if ((dataType = types[names.indexOf(line.substring(pt - 3, pt + 1)) / 4]) < 1)
          break;
        readParams(br, dataType, data);
      }
      br.close();
    } catch (JmolAsyncException e) {
      throw new JmolAsyncException(e.getFileName());
    } catch (Exception e) {
      System.err.println("Exception " + e.toString() + " in getResource "
          + resourceName + " line=" + line);
    } finally {
      try {
        br.close();
      } catch (Exception e) {
        //ignore
      }
    }
    ffParams = data;
  }

  private String line;
  
  private void readParams(BufferedReader br, int dataType, Map<Object, Object> data) throws Exception {
    // parameters are keyed by a 32-bit Integer 
    // that is composed of four 7-bit atom types and one 4-bit parameter type
    // in some cases, the last 7-bit atom type (a4) is used for additional parameter typing
    Object value = null;
      int a1 = 0, a2 = 127, a3 = 127, a4 = 127;
      int type = 0;
      switch (dataType) {
      case TYPE_BOND:  // bond
      case TYPE_ANGLE:  // angle
      case TYPE_TORSION:  // tor
        break;          
      case TYPE_CHRG: // chrg/bci, identified by a4 = 4
        a4 = A4_CHRG;
        break;
      case TYPE_SB: // stretch bend, identified by a4
        a4 = A4_SB;
        break;
      case TYPE_BNDK: // A4_BNDK identified by a4
        a4 = A4_BNDK;
        type = KEY_BNDK;
        break;
      case TYPE_OOP: // oop (tor max type is 5)
        type = KEY_OOP;
        break;
      case TYPE_PBCI:  // pbci
        type = KEY_PBCI;
        break;
      case TYPE_SBDEF: // default stretch bend, by row; identified by a4
        a4 = A4_SBDEF;
        type = KEY_SBDEF;
        break;
      case TYPE_VDW:  // vdw identified by a4 = 3, not 127
        a4 = A4_VDW;
        type = KEY_VDW;
        break;
      }
      while (!br.readLine().startsWith("*")){}
      while ((line = br.readLine()).startsWith("*")){}
      do {
        switch (dataType) {
        case TYPE_BNDK:
        case TYPE_OOP:
        case TYPE_PBCI:
        case TYPE_SBDEF:
          break;
        case TYPE_VDW:
          if (line.charAt(5) != ' ')
            continue; // header stuff
          break;
        case TYPE_CHRG: 
          if (line.charAt(0) == '4')
            continue; // I have no idea what type=4 here would mean. It's supposed to be a bond type
          //$FALL-THROUGH$
        case TYPE_ANGLE: 
        case TYPE_BOND: 
        case TYPE_SB: 
        case TYPE_TORSION:
          type = line.charAt(0) - '0';
          break;
        }
        switch (dataType) {
        case TYPE_OOP:
        case TYPE_TORSION: 
          a4 = ival(18,20);
          //$FALL-THROUGH$
        case TYPE_ANGLE:
        case TYPE_SB:
        case TYPE_SBDEF:
          a3 = ival(13,15);
          //$FALL-THROUGH$
        case TYPE_BNDK:
        case TYPE_BOND:
        case TYPE_CHRG:
          a2 = ival(8,10);
          //$FALL-THROUGH$
        case TYPE_PBCI:
        case TYPE_VDW:
          a1 = ival(3,5);
          break;
        }
        switch (dataType) {
        case TYPE_BNDK: // empirical bond stretch: kb, r0 (reversed in file) 
          value = new double[] {
              dval(19,25),
              dval(13,18) };
          break;
        case TYPE_BOND: // bond stretch: kb, r0 
          value = new double[] {
              dval(14,20),
              dval(25,31) };
         break;
        case TYPE_ANGLE:   // angles: ka, theta0
        case TYPE_SB:  // stretch-bend: kbaIJK, kbaKJI
          value = new double[] {
              dval(19,25),
              dval(28,35) };
          break;
        case TYPE_CHRG: // bond chrg
          value = Float.valueOf(fval(10,20));
          break;
        case TYPE_OOP: // oop: koop  
          value = new double[] { dval(24,30) };
          break;
        case TYPE_PBCI:
          value = Float.valueOf(fval(5,15));
          break;
        case TYPE_SBDEF: // default stretch-bend: F(I_J,K),F(K_J,I)  
          double v1 = dval(19,25);
          double v2 = dval(28,35);
          value = new double[] { v1, v2 };
          Integer key = MinObject.getKey(type, a1, a2, a3, a4);
          data.put(key, value);
          value = new double[] { v2, v1 };
          int a = a1;
          a1 = a3;
          a3 = a;
          break;
        case TYPE_TORSION: // tor: v1, v2, v3
          value = new double[] {
              dval(22,28),
              dval(30,36),
              dval(38,44)
              };
          break;
        case TYPE_VDW: // vdw alpha-i, N-i, A-i, G-i, DA
          value = new double[] {
              dval(10,15),
              dval(20,25),
              dval(30,35),
              dval(40,45),
              line.charAt(46) // '-', 'A', 'D'
              };
          break;
        }        
        Integer key = MinObject.getKey(type, a1, a2, a3, a4);
        data.put(key, value);
        if (Logger.debugging)
          Logger.debug(MinObject.decodeKey(key) + " " + (value instanceof Float ? value : Escape.eAD((double[])value)));
      } while (!(line = br.readLine()).startsWith("$"));
  }
  
  private int ival(int i, int j) {
    return PT.parseInt(line.substring(i,j).trim());
  }

  private float fval(int i, int j) {
    return PT.fVal(line.substring(i,j).trim());
  }

  private double dval(int i, int j) {
    return PT.dVal(line.substring(i,j).trim());
  }

  private void getAtomTypes() throws JmolAsyncException {
    String resourceName = "MMFF94-smarts.txt";
    Lst<AtomType> types = new  Lst<AtomType>();
    try {
      BufferedReader br = getBufferedReader(resourceName);      
      //turns out from the Jar file
      // it's a sun.net.www.protocol.jar.JarURLConnection$JarURLInputStream
      // and within Eclipse it's a BufferedInputStream

      AtomType at;
      types.addLast(new AtomType(0, 0, 0, 0, 1, "H or NOT FOUND", ""));
      while ((line = br.readLine()) != null) {
        if (line.length() == 0 || line.startsWith("#"))
          continue;
        //0         1         2         3         4         5         6
        //0123456789012345678901234567890123456789012345678901234567890123456789
        //Mg 12 99  0  24  0 DIPOSITIVE MAGNESIUM CATI [MgD0]
        //#AtSym ElemNo mmType HType formalCharge*12 val Desc Smiles
        int elemNo = ival(3,5);
        int mmType = ival(6,8);
        int hType = ival(9,11);
        float formalCharge = fval(12,15)/12;
        int val = ival(16,18);
        String desc = line.substring(19,44).trim();
        String smarts = line.substring(45).trim();
        types.addLast(at = new AtomType(elemNo, mmType, hType, formalCharge, val, desc, smarts));
        setFlags(at);
      }
      br.close();
    } catch (JmolAsyncException e) {
      throw new JmolAsyncException(e.getFileName());
    } catch (Exception e) {
      System.err.println("Exception " + e.toString() + " in getResource "
          + resourceName + " line=" + line);

    }
    Logger.info((types.size()-1) + " SMARTS-based atom types read");
    atomTypes = types;

  }
  
  private static void setFlags(AtomType at) {
    // fcadj
    switch (at.mmType) {
    
    // Note that these are NOT fractional charges based on
    // number of connected atoms. These are relatively arbitrary
    // fractions of the formal charge to be shared with other atoms.
    // That is, it is not significant that 0.5 is 1/2, and 0.25 is 1/4; 
    // they are just numbers.
    
    case 32:
    case 35:
    case 72:
      // 32  OXYGEN IN CARBOXYLATE ANION
      // 32  NITRATE ANION OXYGEN
      // 32  SINGLE TERMINAL OXYGEN ON TETRACOORD SULFUR
      // 32  TERMINAL O-S IN SULFONES AND SULFONAMIDES
      // 32  TERMINAL O IN SULFONATES
      // 35  OXIDE OXYGEN ON SP2 CARBON, NEGATIVELY CHARGED
      // 72  TERMINAL SULFUR BONDED TO PHOSPHORUS
      at.fcadj = 0.5f;
      break;
    case 62:
    case 76:
      // 62  DEPROTONATED SULFONAMIDE N-; FORMAL CHARGE=-1
      // 76  NEGATIVELY CHARGED N IN, E.G, TRI- OR TETRAZOLE ANION
      at.fcadj = 0.25f;
      break;
    }

    // arom
    switch (at.mmType) {
    case 37:
    case 38:
    case 39:
    case 44:
    case 58:
    case 59:
    case 63:
    case 64:
    case 65:
    case 66:
    case 69:
    case 78:
    case 79:
    case 81:
    case 82:
      at.arom = true;
    }
    
    // sbmb
    switch (at.mmType) {
    case 2:
    case 3:
    case 4:
    case 9:
    case 30:
    case 37:
    case 39:
    case 54:
    case 57:
    case 58:
    case 63:
    case 64:
    case 67:
    case 75:
    case 78:
    case 80:
    case 81:
      at.sbmb = true;
    }
    
    // pilp
    switch(at.mmType) {
    case 6:
    case 8:
    case 10:
    case 11:
    case 12:
    case 13:
    case 14:
    case 15:
    case 26:
    case 32:
    case 35:
    case 39:
    case 40:
    case 43:
    case 44:
    case 59:
    case 62:
    case 70:
    case 72:
    case 76:
      at.pilp = true;
    }
    
    // mltb:
    switch (at.mmType) {
    case 10:
    case 32:
    case 35:
    case 39:
    case 41:
    case 44:
    case 55:
    case 56:
    case 58:
    case 59:
    case 69:
    case 72:
    case 81:
    case 82:
      at.mltb = 1;
      break;
    case 2:
    case 3:
    case 7:
    case 9:
    case 16:
    case 17:
    case 30:
    case 37:
    case 38:
    case 45:
    case 46:
    case 47:
    case 51:
    case 53:
    case 54:
    case 57:
    case 63:
    case 64:
    case 65:
    case 66:
    case 67:
    case 74:
    case 75:
    case 78:
    case 79:
    case 80:
      at.mltb = 2;
      break;
    case 4:
    case 42:
    case 60:
    case 61:
      at.mltb = 3;
      break;
    }
  }
  /**
   * assign partial charges ala MMFF94
   * 
   * @param bonds
   * @param bTypes 
   * @param atoms
   * @param aTypes
   * @param bsAtoms
   * @param doRound 
   * @return   full array of partial charges
   */
  public static float[] calculatePartialCharges(Bond[] bonds, int[] bTypes, Atom[] atoms,
                                          int[] aTypes, BS bsAtoms, boolean doRound) {

    // start with formal charges specified by MMFF94 (not what is in file!)

    float[] partialCharges = new float[atoms.length];
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1))
      partialCharges[i] = atomTypes.get(Math.max(0, aTypes[i])).formalCharge;
    // run through all bonds, adjusting formal charges as necessary
    Atom a1 = null;
    for (int i = bTypes.length; --i >= 0;) {
      a1 = bonds[i].atom1;
      Atom a2 = bonds[i].atom2;
      // It's possible that some of our atoms are not in the atom set,
      // but we don't want both of them to be out of the set.
      
      boolean ok1 = bsAtoms.get(a1.i);
      boolean ok2 = bsAtoms.get(a2.i); 
      if (!ok1 && !ok2)
        continue;
      int it = aTypes[a1.i];
      AtomType at1 = atomTypes.get(Math.max(0, it));
      int type1 = (it < 0 ? -it : at1.mmType);
      it = aTypes[a2.i];
      AtomType at2 = atomTypes.get(Math.max(0, it));
      int type2 = (it < 0 ? -it : at2.mmType);
      
      // we are only interested in bonds that are between different atom types
      
//      if (type1 == type2)
  //      continue;
      
      // check for bond charge increment
      
      // The table is created using the key (100 * type1 + type2), 
      // where type1 < type2. In addition, we encode the partial bci values
      // with key (100 * type)
      
      float dq;  // the difference in charge to be added or subtracted from the formal charges
      try {
        int bondType = bTypes[i];
        float bFactor = (type1 < type2 ? -1 : 1);
        Integer key = MinObject.getKey(bondType, bFactor == 1 ? type2 : type1, bFactor == 1 ? type1 : type2, 127, A4_CHRG);
        Float bciValue = (Float) ffParams.get(key);
        float bci;
        String msg = (Logger.debugging ? a1 + "/" + a2 + " mmTypes=" + type1 + "/" + type2 + " formalCharges=" + at1.formalCharge + "/" + at2.formalCharge + " bci = " : null);
        if (bciValue == null) { 
          // no bci was found; we have to use partial bond charge increments
          // a failure here indicates we don't have information
          float pa = ((Float) ffParams.get(MinObject.getKey(KEY_PBCI, type1, 127, 127, 127))).floatValue();
          float pb = ((Float) ffParams.get(MinObject.getKey(KEY_PBCI, type2, 127, 127, 127))).floatValue();
          bci = pa - pb;
          if (Logger.debugging)
            msg += pa + " - " + pb + " = ";
        } else {
          bci = bFactor * bciValue.floatValue();
        }
        if (Logger.debugging) {
          msg += bci;
          Logger.debug(msg);
        }
        // Here's the way to do this:
        //
        // 1) The formal charge on each atom is adjusted both by
        // taking on an (arbitrary?) fraction of the formal charge on its partner
        // and by giving up an (arbitrary?) fraction of its own formal charge
        // Note that the formal charge is the one specified in the MMFF94 parameters,
        // NOT the model file. The compounds in MMFF94_dative.mol2, for example, do 
        // not indicate formal charges. The only used fractions are 0, 0.25, and 0.5. 
        //
        // 2) Then the bond charge increment is added.
        //
        // Note that this value I call "dq" is added to one atom and subtracted from its partner
        
        dq = at2.fcadj * at2.formalCharge - at1.fcadj * at1.formalCharge + bci;
      } catch (Exception e) {
        dq = Float.NaN;
      }
      if (ok1)
        partialCharges[a1.i] += dq;
      if (ok2)
        partialCharges[a2.i] -= dq;
    }
    
    // just rounding to 0.001 here:
    
    if (doRound) {
      float abscharge = 0;
      for (int i = partialCharges.length; --i >= 0;) {
        partialCharges[i] = (Math.round(partialCharges[i] * 1000)) / 1000f;
        abscharge += Math.abs(partialCharges[i]);
      }
      if (abscharge == 0 && a1 != null) {
        partialCharges[a1.i]= -0.0f;
      }
    }    
    return partialCharges;
  }

  private static boolean isBondType1(AtomType at1, AtomType at2) {
    return at1.sbmb && at2.sbmb || at1.arom && at2.arom; 
    // but what about at1.sbmb && at2.arom?
  }

  private int getBondType(Bond bond, AtomType at1, AtomType at2,
                               int index1, int index2) {
  return (isBondType1(at1, at2) && 
      bond.getCovalentOrder() == 1 
      && !isAromaticBond(index1, index2) ? 1 : 0);  
 }

  private boolean isAromaticBond(int a1, int a2) {
    if (vRings[Raromatic] != null)
      for (int i = vRings[Raromatic].size(); --i >= 0;) {
        BS bsRing = vRings[Raromatic].get(i);
        if (bsRing.get(a1) && bsRing.get(a2))
          return true;
      }
    return false;
  }

  public static String[] getAtomTypeDescs(int[] types) {
    String[] stypes = new String[types.length];
    for (int i = types.length; --i >= 0;) {
      stypes[i] = String.valueOf(types[i] < 0 ? -types[i] : atomTypes.get(types[i]).mmType);
    }
    return stypes;
  }

  ///////////// MMFF94 object typing //////////////

  /**
   * The file MMFF94-smarts.txt is derived from MMFF94-smarts.xlsx. This file
   * contains records for unique atom type/formal charge sharing/H atom type.
   * For example, the MMFF94 type 6 is distributed over eight AtomTypes, each
   * with a different SMARTS match.
   * 
   * H atom types are given in the file as properties of other atom types, not
   * as their own individual SMARTS searches. H atom types are determined based
   * on their attached atom's atom type.
   * 
   * @param atoms
   * @param bsAtoms
   * @param smartsMatcher
   * @param vRings
   * @param allowUnknowns
   * @return array of indexes into AtomTypes or, for H, negative of mmType
   */
  private static int[] setAtomTypes(Atom[] atoms, BS bsAtoms,
                                    SmilesMatcherInterface smartsMatcher,
                                    Lst<BS>[] vRings, boolean allowUnknowns) {
    Lst<BS> bitSets = new  Lst<BS>();
    String[] smarts = new String[atomTypes.size()];
    int[] types = new int[atoms.length];
    BS bsElements = new BS();
    BS bsHydrogen = new BS();
    BS bsConnected = BSUtil.copy(bsAtoms);

    // It may be important to include all attached atoms

    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
      Atom a = atoms[i];
      Bond[] bonds = a.bonds;
      if (bonds != null)
        for (int j = bonds.length; --j >= 0;)
          if (bonds[j].isCovalent())
            bsConnected.set(bonds[j].getOtherAtom(a).i);
    }

    // we need to identify H atoms and also make a BitSet of all the elements

    for (int i = bsConnected.nextSetBit(0); i >= 0; i = bsConnected
        .nextSetBit(i + 1)) {
      int n = atoms[i].getElementNumber();
      switch (n) {
      case 1:
        bsHydrogen.set(i);
        break;
      default:
        bsElements.set(n);
      }
    }

    // generate a list of SMART codes 

    int nUsed = 0;
    for (int i = 1; i < atomTypes.size(); i++) {
      AtomType at = atomTypes.get(i);
      if (!bsElements.get(at.elemNo))
        continue;
      smarts[i] = at.smartsCode;
      nUsed++;
    }
    Logger.info(nUsed + " SMARTS matches used");

    // The SMARTS list is organized from least general to most general
    // for each atom. So the FIRST occurrence of an atom in the list
    // identifies that atom's MMFF94 type.

    try {
      smartsMatcher.getMMFF94AtomTypes(smarts, atoms, atoms.length,
          bsConnected, bitSets, vRings);
    } catch (Exception e) {
      Logger.error(e.toString());
    }
    BS bsDone = new BS();
    for (int j = 0; j < bitSets.size(); j++) {
      BS bs = bitSets.get(j);
      if (bs == null)
        continue;
      // This is a one-pass system. We first exclude
      // all atoms that are already identified...
      bs.andNot(bsDone);
      // then we set the type of what is remaining...
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        types[i] = j;
      // then we include these atoms in the set of atoms already identified
      bsDone.or(bs);
    }

    // now we add in the H atom types as the negative of their MMFF94 type
    // rather than as an index into AtomTypes. 

    for (int i = bsHydrogen.nextSetBit(0); i >= 0; i = bsHydrogen
        .nextSetBit(i + 1)) {
      Bond[] bonds = atoms[i].bonds;
      if (bonds != null) {
        int j = types[bonds[0].getOtherAtom(atoms[i]).i];
        if (j != 0)
          bsDone.set(i);
        types[i] = -atomTypes.get(j).hType;
      }
    }
    if (Logger.debugging)
      for (int i = bsConnected.nextSetBit(0); i >= 0; i = bsConnected
          .nextSetBit(i + 1))
        Logger.debug("atom "
            + atoms[i]
            + "\ttype "
            + (types[i] < 0 ? "" + -types[i] : (atomTypes.get(types[i]).mmType
                + "\t" + atomTypes.get(types[i]).smartsCode + "\t" + atomTypes
                .get(types[i]).descr)));

    if (!allowUnknowns && bsDone.cardinality() != bsConnected.cardinality())
      return null;
    return types;
  }

  private int[] setBondTypes(Bond[] bonds, int bondCount, BS bsAtoms) {
     int[] bTypes = new int[bondCount];
     for (int i = bondCount; --i >= 0;) {
       Atom a1 = bonds[i].atom1;
       Atom a2 = bonds[i].atom2;
       boolean ok1 = bsAtoms.get(a1.i);
       boolean ok2 = bsAtoms.get(a2.i);
       if (!ok1 && !ok2)
         continue;
       int it = rawAtomTypes[a1.i];
       AtomType at1 = atomTypes.get(Math.max(0, it));
       it = rawAtomTypes[a2.i];
       AtomType at2 = atomTypes.get(Math.max(0, it));
       bTypes[i] = getBondType(bonds[i], at1, at2, a1.i, a2.i);
     }
     return bTypes;
   }

  private boolean fixTypes() {
    // set atom types in minAtoms
    for (int i = minAtomCount; --i >= 0;) {
      MinAtom a = minAtoms[i];
      int rawIndex = a.atom.i;
      int it = rawAtomTypes[rawIndex];
      a.ffAtomType = atomTypes.get(Math.max(0, it));
      int type = (it < 0 ? -it : atomTypes.get(it).mmType);
      a.ffType = type;
      a.vdwKey = MinObject.getKey(KEY_VDW, type, 127, 127, A4_VDW);
      a.partialCharge = rawMMFF94Charges[rawIndex];
    }
    
    for (int i = minBonds.length; --i >= 0;) {
      MinBond bond = minBonds[i];
      bond.type = rawBondTypes[bond.rawIndex];
      bond.key = getKey(bond, bond.type, TYPE_BOND);
      if (bond.key == null)
        return false;
    }
    
    for (int i = minAngles.length; --i >= 0;) {
      MinAngle angle = minAngles[i];
      angle.key = getKey(angle, angle.type, TYPE_ANGLE);
      angle.sbKey = getKey(angle, angle.sbType, TYPE_SB);
    }
    
    for (int i = minTorsions.length; --i >= 0;) {
      MinTorsion torsion = minTorsions[i];
      torsion.key = getKey(torsion, torsion.type, TYPE_TORSION);
    }
    return true;
  }

  private final static int[] sbMap = {0, 1, 3, 5, 4, 6, 8, 9, 11};

  private int setAngleType(MinAngle angle) {
    /*
    0      The angle <i>i-j-k</i> is a "normal" bond angle
    1        Either bond <i>i-j</i> or bond <i>j-k</i> has a bond type of 1
    2      Bonds<i> i-j</i> and <i>j-k</i> each have bond types of 1; the sum is 2.
    3      The angle occurs in a three-membered ring
    4      The angle occurs in a four-membered ring
    5      Is in a three-membered ring and the sum of the bond types is 1
    6      Is in a three-membered ring and the sum of the bond types is 2
    7      Is in a four-membered ring and the sum of the bond types is 1
    8      Is in a four-membered ring and the sum of the bond types is 2
    */
    angle.type = minBonds[angle.data[ABI_IJ]].type + minBonds[angle.data[ABI_JK]].type;
    if (checkRings(vRings[R3], angle.data, 3)) {
      angle.type += (angle.type == 0 ? 3 : 4);
    } else if (checkRings(vRings[R4], angle.data, 3)) {
      angle.type += (angle.type == 0 ? 4 : 6);
    }
    
    /*
    SBT   AT BT[IJ] BT[JK]
    -------------------------------------------------------------
    0     0    0    0
    1     1    1    0
    2     1    0    1  [error in table]
    3     2    1    1  [error in table]
    4     4    0    0
    5     3    0    0
    6     5    1    0
    7     5    0    1
    8     6    1    1
    9     7    1    0
    10     7    0    1
    11     8    1    1
     */
    
    angle.sbType = sbMap[angle.type];
    switch (angle.type) {
    case 1:
    case 5:
    case 7:
      angle.sbType += minBonds[angle.data[ABI_JK]].type;
      break;
    }
    return angle.type;
  }
  
  private int setTorsionType(MinTorsion t) {
    if (checkRings(vRings[R4], t.data, 4)) 
      return (t.type = 4); // in 4-membered ring
    t.type = (minBonds[t.data[TBI_BC]].type == 1 ? 1 
        : minBonds[t.data[TBI_AB]].type == 0 && minBonds[t.data[TBI_CD]].type == 0 ? 0 : 2);
    if (t.type == 0 && checkRings(vRings[R5], t.data, 4)) {
      t.type = 5; // in 5-membered ring
    }
    return t.type;
  }

  private int typeOf(int iAtom) {
    return minAtoms[iAtom].ffType;
  }
  
  private boolean checkRings(Lst<BS> v, int[] minlist, int n) {
    if (v != null)
      for (int i = v.size(); --i >= 0;) {
        BS bs = v.get(i);
        if (bs.get(minAtoms[minlist[0]].atom.i)
            && bs.get(minAtoms[minlist[1]].atom.i)
            && (n < 3 || bs.get(minAtoms[minlist[2]].atom.i))
            && (n < 4 || bs.get(minAtoms[minlist[3]].atom.i)))
          return true;
      }
    return false; 
  }

  private Integer getKey(Object obj, int type, int ktype) {
    MinObject o = (obj instanceof MinObject ? (MinObject) obj : null);
    int[] data = (o == null ? (int[]) obj : o.data);
    int n = 4;
    switch (ktype) {
    case TYPE_BOND:
      fixOrder(data, 0, 1);
      n = 2;
      break;
    case TYPE_ANGLE:
      if (fixOrder(data, 0, 2) == -1)
        swap(data, ABI_IJ, ABI_JK);
      type = setAngleType((MinAngle) o);
      n = 3;
      break;
    case TYPE_SB:
      n = 3;
      break;
    case TYPE_TORSION:
      switch (fixOrder(data, 1, 2)) {
      case 1:
        break;
      case -1:
        swap(data, 0, 3);
        swap(data, TBI_AB, TBI_CD);
        break;
      case 0:
        if (fixOrder(data, 0, 3) == -1)
          swap(data, TBI_AB, TBI_CD);
        break;
      }
      type = setTorsionType((MinTorsion) o);
    }
    Integer key = null;
    for (int i = 0; i < 4; i++)
      typeData[i] = (i < n ? typeOf(data[i]) : 127);
    switch (ktype) {
    case TYPE_SB:
      typeData[3] = A4_SB;
      break;
    case TYPE_OOP:
      sortOop(typeData);
      break;
    }
    key = MinObject.getKey(type, typeData[0], typeData[1], typeData[2],
        typeData[3]);
    double[] ddata = (double[]) ffParams.get(key);
    // default typing
    switch (ktype) {
    case TYPE_BOND:
//      if (!useEmpiricalRules)
//        return key;
      return (ddata != null && ddata[0] > 0 ? key : applyEmpiricalRules(o, ddata, TYPE_BOND));
    case TYPE_ANGLE:
      if (ddata != null && ddata[0] != 0)
        return key;
      break;
    case TYPE_TORSION:
      if (ddata == null) {
        if (!ffParams.containsKey(key = getTorsionKey(type, 0, 2))
            && !ffParams.containsKey(key = getTorsionKey(type, 2, 0))
            && !ffParams.containsKey(key = getTorsionKey(type, 2, 2)))
          key = getTorsionKey(0, 2, 2);
        ddata = (double[]) ffParams.get(key);
      }
//      if (!useEmpiricalRules)
//        return key;
      return (ddata != null ? key : applyEmpiricalRules(o, ddata, TYPE_TORSION));
    case TYPE_SB:
      // use periodic row info
      if (ddata != null)
        return key;
      int r1 = getRowFor(data[0]);
      int r2 = getRowFor(data[1]);
      int r3 = getRowFor(data[2]);
      return MinObject.getKey(KEY_SBDEF, r1, r2, r3, A4_SBDEF);
    case TYPE_OOP:
      // use periodic row info
      if (ddata != null)
        return key;
    }
    // run through equivalent types, really just 3
//    if (!useEmpiricalRules && ddata != null)
//      return key;
    boolean isSwapped = false;
    boolean haveKey = false;
    for (int i = 0; i < 3 && !haveKey; i++) {
      for (int j = 0, bit = 1; j < n; j++, bit <<= 1)
        if ((ktype & bit) == bit)
          typeData[j] = getEquivalentType(typeOf(data[j]), i);
      switch (ktype) {
      case TYPE_BOND:
        // not really supposed to do this for MMFF94
        isSwapped = (fixTypeOrder(typeData, 0, 1));
        break;
      case TYPE_ANGLE:
        isSwapped = (fixTypeOrder(typeData, 0, 2));
        break;
      case TYPE_OOP:
        sortOop(typeData);
        break;
      }
      key = MinObject.getKey(type, typeData[0], typeData[1], typeData[2],
          typeData[3]);
      haveKey = ffParams.containsKey(key);
    }
    if (haveKey) {
      if (isSwapped)
        switch (ktype) {
        case TYPE_ANGLE:
          swap(data, 0, 2);
          swap(data, ABI_IJ, ABI_JK);
          setAngleType((MinAngle) o);
          break;
        }
    } else if (type != 0 && ktype == TYPE_ANGLE) {
      key = Integer.valueOf(key.intValue() ^ 0xFF);
    }
    
//    if (!useEmpiricalRules)
//      return key;
//
    ddata = (double[]) ffParams.get(key);
    // default typing
    switch (ktype) {
    case TYPE_ANGLE:
      return (ddata != null && ddata[0] != 0 ? key : 
        applyEmpiricalRules(o, ddata, TYPE_ANGLE));
    }
    return key;
  }

  private Integer getTorsionKey(int type, int i, int j) {
    return MinObject.getKey(type, getEquivalentType(typeData[0], i),
        typeData[1], typeData[2], getEquivalentType(typeData[3], j));  
  }

  private Integer applyEmpiricalRules(MinObject o, double[] ddata, int ktype) {
    double rr, rr2, beta = 0;
    MinAtom a, b, c;
    switch (ktype) {
    case TYPE_BOND:
      a = minAtoms[o.data[0]];
      b = minAtoms[o.data[1]];
      int elemno1 = a.atom.getElementNumber();
      int elemno2 = b.atom.getElementNumber();
      Integer key = MinObject.getKey(KEY_BNDK, Math.min(elemno1, elemno2), Math
          .max(elemno1, elemno2), 127, A4_BNDK);
      ddata = (double[]) ffParams.get(key);
      if (ddata == null)
        return null;
      double kbref = ddata[0];
      double r0ref = ddata[1];
      
      double r0 = getRuleBondLength(a, b, ((MinBond) o).order, isAromaticBond(
          o.data[0], o.data[1]));
      if (r0 == 0)
        return null;
      
      rr = r0ref / r0;
      rr2 = rr * rr;
      double rr4 = rr2 * rr2;
      double rr6 = rr4 * rr2;
      double kb = kbref * rr6;
      o.ddata = new double[] { kb, r0 };
      return Integer.valueOf(-1);
    case TYPE_ANGLE:
      // from OpenBabel
      double theta0;
      if (ddata == null || (theta0 = ddata[1]) == 0) {
        b = minAtoms[o.data[1]];
        Atom atom = b.atom;
        int elemno = atom.getElementNumber();
        switch (o.type) {
        case 3:
        case 5:
        case 6:
          // 3-membered rings
          theta0 = 60;
          beta *= 0.05;
          break;
        case 4:
        case 7:
        case 8:
          // 4-membered rings
          theta0 = 90;
          break;
         default:
           // everything else
           theta0 = 120;
           int crd = atom.getCovalentBondCount();
           switch (crd) {
           case 2:
             if (MinAtom.isLinear(b))
               theta0 = 180;
             else if (elemno == 8)
               theta0 = 105;
             else if (elemno > 10)
               theta0 = 95.0;
             break;
           case 3:
             if (b.ffAtomType.mltb == 0 && b.ffAtomType.val == 3)
               theta0 = (elemno == 7 ? 107 : 92);
             break;
           case 4:
             theta0 = 109.45;
             break;
           }
        }
      }

      beta = 1.75;
      switch (o.type) {
      case 3:
      case 5:
      case 6:
        // 3-membered rings
        beta *= 0.05;
        break;
      case 4:
      case 7:
      case 8:
        // 4-membered rings
        beta *= 0.85;
        break;
      }
      double za = getZParam(minAtoms[o.data[0]].atom.getElementNumber());
      double cb = getCParam(minAtoms[o.data[1]].atom.getElementNumber());
      double zc = getZParam(minAtoms[o.data[2]].atom.getElementNumber());
      double r0ab = getR0(minBonds[o.data[ABI_IJ]]);
      double r0bc = getR0(minBonds[o.data[ABI_JK]]);
      rr = r0ab + r0bc;
      rr2 = rr * rr;
      double D = (r0ab - r0bc) / rr2;
      double theta2 = theta0 * Calculations.DEG_TO_RAD;
      theta2 *= theta2;
      double ka = (beta * za * cb * zc * Math.exp(-2 * D)) / (rr * theta2);
      o.ddata = new double[] { ka, theta0 };
      return Integer.valueOf(-1);
    case TYPE_TORSION:
      int ib = o.data[1];
      int ic = o.data[2];
      b = minAtoms[ib];
      c = minAtoms[ic];

      // rule (a) page 631: no linear systems

      if (MinAtom.isLinear(b) || MinAtom.isLinear(c))
        return null;

      MinBond bondBC = minBonds[o.data[TBI_BC]];

      int elemnoB = b.atom.getElementNumber();
      int elemnoC = c.atom.getElementNumber();
      double ub = getUParam(elemnoB);
      double uc = getUParam(elemnoC);
      double vb = getVParam(elemnoB);
      double vc = getVParam(elemnoC);

      double v1 = 0,
      v2 = 0,
      v3 = 0;
      double pi_bc = -1; // Eqn 21
      double n_bc = -1; // Eqn 22
      double wb = -1,
      wc = 0; // Eqn 23
      int valB = b.ffAtomType.val;
      int valC = c.ffAtomType.val;
      boolean pilpB = b.ffAtomType.pilp;
      boolean pilpC = c.ffAtomType.pilp;
      int mltbB = b.ffAtomType.mltb;
      int mltbC = c.ffAtomType.mltb;

      out: while (true) {

        // rule (b) page 631: aromatics

        if (isAromaticBond(ib, ic)) {
          pi_bc = (pilpB || pilpC ? 0.3 : 0.5);
          beta = (valB + valC == 7 ? 3 : 6);
          break out;
        }

        // rule (c) page 631: full double bonds

        if (bondBC.order == 2) {
          beta = 6;
          pi_bc = (mltbB == 2 && mltbC == 2 ? 1.0 : 0.4);
          break out;
        }

        // single bonds only from here on out:

        int crdB = b.atom.getCovalentBondCount();
        int crdC = c.atom.getCovalentBondCount();

        // rule (d) page 632: [XD4]-[XD4]

        if (crdB == 4 && crdC == 4) {
          vb = getVParam(elemnoB);
          vc = getVParam(elemnoC);
          n_bc = 9;
          break out;
        }

        // rules (e) and (f) page 632, simplified

        if (crdB != 4 && (valB > crdB || mltbB > 0) || crdC != 4
            && (valC > crdC || mltbC > 0))
          return null;

        // rule (g) page 632 (resonant interactions)

        boolean case2 = (pilpB && mltbC > 0);
        boolean case3 = (pilpC && mltbB > 0);

        if (bondBC.order == 1 && (mltbB > 0 && mltbC > 0 || case2 || case3)) {

          if (pilpB && pilpC)
            return null;

          beta = 6;

          if (case2) {
            pi_bc = (mltbC == 1 ? 0.5 : elemnoB <= 10 && elemnoC <= 10 ? 0.3
                : 0.15);
            break out;
          }

          if (case3) {
            pi_bc = (mltbB == 1 ? 0.5 : elemnoB <= 10 && elemnoC <= 10 ? 0.3
                : 0.15);
            break out;
          }

          if ((mltbB == 1 || mltbC == 1) && (elemnoB == 6 || elemnoC == 6)) {
            pi_bc = 0.4;
            break out;
          }

          pi_bc = 0.15;
          break out;
        }

        // rule (h) page 632 (Eqn 23)  [XD2-XD2]

        switch (elemnoB << 8 + elemnoC) {
        case 0x808: // O, O
          wb = wc = 2;
          break out;
        case 0x810: // O, S
          wb = 2;
          wc = 8;
          break out;
        case 0x1008: // S, O
          wb = 8;
          wc = 2;
          break out;
        case 0x1010: // S, S
          wb = wc = 8;
          break out;
        }

        // everything else -- generic Eqn 22

        n_bc = crdB * crdC;
        break out;
      }
      if (pi_bc > 0)
        v2 = beta * pi_bc * Math.sqrt(ub * uc); // Eqn 21
      else if (n_bc > 0)
        v3 = Math.sqrt(vb * vc) / n_bc; // Eqn 22
      else if (wb != 0)
        v2 = -Math.sqrt(wb * wc); // Eqn 23
      o.ddata = new double[] { v1, v2, v3 };
      return Integer.valueOf(-1);
    default:
      return null;
    }
  }
 
  private static double getR0(MinBond b) {
    return (b.ddata == null ? ((double[]) ffParams.get(b.key)) : b.ddata)[1];   
  }

  private int getRowFor(int i) {
    int elemno = minAtoms[i].atom.getElementNumber();
    return (elemno < 3 ? 0 : elemno < 11 ? 1 : elemno < 19 ? 2 : elemno < 37 ? 3 : 4);
  }

  private int[] typeData = new int[4];
  
  double getOutOfPlaneParameter(int[] data) {
    double[] ddata = (double[]) ffParams.get(getKey(data, KEY_OOP, TYPE_OOP));    
    return (ddata == null ? 0 : ddata[0]);
  }

  private static void sortOop(int[] typeData) {
    fixTypeOrder(typeData, 0, 2);
    fixTypeOrder(typeData, 0, 3);
    fixTypeOrder(typeData, 2, 3);
  }

  /**
   * 
   * @param a
   * @param i
   * @param j
   * @return true if swapped; false if not
   */
  private static boolean fixTypeOrder(int[] a, int i, int j) {
    if (a[i] > a[j]) {
      swap(a, i, j);
      return true;
    }
    return false;
  }

  /**
   * 
   * @param a
   * @param i
   * @param j
   * @return  1 if in order, 0 if same, -1 if reversed
   */
  private int fixOrder(int[] a, int i, int j) {
    int test = typeOf(a[j]) - typeOf(a[i]); 
    if (test < 0)
      swap(a, i, j);
    return (test < 0 ? -1 : test > 0 ? 1 : 0);
  }

  private static void swap(int[] a, int i, int j) {
    int t = a[i];
    a[i] = a[j];
    a[j] = t;
  }

  private final static int[] equivalentTypes = {
    1,  1, //  1
    2,  1, //  2
    3,  1, //  3
    4,  1, //  4
    5,  5, //  5
    6,  6, //  6
    7,  6, //  7
    8,  8, //  8
    9,  8, //  9
   10,  8, // 10
   11, 11, // 11
   12, 12, // 12
   13, 13, // 13
   14, 14, // 14
   15, 15, // 15
   16, 15, // 16
   17, 15, // 17
   18, 15, // 18
   19, 19, // 19
    1,  1, // 20
   21,  5, // 21
   22,  1, // 22
   23,  5, // 23
   24,  5, // 24
   25, 25, // 25
   26, 25, // 26
   28,  5, // 27
   28,  5, // 28
   29,  5, // 29
    2,  1, // 30
   31, 31, // 31
    7,  6, // 32
   21,  5, // 33
    8,  8, // 34
    6,  6, // 35
   36,  5, // 36
    2,  1, // 37
    9,  8, // 38
   10,  8, // 39
   10,  8, // 40
    3,  1, // 41
   42,  8, // 42
   10,  8, // 43
   16, 15, // 44
   10,  8, // 45
    9,  8, // 46
   42,  8, // 47
    9,  8, // 48
    6,  6, // 49
   21,  5, // 50
    7,  6, // 51
   21,  5, // 52
   42,  8, // 53
    9,  8, // 54
   10,  8, // 55
   10,  8, // 56
    2,  1, // 57
   10,  8, // 58
    6,  6, // 59
    4,  1, // 60
   42,  8, // 61
   10,  8, // 62
    2,  1, // 63
    2,  1, // 64
    9,  8, // 65
    9,  8, // 66
    9,  8, // 67
    8,  8, // 68
    9,  8, // 69
   70, 70, // 70
    5,  5, // 71
   16, 15, // 72
   18, 15, // 73
   17, 15, // 74
   26, 25, // 75
    9,  8, // 76
   12, 12, // 77
    2,  1, // 78
    9,  8, // 79
    2,  1, // 80
   10,  8, // 81
    9,  8, // 82
  };

  /**
   * equivalent types for OOP and torsions
   * 
   * @param type  mmFF94 atom type 
   * @param level  0, 1, or 2.
   * @return equivalent type or 0
   * 
   */
  private static int getEquivalentType(int type, int level) {
    return (type == 0 ? 0 : type == 70 || type > 82 ? type : level == 2 ? 0 : 
      equivalentTypes[((type - 1) << 1) + level]);
  }
  

  private static double getZParam(int elemno) {
    switch (elemno) {
    case 1:
      return 1.395;
    case 6:
      return 2.494;
    case 7:
      return 2.711;
    case 8:
      return 3.045;
    case 9:
      return 2.847;
    case 14:
      return 2.350;
    case 15:
      return 2.350;
    case 16:
      return 2.980;
    case 17:
      return 2.909;
    case 35:
      return 3.017;
    case 53:
      return 3.086;
    }
    return 0.0;
  }

  private static double getCParam(int elemno) {
    switch (elemno) {
    case 5:
      return 0.704;
    case 6:
      return 1.016;
    case 7:
      return 1.113;
    case 8:
      return 1.337;
    case 14:
      return 0.811;
    case 15:
      return 1.068;
    case 16:
      return 1.249;
    case 17:
      return 1.078;
    case 33:
      return 0.825;
    }
    return 0.0;
  }

  private static double getUParam(int elemno) {
    switch (elemno) {
    case 6:
    case 7:
    case 8:
      return 2.0;
    case 14:
    case 15:
    case 16:
      return 1.25;
    }
    return 0.0;
  }

  private static double getVParam(int elemno) {
    switch (elemno) {
    case 6:
      return 2.12;
    case 7:
      return 1.5;
    case 8:
      return 0.2;
    case 14:
      return 1.22;
    case 15:
      return 2.4;
    case 16:
      return 0.49;
    }
    return 0.0;
  }

  private static double getCovalentRadius(int elemno) { 
    switch(elemno) {
    case 1:
      return 0.33; // corrected value from MMFF part V
    case 5:
      return 0.81;
    case 6:
      return 0.77; // corrected value from MMFF part V
    case 7:
      return 0.73;
    case 8:
      return 0.72;
    case 9:
      return 0.74;
    case 13:
      return 1.22;
    case 14:
      return 1.15;
    case 15:
      return 1.09;
    case 16:
      return 1.03;
    case 17:
      return 1.01;
    case 31:
      return 1.19;
    case 32:
      return 1.20;
    case 33:
      return 1.20;
    case 34:
      return 1.16;
    case 35:
      return 1.15;
    case 44:
      return 1.46;
    case 50:
      return 1.40;
    case 51:
      return 1.41;
    case 52:
      return 1.35;
    case 53:
      return 1.33;
    case 81:
      return 1.51;
    case 82:
      return 1.53;
    case 83:
      return 1.55;
    default:
      return Elements.getBondingRadius(elemno, 0);
    }
  }

//  private final static double[] r0reductions = new double[] { 
//    0.08, 0.03, 0.10, 0.17, 0.075, 0.04 
//  };

  private static double getRuleBondLength(MinAtom a, MinAtom b, int boAB,
                                          boolean isAromatic) {

    switch  (boAB) {
    case 1:
    case 2:
    case 3:
      break;
    case 5: // aromatic
      break;
    default:
      return 0;
    }
    // Eqn 18, p. 625

    // r0ij = r0i + r0j - c|xi - xj|^n - delta

    int elemnoA = a.atom.getElementNumber();
    int elemnoB = b.atom.getElementNumber();
    double r0a = getCovalentRadius(elemnoA);
    double r0b = getCovalentRadius(elemnoB);
    double Xa = Elements.getAllredRochowElectroNeg(elemnoA);
    double Xb = Elements.getAllredRochowElectroNeg(elemnoB);

    double c = (elemnoA == 1 || elemnoB == 1 ? 0.05 : 0.085);
    //double delta = 0.008;
    double n = 1.4;
    
    double r = r0a + r0b;
    
    if (isAromatic)
      boAB = (a.ffAtomType.pilp || b.ffAtomType.pilp ? 5 : 4);
    else
      switch (a.ffAtomType.mltb << 4 + b.ffAtomType.mltb) {
      case 0x11:
        boAB = 4;
        break;
      case 0x12:
      case 0x21:
        boAB = 5;
        break;
      }

    switch (boAB) {
    case 1:
      // only single bonds involve hybridization
      // Halgren uses a complicated way of addressing this, 
      // but it comes down to the fact that 

      switch (a.ffAtomType.mltb) {
      case 0:                   // sp3 "H = 3"
        break;
      case 1:
      case 2:   
        //red += r0reductions[1]; // sp2  "H = 2"
        break;
      case 3:
        //red += r0reductions[0]; // sp   "H = 1"
        break;
      }
      
      // for some reason mltb for RO- is 1
      
      switch (b.ffAtomType.mltb) {
      case 0:                   // sp3 "H = 3"
        break;
      case 1:
      case 2:
        //red += r0reductions[1]; // sp2  "H = 2"
        break;
      case 3:
        //red += r0reductions[0]; // sp   "H = 1"
        break;
      }
      break;
    default:
      //red += 2 * r0reductions[boAB];
      break;
    }
    r -= c * Math.pow(Math.abs(Xa - Xb), n);
    //r -= red;
    //r -= delta; 

    // Well, guess what? Actually red and delta are not used.
    
    // -- at least not in the program run that is at the validation site:
    //  OPTIMOL: Molecular and Macromolecular Optimization Package 17-Nov-98 16:01:23
    // SGI double-precision version ... Updated 5/6/98
    // 
    // This calculation is run for only the following structures:
    //
    //             bond      red     delta  ro(here/valid)  kb(here/valid)  Etotal(here/valid)
    //            ---------------------------------------------------------------------------------------
    // OHWM1       H1-O1     0.03    0.008  0.978/0.978       7.510/7.51     -21.727/-21.72690
    // ERULE_03    Si1-P1    0.0     0.008  2.223/2.224       1.614/1.609    -2.983/ -2.93518
    // ERULE_06    N1-F1     0.0     0.008  1.381/1.379       5.372/5.438      1.582/  1.58172
    //
    // and in each case, we match the results well, but only without red and delta. 

    return r;
  }

}
