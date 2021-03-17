/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-03-15 07:52:29 -0600 (Wed, 15 Mar 2006) $
 * $Revision: 4614 $
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

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;

import java.util.Hashtable;

import java.util.Map;

import org.jmol.quantum.QS;
import org.jmol.util.Logger;

/**
 * A molecular orbital reader for WebMO files.
 *<p>
 * <a href='http://www.webmo.net/demo/'>
 * http://www.webmo.net/demo/ 
 * </a>
 * 
 * right now WebMO files don't allow for multiple MOS, but we will assume here that that may change.
 *<p>
 */
public class WebMOReader extends MopacSlaterReader {

  @Override
  protected boolean checkLine() throws Exception {
    if (line.equals("[HEADER]")) {
      readHeader();
      return true;
    }

    if (line.equals("[ATOMS]")) {
      readAtoms();
      return false;
    }

    if (line.equals("[BONDS]")) {
      readBonds();
      return false;
    }

    if (!doReadMolecularOrbitals)
      return true;
    
    if (line.equals("[AO_ORDER]")) {
      readAtomicOrbitalOrder();
      return false;
    }

    if (line.equals("[GTO]")) {
      readGaussianBasis();
      return false;
    }

    if (line.equals("[STO]")) {
      readSlaterBasis();
      return false;
    }

    if (line.indexOf("[MO") == 0) {
      if (!doGetModel(++modelNumber, null))
        return checkLastModel();
      readMolecularOrbital();
      return false;
    }
    return true;
  }

  @Override
  protected void finalizeSubclassReader() throws Exception {
    finalizeReaderASCR();
    if (nOrbitals > 0)
      setMOs("eV");
    if (debugging)
      Logger.debug(orbitals.size() + " molecular orbitals read");
  }
  
  void readHeader() throws Exception {
    moData.put("isNormalized", Boolean.TRUE);
    while (rd() != null && line.length() > 0) {
      moData.put("calculationType", "?");
      String[] tokens = getTokens();
      tokens[0] = tokens[0].substring(0, 1).toLowerCase()
          + tokens[0].substring(1, tokens[0].length());
      String str = "";
      for (int i = 1; i < tokens.length; i++)
        str += (i == 1 ? "" : " ") + tokens[i].toLowerCase();
      moData.put(tokens[0], str);
    }
  }

  void readAtoms() throws Exception {
    /*
     * 
     * [ATOMS] C 0 0 -1.11419008746451 O 0 0 1.11433559637682
     * 
     * !!!!or!!!!
     * 
     * [ATOMS] 6 0 0 0 6 2.81259696844285 0 0 16 1.40112510589261
     * 3.14400070481769 0 6 4.21654880978248 -0.850781692374614
     * -2.34559506901613
     */

    while (getLine()) {
      String[] tokens = getTokens();
      if (tokens.length == 0)
        continue;
      String sym = tokens[0];
      int atNo = parseIntStr(sym);
      setAtomCoordScaled(null, tokens, 1, ANGSTROMS_PER_BOHR).elementSymbol = (atNo == Integer.MIN_VALUE ? sym
          : getElementSymbol(atNo));
    }
  }

  void readBonds() throws Exception {
    /*
     
     [BONDS]
     1     2     2     
     1     3     1     
     1     4     1     

     */

    while (getLine()) {
      String[] tokens = getTokens();
      if (tokens.length == 0)
        continue;
      int atomIndex1 = parseIntStr(tokens[0]);
      int atomIndex2 = parseIntStr(tokens[1]);
      int order = parseIntStr(tokens[2]);
      asc
          .addBond(new Bond(atomIndex1 - 1, atomIndex2 - 1, order));
    }
  }


  // DS: org.jmol.quantum.MOCalculation expects 
  //   d2z^2-x2-y2, dxz, dyz, dx2-y2, dxy
  
  // DC: org.jmol.quantum.MOCalculation expects 
  //      Dxx Dyy Dzz Dxy Dxz Dyz

  // FS: org.jmol.quantum.MOCalculation expects
  //        as 2z3-3x2z-3y2z
  //               4xz2-x3-xy2
  //                   4yz2-x2y-y3
  //                           x2z-y2z
  //                               xyz
  //                                  x3-3xy2
  //                                     3x2y-y3

  // FC: org.jmol.quantum.MOCalculation expects
  //           xxx yyy zzz xyy xxy xxz xzz yzz yyz xyz


  private static String DS_LIST = "NOT IMPLEMENTED IN THIS READER";

  private static String DC_LIST = "xx    yy    zz    xy    xz    yz";

  private static String FS_LIST = "NOT IMPLEMENTED IN THIS READER";

  private static String FC_LIST = "xxx   yyy   zzz   yyx   xxy   xxz   zzx   zzy   yyz   xyz";

