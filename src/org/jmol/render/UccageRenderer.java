/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2017-10-28 13:01:20 -0500 (Sat, 28 Oct 2017) $
 * $Revision: 21723 $
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
package org.jmol.render;

//import java.text.NumberFormat;

import javajs.util.DF;
import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.T3;
import javajs.util.T4;

import org.jmol.api.SymmetryInterface;
import org.jmol.script.T;
import org.jmol.shape.Axes;
import org.jmol.shape.Uccage;
import org.jmol.util.BoxInfo;
import org.jmol.util.C;
import org.jmol.util.SimpleUnitCell;
import org.jmol.viewer.JC;
import org.jmol.viewer.StateManager;

public class UccageRenderer extends CageRenderer {

  private final P3[] verticesT = new P3[8]; 

  @Override
  protected void initRenderer() {
    for (int i = 8; --i >= 0; ) 
      verticesT[i] = new P3();
    tickEdges = BoxInfo.uccageTickEdges;    
    draw000 = false;
  }
  
  @Override
  protected boolean render() {
    imageFontScaling = vwr.imageFontScaling;
    font3d = vwr.gdata.getFont3DScaled(((Uccage) shape).font3d, imageFontScaling);
    int mad10 = vwr.getObjectMad10(StateManager.OBJ_UNITCELL);
    if (mad10 == 0 || vwr.isJmolDataFrame() || tm.isNavigating()
        && vwr.getBoolean(T.navigationperiodic))
      return false;
    colix = vwr.getObjectColix(StateManager.OBJ_UNITCELL);
    boolean needTranslucent = C.renderPass2(colix);
    if (!isExport && needTranslucent != vwr.gdata.isPass2)
      return needTranslucent;
    //doLocalize = vwr.getUseNumberLocalization();
    render1(mad10);
    return false;
  }

  private P3 fset0 = P3.new3(555,555,1);
  private P3 cell0 = new P3();
  private P3 cell1 = new P3();
  private P3 offset = new P3();
  private P3 offsetT = new P3();

  private SymmetryInterface unitcell;

  private int lineheight;

  private int xpos;

  private int ypos;
  
  private void render1(int mad10) {
    g3d.setC(colix);
    unitcell = vwr.getCurrentUnitCell();
    if (unitcell == null)
      return;
    isPolymer = unitcell.isPolymer();
    isSlab = unitcell.isSlab();
    P3[] vertices = unitcell.getUnitCellVerticesNoOffset();
    offset.setT(unitcell.getCartesianOffset());
    offsetT.setT(unitcell.getFractionalOrigin());
    unitcell.toCartesian(offsetT, true);
    offset.sub(offsetT);
    boolean hiddenLines = vwr.getBoolean(T.hiddenlinesdashed);
    T3 fset = unitcell.getUnitCellMultiplier();
    boolean haveMultiple = (fset != null && !fset.equals(fset0));
    if (!haveMultiple)
      fset = fset0;
    int t3w = (fset instanceof T4 ? (int)((T4) fset).w : 0);
    if (fset.x > 100) {
    SimpleUnitCell.ijkToPoint3f((int) fset.x, cell0, 0, t3w);
    SimpleUnitCell.ijkToPoint3f((int) fset.y, cell1, 1, t3w);
    } else {
      cell0.set(0, 0, 0);
      cell1.setT(fset);
    }
    int firstLine, allow0, allow1;
    if (fset.z < 0) {
      cell0.scale(-1 / fset.z);
      cell1.scale(-1 / fset.z);
    }
    float scale = Math.abs(fset.z);
    Axes axes = (Axes) vwr.shm.getShape(JC.SHAPE_AXES);
    if (axes != null && vwr.areAxesTainted())
      axes.reinitShape();
    P3[] axisPoints = (axes == null
        || vwr.getObjectMad10(StateManager.OBJ_AXIS1) == 0 || axes.axisXY.z != 0
        || axes.fixedOrigin != null || axes.fixedOriginUC.lengthSquared() > 0 ? null
        : axes.axisPoints);
    boolean drawAllLines = (isExport || vwr.getObjectMad10(StateManager.OBJ_AXIS1) == 0
        || vwr.getFloat(T.axesscale) < 2 || axisPoints == null);
    P3[] aPoints = axisPoints;
    int[][] faces = (hiddenLines ? BoxInfo.facePoints : null);
    if (fset.z == 0) {
      offsetT.setT(cell0);
      unitcell.toCartesian(offsetT, true);
      offsetT.add(offset);
      aPoints = (cell0.x == 0 && cell0.y == 0 && cell0.z == 0 ? axisPoints
          : null);
      firstLine = 0;
      allow0 = 0xFF;
      allow1 = 0xFF;
      P3[] pts = BoxInfo.unitCubePoints;
      for (int i = 8; --i >= 0;) {
        P3 v = P3.new3(pts[i].x * (cell1.x - cell0.x), pts[i].y
            * (cell1.y - cell0.y), pts[i].z * (cell1.z - cell0.z));
        unitcell.toCartesian(v, true);
        verticesT[i].add2(v, offsetT);
      }
      renderCage(mad10, verticesT, faces, aPoints, firstLine, allow0, allow1, 1);
    } else
      for (int x = (int) cell0.x; x < cell1.x; x++) {
        for (int y = (int) cell0.y; y < cell1.y; y++) {
          for (int z = (int) cell0.z; z < cell1.z; z++) {
            if (haveMultiple) {
              offsetT.set(x, y, z);
              offsetT.scale(scale);
              unitcell.toCartesian(offsetT, true);
              offsetT.add(offset);
              aPoints = (x == 0 && y == 0 && z == 0 ? axisPoints : null);
              firstLine = (drawAllLines || aPoints == null ? 0 : 3);
            } else {
              offsetT.setT(offset);
              firstLine = (drawAllLines ? 0 : 3);
            }
            allow0 = 0xFF;
            allow1 = 0xFF;
            for (int i = 8; --i >= 0;)
              verticesT[i].add2(vertices[i], offsetT);
            renderCage(mad10, verticesT, faces, aPoints, firstLine, allow0,
                allow1, scale);
          }
        }
      }
    renderInfo();
  }
  
