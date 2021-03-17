/* $RCSfile$
 * $Author: egonw $
 * $Date: 2006-03-18 15:59:33 -0600 (Sat, 18 Mar 2006) $
 * $Revision: 4652 $
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

import org.jmol.adapter.smarter.Bond;
import org.jmol.adapter.smarter.Atom;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;

import java.util.Hashtable;

import java.util.Map;

import org.jmol.util.Logger;

/**
 * CSF file reader based on CIF idea -- fluid property fields.
 *
 * note that, like CIF, the order of fields is totally unpredictable
 * in addition, ID numbers are not sequential, requiring atomNames
 * 
 * first crack at this 2006/04/13
 * added DGAUSS, MOPAC, EHT orbital/basis reading 2007/04/09
 * streamlined CSF dataset reading capabilities 2007/04/09
 * 
 * 
 * @author hansonr <hansonr@stolaf.edu>
 */
public class CsfReader extends MopacSlaterReader {

  private int nAtoms = 0;
  private String strAtomicNumbers = "";
  private int fieldCount;
  private int nVibrations = 0;
  private int nGaussians = 0;
  private int nSlaters = 0;
  
  private Map<String, Bond> htBonds;
  
  @Override
  protected boolean checkLine() throws Exception {
    if (line.equals("local_transform")) {
      processLocalTransform();
      return true;
    }
    if (line.startsWith("object_class")) {
      if (line.equals("object_class connector")) {
        processConnectorObject();
        return false;
      }
      if (line.equals("object_class atom")) {
        processAtomObject();
        return false;
      }
      if (line.equals("object_class bond")) {
        processBondObject();
        return false;
      }
      if (line.equals("object_class vibrational_level")) {
        processVibrationObject();
        return false;
      }
      if (line.equals("object_class mol_orbital")) {
        processMolecularOrbitalObject();
        return false;
      }
      if (line.equals("object_class sto_basis_fxn")) {
        processBasisObject("sto");
        return false;
      }
      if (line.equals("object_class gto_basis_fxn")) {
        processBasisObject("gto");
        return false;
      }
    }
    return true;
  }
 
  /*
   local_transform
   0.036857 -0.132149 0.003770 0.000000
   -0.118510 -0.031292 0.061744 0.000000
   -0.058592 -0.019837 -0.122513 0.000000
   -0.000142 0.132130 0.060863 1.000000

   */
  private void processLocalTransform() throws Exception {
    String[] tokens = PT.getTokens(rd() + " " + rd() + " "+ rd() + " " + rd());
    setTransform(
        parseFloatStr(tokens[0]), parseFloatStr(tokens[1]), parseFloatStr(tokens[2]), 
        parseFloatStr(tokens[4]), parseFloatStr(tokens[5]), parseFloatStr(tokens[6]),
        parseFloatStr(tokens[8]), parseFloatStr(tokens[9]), parseFloatStr(tokens[10])
        );
  }

  private Map<String, Integer> propertyItemCounts = new Hashtable<String, Integer>();
  private final int[] fieldTypes = new int[100]; // should be enough
  
  private int getPropertyCount(String what) {
    Integer count = propertyItemCounts.get(what);
    return (what.equals("ID") ? 1 : count == null ? 0 : count.intValue());
  }
  
  private int parseLineParameters(String[] fields,
                          byte[] fieldMap) throws Exception {
    for (int i = 0; i < fieldCount; i++)
      fieldTypes[i] = 0;
    fieldCount = -1;
    if (line == null || line.startsWith("property_flags:"))
      rd();
    if (line == null || line.startsWith("object_class"))
      return fieldCount;

    String[] tokens = new String[0];
    //property xyz_coordinates Linus angstrom 6 3 FLOAT

    while (line != null) {
      tokens = getTokens();
      if (line.indexOf("property ") == 0)
        propertyItemCounts.put(tokens[1], Integer.valueOf((tokens[6].equals("STRING") ? 1 : parseIntStr(tokens[5]))));
      else if (line.indexOf("ID") == 0)
        break;
      rd();
    }
    // ID line:
    for (int ipt = 0, fpt = 0; ipt < tokens.length; ipt++ ) {
      String field = tokens[ipt];
      for (int i = fields.length; --i >= 0; )
        if (field.equals(fields[i])) {
          fieldTypes[fpt] = fieldMap[i];
          fieldCount = fpt + 1;
          break;
        }
      fpt += getPropertyCount(field);
    }
    return fieldCount;
  }

