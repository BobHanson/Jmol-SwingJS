/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-07-05 00:45:22 -0500 (Wed, 05 Jul 2006) $
 * $Revision: 5271 $
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

package org.jmol.adapter.readers.spartan;


import org.jmol.adapter.readers.quantum.BasisFunctionReader;
import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Bond;
import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolAdapter;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;

import java.util.Hashtable;
import java.util.Map;


import org.jmol.quantum.QS;
import org.jmol.util.Logger;
import javajs.util.V3;

class SpartanArchive {
  
  // OK, a bit of a hack.
  // It's not a reader, but it needs the capabilities of BasisFunctionReader
  

  private int modelCount = 0;
  private int modelAtomCount = 0;  
  private int ac = 0;
  private String bondData; // not in archive; may or may not have
  private int moCount = 0;
  private int coefCount = 0;
  private int shellCount = 0;
  private int gaussianCount = 0;
  private String endCheck; // for SMOL or SPARTAN files: "END Directory Entry "
  private boolean isSMOL;
  
  private BasisFunctionReader r;

  SpartanArchive(BasisFunctionReader r, String bondData, String endCheck, int smolAtomCount) {
    initialize(r, bondData);
    this.modelAtomCount = smolAtomCount;
    this.endCheck = endCheck;
    isSMOL = (endCheck != null);
  }

  private void initialize(BasisFunctionReader r, String bondData) {
    this.r = r;
    r.moData.put("isNormalized", Boolean.TRUE);
    r.moData.put("energyUnits","");
    this.bondData = bondData;
  }

  int readArchive(String infoLine, boolean haveGeometryLine, 
                  int ac0, boolean doAddAtoms) throws Exception {
    modelAtomCount = setInfo(infoLine);
    line = (haveGeometryLine ? "GEOMETRY" : "");
    boolean haveMOData = false;
    boolean skipping = false;
    while (line != null) {
      if (line.equals("GEOMETRY")) {
        if (!isSMOL && !r.doGetModel(++modelCount, null)) {
          readLine();
          skipping = true;
          continue;
        }
        skipping = false;  
        readAtoms(ac0, doAddAtoms);
        if (doAddAtoms && bondData.length() > 0)
          addBonds(bondData, ac0);
      } else if (line.indexOf("BASIS") == 0) {
        if (r.doReadMolecularOrbitals) {
          readBasis();
        } else {
          r.discardLinesUntilContains("ENERGY");
          line = r.line;
          continue;
        }
      } else if (line.indexOf("WAVEFUNC") == 0 || line.indexOf("BETA") == 0) {
        if (r.doReadMolecularOrbitals && !skipping) {
          readMolecularOrbital();
          haveMOData = true;
        } else {
          r.discardLinesUntilContains("GEOM");
          line = r.line;
        }
      } else if (line.indexOf("ENERGY") == 0 && !skipping) {
        readEnergy();
      } else if (line.equals("ENDARCHIVE")
          || isSMOL && line.indexOf(endCheck) == 0) {
        break;
      }
      readLine();
    }
    if (haveMOData)
      r.finalizeMOData(r.moData);
    return modelAtomCount;
  }

  private void readEnergy() throws Exception {
    String[] tokens = PT.getTokens(readLine());
    float value = parseFloat(tokens[0]);
    r.asc.setCurrentModelInfo("energy", Float.valueOf(value));
    if (isSMOL)
      ((SpartanSmolReader)r).setEnergy(value); 
    r.asc.setAtomSetEnergy(tokens[0], value);
  }

  private int setInfo(String info) throws Exception {
    //    5  17  11  18   0   1  17   0 RHF      3-21G(d)           NOOPT FREQ
    //    0   1  2   3    4   5   6   7  8        9

    String[] tokens = PT.getTokens(info);
    if (Logger.debugging) {
      Logger.debug("reading Spartan archive info :" + info);
    }
    modelAtomCount = parseInt(tokens[0]);
    coefCount = parseInt(tokens[1]);
    shellCount = parseInt(tokens[2]);
    gaussianCount = parseInt(tokens[3]);
    //overallCharge = parseInt(tokens[4]);
    moCount = parseInt(tokens[6]);
    r.calculationType = tokens[9];
    String s = (String) r.moData.get("calculationType");
    if (s == null)
      s = r.calculationType;
    else if (s.indexOf(r.calculationType) < 0)
      s = r.calculationType + s;
    r.moData.put("calculationType", r.calculationType = s);
    return modelAtomCount;
  }

