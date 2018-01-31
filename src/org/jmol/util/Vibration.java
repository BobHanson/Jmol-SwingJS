package org.jmol.util;

import java.util.Map;

import javajs.util.P3;
import javajs.util.T3;
import javajs.util.V3;

/**
 * A class to allow for more complex vibrations and associated 
 * phenomena, such as modulated crystals. In the case of modulations,
 * ModulationSet extends Vibration and is implemented that way, 
 * and, as well, magnetic spin is also a form of Vibration that 
 * may have an associated ModulationSet, as indicated here
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */

public class Vibration extends V3 {

  protected final static double twoPI = 2 * Math.PI;

  public static final int TYPE_VIBRATION = -1;
  public static final int TYPE_SPIN = -2;
 // public static final int TYPE_DISPLACEMENT = -3; // not used

  /**
   * modDim will be > 0 for modulation
   */
  public int modDim = TYPE_VIBRATION;
  public float modScale = Float.NaN; // modulation only

  /**
   * @param pt 
   * @param t456 
   * @param scale 
   * @param modulationScale 
   * @return pt
   */
  public T3 setCalcPoint(T3 pt, T3 t456, float scale, float modulationScale) {
    switch (modDim) {
//    case TYPE_DISPLACEMENT:
//      break;
    case TYPE_SPIN:
      break;
    default:
      pt.scaleAdd2((float) (Math.cos(t456.x * twoPI) * scale), this, pt);    
      break;
    }
    return pt;
  }

  public void getInfo(Map<String, Object> info) {
    info.put("vibVector", V3.newV(this));
    info.put("vibType", (
      //  modDim == TYPE_DISPLACEMENT ? "displacement" 
      modDim == TYPE_SPIN ? "spin" 
      : modDim == TYPE_VIBRATION ? "vib" 
      : "mod"));
  }

  @Override
  public Object clone() {
    Vibration v = new Vibration();
    v.setT(this);
    v.modDim = modDim;
    return v;
  }

  public void setXYZ(T3 vib) {
    setT(vib);
  }

  public Vibration setType(int type) {
    this.modDim = type;
    return this;
  }

  public boolean isNonzero() {
    return x != 0 || y != 0 || z != 0;
  }

  /**
   * @param isTemp used only in ModulationSet
   * @return Integer.MIN_VALUE if not applicable, occupancy if enabled, -occupancy if not enabled
   */
  public int getOccupancy100(boolean isTemp) {
    return Integer.MIN_VALUE;
  }

  public boolean showTrace;
  private P3[] trace = null;
  public int tracePt;
  
  public void startTrace(int n) {
    trace = new P3[n];
    tracePt = n;
  }
  
  public P3[] addTracePt(int n, Point3fi ptNew) {
    if (trace == null || n == 0 || n != trace.length)
      startTrace(n);
    if (ptNew != null && n > 2) {
      if (--tracePt <= 0) {
        P3 p0 = trace[trace.length - 1];
        for (int i = trace.length; --i >= 1;)
          trace[i] = trace[i-1];
        trace[1] = p0;
        tracePt = 1;
      }
      P3 p = trace[tracePt];
      if (p == null)
        p = trace[tracePt] = new P3();
      p.setT(ptNew);
    }      
    return trace;
  }

}
