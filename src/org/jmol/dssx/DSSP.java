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

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;

import org.jmol.c.STR;
import org.jmol.i18n.GT;
import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.HBond;
import org.jmol.modelset.Model;
import org.jmol.modelsetbio.AminoMonomer;
import org.jmol.modelsetbio.AminoPolymer;
import org.jmol.modelsetbio.BioPolymer;
import org.jmol.modelsetbio.Monomer;
import org.jmol.util.C;
import org.jmol.util.Edge;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;
//import javajs.util.List;

public class DSSP {

  ////////////////////// DSSP /////////////////////
  //
  //    W. Kabsch and C. Sander, Biopolymers, vol 22, 1983, pp 2577-2637
  //
  // DSSP 2.0 as described in Int. J. Mol. Sci. 2014, 15, 7841-7864; doi:10.3390/ijms15057841
  //
  //   ------------------license permission-----------------
  //
  //   ---------- Forwarded message ----------
  //   From: Gert Vriend <vriend@cmbi.ru.nl>
  //   Date: Wed, Oct 6, 2010 at 12:28 PM
  //   Subject: Re: DSSP license
  //   To: Robert Hanson <hansonr@stolaf.edu>
  // 
  //   Dear Robert Hanson,
  // 
  //   Feel free to freely distribute your DSSP-like code with JMOL, using any 
  //   license form you want (but preferably one that avoids people from 'stealing' 
  //   the code for activities that go against the spirit of free software exchanges).
  // 
  //   Please put somewhere (doesn't need to be a prominent place, but should be 
  //   clickable/visible one way or another):
  //   "We thank Wolfgang Kabsch and Chris Sander for writing the DSSP software, 
  //   and we thank the CMBI for maintaining it to the extent that it was easy to 
  //   re-engineer for our purposes." 
  // 
  //   Greetings
  //   Gert
  //
  //
  //   ------------------end of license permission-----------------
  //
  //   Added note by Bob Hanson 10/7/2010:
  //   
  //   Although the DSSP code from CMBI was inspected in order to confirm 
  //   conformance with that exact implementation of the algorithm described in
  //   the Kabsch and Sander paper, none of that code was extracted. That is to 
  //   say, this is an entirely different implementation.
  //
  //   This implementation of the DSSP algorithm is based solely upon the published 
  //   description of that algorithm -- as evidenced by the quoted statements
  //   in the Java doc and comments accompanying each method -- and has essentially
  //   no relationship to the Pascal/C++ code, other than that it produces a 
  //   similar result. 
  // 
  //   My approach to identifying chain breaks (use of the BioPolymer class), 
  //   cataloging bridges (using hash tables), and generating the SUMMARY line (using 
  //   bit sets), is an entirely different approach than that used by the original 
  //   authors of the DSSP code.
  //
  //   This implementation has been verified against 1769 high-resolution structures. 
  //   (see http://jmol.svn.sourceforge.net/viewvc/jmol/trunk/Jmol-datafiles/dssp/cullpdb_pc20_res1.6_R0.25_d101001_chains1769)
  //   which is from http://dunbrack.fccc.edu/Guoli/pisces_download.php#culledpdb.
  //
  //   All structures were verified either via comparison with REST data from RCSB, or for the
  //   few cases for which that was not possible, by direct comparison with DSSP files
  //   provided by http://swift.cmbi.ru.nl/gv/dssp.
  //
  //   Still, we cannot guarantee that this code will always result in an EXACT MATCH 
  //   to the Pascal/C++ code provided by CMBI, and we fully expect that there are 
  //   special cases where, particularly, the CMBI code senses a chain break 
  //   even though Jmol does not, or vice-versa, resulting in different analyses.
  //
  //   Known differences to CMBI DSSP:
  //
  //     Chain breaks
  //     ------------
  // 
  //   Jmol has its own way to calculate chain breaks. In addition,
  //   for these purposes, it does consider amino acids lacking a 
  //   carbonyl oxygen to be a chain break, for consistency with DSSP.
  //   When that is the case, no NH hydrogen bond is allowed from that
  //   residue, and no helix is allowed to span it.
  //
  //     Backbone amide hydrogens
  //     ------------------------
  //
  //   This code allows for the use of file-based backbone amide hydrogen
  //   positions (via SET dsspCalculateHydrogenAlways FALSE), which the CMBI code does 
  //   not. Certainly for some models (1def, for example) that produces a different 
  //   result, because it changes the values of calculated hydrogen bond energies.
  //
  //     Alternative locations
  //     ---------------------
  //
  //   As part of this implementation, the CONFIGURATION command was modified to be
  //   consistent with a proper interpretation of the PDB file - that codes A, B, C,
  //   etc., are only relevant WITHIN a specific residue, not across the entire file,
  //   though in most cases that is true as well. 
  //   
  //   Jmol allows for setting "configurations" that include only a subset of the
  //   alternative locations across an entire model. Thus, while the CMBI implementation
  //   of DSSP allows for reading the first configuration, Jmol allows for getting 
  //   the DSSP secondary structure analysis for any configuration. Simply use 
  //   the command CONFIGURATION n, where "n" is a number 1, 2, 3, etc., just prior
  //   to the CALCULATE STRUCTURE command.
  //
  //     Alternative SS methods
  //     ----------------------
  // 
  //   Jmol also allows for a strictly geometric method -- without hydrogen bonding --
  //   to identify sheet strands (not ladders or full "sheets") and helices. This is 
  //   invoked by
  //
  //   CALCULATE STRUCTURE RAMACHANDRAN
  //
  //     Bend (S)
  //     --------
  //
  //   Jmol does not report S in the summary line, only B, E, H, G, I, and T.
  //
  //     Surface Accessibility
  //     ---------------------
  // 
  //   Jmol does not implement surface accessibility.
  //
  //     Detailed Reporting
  //     ------------------
  //
  //   Jmol does not create the detailed report that CMBI DSSP does.
  //   We do not report hydrogen bond energies, C-alpha points, sheet 
  //   or ladder designators, or disulfide bonds.
  // 
  //   Instead, our report includes the helix-5, helix-4, and helix-3 lines
  //   as well as the summary line similar to that given in the original paper
  //   along with a summary list of stretches of B, E, H, G, I, and T structure.
  //
  //   You can use SET DEBUG TRUE to send voluminous amounts of 
  //   information to the Java console if you wish.
  //
  //   One final implementation note: It is curious that the DSSP algorithm 
  //   orders the SUMMARY line -- our final assignments -- as: H B E G I T S. 
  //   The curious thing is that this TECHNICALLY allows for calculated bridges being
  //   assignable to H groups. As noted below, I don't think this is physically possible,
  //   but it seems to me there must be SOME reason to do this rather than the more 
  //   obvious order: B E H G I T S. So there's a bit of a mystery there. My implementation
  //   adds a warning at the end of the helix-4 line if such a bridge should ever appear. 
  //   If this warning is seen, it probably means you forgot to use the CONFIGURATION command.
  // 
  ////////////////////// DSSP /////////////////////

