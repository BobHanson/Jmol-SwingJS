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


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.util.Map;

import javajs.util.AU;
import javajs.util.BC;
import javajs.util.MessagePackReader;
import javajs.util.P3;
import javajs.util.SB;

import org.jmol.util.Logger;



/**
 * Binary CIF density reader. See https://www.ebi.ac.uk/pdbe/densities/x-ray/1eve/box, 
 * for example:
 * 
 *  https://www.ebi.ac.uk/pdbe/densities/x-ray/1eve/box/-4.413,55.607,64.124/-0.4130001,59.607,68.124?space=cartesian&encoding=bcif
 *  
 * Reads a MessagePack file and extracts either 2Fo-Fc or Fo-Fc.
 *
 */


class BCifDensityReader extends MapFileReader {
  
  private String header;

  BCifDensityReader(){}
  
  protected void getCifData(String fileName, Object data) {
    binarydoc = newBinaryDocument();
    if (AU.isAB(data))
      binarydoc.setStream(new BufferedInputStream(new ByteArrayInputStream((byte[]) data)), true);
    else
      setStream(fileName, true);
    nSurfaces = 1; 
  }

  protected P3 readCifP3(String key, P3 p3) {
    if (p3 == null)
      p3 = new P3();
    float x = getCifFloat(key + "[0]");
    if (Float.isNaN(x)) {
      p3.x = Float.NaN;
    } else {
      p3.x = x;
      p3.y = getCifFloat(key + "[1]");
      p3.z = getCifFloat(key + "[2]");
    }
    return p3;
  }
  
  @SuppressWarnings("unchecked")
  protected Map<String, Object> getCifMap(String type) {
    if (cifData == null)
    try {
      cifData = (new MessagePackReader(binarydoc, true)).readMap();
      System.out.println("BCifDensityReader BCIF encoder " + cifData.get("encoder") + " BCIF version " + cifData.get("version"));
    } catch (Exception e) {
      System.out.println("BCifDensityReader error " + e);
    }    

    Object[] dataBlocks = (Object[]) cifData.get("dataBlocks");
    for (int i = dataBlocks.length; --i >= 0;) {
      Map<String, Object> map = (Map<String, Object>) dataBlocks[i];
      header = map.get("header").toString();
      if ("EM".equals(header) || type.equalsIgnoreCase(header)) {
        // flatten hierarchy
        Object[] categories = (Object[]) map.get("categories");
        for (int j = categories.length; --j >= 0;) {
          Map<String, Object> cat = (Map<String, Object>) categories[j];
          String catName = (String) cat.get("name");
          Object[] columns = (Object[]) cat.get("columns");
          for (int k = columns.length; --k >= 0;) {
          Map<String, Object> col = (Map<String, Object>) columns[k];
            map.put(catName + "_" + col.get("name"), col.get("data"));
          }
        }
        map.remove("categories");
        return thisData = map;
      }
    }
    return null;
  }
  
//  @SuppressWarnings("unchecked")
//  protected String getCifString(String key) {
//    Map<String, Object> map = (Map<String, Object>) thisData.get(key);
//    byte[] data = (byte[]) map.get("data");
//    Map<String, Object> encoding = (Map<String, Object>) ((Object[]) map
//        .get("encoding"))[0];
//    Object o = encoding.get("offsetEncoding");
//    System.out.println(encoding + " " + f);
//    return null;
//  }

