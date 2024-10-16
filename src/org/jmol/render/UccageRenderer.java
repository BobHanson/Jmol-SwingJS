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

import org.jmol.api.SymmetryInterface;
import org.jmol.script.T;
import org.jmol.shape.Axes;
import org.jmol.shape.Uccage;
import org.jmol.util.BSUtil;
import org.jmol.util.BoxInfo;
import org.jmol.util.C;
import org.jmol.util.SimpleUnitCell;
import org.jmol.viewer.JC;
import org.jmol.viewer.StateManager;

import javajs.util.BS;

//import java.text.NumberFormat;

import javajs.util.DF;
import javajs.util.Lst;
import javajs.util.P3d;
import javajs.util.T3d;

public class UccageRenderer extends CageRenderer {

  private final P3d[] verticesT = new P3d[8];

  private P3d[] vvertA;

  @Override
  protected void initRenderer() {
    for (int i = 8; --i >= 0; ) 
      verticesT[i] = new P3d();
    tickEdges = BoxInfo.uccageTickEdges;    
    draw000 = false;
  }
  
  @Override
  protected void setPeriodicity(P3d[] vertices, double scale) {
    setShifts();
    P3d p0;
    if (shifting) {
      // layer group shifts axes and unit cell down 50%
      if (vvert == null) { //v[1] is c, v[2,3] is b, v[4,5] is a 
        vvert = new P3d[8];
        for (int i = 8; --i >= 0;) {
          vvert[i] = new P3d();
        }
      }
      
      if (shiftC && periodicity == 0x1) {
        if (vvertA == null) { //v[1] is c, v[2,3] is b, v[4,5] is a 
          vvertA = new P3d[8];
          for (int i = 8; --i >= 0;) {
            vvertA[i] = new P3d();
          }
        }
        p0 = P3d.newP(vertices[1]);
        p0.sub(vertices[0]);
        p0.scale(0.5);
        for (int i = 8; --i >= 0;) {
          pt.setT(vertices[i]);
          pt.sub(vertices[0]);
          pt.scaleAdd2(scale, pt, vertices[0]);
          pt.sub(p0);
          tm.transformPtNoClip(pt, vvertA[i]);
        }
      } else if (shiftA) {
        if (vvertA == null) { //v[1] is c, v[2,3] is b, v[4,5] is a 
          vvertA = new P3d[8];
          for (int i = 8; --i >= 0;) {
            vvertA[i] = new P3d();
          }
        }
        p0 = P3d.newP(vertices[4]);
        p0.sub(vertices[0]);
        p0.scale(0.5);
        for (int i = 8; --i >= 0;) {
          pt.setT(vertices[i]);
          pt.sub(vertices[0]);
          pt.scaleAdd2(scale, pt, vertices[0]);
          pt.sub(p0);
          tm.transformPtNoClip(pt, vvertA[i]);
        }
      } else {
        vvertA = null;
      }
      p0 = P3d.newP(vertices[shiftB ? 2 : 1]);
      p0.sub(vertices[0]);
      p0.scale(0.5);
      for (int i = 8; --i >= 0;) {
        pt.setT(vertices[i]);
        pt.sub(vertices[0]);
        pt.scaleAdd2(scale, pt, vertices[0]);
        pt.sub(p0);
        tm.transformPtNoClip(pt, vvert[i]);
      }
    }
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
    periodicity = unitcell.getPeriodicity();
    nDims = unitcell.getDimensionality();
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
  
  
  @Override
  protected void renderCageLine(int i, int edge0, int edge1, int d, boolean drawTicks) {
    P3d p1;
    P3d p2;
    if (bsVerticals != null && bsVerticals.get(i)) {
      if (vvertA != null && (i == 4 || i == 8 || i == 12 || i == 16 
          || i == 0 && periodicity != 0x2)) {
        p1 = vvertA[edge0];
        p2 = vvertA[edge1];
      } else {
        p1 = vvert[edge0];
        p2 = vvert[edge1];
      }
      
    } else {
      p1 = p3Screens[edge0];
      p2 = p3Screens[edge1];
    }
    renderLine(p1, p2, d, drawTicks);
  }
  
  private BS bsVerticals;

  @Override
  protected void setBSPeriod() {
    BS bs;
    if (bsVerticals != null)
      bsVerticals.clearAll();
    if (bsPeriod != null)
      bsPeriod.clearAll();
    switch (periodicity) {
    case 0x7:
      return;
    case 0x3:
      // plane, layer ab(c)
      bs = (bsPeriod == null ? (bsPeriod = new BS()) : bsPeriod);
      if (nDims == 3) {
        // verticals for four layer c-posts, all set the same direction
       bs.set(0);
       bs.set(10);
       bs.set(16);
       bs.set(22);
       if (bsVerticals == null) {
         bsVerticals = new BS();
       }
       BSUtil.copy2(bs, bsVerticals);
      }
      // horizontals
      bs.set(2);
      bs.set(4);
      bs.set(12);
      bs.set(18);
      break;
    case 0x4:
      // rod
      bs = (bsPeriod == null ? (bsPeriod = new BS()) : bsPeriod);
      bs.set(2); // these offset in b
      bs.set(6);
      bs.set(4); // these two offset in a
      bs.set(8);
      if (bsVerticals == null) {
        bsVerticals = new BS();
      }
      BSUtil.copy2(bs, bsVerticals);
      // c axis
      bs.set(0);
      break;
    case 0x1:
      // rod-a, frieze
      bs = (bsPeriod == null ? (bsPeriod = new BS()) : bsPeriod);
      bs.set(2);
      bs.set(18);
      if (nDims == 3) {
        //rod-a
        bs.set(0);
        bs.set(16);
      }
      if (bsVerticals == null) {
        bsVerticals = new BS();
      }
      BSUtil.copy2(bs, bsVerticals);
      bs.set(4);
      break;
    case 0x2:
      // rod-b
      bs = (bsPeriod == null ? (bsPeriod = new BS()) : bsPeriod);
      bs.set(0); 
      bs.set(4);
      bs.set(12); //"A" shift 
      bs.set(10);
      //bs.set(16);
      if (bsVerticals == null) {
        bsVerticals = new BS();
      }
      BSUtil.copy2(bs, bsVerticals);
      bs.set(2);      
      break;
    }
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

