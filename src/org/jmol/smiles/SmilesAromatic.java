/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-05-08 19:20:44 -0500 (Sat, 08 May 2010) $
 * $Revision: 13038 $
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

import java.util.Hashtable;

import javajs.util.Lst;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.V3;

import javajs.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.Edge;
import org.jmol.util.Logger;
import org.jmol.util.Node;
import org.jmol.util.SimpleNode;

public class SmilesAromatic {

  /**
   * Main entry point. Note that unless bonds are pre-defined as aromatic, Jmol
   * will first check for a flat ring configuration. This is 3D, after all.
   * 
   * @param n
   * @param jmolAtoms
   * @param bsSelected
   * @param vR
   * @param bsAromatic
   * @param strictness
   * @param isOpenSMILES
   * @param justCheckBonding
   * @param checkExplicit
   * @param v
   * @param vOK
   * @param lstSP2
   * @param eCounts
   * @param doTestAromatic
   */
  static void setAromatic(int n, Node[] jmolAtoms, BS bsSelected,
                          Lst<Object> vR, BS bsAromatic, int strictness,
                          boolean isOpenSMILES, boolean justCheckBonding, 
                          boolean checkExplicit, VTemp v,
                          Lst<BS> vOK, Lst<SmilesRing> lstSP2,
                          int[] eCounts, boolean doTestAromatic) {

    boolean doCheck = (isOpenSMILES || strictness > 0);

    // "strict" means we want true Hueckel 4n+2
    //  -- relax planarity, as bonding should be more important

    if (!doTestAromatic) {
      // if the aromaticity has been set by the SMILES string itself, we
      // just collect the aromatic rings here
      for (int r = vR.size(); --r >= 0;) {
        BS bs = BSUtil.copy((BS) vR.get(r));
        bs.and(bsAromatic);
        if (bs.cardinality() == n)
          vOK.addLast(bs);
      }
      return;
    }
    for (int r = vR.size(); --r >= 0;) {
      BS bs = (BS) vR.get(r);
      boolean isOK = isSp2Ring(n, jmolAtoms, bsSelected, bs,
          (justCheckBonding ? Float.MAX_VALUE : strictness > 0 ? 0.1f : 0.01f),
          checkExplicit,
          strictness == 0);
      if (!isOK)
        continue;
      bsAromatic.or(bs);
      if (doCheck) {
        // we will need to check these edges later
        Lst<Edge> edges = new Lst<Edge>();
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
          Node a = jmolAtoms[i];
          Edge[] aedges = a.getEdges();
          int ai = a.getIndex();
          for (int j = aedges.length; --j >= 0;) {
            SimpleNode a2 = aedges[j].getOtherNode(a);
            int a2i = a2.getIndex();
            if (a2i > ai && bs.get(a2i))
              edges.addLast(aedges[j]);
          }
        }
        switch (checkHueckelAromatic(n, jmolAtoms, bsAromatic, bs,
            strictness, eCounts)) {
        case -1: // absolutely not
          continue;
        case 0: // maybe -- needs fused ring check
          isOK = false;
          //$FALL-THROUGH$
        case 1:
          if (lstSP2 != null)
            lstSP2.addLast(new SmilesRing(n, bs, edges, isOK));
          if (!isOK)
            continue;
        }
      }
      vOK.addLast(bs);
    }
  }

  ////////////////////////// Aromaticity Explicitly Defined by User ////////////////////////
  
  /**
   * Set aromatic atoms based on predefined BOND_AROMATIC definitions.
   * Allows for totally customized creation of aromatic SMILES.
   * 
   * @param jmolAtoms
   * @param bsSelected
   * @param bsAromatic 
   */
  static void checkAromaticDefined(Node[] jmolAtoms, BS bsSelected, BS bsAromatic) {
    for (int i = bsSelected.nextSetBit(0); i >= 0; i = bsSelected
        .nextSetBit(i + 1)) {
      Edge[] bonds = jmolAtoms[i].getEdges();
      for (int j = 0; j < bonds.length; j++) {
        switch (bonds[j].order) {
        case Edge.BOND_AROMATIC:
        case Edge.BOND_AROMATIC_DOUBLE:
        case Edge.BOND_AROMATIC_SINGLE:
          bsAromatic.set(bonds[j].getAtomIndex1());
          bsAromatic.set(bonds[j].getAtomIndex2());
        }
      }
    }
  }

  
  
  ///////////////////////////  3D SMILES methods ///////////////////////

  /**
   * 3D-SEARCH aromaticity test.
   * 
   * A simple and unambiguous test for aromaticity based on 3D geometry and
   * connectivity only, not Hueckel theory.
   * 
   * @param n
   * 
   * @param atoms
   *        a set of atoms with coordinate positions and associated bonds.
   * @param bs
   *        a bitset of atoms within the set of atoms, defining the ring
   * @param bsSelected
   *        must not be null
   * @param cutoff
   *        an arbitrary value to test the standard deviation against. 0.01 is
   *        appropriate here. Use Float.MAX_VALUE to just do bond connectivity
   *        check
   * @param checkExplicit
   *        check bonds that are explicit only - for XYZ and QM calcs
   * @param allowSOxide
   *        set TRUE to skip S atoms
   * @return true if standard deviation of vNorm.dot.vMean is less than cutoff
   */

  private final static boolean isSp2Ring(int n, Node[] atoms, BS bsSelected,
                                         BS bs, float cutoff,
                                         boolean checkExplicit,
                                         boolean allowSOxide) {
    ///
    // 
    // Bob Hanson, hansonr@stolaf.edu
    // 
    //   Given a ring of N atoms...
    //   
    //                 1
    //               /   \
    //              2     6 -- 6a
    //              |     |
    //        5a -- 5     4
    //               \   /
    //                 3  
    //   
    //   with arbitrary order and up to N substituents
    //   
    //   1) Check to see if all ring atoms have no more than 3 connections.
    //      Note: An alternative definition might include "and no substituent
    //      is explicitly double-bonded to its ring atom, as in quinone.
    //      Here we opt to allow the atoms of quinone to be called "aromatic."
    //   2) Select a cutoff value close to zero. We use 0.01 here. 
    //   3) Generate a set of normals as follows:
    //      a) For each ring atom, construct the normal associated with the plane
    //         formed by that ring atom and its two nearest ring-atom neighbors.
    //      b) For each ring atom with a substituent, construct a normal 
    //         associated with the plane formed by its connecting substituent
    //         atom and the two nearest ring-atom neighbors.
    //      c) If this is the first normal, assign vMean to it. 
    //      d) If this is not the first normal, check vNorm.dot.vMean. If this
    //         value is less than zero, scale vNorm by -1.
    //      e) Add vNorm to vMean. 
    //   4) Calculate the standard deviation of the dot products of the 
    //      individual vNorms with the normalized vMean. 
    //   5) The ring is deemed flat if this standard deviation is less 
    //      than the selected cutoff value. 
    //      
    //   Efficiencies:
    //   
    //   1) Precheck bond counts.
    //   
    //   2) Each time a normal is added to the running mean, test to see if 
    //      its dot product with the mean is within 5 standard deviations. 
    //      If it is not, return false. Note that it can be shown that for 
    //      a set of normals, even if all are aligned except one, with dot product
    //      to the mean x, then the standard deviation will be (1 - x) / sqrt(N).
    //      Given even an 8-membered ring, this still
    //      results in a minimum value of x of about 1-4c (allowing for as many as
    //      8 substituents), considerably better than our 1-5c. 
    //      So 1-5c is a very conservative test.   
    //      
    //   3) One could probably dispense with the actual standard deviation 
    //      calculation, as it is VERY unlikely that an actual nonaromatic ring
    //      (other than quinones and other such compounds)
    //      would have any chance of passing the first two tests.
    //      

    if (checkExplicit) {
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        if (atoms[i].getCovalentBondCount() > 3)
          return false;
    } else {
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        if (atoms[i].getCovalentBondCountPlusMissingH() > 3)
          return false;
    }
    if (cutoff == Float.MAX_VALUE)
      return true;

    if (cutoff <= 0)
      cutoff = 0.01f;

    V3 vNorm = null;
    V3 vTemp = null;
    V3 vMean = null;
    int nPoints = bs.cardinality();
    V3[] vNorms = new V3[nPoints * 2];
    int nNorms = 0;
    float maxDev = (1 - cutoff * 5);
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      Node ringAtom = atoms[i];
      Edge[] bonds = ringAtom.getEdges();
      // if more than three connections, ring cannot be fully conjugated
      // identify substituent and two ring atoms
      int iSub = -1;
      int r1 = -1;
      int r2 = -1;
      for (int k = bonds.length; --k >= 0;) {
        int iAtom = ringAtom.getBondedAtomIndex(k);
        if (!bsSelected.get(iAtom))
          continue;
        // OpenSMILES allows tetrahedral S
        if (!bs.get(iAtom)) {
          if (ringAtom.getElementNumber() == 16) {
            if (!allowSOxide)
              return false;
            iAtom = -1;
          }
          iSub = iAtom;
        } else if (r1 < 0) {
          r1 = iAtom;
        } else {
          r2 = iAtom;
        }
      }

      if (vMean == null) {
        vMean = new V3();
        vNorm = new V3();
        vTemp = new V3();
      }

      // check the normal for r1 - i - r2 plane
      // check the normal for r1 - iSub - r2 plane

      for (int k = 0, j = i; k < 2; k++) {
        Measure.getNormalThroughPoints((P3) atoms[r1], (P3) atoms[j], (P3) atoms[r2],
            vNorm, vTemp);
        if (!addNormal(vNorm, vMean, maxDev))
          return false;
        vNorms[nNorms++] = V3.newV(vNorm);
        if ((j = iSub) < 0)
          break;
      }
    }
    return checkStandardDeviation(vNorms, vMean, nNorms, cutoff);
  }

  /**
   * adds a normal if similarity is within limits
   * 
   * @param vTemp
   * @param vMean
   * @param maxDev
   * @return true if successful
   */
  private final static boolean addNormal(V3 vTemp, V3 vMean, float maxDev) {
    float similarity = vMean.dot(vTemp);
    if (similarity != 0 && Math.abs(similarity) < maxDev)
      return false;
    if (similarity < 0)
      vTemp.scale(-1);
    vMean.add(vTemp);
    vMean.normalize();
    return true;
  }

  /**
   * calculates a dot-product standard deviation and reports if it is below a
   * cutoff
   * 
   * @param vNorms
   * @param vMean
   * @param n
   * @param cutoff
   * @return true if stddev < cutoff
   */
  private final static boolean checkStandardDeviation(V3[] vNorms, V3 vMean,
                                                      int n, float cutoff) {
    double sum = 0;
    double sum2 = 0;
    for (int i = 0; i < n; i++) {
      float v = vNorms[i].dot(vMean);
      sum += v;
      sum2 += ((double) v) * v;
    }
    sum = Math.sqrt((sum2 - sum * sum / n) / (n - 1));
    return (sum < cutoff);
  }

  