  public DSSP() {
    // for reflection
  }

  private BioPolymer[] bioPolymers;
  private Lst<Bond> vHBonds;
  private BS[] done;
  private boolean doReport;
  private boolean dsspIgnoreHydrogens;
  private boolean setStructure;
  private char[][] labels;
  private BS bsBad;
  private int bioPolymerCount;
  private Map<String, Bridge> htBridges;
  private Map<int[][], Boolean> htLadders;
  private Lst<Bridge> bridgesA;
  private Lst<Bridge> bridgesP;
  private boolean isDSSP2;

  /**
   * 
   * @param objBioPolymers
   * @param bioPolymerCount
   * @param objVHBonds
   * @param doReport
   * @param dsspIgnoreHydrogens
   * @param setStructure
   * @param version  can be 2.0 to reverse order of helix calculation and emphasize pi-helices 
   * @return helix-5, helix-4, helix-3, and SUMMARY lines
   */

  
  @SuppressWarnings("unchecked")
  public String calculateDssp(Object[] objBioPolymers, int bioPolymerCount,
                              Object objVHBonds, boolean doReport,
                              boolean dsspIgnoreHydrogens, boolean setStructure, int version) {
    bioPolymers = (BioPolymer[]) objBioPolymers;
    this.bioPolymerCount = bioPolymerCount;
    vHBonds = (Lst<Bond>) objVHBonds;
    this.doReport = doReport;
    this.dsspIgnoreHydrogens = dsspIgnoreHydrogens;
    this.setStructure = setStructure;
    isDSSP2 = (version > 1);
    BS bsAmino = new BS();
    for (int i = 0; i < bioPolymerCount; i++)
      if (bioPolymers[i] instanceof AminoPolymer)
        bsAmino.set(i);
    if (bsAmino.isEmpty())
      return "";

    Model m = bioPolymers[0].model;
    SB sb = new SB();
    sb.append("Jmol ").append(Viewer.getJmolVersion()).append(
        " DSSP analysis for model ").append(m.ms.getModelNumberDotted(m.modelIndex)).append(
        " - ").append(m.ms.getModelTitle(m.modelIndex)).append("\n");
    if (m.modelIndex == 0)
      sb.append(
              "\nW. Kabsch and C. Sander, Biopolymers, vol 22, 1983, pp 2577-2637\n\nWe thank Wolfgang Kabsch and Chris Sander for writing the DSSP software,\nand we thank the CMBI for maintaining it to the extent that it was easy to\nre-engineer in Java for our purposes. \n\nSecond generation DSSP 2.0 is ")
              .append(isDSSP2 ? "" : "NOT ")
              .append("used in this analysis. See Int. J. Mol. Sci. 2014, 15, 7841-7864; doi:10.3390/ijms15057841.\n");
    if (setStructure && m.modelIndex == 0)
      sb.append("\nAll bioshapes have been deleted and must be regenerated.\n");

    if (m.altLocCount > 0)
      sb
          .append("\nNote: This model contains alternative locations. Use  'CONFIGURATION 1' to be consistent with CMBI DSSP.\n");

    // for each AminoPolymer, we need:
    // (1) a label reading "...EEE....HHHH...GGG...BTTTB...IIIII..."
    // (2) a residue-bitset to indicate that an assignment has been made already
    // (3) an atom-bitset to indicate we have a bad residue (no carbonyl O)

    labels = new char[bioPolymerCount][];
    done = new BS[bioPolymerCount];
    bsBad = new BS();
    boolean haveWarned = false;

    for (int i = bsAmino.nextSetBit(0); i >= 0; i = bsAmino.nextSetBit(i + 1)) {
      AminoPolymer ap = (AminoPolymer) bioPolymers[i];
      if (!haveWarned
          && ((AminoMonomer) ap.monomers[0]).getExplicitNH() != null) {
        if (dsspIgnoreHydrogens)
          sb
              .append(GT.o(GT
                  .$(
                      "NOTE: Backbone amide hydrogen positions are present and will be ignored. Their positions will be approximated, as in standard DSSP analysis.\nUse {0} to not use this approximation.\n\n"),
                      "SET dsspCalculateHydrogenAlways FALSE"));
        else
          sb
              .append(GT.o(GT
                  .$(
                      "NOTE: Backbone amide hydrogen positions are present and will be used. Results may differ significantly from standard DSSP analysis.\nUse {0} to ignore these hydrogen positions.\n\n"),
                      "SET dsspCalculateHydrogenAlways TRUE"));
        haveWarned = true;
      }
      ap.recalculateLeadMidpointsAndWingVectors();
      int n = ap.monomerCount;
      labels[i] = new char[n];
      done[i] = new BS();
      // lacking a C=O counts as done or "chain break"
      for (int j = 0; j < n; j++)
        if (((AminoMonomer) ap.monomers[j]).getCarbonylOxygenAtom() == null)
          bsBad.set(ap.monomers[j].leadAtomIndex);
    }

    // Step 1: Create a polymer-based array of dual-minimum NH->O connections
    //         similar to those used in Rasmol.

    int[][][][] min = getDualHydrogenBondArray();

    // NOTE: (p. 2587) "Structural overalaps are eliminated in this line by giving 
    //                  priority to H,B,E,G,I,T,S in this order." 
    //
    // We do B and E first, then H G I. Oddly enough, this technically allows for 
    // bridges to helix groups; I think, though, that is impossible.
    // These will be flagged on the helix-3 line with a warning.

    // Step 2: Find the bridges and mark them all as "B".

    bridgesA = new Lst<Bridge>();
    bridgesP = new Lst<Bridge>();
    htBridges = new Hashtable<String, Bridge>();
    htLadders = new Hashtable<int[][], Boolean>();
    getBridges(min);

    // Step 3: Find the ladders and bulges, mark them as "E", and add the sheet structures.

    getSheetStructures();

    // Step 4: Find the helices and mark them as "G", "H", or "I", 
    //         mark remaining turn residues as "T", and add the helix and turn structures.

    String[] reports = new String[bioPolymerCount];
    for (int i = bsAmino.nextSetBit(0); i >= 0; i = bsAmino.nextSetBit(i + 1))
      if (min[i] != null)
        reports[i] = findHelixes(i, min[i]);

    // Done!

    if (doReport) {
      SB sbSummary = new SB();
      sb.append("\n------------------------------\n");
      for (int i = bsAmino.nextSetBit(0); i >= 0; i = bsAmino.nextSetBit(i + 1))
        if (labels[i] != null) {
          AminoPolymer ap = (AminoPolymer) bioPolymers[i];
          sbSummary.append(dumpSummary(ap, labels[i]));
          sb.append(reports[i]).append(
              dumpTags(ap, "$.1: " + String.valueOf(labels[i]), bsBad, 2));
        }
      if (bsBad.nextSetBit(0) >= 0)
        sb
            .append("\nNOTE: '!' indicates a residue that is missing a backbone carbonyl oxygen atom.\n");
      sb.append("\n").append("SUMMARY:" + sbSummary);
    }

    return sb.toString();
  }

