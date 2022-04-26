/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2015-08-30 01:18:16 -0500 (Sun, 30 Aug 2015) $
 * $Revision: 20742 $
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

import java.util.Arrays;

import org.jmol.util.Edge;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Node;
import org.jmol.util.SimpleEdge;
import org.jmol.util.SimpleNode;

import javajs.util.AU;
import javajs.util.MeasureD;
import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.T3d;
import javajs.util.V3d;

//import org.jmol.util.Logger;

/**
 * This class relates to stereochemical issues
 */
public class SmilesStereo {

  private int chiralClass = Integer.MIN_VALUE;
  int chiralOrder = Integer.MIN_VALUE;
  int atomCount;
  private String details;
  private SmilesSearch search;
  private Node[] jmolAtoms;
//  private String directives;
  final public static int DEFAULT = 0;
  final public static int POLYHEDRAL = 1;           // Jmol polySMILES
  final public static int ALLENE = 2;
  final public static int TRIGONAL_PYRAMIDAL = 3;  // Jmol SMILES
  final public static int TETRAHEDRAL = 4;
  final public static int TRIGONAL_BIPYRAMIDAL = 5;
  final public static int OCTAHEDRAL = 6;
  final public static int SQUARE_PLANAR = 7;
  final public static int T_SHAPED = 8;           // Jmol SMILES
  final public static int SEESAW = 9;             // Jmol SMILES

  private static int getChiralityClass(String xx) {
    return ("0;PH;AL;TP;TH;TB;OH;SP;TS;SS;".indexOf(xx) + 1) / 3;
  }

  public static SmilesStereo newStereo(SmilesSearch search)
      throws InvalidSmilesException {
    SmilesStereo stereo = new SmilesStereo(0, 0, 0, null, null);
    stereo.search = search;
    return stereo;
  }

  SmilesStereo(int chiralClass, int chiralOrder, int atomCount, String details,
      @SuppressWarnings("unused") String directives) throws InvalidSmilesException {
    this.chiralClass = chiralClass;
    this.chiralOrder = chiralOrder;
    this.atomCount = atomCount;
    this.details = details;
//    this.directives = directives;
    if (chiralClass == POLYHEDRAL)
      getPolyhedralOrders();
  }

  public int getChiralClass(SmilesAtom sAtom) {
    if (chiralClass == 0)
      setChiralClass(sAtom);
    return chiralClass;
  }

  private int setChiralClass(SmilesAtom sAtom) {
    int nBonds = Math.max(sAtom.explicitHydrogenCount, 0)
        + sAtom.getBondCount();
    if (chiralClass == DEFAULT) {
      switch (nBonds) {
      case 2:
        chiralClass = ALLENE;
        break;
      case 3:
        chiralClass = TRIGONAL_PYRAMIDAL;
        break;
      case 4:
      case 5:
      case 6:
        chiralClass = nBonds;
        break;
      }
    }
    return nBonds;
  }

  /**
   * Check number of connections and permute them to match a canonical version
   *  
   * 
   * @param sAtom
   * @throws InvalidSmilesException
   */
  void fixStereo(SmilesAtom sAtom) throws InvalidSmilesException {
    // note:   Implicit H in brackets will fail for 
    //         cases involving string.find("SMILES",string)
    //         and seesaw, trigonal bipyramidal, or octahedral, because those cases need normalization
    //         which is a process that reorganizes the bonds in SmilesAtom.bonds[]
    int nBonds = setChiralClass(sAtom);
    int nH = Math.max(sAtom.explicitHydrogenCount, 0);
    if (nH <= 1)
      switch (chiralClass) {
      case ALLENE:
        if (nBonds != 2)
          sAtom.stereo = null;
        break;
      case TRIGONAL_PYRAMIDAL:
      case T_SHAPED:
        if (nBonds != 3)
          sAtom.stereo = null;
        break;
      case SQUARE_PLANAR:
      case TETRAHEDRAL:
        if (nBonds != 4)
          sAtom.stereo = null;
        break;
      case SEESAW:
      case OCTAHEDRAL:
      case TRIGONAL_BIPYRAMIDAL:
        if (nBonds != (chiralClass == SEESAW ? 4 : chiralClass)
            || !normalizeClass(sAtom))
          sAtom.stereo = null;
        break;
      case POLYHEDRAL:
        // we allow no bonds here, indicating that the next N atoms are associated (but not connected)
        // with this atom
        if (nBonds != 0 && nBonds != atomCount)
          sAtom.stereo = null;
        break;
      default:
        sAtom.stereo = null;
      }
    if (sAtom.stereo == null)
      throw new InvalidSmilesException(
          "Incorrect number of bonds for stereochemistry descriptor");
  }

  // assignments from http://www.opensmiles.org/opensmiles.html
  private static final int[] PERM_TB = new int[] {
    // a,p,z, where:
    // a = first axial -- shift to first position
    // p = 1 @, -1 for @@ -- once shifted, this flag sets chirality
    // z = last axial  -- shift to last position
    0, 1,4, //TB1 @
    0,-1,4, //TB2 @@
    0, 1,3, //TB3 @
    0,-1,3, //TB4 @@
    0, 1,2, //TB5 @
    0,-1,2, //TB6 @@
    0, 1,1, //TB7 @
    0,-1,1, //TB8 @@
    1, 1,4, //TB9 @
    1, 1,3, //TB10 @
    1,-1,4, //TB11 @@
    1,-1,3, //TB12 @@
    1, 1,2, //TB13 @
    1,-1,2, //TB14 @@
    2, 1,4, //TB15 @
    2, 1,3, //TB16 @
    3, 1,4, //TB17 @
    3,-1,4, //TB18 @@
    2,-1,3, //TB19 @@
    2,-1,4, //TB20 @@
  };
  
