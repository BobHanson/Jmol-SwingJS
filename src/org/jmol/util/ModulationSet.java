package org.jmol.util;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.JmolModulationSet;
import org.jmol.api.SymmetryInterface;

import javajs.util.Lst;
import javajs.util.M3d;
import javajs.util.M3d;
import javajs.util.Matrix;
import javajs.util.P3;
import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.T3;
import javajs.util.T3d;
import javajs.util.V3;
import javajs.util.V3d;

/**
 * A class to group a set of modulations for an atom as a "vibration" Extends V3
 * so that it will be a displacement, and its value will be an occupancy
 * 
 * @author Bob Hanson hansonr@stolaf.edu 8/9/2013
 * 
 */

public class ModulationSet extends Vibration implements JmolModulationSet {

  /**
   * an identifier for this modulation
   * 
   */
  String id;
  
  /**
   * the unmodulated original position of this atom;
   * note that x,y,z extended from Vibration(V3) is the current displacement modulation itself 
   *
   */
  private P3 r0;

  
  /**
   * the space group appropriate to this atom
   * 
   */
  private SymmetryInterface symmetry;

  /**
   * unit cell axes -- used in Modulation for calculating magnetic modulations
   */
  double[] axesLengths;

  /**
   * the number of operators in this space group -- needed for occupancy calculation
   */
  private int nOps;

  /**
   * the symmetry operation used to generate this atom
   */
  private int iop;

  /**
   * a string description of the atom's symmetry operator
   */
  private String strop;

  /**
   * the spin operation for this atom: +1/0/-1 
   */
  private int spinOp;

  /**
   * the matrix representation for this atom's symmetry operation
   * 
   */
  private Matrix rsvs;

  /**
   * vib is a spin vector when the model has modulation; otherwise an
   * unmodulated vibration.
   * 
   */
  public Vibration vib;
  
  /**
   * the list of all modulations associated with this atom
   */
  private Lst<Modulation> mods;
    
  /**
   * subsystems can deliver their own unique unit cell;
   * they are commensurate
   * 
   */
  private boolean isSubsystem;
  
  @Override
  public SymmetryInterface getSubSystemUnitCell() {
    return (isSubsystem ? symmetry : null);
  }

  /**
   * commensurate modulations cannot be "undone" 
   * and they cannot be turned off
   * 
   */
  private boolean isCommensurate;

  // parameters necessary for calculating occValue
  
  private double fileOcc;
  private double[] occParams;
  private double occSiteMultiplicity;

  /**
   * for Crenels (simple occupational modulation or Legendre displacement modulation, 
   * the value determined here is absolute (0 or 1), not an adjustment;
   * set in calculate() by one of the modulations
   * 
   */
  boolean occAbsolute;

  /**
   * modCalc is used for calculations independent of 
   * what the current setting is
   * 
   */
  private ModulationSet modCalc;

  /**
   * the modulated magnetic spin
   */
  public V3 mxyz;

  /**
   * current value of anisotropic parameter modulation
   *  
   */
  public Map<String, Double> htUij;
  
  /**
   * the current value of the occupancy modulation
   */
  public double vOcc = Double.NaN;
  
  /**
   * final occupancy value -- absolute; in range [0,1]
   */
  private double occValue = Double.NaN;
  
  // values set in setModTQ
  
  private P3 qtOffset = new P3();
  private boolean isQ;

  /**
   * indicates state of modulated or unmodulated
   */
  private boolean enabled;

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * an adjustable scaling factor, as for vibrations
   * 
   */
  private float scale = 1;

  @Override
  public float getScale() {
    return scale;
  }

  // generator parameters 

  private M3d gammaE;
  private Matrix gammaIinv;
  private Matrix sigma;
  private Matrix tau;
  private Matrix rI;
  private Matrix tFactorInv; // for subsystems

  public ModulationSet() {

  }

