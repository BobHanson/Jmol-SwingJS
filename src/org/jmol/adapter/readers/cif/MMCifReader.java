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

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.Structure;
import org.jmol.api.JmolAdapter;
import org.jmol.c.STR;
import org.jmol.util.BSUtil;
import org.jmol.util.Logger;
import org.jmol.util.SimpleUnitCell;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.M4d;
import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.SB;


/**
 * 
 * mmCIF files are recognized prior to class creation. 
 * Required fields include one of:
 * 
 *   _entry.id
 *   _database_PDB_
 *   _pdbx_
 *   _chem_comp.pdbx_type
 *   _audit_author.name
 *   _atom_site.
 * 
 * 
 * @author Bob Hanson (hansonr@stolaf.edu)
 * 
 */
public class MMCifReader extends CifReader {

  protected boolean isBiomolecule;
  private boolean byChain, bySymop;
  private Map<String, P3d> chainAtomMap;
  private Map<String, int[]> chainAtomCounts;

  protected Lst<Map<String, Object>> vBiomolecules;
  private Map<String, M4d> htBiomts;
  protected Map<String, Map<String, Object>> htSites;
  protected Map<String, String> htHetero;
  private Map<String, Lst<Object[]>> htBondMap;
  private Map<String, BS> assemblyIdAtoms;

  private int thisChain = -1;
  private int modelIndex = 0;
  
  private P3d chainSum;
  private int[] chainAtomCount;
  
  private boolean isLigandBondBug; 
  // Jmol-14.3.3_2014.07.27 broke mmCIF bond reading for ligands
  // Jmol-14.3.9_2014.11.11 fixes this. 

  M4d mident;

  @Override
  protected void initSubclass() {
    setIsPDB();
    mident = M4d.newM4(null);
    isMMCIF = true;
    if (isDSSP1)
      asc.setInfo("isDSSP1",Boolean.TRUE);      
    if (htParams.containsKey("isMutate"))
      asc.setInfo("isMutate",Boolean.TRUE);      
    doSetBonds = checkFilterKey("ADDBONDS");
    byChain = checkFilterKey("BYCHAIN");
    if (checkFilterKey("BIOMOLECULE")) // PDB format
      filter = PT.rep(filter, "BIOMOLECULE", "ASSEMBLY");
    isBiomolecule = checkFilterKey("ASSEMBLY");
    if (isBiomolecule) {
      filter = filter.replace(':', ' '); // no chain selection for biomolecules
      bySymop = checkFilterKey("BYSYMOP");
    }    
    isCourseGrained = byChain || bySymop;
    if (isCourseGrained) {
      chainAtomMap = new Hashtable<String, P3d>();
      chainAtomCounts = new Hashtable<String, int[]>();
    }
    // When this reader was split off from CifReader, a bug was introduced
    // into the Resolver that made it so that ligand files were read by 
    // CifReader and not MMCifReader. This caused CHEM_COMP_BOND records to be 
    // skipped and so in the case of pdbAddHydrogen no hydrogens added.
    isLigandBondBug = (stateScriptVersionInt >= 140204 && stateScriptVersionInt <= 140208
        || stateScriptVersionInt >= 140304 && stateScriptVersionInt <= 140308);

  }

  @Override
  protected void processSubclassEntry() throws Exception {
    if (key0.startsWith(CAT_ASSEM_CAT) 
        || key0.startsWith(CAT_STRUCTCONN_CAT)
        || key0.startsWith(CAT_SEQUENCEDIF_CAT)
        || key0.startsWith(CAT_STRUCTCONF_CAT)
        || key0.startsWith(CAT_SHEET_CAT)
        
//        || key0.startsWith(CAT_PDBX_NONPOLY_CAT)
        )
      processSubclassLoopBlock();
    else if (key.equals("_rna3d")) {
      addedData = (String) field;
      addedDataKey = key;
    } else if (key.equals("_dssr")) {
      dssr = vwr.parseJSONMap(reader.readLine());
      reader.readLine(); // sometimes there is a null character here
    }
  }
  
  @Override
  protected boolean processSubclassLoopBlock() throws Exception {
    if (key0.startsWith(CAT_NCS_CAT))
      return processStructOperListBlock(true);
    if (key0.startsWith(CAT_OPER_CAT))
      return processStructOperListBlock(false);
    if (key0.startsWith(CAT_ASSEM_CAT))
      return processAssemblyGenBlock();
    if (key0.startsWith(CAT_SEQUENCEDIF_CAT))
      return processSequence();

    if (isCourseGrained)
      return false;

    if (key0.startsWith(CAT_STRUCSITE_CAT))
      return processStructSiteBlock();
    if (key0.startsWith(CAT_CHEMCOMP_CAT))
      return processChemCompLoopBlock();
//    if (key0.startsWith(CAT_PDBX_NONPOLY_CAT))
//      return processNonpolyLoopBlock();
    if (key0.startsWith(CAT_STRUCTCONF_CAT))
      return processStructConfLoopBlock();
    if (key0.startsWith(CAT_SHEET_CAT))
      return processStructSheetRangeLoopBlock();

    // alas -- saved states must not read ligand bonding
    // the problem was that these files were not recognized as mmCIF 
    // files by the resolver when this MMCifReader was created.

    if (isLigandBondBug)
      return false;
    if (key0.startsWith(CAT_COMPBOND_CAT))
      return processCompBondLoopBlock();
    if (key0.startsWith(CAT_STRUCTCONN_CAT))
      return processStructConnLoopBlock();
    
    return false;

  }

  private boolean requiresSorting;

