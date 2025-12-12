/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-29 04:39:40 -0500 (Thu, 29 Mar 2007) $
 * $Revision: 7248 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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

package org.jmol.shapespecial;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.M4d;
import javajs.util.MeasureD;
import javajs.util.P3d;
import javajs.util.P4d;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.V3d;

import org.jmol.api.AtomIndexIterator;
import org.jmol.api.Interface;
import org.jmol.api.SmilesMatcherInterface;
import org.jmol.api.SymmetryInterface;
import org.jmol.c.PAL;
import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.script.SV;
import org.jmol.script.T;
import org.jmol.shape.AtomShape;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Logger;
import org.jmol.util.MeshCapper;
import org.jmol.util.Normix;
import org.jmol.viewer.JC;

public class Polyhedra extends AtomShape implements Comparator<Object[]>{

  
  @Override
  public int compare(Object[] a, Object[] b) {
    double da = (a[0] == null  ? Double.MAX_VALUE : ((Number)a[0]).doubleValue());
    double db = (b[0] == null ? Double.MAX_VALUE : ((Number)b[0]).doubleValue());
    return (da < db ? -1 : da > db ? 1 : 0);
  }

//  private final static double DEFAULT_DISTANCE_FACTOR = 1.85d;
  private final static double DEFAULT_FACECENTEROFFSET = 0.25d;
  private final static int EDGES_NONE = 0;
  public final static int EDGES_ALL = 1;
  public final static int EDGES_FRONT = 2;
  public final static int EDGES_ONLY = 3;
  private final static int MAX_VERTICES = 250;
  private final static int FACE_COUNT_MAX = MAX_VERTICES - 3;
  private final static int MAX_OTHER = MAX_VERTICES + FACE_COUNT_MAX + 1;
  private P3d[] otherAtoms = new P3d[MAX_OTHER];
  private V3d[] normalsT = new V3d[MAX_VERTICES + 1];
  private int[][] planesT = AU.newInt2(MAX_VERTICES);
  private final static P3d randomPoint = P3d.new3(3141, 2718, 1414);

  private static final int MODE_BONDING = 1;
  private static final int MODE_POINTS = 2;
  private static final int MODE_RADIUS = 3;
  private static final int MODE_BITSET = 4;
  private static final int MODE_UNITCELL = 5;
  private static final int MODE_INFO = 6;
  /**
   * a dot product comparison term
   */
  private static final double DEFAULT_PLANAR_PARAM = 0.98d;
  private static final double CONVEX_HULL_MAX = 0.05d;//cos(87.13); was 0.02f = cos(88.15), which is too tight 

  public int polyhedronCount;
  public Polyhedron[] polyhedrons = new Polyhedron[32];
  public int drawEdges;

  private double radius, radiusMin, pointScale;
  private int nVertices;

  double faceCenterOffset;
//  double distanceFactor = Double.NaN;
  boolean isCollapsed;
  boolean isFull;

  private boolean iHaveCenterBitSet;
  private boolean bondedOnly;
  private boolean haveBitSetVertices;

  private BS centers;
  private String thisID;
  private P3d center;
  private BS bsVertices;
  private BS bsVertexCount;

  private boolean useUnitCell;
  private int nPoints;
  private double planarParam;
  private Map<String, Object> info;
  private double distanceRef;
  private int modelIndex;
  private boolean isAuto;
   private int[][] explicitFaces;

