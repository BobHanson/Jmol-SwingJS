/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-02-25 17:19:14 -0600 (Sat, 25 Feb 2006) $
 * $Revision: 4529 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.SB;

import java.util.Hashtable;

import java.util.Map;


import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Escape;
import org.jmol.util.Font;
import org.jmol.util.Logger;
import org.jmol.util.MeshSurface;

import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.P3i;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.T3;
import javajs.util.V3;

import org.jmol.viewer.ActionManager;
import org.jmol.viewer.JC;
import javajs.util.BS;
import org.jmol.script.SV;
import org.jmol.script.T;
import org.jmol.shape.Mesh;
import org.jmol.shape.MeshCollection;


public class Draw extends MeshCollection {

  // bob hanson hansonr@stolaf.edu 3/2006
  
  private final static int PT_COORD = 1;
  private final static int PT_IDENTIFIER = 2;
  private final static int PT_BITSET = 3;
  private final static int PT_MODEL_INDEX = 4;
  private final static int PT_MODEL_BASED_POINTS = 5;

  public Draw() {
    // from reflection
    htObjects = new Hashtable<String, Mesh>();
  }

  DrawMesh[] dmeshes = new DrawMesh[4];
  private DrawMesh thisMesh;
  
  @Override
  public void allocMesh(String thisID, Mesh m) {
    int index = meshCount++;
    meshes = dmeshes = (DrawMesh[]) AU.ensureLength(dmeshes,
        meshCount * 2);
    currentMesh = thisMesh = dmeshes[index] = (m == null ? new DrawMesh(vwr, thisID,
        colix, index) : (DrawMesh) m);
    currentMesh.color = color;
    currentMesh.index = index;
    if (thisID != null && thisID != MeshCollection.PREVIOUS_MESH_ID
        && htObjects != null)
      htObjects.put(thisID.toUpperCase(), currentMesh);
  }

  private void setPropertySuper(String propertyName, Object value, BS bs) {
    currentMesh = thisMesh;
    setPropMC(propertyName, value, bs);
    thisMesh = (DrawMesh)currentMesh;  
  }
  
 @Override
public void initShape() {
    super.initShape();
    myType = "draw";
  }
  
  private P3[] ptList;
  private V3 offset = new V3();
  private int nPoints;
  private int diameter;
  private float width;
  private float newScale;
  private float length;
  private boolean isCurve;
  private boolean isArc;
  private boolean isArrow;
  private boolean isLine;
  private boolean isVector;
  private boolean isCircle;
  private boolean isPerpendicular;
  private boolean isCylinder;
  private boolean isVertices;
  private boolean isPlane;
  private boolean isReversed;
  private boolean isRotated45;
  private boolean isCrossed;
  private boolean isValid;
  private boolean noHead;
  private boolean isBarb;
  private int indicatedModelIndex = -1;
  private int[] modelInfo;
  private boolean makePoints;
  //private int nidentifiers;
  //private int nbitsets;
  private P4 plane;
  private BS bsAllModels;
  private Lst<Object> polygon;
  
  private Lst<Object[]> vData;
  private String intersectID;
  private P3[] boundBox;
  
  private Lst<P3[]> lineData;
  public int defaultFontId0 = -1;
  public int defaultFontId = -1;
  private int thisFontID = -1;
  private Integer titleColor;

  @SuppressWarnings("unchecked")
  @Override
  public void setProperty(String propertyName, Object value, BS bs) {

    if ("init" == propertyName) {
      initDraw();
      setPropertySuper("init", value, bs);
      return;
    }

    if ("font" == propertyName) {
      defaultFontId = (value == null ? -1 : ((Font) value).fid);
      defaultFontSize = ((Font) value).fontSize;
      return;
    }
    
    if ("myfont" == propertyName) {
      thisFontID = ((Font) value).fid;
      if (thisMesh != null) {
        thisMesh.fontID = thisFontID; 
      }
      return;
    }
    
    if ("length" == propertyName) {
      length = ((Float) value).floatValue();
      return;
    }

    if ("fixed" == propertyName) {
      isFixed = ((Boolean) value).booleanValue();
      return;
    }

    if ("intersect" == propertyName) {
      if (value instanceof String)
        intersectID = (String) value;
      else
        boundBox = (P3[]) value;
      return;
    }

    if ("slab" == propertyName) {
      int meshIndex = getIndexFromName((String) value);
      if (meshIndex < 0) {
        // could be isosurface?
        return;
      }
      Mesh m = meshes[meshIndex];
      if (m.checkByteCount != 1)
        return;
      MeshSurface ms = new MeshSurface();
      ms.vs = m.vs;
      ms.vvs = new float[m.vc];
      ms.vc = m.vc;
      ms.pis = m.pis;
      ms.pc = m.pc;
      ms.dataOnly = true;
      slabData = ms;
    }

    if ("lineData" == propertyName) {
      lineData = new Lst<P3[]>();
      if (indicatedModelIndex < 0)
        indicatedModelIndex = vwr.am.cmi;
      float[] fdata = (float[]) value;
      int n = fdata.length / 6;
      for (int i = 0, pt = 0; i < n; i++)
        lineData.addLast(new P3[] {
            P3.new3(fdata[pt++], fdata[pt++], fdata[pt++]),
            P3.new3(fdata[pt++], fdata[pt++], fdata[pt++]) });
      return;
    }

    if ("modelIndex" == propertyName) {
      //from saved state -- used to set modelVertices
      indicatedModelIndex = ((Integer) value).intValue();
      if (indicatedModelIndex < 0 || indicatedModelIndex >= vwr.ms.mc)
        return;
      vData.addLast(new Object[] { Integer.valueOf(PT_MODEL_INDEX),
          (modelInfo = new int[] { indicatedModelIndex, 0 }) });
      return;
    }

    if ("planedef" == propertyName) {
      plane = (P4) value;
      if (intersectID != null || boundBox != null || slabData != null)
        return;
      if (isCircle || isArc)
        isPlane = true;
      vData.addLast(new Object[] { Integer.valueOf(PT_COORD),
          P3.new3(Float.NaN, Float.NaN, Float.NaN) });
      return;
    }

    if ("perp" == propertyName) {
      isPerpendicular = true;
      return;
    }

    if ("cylinder" == propertyName) {
      isCylinder = true;
      return;
    }

    if ("plane" == propertyName) {
      isPlane = true;
      return;
    }

    if ("curve" == propertyName) {
      isCurve = true;
      return;
    }

    if ("arrow" == propertyName) {
      isArrow = true;
      return;
    }

    if ("line" == propertyName) {
      isLine = true;
      isCurve = true;
      return;
    }

    if ("arc" == propertyName) {
      isCurve = true;
      isArc = true;
      if (isArrow) {
        isArrow = false;
        isVector = true;
      }
      return;
    }

    if ("circle" == propertyName) {
      isCircle = true;
      return;
    }

    if ("vector" == propertyName) {
      isArrow = true;
      isVector = true;
      return;
    }

    if ("vertices" == propertyName) {
      isVertices = true;
      return;
    }

    if ("reverse" == propertyName) {
      isReversed = true;
      return;
    }

    if ("nohead" == propertyName) {
      noHead = true;
      return;
    }

    if ("isbarb" == propertyName) {
      isBarb = true;
      return;
    }

    if ("rotate45" == propertyName) {
      isRotated45 = true;
      return;
    }

    if ("crossed" == propertyName) {
      isCrossed = true;
      return;
    }

    if ("points" == propertyName) {
      newScale = ((Integer) value).floatValue() / 100;
      if (newScale == 0)
        newScale = 1;
      return;
    }

    if ("scale" == propertyName) {
      newScale = ((Integer) value).floatValue() / 100;
      if (newScale == 0)
        newScale = 0.01f; // very tiny but still sizable;
      if (thisMesh != null) {
        // no points in this script statement
        scale(thisMesh, newScale);
        thisMesh.initialize(T.fullylit, null, null);
      }
      return;
    }

    if ("diameter" == propertyName) {
      diameter = ((Float) value).intValue();
      return;
    }

    if ("width" == propertyName) {
      width = ((Float) value).floatValue();
      return;
    }

    if ("identifier" == propertyName) {
      String thisID = (String) value;
      int meshIndex = getIndexFromName(thisID);
      if (meshIndex >= 0) {
        vData.addLast(new Object[] { Integer.valueOf(PT_IDENTIFIER),
            new int[] { meshIndex, isReversed ? 1 : 0, isVertices ? 1 : 0 } });
        isReversed = isVertices = false;
        //nidentifiers++;
      } else {
        Logger.error("draw identifier " + value + " not found");
        isValid = false;
      }
      return;
    }

    if ("polygon" == propertyName) {
      polygon = (Lst<Object>) value;
      if (polygon == null)
        polygon = new Lst<Object>();
      return;
    }

    if ("coord" == propertyName) {
      vData.addLast(new Object[] { Integer.valueOf(PT_COORD), value });
      if (indicatedModelIndex >= 0)
        modelInfo[1]++; // counts vertices
      return;
    }

    if ("offset" == propertyName) {
      offset = V3.newV((P3) value);
      if (thisMesh != null)
        thisMesh.offset(offset);
      return;
    }

    if ("atomSet" == propertyName) {
      BS bsAtoms = (BS) value;
      if (bsAtoms.isEmpty())
        return;
      vData.addLast(new Object[] { Integer.valueOf(PT_BITSET), bsAtoms });
      //nbitsets++;
      if (isCircle && diameter == 0 && width == 0)
        width = vwr.ms.calcRotationRadiusBs(bsAtoms) * 2.0f;
      return;
    }

    if ("coords" == propertyName) {
      addPoints(PT_COORD, value);
      return;
    }

    if ("modelBasedPoints" == propertyName) {
      addPoints(PT_MODEL_BASED_POINTS, value);
      return;
    }
    
    if ("title" == propertyName) {
      if (thisMesh != null) {
        thisMesh.title = setTitle(value);
        return;
      }
    }

    if ("titlecolor" == propertyName) {
      Integer c = ((Integer) value);
      titleColor = c;
      if (thisMesh != null) {
        thisMesh.titleColor = c;
      }
      return;
    }

    if ("set" == propertyName) {
      if (thisMesh == null) {
        allocMesh(null, null);
        thisMesh.colix = colix;
        thisMesh.color = color;
      }
      thisMesh.isValid = (isValid ? setDrawing((int[]) value) : false);
      if (thisMesh.isValid) {
        if (thisMesh.vc > 2 && length != Float.MAX_VALUE && newScale == 1)
          newScale = length;
        scale(thisMesh, newScale);
        thisMesh.initialize(T.fullylit, null, null);
        setAxes(thisMesh);
        thisMesh.title = title;
        thisMesh.titleColor = titleColor;
        thisMesh.fontID = thisFontID;
        thisMesh.visible = true;
      }
      nPoints = -1; // for later scaling
      vData = null;
      lineData = null;
      return;
    }

    if (propertyName == "deleteModelAtoms") {
      deleteModels(((int[]) ((Object[]) value)[2])[0]);
      return;
    }

    setPropertySuper(propertyName, value, bs);
  }

