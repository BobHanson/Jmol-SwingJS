/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2017  The Jmol Development Team
 * 
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.symmetry;

import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.PT;

import org.jmol.util.Elements;
import org.jmol.util.Logger;
import org.jmol.util.SimpleEdge;
import org.jmol.util.SimpleNode;
import org.jmol.viewer.JC;

/**
 * A fully validated relatively efficient implementation of Cahn-Ingold-Prelog
 * rules for assigning R/S, M/P, and E/Z stereochemical descriptors. Based on
 * IUPAC Blue Book rules of 2013 and assorted corrections.
 * 
 * IUPAC Project: Corrections, Revisions and Extension for the Nomenclature of
 * Organic Chemistry - IUPAC Recommendations and Preferred Names 2013 (the IUPAC
 * Blue Book)
 * https://iupac.org/projects/project-details/?project_nr=2001-043-1-800
 * http://www.sbcs.qmul.ac.uk/iupac/bibliog/BBerrors.html
 * 
 * Settable options:
 * 
 * set testflag1    use advanced in/out-sensitive Rule 6 (r,r-bicyclo[2.2.2]octane)
 * set testflag2    turn off tracking (saving of _M.CIPInfo) for speed 
 * 
 * Features include:
 * 
 * - deeply validated
 * 
 * - includes revised Rules 1b, and 2
 * 
 * - includes a proposed Rule 6
 * 
 * - implemented in Java (Jmol) and JavaScript (JSmol)
 * 
 * - only a few Java classes; < 1000 lines
 * 
 * - efficient, one-pass process for each center using a single finite digraph
 * for all auxiliary descriptors
 * 
 * - exhaustive processing of all 9 sequence rules (1a, 1b, 2, 3, 4a, 4b, 4c, 5,
 * 6)
 * 
 * - includes R/S, r/s, M/P (axial, not planar), E/Z
 * 
 * - covers any-length odd and even cumulenes
 * 
 * - uses Jmol conformational SMARTS to detect atropisomers and helicenes
 * 
 * - covers chiral phosphorus and sulfur, including trigonal pyramidal and
 * tetrahedral
 * 
 * - properly treats complex combinations of R/S, M/P, and seqCis/seqTrans
 * centers (Rule 4b)
 * 
 * - properly treats neutral-species resonance structures using fractional
 * atomic mass and a modified Rule 1b
 * 
 * - implements CIP spiro rule (BB P-93.5.3.1) as part of Rule 6
 * 
 * - detects small rings (fewer than 8 members) and removes E/Z specifications
 * for such
 * 
 * - detects chiral bridgehead nitrogens and E/Z imines and diazines
 * 
 * - reports atom descriptor along with the rule that ultimately decided it
 * 
 * - fills _M.CIPInfo with detailed information about how each ligand was decided
 *   (feature turned off by set testflag2)
 * 
 * - generates advanced Rule 6 descriptors for cubane and the like. (Generally 'r')
 *   using set testflag1
 * 
 * Primary 236-compound Chapter-9 validation set (AY-236) provided by Andrey
 * Yerin, ACD/Labs (Moscow).
 * 
 * Mikko Vainio also supplied a 64-compound testing suite (MV-64), which is
 * available on SourceForge in the Jmol-datafiles directory.
 * (https://sourceforge.net/p/jmol/code/HEAD/tree/trunk/Jmol-datafiles/cip).
 * 
 * Additional test structures provided by John Mayfield.
 * 
 * Additional thanks to the IUPAC Blue Book Revision project, specifically
 * Karl-Heinz Hellwich for alerting me to the errata page for the 2013 IUPAC
 * specs (http://www.chem.qmul.ac.uk/iupac/bibliog/BBerrors.html), Gerry Moss
 * for discussions, Andrey Yerin for discussion and digraph checking.
 * 
 * Many thanks to the members of the BlueObelisk-Discuss group, particularly
 * Mikko Vainio, John Mayfield (aka John May), Wolf Ihlenfeldt, and Egon
 * Willighagen, for encouragement, examples, serious skepticism, and extremely
 * helpful advice.
 * 
 * References:
 * 
 * CIP(1966) R.S. Cahn, C. Ingold, V. Prelog, Specification of Molecular
 * Chirality, Angew.Chem. Internat. Edit. 5, 385ff
 * 
 * Custer(1986) Roland H. Custer, Mathematical Statements About the Revised
 * CIP-System, MATCH, 21, 1986, 3-31
 * http://match.pmf.kg.ac.rs/electronic_versions/Match21/match21_3-31.pdf
 * 
 * Mata(1993) Paulina Mata, Ana M. Lobo, Chris Marshall, A.Peter Johnson The CIP
 * sequence rules: Analysis and proposal for a revision, Tetrahedron: Asymmetry,
 * Volume 4, Issue 4, April 1993, Pages 657-668
 * 
 * Mata(1994) Paulina Mata, Ana M. Lobo, Chris Marshall, and A. Peter Johnson,
 * Implementation of the Cahn-Ingold-Prelog System for Stereochemical Perception
 * in the LHASA Program, J. Chem. Inf. Comput. Sci. 1994, 34, 491-504 491
 * http://pubs.acs.org/doi/abs/10.1021/ci00019a004
 * 
 * Mata(2005) Paulina Mata, Ana M. Lobo, The Cahn, Ingold and Prelog System:
 * eliminating ambiguity in the comparison of diastereomorphic and
 * enantiomorphic ligands, Tetrahedron: Asymmetry, Volume 16, Issue 13, 4 July
 * 2005, Pages 2215-2223
 * 
 * Favre(2013) Henri A Favre, Warren H Powell, Nomenclature of Organic Chemistry
 * : IUPAC Recommendations and Preferred Names 2013 DOI:10.1039/9781849733069
 * http://pubs.rsc.org/en/content/ebook/9780854041824#!divbookcontent
 * 
 * code history:
 *
 * 5/12/18 Jmol 14.29.14 fixes minor Rule 5 bug and adds advanced Rule 6 in/out testflag1 option (857 lines)  
 *  
 * 5/1/18  Jmol 14.29.14 fixes enantiomorphic Rule 5 R/S check for BH64_85 and BH64_86
 * 
 * 4/25/18 Jmol 14.29.14 fixes spiroallene Rule 6 issue for BH64_84
 * 
 * 4/23/18 Jmol 14.29.14 fixes Rule 2 for JM_008, involving mass and duplicates (824 lines)
 * 
 * 4/11/18 Jmol 14.29.13 adds optional CIPDataTracker class (822 lines)
 * 
 * 4/2/18 Jmol 14.29.13 adds optional CIPDataSmiles class
 * 
 * 4/2/18 Jmol 14.29.13 adds John's "mancude-like" cyclic conjugated ene Kekule
 * averaging
 * 
 * 12/10/17 Jmol 14.29.9 adds CIPData, mancude Kekule averaging
 * 
 * 11/11/17 Jmol 14.25.1 adds "duplicate over terminal" in Rule 1b; streamlined
 * (777 lines)
 * 
 * 11/05/17 Jmol 14.24.1 fixes a problem with seqCis/seqTrans and also with Rule
 * 2 (799 lines)
 * 
 * 10/17/17 Jmol 14.20.10 adds S4 check in Rule 6 and also fixes bug in aux
 * descriptors being skipped when two ligands are equivalent for the root (798
 * lines)
 * 
 * 9/19/17 CIPChirality code simplification (778 lines)
 * 
 * 9/14/17 Jmol 14.20.6 switching to Mikko's idea for Rule 4b and 5. Abandons
 * "thread" idea. Uses breadth-first algorithm for generating bitsets for R and
 * S. Processing time reduced by 50%. Still could be optimized some. (820 lines)
 * 
 * 7/25/17 Jmol 14.20.4 consolidates all ene determinations; moves auxiliary
 * descriptor generation to prior to Rule 3 (850 lines) 7/23/17 Jmol 14.20.4
 * adds Rule 6; rewrite/consolidate spiro, C3, double spiran code (853 lines)
 * 
 * 7/19/17 Jmol 14.20.3 fixing Rule 2 (880 lines) 7/13/17 Jmol 14.20.3 more
 * thorough spiro testing (858 lines) 7/10/17 Jmol 14.20.2 adding check for C3
 * and double spiran (CIP 1966 #32 and #33) 7/8/17 Jmol 14.20.2 adding presort
 * for Rules 4a and 4c (test12.mol; 828 lines)
 * 
 * 7/7/17 Jmol 14.20.1 minor coding efficiencies (833 lines)
 * 
 * 7/6/17 Jmol 14.20.1 major rewrite to correct and simplify logic; full
 * validation for 433 structures (many duplicates) in AY236, BH64, MV64, MV116,
 * JM, and L (836 lines)
 * 
 * 6/30/17 Jmol 14.20.1 major rewrite of Rule 4b (999 lines)
 * 
 * 6/25/17 Jmol 14.19.1 minor fixes for Rule 4b and 5 for BH64_012-015; better
 * atropisomer check
 * 
 * 6/12/2017 Jmol 14.18.2 tested for Rule 1b sphere (AY236.53, 163, 173, 192);
 * 957 lines
 * 
 * 6/8/2017 Jmol 14.18.2 removed unnecessary presort for Rule 1b
 * 
 * 5/27/17 Jmol 14.17.2 fully interfaced using SimpleNode and SimpleEdge
 * 
 * 5/27/17 Jmol 14.17.1 fully validated; simplified code; 978 lines
 * 
 * 5/17/17 Jmol 14.16.1. adds helicene M/P chirality; 959 lines validated using
 * CCDC structures HEXHEL02 HEXHEL03 HEXHEL04 ODAGOS ODAHAF
 * http://pubs.rsc.org/en/content/articlehtml/2017/CP/C6CP07552E
 * 
 * 5/14/17 Jmol 14.15.5. trimmed up and documented; no need for lone pairs; 948
 * lines
 * 
 * 5/13/17 Jmol 14.15.4. algorithm simplified; validated for mixed Rule 4b
 * systems involving auxiliary R/S, M/P, and seqCis/seqTrans; 959 lines
 * 
 * 5/06/17 validated for 236 compound set AY-236.
 * 
 * 5/02/17 validated for 161 compounds, including M/P, m/p (axial chirality for
 * biaryls and odd-number cumulenes)
 * 
 * 4/29/17 validated for 160 compounds, including M/P, m/p (axial chirality for
 * biaryls and odd-number cumulenes)
 * 
 * 4/28/17 Validated for 146 compounds, including imines and diazines, sulfur,
 * phosphorus
 * 
 * 4/27/17 Rules 3-5 preliminary version 14.15.1
 * 
 * 4/6/17 Introduced in Jmol 14.12.0; validated for Rules 1 and 2 in Jmol
 * 14.13.2; 100 lines
 * 
 * 
 * NOTE! NOTE! NOTE! NOTE! NOTE! NOTE! NOTE! NOTE! NOTE! NOTE! NOTE! NOTE! NOTE!
 * 
 * Added logic to Rule 1b:
 * 
 * Rule 1b: In comparing duplicate atoms, the one with lower root distance has
 * precedence, where root distance is defined as: (a) in the case of
 * ring-closure duplicates, the sphere of the duplicated atom; and (b) in the
 * case of multiple-bond duplicates, the sphere of the atom to which the
 * duplicate atom is attached.
 * 
 * Rationale: Using only the distance of the duplicated atom (current
 * definition) introduces a Kekule bias, which can be illustrated with various
 * simple models. By moving that distance to be the sphere of the parent atom of
 * the duplicate, the problem is resolved.
 * 
 * Added clarification to Rule 2:
 * 
 * Rule 2: Higher mass precedes lower mass, where mass is defined in the case of
 * nonduplicate atoms with identified isotopes for elements as their exact
 * isotopic mass and, in all other cases, as their element's atomic weight.
 * 
 * Rationale: BB is not self-consistent, including both "mass number" (in the
 * rule) and "atomic mass" in the description, where "79Br < Br < 81Br". And
 * again we have the same Kekule-ambiguous issue as in Rule 1b. The added
 * clarification fixes the Kekule issue (not using isotope mass number for
 * duplicate atoms), solves the problem that F < 19F (though 100% nat.
 * abundance), and is easily programmable.
 * 
 * In Jmol the logic is very simple, actually using the isotope mass number, but
 * doing two checks:
 * 
 * a) if one of four specific isotopes (16O, 52Cr, 96Mo, 175Lu), reverse the test, and
 * 
 * b) if on the list of 100% natural isotopes or one of the non-natural
 * elements, use the element's accepted atomic weight.
 * 
 * See CIPAtom.getMass();
 * 
 * PROPOSED Rule 6: An undifferentiated reference node has priority over any
 * other undifferentiated node.
 * 
 * Rationale: This rule is stated in CIP(1966) p. 357.
 * 
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */
public class CIPChirality {

