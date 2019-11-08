package org.jmol.minimize;

import javajs.util.BS;

public class MMConstraint {

  public final static int TYPE_DISTANCE = 0;
  public final static int TYPE_ANGLE    = 1;
  public final static int TYPE_DIHEDRAL = 2;

  // given:
  public int[] indexes; // [n, ia, ib, ....]
  public double value;
  
  // derived:
  public int type;
  public int[] minList = new int[4];
  public int nAtoms;
  
  
  public MMConstraint(int[] indexes, double value) {
    this.value = value;
    this.indexes = indexes;
  }

  public void set(int steps, BS bsAtoms, int[] atomMap) {
    nAtoms = Math.abs(indexes[0]);
    type = nAtoms - 2;
    for (int j = 1; j <= nAtoms; j++) {
      if (steps <= 0 || !bsAtoms.get(indexes[j])) {
        indexes[0] = -nAtoms; // disable
        break;
      }
      minList[j - 1] = atomMap[indexes[j]];
    }
  }
  
 
}
