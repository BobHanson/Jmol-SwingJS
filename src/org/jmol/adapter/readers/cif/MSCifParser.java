/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-20 07:48:25 -0500 (Fri, 20 Oct 2006) $
 * $Revision: 5991 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.adapter.readers.cif;


import javajs.util.M3;
import javajs.util.Matrix;
import javajs.util.PT;


public class MSCifParser extends MSRdr {

  public MSCifParser() {
    // for reflection
  }
  
  private String field;

  // incommensurate modulation
  ////////////////////////////////////////////////////////////////
 
//  The occupational distortion of a given atom or rigid group is
//  usually parameterized by Fourier series. Each term of the series
//  commonly adopts two different representations: the sine-cosine
//  form,
//           Pc cos(2\p k r)+Ps sin(2\p k r),
//  and the modulus-argument form,
//           |P| cos(2\p k r+\d),
//  where k is the wave vector of the term and r is the atomic
//  average position. _atom_site_occ_Fourier_param_phase is the phase
//  (\d/2\p) in cycles corresponding to the Fourier term defined by
//  _atom_site_occ_Fourier_atom_site_label and
//  _atom_site_occ_Fourier_wave_vector_seq_id.

  private final static int FWV_ID = 0;
  private final static int WV_ID = 1;
  private final static int WV_X = 2;
  private final static int WV_Y = 3;
  private final static int WV_Z = 4;
  private final static int FWV_X = 5;
  private final static int FWV_Y = 6;
  private final static int FWV_Z = 7;
  private final static int FWV_Q_COEF = 8;
  private final static int JANA_FWV_Q1_COEF = 9;
  private final static int JANA_FWV_Q2_COEF = 10;
  private final static int JANA_FWV_Q3_COEF = 11;
  private final static int FWV_DISP_LABEL = 12;
  private final static int FWV_DISP_AXIS = 13;
  private final static int FWV_DISP_SEQ_ID = 14;
  private final static int FWV_DISP_COS = 15;
  private final static int FWV_DISP_SIN = 16;
  private final static int FWV_DISP_MODULUS = 17;
  private final static int FWV_DISP_PHASE = 18;
  private final static int DISP_SPEC_LABEL = 19;
  private final static int DISP_SAW_AX = 20;
  private final static int DISP_SAW_AY = 21;
  private final static int DISP_SAW_AZ = 22;
  private final static int DISP_SAW_C = 23;
  private final static int DISP_SAW_W = 24;
  private final static int FWV_OCC_LABEL = 25;
  private final static int FWV_OCC_SEQ_ID = 26;
  private final static int FWV_OCC_COS = 27;
  private final static int FWV_OCC_SIN = 28;
  private final static int FWV_OCC_MODULUS = 29;
  private final static int FWV_OCC_PHASE = 30;
  private final static int OCC_SPECIAL_LABEL = 31;
  private final static int OCC_CRENEL_C = 32;
  private final static int OCC_CRENEL_W = 33;
  private final static int FWV_U_LABEL = 34;
  private final static int FWV_U_TENS = 35;
  private final static int FWV_U_SEQ_ID = 36;
  private final static int FWV_U_COS = 37;
  private final static int FWV_U_SIN = 38;
  private final static int FWV_U_MODULUS = 39;
  private final static int FWV_U_PHASE = 40;
  private final static int FD_ID = 41;
  private final static int FO_ID = 42;
  private final static int FU_ID = 43;
  private final static int FDP_ID = 44;
  private final static int FOP_ID = 45;
  private final static int FUP_ID = 46;
  private final static int JANA_OCC_ABS_LABEL = 47;
  private final static int JANA_OCC_ABS_O_0 = 48;
  private final static int FWV_SPIN_LABEL = 49;
  private final static int FWV_SPIN_AXIS = 50;
  private final static int FWV_SPIN_SEQ_ID = 51;
  private final static int FWV_SPIN_COS = 52;
  private final static int FWV_SPIN_SIN = 53;
  private final static int FWV_SPIN_MODULUS = 54;
  private final static int FWV_SPIN_PHASE = 55;
  private final static int SPIN_SPEC_LABEL = 56;
  private final static int SPIN_SAW_AX = 57;
  private final static int SPIN_SAW_AY = 58;
  private final static int SPIN_SAW_AZ = 59;
  private final static int SPIN_SAW_C = 60;
  private final static int SPIN_SAW_W = 61;
  private final static int LEG_DISP_LABEL = 62;
  private final static int LEG_DISP_AXIS = 63;
  private final static int LEG_DISP_ORDER = 64;
  private final static int LEG_DISP_COEF = 65;
  private final static int LEG_U_LABEL = 66;
  private final static int LEG_U_TENS = 67;
  private final static int LEG_U_ORDER = 68;
  private final static int LEG_U_COEF = 69;
  private final static int LEG_OCC_LABEL = 70;
  private final static int LEG_OCC_ORDER = 71;
  private final static int LEG_OCC_COEF = 72;
  private final static int DEPR_FD_COS = 73;
  private final static int DEPR_FD_SIN = 74;
  private final static int DEPR_FO_COS = 75;
  private final static int DEPR_FO_SIN = 76;
  private final static int DEPR_FU_COS = 77;
  private final static int DEPR_FU_SIN = 78;

