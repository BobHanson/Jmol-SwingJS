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
import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.T3d;
import javajs.util.T4d;

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

  private final P3d[] verticesT = new P3d[8]; 

  @Override
  protected void initRenderer() {
    for (int i = 8; --i >= 0; ) 
      verticesT[i] = new P3d();
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

  private final static P3d fset0 = P3d.new3(555,555,1);
  private final P3d[] cellRange = { new P3d(), new P3d()} ;
  private final P3d offset = new P3d();
  private final P3d offsetT = new P3d();

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
    P3d[] vertices = unitcell.getUnitCellVerticesNoOffset();
    offset.setT(unitcell.getCartesianOffset());
    offsetT.setT(unitcell.getFractionalOrigin());
    unitcell.toCartesian(offsetT, true);
    offset.sub(offsetT);
    boolean hiddenLines = vwr.getBoolean(T.hiddenlinesdashed);
    T3d fset = unitcell.getUnitCellMultiplier();
    boolean haveMultiple = (fset != null && !fset.equals(fset0));
    if (!haveMultiple)
      fset = fset0;
    SimpleUnitCell.getCellRange(fset, cellRange);
    int firstLine, allow0, allow1;
    double scale = Math.abs(fset.z);
    Axes axes = (Axes) vwr.shm.getShape(JC.SHAPE_AXES);
    if (axes != null && vwr.areAxesTainted())
      axes.reinitShape();
    P3d[] axisPoints = (axes == null
        || vwr.getObjectMad10(StateManager.OBJ_AXIS1) == 0 
        || axes.axisXY.z != 0 && (axes.axes2 == null || axes.axes2.length() == 3)
        || axes.fixedOrigin != null || axes.fixedOriginUC.lengthSquared() > 0
            ? null
            : axes.axisPoints);
    boolean drawAllLines = (isExport
        || vwr.getObjectMad10(StateManager.OBJ_AXIS1) == 0
        || vwr.getDouble(T.axesscale) < 2 || axisPoints == null);
    P3d[] aPoints = axisPoints;
    int[][] faces = (hiddenLines ? BoxInfo.facePoints : null);
    if (fset.z == 0) {
      offsetT.setT(cellRange[0]);
      unitcell.toCartesian(offsetT, true);
      offsetT.add(offset);
      aPoints = (cellRange[0].x == 0 && cellRange[0].y == 0 && cellRange[0].z == 0 ? axisPoints
          : null);
      firstLine = 0;
      allow0 = 0xFF;
      allow1 = 0xFF;
      P3d[] pts = BoxInfo.unitCubePoints;
      for (int i = 8; --i >= 0;) {
        P3d v = P3d.new3(pts[i].x * (cellRange[1].x - cellRange[0].x),
            pts[i].y * (cellRange[1].y - cellRange[0].y), pts[i].z * (cellRange[1].z - cellRange[0].z));
        unitcell.toCartesian(v, true);
        verticesT[i].add2(v, offsetT);
      }
      renderCage(mad10, verticesT, faces, aPoints, firstLine, allow0, allow1,
          1);
    } else {
      for (int x = (int) cellRange[0].x; x < cellRange[1].x; x++) {
        for (int y = (int) cellRange[0].y; y < cellRange[1].y; y++) {
          for (int z = (int) cellRange[0].z; z < cellRange[1].z; z++) {
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
      String sgName = unitcell.getUnitCellDisplayName();
      if (sgName != null)
          drawInfo(sgName, 0, null);
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

