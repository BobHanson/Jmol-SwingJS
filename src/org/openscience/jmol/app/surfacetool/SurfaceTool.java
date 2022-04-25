/* $RCSfile$
 * $J. Gutow$
 * $July 22, 2011$
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.openscience.jmol.app.surfacetool;

import java.util.ArrayList;
import java.util.List;


import javax.swing.JOptionPane;

import org.jmol.api.JmolViewer;
import org.jmol.i18n.GT;
import org.jmol.script.T;
import org.jmol.shape.Mesh;
import org.jmol.shape.MeshCollection;
import org.jmol.shape.Shape;
import org.jmol.util.BoxInfo;
import org.jmol.util.Escape;

import javajs.util.SB;
import javajs.util.P3d;
import javajs.util.P4d;
import javajs.util.V3d;
import org.jmol.viewer.JC;
import org.openscience.jmol.app.HistoryFile;

/**
 * 
 */
public class SurfaceTool {

  private SurfaceToolGUI gui;
  boolean useGUI;
  protected JmolViewer vwr;
  private final P3d negCorner = new P3d();
  private final P3d posCorner = new P3d();
  private final P3d center = new P3d();
  private final V3d boxVec = new V3d();
  //surface specific parameters
  private final List<SurfaceStatus> surfaces = new  ArrayList<SurfaceStatus>();

  final static int DEGREES = 0;
  final static int RADIANS = 1;
  final static int GRADIANS = 2;
  final static int CIRCLE_FRACTION = 3;
  final static int UNITS_PI = 4;
  private int angleUnits = DEGREES;
  //Note order of following list must match above.
  private String[] angleUnitsList = { GT.$("Degrees"), GT.$("Radians"),
      GT.$("Gradians"), GT.$("Circle Fraction"), GT.$("Units of Pi") };

  public SurfaceTool(JmolViewer vwr, HistoryFile hfile, String winName,
      boolean useGUI) {
    this.vwr = vwr;
    this.useGUI = useGUI;
    //initialization must occur before a new gui
    //initialize to match the boundbox
    updateSurfaceInfo();
    chooseBestBoundBox();
    setSurfaceToolParam();
    initSlice();
    gui = (useGUI ? new SurfaceToolGUI(vwr, hfile, winName, this) : null);
  }

  public void toFront() {
    gui.toFront();
  }

  void toFrontOrGotFocus() {
    updateSurfaceInfo();
    chooseBestBoundBox();
    setSurfaceToolParam();
  }

  private void chooseBestBoundBox() {
    //need to set the boundbox to the smallest one that surrounds all the
    //objects that could be sliced.
    //select all atoms and molecules to start as first guess.  Want initialization
    //added to the script so do with call to script

    BoxInfo box = new BoxInfo();
    vwr.calcAtomsMinMax(null, box);
    center.setT(box.getBoundBoxCenter());
    boxVec.setT(box.getBoundBoxCornerVector());
    posCorner.add2(center, boxVec);
    negCorner.sub2(center, boxVec);
    Shape[] shapes = (Shape[]) vwr.getProperty("DATA_API", "shapeManager",
        "getShapes");
    //now iterate through all the shapes and get their XYZmin and XYZmax.  Expand
    //Boundbox used by SurfaceTool to encompass these.
    box = checkMeshBB(shapes, JC.SHAPE_ISOSURFACE, box);
    box = checkMeshBB(shapes, JC.SHAPE_PMESH, box);
    box = checkMeshBB(shapes, JC.SHAPE_MO, box);
    if (box != null) {
      center.setT(box.getBoundBoxCenter());
      negCorner.sub2(center, box.getBoundBoxCornerVector());
      posCorner.add2(center, box.getBoundBoxCornerVector());
      boxVec.setT(box.getBoundBoxCornerVector());
    }
  }

