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


import java.util.List;
import java.util.Map;

import javajs.util.P3;
import javajs.util.PT;



/**
 * CIF density reader. See https://www.ebi.ac.uk/pdbe/densities/x-ray/1eve/box, 
 * for example:
 * 
 *  https://www.ebi.ac.uk/pdbe/densities/x-ray/1eve/box/-4.413,55.607,64.124/-0.4130001,59.607,68.124?space=cartesian&encoding=cif
 *  
 * Extends BCifDensityReader just enough to handle nonbinary CIF data.
 *
 */

class CifDensityReader extends BCifDensityReader {

  CifDensityReader(){}
  
  @Override
  protected void getCifData(String fileName, Object data) {
    cifData = sg.atomDataServer.readCifData(fileName, data, "CIF");
  }

  @Override
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
  
  @Override
  @SuppressWarnings("unchecked")
  protected Map<String, Object> getCifMap(String type) {
    type = "data_" + type;
    List<Object> list = (List<Object>) cifData.get("models");
    for (int i = 0; i < list.size(); i++) {
      Map<String, Object> map = (Map<String, Object>) list.get(i);
      if (type.equalsIgnoreCase(map.get("name").toString()))
        return thisData = map;
    }
    return null;
  }
  
  @Override
  protected float getCifFloat(String key) {
    Object o = thisData.get(key);
    float x = Float.NaN;
    if (o != null) {
      if (o instanceof String) {
        x = PT.parseFloat((String) o);
      } else if (o instanceof Number) {
        x = ((Number) o).floatValue();
      }
    }
    return x;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected float[] readCifFloats(String key, float[] values) {
    List<Object> list = (List<Object>) thisData.get(key);
    for (int i = 0, n = values.length; i < n; i++)
      values[i] = PT.parseFloat((String) list.get(i));
    return values;
  }
}




//data_SERVER
//#
//_density_server_result.server_version     0.9.4 
//_density_server_result.datetime_utc       '2018-01-11 13:05:27' 
//_density_server_result.guid               6b9aaa27-a562-4e00-a888-6f500847868f 
//_density_server_result.is_empty           no 
//_density_server_result.has_error          no 
//_density_server_result.error              . 
//_density_server_result.query_source_id    emd/emd-8003 
//_density_server_result.query_type         box 
//_density_server_result.query_box_type     cartesian 
//_density_server_result.query_box_a[0]     -2 
//_density_server_result.query_box_a[1]     7 
//_density_server_result.query_box_a[2]     10 
//_density_server_result.query_box_b[0]     4 
//_density_server_result.query_box_b[1]     10 
//_density_server_result.query_box_b[2]     15.5 
//#
//data_EM
//#
//_volume_data_3d_info.name                         em 
//_volume_data_3d_info.axis_order[0]                0 
//_volume_data_3d_info.axis_order[1]                1 
//_volume_data_3d_info.axis_order[2]                2 
//_volume_data_3d_info.origin[0]                    0 
//_volume_data_3d_info.origin[1]                    0.015625 
//_volume_data_3d_info.origin[2]                    0.021875 
//_volume_data_3d_info.dimensions[0]                0.009375 
//_volume_data_3d_info.dimensions[1]                0.009375 
//_volume_data_3d_info.dimensions[2]                0.015625 
//_volume_data_3d_info.sample_rate                  1 
//_volume_data_3d_info.sample_count[0]              3 
//_volume_data_3d_info.sample_count[1]              3 
//_volume_data_3d_info.sample_count[2]              5 
//_volume_data_3d_info.spacegroup_number            1 
//_volume_data_3d_info.spacegroup_cell_size[0]      443.2 
//_volume_data_3d_info.spacegroup_cell_size[1]      443.2 
//_volume_data_3d_info.spacegroup_cell_size[2]      443.2 
//_volume_data_3d_info.spacegroup_cell_angles[0]    90 
//_volume_data_3d_info.spacegroup_cell_angles[1]    90 
//_volume_data_3d_info.spacegroup_cell_angles[2]    90 
//_volume_data_3d_info.mean_source                  0.000239 
//_volume_data_3d_info.mean_sampled                 0.000239 
//_volume_data_3d_info.sigma_source                 0.015579 
//_volume_data_3d_info.sigma_sampled                0.015579 
//_volume_data_3d_info.min_source                   -0.121007 
//_volume_data_3d_info.min_sampled                  -0.121007 
//_volume_data_3d_info.max_source                   0.287133 
//_volume_data_3d_info.max_sampled                  0.287133 
//#
//loop_
//_volume_data_3d.values
//0.000639 
//0.000667 
//0.000444 
//data_SERVER
//#
//_density_server_result.server_version     0.9.4 
//_density_server_result.datetime_utc       '2018-01-11 13:49:16' 
//_density_server_result.guid               d7da5dff-3343-4082-a96f-95fd979d8567 
//_density_server_result.is_empty           no 
//_density_server_result.has_error          no 
//_density_server_result.error              . 
//_density_server_result.query_source_id    x-ray/1cbs 
//_density_server_result.query_type         box 
//_density_server_result.query_box_type     fractional 
//_density_server_result.query_box_a[0]     0.1 
//_density_server_result.query_box_a[1]     0.1 
//_density_server_result.query_box_a[2]     0.1 
//_density_server_result.query_box_b[0]     0.23 
//_density_server_result.query_box_b[1]     0.31 
//_density_server_result.query_box_b[2]     0.18 
//#
//data_2FO-FC
//#
//_volume_data_3d_info.name                         2Fo-Fc 
//_volume_data_3d_info.axis_order[0]                1 
//_volume_data_3d_info.axis_order[1]                0 
//_volume_data_3d_info.axis_order[2]                2 
//_volume_data_3d_info.origin[0]                    0.1 
//_volume_data_3d_info.origin[1]                    0.092105 
//_volume_data_3d_info.origin[2]                    0.098485 
//_volume_data_3d_info.dimensions[0]                0.2125 
//_volume_data_3d_info.dimensions[1]                0.144737 
//_volume_data_3d_info.dimensions[2]                0.083333 
//_volume_data_3d_info.sample_rate                  1 
//_volume_data_3d_info.sample_count[0]              17 
//_volume_data_3d_info.sample_count[1]              11 
//_volume_data_3d_info.sample_count[2]              11 
//_volume_data_3d_info.spacegroup_number            19 
//_volume_data_3d_info.spacegroup_cell_size[0]      45.65 
//_volume_data_3d_info.spacegroup_cell_size[1]      47.56 
//_volume_data_3d_info.spacegroup_cell_size[2]      77.61 
//_volume_data_3d_info.spacegroup_cell_angles[0]    90 
//_volume_data_3d_info.spacegroup_cell_angles[1]    90 
//_volume_data_3d_info.spacegroup_cell_angles[2]    90 
//_volume_data_3d_info.mean_source                  0 
//_volume_data_3d_info.mean_sampled                 0 
//_volume_data_3d_info.sigma_source                 0.354201 
//_volume_data_3d_info.sigma_sampled                0.354201 
//_volume_data_3d_info.min_source                   -1.298991 
//_volume_data_3d_info.min_sampled                  -1.298991 
//_volume_data_3d_info.max_source                   3.762699 
//_volume_data_3d_info.max_sampled                  3.762699 
//#
//loop_
//_volume_data_3d.values
//0.186828 
//0.11986 
//-0.015395 
//...
//0.117123 
//0.078306 
//-0.026217 
//-0.135757 
//#
//data_FO-FC
//#
//_volume_data_3d_info.name                         Fo-Fc 
//_volume_data_3d_info.axis_order[0]                1 
//_volume_data_3d_info.axis_order[1]                0 
//_volume_data_3d_info.axis_order[2]                2 
//_volume_data_3d_info.origin[0]                    0.1 
//_volume_data_3d_info.origin[1]                    0.092105 
//_volume_data_3d_info.origin[2]                    0.098485 
//_volume_data_3d_info.dimensions[0]                0.2125 
//_volume_data_3d_info.dimensions[1]                0.144737 
//_volume_data_3d_info.dimensions[2]                0.083333 
//_volume_data_3d_info.sample_rate                  1 
//_volume_data_3d_info.sample_count[0]              17 
//_volume_data_3d_info.sample_count[1]              11 
//_volume_data_3d_info.sample_count[2]              11 
//_volume_data_3d_info.spacegroup_number            19 
//_volume_data_3d_info.spacegroup_cell_size[0]      45.65 
//_volume_data_3d_info.spacegroup_cell_size[1]      47.56 
//_volume_data_3d_info.spacegroup_cell_size[2]      77.61 
//_volume_data_3d_info.spacegroup_cell_angles[0]    90 
//_volume_data_3d_info.spacegroup_cell_angles[1]    90 
//_volume_data_3d_info.spacegroup_cell_angles[2]    90 
//_volume_data_3d_info.mean_source                  0 
//_volume_data_3d_info.mean_sampled                 0 
//_volume_data_3d_info.sigma_source                 0.123854 
//_volume_data_3d_info.sigma_sampled                0.123854 
//_volume_data_3d_info.min_source                   -0.688616 
//_volume_data_3d_info.min_sampled                  -0.688616 
//_volume_data_3d_info.max_source                   0.846369 
//_volume_data_3d_info.max_sampled                  0.846369 
//#
//loop_
//_volume_data_3d.values
//0.170378 
//0.08947 
//-0.047389 
//-0.107031 
//...
//0.236658 
//0.068594 
//-0.079848 
//#


//data_SERVER
//#
//_density_server_result.server_version     0.9.4 
//_density_server_result.datetime_utc       '2018-01-11 13:49:16' 
//_density_server_result.guid               d7da5dff-3343-4082-a96f-95fd979d8567 
//_density_server_result.is_empty           no 
//_density_server_result.has_error          no 
//_density_server_result.error              . 
//_density_server_result.query_source_id    x-ray/1cbs 
//_density_server_result.query_type         box 
//_density_server_result.query_box_type     fractional 
//_density_server_result.query_box_a[0]     0.1 
//_density_server_result.query_box_a[1]     0.1 
//_density_server_result.query_box_a[2]     0.1 
//_density_server_result.query_box_b[0]     0.23 
//_density_server_result.query_box_b[1]     0.31 
//_density_server_result.query_box_b[2]     0.18 
//#
//data_2FO-FC
//#
//_volume_data_3d_info.name                         2Fo-Fc 
//_volume_data_3d_info.axis_order[0]                1 
//_volume_data_3d_info.axis_order[1]                0 
//_volume_data_3d_info.axis_order[2]                2 
//_volume_data_3d_info.origin[0]                    0.1 
//_volume_data_3d_info.origin[1]                    0.092105 
//_volume_data_3d_info.origin[2]                    0.098485 
//_volume_data_3d_info.dimensions[0]                0.2125 
//_volume_data_3d_info.dimensions[1]                0.144737 
//_volume_data_3d_info.dimensions[2]                0.083333 
//_volume_data_3d_info.sample_rate                  1 
//_volume_data_3d_info.sample_count[0]              17 
//_volume_data_3d_info.sample_count[1]              11 
//_volume_data_3d_info.sample_count[2]              11 
//_volume_data_3d_info.spacegroup_number            19 
//_volume_data_3d_info.spacegroup_cell_size[0]      45.65 
//_volume_data_3d_info.spacegroup_cell_size[1]      47.56 
//_volume_data_3d_info.spacegroup_cell_size[2]      77.61 
//_volume_data_3d_info.spacegroup_cell_angles[0]    90 
//_volume_data_3d_info.spacegroup_cell_angles[1]    90 
//_volume_data_3d_info.spacegroup_cell_angles[2]    90 
//_volume_data_3d_info.mean_source                  0 
//_volume_data_3d_info.mean_sampled                 0 
//_volume_data_3d_info.sigma_source                 0.354201 
//_volume_data_3d_info.sigma_sampled                0.354201 
//_volume_data_3d_info.min_source                   -1.298991 
//_volume_data_3d_info.min_sampled                  -1.298991 
//_volume_data_3d_info.max_source                   3.762699 
//_volume_data_3d_info.max_sampled                  3.762699 
//#
//loop_
//_volume_data_3d.values
//0.186828 
//0.11986 
//-0.015395 
//...
//0.117123 
//0.078306 
//-0.026217 
//-0.135757 
//#
//data_FO-FC
//#
//_volume_data_3d_info.name                         Fo-Fc 
//_volume_data_3d_info.axis_order[0]                1 
//_volume_data_3d_info.axis_order[1]                0 
//_volume_data_3d_info.axis_order[2]                2 
//_volume_data_3d_info.origin[0]                    0.1 
//_volume_data_3d_info.origin[1]                    0.092105 
//_volume_data_3d_info.origin[2]                    0.098485 
//_volume_data_3d_info.dimensions[0]                0.2125 
//_volume_data_3d_info.dimensions[1]                0.144737 
//_volume_data_3d_info.dimensions[2]                0.083333 
//_volume_data_3d_info.sample_rate                  1 
//_volume_data_3d_info.sample_count[0]              17 
//_volume_data_3d_info.sample_count[1]              11 
//_volume_data_3d_info.sample_count[2]              11 
//_volume_data_3d_info.spacegroup_number            19 
//_volume_data_3d_info.spacegroup_cell_size[0]      45.65 
//_volume_data_3d_info.spacegroup_cell_size[1]      47.56 
//_volume_data_3d_info.spacegroup_cell_size[2]      77.61 
//_volume_data_3d_info.spacegroup_cell_angles[0]    90 
//_volume_data_3d_info.spacegroup_cell_angles[1]    90 
//_volume_data_3d_info.spacegroup_cell_angles[2]    90 
//_volume_data_3d_info.mean_source                  0 
//_volume_data_3d_info.mean_sampled                 0 
//_volume_data_3d_info.sigma_source                 0.123854 
//_volume_data_3d_info.sigma_sampled                0.123854 
//_volume_data_3d_info.min_source                   -0.688616 
//_volume_data_3d_info.min_sampled                  -0.688616 
//_volume_data_3d_info.max_source                   0.846369 
//_volume_data_3d_info.max_sampled                  0.846369 
//#
//loop_
//_volume_data_3d.values
//0.170378 
//0.08947 
//-0.047389 
//-0.107031 
//...
//0.236658 
//0.068594 
//-0.079848 
//#

