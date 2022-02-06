/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-15 17:34:01 -0500 (Sun, 15 Oct 2006) $
 * $Revision: 5957 $
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

package org.jmol.adapter.readers.pdb;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollection;
import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Structure;
import org.jmol.api.JmolAdapter;
import org.jmol.api.SymmetryInterface;
import org.jmol.c.STR;
import org.jmol.util.Escape;
import org.jmol.util.Logger;

import javajs.util.Lst;
import javajs.util.M4;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.SB;


/**
 * PDB file reader.
 *
 *<p>
 * <a href='http://www.rcsb.org'>
 * http://www.rcsb.org
 * </a>
 *
 * @author Miguel, Egon, and Bob (hansonr@stolaf.edu)
 * 
 * pqr and gromacs pdb_wide_format added by Bob 
 * see http://repo.or.cz/w/gromacs.git/blob/HEAD:/src/gmxlib/pdbio.c line 244
 * see http://repo.or.cz/w/gromacs.git/blob/HEAD:/src/gmxlib/pdbio.c line 323
 * 
 * TLS Motion Determination:
 *  
 *    J Painter & E A Merritt (2006) Acta Cryst. D62, 439-450 
 *    http://skuld.bmsc.washington.edu/~tlsmd
 *    
 * symmetry added by Bob Hanson:
 * 
 *  setFractionalCoordinates()
 *  setSpaceGroupName()
 *  setUnitCell()
 *  initializeCartesianToFractional();
 *  setUnitCellItem()
 *  setAtomCoord()
 *  applySymmetryAndSetTrajectory()
 *  
 */

public class PdbReader extends AtomSetCollectionReader {


  // serial number and sequence number extensions added Jan 2012 by BH, Jmol 13.1.12
  // Schroedinger HEX solution: 
  // https://www.schrodinger.com/AcrobatFile.php?type=supportdocs&type2=&ident=530
  // allows for about 1,000,000 atoms
  // assumes sequential numbering with no surprises
  // incompatible with CONECT
  // because there can be two atoms with atom number "10000", one base 10 and one base 16 
  
  // hybrid-36 solution:
  // cci.lbl.gov/cctbx_sources/iotbx/pdb/hybrid_36.py
  // allows for about 87,000,000 atoms
  // no problem with sequential numbering or CONECT
  
  private final static int MODE_PDB = 0;
  private final static int MODE_HEX = 1;
  private final static int MODE_HYBRID36 = 2;

  private int serMode = MODE_PDB;
  private int seqMode = MODE_PDB;
  
  private int serial;

  
  private int lineLength;

  private SB pdbHeader;

  private boolean applySymmetry;
  private boolean getTlsGroups;
  
  private boolean isMultiModel;  // MODEL ...
  private boolean haveMappedSerials;
  private boolean isConnectStateBug;
  private boolean isLegacyModelType;

  protected boolean gromacsWideFormat;
  
  private final Map<String, Map<String, Boolean>> htFormul = new Hashtable<String, Map<String, Boolean>>();
  private Map<String, String> htHetero;
  private Map<String, Map<String, Object>> htSites;
  private Map<String, Boolean> htElementsInCurrentGroup;
  private Map<String, Map<String, String>> htMolIds;
  
  private  Lst<Map<String, String>> vCompnds;
  private  Lst<Map<String, Object>> vBiomolecules;
  private  Lst<Map<String, Object>> vTlsModels;
  private SB sbTlsErrors;

  protected int[] biomtChainAtomCounts;  
  
  private SB sbIgnored, sbSelected, sbConect, sb;

  private int ac;
  private int maxSerial;
  private int nUNK;
  private int nRes;
  
  private Map<String, String> currentCompnd;
  private String currentGroup3;
  private String currentKey;
  private int currentResno = Integer.MIN_VALUE;
  private int configurationPtr = Integer.MIN_VALUE;
  private boolean resetKey = true;
  private String compnd = null;
  private int conformationIndex;
  protected int fileAtomIndex;
  private char lastAltLoc = '\0';
  private int lastGroup = Integer.MIN_VALUE;
  private char lastInsertion = '\0';
  private int lastSourceSerial = Integer.MIN_VALUE;
  private int lastTargetSerial = Integer.MIN_VALUE;
  private int tlsGroupID;
  private int atomTypePt0;
  private int atomTypeLen;
  private boolean isCourseGrained;
  private boolean isbiomol;

  final private static String lineOptions = 
   "ATOM    " + //0
   "HETATM  " + //1
   "MODEL   " + //2
   "CONECT  " + //3
   "HELIX   " + //4,5,6
   "SHEET   " +
   "TURN    " +
   "HET     " + //7
   "HETNAM  " + //8
   "ANISOU  " + //9
   "SITE    " + //10
   "CRYST1  " + //11
   "SCALE1  " + //12,13,14
   "SCALE2  " +
   "SCALE3  " +
   "EXPDTA  " + //15
   "FORMUL  " + //16
   "REMARK  " + //17
   "HEADER  " + //18
   "COMPND  " + //19
   "SOURCE  " + //20
   "TITLE   " + //21
   "SEQADV  ";  //22


 @SuppressWarnings("unchecked")
@Override
 protected void initializeReader() throws Exception {
   allowPDBFilter = true;
   pdbHeader = (getHeader ? new SB() : null);
   applySymmetry = !checkFilterKey("NOSYMMETRY");
   if (isDSSP1)
     asc.setInfo("isDSSP1",Boolean.TRUE);      
   getTlsGroups = checkFilterKey("TLS");
   if (checkFilterKey("ASSEMBLY")) // CIF syntax
     filter = PT.rep(filter, "ASSEMBLY", "BIOMOLECULE");
   isbiomol = checkFilterKey("BIOMOLECULE");
   if (isbiomol)
     filter = filter.replace(':', ' '); // no chain choices if biomolecule
   boolean byChain = isbiomol && checkFilterKey("BYCHAIN");
   boolean bySymop = isbiomol && checkFilterKey("BYSYMOP");
   isCourseGrained = byChain || bySymop;
   if (!isCourseGrained)
     setIsPDB();
   isConcatenated |= filePath.endsWith(".dssr");
   if (htParams.containsKey("vTlsModels")) {
     // from   load files "tls.out" "xxxx.pdb"
     vTlsModels = ( Lst<Map<String, Object>>) htParams.remove("vTlsModels");
   }
   String s = getFilter("TYPE ");
   if (s != null) {
     // first column, nColumns;
     String[] tokens = PT.getTokens(s.replace(',', ' '));
     atomTypePt0 = Integer.parseInt(tokens[0]) - 1;
     int pt = tokens[1].indexOf("=");
     if (pt >= 0) {
       setFilterAtomTypeStr(tokens[1].substring(pt + 1).toUpperCase());
     } else {
       pt = tokens[1].length();
     }
     atomTypeLen = Integer.parseInt(tokens[1].substring(0, pt));
   }
   String conf = getFilter("CONF ");
   if (conf != null) {
     configurationPtr = parseIntStr(conf);
     sbIgnored = new SB();
     sbSelected = new SB();
   }
   isLegacyModelType = (stateScriptVersionInt < 120000);
   isConnectStateBug = (stateScriptVersionInt >= 120151 && stateScriptVersionInt <= 120220
         || stateScriptVersionInt >= 120300 && stateScriptVersionInt <= 120320);
 }

  @Override
  protected boolean checkLine() throws Exception {
    int ptOption = ((lineLength = line.length()) < 6 ? -1 : lineOptions
        .indexOf(line.substring(0, 6))) >> 3;
    boolean isAtom = (ptOption == 0 || ptOption == 1);
    boolean isModel = (ptOption == 2);
    serial = (isAtom ? getSerial(6, 11) : 0);
    boolean forceNewModel = ((isTrajectory || isSequential) && !isMultiModel
        && isAtom && serial == 1);
    if (getHeader) {
      if (isAtom || isModel)
        getHeader = false;
      else
        readHeader(false);
    }
    if (isModel || forceNewModel) {
      isMultiModel = isModel;
      getHeader = false;
      // PDB is different -- targets actual model number
      int modelNo = (forceNewModel ? modelNumber + 1 : getModelNumber());
      String modelName = getModelName();
      modelNumber = (useFileModelNumbers ? modelNo : modelNumber + 1);
      if (!doGetModel(modelNumber, null)) {
        handleTlsMissingModels();
        boolean isOK = checkLastModel();
        if (!isOK && isConcatenated)
          isOK = continuing = true;
        return isOK;
      }
      if (!isCourseGrained)
        connectAll(maxSerial, isConnectStateBug);
      if (ac > 0)
        applySymmetryAndSetTrajectory();
      // supposedly MODEL is only for NMR
      model(modelNo, modelName);
      
      if (isLegacyModelType || !isAtom) // Jmol 12.0.RC24 fixed this bug, but for earlier scripts we need to unfix it.
        return true;
    }
    /*
     * OK, the PDB file format is messed up here, because the above commands are
     * all OUTSIDE of the Model framework. Of course, different models might
     * have different secondary structures, but it is not clear that PDB
     * actually supports this. So you can't concatinate PDB files the way you
     * can CIF files. --Bob Hanson 8/30/06
     */
    if (isMultiModel && !doProcessLines) {
      return true;
    }
    if (isAtom) {
      getHeader = false;
      atom();
      return true;
    }
    switch (ptOption) {
    case 3:
      conect();
      return true;
    case 4:
    case 5:
    case 6:
      // HELIX, SHEET, TURN
      if (!ignoreStructure)
        structure();
      return true;
    case 7:
      het();
      return true;
    case 8:
      hetnam();
      return true;
    case 9:
      anisou();
      return true;
    case 10:
      site();
      return true;
    case 11:
      cryst1();
      return true;
    case 12:
    case 13:
    case 14:
      // if (line.startsWith("SCALE1")) {
      // if (line.startsWith("SCALE2")) {
      // if (line.startsWith("SCALE3")) {
      scale(ptOption - 11);
      return true;
    case 15:
      expdta();
      return true;
    case 16:
      formul();
      return true;
    case 17:
      if (line.startsWith("REMARK 285")) 
        return remark285();
      if (line.startsWith("REMARK 350"))
        return remark350();
      if (line.startsWith("REMARK 290"))
        return remark290();
      if (line.contains("This file does not adhere to the PDB standard")) {
        gromacsWideFormat = true;
      }
      if (getTlsGroups) {
        if (line.indexOf("TLS DETAILS") > 0)
          return remarkTls();
      }
      checkRemark();
      return true;
    case 18:
      header();
      return true;
    case 19:
    case 20:
      compnd(ptOption == 20);
      return true;
    case 21:
      title();
      return true;
    case 22:
      seqAdv();
      return true;
    }
    return true;
  }

