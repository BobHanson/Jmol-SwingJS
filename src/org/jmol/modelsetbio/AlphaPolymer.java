/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-02-21 16:19:35 -0600 (Wed, 21 Feb 2007) $
 * $Revision: 6903 $
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

import org.jmol.c.STR;
import javajs.util.BS;
import org.jmol.util.Logger;

import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.V3;

public class AlphaPolymer extends BioPolymer {

  /**
   * Specifically for mmTF bitset setting of structures
   */
  public int pt0;

  AlphaPolymer(Monomer[] monomers, int pt0) {
    this.pt0 = pt0;
    set(monomers);
    hasStructure = true;
  }

  
  @Override
  public ProteinStructure getProteinStructure(int monomerIndex) {
    return (ProteinStructure) monomers[monomerIndex].getStructure();
  }

  @Override
  protected P3 getControlPoint(int i, V3 v) {
    if (!monomers[i].isSheet())
      return leadPoints[i];
    v.sub2(leadMidpoints[i], leadPoints[i]);
    v.scale(sheetSmoothing);
    P3 pt = P3.newP(leadPoints[i]);
    pt.add(v);
    return pt;
  }

  public void addStructure(STR type, String structureID,
                           int serialID, int strandCount, int startChainID,
                           int startSeqcode, int endChainID, int endSeqcode,
                           int istart, int iend, BS bsAssigned) {
    int i0 = -1;
    int i1 = -1;
    if (istart < iend) {
        if (monomers[0].firstAtomIndex > iend || monomers[monomerCount - 1].lastAtomIndex < istart)
      return;
     i0 = istart;
     i1 = iend;
    }
    int indexStart, indexEnd;
    if ((indexStart = getIndex(startChainID, startSeqcode, i0, i1)) == -1
        || (indexEnd = getIndex(endChainID, endSeqcode, i0, i1)) == -1)
      return;
    if (/*type != STR.ANNOTATION &&*/ istart >= 0 && bsAssigned != null) {
      int pt = bsAssigned.nextSetBit(monomers[indexStart].firstAtomIndex);
      if (pt >= 0 && pt < monomers[indexEnd].lastAtomIndex)
        return;
    }
    if (addStructureProtected(type, structureID, serialID, strandCount, indexStart,
        indexEnd) && istart >= 0)
      bsAssigned.setBits(istart, iend + 1);
  }

  public boolean addStructureProtected(STR type, String structureID,
                                       int serialID, int strandCount,
                                       int indexStart, int indexEnd) {

    //these two can be the same if this is a carbon-only polymer
    if (indexEnd < indexStart) {
      Logger.error("AlphaPolymer:addSecondaryStructure error: "
          + " indexStart:" + indexStart + " indexEnd:" + indexEnd);
      return false;
    }
    int structureCount = indexEnd - indexStart + 1;
    ProteinStructure ps = null;
    //    boolean isAnnotation = false;
    switch (type) {
    //    case ANNOTATION:
    //      ps = new Annotation(this, indexStart, structureCount, structureID);
    //      isAnnotation = true;
    //      break;
    case HELIX:
    case HELIXALPHA:
    case HELIX310:
    case HELIXPI:
      ps = new Helix(this, indexStart, structureCount, type);
      break;
    case SHEET:
      ps = new Sheet(this, indexStart, structureCount, type);
      break;
    case TURN:
      ps = new Turn(this, indexStart, structureCount);
      break;
    default:
      Logger.error("unrecognized secondary structure type");
      return false;
    }
    ps.structureID = structureID;
    ps.serialID = serialID;
    ps.strandCount = strandCount;
    for (int i = indexStart; i <= indexEnd; ++i)
      ((AlphaMonomer) monomers[i]).setStructure(ps);//, isAnnotation);
    return true;
  }
  
  @Override
  public void clearStructures() {
    for (int i = 0; i < monomerCount; i++)
      ((AlphaMonomer)monomers[i]).setStructure(null);//, false);
  }

  ////////////////////////////////////////////////////////////
  //
  //  alpha-carbon-only secondary structure determination
  //
  //  Levitt and Greer
  //  Automatic Identification of Secondary Structure in Globular Proteins
  //  J.Mol.Biol.(1977) 114, 181-293
  //
  /////////////////////////////////////////////////////////////
  
  /**
   * Uses Levitt & Greer algorithm to calculate protein secondary
   * structures using only alpha-carbon atoms.
   *<p>
   * Levitt and Greer <br />
   * Automatic Identification of Secondary Structure in Globular Proteins <br />
   * J.Mol.Biol.(1977) 114, 181-293 <br />
   *<p>
   * <a
   * href='http://csb.stanford.edu/levitt/Levitt_JMB77_Secondary_structure.pdf'>
   * http://csb.stanford.edu/levitt/Levitt_JMB77_Secondary_structure.pdf
   * </a>
   * @param alphaOnly  caught by AminoPolymer and discarded if desired 
   */
  public void calculateStructures(boolean alphaOnly) { 
    if (monomerCount < 4)
      return;
    float[] angles = calculateAnglesInDegrees();
    Code[] codes = calculateCodes(angles);
    checkBetaSheetAlphaHelixOverlap(codes, angles);
    STR[] tags = calculateRunsFourOrMore(codes);
    extendRuns(tags);
    searchForTurns(codes, angles, tags);
    addStructuresFromTags(tags);
  }

