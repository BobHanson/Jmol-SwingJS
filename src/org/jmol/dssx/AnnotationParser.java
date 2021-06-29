/* $RCSfile$
 * $Author: nicove $
 * $Date: 2007-03-25 06:44:28 -0500 (Sun, 25 Mar 2007) $
 * $Revision: 7224 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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
package org.jmol.dssx;

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.SB;

import org.jmol.api.JmolAnnotationParser;
import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.Group;
import org.jmol.modelset.ModelSet;
import org.jmol.modelsetbio.BioResolver;
import org.jmol.script.SV;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

/**
 * 
 * A parser for output from 3DNA web service.
 * 
 * load =1d66/dssr
 * 
 * also other annotations now,
 * 
 * load *1cbs/dom
 * 
 * calls EBI for the mmCIF file and also retrieves the domains mapping JSON
 * report.
 * 
 * 
 * load *1cbs/val
 * 
 * calls EBI for the mmCIF file and also retrieves the validation outliers JSON
 * report.
 * 
 * Bob Hanson July 2014
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */
public class AnnotationParser implements JmolAnnotationParser {

  public AnnotationParser() {
    // for reflection
  }

  ///////////////////// EBI annotations ////////////////

  //format developed at EBI July 31, 2014

  // domains:
  //  http://wwwdev.ebi.ac.uk/pdbe/api/mappings/sequence_domains/1cbs?metadata=true&pretty=true
  //  {
  //    "1cbs": {
  //        "Pfam": {
  //            "PF00061": {
  //                "identifier": "Lipocalin / cytosolic fatty-acid binding protein family",
  //                "description": "Lipocalin / cytosolic fatty-acid binding protein family",
  //                "mappings": [
  //                    {
  //                        "start": {
  //                            "author_residue_number": 4,
  //                            "author_insertion_code": "",
  //                            "residue_number": 4
  //                        },
  //                        "entity_id": 1,
  //                        "end": {
  //                            "author_residue_number": 137,
  //                            "author_insertion_code": "",
  //                            "residue_number": 137
  //                        },
  //                        "chain_id": "A",
  //                        "struct_asym_id": "A"
  //                    }
  //                ]
  //            }
  //        },
  //        "InterPro": {
  //            "IPR000566": {
  //                "identifier": "Lipocalin/cytosolic fatty-acid binding domain",
  //                "name": "Lipocalin/cytosolic fatty-acid binding domain",
  //                "mappings": [
  //                    {
  //                        "entity_id": 1,
  //                        "end": {
  //                            "author_residue_number": 137,
  //                            "author_insertion_code": "",
  //                            "residue_number": 137
  //                        },
  //                        "start": {
  //                            "author_residue_number": 4,
  //                            "author_insertion_code": "",
  //                            "residue_number": 4
  //                        },
  //                        "chain_id": "A",
  //                        "struct_asym_id": "A"
  //                    }
  //                ]
  //            },
  // ...

  // validation:
  //  {
  //    "1cbs": {
  //        "bond_angles": {},
  //        "rsrz": {},
  //        "symm_clashes": {},
  //        "rama": {},
  //        "clashes": {
  //            "outliers": [
  //                {
  //                    "units": [
  //                        "|1|A|LEU|100|CD2|||",
  //                        "|1|A|LYS|82|HG2|||"
  //                    ],
  //                    "value": 0.58999999999999997
  //                },
  //     ...
  //            ]
  //        },
  //        "bond_lengths": {},
  //        "RNA_pucker": {},
  //        "planes": {},
  //        "RNA_suite": {},
  //        "sidechains": {
  //            "outliers": [
  //                {
  //                    "units": [
  //                        "|1|A|ASN|14||||"
  //                    ]
  //                },  
  //...

