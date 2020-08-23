/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-03-05 12:22:08 -0600 (Sun, 05 Mar 2006) $
 * $Revision: 4545 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

package org.jmol.scriptext;

import java.util.Map;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.M4;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.Quat;
import javajs.util.SB;
import javajs.util.T3;
import javajs.util.V3;

import org.jmol.adapter.readers.quantum.GenNBOReader;
import org.jmol.api.Interface;
import org.jmol.api.JmolDataManager;
import org.jmol.api.SymmetryInterface;
import org.jmol.atomdata.RadiusData;
import org.jmol.atomdata.RadiusData.EnumType;
import org.jmol.c.VDW;
import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.quantum.MepCalculation;
import org.jmol.script.SV;
import org.jmol.script.ScriptError;
import org.jmol.script.ScriptEval;
import org.jmol.script.ScriptException;
import org.jmol.script.ScriptInterruption;
import org.jmol.script.T;
import org.jmol.shape.MeshCollection;
import org.jmol.util.BSUtil;
import org.jmol.util.BoxInfo;
import org.jmol.util.C;
import org.jmol.util.ColorEncoder;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.MeshCapper;
import org.jmol.util.Parser;
import org.jmol.util.SimpleUnitCell;
import org.jmol.util.TempArray;
import org.jmol.util.Triangulator;
import org.jmol.viewer.JC;
import org.jmol.viewer.JmolAsyncException;

public class IsoExt extends ScriptExt {

  public IsoExt() {
    // used by Reflection
  }
  
  @Override
  public String dispatch(int iTok, boolean b, T[] st) throws ScriptException {
    chk = e.chk; 
    slen = e.slen;
    this.st = st;

    switch (iTok) {
    case JC.SHAPE_CGO:
      cgo();
      break;
    case JC.SHAPE_CONTACT:
      contact();
      break;
    case JC.SHAPE_DIPOLES:
      dipole();
      break;
    case JC.SHAPE_DRAW:
      draw();
      break;
    case JC.SHAPE_ISOSURFACE:
    case JC.SHAPE_PLOT3D:
    case JC.SHAPE_PMESH:
      isosurface(iTok);
      break;
    case JC.SHAPE_LCAOCARTOON:
      lcaoCartoon();
      break;
    case JC.SHAPE_MO:
    case JC.SHAPE_NBO:
      mo(b, iTok);
      break;
    }
    return null;
  }

  private void dipole() throws ScriptException {
    ScriptEval eval = e;
    // dipole intWidth floatMagnitude OFFSET floatOffset {atom1} {atom2}
    String propertyName = null;
    Object propertyValue = null;
    boolean iHaveAtoms = false;
    boolean iHaveCoord = false;
    boolean idSeen = false;
    boolean getCharges = false;
    BS bsSelected = null;

    eval.sm.loadShape(JC.SHAPE_DIPOLES);
    if (tokAt(1) == T.list && listIsosurface(JC.SHAPE_DIPOLES))
      return;
    setShapeProperty(JC.SHAPE_DIPOLES, "init", null);
    if (slen == 1) {
      setShapeProperty(JC.SHAPE_DIPOLES, "thisID", null);
      return;
    }
    for (int i = 1; i < slen; ++i) {
      propertyName = null;
      propertyValue = null;
      switch (getToken(i).tok) {
      case T.all:
        propertyName = "all";
        getCharges = true;
        break;
      case T.on:
        propertyName = "on";
        break;
      case T.off:
        propertyName = "off";
        break;
      case T.delete:
        propertyName = "delete";
        break;
      case T.integer:
      case T.decimal:
        propertyName = "value";
        propertyValue = Float.valueOf(floatParameter(i));
        break;
      case T.bitset:
        if (tokAt(i + 1) == T.bitset) {
          // fix for atomno2 < atomno1
          setShapeProperty(JC.SHAPE_DIPOLES, "startSet", atomExpressionAt(i++));
        } else {
          // early Jmol
          propertyName = "atomBitset";
        }
        //$FALL-THROUGH$
      case T.expressionBegin:
        if (propertyName == null)
          propertyName = (iHaveAtoms || iHaveCoord ? "endSet" : "startSet");
        propertyValue = bsSelected = atomExpressionAt(i);
        i = eval.iToken;
        if (tokAt(i + 1) == T.nada && propertyName == "startSet")
          propertyName = "atomBitset";
        iHaveAtoms = true;
        getCharges = true;
        break;
      case T.leftbrace:
      case T.point3f:
        // {X, Y, Z}
        P3 pt = getPoint3f(i, true);
        i = eval.iToken;
        propertyName = (iHaveAtoms || iHaveCoord ? "endCoord" : "startCoord");
        propertyValue = pt;
        iHaveCoord = true;
        break;
      case T.bonds:
        propertyName = "bonds";
        getCharges = true;
        break;
      case T.calculate:
        getCharges = true;
        propertyName = "calculate";
        if (eval.isAtomExpression(i + 1)) {
          propertyValue = bsSelected = atomExpressionAt(++i);
          i = eval.iToken;
        }
        break;
      case T.id:
        setShapeId(JC.SHAPE_DIPOLES, ++i, idSeen);
        i = eval.iToken;
        break;
      case T.cross:
        propertyName = "cross";
        propertyValue = Boolean.TRUE;
        break;
      case T.nocross:
        propertyName = "cross";
        propertyValue = Boolean.FALSE;
        break;
      case T.offset:
        if (isFloatParameter(i + 1)) {
        float v = floatParameter(++i);
        if (eval.theTok == T.integer) {
          propertyName = "offsetPercent";
          propertyValue = Integer.valueOf((int) v);
        } else {
          propertyName = "offset";
          propertyValue = Float.valueOf(v);
        }
        } else {
          propertyName = "offsetPt";
          propertyValue = centerParameter(++i);
          i = eval.iToken;
        }
        break;
      case T.offsetside:
        propertyName = "offsetSide";
        propertyValue = Float.valueOf(floatParameter(++i));
        break;
      case T.val:
        propertyName = "value";
        propertyValue = Float.valueOf(floatParameter(++i));
        break;
      case T.width:
        propertyName = "width";
        propertyValue = Float.valueOf(floatParameter(++i));
        break;
      default:
        if (eval.theTok == T.times || T.tokAttr(eval.theTok, T.identifier)) {
          setShapeId(JC.SHAPE_DIPOLES, i, idSeen);
          i = eval.iToken;
          break;
        }
        invArg();
      }
      idSeen = (eval.theTok != T.delete && eval.theTok != T.calculate);
      if (getCharges) {
        // ensures that we do have charges and catch the Script interruption if asynchronous
        if (!chk)
          eval.getPartialCharges(bsSelected);
        getCharges = false;
      }
      if (propertyName != null)
        setShapeProperty(JC.SHAPE_DIPOLES, propertyName, propertyValue);
    }
    if (iHaveCoord || iHaveAtoms)
      setShapeProperty(JC.SHAPE_DIPOLES, "set", null);
  }

  private void draw() throws ScriptException {
    ScriptEval eval = e;
    eval.sm.loadShape(JC.SHAPE_DRAW);
    switch (tokAt(1)) {
    case T.list:
      if (listIsosurface(JC.SHAPE_DRAW))
        return;
      break;
    case T.helix:
    case T.quaternion:
    case T.ramachandran:
      e.getCmdExt().dispatch(T.plot, false, st);
      return;
    }
    boolean havePoints = false;
    boolean isInitialized = false;
    boolean isSavedState = false;
    boolean isIntersect = false;
    boolean isFrame = false;
    P4 plane;
    int tokIntersect = 0;
    float translucentLevel = Float.MAX_VALUE;
    int[] colorArgb = new int[] { Integer.MIN_VALUE };
    int intScale = 0;
    String swidth = "";
    int iptDisplayProperty = 0;
    P3 center = null;
    String thisId = initIsosurface(JC.SHAPE_DRAW);
    boolean idSeen = (thisId != null);
    boolean isWild = (idSeen && getShapeProperty(JC.SHAPE_DRAW, "ID") == null);
    int[] connections = null;
    int iConnect = 0;
    int iArray = -1;
    SymmetryInterface uc = null;
    for (int i = eval.iToken; i < slen; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      int tok = getToken(i).tok;
      switch (tok) {
      case T.pointgroup:
        // draw pointgroup [array  of points] CENTER xx
        // draw pointgroup SPACEGROUP
        // draw pointgroup [C2|C3|Cs|Ci|etc.] [n] [scale x]
        P3[] pts = (eval.isArrayParameter(++i)
            ? eval.getPointArray(i, -1, false)
            : null);
        if (pts != null) {
          i = eval.iToken + 1;
          if (tokAt(i) == T.center) {
            center = eval.centerParameter(++i, null);
            i = eval.iToken + 1;
          }
        }
        String type;
        switch (tokAt(i)) {
        case T.scale:
          type = "";
          break;
        case T.chemicalshift:
          type = "Cs";
          break;
        case T.dollarsign:
          Object[] data = new Object[] { eval.objectNameParameter(++i), null };
          if (chk)
            return;
          vwr.shm.getShapePropertyData(JC.SHAPE_POLYHEDRA, "points", data);
          pts = (P3[]) data[1];
          if (pts == null)
            invArg();
          type = "";
          break;
        case T.polyhedra:
          type = ":poly";
          break;
        case T.spacegroup:
          if (center == null)
            center = new P3();
          Lst<P3> crpts = vwr.ms.generateCrystalClass(vwr.bsA().nextSetBit(0),
              P3.new3(Float.NaN, Float.NaN, Float.NaN));
          if (pts != null)
            invArg();
          pts = new P3[crpts.size()];
          for (int j = crpts.size(); --j >= 0;)
            pts[j] = crpts.get(j);
          i++;
          type = "";
          break;
        default:
          type = eval.optParameterAsString(i);
          break;
        }
        float scale = 1;
        int index = 0;
        if (type.length() > 0) {
          if (isFloatParameter(++i))
            index = intParameter(i++);
        }
        if (tokAt(i) == T.scale)
          scale = floatParameter(++i);
        if (!chk)
          eval.runScript(vwr.ms.getPointGroupAsString(vwr.bsA(), type, index,
              scale, pts, center, thisId == null ? "" : thisId));
        return;
      case T.unitcell:
      case T.boundbox:
        if (chk)
          break;
        if (tok == T.boundbox && tokAt(i + 1) == T.best) {
          tok = T.unitcell;
        }
        if (tok == T.unitcell) {
          if (eval.isArrayParameter(i + 1)) {
            P3[] oabc = eval.getPointArray(i + 1, -1, false);
            uc = vwr.getSymTemp().getUnitCell(oabc, false, null);
            i = eval.iToken;
          } else if (tokAt(i + 1) == T.best) {
            i++;
            uc = vwr.getSymTemp().getUnitCell(
                (P3[]) vwr.getOrientationText(T.unitcell, "array", null), false,
                null);
          } else {
            uc = vwr.getCurrentUnitCell();
          }
          if (uc == null)
            invArg();
        }
        Lst<Object> vp = getPlaneIntersection(tok, null, uc, intScale / 100f,
            0);
        intScale = 0;
        propertyName = "polygon";
        propertyValue = vp;
        havePoints = true;
        break;
      case T.connect:
        connections = new int[4];
        iConnect = 4;
        float[] farray = eval.floatParameterSet(++i, 4, 4);
        i = eval.iToken;
        for (int j = 0; j < 4; j++)
          connections[j] = (int) farray[j];
        havePoints = true;
        break;
      case T.bonds:
      case T.atoms:
        if (connections == null
            || iConnect > (eval.theTok == T.bondcount ? 2 : 3)) {
          iConnect = 0;
          connections = new int[] { -1, -1, -1, -1 };
        }
        connections[iConnect++] = atomExpressionAt(++i).nextSetBit(0);
        i = eval.iToken;
        connections[iConnect++] = (eval.theTok == T.bonds
            ? atomExpressionAt(++i).nextSetBit(0)
            : -1);
        i = eval.iToken;
        havePoints = true;
        break;
      case T.slab:
        switch (getToken(++i).tok) {
        case T.dollarsign:
          propertyName = "slab";
          propertyValue = eval.objectNameParameter(++i);
          i = eval.iToken;
          havePoints = true;
          break;
        default:
          invArg();
        }
        break;
      case T.intersection:
        switch (getToken(i + 1).tok) {
        case T.unitcell:
        case T.boundbox:
          tokIntersect = eval.theTok;
          isIntersect = true;
          continue;
        case T.dollarsign:
          propertyName = "intersect";
          propertyValue = eval.objectNameParameter(++i);
          i = eval.iToken;
          isIntersect = true;
          havePoints = true;
          break;
        default:
          invArg();
        }
        break;
      case T.polyhedra:
      case T.point:
      case T.polygon:
        tok = eval.theTok;
        boolean isPoints = (tok == T.point);
        propertyName = "polygon";
        havePoints = true;
        Lst<Object> v = new Lst<Object>();
        int nVertices = 0;
        int nTriangles = 0;
        P3[] points = null;
        Lst<SV> vpolygons = null;
        int[][] polygons = null;
        if (eval.isArrayParameter(++i)) {
          // draw POLYGON [points]
          points = eval.getPointArray(i, -1, true);
          if (points.length > 0 && points[0] == null) {
            int[][] faces;
            if (tok == T.polyhedra) {
              faces = getIntArray2(i);
            } else {
              faces = AU.newInt2(1);
              faces[0] = eval.expandFloatArray(
                  eval.floatParameterSet(i, -1, Integer.MAX_VALUE), -1);
            }
            points = getAllPoints(e.iToken + 1, 3);
            try {
              polygons = ((MeshCapper) Interface
                  .getInterface("org.jmol.util.MeshCapper", vwr, "script"))
                      .set(null).triangulateFaces(faces, points, null);
            } catch (Throwable e) {
              invArg();
            }
          }
          nVertices = points.length;
        }

        if (tok == T.polyhedra) {
          // draw POLYHEDRA @x @y 
          //  where x is [[0,3,4][4,5,6] ...] where numbers are atom indices
          //  and optional y is an atom bitset or a list of points
          nVertices = points.length;
        }

        if (points == null) {
          // draw POLYGON nPoints pt1 pt2 pt3...
          nVertices = Math.max(0, intParameter(i));
          points = new P3[nVertices];
          for (int j = 0; j < nVertices; j++)
            points[j] = centerParameter(++eval.iToken);
        }
        i = eval.iToken;
        switch (tokAt(i + 1)) {
        case T.matrix3f:
        case T.matrix4f:
          // draw POLYGON <points> [[0,1,2],[1,2,3]...]
          vpolygons = SV.newT(getToken(++i)).toArray().getList();
          nTriangles = vpolygons.size();
          break;
        case T.varray:
          // draw POLYGON <points> [[0,1,2],[1,2,3]...]
          vpolygons = ((SV) getToken(++i)).getList();
          nTriangles = vpolygons.size();
          break;
        case T.integer:
          // draw POLYGON <points> nTriangles
          nTriangles = intParameter(++i);
          if (nTriangles < 0)
            isPoints = true;
          break;
        default:
          // get triangles from a list of points
          if (polygons == null && !isPoints && !chk)
            polygons = ((MeshCapper) Interface
                .getInterface("org.jmol.util.MeshCapper", vwr, "script"))
                    .set(null).triangulatePolygon(points, -1);
        }
        if (polygons == null && !isPoints) {
          // read array of arrays of triangle vertex pointers
          polygons = AU.newInt2(nTriangles);
          for (int j = 0; j < nTriangles; j++) {
            float[] f = (vpolygons == null
                ? eval.floatParameterSet(++eval.iToken, 3, 4)
                : SV.flistValue(vpolygons.get(j), 0));
            if (f.length < 3 || f.length > 4)
              invArg();
            polygons[j] = new int[] { (int) f[0], (int) f[1], (int) f[2],
                (f.length == 3 ? 7 : (int) f[3]) };
          }
        }
        if (nVertices > 0) {
          v.addLast(points);
          v.addLast(polygons);
        } else {
          v = null;
        }
        propertyValue = v;
        i = eval.iToken;
        break;
      case T.spacegroup:
      case T.symop:
        String xyz = null;
        int iSym = Integer.MAX_VALUE;
        plane = null;
        P3 target = null;
        BS bsAtoms = null;
        int options = 0;
        if (tok == T.symop) {
          iSym = 0;
          switch (tokAt(++i)) {
          case T.string:
            xyz = stringParameter(i);
            break;
          case T.matrix4f:
            xyz = SV.sValue(getToken(i));
            break;
          case T.integer:
          default:
            if (!eval.isCenterParameter(i))
              iSym = intParameter(i++);
            Object[] ret = new Object[] { null, vwr.getFrameAtoms() };
            if (eval.isCenterParameter(i))
              center = eval.centerParameter(i, ret);
            if (eval.isCenterParameter(eval.iToken + 1))
              target = eval.centerParameter(++eval.iToken, ret);
            if (chk)
              return;
            i = eval.iToken;
          }
        }
        if (center == null && i + 1 < slen) {
          center = centerParameter(++i);
          // draw ID xxx symop [n or "x,-y,-z"] [optional {center}]
          // so we also check here for the atom set to get the right model
          bsAtoms = (eval.isAtomExpression(i) ? atomExpressionAt(i) : null);
          i = eval.iToken;
        }
        int nth = (target != null && tokAt(i + 1) == T.integer
            ? eval.getToken(++i).intValue
            : -1);
        if (tokAt(i + 1) == T.unitcell) {
          target = new P3();
          options = T.offset;
          i++;
          eval.iToken = i;
        } else if (tokAt(i + 1) == T.offset) {
          i++;
          target = getPoint3f(i + 1, false);
          options = T.offset;
          i = eval.iToken;
        }

        eval.checkLast(eval.iToken);
        if (!chk) {
          String s = "";
          if (bsAtoms == null && vwr.am.cmi >= 0)
            bsAtoms = vwr.getModelUndeletedAtomsBitSet(vwr.am.cmi);
          if (bsAtoms != null) {
            s = null;
            int iatom = bsAtoms.nextSetBit(0);
            if (options != 0) {
              // options is T.offset, and target is an {i j k} offset from cell 555
              Object o = vwr.getSymmetryInfo(iatom,
                  xyz, iSym, center, target, T.point, null, intScale / 100f,
                  nth, options);
              if (o instanceof P3)
                target = (P3) o;
              else
                s = "";
            }
            if (thisId == null)
              thisId = "sym";
            if (s == null)
              s = (String) vwr.getSymmetryInfo(iatom,
                  xyz, iSym, center, target, T.draw, thisId, intScale / 100f,
                  nth, options);
          }
          eval.runBufferedSafely(
              s.length() > 0 ? s : "draw ID \"" + thisId + "_*\" delete", eval.outputBuffer);
        }
        return;
      case T.frame:
        isFrame = true;
        // draw ID xxx frame {center} {q1 q2 q3 q4}
        continue;
      case T.leftbrace:
      case T.point4f:
      case T.point3f:
        // {X, Y, Z}
        if (eval.theTok == T.point4f || !eval.isPoint3f(i)) {
          propertyValue = eval.getPoint4f(i);
          if (isFrame) {
            eval.checkLast(eval.iToken);
            if (!chk)
              eval.runScript(Escape.drawQuat(Quat.newP4((P4) propertyValue),
                  (thisId == null ? "frame" : thisId), " " + swidth,
                  (center == null ? new P3() : center), intScale / 100f));
            return;
          }
          propertyName = "planedef";
        } else {
          propertyValue = center = getPoint3f(i, true);
          propertyName = "coord";
        }
        i = eval.iToken;
        havePoints = true;
        break;
      case T.hkl:
      case T.plane:
        if (!havePoints && !isIntersect && tokIntersect == 0) {
          if (eval.theTok == T.hkl) {
            havePoints = true;
            setShapeProperty(JC.SHAPE_DRAW, "plane", null);
            plane = eval.hklParameter(++i);
            i = eval.iToken;
            propertyName = "coords";
            Lst<P3> list = new Lst<P3>();
            list.addLast(P3.newP(eval.pt1));
            list.addLast(P3.newP(eval.pt2));
            list.addLast(P3.newP(eval.pt3));
            propertyValue = list;
          } else {
            propertyName = "plane";
            iArray = i + 1;
          }
          break;
        }
        if (eval.theTok == T.plane) {
          plane = eval.planeParameter(i);
        } else {
          plane = eval.hklParameter(++i);
        }
        i = eval.iToken;
        if (tokIntersect != 0) {
          if (chk)
            break;
          Lst<Object> vpc = getPlaneIntersection(tokIntersect, plane, uc,
              intScale / 100f, 0);
          intScale = 0;
          propertyName = "polygon";
          propertyValue = vpc;
        } else {
          propertyValue = plane;
          propertyName = "planedef";
        }
        havePoints = true;
        break;
      case T.linedata:
        propertyName = "lineData";
        propertyValue = eval.floatParameterSet(++i, 0, Integer.MAX_VALUE);
        i = eval.iToken;
        havePoints = true;
        break;
      case T.define:
      case T.bitset:
      case T.expressionBegin:
        propertyName = "atomSet";
        propertyValue = atomExpressionAt(i);
        if (isFrame)
          center = centerParameter(i);
        i = eval.iToken;
        havePoints = true;
        break;
      case T.varray:
        havePoints = true;
        propertyName = (iArray == i ? "coords" : "modelBasedPoints");
        propertyValue = eval.theToken.value;
        break;
      case T.spacebeforesquare:
      case T.comma:
        break;
      case T.leftsquare:
        // [x y] or [x y %]
        propertyValue = eval.xypParameter(i);
        if (propertyValue != null) {
          i = eval.iToken;
          propertyName = "coord";
          havePoints = true;
          break;
        }
        if (isSavedState)
          invArg();
        isSavedState = true;
        break;
      case T.rightsquare:
        if (!isSavedState)
          invArg();
        isSavedState = false;
        break;
      case T.reverse:
        propertyName = "reverse";
        break;
      case T.string:
        propertyValue = stringParameter(i);
        propertyName = "title";
        break;
      case T.vector:
        propertyName = "vector";
        break;
      case T.length:
        propertyValue = Float.valueOf(floatParameter(++i));
        propertyName = "length";
        break;
      case T.decimal:
        // $drawObject
        propertyValue = Float.valueOf(floatParameter(i));
        propertyName = "length";
        break;
      case T.modelindex:
        propertyName = "modelIndex";
        propertyValue = Integer.valueOf(intParameter(++i));
        break;
      case T.integer:
        if (isSavedState) {
          propertyName = "modelIndex";
          propertyValue = Integer.valueOf(intParameter(i));
        } else {
          intScale = intParameter(i);
        }
        break;
      case T.scale:
        intScale = Math.round(
            floatParameter(++i) * (getToken(i).tok == T.integer ? 1 : 100));
        continue;
      case T.id:
        thisId = setShapeId(JC.SHAPE_DRAW, ++i, idSeen);
        isWild = (getShapeProperty(JC.SHAPE_DRAW, "ID") == null);
        i = eval.iToken;
        break;
      case T.modelbased:
        propertyName = "fixed";
        propertyValue = Boolean.FALSE;
        break;
      case T.fixed:
        propertyName = "fixed";
        propertyValue = Boolean.TRUE;
        break;
      case T.offset:
        P3 pt = getPoint3f(++i, true);
        i = eval.iToken;
        propertyName = "offset";
        propertyValue = pt;
        break;
      case T.crossed:
        propertyName = "crossed";
        break;
      case T.width:
        propertyValue = Float.valueOf(floatParameter(++i));
        propertyName = "width";
        swidth = propertyName + " " + propertyValue;
        break;
      case T.line:
        propertyName = "line";
        propertyValue = Boolean.TRUE;
        iArray = i + 1;
        break;
      case T.curve:
        propertyName = "curve";
        iArray = i + 1;
        break;
      case T.arc:
        propertyName = "arc";
        iArray = i + 1;
        break;
      case T.arrow:
        propertyName = "arrow";
        iArray = i + 1;
        break;
      case T.vertices:
        propertyName = "vertices";
        iArray = i + 1;
        break;
      case T.circle:
        propertyName = "circle";
        break;
      case T.cylinder:
        propertyName = "cylinder";
        break;
      case T.nohead:
        propertyName = "nohead";
        break;
      case T.barb:
        propertyName = "isbarb";
        break;
      case T.rotate45:
        propertyName = "rotate45";
        break;
      case T.perpendicular:
        propertyName = "perp";
        break;
      case T.radius:
      case T.diameter:
        boolean isRadius = (eval.theTok == T.radius);
        float f = floatParameter(++i);
        if (isRadius)
          f *= 2;
        propertyValue = Float.valueOf(f);
        propertyName = (isRadius || tokAt(i) == T.decimal ? "width"
            : "diameter");
        swidth = propertyName
            + (tokAt(i) == T.decimal ? " " + f : " " + ((int) f));
        break;
      case T.dollarsign:
        // $drawObject[m]
        if ((tokAt(i + 2) == T.leftsquare || isFrame)) {
          P3 pto = center = centerParameter(i);
          i = eval.iToken;
          propertyName = "coord";
          propertyValue = pto;
          havePoints = true;
          break;
        }
        // $drawObject
        propertyValue = eval.objectNameParameter(++i);
        propertyName = "identifier";
        havePoints = true;
        break;
      case T.color:
      case T.translucent:
      case T.opaque:
        idSeen = true;
        translucentLevel = getColorTrans(eval, i, false, colorArgb);
        i = eval.iToken;
        continue;
      default:
        if (!eval.setMeshDisplayProperty(JC.SHAPE_DRAW, 0, eval.theTok)) {
          if (eval.theTok == T.times || T.tokAttr(eval.theTok, T.identifier)) {
            thisId = setShapeId(JC.SHAPE_DRAW, i, idSeen);
            i = eval.iToken;
            break;
          }
          invArg();
        }
        if (iptDisplayProperty == 0)
          iptDisplayProperty = i;
        i = eval.iToken;
        continue;
      }
      idSeen = (eval.theTok != T.delete);
      if (havePoints && !isInitialized && !isFrame) {
        setShapeProperty(JC.SHAPE_DRAW, "points", Integer.valueOf(intScale));
        isInitialized = true;
        intScale = 0;
      }
      if (havePoints && isWild)
        invArg();
      if (propertyName != null)
        setShapeProperty(JC.SHAPE_DRAW, propertyName, propertyValue);
    }
    finalizeObject(JC.SHAPE_DRAW, colorArgb[0], translucentLevel, intScale,
        havePoints, connections, iptDisplayProperty, null);
  }

