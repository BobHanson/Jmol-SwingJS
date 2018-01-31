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

import java.io.BufferedReader;


import org.jmol.util.Logger;
import javajs.util.P3;
import org.jmol.util.SimpleUnitCell;

// DSN6, MRC, and XPLOR readers

abstract class MapFileReader extends VolumeFileReader {

  protected float dmin = Float.MAX_VALUE;
  protected float dmax, dmean, drange;

  MapFileReader(){}
  
  @Override
  void init2(SurfaceGenerator sg, BufferedReader br) {
    init2MFR(sg, br);
  }

  protected void init2MFR(SurfaceGenerator sg, BufferedReader br) {
    init2VFR(sg, br);
    isAngstroms = true;
// undocumented; for debugging, I think
//    adjustment = center;
//    if (adjustment == null || Float.isNaN(adjustment.x))
//      adjustment = new P3();
  }

    /* 
     * inputs:
     * 
     a b c    cell dimensions in angstroms
     alpha beta gamma  cell angles in degrees
     nx       number of columns (fastest changing in map)
     ny       number of rows   
     yz       number of sections (slowest changing in map)
     a0       offset of uc origin along a axis
     b0       offset of uc origin along b axis
     c0       offset of uc origin along c axis
     na       number of intervals along uc X -- a/na is unit vector
     nb       number of intervals along uc Y -- b/nb is unit vector
     nc       number of intervals along uc Z -- c/nc is unit vector
     mapc     axis corresp to cols (1,2,3 for X,Y,Z)
     mapr     axis corresp to rows (1,2,3 for X,Y,Z)
     maps     axis corresp to sections (1,2,3 for X,Y,Z)
     originX, originY, originZ   origin in X,Y,Z of unitCell (0,0,0)
    */

  protected int mapc, mapr, maps;
  protected int n0, n1, n2, mode;
  protected float[] xyzStart = new float[3];
  protected int na, nb, nc;
  protected float a, b, c, alpha, beta, gamma;
  protected P3 origin = new P3();
  //protected P3 adjustment = new P3();
  protected P3[] vectors = new P3[3];
  protected int xIndex = -1, yIndex, zIndex;

  protected P3 p3 = new P3();  

  protected void checkInsideOut(int mapc, int mapr, int maps) {
    if (params.thePlane == null)
      params.insideOut = (";123;231;312;".indexOf(";" + mapc + mapr + maps) >= 0);
  }

