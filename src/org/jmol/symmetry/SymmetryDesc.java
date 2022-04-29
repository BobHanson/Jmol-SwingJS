/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-05-18 08:19:45 -0500 (Fri, 18 May 2007) $
 * $Revision: 7742 $

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

package org.jmol.symmetry;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.SymmetryInterface;
import org.jmol.modelset.Atom;
import org.jmol.modelset.ModelSet;
import org.jmol.script.T;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.viewer.JC;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.M4d;
import javajs.util.MeasureD;
import javajs.util.P3d;
import javajs.util.P3d;
import javajs.util.P4d;
import javajs.util.PT;
import javajs.util.Qd;
import javajs.util.Qd;
import javajs.util.SB;
import javajs.util.T3d;
import javajs.util.T3d;
import javajs.util.V3d;

/**
 * A class to handle requests for information about space groups and symmetry
 * operations.
 * 
 * Two entry points, both from Symmetry:
 * 
 * getSymopInfo 
 * 
 * getSpaceGroupInfo
 * 
 * 
 * 
 */
public class SymmetryDesc {

  private ModelSet modelSet;

  public SymmetryDesc() {
    // for reflection
  }

  public SymmetryDesc set(ModelSet modelSet) {
    this.modelSet = modelSet;
    return this;
  }

  private final static int RET_XYZ = 0;
  private final static int RET_XYZORIGINAL = 1;
  private final static int RET_LABEL = 2;
  private final static int RET_DRAW = 3;
  private final static int RET_FTRANS = 4;
  private final static int RET_CTRANS = 5;
  private final static int RET_INVCTR = 6;
  private final static int RET_POINT = 7;
  private final static int RET_AXISVECTOR = 8;
  private final static int RET_ROTANGLE = 9;
  private final static int RET_MATRIX = 10;
  private final static int RET_UNITTRANS = 11;
  private final static int RET_CTRVECTOR = 12;
  private final static int RET_TIMEREV = 13;
  private final static int RET_PLANE = 14;
  private final static int RET_TYPE = 15;
  private final static int RET_ID = 16;
  private final static int RET_CIF2 = 17;
  private final static int RET_XYZCANON = 18;
  private final static int RET_COUNT = 19;

  // additional flags
  final static int RET_LIST = 21;
  final static int RET_INVARIANT = 22;

  private final static String[] keys = { 
      "xyz", 
      "xyzOriginal", 
      "label",
      null /*draw*/, 
      "fractionalTranslation", 
      "cartesianTranslation",
      "inversionCenter", 
      null /*point*/, 
      "axisVector", 
      "rotationAngle",
      "matrix", 
      "unitTranslation", 
      "centeringVector", 
      "timeReversal", 
      "plane",
      "_type", 
      "id", 
      "cif2", 
      "xyzCanonical" };

  //////////// private methods ///////////

  /** Determine the type of this request. 
   * Note that label and xyz will be returned as T.xys and T.label 
   * 
   * @param id
   * @return a code that identifies this request.
   */
  private static int getType(String id) {
    int type;
    if (id == null)
      return T.list;
    if (id.equalsIgnoreCase("matrix"))
      return T.matrix4f;
    if (id.equalsIgnoreCase("description"))
      return T.label;
    if (id.equalsIgnoreCase("axispoint"))
      return T.point;
    if (id.equalsIgnoreCase("time"))
      return T.times;
    if (id.equalsIgnoreCase("info"))
      return T.array;
    if (id.equalsIgnoreCase("element"))
      return T.element;
    if (id.equalsIgnoreCase("invariant"))
      return T.var;
    // center, draw, plane, axis, atom, translation, angle, array, list
    type = T.getTokFromName(id);
    if (type != 0)
      return type;
    type = getKeyType(id);
    return (type < 0 ? type : T.all);
  }

  private static int getKeyType(String id) {
    if ("type".equals(id))
      id = "_type";
    for (int type = 0; type < keys.length; type++)
      if (id.equalsIgnoreCase(keys[type]))
        return -1 - type;
    return 0;
  }

  private static Object nullReturn(int type) {
    switch(type) {
    case T.draw:
      return "draw ID sym_* delete";
    case T.full:
    case T.label:
    case T.id:
    case T.xyz:
    case T.matrix3f:
    case T.origin:
      return "";
    case T.atoms:
      return new BS();
    default:
      return null;
    }
  }

  /**
   * Return information about a symmetry operator by type:
   * 
   * array, angle, axis, center, draw, full, info, label, matrix4f, point, time,
   * plane, translation, unitcell, xyz, all,
   * 
   * or a negative number (-length, -1]:
   * 
   * { "xyz", etc. }
   * 
   * where "all" is the info array itself,
   * 
   * @param io
   * @param type
   * @return object specified
   * 
   */
  
  private static Object getInfo(Object[] io, int type) {
    if (io.length == 0)
      return "";
    if (type < 0 && -type <= keys.length && -type <= io.length)
      return io[-1 - type];
    switch (type) {
    case T.all:
    case T.info:
      return io; // 'info' is internal only; if user selects "info", it is turned into "array"
    case T.array:
      Map<String, Object> lst = new Hashtable<String, Object>();
      for (int j = 0, n = io.length; j < n; j++) {
        String key = (j == RET_DRAW ? "draw" : j == RET_POINT ? "axispoint" : keys[j]); 
        if (io[j] != null)
          lst.put(key, io[j]);
      }
      return lst;
    case T.full:
      return io[RET_XYZ] + "  \t" + io[RET_LABEL];
    case T.xyz:
      return io[RET_XYZ];
    case T.origin:
      return io[RET_XYZORIGINAL];
    default:
    case T.label:      
      return io[RET_LABEL];
    case T.draw:
      return io[RET_DRAW] + "\nprint " + PT.esc(io[RET_XYZ] + " " + io[RET_LABEL]);
    case T.fracxyz:
      return io[RET_FTRANS]; // fractional translation "fxyz"
    case T.translation:
      return io[RET_CTRANS]; // cartesian translation
    case T.center:
      return io[RET_INVCTR];
    case T.point:
      return io[RET_POINT];
    case T.axis:
      return io[RET_AXISVECTOR];
    case T.angle:
      return io[RET_ROTANGLE];
    case T.matrix4f:
      return io[RET_MATRIX];
    case T.unitcell:
      return io[RET_UNITTRANS];
    case T.translate:
      // centering
      return io[RET_CTRVECTOR];
    case T.times:
      return io[RET_TIMEREV];
    case T.plane:
      return io[RET_PLANE];
    case T.type:
      return io[RET_TYPE];
    case T.id:
      return io[RET_ID];
    case T.element:
      return new Object[] {io[RET_INVCTR], io[RET_POINT], io[RET_AXISVECTOR], io[RET_PLANE], io[RET_CTRANS]};
    case T.var:
      return (io[RET_CTRANS] != null ? "none" // translation 
      : io[RET_INVCTR] != null ? io[RET_INVCTR] // inversion center 
      : io[RET_AXISVECTOR] != null ? new Object[] { io[RET_POINT], io[RET_AXISVECTOR] } // axis
      : io[RET_PLANE] != null ?  io[RET_PLANE] // plane
      : "identity"); // identity
    }
  }

