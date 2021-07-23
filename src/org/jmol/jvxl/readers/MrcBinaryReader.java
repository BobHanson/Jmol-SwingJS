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

class MrcBinaryReader extends MapFileReader {

  /*
   * also referred to as CCP4 format
   * 
   * examples include the emd_1xxx..map electron microscopy files
   * and xxxx.ccp4 files
   * 
   * Jmol 12.1.33: adjusted to allow for nonstandard "MRC" files
   * that are little-endian, have no labels, have no "MAP" file type marker
   * and are not inside-out.
   *
   */

  
  MrcBinaryReader() {}
  
  /**
   * @param sg 
   */
  @Override
  void init2(SurfaceGenerator sg, BufferedReader br) {
    String fileName = (String) ((Object[]) sg.getReaderData())[0];
    init2MFR(sg, br);
    binarydoc = newBinaryDocument();
    setStream(fileName, true);
    // data are HIGH on the inside and LOW on the outside
    nSurfaces = 1; 
    if (params.thePlane == null)
      params.insideOut = !params.insideOut;
    allowSigma = true;
  }
  

    /* see http://ami.scripps.edu/software/mrctools/mrc_specification.php
     * 
     * many thanks to Eric Martz for providing the test files that
     * made this understandable.
     * 
    1 NX       number of columns (fastest changing in map)
    2 NY       number of rows   
    3 NZ       number of sections (slowest changing in map)
    4 MODE     data type :
         0        image : signed 8-bit bytes range -128 to 127
         1        image : 16-bit halfwords
         2        image : 32-bit reals
         3        transform : complex 16-bit integers
         4        transform : complex 32-bit reals
         6        image : unsigned 16-bit range 0 to 65535
    5 NXSTART number of first column in map (Default = 0)
    6 NYSTART number of first row in map
    7 NZSTART number of first section in map
    8 MX       number of intervals along X
    9 MY       number of intervals along Y
    10  MZ       number of intervals along Z
    11-13 CELLA    cell dimensions in angstroms
    14-16 CELLB    cell angles in degrees
    17  MAPC     axis corresp to cols (1,2,3 for X,Y,Z)
    18  MAPR     axis corresp to rows (1,2,3 for X,Y,Z)
    19  MAPS     axis corresp to sections (1,2,3 for X,Y,Z)
    20  DMIN     minimum density value
    21  DMAX     maximum density value
    22  DMEAN    mean density value
    23  ISPG     space group number 0 or 1 (default=0)
    24  NSYMBT   number of bytes used for symmetry data (multiples of 80)
    25-49 EXTRA    extra space used for anything   - 0 by default
    50-52 ORIGIN   origin in X,Y,Z used for transforms
    53  MAP      character string 'MAP ' to identify file type
    54  MACHST   machine stamp
    55  RMS      rms deviation of map from mean density
    56  NLABL    number of labels being used
    57-256  LABEL(20,10) 10 80-character text labels
    */
    
  protected String[] labels;

