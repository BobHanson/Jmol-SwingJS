/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-30 11:40:16 -0500 (Fri, 30 Mar 2007) $
 * $Revision: 7273 $
 *
 * Copyright (C) 2007 Miguel, Bob, Jmol Development
 *
 * Contact: hansonr@stolaf.edu
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.jvxl.readers;

import java.util.Map;
import java.util.Random;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.MeasureD;
import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.T3d;
import javajs.util.V3d;

import org.jmol.api.Interface;
import org.jmol.jvxl.data.VolumeData;
import org.jmol.quantum.MOCalculation;
import org.jmol.quantum.NciCalculation;
import org.jmol.quantum.QS;
import org.jmol.quantum.QuantumCalculation;
import org.jmol.quantum.QuantumPlaneCalculation;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

class IsoMOReader extends AtomDataReader {

  private Random random;
  private P3d[] points;
  private V3d vTemp;
  private QuantumCalculation q;
  private Lst<Map<String, Object>> mos;
  private boolean isNci;
  private double[] coef;
  private int[][] dfCoefMaps;
  private double[] linearCombination;
  private double[][] coefs;
  private boolean isElectronDensityCalc;

  IsoMOReader() {
  }

  @Override
  void init(SurfaceGenerator sg) {
    initADR(sg);
    isNci = (params.qmOrbitalType == Parameters.QM_TYPE_NCI_PRO);
    if (isNci) {
      // NCI analysis org.jmol.quantum.NciCalculation
      // allows for progressive plane reading, which requires
      // isXLowToHigh to be TRUE
      isXLowToHigh = hasColorData = true;
      precalculateVoxelData = false; // will process as planes
      params.insideOut = !params.insideOut;
    }
  }

  /////// ab initio/semiempirical quantum mechanical orbitals ///////

  private Map<String, Object> mo;

  @Override
  @SuppressWarnings("unchecked")
  protected void setup(boolean isMapData) {
    mos = (Lst<Map<String, Object>>) params.moData.get("mos");
    linearCombination = params.qm_moLinearCombination;
    mo = (mos != null && linearCombination == null ? mos
        .get(params.qm_moNumber - 1) : null);
    boolean haveVolumeData = params.moData.containsKey("haveVolumeData");
    if (haveVolumeData && mo != null) // from XmlChem3dReader
      params.volumeData = (VolumeData) mo.get("volumeData");
    setup2();
    doAddHydrogens = false;
    getAtoms(params.bsSelected, doAddHydrogens, !isNci, isNci, isNci, false,
        false, params.qm_marginAngstroms, (isNci ? null
            : params.modelInvRotation));
    String className;
    if (isNci) {
      className = "quantum.NciCalculation";
      setHeader(
          "NCI (promolecular)",
          "see NCIPLOT: A Program for Plotting Noncovalent Interaction Regions, Julia Contreras-Garcia, et al., J. of Chemical Theory and Computation, 2011, 7, 625-632");
    } else {
      className = "quantum.MOCalculation";
      setHeader("MO",
          "calculation type: " + params.moData.get("calculationType"));
    }
    setRanges(params.qm_ptsPerAngstrom, params.qm_gridMax, 0);
    if (haveVolumeData) {
      for (int i = params.title.length; --i >= 0;)
        fixTitleLine(i, mo);
    } else {
      q = (QuantumCalculation) Interface.getOption(
          className, (Viewer) sg.atomDataServer, "file");
      if (isNci) {
        qpc = (QuantumPlaneCalculation) q;
      } else {
        if (linearCombination == null) {
          for (int i = params.title.length; --i >= 0;)
            fixTitleLine(i, mo);
          coef = (double[]) mo.get("coefficients");
          dfCoefMaps = (int[][]) mo.get("dfCoefMaps");
        } else {
          coefs = AU.newDouble2(mos.size());
          for (int i = 1; i < linearCombination.length; i += 2) {
            int j = (int) linearCombination[i];
            if (j > mos.size() || j < 1)
              return;
            coefs[j - 1] = (double[]) mos.get(j - 1).get("coefficients");
          }
          for (int i = params.title.length; --i >= 0;)
            fixTitleLine(i, null);
        }
      }
      isElectronDensityCalc = (coef == null && linearCombination == null && !isNci);
    }
    volumeData.sr = null;
    if (isMapData && !isElectronDensityCalc && !haveVolumeData) {
      volumeData.doIterate = false;
      volumeData.setVoxelDataAsArray(voxelData = new double[1][1][1]);
      volumeData.sr = this;
      points = new P3d[1];
      points[0] = new P3d();
      if (!setupCalculation())
        q = null;
    } else if (params.psi_monteCarloCount > 0) {
      vertexDataOnly = true;
      random = new Random(params.randomSeed);
    }
  }

  @Override
  protected boolean readVolumeParameters(boolean isMapData) {
    setup(isMapData);
    if (volumeData.sr == null)
      initializeVolumetricData();
    return true;
  }

