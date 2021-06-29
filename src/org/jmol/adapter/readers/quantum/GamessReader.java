/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-16 14:11:08 -0500 (Sat, 16 Sep 2006) $
 * $Revision: 5569 $
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

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;

import java.util.Hashtable;

import java.util.Map;

import org.jmol.adapter.smarter.Atom;
import org.jmol.util.Logger;

abstract public class GamessReader extends MopacSlaterReader {

  protected Lst<String> atomNames;

  abstract protected void readAtomsInBohrCoordinates() throws Exception;  
 
  @Override
  protected void initializeReader() throws Exception {
    allowMopacDCoef = false;
    super.initializeReader();
  }

  protected void setAtom(Atom atom, int atomicNumber, String name, String id) {
    atom.elementNumber = (short) atomicNumber;
    atom.atomName = name;
    atomNames.addLast(id == null ? name : id);
  }

  protected void readEnergy() {
    //  ... ENERGY IS   or    ... ENERGY = 
    String searchTerm = "ENERGY";
    int energyToken = 2;
    String energyType = "ENERGY";
    if (line.indexOf("E(MP2)") > 0) {
      searchTerm = "E(MP2)=";
      energyType = "MP2";
      // this one has the equals sign with no space so it's not picked up as a token
      energyToken = 1;
    }
    else if (line.indexOf("E(CCSD)") > 0) {
      searchTerm = "E(CCSD)";
      energyType = "CCSD";
      energyToken = 2;
    }
    else if (line.indexOf("E(   CCSD(T))") > 0) {
      // the spaces in E(   CCSD cause an extra token)
      searchTerm = "E(   CCSD(T))";
      energyType = "CCSD(T)";
      energyToken = 3;
    }
    String[] tokens = PT.getTokens(line.substring(line.indexOf(searchTerm)));
    if (tokens.length < energyToken + 1)
      return;
    String strEnergy = tokens[energyToken];
    float e = parseFloatStr(strEnergy);
    if (!Float.isNaN(e)) {
      asc.setAtomSetEnergy(strEnergy, e);
      asc.setCurrentModelInfo("EnergyType", energyType);
      if (!energyType.equals("ENERGY"))
        appendLoadNote("GamessReader Energy type " + energyType);
    }
  }
  
  protected void readGaussianBasis(String initiator, String terminator) throws Exception {
    Lst<String[]> gdata = new  Lst<String[]>();
    gaussianCount = 0;
    int nGaussians = 0;
    shellCount = 0;
    String thisShell = "0";
    String[] tokens;
    discardLinesUntilContains(initiator);
    rd();
    int[] slater = null;
    Map<String, Lst<int[]>> shellsByAtomType = new Hashtable<String, Lst<int[]>>();
    Lst<int[]> slatersByAtomType = new  Lst<int[]>();
    String atomType = null;
    
    while (rd() != null && line.indexOf(terminator) < 0) {
      //System.out.println(line);
      if (line.indexOf("(") >= 0)
        line = GamessReader.fixBasisLine(line);
      tokens = getTokens();
      switch (tokens.length) {
      case 1:
        if (atomType != null) {
          if (slater != null) {
            slater[2] = nGaussians;
            slatersByAtomType.addLast(slater);
            slater = null;
          }
          shellsByAtomType.put(atomType, slatersByAtomType);
        }
        slatersByAtomType = new  Lst<int[]>();
        atomType = tokens[0];
        break;
      case 0:
        break;
      default:
        if (!tokens[0].equals(thisShell)) {
          if (slater != null) {
            slater[2] = nGaussians;
            slatersByAtomType.addLast(slater);
          }
          thisShell = tokens[0];
          shellCount++;
          slater = new int[] {
              BasisFunctionReader.getQuantumShellTagID(fixShellTag(tokens[1])), gaussianCount,
              0 };
          nGaussians = 0;
        }
        ++nGaussians;
        ++gaussianCount;
        gdata.addLast(tokens);
      }
    }
    if (slater != null) {
      slater[2] = nGaussians;
      slatersByAtomType.addLast(slater);
    }
    if (atomType != null)
      shellsByAtomType.put(atomType, slatersByAtomType);
    gaussians = AU.newFloat2(gaussianCount);
    for (int i = 0; i < gaussianCount; i++) {
      tokens = gdata.get(i);
      gaussians[i] = new float[tokens.length - 3];
      for (int j = 3; j < tokens.length; j++)
        gaussians[i][j - 3] = parseFloatStr(tokens[j]);
    }
    int ac = atomNames.size();
    if (shells == null && ac > 0) {
      shells = new  Lst<int[]>();
      for (int i = 0; i < ac; i++) {
        atomType = atomNames.get(i);
        Lst<int[]> slaters = shellsByAtomType.get(atomType);
        if (slaters == null) {
          Logger.error("slater for atom " + i + " atomType " + atomType
              + " was not found in listing. Ignoring molecular orbitals");
          return;
        }
        for (int j = 0; j < slaters.size(); j++) {
          slater = slaters.get(j);
          shells.addLast(new int[] { i + 1, slater[0], slater[1] + 1, slater[2] });
        }
      }
    }

    if (debugging) {
      Logger.debug(shellCount + " slater shells read");
      Logger.debug(gaussianCount + " gaussian primitives read");
    }
  }