  private static BS getInfoBS(int type) {
    BS bsInfo = new BS();
     if (type < 0 && -type <= keys.length) {
      bsInfo.set(-1 - type);
      return bsInfo;
    }
    switch (type) {
    case 0:
    case T.atoms:
    case T.list:      
    case T.all:
    case T.info:
    case T.array:
      bsInfo.setBits(0, keys.length);
      break;
    case T.full:
      bsInfo.set(RET_XYZ);
      bsInfo.set(RET_LABEL);
      break;
    case T.xyz:
      bsInfo.set(RET_XYZ);
      break;
    case T.origin:
      bsInfo.set(RET_XYZORIGINAL);
      break;
    default:
    case T.label:      
      bsInfo.set(RET_LABEL);
      break;
    case T.draw:
      bsInfo.set(RET_XYZ);
      bsInfo.set(RET_LABEL);
      bsInfo.set(RET_DRAW);
      break;
    case T.fracxyz:
      bsInfo.set(RET_FTRANS);
      break;
    case T.translation:
      bsInfo.set(RET_CTRANS);
      break;
    case T.center:
      bsInfo.set(RET_INVCTR);
      break;
    case T.point:
      bsInfo.set(RET_POINT);
      break;
    case T.axis:
      bsInfo.set(RET_AXISVECTOR);
      break;
    case T.angle:
      bsInfo.set(RET_ROTANGLE);
      break;
    case T.matrix4f:
      bsInfo.set(RET_MATRIX);
      break;
    case T.unitcell:
      bsInfo.set(RET_UNITTRANS);
      break;
    case T.translate:
      // centering
      bsInfo.set(RET_CTRVECTOR);
      break;
    case T.times:
      bsInfo.set(RET_TIMEREV);
      break;
    case T.plane:
      bsInfo.set(RET_PLANE);
      break;
    case T.type:
      bsInfo.set(RET_TYPE);
      break;
    case T.id:
      bsInfo.set(RET_ID);
      break;
    case T.element:
    case T.var:
      bsInfo.set(RET_CTRANS);
      bsInfo.set(RET_INVCTR);
      bsInfo.set(RET_POINT);
      bsInfo.set(RET_AXISVECTOR);
      bsInfo.set(RET_PLANE);
      bsInfo.set(RET_INVARIANT);
      break;
    }
    return bsInfo;
  }


  private static V3d vtemp = new V3d();
  private static P3d ptemp = new P3d();
  private static P3d ptemp2 = new P3d();
  private static P3d pta01 = new P3d();
  private static P3d pta02 = new P3d();
  private static V3d vtrans = new V3d();
  private static P3d p1 = new P3d();
  private static P3d p0 = new P3d();
  private static P3d p3 = new P3d();
  private static P3d p4 = new P3d();

