package org.jmol.shapesurface;

import javajs.util.OC;
import javajs.util.T3;

import javajs.util.BS;
import org.jmol.util.C;
import org.jmol.util.MeshSurface;

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
public class PMeshWriter {

  private IsosurfaceMesh imesh;
  boolean isBinary;
  private OC oc;
  private int i0;
  private int[][] polygonIndexes;
  private boolean selectedPolyOnly;
  private BS bsPolygons;
  private boolean haveBsDisplay;
  private boolean colorSolid;
  private boolean colorArrayed;
  private short cx;
  private short[] vertexColixes;
  private boolean noColor;
  private short[] contourColixes;
  private float[] vertexValues;
  private int vertexCount;
  private int[] imap;

 public PMeshWriter() {
   // for reflection
 }

  Object write(IsosurfaceMesh isosurfaceMesh, boolean isBinary) {
    imesh = isosurfaceMesh; 
    this.isBinary = isBinary;
    
    // Get all the needed information. Based IsosurfaceRenderer.renderTriangles.
    // Just contours right now.

    BS bsPoly = new BS();
    BS bsVert = new BS();
    BS bsPoints = new BS();
    
    if (imesh.showPoints || imesh.pc <= 0)
      checkPoints(bsPoints);
    bsVert.or(bsPoints);
    if (imesh.drawTriangles)
      checkTriangles(false, bsPoly, bsVert);
    if (imesh.pc > 0 && imesh.fillTriangles)
      checkTriangles(true, bsPoly, bsVert);
    
    imap = new int[vertexCount];
    int[] iimap = new int[vertexCount];
    int nV = 0;
    for (int i = bsVert.nextSetBit(0); i >= 0; i = bsVert.nextSetBit(i + 1)) {
        iimap[nV] = i;
        imap[i] = nV++;
    }
    
    writePmeshHeader(nV);
    if (!isBinary)
      outputInt(nV);
    for (int i = 0; i < nV; i++)
      outputXYZ(imesh.vs[iimap[i]]);
    if (!isBinary)
      outputInt(-1); // null-terminated

    if (imesh.showPoints || imesh.pc <= 0)
      outputPoints(bsPoints);
    bsVert.or(bsPoints);
    BS bsDone = new BS();
    if (imesh.drawTriangles)
      outputTriangles(false, bsPoly, bsDone);
    if (imesh.pc > 0 && imesh.fillTriangles)
      outputTriangles(true, bsPoly, bsDone);
    
    if (isBinary)
      oc.writeInt(0);
    else
      oc.append("0\n");
    oc.closeChannel();
    return (isBinary ? oc.toByteArray() : oc.toString());
  }

  private void outputPoints(BS bsPoints) {
    int color = C.getArgb(cx);
    for (int i = bsPoints.nextSetBit(0); i >= 0; i = bsPoints.nextSetBit(i + 1)) {
      if (!imesh.isColorSolid && imesh.vcs != null) {
        cx = imesh.vcs[i];
        color = C.getArgb(cx);
      }
      outputPoint(imap[i], color);
    }
  }

  private void outputTriangles(boolean fill, BS bsPoly, BS bsDone) {
    int color = C.getArgb(cx);
    for (int i = bsPoly.nextSetBit(0); i >= 0; i = bsPoly.nextSetBit(i + 1)) {
      int[] polygon = polygonIndexes[i];
      int iA = imap[polygon[0]];
      int iB = imap[polygon[1]];
      int iC = imap[polygon[2]];
      if (colorSolid) {
        if (colorArrayed && i < imesh.pcs.length)
          cx = imesh.pcs[i];
      } else {
        cx = vertexColixes[polygon[0]];
      }
      color = C.getArgb(cx);
      if (fill) {
        if (iB == iC) {
          if (iA == iB)
            outputPoint(iA, color);
          else
            outputEdge(iA, iB, color);
          bsDone.set(i);
        } else {
          if (imesh.colorsExplicit)
            color = polygon[MeshSurface.P_EXPLICIT_COLOR];
          outputTriangle(iA, iB, iC, color, 999);
        }
      } else if (!bsDone.get(i)) {
        // mesh only
        // check: 1 (ab) | 2(bc) | 4(ac)
        int check = 7 & polygon[MeshSurface.P_CHECK];
        if (check == 0)
          continue;
        if (noColor) {
        } else if (colorArrayed) {
          color = C.getArgb(imesh.fillTriangles ? C.BLACK
              : contourColixes[polygon[MeshSurface.P_CONTOUR]
                  % contourColixes.length]);
        }
        outputTriangle(iA, iB, iC, color, check);
      }
    }
  }

