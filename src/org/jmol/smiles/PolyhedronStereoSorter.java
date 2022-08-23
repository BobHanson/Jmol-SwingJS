package org.jmol.smiles;

import java.util.Comparator;

import javajs.util.P3d;
import javajs.util.T3d;
import javajs.util.V3d;

class PolyhedronStereoSorter implements Comparator<Object[]> {

  private V3d vTemp = new V3d();
  private V3d vRef;

  PolyhedronStereoSorter() {
  }
  
  void setRef(V3d vRef) {
    this.vRef = vRef;
  }

  /**
   * Comparison is by torsion angle, as set previously and passed in as Float a[1] and b[1].
   * If these two are within 1 degree of each other, then we compare the dot product of
   * the reference vector and the vector from a to b, from points stored as a[2] and b[2]. 
   */
  @Override
  public int compare(Object[] a, Object[] b) {
    double torA = ((Number) a[1]).doubleValue();
    double torB = ((Number) b[1]).doubleValue();
      if (Math.abs(torA - torB) < 1d) {
      torA = 0;
      vTemp.sub2((P3d) b[2], (P3d) a[2]);
      torB = vRef.dot(vTemp);
    }
    return (torA < torB ? -1 : torA > torB ? 1 : 0);
  }

  private V3d align1 = new V3d();
  private V3d align2 = new V3d();

  private static final double MIN_ALIGNED = (10d/180*Math.PI);

  /**
   * check alignment, within 10 degrees is considered aligned.
   * 
   * @param pt1
   * @param pt2
   * @param pt3
   * @return true if within 10 degrees
   */
  boolean isAligned(T3d pt1, T3d pt2, T3d pt3) {
    align1.sub2(pt1, pt2);
    align2.sub2(pt2, pt3);
    double angle = align1.angle(align2);
    return (angle < MIN_ALIGNED);
  }


}