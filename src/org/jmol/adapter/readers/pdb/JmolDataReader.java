/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-15 17:34:01 -0500 (Sun, 15 Oct 2006) $
 * $Revision: 5957 $
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

package org.jmol.adapter.readers.pdb;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.adapter.smarter.Atom;
import org.jmol.modelset.Model;
import org.jmol.modelset.ModelSet;
import org.jmol.script.ScriptEval;
import org.jmol.script.ScriptException;
import org.jmol.script.T;
import org.jmol.util.C;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.util.Vibration;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.P3d;
import javajs.util.PT;

/**
 * JmolData file reader, for a modified PDB format
 * 
 * This class also holds pseudo-static methods for spin
 *
 */

public class JmolDataReader extends PdbReader {

  
  private Map<String, double[]> props;
  private String[] residueNames;
  private String[] atomNames;
  private boolean isSpin;
  private double spinFactor;
  private int originatingModel = -1;
  private String jmolDataHeader;
  private P3d[] jmolDataScaling;
  
  //  REMARK   6 Jmol PDB-encoded data: property atomno temperature; for model 0;
  //  REMARK   6 Jmol atom names ... ... ...;
  //  REMARK   6 Jmol residue names ... ... ...;
  //  REMARK   6 Jmol data min = {1.0 -41.87 0.0} max = {26.0 66.53 0.0} unScaledXyz = xyz * {1.0 1.0 0.0} + {0.0 12.33 0.0} plotscale = {100 100 100};
  //  REMARK   6 Jmol property atomno [1.0, 2.0, ...];
  //  REMARK   6 Jmol property temperature [0.0, 23.0, 21.0, ...];

  @Override
  protected void checkRemark() {
    // REMARK 6 Jmol
    while (true) {
      if (line.length() < 30 || line.indexOf("Jmol") != 11)
        break;
      switch ("Ppard".indexOf(line.substring(16, 17))) {
      case 0: //Jmol PDB-encoded data
        props = new Hashtable<String, double[]>();
        isSpin = (line.indexOf(": spin;") >= 0);
        originatingModel = -1;
        int pt = line.indexOf("for model ");
        if (pt > 0)
          originatingModel = PT.parseInt(line.substring(pt + 10));
        jmolDataHeader = line;
        if (!line.endsWith("#noautobond"))
          line += "#noautobond";
        break;
      case 1: // Jmol property 
        int pt1 = line.indexOf("[");
        int pt2 = line.indexOf("]");
        if (pt1 < 25 || pt2 <= pt1)
          return;
        String name = line.substring(25, pt1).trim();
        line = line.substring(pt1 + 1, pt2).replace(',', ' ');
        String[] tokens = getTokens();
        Logger.info("reading " + name + " " + tokens.length);
        double[] prop = new double[tokens.length];
        for (int i = prop.length; --i >= 0;)
          prop[i] = parseDoubleStr(tokens[i]);
        props.put(name, prop);
        break;
      case 2: // Jmol atom names
        //  REMARK   6 Jmol atom names ... ... ...;
        line = line.substring(27);
        atomNames = getTokens();
        Logger.info("reading atom names " + atomNames.length);
        break;
      case 3: // Jmol residue names
        //  REMARK   6 Jmol residue names ... ... ...;
        line = line.substring(30);
        residueNames = getTokens();
        Logger.info("reading residue names " + residueNames.length);
        break;
      case 4: //Jmol data min
        Logger.info(line);
        // The idea here is to use a line such as the following:
        //
        // REMARK   6 Jmol data min = {-1 -1 -1} max = {1 1 1} 
        //                      unScaledXyz = xyz / {10 10 10} + {0 0 0} 
        //                      plotScale = {100 100 100}
        //
        // to pass on to Jmol how to graph non-molecular data. 
        // The format allows for the actual data to be linearly transformed
        // so that it fits into the PDB format for x, y, and z coordinates.
        // This adapter will then unscale the data and also pass on to
        // Jmol the unit cell equivalent that takes the actual data (which
        // will be considered the fractional coordinates) to Jmol coordinates,
        // which will be a cube centered at {0 0 0} and ranging from {-100 -100 -100}
        // to {100 100 100}.
        //
        // Jmol 12.0.RC23 uses this to pass through the adapter a quaternion,
        // ramachandran, or other sort of plot.

        double[] data = new double[15];
        Parser.parseStringInfestedDoubleArray(line.substring(10)
            .replace('=', ' ').replace('{', ' ').replace('}', ' '), null, data);
        P3d minXYZ = P3d.new3(data[0], data[1],
            data[2]);
        P3d maxXYZ = P3d.new3(data[3], data[4],
            data[5]);
        fileScaling = P3d.new3(data[6], data[7],
            data[8]);
        fileOffset = P3d.new3(data[9], data[10],
            data[11]);
        P3d plotScale = P3d.new3(data[12], data[13],
            data[14]);
        if (plotScale.x <= 0)
          plotScale.x = 100;
        if (plotScale.y <= 0)
          plotScale.y = 100;
        if (plotScale.z <= 0)
          plotScale.z = 100;
        if (fileScaling.y == 0)
          fileScaling.y = 1;
        if (fileScaling.z == 0)
          fileScaling.z = 1;
        if (isSpin) {
          spinFactor = plotScale.x/maxXYZ.x;
        } else {
          setFractionalCoordinates(true);
          latticeCells = new int[4];
          asc.xtalSymmetry = null;
          setUnitCell(plotScale.x * 2 / (maxXYZ.x - minXYZ.x),
              plotScale.y * 2 / (maxXYZ.y - minXYZ.y),
              plotScale.z * 2
                  / (maxXYZ.z == minXYZ.z ? 1 : maxXYZ.z - minXYZ.z),
              90, 90, 90);
          unitCellOffset = P3d.newP(plotScale);
          unitCellOffset.scale(-1);
          getSymmetry();
          symmetry.toFractional(unitCellOffset, false);
          unitCellOffset.scaleAdd2(-1d, minXYZ, unitCellOffset);
          symmetry.setOffsetPt(unitCellOffset);
          doApplySymmetry = true;
        }
        jmolDataScaling = new P3d[] { minXYZ, maxXYZ, plotScale };
        break;
      }
      break;
    }
    checkCurrentLineForScript();
  }

