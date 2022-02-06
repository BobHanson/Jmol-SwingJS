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

import java.util.Map;

import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.PT;

import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.HBond;
import org.jmol.modelset.Model;
import org.jmol.modelset.ModelSet;
import org.jmol.modelsetbio.BasePair;
import org.jmol.modelsetbio.BioModel;
import org.jmol.modelsetbio.BioPolymer;
import org.jmol.modelsetbio.NucleicMonomer;
import org.jmol.modelsetbio.NucleicPolymer;
import org.jmol.script.T;
import org.jmol.util.C;
import org.jmol.util.Edge;
import org.jmol.util.Escape;
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
public class DSSR1 extends AnnotationParser {

  /**
   * The paths to the unit id data within the structure.
   * 
   * This is one long string; all lowercase, each surrounded by double periods.
   * 
   */
  
  private final static String DSSR_PATHS = 
      "..bulges.nts_long" +
      "..coaxstacks.stems.pairs.nt*" +
      "..hairpins.nts_long" +
      "..hbonds.atom1_id;atom2_id" +
      "..helices.pairs.nt*" +
      "..iloops.nts_long" +
      "..isocanonpairs.nt*" +
      "..junctions.nts_long" +
      "..kissingloops.hairpins.nts_long" +
      "..multiplets.nts_long" +
      "..nonstack.nts_long" +
      "..nts.nt_id" +
      "..pairs.nt*" +
      "..sssegments.nts_long" +
      "..stacks.nts_long" +
      "..stems.pairs.nt*" +
      "..";

  public DSSR1() {
    // for reflection
  }

  @Override
  public String calculateDSSRStructure(Viewer vwr, BS bsAtoms) {
    BS bs = vwr.ms.getModelBS(bsAtoms == null ? vwr.bsA() : bsAtoms, true);
    String s = "";
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
      s += getDSSRForModel(vwr, i) + "\n";
    return s;
  }

  @SuppressWarnings("unchecked")
  private String getDSSRForModel(Viewer vwr, int modelIndex) {
    Map<String, Object> info = null;
    String out = null;
    while (true) {
      if (!vwr.ms.am[modelIndex].isBioModel)
        break;
      info = vwr.ms.getModelAuxiliaryInfo(modelIndex);
      if (info.containsKey("dssr"))
        break;
      BS bs = vwr.restrictToModel(vwr.ms.getAtoms(T.nucleic, null), modelIndex);
      if (bs.nextClearBit(0) < 0) {
        info = null;
        break;
      }
      try {
        String name = (String) vwr.setLoadFormat("=dssrModel/", '=', false);
        name = PT.rep(name, "%20", " ");
        Logger.info("fetching " + name + "[pdb data]");
        String data = vwr.getPdbAtomData(bs, null, false, false);
        int modelNumber = vwr.getModelNumber(vwr.ms.getModelBS(bs,false).nextSetBit(0));
        String s =   "          " + modelNumber;
        data = "MODEL" + s.substring(s.length() - 9) + "\n" + data + "ENDMDL\n";  
        data = vwr.getFileAsString3(name + data, false, null);
        Map<String, Object> x = vwr.parseJSONMap(data);
        if (x != null) {
          info.put("dssr", x);
          setGroup1(vwr.ms, modelIndex);
          fixDSSRJSONMap(x);
          setBioPolymers((BioModel) vwr.ms.am[modelIndex], false);
        }
      } catch (Throwable e) {
        info = null;
        out = "" + e;
      }
      break;
    }
    return (info != null ? PT.rep(Escape.escapeMap((Map<String, Object>) ((Map<String, Object>) info.get("dssr"))
        .get("counts")),",",",\n") : out == null ? "model has no nucleotides" : out);
  }