  private enum Code {
    NADA, RIGHT_HELIX, BETA_SHEET, LEFT_HELIX, LEFT_TURN, RIGHT_TURN;
  }
  
  private float[] calculateAnglesInDegrees() {
    float[] angles = new float[monomerCount];
    for (int i = monomerCount - 1; --i >= 2; )
      angles[i] = 
        Measure.computeTorsion(monomers[i - 2].getLeadAtom(),
                                   monomers[i - 1].getLeadAtom(),
                                   monomers[i    ].getLeadAtom(),
                                   monomers[i + 1].getLeadAtom(), true);
    return angles;
  }

  private Code[] calculateCodes(float[] angles) {
    Code[] codes = new Code[monomerCount];
    for (int i = monomerCount - 1; --i >= 2; ) {
      float degrees = angles[i];
      codes[i] = ((degrees >= 10 && degrees < 120)
                  ? Code.RIGHT_HELIX
                  : ((degrees >= 120 || degrees < -90)
                     ? Code.BETA_SHEET
                     : ((degrees >= -90 && degrees < 0)
                        ? Code.LEFT_HELIX
                        : Code.NADA)));
    }
    return codes;
  }

  private void checkBetaSheetAlphaHelixOverlap(Code[] codes, float[] angles) {
    for (int i = monomerCount - 2; --i >= 2; )
      if (codes[i] == Code.BETA_SHEET &&
          angles[i] <= 140 &&
          codes[i - 2] == Code.RIGHT_HELIX &&
          codes[i - 1] == Code.RIGHT_HELIX &&
          codes[i + 1] == Code.RIGHT_HELIX &&
          codes[i + 2] == Code.RIGHT_HELIX)
        codes[i] = Code.RIGHT_HELIX;
  }

  private STR[] calculateRunsFourOrMore(Code[] codes) {
    STR[] tags = new STR[monomerCount];
    STR tag = STR.NONE;
    Code code = Code.NADA;
    int runLength = 0;
    for (int i = 0; i < monomerCount; ++i) {
      // throw away the sheets ... their angle technique does not work well
      if (codes[i] == code && code != Code.NADA) {// && code != Code.BETA_SHEET) {
        ++runLength;
        if (runLength == 4) {
          tag = (code == Code.BETA_SHEET ? STR.SHEET : STR.HELIX);
          for (int j = 4; --j >= 0; )
            tags[i - j] = tag;
        } else if (runLength > 4)
          tags[i] = tag;
      } else {
        runLength = 1;
        code = codes[i];
      }
    }
    return tags;
  }

  private void extendRuns(STR[] tags) {
    for (int i = 1; i < monomerCount - 4; ++i)
      if (tags[i] == STR.NONE && tags[i + 1] != STR.NONE)
        tags[i] = tags[i + 1];
    
    tags[0] = tags[1];
    tags[monomerCount - 1] = tags[monomerCount - 2];
  }

  private void searchForTurns(Code[] codes, float[] angles, STR[] tags) {
    for (int i = monomerCount - 1; --i >= 2; ) {
      codes[i] = Code.NADA;
      if (tags[i] == null || tags[i] == STR.NONE) {
        float angle = angles[i];
        if (angle >= -90 && angle < 0)
          codes[i] = Code.LEFT_TURN;
        else if (angle >= 0 && angle < 90)
          codes[i] = Code.RIGHT_TURN;
      }
    }

    for (int i = monomerCount - 1; --i >= 0; ) {
      if (codes[i] != Code.NADA &&
          codes[i + 1] == codes[i] &&
          tags[i] == STR.NONE)
        tags[i] = STR.TURN;
    }
  }

  private void addStructuresFromTags(STR[] tags) {
    int i = 0;
    while (i < monomerCount) {
      STR tag = tags[i];
      if (tag == null || tag == STR.NONE) {
        ++i;
        continue;
      }
      int iMax;
      for (iMax = i + 1;
           iMax < monomerCount && tags[iMax] == tag;
           ++iMax)
        { }
      addStructureProtected(tag, null, 0, 0, i, iMax - 1);
      i = iMax;
    }
  }

  private final static String[] dsspTypes = {"H", null, "H", "S", "H", null, "T"}; 
  /**
   * bits in the bitset determines the type
   * @param count 
   * @param dsspType 
   * @param type
   * @param bs
   * @param doOffset
   *        allows us to examine just a portion of the
   * @return updated count
   */
  public int setStructureBS(int count, int dsspType, STR type, BS bs, boolean doOffset) {
    int offset = (doOffset ? pt0 : 0);
    for (int pt = 0, i = bs.nextSetBit(offset), i2 = 0, n = monomerCount + offset; i >= 0
        && i < n; i = bs.nextSetBit(i2 + 1)) {
      if ((i2 = bs.nextClearBit(i)) < 0 || i2 > n)
        i2 = n;
      addStructureProtected(type, dsspTypes[dsspType] + (++pt), count++, (dsspType == 3 ? 1 : 0), i - offset, i2 - 1 - offset);
    }
    return count;
  }
  

}
