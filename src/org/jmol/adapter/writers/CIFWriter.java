package org.jmol.adapter.writers;

import org.jmol.api.JmolWriter;
import org.jmol.api.SymmetryInterface;
import org.jmol.modelset.Atom;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.OC;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.SB;

/**
 * An XCrysDen XSF writer
 * 
 * see http://www.xcrysden.org/doc/XSF.html
 * 
 */
public class CIFWriter implements JmolWriter {


  private Viewer vwr;
  private OC oc;

  public CIFWriter() {
    // for JavaScript dynamic loading
  }

  @Override
  public void set(Viewer viewer, OC oc, Object[] data) {
    vwr = viewer;
    this.oc = (oc == null ? vwr.getOutputChannel(null,  null) : oc);
  }

  @Override
  public String write(BS bs) {
    if (bs == null)
      bs = vwr.bsA();
    try {
      SB sb = new SB();
      sb.append("# primitive CIF file created by Jmol " + Viewer.getJmolVersion() + "\ndata_primitive");
      SymmetryInterface uc = vwr.ms.getUnitCellForAtom(bs.nextSetBit(0));
      float[] params = (uc == null ? new float[] {1,1,1,90,90,90} : uc.getUnitCellAsArray(false));
      sb.append("\n_cell_length_a ").appendF(params[0]);
      sb.append("\n_cell_length_b ").appendF(params[1]);
      sb.append("\n_cell_length_c ").appendF(params[2]);
      sb.append("\n_cell_angle_alpha ").appendF(params[3]);
      sb.append("\n_cell_angle_beta ").appendF(params[4]);
      sb.append("\n_cell_angle_gamma ").appendF(params[5]);
      sb.append("\n\n_symmetry_space_group_name_H-M 'P 1'\nloop_\n_space_group_symop_operation_xyz\n'x,y,z'");
      sb.append("\n\nloop_\n_atom_site_label\n_atom_site_fract_x\n_atom_site_fract_y\n_atom_site_fract_z\n");
      Atom[] atoms = vwr.ms.at;
      P3 p = new P3();
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        Atom a = atoms[i];
        p.setT(a);
        if (uc != null)
        uc.toFractional(p, false);
        sb.append(a.getElementSymbol()).append("\t")
        .append(PT.formatF(p.x, 10, 5, true, false)).append("\t")
        .append(PT.formatF(p.y, 10, 5, true, false)).append("\t")
        .append(PT.formatF(p.z, 10, 5, true, false)).append("\n");
      }
      oc.append(sb.toString());
    } catch (Exception e) {
      //
    }
    return toString();
  }

  @Override
  public String toString() {
    return (oc == null ? "" : oc.toString());
  }


}

