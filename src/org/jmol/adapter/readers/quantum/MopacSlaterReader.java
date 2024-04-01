/* $RCSfile$
 * $Author: egonw $
 * $Date: 2006-03-18 15:59:33 -0600 (Sat, 18 Mar 2006) $
 * $Revision: 4652 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.adapter.readers.quantum;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.adapter.smarter.Atom;
import org.jmol.quantum.SlaterData;

/**
 * 
 * @author hansonr <hansonr@stolaf.edu>
 */
abstract class MopacSlaterReader extends SlaterReader {

  protected final static double MIN_COEF = 0.0001d; // sufficient?  
  protected int[] atomicNumbers;

  /**
   * GAMESS may need AM1, PMn, or RM1 zeta/coef data
   */
  protected double[][] mopacBasis;
  protected boolean allowMopacDCoef;

  /**
   * overrides method in SlaterReader to allow for MOPAC's treatment of the
   * radial exponent differently depending upon position in the periodic table
   * -- noble gases and transition metals and for the fact that these are
   * spherical functions (5D, not 6D)
   * 
   * ignores any F orbitals.
   * 
   * @param ex
   * @param ey
   * @param ez
   * @param er
   * @param zeta
   * @return scaling factor
   */
  @Override
  protected double scaleSlater(int ex, int ey, int ez, int er, double zeta) {
    int el = Math.abs(ex + ey + ez);
    switch (el) {
    case 0: // s
      return getSlaterConstSSpherical(er + 1, Math.abs(zeta));
    case 1: // p
      return getSlaterConstPSpherical(er + 2, Math.abs(zeta));
    }
    if (ex >= 0 && ey >= 0) {
      // no need for special attention here
      return super.scaleSlater(ex, ey, ez, er, zeta);
    }
    if (el == 3) {
      return 0; // not set up for spherical f
    }

    // A negative zeta means this is contracted, so 
    // there are not as many molecular orbital 
    // coefficients as there are slaters. For example, 
    // an atom's s orbital might have one coefficient
    // for a set of three slaters -- the contracted set.

    return getSlaterConstDSpherical(el + er + 1, Math.abs(zeta), ex, ey);
  }

  /**
   * spherical scaling factors specifically for x2-y2 and z2 orbitals
   * 
   * see http://openmopac.net/Manual/real_spherical_harmonics.html
   * 
   * dz2 sqrt((1/2p)(5/8))(2cos2(q) -sin2(q)) sqrt(5/16p)(3z2-r2)/r2 dxz
   * sqrt((1/2p)(15/4))(cos(q)sin(q))cos(f) sqrt(15/4p)(xz)/r2 dyz
   * sqrt((1/2p)(15/4))(cos(q)sin(q))sin(f) sqrt(15/4p)(yz)/r2 dx2-y2
   * sqrt((1/2p)(15/16))sin2(q)cos2(f) sqrt(15/16p)(x2-y2)/r2 dxy
   * sqrt((1/2p)(15/16))sin2(q)sin2(f) sqrt(15/4p)(xy)/r2
   *
   * The fact() method returns sqrt(15/4p) for both z2 and x2-y2. So now we ned
   * to correct that with sqrt(1/12) for z2 and sqrt(1/4) for x2-y2.
   * 
   * http://openmopac.net/Manual/real_spherical_harmonics.html
   *
   * Apply the appropriate scaling factor for spherical D orbitals.
   * 
   * ex will be -2 for z2; ey will be -2 for x2-y2
   * 
   * 
   * @param n
   * @param zeta
   * @param ex
   * @param ey
   * @return scaling factor
   */
  private final static double getSlaterConstDSpherical(int n, double zeta,
                                                       int ex, int ey) {
    // BH 2024.03.20 found missing "d" in 15d, so this was using integer division!

    return fact(15d / (ex < 0 ? 12 : ey < 0 ? 4 : 1), zeta, n);
  }

  private final static double getSlaterConstSSpherical(int n, double zeta) {
    // these values were determined empirically using Au (n=6) and Ag (n=5);
    // other atoms with n=3 and 4 were no problem.
    return Math.pow(2 * zeta, n + 0.5) * Math.sqrt(_1_4pi / fact_2n[n]);
  }
  
//  private final static double calcSlater(double r, int n, double zeta) {
//    double N = getSlaterConstSSpherical(n, zeta);
//    return N * Math.pow(r, n-1) * Math.exp(-zeta * r);
//  }
//  static {
// //for Excel. 
//    double zeta = 1;
//    double [] a = new double[7];
//    for (int i = 0; i <= 100; i++) {
//      double r = 10 * i/100d;
//      a[0] = r;
//      for (int n = 1; n <= 6; n++)
//         a[n] = calcSlater(r, n, zeta);
//      System.out.println(Arrays.toString(a).replace('[', ' ').replace(']', ' ').trim());
//    }
//    System.out.println("xxxx");
//  }

