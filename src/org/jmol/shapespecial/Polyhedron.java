package org.jmol.shapespecial;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.Interface;
import org.jmol.api.SmilesMatcherInterface;
import org.jmol.api.SymmetryInterface;
import org.jmol.modelset.Atom;
import org.jmol.script.SV;
import org.jmol.script.T;
import org.jmol.util.C;
import org.jmol.util.Elements;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.MeshCapper;
import org.jmol.util.Normix;
import org.jmol.util.Point3fi;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.util.AU;
import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.M4d;
import javajs.util.MeasureD;
import javajs.util.P3d;
import javajs.util.P4d;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.T3d;
import javajs.util.V3d;

public class Polyhedron {

  Map<String, Object> info;

  public String id;
  public P3d center;

  public Atom centralAtom;
  public P3d[] vertices;
  public int[][] triangles;
  public int[][] faces;
  int nVertices;
  public boolean collapsed;
  private BS bsFlat;
  private double distanceRef;
  private V3d[] normals;
  private short[] normixes;
  
  P4d[] planes; // used only for distance calculation
  
  

  public String smiles, smarts, polySmiles;
  /**
   * includes vertices as atoms, with atomic numbers
   */
  private SymmetryInterface pointGroup;
  /**
   * includes vertices as generic points
   */
  private SymmetryInterface pointGroupFamily;
  private Double volume;

  boolean visible = true;
  boolean isFullyLit;
  public boolean isValid = true;
  public short colixEdge = C.INHERIT_ALL;
  public int visibilityFlags = 0;

  public short colix = C.GOLD;
  public int modelIndex = Integer.MIN_VALUE;

  private P3d offset;

  public double scale = 1;

  public double pointScale;

  private int[][] faceTriangles;

  Polyhedron() {
  }

  Polyhedron set(String id, int modelIndex, P3d atomOrPt, P3d[] points,
                 int nPoints, int vertexCount, int[][] triangles,
                 int triangleCount, int[][] faces, int[][] faceTriangles, V3d[] normals, BS bsFlat,
                 boolean collapsed, double distanceRef, double pointScale) {
    this.pointScale = pointScale;
    this.distanceRef = distanceRef;
    if (id == null) {
      centralAtom = (Atom) atomOrPt;
      this.modelIndex = centralAtom.mi;
    } else {
      this.id = id;
      center = atomOrPt;
      this.modelIndex = modelIndex;
    }
    this.nVertices = vertexCount;
    this.vertices = new P3d[nPoints + 1];
    this.normals = new V3d[triangleCount];
    this.faces = faces;
    this.faceTriangles = faceTriangles;
    this.bsFlat = bsFlat;
    this.triangles = AU.newInt2(triangleCount);
    for (int i = nPoints + 1; --i >= 0;)
      // includes central atom as last atom or possibly reference point
      vertices[i] = points[i];
    for (int i = triangleCount; --i >= 0;)
      this.normals[i] = V3d.newV(normals[i]);
    for (int i = triangleCount; --i >= 0;)
      this.triangles[i] = triangles[i];
    this.collapsed = collapsed;
    return this;
  }

