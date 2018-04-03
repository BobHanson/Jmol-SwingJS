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

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.PT;

import org.jmol.api.SmilesMatcherInterface;
import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.util.BSUtil;
import org.jmol.util.Edge;
import org.jmol.util.Elements;
import org.jmol.util.Logger;
import org.jmol.util.Node;
import org.jmol.util.Point3fi;
import org.jmol.viewer.JC;

/**
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

  // internal flags
  
  private final static int MODE_BITSET       = 0x01;
  private final static int MODE_ARRAY        = 0x02;
  private final static int MODE_MAP          = 0x03;
  private static final int MODE_ATROP        = 0x04;


  @Override
  public String getLastException() {
    return InvalidSmilesException.getLastError();
  }

  @Override
  public String getMolecularFormula(String pattern, boolean isSmarts)
      throws Exception {
    InvalidSmilesException.clear();
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
    SmilesSearch search = SmilesParser.newSearch("/nostereo/"+pattern, isSmarts, true);
    search.createTopoMap(null);
    search.nodes = search.targetAtoms;
    return search.getMolecularFormula(!isSmarts, null, false);
  }

  /**
   * internal to Jmol -- called by org.jmol.Viewer.getSmiles
   */

  @Override
  public String getSmiles(Node[] atoms, int ac, BS bsSelected,
                          String bioComment, int flags) throws Exception {
    InvalidSmilesException.clear();
    return (new SmilesGenerator()).getSmiles(this, atoms, ac, bsSelected, bioComment, flags);
  }

  @Override
  public int areEqual(String smiles1, String smiles2) throws Exception {
    InvalidSmilesException.clear();
    BS[] result = (BS[]) findPriv(smiles1, SmilesParser.newSearch(smiles2,
        false, true), (smiles1.indexOf("*") >= 0 ? JC.SMILES_TYPE_SMARTS
        : JC.SMILES_TYPE_SMILES) | JC.SMILES_FIRST_MATCH_ONLY, MODE_ARRAY); 
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
    BS[] ret = (BS[]) findPriv(smiles, search, JC.SMILES_TYPE_SMILES
        | JC.SMILES_FIRST_MATCH_ONLY, MODE_ARRAY);
    return (ret != null && ret.length == 1);
  }

  /**
   * 
   * Searches for all matches of a pattern within a SMILES string. If SMILES
   * (not isSmarts), requires that all atoms be part of the match.
   * 
   * 
   * @param pattern
   *        SMILES or SMARTS pattern.
   * @param target
   * @param flags
   * @return array of correlations of occurances of pattern within smiles
   * @throws Exception
   */
  @Override
  public int[][] find(String pattern, String target, int flags)
      throws Exception {

    InvalidSmilesException.clear();
    target = SmilesParser.cleanPattern(target);
    pattern = SmilesParser.cleanPattern(pattern);
    // search flags will be set in findPriv
    SmilesSearch search = SmilesParser.newSearch(target, false, true); /// smiles chirality is fixed here
    int[][] array = (int[][]) findPriv(pattern, search, flags, MODE_MAP);
    for (int i = array.length; --i >= 0;) {
      int[] a = array[i];
      for (int j = a.length; --j >= 0;)
        a[j] = ((SmilesAtom) search.targetAtoms[a[j]]).mapIndex;
    }
    return array;
  }

  @Override
  public Node[] getAtoms(String target)
      throws Exception {
    InvalidSmilesException.clear();
    target = SmilesParser.cleanPattern(target);
    SmilesSearch search = SmilesParser.newSearch(target, false, true);
    search.createTopoMap(new BS());
    return search.targetAtoms;
  }


  @Override
  public String getRelationship(String smiles1, String smiles2)
      throws Exception {
    if (smiles1 == null || smiles2 == null || smiles1.length() == 0
        || smiles2.length() == 0)
      return "";
    String mf1 = getMolecularFormula(smiles1, false);
    String mf2 = getMolecularFormula(smiles2, false);
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
   * @param atoms
   * @param ac
   * @param bsSelected
   * @return BitSet indicating which atoms match the pattern.
   */

  @Override
  public BS getSubstructureSet(String pattern, Node[] atoms, int ac, BS bsSelected, int flags) throws Exception {
    return (BS) matchPriv(pattern, atoms, ac, bsSelected, null, true,
        flags | SmilesParser.getFlags(pattern), MODE_BITSET);
  }

  /**
   * called by ForceFieldMMFF.setAtomTypes only
   * 
   */
  @Override
  public void getMMFF94AtomTypes(String[] smarts, Node[] atoms, int ac,
                                 BS bsSelected, Lst<BS> ret, Lst<BS>[] vRings)
      throws Exception {
    InvalidSmilesException.clear();
    SmilesParser sp = new SmilesParser(true, true); // target setting just turns off stereochemistry check
    SmilesSearch search = null;
    int flags = (JC.SMILES_TYPE_SMARTS | JC.SMILES_AROMATIC_MMFF94);
    search = sp.parse("");
    search.exitFirstMatch = false;
    search.targetAtoms = atoms;
    search.targetAtomCount = Math.abs(ac);
    search.setSelected(bsSelected);
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
  public BS[] getSubstructureSetArray(String pattern, Node[] atoms, int ac,
                                      BS bsSelected, BS bsAromatic, int flags)
      throws Exception {
    return (BS[]) matchPriv(pattern, atoms, ac, bsSelected, bsAromatic, true, 
        flags, MODE_ARRAY);
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
        flags, MODE_ATROP);
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
                                   P3[] points, int flags, String details)
      throws Exception {
    SmilesAtom[] atoms = new SmilesAtom[atomCount];
    for (int i = 0; i < atomCount; i++) {
      atoms[i] = new SmilesAtom();
      P3 pt = (points == null ? null : points[i]);
      if (pt instanceof Node) {
        atoms[i].elementNumber = ((Node) pt).getElementNumber();
        atoms[i].bioAtomName = ((Node) pt).getAtomName();
        atoms[i].atomNumber = ((Node) pt).getAtomNumber();
        atoms[i].setT(pt);
      } else {
        atoms[i].elementNumber = (pt instanceof Point3fi ? ((Point3fi) pt).sD
            : -2);
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
      g.polySmilesCenter = (P3) center;
    InvalidSmilesException.clear();
    s = g.getSmiles(this, atoms, atomCount, BSUtil.newBitSet2(0, atomCount),
        null, flags | JC.SMILES_GEN_EXPLICIT_H | JC.SMILES_NO_AROMATIC
            | JC.SMILES_IGNORE_STEREOCHEMISTRY);
    if ((flags & JC.SMILES_GEN_POLYHEDRAL) == JC.SMILES_GEN_POLYHEDRAL) {
      s = "//* " + center + " *//\t["
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
  public int[][] getCorrelationMaps(String pattern, Node[] atoms, int atomCount,
                                    BS bsSelected, int flags) throws Exception {
    return (int[][]) matchPriv(pattern, atoms, atomCount, bsSelected, null, true,
        flags, MODE_MAP);
  }

  /////////////// private methods ////////////////

  private Object findPriv(String pattern, SmilesSearch search, int flags, int mode)
      throws Exception {
    // create a topological model set from smiles
    // do not worry about stereochemistry -- this
    // will be handled by SmilesSearch.setSmilesCoordinates
    BS bsAromatic = new BS();
    search.setFlags(search.flags | SmilesParser.getFlags(pattern));
    search.createTopoMap(bsAromatic);
    return matchPriv(pattern, search.targetAtoms, -search.targetAtoms.length,
        null, bsAromatic, bsAromatic.isEmpty(), flags, mode);
  }

  @SuppressWarnings({ "unchecked" })
  private Object matchPriv(String pattern, Node[] atoms, int ac, BS bsSelected,
                           BS bsAromatic, boolean doTestAromatic, int flags,
                           int mode) throws Exception {
    InvalidSmilesException.clear();
    try {
      boolean isSmarts = ((flags & JC.SMILES_TYPE_SMARTS) == JC.SMILES_TYPE_SMARTS);
      // Note that additional flags are set when the pattern is parsed.
      SmilesSearch search = SmilesParser.newSearch(pattern, isSmarts, false);
      if (!isSmarts && !search.patternAromatic) {
        if (bsAromatic == null)
          bsAromatic = new BS();
        SmilesSearch.normalizeAromaticity(search.patternAtoms, bsAromatic,
            search.flags);
        search.isNormalized = true;
      }
      search.targetAtoms = atoms;
      search.targetAtomCount = Math.abs(ac);
      if (ac < 0)
        search.haveTopo = true;
      if (ac != 0 && (bsSelected == null || !bsSelected.isEmpty())) {
        boolean is3D = !(atoms[0] instanceof SmilesAtom);
        search.setSelected(bsSelected);
        search.getSelections();
        if (!doTestAromatic)
          search.bsAromatic = bsAromatic;
        search.setRingData(null, null, is3D || doTestAromatic);
        search.exitFirstMatch = ((flags & JC.SMILES_FIRST_MATCH_ONLY) == JC.SMILES_FIRST_MATCH_ONLY);
        search.mapUnique = ((flags & JC.SMILES_MAP_UNIQUE) == JC.SMILES_MAP_UNIQUE);
      }
      switch (mode) {
      case MODE_BITSET:
        search.asVector = false;
        return search.search();
      case MODE_ARRAY:
        search.asVector = true;
        Lst<BS> vb = (Lst<BS>) search.search();
        return vb.toArray(new BS[vb.size()]);
      case MODE_ATROP:
        search.exitFirstMatch = true;
        search.setAtropicity = true;
        search.search();
        return search.atropKeys;
      case MODE_MAP:
        search.getMaps = true;
        search.setFlags(flags | search.flags); // important for COMPARE command - no stereochem
        Lst<int[]> vl = (Lst<int[]>) search.search();
        return vl.toArray(AU.newInt2(vl.size()));
      }
    } catch (Exception e) {
      if (Logger.debugging)
        e.printStackTrace();
      if (InvalidSmilesException.getLastError() == null)
        InvalidSmilesException.clear();
      throw new InvalidSmilesException(InvalidSmilesException.getLastError());
    }
    return null;
  }

  @Override
  public String cleanSmiles(String smiles) {
    return SmilesParser.cleanPattern(smiles);
  }

  @Override
  public int[][] getMapForJME(String jme, Atom[] at, BS bsAtoms) {
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
       charge = (ic == sa.length() - 1 ? 1 : PT.parseInt(sa.substring(ic + 1)));
      } else if ((ic = sa.indexOf("-")) >= 0) {
        charge= PT.parseInt(sa.substring(ic));
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

    String s = "";
    try {
      molecule.isSmarts = true;
      molecule.set();
      BS bs = BSUtil.newBitSet2(0, nAtoms);
      s = getSmiles(molecule.patternAtoms, molecule.ac, bs, null, JC.SMILES_TYPE_SMARTS|JC.SMILES_IGNORE_STEREOCHEMISTRY);
      int[][] map = getCorrelationMaps(s, 
          molecule.patternAtoms, nAtoms, bs, JC.SMILES_TYPE_SMARTS |JC.SMILES_FIRST_MATCH_ONLY|JC.SMILES_IGNORE_STEREOCHEMISTRY);
      int[][] map2 = getCorrelationMaps(s, 
          at, bsAtoms.cardinality(), bsAtoms, JC.SMILES_TYPE_SMARTS |JC.SMILES_FIRST_MATCH_ONLY|JC.SMILES_IGNORE_STEREOCHEMISTRY);
//      System.out.println(s);
//      System.out.println(jme);
//      System.out.println(PT.toJSON(null,  map));
//      System.out.println(PT.toJSON(null,  map2));
      return new int[][] {map[0], map2[0]};
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

}
