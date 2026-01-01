package org.jmol.util;

import java.util.Map;

import org.jmol.modelset.Atom;

import javajs.util.M3d;
import javajs.util.P3d;
import javajs.util.T3d;
import javajs.util.V3d;

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

public class Vibration extends V3d {

  protected final static double twoPI = 2 * Math.PI;

  public static final int TYPE_VIBRATION = -1;
  public static final int TYPE_SPIN = -2;
  public static final int TYPE_WYCKOFF = -3;
  /**
   * modDim will be > 0 for modulation
   */
  public int modDim = TYPE_VIBRATION;
  public double modScale = Double.NaN; // modulation only
  public double magMoment;
  public boolean showTrace;
  public boolean isFractional;
  public V3d v0;
  public int tracePt;
  private P3d[] trace = null;
  public String symmform;
  
  public boolean isFrom000;
  

  public Vibration() {
    super();
  }

  /**
   * @param pt 
   * @param t456 
   * @param scale 
   * @param modulationScale 
   * @return pt
   */
  public T3d setCalcPoint(T3d pt, T3d t456, double scale, double modulationScale) {
    switch (modDim) {
//    case TYPE_DISPLACEMENT:
//      break;
    case TYPE_SPIN:
    case TYPE_WYCKOFF:
      break;
    default:
      pt.scaleAdd2((double) (Math.cos(t456.x * twoPI) * scale), this, pt); 
      break;
    }
    return pt;
  }

  public void getInfo(Map<String, Object> info) {
    info.put("vibVector", V3d.newV(this));
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
    v.magMoment = magMoment;
    v.isFrom000 = isFrom000;
    v.v0 = v0;
    return v;
  }

  public void setXYZ(T3d vib) {
    setT(vib);
  }

  public void setV0() {
    v0 = V3d.newV(this);
  }

  public Vibration setType(int type) {
    this.modDim = type;
    return this;
  }

  public boolean isNonzero() {
    return x != 0 || y != 0 || z != 0;
  }

  /**
   * @param isTemp used only in ModulationSet when calculating actual display offset
   * @return Integer.MIN_VALUE if not applicable, occupancy if enabled, -occupancy if not enabled
   */
  public int getOccupancy100(boolean isTemp) {
    return Integer.MIN_VALUE;
  }

  public void startTrace(int n) {
    trace = new P3d[n];
    tracePt = n;
  }
  
  public P3d[] addTracePt(int n, Point3fi ptNew) {
    if (trace == null || n == 0 || n != trace.length)
      startTrace(n);
    if (ptNew != null && n > 2) {
      if (--tracePt <= 0) {
        P3d p0 = trace[trace.length - 1];
        for (int i = trace.length; --i >= 1;)
          trace[i] = trace[i-1];
        trace[1] = p0;
        tracePt = 1;
      }
      P3d p = trace[tracePt];
      if (p == null)
        p = trace[tracePt] = new P3d();
      p.setT(ptNew);
    }      
    return trace;
  }

  public String getApproxString100() {
    return Math.round(x * 100) + "," + Math.round(y * 100) + "," + Math.round(z * 100);
  }

  public void rotateSpin(M3d matInv, M3d rot, M3d dRot, Atom a) {
    rot(matInv, rot, dRot, this);
    if (isFrom000) {
      rot(matInv, rot, dRot, a);
    }

  }

  private static void rot(M3d matInv, M3d rot, M3d dRot, T3d t) {
    if (matInv == null) {
      dRot.rotate(t);
    } else {
      matInv.rotate(t);
      rot.rotate(t);
    }
  }

}
