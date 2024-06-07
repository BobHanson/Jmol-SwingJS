package org.jmol.adapter.writers;

import org.jmol.api.JmolWriter;
import org.jmol.api.SymmetryInterface;
import org.jmol.modelset.Atom;
import org.jmol.util.SimpleUnitCell;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.OC;
import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.T3d;

/**
 * An XCrysDen XSF writer
 * 
 * see http://www.xcrysden.org/doc/XSF.html
 * 
 */
public class CIFWriter extends XtlWriter implements JmolWriter {

  private Viewer vwr;
  private OC oc;

  private boolean isP1;

  private final static T3d fset0 = P3d.new3(555, 555, 1);

  public CIFWriter() {
  }

  @Override
  public void set(Viewer viewer, OC oc, Object[] data) {
    vwr = viewer;
    this.oc = (oc == null ? vwr.getOutputChannel(null, null) : oc);
    isP1 = (data != null && data.length > 0 && "P1".equals(data[0]));
  }

  @Override
  public String write(BS bs) {
    if (bs == null)
      bs = vwr.bsA();
    try {
      short mi = vwr.ms.at[bs.nextSetBit(0)].mi;
      SymmetryInterface uc = vwr.getCurrentUnitCell();
      haveUnitCell = (uc != null);
      if (!haveUnitCell)
        uc = vwr.getSymTemp().setUnitCellFromParams(null, false, 0.00001d);
      slop = uc.getPrecision();
      precision = (int) -Math.log10(slop);
      isHighPrecision = (slop == SimpleUnitCell.SLOPDP);
      P3d offset = uc.getFractionalOffset();
      boolean fractionalOffset = offset != null && (offset.x != (int) offset.x
          || offset.y != (int) offset.y || offset.z != (int) offset.z);
      T3d fset;
      boolean haveCustom = (fractionalOffset
          || (fset = uc.getUnitCellMultiplier()) != null
              && (fset.z == 1 ? !fset.equals(fset0) : fset.z != 0));
      SymmetryInterface ucm = uc.getUnitCellMultiplied();
      isP1 = (isP1 || ucm != uc || fractionalOffset
          || uc.getSpaceGroupOperationCount() < 2);
      uc = ucm;
      // only write the asymmetric unit set
      BS modelAU = (!haveUnitCell ? bs
          : isP1 ? uc.removeDuplicates(vwr.ms, bs, isHighPrecision)
              : vwr.ms.am[mi].bsAsymmetricUnit);
      BS bsOut;
      if (modelAU == null) {
        bsOut = bs;
      } else {
        bsOut = new BS();
        bsOut.or(modelAU);
        bsOut.and(bs);
      }
      // pass message back to WRITE via vwr.errorMessageUntranslated
      vwr.setErrorMessage(null, " (" + bsOut.cardinality() + " atoms)");
      if (bsOut.isEmpty())
        return "";

      SB sb = new SB();
      sb.append("## CIF file created by Jmol " + Viewer.getJmolVersion());
      if (haveCustom) {
        sb.append(
            PT.rep("\n" + uc.getUnitCellInfo(false), "\n", "\n##Jmol_orig "));
      }
      sb.append("\ndata_global");
      double[] params = uc.getUnitCellAsArray(false);
      appendKey(sb, "_cell_length_a").append(cleanT(params[0]));
      appendKey(sb, "_cell_length_b").append(cleanT(params[1]));
      appendKey(sb, "_cell_length_c").append(cleanT(params[2]));
      appendKey(sb, "_cell_angle_alpha").append(cleanT(params[3]));
      appendKey(sb, "_cell_angle_beta").append(cleanT(params[4]));
      appendKey(sb, "_cell_angle_gamma").append(cleanT(params[5]));
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

      // write the atoms 

      Atom[] atoms = vwr.ms.at;
      String elements = "";

      boolean haveOccupancy = false;
      double[] occ = (haveUnitCell ? vwr.ms.occupancies : null);
      if (occ != null) {
        for (int i = bsOut.nextSetBit(0); i >= 0; i = bsOut.nextSetBit(i + 1)) {
          if (occ[i] != 1) {
            haveOccupancy = true;
            break;
          }
        }
      }
      boolean haveAltLoc = false;
      for (int i = bsOut.nextSetBit(0); i >= 0; i = bsOut.nextSetBit(i + 1)) {
        if (atoms[i].altloc != '\0') {
          haveAltLoc = true;
          break;
        }
      }
      
      int sbLength = sb.length();

      sb.append("\n" + "\nloop_" + "\n_atom_site_label"
          + "\n_atom_site_type_symbol" + "\n_atom_site_fract_x"
          + "\n_atom_site_fract_y" + "\n_atom_site_fract_z");
      if (haveAltLoc) {
        sb.append("\n_atom_site_disorder_group");
      }
      if (haveOccupancy) {
        sb.append("\n_atom_site_occupancy");
      } else if (!haveUnitCell) {
        sb.append("\n_atom_site_Cartn_x" + "\n_atom_site_Cartn_y"
            + "\n_atom_site_Cartn_z");
      }
      sb.append("\n");

      SB jmol_atom = new SB();
      jmol_atom.append("\n" + "\nloop_" + "\n_jmol_atom_index"
          + "\n_jmol_atom_name" + "\n_jmol_atom_site_label\n");

      int nAtoms = 0;
      P3d p = new P3d();
      int[] elemNums = new int[130];
      for (int i = bsOut.nextSetBit(0); i >= 0; i = bsOut.nextSetBit(i + 1)) {
        Atom a = atoms[i];
        p.setT(a);
        if (haveUnitCell) {
          uc.toFractional(p, !isP1);
        }

        //        if (isP1 && !SimpleUnitCell.checkPeriodic(p))
        //          continue;
        nAtoms++;
        String name = a.getAtomName();
        String sym = a.getElementSymbol();
        int elemno = a.getElementNumber();
        String key = sym + "\n";
        if (elements.indexOf(key) < 0)
          elements += key;
        String label = sym + ++elemNums[elemno];
        sb.append(PT.formatS(label, 5, 0, true, false)).append(" ")
            .append(PT.formatS(sym, 3, 0, true, false)).append(clean(p.x))
            .append(clean(p.y)).append(clean(p.z));
        if (haveAltLoc) {
          sb.append(" ").appendC(a.altloc == '\0' ? '.' : a.altloc);
        }
        if (haveOccupancy) 
          sb.append(" ").append(clean(occ[i]/100));
        else if (!haveUnitCell)
          sb.append(clean(a.x)).append(clean(a.y)).append(clean(a.z));
        sb.append("\n");

        jmol_atom.append(PT.formatS("" + a.getIndex(), 3, 0, false, false))
            .append(" ");
        writeChecked(jmol_atom, name);
        jmol_atom.append(" ").append(PT.formatS(label, 5, 0, false, false))
            .append("\n");
      }

      if (nAtoms > 0) {
        // add atom_type aka element symbol
        sb.append("\nloop_\n_atom_type_symbol\n").append(elements).append("\n");
        sb.appendSB(jmol_atom);
      } else {
        sb.setLength(sbLength);
      }

      sb.append("\n_jmol_atom_count   " +  nAtoms);
      sb.append("\n_jmol_precision    " + precision + "\n");
      oc.append(sb.toString());
    } catch (Exception e) {
      if (!Viewer.isJS)
        e.printStackTrace();
    }
    return toString();
  }