  @Override
  protected void processAtom2(Atom atom, int serial, double x, double y, double z, int charge) {
    if (isSpin) {
      Vibration vib = new Vibration();
      vib.set(x, y, z);
      vib.isFrom000 = true;
      atom.vib = vib;
      x *= spinFactor;
      y *= spinFactor;
      z *= spinFactor;
    }
    super.processAtom2(atom, serial, x, y, z, charge);
  }

  @Override
  protected void setAdditionalAtomParameters(Atom atom) {
    if (residueNames != null && atom.index < residueNames.length)
      atom.group3 = residueNames[atom.index];
    if (atomNames != null && atom.index < atomNames.length)
      atom.atomName = atomNames[atom.index];
  }
  
  @Override
  protected void finalizeSubclassReader() throws Exception {
    if (jmolDataHeader == null)
      return;
    Map<String, Object> info = new Hashtable<>();
    info.put(JC.INFO_JMOL_DATA_HEADER, jmolDataHeader);
    info.put(JC.INFO_JMOL_DATA_ORIGINATING_MODEL, Integer.valueOf(originatingModel));
    info.put(JC.INFO_JMOL_DATA_PROPERTIES, props);
    info.put(JC.INFO_JMOL_DATA_SCALING, jmolDataScaling);
    asc.setInfo(JC.INFO_JMOL_DATA, info);
    finalizeReaderPDB();
  }

  public String[] getJmolDataFrameScripts(Viewer vwr, int tok,
                                                 int modelIndex, int modelCount,
                                                 String type, String qFrame,
                                                 String[] props,
                                                 boolean isSpinPointGroup) {
    String script, script2 = null;
    switch (tok) {
    default:
      script = "frame 0.0; frame last; reset;select visible;wireframe only;";
      break;
    case T.property:
      vwr.setFrameTitle(modelCount - 1,
          type + " plot for model " + vwr.getModelNumberDotted(modelIndex));
      script = "frame 0.0; frame last; reset;" + "select visible; spacefill 3.0"
          + "; wireframe 0;" + "draw plotAxisX" + modelCount
          + " {100 -100 -100} {-100 -100 -100} \"" + props[0] + "\";"
          + "draw plotAxisY" + modelCount
          + " {-100 100 -100} {-100 -100 -100} \"" + props[1] + "\";";
      if (props[2] != null)
        script += "draw plotAxisZ" + modelCount
            + " {-100 -100 100} {-100 -100 -100} \"" + props[2] + "\";";
      break;
    case T.ramachandran:
      vwr.setFrameTitle(modelCount - 1, "ramachandran plot for model "
          + vwr.getModelNumberDotted(modelIndex));
      script = "frame 0.0; frame last; reset;"
          + "select visible; color structure; spacefill 3.0; wireframe 0;"
          + "draw ramaAxisX" + modelCount + " {100 0 0} {-100 0 0} \"phi\";"
          + "draw ramaAxisY" + modelCount + " {0 100 0} {0 -100 0} \"psi\";";
      break;
    case T.quaternion:
    case T.helix:
      vwr.setFrameTitle(modelCount - 1, type.replace('w', ' ') + qFrame
          + " for model " + vwr.getModelNumberDotted(modelIndex));
      //$FALL-THROUGH$
    case T.spin:
      String color = (C.getHexCode(vwr.cm.colixBackgroundContrast));
      script = "frame 0.0; frame last; reset;"
          + "select visible; wireframe 0; spacefill 3.0; "
          + "isosurface quatSphere" + modelCount + " color " + color
          + " sphere 100.0 mesh nofill frontonly translucent 0.8;"
          + "draw quatAxis" + modelCount
          + "X {100 0 0} {-100 0 0} color red \"x\";" + "draw quatAxis"
          + modelCount + "Y {0 100 0} {0 -100 0} color green \"y\";"
          + "draw quatAxis" + modelCount
          + "Z {0 0 100} {0 0 -100} color blue \"z\";"
          + (tok == T.spin ? "vectors 2.0;spacefill off;" : "color structure;")
          + "draw quatCenter" + modelCount + "{0 0 0} scale 0.02;";
      if (isSpinPointGroup) {
        script2 = ";set symmetryhm;" +
        //"frame 2.1;" + 
            "draw spin pointgroup;" + "var name = {2.1}.pointgroup().hmName;"
            + "set echo hmname 100% 100%;" + "set echo hmname RIGHT;"
            + "set echo hmname model 2.1;" + "echo @name;";
      }
      break;
    }
    return new String[] { script, script2 };
  }
     
