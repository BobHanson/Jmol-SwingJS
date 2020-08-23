/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-12-03 14:51:57 -0600 (Sun, 03 Dec 2006) $
 * $Revision: 6372 $
 *
 * Copyright (C) 2005  Miguel, The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net, jmol-developers@lists.sf.net
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

package org.jmol.shapesurface;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.SB;

import java.util.Hashtable;

import java.util.Map;


import org.jmol.api.Interface;
import org.jmol.api.SymmetryInterface;
import org.jmol.util.C;
import org.jmol.util.ColorEncoder;
import org.jmol.util.GData;
import org.jmol.util.Logger;
import org.jmol.util.MeshSurface;
import org.jmol.util.SimpleUnitCell;

import javajs.util.CU;
import javajs.util.M4;
import javajs.util.P3;
import javajs.util.P3i;
import javajs.util.PT;
import javajs.util.T3;
import javajs.util.V3;
import org.jmol.viewer.Viewer;
import javajs.util.BS;
import org.jmol.jvxl.data.JvxlCoder;
import org.jmol.jvxl.data.JvxlData;
import org.jmol.jvxl.data.MeshData;

import org.jmol.jvxl.calc.MarchingSquares;
import org.jmol.modelset.Atom;
import org.jmol.script.T;
import org.jmol.shape.Mesh;

public class IsosurfaceMesh extends Mesh {
  public JvxlData jvxlData;
  public int vertexIncrement = 1;
  public int firstRealVertex = -1;
  public int dataType;
  public boolean hasGridPoints;
  Object calculatedArea;
  Object calculatedVolume;
  Object info;
  
  @Override
  public float getResolution() {
    return 1/jvxlData.pointsPerAngstrom;
  }

  /**
   * @param vwr 
   * @XXXXj2sIgnoreSuperConstructor
   * 
   * @param thisID
   * @param colix
   * @param index
   */
  IsosurfaceMesh(Viewer vwr, String thisID, short colix, int index) {
    mesh1(vwr, thisID, colix, index);
    jvxlData = new JvxlData();
    checkByteCount = 2;
    jvxlData.version = Viewer.getJmolVersion();
  }

  void clearType(String meshType, boolean iAddGridPoints) {
    clear(meshType);
    jvxlData.clear();
    assocGridPointMap = null;
    assocGridPointNormals = null;
    bsVdw = null;
    calculatedVolume = null;
    calculatedArea = null;
    centers = null;
    colorEncoder = null;
    colorPhased = false;
    colorsExplicit = false;
    firstRealVertex = -1;
    hasGridPoints = iAddGridPoints;
    isColorSolid = true;
    mergeAssociatedNormalCount = 0;
    nSets = 0;
    pcs = null;
    showPoints = iAddGridPoints;
    surfaceSet = null;
    vcs = null;
    vertexColorMap = null;
    vertexIncrement = 1;
    vertexSets = null;
    vvs = null;
  }

  void allocVertexColixes() {
    if (vcs == null) {
      vcs = new short[vc];
      for (int i = vc; --i >= 0;)
        vcs[i] = colix;
    }
    isColorSolid = false;
  }

  private Map<Integer, Integer> assocGridPointMap;
  private Map<Integer, V3> assocGridPointNormals;

  int addVertexCopy(T3 vertex, float value, int assocVertex,
                    boolean associateNormals, boolean asCopy) {
    int vPt = addVCVal(vertex, value, asCopy);
    switch (assocVertex) {
    case MarchingSquares.CONTOUR_POINT:
      if (firstRealVertex < 0)
        firstRealVertex = vPt;
      break;
    case MarchingSquares.VERTEX_POINT:
      hasGridPoints = true;
      break;
    case MarchingSquares.EDGE_POINT:
      vertexIncrement = 3;
      break;
    default:
      if (firstRealVertex < 0)
        firstRealVertex = vPt;
      if (associateNormals) {
        if (assocGridPointMap == null)
          assocGridPointMap = new Hashtable<Integer, Integer>();
        assocGridPointMap.put(Integer.valueOf(vPt), Integer.valueOf(assocVertex + mergeAssociatedNormalCount));
      }
    }
    return vPt;
  }

  @Override
  public void setTranslucent(boolean isTranslucent, float iLevel) {
    colix = C.getColixTranslucent3(colix, isTranslucent, iLevel);
    if (vcs != null)
      for (int i = vc; --i >= 0;)
        vcs[i] = C.getColixTranslucent3(vcs[i],
            isTranslucent, iLevel);
  }

  private int mergeAssociatedNormalCount;
  public void setMerged(boolean TF) {
    isMerged = TF;
    mergePolygonCount0 = (TF ? pc : 0);
    mergeVertexCount0 = (TF ? vc: 0);
    if (TF) {
      mergeAssociatedNormalCount += jvxlData.nPointsX * jvxlData.nPointsY * jvxlData.nPointsZ;
      assocGridPointNormals = null;
    }
  }