  private void mo(boolean isInitOnly, int iShape) throws ScriptException {
    ScriptEval eval = e;
    int offset = Integer.MAX_VALUE;
    boolean isNegOffset = false;
    String nboType = null;
    BS bsModels = vwr.getVisibleFramesBitSet();
    Lst<Object[]> propertyList = new Lst<Object[]>();
    boolean isBeta = false;
    boolean isNBO = (tokAt(0) == T.nbo); 
    int i0 = 1;
    if (isNBO) {
      // NBO command by itself starts the NBO Server Interface panel
      // NBO OPTIONS include "NOZAP;VIEW"
      boolean isViewOnly = e.optParameterAsString(1).equals("view");
      if (e.slen == 1 || isViewOnly ||  e.optParameterAsString(1).equals("options")) {
        if (!chk) {
          String options = (isViewOnly ? "VIEW" : e.optParameterAsString(2));
          vwr.startNBO(options);
        }
        return;
      }
    }
    if (tokAt(1) == T.model || tokAt(1) == T.frame) {
      i0 = eval.modelNumberParameter(2);
      if (i0 < 0)
        invArg();
      bsModels.clearAll();
      bsModels.set(i0);
      i0 = 3;
    }
    eval.sm.loadShape(iShape);
    for (int iModel = bsModels.nextSetBit(0); iModel >= 0; iModel = bsModels
        .nextSetBit(iModel + 1)) {
      int i = i0;
      if (tokAt(i) == T.list && listIsosurface(iShape))
        return;
      setShapeProperty(iShape, "init", Integer.valueOf(iModel));
      if (isInitOnly)
        return;// (moNumber != 0);
      String title = null;
      int moNumber = ((Integer) getShapeProperty(iShape, "moNumber"))
          .intValue();
      float[] linearCombination = (float[]) getShapeProperty(iShape,
          "moLinearCombination");
      Boolean squared = (Boolean) getShapeProperty(iShape, "moSquareData");
      Boolean linearSquared = (linearCombination == null ? null
          : (Boolean) getShapeProperty(iShape, "moSquareLinear"));
      if (moNumber == 0)
        moNumber = Integer.MAX_VALUE;
      String propertyName = null;
      Object propertyValue = null;
      boolean ignoreSquared = false;

      switch (getToken(i).tok) {
      case T.type:
        if (iShape == T.mo) {
          mo(isInitOnly, JC.SHAPE_NBO);
          return;
        }
        nboType = paramAsStr(++i).toUpperCase();
        break;
      case T.cap:
      case T.slab:
        propertyName = (String) eval.theToken.value;
        propertyValue = getCapSlabObject(i, false);
        i = eval.iToken;
        break;
      case T.density:
        linearSquared = Boolean.TRUE;
        linearCombination = new float[] { 1 };
        offset = moNumber = 0;
        break;
      case T.integer:
        moNumber = intParameter(i);
        if (tokAt(i + 1) == T.beta) {
          isBeta = true;
          i++;
        }
        linearCombination = moCombo(propertyList);
        if (linearCombination == null && moNumber < 0)
          linearCombination = new float[] { -100, -moNumber };
        ignoreSquared = true;
        break;
      case T.minus:
        switch (tokAt(++i)) {
        case T.homo:
        case T.lumo:
          break;
        default:
          invArg();
        }
        isNegOffset = true;
        //$FALL-THROUGH$
      case T.homo:
      case T.lumo:
        if ((offset = moOffset(i)) == Integer.MAX_VALUE)
          invArg();
        moNumber = 0;
        linearCombination = moCombo(propertyList);
        ignoreSquared = true;
        break;
      case T.next:
        moNumber = T.next;
        isBeta = false;
        linearCombination = moCombo(propertyList);
        ignoreSquared = true;
        break;
      case T.prev:
        moNumber = T.prev;
        isBeta = false;
        linearCombination = moCombo(propertyList);
        ignoreSquared = true;
        break;
      case T.color:
        setColorOptions(null, i + 1, iShape, 2);
        break;
      case T.plane:
        propertyName = "plane";
        propertyValue = (tokAt(e.iToken = ++i) == T.none ? null : eval
            .planeParameter(i));
        break;
      case T.point:
        addShapeProperty(propertyList, "randomSeed",
            tokAt(i + 2) == T.integer ? Integer.valueOf(intParameter(i + 2))
                : null);
        propertyName = "monteCarloCount";
        propertyValue = Integer.valueOf(intParameter(i + 1));
        break;
      case T.scale:
        propertyName = "scale";
        propertyValue = Float.valueOf(floatParameter(i + 1));
        break;
      case T.cutoff:
        if (tokAt(i + 1) == T.plus) {
          propertyName = "cutoffPositive";
          propertyValue = Float.valueOf(floatParameter(i + 2));
        } else {
          propertyName = "cutoff";
          propertyValue = Float.valueOf(floatParameter(i + 1));
        }
        break;
      case T.debug:
        propertyName = "debug";
        break;
      case T.noplane:
        propertyName = "plane";
        break;
      case T.pointsperangstrom:
      case T.resolution:
        propertyName = "resolution";
        propertyValue = Float.valueOf(floatParameter(i + 1));
        break;
      case T.squared:
        if (linearCombination == null)
          squared = Boolean.TRUE;
        else
          linearSquared = Boolean.TRUE;
        ignoreSquared = false;
        break;
      case T.titleformat:
        if (i + 1 < slen && tokAt(i + 1) == T.string) {
          propertyName = "titleFormat";
          propertyValue = paramAsStr(i + 1);
        }
        break;
      case T.identifier:
        invArg();
        break;
      default:
        if (eval.isArrayParameter(i)) {
          linearCombination = eval.floatParameterSet(i, 1, Integer.MAX_VALUE);
          if (tokAt(eval.iToken + 1) == T.squared) {
            ignoreSquared = false;
            linearSquared = Boolean.TRUE;
            eval.iToken++;
          }
          break;
        }
        int ipt = eval.iToken;
        if (!eval.setMeshDisplayProperty(iShape, 0, eval.theTok))
          invArg();
        setShapeProperty(iShape, "setProperties", propertyList);
        eval.setMeshDisplayProperty(iShape, ipt, tokAt(ipt));
        return;
      }
      if (propertyName != null)
        addShapeProperty(propertyList, propertyName, propertyValue);
      boolean haveMO = (moNumber != Integer.MAX_VALUE || linearCombination != null);
      if (chk)
        return;
      if (nboType != null || haveMO) {
        if (haveMO && tokAt(eval.iToken + 1) == T.string)
          title = paramAsStr(++eval.iToken);
        eval.setCursorWait(true);
        setMoData(propertyList, moNumber, linearCombination, offset,
            isNegOffset, iModel, title, nboType, isBeta);
        if (haveMO) {
          addShapeProperty(propertyList, "finalize", null);
        }
      }
      if (!ignoreSquared) {
        setShapeProperty(iShape, "squareLinear", linearSquared);
        setShapeProperty(iShape, "squareData", squared);
      }
      if (propertyList.size() > 0)
        setShapeProperty(iShape, "setProperties", propertyList);
      if (haveMO && !eval.tQuiet) {
        String moLabel = "";
        if (isNBO) {
          moLabel = (String) getShapeProperty(iShape, "moLabel");
        } else {
          moNumber = ((Integer) getShapeProperty(iShape, "moNumber")).intValue();
          moLabel = "" + moNumber;
        }
        showString(T.nameOf(tokAt(0)) + " " + moLabel + " "
            + (isBeta ? "beta " : "") + getShapeProperty(iShape, "message"));
      }

      propertyList.clear();
    }
  }

  @SuppressWarnings("static-access")
  private void setNBOType(Map<String, Object> moData, String type) throws ScriptException {

    int ext = JC.getNBOTypeFromName(type);
    if (ext < 0)
      invArg();
    if (!moData.containsKey("nboLabels"))
      error(ScriptError.ERROR_moModelError);
    if (chk)
      return;
    if (!((GenNBOReader) Interface.getInterface("org.jmol.adapter.readers.quantum.GenNBOReader", vwr, "script"))
    		.readNBOCoefficients(moData, type, vwr))
      error(ScriptError.ERROR_moModelError);
  }

