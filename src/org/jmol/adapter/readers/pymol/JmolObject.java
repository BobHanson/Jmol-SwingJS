/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-10-14 12:33:20 -0500 (Sun, 14 Oct 2007) $
 * $Revision: 8408 $

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

package org.jmol.adapter.readers.pymol;


import java.util.Collection;
import java.util.Map;

import org.jmol.atomdata.RadiusData;
import javajs.util.BS;
import org.jmol.modelset.MeasurementData;
import org.jmol.modelset.ModelSet;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.Escape;
import org.jmol.util.Point3fi;

import javajs.util.Lst;
import javajs.util.SB;
import javajs.util.P3;

import javajs.util.PT;
import org.jmol.viewer.JC;
import org.jmol.viewer.ShapeManager;

/**
 * a class to store rendering information prior to finishing file loading,
 * specifically designed for reading PyMOL PSE files.
 * 
 * More direct than a script
 * 
 */
class JmolObject {

  int id;
  private BS bsAtoms;
  private Object info;
  private int size = -1;  
  private Object[] colors;
  int modelIndex = Integer.MIN_VALUE;

  String jmolName;
  int argb;
  float translucency = 0;
  boolean visible = true;
  RadiusData rd;
  public String cacheID;

  /**
   * 
   * @param id   A Token or JmolConstants.SHAPE_XXXX
   * @param branchNameID
   * @param bsAtoms
   * @param info     optional additional information for the shape
   */
  JmolObject(int id, String branchNameID, BS bsAtoms, Object info) {
    this.id = id;
    this.bsAtoms = bsAtoms;
    this.info = info;
    this.jmolName = branchNameID;
  }
  
  /**
   * offset is carried out in ModelLoader when the "script" is processed to move
   * the bits to skip the base atom index.
   * 
   * @param modelOffset
   * @param atomOffset
   */
  @SuppressWarnings("unchecked")
  void offset(int modelOffset, int atomOffset) {
    if (modelOffset > 0) {
      if (modelIndex != Integer.MIN_VALUE)
        modelIndex += modelOffset;
      switch (id) {
      case T.display:
      case T.hide:
        return;
      case T.frame:
        int i = ((Integer) info).intValue();
        if (i >= 0)
          info = Integer.valueOf(modelOffset + i);
        return;
      case T.movie:
        Map<String, Object> movie = (Map<String, Object>) info;
        // switch now to simple array
        int[] frames = (int[]) movie.get("frames");
        for (int j = frames.length; --j >= 0;)
          frames[j] += modelOffset;
        return;
      }
    }
    if (atomOffset <= 0)
      return;
    if (id == T.define) {
      Collection<Object> map = ((Map<String, Object>) info).values();
      for (Object o : map)
        BSUtil.offset((BS) o, 0, atomOffset);
      return;
    }
    if (bsAtoms != null)
      BSUtil.offset(bsAtoms, 0, atomOffset);
    if (colors != null) {
      short[] colixes = (short[]) colors[0];
      short[] c = new short[colixes.length + atomOffset];
      System.arraycopy(colixes, 0, c, atomOffset, colixes.length);
      colors[0] = c;
    }
  }

