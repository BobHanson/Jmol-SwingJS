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

import javajs.util.SB;
import javajs.util.V3;

class DelPhiBinaryReader extends VolumeFileReader {

  /*
   * also referred to as GRASP format
   * 
   * see http://www.msg.ucsf.edu/local/programs/grasp/html/Appendix%20A.html
   * 
   * "Unformatted" files contain records of the format:
   * 
   * <byte*4 len>data<byte*4 len>
   * 
   * All we do here is read strings and float arrays.
   * 
   */

// DelPhi Potential Map
//
// This is also an unformatted file.  Its contents follow the format:
//
// character*20 uplbl
// character*10 nxtlbl, character*60 toplbl
// real*4 phi(65,65,65)
// character*16 botlbl
// real*4 scale, mid(3)
//
// Here phi contains the map information, mid the grid midpoint, scale the reciprocal
// grid spacing.  The rest are just character strings containing non-Grasp information.

  DelPhiBinaryReader() {}
  
  /**
   * @param sg 
   */
  @Override
  void init2(SurfaceGenerator sg, BufferedReader br) {
    String fileName = (String) ((Object[]) sg.getReaderData())[0];
    init2VFR(sg, br);
    binarydoc = newBinaryDocument();
    setStream(fileName, false);
    // data are HIGH on the inside and LOW on the outside
    nSurfaces = 1; 
    if (params.thePlane == null)
      params.insideOut = !params.insideOut;
    allowSigma = false;
    isAngstroms = true;
  }
      
  private float[] data;
  
  /**
   * this reader has the critical scaling information at the end,
   * so we just load the data straight into an array.
   * 
   */
  @Override
  protected void readParameters() throws Exception {
    
    // character*20 uplbl
    // character*10 nxtlbl, character*60 toplbl
    // real*4 phi(65,65,65)
    // character*16 botlbl
    // real*4 scale, mid(3)

    String uplbl = readString(); 
    Logger.info(uplbl);
    String nxttoplbl = readString();
    Logger.info(nxttoplbl);
    data = readFloatArray();
    Logger.info("DelPhi data length: " + data.length);
    String botlbl = readString(); 
    Logger.info(botlbl);
    float[] scalemid = readFloatArray();
    float scale = scalemid[0];
    Logger.info("DelPhi scale: " + scale);
    
    // I don't understand why this would be needed this way, 
    // but it seems to be the case.
    
    float dx = (scale == 1 ? 54f/64f : 1/scale);
    volumetricVectors[0] = V3.new3(0, 0, dx);
    volumetricVectors[1] = V3.new3(0, dx, 0);
    volumetricVectors[2] = V3.new3(dx, 0, 0);
    Logger.info("DelPhi resolution (pts/angstrom) set to: " + dx);    
    int nx = 65;
    voxelCounts[0] = voxelCounts[1] = voxelCounts[2] = nx;
    Logger.info("DelPhi voxel counts: " + nx);
    
    // determine origin
    
    dx *= (nx - 1) / 2; 
    volumetricOrigin.set(scalemid[1], scalemid[2], scalemid[3]);
    Logger.info("DelPhi center " + volumetricOrigin);
    volumetricOrigin.x -= dx;
    volumetricOrigin.y -= dx;
    volumetricOrigin.z -= dx;
    
    jvxlFileHeaderBuffer = new SB();
    jvxlFileHeaderBuffer.append("DelPhi DATA ").append(nxttoplbl.replace('\n', ' ').trim()).append("\n\n");
  }
  
  private String readString() throws Exception {
    int n = binarydoc.readInt();
    byte[] buf = new byte[n];
    binarydoc.readByteArray(buf, 0, n);
    binarydoc.readInt(); // trailing byte count
    return new String(buf);
  }

  private float[] readFloatArray() throws Exception {
    int n = binarydoc.readInt() >> 2;
    float[] a = new float[n];
    for (int i = 0; i < n; i++)
      a[i] = binarydoc.readFloat();
    binarydoc.readInt(); // trailing byte count
    return a;
  }

  private int pt;
  
  @Override
  protected float nextVoxel() throws Exception {
    nBytes += 4;
    return data[pt++];
  }

  @Override
  protected void skipData(int nPoints) throws Exception {
    pt += nPoints;
  }
}
