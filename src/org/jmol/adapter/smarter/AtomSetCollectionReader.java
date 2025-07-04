/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-22 14:12:46 -0500 (Sun, 22 Oct 2006) $
 * $Revision: 5999 $
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

package org.jmol.adapter.smarter;

import java.io.BufferedReader;
import java.util.Map;

import org.jmol.adapter.smarter.XtalSymmetry.FileSymmetry;
import org.jmol.api.Interface;
import org.jmol.api.JmolAdapter;
import org.jmol.script.SV;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.Logger;
import org.jmol.util.SimpleUnitCell;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.api.GenericBinaryDocument;
import javajs.api.GenericLineReader;
import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.M3d;
import javajs.util.M4d;
import javajs.util.OC;
import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.Qd;
import javajs.util.SB;
import javajs.util.T3d;
import javajs.util.T4d;
import javajs.util.V3d;

/*
 * Notes 9/2006 Bob Hanson
 * 
 * all parsing functions now moved to org.jmol.util.Parser
 * 
 * to add symmetry capability to any reader, some or all of the following 
 * methods need to be there:
 * 
 *  setFractionalCoordinates()
 *  setSpaceGroupName()
 *  setUnitCell()
 *  setUnitCellItem()
 *  setAtomCoord()
 * 
 * At the very minimum, you need:
 * 
 *  setAtomCoord()
 * 
 * so that:
 *  (a) atom coordinates can be turned fractional by load parameters
 *  (b) symmetry can be applied once per model in the file
 *  
 *  If you know where the end of the atom+bond data are, then you can
 *  use applySymmetryAndSetTrajectory() once, just before exiting. Otherwise, use it
 *  twice -- it has a check to make sure it doesn't RUN twice -- once
 *  at the beginning and once at the end of the model.
 *  
 * htParams is used for passing information to the readers
 * and for returning information from the readers
 * 
 * It won't be null at this stage.
 * 
 * from Eval or Viewer:
 * 
 *  applySymmetryToBonds
 *  atomTypes (for Mol2Reader)
 *  bsModels
 *  filter
 *  firstLastStep
 *  firstLastSteps
 *  getHeader
 *  isTrajectory
 *  lattice
 *  manifest (for SmarterJmolAdapter)
 *  modelNumber
 *  spaceGroupIndex
 *  symmetryRange
 *  unitcell
 *  packed
 *  
 * from FileManager:
 * 
 *  fullPathName
 *  subFileList (for SmarterJmolAdapter)
 * 
 * from MdTopReader:
 *   
 *  isPeriodic
 *  templateAtomCount
 *  
 * from MdCrdReader:   
 * 
 *  trajectorySteps
 *  
 * from Resolver:
 * 
 *  filteredAtomCount
 *  ptFile
 *  readerName
 *  templateAtomCount
 *  
 *  
 * from AtomSetCollectionReader:
 *  
 *  bsFilter
 *  
 * 
 */

public abstract class AtomSetCollectionReader implements GenericLineReader {

  public final static double ANGSTROMS_PER_BOHR = 0.5291772; // used by SpartanArchive and others

  protected static final String CELL_TYPE_CONVENTIONAL = "conventional";
  protected static final String CELL_TYPE_PRIMITIVE = "primitive";
  protected static final String CELL_TYPE_SUPER = "super";

  public boolean isBinary;
  public boolean debugging;
  protected boolean requiresBSFilter;

  public M3d primitiveToCrystal;

  public AtomSetCollection asc;
  public BufferedReader reader;
  public GenericBinaryDocument binaryDoc;
  protected String readerName;
  public Map<String, Object> htParams;
  public Lst<P3d[]> trajectorySteps;
  private Object domains;
  public Object validation, dssr;
  protected boolean isConcatenated;
  public String addedData, addedDataKey;
  //  /**
  //   * This field has no significance for Jmol-SwingJS
  //   */
  //  public boolean floatifyJavaDouble = false;//true; -- removed in Jmol 15.32.53 - all high precision
  public Map<String, Object> thisBiomolecule;
  public Lst<M4d> lstNCS;

  //protected String parameterData;

  // buffer
  public String line, prevline;
  protected int[] next = new int[1];
  protected int ptLine;

  // protected/public state variables

  protected String latticeType;
  public int[] latticeCells;
  public Object fillRange;
  public boolean doProcessLines;
  public boolean iHaveUnitCell;
  public boolean iHaveSymmetryOperators;
  public boolean continuing = true;

  public Viewer vwr; // used by GenNBOReader and by CifReader

  public boolean doApplySymmetry;
  protected boolean ignoreFileSymmetryOperators;
  protected boolean isTrajectory;
  public boolean applySymmetryToBonds;
  protected boolean doCheckUnitCell;
  protected boolean getHeader;
  protected boolean isSequential;
  public boolean optimize2D;
  public boolean noHydrogens;
  public boolean noMinimize;
  public boolean is2D;

  public boolean isMolecular; // only for CIF so that it can read multiple unit cells
  protected int templateAtomCount;
  public int modelNumber;
  public int vibrationNumber;
  public int desiredVibrationNumber = Integer.MIN_VALUE;
  protected BS bsModels;
  protected boolean useFileModelNumbers; // PDB, MMCIF only
  protected boolean havePartialChargeFilter;
  public String calculationType = "?";
  protected String sgName;
  protected boolean ignoreFileUnitCell;
  protected boolean ignoreFileSpaceGroupName;
  public double[] unitCellParams; //0-5 a b c alpha beta gamma; 6-21 matrix c->f
  protected int desiredModelNumber = Integer.MIN_VALUE;
  public FileSymmetry symmetry;
  protected OC out;
  protected boolean iHaveFractionalCoordinates;
  public boolean doPackUnitCell;
  protected P3d ptSupercell;
  protected boolean mustFinalizeModelSet;
  protected boolean forcePacked;
  /**
   * set false for MMCIF; considered false when there is only one operation or
   * no packing
   */
  public boolean checkNearAtoms = true;

  /**
   * the range outside the unit cell that will still be considered packing
   * range;
   */
  private Number packingRange;
  /**
   * this value is way too large; these are fractional coordinates that should
   * be much more precise than this; setting doublePrecision TRUE replaces this
   * with at the most SLOPSP, which is 0.0001
   */
  private static final double OLD_PACKING_RANGE = 0.02d;
  private static final double LOW_PRECISION_PACKING_RANGE = SimpleUnitCell.SLOPSP;


  public double getPackingRangeValue(double def) {
    // def because we have used 0.001 for the MSRdr.trimAtoms()
    return (packingRange != null ? packingRange.doubleValue() : def != 0 ? def : OLD_PACKING_RANGE);
  }

  protected double cellSlop = SimpleUnitCell.SLOPSP; // initially just single precision
  protected boolean rotateHexCell; // aflow CIF reader only
  protected boolean isPrimitive; // VASP POSCAR reader
  public int modDim; // modulation dimension

  protected boolean lowPrecision;
  private boolean highprecision0 = Viewer.isHighPrecision;

  // private state variables

  private SB loadNote = new SB();
  public boolean doConvertToFractional;
  boolean fileCoordinatesAreFractional;
  protected boolean merging;
  double symmetryRange;
  private int[] firstLastStep;
  private int lastModelNumber = Integer.MAX_VALUE;
  public int desiredSpaceGroupIndex = -1;
  protected double latticeScaling = Double.NaN;
  protected P3d unitCellOffset;
  private boolean unitCellOffsetFractional;
  private Lst<String> moreUnitCellInfo;
  public T3d paramsLattice;
  public boolean paramsCentroid;
  private boolean paramsPacked;

  // JmolDataReader and GenNBOReader only
  protected P3d fileScaling;
  protected P3d fileOffset;
  private P3d fileOffsetFractional;

  protected String filePath;
  protected String fileName;

  /**
   * first atom index for this collection, current modelset.ac
   */
  public int baseAtomIndex;

  public int baseBondIndex;

  protected int stateScriptVersionInt = Integer.MAX_VALUE; // for compatibility PDB reader Jmol 12.0.RC24 fix 
  // http://jmol.svn.sourceforge.net/viewvc/jmol/trunk/Jmol/src/org/jmol/adapter/readers/cifpdb/PdbReader.java?r1=13502&r2=13525

  protected void setup(String fullPath, Map<String, Object> htParams,
                       Object readerOrDocument) {
    setupASCR(fullPath, htParams, readerOrDocument);
  }

  protected void setupASCR(String fullPath, Map<String, Object> htParams,
                           Object readerOrDocument) {
    if (fullPath == null)
      return;
    debugging = Logger.debugging;
    this.htParams = htParams;
    // check for drag-drop or coerced type
    filePath = FileManager.stripTypePrefix("" + htParams.get("fullPathName"));
    int i = filePath.lastIndexOf('/');
    fileName = filePath.substring(i + 1);
    if (readerOrDocument instanceof BufferedReader)
      this.reader = (BufferedReader) readerOrDocument;
    else if (readerOrDocument instanceof GenericBinaryDocument)
      binaryDoc = (GenericBinaryDocument) readerOrDocument;
  }