  public Object[] getJmolDataFrameProperties(ScriptEval e, int tok,
                                                    int[] propToks,
                                                    String[] props, BS bs,
                                                    P3d minXYZ, P3d maxXYZ,
                                                    String format,
                                                    boolean isPdbFormat)
      throws ScriptException {
    // prepare data for property plotting

    double pdbFactor = 1;
    double[] dataX = null, dataY = null, dataZ = null;
    dataX = e.getBitsetPropertyFloat(bs, propToks[0] | T.selectedfloat,
        propToks[0] == T.property ? props[0] : null,
        (minXYZ == null ? Double.NaN : minXYZ.x),
        (maxXYZ == null ? Double.NaN : maxXYZ.x));
    String[] propData = new String[3];
    propData[0] = props[0] + " " + Escape.eAD(dataX);
    if (props[1] != null) {
      dataY = e.getBitsetPropertyFloat(bs, propToks[1] | T.selectedfloat,
          propToks[1] == T.property ? props[1] : null,
          (minXYZ == null ? Double.NaN : minXYZ.y),
          (maxXYZ == null ? Double.NaN : maxXYZ.y));
      propData[1] = props[1] + " " + Escape.eAD(dataY);
    }
    if (props[2] != null) {
      dataZ = e.getBitsetPropertyFloat(bs, propToks[2] | T.selectedfloat,
          propToks[2] == T.property ? props[2] : null,
          (minXYZ == null ? Double.NaN : minXYZ.z),
          (maxXYZ == null ? Double.NaN : maxXYZ.z));
      propData[2] = props[2] + " " + Escape.eAD(dataZ);
    }
    if (minXYZ == null)
      minXYZ = P3d.new3(getPlotMinMax(dataX, false, propToks[0]),
          getPlotMinMax(dataY, false, propToks[1]),
          getPlotMinMax(dataZ, false, propToks[2]));
    if (maxXYZ == null)
      maxXYZ = P3d.new3(getPlotMinMax(dataX, true, propToks[0]),
          getPlotMinMax(dataY, true, propToks[1]),
          getPlotMinMax(dataZ, true, propToks[2]));
    Logger.info("plot min/max: " + minXYZ + " " + maxXYZ);
    P3d center = null;
    P3d factors = null;

    if (isPdbFormat) {
      factors = P3d.new3(1, 1, 1);
      center = new P3d();
      center.ave(maxXYZ, minXYZ);
      factors.sub2(maxXYZ, minXYZ);
      if (tok != T.spin)
        factors.set(factors.x / 200, factors.y / 200, factors.z / 200);
      if (T.tokAttr(propToks[0], T.intproperty)) {
        factors.x = 1;
        center.x = 0;
      } else if (factors.x > 0.1 && factors.x <= 10) {
        factors.x = 1;
      }
      if (T.tokAttr(propToks[1], T.intproperty)) {
        factors.y = 1;
        center.y = 0;
      } else if (factors.y > 0.1 && factors.y <= 10) {
        factors.y = 1;
      }
      if (T.tokAttr(propToks[2], T.intproperty)) {
        factors.z = 1;
        center.z = 0;
      } else if (factors.z > 0.1 && factors.z <= 10) {
        factors.z = 1;
      }
      if (props[2] == null || props[1] == null)
        center.z = minXYZ.z = maxXYZ.z = factors.z = 0;
      for (int i = 0; i < dataX.length; i++)
        dataX[i] = (dataX[i] - center.x) / factors.x * pdbFactor;
      if (props[1] != null)
        for (int i = 0; i < dataY.length; i++)
          dataY[i] = (dataY[i] - center.y) / factors.y * pdbFactor;
      if (props[2] != null)
        for (int i = 0; i < dataZ.length; i++)
          dataZ[i] = (dataZ[i] - center.z) / factors.z * pdbFactor;
    }
    return new Object[] { bs, dataX, dataY, dataZ, minXYZ, maxXYZ, factors,
        center, format, propData, Double.valueOf(1) };
  }