  private static final int[] PERM_OCT = new int[] {
    // a,p,z, where:
    // a = first axial -- shift to first position (0)
    // p = 1 @, -1 @@, or position to permute with p+1 after setting a and z (< 0 for @@)
    // z = last axial  -- shift to last position (5)
    // so:
    // for "U" we have 1/-1 -- standard chirality check
    // for "Z" we permute groups 3 and 4
    // for "4" we permute groups 4 and 1
      0, 1,5, //OH1 a U f @ 
      0,-1,5, //OH2 a U f @@  
      0, 1,4, //OH3 a U e @ 
      0, 3,5, //OH4 a Z f @ 
      0, 3,4, //OH5 a Z e @ 
      0, 1,3, //OH6 a U d @ 
      0, 3,3, //OH7 a Z d @ 
      0, 2,5, //OH8 a 4 f @@  
      0, 2,4, //OH9 a 4 e @@  
      0,-2,5, //OH10 a 4 f @  
      0,-2,4, //OH11 a 4 e @  
      0, 2,3, //OH12 a 4 d @@ 
      0,-2,3, //OH13 a 4 d @  
      0,-3,5, //OH14 a Z f @@ 
      0,-3,4, //OH15 a Z e @@ 
      0,-1,4, //OH16 a U e @@ 
      0,-3,3, //OH17 a Z d @@ 
      0,-1,3, //OH18 a U d @@ 
      0, 1,2, //OH19 a U c @  
      0, 3,2, //OH20 a Z c @  
      0, 2,2, //OH21 a 4 c @@ 
      0,-2,2, //OH22 a 4 c @  
      0,-3,2, //OH23 a Z c @@ 
      0,-1,2, //OH24 a U c @@ 
      0, 1,1, //OH25 a U b @  
      0, 3,1, //OH26 a Z b @  
      0, 2,1, //OH27 a 4 b @@ 
      0,-2,1, //OH28 a 4 b @  
      0,-3,1, //OH29 a Z b @@ 
      0,-1,1, //OH30 a U b @@ 
  };

  // TS - like square planar, we are just looking for axial groups
  //      no @/@@ here. 


  // SS - See-Saw - what we are reading is the rotation of the first three groups after permutation
  //      no @/@@ here because there are four atoms, like tetrahedral
  private static final int[] PERM_SS = new int[] {
   0,  1, 3,  //SS1
   0, -1, 3,  //SS2
   0,  1, 2,  //SS3
   0, -1, 2,  //SS4
   0,  1, 1,  //SS5
   0, -1, 1,  //SS6
   1,  1, 3,  //SS7
   1, -1, 3,  //SS8
   1,  1, 2,  //SS9
   1, -1, 2,  //SS10
   2,  1, 3,  //SS11
   2, -1, 3,  //SS12
  };

  /**
   * re-order bonds to match standard @ and @@ types
   * 
   * @param atom
   * @return true if OK
   */
  private boolean normalizeClass(SmilesAtom atom) {
    try {
      SmilesBond[] bonds = atom.bonds;
      if (chiralOrder < 3)
        return true;
      int pt = (chiralOrder - 1) * 3;
      int[] perm;
      int ilast;
      switch (chiralClass) {
      case SEESAW:
        perm = PERM_SS;
        ilast = 3;
        break;
      case TRIGONAL_BIPYRAMIDAL:
        perm = PERM_TB;
        ilast = 4;
        break;
      case OCTAHEDRAL:
        perm = PERM_OCT;
        ilast = 5;
        break;
      default:
        return true;
      }
      if (chiralOrder > perm.length)
        return false;
      int a = perm[pt]; // shifted to first position
      int z = perm[pt + 2]; // shifted to last position
      int p = Math.abs(perm[pt + 1]); // to be permuted with its next position
      boolean isAtAt = (perm[pt + 1] < 0); // negative indicates NOT
      SmilesBond b;
      if (a != 0) {
        b = bonds[a];
        for (int i = a; i > 0; --i)
          bonds[i] = bonds[i - 1];
        bonds[0] = b;
      }
      if (z != ilast) {
        b = bonds[z];
        for (int i = z; i < ilast; i++)
          bonds[i] = bonds[i + 1];
        bonds[ilast] = b;
      }
      switch (p) {
      case 1:
        break;
      default:
        b = bonds[p + 1];
        bonds[p + 1] = bonds[p];
        bonds[p] = b;
      }
      chiralOrder = (isAtAt ? 2 : 1);
    } catch (Exception e) {
      return false;
    }
    return true;

  }

