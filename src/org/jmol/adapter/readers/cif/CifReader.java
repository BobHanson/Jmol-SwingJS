/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-20 07:48:25 -0500 (Fri, 20 Oct 2006) $
 * $Revision: 5991 $
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
package org.jmol.adapter.readers.cif;

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.XtalSymmetry.FileSymmetry;
import org.jmol.api.JmolAdapter;
import org.jmol.script.T;
import org.jmol.symmetry.SymmetryOperation;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.SimpleUnitCell;
import org.jmol.util.Vibration;
import org.jmol.viewer.JC;

import javajs.api.GenericCifDataParser;
import javajs.util.BS;
import javajs.util.CifDataParser;
import javajs.util.Lst;
import javajs.util.M3d;
import javajs.util.M4d;
import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.Rdr;
import javajs.util.V3d;

/**
 * A true line-free CIF file reader for CIF files.
 * 
 * Subclasses of CIF -- mmCIF/PDBx (pre-initialized) and msCIF (initialized
 * here)
 *
 * Note that a file can be a PDB file without being
 * 
 * Added nonstandard mCIF (magnetic_ tags) 5/2/2014 note that PRELIM keys can be
 * removed at some later time
 * 
 * Added preliminary Spin-Frame "ssg" tags2025.02.07
 * 
 * <p>
 * <a href='http://www.iucr.org/iucr-top/cif/'>
 * http://www.iucr.org/iucr-top/cif/ </a>
 * 
 * <a href='http://www.iucr.org/iucr-top/cif/standard/cifstd5.html'>
 * http://www.iucr.org/iucr-top/cif/standard/cifstd5.html </a>
 * 
 * @author Miguel, Egon, and Bob (hansonr@stolaf.edu)
 * 
 *         symmetry added by Bob Hanson:
 * 
 *         setSpaceGroupName() setSymmetryOperator() setUnitCellItem()
 *         setFractionalCoordinates() setAtomCoord()
 *         applySymmetryAndSetTrajectory()
 * 
 */
public class CifReader extends AtomSetCollectionReader {

  protected static final String CELL_TYPE_MAGNETIC_PARENT = "parent";
  protected static final String CELL_TYPE_MAGNETIC_STANDARD = "standard";
  protected static final String CELL_TYPE_SPIN_FRAME = "spin";

  /**
   * Allows checking specific blocks
   * 
   * e.g. topoCifParser
   * 
   */
  interface Parser {

    Parser setReader(CifReader cifReader);

    boolean processBlock(String key) throws Exception;

    boolean finalizeReader() throws Exception;

    void finalizeSymmetry(boolean haveSymmetry) throws Exception;

    void ProcessRecord(String key, String data) throws Exception;

  }

  Parser subParser; // TopoCifParser

  private static final String titleRecords = "__citation_title__publ_section_title__active_magnetic_irreps_details__";

  private MSCifParser modr; // Modulated Structure subreader
  //  private MagCifRdr magr;// Magnetic CIF subreader - not necessary

  // no need for reflection here -- the CIF reader is already
  // protected by reflection

  GenericCifDataParser cifParser;

  private boolean isAFLOW;

  private boolean filterAssembly;
  private boolean allowRotations = true;
  private boolean readIdeal = true;
  private int configurationPtr = Integer.MIN_VALUE;
  protected boolean useAuthorChainID = true;

  protected String thisDataSetName = "", lastDataSetName;
  private String chemicalName = "";
  private String thisStructuralFormula = "";
  private String thisFormula = "";
  protected boolean iHaveDesiredModel;
  protected boolean isMMCIF;
  protected boolean isLigand;
  protected boolean isMagCIF, isSpinCIF;
  boolean haveHAtoms;
  private String molecularType = "GEOM_BOND default";
  private char lastAltLoc = '\0';
  private boolean haveAromatic;
  private int conformationIndex;
  private int nMolecular = 0;
  private String appendedData;
  protected boolean skipping;
  protected int nAtoms;
  protected int ac;
  private boolean haveMagneticMoments;

  private String auditBlockCode;
  private String lastSpaceGroupName;

  private boolean modulated;
  protected boolean isCourseGrained;
  boolean haveCellWaveVector;
  private boolean haveSpinReferences;

  protected Map<String, String> htGroup1;
  protected int nAtoms0;
  private int titleAtomSet = 1;
  private boolean addAtomLabelNumbers;
  private boolean ignoreGeomBonds;
  private boolean allowWyckoff = true;
  private boolean stopOn_SHELX_HKL;
  private String spinFrame;
  private String spinFrameExt;
  private boolean spinFrameSetByFILTER;
  private boolean spinOnly;
  private int maxOps = -1;
  private int nops;
  private boolean noLattice;
  
  
  @Override
  public void initializeReader() throws Exception {
    initSubclass();
    stopOn_SHELX_HKL = checkFilterKey("STOPONSHELXHKL");    
    allowPDBFilter = true;
    appendedData = (String) htParams.get("appendedData");
    spinOnly = checkFilterKey("SPINONLY");
    noLattice = checkFilterKey("NOLATTICE");
    String conf = getFilter("CONF ");
    if (conf != null)
      configurationPtr = parseIntStr(conf);
    if (filterCased != null && filterCased.toLowerCase().startsWith("spinframe=")) {
      spinFrame = filterCased.substring(10).trim();
      int pt = spinFrame.indexOf(";");
      if (pt >= 0)
        spinFrame = spinFrame.substring(0, pt);
      spinFrameSetByFILTER = true;
    }
    isMolecular = checkFilterKey("MOLECUL") && !checkFilterKey("BIOMOLECULE"); // molecular; molecule
    ignoreGeomBonds = checkFilterKey("IGNOREGEOMBOND")
        || checkFilterKey("IGNOREBOND");
    isPrimitive = checkFilterKey("PRIMITIVE");
    readIdeal = !checkFilterKey("NOIDEAL");
    allowWyckoff = !checkFilterKey("NOWYCKOFF");
    filterAssembly = checkFilterKey("$");
    useAuthorChainID = !checkFilterKey("NOAUTHORCHAINS");
    if (isMolecular) {
      forceSymmetry(false);
      molecularType = "filter \"MOLECULAR\"";
    }
    checkNearAtoms = !checkFilterKey("NOSPECIAL"); // as in "no special positions"
    allowRotations = !checkFilterKey("NOSYM");
    if (binaryDoc != null)
      return; // mmtf
    readCifData();
    continuing = false;
  }

  protected void initSubclass() {
    // for MMCifReader
  }

  private void readCifData() throws Exception {

    /*
     * Modified for 10.9.64 9/23/06 by Bob Hanson to remove as much as possible
     * of line dependence. a loop could now go:
     * 
     * blah blah blah loop_ _a _b _c 0 1 2 0 3 4 0 5 6 loop_......
     * 
     * we don't actually check that any skipped loop has the proper number of
     * data points --- some multiple of the number of data keys -- but other
     * than that, we are checking here for proper CIF syntax, and Jmol will
     * report if it finds data where a key is supposed to be.
     */
    cifParser = getCifDataParser();
    line = "";
    cifParser.peekToken();
    addAtomLabelNumbers = (cifParser.getFileHeader()
        .startsWith("# primitive CIF file created by Jmol"));
    while (continueWith(key = (String) cifParser.peekToken())
        && readEntryOrLoopData()) {
      // continue
    }
    if (appendedData != null) {
      cifParser = ((GenericCifDataParser) getInterface(
          "javajs.util.CifDataParser")).set(null, Rdr.getBR(appendedData),
              debugging);
      while ((key = (String) cifParser.peekToken()) != null)
        if (!readEntryOrLoopData())
          break;
    }
  }

  private boolean continueWith(String key) {
    boolean isHKL = false;
    boolean ret = key != null && (!stopOn_SHELX_HKL || (ac == 0 || !key.equals("_shelx_hkl_file")));
    if (ret && isHKL)
      System.err.println("CIFReader reading _shelx_hkl_file; use FILTER 'StopOnShelxHKL' to stop reading when this is found");
    return ret;
  }

  protected GenericCifDataParser getCifDataParser() {
    // overridden in Cif2Reader
    return new CifDataParser().set(this, null, debugging);
  }

  private boolean readEntryOrLoopData() throws Exception {
    if (key.startsWith("data_")) {
      return newData();
    }
    if (skipping && key.equals("_audit_block_code")) {
      iHaveDesiredModel = false;
      skipping = false;
    }
    isLoop = isLoopKey();
    if (isLoop) {
      if (skipping && !isMMCIF) {
        cifParser.getTokenPeeked();
        skipLoop(false);
      } else {
        processLoopBlock();
      }
      return true;
    }
    // global_ and stop_ are reserved STAR keywords
    // see http://www.iucr.org/iucr-top/lists/comcifs-l/msg00252.html
    // http://www.iucr.org/iucr-top/cif/spec/version1.1/cifsyntax.html#syntax

    // stop_ is not allowed, because nested loop_ is not allowed
    // global_ is a reserved STAR word; not allowed in CIF
    // ah, heck, let's just flag them as CIF ERRORS
    /*
     * if (key.startsWith("global_") || key.startsWith("stop_")) {
     * tokenizer.getTokenPeeked(); continue; }
     */
    if (key.indexOf("_") != 0) {
      Logger.warn(key.startsWith("save_") ? "CIF reader ignoring save_"
          : "CIF ERROR ? should be an underscore: " + key);
      cifParser.getTokenPeeked();
    } else if (!getData()) {
      return true;
    }

    if (!skipping) {
      key = cifParser.fixKey(key0 = key);
      if (key.startsWith("_chemical_name") || key.equals("_chem_comp_name")) {
        processChemicalInfo("name");
      } else if (key.startsWith("_chemical_formula_structural")) {
        processChemicalInfo("structuralFormula");
      } else if (key.startsWith("_chemical_formula_sum")
          || key.equals("_chem_comp_formula")) {
        processChemicalInfo("formula");
      } else if (key.equals("_cell_modulation_dimension")) {
        modDim = parseIntField();
        if (modr != null)
          modr.setModDim(modDim);
      } else if (skipKey(key)) {
      } else if (key.startsWith(CAT_CELL) && key.indexOf("_commen_") < 0) {
        processCellParameter();
      } else if (key.startsWith("_atom_sites_fract_tran")) {
        processUnitCellTransformMatrix();
      } else if (key.startsWith("_audit")) {
        if (key.equals("_audit_block_code")) {
          auditBlockCode = fullTrim(field).toUpperCase();
          appendLoadNote(auditBlockCode);
          if (htAudit != null && auditBlockCode.contains("_MOD_")) {
            String key = PT.rep(auditBlockCode, "_MOD_", "_REFRNCE_");
            if (asc.setSymmetryFromAuditBlock(
                (FileSymmetry) htAudit.get(key)) != null) {
              unitCellParams = asc.getSymmetry().getUnitCellParams();
              iHaveUnitCell = true;
            }
          } else if (htAudit != null) {
            if (symops != null)
              for (int i = 0; i < symops.size(); i++)
                setSymmetryOperator(symops.get(i));
          }
          if (lastSpaceGroupName != null)
            setSpaceGroupName(lastSpaceGroupName);
        } else if (key.equals("_audit_creation_date")) {
          symmetry = null;
        }
      } else if (key.startsWith("_chem_comp_atom") || key.startsWith("_atom")) {
        processLoopBlock();
        //        readSingleAtom();
      } else if (key.startsWith("_symmetry_space_group_name_h-m")
          || key.equals("_space_group_it_number")
          || key.startsWith("_symmetry_space_group_name_hall")
          || key.startsWith("_space_group_name") 
          || key.contains("_ssg_name")
          || key.contains("_magn_name") 
          || key.contains("_bns_name")
          || key.contains("_spin_number_")) {
        processSymmetrySpaceGroupName();
      } else if (key.startsWith("_space_group_transform")
          || key.startsWith("_parent_space_group")
          || key.startsWith("_space_group_magn_transform")
          || key.contains("_space_group_spin")) {
                  processUnitCellTransform();
      } else if (key.contains("_database_code")) {
        addModelTitle("ID");
      } else if (titleRecords.contains("_" + key + "__")) {
        addModelTitle("TITLE");
      } else if (key.startsWith("_aflow_")) {
        isAFLOW = true;
      } else if (key.equals("_symmetry_int_tables_number")) {
        int intTableNo = parseIntStr((String) field);
        rotateHexCell = (isAFLOW && (intTableNo >= 143 && intTableNo <= 194)); // trigonal or hexagonal
      } else if (key.equals("_entry_id")) {
        pdbID = (String) field;
      } else if (key.startsWith("_topol_")) {
        getTopologyParser().ProcessRecord(key, (String) field);
      } else {
        // see MMCifReader or MSRdr
        processSubclassEntry();
      }
    }
    return true;
  }