  /**
   * Construct a nice listing for this annotation, including validation
   * 
   * @param a
   * @param match
   * @param dotPath
   * @param sb
   * @param pre
   * @param showDetail
   * @param isMappingOnly
   * @param type
   */
  private void getAnnotationKVPairs(SV a, String match, String dotPath, SB sb,
                                    String pre, boolean showDetail,
                                    boolean isMappingOnly, int type) {
    Map<String, SV> map = a.getMap();
    if (map == null || map.isEmpty())
      return;
    if (map.containsKey("_map"))
      map = map.get("_map").getMap();
    //    map = map.values().iterator().next().getMap();
    String detailKey = getDataKey(type);
    if (showDetail && map.containsKey(detailKey)) {
      if (match == null || dotPath.indexOf(match) >= 0)
        sb.append(map.get(detailKey).asString()).append("\n");
      return;
    }
    for (Entry<String, SV> e : map.entrySet()) {
      String key = e.getKey();
      if (key.equals(detailKey))
        continue;
      if (key.equals("metadata"))
        sb.append("\n");
      SV val = e.getValue();
      if (val.tok == T.hash) {
        if (type == T.validation && !showDetail) {
          sb.append(key).append("\n");
        } else {
          getAnnotationKVPairs(val, match, (dotPath.length() == 0 ? ""
              : dotPath + ".") + key, sb, (pre.length() == 0 ? "" : pre + "\t")
              + key, showDetail, isMappingOnly, type);
        }
      } else {
        String s = val.asString();
        if (match == null || s.indexOf(match) >= 0 || pre.indexOf(match) >= 0
            || key.indexOf(match) >= 0 || dotPath.indexOf(match) >= 0) {
          if (showDetail && isMappingOnly)
            continue;
          if (pre.length() > 0)
            sb.append(pre).append("\t");
          sb.append(key).append("=");
          sb.append(s).append("\n");
        }
      }
    }
  }

  private String getDataKey(int type) {
    switch (type) {
    case T.domains:
      return "mappings";
    case T.validation:
      return "outliers";
    }
    return null;
  }

  /// RNA3D -- 
  // { "hairpinLoops":
  //     { id:"....", units:"...."},
  //     ...
  // },
  // { "internalLoops":
  //     { id:"....", units:"...."},
  //     ...
  // },
  // { "junctions":
  //     { id:"....", units:"...."},
  //     ...
  // }

  @Override
  public String catalogStructureUnits(Viewer viewer, SV map0,
                                      int[] modelAtomIndices,
                                      Map<String, int[]> resMap, Object object,
                                      Map<String, Integer> modelMap) {
    
    String note = "Use within(rna3d, TYPE) where TYPE is one of: ";
    
    Map<String, SV> data = map0.getMap();
    if (data == null)
      return null;
    try {
      // add _map as a new top-level key pointing to the same thing
      map0.mapPut("_map", SV.newV(T.hash, data));
      Lst<SV> list = new Lst<SV>();

      Set<Entry<String, SV>> set = data.entrySet();
      SV sv;
      Map<String, SV> map;
      for (Entry<String, SV> e : set) {
        sv = e.getValue();
        Lst<SV> structures = sv.getList();
        if (structures != null) {
          String key = e.getKey();
          note += "\"" + key + "\" ";
          SV svPath = SV.newS(key);
          for (int j = structures.size(); --j >= 0;) {
            SV struc = structures.get(j);
            map = struc.getMap();
            sv = map.get("units");
            map.put("_isres", SV.vT);
            Lst<SV> units = (sv == null || sv.tok == T.varray ? sv
                .getList() : sv.tok == T.string ? new Lst<SV>() : null);
            if (units != null) {
              if (sv.tok == T.string) {
                // optional string of unit ids
                String[] svl = PT.split(sv.asString(), ",");
                for (int i = svl.length; --i >= 0;)
                  units.addLast(SV.newS(svl[i].trim()));
              }
              if (units.size() > 0) {
                BS bsAtoms = new BS();
                map.put("_atoms", SV.getVariable(bsAtoms));
                map.put("_path", svPath);
                list.addLast(struc);
                for (int k = units.size(); --k >= 0;) {
                  catalogUnit(viewer, null, units.get(k)
                      .asString(), 0, bsAtoms, modelAtomIndices, resMap,
                      null, modelMap);
                }
              }
            }
          }
        }
      }
      map0.mapPut("_list", SV.newV(T.varray, list));
    } catch (Exception e) {
      Logger.info(e + " while cataloging structures");
      return null;
    }
    return note;
  }