  //The rules:
  //  
  //  P-92.1.3.1 Sequence Rule 1 has two parts:
  //
  //    (a) Higher atomic number precedes lower;
  //    (b) A duplicate atom node whose corresponding nonduplicated atom
  //    node is the root or is closer to the root ranks higher than a
  //    duplicate atom node whose corresponding nonduplicated node is 
  //    farther from the root.
  //
  //  P-92.1.3.2 Sequence Rule 2
  //
  //    Higher atomic mass number precedes lower;
  //
  //  P-92.1.3.3 Sequence Rule 3
  //
  //    When considering double bonds and planar tetraligand atoms, 'seqcis' = 'Z'
  //    precedes 'seqtrans' = 'E' and this precedes nonstereogenic double bonds.
  //
  //  P-92.1.3.4 Sequence Rule 4 is best considered in three parts:
  //
  //    (a) Chiral stereogenic units precede pseudoasymmetric stereogenic units and
  //        these precede nonstereogenic units.
  //    (b) When two ligands have different descriptor pairs, then the one with the
  //        first chosen like descriptor pairs has priority over the one with a
  //        corresponding unlike descriptor pair.
  //      (i) Like descriptor pairs are: 'RR', 'SS', 'MM', 'PP', 'RM', 'SP',
  //          'seqCis/seqCis', 'seqTran/sseqTrans', 'RseqCis',
  //          'SseqTrans', 'MseqCis', 'PseqTrans' ...;
  //      (ii) Unlike discriptor pairs are 'RS', 'MP', 'RP', 'SM',
  //           'seqCis/seqTrans', 'RseqTrans', 'SseqCis', 'PseqCis',
  //           'MseqTrans'....
  //    (c) 'r' precedes 's' and 'm' precedes 'p'
  //
  //  P-92.1.3.5 Sequence Rule 5
  //
  //    An atom or group with descriptor 'R', 'M' and 'seqCis' has priority over its
  //    enantiomorph 'S', 'P' or 'seqTrans', 'seqCis' or 'seqTrans'.

  //
  // Rule 1b proposal
  //

  // Rule 1b: In comparing duplicate atoms, the one with lower root distance has 
  // precedence, where root distance is defined as: (a) in the case of ring-closure 
  // duplicates, the sphere of the duplicated atom; and (b) in the case of 
  // multiple-bond duplicates, the sphere of the atom to which the duplicate atom 
  // is attached.

  // [0]---1---2---3==4 
  //   
  // 
  // current:
  // 
  //                (4)(3)
  //                /  /
  // [0]---1---2---3--4 
  //
  // 
  // proposed:
  // 
  //                (3)(4)
  //                /  /  
  // [0]---1---2---3--4   
  //
  // 
  //               7--6                        7==6
  //              //   \                      /    \
  //              8     5                    8      5  
  //           (ri-ng) /                  (ri=ng) //
  // [0]---1---2---3==4        [0]---1---2---3---4
  //   
  // 
  // current:
  // 
  //                (4)(3)                    (?)(5)
  //                /  /                      /  /
  // [0]---1---2---3--4        [0]---1---2---3--4
  //
  // 
  // proposed:
  // 
  //                (3)(4)                    (3)(4)
  //                /  /                      /  /
  // [0]---1---2---3--4        [0]---1---2---3--4
  // 

  //
  // Implementation Notes
  //
  //

  // "Scoring" a vs. b involves returning 0 (TIE) or +/-n, where n>0 indicates b won, n < 0
  // indicates a won, and the |n| indicates in which sphere the decision was made.
  // 
  // This implementation is somewhat inefficient. It digs deep, looking for a first difference
  // but only caches the sphere so that in the end the lowest sphere change is chosen. 
  // I don't think it has to do this. A breadth-first queue should work better. I just
  // did not take the time to implement that.
  //
  // The basic strategy is to loop through all nine sequence rules (1a, 1b, 2, 3, 4a, 4b, 4c, 5, and 6) 
  // in order and exhaustively prior to applying the next rule:
  //
  // Rule 1a (atomic number -- note that this requires an aromaticity check first)
  // Rule 1b (duplicated atom progenitor root-distance check; revised as described above
  //         for aromatic systems.
  // Rule 2  (nominal isotopic mass)
  // Rule 3  (E/Z, not including enantiomorphic seqCis/seqTrans designations)
  // Rule 4a (chiral precedes pseudochiral precedes nonstereochemical)
  // Rule 4b (like precedes unlike)
  // Rule 4c (r precedes s)
  // Rule 5  (R precedes S; M precedes P; C precedes T)
  // Rule 6  (promoted undifferentiated ligand precedes unpromoted undifferentiated ligand)
  //
  // Some nuances I've learned along the way here, some of which are still being checked:
  //
  // 1. Rule 1a Had to be revised to account for Kekule bias using averaging. 
  // 2. Rule 1b Had to be revised to account for Kekule bias (AY-236.215). Note that this 
  //            rule may only be applied AFTER Rule 1a has been applied exhaustively. In  
  //            my mind it deserves its own number for this reason. See AY-236.53, 
  //            (1S,5R)-bicyclo[3.1.0]hex-2-ene, for example.
  // 3. Rule 2  This rule is simple to implement; must be executed only for ties from 1a and 1b.
  //            However, it still needs Kekule consideration.
  // 4. Rule 3  Requires the concept of "auxiliary" (temporary, digraph-specific) descriptors.
  //            This involves the initial generation of all auxiliary descriptors, including r/s and R/S at
  //            branching points. In the course of doing this, all rules, 1-6, must be employed
  //            at these auxiliary centers using the already-created digraph.   
  //            Auxiliary descriptors must be generated in a depth-first manner -- that is,
  //            from highest sphere to lowest within a branch. This is the key to not having 
  //            an analysis blow up or somehow require complex, impossible iteration.
  // 5. Rule 4a needs to be addressed exhaustively prior to Rules 4b and 4c. This rule serves to
  //            avoid the need for Rule 4b for all except the most unusual cases, where, for example,
  //            there are two otherwise identical branches, but one can be identified as S and the
  //            other only r or no-stereo, but not R. Thus, only branches that end up as R/R, R/S, S/S,
  //            r/r, r/s, s/s, or ns/ns comparisons need be investigated by Rule 4b.  
  // 6. Rule 4b This rule filters out all diastereomorphic differences that do not involve r/s issues.
  //            Somehow missed in the discussion is that the reference descriptor is determined
  //            once and only once for each branch from the center under scrutiny. The key is to 
  //            determine two "Mata sequence" like/unlike lists of R and S descriptors, one for each 
  //            pair of branches being considered. This same reference carries through all future 
  //            iterations of the algorithm for that branch.
  // 7. Rule 4c Again, this subrule must be invoked only after Rule 4b is completed. 
  //            It is implemented as for Rule 4a.
  // 8. Rule 5  Setting of pseudoasymmetry (r/s, m/p) and checking for special cases requiring R/S
  //            is done along the same line as Rule 4b, but in this case by setting the reference 
  //            descriptor to "R" for both sequences. The S-reference must also be checked to see 
  //            if these happen to be self-enantiomorphic ligands. Note that if any changes have 
  //            been made by Rule 4c, all descriptors must be recalculated; otherwise the ones
  //            used in Rule 4b can be reused.
  // 9. Rule 6  This new rule takes care of all spiro and high-symmetry cases. An optional  
  //            "full" version (set testflag1 TRUE) assigns all 'r' to bridgehead carbons of 
  //            bicyclo[2.2.2]octane, adamantane, tetrahedrane, cubane, and like-topology compounds, 
  //            and all 's' if half of the stereocenters are pointing "in" instead of "out". 

  /**
   * A useful idea is to switch from a tree metaphor to a "twisted strand" or
   * "thread" metaphor. For example:
   * 
   * (a) In Rule 1b, all ring-duplicates terminate on one of the nodes in the
   * sequence of parent nodes going back to the root. This has nothing to do
   * with branching.
   * 
   * (b) Generation of auxiliary descriptors prior to implementation of Rule 3
   * must start from the highest sphere, proceeding toward the root. In this
   * process the path leading back to the root will have no stereodescriptors,
   * but that does not matter, as its priority is guaranteed to be set by Rule
   * 1a.
   * 
   * (c) All rule 4b auxiliary nodes can be normalized by labeling them one of R
   * or S; there is no need to consider them to be C/T (seqCis or seqTrans) or
   * M/P, other than to immediately equate that to R/S. Note that C/T and M/P
   * must be assigned to the sp2 node closest to the root.
   * 
   * (d) Rule 4b comparisons can be analyzed using a straightforward ranking
   * process as follows:
   * 
   * (d1) Run through all nodes of a ligand branch, checking for descriptors
   * (cipAtom.rule4Type). Save a list of ASCII strings that represent the
   * priority of the chiral nodes relative to the given reference. Do this
   * twice, first using R and second using S. The priority character takes the
   * form of:
   * 
   * (char)(64 + (a.cipPriority<<2) + (a.rule4Type == 0 ? 0 : a.rule4Type ==
   * root.rule4Ref ? 1 : 2))
   * 
   * Such that, for example, "@D@@@A" indicates a node in Sphere 5 with
   * priorities 010000 and a "like" relationship to the reference. In the end,
   * we might have something like this:
   * 
   * "@@@A"
   * 
   * "@@@A@@@A"
   * 
   * "@@@A@@@B"
   * 
   * indicating "AAB" -- "llu" (reading off only the last character for each
   * thread).
   * 
   * (d2) Compare these four lists of data. Sort these lists numerically. The
   * ligand associated with the highest numerical value is preferred. In Jmol,
   * this comparison is simply an XOR of pairs of bitsets. The bitset that has
   * the lowest set bit of the XOR is the winner. Simple as that.
   * 
   * The "like/unlike" business can be thought of as a simple diastereomorphic
   * test. Thus, the lists are tested for the first point at which they become
   * neither identical nor opposites, and only that point need be checked for
   * likeness or unlikeness to the reference. I like thinking about it this way
   * because it focuses on what Rule 4b's role is -- the identification of
   * diastereomorphism.
   * 
   * (e) Rule 4c takes care of diasteriomorphism related to enantiomorphic (r/s,
   * m/p) sub-paths. It is analyzed exactly as for Rule 4a.
   * 
   * (f) Rule 5 processing is a comparison similar to Rule 4b, where only the
   * reference descriptor "R" is used. It tkes care of any remaining
   * enantiomorphic issues, including the possibilty that some even-number
   * combination of a combination of enantiomorphic pairs and self-enantiomeric
   * ligands are present.
   * 
   * (g) Rule 6 tests for high-symmetry cases, including cubane, adamantane,
   * bicyclo[2.2.2]octane, etc. If auxiliary centers are recalculated (set
   * testflag1 true), then these simple "all-out" isomers will be given all-r
   * descriptors.
   * 
   * 
   */

  // The algorithm:
  // 
  //
  //  getChirality(molecule) {
  //    prefilterAtoms()
  //    checkForAlkenes()
  //    checkForSmallRings()
  //    checkForHelicenes()
  //    checkForBridgeheadNitrogens()
  //    checkForKekuleIssues()
  //    checkForAtropisomerism()
  //    for(all filtered atoms) getAtomChirality(atom)
  //    if (haveAlkenes) {
  //      for(all double bonds) getBondChirality(a1, a2)
  //      removeUnnecessaryEZDesignations()
  //      indicateHeliceneChirality()
  //    }
  //  }
  //
  // getAtomChirality(atom) {
  //   for (each Rule){  
  //     sortSubstituents() 
  //     if (done) return checkHandedness();
  //   }
  //   return NO_CHIRALITY
  // }
  // 
  //  getBondChirality(a1, a2) {
  //    atop = getAlkeneEndTopPriority(a1)
  //    btop = getAlkeneEndTopPriority(a2)
  //    return (atop >= 0 && btop >= 0 ? getEneChirality(atop, a1, a2, btop) : NO_CHIRALITY)
  //  }
  //
  // sortSubstituents() {
  //   for (all pairs of substituents a1 and a2) {
  //     score = a1.compareTo(a2, currentRule)
  //     if (score == TIED) 
  //       score = breakTie(a1,a2)
  // }
  // 
  // breakTie(a,b) { 
  //    score = compareShallowly(a, b)
  //    if (score != TIED) return score
  //    a.sortSubstituents(), b.sortSubstituents()
  //    return compareDeeply(a, b)
  // }
  // 
  // compareShallowly(a, b) {
  //    for (each substituent pairing i in a and b) {
  //      score = applyCurrentRule(a_i, b_i)
  //      if (score != TIED) return score
  //    }
  //    return TIED
  // }
  //
  // compareDeeply(a, b) {
  //    bestScore = Integer.MAX_VALUE
  //    for (each substituent pairing i in a and b) {
  //      bestScore = min(bestScore, breakTie(a_i, b_i))
  //    }
  //    return bestScore
  // }
  //
  // Of course, the actual code is a bit more complex than that.
  //
  // approximate line count calculated using MSDOS batch script:
  //
  //  copy ..\..\workspace\jmol\src\org\jmol\symmetry\CIPChirality.java+..\..\workspace\jmol\src\org\jmol\symmetry\CIPData.java tt
  //  type tt | find /V "}"|find /V "//" |find /V "import" | find /V /I "track" | find /V "*" |find ";"|find /V "Logger"|find /V "System.out" |find /V "TEST" > t
  //  type tt | find /V "}"|find /V "//" |find /V "import" | find /V /I "track" | find /V "*" |find "if"|find /V "Logger"|find /V "System.out" |find /V "TEST" >> t
  //  type tt | find "COUNT_LINE"  >> t
  //  type t |find " " /C
  
