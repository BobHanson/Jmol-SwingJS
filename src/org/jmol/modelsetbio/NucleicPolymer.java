/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-30 14:45:19 -0500 (Sat, 30 Sep 2006) $
 * $Revision: 5781 $
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

import java.util.Map;

import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.HBond;
import org.jmol.script.T;
import org.jmol.util.C;
import org.jmol.util.Edge;
import javajs.util.Lst;


import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.V3;


public class NucleicPolymer extends PhosphorusPolymer {

  public boolean isDssrSet;
  public static Map<String, String> htGroup1;
  
  NucleicPolymer(Monomer[] monomers) {
    super(monomers);
    type = TYPE_NUCLEIC;
    hasWingPoints = true;
  }

  Atom getNucleicPhosphorusAtom(int monomerIndex) {
    return monomers[monomerIndex].getLeadAtom();
  }

  @Override
  protected boolean calcEtaThetaAngles() {
    //  Carlos M. Duarte, Leven M. Wadley, and Anna Marie Pyle
    // RNA structure comparison, motif search and discovery using a reduced 
    // representation of RNA conformational space
    // Nucleic Acids Research, 2003, Vol. 31, No. 16 4755-4761
    //   eta (C4_i-1 - P_i - C4_i - P_i+1) 
    // theta (P_i - C4_i - P_i+1 - C4_i+1)

    float eta = Float.NaN;
    for (int i = 0; i < monomerCount - 2; ++i) {
      NucleicMonomer m1 = (NucleicMonomer) monomers[i];
      NucleicMonomer m2 = (NucleicMonomer) monomers[i + 1];
      P3 p1 = m1.getP();
      P3 c41 = m1.getC4P();
      P3 p2 = m2.getP();
      P3 c42 = m2.getC4P();
      if (i > 0) {
        NucleicMonomer m0 = (NucleicMonomer) monomers[i - 1];
        P3 c40 = m0.getC4P();
        eta = Measure.computeTorsion(c40, p1, c41, p2, true);
      }
      float theta = Measure.computeTorsion(p1, c41, p2, c42, true);
      if (eta < 0)
        eta += 360;
      if (theta < 0)
        theta += 360;
      m1.setGroupParameter(T.eta, eta);
      m1.setGroupParameter(T.theta, theta);
      //System.out.println("m1 " + i + " " + eta + " " + theta);
    }
    return true;
  }
  
  @Override
  public void calcRasmolHydrogenBonds(BioPolymer polymer, BS bsA, 
                                      BS bsB, Lst<Bond> vAtoms,
                                      int nMaxPerResidue, int[][][] min, 
                                      boolean checkDistances, boolean dsspIgnoreHydrogens) {
    NucleicPolymer other = (NucleicPolymer) polymer;
    V3 vNorm = new V3();
    V3 vAB = new V3();
    for (int i = monomerCount; --i >= 0;) {
      NucleicMonomer myNucleotide = (NucleicMonomer) monomers[i];
      if (!myNucleotide.isPurine())
        continue;
      Atom myN3 = myNucleotide.getN3();
      boolean isInA = bsA.get(myN3.i);
      if (!isInA && !bsB.get(myN3.i))
        continue;
      Atom myN1 = myNucleotide.getN1();
      Atom myN9 = myNucleotide.getN0();
      P4 plane  = Measure.getPlaneThroughPoints(myN3, myN1, myN9, vNorm,
          vAB, new P4());
      Atom bestN3 = null;
      float minDist2 = 25;
      NucleicMonomer bestNucleotide = null;
      for (int j = other.monomerCount; --j >= 0;) {
        NucleicMonomer otherNucleotide = (NucleicMonomer) other.monomers[j];
        if (!otherNucleotide.isPyrimidine)
          continue;
        Atom otherN3 = otherNucleotide.getN3();
        if (isInA ? !bsB.get(otherN3.i) : !bsA.get(otherN3.i))
          continue;
        Atom otherN1 = otherNucleotide.getN0();
        float dist2 = myN1.distanceSquared(otherN3);
        if (dist2 < minDist2 && myN9.distanceSquared(otherN1) > 50 // not stacked
            && Math.abs(Measure.distanceToPlane(plane, otherN3)) < 1 // in plane
        ) {
          bestNucleotide = otherNucleotide;
          bestN3 = otherN3;
          minDist2 = dist2;
        }
      }
      int n = 0;
      if (bestN3 != null) {
        n += addHydrogenBond(vAtoms, myN1, bestN3);
        if (n >= nMaxPerResidue)
          continue;
        if (myNucleotide.isGuanine()) {
          n += addHydrogenBond(vAtoms, myNucleotide.getN2(), bestNucleotide
              .getO2());
          if (n >= nMaxPerResidue)
            continue;
          n += addHydrogenBond(vAtoms, myNucleotide.getO6(), bestNucleotide
              .getN4());
          if (n >= nMaxPerResidue)
            continue;
        } else {
          n += addHydrogenBond(vAtoms, myNucleotide.getN6(), bestNucleotide
              .getO4());
        }
      }
    }
  }

  static protected int addHydrogenBond(Lst<Bond> vAtoms, Atom atom1, Atom atom2) {
    if (atom1 == null || atom2 == null)
      return 0;
    vAtoms.addLast(new HBond(atom1, atom2, Edge.BOND_H_NUCLEOTIDE, (short) 1, C.INHERIT_ALL, 0));
    return 1;
  }

}
