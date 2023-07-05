package org.jmol.modelset;


import org.jmol.script.T;
import org.jmol.util.BSUtil;

import javajs.util.BS;

public class BondSet extends BS {

  public BondSet() {
  }

  private int[] associatedAtoms;
  
  public static BondSet newBS(BS bs) {
    BondSet b = new BondSet();
    BSUtil.copy2(bs, b);
    return b;
  }

  public int[] getAssociatedAtoms(ModelSet ms) {
    if (associatedAtoms == null)
      associatedAtoms = ms.getAtomIndices(ms.getAtoms(T.bonds, this));
    return associatedAtoms;
  }
}