  @Override
  protected void sumVertexNormals(T3[] vertices, V3[] vectorSums) {
    sumVertexNormals2(this, vertices, vectorSums);
    /* 
     * OK, so if there is an associated grid point (because the 
     * point is so close to one), we now declare that associated
     * point to be used for the   vecetorSum instead of a new, 
     * independent one for the point itself.
     *  
     *  Bob Hanson, 05/2006
     *  
     *  having 2-sided normixes is INCOMPATIBLE with this when not a plane 
     *  
     */
    if (assocGridPointMap != null && vectorSums.length > 0 && !isMerged) {
      if (assocGridPointNormals == null)
        assocGridPointNormals = new Hashtable<Integer, V3>();
      for (Map.Entry<Integer, Integer> entry : assocGridPointMap.entrySet()) {
        // keys are indices into vertices[]
        // values are unique identifiers for a grid point
        Integer gridPoint = entry.getValue();
        if (!assocGridPointNormals.containsKey(gridPoint))
          assocGridPointNormals.put(gridPoint, V3.new3(0, 0, 0));
        assocGridPointNormals.get(gridPoint).add(vectorSums[entry.getKey().intValue()]);
      }
      for (Map.Entry<Integer, Integer> entry : assocGridPointMap.entrySet())
        vectorSums[entry.getKey().intValue()] = assocGridPointNormals.get(entry.getValue());
    }
  }

  P3[] centers;

  P3[] getCenters() {
    if (centers != null)
      return centers;
    centers = new P3[pc];
    for (int i = 0; i < pc; i++) {
      int[] p = pis[i];
      if (p == null)
        continue;
      P3 pt = centers[i] = P3.newP(vs[p[0]]);
      pt.add(vs[p[1]]);
      pt.add(vs[p[2]]);
      pt.scale(1 / 3f);
    }
    return centers;
  }

//  P4 getFacePlane(int i, V3 vNorm) {
//    return Measure.getPlaneThroughPoints(vs[pis[i][0]],
//        vs[pis[i][1]], vs[pis[i][2]], vNorm,
//        vAB, new P4());
//  }

  /**
   * create a set of contour data.
   * 
   * Each contour is a Vector containing: 0 Integer number of polygons (length
   * of BitSet) 1 BitSet of critical triangles 2 Float value 3 int[] [colorArgb]
   * 4 StringXBuilder containing encoded data for each segment: char type ('3',
   * '6', '5') indicating which two edges of the triangle are connected: '3'
   * 0x011 AB-BC '5' 0x101 AB-CA '6' 0x110 BC-CA char fraction along first edge
   * (jvxlFractionToCharacter) char fraction along second edge
   * (jvxlFractionToCharacter) 5- stream of pairs of points for rendering
   * 
   * @return contour vector set
   */
  @SuppressWarnings("unchecked")
  public
  Lst<Object>[] getContours() {
    int n = jvxlData.nContours;
    if (n == 0 || pis == null)
      return null;
    havePlanarContours = (jvxlData.jvxlPlane != null);
    if (havePlanarContours)
      return null; // not necessary; 
    if (n < 0)
      n = -1 - n;
    Lst<Object>[] vContours = jvxlData.vContours;
    if (vContours != null) {
      for (int i = 0; i < n; i++) {
        if (vContours[i].size() > JvxlCoder.CONTOUR_POINTS)
          return jvxlData.vContours;
        JvxlCoder.set3dContourVector(vContours[i], pis, vs);
      }
      //dumpData();
      return jvxlData.vContours;
    }
    //dumpData();
    vContours = new Lst[n];
    for (int i = 0; i < n; i++) {
      vContours[i] = new  Lst<Object>();
    }
    if (jvxlData.contourValuesUsed == null) {
      float dv = (jvxlData.valueMappedToBlue - jvxlData.valueMappedToRed)
          / (n + 1);
      // n + 1 because we want n lines between n + 1 slices
      for (int i = 0; i < n; i++) {
        float value = jvxlData.valueMappedToRed + (i + 1) * dv;
        get3dContour(this, vContours[i], value, jvxlData.contourColixes[i]);
      }
      Logger.info(n + " contour lines; separation = " + dv);
    } else {
      for (int i = 0; i < n; i++) {
        float value = jvxlData.contourValuesUsed[i];
        get3dContour(this, vContours[i], value, jvxlData.contourColixes[i]);
      }
    }
    jvxlData.contourColixes = new short[n];
    jvxlData.contourValues = new float[n];
    for (int i = 0; i < n; i++) {
      jvxlData.contourValues[i] = ((Float) vContours[i].get(2)).floatValue();
      jvxlData.contourColixes[i] = ((short[]) vContours[i].get(3))[0];
    }
    return jvxlData.vContours = vContours;
  }