  /**
   * 
   * @param sAtom0 the first target atom
   * @param pAtom  the pattern atom connected to the target atoms
   * @param sAtom2 allene atom
   * @param cAtoms the target atoms
   * @param isNot
   * @return true if successful
   */
  public boolean setTopoCoordinates(SmilesAtom sAtom0, SmilesAtom pAtom,
                                       SmilesAtom sAtom2, Node[] cAtoms, boolean isNot) {

    // When testing equality of two SMILES strings in terms of stereochemistry,
    // we need to set the atom positions based on the ORIGINAL SMILES order,
    // which, except for the H atom, will be the same as the "matchedAtom"
    // index. By putting them in the correct order for the TARGET, we can 
    // set their coordinates. 

    int chiralOrder = (sAtom0.stereo == null ? 0 : sAtom0.stereo.chiralOrder);
    int chClass = (sAtom0.stereo == null ? POLYHEDRAL : sAtom0.stereo.chiralClass);
 
    // set the chirality center at the origin
    sAtom0.set(0, 0, 0);
    
    int[] map;
    if (jmolAtoms == null) {
      map = new int[] { 0, 1, 2, 3 };
    } else {
      sAtom0 = (SmilesAtom) jmolAtoms[pAtom.getMatchingAtomIndex()];
      sAtom0.set(0, 0, 0);
      SmilesAtom a2 = (SmilesAtom) (chClass == ALLENE ? jmolAtoms[sAtom2.getMatchingAtomIndex()]
          : null);
      map = getMappedTopoAtoms(sAtom0, a2, cAtoms, chiralOrder == 0 ? new int[cAtoms.length] : null);

    // get a map from atoms 
    }
    int pt;
    switch (chClass) {
    case POLYHEDRAL:
      sAtom0.set(0, 0, 0.2d);
      double a = Math.PI * 2 / cAtoms.length;
      for (int i = cAtoms.length; --i >= 0;) {
        cAtoms[map[i]].set((double)(Math.cos(i * a)), (double) Math.sin(i * a), isNot ? 1 : -1);
      }
      break;
    case ALLENE:
    case TETRAHEDRAL:
      if (chiralOrder == 2) {
        pt = map[0];
        map[0] = map[1];
        map[1] = pt;
      }
      cAtoms[map[0]].set(0, 0, 1);
      cAtoms[map[1]].set(1, 0, -1);
      cAtoms[map[2]].set(0, 1, -1);
      cAtoms[map[3]].set(-1, -1, -1);
      break;
    case SQUARE_PLANAR:
      switch (chiralOrder) {
      case 1: // U-shaped
        cAtoms[map[0]].set(1, 0, 0);
        cAtoms[map[1]].set(0, 1, 0);
        cAtoms[map[2]].set(-1, 0, 0);
        cAtoms[map[3]].set(0, -1, 0);
        break;
      case 2: // 4-shaped -- swap 2nd and 3rd
        cAtoms[map[0]].set(1, 0, 0);
        cAtoms[map[1]].set(-1, 0, 0);
        cAtoms[map[2]].set(0, 1, 0);
        cAtoms[map[3]].set(0, -1, 0);
        break;
      case 3: // Z-shaped -- swap 3rd and 4th
        cAtoms[map[0]].set(1, 0, 0);
        cAtoms[map[1]].set(0, 1, 0);
        cAtoms[map[2]].set(0, -1, 0);
        cAtoms[map[3]].set(-1, 0, 0);
        break;
      }
      break;
    case T_SHAPED:
      switch (chiralOrder) {
      case 1:
        break;
      case 2:
        pt = map[2];
        map[2] = map[1];
        map[1] = pt;
        break;
      case 3:
        pt = map[0];
        map[0] = map[1];
        map[1] = pt;
        break;
      }
      cAtoms[map[0]].set(0, 0, -1);
      cAtoms[map[1]].set(0, 1, 0);
      cAtoms[map[2]].set(0, 0, 1);
      break;
    case SEESAW:
      if (chiralOrder == 2) {
        pt = map[0];
        map[0] = map[3];
        map[3] = pt;
      }
      cAtoms[map[0]].set(0, 0, 1);
      cAtoms[map[1]].set(0, 1, 0);
      cAtoms[map[1]].set(1, 1, 0);
      cAtoms[map[2]].set(0, 0, -1);
      break;
    case TRIGONAL_BIPYRAMIDAL:
    case OCTAHEDRAL:
      int n = map.length;
      if (chiralOrder == 2) {
        pt = map[0];
        map[0] = map[n - 1];
        map[n - 1] = pt;
      }
      cAtoms[map[0]].set(0, 0, 1);
      cAtoms[map[n - 1]].set(0, 0, -1);
      cAtoms[map[1]].set(1, 0, 0);
      cAtoms[map[2]].set(0, 1, 0);
      cAtoms[map[3]].set(-1, 0, 0);
      if (n == 6)
        cAtoms[map[4]].set(0, -1, 0);
      break;
    default:
      return false;
    }
    return true;
  }

  private int[] getMappedTopoAtoms(SmilesAtom atom, SmilesAtom a2, Node[] cAtoms, int[] map) {
    // Here is the secret:
    // Sort the atoms by the original order of bonds
    // in the SMILES string that generated the atom set.
    // We do the adjustment here for implicit atoms that matter that
    // are allenic carbons, so those carbons themselves are not chiral, per se.
    if (map == null)
      map = new int[cAtoms[4] == null ? 4 : cAtoms[5] == null ? 5 : 6];
    // initially set to index 
    for (int i = 0; i < map.length; i++) {
      //System.out.println(cAtoms[i]);
      map[i] = (cAtoms[i] == null ? 10004 + i * 10000 : cAtoms[i].getIndex());
    }
    //System.out.println(PT.toJSON(null, map));
    SmilesBond[] bonds = atom.bonds;
    SmilesBond[] b2 = (SmilesBond[]) (a2 == null ? null : a2.getEdges());
    for (int i = 0; i < map.length; i++) {
      SmilesAtom c = (SmilesAtom) cAtoms[i];
      if (!getTopoMapPt(map, i, atom, c, bonds, 10000))
        getTopoMapPt(map, i, a2, c, b2, 30000);
    }
    Arrays.sort(map);
    //System.out.println(PT.toJSON(null, map));
    for (int i = 0; i < map.length; i++) {
      map[i] = map[i] % 10;
    }
    //System.out.println(PT.toJSON(null, map));
    return map;
  }