  /**
   * A collection of modulations for a specific atom.
   * 
   * 
   * @param id
   * @param r00
   *        originating atom position prior to application of symmetry
   * @param r0
   *        unmodulated (average) position after application of symmetry
   * @param d
   * @param mods
   * @param gammaE
   * @param factors
   *        including sigma and tFactor
   * @param symmetry
   * @param iop
   * @param nOps
   * @param v
   *        TODO
   * @param isCommensurate TODO
   * @return this
   * 
   * 
   */

  public ModulationSet setMod(String id, P3d r00, P3d r0, int d,
                              Lst<Modulation> mods, M3d gammaE,
                              Matrix[] factors, 
                              SymmetryInterface symmetry, int nOps, int iop, 
                              Vibration v, boolean isCommensurate) {

    // The superspace operation matrix is (3+d+1)x(3+d+1) rotation/translation matrix
    // that can be blocked as follows:
    // 
    //              rotational part     | translational part
    //          
    //             Gamma_E     0        |  S_E + n
    // {Gamma|S} =                        
    //             Gamma_M   Gamma_I    |  S_I
    //             
    //               0         0        |   1
    // 
    // where Gamma_E is the "external" 3x3 R3 point group operation 
    //       Gamma_I is the "intermal" dxd point group operation
    //       Gamma_M is the dx3 "mixing" component that adds an
    //               external effect to tbe rotation of internal coordinates.
    //       3x1 S_E and 3x1 S_I are the external and internal translations, respectively
    //       n is the R3 part of the lattice translation that is part of this particular operation
    //       
    // Note that all elements of Gamma are 0, 1, or -1 -- "epsilons"
    //
    // Likewise, the 3+d coordinate vector that Gamma|S is being operated upon is a (3+d x 1) column vector
    // that can be considered to be an external ("R3") component and an internal ("d-space") component:
    // 
    //        r_E (3 x 1)
    //   r = 
    //        r_I (d x 1)
    //
    // Thus, we have:
    //
    //   r' = Gamma * r + S + n 
    //
    // with components
    //
    //   r'_E = Gamma_E * r_E + S_E + n    (just as for standard structures)
    //   r'_I = Gamma_M * r_E + Gamma_I * r_I + S_I 
    //
    // These equations are not actually used here.
    //
    // The set of cell wave vectors form the sigma (d x 3) array, one vector per row.
    // Multiplying sigma by the atom vector r'_E and adding a zero-point offset
    // in internal d-space, tuv, gives us r'_I
    //
    //   r'_I = sigma * r'_E + tuv
    //
    // However, this coordinate is not in the "space" that our modulation functions
    // are defined for. In order to apply those functions, we must back-transform this
    // point into the space of the asymmetric unit. We do that inverting our function 
    // 
    //   X'_I = Gamma_M * X_E + Gamma_I * X_I + S_I
    //
    // to give:
    //
    //   X_I = (Gamma_I^-1)(X'_I - Gamma_M * X_E - S_I)
    //
    // The parameters to this Java function r0 and r00 provide values for r'_E and r_E, 
    // respectively. Substituting r'_I for X'_I and r_E for X_E, we get:
    //
    //   r_I = (Gamma_I^-1)(sigma * r'_E + tuv - Gamma_M * r_E - S_I)
    //
    // In the code below, we precalculate all except the zero-point offset as "tau":
    //
    //   tau = gammaIinv.mul(sigma.mul(vR0).sub(gammaM.mul(vR00)).sub(sI));
    //
    // and then, in calculate(), we add in the tuv part and sum all the modulations:
    //
    //   rI = tau.add(gammaIinv.mul(tuv)).toArray();
    //   for (int i = mods.size(); --i >= 0;)
    //     mods.get(i).apply(this, rI);
    // 
    // We can think of tau as an operator leading to a point in the "internal" d-space, 
    // as in a t-plot (van Smaalen, Fig. 2.6, p. 35) but for all internal coordinates together.
    //
    //
    //// Note that Gamma_M is not necessarily all zeros. For example, in 
    //// SSG 67.1.16.12  Acmm(1/2,0,g)0s0 we have an operator
    ////  
    ////  (x1,x2,-x3,x1-x4+1/2) 
    ////  
    //// [http://stokes.byu.edu/iso/ssginfo.php?label=67.1.16.12&notation=x1x2x3x4]
    //// 
    //// Prior to Jmol 14.[2/3].7 10/11/2014 this was not being considered.
    ////
    //// Noting that we have 
    ////
    ////   Gamma_M = sigma * Gamma_E - Gamma_I * sigma
    ////
    //// and
    ////
    ////   X'_I = sigma * X'_E = sigma * (Gamma_E * X_E + S_E)
    ////
    //// we can, with some work, rewrite tau as:
    //// 
    ////   tau = X_I = sigma * X_E + (Gamma_I^-1)(sigma * S_E - S_I)
    //// 
    //// This relationship is used in Jana2006 but not here, because it 
    //// also necessitates adding in the final lattice shift, and that is not
    //// as easy as it sounds. It is easier just to use Gamma_M * X_E.
    ////
    //// Aside: In the case of subsystems, Gamma and S are extracted from:
    //// 
    ////   {Gamma | S} = {Rs_nu | Vs_nu} = W_nu {Rs|Vs} W_nu^-1
    ////
    //// For subsystem nu, we need to use t_nu, which will be
    //// 
    ////   t_nu = (W_dd - sigma W_3d) * tuv   (van Smaalen, p. 101)
    //// 
    ////   t_nu = tFactor * tuv
    //// 
    //// so this becomes
    //// 
    ////   X_I = tau + (Gamma_I^-1)(tFactor^-1 * tuv)
    //// 
    //// Thus we have two subsystem-dependent modulation factors we
    //// need to bring in, sigma and tFactor, and two we need to compute,
    //// GammaIinv and tau.

    this.id = id;// + "_" + symmetry.getSpaceGroupName();
    this.symmetry = symmetry;
    strop = symmetry.getSpaceGroupXyz(iop, false);
    this.iop = iop;
    this.nOps = nOps;
    
    this.r0 = r0.asP3();
    modDim = d;
    rI = new Matrix(null, d, 1);
    this.mods = mods;
    this.gammaE = gammaE; // gammaE_nu, R, the real 3x3 rotation matrix, as M3d
    sigma = factors[0];
    if (factors[1] != null) {
      isSubsystem = true;
      tFactorInv = factors[1].inverse();
    }
    if (v != null) {
      // An atom's modulation will take the place of its vibration, if it
      // has one, so we have to create a field here to hang onto that. 
      // It could be a magnetic moment being modulated, or it may be
      // just a simple vibration that just needs a place to be.
      vib = v;
      vib.modScale = 1;
      mxyz = new V3(); // modulations of spin
      axesLengths = symmetry.getUnitCellParams(); // required for calculating mxyz        
    }
    Matrix vR00 = Matrix.newT(r00.asP3(), true);
    Matrix vR0 = Matrix.newT(r0.asP3(), true);

    rsvs = symmetry.getOperationRsVs(iop);
    gammaIinv = rsvs.getSubmatrix(3, 3, d, d).inverse();
    Matrix gammaM = rsvs.getSubmatrix(3, 0, d, 3);
    Matrix sI = rsvs.getSubmatrix(3, 3 + d, d, 1);
    spinOp = symmetry.getSpinOp(iop);
    tau = gammaIinv.mul(sigma.mul(vR0).sub(gammaM.mul(vR00)).sub(sI));

    if (Logger.debuggingHigh)
      Logger.debug("MODSET create " + id + " r0=" + Escape.ePd(r0) + " tau="
          + tau);

    return this;
  }