  @SuppressWarnings("unchecked")
  @Override
  public void setProperty(String propertyName, Object value, BS bs) {
    if (thisID != null)
      bs = new BS();
    if ("init" == propertyName) {
      faceCenterOffset = DEFAULT_FACECENTEROFFSET;
      //distanceFactor = 
      planarParam = Double.NaN;
      radius = radiusMin = pointScale = 0.0d;
      nVertices = nPoints = 0;
      modelIndex = -1;
      bsVertices = null;
      thisID = null;
      center = null;
      centers = null;
      info = null;
      bsVertexCount = new BS();
      bondedOnly = isCollapsed = isFull = iHaveCenterBitSet = useUnitCell = isAuto = haveBitSetVertices = false;
      if (Boolean.TRUE == value)
        drawEdges = EDGES_NONE;
      return;
    }

    if ("definedFaces" == propertyName) {
      setDefinedFaces((P3d[]) ((Object[]) value)[1], (int[][]) ((Object[]) value)[0]);
      return;
    }
    if ("generate" == propertyName) {
      if (!iHaveCenterBitSet && bs != null && !bs.isEmpty()) {
        centers = bs;
        iHaveCenterBitSet = true;
      }
      boolean modifyOnly = (value == Boolean.TRUE);
      if (modifyOnly)
        bs.and(vwr.getAtomBitSet("polyhedra"));
      Map<String, Object> info = this.info;
      deletePolyhedra();
      this.info = info;
      buildPolyhedra();
      return;
    }

    // thisID, center, and model all involve polyhedron ID ...

    if ("thisID" == propertyName) {
      thisID = (String) value;
      return;
    }

    if ("center" == propertyName) {
      center = (P3d) value;
      return;
    }

    if ("offset" == propertyName) {
      if (thisID != null)
        offsetPolyhedra((P3d) value);
      return;
    }

    if ("scale" == propertyName) {
      if (thisID != null)
        scalePolyhedra(((Number) value).doubleValue());
      return;
      
    }

    if ("model" == propertyName) {
      modelIndex = ((Integer) value).intValue();
      return;
    }

    if ("collapsed" == propertyName) {
      isCollapsed = true;
      return;
    }

    if ("full" == propertyName) {
      isFull = true;
      return;
    }

    if ("nVertices" == propertyName) {
      int n = ((Integer) value).intValue();
      if (n < 0) {
        if (-n >= nVertices) {
          bsVertexCount.setBits(nVertices, 1 - n);
          nVertices = -n;
        }
      } else {
        bsVertexCount.set(nVertices = n);
      }
      return;
    }

    if ("centers" == propertyName) {
      centers = (BS) value;
      iHaveCenterBitSet = true;
      return;
    }

    if ("unitCell" == propertyName) {
      useUnitCell = true;
      return;
    }

    if ("to" == propertyName) {
      bsVertices = (BS) value;
      return;
    }

    if ("toBitSet" == propertyName) {
      bsVertices = (BS) value;
      haveBitSetVertices = true;
      return;
    }

    if ("toVertices" == propertyName) {
      P3d[] points = (P3d[]) value;
      nPoints = Math.min(points.length, MAX_VERTICES);
      for (int i = nPoints; --i >= 0;)
        otherAtoms[i] = points[i];
      return;
    }

    if ("faceCenterOffset" == propertyName) {
      faceCenterOffset = ((Number) value).doubleValue();
      return;
    }

    if ("distanceFactor" == propertyName) {
      // not a general user option
      // ignore 
      //distanceFactor = ((Number) value).doubleValue();
      return;
    }

    if ("planarParam" == propertyName) {
      // not a general user option
      planarParam = ((Number) value).doubleValue();
      return;
    }

    if ("bonds" == propertyName) {
      bondedOnly = true;
      return;
    }

    if ("info" == propertyName) {
      info = (Map<String, Object>) value;
      Object o = info.get("id"); 
      if (o != null) 
        thisID = (o instanceof SV ? ((SV) o).asString() : o.toString());
      centers = (info.containsKey("center") ? null : BSUtil.newAndSetBit(((SV) info
          .get("atomIndex")).intValue));
      iHaveCenterBitSet = (centers != null);
      return;
    }

    if ("delete" == propertyName) {
      if (!iHaveCenterBitSet)
        centers = bs;
      deletePolyhedra();
      return;
    }
    if ("on" == propertyName) {
      if (!iHaveCenterBitSet)
        centers = bs;
      setVisible(true);
      return;
    }
    if ("off" == propertyName) {
      if (!iHaveCenterBitSet)
        centers = bs;
      setVisible(false);
      return;
    }
    if ("noedges" == propertyName) {
      drawEdges = EDGES_NONE;
      return;
    }
    if ("edges" == propertyName) {
      drawEdges = EDGES_ALL;
      return;
    }
    if ("edgesOnly" == propertyName) {
      drawEdges = EDGES_ONLY;
      return;
    }
    if ("frontedges" == propertyName) {
      drawEdges = EDGES_FRONT;
      return;
    }
    if (propertyName.indexOf("color") == 0) {
      // from polyhedra command, we may not be using the prior select
      // but from Color we need to identify the centers.
      bs = ("colorThis" == propertyName && iHaveCenterBitSet ? centers
          : andBitSet(bs));
      boolean isPhase = ("colorPhase" == propertyName);
      Object cvalue = (isPhase ? ((Object[]) value)[1] : value);
      short colixEdge = (isPhase ? C.getColix(((Integer) ((Object[]) value)[0])
          .intValue()) : C.INHERIT_ALL);
      short colix = C.getColixO(isPhase ? cvalue : value);
      Polyhedron p;
      BS bs1 = findPolyBS(bs, false);
      for (int i = bs1.nextSetBit(0); i >= 0; i = bs1.nextSetBit(i + 1)) {
        p = polyhedrons[i];
        if (p.id == null) {
          p.colixEdge = colixEdge;
        } else {
          p.colixEdge = colixEdge;
          p.colix = colix;
        }
      }
      if (thisID != null)
        return;
      value = cvalue;
      propertyName = "color";
      //allow super
    }

    if (propertyName.indexOf("translucency") == 0) {
      // from polyhedra command, we may not be using the prior select
      // but from Color we need to identify the centers.
      boolean isTranslucent = (value.equals("translucent"));
      if (thisID != null) {
        BS bs1 = findPolyBS(bs, false);
        Polyhedron p;
        for (int i = bs1.nextSetBit(0); i >= 0; i = bs1.nextSetBit(i + 1)) {
          p = polyhedrons[i];
          p.colix = C.getColixTranslucent3(p.colix, isTranslucent,
              translucentLevel);
          if (p.colixEdge != 0)
            p.colixEdge = C.getColixTranslucent3(p.colixEdge, isTranslucent,
                translucentLevel);
        }
        return;
      }
      bs = ("translucentThis".equals(value) && iHaveCenterBitSet ? centers
          : andBitSet(bs));
      if (value.equals("translucentThis"))
        value = "translucent";
      //allow super
    }

    //    if ("token" == propertyName) {
    //      int tok = ((Integer) value).intValue();
    //      Swit
    //      if (tok == T.triangles && tok == T.notriangles) {
    //      } else {
    //        setLighting(tok == T.fullylit, bs);
    //      }
    //      return;
    //    }

    if ("radius" == propertyName) {
      double v = ((Number) value).doubleValue();
      if (v <= 0) {
        // negative sets max
        isAuto = true;
        v = (v == 0 ? 6d : -v);
      }
      radius = v;
      return;
    }

    if ("radius1" == propertyName) {
      radiusMin = radius;
      radius = ((Number) value).doubleValue();
      return;
    }

    if ("points" == propertyName) {
      pointScale = ((Number) value).doubleValue();
        pointsPolyhedra(bs, pointScale);
      return;
    }
    
    if (propertyName == JC.PROP_DELETE_MODEL_ATOMS) {
      int modelIndex = ((int[]) ((Object[]) value)[2])[0];
      for (int i = polyhedronCount; --i >= 0;) {
        Polyhedron p = polyhedrons[i];
        p.info = null;
        if (p.modelIndex > modelIndex) {
          p.modelIndex--;
        } else if (p.modelIndex == modelIndex) {
          polyhedronCount--;
          polyhedrons = (Polyhedron[]) AU.deleteElements(polyhedrons, i, 1);
        }
      }
      //pass on to AtomShape
    }

    setPropAS(propertyName, value, bs);
  }

  private void setDefinedFaces(P3d[] points, int[][] faces) {
    BS bsUsed = new BS();
    for (int i = faces.length; --i >= 0;) {
      int[] face = faces[i];
      for (int j = face.length; --j >= 0;)
        bsUsed.set(face[j]);
    }
    BS bsNot = BSUtil.newBitSet2(0,  bsUsed.length());
    bsNot.andNot(bsUsed);
    int nNot = bsNot.cardinality(); 
    if (nNot > 0) {
      int np = points.length;
      int[] mapOldToNew = new int[np];
      int[] mapNewToOld = new int[np];
      int n = 0;
      for (int i = 0; i < np; i++)
        if (!bsNot.get(i)) {
          mapNewToOld[n] = i;
          mapOldToNew[i] = n++;
        }
      P3d[] pnew = new P3d[n];
      for (int i = 0; i < n; i++) 
        pnew[i] = points[mapNewToOld[i]]; 
      points = pnew;
      for (int i = faces.length; --i >= 0;) {
        int[] face = faces[i];
        for (int j = face.length; --j >= 0;)
          face[j] = mapOldToNew[face[j]];
      }
    }
    int n = nPoints = points.length;
    center = new P3d();
    otherAtoms = new P3d[n + 1];
    if (n > 0) {
      otherAtoms[n] = center;
      for (int i = 0; i < n; i++)
        center.add(otherAtoms[i] = points[i]);
      center.scale(1d / n);
    }
    explicitFaces = faces;
  }

  private void pointsPolyhedra(BS bs, double pointScale) {
    bs = findPolyBS(thisID == null ? bs : null, false);
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
      polyhedrons[i].setPointScale(pointScale);  
  }

  private void scalePolyhedra(double scale) {
    BS bs = findPolyBS(null, false);
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
      polyhedrons[i].scale = scale;
  }