  /**
   * 
   * @param map
   * @param i
   * @param atom
   * @param cAtom
   * @param bonds
   * @param n000  
   * @return true if found
   */
  private static boolean getTopoMapPt(int[] map, int i, SmilesAtom atom, SmilesAtom cAtom,
                                  SmilesBond[] bonds, int n000) {
    if (cAtom.index == Integer.MIN_VALUE) {
      map[i] = (bonds[0].isFromPreviousTo(atom) ? 100 : 0) + n000 + i; // "lone pair on imine"
      return true;
    } 
    int n = bonds.length;
    for (int k = 0; k < n; k++) {
      SmilesAtom bAtom = (SmilesAtom) bonds[k].getOtherNode(atom);
      if (bAtom == cAtom) {
        map[i] = (k + 1) * 10 + n000 + i;
        return true;
      }
    }
    return false;
  }

  private Node getJmolAtom(int i) {
    return (i < 0 || i >= jmolAtoms.length ? null : jmolAtoms[i]);
  }

  /**
   * Sort bond array as ccw rotation around the axis connecting the atom and the
   * reference point (polyhedron center) as seen from outside the polyhedron
   * looking in.
   * 
   * Since we are allowing no branching, all atoms will appear as separate
   * components with only numbers after them. These numbers will be processed
   * and listed in this order.
   * 
   * @param atom
   * @param ref
   * @param center
   * @param bonds
   * @param vTemp 
   */
  void sortPolyBondsByStereo(SimpleNode atom, SimpleNode ref, T3d center, SimpleEdge[] bonds,
                          V3d vTemp) {
    if (bonds.length < 2 || !(atom instanceof T3d))
      return;
    boolean checkAlign = (ref != null);
    //if (ref == null) // some early idea?
      ref = bonds[0].getOtherNode(atom);
    Object[][] aTemp = new Object[bonds.length][0];
    if (sorter == null)
      sorter = new PolyhedronStereoSorter();
    vTemp.sub2((T3d)ref, center);
    sorter.setRef(vTemp);
    int nb = bonds.length;
    double f0 = 0;//(nb > 2 ? 360 : 0);
    for (int i = nb; --i >= 0;) {
      SimpleNode a = bonds[i].getOtherNode(atom);
      double f = f0 + (a == ref ? 0 : 
        checkAlign && sorter.isAligned((T3d) a, center, (T3d) ref) ? -999 : 
          MeasureD.computeTorsion((T3d) ref, (T3d) atom, center, (T3d) a, true));
      aTemp[i] = new Object[] { bonds[i], Double.valueOf(f), a };
    }
    Arrays.sort(aTemp, sorter);
    if (Logger.debugging)
      Logger.info(Escape.e(aTemp));
    for (int i = bonds.length; --i >= 0;)
      bonds[i] = (Edge) aTemp[i][0];
  }

  VTemp v;
  
  boolean checkStereoChemistry(SmilesSearch search, VTemp v) {
    this.v = v;
    this.search = search;
    jmolAtoms = search.targetAtoms;
    boolean haveTopo = search.haveTopo;
    boolean invertStereochemistry = search.invertStereochemistry;

    if (Logger.debugging)
      Logger.debug("checking stereochemistry...");

    //for debugging, first try SET DEBUG
//System.out.println("");
//for (int i = 0; i < search.ac; i++) {
//  SmilesAtom sAtom = search.patternAtoms[i];
//  Node atom0 = sAtom.getMatchingAtom();
//  System.out.print(atom0.getIndex() + (sAtom.stereo != null ? "@" : "") + "-");
//}
//System.out.println("");

    for (int i = 0; i < search.ac; i++) {
      SmilesAtom pAtom = search.patternAtoms[i];
      if (pAtom.stereo == null && search.polyhedronStereo == null)
        continue;
      boolean isNot = (pAtom.not != invertStereochemistry);
      switch(checkStereoForAtom(pAtom, isNot, haveTopo)) {
      case 0:
        continue;
      case 1:
        return true;
      case -1:
        return false;
      }
    }
    return true;
  }

