package org.jmol.adapter.readers.cif;

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.Matrix;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.T3;
//import javajs.util.SB;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollection;
import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.MSInterface;
import org.jmol.api.SymmetryInterface;
import javajs.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.BoxInfo;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Modulation;
import org.jmol.util.ModulationSet;
import org.jmol.util.Tensor;
import org.jmol.util.Vibration;

/**
 * generalized modulated structure reader class for CIF and Jana
 * 
 * -- includes Fourier, Crenel, Sawtooth; displacement, occupancy, and Uij
 * 
 * -- handles up to 6 modulation wave vectors
 * 
 * -- commensurate and incommensurate, including composites
 * 
 * -- not handling _cell_commen_t_section_1
 * 
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */


//  Basic strategy:
//  
//  1) CIF or Jana reader compiles all data in the form 
//  of a Hashtable<String,double[]> with keys that identify 
//  model, atom, and type of modulation. This is done so that
//  the data can be collected in any order. Hashtable keys are of the form:
//  
//    type_id#axis;atomLabel@model
//  
//  where 
//  
//  type = W|F|D|J|M|O|U (wave vector, Fourier index, displacement, magnetic moment, occupancy, anisotropy);
//  
//  id = n|S|T|"_coefs_" 
//
//  where 
//
//    n=0 is Crenel, 
//    n>0 is a specific Fourier or cell wave index, (W_1, F_1, F_2, etc.)
//    S indicates displacement sawtooth (D_S)
//    T indicates magnetic moment sawtooth (M_T)
//    "_coefs" used only in F_1_coefs_ indicates coefficients of W_i for a Fourier vector 
//  
//  axis (optional) = 0|x|y|z|Uij 
//
//  where 0 indicates irrelevant, Uij indicates a specific anisotropic parameter U11, U12, etc.
//  
//  atomLabel is only for D, M, and O
//
//  model is the model number, starting with 0.
//
//  double[] data are typically two or three parameters, such as
//  [cos,sin], [width,center], [sigma1,sigma2,sigma3], etc.
// 
//  for example, the following abbreviated CIF code generates
//  the accompaning modulation data:
//
//
//
//  loop_
//  _cell_wave_vector_seq_id
//  _cell_wave_vector_x
//  _cell_wave_vector_y
//  _cell_wave_vector_z
//  1 0.293(2) 0 0.915(9)
//  
//W_1@0 [0.2930000126361847,0.0,0.9150000214576721]
//  
//  
//  loop_
//  _atom_site_Fourier_wave_vector_seq_id
//  _atom_site_Fourier_wave_vector_x
//  _atom_site_Fourier_wave_vector_y
//  _atom_site_Fourier_wave_vector_z
//  1 0.293(2) 0 0.915(9)
//  2 0.586 0 1.83
//  
//F_1@0 [0.2930000126361847,0.0,0.9150000214576721]
//F_2@0 [0.5860000252723694,0.0,1.8299999237060547]
//  
//  loop_
//  _atom_site_displace_Fourier_atom_site_label
//  _atom_site_displace_Fourier_axis
//  _atom_site_displace_Fourier_wave_vector_seq_id
//  _atom_site_displace_Fourier_param_phase
//  _atom_site_displace_Fourier_param_modulus
//  _atom_site_displace_Fourier_param_sin
//  _atom_site_displace_Fourier_param_cos
//  Bi x 1 ? ? 0.0062(4) -0.002(3)
//  Bi x 2 ? ? 0 0
//  Sr x 1 ? ? -0.0025(2) 0.0043(2)
//  Sr x 2 ? ? ? ?
//  Bi y 1 ? ? -0.001(4) 0.0004(3)
//  Bi y 2 ? ? 0 -0.0052(5)
//  Sr y 1 ? ? 0 0
//  Sr y 2 ? ? ? ?
//  Bi z 1 ? ? 0.00018(2) ?
//  Bi z 2 ? ? 0 0
//  Sr z 1 ? ? 0.00132(4) 0.0073(4)
//  Sr z 2 ? ? ? ?

//D_1#x;Bi@0 [0.006199999712407589,-0.0020000000949949026,0.0]
//D_1#x;Sr@0 [-0.0024999999441206455,0.004299999680370092,0.0]
//D_1#y;Bi@0 [-0.0010000000474974513,4.000000189989805E-4,0.0]
//D_2#y;Bi@0 [0.0,-0.005199999548494816,0.0]
//D_1#z;Sr@0 [0.0013199999229982495,0.007299999240785837,0.0]

// (note that Bi z 1 ? ? 0.00018(2) ? was ignored because the
//  cos term is undefined)

//  loop_
//  _atom_site_occ_Fourier_atom_site_label
//  _atom_site_occ_Fourier_wave_vector_seq_id
//  _atom_site_occ_Fourier_param_phase
//  _atom_site_occ_Fourier_param_modulus
//  _atom_site_occ_Fourier_param_sin
//  _atom_site_occ_Fourier_param_cos
//  Bi 1 ? ? -0.082(2) -0.208(2)
//  Bi 2 ? ? -0.098(3) -0.116(3)

//O_1#0;Bi@0 [-0.0820000022649765,-0.20800000429153442,0.0]
//O_2#0;Bi@0 [-0.09799999743700027,-0.11600000411272049,0.0]

//  loop_
//  _atom_site_U_Fourier_atom_site_label
//  _atom_site_U_Fourier_tens_elem
//  _atom_site_U_Fourier_wave_vector_seq_id
//  _atom_site_U_Fourier_param_phase
//  _atom_site_U_Fourier_param_modulus
//  _atom_site_U_Fourier_param_sin
//  _atom_site_U_Fourier_param_cos
//  Bi U33 1 ? ? 0.001(3) 0.0004(3)
//  Bi U33 2 ? ? ? ?
//  Bi U11 1 ? ? 0.0027(3) 0.0069(4)
//  Bi U11 2 ? ? ? ?
//  Bi U12 1 ? ? -0.0028(3) 0
//  Bi U12 2 ? ? ? ?
//  Bi U13 1 ? ? 0.0006(2) -0.0005(3)
//  Bi U13 2 ? ? ? ?
//  Bi U22 1 ? ? 0.0027(3) 0.0057(4)
//  Bi U22 2 ? ? ? ?
//  Bi U23 1 ? ? 0.0013(4) 0.0047(9)
//  Bi U23 2 ? ? ? ?