  protected void checkRemark() {
    checkCurrentLineForScript();
  }


//SEQADV 1EHZ 2MG A   10  GB   M10263      G    10 TRNA                           
//SEQADV 1EHZ H2U A   16  GB   M10263      U    16 TRNA                           
//SEQADV 1EHZ H2U A   17  GB   M10263      U    17 TRNA                           
//SEQADV 1EHZ M2G A   26  GB   M10263      G    26 TRNA                           
//SEQADV 1EHZ OMC A   32  GB   M10263      C    32 TRNA                           
//SEQADV 1EHZ OMG A   34  GB   M10263      G    34 TRNA                           
//SEQADV 1EHZ YYG A   37  GB   M10263      G    37 TRNA                           
//SEQADV 1EHZ PSU A   39  GB   M10263      U    39 TRNA                           
//SEQADV 1EHZ 5MC A   40  GB   M10263      C    40 TRNA                           
//SEQADV 1EHZ 7MG A   46  GB   M10263      G    46 TRNA                           
//SEQADV 1EHZ 5MC A   49  GB   M10263      C    49 TRNA                           
//SEQADV 1EHZ 5MU A   54  GB   M10263      U    54 TRNA                           
//SEQADV 1EHZ PSU A   55  GB   M10263      U    55 TRNA                           
//SEQADV 1EHZ 1MA A   58  GB   M10263      A    58 TRNA                           
//SEQADV 1BLU GLU      7  SWS  P00208    GLN     7 CONFLICT                       

  private void seqAdv() {
    
    // Note: this will be overridden by DSSR if present
    
    String g1 = line.substring(39, 42).trim().toLowerCase();
    if (g1.length() != 1)
      return;
    if (htGroup1 == null)
      asc.setInfo("htGroup1", htGroup1 = new Hashtable<String, String>());
    String g3 = line.substring(12, 15).trim();
    htGroup1.put(g3, g1);
  }

  Map<String, String> htGroup1;
  private int maxLength = 80;
  private String pdbID;
  
  private String readHeader(boolean getLine) throws Exception {
    if (getLine) {
      rd();
      if (!getHeader)
        return line;
    }
    pdbHeader.append(line).appendC('\n');
    return line;
  }

  @Override
  protected void finalizeSubclassReader() throws Exception {
    finalizeReaderPDB();
  }
 
  protected void finalizeReaderPDB() throws Exception {
    checkNotPDB();
    if (pdbID != null && pdbID.length() > 0) {
      if (!isMultiModel)
        asc.setAtomSetName(pdbID);
      asc.setCurrentModelInfo("pdbID", pdbID);
    }
    
    checkUnitCellParams();
    if (!isCourseGrained)
      connectAll(maxSerial, isConnectStateBug);
    SymmetryInterface symmetry;
    if (vBiomolecules != null && vBiomolecules.size() > 0 && asc.ac > 0) {
      asc.setCurrentModelInfo("biomolecules", vBiomolecules);
      setBiomoleculeAtomCounts();
     if (thisBiomolecule != null && applySymmetry) {
        asc.getXSymmetry().applySymmetryBio(thisBiomolecule, applySymmetryToBonds,
            filter);
        vTlsModels = null; // for now, no TLS groups for biomolecules
        asc.xtalSymmetry = null;
      }
    }
    if (vTlsModels != null) {
      symmetry = (SymmetryInterface) getInterface("org.jmol.symmetry.Symmetry");
      int n = asc.atomSetCount;
      if (n == vTlsModels.size()) {
        for (int i = n; --i >= 0;)
          setTlsGroups(i, i, symmetry);
      } else {
        Logger.info(n + " models but " + vTlsModels.size()
            + " TLS descriptions");
        if (vTlsModels.size() == 1) {
          Logger
              .info(" -- assuming all models have the same TLS description -- check REMARK 3 for details.");
          for (int i = n; --i >= 0;)
            setTlsGroups(0, i, symmetry);
        }
      }
      checkForResidualBFactors(symmetry);
    }
    if (sbTlsErrors != null) {
      asc.setInfo("tlsErrors", sbTlsErrors.toString());
      appendLoadNote(sbTlsErrors.toString());
    }
    doCheckUnitCell &= iHaveUnitCell && doApplySymmetry;
    if (doCheckUnitCell && isbiomol) {
      ignoreFileSpaceGroupName = true;
      sgName = fileSgName;
      fractionalizeCoordinates(true);
      asc.setModelInfoForSet("biosymmetry", null, asc.iSet);
      asc.checkSpecial = false;
    }
    if (latticeCells != null && latticeCells[0] != 0)
      addJmolScript("unitcell;axes on;axes unitcell;");
    finalizeReaderASCR();
    if (vCompnds != null) {
      asc.setInfo("compoundSource", vCompnds);
      for (int i = asc.iSet + 1; --i >= 0;)
        asc.setModelInfoForSet("compoundSource", vCompnds, i);
    }
    if (htSites != null) {
      addSites(htSites);
    }
    if (pdbHeader != null)
      asc.setInfo("fileHeader", pdbHeader.toString());
    if (configurationPtr > 0) {
      Logger.info(sbSelected.toString());
      Logger.info(sbIgnored.toString());
    }
  }

  private void checkUnitCellParams() {
    if (iHaveUnitCell) {
      asc.setCurrentModelInfo("unitCellParams", unitCellParams);
      if (sgName != null)
        asc.setCurrentModelInfo("spaceGroup", sgName);
    }
  }

  private void checkForResidualBFactors(SymmetryInterface symmetry) {
    Atom[] atoms = asc.atoms;
    boolean isResidual = false;
     for (int i = asc.ac; --i >= 0;) {
      float[] anisou = tlsU.get(atoms[i]);
      if (anisou == null)
        continue;
      float resid = anisou[7] - (anisou[0] + anisou[1] + anisou[2])/3f;
      if (resid < 0 || Float.isNaN(resid)) {
        isResidual = true; // can't be total
        break;
      }
     }
     
     Logger.info("TLS analysis suggests Bfactors are " + (isResidual ? "" : "NOT") + " residuals");

     for (Map.Entry<Atom, float[]> entry : tlsU.entrySet()) {
       float[] anisou = entry.getValue();
       float resid = anisou[7];
       if (resid == 0)
         continue;
       if (!isResidual)
         resid -= (anisou[0] + anisou[1] + anisou[2])/3f;         
       anisou[0] += resid;
       anisou[1] += resid;
       anisou[2] += resid;
       entry.getKey().addTensor(symmetry.getTensor(vwr, anisou).setType(null), "TLS-R", false);
       
       // check for equal: 
       
       Logger.info("TLS-U:  " + Escape.eAF(anisou));
       anisou = (entry.getKey().anisoBorU);
       if (anisou != null)
         Logger.info("ANISOU: " + Escape.eAF(anisou));       
     }
     tlsU = null;
  }

  private void header() {
    if (lineLength < 8)
      return;
    appendLoadNote(line.substring(7).trim());
    if (lineLength == 80)
      maxLength = 72; // old style
    pdbID = (lineLength >= 66 ? line.substring(62, 66).trim() : "");
    if (pdbID.length() == 4) {
      asc.setCollectionName(pdbID);
      asc.setInfo("havePDBHeaderName", Boolean.TRUE);
    }
    if (lineLength > 50)
      line = line.substring(0, 50);
    asc.setInfo("CLASSIFICATION", line.substring(7).trim());
  }

  private void title() {
    if (lineLength > 10)
      appendLoadNote(line.substring(10, Math.min(maxLength , line.length())).trim());
  }
  
  private void compnd(boolean isSource) {
    if (!isSource) {
      if (compnd == null)
        compnd = "";
      else
        compnd += " ";
      String s = line;
      if (lineLength > 62)
        s = s.substring(0, 62);
      compnd += s.substring(10).trim();
      asc.setInfo("COMPND", compnd);
    }
    if (vCompnds == null) {
      if (isSource)
        return;
      vCompnds = new  Lst<Map<String,String>>();
      htMolIds = new Hashtable<String, Map<String,String>>();
      currentCompnd = new Hashtable<String, String>();
      currentCompnd.put("select", "(*)");
      currentKey = "MOLECULE";
      htMolIds.put("", currentCompnd);
    }
    if (isSource && resetKey) {
      resetKey = false;
      currentKey = "SOURCE";
      currentCompnd = htMolIds.get("");
    }
    line = line.substring(10, Math.min(lineLength, 72)).trim();
    int pt = line.indexOf(":");
    if (pt < 0 || pt > 0 && line.charAt(pt - 1) == '\\')
      pt = line.length();
    String key = line.substring(0, pt).trim();
    String value = (pt < line.length() ? line.substring(pt + 1).trim() : null);
    if (key.equals("MOL_ID")) {
      if (value == null)
        return;
      if (isSource) {
        currentCompnd = htMolIds.remove(value);
        return;
      }
      currentCompnd = new Hashtable<String, String>();
      vCompnds.addLast(currentCompnd);
      htMolIds.put(value, currentCompnd);
    }
    if (currentCompnd == null)
      return;
    if (value == null) {
      value = currentCompnd.get(currentKey);
      if (value == null)
        value = "";
      value += key;
      if (vCompnds.size() == 0)
        vCompnds.addLast(currentCompnd);
    } else {
      currentKey = key;
    }
    if (value.endsWith(";"))
      value = value.substring(0, value.length() - 1);
    currentCompnd.put(currentKey, value);
    if (currentKey.equals("CHAIN"))
      currentCompnd.put("select", "(:"
          + PT.rep(javajs.util.PT
              .rep(value, ", ", ",:"), " ", "") + ")");
  }