  /**
   * kissingLoops and coaxStacks use index arrays instead of duplication;
   * 
   * @param map
   * @return msg
   */
  @Override
  public String fixDSSRJSONMap(Map<String, Object> map) {
    String s = "";
    
    try {

      fixIndices(map, "kissingLoops", "hairpin");
      fixIndices(map, "coaxStacks", "stem");
      
//      lst = (Lst<Object>) map.get("hbonds");
//      if (lst != null) {
//        for (int i = lst.size(); --i >= 0;) {
//          Map<String, Object> smap = (Map<String, Object>) lst.get(i);
//          smap.put("res_long", removeUnitAtom((String) smap.get("atom1_id"))
//              + "," + removeUnitAtom((String) smap.get("atom2_id")));
//        }
//      }  

      if (map.containsKey("counts"))
        s += "_M.dssr.counts = " + map.get("counts").toString() + "\n";
      if (map.containsKey("dbn"))
        s += "_M.dssr.dbn = " + map.get("dbn").toString();
    } catch (Throwable e) {
      // ignore??
    }

    return s;
  }

  /**
   * create a key/value pair root+"s" for all indices of root+"_indices"
   * @param map
   * @param key
   * @param root
   */
  @SuppressWarnings("unchecked")
  private void fixIndices(Map<String, Object> map, String key,
                          String root) {
    String indices = root + "_indices";
    String original = root + "s";
    Lst<Object>lst = (Lst<Object>) map.get(key);
    if (lst != null) {
      Lst<Object>  hpins = (Lst<Object>) map.get(original);
      for (int i = lst.size(); --i >= 0;) {
        Map<String, Object> kmap = (Map<String, Object>) lst.get(i);
        Lst<Object> khlist = (Lst<Object>) kmap.get(indices);
        int n = khlist.size();
        if (n > 0) {
          Lst<Object> khpins = new Lst<Object>();
          kmap.put(original, khpins);
          for (int j = n; --j >= 0;)
            khpins.addLast(hpins.get(((Integer) khlist.get(j)).intValue() - 1));
        }
      }
    }
  }

//  private static String removeUnitAtom(String unitID) {
//    int pt1 = 0;
//    int pt2 = unitID.length();
//    for (int i = 0, pt = -1; i < 7 && (pt = unitID.indexOf("|", pt + 1)) >= 0;i++) {
//      switch (i) {
//      case 4:
//        pt1 = pt + 1;
//        break;
//      case 6:
//        pt2 = pt;
//        break;
//      }
//    }
//    unitID = unitID.substring(0, pt1) + "|" + unitID.substring(pt2);
//    return unitID;
//  }

   @SuppressWarnings("unchecked")
  @Override
  public void getBasePairs(Viewer vwr, int modelIndex) {
    ModelSet ms = vwr.ms;
    Map<String, Object> info = (Map<String, Object>) ms.getInfo(modelIndex,
        "dssr");
    Lst<Map<String, Object>> pairs = (info == null ? null
        : (Lst<Map<String, Object>>) info.get("pairs"));
    Lst<Map<String, Object>> singles = (info == null ? null
        : (Lst<Map<String, Object>>) info.get("ssSegments"));
    if (pairs == null && singles == null) {
      setBioPolymers((BioModel) vwr.ms.am[modelIndex], true);
      return;
    }
    BS bsAtoms = ms.am[modelIndex].bsAtoms;
    try {
      BS bs = new BS();
      Atom[] atoms = ms.at;
      if (pairs != null)
        for (int i = pairs.size(); --i >= 0;) {
          Map<String, Object> map = pairs.get(i);
          String unit1 = (String) map.get("nt1");
          String unit2 = (String) map.get("nt2");
          int a1 = ms.getSequenceBits(unit1, bsAtoms, bs).nextSetBit(0);
          bs.clearAll();
          int a2 = ms.getSequenceBits(unit2, bsAtoms, bs).nextSetBit(0);
          bs.clearAll();
          BasePair.add(map, setRes(atoms[a1]), setRes(atoms[a2]));
        }
      if (singles != null)
        for (int i = singles.size(); --i >= 0;) {
          Map<String, Object> map = singles.get(i);
          String units = (String) map.get("nts_long");
          ms.getSequenceBits(units, bsAtoms, bs);
          for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1))
            setRes(atoms[j]);
        }
    } catch (Throwable e) {
      Logger.error("Exception " + e + " in DSSRParser.getBasePairs");
    }

  }

  private void setBioPolymers(BioModel m, boolean b) {
    int n = m.getBioPolymerCount();
    for (int i = n; --i >= 0;) {
      BioPolymer bp = m.bioPolymers[i];
      if (bp.isNucleic())
        ((NucleicPolymer) bp).isDssrSet = b;
    }
  }

  private NucleicMonomer setRes(Atom atom) {
    if (atom.group.getBioPolymerLength() == 0)
      return  null; // HPA ligand in 4fe5 can be in a pair in DSSR
    NucleicMonomer m = (NucleicMonomer) atom.group;
    ((NucleicPolymer) m.bioPolymer).isDssrSet = true;
    return m;
  }


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
//    System.out.println("testing DSSR1");
    BS bs = null;// (doCache ? (BS) annotationCache.get(key) : null);