  BoxInfo checkMeshBB(Shape[] shapes, int kind, BoxInfo box) {
    MeshCollection mc = (MeshCollection) shapes[kind];
    if (mc == null)
      return box;
    for (int i = 0; i < mc.meshCount; i++) {
      Mesh m = mc.meshes[i];
      if (!m.isValid || m.vc == 0 && m.pc == 0)
        continue;
      if (m.thisID.equalsIgnoreCase("_slicerleft")
          || m.thisID.equalsIgnoreCase("_slicerright"))
        continue;
      P3d[] bb = m.getBoundingBox();
      if (bb == null)
        continue;
      box.addBoundBoxPoint(bb[0]);
      box.addBoundBoxPoint(bb[1]);
    }
    return box;
  }

  void setSurfaceToolParam() {
    //TODO should get stored parameters from History file upon initialization
    // probably belongs in another routine called only on start up.
    thicknessMax = 2 * boxVec.length();
    double delta = position - positionMin;
    if (useMolecular) {
      //set positionMin to minimum of BBoxCornerMin.x .y or .z or if all are 
      //negative -1* distance from origin. PositionMax similarly.
      if (negCorner.x < 0 && negCorner.y < 0 && negCorner.z < 0) {
        positionMin = -1 * negCorner.length();
      } else {
        positionMin = Math.min(negCorner.x, negCorner.y);
        positionMin = Math.min(negCorner.z, positionMin);
      }
    } else {
      positionMin = -1 * (boxVec.length());
    }
    position = positionMin + delta;
  }

  private void updateSurfaceInfo() {
    Shape[] shapes = (Shape[]) vwr.getProperty("DATA_API", "shapeManager",
        "getShapes");
    setSyncStarting();
    updateMeshInfo(shapes, JC.SHAPE_ISOSURFACE);
    updateMeshInfo(shapes, JC.SHAPE_PMESH);
    updateMeshInfo(shapes, JC.SHAPE_MO);
    syncDone();
  }

  private void setSyncStarting() {
    for (int i = 0; i < surfaces.size(); i++)
      surfaces.get(i).foundDuringLastSync = false;
  }

  private void syncDone() {
    //delete any surfaces that were not found
    for (int i = (surfaces.size() - 1); i >= 0; i--) {
      if (!surfaces.get(i).foundDuringLastSync)
        surfaces.remove(i);
    }
  }

  private void updateMeshInfo(Shape[] shapes, int kind) {
    if (shapes != null) {
      MeshCollection mc = (MeshCollection) shapes[kind];
      if (mc != null) {
        //check all the meshes
        int[] meshIndexList = new int[mc.meshCount];
        for (int i = 0; i < mc.meshCount; i++)
          meshIndexList[i] = -1;
        if (!surfaces.isEmpty()) {
          int[] surfaceIndexList = new int[surfaces.size()];
          for (int i = 0; i < surfaces.size(); i++)
            surfaceIndexList[i] = -1;
          for (int i = 0; i < mc.meshCount; i++) {
            Mesh m = mc.meshes[i];
            if (!checkMesh(m)) {
              meshIndexList[i] = -2;
            } else {
              //scan id's and make list of which match which mesh...
              for (int j = 0; j < surfaces.size(); j++) {
                if (surfaces.get(j).id == m.thisID) {
                  surfaceIndexList[j] = i;
                  meshIndexList[i] = j;
                }
              }
            }
          }
          //Now use indices to update things...
          for (int i = 0; i < surfaceIndexList.length; i++) {
            if (surfaceIndexList[i] >= 0) {
              surfaces.get(i).updateExisting(mc.meshes[surfaceIndexList[i]]);
            }
          }
        } else {
          for (int i = 0; i < mc.meshCount; i++) {
            Mesh m = mc.meshes[i];
            if (!checkMesh(m)) {
              meshIndexList[i] = -2;
            } else {
              meshIndexList[i] = -1;
            }
          }
        }
        for (int i = 0; i < meshIndexList.length; i++) {
          if (meshIndexList[i] == -1)
            surfaces.add(new SurfaceStatus(mc.meshes[i], kind));
        }
      }
    }
  }

  private boolean checkMesh(Mesh m) {
    if (!m.isValid || m.vc == 0 && m.pc == 0) {
      return false;
    }
    if (m.thisID.equalsIgnoreCase("_slicerleft")
        || m.thisID.equalsIgnoreCase("_slicerright")) {
      return false;
    }
    return true;
  }