  public Object getPmeshData(boolean isBinary) {
    PMeshWriter mw = (PMeshWriter) Interface.getInterface("org.jmol.shapesurface.PMeshWriter", vwr, "script");
    return mw.write(this, isBinary);
  }

  private static void get3dContour(IsosurfaceMesh m, Lst<Object> v, float value, short colix) {
    BS bsContour = BS.newN(m.pc);
    SB fData = new SB();
    int color = C.getArgb(colix);
    setContourVector(v, m.pc, bsContour, value, colix, color, fData);
    for (int i = 0; i < m.pc; i++)
      if (m.setABC(i) != null)
        addContourPoints(v, bsContour, i, fData, m.vs, m.vvs, m.iA,
            m.iB, m.iC, value);
  }

  public static void setContourVector(Lst<Object> v, int nPolygons,
                                      BS bsContour, float value,
                                      short colix, int color, SB fData) {
    v.add(JvxlCoder.CONTOUR_NPOLYGONS, Integer.valueOf(nPolygons));
    v.add(JvxlCoder.CONTOUR_BITSET, bsContour);
    v.add(JvxlCoder.CONTOUR_VALUE, Float.valueOf(value));
    v.add(JvxlCoder.CONTOUR_COLIX, new short[] { colix });
    v.add(JvxlCoder.CONTOUR_COLOR, new int[] { color });
    v.add(JvxlCoder.CONTOUR_FDATA, fData);
  }

  public static void addContourPoints(Lst<Object> v, BS bsContour, int i,
                                      SB fData, T3[] vertices,
                                      float[] vertexValues, int iA, int iB,
                                      int iC, float value) {
    P3 pt1 = null;
    P3 pt2 = null;
    int type = 0;
    // check AB
    float f1 = checkPt(vertexValues, iA, iB, value);
    if (!Float.isNaN(f1)) {
      pt1 = getContourPoint(vertices, iA, iB, f1);
      type |= 1;
    }
    // check BC only if v not found only at B already in testing AB
    float f2 = (f1 == 1 ? Float.NaN : checkPt(vertexValues, iB, iC, value));
    if (!Float.isNaN(f2)) {
      pt2 = getContourPoint(vertices, iB, iC, f2);
      if (type == 0) {
        pt1 = pt2;
        f1 = f2;
      }
      type |= 2;
    }
    // only check CA under certain circumstances
    switch (type) {
    case 0:
      return; // not in AB or BC, so ignore
    case 1:
      if (f1 == 0)
        return; //because at A and not along BC, so only at A
      //$FALL-THROUGH$
    case 2:
      // check CA only if v not found only at C already in testing BC
      f2 = (f2 == 1 ? Float.NaN : checkPt(vertexValues, iC, iA, value));
      if (!Float.isNaN(f2)) {
        pt2 = getContourPoint(vertices, iC, iA, f2);
        type |= 4;
      }
      break;
    }
    // only types AB-BC, AB-CA, or BC-CA are valid intersections
    switch (type) {
    case 3:
    case 5:
    case 6:
      break;
    default:
      return;
    }
    bsContour.set(i);
    JvxlCoder.appendContourTriangleIntersection(type, f1, f2, fData);
    v.addLast(pt1);
    v.addLast(pt2);
  }

  /**
   * two values -- v1, and v2, which need not be ordered v1 < v2. v == v1 --> 0
   * v == v2 --> 1 v1 < v < v2 --> f in (0,1) v2 < v < v1 --> f in (0,1) i.e.
   * (v1 < v) == (v < v2)
   * 
   * We check AB, then (usually) BC, then (sometimes) CA.
   * 
   * What if two end points are identical values? So, for example, if v = 1.0
   * and:
   * 
   * A 1.0 0.5 1.0 1.0 / \ / \ / \ / \ / \ / \ / \ / \ / \ / \ C-----B 1.0--0.5
   * 1.0--1.0 0.5--1.0 1.0---1.0 case I case II case III case IV
   * 
   * case I: AB[0] and BC[1], type == 3 --> CA not tested. case II: AB[1] and
   * CA[0]; f1 == 1.0 --> BC not tested. case III: AB[0] and BC[0], type == 3
   * --> CA not tested. case IV: AB[0] and BC[0], type == 3 --> CA not tested.
   * 
   * what if v = 0.5?
   * 
   * case I: AB[1]; BC not tested --> type == 1, invalid. case II: AB[0]; type
   * == 1, f1 == 0.0 --> CA not tested. case III: BC[1]; f2 == 1.0 --> CA not
   * tested.
   * 
   * @param vertexValues
   * @param i
   * @param j
   * @param v
   * @return fraction along the edge or NaN
   */
  private static float checkPt(float[] vertexValues, int i, int j, float v) {
    float v1, v2;
    return (v == (v1 = vertexValues[i]) ? 0 : v == (v2 = vertexValues[j]) ? 1
        : (v1 < v) == (v < v2) ? (v - v1) / (v2 - v1) : Float.NaN);
  }