  private void readAtoms(int ac0, boolean doAddAtoms) throws Exception {
    for (int i = 0; i < modelAtomCount; i++) {
      String tokens[] = PT.getTokens(readLine());
      Atom atom = (doAddAtoms ? r.asc.addNewAtom()
          : r.asc.atoms[ac0 - modelAtomCount + i]);
      atom.elementSymbol = AtomSetCollectionReader
          .getElementSymbol(parseInt(tokens[0]));
      r.setAtomCoordScaled(atom, tokens, 1, AtomSetCollectionReader.ANGSTROMS_PER_BOHR);
    }
    if (doAddAtoms && Logger.debugging) {
      Logger.debug(r.asc.ac + " atoms read");
    }
  }

  void addBonds(String data, int ac0) {
    /* from cached data:
     
     <one number per atom>
     1    2    1
     1    3    1
     1    4    1
     1    5    1
     1    6    1
     1    7    1

     */

    String tokens[] = PT.getTokens(data);
    for (int i = modelAtomCount; i < tokens.length;) {
      int sourceIndex = parseInt(tokens[i++]) - 1 + ac0;
      int targetIndex = parseInt(tokens[i++]) - 1 + ac0;
      int bondOrder = parseInt(tokens[i++]);
      if (bondOrder > 0) {
        r.asc.addBond(new Bond(sourceIndex, targetIndex,
            bondOrder < 4 ? bondOrder : bondOrder == 5 ? JmolAdapter.ORDER_AROMATIC : 1));
      }
    }
    int bondCount = r.asc.bondCount;
    if (Logger.debugging) {
      Logger.debug(bondCount + " bonds read");
    }
  }

  /* 
   * Jmol:   XX, YY, ZZ, XY, XZ, YZ 
   * qchem: dxx, dxy, dyy, dxz, dyz, dzz : VERIFIED
   * Jmol:   d0, d1+, d1-, d2+, d2-
   * qchem: d 1=d2-, d 2=d1-, d 3=d0, d 4=d1+, d 5=d2+
   * Jmol:   XXX, YYY, ZZZ, XYY, XXY, XXZ, XZZ, YZZ, YYZ, XYZ
   * qchem: fxxx, fxxy, fxyy, fyyy, fxxz, fxyz, fyyz, fxzz, fyzz, fzzz
   * Jmol:   f0, f1+, f1-, f2+, f2-, f3+, f3-
   * qchem: f 1=f3-, f 2=f2-, f 3=f1-, f 4=f0, f 5=f1+, f 6=f2+, f 7=f3+
   * 
   */

  //private static String DS_LIST = "d2-   d1-   d0    d1+   d2+";
  //private static String FS_LIST = "f3-   f2-   f1-   f0    f1+   f2+   f3+";
  //private static String DC_LIST = "DXX   DXY   DYY   DXZ   DYZ   DZZ";
  //private static String FC_LIST = "XXX   XXY   XYY   YYY   XXZ   XYZ   YYZ   XZZ   YZZ   ZZZ";