  private void offsetPolyhedra(P3d value) {
    BS bs = findPolyBS(null, false);
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
      polyhedrons[i].setOffset(P3d.newP(value));
  }

  @Override
  public int getIndexFromName(String id) {
    if (id != null)
      for (int i = polyhedronCount; --i >= 0;)
        if (id.equalsIgnoreCase(polyhedrons[i].id))
          return i;
    return -1;
  }

  @Override
  public Object getProperty(String property, int i) {
    if ("list".equals(property)) {
      SB sb = new SB();
      for (i = 0; i < polyhedronCount; i++) {
        sb.appendI(i + 1);
        polyhedrons[i].list(sb);
      }
      return sb.toString();
    }
    Map<String, Object> info = polyhedrons[i].getInfo(vwr, property);
    return (property.equalsIgnoreCase("info") ? info : info.get(property));
  }

  @Override
  public boolean getPropertyData(String property, Object[] data) {
    int iatom = (data[0] instanceof Integer ? ((Integer) data[0]).intValue()
        : Integer.MIN_VALUE);
    String id = (data[0] instanceof String ? (String) data[0] : null);
    Polyhedron p;
    if (property == "index") {
      int i = getIndexFromName(id);
      if (i >= 0)
        data[1] = Integer.valueOf(i);
      return (i >= 0);
    }
    if (property == "checkID") {
      return checkID(id);
    }
    if (property == "getAtomsWithin"){
      p = findPoly(id, iatom, true, false);
      if (p == null)
        return false;
      data[2] = getAtomsWithin(p, ((Number) data[1]).doubleValue());
      return true;
    }
    if (property == "info") {
      // note that this does not set data[1] to null -- see ScriptExpr
      p = findPoly(id, iatom, true, false);
      if (p == null)
        return false;
      data[1] = p.getInfo(vwr, "info");
      return true;
    }
    if (property == "syminfo") {
      // note that this does not set data[1] to null -- see ScriptExpr
      p = findPoly(id, iatom, true, false);
      if (p == null)
        return false;
      p.getSymmetry(vwr, true);
      data[1] = p.getInfo(vwr, "info");
      return true;
    }
    if (property == "points") {
      p = findPoly(id, iatom, false, false);
      if (p == null)
        return false;
      data[1] = p.vertices;
      return true;
    }
    if (property == "symmetry") {
      BS bsSelected = (BS) data[2];
      String s = "";
      for (int i = 0; i < polyhedronCount; i++) {
        p = polyhedrons[i];
        if (p.id == null ? 
            id != null || bsSelected != null && !bsSelected.get(p.centralAtom.i)
            : id != null && !PT.isLike(p.id, id))
          continue;
        s += (i + 1) + "\t" + p.getSymmetry(vwr, true) + "\n";
      }     
      data[1] = s;
      return true;
    }
    if (property == "move") {
      M4d mat = (M4d) data[1];
      if (mat == null)
        return false;
      BS bsMoved = (BS) data[0];
      BS bs = findPolyBS(bsMoved, false);
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        polyhedrons[i].move(mat, bsMoved);
      return true;
    }
    if (property == "getCenters") {
      // return matching BS in data[1]

      // number of vertices (>0) or faces (<0) is in data[0], now iatom
      String smiles = (String) data[1];
      BS bsSelected = (BS) data[2];
      SmilesMatcherInterface sm = (smiles == null ? null : vwr
          .getSmilesMatcher());
      if (sm != null)
        smiles = sm.cleanSmiles(smiles);
      int nv = (smiles != null ? PT.countChar(smiles, '*') : iatom);
      if (nv == 0)
        nv = Integer.MIN_VALUE;
      BS bs = new BS();
      if (smiles == null || sm != null)
        for (int i = polyhedronCount; --i >= 0;) {
          p = polyhedrons[i];
          if (p.id != null)
            continue; // for now, no support for non-atomic polyhedra
          // allows for -n being -(number of faces)
          if (nv != (nv > 0 ? p.nVertices
              : nv > Integer.MIN_VALUE ? -p.faces.length : nv))
            continue;
          iatom = p.centralAtom.i;
          if (bsSelected != null && !bsSelected.get(iatom))
            continue;
          if (smiles == null) {
            bs.set(iatom);
            continue;
          }
          p.getSymmetry(vwr, false);
          String smiles0 = sm.cleanSmiles(p.polySmiles);
          try {
            if (sm.areEqual(smiles, smiles0) > 0)
              bs.set(iatom);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      data[1] = bs;
      return true;
    }
    if (property == "allInfo") {
      Lst<Map<String, Object>> info = new Lst<Map<String, Object>>();
      for (int i = polyhedronCount; --i >= 0;)
        info.addLast(polyhedrons[i].getInfo(vwr, "info"));
      data[1] = info;
      return true;
    }
    return getPropShape(property, data);
  }

  private BS getAtomsWithin(Polyhedron p, double offset) {
    int[][] faces = p.faces;
    P3d[] vertices = p.vertices;
    P3d center = (p.center == null ? p.centralAtom : p.center);
    if (p.planes == null) {
      V3d vNorm = new V3d();
      V3d vAB = new V3d();
      p.planes = new P4d[faces.length];
      for (int iface = faces.length; --iface >= 0;) {
        P4d plane = p.planes[iface] = new P4d();
        MeasureD.getPlaneThroughPoints(vertices[faces[iface][0]],
            vertices[faces[iface][1]], vertices[faces[iface][2]], vNorm, vAB,
            plane);
      }
    }
    double maxDistance = 0;
    for (int i = p.nVertices; --i >= 0;) {
      double d = vertices[i].distance(center);
      if (d > maxDistance)
        maxDistance = d;
    }
    BS bsAtoms = new BS();
    vwr.ms.getAtomsWithin(maxDistance + offset, center, bsAtoms, p.modelIndex);
    Atom[] atoms = ms.at;
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
      for (int f = faces.length; --f >= 0;) {
        //System.out.println(MeasureD.distanceToPlane(p.planes[f], atoms[i]));
        if (MeasureD.distanceToPlane(p.planes[f], atoms[i]) > offset + 0.001d) {
          bsAtoms.clear(i);
          break;
        }
      }
    }
  
    return bsAtoms;
  }

  private boolean checkID(String thisID) {
    this.thisID = thisID;
      return (!findPolyBS(null, false).isEmpty());
   }

  /**
   * 
   * @param id  may be null
   * @param iatom  may be < 0 to (along with id==null) to get matching polyhedron 
   * @param allowCollapsed
   * @param andDelete 
   * @return Polyhedron or null
   */
  private Polyhedron findPoly(String id, int iatom, boolean allowCollapsed, boolean andDelete) {
    for (int i = polyhedronCount; --i >= 0;) {
      Polyhedron p = polyhedrons[i]; 
      if (p.id == null ? p.centralAtom.i == iatom : p.id.equalsIgnoreCase(id)) {
        if (andDelete) {
          polyhedronCount--;
          for (; i < polyhedronCount; i++)
            polyhedrons[i] = polyhedrons[i + 1];
        }
        return (allowCollapsed || !polyhedrons[i].collapsed ? polyhedrons[i] : null);
      }
    }
   return null;
  }

  private BS bsPolys = new BS();
  
  private BS findPolyBS(BS bsCenters, boolean isAll) {
    BS bs = bsPolys;
    bs.clearAll();
    if (isAll) {
      bs.setBits(0, polyhedronCount);
    } else {
      Polyhedron p;
      for (int i = polyhedronCount; --i >= 0;) {
        p = polyhedrons[i];
        if (p.id == null ? bsCenters != null && bsCenters.get(p.centralAtom.i)
            : isMatch(p.id))
          bs.set(i);
      }
    }
    return bs;
  }

  private boolean isMatch(String id) {
    // could implement wildcards
    return thisID != null && PT.isMatch(id.toLowerCase(), thisID.toLowerCase(), true, true);
  }

  @Override
  public Object getShapeDetail() {
    Lst<Map<String, Object>> lst = new Lst<Map<String, Object>>();
    for (int i = 0; i < polyhedronCount; i++)
      lst.addLast(polyhedrons[i].getInfo(vwr, "info"));
    return lst;
  }

  private BS andBitSet(BS bs) {
    BS bsCenters = new BS();
    for (int i = polyhedronCount; --i >= 0;) {
      Polyhedron p = polyhedrons[i];
      if (p.id == null)
        bsCenters.set(p.centralAtom.i);
    }
    bsCenters.and(bs);
    return bsCenters;
  }

  private void deletePolyhedra() {
    int newCount = 0;
    byte pid = PAL.pidOf(null);
    BS bs = findPolyBS(centers, false);
    for (int i = 0; i < polyhedronCount; ++i) {
      Polyhedron p = polyhedrons[i];
      if (bs.get(i)) {
        if (colixes != null && p.id == null)
          setColixAndPalette(C.INHERIT_ALL, pid, p.centralAtom.i);
        continue;
      }
      polyhedrons[newCount++] = p;
    }
    for (int i = newCount; i < polyhedronCount; ++i)
      polyhedrons[i] = null;
    polyhedronCount = newCount;
  }

  private void setVisible(boolean visible) {
    BS bs = findPolyBS(centers, false);
    Atom[] atoms = ms.at;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      Polyhedron p = polyhedrons[i];
      p.visible = visible;
      if (p.centralAtom != null)
        atoms[p.centralAtom.i].setShapeVisibility(vf, visible);
    }
  }