  private static P3 getContourPoint(T3[] vertices, int i, int j,
                                         float f) {
    P3 pt = new P3();
    pt.sub2(vertices[j], vertices[i]);
    pt.scaleAdd2(f, pt, vertices[i]);
    return pt;
  }

  float[] contourValues;
  short[] contourColixes;
  public ColorEncoder colorEncoder;
  
  BS bsVdw;
  public boolean colorPhased;
  public float[] probeValues;

  public void setDiscreteColixes(float[] values, short[] colixes) {
    if (values != null)
      jvxlData.contourValues = values;
    if (values == null || values.length == 0)
      values = jvxlData.contourValues = jvxlData.contourValuesUsed;
    if (colixes == null && jvxlData.contourColixes != null) {
      colixes = jvxlData.contourColixes;
    } else {
      jvxlData.contourColixes = colixes;
      jvxlData.contourColors = C.getHexCodes(colixes);
    }
    if (vs == null || vvs == null || values == null)
      return;
    int n = values.length;
    float vMax = values[n - 1];
    colorCommand = null;
    boolean haveColixes = (colixes != null && colixes.length > 0);
    isColorSolid = (haveColixes && jvxlData.jvxlPlane != null);
    if (jvxlData.vContours != null) {
      if (haveColixes)
        for (int i = 0; i < jvxlData.vContours.length; i++) {
          short colix = colixes[i % colixes.length];
          ((short[]) jvxlData.vContours[i].get(JvxlCoder.CONTOUR_COLIX))[0] = colix;
          ((int[]) jvxlData.vContours[i].get(JvxlCoder.CONTOUR_COLOR))[0] = C
              .getArgb(colix);
        }
      return;
    }
    short defaultColix = 0;
    pcs = new short[pc];
    colorsExplicit = false;
    for (int i = 0; i < pc; i++) {
      int[] p = pis[i];
      if (p == null)
        continue;
      pcs[i] = defaultColix;
      float v = (vvs[p[0]] + vvs[p[1]] + vvs[p[2]]) / 3;
      for (int j = n; --j >= 0;) {
        if (v >= values[j] && v < vMax) {
          pcs[i] = (haveColixes ? colixes[j % colixes.length] : 0);
          break;
        }
      }
    }
  }

  /**
   * 
   * @param vwr
   * @return a Hashtable containing "values" and "colors"
   * 
   */
  Map<String, Object> getContourList(Viewer vwr) {
    Map<String, Object> ht = new Hashtable<String, Object>();
    ht.put("values",
        (jvxlData.contourValuesUsed == null ? jvxlData.contourValues
            : jvxlData.contourValuesUsed));
    Lst<P3> colors = new  Lst<P3>();
    if (jvxlData.contourColixes != null) {
      // set in SurfaceReader.colorData()
      for (int i = 0; i < jvxlData.contourColixes.length; i++) {
        colors.addLast(CU.colorPtFromInt(C
            .getArgb(jvxlData.contourColixes[i]), null));
      }
      ht.put("colors", colors);
    }
    return ht;
  }

  void deleteContours() {
    jvxlData.contourValuesUsed = null;
    jvxlData.contourValues = null;
    jvxlData.contourColixes = null;
    jvxlData.vContours = null;
  }

  void setVertexColorMap() {
    vertexColorMap = new Hashtable<String, BS>();
    short lastColix = -999;
    BS bs = null;
    for (int i = vc; --i >= 0;) {
      short c = vcs[i];
      if (c != lastColix) {
        String color = C.getHexCode(lastColix = c);
        bs = vertexColorMap.get(color);
        if (bs == null)
          vertexColorMap.put(color, bs = new BS());
      }
      bs.set(i);
    }
  }

  void setVertexColixesForAtoms(Viewer vwr, short[] colixes, int[] atomMap,
                                BS bs) {
    jvxlData.vertexDataOnly = true;
    jvxlData.vertexColors = new int[vc];
    jvxlData.nVertexColors = vc;
    Atom[] atoms = vwr.ms.at;
    GData gdata = vwr.gdata;
    for (int i = mergeVertexCount0; i < vc; i++) {
      int iAtom = vertexSource[i];
      if (iAtom < 0 || !bs.get(iAtom))
        continue;
      jvxlData.vertexColors[i] = gdata.getColorArgbOrGray(vcs[i] = C
          .copyColixTranslucency(colix, atoms[iAtom].colixAtom));

      short colix = (colixes == null ? C.INHERIT_ALL : colixes[atomMap[iAtom]]);
      if (colix == C.INHERIT_ALL)
        colix = atoms[iAtom].colixAtom;
      vcs[i] = C.copyColixTranslucency(this.colix, colix);
    }
  }