  Polyhedron setInfo(Viewer vwr, Map<String, Object> info, Atom[] at) {
    try {
      Object o = info.get("id");
      collapsed = info.containsKey("collapsed");
      boolean isSV = (!(o instanceof String));
      if (o != null)
        id = (isSV ? ((SV) info.get("id")).asString() : o.toString());
      if (id == null) {
        centralAtom = at[((SV)info.get("atomIndex")).intValue];
        modelIndex = centralAtom.mi;
      } else {
        o = info.get("center");
        center = P3d.newP(isSV ? SV.ptValue((SV)o) : (P3d) o);
        o = info.get("modelIndex");
        modelIndex = (o == null ? vwr.am.cmi : isSV ? ((SV)o).intValue : ((Integer) o).intValue());
        o = info.get("color");
        colix = C.getColixS(o == null ? "gold" : isSV ? ((SV)o).asString() : (String) o);
        o = info.get("colorEdge");
        if (o != null)
        colixEdge = C.getColixS(isSV ? ((SV)o).asString() : o.toString());
        o = info.get("offset");
        if (o != null)
          offset = P3d.newP(isSV ? SV.ptValue((SV) o) : (P3d) o);
        o = info.get("scale");
        if (o != null)
          scale = (isSV ? SV.dValue((SV) o) : ((Double) o).doubleValue());
      }
      o = info.get("vertices");
      Lst<?> lst = (isSV ? ((SV) o).getList() : (Lst<?>) o);
      o = info.get("vertexCount");
      boolean needTriangles = false;
      if (o != null) {
        nVertices = (isSV ? ((SV) o).intValue : ((Integer) o).intValue());
        vertices = new P3d[lst.size()];
        o = info.get("r");
        if (o != null)
          distanceRef = (isSV ? ((SV) o).asDouble() : ((Double) o).doubleValue());
      } else {
        nVertices = lst.size();
        vertices = new P3d[nVertices + 1];
        if (center == null) {
          // old style
          vertices[nVertices] = SV.ptValue((SV)info.get("ptRef"));
        } else {
          // new format involving center, vertices, and faces only
          vertices[nVertices] = center;
          needTriangles = true;
        }
      }
      // note that nVertices will be smaller than lst.size()
      // because lst will contain the central atom and any collapsed points
      for (int i = lst.size(); --i >= 0;) {
        o = lst.get(i);
        vertices[i] = (isSV ? SV.ptValue((SV) o) : (P3d) o);
      }
      o = info.get("explodeOffset");
      if (o != null)
        setExplodeOffset(((Number) o).doubleValue());
      o = info.get("elemNos");
      if (o != null) {
        lst = (isSV ? ((SV)o).getList() : (Lst<?>) o);
        for (int i = nVertices; --i >= 0;) {
          o = lst.get(i);
          int n = (isSV ? ((SV)o).intValue : ((Integer) o).intValue());
          if (n > 0) {
            Point3fi p = new Point3fi();
            p.setT(vertices[i]);
            p.sD = (short) n;
            vertices[i] = p;
          }
        }
      }
      o = info.get("pointScale");
      if (o != null)
        pointScale = Math.max(0, (isSV ? SV.dValue((SV) o) : ((Double) o).doubleValue()));
      this.faces = toInt2(isSV, info.get("faces"));
      o = info.get("triangles");
      if (o == null) {
        if (needTriangles) {
          // polyhedra {id:"...", center:"...", vertices:"...", faces:"..."}
          // need to derive triangles from faces
          faceTriangles = AU.newInt2(this.faces.length);
          triangles = ((MeshCapper) Interface.getInterface(
              "org.jmol.util.MeshCapper", vwr, "script")).set(null).triangulateFaces(this.faces, vertices, faceTriangles);
        } else {
          // formerly
          triangles = this.faces;
          this.faces = null;
        }
      } else {
        triangles = toInt2(isSV, o);        
      }
      normals = new V3d[triangles.length];
      V3d vAB = new V3d();
      for (int i = triangles.length; --i >= 0;) {
        normals[i] = new V3d();
        int[] a = triangles[i];
        MeasureD.getNormalThroughPoints(vertices[a[0]], vertices[a[1]],
            vertices[a[2]], normals[i], vAB);
      }
      o = info.get("bsFlat");
      bsFlat = (o == null ? new BS() : isSV ? SV.getBitSet((SV)o, false) : (BS) o);
      if (info.containsKey("volume"))
        info.put("volume", getVolume());
    } catch (Exception e) {
      return null;
    }
    return this;
  }

  private int[][] toInt2(boolean isSV, Object o) {
    Lst<?> lst = (isSV ? ((SV) o).getList() : (Lst<?>) o);
    int[][] ai = AU.newInt2(lst.size());
    for (int i = ai.length; --i >= 0;) {
      o = lst.get(i);
      if (isSV) {
        Lst<SV> lst2 = ((SV)o).getList();
        int[] a = ai[i] = new int[lst2.size()];
        for (int j = a.length; --j >= 0;)
          a[j] = lst2.get(j).intValue;
      } else {
        ai[i] = (int[]) o;
      }
    }
    return ai;
  }

