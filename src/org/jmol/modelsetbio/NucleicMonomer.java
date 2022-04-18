/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-02-26 22:47:27 -0600 (Mon, 26 Feb 2007) $
 * $Revision: 6957 $

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
package org.jmol.modelsetbio;

import javajs.util.A4;
import javajs.util.Lst;
import javajs.util.M3d;
import javajs.util.P3;
import javajs.util.Quat;
import javajs.util.V3;

import org.jmol.c.STR;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.Chain;
import org.jmol.modelset.Group;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

public class NucleicMonomer extends PhosphorusMonomer {

  final static byte C6 = 1;
  private final static byte O2Pr = 2;
  private final static byte C5 = 3;
  private final static byte N1 = 4;
  private final static byte C2 = 5;
  private final static byte N3 = 6;
  private final static byte C4 = 7;
  private final static byte O2 = 8;
  private final static byte N7 = 9;
  private final static byte C8 = 10;
  private final static byte N9 = 11;  
  private final static byte O4 = 12;
  private final static byte O6 = 13;
  private final static byte N4 = 14;
  private final static byte NP = 15;
  private final static byte N6 = 16;
  private final static byte N2 = 17;
  private final static byte H5T = 18;
  private final static byte O5P = 19;
  private final static byte H3T = 20;
  private final static byte O3P = 21; 
  private final static byte C3P = 22;
  private final static byte O1P = 23; 
  private final static byte O2P = 24;
  private final static byte C1P = 25;
  private final static byte C2P = 26;
  private final static byte C4P = 27;
  private final static byte O4P = 28;
  private final static byte C5P = 29;
   
  // negative values are optional
  final static byte[] interestingNucleicAtomIDs = {
    ~JC.ATOMID_NUCLEIC_PHOSPHORUS,    //  the lead, POSSIBLY P, maybe O5' or O5T 
    JC.ATOMID_C6,   // 1 the wing man, c6

    ~JC.ATOMID_O2_PRIME, // 2  O2' for RNA

    JC.ATOMID_C5,   //  3 C5
    JC.ATOMID_N1,   //  4 N1
    JC.ATOMID_C2,   //  5 C2
    JC.ATOMID_N3,   //  6 N3
    JC.ATOMID_C4,   //  7 C4

    ~JC.ATOMID_O2,  //  8 O2

    ~JC.ATOMID_N7,  // 9 N7
    ~JC.ATOMID_C8,  // 10 C8
    ~JC.ATOMID_N9,  // 11 C9

    ~JC.ATOMID_O4,  // 12 O4   U (& ! C5M)
    ~JC.ATOMID_O6,  // 13 O6   I (& ! N2)
    ~JC.ATOMID_N4,  // 14 N4   C
    ~JC.ATOMID_NUCLEIC_PHOSPHORUS, // 15 
    ~JC.ATOMID_N6,  // 16 N6   A
    ~JC.ATOMID_N2,  // 17 N2   G

    ~JC.ATOMID_H5T_TERMINUS, // 18 H5T terminus
    ~JC.ATOMID_O5_PRIME,     // 19 O5' terminus

    ~JC.ATOMID_H3T_TERMINUS, // 20 H3T terminus
    JC.ATOMID_O3_PRIME,      // 21 O3' terminus
    JC.ATOMID_C3_PRIME,      // 22 C3'
    
    ~JC.ATOMID_O1P,  // 23 Phosphorus O1
    ~JC.ATOMID_O2P,  // 24 Phosphorus O2

    ~JC.ATOMID_C1_PRIME,  // 25 ribose C1'
    ~JC.ATOMID_C2_PRIME,  // 26 ribose C2'
    ~JC.ATOMID_C4_PRIME,  // 27 ribose C4'
    ~JC.ATOMID_O4_PRIME,  // 28 ribose O4'
    ~JC.ATOMID_C5_PRIME,  // 29 ribose C5'

    // unused:

    //~JmolConstants.ATOMID_S4,  // 15 S4   tU
    //~JmolConstants.ATOMID_O5T_TERMINUS, // 26 O5T terminus

    // alternative designations:
    
    //~JmolConstants.ATOMID_OP1,  // 27 Phosphorus O1 (new)
    //~JmolConstants.ATOMID_OP2,  // 28 Phosphorus O2 (new)

    //~JmolConstants.ATOMID_HO3_PRIME, // 29 HO3' terminus (new)
    //~JmolConstants.ATOMID_HO5_PRIME, // 29 HO3' terminus (new)
    
  };

