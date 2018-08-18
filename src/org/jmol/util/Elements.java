/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 10:52:44 -0500 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
 *
 * Copyright (C) 2006  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.util;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.PT;

import org.jmol.c.VDW;
import javajs.util.BS;


public class Elements {

  /**
   * The default elementSymbols. Presumably the only entry which may cause
   * confusion is element 0, whose symbol we have defined as "Xx". 
   */
  public final static String[] elementSymbols = {
    "Xx", // 0
    "H",  // 1
    "He", // 2
    "Li", // 3
    "Be", // 4
    "B",  // 5
    "C",  // 6
    "N",  // 7
    "O",  // 8
    "F",  // 9
    "Ne", // 10
    "Na", // 11
    "Mg", // 12
    "Al", // 13
    "Si", // 14
    "P",  // 15
    "S",  // 16
    "Cl", // 17
    "Ar", // 18
    "K",  // 19
    "Ca", // 20
    "Sc", // 21
    "Ti", // 22
    "V",  // 23
    "Cr", // 24
    "Mn", // 25
    "Fe", // 26
    "Co", // 27
    "Ni", // 28
    "Cu", // 29
    "Zn", // 30
    "Ga", // 31
    "Ge", // 32
    "As", // 33
    "Se", // 34
    "Br", // 35
    "Kr", // 36
    "Rb", // 37
    "Sr", // 38
    "Y",  // 39
    "Zr", // 40
    "Nb", // 41
    "Mo", // 42
    "Tc", // 43
    "Ru", // 44
    "Rh", // 45
    "Pd", // 46
    "Ag", // 47
    "Cd", // 48
    "In", // 49
    "Sn", // 50
    "Sb", // 51
    "Te", // 52
    "I",  // 53
    "Xe", // 54
    "Cs", // 55
    "Ba", // 56
    "La", // 57
    "Ce", // 58
    "Pr", // 59
    "Nd", // 60
    "Pm", // 61
    "Sm", // 62
    "Eu", // 63
    "Gd", // 64
    "Tb", // 65
    "Dy", // 66
    "Ho", // 67
    "Er", // 68
    "Tm", // 69
    "Yb", // 70
    "Lu", // 71
    "Hf", // 72
    "Ta", // 73
    "W",  // 74
    "Re", // 75
    "Os", // 76
    "Ir", // 77
    "Pt", // 78
    "Au", // 79
    "Hg", // 80
    "Tl", // 81
    "Pb", // 82
    "Bi", // 83
    "Po", // 84
    "At", // 85
    "Rn", // 86
    "Fr", // 87
    "Ra", // 88
    "Ac", // 89
    "Th", // 90
    "Pa", // 91
    "U",  // 92
    "Np", // 93
    "Pu", // 94
    "Am", // 95
    "Cm", // 96
    "Bk", // 97
    "Cf", // 98
    "Es", // 99
    "Fm", // 100
    "Md", // 101
    "No", // 102
    "Lr", // 103
    "Rf", // 104
    "Db", // 105
    "Sg", // 106
    "Bh", // 107
    "Hs", // 108
    "Mt", // 109
    /*
    "Ds", // 110
    "Uu",// 111
    "Ub",// 112
    "Ut",// 113
    "Uq",// 114
    "Up",// 115
    "Uh",// 116
    "Us",// 117
    "Uuo",// 118
    */
  };

  // all numbers except radionuclides from:
  // Atomic weights of the elements 2013 (IUPAC Technical Report)
  // Juris Meija, Tyler B. Coplen, Michael Berglund, Willi A. Brand, Paul De Bièvre, 
  // Manfred Gröning, Norman E. Holden, Johanna Irrgeher, Robert D. Loss, Thomas Walczyk, 
  // Thomas Prohaska  Published Online: 2016-02-24 | DOI: https://doi.org/10.1515/pac-2015-0305
  // https://www.degruyter.com/view/j/pac.2016.88.issue-3/pac-2015-0305/pac-2015-0305.xml

   public final static float[] atomicMass = {
    0, 
    /* 1 H */ 1.008f, 4.002f, 
    /* 2 Li */ 6.9675f, 9.012f,      10.8135f, 12.0106f, 14.006f, 15.999f, 18.998f, 20.1797f, 
    /* 3 Na */ 22.989f, 24.307f,     26.981f, 28.084f, 30.973f, 32.059f, 35.4515f, 39.948f, 
    /* 4 K */ 39.0983f, 40.078f, 44.955f, 47.867f, 50.9415f, 51.9961f, 54.938f, 55.845f, 58.933f, 58.6934f, 63.546f, 65.38f, 69.723f, 72.63f, 74.921f, 78.971f, 79.904f, 83.798f, 
    /* 5 Rb */ 85.4678f, 87.62f, 88.905f, 91.224f, 92.906f, 95.95f, 98.91f, 101.07f, 102.905f, 106.42f, 107.8682f, 112.414f, 114.818f, 118.71f, 121.76f, 127.6f, 126.904f, 131.293f, 
    /* 6 Cs, Ba, actinides */132.905f, 137.327f, 138.905f, 140.116f, 140.907f, 144.242f, 144.9f,  150.36f, 151.964f, 157.25f, 158.925f, 162.5f, 164.93f, 167.259f, 168.934f, 173.054f, 174.9668f,
    /* 6 Hf */  178.49f,180.947f,183.84f,186.207f,190.23f,192.217f,195.084f,196.966f,200.592f,204.3835f,207.2f,208.98f,210f, 210f, 222f,    
    /* 7 Fr, Ra, lanthanides */ 223f, 226.03f, 227.03f, 232.0377f, 231.035f, 238.028f, 237.05f, 239.1f, 243.1f, 247.1f, 247.1f, 252.1f, 252.1f, 257.1f, 256.1f, 259.1f, 260.1f, 
    /* 7 Rf - Mt */ 261f, 262f, 263f, 262f, 265f, 268f
    };


   public final static int[] isotopeMass = {
     0, 
     /* 1 H */   1, 4, 
     /* 2 Li */  7, 9,                              11, 12, 14, 16, 19, 20,
     /* 3 Na */ 23,24,                              27, 28, 31, 32, 35, 40,
     /* 4 K */      39, 40,  45,   48,  51,   52,  55,  56,  59,  59,  64,  65,  70,  73,  75,  79,  80,  84,

  /* 5 Rb */   85,  88,  89,  91,  93,  96,  98,  101,  103,  106,  108,  112,  115,  119,  122,  128,  127,  131,
   
  /* 6 Cs, Ba, actinides */   133,  137,  139,  140,  141,  144,  145,  150,  152,  157,  159,  163,  165,  167,  169,  173,  175,
  179,  181,  184,  186,  190,  192,  195,  197,  201,  204,  207,  209,   209,  210,  222,
  
  /* 7 Fr, Ra, lanthanides */   
  223,  226,  227,  232,  231,  238,  237,  244,   243,  247, 247,  251,  252,  257,  258,  259,   260,
   /* 7 Rf - Mt */   261,  262,  263,  262,  265,  268
   };

