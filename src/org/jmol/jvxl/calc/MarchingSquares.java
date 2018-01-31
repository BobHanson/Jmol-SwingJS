/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-30 11:40:16 -0500 (Fri, 30 Mar 2007) $
 * $Revision: 7273 $
 *
 * Copyright (C) 2007 Miguel, Bob, Jmol Development
 *
 * Contact: hansonr@stolaf.edu
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.jvxl.calc;

import java.util.Hashtable;
import java.util.Map;


import org.jmol.jvxl.api.VertexDataServer;
import org.jmol.jvxl.data.VolumeData;
import org.jmol.util.Logger;

import javajs.util.AU;
import javajs.util.P3;
import javajs.util.P4;

public class MarchingSquares {

  /*
   * (Used to be) an adaptation of Marching Cubes to a two-dimensional slice.
   * See Version 11.8 for that; this is far simpler.
   * 
   * Author: Bob Hanson, hansonr@stolaf.edu
   *  
   */

  public final static int CONTOUR_POINT = -1;
  public final static int VERTEX_POINT = -2;
  public final static int EDGE_POINT = -3;

  VertexDataServer surfaceReader;
  VolumeData volumeData;

  private final static int nContourMax = 100;
  public final static int defaultContourCount = 9; //odd is better
  private int nContourSegments;
  public int contourType;//0, 1, or 2
  int thisContour = 0;
  private float valueMin, valueMax;

  final P3 pointA = new P3();
  final P3 pointB = new P3();

  private boolean contourFromZero = true;
  private float[] contoursDiscrete;

  /**
   * 
   * @param surfaceReader
   * @param volumeData
   * @param thePlane   NOT USED
   * @param contoursDiscrete
   * @param nContours
   * @param thisContour
   * @param contourFromZero
   */
  public MarchingSquares(VertexDataServer surfaceReader, VolumeData volumeData,
      P4 thePlane, float[] contoursDiscrete, int nContours,
      int thisContour, boolean contourFromZero) {
    this.surfaceReader = surfaceReader;
    this.volumeData = volumeData;
    this.thisContour = thisContour;
    this.contoursDiscrete = contoursDiscrete;
    this.contourFromZero = contourFromZero; // really just a stand-in for "!fullPlane" //set false for MEP to complete the plane
    if (contoursDiscrete == null) {
      int i = 0;// DEAD CODE (true ? 0 : contourFromZero ? 1  : is3DContour ? 1 : 2);
      nContourSegments = (nContours == 0 ? defaultContourCount : nContours) + i;
      if (nContourSegments > nContourMax)
        nContourSegments = nContourMax;
    } else {
      nContours = contoursDiscrete.length;
      nContourSegments = nContours;
      this.contourFromZero = false;
    }
  }

  public void setMinMax(float valueMin, float valueMax) {
    this.valueMin = valueMin;
    this.valueMax = valueMax;
  }

  public int contourVertexCount;
  ContourVertex[] contourVertexes = new ContourVertex[1000];

  private class ContourVertex extends P3 {
    float value;

    ContourVertex(P3 vertexXYZ) {
      setT(vertexXYZ);
    }

    void setValue(float value) {
      this.value = value;
    }
    
    @Override
    public String toString() {
      return value + " " + x + " " + y + " " + z;
    }

  }

  public int addContourVertex(P3 vertexXYZ, float value) {
    if (contourVertexCount == contourVertexes.length)
      contourVertexes = (ContourVertex[]) AU
          .doubleLength(contourVertexes);
    int vPt = surfaceReader.addVertexCopy(vertexXYZ, value, VERTEX_POINT, true);
    contourVertexes[contourVertexCount++] = new ContourVertex(vertexXYZ);
    return vPt;
  }

  public void setContourData(int i, float value) {
    contourVertexes[i].setValue(value);
  }

  float contourPlaneMinimumValue;
  float contourPlaneMaximumValue;

  public float[] contourValuesUsed;

  /* save - was used previously
   * 
   private boolean isInside2d(float voxelValue, float max) {
     return contourFromZero ? 
         (max > 0 && voxelValue >= max) || (max <= 0 && voxelValue <= max)
         : voxelValue < max;
   }
  */
  