  /**
   * color a specific set of vertices a specific color
   * 
   * @param colix
   * @param bs
   * @param isAtoms
   */
  void colorVertices(short colix, BS bs, boolean isAtoms) {
    if (vertexSource == null)
      return;
    colix = C.copyColixTranslucency(this.colix, colix);
    BS bsVertices = (isAtoms ? new BS() : bs);
    checkAllocColixes();
    // TODO: color translucency?
    if (isAtoms)
      for (int i = 0; i < vc; i++) {
        int pt = vertexSource[i]; 
        if (pt >= 0 && bs.get(pt)) {
          vcs[i] = colix;
          if (bsVertices != null)
            bsVertices.set(i);
        }
      }
    else
      for (int i = 0; i < vc; i++)
        if (bsVertices.get(i))
          vcs[i] = colix;

    if (!isAtoms) {
      // JVXL file color maps do not have to be saved here. 
      // They are just kept in jvxlData 
      return;
    }
    String color = C.getHexCode(colix);
    if (vertexColorMap == null)
      vertexColorMap = new Hashtable<String, BS>();
    addColorToMap(vertexColorMap, color, bs);
  }

  void checkAllocColixes() {
    if (vcs == null || vertexColorMap == null && isColorSolid)
      allocVertexColixes();
    isColorSolid = false;
  }

  /**
   * adds a set of specifically-colored vertices to the map, 
   * ensuring that no vertex is in two maps.
   * 
   * @param colorMap
   * @param color
   * @param bs
   */
  private static void addColorToMap(Map<String, BS> colorMap, String color,
                                    BS bs) {
    BS bsMap = null;
    for (Map.Entry<String, BS> entry : colorMap.entrySet())
      if (entry.getKey() == color) {
        bsMap = entry.getValue();
        bsMap.or(bs);
      } else {
        entry.getValue().andNot(bs);
      }
    if (bsMap == null)
      colorMap.put(color, bs);
  }

  /**
   * set up the jvxlData fields needed for either just the 
   * header (isAll = false) or the full file (isAll = true)
   * 
   * @param isAll
   */
  void setJvxlColorMap(boolean isAll) {
    jvxlData.diameter = diameter;
    jvxlData.color = C.getHexCode(colix);
    jvxlData.meshColor = (meshColix == 0 ? null : C.getHexCode(meshColix));
    jvxlData.translucency = ((colix & C.TRANSLUCENT_SCREENED) == C.TRANSLUCENT_SCREENED ? -1 : C.getColixTranslucencyFractional(colix));
    jvxlData.rendering = getRendering().substring(1);
    jvxlData.colorScheme = (colorEncoder == null ? null : colorEncoder
        .getColorScheme());
    if (jvxlData.vertexColors == null)
      jvxlData.nVertexColors = (vertexColorMap == null ? 0 : vertexColorMap
          .size());
    if (vertexColorMap == null || vertexSource == null || !isAll)
      return;
    if (jvxlData.vertexColorMap == null)
      jvxlData.vertexColorMap = new Hashtable<String, BS>();
    for (Map.Entry<String, BS> entry : vertexColorMap.entrySet()) {
      BS bsMap = entry.getValue();
      if (bsMap.isEmpty())
        continue;
      String color = entry.getKey();
      BS bs = new BS();
      for (int i = 0; i < vc; i++)
        if (bsMap.get(vertexSource[i]))
          bs.set(i);
      addColorToMap(jvxlData.vertexColorMap, color, bs);
    }
    jvxlData.nVertexColors = jvxlData.vertexColorMap.size();
    if (jvxlData.vertexColorMap.size() == 0)
      jvxlData.vertexColorMap = null;
  }

  /**
   *  just sets the color command for this isosurface. 
   */
  void setColorCommand() {
    if (colorEncoder == null || (colorCommand = colorEncoder.getColorScheme()) == null)
      return;
    if (colorCommand.equals("inherit")) {
      colorCommand = "#inherit;";
      return;
    }
    colorCommand = "color $"  + PT.esc(thisID)  + PT.esc(colorCommand) + " range "
        + (jvxlData.isColorReversed ? jvxlData.valueMappedToBlue + " "
            + jvxlData.valueMappedToRed : jvxlData.valueMappedToRed + " "
            + jvxlData.valueMappedToBlue);
  }