  private void deleteModels(int modelIndex) {
    //int firstAtomDeleted = ((int[])((Object[])value)[2])[1];
    //int nAtomsDeleted = ((int[])((Object[])value)[2])[2];
    for (int i = meshCount; --i >= 0;) {
      DrawMesh m = dmeshes[i];
      if (m == null)
        continue;
      boolean deleteMesh = (m.modelIndex == modelIndex);
      if (m.modelFlags != null) {
        m.deleteAtoms(modelIndex);
        deleteMesh = (m.modelFlags.length() == 0);
        if (!deleteMesh)
          continue;
      }
      if (deleteMesh) {
        meshCount--;
        deleteMeshElement(i);
      } else if (meshes[i].modelIndex > modelIndex) {
        meshes[i].modelIndex--;
      }
    }
    resetObjects();
  }

 private void deleteMeshElement(int i) {
   if (meshes[i] == currentMesh)
     currentMesh = thisMesh = null;
   meshes = dmeshes = (DrawMesh[]) AU
       .deleteElements(meshes, i, 1);
}

private void initDraw() {
   boundBox = null;
   bsAllModels = null;
   colix = C.ORANGE;
   color = 0xFFFFFFFF;
   diameter = 0;
   explicitID = false;
   thisFontID = -1;
   indicatedModelIndex = -1;
   intersectID = null;
   isCurve = isArc = isArrow = isPlane = isCircle = isCylinder = isLine = false;
   isFixed = isReversed = isRotated45 = isCrossed = noHead = isBarb = false;
   isPerpendicular = isVertices = isVector = false;
   isValid = true;
   length = Float.MAX_VALUE;
   lineData = null;
   newScale = 0;
   //nidentifiers = nbitsets = 0;
   offset = null;
   plane = null;
   polygon = null;
   slabData = null;
   titleColor = null;
   vData = new  Lst<Object[]>();
   width = 0;
   setPropertySuper("thisID", MeshCollection.PREVIOUS_MESH_ID, null);
  }

  @Override
  public boolean getPropertyData(String property, Object[] data) {
    if (property == "keys") {
      @SuppressWarnings("unchecked")
      Lst<String> keys = (data[1] instanceof Lst<?> ? (Lst<String>) data[1] : new Lst<String>());
      data[1] = keys;
      keys.addLast("getSpinAxis");
      // will continue on to super
    }
    if (property == "getCenter") {
      String id = (String) data[0];
      int index = ((Integer) data[1]).intValue();
      int modelIndex = ((Integer) data[2]).intValue();
      data[2] = getSpinCenter(id, index, modelIndex);
      return (data[2] != null);
    }
    if (property == "getSpinAxis") {
      String id = (String) data[0];
      int index = ((Integer) data[1]).intValue();
      data[2] =  getSpinAxis(id, index);
      return (data[2] != null);
    }
    return getPropDataMC(property, data);
  }

  @Override
  public Object getProperty(String property, int index) {
    DrawMesh m = thisMesh;
    if (index >= 0 && (index >= meshCount || (m = (DrawMesh) meshes[index]) == null))
      return null;
    if (property.equals("font")) {
      if (defaultFontId < 0) {
        setProperty("font", vwr.gdata.getFont3DFSS(JC.DEFAULT_FONTFACE,
            JC.DEFAULT_FONTSTYLE, vwr.getFloat(T.drawfontsize)), null);
        defaultFontId0 = defaultFontId;
      }
      return Font.getFont3D(index < 0 || m.fontID < 0 ? defaultFontId : m.fontID);
    }

    if (property == "command")
      return getCommand(m);
    if (property == "type")
      return Integer.valueOf(m == null ? EnumDrawType.NONE.id : m.drawType.id);
    return getPropMC(property, index);
  }