  Map<String, Object> getInfo(Viewer vwr, String property) {
    boolean isState = (property == null);
    boolean isFaceCalc = (!isState);// && property.startsWith("face_"));
    Map<String, Object> info = this.info;
    if (!isState && info != null
        && (!isFaceCalc || info.containsKey("face_types")) && !Logger.debugging)
      return info;
    info = new Hashtable<String, Object>();

    info.put("vertexCount", Integer.valueOf(nVertices));

    // get COPY of vertices to prevent script variable from referencing Atom
    int nv = (isState ? vertices.length : nVertices);
    P3d[] pts = new P3d[nv];
    for (int i = 0; i < nv; i++)
      pts[i] = P3d.newP(vertices[i]);
    info.put("vertices", pts);
    info.put("elemNos", getElemNos(true));

    if (id == null) {
      info.put("atomIndex", Integer.valueOf(centralAtom.i));
    } else {
      info.put("id", id);
      info.put("center", P3d.newP(center));
      info.put("color", C.getHexCode(colix));
      info.put("colorEdge", C.getHexCode(colixEdge == C.INHERIT_ALL ? colix : colixEdge));
      if (offset != null)
        info.put("offset", offset);
      if (scale != 1)
        info.put("scale", Double.valueOf(scale));
    }
    if (id != null || !isState)
      info.put("modelIndex", Integer.valueOf(modelIndex));
    if (!isState) {
      this.info = info;
      if (id == null) {
        info.put("center", P3d.newP(centralAtom));
        info.put("modelNumber", Integer.valueOf(centralAtom.getModelNumber()));
        info.put("atomNumber", Integer.valueOf(centralAtom.getAtomNumber()));
        info.put("atomName", centralAtom.getInfo());
        info.put("element", centralAtom.getElementSymbol());
        Object energy = vwr.ms.getInfo(centralAtom.mi, "Energy");
        if (energy != null)
          info.put("energy", energy);
      }
      info.put("triangleCount", Integer.valueOf(triangles.length));
      info.put("volume", getVolume());

      String[] names = new String[nVertices];
      int[] indices = new int[nVertices];
      for (int i = nVertices; --i >= 0;) {
        P3d pt = vertices[i];
        boolean isAtom = pt instanceof Atom;
        names[i] = (isAtom ? ((Atom) pt).getAtomName()
            : pt instanceof Point3fi ? Elements
                .elementSymbolFromNumber(((Point3fi) pt).sD) : "");
        indices[i] = (isAtom ? ((Atom) pt).getIndex() : -1);
      }
      info.put("atomNames", names);
      info.put("vertexIndices", indices);

      if (faces != null && !collapsed && faceTriangles != null) {
        info.put("faceCount", Integer.valueOf(faces.length));
        info.put("faceTriangles", faceTriangles);
        if (isFaceCalc) {
          int[] faceTypes = new int[faces.length];
          double[] faceAreas = new double[faces.length];
          Lst<P3d[]> facePoints = new Lst<P3d[]>();
          V3d vAB = new V3d();
          V3d vAC = new V3d();
          V3d vTemp = new V3d();
          for (int i = faces.length; --i >= 0;) {
            int[] face = faces[i];
            faceTypes[i] = face.length;
            double f = 0;
            int[] ft = faceTriangles[i];
            for (int j = ft.length; --j >= 0;) {
              int[] t = triangles[ft[j]];
              f += triangleArea(t[0], t[1], t[2], vAB, vAC, vTemp);
            }
            faceAreas[i] = f;
            P3d[] fpts = new P3d[face.length];
            for (int j = face.length; --j >= 0;)
              fpts[j] = vertices[face[j]];
            facePoints.addLast(fpts);
          }
          info.put("face_types", faceTypes);
          info.put("face_areas", faceAreas);
          info.put("face_points", facePoints);
        }
      }

      if (smarts != null)
        info.put("smarts", smarts);
      if (smiles != null)
        info.put("smiles", smiles);
      if (polySmiles != null)
        info.put("polySmiles", polySmiles);

      if (pointGroup != null)
        info.put("pointGroup", pointGroup.getPointGroupName());
      if (pointGroupFamily != null)
        info.put("pointGroupFamily", pointGroupFamily.getPointGroupName());
    }
    if (pointScale > 0)
      info.put("pointScale", Double.valueOf(pointScale));
    if (faces != null)
      info.put("faces", faces);
    if (isState || Logger.debugging) {
      info.put("bsFlat", bsFlat);
      if (collapsed)
        info.put("collapsed", Boolean.valueOf(collapsed));
      if (distanceRef != 0)
        info.put("r", Double.valueOf(distanceRef));
      P3d[] n = new P3d[normals.length];
      for (int i = n.length; --i >= 0;)
        n[i] = P3d.newP(normals[i]);
      if (!isState)
        info.put("normals", n);
      info.put("triangles", AU.arrayCopyII(triangles, triangles.length));
    }
    return info;
  }

