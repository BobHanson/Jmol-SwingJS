/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-30 11:40:16 -0500 (Fri, 30 Mar 2007) $
 * $Revision: 7273 $
 *
 * Copyright (C) 2007 Miguel, Bob, Jmol Development
 *
 * Contact: hansonr@stolaf.edu
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.jvxl.readers;

import java.util.Date;

import javajs.util.AU;
import javajs.util.M4d;
import javajs.util.P3d;
import javajs.util.P3i;
import javajs.util.SB;
import javajs.util.T3d;
import javajs.util.V3d;

import org.jmol.atomdata.AtomData;
import org.jmol.atomdata.RadiusData;
import org.jmol.atomdata.RadiusData.EnumType;
import org.jmol.c.VDW;
import javajs.util.BS;
import org.jmol.jvxl.data.JvxlCoder;
import org.jmol.jvxl.data.MeshData;
import org.jmol.util.BSUtil;
import org.jmol.util.ContactPair;
import org.jmol.util.Logger;

abstract class AtomDataReader extends VolumeDataReader {

  protected double maxDistance;
  protected ContactPair contactPair;

  AtomDataReader() {
  }

  protected void initADR(SurfaceGenerator sg) {
    initVDR(sg);
    precalculateVoxelData = true;
  }

  protected String fileName;
  protected String fileDotModel;
  protected int modelIndex;

  protected AtomData atomData = new AtomData();

  protected P3d[] atomXyzTruncated;
  protected double[] atomRadius;
  protected double[] atomProp;
  protected int[] atomNo;
  protected int[] atomIndex;
  protected int[] myIndex;
  protected int ac;
  protected int myAtomCount;
  protected int nearbyAtomCount;
  protected int firstNearbyAtom;
  protected BS bsMySelected = new BS();
  protected BS bsMyIgnored = new BS();
  protected BS bsNearby;

  protected boolean doAddHydrogens;
  protected boolean havePlane;
  protected boolean doUseIterator;
  protected double theProperty;
  protected boolean haveOneProperty;
  private double minPtsPerAng;

  /**
   * @param isMapData
   */
  @Override
  protected void setup(boolean isMapData) {
    setup2();
  }

  protected void setup2() {
    //CANNOT BE IN HERE IF atomDataServer is not valid
    contactPair = params.contactPair;
    doAddHydrogens = (sg.atomDataServer != null && params.addHydrogens); //Jvxl cannot do this on its own
    modelIndex = params.modelIndex;
    if (params.bsIgnore != null)
      bsMyIgnored = params.bsIgnore;
    if (params.volumeData != null) {
      setVolumeDataV(params.volumeData);
      setBBox(volumeData.volumetricOrigin, 0);
      ptV.setT(volumeData.volumetricOrigin);
      for (int i = 0; i < 3; i++)
        ptV.scaleAdd2(volumeData.voxelCounts[i] - 1,
            volumeData.volumetricVectors[i], ptV);
      setBBox(ptV, 0);
    }
    havePlane = (params.thePlane != null);
    if (havePlane)
      volumeData.setPlaneParameters(params.thePlane);
  }

  protected void markPlaneVoxels(P3d p, double r) {
    for (int i = 0, pt = thisX * yzCount, pt1 = pt + yzCount; pt < pt1; pt++, i++) {
      volumeData.getPoint(pt, ptV);
      thisPlane[i] = ptV.distance(p) - r;
    }
  }

  protected void setVolumeForPlane() {
    if (useOriginStepsPoints) {
      xyzMin = P3d.newP(params.origin);
      xyzMax = P3d.newP(params.origin);
      xyzMax.add3((params.points.x - 1) * params.steps.x, (params.points.y - 1)
          * params.steps.y, (params.points.z - 1) * params.steps.z);
    } else if (params.boundingBox == null) {
      getAtoms(params.bsSelected, false, true, false, false, false, false,
          params.mep_marginAngstroms, params.modelInvRotation);
      if (xyzMin == null) {
        xyzMin = P3d.new3(-10, -10, -10);
        xyzMax = P3d.new3(10, 10, 10);
      }
    } else {
      xyzMin = P3d.newP(params.boundingBox[0]);
      xyzMax = P3d.newP(params.boundingBox[1]);
    }
    setRanges(params.plane_ptsPerAngstrom, params.plane_gridMax, 0);
  }