  abstract protected String fixShellTag(String tag);

  protected void readFrequencies() throws Exception {
    //not for GamessUK yet
    // For the case when HSSEND=.TRUE. atoms[]
    // now contains all atoms across all models (optimization steps).
    // We only want to set vetor data corresponding to new cloned
    // models and not interfere with the previous ones.
    discardLinesUntilContains("FREQUENCY:");
    boolean haveFreq = false;
    while (line != null && line.indexOf("FREQUENCY:") >= 0) {
      int frequencyCount = 0;
      String[] tokens = getTokens();
      float[] frequencies = new float[tokens.length];
      for (int i = 0; i < tokens.length; i++) {
        float frequency = parseFloatStr(tokens[i]);
        if (tokens[i].equals("I"))
          frequencies[frequencyCount - 1] = -frequencies[frequencyCount - 1];
        if (Float.isNaN(frequency))
          continue; // may be "I" for imaginary
        frequencies[frequencyCount++] = frequency;
        if (debugging) {
          Logger.debug((vibrationNumber + 1) + " frequency=" + frequency);
        }
      }
      String[] red_masses = null;
      String[] intensities = null;
      rd();
      if (line.indexOf("MASS") >= 0) {
        red_masses = getTokens();
        rd();
      }
      if (line.indexOf("INTENS") >= 0) {
        intensities = getTokens();
      }
      int ac = asc.getLastAtomSetAtomCount();
      int iAtom0 = asc.ac;
      boolean[] ignore = new boolean[frequencyCount];
      for (int i = 0; i < frequencyCount; i++) {
        ignore[i] = !doGetVibration(++vibrationNumber);
        // The last model should be cloned because we might
        // have done an optimization with HSSEND=.TRUE.
        if (ignore[i])
          continue;
        if (haveFreq) {
          asc.cloneLastAtomSet();
        } else {
          haveFreq = true;
          iAtom0 -= ac;
        }
        asc.setAtomSetFrequency(vibrationNumber, null, null, "" + frequencies[i], null);
        if (red_masses != null)
          asc.setAtomSetModelProperty("ReducedMass",
              red_masses[red_masses.length - frequencyCount + i] + " AMU");
        if (intensities != null)
          asc.setAtomSetModelProperty("IRIntensity",
              intensities[intensities.length - frequencyCount + i]
                  + " D^2/AMU-Angstrom^2");

      }
      discardLinesUntilBlank();
      fillFrequencyData(iAtom0, ac, ac, ignore, false, 20, 12, null, 0, null);
      readLines(13);
    }
  }

  protected static String fixBasisLine(String line) {
    int pt, pt1;
    line = line.replace(')', ' ');
    while ((pt = line.indexOf("(")) >= 0) {
      pt1 = pt;
      while (line.charAt(--pt1) == ' '){}
      while (line.charAt(--pt1) != ' '){}
      line = line.substring(0, ++pt1) + line.substring(pt + 1);
    }
    return line;
  }
  
  /*
  BASIS OPTIONS
  -------------
  GBASIS=N311         IGAUSS=       6      POLAR=DUNNING 
  NDFUNC=       3     NFFUNC=       1     DIFFSP=       T
  NPFUNC=       3      DIFFS=       T
  SPLIT3=     4.00000000     1.00000000     0.25000000


  $CONTRL OPTIONS
  ---------------
SCFTYP=UHF          RUNTYP=OPTIMIZE     EXETYP=RUN     
MPLEVL=       2     CITYP =NONE         CCTYP =NONE         VBTYP =NONE    
DFTTYP=NONE         TDDFT =NONE    
MULT  =       3     ICHARG=       0     NZVAR =       0     COORD =UNIQUE  
PP    =NONE         RELWFN=NONE         LOCAL =NONE         NUMGRD=       F
ISPHER=       1     NOSYM =       0     MAXIT =      30     UNITS =ANGS    
PLTORB=       F     MOLPLT=       F     AIMPAC=       F     FRIEND=        
NPRINT=       7     IREST =       0     GEOM  =INPUT   
NORMF =       0     NORMP =       0     ITOL  =      20     ICUT  =       9
INTTYP=BEST         GRDTYP=BEST         QMTTOL= 1.0E-06

$SYSTEM OPTIONS
*/



  private Map<String, String> calcOptions;
  private boolean isTypeSet;