  /**
   * 
   * (p. 2579):
   * 
   * Hydrogen bonds in proteins have little wave-function overlap and are well
   * described by an electromodel:
   * 
   * E = q1q2(1/r(ON) + 1/r(CH) - 1/r(OH) - 1/r(CN)) * f
   * 
   * with q1 = 0.42e and q2 = 0.20e, e being the unit electron charge and r(AB)
   * the interatomic distance from A to B. In chemical units, r is in angstroms,
   * the dimensional factor f = 332, and E is in kcal/mol. We ... assign an H
   * bond between C=O of residue i and N-H of residue j if E is less than the
   * cutoff, i.e., "Hbond(i,j) =: [E < -0.5 kcal/mol]."
   * 
   * @return array of dual-minmum NH-->O=C H bonds
   * 
   */
  private int[][][][] getDualHydrogenBondArray() {

    // The min[][][][] array:  min[iPolymer][i][[hb1],[hb2]]
    //   where i is the index of the NH end of the bond, 
    //   and [hb1] and [hb2] are [iPolymer2,i2,iEnergy]
    //   and i2 is the index of the C=O end of the bond
    //   if iEnergy is < -500 and -1 - (that number) if iEnergy is >= -500

    //   This part is the same as the Rasmol hydrogen bond calculation
    //

    int[][][][] min = AU.newInt4(bioPolymerCount);
    for (int i = 0; i < bioPolymerCount; i++) {
      if (!(bioPolymers[i] instanceof AminoPolymer))
        continue;
      int n = bioPolymers[i].monomerCount;
      min[i] = new int[n][2][3];
      for (int j = 0; j < n; ++j) {
        min[i][j][0][1] = min[i][j][1][1] = Integer.MIN_VALUE;
        min[i][j][0][2] = min[i][j][1][2] = 0;
      }
    }

    for (int i = 0; i < bioPolymerCount; i++)
      if (min[i] != null)
        for (int j = 0; j < bioPolymerCount; j++)
          if (min[j] != null)
            bioPolymers[i].calcRasmolHydrogenBonds(bioPolymers[j], null, null,
                null, 2, min[i], false, dsspIgnoreHydrogens);

    return min;
  }