  private NucleicMonomer() {}
  
  private boolean isPurine;
  boolean isPyrimidine;
  public static Monomer
    validateAndAllocate(Chain chain, String group3, int seqcode,
                        int firstAtomIndex, int lastAtomIndex,
                        int[] specialAtomIndexes) {

    byte[] offsets = scanForOffsets(firstAtomIndex,
                                    specialAtomIndexes,
                                    interestingNucleicAtomIDs);

    if (offsets == null)
      return null;
    
    // Actually, O5T is never tested; it MUST have O5P. 
    // O5P is part of the mask that got us here.
    if (!checkOptional(offsets, O5P, firstAtomIndex, 
        specialAtomIndexes[JC.ATOMID_O5T_TERMINUS]))
      return null;
    checkOptional(offsets, H3T, firstAtomIndex, 
        specialAtomIndexes[JC.ATOMID_HO3_PRIME]);
    checkOptional(offsets, H5T, firstAtomIndex, 
        specialAtomIndexes[JC.ATOMID_HO5_PRIME]);
    checkOptional(offsets, O1P, firstAtomIndex, 
        specialAtomIndexes[JC.ATOMID_OP1]);
    checkOptional(offsets, O2P, firstAtomIndex, 
        specialAtomIndexes[JC.ATOMID_OP2]);

    return (new NucleicMonomer()).set4(chain, group3, seqcode,
                         firstAtomIndex, lastAtomIndex, offsets);
  }

  ////////////////////////////////////////////////////////////////

  private boolean hasRnaO2Prime;

  private NucleicMonomer set4(Chain chain, String group3, int seqcode,
                 int firstAtomIndex, int lastAtomIndex,
                 byte[] offsets) {
    set2(chain, group3, seqcode,
          firstAtomIndex, lastAtomIndex, offsets);
    if (!have(offsets, NP)) {
      offsets[0] = offsets[O5P];
      setLeadAtomIndex();
    }
    hasRnaO2Prime = have(offsets, O2Pr);
    isPyrimidine = have(offsets, O2);
    isPurine = have(offsets, N7) && have(offsets, C8) && have(offsets, N9);
    return this;
  }

  @Override
  public boolean isNucleicMonomer() { return true; }

  @Override
  public boolean isDna() { 
    return !hasRnaO2Prime; 
    }

  @Override
  public boolean isRna() { 
    return hasRnaO2Prime; 
    }

  @Override
  public boolean isPurine() { return isPurine || !isPyrimidine && isPurineByID(); }

  @Override
  public boolean isPyrimidine() { return isPyrimidine || !isPurine && isPyrimidineByID(); }

  public boolean isGuanine() { return have(offsets, N2); }

  @Override
  public STR getProteinStructureType() {
    return (hasRnaO2Prime
            ? STR.RNA
            : STR.DNA);
  }

    ////////////////////////////////////////////////////////////////

  Atom getP() {
    return getAtomFromOffsetIndex(P);
  }

  public Atom getC1P() {
    return getAtomFromOffsetIndex(C1P);
  }

  Atom getC2() {
    return getAtomFromOffsetIndex(C2);
  }

  Atom getC5() {
    return getAtomFromOffsetIndex(C5);
  }

  Atom getC6() {
    return getAtomFromOffsetIndex(C6);
  }

  Atom getC8() {
    return getAtomFromOffsetIndex(C8);
  }

  Atom getC4P() {
    return getAtomFromOffsetIndex(C4P);
  }

  Atom getN1() {
    return getAtomFromOffsetIndex(N1);
  }

  public Atom getN3() {
    return getAtomFromOffsetIndex(N3);
  }

  Atom getN2() {
    return getAtomFromOffsetIndex(N2);
  }

  Atom getN4() {
    return getAtomFromOffsetIndex(N4);
  }

  Atom getN6() {
    return getAtomFromOffsetIndex(N6);
  }

  Atom getO2() {
    return getAtomFromOffsetIndex(O2);
  }

  Atom getO4() {
    return getAtomFromOffsetIndex(O4);
  }

  Atom getO6() {
    return getAtomFromOffsetIndex(O6);
  }

  @Override
  Atom getTerminatorAtom() {
    return getAtomFromOffsetIndex(have(offsets, H3T) ? H3T : O3P);
  }