  private void fixTitleLine(int iLine, Map<String, Object> mo) {
    // see Parameters.Java for defaults here.
    if (params.title == null)
      return;
    String line = params.title[iLine];
    if (line.indexOf(" MO ") >= 0) {
      String nboType = (String) params.moData.get("nboType");
      if (nboType != null)
        line = PT.rep(line, " MO ", " " + nboType + " ");
    }
    if (line.indexOf("%M") >= 0)
      line = params.title[iLine] = PT.formatStringS(line, "M",
          atomData.modelName);
    if (line.indexOf("%F") >= 0)
      line = params.title[iLine] = PT.formatStringS(line, "F",
          PT.rep(params.fileName, "DROP_", ""));
    int pt = line.indexOf("%");
    if (line.length() == 0 || pt < 0)
      return;
    int rep = 0;
    //    if (line.indexOf("%F") >= 0)
    //    line = PT.formatStringS(line, "F", params.fileName);
    if (line.indexOf("%I") >= 0)
      line = PT.formatStringS(line, "I",
          params.qm_moLinearCombination == null ? "" + params.qm_moNumber
              : QS.getMOString(params.qm_moLinearCombination));
    if (line.indexOf("%N") >= 0)
      line = PT.formatStringS(line, "N", "" + params.qmOrbitalCount);
    Number energy = null;
    if (mo == null) {
      // check to see if all orbitals have the same energy
      for (int i = 0; i < linearCombination.length; i += 2)
        if (linearCombination[i] != 0) {
          mo = mos.get((int) linearCombination[i + 1] - 1);
          Number e = (Number) mo.get("energy");
          if (energy == null) {
            if (e == null)
              break;
            energy = e;
          } else if (!energy.equals(e)) {
            energy = null;
            break;
          }
        }
    } else {
      if (mo.containsKey("energy"))
        energy = (Number) mo.get("energy");
    }
    if (line.indexOf("%E") >= 0) {
      line = PT.formatStringS(line, "E",
          energy != null && ++rep != 0 ? "" + energy : "");
    } else if (energy != null) {
        String s = PT.formatStringF(line, "E", energy.doubleValue());
        if (s != line) {
          line = s;
          rep++;
        }
    }
    if (line.indexOf("%U") >= 0)
      line = PT
          .formatStringS(line, "U",
              energy != null && params.moData.containsKey("energyUnits")
                  && ++rep != 0 ? (String) params.moData.get("energyUnits")
                      : "");
    if (line.indexOf("%L") >= 0) {
      String[] labels = (String[]) params.moData.get("nboLabels");
      line = PT.formatStringS(line, "L",
          (labels != null && params.qm_moNumber > 0 && ++rep != 0
              ? labels[(params.qm_moNumber - 1) % labels.length]
              : ""));
    }
    if (line.indexOf("%S") >= 0)
      line = PT.formatStringS(line, "S",
          mo != null && mo.containsKey("symmetry") && ++rep != 0
              ? "" + mo.get("symmetry")
              : "");
    if (line.indexOf("%O") >= 0) {
      Number obj = (mo == null ? null : (Number) mo.get("occupancy"));
      double o = (obj == null ? 0 : obj.doubleValue());
      line = PT.formatStringS(line, "O", obj != null 
          && params.qm_moLinearCombination == null && ++rep != 0
          ? (o == (int) o ? "" + (int) o : PT.formatF(o, 0, 4, false, false))
          : "");
    }
    if (line.indexOf("%T") >= 0)
      line = PT.formatStringS(line, "T",
          mo != null && mo.containsKey("type")
              ? (params.qm_moLinearCombination == null  && ++rep != 0 ? "" + mo.get("type")
              : "") + ((params.isSquared || params.isSquaredLinear) && ++rep != 0 ? " ^2" : "")
              : "");
    if (line.equals("string")) {
      params.title[iLine] = "";
      return;
    }
    boolean isOptional = (line.indexOf("?") == 0);
    params.title[iLine] = (!isOptional ? line
        : rep > 0 && !line.trim().endsWith("=") ? line.substring(1) : "");
  }

  private final double[] vDist = new double[3];