  private void buildPolyhedra() {
    Polyhedron p = null;
    if (info == null && centers == null && (center == null || nPoints == 0 && bsVertices == null))
      return;
    if (info != null && info.containsKey("id")) {
      Object o = info.get("id"); 
      thisID = (o instanceof SV ? ((SV) o).asString() : o.toString());
      //Polyhedron pold = findPoly(thisID, -1, true, true);
      p = new Polyhedron().setInfo(vwr, info, ms.at);
    } else if (thisID != null) {
      if (PT.isWild(thisID))
        return;
      if (center != null) {
        if (nPoints == 0)
          setPointsFromBitset();
        p = validatePolyhedron(center, nPoints);
      }
    }
    if (p != null) {
      addPolyhedron(p);
      return;
    }
    boolean useBondAlgorithm = (radius == 0 || bondedOnly);
    int buildMode = (info != null ? MODE_INFO : nPoints > 0 ? MODE_POINTS
        : haveBitSetVertices ? MODE_BITSET : useUnitCell ? MODE_UNITCELL
            : useBondAlgorithm ? MODE_BONDING : MODE_RADIUS);
    AtomIndexIterator iter = (buildMode == MODE_RADIUS ? ms
        .getSelectedAtomIterator(null, false, false, false, false) : null);
    Atom[] atoms = ms.at;
    for (int i = centers.nextSetBit(0); i >= 0; i = centers.nextSetBit(i + 1)) {
      Atom atom = atoms[i];
      p = null;
      switch (buildMode) {
      case MODE_BITSET:
        p = constructBitSetPolyhedron(atom);
        break;
      case MODE_UNITCELL:
        p = constructUnitCellPolygon(atom, useBondAlgorithm);
        break;
      case MODE_BONDING:
        p = constructBondsPolyhedron(atom, 0);
        break;
      case MODE_RADIUS:
        vwr.setIteratorForAtom(iter, i, radius);
        p = constructRadiusPolyhedron(atom, iter);
        break;
      case MODE_INFO:
        p = new Polyhedron().setInfo(vwr, info, ms.at);
        break;
      case MODE_POINTS:
        p = validatePolyhedron(atom, nPoints);
        break;
      }
      if (p != null)
        addPolyhedron(p);
      if (haveBitSetVertices)
        break;
    }
    if (iter != null)
      iter.release();
  }

  private void setPointsFromBitset() {
    // polyhedra ID p1 @6 to {connected(@6)}
    if (bsVertices != null) {
      Atom[] atoms = ms.at;
      for (int i = bsVertices.nextSetBit(0); i >= 0 && nPoints < MAX_VERTICES; i = bsVertices
          .nextSetBit(i + 1))
        otherAtoms[nPoints++] = atoms[i];
    }
  }

  private void addPolyhedron(Polyhedron p) {
    if (polyhedronCount == polyhedrons.length)
      polyhedrons = (Polyhedron[]) AU.doubleLength(polyhedrons);
    polyhedrons[polyhedronCount++] = p;
  }

  private Polyhedron constructBondsPolyhedron(Atom atom, int otherAtomCount) {
    distanceRef = 0;
    if (otherAtomCount == 0) {
      Bond[] bonds = atom.bonds;
      if (bonds == null)
        return null;
      double r2 = radius * radius;
      double r1 = radiusMin * radiusMin;
      double r;
      for (int i = bonds.length; --i >= 0;) {
        Bond bond = bonds[i];
        if (!bond.isCovalent())
          continue;
        Atom other = bond.getOtherAtom(atom);
        if (bsVertices != null && !bsVertices.get(other.i) || radius > 0
            && ((r = other.distanceSquared(atom)) > r2 || r < r1))
          continue;
        otherAtoms[otherAtomCount++] = other;
        if (otherAtomCount >= MAX_VERTICES)
          return null;
      }
    }
    if (isAuto)
      otherAtomCount = setGap(atom, otherAtomCount);
    return (otherAtomCount < 3 || nVertices > 0
        && !bsVertexCount.get(otherAtomCount) ? null : validatePolyhedron(atom,
        otherAtomCount));
  }