//U_1#U33;Bi@0 [0.0010000000474974513,4.000000189989805E-4,0.0]
//U_1#U11;Bi@0 [0.002699999837204814,0.006899999920278788,0.0]
//U_1#U12;Bi@0 [-0.00279999990016222,0.0,0.0]
//U_1#U13;Bi@0 [6.000000284984708E-4,-5.000000237487257E-4,0.0]
//U_1#U22;Bi@0 [0.002699999837204814,0.005699999630451202,0.0]
//U_1#U23;Bi@0 [0.001299999887123704,0.004699999932199717,0.0]

//
// 2) The modulations are bundled into ModulationSets, one per atom.
//
// 3) ModulationSets are calculated for each atom. 

public class MSRdr implements MSInterface {

  protected AtomSetCollectionReader cr; // Cif or Jana

  protected int modDim;
  protected String modAxes;
  protected boolean modAverage;
  protected boolean isCommensurate;
  protected int commensurateSection1; // TODO

  private boolean modPack;
  private boolean modVib;
  private String modType;
  private String modCell;
  private boolean modDebug;
  private int modSelected = -1;
  private boolean modLast;

  private Matrix sigma;

  Matrix getSigma() {
    return sigma;
  }

  //private double[] q1;
  //private P3 q1Norm;
  private Map<String, double[]> htModulation;
  private Map<String, Lst<Modulation>> htAtomMods;

  private int iopLast = -1;
  private M3 gammaE; // standard operator rotation matrix
  private int nOps;
  private boolean haveOccupancy;
  private Atom[] atoms;
  private int ac;
  private boolean haveAtomMods;

  private boolean modCoord;

  private boolean finalized;

  private SymmetryInterface symmetry, supercellSymmetry;

  private Lst<String> legendres;

  public MSRdr() {
    // for reflection from Jana
  }

  @Override
  public int initialize(AtomSetCollectionReader r, int modDim) throws Exception {
    cr = r;
    modCoord = r.checkFilterKey("MODCOORD");
    modDebug = r.checkFilterKey("MODDEBUG");
    modPack = !r.checkFilterKey("MODNOPACK");
    modLast = r.checkFilterKey("MODLAST"); // select last symmetry, not first, for special positions  
    modAxes = r.getFilter("MODAXES="); // xyz
    modType = r.getFilter("MODTYPE="); //ODU
    modCell = r.getFilter("MODCELL="); // subsystem for cell
    modSelected = r.parseIntStr("" + r.getFilter("MOD="));
    modVib = r.checkFilterKey("MODVIB"); // then use MODULATION ON  to see modulation
    modAverage = r.checkFilterKey("MODAVE");
    String smodTUV = r.getFilter("MODT=");
    if (smodTUV != null || (smodTUV = r.getFilter("MODTUV=")) != null) {
      modTUV = new P3();
      String[] tuv = (PT.replaceAllCharacters(smodTUV,"{}()","") + ",0,0,0").split(",");
      modTUV.x = PT.parseFloatFraction(tuv[0]);
      modTUV.y = PT.parseFloatFraction(tuv[1]);
      modTUV.z = PT.parseFloatFraction(tuv[2]);
      if (Float.isNaN(modTUV.lengthSquared())) {
        Logger.error("MSRdr cannot read modTUV=" + smodTUV);
        modTUV = null;
      }
    }
    setModDim(modDim);
    return modDim;
  }

  private void setSubsystemOptions() {
    cr.forceSymmetry(modPack);
    if (modCell != null)
      cr.addJmolScript("unitcell {%" + modCell + "}");
  }

  protected void setModDim(int ndim) {
    htModulation = new Hashtable<String, double[]>();
    modDim = ndim;
    cr.appendLoadNote("Modulation dimension = " + modDim);
  }

  /**
   * Types include O (occupation) D (displacement) U (anisotropy) M (magnetic moment)
   * 
   *  _coefs_ indicates this is a wave description
   * 
   * 
   * @param map
   * @param id
   * @param pt
   * @param iModel
   */
  @Override
  public void addModulation(Map<String, double[]> map, String id, double[] pt,
                            int iModel) {
    char ch = id.charAt(0);
    switch (ch) {
    case 'O':
    case 'D':
    case 'M':
    case 'U':
      if (modType != null && modType.indexOf(ch) < 0 || modSelected > 0
          && modSelected != 1)
        return;
      break;
    }
    boolean isOK = false;
    for (int i = pt.length; --i >= 0;) {
      if (modSelected > 0 && i + 1 != modSelected && id.contains("_coefs_")) {
        pt[i] = 0;
      } else if (pt[i] != 0) {
        isOK = true;
        break;
      }
    }
    if (!isOK)
      return;
    if (map == null)
      map = htModulation;
    if (id.indexOf("@") < 0)
      id += "@" + (iModel >= 0 ? iModel : cr.asc.iSet);
    if (id.startsWith("D_L#") || id.startsWith("U_L")) {
      if (legendres == null)
        legendres = new Lst<String>();
      legendres.addLast(id);
    }
    Logger.info("Adding " + id + " " + Escape.e(pt));
    map.put(id, pt);
  }

  /**
   * Both the Jana reader and the CIF reader will call this to set the
   * modulation for a given model.
   * @throws Exception 
   * 
   */
  @Override
  public void setModulation(boolean isPost, SymmetryInterface symmetry) throws Exception {
    if (modDim == 0 || htModulation == null)
      return;
    if (modDebug)
      Logger.debugging = Logger.debuggingHigh = true;
    cr.asc.setInfo("someModelsAreModulated", Boolean.TRUE);
    this.symmetry = symmetry;
    setModulationForStructure(cr.asc.iSet, isPost);
    if (modDebug)
      Logger.debugging = Logger.debuggingHigh = false;
  }

  /**
   * Create a script that will run to turn modulation on and to display only
   * atoms with modulated occupancy >= 0.5.
   * 
   */
  @Override
  public void finalizeModulation() {
    if (!finalized && modDim > 0 && !modVib) {
      // handled by ModelLoader
      if (modTUV != null)
        cr.appendLoadNote("modTUV=" + modTUV);
      cr.asc.setInfo("modulationOn", modTUV == null ? Boolean.TRUE : modTUV);
      cr.addJmolScript("set modulateOccupancy " + (haveOccupancy && !isCommensurate ? true: false));
    }
    finalized = true;
  }