  protected void getVectorsAndOrigin() {

    checkInsideOut(mapc, mapr, maps);
    
    Logger.info("grid parameters: nx,ny,nz: " + n0 + "," + n1 + "," + n2);
    Logger.info("grid parameters: nxStart,nyStart,nzStart: " + xyzStart[0]
        + "," + xyzStart[1] + "," + xyzStart[2]);

    Logger.info("grid parameters: mx,my,mz: " + na + "," + nb + "," + nc);
    Logger.info("grid parameters: a,b,c,alpha,beta,gamma: " + a + "," + b + ","
        + c + "," + alpha + "," + beta + "," + gamma);
    Logger.info("grid parameters: mapc,mapr,maps: " + mapc + "," + mapr + ","
        + maps);
    Logger.info("grid parameters: originX,Y,Z: " + origin);

    SimpleUnitCell unitCell = SimpleUnitCell.newA(new float[] { a / na, b / nb,
        c / nc, alpha, beta, gamma });

    /*
     
     Many thanks to Eric Martz for helping get this right. 
     Basically we have:
    
     Three principal crystallographic axes: a, b, and c...
     ...also referred to as x, y, and z
     ...also referred to as directions 1, 2, and 3
     ...a mapping of "sheets" "rows" and "columns" of data
        set in the file as 
         
                         s1r1c1...s1r1c9.....
                         s1r2c1...s1r2c9.....
                        
                         s2r1c1...s2r1c9.....
                         s2r2c1...s2r2c9.....
     etc.
     
     In Jmol, we always have x (our [0]) running slowest, so we 
     ultimately must make the following assignment:
     
       MRC "maps" maps to .x or [0]
       MRC "mapr" maps to .y or [1]
       MRC "mapc" maps to .z or [2]
    
     We really don't care if this is actually physical "x" "y" or "z".
     In fact, for a hexagonal cell these will be combinations of xyz.
     
     So it goes something like this:
     
     scale the (a) vector by 1/mx and call that vector[0]
     scale the (b) vector by 1/my and call that vector[1]
     scale the (c) vector by 1/mz and call that vector[2]
     
     Now map these vectors to Jmol volumetricVectors using
    
     our x: volVector[0] = vector[maps - 1]  (slow)
     our y: volVector[1] = vector[mapr - 1] 
     our z: volVector[2] = vector[mapc - 1]  (fast)
    
     This is because our x is the slowest running variable.
    */

    vectors[0] = P3.new3(1, 0, 0);
    vectors[1] = P3.new3(0, 1, 0);
    vectors[2] = P3.new3(0, 0, 1);
    unitCell.toCartesian(vectors[0], false);
    unitCell.toCartesian(vectors[1], false);
    unitCell.toCartesian(vectors[2], false);

    Logger.info("Jmol unit cell vectors:");
    Logger.info("    a: " + vectors[0]);
    Logger.info("    b: " + vectors[1]);
    Logger.info("    c: " + vectors[2]);

    // we must pass through the data as it is present
    
    voxelCounts[0] = n2; // slowest
    voxelCounts[1] = n1;
    voxelCounts[2] = n0; // fastest

    volumetricVectors[0].setT(vectors[maps - 1]);
    volumetricVectors[1].setT(vectors[mapr - 1]);
    volumetricVectors[2].setT(vectors[mapc - 1]);

    // only use nxyzStart if the origin is {0, 0, 0}

    if (origin.x == 0 && origin.y == 0 && origin.z == 0) {

      // older method -- wow! Beats me.....

      if (xIndex == -1) {
        int[] xyz2crs = new int[3];
        xyz2crs[mapc - 1] = 0; // mapc = 2 ==> [1] = 0
        xyz2crs[mapr - 1] = 1; // mapr = 1 ==> [0] = 1
        xyz2crs[maps - 1] = 2; // maps = 3 ==> [2] = 2
        xIndex = xyz2crs[0]; // xIndex = 1
        yIndex = xyz2crs[1]; // yIndex = 0
        zIndex = xyz2crs[2]; // zIndex = 2
      }

      origin.scaleAdd2(xyzStart[xIndex]/* + adjustment.x */, vectors[0],
          origin);
      origin.scaleAdd2(xyzStart[yIndex]/* + adjustment.y */, vectors[1],
          origin);
      origin.scaleAdd2(xyzStart[zIndex]/* + adjustment.z */, vectors[2],
          origin);
      
    }

    volumetricOrigin.setT(origin);

    Logger.info("Jmol grid origin in Cartesian coordinates: " + origin);
    Logger.info("Use  isosurface OFFSET {x y z}  if you want to shift it.\n");
    
    p3.set(na, nb, nc);
    unitCell.toCartesian(p3, true);
    p3.add(origin);
    Logger.info("boundbox corners " +  origin + " " + p3 + ";draw bbox boundbox mesh nofill");

    /* example:
        
    isosurface within 5 {_Fe} "1blu.ccp4";
    reading isosurface data from C:/jmol-dev/workspace/Jmol/bobtest/1blu.ccp4
    FileManager opening C:\jmol-dev\workspace\Jmol\bobtest\1blu.ccp4
    data file type was determined to be MRC-
    FileManager opening C:\jmol-dev\workspace\Jmol\bobtest\1blu.ccp4
    MRC header: mode: 2
    MRC header: dmin,dmax,dmean: -2.0043933,4.9972544,-0.0151823275
    MRC header: ispg,nsymbt: 152,0
    MRC header: rms: 0.46335652
    MRC header: labels: 1
    Created by MAPMAN V. 080625/7.8.5 at Tue Jan 19 08:04:40 2010 for A. Nonymous
    MRC header: bytes read: 1024

    cutoff set to (dmean + 2*rms) = 0.91153073
    grid parameters: nx,ny,nz: 73,60,66
    grid parameters: nxStart,nyStart,nzStart: -12,23,-32
    grid parameters: mx,my,mz: 78,78,114
    grid parameters: a,b,c,alpha,beta,gamma: 52.0,52.0,77.1875,90.0,90.0,120.0
    grid parameters: mapc,mapr,maps: 2,1,3
    grid parameters: originX,Y,Z: 0.0,0.0,0.0
    Jmol unit cell vectors:
    a: (0.6666667, 0.0, 0.0)
    b: (-0.33333337, 0.57735026, 0.0)
    c: (-2.9596254E-8, -5.1262216E-8, 0.6770833)
    Jmol grid origin in Cartesian coordinates: (19.333334, -6.9282017, -21.666666)
    Jmol origin in slow-to-fast system: (19.333334, -6.9282017, -21.666666)

    */

  }    
  
  protected void setCutoffAutomatic() {
    if (params.thePlane == null && params.cutoffAutomatic) {
      params.cutoff = -1f;
      Logger.info("MapReader: setting cutoff to default value of "
          + params.cutoff
          + (boundingBox == null ? " (no BOUNDBOX parameter)\n" : "\n"));
    }
  }


}
