/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-02-15 07:31:37 -0600 (Mon, 15 Feb 2010) $
 * $Revision: 12396 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development
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
package org.jmol.viewer;


import java.util.Map;


import org.jmol.api.Interface;
import org.jmol.atomdata.RadiusData;
import org.jmol.c.PAL;
import org.jmol.c.VDW;
import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Group;
import org.jmol.modelset.ModelSet;
import org.jmol.script.T;
import org.jmol.shape.Shape;
import org.jmol.util.BSUtil;
import org.jmol.util.GData;
import org.jmol.util.Edge;

import org.jmol.util.JmolMolecule;

import javajs.util.M4;
import javajs.util.P3;
import javajs.util.P3i;
import org.jmol.util.Vibration;

public class ShapeManager {

  private ModelSet ms;
  public Shape[] shapes;
  public Viewer vwr;

  public ShapeManager(Viewer vwr) {
    this.vwr = vwr;
    bsRenderableAtoms = new BS();
    bsSlabbedInternal = new BS();
  }

  /**
   * @j2sIgnore
   * 
   */
  public void setParallel() {
    resetShapes();
    loadDefaultShapes(vwr.ms);
  }
  

  // public methods 
  
  public void findNearestShapeAtomIndex(int x, int y, Atom[] closest, BS bsNot) {
    if (shapes != null)
      for (int i = 0; i < shapes.length && closest[0] == null; ++i)
        if (shapes[i] != null)
          shapes[i].findNearestAtomIndex(x, y, closest, bsNot);
  }

  public Object getShapePropertyIndex(int shapeID, String propertyName, int index) {
    if (shapes == null || shapes[shapeID] == null)
      return null;
    vwr.setShapeErrorState(shapeID, "get " + propertyName);
    Object result = shapes[shapeID].getProperty(propertyName, index);
    vwr.setShapeErrorState(-1, null);
    return result;
  }

  public boolean getShapePropertyData(int shapeID, String propertyName, Object[] data) {
    if (shapes == null || shapes[shapeID] == null)
      return false;
    vwr.setShapeErrorState(shapeID, "get " + propertyName);
    // echo, dipoles, draw, ellipsoids, isosurface
    boolean result = shapes[shapeID].getPropertyData(propertyName, data);
    vwr.setShapeErrorState(-1, null);
    return result;
  }

  /**
   * Returns the shape type index for a shape object given the object name.
   * @param objectName (string) string name of object
   * @return shapeType (int) integer corresponding to the shape type index
   *                   see ShapeManager.shapes[].
   */
  public int getShapeIdFromObjectName(String objectName) {
    if (shapes != null)
      for (int i = JC.SHAPE_MIN_SPECIAL; i < JC.SHAPE_MAX_MESH_COLLECTION; ++i)
        if (shapes[i] != null && shapes[i].getIndexFromName(objectName) >= 0)
          return i;
    return -1;
  }

  public void loadDefaultShapes(ModelSet newModelSet) {
    ms = newModelSet;
    if (shapes != null)
      for (int i = 0; i < shapes.length; ++i)
        if (shapes[i] != null)
          shapes[i].setModelSet(newModelSet);
    loadShape(JC.SHAPE_BALLS);
    loadShape(JC.SHAPE_STICKS);
  }

  public Shape loadShape(int shapeID) {
    if (shapes == null)
      return null;
    if (shapes[shapeID] != null)
      return shapes[shapeID];
    if (shapeID == JC.SHAPE_HSTICKS || shapeID == JC.SHAPE_SSSTICKS
        || shapeID == JC.SHAPE_STRUTS)
      return null;
    String className = JC.getShapeClassName(shapeID, false);
    Shape shape;
    if ((shape = (Shape) Interface.getInterface(className, vwr, "shape")) == null)
      return null;
    vwr.setShapeErrorState(shapeID, "allocate");
    shape.initializeShape(vwr, ms, shapeID);
    vwr.setShapeErrorState(-1, null);
    return shapes[shapeID] = shape;
  }

  public void notifyAtomPositionsChanged(int baseModel, BS bs, M4 mat) {
    Integer Imodel = Integer.valueOf(baseModel);
    BS bsModelAtoms = vwr.getModelUndeletedAtomsBitSet(baseModel);
    for (int i = 0; i < JC.SHAPE_MAX; i++)
      if (shapes[i] != null)
        setShapePropertyBs(i, "refreshTrajectories", new Object[] { Imodel, bs, mat }, bsModelAtoms);    
  }

  public void releaseShape(int shapeID) {
    if (shapes != null) 
      shapes[shapeID] = null;  
  }
  