  private String atModel = "@0";

  private Matrix[] modMatrices;

  private double[] qlist100;

  /**
   * Filter keys only for this model.
   * 
   * @param key
   * @param checkQ
   * @return trimmed key without model part or null
   * 
   */
  private String checkKey(String key, boolean checkQ) {
    int pt = key.indexOf(atModel);
    return (pt < 0 || key.indexOf("_pos#") >= 0 || key.indexOf("*;*") >= 0 || checkQ
        && key.indexOf("?") >= 0 ? null : key.substring(0, pt));
  }

  /**
   * Modulation data keys are keyed by model number as well as type using [at]n,
   * where n is the model number, starting with 0.
   * 
   * @param key
   * @return modulation data
   */
  @Override
  public double[] getMod(String key) {
    return htModulation.get(key + atModel);
  }

  @Override
  public Map<String, double[]> getModulationMap() {
    return htModulation;
  }

  /**
   * Called when structure creation is complete and all modulation data has been
   * collected.
   * 
   * @param iModel
   * @param isPost
   * @throws Exception
   */
  private void setModulationForStructure(int iModel, boolean isPost)
      throws Exception {
    atModel = "@" + iModel;

    if (htModulation.containsKey("X_" + atModel))
      return;

    if (!isPost) {
      initModForStructure(iModel);
      return;
    }

    // check to see we have not already done this.
    // X_ is just a dummy flag specifically for this purpose.

    htModulation.put("X_" + atModel, new double[0]);

    cr.appendLoadNote(modCount + " modulations for " + ac + " modulated atoms");
    if (!haveAtomMods)
      return;

    // here we go -- apply all atom modulations. 

    int n = cr.asc.ac;
    atoms = cr.asc.atoms;
    //cr.symmetry = cr.asc.getSymmetry();
    if (symmetry != null)
      nOps = symmetry.getSpaceGroupOperationCount();
    supercellSymmetry = cr.asc.getXSymmetry().symmetry;
    if (supercellSymmetry  == symmetry)
      supercellSymmetry = null;
    iopLast = -1;
    int i0 = cr.asc.getLastAtomSetAtomIndex();
    for (int i = i0; i < n; i++)
      modulateAtom(atoms[i]);
    htAtomMods = null;
    if (minXYZ0 != null)
      trimAtomSet();
    htSubsystems = null;
  }

  private void initModForStructure(int iModel) throws Exception {
    String key;

    // we allow an unlimited number of wave vectors in the form of a matrix
    // along with their lengths as an array.

    if (legendres != null)
      fixLegendre();

    sigma = new Matrix(null, modDim, 3);
    qs = null;

    modMatrices = new Matrix[] { sigma, null };

    // we should have W_1, W_2, W_3 up to the modulation dimension

    double[] pt;
    for (int i = 0; i < modDim; i++) {
      pt = getMod("W_" + (i + 1));
      if (pt == null) {
        cr.appendLoadNote("NOTE!: Not enough cell wave vectors for d=" + modDim);
        return;
      }
      fixDouble(pt);
      cr.appendLoadNote("W_" + (i + 1) + " = " + Escape.e(pt));
      cr.appendUunitCellInfo("q" + (i + 1) + "=" + pt[0] + " " + pt[1] + " "
          + pt[2]);
      sigma.getArray()[i] = new double[] { pt[0], pt[1], pt[2] };
    }

    // Take care of loose ends.
    // O: occupation   (set haveOccupancy; set a cos(theta) + b sin(theta) format)
    // D: displacement (set a cos(theta) + b sin(theta) format)
    // U: anisotropy   (no issues)
    // W: primary wave vector (see F if dim > 1)
    // F: Jana-type wave vector, referencing W vectors (set pt to coefficients, including harmonics)
    Map<String, double[]> map = new Hashtable<String, double[]>();
    for (Entry<String, double[]> e : htModulation.entrySet()) {
      if ((key = checkKey(e.getKey(), false)) == null)
        continue;
      pt = e.getValue();
      switch (key.charAt(0)) {
      case 'O':
        haveOccupancy = true;
        //$FALL-THROUGH$
      case 'D':
      case 'M':
      case 'U':
        // fix modulus/phase option only for non-special modulations;
        if (pt[2] == 1 && key.charAt(2) != 'S' && key.charAt(2) != 'T' && key.charAt(2) != 'L') {
          int ipt = key.indexOf("?");
          if (ipt >= 0) {
            String s = key.substring(ipt + 1);
            pt = getMod(key.substring(0, 2) + s + "#*;*");
            // may have       Vy1    0.0          0.0   , resulting in null pt here
            if (pt != null)
              addModulation(map, key = key.substring(0, ipt), pt, iModel);
          } else {
            // modulus/phase M cos(2pi(q.r) + 2pi(p))
            //  --> A cos(2pi(p)) cos(2pi(q.r)) + A sin(-2pi(p)) sin(2pi(q.r))
            double a = pt[0];
            double d = 2 * Math.PI * pt[1];
            pt[0] = (float) (a * Math.cos(d));
            pt[1] = (float) (a * Math.sin(-d));
            pt[2] = 0;
            Logger.info("msCIF setting " + key + " " + Escape.e(pt));
          }
        }
        break;
      case 'W':
        if (modDim > 1) {
          continue;
        }
        //$FALL-THROUGH$
      case 'F':
        // convert JANA Fourier descriptions to standard descriptions
        if (key.indexOf("_coefs_") >= 0) {
          // d > 1 -- already set from coefficients
          cr.appendLoadNote("Wave vector " + key + "=" + Escape.eAD(pt));
        } else {
          double[] ptHarmonic = calculateQCoefs(pt);
          if (ptHarmonic == null) {
            cr.appendLoadNote("Cannot match atom wave vector " + key + " "
                + Escape.eAD(pt) + " to a cell wave vector or its harmonic");
          } else {
            String k2 = key + "_coefs_";
            if (!htModulation.containsKey(k2 + atModel)) {
              addModulation(map, k2, ptHarmonic, iModel);
              if (key.startsWith("F_"))
                cr.appendLoadNote("atom wave vector " + key + " = "
                    + Escape.e(pt) + " fn = " + Escape.e(ptHarmonic));
            }
          }
        }
        break;
      }
    }

    if (!map.isEmpty())
      htModulation.putAll(map);

    // Collect atom modulations as lists keyed on atom names in htAtomMods.
    // Loop through all modulations, selecting only those for the current model.
    // Process O, D, and U modulations via method addAtomModulation

    if (htSubsystems == null) {
      haveAtomMods = false;
    } else {
      cr.strSupercell = null; // disallow setting unit cell
      haveAtomMods = true;
      htAtomMods = new Hashtable<String, Lst<Modulation>>();
    }
    for (Entry<String, double[]> e : htModulation.entrySet()) {
      if ((key = checkKey(e.getKey(), true)) == null)
        continue;
      double[] params = e.getValue();
      String atomName = key.substring(key.indexOf(";") + 1);
      int pt_ = atomName.indexOf("#=");
      if (pt_ >= 0) {
        params = getMod(atomName.substring(pt_ + 2));
        atomName = atomName.substring(0, pt_);
      }
      if (Logger.debuggingHigh)
        Logger.debug("SetModulation: " + key + " " + Escape.e(params));
      char type = key.charAt(0);
      pt_ = key.indexOf("#") + 1;
      String utens = null;
      switch (type) {
      case 'U':
        utens = key.substring(pt_, key.indexOf(";"));
        //$FALL-THROUGH$
      case 'O':
      case 'D':
      case 'M':
        if (modAverage)
          break;
        char axis = key.charAt(pt_);
        type = getModType(key);
        if (htAtomMods == null)
          htAtomMods = new Hashtable<String, Lst<Modulation>>();
        double[] p = new double[params.length];
        for (int i = p.length; --i >= 0;)
          p[i] = params[i];
        double[] qcoefs = getQCoefs(key);
        if (qcoefs == null)
          throw new Exception(
              "Missing cell wave vector for atom wave vector for " + key + " "
                  + Escape.e(params));
        addAtomModulation(atomName, axis, type, p, utens, qcoefs);
        haveAtomMods = true;
        break;
      }
    }
  }
  
