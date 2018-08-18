/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-20 07:56:22 -0500 (Tue, 20 Mar 2007) $
 * $Revision: 7182 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.jmol.geodesic;

import org.jmol.util.BSUtil;
import org.jmol.util.Geodesic;
import org.jmol.util.Normix;

import javajs.util.AU;
import javajs.util.M3;
import javajs.util.P3;
import javajs.util.V3;
import org.jmol.viewer.JC;

import org.jmol.api.AtomIndexIterator;
import org.jmol.api.JmolEnvCalc;
import org.jmol.atomdata.AtomData;
import org.jmol.atomdata.AtomDataServer;
import org.jmol.atomdata.RadiusData;
import org.jmol.atomdata.RadiusData.EnumType;
import javajs.util.BS;

/* ***************************************************************
 * 
 * 3/20/07 -- consolidation -- Bob Hanson
 * 
 * The two geodesic code segments in g3d.Geodesic and DotsRenderer were
 * cleaned up and all put in g3d.Geodesic (no new code required!)
 * Then GeoSurface was split off from Dots.
 * Finally, all the dot calculations were split off as EnvelopeCalculation,
 * which can be used then independently of the Dots shape.
 * 
 * 
 * 7/17/06 History -- Bob Hanson
 * 
 * Connolly surface rendering was never completed. Miguel got to the point
 * where he identified the three issues -- convex single-atom areas, 
 * two-atom connection "toruses" or "troughs", and three-atom connection "cavities",
 * and he successfully took care of each in its own way. However, he never figured 
 * out how to patch these together effectively, and the surface had triangular 
 * holes.
 *
 * This code was never documented, so users never worked with it.
 * In July of 2006, this code was superceded by the "isosurface solvent" 
 * command, which does this using the marching cubes algorithm to produce 
 * a much cleaner surface. Of course it also takes more time. 
 * 
 * What remains is the van der Waals surface, which can be extended using
 * 
 * dots/geosurface +1.2
 * 
 * to provide the solvent-accessible surface.
 * 
 * A better rendering of the solvent accessible surface is given using
 * 
 * isosurface sasurface 1.2  
 * 
 * A discussion of molecular/solvent-accessible surfaces can be found at
 * http://www.netsci.org/Science/Compchem/feature14e.html
 * 
 * In March 2007, Bob refactored all Geodesic business that was here 
 * into the class Geodesic, made GeoSurface an extension of Dots,
 * and generally similified the code. 
 * 
 */

/*
 * Miguel's original comments:
 * 
 *  The Dots and DotsRenderer classes implement vanderWaals and Connolly
 * dot surfaces. <p>
 * The vanderWaals surface is defined by the vanderWaals radius of each
 * atom. The surface of the atom is 'peppered' with dots. Each dot is
 * tested to see if it falls within the vanderWaals radius of any of
 * its neighbors. If so, then the dot is not displayed. <p>
 * See g3d.Geodesic for more discussion of the implementation. <p>
 * The Connolly surface is defined by rolling a probe sphere over the
 * surface of the molecule. In this way, a smooth surface is generated ...
 * one that does not have crevices between atoms. Three types of shapes
 * are generated: convex, saddle, and concave. <p>
 * The 'probe' is a sphere. A sphere of 1.2 angstroms representing HOH
 * is commonly used. <p>
 * Convex shapes are generated on the exterior surfaces of exposed atoms.
 * They are points on the sphere which are exposed. In these areas of
 * the molecule they look just like the vanderWaals dot surface. <p>
 * The saddles are generated between pairs of atoms. Imagine an O2
 * molecule. The probe sphere is rolled around the two oxygen spheres so
 * that it stays in contact with both spheres. The probe carves out a
 * torus (doughnut). The portion of the torus between the two points of
 * contact with the oxygen spheres is a saddle. <p>
 * The concave shapes are defined by triples of atoms. Imagine three
 * atom spheres in a close triangle. The probe sphere will sit (nicely)
 * in the little cavity formed by the three spheres. In fact, there are
 * two cavities, one on each side of the triangle. The probe sphere makes
 * one point of contact with each of the three atoms. The shape of the
 * cavity is the spherical triangle on the surface of the probe sphere
 * determined by these three contact points. <p>
 * For each of these three surface shapes, the dots are painted only
 * when the probe sphere does not interfere with any of the neighboring
 * atoms. <p>
 * See the following scripting commands:<br>
 * set solvent on/off (on defaults to 1.2 angstroms) <br>
 * set solvent 1.5 (choose another probe size) <br>
 * dots on/off <br>
 * color dots [color] <br>
 * color dotsConvex [color] <br>
 * color dotsSaddle [color] <br>
 * color dotsConcave [color] <br>
 *
 * The reference article for this implementation is: <br>
 * Analytical Molecular Surface Calculation, Michael L. Connolly,
 * Journal of Applied Crystalography, (1983) 15, 548-558 <p>
 *
 ****************************************************************/