  // * will be _atom_site
  final private static String[] modulationFields = {
    "*_fourier_wave_vector_seq_id",  // *_ must be first
    "_cell_wave_vector_seq_id", 
    "_cell_wave_vector_x", 
    "_cell_wave_vector_y", 
    "_cell_wave_vector_z", 
    "*_fourier_wave_vector_x", // 5
    "*_fourier_wave_vector_y", 
    "*_fourier_wave_vector_z",
    "*_fourier_wave_vector_q_coeff", //an array
    "*_fourier_wave_vector_q1_coeff", //_jana 
    "*_fourier_wave_vector_q2_coeff", // jana 
    "*_fourier_wave_vector_q3_coeff", // jana
    "*_displace_fourier_atom_site_label", 
    "*_displace_fourier_axis", 
    "*_displace_fourier_wave_vector_seq_id", // 14 
    "*_displace_fourier_param_cos",  
    "*_displace_fourier_param_sin",
    "*_displace_fourier_param_modulus", 
    "*_displace_fourier_param_phase", 
    "*_displace_special_func_atom_site_label", // 19 
    "*_displace_special_func_sawtooth_ax", 
    "*_displace_special_func_sawtooth_ay", 
    "*_displace_special_func_sawtooth_az", 
    "*_displace_special_func_sawtooth_c", 
    "*_displace_special_func_sawtooth_w", 
    "*_occ_fourier_atom_site_label", 
    "*_occ_fourier_wave_vector_seq_id", 
    "*_occ_fourier_param_cos", // 27
    "*_occ_fourier_param_sin",
    "*_occ_fourier_param_modulus", 
    "*_occ_fourier_param_phase", 
    "*_occ_special_func_atom_site_label", // 31
    "*_occ_special_func_crenel_c",
    "*_occ_special_func_crenel_w",

    "*_u_fourier_atom_site_label",
    "*_u_fourier_tens_elem",
    "*_u_fourier_wave_vector_seq_id", // 36
    "*_u_fourier_param_cos",
    "*_u_fourier_param_sin",
    "*_u_fourier_param_modulus",
    "*_u_fourier_param_phase",
    
    "*_displace_fourier_id", // 41
    "*_occ_fourier_id",
    "*_u_fourier_id",

    "*_displace_fourier_param_id", // 44
    "*_occ_fourier_param_id",
    "*_u_fourier_param_id",
    
    "*_occ_fourier_absolute_site_label", // 47 // _jana
    "*_occ_fourier_absolute",  // _jana

    "*_moment_fourier_atom_site_label", // 49 
    "*_moment_fourier_axis", 
    "*_moment_fourier_wave_vector_seq_id", // 51 
    "*_moment_fourier_param_cos",  
    "*_moment_fourier_param_sin", 
    "*_moment_fourier_param_modulus", 
    "*_moment_fourier_param_phase", 
    "*_moment_special_func_atom_site_label",// 56 
    "*_moment_special_func_sawtooth_ax",  
    "*_moment_special_func_sawtooth_ay", 
    "*_moment_special_func_sawtooth_az", 
    "*_moment_special_func_sawtooth_c", 
    "*_moment_special_func_sawtooth_w",  // 61
    
    //_jana:
    "*_displace_legendre_atom_site_label", // 62 
    "*_displace_legendre_axis", 
    "*_displace_legendre_param_order",  // NOT SUPPORTED
    "*_displace_legendre_param_coeff",  // NOT SUPPORTED
    "*_u_legendre_atom_site_label", // 66  
    "*_u_legendre_tens_elem",
    "*_u_legendre_param_order", 
    "*_u_legendre_param_coeff",
    "*_occ_legendre_atom_site_label", 
    "*_occ_legendre_param_order",
    "*_occ_legendre_param_coeff", // 72    
    // deprecated:
    "*_displace_fourier_cos", // 73
    "*_displace_fourier_sin",
    "*_occ_fourier_cos",
    "*_occ_fourier_sin",
    "*_u_fourier_cos",
    "*_u_fourier_sin" // 78
  };
  