  float calcContourPoint(float cutoff, float valueA, float valueB,
                         P3 pt) {
    
    return volumeData.calculateFractionalPoint(cutoff, pointA, pointB, valueA,
        valueB, pt);
  }

  final P3 ptTemp = new P3();

  /////////// MUCH simpler!

  private int triangleCount = 0;
  private Triangle[] triangles = new Triangle[1000];

  /**
   * 
   * @param iA
   * @param iB
   * @param iC
   * @param check
   * @param iContour
   * @return       0
   */
  public int addTriangle(int iA, int iB, int iC, int check, int iContour) {
    if (triangleCount == triangles.length)
      triangles = (Triangle[]) AU.doubleLength(triangles);
    triangles[triangleCount++] = new Triangle(iA, iB, iC, check, iContour);
    return 0;
  }

  Map<String, Integer> htPts = new Hashtable<String, Integer>();

  private class Triangle {
    protected int[] pts;
    protected int check;
    protected boolean isValid = true;
    protected int contourIndex;

    Triangle(int iA, int iB, int iC, int check, int contourIndex) {
      pts = new int[] { iA, iB, iC };
      this.check = check;
      this.contourIndex = contourIndex;
    }

/*
    void setValidity() {
      isValid &= (!Float.isNaN(contourVertexes[pts[0]].value)
          && !Float.isNaN(contourVertexes[pts[1]].value)
          && !Float.isNaN(contourVertexes[pts[2]].value));
    }
*/    
  }

  
  
  public int generateContourData(boolean haveData, float zeroOffset) {
    Logger.info("generateContours: " + nContourSegments + " segments");
    getVertexValues(haveData);
    createContours(valueMin, valueMax, zeroOffset);
    addAllTriangles();
    return contourVertexCount;
  }

  private void getVertexValues(boolean haveData) {
    contourPlaneMinimumValue = Float.MAX_VALUE;
    contourPlaneMaximumValue = -Float.MAX_VALUE;
    for (int i = 0; i < contourVertexCount; i++) {
      ContourVertex c = contourVertexes[i];
      //Point3i pt = locatePixel(c);
      //c.setPixelLocation(pt);
      float value;
      if (haveData) {
        value = c.value;
      } else {
        value = volumeData.lookupInterpolatedVoxelValue(c, false);
        c.setValue(value);
      }
      if (value < contourPlaneMinimumValue)
        contourPlaneMinimumValue = value;
      if (value > contourPlaneMaximumValue)
        contourPlaneMaximumValue = value;
    }
  }

  private boolean createContours(float min, float max, float zeroOffset) {
    float diff = max - min;
    contourValuesUsed = new float[nContourSegments];
    for (int i = triangleCount; --i >= 0;)
      triangles[i].check = 0;

    //float maxCutoff = Float.MAX_VALUE;
    float minCutoff = -Float.MAX_VALUE;
    //float lastCutoff = 0;
    float cutoff = minCutoff;
    for (int i = 0; i < nContourSegments; i++) {
      //lastCutoff = cutoff;
      cutoff = (contoursDiscrete != null ? contoursDiscrete[i]
          : contourFromZero ? min + (i * 1f / nContourSegments) * diff
              : i == 0 ? -Float.MAX_VALUE
                  : i == nContourSegments - 1 ? Float.MAX_VALUE : min
                      + ((i - 1) * 1f / (nContourSegments - 1)) * diff);
      /*
       * cutoffs right near zero cause problems, so we adjust just a tad
       * 
       */
      if (contoursDiscrete == null && Math.abs(cutoff) < zeroOffset)
        cutoff = (cutoff < 0 ? -zeroOffset : zeroOffset);
      contourValuesUsed[i] = cutoff;

      Logger.info("#contour " + (i + 1)+ " " + cutoff + " " + triangleCount);
      htPts.clear();
      for (int ii = triangleCount; --ii >= 0;) {
        if (triangles[ii].isValid)
          checkContour(triangles[ii], i, cutoff);
      }
      if (thisContour > 0) {
        if (i + 1 == thisContour)
          minCutoff = cutoff;
      //  else if (i == thisContour)
        //  maxCutoff = cutoff;
      } else {
       // maxCutoff = cutoff;
      }
    }

    if (contoursDiscrete != null) {
      minCutoff = contoursDiscrete[0];
      //maxCutoff = contoursDiscrete[contoursDiscrete.length - 1];
    }
    valueMin = contourValuesUsed[0];
    valueMax = (contourValuesUsed.length == 0 ?
        valueMin : contourValuesUsed[contourValuesUsed.length - 1]);
    
    /*
    if (contourFromZero || contoursDiscrete != null) {
      
      
      for (int i = 0; i < contourVertexCount; i++) {
        float v = contourVertexes[i].value;
        if (v > maxCutoff || v < minCutoff)
          contourVertexes[i].value = Float.NaN;
      }
      
      
      for (int i = triangleCount; --i >= 0;)
        triangles[i].setValidity();
    }
    */
    
    return true;
  }