  Object readData() throws Exception {
    initialize();
    asc = new AtomSetCollection(readerName, this, null, null);
    try {
      initializeReader();
      if (binaryDoc == null) {
        if (line == null && continuing)
          rd();
        while (line != null && continuing)
          if (checkLine())
            rd();
      } else {
        binaryDoc.setOutputChannel(out);
        processBinaryDocument();
      }
      finalizeSubclassReader(); // upstairs
      if (!isFinalized)
        finalizeReaderASCR();
    } catch (Throwable e) {
      Logger.info("Reader error: " + e);
      e.printStackTrace();
      setError(e);
    }
    if (reader != null)
      reader.close();
    if (binaryDoc != null)
      binaryDoc.close();
    return finish();
  }

  private void fixBaseIndices() {
    try {
      Integer ii = (Integer) htParams.get("baseModelIndex");
      if (ii == null)
        return;
      int baseModelIndex = ii.intValue();
      baseAtomIndex += asc.ac;
      baseBondIndex += asc.bondCount;
      baseModelIndex += asc.atomSetCount;
      htParams.put("baseAtomIndex", Integer.valueOf(baseAtomIndex));
      htParams.put("baseBondIndex", Integer.valueOf(baseBondIndex));
      htParams.put("baseModelIndex", Integer.valueOf(baseModelIndex));
    } catch (Exception e) {
      // ignore
    }
  }

  protected Object readDataObject(Object node) throws Exception {
    initialize();
    asc = new AtomSetCollection(readerName, this, null, null);
    initializeReader();
    processDOM(node);
    return finish();
  }

  /**
   * 
   * @param DOMNode
   */
  protected void processDOM(Object DOMNode) {
    // XML readers only
  }

  /**
   * @throws Exception
   */
  protected void processBinaryDocument() throws Exception {
    // Binary readers only
  }

  protected void initializeReader() throws Exception {
    // reader-dependent
  }

  /**
   * @return true if need to read new line
   * @throws Exception
   * 
   */
  protected boolean checkLine() throws Exception {
    // reader-dependent
    return true;
  }

  /**
   * sets continuing and doProcessLines
   * 
   * @return TRUE if continuing, FALSE if not
   * 
   */
  public boolean checkLastModel() {
    if (isLastModel(modelNumber) && doProcessLines)
      return (continuing = doProcessLines = false);
    doProcessLines = false;
    return true;
  }

  /**
   * after reading a model, Q: Is this the last model?
   * 
   * @param modelNumber
   * @return Yes/No
   */
  public boolean isLastModel(int modelNumber) {
    return (desiredModelNumber > 0 || modelNumber >= lastModelNumber);
  }

  public void appendLoadNote(String info) {
    if (info == null) {
      loadNote = new SB();
      return;
    }
    loadNote.append(info).append("\n");
    Logger.info(info);
  }

  @SuppressWarnings("unchecked")
  protected void initializeTrajectoryFile() {
    // add a dummy atom, just so not "no atoms found"
    asc.addAtom(new Atom());
    trajectorySteps = (Lst<P3d[]>) htParams.get(JC.INFO_TRAJECTORY_STEPS);
    if (trajectorySteps == null)
      htParams.put(JC.INFO_TRAJECTORY_STEPS, trajectorySteps = new Lst<P3d[]>());
  }

  /**
   * Optional reader-specific method run first. May or may not call
   * finalizeReaderASCR(), when symmetry for the last model is finalized if
   * necessary.
   * 
   * @throws Exception
   */
  protected void finalizeSubclassReader() throws Exception {
    // can be customized
  }

  protected boolean isFinalized;

  protected boolean noPack;

  /**
   * actual SUPERCELL keyword, not just "cell="
   */
  public boolean isSUPERCELL;

  protected void finalizeReaderASCR() throws Exception {
    isFinalized = true;
    if (asc.atomSetCount > 0) {
      if (asc.atomSetCount == 1) {
        asc.setCurrentModelInfo("dbName", htParams.get("dbName"));
        asc.setCurrentModelInfo("auxFiles", htParams.get("auxFiles"));
      }
      applySymmetryAndSetTrajectory();
      asc.finalizeStructures();
      if (doCentralize)
        asc.centralize();
      if (fillRange != null)// && previousUnitCell == null)
        asc.setInfo("boundbox", fillRange);

      Map<String, Object> info = asc.getAtomSetAuxiliaryInfo(0);
      if (info != null) {
        if (domains != null) {
          asc.setGlobalBoolean(JC.GLOBAL_DOMAINS);
          String s = ((SV) domains).getMapKeys(2, true);
          int pt = s.indexOf("{ ", 2);
          if (pt >= 0)
            s = s.substring(pt + 2);
          pt = s.indexOf("_metadata");
          if (pt < 0)
            pt = s.indexOf("metadata");
          if (pt >= 0)
            s = s.substring(0, pt);
          s = PT.rep(PT.replaceAllCharacters(s, "{}", "").trim(), "\n", "\n  ")
              + "\n\nUse SHOW DOMAINS for details.";
          appendLoadNote("\nDomains loaded:\n   " + s);
          for (int i = asc.atomSetCount; --i >= 0;) {
            info = asc.getAtomSetAuxiliaryInfo(i);
            info.put("domains", domains);
          }
        }
        if (validation != null) {
          for (int i = asc.atomSetCount; --i >= 0;) {
            info = asc.getAtomSetAuxiliaryInfo(i);
            info.put("validation", validation);
          }
        }
        if (dssr != null) {
          info.put("dssrJSON", Boolean.TRUE);
          for (int i = asc.atomSetCount; --i >= 0;) {
            info = asc.getAtomSetAuxiliaryInfo(i);
            info.put("dssr", dssr);
          }
        }
      }
    }
    setLoadNote();
  }

  /////////////////////////////////////////////////////////////////////////////////////

  protected String setLoadNote() {
    String s = loadNote.toString();
    if (loadNote.length() > 0)
      asc.setInfo("modelLoadNote", s);
    return s;
  }

  public void setIsPDB() {
    asc.setGlobalBoolean(JC.GLOBAL_ISPDB);
    if (htParams.get("pdbNoHydrogens") != null)
      asc.setInfo("pdbNoHydrogens", htParams.get("pdbNoHydrogens"));
    if (checkFilterKey("ADDHYDROGENS"))
      asc.setInfo("pdbAddHydrogens", Boolean.TRUE);
  }

  protected void setModelPDB(boolean isPDB) {
    if (isPDB)
      asc.setGlobalBoolean(JC.GLOBAL_ISPDB);
    else
      asc.clearGlobalBoolean(JC.GLOBAL_ISPDB);
    asc.setCurrentModelInfo(JC.getBoolName(JC.GLOBAL_ISPDB),
        isPDB ? Boolean.TRUE : null);
  }

  private Object finish() {
    if (Viewer.isDoublePrecision != highprecision0)
      vwr.setBooleanPropertyTok("doubleprecision", T.doubleprecision,
          highprecision0);
    String s = (String) htParams.get("loadState");
    asc.setInfo("loadState", s == null ? "" : s);
    s = (String) htParams.get("smilesString");
    if (s != null)
      asc.setInfo("smilesString", s);
    if (!htParams.containsKey("templateAtomCount"))
      htParams.put("templateAtomCount", Integer.valueOf(asc.ac));
    if (bsFilter != null) {
      htParams.put("filteredAtomCount",
          Integer.valueOf(BSUtil.cardinalityOf(bsFilter)));
      htParams.put("bsFilter", bsFilter);
    }
    if (!calculationType.equals("?"))
      asc.setInfo("calculationType", calculationType);

    String name = asc.fileTypeName;
    String fileType = name;
    if (fileType.indexOf("(") >= 0)
      fileType = fileType.substring(0, fileType.indexOf("("));
    for (int i = asc.atomSetCount; --i >= 0;) {
      asc.setModelInfoForSet("fileName", filePath, i);
      asc.setModelInfoForSet("fileType", fileType, i);
    }
    asc.freeze(reverseModels);
    if (asc.errorMessage != null)
      return asc.errorMessage + "\nfor file " + filePath + "\ntype " + name;
    if (!merging
        && (asc.bsAtoms == null ? asc.ac == 0 : asc.bsAtoms.nextSetBit(0) < 0)
        && fileType.indexOf("DataOnly") < 0
        && asc.atomSetInfo.get("dataOnly") == null)
      return "No atoms found\nfor file " + filePath + "\ntype " + name;
    fixBaseIndices();
    return asc;
  }

  /**
   * @param e
   */
  private void setError(Throwable e) {
    String s = e.getMessage();
    if (line == null)
      asc.errorMessage = "Error reading file at end of file \n" + s;
    else
      asc.errorMessage = "Error reading file at line " + ptLine + ":\n" + line
          + "\n" + s;
    e.printStackTrace();
  }