  void readAtomicOrbitalOrder() throws Exception {
    /*
     [AO_ORDER]
     DOrbitals XX YY ZZ XY XZ YZ
     FOrbitals XXX YYY ZZZ XXY XXZ YYX YYZ ZZX ZZY XYZ
     */
    
    while (getLine()) {
      String[] tokens = getTokens();
      if (tokens.length == 0)
        continue;
      String data = line.substring(9).trim().toLowerCase();
      boolean isOK = false;
      switch(tokens.length - 1) {
      case 3:
      case 4:
        isOK = true;
        break;
      case 5:
        isOK = (tokens[0].equals("DOrbitals") && getDFMap("DS", data, QS.DS, DS_LIST, 99));
        break;
      case 6:
        isOK = (tokens[0].equals("DOrbitals") && getDFMap("DC", data, QS.DC, DC_LIST, 2));
        break;
      case 7:
        isOK = (tokens[0].equals("FOrbitals") && getDFMap("FS", data, QS.FS, FS_LIST, 99));
        break;
      case 10:
        isOK = (tokens[0].equals("FOrbitals") && getDFMap("FC", data, QS.FC, FC_LIST, 3));
        break;
      }      
      if (!isOK) {
        Logger.error("atomic orbital order is unrecognized -- skipping reading of MOs due to line: " + line);
        orbitals = null;
      }
    }
  }

  private boolean getLine() throws Exception {
    return (rd() != null && (line.length() == 0 || line.charAt(0) != '['));
  }

  void readGaussianBasis() throws Exception {
    /*
     * standard Gaussian format:
     
     [GTO]
     1
     S 3
     172.2560000 2.0931324849764
     25.9109000 2.93675143488078
     5.5333500 1.80173711536432
     
     1
     SP 2
     3.6649800 -0.747384339731355 1.70917757609178
     0.7705450 0.712661025209793 0.885622064435248

     */

    Lst<int[]> sdata = new  Lst<int[]>();
    Lst<float[]> gdata = new  Lst<float[]>();
    int atomNo = 1;
    int gaussianPtr = 0;

    while (getLine()) {
      String[] tokens = getTokens();
      if (tokens.length == 0)
        continue;
      if (tokens.length != 1) // VERY unlikely event -- might as well note it, though.
        throw new Exception("Error reading GTOs: missing atom index");
      int[] slater = new int[4];
      atomNo = parseIntStr(tokens[0]);
      tokens = PT.getTokens(rd());
      int nGaussians = parseIntStr(tokens[1]);
      slater[0] = atomNo;
      slater[1] = BasisFunctionReader.getQuantumShellTagID(tokens[0]);
      slater[2] = gaussianPtr + 1;
      slater[3] = nGaussians;
      for (int i = 0; i < nGaussians; i++) {
        String[] strData = PT.getTokens(rd());
        int nData = strData.length;
        float[] data = new float[nData];
        for (int d = 0; d < nData; d++) {
          data[d] = parseFloatStr(strData[d]);
        }
        gdata.addLast(data);
        gaussianPtr++;
      }
      sdata.addLast(slater);
    }
    float[][] garray = AU.newFloat2(gaussianPtr);
    for (int i = 0; i < gaussianPtr; i++) {
      garray[i] = gdata.get(i);
    }
    moData.put("shells", sdata);
    moData.put("gaussians", garray);
    if (debugging) {
      Logger.debug(sdata.size() + " slater shells read");
      Logger.debug(garray.length + " gaussian primitives read");
    }
    asc.setCurrentModelInfo("moData", moData);
  }

  void readSlaterBasis() throws Exception {
    /*
     * slater format: [STO] 1 0 0 0 1 1.565085 0.998181645138011 1 1 0 0 0
     * 1.842345 2.59926303779824 1 0 1 0 0 1.842345 2.59926303779824 1 0 0 1 0
     * 1.842345 2.59926303779824
     */
    while (getLine()) {
      String[] tokens = getTokens();
      if (tokens.length < 7)
        continue;
      addSlater(parseIntStr(tokens[0]), parseIntStr(tokens[1]),
          parseIntStr(tokens[2]), parseIntStr(tokens[3]), parseIntStr(tokens[4]),
          parseFloatStr(tokens[5]), parseFloatStr(tokens[6]));
    }
    scaleSlaters = false;
    setSlaters(false);
  }

  void readMolecularOrbital() throws Exception {
    /*
     [MOn]
     -11.517
     2
     1 0.0939313753737777
     2 0.204585583790748
     3 0.111068760356317
     4 -0.020187156204269
     */
    if (orbitals == null) {
      Logger.error("MOLECULAR ORBITALS SKIPPED");
      while(getLine()){
        // skip
      }
      return;
    }
    Map<String, Object> mo = new Hashtable<String, Object>();
    Lst<String> data = new  Lst<String>();
    float energy = parseFloatStr(rd());
    float occupancy = parseFloatStr(rd());
    while (getLine()) {
      String[] tokens = getTokens();
      if (tokens.length == 0) {
        continue;
      }
      data.addLast(tokens[1]);
    }
    float[] coefs = new float[data.size()];
    for (int i = data.size(); --i >= 0;) {
      coefs[i] = parseFloatStr(data.get(i));
    }
    mo.put("energy", Float.valueOf(energy));
    mo.put("occupancy", Float.valueOf(occupancy));
    mo.put("coefficients", coefs);
    orbitals.addLast(mo);
    nOrbitals++;
    if (occupancy > 0)
      moData.put("HOMO", Integer.valueOf(nOrbitals));
  }
}
