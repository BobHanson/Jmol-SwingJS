package org.jmol.adapter.writers;

import org.jmol.api.JmolWriter;
import org.jmol.api.SymmetryInterface;
import org.jmol.modelset.Atom;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.OC;
import javajs.util.P3;
import javajs.util.PT;

/**
 * An XCrysDen XSF writer. Will create Animated XSF files if selected atoms span
 * more than one model.
 * 
 * see http://www.xcrysden.org/doc/XSF.html
 * 
 */
public class XSFWriter implements JmolWriter {

  private Viewer vwr;
  private OC oc;
  private SymmetryInterface uc;
  private int len;

  public XSFWriter() {
    // for JavaScript dynamic loading
  }

  @Override
  public void set(Viewer viewer, OC oc, Object[] data) {
    vwr = viewer;
    this.oc = (oc == null ? vwr.getOutputChannel(null, null) : oc);
  }

  @Override
  public String write(BS bs) {
    if (bs == null)
      bs = vwr.bsA();
    len = bs.length();
    if (len == 0)
      return "";
    try {
      Atom[] a = vwr.ms.at;
      int i0 = bs.nextSetBit(0);
      uc = vwr.ms.getUnitCellForAtom(i0);
      int model1 = a[i0].getModelIndex();
      int model2 = a[len - 1].getModelIndex();
      boolean isAnim = (model2 != model1);
      if (isAnim) {
        int nModels = vwr.ms.getModelBS(bs, false).cardinality();
        oc.append("ANIMSTEPS " + nModels + "\n");
      }
      if (uc != null)
        oc.append("CRYSTAL\n");
      String f = "%4i%18.12p%18.12p%18.12p\n";
      String prefix = (uc == null ? "ATOMS" : "PRIMCOORD");
      for (int lastmi = -1, imodel = 0, i = bs.nextSetBit(0); i >= 0; i = bs
          .nextSetBit(i + 1)) {
        Atom atom = a[i];
        int mi = atom.getModelIndex();
        if (mi != lastmi) {
          String sn = (isAnim ? " " + (++imodel) : "");
          String header = prefix + sn + "\n";
          uc = vwr.ms.getUnitCellForAtom(i);
          if (uc == null) {
            oc.append(header);            
          } else {
            writeLattice(sn);
            oc.append(header);
            BS bsm = vwr.restrictToModel(bs, mi);
            oc.append(PT.formatStringI("%6i 1\n", "i", bsm.cardinality()));
          }
          lastmi = mi;
        }
        oc.append(PT.sprintf(f, "ip",
            new Object[] { Integer.valueOf(atom.getElementNumber()), atom }));
      }
    } catch (Exception e) {
      //
    }
    return toString();
  }

  private void writeLattice(String sn) {
    P3[] abc = uc.getUnitCellVectors();
    String f = "%18.10p%18.10p%18.10p\n";
    String s = PT.sprintf(f, "p", new Object[] { abc[1] })
        + PT.sprintf(f, "p", new Object[] { abc[2] })
        + PT.sprintf(f, "p", new Object[] { abc[3] });
    oc.append("PRIMVEC" + sn + "\n").append(s).append("CONVVEC" + sn + "\n")
        .append(s);
  }

  @Override
  public String toString() {
    return (oc == null ? "" : oc.toString());
  }

}