  /**
   * 
   * @param op
   * @param uc
   * @param ptAtom
   *        optional initial atom point
   * @param ptTarget
   *        optional target atom point
   * @param id
   * @param scaleFactor
   *        scale for rotation vector only
   * @param options
   *        0 or T.offset
   * @param haveTranslation
   *        TODO
   * @param bsInfo
   * @return Object[] containing:
   * 
   *         [0] xyz (Jones-Faithful calculated from matrix)
   * 
   *         [1] xyzOriginal (Provided by calling method)
   * 
   *         [2] info ("C2 axis", for example)
   * 
   *         [3] draw commands
   * 
   *         [4] translation vector (fractional)
   * 
   *         [5] translation vector (Cartesian)
   * 
   *         [6] inversion point
   * 
   *         [7] axis point
   * 
   *         [8] axis vector (defines plane if angle = 0
   * 
   *         [9] angle of rotation
   * 
   *         [10] matrix representation
   * 
   *         [11] lattice translation
   * 
   *         [12] centering
   * 
   *         [13] time reversal
   * 
   *         [14] plane
   * 
   *         [15] _type
   * 
   *         [16] id
   * 
   *         [17] element
   * 
   *         [18] invariant
   * 
   */
  private Object[] createInfoArray(SymmetryOperation op, SymmetryInterface uc,
                                 P3d ptAtom, P3d ptTarget, String id,
                                 double scaleFactor, int options,
                                 boolean haveTranslation, BS bsInfo) {
    if (!op.isFinalized)
      op.doFinalize();
    boolean matrixOnly = (bsInfo.cardinality() == 1 && bsInfo.get(RET_MATRIX));
    boolean isTimeReversed = (op.timeReversal == -1);
    if (scaleFactor == 0)
      scaleFactor = 1d;
    ptemp.set(0, 0, 0);
    vtrans.set(0, 0, 0);
    P4d plane = null;
    P3d pta00 = (ptAtom == null || Double.isNaN(ptAtom.x) ? new P3d() : P3d.newPd(ptAtom));

    if (ptTarget != null) {

      // Check to see that this is the correct operator
      // using cartesian here
      // Check to see if the two points only differ by
      // a translation after transformation.
      // If so, add that difference to the matrix transformation

      //      setFractional(uc, pta00, pta01, ptemp);
      //      op.rotTrans(pta01);
      //      uc.toCartesian(pta01, false);
      //      uc.toUnitCell(pta01, ptemp);
      //      pta02.setT(ptTarget);
      //      uc.toUnitCell(pta02, ptemp);
      //      if (pta01.distance(pta02) > 0.1d)
      //        return null;
      //      setFractional(uc, pta00, pta01, null);
      //      op.rotTrans(pta01);
      //      setFractional(uc, ptTarget, pta02, null);
      //      vtrans.sub2(pta02, pta01)

      pta01.setT(pta00);
      pta02.setT(ptTarget);
      uc.toFractional(pta01, true);
      uc.toFractional(pta02, true);
      op.rotTrans(pta01);
      ptemp.setT(pta01);
      uc.unitize(pta01);
      vtrans.setT(pta02);
      uc.unitize(pta02);
      //      uc.toCartesian(pta01, true);
      //      uc.toCartesian(pta02, true);
      //      if (pta01.distanceSquared(pta02) > 0.1d)
      //        return null;
      if (pta01.distanceSquared(pta02) >= JC.UC_TOLERANCE2)
        return null;
      vtrans.sub(ptemp);
    }
    M4d m2 = M4d.newM4(op);
    m2.add(vtrans);
    boolean isMagnetic = (op.timeReversal != 0);

    if (matrixOnly && !isMagnetic) {
      // quick-return -- note that this is not for magnetic!
      int im = getKeyType("matrix");
      Object[] o = new Object[-im];
      o[-1 - im] = m2;
      return o;
    }

    V3d ftrans = new V3d();

    // get the frame vectors and points

    pta01.set(1, 0, 0);
    pta02.set(0, 1, 0);
    P3d pta03 = P3d.new3(0, 0, 1);
    pta01.add(pta00);
    pta02.add(pta00);
    pta03.add(pta00);

    // target point, rotated, inverted, and translated

    P3d pt0 = rotTransCart(op, uc, pta00, vtrans);
    P3d pt1 = rotTransCart(op, uc, pta01, vtrans);
    P3d pt2 = rotTransCart(op, uc, pta02, vtrans);
    P3d pt3 = rotTransCart(op, uc, pta03, vtrans);

    V3d vt1 = V3d.newVsub(pt1, pt0);
    V3d vt2 = V3d.newVsub(pt2, pt0);
    V3d vt3 = V3d.newVsub(pt3, pt0);

    approx(vtrans);

    // check for inversion

    vtemp.cross(vt1, vt2);
    boolean haveInversion = (vtemp.dot(vt3) < 0);

    // The first trick is to check cross products to see if we still have a
    // right-hand axis.

    if (haveInversion) {

      // undo inversion for quaternion analysis (requires proper rotations only)

      pt1.sub2(pt0, vt1);
      pt2.sub2(pt0, vt2);
      pt3.sub2(pt0, vt3);

    }

    // The second trick is to use quaternions. Each of the three faces of the
    // frame (xy, yz, and zx)
    // is checked. The helix() function will return data about the local helical
    // axis, and the
    // symop(sym,{0 0 0}) function will return the overall translation.

    Qd q = Qd.getQuaternionFrame(pt0, pt1, pt2)
        .div(Qd.getQuaternionFrame(pta00, pta01, pta02));
    Qd qF = Qd.new4(q.q1, q.q2, q.q3, q.q0);
    T3d[] info = MeasureD.computeHelicalAxis(pta00, pt0, qF);
    // new T3[] { pt_a_prime, n, r, P3.new3(theta, pitch, residuesPerTurn), pt_b_prime };
    P3d pa1 = P3d.newPd(info[0]);
    P3d ax1 = P3d.newPd(info[1]);
    int ang1 = (int) Math.abs(PT.approxD(((P3d) info[3]).x, 1));
    double pitch1 = (double) SymmetryOperation.approxF(((P3d) info[3]).y);

    if (haveInversion) {

      // redo inversion

      pt1.add2(pt0, vt1);
      pt2.add2(pt0, vt2);
      pt3.add2(pt0, vt3);

    }

    V3d trans = V3d.newVsub(pt0, pta00);
    if (trans.length() < 0.1)
      trans = null;

    // ////////// determination of type of operation from first principles

    P3d ptinv = null; // inverted point for translucent frame
    P3d ipt = null; // inversion center
    P3d ptref = null; // reflection center

    double w = 0;
    double margin = 0; // for plane

    boolean isTranslation = (ang1 == 0);
    boolean isRotation = !isTranslation;
    boolean isInversionOnly = false;
    boolean isMirrorPlane = false;

    if (isRotation || haveInversion) {
      // the translation will be taken care of other ways
      trans = null;
    }

    // handle inversion

    if (haveInversion && isTranslation) {

      // simple inversion operation

      ipt = P3d.newP(pta00);
      ipt.add(pt0);
      ipt.scale(0.5d);
      ptinv = pt0;
      isInversionOnly = true;

    } else if (haveInversion) {

      /*
       * 
       * We must convert simple rotations to rotation-inversions; 2-screws to
       * planes and glide planes.
       * 
       * The idea here is that there is a relationship between the axis for a
       * simple or screw rotation of an inverted frame and one for a
       * rotation-inversion. The relationship involves two adjacent equilateral
       * triangles:
       * 
       * 
       *       o 
       *      / \
       *     /   \    i'
       *    /     \ 
       *   /   i   \
       * A/_________\A' 
       *  \         / 
       *   \   j   / 
       *    \     / 
       *     \   / 
       *      \ / 
       *       x
       *      
       * Points i and j are at the centers of the triangles. Points A and A' are
       * the frame centers; an operation at point i, j, x, or o is taking A to
       * A'. Point i is 2/3 of the way from x to o. In addition, point j is half
       * way between i and x.
       * 
       * The question is this: Say you have an rotation/inversion taking A to
       * A'. The relationships are:
       * 
       * 6-fold screw x for inverted frame corresponds to 6-bar at i for actual
       * frame 3-fold screw i for inverted frame corresponds to 3-bar at x for
       * actual frame
       * 
       * The proof of this follows. Consider point x. Point x can transform A to
       * A' as a clockwise 6-fold screw axis. So, say we get that result for the
       * inverted frame. What we need for the real frame is a 6-bar axis
       * instead. Remember, though, that we inverted the frame at A to get this
       * result. The real frame isn't inverted. The 6-bar must do that inversion
       * AND also get the frame to point A' with the same (clockwise) rotation.
       * The key is to see that there is another axis -- at point i -- that does
       * the trick.
       * 
       * Take a look at the angles and distances that arise when you project A
       * through point i. The result is a frame at i'. Since the distance i-i'
       * is the same as i-A (and thus i-A') and the angle i'-i-A' is 60 degrees,
       * point i is also a 6-bar axis transforming A to A'.
       * 
       * Note that both the 6-fold screw axis at x and the 6-bar axis at i are
       * both clockwise.
       * 
       * Similar analysis shows that the 3-fold screw i corresponds to the 3-bar
       * axis at x.
       * 
       * So in each case we just calculate the vector i-j or x-o and then factor
       * appropriately.
       * 
       * The 4-fold case is simpler -- just a parallelogram.
       */

      T3d d = (pitch1 == 0 ? new V3d() : ax1);
      double f = 0;
      switch (ang1) {
      case 60: // 6_1 at x to 6-bar at i
        f = 2d / 3d;
        break;
      case 120: // 3_1 at i to 3-bar at x
        f = 2;
        break;
      case 90: // 4_1 to 4-bar at opposite corner
        f = 1;
        break;
      case 180: // 2_1 to mirror plane
        // C2 with inversion is a mirror plane -- but could have a glide
        // component.
        ptref = P3d.newP(pta00);
        ptref.add(d);
        pa1.scaleAdd2(0.5, d, pta00);
        if (ptref.distance(pt0) > 0.1d) {
          trans = V3d.newVsub(pt0, ptref);
          setFractional(uc, trans, ptemp, null);
          ftrans.setT(ptemp);
        } else {
          trans = null;
        }
        vtemp.setT(ax1);
        vtemp.normalize();
        // ax + by + cz + d = 0
        // so if a point is in the plane, then N dot X = -d
        w = -vtemp.x * pa1.x - vtemp.y * pa1.y - vtemp.z * pa1.z;
        plane = P4d.new4((double) vtemp.x, (double) vtemp.y, (double) vtemp.z, (double) w);
        margin = (Math.abs(w) < 0.01f && vtemp.x * vtemp.y > 0.4 ? 1.30d
            : 1.05d);
        isRotation = false;
        haveInversion = false;
        isMirrorPlane = true;
        break;
      default:
        haveInversion = false;
        break;
      }
      if (f != 0) {
        vtemp.sub2(pta00, pa1);
        vtemp.add(pt0);
        vtemp.sub(pa1);
        vtemp.sub(d);
        vtemp.scale(f);
        pa1.add(vtemp);
        ipt = new P3d();
        ipt.scaleAdd2(0.5, d, pa1);
        ptinv = new P3d();
        ptinv.scaleAdd2(-2, ipt, pta00);
        ptinv.scale(-1);
      }

    } else if (trans != null) {

      // get rid of unnecessary translations added to keep most operations
      // within cell 555

      ptemp.setT(trans);
      uc.toFractional(ptemp, false);
      //      if (SymmetryOperation.approxF(ptemp.x) == 1)
      //        ptemp.x = 0;
      //      if (SymmetryOperation.approxF(ptemp.y) == 1)
      //        ptemp.y = 0;
      //      if (SymmetryOperation.approxF(ptemp.z) == 1)
      //        ptemp.z = 0;

      ftrans.setT(ptemp);
      uc.toCartesian(ptemp, false);
      trans.setT(ptemp);
    }

    // fix angle based on direction of axis

    int ang = ang1;
    approx0(ax1);

    pa1.putP(p1);
    pt0.putP(p0);
    
    if (isRotation) {
      
      P3d ptr = new P3d();

      vtemp.setT(ax1);

      // draw the lines associated with a rotation

      int ang2 = ang1;
      if (haveInversion) {
        ptr.add2(pa1, vtemp);
        ptr.putP(p3);
        ptinv.putP(p4);
        ang2 = (int) Math.round(MeasureD.computeTorsion(p4, p1, p3, p0, true));
      } else if (pitch1 == 0) {
        ptr.setT(pa1);
        ptemp.scaleAdd2(1, ptr, vtemp);
        ptemp.putP(p3);
        pta00.putP(p4);
        ang2 = (int) Math.round(MeasureD.computeTorsion(p4, p1, p3, p0, true));
      } else {
        ptemp.add2(pa1, vtemp);
        ptr.scaleAdd2(0.5d, vtemp,pa1);
        ptemp.putP(p3);
        pta00.putP(p4);
        ang2 = (int) Math.round(MeasureD.computeTorsion(p4, p1, p3, p0, true));
      }

      if (ang2 != 0)
        ang1 = ang2;

      if (!haveInversion && pitch1 == 0 && (ax1.z < 0
          || ax1.z == 0 && (ax1.y < 0 || ax1.y == 0 && ax1.x < 0))) {
        ax1.scale(-1);
        ang1 = -ang1;
      }

    }

    // time to get the description

    String info1 = null;
    String type = null;

    if (bsInfo.get(RET_LABEL) || bsInfo.get(RET_TYPE)) {

      info1 = type = "identity";

      if (isInversionOnly) {
        ptemp.setT(ipt);
        uc.toFractional(ptemp, false);
        info1 = "Ci: " + strCoord(op, ptemp, op.isBio);
        type = "inversion center";
      } else if (isRotation) {
        if (haveInversion) {
          type = info1 = (360 / ang) + "-bar axis";
        } else if (pitch1 != 0) {
          type = info1 = (360 / ang) + "-fold screw axis";
          ptemp.setT(ax1);
          uc.toFractional(ptemp, false);
          info1 += "|translation: " + strCoord(op, ptemp, op.isBio);
        } else {
          type = info1 = "C" + (360 / ang) + " axis";
        }
      } else if (trans != null) {
        String s = " " + strCoord(op, ftrans, op.isBio);
        if (isTranslation) {
          type = info1 = "translation";
          info1 += ":" + s;
        } else if (isMirrorPlane) {
          double fx = Math.abs(SymmetryOperation.approxD(ftrans.x));
          double fy = Math.abs(SymmetryOperation.approxD(ftrans.y));
          double fz = Math.abs(SymmetryOperation.approxD(ftrans.z));
          s = " " + strCoord(op, ftrans, op.isBio);
          // set ITA Table 2.1.2.1
          if (fx != 0 && fy != 0 && fz != 0) {
            if (fx == 1 / 4d && fy == 1 / 4d && fz == 1 / 4d) {
              // diamond
              info1 = "d-";
            } else if (fx == 1 / 2d && fy == 1 / 2d && fz == 1 / 2d) {
              info1 = "n-";
            } else {
              info1 = "g-";
            }
          } else if (fx != 0 && fy != 0 || fy != 0 && fz != 0
              || fz != 0 && fx != 0) {
            // any two
            if (fx == 1 / 4d && fy == 1 / 4d || fx == 1 / 4d && fz == 1 / 4d
                || fy == 1 / 4d && fz == 1 / 4d) {
              info1 = "d-";
            } else if (fx == 1 / 2d && fy == 1 / 2d
                || fx == 1 / 2d && fz == 1 / 2d
                || fy == 1 / 2d && fz == 1 / 2d) {
              // making sure here that this is truly a diagonal in the plane, not just
              // a glide parallel to a face on a diagonal plane! Mois Aroyo 2018
              if (fx == 0 && ax1.x == 0 || fy == 0 && ax1.y == 0
                  || fz == 0 && ax1.z == 0) {
                info1 = "g-";
              } else {
                info1 = "n-";
              }
            } else {
              info1 = "g-";
            }
          } else if (fx != 0)
            info1 = "a-";
          else if (fy != 0)
            info1 = "b-";
          else
            info1 = "c-";
          type = info1 = info1 + "glide plane";
          info1 += "|translation:" + s;
        }
      } else if (isMirrorPlane) {
        type = info1 = "mirror plane";
      }

      if (haveInversion && !isInversionOnly) {
        ptemp.setT(ipt);
        uc.toFractional(ptemp, false);
        info1 += "|inversion center at " + strCoord(op, ptemp, op.isBio);
      }

      if (isTimeReversed) {
        info1 += "|time-reversed";
        type += " (time-reversed)";
      }

    }

    String cmds = null;

    // check for drawing

    if (id != null && bsInfo.get(RET_DRAW)) {

      String opType = null;
      String drawid = "\ndraw ID " + id + "_";

      // delete previous elements of this user-settable ID

      SB draw1 = new SB();

      draw1.append(drawid).append("* delete");
      //    .append(
      //    ("print " + PT.esc(
      //        id + " " + (op.index + 1) + " " + op.fixMagneticXYZ(op, op.xyzOriginal, false) + "|"
      //            + op.fixMagneticXYZ(op, xyzNew, true) + "|" + info1).replace(
      //        '\n', ' '))).append("\n")

      // draw the initial frame

      drawLine(draw1, drawid + "frame1X", 0.15d, pta00, pta01, "red");
      drawLine(draw1, drawid + "frame1Y", 0.15d, pta00, pta02, "green");
      drawLine(draw1, drawid + "frame1Z", 0.15d, pta00, pta03, "blue");

      String color;

      if (isRotation) {

        P3d ptr = new P3d();

        color = "red";

        ang = ang1;
        double scale = 1d;
        vtemp.setT(ax1);

        // draw the lines associated with a rotation

        if (haveInversion) {
          opType = drawid + "rotinv";
          ptr.add2(pa1, vtemp);
          if (pitch1 == 0) {
            ptr.setT(ipt);
            vtemp.scale(3 * scaleFactor);
            ptemp.scaleAdd2(-1, vtemp, pa1);
            drawVector(draw1, drawid, "rotVector2", "", pa1, ptemp, "red");
          }
          scale = pt0.distance(ptr);
          draw1.append(drawid).append("rotLine1 ").append(Escape.ePd(ptr))
              .append(Escape.ePd(ptinv)).append(" color red");
          draw1.append(drawid).append("rotLine2 ").append(Escape.ePd(ptr))
              .append(Escape.ePd(pt0)).append(" color red");
        } else if (pitch1 == 0) {
          opType = drawid + "rot";
          boolean isSpecial = (pta00.distance(pt0) < 0.2d);
          if (!isSpecial) {
            draw1.append(drawid).append("rotLine1 ").append(Escape.ePd(pta00))
                .append(Escape.ePd(pa1)).append(" color red");
            draw1.append(drawid).append("rotLine2 ").append(Escape.ePd(pt0))
                .append(Escape.ePd(pa1)).append(" color red");
          }
          vtemp.scale(3 * scaleFactor);
          ptemp.scaleAdd2(-1, vtemp, pa1);
          drawVector(draw1, drawid, "rotVector2", "", pa1, ptemp,
              isTimeReversed ? "gray" : "red");
          ptr.setT(pa1);
          if (pitch1 == 0 && pta00.distance(pt0) < 0.2)
            ptr.scaleAdd2(0.5d, vtemp, ptr);
        } else {
          opType = drawid + "screw";
          color = "orange";
          draw1.append(drawid).append("rotLine1 ").append(Escape.ePd(pta00))
              .append(Escape.ePd(pa1)).append(" color red");
          ptemp.add2(pa1, vtemp);
          draw1.append(drawid).append("rotLine2 ").append(Escape.ePd(pt0))
              .append(Escape.ePd(ptemp)).append(" color red");
          ptr.scaleAdd2(0.5d, vtemp, pa1);
        }

        // draw arc arrow

        ptemp.add2(ptr, vtemp);
        if (haveInversion && pitch1 != 0) {
          draw1.append(drawid).append("rotRotLine1").append(Escape.ePd(ptr))
              .append(Escape.ePd(ptinv)).append(" color red");
          draw1.append(drawid).append("rotRotLine2").append(Escape.ePd(ptr))
              .append(Escape.ePd(pt0)).append(" color red");
        }
        draw1.append(drawid)
            .append(
                "rotRotArrow arrow width 0.1 scale " + PT.escD(scale) + " arc ")
            .append(Escape.ePd(ptr)).append(Escape.ePd(ptemp));
        ptemp.setT(haveInversion ? ptinv : pta00);
        if (ptemp.distance(pt0) < 0.1d)
          ptemp.set((double) Math.random(), (double) Math.random(),
              (double) Math.random());
        draw1.append(Escape.ePd(ptemp));
        ptemp.set(0, ang, 0);
        draw1.append(Escape.ePd(ptemp)).append(" color red");

        // draw the main vector

        drawVector(draw1, drawid, "rotVector1", "vector", pa1, vtemp,
            isTimeReversed ? "Gray" : color);

      } else if (isMirrorPlane) {

        // lavender arrow across plane from pt00 to pt0

        ptemp.sub2(ptref, pta00);
        if (pta00.distance(ptref) > 0.2)
          drawVector(draw1, drawid, "planeVector", "vector", pta00, ptemp,
              isTimeReversed ? "Gray" : "cyan");

        // faint inverted frame if mirror trans is not null

        opType = drawid + "plane";
        if (trans != null) {
          opType = drawid + "glide";
          drawFrameLine("X", ptref, vt1, 0.15d, ptemp, draw1, opType, "red");
          drawFrameLine("Y", ptref, vt2, 0.15d, ptemp, draw1, opType, "green");
          drawFrameLine("Z", ptref, vt3, 0.15d, ptemp, draw1, opType, "blue");
        }

        color = (trans == null ? "green" : "blue");

        // ok, now HERE's a good trick. We use the Marching Cubes
        // algorithm to find the intersection points of a plane and the unit
        // cell.
        // We expand the unit cell by 5% in all directions just so we are
        // guaranteed to get cutoffs.

        // returns triangles and lines
        Lst<Object> v = modelSet.vwr.getTriangulator().intersectPlane(plane,
            uc.getCanonicalCopy(margin, true), 3);
        if (v != null)
          for (int i = v.size(); --i >= 0;) {
            P3d[] pts = (P3d[]) v.get(i);
            draw1.append(drawid).append("planep").appendI(i).append(" ")
                .append(Escape.eP(pts[0])).append(Escape.eP(pts[1]));
            if (pts.length == 3)
              draw1.append(Escape.eP(pts[2]));
            draw1.append(" color translucent ").append(color);
          }

        // and JUST in case that does not work, at least draw a circle

        if (v == null || v.size() == 0) {
          ptemp.add2(pa1, ax1);
          draw1.append(drawid).append("planeCircle scale 2.0 circle ")
              .append(Escape.ePd(pa1)).append(Escape.ePd(ptemp))
              .append(" color translucent ").append(color).append(" mesh fill");
        }
      }

      if (haveInversion) {
        opType = drawid + "inv";
        draw1.append(drawid).append("invPoint diameter 0.4 ")
            .append(Escape.ePd(ipt));
        ptemp.sub2(ptinv, pta00);
        drawVector(draw1, drawid, "invArrow", "vector", pta00, ptemp,
            isTimeReversed ? "gray" : "cyan");
        if (!isInversionOnly && options != T.offset) {
          // n-bar: draw a faint frame showing the inversion
          drawFrameLine("X", ptinv, vt1, 0.15d, ptemp, draw1, opType, "red");
          drawFrameLine("Y", ptinv, vt2, 0.15d, ptemp, draw1, opType, "green");
          drawFrameLine("Z", ptinv, vt3, 0.15d, ptemp, draw1, opType, "blue");
        }
      }

      // and display translation if still not {0 0 0}

      if (trans != null) {
        if (ptref == null)
          ptref = P3d.newP(pta00);
        drawVector(draw1, drawid, "transVector", "vector", ptref, trans,
            isTimeReversed && !haveInversion && !isMirrorPlane && !isRotation
                ? "darkGray"
                : "gold");
      }

      // draw the final frame just a bit fatter and shorter, in case they
      // overlap

      ptemp2.setT(pt0);
      ptemp.sub2(pt1, pt0);
      ptemp.scaleAdd2(0.9d, ptemp, ptemp2);
      drawLine(draw1, drawid + "frame2X", 0.2d, ptemp2, ptemp, "red");
      ptemp.sub2(pt2, pt0);
      ptemp.scaleAdd2(0.9d, ptemp, ptemp2);
      drawLine(draw1, drawid + "frame2Y", 0.2d, ptemp2, ptemp, "green");
      ptemp.sub2(pt3, pt0);
      ptemp.scaleAdd2(0.9d, ptemp, ptemp2);
      drawLine(draw1, drawid + "frame2Z", 0.2d, ptemp2, ptemp, "purple");

      // color the targeted atoms opaque and add another frame if necessary

      draw1.append("\nsym_point = " + Escape.ePd(pta00));
      draw1.append("\nvar p0 = " + Escape.ePd(ptemp2));

      if (ptAtom instanceof Atom) {
        draw1.append(
            "\nvar set2 = within(0.2,p0);if(!set2){set2 = within(0.2,p0.uxyz.xyz)}");
        draw1.append("\n set2 &= {_" + ((Atom) ptAtom).getElementSymbol() + "}");
      } else {
        draw1.append(
            "\nvar set2 = p0.uxyz");
      }
      draw1.append("\nsym_target = set2;if (set2) {");
      //      if (haveCentering)
      //      draw1.append(drawid).append(
      //        "cellOffsetVector arrow @p0 @set2 color grey");
      if (options != T.offset && ptTarget == null && !haveTranslation) {
        draw1.append(drawid)
            .append("offsetFrameX diameter 0.20 @{set2.xyz} @{set2.xyz + ")
            .append(Escape.ePd(vt1)).append("*0.9} color red");
        draw1.append(drawid)
            .append("offsetFrameY diameter 0.20 @{set2.xyz} @{set2.xyz + ")
            .append(Escape.ePd(vt2)).append("*0.9} color green");
        draw1.append(drawid)
            .append("offsetFrameZ diameter 0.20 @{set2.xyz} @{set2.xyz + ")
            .append(Escape.ePd(vt3)).append("*0.9} color purple");
      }
      draw1.append("\n}\n");
      cmds = draw1.toString();
      if (Logger.debugging)
        Logger.info(cmds);
      draw1 = null;
      drawid = null;
    }

    // finalize returns

    if (trans == null)
      ftrans = null;
    if (isRotation) {
      if (haveInversion) {
      } else if (pitch1 == 0) {
      } else {
        // screw
        trans = V3d.newV(ax1);
        ptemp.setT(trans);
        uc.toFractional(ptemp, false);
        ftrans = V3d.newV(ptemp);
      }
    }
    if (isMirrorPlane) {
      ang1 = 0;
    }
    if (haveInversion) {
      if (isInversionOnly) {
        pa1 = null;
        ax1 = null;
        trans = null;
        ftrans = null;
      }
    } else if (isTranslation) {
      pa1 = null;
      ax1 = null;
    }
    // and display translation if still not {0 0 0}
    // TODO: here we need magnetic
    if (ax1 != null)
      ax1.normalize();

    String xyzNew = null;
    if (bsInfo.get(RET_XYZ) || bsInfo.get(RET_CIF2)) {
      xyzNew = (op.isBio ? m2.toString()
          : op.modDim > 0 ? op.xyzOriginal
              : SymmetryOperation.getXYZFromMatrix(m2, false, false, false));
      if (isMagnetic)
        xyzNew = op.fixMagneticXYZ(m2, xyzNew, true);
    }

    Object[] ret = new Object[RET_COUNT];
    for (int i = bsInfo.nextSetBit(0); i >= 0; i = bsInfo.nextSetBit(i + 1)) {
      switch (i) {
      case RET_XYZ:
        ret[i] = xyzNew;
        break;
      case RET_XYZORIGINAL:
        ret[i] = op.xyzOriginal;
        break;
      case RET_LABEL:
        ret[i] = info1;
        break;
      case RET_DRAW:
        ret[i] = cmds;
        break;
      case RET_FTRANS:
        ret[i] = approx0(ftrans);
        break;
      case RET_CTRANS:
        ret[i] = approx0(trans);
        break;
      case RET_INVCTR:
        ret[i] = approx0(ipt);
        break;
      case RET_POINT:
        ret[i] = approx0(pa1 != null && bsInfo.get(RET_INVARIANT) ? pta00 : pa1);
        break;
      case RET_AXISVECTOR:
        ret[i] = (plane == null ? approx0(ax1) : null);
        break;
      case RET_ROTANGLE:
        ret[i] = 
            (ang1 != 0 ? Integer.valueOf(ang1) : null);
        break;
      case RET_MATRIX:
        ret[i] = m2;
        break;
      case RET_UNITTRANS:
        ret[i] = (vtrans.lengthSquared() > 0 ? vtrans : null);
        break;
      case RET_CTRVECTOR:
        ret[i] = op.getCentering();
        break;
      case RET_TIMEREV:
        ret[i] = Integer.valueOf(op.timeReversal);
        break;
      case RET_PLANE:
        if (plane != null && bsInfo.get(RET_INVARIANT)) {
          double d = MeasureD.distanceToPlane(plane, pta00.putP(p1));
          plane.w += d;
        }
        ret[i] = plane;
        break;
      case RET_TYPE:
        ret[i] = type;
        break;
      case RET_ID:
        ret[i] = Integer.valueOf(op.number);
        break;
      case RET_CIF2:
        T3d cift = null;
        if (!op.isBio && !xyzNew.equals(op.xyzOriginal)) {
          if (op.number > 0) {
            M4d orig = SymmetryOperation.getMatrixFromXYZ(op.xyzOriginal);
            orig.sub(m2);
            cift = new P3d();
            orig.getTranslation(cift);
          }
        }
        int cifi = (op.number < 0 ? 0 : op.number);
        ret[i] = cifi + (cift == null ? " [0 0 0]"
            : " [" + (int) -cift.x + " " + (int) -cift.y + " " + (int) -cift.z
                + "]");
        break;
      case RET_XYZCANON:
        ret[i] = op.xyzCanonical;
        break;
      }
    }

    return ret;
  }

