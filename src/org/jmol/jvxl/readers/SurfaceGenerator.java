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
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/*
 
 * The JVXL file format
 * --------------------
 * 
 * as of 3/29/07 this code is COMPLETELY untested. It was hacked out of the
 * Jmol code, so there is probably more here than is needed.
 * 
 * 
 * 
 * see http://www.stolaf.edu/academics/chemapps/jmol/docs/misc/JVXL-format.pdf
 *
 * The JVXL (Jmol VoXeL) format is a file format specifically designed
 * to encode an isosurface or planar slice through a set of 3D scalar values
 * in lieu of a that set. A JVXL file can contain coordinates, and in fact
 * it must contain at least one coordinate, but additional coordinates are
 * optional. The file can contain any finite number of encoded surfaces. 
 * However, the compression of 300-500:1 is based on the reduction of the 
 * data to a SINGLE surface. 
 * 
 * 
 * The original Marching Cubes code was written by Miguel Howard in 2005.
 * The classes Parser, ArrayUtil, and TextFormat are condensed versions
 * of the classes found in org.jmol.util.
 * 
 * All code relating to JVXL format is copyrighted 2006/2007 and invented by 
 * Robert M. Hanson, 
 * Professor of Chemistry, 
 * St. Olaf College, 
 * 1520 St. Olaf Ave.
 * Northfield, MN. 55057.
 * 
 * Implementations of the JVXL format should reference 
 * "Robert M. Hanson, St. Olaf College" and the opensource Jmol project.
 * 
 * 
 * implementing marching squares; see 
 * http://www.secam.ex.ac.uk/teaching/ug/studyres/COM3404/COM3404-2006-Lecture15.pdf
 * 
 * lines through coordinates are identical to CUBE files
 * after that, we have a line that starts with a negative number to indicate this
 * is a JVXL file:
 * 
 * line1:  (int)-nSurfaces  (int)edgeFractionBase (int)edgeFractionRange  
 * (nSurface lines): (float)cutoff (int)nBytesData (int)nBytesFractions
 * 
 * definition1
 * edgedata1
 * fractions1
 * colordata1
 * ....
 * definition2
 * edgedata2
 * fractions2
 * colordata2
 * ....
 * 
 * definitions: a line with detail about what sort of compression follows
 * 
 * edgedata: a list of the count of vertices ouside and inside the cutoff, whatever
 * that may be, ordered by nested for loops for(x){for(y){for(z)}}}.
 * 
 * nOutside nInside nOutside nInside...
 * 
 * fractions: an ascii list of characters representing the fraction of distance each
 * encountered surface point is along each voxel cube edge found to straddle the 
 * surface. The order written is dictated by the reader algorithm and is not trivial
 * to describe. Each ascii character is constructed by taking a base character and 
 * adding onto it the fraction times a range. This gives a character that can be
 * quoted EXCEPT for backslash, which MAY be substituted for by '!'. Jmol uses the 
 * range # - | (35 - 124), reserving ! and } for special meanings.
 * 
 * colordata: same deal here, but with possibility of "double precision" using two bytes.
 * 
 * 
 * 
 * THIS READER
 * -----------
 * 
 * This is a first attempt at a generic JVXL file reader and writer class.
 * It is an extraction of Jmol org.jmol.viewer.Isosurface.Java and related pieces.
 * 
 * The goal of the reader is to be able to read CUBE-like data and 
 * convert that data to JVXL file data.
 * 
 * 
 */

package org.jmol.jvxl.readers;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;

import java.util.Map;


import org.jmol.atomdata.AtomData;
import org.jmol.atomdata.AtomDataServer;
import org.jmol.atomdata.RadiusData;
import javajs.util.BS;
import org.jmol.jvxl.data.JvxlCoder;
import org.jmol.jvxl.data.JvxlData;
import org.jmol.jvxl.data.VolumeData;
import org.jmol.jvxl.data.MeshData;
import org.jmol.jvxl.api.MeshDataServer;
import org.jmol.jvxl.calc.MarchingSquares;

import javajs.api.GenericBinaryDocument;
import javajs.util.AU;
import javajs.util.Lst;
import org.jmol.util.Logger;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.Viewer;

import javajs.util.M4;
import javajs.util.Measure;
import javajs.util.Rdr;
import javajs.util.OC;
import javajs.util.PT;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.V3;

public class SurfaceGenerator {

  public Parameters params;
  public JvxlData jvxlData;
  public MeshData meshData;
  public VolumeData volumeDataTemp; 
  public MeshDataServer meshDataServer;
  public AtomDataServer atomDataServer;
  public MarchingSquares marchingSquares;
  public String version;
  public boolean isValid = true;
  public String fileType;
  public BS bsVdw;
  
  private int colorPtr;
  private SurfaceReader surfaceReader;
  private OC out;

  public SurfaceGenerator(AtomDataServer atomDataServer, MeshDataServer meshDataServer,
                          MeshData meshData, JvxlData jvxlData) {
    this.atomDataServer = atomDataServer;
    this.meshDataServer = meshDataServer;
    params = new Parameters();
    this.meshData = (meshData == null ? new MeshData() : meshData);
    this.jvxlData = (jvxlData == null ? new JvxlData() : jvxlData);
    volumeDataTemp = new VolumeData();
    // volumeDataTemp is used for some initial work, but it is sometimes
    // replaced in surfaceReader by other sources of volume data. 
    // Contact will re-use it.
    initializeIsosurface();
  }
  
