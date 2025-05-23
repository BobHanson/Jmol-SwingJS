/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-26 16:57:51 -0500 (Thu, 26 Apr 2007) $
 * $Revision: 7502 $
 *
 * Copyright (C) 2005  The Jmol Development Team
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

package org.jmol.smiles;

import org.jmol.api.SmilesMatcherInterface;
import org.jmol.modelset.Atom;
import org.jmol.util.BSUtil;
import org.jmol.util.Edge;
import org.jmol.util.Elements;
import org.jmol.util.Logger;
import org.jmol.util.Node;
import org.jmol.util.Point3fi;
import org.jmol.viewer.JC;

import javajs.util.AU;
import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.P3d;
import javajs.util.PT;

/**
 * 
 * @j2sExport
 * 
 * Originating author: Nicholas Vervelle
 * 
 * A class to handle a variety of SMILES/SMARTS-related functions, including: --
 * determining if two SMILES strings are equivalent -- determining the molecular
 * formula of a SMILES or SMARTS string -- searching for specific runs of atoms
 * in a 3D model -- searching for specific runs of atoms in a SMILES description
 * -- generating valid (though not canonical) SMILES and bioSMILES strings --
 * getting atom-atom correlation maps to be used with biomolecular alignment
 * methods
 * 
 * <p>
 * The original SMILES description can been found at the <a
 * href="http://www.daylight.com/smiles/">SMILES Home Page</a>.
 * 
 * Specification for this implementation can be found in package.html.
 * 
 * <p>
 * 
 * <pre>
 * <code>
 * public methods:
 * 
 * int areEqual  -- checks a SMILES string against a reference (-1 for error; 0 for no finds; >0 for number of finds)
 * 
 * BitSet[] find  -- finds one or more occurances of a SMILES or SMARTS string within a SMILES string
 * 
 * int[][] getCorrelationMaps  -- returns correlated arrays of atoms
 * 
 * String getLastError  -- returns any error that was last encountered.
 * 
 * String getMolecularFormula   -- returns the MF of a SMILES or SMARTS string
 * 
 * String getRelationship -- returns isomeric relationship
 * 
 * String getSmiles  -- returns a standard SMILES string or a
 *                  Jmol BIOSMILES string with comment header.
 * 
 * BitSet getSubstructureSet  -- returns a single BitSet with all found atoms included
 *   
 *   
 *   in Jmol script:
 *   
 *   string2.find("SMILES", string1)
 *   string2.find("SMARTS", string1)
 *   
 *   e.g.
 *   
 *     print "CCCC".find("SMILES", "C[C]")
 * 
 *   select search("smartsString")
 *   
 *   All bioSMARTS strings begin with ~ (tilde).
 *   
 * </code>
 * </pre>
 * 
 * @author Bob Hanson
 * 
 */
public class SmilesMatcher implements SmilesMatcherInterface {

  public static boolean j2sHeadless = true;
  
  // internal flags
  
  private final static int MODE_BITSET       = 0x01;
  private final static int MODE_ARRAY        = 0x02;
  private final static int MODE_MAP          = 0x03;
  private static final int MODE_ATROP        = 0x04;
  private final static int MODE_BOOLEAN      = 0x05;

  private boolean okMF = true;

  @Override
  public String getLastException() {
    return (okMF == true ? InvalidSmilesException.getLastError() : "MF_FAILED");
  }

  @Override
  public String getMolecularFormula(Object pattern, boolean isSmarts, boolean isEmpirical)
      throws Exception {
    clearExceptions();
    // note: Jmol may undercount the number of hydrogen atoms
    // for aromatic amines where the ring bonding to N is 
    // not explicit. Each "n" will be assigned a bonding count
    // of two unless explicitly indicated as -n-.
    // Thus, we take the position that "n" is the 
    // N of pyridine unless otherwise indicated.
    //
    // For example:
    //   $ print "c1ncccc1C".find("SMILES","MF")
    //   H 7 C 5 N 1   (correct)
    //   $ print "c1nc-n-c1C".find("SMILES","MF")
    //   H 6 C 4 N 2   (correct)
    // but
    //   $ print "c1ncnc1C".find("SMILES","MF")
    //   H 5 C 4 N 2   (incorrect)
    SmilesSearch search = SmilesParser.newSearch("/nostereo/"+pattern.toString(), isSmarts, true);
    search.createTopoMap(null);
    search.nodes = search.target.nodes;
    return search.getMolecularFormula(!isSmarts, null, isEmpirical);
  }

