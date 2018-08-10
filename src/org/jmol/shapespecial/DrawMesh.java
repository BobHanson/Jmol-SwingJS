/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-14 23:28:16 -0500 (Sat, 14 Apr 2007) $
 * $Revision: 7408 $
 *
 * Copyright (C) 2005  Miguel, The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net, jmol-developers@lists.sf.net
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

package org.jmol.shapespecial;

import org.jmol.java.BS;
import org.jmol.shape.Mesh;
import org.jmol.shapespecial.Draw.EnumDrawType;
import org.jmol.util.BSUtil;
import org.jmol.viewer.Viewer;

import javajs.util.AU;
import javajs.util.P3;
import javajs.util.V3;

public class DrawMesh extends Mesh {

  /**
   * @j2sIgnoreSuperConstructor
   * 
   * @param vwr 
   * @param thisID
   * @param colix
   * @param index
   */
  public DrawMesh(Viewer vwr, String thisID, short colix, int index) {
    drawType = EnumDrawType.NONE;
    axis = V3.new3 (1, 0, 0);
    bsMeshesVisible =  new BS ();
    mesh1(vwr, thisID, colix, index);
  }

  public BS bsMeshesVisible;
  public BS modelFlags;
  public EnumDrawType drawType;
  EnumDrawType[] drawTypes;
  P3 ptCenters[];
  V3 axis;
  V3 axes[];
  int drawVertexCount;
  int[] drawVertexCounts;
  boolean isFixed;
  public boolean isVector;
  public float drawArrowScale;
  public boolean noHead;
  public boolean isBarb;
  public float scale = 1;
  public boolean isScaleSet;


  @Override
  public void clear(String meshType) {
    clearMesh(meshType);
    scale = 1;
    isScaleSet = false;
  }
  
  void setCenters() {
    if (ptCenters == null)
      setCenter(-1);
    else
      for (int i = ptCenters.length; --i >= 0; )
        setCenter(i);
  }

  final void setCenter(int iModel) {
    P3 center = P3.new3(0, 0, 0);
    int iptlast = -1;
    int ipt = 0;
    int n = 0;
    for (int i = pc; --i >= 0;) {
      if (iModel >=0 && i != iModel || pis[i] == null)
        continue;
      iptlast = -1;
      for (int iV = (drawType == EnumDrawType.POLYGON) ? 3 
          : pis[i].length; --iV >= 0;) {
        ipt = pis[i][iV];
        if (ipt == iptlast)
          continue;
        iptlast = ipt;
        center.add(vs[ipt]);
        n++;
      }
      if (n > 0 && (i == iModel || i == 0)) {
        center.scale(1.0f / n);
        if (mat4 != null)
          mat4.rotTrans(center);
        break;
      }
    }
    if (iModel < 0){
      ptCenter.setT(center);
    } else {
      ptCenters[iModel] = center;
    }
  }

  void offset(V3 offset) {
    rotateTranslate(null, offset, false);
    setCenters();
  }

  public void deleteAtoms(int modelIndex) {
    if (modelIndex >= pc)
      return;
    pc--;
    pis = (int[][]) AU.deleteElements(pis, modelIndex, 1);
    drawTypes = (EnumDrawType[]) AU.deleteElements(drawTypes, modelIndex, 1);
    drawVertexCounts = (int[]) AU.deleteElements(drawVertexCounts, modelIndex, 1);
    ptCenters = (P3[]) AU.deleteElements(ptCenters, modelIndex, 1);
    axes = (V3[]) AU.deleteElements(axes, modelIndex, 1);
    BS bs = BSUtil.newAndSetBit(modelIndex);
    BSUtil.deleteBits(modelFlags, bs);
    //no! title = (String[]) ArrayUtil.deleteElements(title, modelIndex, 1);
  }

  public boolean isRenderScalable() {
    switch (drawType) {
    case ARROW:
      return (connectedAtoms != null);
    case ARC:
    case CIRCLE: 
    case CIRCULARPLANE:
      return true;
    default:
      return haveXyPoints;
    }
  }

}