  private float[] moCombo(Lst<Object[]> propertyList) {
    if (tokAt(e.iToken + 1) != T.squared)
      return null;
    addShapeProperty(propertyList, "squareLinear", Boolean.TRUE);
    e.iToken++;
    return new float[0];
  }

  private int moOffset(int index) throws ScriptException {
    boolean isHomo = (getToken(index).tok == T.homo);
    int offset = (isHomo ? 0 : 1);
    int tok = tokAt(++index);
    if (tok == T.integer && ((String) e.st[index].value).charAt(0) == '-') // could be '-0'
      offset += intParameter(index);
    else if (tok == T.plus)
      offset += intParameter(++index);
    else if (tok == T.minus) // - <space> n
      offset -= intParameter(++index);
    return offset;
  }

  @SuppressWarnings("unchecked")
  private void setMoData(Lst<Object[]> propertyList, int moNumber, float[] lc,
                         int offset, boolean isNegOffset, int modelIndex,
                         String title, String nboType, boolean isBeta)
      throws ScriptException {
    ScriptEval eval = e;
    if (modelIndex < 0) {
      modelIndex = vwr.am.cmi;
      if (modelIndex < 0)
        eval.errorStr(ScriptError.ERROR_multipleModelsDisplayedNotOK,
            "MO isosurfaces");
    }
    Map<String, Object> moData = (Map<String, Object>) vwr.ms.getInfo(
        modelIndex, "moData");
    if (moData == null)
      error(ScriptError.ERROR_moModelError);
    vwr.checkMenuUpdate();
    if (nboType != null) {
      setNBOType(moData, nboType);
      if (lc == null && moNumber == Integer.MAX_VALUE)
        return;
    }
    Lst<Map<String, Object>> mos = null;
    Map<String, Object> mo;
    int nOrb = 0;
    Float f = null;
    if (lc == null || lc.length < 2) {
      if (lc != null && lc.length == 1)
        offset = 0;
      else if (isBeta && moData.containsKey("firstBeta"))
        offset = ((Integer) moData.get("firstBeta")).intValue();
      int lastMoNumber = (moData.containsKey("lastMoNumber") ? ((Integer) moData
          .get("lastMoNumber")).intValue() : 0);
      int lastMoCount = (moData.containsKey("lastMoCount") ? ((Integer) moData
          .get("lastMoCount")).intValue() : 1);
      if (moNumber == T.prev)
        moNumber = lastMoNumber - 1;
      else if (moNumber == T.next)
        moNumber = lastMoNumber + lastMoCount;
      mos = (Lst<Map<String, Object>>) (moData.get("mos"));
      nOrb = (mos == null ? 0 : mos.size());
      if (nOrb == 0)
        error(ScriptError.ERROR_moCoefficients);
      if (nOrb == 1 && moNumber > 1)
        error(ScriptError.ERROR_moOnlyOne);
      if (offset != Integer.MAX_VALUE) {
        // 0: HOMO;
        if (isBeta) {
        } else if (moData.containsKey("HOMO")) {
          moNumber = ((Integer) moData.get("HOMO")).intValue() + offset;
          offset = 0;
        } else {
          moNumber = nOrb;
          for (int i = 0; i < nOrb; i++) {
            mo = mos.get(i);
            if ((f = (Float) mo.get("occupancy")) != null) {
              if (f.floatValue() < 0.5f) {
                // go for LUMO = first unoccupied
                moNumber = i;
                break;
              }
              continue;
            } else if ((f = (Float) mo.get("energy")) != null) {
              if (f.floatValue() > 0) {
                // go for LUMO = first positive
                moNumber = i;
                break;
              }
              continue;
            }
            break;
          }
          if (f == null)
            error(ScriptError.ERROR_moOccupancy);
        }
        moNumber += offset;
        if (!chk)
          Logger.info("MO " + moNumber);
      }
      if (moNumber < 1 || moNumber > nOrb)
        eval.errorStr(ScriptError.ERROR_moIndex, "" + nOrb);
    }
    moNumber = Math.abs(moNumber);
    moData.put("lastMoNumber", Integer.valueOf(moNumber));
    moData.put("lastMoCount", Integer.valueOf(1));
    if (isNegOffset && lc == null)
      lc = new float[] { -100, moNumber };
    if (lc != null && lc.length < 2) {
      mo = mos.get(moNumber - 1);
      if ((f = (Float) mo.get("energy")) == null) {
        lc = new float[] { 100, moNumber };
      } else {

        // constuct set of equivalent energies and square this

        float energy = f.floatValue();
        BS bs = BS.newN(nOrb);
        int n = 0;
        boolean isAllElectrons = (lc.length == 1 && lc[0] == 1);
        for (int i = 0; i < nOrb; i++) {
          if ((f = (Float) mos.get(i).get("energy")) == null)
            continue;
          float e = f.floatValue();
          if (isAllElectrons ? e <= energy : e == energy) {
            bs.set(i + 1);
            n += 2;
          }
        }
        lc = new float[n];
        for (int i = 0, pt = 0; i < n; i += 2) {
          lc[i] = 1;
          lc[i + 1] = (pt = bs.nextSetBit(pt + 1));
        }
        moData.put("lastMoNumber", Integer.valueOf(bs.nextSetBit(0)));
        moData.put("lastMoCount", Integer.valueOf(n / 2));
      }
      addShapeProperty(propertyList, "squareLinear", Boolean.TRUE);
    }
    addShapeProperty(propertyList, "moData", moData);
    if (title != null)
      addShapeProperty(propertyList, "title", title);
    addShapeProperty(propertyList, "molecularOrbital", lc != null ? lc
        : Integer.valueOf(Math.abs(moNumber)));
    addShapeProperty(propertyList, "clear", null);
  }