  private final static double getSlaterConstPSpherical(int n, double zeta) {
    // these values were determined emprically using Au (n=6) and Ag (n=5);
    // other atoms with n=3 and 4 were no problem.
    double f = fact_2n[n] / 3;
    return Math.pow(2 * zeta, n + 0.5) * Math.sqrt(_1_4pi / f);
  }

  @Override
  public void setMOData(boolean clearOrbitals) {
    if (!allowNoOrbitals && orbitals.size() == 0)
      return;
    if (mopacBasis == null
        || !forceMOPAC && gaussians != null && shells != null) {
      if (forceMOPAC)
        System.out.println(
            "MopacSlaterReader ignoring MOPAC zeta parameters -- using Gaussian contractions");
      super.setMOData(clearOrbitals);
      return;
    }
    setSlaters(false);
    moData.put("calculationType", calculationType);
    moData.put("energyUnits", energyUnits);
    moData.put("mos", orbitals);
    finalizeMOData(lastMoData = moData);
    if (clearOrbitals) {
      clearOrbitals();
    }

  }

  /*
   * Sincere thanks to Jimmy Stewart, MrMopac@att.net for these constants
   * 
   */

  ///////////// MOPAC CALCULATION SLATER CONSTANTS //////////////

  // see http://openmopac.net/Downloads/Mopac_7.1source.zip|src_modules/parameters_C.f90

  //H                                                             He
  //Li Be                                          B  C  N  O  F  Ne
  //Na Mg                                          Al Si P  S  Cl Ar
  //K  Ca Sc          Ti V  Cr Mn Fe Co Ni Cu Zn   Ga Ge As Se Br Kr
  //Rb Sr Y           Zr Nb Mo Tc Ru Rh Pd Ag Cd   In Sn Sb Te I  Xe
  //Cs Ba La Ce-Lu    Hf Ta W  Re Os Ir Pt Au Hg   Tl Pb Bi Po At Rn
  //Fr Ra Ac Th-Lr    ?? ?? ?? ??

  private final static int[] principalQuantumNumber = new int[] { 0, 1, 1, //  2
      2, 2, 2, 2, 2, 2, 2, 2, // 10
      3, 3, 3, 3, 3, 3, 3, 3, // 18
      4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, // 36
      5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, // 54
      6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
      6, 6, 6, 6, 6, 6, 6, // 86
  };

  private final static int[] npqd = new int[] { 0, 0, 3, // 2
      0, 0, 0, 0, 0, 0, 0, 3, // 10
      3, 3, 3, 3, 3, 3, 3, 4, // 18
      3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 5, // 36
      4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 6, // 54
      5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
      6, 6, 6, 6, 6, 6, 7, // 86
  };

  private final static int getNPQ(int atomicNumber) {
    return (atomicNumber < principalQuantumNumber.length
        ? principalQuantumNumber[atomicNumber]
        : 0);
  }

  /**
   * for S orbitals, MOPAC adds 1 to n for noble gases other than helium
   * 
   * @param atomicNumber
   * @return adjusted principal quantum number
   */
  private final static int getNPQs(int atomicNumber) {
    int n = getNPQ(atomicNumber);
    switch (atomicNumber) {
    case 10:
    case 18:
    case 36:
    case 54:
    case 86:
      return n + 1;
    default:
      return n;
    }
  }

  /**
   * for P orbitals, MOPAC adds 1 to n for helium only
   * 
   * @param atomicNumber
   * @return adjusted principal quantum number
   */
  private final static int getNPQp(int atomicNumber) {
    int n = getNPQ(atomicNumber);
    switch (atomicNumber) {
    case 2:
      return n + 1;
    default:
      return n;
    }
  }

  /**
   * for D orbitals, MOPAC adds 1 to n for noble gases but subtracts 1 from n
   * for transition metals
   * 
   * @param atomicNumber
   * @return adjusted principal quantum number
   */
  private final static int getNPQd(int atomicNumber) {
    return (atomicNumber < npqd.length ? npqd[atomicNumber] : 0);
  }

  final private static int[] sphericalDValues = new int[] {
      // MOPAC2007 graphf output data order
      0, -2, 0, //dx2-y2
      1, 0, 1, //dxz
      -2, 0, 0, //dz2
      0, 1, 1, //dyz
      1, 1, 0, //dxy
  };

  /**
   * When slater basis is referred to only by "AM1" "PM6" etc., as in GAMESS
   */
  @Override
  protected void addSlaterBasis() {
    if (mopacBasis == null || slaters != null && slaters.size() > 0)
      return;
    int ac = asc.ac;
    int i0 = asc.getLastAtomSetAtomIndex();
    Atom[] atoms = asc.atoms;
    for (int i = i0; i < ac; ++i) {
      int an = atoms[i].elementNumber;
      createMopacSlaters(i, an, mopacBasis[an], allowMopacDCoef);
    }
  }

