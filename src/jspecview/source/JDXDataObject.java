package jspecview.source;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.util.Logger;

import javajs.util.DF;
import javajs.util.PT;
import jspecview.common.Annotation.AType;
import jspecview.common.Coordinate;
import jspecview.common.Integral;
import jspecview.common.Measurement;
import jspecview.common.Spectrum;
import jspecview.exception.JSVException;

/**
 * spectrum data AS READ FROM FILE
 * 
 * @author Bob Hanson
 * 
 */
public abstract class JDXDataObject extends JDXHeader {

  /**
   * Error number that is returned when a min value is undefined
   */
  public static final double ERROR = Double.MAX_VALUE;

  public static final int REF_TYPE_UNSPECIFIED = -1;
  public static final int REF_TYPE_STANDARD = 0;  
  public static final int REF_TYPE_BRUKER = 1;
  public static final int REF_TYPE_VARIAN = 2;

  public String sourceID = "";
  public boolean isSimulation;
  
  protected double blockID; // a random number generated in JDXFileReader

  private String filePath;
  private String filePathForwardSlash;
  private String inlineData;

  private double fileShiftRef = ERROR; // ##.Shift Reference
  private int fileShiftRefType = REF_TYPE_UNSPECIFIED; // shiftRef = 0, bruker = 1, varian = 2
  private int fileShiftRefDataPt = -1;

  private double minX = Double.NaN, minY = Double.NaN;
  private double maxX = Double.NaN, maxY = Double.NaN;
  private double deltaX = Double.NaN;
  
  /**
   * array of x,y coordinates
   */
  public Coordinate[] xyCoords;
  private boolean continuous;

  /**
   * whether the x values were converted from HZ to PPM
   */
  private boolean isHZtoPPM = false;
  private boolean xIncreases = true;

  // --Required JDX Parameters --
  
  /**
   * JDXReader only
   */
  public double fileFirstX = ERROR;

  /**
   * JDXReader only
   */
  public double fileLastX = ERROR;
  
  
  /**
   * JDXReader only
   */
  public int fileNPoints = -1;

  /**
   * Also used in JDXExport
   */
  public double xFactor = ERROR;

  /**
   * Also used in JDXExport
   */
  public double yFactor = ERROR;
  
  // --Optional Spectral Parameters

  public String nucleusX, nucleusY = "?";  
  public double freq2dX = Double.NaN;
  public double freq2dY = Double.NaN;
  public String y2DUnits = "";
  
  protected Spectrum parent;
  protected String xUnits = "";
  protected String yUnits = "";
  protected String xLabel = null;
  protected String yLabel = null;

  /**
   * from JDXReader ##VARNAME
   */
  String varName = "";
  

  /**
   * for example, ^1H, ^13C
   */
  private String observedNucl = "";
  private double observedFreq = ERROR;
  private int numDim = 1;
  private int nH;
  private double y2D = Double.NaN;

  public void setInlineData(String data) {
    inlineData = data;
    
  }
  
  public String getInlineData() {
    return inlineData;
  }

  public void setFilePath(String filePath) {
  	if (filePath != null)
  		filePathForwardSlash = (this.filePath = filePath.trim()).replace('\\','/');
  }

  /**
   * The path to the file
   * @return path to file or [inline] (if loaded inline)
   */
  public String getFilePath() {
    return filePath;
  }

  public String getFilePathForwardSlash() {
    return filePathForwardSlash;
  }

  public void setBlockID(double id) {
    blockID = id;
  }

  /**
   * for JDXReader only
   * @throws JSVException
   */
  public void checkJDXRequiredTokens() throws JSVException {
    String missingTag = (
        fileFirstX == ERROR ? "##FIRSTX"
        : fileLastX == ERROR ? "##LASTX" 
        : fileNPoints == -1 ? "##NPOINTS"
        : xFactor == ERROR ? "##XFACTOR" 
        : yFactor == ERROR ? "##YFACTOR"
                : null);
    if (missingTag != null)
      throw new JSVException("Error Reading Data Set: " + missingTag + " not found");
  }

  /**
   * Sets the original xfactor, from JDXReader and XMLReader
   * 
   * @param xFactor
   *        the x factor
   */
  public void setXFactor(double xFactor) {
    this.xFactor = xFactor;
  }

  /**
   * Returns the original x factor
   * 
   * @return the original x factor
   */
  public double getXFactor() {
    return xFactor;
  }