  void setAngleUnits(int units) {
    angleUnits = units;
  }

  P3d getNegCorner() {
    return negCorner;
  }

  P3d getPosCorner() {
    return posCorner;
  }

  /* Slicer section Begins
   * 
   */
  private double angleXY;
  private double anglefromZ;
  private double positionMin;
  private double position;
  private double thickness;
  private double thicknessMax;
  private Slice slice = new Slice();

  private boolean leftOn = false;
  private boolean rightOn = false;
  private boolean ghostOn = false;
  private boolean capOn = false;
  private boolean useMolecular = false;
  private boolean usePercent = false;

  private void initSlice() {
    //set to middle and full width
    angleXY = 0;
    anglefromZ = (double) (Math.PI / 2);
    position = 0;
    thickness = negCorner.distance(posCorner) / 5;
    slice.setSlice(angleXY, anglefromZ, position, thickness, center, boxVec,
        useMolecular);
  }

  void showSliceBoundaryPlanes(boolean onOrOff) {
    leftOn = rightOn = onOrOff;
    SB cmd = new SB();
    drawSlicePlane(cmd, T.left, onOrOff);
    drawSlicePlane(cmd, T.right, onOrOff);
    vwr.evalStringQuiet(cmd.toString());
  }

  /**
   * Defines a slice within which isosurfaces (and in future? atoms) are
   * displayed.
   * 
   * @param angleXY
   *        (double)angle in radians from X-axis to projection in XY plane
   * @param anglefromZ
   *        (double)angle in radians from z-axis to vector
   * @param position
   *        (double) position along direction vector in absolute units
   * @param thickness
   *        (double) thickness of slice in absolute units
   */
  void setSlice(double angleXY, double anglefromZ, double position, double thickness) {
    if (usePercent) {//convert to absolute units
      //TODO
      JOptionPane.showMessageDialog(null,
          GT.$("Percentage scaling not implemented yet!"), "Warning",
          javax.swing.JOptionPane.WARNING_MESSAGE);
    }
    this.angleXY = angleXY;
    this.anglefromZ = anglefromZ;
    this.position = position;
    this.thickness = thickness;
    slice.setSlice(angleXY, anglefromZ, position, thickness, center, boxVec,
        useMolecular);
  }

  /**
   * 
   * @param angle
   *        (double) angle from X-axis of projection on XY plane in radians.
   */
  void setSliceAngleXY(double angle) {
    if (angleXY != angle) {
      angleXY = angle;
      slice.setSlice(angleXY, anglefromZ, position, thickness, center, boxVec,
          useMolecular);
    }
  }

  double getSliceAngleXY() {
    return (angleXY);
  }

  /**
   * 
   * @param angle
   *        (double) angle of vector from Z axis in radians.
   */
  void setSliceAnglefromZ(double angle) {
    if (anglefromZ != angle) {
      anglefromZ = angle;
      slice.setSlice(angleXY, anglefromZ, position, thickness, center, boxVec,
          useMolecular);
    }
  }

  double getAnglefromZ() {
    return (anglefromZ);
  }

  /**
   * 
   * @param where
   *        (double) position of slice center along direction vector.
   */
  void setSlicePosition(double where) {
    if (usePercent) {//convert to absolute units
      //TODO
      JOptionPane.showMessageDialog(null,
          GT.$("Percentage scaling not implemented yet!"), "Warning",
          javax.swing.JOptionPane.WARNING_MESSAGE);
    }
    if (position != where) {
      position = where;
      slice.setSlice(angleXY, anglefromZ, position, thickness, center, boxVec,
          useMolecular);
    }
  }

  double getSlicePosition() {
    return (position);
  }

  /**
   * 
   * @param width
   *        (double) thickness of slice.
   */
  void setSliceThickness(double width) {
    if (usePercent) {//convert to absolute units
      //TODO
      JOptionPane.showMessageDialog(null,
          GT.$("Percentage scaling not implemented yet!"), "Warning",
          javax.swing.JOptionPane.WARNING_MESSAGE);
    }
    if (thickness != width) {
      thickness = width;
      slice.setSlice(angleXY, anglefromZ, position, thickness, center, boxVec,
          useMolecular);
    }
  }