  /**
   * (p. 2581):
   * 
   * Two nonoverlapping stretches of three residues each, i-1,i,i+1 and
   * j-1,j,j+1, form either a parallel or antiparallel bridge, depending on
   * which of two basic patterns (Fig. 2) is matched. We assign a bridge between
   * residues i and j if there are two H bonds characteristic of beta-structure;
   * in particular:
   * 
   * Parallel Bridge(i,j) =: [Hbond(i-1,j) and Hbond(j,i+1)] or [Hbond(j-1,i)
   * and Hbond(i,j+1)]
   * 
   * Antiparallel Bridge(i,j) =: [Hbond(i,j) and Hbond(j,i)] or [Hbond(i-1,j+1)
   * and Hbond(j-1,i+1)]
   * 
   * @param min
   */
  private void getBridges(int[][][][] min) {
    // ooooooh! It IS possible to have 3 bridges to the same residue. (3A5F) 
    // 
    Atom[] atoms = bioPolymers[0].model.ms.at;
    Bridge bridge = null;

    Map<String, Boolean> htTemp = new Hashtable<String, Boolean>();
    for (int p1 = 0; p1 < min.length; p1++)
      if (bioPolymers[p1] instanceof AminoPolymer) {
        AminoPolymer ap1 = ((AminoPolymer) bioPolymers[p1]);
        int n = min[p1].length - 1;
        for (int a = 1; a < n; a++) {
          int ia = ap1.monomers[a].leadAtomIndex;
          if (bsBad.get(ia))
            continue;
          for (int p2 = p1; p2 < min.length; p2++)
            if (bioPolymers[p2] instanceof AminoPolymer)
              for (int b = (p1 == p2 ? a + 3 : 1); b < min[p2].length - 1; b++) {
                AminoPolymer ap2 = (AminoPolymer) bioPolymers[p2];
                int ib = ap2.monomers[b].leadAtomIndex;
                if (bsBad.get(ib))
                  continue;
                if ((bridge = getBridge(min, p1, a, p2, b, bridgesP, atoms[ia],
                    atoms[ib], ap1, ap2, htTemp, false)) != null) {
                } else if ((bridge = getBridge(min, p1, a, p2, b, bridgesA,
                    atoms[ia], atoms[ib], ap1, ap2, htTemp, true)) != null) {
                  bridge.isAntiparallel = true;
                } else {
                  continue;
                }
                if (Logger.debugging)
                  Logger.debug("Bridge found " + bridge);
                //setDone(bsDone1, bsDone2, ia);
                //setDone(bsDone1, bsDone2, ib);
                done[p1].set(a);
                done[p2].set(b);
                htBridges.put(ia + "-" + ib, bridge);
              }
        }
      }
  }