  private static final int NONE = -1;

  private M3 comSSMat;
  public void processEntry() throws Exception {
    CifReader cr = (CifReader) this.cr;
    if (cr.key.equals("_cell_commen_t_section_1")) {
      isCommensurate = true;
      commensurateSection1 = cr.parseIntStr(cr.data);
    }
    if (cr.key.startsWith("_cell_commen_supercell_matrix")) {
      isCommensurate = true;
      if (comSSMat == null)
        comSSMat = M3.newM3(null);
      String[] tokens = PT.split(cr.key, "_");
      int r = cr.parseIntStr(tokens[tokens.length - 2]);
      int c = cr.parseIntStr(tokens[tokens.length - 1]);
      if (r > 0 && c > 0)
        comSSMat.setElement(r - 1, c - 1, cr.parseFloatStr(cr.data)); 
    }
  }

  /**
   * creates entries in htModulation with a key of the form:
   * 
   * type_id_axis;atomLabel@model
   * 
   * where type = W|F|D|O (wave vector, Fourier index, displacement, occupancy);
   * id = 1|2|3|0|S (Fourier index, Crenel(0), sawtooth); axis (optional) =
   * 0|x|y|z (0 indicates irrelevant -- occupancy); and ;atomLabel is only for D
   * and O.
   * 
   * @return 1:handled; -1: skip; 0: unrelated
   * @throws Exception
   */
  public int processLoopBlock() throws Exception {
    CifReader cr = (CifReader) this.cr;
    String key = cr.key;
    if (key.equals("_cell_subsystem_code"))
      return processSubsystemLoopBlock();
    
    if (!key.startsWith("_cell_wave") && !key.contains("fourier")
        && !key.contains("legendre") && !key.contains("_special_func")) {
      if (key.contains("crenel_ortho"))
        cr.appendLoadNote("WARNING: Orthogonalized non-Legendre functions not supported.\nThe following block has been ignored. Use Legendre functions instead.\n\n" 
            + cr.parser.skipLoop(true) + "=================================\n");
      return 0;
    }
    if (cr.asc.iSet < 0)
      cr.asc.newAtomSet();
    cr.parseLoopParametersFor(CifReader.FAMILY_ATOM, modulationFields);
    int tok;
//    if (cr.key2col[JANA_FWV_Q1_COEF] != NONE) {
//      // disable x y z for atom_site_fourier if we have coefficients
//      cr.key2col[FWV_X] = cr.key2col[FWV_Y] = cr.key2col[FWV_Z] = NONE;
//    }
    while (cr.parser.getData()) {
      boolean ignore = false;
      String type_id = null;
      String atomLabel = null;
      String axis = null;
      double[] pt = new double[] { Double.NaN, Double.NaN, Double.NaN };
      double[] q = null;
      double c = Double.NaN;
      double w = Double.NaN;
      String fid = null;
      int n = cr.parser.getColumnCount();
      for (int i = 0; i < n; ++i) {
        switch (tok = fieldProperty(cr, i)) {
        case FWV_ID:
          // BH 2020.09.16 This was producing F_11 and F_22
          // by pulling it out, the field is not added twice.
          pt[0] = pt[1] = pt[2] = 0;
          type_id = "F_";
          fid = field;
          break;
        case WV_ID:
          cr.haveCellWaveVector = true;
          //$FALL-THROUGH$
        case FD_ID:
        case FO_ID:
        case FU_ID:
          pt[0] = pt[1] = pt[2] = 0;
          //$FALL-THROUGH$
        case FWV_DISP_SEQ_ID:
        case FWV_OCC_SEQ_ID:
        case FWV_SPIN_SEQ_ID:
        case FWV_U_SEQ_ID:
        case FDP_ID:
        case FOP_ID:
        case FUP_ID:
          switch (tok) {
          case WV_ID:
            type_id = "W_";
            break;
          case FD_ID:
          case FO_ID:
          case FU_ID:
            fid = "?" + field;
            pt[2] = 1;
            continue;
          case FDP_ID:
          case FOP_ID:
          case FUP_ID:
            atomLabel = axis = "*";
            //$FALL-THROUGH$
          case FWV_DISP_SEQ_ID:
          case FWV_OCC_SEQ_ID:
          case FWV_SPIN_SEQ_ID:
          case FWV_U_SEQ_ID:
            type_id = Character.toUpperCase(modulationFields[tok].charAt(11))
                + "_";
            break;
          }
          type_id += field;
          break;
        case JANA_OCC_ABS_LABEL:
          type_id = "J_O";
          pt[0] = pt[2] = 1;
          axis = "0";
          atomLabel = field;
          break;
        case OCC_SPECIAL_LABEL:
          type_id = "O_0";
          axis = "0";
          atomLabel = field;
          break;
        case DISP_SPEC_LABEL:
          type_id = "D_S";
          axis = "0";
          atomLabel = field;
          break;
        case SPIN_SPEC_LABEL:
          type_id = "M_T";
          axis = "0";
          atomLabel = field;
          break;
        case LEG_DISP_LABEL:
          type_id = "D_L";
          atomLabel = field;
          break;
        case LEG_U_LABEL:
          type_id = "U_L";
          atomLabel = field;
          break;
        case LEG_OCC_LABEL:
          type_id = "O_L";
          atomLabel = field;
          break;
        case FWV_DISP_LABEL:
        case FWV_OCC_LABEL:
        case FWV_SPIN_LABEL:
        case FWV_U_LABEL:
          atomLabel = field;
          break;
        case FWV_DISP_AXIS:
        case FWV_SPIN_AXIS:
        case LEG_DISP_AXIS:
          axis = field;
          if (modAxes != null && modAxes.indexOf(axis.toUpperCase()) < 0)
            ignore = true;
          break;
        case LEG_U_TENS:
        case FWV_U_TENS:
          axis = field.toUpperCase();
          break;
        case FWV_Q_COEF:
          q = Cif2DataParser.getArrayFromStringList(field, modDim);
          break;
        default:
          float f = cr.parseFloatStr(field);
          switch (tok) {
          case LEG_DISP_COEF:
          case LEG_OCC_COEF:
          case LEG_U_COEF:
            pt[0] = f;
            if (f != 0)
              pt[2] = 0;
            break;
          case FWV_OCC_SIN:
          case OCC_CRENEL_C:
          case FWV_DISP_SIN:
          case FWV_SPIN_SIN:
          case FWV_U_SIN:
          case DEPR_FU_SIN:
          case DEPR_FD_SIN:
          case DEPR_FO_SIN:
            pt[2] = 0;
            //$FALL-THROUGH$
          case WV_X:
          case FWV_X:
          case DISP_SAW_AX:
          case SPIN_SAW_AX:
            pt[0] = f;
            break;
          case JANA_FWV_Q1_COEF:
            // type_id += _coefs_
            if (q == null)
              q = new double[modDim];
            q[0] = f;
            break;
          case JANA_FWV_Q2_COEF:
            if (q == null)
              q = new double[modDim];
            q[1] = f;
            break;
          case JANA_FWV_Q3_COEF:
            if (q == null)
              q = new double[modDim];
            q[2] = f;
            break;
          case FWV_DISP_MODULUS:
          case FWV_OCC_MODULUS:
          case FWV_SPIN_MODULUS:
          case FWV_U_MODULUS:
            pt[0] = f;
            pt[2] = 1;
            break;
          case LEG_OCC_ORDER:
          case DEPR_FO_COS:
          case FWV_OCC_COS:
            axis = "0";
            //$FALL-THROUGH$
          case LEG_DISP_ORDER:
          case LEG_U_ORDER:
          case WV_Y:
          case FWV_Y:
          case FWV_DISP_PHASE:
          case FWV_OCC_PHASE:
          case FWV_SPIN_PHASE:
          case FWV_U_PHASE:
          case OCC_CRENEL_W:
          case DISP_SAW_AY:
          case SPIN_SAW_AY:
          case JANA_OCC_ABS_O_0:
          case FWV_DISP_COS:
          case FWV_SPIN_COS:
          case FWV_U_COS:
          case DEPR_FD_COS:
          case DEPR_FU_COS:
            pt[1] = f;
            break;
          case WV_Z:
          case FWV_Z:
          case DISP_SAW_AZ:
          case SPIN_SAW_AZ:
            pt[2] = f;
            break;
          case DISP_SAW_C:
          case SPIN_SAW_C:
            c = f;
            break;
          case DISP_SAW_W:
          case SPIN_SAW_W:
            w = f;
            break;
          }
          break;
        }
      }
      if (ignore || type_id == null && q == null 
          || atomLabel != null && !atomLabel.equals("*") && cr.rejectAtomName(atomLabel))
        continue;
      boolean ok = true;
      
      if (q != null) {
        for (int j = 0, nzero = q.length; j < q.length; j++)
          if (Double.isNaN(q[j]) || q[j] > 1e100 || q[j] == 0 && --nzero == 0) {
            ok = false;
          }
        if (!ok)
          continue;
        addMod("F_coefs_", fid, q);
        // we have a Q-coef, so we ignore any x y z also in this block
        pt[0] = Double.NaN;
      }      
      for (int j = 0, nzero = pt.length; j < pt.length; j++)
        if (Double.isNaN(pt[j]) || pt[j] > 1e100 || pt[j] == 0 && --nzero == 0) {
          ok = false;
          break;
        }
      if (!ok)
        continue;
      switch (type_id.charAt(0)) {
      case 'C':
      case 'D':
      case 'O':
      case 'M':
      case 'U':
      case 'J':
        if (atomLabel == null || axis == null)
          continue;
        if (type_id.equals("D_S") || type_id.equals("M_T")) {
          // saw tooth displacement  center/width/Axyz
          if (Double.isNaN(c) || Double.isNaN(w))
            continue;
          if (pt[0] != 0)
            addMod(type_id + "#x;" + atomLabel, fid,
                new double[] { c, w, pt[0] });
          if (pt[1] != 0)
            addMod(type_id + "#y;" + atomLabel, fid,
                new double[] { c, w, pt[1] });
          if (pt[2] != 0)
            addMod(type_id + "#z;" + atomLabel, fid,
                new double[] { c, w, pt[2] });
          continue;
        }
        if (type_id.indexOf("_L") == 1) {
          if (type_id.startsWith("U"))
            type_id += (int) pt[1]; // axis is Utype here
          else
            axis += (int) pt[1];
        }
        type_id += "#" + axis + ";" + atomLabel;
        break;
      }
      addMod(type_id, fid, pt);
    }
    return 1;
  }
  