  private final static byte[] ring6OffsetIndexes = {C5, C6, N1, C2, N3, C4};
  private final static byte[] ring5OffsetIndexes = {C5, N7, C8, N9, C4};
  private final static byte[] riboseOffsetIndexes = {C1P, C2P, C3P, C4P, O4P, O3P, C5P, O5P, P};
  public void getBaseRing6Points(P3[] pts) {
    getPoints(ring6OffsetIndexes, pts);
  }
  
  private void getPoints(byte[] a, P3[] pts) {
    for (int i = a.length; --i >= 0;)
      pts[i] = getAtomFromOffsetIndex(a[i]);
  }

  public boolean maybeGetBaseRing5Points(P3[] pts) {
    if (isPurine)
      getPoints(ring5OffsetIndexes, pts);
    return isPurine;
  }

  public void getRiboseRing5Points(P3[] pts) {
    getPoints(riboseOffsetIndexes, pts);
  }
  
  private final static byte[] heavyAtomIndexes = {
    /*C1P Sarver: apparently not!,*/ 
    C5, C6, N1, C2, N3, C4, // all
    N9, C8, N7, // purine
    N6, // A
    N4, // C
    O2, // C, T, U
    O4, // T, U
    N2, O6, // G
    };

  ////////////////////////////////////////////////////////////////

  @Override
  boolean isConnectedAfter(Monomer possiblyPreviousMonomer) {
    if (possiblyPreviousMonomer == null)
      return true;
    Atom myPhosphorusAtom = getAtomFromOffsetIndex(NP);
    if (myPhosphorusAtom == null)
      return false;
    return (((NucleicMonomer) possiblyPreviousMonomer).getAtomFromOffsetIndex(
        O3P).isBonded(myPhosphorusAtom) || isCA2(possiblyPreviousMonomer));
  }

  ////////////////////////////////////////////////////////////////

  @Override
  public void findNearestAtomIndex(int x, int y, Atom[] closest,
                            short madBegin, short madEnd) {
    Atom competitor = closest[0];
    Atom lead = getLeadAtom();
    Atom o5prime = getAtomFromOffsetIndex(O5P);
    Atom c3prime = getAtomFromOffsetIndex(C3P);
    short mar = (short)(madBegin / 2);
    if (mar < 1900)
      mar = 1900;
    int radius = (int) scaleToScreen(lead.sZ, mar);
    if (radius < 4)
      radius = 4;
    if (isCursorOnTopOf(lead, x, y, radius, competitor)
        || isCursorOnTopOf(o5prime, x, y, radius, competitor)
        || isCursorOnTopOf(c3prime, x, y, radius, competitor))
      closest[0] = lead;
  }
  
  public void setRingsVisible(boolean isVisible) {
    for (int i = 6; --i >= 0;)
      getAtomFromOffsetIndex(ring6OffsetIndexes[i]).setShapeVisibility(JC.VIS_CARTOON_FLAG, isVisible);
    if (isPurine)
      for (int i = 4; --i >= 1;)
        getAtomFromOffsetIndex(ring5OffsetIndexes[i]).setShapeVisibility(JC.VIS_CARTOON_FLAG, isVisible);
  }

  public void setRingsClickable() {
    // this also sets them VISIBLE
    for (int i = 6; --i >= 0;)
      getAtomFromOffsetIndex(ring6OffsetIndexes[i]).setClickable(JC.VIS_CARTOON_FLAG);
    if (isPurine)
      for (int i = 4; --i >= 1;)
        getAtomFromOffsetIndex(ring5OffsetIndexes[i]).setClickable(JC.VIS_CARTOON_FLAG);
  }
 
  public Atom getN0() {
    return (getAtomFromOffsetIndex(isPurine ? N9 : N1));
  }
 
  @Override
  public Object getHelixData(int tokType, char qType, int mStep) {
    return getHelixData2(tokType, qType, mStep);
  }
   
  P3 baseCenter;

  @Override
  P3 getQuaternionFrameCenter(char qType) {
    switch (qType) {
    case 'x':
    case 'a':
    case 'b':
    case 'p':
      return getP();
    case 'c':
      // Sarver's base center; does not include C4'
      if (baseCenter == null) {
        int n = 0;
        baseCenter = new P3();
        for (int i = 0; i < heavyAtomIndexes.length; i++) {
          Atom a = getAtomFromOffsetIndex(heavyAtomIndexes[i]);
          if (a == null)
            continue;
          baseCenter.add(a);
          n++;
        }
        baseCenter.scale(1f / n);
      }
      return baseCenter;
    case 'n':
    default:
      return getN0();
    }
  }