  private void isosurface(int iShape) throws ScriptException {
    // also called by lcaoCartoon
    ScriptEval eval = e;
    eval.sm.loadShape(iShape);
    if (tokAt(1) == T.list && listIsosurface(iShape))
      return;
    int iptDisplayProperty = 0;
    boolean isDisplay = false;
    boolean isIsosurface = (iShape == JC.SHAPE_ISOSURFACE);
    boolean isPmesh = (iShape == JC.SHAPE_PMESH);
    boolean isPlot3d = (iShape == JC.SHAPE_PLOT3D);
    boolean isLcaoCartoon = (iShape == JC.SHAPE_LCAOCARTOON);
    boolean isSilent = (isLcaoCartoon || tokAt(1) == T.delete
        || eval.isStateScript);
    boolean surfaceObjectSeen = false;
    boolean planeSeen = false;
    boolean isMapped = false;
    boolean isBicolor = false;
    boolean isPhased = false;
    boolean doCalcArea = false;
    boolean doCalcVolume = false;
    boolean isBeta = false;
    boolean isCavity = false;
    boolean haveRadius = false;
    boolean toCache = false;
    boolean isFxy = false;
    boolean haveSlab = false;
    boolean haveIntersection = false;
    boolean isFrontOnly = false;
    String nbotype = null;
    float[] data = null;
    String cmd = null;
    int thisSetNumber = Integer.MIN_VALUE;
    int nFiles = 0;
    int nX, nY, nZ, ptX, ptY;
    float sigma = Float.NaN;
    float cutoff = Float.NaN;
    int ptWithin = 0;
    Boolean smoothing = null;
    int smoothingPower = Integer.MAX_VALUE;
    BS bs = null;
    BS bsSelect = null;
    BS bsIgnore = null;
    SB sbCommand = new SB();
    P3 pt;
    P4 plane = null;
    P3 lattice = null;
    boolean fixLattice = false;
    P3[] pts = null;
    int color = 0;
    String str = null;
    int modelIndex = (chk ? 0 : Integer.MIN_VALUE);
    eval.setCursorWait(true);
    boolean idSeen = (initIsosurface(iShape) != null);
    boolean isWild = (idSeen && getShapeProperty(iShape, "ID") == null);
    boolean isColorSchemeTranslucent = false;
    boolean isInline = false;
    boolean isSign = false;
    Object onlyOneModel = null;
    Object[] filesData = null;
    String translucency = null;
    String colorScheme = null;
    String mepOrMlp = null;
    M4[] symops = null;
    short[] discreteColixes = null;
    Lst<Object[]> propertyList = new Lst<Object[]>();
    boolean defaultMesh = false;
    if (isPmesh || isPlot3d)
      addShapeProperty(propertyList, "fileType", "Pmesh");

    for (int i = eval.iToken; i < slen; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      getToken(i);
      if (eval.theTok == T.identifier)
        str = paramAsStr(i);
      switch (eval.theTok) {
      // settings only
      case T.silent:
        isSilent = true;
        sbCommand.append(" silent");
        propertyName = "silent";
        break;
      case T.isosurfacepropertysmoothing:
        smoothing = (getToken(++i).tok == T.on ? Boolean.TRUE
            : eval.theTok == T.off ? Boolean.FALSE : null);
        if (smoothing == null)
          invArg();
        continue;
      case T.isosurfacepropertysmoothingpower:
        smoothingPower = intParameter(++i);
        continue;
      // offset, rotate, and scale3d don't need to be saved in sbCommand
      // because they are display properties
      case T.move: // Jmol 13.0.RC2 -- required for state saving after coordinate-based translate/rotate
        // but this will not work for MO calculations, which have to be
        // generated in their original atom-based frame
        propertyName = "moveIsosurface";
        if (tokAt(++i) != T.matrix4f)
          invArg();
        propertyValue = getToken(i++).value;
        break;
      case T.symop:
        float[][] ff = floatArraySet(i + 2, intParameter(i + 1), 16);
        symops = new M4[ff.length];
        for (int j = symops.length; --j >= 0;)
          symops[j] = M4.newA16(ff[j]);
        i = eval.iToken;
        break;
      case T.symmetry:
        if (modelIndex < 0)
          modelIndex = Math.min(vwr.am.cmi, 0);
        boolean needIgnore = (bsIgnore == null);
        if (bsSelect == null)
          bsSelect = BSUtil.copy(vwr.bsA());
        // and in symop=1
        bsSelect.and(vwr.ms.getAtoms(T.symop, Integer.valueOf(1)));
        if (!needIgnore)
          bsSelect.andNot(bsIgnore);
        addShapeProperty(propertyList, "select", bsSelect);
        if (needIgnore) {
          bsIgnore = BSUtil.copy(bsSelect);
          BSUtil.invertInPlace(bsIgnore, vwr.ms.ac);
          isFrontOnly = true;
          addShapeProperty(propertyList, "ignore", bsIgnore);
          sbCommand.append(" ignore ").append(Escape.eBS(bsIgnore));
        }
        sbCommand.append(" symmetry");
        if (color == 0)
          addShapeProperty(propertyList, "colorRGB", Integer.valueOf(T.symop));
        symops = vwr.ms.getSymMatrices(modelIndex);
        break;
      case T.offset:
        propertyName = "offset";
        propertyValue = centerParameter(++i);
        i = eval.iToken;
        break;
      case T.rotate:
        propertyName = "rotate";
        propertyValue = (tokAt(eval.iToken = ++i) == T.none ? null
            : eval.getPoint4f(i));
        i = eval.iToken;
        break;
      case T.scale3d:
        propertyName = "scale3d";
        propertyValue = Float.valueOf(floatParameter(++i));
        break;
      case T.period:
        sbCommand.append(" periodic");
        propertyName = "periodic";
        break;
      case T.origin:
      case T.step:
      case T.point:
        propertyName = eval.theToken.value.toString();
        sbCommand.append(" ").appendO(eval.theToken.value);
        propertyValue = centerParameter(++i);
        sbCommand.append(" ").append(Escape.eP((P3) propertyValue));
        i = eval.iToken;
        break;
      case T.boundbox:
        if (eval.fullCommand.indexOf("# BBOX=") >= 0) {
          String[] bbox = PT
              .split(PT.getQuotedAttribute(eval.fullCommand, "# BBOX"), ",");
          pts = new P3[] { (P3) Escape.uP(bbox[0]), (P3) Escape.uP(bbox[1]) };
        } else if (eval.isCenterParameter(i + 1)) {
          pts = new P3[] { getPoint3f(i + 1, true),
              getPoint3f(eval.iToken + 1, true) };
          i = eval.iToken;
        } else {
          pts = vwr.ms.getBBoxVertices();
        }
        sbCommand.append(" boundBox " + Escape.eP(pts[0]) + " "
            + Escape.eP(pts[pts.length - 1]));
        propertyName = "boundingBox";
        propertyValue = pts;
        break;
      case T.pmesh:
        isPmesh = true;
        sbCommand.append(" pmesh");
        propertyName = "fileType";
        propertyValue = "Pmesh";
        break;
      case T.intersection:
        // isosurface intersection {A} {B} VDW....
        // isosurface intersection {A} {B} function "a-b" VDW....
        bsSelect = atomExpressionAt(++i);
        if (chk) {
          bs = new BS();
        } else if (tokAt(eval.iToken + 1) == T.expressionBegin
            || tokAt(eval.iToken + 1) == T.bitset) {
          bs = atomExpressionAt(++eval.iToken);
          bs.and(vwr.ms.getAtomsWithinRadius(5.0f, bsSelect, false, null));
        } else {
          // default is "within(5.0, selected) and not within(molecule,selected)"
          bs = vwr.ms.getAtomsWithinRadius(5.0f, bsSelect, true, null);
          bs.andNot(vwr.ms.getAtoms(T.molecule, bsSelect));
        }
        bs.andNot(bsSelect);
        sbCommand.append(" intersection ").append(Escape.eBS(bsSelect))
            .append(" ").append(Escape.eBS(bs));
        i = eval.iToken;
        if (tokAt(i + 1) == T.function) {
          i++;
          String f = (String) getToken(++i).value;
          sbCommand.append(" function ").append(PT.esc(f));
          if (!chk)
            addShapeProperty(propertyList, "func",
                (f.equals("a+b") || f.equals("a-b") ? f
                    : createFunction("__iso__", "a,b", f)));
        } else {
          haveIntersection = true;
        }
        propertyName = "intersection";
        propertyValue = new BS[] { bsSelect, bs };
        break;
      case T.display:
      case T.within:
        isDisplay = (eval.theTok == T.display);
        if (isDisplay) {
          sbCommand.append(" display");
          iptDisplayProperty = i;
          int tok = tokAt(i + 1);
          if (tok == T.nada)
            continue;
          i++;
          addShapeProperty(propertyList, "token", Integer.valueOf(T.on));
          if (tok == T.bitset || tok == T.all) {
            propertyName = "bsDisplay";
            if (tok == T.all) {
              sbCommand.append(" all");
            } else {
              propertyValue = st[i].value;
              sbCommand.append(" ").append(Escape.eBS((BS) propertyValue));
            }
            eval.checkLast(i);
            break;
          } else if (tok != T.within) {
            eval.iToken = i;
            invArg();
          }
        } else {
          ptWithin = i;
        }
        float distance;
        P3 ptc = null;
        bs = null;
        Object[] ret = new Object[1];
        if (tokAt(i + 1) == T.expressionBegin) {
          // within ( x.x , .... )
          distance = floatParameter(i + 3);
          if (eval.isPoint3f(i + 4)) {
            ptc = eval.centerParameter(i + 4, null);
            eval.iToken += 2;
          } else if (eval.isPoint3f(i + 5)) {
            ptc = eval.centerParameter(i + 5, null);
            eval.iToken += 2;
          } else {
            bs = eval.atomExpression(st, i + 5, slen, true, false, ret, true);
            if (bs == null)
              invArg();
          }
        } else {
          distance = floatParameter(++i);
          ptc = eval.centerParameter(++i, ret);
          bs = (ret[0] instanceof BS ? (BS) ret[0] : null);
        }
        if (isDisplay)
          eval.checkLast(eval.iToken);
        i = eval.iToken;
        if (eval.fullCommand.indexOf("# WITHIN=") >= 0)
          bs = BS.unescape(PT.getQuotedAttribute(eval.fullCommand, "# WITHIN"));
        if (!chk) {
          if (bs != null && modelIndex >= 0) {
            bs.and(vwr.getModelUndeletedAtomsBitSet(modelIndex));
          }
          if (ptc == null)
            ptc = (bs == null ? new P3() : vwr.ms.getAtomSetCenter(bs));
          pts = getWithinDistanceVector(propertyList, distance, ptc, bs,
              isDisplay);
          sbCommand.append(" within ").appendF(distance).append(" ")
              .append(bs == null ? Escape.eP(ptc) : Escape.eBS(bs));
        }
        continue;
      case T.parameters:
        propertyName = "parameters";
        // if > 1 parameter, then first is assumed to be the cutoff. 
        float[] fparams = eval.floatParameterSet(++i, 1, 10);
        i = eval.iToken;
        propertyValue = fparams;
        sbCommand.append(" parameters ").append(Escape.eAF(fparams));
        break;
      case T.property:
      case T.variable:
        onlyOneModel = eval.theToken.value;
        boolean isVariable = (eval.theTok == T.variable);
        int tokProperty = tokAt(i + 1);
        if (mepOrMlp == null) { // not mlp or mep
          if (!surfaceObjectSeen && !isMapped && !planeSeen) {
            addShapeProperty(propertyList, "sasurface", Float.valueOf(0));
            //if (surfaceObjectSeen)
            sbCommand.append(" vdw");
            surfaceObjectSeen = true;
          }
          propertyName = "property";
          if (smoothing == null) {
            boolean allowSmoothing = T.tokAttr(tokProperty, T.floatproperty);
            smoothing = (allowSmoothing
                && vwr.getIsosurfacePropertySmoothing(false) == 1 ? Boolean.TRUE
                    : Boolean.FALSE);
          }
          addShapeProperty(propertyList, "propertySmoothing", smoothing);
          sbCommand.append(" isosurfacePropertySmoothing " + smoothing);
          if (smoothing == Boolean.TRUE) {
            if (smoothingPower == Integer.MAX_VALUE)
              smoothingPower = vwr.getIsosurfacePropertySmoothing(true);
            addShapeProperty(propertyList, "propertySmoothingPower",
                Integer.valueOf(smoothingPower));
            sbCommand
                .append(" isosurfacePropertySmoothingPower " + smoothingPower);
          }
          if (vwr.g.rangeSelected)
            addShapeProperty(propertyList, "rangeSelected", Boolean.TRUE);
        } else {
          propertyName = mepOrMlp;
        }
        str = paramAsStr(i);
        //        if (surfaceObjectSeen)
        sbCommand.append(" ").append(str);

        if (str.toLowerCase().indexOf("property_") == 0) {
          data = new float[vwr.ms.ac];
          if (chk)
            continue;
          data = (float[]) vwr.getDataObj(str, null,
              JmolDataManager.DATA_TYPE_AF);
          if (data == null)
            invArg();
          addShapeProperty(propertyList, propertyName, data);
          continue;
        }

        int ac = vwr.ms.ac;
        data = new float[ac];

        if (isVariable) {
          String vname = paramAsStr(++i);
          if (vname.length() == 0) {
            data = eval.floatParameterSet(i, ac, ac);
          } else {
            data = new float[ac];
            if (!chk)
              Parser.parseStringInfestedFloatArray(
                  "" + eval.getParameter(vname, T.string, true), null, data);
          }
          if (!chk/* && (surfaceObjectSeen)*/)
            sbCommand.append(" \"\" ").append(Escape.eAF(data));
        } else {
          getToken(++i);
          if (!chk) {
            sbCommand.append(" " + eval.theToken.value);
            Atom[] atoms = vwr.ms.at;
            vwr.autoCalculate(tokProperty, null);
            if (tokProperty != T.color) {
              pt = new P3();
              for (int iAtom = ac; --iAtom >= 0;)
                data[iAtom] = atoms[iAtom].atomPropertyFloat(vwr, tokProperty,
                    pt);
            }
          }
          if (tokProperty == T.color)
            colorScheme = "inherit";
          if (tokAt(i + 1) == T.within) {
            float d = floatParameter(i = i + 2);
            sbCommand.append(" within " + d);
            addShapeProperty(propertyList, "propertyDistanceMax",
                Float.valueOf(d));
          }
        }
        propertyValue = data;
        break;
      case T.modelindex:
      case T.model:
        if (surfaceObjectSeen)
          invArg();
        modelIndex = (eval.theTok == T.modelindex ? intParameter(++i)
            : eval.modelNumberParameter(++i));
        sbCommand.append(" modelIndex " + modelIndex);
        if (modelIndex < 0) {
          propertyName = "fixed";
          propertyValue = Boolean.TRUE;
          break;
        }
        propertyName = "modelIndex";
        propertyValue = Integer.valueOf(modelIndex);
        break;
      case T.select:
        // in general, vwr.getCurrentSelection() is used, but we may
        // override that here. But we have to be careful that
        // we PREPEND the selection to the command if no surface object
        // has been seen yet, and APPEND it if it has.
        propertyName = "select";
        BS bs1 = atomExpressionAt(++i);
        propertyValue = bs1;
        i = eval.iToken;
        boolean isOnly = (tokAt(i + 1) == T.only);
        if (isOnly) {
          i++;
          bsIgnore = BSUtil.copy(bs1);
          BSUtil.invertInPlace(bsIgnore, vwr.ms.ac);
          addShapeProperty(propertyList, "ignore", bsIgnore);
          sbCommand.append(" ignore ").append(Escape.eBS(bsIgnore));
          isFrontOnly = true;
        }
        if (surfaceObjectSeen || isMapped) {
          sbCommand.append(" select " + Escape.eBS(bs1));
        } else {
          bsSelect = (BS) propertyValue;
          if (modelIndex < 0 && bsSelect.nextSetBit(0) >= 0)
            modelIndex = vwr.ms.at[bsSelect.nextSetBit(0)].mi;
        }
        break;
      case T.set:
        thisSetNumber = intParameter(++i);
        break;
      case T.center:
        propertyName = "center";
        propertyValue = centerParameter(++i);
        sbCommand.append(" center " + Escape.eP((P3) propertyValue));
        i = eval.iToken;
        break;
      case T.sign:
      case T.color:
        idSeen = true;
        if (eval.theTok == T.sign) {
          isSign = true;
          sbCommand.append(" sign");
          addShapeProperty(propertyList, "sign", Boolean.TRUE);
        } else {
          if (tokAt(i + 1) == T.density) {
            i++;
            propertyName = "colorDensity";
            sbCommand.append(" color density");
            if (isFloatParameter(i + 1)) {
              float ptSize = floatParameter(++i);
              sbCommand.append(" " + ptSize);
              propertyValue = Float.valueOf(ptSize);
            }
            break;
          }
          /*
           * "color" now is just used as an equivalent to "sign" and as an
           * introduction to "absolute" any other use is superfluous; it has
           * been replaced with MAP for indicating "use the current surface"
           * because the term COLOR is too general.
           */

          if (getToken(i + 1).tok == T.string) {
            colorScheme = paramAsStr(++i);
            if (colorScheme.indexOf(" ") > 0) {
              discreteColixes = C.getColixArray(colorScheme);
              if (discreteColixes == null)
                error(ScriptError.ERROR_badRGBColor);
            }
          } else if (eval.theTok == T.mesh) {
            i++;
            sbCommand.append(" color mesh");
            color = eval.getArgbParam(++i);
            addShapeProperty(propertyList, "meshcolor", Integer.valueOf(color));
            sbCommand.append(" ").append(Escape.escapeColor(color));
            i = eval.iToken;
            continue;
          }
          if ((eval.theTok = tokAt(i + 1)) == T.translucent
              || eval.theTok == T.opaque) {
            sbCommand.append(" color");
            translucency = setColorOptions(sbCommand, i + 1,
                JC.SHAPE_ISOSURFACE, -2);
            i = eval.iToken;
            continue;
          }
          switch (tokAt(i + 1)) {
          case T.absolute:
          case T.range:
            getToken(++i);
            sbCommand.append(" color range");
            addShapeProperty(propertyList, "rangeAll", null);
            if (tokAt(i + 1) == T.all) {
              i++;
              sbCommand.append(" all");
              continue;
            }
            float min = floatParameter(++i);
            float max = floatParameter(++i);
            addShapeProperty(propertyList, "red", Float.valueOf(min));
            addShapeProperty(propertyList, "blue", Float.valueOf(max));
            sbCommand.append(" ").appendF(min).append(" ").appendF(max);
            continue;
          }
          if (eval.isColorParam(i + 1)) {
            color = eval.getArgbParam(i + 1);
            if (tokAt(i + 2) == T.to) {
              colorScheme = eval.getColorRange(i + 1);
              i = eval.iToken;
              break;
            }
          }
          sbCommand.append(" color");
        }
        if (eval.isColorParam(i + 1)) {
          color = eval.getArgbParam(++i);
          sbCommand.append(" ").append(Escape.escapeColor(color));
          i = eval.iToken;
          addShapeProperty(propertyList, "colorRGB", Integer.valueOf(color));
          idSeen = true;
          if (eval.isColorParam(i + 1)) {
            color = eval.getArgbParam(++i);
            i = eval.iToken;
            addShapeProperty(propertyList, "colorRGB", Integer.valueOf(color));
            sbCommand.append(" ").append(Escape.escapeColor(color));
            isBicolor = true;
          } else if (isSign) {
            invPO();
          }
        } else if (!isSign && discreteColixes == null && colorScheme == null) {
          invPO();
        }
        continue;
      case T.cache:
        if (!isIsosurface)
          invArg();
        toCache = !chk;
        continue;
      case T.file:
        if (tokAt(i + 1) != T.string)
          invPO();
        continue;
      case T.bondingradius:
      case T.vanderwaals:
        //if (surfaceObjectSeen)
        sbCommand.append(" ").appendO(eval.theToken.value);
        RadiusData rd = eval.encodeRadiusParameter(i, false, true);
        if (rd == null)
          return;
        //if (surfaceObjectSeen)
        sbCommand.append(" ").appendO(rd);
        if (Float.isNaN(rd.value))
          rd.value = 100;
        propertyValue = rd;
        propertyName = "radius";
        haveRadius = true;
        if (isMapped)
          surfaceObjectSeen = false;
        i = eval.iToken;
        break;
      case T.plane:
        // plane {X, Y, Z, W}
        planeSeen = true;
        propertyName = "plane";
        propertyValue = eval.planeParameter(i);
        i = eval.iToken;
        //if (surfaceObjectSeen)
        sbCommand.append(" plane ").append(Escape.eP4((P4) propertyValue));
        break;
      case T.scale:
        propertyName = "scale";
        propertyValue = Float.valueOf(floatParameter(++i));
        sbCommand.append(" scale ").appendO(propertyValue);
        break;
      case T.all:
        if (idSeen)
          invArg();
        propertyName = "thisID";
        break;
      case T.ellipsoid:
        // ellipsoid {xc yc zc f} where a = b and f = a/c
        // NOT OR ellipsoid {u11 u22 u33 u12 u13 u23}
        surfaceObjectSeen = true;
        ++i;
        //        ignoreError = true;
        //      try {
        propertyValue = eval.getPoint4f(i);
        propertyName = "ellipsoid";
        i = eval.iToken;
        sbCommand.append(" ellipsoid ").append(Escape.eP4((P4) propertyValue));
        break;
      //        } catch (Exception e) {
      //        }
      //        try {
      //          propertyName = "ellipsoid";
      //          propertyValue = eval.floatParameterSet(i, 6, 6);
      //          i = eval.iToken;
      //          sbCommand.append(" ellipsoid ").append(
      //              Escape.eAF((float[]) propertyValue));
      //          break;
      //        } catch (Exception e) {
      //        }
      //        ignoreError = false;
      //        bs = atomExpressionAt(i);
      //        sbCommand.append(" ellipsoid ").append(Escape.eBS(bs));
      //        int iAtom = bs.nextSetBit(0);
      //        if (iAtom < 0)
      //          return;
      //        Atom[] atoms = vwr.modelSet.atoms;
      //        Tensor[] tensors = atoms[iAtom].getTensors();
      //        if (tensors == null || tensors.length < 1 || tensors[0] == null
      //            || (propertyValue = vwr.getQuadricForTensor(tensors[0], null)) == null)
      //          return;
      //        i = eval.iToken;
      //        propertyName = "ellipsoid";
      //        if (!chk)
      //          addShapeProperty(propertyList, "center", vwr.getAtomPoint3f(iAtom));
      //        break;
      case T.hkl:
        // miller indices hkl
        planeSeen = true;
        propertyName = "plane";
        propertyValue = eval.hklParameter(++i);
        i = eval.iToken;
        sbCommand.append(" plane ").append(Escape.eP4((P4) propertyValue));
        break;
      case T.lcaocartoon:
        surfaceObjectSeen = true;
        String lcaoType = paramAsStr(++i);
        addShapeProperty(propertyList, "lcaoType", lcaoType);
        sbCommand.append(" lcaocartoon ").append(PT.esc(lcaoType));
        switch (getToken(++i).tok) {
        case T.define:
        case T.bitset:
        case T.expressionBegin:
          // automatically selects just the model of the first atom in the set.
          propertyName = "lcaoCartoon";
          bs = atomExpressionAt(i);
          i = eval.iToken;
          if (chk)
            continue;
          int atomIndex = bs.nextSetBit(0);
          if (atomIndex < 0)
            error(ScriptError.ERROR_expressionExpected);
          sbCommand.append(" ({").appendI(atomIndex).append("})");
          modelIndex = vwr.ms.at[atomIndex].mi;
          addShapeProperty(propertyList, "modelIndex",
              Integer.valueOf(modelIndex));
          V3[] axes = { new V3(), new V3(), V3.newV(vwr.ms.at[atomIndex]),
              new V3() };
          if (!lcaoType.equalsIgnoreCase("s")
              && vwr.getHybridizationAndAxes(atomIndex, axes[0], axes[1],
                  lcaoType) == null)
            return;
          propertyValue = axes;
          break;
        default:
          error(ScriptError.ERROR_expressionExpected);
        }
        break;
      case T.nbo:
        nbotype = paramAsStr(++i).toUpperCase();
        sbCommand.append(" nbo ").append(nbotype).append(" ");
        //$FALL-THROUGH$
      case T.mo:
        if (nbotype == null)
          sbCommand.append(" mo ");
        // mo 1-based-index
        int moNumber = Integer.MAX_VALUE;
        int offset = Integer.MAX_VALUE;
        boolean isNegOffset = (tokAt(i + 1) == T.minus);
        if (isNegOffset)
          i++;
        float[] linearCombination = null;
        switch (tokAt(++i)) {
        case T.nada:
          eval.bad();
          break;
        case T.density:
          sbCommand.append("[1] squared ");
          addShapeProperty(propertyList, "squareLinear", Boolean.TRUE);
          linearCombination = new float[] { 1 };
          offset = moNumber = 0;
          i++;
          break;
        case T.homo:
        case T.lumo:
          offset = moOffset(i);
          moNumber = 0;
          i = eval.iToken;
          //if (surfaceObjectSeen) {
          sbCommand.append((isNegOffset ? "-" : "") + "HOMO ");
          if (offset > 0)
            sbCommand.append("+");
          if (offset != 0)
            sbCommand.appendI(offset);
          //}
          break;
        case T.integer:
          moNumber = intParameter(i);
          //if (surfaceObjectSeen)
          sbCommand.appendI(moNumber);
          if (tokAt(i + 1) == T.beta) {
            isBeta = true;
            i++;
          }
          break;
        default:
          if (eval.isArrayParameter(i)) {
            linearCombination = eval.floatParameterSet(i, 1, Integer.MAX_VALUE);
            i = eval.iToken;
          }
        }
        boolean squared = (tokAt(i + 1) == T.squared);
        if (squared) {
          addShapeProperty(propertyList, "squareLinear", Boolean.TRUE);
          sbCommand.append(" squared");
          if (linearCombination == null)
            linearCombination = new float[0];
        } else if (tokAt(i + 1) == T.point) {
          ++i;
          int monteCarloCount = intParameter(++i);
          int seed = (tokAt(i + 1) == T.integer ? intParameter(++i)
              : ((int) -System.currentTimeMillis()) % 10000);
          addShapeProperty(propertyList, "monteCarloCount",
              Integer.valueOf(monteCarloCount));
          addShapeProperty(propertyList, "randomSeed", Integer.valueOf(seed));
          sbCommand.append(" points ").appendI(monteCarloCount).appendC(' ')
              .appendI(seed);
        }
        setMoData(propertyList, moNumber, linearCombination, offset,
            isNegOffset, modelIndex, null, nbotype, isBeta);
        surfaceObjectSeen = true;
        continue;
      case T.nci:
        propertyName = "nci";
        //if (surfaceObjectSeen)
        sbCommand.append(" " + propertyName);
        int tok = tokAt(i + 1);
        boolean isPromolecular = (tok != T.file && tok != T.string
            && tok != T.mrc);
        propertyValue = Boolean.valueOf(isPromolecular);
        if (isPromolecular)
          surfaceObjectSeen = true;
        break;
      case T.mep:
      case T.mlp:
        boolean isMep = (eval.theTok == T.mep);
        propertyName = (isMep ? "mep" : "mlp");
        //if (surfaceObjectSeen)
        sbCommand.append(" " + propertyName);
        String fname = null;
        int calcType = -1;
        surfaceObjectSeen = true;
        if (tokAt(i + 1) == T.integer) {
          calcType = intParameter(++i);
          sbCommand.append(" " + calcType);
          addShapeProperty(propertyList, "mepCalcType",
              Integer.valueOf(calcType));
        }
        if (tokAt(i + 1) == T.string) {
          fname = stringParameter(++i);
          //if (surfaceObjectSeen)
          sbCommand.append(" /*file*/" + PT.esc(fname));
        } else if (tokAt(i + 1) == T.property) {
          mepOrMlp = propertyName;
          continue;
        }
        if (!chk)
          try {
            data = (fname == null && isMep
                ? vwr.getOrCalcPartialCharges(bsSelect, bsIgnore)
                : getAtomicPotentials(bsSelect, bsIgnore, fname));
          } catch (JmolAsyncException e1) {
            throw new ScriptInterruption(e, "partialcharge", 1);
          }
        if (!chk && data == null)
          error(ScriptError.ERROR_noPartialCharges);
        propertyValue = data;
        break;
      case T.volume:
        doCalcVolume = !chk;
        sbCommand.append(" volume");
        break;
      case T.id:
        setShapeId(iShape, ++i, idSeen);
        isWild = (getShapeProperty(iShape, "ID") == null);
        i = eval.iToken;
        break;
      case T.colorscheme:
        // either order NOT OK -- documented for TRANSLUCENT "rwb"
        if (tokAt(i + 1) == T.translucent) {
          isColorSchemeTranslucent = true;
          i++;
        }
        colorScheme = paramAsStr(++i).toLowerCase();
        if (colorScheme.equals("sets")) {
          sbCommand.append(" colorScheme \"sets\"");
        } else if (eval.isColorParam(i)) {
          colorScheme = eval.getColorRange(i);
          i = eval.iToken;
        }
        break;
      case T.addhydrogens:
        propertyName = "addHydrogens";
        propertyValue = Boolean.TRUE;
        sbCommand.append(" addHydrogens");
        break;
      case T.angstroms:
        propertyName = "angstroms";
        sbCommand.append(" angstroms");
        break;
      case T.anisotropy:
        propertyName = "anisotropy";
        propertyValue = getPoint3f(++i, false);
        sbCommand.append(" anisotropy").append(Escape.eP((P3) propertyValue));
        i = eval.iToken;
        break;
      case T.area:
        doCalcArea = !chk;
        sbCommand.append(" area");
        break;
      case T.atomicorbital:
      case T.orbital:
        surfaceObjectSeen = true;
        if (isBicolor && !isPhased) {
          sbCommand.append(" phase \"_orb\"");
          addShapeProperty(propertyList, "phase", "_orb");
        }
        float[] nlmZprs = new float[7];
        nlmZprs[0] = intParameter(++i);
        nlmZprs[1] = intParameter(++i);
        nlmZprs[2] = intParameter(++i);
        nlmZprs[3] = (isFloatParameter(i + 1) ? floatParameter(++i) : 6f);
        //if (surfaceObjectSeen)
        sbCommand.append(" atomicOrbital ").appendI((int) nlmZprs[0])
            .append(" ").appendI((int) nlmZprs[1]).append(" ")
            .appendI((int) nlmZprs[2]).append(" ").appendF(nlmZprs[3]);
        if (tokAt(i + 1) == T.point) {
          i += 2;
          nlmZprs[4] = intParameter(i);
          nlmZprs[5] = (tokAt(i + 1) == T.decimal ? floatParameter(++i) : 0);
          nlmZprs[6] = (tokAt(i + 1) == T.integer ? intParameter(++i)
              : ((int) -System.currentTimeMillis()) % 10000);
          //if (surfaceObjectSeen)
          sbCommand.append(" points ").appendI((int) nlmZprs[4]).appendC(' ')
              .appendF(nlmZprs[5]).appendC(' ').appendI((int) nlmZprs[6]);
        }
        propertyName = "hydrogenOrbital";
        propertyValue = nlmZprs;
        break;
      case T.binary:
        sbCommand.append(" binary");
        // for PMESH, specifically
        // ignore for now
        continue;
      case T.blockdata:
        sbCommand.append(" blockData");
        propertyName = "blockData";
        propertyValue = Boolean.TRUE;
        break;
      case T.cap:
      case T.slab:
        haveSlab = true;
        propertyName = (String) eval.theToken.value;
        propertyValue = getCapSlabObject(i, false);
        i = eval.iToken;
        break;
      case T.cavity:
        if (!isIsosurface)
          invArg();
        isCavity = true;
        float cavityRadius = (isFloatParameter(i + 1) ? floatParameter(++i)
            : 1.2f);
        float envelopeRadius = (isFloatParameter(i + 1) ? floatParameter(++i)
            : 10f);
        if (chk)
          continue;
        if (envelopeRadius > 10f) {
          eval.integerOutOfRange(0, 10);
          return;
        }
        sbCommand.append(" cavity ").appendF(cavityRadius).append(" ")
            .appendF(envelopeRadius);
        addShapeProperty(propertyList, "envelopeRadius",
            Float.valueOf(envelopeRadius));
        addShapeProperty(propertyList, "cavityRadius",
            Float.valueOf(cavityRadius));
        propertyName = "cavity";
        break;
      case T.contour:
      case T.contours:
        propertyName = "contour";
        sbCommand.append(" contour");
        switch (tokAt(i + 1)) {
        case T.discrete:
          propertyValue = eval.floatParameterSet(i + 2, 1, Integer.MAX_VALUE);
          sbCommand.append(" discrete ")
              .append(Escape.eAF((float[]) propertyValue));
          i = eval.iToken;
          break;
        case T.increment:
          pt = getPoint3f(i + 2, false);
          if (pt.z <= 0 || pt.y < pt.x)
            invArg(); // from to step
          if (pt.z == (int) pt.z && pt.z > (pt.y - pt.x))
            pt.z = (pt.y - pt.x) / pt.z;
          propertyValue = pt;
          i = eval.iToken;
          sbCommand.append(" increment ").append(Escape.eP(pt));
          break;
        default:
          propertyValue = Integer
              .valueOf(tokAt(i + 1) == T.integer ? intParameter(++i) : 0);
          sbCommand.append(" ").appendO(propertyValue);
          if (tokAt(i + 1) == T.integer) {
            addShapeProperty(propertyList, propertyName, propertyValue);
            propertyValue = Integer.valueOf(-Math.abs(intParameter(++i)));
            sbCommand.append(" ").appendO(propertyValue);
          }
        }
        break;
      case T.cutoff:
        sbCommand.append(" cutoff ");
        if (tokAt(++i) == T.plus) {
          propertyName = "cutoffPositive";
          propertyValue = Float.valueOf(cutoff = floatParameter(++i));
          sbCommand.append("+").appendO(propertyValue);
        } else if (isFloatParameter(i)) {
          propertyName = "cutoff";
          propertyValue = Float.valueOf(cutoff = floatParameter(i));
          sbCommand.appendO(propertyValue);
        } else {
          propertyName = "cutoffRange";
          propertyValue = eval.floatParameterSet(i, 2, 2);
          addShapeProperty(propertyList, "cutoff", Float.valueOf(0));
          sbCommand.append(Escape.eAF((float[]) propertyValue));
          i = eval.iToken;
        }
        break;
      case T.downsample:
        propertyName = "downsample";
        propertyValue = Integer.valueOf(intParameter(++i));
        //if (surfaceObjectSeen)
        sbCommand.append(" downsample ").appendO(propertyValue);
        break;
      case T.eccentricity:
        propertyName = "eccentricity";
        propertyValue = eval.getPoint4f(++i);
        //if (surfaceObjectSeen)
        sbCommand.append(" eccentricity ")
            .append(Escape.eP4((P4) propertyValue));
        i = eval.iToken;
        break;
      case T.ed:
        sbCommand.append(" ed");
        // electron density - never documented
        setMoData(propertyList, -1, null, 0, false, modelIndex, null, null,
            false);
        surfaceObjectSeen = true;
        continue;
      case T.debug:
      case T.nodebug:
        sbCommand.append(" ").appendO(eval.theToken.value);
        propertyName = "debug";
        propertyValue = (eval.theTok == T.debug ? Boolean.TRUE : Boolean.FALSE);
        break;
      case T.fixed:
        sbCommand.append(" fixed");
        propertyName = "fixed";
        propertyValue = Boolean.TRUE;
        break;
      case T.fullplane:
        sbCommand.append(" fullPlane");
        propertyName = "fullPlane";
        propertyValue = Boolean.TRUE;
        break;
      case T.functionxy:
      case T.functionxyz:
        // isosurface functionXY "functionName"|"data2d_xxxxx"
        // isosurface functionXYZ "functionName"|"data3d_xxxxx"
        // {origin} {ni ix iy iz} {nj jx jy jz} {nk kx ky kz}
        // or
        // isosurface origin.. step... count... functionXY[Z] = "x + y + z"
        boolean isFxyz = (eval.theTok == T.functionxyz);
        propertyName = "" + eval.theToken.value;
        Lst<Object> vxy = new Lst<Object>();
        propertyValue = vxy;
        isFxy = surfaceObjectSeen = true;
        //if (surfaceObjectSeen)
        sbCommand.append(" ").append(propertyName);
        String name = paramAsStr(++i);
        if (name.equals("=")) {
          //if (surfaceObjectSeen)
          sbCommand.append(" =");
          name = paramAsStr(++i);
          //if (surfaceObjectSeen)
          sbCommand.append(" ").append(PT.esc(name));
          vxy.addLast(name);
          if (!chk)
            addShapeProperty(propertyList, "func",
                createFunction("__iso__", "x,y,z", name));
          //surfaceObjectSeen = true;
          break;
        }
        // override of function or data name when saved as a state
        String dName = PT.getQuotedAttribute(eval.fullCommand,
            "# DATA" + (isFxy ? "2" : ""));
        if (dName == null)
          dName = "inline";
        else
          name = dName;
        boolean isXYZ = (name.indexOf("data2d_") == 0);
        boolean isXYZV = (name.indexOf("data3d_") == 0);
        isInline = name.equals("inline");
        //if (!surfaceObjectSeen)
        sbCommand.append(" inline");
        vxy.addLast(name); // (0) = name
        P3 pt3 = getPoint3f(++i, false);
        //if (!surfaceObjectSeen)
        sbCommand.append(" ").append(Escape.eP(pt3));
        vxy.addLast(pt3); // (1) = {origin}
        P4 pt4;
        ptX = ++eval.iToken;
        vxy.addLast(pt4 = eval.getPoint4f(ptX)); // (2) = {ni ix iy iz}
        //if (!surfaceObjectSeen)
        sbCommand.append(" ").append(Escape.eP4(pt4));
        nX = (int) pt4.x;
        ptY = ++eval.iToken;
        vxy.addLast(pt4 = eval.getPoint4f(ptY)); // (3) = {nj jx jy jz}
        //if (!surfaceObjectSeen)
        sbCommand.append(" ").append(Escape.eP4(pt4));
        nY = (int) pt4.x;
        vxy.addLast(pt4 = eval.getPoint4f(++eval.iToken)); // (4) = {nk kx ky kz}
        //if (!surfaceObjectSeen)
        sbCommand.append(" ").append(Escape.eP4(pt4));
        nZ = (int) pt4.x;

        if (nX == 0 || nY == 0 || nZ == 0)
          invArg();
        if (!chk) {
          float[][] fdata = null;
          float[][][] xyzdata = null;
          if (isFxyz) {
            if (isInline) {
              nX = Math.abs(nX);
              nY = Math.abs(nY);
              nZ = Math.abs(nZ);
              xyzdata = floatArraySetXYZ(++eval.iToken, nX, nY, nZ);
            } else if (isXYZV) {
              xyzdata = (float[][][]) vwr.getDataObj(name, null,
                  JmolDataManager.DATA_TYPE_AFFF);
            } else {
              xyzdata = vwr.functionXYZ(name, nX, nY, nZ);
            }
            nX = Math.abs(nX);
            nY = Math.abs(nY);
            nZ = Math.abs(nZ);
            if (xyzdata == null) {
              eval.iToken = ptX;
              eval.errorStr(ScriptError.ERROR_what, "xyzdata is null.");
            }
            if (xyzdata.length != nX || xyzdata[0].length != nY
                || xyzdata[0][0].length != nZ) {
              eval.iToken = ptX;
              eval.errorStr(ScriptError.ERROR_what,
                  "xyzdata[" + xyzdata.length + "][" + xyzdata[0].length + "]["
                      + xyzdata[0][0].length + "] is not of size [" + nX + "]["
                      + nY + "][" + nZ + "]");
            }
            vxy.addLast(xyzdata); // (5) = float[][][] data
            //if (!surfaceObjectSeen)
            sbCommand.append(" ").append(Escape.e(xyzdata));
          } else {
            if (isInline) {
              nX = Math.abs(nX);
              nY = Math.abs(nY);
              fdata = floatArraySet(++eval.iToken, nX, nY);
            } else if (isXYZ) {
              fdata = (float[][]) vwr.getDataObj(name, null,
                  JmolDataManager.DATA_TYPE_AFF);
              nX = (fdata == null ? 0 : fdata.length);
              nY = 3;
            } else {
              fdata = vwr.functionXY(name, nX, nY);
              nX = Math.abs(nX);
              nY = Math.abs(nY);
            }
            if (fdata == null) {
              eval.iToken = ptX;
              eval.errorStr(ScriptError.ERROR_what, "fdata is null.");
            }
            if (fdata.length != nX && !isXYZ) {
              eval.iToken = ptX;
              eval.errorStr(ScriptError.ERROR_what,
                  "fdata length is not correct: " + fdata.length + " " + nX
                      + ".");
            }
            for (int j = 0; j < nX; j++) {
              if (fdata[j] == null) {
                eval.iToken = ptY;
                eval.errorStr(ScriptError.ERROR_what,
                    "fdata[" + j + "] is null.");
              }
              if (fdata[j].length != nY) {
                eval.iToken = ptY;
                eval.errorStr(ScriptError.ERROR_what,
                    "fdata[" + j + "] is not the right length: "
                        + fdata[j].length + " " + nY + ".");
              }
            }
            vxy.addLast(fdata); // (5) = float[][] data
            //if (!surfaceObjectSeen)
            sbCommand.append(" ").append(Escape.e(fdata));
          }
        }
        i = eval.iToken;
        break;
      case T.val:
        boolean isBS = e.isAtomExpression(++i);
        P3[] probes = getAllPoints(i, 1);
        sbCommand.append(" value " + (isBS ? e.atomExpressionAt(i).toString() : Escape.eAP(probes)));
        propertyName = "probes";
        propertyValue = probes;
        i = e.iToken;
        break;
      case T.gridpoints:
        propertyName = "gridPoints";
        sbCommand.append(" gridPoints");
        break;
      case T.ignore:
        propertyName = "ignore";
        propertyValue = bsIgnore = atomExpressionAt(++i);
        sbCommand.append(" ignore ").append(Escape.eBS(bsIgnore));
        i = eval.iToken;
        break;
      case T.insideout:
        propertyName = "insideOut";
        sbCommand.append(" insideout");
        break;
      case T.internal:
      case T.interior:
      case T.pocket:
        //if (!surfaceObjectSeen)
        sbCommand.append(" ").appendO(eval.theToken.value);
        propertyName = "pocket";
        propertyValue = (eval.theTok == T.pocket ? Boolean.TRUE
            : Boolean.FALSE);
        break;
      case T.lobe:
        // lobe {eccentricity}
        propertyName = "lobe";
        propertyValue = eval.getPoint4f(++i);
        i = eval.iToken;
        //if (!surfaceObjectSeen)
        sbCommand.append(" lobe ").append(Escape.eP4((P4) propertyValue));
        surfaceObjectSeen = true;
        break;
      case T.lonepair:
      case T.lp:
        // lp {eccentricity}
        propertyName = "lp";
        propertyValue = eval.getPoint4f(++i);
        i = eval.iToken;
        //if (!surfaceObjectSeen)
        sbCommand.append(" lp ").append(Escape.eP4((P4) propertyValue));
        surfaceObjectSeen = true;
        break;
      case T.mapproperty:
        if (isMapped || slen == i + 1)
          invArg();
        isMapped = true;
        if ((isCavity || haveRadius || haveIntersection)
            && !surfaceObjectSeen) {
          surfaceObjectSeen = true;
          addShapeProperty(propertyList, "bsSolvent",
              (haveRadius || haveIntersection ? new BS()
                  : eval.lookupIdentifierValue("solvent")));
          addShapeProperty(propertyList, "sasurface", Float.valueOf(0));
        }
        if (sbCommand.length() == 0) {
          plane = (P4) getShapeProperty(JC.SHAPE_ISOSURFACE, "plane");
          if (plane == null) {
            if (getShapeProperty(JC.SHAPE_ISOSURFACE, "contours") != null) {
              addShapeProperty(propertyList, "nocontour", null);
            }
          } else {
            addShapeProperty(propertyList, "plane", plane);
            sbCommand.append("plane ").append(Escape.eP4(plane));
            planeSeen = true;
            plane = null;
          }
        } else if (!surfaceObjectSeen && !planeSeen) {
          invArg();
        }
        sbCommand.append("; isosurface map");
        addShapeProperty(propertyList, "map",
            (surfaceObjectSeen ? Boolean.TRUE : Boolean.FALSE));
        break;
      case T.maxset:
        propertyName = "maxset";
        propertyValue = Integer.valueOf(intParameter(++i));
        sbCommand.append(" maxSet ").appendO(propertyValue);
        break;
      case T.minset:
        propertyName = "minset";
        propertyValue = Integer.valueOf(intParameter(++i));
        sbCommand.append(" minSet ").appendO(propertyValue);
        break;
      case T.radical:
        // rad {eccentricity}
        surfaceObjectSeen = true;
        propertyName = "rad";
        propertyValue = eval.getPoint4f(++i);
        i = eval.iToken;
        //if (!surfaceObjectSeen)
        sbCommand.append(" radical ").append(Escape.eP4((P4) propertyValue));
        break;
      case T.modelbased:
        propertyName = "fixed";
        propertyValue = Boolean.FALSE;
        sbCommand.append(" modelBased");
        break;
      case T.molecular:
      case T.sasurface:
      case T.solvent:
        onlyOneModel = eval.theToken.value;
        float radius;
        if (eval.theTok == T.molecular) {
          propertyName = "molecular";
          sbCommand.append(" molecular");
          radius = (isFloatParameter(i + 1) ? floatParameter(++i) : 1.4f);
        } else {
          addShapeProperty(propertyList, "bsSolvent",
              eval.lookupIdentifierValue("solvent"));
          propertyName = (eval.theTok == T.sasurface ? "sasurface" : "solvent");
          sbCommand.append(" ").appendO(eval.theToken.value);
          radius = (isFloatParameter(i + 1) ? floatParameter(++i)
              : vwr.getFloat(T.solventproberadius));
        }
        sbCommand.append(" ").appendF(radius);
        propertyValue = Float.valueOf(radius);
        if (tokAt(i + 1) == T.full) {
          addShapeProperty(propertyList, "doFullMolecular", null);
          //if (!surfaceObjectSeen)
          sbCommand.append(" full");
          i++;
        }
        surfaceObjectSeen = true;
        break;
      case T.mrc:
        addShapeProperty(propertyList, "fileType", "Mrc");
        sbCommand.append(" mrc");
        continue;
      case T.object:
      case T.obj:
        addShapeProperty(propertyList, "fileType", "Obj");
        sbCommand.append(" obj");
        continue;
      case T.msms:
        addShapeProperty(propertyList, "fileType", "Msms");
        sbCommand.append(" msms");
        continue;
      case T.phase:
        if (surfaceObjectSeen)
          invArg();
        propertyName = "phase";
        isPhased = true;
        propertyValue = (tokAt(i + 1) == T.string ? stringParameter(++i)
            : "_orb");
        sbCommand.append(" phase ").append(PT.esc((String) propertyValue));
        break;
      case T.pointsperangstrom:
      case T.resolution:
        propertyName = "resolution";
        propertyValue = Float.valueOf(floatParameter(++i));
        sbCommand.append(" resolution ").appendO(propertyValue);
        break;
      case T.reversecolor:
        propertyName = "reverseColor";
        propertyValue = Boolean.TRUE;
        sbCommand.append(" reversecolor");
        break;
      case T.rmsd:
      case T.sigma:
        propertyName = "sigma";
        propertyValue = Float.valueOf(sigma = floatParameter(++i));
        sbCommand.append(" sigma ").appendO(propertyValue);
        break;
      case T.geosurface:
        // geosurface [radius]
        propertyName = "geodesic";
        propertyValue = Float.valueOf(floatParameter(++i));
        //if (!surfaceObjectSeen)
        sbCommand.append(" geosurface ").appendO(propertyValue);
        surfaceObjectSeen = true;
        break;
      case T.sphere:
        // sphere [radius]
        propertyName = "sphere";
        propertyValue = Float.valueOf(floatParameter(++i));
        //if (!surfaceObjectSeen)
        sbCommand.append(" sphere ").appendO(propertyValue);
        surfaceObjectSeen = true;
        break;
      case T.squared:
        propertyName = "squareData";
        propertyValue = Boolean.TRUE;
        sbCommand.append(" squared");
        break;
      case T.inline:
        propertyName = (!surfaceObjectSeen && !planeSeen && !isMapped
            ? "readFile"
            : "mapColor");
        str = stringParameter(++i);
        if (str == null)
          invArg();
        // inline PMESH data
        if (isPmesh)
          str = PT.replaceWithCharacter(str, "{,}|", ' ');
        if (eval.debugHigh)
          Logger.debug("pmesh inline data:\n" + str);
        propertyValue = (chk ? null : str);
        addShapeProperty(propertyList, "fileName", "");
        sbCommand.append(" INLINE ").append(PT.esc(str));
        surfaceObjectSeen = true;
        break;
      case T.leftsquare:
      case T.spacebeforesquare:
      case T.varray:
        // [ float, filename, float, filename, ...]
        if (filesData != null || isWild)
          invArg();
        Lst<Object> list = eval.listParameter4(i, 2, Integer.MAX_VALUE, true);
        i = eval.iToken;
        int n = list.size() / 2;
        if (n == 0 || n * 2 != list.size())
          invArg();
        String[] files = new String[n];
        float[] factors = new float[n];
        sbCommand.append("[");
        try {
          for (int j = 0, ptf = 0; j < n; j++) {
            factors[j] = ((Float) list.get(ptf++)).floatValue();
            files[j] = e.checkFileExists("ISOSURFACE_" + j + "_", false, (String) list.get(ptf++), i, false);
            sbCommand.appendF(factors[j]);
            sbCommand.append(" /*file*/").append(PT.esc(files[j]));
          }
          sbCommand.append("]");
        } catch (Exception e) {
          invArg();
        }
        filesData = new Object[] { files, factors };
        propertyName = (!surfaceObjectSeen && !planeSeen && !isMapped ? "readFile" : "mapColor");
        surfaceObjectSeen = true;
        if (chk)
          break;
        addShapeProperty(propertyList, "filesData", filesData);
        break;
      case T.eds:
      case T.edsdiff:
      case T.string:
        boolean firstPass = (!surfaceObjectSeen && !planeSeen);
        String filename;
        propertyName = (firstPass && !isMapped ? "readFile" : "mapColor");
        if (eval.theTok == T.string) {
          filename = paramAsStr(i);
        } else {
          String pdbID = vwr.getPdbID();
          if (pdbID == null)
            eval.errorStr(ScriptError.ERROR_invalidArgument,
                "no PDBID available");
          filename = "*" + (eval.theTok == T.edsdiff ? "*" : "") + pdbID;
        }

        /*
         * A file name, optionally followed by a calculation type and/or an integer file index.
         * Or =xxxx, an EDM from Uppsala Electron Density Server
         * If the model auxiliary info has "jmolSufaceInfo", we use that.
         */
        boolean checkWithin = false;
        boolean isUppsala = false;
        if (filename.startsWith("http://eds.bmc.uu.se/eds/dfs/cb/")
            && filename.endsWith(".omap")) {
          // decommissoning Uppsala
          filename = (filename.indexOf("_diff") >= 0 ? "*" : "") + "*"
              + filename.substring(32, 36);
          //"http://eds.bmc.uu.se/eds/dfs/cb/1cbs/1cbs_diff.omap"
          // 0         1         2         3 xxxx
          // 0123456789012345678901234567890123456789
        }
        if (filename.startsWith("*") || (isUppsala = filename.startsWith("="))
            && filename.length() > 1) {
          if (isUppsala) // Uppsala EDS decommissioned
            filename = filename.replace('=', '*');
          // new PDB ccp4 option
          boolean isFull = (filename.indexOf("/full") >= 0);
          if (filename.indexOf("/diff") >= 0)
            filename = "*" + filename.substring(0, filename.indexOf("/diff"));
          if (filename.startsWith("**")) {
            if (Float.isNaN(sigma))
              addShapeProperty(propertyList, "sigma", Float.valueOf(sigma = 3));
            if (!isSign) {
              isSign = true;
              sbCommand.append(" sign");
              addShapeProperty(propertyList, "sign", Boolean.TRUE);
            }
          }
          if (!Float.isNaN(sigma))
            showString("using cutoff = " + sigma + " sigma");
          filename = (String) vwr.setLoadFormat(filename,
              (isFull || pts == null ? '_' : '-'), false);
          // the initial dummy call just asertains that 
          checkWithin = !isFull;
        }
        if (checkWithin) {
          if (pts == null && ptWithin == 0) {
            onlyOneModel = filename;
            if (modelIndex < 0)
              modelIndex = vwr.am.cmi;
            bs = vwr.getModelUndeletedAtomsBitSet(modelIndex);
            if (bs.nextSetBit(0) >= 0) {
              pts = getWithinDistanceVector(propertyList, 2.0f, null, bs,
                  false);
              sbCommand.append(" within 2.0 ").append(Escape.eBS(bs));
            }
          }
          if (pts != null && filename.indexOf("/0,0,0/0,0,0?") >= 0) {
            filename = filename.replace("0,0,0/0,0,0",
                pts[0].x + "," + pts[0].y + "," + pts[0].z + "/"
                    + pts[pts.length - 1].x + "," + pts[pts.length - 1].y + ","
                    + pts[pts.length - 1].z);
          }
          if (firstPass)
            defaultMesh = true;
        }
        if (firstPass && vwr.getP("_fileType").equals("Pdb")
            && Float.isNaN(sigma) && Float.isNaN(cutoff)) {
          // negative sigma just indicates that 
          addShapeProperty(propertyList, "sigma", Float.valueOf(-1));
          sbCommand.append(" sigma -1.0");
        }
        if (filename.length() == 0) {
          if (modelIndex < 0)
            modelIndex = vwr.am.cmi;
          filename = eval.getFullPathName();
          propertyValue = vwr.ms.getInfo(modelIndex, "jmolSurfaceInfo"); // not implemented?
        }
        int fileIndex = -1;
        if (propertyValue == null && tokAt(i + 1) == T.integer)
          addShapeProperty(propertyList, "fileIndex",
              Integer.valueOf(fileIndex = intParameter(++i)));
        String stype = (tokAt(i + 1) == T.string ? stringParameter(++i) : null);
        // done reading parameters
        surfaceObjectSeen = true;
        if (chk) {
          break;
        }
        String[] fullPathNameOrError;
        String localName = null;
        if (propertyValue == null) {
          if (eval.fullCommand.indexOf("# FILE" + nFiles + "=") >= 0) {
            // old way, abandoned
            filename = PT.getQuotedAttribute(eval.fullCommand,
                "# FILE" + nFiles);
            if (tokAt(i + 1) == T.as)
              i += 2; // skip that
          } else if (tokAt(i + 1) == T.as) {
            localName = vwr.fm.getFilePath(
                stringParameter(eval.iToken = (i = i + 2)), false, false);
            fullPathNameOrError = vwr.getFullPathNameOrError(localName);
            localName = fullPathNameOrError[0];
            if (vwr.fm.getPathForAllFiles() != "") {
              // we use the LOCAL name when reading from a local path only (in the case of JMOL files)
              filename = localName;
              localName = null;
            } else {
              addShapeProperty(propertyList, "localName", localName);
            }
          }
        }
        // just checking here, and getting the full path name
        if (stype == null) {
          filename = e.checkFileExists("ISOSURFACE_" + (isMapped ? "MAP_" : ""), false, filename, i, false);
        }
        showString("reading isosurface data from " + filename);

        if (stype != null) {
          propertyValue = vwr.fm.cacheGet(filename, false);
          addShapeProperty(propertyList, "calculationType", stype);
        }
        if (propertyValue == null) {
          addShapeProperty(propertyList, "fileName", filename);
          if (localName != null)
            filename = localName;
          if (fileIndex >= 0)
            sbCommand.append(" ").appendI(fileIndex);
        }
        sbCommand.append(" /*file*/").append(PT.esc(filename));
        if (stype != null)
          sbCommand.append(" ").append(PT.esc(stype));
        break;
      case T.connect:
        propertyName = "connections";
        switch (tokAt(++i)) {
        case T.define:
        case T.bitset:
        case T.expressionBegin:
          propertyValue = new int[] { atomExpressionAt(i).nextSetBit(0) };
          break;
        default:
          propertyValue = new int[] {
              (int) eval.floatParameterSet(i, 1, 1)[0] };
          break;
        }
        i = eval.iToken;
        break;
      case T.atomindex:
        propertyName = "atomIndex";
        propertyValue = Integer.valueOf(intParameter(++i));
        break;
      case T.link:
        propertyName = "link";
        sbCommand.append(" link");
        break;
      case T.unitcell:
        if (iShape != JC.SHAPE_ISOSURFACE)
          invArg();
        if (tokAt(i + 1) == T.plus)
          i++;
        propertyName = "extendGrid";
        propertyValue = Float.valueOf(floatParameter(++i));
        sbCommand.append(" unitcell " + propertyValue);
        break;
      case T.lattice:
        if (iShape != JC.SHAPE_ISOSURFACE)
          invArg();
        pt = getPoint3f(++i, false);
        i = eval.iToken;
        if (pt.x <= 0 || pt.y <= 0 || pt.z <= 0)
          break;
        pt.x = (int) pt.x;
        pt.y = (int) pt.y;
        pt.z = (int) pt.z;
        sbCommand.append(" lattice ").append(Escape.eP(pt));
        if (isMapped) {
          propertyName = "mapLattice";
          propertyValue = pt;
        } else {
          lattice = pt;
          if (tokAt(i + 1) == T.fixed) {
            sbCommand.append(" fixed");
            fixLattice = true;
            i++;
          }
        }
        break;
      default:
        if (eval.theTok == T.identifier) {
          propertyName = "thisID";
          propertyValue = str;
        }
        /* I have no idea why this is here....
        if (planeSeen && !surfaceObjectSeen) {
          addShapeProperty(propertyList, "nomap", Float.valueOf(0));
          surfaceObjectSeen = true;
        }
        */
        if (!eval.setMeshDisplayProperty(iShape, 0, eval.theTok)) {
          if (T.tokAttr(eval.theTok, T.identifier) && !idSeen) {
            setShapeId(iShape, i, idSeen);
            i = eval.iToken;
            break;
          }
          invArg();
        }
        if (iptDisplayProperty == 0)
          iptDisplayProperty = i;
        i = slen - 1;
        break;
      }
      idSeen = (eval.theTok != T.delete);
      if (isWild && surfaceObjectSeen)
        invArg();
      if (propertyName != null)
        addShapeProperty(propertyList, propertyName, propertyValue);
    }

    // OK, now send them all

    if (!chk) {
      if ((isCavity || haveRadius) && !surfaceObjectSeen) {
        surfaceObjectSeen = true;
        addShapeProperty(propertyList, "bsSolvent",
            (haveRadius ? new BS() : eval.lookupIdentifierValue("solvent")));
        addShapeProperty(propertyList, "sasurface", Float.valueOf(0));
      }
      if (planeSeen && !surfaceObjectSeen && !isMapped) {
        // !isMapped mp.added 6/14/2012 12.3.30
        // because it was preventing planes from being mapped properly
        addShapeProperty(propertyList, "nomap", Float.valueOf(0));
        surfaceObjectSeen = true;
      }
      if (thisSetNumber >= -1)
        addShapeProperty(propertyList, "getSurfaceSets",
            Integer.valueOf(thisSetNumber - 1));
      if (discreteColixes != null) {
        addShapeProperty(propertyList, "colorDiscrete", discreteColixes);
      } else if ("sets".equals(colorScheme)) {
        addShapeProperty(propertyList, "setColorScheme", null);
      } else if (colorScheme != null) {
        ColorEncoder ce = vwr.cm.getColorEncoder(colorScheme);
        if (ce != null) {
          ce.isTranslucent = isColorSchemeTranslucent;
          ce.hi = Float.MAX_VALUE;
          addShapeProperty(propertyList, "remapColor", ce);
        }
      }
      if (surfaceObjectSeen && !isLcaoCartoon && sbCommand.indexOf(";") != 0) {
        propertyList.add(0, new Object[] { "newObject", null });
        boolean needSelect = (bsSelect == null);
        if (needSelect)
          bsSelect = BSUtil.copy(vwr.bsA());
        if (modelIndex < 0)
          modelIndex = vwr.am.cmi;
        bsSelect.and(vwr.getModelUndeletedAtomsBitSet(modelIndex));
        if (onlyOneModel != null) {
          BS bsModels = vwr.ms.getModelBS(bsSelect, false);
          if (bsModels.cardinality() > 1)
            eval.errorStr(ScriptError.ERROR_multipleModelsDisplayedNotOK,
                "ISOSURFACE " + onlyOneModel);
          if (needSelect) {
            propertyList.add(0, new Object[] { "select", bsSelect });
            if (sbCommand.indexOf("; isosurface map") == 0) {
              sbCommand = new SB().append("; isosurface map select ")
                  .append(Escape.eBS(bsSelect)).append(sbCommand.substring(16));
            }
          }
        }
      }
      if (haveIntersection && !haveSlab) {
        if (!surfaceObjectSeen)
          addShapeProperty(propertyList, "sasurface", Float.valueOf(0));
        if (!isMapped) {
          addShapeProperty(propertyList, "map", Boolean.TRUE);
          addShapeProperty(propertyList, "select", bs);
          addShapeProperty(propertyList, "sasurface", Float.valueOf(0));
        }
        addShapeProperty(propertyList, "slab", getCapSlabObject(-100, false));
      }

      boolean timeMsg = (surfaceObjectSeen && vwr.getBoolean(T.showtiming));
      if (timeMsg)
        Logger.startTimer("isosurface");
      setShapeProperty(iShape, "setProperties", propertyList);
      if (timeMsg)
        showString(Logger.getTimerMsg("isosurface", 0));
      if (defaultMesh) {
        setShapeProperty(iShape, "token", Integer.valueOf(T.mesh));
        setShapeProperty(iShape, "token", Integer.valueOf(T.nofill));
        isFrontOnly = true;
        sbCommand.append(" mesh nofill frontOnly");
      }
    }
    if (lattice != null) { // before MAP, this is a display option
      setShapeProperty(iShape, "lattice", lattice);
      if (fixLattice)
        setShapeProperty(iShape, "fixLattice", Boolean.TRUE);
    }
    if (symops != null) // before MAP, this is a display option
      setShapeProperty(iShape, "symops", symops);
    if (isFrontOnly)
      setShapeProperty(iShape, "token", Integer.valueOf(T.frontonly));
    if (iptDisplayProperty > 0) {
      if (!eval.setMeshDisplayProperty(iShape, iptDisplayProperty, 0))
        invArg();
    }
    if (chk)
      return;
    Object area = null;
    Object volume = null;
    if (doCalcArea) {
      area = getShapeProperty(iShape, "area");
      if (area instanceof Float)
        vwr.setFloatProperty("isosurfaceArea", ((Float) area).floatValue());
      else
        vwr.g.setUserVariable("isosurfaceArea",
            SV.getVariableAD((double[]) area));
    }
    if (doCalcVolume) {
      volume = (doCalcVolume ? getShapeProperty(iShape, "volume") : null);
      if (volume instanceof Float)
        vwr.setFloatProperty("isosurfaceVolume", ((Float) volume).floatValue());
      else
        vwr.g.setUserVariable("isosurfaceVolume",
            SV.getVariableAD((double[]) volume));
    }
    if (!isLcaoCartoon) {
      String s = null;
      if (isMapped && !surfaceObjectSeen) {
        setShapeProperty(iShape, "finalize", sbCommand.toString());
      } else if (surfaceObjectSeen) {
        cmd = sbCommand.toString();
        setShapeProperty(iShape, "finalize",
            (cmd.indexOf("; isosurface map") == 0 ? ""
                : " select " + Escape.eBS(bsSelect) + " ") + cmd);
        s = (String) getShapeProperty(iShape, "ID");
        if (s != null && !eval.tQuiet && !isSilent) {
          cutoff = ((Float) getShapeProperty(iShape, "cutoff")).floatValue();
          if (Float.isNaN(cutoff) && !Float.isNaN(sigma))
            Logger.error("sigma not supported");
          s += " created " + getShapeProperty(iShape, "message");
        }
      }
      String sarea, svol;
      if (doCalcArea || doCalcVolume) {
        sarea = (doCalcArea ? "isosurfaceArea = "
            + (area instanceof Float ? "" + area : Escape.eAD((double[]) area))
            : null);
        svol = (doCalcVolume
            ? "isosurfaceVolume = " + (volume instanceof Float ? "" + volume
                : Escape.eAD((double[]) volume))
            : null);
        if (s == null) {
          if (doCalcArea)
            showString(sarea);
          if (doCalcVolume)
            showString(svol);
        } else {
          if (doCalcArea)
            s += "\n" + sarea;
          if (doCalcVolume)
            s += "\n" + svol;
        }
      }
      if (s != null && !isSilent)
        showString(s);
    }
    if (translucency != null)
      setShapeProperty(iShape, "translucency", translucency);
    setShapeProperty(iShape, "clear", null);
    if (toCache)
      setShapeProperty(iShape, "cache", null);
    if (!isSilent && !isDisplay && !haveSlab && eval.theTok != T.delete)
      listIsosurface(iShape);
  }

