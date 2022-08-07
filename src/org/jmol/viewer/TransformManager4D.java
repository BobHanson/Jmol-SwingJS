/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-12-18 10:29:29 -0600 (Mon, 18 Dec 2006) $
 * $Revision: 6502 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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
package org.jmol.viewer;

import javajs.util.M4d;
import javajs.util.P4d;
import javajs.util.T3d;
import javajs.util.T4d;
import javajs.util.V3d;

import javajs.util.BS;

public class TransformManager4D extends TransformManager {

  private static final int MODE_3D = 0;
  private static final int MODE_4D_WX = 1;
  private static final int MODE_4D_WY = 2;
  private static final int MODE_4D_WZ = 3;
  public TransformManager4D() {
  }

  private boolean is4D = false;
    
  private int mouseMode = MODE_4D_WZ;

  private M4d m2_rotate;
  private final M4d m3_toScreen = new M4d();
  private double zOffset;
  private final T3d v1 = new V3d();
  private final M4d m4 = new M4d();
  private T4d p4 = new P4d();
  private final V3d zero = new V3d();
    
  @Override
  public void resetRotation() {
    if (m2_rotate != null)
      m2_rotate.setIdentity();
    matrixRotate.setScale(1); // no rotations
  }

  @Override
  protected void rotateXYBy(double xDelta, double yDelta, BS bsAtoms) {
    // from mouse action
    
    rotate3DBall(xDelta, yDelta, bsAtoms);
    switch (is4D && bsAtoms == null ? mouseMode : MODE_3D) {
    case MODE_3D:
      m2_rotate = null;
      break;
    case MODE_4D_WX: // arbitrary definition here
      checkM2();
      rotate4DBall(0, xDelta, yDelta);
      //rotateXZRadians4D(-yDelta * JC.radiansPerDegree);
      //rotateYZRadians4D(xDelta * JC.radiansPerDegree);
      break;
    case MODE_4D_WY:
      checkM2();
      rotate4DBall(xDelta, 0, yDelta);
      //rotateWXRadians4D(yDelta * JC.radiansPerDegree);
      //rotateWYRadians4D(xDelta * JC.radiansPerDegree);
      break;
    case MODE_4D_WZ:
      checkM2();
      rotate4DBall(xDelta, yDelta, 0);
      //rotateWXRadians4D(yDelta * JC.radiansPerDegree);
      //rotateWYRadians4D(xDelta * JC.radiansPerDegree);
      break;
    }
  }

  protected void rotate4DBall(double xDelta, double yDelta, double zDelta) {
    double scale = 50d;
    setAsBallRotation(m4, scale, xDelta, yDelta, zDelta);
    m2_rotate.mul2(m4, m2_rotate);
  }
  /**
   * 4D ball rotation from nominal dx, dy, dz motion.
   * 
   * 
   * @param m 
   * @param scale 
   * @param dx
   * @param dy
   * @param dz
   * @author Andrew Hanson -- see http://www.cse.ohio-state.edu/~hwshen/888_su02/hanson_note.pdf
   */
  public void setAsBallRotation(M4d m, double scale, double dx, double dy, double dz) {
    double dxyz2 = dx * dx + dy * dy + dz * dz;
    double sxyz = (double) Math.sqrt(dxyz2);
    double th =  sxyz / scale;
    double c = (double) Math.cos(th);
    double s = (double) Math.sin(th);
    double nx = dx / sxyz;
    double ny = dy / sxyz;
    double nz = dz / sxyz;
    double c1 = c - 1;
    
    m.m00 = 1 + c1 * nx * nx;
    m.m11 = 1 + c1 * ny * ny;
    m.m22 = 1 + c1 * nz * nz;
    m.m33 = c;

    m.m01 = m.m10 = c1 * nx * ny;
    m.m02 = m.m20 = c1 * nx * nz;
    m.m12 = m.m21 = c1 * ny * nz;
    
    m.m30 = -(m.m03 = s * nx);
    m.m31 = -(m.m13 = s * ny);
    m.m32 = -(m.m23 = s * nz);
  }

  private void checkM2() {
    if (m2_rotate == null)
      m2_rotate = M4d.newMV(matrixRotate, zero);
  }

  @Override
  public synchronized void calcTransformMatrix() {
    super.calcTransformMatrix();
    //is4D = vwr.getTestFlag(2);
    doTransform4D  = (is4D && !stereoFrame && mode != MODE_NAVIGATION);
    if (!doTransform4D)
      return;

    // first, translate the coordinates back to the center

    v1.sub2(frameOffset, fixedRotationCenter);

    checkM2();
    
    // scale to screen coordinates
    m3_toScreen.setIdentity();
    m3_toScreen.m00 = m3_toScreen.m11 = m3_toScreen.m22 = scalePixelsPerAngstrom;
    // negate y (for screen) and z (for zbuf)
    m3_toScreen.m11 = m3_toScreen.m22 = -scalePixelsPerAngstrom;

    //System.out.println(m2_rotate);

    zOffset = modelCenterOffset;
  }

  @Override
  protected void getScreenTemp(T3d ptXYZ) {
    if (doTransform4D && ptXYZ instanceof T4d) {
      p4.add2(ptXYZ, v1);                      // 3D centering
      m2_rotate.rotate(p4);                    // 4D rotation
      fScrPt.setT(p4);              // 3D truncation
      m3_toScreen.rotTrans(fScrPt); // 3D scaling
      fScrPt.z += zOffset;          // 3D offset
    } else {
      matrixTransform.rotTrans2(ptXYZ, fScrPt);
    }
  }

 

/*
 * From Andy Hanson (U. Indiana)

// first is just what we use here

mat33 roll3D(dx, dy, scale) {
  th = sqrt(dx^2+dy^2)/scale
  nx = -dy/sqrt(dx^2+dy^2)
  ny = dx/sqrt(dx^2+dy^2)
  nz = 0
  double c,s,mat[3,3];
  c= cos(th); s = sin(th);
  mat[1,1] =  1 + (-1 + c)*nx^2,;
  mat[1,2] =  (-1 + c)*nx*ny;
  mat[1,3] =  nx*s;
  mat[2,1] = (-1 + c)*nx*ny;
  mat]2,2] = 1 + (-1 + c)*ny^2;
  mat[2,3] =  ny*s;
  mat[3,1] = -(nx*s);
  mat[3,2] = -(ny*s);
  mat[3,3] = c;
  return(mat);
}
  
  
// second just mixes in w.
// nz here comes from swapping with ny 

mat44 roll34D(th,nx,ny,nz) {
double c,s,mat[4,4];
c= cos(th); s = sin(th);
mat[1,1] =  1 + (-1 + c)*nx^2,;
mat[1,2] =  (-1 + c)*nx*ny;
mat[1,3] =  (-1 + c)*nx*nz;
mat[1,4] =  nx*s;
mat[2,1] = (-1 + c)*nx*ny;
mat]2,2] = 1 + (-1 + c)*ny^2;
mat[2,3] = (-1 + c)* ny* nz;
mat[2,4] =  ny*s;
mat[3,1] = (-1 + c)*nx* nz;
mat[3,2] = (-1 + c)*ny* nz;
mat[3,3] =   1 + (-1 + c)*nz^2
mat[3,4] =  nz*s;
mat[4,1] = -(nx*s);
mat[4,2] = -(ny*s);
mat[4,3] = -(nz*s);
mat[4,4] =  c;
 return(mat); 
*/ 
  
}