  /**
   * Sets the original y factor, from JDXReader and XMLReader
   * 
   * @param yFactor
   *        the y factor
   */
  public void setYFactor(double yFactor) {
    this.yFactor = yFactor;
  }

  /**
   * Returns the original y factor
   * 
   * @return the original y factor
   */
  public double getYFactor() {
    return yFactor;
  }

  /**
   * From ##VARNAME
   * 
   * @param name
   */
  public void setVarName(String name) {
    varName = name;
  }

  public boolean isImaginary() {
    return varName.contains("IMAG");
  }

  /**
   * Sets the units for the x axis
   * 
   * @param xUnits
   *        the x units
   */
  public void setXUnits(String xUnits) {
    this.xUnits = xUnits;
  }

  /**
   * Returns the units for x-axis when spectrum is displayed
   * 
   * @return the units for x-axis when spectrum is displayed
   */
  public String getXUnits() {
    return xUnits;
  }

  /**
   * Sets the units for the y axis
   * 
   * @param yUnits
   *        the y units
   */
  public void setYUnits(String yUnits) {
  	if (yUnits.equals("PPM"))
  		yUnits = "ARBITRARY UNITS"; // EPFL bug
    this.yUnits = yUnits;
  }

  /**
   * Returns the units for y-axis when spectrum is displayed
   * 
   * @return the units for y-axis when spectrum is displayed
   */
  public String getYUnits() {
    return yUnits;
  }

  public void setXLabel(String value) {
    xLabel = value;
  }

  public void setYLabel(String value) {
    yLabel = value;
  }


	public void setObservedNucleus(String value) {
		observedNucl = value;
		if (is1D())
			parent.nucleusX = nucleusX = fixNucleus(value);
	}
	
	/**
	 * ^1H, ^13C, for example
	 * 
	 * @return observedNucl
	 */
	public String getObservedNucleus() {
	  return observedNucl;
	}

  /**
   * Sets the Observed Frequency (for NMR Spectra)
   * 
   * @param observedFreq
   *        the observed frequency
   */
  public void setObservedFreq(double observedFreq) {
    this.observedFreq = observedFreq;
  }

  /**
   * Returns the observed frequency (for NMR Spectra)
   * 
   * @return the observed frequency (for NMR Spectra)
   */
  public double getObservedFreq() {
    return observedFreq;
  }

  public void setHydrogenCount(int nH) {
    this.nH = nH;
  }

  public int getHydrogenCount() {
    return nH;
  }

  public boolean is1D() {
    return numDim == 1;
  }

  public int getNumDim() {
    return numDim;
  }
    
  public void setNumDim(int n) {
    numDim = n;
  }

  // 2D nucleus calc  

  public void setY2D(double d) {
    y2D = d;
  }
  
  public double getY2D() {
    return y2D;
  } 

  public void setY2DUnits(String units) {
    y2DUnits = units;
  }
  
  public double getY2DPPM() {
    double d = y2D;
    if (y2DUnits.equals("HZ"))
      d /= freq2dY;
    return d;
  }

  public void setNucleusAndFreq(String nuc, boolean isX) {
  	nuc = fixNucleus(nuc);
    if (isX)
      nucleusX = nuc;
    else
      nucleusY = nuc;
    double freq;
    if (observedNucl.indexOf(nuc) >= 0) {
      freq = observedFreq;
    } else {
      double g1 = getGyromagneticRatio(fixNucleus(observedNucl));
      double g2 = getGyromagneticRatio(nuc);
      freq = observedFreq * g2 / g1;
    }
    if (isX)
      freq2dX = freq;
    else
      freq2dY = freq;
    Logger.info("Freq for " + nuc + " = " + freq);
  }


  /**
   * Remove nonstandard marks to give simple nucleus 1H not ^1H
   * @param nuc
   * @return "1H", "13C", etc.
   */
  private String fixNucleus(String nuc) {
  	return PT.rep(PT.trim(nuc,"[]^<>"), "NUC_", "");
	}

	/**
   * source: http://www.mathworks.com/matlabcentral/fileexchange/11722-nmr-properties/content/isotopes.m
   * % REFERENCES 
% 1. "Nuclear spins, moments, and other data related to NMR spectroscopy" (9-93)
%    CRC Handbook of Chemistry and Physics, 83th Ed., CRC Press, Boca Raton, FL, 
%    2002. 
% 2. Stone, N. J., <www.nndc.bnl.gov/nndc/stone_moments/>
% 3. http://www.hbcpnetbase.com/
   */
  