  /**
   * These elements have 100% natural abundance; we will use their isotope mass number instead of their actual average mass, since there is no difference 
   */
  static final String RULE_2_nXX_EQ_XX = ";9Be;19F;23Na;27Al;31P;45Sc;55Mn;59Co;75As;89Y;93Nb;98Tc;103Rh;127I;133Cs;141Pr;145Pm;159Tb;165Ho;169Tm;197Au;209Bi;209Po;210At;222Rn;223Fr;226Ra;227Ac;231Pa;232Th;and all > U (atomno > 92)";
  
  /**
   * These elements have an isotope number that is a bit higher than the average
   * mass, even though their actual isotope mass is a bit lower. We will change
   * 16 to 15.9, 52 to 51.9, 96 to 95.9, 175 to 174.9 so as to force the unspecified
   * mass atom to be higher priority than the specified one. 
   * 
   * All other isotopes can use their integer isotope mass number instead of looking up
   * their exact isotope mass.
   * 
   */
  static final String RULE_2_REDUCE_ISOTOPE_MASS_NUMBER = ";16O;52Cr;96Mo;175Lu;";  

  static final int NO_CHIRALITY = 0, TIED = 0;
  static final int A_WINS = -1, B_WINS = 1;

  static final int IGNORE = Integer.MIN_VALUE;

  static final int UNDETERMINED = -1;

  static final int STEREO_R = JC.CIP_CHIRALITY_R_FLAG,
      STEREO_S = JC.CIP_CHIRALITY_S_FLAG;
  
  static final int STEREO_M = JC.CIP_CHIRALITY_M_FLAG,
      STEREO_P = JC.CIP_CHIRALITY_P_FLAG;
  
  static final int STEREO_Z = JC.CIP_CHIRALITY_seqcis_FLAG,
      STEREO_E = JC.CIP_CHIRALITY_seqtrans_FLAG;

  static final int STEREO_BOTH_RS = STEREO_R | STEREO_S; // must be the number 3
  
  static final int STEREO_BOTH_EZ = STEREO_E | STEREO_Z;

  static final int RULE_1a = 1, RULE_1b = 2, RULE_2 = 3, RULE_3 = 4,
      RULE_4a = 5, RULE_4b = 6, RULE_4c = 7, RULE_5 = 8, RULE_6 = 9;

  final static String[] ruleNames = { "", "1a", "1b", "2", "3", "4a", "4b",
      "4c", "5", "6" }; // Logger only

  public String getRuleName(int rule) { // Logger only
    return ruleNames[rule]; // Logger only
  }

  /**
   * maximum path to display for debugging only using SET DEBUG in Jmol
   */
  static final int MAX_PATH = 50;  // Logger

  /**
   * maximum ring size that can have a double bond with no E/Z designation; also
   * used for identifying aromatic rings and bridgehead nitrogens
   */
  static final int SMALL_RING_MAX = 7;

  /**
   * the current rule being applied exhaustively
   * 
   */
  int currentRule = RULE_1a;

  /**
   * The atom for which we are determining the stereochemistry
   * 
   */
  CIPAtom root;

  /**
   * collected bitsets and more specialized SMILES/SMARTS searches and vwr references
   * 
   */
  CIPData data;

  /**
   * are we tracking pathways for _M.CIPInfo?
   * 
   */
  boolean doTrack;
  
  /**
   * are we in the midst of auxiliary center creation?
   * 
   */
  boolean isAux;

  /**
   * set bits RULE_1a - RULE_6 to indicate a need for that rule based on what is in the model
   */
  BS bsNeedRule = new BS();
  
  /** 
   * do we have r or s and so will need to recalculate Mata like/unlike lists in Rule 5?
   */
  boolean havePseudoAuxiliary;

  /**
   * incremental pointer providing a unique ID to every CIPAtom for debugging
   * 
   */
  int ptIDLogger;

  public CIPChirality() {
    // for reflection
  }

  /**
   * A general determination of chirality that involves ultimately all of Rules
   * 1-6.
   * 
   * @param data
   * 
   */
  public void getChiralityForAtoms(CIPData data) {

    if (data.bsAtoms.cardinality() == 0)
      return;
    
    this.data = data;

    doTrack = data.isTracker();
    
    ptIDLogger = 0;

    // using BSAtoms here because we need the entire graph,
    // including multiple molecular units (AY-236.93

    BS bsToDo = (BS) data.bsMolecule.clone();
    boolean haveAlkenes = preFilterAtomList(data.atoms, bsToDo, data.bsEnes);
    if (!data.bsEnes.isEmpty()) 
      data.getEneKekule();
    Logger.info("bsKekule:" + data.bsKekuleAmbiguous);

    // set atom chiralities

    bsToDo = (BS) data.bsAtoms.clone();
    
    for (int i = bsToDo.nextSetBit(0); i >= 0; i = bsToDo.nextSetBit(i + 1)) {
      SimpleNode a = data.atoms[i];
      a.setCIPChirality(0);
      ptIDLogger = 0;
      int c = getAtomChiralityLimited(a, null, null);
      a.setCIPChirality(c == CIPChirality.NO_CHIRALITY ? JC.CIP_CHIRALITY_NONE : c
          | ((currentRule - 1) << JC.CIP_CHIRALITY_NAME_OFFSET));
      if (doTrack && c != CIPChirality.NO_CHIRALITY)
        data.getRootTrackerResult(root);
    }
    if (haveAlkenes) {

      // set bond chiralities E/Z and M/P

      Lst<int[]> lstEZ = new Lst<int[]>();
      for (int i = bsToDo.nextSetBit(0); i >= 0; i = bsToDo.nextSetBit(i + 1))
        getAtomBondChirality(data.atoms[i], lstEZ, bsToDo);
      if (data.lstSmallRings.length > 0 && lstEZ.size() > 0)
        clearSmallRingEZ(data.atoms, lstEZ);

      // Add helicene chiralities -- predetermined using a Jmol SMARTS conformational search.
      //
      // M: A{a}(.t:-10,-40)a(.t:-10,-40)aaa
      // P: A{a}(.t:10,40)a(.t:10,40)aaa
      //
      // Note that indicators are on the first and last aromatic atoms {a}. 

      setStereoFromSmiles(data.bsHelixM, STEREO_M, data.atoms);
      setStereoFromSmiles(data.bsHelixP, STEREO_P, data.atoms);
    }

    if (Logger.debugging) {
      Logger.info("Kekule ambiguous = " + data.bsKekuleAmbiguous);
      Logger.info("small rings = " + PT.toJSON(null, data.lstSmallRings));
    }

  }

  private void setStereoFromSmiles(BS bsHelix, int stereo, SimpleNode[] atoms) {
    if (bsHelix != null)
      for (int i = bsHelix.nextSetBit(0); i >= 0; i = bsHelix
          .nextSetBit(i + 1))
        atoms[i].setCIPChirality(stereo);
  }

  /**
   * Remove unnecessary atoms from the list and let us know if we have alkenes
   * to consider.
   * 
   * @param atoms
   * @param bsToDo
   * @param bsEnes 
   * @return whether we have any alkenes that could be EZ
   */
  private boolean preFilterAtomList(SimpleNode[] atoms, BS bsToDo, BS bsEnes) {
    boolean haveAlkenes = false;
    for (int i = bsToDo.nextSetBit(0); i >= 0; i = bsToDo.nextSetBit(i + 1)) {
      if (!data.couldBeChiralAtom(atoms[i])) {
        bsToDo.clear(i);
        continue;
      }
      switch (data.couldBeChiralAlkene(atoms[i], null)) {
      case UNDETERMINED:
        break;
      case STEREO_Z:
        bsEnes.set(i);
        //$FALL-THROUGH$
      case STEREO_M:
        haveAlkenes = true;
        break;
      }
    }
    return haveAlkenes;
  }

  /**
   * Check if an atom is 1st row.
   * 
   * @param a
   * @return elemno > 2 && elemno <= 10
   */
  static boolean isFirstRow(SimpleNode a) {
    int n = a.getElementNumber();
    return (n > 2 && n <= 10);
  }

  /**
   * Remove E/Z designations for small-rings double bonds (IUPAC
   * 2013.P-93.5.1.4.1).
   * 
   * @param atoms
   * @param lstEZ
   */
  private void clearSmallRingEZ(SimpleNode[] atoms, Lst<int[]> lstEZ) {
    for (int j = data.lstSmallRings.length; --j >= 0;)
      data.lstSmallRings[j].andNot(data.bsAtropisomeric);
    for (int i = lstEZ.size(); --i >= 0;) {
      int[] ab = lstEZ.get(i);
      for (int j = data.lstSmallRings.length; --j >= 0;) {
        BS ring = data.lstSmallRings[j];
        if (ring.get(ab[0]) && ring.get(ab[1])) {
          atoms[ab[0]].setCIPChirality(JC.CIP_CHIRALITY_NONE);
          atoms[ab[1]].setCIPChirality(JC.CIP_CHIRALITY_NONE);
        }
      }
    }
  }

  /**
   * Get E/Z characteristics for specific atoms. Also check here for
   * atropisomeric M/P designations
   * 
   * @param atom
   * @param lstEZ
   * @param bsToDo
   */

  private void getAtomBondChirality(SimpleNode atom, Lst<int[]> lstEZ, BS bsToDo) {
    int index = atom.getIndex();
    SimpleEdge[] bonds = atom.getEdges();
    int c = NO_CHIRALITY;
    boolean isAtropic = data.bsAtropisomeric.get(index);
    for (int j = bonds.length; --j >= 0;) {
      SimpleEdge bond = bonds[j];
      SimpleNode atom1;
      int index1;
      if (isAtropic) {
        atom1 = bonds[j].getOtherNode(atom);
        index1 = atom1.getIndex();
        if (!data.bsAtropisomeric.get(index1))
          continue;
        c = setBondChirality(atom, atom1, atom, atom1, true);
      } else if (data.getBondOrder(bond) == 2) {
        atom1 = getLastCumuleneAtom(bond, atom, null, null);
        index1 = atom1.getIndex();
        if (index1 < index)
          continue;
        c = getBondChiralityLimited(bond, atom);
      } else {
        continue;
      }
      if (c != NO_CHIRALITY) {
        if (!isAtropic)
          lstEZ.addLast(new int[] { index, index1 });
        bsToDo.clear(index);
        bsToDo.clear(index1);
      }
      if (isAtropic)
        break;
    }
  }

  /**
   * 
   * @param bond
   * @param atom
   * @param nSP2
   *        returns the number of sp2 carbons in this alkene or cumulene
   * @param parents
   * @return the terminal atom of this alkene or cumulene
   */
  private SimpleNode getLastCumuleneAtom(SimpleEdge bond, SimpleNode atom,
                                         int[] nSP2, SimpleNode[] parents) {
    // we know this is a double bond
    SimpleNode atom2 = bond.getOtherNode(atom);
    if (parents != null) {
      parents[0] = atom2;
      parents[1] = atom;
    }
    // connected atom must have only two covalent bonds
    if (nSP2 != null)
      nSP2[0] = 2;
    int ppt = 0;
    while (true) { // COUNT_LINE
      if (atom2.getCovalentBondCount() != 2)
        return atom2;
      SimpleEdge[] edges = atom2.getEdges();
      for (int i = edges.length; --i >= 0;) {
        SimpleNode atom3 = (bond = edges[i]).getOtherNode(atom2);
        if (atom3 == atom)
          continue;
        // connected atom must only have one other bond, and it must be double to continue
        if (data.getBondOrder(bond) != 2)
          return atom2; // was atom3
        if (parents != null) {
          if (ppt == 0) {
            parents[0] = atom2;
            ppt = 1;
          }
          parents[1] = atom2;
        }
        // a=2=3
        if (nSP2 != null)
          nSP2[0]++;
        atom = atom2;
        atom2 = atom3;
        // we know we only have two covalent bonds
        break;
      }
    }
  }