  public void setJvxlData(JvxlData jvxlData) {
    this.jvxlData = jvxlData;
    if (jvxlData != null)
      jvxlData.version = version;
  }

  //////////////////////////////////////////////////////////////

  /**
   * 
   * @param propertyName
   * @param value
   * @param bs
   * @return TRUE if handled
   */
  @SuppressWarnings("unchecked")
  public boolean setProp(String propertyName, Object value, BS bs) {

    if ("debug" == propertyName) {
      boolean TF = ((Boolean) value).booleanValue();
      params.logMessages = TF;
      // logCompression = TF;
      params.logCube = TF;
      return true;
    }

    if ("init" == propertyName) {
      initializeIsosurface();
      if (value instanceof Parameters) {
        params = (Parameters) value;
      } else {
        params.script = (String) value;
        if (params.script != null && params.script.indexOf(";#") >= 0) {
          // crude hack for ScriptEvaluator messing up
          params.script = PT.rep(params.script, ";#", "; #");
        }
      }
      return false; // more to do
    }

    if ("silent" == propertyName) {
      params.isSilent = true;
      return true;
    }

    if ("map" == propertyName) {
      params.resetForMapping(((Boolean) value).booleanValue());
      if (surfaceReader != null)
        surfaceReader.minMax = null;
      return true;
    }
    if ("finalize" == propertyName) {
      initializeIsosurface();
      return true;
    }

    if ("clear" == propertyName) {
      if (surfaceReader != null)
        surfaceReader.discardTempData(true);
      return false;
    }

    if ("fileIndex" == propertyName) {
      params.fileIndex = ((Integer) value).intValue();
      if (params.fileIndex < 0)
        params.fileIndex = 0;// 0 used in efvet reader for "current color"
      params.readAllData = false;
      return true;
    }

    if ("blockData" == propertyName) {
      params.blockCubeData = ((Boolean) value).booleanValue();
      return true;
    }

    if ("withinPoints" == propertyName) {
      params.boundingBox = (P3[]) ((Object[]) value)[1];
      return true;
    }

    if ("boundingBox" == propertyName) {
      P3[] pts = (P3[]) value;
      params.boundingBox = new P3[] { P3.newP(pts[0]),
          P3.newP(pts[pts.length - 1]) };
      return true;
    }

    if ("func" == propertyName) {
      params.func = value;
      return true;
    }

    if ("intersection" == propertyName) {
      params.intersection = (BS[]) value;
      return true;
    }

    if ("bsSolvent" == propertyName) {
      params.bsSolvent = (BS) value;
      return true;
    }

    if ("select" == propertyName) {
      params.bsSelected = (BS) value;
      return true;
    }

    if ("ignore" == propertyName) {
      params.bsIgnore = (BS) value;
      return true;
    }

    if ("propertySmoothing" == propertyName) {
      params.propertySmoothing = ((Boolean) value).booleanValue();
      return true;
    }

    if ("propertyDistanceMax" == propertyName) {
      params.propertyDistanceMax = ((Float) value).floatValue();
      return true;
    }

    if ("propertySmoothingPower" == propertyName) {
      params.propertySmoothingPower = ((Integer) value).intValue();
      return true;
    }

    if ("title" == propertyName) {
      if (value == null) {
        params.title = null;
        return true;
      } else if (AU.isAS(value)) {
        params.title = (String[]) value;
        if (Logger.debugging)
         for (int i = 0; i < params.title.length; i++)
           if (params.title[i].length() > 0)
             Logger.info(params.title[i]);
      }
      return true;
    }

    if ("sigma" == propertyName) {
      // not all readers will take this, so we assign
      // cutoff to the value as well.
      params.cutoff = params.sigma = ((Float) value).floatValue();
      params.isPositiveOnly = false;
      params.cutoffAutomatic = false;
      return true;
    }

    if ("cutoff" == propertyName) {
      params.cutoff = ((Float) value).floatValue();
      params.isPositiveOnly = false;
      params.cutoffAutomatic = false;
      return true;
    }

    if ("parameters" == propertyName) {
      params.parameters = AU.ensureLengthA((float[]) value, 2);
      if (params.parameters.length > 0 && params.parameters[0] != 0)
        params.cutoff = params.parameters[0];
      return true;
    }

    if ("cutoffPositive" == propertyName) {
      params.cutoff = ((Float) value).floatValue();
      params.isPositiveOnly = true;
      return true;
    }

    if ("cap" == propertyName || "slab" == propertyName) {
      if (value != null)
        params.addSlabInfo((Object[]) value);
      return true;
    }

    if ("scale" == propertyName) {
      params.scale = ((Float) value).floatValue();
      return true;
    }

    if ("scale3d" == propertyName) {
      params.scale3d = ((Float) value).floatValue();
      return true;
    }

    if ("angstroms" == propertyName) {
      params.isAngstroms = true;
      return true;
    }

    if ("resolution" == propertyName) {
      float resolution = ((Float) value).floatValue();
      params.resolution = (resolution > 0 ? resolution : Float.MAX_VALUE);
      return true;
    }

    if ("downsample" == propertyName) {
      int rate = ((Integer) value).intValue();
      params.downsampleFactor = (rate >= 0 ? rate : 0);
      return true;
    }

    if ("anisotropy" == propertyName) {
      if ((params.dataType & Parameters.NO_ANISOTROPY) == 0)
        params.setAnisotropy((P3) value);
      return true;
    }

    if ("eccentricity" == propertyName) {
      params.setEccentricity((P4) value);
      return true;
    }

    if ("addHydrogens" == propertyName) {
      params.addHydrogens = ((Boolean) value).booleanValue();
      return true;
    }

    if ("squareData" == propertyName) {
      params.isSquared = (value == null ? false : ((Boolean) value)
          .booleanValue());
      return true;
    }

    if ("squareLinear" == propertyName) {
      params.isSquaredLinear = (value == null ? false : ((Boolean) value)
          .booleanValue());
      return true;
    }

    if ("gridPoints" == propertyName) {
      params.iAddGridPoints = true;
      return true;
    }

    if ("atomIndex" == propertyName) {
      params.atomIndex = ((Integer) value).intValue();
      return true;
    }

    // / color options

    if ("insideOut" == propertyName) {
      params.insideOut = true;
      return true;
    }

    if ("sign" == propertyName) {
      params.isCutoffAbsolute = true;
      params.colorBySign = true;
      colorPtr = 0;
      return true;
    }

    if ("colorRGB" == propertyName) {
      int rgb = ((Integer) value).intValue();
      params.colorRgb = params.colorPos = params.colorPosLCAO = rgb;
      if (colorPtr++ == 0) {
        params.colorNeg = params.colorNegLCAO = rgb;
      } else {
        params.colorRgb = Integer.MAX_VALUE;
      }
      return true;
    }

    if ("monteCarloCount" == propertyName) {
      params.psi_monteCarloCount = ((Integer) value).intValue();
      return true;
    }
    if ("rangeAll" == propertyName) {
      params.rangeAll = true;
      return true;
    }

    if ("rangeSelected" == propertyName) {
      params.rangeSelected = true;
      return true;
    }

    if ("red" == propertyName) {
      params.valueMappedToRed = ((Float) value).floatValue();
      return true;
    }

    if ("blue" == propertyName) {
      params.valueMappedToBlue = ((Float) value).floatValue();
      if (params.valueMappedToRed > params.valueMappedToBlue) {
        float f = params.valueMappedToRed;
        params.valueMappedToRed = params.valueMappedToBlue;
        params.valueMappedToBlue = f;
        params.isColorReversed = !params.isColorReversed;
      }
      params.rangeDefined = true;
      params.rangeAll = false;
      return true;
    }

    if ("reverseColor" == propertyName) {
      params.isColorReversed = true;
      return true;
    }

    if ("setColorScheme" == propertyName) {
      getSurfaceSets();
      params.colorBySets = true;
      mapSurface();
      return true;
    }

    if ("center" == propertyName) {
      params.center.setT((P3) value);
      return true;
    }

//    if ("volumeData" == propertyName) {
//      params.volumeData = (VolumeData) value;
//      return true;
//    }

    if ("origin" == propertyName) {
      params.origin = (P3) value;
      return true;
    }

    if ("step" == propertyName) {
      params.steps = (P3) value;
      return true;
    }

    if ("modelInvRotation" == propertyName) {
      params.modelInvRotation = (M4) value;
      return true;
    }

    if ("point" == propertyName) {
      params.points = (P3) value;
      return true;
    }

    if ("withinDistance" == propertyName) {
      params.distance = ((Float) value).floatValue();
      return true;
    }

    if ("withinPoint" == propertyName) {
      params.point = (P3) value;
      return true;
    }

    if ("progressive" == propertyName) {
      // an option for JVXL.java
      params.isXLowToHigh = true;
      return true;
    }

    if ("phase" == propertyName) {
      String color = (String) value;
      params.isCutoffAbsolute = true;
      params.colorBySign = true;
      params.colorByPhase = true;
      params.colorPhase = SurfaceReader.getColorPhaseIndex(color);
      if (params.colorPhase < 0) {
        Logger.warn(" invalid color phase: " + color);
        params.colorPhase = 0;
      }
      params.colorByPhase = params.colorPhase != 0;
      if (params.state >= Parameters.STATE_DATA_READ) {
        params.dataType = params.surfaceType;
        params.state = Parameters.STATE_DATA_COLORED;
        params.isBicolorMap = true;
        surfaceReader.applyColorScale();
      }
      return true;
    }

    /*
     * Based on the form of the parameters, returns and encoded radius as
     * follows:
     * 
     * script meaning range encoded
     * 
     * +1.2 offset [0 - 10] x -1.2 offset 0) x 1.2 absolute (0 - 10] x + 10 -30%
     * 70% (-100 - 0) x + 200 +30% 130% (0 x + 200 80% percent (0 x + 100
     * 
     * in each case, numbers can be integer or float
     */

    if ("radius" == propertyName) {
      Logger.info("solvent probe radius set to " + value);
      params.atomRadiusData = (RadiusData) value;
      return true;
    }

    if ("envelopeRadius" == propertyName) {
      params.envelopeRadius = ((Float) value).floatValue();
      return true;
    }

    if ("cavityRadius" == propertyName) {
      params.cavityRadius = ((Float) value).floatValue();
      return true;
    }

    if ("cavity" == propertyName) {
      params.isCavity = true;
      return true;
    }

    if ("doFullMolecular" == propertyName) {
      params.doFullMolecular = true;
      return true;
    }

    if ("pocket" == propertyName) {
      params.pocket = (Boolean) value;
      params.fullyLit = params.pocket.booleanValue();
      return true;
    }

    if ("minset" == propertyName) {
      params.minSet = ((Integer) value).intValue();
      return true;
    }

    if ("maxset" == propertyName) {
      params.maxSet = ((Integer) value).intValue();
      return true;
    }

    if ("plane" == propertyName) {
      params.setPlane((P4) value);
      return true;
    }

    if ("contour" == propertyName) {
      params.isContoured = true;
      int n;
      if (AU.isAF(value)) {
        // discrete values
        params.contoursDiscrete = (float[]) value;
        params.nContours = params.contoursDiscrete.length;
      } else if (value instanceof P3) {
        P3 pt = params.contourIncrements = (P3) value;
        float from = pt.x;
        float to = pt.y;
        float step = pt.z;
        if (step <= 0)
          step = 1;
        n = 0;
        for (float p = from; p <= to + step / 10; p += step, n++) {
        }
        params.contoursDiscrete = new float[n];
        float p = from;
        for (int i = 0; i < n; i++, p += step) {
          params.contoursDiscrete[i] = p;
        }
        params.nContours = n;
      } else {
        n = ((Integer) value).intValue();
        params.thisContour = 0; // flags as marked for JVXL reader
        if (n == 0)
          params.nContours = MarchingSquares.defaultContourCount;
        else if (n > 0)
          params.nContours = n;
        else
          params.thisContour = -n;
      }
      return true;
    }

    if ("colorDiscrete" == propertyName) {
      params.contourColixes = (short[]) value;
      return true;
    }

    if ("colorDensity" == propertyName) {
      params.colorDensity = true;
      if (value != null)
        params.pointSize = ((Float) value).floatValue();
      return false;
    }
    if ("fullPlane" == propertyName) {
      // fullPlane == true --> params.contourFromZero is false
      // fullPlane == false --> params.contourFromZero is true
      // this only relates to projections onto a plane
      // the default is contourFromZero TRUE
      // but MEP default is contourFromZero FALSE
      // the setting is ignored when discrete contours
      // are specified, because in that case we just
      // define the triangle color by their centers

      params.contourFromZero = !((Boolean) value).booleanValue();
      return true;
    }

    if ("mapLattice" == propertyName) {
      params.mapLattice = (P3) value;
      return true;
    }

    if ("extendGrid" == propertyName) {
      params.extendGrid = ((Float) value).floatValue();
      return true;
    }

    // / final actions ///

    if ("property" == propertyName) {
      params.dataType = Parameters.SURFACE_PROPERTY;
      params.theProperty = (float[]) value;
      mapSurface();
      return true;
    }

    // these next four set the reader themselves.
    if ("sphere" == propertyName) {
      params.setSphere(((Float) value).floatValue(), false);
      readerData = Float.valueOf(params.distance);
      surfaceReader = newReader("IsoShapeReader");
      generateSurface();
      return true;
    }

    // these next four set the reader themselves.
    if ("geodesic" == propertyName) {
      params.setSphere(((Float) value).floatValue(), true);
      readerData = Float.valueOf(params.distance);
      surfaceReader = newReader("IsoShapeReader");
      generateSurface();
      return true;
    }

    if ("ellipsoid" == propertyName) {
      if (value instanceof P4)
        params.setEllipsoidP4((P4) value);
      else if (AU.isAF(value))
        params.setEllipsoidAF((float[]) value);
      else
        return true;
      readerData = Float.valueOf(params.distance);
      surfaceReader = newReader("IsoShapeReader");
      generateSurface();
      return true;
    }

    if ("ellipsoid3" == propertyName) {
      params.setEllipsoidAF((float[]) value);
      readerData = Float.valueOf(params.distance);
      surfaceReader = newReader("IsoShapeReader");
      generateSurface();
      return true;
    }

    if ("lp" == propertyName) {
      params.setLp((P4) value);
      readerData = new float[] { 3, 2, 0, 15, 0 };
      surfaceReader = newReader("IsoShapeReader");
      generateSurface();
      return true;
    }

    if ("rad" == propertyName) {
      params.setRadical((P4) value);
      readerData = new float[] { 3, 2, 0, 15, 0 };
      surfaceReader = newReader("IsoShapeReader");
      generateSurface();
      return true;
    }

    if ("lobe" == propertyName) {
      params.setLobe((P4) value);
      readerData = new float[] { 3, 2, 0, 15, 0 };
      surfaceReader = newReader("IsoShapeReader");
      generateSurface();
      return true;
    }

    if ("hydrogenOrbital" == propertyName) {
      if (!params.setAtomicOrbital((float[]) value)) {
        isValid = false;
        return true;
      }
      readerData = new float[] { params.psi_n, params.psi_l, params.psi_m,
          params.psi_Znuc, params.psi_monteCarloCount };
      surfaceReader = newReader("IsoShapeReader");
      processState();
      return true;
    }

    if ("functionXY" == propertyName) {
      params.setFunctionXY((Lst<Object>) value);
      if (params.isContoured)      // xy plane through origin
        volumeDataTemp.setPlaneParameters(
            params.thePlane == null ? params.thePlane = P4.new4(0, 0, 1, 0)
            : params.thePlane); 
      if (((String) params.functionInfo.get(0)).indexOf("_xyz") >= 0)
        getFunctionZfromXY();
      processState();
      return true;
    }

    if ("functionXYZ" == propertyName) {
      params.setFunctionXYZ((Lst<Object>) value);
      processState();
      return true;
    }

    if ("lcaoType" == propertyName) {
      params.setLcao((String) value, colorPtr);
      return true;
    }

    if ("lcaoCartoonCenter" == propertyName) {
      if (++params.state != Parameters.STATE_DATA_READ)
        return true;
      if (Float.isNaN(params.center.x))
        params.center.setT((V3) value);
      return false;
    }

    if ("molecular" == propertyName || "solvent" == propertyName
        || "sasurface" == propertyName || "nomap" == propertyName) {
      params.setSolvent(propertyName, ((Float) value).floatValue());
      if (!params.isSilent)
        Logger.info(params.calculationType);
      processState();
      return true;
    }

    if ("moData" == propertyName) {
      params.moData = (Map<String, Object>) value;
      return true;
    }

    if ("mepCalcType" == propertyName) {
      params.mep_calcType = ((Integer) value).intValue();
      return true;
    }

    if ("mep" == propertyName) {
      params.setMep((float[]) value, false); // mep charges
      processState();
      return true;
    }

    if ("mlp" == propertyName) {
      params.setMep((float[]) value, true); // mlp charges
      processState();
      return true;
    }

    if ("nci" == propertyName) {
      boolean isPromolecular = ((Boolean) value).booleanValue();
      params.setNci(isPromolecular);
      if (isPromolecular)
        processState();
      return true;
    }

    if ("calculationType" == propertyName) {
      params.calculationType = (String) value;
      return true;
    }

    if ("charges" == propertyName) {
      params.theProperty = (float[]) value;
      return true;
    }

    if ("randomSeed" == propertyName) {
      // for any object requiring reproduction in the state
      // and using random numbers -- AO and MO
      params.randomSeed = ((Integer) value).intValue();
      return true;
    }

    if ("molecularOrbital" == propertyName) {
      int iMo = 0;
      float[] linearCombination = null;
      if (value instanceof Integer) {
        iMo = ((Integer) value).intValue();
      } else {
        linearCombination = (float[]) value;
      }
      params.setMO(iMo, linearCombination);
      Logger.info(params.calculationType);
      processState();
      return true;
    }

    if ("fileType" == propertyName) {
      fileType = (String) value;
      return true;
    }

    if ("fileName" == propertyName) {
      params.fileName = (String) value;
      return true;
    }

    if ("filesData" == propertyName) {
      params.filesData = (Object[]) value;
      return true;
    }

    if ("outputChannel" == propertyName) {
      out = (OC) value;
      return true;
    }

    if ("readFile" == propertyName) {
      if ((surfaceReader = setFileData((Viewer) atomDataServer, value)) == null) {
        Logger.error("Could not set the surface data");
        return true;
      }
      surfaceReader.setOutputChannel(out);
      generateSurface();
      return true;
    }

    if ("mapColor" == propertyName) {
      if ((surfaceReader = setFileData((Viewer) atomDataServer, value)) == null) {
        Logger.error("Could not set the mapping data");
        return true;
      }
      surfaceReader.setOutputChannel(out);
      mapSurface();
      return true;
    }

    if ("getSurfaceSets" == propertyName) {
      getSurfaceSets();
      return true;
    }

    if ("periodic" == propertyName) {
      params.isPeriodic = true;
    }

    // continue with operations in calling class...
    return false;
  }