  private Polyhedron constructUnitCellPolygon(Atom atom,
                                              boolean useBondAlgorithm) {
    SymmetryInterface unitcell = ms.getUnitCellForAtom(atom.i);
    if (unitcell == null)
      return null;
    BS bsAtoms = BSUtil.copy(vwr.getModelUndeletedAtomsBitSet(atom.mi));
    if (bsVertices != null)
      bsAtoms.and(bsVertices);
    if (bsAtoms.isEmpty())
      return null;
    Atom[] atoms = ms.at;
    AtomIndexIterator iter = unitcell.getIterator(atom, bsAtoms, useBondAlgorithm ? 5d : radius);
    if (!useBondAlgorithm)
      return constructRadiusPolyhedron(atom, iter);
    double myBondingRadius = atom.getBondingRadius();
    if (myBondingRadius == 0)
      return null;
    double bondTolerance = vwr.getDouble(T.bondtolerance);
    double minBondDistance = (radiusMin == 0 ? vwr.getDouble(T.minbonddistance) : radiusMin);
    double minBondDistance2 = minBondDistance * minBondDistance;
    int otherAtomCount = 0;
   outer: while (iter.hasNext()) {
      Atom other = atoms[iter.next()];
      double otherRadius = other.getBondingRadius();
      P3d pt = iter.getPosition();
      double distance2 = atom.distanceSquared(pt);
      if (!ms.isBondable(myBondingRadius, otherRadius, distance2,
          minBondDistance2, bondTolerance))
        continue;
      for (int i = 0; i < otherAtomCount; i++)
        if (otherAtoms[i].distanceSquared(pt) < 0.01f)
          continue outer;
      otherAtoms[otherAtomCount++] = pt;
      if (otherAtomCount >= MAX_VERTICES)
        return null;
    }
    return constructBondsPolyhedron(atom, otherAtomCount);
  }

  private Polyhedron constructBitSetPolyhedron(Atom atom) {
    bsVertices.clear(atom.i);
    if (bsVertices.cardinality() >= MAX_VERTICES)
      return null;
    int otherAtomCount = 0;
    distanceRef = 0;
    Atom[] atoms = ms.at;
    for (int i = bsVertices.nextSetBit(0); i >= 0; i = bsVertices
        .nextSetBit(i + 1))
      otherAtoms[otherAtomCount++] = atoms[i];
    return validatePolyhedron(atom, otherAtomCount);
  }

  private Polyhedron constructRadiusPolyhedron(Atom atom, AtomIndexIterator iter) {
    int otherAtomCount = 0;
    distanceRef = radius;
    double r2 = radius * radius;
    double r2min = radiusMin * radiusMin;
    Atom[] atoms = ms.at;
    outer: while (iter.hasNext()) {
      Atom other = atoms[iter.next()];
      P3d pt = iter.getPosition();
      if (pt == null) {
        // this will happen with standard radius atom iterator
        pt = other;
        if (bsVertices != null && !bsVertices.get(other.i))
          continue;
      }
      double r = atom.distanceSquared(pt);

      if (other.altloc != atom.altloc && other.altloc != 0 && atom.altloc != 0
          || r > r2 || r < r2min)
        continue;
      if (otherAtomCount == MAX_VERTICES)
        break;
      for (int i = 0; i < otherAtomCount; i++)
        if (otherAtoms[i].distanceSquared(pt) < 0.01f)
          continue outer;
      otherAtoms[otherAtomCount++] = pt;
    }
    if (isAuto)
      otherAtomCount = setGap(atom, otherAtomCount);
    return (otherAtomCount < 3 || nVertices > 0
        && !bsVertexCount.get(otherAtomCount) ? null : validatePolyhedron(atom,
        otherAtomCount));
  }

  private int setGap(P3d atom, int otherAtomCount) {
    if (otherAtomCount < 4)
      return otherAtomCount;
    Object[][] dist = new Object[MAX_VERTICES][2];
    for (int i = 0; i < otherAtomCount; i++)
      dist[i][0] = Double.valueOf(atom.distance((P3d) (dist[i][1] = otherAtoms[i])));
    Arrays.sort(dist, this);
    double maxGap = 0;
    int iMax = 0;
    int n = otherAtomCount;
    double dlast = ((Number)dist[0][0]).doubleValue();
    otherAtoms[0] = (P3d) dist[0][1];
    for (int i = 1; i < n; i++) {
      double d = ((Number)dist[i][0]).doubleValue();
      double gap = d - dlast;
      otherAtoms[i] = (P3d) dist[i][1];
      if (Logger.debugging)
        Logger.info("polyhedron d=" + d + " " + otherAtoms[i]);
      if (gap > maxGap) {
        if (Logger.debugging)
          Logger.info("polyhedron maxGap=" + gap + " for i=" + i + " d=" + d + " " + otherAtoms[i]);
        maxGap = gap;
        iMax = i;
      }
      dlast = d;
    }
    return (iMax == 0 ? otherAtomCount : iMax);
  }