  private void clearExceptions() {
    InvalidSmilesException.clear();
  }

  /**
   * internal to Jmol -- called by org.jmol.Viewer.getSmiles
   */

  @Override
  public String getSmiles(Node[] atoms, int ac, BS bsSelected,
                          String bioComment, int flags) throws Exception {
    clearExceptions();
    return (new SmilesGenerator()).getSmiles(this, atoms, ac, bsSelected, bioComment, flags);
  }

  @Override
  public int areEqual(Object smiles1, Object smiles2) throws Exception {
    clearExceptions();
    if ((smiles1 == null) != (smiles2 == null))
    		return 0;
    if (smiles1 == null)
    	return 1;
    boolean isWild = false;
    if (smiles1 instanceof String && smiles2 instanceof String) {
      isWild = (((String) smiles1).indexOf("*") >= 0);
    if (!isWild && smiles1.equals(smiles2))
      return 1;
    }
    int flags = (isWild ? JC.SMILES_TYPE_SMARTS
        : JC.SMILES_TYPE_SMILES) | JC.SMILES_FIRST_MATCH_ONLY;    
    BS[] result = (BS[]) matchPriv(smiles1, null, 0, null, null, false, flags, MODE_ARRAY, 
    		newSearch(smiles2));
    return (result == null ? -1 : result.length);
  }

  /**
   * for JUnit test, mainly
   * 
   * @param smiles
   * @param search
   * @return true only if the SMILES strings match and there are no errors
   * @throws Exception
   */
  public boolean areEqualTest(String smiles, SmilesSearch search)
      throws Exception {
    search.set();
    BS[] ret = (BS[]) matchPriv(smiles, null, 0, null, null, false, JC.SMILES_TYPE_SMILES
        | JC.SMILES_FIRST_MATCH_ONLY, MODE_ARRAY, search);
    return (ret != null && ret.length == 1);
  }

  /**
   * 
   * Searches for all matches of a pattern within a SMILES string. If SMILES
   * (not isSmarts), requires that all atoms be part of the match.
   * 
   * 
   * @param pattern
   *        SMILES or SMARTS pattern or SmilesSearch from compiling of a pattern string.
   * @param target
   * @param flags
   * @return array of correlations of occurances of pattern within smiles
   * @throws Exception
   */
  @Override
  public int[][] find(Object pattern, Object target, int flags)
      throws Exception {
    clearExceptions();
    if (target instanceof String)
      target = SmilesParser.cleanPattern((String) target);
    if (pattern instanceof String)
    	pattern = SmilesParser.cleanPattern((String) pattern);
    // search flags will be set in findPriv
    SmilesSearch search = newSearch(target); 
    /// smiles chirality is fixed here
    int[][] array = (int[][]) matchPriv(pattern, null, 0, null, null, false, flags, MODE_MAP, search);
    for (int i = array.length; --i >= 0;) {
      int[] a = array[i];
      for (int j = a.length; --j >= 0;)
        a[j] = ((SmilesAtom) search.target.nodes[a[j]]).mapIndex;
    }
    return array;
  }


  @Override
  public Node[] getAtoms(Object target)
      throws Exception {
    clearExceptions();
    SmilesSearch search = newSearch(target);
    search.createTopoMap(new BS());
    return search.target.nodes;
  }


  private SmilesSearch newSearch(Object s) throws Exception {
    return (s == null ? null
        : s instanceof SmilesSearch ? (SmilesSearch) s
            : SmilesParser.newSearch(SmilesParser.cleanPattern(s.toString()),
                false, true));
  }