  /**
   * Returns a Lst<Object> of property data in the form name(String),
   * data(float[]), modelIndex (Integer), isGroup (Boolean);
   * 
   */
  @Override
  public Lst<Object> catalogValidations(Viewer viewer, SV map0,
                                        int[] modelAtomIndices,
                                        Map<String, int[]> resMap,
                                        Map<String, Integer> atomMap,
                                        Map<String, Integer> modelMap) {
    Map<String, SV> data = map0.getMap();
    if (data == null)
      return null;
    Lst<Object> retProperties = new Lst<Object>();
    int nModels = modelAtomIndices.length - 1;
    try {
      // get second level, skipping "xxxx":
      data = getMainItem(data).getMap();
      // add _map as a new top-level key pointing to the same thing
      map0.mapPut("_map", SV.newV(T.hash, data));
      Lst<SV> list = new Lst<SV>();
      map0.mapPut("_list", SV.newV(T.varray, list));

      //    {
      //      "1blu": {
      //  -->     "bond_angles": [
      //              {
      //                  "units": [
      //                      "|1|A|TYR|44|CB|||",
      //                      "|1|A|TYR|44|CG|||",
      //                      "|1|A|TYR|44|CD1|||"
      //                  ],
      //                  "value": 5.57
      //              },
      //
      /// each residue ID points to the specific properties that involve them. 
      Set<Entry<String, SV>> set = data.entrySet();
      SV sv;
      Map<String, SV> map;
      for (Entry<String, SV> e : set) {
        float[][] floats = AU.newFloat2(nModels);
        for (int m = nModels; --m >= 0;)
          floats[m] = new float[modelAtomIndices[m + 1] - modelAtomIndices[m]];
        sv = e.getValue();
        Lst<SV> outliers = sv.getList();
        if (outliers == null) {
          map = sv.getMap();
          if (map != null && (sv = map.get("outliers")) != null)
            outliers = sv.getList();
        }
        if (outliers != null) {
          boolean hasUnit = false;
          String key = e.getKey();
          SV svPath = SV.newS(key);
          boolean isRes = false;
          for (int j = outliers.size(); --j >= 0;) {
            SV out = outliers.get(j);
            map = out.getMap();
            sv = map.get("units");
            SV svv = map.get("value");
            float val = (svv == null ? 1 : SV.fValue(svv));
            Lst<SV> units = (val == 0 || sv == null || sv.tok == T.varray ? sv
                .getList() : sv.tok == T.string ? new Lst<SV>() : null);
            if (units != null) {
              if (sv.tok == T.string) {
                // optional string of unit ids
                String[] svl = PT.split(sv.asString(), ",");
                for (int i = svl.length; --i >= 0;)
                  units.addLast(SV.newS(svl[i].trim()));
              }
              if (units.size() > 0) {
                BS bsAtoms = new BS();
                map.put("_atoms", SV.getVariable(bsAtoms));
                map.put("_path", svPath);
                hasUnit = true;
                list.addLast(out);
                for (int k = units.size(); --k >= 0;) {
                  boolean ret = catalogUnit(viewer, floats, units.get(k)
                      .asString(), val, bsAtoms, modelAtomIndices, resMap,
                      atomMap, modelMap);
                  if (ret)
                    map.put("_isres", SV.vT);
                  isRes |= ret;
                }
              }
            }
          }
          if (hasUnit) {
            for (int m = nModels; --m >= 0;)
              if (floats[m] != null) {
                retProperties.addLast(key);
                retProperties.addLast(floats[m]);
                retProperties.addLast(Integer.valueOf(m));
                retProperties.addLast(Boolean.valueOf(isRes));
              }
          }
        }
      }
      return retProperties;
    } catch (Exception e) {
      Logger.info(e + " while cataloging validations");
      return null;
    }
  }

  private SV getMainItem(Map<String, SV> data) {
    for (Entry<String, SV> e : data.entrySet()) {
      String key = e.getKey();
      if (!key.contains("metadata"))
        return e.getValue();
    }
    return null;
  }

