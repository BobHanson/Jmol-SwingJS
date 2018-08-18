package org.jmol.adapter.readers.xtal;

/**
 * Piero Canepa
 * 
 * Dmol3 http://people.web.psi.ch/delley/dmol3.html
 * 
 * @author Pieremanuele Canepa, Room 104, FM Group School of Physical Sciences,
 *         Ingram Building, University of Kent, Canterbury, Kent, CT2 7NH United
 *         Kingdom, pc229@kent.ac.uk
 * 
 * @version 1.0
 */

import javajs.util.DF;
import javajs.util.PT;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Atom;
import org.jmol.util.Logger;

public class DmolReader extends AtomSetCollectionReader {

  private float[] unitCellData;
  private Double totE;
  private boolean geomOpt;

  @Override
  protected boolean checkLine() throws Exception {
    //discardLinesUntilContains("INCOOR, atomic coordinates");
    if (line.contains("** GEOMETRY OPTIMIZATION IN DELOCALIZED COORDINATES **")) {
      geomOpt = true;
    } else if (line.contains("INCOOR, atomic coordinates")) {
      geomOpt = false;
    } else if (!geomOpt ? line.contains("$cell vectors") : line
        .contains("Lattice:")) {
      readCellParam();
    } else if (!geomOpt ? line.contains("$coordinates") : line
        .contains("Input Coordinates")) {
      readCoord();
    } else if (line.contains(" Total Energy")) {
      readEnergy();
    } else if (line.contains("Frequencies (cm-1)")) {
      readFreq();
    }
    return true;
  }

  /*  This is the intial cell in Bohr
  $cell vectors
  29.35935199573791    0.00000000000000    0.00000000000000
   0.00000000000000   25.95066401147447    0.00000000000000
   1.69922562728373    0.00000000000000   17.54793198563486

   */

  private void readCellParam() throws Exception {
    unitCellData = new float[9];
    for (int n = 0, i = 0; n < 3; n++) {
      String[] tokens = PT.getTokens(rd());
      unitCellData[i++] = parseFloatStr(!geomOpt ? tokens[0] : tokens[4])
      * ANGSTROMS_PER_BOHR;
      unitCellData[i++] = parseFloatStr(!geomOpt ? tokens[1] : tokens[5])
      * ANGSTROMS_PER_BOHR;
      unitCellData[i++] = parseFloatStr(!geomOpt ? tokens[2] : tokens[6])
      * ANGSTROMS_PER_BOHR;
    }
  }

  private void newAtomSet() throws Exception {
    applySymmetryAndSetTrajectory();
    asc.newAtomSet();
    if (totE != null)
      setEnergy();
    doApplySymmetry = true;
    if (unitCellData != null) {
      addExplicitLatticeVector(0, unitCellData, 0);
      addExplicitLatticeVector(1, unitCellData, 3);
      addExplicitLatticeVector(2, unitCellData, 6);
      setSpaceGroupName("P1");
    }
    setFractionalCoordinates(false);
  }

  /*  
   * This are the initial coordinates in Bohr 
  $coordinates
  Cl            9.46746707483118  -14.23892561172251   16.08667007245203
  N            14.21326641906855   -5.69691653093388   24.31834296217202
  C            13.13775850152313   -6.97234560502355   18.68982922572976
  C            14.29353578655659   -4.55611608133144   19.67134825073612
  C            13.37907463795869   -3.79263006614721   22.32571709227506
  H            14.26305374826999   -2.21782944550038   22.73141162261102
  C            14.74314585128389   -4.50474463251394   26.82192772160853
  .......................
  O            21.48475665233617   -3.60840142211203   27.57186515636701
  H            20.70238433631352   -2.51542956278797   26.79973685669534
  H            21.31202510204545   -4.76514473514059   26.64610779191174
  $end

   */