  private void addMod(String id, String fid, double[] params) {
    if (fid != null)
      id += fid;
    addModulation(null, id, params, -1);
  }

  //loop_
  //_cell_subsystem_code
  //_cell_subsystem_description
  //_cell_subsystem_matrix_W_1_1
  //_cell_subsystem_matrix_W_1_2
  //_cell_subsystem_matrix_W_1_3
  //_cell_subsystem_matrix_W_1_4
  //_cell_subsystem_matrix_W_2_1
  //_cell_subsystem_matrix_W_2_2
  //_cell_subsystem_matrix_W_2_3
  //_cell_subsystem_matrix_W_2_4
  //_cell_subsystem_matrix_W_3_1
  //_cell_subsystem_matrix_W_3_2
  //_cell_subsystem_matrix_W_3_3
  //_cell_subsystem_matrix_W_3_4
  //_cell_subsystem_matrix_W_4_1
  //_cell_subsystem_matrix_W_4_2
  //_cell_subsystem_matrix_W_4_3
  //_cell_subsystem_matrix_W_4_4
  //1 '1-st subsystem' 1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 1
  //2 '2-nd subsystem' 1 0 0 1 0 1 0 0 0 0 1 0 0 0 0 1
  //
  
  private int processSubsystemLoopBlock() throws Exception {
    CifReader cr = (CifReader) this.cr;
    cr.parseLoopParameters(null);
    while (cr.parser.getData()) {
      fieldProperty(cr, 0);
      String id = field;
      addSubsystem(id, getSparseMatrix(cr, "_w_", 1, 3 + modDim));
    }
    return 1;
  }

//  private int processTwinMatrixLoopBlock() throws Exception {
//    CifReader cr = (CifReader) this.cr;
//    cr.parseLoopParameters(null);
//    while (cr.parser.getData()) {
//      fieldProperty(cr, 0);
//      String id = field;
//      addTwin(id, getSparseMatrix(cr, "_matrix_", 1, 3));
//    }
//    return 1;
//  }

//  private void addTwin(String id, Matrix m) {
//    //TODO implement twinning
//    System.out.println("twin " + id + " = " + m);    
//  }

  private Matrix getSparseMatrix(CifReader cr, String term, int i, int dim) {
    Matrix m = new Matrix(null, dim, dim);
    double[][] a = m.getArray();
    String key;
    int p;
    int n = cr.parser.getColumnCount();
    for (; i < n; ++i) {
      if ((p = fieldProperty(cr, i)) < 0 
          || !(key = cr.parser.getColumnName(p)).contains(term))
        continue;
      String[] tokens = PT.split(key, "_");
      int r = cr.parseIntStr(tokens[tokens.length - 2]);
      int c = cr.parseIntStr(tokens[tokens.length - 1]);
      if (r > 0 && c > 0)
        a[r - 1][c - 1] = cr.parseFloatStr(field);
    }
    return m;
  }

  private int fieldProperty(CifReader cr, int i) {
    return ((field = (String) cr.parser.getColumnData(i)).length() > 0 
        && field.charAt(0) != '\0' ? 
            cr.col2key[i] : NONE);
  }

}
