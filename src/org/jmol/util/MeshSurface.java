package org.jmol.util;

import org.jmol.api.Interface;
import javajs.util.BS;
import org.jmol.script.T;
import org.jmol.viewer.Viewer;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.SB;
import javajs.util.M4d;
import javajs.util.P3d;
import javajs.util.V3d;
import javajs.util.T3d;

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


  public V3d[] oabc;

  public String meshType;

  /**
   * vertex count
   */
  public int vc;
  /**
   * vertices
   */
  public T3d[] vs;
  /**
   * vertex values
   */
  public double[] vvs;
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
  //public double[] polygonTranslucencies;

  public boolean colorsExplicit;

  public boolean isDrawPolygon; // just a set of flat polygons
  public boolean haveQuads;
  public short colix;
  public short colixBack;

  public boolean isColorSolid = true;
  public P3d offset;
  public T3d[] altVertices;

  /**
   * polygon color index "colix" array
   */
  public short[] pcs;
  /**
   * vertex color index "colix" array
   */
  public short[] vcs;
  public T3d[] normals; // for export only or for cartoons
  public V3d[] normalsTemp; // for cartoons
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

  public static MeshSurface newMesh(boolean isAlt, T3d[] vertices,
                                    int vertexCount, int[][] polygonIndexes,
                                    T3d[] normals, int nNormals) {
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
  public T3d[] getVertices() {
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

  public int addV(T3d vertex, boolean asCopy) { //used by mps and surfaceGenerator
    if (vc == 0)
      vs = new T3d[SEED_COUNT];
    else if (vc == vs.length)
      vs = (T3d[]) AU.doubleLength(vs);
    vs[vc] = (asCopy ? P3d.newP(vertex) : vertex);
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

  public int addVCVal(T3d vertex, double value, boolean asCopy) {
    if (vc == 0)
      vvs = new double[SEED_COUNT];
    else if (vc >= vvs.length)
      vvs = AU.doubleLengthD(vvs);
    vvs[vc] = value;
    return addV(vertex, asCopy);
  }

  public int addTriangleCheck(int vertexA, int vertexB, int vertexC, int check,
                              int iContour, int color) {
    return (vs == null
        || vvs != null
        && (Double.isNaN(vvs[vertexA]) || Double.isNaN(vvs[vertexB]) || Double.isNaN(vvs[vertexC])) || Double.isNaN(vs[vertexA].x)
        || Double.isNaN(vs[vertexB].x) || Double.isNaN(vs[vertexC].x) ? -1
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
    return (vvs == null || !Double.isNaN(vvs[iA]) && !Double.isNaN(vvs[iB])
        && !Double.isNaN(vvs[iC]) ? polygon : null);
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
//                      double translucency) {
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
//    double translucentLevel = C.getColixTranslucencyFractional(colix);
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
      slicer.slabPolygons(MeshSurface.getSlabObjectType(T.none, null, false, null), false);
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
  protected void slabBrillouin(P3d[] unitCellVectors) {
    // isosurfaceMesh only
    return;
  }

  public int mergeVertexCount0;
  public int mergePolygonCount0;
  public boolean isMerged;

  public double getResolution() {
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
    V3d[] vectors = new V3d[vertexCount];
    for (int i = 0; i < vertexCount; i++)
      vectors[i] = Geodesic.getVertexVector(i);
    return newMesh(true, vectors, 0, faces, vectors, 0);
  }

  public void setBox(P3d xyzMin, P3d xyzMax) {
    xyzMin.set(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
    xyzMax.set(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);
    for (int i = 0; i < vc; i++) {
      T3d p = vs[i];
      if (!Double.isNaN(p.x))
        BoxInfo.addPoint(p, xyzMin, xyzMax, 0);
    }
  }

  /**
   * @param boundBoxPoints  
   */
  public void setBoundingBox(P3d[] boundBoxPoints) {
    // isosurfaceMesh only
  }

  // in Legacy Jmol, these two methods are in org.jmol.util.TempArray
  
  public static Object[] getSlabObjectType(int tok, Object data, boolean isCap, Object colorData) {
    return new Object[] { Integer.valueOf(tok), data, Boolean.valueOf(isCap), colorData };
  }

  public static Object[] getSlabWithinRange(double min, double max) {
    return new Object[] { Integer.valueOf(T.range), 
        new Double[] {Double.valueOf(min), Double.valueOf(max)}, Boolean.FALSE, null };
  }

  public static double getSphericalInterpolationFraction(double r,
                                                        double valueA,
                                                        double valueB, double d) {
    double ra = Math.abs(r + valueA) / d;
    double rb = Math.abs(r + valueB) / d;
    r /= d;
    double ra2 = ra * ra;
    double q = ra2 - rb * rb + 1;
    double p = 4 * (r * r - ra2);
    double factor = (ra < rb ? 1 : -1);
    return (((q) + factor * Math.sqrt(q * q + p)) / 2);
  }

}