  public void createMopacSlaters(int iAtom, int atomicNumber, double[] values,
                                 boolean allowD) {
    double zeta;
    if ((zeta = values[0]) != 0) {
      createSphericalSlaterByType(iAtom, atomicNumber, "S", zeta, 1);
    }
    if ((zeta = values[1]) != 0) {
      createSphericalSlaterByType(iAtom, atomicNumber, "Px", zeta, 1);
      createSphericalSlaterByType(iAtom, atomicNumber, "Py", zeta, 1);
      createSphericalSlaterByType(iAtom, atomicNumber, "Pz", zeta, 1);
    }
    if ((zeta = values[2]) != 0 && allowD) {
      createSphericalSlaterByType(iAtom, atomicNumber, "Dx2-y2", zeta, 1);
      createSphericalSlaterByType(iAtom, atomicNumber, "Dxz", zeta, 1);
      createSphericalSlaterByType(iAtom, atomicNumber, "Dz2", zeta, 1);
      createSphericalSlaterByType(iAtom, atomicNumber, "Dyz", zeta, 1);
      createSphericalSlaterByType(iAtom, atomicNumber, "Dxy", zeta, 1);
    }
  }

  /**
   * We have the type as a string and need to translate that to exponents for x,
   * y, z, and r. No F here.
   * 
   * @param iAtom
   * @param atomicNumber
   * @param type
   * @param zeta
   * @param coef
   */
  protected void createSphericalSlaterByType(int iAtom, int atomicNumber,
                                             String type, double zeta,
                                             double coef) {
    int pt = "S Px Py Pz  Dx2-y2Dxz Dz2 Dyz Dxy".indexOf(type);
    //....... 0 2  5  8   12    18  22  26  30
    switch (pt) {
    case 0: // s
      SlaterData sd = addSlater(iAtom + 1, 0, 0, 0, getNPQs(atomicNumber) - 1, zeta, coef);
      sd.elemNo = atomicNumber;
      return;
    case 2: // Px
    case 5: // Py
    case 8: // Pz
      addSlater(iAtom + 1, pt == 2 ? 1 : 0, pt == 5 ? 1 : 0, pt == 8 ? 1 : 0,
          getNPQp(atomicNumber) - 2, zeta, coef);
      return;
    }
    pt = (pt >> 2) * 3 - 9; // 12->0, 18->3, 22->6, 26->9, 30->12
    addSlater(iAtom + 1, sphericalDValues[pt++], sphericalDValues[pt++],
        sphericalDValues[pt++], getNPQd(atomicNumber) - 3, zeta, coef);
  }

  private static Map<String, double[][]> mopacParams;

  public final static String MOPAC_TYPES = "AM1  MNDO PM3  PM6  PM7  RM1";

  /**
   * Retrieve the MOPAC zeta(1/bohr) [s,p,d] array by atom number
   * 
   * @param type
   * @return [[zs,zp,zd],[zs,zp,zd]...] where [1] is for hydrogen, [6] is for
   *         carbon, etc.
   */
  public static double[][] getMopacAtomZetaSPD(String type) {
    if (mopacParams == null)
      mopacParams = new Hashtable<String, double[][]>();
    double[][] params = mopacParams.get(type);
    if (params == null) {
      mopacParams.put(type, params = new double[120][3]);
      double[] data = null;
      switch (MOPAC_TYPES.indexOf(type)) {
      case 0:
        data = _AM1_C;
        break;
      case 5:
        data = _MNDO_C;
        break;
      case 10:
        data = _PM3_C;
        break;
      case 15:
        data = _PM6_C;
        break;
      case 20:
        data = _PM7_C;
        break;
      case 25:
        addData(params, _AM1_C);
        data = _RM1_C;
        break;
      default:
        System.err.println(
            "MopacSlaterReader could not find MOPAC params for " + type);
        return null;
      }
      addData(params, data);
    }
    System.out.println("MopacSlaterReader using MOPAC params for " + type);
    return params;
  }

  /**
   * Add Fortran zeta data
   * 
   * @param params
   * @param data
   */
  private static void addData(double[][] params, double[] data) {
    for (int i = 0, p = 0, a = 0; i < data.length; i++) {
      double d = data[i];
      if (d < 0) {
        a = (int) -d;
        p = 0;
        continue;
      }
      params[a][p++] = d;
    }
  }

