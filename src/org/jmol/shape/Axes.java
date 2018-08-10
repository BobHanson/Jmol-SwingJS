/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-05-08 12:29:47 -0500 (Sun, 08 May 2016) $
 * $Revision: 21082 $
 *
 * Copyright (C) 2002-2006  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.shape;



import javajs.awt.Font;
import javajs.util.P3;
import javajs.util.V3;

import org.jmol.api.SymmetryInterface;
import org.jmol.java.BS;
import org.jmol.script.T;
import org.jmol.viewer.JC;

public class Axes extends FontLineShape {

  public P3 axisXY = new P3();
  public float scale;
  
  public P3 fixedOrigin;
  public final P3 originPoint = new P3();
  
  /**
   * [x, y, z, -x, -y, -z] or [a, b, c, -a, -b, -c]
   */
  final public P3[] axisPoints = new P3[6];
  public String[] labels;
  public String axisType; //a b c ab, ac, bc
  
  {
    for (int i = 6; --i >= 0; )
      axisPoints[i] = new P3();
  }

  private final static float MIN_AXIS_LEN = 1.5f;

  @Override
  public void setProperty(String propertyName, Object value, BS bs) {
    if ("position" == propertyName) {
      boolean doSetScale = (axisXY.z == 0 && ((P3) value).z != 0);
      axisXY = (P3) value;
      setScale(doSetScale ? 1 : scale); 
      // z = 0 for no set xy position (default)
      // z = -Float.MAX_VALUE for percent
      // z = Float.MAX_VALUE for positioned
      return;
    }
    if ("origin" == propertyName) {
      if (value == null || ((P3) value).length() == 0) {
        fixedOrigin = null;
      } else {
        if (fixedOrigin == null)
          fixedOrigin = new P3();
        fixedOrigin.setT((P3) value);
      }
      reinitShape();
      return;
    }
    if ("labels" == propertyName) {
      labels = (String[]) value;
      return;
    }
    if ("labelsOn" == propertyName) {
      labels = null;
      return;
    }
    if ("labelsOff" == propertyName) {
      labels = new String[] {"", "", ""};
      return;
    }
    if ("type" == propertyName) {
      axisType = (String) value;
      if (axisType.equals("abc"))
        axisType = null;
    }
    
    setPropFLS(propertyName, value);
  }

  private final P3 pt0 = new P3();
  public final P3 fixedOriginUC = new P3();

  @Override
  public void initShape() {
    translucentAllowed = false;
    myType = "axes";
    font3d = vwr.gdata.getFont3D(JC.AXES_DEFAULT_FONTSIZE);
    int axesMode = vwr.g.axesMode;
    if (axesMode == T.axesunitcell && ms.unitCells != null) {
      SymmetryInterface unitcell = vwr.getCurrentUnitCell();
      if (unitcell != null) {
        float voffset = vwr.getFloat(T.axesoffset);
        fixedOriginUC.set(voffset, voffset, voffset);
        P3 offset = unitcell.getCartesianOffset();
        P3[] vertices = unitcell.getUnitCellVerticesNoOffset();
        originPoint.add2(offset, vertices[0]);
        if (voffset != 0)
          unitcell.toCartesian(fixedOriginUC, false);
        else if (fixedOrigin != null)
          originPoint.setT(fixedOrigin);
        if (voffset != 0) {
          originPoint.add(fixedOriginUC);
        }
//        unitcell.setAxes(scale, axisPoints, fixedO, originPoint);
        // We must divide by 2 because that is the default for ALL axis types.
        // Not great, but it will have to do.
        scale = vwr.getFloat(T.axesscale) / 2f;
        // these are still relative vectors, not points
        axisPoints[0].scaleAdd2(scale, vertices[4], originPoint);
        axisPoints[1].scaleAdd2(scale, vertices[2], originPoint);
        axisPoints[2].scaleAdd2(scale, vertices[1], originPoint);
        return;
      }
    }
    originPoint.setT(fixedOrigin != null ? fixedOrigin
        : axesMode == T.axeswindow ? vwr.getBoundBoxCenter() 
        : pt0);
    setScale(vwr.getFloat(T.axesscale) / 2f);
  }
  
  public void reinitShape() {
    Font f = font3d;
    initShape();
    if (f != null)
      font3d = f;
  }

  final P3 ptTemp = new P3();
  /**
   * get actual point or 1/2 vector from origin to this point
   * 
   * @param i
   * @param isDataFrame
   * @return actual point if not a data frame and not an XY request; otherwise return 1/2 vector along unit cell
   */
  public P3 getAxisPoint(int i, boolean isDataFrame) {
    if (!isDataFrame && axisXY.z == 0)
      return axisPoints[i];
    ptTemp.sub2(axisPoints[i], originPoint);
    ptTemp.scale(0.5f);
    return ptTemp; 
  }
  

  @Override
  public Object getProperty(String property, int index) {
    if (property == "origin")
      return fixedOrigin;
    if (property == "axesTypeXY")
      return (axisXY.z == 0 ? Boolean.FALSE : Boolean.TRUE);
    return null;
  }

  V3 corner = new V3();
  
  void setScale(float scale) {
    this.scale = scale;
    corner.setT(vwr.getBoundBoxCornerVector());
    for (int i = 6; --i >= 0;) {
      P3 axisPoint = axisPoints[i];
      axisPoint.setT(JC.unitAxisVectors[i]);
      // we have just set the axisPoint to be a unit on a single axis
   
      // therefore only one of these values (x, y, or z) will be nonzero
      // it will have value 1 or -1
      if (corner.x < MIN_AXIS_LEN)
        corner.x = MIN_AXIS_LEN;
      if (corner.y < MIN_AXIS_LEN)
        corner.y = MIN_AXIS_LEN;
      if (corner.z < MIN_AXIS_LEN)
        corner.z = MIN_AXIS_LEN;
      if (axisXY.z == 0) {
        axisPoint.x *= corner.x * scale;
        axisPoint.y *= corner.y * scale;
        axisPoint.z *= corner.z * scale;
      }
      axisPoint.add(originPoint);
    }
  }
  
// @Override
//public String getShapeState() {
//   return null;
//  }

}