  public int checkStereoForAtom(SmilesAtom pAtom, boolean isNot, boolean haveTopo) {
    Node atom1 = null, atom2 = null, atom3 = null, atom4 = null, atom5 = null, atom6 = null;
    SmilesAtom sAtom0 = null;
    Node[] jn;

    Node atom0 = pAtom.getMatchingAtom();
    if (haveTopo)
      sAtom0 = (SmilesAtom) atom0;
    int nH = Math.max(pAtom.explicitHydrogenCount, 0);
    int order = (pAtom.stereo == null ? 0 :  pAtom.stereo.chiralOrder);
    int chiralClass = (pAtom.stereo == null ? POLYHEDRAL : pAtom.stereo.chiralClass);
    // SMILES string must match pattern for chiral class.
    // but we could do something about changing those if desired.
    if (haveTopo && chiralOrder != 0 && sAtom0.getChiralClass() != chiralClass)
      return -1;
    if (Logger.debugging)
      Logger.debug("...type " + chiralClass + " for pattern atom \n " + pAtom
          + "\n " + atom0);
    switch (chiralClass) {
    case POLYHEDRAL:
      if (pAtom.bondCount == 0) {
        search.polyhedronStereo = pAtom.stereo;
        return 0;
      } 
      if (chiralOrder == 0) {
        Node[] atoms12N = new Node[pAtom.bondCount];
        for (int i = 0; i < atoms12N.length; i++)
          atoms12N[i] = getJmolAtom(pAtom.getMatchingBondedAtom(i));
        return (haveTopo
            && !setTopoCoordinates(sAtom0, pAtom, null, atoms12N, search.polyhedronStereo.isNot ? !isNot : isNot)
            || !checkPolyHedralWinding(pAtom.getMatchingAtom(), atoms12N) ? -1 : 0);
      }
      if (nH > 1)
        return 0; // no chirality for [CH2@]; skip if just an indicator
      if (pAtom.stereo.isNot)
        isNot = !isNot;
      if (haveTopo) {
        // TODO
        return 0;
      }
      SmilesBond[] bonds = pAtom.bonds;
      int jHpt = -1;
      if (nH == 1) {
        jHpt = (pAtom.isFirst ? 0 : 1);
        // can't process this unless it is tetrahedral or perhaps square planar
        if (pAtom.getBondCount() != 3)
          return -1;
        v.vA.set(0, 0, 0);
        for (int j = 0; j < 3; j++)
          v.vA.add((T3d) bonds[j].getOtherAtom(sAtom0).getMatchingAtom());
        v.vA.scale(0.3333f);
        v.vA.sub2((T3d) atom0, v.vA);
        v.vA.add((T3d) atom0);
      }
      int[][] po = pAtom.stereo.polyhedralOrders;
      int pt;
      for (int j = po.length; --j >= 0;) {
        int[] orders = po[j];
        if (orders == null || orders.length < 2)
          continue;
        // the atom we are looking down
        pt = (j > jHpt ? j - nH : j);
        T3d ta1 = (j == jHpt ? v.vA : (T3d) bonds[pt].getOtherAtom(pAtom)
            .getMatchingAtom());
        double flast = (isNot ? Double.MAX_VALUE : 0);
        T3d ta2 = null;
        for (int k = 0; k < orders.length; k++) {
          pt = orders[k];
          T3d ta3;
          if (pt == jHpt) { // attached H
            ta3 = v.vA;
          } else {
            if (pt > jHpt)
              pt--;
            ta3 = (T3d) bonds[pt].getOtherAtom(pAtom).getMatchingAtom();
          }
          if (k == 0) {
            ta2 = ta3;
            continue;
          }
          double f = MeasureD.computeTorsion(ta3, ta1, (T3d) atom0, ta2, true);
          if (Double.isNaN(f))
            f = 180; // directly across the center from the previous atom
          if (orders.length == 2)
            return ((f < 0) != isNot ? 1 : -1); // SHOULD BE 0 : -1???
          if (f < 0)
            f += 360;
          if ((f < flast) != isNot)
            return -1;
          flast = f;
        }
      }
      return 0;
    case ALLENE:
      jn = getAlleneAtoms(haveTopo, sAtom0, pAtom, null);
      if (jn == null)
        return 0;
      if (jn.length == 0)
        return -1;
      if (!checkStereochemistryAll(isNot, atom0,
          chiralClass, order, jn[0], jn[1], jn[2], jn[3], null, null, v))
        return -1;
      return 0;
    case T_SHAPED:
    case SEESAW:
    case TRIGONAL_PYRAMIDAL:
    case TETRAHEDRAL:
    case SQUARE_PLANAR:
    case TRIGONAL_BIPYRAMIDAL:
    case OCTAHEDRAL:
      atom1 = getJmolAtom(pAtom.getMatchingBondedAtom(0));
      switch (nH) {
      case 0:
        atom2 = getJmolAtom(pAtom.getMatchingBondedAtom(1));
        break;
      case 1:
        // have to correct for implicit H atom in  [@XXnH]
        atom2 = search.findImplicitHydrogen(pAtom.getMatchingAtom());
        if (pAtom.isFirst) {
          Node a = atom2;
          atom2 = atom1;
          atom1 = a;
        }
        break;
      default:
        return 0;
      }
      atom3 = getJmolAtom(pAtom.getMatchingBondedAtom(2 - nH));
      atom4 = getJmolAtom(pAtom.getMatchingBondedAtom(3 - nH));
      atom5 = getJmolAtom(pAtom.getMatchingBondedAtom(4 - nH));
      atom6 = getJmolAtom(pAtom.getMatchingBondedAtom(5 - nH));

      // in all the checks below, we use Measure utilities to 
      // three given atoms -- the normal, in particular. We 
      // then use dot products to check the directions of normals
      // to see if the rotation is in the direction required. 

      // we only use TP1, TP2, OH1, OH2 here.
      // so we must also check that the two bookend atoms are axial

      if (haveTopo
          && !setTopoCoordinates(sAtom0, pAtom, null, new Node[] { atom1,
              atom2, atom3, atom4, atom5, atom6 }, false))
        return -1;
      if (!checkStereochemistryAll(isNot, atom0, chiralClass, order, atom1,
          atom2, atom3, atom4, atom5, atom6, v))
        return -1;
      return 0;
    }
    return 0;
  }

  private boolean checkPolyHedralWinding(Node a0, Node[] a) {
    for (int i = 0; i < a.length - 2; i++)
      if (getHandedness(a[i], a[i + 1], a[i + 2], a0, v) != 1)
        return false;
    return true;
  }