  private T3 getSpinCenter(String axisID, int vertexIndex, int modelIndex) {
    String id;
    int pt = axisID.indexOf("[");
    int pt2;
    if (pt > 0) {
      id = axisID.substring(0, pt);
      if ((pt2 = axisID.lastIndexOf("]")) < pt)
        pt2 = axisID.length();
      try {
        vertexIndex = Integer.parseInt(axisID.substring(pt + 1, pt2));
      } catch (Exception e) {
        // ignore
      }
    } else {
      id = axisID;
    }
    DrawMesh m = (DrawMesh) getMesh(id);
    if (m == null || m.vs == null)
      return null;
    // >= 0 ? that vertexIndex
    // < 0 and no ptCenters or modelIndex < 0 -- center point
    // < 0 center for modelIndex
    if (vertexIndex == Integer.MAX_VALUE)
      return P3.new3(m.index + 1, meshCount, m.vc);
    if (vertexIndex != Integer.MIN_VALUE) 
      vertexIndex = m.getVertexIndexFromNumber(vertexIndex);
    return (vertexIndex >= 0 ? m.vs[vertexIndex] : m.ptCenters == null
        || modelIndex < 0 || modelIndex >= m.ptCenters.length 
        ? m.ptCenter : m.ptCenters[modelIndex]);
  }
   
  private V3 getSpinAxis(String axisID, int modelIndex) {
    DrawMesh m = (DrawMesh) getMesh(axisID);
    return (m == null || m.vs == null ? null 
        : m.ptCenters == null || modelIndex < 0 ? m.axis : m.axes[modelIndex]);
   }
  
  private boolean setDrawing(int[] connections) {
    if (thisMesh == null)
      allocMesh(null, null);
    thisMesh.clear("draw");
    thisMesh.diameter = diameter;
    thisMesh.width = width;
    if (intersectID != null || boundBox != null) {
      if (boundBox != null) {
        // TODO
        if (plane == null) {

        }
      } else if (plane != null && intersectID != null) {
        Lst<P3[]> vData = new Lst<P3[]>();
        Object[] data = new Object[] { intersectID, plane, vData, null };
        vwr.shm.getShapePropertyData(JC.SHAPE_ISOSURFACE, "intersectPlane",
            data);
        if (vData.size() > 0) {
          indicatedModelIndex = ((Integer) data[3]).intValue();
          lineData = vData;
        }
      }
    } else if (slabData != null && plane != null) {
      slabData.getMeshSlicer().getIntersection(0, plane, null, null, null,
          null, null, false, true, T.plane, false);
      polygon = new Lst<Object>();
      polygon.addLast(slabData.vs);
      polygon.addLast(slabData.pis);
    }
    if (polygon == null
        && (lineData != null ? lineData.size() == 0
            : (vData.size() == 0) == (connections == null)) || !isArrow
        && connections != null)
      return false; // connections only for arrows at this point
    int modelCount = vwr.ms.mc;
    if (polygon != null
        || lineData != null
        || indicatedModelIndex < 0
        && (isFixed || isArrow || isCurve || isCircle || isCylinder || modelCount == 1)) {
      // make just ONE copy 
      
      // arrows and curves simply can't be handled as
      // multiple frames yet
      thisMesh.modelIndex = (lineData == null ? vwr.am.cmi
          : indicatedModelIndex);
      thisMesh.isFixed = (isFixed || lineData == null
          && thisMesh.modelIndex < 0 && modelCount > 1);
      if (isFixed && modelCount > 1)
        thisMesh.modelIndex = -1;
      else if (lineData == null && thisMesh.modelIndex < 0)
        thisMesh.modelIndex = 0;
      thisMesh.ptCenters = null;
      thisMesh.modelFlags = null;
      thisMesh.drawTypes = null;
      thisMesh.drawVertexCounts = null;
      thisMesh.connectedAtoms = connections;
      if (polygon != null) {
        if (polygon.size() == 0)
          return false;
        thisMesh.isDrawPolygon = true;
        thisMesh.vs = (P3[]) polygon.get(0);
        thisMesh.pis = (int[][]) polygon.get(1);
        thisMesh.drawVertexCount = thisMesh.vc = thisMesh.vs.length;
        thisMesh.pc = (thisMesh.pis == null ? -1 : thisMesh.pis.length);
        for (int i = 0; i < thisMesh.pc; i++) {
          for (int j = 0; j < 3; j++)
            if (thisMesh.pis[i][j] >= thisMesh.vc)
              return false;
        }
        thisMesh.drawType = EnumDrawType.POLYGON;
        thisMesh.checkByteCount = 1;
      } else if (lineData != null) {
        thisMesh.lineData = lineData;
      } else {
        thisMesh.setPolygonCount(1);
        if (setPoints(-1, -1))
          setPoints(-1, nPoints);
        setPolygon(0);
      }
    } else {
      // multiple copies, one for each model involved
      thisMesh.modelIndex = -1;
      thisMesh.setPolygonCount(modelCount);
      thisMesh.ptCenters = new P3[modelCount];
      thisMesh.modelFlags = new BS();
      thisMesh.drawTypes = new EnumDrawType[modelCount];
      thisMesh.drawVertexCounts = new int[modelCount];
      thisMesh.vc = 0;
      if (indicatedModelIndex >= 0) {
        setPoints(-1, 0);
        thisMesh.drawType = EnumDrawType.MULTIPLE;
        thisMesh.drawVertexCount = -1;
        thisMesh.modelFlags.set(indicatedModelIndex);
        indicatedModelIndex = -1;
      } else {
        BS bsModels = vwr.getVisibleFramesBitSet();
        for (int iModel = 0; iModel < modelCount; iModel++) {
          if (bsModels.get(iModel) && setPoints(iModel, -1)) {
            setPoints(iModel, nPoints);
            setPolygon(iModel);
            thisMesh.setCenter(iModel);
            thisMesh.drawTypes[iModel] = thisMesh.drawType;
            thisMesh.drawVertexCounts[iModel] = thisMesh.drawVertexCount;
            thisMesh.drawType = EnumDrawType.MULTIPLE;
            thisMesh.drawVertexCount = -1;
            thisMesh.modelFlags.set(iModel);
          } else {
            thisMesh.drawTypes[iModel] = EnumDrawType.NONE;
            thisMesh.pis[iModel] = new int[0];
          }
        }
      }
    }
    thisMesh.isVector = isVector;
    thisMesh.noHead = noHead;
    thisMesh.isBarb = isBarb;
    thisMesh.width = (thisMesh.drawType == EnumDrawType.CYLINDER
        || thisMesh.drawType == EnumDrawType.CIRCULARPLANE ? -Math.abs(width)
        : width);
    thisMesh.setCenter(-1);
    if (offset != null)
      thisMesh.offset(offset);
    if (thisMesh.thisID == null) {
      thisMesh.thisID = thisMesh.drawType.name + (++nUnnamed);
      htObjects.put(thisMesh.thisID, thisMesh);
    }
    clean();
    return true;
  }
  
  @Override
  protected void clean() {
    for (int i = meshCount; --i >= 0;)
      if (meshes[i] == null || meshes[i].vc == 0 
          && meshes[i].connectedAtoms == null && meshes[i].lineData == null)
        deleteMeshI(i);
  }

  MeshSurface slabData;
  