  @SuppressWarnings("unchecked")
  private void setBiomoleculeAtomCounts() {
    for (int i = vBiomolecules.size(); --i >= 0;) {
      Map<String, Object> biomolecule = vBiomolecules.get(i);
      Lst<M4> biomts = (Lst<M4>) biomolecule.get("biomts");
      Lst<String> biomtchains = (Lst<String>) biomolecule.get("chains");
      int nTransforms = biomts.size();
      int nAtoms = 0;
      for (int k = nTransforms; --k >= 0;) {
        String chains = biomtchains.get(k);
        for (int j = chains.length() - 1; --j >= 0;)
          if (chains.charAt(j) == ':')
            nAtoms += biomtChainAtomCounts[0 + chains.charAt(j + 1)];
      }
      biomolecule.put("atomCount", Integer.valueOf(nAtoms));
    }
  }

//  REMARK 350 BIOMOLECULE: 1                                                       
//  REMARK 350 APPLY THE FOLLOWING TO CHAINS: 1, 2, 3, 4, 5, 6,  
//  REMARK 350 A, B, C
//  REMARK 350   BIOMT1   1  1.000000  0.000000  0.000000        0.00000            
//  REMARK 350   BIOMT2   1  0.000000  1.000000  0.000000        0.00000            
//  REMARK 350   BIOMT3   1  0.000000  0.000000  1.000000        0.00000            
//  REMARK 350   BIOMT1   2  0.309017 -0.809017  0.500000        0.00000            
//  REMARK 350   BIOMT2   2  0.809017  0.500000  0.309017        0.00000            
//  REMARK 350   BIOMT3   2 -0.500000  0.309017  0.809017        0.00000
//   
//               
//               or, as fount in http://www.ebi.ac.uk/msd-srv/pqs/pqs-doc/macmol/1k28.mmol
//               
//  REMARK 350 AN OLIGOMER OF TYPE :HEXAMERIC : CAN BE ASSEMBLED BY
//  REMARK 350 APPLYING THE FOLLOWING TO CHAINS:
//  REMARK 350 A, D
//  REMARK 350   BIOMT1   1  1.000000  0.000000  0.000000        0.00000
//  REMARK 350   BIOMT2   1  0.000000  1.000000  0.000000        0.00000
//  REMARK 350   BIOMT3   1  0.000000  0.000000  1.000000        0.00000
//  REMARK 350 IN ADDITION APPLY THE FOLLOWING TO CHAINS:
//  REMARK 350 A, D
//  REMARK 350   BIOMT1   2  0.000000 -1.000000  0.000000        0.00000
//  REMARK 350   BIOMT2   2  1.000000 -1.000000  0.000000        0.00000
//  REMARK 350   BIOMT3   2  0.000000  0.000000  1.000000        0.00000
//  REMARK 350 IN ADDITION APPLY THE FOLLOWING TO CHAINS:
//  REMARK 350 A, D
//  REMARK 350   BIOMT1   3 -1.000000  1.000000  0.000000        0.00000
//  REMARK 350   BIOMT2   3 -1.000000  0.000000  0.000000        0.00000
//  REMARK 350   BIOMT3   3  0.000000  0.000000  1.000000        0.00000


 private boolean remark350() throws Exception {
    Lst<M4> biomts = null;
    Lst<String> biomtchains = null;
    vBiomolecules = new Lst<Map<String, Object>>();
    biomtChainAtomCounts = new int[255];
    String title = "";
    String chainlist = "";
    String id = "";
    boolean needLine = true;
    Map<String, Object> info = null;
    int nBiomt = 0;
    M4 mIdent = M4.newM4(null);
    while (true) {
      if (needLine)
        readHeader(true);
      else
        needLine = true;
      if (line == null || !line.startsWith("REMARK 350"))
        break;
      try {
        if (line.startsWith("REMARK 350 BIOMOLECULE:")) {
          if (nBiomt > 0)
            Logger.info("biomolecule " + id + ": number of transforms: "
                + nBiomt);
          info = new Hashtable<String, Object>();
          id = line.substring(line.indexOf(":") + 1).trim();
          title = line.trim();
          info.put("name", "biomolecule " + id);
          info.put("molecule",
              id.length() == 3 ? id : Integer.valueOf(parseIntStr(id)));
          info.put("title", title);
          info.put("chains", biomtchains = new Lst<String>());
          info.put("biomts", biomts = new Lst<M4>());
          vBiomolecules.addLast(info);
          nBiomt = 0;
          //continue; need to allow for next IF, in case this is a reconstruction
        }
        if (line.indexOf("APPLY THE FOLLOWING TO CHAINS:") >= 0) {
          if (info == null) {
            // need to initialize biomolecule business first and still flag this section
            // see http://www.ebi.ac.uk/msd-srv/pqs/pqs-doc/macmol/1k28.mmol
            needLine = false;
            line = "REMARK 350 BIOMOLECULE: 1  APPLY THE FOLLOWING TO CHAINS:";
            continue;
          }
          String list = line.substring(41).trim();
          appendLoadNote("found biomolecule " + id + ": " + list);
          chainlist = ":" + list.replace(',', ';').replace(' ', ':');
          needLine = false;
          while (readHeader(true) != null && line.indexOf("BIOMT") < 0
              && line.indexOf("350") == 7)
            chainlist += ":" + line.substring(11).trim().replace(',',';').replace(' ', ':');
          chainlist += ";";
          if (checkFilterKey("BIOMOLECULE " + id + ";")
              || checkFilterKey("BIOMOLECULE=" + id + ";")) {
            setFilter(filter + chainlist);
            Logger.info("filter set to \"" + filter + "\"");
            thisBiomolecule = info;
          }
          continue;
        }
//         0         1         2         3         4         5         6         7
//         0123456789012345678901234567890123456789012345678901234567890123456789
//         REMARK 350   BIOMT2   1  0.000000  1.000000  0.000000        0.00000

        if (line.startsWith("REMARK 350   BIOMT1 ")) {
          nBiomt++;
          float[] mat = new float[16];
          for (int i = 0; i < 12;) {
            String[] tokens = getTokens();
            mat[i++] = parseFloatStr(tokens[4]);
            mat[i++] = parseFloatStr(tokens[5]);
            mat[i++] = parseFloatStr(tokens[6]);
            mat[i++] = parseFloatStr(tokens[7]);
            if (i == 4 || i == 8)
              readHeader(true);
          }
          mat[15] = 1;
          M4 m4 = new M4();
          m4.setA(mat);
          if (m4.equals(mIdent)) {
            biomts.add(0, m4);
            biomtchains.add(0, chainlist);
          } else {
            biomts.addLast(m4);
            biomtchains.addLast(chainlist);
          }
          continue;
        }
      } catch (Exception e) {
        // probably just 
        thisBiomolecule = null;
        vBiomolecules = null;
        return false;
      }
    }
    if (nBiomt > 0)
      Logger.info("biomolecule " + id + ": number of transforms: " + nBiomt);
    return false;
  }

 
// REMARK 285           (1QL2)                                                           
// REMARK 285 CRYST1                                                               
// REMARK 285  THE ANALOGUE OF THE CRYSTALLOGRAPHIC SPACE GROUP FOR                
// REMARK 285  HELICAL STRUCTURES IS THE LINE GROUP (A.KLUG, F.H.C.CRICK,          
// REMARK 285  H.W.WYCKOFF, ACTA CRYSTALLOG. V.11, 199, 1958).  THE                
// REMARK 285  LINE GROUP OF PF1 IS S.  THE UNIT CELL DIMENSIONS ARE THE           
// REMARK 285  HELIX PARAMETERS (UNIT TWIST TAU, UNIT HEIGHT P).                   
// REMARK 285                                                                      
// REMARK 285  FOR THE PERTURBED MODEL OF PF1,                                     
// REMARK 285    TAU = 200. DEGREES, P = 8.70 ANGSTROMS.                           
// REMARK 285                                                                      
// REMARK 285  THE INDEXING OF UNITS ALONG THE BASIC HELIX IS ILLUSTRATED          
// REMARK 285  IN REFERENCE 4.  TO GENERATE COORDINATES X(K), Y(K), Z(K)           
// REMARK 285  OF UNIT K FROM THE GIVEN COORDINATES X(0), Y(0), Z(0) OF            
// REMARK 285  UNIT 0 IN A UNIT CELL WITH HELIX PARAMETERS                         
// REMARK 285         (TAU, P) = (66.667, 2.90),                                   
// REMARK 285  APPLY THE MATRIX AND VECTOR:                                        
// REMARK 285                                                                      
// REMARK 285     |    COS(TAU*K)   -SIN(TAU*K)   0 |    |   0         |           
// REMARK 285     |    SIN(TAU*K)   COS(TAU*K)    0 | +  |   0         |           
// REMARK 285     |    0            0             1 |    |   P*K       |           
// REMARK 285                                                                      
// REMARK 285  THE NEIGHBORS IN CONTACT WITH UNIT 0 ARE UNITS                      
// REMARK 285         K = +/-1, +/-5, +/-6, +/-11 AND +/-17.                       
// REMARK 285  THESE SYMMETRY-RELATED COPIES ARE USED TO DETERMINE INTERCHAIN      
// REMARK 285  NON-BONDED CONTACTS DURING THE REFINEMENT.                          
// REMARK 285                                                                      
// REMARK 285  [ THE LOWER-TEMPERATURE FORM OF PF1 HAS HELIX PARAMETERS,           
// REMARK 285     TAU = 65.915 DEGREES,                                            
// REMARK 285      P = 3.05 ANGSTROMS. ]
 
 private boolean remark285() {
   // helical data could be here
   return true; // haven't read anything;
 }

//REMARK 290                                                                      
//REMARK 290 CRYSTALLOGRAPHIC SYMMETRY                                            
//REMARK 290 SYMMETRY OPERATORS FOR SPACE GROUP: P 1 21 1                         
//REMARK 290                                                                      
//REMARK 290      SYMOP   SYMMETRY                                                
//REMARK 290     NNNMMM   OPERATOR                                                
//REMARK 290       1555   X,Y,Z                                                   
//REMARK 290       2555   -X,Y+1/2,-Z                                             
//REMARK 290                                                                      
//REMARK 290     WHERE NNN -> OPERATOR NUMBER                                     
//REMARK 290           MMM -> TRANSLATION VECTOR                                  
//REMARK 290                                                                      
//REMARK 290 CRYSTALLOGRAPHIC SYMMETRY TRANSFORMATIONS                            
//REMARK 290 THE FOLLOWING TRANSFORMATIONS OPERATE ON THE ATOM/HETATM             
//REMARK 290 RECORDS IN THIS ENTRY TO PRODUCE CRYSTALLOGRAPHICALLY                
//REMARK 290 RELATED MOLECULES.                                                   
//REMARK 290   SMTRY1   1  1.000000  0.000000  0.000000        0.00000            
//REMARK 290   SMTRY2   1  0.000000  1.000000  0.000000        0.00000            
//REMARK 290   SMTRY3   1  0.000000  0.000000  1.000000        0.00000            
//REMARK 290   SMTRY1   2 -1.000000  0.000000  0.000000        0.00000            
//REMARK 290   SMTRY2   2  0.000000  1.000000  0.000000        9.32505            
//REMARK 290   SMTRY3   2  0.000000  0.000000 -1.000000        0.00000            
//REMARK 290                                                                      
//REMARK 290 REMARK: NULL                                                         

