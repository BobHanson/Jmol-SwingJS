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

import org.jmol.api.Interface;
import org.jmol.api.SymmetryInterface;
import org.jmol.modelset.Atom;
import org.jmol.modelset.ModelSet;
import org.jmol.script.T;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.viewer.JC;

import javajs.util.A4d;
import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.M3d;
import javajs.util.M4d;
import javajs.util.MeasureD;
import javajs.util.P3d;
import javajs.util.P4d;
import javajs.util.PT;
import javajs.util.Qd;
import javajs.util.SB;
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


  private static final P3d ptemp = new P3d();
  private static final P3d ptemp2 = new P3d();
  private static final P3d pta01 = new P3d();
  private static final P3d pta02 = new P3d();
  private static final V3d vtrans = new V3d();

  private final static String THIN_LINE = "0.05";
  private final static String THICK_LINE = "0.1";

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
  private final static int RET_XYZNORMALIZED = 19;
  private final static int RET_SPIN = 20;
  private final static int RET_COUNT = 21;

  // additional flags
  final static int RET_LIST = 22;
  final static int RET_INVARIANT = 23;
  final static int RET_RXYZ = 24;

  private final static String[] keys = { "xyz", "xyzOriginal", "label",
      null /*draw*/, "fractionalTranslation", "cartesianTranslation",
      "inversionCenter", null /*point*/, "axisVector", "rotationAngle",
      "matrix", "unitTranslation", "centeringVector", "timeReversal", "plane",
      "_type", "id", "cif2", "xyzCanonical", "xyzNormalized", "spin" };

  private static final String COLOR_PLANE_MIRROR = "magenta";
  private static final String COLOR_PLANE_A_GLIDE = "[x4080ff]"; // azure (light blue) 
  private static final String COLOR_PLANE_B_GLIDE = "blue";
  private static final String COLOR_PLANE_C_GLIDE = "cyan";
  private static final String COLOR_PLANE_D_GLIDE = "grey";
  private static final String COLOR_PLANE_G_GLIDE = "lightgreen";
  private static final String COLOR_PLANE_N_GLIDE = "orange"; // naranja
  private static final String COLOR_SCREW_1       = "orange";
  private static final String COLOR_SCREW_2       = "blue";
  private static final String COLOR_2             = "red";
  private static final String COLOR_BAR_3         = "[xA00040]";
  private static final String COLOR_BAR_4         = "[x800080]";
  private static final String COLOR_BAR_6         = "[x4000A0]";
  private static final String COLOR_GLIDE_ARROW = "green";
  private static final String COLOR_ARROW_TIME_REVERSED = "orange";
  private static final String COLOR_CENTERING_ARROW = "gold";
  private static final String COLOR_CENTERING_ARROW_TIME_REVERSED = "darkgray";
 
  private ModelSet modelSet;
  private String drawID;

  public SymmetryDesc() {
    // for reflection
  }

  public SymmetryDesc set(ModelSet modelSet) {
    this.modelSet = modelSet;
    return this;
  }
  
  ////// "public" methods ////////

  /**
   * get information about a symmetry operation relating two specific points or
   * atoms
   * 
   * @param sym
   * @param modelIndex
   * @param symOp
   * @param translation
   *        TODO
   * @param pt1
   * @param pt2
   * @param drawID
   * @param stype
   * @param scaleFactor
   * @param nth
   * @param options
   *        0 or T.offset
   * @param bsInfo
   * @return Object[] or String or Object[Object[]] (nth = 0, "array")
   * 
   */
  Object getSymopInfoForPoints(SymmetryInterface sym, int modelIndex, int symOp,
                               P3d translation, P3d pt1, P3d pt2, String drawID,
                               String stype, double scaleFactor, int nth,
                               int options, BS bsInfo) {
    boolean asString = (bsInfo.get(RET_LIST)
        || bsInfo.get(RET_DRAW) && bsInfo.cardinality() == 3);
    bsInfo.clear(RET_LIST);
    Object ret = (asString ? "" : null);
    Map<String, Object> sginfo = getSpaceGroupInfo(sym, modelIndex, null, symOp,
        pt1, pt2, drawID, scaleFactor, nth, false, true, options, null, bsInfo);
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
      return (drawID != null ? "draw ID \"" + drawID + "*\" delete" : ret);
    return sb.toString();
  }

  private String getDrawID(String id) {
    return drawID + id +"\" ";
  }

  /**
   * 
   * @param iAtom
   * @param xyz
   * @param op
   * @param translation
   *        TODO
   * @param pt
   * @param pt2
   * @param id
   * @param type
   * @param scaleFactor
   * @param nth
   * @param options
   *        0 or T.offset
   * @param opList
   *        TODO
   * @return "" or a bitset of matching atoms, or
   */
  Object getSymopInfo(int iAtom, String xyz, int op, P3d translation, P3d pt,
                      P3d pt2, String id, int type, double scaleFactor, int nth,
                      int options, int[] opList) {
    if (type == 0)
      type = getType(id);
    Object ret = (type == T.atoms ? new BS() : "");
    int iModel = (iAtom >= 0 ? modelSet.at[iAtom].mi : modelSet.vwr.am.cmi);
    if (iModel < 0)
      return ret;

    // get model symmetry

    SymmetryInterface uc = modelSet.am[iModel].biosymmetry;
    if (uc == null && (uc = modelSet.getUnitCell(iModel)) == null) {
      // just assign a simple [1 1 1 90 90 90] unit cell
      // no vwr object here
      uc = new Symmetry().setUnitCellFromParams(null, false, Double.NaN);
      //      return ret;
    }

    // generally get the result from getSymmetryInfo

    if (type != T.draw || op != Integer.MAX_VALUE && opList == null) {
      return getSymmetryInfo(iModel, iAtom, uc, xyz, op, translation, pt, pt2,
          id, type, scaleFactor, nth, options, false);
    }

    // draw SPACEGROUP or draw SYMOP [...] @a

    if (uc == null)
      return ret;

    boolean isSpaceGroup = (xyz == null && nth < 0 && opList == null);
    String s = "";
    M4d[] ops = (isSpaceGroup && nth == -2 ? uc.getAdditionalOperations()
        : uc.getSymmetryOperations());
   if (ops != null) {
      if (id == null)
        id = "sg";
      int n = ops.length;
      if (pt != null && pt2 == null || opList != null) {
        if (opList == null)
          opList = uc.getInvariantSymops(pt, null);
        n = opList.length;
        for (int i = 0; i < n; i++) {
          if (nth > 0 && nth != i + 1)
            continue;
          op = opList[i];
          s += (String) getSymmetryInfo(iModel, iAtom, uc, xyz, op, translation,
              pt, pt2, id + op, T.draw, scaleFactor, nth, options, pt == null);
        }
      } else {
        for (op = 1; op <= n; op++) {
          s += (String) getSymmetryInfo(iModel, iAtom, uc, xyz, op, translation,
              pt, pt2, id + op, T.draw, scaleFactor, nth, options, true);
        }
      }
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
      bsInfo.clear(RET_XYZNORMALIZED);
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
          info.put(JC.INFO_SPACE_GROUP_INFO, "");
          info.put(JC.INFO_SPACE_GROUP_NOTE, sgNote);
          info.put("symmetryInfo", "");
        } else if (isStandard) {
          info = (Map<String, Object>) modelSet.getInfo(modelIndex,
              JC.INFO_SPACE_GROUP_INFO);
        }
        // created once
        if (info != null)
          return info;

        // show symop or symop(a,b)

        // full check
        sgName = cellInfo.getSpaceGroupName();

      }
      int nDim = cellInfo.getDimensionality();
      info = new Hashtable<String, Object>();
      SymmetryOperation[] ops = (SymmetryOperation[]) cellInfo
          .getSymmetryOperations();
      SpaceGroup sg = (isBio ? ((Symmetry) cellInfo).spaceGroup : null);
      String slist = (haveRawName ? "" : null);
      int opCount = 0;
      if (ops != null) {
        // next line added because otherwise atom.symmetry is lost from the "file" 
        sym = Interface.getSymmetry(null, "desc");
        if (!matrixOnly) {
          if (isBio) {
            sym.setSpaceGroupTo(SpaceGroup.getNull(false, false, false));
          } else {
            sym.setSpaceGroup(false);
            if (cellInfo.getGroupType() != SpaceGroup.TYPE_SPACE) {
              SpaceGroup sg0 = (SpaceGroup) cellInfo.getSpaceGroup();
              SpaceGroup sg1 = (SpaceGroup) sym.getSpaceGroup();
              sg1.nDim = nDim;
              sg1.groupType = sg0.groupType;
              sg1.setClegId(sg0.getClegId());
            }
          }
        }
        // check to make sure that new group has been created magnetic or not
        Object[][] infolist = new Object[ops.length][];
        String sops = "";
        int i0 = (drawID == null || pt1 == null || pt2 == null && nth < 0 ? 0
            : 1);
        for (int i = i0, nop = 0; i < ops.length && nop != nth; i++) {
          SymmetryOperation op = ops[i];
          String xyzOriginal = op.xyzOriginal;
          String xyzCanonical = op.xyzCanonical;
          M4d spinUOrig = op.spinU;
          int timeReversal = op.timeReversal;
          int spinIndex = op.spinIndex;
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
            op.xyzCanonical = xyzCanonical;
            op.spinU = spinUOrig;
            op.spinIndex = spinIndex;
            op.timeReversal = timeReversal;
          }
          if (op.timeReversal != 0 || op.modDim > 0)
            isStandard = false;
          if (slist != null)
            slist += ";" + op.xyz;
          Object[] ret = (symOp > 0 && symOp - 1 != iop ? null
              : createInfoArray(op, cellInfo, pt1, pt2, drawID, scaleFactor,
                  options, false, bsInfo, false, false, nDim));
          if (ret != null) {
            nop++;
            if (nth > 0 && nop != nth)
              continue;
            infolist[i] = ret;
            if (!matrixOnly)
              sops += "\n" + (i + 1) + (drawID != null && nop == 1 ? "*" : "")
                  + "\t"
                  + ret[bsInfo.get(RET_XYZNORMALIZED) ? RET_XYZNORMALIZED
                      : nDim == 2 ? RET_XYZCANON : RET_XYZ]
                  + "\t  " + ret[RET_LABEL];
            opCount++;
            if (symOp > 0)
              break;
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
        modelSet.setInfo(modelIndex, JC.INFO_SPACE_GROUP_INFO, info);
    } else {
      info = new Hashtable<String, Object>();
    }
    info.put(JC.INFO_SPACE_GROUP_NAME, sgName);
    info.put(JC.INFO_SPACE_GROUP_NOTE, sgNote == null ? "" : sgNote);
    Object data;
    if (isBio) {
      data = sgName;
    } else {
      if (haveName && !haveRawName)
        sym.setSpaceGroupName(sgName);
      data = sym.getSpaceGroupInfoObj(sgName,
          (cellInfo == null ? null : cellInfo.getUnitCellParams()), isFull,
          !isForModel);
      if (data == null || data.equals("?")) {
        data = "?";
        info.put(JC.INFO_SPACE_GROUP_NOTE,
            "could not identify space group from name: " + sgName
                + "\nformat: show spacegroup \"2\" or \"P 2c\" "
                + "or \"C m m m\" or \"x, y, z;-x ,-y, -z\"\n");
      }
    }
    info.put(JC.INFO_SPACE_GROUP_INFO, data);
    return info;
  }

  public M4d getTransform(UnitCell uc, SymmetryOperation[] ops, P3d fracA,
                          P3d fracB, boolean best) {
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
        SymmetryOperation.normalize12ths(vtrans);
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
    }
    return null;
  }


  //////////// private methods ///////////

  /**
   * Determine the type of this request. Note that label and xyz will be
   * returned as T.xyz and T.label
   * 
   * @param id
   * @return a code that identifies this request.
   */
  private static int getType(String id) {
    int type;
    if (id == null)
      return T.list;
    switch (id.toLowerCase()) {
    case "xyzcanonical":
      return T.x;
    case "xyzoriginal":
      return T.origin;
    case "matrix":
      return T.matrix4f;
    case "description":
      return T.label;
    case "axispoint":
      return T.point;
    case "time":
      return T.times;
    case "info":
      return T.array;
    case "element":
      return T.element;
    case JC.MODELKIT_INVARIANT:
      return T.var;
    }
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
    switch (type) {
    case T.draw:
      return ";draw ID sym* delete;draw ID sg* delete;";
    case T.full:
    case T.label:
    case T.id:
    case T.xyz:
    case T.fuxyz:
    case T.matrix3f:
    case T.x:
    case T.origin:
    case T.rxyz:
    case T.spin:
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
   * @param nDim 
   * @return object specified
   * 
   */

  private static Object getInfo(Object[] io, int type, int nDim) {
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
        String key = (j == RET_DRAW ? "draw"
            : j == RET_POINT ? "axispoint" : keys[j]);
        if (io[j] != null)
          lst.put(key, io[j]);
      }
      return lst;
    case T.list:
      return io[RET_ID] + "\t" + io[nDim == 2 ? RET_XYZCANON : RET_XYZ] + "  \t" + io[RET_LABEL];
    case T.full:
      return io[RET_XYZ] + "  \t" + io[RET_LABEL];
    case T.xyz:
      return io[RET_XYZ];
    case T.fuxyz:
      return io[RET_XYZNORMALIZED];
    case T.x:
      return io[RET_XYZCANON];
    case T.origin:
      return io[RET_XYZORIGINAL];
    default:
    case T.label:
      return io[RET_LABEL];
    case T.spacegroup:
      if (!Logger.debugging)
        return io[RET_DRAW];      
      //$FALL-THROUGH$
    case T.draw:
      return io[RET_DRAW] + "\nprint "
          + PT.esc(io[RET_XYZ] + " " + io[RET_LABEL]);
    case T.fracxyz:
      return io[RET_FTRANS]; // fractional translation "fxyz"
    case T.translation:
      return io[RET_CTRANS]; // cartesian translation
    case T.center:
      return io[RET_INVCTR];
    case T.point:
      return io[RET_POINT];
    case T.spin:
      return io[RET_SPIN];
         case T.axis:
      return io[RET_AXISVECTOR];
    case T.angle:
      return io[RET_ROTANGLE];
    case T.rxyz:
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
      return new Object[] { io[RET_INVCTR], io[RET_POINT], io[RET_AXISVECTOR],
          io[RET_PLANE], io[RET_CTRANS] };
    case T.var:
      // presumption here is that we already know there is an invariance here
      // so a screw axis is allowed. Apparently the diagonal screw 
      // sets the new point in the original location plus a lattice translation
      // see SG 224 Wyckoff i
      // ah, but "trans" is not just the translation vector. 
      // a diagonal plane will be (0.7071067811865476, -0.7071067811865475, 0, 0) with "translation" 1/2 1/2
      // However, there is a problem in p/12 with symop 8 diagonal mirror -y+1/2,-x+1/2,z
      // if the nDim check is not here, then an atom on the line intersecting the 
      // plane of the group fails to move properly. The translation is 
      // not indicated.
      return (io[RET_INVCTR] != null ? io[RET_INVCTR] // inversion center 
              : io[RET_AXISVECTOR] != null ? new Object[] { io[RET_POINT], io[RET_AXISVECTOR], io[RET_CTRANS] } // axis
                  : io[RET_CTRANS] != null ? "none" // translation 
                      : io[RET_PLANE] != null ? io[RET_PLANE] // plane
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
    case T.fuxyz:
      bsInfo.set(RET_XYZNORMALIZED);
      break;
    case T.x:
      bsInfo.set(RET_XYZCANON);
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
//      bsInfo.set(RET_XYZCANON);
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
    case T.spin:
      bsInfo.set(RET_SPIN);
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

  /**
   * 
   * @param op
   * @param uc
   * @param ptFrom
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
   * @param isSpaceGroup
   *        DRAW SPACEGROUP
   * @param isSpaceGroupAll
   *        DRAW SPACEGROUP ALL
   * @param nDim 
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
                                   P3d ptFrom, P3d ptTarget, String id,
                                   double scaleFactor, int options,
                                   boolean haveTranslation, BS bsInfo,
                                   boolean isSpaceGroup,
                                   boolean isSpaceGroupAll, int nDim) {

    op.doFinalize();

    boolean matrixOnly = (bsInfo.get(RET_MATRIX) & (bsInfo.cardinality() == (bsInfo.get(RET_RXYZ) ? 2 : 1)));
    boolean isTimeReversed = (op.timeReversal == -1);
    boolean isSpinSG = (op.spinU != null);
    if (scaleFactor == 0)
      scaleFactor = 1;
    vtrans.set(0, 0, 0);
    P4d plane = null;

    P3d pta00 = (ptFrom == null || Double.isNaN(ptFrom.x)
        ? uc.getCartesianOffset()
        : ptFrom);
    if (ptTarget != null) {

      // Check to see that this is the correct operator
      // using cartesian here
      // Check to see if the two points only differ by
      // a translation after transformation.
      // If so, add that difference to the matrix transformation

      pta01.setT(pta00);
      pta02.setT(ptTarget);
      uc.toFractional(pta01, false);
      uc.toFractional(pta02, false);
      op.rotTrans(pta01);
      ptemp.setT(pta01);
      uc.unitize(pta01);
      vtrans.setT(pta02);
      uc.unitize(pta02);
      if (pta01.distanceSquared(pta02) >= JC.UC_TOLERANCE2)
        return null;
      vtrans.sub(ptemp);
    }
    M4d m2 = M4d.newM4(op);
    m2.add(vtrans);
    if (bsInfo.get(RET_MATRIX) && ptTarget != null && pta00.equals(ptTarget)) {
      // must be integer if points are identical -- double precision issue
      m2.m00 = Math.round(m2.m00);
      m2.m01 = Math.round(m2.m01);
      m2.m02 = Math.round(m2.m02);
      m2.m03 = Math.round(m2.m03);
      m2.m10 = Math.round(m2.m10);
      m2.m11 = Math.round(m2.m11);
      m2.m12 = Math.round(m2.m12);
      m2.m13 = Math.round(m2.m13);
      m2.m20 = Math.round(m2.m20);
      m2.m21 = Math.round(m2.m21);
      m2.m22 = Math.round(m2.m22);
      m2.m23 = Math.round(m2.m23);
    }

    boolean isMagnetic = (op.timeReversal != 0 || op.spinU != null);

    if (matrixOnly && !isMagnetic) {
      // quick-return -- note that this is not for magnetic!
      int im = getKeyType("matrix");
      Object[] o = new Object[-im];
      o[-1 - im] = (bsInfo.get(RET_RXYZ) ? SymmetryOperation.matrixToRationalString(m2) : m2);
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

    SymmetryOperation.approx6Pt(vtrans);

    // check for inversion

    V3d vtemp = new V3d();
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

    //System.out.println("pt012 " + pta00 + " " + pta01 + " " + pta02);
    Qd q = Qd.getQuaternionFrame(pt0, pt1, pt2)
        .div(Qd.getQuaternionFrame(pta00, pta01, pta02));
    Qd qF = Qd.new4(q.q1, q.q2, q.q3, q.q0);
    final T3d[] info = MeasureD.computeHelicalAxis(pta00, pt0, qF);
    // new T3[] { pt_a_prime, n, r, P3.new3(theta, pitch, residuesPerTurn), pt_b_prime };
    P3d pa1 = P3d.newP(info[0]);
    P3d ax1 = P3d.newP(info[1]);
    int ang1 = (int) Math.abs(PT.approxD(((P3d) info[3]).x, 1));
    // one of the issues here is that the axes calc always returns a
    // positive angle
    double pitch1 = SymmetryOperation.approx(((P3d) info[3]).y);

    if (haveInversion) {

      // redo inversion

      pt1.add2(pt0, vt1);
      pt2.add2(pt0, vt2);
      pt3.add2(pt0, vt3);

    }

    V3d trans = V3d.newVsub(pt0, pta00);
    if (trans.length() < 0.1d)
      trans = null;

    // ////////// determination of type of operation from first principles

    P3d ptinv = null; // inverted point for translucent frame
    P3d ipt = null; // inversion center
    P3d ptref = null; // reflection center
    V3d vShift = null; // layer group shift along C
    double w = 0;
    double margin = 0; // for plane

    boolean isTranslation = (ang1 == 0);
    boolean isRotation = !isTranslation;
    boolean isInversionOnly = false;
    boolean isMirrorPlane = false;
    boolean isTranslationOnly = !isRotation && !haveInversion;

    if (isRotation || haveInversion) {
      // the translation will be taken care of other ways
      trans = null;
    }

    // handle inversion

    boolean notC = false;
    /**
     * Indicates that our vertical planes need to be shifted down 1/2 c
     */
    int periodicity = uc.getPeriodicity();
    boolean shiftA = ((periodicity & 0x1) == 0); // rod (ab)c
    boolean shiftB = ((periodicity & 0x2) == 0); // frieze a(b), rod (ab)c
    boolean shiftC = ((periodicity & 0x4) == 0); // plane ab, layer ab(c), frieze a(b)

    vShift = V3d.new3(0,  0,  1);
    if (nDim == 3) {
      // special issue with rods
      switch (periodicity) {
      case 0x1:
        vShift = V3d.new3(1,  0,  0);
        notC = true;
        break;
      case 0x2:
        notC = true;
        vShift = V3d.new3(0,  1,  0);
        break;
      case 0x4:
        notC = true;
        break;
      }
    }
    /**
     * will depend upon the dot product; used to show the second vector for a
     * rotation
     */
    boolean isPeriodic = !(shiftA || shiftB || shiftC);

    // check for axis colinear with c. We will not show the second of these
    // if they exist
    double dot = Math.abs(ax1.dot(vShift) / vShift.length() / ax1.length());
    if (Math.abs(dot - 1) < 0.001d) {
      // c dot axis = 1
      notC = false;
      vShift = null;
    } else {
      isPeriodic = !notC;
    }

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
        pa1.scaleAdd2(0.5d, d, pta00);
        if (ptref.distance(pt0) > 0.1d) {
          trans = V3d.newVsub(pt0, ptref);
          ftrans.setT(trans);
          uc.toFractional(ftrans, true);// ignore offset, as this is a vector
        } else {
          trans = null;
        }
        vtemp.setT(ax1);
        vtemp.normalize();
        // ax + by + cz + d = 0
        // so if a point is in the plane, then N dot X = -d
        w = -vtemp.x * pa1.x - vtemp.y * pa1.y - vtemp.z * pa1.z;
        plane = P4d.new4(vtemp.x, vtemp.y, vtemp.z, w);
        margin = (Math.abs(w) < 0.01f && vtemp.x * vtemp.y > 0.4 ? 1.30d
            : 1.05d);
        isRotation = false;
        haveInversion = false;
        isMirrorPlane = true;
        if (shiftC) {
          vShift = V3d.newVsub(pta00, pta03);
          dot = Math.abs(ax1.dot(vShift) / vShift.length() / ax1.length());
          shiftC = (dot < 0.001d);
          if (shiftC) {
            vShift.scale(0.5d);
            uc.toCartesian(vShift, true);
          }
        }
        if (shiftB) {
          V3d vs = V3d.newVsub(pta00, pta02);
          dot = notC ? 0 : Math.abs(ax1.dot(vs) / vs.length() / ax1.length());
          shiftB = (dot < 0.001d);
          if (shiftB) {
            vs.scale(0.5d);
            uc.toCartesian(vs, true);
            if (shiftC) {
              vShift.add(vs);
            } else {
              vShift = vs;
            }
          }
        }
        if (shiftA) {
          V3d vs = V3d.newVsub(pta00, pta01);
          dot = notC ? 0 : Math.abs(ax1.dot(vs) / vs.length() / ax1.length());
          shiftA = (dot < 0.001d);
          if (shiftA) {
            vs.scale(0.5d);
            uc.toCartesian(vs, true);
            if (shiftB || shiftC) {
              vShift.add(vs);
            } else {
              vShift = vs;
            }
          }
        }
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
        ipt.scaleAdd2(0.5d, d, pa1);
        ptinv = new P3d();
        ptinv.scaleAdd2(-2, ipt, pt0);
        ptinv.scale(-1);
      }

    } else if (trans != null) {
      ptemp.setT(trans);
      uc.toFractional(ptemp, false);
      ftrans.setT(ptemp);
      uc.toCartesian(ptemp, false);
      trans.setT(ptemp);
    }

    // fix angle based on direction of axis

    int ang = ang1;
    approx0(ax1);
    if (isRotation) {
      P3d ptr = new P3d();
      vtemp.setT(ax1);
      int ang2 = ang1;
      P3d p0;
      if (haveInversion) {
        ptr.setT(ptinv);
        p0 = ptinv;
      } else if (pitch1 == 0) {
        p0 = pt0;
        ptr.setT(pa1);
      } else {
        p0 = pt0;
        ptr.scaleAdd2(0.5d, vtemp, pa1);
      }
      ptemp.add2(pa1, vtemp);
      ang2 = (int) Math
          .round(MeasureD.computeTorsion(pta00, pa1, ptemp, p0, true));
      if (SymmetryOperation.approx(ang2) != 0) {
        ang1 = ang2;
        if (ang1 < 0)
          ang1 = 360 + ang1;
      }

    }
    // time to get the description

    String info1 = null;
    String type = null;

    char glideType = 0;
    boolean isIrrelevant = op.isIrrelevant;
    int order = op.getOpOrder();
    op.isIrrelevant |= isIrrelevant;
    Boolean isccw = op.getOpIsCCW();
    int screwDir = 0;
    int nrot = 0;
    if (bsInfo.get(RET_LABEL) || bsInfo.get(RET_TYPE)) {

      info1 = type = "identity";

      if (isInversionOnly) {
        ptemp.setT(ipt);
        uc.toFractional(ptemp, false);
        info1 = "Ci: " + strCoord(ptemp, op.isBio);
        type = "inversion center";
      } else if (isRotation) {
        String screwtype = "";
        if (isccw != null) {
          screwtype = (isccw == Boolean.TRUE ? "(+)" : "(-)");
          screwDir = (isccw == Boolean.TRUE ? 1 : -1);
          if (haveInversion && screwDir == -1)
            isIrrelevant = true;
        }
        nrot = 360 / ang;
        if (haveInversion) {
          // n-bar
          info1 = nrot + "-bar" + screwtype + " axis";
        } else if (pitch1 != 0) {
          // screw axis
          ptemp.setT(ax1);
          uc.toFractional(ptemp, true);
          info1 = nrot + screwtype + " (" + strCoord(ptemp, op.isBio)
              + ") screw axis";

        } else {
          info1 = nrot + screwtype + " axis";
          if (order % 2 == 0)
            screwDir *= order / 2; // 6_3, 4_2
        }
        type = info1;
      } else if (trans != null) {
        String s = " " + strCoord(ftrans, op.isBio);
        if (nDim == 2)
          s = s.substring(0, s.lastIndexOf(' '));
        if (isTranslation) {
          type = info1 = "translation";
          info1 += ":" + s;
        } else if (isMirrorPlane) {
          if (isSpaceGroup) {
            fixGlideTrans(ftrans);
            trans.setT(ftrans);
            uc.toCartesian(trans, true);
          }
          s = " " + strCoord(ftrans, op.isBio);
          if (nDim == 2)
            s = s.substring(0, s.lastIndexOf(' '));
          // set ITA Table 2.1.2.1
          glideType = SymmetryOperation.getGlideFromTrans(ftrans, ax1);

          type = info1 = glideType + "-glide plane";
          info1 += "|translation:" + s;
        }
      } else if (isMirrorPlane) {
        type = info1 = "mirror plane";
      }

      if (haveInversion && !isInversionOnly) {
        ptemp.setT(ipt);
        uc.toFractional(ptemp, false);
        info1 += "|at " + strCoord(ptemp, op.isBio);
      }

      if (isTimeReversed) {
        info1 += "|time-reversed";
        type += " (time-reversed)";
      }

    }

    boolean isRightHand = true;
    boolean isScrew = (isRotation && !haveInversion && pitch1 != 0);
    if (!isScrew) {
      screwDir = 0;
      // not a screw axis
      isRightHand = checkHandedness(uc, ax1);
      if (!isRightHand) {
        ang1 = -ang1;
        if (ang1 < 0)
          ang1 = 360 + ang1;
        ax1.scale(-1);
      }
    }

    // check for drawing

    boolean ignore = false;
    String cmds = null;
    while (true) {
      if (id == null || !bsInfo.get(RET_DRAW))
        break;

      if (op.getOpType() == SymmetryOperation.TYPE_IDENTITY
          || isSpaceGroupAll && op.isIrrelevant) {
        if (Logger.debugging)
          System.out
              .println("!!SD irrelevent " + op.getOpTitle() + op.getOpPoint());
        cmds = "";
        break;
      }
      String opType = null;
      drawID = "\ndraw ID \"" + id;

      // delete previous elements of this user-settable ID
      SB drawSB = new SB();

      drawSB.append(getDrawID("*")).append(" delete");
      //    .append(
      //    ("print " + PT.esc(
      //        id + " " + (op.index + 1) + " " + op.fixMagneticXYZ(op, op.xyzOriginal, false) + "|"
      //            + op.fixMagneticXYZ(op, xyzNew, true) + "|" + info1).replace(
      //        '\n', ' '))).append("\n")

      // draw the initial frame

      boolean drawFrameZ = (nDim == 3);
      if (!isSpaceGroup) {
        drawLine(drawSB, "frame1X", 0.15d, pta00, pta01, "red");
        drawLine(drawSB, "frame1Y", 0.15d, pta00, pta02, "green");
        if (drawFrameZ)
          drawLine(drawSB, "frame1Z", 0.15d, pta00, pta03, "blue");
      }
      String color;
      P3d planeCenter = null;
      int nPC = 0;

      boolean isSpecial = (pta00.distance(pt0) < 0.2d);

      String title = (isSpaceGroup
          ? "<hover>" + id + ": " + (nDim == 2 ? op.xyz.replace(",z", "") : op.xyz) + "|" + info1 + "</hover>"
          : null);

      // now check for:
      //
      // 1) rotation or mirror plane
      // 2) inversion
      // 3) translation

      if (isRotation) {

        color = (nrot == 2 ? COLOR_2
            : nrot == 3 ? COLOR_BAR_3 : nrot == 4 ? COLOR_BAR_4 : COLOR_BAR_6);

        ang = ang1;
        double scale = 1d;
        vtemp.setT(ax1);

        // draw the lines associated with a rotation

        String wp = "";
        if (isSpaceGroup) {
          pa1.setT(op.getOpPoint());
          uc.toCartesian(pa1, false);
        }
        P3d ptr = new P3d();
        if (pitch1 != 0 && !haveInversion) {
          // screw axis
          opType = "screw";
          color = (isccw == Boolean.TRUE ? COLOR_SCREW_1
              : isccw == Boolean.FALSE ? COLOR_SCREW_2 // was yellow
                  : order == 4 ? "lightgray" : "grey");
          if (!isSpaceGroup) {
            drawLine(drawSB, "rotLine1", 0.1d, pta00, pa1, "red");
            ptemp.add2(pa1, vtemp);
            drawLine(drawSB, "rotLine2", 0.1d, pt0, ptemp, "red");
            ptr.scaleAdd2(0.5d, vtemp, pa1);
          }
        } else {

          // check here for correct direction

          ptr.setT(pa1);

          if (!isRightHand) {
            if (!isSpecial && !isSpaceGroup)
              pa1.sub2(pa1, vtemp);
          }
          if (haveInversion) {
            // rotation-inversion
            opType = "bar";

            if (isSpaceGroup) {
              vtemp.normalize();
              if (isccw == Boolean.TRUE) {
                vtemp.scale(-1);
              }
            } else {
              if (pitch1 == 0) {
                // atom to atom or no change in atom position
                ptr.setT(ipt);
                vtemp.scale(3 * scaleFactor);
                if (isSpecial) {
                  ptemp.scaleAdd2(0.25d, vtemp, pa1);
                  pa1.scaleAdd2(-0.24d, vtemp, pa1);
                  ptr.scaleAdd2(0.31d, vtemp, ptr);
                  color = "cyan";
                } else {
                  ptemp.scaleAdd2(-1, vtemp, pa1);
                  //                drawVector(drawSB, drawid, "rotVector2", "", pa1, ptemp, "red");
                  drawLine(drawSB, "rotLine1", 0.1d, pta00, ipt, "red");
                  drawLine(drawSB, "rotLine2", 0.1d, ptinv, ipt, "red");
                }
              } else if (!isSpecial) {
                scale = pta00.distance(ptr);
                drawLine(drawSB, "rotLine1", 0.1d, pta00, ptr, "red");
                drawLine(drawSB, "rotLine2", 0.1d, ptinv, ptr, "red");
              }
            }
          } else {
            // simple rotation
            opType = "rot";
            vtemp.scale(3 * scaleFactor);
            if (isSpecial) {
              // flat
            } else {
              // lines from base
              if (!isSpaceGroup) {
                drawLine(drawSB, "rotLine1", 0.1d, pta00, ptr, "red");
                drawLine(drawSB, "rotLine2", 0.1d, pt0, ptr, "red");
              }
            }
            ptr.setT(pa1);
            if (pitch1 == 0 && isSpecial)
              ptr.scaleAdd2(0.25d, vtemp, ptr);
          }
        }

        // draw arc arrow

        if (!isSpaceGroup) {
          if (ang > 180) {
            // "(+)" is CCW, (-) is CW
            ang = 180 - ang;
          }
          ptemp.add2(ptr, vtemp);
          drawSB.append(getDrawID("rotRotArrow"))
              .append(" arrow width 0.1 scale " + PT.escD(scale) + " arc ")
              .append(Escape.eP(ptr)).append(Escape.eP(ptemp));
          ptemp.setT(pta00);
          if (ptemp.distance(pt0) < 0.1d)
            ptemp.set(Math.random(), Math.random(), Math.random());
          drawSB.append(Escape.eP(ptemp));
          ptemp.set(0, ang - 5 * Math.signum(ang), 0);
          drawSB.append(Escape.eP(ptemp)).append(" color red");
        }

        // draw the main vector

        double d;
        // idea here is that we only show the smaller rotation if
        // there are, say, a 3(+)1/3 and a 3(-)2/3, because one implies the other.
        // but there is no smaller rotation for 63(+)1/2 -- the rotation is 60o. All others generate translations.

        double opTransLength = 0;
        if (!op.opIsLong && (isSpaceGroupAll && pitch1 > 0 && !haveInversion)) {

          // skewDir is what defines the pictogram direction. 
          // Whichever one is >= 1/2 is the key, technically.
          // So the challenge here is to indicate the right one. 
          // Actually, this should probably just be to show the (+), 
          // and then switch the symbol depending upon length, as discussed
          // in ITA(1969) Table 4.1.7.

          // But, either way, when there is a large 6(+) script, so for 
          // example, the rotation is (0,0,2/3), as in P6422 (#181), 
          // there is also in the same location a SMALL 3(+), because the 
          // 3(+) associated with the 6(+) is then (0,0,4/3) -- that is, 
          // (0,0,1/3), and so the combination always produces an oppositely
          // directed 3-skew symbol. 

          // The ITA simply ignores this and only shows the 6(+) symbol.

          // The solution here had to be made earlier in the process, where
          // multiple operations are available. Specifically, in the calculation
          // of additonal operators. This is what SymmetryOperation.isIrrelevant is for. 

          // The following crazy calculation has to do with how symbols might
          // change when changing settings, particularly :h>>:r.
          // And perhaps the difference in diagonal 3-screw axes in cubic groups.

          ignore = ((opTransLength = op.getOpTrans().length()) > (order == 2
              ? 0.71d
              : order == 3 ? 0.578d
                  : order == 4 ? 0.51d
                      // 6:  1/2 is OK
                      //    : 0.3d
                      : 0.51d));

        }
        if (ignore && Logger.debugging) {
          System.out.println("SD ignoring " + op.getOpTrans().length() + " "
              + op.getOpTitle() + op.xyz);
          //ignore = false; // set true for debugging only
        }

        P3d p2 = null;
        if (pitch1 == 0 && !haveInversion) {
          // simple rotation
          ptemp.scaleAdd2(0.5d, vtemp, pa1);
          pa1.scaleAdd2(isSpaceGroup ? -0.5d : -0.45d, vtemp, pa1);
          if (isSpaceGroupAll && isPeriodic
              && (p2 = op.getOpPoint2()) != null) {
            // second vector on other side of the cell
            ptr.setT(p2);
            uc.toCartesian(ptr, false);
            ptr.scaleAdd2(-0.5d, vtemp, ptr);
          }
          if (isSpaceGroup) {
            // when showing the space group, adjust the length 
            // based on the order and +/- sense
            scaleByOrder(vtemp, order, isccw);
          }
        } else if (isSpaceGroupAll && pitch1 != 0 && !haveInversion
            && (d = op.getOpTrans().length()) > 0.4d) {
          // all space group screw
          if (isccw == Boolean.TRUE) {
            // n/a
          } else if (isccw == null) {
            // 2-fold
            // maybe add a second
            //              p2 = P3d.newP(pa1);
            //              p2.add(vtemp);
            //              ptr.scaleAdd2(2d, vtemp, pa1);
            //              uc.toFractional(ptr, false);
            //              if (SymmetryOperation
            //                  .checkOpPoint(SymmetryOperation.opClean(ptr)))
            //                ptr.setT(p2);
            //              else
            //                p2 = null;
          } else if (d == 0.5d) {
            ignore = true;
          }
        } else if (isSpaceGroup && haveInversion) {
          // all space group n-bar
          // pitch1 here is 120 or 60 or 0 ??
          scaleByOrder(vtemp, order, isccw);
          wp = "80";
        }
        if (pitch1 > 0 && !haveInversion) {
          wp = "" + (90 - (int) (vtemp.length() / pitch1 * 90));
        }
        if (!ignore) {

          if (screwDir != 0) {
            // get the polygon wings right (isSpaceGroupAll only)
            switch (order) {
            case 2:
              // ignoring
              break;
            case 3:
              // +/-1 is fine
              break;
            case 4:
              if (opTransLength > 0.49)
                screwDir = -2;
              break;
            case 6:
              if (opTransLength > 0.49)
                screwDir = -3; // convention
              else if (opTransLength > 0.33)
                screwDir *= 2;
              break;
            }
            color = (screwDir < 0 ? COLOR_SCREW_2 : COLOR_SCREW_1);
          }
          // BH 2025.06.09 explicit naming should only be for DRAW SPACEGROUP 
          // otherwise this breaks code designed to get information from the drawing 
          // of specific operations, as in ITAOnLine
          String name = (isSpaceGroup ? opType + "_" + nrot : "") + "rotvector1";
          drawOrderVector(drawSB, name, "vector", THICK_LINE + wp, pa1, nrot,
              screwDir, haveInversion && isSpaceGroupAll, isccw == Boolean.TRUE,
              vtemp, isTimeReversed ? COLOR_ARROW_TIME_REVERSED : color, title, isSpaceGroupAll);
          if (p2 != null) {
            // second standard rotation arrow on other side of unit cell only
            drawOrderVector(drawSB, name + "2", "vector", THICK_LINE + wp, ptr,
                order, screwDir, haveInversion, isccw == Boolean.TRUE, vtemp,
                isTimeReversed ? "gray" : color, title, isSpaceGroupAll);
          }
        }
      } else if (isMirrorPlane) {

        // lavender arrow across plane from pt00 to pt0

        ptemp.sub2(ptref, pta00);
        if (!isSpaceGroup && pta00.distance(ptref) > 0.2d)
          drawVector(drawSB, "planeVector", "vector", THIN_LINE, pta00, ptemp,
              isTimeReversed ? "gray" : "cyan", null);

        // faint inverted frame if mirror trans is not null

        opType = "plane";
        if (trans == null) {
          color = COLOR_PLANE_MIRROR;
        } else {
          opType = "glide";
          switch (glideType) {
          case 'a':
            color = COLOR_PLANE_A_GLIDE;
            break;
          case 'b':
            color = COLOR_PLANE_B_GLIDE;
            break;
          case 'c':
            color = COLOR_PLANE_C_GLIDE;
            break;
          case 'n':
            color = COLOR_PLANE_N_GLIDE;
            break;
          case 'd':
            color = COLOR_PLANE_D_GLIDE;
            break;
          case 'g':
          default:
            color = COLOR_PLANE_G_GLIDE;
            break;
          }
          if (!isSpaceGroup) {
            drawFrameLine("X", ptref, vt1, 0.15d, ptemp, drawSB, opType, "red");
            drawFrameLine("Y", ptref, vt2, 0.15d, ptemp, drawSB, opType,
                "green");
            if (drawFrameZ)
              drawFrameLine("Z", ptref, vt3, 0.15d, ptemp, drawSB, opType,
                "blue");
          }
        }

        // ok, now HERE's a good trick. We use the Marching Cubes
        // algorithm to find the intersection points of a plane and the unit
        // cell.
        // We expand the unit cell by 5% in all directions just so we are
        // guaranteed to get cutoffs.

        // returns triangles and lines
        P3d[] points = uc.getCanonicalCopy(margin, true);
        if ((shiftA || shiftB || shiftC)) {
          for (int i = 8; --i >= 0;) {
            points[i].add(vShift);
          }
        }
        Lst<Object> v = modelSet.vwr.getTriangulator().intersectPlane(plane,
            points, 3);
        if (v != null) {
          int iCoincident = (isSpaceGroup ? op.iCoincident : 0);
          planeCenter = new P3d();

          for (int i = 0, iv = 0, n = v.size(); i < n; i++) {
            P3d[] pts = (P3d[]) v.get(i);
            // these lines provide a rendering when side-on
            drawSB
                .append(getDrawID((trans == null ? "mirror_" : glideType + "_g")
                    + "planep" + i))
                .append(Escape.eP(pts[0])).append(Escape.eP(pts[1]));
            if (pts.length == 3) {
              // 110 has <---d---> double-ended arrow OK this is due to a z+3/4 and a matching z-1/4 which is the same in this case  
              // 122 has <---d---> same deal 
              // 166 crossed diag  fixed sg183 2/3 -2/3 1/3  sg133 mrror
              if (iCoincident == 0 || (iv % 2 == 0) != (iCoincident == 1)) {
                drawSB.append(Escape.eP(pts[2]));
              }
              iv++;
            } else {
              planeCenter.add(pts[0]);
              planeCenter.add(pts[1]);
              nPC += 2;
            }
            drawSB.append(" color translucent ").append(color);
            if (title != null)
              drawSB.append(" ").append(PT.esc(title));
          }
        }

        // and JUST in case that does not work, at least draw a circle

        if (v == null || v.size() == 0) {
          if (isSpaceGroupAll) {
            // space group 185 can give odd results
            ignore = true;
          } else {
            ptemp.add2(pa1, ax1);
            drawSB.append(getDrawID("planeCircle")).append(" scale 2.0 circle ")
                .append(Escape.eP(pa1)).append(Escape.eP(ptemp))
                .append(" color translucent ").append(color)
                .append(" mesh fill");
            if (title != null)
              drawSB.append(" ").append(PT.esc(title));
          }
        }

      } // isMirrorPlane or isRotation

      if (haveInversion) {
        opType = "inv";
        if (isInversionOnly) {
          drawSB.append(getDrawID("inv_point")).append(" diameter 0.4 ")
              .append(Escape.eP(ipt));
          if (title != null)
            drawSB.append(" ").append(PT.esc(title));

          ptemp.sub2(ptinv, pta00);
          if (!isSpaceGroup) {
            drawVector(drawSB, "Arrow", "vector", THIN_LINE, pta00, ptemp,
                isTimeReversed ? "gray" : "cyan", null);
          }
        } else {
          if (order == 4) {
            drawSB.append(getDrawID("RotPoint"))
                .append(" diameter 0.3 color red").append(Escape.eP(ipt));
            if (title != null)
              drawSB.append(" ").append(PT.esc(title));
          }
          if (!isSpaceGroup) {
            drawSB.append(" color cyan");
            if (!isSpecial) {
              ptemp.sub2(pt0, ptinv);
              drawVector(drawSB, "Arrow", "vector", THIN_LINE, ptinv, ptemp,
                  isTimeReversed ? "gray" : "cyan", null);
            }
            if (options != T.offset) {
              // n-bar: draw a faint frame showing the inversion
              vtemp.setT(vt1);
              vtemp.scale(-1);
              drawFrameLine("X", ptinv, vtemp, 0.15d, ptemp, drawSB, opType,
                  "red");
              vtemp.setT(vt2);
              vtemp.scale(-1);
              drawFrameLine("Y", ptinv, vtemp, 0.15d, ptemp, drawSB, opType,
                  "green");
              vtemp.setT(vt3);
              vtemp.scale(-1);
              if (drawFrameZ)
                drawFrameLine("Z", ptinv, vtemp, 0.15d, ptemp, drawSB, opType,
                  "blue");
            }
          }
        }
      } // haveInversion

      // and display translation if still not {0 0 0}

      if (trans != null) {
        // centering and glides
        if (isMirrorPlane && isSpaceGroup) {
          if (planeCenter != null) {
            ptref = planeCenter;
            ptref.scale(1d / nPC);
            ptref.scaleAdd2(-0.5d, trans, ptref);
          }
        } else if (ptref == null) {
          ptref = (isSpaceGroup ? pta00 : P3d.newP(pta00));
        }
        if (ptref != null && !ignore) {
          boolean isCentered = (glideType == '\0');
          boolean isGlide = (isSpaceGroup && !isCentered && !isTranslationOnly);
          if (isGlide) {
            ptemp.scaleAdd2(0.5d, trans, ptref);
            vtrans.setT(trans);
            vtrans.scale(0.5d);
          } else {
            ptemp.setT(ptref);
            vtrans.setT(trans);
          }
          color = (isGlide ? (isTimeReversed ? COLOR_ARROW_TIME_REVERSED 
              : COLOR_GLIDE_ARROW)
              : isTimeReversed 
              && (isSpinSG || !haveInversion 
              && !isMirrorPlane
              && !isRotation) 
              ? COLOR_CENTERING_ARROW_TIME_REVERSED 
                  : COLOR_CENTERING_ARROW);
          drawVector(drawSB,
              (isCentered ? "centering_" : glideType + "_g") + "trans_vector",
              "vector", (isGlide || isTranslationOnly ? THICK_LINE : THIN_LINE),
              ptemp, vtrans, color, title);
          if (isGlide) {
            // draw reverse arrow as well
            vtrans.scale(-1);
            drawVector(drawSB, glideType + "_g" + "trans_vector2", "vector",
                THICK_LINE, ptemp, vtrans, color, title);
          }
        }
      } // trans != null

      if (!isSpaceGroup) {
        // draw the final frame just a bit fatter and shorter, in case they
        // overlap

        ptemp2.setT(pt0);
        ptemp.sub2(pt1, pt0);
        ptemp.scaleAdd2(0.9d, ptemp, ptemp2);
        drawLine(drawSB, "frame2X", 0.2d, ptemp2, ptemp, "red");
        ptemp.sub2(pt2, pt0);
        ptemp.scaleAdd2(0.9d, ptemp, ptemp2);
        drawLine(drawSB, "frame2Y", 0.2d, ptemp2, ptemp, "green");
        ptemp.sub2(pt3, pt0);
        ptemp.scaleAdd2(0.9d, ptemp, ptemp2);
        if (drawFrameZ)
          drawLine(drawSB, "frame2Z", 0.2d, ptemp2, ptemp, "purple");

        // color the targeted atoms opaque and add another frame if necessary

        drawSB.append("\nsym_point = " + Escape.eP(pta00));
        drawSB.append("\nvar p0 = " + Escape.eP(ptemp2));

        if (pta00 instanceof Atom) {
          drawSB.append(
              "\nvar set2 = within(0.2,p0);if(!set2){set2 = within(0.2,p0.uxyz.xyz)}");
          drawSB.append(
              "\n set2 &= {_" + ((Atom) pta00).getElementSymbol() + "}");
        } else {
          drawSB.append("\nvar set2 = p0.uxyz");
        }
        drawSB.append("\nsym_target = set2;if (set2) {");
        //      if (haveCentering)
        //      drawSB.append(drawid).append(
        //        "cellOffsetVector arrow @p0 @set2 color grey");
        if (!isSpecial && options != T.offset && ptTarget == null
            && !haveTranslation) {
          drawSB.append(getDrawID("offsetFrameX"))
              .append(" diameter 0.20 @{set2.xyz} @{set2.xyz + ")
              .append(Escape.eP(vt1)).append("*0.9} color red");
          drawSB.append(getDrawID("offsetFrameY"))
              .append(" diameter 0.20 @{set2.xyz} @{set2.xyz + ")
              .append(Escape.eP(vt2)).append("*0.9} color green");
          if (drawFrameZ)
            drawSB.append(getDrawID("offsetFrameZ"))
              .append(" diameter 0.20 @{set2.xyz} @{set2.xyz + ")
              .append(Escape.eP(vt3)).append("*0.9} color purple");
        }
        drawSB.append("\n}\n");
      } // end !isSpaceGroup
      cmds = drawSB.toString();
      if (Logger.debugging)
        Logger.info(cmds);
      drawSB = null;
      break;
    }

    // finalize returns

    if (trans == null)
      ftrans = null;
    if (isScrew) {
      // screw
      trans = V3d.newV(ax1);
      ptemp.setT(trans);
      uc.toFractional(ptemp, false);
      ftrans = V3d.newV(ptemp);
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
        xyzNew = op.fixMagneticXYZ(m2, xyzNew);
      else if (nDim == 2)
        xyzNew = xyzNew.replace(",z", "");
    }

    Object[] ret = new Object[RET_COUNT];
    for (int i = bsInfo.nextSetBit(0); i >= 0; i = bsInfo.nextSetBit(i + 1)) {
      switch (i) {
      case RET_XYZ:
        ret[i] = xyzNew;
        break;
      case RET_XYZNORMALIZED:
        if (ptFrom != null && ptTarget == null && !op.isBio && op.modDim == 0) {
          String xyzN;
          pta02.setT(ptFrom);
          uc.toFractional(pta02, true);
          m2.rotTrans(pta02);
          ptemp.setT(pta02);
          uc.unitize(pta02);
          vtrans.sub2(pta02, ptemp);
          m2 = M4d.newM4(op);
          m2.add(vtrans);
          xyzN = SymmetryOperation.getXYZFromMatrix(m2, false, false, false);
          if (isMagnetic)
            xyzN = op.fixMagneticXYZ(m2, xyzN);
          ret[i] = xyzN;
        }
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
        ret[i] = approx0(
            pa1 != null && bsInfo.get(RET_INVARIANT) ? pta00 : pa1);
        break;
      case RET_AXISVECTOR:
        ret[i] = (plane == null ? approx0(ax1) : null);
        break;
      case RET_ROTANGLE:
        ret[i] = (ang1 != 0 ? Integer.valueOf(ang1) : null);
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
          double d = MeasureD.distanceToPlane(plane, pta00);
          plane.w -= d;          
        }
        ret[i] = plane;
        break;
      case RET_SPIN:
        ret[i] = op.spinU;
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
            M4d orig = SymmetryOperation.getMatrixFromXYZ(op.xyzOriginal, null,
                false);
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
        // removes the ",z" from plane and frieze
        String s = op.xyzCanonical;
        if (s == null)
          s = op.xyz;
        if (nDim == 2)
          s = s.replace(",z", "");
        ret[i] = s;
        break;
      }
    }

    return ret;
  }

  private static void fixGlideTrans(V3d ftrans) {
    // set all +3/4 to -1/4
    ftrans.x = fixGlideX(ftrans.x);
    ftrans.y = fixGlideX(ftrans.y);
    ftrans.z = fixGlideX(ftrans.z);
  }
  
  private static double fixGlideX(double x) {
    int n48 = (int) Math.round(x * 48.001);
    switch (n48) {
    case 36:
      return -1/4d;
    case -36:
      return 1/4d;
    default:
      return x;//n48/48d;
    }
   }

  private void scaleByOrder(V3d v, int order, Boolean isccw) {
    v.scale(1 + (0.3d/order) + (isccw == null ? 0 : isccw  == Boolean.TRUE ? 0.02d : -0.02d));
  }

  private static boolean checkHandedness(SymmetryInterface uc, P3d ax1) {
    double a, b, c;
    ptemp.set(1, 0, 0);
    uc.toCartesian(ptemp, false);
    a = approx0d(ptemp.dot(ax1));
    ptemp.set(0, 1, 0);
    uc.toCartesian(ptemp, false);
    b = approx0d(ptemp.dot(ax1));
    ptemp.set(0, 0, 1);
    uc.toCartesian(ptemp, false);
    c = approx0d(ptemp.dot(ax1));
    return (a == 0 ? (b == 0 ? c > 0 : b > 0)
        : c == 0 ? a > 0 : (b == 0 ? c > 0 : a * b * c > 0));
  }

  private void drawLine(SB s, String id, double diameter, P3d pt0,
                               P3d pt1, String color) {
    s.append(getDrawID(id)).append(" diameter ").appendD(diameter).append(Escape.eP(pt0))
        .append(Escape.eP(pt1)).append(" color ").append(color);
  }

  private void drawFrameLine(String xyz, P3d pt, V3d v, double width,
                                    P3d ptemp, SB sb, String key,
                                    String color) {
    ptemp.setT(pt);
    ptemp.add(v);
    drawLine(sb, key + "Pt" + xyz, width, pt, ptemp, "translucent " + color);
  }

  private void drawVector(SB sb, String label,
                                 String type, String d, T3d pt1, T3d v,
                                 String color, String title) {
    if (type.equals("vline")) {
      ptemp2.add2(pt1, v);
      type = "";
      v = ptemp2;
    }
    d += " ";
    sb.append(getDrawID(label)).append(" diameter ").append(d)
        .append(type).append(Escape.eP(pt1)).append(Escape.eP(v))
        .append(" color ").append(color);
    if (title != null)
      sb.append(" \"" + title + "\"");
  }

  @SuppressWarnings("unchecked")
  private void drawOrderVector(SB sb, String label, String type, String d,
                               P3d pt, int order, int screwDir,
                               boolean haveInversion, boolean isCCW, V3d vtemp,
                               String color, String title, boolean isSpaceGroupAll) {
    drawVector(sb, label, type, d, pt, vtemp, color, title);
    if (order == 2 || haveInversion && !isCCW)
      return;
    Object[] poly = getPolygon(order, !haveInversion ? 0 : isCCW ? 1 : -1, haveInversion, pt, vtemp);
    Lst<Object> l = (Lst<Object>) poly[0];
    sb.append(getDrawID(label + "_key")).append(" POLYGON ").appendI(l.size());
    for (int i = 0, n = l.size(); i < n; i++)
      sb.appendO(l.get(i));
    sb.append(" color ").append(color);
    if (screwDir != 0 && isSpaceGroupAll) {
      // add screw axis "windmill"
      poly = getPolygon(order, screwDir, haveInversion, pt, vtemp);
      sb.append(getDrawID(label + "_key2"));
      l = (Lst<Object>) poly[0];
      sb.append(" POLYGON ").appendI(l.size());
      for (int i = 0, n = l.size(); i < n; i++)
        sb.appendO(l.get(i));
      l = (Lst<Object>) poly[1];
      sb.appendI(l.size());
      for (int i = 0, n = l.size(); i < n; i++)
        sb.appendO(PT.toJSON(null, l.get(i)));
      sb.append(" color ").append(color);
    }
  }
  
  private static Object[] getPolygon(int order, int screwDir, boolean haveInversion, P3d pt0, V3d v) {
    double scale = (haveInversion ? 0.6d : 0.4d);
    Lst<P3d> pts = new Lst<>();
    Lst<int[]> faces = new Lst<>();
    V3d offset = V3d.newV(v);
    offset.scale(0);
    offset.add(pt0);

    V3d vZ = V3d.new3(0, 0, 1);
    V3d vperp = new V3d();
    M3d m = new M3d();
    vperp.cross(vZ, v);
    if (vperp.length() < 0.01d) {
      m.m00 = m.m11 = m.m22 = 1;
    } else {
      vperp.normalize();
      double a = vZ.angle(v);
      m.setAA(A4d.newVA(vperp, a));
    }
    double rad = Math.PI * 2 / order * (screwDir < 0 ? -1 : 1);
    V3d vt = new V3d();
    P3d ptLast = null;
    for (int plast = 0, p = 0, i = 0, n = (screwDir == 0 ? order : order + 1); i < n; i++) {
      P3d pt = new P3d();
      pt.x = Math.cos(rad * i) * scale;
      pt.y = Math.sin(rad * i) * scale;
      m.rotate(pt);
      pt.add(offset);
      if (i < order) {
        pts.addLast(pt);
      }
      if (!haveInversion && screwDir != 0 && (i % screwDir == 0) && ptLast != null) {
        // draw wings
        vt.sub2(pt, ptLast);
        int p2 = (i < order ? p++ : 0);
        P3d pt1 = P3d.newP(pt);
        pt1.scaleAdd2(1, pt, pt1);
        pt1.scaleAdd2(-1, offset, pt1);
        pts.addLast(pt1);
        faces.addLast(new int[] {plast, p++, p2, 0});
        plast = p2;
      } else {
        plast = p++;
      }
      ptLast = pt;
    }
    return new Object[] { pts, faces };
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

  private static String strCoord(T3d p, boolean isBio) {
    approx0(p);
    return (isBio ? "(" + p.x + " " + p.y + " " + p.z + ")"
        : SymmetryOperation.fcoord(p, " "));
  }

  private static T3d approx0(T3d pt) {
    if (pt != null) {
      pt.x = approx0d(pt.x);
      pt.y = approx0d(pt.y);
      pt.z = approx0d(pt.z);
    }
    return pt;
  }

  private static double approx0d(double x) {
    return (Math.abs(x) < 0.0001 ? 0 : x);
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
   * @param iModel
   * @param iatom
   * @param uc
   * @param xyz
   * @param op
   * @param translation
   *        [i j k] to be added to operator
   * @param pt
   * @param pt2
   *        second point or offset
   * @param id
   * @param type
   * @param scaleFactor
   * @param nth
   *        -2 here means ALL (additional) space group operations
   * @param options
   * @param isSpaceGroup
   *        true only for DRAW Spacegroup -- don't do all the targeting
   * @return a string or an Object[] containing information
   */
  private Object getSymmetryInfo(int iModel, int iatom, SymmetryInterface uc,
                                 String xyz, int op, P3d translation, P3d pt,
                                 P3d pt2, String id, int type,
                                 double scaleFactor, int nth, int options,
                                 boolean isSpaceGroup) {
    int returnType = 0;
    Object nullRet = nullReturn(type);
    int bsMore = 0;
    switch (type) {
    case T.lattice:
      return "" + uc.getLatticeType();
    case T.list:
      returnType = T.list;
      break;
    case T.draw:
      returnType = T.draw;
      break;
    case T.rxyz:
      returnType = getKeyType("matrix");
      bsMore = RET_RXYZ;
      break;
    case T.array:
      returnType = getType(id);
      switch (returnType) {
      case T.rxyz:
        returnType = getKeyType("matrix");
        bsMore = RET_RXYZ;
        break;
      case T.spin:
      case T.atoms:
      case T.full:
      case T.list:
      case T.point:
      case T.element:
      case T.var:
      case T.x:
      case T.origin:
        type = returnType;
        break;
      default:
        returnType = getKeyType(id);
        break;
      }
      break;
    }
    BS bsInfo = getInfoBS(returnType);
    if (bsMore > RET_COUNT)
      bsInfo.set(bsMore);
    boolean isSpaceGroupAll = (nth == -2);
    int iop = op, iop0 = op;
    P3d offset = (options == T.offset && (type == T.atoms || type == T.point)
        ? pt2
        : null);
    if (offset != null)
      pt2 = null;
    Object[] info = null;
    String xyzOriginal = null;
    SymmetryOperation[] ops = null;
    int nDim = (uc == null ? 3 : uc.getDimensionality());

    if (pt2 == null) {
      if (xyz == null) {
        // it's the additional operators that get us the coincidence business
        ops = (SymmetryOperation[]) (isSpaceGroupAll
            ? uc.getAdditionalOperations()
            : uc.getSymmetryOperations());
        if (ops == null || Math.abs(op) > ops.length)
          return nullRet;
        if (op == 0)
          return nullRet;
        iop = Math.abs(op) - 1;
        xyz = (translation == null ? ops[iop].xyz
            : ops[iop].getxyzTrans(translation));
        xyzOriginal = ops[iop].xyzOriginal;
      } else {
        iop = op = 0;
      }
      SymmetryInterface symTemp = new Symmetry(); // no vwr object here
      symTemp.setSpaceGroup(false);
      boolean isBio = (uc != null && uc.isBio());
      int i = (isBio
          ? symTemp.addBioMoleculeOperation(
              ((SpaceGroup) uc.getSpaceGroup()).finalOperations[iop], op < 0)
          : symTemp.addSpaceGroupOperation((op < 0 ? "!" : "=") + (xyz.indexOf("x") >= 0 && xyz.indexOf("z") < 0 ? xyz + ",z" : xyz),
              Math.abs(op)));

      if (i < 0)
        return nullRet;
      SymmetryOperation opTemp = (SymmetryOperation) symTemp
          .getSpaceGroupOperation(i);
      if (isSpaceGroup) {
        opTemp.iCoincident = ops[iop].iCoincident;
      }
      if (isSpaceGroupAll) {
        opTemp.isIrrelevant = ops[iop].isIrrelevant;
      }
      if (xyzOriginal != null) {
        opTemp.xyzOriginal = xyzOriginal;
        opTemp.timeReversal = ops[iop].timeReversal;
        opTemp.spinU = ops[iop].spinU;
      }
      opTemp.number = (op == 0 ? iop0 : op);
      if (!isBio)
        opTemp.getCentering();
      if (pt == null && iatom >= 0)
        pt = modelSet.at[iatom];
      if (type == T.point || type == T.atoms) {
        if (isBio || pt == null)
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
          sympt.add(offset);
        }
        symTemp.toCartesian(sympt, false);
        P3d ret = sympt;
        return (type == T.atoms ? getAtom(uc, iModel, iatom, ret) : ret);
      }
      info = createInfoArray(opTemp, uc, pt, null, (id == null || id.equals("array") ? JC.DEFAULT_DRAW_SYM_ID : id),
          scaleFactor, options, (translation != null), bsInfo, isSpaceGroup, isSpaceGroupAll, nDim);
      if (type == T.array && id != null && returnType != -1 - RET_MATRIX) {
        returnType = getKeyType(id);
      }
    } else {
      // pt1, pt2
      String stype = "info";
      boolean asString = false;
      switch (type) {
      case T.array: // new Jmol 14.29.45
        if (returnType != -1 - RET_MATRIX)
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
        bsInfo.set(RET_XYZ);
        bsInfo.set(RET_XYZNORMALIZED);
        break;
      case T.draw:
        if (id == null)
          id = (isSpaceGroup ? "sg" : JC.DEFAULT_DRAW_SYM_ID);
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
      Object ret1 = getSymopInfoForPoints(uc, iModel, op, translation, pt, pt2,
          id, stype, scaleFactor, nth, options, bsInfo);
      if (asString) {
        return ret1;
      }
      if (ret1 instanceof String)
        return nullRet; // two atoms are not connected, no such oper
      info = (Object[]) ret1;
      if (type == T.atoms) {
        if (!(pt instanceof Atom) && !(pt2 instanceof Atom))
          iatom = -1;
        return (info == null ? nullRet
            : getAtom(uc, iModel, iatom, (T3d) info[7]));
      }
    }
    if (info == null)
      return nullRet;
    boolean isList = (info.length > 0 && info[0] instanceof Object[]);
    if (nth < 0 && op <= 0 && xyz == null && (type == T.array || isList)) {
      if (type == T.array && info.length > 0 && !(info[0] instanceof Object[]))
        info = new Object[] { info };
      Lst<Object> lst = new Lst<Object>();
      for (int i = 0; i < info.length; i++)
        lst.addLast(
            getInfo((Object[]) info[i], returnType < 0 ? returnType : type, nDim));
      return lst;
    } else if (returnType < 0 && (nth >= 0 || op > 0 || xyz != null)) {
      type = returnType;
    }
    if (nth > 0 && isList)
      info = (Object[]) info[0];
    if (type == T.draw && isSpaceGroup && nth == -2)
      type = T.spacegroup;
    return getInfo(info, type, nDim);
  }

  private BS getAtom(SymmetryInterface uc, int iModel, int iAtom, T3d sympt) {
    BS bsElement = null;
    if (iAtom >= 0)
      modelSet.getAtomBitsMDa(T.elemno,
          Integer.valueOf(modelSet.at[iAtom].getElementNumber()),
          bsElement = new BS());
    BS bsResult = new BS();
    modelSet.getAtomsWithin(0.02d, sympt, bsResult, iModel);
    if (bsElement != null)
      bsResult.and(bsElement);
    if (bsResult.isEmpty()) {
      sympt = P3d.newP(sympt);
      uc.toUnitCell(sympt, null);
      uc.toCartesian(sympt, false);
      modelSet.getAtomsWithin(0.02d, sympt, bsResult, iModel);
      if (bsElement != null)
        bsResult.and(bsElement);
    }
    return bsResult;
  }

}