  /*
   * find Legendre parameters and merge with corresponding Crenel methods 
   */

  private void fixLegendre() {
    for (int i = legendres.size(); --i >= 0;) {
      String key = legendres.get(i);
      double[] pt = htModulation.get(key);
      if (pt != null) {
        // look for corresponding crenel so that we can combine them.
        String key1 = "O_0#0" + key.substring(key.indexOf(";"));
        double[] pt1 = htModulation.get(key1);
        if (pt1 == null) {
          Logger.error("Crenel " + key1 + " not found for legendre modulation " + key);
          pt[2] = Float.NaN;
        } else {
          htModulation.put(key, new double[] { pt1[0], pt1[1], pt[0], pt[1] });
        }
      }
    }
  }
        
  private void fixDouble(double[] pt) {
    if (cr.fixJavaFloat)
      for (int i = pt.length; --i >= 0;)
        pt[i] = PT.fixDouble(pt[i], PT.FRACTIONAL_PRECISION);
  }

  @Override
  public double[] getQCoefs(String key) {
    int fn = Math.max(0, cr.parseIntAt(key, 2));        
    if (fn == 0) {
      if (qlist100 == null) {
        qlist100 = new double[modDim];
        qlist100[0] = 1;
      }
      return qlist100;
    }     
    // BH 2020.09.16 the addition of 2018.05.30 puts fn just after f_. 
    // it is not clear that anything puts it after it, but leaving that as an option.
    double[] p = getMod("F_coefs_" + fn );
    if (p == null)
      p = getMod("F_" + fn + "_coefs_");
    return p;
  }

  @Override
  public char getModType(String key) {
    // key = "type_id"
    char type = key.charAt(0);
    char id = key.charAt(2);
    return  (id == 'S' ? Modulation.TYPE_DISP_SAWTOOTH
        : id == 'T' ? Modulation.TYPE_SPIN_SAWTOOTH
        : id == 'L' ? (type == 'D' ? Modulation.TYPE_DISP_LEGENDRE
            : Modulation.TYPE_U_LEGENDRE)
        : id == '0' ? Modulation.TYPE_OCC_CRENEL
        : type == 'D' ? Modulation.TYPE_DISP_FOURIER
        : type == 'O' ? Modulation.TYPE_OCC_FOURIER
        : type == 'M' ? Modulation.TYPE_SPIN_FOURIER
        : type == 'U' ? Modulation.TYPE_U_FOURIER
        : '?');
  }

  private P3[] qs;

  private int modCount;

  private T3 modTUV;