  @Override
  public int setSymmetryOperator(String xyz) {
    if (maxOps >= 0 && nops++ > maxOps)
      return 0;
    return super.setSymmetryOperator(xyz);
    
  }

  private boolean newData() throws Exception {
    if (!spinFrameSetByFILTER)
      spinFrame = null;        
    spinFrameExt = null;
    isLigand = false;
    if (asc.atomSetCount == 0)
      iHaveDesiredModel = false;
    if (iHaveDesiredModel)
      return false;
    if (desiredModelNumber != Integer.MIN_VALUE)
      appendLoadNote(null);
    newModel(-1);

    haveCellWaveVector = false;
    if (auditBlockCode == null)
      modulated = false;
    if (!skipping) {
      nAtoms0 = asc.ac;
      processDataParameter();
      nAtoms = asc.ac;
    }
    return true;
  }

  final static String CAT_ENTRY = "_entry";

  private boolean skipKey(String key) {
    return key.startsWith("_shelx_") || key.startsWith("_reflns_")
        || key.startsWith("_diffrn_");
  }

  private void addModelTitle(String key) {
    if (asc.atomSetCount > titleAtomSet)
      appendLoadNote("\nMODEL: " + (titleAtomSet = asc.atomSetCount));
    appendLoadNote(key + ": " + fullTrim(field));
  }

  protected void processSubclassEntry() throws Exception {
    if (modDim > 0)
      getModulationReader().processEntry();
  }

  /**
   * (magnetic CIF only)
   * 
   * Process the unit cell transformation as indicated by _parent_space_group or
   * _space_group_magn (or older _magentic_space_group)
   * 
   */
  private void processUnitCellTransform() {
    field = PT.replaceAllCharacters((String) field, " ", "");

    // old:

    // _magnetic_space_group.transform_from_parent_Pp_abc  '-1/3a+1/3b-2/3c,-a-b,-4/3a+4/3b+4/3c;0,0,0'
    // _magnetic_space_group.transform_to_standard_Pp_abc  'a-c,-b,-2a+c;0,0,0'

    // new: 
    // _parent_space_group.child_transform_Pp_abc   '-1/3a+1/3b-2/3c,-a-b,-4/3a+4/3b+4/3c;0,0,0'
    // _space_group_magn.transform_BNS_Pp_abc    '-a-c,-b,c;0,0,0'

    //  related:

    // _space_group_magn.transform_OG_Pp_abc     '-a-c,-b,1/2c;0,0,0'   -- no interest to us
    // _parent_space_group.transform_Pp_abc   'a,b,c;0,0,0'             -- no interest to us

    if (key.startsWith("_space_group_spin_")) {
          processSpinSpaceGroup();
    } else if (key.contains("_from_parent") || key.contains("child_transform")) {
      addCellType(CELL_TYPE_MAGNETIC_PARENT, (String) field, true);
    } else if (key.contains("_to_standard")
        || key.contains("transform_bns_pp_abc")) {
      addCellType(CELL_TYPE_MAGNETIC_STANDARD, (String) field, false);
    }
    appendLoadNote(key + ": " + field);
  }

  private void processSpinSpaceGroup() {

    //    _space_group_spin.transform_spinframe_P_matrix  [[1 -1 0] [1 0 0] [0 0 1]]
    //    _space_group_spin.transform_spinframe_P_abc  'a,b,c'
    //    _space_group_spin.collinear_direction . 
    //    _space_group_spin.coplanar_perp_uvw "0,0,1"
    //    _space_group_spin.rotation_axis "0,0,1"
    //    _space_group_spin.rotation_angle 45
    String tag = key.substring(18);
    switch (tag) {
    case "number_spsg_chen":
    case "name_spsg_chen":
      break;
    case "transform_spinframe_p_matrix":
    case "transform_spinframe_p_abc":
      String sf = parseUvwMath((String) field);
      if (spinFrameSetByFILTER) {
        System.out.println("CifReader spinFrame set by user to " + spinFrame
            + " file setting ignored: " + field);
      } else {
        System.out.println("CifReader spinFrame set to " + field
            + "; use load ... FILTER \"spinframe xxxxx\" to modify");
        spinFrame = sf;
      }
      if (spinFrame.charAt(0) == '[') {
        // string representation of matrix
        // generate matrix, save if nec. and continue on
        M4d m4 = M4d.newM4(null);
        m4.setRotationScale((M3d) Escape.unescapeMatrixD(spinFrame));
        spinFrame = SymmetryOperation.getTransformABC(m4, false);
      }
      addCellType(CELL_TYPE_SPIN_FRAME, spinFrame, false);
      field = spinFrame;
      tag = "spinFrame";
      break;
    case "rotation_axis_cartn":
      field = addSpinFrameExt("axis", false);
      return;
    case "rotation_axis_xyz":
      field = addSpinFrameExt("axis", true);
      return;
    case "rotation_angle":
      field = addSpinFrameExt("angle", false);
      return;
    case "collinear_direction":
      field = addSpinFrameExt("coldir", true);
      break;
    case "coplanar_perp_uvw":
//      we don't need this value, because 
//      it is only incidental information
//      provided to point out the characteristics
//      of the symmetry operation spin components
//      field = addSpinFrameExt("perpuvw");
      break;
    default:
      System.err.println("CIFReader unrecognized spin key " + key);
      return;
    }
    addMoreUnitCellInfo(tag + "=" + field);
  }

  private String addSpinFrameExt(String name, boolean doClean) {
    String val = field.toString();
    if (doClean)
      val = PT.replaceAllCharacters(val, "[]\"", "");
    if (spinFrameExt == null)
      spinFrameExt = "";
    spinFrameExt += ";" + name + "=" + val + ";";
    return val;
  }

  private Map<String, String> htCellTypes;

  /**
   * (magnetic CIF or LOAD ... SUPERCELL)
   * 
   * Add a cell type such as "conventional"
   * 
   * @param type
   * @param data
   *        if starting with "!" then "opposite of"
   * @param isFrom
   *        TRUE for SUPERCELL or "_from_parent" or "child_transform"
   */
  private void addCellType(String type, String data, boolean isFrom) {
    if (htCellTypes == null)
      htCellTypes = new Hashtable<String, String>();
    if (data.startsWith("!")) {
      data = data.substring(1);
      isFrom = !isFrom;
    }
    String cell = (isFrom ? "!" : "") + data;
    htCellTypes.put(type, cell);
    if (type.equalsIgnoreCase(strSupercell)) {
      strSupercell = cell;
      htCellTypes.put(CELL_TYPE_SUPER, (isFrom ? "!" : "") + data);
      htCellTypes.put(CELL_TYPE_CONVENTIONAL, (isFrom ? "" : "!") + data);
    }
  }

  //  /**
  //   * No need for anything other than the atom name and symbol; coordinates will
  //   * be (0 0 0), and no other information is needed.
  //   */
  //  private void readSingleAtom() {
  //    Atom atom = new Atom();
  //    atom.set(0, 0, 0);
  //    atom.atomName = fullTrim(data);
  //    atom.getElementSymbol();
  //    asc.addAtom(atom);
  //  }
  //
  private MSCifParser getModulationReader() throws Exception {
    return (modr == null ? initializeMSCIF() : modr);
  }

  private MSCifParser initializeMSCIF() throws Exception {
    if (modr == null)
      ms = modr = (MSCifParser) getInterface(
          "org.jmol.adapter.readers.cif.MSCifParser");
    modulated = (modr.initialize(this, modDim) > 0);
    return modr;
  }

  //  private MagCifRdr getMagCifReader() throws Exception {
  //    return (magr == null ? initializeMagCIF() : magr);
  //  }

  //  private MagCifRdr initializeMagCIF() throws Exception {
  //    if (magr == null)
  //      magr = (MagCifRdr) getInterface("org.jmol.adapter.readers.cif.MagCifRdr");
  //    return magr;
  //  }

  public Map<String, Integer> modelMap;

  private boolean haveGlobalDummy;

  protected void newModel(int modelNo) throws Exception {
    if (modelNo < 0) {
      if (modelNumber == 1 && asc.ac == 0 && nAtoms == 0 && !haveGlobalDummy
          && !skipping) {
        modelNumber = 0;
        haveModel = false;
        haveGlobalDummy = true;
        asc.removeCurrentAtomSet();
      }
      modelNo = ++modelNumber;
    }

    skipping = !doGetModel(modelNumber = modelNo, null);
    if (skipping) {
      if (!isMMCIF)
        cifParser.getTokenPeeked();
      return;
    }
    chemicalName = "";
    thisStructuralFormula = "";
    thisFormula = "";
    iHaveDesiredModel = isLastModel(modelNumber);
    if (isCourseGrained)
      asc.setCurrentModelInfo("courseGrained", Boolean.TRUE);
    if (nAtoms0 > 0 && nAtoms0 == asc.ac) {
      modelNumber--;
      haveModel = false;
      asc.removeCurrentAtomSet();
    } else if (asc.iSet >= 0) {
      applySymmetryAndSetTrajectory();
    }
    isMolecular = false;
    if (auditBlockCode == null) {
      modDim = 0;
    }

  }

  @Override
  protected void finalizeSubclassReader() throws Exception {
    if (htOxStates != null)
      setOxidationStates();
    if (htJmolNames != null)
      setJmolNames();
    // added check for final data_global
    if (asc.iSet > 0 && asc.getAtomSetAtomCount(asc.iSet) == 0)
      asc.atomSetCount--;
    else if (!finalizeSubclass())
      applySymmetryAndSetTrajectory();
    int n = asc.atomSetCount;
    if (n > 1)
      asc.setCollectionName("<collection of " + n + " models>");
    if (pdbID != null)
      asc.setCurrentModelInfo("pdbID", pdbID);
    finalizeReaderASCR();
    addHeader();
    if (haveAromatic)
      addJmolScript("calculate aromatic");
    // Q: Isn't asc.xtalSymmetry already nulled?
    if (!isMMCIF && asc.xtalSymmetry != null) {
      asc.xtalSymmetry.setPartProperty();
    }
  }

  private void setOxidationStates() {
    for (int i = asc.ac; --i >= 0;) {
      Atom a = asc.atoms[i];
      String sym = a.typeSymbol;
      double[] data;
      if (sym != null && (data = htOxStates.get(sym)) != null) {
        double charge = data[0];
        double radius = data[1];
        if (!Double.isNaN(charge)) {
          a.formalCharge = (int) Math.round(charge);
        }
        if (!Double.isNaN(radius)) {
          a.bondingRadius = radius;
        }
      }
    }
  }

  protected void addHeader() {
    String header = cifParser.getFileHeader();
    if (header.length() > 0) {
      String s = setLoadNote();
      appendLoadNote(null);
      appendLoadNote(header);
      appendLoadNote(s);
      setLoadNote();
      asc.setInfo("fileHeader", header);
    }
  }

  protected boolean finalizeSubclass() throws Exception {
    // overridden by MMCifReader and calling TopoCifReader
    return (subParser == null ? false : subParser.finalizeReader()); // see TopoCifParser
  }

  @Override
  public void doPreSymmetry(boolean doApplySymmetry) throws Exception {
    if (mapSpinIdToUVW != null) {
      asc.getSymmetry().mapSpins(mapSpinIdToUVW);
    } else if (haveSpinReferences) {
      lstSpinLattices = null;
      appendLoadNote(
          "Warning: CIF FILE contains _space_group_symop_spin_lattice_R.uvw_id but not _space_group_symop_spin_operation_U.uvw!");
    }
    if (magCenterings != null || lstSpinLattices != null)
      addLatticeVectors();
    if (modDim > 0)
      getModulationReader().setModulation(false, null);
    if (isMagCIF || isSpinCIF) {
      if (!haveMagneticMoments) {
        isMagCIF = isSpinCIF = false;
      } else {
        asc.getXSymmetry().finalizeMoments(spinFrame, spinFrameExt);
        vibsFractional = true;
      }
    }
    if (isSpinCIF) {
      Lst<String> lst = asc.getSymmetry().setSpinList(null);
      if (lst != null) {
        asc.setCurrentModelInfo("spinList", lst);
        appendLoadNote(lst.size()
            + " spin operations -- see _M.spinList"
            + (doApplySymmetry ? " and atom.spin" : ""));
      }
    }
  }