  private void fillCsfArray(String property, String[] tokens, int i0, Object f, boolean isInteger)
      throws Exception {
    // handles the continuation. i0 should be 1 for actual continuation, I think.
    int n = getPropertyCount(property);
    int ioffset = i0;
    for (int i = 0; i < n; i++) {
      int ipt = ioffset + i;
      if (ipt == tokens.length) {
        tokens = PT.getTokens(rd());
        ioffset -= ipt - i0;
        ipt = i0;
      }
      if (isInteger)
        ((int[]) f)[i] = parseIntStr(tokens[ipt]);
      else
        ((float[]) f)[i] = parseFloatStr(tokens[ipt]);
    }
  }

  ////////////////////////////////////////////////////////////////
  // connector data
  ////////////////////////////////////////////////////////////////

  private final static byte objCls1 = 1;
  private final static byte objID1  = 2;
  private final static byte objCls2 = 3;
  private final static byte objID2  = 4;
  
  private final static String[] connectorFields = {
    "objCls1", "objID1", "objCls2", "objID2"
  };

  private final static byte[] connectorFieldMap = {
    objCls1, objID1, objCls2, objID2
  };
  
  private Map<String, String[]> connectors;
  
  private void processConnectorObject() throws Exception {
    connectors = new Hashtable<String, String[]>();
    rd();
    parseLineParameters(connectorFields, connectorFieldMap);
    out: for (; rd() != null;) {
      if (line.startsWith("property_flags:"))
        break;
      String thisAtomID = null;
      String thisBondID = null;
      String tokens[] = getTokens();
      String field2 = "";
      boolean isVibration = false;
      for (int i = 0; i < fieldCount; ++i) {
        String field = tokens[i];
        switch (fieldTypes[i]) {
        case objCls1:
          if (!field.equals("atom"))
            continue out;
          break;
        case objCls2:
          field2 = field;
          if (field.equals("sto_basis_fxn"))
            nSlaters++;
          else if (field.equals("gto_basis_fxn"))
            nGaussians++;
          else if (field.equals("vibrational_level"))
            isVibration = true;
          else if (!field.equals("bond")) 
            continue out;
          break;
        case objID1:
          thisAtomID = field;
          break;
        case objID2:
          thisBondID = field2+field;
          if (isVibration)
            nVibrations = Math.max(nVibrations, parseIntStr(field));
          break;
        default:
        }
      }
      if (thisAtomID != null && thisBondID != null) {
        if (connectors.containsKey(thisBondID)) {
          String[] connect = connectors.get(thisBondID);
          connect[1] = thisAtomID;
          if (htBonds != null)
            setBond(htBonds.get(thisBondID), connect);
        } else {
          String[] connect = new String[2];
          connect[0] = thisAtomID;
          connectors.put(thisBondID, connect);
        }
      }
    }
  }

  ////////////////////////////////////////////////////////////////
  // atom data
  ////////////////////////////////////////////////////////////////

  private void setBond(Bond bond, String[] connect) {
    bond.atomIndex1 = asc.getAtomIndex(connect[0]);
    bond.atomIndex2 = asc.getAtomIndex(connect[1]);
    asc.addBond(bond);
  }

  private final static byte ID     = -1;

  private final static byte SYM    = 1;
  private final static byte ANUM   = 2;
  private final static byte CHRG   = 3;
  private final static byte XYZ    = 4;
  private final static byte PCHRG  = 5;
  