  private boolean remark290() throws Exception {
    while (readHeader(true) != null && line.startsWith("REMARK 290")) {
      if (line.indexOf("NNNMMM   OPERATOR") >= 0) {
        while (readHeader(true) != null) {
          String[] tokens = getTokens();
          if (tokens.length < 4)
            break;
          if (doApplySymmetry || isbiomol)
            setSymmetryOperator(tokens[3]);
        }
      }
    }
    return false;
  } 
  
  private int getSerial(int i, int j) {
    char c = line.charAt(i);
    boolean isBase10 = (c == ' ' || line.charAt(j - 1) == ' ');
    switch (serMode) {
    default:
    case MODE_PDB:
      if (isBase10)
        return parseIntRange(line, i, j);
      try {
        return serial = Integer.parseInt(line.substring(i, j));
      } catch (Exception e) {
        serMode = (PT.isDigit(c) ? MODE_HEX : MODE_HYBRID36);
        return getSerial(i, j);
      }
    case MODE_HYBRID36:
      // -16696160 = Integer.parseInt("100000", 10) - Integer.parseInt("A0000",36)
      //  26973856 = Integer.parseInt("100000", 10) - Integer.parseInt("A0000",36) 
      //           + (Integer.parseInt("100000",36) - Integer.parseInt("A0000",36))
      return (isBase10 || PT.isDigit(c) ? parseIntRange(line, i, j)
          : PT.parseIntRadix(line.substring(i, j), 36) + (PT.isUpperCase(c) ? -16696160 : 26973856));
    case MODE_HEX:
      if (!isBase10)
        return serial = PT.parseIntRadix(line.substring(i, j), 16);
      // reset from MODEL or new chain
      serMode = MODE_PDB;
      return getSerial(i, j);
    }
  }

  private int getSeqNo(int i, int j) {
    char c = line.charAt(i);
    boolean isBase10 = (c == ' ' || line.charAt(j - 1) == ' ');
    switch (seqMode) {
    default:
    case MODE_PDB:
      if (isBase10)
        return parseIntRange(line, i, j);
      try {
        return Integer.parseInt(line.substring(i, j));
      } catch (Exception e) {
        seqMode = (PT.isDigit(c) ? MODE_HEX : MODE_HYBRID36);
        return getSeqNo(i, j);
      }
    case MODE_HYBRID36:
      // -456560 = Integer.parseInt("10000", 10) - Integer.parseInt("A000",36)
      //  756496 = Integer.parseInt("10000", 10) - Integer.parseInt("A000",36) 
      //         + (Integer.parseInt("10000",36) - Integer.parseInt("A000",36)) 
      return (isBase10 || PT.isDigit(c) ? parseIntRange(line, i, j)
          : PT.parseIntRadix(line.substring(i, j), 36)
              + (PT.isUpperCase(c) ? -456560 : 756496));
    case MODE_HEX:
      if (!isBase10)
        return PT.parseIntRadix(line.substring(i, j), 16);
      // reset from MODEL or new chain
      seqMode = MODE_PDB;
      return getSeqNo(i, j);
    }
  }

// Note that segID (columns 73-76) is not generally read, 
  // but can be read into the atomType atom property 
  // starting in Jmol 13.1.12 using FILTER "type 73,4"
  
  // Gromacs pdb_wide_format:
  //%-6s%5u %-4.4s %3.3s %c%4d%c   %10.5f%10.5f%10.5f%8.4f%8.4f    %2s\n")
  //0         1         2         3         4         5         6         7
  //01234567890123456789012345678901234567890123456789012345678901234567890123456789
  //aaaaaauuuuu ssss sss cnnnnc   xxxxxxxxxxyyyyyyyyyyzzzzzzzzzzccccccccrrrrrrrr
 
  protected Atom processAtom(Atom atom,
        String name, 
        char altID, 
        String group3, 
        int chainID, 
        int seqNo,
        char insCode,
        boolean isHetero,
        String sym
        ) {
    atom.atomName = name;
    if (altID != ' ')
      atom.altLoc = altID;
    atom.group3 = (group3 == null ? "UNK" : group3);
    atom.chainID = chainID;
    if (biomtChainAtomCounts != null)
      biomtChainAtomCounts[chainID % 256]++;
    atom.sequenceNumber = seqNo;
    atom.insertionCode = JmolAdapter.canonizeInsertionCode(insCode);
    atom.isHetero = isHetero;    
    atom.elementSymbol = sym;
    return atom;
  }
  
  protected void processAtom2(Atom atom, int serial, float x, float y, float z, int charge) {
    atom.atomSerial = serial;
    if (serial > maxSerial)
      maxSerial = serial;
    if (atom.group3 == null) {
      if (currentGroup3 != null) {
        currentGroup3 = null;
        currentResno = Integer.MIN_VALUE;
        htElementsInCurrentGroup = null;
      }
    } else if (!atom.group3.equals(currentGroup3)
        || atom.sequenceNumber != currentResno) {
      currentGroup3 = atom.group3;
      currentResno = atom.sequenceNumber;
      htElementsInCurrentGroup = htFormul.get(atom.group3);
      nRes++;
      if (atom.group3.equals("UNK"))
        nUNK++;
    }
    setAtomCoordXYZ(atom, x, y, z);
    atom.formalCharge = charge;
    setAdditionalAtomParameters(atom);
    if (haveMappedSerials)
      asc.addAtomWithMappedSerialNumber(atom);
    else
      asc.addAtom(atom);
    if (ac++ == 0 && !isCourseGrained)
      setModelPDB(true);
    // note that values are +1 in this serial map
    if (atom.isHetero) {
      if (htHetero != null) {
        asc.setCurrentModelInfo("hetNames", htHetero);
        htHetero = null;
      }
    }
  }


  private void atom() {
    boolean isHetero = line.startsWith("HETATM");
    Atom atom = processAtom(new Atom(),
        line.substring(12, 16).trim(), 
        line.charAt(16),
        parseTokenRange(line, 17, 20),
        vwr.getChainID(line.substring(21, 22), true),
        getSeqNo(22, 26),
        line.charAt(26),
        isHetero,
        deduceElementSymbol(isHetero)
    );
    if (atomTypeLen > 0) {
      // becomes atomType
      String s = line.substring(atomTypePt0, atomTypePt0 + atomTypeLen).trim();
      if (s.length() > 0)
      atom.atomName += "\0" + s;
    }
    if (!filterPDBAtom(atom, fileAtomIndex++))
      return;
    int charge = 0;
    float x, y, z;
    if (gromacsWideFormat) {
      x = parseFloatRange(line, 30, 40);
      y = parseFloatRange(line, 40, 50);
      z = parseFloatRange(line, 50, 60);
    } else {
      //calculate the charge from cols 79 & 80 (1-based): 2+, 3-, etc
      if (lineLength >= 80) {
        char chMagnitude = line.charAt(78);
        char chSign = line.charAt(79);
        if (chSign >= '0' && chSign <= '7') {
          char chT = chSign;
          chSign = chMagnitude;
          chMagnitude = chT;
        }
        if ((chSign == '+' || chSign == '-' || chSign == ' ')
            && chMagnitude >= '0' && chMagnitude <= '7') {
          charge = chMagnitude - '0';
          if (chSign == '-')
            charge = -charge;
        }
      }
      x = parseFloatRange(line, 30, 38);
      y = parseFloatRange(line, 38, 46);
      z = parseFloatRange(line, 46, 54);
    }    
    processAtom2(atom, serial, x, y, z, charge);
  }
  
  protected boolean filterPDBAtom(Atom atom, int iAtom) {
    if (!filterAtom(atom, iAtom))
      return false;
    if (configurationPtr > 0) {
      if (atom.sequenceNumber != lastGroup || atom.insertionCode != lastInsertion) {
        conformationIndex = configurationPtr - 1;
        lastGroup = atom.sequenceNumber;
        lastInsertion = atom.insertionCode;
        lastAltLoc = '\0';
      }
      // ignore atoms that have no designation
      if (atom.altLoc != '\0') {
        // count down until we get the desired index into the list
        String msg = " atom [" + atom.group3 + "]"
                           + atom.sequenceNumber 
                           + (atom.insertionCode == '\0' ? "" : "^" + atom.insertionCode)
                           + (atom.chainID == 0 ? "" : ":" + vwr.getChainIDStr(atom.chainID))
                           + "." + atom.atomName
                           + "%" + atom.altLoc + "\n";
        if (conformationIndex >= 0 && atom.altLoc != lastAltLoc) {
          lastAltLoc = atom.altLoc;
          conformationIndex--;
        }
        if (conformationIndex < 0 && atom.altLoc != lastAltLoc) {
          sbIgnored.append("ignoring").append(msg);
          return false;
        }
        sbSelected.append("loading").append(msg);
      }
    }
    return true;
  }

  /**
   * adaptable via subclassing
   * 
   * @param atom
   */
  protected void setAdditionalAtomParameters(Atom atom) {
    float floatOccupancy;    
    if (gromacsWideFormat) {
      floatOccupancy = parseFloatRange(line, 60, 68);
      atom.bfactor = fixRadius(parseFloatRange(line, 68, 76));
    } else {
      /****************************************************************
       * read the occupancy from cols 55-60 (1-based) 
       * --should be in the range 0.00 - 1.00
       ****************************************************************/
    
      floatOccupancy = parseFloatRange(line, 54, 60);

      /****************************************************************
       * read the bfactor from cols 61-66 (1-based)
       ****************************************************************/
        atom.bfactor = parseFloatRange(line, 60, 66);
        
    }
    
    atom.foccupancy = (Float.isNaN(floatOccupancy) ? 1 : floatOccupancy);
    
  }