  @Override
  public void applySymmetryAndSetTrajectory() throws Exception {
    // This speeds up calculation, because no crosschecking
    // No special-position atoms in mmCIF files, because there will
    // be no center of symmetry, no rotation-inversions, 
    // no atom-centered rotation axes, and no mirror or glide planes.
    
    if (isMMCIF)
      checkNearAtoms = false;
    boolean doCheckBonding = doCheckUnitCell && !isMMCIF;
    if (isMMCIF && asc.iSet >= 0) {
      int modelIndex = asc.iSet;
      asc.setCurrentModelInfo("PDB_CONECT_firstAtom_count_max",
          new int[] { asc.getAtomSetAtomIndex(modelIndex),
              asc.getAtomSetAtomCount(modelIndex), maxSerial });
    }
    if (htCellTypes != null) {
      for (Entry<String, String> e : htCellTypes.entrySet())
        asc.setCurrentModelInfo("unitcell_" + e.getKey(), e.getValue());
      htCellTypes = null;
    }
    if (!haveCellWaveVector)
      modDim = 0;
    if (doApplySymmetry && !iHaveFractionalCoordinates)
      fractionalizeCoordinates(true);
    if (!haveCellWaveVector) {
      modDim = 0;
    }
    if (spinOnly) {
      BS bs = asc.getBSAtoms(-1);
      for (int i = asc.getAtomSetAtomIndex(asc.iSet); i < asc.ac; i++) {
        bs.setBitTo(i, asc.atoms[i].vib != null && asc.atoms[i].vib.lengthSquared() > 0);
      }
    }
    applySymTrajASCR();
    if (!haveCellWaveVector) {
      if (!isMolecular) {
        asc.setBSAtomsForSet(-1);
      }
    }
    if (doCheckBonding && (bondTypes.size() > 0 || isMolecular))
      setBondingAndMolecules();
    asc.setCurrentModelInfo("fileHasUnitCell", Boolean.TRUE);
    asc.xtalSymmetry = null;
  }

  @Override
  protected void finalizeSubclassSymmetry(boolean haveSymmetry)
      throws Exception {
    // called by applySymTrajASCR
    // just for modulated, audit block, and magnetic structures
    FileSymmetry sym = (haveSymmetry
        ? asc.getXSymmetry().getBaseSymmetry()
        : null);
    if (sym != null && sym.getSpaceGroup() == null) {
      if (!isBinary && !isMMCIF) // ignore this for MMTF
        appendLoadNote("Invalid or missing space group operations!");
      sym = null;
    }
    if (modDim > 0 && sym != null) {
      addLatticeVectors();
      asc.setTensors();
      getModulationReader().setModulation(true, sym);
      modr.finalizeModulation();
    }
    if (sym != null && auditBlockCode != null
        && auditBlockCode.contains("REFRNCE")) {
      if (htAudit == null)
        htAudit = new Hashtable<String, Object>();
      htAudit.put(auditBlockCode, sym);
    }
    if (subParser != null)
      subParser.finalizeSymmetry(haveSymmetry);
    if (sym != null && (isMagCIF || isSpinCIF))
      finalizeMagneticMoments();
  }

  ////////////////////////////////////////////////////////////////
  // processing methods
  ////////////////////////////////////////////////////////////////

  private void finalizeMagneticMoments() {
    if (asc.xtalSymmetry == null)
      return;
    asc.setNoAutoBond();
    addJmolScript("vectors on;vectors 0.15;");
    int n = asc.xtalSymmetry.setMagneticMoments(false);
    appendLoadNote(n
        + " magnetic moments - use VECTORS ON/OFF or VECTOR MAX x.x or SELECT VXYZ>0");
  }

  private Hashtable<String, Object> htAudit;
  public Lst<String> symops;

  /**
   * initialize a new atom set
   * 
   */
  private void processDataParameter() {
    bondTypes.clear();
    cifParser.getTokenPeeked();
    thisDataSetName = (key.length() < 6 ? "" : key.substring(5));
    if (thisDataSetName.length() > 0)
      nextAtomSet();
    if (debugging)
      Logger.debug(key);
  }

  protected String pdbID;
  private Lst<String> lstSpinLattices;
  private Map<String, String> mapSpinIdToUVW;
  private byte newAtomSetLabel = NONE;

  protected void nextAtomSet() {
    newAtomSetLabel = NONE;
    asc.setCurrentModelInfo("isCIF", Boolean.TRUE);
    if (asc.iSet >= 0) {
      // note that there can be problems with multi-data mmCIF sets each with
      // multiple models; and we could be loading multiple files!
      if (isMMCIF) {
        setModelPDB(true); // first model can be missed
        if (pdbID != null)
          asc.setCurrentModelInfo("pdbID", pdbID);
      }
      asc.newAtomSet();
      if (isMMCIF) {
        setModelPDB(true);
        if (pdbID != null)
          asc.setCurrentModelInfo("pdbID", pdbID);
      }
    } else {
      asc.setCollectionName(thisDataSetName);
    }
  }

  /**
   * reads some of the more interesting info into specific atomSetAuxiliaryInfo
   * elements
   * 
   * @param type
   *        "name" "formula" etc.
   * @return data
   * @throws Exception
   */
  private String processChemicalInfo(String type) throws Exception {
    String field = (String) this.field;
    if (type.equals("name")) {
      chemicalName = field = fullTrim(field);
      appendLoadNote(chemicalName);
      if (!field.equals("?"))
        asc.setInfo("modelLoadNote", field);
    } else if (type.equals("structuralFormula")) {
      thisStructuralFormula = field = fullTrim(field);
    } else if (type.equals("formula")) {
      thisFormula = field = fullTrim(field);
      if (thisFormula.length() > 1)
        appendLoadNote(thisFormula);
    }
    if (debugging) {
      Logger.debug(type + " = " + field);
    }
    return field;
  }

  //  _space_group.magn_ssg_name_BNS "P2_1cn1'(0,0,g)000s"
  //  _space_group.magn_ssg_number_BNS 33.1.9.5.m145.?
  //  _space_group.magn_point_group "mm21'"
  //  _space_group_spin.number_SSG1  "47.123.1.1.L" 


  /**
   * done by AtomSetCollectionReader
   * 
   * @throws Exception
   */
  private void processSymmetrySpaceGroupName() throws Exception {
    if (key.indexOf("_ssg_name") >= 0) {
      modulated = true;
      latticeType = ((String) field).substring(0, 1);
    } else if (modulated) {
      return;
    }
    
    String s = cifParser.toUnicode((String) field);
    setSpaceGroupName(lastSpaceGroupName = (
        key.indexOf("h-m") > 0 ? "HM:"
        : modulated ? "SSG:" 
        : key.indexOf("spin") > 0 ? "spinSG:"
        : key.indexOf("bns") >= 0 ? "BNS:"
        : key.indexOf("hall") >= 0 ? "Hall:" 
        : "")
        + s);
  }

  private void addLatticeVectors() {
    if (lstSpinLattices != null && !noLattice) {
        asc.getSymmetry().addSpinLattice(lstSpinLattices, mapSpinIdToUVW);
    }
    if (noLattice)
      return;
    lattvecs = null;
    if (magCenterings != null) {
      // could be x+1/2,y+1/2,z,+1
      // or   x+0.5,y+0.5,z,+1
      // or   0.5+x,0.5+y,z,+1
      //
      latticeType = "Magnetic";
      lattvecs = new Lst<double[]>();
      for (int i = 0; i < magCenterings.size(); i++) {
        String s = magCenterings.get(i);
        double[] f = new double[modDim + 4];
        if (s.indexOf("x1") >= 0)
          for (int j = 1; j <= modDim + 3; j++)
            s = PT.rep(s, "x" + j, "");
        String[] tokens = PT.split(PT.replaceAllCharacters(s, "xyz+", ""), ",");
        int n = 0;
        for (int j = 0; j < tokens.length; j++) {
          s = tokens[j].trim();
          if (s.length() == 0)
            continue;
          if ((f[j] = PT.parseDoubleFraction(s)) != 0)
            n++;
        }
        if (n >= 2) // needs to have an x y or z as well as a +/-1;
          lattvecs.addLast(f);
      }
      magCenterings = null;
    } else if (latticeType != null && "ABCFI".indexOf(latticeType) >= 0) {
      lattvecs = new Lst<double[]>();
      try {
        ms.addLatticeVector(lattvecs, latticeType);
      } catch (Exception e) {
        // n/a
      }
    }
    if (lattvecs != null && lattvecs.size() > 0
        && asc.getSymmetry().addMagLatticeVectors(lattvecs)) {
      appendLoadNote("Note! " + lattvecs.size()
          + " symmetry operators added for lattice centering " + latticeType);
      for (int i = 0; i < lattvecs.size(); i++)
        appendLoadNote(PT.toJSON(null, lattvecs.get(i)));
    }
    latticeType = null;
  }

  final protected static String CAT_CELL = "_cell";

  /**
   * unit cell parameters -- two options, so we use MOD 6
   * 
   * @throws Exception
   */
  protected void processCellParameter() throws Exception {
    for (int i = 6; --i >= 0;) {
      if (key.equals(JmolAdapter.cellParamNames[i])) {
        double p = parseDoubleField();
        if (rotateHexCell && i == 5 && p == 120)
          p = -1;
        setUnitCellItem(i, p);
        return;
      }
    }
  }

  final private static String[] TransformFields = { "x[1][1]", "x[1][2]",
      "x[1][3]", "r[1]", "x[2][1]", "x[2][2]", "x[2][3]", "r[2]", "x[3][1]",
      "x[3][2]", "x[3][3]", "r[3]", };

  final protected static String CAT_ATOM_SITES = "_atom_sites";

  /**
   * 
   * the PDB transformation matrix cartesian --> fractional
   * 
   * key and field have been set already
   * 
   * @throws Exception
   */
  protected void processUnitCellTransformMatrix() throws Exception {
    /*
     * PDB:
     
     SCALE1       .024414  0.000000  -.000328        0.00000
     SCALE2      0.000000   .053619  0.000000        0.00000
     SCALE3      0.000000  0.000000   .044409        0.00000
    
     * CIF:
    
     _atom_sites.fract_transf_matrix[1][1]   .024414 
     _atom_sites.fract_transf_matrix[1][2]   0.000000 
     _atom_sites.fract_transf_matrix[1][3]   -.000328 
     _atom_sites.fract_transf_matrix[2][1]   0.000000 
     _atom_sites.fract_transf_matrix[2][2]   .053619 
     _atom_sites.fract_transf_matrix[2][3]   0.000000 
     _atom_sites.fract_transf_matrix[3][1]   0.000000 
     _atom_sites.fract_transf_matrix[3][2]   0.000000 
     _atom_sites.fract_transf_matrix[3][3]   .044409 
     _atom_sites.fract_transf_vector[1]      0.00000 
     _atom_sites.fract_transf_vector[2]      0.00000 
     _atom_sites.fract_transf_vector[3]      0.00000 
    
     */
    double v = parseDoubleField();
    if (Double.isNaN(v))
      return;
    //could enable EM box: unitCellParams[0] = 1;
    for (int i = 0; i < TransformFields.length; i++) {
      if (key.indexOf(TransformFields[i]) >= 0) {
        setUnitCellItem(6 + i, v);
        return;
      }
    }
  }

  ////////////////////////////////////////////////////////////////
  // non-loop_ processing
  ////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////
  // loop_ processing
  ////////////////////////////////////////////////////////////////