  @Override
  public String getRelationship(String smiles1, String smiles2)
      throws Exception {
    if (smiles1 == null || smiles2 == null || smiles1.length() == 0
        || smiles2.length() == 0)
      return "";
    String mf1 = getMolecularFormula(smiles1, false, false);
    String mf2 = getMolecularFormula(smiles2, false, false);
    if (!mf1.equals(mf2))
      return "none";
    boolean check;
    // note: find smiles1 IN smiles2 here
    int n1 = PT.countChar(PT.rep(smiles1, "@@", "@"), '@');
    int n2 = PT.countChar(PT.rep(smiles2, "@@", "@"), '@');
    check = (n1 == n2 && areEqual(smiles2, smiles1) > 0);
    if (!check) {
      // MF matched, but didn't match SMILES
      String s = smiles1 + smiles2;
      if (s.indexOf("/") >= 0 || s.indexOf("\\") >= 0 || s.indexOf("@") >= 0) {
        if (n1 == n2 && n1 > 0 && s.indexOf("@SP") < 0) {
          // reverse chirality centers
          check = (areEqual("/invertstereo/" + smiles2, smiles1) > 0);
          if (check)
            return "enantiomers";
        }
        // remove all stereochemistry from SMILES string
        check = (areEqual("/nostereo/" + smiles2, smiles1) > 0);
        if (check)
          return (n1 == n2 ? "diastereomers" : "ambiguous stereochemistry!");
      }
      // MF matches, but not enantiomers or diastereomers
      return "constitutional isomers";
    }
    return "identical";
  }

  /**
   * Note, this may be incompatible with [$(select(..))]
   * 
   * THIS IS NOT DEPENDABLE. USE /invertStereo/ INSTEAD
   */
  @Override
  public String reverseChirality(String smiles) {
    smiles = PT.rep(smiles, "@@", "!@");
    smiles = PT.rep(smiles, "@", "@@");
    smiles = PT.rep(smiles, "!@@", "@");
    // note -- @@SP does not exist
//    smiles = PT.rep(smiles, "@@SP", "@SP");
//    smiles = PT.rep(smiles, "@@OH", "@OH");
//    smiles = PT.rep(smiles, "@@TP", "@TP");
    return smiles;
  }

  /**
   * Returns a bitset matching the pattern within a set of Jmol atoms.
   * 
   * @param pattern
   *        SMILES or SMARTS pattern.
   * @param target
   * @param ac
   * @param bsSelected
   * @param flags
   * @return BitSet indicating which atoms match the pattern.
   */

  @Override
  public BS getSubstructureSet(Object pattern, Object target, int ac, BS bsSelected, int flags) throws Exception {
    Node[] atoms = (target instanceof SmilesSearch ? null : (Node[]) target);
    return (BS) matchPriv(pattern, atoms, ac, bsSelected, null, true,
        flags | (pattern instanceof String ? SmilesParser.getFlags(pattern.toString()) : 0), MODE_BITSET, (atoms == null ? (SmilesSearch) target : null));
  }

  /**
   * called by ForceFieldMMFF.setAtomTypes only
   * 
   */
  @Override
  public void getMMFF94AtomTypes(String[] smarts, Node[] atoms, int ac,
                                 BS bsSelected, Lst<BS> ret, Lst<BS>[] vRings)
      throws Exception {
    clearExceptions();
    SmilesParser sp = new SmilesParser(true, true); // target setting just turns off stereochemistry check
    SmilesSearch search = null;
    int flags = (JC.SMILES_TYPE_SMARTS | JC.SMILES_AROMATIC_MMFF94);
    search = sp.parse("");
    search.exitFirstMatch = false;
    search.target.setAtoms(atoms, Math.abs(ac), bsSelected);
    search.flags = flags;
    search.getRingData(vRings, true, true);
    search.asVector = false;
    search.subSearches = new SmilesSearch[1];
    search.getSelections();
    BS bsDone = new BS();

    for (int i = 0; i < smarts.length; i++) {
      if (smarts[i] == null || smarts[i].length() == 0
          || smarts[i].startsWith("#")) {
        ret.addLast(null);
        continue;
      }
      search.clear();
      search.subSearches[0] = sp.getSubsearch(search,
          SmilesParser.cleanPattern(smarts[i]), flags);
      BS bs = BSUtil.copy((BS) search.search());
      ret.addLast(bs);
      bsDone.or(bs);
      if (bsDone.cardinality() == ac)
        return;
    }
  }