  private Polyhedron validatePolyhedron(P3d atomOrPt, int vertexCount) {
    P3d[] points = otherAtoms;
    int[][] faces = explicitFaces;
    int[][] faceTriangles;
    V3d[] normals;
    boolean collapsed = isCollapsed;
    int triangleCount = 0;

    BS bsCenterPlanes = new BS();
    int[][] triangles;
    if (faces != null) {
      collapsed = false;
      faceTriangles = AU.newInt2(faces.length);
      normals = new V3d[faces.length];
      for (int i = faces.length; --i >= 0;)
        faces[i] = fixExplicitFaceWinding(faces[i], i, points, normals);
      triangles = ((MeshCapper) Interface.getInterface(
          "org.jmol.util.MeshCapper", vwr, "script")).set(null)
          .triangulateFaces(faces, points, faceTriangles);
      triangleCount = triangles.length;
    } else {

      nPoints = vertexCount + 1;
      int ni = vertexCount - 2;
      int nj = vertexCount - 1;
      double planarParam = (Double.isNaN(this.planarParam) ? DEFAULT_PLANAR_PARAM
          : this.planarParam);

      points[vertexCount] = atomOrPt;
      P3d ptAve = P3d.newP(atomOrPt);
      for (int i = 0; i < vertexCount; i++)
        ptAve.add(points[i]);
      ptAve.scale(1d / (vertexCount + 1));
      /*  Start by defining a face to be when all three distances
       *  are < distanceFactor * (longest central) but if a vertex is missed, 
       *  then expand the range. The collapsed trick is to introduce 
       *  a "simple" atom near the center but not quite the center, 
       *  so that our planes on either side of the facet don't overlap. 
       *  We step out faceCenterOffset * normal from the center.
       *  
       *  Alan Hewat pointed out the issue of faces that CONTAIN the center --
       *  square planar, trigonal and square pyramids, see-saw. In these cases with no
       *  additional work, you get a brilliance effect when two faces are drawn over
       *  each other. The solution is to identify this sort of face and, if not collapsed,
       *  to cut them into smaller pieces and only draw them ONCE by producing a little
       *  catalog. This uses the Point3i().toString() method.
       *  
       *  For these special cases, then, we define a reference point just behind the plane
       *  
       *  Note that this is NOT AN OPTION for ID-named polyhedra (Jmol 14.5.0 10/31/2015)
       */

      P3d ptRef = P3d.newP(ptAve);
      BS bsThroughCenter = new BS();
      if (thisID == null)
        for (int pt = 0, i = 0; i < ni; i++)
          for (int j = i + 1; j < nj; j++)
            for (int k = j + 1; k < vertexCount; k++, pt++)
              if (isPlanar(points[i], points[j], points[k], ptRef))
                bsThroughCenter.set(pt);
      // this next check for distance allows for bond AND distance constraints
      triangles = planesT;
      P4d pTemp = new P4d();
      V3d nTemp = new V3d();
      double offset = faceCenterOffset;
      int fmax = FACE_COUNT_MAX;
      int vmax = MAX_VERTICES;
      BS bsTemp = Normix.newVertexBitSet();
      normals = normalsT;
      Map<Integer, Object[]> htNormMap = new Hashtable<Integer, Object[]>();
      Map<String, Object> htEdgeMap = new Hashtable<String, Object>();
      Lst<int[]> lstRejected = (isFull ? new Lst<int[]>() : null);
      Object[] edgeTest = new Object[3];
      V3d vAC = this.vAC;
      for (int i = 0, pt = 0; i < ni; i++)
        for (int j = i + 1; j < nj; j++) {
          for (int k = j + 1; k < vertexCount; k++, pt++) {
            if (triangleCount >= fmax) {
              Logger.error("Polyhedron error: maximum face(" + fmax
                  + ") -- reduce RADIUS");
              return null;
            }
            if (nPoints >= vmax) {
              Logger.error("Polyhedron error: maximum vertex count(" + vmax
                  + ") -- reduce RADIUS");
              return null;
            }
            boolean isThroughCenter = bsThroughCenter.get(pt);
            P3d rpt = (isThroughCenter ? randomPoint : ptAve);
            V3d normal = new V3d();
            boolean isWindingOK = MeasureD.getNormalFromCenter(rpt, points[i],
                points[j], points[k], !isThroughCenter, normal, vAC);
            // the standard face:
            int[] t = new int[] { isWindingOK ? i : j, isWindingOK ? j : i, k,
                -7 };
            double err = checkFacet(points, vertexCount, t, triangleCount,
                normal, pTemp, nTemp, vAC, htNormMap, htEdgeMap, planarParam,
                bsTemp, edgeTest);
            if (err != 0) {
              if (isFull && err != Double.MAX_VALUE && err < 0.5d) {
                t[3] = (int) (err * 100);
                lstRejected.addLast(t);
              }
              continue;
            }
            normals[triangleCount] = normal;
            triangles[triangleCount] = t;
            if (isThroughCenter) {
              bsCenterPlanes.set(triangleCount++);
            } else if (collapsed) {
              points[nPoints] = new P3d();
              points[nPoints].scaleAdd2(offset, normal, atomOrPt);
              ptRef.setT(points[nPoints]);
              addFacet(i, j, k, ptRef, points, normals, triangles,
                  triangleCount++, nPoints, isWindingOK, vAC);
              addFacet(k, i, j, ptRef, points, normals, triangles,
                  triangleCount++, nPoints, isWindingOK, vAC);
              addFacet(j, k, i, ptRef, points, normals, triangles,
                  triangleCount++, nPoints, isWindingOK, vAC);
              nPoints++;
            } else {
              triangleCount++;
            }
          }
        }
      nPoints--;
      if (Logger.debugging) {
        Logger.info("Polyhedron planeCount=" + triangleCount + " nPoints="
            + nPoints);
        for (int i = 0; i < triangleCount; i++)
          Logger.info("Polyhedron "
              + PT.toJSON("face[" + i + "]", triangles[i]));
      }
      //System.out.println(PT.toJSON(null, lstRejected));
      faces = getFaces(triangles, triangleCount, htNormMap);
      faceTriangles = getFaceTriangles(faces.length, htNormMap, triangleCount);
    }
    return new Polyhedron().set(thisID, modelIndex, atomOrPt, points, nPoints,
        vertexCount, triangles, triangleCount, faces, faceTriangles, normals,
        bsCenterPlanes, collapsed, distanceRef, pointScale);
  }

  /**
   * Check to see that the winding of the explicit face is correct. If not,
   * correct it. Also set the normals.
   * @param face
   * @param ipt
   * @param points
   * @param normals
   * @return correctly wound face
   */
  private int[] fixExplicitFaceWinding(int[] face, int ipt, P3d[] points, V3d[] normals) {
    int n = face.length;
    for (int i = 0, nlast = n - 2; i < nlast; i++) {
      P3d a = points[face[i]];
      P3d b = points[face[(i + 1) % n]];
      P3d c = points[face[(i + 2) % n]];
      if (MeasureD.computeAngleABC(a, b, c, true) < 178) {
        // ignore nearly linear angles -- we only need one
        if (!MeasureD.getNormalFromCenter(center, a, b, c, true, normals[ipt] = new V3d(), vAC))
          face = AU.arrayCopyRangeRevI(face, 0, -1);
        break;
      }
    }
    return face;
  }

  private int[][] getFaceTriangles(int n, Map<Integer, Object[]> htNormMap, int triangleCount) {
    int[][] faceTriangles = AU.newInt2(n);
    if (triangleCount == n) {
      for (int i = triangleCount; --i >= 0;)
        faceTriangles[i] = new int[] { i };
      return faceTriangles;
    }
    int i = 0;
    for (Entry<Integer, Object[]> e : htNormMap.entrySet()) {
      Object[] eo = e.getValue();
      if (eo[2] != null && eo[2] != e.getKey())
        continue;
      @SuppressWarnings("unchecked")
      Lst<Integer> lst = (Lst<Integer>)e.getValue()[1];
      n = lst.size();
      int[] a = new int[n];
      for (int j = n; --j >= 0;)
        a[j] = lst.get(j).intValue();
      faceTriangles[i++] = a;
    }
    return faceTriangles;
  }