  private final static String[] atomFields = {
    "ID", "sym", "anum", "chrg", "xyz_coordinates", "pchrg"
  };

  private final static byte[] atomFieldMap = {
    ID, SYM, ANUM, CHRG, XYZ, PCHRG
  };

  private void processAtomObject() throws Exception {
    rd();
    parseLineParameters(atomFields, atomFieldMap);
    nAtoms = 0;
    for (; rd() != null; ) {
      if (line.startsWith("property_flags:"))
        break;
      String tokens[] = getTokens();
      Atom atom = new Atom();
      String field;
      for (int i = 0; i < fieldCount; i++) {
        int type = fieldTypes[i];
        if (type == 0)
          continue;
        if ((field = tokens[i]) == null)
          Logger.warn("field == null in " + line);
        switch (type) {
        case ID:
          atom.atomSerial = PT.parseInt(field);
          break;
        case SYM:
          atom.elementSymbol = field;
          atom.atomName = field + atom.atomSerial;
          break;
        case ANUM:
          strAtomicNumbers += field + " "; // for MO slater basis calc
          break;
        case CHRG:
          atom.formalCharge = parseIntStr(field);
          break;
        case PCHRG:
          atom.partialCharge = parseFloatStr(field);
          break;
        case XYZ:
          setAtomCoordTokens(atom, tokens, i);
          i += 2;
          break;
        }
      }
      if (Float.isNaN(atom.x) || Float.isNaN(atom.y) || Float.isNaN(atom.z)) {
        Logger.warn("atom " + atom.atomName + " has invalid/unknown coordinates");
      } else {
        nAtoms++;
        asc.addAtomWithMappedSerialNumber(atom);
      }
    }
  }

  ////////////////////////////////////////////////////////////////
  // bond order data
  ////////////////////////////////////////////////////////////////

  private final static byte BTYPE = 1;

  private final static String[] bondFields  = {
    "ID", "type"
  };

  private final static byte[] bondFieldMap = {
    ID, BTYPE
  };

  private void processBondObject() throws Exception {
    rd();
    parseLineParameters(bondFields, bondFieldMap);
    for (; rd() != null;) {
      if (line.startsWith("property_flags:"))
        break;
      String thisBondID = null;
      String tokens[] = getTokens();
      for (int i = 0; i < fieldCount; ++i) {
        String field = tokens[i];
        switch (fieldTypes[i]) {
        case ID:
          thisBondID = "bond" + field;
          break;
        case BTYPE:
          int order = 1;
          if (field.equals("single"))
            order = 1;
          else if (field.equals("double"))
            order = 2;
          else if (field.equals("triple"))
            order = 3;
          else
            Logger.warn("unknown CSF bond order: " + field);
          Bond bond = new Bond(-1, -1, 1);
          bond.order = order;
          if (connectors == null) {
            if (htBonds == null)
              htBonds = new Hashtable<String, Bond>();
            htBonds.put(thisBondID, bond);
          } else {
            setBond(bond, connectors.get(thisBondID));
          }
          break;
        }
      }
    }
  }

  
  private final static byte NORMAL_MODE = 1;
  private final static byte VIB_ENERGY = 2;
  private final static byte DIPOLE = 3;

  private final static String[] vibFields = {
    "ID", "normalMode", "Energy", "transitionDipole"
  };

  private final static byte[] vibFieldMap = {
    ID, NORMAL_MODE, VIB_ENERGY, DIPOLE
  };