  /**
   * Returns a vector of bitsets indicating which atoms match the pattern.
   * 
   * @param pattern
   *        SMILES or SMARTS pattern.
   * @param atoms
   * @param ac
   * @param bsSelected
   * @param bsAromatic
   * @return BitSet Array indicating which atoms match the pattern.
   * @throws Exception
   */
  @Override
  public BS[] getSubstructureSetArray(Object pattern, Node[] atoms, int ac,
                                      BS bsSelected, BS bsAromatic, int flags)
      throws Exception {
    return (BS[]) matchPriv(pattern, atoms, ac, bsSelected, bsAromatic, true, 
        flags, MODE_ARRAY, null);
  }
  
  /**
   * called by SmilesParser to get nn in ^nn- base on match to actual structure
   * @param pattern
   * @param atoms
   * @param ac
   * @param bsSelected
   * @param bsAromatic
   * @param flags
   * @return string of nn,nn,nn,nn
   * @throws Exception
   */
  public String getAtropisomerKeys(String pattern, Node[] atoms, int ac,
                                      BS bsSelected, BS bsAromatic, int flags)
      throws Exception {
    return (String) matchPriv(pattern, atoms, ac, bsSelected, bsAromatic, false, 
        flags, MODE_ATROP, null);
  }

  /**
   * Generate a topological SMILES string from a set of faces
   * 
   * @param faces
   * @param atomCount
   * 
   * @return topological SMILES string
   * @throws Exception
   */
  @Override
  public String polyhedronToSmiles(Node center, int[][] faces, int atomCount,
                                   P3d[] points, int flags, String details)
      throws Exception {
    SmilesAtom[] atoms = new SmilesAtom[atomCount];
    for (int i = 0; i < atomCount; i++) {
      atoms[i] = new SmilesAtom();
      P3d pt = (points == null ? null : points[i]);
      if (pt instanceof Node) {
        atoms[i].elementNumber = ((Node) pt).getElementNumber();
        atoms[i].bioAtomName = ((Node) pt).getAtomName();
        atoms[i].atomNumber = ((Node) pt).getAtomNumber();
        atoms[i].setT(pt);
      } else {
        atoms[i].elementNumber = (pt instanceof Point3fi ? ((Point3fi) pt).sD
            : -2);
        if (pt != null)
          atoms[i].setT(pt);
      }
      atoms[i].index = i;
    }
    int nBonds = 0;
    for (int i = faces.length; --i >= 0;) {
      int[] face = faces[i];
      int n = face.length;
      int iatom, iatom2;
      for (int j = n; --j >= 0;) {
        if ((iatom = face[j]) >= atomCount
            || (iatom2 = face[(j + 1) % n]) >= atomCount)
          continue;
        if (atoms[iatom].getBondTo(atoms[iatom2]) == null) {
          SmilesBond b = new SmilesBond(atoms[iatom], atoms[iatom2],
              Edge.BOND_COVALENT_SINGLE, false);
          b.index = nBonds++;
        }
      }
    }
    for (int i = 0; i < atomCount; i++) {
      int n = atoms[i].bondCount;
      if (n == 0 || n != atoms[i].bonds.length)
        atoms[i].bonds = (SmilesBond[]) AU.arrayCopyObject(atoms[i].bonds, n);
    }
    String s = null;
    SmilesGenerator g = new SmilesGenerator();
    if (points != null)
      g.polySmilesCenter = (P3d) center;
    clearExceptions();
    s = g.getSmiles(this, atoms, atomCount, BSUtil.newBitSet2(0, atomCount),
        null, flags | JC.SMILES_GEN_EXPLICIT_H_ALL | JC.SMILES_NO_AROMATIC
            | JC.SMILES_IGNORE_STEREOCHEMISTRY);
    if ((flags & JC.SMILES_GEN_POLYHEDRAL) == JC.SMILES_GEN_POLYHEDRAL) {
      s = ((flags & JC.SMILES_GEN_ATOM_COMMENT) == 0 ? "" : "//* " + center + " *//\t")
          + "["
          + Elements.elementSymbolFromNumber(center.getElementNumber()) + "@PH"
          + atomCount + (details == null ? "" : "/" + details + "/") + "]." + s;
    }

    return s;
  }