  public void resetShapes() {
//    if (!vwr.noGraphicsAllowed)  ?? Why this?? We need shapes!
      shapes = new Shape[JC.SHAPE_MAX];
  }
  
  /**
   * @param shapeID
   * @param size in milliangstroms
   * @param rd
   * @param bsSelected
   */
  public void setShapeSizeBs(int shapeID, int size, RadiusData rd, BS bsSelected) {
    if (shapes == null)
      return;
    if (bsSelected == null && 
        (shapeID != JC.SHAPE_STICKS || size != Integer.MAX_VALUE))
      bsSelected = vwr.bsA();
    if (rd != null && rd.value != 0 && rd.vdwType == VDW.TEMP)
      ms.getBfactor100Lo();
    vwr.setShapeErrorState(shapeID, "set size");
    if (rd == null ? size != 0 : rd.value != 0)
      loadShape(shapeID);
    if (shapes[shapeID] != null) {
      shapes[shapeID].setShapeSizeRD(size, rd, bsSelected);
    }
    vwr.setShapeErrorState(-1, null);
  }

  public void setLabel(Object strLabel, BS bsSelection) {
    if (strLabel == null) {
      if (shapes[JC.SHAPE_LABELS] == null)
        return;
    } else {// force the class to load and display
      loadShape(JC.SHAPE_LABELS);
      setShapeSizeBs(JC.SHAPE_LABELS, 0, null, bsSelection);
    }
    setShapePropertyBs(JC.SHAPE_LABELS, "label", strLabel, bsSelection);
  }

  public void setShapePropertyBs(int shapeID, String propertyName, Object value,
                               BS bsSelected) {
    if (shapes == null || shapes[shapeID] == null)
      return;
    if (bsSelected == null)
      bsSelected = vwr.bsA();
    vwr.setShapeErrorState(shapeID, "set " + propertyName);
    shapes[shapeID].setProperty(propertyName.intern(), value, bsSelected);
    vwr.setShapeErrorState(-1, null);
  }

  // methods local to Viewer and other managers
  
  boolean checkFrankclicked(int x, int y) {
    Shape frankShape = shapes[JC.SHAPE_FRANK];
    return (frankShape != null && frankShape.wasClicked(x, y));
  }

  private final static int[] hoverable = {
    JC.SHAPE_ECHO, 
    JC.SHAPE_CONTACT,
    JC.SHAPE_ISOSURFACE,
    JC.SHAPE_DRAW,
    JC.SHAPE_FRANK,
  };
  
  private static int clickableMax = hoverable.length - 1;
  
  Map<String, Object> checkObjectClicked(int x, int y, int modifiers,
                                         BS bsVisible, boolean drawPicking) {
    Shape shape;
    Map<String, Object> map = null;
    if (vwr.getPickingMode() == ActionManager.PICKING_LABEL) {
      return shapes[JC.SHAPE_LABELS].checkObjectClicked(x, y, modifiers,
          bsVisible, false);
    }
    if (modifiers != 0
        && vwr.getBondsPickable()
        && (map = shapes[JC.SHAPE_STICKS].checkObjectClicked(x, y, modifiers,
            bsVisible, false)) != null)
      return map;
    for (int i = 0; i < clickableMax; i++)
      if ((shape = shapes[hoverable[i]]) != null
          && (map = shape.checkObjectClicked(x, y, modifiers, bsVisible,
              drawPicking)) != null)
        return map;
    return null;
  }
 
  boolean checkObjectDragged(int prevX, int prevY, int x, int y, int modifiers,
                             BS bsVisible, int iShape) {
    boolean found = false;
    int n = (iShape > 0 ? iShape + 1 : JC.SHAPE_MAX);
    for (int i = iShape; !found && i < n; ++i)
      if (shapes[i] != null)
        found = shapes[i].checkObjectDragged(prevX, prevY, x, y, modifiers,
            bsVisible);
    return found;
  }

  boolean checkObjectHovered(int x, int y, BS bsVisible, boolean checkBonds) {
    Shape shape = shapes[JC.SHAPE_STICKS];
    if (checkBonds && shape != null
        && shape.checkObjectHovered(x, y, bsVisible))
      return true;
    for (int i = 0; i < hoverable.length; i++) {
      shape = shapes[hoverable[i]];
      if (shape != null && shape.checkObjectHovered(x, y, bsVisible))
        return true;
    }
    return false;
  }