   public static int getNaturalIsotope(int elementNumber) {
     return isotopeMass[elementNumber & 0x7F];
   }

   public static float getAtomicMass(int i) {
     return (i < 1 || i >= atomicMass.length ? 0 : atomicMass[i]);
   }
   

  /**
   * one larger than the last elementNumber, same as elementSymbols.length
   */
  public final static int elementNumberMax = elementSymbols.length;
  /**
   * @param elementSymbol First char must be upper case, second char accepts upper or lower case
   * @param isSilent
   * @return elementNumber = atomicNumber + IsotopeNumber*128
   */
  public final static int elementNumberFromSymbol(String elementSymbol, boolean isSilent) {
    if (htElementMap == null) {
      Map<String, Integer> map = new Hashtable<String, Integer>();
      for (int elementNumber = elementNumberMax; --elementNumber >= 0;) {
        String symbol = elementSymbols[elementNumber];
        Integer boxed = Integer.valueOf(elementNumber);
        map.put(symbol, boxed);
        if (symbol.length() == 2)
          map.put(symbol.toUpperCase(), boxed);
      }
      for (int i = altElementMax; --i >= firstIsotope;) {
        String symbol = altElementSymbols[i];
        Integer boxed = Integer.valueOf(altElementNumbers[i]);
        map.put(symbol, boxed);
        if (symbol.length() == 2)
          map.put(symbol.toUpperCase(), boxed);
      }
      htElementMap = map;
    }
    if (elementSymbol == null)
      return 0;
    Integer boxedAtomicNumber = htElementMap.get(elementSymbol);
    if (boxedAtomicNumber != null)
      return boxedAtomicNumber.intValue();
    if (PT.isDigit(elementSymbol.charAt(0))) {
      int pt = elementSymbol.length() - 2;
      if (pt >= 0 && PT.isDigit(elementSymbol.charAt(pt)))
        pt++;
      int isotope = (pt > 0 ? PT.parseInt(elementSymbol.substring(0, pt)) : 0);
      if (isotope > 0) {
        int n = elementNumberFromSymbol(elementSymbol.substring(pt), true);
        if (n > 0) {  
          isotope = getAtomicAndIsotopeNumber(n, isotope);
          htElementMap.put(elementSymbol.toUpperCase(), Integer.valueOf(isotope));
          return isotope;
        }        
      }     
    }
    
    if (!isSilent)
      Logger.error("'" + elementSymbol + "' is not a recognized symbol");
    return 0;
  }
  public static Map<String, Integer> htElementMap;
  /**
   * @param elemNo may be atomicNumber + isotopeNumber*128
   * @return elementSymbol
   */
  public final static String elementSymbolFromNumber(int elemNo) {
    //Isotopes as atomicNumber + IsotopeNumber * 128
    int isoNumber = 0;
    if (elemNo >= elementNumberMax) {
      for (int j = altElementMax; --j >= 0;)
        if (elemNo == altElementNumbers[j])
          return altElementSymbols[j];
      isoNumber = getIsotopeNumber(elemNo);
      elemNo %= 128;
      return "" + isoNumber + getElementSymbol(elemNo);
    }
    return getElementSymbol(elemNo);
  }
  private static String getElementSymbol(int elemNo) {
    if (elemNo < 0 || elemNo >= elementNumberMax)
      elemNo = 0;
    return elementSymbols[elemNo];
  }


  public final static String elementNames[] = {
    "unknown",       //  0
    "hydrogen",      //  1
    "helium",        //  2
    "lithium",       //  3
    "beryllium",     //  4
    "boron",         //  5
    "carbon",        //  6
    "nitrogen",      //  7
    "oxygen",        //  8
    "fluorine",      //  9
    "neon",          // 10
    "sodium",        // 11
    "magnesium",     // 12
    "aluminum",      // 13 aluminium
    "silicon",       // 14
    "phosphorus",    // 15
    "sulfur",        // 16 sulphur
    "chlorine",      // 17
    "argon",         // 18
    "potassium",     // 19
    "calcium",       // 20
    "scandium",      // 21
    "titanium",      // 22
    "vanadium",      // 23
    "chromium",      // 24
    "manganese",     // 25
    "iron",          // 26
    "cobalt",        // 27
    "nickel",        // 28
    "copper",        // 29
    "zinc",          // 30
    "gallium",       // 31
    "germanium",     // 32
    "arsenic",       // 33
    "selenium",      // 34
    "bromine",       // 35
    "krypton",       // 36
    "rubidium",      // 37
    "strontium",     // 38
    "yttrium",       // 39
    "zirconium",     // 40
    "niobium",       // 41
    "molybdenum",    // 42
    "technetium",    // 43
    "ruthenium",     // 44
    "rhodium",       // 45
    "palladium",     // 46
    "silver",        // 47
    "cadmium",       // 48
    "indium",        // 49
    "tin",           // 50
    "antimony",      // 51
    "tellurium",     // 52
    "iodine",        // 53
    "xenon",         // 54
    "cesium",        // 55  caesium
    "barium",        // 56
    "lanthanum",     // 57
    "cerium",        // 58
    "praseodymium",  // 59
    "neodymium",     // 60
    "promethium",    // 61
    "samarium",      // 62
    "europium",      // 63
    "gadolinium",    // 64
    "terbium",       // 66
    "dysprosium",    // 66
    "holmium",       // 67
    "erbium",        // 68
    "thulium",       // 69
    "ytterbium",     // 70
    "lutetium",      // 71
    "hafnium",       // 72
    "tantalum",      // 73
    "tungsten",      // 74
    "rhenium",       // 75
    "osmium",        // 76
    "iridium",       // 77
    "platinum",      // 78
    "gold",          // 79
    "mercury",       // 80
    "thallium",      // 81
    "lead",          // 82
    "bismuth",       // 83
    "polonium",      // 84
    "astatine",      // 85
    "radon",         // 86
    "francium",      // 87
    "radium",        // 88
    "actinium",      // 89
    "thorium",       // 90
    "protactinium",  // 91
    "uranium",       // 92
    "neptunium",     // 93
    "plutonium",     // 94
    "americium",     // 95
    "curium",        // 96
    "berkelium",     // 97
    "californium",   // 98
    "einsteinium",   // 99
    "fermium",       // 100
    "mendelevium",   // 101
    "nobelium",      // 102
    "lawrencium",    // 103
    "rutherfordium", // 104
    "dubnium",       // 105
    "seaborgium",    // 106
    "bohrium",       // 107
    "hassium",       // 108
    "meitnerium"     // 109
  };
  