  /**
   * Rather than returning bitsets, this method returns the sets of matching
   * atoms in array form so that a direct atom-atom correlation can be made.
   * 
   * @param pattern
   *        SMILES or SMARTS pattern.
   * @param atoms
   * @param bsSelected
   * @return a set of atom correlations
   * 
   */
  @Override
  public int[][] getCorrelationMaps(Object pattern, Node[] atoms, int atomCount,
                                    BS bsSelected, int flags) throws Exception {
    return (int[][]) matchPriv(pattern, atoms, atomCount, bsSelected, null, true,
        flags, MODE_MAP, null);
  }

  private Object matchPriv(Object pattern, Node[] atoms, int ac, BS bsSelected,
                           BS bsAromatic, boolean doTestAromatic, int flags,
                           int mode, SmilesSearch searchTarget)
      throws Exception {
    clearExceptions();
    try {
      boolean isCompiled = (pattern instanceof SmilesSearch);
      if (isCompiled) {
        flags |= JC.SMILES_TYPE_SMARTS;
        ((SmilesSearch) pattern).reset();
      }
      boolean isSmarts = ((flags
          & JC.SMILES_TYPE_SMARTS) == JC.SMILES_TYPE_SMARTS);
      // Note that additional flags are set when the pattern is parsed.
      SmilesSearch searchPattern = (isCompiled ? (SmilesSearch) pattern : SmilesParser.newSearch(pattern == null ? null : pattern.toString(), isSmarts, false));
      if (searchTarget != null)
        searchTarget.setFlags(flags | searchTarget.flags | (isCompiled ? 0 : SmilesParser.getFlags(pattern.toString())));
      return matchPattern(searchPattern, atoms, ac, bsSelected, bsAromatic, doTestAromatic, flags, mode, searchTarget);
    } catch (Exception e) {
      if (Logger.debugging)
        e.printStackTrace();
      if (InvalidSmilesException.getLastError() == null)
        clearExceptions();
      throw new InvalidSmilesException(InvalidSmilesException.getLastError());
    }
  }

