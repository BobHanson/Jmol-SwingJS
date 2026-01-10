package org.jmol.util;

import java.util.Map;

import org.jmol.modelset.Atom;
import org.jmol.modelset.Model;
import org.jmol.modelset.ModelSet;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.util.A4d;
import javajs.util.BS;
import javajs.util.M3d;
import javajs.util.P3d;
import javajs.util.Qd;
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
      pt.scaleAdd2((Math.cos(t456.x * twoPI) * scale), this, pt); 
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

  private final M3d matTemp = new M3d(), matInv = new M3d();

  public void rotateModelSpinVectors(ModelSet ms, int modelIndex, M3d rot, boolean isdx) {
    if (modelIndex < 0 || modelIndex >= ms.mc || ms.vibrations == null)
      return;
    Viewer vwr = ms.vwr;
    Model m = ms.am[modelIndex];
    if (m.isJmolDataFrame)
      modelIndex = m.dataSourceFrame;
    Map<String, Object> info = ms.getModelAuxiliaryInfo(modelIndex);
    if (rot == null) {
      // reset
      rot = (M3d) info.get(JC.SPIN_FRAME_ROTATION_MATRIX);
      if (rot == null)
        return;
    }
    boolean noref = Double.isNaN(rot.getElement(0, 0));
    boolean isScreenZ = (noref && rot.getElement(2, 2) == 1);
    double deg = (noref || isScreenZ ? rot.getElement(1, 1) : 0);
    if (noref && deg != 0) {
      rot.setElement(1, 1, 0);
      rotateModelSpinVectors(ms, modelIndex, rot, false);
      rot.setElement(1, 1, deg);
    }

    M3d m0 = (M3d) info.get(JC.SPIN_FRAME_ROTATION_MATRIX);
    M3d mat = (M3d) info.get(JC.SPIN_ROTATION_MATRIX_APPLIED);
    if (mat == null && m0 == null) {
      m0 = M3d.newM3(null);
      info.put(JC.SPIN_FRAME_ROTATION_MATRIX, m0);
    }
    if (mat == null) {
      mat = m0;
    }
    if (noref) {
      V3d qn;
      if (isScreenZ) {
        P3d pt3 = P3d.new3(0, 0, 100);
        P3d pt4 = P3d.new3(0, 0, 200);
        vwr.tm.unTransformPoint(pt3, pt3);
        vwr.tm.unTransformPoint(pt4, pt4);
        qn = V3d.newVsub(pt3, pt4);
      } else {
        qn = Qd.newM(mat).getNormal();
      }
      rot = Qd.newVA(qn, deg).getMatrix();
    }
    BS bs = BSUtil.newAndSetBit(modelIndex);
    ms.includeAllRelatedFrames(bs, true);
    BS bsModels = BSUtil.copy(bs);
    bs = vwr.getModelUndeletedAtomsBitSetBs(bs);
    M3d matInv;
    M3d drot;
    drot = matTemp;
    if (isdx) {
      drot.setM3(rot);
      rot.mul2(rot, mat);
      matInv = null;
    } else {
      matInv = this.matInv;
      matInv.setM3(mat);
      matInv.invert();
    }
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      if (i >= ms.vibrations.length)
        return;
      Vibration v = ms.vibrations[i];
      if (v == null || !v.isFrom000 && v.magMoment == 0)
        continue;
      v.rotateSpin(matInv, rot, drot, ms.at[i]);
    }
    Qd quat = Qd.newM(rot);
    A4d aa = quat.toA4d();
    mat = M3d.newM3(rot);
    info.put(JC.SPIN_ROTATION_MATRIX_APPLIED, mat);
    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1)) {
    	m = ms.am[i];
    	if (m.uvw0 != null) {
    	    mat.rotate2(m.uvw0[0], m.uvw[0]);
    	    mat.rotate2(m.uvw0[1], m.uvw[1]);
    	    mat.rotate2(m.uvw0[2], m.uvw[2]);
    	}
    }
    info.put(JC.SPIN_ROTATION_AXIS_ANGLE_APPLIED, aa);
    if (ms.unitCells[modelIndex] != null)
      ms.unitCells[modelIndex].setSpinAxisAngle(aa);
  }

}