  /**
   * We create a main list of mappings, where each mapping has _atoms and _path
   * 
   * @param objAnn
   * @return Lst of mappings
   */
  @Override
  public Lst<SV> initializeAnnotation(SV objAnn, int type, int modelIndex) {
    Map<String, SV> map = objAnn.getMap();
    SV _list = map.get("_list");
    if (_list != null)
      return _list.getList();
    String dataKey = getDataKey(type);
    // assume ONE top-level key
    SV main = getMainItem(map);
    map.put("_map", main);
    boolean noSingles = true; // different for validation
    Map<String, SV> _cat = new Hashtable<String, SV>();
    map.put("_cat", SV.newV(T.hash, _cat));
    Lst<SV> list = new Lst<SV>();
    map.put("_list", _list = SV.newV(T.varray, list));
    for (Entry<String, SV> e : main.getMap().entrySet()) {
      // first level: SCOP, InterPro, etc.
      String _dbName = e.getKey();
      SV _dbMap = e.getValue();
      _cat.putAll(_dbMap.getMap());
      for (Entry<String, SV> e2 : _dbMap.getMap().entrySet()) {
        // second level: ID
        String _domainName = e2.getKey();
        SV _domainMap = e2.getValue();        
        SV _domainList = _domainMap.mapGet(dataKey);
        Lst<SV> _mapList = _domainList.getList();
        for (int i = _mapList.size(); --i >= 0;) {
          SV mapping = _mapList.get(i);
          list.addLast(mapping);
          Map<String, SV> mmap = mapping.getMap();
          SV _chain = mmap.get("chain_id");
          SV start = mmap.get("start");
          SV end = mmap.get("end");
          int res1 = 0;
          int res2 = 0;
          String rescode = "modelIndex=" + modelIndex + "&chain='"
              + _chain.value + "'";
          if (start != null && end != null) {
            res1 = start.mapGet("residue_number").intValue;
            res2 = end.mapGet("residue_number").intValue;
            rescode += "&seqid>=" + res1 + "&seqid<=" + res2;
          } else {
            res2 = 1;
            rescode += "&seqid>0";
          }
          SV _atoms = (noSingles && res1 >= res2 ? SV.getVariable(new BS())
              : _cat.get(rescode));
          if (_atoms == null)
            _cat.put(rescode, _atoms = SV.newS(rescode));
          // note that using SV, we can MUTATE _atoms from String to Bitset
          // and _cat and all references will update, because it is just 
          // a pointer, not a copy.

          mmap.put("_atoms", _atoms);
          mmap.put("_path", SV.newS(_dbName + "." + _domainName));
          mmap.put("domain", _domainMap);
        }
      }
    }
    return list;
  }

  /**
   * find annotations; allows for wild cards InterPro.* where .....
   * 
   * @param vwr
   * @param name
   * @param _list
   * @param key
   * @param bs
   */
  @SuppressWarnings("unchecked")
  private void findAnnotationAtoms(Viewer vwr, String name, Lst<SV> _list,
                                   String key, BS bs) {
    if (_list == null)
      return;
    System.out.println("Checking " + name + " for " + key);
    Object data = vwr.extractProperty(_list, "[" + key + "]", -1);
    Lst<SV> list = null;
    if (data instanceof Lst) {
      list = (Lst<SV>) data;
    } else if (data instanceof SV) {
      list = ((SV) data).getList();
    }
    if (list == null)
      return;

    // go through all mappings
    for (int i = 0, n = list.size(); i < n; i++) {
      Object o = list.get(i);
      Map<String, SV> mapping = (o instanceof SV ? ((SV) o).getMap()
          : (Map<String, SV>) o);
      if (mapping == null)
        return;
      bs.or(setAnnotationAtoms(vwr, mapping, i));
    }
  }

  private BS setAnnotationAtoms(Viewer vwr, Map<String, SV> mapping, int i) {
    SV _atoms = mapping.get("_atoms");
    if (_atoms.tok != T.bitset) {
      BS bs2 = vwr.getAtomBitSet(_atoms.value);
      if (i >= 0)
        Logger.info("#" + (i + 1) + " found " + bs2.cardinality()
            + " atoms for " + _atoms.value);
      // mutate _atoms from String to BitSet!
      // all references are updated instantly!
      _atoms.tok = T.bitset;
      _atoms.value = bs2;
    }
    return (BS) _atoms.value;
  }