  private void readCoord() throws Exception {
    newAtomSet();
    if (geomOpt)
      readLines(2);
    while (rd() != null && !geomOpt ? !line.contains("$end") : !line
        .contains("-----")) {
      String[] tokens = getTokens();
      Atom atom = asc.addNewAtom();
      atom.atomName = !geomOpt ? tokens[0] : tokens[1];
      float factor = (float) (!geomOpt ? ANGSTROMS_PER_BOHR : 1.00);
      float x = parseFloatStr(!geomOpt ? tokens[1] : tokens[2]) * factor;
      float y = parseFloatStr(!geomOpt ? tokens[2] : tokens[3]) * factor;
      float z = parseFloatStr(!geomOpt ? tokens[3] : tokens[4]) * factor;
      atom.set(x, y, z);
      setAtomCoord(atom);
    }
  }

  private void readEnergy() throws Exception {
    rd();
    if (line.contains("Ef"))
      totE = Double.valueOf(Double.parseDouble(PT.getTokens(line.substring(line.indexOf("Ef") +1 , line.indexOf("Ha")  ))[1]));
  }

  private void setEnergy() {
    asc.setAtomSetEnergy("" + totE, totE.floatValue());
    asc.setInfo("Energy", totE);
    asc.setAtomSetName("E = " + totE + " Hartree");
  }

  /*  

    Frequencies (cm-1) and normal modes 
    1:  -16.6    2:   -0.5    3:    0.2    4:    0.5    5:   10.0    6:   21.4    7:   22.1    8:   24.2    9:   31.1

  Cl x     -0.0864      -0.1628      -0.0005       0.0046      -0.0891       0.1852      -0.0800       0.0329       0.1295
  y     -0.2307      -0.0001      -0.1628       0.0000       0.0146       0.1332      -0.0101       0.0200       0.1026
  z      0.0153      -0.0045       0.0008      -0.1628       0.1962      -0.0105       0.2003      -0.1943      -0.0911
  N  x     -0.0093      -0.1023      -0.0002       0.0028      -0.0387       0.0737      -0.0256      -0.0017       0.0623
  y     -0.0556       0.0000      -0.1024       0.0000      -0.0197      -0.0288      -0.0228       0.0123      -0.0237
  z     -0.0199      -0.0029       0.0001      -0.1023       0.0245       0.0262       0.0117      -0.0891       0.0026
  C  x     -0.0136      -0.0947      -0.0002       0.0026      -0.0490       0.0799      -0.0309      -0.0026       0.0752
  y     -0.1142       0.0000      -0.0947       0.0000       0.0277       0.0368       0.0096       0.0001       0.0191
  z     -0.0166      -0.0026       0.0004      -0.0947       0.1016       0.0271       0.1004      -0.0962      -0.0305
  C  x     -0.0130   
   */

  private void readFreq() throws Exception {
    int lastAtomCount = 0;
    int ac = asc.getLastAtomSetAtomCount();
    while (rd() != null && line.charAt(1) == ' ') {
      String[] tokens = getTokens();
      int frequencyCount = tokens.length / 2;
      float[] frequencies = new float[frequencyCount];
      for (int i = 1, n = 0; i < tokens.length; i += 2, n++) {
        frequencies[n] = parseFloatStr(tokens[i]);
        if (debugging)
          Logger.debug((vibrationNumber + n) + " frequency=" + frequencies[n]);
      }

      boolean[] ignore = new boolean[frequencyCount];
      int iAtom0 = 0;

      for (int i = 0; i < frequencyCount; i++) {
        ignore[i] = (!doGetVibration(++vibrationNumber));
        if (ignore[i])
          continue;
        applySymmetryAndSetTrajectory();
        lastAtomCount = cloneLastAtomSet(ac, null);
        if (i == 0)
          iAtom0 = asc.getLastAtomSetAtomIndex();
        asc.setAtomSetFrequency(vibrationNumber, null,
            null, String.valueOf(frequencies[i]), null);
        asc.setAtomSetName(DF.formatDecimal(
            frequencies[i], 2) + " cm-1");

      }
      rd();
      fillFrequencyData(iAtom0, ac, lastAtomCount, ignore, false, 5, 13,
          null, 0, null);
      readLines(2);
    }
  }

}