  @Override
  protected void readSurfaceData(boolean isMapData) throws Exception {
    if (volumeData.sr != null)
      return;
    if (params.psi_monteCarloCount <= 0) {
      readSurfaceDataVDR(isMapData);
      return;
    }
    if (points != null)
      return; // already done
    points = new P3d[1000];
    for (int j = 0; j < 1000; j++)
      points[j] = new P3d();
    if (params.thePlane != null)
      vTemp = new V3d();
    // presumes orthogonal
    for (int i = 0; i < 3; i++)
      vDist[i] = volumeData.volumetricVectorLengths[i]
          * volumeData.voxelCounts[i];
    volumeData.setVoxelDataAsArray(voxelData = new double[1000][1][1]);
    getValues();
    double value;
    double f = 0;
    for (int j = 0; j < 1000; j++)
      if ((value = Math.abs(voxelData[j][0][0])) > f)
        f = value;
    if (f < 0.0001f)
      return;
    if (f > params.cutoff)
      f = params.cutoff;
    //minMax = new double[] {(params.mappedDataMin  = -f / 2), 
    //(params.mappedDataMax = f / 2)};
    for (int i = 0; i < params.psi_monteCarloCount;) {
      getValues();
      for (int j = 0; j < 1000; j++) {
        value = voxelData[j][0][0];
        double absValue = Math.abs(value);
        if (absValue <= getRnd(f))
          continue;
        addVC(points[j], value, 0, false);

        if (i < 200)
          System.out.println(points[j] + " " + value);
        if (++i == params.psi_monteCarloCount)
          break;
      }
    }
  }

  @Override
  protected void postProcessVertices() {
    // not clear that this is a good idea...
    //if (params.thePlane != null && params.slabInfo == null)
    //params.addSlabInfo(MeshSurface.getSlabWithinRange(0.01f, -0.01f));

  }

  private void getValues() {
    for (int j = 0; j < 1000; j++) {
      voxelData[j][0][0] = 0;
      points[j].set(volumeData.volumetricOrigin.x + getRnd(vDist[0]),
          volumeData.volumetricOrigin.y + getRnd(vDist[1]),
          volumeData.volumetricOrigin.z + getRnd(vDist[2]));
      if (params.thePlane != null)
        MeasureD
            .getPlaneProjection(points[j], params.thePlane, points[j], vTemp);
    }
    createOrbital();
  }

  @Override
  public double getValueAtPoint(T3d pt, boolean getSource) {
    return (q == null ? 0 : q.processPt(pt));
  }

  private double getRnd(double f) {
    return random.nextFloat() * f;
  }

  //mapping mos fails

  @Override
  protected void generateCube() {
    if (params.volumeData != null)
      return;
    newVoxelDataCube();
    createOrbital();
  }

  protected void createOrbital() {
    boolean isMonteCarlo = (params.psi_monteCarloCount > 0);
    if (isElectronDensityCalc) {
      // electron density calc
      if (mos == null || isMonteCarlo)
        return;
      for (int i = params.qm_moNumber; --i >= 0;) {
        Logger.info(" generating isosurface data for MO " + (i + 1));
        Map<String, Object> mo = mos.get(i);
        coef = (double[]) mo.get("coefficients");
        dfCoefMaps = (int[][]) mo.get("dfCoefMaps");
        if (!setupCalculation())
          return;
        q.createCube();
      }
    } else {
      if (!isMonteCarlo)
        Logger.info("generating isosurface data for MO using cutoff "
            + params.cutoff);
      if (!setupCalculation())
        return;
      q.createCube();
      jvxlData.integration = q.getIntegration();
      if (mo != null)
        mo.put("integration", Double.valueOf(jvxlData.integration));
    }
  }

  @Override
  public double[] getPlane(int x) {
    if (!qSetupDone)
      setupCalculation();
    return getPlaneSR(x);
  }

  private boolean qSetupDone;

  private boolean setupCalculation() {
    qSetupDone = true;
    switch (params.qmOrbitalType) {
    case Parameters.QM_TYPE_VOLUME_DATA:
      break;
    case Parameters.QM_TYPE_SLATER:
    case Parameters.QM_TYPE_GAUSSIAN:
      return ((MOCalculation) q).setupCalculation(
          params.moData, params.qmOrbitalType == Parameters.QM_TYPE_SLATER,  
          volumeData, bsMySelected,
          atomData.xyz, atomData.atoms,
          atomData.firstAtomIndex,
          dfCoefMaps, coef,
          linearCombination, params.isSquaredLinear, 
          coefs, points);
    case Parameters.QM_TYPE_NCI_PRO:
      return ((NciCalculation) q).setupCalculation(volumeData, bsMySelected,
          params.bsSolvent, atomData.bsMolecules, atomData.atoms,
          atomData.firstAtomIndex, true, points, params.parameters,
          params.testFlags);
    }
    return false;
  }

  @Override
  protected double getSurfacePointAndFraction(double cutoff,
                                             boolean isCutoffAbsolute,
                                             double valueA, double valueB,
                                             T3d pointA, V3d edgeVector, int x,
                                             int y, int z, int vA, int vB,
                                             double[] fReturn, T3d ptReturn) {
    double zero = getSPF(cutoff, isCutoffAbsolute, valueA, valueB, pointA,
        edgeVector, x, y, z, vA, vB, fReturn, ptReturn);
    if (q != null && !Double.isNaN(zero)) {
      zero = q.processPt(ptReturn);
      if (params.isSquared)
        zero *= zero;
    }
    return zero;
  }

}