  /**
   * The problem here stems from the fact that developers have not fully 
   * understood the PDB specifications -- and that those have changed. 
   * The actual rules are as follows (using 1-based numbering:
   * 
   * 1) Chemical symbols may be in columns 77 and 78 for total disambiguity.
   * 2) Only valid chemical symbols should be in columns 13 and 14
   *    These are the first two characters of a four-character field.
   * 3) Four-character atom names for hydrogen necessarily start in 
   *    column 13, so when that is the case, if the four-letter 
   *    name starts with "H" then it is hydrogen regardless of what
   *    letter comes next. For example, "HG3 " is mercury (and should
   *    be in a HETATM record, not an ATOM record, anyway), but "HG33"
   *    is hydrogen, presumably.
   *    
   *    This leave open the ambiguity of a four-letter H name in a 
   *    heteroatom set where the symbol is really H, not Hg or Ha, or Ho or Hf, etc.
   *     
   * 
   * @param isHetero
   * @return           an atom symbol
   */
  protected String deduceElementSymbol(boolean isHetero) {
    if (lineLength >= 78) {
      char ch76 = line.charAt(76);
      char ch77 = line.charAt(77);
      if (ch76 == ' ' && Atom.isValidSym1(ch77))
        return "" + ch77;
      if (Atom.isValidSymNoCase(ch76, ch77))
        return "" + ch76 + ch77;
    }
    char ch12 = line.charAt(12);
    char ch13 = line.charAt(13);
    // PDB atom symbols are supposed to be in these two characters
    // But they could be right-aligned or left-aligned
    if ((htElementsInCurrentGroup == null ||
         htElementsInCurrentGroup.get(line.substring(12, 14)) != null) &&
        Atom.isValidSymNoCase(ch12, ch13))
      return (isHetero || ch12 != 'H' ? "" + ch12 + ch13 : "H");
    // not a known two-letter code
    if (ch12 == 'H') // added check for PQR files "HD22" for example
      return "H";
    // check for " NZ" for example
    if ((htElementsInCurrentGroup == null ||
         htElementsInCurrentGroup.get("" + ch13) != null) &&
        Atom.isValidSym1(ch13))
      return "" + ch13;
    // check for misplaced "O   " for example
    if (ch12 != ' ' && (htElementsInCurrentGroup == null ||
         htElementsInCurrentGroup.get("" + ch12) != null) &&
        Atom.isValidSym1(ch12))
      return "" + ch12;
    // could be GLX or ASX;
    // probably a bad file. But we will make ONE MORE ATTEMPT
    // and read columns 14/15 instead of 12/13. What the heck!
    char ch14 = line.charAt(14);
    if (ch12 == ' ' && ch13 != 'X' && (htElementsInCurrentGroup == null ||
        htElementsInCurrentGroup.get(line.substring(13, 15)) != null) &&
        Atom.isValidSymNoCase(ch13, ch14))
     return  "" + ch13 + ch14;
    return "Xx";
  }
  
  private boolean haveDoubleBonds;

  // ancient provisions:
  // - must check for pre-2000 format with hydrogen bonds
  // - salt bridges are totally ignored
  //7 - 11 source
  //12 - 16 target
  //17 - 21 target
  //22 - 26 target
  //27 - 31 target
  //32 - 36 Hydrogen bond
  //37 - 41 Hydrogen bond
  //42 - 46 Salt bridge
  //47 - 51 Hydrogen bond
  //52 - 56 Hydrogen bond
  //57 - 61 Salt bridge
  //FORMAT (6A1,11I5)
  //Note: Serial numbers are identical to those in cols. 7-11 of the appropriate ATOM/
  //HETATM records, and connectivity entries correspond to these serial numbers. A
  //second CONECT record, with the same serial number in cols. 7-11, may be used
  //if necessary. Either all or none of the covalent connectivity of an atom must be
  //specified, and if hydrogen bonding is specified the covalent connectivity is
  //included also.
  //The occurrence of a negative atom serial number on a CONECT record denotes
  //that a translationally equivalent copy (see TVECT records) of the target atom specified
  //is linked to the origin atom of the record.

  //  0         1         2         3         4         5         6
  //  012345678901234567890123456789012345678901234567890123456789012
  //  CONECT   15   14                                493                     1BNA 635


  private void conect() {
    if (sbConect == null) {
      sbConect = new SB();
      sb = new SB();
    } else {
      sb.setLength(0);
    }
    int sourceSerial = getSerial(6, 11);
    if (sourceSerial < 0)
      return;
    int order = 1;
    int pt1 = line.trim().length();
    if (pt1 > 56) // ancient full line; ignore final salt bridge
      pt1 = line.substring(0, 56).trim().length();
    for (int pt = 11; pt < pt1; pt += 5) {
      switch (pt) {
      case 31:
        order = JmolAdapter.ORDER_HBOND;
        break;
      case 41:
        // ancient salt bridge
        continue;
      }
      int targetSerial = getSerial(pt, pt + 5);
      if (targetSerial < 0) // ancient gap in numbers 
        continue;
      boolean isDoubleBond = (sourceSerial == lastSourceSerial && targetSerial == lastTargetSerial);
      if (isDoubleBond)
        haveDoubleBonds = true;
      lastSourceSerial = sourceSerial;
      lastTargetSerial = targetSerial;
      boolean isSwapped = (targetSerial < sourceSerial);
      int i1;
      if (isSwapped) {
        i1 = targetSerial;
        targetSerial = sourceSerial;
      } else {
        i1 = sourceSerial;
      }
      String st = ";" + i1 + " " + targetSerial + ";";
      if (sbConect.indexOf(st) >= 0 && !isDoubleBond)
        continue;
      // check for previous double
      if (haveDoubleBonds) {
        String st1 = "--" + st;
        if (sbConect.indexOf(st1) >= 0)
          continue;
        sb.append(st1);
      }
      sbConect.append(st);
      addConnection(new int[] { i1, targetSerial, order });
    }
    sbConect.appendSB(sb);
  }

  //  0         1         2         3
  //  0123456789012345678901234567890123456
  //  HELIX    1  H1 ILE      7  LEU     18
  //  HELIX    2  H2 PRO     19  PRO     19
  //  HELIX    3  H3 GLU     23  TYR     29
  //  HELIX    4  H4 THR     30  THR     30
  //  SHEET    1  S1 2 THR     2  CYS     4
  //  SHEET    2  S2 2 CYS    32  ILE    35
  //  SHEET    3  S3 2 THR    39  PRO    41
  //  TURN     1  T1 GLY    42  TYR    44
  //
  //  HELIX     1 H1 ILE A    7  PRO A   19
  //  HELIX     2 H2 GLU A   23  THR A   30
  //  SHEET     1 S1 0 CYS A   3  CYS A   4
  //  SHEET     2 S2 0 CYS A  32  ILE A  35
  //
  //  HELIX  113 113 ASN H  307  ARG H  327  1                                  21    
  //  SHEET    1   A 6 ASP A  77  HIS A  80  0                                        
  //  SHEET    2   A 6 GLU A  47  ILE A  51  1  N  ILE A  48   O  ASP A  77           
  //  SHEET    3   A 6 ARG A  22  ILE A  26  1  N  VAL A  23   O  GLU A  47           
  //
  //
  //TYPE OF HELIX CLASS NUMBER (COLUMNS 39 - 40)
  //--------------------------------------------------------------
  //Right-handed alpha (default) 1
  //Right-handed omega 2
  //Right-handed pi 3
  //Right-handed gamma 4
  //Right-handed 310 5
  //Left-handed alpha 6
  //Left-handed omega 7
  //Left-handed gamma 8
  //27 ribbon/helix 9
  //Polyproline 10
  
  private void structure() {
    STR structureType = STR.NONE;
    STR substructureType = STR.NONE;
    int startChainIDIndex;
    int startIndex;
    int endChainIDIndex;
    int endIndex;
    int strandCount = 0;
    if (line.startsWith("HELIX ")) {
      structureType = STR.HELIX;
      startChainIDIndex = 19;
      startIndex = 21;
      endChainIDIndex = 31;
      endIndex = 33;
      if (line.length() >= 40)
      substructureType = Structure.getHelixType(parseIntRange(line, 38, 40));
    } else if (line.startsWith("SHEET ")) {
      structureType = STR.SHEET;
      startChainIDIndex = 21;
      startIndex = 22;
      endChainIDIndex = 32;
      endIndex = 33;
      strandCount = parseIntRange(line, 14, 16);
    } else if (line.startsWith("TURN  ")) {
      structureType = STR.TURN;
      startChainIDIndex = 19;
      startIndex = 20;
      endChainIDIndex = 30;
      endIndex = 31;
    } else
      return;

    if (lineLength < endIndex + 4)
      return;

    String structureID = line.substring(11, 15).trim();
    int serialID = parseIntRange(line, 7, 10);
    int startChainID = vwr.getChainID(line.substring(startChainIDIndex, startChainIDIndex + 1), true);
    int startSequenceNumber = parseIntRange(line, startIndex, startIndex + 4);
    char startInsertionCode = line.charAt(startIndex + 4);
    int endChainID = vwr.getChainID(line.substring(endChainIDIndex, endChainIDIndex + 1), true);
    int endSequenceNumber = parseIntRange(line, endIndex, endIndex + 4);
    // some files are chopped to remove trailing whitespace
    char endInsertionCode = ' ';
    if (lineLength > endIndex + 4)
      endInsertionCode = line.charAt(endIndex + 4);

    // this should probably call Structure.validateAndAllocate
    // in order to check validity of parameters
    // model number set to -1 here to indicate ALL MODELS
    if (substructureType == STR.NONE)
      substructureType = structureType;
    Structure structure = new Structure(-1, structureType, substructureType,
        structureID, serialID, strandCount, null);
    structure.set(startChainID, startSequenceNumber,
        startInsertionCode, endChainID, endSequenceNumber, endInsertionCode, Integer.MIN_VALUE, Integer.MAX_VALUE);
    asc.addStructure(structure);
  }

  private int getModelNumber() {
    /****************************************************************
     * mth 2004 02 28 note that the pdb spec says: COLUMNS DATA TYPE FIELD
     * DEFINITION
     * ---------------------------------------------------------------------- 1
     * - 6 Record name "MODEL " 11 - 14 Integer serial Model serial number.
     * 
     * but I received a file with the serial number right after the word MODEL
     * :-(
     ****************************************************************/
    int startModelColumn = 6; // should be 10 0-based
    int endModelColumn = 14;
    if (endModelColumn > lineLength)
      endModelColumn = lineLength;
    int iModel = parseIntRange(line, startModelColumn, endModelColumn);
    return (iModel == Integer.MIN_VALUE ? 0 : iModel);
  }