  private Object matchPattern(SmilesSearch search, Node[] atoms, int ac,
                              BS bsSelected, BS bsAromatic,
                              boolean doTestAromatic, int flags, int mode,
                              SmilesSearch searchTarget)
      throws InvalidSmilesException {
    boolean isSmarts = ((flags
        & JC.SMILES_TYPE_SMARTS) == JC.SMILES_TYPE_SMARTS);
    //    boolean isTopo = ((flags
    //        & JC.SMILES_GEN_TOPOLOGY) == JC.SMILES_GEN_TOPOLOGY);
    //
    okMF = true;
    if (searchTarget != null) {
      if (searchTarget.targetSet) {
        search.setTarget(searchTarget);
      } else {
        search.haveSmilesTarget = true;
        bsAromatic = new BS();
        searchTarget.createTopoMap(bsAromatic);
        atoms = searchTarget.target.nodes;
        ac = searchTarget.target.nodes.length;
        if (isSmarts) {
          int[] a1 = searchTarget.elementCounts;
          int[] a2 = search.elementCounts;
          // skip 0 here -- it is wild cards a, [...], *
          int n = search.elementNumberMax;
          if (n <= searchTarget.elementNumberMax) {
            // includes H
            for (int i = 1; i <= n; i++) {
              if (a1[i] < a2[i]) {
                okMF = false;
                break;
              }
            }
          } else {
            okMF = false;
          }
        } else {
          int[] mf = search.getMFArray(true, null, false);
          int[] mft = searchTarget.getMFArray(true, null, false);
          int n = searchTarget.elementNumberMax;
          if (n == search.elementNumberMax) {
            // does NOT include H
            for (int i = 2; i <= n; i++) {
              if (mf[i] != mft[i]) {
                okMF = false;
                break;
              }
            }
          } else {
            okMF = false;
          }
        }
      }
    }
    if (okMF) {
      if (!isSmarts && !search.patternAromatic) {
        if (bsAromatic == null)
          bsAromatic = new BS();
        search.normalizeAromaticity(bsAromatic);
        search.isNormalized = true;
      }
      if (!search.targetSet)
        search.target.setAtoms(atoms, ac, bsSelected);
      if (search.targetSet || ac != 0 && (bsSelected == null || !bsSelected.isEmpty())) {
        boolean is3D = search.targetSet || !(atoms[0] instanceof SmilesAtom);
        search.getSelections();
        if (!doTestAromatic)
          search.target.bsAromatic = bsAromatic;
        if (!search.target.hasRingData(flags))
          search.setRingData(null, null,
              is3D || doTestAromatic || search.patternAromatic);
        search.exitFirstMatch = ((flags
            & JC.SMILES_FIRST_MATCH_ONLY) == JC.SMILES_FIRST_MATCH_ONLY);
        search.mapUnique = ((flags
            & JC.SMILES_MAP_UNIQUE) == JC.SMILES_MAP_UNIQUE);
      }
    }
    switch (mode) {
    case MODE_BITSET:
      search.asVector = false;
      return (okMF ? search.search() : new BS());
    case MODE_ARRAY:
      if (!okMF)
        return new BS[0];//Lst<BS>();
      search.asVector = true;
      @SuppressWarnings("unchecked") Lst<BS> vb = (Lst<BS>) search.search();
      return vb.toArray(new BS[vb.size()]);
    case MODE_ATROP:
      if (!okMF)
        return "";
      search.exitFirstMatch = true;
      search.setAtropicity = true;
      search.search();
      return search.atropKeys;
    case MODE_MAP:
      if (!okMF)
        return new int[0][0];
      search.getMaps = true;
      search.setFlags(flags | search.flags); // important for COMPARE command - no stereochem
      @SuppressWarnings("unchecked")
      Lst<int[]> vl = (Lst<int[]>) search.search();
      return vl.toArray(AU.newInt2(vl.size()));
    case MODE_BOOLEAN:
      if (!okMF)
        return Boolean.FALSE;
      search.retBoolean = true;
      search.setFlags(flags | search.flags); // important for COMPARE command - no stereochem
      return search.search();
    }
    return null;
  }

  @Override
  public String cleanSmiles(String smiles) {
    return SmilesParser.cleanPattern(smiles);
  }

