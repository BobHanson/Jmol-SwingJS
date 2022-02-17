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

package org.jmol.adapter.readers.cif;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.Lst;
import javajs.util.M4;
import javajs.util.MessagePackReader;
import javajs.util.PT;
import javajs.util.SB;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.Bond;
import org.jmol.adapter.smarter.Structure;
import javajs.util.BS;
import org.jmol.script.SV;
import org.jmol.util.Logger;

/**
 * JmolData RCSB MMTF (macromolecular transmission format) file reader
 * 
 * see https://github.com/rcsb/mmtf/blob/master/spec.md
 * 
 * full specification Version: v0.2+dev (as of 2016.08.08) is
 * implemented,including:
 * 
 * reading atoms, bonds, and DSSP 1.0 secondary structure
 * 
 * load =1f88.mmtf filter "DSSP1"
 * 
 * [Note that the filter "DSSP1" is required, since mmtf included DSSP 1.0
 * calculations, while the standard for Jmol itself is DSSP 2.0. These two
 * calculations differ in their treating of helix kinks as one (1.0) or two
 * (2.0) helices.]
 * 
 * 
 * reading space groups and unit cells, and using those as per other readers
 * 
 * load =1crn.mmtf {1 1 1}
 * 
 * reading bioassemblies (biomolecules) and applying all symmetry
 * transformations
 * 
 * load =1auy.mmtf FILTER "biomolecule 1;*.CA,*.P"
 * 
 * reading both biomolecules and lattices, and loading course-grained using the
 * filter "BYCHAIN" or "BYSYMOP"
 * 
 * load =1auy.mmtf {2 2 1} filter "biomolecule 1;bychain";spacefill 30.0; color
 * property symop
 * 
 * Many thanks to the MMTF team at RCSB for assistance in this implementation.
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */

public class MMTFReader extends MMCifReader {

  private boolean haveStructure;
  private String pdbID;


  @Override
  protected void addHeader() {
    // no header for this type
  }

  /**
   * standard set up
   * 
   * @param fullPath
   * @param htParams
   * @param reader
   */
  @Override
  protected void setup(String fullPath, Map<String, Object> htParams,
                       Object reader) {
    isBinary = true;
    isMMCIF = true;
    iHaveFractionalCoordinates = false;
    setupASCR(fullPath, htParams, reader);
  }

  @Override
  protected void processBinaryDocument() throws Exception {

    // load xxx.mmtf filter "..." options 

    // NODOUBLE -- standard PDB-like structures
    // DSSP2 -- use Jmol's built-in DSSP 2.0, not file's DSSP 1.0 

    boolean doDoubleBonds = (!isCourseGrained && !checkFilterKey("NODOUBLE"));
    isDSSP1 = !checkFilterKey("DSSP2");
    boolean mmtfImplementsDSSP2 = false; // so far!
    applySymmetryToBonds = true;
    map = (new MessagePackReader(binaryDoc, true)).readMap();
    entities = (Object[]) map.get("entityList");
    if (Logger.debugging) {
      for (String s : map.keySet())
        Logger.info(s);
    }
    asc.setInfo("noAutoBond", Boolean.TRUE);
    Logger.info("MMTF version " + map.get("mmtfVersion"));
    Logger.info("MMTF Producer " + map.get("mmtfProducer"));
    String title = (String) map.get("title");
    if (title != null)
      appendLoadNote(title);
    pdbID = (String) map.get("structureId");
    if (pdbID == null)
      pdbID = (String) map.get("pdbId");
    fileAtomCount = ((Integer) map.get("numAtoms")).intValue();
    int nBonds = ((Integer) map.get("numBonds")).intValue();

    groupCount = ((Integer) map.get("numGroups")).intValue();
    groupModels = new int[groupCount]; // group model (file-based)
    groupDSSP = new int[groupCount];   // group structure type (Jmol-based)
    groupMap = new int[groupCount];    // file->jmol group index map
    
    int modelCount = ((Integer) map.get("numModels")).intValue();
    appendLoadNote("id=" + pdbID + " numAtoms=" + fileAtomCount + " numBonds="
        + nBonds + " numGroups=" + groupCount + " numModels=" + modelCount);
    getMMTFAtoms(doDoubleBonds);
    if (!isCourseGrained) {
      int[] bo = (int[]) decode("bondOrderList");
      int[] bi = (int[]) decode("bondAtomList");
      addMMTFBonds(bo, bi, 0, doDoubleBonds, true);
      if (isDSSP1 || mmtfImplementsDSSP2)
        getStructure();
    }
    setMMTFSymmetry();
    getMMTFBioAssembly();
    setModelPDB(true);
    if (Logger.debuggingHigh)
      Logger.info(SV.getVariable(map).asString());
  }