  /**
   * processes loop_ blocks of interest or skips the data
   * 
   * @throws Exception
   */
  protected void processLoopBlock() throws Exception {
    if (isLoop) {
      skipLoopKeyword();
      key = (String) cifParser.peekToken();
      if (key == null)
        return;
      key = cifParser.fixKey(key0 = key);
    }
    if (modDim > 0) {
      switch (getModulationReader().processLoopBlock()) {
      case 0:
        break;
      case -1:
        skipLoop(false);
        //$FALL-THROUGH$
      case 1:
        return;
      }
    }
    boolean isLigand = false;
    if (key.startsWith(CAT_ATOM_SITE)
        || (isLigand = key.startsWith("_chem_comp_atom_"))) {
      if (!processAtomSiteLoopBlock(isLigand))
        return;
      if (thisDataSetName.equals("global"))
        asc.setCollectionName(thisDataSetName = chemicalName);
      if (!thisDataSetName.equals(lastDataSetName)) {
        asc.setAtomSetName(thisDataSetName);
        lastDataSetName = thisDataSetName;
      }
      asc.setCurrentModelInfo("chemicalName", chemicalName);
      asc.setCurrentModelInfo("structuralFormula", thisStructuralFormula);
      asc.setCurrentModelInfo("formula", thisFormula);
      return;
    }
    if (key.startsWith(CAT_SGOP) || key.startsWith("_symmetry_equiv_pos")
        || key.startsWith("_symmetry_ssg_equiv")) {
      // 2023.08.03 disallow x,y,z when modDim > 0
      if (ignoreFileSymmetryOperators || modDim > 0 && key.indexOf("ssg") < 0) {
        Logger.warn("ignoring file-based symmetry operators");
        skipLoop(false);
      } else {
        processSymmetryOperationsLoopBlock();
      }
      return;
    }
    if (key.startsWith("_citation")) {
      processCitationListBlock();
      return;
    }
    if (key.startsWith(CAT_ATOM_TYPE)) {
      processAtomTypeLoopBlock();
      return;
    }
    if (key.startsWith("_geom_bond")) {
      processGeomBondLoopBlock();
      return;
    }
    if (key.startsWith("_jmol")) {
      processJmolBlock();
      return;
    }
    if (processSubclassLoopBlock())
      return;
    if (key.equals("_propagation_vector_seq_id")) {// Bilbao mCIF
      addMore();
      return;
    }
    skipLoop(false);
  }

  protected boolean processSubclassLoopBlock() throws Exception {
    if (key.startsWith("_topol_")) {
      return getTopologyParser().processBlock(key);
    }
    return false;
  }

  private Parser getTopologyParser() {
    if (subParser == null) {
      subParser = ((Parser) javajs.api.Interface
          .getInterface("org.jmol.adapter.readers.cif.TopoCifParser"));
      subParser = subParser.setReader(this);
    }
    return subParser;
  }

  private void addMore() {
    String str;
    int n = 0;
    try {
      while ((str = (String) cifParser.peekToken()) != null
          && str.charAt(0) == '_') {
        cifParser.getTokenPeeked();
        n++;
      }
      int m = 0;
      String s = "";
      while ((str = (String) cifParser.getNextDataToken()) != null) {
        s += str + (m % n == 0 ? "=" : " ");
        if (++m % n == 0) {
          addMoreUnitCellInfo(s.trim());
          s = "";
        }
      }
    } catch (Exception e) {
    }
  }

  /**
   * 
   * used for turning off fractional or nonfractional coord.
   * 
   * @param fieldIndex
   */
  private void disableField(int fieldIndex) {
    int i = key2col[fieldIndex];
    if (i != NONE)
      col2key[i] = NONE;
  }

  ////////////////////////////////////////////////////////////////
  // atom type data
  ////////////////////////////////////////////////////////////////

  private Map<String, double[]> htOxStates;
  private Lst<Object[]> bondTypes = new Lst<Object[]>();

  private String disorderAssembly = ".";
  private String lastDisorderAssembly;
  private Lst<double[]> lattvecs;
  private Lst<String> magCenterings;
  protected int maxSerial;

  final private static byte ATOM_TYPE_SYMBOL = 0;
  final private static byte ATOM_TYPE_OXIDATION_NUMBER = 1;
  final private static byte ATOM_TYPE_RADIUS_BOND = 2;

  final protected static String CAT_ATOM_TYPE = "_atom_type";
  final private static String[] atomTypeFields = { 
      "_atom_type_symbol",
      "_atom_type_oxidation_number", 
      "_atom_type_radius_bond" };

  /**
   * 
   * reads the oxidation number and associates it with an atom name, which can
   * then later be associated with the right atom indirectly.
   * 
   * @return true; for convenience only in switch statements
   * 
   * @throws Exception
   */
  protected boolean processAtomTypeLoopBlock() throws Exception {
    parseLoopParameters(atomTypeFields);
    while (cifParser.getData()) {
      String sym = getFieldString(ATOM_TYPE_SYMBOL);
      if (sym == null)
        continue;
      double oxno = parseDoubleStr(getFieldString(ATOM_TYPE_OXIDATION_NUMBER));
      double radius = parseDoubleStr(getFieldString(ATOM_TYPE_RADIUS_BOND));
      if (Double.isNaN(oxno) && Double.isNaN(radius))
        continue;
      if (htOxStates == null)
        htOxStates = new Hashtable<String, double[]>();
      htOxStates.put(sym, new double[] { oxno, radius });
    }
    return true;
  }
  
  //final private static byte JMOL_ATOM_INDEX = 0;
  final private static byte JMOL_ATOM_NAME = 1;
  final private static byte JMOL_ATOM_SITE_LABEL = 2;

  final private static String[] jmolAtomFields = { 
      "_jmol_atom_index",
      "_jmol_atom_name", 
      "_jmol_atom_site_label" 
      };

//  loop_
//  _jmol_atom_index
//  _jmol_atom_name
//  _jmol_atom_site_label
//    0 FE1    Fe1
//    1 CL1    Cl1
//    2 P1     P1
//    3 N1     N1
//  143 Q11   Xx11

  private Map<String, String> htJmolNames;
  
  private void processJmolBlock() throws Exception {
    htJmolNames = new Hashtable<>();
    parseLoopParameters(jmolAtomFields);
    while (cifParser.getData()) {
      String jmolName = getFieldString(JMOL_ATOM_NAME);
      String cifName = getFieldString(JMOL_ATOM_SITE_LABEL);
      if (jmolName == null || cifName == null)
        continue;
      htJmolNames.put(cifName,  jmolName);
    }
  }

  private void setJmolNames() {
    for (int i = 0, n = asc.ac; i < n; i++) {
      Atom a = asc.atoms[i];
      String name = htJmolNames.get(a.atomName);
      if (name != null)
        a.atomName = name;
    }
  }

  ////////////////////////////////////////////////////////////////
  // atom site data
  ////////////////////////////////////////////////////////////////

  protected final static byte EMPTY = -2;
  protected final static byte NONE = -1;
  final private static byte TYPE_SYMBOL = 0;
  final private static byte LABEL = 1;
  final private static byte AUTH_ATOM_ID = 2;
  final private static byte FRACT_X = 3;
  final private static byte FRACT_Y = 4;
  final private static byte FRACT_Z = 5;
  final private static byte CARTN_X = 6;
  final private static byte CARTN_Y = 7;
  final private static byte CARTN_Z = 8;
  final private static byte OCCUPANCY = 9;
  final private static byte B_ISO = 10;
  final private static byte AUTH_COMP_ID = 11;
  final private static byte AUTH_ASYM_ID = 12;
  final private static byte AUTH_SEQ_ID = 13;
  final private static byte INS_CODE = 14;
  final private static byte ALT_ID = 15;
  final private static byte GROUP_PDB = 16;
  final private static byte MODEL_NO = 17;
  final private static byte DUMMY_ATOM = 18;

  final private static byte DISORDER_GROUP = 19;
  final private static byte ANISO_LABEL = 20;
  final private static byte ANISO_MMCIF_ID = 21;
  final private static byte ANISO_U11 = 22;
  final private static byte ANISO_U22 = 23;
  final private static byte ANISO_U33 = 24;
  final private static byte ANISO_U12 = 25;
  final private static byte ANISO_U13 = 26;
  final private static byte ANISO_U23 = 27;
  final private static byte ANISO_MMCIF_U11 = 28;
  final private static byte ANISO_MMCIF_U22 = 29;
  final private static byte ANISO_MMCIF_U33 = 30;
  final private static byte ANISO_MMCIF_U12 = 31;
  final private static byte ANISO_MMCIF_U13 = 32;
  final private static byte ANISO_MMCIF_U23 = 33;
  final private static byte U_ISO_OR_EQUIV = 34;
  final private static byte ANISO_B11 = 35;
  final private static byte ANISO_B22 = 36;
  final private static byte ANISO_B33 = 37;
  final private static byte ANISO_B12 = 38;
  final private static byte ANISO_B13 = 39;
  final private static byte ANISO_B23 = 40;
  final private static byte ANISO_BETA_11 = 41;
  final private static byte ANISO_BETA_22 = 42;
  final private static byte ANISO_BETA_33 = 43;
  final private static byte ANISO_BETA_12 = 44;
  final private static byte ANISO_BETA_13 = 45;
  final private static byte ANISO_BETA_23 = 46;
  final private static byte ADP_TYPE = 47;
  final private static byte CC_COMP_ID = 48;
  final private static byte CC_ATOM_ID = 49;
  final private static byte CC_ATOM_SYM = 50;
  final private static byte CC_ATOM_CHARGE = 51;
  final private static byte CC_ATOM_X = 52;
  final private static byte CC_ATOM_Y = 53;
  final private static byte CC_ATOM_Z = 54;
  final private static byte CC_ATOM_X_IDEAL = 55;
  final private static byte CC_ATOM_Y_IDEAL = 56;
  final private static byte CC_ATOM_Z_IDEAL = 57;
  final private static byte DISORDER_ASSEMBLY = 58;
  final private static byte LABEL_ASYM_ID = 59;
  final private static byte SUBSYS_ID = 60;
  final private static byte SYMMETRY_MULT = 61;
  final private static byte THERMAL_TYPE = 62;
  final private static byte MOMENT_LABEL = 63;
  final private static byte MOMENT_PRELIM_X = 64;
  final private static byte MOMENT_PRELIM_Y = 65;
  final private static byte MOMENT_PRELIM_Z = 66;
  final private static byte MOMENT_X = 67;
  final private static byte MOMENT_Y = 68;
  final private static byte MOMENT_Z = 69;
  final private static byte ATOM_ID = 70;
  final private static byte LABEL_SEQ_ID = 71;
  final private static byte LABEL_COMP_ID = 72;
  final private static byte LABEL_ATOM_ID = 73;
  final private static byte WYCKOFF_LABEL = 74;
  final private static byte SITE_SYMMETRY_MULTIPLICITY= 75;
  final private static byte SPIN_U_PRELIM = 76;
  final private static byte SPIN_V_PRELIM = 77;
  final private static byte SPIN_W_PRELIM = 78;
  final private static byte spin_moment_label = 79;
  final private static byte spin_moment_axis_u = 80;
  final private static byte spin_moment_axis_v = 81;
  final private static byte spin_moment_axis_w = 82;
  final private static byte spin_moment_symmform_uvw = 83;
  final private static byte spin_moment_magnitude = 84; // required if missing 
  final private static byte spin_moment_spherical_azimuthal = 85;
  final private static byte spin_moment_spherical_polar = 86;
  
  
  final protected static String CAT_ATOM_SITE = "_atom_site";
  final private static String[] atomFields = { //
      "*_type_symbol", //0
      "*_label", //
      "*_auth_atom_id", // 
      "*_fract_x", //
      "*_fract_y", //
      "*_fract_z", //
      "*_cartn_x", //
      "*_cartn_y", //
      "*_cartn_z", //
      "*_occupancy", //
      "*_b_iso_or_equiv", //10
      "*_auth_comp_id", // 11
      "*_auth_asym_id", // 
      "*_auth_seq_id", // integer
      "*_pdbx_pdb_ins_code", // 
      "*_label_alt_id", // 
      "*_group_pdb", //
      "*_pdbx_pdb_model_num", // 
      "*_calc_flag", //  18
      "*_disorder_group", //
      "*_aniso_label", // 
      "*_anisotrop_id", // 
      "*_aniso_u_11", // 
      "*_aniso_u_22", //
      "*_aniso_u_33", // 
      "*_aniso_u_12", // 
      "*_aniso_u_13", // 
      "*_aniso_u_23", //
      "*_anisotrop_u[1][1]", // 
      "*_anisotrop_u[2][2]", // 
      "*_anisotrop_u[3][3]", //
      "*_anisotrop_u[1][2]", // 
      "*_anisotrop_u[1][3]", // 
      "*_anisotrop_u[2][3]", //
      "*_u_iso_or_equiv", // 
      "*_aniso_b_11", // 
      "*_aniso_b_22", // 
      "*_aniso_b_33", //
      "*_aniso_b_12", // 
      "*_aniso_b_13", // 
      "*_aniso_b_23", // 
      "*_aniso_beta_11", //
      "*_aniso_beta_22", // 
      "*_aniso_beta_33", // 
      "*_aniso_beta_12", //
      "*_aniso_beta_13", // 
      "*_aniso_beta_23", // 
      "*_adp_type", //
      "_chem_comp_atom_comp_id", //
      "_chem_comp_atom_atom_id", //
      "_chem_comp_atom_type_symbol", // 
      "_chem_comp_atom_charge", //
      "_chem_comp_atom_model_cartn_x", // 
      "_chem_comp_atom_model_cartn_y", //
      "_chem_comp_atom_model_cartn_z", //
      "_chem_comp_atom_pdbx_model_cartn_x_ideal", //
      "_chem_comp_atom_pdbx_model_cartn_y_ideal", //
      "_chem_comp_atom_pdbx_model_cartn_z_ideal", // 
      "*_disorder_assembly", //
      "*_label_asym_id", // 
      "*_subsystem_code", // 
      "*_symmetry_multiplicity", //
      "*_thermal_displace_type", // 
      "*_moment_label", // 
      "*_moment_crystalaxis_mx", //
      "*_moment_crystalaxis_my", // 
      "*_moment_crystalaxis_mz", //
      "*_moment_crystalaxis_x", // 
      "*_moment_crystalaxis_y", //
      "*_moment_crystalaxis_z", //
      "*_id", // 70 mmCIF
      "*_label_seq_id", // 
      "*_label_comp_id", // 72 mmCIF dev 
      "*_label_atom_id", // 73 mCIF dev
      "*_wyckoff_label", // 74
      "*_site_symmetry_multiplicity", // 75
      "*_moment_spinaxis_u", //v3 
      "*_moment_spinaxis_v", //v3
      "*_moment_spinaxis_w", //v3
      "*_spin_moment_label", // 79
      "*_spin_moment_axis_u",
      "*_spin_moment_axis_v",
      "*_spin_moment_axis_w",
      "*_spin_moment_symmform_uvw",
      "*_spin_moment_magnitude",
      "*_spin_moment_spherical_azimuthal",
      "*_spin_moment_spherical_polar", //86

      
  };