  public void deleteShapeAtoms(Object[] value, BS bs) {
    if (shapes != null)
      for (int j = 0; j < JC.SHAPE_MAX; j++)
        if (shapes[j] != null)
          setShapePropertyBs(j, "deleteModelAtoms", value, bs);
  }

  void deleteVdwDependentShapes(BS bs) {
    if (bs == null)
      bs = vwr.bsA();
    if (shapes[JC.SHAPE_ISOSURFACE] != null)
      shapes[JC.SHAPE_ISOSURFACE].setProperty("deleteVdw", null, bs);
    if (shapes[JC.SHAPE_CONTACT] != null)
      shapes[JC.SHAPE_CONTACT].setProperty("deleteVdw", null, bs);
  }
  
  public float getAtomShapeValue(int tok, Group group, int atomIndex) {
    int iShape = JC.shapeTokenIndex(tok);
    if (iShape < 0 || shapes[iShape] == null) 
      return 0;
    int mad = shapes[iShape].getSize(atomIndex);
    if (mad == 0) {
      if ((group.shapeVisibilityFlags & shapes[iShape].vf) == 0)
        return 0;
      mad = shapes[iShape].getSizeG(group);
    }
    return mad / 2000f;
  }

  public void replaceGroup(Group g0, Group g1) {
    if (shapes == null)
      return;
    for (int i = JC.SHAPE_MIN_SECONDARY; i < JC.SHAPE_MAX_SECONDARY; i++)
      if (shapes[i] != null)
        shapes[i].replaceGroup(g0, g1);
  }

  void getObjectMap(Map<String, ?> map, boolean withDollar) {
    if (shapes == null)
      return;
    Boolean bDollar = Boolean.valueOf(withDollar);
    for (int i = JC.SHAPE_MIN_SPECIAL; i < JC.SHAPE_MAX_MESH_COLLECTION; ++i)
      getShapePropertyData(i, "getNames", new Object[] { map, bDollar });
  }

  Object getProperty(Object paramInfo) {
    if (paramInfo.equals("getShapes"))
      return shapes;
    // could be more here...
    return null;
  }

  public final BS bsRenderableAtoms, bsSlabbedInternal;

  public Shape getShape(int i) {
    //RepaintManager
    return (shapes == null ? null : shapes[i]);
  }
  
  public void resetBioshapes(BS bsAllAtoms) {
    if (shapes == null)
      return;
    for (int i = 0; i < shapes.length; ++i)
      if (shapes[i] != null && shapes[i].isBioShape) {
        shapes[i].setModelSet(ms);
        shapes[i].setShapeSizeRD(0, null, bsAllAtoms);
        shapes[i].setProperty("color", PAL.NONE, bsAllAtoms);
      }
  }

  public void setAtomLabel(String strLabel, int i) {
    if (shapes != null)
      shapes[JC.SHAPE_LABELS].setProperty("label:"+strLabel, Integer.valueOf(i), null);
  }
  
  /**
   * Sets shape visibility flags, including ATOM_VIS_INFRAME and
   * ATOM_VIS_NOTHIDDEN.
   * 
   */
  void setModelVisibility() {
    Shape[] shapes = this.shapes;
    if (shapes == null || shapes[JC.SHAPE_BALLS] == null)
      return;

    //named objects must be set individually
    //in the future, we might include here a BITSET of models rather than just a modelIndex

    // all these isTranslucent = f() || isTranslucent are that way because
    // in general f() does MORE than just check translucency. 
    // so isTranslucent = isTranslucent || f() would NOT work.

    BS bs = vwr.getVisibleFramesBitSet();

    // i=2 skips balls and sticks
    // as these are handled differently.

    // Bbcage, Halos, Dipoles, Draw, Ellipsoids, Polyhedra
    // bioshapes, Echo, Hover, 
    for (int i = JC.SHAPE_MIN_HAS_SETVIS; i < JC.SHAPE_MAX_HAS_SETVIS; i++)
      if (shapes[i] != null)
        shapes[i].setModelVisibilityFlags(bs);

    // now check ATOM_IN_FRAME, and ATOM_NOTHIDDEN, VIS_BALLS_FLAG 

    //    todo: deleted atoms not showing up in state
    boolean showHydrogens = vwr.getBoolean(T.showhydrogens);
    BS bsDeleted = vwr.slm.bsDeleted;
    Atom[] atoms = ms.at;
    ms.clearVisibleSets();
    if (atoms.length > 0) {
      for (int i = ms.ac; --i >= 0;) {
        Atom atom = atoms[i];
        atom.shapeVisibilityFlags &= Atom.ATOM_NOFLAGS;
        if (bsDeleted != null && bsDeleted.get(i))
          continue;
        if (bs.get(atom.mi)) {
          int f = Atom.ATOM_INFRAME;
          if (!ms.isAtomHidden(i)
              && (showHydrogens || atom.getElementNumber() != 1)) {
            f |= Atom.ATOM_NOTHIDDEN;
            if (atom.madAtom != 0)
              f |= JC.VIS_BALLS_FLAG;
            atom.setShapeVisibility(f, true);
          }
        }
      }
    }
    setShapeVis();
  }