//    if (bs != null)
//      return bs;
    bs = new BS();
    if (doCache)
      annotationCache.put(key, bs);
    try {
      // drilling
      // allow  select on within(dssr, "nts[SELECT * WHERE v0>1 and v1 <0]")
      // allow  select on within(dssr, "nts[WHERE v0>1 and v1 <0]")
      key = PT.rep(key, "[where", "[select * where");
      key = PT.rep(key, "[WHERE", "[select * where");
      
      String ext = "";
      int n = Integer.MIN_VALUE;
      int pt = key.toLowerCase().indexOf("[select");
      if (pt >= 0) {
        ext = key.substring(pt);
        key = key.substring(0, pt);
        // allow  select on within(dssr, "nts[WHERE v0>1 and v1 <0]..2")
        pt = ext.lastIndexOf("]..");
        if (pt >= 0 && (n = PT.parseInt(ext.substring(pt + 3))) != Integer.MIN_VALUE) 
          ext = ext.substring(0, pt + 1);          
      }
      pt = key.toLowerCase().indexOf(" where ");
      if (pt < 0) {
        // pairs   stems    etc.
        key = key.toLowerCase();
        pt = (n == Integer.MIN_VALUE ? key.lastIndexOf('.') : -1);
        // allow  select on within(dssr, "nts.2")
        boolean haveIndex = false;
        if (pt >= 0 && 
            (haveIndex = (n = PT.parseInt(key.substring(pt +1))) != Integer.MIN_VALUE)) 
          key = key.substring(0, pt);
        pt = DSSR_PATHS.indexOf(".." + key) + 2;
        int len = key.length();
        if (pt < 2)
          return bs;
        int ptLast = (haveIndex ? pt + len : Integer.MAX_VALUE);
        while (pt >= 2 && pt < ptLast && len > 0) {
          if (key.indexOf(".") < 0 && DSSR_PATHS.substring(pt + len, pt + len + 2).equals("..")) {
            key = "[select (" + key + ")]";
          }
          dbObj = vwr.extractProperty(dbObj, key, -1);
          pt += len + 1;
          if (ext.length() > 0) {
            dbObj = vwr.extractProperty(dbObj, ext, -1);
            ext = "";
          }
          int pt1 = DSSR_PATHS.indexOf(".", pt);
          key = DSSR_PATHS.substring(pt, pt1);
          len = key.length();
        }
      } else {
        // select within(dssr, "pairs where bp='G-C' or bp='C-G'")
        key = key.substring(0, pt).trim() + "[select * "
            + key.substring(pt + 1) + "]" + ext;
        dbObj = vwr.extractProperty(dbObj, key, -1);
      }
      if (n != Integer.MIN_VALUE && dbObj instanceof Lst) {
        if (n <= 0)
          n += ((Lst<?>) dbObj).size();
        dbObj = ((Lst<?>) dbObj).get(n - 1);        
      }
      bs.or(vwr.ms.getAtoms(T.sequence, dbObj.toString()));
      bs.and(bsModel);
    } catch (Throwable e) {
      // "Throwable" because array out of bounds in JavaScript is not an Exception
      System.out.println(e.toString() + " in AnnotationParser");
      bs.clearAll();
    }
    return bs;
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public String getHBonds(ModelSet ms, int modelIndex, Lst<Bond> vHBonds,
                          boolean doReport) {
    Map<String, Object> info = (Map<String, Object>) ms.getInfo(modelIndex, "dssr");
    Lst<Object> list;
    if (info == null || (list = (Lst<Object>) info.get("hbonds")) == null)
      return "no DSSR hydrogen-bond data";
    BS bsAtoms = ms.am[modelIndex].bsAtoms; 
    String unit1 = null, unit2 = null;
    int a1 = 0, a2 = 0;
    try {
      BS bs = new BS();
      for (int i = list.size(); --i >= 0;) {
        Map<String, Object> map = (Map<String, Object>) list.get(i); 
        unit1 = (String) map.get("atom1_id");    
        a1 = ms.getSequenceBits(unit1, bsAtoms, bs).nextSetBit(0);
        if (a1 < 0) {
          Logger.error("Atom " + unit1 + " was not found");
          continue;
        }
        unit2 = (String) map.get("atom2_id");
        bs.clearAll();
        a2 = ms.getSequenceBits(unit2, bsAtoms, bs).nextSetBit(0);
        if (a2 < 0) {
          Logger.error("Atom " + unit2 + " was not found");
          continue;
        }
        bs.clearAll();
        float energy = 0;
        vHBonds.addLast(new HBond(ms.at[a1], ms.at[a2], Edge.BOND_H_REGULAR,
            (short) 1, C.INHERIT_ALL, energy));
      }
    } catch (Throwable e) {
    }
    return "DSSR reports " + list.size() + " hydrogen bonds";
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public void setGroup1(ModelSet ms, int modelIndex) {
    Map<String, Object> info = (Map<String, Object>) ms.getInfo(modelIndex,
        "dssr");
    Lst<Map<String, Object>> list;
    if (info == null
        || (list = (Lst<Map<String, Object>>) info.get("nts")) == null)
      return;
    Model m = ms.am[modelIndex];
    BS bsAtoms = m.bsAtoms;
    Atom[] atoms = ms.at;
    BS bs = new BS();
    for (int i = list.size(); --i >= 0;) {
      Map<String, Object> map = list.get(i);
      char ch = ((String) map.get("nt_code")).charAt(0);
      String unit1 = (String) map.get("nt_id");
      ms.bioModelset.getAllSequenceBits(unit1, bsAtoms, bs);
      int pt = bs.nextSetBit(0);
      if (pt < 0)
        continue;
      if ("ACGTU".indexOf(ch) < 0)
        atoms[pt].group.group1 = ch;
      atoms[pt].group.dssrNT = map;
      bs.clearAll();
    }
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public void getAtomicDSSRData(ModelSet ms, int modelIndex, float[] dssrData, String dataType) {
    Map<String, Object> info = (Map<String, Object>) ms.getInfo(modelIndex, "dssr");
    Lst<Object> list;
    if (info == null || (list = (Lst<Object>) info.get(dataType)) == null)
      return;
    BS bsAtoms = ms.am[modelIndex].bsAtoms; 
    try {
      BS bs = new BS();
      for (int i = list.size(); --i >= 0;) {
        Map<String, Object> map = (Map<String, Object>) list.get(i);
        bs.clearAll();
        ms.getSequenceBits(map.toString(), bsAtoms, bs);
        for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1))
          dssrData[j] = i;
      }
    } catch (Throwable e) {
    }
  }


  @SuppressWarnings("unchecked")
  @Override
  public P3[] getDSSRFrame(Map<String, Object> nt) {
    Map<String, Object> frame = (Map<String, Object>) nt.get("frame");
    if (frame == null)
      return null;
    P3[] oxyz = new P3[4];
    for (int i = 4; --i >= 0;)
      oxyz[i] = new P3();
    getPoint(frame, "origin", oxyz[0]);
    getPoint(frame, "x_axis", oxyz[1]);
    getPoint(frame, "y_axis", oxyz[2]);
    getPoint(frame, "z_axis", oxyz[3]);
    return oxyz;
  }

  @SuppressWarnings("unchecked")
  private void getPoint(Map<String, Object> frame, String item, P3 pt) {
    Lst<Float> xyz = (Lst<Float>) frame.get(item);
    pt.x = xyz.get(0).floatValue();
    pt.y = xyz.get(1).floatValue();
    pt.z = xyz.get(2).floatValue();    
  }

