package org.jmol.adapter.writers;

import java.util.Map;
import java.util.Map.Entry;

import org.jmol.api.JmolDataManager;
import org.jmol.api.JmolWriter;
import org.jmol.api.SymmetryInterface;
import org.jmol.modelset.Atom;
import org.jmol.util.Logger;
import org.jmol.util.SimpleUnitCell;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.OC;
import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.V3d;

/**
 * 
 * A writer for PWMAT atom.config files.
 * 
 */
public class PWMATWriter extends XtlWriter implements JmolWriter {

  private final static String PWM_PREFIX = "property_pwm_";
  private final static int PREFIX_LEN = 13;

  private Viewer vwr;
  private OC oc;
  private SymmetryInterface uc;
  private Lst<String> names;
  private BS bs;
  
  boolean isSlab;
  private boolean writeGlobals = false; // for now
  

  
  public PWMATWriter() {
    isHighPrecision = true;
  }

  @Override
  public void set(Viewer viewer, OC oc, Object[] data) {
    vwr = viewer;
    isSlab = (data != null && data[0] != null && data[0].equals("slab"));
    this.oc = (oc == null ? vwr.getOutputChannel(null,  null) : oc);
  }

  
  @SuppressWarnings("unchecked")
  @Override
  public String write(BS bs) {
    if (bs == null)
      bs = vwr.bsA();
    try {
      uc = vwr.ms.getUnitCellForAtom(bs.nextSetBit(0));
      this.bs = (isSlab ? bs : uc.removeDuplicates(vwr.ms, bs, true));
      names = (Lst<String>) vwr.getDataObj(PWM_PREFIX + "*", null, -1);
      writeHeader();
      writeLattice();
      writePositions();
      writeDataBlocks();
      if (writeGlobals)
        writeGlobalBlocks();
    } catch (Exception e) {
      System.err.println("Error writing PWmat file " + e);
    }
    return toString();
  }

  private void writeHeader() {
    oc.append(PT.formatStringI("%12i\n", "i", bs.cardinality()));
  }

  private void writeLattice() {
    oc.append("Lattice vector\n");
    if (uc == null) {
      uc = vwr.getSymTemp();
      V3d bb = vwr.getBoundBoxCornerVector();
      float len = Math.round(bb.length() * 2);
      uc.setUnitCellFromParams(new double[] { len, len, len, 90, 90, 90 }, false, SimpleUnitCell.SLOPDP);
    }
    P3d[] abc = uc.getUnitCellVectors();
    String f = "%18.10p%18.10p%18.10p\n";
    oc.append(PT.sprintf(f, "p", new Object[] { abc[1] }));
    oc.append(PT.sprintf(f, "p", new Object[] { abc[2] }));
    oc.append(PT.sprintf(f, "p", new Object[] { abc[3] }));
    Logger.info("PWMATWriter: LATTICE VECTORS");
  }