  @Override
  protected void readParameters() throws Exception {

    int ispg;
    int nsymbt;
    byte[] extra = new byte[100];
    byte[] map = new byte[4];
    byte[] machst = new byte[4];
    float rmsDeviation;
    int nlabel;

    n0 = binarydoc.readInt(); // CCP4 "extent[0-2]"
    if (n0 < 0 || n0 > 1<<8) {
      setStream(null, false);
      n0 = binarydoc.swapBytesI(n0);
      //removed for PDBE CCP4 files
      //if (params.thePlane == null)
        //params.insideOut = !params.insideOut;
      if (n0 < 0 || n0 > 1000) {
        Logger.info("nx=" + n0 + " not displayable as MRC file");
        throw new Exception("MRC file type not readable");
      }
      Logger.info("reading little-endian MRC file");
    }
    n1 = binarydoc.readInt();
    n2 = binarydoc.readInt();

    mode = binarydoc.readInt();

    if (mode < 0 || mode > 6) {
      setStream(null, false);
      n0 = binarydoc.swapBytesI(n0);
      n1 = binarydoc.swapBytesI(n1);
      n2 = binarydoc.swapBytesI(n2);
      mode = binarydoc.swapBytesI(mode);
    }

    Logger.info("MRC header: mode: " + mode);
    Logger.info("MRC header: nx ny nz: " + n0 + " " + n1 + " " + n2);

    xyzStart[0] = binarydoc.readInt(); // CCP4 "nxyzstart[0-2]"
    xyzStart[1] = binarydoc.readInt();
    xyzStart[2] = binarydoc.readInt();

    Logger.info("MRC header: nxyzStart: " + xyzStart[0] + " " + xyzStart[1]  + " " + xyzStart[2] );

    na = binarydoc.readInt(); // CCP4 "grid[0-2]"
    nb = binarydoc.readInt();
    nc = binarydoc.readInt();

    if (na == 0)
      na = n0 - 1;
    if (nb == 0)
      nb = n1 - 1;
    if (nc == 0)
      nc = n2 - 1;
    
    Logger.info("MRC header: na nb nc: " + na + " " + nb  + " " + nc );

    a = binarydoc.readFloat();
    b = binarydoc.readFloat();
    c = binarydoc.readFloat();
    alpha = binarydoc.readFloat();
    beta = binarydoc.readFloat();
    gamma = binarydoc.readFloat();
    if (alpha == 0) {
      alpha = beta = gamma = 90;
      Logger.info("MRC header: alpha,beta,gamma 0 changed to 90,90,90");
      Logger.info("MRC header: alpha,beta,gamma 0 reversing insideOut sense");
      if (params.thePlane == null)
        params.insideOut = !params.insideOut;
    }

    mapc = binarydoc.readInt(); // CCP4 "crs2xyz[0-2]
    mapr = binarydoc.readInt();
    maps = binarydoc.readInt();
    
    // I do not have this exactly right yet -- just a hack -- EBI  eds/full
    if (mapc == 2 && mapr == 1 && params.thePlane == null)
      params.insideOut = !params.insideOut;

    String s = "" + mapc + mapr + maps;
    Logger.info("MRC header: mapc mapr maps: " + s);

    if (params.thePlane == null && "21321".indexOf(s) >= 1) {
      Logger.info("MRC header: data are xy-reversed");
      params.dataXYReversed = true;
    }

    dmin = binarydoc.readFloat(); 
    dmax = binarydoc.readFloat();
    dmean = binarydoc.readFloat();

    Logger.info("MRC header: dmin,dmax,dmean: " + dmin + "," + dmax + ","
        + dmean);

    ispg = binarydoc.readInt();
    nsymbt = binarydoc.readInt();

    Logger.info("MRC header: ispg,nsymbt: " + ispg + "," + nsymbt);

    binarydoc.readByteArray(extra, 0, extra.length);

    origin.x = binarydoc.readFloat();  // CCP4 "origin2k"
    origin.y = binarydoc.readFloat();
    origin.z = binarydoc.readFloat();

    Logger.info("MRC header: origin: " + origin);

    binarydoc.readByteArray(map, 0, map.length);
    binarydoc.readByteArray(machst, 0, machst.length);

    rmsDeviation = binarydoc.readFloat();

    Logger.info("MRC header: rms: " + rmsDeviation);

    nlabel = binarydoc.readInt();

    Logger.info("MRC header: labels: " + nlabel);

    labels = new String[nlabel];
    if (nlabel > 0)
      labels[0] = "Jmol MrcBinaryReader";

    for (int i = 0; i < 10; i++) {
      s = binarydoc.readString(80).trim();
      if (i < nlabel) {
        labels[i] = s;
        Logger.info(labels[i]);
      }
    }
    
    for (int i = 0; i < nsymbt; i += 80) {
      long position = binarydoc.getPosition();
      s = binarydoc.readString(80).trim();
      if (s.indexOf('\0') != s.lastIndexOf('\0')) {
        // must not really be symmetry info!
        Logger.error("File indicates " + nsymbt + " symmetry lines, but "  + i + " found!");
        binarydoc.seek(position);
        break;
      }
      Logger.info("MRC file symmetry information: " + s);
    }

    Logger.info("MRC header: bytes read: " + binarydoc.getPosition()
        + "\n");

    // setting the cutoff to mean + 2 x RMS seems to work
    // reasonably well as a default.

    getVectorsAndOrigin();
    
    jvxlFileHeaderBuffer = new SB();
    jvxlFileHeaderBuffer.append("MRC DATA ").append(nlabel > 0 ? labels[0]: "").append("\n");
    jvxlFileHeaderBuffer.append("see http://ami.scripps.edu/software/mrctools/mrc_specification.php\n");

    if (params.thePlane == null && (params.cutoffAutomatic || !Float.isNaN(params.sigma))) {
      float sigma = (params.sigma < 0 || Float.isNaN(params.sigma) ? 1 : params.sigma);
      params.cutoff = rmsDeviation * sigma + dmean;
      s = "cutoff set to " + params.cutoff + " (mean + rmsDeviation*sigma = " + dmean + " + " + rmsDeviation + "*" + sigma + ")";
      Logger.info(s);
      jvxlFileHeaderBuffer.append(s + "\n");
    }

  }
  
  @Override
  protected float nextVoxel() throws Exception {
    float voxelValue;
    /*
     *     4 MODE     data type :
         0        image : signed 8-bit bytes range -128 to 127
         1        image : 16-bit halfwords
         2        image : 32-bit reals
         3        transform : complex 16-bit integers
         4        transform : complex 32-bit reals
         6        image : unsigned 16-bit range 0 to 65535

     */
    switch(mode) {
    case 0:
      voxelValue = binarydoc.readByte();
      break;
    case 1:
      voxelValue = binarydoc.readShort();
      break;
    default:
    case 2:
      voxelValue = binarydoc.readFloat();
      break;
    case 3:
      //read first component only
      voxelValue = binarydoc.readShort();
      binarydoc.readShort();
      break;
    case 4:
      //read first component only
      voxelValue = binarydoc.readFloat();
      binarydoc.readFloat();
      break;
    case 6:
      voxelValue = binarydoc.readUnsignedShort();
      break;
    }
    nBytes = binarydoc.getPosition();
    return voxelValue;
  }

  private static byte[] b8 = new byte[8];
  
  @Override
  protected void skipData(int nPoints) throws Exception {
    for (int i = 0; i < nPoints; i++)
      switch(mode) {
      case 0:
        binarydoc.readByte();
        break;
      case 1:
      case 6:
        binarydoc.readByteArray(b8, 0, 2);
        break;
      default:
      case 2:
      case 3:
        binarydoc.readByteArray(b8, 0, 4);
        break;
      case 4:
        binarydoc.readByteArray(b8, 0, 8);
        break;
      }
  }
}