  /**
   * issue here is that mmCIF assembly atoms can be in different blocks by chain:
   * Model1:Chain1 Model2:Chain1 Model1:Chain2 Model2:Chain2 ... and so assigned
   * to too many atom sets.
   * 
   */
  protected void sortAssemblyModels() {
    int natoms = asc.ac;
    int lastSet = -1;
    Atom[] atoms = asc.atoms;
    Atom[] newAtoms = new Atom[natoms];
    String[] ids = PT.split("," + modelStrings + ",", ",,");
    BS bsAtomsNew = (asc.bsAtoms == null ? null : BS.newN(asc.bsAtoms.size()));
    for (int im = 1, n = 0; im < ids.length; im++) {
      String sModel = ids[im];
      int modelIndex = -1;
      for (int is = 0; is < asc.atomSetCount; is++) {
        int ia0 = asc.getAtomSetAtomIndex(is);
        int ia1 = ia0 + asc.getAtomSetAtomCount(is);
        String am = "" + modelMap.get("_" + is);
        if (am.equals(sModel)) {
          if (modelIndex < 0 && (modelIndex = is) > lastSet)
            lastSet = is;
          for (int i = ia0; i < ia1; i++) {
            if (bsAtomsNew == null || asc.bsAtoms.get(i)) {
              if (bsAtomsNew != null)
                bsAtomsNew.set(n);
              atoms[i].atomSetIndex = modelIndex;
              newAtoms[n++] = atoms[i];
            }
          }
        }
      }

    }
    asc.atoms = newAtoms;
    asc.bsAtoms = bsAtomsNew;
    if (++lastSet < asc.atomSetCount)
      asc.atomSetCount = lastSet;
  }


  @Override
  protected boolean finalizeSubclass() throws Exception {
    if (byChain && !isBiomolecule)
      for (String id : chainAtomMap.keySet())
        createParticle(id);
    boolean haveBiomolecule = (isBiomolecule && vBiomolecules != null && vBiomolecules.size() > 0);
    if (!isCourseGrained && asc.ac == nAtoms) {
      asc.removeCurrentAtomSet();
    } else {
      if ((dssr != null || validation != null || addedData != null) && !isCourseGrained && !requiresSorting) {
        MMCifValidationParser vs = ((MMCifValidationParser) getInterface("org.jmol.adapter.readers.cif.MMCifValidationParser"))
            .set(this);
        String note = null;
        if (addedData == null) {
          if (validation != null || dssr != null)
            note = vs.finalizeValidations(vwr, modelMap);
        } else if (addedDataKey.equals("_rna3d")) {
          note = vs.finalizeRna3d(modelMap);
        }
        if (note != null)
          appendLoadNote(note);
      }
      setHetero();
      if (doSetBonds)
        setBonds();
    }
    if (asc.ac == 0 && !isCourseGrained)
      return false;
    String spaceGroup = sgName;
    if (htSites != null)
      addSites(htSites);
    
    if (haveBiomolecule) {
      asc.setCurrentModelInfo("biomolecules", vBiomolecules);
      setBiomolecules();
      if (thisBiomolecule != null) {
        if (iHaveFractionalCoordinates)
          fractionalizeCoordinates(false);
        asc.getXSymmetry().applySymmetryBio(thisBiomolecule,
            applySymmetryToBonds, filter);
        asc.xtalSymmetry = null;
        
      }
      doCheckUnitCell &= iHaveUnitCell && doApplySymmetry;
      if (doCheckUnitCell) {
        ignoreFileSpaceGroupName = true;
        sgName = spaceGroup;
        fractionalizeCoordinates(true);
        asc.setCurrentModelInfo("biosymmetry", null);
        asc.setCurrentModelInfo("biosymmetryCount", null);
        checkNearAtoms = false;
        if (byChain)
          return true;
      }
    }
    if (latticeCells != null && latticeCells[0] != 0)
      addJmolScript("unitcell;axes on;axes unitcell;");
    if (requiresSorting)
      sortAssemblyModels();
    return true;
  }

  ////////////////////////////////////////////////////////////////
  // assembly data
  ////////////////////////////////////////////////////////////////

  @Override
  protected boolean checkSubclassSymmetry() {
    checkNearAtoms = false;
    int modelIndex = asc.iSet;
    asc.setCurrentModelInfo("PDB_CONECT_firstAtom_count_max",
        new int[] { asc.getAtomSetAtomIndex(modelIndex),
            asc.getAtomSetAtomCount(modelIndex), maxSerial });
    return false;
  }

