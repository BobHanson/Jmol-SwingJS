package org.jmol.minimize;

public class MinAngle extends MinObject {
  public int sbType;
  public Integer sbKey;
  public double ka;
  public double theta0 = Double.NaN; // degrees
  
  MinAngle(int[] data) {
    this.data = data; //  ia, ib, ic, iab, ibc
  }  
}