  /**
   * 
   * @param bsSelected
   * @param doAddHydrogens
   * @param getRadii
   * @param getMolecules
   * @param getAllModels
   * @param addNearbyAtoms
   * @param getAtomMinMax
   * @param marginAtoms
   * @param modelInvRotation 
   */
  protected void getAtoms(BS bsSelected, boolean doAddHydrogens,
                          boolean getRadii, boolean getMolecules,
                          boolean getAllModels, boolean addNearbyAtoms,
                          boolean getAtomMinMax, double marginAtoms, M4d modelInvRotation) {
    if (addNearbyAtoms)
      getRadii = true;
    // set atomRadiusData to 100% if it has not been set already
    // if it hasn't already been set.
    if (getRadii) {
      if (params.atomRadiusData == null)
        params.atomRadiusData = new RadiusData(null, 1, EnumType.FACTOR,
            VDW.AUTO);
      atomData.radiusData = params.atomRadiusData;
      atomData.radiusData.valueExtended = params.solventExtendedAtomRadius;
      if (doAddHydrogens)
        atomData.radiusData.vdwType = VDW.NOJMOL;
    }
    atomData.modelIndex = modelIndex; // -1 here means fill ALL atoms; any other
    // means "this model only"
    atomData.bsSelected = bsSelected;
    atomData.bsIgnored = bsMyIgnored;
    sg.fillAtomData(atomData, AtomData.MODE_FILL_COORDS
        | (getAllModels ? AtomData.MODE_FILL_MULTIMODEL : 0)
        | (getMolecules ? AtomData.MODE_FILL_MOLECULES : 0)
        | (getRadii ? AtomData.MODE_FILL_RADII : 0));
    if (doUseIterator)
      atomData.bsSelected = null;
    ac = atomData.ac;
    modelIndex = atomData.firstModelIndex;
    if (modelInvRotation != null)
      atomData.transformXYZ(modelInvRotation, bsSelected);
    boolean needRadius = false;
    for (int i = 0; i < ac; i++) {
      if ((bsSelected == null || bsSelected.get(i)) && (!bsMyIgnored.get(i))) {
        if (havePlane
            && Math.abs(volumeData.distancePointToPlane(atomData.xyz[i])) > 2 * (atomData.atomRadius[i] = getWorkingRadius(
                i, marginAtoms)))
          continue;
        bsMySelected.set(i);
        needRadius = !havePlane;
      }
      if (getRadii && (addNearbyAtoms || needRadius))
        atomData.atomRadius[i] = getWorkingRadius(i, marginAtoms);
    }

    double rH = (getRadii && doAddHydrogens ? getWorkingRadius(-1, marginAtoms)
        : 0);
    myAtomCount = BSUtil.cardinalityOf(bsMySelected);
    BS atomSet = BSUtil.copy(bsMySelected);
    int nH = 0;
    atomProp = null;
    theProperty = Double.MAX_VALUE;
    haveOneProperty = false;
    double[] props = params.theProperty;
    if (myAtomCount > 0) {
      P3d[] hAtoms = null;
      if (doAddHydrogens) {
        atomData.bsSelected = atomSet;
        sg.atomDataServer.fillAtomData(atomData,
            AtomData.MODE_GET_ATTACHED_HYDROGENS);
        hAtoms = new P3d[nH = atomData.hydrogenAtomCount];
        for (int i = 0; i < atomData.hAtoms.length; i++)
          if (atomData.hAtoms[i] != null)
            for (int j = atomData.hAtoms[i].length; --j >= 0;)
              hAtoms[--nH] = atomData.hAtoms[i][j];
        nH = hAtoms.length;
        Logger.info(nH + " attached hydrogens added");
      }
      int n = nH + myAtomCount;
      if (getRadii)
        atomRadius = new double[n];
      atomXyzTruncated = new P3d[n];
      if (params.theProperty != null)
        atomProp = new double[n];
      atomNo = new int[n];
      atomIndex = new int[n];
      myIndex = new int[ac];

      for (int i = 0; i < nH; i++) {
        if (getRadii)
          atomRadius[i] = rH;
        atomXyzTruncated[i] = hAtoms[i];
        atomNo[i] = -1;
        if (atomProp != null)
          addAtomProp(i, Double.NaN);
        // if (params.logMessages)
        // Logger.debug("draw {" + hAtoms[i].x + " " + hAtoms[i].y + " "
        // + hAtoms[i].z + "};");
      }
      myAtomCount = nH;
      for (int i = atomSet.nextSetBit(0); i >= 0; i = atomSet.nextSetBit(i + 1)) {
        if (atomProp != null)
          addAtomProp(myAtomCount,
              (props != null && i < props.length ? props[i] : Double.NaN));
        atomXyzTruncated[myAtomCount] = atomData.xyz[i];
        atomNo[myAtomCount] = atomData.atomicNumber[i];
        atomIndex[myAtomCount] = i;
        myIndex[i] = myAtomCount;
        if (getRadii)
          atomRadius[myAtomCount] = atomData.atomRadius[i];
        myAtomCount++;
      }
    }
    firstNearbyAtom = myAtomCount;
    if (!isQuiet)
      Logger.info(myAtomCount + " atoms will be used in the surface calculation");

    if (myAtomCount == 0) {
      setBBox(P3d.new3(10, 10, 10), 0);
      setBBox(P3d.new3(-10, -10, -10), 0);
    }
    for (int i = 0; i < myAtomCount; i++)
      setBBox(atomXyzTruncated[i], getRadii ? atomRadius[i] + 0.5f : 0); 
    if (!Double.isNaN(params.scale)) {
      V3d v = V3d.newVsub(xyzMax, xyzMin);
      v.scale(0.5f);
      xyzMin.add(v);
      v.scale(params.scale);
      xyzMax.add2(xyzMin, v);
      xyzMin.sub(v);
    }

    // fragment idea

    if (!addNearbyAtoms || myAtomCount == 0)
      return;
    P3d pt = new P3d();

    bsNearby = new BS();
    for (int i = 0; i < ac; i++) {
      if (atomSet.get(i) || bsMyIgnored.get(i))
        continue;
      double rA = atomData.atomRadius[i];
      if (params.thePlane != null
          && Math.abs(volumeData.distancePointToPlane(atomData.xyz[i])) > 2 * rA)
        continue;
      if (params.theProperty != null)
        rA += maxDistance;
      pt = atomData.xyz[i];
      if (pt.x + rA > xyzMin.x && pt.x - rA < xyzMax.x && pt.y + rA > xyzMin.y
          && pt.y - rA < xyzMax.y && pt.z + rA > xyzMin.z
          && pt.z - rA < xyzMax.z) {
        bsNearby.set(i);
        nearbyAtomCount++;
      }
    }
    int nAtoms = myAtomCount;
    if (nearbyAtomCount != 0) {
      nAtoms += nearbyAtomCount;
      atomRadius = AU.arrayCopyD(atomRadius, nAtoms);
      atomXyzTruncated = (P3d[]) AU.arrayCopyObject(atomXyzTruncated, nAtoms);
      if (atomIndex != null)
        atomIndex = AU.arrayCopyI(atomIndex, nAtoms);

      if (props != null)
        atomProp = AU.arrayCopyD(atomProp, nAtoms);
      for (int i = bsNearby.nextSetBit(0); i >= 0; i = bsNearby
          .nextSetBit(i + 1)) {
        if (props != null)
          addAtomProp(myAtomCount, props[i]);
        myIndex[i] = myAtomCount;
        atomIndex[myAtomCount] = i;
        atomXyzTruncated[myAtomCount] = atomData.xyz[i];
        atomRadius[myAtomCount++] = atomData.atomRadius[i];
      }
    }
    
    if (getRadii)
      setRadii();
    
    haveOneProperty = (!Double.isNaN(theProperty));
  }