  private void lcaoCartoon() throws ScriptException {
    ScriptEval eval = e;
    eval.sm.loadShape(JC.SHAPE_LCAOCARTOON);
    if (eval.tokAt(1) == T.list && listIsosurface(JC.SHAPE_LCAOCARTOON))
      return;
    setShapeProperty(JC.SHAPE_LCAOCARTOON, "init", eval.fullCommand);
    if (slen == 1) {
      setShapeProperty(JC.SHAPE_LCAOCARTOON, "lcaoID", null);
      return;
    }
    boolean idSeen = false;
    String translucency = null;
    for (int i = 1; i < slen; i++) {
      String propertyName = null;
      Object propertyValue = null;
      switch (getToken(i).tok) {
      case T.cap:
      case T.slab:
        propertyName = (String) eval.theToken.value;
        if (tokAt(i + 1) == T.off)
          eval.iToken = i + 1;
        propertyValue = getCapSlabObject(i, true);
        i = eval.iToken;
        break;
      case T.center:
        // serialized lcaoCartoon in isosurface format
        isosurface(JC.SHAPE_LCAOCARTOON);
        return;
      case T.rotate:
        float degx = 0;
        float degy = 0;
        float degz = 0;
        switch (getToken(++i).tok) {
        case T.x:
          degx = floatParameter(++i) * JC.radiansPerDegree;
          break;
        case T.y:
          degy = floatParameter(++i) * JC.radiansPerDegree;
          break;
        case T.z:
          degz = floatParameter(++i) * JC.radiansPerDegree;
          break;
        default:
          invArg();
        }
        propertyName = "rotationAxis";
        propertyValue = V3.new3(degx, degy, degz);
        break;
      case T.on:
      case T.display:
      case T.displayed:
        propertyName = "on";
        break;
      case T.off:
      case T.hide:
      case T.hidden:
        propertyName = "off";
        break;
      case T.delete:
        propertyName = "delete";
        break;
      case T.define:
      case T.bitset:
      case T.expressionBegin:
        propertyName = "select";
        propertyValue = atomExpressionAt(i);
        i = eval.iToken;
        break;
      case T.color:
        translucency = setColorOptions(null, i + 1, JC.SHAPE_LCAOCARTOON, -2);
        if (translucency != null)
          setShapeProperty(JC.SHAPE_LCAOCARTOON, "settranslucency",
              translucency);
        i = eval.iToken;
        idSeen = true;
        continue;
      case T.translucent:
      case T.opaque:
        eval.setMeshDisplayProperty(JC.SHAPE_LCAOCARTOON, i, eval.theTok);
        i = eval.iToken;
        idSeen = true;
        continue;
      case T.spacefill:
      case T.string:
        propertyValue = paramAsStr(i).toLowerCase();
        if (propertyValue.equals("spacefill"))
          propertyValue = "cpk";
        propertyName = "create";
        if (eval.optParameterAsString(i + 1).equalsIgnoreCase("molecular")) {
          i++;
          propertyName = "molecular";
        }
        break;
      case T.select:
        if (eval.isAtomExpression(i + 1)) {
          propertyName = "select";
          propertyValue = atomExpressionAt(i + 1);
          i = eval.iToken;
        } else {
          propertyName = "selectType";
          propertyValue = paramAsStr(++i);
          if (propertyValue.equals("spacefill"))
            propertyValue = "cpk";
        }
        break;
      case T.scale:
        propertyName = "scale";
        propertyValue = Float.valueOf(floatParameter(++i));
        break;
      case T.lonepair:
      case T.lp:
        propertyName = "lonePair";
        break;
      case T.radical:
      case T.rad:
        propertyName = "radical";
        break;
      case T.molecular:
        propertyName = "molecular";
        break;
      case T.create:
        propertyValue = paramAsStr(++i);
        propertyName = "create";
        if (eval.optParameterAsString(i + 1).equalsIgnoreCase("molecular")) {
          i++;
          propertyName = "molecular";
        }
        break;
      case T.id:
        propertyValue = eval.setShapeNameParameter(++i);
        i = eval.iToken;
        if (idSeen)
          invArg();
        propertyName = "lcaoID";
        break;
      default:
        if (eval.theTok == T.times || T.tokAttr(eval.theTok, T.identifier)) {
          if (eval.theTok != T.times)
            propertyValue = paramAsStr(i);
          if (idSeen)
            invArg();
          propertyName = "lcaoID";
          break;
        }
        break;
      }
      if (eval.theTok != T.delete)
        idSeen = true;
      if (propertyName == null)
        invArg();
      setShapeProperty(JC.SHAPE_LCAOCARTOON, propertyName, propertyValue);
    }
    setShapeProperty(JC.SHAPE_LCAOCARTOON, "clear", null);
  }