  @SuppressWarnings("unchecked")
  private void initialize() {
    if (htParams.containsKey("baseAtomIndex"))
      baseAtomIndex = ((Integer) htParams.get("baseAtomIndex")).intValue();
    if (htParams.containsKey("baseBondIndex"))
      baseBondIndex = ((Integer) htParams.get("baseBondIndex")).intValue();
    initializeSymmetry();
    vwr = (Viewer) htParams.remove("vwr"); // don't pass this on to user
    if (htParams.containsKey("stateScriptVersionInt"))
      stateScriptVersionInt = ((Integer) htParams.get("stateScriptVersionInt"))
          .intValue();
    packingRange = (Number) htParams.get("packingRange");
    merging = htParams.containsKey("merging");
    getHeader = htParams.containsKey("getHeader");
    isSequential = htParams.containsKey("isSequential");
    readerName = (String) htParams.get("readerName");
    if (htParams.containsKey("outputChannel"))
      out = (OC) htParams.get("outputChannel");
    //parameterData = (String) htParams.get("parameterData");
    if (htParams.containsKey("vibrationNumber"))
      desiredVibrationNumber = ((Integer) htParams.get("vibrationNumber"))
          .intValue();
    else if (htParams.containsKey("modelNumber"))
      desiredModelNumber = ((Integer) htParams.get("modelNumber")).intValue();
    applySymmetryToBonds = htParams.containsKey("applySymmetryToBonds");
    bsFilter = (requiresBSFilter ? (BS) htParams.get("bsFilter") : null);
    setFilter(null);
    fillRange = htParams.get(JC.LOAD_OPTION_FILL_RANGE);
    paramsLattice = (T3d) htParams.get("lattice");
    Object o = htParams.get("supercell");
    // noPack does not work as advertised
    noPack = checkFilterKey("NOPACK");
    if (strSupercell != null && !noPack) {
      // only for filter cell=
      forcePacked = true;
    }
    if (o instanceof P3d) {
      P3d s = ptSupercell = (P3d) o;
      if (s.length() != 1) {
        strSupercell = ((int) s.x) + "a," + ((int) s.y) + "b," + ((int) s.z)
            + "c";
        isSUPERCELL = true;
      }
    } else if (o instanceof String) {
      strSupercell = (String) o;
      isSUPERCELL = true;
    }
    // ptFile < 0 indicates just one file being read
    // ptFile >= 0 indicates multiple files are being loaded
    // if the file is not the first read in the LOAD command, then
    // we look to see if it was loaded using LOAD ... "..." COORD ....
    int ptFile = (htParams.containsKey("ptFile")
        ? ((Integer) htParams.get("ptFile")).intValue()
        : -1);
    isTrajectory = htParams.containsKey("isTrajectory");
    if (ptFile > 0 && htParams.containsKey("firstLastSteps")) {
      Object val = ((Lst<Object>) htParams.get("firstLastSteps"))
          .get(ptFile - 1);
      if (val instanceof BS) {
        bsModels = (BS) val;
      } else {
        firstLastStep = (int[]) val;
      }
    } else if (htParams.containsKey("firstLastStep")) {
      firstLastStep = (int[]) htParams.get("firstLastStep");
    } else if (htParams.containsKey("bsModels")) {
      bsModels = (BS) htParams.get("bsModels");
    }
    useFileModelNumbers = htParams.containsKey("useFileModelNumbers")
        || checkFilterKey("USEFILEMODELNUMBERS");
    if (htParams.containsKey("templateAtomCount"))
      templateAtomCount = ((Integer) htParams.get("templateAtomCount"))
          .intValue();
    if (bsModels != null || firstLastStep != null)
      desiredModelNumber = Integer.MIN_VALUE;
    if (bsModels == null && firstLastStep != null) {
      if (firstLastStep[0] < 0)
        firstLastStep[0] = 0;
      if (firstLastStep[2] == 0 || firstLastStep[1] < firstLastStep[0])
        firstLastStep[1] = -1;
      if (firstLastStep[2] < 1)
        firstLastStep[2] = 1;
      bsModels = BSUtil.newAndSetBit(firstLastStep[0]);
      if (firstLastStep[1] > firstLastStep[0]) {
        for (int i = firstLastStep[0]; i <= firstLastStep[1]; i += firstLastStep[2])
          bsModels.set(i);
      }
    }
    if (bsModels != null && (firstLastStep == null || firstLastStep[1] != -1))
      lastModelNumber = bsModels.length();

    symmetryRange = (htParams.containsKey("symmetryRange")
        ? ((Double) htParams.get("symmetryRange")).doubleValue()
        : 0);
    paramsCentroid = htParams.containsKey("centroid");
    paramsPacked = htParams.containsKey("packed");
    initializeSymmetryOptions();
    //this flag FORCES symmetry -- generally if coordinates are not fractional,
    //we may note the unit cell, but we do not apply symmetry
    //with this flag, we convert any nonfractional coordinates to fractional
    //if a unit cell is available.

    if (htParams.containsKey("spaceGroupIndex")) {
      // three options include:
      // = -1: normal -- use operators if present or name if not
      // = -2: user is supplying operators or name
      // >=0: spacegroup fully determined
      // = -999: ignore -- just the operators

      desiredSpaceGroupIndex = ((Integer) htParams.get("spaceGroupIndex"))
          .intValue();
      if (desiredSpaceGroupIndex == -2)
        sgName = (String) htParams.get("spaceGroupName");
      ignoreFileSpaceGroupName = (desiredSpaceGroupIndex == -2
          || desiredSpaceGroupIndex >= 0);
      ignoreFileSymmetryOperators = (desiredSpaceGroupIndex != -1);
    }
    if (htParams.containsKey(JC.INFO_UNIT_CELL_OFFSET)) {
      fileScaling = P3d.new3(1, 1, 1);
      fileOffset = (P3d) htParams.get(JC.INFO_UNIT_CELL_OFFSET);
      fileOffsetFractional = P3d.newP(fileOffset);
      unitCellOffsetFractional = htParams
          .containsKey("unitCellOffsetFractional");
    }
    if (htParams.containsKey("unitcell")) {
      double[] fParams = (double[]) htParams.get("unitcell");
      if (merging)
        setFractionalCoordinates(true);
      if (fParams.length == 9) {
        // these are vectors
        addExplicitLatticeVector(0, fParams, 0);
        addExplicitLatticeVector(1, fParams, 3);
        addExplicitLatticeVector(2, fParams, 6);
      } else {
        setUnitCell(fParams[0], fParams[1], fParams[2], fParams[3], fParams[4],
            fParams[5]);
      }
      ignoreFileUnitCell = iHaveUnitCell;
      if (merging && !iHaveUnitCell)
        setFractionalCoordinates(false);
      // with appendNew == false and UNITCELL parameter, we assume fractional coordinates
    }
    domains = htParams.get("domains");
    validation = htParams.get("validation");
    dssr = htParams.get("dssr");
    isConcatenated = htParams.containsKey("concatenate");
  }

  /**
   * maximum of the decimal length of unit cell lengths and fractional
   * coordinates
   */
  protected int precision;

  /**
   * Track the precision (count of y digits in "xx.yyyy") for setting cellSlop.
   * 
   * We assume that with the numbers 0.25 0.33333 that the precision is really
   * 5, not 2.
   * 
   * Note that in the end, the precision will never be set lower than 4. 
   * 
   * @param s
   * @return parsed number
   */
  protected double parsePrecision(String s) {
    // xx.yyyyy  max of current precision and number of y digits (5 here).
    // xx.yyyyy(zz) min of current precision and number of y digits - 1 (4 here)
    // I considered subtracting the number of z digits, 
    // but I think that is probably too extreme. 
    // this fixes the Wyckoff calculation for CSD XAZTAW and BROFRM05
    
    if (!filteredPrecision) {
      int pt = s.indexOf('.') + 1;
      if (pt >= 0) {
        int n = s.indexOf('(');
        if (n < 0) {
          precision = Math.max(precision, s.length() - pt);          
        } else {
          if (precision == 0)
            precision = n;
          precision = Math.min(precision, n - 1 - pt);
        } 
      }
    }
    return parseDoubleStr(s);
  }

  private void setLowPrecision() {
    lowPrecision = true;
    cellSlop = LOW_PRECISION_PACKING_RANGE;
    // if PACK x.x has not been specified, then we 
    // use FILTER "lowPrecision" to at least tighten up the packing from 0.02 to 0.0001
    if (packingRange == null)
      packingRange = Double.valueOf(LOW_PRECISION_PACKING_RANGE);
    // otherwise we leave it at 0.02f for backward compatibility
  }

  /**
   * Called by PWMAT ALWAYS and CIFReader as well if a double-precision value
   * for alpha is found.
   */
  protected void setPrecision() {
    boolean isHigh;
    if (lowPrecision) {
      isHigh = false;
      precision = 4; // legacy
    } else {
      if (precision > 1000) {
        // from filter "PRECISION=n"
        precision -= 1000;
      } else {
        // packingRange must be null
        precision = Math.min(12, Math.max(4, precision));
      }
      isHigh = (precision >= 7);
      if (isHigh) {
        vwr.setBooleanProperty("doubleprecision", true);
        if (Viewer.isHighPrecision) {
          cellSlop = SimpleUnitCell.SLOPDP;
          if (!paramsPacked)
            packingRange = Double.valueOf(cellSlop);
          asc.setInfo("highPrecision", Boolean.TRUE);
        } else {
          isHigh = false;
          precision = 6;
          appendLoadNote(
              "Structure read has high precision but this version of Jmol uses float precision.\nUse JmolD.jar or JavaScript for full precision.");
        }
      }
    }
    if (!isHigh) {
      if (precision < 10) {
        cellSlop = Math.pow(10, -precision);
        // leave packing unchanged (null default 0.02) for legacy
      }
    }
    symmetry.setPrecision(cellSlop);
    unitCellParams[SimpleUnitCell.PARAM_SLOP] = cellSlop;
    if (fileCoordinatesAreFractional) {
      for (int i = asc.ac, n = asc.getLastAtomSetAtomIndex(); --i >= n;) {
        symmetry.twelfthify(asc.atoms[i]);
      }
    }
    appendLoadNote("Precision set to " + precision + "; packing set to " + (packingRange == null ? OLD_PACKING_RANGE : packingRange.floatValue()));
  }