  private void writePositions() {
    double[] cx = getData("CONSTRAINTS_X");
    double[] cy = (cx == null ? null : getData("CONSTRAINTS_Y"));
    double[] cz = (cy == null ? null : getData("CONSTRAINTS_Z"));
    oc.append("Position, move_x, move_y, move_z\n");
    String f = "%4i%40s" + (cz == null ? "  1  1  1" : "%4i%4i%4i") + "\n";
    Atom[] a = vwr.ms.at;
    P3d p = new P3d();
    for (int ic = 0, i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1), ic++) {
      p.setT(a[i]);
      uc.toFractional(p, false);
      String coord = clean(p.x) + clean(p.y) + clean(p.z);
      if (cz == null) {
        oc.append(PT.sprintf(f, "is", new Object[] { Integer.valueOf(a[i].getElementNumber()), coord }));
      } else {
        int ix = (int) cx[ic];
        int iy = (int) cy[ic];
        int iz = (int) cz[ic];
        oc.append(PT.sprintf(f, "isiii", new Object[] { Integer.valueOf(a[i].getElementNumber()), coord, Integer.valueOf(ix), Integer.valueOf(iy),Integer.valueOf(iz) }));
      }
    }    
    Logger.info("PWMATWriter: POSITIONS");
  }
  
  private double[] getData(String name) { 
    name = PWM_PREFIX + name.toLowerCase();
    for (int i = names.size(); --i >= 0;) {
      String n = names.get(i);
      if (name.equalsIgnoreCase(n)) {
        names.removeItemAt(i);
        return (double[]) vwr.getDataObj(n, bs, JmolDataManager.DATA_TYPE_AD);
      }
    }
    return null;
  }

  private double[][] getVectors(String name) {
    double[][] vectors = new double[][] { getData(name + "_x"), getData(name + "_y"), getData(name + "_z") };
    return (vectors[0] == null || vectors[1] == null || vectors[2] == null ? null : vectors);
  }

  private void writeDataBlocks() {
    writeVectors("FORCE");
    writeVectors("VELOCITY");
    writeMagnetic();
    writeMoreData();
  }

  private void writeVectors(String name) {
    double[][] xyz = getVectors(name);
    if (xyz == null)
      return;
    Atom[] a = vwr.ms.at;
    P3d p = new P3d();
    oc.append(name.toUpperCase()).append("\n");
    String f = "%4i%18.12p%18.12p%18.12p\n";
    for (int ic = 0, i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1), ic++) {
      p.set(xyz[0][ic], xyz[1][ic], xyz[2][ic]);
      oc.append(PT.sprintf(f, "ip", new Object[] { Integer.valueOf(a[i].getElementNumber()), p }));
    }    
    Logger.info("PWMATWriter: " + name);
  }

  private void writeMagnetic() {
    double[] m = writeItems("MAGNETIC");
    if (m == null)
      return;
    writeItem2(m, "CONSTRAINT_MAG");
//    writeVectors("MAGNETIC_XYZ");
  }

  private void writeItem2(double[] m, String name) {
    double[] v = getData(name);
    if (v == null)
      return;
    Atom[] a = vwr.ms.at;
    oc.append(name.toUpperCase()).append("\n");
    String f = "%4i%18.12f%18.12f\n";
    for (int ic = 0, i = bs.nextSetBit(0); i >= 0; i = bs
        .nextSetBit(i + 1), ic++) {
      oc.append(PT.sprintf(f, "iff",
          new Object[] { Integer.valueOf(a[i].getElementNumber()),
              Double.valueOf(m[ic]), Double.valueOf(v[ic]) }));
    }
  }

  private double[] writeItems(String name) {
    double[] m = getData(name);
    if (m == null)
      return null;
    Atom[] a = vwr.ms.at;
    name = name.toUpperCase();
    oc.append(name).append("\n");
    String f = "%4i%18.12f\n";
    for (int ic = 0, i = bs.nextSetBit(0); i >= 0; i = bs
        .nextSetBit(i + 1), ic++) {
      oc.append(PT.sprintf(f, "if", new Object[] {
          Integer.valueOf(a[i].getElementNumber()), Double.valueOf(m[ic]) }));
    }
    Logger.info("PWMATWriter: " + name);
    return m;
  }

  private void writeMoreData() {
    int n = names.size();
    int i0 = 0;
    while (names.size() > i0 && --n >= 0) {
      String name = names.get(i0).substring(PREFIX_LEN);
      System.out.println(name);
      if (name.endsWith("_y") || name.endsWith("_z")) {
        i0++;
        continue;
      }
      if (name.endsWith("_x")) {
        writeVectors(name.substring(0, name.length() - 2));
        i0 = 0;
      } else {
        writeItems(name);
      }
    }
  }

  private void writeGlobalBlocks() {
    @SuppressWarnings("unchecked")
    Map<String, Object> globals = (Map<String, Object>) vwr
        .getModelForAtomIndex(bs.nextSetBit(0)).auxiliaryInfo
            .get("globalPWmatData");
    if (globals != null)
      for (Entry<String, Object> e : globals.entrySet()) {
        oc.append(e.getKey()).append("\n");
        String[] lines = (String[]) e.getValue();
        for (int i = 0; i < lines.length; i++)
          oc.append(lines[i]).append("\n");
      }
  }

  @Override
  public String toString() {
    return (oc == null ? "" : oc.toString());
  }


}