  protected void setCalculationType() {
    if (calcOptions == null || isTypeSet)
      return;
    isTypeSet = true;
    String SCFtype = calcOptions.get("contrl_options_SCFTYP");
    String Runtype = calcOptions.get("contrl_options_RUNTYP");
    String igauss = calcOptions.get("basis_options_IGAUSS");
    String gbasis = calcOptions.get("basis_options_GBASIS");
    if (gbasis != null && MOPAC_TYPES.indexOf(gbasis) >= 0) {
      mopacBasis = getMopacAtomZetaSPD(gbasis);
      getSlaters();
      calculationType = gbasis;
    } else {
      boolean DFunc = !"0".equals(calcOptions.get("basis_options_NDFUNC"));
      boolean PFunc = !"0".equals(calcOptions.get("basis_options_NPFUNC"));
      boolean FFunc = !"0".equals(calcOptions.get("basis_options_NFFUNC"));
      String DFTtype = calcOptions.get("contrl_options_DFTTYP");
      int perturb = parseIntStr(calcOptions.get("contrl_options_MPLEVL"));
      String CItype = calcOptions.get("contrl_options_CITYP");
      String CCtype = calcOptions.get("contrl_options_CCTYP");

      if (igauss == null && SCFtype == null)
        return;

      if (calculationType.equals("?"))
        calculationType = "";

      if (igauss != null) {
        if ("0".equals(igauss)) { // we have a non Pople basis set.
          // most common translated to standard notation, others in GAMESS
          // internal format.
          boolean recognized = false;
          if (calculationType.length() > 0)
            calculationType += " ";
          if (gbasis.startsWith("ACC"))
            calculationType += "aug-cc-p";
          if (gbasis.startsWith("CC"))
            calculationType += "cc-p";
          if ((gbasis.startsWith("ACC") || gbasis.startsWith("CC"))
              && gbasis.endsWith("C"))
            calculationType += "C";
          if (gbasis.indexOf("CCD") >= 0) {
            calculationType += "VDZ";
            recognized = true;
          }
          if (gbasis.indexOf("CCT") >= 0) {
            calculationType += "VTZ";
            recognized = true;
          }
          if (gbasis.indexOf("CCQ") >= 0) {
            calculationType += "VQZ";
            recognized = true;
          }
          if (gbasis.indexOf("CC5") >= 0) {
            calculationType += "V5Z";
            recognized = true;
          }
          if (gbasis.indexOf("CC6") >= 0) {
            calculationType += "V6Z";
            recognized = true;
          }
          if (!recognized)
            calculationType += gbasis;
        } else {
          if (calculationType.length() > 0)
            calculationType += " ";
          calculationType += igauss + "-" + PT.rep(gbasis, "N", "");
          if ("T".equals(calcOptions.get("basis_options_DIFFSP"))) {
            // check if we have diffuse S on H's too => "++" instead of "+"
            if ("T".equals(calcOptions.get("basis_options_DIFFS")))
              calculationType += "+";
            calculationType += "+";
          }
          calculationType += "G";
          // append (d,p) , (d), (f,d,p), etc. to indicate polarization.
          // not using * and ** notation as it is inconsistent.
          if (DFunc || PFunc || FFunc) {
            calculationType += "(";
            if (FFunc) {
              calculationType += "f";
              if (DFunc || PFunc)
                calculationType += ",";
            }
            if (DFunc) {
              calculationType += "d";
              if (PFunc)
                calculationType += ",";
            }
            if (PFunc)
              calculationType += "p";
            calculationType += ")";
          }
        }
        if (DFTtype != null && DFTtype.indexOf("NONE") < 0) {
          if (calculationType.length() > 0)
            calculationType += " ";
          calculationType += DFTtype;
        }
        if (CItype != null && CItype.indexOf("NONE") < 0) {
          if (calculationType.length() > 0)
            calculationType += " ";
          calculationType += CItype;
        }
        if (CCtype != null && CCtype.indexOf("NONE") < 0) {
          if (calculationType.length() > 0)
            calculationType += " ";
          calculationType += CCtype;
        }
        if (perturb > 0) {
          if (calculationType.length() > 0)
            calculationType += " ";
          calculationType += "MP" + perturb;
        }
        if (SCFtype != null) {
          if (calculationType.length() > 0)
            calculationType += " ";
          calculationType += SCFtype + " " + Runtype;
        }

      }
    }
  }

  protected void readControlInfo() throws Exception {
    readCalculationInfo("contrl_options_");
  }

  protected void readBasisInfo() throws Exception {
    readCalculationInfo("basis_options_");
  }

  private void readCalculationInfo(String type) throws Exception {
    if (calcOptions == null) {
      calcOptions = new Hashtable<String, String>();
      asc.setInfo("calculationOptions",
          calcOptions);
    }
    while (rd() != null && (line = line.trim()).length() > 0) {
      if (line.indexOf("=") < 0)
        continue;
      String[] tokens = PT.getTokens(PT.rep(line, "="," = ") + " ?");
      for (int i = 0; i < tokens.length; i++) {
        if (!tokens[i].equals("="))
          continue;
        try {
        String key = type + tokens[i - 1];
        String value = (key.equals("basis_options_SPLIT3") ? tokens[++i] + " " + tokens[++i]
            + " " + tokens[++i] : tokens[++i]);
        if (debugging)
          Logger.debug(key + " = " + value);
        calcOptions.put(key, value);
        } catch (Exception e) {
          // not interested
        }
      }
    }
  }


}