  /**
   * 
   * @param haveTopo 
   * @param sAtom0 
   * @param pAtom
   * @param pAtom1
   * @return allene atoms
   */
  public Node[] getAlleneAtoms(boolean haveTopo, SmilesAtom sAtom0, SmilesAtom pAtom, SmilesAtom pAtom1) {
    if (pAtom1 == null)
      pAtom1 = pAtom.getBond(0).getOtherAtom(pAtom);
    SmilesAtom pAtom2 = pAtom.getBond(1).getOtherAtom(pAtom);
    if (pAtom2 == pAtom1)
      pAtom2 = pAtom.getBond(0).getOtherAtom(pAtom);
    if (pAtom1 == null || pAtom2 == null)
      return null; // "OK - stereochemistry is desgnated for something like C=C=O
    // cumulenes
    SmilesAtom pAtom1a = pAtom;
    SmilesAtom pAtom2a = pAtom;
    while (pAtom1.getBondCount() == 2 && pAtom2.getBondCount() == 2
        && pAtom1.getValence() == 4 && pAtom2.getValence() == 4) {
      SmilesBond b = pAtom1.getBondNotTo(pAtom1a, true);
      pAtom1a = pAtom1;
      pAtom1 = b.getOtherAtom(pAtom1);
      b = pAtom2.getBondNotTo(pAtom2a, true);
      pAtom2a = pAtom2;
      pAtom2 = b.getOtherAtom(pAtom2);
    }
    pAtom = pAtom1;
    Node[] jn = new Node[6];
    jn[4] = new SmilesAtom().setIndex(60004);
    int nBonds = pAtom.getBondCount();
    if (nBonds != 2 && nBonds != 3)
      return null; // [C@]=O always matches

    // first take care of all explicit atoms

    for (int k = 0, p = 0; k < nBonds; k++) {
      SmilesBond b = pAtom.bonds[k];
      pAtom1 = b.getOtherAtom(pAtom);
      if (b.getMatchingBond().getCovalentOrder() == 2) {
        if (pAtom2 == null)
          pAtom2 = pAtom1;
        continue;
      }
      if ((b.atom1 == pAtom1) && (!b.isConnection || pAtom1.index > pAtom.index)) {
        p = 0;
      } else if (jn[1] == null) {
        p = 1;
      } else {
        jn[0] = jn[p = 1];
      }
      jn[p] = pAtom1.getMatchingAtom();
    }
    if (pAtom2 == null)
      return null;
    nBonds = pAtom2.getBondCount();
    if (nBonds != 2 && nBonds != 3)
      return null; // [C@]=O always matches
    for (int p = 0, k = 0; k < nBonds; k++) {
      SmilesBond b = pAtom2.bonds[k];
      pAtom1 = b.getOtherAtom(pAtom2);
      if (b.getMatchingBond().getCovalentOrder() == 2) {
        continue;
      }
      if ((b.atom1 == pAtom1) && (!b.isConnection || pAtom1.index > pAtom2.index)) {
        p = 2;
      } else if (jn[3] == null) {
        p = 3;
      } else {
        jn[2] = jn[p = 3];
      }
      jn[p] = pAtom1.getMatchingAtom();
    }

    // now fill in the missing pieces

    for (int k = 0; k < 4; k++)
      if (jn[k] == null)
        addAlleneLonePair(k < 2 ? pAtom : pAtom2, jn, k);
    
    if (haveTopo && !setTopoCoordinates(sAtom0, pAtom, pAtom2, jn, false))
      return new Node[0]; 
    return jn;
  }

  /**
   * for allenes, we must check for missing atoms
   * 
   * @param pAtom
   * @param jn
   * @param k
   */
  private void addAlleneLonePair(SmilesAtom pAtom, Node[] jn, int k) {
    
    Node atom = pAtom.getMatchingAtom();
    jn[k] = search.findImplicitHydrogen(atom);
    if (jn[k] != null)
      return;
   
    // NOT just for topological map; atom might be a modelset.Atom

    // add a dummy point for stereochemical reference
    // imines and diazines only
    V3d v = new V3d();
    for (int i = 0; i < 4; i++)
      if (jn[i] != null)
        v.sub((P3d) jn[i]);
    if (v.length() == 0) {
      v.setT(((P3d) jn[4]));
    } else {
      // d = x - ((a-x)+(b-x)+(c-x))/3
      //   = 2x - a - b - c
      //   = 2x + v
      v.scaleAdd2(2, (P3d) pAtom.getMatchingAtom(), v);
    }
    jn[k] = new SmilesAtom().setIndex(Integer.MIN_VALUE);
    ((P3d) jn[k]).setT(v);
  }

  /**
   * 
   * @param atom0
   * @param atoms
   * @param nAtoms
   * @param v
   * @param is2D 
   * @return String
   */
  static String getStereoFlag(SimpleNode atom0, SimpleNode[] atoms, int nAtoms, VTemp v, boolean is2D) {
    SimpleNode atom1 = atoms[0];
    SimpleNode atom2 = atoms[1];
    SimpleNode atom3 = atoms[2];
    SimpleNode atom4 = atoms[3];
    SimpleNode atom5 = atoms[4];
    SimpleNode atom6 = atoms[5];
    int chiralClass = TETRAHEDRAL;
    // what about POLYHEDRAL?
    switch (nAtoms) {
    default:
    case 5:
    case 6:
      // like tetrahedral
      return (checkStereochemistryAll(false, atom0, chiralClass, 1, atom1,
          atom2, atom3, atom4, atom5, atom6, v) ? "@" : "@@");
    case 2: // allene
    case 4: // tetrahedral, square planar
      if (atom3 == null || atom4 == null)
        return "";
      double d = MeasureD.getNormalThroughPoints((P3d) atom1, (P3d) atom2, (P3d) atom3,
          v.vTemp, v.vA);
      if (Math.abs(MeasureD.distanceToPlaneV(v.vTemp, d, (P3d) atom4)) < 0.2d) {
        if (is2D)
          return "";
        chiralClass = SQUARE_PLANAR;
        if (checkStereochemistryAll(false, atom0, chiralClass, 1, atom1, atom2,
            atom3, atom4, atom5, atom6, v))
          return "@SP1";
        if (checkStereochemistryAll(false, atom0, chiralClass, 2, atom1, atom2,
            atom3, atom4, atom5, atom6, v))
          return "@SP2";
        if (checkStereochemistryAll(false, atom0, chiralClass, 3, atom1, atom2,
            atom3, atom4, atom5, atom6, v))
          return "@SP3";
      } else {
        return (checkStereochemistryAll(false, atom0, chiralClass, 1, atom1,
            atom2, atom3, atom4, atom5, atom6, v) ? "@" : "@@");
      }
    }
    return "";
  }