  /**
   * @param elementNumber may be atomicNumber + isotopeNumber*128
   * @return elementName
   */
  public final static String elementNameFromNumber(int elementNumber) {
    //Isotopes as atomicNumber + IsotopeNumber * 128
    if (elementNumber >= elementNumberMax) {
      for (int j = altElementMax; --j >= 0;)
        if (elementNumber == altElementNumbers[j])
          return altElementNames[j];
      elementNumber %= 128;
    }
    if (elementNumber < 0 || elementNumber >= elementNumberMax)
      elementNumber = 0;
    return elementNames[elementNumber];
  }
  
  public final static int elementNumberFromName(String name) {
    for (int i = 1; i < elementNumberMax; i++)
      if (elementNames[i].equalsIgnoreCase(name))
        return i;
    return -1;
  }
  /**
   * @param i index into altElementNames
   * @return elementName
   */
  public final static String altElementNameFromIndex(int i) {
    return altElementNames[i];
  }
  
  /**
   * @param i index into altElementNumbers
   * @return elementNumber (may be atomicNumber + isotopeNumber*128)
   */
  public final static int altElementNumberFromIndex(int i) {
    return altElementNumbers[i];
  }
  
  /** 
   * @param i index into altElementSymbols
   * @return elementSymbol
   */
  public final static String altElementSymbolFromIndex(int i) {
    return altElementSymbols[i];
  }
  
  /**
   * @param i index into altElementSymbols
   * @return 2H
   */
  public final static String altIsotopeSymbolFromIndex(int i) {
    int code = altElementNumbers[i]; 
    return (code >> 7) + elementSymbolFromNumber(code & 127);
  }
  
  /**
   * @param i index into altElementSymbols
   * @return H2
   */
  public final static String altIsotopeSymbolFromIndex2(int i) {
    int code = altElementNumbers[i]; 
    return  elementSymbolFromNumber(code & 127) + (code >> 7);
  }
  
  public final static int getElementNumber(int atomicAndIsotopeNumber) {
    return atomicAndIsotopeNumber & 127;
  }

  public final static int getIsotopeNumber(int atomicAndIsotopeNumber) {
    return atomicAndIsotopeNumber >> 7;
  }

  public final static int getAtomicAndIsotopeNumber(int n, int mass) {
    return ((n < 0 ? 0 : n) + (mass <= 0 ? 0 : mass << 7));
  }
  
  /**
   * @param atomicAndIsotopeNumber (may be atomicNumber + isotopeNumber*128)
   * @return  index into altElementNumbers
   */
  public final static int altElementIndexFromNumber(int atomicAndIsotopeNumber) {
    for (int i = 0; i < altElementMax; i++)
      if (altElementNumbers[i] == atomicAndIsotopeNumber)
        return i;
    return 0;
  }
  

  // add as we go
  private final static String naturalIsotopes = "1H,12C,14N";
  
  public final static boolean isNaturalIsotope(String isotopeSymbol) {
    return (naturalIsotopes.indexOf(isotopeSymbol + ",") >= 0);      
  }
  
  /**
   * first entry of an actual isotope int the altElementSymbols, altElementNames, altElementNumbers arrays
   */
  public final static int firstIsotope = 4;

  private final static int[] altElementNumbers = {
    0,
    13,
    16,
    55,
    (2 << 7) + 1, // D = 2*128 + 1 <-- firstIsotope
    (3 << 7) + 1, // T = 3*128 + 1
    (11 << 7) + 6, // 11C
    (13 << 7) + 6, // 13C
    (14 << 7) + 6, // 14C
    (15 << 7) + 7, // 15N
  };
  
  /**
   * length of the altElementSymbols, altElementNames, altElementNumbers arrays
   */
  public final static int altElementMax = altElementNumbers.length;

  private final static String[] altElementSymbols = {
    "Xx",
    "Al",
    "S",
    "Cs",
    "D",
    "T",
    "11C",
    "13C",
    "14C",
    "15N",
  };
  
  private final static String[] altElementNames = {
    "dummy",
    "aluminium",
    "sulphur",
    "caesium",
    "deuterium",
    "tritium",
    "",
    "",
    "",
    "",
  };

  public final static String VdwPROBE = "#VDW radii for PROBE;{_H}.vdw = 1.0;" +
  "{_H and connected(_C) and not connected(within(smiles,'[a]'))}.vdw = 1.17;" +
  "{_C}.vdw = 1.75;{_C and connected(3) and connected(_O)}.vdw = 1.65;" +
  "{_N}.vdw = 1.55;" +
  "{_O}.vdw = 1.4;" +
  "{_P}.vdw = 1.8;" +
  "{_S}.vdw = 1.8;" +
  "message VDW radii for H, C, N, O, P, and S set according to " +
  "Word, et al., J. Mol. Biol. (1999) 285, 1711-1733"; //MolProbity

