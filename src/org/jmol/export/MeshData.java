package org.jmol.export;


import org.jmol.util.MeshSurface;

import javajs.util.AU;
import javajs.util.P3;
import javajs.util.T3;
import javajs.util.V3;

/**
 * Class to generate mesh data (faces, vertices, and normals) for several kinds
 * of generic meshes.  This allows the same routines to be used in different
 * exporters and possibly in other places, as well.<br><br>
 * The meshes implemented are circle, cone, cylinder, and sphere.
 */
class MeshData {

//  /**
//   * This internal class is a container for the return values of the getXxxData
//   * methods.
//   */
//  static class Data {
//    private int[][] faces;
//    private T3[] normals;
//    private int nVertices;
//    private int nNormals;
//
//    /**
//     * Constructor.
//     * 
//     * @param faces
//     * @param vertexes
//     * @param nVertices TODO
//     * @param normals
//     * @param nNormals TODO
//     */
//
//    /**
//     * @return The faces.
//     */
//    int[][] getFaces() {
//      return faces;
//    }
//
//    /**
//     * @return vertex count
//     */
//    int getVertexCount() {
//      return nVertices;
//    }
//    
//    /**
//     * @return vertex count
//     */
//    int getNormalCount() {
//      return nNormals;
//    }
//    
//
//    /**
//     * @return The normals.
//     */
//    T3[] getNormals() {
//      return normals;
//    }
//  }

  /**
   * Calculates the data (faces, vertices, normals) for a circle.
   * 
   * @return The data.
   */
  static MeshSurface getCircleData() {
    int ndeg = 10;
    int n = 360 / ndeg;
    int vertexCount = n + 1;
    int[][] faces = AU.newInt2(n);
    for (int i = 0; i < n; i++) {
      faces[i] = new int[] { i, (i + 1) % n, n };
    }
    P3[] vertexes = new P3[vertexCount];
    P3[] normals = new P3[vertexCount];
    for (int i = 0; i < n; i++) {
      float x = (float) (Math.cos(i * ndeg / 180. * Math.PI));
      float y = (float) (Math.sin(i * ndeg / 180. * Math.PI));
      vertexes[i] = P3.new3(x, y, 0);
      normals[i] = P3.new3(0, 0, 1);
    }
    vertexes[n] = P3.new3(0, 0, 0);
    normals[n] = P3.new3(0, 0, 1);
    return MeshSurface.newMesh(false, vertexes, 0, faces, normals, 0);
  }

  /**
   * Calculates the data (faces, vertices, normals) for a triangle.
   * 
   * @param pt1 Vertex 1.
   * @param pt2 Vertex 2.
   * @param pt3 Vertex 3.
   * @return The data.
   */
  static MeshSurface getTriangleData(T3 pt1, T3 pt2,
                                              T3 pt3) {
    T3[] vertexes = new T3[] { pt1, pt2, pt3 };
    V3 v1 = V3.newVsub(pt3, pt1);
    V3 v2 = V3.newVsub(pt2, pt1);
    v2.cross(v2, v1);
    v2.normalize();
    V3[] normals = new V3[] { v2, v2, v2 };
    int[][] faces = { { 0, 1, 2 } };
    return MeshSurface.newMesh(false, vertexes, 0, faces, normals, 0);
  }

  /**
   * Calculates the data (faces, vertices, normals) for a cone.
   * 
   * @return The data.
   */
  static MeshSurface getConeData() {
    int ndeg = 10;
    int n = 360 / ndeg;
    P3[] vertices = new P3[n + 1];
    int[][] faces = AU.newInt2(n);
    for (int i = 0; i < n; i++)
      faces[i] = new int[] { i, (i + 1) % n, n };
    double d = ndeg / 180. * Math.PI;
    for (int i = 0; i < n; i++) {
      float x = (float) (Math.cos(i * d));
      float y = (float) (Math.sin(i * d));
      vertices[i] = P3.new3(x, y, 0);
    }
    vertices[n] = P3.new3(0, 0, 1);
    return MeshSurface.newMesh(false, vertices, 0, faces, vertices, 0);
  }

  /**
   * Calculates the data (faces, vertices, normals) for a cylinder.
   * 
   * @param inSide Whether inside or not.
   * @return The data.
   */
  static MeshSurface getCylinderData(boolean inSide) {
    int ndeg = 10;
    int vertexCount = 360 / ndeg * 2;
    int n = vertexCount / 2;
    int[][] faces = AU.newInt2(vertexCount);
    int fpt = -1;
    for (int i = 0; i < n; i++) {
      if (inSide) {
        // Adobe 9 bug: 
        // does not treat normals properly --
        // if you have normals, you should use them to decide
        // which faces to render - but NO, faces are rendered
        // strictly on the basis of windings. What??!

        faces[++fpt] = new int[] { i + n, (i + 1) % n, i };
        faces[++fpt] = new int[] { i + n, (i + 1) % n + n, (i + 1) % n };
      } else {
        faces[++fpt] = new int[] { i, (i + 1) % n, i + n };
        faces[++fpt] = new int[] { (i + 1) % n, (i + 1) % n + n, i + n };
      }
    }
    P3[] vertexes = new P3[vertexCount];
    P3[] normals = new P3[vertexCount];
    for (int i = 0; i < n; i++) {
      float x = (float) (Math.cos(i * ndeg / 180. * Math.PI));
      float y = (float) (Math.sin(i * ndeg / 180. * Math.PI));
      vertexes[i] = P3.new3(x, y, 0);
      normals[i] = P3.new3(x, y, 0);
    }
    for (int i = 0; i < n; i++) {
      float x = (float) (Math.cos((i + 0.5) * ndeg / 180 * Math.PI));
      float y = (float) (Math.sin((i + 0.5) * ndeg / 180 * Math.PI));
      vertexes[i + n] = P3.new3(x, y, 1);
      normals[i + n] = normals[i];
    }
    if (inSide)
      for (int i = 0; i < n; i++)
        normals[i].scale(-1);
    return MeshSurface.newMesh(false, vertexes, 0, faces, normals, 0);
  }

}