  /**
   * Data from MOPAC F90 source parameters_for_xxx_C.F90 for use in
   * GamessReader. Values are 1/Bohr
   * 
   * Bob Hanson 2021.03.16
   */
  final static double[] _AM1_C = {

      /* Hydrogen */-1, 1.188078d,

      /* Helium */-2, 2.1956103d, 6.9012486d,

      /* Lithium */-3, 0.7973487d, 0.9045583d,

      /* Beryllium */-4, 0.7425237d, 0.8080499d,

      /* Carbon */-6, 1.808665d, 1.685116d,

      /* Nitrogen */-7, 2.31541d, 2.15794d,

      /* Oxygen */-8, 3.108032d, 2.524039d,

      /* Fluorine */-9, 3.770082d, 2.49467d,

      /* Neon */-10, 5.998377d, 4.1699304d,

      /* Sodium */-11, 0.789009d, 1.1399864d,

      /* Magnesium */-12, 1.0128928d, 1.1798191d,

      /* Aluminum */-13, 1.516593d, 1.306347d,

      /* Silicon */-14, 1.830697d, 1.284953d,

      /* Phosphorus */-15, 1.98128d, 1.87515d,

      /* Sulfur */-16, 2.366515d, 1.667263d,

      /* Chlorine */-17, 3.631376d, 2.076799d,

      /* Argon */-18, 0.9714216d, 5.9236231d,

      /* Potassium */-19, 1.2660244d, 0.9555939d,

      /* Calcium */-20, 1.1767754d, 1.273852d,

      /* Zinc */-30, 1.954299d, 1.372365d,

      /* Gallium */-31, 4.000216d, 1.3540466d,

      /* Germanium */-32, 1.219631d, 1.982794d,

      /* Arsenic */-33, 2.2576897d, 1.724971d,

      /* Selenium */-34, 2.684157d, 2.0506164d,

      /* Bromine */-35, 3.064133d, 2.038333d,

      /* Krypton */-36, 3.5931632d, 2.0944633d,

      /* Rubidium */-37, 4.0000187d, 1.0140619d,

      /* Strontium */-38, 1.5236848d, 1.5723524d,

      /* Molybdenum */-42, 1.945d, 1.477d, 2.468d,

      /* Indium */-49, 1.8281576d, 1.48475d,

      /* Tin */-50, 1.6182807d, 1.5084984d,

      /* Antimony */-51, 2.254823d, 2.218592d,

      /* Tellurium */-52, 2.1321165d, 1.971268d,

      /* Iodine */-53, 2.102858d, 2.161153d,

      /* Xenon */-54, 4.9675243d, 3.1432142d,

      /* Cesium */-55, 5.7873708d, 1.0311693d,

      /* Barium */-56, 1.9136517d, 1.3948894d,

      /* Mercury */-80, 2.036413d, 1.955766d,

      /* Thallium */-81, 3.8077333d, 1.5511578d,

      /* Lead */-82, 2.4432161d, 1.5506706d,

      /* Bismuth */-83, 4.0007862d, 0.9547714d,

      /* Nobelium */-102, 4d, 0.3d, 0.3d, /* Rutherfordium */-104,

  };
  private final static double[] _MNDO_C = {

      /* Hydrogen */-1, 1.331967d,

      /* Helium */-2, 1.7710761d, 6.9018258d,

      /* Lithium */-3, 0.4296141d, 0.7554884d,

      /* Beryllium */-4, 1.00421d, 1.00421d,

      /* Boron */-5, 1.506801d, 1.506801d,

      /* Carbon */-6, 1.787537d, 1.787537d,

      /* Nitrogen */-7, 2.255614d, 2.255614d,

      /* Oxygen */-8, 2.699905d, 2.699905d,

      /* Fluorine */-9, 2.848487d, 2.848487d,

      /* Neon */-10, 5.9998745d, 4.17526d,

      /* Sodium */-11, 0.8213124d, 1.030327d,

      /* Magnesium */-12, 0.9394811d, 1.3103428d,

      /* Aluminum */-13, 1.444161d, 1.444161d,

      /* Silicon */-14, 1.315986d, 1.709943d,

      /* Phosphorus */-15, 2.10872d, 1.78581d,

      /* Sulfur */-16, 2.312962d, 2.009146d,

      /* Chlorine */-17, 3.784645d, 2.036263d,

      /* Argon */-18, 0.9821697d, 5.999715d,

      /* Potassium */-19, 0.7276039d, 0.9871174d,

      /* Calcium */-20, 1.0034161d, 1.3102564d,

      /* Scandium */-21, 1.3951231d, 5.0160943d, 0.9264186d,

      /* Titanium */-22, 0.8961552d, 0.9676159d, 1.8698884d,

      /* Vanadium */-23, 1.2873544d, 1.1744379d, 2.015022d,

      /* Chromium */-24, 2.1495003d, 1.3131074d, 2.3289346d,

      /* Iron */-26, 1.4536275d, 0.8933716d, 1.8691105d,

      /* Cobalt */-27, 0.59975d, 0.607314d, 1.856797d,

      /* Nickel */-28, 0.7735888d, 6.0000132d, 2.7857108d,

      /* Copper */-29, 3.3957872d, 1.786178d, 3.3573266d,

      /* Zinc */-30, 2.047359d, 1.460946d,

      /* Gallium */-31, 0.6986316d, 1.8386933d,

      /* Germanium */-32, 1.29318d, 2.020564d,

      /* Arsenic */-33, 2.5614338d, 1.6117315d,

      /* Selenium */-34, 0.7242956d, 1.9267288d,

      /* Bromine */-35, 3.8543019d, 2.1992091d,

      /* Krypton */-36, 3.5608622d, 1.9832062d,

      /* Rubidium */-37, 4.0001632d, 0.9187408d,

      /* Strontium */-38, 1.3729266d, 1.1118128d,

      /* Zirconium */-40, 1.5386288d, 1.1472515d, 1.8744783d,

      /* Molybdenum */-42, 2.0001083d, 1.4112837d, 2.1944707d,

      /* Palladium */-46, 1.6942397d, 6.0000131d, 2.2314824d,

      /* Silver */-47, 2.6156672d, 1.5209942d, 3.1178537d,

      /* Cadmium */-48, 1.4192491d, 1.0480637d,

      /* Indium */-49, 1.762574d, 1.8648962d,

      /* Tin */-50, 2.08038d, 1.937106d,

      /* Antimony */-51, 3.6458835d, 1.9733156d,

      /* Tellurium */-52, 2.7461609d, 1.6160376d,

      /* Iodine */-53, 2.272961d, 2.169498d,

      /* Xenon */-54, 4.9900791d, 2.6929255d,

      /* Cesium */-55, 6.000417d, 0.8986916d,

      /* Barium */-56, 1.9765973d, 1.3157348d,

      /* Platinum */-78, 1.8655763d, 1.9475781d, 2.8552253d,

      /* Mercury */-80, 2.218184d, 2.065038d,

      /* Thallium */-81, 4.0000447d, 1.8076332d,

      /* Lead */-82, 2.498286d, 2.082071d,

      /* Bismuth */-83, 2.6772255d, 0.6936864d, /* Astatine */-85,
      /* Francium */-87,

      /* Thorium */-90, 1.435306d, 1.435306d, /* Fermium */-100,
      /* Mendelevium */-101,

      /* Nobelium */-102, 4d, 0.3d, 0.3d, /* Lawrencium */-103,
      /* Rutherfordium */-104, /* Dubnium */-105,

  };
  private final static double[] _PM3_C = {

      /* Hydrogen */-1, 0.967807d,

      /* Helium */-2, 1.7710761d, 6.9018258d,

      /* Lithium */-3, 0.65d, 0.75d,

      /* Beryllium */-4, 0.877439d, 1.508755d,

      /* Boron */-5, 1.5312597d, 1.1434597d,

      /* Carbon */-6, 1.565085d, 1.842345d,

      /* Nitrogen */-7, 2.028094d, 2.313728d,

      /* Oxygen */-8, 3.796544d, 2.389402d,

      /* Fluorine */-9, 4.708555d, 2.491178d,

      /* Neon */-10, 5.9998745d, 4.17526d,

      /* Sodium */-11, 2.6618938d, 0.8837425d,

      /* Magnesium */-12, 0.698552d, 1.483453d,

      /* Aluminum */-13, 1.702888d, 1.073629d,

      /* Silicon */-14, 1.635075d, 1.313088d,

      /* Phosphorus */-15, 2.017563d, 1.504732d,

      /* Sulfur */-16, 1.891185d, 1.658972d,

      /* Chlorine */-17, 2.24621d, 2.15101d,

      /* Argon */-18, 0.9821697d, 5.999715d,

      /* Potassium */-19, 0.8101687d, 0.9578342d,

      /* Calcium */-20, 1.2087415d, 0.940937d,

      /* Zinc */-30, 1.819989d, 1.506922d,

      /* Gallium */-31, 1.84704d, 0.839411d,

      /* Germanium */-32, 2.2373526d, 1.5924319d,

      /* Arsenic */-33, 2.636177d, 1.703889d,

      /* Selenium */-34, 2.828051d, 1.732536d,

      /* Bromine */-35, 5.348457d, 2.12759d,

      /* Krypton */-36, 3.5608622d, 1.9832062d,

      /* Rubidium */-37, 4.0000415d, 1.013459d,

      /* Strontium */-38, 1.2794532d, 1.39125d,

      /* Cadmium */-48, 1.679351d, 2.066412d,

      /* Indium */-49, 2.016116d, 1.44535d,

      /* Tin */-50, 2.373328d, 1.638233d,

      /* Antimony */-51, 2.343039d, 1.899992d,

      /* Tellurium */-52, 4.165492d, 1.647555d,

      /* Iodine */-53, 7.001013d, 2.454354d,

      /* Xenon */-54, 4.9900791d, 2.6929255d,

      /* Cesium */-55, 3.5960298d, 0.9255168d,

      /* Barium */-56, 1.9258219d, 1.4519912d,

      /* Mercury */-80, 1.476885d, 2.479951d,

      /* Thallium */-81, 6.867921d, 1.969445d,

      /* Lead */-82, 3.141289d, 1.892418d,

      /* Bismuth */-83, 4.916451d, 1.934935d, /* Francium */-87,

      /* Nobelium */-102, 4d, 0.3d, /* Rutherfordium */-104,

  };
  private final static double[] _PM6_C = {

      /* Hydrogen */-1, 1.268641d,

      /* Helium */-2, 3.313204d, 3.657133d,

      /* Lithium */-3, 0.981041d, 2.953445d,

      /* Beryllium */-4, 1.212539d, 1.276487d,

      /* Boron */-5, 1.634174d, 1.479195d,

      /* Carbon */-6, 2.047558d, 1.702841d,

      /* Nitrogen */-7, 2.380406d, 1.999246d,

      /* Oxygen */-8, 5.421751d, 2.27096d,

      /* Fluorine */-9, 6.043849d, 2.906722d,

      /* Neon */-10, 6.000148d, 3.834528d,

      /* Sodium */-11, 0.686327d, 0.950068d,

      /* Magnesium */-12, 1.31083d, 1.388897d,

      /* Aluminum */-13, 2.364264d, 1.749102d, 1.269384d,

      /* Silicon */-14, 1.752741d, 1.198413d, 2.128593d,

      /* Phosphorus */-15, 2.158033d, 1.805343d, 1.230358d,

      /* Sulfur */-16, 2.192844d, 1.841078d, 3.109401d,

      /* Chlorine */-17, 2.63705d, 2.118146d, 1.324033d,

      /* Argon */-18, 6.000272d, 5.94917d,

      /* Potassium */-19, 6.000478d, 1.127503d,

      /* Calcium */-20, 1.528258d, 2.060094d,

      /* Scandium */-21, 1.402469d, 1.345196d, 1.859012d,

      /* Titanium */-22, 5.324777d, 1.164068d, 1.41828d,

      /* Vanadium */-23, 1.97433d, 1.063106d, 1.394806d,

      /* Chromium */-24, 3.28346d, 1.029394d, 1.623119d,

      /* Manganese */-25, 2.13168d, 1.52588d, 2.6078d,

      /* Iron */-26, 1.47915d, 6.002246d, 1.080747d,

      /* Cobalt */-27, 1.166613d, 3d, 1.860218d,

      /* Nickel */-28, 1.591828d, 2.304739d, 2.514761d,

      /* Copper */-29, 1.669096d, 3d, 2.73499d,

      /* Zinc */-30, 1.512875d, 1.789482d,

      /* Gallium */-31, 2.339067d, 1.729592d,

      /* Germanium */-32, 2.546073d, 1.70913d,

      /* Arsenic */-33, 2.926171d, 1.765191d, 1.392142d,

      /* Selenium */-34, 2.512366d, 2.007576d,

      /* Bromine */-35, 4.670684d, 2.035626d, 1.521031d,

      /* Krypton */-36, 1.312248d, 4.491371d,

      /* Rubidium */-37, 5.510145d, 1.33517d,

      /* Strontium */-38, 2.197303d, 1.730137d,

      /* Yttrium */-39, 0.593368d, 1.490422d, 1.650893d,

      /* Zirconium */-40, 1.69259d, 1.694916d, 1.567392d,

      /* Niobium */-41, 2.355562d, 1.386907d, 1.977324d,

      /* Molybdenum */-42, 1.060429d, 1.350412d, 1.827152d,

      /* Technetium */-43, 1.956245d, 6.006299d, 1.76736d,

      /* Ruthenium */-44, 1.459195d, 5.537201d, 2.093164d,

      /* Rhodium */-45, 1.324919d, 4.306111d, 2.901406d,

      /* Palladium */-46, 1.658503d, 1.156718d, 2.219861d,

      /* Silver */-47, 1.994004d, 0.681817d, 6.007328d,

      /* Cadmium */-48, 1.384108d, 1.957413d,

      /* Indium */-49, 2.023087d, 2.106618d,

      /* Tin */-50, 2.383941d, 2.057908d,

      /* Antimony */-51, 2.391178d, 1.773006d, 2.46559d,

      /* Tellurium */-52, 2.769862d, 1.731319d,

      /* Iodine */-53, 4.498653d, 1.917072d, 1.875175d,

      /* Xenon */-54, 2.759787d, 1.977446d,

      /* Cesium */-55, 5.956008d, 1.619485d,

      /* Barium */-56, 1.395379d, 1.430139d,

      /* Lanthanum */-57, 2.67378d, 1.248192d, 1.688562d,

      /* Lutetium */-71, 5.471741d, 1.712296d, 2.225892d,

      /* Hafnium */-72, 3.085344d, 1.575819d, 1.84084d,

      /* Tantalum */-73, 4.578087d, 4.841244d, 1.838249d,

      /* Tungsten */-74, 2.66456d, 1.62401d, 1.7944d,

      /* Rhenium */-75, 2.411839d, 1.815351d, 2.522766d,

      /* Osmium */-76, 3.031d, 1.59396d, 1.77557d,

      /* Iridium */-77, 1.500907d, 4.106373d, 2.676047d,

      /* Platinum */-78, 2.301264d, 1.662404d, 3.168852d,

      /* Gold */-79, 1.814169d, 1.618657d, 5.053167d,

      /* Mercury */-80, 2.104896d, 1.516293d,

      /* Thallium */-81, 3.335883d, 1.766141d,

      /* Lead */-82, 2.368901d, 1.685246d,

      /* Bismuth */-83, 3.702377d, 1.872327d, /* Astatine */-85,
      /* Francium */-87,

      /* Thorium */-90, 1.435306d, 1.435306d, /* Berkelium */-97,

      /* Californium */-98, 2d, /* Fermium */-100, /* Mendelevium */-101,

      /* Nobelium */-102, 4d, /* Lawrencium */-103, /* Rutherfordium */-104,
      /* Dubnium */-105,

  };
  private final static double[] _PM7_C = {

      /* Hydrogen */-1, 1.260237d,

      /* Helium */-2, 3.313204d, 3.657133d,

      /* Lithium */-3, 0.804974d, 6.02753d,

      /* Beryllium */-4, 1.036199d, 1.764629d,

      /* Boron */-5, 1.560481d, 1.449712d,

      /* Carbon */-6, 1.942244d, 1.708723d,

      /* Nitrogen */-7, 2.354344d, 2.028288d,

      /* Oxygen */-8, 5.972309d, 2.349017d,

      /* Fluorine */-9, 6.07003d, 2.930631d,

      /* Neon */-10, 6.000148d, 3.834528d,

      /* Sodium */-11, 1.666701d, 1.397571d,

      /* Magnesium */-12, 1.170297d, 1.840439d,

      /* Aluminum */-13, 1.232599d, 1.219336d, 1.617502d,

      /* Silicon */-14, 1.433994d, 1.671776d, 1.221915d,

      /* Phosphorus */-15, 2.257933d, 1.555172d, 1.235995d,

      /* Suldur */-16, 2.046153d, 1.807678d, 3.510309d,

      /* Chlorine */-17, 2.223076d, 2.264466d, 0.949994d,

      /* Argon */-18, 6.000272d, 5.94917d,

      /* Potassium */-19, 5.422018d, 1.471023d,

      /* Calcium */-20, 1.477988d, 2.220194d,

      /* Scandium */-21, 1.794897d, 2.174934d, 5.99286d,

      /* Titanium */-22, 1.448579d, 1.940695d, 1.093648d,

      /* Vanadium */-23, 6.051795d, 2.249871d, 1.087345d,

      /* Chromium */-24, 2.838413d, 1.37956d, 1.188729d,

      /* Manganese */-25, 1.66644d, 2.078735d, 2.89707d,

      /* Iron */-26, 1.157576d, 2.737621d, 1.860792d,

      /* Cobalt */-27, 1.789441d, 1.531664d, 1.951497d,

      /* Nickel */-28, 1.70834d, 2.000099d, 5.698724d,

      /* Copper */-29, 1.735325d, 3.219976d, 6.013523d,

      /* Zinc */-30, 1.56014d, 1.915631d,

      /* Gallium */-31, 1.913326d, 1.811217d,

      /* Germanium */-32, 2.762845d, 1.531131d,

      /* Arsenic */-33, 3.21385d, 1.628384d, 3.314358d,

      /* Selenium */-34, 2.75113d, 1.901764d,

      /* Bromine */-35, 3.72548d, 2.242318d, 1.591034d,

      /* Krypton */-36, 1.312248d, 4.491371d,

      /* Rubidium */-37, 1.314831d, 6.015581d,

      /* Strontium */-38, 2.092264d, 3.314082d,

      /* Yttrium */-39, 1.605083d, 2.131069d, 6.021645d,

      /* Zirconium */-40, 1.373517d, 1.141705d, 1.618769d,

      /* Niobium */-41, 2.761686d, 5.999062d, 1.611677d,

      /* Molybdenum */-42, 1.595399d, 1.426575d, 1.787748d,

      /* Technetium */-43, 2.104672d, 2.669984d, 3.030496d,

      /* Ruthenium */-44, 1.605646d, 4.58082d, 1.244578d,

      /* Rhodium */-45, 1.591465d, 4.546046d, 2.685918d,

      /* Palladium */-46, 5.790768d, 2.169788d, 1.327661d,

      /* Silver */-47, 1.793032d, 2.528721d, 3.524808d,

      /* Cadmium */-48, 3.670047d, 1.857036d,

      /* Indium */-49, 1.902085d, 1.940127d,

      /* Tin */-50, 1.959238d, 1.976146d,

      /* Antimony */-51, 1.9986d, 1.887062d, 1.475516d,

      /* Tellurium */-52, 3.024819d, 2.598283d,

      /* Iodine */-53, 3.316202d, 2.449124d, 1.716121d,

      /* Xenon */-54, 3.208788d, 2.727979d,

      /* Cesium */-55, 1.776064d, 6.02531d,

      /* Barium */-56, 1.75049d, 1.968788d,

      /* Lanthanum */-57, 3.398968d, 1.811983d, 1.894574d,

      /* Lutetium */-71, 2.327039d, 6.000335d, 1.208414d,

      /* Hafnium */-72, 2.854938d, 3.079458d, 2.067146d,

      /* Tantalum */-73, 4.116264d, 3.380936d, 1.755408d,

      /* Tungsten */-74, 3.881177d, 2.044717d, 1.928901d,

      /* Rhenium */-75, 2.452162d, 1.583194d, 2.414839d,

      /* Osmium */-76, 3.094808d, 2.845232d, 1.986395d,

      /* Iridium */-77, 1.924564d, 3.510744d, 2.437796d,

      /* Platinum */-78, 2.922551d, 0.725689d, 2.158085d,

      /* Gold */-79, 1.904923d, 2.408005d, 4.377691d,

      /* Mercury */-80, 2.575831d, 1.955505d,

      /* Thallium */-81, 1.903342d, 2.838647d, 5.015677d,

      /* Lead */-82, 4.706006d, 2.591455d,

      /* Bismuth */-83, 5.465413d, 2.037481d, 2.8554d, /* Astatine */-85,
      /* Francium */-87,

      /* Thorium */-90, 1.435306d, 1.435306d, /* Berkelium */-97,

      /* Californium */-98, 2d, /* Fermium */-100, /* Mendelevium */-101,

      /* Nobelium */-102, 4d, /* Lawrencium */-103, /* Rutherfordium */-104,
      /* Dubnium */-105,

  };
  private final static double[] _RM1_C = {

      /* Hydrogen */-1, 1.0826737d,

      /* Carbon */-6, 1.850188d, 1.7683009d,

      /* Nitrogen */-7, 2.3744716d, 1.9781257d,

      /* Oxygen */-8, 3.1793691d, 2.5536191d,

      /* Fluorine */-9, 4.4033791d, 2.6484156d,

      /* Phosphorus */-15, 2.1224012d, 1.7432795d,

      /* Sulfur */-16, 2.1334431d, 1.8746065d,

      /* Chlorine */-17, 3.8649107d, 1.8959314d,

      /* Bromine */-35, 5.7315721d, 2.0314758d,

      /* Iodine */-53, 2.5300375d, 2.3173868d,

      /* Lanthanum */-57, 1.272677d, 1.423276d, 1.410369d,

      /* Cerium */-58, 1.281028d, 1.425366d, 1.412866d,

      /* Praseodymium */-59, 1.538039d, 1.581647d, 1.374904d,

      /* Neodymium */-60, 1.45829d, 1.570516d, 1.513561d,

      /* Promethium */-61, 1.065536d, 1.846925d, 1.424049d,

      /* Samarium */-62, 1.293914d, 1.738656d, 1.521378d,

      /* Europium */-63, 1.350342d, 1.733714d, 1.494122d,

      /* Gadolinium */-64, 1.272776d, 1.908122d, 1.515905d,

      /* Terbium */-65, 1.210052d, 1.921514d, 1.528123d,

      /* Dysprosium */-66, 1.295275d, 1.912107d, 1.413397d,

      /* Holmium */-67, 1.33055d, 1.779559d, 1.536524d,

      /* Erbium */-68, 1.347757d, 1.806481d, 1.466189d,

      /* Thulium */-69, 1.369147d, 1.674365d, 1.714394d,

      /* Ytterbium */-70, 1.239808d, 1.849144d, 1.485378d,

      /* Lutetium */-71, 1.425302d, 1.790353d, 1.642603d,

      /* Nobelium */-102, 4d, 0.3d, };

}
