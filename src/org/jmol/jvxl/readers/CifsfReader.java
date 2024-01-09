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
import java.util.Map;

import javajs.util.CifDataParser;
import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;

/*
 * A simple CIF structure factor reader. 
 * 
 */
class CifsfReader extends VolumeFileReader {

  CifsfReader(){}
  
  @Override
  void init2(SurfaceGenerator sg, BufferedReader br) {
    readCIFData(br);
    init2VFR(sg, br);
    nSurfaces = 1;
  }
  private void readCIFData(BufferedReader br) {
    CifDataParser parser = new CifDataParser();
    parser.set(null, br, false);
    Map<String, Object> map = parser.getAllCifDataType("_refln", "_diffrn");
    processMap(map);
  }

  int h0, k0, l0, nh, nk, nl;
  
  private void processMap(Map<String, Object> map) {
    Lst<Object> models = (Lst<Object>) map.get("models");
    if (models.size() == 0)
      return;
    Map<String, Object> model = (Map<String, Object>) models.get(0);
    h0 = (int) getValue(model.get("_diffrn_reflns_limit_h_min"));
    k0 = (int) getValue(model.get("_diffrn_reflns_limit_k_min"));
    l0 = (int) getValue(model.get("_diffrn_reflns_limit_l_min"));
    nh = (int) (getValue(model.get("_diffrn_reflns_limit_h_max")) - h0 + 1);
    nk = (int) (getValue(model.get("_diffrn_reflns_limit_k_max")) - k0 + 1);
    nl = (int) (getValue(model.get("_diffrn_reflns_limit_l_max")) - l0 + 1);

    jvxlFileHeaderBuffer = new SB();
    jvxlFileHeaderBuffer.append("CIF structure factor data\n");
    jvxlFileHeaderBuffer.append("\n");
    volumetricOrigin.set(h0, k0, l0);
    voxelCounts[0] = nh;
    voxelCounts[1] = nk;
    voxelCounts[2] = nl;
    volumetricVectors[0].set(nh - 1, 0, 0);
    volumetricVectors[1].set(0, nk - 1, 0);
    volumetricVectors[2].set(0, 0, nl - 1);
    jvxlFileHeaderBuffer.append(nh + " 0 0 " + (nh - 1) + "\n");
    jvxlFileHeaderBuffer.append(nk + " 0 0 " + (nk - 1) + "\n");
    jvxlFileHeaderBuffer.append(nl + " 0 0 " + (nl - 1) + "\n");
    
    voxelData = new double[nh][nk][nl];
    double[] hdata = getValueArray(model, "_refln_index_h");
    double[] kdata = getValueArray(model, "_refln_index_k");
    double[] ldata = getValueArray(model, "_refln_index_l");
    double[] m1data = getValueArray(model, "_refln_index_m1");
    double[] value = getValueArray(model, "_refln_F_squared_meas");
    if (m1data == null) {
      for (int i = 0; i < hdata.length; i++) {
        voxelData[(int) hdata[i]][(int) kdata[i]][(int) ldata[i]] = value[i];
      }
    } else {
      for (int i = 0; i < m1data.length; i++) {
        if (m1data[i] == 0)
          voxelData[(int) hdata[i]][(int) kdata[i]][(int) ldata[i]] = value[i];
      }
    }
    
    
    
    
    System.out.println("OK");
  }

  /*
  
  # 11. STRUCTURE-FACTOR LIST 

    loop_
  _refln_index_h 
  _refln_index_k 
  _refln_index_l 
  _refln_index_m_1 
  _refln_F_squared_calc_refln_F_squared_meas_refln_F_squared_sigma_refln_observed_status 

  //0 0 0 1 1.15 9.74 9.74 < 
  
   */

  private double[] getValueArray(Map<String, Object> model, String key) {
    Lst<Object>data = (Lst<Object>) model.get(key);
    if (data == null)
      return null;
    double[] d = new double[data.size()];
    for (int i = data.size(); --i >= 0;)
      d[i] = ((Number) data.get(i)).doubleValue();
    return d;
  }

  private static double getValue(Object v) {
    return (v == null ? Double.NaN : ((Number) v).doubleValue());
  }

  /**
   * nothing much here
   * 
   * @exception Exception -- generally a reader issue
   */
  @Override
  protected void readParameters() throws Exception {
  }
  
  int vpt = 0;
  
  protected double nextVoxel() throws Exception {
    // l
    int l = vpt % nl;
    int k = ((vpt - l)/nl) % (nk * nl);
    
    return 0;
  }


}