  private static void drawLine(SB s, String id, double diameter, P3d pt0, P3d pt1,
                               String color) {
    s.append(id).append(" diameter ").appendF(diameter).append(Escape.ePd(pt0))
        .append(Escape.ePd(pt1)).append(" color ").append(color);
  }

  private static void drawFrameLine(String xyz, P3d pt, V3d v, double width,
                                    P3d ptemp, SB draw1, String key, String color) {
    ptemp.setT(pt);
    ptemp.add(v);
    drawLine(draw1, key + "Pt" + xyz, width, pt, ptemp, "translucent " + color);
  }

  private static void drawVector(SB draw1, String drawid, String label,
                                 String type, T3d pt1, T3d v, String color) {
    draw1.append(drawid).append(label).append(" diameter 0.1 ").append(type)
        .append(Escape.ePd(pt1)).append(Escape.ePd(v)).append(" color ")
        .append(color);
  }

  /**
   * Set pt01 to pt00, possibly adding offset into unit cell
   * 
   * @param uc
   * @param pt00
   * @param pt01
   * @param offset
   */
  private static void setFractional(SymmetryInterface uc, T3d pt00, P3d pt01,
                                    P3d offset) {
    pt01.setT(pt00);
    if (offset != null)
      uc.toUnitCellD(pt01, offset);
    uc.toFractional(pt01, false);
  }