  private int intercept(Triangle t, int i, float value) {
    int iA = t.pts[i];
    int iB = t.pts[(i + 1) % 3];
    if (iA == Integer.MAX_VALUE || iB == Integer.MAX_VALUE)
      return -1;
    String key = (iA < iB ? iA + "_" + iB : iB + "_" + iA);
    if (htPts.containsKey(key))
      return htPts.get(key).intValue();
    float valueA = contourVertexes[iA].value;
    float valueB = contourVertexes[iB].value;
    int iPt = -1;
    if (valueA != valueB) {
      float f = (value - valueA) / (valueB - valueA);
      if (f >= 0 && f <= 1) {
        pointA.setT(contourVertexes[iA]);
        pointB.setT(contourVertexes[iB]);
        value = calcContourPoint(value, valueA, valueB, ptTemp);
        if (!Float.isNaN(value)) {
          iPt = addContourVertex(ptTemp, value);
          if (iPt < 0)
            return -1;
          contourVertexes[iPt].setValue(value);
        } else {
//          System.out.println("#MarchingSquares nonlinear problem for contour " + (i + 1) + " at " + ptTemp + " " + valueA + " " + valueB 
//              + "\ndraw ID \"pt" + ptTemp + "\" scale 5.0 " + ptTemp );
        }
      }
    }
    htPts.put(key, Integer.valueOf(iPt));
    return iPt;
  }

  private void checkContour(Triangle t, int i, float value) {
    if (thisContour > 0 && i + 1 != thisContour)
      return;
    int ipt0 = intercept(t, 0, value);
    int ipt1 = intercept(t, 1, value);
    int ipt2 = intercept(t, 2, value);
    int[] pts = t.pts;
    int mode = 0;
    if (ipt0 >= 0) {
      mode += 1;
    }
    if (ipt1 >= 0) {
      mode += 2;
    }
    if (ipt2 >= 0) {
      mode += 4;
    }
    switch (mode) {
    case 3:
      addTriangle(pts[0], ipt0, ipt1, 2 | (t.check & 1), i);
      addTriangle(ipt0, pts[1], ipt1, 4 | (t.check & 3), i);
      addTriangle(pts[0], ipt1, pts[2], (t.check & 6), i);
      break;
    case 5:
      addTriangle(pts[0], ipt0, ipt2, 2 | (t.check & 5), i);
      addTriangle(ipt0, pts[1], ipt2, 4 | (t.check & 1), i);
      addTriangle(ipt2, pts[1], pts[2], (t.check & 6), i);
      break;
    case 6:
      addTriangle(pts[0], pts[1], ipt2, (t.check & 5), i);
      addTriangle(ipt2, pts[1], ipt1, 4 | (t.check & 2), i);
      addTriangle(ipt2, ipt1, pts[2], 1 | (t.check & 6), i);
      break;
    default:
      return;
    }
    t.isValid = false;
  }

  public float[] getMinMax() {
    return new float[] { valueMin, valueMax };
  }
  private void addAllTriangles() {
    for (int i = 0; i < triangleCount; i++)
      if (triangles[i].isValid) {
        Triangle t = triangles[i];
        surfaceReader.addTriangleCheck(t.pts[0], t.pts[1], t.pts[2], t.check,
            t.contourIndex, false, -1);
      }
  }

}