  /**
   * Calculate  r_I internal d-space coordinate of an atom.
   *  
   * @param tuv
   * @param isQ
   * @return this ModulationSet, with this.rI set to the coordinate
   */

  public synchronized ModulationSet calculate(T3 tuv, boolean isQ) {
    // initialize modulation components
    x = y = z = 0;
    htUij = null;
    vOcc = Float.NaN;
    if (mxyz != null)
      mxyz.set(0, 0, 0);
    double[][] a;
    if (isQ && qtOffset != null) {
      // basically moving whole unit cells here
      // applied to all cell wave vectors
      Matrix q = new Matrix(null, 3, 1);
      a = q.getArray();
      a[0][0] = qtOffset.x;
      a[1][0] = qtOffset.y;
      a[2][0] = qtOffset.z;
      a = (rI = sigma.mul(q)).getArray();
    } else {
      // initialize to 0 0 0
      a = rI.getArray();
      for (int i = 0; i < modDim; i++)
        a[i][0] = 0;
    }
    if (tuv != null) {
      // add in specified x4,x5,x6 offset:
      switch (modDim) {
      default:
        a[2][0] += tuv.z;
        //$FALL-THROUGH$
      case 2:
        a[1][0] += tuv.y;
        //$FALL-THROUGH$
      case 1:
        a[0][0] += tuv.x;
        break;
      }
    }
    if (isSubsystem) {
      // apply subsystem scaling adjustment
      rI = tFactorInv.mul(rI);
    }
    // add in precalculated part
    rI = tau.add(gammaIinv.mul(rI));
    // modulate
    double[][] arI = rI.getArray();
    for (int i = mods.size(); --i >= 0;)
      mods.get(i).apply(this, arI);
    // rotate by R3 rotation
    gammaE.rotate(this);
    if (mxyz != null){
      gammaE.rotate(mxyz);
      if (spinOp < 0)
        mxyz.scale(spinOp);
    }
    return this;
  }