  private int[][] sheetOffsets = { new int[] { 0, -1, 1, 0, 1, 0, 0, -1 },
      new int[] { 0, 0, 0, 0, 1, -1, 1, -1 } };

  private Bridge getBridge(int[][][][] min, int p1, int a, int p2, int b,
                             Lst<Bridge> bridges, Atom atom1, Atom atom2,
                             AminoPolymer ap1, AminoPolymer ap2,
                             Map<String, Boolean> htTemp,
                             boolean isAntiparallel) {

    int[] b1 = null, b2 = null;
    int ipt = 0;
    int[] offsets = (isAntiparallel ? sheetOffsets[1] : sheetOffsets[0]);
    if ((b1 = isHbonded(a + offsets[0], b + offsets[1], p1, p2, min)) != null
        && (b2 = isHbonded(b + offsets[2], a + offsets[3], p2, p1, min)) != null
        || (b1 = isHbonded(a + offsets[ipt = 4], b + offsets[5], p1, p2, min)) != null
        && (b2 = isHbonded(b + offsets[6], a + offsets[7], p2, p1, min)) != null) {
      Bridge bridge = new Bridge(atom1, atom2, htLadders);
      bridges.addLast(bridge);
      if (vHBonds != null) {
        int type = (isAntiparallel ? Edge.BOND_H_MINUS_3
            : Edge.BOND_H_PLUS_2);
        addHbond(ap1.monomers[a + offsets[ipt]], ap2.monomers[b
            + offsets[++ipt]], b1[2], type, htTemp);
        addHbond(ap2.monomers[b + offsets[++ipt]], ap1.monomers[a
            + offsets[++ipt]], b2[2], type, htTemp);
      }
      return bridge;
    }
    return null;
  }

  private void addHbond(Monomer donor, Monomer acceptor, int iEnergy, int type,
                        Map<String, Boolean> htTemp) {
    Atom nitrogen = ((AminoMonomer) donor).getNitrogenAtom();
    Atom oxygen = ((AminoMonomer) acceptor).getCarbonylOxygenAtom();
    if (htTemp != null) {
      String key = nitrogen.i + " " + oxygen.i;
      if (htTemp.containsKey(key))
        return;
      htTemp.put(key, Boolean.TRUE);
    }
    vHBonds.addLast(new HBond(nitrogen, oxygen, type, (short) 1, C.INHERIT_ALL,
        iEnergy / 1000f));
  }