  /**
   * Note that setting bonds from _struct_conn is only done if we have updated
   * CIF files, which include _chem_comp_bond.
   */
  private void setBonds() {
    if (htBondMap == null)
      return;
    BS bsAtoms = asc.bsAtoms;
    if (bsAtoms == null)
      bsAtoms = BSUtil.newBitSet2(0, asc.ac);
    Atom[] atoms = asc.atoms;
    double seqid = -1;
    String comp = null;
    Map<Object, Integer> map = null;
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
      Atom a = atoms[i];
      // 2023.04.28 this should be just sequenceNumber, because we are reading _struct_conn.ptnr1_auth_asym_id
      double pt = a.sequenceNumber;//(a.vib == null ? a.sequenceNumber : a.vib.x);
      if (pt != seqid) {
        seqid = pt;
        if (comp != null)
          processBonds(htBondMap.get(comp), map, false);
        map = new Hashtable<Object, Integer>();
        comp = atoms[i].group3;
        if (!htBondMap.containsKey(comp)) {
          comp = null;
          continue;
        }
      }
      if (comp == null)
        continue;
      map.put(a.atomName, Integer.valueOf(a.index));
    }
    if (comp != null)
      processBonds(htBondMap.get(comp), map, false);
    if (structConnMap != null) {
      map = new Hashtable<Object, Integer>();
      seqid = -1;
      comp = null;
      for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
        Atom a = atoms[i];
        double pt = a.sequenceNumber;//(a.vib == null ? a.sequenceNumber : a.vib.x);
        if (pt != seqid) {
          seqid = pt;
          String ckey = a.chainID + a.group3 + seqid;
          if (structConnList.indexOf(ckey) < 0) {
            comp = null;
            continue;
          }
          comp = ckey;
        }
        if (comp == null)
          continue;
        map.put(comp + a.atomName + a.altLoc, Integer.valueOf(a.index));
      }
      processBonds(structConnMap, map, true);
    }
    appendLoadNote(asc.bondCount + " bonds added");
  }

  private void processBonds(Lst<Object[]> cmap, Map<Object, Integer> map, boolean isStructConn) {
    Integer i1, i2;
    for (int i = 0, n = cmap.size(); i < n; i++) {
      Object[] o = cmap.get(i);
      if ((i1 = map.get(o[0])) == null || (i2 = map.get(o[1])) == null)
        continue;
      if (debugging)
        Logger.debug((isStructConn ? "_struct_conn" : "_comp_bond") + " adding bond " + i1 + " " + i2 + " order=" + o[2]);
      asc.addNewBondWithOrder(i1.intValue(), i2.intValue(), ((Integer) o[2]).intValue());
    }
  }

  final private static byte OPER_ID = 12;
  final private static byte OPER_XYZ = 13;
  
  final protected static String CAT_NCS_CAT = "_struct_ncs_oper.";
  final protected static String CAT_NCS = "_struct_ncs_oper";
  final private static String[] ncsoperFields = {
    "*_matrix[1][1]",
    "*_matrix[1][2]",
    "*_matrix[1][3]",
    "*_vector[1]",
    "*_matrix[2][1]",
    "*_matrix[2][2]",
    "*_matrix[2][3]",
    "*_vector[2]",
    "*_matrix[3][1]",
    "*_matrix[3][2]",
    "*_matrix[3][3]",
    "*_vector[3]", 
    "*_id",
    "*_symmetry_operation" 
  };

  final protected static String CAT_OPER_CAT = "_pdbx_struct_oper_list.";
  final protected static String CAT_OPER = "_pdbx_struct_oper_list";
  final private static String[] operFields = {
    "*_matrix[1][1]",
    "*_matrix[1][2]",
    "*_matrix[1][3]",
    "*_vector[1]",
    "*_matrix[2][1]",
    "*_matrix[2][2]",
    "*_matrix[2][3]",
    "*_vector[2]",
    "*_matrix[3][1]",
    "*_matrix[3][2]",
    "*_matrix[3][3]",
    "*_vector[3]", 
    "*_id",
    "*_symmetry_operation" 
  };

  final private static byte ASSEM_ID = 0;
  final private static byte ASSEM_OPERS = 1;
  final private static byte ASSEM_LIST = 2;
  
  final protected static String CAT_ASSEM_CAT = "_pdbx_struct_assembly_gen.";
  
  final private static String[] assemblyFields = {
    "_pdbx_struct_assembly_gen_assembly_id",
    "_pdbx_struct_assembly_gen_oper_expression",
    "_pdbx_struct_assembly_gen_asym_id_list" 
  };

  /*
  _pdbx_struct_assembly_gen.assembly_id       1 
  _pdbx_struct_assembly_gen.oper_expression   1,2,3,4 
  _pdbx_struct_assembly_gen.asym_id_list      A,B,C 
  # 
  loop_
  _pdbx_struct_oper_list.id 
  _pdbx_struct_oper_list.type 
  _pdbx_struct_oper_list.name 
  _pdbx_struct_oper_list.symmetry_operation 
  _pdbx_struct_oper_list.matrix[1][1] 
  _pdbx_struct_oper_list.matrix[1][2] 
  _pdbx_struct_oper_list.matrix[1][3] 
  _pdbx_struct_oper_list.vector[1] 
  _pdbx_struct_oper_list.matrix[2][1] 
  _pdbx_struct_oper_list.matrix[2][2] 
  _pdbx_struct_oper_list.matrix[2][3] 
  _pdbx_struct_oper_list.vector[2] 
  _pdbx_struct_oper_list.matrix[3][1] 
  _pdbx_struct_oper_list.matrix[3][2] 
  _pdbx_struct_oper_list.matrix[3][3] 
  _pdbx_struct_oper_list.vector[3] 
  1 'identity operation'         1_555  x,y,z          1.0000000000  0.0000000000  0.0000000000 0.0000000000  0.0000000000  
  1.0000000000  0.0000000000 0.0000000000  0.0000000000 0.0000000000 1.0000000000  0.0000000000  
  2 'crystal symmetry operation' 15_556 y,x,-z+1       0.0000000000  1.0000000000  0.0000000000 0.0000000000  1.0000000000  
  0.0000000000  0.0000000000 0.0000000000  0.0000000000 0.0000000000 -1.0000000000 52.5900000000 
  3 'crystal symmetry operation' 10_665 -x+1,-y+1,z    -1.0000000000 0.0000000000  0.0000000000 68.7500000000 0.0000000000  
  -1.0000000000 0.0000000000 68.7500000000 0.0000000000 0.0000000000 1.0000000000  0.0000000000  
  4 'crystal symmetry operation' 8_666  -y+1,-x+1,-z+1 0.0000000000  -1.0000000000 0.0000000000 68.7500000000 -1.0000000000 
  0.0000000000  0.0000000000 68.7500000000 0.0000000000 0.0000000000 -1.0000000000 52.5900000000 
  # 

   */

  final protected static String CAT_SEQUENCEDIF_CAT = "_struct_ref_seq_dif."; 
  final private static byte STRUCT_REF_G3 = 0;
  final private static byte STRUCT_REF_G1 = 1;
  final private static String[] structRefFields = {
    "_struct_ref_seq_dif_mon_id", 
    "_struct_ref_seq_dif_db_mon_id" 
  };

  /**
   * get canonical 1-letter DNA/RNA sequence code from 3-letter code. For example, "2MG" --> "G" 
   * @return true
   * @throws Exception
   */
  protected boolean processSequence() throws Exception {
    parseLoopParameters(structRefFields);
    String g1, g3;
    while (cifParser.getData()) {
      if (isNull(g1 = getFieldString(STRUCT_REF_G1).toLowerCase())
          || g1.length() != 1 || isNull(g3 = getFieldString(STRUCT_REF_G3)))
        continue;
      if (htGroup1 == null)
        asc.setInfo("htGroup1", htGroup1 = new Hashtable<String, String>());
      htGroup1.put(g3, g1);
    }
    return true;
  }

  protected boolean processAssemblyGenBlock() throws Exception {
    parseLoopParameters(assemblyFields);
    while (cifParser.getData()) {
      String[] assem = new String[3];
      int count = 0;
      int p;
      int n = cifParser.getColumnCount();
      for (int i = 0; i < n; ++i) {
        switch (p = fieldProperty(i)) {
        case ASSEM_ID:
        case ASSEM_OPERS:
        case ASSEM_LIST:
          count++;
          assem[p] = (String) field;
          break;
        }
      }
      if (count == 3)
        addAssembly(assem);
    }
    return true;
  }

  @SuppressWarnings("unchecked")
  protected void addAssembly(String[] assem) throws Exception {
    String id = assem[ASSEM_ID];
    String list = assem[ASSEM_LIST];
    String operators = assem[ASSEM_OPERS];
    String name = "biomolecule " + id;
    Logger.info(name + " operators " + operators
        + " ASYM_IDs " + list);
    appendLoadNote("found " + name + ": " + list);
    if (vBiomolecules == null)
      vBiomolecules = new Lst<Map<String, Object>>();
    Map<String, Object> info = null;
    for (int i = vBiomolecules.size(); --i >= 0;)
      if (vBiomolecules.get(i).get("name").equals(name)) {
        info = vBiomolecules.get(i);
        break;
      }
    if (info == null) {
      info = new Hashtable<String, Object>();
      info.put("name", name);
      int iMolecule = parseIntStr(id);
      info.put("molecule",
        iMolecule == Integer.MIN_VALUE ? id : Integer.valueOf(iMolecule));
      info.put("biomts", new Lst<M4d>());
      info.put("chains", new Lst<String>());
      info.put("assemblies", new Lst<String>()); 
      info.put("operators", new Lst<String>());
      vBiomolecules.addLast(info);
    }
    ((Lst<String>) info.get("assemblies")).addLast("$" + list.replace(',', '$'));
    ((Lst<String>) info.get("operators")).addLast(decodeAssemblyOperators(operators));
    checkFilterAssembly(id, info);
  }

  protected void checkFilterAssembly(String id, Map<String, Object> info) {
    if (checkFilterKey("ASSEMBLY " + id + ";") || checkFilterKey("ASSEMBLY=" + id + ";"))
      thisBiomolecule = info;
  }

  private String decodeAssemblyOperators(String ops) {

    //    Identifies the operation of collection of operations 
    //    from category PDBX_STRUCT_OPER_LIST.  
    //
    //    Operation expressions may have the forms:
    //
    //     (1)        the single operation 1
    //     (1,2,5)    the operations 1, 2, 5
    //     (1-4)      the operations 1,2,3 and 4
    //     (1,2)(3,4) the combinations of operations
    //                3 and 4 followed by 1 and 2 (i.e.
    //                the cartesian product of parenthetical
    //                groups applied from right to left)
    int pt = ops.indexOf(")(");
    if (pt >= 0)
      return crossBinary(decodeAssemblyOperators(ops.substring(0, pt + 1)),
          decodeAssemblyOperators(ops.substring(pt + 1)));
    if (ops.startsWith("(")) {
      if (ops.indexOf("-") >= 0)
        ops = BS.unescape(
            "({" + ops.substring(1, ops.length() - 1).replace('-', ':').replace(',', ' ') + "})")
            .toJSON();
      ops = PT.rep(ops, " ", "");
      ops = ops.substring(1, ops.length() - 1);
    }
    return ops;
  }

  private String crossBinary(String ops1, String ops2) {
    SB sb = new SB();
    String[] opsLeft = PT.split(ops1, ",");
    String[] opsRight = PT.split(ops2, ",");
    for (int i = 0; i < opsLeft.length; i++)
      for (int j = 0; j < opsRight.length; j++)
        sb.append(",").append(opsLeft[i]).append("|").append(opsRight[j]);
    return sb.toString().substring(1);
  }

  protected boolean processStructOperListBlock(boolean isNCS) throws Exception {
    parseLoopParametersFor((isNCS ? CAT_NCS : CAT_OPER), isNCS ? ncsoperFields : operFields);
    double[] m = new double[16];
    m[15] = 1;
    while (cifParser.getData()) {
      int count = 0;
      String id = null;
      String xyz = null;
      int n = cifParser.getColumnCount();
      for (int i = 0; i < n; ++i) {
        int p = fieldProperty(i);
        switch (p) {
        case NONE:
          break;
        case OPER_ID:
          id = (String) field;
          break;
        case OPER_XYZ:
          xyz = (String) field;
          break;
        default:
          m[p] = parseDoubleField();
          ++count;
        }
      }
      if (id != null && (count == 12 || xyz != null && symmetry != null)) {
        Logger.info((isNCS ? "noncrystallographic symmetry operator " : "assembly operator ") + id + " " + xyz);
        M4d m4 = new M4d();
        if (count != 12) {
          symmetry.getMatrixFromString(xyz, m, false, 0);
          m[3] *= symmetry.getUnitCellInfoType(SimpleUnitCell.INFO_A) / 12;
          m[7] *= symmetry.getUnitCellInfoType(SimpleUnitCell.INFO_B) / 12;
          m[11] *= symmetry.getUnitCellInfoType(SimpleUnitCell.INFO_C) / 12;
        }
        m4.setA(m);
        addMatrix(id, m4, isNCS);
      }
    }
    return true;
  }

  protected void addMatrix(String id, M4d m4, boolean isNCS) {
    if (isNCS) {
      if (m4.equals(mident))
        return;
      m4.m33 = 0; // flag for normalization
      if (lstNCS == null)
        lstNCS = new Lst<M4d>();
      lstNCS.addLast(m4);
    } else {
      if (htBiomts == null)
        htBiomts = new Hashtable<String, M4d>();
      htBiomts.put(id, m4);
    }
  }

  ////////////////////////////////////////////////////////////////
  // HETATM identity
  ////////////////////////////////////////////////////////////////

  final private static byte CHEM_COMP_ID = 0;
  final private static byte CHEM_COMP_NAME = 1;

  final protected static String CAT_CHEMCOMP_CAT = "_chem_comp.";
  
  final private static String[] chemCompFields = { 
    "_chem_comp_id",
    "_chem_comp_name"
  };

  /**
   * 
   * a general name definition field. Not all hetero
   * 
   * @return true if successful; false to skip
   * 
   * @throws Exception
   */
  protected boolean processChemCompLoopBlock() throws Exception {
    parseLoopParameters(chemCompFields);
    String groupName, hetName;
    while (cifParser.getData())
      if (!isNull(groupName = getFieldString(CHEM_COMP_ID))
          && !isNull(hetName = getFieldString(CHEM_COMP_NAME)))
        addHetero(groupName, hetName, true, true);
    return true;
  }