  //  final private static String singleAtomID = atomFields[CC_COMP_ID];

  /* to: hansonr@stolaf.edu
   * from: Zukang Feng zfeng@rcsb.rutgers.edu
   * re: Two mmCIF issues
   * date: 4/18/2006 10:30 PM
   * "You should always use _atom_site.auth_asym_id for PDB chain IDs."
   * 
   * 
   */

  void parseLoopParametersFor(String key, String[] fieldNames)
      throws Exception {
    // just once for static fields
    // first field must start with * if any do
    if (fieldNames[0].charAt(0) == '*')
      for (int i = fieldNames.length; --i >= 0;)
        if (fieldNames[i].charAt(0) == '*')
          fieldNames[i] = key + fieldNames[i].substring(1);
    parseLoopParameters(fieldNames);
  }

  protected int fieldProperty(int col) {
    int k = (col < 0 ? NONE : col2key[col]);
    if (k == NONE)
      return NONE;
    field = cifParser.getColumnData(col);
    return (col >= 0 && isFieldValid() ? col2key[col] : NONE);
  }

  /**
   * reads atom data in any order
   * 
   * @param isLigand
   * 
   * @return TRUE if successful; FALS if EOF encountered
   * @throws Exception
   */
  boolean processAtomSiteLoopBlock(boolean isLigand) throws Exception {
    this.isLigand = isLigand;
    int pdbModelNo = -1; // PDBX
    boolean haveCoord = true;
    boolean noPreviousReferences = asc.atomSymbolicMap.isEmpty();
    parseLoopParametersFor(CAT_ATOM_SITE, atomFields);
    if (key2col[CC_ATOM_X_IDEAL] != NONE) {
      setFractionalCoordinates(false);
    } else if (key2col[CARTN_X] != NONE || key2col[CC_ATOM_X] != NONE) {
      setFractionalCoordinates(false);
      disableField(FRACT_X);
      disableField(FRACT_Y);
      disableField(FRACT_Z);
      if (key2col[GROUP_PDB] != NONE && !isMMCIF) {
        // this should not happen
        setIsPDB();
        isMMCIF = true;
      }
    } else if (key2col[FRACT_X] != NONE) {
      setFractionalCoordinates(true);
      disableField(CARTN_X);
      disableField(CARTN_Y);
      disableField(CARTN_Z);
    } else if (key2col[ANISO_LABEL] != NONE || key2col[ANISO_MMCIF_ID] != NONE
        || key2col[MOMENT_LABEL] != NONE
        || key2col[spin_moment_label] != NONE) {
      haveCoord = false;
      // no coordinates, but valuable information
    } else {
      // it is a different kind of _atom_site loop block
      skipLoop(false);
      return false;
    }
    if (key2col[SPIN_U_PRELIM] != NONE || key2col[spin_moment_axis_u] != NONE) {
      disableField(MOMENT_X);
      disableField(MOMENT_Y);
      disableField(MOMENT_Z);      
    }
    int modelField = key2col[MODEL_NO];
    int siteMult = 0;
    String atomLabels = (isMMCIF ? null : "");
    while (cifParser.getData()) {
      if (modelField >= 0) {
        // mmCIF only
        pdbModelNo = checkPDBModelField(modelField, pdbModelNo);
        if (pdbModelNo < 0)
          break;
        if (skipping)
          continue;
      }
      Atom atom = null;
      String atomName = null;
      if (isMMCIF) {
        if (haveCoord) {
          atom = new Atom();
        } else {
          if (fieldProperty(key2col[ANISO_LABEL]) != NONE
              || fieldProperty(key2col[ANISO_MMCIF_ID]) != NONE
              || fieldProperty(key2col[MOMENT_LABEL]) != NONE) {
            if ((atom = asc.getAtomFromName((String) field)) == null)
              continue; // atom has been filtered out
          } else {
            continue;
          }
        }
      } else {
        // check for atom reference before atom definition
        int fNewAtomSet = NONE;
        int f0 = NONE;
        byte label = NONE; // will be set to the fNewAtomSet type
        if ((f0 = fNewAtomSet = fieldProperty(key2col[LABEL])) != NONE && (label = LABEL) != NONE
            || (fNewAtomSet = fieldProperty(key2col[CC_ATOM_ID])) != NONE && (label = CC_ATOM_ID) != NONE
            || (fNewAtomSet = fieldProperty(key2col[LABEL_ATOM_ID])) != NONE && (label = LABEL_ATOM_ID) != NONE
            || (f0 = fNewAtomSet = fieldProperty(key2col[ANISO_LABEL])) != NONE && (label = ANISO_LABEL) != NONE
            || (fNewAtomSet = fieldProperty(key2col[ANISO_MMCIF_ID])) != NONE && (label = ANISO_MMCIF_ID) != NONE
            || (f0 = fNewAtomSet = fieldProperty(key2col[MOMENT_LABEL])) != NONE && (label = MOMENT_LABEL) != NONE
            || (f0 = fNewAtomSet = fieldProperty(key2col[spin_moment_label])) != NONE && (label = spin_moment_label) != NONE
            ) {
          if (f0 != NONE && atomLabels != null) {
            atom = asc.getAtomFromName((String) field);
            if (addAtomLabelNumbers || atom != null) {
              String key = ";" + field + ";";
              if (noPreviousReferences) {
                atomLabels += key;
              }
              // just check THIS loop
              if (atomLabels.indexOf(key) < 0) {
                atomLabels += key;
              } else {
                field = atomName = ((String) field) + (asc.ac + 1);
                System.err.println(
                    "CifReader found duplicate atom_site_label! New label is "
                        + field);
                // user request to allow this to be automatic. 
                // note, however, that this will only work if atom_site_label is the FIRST 
                // reference to this atom
                // well, Jmol 14.30 did not produce valid CIF files in that it 
                // did not ensure that _atom_site_label was unique :(
                atom = null;
              }
            }
          }
        }
        String field = (String) this.field;
        if (atom == null) {
          atom = new Atom();
          if (fNewAtomSet != NONE) {
            if (asc.iSet < 0 && newAtomSetLabel == NONE) {
              nextAtomSet();
              asc.newAtomSet();
              newAtomSetLabel = label;
            }
            asc.atomSymbolicMap.put(field, atom);
          }
        }
      }
      String componentId = null;
      String id = null;
      String authAtom = null;
      String authComp = null;
      int authSeq = Integer.MIN_VALUE;
      String authAsym = null;
      String wyckoff = null;
      boolean haveAuth = false;
      int seqID = 0;
      int n = cifParser.getColumnCount();
      for (int i = 0; i < n; ++i) {
        int tok = fieldProperty(i);
        String field = (String) this.field;
        switch (tok) {
        case NONE:
          break;
        case ATOM_ID:
          // MMCIF only
          id = field;
          break;
        case CC_ATOM_SYM:
        case TYPE_SYMBOL:
          String elementSymbol;
          if (field.length() < 2) {
            elementSymbol = field;
          } else {
            char ch1 = Character.toLowerCase(field.charAt(1));
            if (Atom.isValidSym2(firstChar, ch1)) {
              elementSymbol = "" + firstChar + ch1;
            } else {
              elementSymbol = "" + firstChar;
              if (!haveHAtoms && firstChar == 'H')
                haveHAtoms = true;
            }
          }
          atom.elementSymbol = elementSymbol;
          atom.typeSymbol = field;
          break;
        case CC_ATOM_ID:
        case LABEL:
        case LABEL_ATOM_ID:
          atom.atomName = (atomName == null ? field : atomName);
          break;
        case AUTH_ATOM_ID:
          haveAuth = true;
          authAtom = field;
          break;
        case CC_COMP_ID:
        case LABEL_COMP_ID:
          atom.group3 = field;
          break;
        case AUTH_COMP_ID:
          authComp = field;
          haveAuth = true;
          break;
        case LABEL_ASYM_ID:
          componentId = field;
          break;
        case AUTH_ASYM_ID:
          authAsym = field;
          haveAuth = true;
          break;
        case LABEL_SEQ_ID:
          atom.sequenceNumber = seqID = parseIntField();
          break;
        case WYCKOFF_LABEL:
          if (allowWyckoff) {
            wyckoff = field;
          }
          break;
        case AUTH_SEQ_ID:
          haveAuth = true;
          authSeq = parseIntField();
          break;
        case CC_ATOM_X_IDEAL:
          double x = parseDoubleField();
          if (readIdeal && !Double.isNaN(x))
            atom.x = x;
          break;
        case CC_ATOM_Y_IDEAL:
          double y = parseDoubleField();
          if (readIdeal && !Double.isNaN(y))
            atom.y = y;
          break;
        case CC_ATOM_Z_IDEAL:
          double z = parseDoubleField();
          if (readIdeal && !Double.isNaN(z))
            atom.z = z;
          break;
        case FRACT_X:
          atom.x = parsePrecision(field);
          break;
        case CC_ATOM_X:
        case CARTN_X:
          atom.x = parseCartesianField();
          break;
        case FRACT_Y:
          atom.y = parsePrecision(field);
          break;
        case CC_ATOM_Y:
        case CARTN_Y:
          atom.y = parseCartesianField();
          break;
        case FRACT_Z:
          atom.z = parsePrecision(field);
          break;
        case CC_ATOM_Z:
        case CARTN_Z:
          atom.z = parseCartesianField();
          break;
        case CC_ATOM_CHARGE:
          atom.formalCharge = parseIntField();
          break;
        case OCCUPANCY:
          double doubleOccupancy = parseDoubleField();
          if (!Double.isNaN(doubleOccupancy))
            atom.foccupancy = doubleOccupancy;
          break;
        case B_ISO:
          atom.bfactor = parseDoubleField() * (isMMCIF ? 1 : 100d);
          break;
        case INS_CODE:
          atom.insertionCode = firstChar;
          break;
        case ALT_ID:
        case SUBSYS_ID:
          atom.altLoc = firstChar;
          break;
        case DISORDER_ASSEMBLY:
          disorderAssembly = field;
          break;
        case DISORDER_GROUP:
          if (!isMMCIF) {
            atom.part = PT.parseInt(field);
          }
          atom.altLoc = (atom.part < 0 ? field.charAt(1) : firstChar);

            // disorder group -1

            // email exchange with Brian McMahon 22.10.11
            //            see
            //            https://journals.iucr.org/c/issues/2015/01/00/fa3356/ (in the below I
            //            take PART to be the same as
            //            _atom_site_disorder_group):
            //
            //            "The use of PART numbers, introduced in SHELXL93, has proved invaluable
            //            in the refinement of disordered structures. Two atoms are considered to
            //            be bonded if they have the same PART number or if one of them is in
            //            PART 0. The resulting connectivity table is used for the generation of
            //            H atoms (HFIX and AFIX), for setting up restraints such as DELU, SIMU,
            //            RIGU, CHIV, BUMP and SAME, and for generating tables of geometric
            //            parameters (BOND, CONF, HTAB). Usually, most of the atoms are in
            //            PART 0, but, for example, a molecule or side chain dis­ordered over
            //            three positions could use PART 1, PART 2 and PART 3. If the PART
            //            number is negative, bonds are not generated to symmetry-equivalent
            //            atoms. It should be noted that positive PART numbers 1, 2, 3 etc.
            //            correspond to the alternative location indicators A, B, C etc. in
            //            PDB format. However, this notation is difficult to use when there
            //            is a disorder within a disorder."

            // atom.part < 0 indicates the negative case; atom.altloc is the "n" of "-n"
            // as symmetry is applied, if atom.isNegDisorder is true, then 
            // the cloned atom is given an incremented altloc
            // this only works with C2 and m; with higher-order symmetry, this
            // will dump all the symmetry-related groups into the same configuration=2

          break;
        case GROUP_PDB:
          if ("HETATM".equals(field))
            atom.isHetero = true;
          break;
        case DUMMY_ATOM:
          //see http://www.iucr.org/iucr-top/cif/cifdic_html/1/cif_core.dic/Iatom_site_calc_flag.html
          if ("dum".equals(field)) {
            atom.x = Double.NaN;
            continue; //skip 
          }
          break;
        case SITE_SYMMETRY_MULTIPLICITY:
        case SYMMETRY_MULT:
          siteMult = parseIntField();
          break;
        case THERMAL_TYPE:
        case ADP_TYPE:
          if (field.equalsIgnoreCase("Uiso")) {

            int j = key2col[U_ISO_OR_EQUIV];
            if (j != NONE)
              asc.setU(atom, 7, getDoubleColumnData(j));
          }
          break;
        case ANISO_U11:
        case ANISO_U22:
        case ANISO_U33:
        case ANISO_U12:
        case ANISO_U13:
        case ANISO_U23:
        case ANISO_MMCIF_U11:
        case ANISO_MMCIF_U22:
        case ANISO_MMCIF_U33:
        case ANISO_MMCIF_U12:
        case ANISO_MMCIF_U13:
        case ANISO_MMCIF_U23:
          // Ortep Type 8: D = 2pi^2, C = 2, a*b*
          asc.setU(atom, (col2key[i] - ANISO_U11) % 6, parseDoubleField());
          break;
        case ANISO_B11:
        case ANISO_B22:
        case ANISO_B33:
        case ANISO_B12:
        case ANISO_B13:
        case ANISO_B23:
          // Ortep Type 4: D = 1/4, C = 2, a*b*
          asc.setU(atom, 6, 4);
          asc.setU(atom, (col2key[i] - ANISO_B11) % 6, parseDoubleField());
          break;
        case ANISO_BETA_11:
        case ANISO_BETA_22:
        case ANISO_BETA_33:
        case ANISO_BETA_12:
        case ANISO_BETA_13:
        case ANISO_BETA_23:
          //Ortep Type 0: D = 1, c = 2 -- see org.jmol.symmetry/UnitCell.java
          asc.setU(atom, 6, 0);
          asc.setU(atom, (col2key[i] - ANISO_BETA_11) % 6, parseDoubleField());
          break;
        case MOMENT_PRELIM_X:
        case MOMENT_PRELIM_Y:
        case MOMENT_PRELIM_Z:
        case MOMENT_X:
        case MOMENT_Y:
        case MOMENT_Z:
        case SPIN_U_PRELIM:
        case SPIN_V_PRELIM:
        case SPIN_W_PRELIM:
        case spin_moment_axis_u:
        case spin_moment_axis_v:
        case spin_moment_axis_w:
        case spin_moment_magnitude:
        case spin_moment_symmform_uvw:
          haveMagneticMoments = true;
          Vibration pt = (Vibration) atom.vib;
          if (pt == null)
            atom.vib = pt = new Vibration().setType(Vibration.TYPE_SPIN);
          double v = parseDoubleField();
          switch (tok) {
          case spin_moment_magnitude:
            pt.magMoment = v;
            continue;
          case spin_moment_symmform_uvw:
            pt.symmform = field;
            continue;
          case MOMENT_PRELIM_X:
          case MOMENT_X:
          case SPIN_U_PRELIM:
          case spin_moment_axis_u:
            pt.x = v;
            appendLoadNote("magnetic moment: " + line);
            break;
          case MOMENT_PRELIM_Y:
          case MOMENT_Y:
          case SPIN_V_PRELIM:
          case spin_moment_axis_v:
            pt.y = v;
            break;
          case MOMENT_PRELIM_Z:
          case MOMENT_Z:
          case SPIN_W_PRELIM:
          case spin_moment_axis_w:
            pt.z = v;
            if (pt.length() == 0 && modDim == 0) {
              atom.vib = null;
            } else {
              pt.isFractional = true;
            }
            break;
          }
          break;
        }
      }
      if (!haveCoord)
        continue;
      if (Double.isNaN(atom.x) || Double.isNaN(atom.y)
          || Double.isNaN(atom.z)) {
        Logger
            .warn("atom " + atom.atomName + " has invalid/unknown coordinates");
        continue;
      }
      // auth_xxx are optional; label_xxx are required
      if (siteMult > 0 && wyckoff != null && wyckoff.length() > 0)
        seqID = (siteMult << 16) + wyckoff.charAt(0);
      String strChain = componentId;
      if (haveAuth) {
        if (authAtom != null)
          atom.atomName = authAtom;
        if (authComp != null)
          atom.group3 = authComp;
        if (authSeq != Integer.MIN_VALUE)
          atom.sequenceNumber = authSeq;
        if (authAsym != null && useAuthorChainID)
          strChain = authAsym;
      }
      if (strChain != null) {
        setChainID(atom, strChain);
      }
      if (maxSerial != Integer.MIN_VALUE)
        maxSerial = Math.max(maxSerial, atom.sequenceNumber);
      if (!addCifAtom(atom, id, componentId, strChain))
        continue;
      if ((id != null || wyckoff != null) && seqID > 0) {
        // co-op vibration vector when we have both id and seqID
        V3d pt = atom.vib;
        if (pt == null)
          pt = asc.addVibrationVector(atom.index, 0, Double.NaN, T.seqid);
        pt.x = seqID;
      }
      if (modDim > 0 && siteMult != 0)
        atom.vib = V3d.new3(siteMult, 0, Double.NaN);
    }
    asc.setCurrentModelInfo("isCIF", Boolean.TRUE);
    if (isMMCIF)
      setModelPDB(true);
    if (isMMCIF && skipping)
      skipping = false;
    return true;
  }