//////////////////////////////  OpenSMILES Specification for Aromaticity  //////////////////////////
  
  /**
   * 
   * Index to inner array is covalent bond count (b) + valence (v) + charge (c,
   * carbon only) - 4. Special cases are listed here as -1. -2 indicates not
   * considered aromatic (probably not possible).
   * 
   * Many thanks to John May for the excellent visual guide that I have
   * condensed here.
   * 
   */
  final static private int[][] OS_PI_COUNTS = { 
      { -2, 1, 0 },          // 0 B    b+v-4
      { 1, 2, 1, -1 },       // 1 C    b+v+c-4
      { 2, 1, 2, 1, 1 },    // 2 N,P  b+v-4
      { 2, 1 },              // 3 O,Se b+v-4
      { -2, 1, 2, 1, -2 },   // 4 As   b+v-4
      { 2, 1, 2, 2 }         // 5 S    b+v-4
  };

  /**
   * For each atom in the ring, look up a unique combination of covalent bond
   * count, valence, and charge for each atom and use that as a key into the
   * PI_COUNTS array. c=X is the only special case. Final trimming will be
   * necesseary if isStrict == true.
   * 
   * @param nAtoms
   *        this ring's size
   * @param jmolAtoms
   *        could also be constructed nodes from a SMILES string
   * @param bsAromatic
   *        at least nominally aromatic atoms
   * @param bsRing
   *        specific atoms of this ring
   * @param strictness
   *        0 (not) 1 (OpenSMILES), 2 (MMFF94) standard organic chemist's
   *        Hueckel interpretation, not allowing c=O
   * @return -1 if absolutely not possible, 0 if possible but not 4n+2, 1 if
   *         4n+2
   * @author Bob Hanson 3/12/2016
   * @param eCounts
   * 
   */
  private static int checkHueckelAromatic(int nAtoms, Node[] jmolAtoms,
                                                 BS bsAromatic, BS bsRing,
                                                 int strictness, int[] eCounts) {
    int npi = 0; // total number of pi electrons
    int n1 = 0;  // total number of atoms contributing exactly 1 electron (for strictness==2)
    for (int i = bsRing.nextSetBit(0); i >= 0 && npi >= 0; i = bsRing
        .nextSetBit(i + 1)) {
      Node atom = jmolAtoms[i];
      int z = atom.getElementNumber();
      int n = atom.getCovalentBondCountPlusMissingH();
      n += atom.getValence();
      n -= 4;
      if (z == 6) {
        int fc = atom.getFormalCharge(); // add in charge for C
        if (fc != Integer.MIN_VALUE) // SmilesAtom charge not set
          n += fc;
      }
      int pt = (z >= 5 && z <= 8 ? z - 5 // B, C, N, O
          : z == 15 ? 2 // P -> N
              : z == 34 ? 3 // Se - > O
                  : z == 33 ? 4 // As special
                      : z == 16 ? 5 // S special
                          : -1);
      if (pt >= 0) {
        int[] a = OS_PI_COUNTS[pt];
        if (n < 0 || n >= a.length)
          return -1;
        switch (n = a[n]) {
        case -2:
          // not a connection/valence/charge of interest
          return -1;
        case -1:
          // check for c=X(nonaromatic) 0
          // against c[c+1](C)c (0) and c=c (1)
          Edge[] bonds = atom.getEdges();
          n = 0; // no double bond; sp2 c cation
          for (int j = bonds.length; --j >= 0;) {
            Edge b = bonds[j];
            if (b.getCovalentOrder() != 2)
              continue;
            // just check that the connected atom is either C or flat-aromatic
            // if it is, assign 1 pi electron; if it is not, set it to 0 as long
            // we are not being strict or discard this ring if we are.
            SimpleNode het = b.getOtherNode(atom);
            n = (het.getElementNumber() == 6 || bsAromatic.get(het.getIndex()) ? 1
                : strictness > 0 ? -100 : 0);
            break;
          }
          //$FALL-THROUGH$
        default:
          // ok -- add in the number of pi electrons for this atom
          if (n < 0)
            return -1;
          if (eCounts != null)
            eCounts[i] = n;
          npi += n;
          if (n == 1)
            n1++;
          if (Logger.debuggingHigh)
            Logger.info("atom " + atom + " pi=" + n + " npi=" + npi);
          // normal continuance
          continue;
        }
      }
    }
    // Hueckel: npi =?= 4n + 2
    // MMFF94: all atoms must contribute 1 for 6- or 7-membered rings (3 double bonds)
    return ((npi - 2) % 4 == 0 && (strictness < 2 || nAtoms == 5 || n1 == 6) ? 1 : 0);
  }

  /**
   * Iteratively trims a set of aromatic atoms that may be initially assigned to
   * be aromatic but because their double bonds extend to non-aromatic atoms
   * must be removed. Checks to see that these atoms really have two adjacent
   * aromatic atoms and are not connected to any nonaromatic atom by a double
   * bond.
   * 
   * Could be entered with one of /open/, /open strict/, or /strict/
   * 
   * @param jmolAtoms
   * @param bsAromatic
   * @param lstAromatic all rings passing the sp2 test and (if strict) the Hueckel strict test
   * @param lstSP2 all rings passing the sp2 test
   * @param eCounts
   * @param isOpenNotStrict
   *        /open/ option
   * @param isStrict
   *        remove noncyclic double bonds and do not allow bridging aromatic
   *        ring systems (/strict/ option)
   */
  static void finalizeAromatic(Node[] jmolAtoms, BS bsAromatic,
                               Lst<BS> lstAromatic, Lst<SmilesRing> lstSP2,
                               int[] eCounts, boolean isOpenNotStrict,
                               boolean isStrict) {

    // strictly speaking, there is no such thing as a bridged aromatic pi system    
    if (isStrict)
      removeBridgingRings(lstAromatic, lstSP2);

    // we allow for combined 4n+2 even if contributing rings are not (5+7 for azulene) 
    checkFusedRings(lstSP2, eCounts, lstAromatic);

    // regenerate bsAromatic, using only valid rings now
    bsAromatic.clearAll();
    for (int i = lstAromatic.size(); --i >= 0;)
      bsAromatic.or(lstAromatic.get(i));

    if (isStrict || isOpenNotStrict) {

      // Check each aromatic atom for 
      // (a) no exocyclic double bonds that are not part of an aromatic ring (biphenylene; strict only)
      // (b) no exocyclic double bonds to nonaromatic atoms ( strict only)
      // (c) at least two adjacent aromatic atoms (Hueckel cyclic requirement)
      
      
      for (int i = bsAromatic.nextSetBit(0); i >= 0; i = bsAromatic
          .nextSetBit(i + 1)) {
        Edge[] bonds = jmolAtoms[i].getEdges();
        int naro = 0;
        for (int j = bonds.length; --j >= 0;) {
          SimpleNode otherAtom = bonds[j].getOtherNode(jmolAtoms[i]);
          int order = bonds[j].getCovalentOrder();
          int ai2 = otherAtom.getIndex();
          boolean isJAro = bsAromatic.get(ai2);
          if (isJAro) {
            if (order == 2) {
              // test (a)
              // make sure these two aromatic atoms are in the same aromatic ring
              boolean isOK = false;
              for (int k = lstSP2.size(); --k >= 0;) {
                SmilesRing r = lstSP2.get(k);
                if (r.get(i) && r.get(ai2)) {
                  isOK = true;
                  break;
                }
              }
              if (!isOK) {
                naro = -1;
                break;
              }
            }
            naro++;
          } else if (isStrict && otherAtom.getElementNumber() == 6
              && order == 2) {
            // test (b)
            naro = -1;
            break;
          }
        }
        if (naro < 2) {
          // test (c)
          bsAromatic.clear(i);
          // reiterate
          i = -1; 
        }
      }
    }
  }

  /**
   * check for any two rings with more than two common atoms and remove them
   * from the pool
   * 
   * @param lstAromatic
   * @param lstSP2 
   */
  private static void removeBridgingRings(Lst<BS> lstAromatic, Lst<SmilesRing> lstSP2) {
    BS bs = new BS();
    BS bsBad = new BS();
    BS bsBad2 = new BS();
    checkBridges(lstAromatic, bsBad, lstAromatic, bsBad, bs);
    checkBridges(lstSP2, bsBad2, lstSP2, bsBad2, bs);
    checkBridges(lstAromatic, bsBad, lstSP2, bsBad2, bs);
    for (int i = lstAromatic.size(); --i >= 0;)
      if (bsBad.get(i))
        lstAromatic.removeItemAt(i);
    for (int i = lstSP2.size(); --i >= 0;)
      if (bsBad2.get(i))
        lstSP2.removeItemAt(i);
  }

  private static void checkBridges(Lst<?> lst, BS bsBad, Lst<?> lst2,
                                   BS bsBad2, BS bs) {
    boolean isSameList = (lst == lst2);
    for (int i = lst.size(); --i >= 0;) {
      BS bs1 = (BS) lst.get(i);
        for (int j0 = (isSameList ? i + 1 : 0), j = lst2.size(); --j >= j0;) {
          BS bs2 = (BS) lst2.get(j);
          if (bs2.equals(bs1))
            continue;
          bs.clearAll();
          bs.or(bs1);
          bs.and(bs2);
          int n = bs.cardinality();
          if (n > 2) {
            bsBad.set(i);
            bsBad2.set(j);
          }
        }
    }
  }

  /**
   * Add fused rings based on the Hueckel 4n+2 rule. Note that this may be
   * reverted later if in a STRICT setting, because in at this point in some
   * cases we will have double bonds to exocyclic nonaromatic atoms.
   * 
   * We are being careful here to only group FUSED rings -- that is rings that
   * have only one bond in common.
   * 
   * @param rings
   * @param eCounts
   * @param lstAromatic
   *        list to be appended to
   */
  private static void checkFusedRings(Lst<SmilesRing> rings, int[] eCounts,
                                    Lst<BS> lstAromatic) {
    Hashtable<String, SmilesRingSet> htEdgeMap = new Hashtable<String, SmilesRingSet>();
    for (int i = rings.size(); --i >= 0;) {
      SmilesRing r = rings.get(i);
      Lst<Edge> edges = r.edges;
      for (int j = edges.size(); --j >= 0;) {
        SmilesRingSet set = SmilesRing.getSetByEdge(edges.get(j), htEdgeMap);
        if (set == null || set == r.set)
          continue;
        // TODO what about bridging?
        if (r.set != null)
          set.addSet(r.set, htEdgeMap);
        else
          set.addRing(r);
      }
      (r.set == null ? r.set = new SmilesRingSet() : r.set).addRing(r);
      r.addEdges(htEdgeMap);
    }
    SmilesRingSet set;
    SmilesRing r;
    for (int i = rings.size(); --i >= 0;) {
      if ((r = rings.get(i)).isOK || (set = r.set) == null || set.isEmpty())
        continue;
      if ((set.getElectronCount(eCounts) % 4) == 2)
        for (int j = set.size(); --j >= 0;)
          if (!(r = set.get(j)).isOK)
            lstAromatic.addLast(r);
      set.clear();
    }
  }

}
