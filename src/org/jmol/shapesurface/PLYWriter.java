package org.jmol.shapesurface;

import org.jmol.util.C;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.OC;
import javajs.util.T3d;

/**
 * A class called by reflection from IsosurfaceMesh from the Jmol command
 * 
 * WRITE xxxx.pmesh (ascii)  
 * 
 * or
 * 
 * WRITE xxxx.pmb  (binary)
 * 
 * 
 */
public class PLYWriter {

  private IsosurfaceMesh imesh;
  boolean isBinary, writeShorts;
  private OC oc;
  private int[][] polygonIndexes;
  private boolean selectedPolyOnly;
  private BS bsPolygons;
  private boolean haveBsDisplay;
  private int vertexCount;
  private int[] mapJmolToPLY, mapPLYToJmol;

 public PLYWriter() {
   // for reflection
 }

  Object write(IsosurfaceMesh isosurfaceMesh, boolean isBinary) {
    imesh = isosurfaceMesh; 
    this.isBinary = isBinary;
    
    // Get all the needed information.

    setup();

    // identify the needed vertices and triangles
    
    BS bsPoly = new BS();
    BS bsVert = new BS();
    
    checkTriangles(bsPoly, bsVert);

    int nP = bsPoly.cardinality();
    int nV = bsVert.cardinality();
    
    mapJmolToPLY = new int[vertexCount];
    mapPLYToJmol = new int[nV];
    for (int v = 0, i = bsVert.nextSetBit(0); i >= 0; i = bsVert.nextSetBit(i + 1)) {
        mapPLYToJmol[v] = i;
        mapJmolToPLY[i] = v++;
    }
    
    writeShorts = (nV <= Short.MAX_VALUE);    
    writePLYHeader(nV, nP);
    writeVertices(nV);
    writeTriangles(bsPoly);

    oc.closeChannel();
    
    return (isBinary ? oc.toByteArray() : oc.toString());
  }
  
  private void writeVertices(int nV) {
    boolean haveVertexColors = (!imesh.isColorSolid && imesh.vcs != null);
    short cx = 0;
    if (!haveVertexColors)
      cx = imesh.colix;
    for (int i = 0; i < nV; i++) {
      if (haveVertexColors) {
        cx = imesh.vcs[mapPLYToJmol[i]];
      }
      int color = C.getArgb(cx);
      outputXYZ(imesh.vs[mapPLYToJmol[i]], color);
    }
  }

  private void writeTriangles(BS bsPoly) {
    for (int i = bsPoly.nextSetBit(0); i >= 0; i = bsPoly.nextSetBit(i + 1)) {
      int[] polygon = polygonIndexes[i];
      int iA = mapJmolToPLY[polygon[0]];
      int iB = mapJmolToPLY[polygon[1]];
      int iC = mapJmolToPLY[polygon[2]];
      outputTriangle(iA, iB, iC);
    }
  }

  private void checkTriangles(BS bsPoly, BS bsVert) {
    for (int i = imesh.pc; --i >= 0;) {
      int[] polygon = polygonIndexes[i];
      if (polygon == null || selectedPolyOnly && !bsPolygons.get(i))
        continue;
      int iA = polygon[0];
      if (imesh.jvxlData.thisSet != null && imesh.vertexSets != null
          && !imesh.jvxlData.thisSet.get(imesh.vertexSets[iA]))
        continue;
      int iB = polygon[1];
      int iC = polygon[2];
      if (haveBsDisplay
          && (!imesh.bsDisplay.get(iA) || !imesh.bsDisplay.get(iB) || !imesh.bsDisplay.get(iC)))
        continue;
      if (iA == iB || iB == iC || iA == iC)
        continue;
      bsPoly.set(i);
      bsVert.set(iA);
      bsVert.set(iB);
      bsVert.set(iC);
    }
  }

  private void setup() {
    oc = imesh.vwr.getOutputChannel(null, null);
    if (isBinary)
      oc.setBigEndian(false);
    vertexCount = imesh.vc;
    polygonIndexes = imesh.pis;
    haveBsDisplay = (imesh.bsDisplay != null);
    selectedPolyOnly = (imesh.bsSlabDisplay != null);
    bsPolygons = (selectedPolyOnly ? imesh.bsSlabDisplay : null);

  }

  private void writePLYHeader(int nV, int nT) {
    String format = (isBinary ? "binary_little_endian" : "ascii");
    oc.append("ply\n" + "format "+ format + " 1.0\n" + "comment Created by Jmol "
        + Viewer.getJmolVersion() + "\n" + "element vertex " + nV + "\n"
        + "property float x\n" //
        + "property float y\n" //
        + "property float z\n" +
        //            (data.normalCount > 0 ? 
        //                "property float nx\n" + 
        //                "property float ny\n" + 
        //                "property float nz\n"
        //            : "") +
        "property uchar red\n" //
        + "property uchar green\n" //
        + "property uchar blue\n"// 
        + "element face " + nT + "\n" //
        + "property list uchar " + (writeShorts ? "short" : "int") + " vertex_indices\n" //
        + "end_header\n");
  }

  private void outputInt(int i) {
    if (isBinary)
      oc.writeInt(i);
    else
      oc.append(" " + i);
  }

  private void outputXYZ(T3d pt, int color) {
    int r = (color >> 16) & 0xFF;
    int g = (color >> 8) & 0xFF;
    int b = (color) & 0xFF;
    if (isBinary) {
      oc.writeFloat((float) pt.x);
      oc.writeFloat((float) pt.y);
      oc.writeFloat((float) pt.z);
      oc.writeByteAsInt(r);
      oc.writeByteAsInt(g);
      oc.writeByteAsInt(b);
    } else {
      oc.append(((float) pt.x) + " " + ((float)pt.y) + " " + ((float) pt.z) 
          + " " + r + " " + g + " " + b + "\n");
    }
  }

  private void outputTriangle(int iA, int iB, int iC) {
    if (isBinary) {
      oc.writeByteAsInt(3);
      if (writeShorts) {
        oc.writeShort((short) iA);
        oc.writeShort((short) iB);
        oc.writeShort((short) iC);
      } else {
        outputInt(iA);
        outputInt(iB);
        outputInt(iC);
      }

    } else {
      oc.append("3 " + iA + " " + iB + " " + iC + "\n");
    }
  }
}