  private static P3d rotTransCart(SymmetryOperation op, SymmetryInterface uc,
                                 P3d pt00, V3d vtrans) {
    P3d p0 = P3d.newP(pt00);
    uc.toFractional(p0, false);
    op.rotTrans(p0);
    p0.add(vtrans);
    uc.toCartesian(p0, false);
    return p0;
  }

  private static String strCoord(SymmetryOperation op, T3d p, boolean isBio) {
    approx0(p);
    return (isBio ? p.x + " " + p.y + " " + p.z : op.fcoord2(p.putP(p4)));
  }

  private static T3d approx0(T3d pt) {
    if (pt != null) {
      if (Math.abs(pt.x) < 0.0001)
        pt.x = 0;
      if (Math.abs(pt.y) < 0.0001)
        pt.y = 0;
      if (Math.abs(pt.z) < 0.0001)
        pt.z = 0;
    }
    return pt;
  }

  private static T3d approx(T3d pt) {
    if (pt != null) {
      pt.x = SymmetryOperation.approxF((double) pt.x);
      pt.y = SymmetryOperation.approxF((double) pt.y);
      pt.z = SymmetryOperation.approxF((double) pt.z);
    }
    return pt;
  }

  /**
   * multipurpose function handling a variety of tasks, including:
   * 
   * processing of "lattice", "list", "atom", "point", and some "draw" output
   * types
   * 
   * finding the operator in the given space group
   * 
   * creating a temporary space group for an xyz operator
   * 
   * 
   * @param sym
   * @param iModel
   * @param iatom
   * @param uc
   * @param xyz
   * @param op
   * @param translation [i j k] to be added to operator
   * @param pt
   * @param pt2 second point or offset
   * @param id
   * @param type
   * @param scaleFactor
   * @param nth
   * @param options 0 or T.offset
   * @return a string or an Object[] containing information
   */
  private Object getSymmetryInfo(SymmetryInterface sym, int iModel, int iatom,
                                 SymmetryInterface uc, String xyz, int op, P3d translation,
                                 P3d pt, P3d pt2, String id,
                                 int type, double scaleFactor, int nth, int options) {
    int returnType = 0;
    Object nullRet = nullReturn(type);
    switch (type) {
    case T.lattice:
      return "" + uc.getLatticeType();
    case T.list:
      returnType = T.label;
      break;
    case T.array:
      returnType = getType(id);
      switch (returnType) {
      case T.atoms:
      case T.draw:
      case T.full:
      case T.list:
      case T.point:
      case T.element:
      case T.var:
        type = returnType;
        break;
      default:
        returnType = getKeyType(id);
        break;
      }
      break;
    }
    BS bsInfo = getInfoBS(returnType);

    int iop = op;
    P3d offset = (options == T.offset  && (type == T.atoms || type == T.point)? pt2 : null);
    if (offset != null)
      pt2 = null;
    Object[] info = null;
    String xyzOriginal = null;
    if (pt2 == null) {
      if (xyz == null) {
        SymmetryOperation[] ops = (SymmetryOperation[]) uc
            .getSymmetryOperations();
        if (ops == null || op == 0 || Math.abs(op) > ops.length)
          return nullRet;
        iop = Math.abs(op) - 1;
        xyz = (translation == null ? ops[iop].xyz : ops[iop].getxyzTrans(ptemp.setP(translation)));
        xyzOriginal = ops[iop].xyzOriginal;
      } else {
        iop = op = 0;
      }
      SymmetryInterface symTemp = new Symmetry();
      symTemp.setSpaceGroup(false);
      boolean isBio = uc.isBio();
      int i = (isBio ? symTemp.addBioMoleculeOperation(
          ((SpaceGroup) uc.getSpaceGroup()).finalOperations[iop], op < 0) : symTemp
          .addSpaceGroupOperation((op < 0 ? "!" : "=") + xyz, Math.abs(op)));

      if (i < 0)
        return nullRet;
      SymmetryOperation opTemp = (SymmetryOperation) symTemp
          .getSpaceGroupOperation(i);
      if (xyzOriginal != null)
        opTemp.xyzOriginal = xyzOriginal;
      opTemp.number = op;
      if (!isBio)
        opTemp.getCentering();
      if (pt == null && iatom >= 0)
        pt = modelSet.at[iatom];
      if (type == T.point || type == T.atoms) {
        if (isBio)
          return nullRet;
        symTemp.setUnitCell(uc);
        ptemp.setT(pt);
        uc.toFractional(ptemp, false);
        if (Double.isNaN(ptemp.x))
          return nullRet;
        P3d sympt = new P3d();
        symTemp.newSpaceGroupPoint(ptemp, i, null, 0, 0, 0, sympt);        
        if (options == T.offset) {
          uc.unitize(sympt);
          sympt.addF(offset);
        }
        symTemp.toCartesian(sympt, false);
        P3d ret = sympt;
        return (type == T.atoms ? getAtom(uc, iModel, iatom, ret) : ret);
      }
      info = createInfoArray(opTemp, uc, pt, null, (id == null ? "sym" : id),
          scaleFactor, options, (translation != null), bsInfo);
      if (type == T.array && id != null) {
        returnType = getKeyType(id);        
      }
    } else {
      // pt1, pt2
      String stype = "info";
      boolean asString = false;
      switch (type) {
      case T.array: // new Jmol 14.29.45
        returnType = getKeyType(id);
        id = stype = null;
        if (nth == 0)
          nth = -1;
        break;
      case T.list:
        id = stype = null;
        if (nth == 0)
          nth = -1;
        asString = true;
        bsInfo.set(RET_LIST);
        break;
      case T.draw:
        if (id == null)
          id = "sym";
        stype = "all";
        asString = true;
        break;
      case T.atoms:
        id = stype = null;
        //$FALL-THROUGH$
      default:
        if (nth == 0)
          nth = 1;
      }
      Object ret1 = getSymopInfoForPoints(sym, iModel, op, translation, pt, pt2, id,
          stype, scaleFactor, nth, options, bsInfo);
      if (asString)
        return ret1;
      if (ret1 instanceof String)
        return nullRet; // two atoms are not connected, no such oper
      info = (Object[]) ret1;
      if (type == T.atoms) {
        if (!(pt instanceof Atom) && !(pt2 instanceof Atom))
          iatom = -1;
        return (info == null ? nullRet : getAtom(uc, iModel, iatom,
            (T3d) info[7]));
      }
    }
    if (info == null)
      return nullRet;
    boolean isList = (info.length > 0 && info[0] instanceof Object[]);
    if (nth < 0 && op <= 0 && (type == T.array || isList)) {
     if (type == T.array && info.length > 0 && !(info[0] instanceof Object[]))
       info = new Object[] { info }; 
      Lst<Object> lst = new Lst<Object>();
      for (int i = 0; i < info.length; i++)
        lst.addLast(getInfo((Object[])info[i], returnType < 0 ? returnType : type));
      return lst;
    } else if (returnType < 0 && (nth >= 0 || op > 0)) {
      type = returnType;
    }
    if (nth > 0 && isList)
      info = (Object[]) info[0];
    return getInfo(info, type);
  }