  void readBasis() throws Exception {
    /*
     * standard Gaussian format:
     
     BASIS
     0   2   1   1   0
     0   1   3   1   0
     0   3   4   2   0
     1   2   7   2   0
     1   1   9   2   0
     0   3  10   3   0
     ...
     5.4471780000D+00
     3.9715132057D-01   0.0000000000D+00   0.0000000000D+00   0.0000000000D+00
     8.2454700000D-01
     5.5791992333D-01   0.0000000000D+00   0.0000000000D+00   0.0000000000D+00

     */

    Lst<int[]> shells = new  Lst<int[]>();
    float[][] gaussians = AU.newFloat2(gaussianCount);
    int[] typeArray = new int[gaussianCount];
    //if (false) { // checking these still
    // r.getDFMap(DC_LIST, JmolAdapter.SHELL_D_CARTESIAN, BasisFunctionReader.CANONICAL_DC_LIST, 3);
    // r.getDFMap(FC_LIST, JmolAdapter.SHELL_F_CARTESIAN, BasisFunctionReader.CANONICAL_FC_LIST, 3);
    // r.getDFMap(DS_LIST, JmolAdapter.SHELL_D_SPHERICAL, BasisFunctionReader.CANONICAL_DS_LIST, 3);
    // r.getDFMap(FS_LIST, JmolAdapter.SHELL_F_SPHERICAL, BasisFunctionReader.CANONICAL_FS_LIST, 3);
    // }
    for (int i = 0; i < shellCount; i++) {
      String[] tokens = PT.getTokens(readLine());
      boolean isSpherical = (tokens[4].charAt(0) == '1');
      int[] slater = new int[4];
      slater[0] = parseInt(tokens[3]); //atom pointer; 1-based
      int iBasis = parseInt(tokens[0]); //0 = S, 1 = SP, 2 = D, 3 = F
      switch (iBasis) {
      case 0:
        iBasis = QS.S;
        break;
      case 1:
        iBasis = (isSpherical ? QS.P : QS.SP);
        break;
      case 2:
        iBasis = (isSpherical ? QS.DS : QS.DC);
        break;
      case 3:
        iBasis = (isSpherical ? QS.FS : QS.FC);
        break;
      }
      slater[1] = iBasis;
      slater[2] = parseInt(tokens[2]);
      int gaussianPtr = slater[2] - 1;
      int nGaussians = slater[3] = parseInt(tokens[1]);
      for (int j = 0; j < nGaussians; j++)
        typeArray[gaussianPtr + j] = iBasis;
      shells.addLast(slater);
    }
    for (int i = 0; i < gaussianCount; i++) {
      float alpha = parseFloat(readLine());
      String[] tokens = PT.getTokens(readLine());
      int nData = tokens.length;
      float[] data = new float[nData + 1];
      data[0] = alpha;
      //we put D and F into coef 1. This may change if I find that Gaussian output
      //lists D and F in columns 3 and 4 as well.
      switch (typeArray[i]) {
      case QS.S:
        data[1] = parseFloat(tokens[0]);
        break;
      case QS.P:
        data[1] = parseFloat(tokens[1]);
        break;
      case QS.SP:
        data[1] = parseFloat(tokens[0]);
        data[2] = parseFloat(tokens[1]);
        if (data[1] == 0) {
          data[1] = data[2];
          typeArray[i] = QS.SP;
        }
        break;
      case QS.DC:
      case QS.DS:
        data[1] = parseFloat(tokens[2]);
        break;
      case QS.FC:
      case QS.FS:
        data[1] = parseFloat(tokens[3]);
        break;
      }
      gaussians[i] = data;
    }
    int nCoeff = 0;
    for (int i = 0; i < shellCount; i++) {
      int[] slater = shells.get(i);
      switch(typeArray[slater[2] - 1]) {
      case QS.S:
        nCoeff++;
        break;
      case QS.P:
        slater[1] = QS.P;
        nCoeff += 3;
        break;
      case QS.SP:
        nCoeff += 4;
        break;
      case QS.DS:
        nCoeff += 5;
        break;
      case QS.DC:
        nCoeff += 6;
        break;
      case QS.FS:
        nCoeff += 7;
        break;
      case QS.FC:
        nCoeff += 10;
        break;
      }
    }
    boolean isD5F7 = (nCoeff < coefCount);
    if (isD5F7)
    for (int i = 0; i < shellCount; i++) {
      int[] slater = shells.get(i);
      switch (typeArray[i]) {
      case QS.DC:
        slater[1] = QS.DS;
        break;
      case QS.FC:
        slater[1] = QS.FS;
        break;
      }
    }
    r.moData.put("shells", shells);
    r.moData.put("gaussians", gaussians);
    if (Logger.debugging) {
      Logger.debug(shells.size() + " slater shells read");
      Logger.debug(gaussians.length + " gaussian primitives read");
    }
  }

  void readMolecularOrbital() throws Exception {
    int tokenPt = 0;
    r.orbitals = new  Lst<Map<String, Object>>();
    String[] tokens = PT.getTokens("");
    float[] energies = new float[moCount];
    float[][] coefficients = new float[moCount][coefCount];
    for (int i = 0; i < moCount; i++) {
      if (tokenPt == tokens.length) {
        tokens = PT.getTokens(readLine());
        tokenPt = 0;
      }
      energies[i] = parseFloat(tokens[tokenPt++]);
    }
    for (int i = 0; i < moCount; i++) {
      for (int j = 0; j < coefCount; j++) {
        if (tokenPt == tokens.length) {
          tokens = PT.getTokens(readLine());
          tokenPt = 0;
        }
        coefficients[i][j] = parseFloat(tokens[tokenPt++]);
      }
    }
    for (int i = 0; i < moCount; i++) {
      Map<String, Object> mo = new Hashtable<String, Object>();
      mo.put("energy", Float.valueOf(energies[i]));
      //mo.put("occupancy", Float.valueOf(-1));
      mo.put("coefficients", coefficients[i]);
      r.setMO(mo);
    }
    if (Logger.debugging) {
      Logger.debug(r.orbitals.size() + " molecular orbitals read");
    }
    r.moData.put("mos", r.orbitals);
  }