  @SuppressWarnings("unchecked")
  void finalizeObject(PyMOLScene pymolScene, ModelSet m, String mepList,
                      boolean doCache) {
    ShapeManager sm = m.sm;
    String color = "color";
    String sID;
    SB sb = null;
    if (bsAtoms != null)
      modelIndex = getModelIndex(m);
    switch (id) {
    case T.hidden:
      // bsHidden
      sm.vwr.displayAtoms(bsAtoms, false, false, T.add, true);
      return;
    case T.restrict:
      // start of generating shapes; argb is modelIndex
      BS bs = sm.vwr.getModelUndeletedAtomsBitSet(argb);
      BSUtil.invertInPlace(bs, sm.vwr.ms.ac);
      sm.vwr.select(bs, false, 0, true);
      sm.restrictSelected(false, true);
      return;
    case T.display:
    case T.hide:
      // from PyMOLScene after restore scene
      if (bsAtoms == null) {
        if (info == null) {
          sm.vwr.displayAtoms(null, true, false, 0, true);
        }
        sm.vwr.setObjectProp((String) info, id);
      } else {
        sm.vwr.displayAtoms(bsAtoms, id == T.display, false, T.add, true);
      }
      return;
    case T.define:
      // executed even for states
      sm.vwr.defineAtomSets((Map<String, Object>) info);
      return;
    case T.movie:
      sm.vwr.am.setMovie((Map<String, Object>) info);
      return;
    case T.frame:
      int frame = ((Integer) info).intValue();
      if (frame >= 0) {
        sm.vwr.setCurrentModelIndex(frame);
      } else {
        sm.vwr.setAnimationRange(-1, -1);
        sm.vwr.setCurrentModelIndex(-1);
      }
      return;
    case T.scene:
      sm.vwr.stm.saveScene(jmolName, (Map<String, Object>) info);
      sm.vwr.stm.saveOrientation(jmolName,
          (float[]) ((Map<String, Object>) info).get("pymolView"));
      return;
    case JC.SHAPE_LABELS:
      sm.loadShape(id);
      sm.setShapePropertyBs(id, "pymolLabels", info, bsAtoms);
      return;
    case T.bonds:
      break;
    case T.wireframe:
    case JC.SHAPE_STICKS:
      if (size != -1) {
        sm.setShapeSizeBs(JC.SHAPE_STICKS, size, null, bsAtoms);
        BS bsBonds = ((BS[]) sm.getShapePropertyIndex(JC.SHAPE_STICKS, "sets",
            0))[1];
        pymolScene.setUniqueBonds(bsBonds, id == JC.SHAPE_STICKS);
        size = -1;
      }
      id = JC.SHAPE_STICKS;
      break;
    case T.atoms:
      id = JC.SHAPE_BALLS;
      break;
    case JC.SHAPE_BALLS:
      break;
    case JC.SHAPE_TRACE:
    case JC.SHAPE_BACKBONE:
      sm.loadShape(id);
      BS bsCarb = m.getAtoms(T.carbohydrate, null);
      BSUtil.andNot(bsAtoms, bsCarb);
      break;
    case JC.SHAPE_DOTS:
      sm.loadShape(id);
      sm.setShapePropertyBs(id, "ignore", BSUtil.copyInvert(bsAtoms, sm.vwr
          .ms.ac), null);
      break;
    default:
      if (!visible)
        return; // for now -- could be occluded by a nonvisible group
      break;
    }

    switch (id) {
    case JC.SHAPE_CGO:
      sm.vwr.setCGO((Lst<Object>) info);
      break;
    case JC.SHAPE_DOTS:
    case JC.SHAPE_BALLS:
    case JC.SHAPE_STARS:
    case JC.SHAPE_ELLIPSOIDS:
    case JC.SHAPE_CARTOON:
    case JC.SHAPE_BACKBONE:
    case JC.SHAPE_TRACE:
    case JC.SHAPE_ISOSURFACE:
      if (info instanceof Object[]) {
        sm.loadShape(id);
        sm.setShapePropertyBs(id, "params", info, bsAtoms);
      }
      break;
    case JC.SHAPE_MEASURES:
      if (modelIndex < 0)
        return;
      sm.loadShape(id);
      MeasurementData md = (MeasurementData) info;
      md.setModelSet(m);
      Lst<Object> points = md.points;
      for (int i = points.size(); --i >= 0;)
        ((Point3fi) points.get(i)).mi = (short) modelIndex;
      sm.setShapePropertyBs(id, "measure", md, bsAtoms);
      return;
    case T.isosurface:
      sID = (bsAtoms == null ? (String) info : jmolName);
      // when getting a scene, ignore creation of this surface
      if (sm.getShapeIdFromObjectName(sID) >= 0) {
        sm.vwr.setObjectProp(sID, T.display);
        return;
      }
      sb = new SB();
      sb.append("isosurface ID ").append(PT.esc(sID));
      if (modelIndex < 0)
        modelIndex = sm.vwr.am.cmi;
      if (bsAtoms == null) {
        // point display of map 
        sb.append(" model ")        
            .append(m.getModelNumberDotted(modelIndex)).append(
                " color density sigma 1.0 ").append(PT.esc(cacheID)).append(" ").append(
                PT.esc(sID));
        if (doCache)
          sb.append(";isosurface cache");
      } else {
        String lighting = (String) ((Object[]) info)[0];
        String only = (String) ((Object[]) info)[1];
        only = " only";
        BS bsCarve = (BS) ((Object[]) info)[2];
        float carveDistance = ((Float) ((Object[]) info)[3]).floatValue();
        // not implementing "not only" yet because if we did that, since we have so
        // many sets of atoms, we could have real problems here.
        String resolution = "";
        if (lighting == null) {
          lighting = "mesh nofill";
          resolution = " resolution 1.5";
        }
        boolean haveMep = PT.isOneOf(sID, mepList);
        String model = m.getModelNumberDotted(modelIndex);
        //        BS bsIgnore = sm.vwr.getAtomsWithinRadius(0.1f, bsAtoms, true, 
        //            new RadiusData(null, 0.1f, EnumType.ABSOLUTE, null));
        //        bsIgnore.andNot(bsAtoms);
        //        String ignore = " ignore " + Escape.eBS(bsIgnore);
        String ignore = "";
        String type = (size < 0 ? " sasurface " : " solvent ");
        sb.append(" model ").append(model).append(resolution)
            .append(" select ").append(Escape.eBS(bsAtoms)).append(only)
            .append(ignore).append(type).appendF(Math.abs(size / 1000f));
        if (!haveMep) {
          if (argb == 0)
            sb.append(" map property color");
          else
            sb.append(";color isosurface ").append(Escape.escapeColor(argb));
        }
        sb.append(";isosurface frontOnly ").append(lighting);
        if (translucency > 0)
          sb.append(";color isosurface translucent " + translucency);
        if (bsCarve != null && !bsCarve.isEmpty())
          sb.append(";isosurface slab within " + carveDistance + " {" + model
              + " and " + Escape.eBS(bsCarve) + "}");
        if (doCache && !haveMep)
          sb.append(";isosurface cache");
      }
      break;
    case T.mep:
      Lst<Object> mep = (Lst<Object>) info;
      sID = mep.get(mep.size() - 2).toString();
      String mapID = mep.get(mep.size() - 1).toString();
      float min = PyMOLReader.floatAt(PyMOLReader.listAt(mep, 3), 0);
      float max = PyMOLReader.floatAt(PyMOLReader.listAt(mep, 3), 2);
      sb = new SB();
      sb.append(";isosurface ID ").append(PT.esc(sID)).append(" map ").append(PT.esc(cacheID)).append(" ")
          .append(PT.esc(mapID)).append(
              ";color isosurface range " + min + " " + max
                  + ";isosurface colorscheme rwb;set isosurfacekey true");
      if (translucency > 0)
        sb.append(";color isosurface translucent " + translucency);
      if (doCache)
        sb.append(";isosurface cache");
      break;
    case T.mesh:
      modelIndex = sm.vwr.am.cmi;
      Lst<Object> mesh = (Lst<Object>) info;
      sID = mesh.get(mesh.size() - 2).toString();
      sb = new SB();
      sb.append("isosurface ID ").append(PT.esc(sID)).append(" model ")
          .append(m.getModelNumberDotted(modelIndex))
          .append(" color ").append(Escape.escapeColor(argb)).append("  ").append(PT.esc(cacheID)).append(" ")
          .append(PT.esc(sID)).append(" mesh nofill frontonly");
      Lst<Object> list = PyMOLReader.sublistAt(mesh, 2, 0);
      float within = PyMOLReader.floatAt(list, 11);  
      list = PyMOLReader.listAt(list, 12);
      if (within > 0) {
        P3 pt = new P3();
        sb.append(";isosurface slab within ").appendF(within).append(" [ ");
        for (int j = list.size() - 3; j >= 0; j -= 3) {
          PyMOLReader.pointAt(list, j, pt);
          sb.append(Escape.eP(pt));
        }
        sb.append(" ]");
      }
      if (doCache && !PT.isOneOf(sID, mepList))
        sb.append(";isosurface cache");
      sb.append(";set meshScale ").appendI(size / 500);
      break;
    case T.script:
      sb = (SB) info;
      break;
    case T.trace:
      sm.loadShape(id = JC.SHAPE_TRACE);
      sm.setShapePropertyBs(id, "putty", info, bsAtoms);
      break;
    }
    if (sb != null) {
      //System.out.println("jmolobject " + sb);
      sm.vwr.runScriptCautiously(sb.toString());
      return;
    }
    // cartoon, trace, etc.
    if (size != -1 || rd != null)
      sm.setShapeSizeBs(id, size, rd, bsAtoms);
    if (argb != 0)
      sm.setShapePropertyBs(id, color, Integer.valueOf(argb), bsAtoms);
    if (translucency > 0) {
      sm.setShapePropertyBs(id, "translucentLevel",
          Float.valueOf(translucency), bsAtoms);
      sm.setShapePropertyBs(id, "translucency", "translucent", bsAtoms);
    } else if (colors != null)
      sm.setShapePropertyBs(id, "colors", colors, bsAtoms);
  }

  private int getModelIndex(ModelSet m) {
    if (bsAtoms == null)
      return -1;
    int iAtom = bsAtoms.nextSetBit(0);
    if (iAtom >= m.at.length)
      System.out.println("PyMOL LOADING ERROR IN MERGE");
    return (iAtom < 0 ? -1 : m.at[iAtom].mi);
  }

  void setColors(short[] colixes, float translucency) {
    colors = new Object[] {colixes, Float.valueOf(translucency) };
  }
  
  void setSize(float size) {
    this.size = (int) (size * 1000);
  }

}