  /**
   * 
   * "sheet =: a set of one or more ladders connected by shared residues" (p.
   * 2582)
   * 
   */
  private void getSheetStructures() {

    // check to be sure all bridges are part of bridgeList

    if (bridgesA.size() == 0 && bridgesP.size() == 0)
      return;
    createLadders(bridgesA, true);
    createLadders(bridgesP, false);

    BS bsEEE = new BS();
    BS bsB = new BS();
    for (int[][] ladder : htLadders.keySet()) {
      if (ladder[0][0] == ladder[0][1] && ladder[1][0] == ladder[1][1]) {
        bsB.set(ladder[0][0]);
        bsB.set(ladder[1][0]);
      } else {
        bsEEE.setBits(ladder[0][0], ladder[0][1] + 1);
        bsEEE.setBits(ladder[1][0], ladder[1][1] + 1);
      }
    }
    // add Jmol structures and set sheet labels to "E"

    BS bsSheet = new BS();
    BS bsBridge = new BS();

    for (int i = bioPolymers.length; --i >= 0;) {
      if (!(bioPolymers[i] instanceof AminoPolymer))
        continue;
      bsSheet.clearAll();
      bsBridge.clearAll();
      AminoPolymer ap = (AminoPolymer) bioPolymers[i];
      for (int iStart = 0; iStart < ap.monomerCount;) {
        int index = ap.monomers[iStart].leadAtomIndex;
        if (bsEEE.get(index)) {
          int iEnd = iStart + 1;
          while (iEnd < ap.monomerCount
              && bsEEE.get(ap.monomers[iEnd].leadAtomIndex))
            iEnd++;
          bsSheet.setBits(iStart, iEnd);
          iStart = iEnd;
        } else {
          if (bsB.get(index))
            bsBridge.set(iStart);
          ++iStart;
        }
      }
      if (doReport) {
        setTag(labels[i], bsBridge, 'B');
        setTag(labels[i], bsSheet, 'E');
      }
      if (setStructure) {
        ap.setStructureBS(0, 3, STR.SHEET, bsSheet, false);
      }
      done[i].or(bsSheet);
      done[i].or(bsBridge);
    }
  }
  /**
   * "ladder =: one or more consecutive bridges of identical type" (p. 2582)
   * 
   * "For beta structures, we define explicitly: a bulge-linked ladder consists
   * of two (perfect) ladder or bridges of the same type connected by at most
   * one extra residue on one strand and at most four extra resideus on the
   * other strand.... all residues in bulge-linked ladders are marked "E,"
   * including the extra residues." (p. 2585)
   * 
   * @param bridges
   * @param isAntiparallel
   * 
   */
  private void createLadders(Lst<Bridge> bridges,
                             boolean isAntiparallel) {
    int dir = (isAntiparallel ? -1 : 1);
    int n = bridges.size();
    for (int i = 0; i < n; i++)
      checkBridge(bridges.get(i), isAntiparallel, 1, dir);
    for (int i = 0; i < n; i++)
      checkBulge(bridges.get(i), isAntiparallel, 1);
  }

  /**
   * check to see if another bridge exists offset by n1 and n2 from the two ends
   * of a bridge
   * 
   * @param bridge
   * @param isAntiparallel
   * @param n1
   * @param n2
   * @return TRUE if bridge is part of a ladder
   */
  private boolean checkBridge(Bridge bridge,
                              boolean isAntiparallel, int n1, int n2) {
    Bridge b = htBridges.get(bridge.a.getOffsetResidueAtom("\0", n1) + "-"
        + bridge.b.getOffsetResidueAtom("\0", n2));
    return (b != null && bridge.addBridge(b, htLadders));
  }

  private void checkBulge(Bridge bridge, 
                          boolean isAntiparallel, int dir) {
    int dir1 = (isAntiparallel ? -1 : 1);
    for (int i = 0; i < 3; i++)
      for (int j = (i == 0 ? 1 : 0); j < 6; j++) {
        checkBridge(bridge, isAntiparallel, i * dir, j
            * dir1);
        if (j > i)
          checkBridge(bridge, isAntiparallel, j * dir, i
              * dir1);
      }
  }

  private String dumpSummary(AminoPolymer ap, char[] labels) {
    Atom a = ap.monomers[0].getLeadAtom();
    int id = a.getChainID();
    String prefix = (id == 0 ? "" : a.getChainIDStr() + ":");
    SB sb = new SB();
    char lastChar = '\0';
    char insCode1 = '\0';
    char insCode2 = '\0';
    int firstResno = -1, lastResno = -1;
    int n = ap.monomerCount;
    Monomer[] m = ap.monomers;
    for (int i = 0; i <= n; i++) {
      if (i == n || labels[i] != lastChar) {
        if (lastChar != '\0')
          sb.appendC('\n').appendC(lastChar).append(" : ").append(prefix)
              .appendI(firstResno).append(
                  insCode1 == '\0' ? "" : String.valueOf(insCode1)).append("_")
              .append(prefix).appendI(lastResno).append(
                  insCode2 == '\0' ? "" : String.valueOf(insCode2));
        if (i == n)
          break;
        lastChar = labels[i];
        firstResno = m[i].getResno();
        insCode1 = m[i].getInsertionCode();
      }
      lastResno = m[i].getResno();
      insCode2 = m[i].getInsertionCode();
    }
    return sb.toString();
  }