  private void addPoints(int type, Object value) {
    @SuppressWarnings("unchecked")
    Lst<SV> pts = (Lst<SV>) value;
    Integer key = Integer.valueOf(type);
    boolean isModelPoints = (type == PT_MODEL_BASED_POINTS);
    if (isModelPoints)
      vData.addLast(new Object[] { key, pts });
    for (int i = 0, n = pts.size(); i < n; i++) {
      Object o = pts.get(i);
      P3 pt;
      if (o instanceof P3) {
        // from Draw HKL
        pt = (P3) o;
      } else {
        SV v = (SV) o;
        switch (v.tok) {
        case T.bitset:
          if (!isModelPoints && ((BS) v.value).isEmpty())
            continue;
          pt = vwr.ms.getAtomSetCenter((BS) v.value);
          break;
        case T.point3f:
          if (isModelPoints)
            continue;
          //$FALL-THROUGH$
        default:
          pt = SV.ptValue(v);
        }
      }
      if (isModelPoints) {
        pts.set(i, SV.getVariable(pt));
      } else {
        vData.addLast(new Object[] { key, pt });
      }
    }
  }

  private void addPoint(T3 newPt, int iModel) {
    if (makePoints) {
      if (newPt == null || iModel >= 0 && !bsAllModels.get(iModel))
        return;
      ptList[nPoints] = P3.newP(newPt);
      if (newPt.z == Float.MAX_VALUE || newPt.z == -Float.MAX_VALUE)
        thisMesh.haveXyPoints = true;
    } else if (iModel >= 0) {
      bsAllModels.set(iModel);
    }
    nPoints++;
  }

  private boolean setPoints(int iModel, int n) {
    // {x,y,z} points are already defined in ptList
    // $drawID references may be fixed or not
    // Prior to 11.5.37, points were created in the order:
    //  1) all {x,y,z} points
    //  2) all $drawID points
    //  3) all {atomExpression} points
    //  4) all {atomExpression}.split() points
    // Order is only important when there are four points, 
    // where they may become crossed, so
    // we also provide a flag CROSSED to uncross them
    this.makePoints = (n >= 0);
    if (makePoints) {
      ptList = new P3[Math.max(5,n)];
      if (bsAllModels == null)
        bsAllModels = vwr.getVisibleFramesBitSet();
    }
    nPoints = 0;
    int nData = vData.size();
    int modelIndex = 0;
    BS bs;
    BS bsModel = (iModel < 0 ? null : vwr.getModelUndeletedAtomsBitSet(iModel));
    for (int i = 0; i < nData; i++) {
      Object[] info = vData.get(i);
      switch (((Integer) info[0]).intValue()) {
      case PT_MODEL_INDEX:
        // from the saved state
        int[] modelInfo = (int[]) info[1];
        modelIndex = modelInfo[0];
        nPoints = modelInfo[1];
        int nVertices = Math.max(nPoints, 3);
        int n0 = thisMesh.vc;
        if (nPoints > 0) {
          int[] p = thisMesh.pis[modelIndex] = new int[nVertices];
          for (int j = 0; j < nPoints; j++) {
            info = vData.get(++i);
            p[j] = thisMesh.addV((P3) info[1], false);
          }
          for (int j = nPoints; j < 3; j++) {
            p[j] = n0 + nPoints - 1;
          }
          thisMesh.drawTypes[modelIndex] = EnumDrawType.getType(nPoints);
          thisMesh.drawVertexCounts[modelIndex] = nPoints;
          thisMesh.modelFlags.set(modelIndex);
        }
        break;
      case PT_COORD:
        addPoint((P3) info[1], (makePoints ? iModel : -1));
        break;
      case PT_BITSET:
        // (atom set) references must be filtered for relevant model
        // note that if a model doesn't have a relevant point, one may
        // get a line instead of a plane, a point instead of a line, etc.
        bs = BSUtil.copy((BS) info[1]);
        if (bsModel != null)
          bs.and(bsModel);
        if (bs.length() > 0)
          addPoint(vwr.ms.getAtomSetCenter(bs), (makePoints ? iModel : -1));
        break;
      case PT_IDENTIFIER:
        int[] idInfo = (int[]) info[1];
        DrawMesh m = dmeshes[idInfo[0]];
        boolean isReversed = (idInfo[1] == 1);
        boolean isVertices = (idInfo[2] == 1);
        if (m.modelIndex > 0 && m.modelIndex != iModel)
          return false;
        if (bsAllModels == null)
          bsAllModels = new BS();
        if (isPlane && !isCircle || isPerpendicular || isVertices) {
          if (isReversed) {
            if (iModel < 0 || iModel >= m.pc)
              for (int ipt = m.drawVertexCount; --ipt >= 0;)
                addPoint(m.vs[ipt], iModel);
            else if (m.pis[iModel] != null)
              for (int ipt = m.drawVertexCounts[iModel]; --ipt >= 0;)
                addPoint(m.vs[m.pis[iModel][ipt]], iModel);
          } else {
            if (iModel < 0 || iModel >= m.pc)
              for (int ipt = 0; ipt < m.drawVertexCount; ipt++)
                addPoint(m.vs[ipt], iModel);
            else if (m.pis[iModel] != null)
              for (int ipt = 0; ipt < m.drawVertexCounts[iModel]; ipt++)
                addPoint(m.vs[m.pis[iModel][ipt]], iModel);
          }
        } else {
          if (iModel < 0 || m.ptCenters == null || m.ptCenters[iModel] == null)
            addPoint(m.ptCenter, iModel);
          else
            addPoint(m.ptCenters[iModel], iModel);
        }
        break;
      case PT_MODEL_BASED_POINTS:
        // from list variables
        @SuppressWarnings("unchecked")
        Lst<SV> modelBasedPoints = (Lst<SV>)info[1];
        if (bsAllModels == null)
          bsAllModels = new BS();
        for (int j = 0; j < modelBasedPoints.size(); j++)
          if (iModel < 0 || j == iModel) {
            Object point = modelBasedPoints.get(j);
            bsAllModels.set(j);
            if (point instanceof P3) {
              addPoint((P3) point, j);
            } else if (point instanceof BS) {
              bs = (BS) point;
              if (bsModel != null)
                bs.and(bsModel);
              if (bs.length() > 0)
                addPoint(vwr.ms.getAtomSetCenter(bs), j);
            } else if (point instanceof SV) {
              addPoint(SV.ptValue((SV) point), j);
            }
          }
        break;
      }
    }
    if (makePoints && isCrossed && nPoints == 4) {
      P3 pt = ptList[1];
      ptList[1] = ptList[2];
      ptList[2] = pt;
    }
    return (nPoints > 0);
  }

  private final V3 vAB = new V3();

