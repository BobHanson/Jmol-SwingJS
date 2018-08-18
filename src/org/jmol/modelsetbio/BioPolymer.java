/* $RCSfile$
* $Author: hansonr $
* $Date: 2007-04-24 08:15:07 -0500 (Tue, 24 Apr 2007) $
* $Revision: 7479 $
*
* Copyright (C) 2002-2005  The Jmol Development Team
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

import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.Model;
import org.jmol.modelset.Structure;

import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.V3;

import org.jmol.script.T;


/**
 * A "BioPolymer" is a constructed set of contiguous (probably connected) "Monomers",
 * which may be one of Alpha (Calpha atoms), Amino (Calpha + backbone), 
 * Phosphorus (P atoms),  Nucleic (DNA/RNA), or Carbohydrate.
 * 
 * BioPolymers are constructed after file loading and after various changes that might
 * affect secondary structure.
 * 
 * BioPolymers are not Chains. Chains are set at load time and just constitute 
 * collections of unique chain identifiers in the file. 
 * 
 */
public abstract class BioPolymer implements Structure {

  protected BioPolymer() {
    
  }
  
  public Model model;

  public Monomer[] monomers;
  protected boolean hasStructure;

  // these arrays will be one longer than the polymerCount
  // we probably should have better names for these things
  // holds center points between alpha carbons or sugar phosphoruses

  protected P3[] leadMidpoints;
  protected P3[] leadPoints;
  protected P3[] controlPoints;
  // holds the vector that runs across the 'ribbon'
  protected V3[] wingVectors;

  protected int[] leadAtomIndices;

  protected int type = TYPE_NOBONDING;
  public int bioPolymerIndexInModel;
  public int monomerCount;

  protected final static int TYPE_NOBONDING = 0; // could be phosphorus or alpha
  protected final static int TYPE_AMINO = 1;
  protected final static int TYPE_NUCLEIC = 2;
  protected final static int TYPE_CARBOHYDRATE = 3;

  public int cyclicFlag;
  
  protected void set(Monomer[] monomers) {
    this.monomers = monomers;
    monomerCount = monomers.length;
    for (int i = monomerCount; --i >= 0;)
      monomers[i].setBioPolymer(this, i);
    model = monomers[0].getModel();
  }
  
  @Override
  public void setAtomBits(BS bs) {
    getRange(bs, true);
  }
  
  @Override
  public void setAtomBitsAndClear(BS bs, BS bsOut) {
    for (int i = monomerCount; --i >= 0;)
      monomers[i].setAtomBitsAndClear(bs, bsOut);
  }
  
  public void getRange(BS bs, boolean isMutated) {
    // this is OK -- doesn't relate to added hydrogens
    if (monomerCount == 0)
      return;
    if (isMutated) {
      for (int i = monomerCount; --i >= 0;)
        monomers[i].setAtomBits(bs);
    } else {
      bs.setBits(monomers[0].firstAtomIndex,
          monomers[monomerCount - 1].lastAtomIndex + 1);
    }
  }

  public void clearStructures() {
  }

  public int[] getLeadAtomIndices() {
    if (leadAtomIndices == null) {
      leadAtomIndices = new int[monomerCount];
      invalidLead = true;
    }
    if (invalidLead) {
      for (int i = monomerCount; --i >= 0;)
        leadAtomIndices[i] = monomers[i].leadAtomIndex;
      invalidLead = false;
    }
    return leadAtomIndices;
  }

  protected int getIndex(int chainID, int seqcode, int istart, int iend) {
    int i;
    for (i = monomerCount; --i >= 0;) {
      Monomer m = monomers[i];
      if (m.chain.chainID == chainID && m.seqcode == seqcode
          && (istart < 0 || istart == m.firstAtomIndex || iend == m.lastAtomIndex))
        break;
    }
    return i;
  }

  final P3 getLeadPoint(int monomerIndex) {
    return monomers[monomerIndex].getLeadAtom();
  }

//  void getLeadPoint2(int groupIndex, Point3f midPoint) {
//    if (groupIndex == monomerCount)
//      --groupIndex;
//    midPoint.setT(getLeadPoint(groupIndex));
//  }

