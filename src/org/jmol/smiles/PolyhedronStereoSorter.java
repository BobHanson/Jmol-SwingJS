package org.jmol.smiles;

import java.util.Comparator;

import javajs.util.P3;
import javajs.util.T3;
import javajs.util.V3;

class PolyhedronStereoSorter implements Comparator<Object[]> {

  private V3 vTemp = new V3();
  private V3 vRef;

  PolyhedronStereoSorter() {
  }
  
  void setRef(V3 vRef) {
    this.vRef = vRef;
  }

  /**
   * Comparison is by torsion angle, as set previously and passed in as Float a[1] and b[1].
   * If these two are within 1 degree of each other, then we compare the dot product of
   * the reference vector and the vector from a to b, from points stored as a[2] and b[2]. 
   */
  @Override
  public int compare(Object[] a, Object[] b) {
    float torA = ((Float) a[1]).floatValue();
    float torB = ((Float) b[1]).floatValue();
    if (Math.abs(torA - torB) < 1f) {
      torA = 0;
      vTemp.sub2((P3) b[2], (P3) a[2]);
      torB = vRef.dot(vTemp);
    }
    return (torA < torB ? 1 : torA > torB ? -1 : 0);
  }

  private V3 align1 = new V3();
  private V3 align2 = new V3();

  private static final float MIN_ALIGNED = (float) (10f/180*Math.PI);

  /**
   * check alignment, within 10 degrees is considered aligned.
   * 
   * @param pt1
   * @param pt2
   * @param pt3
   * @return true if within 10 degrees
   */
  boolean isAligned(T3 pt1, T3 pt2, T3 pt3) {
    align1.sub2(pt1, pt2);
    align2.sub2(pt2, pt3);
    float angle = align1.angle(align2);
    return (angle < MIN_ALIGNED);
  }


}