  /**
   * Default table of van der Waals Radii.
   * values are stored as MAR -- Milli Angstrom Radius
   * Used for spacefill rendering of atoms.
   * Values taken from OpenBabel.
   * 
   * Note that AUTO_JMOL, AUTO_BABEL, and AUTO_RASMOL are 4, 5, and 6, respectively,
   * so their mod will be JMOL, BABEL, and RASMOL. AUTO is 8, so will default to Jmol
   * 
   * @see <a href="http://openbabel.sourceforge.net">openbabel.sourceforge.net</a>
   * @see <a href="http://jmol.svn.sourceforge.net/viewvc/jmol/trunk/Jmol/src/org/jmol/_documents/vdw_comparison.xls">vdw_comparison.xls</a>
   */
  public final static short[] vanderwaalsMars = {
    // Jmol -- accounts for missing H atoms on PDB files -- source unknown
         // openBabel -- standard values http://openbabel.svn.sourceforge.net/viewvc/openbabel/openbabel/trunk/data/element.txt?revision=3469&view=markup
              // openRasmol -- VERY tight values http://www.openrasmol.org/software/RasMol_2.7.3/src/abstree.h 
                   // filler/reserved
    //Jmol-11.2,
         //OpenBabel-2.2.1,
              //OpenRasmol-2.7.2.1.1,
                   //OpenBabel-2.1.1
    1000,1000,1000,1000, // XX 0
    1200,1100,1100,1200, // H 1
    1400,1400,2200,1400, // He 2
    1820,1810,1220,2200, // Li 3
    1700,1530,628,1900, // Be 4
    2080,1920,1548,1800, // B 5
    1950,1700,1548,1700, // C 6
    1850,1550,1400,1600, // N 7
    1700,1520,1348,1550, // O 8
    1730,1470,1300,1500, // F 9
    1540,1540,2020,1540, // Ne 10
    2270,2270,2200,2400, // Na 11
    1730,1730,1500,2200, // Mg 12
    2050,1840,1500,2100, // Al 13
    2100,2100,2200,2100, // Si 14
    2080,1800,1880,1950, // P 15
    2000,1800,1808,1800, // S 16
    1970,1750,1748,1800, // Cl 17
    1880,1880,2768,1880, // Ar 18
    2750,2750,2388,2800, // K 19
    1973,2310,1948,2400, // Ca 20
    1700,2300,1320,2300, // Sc 21
    1700,2150,1948,2150, // Ti 22
    1700,2050,1060,2050, // V 23
    1700,2050,1128,2050, // Cr 24
    1700,2050,1188,2050, // Mn 25
    1700,2050,1948,2050, // Fe 26
    1700,2000,1128,2000, // Co 27
    1630,2000,1240,2000, // Ni 28
    1400,2000,1148,2000, // Cu 29
    1390,2100,1148,2100, // Zn 30
    1870,1870,1548,2100, // Ga 31
    1700,2110,3996,2100, // Ge 32
    1850,1850,828,2050, // As 33
    1900,1900,900,1900, // Se 34
    2100,1830,1748,1900, // Br 35
    2020,2020,1900,2020, // Kr 36
    1700,3030,2648,2900, // Rb 37
    1700,2490,2020,2550, // Sr 38
    1700,2400,1608,2400, // Y 39
    1700,2300,1420,2300, // Zr 40
    1700,2150,1328,2150, // Nb 41
    1700,2100,1748,2100, // Mo 42
    1700,2050,1800,2050, // Tc 43
    1700,2050,1200,2050, // Ru 44
    1700,2000,1220,2000, // Rh 45
    1630,2050,1440,2050, // Pd 46
    1720,2100,1548,2100, // Ag 47
    1580,2200,1748,2200, // Cd 48
    1930,2200,1448,2200, // In 49
    2170,1930,1668,2250, // Sn 50
    2200,2170,1120,2200, // Sb 51
    2060,2060,1260,2100, // Te 52
    2150,1980,1748,2100, // I 53
    2160,2160,2100,2160, // Xe 54
    1700,3430,3008,3000, // Cs 55
    1700,2680,2408,2700, // Ba 56
    1700,2500,1828,2500, // La 57
    1700,2480,1860,2480, // Ce 58
    1700,2470,1620,2470, // Pr 59
    1700,2450,1788,2450, // Nd 60
    1700,2430,1760,2430, // Pm 61
    1700,2420,1740,2420, // Sm 62
    1700,2400,1960,2400, // Eu 63
    1700,2380,1688,2380, // Gd 64
    1700,2370,1660,2370, // Tb 65
    1700,2350,1628,2350, // Dy 66
    1700,2330,1608,2330, // Ho 67
    1700,2320,1588,2320, // Er 68
    1700,2300,1568,2300, // Tm 69
    1700,2280,1540,2280, // Yb 70
    1700,2270,1528,2270, // Lu 71
    1700,2250,1400,2250, // Hf 72
    1700,2200,1220,2200, // Ta 73
    1700,2100,1260,2100, // W 74
    1700,2050,1300,2050, // Re 75
    1700,2000,1580,2000, // Os 76
    1700,2000,1220,2000, // Ir 77
    1720,2050,1548,2050, // Pt 78
    1660,2100,1448,2100, // Au 79
    1550,2050,1980,2050, // Hg 80
    1960,1960,1708,2200, // Tl 81
    2020,2020,2160,2300, // Pb 82
    1700,2070,1728,2300, // Bi 83
    1700,1970,1208,2000, // Po 84
    1700,2020,1120,2000, // At 85
    1700,2200,2300,2000, // Rn 86
    1700,3480,3240,2000, // Fr 87
    1700,2830,2568,2000, // Ra 88
    1700,2000,2120,2000, // Ac 89
    1700,2400,1840,2400, // Th 90
    1700,2000,1600,2000, // Pa 91
    1860,2300,1748,2300, // U 92
    1700,2000,1708,2000, // Np 93
    1700,2000,1668,2000, // Pu 94
    1700,2000,1660,2000, // Am 95
    1700,2000,1648,2000, // Cm 96
    1700,2000,1640,2000, // Bk 97
    1700,2000,1628,2000, // Cf 98
    1700,2000,1620,2000, // Es 99
    1700,2000,1608,2000, // Fm 100
    1700,2000,1600,2000, // Md 101
    1700,2000,1588,2000, // No 102
    1700,2000,1580,2000, // Lr 103
    1700,2000,1600,2000, // Rf 104
    1700,2000,1600,2000, // Db 105
    1700,2000,1600,2000, // Sg 106
    1700,2000,1600,2000, // Bh 107
    1700,2000,1600,2000, // Hs 108
    1700,2000,1600,2000, // Mt 109
  };

  public final static int RAD_COV_IONIC_OB1_100_1 = 0; // default
  public final static int RAD_COV_BODR_2014_02_22 = 1;
  
  public static int covalentVersion = RAD_COV_BODR_2014_02_22; // starting in Jmol 14.1.11

  public static int bondingVersion = RAD_COV_IONIC_OB1_100_1; // static but not final -- adjusted by Viewer.setIntProperty
  
  /**
   * This method is used by:
   * 
   * (1) the CIF reader to create molecular systems when no bonding information is
   * present
   * 
   * (2) Atom.getBondingRadiusFloat, used by AtomCollection.findMaxRadii and
   * getWorkingRadius, BondCollection.deleteConnections,
   * ModelCollection.autoBond and makeConnections
   * 
   * (3) the MMFF minimizer for unidentified atoms
   * 
   * In terms of bondingVersion, the critical ones are the first two. Changing
   * the version will change the number of bonds created initially, and that would throw
   * off any state script. So we have to do a reset after a state script to 
   * return values to defaults.
   * 
   * @param atomicNumberAndIsotope
   * @param charge
   * @return a bonding radius, either ionic or covalent
   */
  public static float getBondingRadius(int atomicNumberAndIsotope,
                                            int charge) {
    int atomicNumber = atomicNumberAndIsotope & 127;
    return (charge > 0 && bsCations.get(atomicNumber) ? getBondingRadFromTable(
        atomicNumber, charge, cationLookupTable) : charge < 0
        && bsAnions.get(atomicNumber) ? getBondingRadFromTable(atomicNumber,
        charge, anionLookupTable) : defaultBondingMars[(atomicNumber << 1)
        + bondingVersion] / 1000f);
  }

  /**
   * Prior to Jmol 14.1.11, this was OpenBabel 1.100.1, but now it is BODR
   *  
   * @param atomicNumberAndIsotope
   * @return BODR covalent data, generally.
   */
  public static float getCovalentRadius(int atomicNumberAndIsotope) {
    return defaultBondingMars[((atomicNumberAndIsotope & 127) << 1)
        + covalentVersion] / 1000f;

  }
  