  /**
   * Add one of the three "facets" that compose the planes of a "collapsed" polyhedron.
   * A mask of -2 ensures that only the [1-2] edge is marked as an outer edge.
   * 
   * @param i
   * @param j
   * @param k
   * @param ptRef slightly out from the center; based on centerOffset parameter
   * @param points
   * @param normals
   * @param faces
   * @param planeCount
   * @param nRef
   * @param isWindingOK
   * @param vTemp 
   */
  private void addFacet(int i, int j, int k, P3d ptRef, P3d[] points,
                        V3d[] normals, int[][] faces, int planeCount, int nRef,
                        boolean isWindingOK, V3d vTemp) {
    V3d normal = new V3d();
    int ii = isWindingOK ? i : j;
    int jj = isWindingOK ? j : i;
    MeasureD.getNormalFromCenter(points[k], ptRef, points[ii], points[jj], false, normal, vTemp);
    normals[planeCount] = normal;
    faces[planeCount] = new int[] { nRef, ii, jj, -2 };
    //System.out.println("draw ID \"d" + i+ j+ k + "\" VECTOR "
      //           + ptRef + " " + normal + " color blue \">" + i + j +k + isWindingOK
        //        + "\"");
  }

  /**
   * Clean out overlapping triangles based on normals and cross products. For
   * now, we use normixes, which are approximations of normals. It is not 100%
   * guaranteed that this will work.
   * 
   * @param points
   * @param nPoints
   * @param t
   * @param index
   * @param norm
   * @param pTemp
   * @param vNorm
   * @param vAC
   * @param htNormMap
   * @param htEdgeMap
   * @param planarParam
   * @param bsTemp
   * @param edgeTest
   * @return 0 if no error or value indicating the error
   */
  private double checkFacet(P3d[] points, int nPoints, int[] t, int index,
                           V3d norm, P4d pTemp, V3d vNorm, V3d vAC,
                           Map<Integer, Object[]> htNormMap,
                           Map<String, Object> htEdgeMap, double planarParam,
                           BS bsTemp, Object[] edgeTest) {

    // Check here for a 3D convex hull:

//    if (t[2] == 3)
//System.out.println(index + ": t = " + PT.toJSON(null, t));
    int i0 = t[0];
    MeasureD.getPlaneThroughPoints(points[i0], points[t[1]],
        points[t[2]], vNorm, vAC, pTemp);
    // See if all vertices are OUTSIDE the plane we are considering.
    //System.out.println(PT.toJSON(null, p1));
    P3d pt = points[i0];
    for (int j = 0; j < nPoints; j++) {
      if (j == i0)
        continue;
      //System.out.println("isosurface plane " + pTemp);
      //System.out.println("polyh distance to pt" + Measure.distanceToPlane(pTemp, points[j]));
      vAC.sub2(points[j], pt);
      vAC.normalize();
      double v = vAC.dot(vNorm);
      // this should be 0 for a perfect plane

      // we cannot just take a negative dot product as indication of being
      // inside the convex hull. That would be fine if this were about regular
      // polyhedra. But we can have imperfect quadrilateral faces that are slightly
      // bowed inward. 
      if (v > CONVEX_HULL_MAX) {
        return v;
      }
      if (Logger.debugging)
        Logger.info("checkFacet " + j + " " + v + " " + PT.toJSON(null, t));
    }
    Integer normix = Integer.valueOf(Normix.getNormixV(norm, bsTemp));
    Object[] o = htNormMap.get(normix);
    //System.out.println(normix + " t = " + PT.toJSON(null, t) + o);
    if (o == null) {
      // We must see if there is a close normix to this.
      // The Jmol lighting model uses 642 normals,
      // which have a neighboring vertex dot product of 0.990439.
      // This is too tight for polyhedron planarity. We relax this
      // requirement to 0.98 by default, but this can be relaxed even
      // more if desired.

      V3d[] norms = Normix.getVertexVectors();
      for (Entry<Integer, Object[]> e : htNormMap.entrySet()) {
        Integer n = e.getKey();
        //System.out.println("norm " + n + " " + norms[n.intValue()].dot(norm) + " " + planarParam);
        if (norms[n.intValue()].dot(norm) > planarParam) {
          o = e.getValue();
          o[2] = n;
          htNormMap.put(normix, o);
          //System.out.println("switching " + normix + " to " + n);
          break;
        }
      }
      if (o == null)
        htNormMap.put(normix, o = new Object[] { new Lst<int[]>(),
            new Lst<Integer>(), normix });
    }
    normix = (Integer) o[2];

    //    if (p1[0] == 3 && p1[1] == 11 &&  p1[2] == 16)
    //        if (normix == 629) {
    //System.out.println("testing poly" + PT.toJSON(null, p1) + normix);
    //System.out.println(normix);
    //
    //        }
    //       
    @SuppressWarnings("unchecked")
    Lst<int[]> faceEdgeList = (Lst<int[]>) o[0];
    @SuppressWarnings("unchecked")
    Lst<Integer> faceTriList = (Lst<Integer>) o[1];
    for (int i = 0; i < 3; i++)
      if ((edgeTest[i] = addEdge(faceEdgeList, htEdgeMap, normix, t, i, points)) == null)
        return Double.MAX_VALUE;
    for (int i = 0; i < 3; i++) {
      Object oo = edgeTest[i];
      if (oo == Boolean.TRUE)
        continue;
      Object[] oe = (Object[]) oo;
      faceEdgeList.addLast((int[]) oe[2]);
      htEdgeMap.put((String) oe[3], oe);
    }
    faceTriList.addLast(Integer.valueOf(index));
    return 0;
  }
  
  /**
   * Check each edge to see that
   * 
   * (a) it has not been used before
   * 
   * (b) it does not have vertex points on both sides of it
   * 
   * (c) if it runs opposite another edge, then both edge masks are set properly
   * 
   * @param faceEdgeList
   * @param htEdgeMap
   * @param normix
   * @param p1
   * @param i
   * @param points
   * @return true if this triangle is OK
   * 
   */
  private Object addEdge(Lst<int[]> faceEdgeList,
                         Map<String, Object> htEdgeMap, Integer normix,
                         int[] p1, int i, P3d[] points) {
    // forward maps are out
    int pt = p1[i];
    int pt1 = p1[(i + 1) % 3];
    String s1 = "_" + pt1;
    String s = "_" + pt;
    String edge = normix + s + s1;
    if (htEdgeMap.containsKey(edge))
      return null;
    //reverse maps are in
    String edge0 = normix + s1 + s;
    Object o = htEdgeMap.get(edge0);
    int[] b;
    
//    if (normix == 389)
//System.out.println(normix + " normix " + edge0);
    if (o == null) {
      P3d coord2 = points[pt1];
      P3d coord1 = points[pt];
      vAB.sub2(coord2, coord1);
      for (int j = faceEdgeList.size(); --j >= 0;) {
        int[] e = faceEdgeList.get(j);
        P3d c1 = points[e[0]];
        P3d c2 = points[e[1]];
        if (c1 != coord1 && c1 != coord2 && c2 != coord1 && c2 != coord2
            && testDiff(c1, c2, coord1, coord2)
            && testDiff(coord1, coord2, c1, c2))
          return null;
      }
      return new Object[] { p1, Integer.valueOf(i), new int[] { pt, pt1, 0 }, edge };
    }
    // set mask to exclude both of these.
    int[] p10 = (int[]) ((Object[]) o)[0];
    if (p10 == null)
      return null; // already seen
    int i0 = ((Integer) ((Object[]) o)[1]).intValue();
    p10[3] = -((-p10[3]) ^ (1 << i0));
    p1[3] = -((-p1[3]) ^ (1 << i));
    b = (int[]) ((Object[]) o)[2];
    for (int j = faceEdgeList.size(); --j >= 0;) {
      int[] f = faceEdgeList.get(j);
      if (f[0] == b[0] && f[1] == b[1]) {
        f[2] = -1;
//        faceEdgeList.remove(j);  No! Need this for final face generation from edges
        break;
      }
    }
    htEdgeMap.put(edge, new Object[] { null });
    htEdgeMap.put(edge0, new Object[] { null });
    return Boolean.TRUE;
  }