//  final private static byte NONPOLY_NAME = 0;
//  final private static byte NONPOLY_COMP_ID = 1;
//
//  private static final String CAT_PDBX_NONPOLY_CAT = "_pdbx_entity_nonpoly.";
//  
//  final private static String[] nonpolyFields = {
//      "_pdbx_entity_nonpoly_name",
//      "_pdbx_entity_nonpoly_comp_id", };
//
//
//  /**
//   * 
//   * a HETERO name definition field. Maybe not all hetero? nonpoly?
//   * 
//   * @return true if successful; false to skip
//   * 
//   * @throws Exception
//   */
//  private boolean processNonpolyLoopBlock() throws Exception {
//    parseLoopParameters(nonpolyFields);
//    String groupName, hetName;
//    while (parser.getData()) {
//      if (isNull(groupName = getFieldString(NONPOLY_COMP_ID))
//          || isNull(hetName = getFieldString(NONPOLY_NAME)))
//        return false;
//      addHetero(groupName, hetName, true);
//    }
//    return true;
//  }

  protected void addHetero(String groupName, String hetName, boolean doCheck, boolean addNote) {
    if (doCheck && !vwr.getJBR().isHetero(groupName))
      return;
    if (htHetero == null)
      htHetero = new Hashtable<String, String>();
    if (doCheck && htHetero.containsKey(groupName))
      return;
    htHetero.put(groupName, hetName);
    if (addNote)
      appendLoadNote(groupName + " = " + hetName);
  }

  ////////////////////////////////////////////////////////////////
  // helix and turn structure data
  ////////////////////////////////////////////////////////////////

  final private static byte CONF_TYPE_ID = 0;
  final private static byte BEG_ASYM_ID = 1;
  final private static byte BEG_SEQ_ID = 2;
  final private static byte BEG_INS_CODE = 3;
  final private static byte END_ASYM_ID = 4;
  final private static byte END_SEQ_ID = 5;
  final private static byte END_INS_CODE = 6;
  final private static byte STRUCT_ID = 7;
  final private static byte SERIAL_NO = 8;
  final private static byte HELIX_CLASS = 9;

  final protected static String CAT_STRUCTCONF_CAT = "_struct_conf.";
  
  final protected static String CAT_STRUCTCONF = "_struct_conf";
  final private static String[] structConfFields = {
      "*_conf_type_id", 
      "*_beg_auth_asym_id",
      "*_beg_auth_seq_id", 
      "*_pdbx_beg_pdb_ins_code",
      "*_end_auth_asym_id", 
      "*_end_auth_seq_id",
      "*_pdbx_end_pdb_ins_code", 
      "*_id",
      "*_pdbx_pdb_helix_id", 
      "*_pdbx_pdb_helix_class" };

  /**
   * identifies ranges for HELIX and TURN
   * 
   * @return true if successful; false to skip
   * @throws Exception
   */
  protected boolean processStructConfLoopBlock() throws Exception {
    if (ignoreStructure) {
      skipLoop(false);
      return false;
    }
    parseLoopParametersFor(CAT_STRUCTCONF, structConfFields);
    if (!checkAllFieldsPresent(structConfFields, -1, true)) {
      skipLoop(true);
      return false;
    }
    while (cifParser.getData()) {
      Structure structure = new Structure(-1, STR.HELIX, STR.HELIX, null, null, 0, null);
      
      String type = getFieldString(CONF_TYPE_ID);
      if (type.startsWith("TURN"))
        structure.structureType = structure.substructureType = STR.TURN;
      else if (!type.startsWith("HELX"))
        structure.structureType = structure.substructureType = STR.NONE; 
      else
        structure.substructureType = Structure.getHelixType(parseIntFieldTok(HELIX_CLASS));
      structure.strandID = getFieldString(SERIAL_NO);
      structure.structureID = getFieldString(STRUCT_ID);

      addStructure(structure);
    }
    return true;
  }

  ////////////////////////////////////////////////////////////////
  // sheet structure data
  ////////////////////////////////////////////////////////////////

  private void addStructure(Structure structure) {    
    structure.startChainID = vwr.getChainID(getFieldString(BEG_ASYM_ID), true);
    structure.startSequenceNumber = parseIntFieldTok(BEG_SEQ_ID);
    structure.startInsertionCode = (getFieldString(BEG_INS_CODE)).charAt(0);
    structure.endChainID = vwr.getChainID(getFieldString(END_ASYM_ID), true);
    structure.endSequenceNumber = parseIntFieldTok(END_SEQ_ID);
    structure.endInsertionCode = (getFieldString(END_INS_CODE)).charAt(0);
    asc.addStructure(structure);
  }

  protected int parseIntFieldTok(byte  tok) {
    return parseIntStr(getFieldString(tok));
  }

  final private static byte SHEET_ID = 0;
  final private static byte STRAND_ID = 7;

  final protected static String CAT_SHEET_CAT = "_struct_sheet_range.";
  
  final protected static String CAT_SHEET = "_struct_sheet_range";
  final private static String[] structSheetRangeFields = {
      "*_sheet_id",
      "*_beg_auth_asym_id",
      "*_beg_auth_seq_id",
      "*_pdbx_beg_pdb_ins_code",
      "*_end_auth_asym_id",
      "*_end_auth_seq_id",
      "*_pdbx_end_pdb_ins_code", 
      "*_id"
  };

  /**
   * 
   * identifies sheet ranges
   * 
   * @return true if successful; false to skip
   * 
   * @throws Exception
   */
  protected boolean processStructSheetRangeLoopBlock() throws Exception {
    if (ignoreStructure) {
      skipLoop(false);
      return false;
    }
    parseLoopParametersFor(CAT_SHEET, structSheetRangeFields);
    if (!checkAllFieldsPresent(structSheetRangeFields, -1, true)) {
      skipLoop(true);
      return false;
    }
    while (cifParser.getData())
      addStructure(new Structure(-1, STR.SHEET, STR.SHEET, getFieldString(SHEET_ID), 
          getFieldString(STRAND_ID), 1, null));
    return true;
  }

  final private static byte SITE_ID = 0;
  final private static byte SITE_COMP_ID = 1;
  final private static byte SITE_ASYM_ID = 2;
  final private static byte SITE_SEQ_ID = 3;
  final private static byte SITE_INS_CODE = 4; //???

  final protected static String CAT_STRUCSITE_CAT = "_struct_site_gen.";
  
  final private static String CAT_STRUCSITE = "_struct_site_gen";
  final private static String[] structSiteFields = {
      "*_site_id", 
      "*_auth_comp_id",
      "*_auth_asym_id", 
      "*_auth_seq_id",
      "*_label_alt_id", //should be an insertion code, not an alt ID? 
  };

  //  loop_
  //  _struct_site_gen.id 
  //  _struct_site_gen.site_id 
  //  _struct_site_gen.pdbx_num_res 
  //  _struct_site_gen.label_comp_id 
  //  _struct_site_gen.label_asym_id 
  //  _struct_site_gen.label_seq_id 
  //  _struct_site_gen.auth_comp_id 
  //  _struct_site_gen.auth_asym_id 
  //  _struct_site_gen.auth_seq_id 
  //  _struct_site_gen.label_atom_id 
  //  _struct_site_gen.label_alt_id 
  //  _struct_site_gen.symmetry 
  //  _struct_site_gen.details 
  //  1 CAT 5 GLN A 92  GLN A 92  . . ? ? 
  //  2 CAT 5 GLU A 58  GLU A 58  . . ? ? 
  //  3 CAT 5 HIS A 40  HIS A 40  . . ? ? 
  //  4 CAT 5 TYR A 38  TYR A 38  . . ? ? 
  //  5 CAT 5 PHE A 100 PHE A 100 . . ? ? 
  //  # 

  /**
   * 
   * identifies structure sites
   * 
   * @return true if successful; false to skip
   * 
   * @throws Exception
   */
  protected boolean processStructSiteBlock() throws Exception {
    parseLoopParametersFor(CAT_STRUCSITE, structSiteFields);
    Map<String, Object> htSite = null;
    htSites = new Hashtable<String, Map<String, Object>>();
    String seqNum, resID;
    while (cifParser.getData()) {
      if (isNull(seqNum = getFieldString(SITE_SEQ_ID))
          || isNull(resID = getFieldString(SITE_COMP_ID)))
        continue;
      String siteID = getFieldString(SITE_ID);
      htSite = htSites.get(siteID);
      if (htSite == null) {
        htSite = new Hashtable<String, Object>();
        htSite.put("groups", "");
        htSites.put(siteID, htSite);
      }
      String insCode = getFieldString(SITE_INS_CODE);
      String chainID = getFieldString(SITE_ASYM_ID);
      String group = "[" + resID + "]" + seqNum
          + (isNull(insCode) ? "" : "^" + insCode)
          + (isNull(chainID) ? "" : ":" + chainID);
      String groups = (String) htSite.get("groups");
      groups += (groups.length() == 0 ? "" : ",") + group;
      htSite.put("groups", groups);
    }
    return true;
  }

  private void setBiomolecules() {
    if (assemblyIdAtoms == null && chainAtomCounts == null)
      return;
    BS bsAll = new BS();
    for (int i = vBiomolecules.size(); --i >= 0;) {
      Map<String, Object> biomolecule = vBiomolecules.get(i);
      setBiomolecule(biomolecule, (biomolecule == thisBiomolecule ? bsAll : null));
    }
    if (isBiomolecule && bsAll.cardinality() < asc.ac) {
      if (asc.bsAtoms != null)
        asc.bsAtoms.and(bsAll);
      else if (!isCourseGrained)
        asc.bsAtoms = bsAll;
    }
  }
  
  @SuppressWarnings("unchecked")
  private int setBiomolecule(Map<String, Object> biomolecule, BS bsAll) {
    Lst<String> biomtchains = (Lst<String>) biomolecule.get("chains");
    Lst<M4d> biomts = (Lst<M4d>) biomolecule.get("biomts");
    Lst<String> operators = (Lst<String>) biomolecule.get("operators");
    Lst<String> assemblies = (Lst<String>) biomolecule.get("assemblies");
    P3d sum = new P3d();
    int count = 0;
    BS bsAtoms = new BS();
    int nAtomsTotal = 0;
    boolean isBioCourse = (isBiomolecule && isCourseGrained);
    for (int i = operators.size(); --i >= 0;) {
      String[] ops = PT.split(operators.get(i), ",");
      String[] ids = PT.split(assemblies.get(i), "$");
      String chainlist = "";
      int nAtoms = 0;
      for (int j = 1; j < ids.length; j++) {
        String id = ids[j];
        chainlist += ":" + id + ";";
        if (assemblyIdAtoms != null) {
          biomolecule.put("asemblyIdAtoms", assemblyIdAtoms);
          BS bs = assemblyIdAtoms.get(id);
          if (bs != null) {
            bsAtoms.or(bs);
            if (bsAll != null)
              bsAll.or(bs);
            nAtoms += bs.cardinality();
          }
        } else if (isBioCourse) {
          P3d asum = chainAtomMap.get(id);
          if (asum != null) {
            if (bySymop) {
              sum.add(asum);
              count += chainAtomCounts.get(id)[0];
            } else {
              createParticle(id);
              nAtoms++;
            }
          }
        }
      }
      if (!isBiomolecule)
        continue;
      for (int j = 0; j < ops.length; j++) {
        M4d m = getOpMatrix(ops[j]);
        if (m == null)
          return 0;
        if (m.equals(mident)) {
          biomts.add(0, mident);
          biomtchains.add(0, chainlist);
        } else {
          biomts.addLast(m);
          biomtchains.addLast(chainlist);
        }
      }
      if (bySymop && bsAll != null) {
        nAtoms = 1;
        Atom a1 = new Atom();
        a1.setT(sum);
        a1.scale(1d / count);
        a1.radius = 16;
        asc.addAtom(a1);
      }
      nAtoms *= ops.length;
      nAtomsTotal += nAtoms;
    }
    biomolecule.put("atomCount", Integer.valueOf(nAtomsTotal));
    return nAtomsTotal;

  }

  private void createParticle(String id) {
    P3d asum = chainAtomMap.get(id);
    int c = chainAtomCounts.get(id)[0];
    Atom a = new Atom();
    a.setT(asum);
    a.scale(1d / c);
    a.elementSymbol = "Pt";
    setChainID(a, id); 
    a.radius = 16;
    asc.addAtom(a);
  }

  private M4d getOpMatrix(String ops) {
    if (htBiomts == null)
      return M4d.newM4(null);
    int pt = ops.indexOf("|");
    if (pt >= 0) {
      M4d m = M4d.newM4(htBiomts.get(ops.substring(0, pt)));
      m.mul(htBiomts.get(ops.substring(pt + 1)));
      return m;
    }
    return htBiomts.get(ops);
  }

  ////////////////////////////////////////////////////////////////
  // bond data
  ////////////////////////////////////////////////////////////////

  final private static byte STRUCT_CONN_ASYM1 = 0;
  final private static byte STRUCT_CONN_SEQ1  = 1;
  final private static byte STRUCT_CONN_COMP1 = 2;
  final private static byte STRUCT_CONN_ATOM1 = 3;
  final private static byte STRUCT_CONN_ALT1  = 4;
  final private static byte STRUCT_CONN_SYMM1 = 5;
  final private static byte STRUCT_CONN_ASYM2 = 6;
  final private static byte STRUCT_CONN_SEQ2  = 7;
  final private static byte STRUCT_CONN_COMP2 = 8;
  final private static byte STRUCT_CONN_ATOM2 = 9;
  final private static byte STRUCT_CONN_ALT2  = 10;
  final private static byte STRUCT_CONN_SYMM2 = 11;
  final private static byte STRUCT_CONN_TYPE  = 12;
  final private static byte STRUCT_CONN_ORDER = 13;

  
  final protected static String CAT_STRUCTCONN_CAT = "_struct_conn.";
  
  final protected static String CAT_STRUCTCONN = "_struct_conn";
  final private static String[] structConnFields = {
    "*_ptnr1_auth_asym_id",
    "*_ptnr1_auth_seq_id",
    "*_ptnr1_auth_comp_id",
    "*_ptnr1_label_atom_id",  
    "*_pdbx_ptnr1_label_alt_id",
    "*_ptnr1_symmetry",
    "*_ptnr2_auth_asym_id",
    "*_ptnr2_auth_seq_id", // an integer
    "*_ptnr2_auth_comp_id",
    "*_ptnr2_label_atom_id",  
    "*_pdbx_ptnr2_label_alt_id",
    "*_ptnr2_symmetry",
    "*_conn_type_id",
    "*_pdbx_value_order"
  };
  