  /**
   * Default table of bonding radii stored as a short mar ... milliangstrom
   * radius
   * 
   * Column 1 (default): Values taken from OpenBabel.
   * http://sourceforge.net/p/openbabel
   * /code/485/tree/openbabel/trunk/data/element.txt (dated 10/20/2004)
   * 
   * These values are a mix of common ion (Ba2+, Na+) distances and covalent distances.
   * They are the default for autobonding in Jmol.
   * 
   * Column 2: Blue Obelisk Data Repository (2/22/2014)
   * https://github.com/wadejong/bodr/blob/c7917225cad829507bdd4c8c2fe7ebd3d795c021/bodr/elements/elements.xml 
   * which is from: 
   * 
   * Pyykkö, P. and Atsumi, M. (2009), 
   * Molecular Single-Bond Covalent Radii for Elements 1–118. 
   * Chem. Eur. J., 15: 186–197. doi: 10.1002/chem.200800987
   * 
   * (See also http://en.wikipedia.org/wiki/Covalent_radius)
   *  
   * These are strictly covalent numbers.
   * The user must use "set bondingVersion 1" to set these to be used for autobonding
   * 
   */
  
  public final static short[] defaultBondingMars = {
    0,  0,      //   0  Xx does not bond
    230,  320,  //1 H 39% larger
    930,  460,  //2 He 51% smaller
    680,  1330, //3 Li 95% larger
    350,  1020, //4 Be 191% larger
    830,  850,  //5 B 2% larger
    680,  750,  //6 C 10% larger
    680,  710,  //7 N 4% larger
    680,  630,  //8 O 8% smaller
    640,  640,  //9 F 0% larger
    1120, 670,  //10 Ne 41% smaller
    970,  1550, //11 Na 59% larger
    1100, 1390, //12 Mg 26% larger
    1350, 1260, //13 Al 7% smaller
    1200, 1160, //14 Si 4% smaller
    750,  1110, //15 P 48% larger
    1020, 1030, //16 S 0% larger
    990,  990,  //17 Cl 0% larger
    1570, 960,  //18 Ar 39% smaller
    1330, 1960, //19 K 47% larger
    990,  1710, //20 Ca 72% larger
    1440, 1480, //21 Sc 2% larger
    1470, 1360, //22 Ti 8% smaller
    1330, 1340, //23 V 0% larger
    1350, 1220, //24 Cr 10% smaller
    1350, 1190, //25 Mn 12% smaller
    1340, 1160, //26 Fe 14% smaller
    1330, 1110, //27 Co 17% smaller
    1500, 1100, //28 Ni 27% smaller
    1520, 1120, //29 Cu 27% smaller
    1450, 1180, //30 Zn 19% smaller
    1220, 1240, //31 Ga 1% larger
    1170, 1210, //32 Ge 3% larger
    1210, 1210, //33 As 0% larger
    1220, 1160, //34 Se 5% smaller
    1210, 1140, //35 Br 6% smaller
    1910, 1170, //36 Kr 39% smaller
    1470, 2100, //37 Rb 42% larger
    1120, 1850, //38 Sr 65% larger
    1780, 1630, //39 Y 9% smaller
    1560, 1540, //40 Zr 2% smaller
    1480, 1470, //41 Nb 1% smaller
    1470, 1380, //42 Mo 7% smaller
    1350, 1280, //43 Tc 6% smaller
    1400, 1250, //44 Ru 11% smaller
    1450, 1250, //45 Rh 14% smaller
    1500, 1200, //46 Pd 20% smaller
    1590, 1280, //47 Ag 20% smaller
    1690, 1360, //48 Cd 20% smaller
    1630, 1420, //49 In 13% smaller
    1460, 1400, //50 Sn 5% smaller
    1460, 1400, //51 Sb 5% smaller
    1470, 1360, //52 Te 8% smaller
    1400, 1330, //53 I 5% smaller
    1980, 1310, //54 Xe 34% smaller
    1670, 2320, //55 Cs 38% larger
    1340, 1960, //56 Ba 46% larger
    1870, 1800, //57 La 4% smaller
    1830, 1630, //58 Ce 11% smaller
    1820, 1760, //59 Pr 4% smaller
    1810, 1740, //60 Nd 4% smaller
    1800, 1730, //61 Pm 4% smaller
    1800, 1720, //62 Sm 5% smaller
    1990, 1680, //63 Eu 16% smaller
    1790, 1690, //64 Gd 6% smaller
    1760, 1680, //65 Tb 5% smaller
    1750, 1670, //66 Dy 5% smaller
    1740, 1660, //67 Ho 5% smaller
    1730, 1650, //68 Er 5% smaller
    1720, 1640, //69 Tm 5% smaller
    1940, 1700, //70 Yb 13% smaller
    1720, 1620, //71 Lu 6% smaller
    1570, 1520, //72 Hf 4% smaller
    1430, 1460, //73 Ta 2% larger
    1370, 1370, //74 W 0% larger
    1350, 1310, //75 Re 3% smaller
    1370, 1290, //76 Os 6% smaller
    1320, 1220, //77 Ir 8% smaller
    1500, 1230, //78 Pt 18% smaller
    1500, 1240, //79 Au 18% smaller
    1700, 1330, //80 Hg 22% smaller
    1550, 1440, //81 Tl 8% smaller
    1540, 1440, //82 Pb 7% smaller
    1540, 1510, //83 Bi 2% smaller
    1680, 1450, //84 Po 14% smaller
    1700, 1470, //85 At 14% smaller
    2400, 1420, //86 Rn 41% smaller
    2000, 2230, //87 Fr 11% larger
    1900, 2010, //88 Ra 5% larger
    1880, 1860, //89 Ac 2% smaller
    1790, 1750, //90 Th 3% smaller
    1610, 1690, //91 Pa 4% larger
    1580, 1700, //92 U 7% larger
    1550, 1710, //93 Np 10% larger
    1530, 1720, //94 Pu 12% larger
    1510, 1660, //95 Am 9% larger
    1500, 1660, //96 Cm 10% larger
    1500, 1680, //97 Bk 12% larger
    1500, 1680, //98 Cf 12% larger
    1500, 1650, //99 Es 9% larger
    1500, 1670, //100 Fm 11% larger
    1500, 1730, //101 Md 15% larger
    1500, 1760, //102 No 17% larger
    1500, 1610, //103 Lr 7% larger
    1600, 1570, //104 Rf 2% smaller
    1600, 1490, //105 Db 7% smaller
    1600, 1430, //106 Sg 11% smaller
    1600, 1410, //107 Bh 12% smaller
    1600, 1340, //108 Hs 17% smaller
    1600, 1290, //109 Mt 20% smaller
  }
;