  public void addUTens(String utens, double v) {
    if (htUij == null)
      htUij = new Hashtable<String, Double>();
    Double f = htUij.get(utens);
    if (Logger.debuggingHigh)
      Logger.debug("MODSET " + id + " utens=" + utens + " f=" + f + " v=" + v);
    if (f != null)
      v += f.doubleValue();
    htUij.put(utens, Double.valueOf(v));
  }

  /**
   * Set modulation "t" value, which sets which unit cell in sequence we are
   * looking at; d=1 only.
   * 
   * @param isOn
   * @param qtOffset
   * @param isQ
   * @param scale
   * 
   */
  @Override
  public synchronized void setModTQ(T3 a, boolean isOn, T3 qtOffset,
                                    boolean isQ, float scale) {
    if (enabled)
      addTo(a, Float.NaN);
    enabled = false;
    this.scale = scale;
    if (qtOffset != null) {
      this.qtOffset.setT(qtOffset);
      this.isQ = isQ;
      if (isQ)
        qtOffset = null;
      calculate(qtOffset, isQ);
      if (!Double.isNaN(vOcc))
        occValue = getOccupancy();
    }
    if (isOn) {
      addTo(a, 1);
      enabled = true;
    }
  }

  @Override
  public void addTo(T3 a, float scale) {
    boolean isReset = (Float.isNaN(scale));
    if (isReset)
      scale = -1;
    ptTemp.setT(this);
    ptTemp.scale(this.scale * scale);
    if (a != null) {
      //if (!isReset)
      //System.out.println(a + " ms " + ptTemp);
      symmetry.toCartesianF(ptTemp, true);
      a.add(ptTemp);
    }
    // magnetic moment part
    if (mxyz != null)
      setVib(isReset, scale);
  }

