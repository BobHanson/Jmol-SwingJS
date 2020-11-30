/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-11 23:56:13 -0500 (Mon, 11 Sep 2006) $
 * $Revision: 5499 $
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

import java.util.Hashtable;
import java.util.Map;

import org.jmol.util.Logger;

import javajs.util.BC;
import javajs.util.PT;
import javajs.util.SB;

/*
 * Spartan SMOL and .spartan compound document reader and .spartan06 zip files
 * 
 */

public class SpartanSmolReader extends SpartanInputReader {

  private boolean iHaveModelStatement;
  private boolean isCompoundDocument;
  private boolean inputOnly;
  private boolean espCharges;
  private boolean natCharges;
  private boolean isInputFirst;
  private boolean iHaveNewDir;

  @Override
  protected void initializeReader() throws Exception {
    isCompoundDocument = (rd()
        .indexOf("Compound Document File Directory") >= 0);
    inputOnly = checkFilterKey("INPUT");
    natCharges = checkFilterKey("NATCHAR");
    espCharges = !natCharges && !checkFilterKey("MULLIKEN"); // changed default in Jmol 12.1.41, 12.0.38

  }

  @Override
  protected boolean checkLine() throws Exception {
    // BH Note: I will be the first to say that this coding is way too complicated. 
    // The original design accommodates too many variations. There are
    // the Spartan for Windows compound document, the MacSpartan directory, and
    // the Jmol translation of each of those. 

    // JMOL_MODEL is a bogus type added by Jmol as a marker only, 
    // added only to the MacSpartan directory format.
    // That record is placed BEFORE the Input record, while 
    // for the compound document, Input comes before Molecule.

    // MacSpartan:
    //       c:/temp/cyclohexane_movie.spardir/M0001/#JMOL_MODEL M0001
    //       BEGIN Directory Entry c:/temp/cyclohexane_movie.spardir/M0001/input
    //       BEGIN Directory Entry c:/temp/cyclohexane_movie.spardir/M0001/Molecule:asBinaryString
    // Spartan for Windows; Spartan 14:
    //       BEGIN Directory Entry Input
    //       BEGIN Directory Entry Molecule
    // 
    int pt = 3;
    boolean isNewDir = (isCompoundDocument
        && line.startsWith("NEW Directory M")
        && !line.startsWith("NEW Directory Molecules"));
    if (isNewDir)
      iHaveNewDir = true;
    boolean isMolecule = (!iHaveNewDir && !isNewDir && isCompoundDocument && line
        .equals("BEGIN Directory Entry Molecule"));
    boolean isMacDir = (!isCompoundDocument && (pt = line.indexOf("#JMOL_MODEL")) >= 0); 
    if (isNewDir || isMolecule || isMacDir) {
      if (modelNumber > 0 && !isInputFirst)
        applySymmetryAndSetTrajectory();
      iHaveModelStatement = true;
      int modelNo = (isMolecule ? 0 : parseIntAt(line, pt + 12));
      modelNumber = (bsModels == null && modelNo != Integer.MIN_VALUE && modelNo != 0 ? modelNo
          : modelNumber + 1);
      bondData = "";
      if (!doGetModel(modelNumber, null)) {
        if (isInputFirst) {
          asc.removeCurrentAtomSet();
          discardLinesUntilContains("BEGIN Directory Entry Input");
        } else if (isNewDir) {
          discardLinesUntilContains("NEW Directory M");
        } else if (isMolecule) {
          discardLinesUntilContains("BEGIN Directory Entry M");
        } else {
          discardLinesUntilContains("#JMOL_MODEL");
        }
        checkLastModel();
        return false;
      }
      if (!isInputFirst) {
        makeNewAtomSet();
      }
      moData = new Hashtable<String, Object>();
      moData.put("isNormalized", Boolean.TRUE);
      boolean isOK = false;
      if (modelNo == Integer.MIN_VALUE || titles == null) {
        modelNo = modelNumber;
        title = "Model " + modelNo;
      } else {
        isOK = true;
        title = titles.get("Title" + modelNo);
        title = "Profile " + modelNo + (title == null ? "" : ": " + title);
      }
      if (constraints == null  && (isOK || !isInputFirst))
        asc.setAtomSetName(title);
      setModelPDB(false);
      asc.setCurrentAtomSetNumber(modelNo);
      if (isMolecule)
        readMyTransform();
      return true;
    }
    if (iHaveModelStatement && !doProcessLines)
      return true;
    if ((line.indexOf("BEGIN") == 0)) {
      String lcline = line.toLowerCase();
      if (lcline.endsWith("input")) {
        if (!iHaveModelStatement)
          isInputFirst = true;
        if (isInputFirst) {
          makeNewAtomSet();
        }
        bondData = "";
        title = readInputRecords();
        if (asc.errorMessage != null) {
          continuing = false;
          return false;
        }
        if (title != null && constraints == null)
          asc.setAtomSetName(title);
        setCharges();
        if (inputOnly) {
          continuing = false;
          return false;
        }
      } else if (lcline.endsWith("_output")) {
        return true;
      } else if (lcline.endsWith("output")) {
        readOutput();
        return false;
      } else if (lcline.endsWith("molecule")
          || lcline.endsWith("molecule:asbinarystring")) {
        readMyTransform();
        return false;
      } else if (lcline.endsWith("proparc")
          || lcline.endsWith("propertyarchive")) {
        readProperties();
        return false;
      } else if (lcline.endsWith("molstate")) {
          readTransform();
          return false;
      } else if (lcline.endsWith("archive")) {
        asc.setAtomSetName(readArchive());
        return false;
      }
      return true;
    } 
    if (line.indexOf("5D shell") >= 0)
      moData.put("calculationType", calculationType = line);
    return true;
  }