  //UnitIDs are based on http://rna.bgsu.edu/main/rna-3d-hub-help/unit-ids/
  //  
  //  Unit Identifier Specification
  //
  //  We describe the type and case sensitivity of each field in the list below. In addition, we list which item in the mmCIF the data for each field comes from. We also show several examples of the IDs and their interpretation at the end.
  //
  //  Unit ids can also be used to identify atoms. When identifying entire residues, the atom field is left blank.
  //
  //      PDB ID Code
  //          From PDBx/mmCIF item: _entry.id
  //          4 characters, case-insensitive
  //      Model Number
  //          From PDBx/mmCIF item: _atom_site.pdbx_PDB_model_num
  //          integer, range 1-99
  //      Chain ID
  //          From PDBx/mmCIF item: _atom_site.auth_asym_id
  //          <= 4 character, case-sensitive
  //      Residue/Nucleotide/Component Identifier
  //          From PDBx/mmCIF item: _atom_site.label_comp_id
  //          1-3 characters, case-insensitive
  //      Residue/Nucleotide/Component Number
  //          From PDBx/mmCIF item: _atom_site.auth_seq_id
  //          integer, range: -999..9999 (there are negative residue numbers)
  //      Atom Name (Optional, default: blank)
  //          From PDBx/mmCIF item: _atom_site.label_atom_id
  //          0-4 characters, case-insensitive
  //          blank means all atoms
  //      Alternate ID (Optional, default: blank)
  //          From PDBx/mmCIF item: _atom_site.label_alt_id
  //          Default value: blank
  //          One of ['A', 'B', '0'], case-insensitive
  //      Insertion Code (Optional, default: blank)
  //          From PDBx/mmCIF item: _atom_site.pdbx_PDB_ins_code
  //          1 character, case-insensitive
  //      Symmetry Operation (Optional, default: 1_555)
  //          As defined in PDBx/mmCIF item: _pdbx_struct_oper_list.name
  //          5-6 characters, case-insensitive
  //          For viral icosahedral structures, use “P_” + model number instead of symmetry operators. For example, 1A34|1|A|VAL|88|||P_1
  //
  //  Examples
  //
  //      Chain A in 1ABC = “1ABC|1|A”
  //      Nucleotide U(10) chain B of 1ABC = “1ABC|1|B|U|10”
  //      Nucleotide U(15A) chain B, default symmetry operator = “1ABC|1|B|U|15|||A”
  //      Nucleotide C(25) chain D subject to symmetry operation 2_655 = “1ABC|1|D|C|25||||2_655”
  //
  //  Unit ids for entire residues can contain 4, 7, or 8 string separators (|).

  /**
   * Carried out for each unit
   * 
   * @param viewer
   * @param vals
   *        model-based array of float values for a given validation type
   * @param unitID
   * @param val
   * @param bsAtoms
   * @param modelAtomIndices
   * @param resMap
   * @param atomMap
   * @param modelMap
   *        TODO
   * 
   * @return true if this is residue-based validation (to be added to H atoms
   *         when pdbAddHydrogens is set
   */
  private boolean catalogUnit(Viewer viewer, float[][] vals, String unitID,
                              float val, BS bsAtoms, int[] modelAtomIndices,
                              Map<String, int[]> resMap,
                              Map<String, Integer> atomMap,
                              Map<String, Integer> modelMap

  ) {

    // (pdbid)|model|chain|RESNAME|resno|ATOMNAME|altcode|inscode|(symmetry)
    //   0       1     2      3      4      5        6       7       8

    // becomes

    // model_chainCode_resno_inscode
    // model_chainCode_resno_inscode_ATOMNAME_altcode
    //   

    String[] s = PT.split(unitID + (vals == null ? "||||" : "|||"), "|");
    // must have at least model, chain, resname, and resno
    if (s.length < 8 || s[1].length() == 0 || s[2].length() == 0
        || s[3].length() == 0 || s[4].length() == 0)
      return false;
    String sm = (s[1].length() == 0 ? "1" : s[1]);
    int m = (modelMap == null ? PT.parseInt(sm) - 1 : -1);
    Integer im = (m >= 0 ? null : modelMap.get(sm));
    if (im != null)
      m = im.intValue();
    if (m >= modelAtomIndices.length)
      return false;
    String res = s[1] + "_" + viewer.getChainID(s[2], true) + "_" + s[4] + "_"
        + s[7].toLowerCase();
    int i0 = modelAtomIndices[m];
    boolean isRes = (atomMap == null || s[5].length() == 0);
    if (isRes) {
      int[] a2 = resMap.get(res);
      if (a2 != null)
        for (int j = a2[1], j0 = a2[0]; --j >= j0;) {
          bsAtoms.set(i0 + j);
          if (vals != null)
            vals[m][j] += Math.abs(val);
        }
    } else {
      if (s[5].charAt(0) == 'H')
        s[5] = getAttachedAtomForPDBH(s[3], s[5]);
      String atom = res + "_" + s[5] + "_" + s[6].toLowerCase();
      Integer ia = atomMap.get(atom);
      if (ia != null) {
        int j = ia.intValue();
        bsAtoms.set(i0 + j);
        if (vals != null)
          vals[m][j] += Math.abs(val);
      }
    }
    return isRes;
  }