  private static double getPlotMinMax(double[] data, boolean isMax, int tok) {
    if (data == null)
      return 0;
    switch (tok) {
    case T.omega:
    case T.phi:
    case T.psi:
      return (isMax ? 180 : -180);
    case T.eta:
    case T.theta:
      return (isMax ? 360 : 0);
    case T.straightness:
      return (isMax ? 1 : -1);
    }
    double fmax = (isMax ? -1E10d : 1E10d);
    for (int i = data.length; --i >= 0;) {
      double f = data[i];
      if (Double.isNaN(f))
        continue;
      if (isMax == (f > fmax))
        fmax = f;
    }
    return fmax;
  }

  /**
   * Set up the JmolDataFrame initially
   * @param ms
   * @param type
   * @param modelIndex
   * @param modelDataIndex
   */
  public void setJmolDataFrame(ModelSet ms, String type, int modelIndex,
                               int modelDataIndex) {
    ms.haveJmolDataFrames = true;
    Model mdata = ms.am[modelDataIndex];
    Model model0 = ms.am[type == null ? mdata.dataSourceFrame
        : modelIndex];
    if (type == null) {
      //leaving a data frame -- just set generic to this one if quaternion
      type = mdata.jmolFrameType;
    }
    if (modelIndex >= 0) {
      // initializing
      if (model0.dataFrames == null) {
        model0.dataFrames = new Hashtable<String, Integer>();
      }
      mdata.dataSourceFrame = modelIndex;
      mdata.jmolFrameType = type;
      model0.dataFrames.put(type, Integer.valueOf(modelDataIndex));
      if (mdata.jmolFrameTypeInt == T.quaternion && type.indexOf("deriv") < 0) { //generic quaternion
        type = type.substring(0, type.indexOf(" "));
        model0.dataFrames.put(type, Integer.valueOf(modelDataIndex));
      }
      mdata.uvw0 = (P3d[]) ms.getModelAuxiliaryInfo(modelIndex)
          .get(JC.SSG_POINT_GROUP_AXES);
      if (mdata.uvw0 != null) {
        mdata.uvw0[0].scale(105);
        mdata.uvw0[1].scale(105);
        mdata.uvw0[2].scale(105);
        mdata.uvw = new P3d[] { P3d.newP(mdata.uvw0[0]), P3d.newP(mdata.uvw0[1]),
            P3d.newP(mdata.uvw0[2]) };
      }
    }
  }

  /**
   * remove non-spin atoms and atoms with duplicated spin and set min/max XYZ for the plot
   * @param vwr
   * @param bs
   * @param modelIndex
   * @param minXYZ
   * @param maxXYZ
   * @return bitset for plot
   */
  public BS getPlotSpinSet(Viewer vwr, BS bs, int modelIndex, P3d minXYZ,
                       P3d maxXYZ) {
    if (bs.nextSetBit(0) < 0) {
      bs = vwr.getModelUndeletedAtomsBitSet(modelIndex);
    }
    if (bs.isEmpty())
      return null;
    // remove non-spin atoms
    double len = 0;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      Vibration v = vwr.ms.getVibration(i, false);
      if (v == null)
        bs.clear(i);
      else
        len = Math.max(len, v.length());
    }
    if (len == 0)
      return null;
    minXYZ.set(-len, -len, -len);
    maxXYZ.set(len, len, len);
    Lst<Vibration> lst = new Lst<>();
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      Vibration v = vwr.ms.getVibration(i, false);
      boolean found = false;
      for (int j = lst.size(); --j >= 0;) {
        if (v.distance(lst.get(j)) < 0.1d) {
          found = true;
          bs.clear(i);
          break;
        }
      }
      if (!found)
        lst.addLast(v);
    }
    return bs;
  }
  

}