  /**
   * A Jmol add-on -- allows for model name on MODEL line starting in column 15 (0-based)
   * 
   * @return name or null
   */
  private String getModelName() {
    if (lineLength < 16)
      return null;
    if (line.startsWith("ATOM"))
      return "";
    String name = line.substring(15, lineLength).trim();
    return (name.length() == 0 ? null  : name);
  }

  protected void model(int modelNumber, String name) {
    checkNotPDB();
    if (name == null)
      name = pdbID;
    //not cleaer that this shoudl come irset...    
    haveMappedSerials = false;
    sbConect = null;
    asc.newAtomSet();
    asc.setCurrentModelInfo("pdbID", pdbID);
    if (asc.iSet == 0 || isTrajectory)
      asc.setAtomSetName(pdbID);
    asc.setCurrentModelInfo("name", name);
    checkUnitCellParams();
    if (!isCourseGrained)
      setModelPDB(true);
    asc.setCurrentAtomSetNumber(modelNumber);
    if (isCourseGrained)
      asc.setCurrentModelInfo("courseGrained", Boolean.TRUE);
  }

  private void checkNotPDB() {
    boolean isPDB = (!isCourseGrained && (nRes == 0 || nUNK != nRes));
    // This speeds up calculation, because no crosschecking
    // No special-position atoms in mmCIF files, because there will
    // be no center of symmetry, no rotation-inversions, 
    // no atom-centered rotation axes, and no mirror or glide planes. 
    asc.checkSpecial = !isPDB;
    setModelPDB(isPDB);
    nUNK = nRes = 0;
    currentGroup3 = null;
  }

  private float cryst1;
  private String fileSgName;
  private void cryst1() throws Exception {
    float a = cryst1 = getFloat(6, 9);
    if (a == 1)
      a = Float.NaN; // 1 for a means no unit cell
    setUnitCell(a, getFloat(15, 9), getFloat(24, 9), getFloat(33,
        7), getFloat(40, 7), getFloat(47, 7));
    if (isbiomol)
      doConvertToFractional = false;
    if (sgName == null || sgName.equals("unspecified!"))
      setSpaceGroupName(PT.parseTrimmedRange(line, 55, 66));
    fileSgName = sgName;
  }

  private float getFloat(int ich, int cch) throws Exception {
    return parseFloatRange(line, ich, ich+cch);
  }

  private void scale(int n) throws Exception {
    if (unitCellParams == null)
      return; 
    // Could be an EM image
    // this information will only be processed
    // if a lattice is requested.
    
    int pt = n * 4 + 2;
    unitCellParams[0] = cryst1;
    setUnitCellItem(pt++,getFloat(10, 10));
    setUnitCellItem(pt++,getFloat(20, 10));
    setUnitCellItem(pt++,getFloat(30, 10));
    setUnitCellItem(pt++,getFloat(45, 10));
    if (isbiomol)
      doConvertToFractional = false;
  }

  private void expdta() {
    if (line.toUpperCase().indexOf("NMR") >= 0)
      asc.setInfo("isNMRdata", "true");
  }

  private void formul() {
    String groupName = parseTokenRange(line, 12, 15);
    String formula = PT.parseTrimmedRange(line, 19, 70);
    int ichLeftParen = formula.indexOf('(');
    if (ichLeftParen >= 0) {
      int ichRightParen = formula.indexOf(')');
      if (ichRightParen < 0 || ichLeftParen >= ichRightParen ||
          ichLeftParen + 1 == ichRightParen ) // pick up () case in 1SOM.pdb
        return; // invalid formula;
      formula = PT.parseTrimmedRange(formula, ichLeftParen + 1, ichRightParen);
    }
    Map<String, Boolean> htElementsInGroup = htFormul.get(groupName);
    if (htElementsInGroup == null)
      htFormul.put(groupName, htElementsInGroup = new Hashtable<String, Boolean>());
    // now, look for atom names in the formula
    next[0] = 0;
    String elementWithCount;
    while ((elementWithCount = parseTokenNext(formula)) != null) {
      if (elementWithCount.length() < 2)
        continue;
      char chFirst = elementWithCount.charAt(0);
      char chSecond = elementWithCount.charAt(1);
      if (Atom.isValidSymNoCase(chFirst, chSecond))
        htElementsInGroup.put("" + chFirst + chSecond, Boolean.TRUE);
      else if (Atom.isValidSym1(chFirst))
        htElementsInGroup.put("" + chFirst, Boolean.TRUE);
    }
  }
  
  private void het() {
    if (line.length() < 30) {
      return;
    }
    if (htHetero == null) {
      htHetero = new Hashtable<String, String>();
    }
    String groupName = parseTokenRange(line, 7, 10);
    if (htHetero.containsKey(groupName)) {
      return;
    }
    String hetName = PT.parseTrimmedRange(line, 30, 70);
    htHetero.put(groupName, hetName);
  }
  
  private void hetnam() {
    if (htHetero == null) {
      htHetero = new Hashtable<String, String>();
    }
    String groupName = parseTokenRange(line, 11, 14);
    String hetName = PT.parseTrimmedRange(line, 15, 70);
    if (groupName == null) {
      Logger.error("ERROR: HETNAM record does not contain a group name: " + line);
      return;
    }
    String htName = htHetero.get(groupName);
    if (htName != null) {
      hetName = htName + hetName;
    }
    htHetero.put(groupName, hetName);
    appendLoadNote(groupName + " = " + hetName);

    //Logger.debug("hetero: "+groupName+" "+hetName);
  }
  
//  The ANISOU records present the anisotropic temperature factors.
//
//
//  Record Format
//
//  COLUMNS        DATA TYPE       FIELD         DEFINITION                  
//  ----------------------------------------------------------------------
//  1 -  6        Record name     "ANISOU"                                  
//
//  7 - 11        Integer         serial        Atom serial number.         
//
//  13 - 16        Atom            name          Atom name.                  
//
//  17             Character       altLoc        Alternate location indicator.                  
//
//  18 - 20        Residue name    resName       Residue name.               
//
//  22             Character       chainID       Chain identifier.           
//
//  23 - 26        Integer         resSeq        Residue sequence number.    
//
//  27             AChar           iCode         Insertion code.             
//
//  29 - 35        Integer         u[0][0]       U(1,1)                
//
//  36 - 42        Integer         u[1][1]       U(2,2)                
//
//  43 - 49        Integer         u[2][2]       U(3,3)                
//
//  50 - 56        Integer         u[0][1]       U(1,2)                
//
//  57 - 63        Integer         u[0][2]       U(1,3)                
//
//  64 - 70        Integer         u[1][2]       U(2,3)                
//
//  73 - 76        LString(4)      segID         Segment identifier, left-justified. 
//
//  77 - 78        LString(2)      element       Element symbol, right-justified.
//
//  79 - 80        LString(2)      charge        Charge on the atom.       
//
//  Details
//
//  * Columns 7 - 27 and 73 - 80 are identical to the corresponding ATOM/HETATM record.
//
//  * The anisotropic temperature factors (columns 29 - 70) are scaled by a factor of 10**4 (Angstroms**2) and are presented as integers.
//
//  * The anisotropic temperature factors are stored in the same coordinate frame as the atomic coordinate records. 

  private void anisou() {
    float[] data = new float[8];
    data[6] = 1; //U not B
    String serial = line.substring(6, 11).trim();
    if (!haveMappedSerials && asc.ac > 0) {
      for (int i = asc.getAtomSetAtomIndex(asc.iSet); i < asc.ac; i++) {
        int atomSerial = asc.atoms[i].atomSerial;
        if (atomSerial != Integer.MIN_VALUE)
          asc.atomSymbolicMap.put(""+ atomSerial, asc.atoms[i]);
      }
      haveMappedSerials = true;
    }
    Atom atom = asc.getAtomFromName(serial);
    if (atom == null) {
      //normal when filtering
      //System.out.println("ERROR: ANISOU record does not correspond to known atom");
      return;
    }
    for (int i = 28, pt = 0; i < 70; i += 7, pt++)
      data[pt] = parseFloatRange(line, i, i + 7);
    for (int i = 0; i < 6; i++) {
      if (Float.isNaN(data[i])) {
        Logger.error("Bad ANISOU record: " + line);
        return;
      }
      data[i] /= 10000f;
    }
    asc.setAnisoBorU(atom, data, 12); // was 8 12.3.16
    // new type 12 - cartesian already
    // Ortep Type 0: D = 1, C = 2, Cartesian
    // Ortep Type 8: D = 2pi^2, C = 2, a*b*
    // Ortep Type 10: D = 2pi^2, C = 2, Cartesian
  }
  
//
//   * http://www.wwpdb.org/documentation/format23/sect7.html
//   * 
// Record Format
//
//COLUMNS       DATA TYPE         FIELD            DEFINITION
//------------------------------------------------------------------------
// 1 -  6       Record name       "SITE    "
// 8 - 10       Integer           seqNum      Sequence number.
//12 - 14       LString(3)        siteID      Site name.
//16 - 17       Integer           numRes      Number of residues comprising 
//                                            site.
//
//19 - 21       Residue name      resName1    Residue name for first residue
//                                            comprising site.
//23            Character         chainID1    Chain identifier for first residue
//                                            comprising site.
//24 - 27       Integer           seq1        Residue sequence number for first
//                                            residue comprising site.
//28            AChar             iCode1      Insertion code for first residue
//                                            comprising site.
//30 - 32       Residue name      resName2    Residue name for second residue
//...
//41 - 43       Residue name      resName3    Residue name for third residue
//...
//52 - 54       Residue name      resName4    Residue name for fourth residue
 
  
  private void site() {
    if (htSites == null) {
      htSites = new Hashtable<String, Map<String, Object>>();
    }
    //int seqNum = parseInt(line, 7, 10);
    int nResidues = parseIntRange(line, 15, 17);
    String siteID = PT.parseTrimmedRange(line, 11, 14);
    Map<String, Object> htSite = htSites.get(siteID);
    if (htSite == null) {
      htSite = new Hashtable<String, Object>();
      //htSite.put("seqNum", "site_" + seqNum);
      htSite.put("nResidues", Integer.valueOf(nResidues));
      htSite.put("groups", "");
      htSites.put(siteID, htSite);
    }
    String groups = (String)htSite.get("groups");
    for (int i = 0; i < 4; i++) {
      int pt = 18 + i * 11;
      String resName = PT.parseTrimmedRange(line, pt, pt + 3);
      if (resName.length() == 0)
        break;
      String chainID = PT.parseTrimmedRange(line, pt + 4, pt + 5);
      String seq = PT.parseTrimmedRange(line, pt + 5, pt + 9);
      String iCode = PT.parseTrimmedRange(line, pt + 9, pt + 10);
      groups += (groups.length() == 0 ? "" : ",") + "[" + resName + "]" + seq;
      if (iCode.length() > 0)
        groups += "^" + iCode;
      if (chainID.length() > 0)
        groups += ":" + chainID;
      htSite.put("groups", groups);
    }
  }

//  REMARK   3  TLS DETAILS                                                         
//  REMARK   3   NUMBER OF TLS GROUPS  : NULL
//   or 
//  REMARK   3  TLS DETAILS                                                         
//  REMARK   3   NUMBER OF TLS GROUPS  : 20                                         
//  REMARK   3                                                                      
//  REMARK   3   TLS GROUP : 1                                                      
//  REMARK   3    NUMBER OF COMPONENTS GROUP : 1                                    
//  REMARK   3    COMPONENTS        C SSSEQI   TO  C SSSEQI                         
//  REMARK   3    RESIDUE RANGE :   A     2        A     8                          
//  REMARK   3    ORIGIN FOR THE GROUP (A):  17.3300  62.7550  29.2560              
//  REMARK   3    T TENSOR                                                          
//  REMARK   3      T11:   0.0798 T22:   0.0357                                     
//  REMARK   3      T33:   0.0678 T12:   0.0530                                     
//  REMARK   3      T13:  -0.0070 T23:   0.0011                                     
//  REMARK   3    L TENSOR                                                          
//  REMARK   3      L11:  13.1074 L22:   7.9735                                     
//  REMARK   3      L33:   2.5703 L12:  -6.5507                                     
//  REMARK   3      L13:  -1.5297 L23:   4.1172                                     
//  REMARK   3    S TENSOR                                                          
//  REMARK   3      S11:  -0.4246 S12:  -0.4216 S13:   0.1672                       
//  REMARK   3      S21:   0.5307 S22:   0.3071 S23:   0.0385                       
//  REMARK   3      S31:   0.0200 S32:  -0.2454 S33:   0.1174                       
//  REMARK   3                                                                      
//  REMARK   3   TLS GROUP : 2
//   ...                                                      
//   or (1zy8)
//  REMARK   7                                                                      
//  REMARK   7 TLS DEFINITIONS USED IN A FEW FINAL ROUNDS                           
//  REMARK   7 OF REFINEMENT:                                                       
//  REMARK   7 TLS DETAILS                                                          

