package org.jmol.adapter.writers;

import java.util.Map;

import org.jmol.modelset.Atom;
import org.jmol.util.Vibration;
import org.jmol.viewer.JC;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.M3d;
import javajs.util.Qd;
import javajs.util.SB;
import javajs.util.V3d;

/**
 * This class, a subclass of CIFWriter, should only be
 * initialized dynamically and by CIFWriter.
 * 
 * It is intended ONLY as a way of converting 
 * FindSpinGroup JSON output files loaded using
 * the FSGOutputReader using the WRITE command. 
 * 
 * FSGOutputReader creates a Map object scfInfo
 * in the _M model auxiliaryInfo array. When writing
 * CIF files, this array provides the needed information
 * to write the moment data for writing of SCIF files. 
 * 
 * As of 2025.06.07 it has been only very minimally tested.
 * 
 */
@SuppressWarnings("unchecked")
public class FSG2SCIFConverter extends CIFWriter {

  private Map<String, Object> scifInfo;

  private int[] spinIndex;

  private final static V3d v0 = V3d.new3(3.14159,2.71828,1.4142);

  public FSG2SCIFConverter() {
    // for internal use only
    super();
  }

  @Override
  protected void prepareAtomSet(BS bs) {
    super.prepareAtomSet(bs);
    modelInfo = (Map<String, Object>) data[0];
    scifInfo = (Map<String, Object>) data[1];
  }

  private final static String[] headerKeys = new String[] {
      "_space_group_spin.transform_spinframe_P_abc",
      "_space_group_spin.collinear_direction",
      "_space_group_spin.coplanar_perp_uvw",
      "_space_group_spin.rotation_axis_cartn",
      "_space_group_spin.rotation_angle",
      "_space_group_spin.number_SpSG_Chen",
      "_space_group_spin.name_SpSG_Chen", };

  private final static String operationKeys = "\nloop_\n"
      +"_space_group_symop_spin_operation.id\n"
      +"_space_group_symop_spin_operation.xyzt\n"
      +"_space_group_symop_spin_operation.uvw_id\n";

  private final static String latticeKeys = "\nloop_\n"
      + "_space_group_symop_spin_lattice.id\n"
      + "_space_group_symop_spin_lattice.xyzt\n"
      + "_space_group_symop_spin_lattice.uvw_id\n";

  private final static String upartKeys = "\nloop_\n"
      + "_space_group_symop_spin_Upart.id\n"
      + "_space_group_symop_spin_Upart.time_reversal\n"
      + "_space_group_symop_spin_Upart.uvw\n";

  private final static String momentKeys = "\n" + "loop_\n"
      + "_atom_site_spin_moment.label\n" 
      + "_atom_site_spin_moment.axis_u\n"
      + "_atom_site_spin_moment.axis_v\n" 
      + "_atom_site_spin_moment.axis_w\n"
      + "_atom_site_spin_moment.symmform_uvw\n"
      + "_atom_site_spin_moment.magnitude\n";

  @Override
  protected void writeHeader(SB sb) {
    V3d axis = null;
    double angle = 0;
    M3d m3 = (M3d) modelInfo.get(JC.SPIN_ROTATION_MATRIX_APPLIED);
    if (m3 != null) {
      Qd q = Qd.newM(m3);
      axis = q.getNormalDirected(v0);
      angle = q.getThetaDirectedV(v0);
      if (Math.abs(angle) < 1)
        axis = null;
    }

    for (int i = 0; i < headerKeys.length; i++) {
      String type = (String) scifInfo.get("configuration");
      String s;
      switch (i) {
      case 0:
        s = (String) scifInfo.get("spinFrame");
        break;
      case 1:
        s = (type.equals("Collinear") ? "1,0,0" : ".");
        break;
      case 2:
        s = (type.equals("Coplanar") ? "0,0,1" : ".");
        break;
      case 3:
        // rotation axis 
        s = (axis == null ? "?" : "[ " + axis.x + " " + axis.y + " " + axis.z + " ]");
        break;
      case 4:
        // rotation angle
        s = (angle == 0 ? "?" : clean(angle));
        break;
      case 5:
        s = (String) scifInfo.get("fsgID");
        break;
      case 6:
        s = "\n;\n" + scifInfo.get("simpleName") + "\n;\n";
        break;
      default:
        s = "??";
        break;
      }
      appendField(sb, headerKeys[i], 45);
      sb.append(s).append("\n");
    }
  }

  @Override
  protected void writeOperations(SB sb) {
    int[] refsOp = (int[]) scifInfo.get("G0_operationURefs");
    int[] refsLat = (int[]) scifInfo.get("G0_spinLatticeURefs");
    BS bsUparts = getReferences(refsOp, refsLat);
    int[] timeRev = (int[]) scifInfo.get("SCIF_spinListTR");
    addOps(sb, operationKeys, "G0_operations", refsOp, timeRev);
    addOps(sb, latticeKeys, "G0_spinLattice", refsLat, timeRev);
    sb.append(upartKeys);
    Lst<String> uparts = (Lst<String>) scifInfo.get("SCIF_spinList");
    for (int i = bsUparts.nextSetBit(0); i >= 0; i = bsUparts.nextSetBit(i + 1)) {
      appendField(sb, "" + (spinIndex[i]), 3);
      appendField(sb, "" + timeRev[i], 3);
      appendField(sb, uparts.get(i), 30);
      sb.append("\n");
    }
  }

  private BS getReferences(int[] refsOp, int[] refsLat) {
    BS bs = new BS();
    for (int i = refsOp.length; --i >= 0;)
      bs.set(refsOp[i]);
    if (refsLat != null)
      for (int i = refsLat.length; --i >= 0;)
        bs.set(refsLat[i]);
    spinIndex = new int[bs.length()];
    for (int pt = 0, i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      spinIndex[i] = ++pt;
    }
    return bs;
  }

  private static String timeRevValue(int t) {
    return (t < 0 ? "-1" : "+1");
  }

  private void addOps(SB sb, String loopKeys, String opsKey, int[] refs,
                      int[] timeReversal) {
    Lst<String> ops = (Lst<String>) scifInfo.get(opsKey);
    if (ops == null)
      return;
    sb.append(loopKeys);
    for (int i = 0, n = ops.size(); i < n; i++) {
      String xyz = ops.get(i).substring(0, ops.get(i).indexOf('('));
      appendField(sb, "" + (i + 1), 3);
      appendField(sb, xyz + "," + timeRevValue(timeReversal[refs[i]]), 30);
      appendField(sb, "" + spinIndex[refs[i]], 3);
      sb.append("\n");
    }
  }

  @Override
  protected int writeAtomSite(SB sb) {
    int natoms = super.writeAtomSite(sb);
    sb.append(momentKeys);
    for (int i = bsOut.nextSetBit(0), p = 0; i >= 0; i = bsOut
        .nextSetBit(i + 1)) {
      Atom a = atoms[i];
      String label = atomLabels[p++];
      Vibration m = a.getVibrationVector();
      // TODO allow for rotationVector
      if (m == null || m.magMoment == 0)
        continue;
      appendField(sb, label, 5);
      V3d v = (m.v0 == null ? m 
          : m.v0);
      append3(sb, v);
      sb.append(" ?");
      sb.append(" ").append(clean(v.length()));
      sb.append("\n");
    }
    return natoms;
  }
  
}