  private void processVibrationObject() throws Exception {
    //int iatom = asc.getFirstAtomSetAtomCount();
    float[][] vibData = new float[nVibrations][nAtoms * 3];
    String[] energies = new String[nVibrations];
    rd();
    while (line != null && parseLineParameters(vibFields, vibFieldMap) > 0) {
      while (rd() != null && !line.startsWith("property_flags:")) {
        String tokens[] = getTokens();
        int thisvib = -1;
        for (int i = 0; i < fieldCount; ++i) {
          String field = tokens[i];
          switch (fieldTypes[i]) {
          case ID:
            thisvib = parseIntStr(field) - 1;
            break;
          case NORMAL_MODE:
            fillCsfArray("normalMode", tokens, i, vibData[thisvib], false);
            break;
          case VIB_ENERGY:
            energies[thisvib] = field;
            break;
          }
        }
      }
    }
    for (int i = 0; i < nVibrations; i++) {
      if (!doGetVibration(++vibrationNumber))
        continue;
      asc.cloneAtomSetWithBonds(false);
      asc.setAtomSetFrequency(vibrationNumber, null, null, energies[i], null);
      int ipt = 0;
      int baseAtom = nAtoms * (i + 1);
      for (int iAtom = 0; iAtom < nAtoms; iAtom++)
        asc.addVibrationVector(baseAtom + iAtom,
            vibData[i][ipt++],
            vibData[i][ipt++], 
            vibData[i][ipt++]);
    }
  }

  ////////////////////////////////////////////////////////////////
  // Molecular Orbitals
  ////////////////////////////////////////////////////////////////

  private final static byte EIG_VAL = 1;
  private final static byte MO_OCC  = 2;
  private final static byte EIG_VEC = 3;
  private final static byte EIG_VEC_COMPRESSED = 4;
  private final static byte COEF_INDICES  = 5;
  private final static byte BFXN_ANGL  = 6;
  private final static byte STO_EXP  = 7;
  private final static byte CONTRACTIONS  = 8;
  private final static byte GTO_EXP = 9;
  private final static byte SHELL = 10;

  private final static String[] moFields = {
    "ID", "eig_val", "mo_occ", "eig_vec",
      "eig_vec_compressed", "coef_indices", "bfxn_ang", "sto_exp",
      "contractions", "gto_exp", "shell"
  };

  private final static byte[] moFieldMap = {
    ID, EIG_VAL, MO_OCC, EIG_VEC, EIG_VEC_COMPRESSED, 
    COEF_INDICES, BFXN_ANGL, STO_EXP, CONTRACTIONS, GTO_EXP, SHELL
  };
   