  protected void initializeSymmetryOptions() {
    latticeCells = new int[4];
    doApplySymmetry = false;
    T3d pt = paramsLattice;
    if (pt == null || pt.length() == 0) {
      if (!forcePacked && strSupercell == null)
        return;
      pt = P3d.new3(1, 1, 1);
    }
    latticeCells[0] = (int) pt.x;
    latticeCells[1] = (int) pt.y;
    latticeCells[2] = (int) pt.z;
    if (pt instanceof T4d)
      latticeCells[3] = (int) ((T4d) pt).w;
    doCentroidUnitCell = paramsCentroid;
    if (doCentroidUnitCell && (latticeCells[2] == -1 || latticeCells[2] == 0))
      latticeCells[2] = 1;
    boolean isPacked = forcePacked || paramsPacked;
    centroidPacked = doCentroidUnitCell && isPacked;
    doPackUnitCell = !doCentroidUnitCell && (isPacked || latticeCells[2] < 0);
    doApplySymmetry = (latticeCells[0] > 0 && latticeCells[1] > 0);
    //allows for {1 1 1} or {1 1 -1} or {555 555 0|1|-1} (-1  being "packed")
    if (!doApplySymmetry)
      latticeCells = new int[3];
  }

  protected boolean haveModel;

  public boolean doGetModel(int modelNumber, String title) {
    if (title != null && nameRequired != null && nameRequired.length() > 0
        && title.toUpperCase().indexOf(nameRequired) < 0)
      return false;
    // modelNumber is 1-based, but firstLastStep is 0-based
    boolean isOK = (bsModels == null
        ? desiredModelNumber < 1 || modelNumber == desiredModelNumber
        : modelNumber > lastModelNumber ? false
            : modelNumber > 0 && bsModels.get(modelNumber - 1) || haveModel
                && firstLastStep != null && firstLastStep[1] < 0
                && (firstLastStep[2] < 2 || (modelNumber - 1 - firstLastStep[0])
                    % firstLastStep[2] == 0));
    if (isOK && desiredModelNumber == 0)
      discardPreviousAtoms();
    haveModel |= isOK;
    if (isOK)
      doProcessLines = true;
    return isOK;
  }

  protected void discardPreviousAtoms() {
    asc.discardPreviousAtoms();
  }

  private String previousSpaceGroup;
  private double[] previousUnitCell;

  protected final void initializeSymmetry() {
    previousSpaceGroup = sgName;
    previousUnitCell = unitCellParams;
    iHaveUnitCell = ignoreFileUnitCell;
    if (!ignoreFileUnitCell) {
      unitCellParams = new double[SimpleUnitCell.PARAM_COUNT];
      //0-5 a b c alpha beta gamma
      //6-21 m00 m01... m33 cartesian-->fractional
      //22-24 supercell.x supercell.y supercell.z
      //25 scaling
      for (int i = SimpleUnitCell.PARAM_COUNT; --i >= 0;)
        unitCellParams[i] = Double.NaN;
      unitCellParams[SimpleUnitCell.PARAM_SCALE] = latticeScaling;
      unitCellParams[SimpleUnitCell.PARAM_SLOP] = cellSlop;
      symmetry = null;
    }
    if (!ignoreFileSpaceGroupName)
      sgName = "unspecified!";
    doCheckUnitCell = false;
  }

  protected void newAtomSet(String name) {
    if (asc.iSet >= 0) {
      asc.newAtomSet();
      asc.setCollectionName("<collection of " + (asc.iSet + 1) + " models>");
    } else {
      asc.setCollectionName(name);
    }
    asc.setModelInfoForSet("name", name, Math.max(0, asc.iSet));
    asc.setAtomSetName(name);
  }

  protected int cloneLastAtomSet(int ac, P3d[] pts) throws Exception {
    int lastAtomCount = asc.getLastAtomSetAtomCount();
    asc.cloneLastAtomSetFromPoints(ac, pts);
    if (asc.haveUnitCell) {
      iHaveUnitCell = true;
      doCheckUnitCell = true;
      sgName = previousSpaceGroup;
      unitCellParams = previousUnitCell;
    }
    return lastAtomCount;
  }

  public void setSpaceGroupName(String name) {
    if (ignoreFileSpaceGroupName || name == null)
      return;
    String s = name.trim();
    if (s.length() == 0 || s.equals("HM:") || s.equals(sgName))
      return;
    if (!s.equals("P1"))
      Logger.info("Setting space group name to " + s);
    sgName = s;
  }

  public int setSymmetryOperator(String xyz) {
    if (ignoreFileSymmetryOperators)
      return -1;
    int isym = asc.getXSymmetry().addSpaceGroupOperation(xyz, true);
    if (isym < 0)
      warnSkippingOperation(xyz);
    iHaveSymmetryOperators = true;
    return isym;
  }

  protected void warnSkippingOperation(String xyz) {
    Logger.warn("Skipping symmetry operation " + xyz);
  }

  private int nMatrixElements = 0;

  private void initializeCartesianToFractional() {
    for (int i = 0; i < 16; i++)
      if (!Double.isNaN(unitCellParams[SimpleUnitCell.PARAM_M4 + i]))
        return; //just do this once
    // set the matrix to the identity matrix
    for (int i = 0; i < 16; i++)
      unitCellParams[SimpleUnitCell.PARAM_M4 + i] = ((i % 5 == 0 ? 1 : 0));
    nMatrixElements = 0;
  }

  public void clearUnitCell() {
    if (ignoreFileUnitCell)
      return;
    for (int i = SimpleUnitCell.PARAM_STD; i < SimpleUnitCell.PARAM_SUPERCELL; i++)
      unitCellParams[i] = Double.NaN;
    checkUnitCell(SimpleUnitCell.PARAM_STD);
  }

  public double[] ucItems;

  public void setUnitCellItem(int i, double x) {
    if (ignoreFileUnitCell)
      return;
    if (i == 0 && x == 1 && !allow_a_len_1 || i == 3 && x == 0) {
      if (ucItems == null)
        ucItems = new double[SimpleUnitCell.PARAM_STD];
      ucItems[i] = x;
      return;
    }
    if (ucItems != null && i < SimpleUnitCell.PARAM_STD)
      ucItems[i] = x;

    if (!Double.isNaN(x) && i >= SimpleUnitCell.PARAM_M4
        && Double.isNaN(unitCellParams[SimpleUnitCell.PARAM_M4]))
      initializeCartesianToFractional();
    unitCellParams[i] = x;
    if (debugging) {
      Logger.debug("setunitcellitem " + i + " " + x);
    }
    if (i < SimpleUnitCell.PARAM_STD || Double.isNaN(x))
      iHaveUnitCell = checkUnitCell(SimpleUnitCell.PARAM_STD);
    else if (++nMatrixElements == 12)
      iHaveUnitCell = checkUnitCell(SimpleUnitCell.PARAM_M4 + 16);
  }

  protected M3d matUnitCellOrientation;

  public void setUnitCell(double a, double b, double c, double alpha,
                          double beta, double gamma) {
    if (ignoreFileUnitCell)
      return;
    clearUnitCell();
    unitCellParams[SimpleUnitCell.INFO_A] = a;
    unitCellParams[SimpleUnitCell.INFO_B] = b;
    unitCellParams[SimpleUnitCell.INFO_C] = c;
    if (alpha != 0)
      unitCellParams[SimpleUnitCell.INFO_ALPHA] = alpha;
    if (beta != 0)
      unitCellParams[SimpleUnitCell.INFO_BETA] = beta;
    if (gamma != 0)
      unitCellParams[SimpleUnitCell.INFO_GAMMA] = gamma;
    iHaveUnitCell = checkUnitCell(SimpleUnitCell.PARAM_STD);
  }

  public void addExplicitLatticeVector(int i, double[] xyz, int i0) {
    if (ignoreFileUnitCell)
      return;
    if (i == 0)
      for (int j = 0; j < SimpleUnitCell.PARAM_STD; j++)
        unitCellParams[j] = 0;
    i = 6 + i * 3;
    unitCellParams[i++] = xyz[i0++];
    unitCellParams[i++] = xyz[i0++];
    unitCellParams[i] = xyz[i0];
    if (Double.isNaN(unitCellParams[0])) {
      for (i = 0; i < SimpleUnitCell.PARAM_STD; i++)
        unitCellParams[i] = -1;
    }
    iHaveUnitCell = checkUnitCell(SimpleUnitCell.PARAM_VAX + 9);
    if (iHaveUnitCell) {
      if (slabXY || polymerX)
        unitCellParams[SimpleUnitCell.INFO_C] = -1;
      if (polymerX)
        unitCellParams[SimpleUnitCell.INFO_B] = -1;
    }
  }