  private BS getAtom(SymmetryInterface uc, int iModel, int iAtom, T3d sympt) {
    BS bsElement = null;
    if (iAtom >= 0)
      modelSet.getAtomBitsMDa(T.elemno,
          Integer.valueOf(modelSet.at[iAtom].getElementNumber()),
          bsElement = new BS());
    BS bsResult = new BS();
    modelSet.getAtomsWithin(0.02f, sympt, bsResult, iModel);
    if (bsElement != null)
      bsResult.and(bsElement);
    if (bsResult.isEmpty()) {
      sympt = P3d.newP(sympt);
      uc.toUnitCell(sympt, null);
      uc.toCartesianF(sympt, false);
      modelSet.getAtomsWithin(0.02f, sympt, bsResult, iModel);
      if (bsElement != null)
        bsResult.and(bsElement);
    }
    return bsResult;
  }

  ////// "public" methods ////////

  /**
   * get information about a symmetry operation relating two specific points or atoms
   * 
   * @param sym
   * @param modelIndex
   * @param symOp
   * @param translation TODO
   * @param pt1
   * @param pt2
   * @param drawID
   * @param stype
   * @param scaleFactor
   * @param nth
   * @param options 0 or T.offset
   * @param bsInfo
   * @return Object[] or String or Object[Object[]] (nth = 0, "array")
   * 
   */
  Object getSymopInfoForPoints(SymmetryInterface sym, int modelIndex, int symOp,
                                       P3d translation, P3d pt1, P3d pt2,
                                       String drawID, String stype, double scaleFactor,
                                       int nth, int options, BS bsInfo) {
    boolean asString = (bsInfo.get(RET_LIST) || bsInfo.get(RET_DRAW) && bsInfo.cardinality() == 3);
    bsInfo.clear(RET_LIST);
    Object ret = (asString ? "" : null);
    Map<String, Object> sginfo = getSpaceGroupInfo(sym, modelIndex, null,
        symOp, pt1, pt2, drawID, scaleFactor, nth, false, true, options, null, bsInfo);
    if (sginfo == null)
      return ret;
    Object[][] infolist = (Object[][]) sginfo.get("operations");
    // at this point, if we have two points, we have a full list of operations, but 
    // some are null. 
    if (infolist == null)
      return ret;
    SB sb = (asString ? new SB() : null);
    symOp--;
    boolean isAll = (!asString && symOp < 0);
    String strOperations = (String) sginfo.get("symmetryInfo");
    boolean labelOnly = "label".equals(stype);
    int n = 0;
    for (int i = 0; i < infolist.length; i++) {
      if (infolist[i] == null || symOp >= 0 && symOp != i)
        continue;
      if (!asString) {
        if (!isAll)
          return infolist[i];
        infolist[n++] = infolist[i];
        continue;
      }
      if (drawID != null)
        return ((String) infolist[i][3]) + "\nprint " + PT.esc(strOperations);
      if (sb.length() > 0)
        sb.appendC('\n');
      if (!labelOnly) {
        if (symOp < 0)
          sb.appendI(i + 1).appendC('\t');
        sb.append((String) infolist[i][0]).appendC('\t'); //xyz
      }
      sb.append((String) infolist[i][2]); //desc
    }
    if (!asString) {
      Object[] a = new Object[n];
      for (int i = 0; i < n; i++)
        a[i] = infolist[i];
      return a;
    }
    if (sb.length() == 0)
      return (drawID != null ? "draw " + drawID + "* delete" : ret);
    return sb.toString();
  }
  