  /**
   * from Isosurface.notifySurfaceGenerationCompleted()
   * 
   * starting with Jmol 12.1.50, JVXL files contain color, translucency, color
   * scheme information, and vertex color mappings (as from COLOR ISOSURFACE
   * {hydrophobic} WHITE), returning these settings when the JVXL file is
   * opened.
   * 
   * @param colorRgb
   * @return true if still need color handling
   */
  boolean setColorsFromJvxlData(int colorRgb) {
    diameter = jvxlData.diameter;
    if (colorRgb == -1) {
    } else if (colorRgb != Integer.MIN_VALUE && colorRgb != Integer.MAX_VALUE) {
      // max value set when second color option given in isosurface command
      colix = C.getColix(colorRgb);
    } else if (jvxlData.color != null) {
      colix = C.getColixS(jvxlData.color);
    }
    if (colix == 0)
      colix = C.ORANGE;
    colix = C.getColixTranslucent3(colix, jvxlData.translucency != 0,
        jvxlData.translucency);
    float translucencyLevel = (jvxlData.translucency == 0 ? Float.NaN
        : jvxlData.translucency);
    if (jvxlData.meshColor != null)
      meshColix = C.getColixS(jvxlData.meshColor);
    setJvxlDataRendering();

    isColorSolid = !jvxlData.isBicolorMap && jvxlData.vertexColors == null
        && jvxlData.vertexColorMap == null;
    if (colorEncoder == null)
      return false;
    // bicolor map will be taken care of with params.isBicolorMap
    if (jvxlData.vertexColorMap == null) {
      if (jvxlData.colorScheme != null) {
        String colorScheme = jvxlData.colorScheme;
        boolean isTranslucent = colorScheme.startsWith("translucent ");
        if (isTranslucent) {
          colorScheme = colorScheme.substring(12);
          translucencyLevel = Float.NaN;
        }
        colorEncoder.setColorScheme(colorScheme, isTranslucent);
        remapColors(null, null, translucencyLevel);
      }
    } else {
      if (jvxlData.baseColor != null) {
        for (int i = vc; --i >= 0;)
          vcs[i] = colix;
      }
      for (Map.Entry<String, BS> entry : jvxlData.vertexColorMap.entrySet()) {
        BS bsMap = entry.getValue();
        short colix = C.copyColixTranslucency(this.colix,
            C.getColixS(entry.getKey()));
        for (int i = bsMap.nextSetBit(0); i >= 0; i = bsMap.nextSetBit(i + 1))
          vcs[i] = colix;
      }
    }
    return true;
  }

  void setJvxlDataRendering() {
    if (jvxlData.rendering != null) {
      String[] tokens = PT.getTokens(jvxlData.rendering);
      for (int i = 0; i < tokens.length; i++)
        setTokenProperty(T.getTokFromName(tokens[i]), true);
    }
  }

  /**
   * remaps colors based on a new color scheme or translucency level
   * @param vwr 
   * 
   * @param ce
   * @param translucentLevel
   */
  void remapColors(Viewer vwr, ColorEncoder ce, float translucentLevel) {
    if (ce == null)
      ce = colorEncoder;
    if (ce == null)
      ce = colorEncoder = new ColorEncoder(null, vwr);
    colorEncoder = ce;
    setColorCommand();
    if (Float.isNaN(translucentLevel)) {
      translucentLevel = C.getColixTranslucencyLevel(colix);
    } else {
      colix = C.getColixTranslucent3(colix, true, translucentLevel);
    }
    float min = ce.lo;
    float max = ce.hi;
    boolean inherit = (vertexSource != null && ce.currentPalette == ColorEncoder.INHERIT);
    vertexColorMap = null;
    pcs = null;
    colorsExplicit = false;
    jvxlData.baseColor = null;
    jvxlData.vertexCount = vc;
    if (vvs == null || jvxlData.vertexCount == 0)
      return;
    if (vcs == null || vcs.length != vc)
      allocVertexColixes();
    if (inherit) {
      jvxlData.vertexDataOnly = true;
      jvxlData.vertexColors = new int[vc];
      jvxlData.nVertexColors = vc;
      Atom[] atoms = vwr.ms.at;
      GData gdata = vwr.gdata;
      for (int i = mergeVertexCount0; i < vc; i++) {
        int pt = vertexSource[i];
        if (pt >= 0 && pt < atoms.length)
          jvxlData.vertexColors[i] = gdata.getColorArgbOrGray(vcs[i] = C.copyColixTranslucency(colix,
            atoms[pt].colixAtom));
      }
      return;
    }
    jvxlData.vertexColors = null;
    jvxlData.vertexColorMap = null;
    if (jvxlData.isBicolorMap) {
      for (int i = mergeVertexCount0; i < vc; i++)
        vcs[i] = C.copyColixTranslucency(colix,
            vvs[i] < 0 ? jvxlData.minColorIndex
                : jvxlData.maxColorIndex);
      return;
    }
    jvxlData.isColorReversed = ce.isReversed;
    if (max != Float.MAX_VALUE) {
      jvxlData.valueMappedToRed = min;
      jvxlData.valueMappedToBlue = max;
    }
    ce.setRange(jvxlData.valueMappedToRed, jvxlData.valueMappedToBlue,
        jvxlData.isColorReversed);
    // colix must be translucent if the scheme is translucent
    // but may be translucent if the scheme is not translucent
    boolean isTranslucent = C.isColixTranslucent(colix);
    if (ce.isTranslucent) {
      if (!isTranslucent)
        colix = C.getColixTranslucent3(colix, true, 0.5f);
      // still, if the scheme is translucent, we don't want to color the vertices translucent
      isTranslucent = false;
    }
    vcs = AU.ensureLengthShort(vcs, vc); // when reading contour plane may be remapped
    for (int i = vc; --i >= mergeVertexCount0;)
      vcs[i] = ce.getColorIndex(vvs[i]);
    setTranslucent(isTranslucent, translucentLevel);
    colorEncoder = ce;
    Lst<Object>[] contours = getContours();
    if (contours != null) {
      for (int i = contours.length; --i >= 0;) {
        float value = ((Float) contours[i].get(JvxlCoder.CONTOUR_VALUE))
            .floatValue();
        short[] colix = ((short[]) contours[i].get(JvxlCoder.CONTOUR_COLIX));
        colix[0] = ce.getColorIndex(value);
        int[] color = ((int[]) contours[i].get(JvxlCoder.CONTOUR_COLOR));
        color[0] = C.getArgb(colix[0]);
      }
    }
    if (contourValues != null) {
      contourColixes = new short[contourValues.length];
      for (int i = 0; i < contourValues.length; i++)
        contourColixes[i] = ce.getColorIndex(contourValues[i]);
      setDiscreteColixes(null, null);
    }
    jvxlData.isJvxlPrecisionColor = true;
    JvxlCoder.jvxlCreateColorData(jvxlData, vvs);
    setColorCommand();
    isColorSolid = false;
  }

