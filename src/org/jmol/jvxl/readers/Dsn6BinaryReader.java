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


import java.io.DataInputStream;
import java.io.BufferedReader;

import org.jmol.util.Logger;

import javajs.util.Rdr;
import javajs.util.SB;


class Dsn6BinaryReader extends MapFileReader {

  /*
   * DSN6 map file reader. 
   * 
   * http://eds.bmc.uu.se/eds/
   * 
   * Also referred to as "O" format
   * 
   * see http://www.ks.uiuc.edu/Research/vmd/plugins/doxygen/dsn6plugin_8C-source.html
   *
   */

  
  Dsn6BinaryReader(){}
  
  @Override
  void init2(SurfaceGenerator sg, BufferedReader brNull) {
    init2MFR(sg, br);
    binarydoc = newBinaryDocument();
    Object[] o2 = (Object[]) sg.getReaderData();
    String fileName = (String) o2[0];
    String data = (String) o2[1];
    if (data == null)
      binarydoc.setStream(sg.atomDataServer.getBufferedInputStream(fileName), true);
    else 
      binarydoc.setStreamData(new DataInputStream(Rdr.getBIS(data.getBytes())), true);
    // data are HIGH on the inside and LOW on the outside
    if (params.thePlane == null)
      params.insideOut = !params.insideOut;
    nSurfaces = 1; 
  }

  private float byteFactor;
  private int xyCount;
  private int nBrickX, nBrickY;
  private int brickLayerVoxelCount;
  private int brickLayerByteCount;
  private int brickRowByteCount;
  private byte[] brickLayer;

  @Override
  protected void readParameters() throws Exception {
    
    short[] header = new short[19];
    for (int i = 0; i < 19; i++)
      header[i] = binarydoc.readShort();
    if (header[18] != 100) {
      binarydoc.setStream(null, false);
      for (int i = 0; i < 19; i++)
        header[i] = binarydoc.swapBytesS(header[i]);
    }
    
    xyzStart[0] = header[0];
    xyzStart[1] = header[1];
    xyzStart[2] = header[2];

    n0 = header[3]; // CCP4 "extent[0-2]"
    n1 = header[4];
    n2 = header[5];

    na = header[6]; // CCP4 "grid[0-2]"
    nb = header[7];
    nc = header[8];

    a = header[9];
    b = header[10];
    c = header[11];
    alpha = header[12];
    beta = header[13];
    gamma = header[14];

    float header16 = header[15]; // 100 * 255 / (dmax - dmin)
    float header17 = header[16]; // -255dmin / (dmax - dmin)
    float scalingFactor = header[17];
    float header19 = header[18];

    maps = 3;
    mapr = 2;
    mapc = 1;

    // range parameters are adjusted to be short integers
    //
    // original: byte range b from 3 to 253 for data values m to M
    // [ref: http://www.uoxray.uoregon.edu/tnt/manual/node104.html]
    //
    // value = [ (b - 3) / 250 ] (M - m) + m
    //       = [ b - 3 ] * (M - m) / 250 + m
    //       = [ b - 3 + m * 250 / (M - m) ] * (M - m) / 250
    //       = [ b - (3 (M - m) - 250 m) / (M - m) ] * (M - m) / 250
    //       = [ b - (3M - 253m) / (M - m) ] * (M - m) / 250
    //       = [ b - header17 ] * header19 / header16
    //
    // where header16 = 100 * 250 / (M - m)
    //   and header17 = (3M - 253m) / (M - m)
    //   and header19 = 100
    //
    // Comment: Perhaps, but what it is actually is simply this:
    // [ref: empirical fit to CCP4 and XPLOR data from Uppsala server]
    //
    //   value = [ b / 255 ] (M - m) + m
    //
    // That is, we are just scaling min to max allowing for a range of 255 byte values.
    // So then what we REALLY have is this:
    //
    // value = [ b / 255 ] (M - m) + m
    //       = [ b ] * (M - m) / 255 + m
    //       = [ b + m * 255 / (M - m) ] * (M - m) / 255
    //       = [ b - (-255 m) / (M - m) ] * (M - m) / 255
    //       = [ b - header17 ] * header19 / header16
    //
    // where header16 = 100 * 255 / (M - m)
    //   and header17 = -255m / (M - m)
    //   and header19 = 100
    //
    // If you ask me, this is rather odd, because you 
    // then have restricted the resolution to only 255 possible values. 
    // Is the raw data really that low resolution?
    // Note that this format has a minimum range limitation of 0.389 = 25500 / 0x10000
    // and also a severe limitation in m / (M - m), which very quickly loses precision
    // when m is small or (M - m) is large.
    //
    // In any case, what we do here is simply to
    // calculate min and max from headers 16, 17, and 19,
    // and then just use:
    // 
    //   value = min + b * byteFactor
    //
    // where byteFactor = (M - m) / 255
    //
    // Just seems simpler to me. Bob Hanson 2/2010
    
    dmin = (0 - header17) * header19 / header16;
    dmax = (255 - header17) * header19 / header16;
    drange = dmax - dmin;
    byteFactor = drange / 255;
    
    // just to satisfy my curiosity:
    
    float dminError1 = (0 - header17 - 0.5f) * header19 / (header16 - 0.5f);
    float dminError2 = (0 - header17 + 0.5f) * header19 / (header16 + 0.5f);
    float dmaxError1 = (255 - header17 - 0.5f) * header19 / (header16 - 0.5f);
    float dmaxError2 = (255 - header17 + 0.5f) * header19 / (header16 + 0.5f);

    float dminError = Math.round((dminError2 - dminError1) / 0.002f) * 0.001f;
    float dmaxError = Math.round((dmaxError2 - dmaxError1) / 0.002f) * 0.001f;
    
    Logger.info("DNS6 dmin,dmax = " + dmin + "+/-" + dminError + "," + dmax + "+/-" + dmaxError);

    
    
    a /= scalingFactor;
    b /= scalingFactor;
    c /= scalingFactor;
    alpha /= scalingFactor;
    beta /= scalingFactor;
    gamma /= scalingFactor;

    binarydoc.seek(0x200);

    getVectorsAndOrigin();
    setCutoffAutomatic();

    xyCount = n0 * n1;
    brickLayerVoxelCount = xyCount * 8;
    // byte blocks are 8 layers of 8x8 voxels, with remainders
    nBrickX = (n0 + 7) / 8;
    nBrickY = (n1 + 7) / 8;
    brickRowByteCount = nBrickX * 512;
    brickLayerByteCount = brickRowByteCount * nBrickY;
    brickLayer = new byte[brickLayerByteCount];
    
    jvxlFileHeaderBuffer = new SB();
    jvxlFileHeaderBuffer.append("DNS6/O progressive brick data reader\n");
    jvxlFileHeaderBuffer
        .append("see http://www.uoxray.uoregon.edu/tnt/manual/node104.html\n");

  }
  