  private boolean testDiff(P3d a1, P3d b1, P3d a2, P3d b2) {
    //
    //       b1
    //  a2_ /__b2
    //     /
    //    a1
    //
    vAB.sub2(b1, a1);
    vAC.sub2(a2, a1);
    vAC.cross(vAC, vAB);
    vBC.sub2(b2, a1);
    vBC.cross(vBC, vAB);
    return (vBC.dot(vAC) < 0);
  }

  private final V3d vAB = new V3d();
  private final V3d vAC = new V3d();
  private final V3d vBC = new V3d();

  private static double MAX_DISTANCE_TO_PLANE = 0.1d;

  private boolean isPlanar(P3d pt1, P3d pt2, P3d pt3, P3d ptX) {
    /*
     * what is the quickest way to find out if four points are planar? 
     * here we determine the plane through three and then the distance to that plane
     * of the fourth
     * 
     */
    V3d norm = new V3d();
    double w = MeasureD.getNormalThroughPoints(pt1, pt2, pt3, norm, vAB);
    double d = MeasureD.distanceToPlaneV(norm, w, ptX);
    return (Math.abs(d) < MAX_DISTANCE_TO_PLANE);
  }


  /**
   * Face: a CCW loop of edges all (within tolerance) in the same plane.
   * 
   * Objective is to find all triangles with *essentially* the same normal and to 
   * then group them into a face. But we have to be careful here; not everything is 
   * perfect. We can have be so slightly off in a 4- or 6-face, and we still want it
   * to be called a face. We allow a normal dot product (i.e. cos(theta)) to be < 0.05.
   * This empirically seems to work.  
   *  
   * @param triangles
   * @param triangleCount
   * @param htNormMap
   * @return array of CCW connecting edges
   */
  private int[][] getFaces(int[][] triangles, int triangleCount,
                           Map<Integer, Object[]> htNormMap) {
    int n = 0;
    for (Entry<Integer, Object[]> e : htNormMap.entrySet()) {
      Object[] eo = e.getValue();
      if (eo[2] == e.getKey())
        n++;
    }
      
    int[][] faces = AU.newInt2(n);
    if (triangleCount == n) {
      for (int i = triangleCount; --i >= 0;)
        faces[i] = AU.arrayCopyI(triangles[i], 3);
      return faces;
    }
    int fpt = 0;
    for (Entry<Integer, Object[]> e : htNormMap.entrySet()) {
      //System.out.println("polyhedra normix=" + e.getKey());
      Object[] eo = e.getValue();
      if (eo[2] != null && eo[2] != e.getKey())
        continue;
      @SuppressWarnings("unchecked")
      Lst<int[]> faceEdgeList = (Lst<int[]>)e.getValue()[0];
      n = faceEdgeList.size();
      int nOK = 0;
      for (int i = faceEdgeList.size(); --i >= 0;)
        if (faceEdgeList.get(i)[2] >= 0)
          nOK++;
      int[] face = faces[fpt++] = new int[nOK];
      if (n < 2)
        continue;
      int[] edge = null;
      int pt = 0;
      do {
        edge = faceEdgeList.get(pt);
      } while (pt++ < nOK && edge[2] == -1);
      //System.out.println("poly edge=" + PT.toJSON(null, edge));
      face[0] = edge[0];
      face[1] = edge[1];
      pt = 2;
      int i0 = 1;
      int  pt0 =  -1;
      while (pt < nOK && pt0 != pt) {
        pt0 = pt;
        for (int i = i0; i < n; i++) {
          edge = faceEdgeList.get(i);
          if (edge[2] != -1 && edge[0] == face[pt - 1]) {
            face[pt++] = edge[1];
            if (i == i0)
              i0++;
            break;
          }
        }
      }
    }
    return faces;
  }

  @Override
  public void setModelVisibilityFlags(BS bsModels) {
    /*
    * set all fixed objects visible; others based on model being displayed note
    * that this is NOT done with atoms and bonds, because they have mads. When
    * you say "frame 0" it is just turning on all the mads.
    */
    for (int i = polyhedronCount; --i >= 0;) {
      Polyhedron p = polyhedrons[i];
      if (p.id == null) {
        int ia = p.centralAtom.i;
        if (ms.at[ia].isDeleted())
          p.isValid = false;
       p.visibilityFlags = (p.visible && bsModels.get(p.modelIndex)
            && !ms.isAtomHidden(ia)
            && !ms.at[ia].isDeleted() ? vf : 0);
        ms.at[ia].setShapeVisibility(vf, p.visibilityFlags != 0);
      } else {
        p.visibilityFlags = (p.visible
            && (p.modelIndex < 0 || bsModels.get(p.modelIndex)) ? vf : 0);
      }
    }
  }

  @Override
  public String getShapeState() {
    if (polyhedronCount == 0)
      return "";
    SB s = new SB();
    for (int i = 0; i < polyhedronCount; i++)
      if (polyhedrons[i].isValid)
        s.append(polyhedrons[i].getState(vwr));
    if (drawEdges == EDGES_FRONT)
      appendCmd(s, "polyhedra frontedges");
    else if (drawEdges == EDGES_ALL)
      appendCmd(s, "polyhedra edges");
    else if (drawEdges == EDGES_ONLY)
      appendCmd(s, "polyhedra edgesOnly");
    s.append(vwr.getStateCreator().getAtomShapeState(this));
    int ia;
    for (int i = 0; i < polyhedronCount; i++) {
      Polyhedron p = polyhedrons[i];
      if (p.isValid && p.id == null && p.colixEdge != C.INHERIT_ALL
          && bsColixSet.get(ia = p.centralAtom.i))
        appendCmd(
            s,
            "select ({"
                + ia
                + "}); color polyhedra "
                + (C.isColixTranslucent(colixes[ia]) ? "translucent "
                    : "") + C.getHexCode(colixes[ia]) + " "
                + C.getHexCode(p.colixEdge));
    }
    return s.toString();
  }
}