  double getSliceThickness() {
    return (thickness);
  }

  void updateSlices() {//slices all surfaces...for testing
    for (int i = 0; i < surfaces.size(); i++) {
      sliceObject(surfaces.get(i).id, surfaces.get(i).kind);
    }
  }

  void sliceObject(String objectName, int kind) {
    //valid surface because it is in the updated list and we check
    //every time the window gains focus to catch changes.
    String cmdStart = "";
    String idStr = " ID \"" + objectName + "\"";
    String slabCapStr = (capOn ? " cap " : " slab ");
    String ghostStr = (ghostOn ? "translucent 0.8 mesh " : "");
    switch (kind) {
    case JC.SHAPE_ISOSURFACE:
      cmdStart = "isosurface";
      break;
    case JC.SHAPE_PMESH:
      cmdStart = "pmesh";
      break;
    case JC.SHAPE_MO:
      cmdStart = "mo";
      idStr = "";//since mo command does not take IDs
      slabCapStr = " slab ";
      break;
    }
    SB cmd = new SB();
    //planes on or off as appropriate
    drawSlicePlane(cmd, T.left, leftOn);
    drawSlicePlane(cmd, T.right, rightOn);
    //now handle the surface
    cmd.append(cmdStart).append(idStr).append(" slab none;");
    cmd.append(cmdStart).append(idStr);
    cmd.append(slabCapStr).append(ghostStr).append("-")
        .append(Escape.eP4(slice.leftPlane));
    cmd.append(";").append(cmdStart).append(idStr);
    cmd.append(slabCapStr).append(ghostStr)
        .append(Escape.eP4(slice.rightPlane));
    cmd.append(";");
    vwr.evalStringQuiet(cmd.toString());
    return;
  }

  private void drawSlicePlane(SB cmd, int side, boolean on) {
    String color;
    String name = T.nameOf(side);
    P4d plane;
    switch (side) {
    default:
    case T.left:
      plane = slice.leftPlane;
      color = "magenta";
      break;
    case T.right:
      plane = slice.rightPlane;
      color = "cyan";
      break;
    }
    cmd.append("isosurface _slicer").append(name);
    if (on) {
      cmd.append(" plane ").append(Escape.eP4(plane))
          .append(" translucent 0.7 ").append(color).append(";");
    } else {
      cmd.append(" off;");
    }
  }

  /**
   * @return (int) possible values: SurfaceTool.RADIANS, SurfaceTool.DEGREES,
   *         SurfaceTool.GRADIANS, SurfaceTool.CIRCLE_FRACTION,
   *         SurfaceTool.UNITS_PI.
   */
  int getAngleUnits() {
    return angleUnits;
  }

  /**
   * @return (boolean) true = ghost showing; false = ghost hiding.
   */
  boolean getGhostOn() {
    return ghostOn;
  }

  /**
   * @param b
   *        (boolean) true for ghost on.
   */
  void setGhostOn(boolean b) {
    ghostOn = b;
  }

  /**
   * @return (boolean) true = using molecular coordinates; false = using
   *         boundbox coordinates.
   */
  boolean getUseMolecular() {
    return useMolecular;
  }

  void setUseMolecular(boolean on) {
    useMolecular = on;
  }

  double getPositionMin() {
    return positionMin;
  }

  double getThicknessMax() {
    return thicknessMax;
  }

  P3d getCenter() {
    return center;
  }

  V3d getBoxVec() {
    return boxVec;
  }

  P4d getSliceMiddle() {
    return slice.getMiddle();
  }

  String[] getAngleUnitsList() {
    return angleUnitsList;
  }

  boolean getCapOn() {
    return capOn;
  }

  void setCapOn(boolean b) {
    capOn = b;
  }

  public List<SurfaceStatus> getSurfaces() {
    return surfaces;
  }
}