  private int[] elemNos;
  
  void setPointScale(double scale) {
    pointScale = scale;
    elemNos = null;
  }

  public int[] getElemNos(boolean forInfo) {
    if (forInfo) {
      int[] a = new int[nVertices];
      for (int i = 0; i < nVertices; i++) {
        P3d pt = vertices[i];
        a[i] = (pt instanceof Atom ? ((Atom) pt).getElementNumber()
            : pt instanceof Point3fi ? ((Point3fi) pt).sD : -2);
      }
      return a;
    }
    if (elemNos == null || elemNos.length < nVertices)
      elemNos = new int[nVertices];
    for (int i = 0; i < nVertices; i++) {
      P3d pt = vertices[i];
      elemNos[i] = (pt instanceof Atom ? ((Atom) pt).getAtomicAndIsotopeNumber()
          : pt instanceof Point3fi ? ((Point3fi) pt).sD : -2);
    }
    return elemNos;
  }

  String getSymmetry(Viewer vwr, boolean withPointGroup) {
    if (id == null) {
      if (smarts == null) {
        info = null;
        SmilesMatcherInterface sm = vwr.getSmilesMatcher();
        try {
          String details = (distanceRef <= 0 ? null : "r=" + distanceRef);
          polySmiles = sm.polyhedronToSmiles(centralAtom, faces, nVertices,
              vertices, JC.SMILES_TYPE_SMILES | JC.SMILES_GEN_POLYHEDRAL
                  | (Logger.debugging ? JC.SMILES_GEN_ATOM_COMMENT : 0), details);
          smarts = sm.polyhedronToSmiles(centralAtom, faces, nVertices, null,
              JC.SMILES_GEN_TOPOLOGY, null);
          smiles = sm.polyhedronToSmiles(centralAtom, faces, nVertices,
              vertices, JC.SMILES_TYPE_SMILES, null);
        } catch (Exception e) {
        }
      }
    }
    if (!withPointGroup)
      return null;
    if (pointGroup == null) {
      T3d[] pts = new T3d[nVertices];
      // first time through includes all atoms as atoms
      for (int i = pts.length; --i >= 0;)
        pts[i] = vertices[i];
      pointGroup = vwr.getSymTemp().setPointGroup(null, null, pts,
          null, false,
          vwr.getDouble(T.pointgroupdistancetolerance), vwr.getDouble(T.pointgrouplineartolerance), pts.length, true);
      // second time through includes all atoms as points only
      for (int i = pts.length; --i >= 0;)
        pts[i] = P3d.newP(vertices[i]);
      pointGroupFamily = vwr.getSymTemp().setPointGroup(null, null,
          pts, null, false,
          vwr.getDouble(T.pointgroupdistancetolerance), vwr.getDouble(T.pointgrouplineartolerance), pts.length, true);
    }
    return (center == null ? centralAtom : center) + "    \t"
        + pointGroup.getPointGroupName() + "\t"
        + pointGroupFamily.getPointGroupName();
  }