public final class EnvelopeCalculation implements JmolEnvCalc {

  private BS geodesicMap;
  private BS mapT;

  //Viewer vwr;
  private short[] mads;
  private AtomData atomData = new AtomData();
  private AtomDataServer vwr;
  private int ac;
  private static BS EMPTY_SET;

  public EnvelopeCalculation() {
  }

  /**
   * 
   * @param vwr
   * @param ac
   * @param mads
   * @return this
   */
  @Override
  public EnvelopeCalculation set(AtomDataServer vwr, int ac, short[] mads) {
    this.vwr = vwr;
    this.ac = ac; //preliminary, for setFromBits()
    this.mads = mads;
    geodesicCount = Geodesic.getVertexCount(JC.ENV_CALC_MAX_LEVEL);
    geodesicMap = BS.newN(geodesicCount);
    mapT = BS.newN(geodesicCount);
    EMPTY_SET = BSUtil.emptySet;
    return this;
  }

  private float maxRadius = 0;
  private boolean modelZeroBased;
  private boolean disregardNeighbors = false;
  private BS bsMySelected;

  private BS[] dotsConvexMaps;

  public BS[] getDotsConvexMaps() {
    return dotsConvexMaps;
  }

  private int dotsConvexMax; // the Max == the highest atomIndex with dots + 1

  public int getDotsConvexMax() {
    return dotsConvexMax;
  }

  public void allocDotsConvexMaps(int max) {
    if (dotsConvexMax >= max)
      return;
    dotsConvexMax = max;
    dotsConvexMaps = new BS[max];
  }

  private int geodesicCount;
  private BS bsSurface;

  @Override
  public BS getBsSurfaceClone() {
    return BSUtil.copy(bsSurface);
  }

  public void setMads(short[] mads) {
    this.mads = mads;
  }

  public void setFromBits(int index, BS bs) {
    geodesicMap.setBits(0, geodesicCount);
    for (int iDot = geodesicCount; --iDot >= 0;)
      if (!bs.get(iDot))
        geodesicMap.clear(iDot);
    if (dotsConvexMaps == null)
      dotsConvexMaps = new BS[ac];
    BS map;
    if (geodesicMap.isEmpty())
      map = EMPTY_SET;
    else
      map = BSUtil.copy(geodesicMap);
    if (index >= dotsConvexMaps.length)
      return;
    dotsConvexMaps[index] = map;
    dotsConvexMax = Math.max(dotsConvexMax, index);
  }

  private float radiusP, diameterP;

  public void newSet() {
    dotsConvexMax = 0;
    dotsConvexMaps = null;
    radiusP = diameterP = 0;
    mads = null;
  }

  private BS bsTemp;