  protected double parseCartesianField() {
    return parseDoubleField();
  }

  protected boolean addCifAtom(Atom atom, String id, String componentId,
                               String strChain) {
    if (atom.elementSymbol == null && atom.atomName != null)
      atom.getElementSymbol();
    if (!filterCIFAtom(atom, componentId))
      return false;
    setAtomCoord(atom);
    if (isMMCIF && !processSubclassAtom(atom, componentId, strChain))
      return false;
    if (asc.iSet < 0)
      nextAtomSet();
    asc.addAtomWithMappedName(atom);
    if (id != null) {
      asc.atomSymbolicMap.put(id, atom);
    }
    ac++;
    return true;
  }

  /**
   * @param modelField
   * @param currentModelNo
   * @return new currentModelNo
   * @throws Exception
   */
  protected int checkPDBModelField(int modelField, int currentModelNo)
      throws Exception {
    // overridden in MMCIF reader
    return 0;
  }

  /**
   * @param atom
   * @param assemblyId
   * @param strChain
   * @return true if valid atom
   */
  protected boolean processSubclassAtom(Atom atom, String assemblyId,
                                        String strChain) {
    return true;
  }

  protected boolean filterCIFAtom(Atom atom, String componentId) {
    if (!filterAtom(atom, -1))
      return false;
    if (filterAssembly && filterReject(filter, "$", componentId))
      return false;
    if (configurationPtr > 0) {
      if (!disorderAssembly.equals(lastDisorderAssembly)) {
        lastDisorderAssembly = disorderAssembly;
        lastAltLoc = '\0';
        conformationIndex = configurationPtr;
      }
      // ignore atoms that have no designation
      if (atom.altLoc != '\0') {
        // count down until we get the desired index into the list
        if (conformationIndex >= 0 && atom.altLoc != lastAltLoc) {
          lastAltLoc = atom.altLoc;
          conformationIndex--;
        }
        if (conformationIndex != 0) {
          Logger.info("ignoring " + atom.atomName);
          return false;
        }
      }
    }
    return true;
  }

  final private static byte CITATION_TITLE = 0;

  final private static String[] citationFields = { "_citation_title" };

  protected void processCitationListBlock() throws Exception {
    parseLoopParameters(citationFields);
    while (cifParser.getData()) {
      String title = getFieldString(CITATION_TITLE);
      if (!isNull(title))
        appendLoadNote("TITLE: " + cifParser.toUnicode(title));
    }
  }

  ////////////////////////////////////////////////////////////////
  // symmetry operations
  ////////////////////////////////////////////////////////////////

  //loop_
  //_space_group_symop.magn_centering_id
  //_space_group_symop.magn_centering_xyz
  //1 x+2/3,y+1/3,z+1/3,+1 mx,my,mz
  //2 x+1/3,y+2/3,z+2/3,+1 mx,my,mz
  //3 x,y,z,+1 mx,my,mz

  //_space_group_symop.magn_ssg_centering_id
  //_space_group_symop.magn_ssg_centering_algebraic
  // 1 x1,x2,x3,x4,+1
  // 2 x1,x2,x3,x4+1/2,-1 

  final private static byte SYM_XYZ = 0;
  final private static byte SYM_MAGN_XYZ = 1;

  final private static byte SYM_SSG_ALG = 2;
  final private static byte SYM_MAGN_SSG_ALG = 3;
  final private static byte SYM_EQ_XYZ = 4;
  final private static byte SYM_SSG_EQ_XYZ = 5;

  final private static byte SYM_MAGN_REV = 7;
  final private static byte SYM_MAGN_SSG_REV = 8;
  final private static byte SYM_MAGN_REV_PRELIM = 6;

  final private static byte SYM_MAGN_CENTERING = 9;
  final private static byte SYM_MAGN_SSG_CENTERING = 10;
  final private static byte SYM_MAGN_SSG_CENT_XYZ = 11;

  final private static byte SYM_SPIN_LATTICE_XYZT = 12;
  final private static byte SYM_SPIN_LATTICE_UVW = 13;
  final private static byte SYM_SPIN_LATTICE_UVW_ID = 14;
  final private static byte SYM_SPIN_OP_XYZT = 15;
  final private static byte SYM_SPIN_OP_UVW = 16;
  final private static byte SYM_SPIN_OP_UVW_ID = 17;
  final private static byte SYM_SPIN_UPART_ID = 18;
  final private static byte SYM_SPIN_UPART_UVW = 19;
  
  final private static String CAT_SGOP = "_space_group_symop";
  final private static String[] symmetryOperationsFields = { 
      "*_operation_xyz",
      "*_magn_operation_xyz",
      "*_ssg_operation_algebraic", 
      "*_magn_ssg_operation_algebraic",
      "_symmetry_equiv_pos_as_xyz", // old
      "_symmetry_ssg_equiv_pos_as_xyz", // old

      "*_magn_operation_timereversal", // second iteration
      "*_magn_ssg_operation_timereversal", // another iteration
      "*_operation_timereversal", // preliminary only

      "*_magn_centering_xyz", 
      "*_magn_ssg_centering_algebraic",
      "*_magn_ssg_centering_xyz", // preliminary
      
      "*_spin_lattice_xyzt",
      "*_spin_lattice_uvw",
      "*_spin_lattice_uvw_id",

      "*_spin_operation_xyzt",
      "*_spin_operation_uvw",
      "*_spin_operation_uvw_id",
      
      "*_spin_upart_id",
      "*_spin_upart_uvw",
      
  };