  @Override
  public Quat getQuaternion(char qType) {
    if (bioPolymer == null)
      return null;
    // quaternionFrame 'c' from  
    // Sarver M, Zirbel CL, Stombaugh J, Mokdad A, Leontis NB. 
    // FR3D: finding local and composite recurrent structural motifs in RNA 3D structures. 
    // J. Math. Biol. (2006) 215-252
    // quaternionFrame 'n' same, but with N1/N9 as base atom (only different for DRAW)
    Atom ptA = null, ptB = null, ptNorP;
    boolean yBased = false;
    boolean reverseY = false;
    switch (qType) {
    case 'a': // alternative C4' - P - C4'
      //   (C4_i-1 - P_i - C4_i), with Y P_i - C4_i      
      ptNorP = getP();
      if (monomerIndex == 0 || ptNorP == null)
        return null;
      yBased = true;
      ptA = ((NucleicMonomer) bioPolymer.monomers[monomerIndex - 1]).getC4P();
      ptB = getC4P();
      break;
    case 'x':
      // P[i]-C4'[i]-P[i+1]
      ptNorP = getP();
      if (monomerIndex == bioPolymer.monomerCount - 1 || ptNorP == null)
        return null;
      ptA = ((NucleicMonomer) bioPolymer.monomers[monomerIndex + 1]).getP();
      ptB = getC4P();
      break;
    case 'b': // phosphorus backbone
      return getQuaternionP();
    case 'c': // Sarver-defined, with Y in the C1'-N1/9 direction, x toward C2 (W-C edge) 
    case 'n': // same, just different quaternion center
      // N0 = (purine N9, pyrimidine N1): 
      ptNorP = getN0();
      if (ptNorP == null)
        return null;
      yBased = true;
      reverseY = true;
      // vB = -(N0-C1P)
      // vA = vB x (vB x (N0-C2))
      ptA = getAtomFromOffsetIndex(C2);
      ptB = getAtomFromOffsetIndex(C1P);
      break;
    case 'p': // phosphorus tetrahedron
      // O1P - P - O2P
      ptNorP = getP();
      if (ptNorP == null)
        return null;
      Atom p1 = getAtomFromOffsetIndex(O1P);
      Atom p2 = getAtomFromOffsetIndex(O2P);
      Bond[] bonds = ptNorP.bonds;
      if (bonds == null)
        return null;
      Group g = ptNorP.group;
      for (int i = 0; i < bonds.length; i++) {
        Atom atom = bonds[i].getOtherAtom(ptNorP);
        if (p1 != null && atom.i == p1.i)
          continue;
        if (p2 != null && atom.i == p2.i)
          continue;
        if (atom.group == g)
          ptB = atom;
        else
          ptA = atom;
      }
      break;
    case 'q': // Quine
      return null;
    default:
      ptNorP = getN0();
      if (ptNorP == null)
        return null;
      if (isPurine) {
        // 11.9.34 experimenting:
        // vA = N9--C2 // was N9--C4
        // vB = N9--N7 // was N9--C8
        ptA = getAtomFromOffsetIndex(C2);
        ptB = getAtomFromOffsetIndex(N7);
      } else {
        // 11.9.34 experimenting:
        // vA = N1--N3 // was N1--C2
        // vB = N1--C6
        ptA = getAtomFromOffsetIndex(N3);
        ptB = getAtomFromOffsetIndex(C6);
      }
      break;
    }
    if (ptA == null || ptB == null)
      return null;

    V3 vA = V3.newVsub(ptA, ptNorP);
    V3 vB = V3.newVsub(ptB, ptNorP);
    if (reverseY)
      vB.scale(-1);
    return Quat.getQuaternionFrameV(vA, vB, null, yBased);
  }
 
 @Override
public boolean isCrossLinked(Group g) {
    if (!(g instanceof NucleicMonomer) || isPurine == g.isPurine())
      return false;
    NucleicMonomer otherNucleotide = (isPurine ? (NucleicMonomer) g : this);
    NucleicMonomer myNucleotide = (isPurine ? this : (NucleicMonomer) g);
    Atom myN1 = myNucleotide.getN1();
    Atom otherN3 = otherNucleotide.getN3();
    return (myN1.isBonded(otherN3));
  }
 