  /* '*' indicates
   * from CODATA 2010 Physical Constants Task Group
1H      267.5222005        42.57748060
2H      41.0662791      6.53590131
13C     67.2828400      10.70839657
15N     -27.1261804     -4.31726570
19F     251.8148000        40.07757016
23Na    70.8084930      11.26952167
31P     108.3940000     17.25144090
   */

  private final static double[] gyroData = {
    1, 42.5774806,//*H   Hydrogen  1/2
    2, 6.53590131,//*H   Deuterium 1
    3, 45.4148 ,  // H   Tritium  1/2
    3, 32.436 ,   // He  Helium  1/2
    6, 6.2661 ,   // Li  Lithium 1
    7, 16.5483 ,  // Li  Lithium  3/2
    9, 5.9842 ,   // Be  Beryllium  3/2
    10, 4.5752 ,  // B   Boron 3
    11, 13.663 ,  // B   Boron  3/2
    13, 10.70839657,//*C Carbon  1/2
    14, 3.07770646,//*N  Nitrogen 1
    15, 4.31726570,//*N  Nitrogen  1/2
    17, 5.7742 ,  // O   Oxygen  5/2
    19, 40.07757016,//*F Fluorine  1/2
    21, 3.3631 ,  // Ne  Neon  3/2
    23, 11.26952167,//*Na Sodium  3/2
    25, 2.6083 ,  // Mg  Magnesium  5/2
    27, 11.1031 , // Al  Aluminum  5/2
    29, 8.4655 ,  // Si  Silicon  1/2
    31, 17.25144090,//*P Phosphorus  1/2
    33, 3.2717 ,  // S   Sulfur  3/2
    35, 4.1765 ,  // Cl  Chlorine  3/2
    37, 3.4765 ,  // Cl  Chlorine  3/2
    37, 5.819 ,   // Ar  Argone  3/2
    39, 3.46 ,    // Ar  Argone  7/2
    39, 1.9893 ,  // K   Potassium  3/2
    40, 2.4737 ,  // K   Potassium 4
    41, 1.0919 ,  // K   Potassium  3/2
    43, 2.8688 ,  // Ca  Calcium  7/2
    45, 10.3591 , // Sc  Scandium  7/2
    47, 2.4041 ,  // Ti  Titanium  5/2
    49, 2.4048 ,  // Ti  Titanium  7/2
    50, 4.2505 ,  // V   Vanadium 6
    51, 11.2133 , // V   Vanadium  7/2
    53, 2.4115 ,  // Cr  Chromium  3/2
    55, 10.5763 , // Mn  Manganese  5/2
    57, 1.3816 ,  // Fe  Iron  1/2
    59, 10.077 ,  // Co  Cobalt  7/2
    61, 3.8114 ,  // Ni  Nickel  3/2
    63, 11.2982 , // Cu  Copper  3/2
    65, 12.103 ,  // Cu  Copper  3/2
    67, 2.6694 ,  // Zn  Zinc  5/2
    69, 10.2478 , // Ga  Gallium  3/2
    71, 13.0208 , // Ga  Gallium  3/2
    73, 1.4897 ,  // Ge  Germanium  9/2
    75, 7.315 ,   // As  Arsenic  3/2
    77, 8.1571 ,  // Se  Selenium  1/2
    79, 10.7042 , // Br  Bromine  3/2
    81, 11.5384 , // Br  Bromine  3/2
    83, 1.6442 ,  // Kr  Krypton  9/2
    85, 4.1254 ,  // Rb  Rubidium  5/2
    87, 13.9811 , // Rb  Rubidium  3/2
    87, 1.8525 ,  // Sr  Strontium  9/2
    89, 2.0949 ,  // Y   Yttrium  1/2
    91, 3.9748 ,  // Zr  Zirconium  5/2
    93, 10.4523 , // Nb  Niobium  9/2
    95, 2.7874 ,  // Mo  Molybdenum  5/2
    97, 2.8463 ,  // Mo  Molybdenum  5/2
    99, 9.6294 ,  // Tc  Technetium  9/2
    99, 1.9553 ,  // Ru  Ruthenium  5/2
    101, 2.1916 , // Ru  Ruthenium  5/2
    103, 1.3477 , // Rh  Rhodium  1/2
    105, 1.957 ,  // Pd  Palladium  5/2
    107, 1.7331 , // Ag  Silver  1/2
    109, 1.9924 , // Ag  Silver  1/2
    111, 9.0692 , // Cd  Cadmium  1/2
    113, 9.4871 , // Cd  Cadmium  1/2
    113, 9.3655 , // In  Indium  9/2
    115, 9.3856 , // In  Indium  9/2
    115, 14.0077 ,// Sn  Tin  1/2
    117, 15.261 , // Sn  Tin  1/2
    119, 15.966 , // Sn  Tin  1/2
    121, 10.2551 ,// Sb  Antimony  5/2
    123, 5.5532 , // Sb  Antimony  7/2
    123, 11.2349 ,// Te  Tellurium  1/2
    125, 13.5454 ,// Te  Tellurium  1/2
    127, 8.5778 , // I   Iodine  5/2
    129, 11.8604 ,// Xe  Xenon  1/2
    131, 3.5159 , // Xe  Xenon  3/2
    133, 5.6234 , // Cs  Cesium  7/2
    135, 4.2582 , // Ba  Barium  3/2
    137, 4.7634 , // Ba  Barium  3/2
    138, 5.6615 , // La  Lanthanum 5
    139, 6.0612 , // La  Lanthanum  7/2
    137, 4.88 ,   // Ce  Cerium  3/2
    139, 5.39 ,   // Ce  Cerium  3/2
    141, 2.37 ,   // Ce  Cerium  7/2
    141, 13.0359 ,// Pr  Praseodymium  5/2
    143, 2.319 ,  // Nd  Neodymium  7/2
    145, 1.429 ,  // Nd  Neodymium  7/2
    143, 11.59 ,  // Pm  Promethium  5/2
    147, 5.62 ,   // Pm  Promethium  7/2
    147, 1.7748 , // Sm  Samarium  7/2
    149, 14631 ,  // Sm  Samarium  7/2
    151, 10.5856 ,// Eu  Europium  5/2
    153, 4.6745 , // Eu  Europium  5/2
    155, 1.312 ,  // Gd  Gadolinium  3/2
    157, 1.72 ,   // Gd  Gadolinium  3/2
    159, 10.23 ,  // Tb  Terbium  3/2
    161, 1.4654 , // Dy  Dysprosium  5/2
    163, 2.0508 , // Dy  Dysprosium  5/2
    165, 9.0883 , // Ho  Holmium  7/2
    167, 1.2281 , // Er  Erbium  7/2
    169, 3.531 ,  // Tm  Thulium  1/2
    171, 7.5261 , // Yb  Ytterbium  1/2
    173, 2.073 ,  // Yb  Ytterbium  5/2
    175, 4.8626 , // Lu  Lutetium  7/2
    176, 3.451 ,  // Lu  Lutetium 7
    177, 1.7282 , // Hf  Hafnium  7/2
    179, 1.0856 , // Hf  Hafnium  9/2
    180, 4.087 ,  // Ta  Tantalum 9
    181, 5.1627 , // Ta  Tantalum  7/2
    183, 1.7957 , // W   Tungsten  1/2
    185, 9.7176 , // Re  Rhenium  5/2
    187, 9.817 ,  // Re  Rhenium  5/2
    187, 0.9856 , // Os  Osmium  1/2
    189, 3.3536 , // Os  Osmium  3/2
    191, 0.7658 , // Ir  Iridium  3/2
    191, 0.8319 , // Ir  Iridium  3/2
    195, 9.2922 , // Pt  Platinum  1/2
    197, 0.7406 , // Au  Gold  3/2
    199, 7.7123 , // Hg  Mercury  1/2
    201, 2.8469 , // Hg  Mercury  3/2
    203, 24.7316 ,// Tl  Thallium  1/2
    205, 24.9749 ,// Tl  Thallium  1/2
    207, 9.034 ,  // Pb  Lead  1/2
    209, 6.963 ,  // Bi  Bismuth  9/2
    209, 11.7 ,   // Po  Polonium  1/2
    211, 9.16 ,   // Rn  Radon  1/2
    223, 5.95 ,   // Fr  Francium  3/2
    223, 1.3746 , // Ra  Radium  3/2
    225, 11.187 , // Ra  Radium  1/2
    227, 5.6 ,    // Ac  Actinium  3/2
    229, 1.4 ,    // Th  Thorium  5/2
    231, 10.2 ,   // Pa  Protactinium  3/2
    235, 0.83 ,   // U   Uranium  7/2
    237, 9.57 ,   // Np  Neptunium  5/2
    239, 3.09 ,   // Pu  Plutonium  1/2
    243, 4.6 ,    // Am  Americium  5/2
    1E100
  };
  