  private boolean checkUnitCell(int n) {
    for (int i = 0; i < n; i++) {
      if (Double.isNaN(unitCellParams[i]))
        return false;
    }
    if (n == SimpleUnitCell.PARAM_M4 + 16 && unitCellParams[0] == 1) {
      if (unitCellParams[1] == 1 && unitCellParams[2] == 1
          && unitCellParams[SimpleUnitCell.PARAM_M4] == 1
          && unitCellParams[SimpleUnitCell.PARAM_M4 + 5] == 1
          && unitCellParams[SimpleUnitCell.PARAM_M4 + 10] == 1) {
        // this is an mmCIF or PDB case for NMR models having
        // CRYST1    1.000    1.000    1.000  90.00  90.00  90.00 P 1           1          
        // ORIGX1      1.000000  0.000000  0.000000        0.00000                         
        // ORIGX2      0.000000  1.000000  0.000000        0.00000                         
        // ORIGX3      0.000000  0.000000  1.000000        0.00000                         
        // SCALE1      1.000000  0.000000  0.000000        0.00000                         
        // SCALE2      0.000000  1.000000  0.000000        0.00000                         
        // SCALE3      0.000000  0.000000  1.000000        0.00000 
        return false;
      }
    }
    if (n == SimpleUnitCell.PARAM_STD
        && Double.isNaN(unitCellParams[SimpleUnitCell.PARAM_VAX])) {
      if (slabXY && unitCellParams[SimpleUnitCell.INFO_C] > 0) {
        SimpleUnitCell.addVectors(unitCellParams);
        unitCellParams[SimpleUnitCell.INFO_C] = -1;
      } else if (polymerX && unitCellParams[SimpleUnitCell.INFO_B] > 0) {
        SimpleUnitCell.addVectors(unitCellParams);
        unitCellParams[SimpleUnitCell.INFO_B] = unitCellParams[SimpleUnitCell.INFO_C] = -1;
      }
    }
    if (doApplySymmetry) {
      getSymmetry();
      doConvertToFractional = !fileCoordinatesAreFractional;
    }
    //if (but not only if) applying symmetry do we force conversion
    //    checkUnitCellOffset();
    return true;
  }

  public FileSymmetry getSymmetry() {
    if (!iHaveUnitCell)
      return null;
    if (symmetry == null) {
      (symmetry = asc.newFileSymmetry()).setUnitCellFromParams(unitCellParams, false, cellSlop);
      checkUnitCellOffset();
    }
    if (symmetry == null) // cif file with no symmetry triggers exception on LOAD {1 1 1}
      iHaveUnitCell = false;
    else
      symmetry.setSpaceGroupName(sgName);
    return symmetry;
  }

  private void checkUnitCellOffset() {
    if (fileOffsetFractional == null || symmetry == null)
      return;
    fileOffset.setT(fileOffsetFractional);
    if (unitCellOffsetFractional != fileCoordinatesAreFractional) {
      if (unitCellOffsetFractional)
        symmetry.toCartesian(fileOffset, false);
      else
        symmetry.toFractional(fileOffset, false);
    }
  }

  protected void fractionalizeCoordinates(boolean toFrac) {
    if (getSymmetry() == null)
      return;
    Atom[] a = asc.atoms;
    if (toFrac)
      for (int i = asc.ac; --i >= 0;)
        symmetry.toFractional(a[i], false);
    else
      for (int i = asc.ac; --i >= 0;)
        symmetry.toCartesian(a[i], false);
    setFractionalCoordinates(toFrac);
  }

  public void setFractionalCoordinates(boolean TF) {
    iHaveFractionalCoordinates = fileCoordinatesAreFractional = TF;
    checkUnitCellOffset();
  }

  /////////// FILTER /////////////////

  protected BS bsFilter;
  public String filter, filterCased;
  public boolean haveAtomFilter;
  private boolean filterAltLoc;
  private boolean filterGroup3;
  private boolean filterChain;
  private boolean filterAtomName;
  private boolean filterAtomType;
  private String filterAtomTypeStr;
  private String filterAtomNameTerminator = ";";
  private boolean filterElement;
  protected boolean filterHetero;
  protected boolean filterAllHetero;
  private boolean filterEveryNth;
  String filterSymop;
  private int filterN;
  private int nFiltered;
  private boolean doSetOrientation;
  protected boolean doCentralize;
  protected boolean addVibrations;
  protected boolean useAltNames;
  protected boolean ignoreStructure;
  protected boolean isDSSP1;
  protected boolean allowPDBFilter;
  public boolean doReadMolecularOrbitals;
  protected boolean reverseModels;
  private String nameRequired;
  public boolean doCentroidUnitCell;
  public boolean centroidPacked;
  public String strSupercell;

  public boolean allow_a_len_1 = false;

  public boolean slabXY;

  private boolean polymerX;

  boolean fixUnitCell;

  protected boolean filteredPrecision;

  // xtal structures -- SLAB

  // ALL:  "CENTER" "REVERSEMODELS"
  // ALL:  "SYMOP=n"
  // MANY: "NOVIB" "NOMO"
  // CASTEP: "CHARGE=HIRSH q={i,j,k};"
  // CIF: "ASSEMBLY n" 
  // CRYSTAL: "CONV" (conventional), "INPUT"
  // CSF, SPARTAN: "NOORIENT"
  // GAMESS-US:  "CHARGE=LOW"
  // JME, MOL: "NOMIN"
  // MOL:  "2D"
  // Molden: "INPUT" "GEOM" "NOGEOM"
  // MopacArchive: "NOCENTER"
  // MOReaders: "NBOCHARGES"
  // P2N: "ALTNAME"
  // PDB: "BIOMOLECULE n;" "NOSYMMETRY"  "CONF n"
  // Spartan: "INPUT", "ESPCHARGES"
  // 

  protected void setFilterAtomTypeStr(String s) {
    // PDB reader TYPE=...
    filterAtomTypeStr = s;
    filterAtomNameTerminator = "\0";
  }

  protected void setFilter(String filter0) {
    if (filter0 == null) {
      filter0 = (String) htParams.get("filter");
    } else {
      // from PDB REMARK350()
      bsFilter = null;
      filterCased = null;
    }
    if (filterCased == null)
      filterCased = (filter0 == null ? null : filter0 + ";");
    if (filter0 != null)
      filter0 = filter0.toUpperCase();
    filter = filter0;
    doSetOrientation = !checkFilterKey("NOORIENT");
    doCentralize = (!checkFilterKey("NOCENTER") && checkFilterKey("CENTER"));
    addVibrations = !checkFilterKey("NOVIB");
    ignoreStructure = checkFilterKey("DSSP");
    isDSSP1 = checkFilterKey("DSSP1");
    doReadMolecularOrbitals = !checkFilterKey("NOMO");
    useAltNames = checkFilterKey("ALTNAME");
    reverseModels = checkFilterKey("REVERSEMODELS");
    allow_a_len_1 = checkFilterKey("TOPOS");
    slabXY = checkFilterKey("SLABXY");
    polymerX = !slabXY && checkFilterKey("POLYMERX");
    noHydrogens = checkFilterKey("NOH");
    noMinimize = checkFilterKey("NOMIN");
    optimize2D = checkFilterKey("2D") && !noHydrogens && !noMinimize;

    if (filter == null)
      return;

    fixUnitCell = checkFilterKey("FIXUNITCELL");
    if (checkFilterKey("LOWPRECISION")) {
      // adding filter "lowPrecision" overrides the CIF and PWMAT reader HIGH setting
      setLowPrecision();
    }
    if (checkFilterKey("HETATM")) {
      filterHetero = true;
      filter = PT.rep(filter, "HETATM", "HETATM-Y");
      filterCased = PT.rep(filterCased, "HETATM", "HETATM-Y");
    }
    if (checkFilterKey("ATOM")) {
      filterHetero = true;
      filter = PT.rep(filter, "ATOM", "HETATM-N");
      filterCased = PT.rep(filterCased, "ATOM", "HETATM-N");
    }

    // can't use getFilter() here because form includes a semicolon:
    // cell=a+b,a-b,c;0,1/2,1/2 
    // also allows for NOPACKCELL by documentation 14.0
    if (checkFilterKey("CELL="))
      strSupercell = filter.substring(filter.indexOf("CELL=") + 5)
          .toLowerCase(); // must be last filter option
    nameRequired = PT.getQuotedAttribute(filter, "NAME");
    if (nameRequired != null) {
      if (nameRequired.startsWith("'"))
        nameRequired = PT.split(nameRequired, "'")[1];
      else if (nameRequired.startsWith("\""))
        nameRequired = PT.split(nameRequired, "\"")[1];
      filter = PT.rep(filter, nameRequired, "");
      filter0 = filter = PT.rep(filter, "NAME=", "");
    }
    filterAtomName = checkFilterKey("*.") || checkFilterKey("!.");
    if (filter.startsWith("_") || filter.startsWith("!_")
        || filter.indexOf(";_") >= 0)
      filterElement = checkFilterKey("_");

    filterGroup3 = checkFilterKey("[");
    filterChain = checkFilterKey(":");
    filterAltLoc = checkFilterKey("%");
    filterEveryNth = checkFilterKey("/=");
    filterAllHetero = checkFilterKey("ALLHET");
    if (filterEveryNth)
      filterN = parseIntAt(filter, filter.indexOf("/=") + 2);
    else if (filter.startsWith("=") || filter.indexOf(";=") >= 0)
      filterAtomType = checkFilterKey("=");
    if (filterN == Integer.MIN_VALUE)
      filterEveryNth = false;
    haveAtomFilter = filterAtomName || filterAtomType || filterElement
        || filterGroup3 || filterChain || filterAltLoc || filterHetero
        || filterEveryNth || checkFilterKey("/=");
    if (bsFilter == null) {
      // bsFilter is usually null, but from MDTOP it gets set to indicate
      // which atoms were selected by the filter. This then
      // gets used by COORD files to load just those coordinates
      // and it returns the bitset of filtered atoms
      bsFilter = new BS();
      htParams.put("bsFilter", bsFilter);
      filter = (";" + filter + ";").replace(',', ';');
      String p = getFilter("PRECISION=");
      if (p != null) {
        int prec = PT.parseInt(p);
        if (prec > 0 && prec <= 16) {
          precision = 1000 + prec;
          filteredPrecision = true;
        }
      }
      String s = getFilter("LATTICESCALING=");
      if (s != null && unitCellParams.length > SimpleUnitCell.PARAM_SCALE)
        unitCellParams[SimpleUnitCell.PARAM_SCALE] = latticeScaling = parseDoubleStr(
            s);
      s = getFilter("SYMOP=");
      if (s != null)
        filterSymop = " " + s + " ";
      Logger.info("filtering with " + filter);
      if (haveAtomFilter) {
        int ipt;
        filter1Cased = filterCased;
        filter2Cased = "";
        if ((ipt = filter.indexOf("|")) >= 0) {
          filter1Cased = filter.substring(0, ipt).trim() + ";";
          filter2Cased = ";" + filter.substring(ipt).trim();
        }
        filter1 = filter1Cased.toUpperCase();
        filter2 = (filter2Cased.length() == 0 ? null
            : filter2Cased.toUpperCase());
      }
    }
  }

