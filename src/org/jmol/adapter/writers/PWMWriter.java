package org.jmol.adapter.writers;

import org.jmol.api.JmolWriter;
import org.jmol.api.SymmetryInterface;
import org.jmol.modelset.Atom;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.OC;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.V3;

/**
 * 
 * A writer for PWMAT atom.config files.
 * 
 */
public class PWMWriter implements JmolWriter {


  private Viewer vwr;
  private OC oc;
  private SymmetryInterface uc;

  
  public PWMWriter() {
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
    int n = bs.cardinality();
    String line = PT.formatStringI("%12i\n", "i", n);
    oc.append(line);
    uc = vwr.ms.getUnitCellForAtom(bs.nextSetBit(0));
    writeLattice();
    writePositions(bs);
    } catch (Exception e) {
      //
    }
    return toString();
  }

  private void writeLattice() {
    oc.append("Lattice vector\n");
    if (uc == null) {
      uc = vwr.getSymTemp();
      V3 bb = vwr.getBoundBoxCornerVector();
      float len = Math.round(bb.length() * 2);
      uc.setUnitCell(new float[] { len, len, len, 90, 90, 90 }, false);
    }
    P3[] abc = uc.getUnitCellVectors();
    String f = "%18.10p%18.10p%18.10p\n";
    oc.append(PT.sprintf(f, "p", new Object[] { abc[1] }));
    oc.append(PT.sprintf(f, "p", new Object[] { abc[2] }));
    oc.append(PT.sprintf(f, "p", new Object[] { abc[3] }));

  }

  private void writePositions(BS bs) {
    oc.append("Position, move_x, move_y, move_z\n");
    String f = "%4i%18.12p%18.12p%18.12p  1  1  1\n";
    Atom[] a = vwr.ms.at;
    P3 p = new P3();
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      p.setT(a[i]);
      uc.toFractional(p, true);
      oc.append(PT.sprintf(f, "ip", new Object[] { Integer.valueOf(a[i].getElementNumber()), p }));
    }
    
  }

  @Override
  public String toString() {
    return (oc == null ? "" : oc.toString());
  }


}