  private void setPolygon(int nPoly) {
    int nVertices = nPoints;
    EnumDrawType drawType = EnumDrawType.POINT;
    if (isArc) {
      if (nVertices >= 2) {
        drawType = EnumDrawType.ARC;
      } else {
        isArc = false;
        isVector = false;
        isCurve = false;
        isArrow = true;
      }
    }
    if (isCircle) {
      length = 0;
      if (nVertices == 2)
        isPlane = true;
      if (!isPlane)
        drawType = EnumDrawType.CIRCLE;
      if (width == 0)
        width = 1;
    } else if ((isCurve || isArrow) && nVertices >= 2 && !isArc) {
      drawType = (isLine ? EnumDrawType.LINE_SEGMENT
          : isCurve ? EnumDrawType.CURVE : EnumDrawType.ARROW);
    }
    if (isVector && !isArc) {
      if (nVertices > 2)
        nVertices = 2;
      else if (plane == null && nVertices != 2)
        isVector = false;
    }
    if (thisMesh.haveXyPoints) {
      isPerpendicular = false;
      if (nVertices == 3 && isPlane)
        isPlane = false;
      length = Float.MAX_VALUE;
      if (isVector)
        thisMesh.diameter = 0;
    } else if (nVertices == 2 && isVector) {
      ptList[1].add(ptList[0]);
    }
    float dist = 0;
    if (isArc || plane != null && isCircle) {
      if (plane != null) {
        dist = Measure.distanceToPlane(plane, ptList[0]);
        V3 vAC = V3.new3(-plane.x, -plane.y, -plane.z);
        vAC.normalize();
        if (dist < 0)
          vAC.scale(-1);
        if (isCircle) {
          vAC.scale(0.005f);
          ptList[0].sub(vAC);
          vAC.scale(2);
        }
        vAC.add(ptList[0]);
        ptList[1] = P3.newP(vAC);
        drawType = (isArrow ? EnumDrawType.ARROW
            : isArc ? EnumDrawType.ARC : EnumDrawType.CIRCULARPLANE);
      }
      if (isArc) {
        dist = Math.abs(dist);
        if (nVertices > 3) {
          // draw arc {center} {pt2} {ptRef} {angleOffset theta
          // fractionalAxisOffset}
        } else if (nVertices == 3) {
          // draw arc {center} {pt2} {angleOffset theta fractionalAxisOffset}
          ptList[3] = P3.newP(ptList[2]);
          ptList[2] = randomPoint();
        } else {
          if (nVertices == 2) {
            // draw arc {center} {pt2}
            ptList[2] = randomPoint();
          }
          ptList[3] = P3.new3(0, 360, 0);
        }
        if (plane != null)
          ptList[3].z *= dist;
        nVertices = 4;
      }
      plane = null;
    } else if (drawType == EnumDrawType.POINT) {
      P3 pt;
      P3 center = new P3();
      V3 normal = new V3();
      if (nVertices == 2 && plane != null) {
        ptList[1] = P3.newP(ptList[0]);
        V3 vTemp = new V3();
        Measure.getPlaneProjection(ptList[1], plane, ptList[1], vTemp);
        nVertices = -2;
        if (isArrow)
          drawType = EnumDrawType.ARROW;
        plane = null;
      }
      if (nVertices == 3 && isPlane && !isPerpendicular) {
        // three points define a plane
        pt = P3.newP(ptList[1]);
        pt.sub(ptList[0]);
        pt.scale(0.5f);
        ptList[3] = P3.newP(ptList[2]);
        ptList[2].add(pt);
        ptList[3].sub(pt);
        nVertices = 4;
      } else if (nVertices >= 3 && !isPlane && isPerpendicular) {
        // normal to plane
        Measure.calcNormalizedNormal(ptList[0], ptList[1], ptList[2], normal,
            vAB);
        center = new P3();
        Measure.calcAveragePointN(ptList, nVertices, center);
        dist = (length == Float.MAX_VALUE ? ptList[0].distance(center) : length);
        normal.scale(dist);
        ptList[0].setT(center);
        ptList[1].add2(center, normal);
        nVertices = 2;
      } else if (nVertices == 2 && isPerpendicular) {
        // perpendicular line to line or plane to line
        Measure.calcAveragePoint(ptList[0], ptList[1], center);
        dist = (length == Float.MAX_VALUE ? ptList[0].distance(center) : length);
        if (isPlane && length != Float.MAX_VALUE)
          dist /= 2f;
        if (isPlane && isRotated45)
          dist *= 1.4142f;
        Measure.getNormalToLine(ptList[0], ptList[1], normal);
        normal.scale(dist);
        if (isPlane) {
          ptList[2] = P3.newP(center);
          ptList[2].sub(normal);
          pt = P3.newP(center);
          pt.add(normal);
          // pt
          // |
          // 0-------+--------1
          // |
          // 2
          Measure.calcNormalizedNormal(ptList[0], ptList[1], ptList[2], normal,
              vAB);
          normal.scale(dist);
          ptList[3] = P3.newP(center);
          ptList[3].add(normal);
          ptList[1].sub2(center, normal);
          ptList[0].setT(pt);
          //             
          // pt,0 1
          // |/
          // -------+--------
          // /|
          // 3 2

          if (isRotated45) {
            Measure.calcAveragePoint(ptList[0], ptList[1], ptList[0]);
            Measure.calcAveragePoint(ptList[1], ptList[2], ptList[1]);
            Measure.calcAveragePoint(ptList[2], ptList[3], ptList[2]);
            Measure.calcAveragePoint(ptList[3], pt, ptList[3]);
          }
          nVertices = 4;
        } else {
          ptList[0].sub2(center, normal);
          ptList[1].add2(center, normal);
        }
        if (isArrow && nVertices != -2)
          isArrow = false;
      } else if (nVertices == 2 && length != Float.MAX_VALUE) {
        Measure.calcAveragePoint(ptList[0], ptList[1], center);
        normal.sub2(ptList[1], center);
        normal.scale(0.5f / normal.length() * (length == 0 ? 0.01f : length));
        if (length == 0)
          center.setT(ptList[0]);
        ptList[0].sub2(center, normal);
        ptList[1].add2(center, normal);
      }
      if (nVertices > 4)
        nVertices = 4; // for now

      switch (nVertices) {
      case -2:
        nVertices = 2;
        break;
      case 1:
        break;
      case 2:
        drawType = (isArc ? EnumDrawType.ARC
            : isPlane && isCircle ? EnumDrawType.CIRCULARPLANE
                : isCylinder ? EnumDrawType.CYLINDER
                    : EnumDrawType.LINE);
        break;
      default:
        drawType = (thisMesh.connectedAtoms == null ? EnumDrawType.PLANE
            : EnumDrawType.ARROW);
      }
    }
    thisMesh.drawType = drawType;
    thisMesh.drawVertexCount = nVertices;

    if (nVertices == 0)
      return;
    int nVertices0 = thisMesh.vc;
    for (int i = 0; i < nVertices; i++) {
      thisMesh.addV(ptList[i], false);
    }
    int npoints = (nVertices < 3 ? 3 : nVertices);
    thisMesh.setPolygonCount(nPoly + 1);
    thisMesh.pis[nPoly] = new int[npoints];
    for (int i = 0; i < npoints; i++) {
      thisMesh.pis[nPoly][i] = nVertices0
          + (i < nVertices ? i : nVertices - 1);
    }
    return;
  }

  private void scale(Mesh mesh, float newScale) {
    DrawMesh dmesh = (DrawMesh) mesh;
    /*
     * allows for Draw to scale object
     * have to watch out for double-listed vertices
     * 
     */
    if (newScale == 0 || dmesh.vc == 0 && dmesh.connectedAtoms == null
        || dmesh.scale == newScale)
      return;
    float f = newScale / dmesh.scale;
    dmesh.scale = newScale;
    dmesh.isScaleSet = true;
    if (dmesh.isRenderScalable())
      return; // done in renderer
    V3 diff = new V3();
    int iptlast = -1;
    int ipt = 0;
    try {
      for (int i = dmesh.pc; --i >= 0;) {
        T3 center = (dmesh.isVector ? dmesh.vs[0]
            : dmesh.ptCenters == null ? dmesh.ptCenter : dmesh.ptCenters[i]);
        if (center == null)
          return;
        if (dmesh.pis[i] == null)
          continue;
        iptlast = -1;
        for (int iV = dmesh.pis[i].length; --iV >= 0;) {
          ipt = dmesh.pis[i][iV];
          if (ipt == iptlast)
            continue;
          iptlast = ipt;
          diff.sub2(dmesh.vs[ipt], center);
          diff.scale(f);
          diff.add(center);
          dmesh.vs[ipt].setT(diff);
        }
      }
    } catch (Exception e) {
      Logger.info("Error executing DRAW command: " + e);
      dmesh.isValid = false;
    }
  }