  private static boolean checkStereochemistryAll(boolean isNot, SimpleNode atom0,
                                         int chiralClass, int order,
                                         SimpleNode atom1, SimpleNode atom2, SimpleNode atom3,
                                         SimpleNode atom4, SimpleNode atom5, SimpleNode atom6,
                                         VTemp v) {

    switch (chiralClass) {
    default:
      return true;
    case ALLENE:
    case TETRAHEDRAL:
      return (isNot == (getHandedness(atom2, atom3, atom4, atom1, v) != order));
    case SQUARE_PLANAR:
      getPlaneNormals((P3d) atom1, (P3d) atom2, (P3d) atom3, (P3d) atom4, v);
      // vNorm1 vNorm2 vNorm3 are right-hand normals for the given
      // triangles
      // 1-2-3, 2-3-4, 3-4-1
      // sp1 up up up U-shaped   1 2 3 4
      // sp2 up up DOWN 4-shaped 1 3 2 4
      // sp3 up DOWN DOWN Z-shaped  1 2 4 3
      // 
      return (v.vNorm2.dot(v.vNorm3) < 0 ? isNot == (order != 3) : v.vNorm3
          .dot(v.vNorm4) < 0 ? isNot == (order != 2) : isNot == (order != 1));
    case TRIGONAL_PYRAMIDAL:
      return (isNot == (getHandedness(atom1, atom2, atom3, atom0, v) != order));
    case TRIGONAL_BIPYRAMIDAL:
      // check for axial-axial'
      if (!isDiaxial(atom0, atom0, atom5, atom1, v, -0.95d))
        return false;
      return (isNot == (getHandedness(atom2, atom3, atom4, atom1, v) != order));
    case T_SHAPED:
      // just checking linearity here; could do better.
      switch (order) {
      case 1:
        // @TS1
        // 1----0----3
        //      |
        //      2
        break;
      case 2:
        // @TS2
        // 1----0----2
        //      |
        //      3
        atom3 = atom2;
        break;
      case 3:
        // @TS3
        // 2----0----3
        //      |
        //      1
        atom1 = atom2;
        break;
      }
      return (isNot == !isDiaxial(atom0, atom0, atom1, atom3, v, -0.95d));
    case SEESAW:
      if (!isDiaxial(atom0, atom0, atom4, atom1, v, -0.95d))
        return false;
      return (isNot == (getHandedness(atom2, atom3, atom4, atom1, v) != order));
    case OCTAHEDRAL:
      if (!isDiaxial(atom0, atom0, atom6, atom1, v, -0.95d)
          || !isDiaxial(atom0, atom0, atom2, atom4, v, -0.95d)
          || !isDiaxial(atom0, atom0, atom3, atom5, v, -0.95d))
        return false;
      getPlaneNormals((P3d) atom2, (P3d) atom3, (P3d) atom4, (P3d) atom5, v);
      // check for proper order 2-3-4-5
      //                          n1n2n3
      if (v.vNorm2.dot(v.vNorm3) < 0 || v.vNorm3.dot(v.vNorm4) < 0)
        return false;
      // check for CW or CCW set in relation to the first atom
      v.vNorm3.sub2((P3d) atom0, (P3d) atom1);
      return (isNot == ((v.vNorm2.dot(v.vNorm3) < 0 ? 2 : 1) == order));
    case POLYHEDRAL:
      return true;
    }
  }

  static boolean isDiaxial(SimpleNode atomA, SimpleNode atomB, SimpleNode atom1, SimpleNode atom2,
                           VTemp v, double f) {
    v.vA.sub2((P3d) atomA, (P3d) atom1);
    v.vB.sub2((P3d) atomB, (P3d) atom2);
    v.vA.normalize();
    v.vB.normalize();
    // -0.95d about 172 degrees
    return (v.vA.dot(v.vB) < f);
  }

  /**
   * determine the winding of the circuit a--b--c relative to point pt
   * 
   * @param a
   * @param b
   * @param c
   * @param pt
   * @param v
   * @return 1 for "@", 2 for "@@"
   */
  static int getHandedness(SimpleNode a, SimpleNode b, SimpleNode c, SimpleNode pt, VTemp v) {
    double d = MeasureD.getNormalThroughPoints((P3d) a, (P3d) b, (P3d) c, v.vTemp, v.vA);
    //int atat = (Measure.distanceToPlaneV(v.vTemp, d, (P3) pt) > 0 ? 1 : 2);
//    System.out.println("$ draw p1 " + P3.newP((P3)a) +" color red '"+a+" [2]'");
//    System.out.println("$ draw p2 " + P3.newP((P3)b) +"  color green '"+b+" [3]'");
//    System.out.println("$ draw p3 " + P3.newP((P3)c) +"  color blue '"+c+" [4]'");
//    System.out.println("$ draw p " + P3.newP((P3)a) +" " + P3.newP((P3)b) +" " + P3.newP((P3)c) +"" );
//    System.out.println("$ draw v vector {" + P3.newP((P3)pt) +"}  " + v.vTemp+" '"+ (atat==2 ? "@@" : "@")+ pt + " [1]' color " + (atat == 2 ? "white" : "yellow"));
//    double e = v.vTemp.dot((P3) pt);
    d = MeasureD.distanceToPlaneV(v.vTemp, d, (P3d) pt);
//    System.out.println("# " + d);
    return (d > 0 ? 1 : 2);
  }

