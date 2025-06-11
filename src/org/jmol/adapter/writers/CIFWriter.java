package org.jmol.adapter.writers;

import java.util.Map;

import org.jmol.api.Interface;
import org.jmol.api.JmolDataManager;
import org.jmol.api.JmolWriter;
import org.jmol.api.SymmetryInterface;
import org.jmol.modelset.Atom;
import org.jmol.symmetry.SymmetryOperation;
import org.jmol.util.BSUtil;
import org.jmol.util.SimpleUnitCell;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.OC;
import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.T3d;

/**
 * A basic CIF writer only.
 * 
 */
public class CIFWriter extends XtlWriter implements JmolWriter {

  protected Viewer vwr;
  protected OC oc;
  protected Object[] data;

  protected boolean isP1;
  protected boolean isCIF2;
  protected short modelIndex;
  protected boolean haveCustom;
  protected SymmetryInterface uc;
  protected BS bsOut;
  protected Atom[] atoms;
  protected int nops;
  protected String[] atomLabels;
  protected SB jmol_atoms;
  protected Map<String, Object> modelInfo;

  private final static T3d fset0 = P3d.new3(555, 555, 1);

  public CIFWriter() {
  }

  @Override
  public void set(Viewer viewer, OC oc, Object[] data) {
    vwr = viewer;
    this.oc = (oc == null ? vwr.getOutputChannel(null, null) : oc);
    this.data = data;
    isP1 = (data != null && data.length > 0 && "P1".equals(data[0]));
  }

  @SuppressWarnings({ "cast", "unchecked" })
  @Override
  public String write(BS bs) {

    try {
      bs = (bs == null ? vwr.bsA() : BSUtil.copy(bs));
      if (bs.isEmpty())
        return "";
      this.modelInfo = (Map<String, Object>) vwr.ms.getModelAuxiliaryInfo(vwr.ms.at[bs.nextSetBit(0)].mi);
      Map<String, Object> info = (Map<String, Object>) modelInfo.get("scifInfo");
      CIFWriter writer;
      if (info == null) {
        writer = this;
      } else {
        writer = (FSG2SCIFConverter) Interface.getInterface("org.jmol.adapter.writers.FSG2SCIFConverter", vwr, "cifwriter");
        writer.set(vwr, null, new Object[] { modelInfo, info });
        isCIF2 = true;
      }
             
      writer.prepareAtomSet(bs);
      SB sb = new SB();
      if (isCIF2) {
        sb.append("#\\#CIF_2.0\n");
      }
      sb.append("## CIF file created by Jmol " + Viewer.getJmolVersion());
      if (writer.haveCustom) {
        sb.append(
            PT.rep("\n" + uc.getUnitCellInfo(false), "\n", "\n##Jmol_orig "));
      }
      sb.append("\ndata_global\n\n");

      writer.writeHeader(sb);
      writer.writeParams(sb);
      writer.writeOperations(sb);

      int nAtoms = writer.writeAtomSite(sb);

      if (writer.jmol_atoms != null) {
        sb.appendSB(writer.jmol_atoms);
        sb.append("\n_jmol_atom_count   " + nAtoms);
      }
      sb.append("\n_jmol_precision    " + writer.precision + "\n");
      oc.append(sb.toString());
    } catch (Exception e) {
      if (!Viewer.isJS)
        e.printStackTrace();
    }
    return toString();
  }

  protected void prepareAtomSet(BS bs) {
    // note that this is confined to the FIRST model in the set. 
    // TODO allow multiple unit cells? 
	atoms = vwr.ms.at;
    modelIndex = atoms[bs.nextSetBit(0)].mi;
    int n0 = bs.cardinality();
    bs.and(vwr.getModelUndeletedAtomsBitSet(modelIndex));
    if (n0 < bs.cardinality()) {
      System.err.println("CIFWriter Warning: Atoms not in model "
          + (modelIndex + 1) + " ignored");
    }
    uc = vwr.ms.getUnitCell(modelIndex);
    
    haveUnitCell = (uc != null);
    if (!haveUnitCell)
      uc = vwr.getSymTemp().setUnitCellFromParams(null, false, 0.00001f);

    // initialize space group
    nops = (isP1 ? 0 : uc.getSpaceGroupOperationCount());

    slop = uc.getPrecision();
    precision = (int) -Math.log10(slop);
    isHighPrecision = (slop == SimpleUnitCell.SLOPDP); // not used in legacy Jmol
    boolean fractionalOffset = (uc.getFractionalOffset(true) != null);
    T3d fset;
    haveCustom = (fractionalOffset
        || (fset = uc.getUnitCellMultiplier()) != null
            && (fset.z == 1 ? !fset.equals(fset0) : fset.z != 0));
    SymmetryInterface ucm = uc.getUnitCellMultiplied();
    isP1 = (isP1 || ucm != uc || fractionalOffset
        || uc.getSpaceGroupOperationCount() < 2);
    uc = ucm;
    // only write the asymmetric unit set
    BS modelAU = (!haveUnitCell ? bs
        : isP1 ? uc.removeDuplicates(vwr.ms, bs, false) // in SwingJS this is isHighPrecision
            : vwr.ms.am[modelIndex].bsAsymmetricUnit);
    if (modelAU == null) {
      bsOut = bs;
    } else {
      bsOut = new BS();
      bsOut.or(modelAU);
      bsOut.and(bs);
    }
    // pass message back to WRITE via vwr.errorMessageUntranslated
    vwr.setErrorMessage(null, " (" + bsOut.cardinality() + " atoms)");
  }