  @Override
  public int[][] getMapForJME(String jme, Atom[] at, BS bsAtoms) {
    try {
      SmilesSearch molecule = jmeToMolecule(jme);
      BS bs = BSUtil.newBitSet2(0, molecule.ac);
      String s = getSmiles(molecule.patternAtoms, molecule.ac, bs, null,
          JC.SMILES_TYPE_SMARTS | JC.SMILES_IGNORE_STEREOCHEMISTRY);
      int[][] map = getCorrelationMaps(s, molecule.patternAtoms, molecule.ac, bs,
          JC.SMILES_TYPE_SMARTS | JC.SMILES_FIRST_MATCH_ONLY
              | JC.SMILES_IGNORE_STEREOCHEMISTRY);
      int[][] map2 = getCorrelationMaps(s, at, bsAtoms.cardinality(), bsAtoms,
          JC.SMILES_TYPE_SMARTS | JC.SMILES_FIRST_MATCH_ONLY
              | JC.SMILES_IGNORE_STEREOCHEMISTRY);
      //System.out.println(s);
      //System.out.println(jme);
      //System.out.println(PT.toJSON(null,  map));
      //System.out.println(PT.toJSON(null,  map2));
      return new int[][] { map[0], map2[0] };
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public static SmilesSearch jmeToMolecule(String jme) throws InvalidSmilesException {
    SmilesSearch molecule = new SmilesSearch();
    String[] tokens = PT.getTokens(jme);
    int nAtoms = PT.parseInt(tokens[0]);
    int nBonds = PT.parseInt(tokens[1]);
    int pt = 2;
    for (int i = 0; i < nAtoms; i++, pt += 3) {
      String sa = tokens[pt];
      SmilesAtom a = molecule.addAtom();
      int ic = sa.indexOf("+");
      int charge = 0;
      if (ic >= 0) {
        charge = (ic == sa.length() - 1 ? 1
            : PT.parseInt(sa.substring(ic + 1)));
      } else if ((ic = sa.indexOf("-")) >= 0) {
        charge = PT.parseInt(sa.substring(ic));
      }
      a.setCharge(charge);
      a.setSymbol(ic < 0 ? sa : sa.substring(0, ic));
    }
    for (int i = 0; i < nBonds; i++) {
      int ia = PT.parseInt(tokens[pt++]) - 1;
      int ib = PT.parseInt(tokens[pt++]) - 1;
      int iorder = PT.parseInt(tokens[pt++]);
      SmilesAtom a1 = molecule.patternAtoms[ia];
      SmilesAtom a2 = molecule.patternAtoms[ib];
      int order = Edge.BOND_COVALENT_SINGLE;
      switch (iorder) {
      default:
      case 1:
        break;
      case 2:
        order = Edge.BOND_COVALENT_DOUBLE;
        break;
      case 3:
        order = Edge.BOND_COVALENT_TRIPLE;
        break;
      }
      new SmilesBond(a1, a2, order, false).index = i;

    }
    molecule.isSmarts = true;
    molecule.set();
    return molecule;
  }

  @Override
  public String getSmilesFromJME(String jme) {
    try {
      SmilesSearch molecule = jmeToMolecule(jme);
      BS bs = BSUtil.newBitSet2(0, molecule.ac);
      return getSmiles(molecule.patternAtoms, molecule.ac, bs, null,
          JC.SMILES_TYPE_SMILES);
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public Object compileSmartsPattern(String pattern) throws Exception {
    pattern = SmilesParser.cleanPattern(pattern);
    return SmilesParser.newSearch(pattern, true, false);
  }

  @Override
  public Object compileSearchTarget(Node[] atoms, int atomCount, BS bsSelected) {
    SmilesSearch ss = new SmilesSearch();
    ss.target.setAtoms(atoms, atomCount, bsSelected);
    ss.targetSet = true;
    return ss;
  }

  /**
   * Look for pattern in each smilesSet string.
   * 
   * @pattern to look for, probably SMARTS
   * @smilesSet array of target strings
   * @flags
   * @return int array of same length as smiles set with 1 = match found, 0 =
   *         not found, and -1 meaning a parsing or searching error.
   * 
   */
  @Override
  public int[] hasStructure(Object pattern, Object[] smilesSet, int flags)
      throws Exception {
    int[] ret = new int[smilesSet.length];
    if ((flags & JC.SMILES_TYPE_SMILES) != JC.SMILES_TYPE_SMILES) {
      // accept SMILES/SMARTS flags default as SMARTS
      flags = flags | JC.SMILES_TYPE_SMARTS;
    }
    clearExceptions();
    if (pattern instanceof String)
      pattern = SmilesParser.cleanPattern((String) pattern);
    try {
      // Note that additional flags are set when the pattern is parsed.
      SmilesSearch search = (pattern instanceof String ? SmilesParser.newSearch((String) pattern, true, false) : (SmilesSearch) pattern);
      for (int i = 0; i < smilesSet.length; i++) {  
        Object smiles = smilesSet[i];
        SmilesSearch searchTarget;
        if (smiles instanceof String) {
          searchTarget = newSearch(smiles); /// smiles chirality is fixed here
          searchTarget
          .setFlags(searchTarget.flags | (pattern instanceof String ? SmilesParser.getFlags((String)pattern) : 0));
        } else {
          searchTarget = (SmilesSearch) smiles;
        }
        try {
          clearExceptions();
          ret[i] = (matchPattern(search, null, 0, null, null, false,
              flags | JC.SMILES_FIRST_MATCH_ONLY, MODE_BOOLEAN,
              searchTarget) == Boolean.TRUE ? 1 : 0);
        } catch (Exception e) {
          ret[i] = -1; // failed
          e.printStackTrace();
        }
      }
    } catch (Exception e) {
      if (Logger.debugging)
        e.printStackTrace();
      if (InvalidSmilesException.getLastError() == null)
        clearExceptions();
      throw new InvalidSmilesException(InvalidSmilesException.getLastError());
    }
    return ret;
  }

  public static void main(String[] args) {
	  
  }

}