  private String dumpTags(AminoPolymer ap, String lines, BS bsBad, int mode) {
    String prefix = ap.monomers[0].getLeadAtom().getChainID() + "."
        + (ap.bioPolymerIndexInModel + 1);
    lines = PT.rep(lines, "$", prefix);
    int iFirst = ap.monomers[0].getResno();
    String pre = "\n" + prefix;
    SB sb = new SB();
    SB sb0 = new SB().append(pre + ".8: ");
    SB sb1 = new SB().append(pre + ".7: ");
    SB sb2 = new SB().append(pre + ".6: ");
    SB sb3 = new SB().append(pre + ".0: ");
    int i = iFirst;
    int n = ap.monomerCount;
    for (int ii = 0; ii < n; ii++) {
      i = ap.monomers[ii].getResno();
      sb0.append(i % 100 == 0 ? "" + ((i / 100) % 100) : " ");
      sb1.append(i % 10 == 0 ? "" + ((i / 10) % 10) : " ");
      sb2.appendI(i % 10);
      sb3.appendC(bsBad.get(ap.monomers[ii].leadAtomIndex) ? '!'
          : ap.monomers[ii].getGroup1());
    }
    if ((mode & 1) == 1)
      sb.appendSB(sb0).appendSB(sb1).appendSB(sb2);
    sb.append("\n");
    sb.append(lines);
    if ((mode & 2) == 2) {
      sb.appendSB(sb3);
      sb.append("\n\n");
    }
    return sb.toString().replace('\0', '.');
  }

  private int[] isHbonded(int indexDonor, int indexAcceptor, int pDonor,
                          int pAcceptor, int[][][][] min) {
    if (indexDonor < 0 || indexAcceptor < 0)
      return null;
    int[][][] min1 = min[pDonor];
    int[][][] min2 = min[pAcceptor];
    if (indexDonor >= min1.length || indexAcceptor >= min2.length)
      return null;
    return (min1[indexDonor][0][0] == pAcceptor
        && min1[indexDonor][0][1] == indexAcceptor ? min1[indexDonor][0]
        : min1[indexDonor][1][0] == pAcceptor
            && min1[indexDonor][1][1] == indexAcceptor ? min1[indexDonor][1]
            : null);
  }

/**
   * (p. 2581):
   * 
   * A basic turn pattern (Fig. 2) is a single H bond of type (i,i+n). We
   * assign an n-turn at residue i if there is an H bond from CO(i) to NH(i+n)....
   *   When the pattern is found, the ends of the H bond are indicated using ">" at i
   * and "<" at i+n...; the residues bracketed by the H bond are noted "3," "4," or "5"
   * unless they are also end points of other H bonds. Coincidence of ">" and "<" at
   * one residue is indicated by "X." ... Residues bracketed by the hydrogen bond
   * are marked "T," unless they are part of an n-helix (defined below). 
   * 
   * (p. 2582):
   * 
   * A minimal helix is defined by two consecutive n-turns.... Longer helices are 
   * defined as overlaps of minimal helices.... Residues bracketed by H bonds are 
   * labeled G, H, I.... Long helices can deviate from regularity in that not all 
   * possible H bonds are formed. This possibility is implicit in the above helix 
   * definition.
   * @param min
   * @param iPolymer
   * @return             string label
   */
  private String findHelixes(int iPolymer, int[][][] min) {
    AminoPolymer ap = (AminoPolymer) bioPolymers[iPolymer];
    if (Logger.debugging)
      for (int j = 0; j < ap.monomerCount; j++)
        Logger.debug(iPolymer + "." + ap.monomers[j].getResno() + "\t"
            + Escape.e(min[j]));

    BS bsTurn = new BS();

    String line3, line4, line5;

    if (isDSSP2) {
      line5 = findHelixes2(0, iPolymer, 5, min, STR.HELIXPI,
          Edge.BOND_H_PLUS_5, bsTurn, true);
      line4 = findHelixes2(2, iPolymer, 4, min, STR.HELIXALPHA,
          Edge.BOND_H_PLUS_4, bsTurn, false);
      line3 = findHelixes2(4, iPolymer, 3, min, STR.HELIX310,
          Edge.BOND_H_PLUS_3, bsTurn, false);

    } else {
      line4 = findHelixes2(2, iPolymer, 4, min, STR.HELIXALPHA,
          Edge.BOND_H_PLUS_4, bsTurn, true);
      line3 = findHelixes2(4, iPolymer, 3, min, STR.HELIX310,
          Edge.BOND_H_PLUS_3, bsTurn, false);
      line5 = findHelixes2(0, iPolymer, 5, min, STR.HELIXPI,
          Edge.BOND_H_PLUS_5, bsTurn, false);
    }

    //   String line5 = findHelixes2(iPolymer, 5, min, STR.HELIXPI,
    //       Edge.BOND_H_PLUS_5, bsTurn);

    // G, H, and I have been set; now set what is left over as turn

    if (setStructure)
      ap.setStructureBS(0, 6, STR.TURN, bsTurn, false);

    if (doReport) {
      setTag(labels[iPolymer], bsTurn, 'T');
      return dumpTags(ap, "$.5: " + line5 + "\n" + "$.4: " + line4 + "\n"
          + "$.3: " + line3, bsBad, 1);
    }

    return "";
  }

