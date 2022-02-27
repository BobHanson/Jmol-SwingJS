package org.jmol.adapter.writers;

import org.jmol.api.JmolDataManager;
import org.jmol.api.JmolWriter;
import org.jmol.api.SymmetryInterface;
import org.jmol.modelset.Atom;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

import javajs.util.AU;
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
public class PWMATWriter implements JmolWriter {


  private Viewer vwr;
  private OC oc;
  private SymmetryInterface uc;
  private String[] names;
  private BS bs;
  

  
  public PWMATWriter() {
    // for JavaScript dynamic loading
  }

  @Override
  public void set(Viewer viewer, OC oc, Object[] data) {
    vwr = viewer;
    this.oc = (oc == null ? vwr.getOutputChannel(null,  null) : oc);
  }

  @Override
  public String write(BS bs) {
    this.bs = (bs == null ? vwr.bsA() : bs);
    try {
    int n = bs.cardinality();
    String line = PT.formatStringI("%12i\n", "i", n);
    oc.append(line);
    uc = vwr.ms.getUnitCellForAtom(bs.nextSetBit(0));
    names = (String[]) vwr.getDataObj("*", bs, JmolDataManager.DATA_TYPE_AF);
    writeLattice();
    writePositions();
    writeDataBlocks();
    } catch (Exception e) {
      System.err.println("Error writing PWmat file " + e);
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
    Logger.info("PWMATWriter: LATTICE VECTORS");
  }

  private void writePositions() {
    float[] cx = getData("CONSTRAINTS_X");
    float[] cy = (cx == null ? null : getData("CONSTRAINTS_Y"));
    float[] cz = (cy == null ? null : getData("CONSTRAINTS_Z"));
    oc.append("Position, move_x, move_y, move_z\n");
    String f = "%4i%18.12p%18.12p%18.12p" + (cz == null ? "  1  1  1" : "%4i%4i%4i") + "\n";
    Atom[] a = vwr.ms.at;
    P3 p = new P3();
    for (int ic = 0, i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1), ic++) {
      p.setT(a[i]);
      uc.toFractional(p, true);
      if (cz == null) {
        oc.append(PT.sprintf(f, "ip", new Object[] { Integer.valueOf(a[i].getElementNumber()), p }));
      } else {
        int ix = (int) cx[ic];
        int iy = (int) cy[ic];
        int iz = (int) cz[ic];
        oc.append(PT.sprintf(f, "ipiii", new Object[] { Integer.valueOf(a[i].getElementNumber()), p, Integer.valueOf(ix), Integer.valueOf(iy),Integer.valueOf(iz) }));
      }
    }    
    Logger.info("PWMATWriter: POSITIONS");
  }
  
  private float[] getData(String name) { 
    name = "property_pwm_" + name.toLowerCase();
    for (int i = names.length; --i >= 0;) {
      if (names[i] != null && name.equalsIgnoreCase(names[i])) {
        names[i] = null;
        return (float[]) vwr.getDataObj(name, bs, JmolDataManager.DATA_TYPE_AF);
      }
    }
    return null;
  }

  private float[][] getVectors(String name) {
    float[][] vectors = AU.newFloat2(3);
    float[] x = vectors[0] = getData(name + "_x");
    x = vectors[1] = (x == null ? null : getData(name + "_y"));
    vectors[2] = (x == null ? null : getData(name + "_z"));
    return (vectors[2] == null ? null : vectors);
  }

  private void writeDataBlocks() {
    writeVectors("FORCE");
    writeVectors("VELOCITY");
    writeMagnetic();
    writeMoreData();
  }

  private void writeVectors(String name) {
    float[][] xyz = getVectors(name);
    if (xyz[0] == null)
      return;
    Atom[] a = vwr.ms.at;
    P3 p = new P3();
    oc.append(name.toUpperCase()).append("\n");
    String f = "%4i%18.12p%18.12p%18.12p\n";
    for (int ic = 0, i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1), ic++) {
      p.set(xyz[0][ic], xyz[1][ic], xyz[2][ic]);
      oc.append(PT.sprintf(f, "ip", new Object[] { Integer.valueOf(a[i].getElementNumber()), p }));
    }    
  }

  private void writeMagnetic() {
    float[] m = writeItems("MAGNETIC");
    if (m == null)
      return;
    writeItem2(m, "CONSTRAINT_MAG");
    writeVectors("MAGNETIC_XYZ");
  }

  private void writeItem2(float[] m, String name) {
    float[] v = getData(name);
    if (v == null)
      return;
    Atom[] a = vwr.ms.at;
    oc.append(name.toUpperCase()).append("\n");
    String f = "%4i%18.12f%18.12f\n";
    for (int ic = 0, i = bs.nextSetBit(0); i >= 0; i = bs
        .nextSetBit(i + 1), ic++) {
      oc.append(PT.sprintf(f, "iff",
          new Object[] { Integer.valueOf(a[i].getElementNumber()),
              Float.valueOf(m[ic]), Float.valueOf(v[ic]) }));
    }
  }

  private float[] writeItems(String name) {
    float[] m = getData(name);
    if (m == null)
      return null;
    Atom[] a = vwr.ms.at;
    oc.append(name.toUpperCase()).append("\n");
    String f = "%4i%18.12f\n";
    for (int ic = 0, i = bs.nextSetBit(0); i >= 0; i = bs
        .nextSetBit(i + 1), ic++) {
      oc.append(PT.sprintf(f, "if", new Object[] {
          Integer.valueOf(a[i].getElementNumber()), Float.valueOf(m[ic]) }));
    }
    return m;
  }

  private void writeMoreData() {
    // TODO
    
  }

  @Override
  public String toString() {
    return (oc == null ? "" : oc.toString());
  }


}