  /**
   * retrieves symmetry operations
   * 
   * @throws Exception
   */
  private void processSymmetryOperationsLoopBlock() throws Exception {
    parseLoopParametersFor(CAT_SGOP, symmetryOperationsFields);
    int n;
    symops = new Lst<String>();
    for (n = symmetryOperationsFields.length; --n >= 0;)
      if (key2col[n] != NONE)
        break;
    if (n < 0) {
      Logger.warn("required " + CAT_SGOP + " key not found");
      skipLoop(false);
      return;
    }
    n = 0;
    boolean isMag = false;
    String sxyz = null;
    String suvw = null;
    String suvwMapped = null;
    String suvwId = null;
    while (cifParser.getData()) {
      boolean ssgop = false;
      String oxyz = null;
      String ouvw = null;
      int nn = cifParser.getColumnCount();
      int timeRev = (fieldProperty(key2col[SYM_MAGN_REV]) == NONE
          && fieldProperty(key2col[SYM_MAGN_SSG_REV]) == NONE
          && fieldProperty(key2col[SYM_MAGN_REV_PRELIM]) == NONE ? 0
              : ((String) field).equals("-1") ? -1 : 1);
      if (timeRev != 0)
        isMagCIF = true; // for old-style _magnetic_... because "_magnetic" has been removed in CifDataParser
      for (int i = 0; i < nn; ++i) {
        int tok = fieldProperty(i);
        String field = (String) this.field;
        switch (tok) {
        case SYM_SSG_EQ_XYZ:
          // check for non-standard record x~1~,x~2~,x~3~,x~4~  kIsfCqpM.cif
          if (field.indexOf('~') >= 0)
            field = PT.rep(field, "~", "");
          //$FALL-THROUGH$
        case SYM_SSG_ALG:
        case SYM_MAGN_SSG_ALG:
          modulated = true;
          ssgop = true;
          //$FALL-THROUGH$
        case SYM_XYZ:
        case SYM_EQ_XYZ:
        case SYM_MAGN_XYZ:
        case SYM_SPIN_OP_XYZT:
        case SYM_SPIN_OP_UVW:
        case SYM_SPIN_OP_UVW_ID:
          if (allowRotations || timeRev != 0 || ++n == 1)
            if (!modulated || ssgop) {
              switch (tok) {
              case SYM_MAGN_XYZ:
              case SYM_MAGN_SSG_ALG:
                isMag = isMagCIF = true;
                timeRev = getTimeReversal(field);
                if (timeRev != 0)
                  field = (String) this.field;
                break;
              case SYM_SPIN_OP_XYZT:
                isSpinCIF = true;
                timeRev = getTimeReversal(field);
                if (timeRev != 0)
                  field = (String) this.field;
                oxyz = field;
                if (ouvw == null) {
                  continue;
                }
                //$FALL-THROUGH$
              case SYM_SPIN_OP_UVW_ID:
                if (ouvw == null) {
                  haveSpinReferences = true;
                  ouvw = "u" + field;
                }
                //$FALL-THROUGH$
              case SYM_SPIN_OP_UVW:
                if (ouvw == null)
                  ouvw = field;
                if (oxyz == null) {
                  continue;
                }
                ouvw = parseUvwMath(ouvw);
                field = oxyz + "(" + ouvw + ")";
                timeRev = 0;// ignore here
              }
              if (timeRev != 0)
                field += "," + (timeRev == 1 ? "m" : "-m");
              field = field.replace(';', ' ');
              symops.addLast(field);
              setSymmetryOperator(field);
              if (modulated && modDim == 0 && modr != null)
                modDim = modr.modDim;
            }
          break;
        case SYM_SPIN_UPART_ID:
          suvwId = "u" + field;
          if (suvwMapped == null)
            continue;
          field = suvwMapped;
          //$FALL-THROUGH$
        case SYM_SPIN_UPART_UVW:
          suvwMapped = field;
          if (suvwId == null)
            continue;
          if (mapSpinIdToUVW == null)
            mapSpinIdToUVW = new Hashtable<String, String>();
          mapSpinIdToUVW.put(suvwId, suvwMapped);
          break;
        case SYM_MAGN_CENTERING:
        case SYM_MAGN_SSG_CENTERING:
        case SYM_MAGN_SSG_CENT_XYZ:
          isMag = isMagCIF = true;
          if (magCenterings == null)
            magCenterings = new Lst<String>();
          magCenterings.addLast(field);
          break;
        case SYM_SPIN_LATTICE_XYZT:
          timeRev = getTimeReversal(field);
          sxyz = (String) this.field;
          break;
        case SYM_SPIN_LATTICE_UVW_ID:
          field = "u" + field;
          haveSpinReferences = true;
          //$FALL-THROUGH$
        case SYM_SPIN_LATTICE_UVW:
          suvw = field;
          break;
        }
      }
      if (sxyz != null) {
        if (suvw != null) {
          isMag = isSpinCIF = true;
          if (lstSpinLattices == null) {
            lstSpinLattices = new Lst<String>();
          }
          suvw = parseUvwMath(suvw);
          lstSpinLattices.addLast(sxyz + "(" + suvw + ")");
          suvw = suvwMapped = suvwId = null;
        }
        sxyz = null;
      }
    }
    if (ms != null && !isMag) {
      addLatticeVectors();
    }
  }

  /**
   * converts sqrt(3) to decimal format
   * @param suvw
   * @return converted string
   */
  private String parseUvwMath(String suvw) {
    return SimpleUnitCell.parseSimpleMath(vwr, suvw);
  }

  private int getTimeReversal(String field) {
    int tr = (field.endsWith(",+1") || field.endsWith(",1") ? 1
        : field.endsWith(",-1") ? -1 : 0);
    if (tr != 0)
      this.field = field.substring(0, field.lastIndexOf(','));
    return tr;
  }

  public int getBondOrder(String field) {
    switch (field.toUpperCase().charAt(0)) {
    default:
      Logger.warn("unknown CIF bond order: " + field);
      //$FALL-THROUGH$
    case '\0':
    case 'S':
      return JmolAdapter.ORDER_COVALENT_SINGLE;
    case 'D':
      return JmolAdapter.ORDER_COVALENT_DOUBLE;
    case 'T':
      return JmolAdapter.ORDER_COVALENT_TRIPLE;
    case 'Q':
      return JmolAdapter.ORDER_COVALENT_QUAD;
    case 'A':
      haveAromatic = true;
      return JmolAdapter.ORDER_AROMATIC;
    }
  }

  final private static byte GEOM_BOND_ATOM_SITE_LABEL_1 = 0;
  final private static byte GEOM_BOND_ATOM_SITE_LABEL_2 = 1;
  final private static byte GEOM_BOND_DISTANCE = 2;
  final private static byte CCDC_GEOM_BOND_TYPE = 3;

  //final private static byte GEOM_BOND_SITE_SYMMETRY_2 = 3;

  final private static String[] geomBondFields = {
      "_geom_bond_atom_site_label_1", "_geom_bond_atom_site_label_2",
      "_geom_bond_distance", "_ccdc_geom_bond_type"
      //  "_geom_bond_site_symmetry_2",
  };

  /**
   * 
   * reads bond data -- N_ijk symmetry business is ignored, so we only indicate
   * bonds within the unit cell to just the original set of atoms. "connect"
   * script or "set forceAutoBond" will override these values, but see below.
   * 
   * @throws Exception
   */
  private void processGeomBondLoopBlock() throws Exception {
    // broken in 13.3.4_dev_2013.08.20c
    // fixed in 14.4.3_2016.02.16
    boolean ok = !modulated && (isMolecular || !doApplySymmetry
        && !ignoreGeomBonds && !(stateScriptVersionInt >= 130304
            && stateScriptVersionInt < 140403));
    if (ok) {
      parseLoopParameters(geomBondFields);
      ok = checkAllFieldsPresent(geomBondFields, 2, true);
    }
    if (!ok) {
      skipLoop(false);
      return;
    }
    int bondCount = 0;
    while (cifParser.getData()) {
      String name1 = getFieldString(GEOM_BOND_ATOM_SITE_LABEL_1);
      String name2 = getFieldString(GEOM_BOND_ATOM_SITE_LABEL_2);
      int order = getBondOrder(getFieldString(CCDC_GEOM_BOND_TYPE));
      String sdist = getFieldString(GEOM_BOND_DISTANCE);
      double distance = parseDoubleStr(sdist);
      if (distance == 0 || Double.isNaN(distance)) {
        if (!iHaveFractionalCoordinates) {
          // maybe this is a simple Cartesian file with coordinates and bonds
          Atom a = getAtomFromNameCheckCase(name1);
          Atom b = getAtomFromNameCheckCase(name2);
          if (a == null || b == null) {
            System.err.println("ATOM_SITE atom for name "
                + (a != null ? name2
                    : b != null ? name1 : name1 + " and " + name2)
                + " not found");
            continue;
          }
          asc.addNewBondWithOrder(a.index, b.index, order);
        }
        continue;
      }
      double dx = getStandardDeviation(sdist);
      // This field is from Materials Studio. See supplemental material for
      // http://pubs.rsc.org/en/Content/ArticleLanding/2012/CC/c2cc34714h
      // http://www.rsc.org/suppdata/cc/c2/c2cc34714h/c2cc34714h.txt
      // Jmol list discussion: https://sourceforge.net/p/jmol/mailman/message/31308577/
      // this 5-model file can be read using one model at a time: load "c2cc34714h.txt" 3
      // but it is far from perfect, and still the best way is load "c2cc34714h.txt" 3 packed
      bondCount++;
      bondTypes.addLast(new Object[] { name1, name2, Double.valueOf(distance),
          Double.valueOf(dx), Integer.valueOf(order) });
    }
    if (bondCount > 0) {
      Logger.info(bondCount + " bonds read");
      if (!doApplySymmetry) {
        isMolecular = true;
        forceSymmetry(false);
      }
    }
  }

  /////////////////////////////////////
  //  bonding and molecular 
  /////////////////////////////////////

  private double getStandardDeviation(String sdist) {
    int pt = sdist.indexOf('(');
    if (pt >= 0) {
      char[] data = sdist.toCharArray();
      // 3.567(12) --> 0.012
      String sdx = sdist.substring(pt + 1, data.length - 1);
      int n = sdx.length();
      for (int j = pt; --j >= 0;) {
        if (data[j] == '.' && --j < 0)
          break;
        data[j] = (--n < 0 ? '0' : data[pt + 1 + n]);
      }
      double dx = parseDoubleStr(String.valueOf(data));
      if (!Double.isNaN(dx)) {
        return dx;
      }
    }
    Logger.info("CifReader error reading uncertainty for " + sdist
        + " (set to 0.015) on line " + line);
    return 0.015;
  }

  /**
   * From GEOM_BOND, check first for an exact match. If that is not found, add
   * all-caps keys and try again with all upper case.
   * 
   * This should solve the problem of GEOM_BOND_* using the wrong case while
   * still preserving functionality when H15a and H15A are both present (COD
   * 7700953 https://www.crystallography.net/cod/7700953.html)
   * 
   * @param name
   * @return atom
   */
  private Atom getAtomFromNameCheckCase(String name) {
    Atom a = asc.getAtomFromName(name);
    if (a == null) {
      if (!asc.atomMapAnyCase) {
        asc.setAtomMapAnyCase();
      }
      a = asc.getAtomFromName(name.toUpperCase());
    }
    return a;
  }

  private double[] atomRadius;
  private BS[] bsConnected;
  private BS[] bsSets;
  final private P3d ptOffset = new P3d();
  private BS bsMolecule;
  private BS bsExclude;
  private int firstAtom;

  private Atom[] atoms;
  private BS bsBondDuplicates;