  final private static Map<String, Double> gyroMap = new Hashtable<String, Double>();

  static {
    for (int i = 0, n = gyroData.length - 1; i < n; i += 2)
      gyroMap.put("" + (int)gyroData[i], Double.valueOf(gyroData[i + 1]));

//    System.out.println(getGyroMagneticRatio("13C"));
//    System.out.println(getGyroMagneticRatio("1H"));
//    System.out.println(getNominalSpecFreq("13C", 100.612769));
  }

  public static int getNominalSpecFreq(String nuc, double freq) {
    double d = freq * getGyromagneticRatio("1H") / getGyromagneticRatio(nuc);
    int century = (int) Math.round(d / 100) * 100;
    return (Double.isNaN(d) ? -1 : Math.abs(d - century) < 2 ? century : (int) Math.round(d));
  }
  
  public static double getGyromagneticRatio(String nuc) {
    Double v = null;
    try {
      v = gyroMap.get(nuc);
      if (v != null)
        return v.doubleValue();

      int pt = 0;
      while (pt < nuc.length() && Character.isDigit(nuc.charAt(pt)))
        pt++;
      v = gyroMap.get(nuc.substring(0, pt));
      if (v != null)
        gyroMap.put(nuc, v);
    } catch (Exception e) {
      /// ignore
    }
    return (v == null ? Double.NaN : v.doubleValue());
    //    pt = PT.parseInt(nuc.substring(pt));
    //    int i = 0;
    //    for (int n = gyroData.length - 1; i < n; i += 2)
    //      if (gyroData[i] >= pt)
    //        break;
    //    return (gyroData[i] == pt ? gyroData[i + 1] : Double.NaN);
  }
  
  

//////////////// derived info /////////////
  
