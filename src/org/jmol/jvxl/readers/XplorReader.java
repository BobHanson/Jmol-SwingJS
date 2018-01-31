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

import org.jmol.viewer.Viewer;

class XplorReader extends MapFileReader {

  /*
   * http://cci.lbl.gov/~rwgk/shortcuts/htdocs/current/python/iotbx.xplor.map.html
   * http://www.csb.yale.edu/userguides/datamanip/xplor/xplorman/node312.html#SECTION001530000000000000000
   * 
   * VERY preliminary Xplor 3.1 electron density map reader
   * requires "!NTITLE" on second line or "REMARKS" on a later line.
   * 
   * NOT for more recent Xplor files or binary Xplor files. See 
   *   http://www.scripps.edu/rc/softwaredocs/msi/xplor981/formats.html
   * 
   * 
   * Example format for Xplor Maps:
 
       2 !NTITLE
 REMARKS FILENAME=""
 REMARKS scitbx.flex.double to Xplor map format
      24       0      24     120       0     120      54       0      54
 3.20420E+01 1.75362E+02 7.96630E+01 9.00000E+01 9.00000E+01 9.00000E+01
ZYX
       0
-2.84546E-01-1.67775E-01-5.66095E-01-1.18305E+00-1.49559E+00-1.31942E+00
-1.01611E+00-1.00873E+00-1.18992E+00-1.02460E+00-2.72099E-01 5.94242E-01
<deleted>
   -9999
  0.0000E+00  1.0000E+00
That is:
...a blank line
...an integer giving the number of title lines, with mandatory !NTITLE
...title lines in %-264s format
...X, Y, and Z sections giving:
       sections per unit cell, in the given direction
       ordinal of first section in file
       ordinal of last section in file

  MRC equiv:
    mx     nxStart     _x      my    nyStart   _y      mz    nzStart   _z
    144       -6       83      16     -11      26      56     -11      43


    // ZYX here:
     
    nz = _x - nxStart + 1
    ny = _y - nyStart + 1
    nx = _z - nzStart + 1
    
    maps = 3
    mapr = 2
    mapc = 1
    
    
  
MRC header: nx,ny,nz: 38,90,55
MRC header: nxStart,nyStart,nzStart: -11,-6,-11
MRC header: mx,my,mz: 144,16,56
MRC header: mapc,mapr,maps: 2,1,3

...unit cell dimensions
...slow, medium, fast section order, always ZYX
...for each slow section, the section number
...sectional data in special fortran format shown
...-9999
...map average and standard deviation
   */

  XplorReader(){}
  
  @Override
  void init2(SurfaceGenerator sg, BufferedReader br) {
    init2MFR(sg, br);
    if (params.thePlane == null)
      params.insideOut = !params.insideOut;
    nSurfaces = 1;
  }

  private int nBlock;

  @Override
  protected void readParameters() throws Exception {

    jvxlFileHeaderBuffer = new SB();
    int nLines = parseIntStr(getLine());
    for (int i = nLines; --i >= 0; ) {
      line = br.readLine().trim();
      Logger.info("XplorReader: " + line);
      jvxlFileHeaderBuffer.append("# ").append(line).appendC('\n');
    }
    jvxlFileHeaderBuffer.append("Xplor data\nJmol " + Viewer.getJmolVersion() + '\n');

    na = parseIntStr(getLine());
    xyzStart[0] = parseInt();
    n0 = (int) (parseInt() - xyzStart[0] + 1);
    
    nb = parseInt();
    xyzStart[1] = parseInt();
    n1 = (int) (parseInt() - xyzStart[1] + 1);
    
    nc = parseInt();
    xyzStart[2] = parseInt();
    n2 = (int) (parseInt() - xyzStart[2] + 1);
    
    a = parseFloatStr(getLine());
    b = parseFloat();
    c = parseFloat();
    alpha = parseFloat();
    beta = parseFloat();
    gamma = parseFloat();

    getLine();     //"ZYX"
    
    maps = 3;
    mapr = 2;
    mapc = 1;

    getVectorsAndOrigin();      
    setCutoffAutomatic();

    nBlock = voxelCounts[2] * voxelCounts[1];
  }


  private String getLine() throws Exception {
    rd();
    while (line != null && (line.length() == 0 || line.indexOf("REMARKS") >= 0 || line.indexOf("XPLOR:") >= 0))
      rd();
    return line;
  }
  
  private int linePt = Integer.MAX_VALUE;
  private int nRead;
  
  @Override
  protected float nextVoxel() throws Exception {
    if (linePt >= line.length()) {
      rd();
      //System.out.println(nRead + " " + line);
      linePt = 0;
      if ((nRead % nBlock) == 0) {
        //if (Logger.debugging)
          //Logger.info("XplorReader: block " + line + " min/max " 
           //+ dataMin + "/" + dataMax);
        rd();
      }
    }
    if (line == null)
      return 0;
    float val = parseFloatRange(line, linePt, linePt+12);
    linePt += 12;
    nRead++;
    //System.out.println("val " + val);
    return val;
  }
}