  private static void getPlaneNormals(P3d atom1, P3d atom2, P3d atom3,
                                      P3d atom4, VTemp v) {
    MeasureD.getNormalThroughPoints(atom1, atom2, atom3, v.vNorm2,
        v.vTemp1);
    MeasureD.getNormalThroughPoints(atom2, atom3, atom4, v.vNorm3,
        v.vTemp1);
    MeasureD.getNormalThroughPoints(atom3, atom4, atom1, v.vNorm4,
        v.vTemp1);
  }

  static int checkChirality(SmilesSearch search, String pattern, int index, SmilesAtom newAtom)
      throws InvalidSmilesException {
    int stereoClass = 0;
    int order = Integer.MIN_VALUE;
    int len = pattern.length();
    String details = null;
    String directives = null;
    int atomCount = 0;
    char ch;
    stereoClass = DEFAULT;
    order = 1;
    boolean isPoly = false;
    if (++index < len) {
      switch (ch = pattern.charAt(index)) {
      case '@':
        order = 2;
        index++;
        break;
      case '+':
      case '-':
      case 'H': // @H, @@H
        break;
      case 'P': //PH // Jmol
        isPoly = true;
        //$FALL-THROUGH$
      case 'A': //AL
      case 'O': //OH
      case 'S': //SP
      case 'T': //TH, TP
        stereoClass = (index + 1 < len ? getChiralityClass(pattern.substring(
            index, index + 2)) : -1);
        index += 2;
        break;
      default:
        order = (PT.isDigit(ch) ? 1 : -1);
      }
      int pt = index;
      if (order == 1 || isPoly) {
        while (pt < len && PT.isDigit(pattern.charAt(pt)))
          pt++;
        if (pt > index) {
          try {
            int n = Integer.parseInt(pattern.substring(index, pt));
            if (isPoly) {
              atomCount = n;
              if (pt < len && pattern.charAt(pt) == '(') {
                details = SmilesParser.getSubPattern(pattern, pt, '(');
                pt += details.length() + 2;
              } else if (pt < len && pattern.charAt(pt) == '@') {
                details = "@";
                pt++;
              }
              if (pt < len && pattern.charAt(pt) == '/') {
                directives = SmilesParser.getSubPattern(pattern, pt, '/');
                pt += directives.length() + 2;
              }
            } else {
              order = n;
            }
          } catch (NumberFormatException e) {
            order = -1;
          }
          index = pt;
        }
      }
      if (order < 1 || stereoClass < 0)
        throw new InvalidSmilesException("Invalid stereochemistry descriptor");
    }
    newAtom.stereo = new SmilesStereo(stereoClass, order, atomCount, details,
        directives);
    if (stereoClass == POLYHEDRAL) {
      search.polyAtom = newAtom;
      search.noAromatic = true;
    }
    newAtom.stereo.search = search; // allows for CIPChirality to work with SMILES strings
    if (SmilesParser.getChar(pattern, index) == '?') {
      Logger.info("Ignoring '?' in stereochemistry");
      index++;
    }
    return index;
  }
  
  private int[][] polyhedralOrders;
  private boolean isNot;
  private PolyhedronStereoSorter sorter;

  /**
   * experimental Jmol polySMILES 
   *  
   * @throws InvalidSmilesException
   */
  private void getPolyhedralOrders() throws InvalidSmilesException {
    int[][] po = polyhedralOrders = AU.newInt2(atomCount);
    if (details == null)
      return;
    if (details.length() > 0 && details.charAt(0) == '@')
      details = "!" + details.substring(1);
    if (details.length() == 0 || details.equals("!")) {
      for (int i = 2; i <= atomCount; i++)
        details += (i < 10 ? "" + i : "%" + i);
    }
    int[] temp = new int[details.length()];
    int[] ret = new int[1];
    String msg = null;
    int pt = 0;
    String s = details + "/";
    int n = 0;
    int len = s.length();
    int index = 0;
    int atomPt = 0;
    do {
      char ch = s.charAt(index);
      switch (ch) {
      case '!':
        isNot = true;
        index++;
        break;
      case '/':
      case '.':
        if ((pt = atomPt) >= atomCount) {
          msg = "Too many descriptors";
          break;
        }
        int[] a = po[atomPt] = new int[n];
        for (; --n >= 0;)
          a[n] = temp[n];
        n = 0;
        if (Logger.debugging)
          Logger.info(PT.toJSON("@PH" + atomCount + "[" + atomPt + "]", a));
        if (ch == '/')
          index = Integer.MAX_VALUE;
        else
          index++;
        atomPt++;
        break;
      default:
        index = SmilesParser.getRingNumber(s, index, ch, ret);
        pt = temp[n++] = ret[0] - 1;
        if (pt == atomPt)
          msg = "Atom cannot connect to itself";
        else if (pt < 0 || pt >= atomCount)
          msg = "Connection number outside of range (1-" + atomCount + ")";
        else if (n >= atomCount)
          msg = "Too many connections indicated";
      }
      if (msg != null) {
        msg += ": " + s.substring(0, index) + "<<";
        throw new InvalidSmilesException(msg);
      }
    } while (index < len);
    return;
  }

  public static int getAtropicStereoFlag(Node[] nodes) {
    return (MeasureD.computeTorsion((T3d) nodes[0], 
        (T3d) nodes[1], (T3d)nodes[2], (T3d) nodes[3], true) < 0 ? 1 : -1); 
  }
}
 