  private void processMolecularOrbitalObject() throws Exception {
    if (nSlaters == 0 && nGaussians == 0 ||  !doReadMolecularOrbitals) {
      rd();
      return; // no slaters or gaussians?;
    }
    /* we read the following blocks in ANY order:
     
     ID dflag eig_val    mo_occ
     1   0x0 -36.825790 2.00000000
     2   0x0 -17.580715 2.00000000
     3   0x0 -14.523387 2.00000000
     4   0x0 -12.316568 2.00000000
     5   0x0   4.060438 0.00000000
     6   0x0   5.331586 0.00000000

     ID eig_vec_compressed                                          nom_coef 
     1 -0.845963 -0.179125 -0.179118 -0.067970  0.049666  0.000000        5
     2 -0.517325 -0.474120  0.474119 -0.377978  0.000000  0.000000        4
     3 -0.638505  0.466520  0.400882 -0.255125 -0.255118  0.000000        5
     4 -0.999990  0.000000  0.000000  0.000000  0.000000  0.000000        1
     5  0.582228  0.582125 -0.529468 -0.521559  0.386787 -0.004860        6
     6 -0.906355  0.906280 -0.753041 -0.550159  0.000000  0.000000        4
     
     ID coef_indices
     1  2 1 6 4 3 0
     2  3 6 1 4 0 0
     3  4 3 2 6 1 0
     4  5 0 0 0 0 0
     5  6 1 4 2 3 5
     6  1 6 3 4 0 0
     
     ID eig_vec
     1 -0.245163 -0.011925  0.000554  0.000542 -0.236038 -0.002974  0.006251
     1 -0.000460 -0.231155  0.003499  0.009902  0.000555 -0.236059  0.006221
     1  0.002910  0.001090 -0.245083  0.008801 -0.006892  0.004063 -0.264182
     1 -0.001313 -0.005736 -0.004526 -0.166087 -0.008065 -0.001462 -0.002563
     1 -0.166219  0.005699 -0.006460 -0.000149 -0.021764 -0.016402 -0.019220
     1 -0.014385 -0.022278 -0.016332 -0.019246 -0.021743 -0.023016 -0.018217
     1 -0.013078 -0.016269 -0.012006 -0.016322 -0.013100 -0.011989
     2  0.289501 -0.029400  0.007611 -0.002158  0.222093 -0.028271 -0.003105
     2 -0.004796 -0.000433 -0.025248  0.009182 -0.004274 -0.222016 -0.019921
     2  0.020485 -0.003535 -0.289379 -0.026658  0.012619 -0.007483  0.000107
     2 -0.046398  0.016755 -0.008013  0.351901  0.007737  0.007248 -0.001799
     2 -0.351606  0.000505 -0.010223  0.003301  0.024485  0.031084  0.019300
     2  0.000034 -0.000129 -0.031187 -0.019043 -0.024563  0.000012 -0.000014
     2  0.028784  0.031377  0.035556 -0.031461 -0.028669 -0.035517

     */

    nOrbitals = (nSlaters + nGaussians);
    Logger.info("Reading CSF data for " + nOrbitals + " molecular orbitals");
    float[] energy = new float[nOrbitals];
    float[] occupancy = new float[nOrbitals];
    float[][] list = new float[nOrbitals][nOrbitals];
    float[][] listCompressed = null;
    int[][] coefIndices = null;
    int ipt = 0;
    boolean isCompressed = false;
    rd();
    while (line != null && parseLineParameters(moFields, moFieldMap) > 0)
      while (rd() != null && !line.startsWith("property_flags:")) {
        String tokens[] = getTokens();
        for (int i = 0; i < fieldCount; ++i) {
          switch (fieldTypes[i]) {
          case ID:
            ipt = parseIntStr(tokens[i]) - 1;
            break;
          case EIG_VAL:
            energy[ipt] = parseFloatStr(tokens[i]);
            break;
          case MO_OCC:
            occupancy[ipt] = parseFloatStr(tokens[i]);
            break;
          case EIG_VEC:
            fillCsfArray("eig_vec", tokens, i, list[ipt], false);
            break;
          case EIG_VEC_COMPRESSED:
            isCompressed = true;
            if (listCompressed == null)
              listCompressed = new float[nOrbitals][nOrbitals];
            fillCsfArray("eig_vec_compressed", tokens, i, listCompressed[ipt], false);
            break;
          case COEF_INDICES:
            if (coefIndices == null)
              coefIndices = new int[nOrbitals][nOrbitals];
            fillCsfArray("coef_indices", tokens, i, coefIndices[ipt], true);
            break;
          }
        }
      }
    //put it all together
    for (int iMo = 0; iMo < nOrbitals; iMo++) {
      if (isCompressed) { // must uncompress
        for (int i = 0; i < coefIndices[iMo].length; i++) {
          int pt = coefIndices[iMo][i] - 1;
          if (pt < 0)
            break;
          list[iMo][pt] = listCompressed[iMo][i];
        }
      }
      for (int i = 0; i < nOrbitals; i++)
        if (Math.abs(list[iMo][i]) < MIN_COEF)
          list[iMo][i] = 0;
      Map<String, Object> mo = new Hashtable<String, Object>();
      mo.put("energy", Float.valueOf(energy[iMo]));
      mo.put("occupancy", Float.valueOf(occupancy[iMo]));
      mo.put("coefficients", list[iMo]);
//       System.out.print("MO " + iMo + " : ");
//       for (int i = 0; i < nOrbitals; i++)
//       System.out.print(" " + list[iMo][i]);
//       System.out.println();
      setMO(mo);
    }
    setMOs("eV");
  }