  private void setShapeVis() {
    //set clickability -- this enables measures and such
    for (int i = 0; i < JC.SHAPE_MAX; ++i) {
      Shape shape = shapes[i];
      if (shape != null)
        shape.setAtomClickability();
    }
  }

  private final int[] navMinMax = new int[4];

  public int[] finalizeAtoms(BS bsTranslateSelected, boolean finalizeParams) {
    Viewer vwr = this.vwr;
    TransformManager tm = vwr.tm;
    if (finalizeParams)
      vwr.finalizeTransformParameters();
    if (bsTranslateSelected != null) {
      // translateSelected operation
      P3 ptCenter = ms.getAtomSetCenter(bsTranslateSelected);
      P3 pt = new P3();
      tm.transformPt3f(ptCenter, pt);
      pt.add(tm.ptOffset);
      tm.unTransformPoint(pt, pt);
      pt.sub(ptCenter);
      vwr.setAtomCoordsRelative(pt, bsTranslateSelected);
      tm.ptOffset.set(0, 0, 0);
      tm.bsSelectedAtoms = null;
    }
    BS bsOK = bsRenderableAtoms;
    ms.getAtomsInFrame(bsOK);
    Vibration[] vibrationVectors = ms.vibrations;
    boolean vibs = (vibrationVectors != null && tm.vibrationOn);
    boolean checkOccupancy = (ms.bsModulated != null && ms.occupancies != null);
    Atom[] atoms = ms.at;
    int occ;
    boolean haveMods = false;
    BS bsSlabbed = bsSlabbedInternal;
    bsSlabbed.clearAll();
    for (int i = bsOK.nextSetBit(0); i >= 0; i = bsOK.nextSetBit(i + 1)) {
      // note that this vibration business is not compatible with
      // PDB objects such as cartoons and traces, which 
      // use Cartesian coordinates, not screen coordinates
      Atom atom = atoms[i];
      P3i screen = (vibs && atom.hasVibration() ? tm.transformPtVib(atom,
          vibrationVectors[i]) : tm.transformPt(atom));
      if (screen.z == 1 && tm.internalSlab && tm.xyzIsSlabbedInternal(atom)) {
        bsSlabbed.set(i);
      }
      atom.sX = screen.x;
      atom.sY = screen.y;
      atom.sZ = screen.z;
      int d = Math.abs(atom.madAtom);
      if (d == Atom.MAD_GLOBAL)
        d = (int) (vwr.getFloat(T.atoms) * 2000);
      atom.sD = (short) vwr.tm.scaleToScreen(screen.z, d);
      if (checkOccupancy
          && vibrationVectors[i] != null
          && (occ = vibrationVectors[i].getOccupancy100(vibs)) != Integer.MIN_VALUE) {
        //System.out.println(atom + " " + occ);
        haveMods = true;
        atom.setShapeVisibility(Atom.ATOM_VISSET, false);
        if (occ >= 0 && occ < 50)
          atom.setShapeVisibility(Atom.ATOM_NOTHIDDEN | JC.VIS_BALLS_FLAG,
              false);
        else
          atom.setShapeVisibility(Atom.ATOM_NOTHIDDEN
              | (atom.madAtom > 0 ? JC.VIS_BALLS_FLAG : 0), true);
        ms.occupancies[atom.i] = Math.abs(occ);
      }
    }
    if (haveMods)
      setShapeVis();
    GData gdata = vwr.gdata;
    if (tm.slabEnabled) {
      boolean slabByMolecule = vwr.getBoolean(T.slabbymolecule);
      boolean slabByAtom = vwr.getBoolean(T.slabbyatom);
      int minZ = gdata.slab;
      int maxZ = gdata.depth;
      if (slabByMolecule) {
        JmolMolecule[] molecules = ms.getMolecules();
        int moleculeCount = ms.getMoleculeCountInModel(-1);
        for (int i = 0; i < moleculeCount; i++) {
          JmolMolecule m = molecules[i];
          int j = 0;
          int pt = m.firstAtomIndex;
          if (!bsOK.get(pt))
            continue;
          for (; j < m.ac; j++, pt++)
            if (gdata.isClippedZ(atoms[pt].sZ - (atoms[pt].sD >> 1)))
              break;
          if (j != m.ac) {
            pt = m.firstAtomIndex;
            for (int k = 0; k < m.ac; k++) {
              bsOK.clear(pt);
              atoms[pt++].sZ = 0;
            }
          }
        }
      }
      for (int i = bsOK.nextSetBit(0); i >= 0; i = bsOK.nextSetBit(i + 1)) {
        Atom atom = atoms[i];
        if (gdata.isClippedZ(atom.sZ - (slabByAtom ? atoms[i].sD >> 1 : 0))) {
          atom.setClickable(0);
          // note that in the case of navigation,
          // maxZ is set to Integer.MAX_VALUE.
          int r = (slabByAtom ? -1 : 1) * atom.sD / 2;
          if (atom.sZ + r < minZ || atom.sZ - r > maxZ
              || !gdata.isInDisplayRange(atom.sX, atom.sY)) {
            bsOK.clear(i);
          }
        }
      }
    }
    if (ms.ac == 0 || !vwr.getShowNavigationPoint())
      return null;
    // set min/max for navigation crosshair rendering
    int minX = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxY = Integer.MIN_VALUE;
    for (int i = bsOK.nextSetBit(0); i >= 0; i = bsOK.nextSetBit(i + 1)) {
      Atom atom = atoms[i];
      if (atom.sX < minX)
        minX = atom.sX;
      if (atom.sX > maxX)
        maxX = atom.sX;
      if (atom.sY < minY)
        minY = atom.sY;
      if (atom.sY > maxY)
        maxY = atom.sY;
    }
    navMinMax[0] = minX;
    navMinMax[1] = maxX;
    navMinMax[2] = minY;
    navMinMax[3] = maxY;
    return navMinMax;
  }