  private void renderInfo() {
    boolean showDetails = vwr.getBoolean(T.showunitcelldetails);
    if (isExport || !vwr.getBoolean(T.displaycellparameters)
        || vwr.isPreviewOnly || !vwr.gdata.setC(vwr.cm.colixBackgroundContrast)
        || vwr.gdata.getTextPosition() != 0) // molecularOrbital has displayed
      return;
    vwr.gdata.setFontBold("Monospaced", 14 * imageFontScaling);
    xpos = (int) Math.floor(10 * imageFontScaling);
    ypos = lineheight = (int) Math.floor(15 * imageFontScaling);

    if (!unitcell.isSimple()) {
      String sgName = (isPolymer ? "polymer"
          : isSlab ? "slab" : unitcell.getSpaceGroupName());
      if (sgName != null) {
        if (sgName.startsWith("cell=!"))
          sgName = "cell=inverse[" + sgName.substring(6) + "]";
        sgName = PT.rep(sgName, ";0,0,0", "");
        if (sgName.indexOf("#") < 0) {
          String intTab = unitcell.getIntTableNumber();
          if (intTab != null)
            sgName += " #" + intTab;
        }
        if (!sgName.equals("-- [--]")) {
          drawInfo(sgName, 0, null);
        }
      }
      Lst<String> info = unitcell.getMoreInfo();
      if (info != null)
        for (int i = 0; i < info.size(); i++)
          drawInfo(info.get(i), 0, null);
      if (!showDetails)
        return;
    }
    drawInfo("a=", SimpleUnitCell.INFO_A, "\u00C5");
    if (!isPolymer)
      drawInfo("b=", SimpleUnitCell.INFO_B, "\u00C5");
    if (!isPolymer && !isSlab)
      drawInfo("c=", SimpleUnitCell.INFO_C, "\u00C5");
    if (!isPolymer) {
      if (!isSlab) {
        drawInfo("\u03B1=", SimpleUnitCell.INFO_ALPHA, "\u00B0");
        drawInfo("\u03B2=", SimpleUnitCell.INFO_BETA, "\u00B0");
      }
      drawInfo("\u03B3=", SimpleUnitCell.INFO_GAMMA, "\u00B0");
    }
  }

  private void drawInfo(String s, int type, String post) {
    ypos += lineheight;
    if (post != null)
      s += DF.formatDecimal(unitcell.getUnitCellInfoType(type), 3) + post;
    g3d.drawStringNoSlab(s, null, xpos, ypos, 0, (short) 0);
  }

}