  /**
   * determine simple linear combination assuming simple -3 to 3 no more than
   * two dimensions.
   * 
   * @param p
   * @return {i j k}
   */
  private double[] calculateQCoefs(double[] p) {
    if (qs == null) {
      qs = new P3[modDim];
      for (int i = 0; i < modDim; i++) {
        qs[i] = toP3(getMod("W_" + (i + 1)));
      }
    }
    P3 pt = toP3(p);
    // test n * q
    for (int i = 0; i < modDim; i++)
      if (qs[i] != null) {
        int ifn = approxInt(pt.dot(qs[i]) / qs[i].dot(qs[i]));
        if (ifn != 0) {
          p = new double[modDim];
          p[i] = ifn;
          return p;
        }
      }
    P3 p3 = toP3(p);
    int jmin = (modDim < 2 ? 0 : -3);
    int jmax = (modDim < 2 ? 0 : 3);
    int kmin = (modDim < 3 ? 0 : -3);
    int kmax = (modDim < 3 ? 0 : 3);
    for (int i = -3; i <= 3; i++)
      for (int j = jmin; j <= jmax; j++)
        for (int k = kmin; k <= kmax; k++) {
          // test linear combination -3 to +3:
          pt.setT(qs[0]);
          pt.scale(i);
          if (modDim > 1 && qs[1] != null)
            pt.scaleAdd2(j, qs[1], pt);
          if (modDim > 2 && qs[2] != null)
            pt.scaleAdd2(k, qs[2], pt);
          if (pt.distanceSquared(p3) < 0.0001f) {
            p = new double[modDim];
            switch (modDim) {
            default:
              p[2] = k;
              //$FALL-THROUGH$
            case 2:
              p[1] = j;
              //$FALL-THROUGH$
            case 1:
              p[0] = i;
              break;
            }
            return p;
          }
          // test linear combination 1/[-3 to +3]:
          pt.setT(qs[0]);
          pt.scale(1f/i);
          if (modDim > 1 && qs[1] != null)
            pt.scaleAdd2(1f/j, qs[1], pt);
          if (modDim > 2 && qs[2] != null)
            pt.scaleAdd2(1f/k, qs[2], pt);
          if (pt.distanceSquared(p3) < 0.0001f) {
            p = new double[modDim];
            switch (modDim) {
            default:
              p[2] = 1f/k;
              //$FALL-THROUGH$
            case 2:
              p[1] = 1f/j;
              //$FALL-THROUGH$
            case 1:
              p[0] = 1f/i;
              break;
            }
            return p;
          }
          
        }

    // test dropped rational component
    // eg: 
    //     q = 0 0.33333 0.166666 
    // and f = 0 0.33333 0.666666 

    pt = toP3(p);
    for (int i = 0; i < modDim; i++)
      if (qs[i] != null) {
        p3 = qs[i];
        int ifn = 0;
        if (pt.x != 0)
          ifn = approxInt(pt.x / p3.x);
        if (pt.y != 0)
          ifn = Math.max(approxInt(pt.y / p3.y), ifn);
        if (ifn == 0 && pt.z != 0)
          ifn = Math.max(approxInt(pt.z / p3.z), ifn);
        if (ifn == 0)
          continue;
        if (p3.x != 0 && approxInt(10 + p3.x * ifn - pt.x) == 0 || p3.y != 0
            && approxInt(10 + p3.y * ifn - pt.y) == 0 || p3.z != 0
            && approxInt(10 + p3.z * ifn - pt.z) == 0)
          continue;

        p = new double[modDim];
        p[i] = ifn;
        return p;
      }
    return null;
  }

  private int approxInt(float fn) {
    int ifn = Math.round(fn);
    return (Math.abs(fn - ifn) < 0.001f ? ifn : 0);
  }

  private P3 toP3(double[] x) {
    return P3.new3((float) x[0], (float) x[1], (float) x[2]);
  }

  /**
   * Create a list of modulations for each atom type (atom name).
   * 
   * @param atomName
   * @param axis
   * @param type
   * @param params
   * @param utens
   * @param qcoefs
   */
  private void addAtomModulation(String atomName, char axis, char type,
                                 double[] params, String utens, double[] qcoefs) {
    Lst<Modulation> list = htAtomMods.get(atomName);
    if (list == null) {
      ac++;
      htAtomMods.put(atomName, list = new Lst<Modulation>());
    }
    list.addLast(new Modulation(axis, type, params, utens, qcoefs));
    modCount++;
  }

  @Override
  public void addSubsystem(String code, Matrix w) {
    if (code == null)
      return;
    Subsystem ss = new Subsystem(this, code, w);
    cr.appendLoadNote("subsystem " + code + "\n" + w);
    setSubsystem(code, ss);
  }

  //  private void setSubsystems() {
  //    atoms = cr.asc.atoms;
  //    int n = cr.asc.ac;
  //    for (int i = cr.asc.getLastAtomSetAtomIndex(); i < n; i++) 
  //      getUnitCell(atoms[i]);
  //  }

  private final static String U_LIST = "U11U22U33U12U13U23UISO";

  private void addUStr(Atom atom, String id, float val) {
    int i = U_LIST.indexOf(id) / 3;
    if (Logger.debuggingHigh)
      Logger.debug("MOD RDR adding " + id + " " + i + " " + val + " to "
          + atom.anisoBorU[i]);
    cr.asc.setU(atom, i, val + atom.anisoBorU[i]);
  }

