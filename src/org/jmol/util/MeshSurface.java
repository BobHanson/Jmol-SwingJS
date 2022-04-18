package org.jmol.util;

import org.jmol.api.Interface;
import javajs.util.BS;
import org.jmol.script.T;
import org.jmol.viewer.Viewer;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.SB;
import javajs.util.M4d;
import javajs.util.P3;
import javajs.util.V3;
import javajs.util.T3;

public class MeshSurface {
  
  protected static final int SEED_COUNT = 25;
  
  public static final int P_CHECK = 3;
  public static final int P_CONTOUR = 4;
  public static final int P_EXPLICIT_COLOR = 4;

  public Viewer vwr;
  
  private MeshSlicer slicer;
  
  public MeshSlicer getMeshSlicer() {
    return (slicer == null ? slicer = ((MeshSlicer) Interface.getInterface("org.jmol.util.MeshSlicer", vwr, "script")).set(this) : slicer);
  }


  public V3[] oabc;

  public String meshType;

  /**
   * vertex count
   */
  public int vc;
  /**
   * vertices
   */
  public T3[] vs;
  /**
   * vertex values
   */
  public float[] vvs;
  public int[] vertexSource;
  public BS surfaceAtoms;
  

  /**
   * polygon count
   */
  public int pc;
  /**
   * polygon indexes
   */
  public int[][] pis;
  //public float[] polygonTranslucencies;

  public boolean colorsExplicit;

  public boolean isDrawPolygon; // just a set of flat polygons
  public boolean haveQuads;
  public short colix;
  public short colixBack;

  public boolean isColorSolid = true;
  public P3 offset;
  public T3[] altVertices;

  /**
   * polygon color index "colix" array
   */
  public short[] pcs;
  /**
   * vertex color index "colix" array
   */
  public short[] vcs;
  public T3[] normals; // for export only or for cartoons
  public V3[] normalsTemp; // for cartoons
  public int normalCount; // for export only
  public int normixCount;
  public BS bsPolygons;
  public M4d mat4;
  public BS[] surfaceSet;
  public int[] vertexSets;
  public int nSets = 0;

  public boolean dataOnly;

  public MeshSurface() {
  }

  public static MeshSurface newMesh(boolean isAlt, T3[] vertices,
                                    int vertexCount, int[][] polygonIndexes,
                                    T3[] normals, int nNormals) {
    MeshSurface ms = new MeshSurface();
    ms.pis = polygonIndexes;
    if (isAlt)
      ms.altVertices = vertices;
    else
      ms.vs = vertices;
    ms.vc = (vertexCount == 0 ? vertices.length : vertexCount);
    ms.normals = normals;
    ms.normalCount = (nNormals == 0 && normals != null ? normals.length
        : nNormals);
    return ms;
  }

  /**
   * @return The vertices.
   */
  public T3[] getVertices() {
    return (altVertices == null ? vs : altVertices);
  }

  /**
   * @return faces, if defined (in exporter), otherwise polygonIndexes
   */
  public int[][] getFaces() {
    return pis;
  }

  public void setColix(short colix) {
    this.colix = colix;
  }

  public void setColixBack(short colix) {
    this.colixBack = colix;
  }

  public int addV(T3 vertex, boolean asCopy) { //used by mps and surfaceGenerator
    if (vc == 0)
      vs = new T3[SEED_COUNT];
    else if (vc == vs.length)
      vs = (T3[]) AU.doubleLength(vs);
    vs[vc] = (asCopy ? P3.newP(vertex) : vertex);
    return vc++;
  }

  public void addTriangle(int vertexA, int vertexB, int vertexC) {
    addPolygon(new int[] { vertexA, vertexB, vertexC }, null);
  }

  public void addQuad(int vertexA, int vertexB, int vertexC, int vertexD) {
    haveQuads = true;
    addPolygon(new int[] { vertexA, vertexB, vertexC, vertexD }, null);
  }