  private void setVib(boolean isReset, float modulationScale) {
    // vib.modScale will be dynamically set
    // it can be turned off using VECTOR MAX 0
    // modulationScale will be -1 on a reset. 
    // this.scale will then hold the actual scale
    // otherwise, modulation scale will be the actual scale,
    // and this.scale will be 1. 
    if (isReset) {
      vib.setT(v0);
      return;
    }
    ptTemp.setT(mxyz);
    // vib.modScale is from VECTOR MAX <n> 
    symmetry.toCartesianF(ptTemp, true);
    PT.fixPtFloats(ptTemp, PT.CARTESIAN_PRECISION);
    // we must scale v0 with mod to preserve angle
    ptTemp.add(v0);
    ptTemp.scale(vib.modScale * modulationScale * this.scale);
    vib.setT(ptTemp);
  }

  @Override
  public String getState() {
    String s = "";
    if (qtOffset != null && qtOffset.length() > 0)
      s += "; modulation " + Escape.eP(qtOffset) + " " + isQ + ";\n";
    s += "modulation {selected} " + (enabled ? "ON" : "OFF");
    return s;
  }

  @Override
  public T3 getModPoint(boolean asEnabled) {
    return (asEnabled ? this : r0);
  }

  @Override
  public Object getModulation(char type, T3 tuv) {
    getModCalc();
    switch (type) {
    case 'D':
      // return r0 if t456 is null, otherwise calculate dx,dy,dz for a given t4,5,6
      return P3.newP(tuv == null ? r0 : modCalc.calculate(tuv, false));
    case 'M':
      // return v0 if t456 is null, otherwise calculate vx,vy,vz for a given t4,5,6
      return P3.newP(tuv == null ? v0 : modCalc.calculate(tuv, false).mxyz);
    case 'T':
      // do a calculation and return the t-value for the first three dimensions of modulation
      modCalc.calculate(tuv, false);
      double[][] ta = modCalc.rI.getArray();
      return P3.new3((float) ta[0][0], (modDim > 1 ? (float) ta[1][0] : 0),
          (modDim > 2 ? (float) ta[2][0] : 0));
    case 'O':
      // return the currently modulated or calculated occupation on [0,100]
      return Float.valueOf(Math.abs(tuv == null ? getOccupancy100(false)
          : modCalc.calculate(tuv, false).getOccupancy100(false)));
    }
    return null;
  }

  P3 ptTemp = new P3();
  private V3 v0;

  /**
   * get updated value for offset vector and for occupancy
   */
  @Override
  public T3 setCalcPoint(T3 pt, T3 t456, float vibScale, float scale) {
    if (enabled) {
      addTo(pt, Float.NaN);
      getModCalc().calculate(t456, false).addTo(pt, scale);
      //System.out.println("MS setTempPoint " + id + " v=" + vOcc);
      // note: this does not include setting occValue
    }
    return pt;
  }

  private ModulationSet getModCalc() {
    if (modCalc == null) {
      modCalc = new ModulationSet();
      modCalc.axesLengths = axesLengths;
      modCalc.enabled = true;
      modCalc.fileOcc = fileOcc;
      modCalc.gammaE = gammaE;
      modCalc.gammaIinv = gammaIinv;
      modCalc.id = id;
      modCalc.modDim = modDim;
      modCalc.mods = mods;
      modCalc.nOps = nOps;
      modCalc.occParams = occParams;
      modCalc.occSiteMultiplicity = occSiteMultiplicity;
      modCalc.r0 = r0;
      modCalc.rI = rI;
      modCalc.sigma = sigma;
      modCalc.spinOp = spinOp;
      modCalc.symmetry = symmetry;
      modCalc.tau = tau;
      modCalc.v0 = v0;
      modCalc.vib = vib;
      if (mxyz != null)
        modCalc.mxyz = new V3();
    }
    return modCalc;
  }