  private boolean contact() throws ScriptException {
    ScriptEval eval = e;
    eval.sm.loadShape(JC.SHAPE_CONTACT);
    if (tokAt(1) == T.list && listIsosurface(JC.SHAPE_CONTACT))
      return false;
    int iptDisplayProperty = 0;
    eval.iToken = 1;
    String thisId = initIsosurface(JC.SHAPE_CONTACT);
    boolean idSeen = (thisId != null);
    boolean isWild = (idSeen && getShapeProperty(JC.SHAPE_CONTACT, "ID") == null);
    BS bsA = null;
    BS bsB = null;
    BS bs = null;
    RadiusData rd = null;
    float[] params = null;
    boolean colorDensity = false;
    SB sbCommand = new SB();
    int minSet = Integer.MAX_VALUE;
    int displayType = T.plane;
    int contactType = T.nada;
    float distance = Float.NaN;
    float saProbeRadius = Float.NaN;
    boolean localOnly = true;
    Boolean intramolecular = null;
    Object userSlabObject = null;
    int colorpt = 0;
    boolean colorByType = false;
    int tok;
    int modelIndex = Integer.MIN_VALUE;
    boolean okNoAtoms = (eval.iToken > 1);
    for (int i = eval.iToken; i < slen; ++i) {
      switch (tok = getToken(i).tok) {
      // these first do not need atoms defined
      default:
        okNoAtoms = true;
        if (!eval.setMeshDisplayProperty(JC.SHAPE_CONTACT, 0, eval.theTok)) {
          if (eval.theTok != T.times && !T.tokAttr(eval.theTok, T.identifier))
            invArg();
          thisId = setShapeId(JC.SHAPE_CONTACT, i, idSeen);
          i = eval.iToken;
          break;
        }
        if (iptDisplayProperty == 0)
          iptDisplayProperty = i;
        i = eval.iToken;
        continue;
      case T.id:
        okNoAtoms = true;
        setShapeId(JC.SHAPE_CONTACT, ++i, idSeen);
        isWild = (getShapeProperty(JC.SHAPE_CONTACT, "ID") == null);
        i = eval.iToken;
        break;
      case T.color:
        switch (tokAt(i + 1)) {
        case T.density:
          tok = T.nada;
          colorDensity = true;
          sbCommand.append(" color density");
          i++;
          break;
        case T.type:
          tok = T.nada;
          colorByType = true;
          sbCommand.append(" color type");
          i++;
          break;
        }
        if (tok == T.nada)
          break;
        //$FALL-THROUGH$ to translucent
      case T.translucent:
      case T.opaque:
        okNoAtoms = true;
        if (colorpt == 0)
          colorpt = i;
        eval.setMeshDisplayProperty(JC.SHAPE_CONTACT, i, eval.theTok);
        i = eval.iToken;
        break;
      case T.slab:
        okNoAtoms = true;
        userSlabObject = getCapSlabObject(i, false);
        setShapeProperty(JC.SHAPE_CONTACT, "slab", userSlabObject);
        i = eval.iToken;
        break;

      // now after this you need atoms

      case T.density:
        colorDensity = true;
        sbCommand.append(" density");
        if (isFloatParameter(i + 1)) {
          if (params == null)
            params = new float[1];
          params[0] = -Math.abs(floatParameter(++i));
          sbCommand.append(" " + -params[0]);
        }
        break;
      case T.resolution:
        float resolution = floatParameter(++i);
        if (resolution > 0) {
          sbCommand.append(" resolution ").appendF(resolution);
          setShapeProperty(JC.SHAPE_CONTACT, "resolution",
              Float.valueOf(resolution));
        }
        break;
      case T.model:
      case T.modelindex:
        modelIndex = (eval.theTok == T.modelindex ? intParameter(++i) : eval
            .modelNumberParameter(++i));
        sbCommand.append(" modelIndex " + modelIndex);
        break;
      case T.within:
      case T.distance:
        distance = floatParameter(++i);
        sbCommand.append(" within ").appendF(distance);
        break;
      case T.plus:
      case T.integer:
      case T.decimal:
        rd = eval.encodeRadiusParameter(i, false, false);
        if (rd == null)
          return false;
        sbCommand.append(" ").appendO(rd);
        i = eval.iToken;
        break;
      case T.intermolecular:
      case T.intramolecular:
        intramolecular = (tok == T.intramolecular ? Boolean.TRUE
            : Boolean.FALSE);
        sbCommand.append(" ").appendO(eval.theToken.value);
        break;
      case T.minset:
        minSet = intParameter(++i);
        break;
      case T.hbond:
      case T.clash:
      case T.vanderwaals:
        contactType = tok;
        sbCommand.append(" ").appendO(eval.theToken.value);
        break;
      case T.sasurface:
        if (isFloatParameter(i + 1))
          saProbeRadius = floatParameter(++i);
        //$FALL-THROUGH$
      case T.cap:
      case T.nci:
      case T.surface:
        localOnly = false;
        //$FALL-THROUGH$
      case T.trim:
      case T.full:
      case T.plane:
      case T.connect:
        displayType = tok;
        sbCommand.append(" ").appendO(eval.theToken.value);
        if (tok == T.sasurface)
          sbCommand.append(" ").appendF(saProbeRadius);
        break;
      case T.parameters:
        params = eval.floatParameterSet(++i, 1, 10);
        i = eval.iToken;
        break;
      case T.define:
      case T.bitset:
      case T.expressionBegin:
        if (isWild || bsB != null)
          invArg();
        bs = BSUtil.copy(atomExpressionAt(i));
        i = eval.iToken;
        if (bsA == null)
          bsA = bs;
        else
          bsB = bs;
        sbCommand.append(" ").append(Escape.eBS(bs));
        break;
      }
      idSeen = (eval.theTok != T.delete);
    }
    if (!okNoAtoms && bsA == null)
      error(ScriptError.ERROR_endOfStatementUnexpected);
    if (chk)
      return false;

    if (bsA != null) {
      // bond mode, intramolec set here
      if (contactType == T.vanderwaals && rd == null)
        rd = new RadiusData(null, 0, EnumType.OFFSET, VDW.AUTO);
      RadiusData rd1 = (rd == null ? new RadiusData(null, 0.26f,
          EnumType.OFFSET, VDW.AUTO) : rd);
      if (displayType == T.nci && bsB == null && intramolecular != null
          && intramolecular.booleanValue())
        bsB = bsA;
      else
        bsB = eval.getMathExt().setContactBitSets(bsA, bsB, localOnly, distance, rd1, true);
      switch (displayType) {
      case T.cap:
      case T.sasurface:
        BS bsSolvent = eval.lookupIdentifierValue("solvent");
        bsA.andNot(bsSolvent);
        bsB.andNot(bsSolvent);
        bsB.andNot(bsA);
        break;
      case T.surface:
        bsB.andNot(bsA);
        break;
      case T.nci:
        if (minSet == Integer.MAX_VALUE)
          minSet = 100;
        setShapeProperty(JC.SHAPE_CONTACT, "minset", Integer.valueOf(minSet));
        sbCommand.append(" minSet ").appendI(minSet);
        if (params == null)
          params = new float[] { 0.5f, 2 };
      }

      if (intramolecular != null) {
        params = (params == null ? new float[2] : AU.ensureLengthA(params, 2));
        params[1] = (intramolecular.booleanValue() ? 1 : 2);
      }

      if (params != null)
        sbCommand.append(" parameters ").append(Escape.eAF(params));

      // now adjust for type -- HBOND or HYDROPHOBIC or MISC
      // these are just "standard shortcuts" they are not necessary at all
      setShapeProperty(
          JC.SHAPE_CONTACT,
          "set",
          new Object[] { Integer.valueOf(contactType),
              Integer.valueOf(displayType), Boolean.valueOf(colorDensity),
              Boolean.valueOf(colorByType), bsA, bsB, rd,
              Float.valueOf(saProbeRadius), params, Integer.valueOf(modelIndex), sbCommand.toString() });
      if (colorpt > 0)
        eval.setMeshDisplayProperty(JC.SHAPE_CONTACT, colorpt, 0);
    }
    if (iptDisplayProperty > 0) {
      if (!eval.setMeshDisplayProperty(JC.SHAPE_CONTACT, iptDisplayProperty, 0))
        invArg();
    }
    if (userSlabObject != null && bsA != null)
      setShapeProperty(JC.SHAPE_CONTACT, "slab", userSlabObject);
    if (bsA != null && (displayType == T.nci || localOnly)) {
      Object volume = getShapeProperty(JC.SHAPE_CONTACT, "volume");
      double v;
      boolean isFull = (displayType == T.full);
      if (AU.isAD(volume)) {
        double[] vs = (double[]) volume;
        v = 0;
        for (int i = 0; i < vs.length; i++)
          v += (isFull ? vs[i] : Math.abs(vs[i])); // no abs value for full -- some are negative
      } else {
        v = ((Float) volume).floatValue();
      }
      v = (Math.round(v * 1000) / 1000.);
      if (colorDensity || displayType != T.trim) {
        int nsets = ((Integer) getShapeProperty(JC.SHAPE_CONTACT, "nSets"))
            .intValue(); // will be < 0 if FULL option
        String s = "Contacts: " + (nsets < 0 ? -nsets/2 : nsets);
        if (v != 0)
          s += ", with " + (isFull ? "approx " : "net ") + "volume " + v + " A^3";
        showString(s);
      }
    }
    return true;
  }