  private void makeNewAtomSet() {
    // Spartan 16 files may have an empty first model
    if (asc.ac == 0)
      asc.removeCurrentAtomSet();
    asc.newAtomSet();
  }

  @Override
  protected void finalizeSubclassReader() throws Exception {
    finalizeReaderASCR();
    // info out of order -- still a chance, at least for first model
    if (asc.ac > 0 && spartanArchive != null && asc.bondCount == 0
        && bondData != null)
      spartanArchive.addBonds(bondData, 0);
    if (moData != null) {
      Float n = (Float) asc.atomSetInfo.get("HOMO_N");
      if (n != null) {
        int i = n.intValue();
        moData.put("HOMO", Integer.valueOf(i));
        // TODO: This would take some work -- SOMO, degenerate HOMO etc.
        //for (int j = orbitals.size(); --j >= 0;)
          //orbitals.get(j).put("occupancy", Float.valueOf(j > i ? 0 : 2));

      }
    }
  }

  private void readMyTransform() throws Exception {
    float[] mat;
    String binaryCodes = rd();
    // last 16x4 bytes constitutes the 4x4 matrix, using doubles
    String[] tokens = PT.getTokens(binaryCodes.trim());
    if (tokens.length < 16)
      return;
    byte[] bytes = new byte[tokens.length];
    for (int i = 0; i < tokens.length; i++)
      bytes[i] = (byte) PT.parseIntRadix(tokens[i], 16);
    mat = new float[16];
    for (int i = 16, j = bytes.length - 8; --i >= 0; j -= 8)
      mat[i] = BC.bytesToDoubleToFloat(bytes, j, false);
    setTransform(mat[0], mat[1], mat[2], mat[4], mat[5], mat[6], mat[8],
        mat[9], mat[10]);
  }

  private final static String endCheck = "END Directory Entry ";
  private String title;

  SpartanArchive spartanArchive;

  Map<String, String> titles;

  private void readOutput() throws Exception {
    titles = new Hashtable<String, String>();
    SB header = new SB();
    int pt;
    while (rd() != null && !line.startsWith("END ") && !line.startsWith("ENDOUTPUT")) {
      header.append(line).append("\n");
      if ((pt = line.indexOf(")")) > 0)
        titles.put("Title" + parseIntRange(line, 0, pt), (line
            .substring(pt + 1).trim()));
    }
    asc.setInfo("fileHeader", header
        .toString());
  }

  private String readArchive() throws Exception {
    spartanArchive = new SpartanArchive(this, bondData, endCheck, 0);
    String modelName = readArchiveHeader();
    if (modelName != null)
      modelAtomCount = spartanArchive.readArchive(line, false, asc.ac, false);
    return (constraints == null ? modelName : null);
  }

  private boolean haveCharges;

  private void setCharges() {
    if (haveCharges || asc.ac == 0)
      return;
    haveCharges = (espCharges
        && asc.setAtomSetCollectionPartialCharges("ESPCHARGES")
        || natCharges && asc.setAtomSetCollectionPartialCharges("NATCHARGES")
        || asc.setAtomSetCollectionPartialCharges("MULCHARGES")
        || asc.setAtomSetCollectionPartialCharges("Q1_CHARGES") || asc
        .setAtomSetCollectionPartialCharges("ESPCHARGES"));
  }

  private void readProperties() throws Exception {
    if (modelAtomCount == 0) {
      rd();
      return;
    }
    if (spartanArchive == null)
      spartanArchive = new SpartanArchive(this, bondData, endCheck,
          modelAtomCount);
    spartanArchive.readProperties();
    rd();
    setCharges();
  }

  private String readArchiveHeader() throws Exception {
    String modelInfo = rd();
    if (debugging)
      Logger.debug(modelInfo);
    if (modelInfo.indexOf("Error:") == 0) // no archive here
      return null;
    asc.setCollectionName(modelInfo);
    asc.setAtomSetName(modelInfo);
    String modelName = rd();
    if (debugging)
      Logger.debug(modelName);
    //    5  17  11  18   0   1  17   0 RHF      3-21G(d)           NOOPT FREQ
    rd();
    return modelName;
  }

  public void setEnergy(float value) {    
    asc.setAtomSetName(constraints + (constraints.length() == 0 ? "" : " ") + "Energy=" + value + " KJ");
  }

}
