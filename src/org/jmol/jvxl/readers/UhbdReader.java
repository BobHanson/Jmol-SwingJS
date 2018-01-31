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

import javajs.util.SB;

/**
 * UHBD reader
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 *  
 */

class UhbdReader extends VolumeFileReader {

  private int planeCount;
  private int voxelCount;

  //  This code is based on
  //
  //  http://sourceforge.net/p/apbs/code/ci/9527462a39126fb6cd880924b3cc4880ec4b78a9/tree/src/mg/vgrid.c
  //
  //    Vio_printf(sock, "%72s\n", title);
  //    Vio_printf(sock, "%12.5e%12.5e%7d%7d%7d%7d%7d\n", 1.0, 0.0, -1, 0, nz, 1, nz);
  //    Vio_printf(sock, "%7d%7d%7d%12.5e%12.5e%12.5e%12.5e\n", nx, ny, nz,
  //    hx, (xmin-hx), (ymin-hx), (zmin-hx));
  //    Vio_printf(sock, "%12.5e%12.5e%12.5e%12.5e\n", 0.0, 0.0, 0.0, 0.0);
  //    Vio_printf(sock, "%12.5e%12.5e%7d%7d", 0.0, 0.0, 0, 0);
  //    icol = 0;
  //    for (k=0; k<nz; k++) {
  //      Vio_printf(sock, "\n%7d%7d%7d\n", k+1, thee->nx, thee->ny);
  //      icol = 0;
  //      for (j=0; j<ny; j++) {
  //        for (i=0; i<nx; i++) {
  //          u = k*(nx)*(ny)+j*(nx)+i;
  //          icol++;
  //          Vio_printf(sock, " %12.5e", thee->data[u]);
  //          if (icol == 6) {
  //            icol = 0;
  //            Vio_printf(sock, "\n");
  //          }
  //        }
  //      }
  //    }
  //    if (icol != 0) Vio_printf(sock, "\n");
   
  UhbdReader(){}
  
  @Override
  void init2(SurfaceGenerator sg, BufferedReader br) {
    init2VFR(sg, br);
    // data are HIGH on the inside and LOW on the outside
    if (params.thePlane == null)
      params.insideOut = !params.insideOut;
    isAngstroms = true;
    nSurfaces = 1;
  }

  @Override
  protected void readParameters() throws Exception {
    rd();
    //                                                        POTENTIAL (kT/e)
    jvxlFileHeaderBuffer = SB.newS(line);
    jvxlFileHeaderBuffer.append("UHBD format ").append(line).append("\n");
    jvxlFileHeaderBuffer.append("see http://sourceforge.net/p/apbs/code/ci/9527462a39126fb6cd880924b3cc4880ec4b78a9/tree/src/mg/vgrid.c\n");
    rd(); // ignored
    // 1.00000e+00 0.00000e+00     -1      0    161      1    161
    rd();
    //    161    161    161 1.56250e+00-8.79940e+01-6.20705e+01-7.07875e+01
    voxelCounts[0] = parseIntStr(line.substring(0, 7));
    voxelCounts[1] = parseIntStr(line.substring(7,14));
    voxelCounts[2] = parseIntStr(line.substring(14, 21));
    float dx = parseFloatStr(line.substring(21, 33));    
    volumetricOrigin.set(parseFloatStr(line.substring(33, 45)), 
        parseFloatStr(line.substring(45,57)), 
        parseFloatStr(line.substring(57, 69)));
    volumetricVectors[0].set(0, 0, dx);
    volumetricVectors[1].set(0, dx, 0);
    volumetricVectors[2].set(dx, 0, 0);
    planeCount = voxelCounts[0]*voxelCounts[1]; 
    rd(); // ignored
    // 0.00000e+00 0.00000e+00 0.00000e+00 0.00000e+00
    rd(); // ignored
    // 0.00000e+00 0.00000e+00      0      0    
  }

  int pt;    
  
  @Override
  protected float nextVoxel() throws Exception {
    if (voxelCount % planeCount == 0) {
      rd();
      //      1    161    161
      pt = 0;
    }
    if (pt%78 == 0) {
      rd();
      // -4.19027e-06 -4.40880e-06 -4.63681e-06 -4.87457e-06 -5.12234e-06 -5.38038e-06
      pt = 0;
    }
    voxelCount++;
    float voxelValue = parseFloatStr(line.substring(pt, pt + 13));
    pt += 13;
    return voxelValue;
  }
}
