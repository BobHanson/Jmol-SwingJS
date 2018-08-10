package org.jmol.modelset;


import org.jmol.java.BS;
import org.jmol.util.BSUtil;

public class BondSet extends BS {

  public BondSet() {
  }

  public int[] associatedAtoms;
  
  public static BondSet newBS(BS bs, int[] atoms) {
    BondSet b = new BondSet();
    BSUtil.copy2(bs, b);
    b.associatedAtoms = atoms;
    return b;
  }
}