  /****************************************************************
   * ionic radii are looked up using an array of shorts (16 bits each) 
   * that contains the atomic number, the charge, and the radius in two
   * consecutive values, encoded as follows:
   * 
   *   (atomicNumber << 4) + (charge + 4), radiusAngstroms*1000
   * 
   * That is, (atomicNumber * 16 + charge + 4), milliAngstromRadius
   * 
   * This allows for charges from -4 to 11, but we only really have -4 to 7.
   *
   * This data is from
   *  Handbook of Chemistry and Physics. 48th Ed, 1967-8, p. F143
   *  (scanned for Jmol by Phillip Barak, Jan 2004)
   *  
   * Reorganized from two separate arrays 9/2006 by Bob Hanson, who thought
   * it was just too hard to look these up and, if necessary, add or modify.
   * At the same time, the table was split into cations and anions for easier
   * retrieval.
   * 
   * O- and N+ removed 9/2008 - BH. The problem is that
   * the formal charge is used to determine bonding radius. 
   * But these formal charges are different than the charges used in 
   * compilation of HCP data (which is crystal ionic radii). 
   * Specifically, because O- and N+ are very common in organic 
   * compounds, I have removed their radii from the table FOR OUR PURPOSES HERE.
   * 
   * I suppose there are some ionic compounds that have O- and N+ as 
   * isolated ions, but what they would be I have no clue. Better to 
   * be safe and go with somewhat more reasonable values.
   * 
   *  Argh. Changed for Jmol 11.6.RC15
   * 
   *  
   ****************************************************************/
  
  public final static int FORMAL_CHARGE_MIN = -4;
  public final static int FORMAL_CHARGE_MAX = 7;