  @Override
  public void applySymmetryAndSetTrajectory() throws Exception {
    ac0 = ac;
    super.applySymmetryAndSetTrajectory();
    addStructureSymmetry();
  }
  
  //////////////////////////////// MMTF-Specific /////////////////////////  

  private Map<String, Object> map; // input JSON-like map from MessagePack binary file  
  private int fileAtomCount;
  private int opCount = 0;
  private int[] groupModels, groupMap, groupDSSP, atomGroup;
  private String[] labelAsymList; // created in getAtoms; used in getBioAssembly
  private Atom[] atomMap; // necessary because some atoms may be deleted. 
  private Object[] entities;
  private int groupCount;
  private int ac0;
  private BS[] bsStructures;


  // TODO  - also consider mapping group indices

  /**
   * set up all atoms, including bonding, within a group
   * 
   * @param doMulti
   *        true to add double bonds
   * 
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  private void getMMTFAtoms(boolean doMulti) throws Exception {

    // chains
    int[] chainsPerModel = (int[]) map.get("chainsPerModel");
    int[] groupsPerChain = (int[]) map.get("groupsPerChain"); // note that this is label_asym, not auth_asym
    labelAsymList = (String[]) decode("chainIdList"); // label_asym
    String[] authAsymList = (String[]) decode("chainNameList"); // Auth_asym

    // groups
    int[] groupTypeList = (int[]) decode("groupTypeList");
    int[] groupIdList = (int[]) decode("groupIdList");
    Object[] groupList = (Object[]) map.get("groupList");
    char[] insCodes = (char[]) decode("insCodeList");
    int[] atomId =    (int[]) decode("atomIdList");
    boolean haveSerial = (atomId != null);
    char[] altloc = (char[]) decode("altLocList"); // rldecode32
    float[] occ = (float[]) decode("occupancyList");
    float[] x = (float[]) decode("xCoordList");//getFloatsSplit("xCoord", 1000f);
    float[] y = (float[]) decode("yCoordList");
    float[] z = (float[]) decode("zCoordList");
    float[] bf = (float[]) decode("bFactorList");
    String[] nameList = (useAuthorChainID ? authAsymList : labelAsymList);
    int iModel = -1;
    int iChain = 0;
    int nChain = 0;
    int iGroup = 0;
    int nGroup = 0;
    int chainpt = 0;
    int seqNo = 0;
    int iatom = 0;
    String chainID = "";
    String authAsym = "", labelAsym = "";
    char insCode = '\0';
    atomMap = new Atom[fileAtomCount];
    atomGroup = new int[fileAtomCount];
    for (int j = 0, thisGroup = -1; j < groupCount; j++) {
      if (++iGroup >= nGroup) {
        chainID = nameList[chainpt];
        authAsym = authAsymList[chainpt];
        labelAsym = labelAsymList[chainpt];
        nGroup = groupsPerChain[chainpt++];
        iGroup = 0;
        if (++iChain >= nChain) {
          groupModels[j] = ++iModel;
          nChain = chainsPerModel[iModel];
          iChain = 0;
          setModelPDB(true);
          incrementModel(iModel + 1);
          asc.setCurrentModelInfo("pdbID", pdbID);
          nAtoms0 = asc.ac;
          if (done)
            return;
        }
      }
      Map<String, Object> g = (Map<String, Object>) groupList[groupTypeList[j]];
      String[] atomNameList = (String[]) g.get("atomNameList");
      int len = atomNameList.length;
      if (skipping) {
        iatom += len;
        continue;
      }          
      int a0 = iatom;
      if (insCodes != null)
        insCode = insCodes[j];
      seqNo = groupIdList[j];
      String group3 = (String) g.get("groupName");
      boolean isHetero = vwr.getJBR().isHetero(group3);
      if (isHetero) {
        // looking for CHEM_COMP_NAME here... "IRON/SULFUR CLUSTER" for SF4 in 1blu
        String hetName = "" + g.get("chemCompType");
        if (htHetero == null || !htHetero.containsKey(group3)) {
          
          // this is not ideal -- see 1a37.mmtf, where PSE is LSQRQRST(PSE)TPNVHM
          // (actually that should be phosphoserine, SPE...)
          // It is still listed as NON-POLYMER even though it is just a modified AA.
          
          if (entities != null && hetName.equals("NON-POLYMER"))
            out: for (int i = entities.length; --i >= 0;) {
              Map<String, Object> entity = (Map<String, Object>) entities[i];
              int[] chainList = (int[]) entity.get("chainIndexList");
              for (int k = chainList.length; --k >= 0;)
                if (chainList[k] == iChain) {
                  hetName = "a component of the entity \"" + entity.get("description") + "\"";
                  break out;
                }
            }
          addHetero(group3, hetName, false, true);
        }
      }
      String[] elementList = (String[]) g.get("elementList");
      boolean haveAtom = false;
      for (int ia = 0, pt = 0; ia < len; ia++, iatom++) {
        Atom a = new Atom();
        a.isHetero = isHetero;
        if (insCode != 0)
          a.insertionCode = insCode;
        setAtomCoordXYZ(a, x[iatom], y[iatom], z[iatom]);
        a.elementSymbol = elementList[pt];
        a.atomName = atomNameList[pt++];
        if (seqNo >= 0)
          maxSerial = Math.max(maxSerial, a.sequenceNumber = seqNo);
        a.group3 = group3;
        setChainID(a, chainID);
        if (bf != null)
          a.bfactor = bf[iatom];
        if (altloc != null)
          a.altLoc = altloc[iatom];
        if (occ != null)
          a.foccupancy = occ[iatom];
        if (haveSerial)
          a.atomSerial = atomId[iatom];
        if (!filterAtom(a, -1) || !processSubclassAtom(a, labelAsym, authAsym))
          continue;
        
        if (!haveAtom) {
          thisGroup++;
          haveAtom = true;
        }
        if (haveSerial) {
          asc.addAtomWithMappedSerialNumber(a);
        } else {
          asc.addAtom(a);
        }
        atomMap[iatom] = a;
        atomGroup[ac] = j;
        groupMap[j] = thisGroup;
        ac++;
      }
      if (!isCourseGrained) {
        int[] bo = (int[]) g.get("bondOrderList");
        int[] bi = (int[]) g.get("bondAtomList");
        addMMTFBonds(bo, bi, a0, doMulti, false);
      }
    }
    asc.setCurrentModelInfo("pdbID", pdbID);
  }

  private void addMMTFBonds(int[] bo, int[] bi, int a0, boolean doMulti, boolean isInter) {
    if (bi == null)
      return;
    doMulti &= (bo != null);
    for (int bj = 0, pt = 0, nj = bi.length / 2; bj < nj; bj++) {
      Atom a1 = atomMap[bi[pt++] + a0];
      Atom a2 = atomMap[bi[pt++] + a0];
      if (a1 != null && a2 != null) {
        Bond bond = new Bond(a1.index, a2.index, doMulti ? bo[bj] : 1);
        asc.addBond(bond);
        if (Logger.debugging && isInter) {
          Logger.info("inter-group (" + (a1.atomSetIndex + 1) + "." + a1.index + "/" + (a2.atomSetIndex + 1) + "." + a2.index + ") bond " + a1.group3 + a1.sequenceNumber + "." + a1.atomName
              + " - " + a2.group3 + a2.sequenceNumber + "." + a2.atomName + " "
              + bond.order);
        }
      }
    }
  }

  private void setMMTFSymmetry() {
    setSpaceGroupName((String) map.get("spaceGroup"));
    float[] o = (float[]) map.get("unitCell");
    if (o != null)
      for (int i = 0; i < 6; i++)
        setUnitCellItem(i, o[i]);
  }

  @SuppressWarnings("unchecked")
  private void getMMTFBioAssembly() {
    Object[] o = (Object[]) map.get("bioAssemblyList");
    if (o == null)
      return;
    if (vBiomolecules == null)
      vBiomolecules = new Lst<Map<String, Object>>();

    for (int i = o.length; --i >= 0;) {
      Map<String, Object> info = new Hashtable<String, Object>();
      vBiomolecules.addLast(info);
      int iMolecule = i + 1;
      checkFilterAssembly("" + iMolecule, info);
      info.put("name", "biomolecule " + iMolecule);
      info.put("molecule", Integer.valueOf(iMolecule));
      Lst<String> assemb = new Lst<String>();
      Lst<String> ops = new Lst<String>();
      info.put("biomts", new Lst<M4>());
      info.put("chains", new Lst<String>());
      info.put("assemblies", assemb);
      info.put("operators", ops);
      // need to add NCS here.
      Map<String, Object> m = (Map<String, Object>) o[i];
      Object[] tlist = (Object[]) m.get("transformList");
      SB chlist = new SB();

      for (int j = 0, n = tlist.length; j < n; j++) {

        Map<String, Object> t = (Map<String, Object>) tlist[j];

        // for every transformation...

        chlist.setLength(0);

        // for compatibility with the mmCIF reader, we create
        // string lists of the chains in the form $A$B$C...

        int[] chainList = (int[]) t.get("chainIndexList");
        for (int k = 0, kn = chainList.length; k < kn; k++)
          chlist.append("$").append(labelAsymList[chainList[k]]);
        assemb.addLast(chlist.append("$").toString());

        // now save the 4x4 matrix transform for this operation

        String id = "" + (++opCount);
        addMatrix(id, M4.newA16((float[]) t.get("matrix")), false);
        ops.addLast(id);
      }
    }
  }

  //  Code  Name
  //  0*   pi helix
  //  1   bend   (ignored)
  //  2*   alpha helix
  //  3*   extended (sheet)
  //  4*   3-10 helix
  //  5   bridge (ignored)
  //  6*   turn 
  //  7   coil (ignored)
  //  -1  undefined

  // 1F88: "secStructList": [7713371173311776617777666177444172222222222222222222222222222222166771222222222222222222262222222222617662222222222222222222222222222222222177111777722222222222222222221222261173333666633337717776666222222222226622222222222226611771777
  // DSSP (Jmol):            ...EE....EE....TT.....TTT...GGG..HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH.TT...HHHHHHHHHHHHHHHHHHHTHHHHHHHHHHT..TTHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH..........HHHHHHHHHHHHHHHHHHH.HHHHT...EEEETTTTEEEE......TTTTHHHHHHHHHHHTTHHHHHHHHHHHHHTT........

  /**
   * Get and translate the DSSP string from digit format
   * 
   *        input data
   */
  private void getStructure() {
    int[] a = (int[]) decode("secStructList");
    if (Logger.debugging)
      Logger.info(PT.toJSON("secStructList", a));
    bsStructures = new BS[] { new BS(), null, new BS(), new BS(),
        new BS(), null, new BS() };
    int lastGroup = -1;
    for (int j = 0; j < a.length; j++) {
      int type = a[j];
      switch (type) {
      case 0: // PI
      case 2: // alpha
      case 3: // sheet
      case 4: // 3-10
      case 6: // turn
        int igroup = groupMap[j];
        bsStructures[type].set(igroup);
        groupDSSP[igroup] = type + 1;
        lastGroup = j;
      }
    }

    int n = (isDSSP1 ? asc.iSet : groupModels[lastGroup]);
    if (lastGroup >= 0) {
      // a single structure takes care of everything
      haveStructure = true;
      asc.addStructure(new Structure(n, null, null, null, 0, 0, bsStructures));
    }
  }

  /**
   * We must add groups to the proper bsStructure element
   *
   */
  public void addStructureSymmetry() {
    if (asc.ac == 0 || !haveStructure || thisBiomolecule == null || ac0 == asc.ac)
      return;
    Atom[] atoms = asc.atoms;
    BS bsAtoms = asc.bsAtoms;

    // must point to groups here.
    
    
    int ptGroup = -1;
    int mygroup = -1;
    for (int i = bsStructures.length; --i >= 0;)
      if (bsStructures[i] != null)
        bsStructures[i].clearAll();
    for (int i = ac0, n = asc.ac; i < n; i++) {
      if (bsAtoms == null || bsAtoms.get(i)) {
        Atom a = atoms[i];
        int igroup = atomGroup[a.atomSite];
        if (igroup != mygroup) {
          mygroup = igroup;
          ptGroup++;
          int dssp = groupDSSP[igroup];
          if (dssp > 0) {
            bsStructures[dssp - 1].set(ptGroup);
          }
        }
      }
    }
  }


  private Object decode(String key) {
      return MessagePackReader.decode((byte[]) map.get(key));
  }
}