  public void setPolygonCount(int polygonCount) {
    this.pc = polygonCount;
    if (polygonCount < 0)
      return;
    if (pis == null || polygonCount > pis.length)
      pis = AU.newInt2(polygonCount);
  }

  public int addVCVal(T3 vertex, float value, boolean asCopy) {
    if (vc == 0)
      vvs = new float[SEED_COUNT];
    else if (vc >= vvs.length)
      vvs = AU.doubleLengthF(vvs);
    vvs[vc] = value;
    return addV(vertex, asCopy);
  }

  public int addTriangleCheck(int vertexA, int vertexB, int vertexC, int check,
                              int iContour, int color) {
    return (vs == null
        || vvs != null
        && (Float.isNaN(vvs[vertexA]) || Float.isNaN(vvs[vertexB]) || Float
            .isNaN(vvs[vertexC])) || Float.isNaN(vs[vertexA].x)
        || Float.isNaN(vs[vertexB].x) || Float.isNaN(vs[vertexC].x) ? -1
        : addPolygonV3(vertexA, vertexB, vertexC, check, iContour, color, null));
  }

  int addPolygonV3(int vertexA, int vertexB, int vertexC, int check,
                   int iContour, int color, BS bs) {
    return (dataOnly ? 
        addPolygon(new int[] { vertexA, vertexB, vertexC, check }, bs) : 
        addPolygonC(new int[] { vertexA, vertexB, vertexC, check, iContour }, color, bs, (iContour < 0)));
  }

  private int lastColor;
  private short lastColix;

  protected int addPolygonC(int[] polygon, int color, BS bs, boolean isExplicit) {
    if (color != 0) {
      if (pcs == null || pc == 0)
        lastColor = 0;
      if (isExplicit) {
        colorsExplicit = true;
      } else {
        if (pcs == null) {
          pcs = new short[SEED_COUNT];
        } else if (pc >= pcs.length) {
          pcs = AU.doubleLengthShort(pcs);
        }
        pcs[pc] = (isExplicit ? C.LAST_AVAILABLE_COLIX
            : color == lastColor ? lastColix : (lastColix = C
            .getColix(lastColor = color)));
      }
    }
    return addPolygon(polygon, bs);
  }

  public int addPolygon(int[] polygon, BS bs) {
    int n = pc;
    if (n == 0)
      pis = AU.newInt2(SEED_COUNT);
    else if (n == pis.length)
      pis = (int[][]) AU.doubleLength(pis);
    if (bs != null)
      bs.set(n);
    pis[pc++] = polygon;
    return n;
  }

  public void invalidatePolygons() {
    for (int i = pc; --i >= mergePolygonCount0;)
      if ((bsSlabDisplay == null || bsSlabDisplay.get(i)) && setABC(i) == null)
        pis[i] = null;
  }

  protected int iA, iB, iC;

  protected int[] setABC(int i) {
    if (bsSlabDisplay != null && !bsSlabDisplay.get(i)
        && (bsSlabGhost == null || !bsSlabGhost.get(i)))
      return null;
    int[] polygon = pis[i];
    if (polygon == null || polygon.length < 3)
      return null;
    iA = polygon[0];
    iB = polygon[1];
    iC = polygon[2];
    return (vvs == null || !Float.isNaN(vvs[iA]) && !Float.isNaN(vvs[iB])
        && !Float.isNaN(vvs[iC]) ? polygon : null);
  }

  public int polygonCount0;
  public int vertexCount0;

  public BS bsSlabDisplay;
  public BS bsSlabGhost;
  //public BS bsTransPolygons;
  public int slabMeshType;
  public short slabColix;

  /**
   * Must create bsTransPolygons, polygonTranslucencies, and new triangle set
   * for partially translucent polygons
   * 
   * @param bsVertices
   */
  public void setTranslucentVertices(BS bsVertices) {
    //TODO 

  }

//  public void setSlab(BS bsDisplay, BS bsGhost, String type, String color,
//                      float translucency) {
//    bsSlabDisplay = bsDisplay;
//    bsSlabGhost = bsGhost;
//    slabMeshType = (type.equalsIgnoreCase("mesh") ? T.mesh : T.fill);
//    slabColix = C.getColixTranslucent3(C.getColixS(color), true, translucency);
//  }

