/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-08-09 23:55:19 -0500 (Tue, 09 Aug 2016) $
 * $Revision: 21195 $

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

package org.jmol.shape;

import org.jmol.util.C;

import javajs.awt.Font;
import javajs.util.Lst;
import javajs.util.SB;
import javajs.util.T3;

import org.jmol.util.Logger;
import javajs.util.P3;
import javajs.util.P3i;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;
import org.jmol.atomdata.RadiusData;
import org.jmol.c.PAL;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.Group;
import org.jmol.modelset.ModelSet;



import java.util.Map;

/**
 * Shape now encompasses:
 * 
 * AtomShape (abstract)
 *     |
 *   Balls, Dots, Ellipsoids, Halos, Labels, Polyhedra, Stars, Vectors
 *           |
 *         GeoSurface  
 * 
 * BioShapeCollection (abstract)
 *     |
 *   Backbone, Cartoon, MeshRibbon, Ribbons, Rockets, Strands, Trace
 * 
 * Dipoles
 * 
 * FontLineShape (abstract)
 *     |
 *   Axes, Bbcage, Frank, Uccage
 * 
 * Measures
 * 
 * MeshCollection (abstract)
 *   |       |
 * Draw   Isosurface
 *           |
 *        LcaoCartoon, MolecularOrbital, Pmesh
 * 
 * Sticks
 *     |
 *    Hsticks, Sssticks
 * 
 * TextShape (abstract)
 *     |
 *    Echo, Hover
 *    
 */
public abstract class Shape {

  public String myType;

  abstract public String getShapeState();
  abstract public void setProperty(String propertyName, Object value, BS bsSelected);

  //public Shape () {
  //  System.out.println("Shape " + this + " constructed");
  //}
  
  //public void finalize() {
  //  System.out.println("Shape " + shapeID + " " + this + " finalized");
  //}
  
  public static final float RADIUS_MAX = 4;
  public Viewer vwr; //public for now for Backbone
  public ModelSet ms;
  public int shapeID;
  /**
   * shape visibility flag
   */
  public int vf;
  // AtomShape: Balls, Dots, GeoSurface, Ellipsoids, Halos, Labels, Polyhedra, Stars, Vectors
  // Sticks, Dipoles, BioShape
  // MeshCollection: Draw, CGO, Isosurface, LcaoCartoon, MolecularOrbital, Pmesh 

  protected float translucentLevel;
  public boolean translucentAllowed = true;
  public boolean isBioShape;
  public BS bsSizeSet;
  public BS bsColixSet;
  
  final public void initializeShape(Viewer vwr, ModelSet modelSet,
                               int shapeID) {
    this.vwr = vwr;
    this.shapeID = shapeID;
    vf = JC.getShapeVisibilityFlag(shapeID);
    setModelSet(modelSet);
    initShape();
  }

  public abstract void initShape();

  /**
   * @param bsModels  
   */
  public void setModelVisibilityFlags(BS bsModels) {
    // only some atom-based shapes implement this.
  }
  /**
   * overridden in Dots, CGOMesh, and AtomShape
   * 
   * @param atomIndex
   * @return size
   */
  public int getSize(int atomIndex) {
    return 0;
  }

  /**
   * 
   * 
   * @param group
   * @return size
   */
  public int getSizeG(Group group) {
    return 0;
  }

  /**
   * @param g0
   * @param g1
   */
  public void replaceGroup(Group g0, Group g1) {
    //BioShapeCollection only; after MUTATE
  }

  public void setModelSet(ModelSet modelSet) {
    this.ms = modelSet;
    initModelSet();
  }
  
  protected void initModelSet() {
  }

  public void setShapeSizeRD(int size, RadiusData rd, BS bsSelected) {
    if (rd == null)
      setSize(size, bsSelected);
    else
      setSizeRD(rd, bsSelected);
  }

  /**
   * 
   * @param size
   * @param bsSelected
   */
  protected void setSize(int size, BS bsSelected) {
    // not for atoms except to turn off -- size = 0
  }

  /**
   * 
   * @param rd
   * @param bsSelected
   */
  protected void setSizeRD(RadiusData rd, BS bsSelected) {
    // balls, dots, other atomshapes
  }

  @SuppressWarnings("unchecked")
  protected void setPropS(String propertyName, Object value, BS bsSelected) {
    if (propertyName == "setProperties") {
      if (bsSelected == null)
        bsSelected = vwr.bsA();
      Lst<Object[]> propertyList = (Lst<Object[]>) value;
      while (propertyList.size() > 0) {
        Object[] data = propertyList.removeItemAt(0);
        setProperty(((String) data[0]).intern(), data[1], bsSelected);
      }
      return;
    }
    if (propertyName == "translucentLevel") {
      translucentLevel = ((Float) value).floatValue();
      return;
    }

    if (propertyName == "refreshTrajectories") {
      return;
    }

    Logger.warn("unassigned " + JC.shapeClassBases[shapeID] + " + shape setProperty:" + propertyName + ":" + value);
  }