  private final P3 getInitiatorPoint() {
    return monomers[0].getInitiatorAtom();
  }

  private final P3 getTerminatorPoint() {
    return monomers[monomerCount - 1].getTerminatorAtom();
  }

  void getLeadMidPoint(int i, P3 midPoint) {
    if (i == monomerCount) {
      --i;
    } else if (i > 0) {
      midPoint.ave(getLeadPoint(i), getLeadPoint(i - 1));
      //System.out.println("bp leadmidpoint for axis: " + i + " " + monomers[i-1] + " " + monomers[i] + " " + midPoint);
      return;
    }
    midPoint.setT(getLeadPoint(i));
  }

  // this might change in the future ... if we calculate a wing point
  // without an atom for an AlphaPolymer
  final P3 getWingPoint(int polymerIndex) {
    return monomers[polymerIndex].getWingAtom();
  }

  public void setConformation(BS bsSelected) {
    Atom[] atoms = model.ms.at;
    for (int i = monomerCount; --i >= 0;)
      monomers[i].updateOffsetsForAlternativeLocations(atoms, bsSelected);
    recalculateLeadMidpointsAndWingVectors();
  }

  private boolean invalidLead;
  protected boolean invalidControl = false;

  public void recalculateLeadMidpointsAndWingVectors() {
    //System.out.println("biopo recalcLeadMP");
    invalidLead = invalidControl = true;
    getLeadAtomIndices();
    resetHydrogenPoints();
    calcLeadMidpointsAndWingVectors();
  }

  protected void resetHydrogenPoints() {
    // amino polymer only
  }

  public P3[] getLeadMidpoints() {
    if (leadMidpoints == null)
      calcLeadMidpointsAndWingVectors();
    return leadMidpoints;
  }

  P3[] getLeadPoints() {
    if (leadPoints == null)
      calcLeadMidpointsAndWingVectors();
    return leadPoints;
  }

  public P3[] getControlPoints(boolean isTraceAlpha, float sheetSmoothing,
                                    boolean invalidate) {
    if (invalidate)
      invalidControl = true;
    return (!isTraceAlpha ? leadMidpoints : sheetSmoothing == 0 ? leadPoints
        : getControlPoints2(sheetSmoothing));
  }

  protected float sheetSmoothing;

  private P3[] getControlPoints2(float sheetSmoothing) {
    if (!invalidControl && sheetSmoothing == this.sheetSmoothing)
      return controlPoints;
    getLeadPoints();
    V3 v = new V3();
    if (controlPoints == null)
      controlPoints = new P3[monomerCount + 1];
    if (!Float.isNaN(sheetSmoothing))
      this.sheetSmoothing = sheetSmoothing;
    for (int i = 0; i < monomerCount; i++)
      controlPoints[i] = getControlPoint(i, v);
    controlPoints[monomerCount] = getTerminatorPoint();
    //controlPoints[monomerCount] = controlPoints[monomerCount - 1];
    invalidControl = false;
    return controlPoints;
  }

  /**
   * 
   * @param i
   * @param v
   * @return the leadPoint unless a protein sheet residue (see AlphaPolymer)
   */
  protected P3 getControlPoint(int i, V3 v) {
    return leadPoints[i];
  }

  public final V3[] getWingVectors() {
    if (leadMidpoints == null) // this is correct ... test on leadMidpoints
      calcLeadMidpointsAndWingVectors();
    return wingVectors; // wingVectors might be null ... before autocalc
  }

  protected boolean hasWingPoints; // true for nucleic and SOME amino

  public BS reversed;

  public boolean twistedSheets;