  private final static void setAxes(DrawMesh m) {
    m.axis = V3.new3(0, 0, 0);
    m.axes = new V3[m.pc > 0 ? m.pc : 1];
    if (m.vs == null)
      return;
    int n = 0;
    for (int i = m.pc; --i >= 0;) {
      int[] p = m.pis[i];
      m.axes[i] = new V3();
      if (p == null || p.length == 0) {
      } else if (m.drawVertexCount == 2 || m.drawVertexCount < 0
          && m.drawVertexCounts[i] == 2) {
        m.axes[i].sub2(m.vs[p[0]],
            m.vs[p[1]]);
        n++;
      } else {
        Measure.calcNormalizedNormal(m.vs[p[0]],
            m.vs[p[1]],
            m.vs[p[2]], m.axes[i], m.vAB);
        n++;
      }
      m.axis.add(m.axes[i]);
    }
    if (n == 0)
      return;
    m.axis.scale(1f / n);
  }

  @Override
  public void setModelVisibilityFlags(BS bsModels) {
    /*
     * set all fixed objects visible; others based on model being displayed note
     * that this is NOT done with atoms and bonds, because they have mads. When
     * you say "frame 0" it is just turning on all the mads.
     */
    for (int i = 0; i < meshCount; i++) {
      DrawMesh m = dmeshes[i];
      if (m == null) {
        continue;
      }
      m.visibilityFlags = (m.isValid && m.visible ? vf : 0);
      if (m.modelIndex >= 0 && !bsModels.get(m.modelIndex) || m.modelFlags != null
          && !BSUtil.haveCommon(bsModels, m.modelFlags)) {
        m.visibilityFlags = 0;
      } else if (m.modelFlags != null) {
        m.bsMeshesVisible.clearAll();
        m.bsMeshesVisible.or(m.modelFlags);
        m.bsMeshesVisible.and(bsModels);
      }

    }
  }
  
  private final static int MAX_OBJECT_CLICK_DISTANCE_SQUARED = 10 * 10;

  private final P3i ptXY = new P3i();
  private float defaultFontSize;
  
  @Override
  public Map<String, Object> checkObjectClicked(int x, int y, int action,
                                                BS bsVisible,
                                                boolean drawPicking) {
    boolean isPickingMode = (vwr.getPickingMode() == ActionManager.PICKING_DRAW);
    boolean isSpinMode = (vwr.getPickingMode() == ActionManager.PICKING_SPIN);
    if (!isPickingMode && !drawPicking && !isSpinMode
        || C.isColixTranslucent(colix))
      return null;
    if (!findPickedObject(x, y, false, bsVisible))
      return null;
    T3 v = pickedMesh.vs[pickedMesh.pis[pickedModel][pickedVertex]];
    int modelIndex = pickedMesh.modelIndex;
    BS bs = ((DrawMesh) pickedMesh).modelFlags;
    if (modelIndex < 0 && BSUtil.cardinalityOf(bs) == 1)
      modelIndex = bs.nextSetBit(0);
    Map<String, Object> map = null;
    if (action != 0)
      map = getPickedPoint(v, modelIndex);
    if (drawPicking && !isPickingMode) {
      if (action != 0) // not mouseMove
        setStatusPicked(-2, v, map);
      return getPickedPoint(v, modelIndex);
    }
    if (action == 0
        || pickedMesh.pis[pickedModel][0] == pickedMesh.pis[pickedModel][1]) {
      return map;
    }
    boolean isClockwise = vwr.isBound(action,
        ActionManager.ACTION_spinDrawObjectCW);
    if (pickedVertex == 0) {
      vwr.startSpinningAxis(
          pickedMesh.vs[pickedMesh.pis[pickedModel][1]],
          pickedMesh.vs[pickedMesh.pis[pickedModel][0]],
          isClockwise);
    } else {
      vwr.startSpinningAxis(
          pickedMesh.vs[pickedMesh.pis[pickedModel][0]],
          pickedMesh.vs[pickedMesh.pis[pickedModel][1]],
          isClockwise);
    }
    return getPickedPoint(null, 0);
  }

  @Override
  public boolean checkObjectHovered(int x, int y, BS bsVisible) {
    if (!vwr.getDrawHover())
      return false;
    if (C.isColixTranslucent(colix))
      return false;
    if (!findPickedObject(x, y, false, bsVisible))
      return false;
    if (vwr.gdata.antialiasEnabled) {
      //because hover rendering is done in FIRST pass only
      x <<= 1;
      y <<= 1;
    }      
    String s = (pickedMesh.title == null ? pickedMesh.thisID
        : pickedMesh.title[0]);
    if (s.length() > 1 && s.charAt(0) == '>')
      s = s.substring(1);
    vwr.hoverOnPt(x, y, s, pickedMesh.thisID, pickedPt);
    return true;
  }

  @Override
  public synchronized boolean checkObjectDragged(int prevX, int prevY, int x,
                                                 int y, int dragAction,
                                                 BS bsVisible) {
    //TODO -- can dispense with this first check:
    if (vwr.getPickingMode() != ActionManager.PICKING_DRAW)
      return false;
    boolean moveAll = vwr.isBound(dragAction,
        ActionManager.ACTION_dragDrawObject);
    boolean movePoint = vwr.isBound(dragAction,
        ActionManager.ACTION_dragDrawPoint);
    if (!moveAll && !movePoint)
      return false;
    // mouse down ?
    if (prevX == Integer.MIN_VALUE)
      return findPickedObject(x, y, true, bsVisible);
    // mouse up ?
    if (prevX == Integer.MAX_VALUE) {
      pickedMesh = null;
      return false;
    }
    if (pickedMesh == null)
      return false;
    DrawMesh dm = (DrawMesh) pickedMesh;
    move2D(dm, dm.pis[pickedModel], pickedVertex, x,
        y, moveAll);
    thisMesh = dm;
    return true;
  }
  