  void readProperties() throws Exception {
    if (Logger.debugging)
      Logger.debug("Reading PROPARC properties records...");
    while (readLine() != null
        && !line.startsWith("ENDPROPARC") && !line.startsWith("END Directory Entry ")) {
      if (line.startsWith("PROP"))
        readProperty();
      else if (line.startsWith("DIPOLE"))
        readDipole();
      else if (line.startsWith("VIBFREQ"))
        readVibFreqs();
    }
    setVibrationsFromProperties();
  }

  void readDipole() throws Exception {
    //fall-back if no other dipole record
    setDipole(PT.getTokens(readLine()));
  }

  private void setDipole(String[] tokens) {
    if (tokens.length != 3)
      return;
    V3 dipole = V3.new3(parseFloat(tokens[0]),
        parseFloat(tokens[1]), parseFloat(tokens[2]));
    r.asc.setCurrentModelInfo("dipole", dipole);
  }

  private void readProperty() throws Exception {
    String tokens[] = PT.getTokens(line);
    if (tokens.length == 0)
      return;
    //System.out.println("reading property line:" + line);
    boolean isString = (tokens[1].startsWith("STRING"));
    String keyName = tokens[2];
    boolean isDipole = (keyName.equals("DIPOLE_VEC"));
    Object value = new Object();
    Lst<Object> vector = new  Lst<Object>();
    if (tokens[3].equals("=")) {
      if (isString) {
        value = getQuotedString(tokens[4].substring(0, 1));
      } else {
        value = Float.valueOf(parseFloat(tokens[4]));
      }
    } else if (tokens[tokens.length - 1].equals("BEGIN")) {
      int nValues = parseInt(tokens[tokens.length - 2]);
      if (nValues == 0)
        nValues = 1;
      boolean isArray = (tokens.length == 6);
      Lst<Float> atomInfo = new  Lst<Float>();
      int ipt = 0;
      while (readLine() != null
          && !line.substring(0, 3).equals("END")) {
        if (isString) {
          value = getQuotedString("\"");
          vector.addLast(value);
        } else {
          String tokens2[] = PT.getTokens(line);
          if (isDipole)
            setDipole(tokens2);
          for (int i = 0; i < tokens2.length; i++, ipt++) {
            if (isArray) {
              atomInfo.addLast(Float.valueOf(parseFloat(tokens2[i])));
              if ((ipt + 1) % nValues == 0) {
                vector.addLast(atomInfo);
                atomInfo = new  Lst<Float>();
              }
            } else {
              value = Float.valueOf(parseFloat(tokens2[i]));
              vector.addLast(value);
            }
          }
        }
      }
      value = null;
    } else {
      if (Logger.debugging) {
        Logger.debug(" Skipping property line " + line);
      }
    }
    //Logger.debug(keyName + " = " + value + " ; " + vector);
    if (value != null)
      r.asc.setInfo(keyName, value);
    if (vector.size() != 0)
      r.asc.setInfo(keyName, vector);
  }

  // Logger.debug("reading property line:" + line);

  void readVibFreqs() throws Exception {
    readLine();
    String label = "";
    int frequencyCount = parseInt(line);
    Lst<Lst<Lst<Float>>> vibrations = new  Lst<Lst<Lst<Float>>>();
    Lst<Map<String, Object>> freqs = new  Lst<Map<String,Object>>();
    if (Logger.debugging) {
      Logger.debug("reading VIBFREQ vibration records: frequencyCount = "
          + frequencyCount);
    }
    boolean[] ignore = new boolean[frequencyCount];
    for (int i = 0; i < frequencyCount; ++i) {
      int ac0 = r.asc.ac;
      ignore[i] = !r.doGetVibration(++r.vibrationNumber);
      if (!ignore[i] && r.desiredVibrationNumber <= 0) {
        r.asc.cloneLastAtomSet();
        addBonds(bondData, ac0);
      }
      readLine();
      Map<String, Object> info = new Hashtable<String, Object>();
      float freq = parseFloat(line);
      info.put("freq", Float.valueOf(freq));
      if (line.length() > 15
          && !(label = line.substring(15, line.length())).equals("???"))
        info.put("label", label);
      freqs.addLast(info);
      if (!ignore[i]) {
        r.asc.setAtomSetFrequency(r.vibrationNumber, null, label, "" + freq, null);
      }
    }
    r.asc.setInfo("VibFreqs", freqs);
    int ac = r.asc.getAtomSetAtomCount(0);
    Lst<Lst<Float>> vib = new  Lst<Lst<Float>>();
    Lst<Float> vibatom = new  Lst<Float>();
    int ifreq = 0;
    int iatom = ac;
    int nValues = 3;
    float[] atomInfo = new float[3];
    while (readLine() != null) {
      String tokens2[] = PT.getTokens(line);
      for (int i = 0; i < tokens2.length; i++) {
        float f = parseFloat(tokens2[i]);
        atomInfo[i % nValues] = f;
        vibatom.addLast(Float.valueOf(f));
        if ((i + 1) % nValues == 0) {
          if (!ignore[ifreq]) {
            r.asc.addVibrationVector(iatom, atomInfo[0], atomInfo[1], atomInfo[2]);
            vib.addLast(vibatom);
            vibatom = new  Lst<Float>();
          }
          ++iatom;
        }
      }
      if (iatom % ac == 0) {
        if (!ignore[ifreq]) {
          vibrations.addLast(vib);
        }
        vib = new  Lst<Lst<Float>>();
        if (++ifreq == frequencyCount) {
          break; // /loop exit
        }
      }
    }
    r.asc.setInfo("vibration", vibrations);
  }