  @SuppressWarnings("unchecked")
  protected float getCifFloat(String key) {
    Map<String, Object> map = (Map<String, Object>) thisData.get(key);
    byte[] data = (byte[]) map.get("data");
    int encoding = ((Integer) ((Map<String, Object>) ((Object[]) map
        .get("encoding"))[0]).get("type")).intValue();
    float f = Float.NaN; 
    try {
      switch (encoding) {
      case 3:
        f = BC.bytesToInt(data, 0, false);
        break;
      case 33:
        f = BC.bytesToDoubleToFloat(data, 0, false);
        break;
      default:
        System.out.println("BCifDensityReader: Number encoding not recognized: " + encoding);
        break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
//    System.out.println(encoding + " " + f);
    return f;
  }

  @SuppressWarnings("unchecked")
  protected float[] readCifFloats(String key, float[] values) {
    Map<String, Object> map = (Map<String, Object>) thisData.get(key);
    byte[] data = (byte[]) map.get("data");
    Map<String, Object> encoding = (Map<String, Object>) ((Object[]) map
        .get("encoding"))[0];
    float min = ((Float) encoding.get("min")).floatValue();
    float max = ((Float) encoding.get("max")).floatValue();
    int numSteps = ((Integer) encoding.get("numSteps")).intValue();
    String kind = (String) encoding.get("kind");
    if ("IntervalQuantization".equals(kind)) {
      float delta = (max - min) / (numSteps - 1);
      for (int i = data.length; --i >= 0;) {
        // Java byte[], not uint[]
        values[i] = min + delta * ((data[i] + 256) % 256);
      }
    } else {
      System.out.println("BCifDensityReader: value encoding type? " + kind);
    }
    return values;
  }

  private int pt;

  
  float checkSum;
  
  protected float[] values;
  
  public Map<String, Object> cifData, thisData;
  private boolean isDiff;
  
  @Override
  void init2(SurfaceGenerator sg, BufferedReader br) {    
    allowSigma = true;
    init2MFR(sg, br);
    Object[] o2 = (Object[]) sg.getReaderData();
    String fileName = (String) o2[0];
    // TODO  -- what about cached data -- must append "#diff=1" to that
    Object data = o2[1];
    isDiff = (fileName != null && fileName.indexOf("&diff=1") >= 0
        || data instanceof String && ((String)data).indexOf("#diff=1") >= 0);
    getCifData(fileName, data);
    // data are HIGH on the inside and LOW on the outside
//    if (params.thePlane == null)
//      params.insideOut = !params.insideOut;
    nSurfaces = 1; 
  }

  @Override
  protected void readParameters() throws Exception {

    //    getMap("SERVER");
    //    String type = getString("_density_server_result.query_type");
    //    if (!"box".equals(type))
    //      return;
    //    P3 origin = readP3("_density_server_result.query_box_a", null);
    //    readP3("_density_server_result.query_box_b", p3);
    getCifMap(isDiff ? "FO-FC" : "2FO-FC");

//    String test = getCifString("_volume_data_3d_info_name");
    readCifP3("_volume_data_3d_info_axis_order", p3);

    //    _volume_data_3d_info.axis_order[0]                1 
    //    _volume_data_3d_info.axis_order[1]                0 
    //    _volume_data_3d_info.axis_order[2]                2 

    P3 axis_order = readCifP3("_volume_data_3d_info_axis_order", null);

    //    _volume_data_3d_info.origin[0]                    0.6 
    //    _volume_data_3d_info.origin[1]                    0.5 
    //    _volume_data_3d_info.origin[2]                    0.69697

    P3 fracOrigin = readCifP3("_volume_data_3d_info_origin", null);

    //    _volume_data_3d_info.dimensions[0]                0.4 
    //    _volume_data_3d_info.dimensions[1]                0.5 
    //    _volume_data_3d_info.dimensions[2]                0.30303

    P3 fracDimensions = readCifP3("_volume_data_3d_info_dimensions", null);

    //    _volume_data_3d_info.sample_count[0]              32 
    //    _volume_data_3d_info.sample_count[1]              38 
    //    _volume_data_3d_info.sample_count[2]              40

    P3 sampleCounts = readCifP3("_volume_data_3d_info_sample_count", p3);

    mapc = (int) axis_order.x + 1; // fastest "column"  2 --> y
    mapr = (int) axis_order.y + 1; // intermediat "row" 1 --> x
    maps = (int) axis_order.z + 1; // slowest "section" 3 --> z
    
    int[] crs2abc = new int[3];
    crs2abc[mapc - 1] = 0;
    crs2abc[mapr - 1] = 1;
    crs2abc[maps - 1] = 2;
    
    // Jmol will run through these z slowest, then y next, then x.

    // TODO check for inversion of inside/outside due to switching x/y axes
    
    
    // these counts are for the ordering of the data, not the dimensions of the axes

    n0 = (int) sampleCounts.x;
    n1 = (int) sampleCounts.y;
    n2 = (int) sampleCounts.z
        ;
    // these counts are for the dimensions of the axes

    na = (int) getXYZ(sampleCounts, crs2abc[0]);
    nb = (int) getXYZ(sampleCounts, crs2abc[1]);
    nc = (int) getXYZ(sampleCounts, crs2abc[2]);

    //  _volume_data_3d_info.spacegroup_cell_size[0]      45.65 
    //  _volume_data_3d_info.spacegroup_cell_size[1]      47.56 
    //  _volume_data_3d_info.spacegroup_cell_size[2]      77.61 

    readCifP3("_volume_data_3d_info_spacegroup_cell_size", p3);

    a = p3.x;
    b = p3.y;
    c = p3.z;

    float fa = getXYZ(fracDimensions, crs2abc[0]);
    float fb = getXYZ(fracDimensions, crs2abc[1]);
    float fc = getXYZ(fracDimensions, crs2abc[2]);

    // fraction is in terms of a and in the units of na
    
    xyzStart[xIndex = 0] = getXYZ(fracOrigin, crs2abc[0]) * na / fa;
    xyzStart[yIndex = 1] = getXYZ(fracOrigin, crs2abc[1]) * nb / fb;
    xyzStart[zIndex = 2] = getXYZ(fracOrigin, crs2abc[2]) * nc / fc;

    a *= fa;
    b *= fb;
    c *= fc;
    
    // our "axes" may be shorter than the original ones,
    // but they will be in the same directions


    //  _volume_data_3d_info.spacegroup_cell_angles[0]    90 
    //  _volume_data_3d_info.spacegroup_cell_angles[1]    90 
    //  _volume_data_3d_info.spacegroup_cell_angles[2]    90 

    readCifP3("_volume_data_3d_info_spacegroup_cell_angles", p3);
    alpha = p3.x;
    beta = p3.y;
    gamma = p3.z;

    values = readCifFloats("_volume_data_3d_values", new float[na * nb * nc]);

    //  _volume_data_3d_info.spacegroup_number            19   
    //    _volume_data_3d_info.sample_rate                  1 

    getVectorsAndOrigin();
    
    if (params.thePlane == null && (params.cutoffAutomatic || !Float.isNaN(params.sigma))) {
      float sigma = (params.sigma < 0 || Float.isNaN(params.sigma) ? 1 : params.sigma);
      dmean = getCifFloat("_volume_data_3d_info_mean_sampled");
      float rmsDeviation = getCifFloat("_volume_data_3d_info_sigma_sampled");
      params.cutoff = rmsDeviation * sigma + dmean;
      Logger.info("Cutoff set to (mean + rmsDeviation*" + sigma + " = " + params.cutoff + ")\n");
    }


    //setCutoffAutomatic();

    jvxlFileHeaderBuffer = new SB();
    jvxlFileHeaderBuffer.append("BCifDensity reader type=" + header + "\n");
  }
  
  private float getXYZ(P3 a, float x) {
    switch ((int) x) {
    case 0:
      return a.x;
    case 1:
      return a.y;
    case 2:
    default:
      return a.z;
    }
  }

  @Override
  protected float nextVoxel() throws Exception {
    float v = values[pt++];
    checkSum += v;
    return v;
  }

  @Override
  protected void skipData(int nPoints) throws Exception {
    pt += nPoints;
  }
  
  @Override
  protected void closeReader() {
    if (readerClosed)
      return;
    System.out.println("CifDensityReader checkSum=" + checkSum);
    super.closeReader(); 
  }
}