  /**
   * 
   * @param iAtom
   * @param xyz
   * @param op
   * @param translation TODO
   * @param pt
   * @param pt2
   * @param id
   * @param type
   * @param scaleFactor
   * @param nth
   * @param options 0 or T.offset
   * @return "" or a bitset of matching atoms, or 
   */
  Object getSymopInfo(int iAtom, String xyz, int op, P3d translation, P3d pt, P3d pt2,
                      String id, int type, double scaleFactor, int nth, int options) {
    if (type == 0)
      type = getType(id);
    Object ret = (type == T.atoms ? new BS() : "");
    if (iAtom < 0)
      return ret;
    
    // get model symmetry
    
    int iModel = modelSet.at[iAtom].mi;
    SymmetryInterface uc = modelSet.am[iModel].biosymmetry;
    if (uc == null && (uc = modelSet.getUnitCell(iModel)) == null)
      return ret;

    // generally get the result from getSymmetryInfo
    
    if (type != T.draw || op != Integer.MAX_VALUE) {
      return getSymmetryInfo(uc, iModel, iAtom, uc, xyz,
          op, translation, pt, pt2, id, type, scaleFactor, nth, options);
    }
    
    // draw SPACEGROUP
    
    String s = "";
    M4d[] ops = uc.getSymmetryOperations();
    if (ops != null) {
      if (id == null)
        id = "sg";
      int n = ops.length;
      for (op = 1; op <= n; op++)
        s += (String) getSymmetryInfo(uc, iModel, iAtom,
            uc, xyz, op, translation, pt, pt2, id + op, T.draw, scaleFactor, nth, options);
    }
    return s;
  }