  /**
   * solvent radius
   */
  protected double sr;
  /**
   * atom radius + solvent radius
   */
  protected double[] rs;
  /**
   * square of (atom radius + solvent radius)
   */
  protected double[] rs2;
  /**
   * maximun (atom radius + solvent radius) 
   */
  protected double maxRS;

  protected void setRadii() {
    if (rs != null)
      return;
    maxRS = 0;
    rs = new double[myAtomCount];
    rs2 = new double[myAtomCount];
    for (int i = 0; i < myAtomCount; i++) {
      double r = rs[i] = atomRadius[i] + sr;
      if (r > maxRS)
        maxRS = r;
      rs2[i] = rs[i] * rs[i];
    }   
  }

  private void addAtomProp(int i, double f) {
    atomProp[i] = f;
    if (!Double.isNaN(theProperty))
      if (f != theProperty)
        theProperty = (theProperty == Double.MAX_VALUE ? f : Double.NaN);
  }

  private double getWorkingRadius(int i, double marginAtoms) {
    double r = (i < 0 ? atomData.hAtomRadius : atomData.atomRadius[i]);
    return (Double.isNaN(marginAtoms) ? Math.max(r, 0.1f) : r + marginAtoms);
  }

  protected void setHeader(String calcType, String line2) {
    jvxlFileHeaderBuffer = new SB();
    if (atomData.programInfo != null)
      jvxlFileHeaderBuffer.append("#created by ").append(atomData.programInfo)
          .append(" on ").append("" + new Date()).append("\n");
    jvxlFileHeaderBuffer.append(calcType).append("\n").append(line2)
        .append("\n");
  }

