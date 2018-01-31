/* $RCSfile$
 * $Author: nicove $
 * $Date: 2007-03-25 06:09:49 -0500 (Sun, 25 Mar 2007) $
 * $Revision: 7221 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
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
package org.openscience.jvxl.simplewriter;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import javajs.util.Lst;
import java.util.Date;



import org.jmol.jvxl.data.JvxlCoder;
import org.jmol.jvxl.data.JvxlData;
import org.jmol.jvxl.data.VolumeData;
import org.jmol.jvxl.readers.Parameters;
import javajs.util.P3;

public class ASimpleJvxlWriter {

  // example for how to create simple JVXL files from cube data
  // no color mapping, no planes, just simple surfaces.

  public static void main(String[] args) {

    // parameters that need setting:

    Parameters params = new Parameters();
    String outputFile = "c:/temp/simple.jvxl";
    params.cutoff = 0.01f;
    params.isCutoffAbsolute = false;
    
    int nX = 31;
    int nY = 31;
    int nZ = 31;

    String[] title = new String[] {"created by SimpleJvxlWriter "
        + new SimpleDateFormat("yyyy-MM-dd', 'HH:mm").format(new Date()) };

    VolumeData volumeData;
    VoxelDataCreator vdc;
    volumeData = new VolumeData();
    volumeData.setVolumetricOrigin(0, 0, 0);
    volumeData.setVolumetricVector(0, 1f, 0f, 0f);
    volumeData.setVolumetricVector(1, 0f, 1f, 0f);
    volumeData.setVolumetricVector(2, 0f, 0f, 1f);
    volumeData.setVoxelCounts(nX, nY, nZ);

    vdc = new VoxelDataCreator(volumeData);
    vdc.createVoxelData();

    // areaVolumeReturn and surfacePointsReturn are optional
    // -- set to null for faster calculation of JVXL data
    
    float[] areaVolumeReturn = new float[2]; // or null;
    Lst<P3> surfacePointsReturn = new  Lst<P3>(); // or null;

    params.isXLowToHigh = false;
    writeFile(outputFile + "A", jvxlGetData(null, params,
        volumeData, title, surfacePointsReturn, areaVolumeReturn));

     System.out.println("calculated area = " + areaVolumeReturn[0] 
                         + " volume = " + areaVolumeReturn[1]
                         + " for " + surfacePointsReturn.size() 
                         + " surface points");
    // streaming option: null voxelData
    volumeData.setVoxelDataAsArray(null);
    params.isXLowToHigh = true;
    writeFile(outputFile + "B", jvxlGetData(vdc, params,
        volumeData, title, surfacePointsReturn, areaVolumeReturn));

    System.out.flush();
    System.exit(0);
  }

  public static String jvxlGetData(VoxelDataCreator vdc, Parameters params,
                                   VolumeData volumeData, String[] title,
                                   Lst<P3> surfacePointsReturn,
                                   float[] areaVolumeReturn) {
    
    JvxlData jvxlData = new JvxlData();
    new SimpleMarchingCubes(vdc, volumeData, params,
        jvxlData, surfacePointsReturn, areaVolumeReturn);
    jvxlData.isXLowToHigh = params.isXLowToHigh;
    jvxlData.cutoff = params.cutoff;
    jvxlData.isCutoffAbsolute = params.isCutoffAbsolute;
    jvxlData.version = "ASimpleJvxlWriter -- version 2.2";
    return JvxlCoder.jvxlGetFile(volumeData, jvxlData, title);
  }

  static void writeFile(String fileName, String text) {
    try {
      FileOutputStream os = new FileOutputStream(fileName);
      BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os), 8192);
      bw.write(text);
      bw.close();
      os = null;
    } catch (IOException e) {
      System.out.println("IO Exception: " + e.toString());
    }
  }

}