  private final static short[] cationLookupTable = {
    (3 << 4) + (1 + 4),   680,  // "Li+1"
    (4 << 4) + (1 + 4),   440,  // "Be+1"
    (4 << 4) + (2 + 4),   350,  // "Be+2"
    (5 << 4) + (1 + 4),   350,  // "B+1"
    (5 << 4) + (3 + 4),   230,  // "B+3"
    (6 << 4) + (4 + 4),   160,  // "C+4"
    (7 << 4) + (1 + 4),   680,  // "N+1" // covalent radius --  250 way too small for organic charges
    (7 << 4) + (3 + 4),   160,  // "N+3"
    (7 << 4) + (5 + 4),   130,  // "N+5"
    (8 << 4) + (1 + 4),   220,  // "O+1"
    (8 << 4) + (6 + 4),   90,   // "O+6"
    (9 << 4) + (7 + 4),   80,   // "F+7"
    (10 << 4) + (1 + 4),  1120, // "Ne+1"
    (11 << 4) + (1 + 4),  970,  // "Na+1"
    (12 << 4) + (1 + 4),  820,  // "Mg+1"
    (12 << 4) + (2 + 4),  660,  // "Mg+2"
    (13 << 4) + (3 + 4),  510,  // "Al+3"
    (14 << 4) + (1 + 4),  650,  // "Si+1"
    (14 << 4) + (4 + 4),  420,  // "Si+4"
    (15 << 4) + (3 + 4),  440,  // "P+3"
    (15 << 4) + (5 + 4),  350,  // "P+5"
    (16 << 4) + (2 + 4),  2190, // "S+2"
    (16 << 4) + (4 + 4),  370,  // "S+4"
    (16 << 4) + (6 + 4),  300,  // "S+6"
    (17 << 4) + (5 + 4),  340,  // "Cl+5"
    (17 << 4) + (7 + 4),  270,  // "Cl+7"
    (18 << 4) + (1 + 4),  1540, // "Ar+1"
    (19 << 4) + (1 + 4),  1330, // "K+1"
    (20 << 4) + (1 + 4),  1180, // "Ca+1"
    (20 << 4) + (2 + 4),  990,  // "Ca+2"
    (21 << 4) + (3 + 4),  732,  // "Sc+3"
    (22 << 4) + (1 + 4),  960,  // "Ti+1"
    (22 << 4) + (2 + 4),  940,  // "Ti+2"
    (22 << 4) + (3 + 4),  760,  // "Ti+3"
    (22 << 4) + (4 + 4),  680,  // "Ti+4"
    (23 << 4) + (2 + 4),  880,  // "V+2"
    (23 << 4) + (3 + 4),  740,  // "V+3"
    (23 << 4) + (4 + 4),  630,  // "V+4"
    (23 << 4) + (5 + 4),  590,  // "V+5"
    (24 << 4) + (1 + 4),  810,  // "Cr+1"
    (24 << 4) + (2 + 4),  890,  // "Cr+2"
    (24 << 4) + (3 + 4),  630,  // "Cr+3"
    (24 << 4) + (6 + 4),  520,  // "Cr+6"
    (25 << 4) + (2 + 4),  800,  // "Mn+2"
    (25 << 4) + (3 + 4),  660,  // "Mn+3"
    (25 << 4) + (4 + 4),  600,  // "Mn+4"
    (25 << 4) + (7 + 4),  460,  // "Mn+7"
    (26 << 4) + (2 + 4),  740,  // "Fe+2"
    (26 << 4) + (3 + 4),  640,  // "Fe+3"
    (27 << 4) + (2 + 4),  720,  // "Co+2"
    (27 << 4) + (3 + 4),  630,  // "Co+3"
    (28 << 4) + (2 + 4),  690,  // "Ni+2"
    (29 << 4) + (1 + 4),  960,  // "Cu+1"
    (29 << 4) + (2 + 4),  720,  // "Cu+2"
    (30 << 4) + (1 + 4),  880,  // "Zn+1"
    (30 << 4) + (2 + 4),  740,  // "Zn+2"
    (31 << 4) + (1 + 4),  810,  // "Ga+1"
    (31 << 4) + (3 + 4),  620,  // "Ga+3"
    (32 << 4) + (2 + 4),  730,  // "Ge+2"
    (32 << 4) + (4 + 4),  530,  // "Ge+4"
    (33 << 4) + (3 + 4),  580,  // "As+3"
    (33 << 4) + (5 + 4),  460,  // "As+5"
    (34 << 4) + (1 + 4),  660,  // "Se+1"
    (34 << 4) + (4 + 4),  500,  // "Se+4"
    (34 << 4) + (6 + 4),  420,  // "Se+6"
    (35 << 4) + (5 + 4),  470,  // "Br+5"
    (35 << 4) + (7 + 4),  390,  // "Br+7"
    (37 << 4) + (1 + 4),  1470, // "Rb+1"
    (38 << 4) + (2 + 4),  1120, // "Sr+2"
    (39 << 4) + (3 + 4),  893,  // "Y+3"
    (40 << 4) + (1 + 4),  1090, // "Zr+1"
    (40 << 4) + (4 + 4),  790,  // "Zr+4"
    (41 << 4) + (1 + 4),  1000, // "Nb+1"
    (41 << 4) + (4 + 4),  740,  // "Nb+4"
    (41 << 4) + (5 + 4),  690,  // "Nb+5"
    (42 << 4) + (1 + 4),  930,  // "Mo+1"
    (42 << 4) + (4 + 4),  700,  // "Mo+4"
    (42 << 4) + (6 + 4),  620,  // "Mo+6"
    (43 << 4) + (7 + 4),  979,  // "Tc+7"
    (44 << 4) + (4 + 4),  670,  // "Ru+4"
    (45 << 4) + (3 + 4),  680,  // "Rh+3"
    (46 << 4) + (2 + 4),  800,  // "Pd+2"
    (46 << 4) + (4 + 4),  650,  // "Pd+4"
    (47 << 4) + (1 + 4),  1260, // "Ag+1"
    (47 << 4) + (2 + 4),  890,  // "Ag+2"
    (48 << 4) + (1 + 4),  1140, // "Cd+1"
    (48 << 4) + (2 + 4),  970,  // "Cd+2"
    (49 << 4) + (3 + 4),  810,  // "In+3"
    (50 << 4) + (2 + 4),  930,  // "Sn+2"
    (50 << 4) + (4 + 4),  710,  // "Sn+4"
    (51 << 4) + (3 + 4),  760,  // "Sb+3"
    (51 << 4) + (5 + 4),  620,  // "Sb+5"
    (52 << 4) + (1 + 4),  820,  // "Te+1"
    (52 << 4) + (4 + 4),  700,  // "Te+4"
    (52 << 4) + (6 + 4),  560,  // "Te+6"
    (53 << 4) + (5 + 4),  620,  // "I+5"
    (53 << 4) + (7 + 4),  500,  // "I+7"
    (55 << 4) + (1 + 4),  1670, // "Cs+1"
    (56 << 4) + (1 + 4),  1530, // "Ba+1"
    (56 << 4) + (2 + 4),  1340, // "Ba+2"
    (57 << 4) + (1 + 4),  1390, // "La+1"
    (57 << 4) + (3 + 4),  1016, // "La+3"
    (58 << 4) + (1 + 4),  1270, // "Ce+1"
    (58 << 4) + (3 + 4),  1034, // "Ce+3"
    (58 << 4) + (4 + 4),  920,  // "Ce+4"
    (59 << 4) + (3 + 4),  1013, // "Pr+3"
    (59 << 4) + (4 + 4),  900,  // "Pr+4"
    (60 << 4) + (3 + 4),  995,  // "Nd+3"
    (61 << 4) + (3 + 4),  979,  // "Pm+3"
    (62 << 4) + (3 + 4),  964,  // "Sm+3"
    (63 << 4) + (2 + 4),  1090, // "Eu+2"
    (63 << 4) + (3 + 4),  950,  // "Eu+3"
    (64 << 4) + (3 + 4),  938,  // "Gd+3"
    (65 << 4) + (3 + 4),  923,  // "Tb+3"
    (65 << 4) + (4 + 4),  840,  // "Tb+4"
    (66 << 4) + (3 + 4),  908,  // "Dy+3"
    (67 << 4) + (3 + 4),  894,  // "Ho+3"
    (68 << 4) + (3 + 4),  881,  // "Er+3"
    (69 << 4) + (3 + 4),  870,  // "Tm+3"
    (70 << 4) + (2 + 4),  930,  // "Yb+2"
    (70 << 4) + (3 + 4),  858,  // "Yb+3"
    (71 << 4) + (3 + 4),  850,  // "Lu+3"
    (72 << 4) + (4 + 4),  780,  // "Hf+4"
    (73 << 4) + (5 + 4),  680,  // "Ta+5"
    (74 << 4) + (4 + 4),  700,  // "W+4"
    (74 << 4) + (6 + 4),  620,  // "W+6"
    (75 << 4) + (4 + 4),  720,  // "Re+4"
    (75 << 4) + (7 + 4),  560,  // "Re+7"
    (76 << 4) + (4 + 4),  880,  // "Os+4"
    (76 << 4) + (6 + 4),  690,  // "Os+6"
    (77 << 4) + (4 + 4),  680,  // "Ir+4"
    (78 << 4) + (2 + 4),  800,  // "Pt+2"
    (78 << 4) + (4 + 4),  650,  // "Pt+4"
    (79 << 4) + (1 + 4),  1370, // "Au+1"
    (79 << 4) + (3 + 4),  850,  // "Au+3"
    (80 << 4) + (1 + 4),  1270, // "Hg+1"
    (80 << 4) + (2 + 4),  1100, // "Hg+2"
    (81 << 4) + (1 + 4),  1470, // "Tl+1"
    (81 << 4) + (3 + 4),  950,  // "Tl+3"
    (82 << 4) + (2 + 4),  1200, // "Pb+2"
    (82 << 4) + (4 + 4),  840,  // "Pb+4"
    (83 << 4) + (1 + 4),  980,  // "Bi+1"
    (83 << 4) + (3 + 4),  960,  // "Bi+3"
    (83 << 4) + (5 + 4),  740,  // "Bi+5"
    (84 << 4) + (6 + 4),  670,  // "Po+6"
    (85 << 4) + (7 + 4),  620,  // "At+7"
    (87 << 4) + (1 + 4),  1800, // "Fr+1"
    (88 << 4) + (2 + 4),  1430, // "Ra+2"
    (89 << 4) + (3 + 4),  1180, // "Ac+3"
    (90 << 4) + (4 + 4),  1020, // "Th+4"
    (91 << 4) + (3 + 4),  1130, // "Pa+3"
    (91 << 4) + (4 + 4),  980,  // "Pa+4"
    (91 << 4) + (5 + 4),  890,  // "Pa+5"
    (92 << 4) + (4 + 4),  970,  // "U+4"
    (92 << 4) + (6 + 4),  800,  // "U+6"
    (93 << 4) + (3 + 4),  1100, // "Np+3"
    (93 << 4) + (4 + 4),  950,  // "Np+4"
    (93 << 4) + (7 + 4),  710,  // "Np+7"
    (94 << 4) + (3 + 4),  1080, // "Pu+3"
    (94 << 4) + (4 + 4),  930,  // "Pu+4"
    (95 << 4) + (3 + 4),  1070, // "Am+3"
    (95 << 4) + (4 + 4),  920,  // "Am+4"
  };

  private final static short[] anionLookupTable = {
    (1 << 4) + (-1 + 4),  1540, // "H-1"
    (6 << 4) + (-4 + 4),  2600, // "C-4"
    (7 << 4) + (-3 + 4),  1710, // "N-3"
    (8 << 4) + (-2 + 4),  1360, // "O-2" *Shannon (1976)
    (8 << 4) + (-1 + 4),   680, // "O-1" *necessary for CO2-, NO2, etc.  
    (9 << 4) + (-1 + 4),  1330, // "F-1"
  //(14 << 4) + (-4 + 4), 2710, // "Si-4" *not in 77th
  //(14 << 4) + (-1 + 4), 3840, // "Si-1" *not in 77th 
    (15 << 4) + (-3 + 4), 2120, // "P-3"
    (16 << 4) + (-2 + 4), 1840, // "S-2"
    (17 << 4) + (-1 + 4), 1810, // "Cl-1"
    (32 << 4) + (-4 + 4), 2720, // "Ge-4"
    (33 << 4) + (-3 + 4), 2220, // "As-3"
    (34 << 4) + (-2 + 4), 1980, // "Se-2"  *Shannon (1976)
  //(34 << 4) + (-1 + 4), 2320, // "Se-1" *not in 77th
    (35 << 4) + (-1 + 4), 1960, // "Br-1"
    (50 << 4) + (-4 + 4), 2940, // "Sn-4"
    (50 << 4) + (-1 + 4), 3700, // "Sn-1"
    (51 << 4) + (-3 + 4), 2450, // "Sb-3"
    (52 << 4) + (-2 + 4), 2110, // "Te-2"
    (52 << 4) + (-1 + 4), 2500, // "Te-1"
    (53 << 4) + (-1 + 4), 2200, // "I-1"
  };

