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


import org.jmol.quantum.MepCalculation;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

import javajs.util.P3;
import javajs.util.T3;

import org.jmol.api.AtomIndexIterator;
import org.jmol.api.Interface;
import org.jmol.jvxl.data.MeshData;

/**
 * maps property data
 * 
 */
class AtomPropertyMapper extends AtomDataReader {

  private MepCalculation mepCalc;
  private String mepType;
  private int calcType = 0;

  AtomPropertyMapper(){}
  
  @Override
  void init(SurfaceGenerator sg) {
    initADR(sg);
    mepType = (String) sg.getReaderData();
  }
  
  private boolean doSmoothProperty;
  private AtomIndexIterator iter;
  private float smoothingPower;
  
  @Override
  protected void setup(boolean isMapData) {
    setup2();
    // MAP only
    haveSurfaceAtoms = true;
    volumeData.sr = this;
    volumeData.doIterate = false;
    point = params.point;
    doSmoothProperty = params.propertySmoothing;
    doUseIterator = true;
    if (doSmoothProperty) {
      smoothingPower = params.propertySmoothingPower;
      if (smoothingPower < 0)
        smoothingPower = 0;
      else if (smoothingPower > 10)
        smoothingPower = 10;
      if (smoothingPower == 0)
        doSmoothProperty = false;
      smoothingPower = (smoothingPower - 11) / 2f;
      // 0 to 10 becomes d^-10 to d^-1, and we'll be using distance^2
    }
    maxDistance = params.propertyDistanceMax;
    if (mepType != null) {
      doSmoothProperty = true;
      if (params.mep_calcType >= 0)
        calcType = params.mep_calcType;
      mepCalc = (MepCalculation) Interface.getOption("quantum."
          + mepType + "Calculation", (Viewer) sg.atomDataServer, "file");
    }
    if (!doSmoothProperty && maxDistance == Integer.MAX_VALUE)
      maxDistance = 5; // usually just local to a group
    //if (maxDistance == Integer.MAX_VALUE && calcType != params.mep_calcType)
      //maxDistance = 5; // max distance just for mep 
    getAtoms(params.bsSelected, doAddHydrogens, true, false, false, true, false, Float.NaN, null);
    if (meshDataServer != null)
      meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES, null);
    if (!doSmoothProperty && meshData.vertexSource != null) {
      hasColorData = true;
      for (int i = meshData.vc; --i >= 0;) {
        int iAtom = meshData.vertexSource[i];
        if (iAtom >= 0) {
          meshData.vvs[i] = params.theProperty[iAtom];
        } else {
          hasColorData = false;
          break;
        }
      }
    }

    setHeader("property", params.calculationType);
    // for plane mapping
    setRanges(params.solvent_ptsPerAngstrom, params.solvent_gridMax, 0);
    params.cutoff = 0;
  }

  @Override
  protected void setVolumeData() {
    if (params.thePlane != null)
      setVolumeDataADR();// unnecessary -- point-specific property mapper only    
  }

  @Override
  protected void initializeMapping() {
    if (params.showTiming)
      Logger.startTimer("property mapping");
    if (bsNearby != null)
      bsMySelected.or(bsNearby);
    iter = sg.atomDataServer.getSelectedAtomIterator(bsMySelected, false, false, false);
  }
  
  @Override
  protected void finalizeMapping() {
    iter.release();
    iter = null;
    if (params.showTiming)
      Logger.checkTimer("property mapping", false);
  }
  
  //////////// meshData extensions ////////////

  /////////////// calculation methods //////////////
    
  @Override
  protected void generateCube() {
    // not applicable
  }

  
  private int iAtomSurface;
  @Override
  public int getSurfaceAtomIndex() {
    return iAtomSurface;
  }
  
  @Override
  public float getValueAtPoint(T3 pt, boolean getSource) {
    if (haveOneProperty && !getSource)
      return theProperty;
    float dmin = Float.MAX_VALUE;
    float dminNearby = Float.MAX_VALUE;
    float value = (doSmoothProperty ? 0 : Float.NaN);
    float vdiv = 0;
    sg.atomDataServer.setIteratorForPoint(iter, modelIndex, pt, maxDistance);
    iAtomSurface = -1;
    while (iter.hasNext()) {
      int ia = iter.next();
      int myAtom = myIndex[ia];
      boolean isNearby = (myAtom >= firstNearbyAtom);
      P3 ptA = atomXyzTruncated[myAtom];
      float p = atomProp[myAtom];
      //System.out.println(iAtom + " " + ia + ptA + " " + isNearby + " " + p);
      if (Float.isNaN(p))
        continue;
      float d2 = pt.distanceSquared(ptA);
      if (isNearby) {
        if (d2 < dminNearby) {
          dminNearby = d2;
          if (!doSmoothProperty && dminNearby < dmin) {
            dmin = d2;
            value = Float.NaN;
          }
        }
      } else if (d2 < dmin) {
        dmin = d2;
        iAtomSurface = ia;
        if (!doSmoothProperty)
          value = p;
      }
      if (mepCalc != null) {
        value += mepCalc.valueFor(p, d2, calcType);
      } else if (doSmoothProperty) {
        d2 = (float) Math.pow(d2, smoothingPower);
        vdiv += d2;
        value += d2 * p;
      }
    }
    //System.out.println(pt + " " + value + " " + vdiv + " " + value / vdiv);
    return (mepCalc != null ? value : doSmoothProperty ? (vdiv == 0
        || dminNearby < dmin ? Float.NaN : value / vdiv) : value);
  }

}