  private boolean remarkTls() throws Exception {
    int nGroups = 0;
    int iGroup = 0;
    String components = null;
     Lst<Map<String, Object>> tlsGroups = null;
    Map<String, Object> tlsGroup = null;
     Lst<Map<String, Object>> ranges = null;
    Map<String, Object> range = null;
    String remark = line.substring(0, 11);
    while (readHeader(true) != null && line.startsWith(remark)) {
      try {
        String[] tokens = PT.getTokens(line.substring(10).replace(':', ' '));
        if (tokens.length < 2)
          continue;
        Logger.info(line);
        if (tokens[1].equalsIgnoreCase("GROUP")) {
          tlsGroup = new Hashtable<String, Object>();
          ranges = new  Lst<Map<String, Object>>();
          tlsGroup.put("ranges", ranges);
          tlsGroups.addLast(tlsGroup);
          tlsGroupID = parseIntStr(tokens[tokens.length - 1]);
          tlsGroup.put("id", Integer.valueOf(tlsGroupID));
        } else if (tokens[0].equalsIgnoreCase("NUMBER")) {
          if (tokens[2].equalsIgnoreCase("COMPONENTS")) {
            // ignore
          } else {
            nGroups = parseIntStr(tokens[tokens.length - 1]);
            if (nGroups < 1)
              break;
            if (vTlsModels == null)
              vTlsModels = new  Lst<Map<String, Object>>();
            tlsGroups = new  Lst<Map<String, Object>>();
            appendLoadNote(line.substring(11).trim());
          }
        } else if (tokens[0].equalsIgnoreCase("COMPONENTS")) {
          components = line;
        } else if (tokens[0].equalsIgnoreCase("RESIDUE")) {
          /*
          REMARK   3    RESIDUE RANGE :   A     2        A     8
          token 0  1      2       3   4   5     6        7     8
          */
          range = new Hashtable<String, Object>();
          char chain1, chain2;
          int res1, res2;
          if (tokens.length == 6) {
            chain1 = tokens[2].charAt(0);
            chain2 = tokens[4].charAt(0);
            res1 = parseIntStr(tokens[3]);
            res2 = parseIntStr(tokens[5]);
          } else {
            int toC = components.indexOf(" C ");
            int fromC = components.indexOf(" C ", toC + 4);
            chain1 = line.charAt(fromC);
            chain2 = line.charAt(toC);
            res1 = parseIntRange(line, fromC + 1, toC);
            res2 = parseIntStr(line.substring(toC + 1));
          }
          if (chain1 == chain2) {
            range.put("chains", "" + chain1 + chain2);
            if (res1 <= res2) {
              range.put("residues", new int[] { res1, res2 });
              ranges.addLast(range);
            } else {
              tlsAddError(" TLS group residues are not in order (range ignored)");            
            }
          } else {
            tlsAddError(" TLS group chains are different (range ignored)");            
          }
        } else if (tokens[0].equalsIgnoreCase("SELECTION")) {
          /*
           * REMARK   3    SELECTION: RESID 513:544 OR RESID 568:634 OR RESID
           * 
           * REMARK   3    SELECTION: (CHAIN A AND RESID 343:667)                            
           */
          char chain = '\0';
          for (int i = 1; i < tokens.length; i++) {
            if (tokens[i].toUpperCase().indexOf("CHAIN") >= 0) {
              chain = tokens[++i].charAt(0);
              continue;
            }
            int resno = parseIntStr(tokens[i]);
            if (resno == Integer.MIN_VALUE)
              continue;
            range = new Hashtable<String, Object>();
            range.put("residues", new int[] { resno, parseIntStr(tokens[++i]) });
            if (chain != '\0')
              range.put("chains", "" + chain + chain);
            ranges.addLast(range);
          }
        } else if (tokens[0].equalsIgnoreCase("ORIGIN")) {
          /*
          REMARK   3    ORIGIN FOR THE GROUP (A):  17.3300  62.7550  29.2560              
          */
          /* 
           * Parse tightly packed numbers e.g. -999.1234-999.1234-999.1234
           * assuming there are 4 places to the right of each decimal point
           */
          P3 origin = new P3();
          tlsGroup.put("origin", origin);
          if (tokens.length == 8) {
            origin.set(parseFloatStr(tokens[5]), parseFloatStr(tokens[6]),
                parseFloatStr(tokens[7]));
          } else {
            int n = line.length();
            origin.set(parseFloatRange(line, n - 27, n - 18),
                parseFloatRange(line, n - 18, n - 9), parseFloatRange(line, n - 9, n));
          }
          if (Float.isNaN(origin.x) || Float.isNaN(origin.y) || Float.isNaN(origin.z)) {
            origin.set(Float.NaN, Float.NaN, Float.NaN);
            tlsAddError("invalid origin: " + line);
          }
        } else if (tokens[1].equalsIgnoreCase("TENSOR")) {
          /*
           * REMARK   3    T TENSOR                                                          
           * REMARK   3      T11:   0.0798 T22:   0.0357                                     
           * REMARK   3      T33:   0.0678 T12:   0.0530                                     
           * REMARK   3      T13:  -0.0070 T23:   0.0011                                     
           */
          char tensorType = tokens[0].charAt(0);
          String s = (readHeader(true).substring(10)
              + readHeader(true).substring(10) + readHeader(true).substring(10)).replace(
                  tensorType, ' ').replace(':', ' ');
          //System.out.println("Tensor data = " + s);
          tokens = PT.getTokens(s);
          float[][] data = new float[3][3];
          tlsGroup.put("t" + tensorType, data);
          for (int i = 0; i < tokens.length; i++) {
            int ti = tokens[i].charAt(0) - '1';
            int tj = tokens[i].charAt(1) - '1';
            data[ti][tj] = parseFloatStr(tokens[++i]);
            if (ti < tj)
              data[tj][ti] = data[ti][tj];
          }
          for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
              if (Float.isNaN(data[i][j]))
                tlsAddError("invalid tensor: " + Escape.escapeFloatAA(data, false));
          //System.out.println("Tensor t" + tensorType + " = " + Escape.escape(tensor));
          if (tensorType == 'S' && ++iGroup == nGroups) {
            Logger.info(nGroups + " TLS groups read");
            readHeader(true);
            break;
          }
        }
      } catch (Exception e) {
        Logger.error(line + "\nError in TLS parser: ");
        System.out.println(e.getMessage());
        tlsGroups = null;
        break;
      }
    }
    if (tlsGroups != null) {
      Hashtable<String, Object> tlsModel = new Hashtable<String, Object>();
      tlsModel.put("groupCount", Integer.valueOf(nGroups));
      tlsModel.put("groups", tlsGroups);
      vTlsModels.addLast(tlsModel);
    }
    return (nGroups < 1);
  }

  /**
   * for now, we just ignore TLS details if user has selected a specific model
   */
  private void handleTlsMissingModels() {
    vTlsModels = null;
  }