  public boolean isTransmittance() {
    String s = yUnits.toLowerCase();
    return (s.equals("transmittance") || s.contains("trans") || s.equals("t"));
  }

  public boolean isAbsorbance() {
    String s = yUnits.toLowerCase();
    return (s.equals("absorbance") || s.contains("abs") || s.equals("a"));
  }

  public boolean canSaveAsJDX() {
    return getDataClass().equals("XYDATA");
  }
  
  public boolean canIntegrate() {
    return (continuous && (isHNMR() || isGC()) && is1D());
  }

  public boolean isAutoOverlayFromJmolClick() {
    return (isGC());// || isUVVis());
  }

  public boolean isGC() {
    return dataType.startsWith("GC") || dataType.startsWith("GAS");
  }

  public boolean isMS() {
    return dataType.startsWith("MASS") || dataType.startsWith("MS");
  }

	public boolean isStackable() {
		return !isMS();
	}

	public boolean isScalable() {
		return true;
	}


	public double getYRef() {
		return (!isTransmittance() ? 0.0 : Coordinate.getMaxY(xyCoords, 0, xyCoords.length - 1) < 2 ? 1.0 : 100.0);
	}

	public boolean isInverted() {
		return isTransmittance(); // IR
	}

  public boolean canConvertTransAbs() {
    return (continuous && (yUnits.toLowerCase().contains("abs"))
        || yUnits.toLowerCase().contains("trans"));
  }

  public boolean canShowSolutionColor() {
    return (isContinuous() && canConvertTransAbs()
        && (xUnits.toLowerCase().contains("nanometer") || xUnits.equalsIgnoreCase("nm")) 
        && getFirstX() < 401 && getLastX() > 699 && xyCoords.length >= 30);
  }
  
  /**
   * Determines if the spectrum should be displayed with abscissa unit of Part
   * Per Million (PPM) instead of Hertz (HZ)
   * 
   * @return true if abscissa unit should be PPM
   */
  public boolean isHZtoPPM() {
    return isHZtoPPM;
  }

  /**
   * Sets the value to true if the spectrum should be displayed with abscissa
   * unit of Part Per Million (PPM) instead of Hertz (HZ)
   * 
   * @param val
   *        true or false
   */
  public void setHZtoPPM(boolean val) {
    isHZtoPPM = val;
  }

  /**
   * Sets value to true if spectrum is increasing
   * 
   * @param val
   *        true if spectrum is increasing
   */
  public void setIncreasing(boolean val) {
    xIncreases = val;
  }