  @SuppressWarnings("unchecked")
  private void setVibrationsFromProperties() throws Exception {
    Lst<Lst<Float>> freq_modes = (Lst<Lst<Float>>) r.asc.atomSetInfo.get("FREQ_MODES");
    if (freq_modes == null) {
      return;
    }
    Lst<String> freq_lab = (Lst<String>) r.asc.atomSetInfo.get("FREQ_LAB");
    Lst<Float> freq_val = (Lst<Float>) r.asc.atomSetInfo.get("FREQ_VAL");
    int frequencyCount = freq_val.size();
    Lst<Lst<Lst<Float>>> vibrations = new  Lst<Lst<Lst<Float>>>();
    Lst<Map<String, Object>> freqs = new  Lst<Map<String,Object>>();
    if (Logger.debugging) {
      Logger.debug(
          "reading PROP VALUE:VIB FREQ_MODE vibration records: frequencyCount = " + frequencyCount);
    }
    Float v;
    for (int i = 0; i < frequencyCount; ++i) {
      int ac0 = r.asc.ac;
      r.asc.cloneLastAtomSet();
      addBonds(bondData, ac0);
      Map<String, Object> info = new Hashtable<String, Object>();
      info.put("freq", (v = freq_val.get(i)));
      float freq = v.floatValue();
      String label = freq_lab.get(i);
      if (!label.equals("???")) {
        info.put("label", label);
      }
      freqs.addLast(info);
      r.asc.setAtomSetName(label + " " + freq + " cm^-1");
      r.asc.setAtomSetModelProperty("Frequency", freq + " cm^-1");
      r.asc.setAtomSetModelProperty(SmarterJmolAdapter.PATH_KEY, "Frequencies");
    }
    r.asc.setInfo("VibFreqs", freqs);
    int ac = r.asc.getAtomSetAtomCount(0);
    int iatom = ac; // add vibrations starting at second atomset
    for (int i = 0; i < frequencyCount; i++) {
      if (!r.doGetVibration(i + 1))
        continue;
      int ipt = 0;
      Lst<Lst<Float>> vib = new  Lst<Lst<Float>>();
      Lst<Float> mode = freq_modes.get(i);
      for (int ia = 0; ia < ac; ia++, iatom++) {
        Lst<Float> vibatom = new  Lst<Float>();
        float vx = (v = mode.get(ipt++)).floatValue();
        vibatom.addLast(v);
        float vy = (v = mode.get(ipt++)).floatValue();
        vibatom.addLast(v);
        float vz = (v = mode.get(ipt++)).floatValue();
        vibatom.addLast(v);
        r.asc.addVibrationVector(iatom, vx, vy, vz);
        vib.addLast(vibatom);
      }
      vibrations.addLast(vib);
    }
    r.asc.setInfo("vibration", vibrations);
  }

  private String getQuotedString(String strQuote) {
    int i = line.indexOf(strQuote);
    int j = line.lastIndexOf(strQuote);
    return (j == i ? "" : line.substring(i + 1, j));
  }
  
  private int parseInt(String info) {
    return r.parseIntStr(info);
  }

  private float parseFloat(String info) {
    return r.parseFloatStr(info);
  }

  private String line;
 
  private String readLine() throws Exception {
    return (line = r.rd());
  }
}