  /**
   * The displacement will be set as the atom vibration vector; the string
   * buffer will be appended with the t value for a given unit cell.
   * 
   * Modulation generally involves x4 = q.r + t. Here we arbitrarily set t =
   * modT = 0, but modT could be a FILTER option MODT=n. There would need to be
   * one modT per dimension or modU, modV.
   * 
   * @param a
   */
  private void /*float*/ modulateAtom(Atom a) {

    // Modulation is based on an atom's first symmetry operation.
    // (Special positions should generate the same atom regardless of which operation is employed.)

    if (modCoord && htSubsystems != null) {
      // I think this does nothing.....
      P3 ptc = P3.newP(a);
      SymmetryInterface spt = getSymmetry(a);
      spt.toCartesian(ptc, true);
    }

    Lst<Modulation> list = htAtomMods.get(a.atomName);
    if (list == null && a.altLoc != '\0' && htSubsystems != null) {
      // force treatment if a subsystem
      list = new Lst<Modulation>();
    }
    if (list == null || symmetry == null || a.bsSymmetry == null)
      return;// 0;

    int iop = Math.max(a.bsSymmetry.nextSetBit(0), 0);
    if (modLast)
      iop = Math.max((a.bsSymmetry.length() - 1) % nOps, iop);
    if (Logger.debuggingHigh)
      Logger.debug("\nsetModulation: i=" + a.index + " " + a.atomName + " xyz="
          + a + " occ=" + a.foccupancy);
    if (iop != iopLast) {
      // for each new operator, we need to generate new matrices.
      // gammaE is the pure rotation part of the operation;
      // nOps is used as a factor in occupation modulation only.
      iopLast = iop;
      gammaE = new M3();
      getSymmetry(a).getSpaceGroupOperation(iop).getRotationScale(gammaE);
    }
    if (Logger.debugging) {
      Logger.debug("setModulation iop = " + iop + " "
          + symmetry.getSpaceGroupXyz(iop, false) + " " + a.bsSymmetry);
    }

    // The magic happens here. Note that we must preserve any spin "vibration"
    // because we are going to repurpose that.
    ModulationSet ms = new ModulationSet().setMod(a.index + " " + a.atomName,
        getAtomR0(cr.asc.atoms[a.atomSite]), getAtomR0(a), 
        modDim, list, gammaE, getMatrices(a), getSymmetry(a), nOps, iop, 
        a.vib instanceof Vibration ? (Vibration) a.vib : null, isCommensurate);

    ms.calculate(modTUV, false);

    // ms parameter values are used to set occupancies, 
    // vibrations, and anisotropy tensors.

    if (!Float.isNaN(ms.vOcc)) {
      // a.vib may be used to temporarily store an M40 site multiplicity
      a.foccupancy = ms.setOccupancy(getMod("J_O#0;" + a.atomName), a.foccupancy, (a.vib == null ? 0 : a.vib.x));
      //Logger.info("atom " + a.atomName + " occupancy = " + a.foccupancy);
    }
    if (ms.htUij != null) {
      // Uiso or Uij. We add the displacements, create the tensor, then rotate it, 
      // replacing the tensor already present for that atom.
      Tensor t = (a.tensors == null ? null : (Tensor) a.tensors.get(0));
      if (t != null && t.parBorU != null) {
        // restore ORIGINAL (unrotated) anisotropy parameters
        a.anisoBorU = new float[8];
        for (int i = 0; i < 8; i++)
          a.anisoBorU[i] = t.parBorU[i];
        t.isUnmodulated = true;
      }
      if (a.anisoBorU == null) {
        Logger
            .error("MOD RDR cannot modulate nonexistent atom anisoBorU for atom "
                + a.atomName);
      } else {
        if (Logger.debuggingHigh) {
          Logger.debug("setModulation Uij(initial)=" + Escape.eAF(a.anisoBorU));
          Logger.debug("setModulation tensor="
              + Escape.e(((Tensor) a.tensors.get(0)).getInfo("all")));
        }
        for (Entry<String, Float> e : ms.htUij.entrySet())
          addUStr(a, e.getKey(), e.getValue().floatValue());

        SymmetryInterface sym = getAtomSymmetry(a, symmetry);
        t = cr.asc.getXSymmetry().addRotatedTensor(a,
            sym.getTensor(cr.vwr, a.anisoBorU), iop, false, sym);
        t.isModulated = true;
        t.id = Escape.e(a.anisoBorU);
        // note that a.bFactor will be modulated value
        a.bfactor = a.anisoBorU[7] * 100f;
        // prevent further tensor production
        a.anisoBorU = null;
        if (Logger.debuggingHigh) {
          Logger.debug("setModulation Uij(final)=" + Escape.eAF(a.anisoBorU)
              + "\n");
          Logger.debug("setModulation tensor="
              + Escape.e(((Tensor) a.tensors.get(1)).getInfo("all")));
        }
      }
    }
    if (Float.isNaN(ms.x))
      ms.set(0, 0, 0);
    // notice that if we had a spin, it is REPLACED by its 
    // modulation, which now refers to it. 
    // This modulation may or may not be a modulation of 
    // the vibration itself.
    
    a.vib = ms;

    // BH: removed 7/2014; not documented and not useful
//    // set property_modT to be Math.floor (q.r/|q|) -- really only for d=1
//
//    if (!modVib && a.foccupancy == 0)
//      return Float.NaN;
//    float t = q1Norm.dot(a);
//    if (Math.abs(t - (int) t) > 0.001f)
//      t = (int) Math.floor(t);
//    return (int) t;
  }

  private P3 getAtomR0(Atom atom) {
    P3 r0 = P3.newP(atom);
    if (supercellSymmetry != null) {
      supercellSymmetry.toCartesian(r0, true);
      symmetry.toFractional(r0, true);
    }
    return r0;
  }

  /**
   * When applying symmetry, this method allows us to use a set of symmetry
   * operators unique to this particular atom -- or in this case, to its
   * subsystem.
   * 
   */
  @Override
  public SymmetryInterface getAtomSymmetry(Atom a,
                                           SymmetryInterface defaultSymmetry) {
    Subsystem ss;
    return (htSubsystems == null || (ss = getSubsystem(a)) == null ? defaultSymmetry
        : ss.getSymmetry());
  }

  Map<String, Subsystem> htSubsystems;

  private void setSubsystem(String code, Subsystem system) {
    if (htSubsystems == null)
      htSubsystems = new Hashtable<String, Subsystem>();
    htSubsystems.put(code, system);
    setSubsystemOptions();
  }

  private Matrix[] getMatrices(Atom a) {
    Subsystem ss = getSubsystem(a);
    return (ss == null ? modMatrices : ss.getModMatrices());
  }

  private SymmetryInterface getSymmetry(Atom a) {
    Subsystem ss = getSubsystem(a);
    return (ss == null ? symmetry : ss.getSymmetry());
  }

  private Subsystem getSubsystem(Atom a) {
    return (htSubsystems == null ? null : htSubsystems.get("" + a.altLoc));
  }

  private P3 minXYZ0, maxXYZ0;

  @Override
  public void setMinMax0(P3 minXYZ, P3 maxXYZ) {
    if (htSubsystems == null)
      return;
    SymmetryInterface symmetry = getDefaultUnitCell();
    minXYZ0 = P3.newP(minXYZ);
    maxXYZ0 = P3.newP(maxXYZ);
    P3 pt0 = P3.newP(minXYZ);
    P3 pt1 = P3.newP(maxXYZ);
    P3 pt = new P3();
    symmetry.toCartesian(pt0, true);
    symmetry.toCartesian(pt1, true);
    P3[] pts = BoxInfo.unitCubePoints;
    if (sigma == null) {
      Logger.error("Why are we in MSRdr.setMinMax0 without modulation init?");
      return;
    }
    for (Entry<String, Subsystem> e : htSubsystems.entrySet()) {
      SymmetryInterface sym = e.getValue().getSymmetry();
      for (int i = 8; --i >= 0;) {
        pt.x = (pts[i].x == 0 ? pt0.x : pt1.x);
        pt.y = (pts[i].y == 0 ? pt0.y : pt1.y);
        pt.z = (pts[i].z == 0 ? pt0.z : pt1.z);
        expandMinMax(pt, sym, minXYZ, maxXYZ);
      }
    }
    //System.out.println("msreader min max " + minXYZ + " " + maxXYZ);
  }