  /**
   * Returns true if the spectrum is increasing; used by SVGExporter only
   * 
   * @return true if the spectrum is increasing
   */
  public boolean isXIncreasing() {
    return xIncreases;
  }

  /**
   * Determines if the plot should be displayed decreasing by default
   * 
   * @return true or false
   */
  public boolean shouldDisplayXAxisIncreasing() {
    String dt = dataType.toUpperCase();
    String xu = xUnits.toUpperCase();
    if (dt.contains("NMR") && !dt.contains("FID")) {
      return false;
    } else if (dt.contains("LINK") && xu.contains("CM")) {
      return false; // I think this was because of a bug where BLOCK files kept type as LINK ?      
    } else if (dt.startsWith("IR") || dt.contains("INFRA")
        && xu.contains("CM")) {
      return false;
    } else if (dt.contains("RAMAN") && xu.contains("CM")) {
      return false;
    } else if (dt.contains("VIS") && xu.contains("NANOMETERS")) {
      return true;
    }
    return xIncreases;
  }

  /**
   * Sets value to true if spectrum is continuous
   * 
   * @param val
   *        true if spectrum is continuous
   */
  public void setContinuous(boolean val) {
    continuous = val;
  }

  /**
   * Returns true if spectrum is continuous
   * 
   * @return true if spectrum is continuous
   */
  public boolean isContinuous() {
    return continuous;
  }

  // For AnIML IR/UV files:
  
  // public double pathlength
  
//  /**
//   * Returns the pathlength of the sample (required for AnIML IR/UV files)
//   * 
//   * @return the pathlength
//   */
//  public String getPathlength() {
//    return pathlength;
//  }

  public String[][] getHeaderRowDataAsArray() {
    int n = 8;
    if (observedFreq != ERROR)
      n++;
    if (observedNucl != "")
      n++;
    String[][] rowData = getHeaderRowDataAsArray(true, n);
    int i = rowData.length - n;
    if (observedFreq != ERROR)
      rowData[i++] = new String[] { "##.OBSERVE FREQUENCY", "" + observedFreq };
    if (observedNucl != "")
      rowData[i++] = new String[] { "##.OBSERVE NUCLEUS", observedNucl };
    rowData[i++] = new String[] { "##XUNITS", isHZtoPPM ? "HZ" : xUnits };
    rowData[i++] = new String[] { "##YUNITS", yUnits };
    double x = (xIncreases ? getFirstX() : getLastX());
    rowData[i++] = new String[] { "##FIRSTX",
        String.valueOf(isHZtoPPM() ? x * observedFreq : x) };
    x = (xIncreases ? getLastX() : getFirstX());
    rowData[i++] = new String[] { "##FIRSTY",
        String.valueOf(xIncreases ? getFirstY() : getLastY()) };
    rowData[i++] = new String[] { "##LASTX",
        String.valueOf(isHZtoPPM() ? x * observedFreq : x) };
    rowData[i++] = new String[] { "##XFACTOR", String.valueOf(getXFactor()) };
    rowData[i++] = new String[] { "##YFACTOR", String.valueOf(getYFactor()) };
    rowData[i++] = new String[] { "##NPOINTS",
        String.valueOf(xyCoords.length) };
    return rowData;
  }

	public int getDefaultUnitPrecision() {
		return 2;
	}

	public String setMeasurementText(Measurement m) {
		double dx = m.getValue();
		if (Double.isNaN(dx))
			return "";
		int precision = 1;
		String units = "";
		if (isNMR()) {
			if (is1D()) {
				boolean isIntegral = (m instanceof Integral);
				if (isHNMR() || isIntegral) {
					if (!isIntegral) {
						dx *= observedFreq;
  					units = " Hz";
					}
				} else {
					units = " ppm";
					precision = 2;
				}
			} else {
				return "";
				// 2D?
			}
		}
		return (dx < 0.1 ? "" : DF.formatDecimalDbl(dx, precision) + units);
	}

  public boolean isNMR() {
    return (dataType.toUpperCase().indexOf("NMR") >= 0);
  }
  /**
   * Determines if a spectrum is an HNMR spectrum
   * @return true if an HNMR, false otherwise
   */
  public boolean isHNMR() {
    return (isNMR() && observedNucl.toUpperCase().indexOf("H") >= 0);
  }

  /**
   * Sets the array of coordinates
   * 
   * @param coords
   *        the array of Coordinates
   */
  public void setXYCoords(Coordinate[] coords) {
    xyCoords = coords;
  }