  /**
   * (1) If GEOM_BOND records are present, we (a) use them to generate bonds (b)
   * add H atoms to bonds if necessary (c) turn off autoBonding ("hasBonds") (2)
   * If MOLECULAR, then we (a) use {1 1 1} if lattice is not defined (b) use
   * asc.bonds[] to construct a preliminary molecule and connect as we go (c)
   * check symmetry for connections to molecule in any one of the 27 3x3
   * adjacent cells (d) move those atoms and their connected branch set (e)
   * iterate as necessary to get all atoms desired (f) delete unselected atoms
   * (g) set all coordinates as Cartesians (h) remove all unit cell information
   */
  private void setBondingAndMolecules() {
    atoms = asc.atoms;
    firstAtom = asc.getLastAtomSetAtomIndex();
    int nat = asc.getLastAtomSetAtomCount();
    ac = firstAtom + nat;
    Logger.info("CIF creating molecule for " + nat + " atoms "
        + (bondTypes.size() > 0 ? " using GEOM_BOND records" : ""));

    // get list of sites based on atom names

    bsSets = new BS[nat];
    symmetry = asc.getSymmetry();
    for (int i = firstAtom; i < ac; i++) {
      int ipt = asc.getAtomFromName(atoms[i].atomName).index - firstAtom;
      if (ipt < 0)
        continue;
      if (bsSets[ipt] == null)
        bsSets[ipt] = new BS();
      bsSets[ipt].set(i - firstAtom);
    }

    // if molecular, we need atom connection lists and radii

    if (isMolecular) {
      atomRadius = new double[ac];
      for (int i = firstAtom; i < ac; i++) {
        int elemnoWithIsotope = JmolAdapter
            .getElementNumber(atoms[i].getElementSymbol());
        atoms[i].elementNumber = (short) elemnoWithIsotope;
        int charge = (atoms[i].formalCharge == Integer.MIN_VALUE ? 0
            : atoms[i].formalCharge);
        if (elemnoWithIsotope > 0)
          atomRadius[i] = JmolAdapter.getBondingRadius(elemnoWithIsotope,
              charge);
      }
      bsConnected = new BS[ac];
      for (int i = firstAtom; i < ac; i++)
        bsConnected[i] = new BS();

      // Set up a working set of atoms in the "molecule".

      bsMolecule = new BS();

      // Set up a working set of atoms that should be excluded 
      // because they would map onto an equivalent atom's position.

      bsExclude = new BS();
    }

    boolean isFirst = true;
    bsBondDuplicates = new BS();
    while (createBonds(isFirst)) {
      isFirst = false;
      // main loop continues until no new atoms are found
    }

    if (isMolecular && iHaveFractionalCoordinates && !bsMolecule.isEmpty()) {

      // Set bsAtoms to control which atoms and 
      // bonds are delivered by the iterators.

      BS bs = asc.getBSAtoms(asc.bsAtoms == null ? firstAtom : 0);
      bs.clearBits(firstAtom, ac);
      bs.or(bsMolecule);
      bs.andNot(bsExclude);

      // Set atom positions to be Cartesians and clear out unit cell
      // so that the model displays without it.

      for (int i = firstAtom; i < ac; i++) {
        if (bs.get(i))
          symmetry.toCartesian(atoms[i], true);
        else if (debugging)
          Logger.debug(molecularType + " removing " + i + " "
              + atoms[i].atomName + " " + atoms[i]);
      }
      asc.setCurrentModelInfo(JC.INFO_UNIT_CELL_PARAMS, null);
      if (nMolecular++ == asc.iSet) {
        asc.clearGlobalBoolean(JC.GLOBAL_FRACTCOORD);
        asc.clearGlobalBoolean(JC.GLOBAL_SYMMETRY);
        asc.clearGlobalBoolean(JC.GLOBAL_UNITCELLS);
      }

    }

    // Set return info to enable desired defaults.

    if (bondTypes.size() > 0)
      asc.setCurrentModelInfo("hasBonds", Boolean.TRUE);

    // Clear temporary fields.

    bondTypes.clear();
    atomRadius = null;
    bsSets = null;
    bsConnected = null;
    bsMolecule = null;
    bsExclude = null;
  }

  private void fixAtomForBonding(P3d pt, int i) {
    pt.setT(atoms[i]);//pt = (pt == null ? atoms[i] : P3.newP(atoms[i]));
    if (iHaveFractionalCoordinates)
      symmetry.toCartesian(pt, true);
  }

  /**
   * Use the site bitset to check for atoms that are within +/-dx Angstroms of
   * the specified distances in GEOM_BOND where dx is determined by the
   * uncertainty (dx) in the record. Note that this also "connects" the atoms
   * that might have been moved in a previous iteration.
   * 
   * Also connect H atoms based on a distance <= 1.1 Angstrom from a nearby
   * atom.
   * 
   * Then create molecules.
   * 
   * @param doInit
   * @return TRUE if need to continue
   */
  private boolean createBonds(boolean doInit) {
    // process GEOM_BOND records
    String list = "";
    boolean haveH = false;
    for (int i = bondTypes.size(); --i >= 0;) {
      if (bsBondDuplicates.get(i))
        continue;
      Object[] o = bondTypes.get(i);
      double distance = ((Number) o[2]).doubleValue();
      double dx = ((Number) o[3]).doubleValue();
      int order = ((Integer) o[4]).intValue();
      Atom a1 = getAtomFromNameCheckCase((String) o[0]);
      Atom a2 = getAtomFromNameCheckCase((String) o[1]);
      if (a1 == null || a2 == null) {
        System.err.println("CifReader checking GEOM_BOND " + o[0] + "-" + o[1] + " found " + a1 + " " + a2);
        continue;
      }
      if (Double.isNaN(a1.x) || Double.isNaN(a2.x)) {
        System.err.println("CifReader checking GEOM_BOND " + o[0] + "-" + o[1] + " found x coord NaN");
        continue;
      }
      int iatom1 = a1.index;
      int iatom2 = a2.index;
      if (doInit) {
        String key = ";" + iatom1 + ";" + iatom2 + ";" + distance;
        if (list.indexOf(key) >= 0) {
          bsBondDuplicates.set(i);
          continue;
        }
        list += key;
      }
      BS bs1 = bsSets[iatom1 - firstAtom];
      BS bs2 = bsSets[iatom2 - firstAtom];
      if (bs1 == null || bs2 == null)
        continue;
      if (atoms[iatom1].elementNumber == 1 || atoms[iatom2].elementNumber == 1)
        haveH = true;
      for (int j = bs1.nextSetBit(0); j >= 0; j = bs1.nextSetBit(j + 1)) {
        for (int k = bs2.nextSetBit(0); k >= 0; k = bs2.nextSetBit(k + 1)) {
          if ((!isMolecular || !bsConnected[j + firstAtom].get(k))
              && checkBondDistance(atoms[j + firstAtom], atoms[k + firstAtom],
                  distance, dx)) {
            addNewBond(j + firstAtom, k + firstAtom, order);
          }
        }
      }
    }

    if (!iHaveFractionalCoordinates)
      return false;

    // do a quick check for H-X bonds if we have GEOM_BOND

    if (bondTypes.size() > 0 && !haveH)
      for (int i = firstAtom; i < ac; i++)
        if (atoms[i].elementNumber == 1) {
          boolean checkAltLoc = (atoms[i].altLoc != '\0');
          for (int k = firstAtom; k < ac; k++)
            if (k != i && atoms[k].elementNumber != 1
                && (!checkAltLoc || atoms[k].altLoc == '\0'
                    || atoms[k].altLoc == atoms[i].altLoc)) {
              if (!bsConnected[i].get(k)
                  && checkBondDistance(atoms[i], atoms[k], 1.1d, 0))
                addNewBond(i, k, 1);
            }
        }
    if (!isMolecular)
      return false;

    // generate the base atom set

    if (doInit)
      for (int i = firstAtom; i < ac; i++)
        if (atoms[i].atomSite + firstAtom == i && !bsMolecule.get(i))
          setBs(atoms, i, bsConnected, bsMolecule);

    // Now look through unchecked atoms for ones that
    // are within bonding distance of the "molecular" set
    // in any one of the 27 adjacent cells in 444 - 666.
    // If an atom is found, move it along with its "branch"
    // to the new location. BUT also check that we are
    // not overlaying another atom -- if that happens
    // go ahead and move it, but mark it as excluded.

    double bondTolerance = vwr.getDouble(T.bondtolerance);
    BS bsBranch = new BS();
    P3d cart1 = new P3d();
    P3d cart2 = new P3d();
    int nFactor = 2; // 1 was not enough. (see data/cif/triclinic_issue.cif)
    for (int i = firstAtom; i < ac; i++)
      if (!bsMolecule.get(i) && !bsExclude.get(i))
        for (int j = bsMolecule.nextSetBit(0); j >= 0; j = bsMolecule
            .nextSetBit(j + 1))
          if (symmetry.checkDistance(atoms[j], atoms[i],
              atomRadius[i] + atomRadius[j] + bondTolerance, 0, nFactor,
              nFactor, nFactor, ptOffset)) {
            setBs(atoms, i, bsConnected, bsBranch);
            for (int k = bsBranch.nextSetBit(0); k >= 0; k = bsBranch
                .nextSetBit(k + 1)) {
              atoms[k].add(ptOffset);
              fixAtomForBonding(cart1, k);
              BS bs = bsSets[asc.getAtomIndex(atoms[k].atomName) - firstAtom];
              if (bs != null)
                for (int ii = bs.nextSetBit(0); ii >= 0; ii = bs
                    .nextSetBit(ii + 1)) {
                  if (ii + firstAtom == k)
                    continue;
                  fixAtomForBonding(cart2, ii + firstAtom);
                  if (cart2.distance(cart1) < 0.1d) {
                    bsExclude.set(k);
                    break;
                  }
                }
              bsMolecule.set(k);
            }
            return true;
          }
    return false;
  }

  private boolean checkBondDistance(Atom a, Atom b, double distance,
                                    double dx) {
    boolean ret;
    if (iHaveFractionalCoordinates) {
      ret = symmetry.checkDistance(a, b, distance, dx, 0, 0, 0, ptOffset);
    } else {
      double d = a.distance(b);
      ret = (dx > 0 ? Math.abs(d - distance) <= dx : d <= distance && d > 0.1d); // same as in Symmetry
    }
    return ret;
  }

  /**
   * add the bond and mark it for molecular processing
   * 
   * @param i
   * @param j
   * @param order
   */
  private void addNewBond(int i, int j, int order) {
    asc.addNewBondWithOrder(i, j, order);
    if (!isMolecular)
      return;
    bsConnected[i].set(j);
    bsConnected[j].set(i);
  }

  /**
   * iteratively run through connected atoms, adding them to the set
   * 
   * @param atoms
   * @param iatom
   * @param bsBonds
   * @param bs
   */
  private void setBs(Atom[] atoms, int iatom, BS[] bsBonds, BS bs) {
    BS bsBond = bsBonds[iatom];
    bs.set(iatom);
    for (int i = bsBond.nextSetBit(0); i >= 0; i = bsBond.nextSetBit(i + 1)) {
      if (!bs.get(i))
        setBs(atoms, i, bsBonds, bs);
    }
  }

  protected boolean checkSubclassSymmetry() {
    return doCheckUnitCell;
  }

  protected boolean checkAllFieldsPresent(String[] keys, int lastKey,
                                          boolean critical) {
    for (int i = (lastKey < 0 ? keys.length : lastKey); --i >= 0;)
      if (key2col[i] == NONE) {
        if (critical)
          Logger.warn("CIF reader missing property: " + keys[i]);
        return false;
      }
    return true;
  }

  protected boolean isNull(String key) {
    return key.equals("\0");
  }

  protected void skipLoop(boolean doReport) throws Exception {
    if (isLoop)
      cifParser.skipLoop(doReport);
  }

  public static String fullTrim(Object s) {
    String str = (String) s;
    int pt0 = -1;
    int pt1 = str.length();
    while (++pt0 < pt1 && PT.isWhitespace(str.charAt(pt0))) {
    }
    while (--pt1 > pt0 && PT.isWhitespace(str.charAt(pt1))) {
    }
    return str.substring(pt0, pt1 + 1);
  }

  protected boolean isFieldValid() {
    return (((String) field).length() > 0
        && (firstChar = ((String) field).charAt(0)) != '\0');
  }

  protected int parseIntField() {
    return parseIntStr((String) field);
  }

  protected double parseDoubleField() {
    return parseDoubleStr((String) field);
  }

  String key, key0;
  Object field;
  protected boolean isLoop;

  /**
   * 
   * @return TRUE if data, even if ''; FALSE if '.' or '?' or eof.
   * 
   * @throws Exception
   */
  private boolean getData() throws Exception {
    key = (String) cifParser.getTokenPeeked();
    if (!continueWith(key))
      return false;
    if (skipKey(key)) {
      field = cifParser.skipNextToken();
    } else {
      field = cifParser.getNextToken(); // the only place this is found
    }
    if (field == null) {
      Logger.warn("CIF ERROR ? end of file; data missing: " + key);
      return false;
    }
    String field = (String) this.field;
    return (field.length() == 0 || field.charAt(0) != '\0');
  }

  int[] col2key = new int[CifDataParser.KEY_MAX]; // 100
  int[] key2col = new int[CifDataParser.KEY_MAX];

  protected char firstChar = '\0';

  /**
   * sets up arrays and variables for tokenizer.getData() after the first tag of
   * the loop has been checked.
   * 
   * @param fieldNames
   * @throws Exception
   */
  protected void parseLoopParameters(String[] fieldNames) throws Exception {
    cifParser.parseDataBlockParameters(fieldNames, isLoop ? null : key0,
        (String) field, key2col, col2key);
  }

  protected String getFieldString(byte type) {
    int i = key2col[type];
    return (i <= NONE ? "\0" : (String) cifParser.getColumnData(i));
  }

  protected void skipLoopKeyword() {
    cifParser.getTokenPeeked(); //loop_
  }

  protected boolean isLoopKey() {
    return key.startsWith("loop_");
  }

  protected double getDoubleColumnData(int i) {
    return parseDoubleStr((String) cifParser.getColumnData(i));
  }

}