  @Override
  public void getInfo(Map<String, Object> info) {
    Hashtable<String, Object> modInfo = new Hashtable<String, Object>();
    modInfo.put("id", id);
    modInfo.put("r0", r0);
    modInfo.put("tau", tau.getArray());
    modInfo.put("modDim", Integer.valueOf(modDim));
    modInfo.put("rsvs", rsvs);
    modInfo.put("sigma", sigma.getArray());
    modInfo.put("symop", Integer.valueOf(iop + 1));
    modInfo.put("strop", strop);
    modInfo.put("unitcell", symmetry.getUnitCellInfo(true));

    Lst<Hashtable<String, Object>> mInfo = new Lst<Hashtable<String, Object>>();
    for (int i = 0; i < mods.size(); i++)
      mInfo.addLast(mods.get(i).getInfo());
    modInfo.put("mods", mInfo);
    info.put("modulation", modInfo);
  }

  @Override
  public void setXYZ(T3 v) {
    // we do not allow setting of the modulation vector,
    // but if there is an associated magnetic spin "vibration"
    // or an associated simple vibration,
    // then we allow setting of that.
    // but this is temporary, since really we set these from v0.
    if (vib == null)
      return;
    if (vib.modDim == Vibration.TYPE_SPIN) {
      if (v.x == PT.FLOAT_MIN_SAFE && v.y == PT.FLOAT_MIN_SAFE) {
        // written by StateCreator -- for modulated magnetic moments
        // 957 Fe Fe_1_#957 1.4E-45 1.4E-45 0.3734652 ;
        vib.modScale = v.z;
        return;
      }
    }
    vib.setT(v);
  }

  @Override
  public Vibration getVibration(boolean forceNew) {
    // ModulationSets can be place holders for standard vibrations
    if (vib == null && forceNew)
      vib = new Vibration();
    return vib;
  }

  @Override
  public V3 getV3() {
    return (mxyz == null ? this : mxyz);
  }

  @Override
  public void scaleVibration(float m) {
    if (vib == null)
      return;
    if (m == 0) {
      // this is a reset
      m = 1/vib.modScale;
    }
    vib.scale(m);
    vib.modScale *= m;
  }

  @Override
  public void setMoment() {
    if (mxyz == null)
      return;
    symmetry.toCartesianF(vib, true);
    v0 = V3.newV(vib);
  }

  @Override
  public boolean isNonzero() {
    return x != 0 || y != 0 || z != 0 || mxyz != null
        && (mxyz.x != 0 || mxyz.y != 0 || mxyz.z != 0);
  }

  /**
   * 
   * get the occupancy, first from the reader, then from renderer
   * 
   * @param pt
   * @param foccupancy
   * @param siteMult or 0 is this is not relevant
   * 
   * @return occupancy on [0,1]
   */
  public float setOccupancy(double[] pt, double foccupancy,
                            double siteMult) {
    occParams = pt;
    fileOcc = foccupancy;
    occSiteMultiplicity = siteMult;
    return (float) getOccupancy();
  }
  
  @Override
  public int getOccupancy100(boolean isTemp) {
    if (isCommensurate || Double.isNaN(vOcc))
      return Integer.MIN_VALUE;
    if (!isTemp && !enabled)
      return (int) (-fileOcc * 100);
    if (isTemp && modCalc != null) {
      modCalc.getOccupancy();
      return modCalc.getOccupancy100(false);
    }
    return (int) (occValue * 100);
  }

  private double getOccupancy() {
    double occ;
    if (occAbsolute) {
      // Crenel
      occ = vOcc;
    } else if (occParams == null) {
      // cif Fourier
      // _atom_site_occupancy + SUM
      occ = fileOcc + vOcc;
    } else if (occSiteMultiplicity > 0) {
      // cif with m40 Fourier
      // occ_site * (occ_0 + SUM)
      double o_site = fileOcc * occSiteMultiplicity / nOps / occParams[1];
      occ = o_site * (occParams[1] + vOcc);
    } else {
      // m40 Fourier
      // occ_site * (occ_0 + SUM)
      occ = occParams[0] * (occParams[1] + vOcc);
    }
    // 49/50 is an important range for cutoffs -- we never return a value in this range
    occ = (occ > 0.49 && occ < 0.50 ? 0.489 : Math.min(1, Math.max(0, occ)));
    return occValue = occ;
  }

}