  ///////////////////// general post-load processing ////////////////

  @Override
  /**
   * 
   * Retrieve a set of atoms using vwr.extractProperty with 
   * and for other annotations
   * 
   */
  public BS getAtomBits(Viewer vwr, String key, Object dbObj,
                        Map<String, Object> annotationCache, int type,
                        int modelIndex, BS bsModel) {
    if (dbObj == null)
      return new BS();
    //boolean isStruc = (type == T.rna3d);
    //boolean isDomains = (type == T.domains);
    //boolean isValidation = (type == T.validation);
    boolean doCache = !key.contains("NOCACHE");
    if (!doCache) {
      key = PT.rep(key, "NOCACHE", "").trim();
    }
    BS bs = (doCache ? (BS) annotationCache.get(key) : null);
    if (bs != null)
      return bs;
    bs = new BS();
    if (doCache)
      annotationCache.put(key, bs);
    try {
      Lst<SV> list = initializeAnnotation((SV) dbObj, type, modelIndex);
      // select within(domains,"InterPro where identifier like '*-like'")
      int pt = key.toLowerCase().indexOf(" where ");
      String path = PT.rep((pt < 0 ? key : key.substring(0, pt)), " ", "");
      String newKey = (pt < 0 ? "" : key.substring(pt + 7).trim());
      if (path.indexOf(".") < 0) {
        path = " _path like '" + path + "*'";
      } else {
        path = " _path='" + path + "'";
      }
      newKey = "select * where "
          + (pt < 0 ? path : "(" + newKey + ") and (" + path + ")");
      Logger.info("looking for " + newKey);
      // this is either the right map or we have a wildcard.
      findAnnotationAtoms(vwr, path, list, newKey, bs);
      bs.and(bsModel);
    } catch (Exception e) {
      System.out.println(e.toString() + " in AnnotationParser");
      bs.clearAll();
    }
    return bs;
  }

  /////////// EBI validation /////////////