  private int pt;
  private void readBrickLayer() throws Exception {
    /*
     * Read one full layer of nBrickX*nBrickY 8x8x8 "bricks".
     * 
     */
    binarydoc.readByteArray(brickLayer, 0, brickLayerByteCount);
    pt = 0;
    nBytes = binarydoc.getPosition();
    //System.out.println("DNs6B reader: " + nBytes);
  }
  
  private float getBrickValue(int pt) {
    /*
     * pt runs from [0, n), where n is the number of voxels in a layer of bricks
     * But we are running at a specific z through the xy plane, rapidly throuch x.
     * You can think of the strips of data being laid out in this order:
     * 
     * brick 0,0,0:
     * z = 0, y = 0, x [0-7]
     * ...
     * z = 0, y = 7, x [0-7]
     * z = 1, y = 0, x [0-7]
     * ...
     * z = 7, y = 7, x [0-7]
     * 
     * brick 0,0,8:
     * z = 0, y = 0, x [8-15]
     * ...
     * z = 0, y = 7, x [8-15]
     * z = 1, y = 0, x [8-15]
     * ...
     * z = 7, y = 7, x [8-15]
     * 
     * So we need to reconstruct from the pointer x, y, and z.
     * 
     */
    
    // within the data:
    
    int x = pt % n0;
    int y = (pt / n0) % n1;
    int z = pt / xyCount;
    
    // within the brick:
    
    int brickX = x % 8;
    int brickY = y % 8;
    int brickZ = z % 8;
    
    // brick pointers:
    
    int bX = x / 8;
    int bY = y / 8;
    //        brick row           brick col   
    int bPt = bY * 512 * nBrickX + bX * 512 + brickZ * 64 + brickY * 8 + brickX;
    
    // reversing byte order:
    
    if (bPt % 2 == 0)
      bPt++;
    else
      bPt--;
    // bytes read can be negative
    float value = (brickLayer[bPt] + 256) % 256; 
    return dmin + value * byteFactor;
  }
  
  @Override
  protected float nextVoxel() throws Exception {
    if ((pt % brickLayerVoxelCount) == 0)
      readBrickLayer();
    return getBrickValue(pt++);
  }

  @Override
  protected void skipData(int nPoints) throws Exception {
    for (int i = 0; i < nPoints; i++)
        binarydoc.readByte();
  }
}