  protected void setRanges(double ptsPerAngstrom, int maxGrid, double minPtsPerAng) {
    if (xyzMin == null)
      return;
    this.ptsPerAngstrom = ptsPerAngstrom;
    this.maxGrid = maxGrid;
    this.minPtsPerAng = minPtsPerAng;
    setVolumeData();
    JvxlCoder.jvxlCreateHeader(volumeData, jvxlFileHeaderBuffer);
  }

  @Override
  protected void setVolumeData() {
    setVolumeDataADR();
  }

  protected void setVolumeDataADR() {
    if (!setVolumeDataParams()) {
      setVoxelRange(0, xyzMin.x, xyzMax.x, ptsPerAngstrom, maxGrid,
          minPtsPerAng);
      setVoxelRange(1, xyzMin.y, xyzMax.y, ptsPerAngstrom, maxGrid,
          minPtsPerAng);
      setVoxelRange(2, xyzMin.z, xyzMax.z, ptsPerAngstrom, maxGrid,
          minPtsPerAng);
    }
  }

  protected void setVertexSource() {
    if (meshDataServer != null)
      meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES, null);
    if (params.vertexSource != null) {
      params.vertexSource = AU.arrayCopyI(params.vertexSource,
          meshData.vc);
      for (int i = 0; i < meshData.vc; i++)
        params.vertexSource[i] = Math.abs(params.vertexSource[i]) - 1;
    }
  }

  protected void resetPlane(double value) {
    for (int i = 0; i < yzCount; i++)
      thisPlane[i] = value;
  }

  protected void resetVoxelData(double value) {
    for (int x = 0; x < nPointsX; ++x)
      for (int y = 0; y < nPointsY; ++y)
        for (int z = 0; z < nPointsZ; ++z)
          voxelData[x][y][z] = value;
  }

  protected double[] thisPlane;
  protected BS thisAtomSet;
  protected int thisX;

  private double getVoxel(int i, int j, int k, int ipt) {
    return (isProgressive ? thisPlane[ipt % yzCount] : voxelData[i][j][k]);
  }

  protected void unsetVoxelData() {
    unsetVoxelData2();
  }

  protected void unsetVoxelData2() {
    if (isProgressive)
      for (int i = 0; i < yzCount; i++) {
        if (thisPlane[i] == Double.MAX_VALUE)
          thisPlane[i] = Double.NaN;
      }
    else
      for (int x = 0; x < nPointsX; ++x)
        for (int y = 0; y < nPointsY; ++y)
          for (int z = 0; z < nPointsZ; ++z)
            if (voxelData[x][y][z] == Double.MAX_VALUE)
              voxelData[x][y][z] = Double.NaN;
  }

  protected double margin;
  protected double vl0, vl1, vl2;

  protected void setGridLimitsForAtom(P3d ptA, double rA, P3i pt0, P3i pt1) {
    rA += margin; // to span corner-to-corner possibility
    volumeData.xyzToVoxelPt(ptA.x, ptA.y, ptA.z, pt0);
    int x = (int) Math.floor(rA / volumeData.volumetricVectorLengths[0]);
    int y = (int) Math.floor(rA / volumeData.volumetricVectorLengths[1]);
    int z = (int) Math.floor(rA / volumeData.volumetricVectorLengths[2]);
    pt1.set(pt0.x + x, pt0.y + y, pt0.z + z);
    pt0.set(pt0.x - x, pt0.y - y, pt0.z - z);
    pt0.x = Math.max(pt0.x - 1, 0);
    pt0.y = Math.max(pt0.y - 1, 0);
    pt0.z = Math.max(pt0.z - 1, 0);
    pt1.x = Math.min(pt1.x + 1, nPointsX);
    pt1.y = Math.min(pt1.y + 1, nPointsY);
    pt1.z = Math.min(pt1.z + 1, nPointsZ);
  }

  // for isoSolventReader and isoIntersectReader

  protected BS bsSurfaceVoxels;
  protected BS validSpheres, noFaceSpheres;
  protected int[] voxelSource;

  protected void getAtomMinMax(BS bs, BS[] bsAtomMinMax) {
    for (int i = 0; i < nPointsX; i++)
      bsAtomMinMax[i] = new BS();
    for (int iAtom = myAtomCount; --iAtom >= 0;) {
      if (bs != null && !bs.get(iAtom))
        continue;
      setGridLimitsForAtom(atomXyzTruncated[iAtom], atomRadius[iAtom], pt0, pt1);
      for (int i = pt0.x; i < pt1.x; i++)
        bsAtomMinMax[i].set(iAtom);
    }
  }

  protected final P3d ptY0 = new P3d();
  protected final P3d ptZ0 = new P3d();
  protected final P3i pt0 = new P3i();
  protected final P3i pt1 = new P3i();
  protected final P3d ptV = new P3d();

  protected void markSphereVoxels(double r0, double distance) {
    boolean isWithin = (distance != Double.MAX_VALUE && point != null);
    T3d v0 = volumetricVectors[0];
    T3d v1 = volumetricVectors[1];
    T3d v2 = volumetricVectors[2];
    for (int iAtom = thisAtomSet.nextSetBit(0); iAtom >= 0; iAtom = thisAtomSet
        .nextSetBit(iAtom + 1)) {
      if (!havePlane && validSpheres != null && !validSpheres.get(iAtom))
        continue;
      boolean isSurface = (noFaceSpheres != null && noFaceSpheres.get(iAtom));
      boolean isNearby = (iAtom >= firstNearbyAtom);
      P3d ptA = atomXyzTruncated[iAtom];
      double rA = atomRadius[iAtom];
      if (isWithin && ptA.distance(point) > distance + rA + 0.5)
        continue;
      double rA0 = rA + r0;
      setGridLimitsForAtom(ptA, rA0, pt0, pt1);
      //pt1.y = nPointsY;
      //pt1.z = nPointsZ;
      if (isProgressive) {
        pt0.x = thisX;
        pt1.x = thisX + 1;
      }
      volumeData.voxelPtToXYZ(pt0.x, pt0.y, pt0.z, ptV);
      for (int i = pt0.x; i < pt1.x; i++, ptV.add2(v0, ptY0)) {
        ptY0.setT(ptV);
        for (int j = pt0.y; j < pt1.y; j++, ptV.add2(v1, ptZ0)) {
          ptZ0.setT(ptV);
          for (int k = pt0.z; k < pt1.z; k++, ptV.add(v2)) {
            double value = ptV.distance(ptA) - rA;
            int ipt = volumeData.getPointIndex(i, j, k);
            if ((r0 == 0 || value <= rA0) && value < getVoxel(i, j, k, ipt)) {
              if (isNearby || isWithin && ptV.distance(point) > distance)
                value = Double.NaN;
              setVoxel(i, j, k, ipt, value);
              if (!Double.isNaN(value)) {
                if (voxelSource != null)
                  voxelSource[ipt] = iAtom + 1;
                if (value < 0 && isSurface)
                  bsSurfaceVoxels.set(ipt);
              }
            }
          }
        }
      }
    }
  }

  protected void setVoxel(int i, int j, int k, int ipt, double value) {
    if (isProgressive)
      thisPlane[ipt % yzCount] = value;
    else
      voxelData[i][j][k] = value;
  }

}