  /**
   * Determine R/S or one half of E/Z determination
   * 
   * @param atom
   *        ignored if a is not null (just checking ene end top priority)
   * @param cipAtom
   *        ignored if atom is not null
   * @param parentAtom
   *        null for tetrahedral, other alkene carbon for E/Z
   * 
   * @return if and E/Z test, [0:none, 1: atoms[0] is higher, 2: atoms[1] is
   *         higher] otherwise [0:none, 1:R, 2:S]
   */
  int getAtomChiralityLimited(SimpleNode atom, CIPAtom cipAtom,
                                      SimpleNode parentAtom) {
    int rs = NO_CHIRALITY;
    bsNeedRule.clearAll();
    bsNeedRule.set(RULE_1a);
    try {
      boolean isAlkeneEndCheck = (atom == null);
      if (isAlkeneEndCheck) {
        // This is an alkene end determination.
        atom = (root = cipAtom).atom;
        cipAtom.htPathPoints = (cipAtom.parent = new CIPAtom().create(
            parentAtom, null, true, false, false)).htPathPoints;
      } else {
        if (!(root = cipAtom = (cipAtom == null ? new CIPAtom().create(atom, null, false, false,
            false) : cipAtom)).isSP3) {
          // This is a root-atom call. 
          // Just checking here that center has 4 covalent bonds or is trigonal pyramidal.
          return NO_CHIRALITY;
        }
      }
      if (cipAtom.setNode()) {
        for (currentRule = RULE_1a; currentRule <= RULE_6; currentRule++) {
          //          if (Logger.debugging)
          //            Logger.info("-Rule " + getRuleName(currentRule)
          //                + " CIPChirality for " + cipAtom + "-----"); // Logger
          int nPrioritiesPrev = cipAtom.nPriorities;
          switch (currentRule) {
          case RULE_2:
            if (cipAtom.rule6refIndex >= 0)
              bsNeedRule.set(RULE_2);
            break;
          case RULE_3:
            // We need to create auxiliary descriptors PRIOR to Rule 3, 
            // as seqcis and seqtrans are auxiliary only
            //  BH64_037
            isAux = true;
            doTrack = false;
            havePseudoAuxiliary = false;
            cipAtom.createAuxiliaryDescriptors(null, null);
            doTrack = data.isTracker();
            isAux = false;
            break;
          case RULE_4a:
            if (!bsNeedRule.get(RULE_4a)) {
              // We can skip Rules 4a - 5 if there are no chirality centers.
              currentRule = RULE_5;
              continue;
            }
            //$FALL-THROUGH$
          case RULE_4b:
          case RULE_4c:
            // We need to presort with no tie-breaking for Rules 4a, 4b, and 4c.
            cipAtom.sortSubstituents(Integer.MIN_VALUE);
            bsNeedRule.set(currentRule);
            break;
          case RULE_5:
            // We need to presort with no tie-breaking for Rule 5,
            // clearing the Rule4List is advisable, as Rule 4c may have changed orders.
            if (havePseudoAuxiliary)
              cipAtom.clearRule4Lists();
            cipAtom.sortSubstituents(Integer.MIN_VALUE);
            bsNeedRule.set(currentRule);
            break;
          case RULE_6:
            // We only need to do Rule 6 under certain conditions.
            bsNeedRule.setBitTo(RULE_6,
                (cipAtom.rule6refIndex < 0 && (rs = cipAtom.getRule6Descriptor(false)) != NO_CHIRALITY));
            break;
          }
          if (!bsNeedRule.get(currentRule))
            continue;

          // initial call to sortSubstituents does all, recursively

          if (rs == NO_CHIRALITY && cipAtom.sortSubstituents(0)) {
            if (Logger.debuggingHigh && cipAtom.h1Count < 2) {
              for (int i = 0; i < cipAtom.bondCount; i++) { // Logger
                if (cipAtom.atoms[i] != null) // Logger
                  Logger.info(cipAtom.atoms[i] + " " + cipAtom.priorities[i]); // Logger
              }
            }

            // If this is an alkene end check, we just use STEREO_S and STEREO_R as markers

            if (isAlkeneEndCheck)
              return cipAtom.getEneTop();

            rs = data.checkHandedness(cipAtom);
            if (currentRule == RULE_5) {
              if (cipAtom.nPriorities == 4 && nPrioritiesPrev == 2)
                cipAtom.isRule5Pseudo = !cipAtom.isRule5Pseudo;
              if (cipAtom.isRule5Pseudo)
                rs |= JC.CIP_CHIRALITY_PSEUDO_FLAG;

              // Exclude special cases:

              //  P-92.2.1.1(c) pseudoasymmetric centers must have 
              //                two and only two enantiomorphic ligands
              //

              // Rule 5 has decided the issue, but how many decisions did we make?
              // If priorities [0 0 2 2] went to [0 1 2 3] then
              // we have two Rule-5 decisions -- R,S,R',S'.
              // In that case, Rule 5 results in R/S, not r/s.
              //
              //     S
              //     -
              //     -
              // R---C---R'      despite this being Rule 5, the results is R, not r. 
              //     -
              //     -
              //     S'
              //
              // --------- mirror plane
              //
              //     R'
              //     -
              //     -
              // S---C---S'     not superimposible
              //     -
              //     -
              //     R
              // 
              // in addition, Rule 4c may not have caught a case where a ligand is self-enantiomorphic. 
              // 
              // s'          r'
              //  \         /
              //   N---C---N
              //  /         \
              // r           s
              //
              // Neither ligand changes chirality with 
              // since there are no descriptors on N, we won't catch this there. 
            }
            if (Logger.debugging)
              Logger.info(atom + " " + JC.getCIPChiralityName(rs) + " by Rule "
                  + getRuleName(currentRule)
                  + "\n----------------------------------"); // Logger
            return rs;
          }
        }
      }
    } catch (Throwable e) {
      System.out.println(e + " in CIPChirality " + currentRule);
      /**
       * @j2sNative alert(e);
       */
      {
        e.printStackTrace();
      }
      return STEREO_BOTH_RS;
    }
    return rs;
  }

  /**
   * Determine the axial or E/Z chirality for this bond, with the given starting
   * atom a
   * 
   * @param bond
   * @param a
   *        first atom to consider, or null
   * @return one of: {NO_CHIRALITY | STEREO_Z | STEREO_E | STEREO_Ra | STEREO_Sa
   *         | STEREO_ra | STEREO_sa}
   */
  private int getBondChiralityLimited(SimpleEdge bond, SimpleNode a) {
    if (a == null)
      a = bond.getOtherNode(null);
    if (data.couldBeChiralAlkene(a, bond) == UNDETERMINED)
      return NO_CHIRALITY;
    int[] nSP2 = new int[1];
    SimpleNode[] parents = new SimpleNode[2];
    SimpleNode b = getLastCumuleneAtom(bond, a, nSP2, parents);
    boolean isAxial = nSP2[0] % 2 == 1;
    if (!isAxial && data.bsAromatic.get(a.getIndex()))
      return UNDETERMINED;
    int c = setBondChirality(a, parents[0], parents[1], b, isAxial);
    if (Logger.debugging)
      Logger.info("get Bond Chirality " + JC.getCIPChiralityName(c) + " " + bond);
    return c;
  }

  /**
   * Determine the axial or E/Z chirality for the a-b bond.
   * 
   * @param a
   * @param pa
   * @param pb
   * @param b
   * @param isAxial
   * @return one of: {NO_CHIRALITY | STEREO_Z | STEREO_E | STEREO_M | STEREO_P |
   *         STEREO_m | STEREO_p}
   */
  private int setBondChirality(SimpleNode a, SimpleNode pa, SimpleNode pb,
                               SimpleNode b, boolean isAxial) {
    CIPAtom a1 = new CIPAtom().create(a, null, true, false, false);
    CIPAtom b2 = new CIPAtom().create(b, null, true, false, false);
    int atop = getAtomChiralityLimited(null, a1, pa) - 1;
    int ruleA = currentRule;
    int btop = getAtomChiralityLimited(null, b2, pb) - 1;
    int ruleB = currentRule;
    if (isAxial && a1.nRootDuplicates > 3 && atop < 0 && btop < 0) {
      // No resolution for odd cumulene, but we have multiple root duplicates.
      // Quick check of Rule 6.
      ruleA = ruleB = currentRule = RULE_6;
      b2.rule6refIndex = a1.atoms[atop = a1.getEneTop() - 1].atomIndex;
      if (b2.sortSubstituents(0))
        btop = b2.getEneTop() - 1;
    }
    int c = (atop >= 0 && btop >= 0 ? getEneChirality(b2.atoms[btop], b2, a1,
        a1.atoms[atop], isAxial, true) : NO_CHIRALITY);
    if (c != NO_CHIRALITY
        && (isAxial || !data.bsAtropisomeric.get(a.getIndex())
            && !data.bsAtropisomeric.get(b.getIndex()))) {
      //
      // We must check maxRules. 
      // 
      // nonaxial:
      //
      //    a     c
      //     \   /
      //      C=C
      //     /   \
      //    b     d
      //
      //    R     R'
      //     \   /
      //      C=C
      //     /   \
      //    S     S'
      //
      // planar flip is unchanged, and this is seqcis, seqtrans
      // 
      //    a     R'
      //     \   /
      //      C=C
      //     /   \
      //    b     S'
      //
      // planar flip is changed, and this is seqCis, seqTrans
      //
      //
      // axial is the opposite:
      //
      // see AY236.70 and AY236.170
      //
      // 
      //    a       c
      //     \     /
      //      C=C=C
      //     /     \
      //    b       d
      //
      //    R       R'
      //     \     /
      //      C=C=C
      //     /     \
      //    S       S'
      //
      // planar flip is changed, and this is M, P
      // 
      //    a       R
      //     \     /
      //      C=C=C
      //     /     \
      //    b       S
      //
      // planar flip is unchanged, and this is m, p
        
      if (isAxial == (ruleA == RULE_5) == (ruleB == RULE_5))
        c &= ~JC.CIP_CHIRALITY_PSEUDO_FLAG;
      else
        c |= JC.CIP_CHIRALITY_PSEUDO_FLAG;
   
      a.setCIPChirality(c | ((ruleA - 1) << JC.CIP_CHIRALITY_NAME_OFFSET));
      b.setCIPChirality(c | ((ruleB - 1) << JC.CIP_CHIRALITY_NAME_OFFSET));
      if (Logger.debugging)
        Logger.info(a + "-" + b + " " + JC.getCIPChiralityName(c));
    }
    return c;
  }

  /**
   * Determine the stereochemistry of a bond
   * 
   * @param winner1
   * @param end1
   * @param end2
   * @param winner2
   * @param isAxial
   *        if an odd-cumulene
   * @param allowPseudo
   *        if we are working from a high-level bond stereochemistry method
   * @return STEREO_M, STEREO_P, STEREO_Z, STEREO_E, or NO_CHIRALITY
   */
  int getEneChirality(CIPAtom winner1, CIPAtom end1, CIPAtom end2,
                      CIPAtom winner2, boolean isAxial, boolean allowPseudo) {
    return (winner1 == null || winner2 == null || winner1.atom == null
        || winner2.atom == null ? NO_CHIRALITY : isAxial ? data.isPositiveTorsion(winner1,
        end1, end2, winner2) : data.isCis(winner1, end1, end2, winner2));
  }

  class CIPAtom implements Comparable<CIPAtom>, Cloneable {

    /**
     * odd/even toggle for comparisons of Rule5 results that would make for R/S, not r/s, if this flag is false.
     * 
     */
    boolean isRule5Pseudo = true;

    /**
     * unique ID for this CIPAtom for debugging only
     * 
     */
    private int id;

    /**
     * bond distance from the root atom to this atom
     */
    private int sphere;

    /**
     * Rule 1b measure: Distance from root of the corresponding nonduplicated
     * atom (duplicate nodes only).
     * 
     * AMENDED HERE for duplicate nodes associated with a double-bond in a
     * 6-membered-ring benzenoid (benzene, naphthalene, pyridine, pyrazoline,
     * etc.) "Kekule-ambiguous" system to be its sphere.
     * 
     */

    private int rootDistance;

    /**
     * a flag to prevent finalization of an atom node more than once
     * 
     */
    private boolean isSet;

    /**
     * a flag to indicate atom that is a duplicate, either due to
     * ring closure or multiple bonding -- element number and mass, but no
     * substituents; slightly lower in priority than standard atoms.
     * 
     */
    boolean isDuplicate = true;

    /**
     * a flag to indicate an atom that has no substituents; a branch end point;
     * typically H or a halogen (F, Cl, Br, I); also set TRUE if there is a problem
     * setting an atom; does not include duplicates
     * 
     */
    boolean isTerminal;

    /**
     * is one atom of a double bond
     */

    private boolean isAlkene;

    /**
     * the associated Jmol (or otherwise) atom; use of the SimpleNode interface
     * allows us to implement this in SMILES or Jmol as well as providing an
     * interface other programs could use if implementing this code
     * 
     */
    SimpleNode atom;

    /**
     * the application-assigned unique atom index for this atom; used in
     * updating lstSmallRings
     * 
     */
    int atomIndex = -1;

    /**
     * true atom covalent bond count; cached for better performance
     */
    int bondCount;

    /**
     * Rule 1a nominal element number; may be fractional for Kekule issues
     */
    float elemNo;

    /**
     * Rule 2 isotope mass number if identified or average atomic mass if not
     * 
     * C (12.011) > 12C, O (15.999) < 16O, and F (18.998) < 19F
     * 
     * Source:
     * 
     */
    private float mass = UNDETERMINED;

    ///// SUBSTITUENTS ////

    /**
     * direct ancestor of this atom
     * 
     */
    CIPAtom parent;

    /**
     * sphere-1 node in this atom's root branch
     */
    CIPAtom rootSubstituent;

    /**
     * a count of how many 1H atoms we have found on this atom; used to halt
     * further processing of this atom
     */
    int h1Count;