  /**
   * problem prior to 12.3.18 was that dots once on the deodesic were not being
   * moved. this isn't perfect, but it's reasonably good. Mostly, you should
   * recreate dots after rotateSelected. This isn't a problem until after a
   * state is saved and reloaded, since only then with atomData.radiusData be
   * null.
   * 
   * @param bs
   * @param m
   */
  public void reCalculate(BS bs, M3 m) {
    if (atomData.radiusData != null) {
      calculate(null, maxRadius, bs, bsIgnore, disregardNeighbors,
          onlySelectedDots, isSurface, multiModel);
      return;
    }
    if (dotsConvexMaps == null || dotsConvexMax == 0)
      return;
    V3 pt = new V3();
    if (bsTemp == null)
      bsTemp = Normix.newVertexBitSet();
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      if (i >= dotsConvexMax)
        return;
      BS map = dotsConvexMaps[i];
      if (map == null || map.isEmpty())
        continue;
      BS bsNew = new BS();
      for (int j = map.nextSetBit(0); j >= 0; j = map.nextSetBit(j + 1)) {
        pt.setT(Geodesic.getVertexVector(j));
        m.rotate(pt);
        bsNew.set(Normix.getNormixV(pt, bsTemp));
      }
      dotsConvexMaps[i] = bsNew;
    }

  }

  private BS bsIgnore;
  private boolean onlySelectedDots;
  private boolean isSurface;
  private boolean multiModel;

  /**
   * @param rd
   * @param maxRadius
   * @param bsSelected
   * @param bsIgnore
   * @param disregardNeighbors
   * @param onlySelectedDots
   * @param isSurface
   * @param multiModel
   */
  @Override
  public void calculate(RadiusData rd, float maxRadius, BS bsSelected,
                        BS bsIgnore, boolean disregardNeighbors,
                        boolean onlySelectedDots, boolean isSurface,
                        boolean multiModel) {
    // was: this.setRadius = (setRadius == Float.MAX_VALUE &&
    // !useVanderwaalsRadius ? SURFACE_DISTANCE_FOR_CALCULATION : setRadius);

    if (rd == null) {
      rd = atomData.radiusData;
      if (rd == null)
        return;
    } else {
      atomData.radiusData = rd;
      this.bsIgnore = bsIgnore;
      this.onlySelectedDots = onlySelectedDots;
      this.multiModel = multiModel;
      this.isSurface = isSurface;
    }
    if (rd.value == Float.MAX_VALUE)
      rd.value = JC.ENC_CALC_MAX_DIST;
    atomData.modelIndex = (multiModel ? -1 : 0);
    modelZeroBased = !multiModel;

    vwr.fillAtomData(atomData, AtomData.MODE_FILL_COORDS
        | (mads == null ? AtomData.MODE_FILL_RADII : 0));
    ac = atomData.ac;
    if (mads != null)
      for (int i = 0; i < ac; i++)
        atomData.atomRadius[i] = mads[i] / 1000f;

    bsMySelected = (onlySelectedDots && bsSelected != null ? BSUtil
        .copy(bsSelected) : bsIgnore != null ? BSUtil.setAll(ac) : null);
    BSUtil.andNot(bsMySelected, bsIgnore);
    this.disregardNeighbors = disregardNeighbors;
    this.maxRadius = maxRadius;
    bsSurface = new BS();
    // now, calculate surface for selected atoms
    boolean isAll = (bsSelected == null);
    AtomIndexIterator iter = vwr.getSelectedAtomIterator(bsMySelected, false,
        modelZeroBased, false);
    //true ==> only atom index > this atom accepted
    checkNewDotsArray(); // possible that atoms have been added
    int i0 = (isAll ? ac - 1 : bsSelected.nextSetBit(0));
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bsSelected.nextSetBit(i + 1)))
      if (bsIgnore == null || !bsIgnore.get(i)) {
        setAtomI(i);
        getNeighbors(iter);
        calcConvexMap(isSurface);
      }
    iter.release();
    currentPoints = null;
    setDotsConvexMax();
  }

  public float getRadius(int atomIndex) {
    return atomData.atomRadius[atomIndex];
  }

  private P3[] currentPoints;

  @Override
  public P3[] getPoints() {
    if (dotsConvexMaps == null) {
      calculate(new RadiusData(null, JC.ENC_CALC_MAX_DIST, EnumType.ABSOLUTE,
          null), Float.MAX_VALUE, bsMySelected, null, false, false, false,
          false);
    }
    if (currentPoints != null)
      return currentPoints;
    int nPoints = 0;
    int dotCount = 42;
    for (int i = dotsConvexMax; --i >= 0;)
      if (dotsConvexMaps[i] != null)
        nPoints += dotsConvexMaps[i].cardinalityN(dotCount);
    P3[] points = new P3[nPoints];
    if (nPoints == 0)
      return points;
    nPoints = 0;
    for (int i = dotsConvexMax; --i >= 0;)
      if (dotsConvexMaps[i] != null) {
        int iDot = dotsConvexMaps[i].size();
        if (iDot > dotCount)
          iDot = dotCount;
        while (--iDot >= 0)
          if (dotsConvexMaps[i].get(iDot)) {
            P3 pt = new P3();
            pt.scaleAdd2(atomData.atomRadius[i],
                Geodesic.getVertexVector(iDot), atomData.xyz[i]);
            points[nPoints++] = pt;
          }
      }
    currentPoints = points;
    return points;
  }

  ///////////////// private methods ///////////////////

  private void setDotsConvexMax() {
    if (dotsConvexMaps == null)
      dotsConvexMax = 0;
    else {
      int i;
      for (i = ac; --i >= 0 && dotsConvexMaps[i] == null;) {
      }
      dotsConvexMax = i + 1;
    }
  }

  /*
    BitSet getSurfaceAtoms() {
      return bsSurface;
    }
  */
  public float getAppropriateRadius(int atomIndex) {
    return (mads != null ? (atomIndex >= mads.length ? 0
        : mads[atomIndex] / 1000f) : atomData.atomRadius[atomIndex]);
  }

  private int indexI;
  private P3 centerI;
  private float radiusI;
  private float radiiIP2;
  private final P3 pointT = new P3();

  private void setAtomI(int indexI) {
    this.indexI = indexI;
    centerI = atomData.xyz[indexI];
    radiusI = atomData.atomRadius[indexI];
    radiiIP2 = radiusI + radiusP;
    radiiIP2 *= radiiIP2;
  }

  private void calcConvexMap(boolean isSurface) {
    calcConvexBits();
    BS map;
    if (geodesicMap.isEmpty())
      map = EMPTY_SET;
    else {
      bsSurface.set(indexI);
      if (isSurface) {
        addIncompleteFaces(geodesicMap);
        addIncompleteFaces(geodesicMap);
      }
      map = BSUtil.copy(geodesicMap);
    }
    dotsConvexMaps[indexI] = map;
  }

  private void addIncompleteFaces(BS points) {
    mapT.clearAll();
    short[] faces = Geodesic.getFaceVertexes(JC.ENV_CALC_MAX_LEVEL);
    int len = faces.length;
    int maxPt = -1;
    for (int f = 0; f < len;) {
      short p1 = faces[f++];
      short p2 = faces[f++];
      short p3 = faces[f++];
      boolean ok1 = points.get(p1);
      boolean ok2 = points.get(p2);
      boolean ok3 = points.get(p3);
      if (!(ok1 || ok2 || ok3) || ok1 && ok2 && ok3)
        continue;

      // trick: DO show faces if ANY ONE vertex is missing
      if (!ok1) {
        mapT.set(p1);
        if (maxPt < p1)
          maxPt = p1;
      }
      if (!ok2) {
        mapT.set(p2);
        if (maxPt < p2)
          maxPt = p2;
      }
      if (!ok3) {
        mapT.set(p3);
        if (maxPt < p3)
          maxPt = p3;
      }
    }
    for (int i = 0; i <= maxPt; i++) {
      if (mapT.get(i))
        points.set(i);
    }
  }

  private P3 centerT;

  //level = 3 for both
  private final P3[] vertexTest = new P3[12];
  {
    for (int i = 0; i < 12; i++)
      vertexTest[i] = new P3();
  }

  private static int[] power4 = { 1, 4, 16, 64, 256 };

  private void calcConvexBits() {
    geodesicMap.setBits(0, geodesicCount);
    float combinedRadii = radiusI + radiusP;
    if (neighborCount == 0)
      return;
    int faceTest;
    int p1, p2, p3;
    short[] faces = Geodesic.getFaceVertexes(JC.ENV_CALC_MAX_LEVEL);
    int p4 = power4[JC.ENV_CALC_MAX_LEVEL - 1];
    boolean ok1, ok2, ok3;
    mapT.clearAll();
    for (int i = 0; i < 12; i++) {
      vertexTest[i].scaleAdd2(combinedRadii, Geodesic.getVertexVector(i),
          centerI);
    }
    for (int f = 0; f < 20; f++) {
      faceTest = 0;
      p1 = faces[3 * p4 * (4 * f + 0)];
      p2 = faces[3 * p4 * (4 * f + 1)];
      p3 = faces[3 * p4 * (4 * f + 2)];
      for (int j = 0; j < neighborCount; j++) {
        float maxDist = neighborPlusProbeRadii2[j];
        centerT = neighborCenters[j];
        ok1 = vertexTest[p1].distanceSquared(centerT) >= maxDist;
        ok2 = vertexTest[p2].distanceSquared(centerT) >= maxDist;
        ok3 = vertexTest[p3].distanceSquared(centerT) >= maxDist;
        if (!ok1)
          geodesicMap.clear(p1);
        if (!ok2)
          geodesicMap.clear(p2);
        if (!ok3)
          geodesicMap.clear(p3);
        if (!ok1 && !ok2 && !ok3) {
          faceTest = -1;
          break;
        }
      }
      int kFirst = f * 12 * p4;
      int kLast = kFirst + 12 * p4;
      for (int k = kFirst; k < kLast; k++) {
        int vect = faces[k];
        if (mapT.get(vect) || !geodesicMap.get(vect))
          continue;
        switch (faceTest) {
        case -1:
          //face full occluded
          geodesicMap.clear(vect);
          break;
        case 0:
          //face partially occluded
          for (int j = 0; j < neighborCount; j++) {
            float maxDist = neighborPlusProbeRadii2[j];
            centerT = neighborCenters[j];
            pointT.scaleAdd2(combinedRadii, Geodesic.getVertexVector(vect),
                centerI);
            if (pointT.distanceSquared(centerT) < maxDist)
              geodesicMap.clear(vect);
          }
          break;
        case 1:
          //face is fully surface
        }
        mapT.set(vect);
      }
    }
  }

  private void checkNewDotsArray() {
    if (dotsConvexMaps == null) {
      dotsConvexMaps = new BS[ac];
    } else if (dotsConvexMaps.length != ac) {
      BS[] a = new BS[ac];
      for (int i = 0; i < ac && i < dotsConvexMaps.length; i++)
        a[i] = dotsConvexMaps[i];
      dotsConvexMaps = a;
    }
  }

  private int neighborCount;
  private int[] neighborIndices = new int[16];
  private P3[] neighborCenters = new P3[16];
  private float[] neighborPlusProbeRadii2 = new float[16];
  private float[] neighborRadii2 = new float[16];

  private AtomIndexIterator getNeighbors(AtomIndexIterator iter) {
    neighborCount = 0;
    if (disregardNeighbors)
      return null;
    vwr.setIteratorForAtom(iter, indexI, radiusI + diameterP + maxRadius);
    while (iter.hasNext()) {
      int indexN = iter.next();
      float neighborRadius = atomData.atomRadius[indexN];
      if (centerI.distance(atomData.xyz[indexN]) > radiusI + radiusP
          + radiusP + neighborRadius)
        continue;
      if (neighborCount == neighborIndices.length) {
        neighborIndices = AU.doubleLengthI(neighborIndices);
        neighborCenters = (P3[]) AU.doubleLength(neighborCenters);
        neighborPlusProbeRadii2 = AU.doubleLengthF(neighborPlusProbeRadii2);
        neighborRadii2 = AU.doubleLengthF(neighborRadii2);
      }
      neighborCenters[neighborCount] = atomData.xyz[indexN];
      neighborIndices[neighborCount] = indexN;
      float r = neighborRadius + radiusP;
      neighborPlusProbeRadii2[neighborCount] = r * r;
      neighborRadii2[neighborCount] = neighborRadius * neighborRadius;
      ++neighborCount;
    }
    return iter;
  }

  /**
   * 
   * @param firstAtomDeleted
   * @param nAtomsDeleted
   */
  public void deleteAtoms(int firstAtomDeleted, int nAtomsDeleted) {
    dotsConvexMaps = (BS[]) AU.deleteElements(dotsConvexMaps, firstAtomDeleted,
        nAtomsDeleted);
    dotsConvexMax = dotsConvexMaps.length;
    if (mads != null)
      mads = (short[]) AU.deleteElements(mads, firstAtomDeleted, nAtomsDeleted);
    atomData.atomRadius = (float[]) AU.deleteElements(atomData.atomRadius,
        firstAtomDeleted, nAtomsDeleted);
    atomData.xyz = (P3[]) AU.deleteElements(atomData.xyz,
        firstAtomDeleted, nAtomsDeleted);
    atomData.ac -= nAtomsDeleted;
    ac = atomData.ac;

  }

}