  private final void calcLeadMidpointsAndWingVectors() {
    if (leadMidpoints == null) {
      leadMidpoints = new P3[monomerCount + 1];
      leadPoints = new P3[monomerCount + 1];
      wingVectors = new V3[monomerCount + 1];
      sheetSmoothing = PT.FLOAT_MIN_SAFE;
    }
    if (reversed == null)
      reversed = BS.newN(monomerCount);
    else
      reversed.clearAll();
    twistedSheets = model.ms.vwr.getBoolean(T.twistedsheets);
    V3 vectorA = new V3();
    V3 vectorB = new V3();
    V3 vectorC = new V3();
    V3 vectorD = new V3();

    P3 leadPointPrev, leadPoint;
    leadMidpoints[0] = getInitiatorPoint();
    leadPoints[0] = leadPoint = getLeadPoint(0);
    V3 previousVectorD = null;
    //proteins:
    //       C        O (wing)
    //        \       |
    //         CA--N--C        O (wing)
    //      (lead)     \       |    
    //                  CA--N--C 
    //               (lead)     \
    //                           CA--N
    //                        (lead)
    // mon#    2         1        0
    for (int i = 1; i < monomerCount; ++i) {
      leadPointPrev = leadPoint;
      leadPoints[i] = leadPoint = getLeadPoint(i);
      P3 midpoint = new P3();
      midpoint.ave(leadPoint, leadPointPrev);
      leadMidpoints[i] = midpoint;
      if (hasWingPoints) {
        vectorA.sub2(leadPoint, leadPointPrev);
        vectorB.sub2(leadPointPrev, getWingPoint(i - 1));
        vectorC.cross(vectorA, vectorB);
        vectorD.cross(vectorA, vectorC);
        vectorD.normalize();
        if (!twistedSheets && previousVectorD != null
            && previousVectorD.angle(vectorD) > Math.PI / 2) {
          reversed.set(i);
          vectorD.scale(-1);
        }
        previousVectorD = wingVectors[i] = V3.newV(vectorD);
        //System.out.println("draw v" + i + " vector " + midpoint + " " + vectorD); 
      }
    }
    leadPoints[monomerCount] = leadMidpoints[monomerCount] = getTerminatorPoint();
    if (!hasWingPoints) {
      if (monomerCount < 3) {
        wingVectors[1] = unitVectorX;
      } else {
        // auto-calculate wing vectors based upon lead atom positions only
        V3 previousVectorC = null;
        for (int i = 1; i < monomerCount; ++i) {
          // perfect for traceAlpha on; reasonably OK for traceAlpha OFF
          vectorA.sub2(leadMidpoints[i], leadPoints[i]);
          vectorB.sub2(leadPoints[i], leadMidpoints[i + 1]);
          vectorC.cross(vectorA, vectorB);
          vectorC.normalize();
          if (previousVectorC != null
              && previousVectorC.angle(vectorC) > Math.PI / 2)
            vectorC.scale(-1);
          previousVectorC = wingVectors[i] = V3.newV(vectorC);
        }
      }
    }
    wingVectors[0] = wingVectors[1];
    wingVectors[monomerCount] = wingVectors[monomerCount - 1];
  }

  private final V3 unitVectorX = V3.new3(1, 0, 0);

  public void findNearestAtomIndex(int xMouse, int yMouse, Atom[] closest,
                                   short[] mads, int myVisibilityFlag,
                                   BS bsNot) {
    for (int i = monomerCount; --i >= 0;) {
      if ((monomers[i].shapeVisibilityFlags & myVisibilityFlag) == 0)
        continue;
      Atom a = monomers[i].getLeadAtom();
      if (!a.checkVisible() || bsNot != null && bsNot.get(a.i))
        continue;
      if (mads[i] > 0 || mads[i + 1] > 0)
        monomers[i].findNearestAtomIndex(xMouse, yMouse, closest, mads[i],
            mads[i + 1]);
    }
  }

  private int selectedMonomerCount;

  int getSelectedMonomerCount() {
    return selectedMonomerCount;
  }

  BS bsSelectedMonomers;

  public void calcSelectedMonomersCount(BS bsSelected) {
    selectedMonomerCount = 0;
    if (bsSelectedMonomers == null)
      bsSelectedMonomers = new BS();
    bsSelectedMonomers.clearAll();
    for (int i = 0; i < monomerCount; i++) {
      if (monomers[i].isSelected(bsSelected)) {
        ++selectedMonomerCount;
        bsSelectedMonomers.set(i);
      }
    }
  }