  private void move2D(DrawMesh mesh, int[] vertexes, 
                      int iVertex, int x, int y,
                      boolean moveAll) {
    if (vertexes == null || vertexes.length == 0)
      return;
    if (vwr.gdata.isAntialiased()) {
      x <<= 1;
      y <<= 1;
    }
    
    int action = moveAll ? ActionManager.ACTION_dragDrawObject : ActionManager.ACTION_dragDrawPoint;
    if (vwr.acm.userActionEnabled(action) 
    		&& !vwr.acm.userAction(action, new Object[] { mesh.thisID, new int[] {x, y, iVertex} } ))
    	return;
    P3 pt = new P3();
    int ptVertex = vertexes[iVertex];
    P3 coord = P3.newP(mesh.altVertices == null ? mesh.vs[ptVertex] : (P3) mesh.altVertices[ptVertex]);
    P3 newcoord = new P3();
    V3 move = new V3();
    vwr.tm.transformPt3f(coord, pt);
    pt.x = x;
    pt.y = y;
    vwr.tm.unTransformPoint(pt, newcoord);
    move.sub2(newcoord, coord);
    if (mesh.isDrawPolygon)
      iVertex = ptVertex; // operate on entire set of vertices, not just the
                          // one for this model
    int n = (!moveAll ? iVertex + 1 
        : mesh.isDrawPolygon ? mesh.vs.length : vertexes.length);
    BS bsMoved = new BS();
    for (int i = (moveAll ? 0 : iVertex); i < n; i++)
      if (moveAll || i == iVertex) {
        int k = (mesh.isDrawPolygon ? i : vertexes[i]);
        if (bsMoved.get(k))
          continue;
        bsMoved.set(k);
        mesh.vs[k].add(move);
      }
    if (mesh.altVertices != null)
      mesh.recalcAltVertices = true;
    mesh.setCenters();
  }
  
  /**
   * 
   * @param x
   * @param y
   * @param isPicking
   *        IGNORED
   * @param bsVisible
   * @return true if found
   */
  private boolean findPickedObject(int x, int y, boolean isPicking, BS bsVisible) {
    int dmin2 = MAX_OBJECT_CLICK_DISTANCE_SQUARED;
    if (vwr.gdata.isAntialiased()) {
      x <<= 1;
      y <<= 1;
      dmin2 <<= 1;
    }
    pickedModel = 0;
    pickedVertex = 0;
    pickedMesh = null;
    for (int i = 0; i < meshCount; i++) {
      DrawMesh m = dmeshes[i];
      if (m.visibilityFlags != 0) {
        int mCount = (m.isDrawPolygon ? m.pc
            : m.modelFlags == null ? 1 : vwr.ms.mc);
        for (int iModel = mCount; --iModel >= 0;) {
          if (m.modelFlags != null
              && !m.modelFlags.get(iModel)
              || m.pis == null
              || !m.isDrawPolygon
              && (iModel >= m.pis.length || m.pis[iModel] == null))
            continue;
          for (int iVertex = (m.isDrawPolygon ? 3
              : m.pis[iModel].length); --iVertex >= 0;) {
            try {
              int iv = m.pis[iModel][iVertex];
              T3 pt = (m.altVertices == null ? m.vs[iv]
                  : (P3) m.altVertices[iv]);
              int d2 = coordinateInRange(x, y, pt, dmin2, ptXY);
              if (d2 >= 0) {
                pickedMesh = m;
                dmin2 = d2;
                pickedModel = iModel;
                pickedVertex = iVertex;
                pickedPt = pt;
              }
            } catch (Exception e) {
              System.out.println(e);
            }
          }
        }
      }
    }
    return (pickedMesh != null);
  }

  private String getCommand(Mesh mesh) {
    if (mesh != null)
      return getCommand2(mesh, mesh.modelIndex);

    SB sb = new SB();
    String key = (explicitID && previousMeshID != null
        && PT.isWild(previousMeshID) ? previousMeshID : null);
    Lst<Mesh> list = getMeshList(key, false);
    // counting down on list will be up in order
    for (int i = list.size(); --i >= 0;) {
      Mesh m = list.get(i);
      sb.append(getCommand2(m, m.modelIndex));
    }
    return sb.toString();
  }

  private String getCommand2(Mesh mesh, int iModel) {
    DrawMesh dmesh = (DrawMesh) mesh;
    if (!dmesh.isValid || 
        dmesh.drawType == EnumDrawType.NONE  
        && dmesh.lineData == null 
        && dmesh.drawVertexCount == 0 && dmesh.drawVertexCounts == null
        )
      return "";
    SB str = new SB();
    int modelCount = vwr.ms.mc;
    if (!dmesh.isFixed && iModel >= 0 && modelCount > 1)
      appendCmd(str, "frame " + vwr.getModelNumberDotted(iModel));
    str.append("  draw ID ").append(PT.esc(dmesh.thisID));
    if (dmesh.isFixed)
      str.append(" fixed");
    if (iModel < 0)
      iModel = 0;
    if (dmesh.noHead)
      str.append(" noHead");
    else if (dmesh.isBarb)
      str.append(" barb");
    if (dmesh.scale != 1 && dmesh.isScaleSet
        && (dmesh.haveXyPoints || dmesh.connectedAtoms != null || dmesh.drawType == EnumDrawType.CIRCLE || dmesh.drawType == EnumDrawType.ARC))
      str.append(" scale ").appendF(dmesh.scale);
    if (dmesh.width != 0)
      str.append(" diameter ").appendF(
          (dmesh.drawType == EnumDrawType.CYLINDER ? Math.abs(dmesh.width)
              : dmesh.drawType == EnumDrawType.CIRCULARPLANE ? Math
                  .abs(dmesh.width * dmesh.scale) : dmesh.width));
    else if (dmesh.diameter > 0)
      str.append(" diameter ").appendI(dmesh.diameter);
    if (dmesh.lineData != null) {
      str.append("  lineData [");
      int n = dmesh.lineData.size();
      for (int j = 0; j < n;) {
        P3[] pts = dmesh.lineData.get(j);
        String s = Escape.eP(pts[0]);
        str.append(s.substring(1, s.length() - 1));
        str.append(",");
        s = Escape.eP(pts[1]);
        str.append(s.substring(1, s.length() - 1));
        if (++j < n)
          str.append(", ");
      }
      str.append("]");
    } else {
      int nVertices = dmesh.drawVertexCount > 0  || dmesh.drawVertexCounts == null ? dmesh.drawVertexCount
          : dmesh.drawVertexCounts[iModel >= 0 ? iModel : 0];
      switch (dmesh.drawTypes == null || dmesh.drawTypes[iModel] == null ? dmesh.drawType : dmesh.drawTypes[iModel]) {
      case NONE:
      case MULTIPLE:
        break;
      case POLYGON:
        str.append(" POLYGON ").appendI(nVertices);
        break;
      case PLANE:
        if (nVertices == 4)
          str.append(" PLANE");
        break;
      case LINE_SEGMENT:
        str.append(" LINE");
        break;
      case ARC:
        str.append(dmesh.isVector ? " ARROW ARC" : " ARC");
        break;
      case ARROW:
        str.append(dmesh.isVector ? " VECTOR" : " ARROW");
        if (dmesh.connectedAtoms != null)
          str.append(" connect ").append(Escape.eAI(dmesh.connectedAtoms));
        break;
      case CIRCLE:
        str.append(" CIRCLE");
        break;
      case CURVE:
        str.append(" CURVE");
        break;
      case CIRCULARPLANE:
      case CYLINDER:
        str.append(" CYLINDER");
        break;
      case POINT:
        nVertices = 1; // because this might be multiple points
        break;
      case LINE:
        nVertices = 2; // because this might be multiple lines
        break;
      }
      if (dmesh.modelIndex < 0 && !dmesh.isFixed) {
        for (int i = 0; i < modelCount; i++)
          if (isPolygonDisplayable(dmesh, i)) {
            if (nVertices == 0)
              nVertices = dmesh.drawVertexCounts[i];
            str.append(" [ " + i);
            String s = getVertexList(dmesh, i, nVertices);
            if (s.indexOf("NaN") >= 0)
              return "";
            str.append(s);
            str.append(" ] ");
          }
      } else if (dmesh.drawType == EnumDrawType.POLYGON) {
        for (int i = 0; i < dmesh.vc; i++)
          str.append(" ").append(Escape.eP(dmesh.vs[i]));
        str.append(" ").appendI(dmesh.pc);
        for (int i = 0; i < dmesh.pc; i++)
          if (dmesh.pis[i] == null)
            str.append(" [0 0 0 0]");
          else
            str.append(" ").append(Escape.eAI(dmesh.pis[i]));
      } else {
        String s = getVertexList(dmesh, iModel, nVertices);
        if (s.indexOf("NaN") >= 0)
          return "";
        str.append(s);
      }
    }
    if (dmesh.mat4 != null) {
      V3 v = new V3();
      dmesh.mat4.getTranslation(v);
      str.append(" offset ").append(Escape.eP(v));
    }
    if (dmesh.title != null) {
      if (dmesh.titleColor != null) {
        str.append(" title color ").append(Escape.escapeColor(dmesh.titleColor.intValue()));
      }
      String s = "";
      for (int i = 0; i < dmesh.title.length; i++)
        s += "|" + dmesh.title[i];
      str.append(" ").append(PT.esc(s.substring(1)));
    }
    if (dmesh.fontID >= 0) {
      str.append(" font " + Font.getFont3D(dmesh.fontID).getInfo());
    }
    str.append(";\n");
    appendCmd(str, dmesh.getState("draw"));
    appendCmd(str, getColorCommandUnk("draw", dmesh.colix, translucentAllowed));
    return str.toString();
  }

