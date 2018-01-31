package org.jmol.jvxl.api;



import javajs.util.P3i;
import javajs.util.T3;
import javajs.util.V3;

public interface VertexDataServer {
  
  /*
   * An interface for interacting with 
   * the MarchingCubes and MarchingSquares classes 
   * as well as the SurfaceReader classes
   * during and after surface generation
   * 
   * SurfaceReader and Isosurface are examples.
   * 
   * SurfaceReader accepts vertexes from MarchingCubes/MarchingSquares
   * and then either consumes them or passes them on to Isosurface.
   * 
   * In addition, MeshData information is passed back and forth
   * via this mechanism.
   * 
   * This is crude. I would like to do it better.
   * 
   * Bob Hanson 20 Apr 2007
   * 
   */
  
  
  /**
   * getSurfacePointIndex is used by the Marching Cubes algorithm and 
   * must return a unique integer identifier for
   * a vertex created by the Marching Cube algorithm when it finds an 
   * edge. If a vertex is discarded, then Integer.MAX_VALUE should be returned.
   * 
   * the 3D coordinate of the point can be calculated using 
   * 
   * surfacePoint.scaleAdd(fraction, edgeVector, pointA);
   * 
   * where fraction is generally calculated as:
   * 
   *  fraction = (cutoff - valueA) / (valueB - valueA);
   *  if (isCutoffAbsolute && (fraction < 0 || fraction > 1))
   *  fraction = (-cutoff - valueA) / (valueB - valueA);
   *  
   *  This method is also used by MarchingCubes to deliver the appropriate
   *  oblique planar coordinate to MarchingSquares for later contouring.
   * 
   * @param cutoff
   * @param isCutoffAbsolute
   * @param x
   * @param y
   * @param z
   * @param offset
   * @param vertexA [0:7]
   * @param vertexB [0:7]
   * @param valueA
   * @param valueB
   * @param pointA
   * @param edgeVector      vector from A to B
   * @param isContourType
   * @param fReturn 
   * @return                 new vertex index or Integer.MAX_VALUE
   */
  public abstract int getSurfacePointIndexAndFraction(float cutoff,
                                           boolean isCutoffAbsolute, int x,
                                           int y, int z, P3i offset,
                                           int vertexA, int vertexB, 
                                           float valueA, float valueB,
                                           T3 pointA, V3 edgeVector,
                                           boolean isContourType, float[] fReturn);

  /**
   * addVertexCopy is used by the Marching Squares algorithm to
   * uniquely identify a new vertex when an edge is crossed in the 2D plane.
   * The implementing method should COPY the Point3f using Point3f.set(). 
   * 
   * The data consumer can use the association key to group this vertex with others
   * near the same gridpoint.
   *  
   * @param vertexXYZ
   * @param value
   * @param assocVertex       unique association vertex or -1
   * @param asCopy 
   * @return                  new vertex index
   * 
   */
  public abstract int addVertexCopy(T3 vertexXYZ, float value, int assocVertex, boolean asCopy);

  /**
   * addTriangleCheck adds a triangle along with a 3-bit check indicating
   * which edges to draw in mesh mode: 1 (iA-iB) + 2 (iB-iC) + 4 (iC-iA) 
   * 
   * @param iA
   * @param iB
   * @param iC
   * @param check
   * @param iContour TODO
   * @param isAbsolute
   * @param color 
   * @return polygon index or -1
   */
  public abstract int addTriangleCheck(int iA, int iB, int iC, int check,
                                        int iContour, boolean isAbsolute, int color);
  
  /**
   * 
   * for readers only
   * 
   * @param x
   * @param y
   * @param z
   * @param ptyz 
   * @return  value[x][y][z]
   */
  public abstract float getValue(int x, int y, int z, int ptyz);

  public abstract float[] getPlane(int x);

}