  boolean isMonomerSelected(int i) {
    return (i >= 0 && bsSelectedMonomers.get(i));
  }

  public int getPolymerPointsAndVectors(int last, BS bs,
                                        Lst<P3[]> vList,
                                        boolean isTraceAlpha,
                                        float sheetSmoothing) {
    P3[] points = getControlPoints(isTraceAlpha, sheetSmoothing, false);
    V3[] vectors = getWingVectors();
    int count = monomerCount;
    for (int j = 0; j < count; j++)
      if (bs.get(monomers[j].leadAtomIndex)) {
        vList.addLast(new P3[] { points[j], P3.newP(vectors[j]) });
        last = j;
      } else if (last != Integer.MAX_VALUE - 1) {
        vList.addLast(new P3[] { points[j], P3.newP(vectors[j]) });
        last = Integer.MAX_VALUE - 1;
      }
    if (last + 1 < count)
      vList.addLast(new P3[] { points[last + 1],
          P3.newP(vectors[last + 1]) });
    return last;
  }

  public String getSequence() {
    char[] buf = new char[monomerCount];
    for (int i = 0; i < monomerCount; i++)
      buf[i] = monomers[i].getGroup1();
    return String.valueOf(buf);
  }

  public void getPolymerSequenceAtoms(int group1, int nGroups,
                                      BS bsInclude, BS bsResult) {
    for (int i = Math.min(monomerCount, group1 + nGroups); --i >= group1;)
      monomers[i].getMonomerSequenceAtoms(bsInclude, bsResult);
  }

  /**
   * @param monomerIndex  
   * @return  "HELIX" "TURN" etc
   */
  public ProteinStructure getProteinStructure(int monomerIndex) {
    return null;
  }

  public boolean haveParameters;

  public boolean calcParameters() {
    haveParameters = true;
    return calcEtaThetaAngles() || calcPhiPsiAngles();
  }

  protected boolean calcEtaThetaAngles() {
    return false;
  }

  protected boolean calcPhiPsiAngles() {
    return false;
  }

  /**
   * 
   * @param m
   * @param qtype
   * @return calculated value
   */
  protected float calculateRamachandranHelixAngle(int m, char qtype) {
    return Float.NaN;
  }

  public boolean isNucleic() {
    return (monomerCount > 0 && this instanceof NucleicPolymer);
  }

  public void getRangeGroups(int nResidues, BS bsAtoms, BS bsResult) {
    BS bsTemp = new BS();
    for (int i = 0; i < monomerCount; i++) {
      if (!monomers[i].isSelected(bsAtoms))
        continue;
      bsTemp.setBits(Math.max(0, i - nResidues), i + nResidues + 1);
      i += nResidues - 1;
    }
    for (int i = bsTemp.nextSetBit(0); i >= 0 && i < monomerCount; i = bsTemp
        .nextSetBit(i + 1))
      monomers[i].setAtomBits(bsResult);
  }

  /**
   * 
   * @param polymer
   * @param bsA
   * @param bsB
   * @param vHBonds
   * @param nMaxPerResidue
   * @param min
   * @param checkDistances
   * @param dsspIgnoreHydrogens
   */
  public void calcRasmolHydrogenBonds(BioPolymer polymer, BS bsA,
                                      BS bsB, Lst<Bond> vHBonds,
                                      int nMaxPerResidue, int[][][] min,
                                      boolean checkDistances,
                                      boolean dsspIgnoreHydrogens) {
    // Amino, Nucleic override
  }

  public int getType() {
    return type;
  }

  public boolean isCyclic() { // Jmol 14.5.3
    return ((cyclicFlag == 0 ? (cyclicFlag = (monomerCount >= 4 && monomers[0]
        .isConnectedAfter(monomers[monomerCount - 1])) ? 1 : -1) : cyclicFlag) == 1);
  }

}