  public static boolean isPolygonDisplayable(Mesh mesh, int i) {
    return (i < mesh.pis.length 
        && mesh.pis[i] != null 
        && mesh.pis[i].length > 0);
  }
  
  private static String getVertexList(DrawMesh mesh, int iModel, int nVertices) {
    String str = "";
    try {
      if (iModel >= mesh.pis.length)
        iModel = 0; // arrows and curves may not have multiple model representations
      boolean adjustPt = (mesh.isVector && mesh.drawType != EnumDrawType.ARC);
      for (int i = 0; i < nVertices; i++) {
        T3 pt = mesh.vs[mesh.pis[iModel][i]];
        if (pt.z == Float.MAX_VALUE || pt.z == -Float.MAX_VALUE) {
          str += (i == 0 ? " " : " ,") + "[" + (int) pt.x + " " + (int) pt.y + (pt.z < 0 ? " %]" : "]");
        } else if (adjustPt && i == 1){
          P3 pt1 = P3.newP(pt);
          pt1.sub(mesh.vs[mesh.pis[iModel][0]]);
          str += " " + Escape.eP(pt1);
        } else {
          str += " " + Escape.eP(pt);
        }
      }
    } catch (Exception e) {
      Logger.error("Unexpected error in Draw.getVertexList");
    }
    return str;
  }
  
  @Override
  public Object getShapeDetail() {
    Lst<Map<String, Object>> V = new  Lst<Map<String,Object>>();
    for (int i = 0; i < meshCount; i++) {
      DrawMesh mesh = dmeshes[i];
      if (mesh.vc == 0)
        continue;
      Map<String, Object> info = new Hashtable<String, Object>();
      info.put("visible", mesh.visible ? Boolean.TRUE : Boolean.FALSE);
      info.put("fixed", mesh.ptCenters == null ? Boolean.TRUE : Boolean.FALSE);
      info.put("ID", (mesh.thisID == null ? "<noid>" : mesh.thisID));
      info.put("drawType", mesh.drawType.name);
      if (mesh.diameter > 0)
        info.put("diameter", Integer.valueOf(mesh.diameter));
      if (mesh.width != 0)
        info.put("width", Float.valueOf(mesh.width));
      info.put("scale", Float.valueOf(mesh.scale));
      if (mesh.drawType == EnumDrawType.MULTIPLE) {
        Lst<Map<String, Object>> m = new  Lst<Map<String,Object>>();
        int modelCount = vwr.ms.mc;
        for (int k = 0; k < modelCount; k++) {
          if (mesh.ptCenters[k] == null)
            continue;
          Map<String, Object> mInfo = new Hashtable<String, Object>();
          mInfo.put("modelIndex", Integer.valueOf(k));
          mInfo.put("command", getCommand2(mesh, k));
          mInfo.put("center", mesh.ptCenters[k]);
          int nPoints = mesh.drawVertexCounts[k];
          mInfo.put("vertexCount", Integer.valueOf(nPoints));
          if (nPoints > 1)
            mInfo.put("axis", mesh.axes[k]);
          Lst<T3> v = new  Lst<T3>();
          for (int ipt = 0; ipt < nPoints; ipt++)
            v.addLast(mesh.vs[mesh.pis[k][ipt]]);
          mInfo.put("vertices", v);
          if (mesh.drawTypes[k] == EnumDrawType.LINE) {
            float d = mesh.vs[mesh.pis[k][0]]
                .distance(mesh.vs[mesh.pis[k][1]]);
            mInfo.put("length_Ang", Float.valueOf(d));
          }
          m.addLast(mInfo);
        }
        info.put("models", m);
      } else {
        info.put("command", getCommand(mesh));
        info.put("center", mesh.ptCenter);
        if (mesh.drawVertexCount > 1)
          info.put("axis", mesh.axis);
        Lst<T3> v = new  Lst<T3>();
        for (int j = 0; j < mesh.vc; j++)
          v.addLast(mesh.vs[j]);
        info.put("vertices", v);
        if (mesh.drawType == EnumDrawType.LINE)
          info.put("length_Ang", Float.valueOf(mesh.vs[0]
              .distance(mesh.vs[1])));
      }
      V.addLast(info);
    }
    return V;
  }

  @Override
  public String getShapeState() {
    SB s = new SB();
    s.append("\n");
    appendCmd(s, myType + " delete");
    if (defaultFontId >= 0 && (
        defaultFontId != defaultFontId0 || defaultFontSize != JC.DRAW_DEFAULT_FONTSIZE))
      appendCmd(s, getFontCommand("draw", (Font) getProperty("font", -1)));
    for (int i = 0; i < meshCount; i++) {
      DrawMesh mesh = dmeshes[i];
      if (mesh.vc == 0 && mesh.lineData == null)
        continue;
      s.append(getCommand2(mesh, mesh.modelIndex));
      if (!mesh.visible)
        s.append(" " + myType + " ID " + PT.esc(mesh.thisID) + " off;\n");
    }
    return s.toString();
  }

  public static P3 randomPoint() {
    return P3.new3((float) Math.random(), (float) Math.random(), (float) Math.random());
  }

  public enum EnumDrawType {
    MULTIPLE(-1,"multiple"),
    NONE(0,"none"),
    
    POINT(1,"point"),
    LINE(2,"line"),
    PLANE(4,"plane"),
    
    CYLINDER(14,"cylinder"),
    ARROW(15,"arrow"),
    CIRCLE(16,"circle"),
    CURVE(17,"curve"),
    CIRCULARPLANE(18,"circularPlane"),
    ARC(19,"arc"),
    LINE_SEGMENT(20,"lineSegment"),
    POLYGON(21,"polygon");

    final int id;
    final String name;
    
    EnumDrawType(int id, String name) {
      this.id = id;
      this.name = name;
    }

    public static EnumDrawType getType(int nPoints) {
      switch (nPoints) {
      case 1:
        return POINT;
      case 2:
        return LINE;
      case 4:
        return PLANE;
      default:
        return NONE;
      }
    }
  }
}