  /**
   * allows for n-gon, not just triangle; if last component index is negative,
   * then that's a mesh code
   * 
   * @return volume
   */
  private Double getVolume() {
    // this will give spurious results for overlapping faces triangles
    if (volume != null)
      return volume;
    V3d vAB = new V3d();
    V3d vAC = new V3d();
    V3d vTemp = new V3d();
    double v = 0;
    if (bsFlat.cardinality() < triangles.length)
      for (int i = triangles.length; --i >= 0;) {
        int[] t = triangles[i];
        v += triangleVolume(t[0], t[1], t[2], vAB, vAC, vTemp);
      }
    return Double.valueOf(v / 6);
  }

  private double triangleArea(int i, int j, int k, V3d vAB, V3d vAC, V3d vTemp) {
    // area
    vAB.sub2(vertices[j], vertices[i]);
    vAC.sub2(vertices[k], vertices[i]);
    vTemp.cross(vAB, vAC);
    return vTemp.length();
  }

  private double triangleVolume(int i, int j, int k, V3d vAB, V3d vAC, V3d vTemp) {
    // volume
    vAB.setT(vertices[i]);
    vAC.setT(vertices[j]);
    vTemp.cross(vAB, vAC);
    vAC.setT(vertices[k]);
    return vAC.dot(vTemp);
  }

  String getState(Viewer vwr) {
    String ident = (id == null ? "({" + centralAtom.i + "})" : "ID "
        + Escape.e(id));
    return "  polyhedron" + " @{" + Escape.e(getInfo(vwr, null)) + "} "
        + (isFullyLit ? " fullyLit" : "") + ";"
        + (visible ? "" : "polyhedra " + ident + " off;") + "\n";
  }

  void move(M4d mat, BS bsMoved) {
    info = null;
    for (int i = 0; i < nVertices; i++) {
      P3d p = vertices[i];
      if (p instanceof Atom) {
        if (bsMoved.get(((Atom) p).i))
          continue;
        p = vertices[i] = P3d.newP(p);
      }
      mat.rotTrans(p);
    }
    for (int i = normals.length; --i >= 0;)
      mat.rotate(normals[i]);
    normixes = null;
  }

  public short[] getNormixes() {
    if (normixes == null) {
      normixes = new short[normals.length];
      BS bsTemp = new BS();
      for (int i = normals.length; --i >= 0;)
        normixes[i] = (bsFlat.get(i) ? Normix.get2SidedNormix(normals[i],
            bsTemp) : Normix.getNormixV(normals[i], bsTemp));
    }
    return normixes;
  }

  void setOffset(P3d value) {
    planes = null; // clear all planes
    if (center == null)
      return; // ID  polyhedra only
    P3d v = P3d.newP(value);
    if (offset != null)
      value.sub(offset);
    offset = v;
    for (int i = vertices.length; --i >= 0;)
      vertices[i].add(value);
  }

  P3d[] v0;
  
  /**
   * Specifically for Brillouin zones. From absolute {0 0 0} outward.
   * Only used during construction.
   * 
   * @param value
   */
  public void setExplodeOffset(double value) {
    if (id == null || center == null || value == 0)
      return;
    double d = center.length();
    if (d < 0.0001d)
      return;
    P3d v = P3d.newP(center);
    v.scale(value / d);
    setOffset(v);
  }


  public void list(SB sb) {
    if (id == null) {
      sb.append(" atomIndex:" + centralAtom.i);
      sb.append(" atomName:" + centralAtom.getAtomName());
      sb.append(" element:" + centralAtom.getElementSymbol());
    } else {
      sb.append(" id:" + id);
      sb.append("; center:" + PT.sprintf("{%6.3p %6.3p %6.3p}", "p", new Object[] { center }))
        .append("; visible:" + visible);
    }
    sb.append("; model:" + modelIndex)
    .append("; vertices:" + nVertices)
    .append("; faces:" + faces.length)
    .append("; volume:" + PT.formatD(getVolume().doubleValue(), 1, 3, false, false));
    if (scale != 1)
        sb.append("; scale:" + scale);
    sb.append("\n");
  }

}
