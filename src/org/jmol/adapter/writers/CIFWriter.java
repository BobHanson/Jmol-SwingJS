package org.jmol.adapter.writers;

import org.jmol.api.JmolWriter;
import org.jmol.api.SymmetryInterface;
import org.jmol.modelset.Atom;
import org.jmol.util.SimpleUnitCell;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.OC;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.T3;

/**
 * An XCrysDen XSF writer
 * 
 * see http://www.xcrysden.org/doc/XSF.html
 * 
 */
public class CIFWriter implements JmolWriter {


  private Viewer vwr;
  private OC oc;

  private boolean isP1;

  private final static P3 fset0 = P3.new3(555,555,1);

  public CIFWriter() {
    // for JavaScript dynamic loading
  }

  @Override
  public void set(Viewer viewer, OC oc, Object[] data) {
    vwr = viewer;
    this.oc = (oc == null ? vwr.getOutputChannel(null,  null) : oc);
    isP1 = (data != null && data.length > 0 && "P1".equals(data[0]));
  }

  @Override
  public String write(BS bs) {
    if (bs == null)
      bs = vwr.bsA();
    try {
      short mi = vwr.ms.at[bs.nextSetBit(0)].mi;

      SymmetryInterface uc = vwr.getCurrentUnitCell();
      boolean haveUnitCell = (uc != null);
      if (!haveUnitCell)
        uc = vwr.getSymTemp().setUnitCell(new float[] { 1, 1, 1, 90, 90, 90 },
            false);

      P3 offset = uc.getFractionalOffset();
      boolean fractionalOffset = offset != null && (offset.x != (int) offset.x
          || offset.y != (int) offset.y || offset.z != (int) offset.z);
      T3 fset;
      boolean haveCustom = (fractionalOffset
          || (fset = uc.getUnitCellMultiplier()) != null
              && (fset.z == 1 ? !fset.equals(fset0) : fset.z != 0));
      SymmetryInterface ucm = uc.getUnitCellMultiplied();
      isP1 |= (ucm != uc || fractionalOffset);
      uc = ucm;

      // only write the asymmetric unit set
      BS modelAU = (isP1 ? null : vwr.ms.am[mi].bsAsymmetricUnit);
      BS bsOut;
      if (modelAU == null) {
        bsOut = bs;
      } else {
        bsOut = new BS();
        bsOut.or(modelAU);
        bsOut.and(bs);
      }
      if (bsOut.cardinality() == 0)
        return "";
      SB sb = new SB();
      sb.append("## CIF file created by Jmol " + Viewer.getJmolVersion());
      if (haveCustom) {
        sb.append(
            PT.rep("\n" + uc.getUnitCellInfo(false), "\n", "\n##Jmol_orig "));
      }
      sb.append("\ndata_global");
      float[] params = uc.getUnitCellAsArray(false);
      appendKey(sb, "_cell_length_a").appendF(params[0]);
      appendKey(sb, "_cell_length_b").appendF(params[1]);
      appendKey(sb, "_cell_length_c").appendF(params[2]);
      appendKey(sb, "_cell_angle_alpha").appendF(params[3]);
      appendKey(sb, "_cell_angle_beta").appendF(params[4]);
      appendKey(sb, "_cell_angle_gamma").appendF(params[5]);
      sb.append("\n");
      int n;
      String hallName;
      String hmName;
      Object ita;

      if (isP1) {
        ita = "1";
        hallName = "P 1";
        hmName = "P1";
        n = 0;
      } else {
        uc.getSpaceGroupInfo(vwr.ms, null, mi, true, null);
        ita = uc.getSpaceGroupNameType("ITA");
        hallName = uc.getSpaceGroupNameType("Hall");
        hmName = uc.getSpaceGroupNameType("HM");
        n = uc.getSpaceGroupOperationCount();
      }
      appendKey(sb, "_space_group_IT_number")
          .append(ita == null ? "?" : ita.toString());
      appendKey(sb, "_space_group_name_Hall")
          .append(hallName == null || hallName.equals("?") ? "?"
              : "'" + hallName + "'");
      appendKey(sb, "_space_group_name_H-M_alt")
          .append(hmName == null ? "?" : "'" + hmName + "'");
      sb.append(
          "\n\nloop_\n_space_group_symop_id\n_space_group_symop_operation_xyz");
      if (n == 0) {
        sb.append("\n1 x,y,z");
      } else {
        for (int i = 0; i < n; i++) {
          sb.append("\n").appendI(i + 1).append("\t")
              .append(uc.getSpaceGroupXyz(i, false).replaceAll(" ", ""));
        }
      }

      sb.append("\n" + "\nloop_" + "\n_atom_site_label" + "\n_atom_site_fract_x"
          + "\n_atom_site_fract_y" + "\n_atom_site_fract_z");
      if (!haveUnitCell)
        sb.append("\n_atom_site_Cartn_x" + "\n_atom_site_Cartn_y"
            + "\n_atom_site_Cartn_z");
      sb.append("\n");
      Atom[] atoms = vwr.ms.at;
      P3 p = new P3();
      int nAtoms = 0;
      for (int c = 0, i = bsOut.nextSetBit(0); i >= 0; i = bsOut.nextSetBit(i + 1)) {
        Atom a = atoms[i];
        p.setT(a);
        if (haveUnitCell) {
          uc.toFractional(p, !isP1);
        }
        if (isP1 && !SimpleUnitCell.checkPeriodic(p))
          continue;
        nAtoms++;
        String name = a.getAtomName();
        String sym = a.getElementSymbol();
        sb.append(PT.formatS(sym + ++c, 5, 0, true, false))
            .append(PT.formatF(p.x, 18, 12, false, false))
            .append(PT.formatF(p.y, 18, 12, false, false))
            .append(PT.formatF(p.z, 18, 12, false, false));
        if (!haveUnitCell)
          sb.append(PT.formatF(a.x, 18, 12, false, false))
              .append(PT.formatF(a.y, 18, 12, false, false))
              .append(PT.formatF(a.z, 18, 12, false, false));
        sb.append("  # ").append(name).append("\n");
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