  private final static BS bsCations = new BS();
  private final static BS bsAnions = new BS();

  static {
    // OK for J2S compiler because these fields are private
    for (int i = 0; i < anionLookupTable.length; i+=2)
      bsAnions.set(anionLookupTable[i]>>4);
    for (int i = 0; i < cationLookupTable.length; i+=2)
      bsCations.set(cationLookupTable[i]>>4);
  }

  public static float getBondingRadFromTable(int atomicNumber, int charge, short[] table) {
    // when found, return the corresponding value in ionicMars
    // if atom is not found, just return covalent radius
    // if atom is found, but charge is not found, return next lower charge
    int ionic = (atomicNumber << 4) + (charge + 4); 
    int iVal = 0, iMid = 0, iMin = 0, iMax = table.length / 2;
    while (iMin != iMax) {
      iMid = (iMin + iMax) / 2;
      iVal = table[iMid<<1];
      if (iVal > ionic)
        iMax = iMid;
      else if (iVal < ionic)
        iMin = iMid + 1;
      else
        return table[(iMid << 1) + 1] / 1000f;
    }
    // find closest with same element and charge <= this charge
    if (iVal > ionic) 
      iMid--; // overshot
    iVal = table[iMid << 1];
    if (atomicNumber != (iVal >> 4)) 
      iMid++; // must be same element and not a negative charge;
    return table[(iMid << 1) + 1] / 1000f;
  }

  public static int getVanderwaalsMar(int atomicAndIsotopeNumber, VDW type) {
    return vanderwaalsMars[((atomicAndIsotopeNumber & 127) << 2) + (type.pt % 4)];
  }

  public static float getHydrophobicity(int i) {
    return (i < 1 || i >= Elements.hydrophobicities.length ? 0 : Elements.hydrophobicities[i]);
  }

  // source: Bioinformatics explained: Hydrophobicity
  // December 17, 2005
  // CLC bio; Gustav Wieds Vej 10 8000 Aarhus C Denmark;
  // www.clcbio.com info@clcbio.com  
  // http://home.hiroshima-u.ac.jp/kei/IdentityX/picts/BE-hydrophobicity.pdf
  //
  // "Rose scale. The hydrophobicity scale by Rose et al. 
  // is correlated to the average area of buried
  // amino acids in globular proteins [Rose et al., 1985]. 
  // This results in a scale which is not showing the helices 
  // of a protein, but rather the surface accessibility."
  // [Rose et al., 1985] Rose, G. D., Geselowitz, A. R., Lesser, 
  // G. J., Lee, R. H., and Zehfus, M. H. (1985). 
  // Hydrophobicity of amino acid residues in globular proteins. 
  // Science, 229(4716):834-838.

  private final static float[] hydrophobicities = {
                0f,
      /* Ala*/  0.62f,
      /* Arg*/ -2.53f,
      /* Asn*/ -0.78f,
      /* Asp*/ -0.90f,
      /* Cys*/  0.29f,
      /* Gln*/ -0.85f,
      /* Glu*/ -0.74f,
      /* Gly*/  0.48f,
      /* His*/ -0.40f,
      /* Ile*/  1.38f,
      /* Leu*/  1.06f,
      /* Lys*/ -1.50f,
      /* Met*/  0.64f,
      /* Phe*/  1.19f,
      /* Pro*/  0.12f,
      /* Ser*/ -0.18f,
      /* Thr*/ -0.05f,
      /* Trp*/  0.81f,
      /* Tyr*/  0.26f,
      /* Val*/  1.08f
  };


  static {
    // if the length of these tables is all the same then the
    // java compiler should eliminate all of this code.
    if ((elementNames.length != elementNumberMax) ||
        (vanderwaalsMars.length >> 2 != elementNumberMax) ||
        (defaultBondingMars.length >> 1  != elementNumberMax)) {
      Logger.error("ERROR!!! Element table length mismatch:" +
                         "\n elementSymbols.length=" + elementSymbols.length +
                         "\n elementNames.length=" + elementNames.length +
                         "\n vanderwaalsMars.length=" + vanderwaalsMars.length+
                         "\n covalentMars.length=" +
                         defaultBondingMars.length);
    }
  }

  private static float[] electroNegativities = {
    // from http://chemwiki.ucdavis.edu/Physical_Chemistry/Physical_Properties_of_Matter/Atomic_and_Molecular_Properties/Allred-Rochow_Electronegativity
    0,
    2.2f,//H
    0,//He
    0.97f,//Li
    1.47f,//Be
    2.01f,//B
    2.5f,//C
    3.07f,//N
    3.5f,//O
    4.1f,//F
    0f,//
    1.01f,//Na
    1.23f,//Mg
    1.47f,//Al
    1.74f,//Si
    2.06f,//P
    2.44f,//S
    2.83f,//Cl
    0f,//
    0.91f,//K
    1.04f,//Ca
    1.2f,//Sc
    1.32f,//Ti
    1.45f,//V
    1.56f,//Cr
    1.6f,//Mn
    1.64f,//Fe
    1.7f,//Co
    1.75f,//Ni
    1.75f,//Cu
    1.66f,//Zn
    1.82f,//Ga
    2.02f,//Ge
    2.2f,//As
    2.48f,//Se
    2.74f,//Br
    0f,//
    0.89f,//Rb
    0.99f,//Sr
    1.11f,//Y
    1.22f,//Zr
    1.23f,//Nb
    1.3f,//Mo
    1.36f,//Te
    1.42f,//Ru
    1.45f,//Rh
    1.35f,//Pd
    1.42f,//Ag
    1.46f,//Cd
    1.49f,//In
    1.72f,//Sn
    1.82f,//Sb
    2.01f,//Te
    2.21f//I    
  };
  public static float getAllredRochowElectroNeg(int elemno) {
    return (elemno > 0 && elemno < electroNegativities.length ? electroNegativities[elemno] : 0);
  }
  
  public static boolean isElement(int atomicAndIsotopeNumber, int elemNo) {
    return ((atomicAndIsotopeNumber & 127) == elemNo);
  }
  
}