    /**
     * the substituents -- up to four supported here at this time
     * 
     */
    CIPAtom[] atoms = new CIPAtom[4];

    /**
     * number of substituent atoms (non-null atoms[] entries)
     */
    private int nAtoms;

    /**
     * bitset of indices of the associated atoms in the path to this atom node
     * from the root; used to keep track of the path to this atom in order to
     * prevent infinite cycling; the last atom in the path when cyclic is a
     * duplicate atom.
     * 
     */
    private BS bsPath;

    /**
     * String path, for debugging
     * 
     */
    String myPath = "";

    /**
     * priorities associated with each subsituent from high (0) to low (3); due
     * to equivaliencies at a given rule level, these numbers may duplicate and
     * have gaps - for example, [0 2 0 3]
     */
    int[] oldPriorities, priorities = new int[4];

    /**
     * the number of distinct priorities determined for this atom for the
     * current rule; 0-4 for the root atom; 0-3 for all others
     */
    int oldNPriorities, nPriorities;

    /**
     * current priority 0-3; used for Rule 4b and 5 priority sorting
     * 
     */
    int priority;

    /**
     * ASCII-encoded priority string 
     */
    private String chiralPath;
    
    /**
     * number of root-duplicate atoms (root atom only
     */

    int nRootDuplicates;

    /**
     * Rule 1b hash table that maintains distance of the associated
     * nonduplicated atom node
     * 
     */
    Map<Integer, Integer> htPathPoints;

    /**
     * reference index for Rule 6 -- root atom only
     */
    int rule6refIndex = -1;

    /**
     * a list of only the undifferentiated ligands in Rule 6 -- root atom only
     */
    private BS bsRule6Subs; 

    /////// double and triple bonds ///////

    /**
     * first atom of an alkene or cumulene atom
     */

    private CIPAtom alkeneParent;

    /**
     * last atom of an alkene or cumulene atom
     */

    private CIPAtom alkeneChild;

    /**
     * a flag used in Rule 3 to indicate the second carbon of a double bond
     */

    private boolean isAlkeneAtom2;

    /**
     * is an atom that is involved in more than one Kekule form
     */
    private boolean isKekuleAmbiguous;

    /**
     * first =X= atom in a string of =X=X=X=...
     */
    private CIPAtom nextSP2;

    /**
     * potentially useful information that this duplicate is from an double- or
     * triple-bond, not a ring closure
     */
    private boolean multipleBondDuplicate;

    /**
     * alkene or even cumulene, so chirality will be EZ, not MP
     */
    private boolean isEvenEne = true;

    //// AUXILIARY CHIRALITY (SET JUST PRIOR TO RULE 3) /////

    /**
     * auxiliary chirality E/Z for this atom node
     */
    private int auxEZ = UNDETERMINED;

    /**
     * a flag set false in evaluation of Rule 5 to indicate that there was more
     * than one R/S decision made, so this center cannot be r/s; initially just
     * indicates that the atom has 4 covalent bonds or is trigonal pyriamidal
     */
    boolean isSP3 = true;

    /**
     * auxiliary chirality as determined in createAuxiliaryRule4Data;
     * possibilities include R/S, r/s, M/P, m/p, C/T (but not c/t), or ~ (ASCII
     * 126, no stereochemistry); for sorting purposes C=M=R < p=r=s < ~
     */
    private char auxChirality = '~';

    /**
     * points to next branching point that has two or more chiral branches
     */
    private CIPAtom nextChiralBranch;

    /**
     * a check for downstream chirality
     * 
     */
    private boolean isChiralPath;

    /**
     * the auxiiary chirality type: 0: ~, 1: R, 2: S; normalized to R/S even if
     * M/P or C/T
     */
    int rule4Type;

    /**
     * used to count the number of priorities
     */
    private BS bsTemp = new BS();

    /**
     * Rule 4b reference chirality (R or S, 1 or 2); root only
     */
    private int rule4Ref;

    /**
     * bitsets for tracking R and S for Rule 4b
     */
    BS[] listRS;

    CIPAtom() {
      // had a problem in JavaScript that the constructor of an inner function cannot
      // access this.b$ yet. That assignment is made after construction.
    }

    /**
     * 
     * @param atom
     *        or null to indicate a null placeholder
     * @param parent
     * @param isAlkene
     * @param isDuplicate
     * @param isParentBond
     * @return this
     */
    @SuppressWarnings("unchecked")
    CIPAtom create(SimpleNode atom, CIPAtom parent, boolean isAlkene,
                   boolean isDuplicate, boolean isParentBond) {
      this.id = ++ptIDLogger;
      this.parent = parent;
      if (atom == null)
        return this;
      this.isAlkene = isAlkene;
      this.atom = atom;
      atomIndex = atom.getIndex();
      if (atom.getIsotopeNumber() > 0)
        bsNeedRule.set(RULE_2);
      this.isDuplicate = multipleBondDuplicate = isDuplicate;
      isKekuleAmbiguous = (data.bsKekuleAmbiguous != null && data.bsKekuleAmbiguous
          .get(atomIndex));
      elemNo = (isDuplicate && isKekuleAmbiguous ? 
          parent.getKekuleElementNumber() 
          : atom.getElementNumber());
      bondCount = atom.getCovalentBondCount();
      isSP3 = (bondCount == 4 || bondCount == 3 && !isAlkene
          && (elemNo > 10 || data.bsAzacyclic != null && data.bsAzacyclic.get(atomIndex)));
      if (parent != null)
        sphere = parent.sphere + 1;
      if (sphere == 1) {
        rootSubstituent = this;
        htPathPoints = new Hashtable<Integer, Integer>();
      } else if (parent != null) {
        rootSubstituent = parent.rootSubstituent;
        htPathPoints = (Map<Integer, Integer>) ((Hashtable<Integer, Integer>) parent.htPathPoints)
            .clone();
      }
      bsPath = (parent == null ? new BS() : (BS) parent.bsPath.clone());

      if (isDuplicate)
        bsNeedRule.set(RULE_3);
      rootDistance = sphere;
      // The rootDistance for a nonDuplicate atom is just its sphere.
      // The rootDistance for a duplicate atom is (by IUPAC) the sphere of its duplicated atom.
      // I argue that for aromatic compounds, this introduces a Kekule problem and that for
      // those cases, the rootDistance should be its own sphere, not the sphere of its duplicated atom.
      // This shows up in AV-360#215. 

      if (parent == null) {
        // original atom
        bsPath.set(atomIndex);
      } else if (multipleBondDuplicate
          ) {
        rootDistance--;
      } else if (bsPath.get(atomIndex)) {
        bsNeedRule.setBitTo(RULE_1b, (this.isDuplicate = true));
        if ((rootDistance = (atom == root.atom ? 0 : isParentBond ? parent.sphere 
            : htPathPoints.get(Integer.valueOf(atomIndex)).intValue())) == 0) {
          root.nRootDuplicates++;
        }
      } else {
        bsPath.set(atomIndex);
        htPathPoints.put(Integer.valueOf(atomIndex), Integer.valueOf(rootDistance));
      }
      if (doTrack) {
        if (sphere < MAX_PATH) // Logger
          myPath = (parent != null ? parent.myPath + "-" : "") + this; // Logger
        if (Logger.debuggingHigh)
          Logger.info("new CIPAtom " + myPath);
      }
      return this;
    }

    /**
     * Check ene for first nonduplicate.
     * 
     * @return 1 or 2
     */
    int getEneTop() {
      return (atoms[0].isDuplicate ? 2 : 1);
    }

    /**
     * The original Rule 6 implementation; allows cubane, tetrahedrane, and
     * bicyclo[2.2.2]octane to be ns.
     * 
     * 
     * @param isAux
     * 
     * @return NO_CHIRALITY or descriptor
     */
    int getRule6Descriptor(boolean isAux) {
      if (nPriorities > 2
          || (isAux ? countAuxDuplicates(atomIndex) : nRootDuplicates) <= 2)
        return NO_CHIRALITY;

      // we have more than two root-duplicates and priorities array is one of:
      // [0 0 0 0] CIP Helv Chim. Acta 1966 #33 -- double spiran
      // [0 0 0 0] CIP 1982 S4
      // [0 0 2 2] P-93.5.3.2 spiro
      // [0 1 1 1] or [0 0 0 3] CIP Helv. Chim. Acta 1966 #32 -- C3-symmetric
      int i1 = (priorities[0] == priorities[1] ? 0 : 1);
      int i2 = (priorities[2] != priorities[3] ? 3 : 4);
      int istep = (priorities[2] == priorities[1] ? 1 : 2);
      int rsRM = 0, rsSP = 0;
      BS bsSubs = new BS();
      for (int i = i1; i < i2; i++)
        bsSubs.set(atoms[i].atomIndex);
      if (nPriorities == 1)
        i2 = 2; // actually, we only want two for double spiran or S4

      // Check result of promoting each undifferentiated ligand.
      // Exit if two R or two S are found.

      CIPAtom cipAtom = null;
      int rs;
      for (int i = i1; i < i2; i += istep) {
        if (data.testRule6Full) {
          // Full Rule 6 -- allow for new auxiliary descriptors in Rule 6.
          // This will cause bicyclo[2.2.2]octane to be rr.
          cipAtom = new CIPAtom().create(atom, null, false, false, false);
          cipAtom.rule6refIndex = atoms[i].atomIndex;
          cipAtom.setNode();
          for (int j = 0; j < 4; j++) {
            cipAtom.atoms[j] = (CIPAtom) atoms[j].clone();
            cipAtom.priorities[j] = priorities[j];
          }
          cipAtom.bsRule6Subs = bsSubs;
          rs = getAtomChiralityLimited(atom, cipAtom, null);
          currentRule = RULE_6;
          if (rs == NO_CHIRALITY)
            return NO_CHIRALITY;
        } else {
          // Original Rule 6 -- just check for switches.
          // Bicyclo[2.2.2]octane will be ns.
          root.bsRule6Subs = new BS();
          root.rule6refIndex = atoms[i].atomIndex;
          saveRestorePriorities(false);
          sortSubstituents(Integer.MIN_VALUE);
          if (!sortSubstituents(0))
            return NO_CHIRALITY;
          rs = data.checkHandedness(this);
          saveRestorePriorities(true);
        }        
        // If the descriptor is r or s, that is our descriptor.
        if ((rs & JC.CIP_CHIRALITY_PSEUDO_FLAG) == 0) {
          // If it is not r/s, check for more than one R or more than one S.
          // If more than one R or more than one S, that is our descriptor.
          if (rs == STEREO_R || rs == STEREO_M) {
            if (rsRM == 0) {
              rsRM = rs;
              continue;
            }
          } else if (rsSP == 0) {
            rsSP = rs;
            continue;
          }
        }
        // We have a determination.
        return rs;
      }
      return NO_CHIRALITY;
    }

    /**
     * Reset priorities after each Rule 6 test.
     *  
     * @param isRestore
     */
    private void saveRestorePriorities(boolean isRestore) {
      if (isRestore) {
        priorities = oldPriorities;
        nPriorities = oldNPriorities;
      } else {
        oldPriorities = Arrays.copyOf(priorities, 4);
        oldNPriorities = nPriorities;        
      }
      for (int i = 0; i < nAtoms; i++)
          atoms[i].saveRestorePriorities(isRestore);
    }

    /**
     * Get a count of the number of duplicate nodes to the auxiliary atom.
     * 
     * @param index
     * 
     * @return the number of dublicates to the auxiliary center
     */
    private int countAuxDuplicates(int index) {
      int n = 0;
      for (int i = 0; i < 4; i++) {
        if (atoms[i] == null)
          continue;
        if (atoms[i].isDuplicate) {
          if (atoms[i].atomIndex == index)
            n++;
        } else {
          n += atoms[i].countAuxDuplicates(index);
        }
      }
      return n;
    }

    /**
     * get the atomic mass only if needed by Rule 2, testing for three special
     * conditions in the case of isotopes:
     * 
     * If a duplicate, or an isotope that is 100% nat abundant is specified, or
     * isotopic mass is not specified, just use the average mass.
     * 
     * Otherwise, use the integer isotope mass number, taking care in the cases
     * of 16O, 52Cr, 96Mo, and 175Lu, to reduce this integer by 0.1 so as to put
     * it in the correct relationship to the element's average mass.
     * 
     * @return mass or mass surrogate
     */
    private float getMass() {
      if (isDuplicate)
        return 0;
      if (mass == UNDETERMINED) {
        if (isDuplicate || (mass = atom.getMass()) != (int) mass
            || isType(RULE_2_nXX_EQ_XX))
          return (mass == UNDETERMINED ? mass = Elements.getAtomicMass((int) elemNo)
              : mass);
        // for 16O;52Cr;96Mo;175Lu;
        // adjust integer mass number down just a bit to account
        // for average mass being slightly higher than actual isotope mass, not lower
        if (isType(RULE_2_REDUCE_ISOTOPE_MASS_NUMBER))
          mass -= 0.1f;
      }
      return mass;
    }

    private boolean isType(String rule2Type) {
      return PT.isOneOf(
          (int) mass + Elements.elementSymbolFromNumber((int) elemNo),
          rule2Type);
    }