  public BS bsDisplay;

  public String getSlabColor() {
    return (bsSlabGhost == null ? null : C.getHexCode(slabColix));
  }

  public String getSlabType() {
    return (bsSlabGhost != null && slabMeshType == T.mesh ? "mesh" : null);
  }

  public SB slabOptions;

//  public void resetTransPolygons() {
//    boolean isTranslucent = C.isColixTranslucent(colix);
//    float translucentLevel = C.getColixTranslucencyFractional(colix);
//    for (int i = 0; i < pc; i++)
//      if (bsTransPolygons.get(i)) {
//        if (!setABC(i))
//          continue;
//        vcs[iA] = C.getColixTranslucent3(vcs[iA], isTranslucent,
//            translucentLevel);
//        vcs[iB] = C.getColixTranslucent3(vcs[iB], isTranslucent,
//            translucentLevel);
//        vcs[iC] = C.getColixTranslucent3(vcs[iC], isTranslucent,
//            translucentLevel);
//      }
//    bsTransPolygons = null;
//    polygonTranslucencies = null;
//  }

  public void resetSlab() {
    if (slicer != null)
      slicer.slabPolygons(TempArray.getSlabObjectType(T.none, null, false, null), false);
  }

  public void slabPolygonsList(Lst<Object[]> slabInfo, boolean allowCap) {
    getMeshSlicer();
    for (int i = 0; i < slabInfo.size(); i++)
      if (!slicer.slabPolygons(slabInfo.get(i), allowCap))
        break;
  }

  /**
   * @param unitCellVectors
   */
  protected void slabBrillouin(P3[] unitCellVectors) {
    // isosurfaceMesh only
    return;
  }

  public int mergeVertexCount0;
  public int mergePolygonCount0;
  public boolean isMerged;

  public float getResolution() {
    return 0; // overridden in IsosurfaceMesh
  }
  /**
   * Calculates the data (faces, vertices, normals) for a sphere.
   * 
   * @param lvl
   * 
   * @return The data.
   */
  public static MeshSurface getSphereData(int lvl) {
    // _ObjExporter only
    Geodesic.createGeodesic(lvl);
    int vertexCount = Geodesic.getVertexCount(lvl);
    short[] f = Geodesic.getFaceVertexes(lvl);
    int nFaces = f.length / 3;
    int[][] faces = AU.newInt2(nFaces);
    for (int i = 0, fpt = 0; i < nFaces; i++) {
      faces[i] = new int[] { f[fpt++], f[fpt++], f[fpt++] };
    }
    V3[] vectors = new V3[vertexCount];
    for (int i = 0; i < vertexCount; i++)
      vectors[i] = Geodesic.getVertexVector(i);
    return newMesh(true, vectors, 0, faces, vectors, 0);
  }

  public void setBox(P3 xyzMin, P3 xyzMax) {
    xyzMin.set(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
    xyzMax.set(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
    for (int i = 0; i < vc; i++) {
      T3 p = vs[i];
      if (!Float.isNaN(p.x))
        BoxInfo.addPoint(p, xyzMin, xyzMax, 0);
    }
  }

  /**
   * @param boundBoxPoints  
   */
  public void setBoundingBox(P3[] boundBoxPoints) {
    // isosurfaceMesh only
  }

  public static float getSphericalInterpolationFraction(double r,
                                                        double valueA,
                                                        double valueB, double d) {
    double ra = Math.abs(r + valueA) / d;
    double rb = Math.abs(r + valueB) / d;
    r /= d;
    double ra2 = ra * ra;
    double q = ra2 - rb * rb + 1;
    double p = 4 * (r * r - ra2);
    double factor = (ra < rb ? 1 : -1);
    return (float) (((q) + factor * Math.sqrt(q * q + p)) / 2);
  }

}
