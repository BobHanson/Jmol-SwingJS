/* $RCSfile$
 * $J. Gutow$
 * $July 2011$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.openscience.jmol.app.surfacetool;


import javajs.util.MeasureD;
import javajs.util.P3d;
import javajs.util.P4d;
import javajs.util.V3d;

class Slice {

  final P4d leftPlane = new P4d(); // definition of the left plane, using Jmol format
  final P4d middle = new P4d();//plane representing center of slice.
  final P4d rightPlane = new P4d(); // definition of the left plane
  double angleXY; // 0<=anglexy< PI/2 radians
  double anglefromZ;// 0<=anglefromZ < PI/2 radians
  double position; // distance of slice middle from origin
  double thickness; // thickness of slice
  final P3d boundBoxNegCorner = new P3d();
  final P3d boundBoxPosCorner = new P3d();
  final P3d boundBoxCenter = new P3d();
  double diagonal;

  /**
   * @param length
   *        (double) length of vector from origin
   * @param angleXY
   *        (double) angle of vector projection in XY plane (radians)
   * @param anglefromZ
   *        (double) angle of vector from Z axis (radians)
   * @param result
   *        (Point4f) meeting the Jmol definition of a plane.
   */
  static void makePlane(double length, double angleXY, double anglefromZ,
                        P4d result) {
    result.set4((double) (Math.cos(angleXY) * Math.sin(anglefromZ)),
        (double) (Math.sin(angleXY) * Math.sin(anglefromZ)),
        (double) (Math.cos(anglefromZ)), -length);
  }

  /**
   * Sets the right plane and left plane bounding a slice.
   * 
   * @param angleXY
   *        (double)angle in radians from X-axis to projection in XY plane
   * @param anglefromZ
   *        (double)angle in radians from z-axis to vector
   * @param position
   *        (double) position from origin of slice center along vector in
   *        molecular units
   * @param thickness
   *        (double) thickness of slice in molecular units.
   * @param boundBoxCenter
   *        (Point3f) center of the boundbox in molecular coordinates
   * @param boundBoxVec
   *        (Vector3f) vector from the boundbox center to the most positive
   *        corner.
   * @param useMolecular
   *        (boolean) if true angles and positions are relative to the origin of
   *        the molecular coordinate system. If false angles and position are
   *        relative to the center of the boundbox, which is usually more
   *        intuitive for the vwr as this is typically close to the center of
   *        the viewed object.
   */
  void setSlice(double angleXY, double anglefromZ, double position,
                double thickness, P3d boundBoxCenter, V3d boundBoxVec,
                boolean useMolecular) {
    if (angleXY >= 0 && angleXY < Math.PI) {
      this.angleXY = angleXY;
    } else {
      double fix = (double) (Math.floor(angleXY / Math.PI));
      this.angleXY = (double) (angleXY - fix * Math.PI);
    }
    if (anglefromZ >= 0 && anglefromZ < Math.PI) {
      this.anglefromZ = anglefromZ;
    } else {
      double fix = Math.floor(anglefromZ / Math.PI);
      this.anglefromZ = (double) (anglefromZ - fix * Math.PI);
    }
    this.position = position;
    this.thickness = thickness;
    this.boundBoxCenter.setT(boundBoxCenter);
    boundBoxNegCorner.sub2(boundBoxCenter, boundBoxVec);
    boundBoxPosCorner.add2(boundBoxCenter, boundBoxVec);
    diagonal = boundBoxPosCorner.distance(boundBoxNegCorner);
    makePlane(position, angleXY, anglefromZ, middle);
    if (!useMolecular) {
      //correct for the offset between the boundbox center and the origin
      P3d pt = P3d.new3(middle.x, middle.y, middle.z);
      pt.scaleAdd2(-middle.w, pt, boundBoxCenter);
      MeasureD.getPlaneThroughPoint(pt, V3d.new3(middle.x, middle.y,
          middle.z), middle);
    }
    leftPlane.set4(middle.x, middle.y, middle.z, middle.w);
    leftPlane.w += thickness / 2;
    rightPlane.set4(middle.x, middle.y, middle.z, middle.w);
    rightPlane.w -= thickness / 2;
    System.out.println(thickness + " left:" + leftPlane + " right:"
        + rightPlane);
  }

  /**
   * @param plane
   *        (Plane) the plane
   * @param start
   *        (Point3f) start of line segment
   * @param end
   *        (Point3f) end of line segement
   * @return a Point3f if line segment intersects plane
   */
  /*  private Point3f intersectionSegmentPlane(Plane plane, Point3f start,
                                             Point3f end) {
      Point3f intersection = new Point3f();
      Vector3f planeVec = Vector3f.new3(plane);
      Vector3f startVec = Vector3f.new3(start);
      Vector3f endVec = Vector3f.new3(end);
      double d = (planeVec.lengthSquared() - planeVec.dot(startVec))
          / (planeVec.dot(endVec) - planeVec.dot(startVec));
      if (d > 0 && d < 1) {
        intersection.x = start.x + d * (end.x - start.x);
        intersection.y = start.y + d * (end.y - start.y);
        intersection.z = start.z + d * (end.z - start.z);
      } else {
        intersection = null; // no intersection so don't return a value.
      }
      return (intersection);
    }*/

  /**
   * 
   * @return returns this Slice
   */
  Slice getSlice() {
    return this;
  }

  P4d getMiddle() {
    return middle;
  }

  /*	private Point3f[] calcPlaneVert(Plane plane) {
  		Point3f[] result = new Point3f[4];
  		double scale = (double) (0.5 * diagonal);
  		Vector3f tempVec = new Vector3f();
  		tempVec = vecScale(scale, vecAdd(plane.basis[0], plane.basis[1]));
  		result[0] = vectoPoint(vecAdd(tempVec, plane));
  		result[2] = vectoPoint(vecAdd(plane, vecScale(-1, tempVec)));
  		tempVec = vecScale(scale,
  				vecAdd(plane.basis[1], vecScale(-1, plane.basis[0])));
  		result[1] = vectoPoint(vecAdd(plane, tempVec));
  		result[3] = vectoPoint(vecAdd(plane, vecScale(-1, tempVec)));
  		return (result);
  	}*/
}