  private String filter1, filter2, filter1Cased, filter2Cased;

  public String getFilterWithCase(String key) {
    int pt = (filterCased == null ? -1
        : filterCased.toUpperCase().indexOf(key.toUpperCase()));
    return (pt < 0 ? null
        : filterCased.substring(pt + key.length(),
            filterCased.indexOf(";", pt)));
  }

  public String getFilter(String key) {
    int pt = (filter == null ? -1 : filter.indexOf(key));
    return (pt < 0 ? null
        : filter.substring(pt + key.length(), filter.indexOf(";", pt)));
  }

  public boolean checkFilterKey(String key) {
    return (filter != null && filter.indexOf(key) >= 0);
  }

  /**
   * @param key
   * @return true if the key existed; filter is set null if this is the only key
   * 
   */

  public boolean checkAndRemoveFilterKey(String key) {
    if (!checkFilterKey(key))
      return false;
    filter = PT.rep(filter, key, "");
    // allows for "!" and ";" 
    if (filter.length() < 3)
      filter = null;
    return true;
  }

  /**
   * @param atom
   * @param iAtom
   * @return true if we want this atom
   */
  protected boolean filterAtom(Atom atom, int iAtom) {
    if (!haveAtomFilter)
      return true;
    // cif, mdtop, pdb, gromacs, pqr
    boolean isOK = checkFilter(atom, filter1, filter1Cased);
    if (filter2 != null)
      isOK |= checkFilter(atom, filter2, filter2Cased);
    if (isOK && filterEveryNth && (!atom.isHetero || !filterAllHetero))
      isOK = (((nFiltered++) % filterN) == 0);
    bsFilter.setBitTo(iAtom >= 0 ? iAtom : asc.ac, isOK);
    return isOK;
  }

  /**
   * 
   * @param atom
   * @param f
   * @param fCased
   * @return true if a filter is found
   */
  private boolean checkFilter(Atom atom, String f, String fCased) {
    if (atom.isHetero && filterAllHetero)
      return true;
    return (!filterGroup3 || atom.group3 == null
        || !filterReject(f, "[", atom.group3.toUpperCase() + "]"))
        && (!filterAtomName || allowAtomName(atom.atomName, f))
        && (filterAtomTypeStr == null || atom.atomName == null
            || atom.atomName.toUpperCase()
                .indexOf("\0" + filterAtomTypeStr) >= 0)
        && (!filterElement || atom.elementSymbol == null
            || !filterReject(f, "_", atom.elementSymbol.toUpperCase() + ";"))
        && (!filterChain || atom.chainID == 0
            || !filterReject(fCased, ":", "" + vwr.getChainIDStr(atom.chainID)))
        && (!filterAltLoc || atom.altLoc == '\0'
            || !filterReject(f, "%", "" + atom.altLoc))
        && (!filterHetero || !allowPDBFilter
            || !filterReject(f, "HETATM", atom.isHetero ? "-Y" : "-N"));
  }

  public boolean rejectAtomName(String name) {
    return filterAtomName && !allowAtomName(name, filter);
  }

  private boolean allowAtomName(String atomName, String f) {
    return (atomName == null || !filterReject(f, ".",
        atomName.toUpperCase() + filterAtomNameTerminator));
  }

  protected boolean filterReject(String f, String code, String atomCode) {
    return (f.indexOf(code) >= 0
        && (f.indexOf("!" + code) >= 0) == (f.indexOf(code + atomCode) >= 0));
  }

  protected void set2D() {
    // MOL and JME - sets for ALL MODELS, but just for ModelLoader.
    // It is quite possible that multiple 2D models cannot be loaded as 3D
    asc.setInfo("is2D", Boolean.TRUE);
    asc.getBSAtoms(-1);
    if (noHydrogens) {
      asc.setInfo("noHydrogen", Boolean.TRUE);
      optimize2D = false;
    }
    if (optimize2D) {
      asc.fix2Stereo();
      asc.setInfo("doMinimize", Boolean.TRUE);
      appendLoadNote("This model is 2D. Its 3D structure was generated.");
    } else {
      appendLoadNote(
          "This model is 2D. Its 3D structure has not been generated; use LOAD \"\" FILTER \"2D\" to optimize 3D.");
      addJmolScript("select thismodel;wireframe only");
    }
  }

  public boolean doGetVibration(int vibrationNumber) {
    // vibrationNumber is 1-based
    return addVibrations && (desiredVibrationNumber <= 0
        || vibrationNumber == desiredVibrationNumber);
  }

  private M3d matRot;

  public MSInterface ms;

  public void setTransform(double x1, double y1, double z1, double x2,
                           double y2, double z2, double x3, double y3,
                           double z3) {
    if (matRot != null || !doSetOrientation)
      return;
    matRot = new M3d();
    V3d v = V3d.new3(x1, y1, z1);
    // rows in Sygress/CAChe and Spartan become columns here
    v.normalize();
    matRot.setColumnV(0, v);
    v.set(x2, y2, z2);
    v.normalize();
    matRot.setColumnV(1, v);
    v.set(x3, y3, z3);
    v.normalize();
    matRot.setColumnV(2, v);
    asc.setInfo("defaultOrientationMatrix", M3d.newM3(matRot));
    // first two matrix column vectors define quaternion X and XY plane
    Qd q = Qd.newM(matRot);
    asc.setInfo("defaultOrientationQuaternion", q);
    Logger.info("defaultOrientationMatrix = " + matRot);

  }

  /////////////////////////////

  public void setAtomCoordXYZ(Atom atom, double x, double y, double z) {
    atom.set(x, y, z);
    setAtomCoord(atom);
  }

  public Atom setAtomCoordScaled(Atom atom, String[] tokens, int i, double f) {
    if (atom == null)
      atom = asc.addNewAtom();
    setAtomCoordXYZ(atom, parsePrecision(tokens[i]) * f,
        parsePrecision(tokens[i + 1]) * f, parsePrecision(tokens[i + 2]) * f);
    return atom;
  }

  protected void setAtomCoordTokens(Atom atom, String[] tokens, int i) {
    setAtomCoordXYZ(atom, parsePrecision(tokens[i]),
        parsePrecision(tokens[i + 1]), parsePrecision(tokens[i + 2]));
  }

  public Atom addAtomXYZSymName(String[] tokens, int i, String sym,
                                String name) {
    Atom atom = asc.addNewAtom();
    if (sym != null)
      atom.elementSymbol = sym;
    if (name != null)
      atom.atomName = name;
    setAtomCoordTokens(atom, tokens, i);
    return atom;
  }

  public void setAtomCoord(Atom atom) {
    // fileScaling is used by the PLOT command to 
    // put data into PDB format, preserving name/residue information,
    // and still get any xyz data into the allotted column space.
    boolean mustFractionalize = (doConvertToFractional
        && !fileCoordinatesAreFractional && getSymmetry() != null);
    if (fileScaling != null) {
      atom.x = atom.x * fileScaling.x + fileOffset.x;
      atom.y = atom.y * fileScaling.y + fileOffset.y;
      atom.z = atom.z * fileScaling.z + fileOffset.z;
    }
    if (mustFractionalize) {
      if (!symmetry.haveUnitCell())
        symmetry.setUnitCellFromParams(unitCellParams, false, Double.NaN);
      symmetry.toFractional(atom, false);
      iHaveFractionalCoordinates = true;
    }
    //    if (fixJavaDouble && fileCoordinatesAreFractional) {
    //      PT.fixPtDoubles(atom, PT.FRACTIONAL_PRECISION);
    //    }
    doCheckUnitCell = true;
  }