  private SurfaceReader newReader(String name) {
    SurfaceReader sr = (SurfaceReader) getInterface(name);
    if (sr != null)
      sr.init(this);
    return sr;
  }

  private SurfaceReader newReaderBr(String name, BufferedReader br) {
    SurfaceFileReader sr = (SurfaceFileReader) getInterface(name);
    if (sr != null)
      sr.init2(this, br);
    return sr;
  }
  
  private static Object getInterface(String name) {
    try {
      Class<?> x = Class.forName("org.jmol.jvxl.readers." + name);
      return (x == null ? null : x.newInstance());
    } catch (Exception e) {
      Logger.error("Interface.java Error creating instance for " + name + ": \n" + e.toString());
      return null;
    }
  }

  private void getSurfaceSets() {
    if (meshDataServer == null) {
      meshData.getSurfaceSet();
    } else {
      meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES, null);
      meshData.getSurfaceSet();
      meshDataServer.fillMeshData(meshData, MeshData.MODE_PUT_SETS, null);
    }
  }

  private void processState() {   
    if (params.state == Parameters.STATE_INITIALIZED && params.thePlane != null)
      params.state++;
    if (params.state >= Parameters.STATE_DATA_READ) {
      mapSurface();
    } else {
      generateSurface();
    }
  }
  
  private boolean setReader() {
    readerData = null;
    if (surfaceReader != null)
      return !surfaceReader.vertexDataOnly;
    switch (params.dataType) {
    case Parameters.SURFACE_NOMAP:
      surfaceReader = newReader("IsoPlaneReader");
      break;
    case Parameters.SURFACE_PROPERTY:
      surfaceReader = newReader("AtomPropertyMapper");
      break;
    case Parameters.SURFACE_MEP:
    case Parameters.SURFACE_MLP:
      readerData = (params.dataType == Parameters.SURFACE_MEP ? "Mep" : "Mlp");
      if (params.state == Parameters.STATE_DATA_COLORED) {
        surfaceReader = newReader("AtomPropertyMapper");
      } else {
        surfaceReader = newReader("Iso" + readerData + "Reader");
      }
      break;
    case Parameters.SURFACE_INTERSECT_FILE:
      surfaceReader = newReader("IsoIntersectFileReader");
      break;
    case Parameters.SURFACE_INTERSECT_ATOM:
      surfaceReader = newReader("IsoIntersectAtomReader");
      break;
    case Parameters.SURFACE_SOLVENT:
    case Parameters.SURFACE_MOLECULAR:
    case Parameters.SURFACE_SASURFACE:
      surfaceReader = newReader("IsoSolventReader");
      break;
    case Parameters.SURFACE_NCI:
    case Parameters.SURFACE_MOLECULARORBITAL:
      surfaceReader = newReader("IsoMOReader");
      break;
    case Parameters.SURFACE_FUNCTIONXY:
      surfaceReader = newReader("IsoFxyReader");
      break;
    case Parameters.SURFACE_FUNCTIONXYZ:
      surfaceReader = newReader("IsoFxyzReader");
      break;
    }
    if (Logger.debugging)
      Logger.info("Using surface reader " + surfaceReader);
    if (params.isSilent && surfaceReader != null)
      surfaceReader.isQuiet = true;
    return true;
  }
  
  private void generateSurface() {       
    if (++params.state != Parameters.STATE_DATA_READ)
      return;
    setReader();    
    boolean haveMeshDataServer = (meshDataServer != null);
    if (params.colorBySign)
      params.isBicolorMap = true;
    if (surfaceReader == null) {
      Logger.error("surfaceReader is null for " + params.dataType);
      return;
    }
    if (!surfaceReader.createIsosurface(false)) {
      Logger.error("Could not create isosurface");
      params.cutoff = Float.NaN;
      surfaceReader.closeReader();
      return;
    }
    
    if (params.pocket != null && haveMeshDataServer)
      surfaceReader.selectPocket(!params.pocket.booleanValue());

    if (params.minSet > 0)
      surfaceReader.excludeMinimumSet();

    if (params.maxSet > 0)
      surfaceReader.excludeMaximumSet();

    if (params.slabInfo != null)
      surfaceReader.slabIsosurface(params.slabInfo);

    if (haveMeshDataServer && meshDataServer.notifySurfaceGenerationCompleted())
      surfaceReader.hasColorData = false;
    
    if (jvxlData.thisSet >= 0)
      getSurfaceSets();
    if (jvxlData.jvxlDataIs2dContour) {
      surfaceReader.colorIsosurface();
      params.state = Parameters.STATE_DATA_COLORED;
    }
    if (jvxlData.jvxlDataIsColorDensity) {
      params.state = Parameters.STATE_DATA_COLORED;
    }
    if (params.colorBySign || params.isBicolorMap) {
      params.state = Parameters.STATE_DATA_COLORED;
      surfaceReader.applyColorScale();
    }
    if (jvxlData.vertexColorMap != null) {
      jvxlData.vertexColorMap = null;
      surfaceReader.hasColorData = false;
    }
    surfaceReader.jvxlUpdateInfo();
    marchingSquares = surfaceReader.marchingSquares;
    surfaceReader.discardTempData(false);
    params.mappedDataMin = Float.MAX_VALUE;
    surfaceReader.closeReader();
    if (params.state != Parameters.STATE_DATA_COLORED &&
        (surfaceReader.hasColorData || params.colorDensity)) {
      params.state = Parameters.STATE_DATA_COLORED;
      colorIsosurface();
    }
    surfaceReader = null;
  }

  private void mapSurface() {
    if (params.state == Parameters.STATE_INITIALIZED && params.thePlane != null)
      params.state++;
    if (++params.state < Parameters.STATE_DATA_COLORED)
      return;
    if (!setReader())
      return;    
    //if (params.dataType == Parameters.SURFACE_FUNCTIONXY)
      //params.thePlane = new Point4f(0, 0, 1, 0);
    if (params.isPeriodic)
      surfaceReader.volumeData.isPeriodic = true;
    if (params.thePlane != null) {
      boolean isSquared = params.isSquared;
      params.isSquared = false;
      params.cutoff = 0;
      surfaceReader.volumeData.setMappingPlane(params.thePlane);
      surfaceReader.createIsosurface(!params.isPeriodic);//but don't read volume data yet
      surfaceReader.volumeData.setMappingPlane(null);
      if (meshDataServer != null)
        meshDataServer.notifySurfaceGenerationCompleted();
      if (params.dataType == Parameters.SURFACE_NOMAP) {
        // just a simple plane
        surfaceReader.discardTempData(true);
        return;
      }
      params.isSquared = isSquared;
      params.mappedDataMin = Float.MAX_VALUE;
      surfaceReader.readVolumeData(true);
      if (params.mapLattice != null)
        surfaceReader.volumeData.isPeriodic = true;
    } else if (!params.colorBySets && !params.colorDensity) {
      surfaceReader.readAndSetVolumeParameters(true);
      params.mappedDataMin = Float.MAX_VALUE;
      surfaceReader.readVolumeData(true);
    }
    colorIsosurface();
    surfaceReader.closeReader();
    surfaceReader = null;
  }

  void colorIsosurface() {
    surfaceReader.colorIsosurface();
    surfaceReader.jvxlUpdateInfo();
    surfaceReader.updateTriangles();
    surfaceReader.discardTempData(true);
    if (meshDataServer != null)
      meshDataServer.notifySurfaceMappingCompleted();
  }
  
  /**
   * only called from org.openscience.jvxl.Jvxl.main
   * 
   * @param property
   * @param index
   * @return Object
   */
  public Object getProperty(String property, int index) {
    if (property == "jvxlFileData")
      return JvxlCoder.jvxlGetFileVwr(null, jvxlData, null, params.title, "",
          true, index, null, null); // for Jvxl.java
    if (property == "jvxlFileInfo")
      return JvxlCoder.jvxlGetInfo(jvxlData); // for Jvxl.java
    return null;
  }

  /**
   * @param vwr  
   * @param value 
   * @return SurfaceReader
   */
  @SuppressWarnings("unchecked")
  private SurfaceReader setFileData(Viewer vwr, Object value) {
    // comes from "readFile" or "mapColors"
    String fileType = this.fileType;
    this.fileType = null;
    if (value instanceof Map) {
      Map<String, Object> map = (Map<String, Object>) value;
      if (map.containsKey("__pymolSurfaceData__")) {
        readerData = map;
        return newReaderBr("PyMOLMeshReader", null);
      }
      value = map.get("volumeData");      
    }
    if (value instanceof VolumeData) {
      // never implemented? 
      volumeDataTemp = (VolumeData) value;
      return newReader("VolumeDataReader");
    }
    String data = null;
    if (value instanceof String) {
      // inline string data
      data = (String) value;
      // this will be OK, because any string will be a simple string, 
      // not a binary file.
      value = Rdr.getBR((String) value);
    }
    if (value instanceof Object[]) {
      // [BufferedFileReader[], float[]] -> [VolumeFileReader[], float[]]
      Object[] a = (Object[]) ((Object[]) value)[0];
      VolumeFileReader[] b = new VolumeFileReader[a.length];
      for (int i = 0; i < a.length; i++)
        b[i] = (VolumeFileReader) setFileData(vwr, a[i]);
      ((Object[]) value)[0] = b;
      readerData = value;
      return newReader("IsoIntersectGridReader");
    }
    BufferedReader br = (BufferedReader) value;
    if (fileType == null)
      fileType = FileManager.determineSurfaceFileType(br);
    if (fileType != null && fileType.startsWith("UPPSALA")) {
      //"http://eds.bmc.uu.se/cgi-bin/eds/gen_maps_zip.pl?POST?pdbCode=1blu&mapformat=ccp4&maptype=2fofc&page=generate"
      // -- ah, but this does not work, because it is asynchronous!
      // -- first a message is sent back that suggests you might have to wait, 
      //    then a message that says, "Here is your map" is sent.

      String fname = params.fileName;
      fname = fname.substring(0, fname.indexOf("/", 10));
      fname += PT.getQuotedStringAt(fileType,
          fileType.indexOf("A HREF") + 1);
      params.fileName = fname;
      value = atomDataServer.getBufferedInputStream(fname);
      if (value == null) {
        Logger.error("Isosurface: could not open file " + fname);
        return null;
      }
      try {
        br = Rdr.getBufferedReader((BufferedInputStream) value, null);
      } catch (Exception e) {
        // TODO
      }
      fileType = FileManager.determineSurfaceFileType(br);
    }
    if (fileType == null)
      fileType = "UNKNOWN";
    Logger.info("data file type was determined to be " + fileType);
    if (fileType.equals("Jvxl+"))
      return newReaderBr("JvxlReader", br);
    
    readerData = new Object[] { params.fileName, data };

    if ("MRC DELPHI DSN6".indexOf(fileType.toUpperCase()) >= 0) {
      fileType += "Binary";
    }
    return newReaderBr(fileType + "Reader", br);
  }
  
  private Object readerData;
  
  Object getReaderData() {
    // needed by DelPhiBinary, Dsn6Binary, IsoShape, MrcBinary, Msms, Pmesh, CifDensity
    Object o = readerData;
    readerData = null;
    return o;
  }

  void initializeIsosurface() {
    params.initialize();
    colorPtr = 0;
    surfaceReader = null;
    marchingSquares = null;
    initState();
  }

  public void initState() {
    params.state = Parameters.STATE_INITIALIZED;
    params.dataType = params.surfaceType = Parameters.SURFACE_NONE;
  }

  public String setLcao() {
    params.colorPos = params.colorPosLCAO;
    params.colorNeg = params.colorNegLCAO;
    return params.lcaoType;

  }

  private void getFunctionZfromXY() {
    P3 origin = (P3) params.functionInfo.get(1);
    int[] counts = new int[3];
    int[] nearest = new int[3];
    V3[] vectors = new V3[3];
    for (int i = 0; i < 3; i++) {
      P4 info = (P4) params.functionInfo.get(i + 2);
      counts[i] = Math.abs((int) info.x);
      vectors[i] = V3.new3(info.y, info.z, info.w);
    }
    int nx = counts[0];
    int ny = counts[1];
    P3 pt = new P3();
    P3 pta = new P3();
    P3 ptb = new P3();
    P3 ptc = new P3();

    float[][] data = (float[][]) params.functionInfo.get(5);
    float[][] data2 = new float[nx][ny];
    float[] d;
    //int n = 0;
    //for (int i = 0; i < data.length; i++) 
      //System.out.println("draw pt"+(++n)+" {" + data[i][0] + " " + data[i][1] + " " + data[i][2] + "} color yellow");
    for (int i = 0; i < nx; i++)
      for (int j = 0; j < ny; j++) {
        pt.scaleAdd2(i, vectors[0], origin);
        pt.scaleAdd2(j, vectors[1], pt);
        float dist = findNearestThreePoints(pt.x, pt.y, data, nearest);
        pta.set((d = data[nearest[0]])[0], d[1], d[2]);
        if (dist < 0.00001) {
          pt.z = d[2];
        } else {
          ptb.set((d = data[nearest[1]])[0], d[1], d[2]);
          ptc.set((d = data[nearest[2]])[0], d[1], d[2]);
          pt.z = distanceVerticalToPlane(pt.x, pt.y, pta, ptb, ptc);
        }
        data2[i][j] = pt.z;
        //System.out.println("draw pt"+(++n)+" " + Escape.escape(pt) + " color red");
      }
    params.functionInfo.set(5, data2);
  }

  private final V3 vAB = new V3();
  private final V3 vNorm = new V3();
  private final P3 ptRef = P3.new3(0, 0, 1e15f);
  
  private float distanceVerticalToPlane(float x, float y, P3 pta,
                                              P3 ptb, P3 ptc) {
    // ax + by + cz + d = 0

    float d = Measure.getDirectedNormalThroughPoints(pta, ptb, ptc, ptRef, vNorm, vAB);
    return (vNorm.x * x + vNorm.y * y + d) / -vNorm.z;
  }
  
  private static float findNearestThreePoints(float x, float y, float[][] xyz, int[] result) {
    //result should be int[3];
    float d, dist1, dist2, dist3;
    int i1, i2, i3;
    i1 = i2 = i3 = -1;
    dist1 = dist2 = dist3 = Float.MAX_VALUE;
    for (int i = xyz.length; --i >= 0;) {
      d = (d = xyz[i][0] - x) * d + (d = xyz[i][1] - y) * d;
      if (d < dist1) {
        dist3 = dist2;
        dist2 = dist1;
        dist1 = d;
        i3 = i2;
        i2 = i1;
        i1 = i;
      } else if (d < dist2) {
        dist3 = dist2;
        dist2 = d;
        i3 = i2;
        i2 = i;
      } else if (d < dist3) {
        dist3 = d;
        i3 = i;
      }
    }
    //System.out.println("findnearest " + dist1);
    result[0] = i1;
    result[1] = i2;
    result[2] = i3;
    return dist1;
  }

  public void addRequiredFile(String fileName) {
    if (meshDataServer == null)
      return;
    meshDataServer.addRequiredFile(fileName);    
  }

  public void setRequiredFile(String oldName, String fileName) {
    if (meshDataServer == null)
      return;
    meshDataServer.setRequiredFile(oldName, fileName);    
  }

  void log(String msg) {
    if (atomDataServer == null)
      System.out.println(msg);
    else
      atomDataServer.log(msg);
  }

  void setOutputChannel(GenericBinaryDocument binaryDoc, OC out) {
    if (meshDataServer == null)
      return;
     meshDataServer.setOutputChannel(binaryDoc, out);    
  }

  void fillAtomData(AtomData atomData, int mode) {
    if ((mode & AtomData.MODE_FILL_RADII) != 0 
        && atomData.bsSelected != null) {
      if (bsVdw == null)
        bsVdw = new BS();
      bsVdw.or(atomData.bsSelected);
    }
    atomDataServer.fillAtomData(atomData, mode);
  }

  public V3[] getOriginVaVbVc() {
    return (surfaceReader.volumeData == null ? null : surfaceReader.volumeData.oabc);
  }

}