  public void reinitializeLightingAndColor(Viewer vwr) {
    initialize(lighting, null, null);
    if (colorEncoder != null || jvxlData.isBicolorMap) {
      vcs = null;
      remapColors(vwr, null, Float.NaN);
    }
  }

  @Override
  public P3[] getBoundingBox() {
    return jvxlData.boundingBox;
  }

  @Override
  public void setBoundingBox(P3[] pts) {
    jvxlData.boundingBox = pts;
  }
  
  //private void dumpData() {
  //for (int i =0;i<10;i++) {
  //  System.out.println("P["+i+"]="+polygonIndexes[i][0]+" "+polygonIndexes[i][1]+" "+polygonIndexes[i][2]+" "+ polygonIndexes[i][3]+" "+vertices[i]);
  //}
  //}
  
  protected void merge(MeshData m) {
    int nV = vc + (m == null ? 0 : m.vc);
    if (pis == null)
      pis = new int[0][0];
    if (m != null && m.pis == null)
      m.pis = new int[0][0];
    int nP = (bsSlabDisplay == null || pc == 0 ? pc : bsSlabDisplay
        .cardinality())
        + (m == null || m.pc == 0 ? 0 : m.bsSlabDisplay == null ? m.pc
            : m.bsSlabDisplay.cardinality());
    if (vs == null)
      vs = new P3[0];
    vs = (T3[]) AU.ensureLength(vs, nV);
    vvs = AU.ensureLengthA(vvs, nV);
    boolean haveSources = (vertexSource != null && (m == null || m.vertexSource != null));
    vertexSource = AU.ensureLengthI(vertexSource, nV);
    int[][] newPolygons = AU.newInt2(nP);
    // note -- no attempt here to merge vertices
    int ipt = mergePolygons(this, 0, 0, newPolygons);
    if (m != null) {
      ipt = mergePolygons(m, ipt, vc, newPolygons);
      for (int i = 0; i < m.vc; i++, vc++) {
        vs[vc] = m.vs[i];
        vvs[vc] = m.vvs[i];
        if (haveSources)
          vertexSource[vc] = m.vertexSource[i];
      }
    }
    pc = polygonCount0 = nP;
    vc = vertexCount0 = nV;
    if (nP > 0)
      resetSlab();
    pis = newPolygons;
  }

  private static int mergePolygons(MeshSurface m, int ipt, int vertexCount, int[][] newPolygons) {
    int[] p;
    for (int i = 0; i < m.pc; i++) {
      if ((p = m.pis[i]) == null || m.bsSlabDisplay != null && !m.bsSlabDisplay.get(i))
        continue;
      newPolygons[ipt++] = m.pis[i];
      if (vertexCount > 0)
        for (int j = 0; j < 3; j++)
          p[j] += vertexCount;
    }
    //System.out.println("isosurfaceMesh mergePolygons " + m.polygonCount + " " + m.polygonIndexes.length);
    return ipt;
  }