    /**
     * Calculate the average element numbers of associated double-bond atoms
     * weighted by their most significant Kekule resonance contributor(s). We
     * only consider simple benzenoid systems -- 6-membered rings and their
     * 6-memebered rings fused to them. Calculated for the parent of an
     * sp2-duplicate atom.
     * 
     * @return an averaged element number
     */
    private float getKekuleElementNumber() {
      SimpleEdge[] edges = atom.getEdges();
      SimpleEdge bond;
      float ave = 0;//atom.getElementNumber();
      int n = 0;//1;
      for (int i = edges.length; --i >= 0;)
        if ((bond = edges[i]).isCovalent()) {
          SimpleNode other = bond.getOtherNode(atom);
          if (data.bsKekuleAmbiguous.get(other.getIndex())) {
//            System.out.println(this + " adding " + other + " " + ave + " " + n);
            n++;
            ave += other.getElementNumber();
          }
        }
      return ave / n;
    }

    /**
     * Set the atom to have substituents.
     * 
     * @return true if a valid atom for consideration
     * 
     */
    boolean setNode() {
      // notice we are setting isSet TRUE here, not just testing it.
      if (isSet || (isSet = true) && isDuplicate)
        return true;
      int index = atom.getIndex();
      SimpleEdge[] bonds = atom.getEdges();
      int nBonds = bonds.length;
      if (Logger.debuggingHigh)
        Logger.info("set " + this);
      int pt = 0;
      for (int i = 0; i < nBonds; i++) {
        SimpleEdge bond = bonds[i];
        if (!bond.isCovalent())
          continue;
        SimpleNode other = bond.getOtherNode(atom);
        boolean isParentBond = (parent != null && parent.atom == other);
        int order = data.getBondOrder(bond);
        if (order == 2) {
          if (elemNo > 10 || !isFirstRow(other))
            order = 1;
          else {
            isAlkene = true;
            if (isParentBond)
              setEne();
          }
        }
        if (nBonds == 1 && order == 1 && isParentBond)
          return isTerminal = true;
        // from here on, isTerminal indicates an error condition
        switch (order) {
        case 3:
          if (addAtom(pt++, other, isParentBond, false, isParentBond) == null)
            return !(isTerminal = true);
          //$FALL-THROUGH$
        case 2:
          if (addAtom(pt++, other, order != 2 || isParentBond, order == 2,
              isParentBond) == null)
            return !(isTerminal = true);
          //$FALL-THROUGH$
        case 1:
          if (isParentBond
              || addAtom(pt++, other, order != 1 && elemNo <= 10, false, false) != null)
            break;
          //$FALL-THROUGH$
        default:
          return !(isTerminal = true);
        }
      }
      nAtoms = pt;
      switch (pt) {
      case 2:
      case 3:
        // [c-,r5d3n+0,r5d2o+0]
        if (elemNo == 6 && data.bsNegativeAromatic.get(index) || data.bsXAromatic.get(index)) {
          nAtoms++;
          addAtom(pt++, this.atom, true, false, false);
        }
        break;

      }
      if (pt < 4)
        for (; pt < atoms.length; pt++)
          atoms[pt] = new CIPAtom().create(null, this, false, true, false);

      // Do an initial very shallow atom-only Rule 1 sort using a.compareTo(b)

      try {
        Arrays.sort(atoms);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return true;
    }

    /**
     * Set all ene-related fields upon finding the second atom.
     * 
     */
    private void setEne() {
      parent.alkeneChild = null;
      alkeneParent = (parent.alkeneParent == null ? parent
          : parent.alkeneParent);
      alkeneParent.alkeneChild = this;
      nextSP2 = parent;
      if (parent.alkeneParent == null)
        parent.nextSP2 = this;
      if (atom.getCovalentBondCount() == 2 && atom.getValence() == 4) {
        parent.isAlkeneAtom2 = false;
        alkeneParent.isEvenEne = !alkeneParent.isEvenEne;
      } else {
        isAlkeneAtom2 = true;
      }
    }

    /**
     * Add a new atom or return null
     * 
     * @param i
     * @param other
     * @param isDuplicate
     * @param isAlkene
     * @param isParentBond
     * @return new atom or null
     */
    private CIPAtom addAtom(int i, SimpleNode other, boolean isDuplicate,
                    boolean isAlkene, boolean isParentBond) {
      if (i >= atoms.length) {
        if (Logger.debugging)
          Logger.info(" too many bonds on " + atom);
        return null;
      }
      if (other.getElementNumber() == 1 && other.getIsotopeNumber() == 0) {
        if (++h1Count > 1) {
          if (parent == null) {
            // For top level, we do not allow two 1H atoms.
            if (Logger.debuggingHigh)
              Logger.info(" second H atom found on " + atom);
            return null;
          }
        }
      }
      return atoms[i] = new CIPAtom().create(other, this, isAlkene,
          isDuplicate, isParentBond);
    }

    ///// sorting methods /////
    
    /**
     * Deep-Sort the substituents of an atom, setting the node's atoms[] and
     * priorities[] arrays. Checking for "ties" that will lead to
     * pseudochirality is also done here.
     * 
     * @param sphere
     *        current working sphere; Integer.MIN_VALUE to not break ties
     * @return all priorities assigned
     * 
     */
    boolean sortSubstituents(int sphere) {

      // runs about 20% faster with this check
      if (nPriorities == (sphere < 1 ? 4 : 3))
        return true;

      // Note that this method calls breakTie and is called recursively from breakTie.

      boolean ignoreTies = (sphere == Integer.MIN_VALUE);
      if (ignoreTies) {
        // If this is Rule 4a, 4c, or 6, we must presort the full tree without breaking ties
        if (isTerminal)
          return false;
        switch (currentRule) {
        case RULE_4a:
        case RULE_4c:
          for (int i = 0; i < 4; i++)
            if (atoms[i] != null && (atoms[i].isChiralPath || atoms[i].nextChiralBranch != null))
              atoms[i].sortSubstituents(Integer.MIN_VALUE);
          if (isAlkene) // was isSP3
            return false;
          break;
        case RULE_6:
          for (int i = 0; i < 4; i++)
            if (atoms[i] != null && !atoms[i].isDuplicate
                && atoms[i].atom != null && atoms[i].setNode())
              atoms[i].sortSubstituents(Integer.MIN_VALUE);
          break;
       }
      }

      ignoreTies |= (currentRule == RULE_4b || currentRule == RULE_5);

      int[] indices = new int[4];

      int[] newPriorities = new int[4];

      if (Logger.debuggingHigh && h1Count < 2) {
        Logger.info(root + "---sortSubstituents---" + this);
        for (int i = 0; i < 4; i++) { // Logger
          Logger.info(getRuleName(currentRule) + ": " + this + "[" + i + "]="
              + atoms[i].myPath + " " + Integer.toHexString(priorities[i])); // Logger
        }
        Logger.info("---" + nPriorities);
      }

      int loser;
      for (int i = 0; i < 3; i++) {
        CIPAtom a = atoms[i];
        boolean aLoses = a.isDuplicate && currentRule > RULE_1b; 
        for (int j = i + 1; j < 4; j++) {          
          CIPAtom b = atoms[loser = j];
          int score = TIED;

          // Check:
          
          // (a) if one of the atoms is a phantom atom (P-92.1.4.1); if not, then
          // (b) if the prioritiy has already been set; if not, then
          // (c) if the current rule decides; if not, then
          // (d) if the tie can be broken in the next sphere
          switch (b.atom == null || priorities[i] < priorities[j] ? A_WINS
              : aLoses || a.atom == null || priorities[j] < priorities[i] ? B_WINS
                  : (score = a.checkCurrentRule(b)) != TIED  && score != IGNORE || ignoreTies ? score
                          : sign(a.breakTie(b, sphere + 1))) {
          case B_WINS:
            loser = i;
            //$FALL-THROUGH$
          case A_WINS:
            newPriorities[loser]++;
            if (doTrack && score != TIED && (sphere == 0 || ignoreTies))
              data.track(CIPChirality.this, a, b, 1, score, false);
            //$FALL-THROUGH$
          case IGNORE:
          case TIED:
            indices[loser]++;
            continue;
          }
        }
      }

      // update nPriorities and all arrays

      bsTemp.clearAll(); // track number of priorities
      CIPAtom[] newAtoms = new CIPAtom[4];
      for (int i = 0; i < 4; i++) {
        int pt = indices[i];
        CIPAtom a = newAtoms[pt] = atoms[i];
        int p = newPriorities[i];
        if (a.atom != null)
          bsTemp.set(p);
        a.priority = priorities[pt] = p;
      }
      atoms = newAtoms;
      nPriorities = bsTemp.cardinality();
      if (Logger.debuggingHigh && atoms[2].atom != null && atoms[2].elemNo != 1) { // Logger
        Logger.info(dots() + atom + " nPriorities = " + nPriorities);
        for (int i = 0; i < 4; i++) { // Logger
          Logger.info(dots() + myPath + "[" + i + "]=" + atoms[i] + " "
              + priorities[i] + " " + Integer.toHexString(priorities[i])); // Logger
        }
        Logger.info(dots() + "-------" + nPriorities);
      }

      // We are done if the number of priorities equals the bond count.

      return (nPriorities == bondCount);
    }

    /**
     * Provide an indent for clarity in debugging messages
     * 
     * @return a string of dots based on the value of atom.sphere.
     */
    private String dots() {
      return ".....................".substring(0, Math.min(20, sphere)); // Logger
    }

    /**
     * Break a tie at any level in the iteration between to atoms that otherwise
     * are the same by sorting their substituents.
     * 
     * @param b
     * @param sphere
     *        current working sphere
     * @return [0 (TIED), -1 (A_WINS), or 1 (B_WINS)] * (sphere where broken)
     */
    private int breakTie(CIPAtom b, int sphere) {

      int finalScore = TIED;

      while (true) {
        //System.out.println("breaking tie for " + this + " " + b);

        // Phase I: Quick check of this atom itself

        // return TIED if:

        // a) this is a duplicate, and we are done with Rule 1b
        // b) two duplicates are of the same node (atom and root distance)
        // c) one or the other can't be set (because it has too many connections), or
        // d) both are terminal or both are duplicates (no atoms to check)

        if (isDuplicate
            && (currentRule > RULE_1b || b.isDuplicate && atom == b.atom
                && rootDistance == b.rootDistance) 
                || !setNode() || !b.setNode() 
                || isTerminal && b.isTerminal 
                || isDuplicate && b.isDuplicate)
          break;

        
        // We are done if one of these is terminal 
        // (for the next sphere, unless one is a duplicate -- Custer Rule 1b "duplicate > nonduplicate").

        if (isTerminal != b.isTerminal) {
          finalScore = (isTerminal ? B_WINS : A_WINS)
              * (sphere + (b.isDuplicate || isDuplicate ? 0 : 1)); // COUNT_LINE
          if (doTrack)
            data.track(CIPChirality.this, this, b, sphere, finalScore, true);
          break;
        }
        // Do a duplicate check.

        // If this is not a TIE, we do not have to go any further.

        // NOTE THAT THIS CHECK IS NOT EXPLICIT IN THE RULES
        // BECAUSE DUPLICATES LOSE IN THE NEXT SPHERE, NOT THIS ONE.
        // THE NEED FOR (sphere+1) in AY236.53, 163, 173, 192 SHOWS THAT 
        // SUBRULE 1a MUST BE COMPLETED EXHAUSTIVELY PRIOR TO SUBRULE 1b.
        //
        // Note that this check must return "N+1", because that is where the actual difference is.
        // This nuance is not clear from the "simplified" digraphs found in Chapter 9. 
        //
        // Say we have {O (O) C} and {O O H}
        //
        // The rules require that we first only look at just the atoms, so OOC beats OOH in this sphere,
        // but there are no atoms to check on (O), so we can do the check here to save time, reporting back
        // to breatTie that we found a difference, but not in this sphere.
        // This allows C to still beat H in this sphere, but had it been {O (O} C} and {O O C}, then 
        // we would be done.

        int score = (currentRule > RULE_1a ? TIED : unlikeDuplicates(b));
        if (score != TIED) {
          finalScore = score * (sphere + 1); // COUNT_LINE
          if (doTrack)
            data.track(CIPChirality.this, this, b, sphere, finalScore, false);
          break;
        }

        // Phase II -- shallow check only
        //
        // Compare only in the current substitutent sphere. 
        //
        // Check to see if any of the three connections to a and b are 
        // different themselves, without any deeper check.
        //
        // This requires that both a annd b have their ligands sorted
        // at least in a preliminary fashion, using Array.sort() and compareTo()
        // for Rules 1a, 1b, and 2.
        // But if we do not do a presort, then we have to do those first.
        // Doing the presort saves considerably on run time.

        for (int i = 0; i < nAtoms; i++)
          if ((score = atoms[i].checkCurrentRule(b.atoms[i])) != TIED) {
            finalScore = score * (sphere + 1); // COUNT_LINE
            if (doTrack)
              data.track(CIPChirality.this, atoms[i], b.atoms[i], sphere, finalScore, false);
            break;
          }

        if (finalScore != TIED) {
          break;
        }
        // Time to do a full sort of eash ligand, including breaking ties
        
        sortSubstituents(sphere);
        b.sortSubstituents(sphere);

        // Phase III -- check deeply using re-entrant call to breakTie
        //
        // Now iteratively deep-sort each list based on substituents
        // and then check them one by one to see if the tie can be broken.

        // Note that if not catching duplicates early, we must check for 
        // nAtoms == 0 and set finalScore in that case to B_WINS * (sphere + 1)
        // but we are checking for duplicates early in this implementation.

        for (int i = 0, abs, absScore = Integer.MAX_VALUE; i < nAtoms; i++) {
          if ((score = atoms[i].breakTie(b.atoms[i], sphere + 1)) != TIED
              && (abs = Math.abs(score)) < absScore) {
            absScore = abs;
            finalScore = score;
          }
        }
        break;
      }
      return finalScore;
    }

    ///// atom comparison methods //////
    
    /**
     * Used in Array.sort when an atom is set and Collection.sort when
     * determining the Mata like/unlike sequence for Rules 4b and 5. Includes a
     * preliminary check for duplicates, since we know that that atom will
     * ultimately be lower priority if all other rules are tied. 
     *      * 
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    @Override
    public int compareTo(CIPAtom b) {
      int score;
      return (root.rule4Ref == NO_CHIRALITY ?
         // standard constitutional presort - Rules 1a, 1b, and 2 
          (b == null ? A_WINS
              : (atom == null) != (b.atom == null) ? (atom == null ? B_WINS
                  : A_WINS) : (score = compareRule1a(b)) != TIED ? score
                  : (score = unlikeDuplicates(b)) != TIED ? score
                      : isDuplicate ? compareRule1b(b) : compareRule2(b)) 
         : 
         // Rule 4b/5 Mata list referenced sort -- first by sphere, then a
         // simple string sort
           sphere < b.sphere ? -1 : sphere > b.sphere ? 1 : chiralPath
              .compareTo(b.chiralPath));
    }

    /**
     * Check this atom "A" vs a challenger "B" against the current rule.
     * 
     * Note that example BB 2013 page 1258, P93.5.2.3 requires that RULE_1b be
     * applied only after RULE_1a has been applied exhaustively for a sphere;
     * otherwise C1 is R, not S (AY-236.53).
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS), or Intege.MIN_VALUE
     *         (IGNORE)
     */
    private int checkCurrentRule(CIPAtom b) {
      switch (currentRule) {
      default:
      case RULE_1a:
        return compareRule1a(b);
      case RULE_1b:
        return compareRule1b(b);
      case RULE_2:
        return compareRule2(b);
      case RULE_3:
        return compareRule3(b); // can be IGNORE
      case RULE_4a:
        return compareRules4ac(b, " sr SR PM");
      case RULE_4b:
      case RULE_5:
        // can be terminal when we are checking the two groups on an alkene end
        return (isTerminal || b.isTerminal ? TIED : compareRule4b5(b));
      case RULE_4c:
        return compareRules4ac(b, " s r p m");
      case RULE_6:
        return compareRule6(b);
      }
    }

    /**
     * This check is not technically one of those listed in the rules, but it is
     * useful when preparing to check substituents because if one of the atoms
     * has substituents and the other doesn't, we are done -- there is no reason
     * to check substituents.
     * 
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    private int unlikeDuplicates(CIPAtom b) {
      return b.isDuplicate == isDuplicate ? TIED : isDuplicate ? B_WINS : A_WINS;
    }

    /**
     * Looking for phantom atom (atom number 0) or element number
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    private int compareRule1a(CIPAtom b) {
      return b.atom == null ? A_WINS 
          : atom == null ? B_WINS
          : b.elemNo < elemNo ? A_WINS 
              : b.elemNo > elemNo ? B_WINS 
                  : TIED;
    }

    /**
     * Looking for root distance -- duplicate atoms only.
     * (We have checked for 
     * 
     * @param b
     * 
     * 
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */

    private int compareRule1b(CIPAtom b) {
      return b.isDuplicate != isDuplicate 
          ? TIED
          : Integer.compare(rootDistance, b.rootDistance);
    }

    /**
     * Chapter 9 Rule 2. atomic mass, with possible reversal due to use of mass
     * numbers.
     * 
     * Also checks for temporary Rule 6 promotion for full Rule 6 implemenation
     * (rr bicyclo[2.2.2]octane)
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    private int compareRule2(CIPAtom b) {
      return (atomIndex == b.atomIndex? TIED : getMass() > b.getMass() ? A_WINS
          : mass < b.mass ? B_WINS
              // full Rule 6 priority check is done here
              : root.rule6refIndex < 0 ? TIED 
              : !root.bsRule6Subs.get(atomIndex) 
              || !root.bsRule6Subs.get(b.atomIndex) ?
                  TIED
              : root.rule6refIndex == atomIndex ? A_WINS : root.rule6refIndex == b.atomIndex ? B_WINS : TIED);
    }

    /**
     * Chapter 9 Rule 3. E/Z.
     * 
     * We carry out this step only as a tie in the sphere AFTER the final atom
     * of the alkene or even-cumulene.
     * 
     * If the this atom and the comparison atom b are on the same parent, then
     * this is simply a request to break their tie based on Rule 3; otherwise
     * two paths are being checked for one being seqCis and one being seqTrans.
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    private int compareRule3(CIPAtom b) {
      return isDuplicate || b.isDuplicate || !parent.isAlkeneAtom2
          || !b.parent.isAlkeneAtom2 || !parent.alkeneParent.isEvenEne
          || !b.parent.alkeneParent.isEvenEne || parent == b.parent ? TIED
          : Integer.compare(parent.auxEZ, b.parent.auxEZ);
    }

    /**
     * Chapter 9 Rules 4a and 4c. This method allows for "RS" to be checked as
     * "either R or S". See AY236.66, AY236.67,
     * AY236.147,148,156,170,171,201,202, etc. (4a)
     * 
     * @param b
     * @param test
     *        String to test against; depends upon subrule being checked
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    private int compareRules4ac(CIPAtom b, String test) {
      if (isTerminal || isDuplicate)
        return TIED;
      int isRa = test.indexOf(auxChirality), isRb = test
          .indexOf(b.auxChirality);
      return (isRa > isRb + 1 ? A_WINS : isRb > isRa + 1 ? B_WINS : TIED);
    }

    /**
     * Compare the better R-ref or S-ref list for A with the same for B.
     * 
     * @param b
     * @return A_WINS, B_WINS, or IGNORE
     */
    private int compareRule4b5(CIPAtom b) {
      BS bsA = getBetter4bList();
      BS bsB = b.getBetter4bList();
      BS best = compareLikeUnlike(bsA, bsB);
      int score = (best == null ? IGNORE : best == bsA ? A_WINS : B_WINS);
      if (best != null) {
        if (currentRule == RULE_5) {
          // this is our check for self-enantiomorphic ligands
          // if the two ligands are each self-enatiomorphic (first chiral sphere r,s only, no R,S),
          // then we need to switch to R/S mode.
          // This is tested as (A(R) > B(R)) == (A(S) > B(S))
          if ((compareLikeUnlike(listRS[STEREO_S], b.listRS[STEREO_S]) == listRS[STEREO_S])
              == (best == bsA))
          parent.isRule5Pseudo = !parent.isRule5Pseudo;
        }
        if (doTrack)
          data.track(CIPChirality.this, this, b, 1, score, false);
      }
      return score;
    }

    /**
     * Just look for match with the Rule 6 reference atom index
     * 
     * @param b
     * @return A_WINS, B_WINS, or TIED
     */
    private int compareRule6(CIPAtom b) {
      return ((atomIndex == root.rule6refIndex) == (b.atomIndex == root.rule6refIndex) ? TIED
          : atomIndex == root.rule6refIndex ? A_WINS : B_WINS);
    }

    ////// Rule 4b, 5 processsing //////
    
    /**
     * Clear Rule 4b information if Rule-5 pseudochiral centers have been found,
     * as that could change the order of descriptors in the Mata list.
     */
    void clearRule4Lists() {
      listRS = null;
      for (int i = 0; i < 4 && atoms[i] != null; i++)
        atoms[i].clearRule4Lists();
    }

    /**
     * Create Mata-style linear atom.listRS and return the better of R-ref or S-ref.
     * Used first in Rule 4b and again in Rule 5. 
     * @return the better 4b list
     */
    private BS getBetter4bList() {
      if (listRS != null)
        return listRS[currentRule == RULE_5 ? STEREO_R : 0];
      BS bs;
      listRS = new BS[] { null, bs = rank4bAndRead(null), rank4bAndRead(bs)};
      Logger.info("getBest " + currentRule + " " + this + " " + listRS[STEREO_R] + listRS[STEREO_S] + " " + myPath);      
      bs = compareLikeUnlike(listRS[STEREO_R], listRS[STEREO_S]);
      return listRS[0] = (currentRule == RULE_5 || bs == null ? listRS[STEREO_R] : bs);
    }

    /**
     * A thread-based sphere-ordered implementation that 
     * takes into account that lists cross the boundaries of branches.
     * 
     * @param bsR
     *        null if this is for R or the R-ref list if this is for S
     * @return ranked list
     */
    private BS rank4bAndRead(BS bsR) {
      boolean isS = (bsR != null);
      int ref = (isS ? STEREO_S : STEREO_R);
      BS list = new BS();
      Lst<CIPAtom> chiralAtoms = new Lst<CIPAtom>();
      root.rule4Ref = ref;
      addChiralAtoms(chiralAtoms, ref);
      Collections.sort(chiralAtoms);
      root.rule4Ref = NO_CHIRALITY;
      for (int i = 0, n = chiralAtoms.size(); i < n; i++) {
        if (Logger.debugging)
          Logger.info("" + ref + " " + this + " " + chiralAtoms.get(i).chiralPath);
        if (chiralAtoms.get(i).rule4Type == ref)
          list.set(i);
      }
      return list;
    }

    /**
     * Create an ASCII string that allows the list of descriptors to be
     * generated in order. This list must take into account both the 
     * current (Rule 4a or Rule 4c) priority group (0 highest to 3 lowest)
     * of each point along the path back to the root for this node.
     * This is done by multiplying the priority group by 4 and adding in 
     * the like- unlike-ness of the node as one of 0 (ns), 1 (like), or 2 (unlike).
     * 
     * In this way, we can get cross-branch sorting, not just the typical 
     * breadth-first sorting that we usually use for ranking. For example,
     * in BH_64_085, two branches of a ligand each have two branches that
     * have identical Rule 4a priorities, leading to four branches that must be
     * sorted as the "highest-priority" set in Rule 4b.   
     * 
     * @param chiralAtoms
     * @param ref
     */
    private void addChiralAtoms(Lst<CIPAtom> chiralAtoms, int ref) {
      // encodings:
      //
      // @: 0-priority ns
      // A: 0-priority LIKE
      // B: 0-priority UNLIKE
      // C: n/a
      // D: 1-priority ns
      // E: 1-priority LIKE
      // F: 1-priority UNLIKE
      // G: n/a
      // H: 2-priority ns 
      // I: 2-priority LIKE
      // J: 2-priority UNLIKE
      // K: n/a
      // L: 3-priority ns
      // M: 3-priority LIKE
      // N: 3-priority UNLIKE
      
      // For example, in the processing of BH64_073 C28, we get for ligand C89
      // in Rule 4b:
      // R-ref:
      //      1 C89 @DA
      //      1 C89 @DADA
      //      1 C89 @DADB
      //      1 C89 @DADADA
      //      1 C89 @DADADB
      //      1 C89 @DADBDA
      //      1 C89 @DADBDB
      // S-ref:
      //      2 C89 @DB
      //      2 C89 @DBDA
      //      2 C89 @DBDB
      //      2 C89 @DBDADA
      //      2 C89 @DBDADB
      //      2 C89 @DBDBDA
      //      2 C89 @DBDBDB
      //
      // resulting R- and S-lists, from last descriptor only:
      // (R)llululu and (S)ulululu
      // 
      // and in BH64_085, we get different results for Rule 4b than for Rule 5:
      //
      // Rule 4b:
      // R-ref:
      //      1 N17 @D@@A
      //      1 N17 @D@@A
      //      1 N17 @D@@B
      //      1 N17 @D@@B
      //      1 N17 @D@@AE
      //      1 N17 @D@@AF
      //      1 N17 @D@@BE
      //      1 N17 @D@@BF
      //
      // giving AABBEFEF, or lluululu
      //
      // Rule 5, after a Rule 4c sort:     
      // R-ref:
      //      1 N17 @D@@A
      //      1 N17 @D@@B
      //      1 N17 @DD@A
      //      1 N17 @DD@B
      //      1 N17 @D@@AE
      //      1 N17 @D@@BF
      //      1 N17 @DD@AF
      //      1 N17 @DD@BE
      //
      // giving ABABEFFE or lululuul
      
      if (atom == null || isTerminal || isDuplicate)
        return;
      if (rule4Type != 0) {
        String s = "";
        CIPAtom a = this;
        while (a != null) {
          s = (char)(64 + (a.priority<<2) + (a.rule4Type == 0 ? 0 : a.rule4Type == ref ? 1 : 2)) + s;
          if ((a = a.parent) != null && a.chiralPath != null) {
            s = a.chiralPath + s;
            break;
          }
        }
        chiralPath = s;
        chiralAtoms.addLast(this);
      }
      for (int i = 0; i < 4; i++)
        if (atoms[i] != null)
          atoms[i].addChiralAtoms(chiralAtoms, ref);
    }

    private BS compareLikeUnlike(BS bsA, BS bsB) {
      BS bsXOR = (BS) bsB.clone();
      // A   = 1101111   // llullll 
      // B   = 1100111   // lluulll
      // xor = 0001000 
      bsXOR.xor(bsA);
      int l = bsXOR.nextSetBit(0);
      return (l < 0 ? null : bsA.get(l) ? bsA : bsB);
    }

    ///// auxiliary processing
    
    /**
     * By far the most complex of the methods, this method creates a list of
     * downstream (higher-sphere) auxiliary chirality designators, starting with
     * those furthest from the root and moving in, toward the root.
     * 
     * @param node1
     *        first node; sphere 1
     * @param ret
     *        CIPAtom of next stereochemical branching point
     * 
     * @return collective string, with setting of rule4List
     */
    boolean createAuxiliaryDescriptors(CIPAtom node1, CIPAtom[] ret) {
      boolean isChiralPath = false;
      char c = '~';
      if (atom == null)
        return false;
      setNode();
      int rs = -1, nRS = 0;
      CIPAtom[] ret1 = new CIPAtom[1];
      boolean skipRules4And5 = false;
      boolean prevIsChiral = true;
      // have to allow two same because could be a C3-symmetric subunit 
      boolean allowTwoSame = (!isAlkene && nPriorities <= (node1 == null ? 2
          : 1));
      for (int i = 0; i < 4; i++) {
        CIPAtom a = atoms[i];
        if (a != null && !a.isDuplicate && !a.isTerminal) {
          // we use ret1 to pass a reference to the next branch with two or more chiral paths
          ret1[0] = null;
          boolean aIsChiralPath = a.createAuxiliaryDescriptors(
              node1 == null ? a : node1, ret1);
          if (ret1[0] != null && ret != null)
            ret[0] = nextChiralBranch = a.nextChiralBranch;
          if (a.nextChiralBranch != null || aIsChiralPath) {
            nRS++;
            isChiralPath = aIsChiralPath;
            prevIsChiral = true;
          } else {
            if (!allowTwoSame && !prevIsChiral
                && priorities[i] == priorities[i - 1]) {
              return false;
            }
            prevIsChiral = false;
          }
        }
      }
      boolean isBranch = (nRS >= 2);
      switch (nRS) {
      case 0:
        isChiralPath = false;
        //$FALL-THROUGH$
      case 1:
        skipRules4And5 = true;
        break;
      case 2:
      case 3:
      case 4:
        isChiralPath = false;
        if (ret != null)
          ret[0] = nextChiralBranch = this;
        break;
      }
      if (isAlkene) {
        if (alkeneChild != null) {
          // must be alkeneParent -- first C of an alkene -- this is where C/T is recorded
          // All odd cumulenes need to be checked.
          // If it is an alkene or even cumulene, we must do an auxiliary check 
          // only if it is not already a defined stereochemistry, because in that
          // case we have a simple E/Z (c/t), and there is no need to check AND
          // it does not contribute to the Mata sequence (similar to r/s or m/p).
          //

          if (!isEvenEne || (auxEZ == STEREO_BOTH_EZ || auxEZ == UNDETERMINED)
              && !isKekuleAmbiguous && alkeneChild.bondCount >= 2) {
            int[] rule2 = (isEvenEne ? new int[1] : null);
            rs = getAuxEneWinnerChirality(this, alkeneChild, !isEvenEne, rule2);
            
            //
            // Note that we can have C/T (rule4Type = R/S):
            // 
            //    R      x
            //     \    /
            //       ==
            //     /    \
            //    S      root
            //
            // flips sense upon planar inversion; determination was Rule 5.
            //
            // and ALSO we can have c/t here that has not been discovered yet
            //
            //
            //   SR      x
            //     \    /
            //       ==
            //     /    \
            //   SS      root

            if (rs == NO_CHIRALITY) {
              auxEZ = alkeneChild.auxEZ = STEREO_BOTH_EZ;
            } else {
              isChiralPath = true;
              if (rule2 != null && rule2[0] != RULE_5) {
                // normal even-ene seqcis/seqtrans
                auxEZ = alkeneChild.auxEZ = rs;
                if (Logger.debuggingHigh)
                  Logger.info("alkene type " + this + " " + (auxEZ == STEREO_E ? "E" : "Z"));
              } else if (!isBranch) {
                // Normalize M/P and Z/E to R/S
                switch (rs) {
                case STEREO_M:
                case STEREO_Z:
                  rs = STEREO_R;
                  c = 'R';
                  isChiralPath = true;
                  break;
                case STEREO_P:
                case STEREO_E:
                  rs = STEREO_S;
                  c = 'S';
                  isChiralPath = true;
                  break;
                }
                auxChirality = c;
                rule4Type = rs;
              }
            }
          }
        }
      } else if (isSP3 && ret != null) {
        // if here, adj is TIED (0) or NOT_RELEVANT
        CIPAtom atom1 = (CIPAtom) clone();
        if (atom1.setNode()) {
          atom1.addReturnPath(null, this);
          int rule = RULE_1a; 
          for (; rule <= RULE_6; rule++) 
            // two C3 groups....
            if ((!skipRules4And5 || rule < RULE_4a || rule > RULE_5)
                && atom1.auxSort(rule))
              break;
          if (rule > RULE_6) {
            c = '~';
          } else {
            rs = data.checkHandedness(atom1);
            isChiralPath |= (rs != NO_CHIRALITY);
            c = (rs == STEREO_R ? 'R' : rs == STEREO_S ? 'S' : '~');
            if (rule == RULE_5) {
              c = (c == 'R' ? 'r' : c == 'S' ? 's' : '~');
              if (rs != NO_CHIRALITY)
                havePseudoAuxiliary = true;
            } else {
              rule4Type = rs;
            }
          }
        }
        auxChirality = c;
      }
      if (node1 == null)
        bsNeedRule.setBitTo(RULE_4a, nRS > 0);
      if (c != '~') {
        Logger.info("creating aux " + c + " for " + this + (myPath.length() == 0 ? "" : " = " + myPath));
      }
      return (this.isChiralPath = isChiralPath);
    }

    /**
     * Sort by a given rule, preserving currentRule, which could be 4 or 5
     * 
     * @param rule
     * @return true if a decision has been made
     */
    private boolean auxSort(int rule) {
      int current = currentRule;
      currentRule = rule;
      int rule6ref = root.rule6refIndex;
      int nDup = root.nRootDuplicates;
      boolean isChiral = (rule == RULE_6 ? 
        getRule6Descriptor(true) != NO_CHIRALITY : sortSubstituents(0));
      root.nRootDuplicates = nDup;
      root.rule6refIndex = rule6ref;
      currentRule = current;
      return isChiral;
    }

    /**
     * Determine the winner on one end of an alkene or cumulene and return also
     * the rule by which that was determined.
     * 
     * @param end1
     * @param end2
     * @param isAxial
     * @param retRule2
     *        return for rule found for child end (furthest from root)
     * @return one of: {NO_CHIRALITY | STEREO_Z | STEREO_E | STEREO_M |
     *         STEREO_P}
     */
    private int getAuxEneWinnerChirality(CIPAtom end1, CIPAtom end2,
                                         boolean isAxial, int[] retRule2) {
      if (isAxial && end1.nextSP2 == end2)
        return  NO_CHIRALITY; // allene terminating on root
      CIPAtom winner1 = getAuxEneEndWinner(end1, end1.nextSP2, null);
      CIPAtom winner2 = (winner1 == null || winner1.atom == null ? null
          : getAuxEneEndWinner(end2, end2.nextSP2, retRule2));
      if (Logger.debuggingHigh)
        Logger.info(this + " alkene end winners " + winner1 + winner2);
      return getEneChirality(winner1, end1, end2, winner2, isAxial, false);
    }
    
    /**
     * Get the atom that is the highest priority of two atoms on the end of a
     * double bond after sorting from Rule 1a through a given rule (Rule 3 or
     * Rule 5)
     * 
     * @param end
     * @param prevSP2
     * @param retRule
     *        return for deciding rule
     * @return higher-priority atom, or null if they are equivalent
     */
    private CIPAtom getAuxEneEndWinner(CIPAtom end, CIPAtom prevSP2,
                                       int[] retRule) {
      CIPAtom atom1 = (CIPAtom) end.clone();
      if (atom1.parent != prevSP2)
        atom1.addReturnPath(prevSP2, end);
      CIPAtom a;
      for (int rule = RULE_1a; rule <= RULE_6; rule++) {
        if (atom1.auxSort(rule)) {
          for (int i = 0; i < 4; i++) {
            a = atom1.atoms[i];
            if (!a.multipleBondDuplicate) {
              if (atom1.priorities[i] != atom1.priorities[i + 1]) {
                if (retRule != null)
                  retRule[0] = rule;
                return (a.atom == null ? null : a);
              }
            }
          }
        }
      }
      return null;
    }

    /**
     * Add the path back to the root for an auxiliary center.
     * 
     * @param newParent
     * @param fromAtom
     */
    private void addReturnPath(CIPAtom newParent, CIPAtom fromAtom) {
      Lst<CIPAtom> path = new Lst<CIPAtom>();
      CIPAtom thisAtom = this, newSub, oldParent = fromAtom, oldSub = newParent;
      // create path back to root
      while (oldParent.parent != null && oldParent.parent.atoms[0] != null) { // COUNT_LINE
        if (Logger.debuggingHigh)
          Logger.info("path:" + oldParent.parent + "->" + oldParent);
        path.addLast(oldParent = oldParent.parent);
      }
      path.addLast(null);
      for (int i = 0, n = path.size(); i < n; i++) {
        oldParent = path.get(i);
        newSub = (oldParent == null ? new CIPAtom().create(null, this,
            isAlkene, true, false) : (CIPAtom) oldParent.clone());
        newSub.nPriorities = 0;
        newSub.sphere = thisAtom.sphere + 1;
        thisAtom.replaceParentSubstituent(oldSub, newParent, newSub);
        if (i > 0 && thisAtom.isAlkene && !thisAtom.isAlkeneAtom2) {
          // reverse senses of alkenes
          if (newParent.isAlkeneAtom2) {
            newParent.isAlkeneAtom2 = false;
            thisAtom.alkeneParent = newParent;
          }
          thisAtom.setEne();
        }
        newParent = thisAtom;
        thisAtom = newSub;
        oldSub = fromAtom;
        fromAtom = oldParent;
      }
    }

    /**
     * Swap a substituent and the parent in preparation for reverse traversal of
     * this path back to the root atom.
     * 
     * @param oldSub
     * @param newParent
     * @param newSub
     */
    private void replaceParentSubstituent(CIPAtom oldSub, CIPAtom newParent,
                                          CIPAtom newSub) {
      for (int i = 0; i < 4; i++)
        if (atoms[i] == oldSub || newParent == null && atoms[i].atom == null) {
          if (Logger.debuggingHigh)
            Logger.info("reversed: " + newParent + "->" + this + "->" + newSub);
          parent = newParent;
          atoms[i] = newSub;
          Arrays.sort(atoms);
          break;
        }
    }

    ///// general utility methods
    
    /**
     * Just a simple signum for integers
     * 
     * @param score
     * @return 0, -1, or 1
     */
    private int sign(int score) {
      return (score < 0 ? -1 : score > 0 ? 1 : 0);
    }

    @Override
    public Object clone() {
      CIPAtom a = null;
      try {
        a = (CIPAtom) super.clone();
      } catch (CloneNotSupportedException e) {
      }
      a.id = ptIDLogger++;
      a.atoms = new CIPAtom[4];
      for (int i = 0; i < 4; i++)
        a.atoms[i] = atoms[i];
      a.priorities = new int[4];
      a.htPathPoints = htPathPoints;
      a.alkeneParent = null;
      a.auxEZ = UNDETERMINED;
      a.rule4Type = NO_CHIRALITY;
      a.listRS = null;
      if (Logger.debuggingHigh)
        a.myPath = a.toString();
      return a;
    }

    @Override
    public String toString() {
      if (atom == null)
        return "<null>";
      if (Logger.debuggingHigh)
        return ("[" + currentRule + "." + sphere
            + "," + id + "." + (isDuplicate ? parent.atom : atom).getAtomName()
            + (isDuplicate ? "*(" + rootDistance + ")" : "")
            + (auxChirality == '~' ? "" : "" + auxChirality) + " " + elemNo
            + "]");
      return (isDuplicate ? "(" + atom.getAtomName() + "."  + rootDistance + ")"
          : atom.getAtomName());

    }
  }
  
}