  private boolean cgo() throws ScriptException {
    ScriptEval eval = e;
    eval.sm.loadShape(JC.SHAPE_CGO);
    if (tokAt(1) == T.list && listIsosurface(JC.SHAPE_CGO))
      return false;
    int iptDisplayProperty = 0;
    String thisId = initIsosurface(JC.SHAPE_CGO);
    boolean idSeen = (thisId != null);
    boolean isWild = (idSeen && getShapeProperty(JC.SHAPE_CGO, "ID") == null);
    boolean isInitialized = false;
    int modelIndex = -1;
    Lst<Object> data = null;
    float translucentLevel = Float.MAX_VALUE;
    int[] colorArgb = new int[] { Integer.MIN_VALUE };
    int intScale = 0;
    for (int i = eval.iToken; i < slen; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      int tok = getToken(i).tok;
      switch (tok) {
      case T.leftsquare:
      case T.spacebeforesquare:
      case T.varray:
        if (data != null || isWild)
          invArg();
        data = new Lst<Object>();
        int[] ai = new int[] {i, slen};
        if (!eval.getShapePropertyData(JC.SHAPE_CGO, "data", new Object[] { st, ai, data, vwr }))
          invArg();
        i = ai[0];
        continue;
      case T.scale:
        if (++i >= slen)
          error(ScriptError.ERROR_numberExpected);
        switch (getToken(i).tok) {
        case T.integer:
          intScale = intParameter(i);
          continue;
        case T.decimal:
          intScale = Math.round(floatParameter(i) * 100);
          continue;
        }
        error(ScriptError.ERROR_numberExpected);
        break;
      case T.fixed:
        propertyName = "modelIndex";
        propertyValue = Integer.valueOf(-1);
        break;
      case T.modelindex:
      case T.model:
        modelIndex = (eval.theTok == T.modelindex ? intParameter(++i) : eval
            .modelNumberParameter(++i));
        propertyName = "modelIndex";
        propertyValue = Integer.valueOf(modelIndex);
        break;
      case T.color:
      case T.translucent:
      case T.opaque:
        translucentLevel = getColorTrans(eval, i, false, colorArgb);
        i = eval.iToken;
        idSeen = true;
        continue;
      case T.id:
        thisId = setShapeId(JC.SHAPE_CGO, ++i, idSeen);
        isWild = (getShapeProperty(JC.SHAPE_CGO, "ID") == null);
        i = eval.iToken;
        break;
      default:
        if (!eval.setMeshDisplayProperty(JC.SHAPE_CGO, 0, eval.theTok)) {
          if (eval.theTok == T.times || T.tokAttr(eval.theTok, T.identifier)) {
            thisId = setShapeId(JC.SHAPE_CGO, i, idSeen);
            i = eval.iToken;
            break;
          }
          invArg();
        }
        if (iptDisplayProperty == 0)
          iptDisplayProperty = i;
        i = eval.iToken;
        continue;
      }
      idSeen = (eval.theTok != T.delete);
      if (data != null && !isInitialized) {
        propertyName = "points";
        propertyValue = Integer.valueOf(intScale);
        isInitialized = true;
        intScale = 0;
      }
      if (propertyName != null)
        setShapeProperty(JC.SHAPE_CGO, propertyName, propertyValue);
    }
    finalizeObject(JC.SHAPE_CGO, colorArgb[0], translucentLevel,
        intScale, data != null, data, iptDisplayProperty, null);
    return true;
  }