  private void expandMinMax(P3 pt, SymmetryInterface sym, P3 minXYZ, P3 maxXYZ) {
    P3 pt2 = P3.newP(pt);
    float slop = 0.0001f;
    sym.toFractional(pt2, false);
    if (minXYZ.x > pt2.x + slop)
      minXYZ.x = (int) Math.floor(pt2.x) - 1;
    if (minXYZ.y > pt2.y + slop)
      minXYZ.y = (int) Math.floor(pt2.y) - 1;
    if (minXYZ.z > pt2.z + slop)
      minXYZ.z = (int) Math.floor(pt2.z) - 1;
    if (maxXYZ.x < pt2.x - slop)
      maxXYZ.x = (int) Math.ceil(pt2.x) + 1;
    if (maxXYZ.y < pt2.y - slop)
      maxXYZ.y = (int) Math.ceil(pt2.y) + 1;
    if (maxXYZ.z < pt2.z - slop)
      maxXYZ.z = (int) Math.ceil(pt2.z) + 1;
  }

  private void trimAtomSet() {
    if (!cr.doApplySymmetry)
      return;
    AtomSetCollection asc = cr.asc;
    BS bs = asc.bsAtoms;
    SymmetryInterface sym = getDefaultUnitCell();
    Atom[] atoms = asc.atoms;
    P3 pt = new P3();
    if (bs == null)
      bs = asc.bsAtoms = BSUtil.newBitSet2(0, asc.ac);
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      Atom a = atoms[i];
      boolean isOK = (!isCommensurate || modAverage || a.foccupancy >= 0.5f);
      if (isOK) {
        pt.setT(a);
        // add in modulation
        if (a.vib != null)
          pt.add(a.vib);
        getSymmetry(a).toCartesian(pt, false);
        sym.toFractional(pt, false);
        if (cr.fixJavaFloat)
          PT.fixPtFloats(pt, PT.FRACTIONAL_PRECISION);
        isOK = asc.xtalSymmetry.isWithinCell(3, pt, minXYZ0.x, maxXYZ0.x,
            minXYZ0.y, maxXYZ0.y, minXYZ0.z, maxXYZ0.z, 0.001f);
        //          || (cr.legacyJavaFloat ? !asc.xtalSymmetry.isWithinCell(3, pt, minXYZ0.x, maxXYZ0.x,
        //          minXYZ0.y, maxXYZ0.y, minXYZ0.z, maxXYZ0.z, 0.001f) 
        //          : !asc.xtalSymmetry.isWithinCellInt(3, pt, minXYZ0.x, maxXYZ0.x,
        //              minXYZ0.y, maxXYZ0.y, minXYZ0.z, maxXYZ0.z, 100))) {
        //              }
      }
      if (isOK) {
        if (cr.fixJavaFloat)
          PT.fixPtFloats(a, PT.FRACTIONAL_PRECISION);
      } else {
        bs.clear(i);
      }
    }
  }

  private SymmetryInterface getDefaultUnitCell() {
    return (modCell != null && htSubsystems.containsKey(modCell) ? htSubsystems
        .get(modCell).getSymmetry() : cr.asc.getSymmetry());
  }

  @Override
  public SymmetryInterface getSymmetryFromCode(String code) {
    return htSubsystems.get(code).getSymmetry();
  }

  @Override
  public boolean addLatticeVector(Lst<float[]> lattvecs, String data)
      throws Exception {
    float[] a = null;
    char c = data.charAt(0);
    int dim = modDim + 3;
    switch (c) {
    case 'P':
    case 'X':
      break;
    case 'A':
    case 'B':
    case 'C':
    case 'I':
      a = new float[] { 0.5f, 0.5f, 0.5f };
      if (c != 'I')
        a[c - 'A'] = 0;
      break;
    case 'F':
      addLatticeVector(lattvecs, "A");
      addLatticeVector(lattvecs, "B");
      addLatticeVector(lattvecs, "C");
      break;
    case 'M': // magnetic
      dim++;
      //$FALL-THROUGH$
    case '0': // X explicit
      if (data.indexOf(".") >= 0)
        a = AtomSetCollectionReader.getTokensFloat(data, null, dim);
      break;
    default:
      return false;
    }
    if (a != null)
      lattvecs.addLast(a);
    return true;
  }