//  "purine"  :
//  [
//       12 ATOMS,    12 BONDS
//        1 N      -2.2500   5.0000   0.2500
//        2 N      -2.2500   0.5000   0.2500
//        3 N      -2.2500   0.5000  -0.2500
//        4 N      -2.2500   5.0000  -0.2500
//        5 C       2.2500   5.0000   0.2500
//        6 C       2.2500   0.5000   0.2500
//        7 C       2.2500   0.5000  -0.2500
//        8 C       2.2500   5.0000  -0.2500
//        9 C      -2.2500   5.0000   0.2500
//       10 C      -2.2500   0.5000   0.2500
//       11 C      -2.2500   0.5000  -0.2500
//       12 C      -2.2500   5.0000  -0.2500
//        1     1     2
//        2     2     3
//        3     3     4
//        4     4     1
//        5     5     6
//        6     6     7
//        7     7     8
//        8     5     8
//        9     9     5
//       10    10     6
//       11    11     7
//       12    12     8
//  ]
//  "pyrimidine"  :
//  [
//       12 ATOMS,    12 BONDS
//        1 N      -2.2500   5.0000   0.2500
//        2 N      -2.2500   2.0000   0.2500
//        3 N      -2.2500   2.0000  -0.2500
//        4 N      -2.2500   5.0000  -0.2500
//        5 C       2.2500   5.0000   0.2500
//        6 C       2.2500   2.0000   0.2500
//        7 C       2.2500   2.0000  -0.2500
//        8 C       2.2500   5.0000  -0.2500
//        9 C      -2.2500   5.0000   0.2500
//       10 C      -2.2500   2.0000   0.2500
//       11 C      -2.2500   2.0000  -0.2500
//       12 C      -2.2500   5.0000  -0.2500
//        1     1     2
//        2     2     3
//        3     3     4
//        4     4     1
//        5     5     6
//        6     6     7
//        7     7     8
//        8     5     8
//        9     9     5
//       10    10     6
//       11    11     7
//       12    12     8
//  ]
//  "wc_pair"  :
//  [
//       12 ATOMS,    12 BONDS
//        1 N      -2.2500   5.0000   0.2500
//        2 N      -2.2500  -5.0000   0.2500
//        3 N      -2.2500  -5.0000  -0.2500
//        4 N      -2.2500   5.0000  -0.2500
//        5 C       2.2500   5.0000   0.2500
//        6 C       2.2500  -5.0000   0.2500
//        7 C       2.2500  -5.0000  -0.2500
//        8 C       2.2500   5.0000  -0.2500
//        9 C      -2.2500   5.0000   0.2500
//       10 C      -2.2500  -5.0000   0.2500
//       11 C      -2.2500  -5.0000  -0.2500
//       12 C      -2.2500   5.0000  -0.2500
//        1     1     2
//        2     2     3
//        3     3     4
//        4     4     1
//        5     5     6
//        6     6     7
//        7     7     8
//        8     5     8
//        9     9     5
//       10    10     6
//       11    11     7
//       12    12     8
//  ]

  
}