  public void addSites(Map<String, Map<String, Object>> htSites) {
    asc.setCurrentModelInfo("pdbSites", htSites);
    String sites = "";
    for (Map.Entry<String, Map<String, Object>> entry : htSites.entrySet()) {
      String name = entry.getKey();
      Map<String, Object> htSite = entry.getValue();
      char ch;
      for (int i = name.length(); --i >= 0;)
        if (!PT.isLetterOrDigit(ch = name.charAt(i)) && ch != '\'')
          name = name.substring(0, i) + "_" + name.substring(i + 1);
      String groups = (String) htSite.get("groups");
      if (groups.length() == 0)
        continue;
      addSiteScript("@site_" + name + " " + groups);
      addSiteScript(
          "site_" + name + " = [\"" + PT.rep(groups, ",", "\",\"") + "\"]");
      sites += ",\"site_" + name + "\"";
    }
    if (sites.length() > 0)
      addSiteScript("site_list = [" + sites.substring(1) + "]");
  }

  public void applySymmetryAndSetTrajectory() throws Exception {
    // overridden in many readers
    applySymTrajASCR();
  }

  public boolean vibsFractional = false;

  public FileSymmetry applySymTrajASCR() throws Exception {
    if (forcePacked)
      initializeSymmetryOptions();
    boolean doApply = (iHaveUnitCell && doCheckUnitCell);
    FileSymmetry sym = null;
    //    int n = asc.getLastAtomSetAtomIndex();
    //    int n1 = asc.ac;
    if (doApply) {
      sym = getSymmetry();
      setPrecision();
      //      for (int i = n1; --i >= n;) {
      //        System.out.println(P3d.newP(asc.atoms[i]));
      //      }
      sym = asc.getXSymmetry().applySymmetryFromReader(sym);
    } else {
      asc.setTensors();
    }
    //    for (int i = n1; --i >= n;) {
    //      System.out.println(asc.atoms[i].atomName + " " + P3d.newP(asc.atoms[i]));
    //    }

    if (isTrajectory)
      asc.setTrajectory();
    if (moreUnitCellInfo != null) {
      setMoreInfo();
      moreUnitCellInfo = null;
    }
    finalizeSubclassSymmetry(sym != null);

    if (merging && sym != null && iHaveFractionalCoordinates && iHaveUnitCell
        && iHaveSymmetryOperators) {
      // only do this if XtalSymmetry has not done it already
      fractionalizeCoordinates(false);
      addJmolScript("modelkit spacegroup P1");
    }
    initializeSymmetry();
    return sym;
  }

  private void setMoreInfo() {
    asc.setCurrentModelInfo(JC.UC_MOREINFO, moreUnitCellInfo);
    for (int i = moreUnitCellInfo.size(); --i >= 0;) {
      String[] s = moreUnitCellInfo.get(i).split("=");
      asc.setCurrentModelInfo(s[0], s[1]);
    }
  }

  /**
   * @param haveSymmetry
   * @throws Exception
   */
  protected void finalizeSubclassSymmetry(boolean haveSymmetry)
      throws Exception {
  }

  protected void doPreSymmetry(boolean doApplySymmetry) throws Exception {
  }

  @SuppressWarnings("unchecked")
  public void finalizeMOData(Map<String, Object> moData) {
    asc.setCurrentModelInfo("moData", moData);
    if (moData == null)
      return;
    Lst<Map<String, Object>> orbitals = (Lst<Map<String, Object>>) moData
        .get("mos");
    if (orbitals != null)
      Logger.info(orbitals.size() + " molecular orbitals read in model "
          + asc.atomSetCount);
  }

  public static String getElementSymbol(int elementNumber) {
    return JmolAdapter.getElementSymbol(elementNumber);
  }

  /**
   * fills an array with a pre-defined number of lines of token data, skipping
   * blank lines in the process
   * 
   * @param data
   * @param minLineLen
   * @throws Exception
   */
  protected void fillDataBlock(String[][] data, int minLineLen)
      throws Exception {
    int nLines = data.length;
    for (int i = 0; i < nLines; i++) {
      data[i] = PT.getTokens(discardLinesUntilNonBlank());
      if (data[i].length < minLineLen)
        --i;
    }

  }

  /**
   * fills a double[3][3]
   * 
   * @param tokens
   *        or null if to read each line for three values (as last 3 on line)
   * @param pt
   *        initial index; if tokens == null, then negative index is from end of
   *        each line
   * @return double[3][3]
   * @throws Exception
   */
  protected double[][] fill3x3(String[] tokens, int pt) throws Exception {
    double[][] a = new double[3][3];
    boolean needTokens = (tokens == null);
    int pt0 = pt;
    for (int i = 0; i < 3; i++) {
      if (needTokens || pt >= tokens.length) {
        while ((tokens = PT.getTokens(rd())).length < 3) {
        }
        pt = (pt0 < 0 ? tokens.length + pt0 : pt0);
      }
      for (int j = 0; j < 3; j++)
        a[i][j] = Double.valueOf(tokens[pt++]).doubleValue();
    }
    return a;
  }

  /**
   * fills a double array with string data from a file
   * 
   * @param s
   *        string data containing doubles
   * @param width
   *        column width or 0 to read tokens
   * @param data
   *        result data to be filled
   * @return data
   * @throws Exception
   */
  protected double[] fillDoubleArray(String s, int width, double[] data)
      throws Exception {
    String[] tokens = new String[0];
    int pt = 0;
    for (int i = 0; i < data.length; i++) {
      while (tokens != null && pt >= tokens.length) {
        if (s == null)
          s = rd();
        if (width == 0) {
          tokens = PT.getTokens(s);
        } else {
          tokens = new String[s.length() / width];
          for (int j = 0; j < tokens.length; j++)
            tokens[j] = s.substring(j * width, (j + 1) * width);
        }
        s = null;
        pt = 0;
      }
      if (tokens == null)
        break;
      data[i] = parseDoubleStr(tokens[pt++]);
    }
    return data;
  }

  /**
   * Extracts a block of frequency data from a file. This block may be of two
   * types -- either X Y Z across a row or each of X Y Z on a separate line.
   * Data is presumed to be in fixed FORTRAN-like column format, not
   * space-separated columns.
   * 
   * @param iAtom0
   *        the first atom to be assigned a frequency
   * @param ac
   *        the number of atoms to be assigned
   * @param modelAtomCount
   *        the number of atoms in each model
   * @param ignore
   *        the frequencies to ignore because the user has selected only certain
   *        vibrations to be read or for whatever reason; length serves to set
   *        the number of frequencies to be read
   * @param isWide
   *        when TRUE, this is a table that has X Y Z for each mode within the
   *        same row; when FALSE, this is a table that has X Y Z for each mode
   *        on a separate line.
   * @param col0
   *        the column in which data starts
   * @param colWidth
   *        the width of the data columns
   * @param atomIndexes
   *        an array either null or indicating exactly which atoms get the
   *        frequencies (used by CrystalReader)
   * @param minLineLen
   * @param data
   * @throws Exception
   */
  protected void fillFrequencyData(int iAtom0, int ac, int modelAtomCount,
                                   boolean[] ignore, boolean isWide, int col0,
                                   int colWidth, int[] atomIndexes,
                                   int minLineLen, String[][] data)
      throws Exception {
    boolean withSymmetry = (ac != 0 && modelAtomCount != ac && data == null);
    if (ac == 0 && atomIndexes != null)
      ac = atomIndexes.length;
    int nLines = (isWide ? ac : ac * 3);
    int nFreq = ignore.length;
    if (data == null) {
      data = new String[nLines][];
      fillDataBlockFixed(data, col0, colWidth, minLineLen);
    } else if (!isWide) {
      // Gaussian high precision - get atom index at ptNonblank + 1
      int ptNonblank = minLineLen;
      fillDataBlockFixed(data, col0, colWidth, -ptNonblank);
      if (data[0] == null)
        return;
      iAtom0 += parseIntAt(line, ptNonblank - 5) - 1;
    }
    for (int i = 0, atomPt = 0; i < nLines; i++, atomPt++) {
      String[] values = data[i];
      String[] valuesY = (isWide ? null : data[++i]);
      String[] valuesZ = (isWide ? null : data[++i]);
      int dataPt = values.length - (isWide ? nFreq * 3 : nFreq) - 1;
      for (int j = 0, jj = 0; jj < nFreq; jj++) {
        ++dataPt;
        String x = values[dataPt];
        if (x.charAt(0) == ')') // AMPAC reader!
          x = x.substring(1);
        double vx = parseDoubleStr(x);
        double vy = parseDoubleStr(isWide ? values[++dataPt] : valuesY[dataPt]);
        double vz = parseDoubleStr(isWide ? values[++dataPt] : valuesZ[dataPt]);
        if (ignore[jj])
          continue;
        int iAtom = (atomIndexes == null ? atomPt : atomIndexes[atomPt]);
        if (iAtom < 0)
          continue;
        iAtom += iAtom0 + modelAtomCount * j++;
        if (debugging)
          Logger.debug(
              "atom " + iAtom + " vib" + j + ": " + vx + " " + vy + " " + vz);
        asc.addVibrationVectorWithSymmetry(iAtom, vx, vy, vz, withSymmetry);
      }
    }
  }