  /**
   * 
   * @param property
   * @param data
   * @return true if serviced
   */
  public boolean getPropertyData(String property, Object[] data) {
    return getPropShape(property, data);
  }

  protected boolean getPropShape(String property, Object[] data) {
    if (data[1] instanceof Integer) {
       int index = ((Integer) data[1]).intValue();
       data[1] = getProperty(property, index);
       return (data[1] != null);
     }
    return false;
   }

  /**
   * 
   * @param property
   * @param index
   * @return true if serviced
   */
  public Object getProperty(String property, int index) {
    return null;
  }

  /**
   * 
   * @param thisID
   * @return index
   */
  public int getIndexFromName(String thisID) {
    return -1;
  }

  /**
   * 
   * @param x
   * @param y
   * @return T/F
   */
  public boolean wasClicked(int x, int y) {
    return false;
  }

  /**
   * 
   * @param xMouse
   * @param yMouse
   * @param closest
   * @param bsNot
   */
  public void findNearestAtomIndex(int xMouse, int yMouse, Atom[] closest, BS bsNot) {
  }

  /**
   * 
   * @param pointMin
   * @param pointMax
   */
  public void checkBoundsMinMax(P3 pointMin, P3 pointMax) {
  }

  public void setAtomClickability() {
  }

  /**
   * 
   * @param x
   * @param y
   * @param modifiers
   * @param bsVisible
   * @param drawPicking TODO
   * @return Hashtable containing information about pt clicked
   */
  public Map<String, Object> checkObjectClicked(int x, int y, int modifiers, BS bsVisible, boolean drawPicking) {
    return null;
  }

  /**
   * 
   * @param x
   * @param y
   * @param bsVisible
   * @return T/F
   */
  public boolean checkObjectHovered(int x, int y, BS bsVisible) {
    return false;
  }

  /**
   * 
   * @param prevX
   * @param prevY
   * @param x
   * @param y
   * @param dragAction
   * @param bsVisible
   * @return T/F
   */
  public boolean checkObjectDragged(int prevX, int prevY, int x, int y,
                             int dragAction, BS bsVisible) {
    return false;
  }

  protected int coordinateInRange(int x, int y, T3 vertex, int dmin2, P3i ptXY) {
    vwr.tm.transformPtScr(vertex, ptXY);
    int d2 = (x - ptXY.x) * (x - ptXY.x) + (y - ptXY.y) * (y - ptXY.y);
    return (d2 < dmin2 ? d2 : -1);
  }
  
  public short getColixI(short colix, byte paletteID, int atomIndex) {
    return getColixA(colix, paletteID, ms.at[atomIndex]);
  }

  protected short getColixA(short colix, byte paletteID, Atom atom) {
    return (colix == C.USE_PALETTE ? vwr.cm.getColixAtomPalette(atom,
        paletteID) : colix);
  }

  protected short getColixB(short colix, int pid, Bond bond) {
    return (colix == C.USE_PALETTE ? vwr.cm.getColixBondPalette(bond,
        pid) : colix);
  }

  public Object getShapeDetail() {
    return null;
  }

  public static short getColix(short[] colixes, int i, Atom atom) {
    return C.getColixInherited(
        (colixes == null || i >= colixes.length ? C.INHERIT_ALL
            : colixes[i]), atom.colixAtom);
  }
  
  public static String getFontCommand(String type, Font font) {
    if (font == null)
      return "";
    return "font " + type + " " + font.getInfo();
  }

  public static String getColorCommandUnk(String type, short colix,
                                   boolean translucentAllowed) {
    return getColorCommand(type, PAL.UNKNOWN.id, colix,
        translucentAllowed);
  }

  public static String getColorCommand(String type, byte pid, short colix,
                                       boolean translucentAllowed) {
    if (pid == PAL.UNKNOWN.id && colix == C.INHERIT_ALL)
      return "";
    String s = (pid == PAL.UNKNOWN.id && colix == C.INHERIT_ALL ? ""
        : (translucentAllowed ? getTranslucentLabel(colix) + " " : "")
            + (pid != PAL.UNKNOWN.id
                && !PAL.isPaletteVariable(pid) ? PAL
                .getPaletteName(pid) : encodeColor(colix)));
    return "color " + type + " " + s;
  }

  public static String encodeColor(short colix) {
    // used also by labels for background state (no translucent issues there?)
    return (C.isColixColorInherited(colix) ? "none" : C
        .getHexCode(colix));
  }

  public static String getTranslucentLabel(short colix) {
    return (C.isColixTranslucent(colix) ? C.getColixTranslucencyLabel(colix): "opaque");
  }

  
  protected static void appendCmd(SB s, String cmd) {
    if (cmd.length() == 0)
      return;
    s.append("  ").append(cmd).append(";\n");
  }    
  
}