  private String findHelixes2(int mmtfType, int iPolymer, int pitch, int[][][] min,
                              STR subtype, int type,
                              BS bsTurn, boolean isFirst) {

    // The idea here is to run down the polymer setting bit sets
    // that identify start, stop, N, and X codes: >, <, 3, 4, 5, and X
    // In addition, we create a bit set that will identify G H or I.

    AminoPolymer ap = (AminoPolymer) bioPolymers[iPolymer];
    BS bsStart = new BS();
    BS bsNNN = new BS();
    BS bsX = new BS();
    BS bsStop = new BS();
    BS bsHelix = new BS();
    BS bsDone = done[iPolymer];

    String warning = "";

    // index is to the NH (higher index) end, not the C=O end

    int n = ap.monomerCount;
    for (int i = pitch; i < n; ++i) {
      int i0 = i - pitch;
      int bpt = 0;
      if (min[i][0][0] == iPolymer && min[i][0][1] == i0
          || min[i][bpt = 1][0] == iPolymer && min[i][1][1] == i0) {

        // the basic indicators are >33< or >444< or >5555<

        // we use bit sets here for efficiency

        int ia = ap.monomers[i0].leadAtomIndex;
        int ipt = bsBad.nextSetBit(ia);
        Monomer m = ap.monomers[i];
        if (ipt >= ia && ipt <= m.leadAtomIndex)
          continue;

        bsStart.set(i0); //   >
        bsNNN.setBits(i0 + 1, i); //    nnnn
        bsStop.set(i); //        <

        // a run of HHHH or GGG or IIIII is made if: 
        // (1) the previous position was a start for this n-helix, and
        // (2) no position within that run has already been assigned one of BEHGI
        // also look for >< and mark those with an X

        // Note: The DSSP assignment priority is HBEGITS, so H must ignore determination of B or E.
        // This would appear as "H" with a bridge connection in DSSP output.
        // I don't think it's possible. An antiparallel bridge would require connections
        // between NH and CO of the same group or NH and CO of adjacent groups, but
        // they would be in a helix and not oriented at all the correct direction;
        // a parallel bridge would require connections between NH and CO for two 
        // groups separated by a group. This is certainly impossible in an alpha helix.
        // Still, that is what we are implementing here -- just as described -- H with
        // the possibility of a bridge.

        ipt = bsDone.nextSetBit(i0);
        boolean isClear = (ipt < 0 || ipt >= i);
        boolean addH = false;
        if (i0 > 0 && bsStart.get(i0 - 1) && (isFirst || isClear)) {
          bsHelix.setBits(i0, i);
          if (!isClear)
            warning += "  WARNING! Bridge to helix at " + ap.monomers[ipt];
          addH = true;
        } else if (isClear || bsDone.nextClearBit(ipt) < i) {
          addH = true;
        }
        if (bsStop.get(i0))
          bsX.set(i0);
        if (addH && vHBonds != null) {
          addHbond(m, ap.monomers[i0], min[i][bpt][2], type, null);
        }
      }
    }

    char[] taglines;
    if (doReport) {
      taglines = new char[n];
      setTag(taglines, bsNNN, (char) ('0' + pitch)); // 345
      setTag(taglines, bsStart, '>'); // may overwrite n
      setTag(taglines, bsStop, '<'); // may overwrite n or ">"
      setTag(taglines, bsX, 'X'); // may overwrite "<"
    } else {
      taglines = null;
    }

    // update the bit sets based on this type of helix

    bsDone.or(bsHelix); // add HELIX to DONE
    bsNNN.andNot(bsDone); // remove DONE from nnnnn
    bsTurn.or(bsNNN); // add nnnnn to TURN
    bsTurn.andNot(bsHelix); // remove HELIX from TURN

    // create the Jmol helix structures of the given subtype

    if (setStructure)
      ap.setStructureBS(0, mmtfType, subtype, bsHelix, false); // GHI;

    if (doReport) {
      setTag(labels[iPolymer], bsHelix, (char) ('D' + pitch));
      return String.valueOf(taglines) + warning;
    }
    return "";
  }

  private void setTag(char[] tags, BS bs, char ch) {
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
      tags[i] = ch;
  }

}