//Allowed Value   Details
//covale  covalent bond
//covale_base   covalent modification of a nucleotide base
//covale_phosphate  covalent modification of a nucleotide phosphate
//covale_sugar  covalent modification of a nucleotide sugar
//disulf  disulfide bridge
//metalc  metal coordination
//
//// not used:
//hydrog  hydrogen bond
//mismat  mismatched base pairs
//modres  covalent residue modification
//saltbr  ionic interaction

  private Lst<Object[]> structConnMap;
  private String structConnList = "";
  private boolean doSetBonds;

  protected boolean processStructConnLoopBlock() throws Exception {
    parseLoopParametersFor(CAT_STRUCTCONN, structConnFields);
    while (cifParser.getData()) {
      String sym1 = getFieldString(STRUCT_CONN_SYMM1);
      String sym2 = getFieldString(STRUCT_CONN_SYMM2);
      if (!sym1.equals(sym2) || !isNull(sym1) && !sym1.equals("1_555"))
        continue;
      String type = getFieldString(STRUCT_CONN_TYPE);
      if (!type.startsWith("covale") && !type.equals("disulf")
          && !type.equals("metalc"))
        continue;
      if (htBondMap == null)
        htBondMap = new Hashtable<String, Lst<Object[]>>();
      String key1 = "" + vwr.getChainID(getFieldString(STRUCT_CONN_ASYM1), true) 
          + getFieldString(STRUCT_CONN_COMP1)
          + parseIntFieldTok(STRUCT_CONN_SEQ1)
          + getFieldString(STRUCT_CONN_ATOM1) 
          + getFieldString(STRUCT_CONN_ALT1);
      String key2 = "" + vwr.getChainID(getFieldString(STRUCT_CONN_ASYM2), true) 
          + getFieldString(STRUCT_CONN_COMP2)
          + parseIntFieldTok(STRUCT_CONN_SEQ2)
          + getFieldString(STRUCT_CONN_ATOM2) 
          + getFieldString(STRUCT_CONN_ALT2);
      int order = getBondOrder(getFieldString(STRUCT_CONN_ORDER));
      if (structConnMap == null)
        structConnMap = new Lst<Object[]>();
      structConnMap
          .addLast(new Object[] { key1, key2, Integer.valueOf(order) });
      if (structConnList.indexOf(key1) < 0)
        structConnList += key1;
      if (structConnList.indexOf(key2) < 0)
        structConnList += key2;
    }
    return true;
  }

  final private static byte CHEM_COMP_BOND_ID = 0;
  final private static byte CHEM_COMP_BOND_ATOM_ID_1 = 1;
  final private static byte CHEM_COMP_BOND_ATOM_ID_2 = 2;
  final private static byte CHEM_COMP_BOND_VALUE_ORDER = 3;
  final private static byte CHEM_COMP_BOND_AROMATIC_FLAG = 4;
  
  final protected static String CAT_COMPBOND_CAT = "_chem_comp_bond.";
  
  final protected static String CAT_COMPBOND = "_chem_comp_bond";
  final private static String[] chemCompBondFields = {
    "*_comp_id",
    "*_atom_id_1", 
    "*_atom_id_2",
    "*_value_order", 
    "*_pdbx_aromatic_flag"
  };

  protected boolean processCompBondLoopBlock() throws Exception {
    doSetBonds = true;
    parseLoopParametersFor(CAT_COMPBOND, chemCompBondFields);
    while (cifParser.getData()) {
      String comp = getFieldString(CHEM_COMP_BOND_ID);
      String atom1 = getFieldString(CHEM_COMP_BOND_ATOM_ID_1);
      String atom2 = getFieldString(CHEM_COMP_BOND_ATOM_ID_2);
      int order = getBondOrder(getFieldString(CHEM_COMP_BOND_VALUE_ORDER));
      if (getFieldString(CHEM_COMP_BOND_AROMATIC_FLAG).charAt(0) == 'Y')
        switch (order) {
        case JmolAdapter.ORDER_COVALENT_SINGLE:
          order = JmolAdapter.ORDER_AROMATIC_SINGLE;
          break;
        case JmolAdapter.ORDER_COVALENT_DOUBLE:
          order = JmolAdapter.ORDER_AROMATIC_DOUBLE;
          break;
        }
      if (isLigand) {
        asc.addNewBondWithOrderA(asc.getAtomFromName(atom1),
            asc.getAtomFromName(atom2), order);
      } else if (haveHAtoms || htHetero != null && htHetero.containsKey(comp)) {
        if (htBondMap == null)
          htBondMap = new Hashtable<String, Lst<Object[]>>();
        Lst<Object[]> cmap = htBondMap.get(comp);
        if (cmap == null)
          htBondMap.put(comp, cmap = new Lst<Object[]>());
        cmap.addLast(new Object[] { atom1, atom2,
            Integer.valueOf(haveHAtoms ? order : 1) });
      }
    }
    return true;
  }
  
  @Override
  public boolean processSubclassAtom(Atom atom, String assemblyId, String strChain) {
    if (isBiomolecule) {
      if (isCourseGrained) {
        P3d sum = chainAtomMap.get(assemblyId);
        if (sum == null) {
          chainAtomMap.put(assemblyId, sum = new P3d());
          chainAtomCounts.put(assemblyId, new int[1]);
        }
        chainAtomCounts.get(assemblyId)[0]++;
        sum.add(atom);
        return false;
      }
    } else if (byChain) {
      if (thisChain != atom.chainID) {
        thisChain = atom.chainID;
        chainSum = chainAtomMap.get(strChain);
        if (chainSum == null) {
          chainAtomMap.put(strChain, chainSum = new P3d());
          chainAtomCounts.put(strChain, chainAtomCount = new int[1]);
        }
      }
      chainSum.add(atom);
      chainAtomCount[0]++;
      return false;
    }
    if (assemblyId != null) {
      if (assemblyIdAtoms == null)
        assemblyIdAtoms = new Hashtable<String, BS>();
      BS bs = assemblyIdAtoms.get(assemblyId);
      if (bs == null)
        assemblyIdAtoms.put(assemblyId, bs = new BS());
      bs.set(ac);
    }    
    return true;
  }

  private String modelStrings = "";
  
  protected boolean done;
  
  @Override
  protected int checkPDBModelField(int modelField, int currentModelNo) throws Exception {
    // the model field value is only used if 
    // it is indicated AFTER the file name in the load command, 
    // not if we have a MODEL keyword before the file name.
    
    fieldProperty(modelField);
    int modelNo = parseIntField();
    return (modelNo == currentModelNo ? modelNo : incrementModel(modelNo));
  }

  protected int incrementModel(int modelNo) throws Exception {
    boolean isAssembly = (thisDataSetName != null && thisDataSetName.indexOf("-assembly-") >= 0);
    if (isAssembly) {
      // Files such as http://www.ebi.ac.uk/pdbe/static/entry/download/2lev-assembly-1.cif.gz
      // may require sorting if there are multiple models, since the models are by chain, not by model.

      useFileModelNumbers = true;
      String key = "," + modelNo + ",";
      if (modelStrings.indexOf(key) >= 0) {
        requiresSorting = true;
      } else {
        modelStrings += key;
      }
    }      
    if (iHaveDesiredModel && asc.atomSetCount > 0 && !isAssembly) {
      done = true;
      if (cifParser != null) {
        skipLoop(false);
        // but only this atom loop
        skipping = false;
      }
      continuing = true;
      return Integer.MIN_VALUE;
    }
    int modelNumberToUse = (useFileModelNumbers ? modelNo : ++modelIndex);
    setHetero();
    newModel(modelNumberToUse);
    if (!skipping) {
      nextAtomSet();
      if (modelMap == null || asc.ac == 0)
        modelMap = new Hashtable<String, Integer>();
      modelMap.put("" + modelNo, Integer.valueOf(Math.max(0, asc.iSet)));
      modelMap
          .put("_" + Math.max(0, asc.iSet), Integer.valueOf(modelNo));
    }
  return modelNo;
  }

  private void setHetero() {
    if (htHetero != null) {
      asc.setCurrentModelInfo("hetNames", htHetero);
      asc.setInfo("hetNames", htHetero);
    }    
  }

}