  public JDXDataObject invertYAxis() {
  	for (int i = xyCoords.length; --i >= 0;) {
  		xyCoords[i].setYVal(-xyCoords[i].getYVal());
  	}
  	double d = minY;
  	minY = -maxY;
  	maxY = -d;
  	return this;
  }
  
  /**
   * Returns the first X value
   * 
   * @return the first X value
   */
  public double getFirstX() {
    return xyCoords[0].getXVal();
  }

  /**
   * Returns the first Y value
   * 
   * @return the first Y value
   */
  public double getFirstY() {
    //if(isIncreasing())
    return xyCoords[0].getYVal();
    //else
    //  return xyCoords[getNumberOfPoints() - 1].getYVal();
  }

  /**
   * Returns the last X value
   * 
   * @return the last X value
   */
  public double getLastX() {
    // if(isIncreasing())
    return xyCoords[xyCoords.length - 1].getXVal();
    // else
    //   return xyCoords[0].getXVal();
  }

  /**
   * Returns the last Y value
   * 
   * @return the last Y value
   */
  public double getLastY() {
    return xyCoords[xyCoords.length - 1].getYVal();
  }

  /**
   * Calculates and returns the minimum x value in the list of coordinates
   * Fairly expensive operation
   * 
   * @return the minimum x value in the list of coordinates
   */
  public double getMinX() {
    return (Double.isNaN(minX) ? (minX = Coordinate.getMinX(xyCoords, 0, xyCoords.length - 1)) : minX);
  }

  /**
   * Calculates and returns the minimum y value in the list of coordinates
   * Fairly expensive operation
   * 
   * @return the minimum x value in the list of coordinates
   */
  public double getMinY() {
    return (Double.isNaN(minY) ? (minY = Coordinate.getMinY(xyCoords, 0, xyCoords.length - 1)) : minY);
  }

  /**
   * Calculates and returns the maximum x value in the list of coordinates
   * Fairly expensive operation
   * 
   * @return the maximum x value in the list of coordinates
   */
  public double getMaxX() {
    return (Double.isNaN(maxX) ? (maxX = Coordinate.getMaxX(xyCoords, 0, xyCoords.length - 1)) : maxX);
  }

  /**
   * Calculates and returns the maximum y value in the list of coordinates
   * Fairly expensive operation
   * 
   * @return the maximum y value in the list of coordinates
   */
  public double getMaxY() {
    return (Double.isNaN(maxY) ? (maxY = Coordinate.getMaxY(xyCoords, 0, xyCoords.length - 1)) : maxY);
  }

	public void normalizeSimulation(double max) {
		// for simulations
		if (!isNMR() || !is1D())
			return;
		double f = max / getMaxY();
		maxY = Double.NaN;
		Coordinate.applyScale(xyCoords, 1, f);
		Logger.info("Y values have been scaled by a factor of " + f);		
	}

  /**
   * Returns the delta X
   * 
   * @return the delta X
   */
  public double getDeltaX() {
    return (Double.isNaN(deltaX) ? (deltaX = Coordinate.deltaX(getLastX(), getFirstX(), xyCoords.length)) : deltaX);
  }

  public void copyTo(JDXDataObject newObj) {
    newObj.setTitle(title);
    newObj.setJcampdx(jcampdx);
    newObj.setOrigin(origin);
    newObj.setOwner(owner);
    newObj.setDataClass(dataClass);
    newObj.setDataType(dataType);
    newObj.setHeaderTable(headerTable);

    newObj.setXFactor(xFactor);
    newObj.setYFactor(yFactor);
    newObj.setXUnits(xUnits);
    newObj.setYUnits(yUnits);
    newObj.setXLabel(xLabel);
    newObj.setYLabel(yLabel);

    //newSpectrum.setPathlength(getPathlength());
    newObj.setXYCoords(xyCoords);
    newObj.setContinuous(continuous);
    newObj.setIncreasing(xIncreases);

    newObj.observedFreq = observedFreq;
    newObj.observedNucl = observedNucl;
    newObj.fileShiftRef = fileShiftRef;
    newObj.fileShiftRefDataPt = fileShiftRefDataPt;
    newObj.fileShiftRefType = fileShiftRefType;
    newObj.isHZtoPPM = isHZtoPPM;
    newObj.numDim = numDim;
    newObj.nucleusX = nucleusX;
    newObj.nucleusY = nucleusY;
    newObj.freq2dX = freq2dX;
    newObj.freq2dY = freq2dY;
    newObj.setFilePath(filePath);
    newObj.nH = nH;

  }