  @SuppressWarnings("unchecked")
  Map<String, Object> getSpaceGroupInfo(SymmetryInterface sym, int modelIndex,
                                        String sgName, int symOp, P3d pt1,
                                        P3d pt2, String drawID,
                                        double scaleFactor, int nth,
                                        boolean isFull, boolean isForModel,
                                        int options, SymmetryInterface cellInfo,
                                        BS bsInfo) {
    if (bsInfo == null) {
      // just for SHOW SYMOP
      bsInfo = new BS();
      bsInfo.setBits(0, keys.length);
      bsInfo.set(RET_XYZ);
      bsInfo.set(RET_LABEL);
    }
    boolean matrixOnly = (bsInfo.cardinality() == 1 && bsInfo.get(RET_MATRIX));
    Map<String, Object> info = null;
    boolean isStandard = (!matrixOnly && pt1 == null && drawID == null
        && nth <= 0 && bsInfo.cardinality() >= keys.length);
    boolean isBio = false;
    String sgNote = null;
    boolean haveName = (sgName != null && sgName.length() > 0);
    boolean haveRawName = (haveName && sgName.indexOf("[--]") >= 0);
    if (isForModel || !haveName) {
      boolean saveModelInfo = (isStandard && symOp == 0);
      if (matrixOnly) {
        cellInfo = sym;
      } else {
        if (modelIndex < 0)
          modelIndex = (pt1 instanceof Atom ? ((Atom) pt1).mi
              : modelSet.vwr.am.cmi);
        if (modelIndex < 0)
          sgNote = "no single current model";
        else if (cellInfo == null
            && !(isBio = (cellInfo = modelSet.am[modelIndex].biosymmetry) != null)
            && (cellInfo = modelSet.getUnitCell(modelIndex)) == null)
          sgNote = "not applicable";
        if (sgNote != null) {
          info = new Hashtable<String, Object>();
          info.put("spaceGroupInfo", "");
          info.put("spaceGroupNote", sgNote);
          info.put("symmetryInfo", "");
        } else if (isStandard) {
          info = (Map<String, Object>) modelSet.getInfo(modelIndex,
              "spaceGroupInfo");
        }
        // created once
        if (info != null)
          return info;

        // show symop or symop(a,b)

        // full check
        sgName = cellInfo.getSpaceGroupName();

      }
      info = new Hashtable<String, Object>();
      SymmetryOperation[] ops = (SymmetryOperation[]) cellInfo
          .getSymmetryOperations();
      SpaceGroup sg = (isBio ? ((Symmetry) cellInfo).spaceGroup : null);
      String slist = (haveRawName ? "" : null);
      int opCount = 0;
      if (ops != null) {
        if (!matrixOnly) {
          if (isBio)
            sym.setSpaceGroupTo(SpaceGroup.getNull(false, false, false));
          else
            sym.setSpaceGroup(false);
        }
        // check to make sure that new group has been created magnetic or not
        if (ops[0].timeReversal != 0)
          ((SymmetryOperation) sym.getSpaceGroupOperation(0)).timeReversal = 1;
        Object[][] infolist = new Object[ops.length][];
        String sops = "";
        for (int i = 0, nop = 0; i < ops.length && nop != nth; i++) {
          SymmetryOperation op = ops[i];
          String xyzOriginal = op.xyzOriginal;
          int iop;
          if (matrixOnly) {
            iop = i;
          } else {
            boolean isNewIncomm = (i == 0 && op.xyz.indexOf("x4") >= 0);
            iop = (!isNewIncomm && sym.getSpaceGroupOperation(i) != null ? i
                : isBio
                    ? sym.addBioMoleculeOperation(sg.finalOperations[i], false)
                    : sym.addSpaceGroupOperation("=" + op.xyz, i + 1));
            if (iop < 0)
              continue;
            op = (SymmetryOperation) sym.getSpaceGroupOperation(i);
            if (op == null)
              continue;
            op.xyzOriginal = xyzOriginal;
          }
          if (op.timeReversal != 0 || op.modDim > 0)
            isStandard = false;
          if (slist != null)
            slist += ";" + op.xyz;
          Object[] ret = (symOp > 0 && symOp - 1 != iop ? null
              : createInfoArray(op, cellInfo, pt1, pt2, drawID, scaleFactor,
                  options, false, bsInfo));
          if (ret != null) {
            if (nth > 0 && ++nop != nth)
              continue;
            infolist[i] = ret;
            if (!matrixOnly)
              sops += "\n" + (i + 1) + "\t" + ret[RET_XYZ] + "\t"
                  + ret[RET_LABEL];
            opCount++;
          }
        }
        info.put("operations", infolist);
        if (!matrixOnly)
          info.put("symmetryInfo",
              (sops.length() == 0 ? "" : sops.substring(1)));
      }
      if (matrixOnly) {
        return info;
      }
      sgNote = (opCount == 0 ? "\n no symmetry operations"
          : nth <= 0 && symOp <= 0
              ? "\n" + opCount + " symmetry operation"
                  + (opCount == 1 ? ":\n" : "s:\n")
              : "");
      if (slist != null)
        sgName = slist.substring(slist.indexOf(";") + 1);
      if (saveModelInfo)
        modelSet.setInfo(modelIndex, "spaceGroupInfo", info);
    } else {
      info = new Hashtable<String, Object>();
    }
    info.put("spaceGroupName", sgName);
    info.put("spaceGroupNote", sgNote == null ? "" : sgNote);
    Object data;
    if (isBio) {
      data = sgName;
    } else {
      if (haveName && !haveRawName)
        sym.setSpaceGroupName(sgName);
      data = sym.getSpaceGroupInfoObj(sgName, cellInfo, isFull, !isForModel);
      if (data == null || data.equals("?")) {
        data = "?";
        info.put("spaceGroupNote",
            "could not identify space group from name: " + sgName
                + "\nformat: show spacegroup \"2\" or \"P 2c\" "
                + "or \"C m m m\" or \"x, y, z;-x ,-y, -z\"");
      }
    }
    info.put("spaceGroupInfo", data);
    return info;
  }

  public M4d getTransform(UnitCell uc, SymmetryOperation[] ops, P3d fracA,
                         P3d fracB, boolean best) {
    if (pta01 == null) {
      pta01 = new P3d();
      pta02 = new P3d();
      ptemp = new P3d();
      vtrans = new V3d();
    }
    pta02.setT(fracB);
    vtrans.setT(pta02);
    uc.unitize(pta02);
    double dmin = Double.MAX_VALUE;
    int imin = -1;
    for (int i = 0, n = ops.length; i < n; i++) {
      SymmetryOperation op = ops[i];
      pta01.setT(fracA);
      op.rotTrans(pta01);
      ptemp.setT(pta01);
      uc.unitize(pta01);
      double d = pta01.distanceSquared(pta02);
      if (d < JC.UC_TOLERANCE2) {
        vtrans.sub(ptemp);
        uc.normalize12ths(vtrans);
        M4d m2 = M4d.newM4(op);
        m2.add(vtrans);
        // but check...
        pta01.setT(fracA);
        m2.rotTrans(pta01);
        uc.unitize(pta01);
        d = pta01.distanceSquared(pta02);
        if (d >= JC.UC_TOLERANCE2) {
          continue;
        }
        return m2;
      }
      if (d < dmin) {
        dmin = d;
        imin = i;
      }
    }
    if (best) {
      SymmetryOperation op = ops[imin];
      pta01.setT(fracA);
      op.rotTrans(pta01);
      uc.unitize(pta01);
      System.err.println("" + imin + " " + pta01.distance(pta02) + " " + pta01
          + " " + pta02 + " " + V3d.newVsub(pta02, pta01));
    }
    return null;
  }


}