  @Override
  public boolean getCrossLinkVector(Lst<Integer> vReturn, boolean crosslinkCovalent, boolean crosslinkHBond) {
    if (!crosslinkHBond)
      return false;
    Atom N = (isPurine ? getN1() : getN3());
    //System.out.println(N.getInfo());
    Bond[] bonds = N.bonds;
    if (bonds == null)
      return false;
    for (int i = 0; i < bonds.length; i++) {
      //System.out.println(bonds[i].getOtherAtom(N).getInfo());
      if (bonds[i].isHydrogen()) {
        Atom N2 = bonds[i].getOtherAtom(N);
        Group g = N2.group;
        if (!(g instanceof NucleicMonomer))
          continue;
        NucleicMonomer m = (NucleicMonomer) g;
        if ((isPurine ? m.getN3() : m.getN1()) == N2) {
          if (vReturn == null)
            return true;
          vReturn.addLast(Integer.valueOf(N.i));
          vReturn.addLast(Integer.valueOf(N2.i));
          vReturn.addLast(Integer.valueOf(m.leadAtomIndex));
        }
      }
    }
    return vReturn != null && vReturn.size() > 0;
  }

  public boolean getEdgePoints(P3[] pts) {
    pts[0] = getLeadAtom();
    pts[1] = getC4P();
    pts[2] = pts[5] = getC1P();
    switch (getGroup1()) {
    case 'C':
      pts[3] = getO2();
      pts[4] = getN4();
      return true;
    case 'A':
      pts[3] = getC2();
      pts[4] = getN6();
      return true;
    case 'G':
    case 'I':
      pts[3] = getC2();
      pts[4] = getO6();
      return true;
    case 'T':
    case 'U':
      pts[3] = getO2();
      pts[4] = getO4();
      return true;
    default:
      return false;
    }    
  }

  private Lst<BasePair> bps;
  public P3[] dssrBox;
  public float dssrBoxHeight;
  public P3[] dssrFrame;
  
  public void addBasePair(BasePair bp) {
    if (bps == null)
      bps = new Lst<BasePair>();
    bps.addLast(bp);
  }

  public void setGroup1(char g) {
    // once only
    if (group1 == '\0')
      group1 = g;
  }
  
  /**
   * 
   * @return list of base pairs associated with this monomer, possibly more than one if noncanonical
   */
  public Lst<BasePair> getBasePairs() {
    if (bioPolymer != null && !((NucleicPolymer) bioPolymer).isDssrSet)
      bioPolymer.model.ms.vwr.getAnnotationParser(true).getBasePairs(bioPolymer.model.ms.vwr, bioPolymer.model.modelIndex);    
    return bps;
  }

  @Override
  protected char getGroup1b () {
    String g3 = Group.group3Names[groupID];
    String g1 = (NucleicPolymer.htGroup1 == null ? null : NucleicPolymer.htGroup1.get(g3));
    return (g1 == null ? Character.toLowerCase(g3.charAt(g3.length() - 1)) : g1.charAt(0));
  }

  public P3[] getDSSRFrame(Viewer vwr) {
    if (dssrFrame != null)
      return dssrFrame;
    if (dssrNT != null)
      return dssrFrame = vwr.getAnnotationParser(true).getDSSRFrame(dssrNT);
    P3[] oxyz = dssrFrame = new P3[4];
    for (int i = 4; --i >= 0;)
      oxyz[i] = new P3();
    if (isPurine()) {
      P3 v85 = P3.newP(getC5());
      v85.sub(getC8());
      v85.normalize();
      oxyz[2].setT(v85);
      oxyz[2].scale(-1);
      oxyz[0].scaleAdd2(4.9f, v85, getC8());
      P3 v89 = P3.newP(getN0());
      v89.sub(getC8());
      oxyz[3].cross(v89, v85);
      oxyz[3].normalize();
    } else {
      P3 v61 = P3.newP(getN0());
      v61.sub(getC6());
      P3 v65 = P3.newP(getC5());
      v65.sub(getC6());
      oxyz[3].cross(v61, v65);
      oxyz[3].normalize();
      oxyz[2].setT(v61);
      oxyz[2].normalize();
      A4 aa = A4.new4(oxyz[3].x, oxyz[3].y, oxyz[3].z,
          (float) (66.6 * Math.PI / 180));
      M3d m3 = new M3d();
      m3.setAA(aa);
      m3.rotate(oxyz[2]);
      oxyz[0].scaleAdd2(5.1f, oxyz[2], getC6());
      oxyz[2].scale(-1);
    }
    oxyz[1].cross(oxyz[2], oxyz[3]);
    return dssrFrame;
  }
}