  public String getTypeLabel() {
    return (isNMR() ? nucleusX + "NMR" : dataType); 	
  }

  public Object[] getDefaultAnnotationInfo(AType type) {
		String[] s1;
		int[] s2;
		boolean isNMR = isNMR();
		switch (type) {
		case Integration:
			return new Object[] { null, new int[] { 1 }, null };
		case Measurements:
			s1 = (isNMR ? new String[] { "Hz", "ppm" } : new String[] {""});
			s2 = (isHNMR() ? new int[] { 1, 4 }
			: new int[] { 1, 3 });
			return new Object[] { s1, s2, Integer.valueOf(0) };
		case PeakList:
			s1 = (isNMR ? new String[] { "Hz", "ppm" } : new String[] {""} );
			s2 = (isHNMR() ? new int[] { 1, 2 } : new int[] { 1, 1 });
			return new Object[] { s1, s2, Integer.valueOf(isNMR ? 1 : 0) };
		case NONE:
		case OverlayLegend:
			break;
		case Views:
			break;
		}
		return null;
	}

	public double[] getPeakListArray(Measurement m, double[]last, double maxY) {
		double x = m.getXVal();
		double y = m.getYVal();
		if (isNMR())
			y /= maxY;
		double dx = Math.abs(x - last[0]);
		last[0] = x;
		double ddx = dx + last[1];
		last[1] = dx;
		double dddx = ddx + last[2];
		last[2] = ddx;
		if (isNMR()) {
			return new double[] {x, y, x * observedFreq, (dx * observedFreq > 20 ? 0 : dx * observedFreq)
			, (ddx * observedFreq > 20 ? 0 : ddx * observedFreq)
			, (dddx * observedFreq > 20 ? 0 : dddx * observedFreq)}; 			
		}
		return new double[] { x, y };
	}

	/**
	 * Called by JDXReader immediately after decompression.
	 * 
	 */
  void finalizeCoordinates() {
    double freq = (Double.isNaN(freq2dX) ? observedFreq
        : freq2dX);
    // apply offset
    boolean isHz = (freq != ERROR
        && getXUnits().toUpperCase().equals("HZ"));
    if (fileShiftRef != ERROR && freq != ERROR
        && dataType.toUpperCase().contains("SPECTRUM")
        && jcampdx.indexOf("JEOL") < 0 // BH 2020.09.16 J Muzyka Centre College
    ) {
      applyShiftReference(isHz ? freq : 1, fileShiftRef);
    }
    if (fileFirstX > fileLastX)
      Coordinate.reverse(xyCoords);
    if (isHz) {
      Coordinate.applyScale(xyCoords, (1.0 / freq), 1);
      setXUnits("PPM");
      setHZtoPPM(true);
    }
  }

  /**
   * Set the shift, pt, and type for applyShiftReference() from processing
   * ##.SHIFTREFERENCE, ##$REFERENCEPOINT, or ##OFFSET
   * 
   * @param shift
   * @param pt
   * @param type
   */
  void setShiftReference(double shift, int pt, int type) {
    fileShiftRef = shift;
    fileShiftRefDataPt = (pt > 0 ? pt : 1);
    fileShiftRefType = type;    
  }
  
  boolean isShiftTypeSpecified() {
    return  (fileShiftRefType != JDXDataObject.REF_TYPE_UNSPECIFIED);
  }

  /**
   * Applies the shift reference to all coordinates. Used by JDXReader only.
   * 
   * @param referenceFreq
   *        the observed frequency
   * @param shift 
   */
  private void applyShiftReference(double referenceFreq, double shift) {
  
    if (fileShiftRefDataPt > xyCoords.length || fileShiftRefDataPt < 0)
      return;
  
    Coordinate coord;
    switch (fileShiftRefType) {
    case REF_TYPE_STANDARD:
      shift = xyCoords[fileShiftRefDataPt - 1].getXVal() - shift * referenceFreq;
      break;
    case REF_TYPE_BRUKER:
      shift = fileFirstX - shift * referenceFreq;
      break;
    case REF_TYPE_VARIAN:
      shift = fileLastX + shift;
      break;
    }
  
    for (int index = 0; index < xyCoords.length; index++) {
      coord = xyCoords[index];
      coord.setXVal(coord.getXVal() - shift);
      xyCoords[index] = coord;
    }
  
  }


}