  /**
   * Fills an array with a predefined number of lines of data that is arranged
   * in fixed FORTRAN-like column format.
   * 
   * Used exclusively for frequency data
   * 
   * @param data
   * @param col0
   * @param colWidth
   * @param minLineLen
   *        or -ptNonblank
   * @throws Exception
   */
  protected void fillDataBlockFixed(String[][] data, int col0, int colWidth,
                                    int minLineLen)
      throws Exception {
    if (colWidth == 0) {
      fillDataBlock(data, minLineLen);
      return;
    }
    int nLines = data.length;
    for (int i = 0; i < nLines; i++) {
      discardLinesUntilNonBlank();
      // neg minLineLen is a nonblank pt
      if (minLineLen < 0 && line.charAt(-minLineLen) == ' ') {
        data[0] = null;
        return;
      }
      int nFields = (line.length() - col0 + 1) / colWidth; // Dmol reader is one short
      data[i] = new String[nFields];
      for (int j = 0, start = col0; j < nFields; j++, start += colWidth)
        data[i][j] = line.substring(start,
            Math.min(line.length(), start + colWidth));
    }
  }

  protected String readLines(int nLines) throws Exception {
    for (int i = nLines; --i >= 0;)
      rd();
    return line;
  }

  public String discardLinesUntilStartsWith(String startsWith)
      throws Exception {
    while (rd() != null && !line.startsWith(startsWith)) {
    }
    return line;
  }

  public String discardLinesUntilContains(String containsMatch)
      throws Exception {
    while (rd() != null && line.indexOf(containsMatch) < 0) {
    }
    return line;
  }

  public String discardLinesUntilContains2(String s1, String s2)
      throws Exception {
    while (rd() != null && line.indexOf(s1) < 0 && line.indexOf(s2) < 0) {
    }
    return line;
  }

  public String discardLinesUntilBlank() throws Exception {
    while (rd() != null && line.trim().length() != 0) {
    }
    return line;
  }

  public String discardLinesUntilNonBlank() throws Exception {
    while (rd() != null && line.trim().length() == 0) {
    }
    return line;
  }

  protected void checkLineForScript(String line) {
    this.line = line;
    checkCurrentLineForScript();
  }

  public void checkCurrentLineForScript() {
    if (line.endsWith("#noautobond")) {
      line = line.substring(0, line.lastIndexOf('#')).trim();
      asc.setNoAutoBond();
    }
    int pt = line.indexOf("jmolscript:");
    if (pt >= 0) {
      String script = line.substring(pt + 11, line.length());
      if (script.indexOf("#") >= 0) {
        script = script.substring(0, script.indexOf("#"));
      }
      addJmolScript(script);
      line = line.substring(0, pt).trim();
    }
  }

  private String previousScript;

  public void addJmolScript(String script) {
    Logger.info("#jmolScript: " + script);
    if (previousScript == null)
      previousScript = "";
    else if (!previousScript.endsWith(";"))
      previousScript += ";";
    previousScript += script;
    asc.setInfo("jmolscript", previousScript);
  }

  private String siteScript;

  protected void addSiteScript(String script) {
    if (siteScript == null)
      siteScript = "";
    else if (!siteScript.endsWith(";"))
      siteScript += ";";
    siteScript += script;
    asc.setInfo("sitescript", siteScript); // checked in ScriptEvaluator.load()
  }

  public String rd() throws Exception {
    return RL();
  }

  public String RL() throws Exception {
    prevline = line;
    line = reader.readLine();
    if (out != null && line != null)
      out.append(line).append("\n");
    ptLine++;
    if (debugging && line != null)
      Logger.info(line);
    return line;
  }

  final static protected String[] getStrings(String sinfo, int nFields,
                                             int width) {
    String[] fields = new String[nFields];
    for (int i = 0, pt = 0; i < nFields; i++, pt += width)
      fields[i] = sinfo.substring(pt, pt + width);
    return fields;
  }

  // parser functions are static, so they need notstatic counterparts

  public String[] getTokens() {
    return PT.getTokens(line);
  }

  public static double[] getTokensDouble(String s, double[] f, int n) {
    if (f == null)
      f = new double[n];
    PT.parseDoubleArrayDataN(PT.getTokens(s), f, n);
    return f;
  }

  protected double parseDouble() {
    return PT.parseDoubleNext(line, next);
  }

  public double parseDoubleStr(String s) {
    next[0] = 0;
    return PT.parseDoubleNext(s, next);
  }

  protected double parseDoubleRange(String s, int iStart, int iEnd) {
    next[0] = iStart;
    return PT.parseDoubleRange(s, iEnd, next);
  }

  protected int parseInt() {
    return PT.parseIntNext(line, next);
  }

  public int parseIntStr(String s) {
    next[0] = 0;
    return PT.parseIntNext(s, next);
  }

  public int parseIntAt(String s, int iStart) {
    next[0] = iStart;
    return PT.parseIntNext(s, next);
  }

  protected int parseIntRange(String s, int iStart, int iEnd) {
    next[0] = iStart;
    return PT.parseIntRange(s, iEnd, next);
  }

  protected String parseToken() {
    return PT.parseTokenNext(line, next);
  }

  protected String parseTokenStr(String s) {
    next[0] = 0;
    return PT.parseTokenNext(s, next);
  }

  protected String parseTokenNext(String s) {
    return PT.parseTokenNext(s, next);
  }

  protected String parseTokenRange(String s, int iStart, int iEnd) {
    next[0] = iStart;
    return PT.parseTokenRange(s, iEnd, next);
  }

  /**
   * get all integers after letters negative entries are spaces (1Xn)
   * 
   * @param s
   * @return Vector of integers
   */
  protected static Lst<Integer> getFortranFormatLengths(String s) {
    Lst<Integer> vdata = new Lst<Integer>();
    int n = 0;
    int c = 0;
    int factor = 1;
    boolean inN = false;
    boolean inCount = true;
    s += ",";
    for (int i = 0; i < s.length(); i++) {
      char ch = s.charAt(i);
      switch (ch) {
      case '.':
        inN = false;
        continue;
      case ',':
        for (int j = 0; j < c; j++)
          vdata.addLast(Integer.valueOf(n * factor));
        inN = false;
        inCount = true;
        c = 0;
        continue;
      case 'X':
        n = c;
        c = 1;
        factor = -1;
        continue;

      }
      boolean isDigit = PT.isDigit(ch);
      if (isDigit) {
        if (inN)
          n = n * 10 + ch - '0';
        else if (inCount)
          c = c * 10 + ch - '0';
      } else if (PT.isLetter(ch)) {
        n = 0;
        inN = true;
        inCount = false;
        factor = 1;
      } else {
        inN = false;
      }
    }
    return vdata;
  }

  /**
   * read three vectors, as for unit cube definitions allows for non-numeric
   * data preceding the number block
   * 
   * @param isBohr
   * @return three vectors
   * @throws Exception
   * 
   */
  protected V3d[] read3Vectors(boolean isBohr) throws Exception {
    V3d[] vectors = new V3d[3];
    double[] f = new double[3];
    for (int i = 0; i < 3; i++) {
      if (i > 0 || Double.isNaN(parseDoubleStr(line))) {
        rd();
        if (i == 0 && line != null) {
          i = -1;
          continue;
        }
      }
      fillDoubleArray(line, 0, f);
      vectors[i] = V3d.new3(f[0], f[1], f[2]);
      if (isBohr)
        vectors[i].scale(ANGSTROMS_PER_BOHR);
    }
    return vectors;
  }

  /**
   * allow 13C, 15N, 2H, etc. for isotopes
   * 
   * @param atom
   * @param str
   */
  protected void setElementAndIsotope(Atom atom, String str) {
    int isotope = parseIntStr(str);
    if (isotope == Integer.MIN_VALUE) {
      atom.elementSymbol = str;
    } else {
      str = str.substring(("" + isotope).length());
      atom.elementNumber = (short) (str.length() == 0 ? isotope
          : ((isotope << 7) + JmolAdapter.getElementNumber(str)));
    }
  }

  public void finalizeModelSet() {
    // PyMOL reader only
  }

  public void setChainID(Atom atom, String label) {
    atom.chainID = vwr.getChainID(label, true);
  }

  @Override
  public String readNextLine() throws Exception {
    // from CifDataReader, DSSRDataReader
    if (rd() != null && line.indexOf("#jmolscript:") >= 0)
      checkCurrentLineForScript();
    return line;
  }

  public void addMoreUnitCellInfo(String info) {
    if (moreUnitCellInfo == null)
      moreUnitCellInfo = new Lst<String>();
    moreUnitCellInfo.addLast(info);
    //appendLoadNote(info);
  }

  public Object getInterface(String className) {
    Object o = Interface.getInterface(className, vwr, "file");
    if (o == null)
      throw new NullPointerException("Interface");
    return o;
  }

  public void forceSymmetry(boolean andPack) {
    if (andPack)
      doPackUnitCell = andPack;
    if (!doApplySymmetry) {
      doApplySymmetry = true;
      latticeCells[0] = latticeCells[1] = latticeCells[2] = 1;
    }
  }
}