  public void setModelSet(ModelSet modelSet) {
    ms = vwr.ms = modelSet;
  }

  /**
   * starting with Jmol 13.1.13, isosurfaces can use "property color" 
   * to inherit the color of the underlying atoms. This is then dynamic
   * 
   */
  public void checkInheritedShapes() {
    if (shapes[JC.SHAPE_ISOSURFACE] == null)
      return;
    setShapePropertyBs(JC.SHAPE_ISOSURFACE, "remapInherited", null, null);
  }

  public void restrictSelected(boolean isBond, boolean doInvert) {
    BS bsSelected = vwr.slm.getSelectedAtomsNoSubset();
    if (doInvert) {
      vwr.slm.invertSelection();
      BS bsSubset = vwr.slm.bsSubset;
      if (bsSubset != null) {
        bsSelected = vwr.slm.getSelectedAtomsNoSubset();
        bsSelected.and(bsSubset);
        vwr.select(bsSelected, false, 0, true);
        BSUtil.invertInPlace(bsSelected, vwr.ms.ac);
        bsSelected.and(bsSubset);
      }
    }
    BSUtil.andNot(bsSelected, vwr.slm.bsDeleted);
    boolean bondmode = vwr.getBoolean(T.bondmodeor);

    if (!isBond)
      vwr.setBooleanProperty("bondModeOr", true);
    setShapeSizeBs(JC.SHAPE_STICKS, 0, null, null);
    // wireframe will not operate on STRUTS even though they are
    // a form of bond order (see BondIteratoSelected)
    setShapePropertyBs(JC.SHAPE_STICKS, "type", Integer
        .valueOf(Edge.BOND_STRUT), null);
    setShapeSizeBs(JC.SHAPE_STICKS, 0, null, null);
    setShapePropertyBs(JC.SHAPE_STICKS, "type", Integer
        .valueOf(Edge.BOND_COVALENT_MASK), null);
    // also need to turn off backbones, ribbons, strands, cartoons
    BS bs = vwr.bsA();
    for (int iShape = JC.SHAPE_MAX_SIZE_ZERO_ON_RESTRICT; --iShape >= 0;)
      if (iShape != JC.SHAPE_MEASURES && getShape(iShape) != null)
        setShapeSizeBs(iShape, 0, null, bs);
    if (getShape(JC.SHAPE_POLYHEDRA) != null)
      setShapePropertyBs(JC.SHAPE_POLYHEDRA, "off", bs, null);
    setLabel(null, bs);
    if (!isBond)
      vwr.setBooleanProperty("bondModeOr", bondmode);
    vwr.select(bsSelected, false, 0, true);
  }

}