  @Override
  public SymmetryInterface getUnitCell() {
    return (unitCell != null
        || (unitCell = vwr.ms.am[modelIndex].biosymmetry) != null
        || (unitCell = vwr.ms.getUnitCell(modelIndex)) != null
        || oabc != null && (unitCell = Interface.getSymmetry(vwr, "symmetry").getUnitCell(
            oabc, true, null)) != null ? unitCell : null);
  }

  void fixLattice() {
    if (getUnitCell() == null)
      return;
    P3i minXYZ = new P3i();
    P3i maxXYZ = P3i.new3((int) lattice.x, (int) lattice.y, (int) lattice.z);
    jvxlData.fixedLattice = lattice;
    lattice = null;
    SimpleUnitCell.setMinMaxLatticeParameters((int) unitCell.getUnitCellInfoType(SimpleUnitCell.INFO_DIMENSIONS), minXYZ, maxXYZ, 0);
    int nCells = (maxXYZ.x - minXYZ.x) * (maxXYZ.y - minXYZ.y)
        * (maxXYZ.z - minXYZ.z);
    P3 latticeOffset = new P3();
    int vc0 = vc;
    int vcNew = nCells * vc;
    vs = AU.arrayCopyPt(vs, vcNew);
    vvs = (vvs == null ? null : AU.ensureLengthA(vvs, vcNew));
    int pc0 = pc;
    int pcNew = nCells * pc;
    pis = AU.arrayCopyII(pis, pcNew);
    int off = 0;
    normixes = AU.arrayCopyShort(normixes, vcNew);    
    for (int tx = minXYZ.x; tx < maxXYZ.x; tx++)
      for (int ty = minXYZ.y; ty < maxXYZ.y; ty++)
        for (int tz = minXYZ.z; tz < maxXYZ.z; tz++) {
          if (tx == 0 && ty == 0 && tz == 0)
            continue;
          latticeOffset.set(tx, ty, tz);
          unitCell.toCartesian(latticeOffset, false);
          for (int i = 0; i < vc0; i++) {
            normixes[vc] = normixes[i];
            P3 v = P3.newP(vs[i]);
            v.add(latticeOffset);
            addVCVal(v, vvs[i], false);
          }
          off += vc0;
          for (int i = 0; i < pc0; i++) {
            int[] p = AU.arrayCopyI(pis[i], -1);
            p[0] += off;
            p[1] += off;
            p[2] += off;
            addPolygon(p, null);
          }
        }
    P3 xyzMin = new P3();
    P3 xyzMax = new P3();
    setBox(xyzMin, xyzMax);
    jvxlData.boundingBox = new P3[] { xyzMin, xyzMax };    

  }

  @Override
  protected float getMinDistance2ForVertexGrouping() {
    if (jvxlData.boundingBox != null && jvxlData.boundingBox[0] != null) {
      float d2 = jvxlData.boundingBox[1]
          .distanceSquared(jvxlData.boundingBox[0]);
      if (d2 < 5)
        return 1e-10f;
    }
    return 1e-8f;
  }

  @Override
  public BS getVisibleVertexBitSet() {
    BS bs = getVisibleVBS();
    if (jvxlData.thisSet >= 0)
      for (int i = 0; i < vc; i++)
        if (vertexSets[i] != jvxlData.thisSet)
          bs.clear(i);
   return bs;
  }

  /**
   * 
   * bs will be null if this is a set from the new isosurface MOVE [mat4]
   * command
   * 
   * @param m
   * @param bs
   */
  public void updateCoordinates(M4 m, BS bs) {
    boolean doUpdate = (bs == null || isModelConnected);
    if (!doUpdate)
      for (int i = 0; i < connectedAtoms.length; i++)
        if (connectedAtoms[i] >= 0 && bs.get(connectedAtoms[i])) {
          doUpdate = true;
          break;
        }
    if (!doUpdate)
      return;
    if (isModelConnected) {
      mat4 = vwr.ms.am[modelIndex].mat4;
    } else {
      if (mat4 == null)
        mat4 = M4.newM4(null);
      mat4.mul2(m, mat4);
    }
    recalcAltVertices = true;
  }

  float[] getDataMinMax() { 
    float min = Float.MAX_VALUE;
    float max = Float.MIN_VALUE;
    for (int i = vvs.length; --i >= 0;) {
      float v = vvs[i];
      if (v < min)
        min = v;
      if (v > max)
        max = v;      
    }
    return new float[] { min, max };
  }
  
  float[] getDataRange() {
    return (jvxlData.jvxlPlane != null
        && colorEncoder == null ? null : new float[] {
        jvxlData.mappedDataMin,
        jvxlData.mappedDataMax,
        (jvxlData.isColorReversed ? jvxlData.valueMappedToBlue
            : jvxlData.valueMappedToRed),
        (jvxlData.isColorReversed ? jvxlData.valueMappedToRed
            : jvxlData.valueMappedToBlue) });
  }

}