  /**
   * Get all validation values corresponding to a specific validation type. Used
   * by label %[validation.clashes]
   * 
   * @param vwr
   * @param type
   *        e.g. "clashes"
   * @param atom
   * @return a list of Float values associated with this atom and this type of
   *         validation
   */
  @Override
  public Lst<Float> getAtomValidation(Viewer vwr, String type, Atom atom) {
    int i = 0;
    int n = 0;

    Lst<Float> l = null;
    Map<String, SV> map = null;
    Lst<SV> list = null;
    try {
      int ia = atom.i;
      l = new Lst<Float>();
      list = ((SV) vwr.ms.getModelAuxiliaryInfo(atom.mi).get("validation"))
          .mapGet("_list").getList();

      for (i = 0, n = list.size(); i < n; i++) {
        map = list.get(i).getMap();
        if (map.get("_path").value.equals(type)
            && ((BS) map.get("_atoms").value).get(ia)) {
          SV v = map.get("value");
          l.addLast(v.tok == T.decimal ? (Float) v.value : Float.valueOf(v
              .asFloat()));
        }
      }
      return l;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * 
   * Get a string report of annotation data
   * 
   * @param a
   *        an annotation structure wrapped as a script variable
   * @param match
   *        can contain "mappings" to get those specifically
   * 
   * @return tab-separated line-based listing
   */
  @Override
  public String getAnnotationInfo(Viewer vwr, SV a, String match, int type,
                                  int modelIndex) {
    SB sb = new SB();
    if ("".equals(match))
      match = null;
    boolean isDetail = (match != null && (match.equals("all") || match
        .endsWith(" all")));
    if (isDetail) {
      Lst<SV> _list = initializeAnnotation(a, type, modelIndex);
      for (int i = _list.size(); --i >= 0;)
        setAnnotationAtoms(vwr, _list.get(i).getMap(), -1);
      match = match.substring(0, Math.max(0, match.length() - 4)).trim();
    }
    if ("".equals(match))
      match = null;
    if (type == T.validation && !isDetail && match == null)
      return a.mapGet("_note").asString();
    boolean isMappingOnly = (match != null && match.indexOf(".") >= 0 && match
        .indexOf(".*") < 0);
    match = PT.rep(match, "*", "");
    try {
      getAnnotationKVPairs(a, match, "", sb, "", isDetail, isMappingOnly, type);
    } catch (Exception e) {
      /**
       * @j2sNative
       * 
       *            System.out.println(e);
       */
      {
        System.out.println(e.getStackTrace());
      }
    }
    return sb.toString();
  }

  private static Map<String, String> pdbAtomForH;

  /**
   * Finds the standard attached heavy atom for a PDB H atom; used in EBI clash
   * validation.
   * 
   * @param group3
   * @param name
   * @return name of attached atom or hName
   */
  public String getAttachedAtomForPDBH(String group3, String name) {
    if (name.charAt(0) == 'H') {
      if (pdbAtomForH == null) {
        pdbAtomForH = new Hashtable<String, String>();
        assignPDBH(
            "",
            "N H H1 H2 H3 CB HB2 HB3 CD HD2 HD3 CG HG2 HG3 C2' H2'' H2' C5' H5'' H5' OXT HXT");
        
        for (int i = BioResolver.pdbBondInfo.length; --i >= 1;) {
          assignPDBH(Group.group3Names[i], BioResolver.pdbBondInfo[i]);
        }
      }
      String a = pdbAtomForH.get(name);
      if (a == null)
        a = pdbAtomForH.get(group3 + name);
      if (a != null)
        return a;
    }
    return name;
  }

  private void assignPDBH(String group3, String sNames) {
    String[] names = PT.getTokens(PT.rep(sNames, "@", " "));
    String a = null;
    for (int i = 0, n = names.length; i < n; i++) {
      String s = names[i];
      if (s.charAt(0) != 'H') {
        // just assigning attached atom
        a = s;
        continue;
      }
      // this is an H
      s = group3 + s;
      if (s.indexOf("?") >= 0) {
        // CH3 groups
        s = s.substring(0, s.length() - 1);
        pdbAtomForH.put(s + "1", a);
        pdbAtomForH.put(s + "2", a);
        pdbAtomForH.put(s + "3", a);
      } else {
        pdbAtomForH.put(s, a);
      }
    }
  }

  /**
   * Adjusts _atoms bitset to account for added hydrogen atoms. A margin of 20
   * allows for 20 added H atoms per group
   * 
   */
  @Override
  public void fixAtoms(int modelIndex, SV dbObj, BS bsAddedMask, int type,
                       int margin) {
    Lst<SV> _list = initializeAnnotation(dbObj, type, modelIndex);
    for (int i = _list.size(); --i >= 0;) {
      Map<String, SV> m = _list.get(i).getMap();
      SV _atoms = m.get("_atoms");
      if (_atoms != null && _atoms.tok == T.bitset)
        BSUtil.shiftBits((BS) _atoms.value, bsAddedMask, _list.get(i).mapGet("_isres") != null, ((BS) _atoms.value).length() + margin);
    }
  }

  // DSSR stuff
  
  @Override
  public void getBasePairs(Viewer vwr, int modelIndex) {
  }

  @Override
  public String calculateDSSRStructure(Viewer vwr, BS bsAtoms) {
    return null;
  }

  @Override
  public String fixDSSRJSONMap(Map<String, Object> map) {
    return null;
  }

  @Override
  public String getHBonds(ModelSet ms, int modelIndex, Lst<Bond> vHBonds,
                          boolean doReport) {
    return null;
  }

  @Override
  public void getAtomicDSSRData(ModelSet ms, int modelIndex, float[] dssrData, String dataType) {
  }

  @Override
  public void setGroup1(ModelSet ms, int modelIndex) {
  }
  
  @Override
  public P3[] getDSSRFrame(Map<String, Object> dssrNT) {
    return null;
  }



}