  private void processBasisObject(String sto_gto) throws Exception {
    String[] atomNos = PT.getTokens(strAtomicNumbers);
    atomicNumbers = new int[atomNos.length];
    for (int i = 0; i < atomicNumbers.length; i++)
      atomicNumbers[i] = parseIntStr(atomNos[i]);

    /*
     ID dflag bfxn_ang contr_len Nquant sto_exp  shell
     1   0x0        S         6      1 0.967807     1
     2   0x0        S         6      2 3.796544     2
     3   0x0       Px         6      2 2.389402     3
     4   0x0       Py         6      2 2.389402     3
     5   0x0       Pz         6      2 2.389402     3
     6   0x0        S         6      1 0.967807     4
     */

    nOrbitals = (nSlaters + nGaussians);
    boolean isGaussian = (sto_gto.equals("gto"));
    float[][] zetas = AU.newFloat2(nOrbitals);
    float[][] contractionCoefs = null;
    String[] types = new String[nOrbitals];
    int[] shells = new int[nOrbitals];
    int nZetas = 0;

    rd();
    while (line != null && parseLineParameters(moFields, moFieldMap) > 0) {
      if (nZetas == 0)
        nZetas = getPropertyCount(sto_gto + "_exp");
      int ipt = 0;
      while (rd() != null && !line.startsWith("property_flags:")) {
        String tokens[] = getTokens();
        for (int i = 0; i < fieldCount; ++i) {
          String field = tokens[i];
          switch (fieldTypes[i]) {
          case ID:
            ipt = parseIntStr(field) - 1;
            break;
          case BFXN_ANGL:
            types[ipt] = field;
            break;
          case STO_EXP:
          case GTO_EXP:
            zetas[ipt] = new float[nZetas];
            fillCsfArray(sto_gto + "_exp", tokens, i, zetas[ipt], false);
            break;
          case SHELL:
            shells[ipt] = parseIntStr(field);
            break;
          case CONTRACTIONS:
            if (contractionCoefs == null)
              contractionCoefs = new float[nOrbitals][nZetas];
            fillCsfArray("contractions", tokens, i, contractionCoefs[ipt], false);
          }
        }
      }
    }
    if (isGaussian) {
      Lst<int[]> sdata = new  Lst<int[]>();
      Lst<float[]> gdata = new  Lst<float[]>();
      int iShell = 0;
      int gaussianCount = 0;
      for (int ipt = 0; ipt < nGaussians; ipt++) {
        if (shells[ipt] != iShell) {
          iShell = shells[ipt];
          int[] slater = new int[4];
          int iAtom = asc
              .getAtomIndex(connectors.get(sto_gto + "_basis_fxn" + (ipt + 1))[0]);
          slater[0] = iAtom + 1;
          slater[1] = BasisFunctionReader.getQuantumShellTagID(types[ipt]
              .substring(0, 1));
          int nZ = 0;
          while (++nZ < nZetas && zetas[ipt][nZ] != 0) {
          }
          slater[2] = gaussianCount; //pointer
          slater[3] = nZ;
          sdata.addLast(slater);
          gaussianCount += nZ;
          for (int i = 0; i < nZ; i++)
            gdata.addLast(new float[] { zetas[ipt][i], contractionCoefs[ipt][i] });
        }
      }
      float[][] garray = AU.newFloat2(gaussianCount);
      for (int i = 0; i < gaussianCount; i++)
        garray[i] = gdata.get(i);
      moData.put("shells", sdata);
      moData.put("gaussians", garray);
    } else {
      for (int ipt = 0; ipt < nSlaters; ipt++) {
        int iAtom = asc.getAtomIndex(connectors
            .get(sto_gto + "_basis_fxn" + (ipt + 1))[0]);
        for (int i = 0; i < nZetas; i++) {
          if (zetas[ipt][i] == 0)
            break;
          createSphericalSlaterByType(iAtom, atomicNumbers[iAtom], types[ipt], zetas[ipt][i]
              * (i == 0 ? 1 : -1), contractionCoefs == null ? 1
              : contractionCoefs[ipt][i]);
        }
      }
      setSlaters(false); 
    }
  }  
}