  private void checkPoints(BS bsVert) {
    boolean slabPoints = ((imesh.pc == 0) && selectedPolyOnly);
    int incr = imesh.vertexIncrement;
    for (int i = (!imesh.hasGridPoints || imesh.firstRealVertex < 0 ? 0
        : imesh.firstRealVertex); i < vertexCount; i += incr) {
      if (vertexValues != null && Float.isNaN(vertexValues[i])
          || imesh.jvxlData.thisSet != null
          && !imesh.jvxlData.thisSet.get(imesh.vertexSets[i])
          || !imesh.isColorSolid || haveBsDisplay && !imesh.bsDisplay.get(i)
          || slabPoints && !bsPolygons.get(i))
        continue;
      bsVert.set(i);
    }
  }

  private void checkTriangles(boolean fill, BS bsPoly, BS bsVert) {
    
    setup(fill);
    
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
      if (colorSolid && colorArrayed && i < imesh.pcs.length && imesh.pcs[i] == 0)
        continue;
      bsPoly.set(i);
      bsVert.set(iA);
      bsVert.set(iB);
      bsVert.set(iC);
    }
  }

  private void setup(boolean fill) {
    vertexCount = imesh.vc;
    vertexValues = imesh.vvs;
    polygonIndexes = imesh.pis;
    cx = (!fill && imesh.meshColix != 0 ? imesh.meshColix : imesh.colix);
    vertexColixes = (!fill && imesh.meshColix != 0 ? null : imesh.vcs);
    colorSolid = (vertexColixes == null);
    noColor = (vertexColixes == null || !fill && imesh.meshColix != 0);
    colorArrayed = (colorSolid && imesh.pcs != null);
    if (colorArrayed && !fill && imesh.fillTriangles)
      colorArrayed = false;
    contourColixes = imesh.jvxlData.contourColixes;
    haveBsDisplay = (imesh.bsDisplay != null);
    selectedPolyOnly = (imesh.bsSlabDisplay != null);
    bsPolygons = (selectedPolyOnly ? imesh.bsSlabDisplay : null);

  }

  private void writePmeshHeader(int nV) {
    oc = imesh.vwr.getOutputChannel(null, null);
    if (isBinary) {
      oc.writeByteAsInt(80);
      oc.writeByteAsInt(77);
      oc.writeByteAsInt(1);
      oc.writeByteAsInt(0);
      oc.writeInt(1);
      oc.writeInt(nV);
      oc.writeInt(-1);
      for (int i = 0; i < 16; i++)
        oc.writeInt(0); 
    } else {
      oc.append("#JmolPmesh\n");
    }
  }

  private void outputInt(int i) {
    if (isBinary)
      oc.writeInt(i);
    else
      oc.append("" + i + "\n");
  }

  private int outputPoint(int iA, int color) {
    outputInt(-1);
    outputInt(iA);
    outputInt(color);
    return 1;
  }

  private void outputXYZ(T3 pt) {
    if (isBinary) {
      oc.writeFloat(pt.x);
      oc.writeFloat(pt.y);
      oc.writeFloat(pt.z);
    } else {
      oc.append(pt.x + " " + pt.y + " " + pt.z + "\n");
    }
  }

  private void outputEdge(int iA, int iB, int color) {
    outputInt(-2);
    outputInt(iA);
    outputInt(iB);
    outputInt(color);
  }

  private void outputTriangle(int iA, int iB, int iC, int color, int check) {
    if (check == 999) {
      outputInt(-3);
      outputInt(iA);
      outputInt(iB);
      outputInt(iC);
      outputInt(color);
      return;
    }
    if ((check & 1) != 0)
      outputEdge(iA, iB, color);
    if ((check & 2) != 0)
      outputEdge(iB, iC, color);
    if ((check & 4) != 0)
      outputEdge(iC, iA, color);
  }
}