  protected void writeHeader(SB sb) {
      String hallName;
      String hmName;
      Object ita;

      if (isP1) {
        ita = "1";
        hallName = "P 1";
        hmName = "P1";
      } else {
      uc.getSpaceGroupInfo(vwr.ms, null, modelIndex, true, null);
        ita = uc.geCIFWriterValue(JC.INFO_ITA);
        hallName = uc.geCIFWriterValue(JC.INFO_HALL);
        hmName = uc.geCIFWriterValue(JC.INFO_HM);
      }
    appendKey(sb, "_space_group_IT_number", 27)
          .append(ita == null ? "?" : ita.toString());
    appendKey(sb, "_space_group_name_Hall", 27).append(
        hallName == null || hallName.equals("?") ? "?" : "'" + hallName + "'");
    appendKey(sb, "_space_group_name_H-M_alt", 27)
          .append(hmName == null ? "?" : "'" + hmName + "'");
  }

  protected void writeOperations(SB sb) {
    sb.append(
        "\n\nloop_\n_space_group_symop_id\n_space_group_symop_operation_xyz");
    if (nops == 0) {
      sb.append("\n1 x,y,z");
    } else {
      SymmetryOperation[] symops = (SymmetryOperation[]) uc.getSymmetryOperations();
      for (int i = 0; i < nops; i++) {
        String sop = symops[i].getXyz(true);
        sb.append("\n").appendI(i + 1).append("\t")
            .append(sop.replaceAll(" ", ""));
      }
    }
  }

  protected void writeParams(SB sb) {
    double[] params = uc.getUnitCellAsArray(false);
    appendKey(sb, "_cell_length_a", 27).append(cleanT(params[0]));
    appendKey(sb, "_cell_length_b", 27).append(cleanT(params[1]));
    appendKey(sb, "_cell_length_c", 27).append(cleanT(params[2]));
    appendKey(sb, "_cell_angle_alpha", 27).append(cleanT(params[3]));
    appendKey(sb, "_cell_angle_beta", 27).append(cleanT(params[4]));
    appendKey(sb, "_cell_angle_gamma", 27).append(cleanT(params[5]));
    sb.append("\n");
  }

  protected int writeAtomSite(SB sb) {

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
    double[] parts = (haveAltLoc
        ? (double[]) vwr.getDataObj("property_part", bsOut,
            JmolDataManager.DATA_TYPE_AD)
        : null);

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

      jmol_atoms = new SB();
      jmol_atoms.append("\n" + "\nloop_" + "\n_jmol_atom_index"
          + "\n_jmol_atom_name" + "\n_jmol_atom_site_label\n");

      int nAtoms = 0;
      P3d p = new P3d();
      int[] elemNums = new int[130];
      atomLabels = new String[bsOut.cardinality()];
      for (int pi = 0, labeli = 0, i = bsOut.nextSetBit(0); i >= 0; i = bsOut
        .nextSetBit(i + 1)) {
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
        appendField(sb, label, 5);
        appendField(sb, sym, 3);
        atomLabels[labeli++] = label;
        append3(sb, p);
        if (haveAltLoc) {
          sb.append(" ");
          String sdis;
          if (parts != null) {
            int part = (int) parts[pi++];
            sdis = (part == 0 ? "." : "" + part);
          } else {
            sdis = "" + (a.altloc == '\0' ? '.' : a.altloc);
          }
          sb.append(sdis);
        }
        if (haveOccupancy) 
          sb.append(" ").append(clean(occ[i]/100));
        else if (!haveUnitCell)
        append3(sb, a);
        sb.append("\n");
        appendField(jmol_atoms, "" + a.getIndex(), 3);
        writeChecked(jmol_atoms, name);
        appendField(jmol_atoms, label, 5);
        jmol_atoms.append("\n");
      }

      if (nAtoms > 0) {
        // add atom_type aka element symbol
        sb.append("\nloop_\n_atom_type_symbol\n").append(elements).append("\n");
      } else {
        sb.setLength(sbLength);
        jmol_atoms = null;
      }
    return nAtoms;
  }

  protected static void appendField(SB sb, String val, int width) {
    sb.append(PT.formatS(val, width, 0, true, false)).append(" ");
  }

  protected void append3(SB sb, T3d a) {
    sb.append(clean(a.x)).append(clean(a.y)).append(clean(a.z));
  }

  /**
   * see
   * https://github.com/rcsb/ciftools-java/blob/master/src/main/java/org/rcsb/cif/text/TextCifWriter.java
   * 
   * @param output 
   * @param val 
   * @return true if multiline
   * 
   */
  protected boolean writeChecked(SB output, String val) {
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

  protected void writeMultiline(SB output, String val) {
    output.append("\n;").append(val).append("\n;\n");
  }

  protected SB appendKey(SB sb, String key, int width) {
    return sb.append("\n").append(PT.formatS(key, width, 0, true, false));
  }

  @Override
  public String toString() {
    return (oc == null ? "" : oc.toString());
  }

}
