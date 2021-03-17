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

/**
 * 
 * @author hansonr <hansonr@stolaf.edu>
 */
abstract class MopacSlaterReader extends SlaterReader {

  protected final static float MIN_COEF = 0.0001f; // sufficient?  
  protected int[] atomicNumbers;

  /**
   * GAMESS may need AM1, PMn, or RM1 zeta/coef data
   */
  protected float[][] mopacBasis;
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
    if (ex >= 0 && ey >= 0) {
      // no need for special attention here
      return super.scaleSlater(ex, ey, ez, er, zeta);
    }
    int el = Math.abs(ex + ey + ez);
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

  @Override
  public void setMOData(boolean clearOrbitals) {
    if (!allowNoOrbitals && orbitals.size() == 0)
      return;
    if (mopacBasis == null || !forceMOPAC && gaussians != null && shells != null) {
      if (forceMOPAC) 
        System.out.println("MopacSlaterReader ignoring MOPAC zeta parameters -- using Gaussian contractions");
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
    if (mopacBasis == null ||  slaters != null && slaters.size() > 0)
      return;
    int ac = asc.ac;
    int i0 = asc.getLastAtomSetAtomIndex();
    Atom[] atoms = asc.atoms;
    for (int i = i0; i < ac; ++i) {
      int an = atoms[i].elementNumber;
      createMopacSlaters(i, an, mopacBasis[an], allowMopacDCoef);
    }
  }

  public void createMopacSlaters(int iAtom, int atomicNumber, float[] values, boolean allowD) {
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
                                             float coef) {
    int pt = "S Px Py Pz  Dx2-y2Dxz Dz2 Dyz Dxy".indexOf(type);
    //....... 0 2  5  8   12    18  22  26  30
    switch (pt) {
    case 0: // s
      addSlater(iAtom + 1, 0, 0, 0, getNPQs(atomicNumber) - 1, zeta, coef);
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

  private static Map<String, float[][]> mopacParams;

  public final static String MOPAC_TYPES = "AM1  MNDO PM3  PM6  PM7  RM1";

  /**
   * Retrieve the MOPAC zeta(1/bohr) [s,p,d] array by atom number
   * 
   * @param type
   * @return [[zs,zp,zd],[zs,zp,zd]...] where [1] is for hydrogen, [6] is for
   *         carbon, etc.
   */
  public static float[][] getMopacAtomZetaSPD(String type) {
    if (mopacParams == null)
      mopacParams = new Hashtable<String, float[][]>();
    float[][] params = mopacParams.get(type);
    if (params == null) {
      mopacParams.put(type, params = new float[120][3]);
      float[] data = null;
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
  private static void addData(float[][] params, float[] data) {
    for (int i = 0, p = 0, a = 0; i < data.length; i++) {
      float d = data[i];
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
  final static float[] _AM1_C = {

      /* Hydrogen */-1, 1.188078f,

      /* Helium */-2, 2.1956103f, 6.9012486f,

      /* Lithium */-3, 0.7973487f, 0.9045583f,

      /* Beryllium */-4, 0.7425237f, 0.8080499f,

      /* Carbon */-6, 1.808665f, 1.685116f,

      /* Nitrogen */-7, 2.31541f, 2.15794f,

      /* Oxygen */-8, 3.108032f, 2.524039f,

      /* Fluorine */-9, 3.770082f, 2.49467f,

      /* Neon */-10, 5.998377f, 4.1699304f,

      /* Sodium */-11, 0.789009f, 1.1399864f,

      /* Magnesium */-12, 1.0128928f, 1.1798191f,

      /* Aluminum */-13, 1.516593f, 1.306347f,

      /* Silicon */-14, 1.830697f, 1.284953f,

      /* Phosphorus */-15, 1.98128f, 1.87515f,

      /* Sulfur */-16, 2.366515f, 1.667263f,

      /* Chlorine */-17, 3.631376f, 2.076799f,

      /* Argon */-18, 0.9714216f, 5.9236231f,

      /* Potassium */-19, 1.2660244f, 0.9555939f,

      /* Calcium */-20, 1.1767754f, 1.273852f,

      /* Zinc */-30, 1.954299f, 1.372365f,

      /* Gallium */-31, 4.000216f, 1.3540466f,

      /* Germanium */-32, 1.219631f, 1.982794f,

      /* Arsenic */-33, 2.2576897f, 1.724971f,

      /* Selenium */-34, 2.684157f, 2.0506164f,

      /* Bromine */-35, 3.064133f, 2.038333f,

      /* Krypton */-36, 3.5931632f, 2.0944633f,

      /* Rubidium */-37, 4.0000187f, 1.0140619f,

      /* Strontium */-38, 1.5236848f, 1.5723524f,

      /* Molybdenum */-42, 1.945f, 1.477f, 2.468f,

      /* Indium */-49, 1.8281576f, 1.48475f,

      /* Tin */-50, 1.6182807f, 1.5084984f,

      /* Antimony */-51, 2.254823f, 2.218592f,

      /* Tellurium */-52, 2.1321165f, 1.971268f,

      /* Iodine */-53, 2.102858f, 2.161153f,

      /* Xenon */-54, 4.9675243f, 3.1432142f,

      /* Cesium */-55, 5.7873708f, 1.0311693f,

      /* Barium */-56, 1.9136517f, 1.3948894f,

      /* Mercury */-80, 2.036413f, 1.955766f,

      /* Thallium */-81, 3.8077333f, 1.5511578f,

      /* Lead */-82, 2.4432161f, 1.5506706f,

      /* Bismuth */-83, 4.0007862f, 0.9547714f,

      /* Nobelium */-102, 4f, 0.3f, 0.3f, /* Rutherfordium */-104,

  };
  private final static float[] _MNDO_C = {

      /* Hydrogen */-1, 1.331967f,

      /* Helium */-2, 1.7710761f, 6.9018258f,

      /* Lithium */-3, 0.4296141f, 0.7554884f,

      /* Beryllium */-4, 1.00421f, 1.00421f,

      /* Boron */-5, 1.506801f, 1.506801f,

      /* Carbon */-6, 1.787537f, 1.787537f,

      /* Nitrogen */-7, 2.255614f, 2.255614f,

      /* Oxygen */-8, 2.699905f, 2.699905f,

      /* Fluorine */-9, 2.848487f, 2.848487f,

      /* Neon */-10, 5.9998745f, 4.17526f,

      /* Sodium */-11, 0.8213124f, 1.030327f,

      /* Magnesium */-12, 0.9394811f, 1.3103428f,

      /* Aluminum */-13, 1.444161f, 1.444161f,

      /* Silicon */-14, 1.315986f, 1.709943f,

      /* Phosphorus */-15, 2.10872f, 1.78581f,

      /* Sulfur */-16, 2.312962f, 2.009146f,

      /* Chlorine */-17, 3.784645f, 2.036263f,

      /* Argon */-18, 0.9821697f, 5.999715f,

      /* Potassium */-19, 0.7276039f, 0.9871174f,

      /* Calcium */-20, 1.0034161f, 1.3102564f,

      /* Scandium */-21, 1.3951231f, 5.0160943f, 0.9264186f,

      /* Titanium */-22, 0.8961552f, 0.9676159f, 1.8698884f,

      /* Vanadium */-23, 1.2873544f, 1.1744379f, 2.015022f,

      /* Chromium */-24, 2.1495003f, 1.3131074f, 2.3289346f,

      /* Iron */-26, 1.4536275f, 0.8933716f, 1.8691105f,

      /* Cobalt */-27, 0.59975f, 0.607314f, 1.856797f,

      /* Nickel */-28, 0.7735888f, 6.0000132f, 2.7857108f,

      /* Copper */-29, 3.3957872f, 1.786178f, 3.3573266f,

      /* Zinc */-30, 2.047359f, 1.460946f,

      /* Gallium */-31, 0.6986316f, 1.8386933f,

      /* Germanium */-32, 1.29318f, 2.020564f,

      /* Arsenic */-33, 2.5614338f, 1.6117315f,

      /* Selenium */-34, 0.7242956f, 1.9267288f,

      /* Bromine */-35, 3.8543019f, 2.1992091f,

      /* Krypton */-36, 3.5608622f, 1.9832062f,

      /* Rubidium */-37, 4.0001632f, 0.9187408f,

      /* Strontium */-38, 1.3729266f, 1.1118128f,

      /* Zirconium */-40, 1.5386288f, 1.1472515f, 1.8744783f,

      /* Molybdenum */-42, 2.0001083f, 1.4112837f, 2.1944707f,

      /* Palladium */-46, 1.6942397f, 6.0000131f, 2.2314824f,

      /* Silver */-47, 2.6156672f, 1.5209942f, 3.1178537f,

      /* Cadmium */-48, 1.4192491f, 1.0480637f,

      /* Indium */-49, 1.762574f, 1.8648962f,

      /* Tin */-50, 2.08038f, 1.937106f,

      /* Antimony */-51, 3.6458835f, 1.9733156f,

      /* Tellurium */-52, 2.7461609f, 1.6160376f,

      /* Iodine */-53, 2.272961f, 2.169498f,

      /* Xenon */-54, 4.9900791f, 2.6929255f,

      /* Cesium */-55, 6.000417f, 0.8986916f,

      /* Barium */-56, 1.9765973f, 1.3157348f,

      /* Platinum */-78, 1.8655763f, 1.9475781f, 2.8552253f,

      /* Mercury */-80, 2.218184f, 2.065038f,

      /* Thallium */-81, 4.0000447f, 1.8076332f,

      /* Lead */-82, 2.498286f, 2.082071f,

      /* Bismuth */-83, 2.6772255f, 0.6936864f, /* Astatine */-85,
      /* Francium */-87,

      /* Thorium */-90, 1.435306f, 1.435306f, /* Fermium */-100,
      /* Mendelevium */-101,

      /* Nobelium */-102, 4f, 0.3f, 0.3f, /* Lawrencium */-103,
      /* Rutherfordium */-104, /* Dubnium */-105,

  };
  private final static float[] _PM3_C = {

      /* Hydrogen */-1, 0.967807f,

      /* Helium */-2, 1.7710761f, 6.9018258f,

      /* Lithium */-3, 0.65f, 0.75f,

      /* Beryllium */-4, 0.877439f, 1.508755f,

      /* Boron */-5, 1.5312597f, 1.1434597f,

      /* Carbon */-6, 1.565085f, 1.842345f,

      /* Nitrogen */-7, 2.028094f, 2.313728f,

      /* Oxygen */-8, 3.796544f, 2.389402f,

      /* Fluorine */-9, 4.708555f, 2.491178f,

      /* Neon */-10, 5.9998745f, 4.17526f,

      /* Sodium */-11, 2.6618938f, 0.8837425f,

      /* Magnesium */-12, 0.698552f, 1.483453f,

      /* Aluminum */-13, 1.702888f, 1.073629f,

      /* Silicon */-14, 1.635075f, 1.313088f,

      /* Phosphorus */-15, 2.017563f, 1.504732f,

      /* Sulfur */-16, 1.891185f, 1.658972f,

      /* Chlorine */-17, 2.24621f, 2.15101f,

      /* Argon */-18, 0.9821697f, 5.999715f,

      /* Potassium */-19, 0.8101687f, 0.9578342f,

      /* Calcium */-20, 1.2087415f, 0.940937f,

      /* Zinc */-30, 1.819989f, 1.506922f,

      /* Gallium */-31, 1.84704f, 0.839411f,

      /* Germanium */-32, 2.2373526f, 1.5924319f,

      /* Arsenic */-33, 2.636177f, 1.703889f,

      /* Selenium */-34, 2.828051f, 1.732536f,

      /* Bromine */-35, 5.348457f, 2.12759f,

      /* Krypton */-36, 3.5608622f, 1.9832062f,

      /* Rubidium */-37, 4.0000415f, 1.013459f,

      /* Strontium */-38, 1.2794532f, 1.39125f,

      /* Cadmium */-48, 1.679351f, 2.066412f,

      /* Indium */-49, 2.016116f, 1.44535f,

      /* Tin */-50, 2.373328f, 1.638233f,

      /* Antimony */-51, 2.343039f, 1.899992f,

      /* Tellurium */-52, 4.165492f, 1.647555f,

      /* Iodine */-53, 7.001013f, 2.454354f,

      /* Xenon */-54, 4.9900791f, 2.6929255f,

      /* Cesium */-55, 3.5960298f, 0.9255168f,

      /* Barium */-56, 1.9258219f, 1.4519912f,

      /* Mercury */-80, 1.476885f, 2.479951f,

      /* Thallium */-81, 6.867921f, 1.969445f,

      /* Lead */-82, 3.141289f, 1.892418f,

      /* Bismuth */-83, 4.916451f, 1.934935f, /* Francium */-87,

      /* Nobelium */-102, 4f, 0.3f, /* Rutherfordium */-104,

  };
  private final static float[] _PM6_C = {

      /* Hydrogen */-1, 1.268641f,

      /* Helium */-2, 3.313204f, 3.657133f,

      /* Lithium */-3, 0.981041f, 2.953445f,

      /* Beryllium */-4, 1.212539f, 1.276487f,

      /* Boron */-5, 1.634174f, 1.479195f,

      /* Carbon */-6, 2.047558f, 1.702841f,

      /* Nitrogen */-7, 2.380406f, 1.999246f,

      /* Oxygen */-8, 5.421751f, 2.27096f,

      /* Fluorine */-9, 6.043849f, 2.906722f,

      /* Neon */-10, 6.000148f, 3.834528f,

      /* Sodium */-11, 0.686327f, 0.950068f,

      /* Magnesium */-12, 1.31083f, 1.388897f,

      /* Aluminum */-13, 2.364264f, 1.749102f, 1.269384f,

      /* Silicon */-14, 1.752741f, 1.198413f, 2.128593f,

      /* Phosphorus */-15, 2.158033f, 1.805343f, 1.230358f,

      /* Sulfur */-16, 2.192844f, 1.841078f, 3.109401f,

      /* Chlorine */-17, 2.63705f, 2.118146f, 1.324033f,

      /* Argon */-18, 6.000272f, 5.94917f,

      /* Potassium */-19, 6.000478f, 1.127503f,

      /* Calcium */-20, 1.528258f, 2.060094f,

      /* Scandium */-21, 1.402469f, 1.345196f, 1.859012f,

      /* Titanium */-22, 5.324777f, 1.164068f, 1.41828f,

      /* Vanadium */-23, 1.97433f, 1.063106f, 1.394806f,

      /* Chromium */-24, 3.28346f, 1.029394f, 1.623119f,

      /* Manganese */-25, 2.13168f, 1.52588f, 2.6078f,

      /* Iron */-26, 1.47915f, 6.002246f, 1.080747f,

      /* Cobalt */-27, 1.166613f, 3f, 1.860218f,

      /* Nickel */-28, 1.591828f, 2.304739f, 2.514761f,

      /* Copper */-29, 1.669096f, 3f, 2.73499f,

      /* Zinc */-30, 1.512875f, 1.789482f,

      /* Gallium */-31, 2.339067f, 1.729592f,

      /* Germanium */-32, 2.546073f, 1.70913f,

      /* Arsenic */-33, 2.926171f, 1.765191f, 1.392142f,

      /* Selenium */-34, 2.512366f, 2.007576f,

      /* Bromine */-35, 4.670684f, 2.035626f, 1.521031f,

      /* Krypton */-36, 1.312248f, 4.491371f,

      /* Rubidium */-37, 5.510145f, 1.33517f,

      /* Strontium */-38, 2.197303f, 1.730137f,

      /* Yttrium */-39, 0.593368f, 1.490422f, 1.650893f,

      /* Zirconium */-40, 1.69259f, 1.694916f, 1.567392f,

      /* Niobium */-41, 2.355562f, 1.386907f, 1.977324f,

      /* Molybdenum */-42, 1.060429f, 1.350412f, 1.827152f,

      /* Technetium */-43, 1.956245f, 6.006299f, 1.76736f,

      /* Ruthenium */-44, 1.459195f, 5.537201f, 2.093164f,

      /* Rhodium */-45, 1.324919f, 4.306111f, 2.901406f,

      /* Palladium */-46, 1.658503f, 1.156718f, 2.219861f,

      /* Silver */-47, 1.994004f, 0.681817f, 6.007328f,

      /* Cadmium */-48, 1.384108f, 1.957413f,

      /* Indium */-49, 2.023087f, 2.106618f,

      /* Tin */-50, 2.383941f, 2.057908f,

      /* Antimony */-51, 2.391178f, 1.773006f, 2.46559f,

      /* Tellurium */-52, 2.769862f, 1.731319f,

      /* Iodine */-53, 4.498653f, 1.917072f, 1.875175f,

      /* Xenon */-54, 2.759787f, 1.977446f,

      /* Cesium */-55, 5.956008f, 1.619485f,

      /* Barium */-56, 1.395379f, 1.430139f,

      /* Lanthanum */-57, 2.67378f, 1.248192f, 1.688562f,

      /* Lutetium */-71, 5.471741f, 1.712296f, 2.225892f,

      /* Hafnium */-72, 3.085344f, 1.575819f, 1.84084f,

      /* Tantalum */-73, 4.578087f, 4.841244f, 1.838249f,

      /* Tungsten */-74, 2.66456f, 1.62401f, 1.7944f,

      /* Rhenium */-75, 2.411839f, 1.815351f, 2.522766f,

      /* Osmium */-76, 3.031f, 1.59396f, 1.77557f,

      /* Iridium */-77, 1.500907f, 4.106373f, 2.676047f,

      /* Platinum */-78, 2.301264f, 1.662404f, 3.168852f,

      /* Gold */-79, 1.814169f, 1.618657f, 5.053167f,

      /* Mercury */-80, 2.104896f, 1.516293f,

      /* Thallium */-81, 3.335883f, 1.766141f,

      /* Lead */-82, 2.368901f, 1.685246f,

      /* Bismuth */-83, 3.702377f, 1.872327f, /* Astatine */-85,
      /* Francium */-87,

      /* Thorium */-90, 1.435306f, 1.435306f, /* Berkelium */-97,

      /* Californium */-98, 2f, /* Fermium */-100, /* Mendelevium */-101,

      /* Nobelium */-102, 4f, /* Lawrencium */-103, /* Rutherfordium */-104,
      /* Dubnium */-105,

  };
  private final static float[] _PM7_C = {

      /* Hydrogen */-1, 1.260237f,

      /* Helium */-2, 3.313204f, 3.657133f,

      /* Lithium */-3, 0.804974f, 6.02753f,

      /* Beryllium */-4, 1.036199f, 1.764629f,

      /* Boron */-5, 1.560481f, 1.449712f,

      /* Carbon */-6, 1.942244f, 1.708723f,

      /* Nitrogen */-7, 2.354344f, 2.028288f,

      /* Oxygen */-8, 5.972309f, 2.349017f,

      /* Fluorine */-9, 6.07003f, 2.930631f,

      /* Neon */-10, 6.000148f, 3.834528f,

      /* Sodium */-11, 1.666701f, 1.397571f,

      /* Magnesium */-12, 1.170297f, 1.840439f,

      /* Aluminum */-13, 1.232599f, 1.219336f, 1.617502f,

      /* Silicon */-14, 1.433994f, 1.671776f, 1.221915f,

      /* Phosphorus */-15, 2.257933f, 1.555172f, 1.235995f,

      /* Sulfur */-16, 2.046153f, 1.807678f, 3.510309f,

      /* Chlorine */-17, 2.223076f, 2.264466f, 0.949994f,

      /* Argon */-18, 6.000272f, 5.94917f,

      /* Potassium */-19, 5.422018f, 1.471023f,

      /* Calcium */-20, 1.477988f, 2.220194f,

      /* Scandium */-21, 1.794897f, 2.174934f, 5.99286f,

      /* Titanium */-22, 1.448579f, 1.940695f, 1.093648f,

      /* Vanadium */-23, 6.051795f, 2.249871f, 1.087345f,

      /* Chromium */-24, 2.838413f, 1.37956f, 1.188729f,

      /* Manganese */-25, 1.66644f, 2.078735f, 2.89707f,

      /* Iron */-26, 1.157576f, 2.737621f, 1.860792f,

      /* Cobalt */-27, 1.789441f, 1.531664f, 1.951497f,

      /* Nickel */-28, 1.70834f, 2.000099f, 5.698724f,

      /* Copper */-29, 1.735325f, 3.219976f, 6.013523f,

      /* Zinc */-30, 1.56014f, 1.915631f,

      /* Gallium */-31, 1.913326f, 1.811217f,

      /* Germanium */-32, 2.762845f, 1.531131f,

      /* Arsenic */-33, 3.21385f, 1.628384f, 3.314358f,

      /* Selenium */-34, 2.75113f, 1.901764f,

      /* Bromine */-35, 3.72548f, 2.242318f, 1.591034f,

      /* Krypton */-36, 1.312248f, 4.491371f,

      /* Rubidium */-37, 1.314831f, 6.015581f,

      /* Strontium */-38, 2.092264f, 3.314082f,

      /* Yttrium */-39, 1.605083f, 2.131069f, 6.021645f,

      /* Zirconium */-40, 1.373517f, 1.141705f, 1.618769f,

      /* Niobium */-41, 2.761686f, 5.999062f, 1.611677f,

      /* Molybdenum */-42, 1.595399f, 1.426575f, 1.787748f,

      /* Technetium */-43, 2.104672f, 2.669984f, 3.030496f,

      /* Ruthenium */-44, 1.605646f, 4.58082f, 1.244578f,

      /* Rhodium */-45, 1.591465f, 4.546046f, 2.685918f,

      /* Palladium */-46, 5.790768f, 2.169788f, 1.327661f,

      /* Silver */-47, 1.793032f, 2.528721f, 3.524808f,

      /* Cadmium */-48, 3.670047f, 1.857036f,

      /* Indium */-49, 1.902085f, 1.940127f,

      /* Tin */-50, 1.959238f, 1.976146f,

      /* Antimony */-51, 1.9986f, 1.887062f, 1.475516f,

      /* Tellurium */-52, 3.024819f, 2.598283f,

      /* Iodine */-53, 3.316202f, 2.449124f, 1.716121f,

      /* Xenon */-54, 3.208788f, 2.727979f,

      /* Cesium */-55, 1.776064f, 6.02531f,

      /* Barium */-56, 1.75049f, 1.968788f,

      /* Lanthanum */-57, 3.398968f, 1.811983f, 1.894574f,

      /* Lutetium */-71, 2.327039f, 6.000335f, 1.208414f,

      /* Hafnium */-72, 2.854938f, 3.079458f, 2.067146f,

      /* Tantalum */-73, 4.116264f, 3.380936f, 1.755408f,

      /* Tungsten */-74, 3.881177f, 2.044717f, 1.928901f,

      /* Rhenium */-75, 2.452162f, 1.583194f, 2.414839f,

      /* Osmium */-76, 3.094808f, 2.845232f, 1.986395f,

      /* Iridium */-77, 1.924564f, 3.510744f, 2.437796f,

      /* Platinum */-78, 2.922551f, 0.725689f, 2.158085f,

      /* Gold */-79, 1.904923f, 2.408005f, 4.377691f,

      /* Mercury */-80, 2.575831f, 1.955505f,

      /* Thallium */-81, 1.903342f, 2.838647f, 5.015677f,

      /* Lead */-82, 4.706006f, 2.591455f,

      /* Bismuth */-83, 5.465413f, 2.037481f, 2.8554f, /* Astatine */-85,
      /* Francium */-87,

      /* Thorium */-90, 1.435306f, 1.435306f, /* Berkelium */-97,

      /* Californium */-98, 2f, /* Fermium */-100, /* Mendelevium */-101,

      /* Nobelium */-102, 4f, /* Lawrencium */-103, /* Rutherfordium */-104,
      /* Dubnium */-105,

  };
  private final static float[] _RM1_C = {

      /* Hydrogen */-1, 1.0826737f,

      /* Carbon */-6, 1.850188f, 1.7683009f,

      /* Nitrogen */-7, 2.3744716f, 1.9781257f,

      /* Oxygen */-8, 3.1793691f, 2.5536191f,

      /* Fluorine */-9, 4.4033791f, 2.6484156f,

      /* Phosphorus */-15, 2.1224012f, 1.7432795f,

      /* Sulfur */-16, 2.1334431f, 1.8746065f,

      /* Chlorine */-17, 3.8649107f, 1.8959314f,

      /* Bromine */-35, 5.7315721f, 2.0314758f,

      /* Iodine */-53, 2.5300375f, 2.3173868f,

      /* Lanthanum */-57, 1.272677f, 1.423276f, 1.410369f,

      /* Cerium */-58, 1.281028f, 1.425366f, 1.412866f,

      /* Praseodymium */-59, 1.538039f, 1.581647f, 1.374904f,

      /* Neodymium */-60, 1.45829f, 1.570516f, 1.513561f,

      /* Promethium */-61, 1.065536f, 1.846925f, 1.424049f,

      /* Samarium */-62, 1.293914f, 1.738656f, 1.521378f,

      /* Europium */-63, 1.350342f, 1.733714f, 1.494122f,

      /* Gadolinium */-64, 1.272776f, 1.908122f, 1.515905f,

      /* Terbium */-65, 1.210052f, 1.921514f, 1.528123f,

      /* Dysprosium */-66, 1.295275f, 1.912107f, 1.413397f,

      /* Holmium */-67, 1.33055f, 1.779559f, 1.536524f,

      /* Erbium */-68, 1.347757f, 1.806481f, 1.466189f,

      /* Thulium */-69, 1.369147f, 1.674365f, 1.714394f,

      /* Ytterbium */-70, 1.239808f, 1.849144f, 1.485378f,

      /* Lutetium */-71, 1.425302f, 1.790353f, 1.642603f,

      /* Nobelium */-102, 4f, 0.3f, };

}