  /**
   * Sets the atom property property_tlsGroup based on TLS group ranges
   * and adds "TLS" key to model's auxiliaryInfo.
   * 
   * @param iGroup
   * @param iModel
   * @param symmetry 
   */
  @SuppressWarnings("unchecked")
  private void setTlsGroups(int iGroup, int iModel, SymmetryInterface symmetry) {

    // TLS.groupCount   Integer
    // TLS.groups       List of Map
    //   .id            String
    //   .ranges        List of Map
    //      .chains     String
    //      .residues   int[2]
    //   .origin        P3
    //   .tT            float[3][3]
    //   .tL            float[3][3]
    //   .tS            float[3][3]
    //
    // ultimately, each atom gets an associated TLS-U and TLS-R org.jmol.util.Tensor
    // that can be visualized using 
    //
    //  ellipsoid set "TLS-R"
    //  ellipsoids ON
    //
    //
    
    Logger.info("TLS model " + (iModel + 1) + " set " + (iGroup + 1));
    Map<String, Object> tlsGroupInfo = vTlsModels.get(iGroup);
    Lst<Map<String, Object>> groups = ( Lst<Map<String, Object>>) tlsGroupInfo
        .get("groups");
    int index0 = asc.getAtomSetAtomIndex(iModel);
    float[] data = new float[asc.getAtomSetAtomCount(iModel)];
    int indexMax = index0 + data.length;
    Atom[] atoms = asc.atoms;
    int nGroups = groups.size();
    for (int i = 0; i < nGroups; i++) {
      Map<String, Object> group = groups.get(i);
      Lst<Map<String, Object>> ranges = ( Lst<Map<String, Object>>) group
          .get("ranges");
      tlsGroupID = ((Integer) group.get("id")).intValue();
      for (int j = ranges.size(); --j >= 0;) {
        String chains = (String) ranges.get(j).get("chains");
        int[] residues = (int[]) ranges.get(j).get("residues");
        int chain0 = 0 + chains.charAt(0);
        int chain1 = 0 + chains.charAt(1);
        int res0 = residues[0];
        int res1 = residues[1];
        int index1 = findAtomForRange(index0, indexMax, chain0, res0, false);
        int index2 = (index1 >= 0 ? findAtomForRange(index1, indexMax, chain1,
            res1, false) : -1);
        if (index2 < 0) {
          Logger.info("TLS processing terminated");
          return;
        }
        Logger.info("TLS ID=" + tlsGroupID + " model atom index range "
            + index1 + "-" + index2);
        boolean isSameChain = (chain0 == chain1);  // will be true
        // could demand a contiguous section here for each range.
        for (int iAtom = index0; iAtom < indexMax; iAtom++) {
          Atom atom = atoms[iAtom];
          if (isSameChain ? atom.sequenceNumber >= res0 && atom.sequenceNumber <= res1
            : atom.chainID > chain0 && atom.chainID < chain1 
              || atom.chainID == chain0 && atom.sequenceNumber >= res0
              || atom.chainID == chain1 && atom.sequenceNumber <= res1
          ) {
              data[iAtom - index0] = tlsGroupID;
              setTlsTensor(atom, group, symmetry);
            }
        }
      }
    }
    asc.setAtomProperties("tlsGroup", data, iModel, true);
    asc.setModelInfoForSet("TLS", tlsGroupInfo, iModel);
    asc.setTensors();
  }

  private int findAtomForRange(int atom1, int atom2, int chain, int resno,
                          boolean isLast) {
    int iAtom = findAtom(atom1, atom2, chain, resno, true);
    return (isLast && iAtom >= 0 ? findAtom(iAtom, atom2, chain, resno, false) : iAtom);
  }

  private int findAtom(int atom1, int atom2, int chain, int resno, boolean isTrue) {
    Atom[] atoms = asc.atoms;
    for (int i = atom1; i < atom2; i++) {
     Atom atom = atoms[i];
     if ((atom.chainID == chain && atom.sequenceNumber == resno) == isTrue)
       return i;
    }
    if (isTrue) {
      Logger.warn("PdbReader findAtom chain=" + chain + " resno=" + resno + " not found");
      tlsAddError("atom not found: chain=" + chain + " resno=" + resno);
    }
    return (isTrue ? -1 : atom2);
  }

  private final float[] dataT = new float[8];

  private static final float RAD_PER_DEG = (float) (Math.PI / 180);
  private static final float _8PI2_ = (float) (8 * Math.PI * Math.PI);
  private Map<Atom, float[]>tlsU;
  
  private void setTlsTensor(Atom atom, Map<String, Object> group, SymmetryInterface symmetry) {
    P3 origin = (P3) group.get("origin");
    if (Float.isNaN(origin.x))
      return;
    
    float[][] T = (float[][]) group.get("tT");
    float[][] L = (float[][]) group.get("tL");
    float[][] S = (float[][]) group.get("tS");

    if (T == null || L == null || S == null)
      return;

    // just factor degrees-to-radians into x, y, and z rather
    // than create all new matrices

    float x = (atom.x - origin.x) * RAD_PER_DEG;
    float y = (atom.y - origin.y) * RAD_PER_DEG;
    float z = (atom.z - origin.z) * RAD_PER_DEG;

    float xx = x * x;
    float yy = y * y;
    float zz = z * z;
    float xy = x * y;
    float xz = x * z;
    float yz = y * z;

    /*
     * 
     * from pymmlib-1.2.0.tar|mmLib/TLS.py
     * 
     */

    dataT[0] = T[0][0];
    dataT[1] = T[1][1];
    dataT[2] = T[2][2];
    dataT[3] = T[0][1];
    dataT[4] = T[0][2];
    dataT[5] = T[1][2];
    dataT[6] = 12; // (non)ORTEP type 12 -- macromolecular Cartesian

    float[] anisou = new float[8];

    float bresidual = (Float.isNaN(atom.bfactor) ? 0 : atom.bfactor / _8PI2_);

    anisou[0] /*u11*/= dataT[0] + L[1][1] * zz + L[2][2] * yy - 2 * L[1][2]
        * yz + 2 * S[1][0] * z - 2 * S[2][0] * y;
    anisou[1] /*u22*/= dataT[1] + L[0][0] * zz + L[2][2] * xx - 2 * L[2][0]
        * xz - 2 * S[0][1] * z + 2 * S[2][1] * x;
    anisou[2] /*u33*/= dataT[2] + L[0][0] * yy + L[1][1] * xx - 2 * L[0][1]
        * xy - 2 * S[1][2] * x + 2 * S[0][2] * y;
    anisou[3] /*u12*/= dataT[3] - L[2][2] * xy + L[1][2] * xz + L[2][0] * yz
        - L[0][1] * zz - S[0][0] * z + S[1][1] * z + S[2][0] * x - S[2][1] * y;
    anisou[4] /*u13*/= dataT[4] - L[1][1] * xz + L[1][2] * xy - L[2][0] * yy
        + L[0][1] * yz + S[0][0] * y - S[2][2] * y + S[1][2] * z - S[1][0] * x;
    anisou[5] /*u23*/= dataT[5] - L[0][0] * yz - L[1][2] * xx + L[2][0] * xy
        + L[0][1] * xz - S[1][1] * x + S[2][2] * x + S[0][1] * y - S[0][2] * z;
    anisou[6] = 12; // macromolecular Cartesian
    anisou[7] = bresidual;
    if (tlsU == null)
      tlsU = new Hashtable<Atom, float[]>();
     tlsU.put(atom, anisou);

    // symmetry is set to [1 1 1 90 90 90] -- Cartesians, not actual unit cell

    atom.addTensor(symmetry.getTensor(vwr, dataT).setType(null), "TLS-U", false);
  }

  private void tlsAddError(String error) {
    if (sbTlsErrors == null)
      sbTlsErrors = new SB();
    sbTlsErrors.append(fileName).appendC('\t').append("TLS group ").appendI(
        tlsGroupID).appendC('\t').append(error).appendC('\n');
  }

  protected static float fixRadius(float r) {    
    return (r < 0.9f ? 1 : r);
    // based on parameters in http://pdb2pqr.svn.sourceforge.net/viewvc/pdb2pqr/trunk/pdb2pqr/dat/
    // AMBER forcefield, H atoms may be given 0 (on O) or 0.6 (on N) for radius
    // PARSE forcefield, lots of H atoms may be given 0 radius
    // CHARMM forcefield, HN atoms may be given 0.2245 radius
    // PEOEPB forcefield, no atoms given 0 radius
    // SWANSON forcefield, HW (on water) will be given 0 radius, and H on oxygen given 0.9170
  }

  private Lst<int[]> vConnect;
  private int connectNextAtomIndex = 0;
  private int connectNextAtomSet = 0;
  private int[] connectLast;

  private void addConnection(int[] is) {
    if (vConnect == null) {
      connectLast = null;
      vConnect = new Lst<int[]>();
    }
    if (connectLast != null) {
      if (is[0] == connectLast[0] && is[1] == connectLast[1]
          && is[2] != JmolAdapter.ORDER_HBOND) {
        connectLast[2]++;
        return;
      }
    }
    vConnect.addLast(connectLast = is);
  }

  private void connectAllBad(int maxSerial) {
    // between 12.1.51-12.2.20 and 12.3.0-12.3.20 we have 
    // a problem in that this method was used for connect
    // this means that scripts created during this time could have incorrect 
    // BOND indexes in the state script. It was when we added reading of H atoms
    int firstAtom = connectNextAtomIndex;
    for (int i = connectNextAtomSet; i < asc.atomSetCount; i++) {
      int count = asc.getAtomSetAtomCount(i);
      asc.setModelInfoForSet("PDB_CONECT_firstAtom_count_max",
          new int[] { firstAtom, count, maxSerial }, i);
      if (vConnect != null) {
        asc.setModelInfoForSet("PDB_CONECT_bonds", vConnect, i);
        asc.setGlobalBoolean(AtomSetCollection.GLOBAL_CONECT);
      }
      firstAtom += count;
    }
    vConnect = null;
    connectNextAtomSet = asc.iSet + 1;
    connectNextAtomIndex = firstAtom;
  }

  private void connectAll(int maxSerial, boolean isConnectStateBug) {
    AtomSetCollection a = asc;
    int index = a.iSet;
    if (index < 0)
      return;
    if (isConnectStateBug) {
      connectAllBad(maxSerial);
      return;
    }
    a.setCurrentModelInfo(
        "PDB_CONECT_firstAtom_count_max",
        new int[] { a.getAtomSetAtomIndex(index),
            a.getAtomSetAtomCount(index), maxSerial });
    if (vConnect == null)
      return;
    int firstAtom = connectNextAtomIndex;
    for (int i = a.atomSetCount; --i >= connectNextAtomSet;) {
      a.setModelInfoForSet("PDB_CONECT_bonds", vConnect, i);
      a.setGlobalBoolean(AtomSetCollection.GLOBAL_CONECT);
      firstAtom += a.getAtomSetAtomCount(i);
    }
    vConnect = null;
    connectNextAtomSet = index + 1;
    connectNextAtomIndex = firstAtom;
  }
  
}