  /**
   * 
   * @param bsSelected
   * @param bsIgnore
   * @param fileName
   * @return calculated atom potentials
   */
  private float[] getAtomicPotentials(BS bsSelected, BS bsIgnore,
                                      String fileName) {
    float[] potentials = new float[vwr.ms.ac];
    MepCalculation m = (MepCalculation) Interface
        .getOption("quantum.MlpCalculation", vwr, "script");
    m.set(vwr);
    String data = (fileName == null ? null : vwr.getFileAsString3(fileName,
        false, null));
    try {
      m.assignPotentials(vwr.ms.at, potentials, vwr
          .getSmartsMatch("a", bsSelected), vwr.getSmartsMatch(
          "/noAromatic/[$(C=O),$(O=C),$(NC=O)]", bsSelected), bsIgnore, data);
    } catch (Exception e) {
    }
    return potentials;
  }

  private Object getCapSlabObject(int i, boolean isLcaoCartoon)
      throws ScriptException {
    if (i < 0) {
      // standard range -100 to 0
      return TempArray.getSlabWithinRange(i, 0);
    }
    ScriptEval eval = e;
    Object data = null;
    int tok0 = tokAt(i);
    boolean isSlab = (tok0 == T.slab);
    int tok = tokAt(i + 1);
    P4 plane = null;
    P3[] pts = null;
    float d, d2;
    BS bs = null;
    Short slabColix = null;
    Integer slabMeshType = null;
    if (tok == T.translucent) {
      float slabTranslucency = (isFloatParameter(++i + 1) ? floatParameter(++i)
          : 0.5f);
      if (eval.isColorParam(i + 1)) {
        slabColix = Short.valueOf(C.getColixTranslucent3(
            C.getColix(eval.getArgbParam(i + 1)), slabTranslucency != 0,
            slabTranslucency));
        i = eval.iToken;
      } else {
        slabColix = Short.valueOf(C.getColixTranslucent3(C.INHERIT_COLOR,
            slabTranslucency != 0, slabTranslucency));
      }
      switch (tok = tokAt(i + 1)) {
      case T.mesh:
      case T.fill:
        slabMeshType = Integer.valueOf(tok);
        tok = tokAt(++i + 1);
        break;
      default:
        slabMeshType = Integer.valueOf(T.fill);
        break;
      }
    }
    //TODO: check for compatibility with LCAOCARTOONS
    switch (tok) {
    case T.off:
      eval.iToken = i + 1;
      return Integer.valueOf(Integer.MIN_VALUE);
    case T.none:
      eval.iToken = i + 1;
      break;
    case T.dollarsign:
      // do we need distance here? "-" here?
      i++;
      data = new Object[] { Float.valueOf(1), paramAsStr(++i) };
      tok = T.mesh;
      break;
    case T.within:
      // isosurface SLAB WITHIN RANGE f1 f2
      i++;
      if (tokAt(++i) == T.range) {
        d = floatParameter(++i);
        d2 = floatParameter(++i);
        data = new Object[] { Float.valueOf(d), Float.valueOf(d2) };
        tok = T.range;
      } else if (isFloatParameter(i)) {
        // isosurface SLAB WITHIN distance {atomExpression}|[point array]
        d = floatParameter(i);
        if (eval.isCenterParameter(++i)) {
          Object[] ret = new Object[1];
          P3 pt = eval.centerParameter(i, ret);
          if (chk || !(ret[0] instanceof BS)) {
            pts = new P3[] { pt };
          } else {
            Atom[] atoms = vwr.ms.at;
            bs = (BS) ret[0];
            pts = new P3[bs.cardinality()];
            for (int k = 0, j = bs.nextSetBit(0); j >= 0; j = bs
                .nextSetBit(j + 1), k++)
              pts[k] = atoms[j];
          }
        } else {
          pts = eval.getPointArray(i, -1, false);
        }
        if (pts.length == 0) {
          eval.iToken = i;
          invArg();
        }
        data = new Object[] { Float.valueOf(d), pts, bs };
      } else {
        data = eval.getPointArray(i, 4, false);
        tok = T.boundbox;
      }
      break;
    case T.boundbox:
      eval.iToken = i + 1;
      data = BoxInfo.toOABC(vwr.ms.getBBoxVertices(), null);
      break;
    //case Token.slicebox:
    // data = BoxInfo.getCriticalPoints(((JmolViewer)(vwr)).slicer.getSliceVert(), null);
    //eval.iToken = i + 1;
    //break;  
    case T.brillouin:
    case T.unitcell:
      eval.iToken = i + 1;
      SymmetryInterface unitCell = vwr.getCurrentUnitCell();
      if (unitCell == null) {
        if (tok == T.unitcell)
          invArg();
      } else {
        pts = BoxInfo.toOABC(unitCell.getUnitCellVerticesNoOffset(),
            unitCell.getCartesianOffset());
        int iType = (int) unitCell
            .getUnitCellInfoType(SimpleUnitCell.INFO_DIMENSIONS);
        V3 v1 = null;
        V3 v2 = null;
        switch (iType) {
        case 3:
          break;
        case 1: // polymer
          v2 = V3.newVsub(pts[2], pts[0]);
          v2.scale(1000f);
          //$FALL-THROUGH$
        case 2: // slab
          // "a b c" is really "z y x"
          v1 = V3.newVsub(pts[1], pts[0]);
          v1.scale(1000f);
          pts[0].sub(v1);
          pts[1].scale(2000f);
          if (iType == 1) {
            pts[0].sub(v2);
            pts[2].scale(2000f);
          }
          break;
        }
        data = pts;
      }
      break;
    case T.define:
    case T.bitset:
    case T.expressionBegin:
      data = atomExpressionAt(i + 1);
      tok = T.decimal;
      if (!eval.isCenterParameter(++eval.iToken)) {
        isSlab = true;
        break;
      }
      data = null;
      //$FALL-THROUGH$
    default:
      // isosurface SLAB n
      // isosurface SLAB -100. 0.  as "within range" 
      if (!isLcaoCartoon && isSlab && isFloatParameter(i + 1)) {
        d = floatParameter(++i);
        if (!isFloatParameter(i + 1))
          return Integer.valueOf((int) d);
        d2 = floatParameter(++i);
        data = new Object[] { Float.valueOf(d), Float.valueOf(d2) };
        tok = T.range;
        break;
      }
      // isosurface SLAB [plane]
      plane = eval.planeParameter(++i);
      float off = (isFloatParameter(eval.iToken + 1) ? floatParameter(++eval.iToken)
          : Float.NaN);
      if (!Float.isNaN(off))
        plane.w -= off;
      data = plane;
      tok = T.plane;
    }
    Object colorData = (slabMeshType == null ? null : new Object[] {
        slabMeshType, slabColix });
    return TempArray.getSlabObjectType(tok, data, !isSlab, colorData);
  }

  private String setColorOptions(SB sb, int index, int iShape, int nAllowed)
      throws ScriptException {
    ScriptEval eval = e;
    getToken(index);
    String translucency = "opaque";
    if (eval.theTok == T.translucent) {
      translucency = "translucent";
      if (nAllowed < 0) {
        float value = (isFloatParameter(index + 1) ? floatParameter(++index)
            : Float.MAX_VALUE);
        eval.setShapeTranslucency(iShape, null, "translucent", value, null);
        if (sb != null) {
          sb.append(" translucent");
          if (value != Float.MAX_VALUE)
            sb.append(" ").appendF(value);
        }
      } else {
        eval.setMeshDisplayProperty(iShape, index, eval.theTok);
      }
    } else if (eval.theTok == T.opaque) {
      if (nAllowed >= 0)
        eval.setMeshDisplayProperty(iShape, index, eval.theTok);
    } else {
      eval.iToken--;
    }
    nAllowed = Math.abs(nAllowed);
    for (int i = 0; i < nAllowed; i++) {
      if (eval.isColorParam(eval.iToken + 1)) {
        int color = eval.getArgbParam(++eval.iToken);
        setShapeProperty(iShape, "colorRGB", Integer.valueOf(color));
        if (sb != null)
          sb.append(" ").append(Escape.escapeColor(color));
      } else if (eval.iToken < index) {
        invArg();
      } else {
        break;
      }
    }
    return translucency;
  }

  /**
   * for the ISOSURFACE command
   * 
   * @param fname
   * @param xyz
   * @param ret
   * @return [ ScriptFunction, Params ]
   */
  private Object[] createFunction(String fname, String xyz, String ret) {
    ScriptEval e = (new ScriptEval()).setViewer(vwr);
    try {
      e.compileScript(null, "function " + fname + "(" + xyz + ") { return "
          + ret + "}", false);
      Lst<SV> params = new Lst<SV>();
      for (int i = 0; i < xyz.length(); i += 2)
        params.addLast(SV.newF(0).setName(
            xyz.substring(i, i + 1)));
      return new Object[] { e.aatoken[0][1].value, params };
    } catch (Exception ex) {
      return null;
    }
  }

  private P3[] getWithinDistanceVector(Lst<Object[]> propertyList,
                                       float distance, P3 ptc, BS bs,
                                       boolean isShow) {
    Lst<P3> v = new Lst<P3>();
    P3[] pts = new P3[2];
    if (bs == null) {
      P3 pt1 = P3.new3(distance, distance, distance);
      P3 pt0 = P3.newP(ptc);
      pt0.sub(pt1);
      pt1.add(ptc);
      pts[0] = pt0;
      pts[1] = pt1;
      v.addLast(ptc);
    } else {
      BoxInfo bbox = vwr.ms.getBoxInfo(bs, -Math.abs(distance * 2));
      pts[0] = bbox.getBoundBoxVertices()[0];
      pts[1] = bbox.getBoundBoxVertices()[BoxInfo.XYZ];
      if (bs.cardinality() == 1)
        v.addLast(vwr.ms.at[bs.nextSetBit(0)]);
    }
    if (v.size() == 1 && !isShow) {
      addShapeProperty(propertyList, "withinDistance", Float.valueOf(distance));
      addShapeProperty(propertyList, "withinPoint", v.get(0));
    }
    addShapeProperty(propertyList, (isShow ? "displayWithin" : "withinPoints"),
        new Object[] { Float.valueOf(distance), pts, bs, v });
    return pts;
  }

  private void addShapeProperty(Lst<Object[]> propertyList, String key,
                                Object value) {
    if (chk)
      return;
    propertyList.addLast(new Object[] { key, value });
  }

  private float[][][] floatArraySetXYZ(int i, int nX, int nY, int nZ)
      throws ScriptException {
    ScriptEval eval = e;
    int tok = tokAt(i++);
    if (tok == T.spacebeforesquare)
      tok = tokAt(i++);
    if (tok != T.leftsquare || nX <= 0)
      invArg();
    float[][][] fparams = AU.newFloat3(nX, -1);
    int n = 0;
    while (tok != T.rightsquare) {
      tok = getToken(i).tok;
      switch (tok) {
      case T.spacebeforesquare:
      case T.rightsquare:
        continue;
      case T.comma:
        i++;
        break;
      case T.leftsquare:
        fparams[n++] = floatArraySet(i, nY, nZ);
        i = ++eval.iToken;
        tok = T.nada;
        if (n == nX && tokAt(i) != T.rightsquare)
          invArg();
        break;
      default:
        invArg();
      }
    }
    return fparams;
  }

  private float[][] floatArraySet(int i, int nX, int nY) throws ScriptException {
    int tok = tokAt(i++);
    if (tok == T.spacebeforesquare)
      tok = tokAt(i++);
    if (tok != T.leftsquare)
      invArg();
    float[][] fparams = AU.newFloat2(nX);
    int n = 0;
    while (tok != T.rightsquare) {
      tok = getToken(i).tok;
      switch (tok) {
      case T.spacebeforesquare:
      case T.rightsquare:
        continue;
      case T.comma:
        i++;
        break;
      case T.leftsquare:
        i++;
        float[] f = new float[nY];
        fparams[n++] = f;
        for (int j = 0; j < nY; j++) {
          f[j] = floatParameter(i++);
          if (tokAt(i) == T.comma)
            i++;
        }
        if (tokAt(i++) != T.rightsquare)
          invArg();
        tok = T.nada;
        if (n == nX && tokAt(i) != T.rightsquare)
          invArg();
        break;
      default:
        invArg();
      }
    }
    return fparams;
  }

  private String initIsosurface(int iShape) throws ScriptException {

    // handle isosurface/mo/pmesh delete and id delete here

    ScriptEval eval = e;
    setShapeProperty(iShape, "init", eval.fullCommand);
    eval.iToken = 0;
    int tok1 = tokAt(1);
    int tok2 = tokAt(2);
    if (tok1 == T.delete || tok2 == T.delete && tokAt(++eval.iToken) == T.all) {
      setShapeProperty(iShape, "delete", null);
      eval.iToken += 2;
      if (slen > eval.iToken) {
        setShapeProperty(iShape, "init", eval.fullCommand);
        setShapeProperty(iShape, "thisID", MeshCollection.PREVIOUS_MESH_ID);
      }
      return null;
    }
    eval.iToken = 1;
    if (!eval.setMeshDisplayProperty(iShape, 0, tok1)) {
      setShapeProperty(iShape, "thisID", MeshCollection.PREVIOUS_MESH_ID);
      if (iShape != JC.SHAPE_DRAW)
        setShapeProperty(iShape, "title", new String[] { eval.thisCommand });
      if (tok1 != T.id
          && (tok2 == T.times || tok1 == T.times
              && eval.setMeshDisplayProperty(iShape, 0, tok2))) {
        String id = setShapeId(iShape, 1, false);
        eval.iToken++;
        return id;
      }
    }
    return null;
  }

  private boolean listIsosurface(int iShape) throws ScriptException {
    String s = (slen > 3 ? "0" : tokAt(2) == T.nada ? "" : " "
        + getToken(2).value);
    if (!chk)
      showString((String) getShapeProperty(iShape, "list" + s));
    return true;
  }

  /**
   * 
   * @param type  unitcell or boundbox
   * @param plane
   *        plane to intersect, or null for just the full box
   * @param scale
   * @param uc
   * @param flags
   *        1 -- edges only 2 -- triangles only 3 -- both
   * @return Vector
   */
  private Lst<Object> getPlaneIntersection(int type, P4 plane,
                                           SymmetryInterface uc, float scale,
                                           int flags) {
    T3[] pts = null;
    switch (type) {
    case T.unitcell:
      if (uc == null)
        return null;
      pts = uc.getCanonicalCopy(scale, true);
      break;
    case T.boundbox:
      pts = BoxInfo.getCanonicalCopy(vwr.ms.getBoxInfo().getBoundBoxVertices(), scale);
      break;
    }
    Triangulator t = vwr.getTriangulator(); // this instantiation forces reflection to get Triangulator class
    if (plane != null)
      return t.intersectPlane(plane, pts, flags);
    Lst<Object> v = new Lst<Object>();
    v.addLast(pts);
    v.addLast(Triangulator.fullCubePolygon);
    return v;
  }


}
