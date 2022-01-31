package org.jmol.adapter.writers;

import java.util.Map;

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
    if (bs.length() == 0)
      return "";
    try {
      short mi = vwr.ms.at[bs.nextSetBit(0)].mi;
      Map<String, Object> info = vwr.ms.getModelAuxiliaryInfo(mi);
      Integer asymUnitCount = (Integer) info.get("presymmetryAtomCount");
      int nAtoms = (asymUnitCount == null ? bs.cardinality() : asymUnitCount.intValue());
      
      SB sb = new SB();
      sb.append("# CIF file created by Jmol " + Viewer.getJmolVersion()
          + "\ndata_global");
      SymmetryInterface uc = vwr.ms.getUnitCellForAtom(bs.nextSetBit(0));
      boolean haveUnitCell = (uc != null);
      if (!haveUnitCell)
        uc = vwr.getSymTemp().setUnitCell(new float[] { 1, 1, 1, 90, 90, 90 },
            false);
      float[] params = uc.getUnitCellAsArray(false);
      appendKey(sb, "_cell_length_a").appendF(params[0]);
      appendKey(sb, "_cell_length_b").appendF(params[1]);
      appendKey(sb, "_cell_length_c").appendF(params[2]);
      appendKey(sb, "_cell_angle_alpha").appendF(params[3]);
      appendKey(sb, "_cell_angle_beta").appendF(params[4]);
      appendKey(sb, "_cell_angle_gamma").appendF(params[5]);
      sb.append("\n");
      uc.getSpaceGroupInfo(vwr.ms, null, mi, true, null);
      Object ita = uc.getSpaceGroupNameType("ITA");
      appendKey(sb, "_space_group_IT_number").append(ita == null ? "?" : ita.toString());
      String name = uc.getSpaceGroupNameType("Hall");
      appendKey(sb, "_space_group_name_Hall").append(name == null ? "?" : "'" + name + "'");
      name = uc.getSpaceGroupNameType("HM");
      appendKey(sb, "_space_group_name_H-M_alt").append(name == null ? "?" : "'" + name + "'");
      int n = uc.getSpaceGroupOperationCount();
      sb.append("\n\nloop_\n_space_group_symop_id\n_space_group_symop_operation_xyz");
      if (n == 0) {
        sb.append("\nx,y,z");
      } else {
        for (int i = 0; i < n; i++) {
          sb.append("\n").appendI(i + 1).append("\t").append(uc.getSpaceGroupXyz(i, false).replaceAll(" ", ""));
        }
      }

      sb.append("\n"
          + "\nloop_"
          + "\n_atom_site_label"
          + "\n_atom_site_fract_x"
          + "\n_atom_site_fract_y"
          + "\n_atom_site_fract_z");
      if (!haveUnitCell)
          sb.append("\n_atom_site_Cartn_x"
          + "\n_atom_site_Cartn_y"
          + "\n_atom_site_Cartn_z");
      sb.append("\n");
      Atom[] atoms = vwr.ms.at;
      P3 p = new P3();
      for (int i = bs.nextSetBit(0); i >= 0 && i < nAtoms; i = bs.nextSetBit(i + 1)) {
        Atom a = atoms[i];
        p.setT(a);
        if (haveUnitCell)
          uc.toFractional(p, false);
        name = a.getAtomName();
        String sym = a.getElementSymbol();
        boolean useName = name.startsWith(sym);
        sb.append(PT.formatS(useName ? name : sym, 3, 0, true, false))
            .append(PT.formatF(p.x, 18, 12, false, false))
            .append(PT.formatF(p.y, 18, 12, false, false))
            .append(PT.formatF(p.z, 18, 12, false, false));
        if (!haveUnitCell)
          sb.append(PT.formatF(a.x, 18, 12, false, false))
            .append(PT.formatF(a.y, 18, 12, false, false))
            .append(PT.formatF(a.z, 18, 12, false, false));
       sb.append("  # ").append(useName ? sym : name)
            .append("\n");
      }
      sb.append("\n# ").appendI(nAtoms).append(" atoms\n");
      oc.append(sb.toString());
    } catch (Exception e) {
      //
    }
    return toString();
  }

  private SB appendKey(SB sb, String key) {
    return sb.append("\n").append(PT.formatS(key, 27, 0, true, false));
  }

  @Override
  public String toString() {
    return (oc == null ? "" : oc.toString());
  }


}