//  //  program GetOrthoWaves
//  //  real, allocatable :: OrthoMat(:,:),OrthoMatI(:,:)
//  //  integer, allocatable :: isel(:)
//  //  character*80 FormO,Veta,t80
//  //  open(40,file='GetOrthoWaves.infile')
//  //  read(40,*,err=9999,end=9999) n,delta,x40,eps
//  //  close(40)
//  //  if(n.le.0) go to 9999
//  //  allocate(OrthoMat(n,n),OrthoMatI(n,n),isel(n))
//  //  call SelectWaves(x40,delta,eps,isel,n)
//  //  call SetOrthoMatSelected(X40,Delta,isel,n,OrthoMat,OrthoMatI)
//  //  open(40,file='GetOrthoWaves.outfile')
//  //  write(40,'(''Used harmonics:''/''#1:  1'')')
//  //  do i=2,n
//  //    if(mod(isel(i)+1,2).eq.0) then
//  //      Veta='sin'
//  //    else
//  //      Veta='cos'
//  //    endif
//  //    write(Veta(4:),'(i5)') (isel(i)+1)/2
//  //    call Zhusti(Veta)
//  //    write(t80,'(''#'',i5)') i
//  //    call Zhusti(t80)
//  //    write(40,'(2a5)') t80,Veta
//  //  enddo
//  //  FormO='(...e15.6)'
//  //  write(FormO(2:4),'(i3)') n
//  //  do i=1,n
//  //    write(Veta,'(i5)') i
//  //    call Zhusti(Veta)
//  //    write(40,'(''Ortho function #'',a)') Veta(:idel(Veta))
//  //    write(40,FormO) (OrthoMat(i,j),j=1,n)
//  //  enddo
//  //9999  stop
//  //  end
//
//  /**
//   * 
//   * @param x40
//   *        center
//   * @param delta
//   *        width
//   * @param eps
//   *        cutoff (lambda)
//   * @param isel
//   *        dimension n
//   */
//  public void selectWaves(double x40, double delta, double eps, int[] isel) {
//
//    int nn = isel.length;
//    Matrix gor = new Matrix(null, nn, nn);
//    Matrix p = new Matrix(null, nn, 1);    
//    Matrix gd = new Matrix(null, nn + 1, nn);
//    int[] iselp = new int[nn + 1];
//    if (eps < 999) {
//
//      setOrthoMat(x40, delta, gor, nn);
//      for (int i = 0; i < nn; i++)
//        p.a[i][1] = Math.sqrt(gor.a[i][i]);
//      for (int i = 0; i < nn; i++)
//        for (int j = 0; j < nn; j++)
//          gor.a[i][j] /= p.a[i][1] * p.a[j][1];
//      gd.a[0][0] = 1;
//      int ngd = 0;
//      iselp[0] = 1;
//      for (int i = 0, j = 0; j < nn; j++) {
//        for (int n = 0, k = 0; k < i; k++)
//          if (iselp[k] != 0)
//            p.a[n++][0] = gor.a[j][k];
//        Matrix hh = gd.mul(p).mul(p);
//        if (hh.a[0][0] <= eps * eps) {
//          iselp[j] = 1;
//          ngd++;
//          i = j;
//          for (int n = 0, ii = 0; ii < i; ii++)
//            if (iselp[ii] != 0)
//              for (int jj = 0; jj < ii; jj++)
//                if (iselp[jj] != 0)
//                  gd.a[ii][jj] = gor.a[ii][jj];
//          smi(gd, ngd, ising);
//        }
//      }
//
//    }
//  }
//
//  private void setOrthoMat(double x40, double delta, Matrix gor, int n) {
//    double x1=x40-.5*delta;
//    double x2=x40+.5*delta;
//    jip=1
//    do i=1,n
//      if(i.eq.1) then
//        ii=1
//      else
//        ni=mod(i,2)
//        nj=i/2
//        ii=nj*2+ni
//      endif
//      ij=i
//      ji=jip
//      do j=1,i
//        if(j.eq.1) then
//          jj=1
//        else
//          ni=mod(j,2)
//          nj=j/2
//          jj=nj*2+ni
//        endif
//        pom=CosSinProd(ii,jj,x1,x2)/delta
//        if(i.ne.j) gor(ij)=pom
//        gor(ji)=pom
//        ji=ji+1
//        ij=ij+n
//      enddo
//      jip=jip+n
//    enddo
//    return
//    end
//
//    // TODO
//    
//  }
//
//  double PI2 = 6.283185308;
//
//  private double CosSinProd(int n,int m,double x1,double x2) {
//    double[] fc = new double[3];
//    double[] xs = new double[6];
//    int nl = Math.min(n, m);
//    int ml = Math.max(n, m);
//    int i;
//    double a, b;
//    if (n > 1) {
//      int i=Math.signum(nl) * Math.abs(nl)/2;
//    
//    a=pi2*i;
//  endif
//  if(ml.gt.1) then
//    i=isign(iabs(ml)/2,nl)
//    b=pi2*float(i)
//  endif
//  i=isign(iabs(nl)/2,nl)
//  knl=mod(nl,2)
//  kml=mod(ml,2)
//  if(nl.eq.1) then
//    if(ml.eq.1) then
//      CosSinProd=x2-x1
//    else if(kml.eq.0) then
//      CosSinProd=-1./b*(cosnas(b*x2)-cosnas(b*x1))
//    else
//      CosSinProd=1./b*(sinnas(b*x2)-sinnas(b*x1))
//    endif
//  else if(knl.eq.0.and.kml.eq.0) then
//    if(a.eq.b) then
//      CosSinProd=.5*(x2-x1)-.25/a*(sinnas(2.*a*x2)-sinnas(2.*a*x1))
//    else if(a.eq.-b) then
//      CosSinProd=.5*(x1-x2)+.25/a*(sinnas(2.*a*x2)-sinnas(2.*a*x1))
//    else
//      amb=a-b
//      apb=a+b
//      CosSinProd=.5/amb*(sinnas(amb*x2)-sinnas(amb*x1))-
// 1               .5/apb*(sinnas(apb*x2)-sinnas(apb*x1))
//    endif
//  else if(knl.eq.1.and.kml.eq.1) then
//    if(abs(a).eq.abs(b)) then
//      CosSinProd=.5*(x2-x1)+.25/a*(sinnas(2.*a*x2)-sinnas(2.*a*x1))
//    else
//      amb=a-b
//      apb=a+b
//      CosSinProd=.5/amb*(sinnas(amb*x2)-sinnas(amb*x1))+
// 1               .5/apb*(sinnas(apb*x2)-sinnas(apb*x1))
//    endif
//  else
//    if(knl.eq.1) then
//      pom=a
//      a=b
//      b=pom
//    endif
//    if(abs(a).eq.abs(b)) then
//      CosSinProd=.5/a*(sinnas(a*x2)**2-sinnas(a*x1)**2)
//    else
//      amb=a-b
//      apb=a+b
//      CosSinProd=-.5/amb*(cosnas(amb*x2)-cosnas(amb*x1))-
// 1                .5/apb*(cosnas(apb*x2)-cosnas(apb*x1))
//    endif
//  endif
//  return
//
//  private void nasob(double[] a, double[] p, double[] x, int n) {
//    for (int ijp = 0, i = 0; i < n; i++) {
//      double xi = 0;
//      int ij = ijp;
//      for (int id = 0, j = 0; j < n; j++) {
//        ij += id;
//        xi += a[ij] * p[j];
//        if (j == i)
//          id = j;
//      }
//      x[i] = xi;
//      
//          ijp=ijp+i;
//    }
//  }
//  
//  private void multm(double[] a,double[] b,double[] c, int n1, int n2, int n3) {
//    for (int i = 0; i < )
//  }
//  dimension a(*),b(*),c(*)
//  do i=1,n1
//    jkp=1
//    ik=i
//    do k=1,n3
//      ij=i
//      jk=jkp
//      p=0.
//      do j=1,n2
//        p=p+a(ij)*b(jk)
//        ij=ij+n1
//        jk=jk+1
//      enddo
//      c(ik)=p
//      ik=ik+n1
//      jkp=jkp+n2
//    enddo
//  enddo
//  return
//  end
//  function cosnas(x)
//  cosnas=dcos(dble(od0doPi2(x)))
//  return
//  end
//  function sinnas(x)
//  sinnas=dsin(dble(od0doPi2(x)))
//  return
//  end
//
}
