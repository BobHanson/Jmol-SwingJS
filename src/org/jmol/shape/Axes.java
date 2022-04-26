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



import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.T3d;
import javajs.util.V3d;

import org.jmol.api.SymmetryInterface;

import javajs.util.BS;
import org.jmol.script.T;
import org.jmol.util.Escape;
import org.jmol.util.Font;
import org.jmol.viewer.JC;

public class Axes extends FontLineShape {

  public P3d axisXY = new P3d();
  public double scale;
  
  public P3d fixedOrigin;
  public final P3d originPoint = new P3d();
  
  /**
   * [x, y, z, -x, -y, -z] or [a, b, c, -a, -b, -c]
   */
  final public P3d[] axisPoints = new P3d[6];
  public String[] labels;
  public String axisType; //a b c ab, ac, bc
  public String axes2;
  
  {
    for (int i = 6; --i >= 0; )
      axisPoints[i] = new P3d();
  }

  private final static double MIN_AXIS_LEN = 1.5d;

  @Override
  public void setProperty(String propertyName, Object value, BS bs) {
    if ("position" == propertyName) {
      boolean doSetScale = (axisXY.z == 0 && ((P3d) value).z != 0);
      axisXY = (P3d) value;
      setScale(doSetScale ? 1 : scale); 
      // z = 0 for no set xy position (default)
      // z = -Double.MAX_VALUE for percent
      // z = Double.MAX_VALUE for positioned
      return;
    }
    if ("origin" == propertyName) {
      if (value == null || ((P3d) value).length() == 0) {
        fixedOrigin = null;
      } else {
        if (fixedOrigin == null)
          fixedOrigin = new P3d();
        fixedOrigin.setT((P3d) value);
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
      if ("abc".equals(axisType))
        axisType = null;
    }
    if ("axes2" == propertyName) {
      axes2 = (value == null || value.equals("") ? null : (String) value);
      return;
    }
    setPropFLS(propertyName, value);
  }

  private final P3d pt0 = new P3d();
  public final P3d fixedOriginUC = new P3d();

  @Override
  public void initShape() {
    translucentAllowed = false;
    myType = "axes";
    font3d = vwr.gdata.getFont3D(JC.AXES_DEFAULT_FONTSIZE);
    setPoints(vwr.g.axesMode);
  } 
  
  public void setPoints(int axesMode) {
    SymmetryInterface unitcell;
    if (axesMode != T.axesunitcell || ms.unitCells == null
        || (unitcell = vwr.getCurrentUnitCell()) == null) {
      originPoint.setT(fixedOrigin != null ? fixedOrigin
          : axesMode == T.axeswindow ? vwr.getBoundBoxCenter() : pt0);
      // We must divide by 2 because that is the default for non-unitcell axis types.
      setScale(vwr.getDouble(T.axesscale) / 2d);
      return;
    }
    // have unit cell
    T3d fset = unitcell.getUnitCellMultiplier();
    unitcell = unitcell.getUnitCellMultiplied();
    double voffset = vwr.getDouble(T.axesoffset);
    fixedOriginUC.set(voffset, voffset, voffset);
    P3d offset = unitcell.getCartesianOffset();
    P3d[] vertices = unitcell.getUnitCellVerticesNoOffset();
    originPoint.add2(offset, vertices[0]);
    if (voffset != 0)
      unitcell.toCartesianF(fixedOriginUC, false);
    else if (fixedOrigin != null)
      originPoint.setT(fixedOrigin);
    if (voffset != 0) {
      originPoint.add(fixedOriginUC);
    }
    double scale = this.scale = vwr.getDouble(T.axesscale) / 2d;
    if (fset != null && fset.z > 0)
      scale *= Math.abs(fset.z);
    axisPoints[0].scaleAdd2(scale, vertices[4], originPoint);
    axisPoints[1].scaleAdd2(scale, vertices[2], originPoint);
    axisPoints[2].scaleAdd2(scale, vertices[1], originPoint);
  }

  public void reinitShape() {
    Font f = font3d;
    initShape();
    if (f != null)
      font3d = f;
  }

  /**
   * get actual point or 1/2 vector from origin to this point
   * 
   * @param i
   * @param unscaled
   * @param ptTemp 
   * @return actual point if not a data frame and not an XY request; otherwise return 1/2 vector along unit cell
   */
  public P3d getAxisPoint(int i, boolean unscaled, P3d ptTemp) {
    if (unscaled) {
      ptTemp.setT(axisPoints[i]);
    } else {
      ptTemp.sub2(axisPoints[i], originPoint);
      ptTemp.scale(0.5d);
    }
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

  V3d corner = new V3d();
  
  void setScale(double scale) {
    this.scale = scale;
    corner.setT(vwr.getBoundBoxCornerVector());
    for (int i = 6; --i >= 0;) {
      P3d axisPoint = axisPoints[i];
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
  
  public String getAxesState(SB sb) {
    sb.append("  axes scale ").appendF(vwr.getDouble(T.axesscale)).append(";\n"); 
    if (fixedOrigin != null)
      sb.append("  axes center ")
          .append(Escape.eP(fixedOrigin)).append(";\n");
    if (axisXY.z != 0)
      sb.append("  axes position [")
          .appendI((int) axisXY.x).append(" ")
          .appendI((int) axisXY.y).append(" ")
          .append(axisXY.z < 0 ? " %" : "").append("]");
    if (axes2 != null)
      sb.append(" ").append(PT.esc(axes2));
    sb.append(";\n");
    if (labels != null) {
      sb.append("  axes labels ");
      for (int i = 0; i < labels.length; i++)
        if (labels[i] != null)
          sb.append(PT.esc(labels[i])).append(" ");
      sb.append(";\n");
    }
    if (axisType != null) {
      sb.append("  axes type " + PT.esc(axisType));
    }
    return sb.toString();
  }

}