  /**
   * see https://github.com/rcsb/ciftools-java/blob/master/src/main/java/org/rcsb/cif/text/TextCifWriter.java
   * @param output 
   * @param val 
   * @return true if multiline
   * 
   */
  private boolean writeChecked(SB output, String val) {
    if (val == null || val.length() == 0) {
      output.append(". ");
      return false;
    }

    boolean escape = val.charAt(0) == '_';
    String escapeCharStart = "'";
    String escapeCharEnd = "' ";
    boolean hasWhitespace = false;
    boolean hasSingle = false;
    boolean hasDouble = false;
    for (int i = 0; i < val.length(); i++) {
      char c = val.charAt(i);

      switch (c) {
      case '\t':
      case ' ':
        hasWhitespace = true;
        break;
      case '\n':
        writeMultiline(output, val);
        return true;
      case '"':
        if (hasSingle) {
          writeMultiline(output, val);
          return true;
        }

        hasDouble = true;
        escape = true;
        escapeCharStart = "'";
        escapeCharEnd = "' ";
        break;
      case '\'':
        if (hasDouble) {
          writeMultiline(output, val);
          return true;
        }
        escape = true;
        hasSingle = true;
        escapeCharStart = "\"";
        escapeCharEnd = "\" ";
        break;
      }
    }

    char fst = val.charAt(0);
    if (!escape && (fst == '#' || fst == '$' || fst == ';' || fst == '['
        || fst == ']' || hasWhitespace)) {
      escapeCharStart = "'";
      escapeCharEnd = "' ";
      escape = true;
    }

    if (escape) {
      output.append(escapeCharStart).append(val).append(escapeCharEnd);
    } else {
      output.append(val).append(" ");
    }

    return false;
  }

  private void writeMultiline(SB output, String val) {
    output.append("\n;").append(val).append("\n;\n");
  }


  private SB appendKey(SB sb, String key) {
    return sb.append("\n").append(PT.formatS(key, 27, 0, true, false));
  }

  @Override
  public String toString() {
    return (oc == null ? "" : oc.toString());
  }

}
