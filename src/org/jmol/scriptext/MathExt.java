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

import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jmol.api.Interface;
import org.jmol.api.JmolDataManager;
import org.jmol.api.JmolNMRInterface;
import org.jmol.api.JmolPatternMatcher;
import org.jmol.api.SymmetryInterface;
import org.jmol.atomdata.RadiusData;
import org.jmol.atomdata.RadiusData.EnumType;
import org.jmol.bspt.PointIterator;
import org.jmol.c.VDW;
import org.jmol.i18n.GT;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.BondSet;
import org.jmol.modelset.Measurement;
import org.jmol.modelset.ModelSet;
import org.jmol.script.SV;
import org.jmol.script.ScriptEval;
import org.jmol.script.ScriptException;
import org.jmol.script.ScriptMathProcessor;
import org.jmol.script.ScriptParam;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.BoxInfo;
import org.jmol.util.ColorEncoder;
import org.jmol.util.Edge;
import org.jmol.util.Escape;
import org.jmol.util.JmolMolecule;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.util.Point3fi;
import org.jmol.util.SimpleUnitCell;
import org.jmol.util.Tensor;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.util.A4d;
import javajs.util.AU;
import javajs.util.BArray;
import javajs.util.BS;
import javajs.util.CU;
import javajs.util.Lst;
import javajs.util.M3d;
import javajs.util.M4d;
import javajs.util.MeasureD;
import javajs.util.OC;
import javajs.util.P3d;
import javajs.util.P4d;
import javajs.util.PT;
import javajs.util.Qd;
import javajs.util.Rdr;
import javajs.util.SB;
import javajs.util.T3d;
import javajs.util.V3d;

public class MathExt {

  private static final Double nan = Double.valueOf(Double.NaN);
  private Viewer vwr;
  private ScriptEval e;

  public MathExt() {
    // used by Reflection
  }

  public MathExt init(Object se) {
    e = (ScriptEval) se;
    vwr = e.vwr;
    return this;
  }

  ///////////// ScriptMathProcessor extensions ///////////
  private static long t0 = System.currentTimeMillis();

  public boolean evaluate(ScriptMathProcessor mp, T op, SV[] args, int tok)
      throws ScriptException {
    switch (tok) {
    case T.now:
      return (args.length >= 1 && args[0].tok == T.string
          ? mp.addXStr((args.length == 1 ? new Date().toString()
              : vwr.apiPlatform.getDateFormat(SV.sValue(args[1]))) + "\t"
              + SV.sValue(args[0]).trim())
          : mp.addXInt(((int) (System.currentTimeMillis() - t0))
              - (args.length == 0 ? 0 : args[0].asInt())));
    case T.abs:
      return (args.length == 1 && args[0].tok == T.integer
          ? mp.addXInt(Math.abs(args[0].intValue))
          : mp.addXDouble(Math.abs(args[0].asDouble())));
    case T.acos:
    case T.cos:
    case T.sin:
    case T.sqrt:
    case T.tan:
      return (args.length == 1 && evaluateMath(mp, args, tok));
    case T.add:
    case T.div:
    case T.mul:
    case T.mul3:
    case T.sub:
    case T.push:
    case T.pop:
      return evaluateList(mp, op.intValue, args);
    case T.leftsquare:
      if (args.length == 0)
        mp.wasX = false;
      //$FALL-THROUGH$
    case T.array:
      return evaluateArray(mp, args,
          tok == T.array && op.tok == T.propselector);
    case T.matrix:
      return evaluateMatrix(mp, args);
    case T._args:
      return evaluateCallbackParam(mp, args);
    case T.axisangle:
    case T.quaternion:
      return evaluateQuaternion(mp, args, tok);
    case T.bin:
      return evaluateBin(mp, args);
    case T.cache:
      return evaluateCache(mp, args);
    case T.col:
    case T.row:
      return evaluateRowCol(mp, args, tok);
    case T.color:
      return evaluateColor(mp, args);
    case T.compare:
      return evaluateCompare(mp, args);
    case T.bondcount:
    case T.connected:
    case T.polyhedra:
      return evaluateConnected(mp, args, tok, op.intValue);
    case T.boundbox:
    case T.unitcell:
      return evaluateUnitCell(mp, args, op.tok == T.propselector, op.tok == T.propselector ? op.intValue : op.tok);
    case T.contact:
      return evaluateContact(mp, args);
    case T.data:
      return evaluateData(mp, args);
    case T.dot:
    case T.cross:
      return evaluateDotDist(mp, args, tok, op.intValue);
    case T.distance:
      if (op.tok == T.propselector)
        return evaluateDotDist(mp, args, tok, op.intValue);
      //$FALL-THROUGH$
    case T.angle:
    case T.measure:
      return evaluateMeasure(mp, args, op.tok);
    case T.file:
    case T.load:
      return evaluateLoad(mp, args, tok == T.file);
    case T.find:
      return evaluateFind(mp, args);
    case T.inchi:
      return evaluateInChI(mp, args);
    case T.format:
    case T.label:
      return evaluateFormat(mp, op.intValue, args, tok == T.label);
    case T.function:
      return evaluateUserFunction(mp, (String) op.value, args, op.intValue,
          op.tok == T.propselector);
    case T.pivot2:
      tok = T.pivot;
      //$FALL-THROUGH$
    case T.__:
    case T.pivot:
    case T.select:
    case T.getproperty:
      return evaluateGetProperty(mp, args, tok, op.tok == T.propselector);
    case T.helix:
      return evaluateHelix(mp, args);
    case T.hkl:
    case T.plane:
    case T.intersection:
      return evaluatePlane(mp, args, tok, op.tok == T.propselector);
    case T.eval:
    case T.javascript:
    case T.script:
    case T.show:
      return evaluateScript(mp, args, tok);
    case T.join:
    case T.split:
    case T.trim:
      return evaluateString(mp, op.intValue, args);
    case T.point:
      return evaluatePoint(mp, args);
    case T.pointgroup:
      return evaluatePointGroup(mp, args, op.tok == T.propselector);
    case T.prompt:
      return evaluatePrompt(mp, args);
    case T.random:
      return evaluateRandom(mp, args);
    case T.in:
      return evaluateIn(mp, args);
    case T.modulation:
      return evaluateModulation(mp, args);
    case T.replace:
      return evaluateReplace(mp, args);
    case T.pattern:
    case T.search:
    case T.smiles:
    case T.substructure:
      return evaluateSubstructure(mp, args, tok, op.tok == T.propselector);
    case T.sort:
    case T.count:
      return evaluateSort(mp, args, tok);
    case T.spacegroup:
      return evaluateSpacegroup(mp, args);
    case T.symop:
      return evaluateSymop(mp, args, op.tok == T.propselector);
    //    case Token.volume:
    //    return evaluateVolume(args);
    case T.tensor:
      return evaluateTensor(mp, args);
    case T.within:
      return evaluateWithin(mp, args, op.tok == T.propselector);
    case T.write:
      return evaluateWrite(mp, args);
    }
    return false;
  }

  private boolean evaluateMatrix(ScriptMathProcessor mp, SV[] args) {
    // matrix("x-y+1/2,x+y,z")
    // matrix("a+b,b-a,c;1/2,0,0")
    // matrix([.........])
    // matrix([.................])
    // matrix([,,][,,][,,])
    // matrix([,,,][,,,][,,,][,,,])
    // matrix([[,,][,,][,,]])
    // matrix([[,,,][,,,][,,,][,,,]])
    // matrix([[,,][,,][,,]])
    // matrix({rx,ry,rz},degrees)
    // any of 4x4 with "abc" or "xyz" or "uvw"
    // matrix("!b,c,a>a-c,b,2c;0,0,1/2>a,-a-c,b")
    // matrix("13>>15>>14>>2")
    // matrix("13>sub(...params..)>2", {})
    // matrix("13>...>2", [])

    // matrix("h")
    // matrix("r")

    int n = args.length;
    M4d m4 = null;
    M3d m3 = null;
    String sarg0 = (n > 0 && args[0].tok == T.string ? (String) args[0].value
        : null);
    Map<String, SV> map = (n < 2 || sarg0 == null ? null : args[1].getMap());
    Lst<SV> lst = (n < 2 || sarg0 == null ? null : args[1].getList());
    String retType = (n > 1 && args[n - 1].tok == T.string
        ? (String) args[n - 1].value
        : null);
    boolean asUVW = "uvw".equalsIgnoreCase(retType);
    boolean asABC = "abc".equalsIgnoreCase(retType);
    boolean asXYZ = asUVW || !asABC && "xyz".equalsIgnoreCase(retType);
    boolean asRXYZ = !asUVW && !asABC && !asXYZ
        && "rxyz".equalsIgnoreCase(retType);
    double[] a = null;
    if (retType != null || lst != null || map != null)
      n--;
    switch (n) {
    case 0:
      m4 = new M4d();
      m4.setIdentity();
      break;
    case 1:
      switch (args[0].tok) {
      case T.matrix4f:
        m4 = (M4d) args[0].value;
        break;
      case T.string:
        String s = (String) args[0].value;
        // first check for "t1>t2>t3"
        if (s.equals("h") || s.equals("r")
            || s.indexOf(",") >= 0 && s.indexOf(":") < 0) {
          SymmetryInterface sym;
          if (s.indexOf('*') >= 0) {
            sym = vwr.getCurrentUnitCell();
            if (sym == null)
              return false;
          } else {
            sym = vwr.getSymTemp();
          }
          m4 = (M4d) sym.convertTransform(s, null);
        }
        if (m4 != null)
          break;
        String select = null;
        if (retType != null && !asABC && !asXYZ && !asUVW) {
          if ("map".equals(retType)) {
            map = new Hashtable<String, SV>();
            retType = null;
          } else if ("list".equals(retType)) {
            lst = new Lst<SV>();
            retType = null;
          } else {
            select = "[SELECT (" + retType + ")]";
            lst = new Lst<SV>();
            retType = null;
          }
        }
        boolean returnM4 = (lst == null && map == null);
        Hashtable<String, Object> retMap = (map == null ? null
            : new Hashtable<>());
        Lst<Object> retLst = (lst == null ? null : new Lst<Object>());
        if (returnM4) {
          retMap = new Hashtable<>();
          retMap.put("ASM4", Boolean.TRUE);
        }
        m4 = vwr.getSymStatic().staticGetMatrixTransform(s,
            retMap == null ? retLst : retMap);
        if (returnM4)
          break;
        if (map != null) {
          map.clear();
          for (Entry<String, Object> e : retMap.entrySet()) {
            map.put(e.getKey(), SV.getVariable(e.getValue()));
          }
          return mp.addXMap(map);
        }
        if (lst != null) {
          if (select != null)
            return mp.addXObj(vwr.extractProperty(retLst, select, -1));
          lst.clear();
          for (int i = 0, nl = retLst.size(); i < nl; i++) {
            lst.addLast(SV.getVariable(retLst.get(i)));
          }
          return mp.addXList(lst);
        }
        break;
      case T.varray:
        lst = args[0].getList();
        int len = lst.size();
        switch (len) {
        case 3:
        case 4:
          a = new double[len * len];
          for (int i = 0, pt = 0; i < len; i++) {
            SV row = lst.get(i);
            switch (row.tok) {
            case T.point3f:
              P3d p = (P3d) row.value;
              a[pt++] = p.x;
              a[pt++] = p.y;
              a[pt++] = p.z;
              break;
            case T.point4f:
              P4d p4 = (P4d) row.value;
              a[pt++] = p4.x;
              a[pt++] = p4.y;
              a[pt++] = p4.z;
              a[pt++] = p4.w;
              break;
            case T.varray:
              Lst<SV> a2 = lst.get(i).getList();
              if (a2 == null || a2.size() != len)
                return false;
              for (int j = 0; j < len; j++) {
                a[pt++] = SV.dValue(a2.get(j));
              }
              break;
            }
          }
          break;
        case 9:
        case 16:
          a = SV.dlistValue(args[0], 0);
          break;
        default:
          return false;
        }
        break;
      }
      break;
    case 2:
      m3 = new M3d();
      if (args[0].tok != T.point3f)
        return false;
      V3d v = V3d.newV(SV.ptValue(args[0]));
      double angle = SV.dValue(args[1]);
      m3.setAA(A4d.newVA(v, angle / 180 * Math.PI));
      return mp.addXM3(m3);
    case 3:
    case 4:
      if (args[0].tok == T.varray) {
        a = new double[n == 3 ? 9 : 16];
        for (int p = 0, i = 0; i < n; i++) {
          double[] row = SV.dlistValue(args[i], 0);
          for (int j = 0; j < n; j++)
            a[p++] = row[j];
        }
        break;
      }
    }
    if (a != null) {
      switch (a.length) {
      case 9:
        m3 = M3d.newA9(a);
        break;
      case 16:
        m4 = M4d.newA16(a);
        break;
      default:
        return false;
      }
    }
    if (asRXYZ || asABC || asUVW || asXYZ) {
      if (m3 != null) {
        m4 = M4d.newMV(m3,  new P3d());
      }
      return mp.addXStr(matToString(m4,
          asRXYZ ? 0x1 : asABC ? 0xABC : asUVW ? 0xDEF : 0));
    }
    return (m3 != null ? mp.addXM3(m3) : m4 != null ? mp.addXM4(m4) : false);
  }

  private String matToString(M4d m4, int mode) {
    SymmetryInterface sym = vwr.getSymStatic();
    switch (mode) {
    case 0x1:
      return (String) sym.staticConvertOperation(null, m4, "rxyz");
    case SV.FORMAT_ABC:
    case 0xABC:
      return sym.staticGetTransformABC(m4, false);
    default:
    case SV.FORMAT_UVW:
    case 0xDEF:
    case SV.FORMAT_XYZ:
      return (String) vwr.getSymStatic().staticConvertOperation("", m4, (mode == SV.FORMAT_UVW || mode == 0xDEF ? "uvw" : "xyz"));
    }
  }

  /**
   * Process the _args() or _args(n) request.
   * 
   * @param mp
   * @param args
   * @return true
   */
  private boolean evaluateCallbackParam(ScriptMathProcessor mp, SV[] args) {
    return mp.addX(e.getCallbackParameter(args.length == 0 ? Integer.MIN_VALUE : args[0].asInt()));
  }

  private boolean evaluateSpacegroup(ScriptMathProcessor mp, SV[] args) {
    // spacegroup();
    // spacegroup("list")
    // spacegroup(10, "list")
    // spacegroup("setting")
    // spacegroup(3);
    // spacegroup("133:2") // this name
    // spacegroup("Hall:p 32 2\" (0 0 4)") // XYZ list
    // spacegroup("ITA/155") // all settings for this ITA
    // spacegroup("ITA/155:c,b,a") // setting for this ITA by transform
    // spacegroup("ITA/14.3") // third ITA entry for this ITA
    // spacegroup("ITA/all")  // all, as map
    // spacegroup("x,y,z;-x,-y,-z");
    // spacegroup("x,y,z;-x,y,z", [a, b, c, alpha, beta, gamma])
    // spacegroup(6, [a, b, c, alpha, beta, gamma])
    // spacegroup("x,y,z;-x,-y,-z&"); //space groups with all these operators
    // spacegroup(18, "settings")
    // spacegroup(4,"subgroups") all data for 4
    // spacegroup(4,0,"subgroups") an array of known subgroups
    // spacegroup(4,0,0,0,"subgroups") an array of arrays of [isub, ntrm, subIndex, type(t=1, k(eu)=3, k(ct)= 4]
    // spacegroup(4,5,"subgroups") list of all 4 >> 5
    // spacegroup(4,5,1,"subgroups") first-listed 4 >> 5 as a map
    // spacegroup(4,0,2,"subgroups") second listed subgroup as a map
    // spacegroup(4,0,2,1,"subgroups") second listed subgroup, first transformation
    // spacegroup(4,5,1,2,"subgroups") second transformation for 4>>5, first match
    // the first and second number can be replaced by a Hermann-Mauguin name.
    // "subgroups" is optional if there are more than two parameter.

     // spacegroup("all");
    double[] ucParams = null;
    int nargs = args.length;
    if (nargs == 0)
      return mp.addXObj(vwr.getSymTemp().getSpaceGroupInfo(vwr.ms, null,
          vwr.am.cmi, true, null));
    String mode = (args[args.length - 1].tok == T.string
        ? (String) args[args.length - 1].value
        : null);
    boolean isSettings = "settings".equalsIgnoreCase(mode);
    boolean isSetting = "setting".equalsIgnoreCase(mode);
    boolean isSubgroups = (nargs > 1 && (nargs != 2
        || !isSettings && !isSetting && mode != null 
        && !"list".equalsIgnoreCase(mode)
        && !"jmol".equalsIgnoreCase(mode)
        ));
    String xyzList = args[0].asString();
    if (isSubgroups || "subgroups".equals(mode)) {
      Object ret = getSubgroupInfo(args,
          "subgroups".equals(mode) ? nargs : nargs + 1);
      return (ret == null ? false : mp.addXObj(ret));
    }
    Object params = null;
    Object ret = null;
    switch (nargs) {
    default:
      return false;
    case 2:
      if ("list".equals(mode)) {
        params = Integer.valueOf((String) vwr.getSymTemp()
            .getSpaceGroupInfoObj("itaNumber", xyzList, false, false));
        xyzList = "list";
        break;
      }
      if (args[1].tok != T.string) {
        // spacegroup("x,y,z;-x,y,z", [a, b, c, alpha, beta, gamma])
        // spacegroup(6, [a, b, c, alpha, beta, gamma])
        ucParams = SV.dlistValue(args[1], 0);
        if (ucParams == null || ucParams.length != 6)
          return false;
        // set excess params to NaN; does not set slop
        params = ucParams = SimpleUnitCell.newParams(ucParams, Double.NaN);
      }
      //$FALL-THROUGH$
    case 1:
      if (args[0].tok == T.bitset) {
        BS atoms = SV.getBitSet(args[0], true);
        // undocumented first parameter atoms, so getting a space group for a subset of atoms
        return mp.addXObj(vwr.findSpaceGroup(null, atoms, null, ucParams, null,
            null, JC.SG_AS_STRING));
      }
      int itaNo = (args[0].tok == T.integer ? args[0].intValue
          : nargs == 1 ? Integer.MIN_VALUE : 0);
      if (isSettings || isSetting) {
        if (isSettings && itaNo == 0)
          return false;
        String data = (isSetting && nargs > 1 ? xyzList : null);
        return mp.addXObj((itaNo == Integer.MIN_VALUE ? vwr.getCurrentUnitCell()
            : vwr.getSymTemp()).getSpaceGroupJSON(mode.toLowerCase(), data, itaNo));
      }
      if ("setting".equalsIgnoreCase(xyzList)
          || "settings".equalsIgnoreCase(xyzList)) {
        SymmetryInterface sym = vwr.getOperativeSymmetry();
        return mp.addXObj(sym == null ? null
            : sym.getSpaceGroupJSON(xyzList.toLowerCase(), null, Integer.MIN_VALUE));
      }
      if (xyzList.toUpperCase().startsWith("AFLOWLIB/")) {
        // "15" or "15.1"
        return mp.addXObj(vwr.getSymTemp().getSpaceGroupJSON("AFLOWLIB", xyzList.substring(9),
            0));
      }
      if (xyzList.startsWith("Hall:") || xyzList.indexOf("x") >= 0
          || ucParams != null) {
        // spacegroup("x,y,z;-x,y,z", ...)
        // this returns a Jmol group, not an ITA group. 
        // it is used for the script getITA.spt to retrive Jmol-specific info.
        // possibly with parameters
        ret = vwr.findSpaceGroup(null, null, xyzList, ucParams,
            null, null, JC.SG_AS_STRING);
        break;
      }
      if (itaNo > 0 || args[0].tok == T.decimal || args[0].tok == T.string) {

        // spacegroup("132:2")     
        // spacegroup("p/3")
        // spacegroup("p/p3m1")

        if (itaNo > 0 || xyzList.length() > 1 && xyzList.charAt(1) == '/'
            || !xyzList.endsWith(":") && !Double.isNaN(PT.parseDouble(xyzList)))
          xyzList = "ITA/" + xyzList;
        if (xyzList.toUpperCase().startsWith("ITA/")) {
          // "230" or "155.2"; includes JmolID "15:ab"
          return mp.addXObj(vwr.getSymTemp().getSpaceGroupJSON("ITA", xyzList.substring(4),
              0));
        }
        // spacegroup("P 2") handled by finding Jmol space group 
        // and using the itaIndex for that
      }
      break;
    }
    if (ret == null)
      ret = vwr.getSymStatic().getSpaceGroupInfoObj(xyzList, params, true,
        false);
    if (ret instanceof Map<?,?> && !"jmol".equals(mode) && !"list".equals(mode)) {
      //System.out.println("MathExt " + ret);
      // "jmol" used will return the Jmol group if that was to be returned
      @SuppressWarnings("unchecked")
      Object o = ((Map<String, Object>) ret).get("itaIndex"); 
      if (o != null) {
        ret = vwr.getSymTemp().getSpaceGroupJSON("ITA", o.toString(), 0);
      }
    }
    return mp.addXObj(ret);
  }

  private Object getSubgroupInfo(SV[] args, int nargs) {
 
    //  nargs  0        1       2       3 
    //    * nameFrom  nameTo  index1  index2
    //    1  current  null    -       -      return map for current group, contents of sub_n.json
    //    2    n      null    -       -      return map for group n, contents of sub_n.json
    //    3    n1      n2    MinV     -      return list map.subgroups.select("WHERE subgroup=n2")
    //    3    n     0/""    MinV     -      return int[][] of critical information 
    //    4    n     0/""     m      MinV    return map map.subgroups[m]
    //    4    n1      n2     m      MinV    return map map.subgroups.select("WHERE subgroup=n2")[m]
    //    5    n     0/""     m       t      return string transform map.subgroups[m].trm[t]
    //    5    n     0/""     0       0      return int[] array of list of valid super>>sub 
    //    5    n1      n2     m       t      return string transform map.subgroups.select("WHERE subgroup=n2")[m].trm[t]
    //    * 

SymmetryInterface sym;
    
    int index1 = Integer.MIN_VALUE, index2 = Integer.MIN_VALUE;
    String nameFrom = null, nameTo = null;
    switch (nargs) {
    case 5:
      index2 = args[3].intValue;
      if (index2 < 0)
        return null;
      //$FALL-THROUGH$
    case 4:
      index1 = args[2].intValue;
      if (index1 < 0 || index1 == Integer.MAX_VALUE)
        return null;
      //$FALL-THROUGH$
    case 3:
      nameTo = (args[1].intValue == 0 ? "" : args[1].asString());
      //$FALL-THROUGH$
    case 2:
      nameFrom = args[0].asString();
      // nameTo will be null
      //$FALL-THROUGH$
    case 1:
    default:
      if (nameFrom == null) {
        sym = vwr.getOperativeSymmetry();
        if (sym == null)
          return null;
        nameFrom = sym.getSpaceGroupClegId();
      } else {
        sym = vwr.getSymStatic();
      }
      break;
    }
    return sym.getSubgroupJSON(nameFrom, nameTo, index1, index2, 0, null, null);
  }

  @SuppressWarnings("unchecked")
  private boolean evaluatePointGroup(ScriptMathProcessor mp, SV[] args,
                                     boolean isAtomProperty)
      throws ScriptException {
    // {*}.pointGroup();
    // @1.pointGroup("spacegroup")
    // pointGroup("spacegroup",@1)
    // pointGroup(points)
    // pointGroup(points, center)
    // pointGroup(points, center, distanceTolerance (def. 0.2 A), linearTolerance (def. 8 deg.)
    // center can be non-point to ignore, such as 0 or ""
    T3d[] pts = null;
    P3d center = null;
    double distanceTolerance = -1;
    double linearTolerance = -1;
    BS bsAtoms = null;
    boolean isSpaceGroup = false;
    switch (args.length) {
    case 4:
      linearTolerance = args[3].asDouble();
      //$FALL-THROUGH$
    case 3:
      distanceTolerance = args[2].asDouble();
      //$FALL-THROUGH$
    case 2:
      switch (args[1].tok) {
      case T.point3f:
        center = SV.ptValue(args[1]);
        break;
      case T.bitset:
        // pointgroup {vertices} {center}
        bsAtoms = SV.getBitSet(args[1], false);
        if (args[0].asString().equalsIgnoreCase("spaceGroup")) {
          isSpaceGroup = true;
          if (args.length == 2)
            distanceTolerance = 0; // will set tolerances especially tight
        }
        break;
      }
      if (isSpaceGroup)
        break;
      //$FALL-THROUGH$
    case 1:
      switch (args[0].tok) {
      case T.varray:
        Lst<SV> points = args[0].getList();
        pts = new T3d[points.size()];
        for (int i = pts.length; --i >= 0;)
          pts[i] = SV.ptValue(points.get(i));
        break;
      case T.bitset:
        bsAtoms = SV.getBitSet(args[0], false);
        pts = vwr.ms.at;
        break;
      case T.string:
        if (isAtomProperty) {
          bsAtoms = SV.getBitSet(mp.getX(), true);
          if (bsAtoms == null || bsAtoms.isEmpty())
            return false;
          String s = args[0].asString();
          if ("spacegroup".equals(s)) {
            isSpaceGroup = true;
            break;
          }
        }
        //$FALL-THROUGH$
      default:
        return false;
      }
      break;
    case 0:
      if (!isAtomProperty) {
        return mp.addXObj(vwr.ms.getPointGroupInfo(null));
      }
      bsAtoms = SV.getBitSet(mp.getX(), false);
      break;
    default:
      return false;
    }
    if (bsAtoms != null) {
      int iatom = bsAtoms.nextSetBit(0);
      if (iatom < 0 || iatom >= vwr.ms.ac
          || isSpaceGroup && bsAtoms.cardinality() != 1)
        return false;
      if (isSpaceGroup) {
        // @1.pointgroup("spacegroup")
        // pointgroup("spaceGroup", @1)
        Lst<P3d> lst = vwr.ms.generateCrystalClass(iatom,
            P3d.new3(Double.NaN, Double.NaN, Double.NaN));
        pts = new T3d[lst.size()];
        for (int i = pts.length; --i >= 0;)
          pts[i] = lst.get(i);
        center = new P3d();
      }
    }
    SymmetryInterface pointGroup = vwr.getSymTemp().setPointGroup(null, center,
        (pts == null ? vwr.ms.at : pts), bsAtoms, false,
        distanceTolerance < 0 ? vwr.getDouble(T.pointgroupdistancetolerance)
            : distanceTolerance,
        linearTolerance < 0 ? vwr.getDouble(T.pointgrouplineartolerance)
            : linearTolerance,
        (bsAtoms == null ? pts.length : bsAtoms.cardinality()), true);
    return mp.addXMap((Map<String, ?>) pointGroup.getPointGroupInfo(-1, null,
        true, null, 0, 1));
  }

  private boolean evaluateUnitCell(ScriptMathProcessor mp, SV[] args,
                                   boolean isSelector, int tok)
      throws ScriptException {
    // {xxx}.boundbox()
    // {xxx}.boundbox(volume)
    // optional last parameter: scale
    // unitcell(T1) => a,b,c:ta,tb,tc;
    // unitcell("-a,-b,c;0,1/2,1/2") 
    // unitcell("-a,-b,c;0,0,0.50482") (polar groups can have irrational translations along z)
    // unitcell("a,b,c",asMatrix) default false
    // unitcell(uc)
    // unitcell(uc, "reciprocal")
    // unitcell(origin, [va, vb, vc])
    // unitcell(origin, pta, ptb, ptc)

    // next can be without {1.1}, but then assume "all atoms"
    // {1.1}.unitcell()
    // {1.1}.unitcell(ucconv, "primitive","BCC"|"FCC")
    // {1.1}.unitcell(ucprim, "conventional","BCC"|"FCC")

    BS x1 = (isSelector ? SV.getBitSet(mp.getX(), true)
        : tok == T.boundbox ? vwr.getAllAtoms() : null);
    int iatom = ((x1 == null ? vwr.getAllAtoms() : x1).nextSetBit(0));
    int lastParam = args.length - 1;
    double scale = 1;
    switch (lastParam < 0 ? T.nada : args[lastParam].tok) {
    case T.integer:
    case T.decimal:
      scale = args[lastParam].asDouble();
      lastParam--;
      break;
    }
    boolean normalize = false;
    int tok0 = (lastParam < 0 ? T.nada : args[0].tok);
    T3d[] ucnew = null;
    Lst<SV> uc = null;
    String arg0 = null;
    switch (tok0) {
    case T.varray:
      uc = args[0].getList();
      break;
    case T.matrix4f:
      switch (lastParam > 1 ? T.error : lastParam < 1 ? T.nada : args[1].tok) {
      default:
      case T.error:
        return false;
      case T.nada:
      case T.off:
        break;
      case T.on:
        normalize = true;
        break;
      }
      return mp.addXStr(vwr.getSymStatic().staticGetTransformABC(args[0].value, normalize));
    case T.string:
      arg0 = args[0].asString();
      if (tok == T.unitcell) {
        if (arg0.indexOf("a=") == 0) {
          ucnew = new P3d[4];
          for (int i = 0; i < 4; i++)
            ucnew[i] = new P3d();
          SimpleUnitCell.setAbc(arg0, null, ucnew);
        } else if (arg0.indexOf(",") >= 0 || arg0.equals("r")) {
          boolean asMatrix = (args.length == 2 && SV.bValue(args[1]));
          Object ret;
          if (asMatrix) {
            ret = vwr.getSymTemp().convertTransform(arg0, null);
          } else {
            ret = vwr.getV0abc(-1, arg0);
          }
          return mp.addXObj(ret);
        }
      }
      break;
    }
    if (tok == T.boundbox) {
      // arg0: "center", "volume", "info" or null
      BoxInfo b = vwr.ms.getBoxInfo(x1, 1);
      return mp.addXObj(b.getInfo(arg0));
    }
    SymmetryInterface u = null;
    boolean haveUC = (uc != null);
    if (ucnew == null && haveUC && uc.size() < 4)
      return false;
    int ptParam = (haveUC ? 1 : 0);
    if (ucnew == null && !haveUC && tok0 != T.point3f) {
      // unitcell() or {1.1}.unitcell
      u = (iatom < 0 ? vwr.getCurrentUnitCell()
          : vwr.ms.getUnitCell(vwr.ms.at[iatom].mi));
      ucnew = (u == null
          ? new P3d[] { P3d.new3(0, 0, 0), P3d.new3(1, 0, 0), P3d.new3(0, 1, 0),
              P3d.new3(0, 0, 1) }
          : u.getUnitCellVectors());
    }
    if (ucnew == null) {
      ucnew = new P3d[4];
      if (haveUC) {
        switch (uc.size()) {
        case 3:
          // [va. vb. vc]
          ucnew[0] = new P3d();
          for (int i = 0; i < 3; i++)
            ucnew[i + 1] = P3d.newP(SV.ptValue(uc.get(i)));
          break;
        case 4:
          for (int i = 0; i < 4; i++)
            ucnew[i] = P3d.newP(SV.ptValue(uc.get(i)));
          break;
        case 6:
          // unitcell([a b c alpha beta gamma])
          double[] params = new double[6];
          for (int i = 0; i < 6; i++)
            params[i] = uc.get(i).asDouble();
          SimpleUnitCell.setAbc(null, params, ucnew);
          break;
        default:
          return false;
        }
      } else {
        ucnew[0] = P3d.newP(SV.ptValue(args[0]));
        switch (lastParam) {
        case 3:
          // unitcell(origin, pa, pb, pc)
          for (int i = 1; i < 4; i++)
            (ucnew[i] = P3d.newP(SV.ptValue(args[i]))).sub(ucnew[0]);
          break;
        case 1:
          // unitcell(origin, [va, vb, vc])
          Lst<SV> l = args[1].getList();
          if (l != null && l.size() == 3) {
            for (int i = 0; i < 3; i++)
              ucnew[i + 1] = P3d.newP(SV.ptValue(l.get(i)));
            break;
          }
          //$FALL-THROUGH$
        default:
          return false;
        }
      }
    }

    String op = (ptParam <= lastParam ? args[ptParam].asString() : null);

    boolean toPrimitive = "primitive".equalsIgnoreCase(op);
    if (toPrimitive || "conventional".equalsIgnoreCase(op)) {
      String stype = (++ptParam > lastParam ? ""
          : args[ptParam].asString().toUpperCase());
      if (stype.equals("BCC"))
        stype = "I";
      else if (stype.length() == 0)
        stype = (String) vwr.getSymmetryInfo(iatom, null, 0, null, null, null,
            T.lattice, null, 0, -1, 0, null);
      if (stype == null || stype.length() == 0)
        return false;
      if (u == null)
        u = vwr.getSymTemp();
      M3d m3 = (M3d) vwr.getModelForAtomIndex(iatom).auxiliaryInfo
          .get("primitiveToCrystal");
      if (!u.toFromPrimitive(toPrimitive, stype.charAt(0), ucnew, m3))
        return false;
    } else if ("reciprocal".equalsIgnoreCase(op)) {
      ucnew = SimpleUnitCell.getReciprocal(ucnew, null, scale);
      scale = 1;
    } else if ("vertices".equalsIgnoreCase(op)) {
      return mp.addXObj(BoxInfo.getVerticesFromOABC(ucnew));
    }
    if (scale != 1)
      for (int i = 1; i < 4; i++)
        ucnew[i].scale(scale);
    return mp.addXObj(ucnew);
  }

  @SuppressWarnings("unchecked")
  private boolean evaluateArray(ScriptMathProcessor mp, SV[] args,
                                boolean isSelector)
      throws ScriptException {
    if (isSelector) {
      SV x1 = mp.getX();
      switch (x1.tok) {
      case T.matrix3f:
      case T.matrix4f:
        return mp.addX(x1.toArray());
      }
      if (args.length == 0 || args.length > 2)
        return false;
      boolean doCopy = (args.length == 1 || args[1].tok == T.off);
      String id;
      Map<String, SV> m1;
      switch (x1.tok) {
      case T.matrix3f:
      case T.matrix4f:
        return mp.addX(x1.toArray());
      case T.hash:
        if (args.length != 1)
          return false;
        // map of maps to lst of maps
        Lst<SV> lst = new Lst<SV>();
        Map<String, SV> map = x1.getMap();
        String[] keys = x1.getKeys(false);
        // values must all be maps
        for (int i = 0, n = keys.length; i < n; i++)
          if (map.get(keys[i]).getMap() == null)
            return false;
        id = args[0].asString();
        for (int i = 0, n = keys.length; i < n; i++) {
          m1 = map.get(keys[i]).getMap();
          if (doCopy)
            m1 = (Map<String, SV>) SV.deepCopy(m1, true, false);
          m1.put(id, SV.newS(keys[i]));
          lst.addLast(SV.newV(T.hash, m1));
        }
        return mp.addXList(lst);
      case T.varray:
        boolean toArray = true;
        BS bsIndex = new BS();
        Map<String, SV> map1 = new Hashtable<String, SV>();
        Lst<SV> lst1 = x1.getList();
         //values must all be maps
        id = args[0].asString();
        for (int i = lst1.size(); --i >- 0;) {
          m1 = lst1.get(i).getMap();
          if (m1 == null || m1.get(id) == null)
            return false;
        }
        for (int i = 0, n = lst1.size(); i < n; i++) {
          m1 = lst1.get(i).getMap();
          if (m1 ==  null)
            return false;
          if (doCopy)
            m1 = (Map<String, SV>) SV.deepCopy(m1, true, false);
          SV mid = m1.remove(id);
          // only [0,10000]
          if (toArray) {
            if (mid.tok == T.integer && mid.intValue >= 0 && mid.intValue <= 10000) {
              bsIndex.set(mid.intValue);
            } else {
              toArray = false;
            }
          }
          String key = mid.asString();
          if (map1.containsKey(key))
            return false;
          map1.put(key, SV.newV(T.hash, m1));
        }
        if (toArray) {
          // first index needs to be 0 or 1
          int pt = bsIndex.nextSetBit(0);
          if (pt == 0 | pt == 1) {
            int len = bsIndex.cardinality();
            if (bsIndex.nextClearBit(pt) == len + pt) {
              // no empty slots
              SV[] a = new SV[len];
              for (Entry<String, SV> e: map1.entrySet()) {
                a[Integer.parseInt(e.getKey()) - pt] = e.getValue();
              }
              Lst<SV> list = new Lst<>();
              for (int i = 0; i < len; i++)
                list.addLast(a[i]);
              return mp.addXList(list);
            }
          }
        }
        return mp.addXObj(map1);          
      }
      return false;
    }
    SV[] a = new SV[args.length];
    for (int i = a.length; --i >= 0;)
      a[i] = SV.newT(args[i]);
    return mp.addXAV(a);
  }

  private boolean evaluateBin(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {
    int n = args.length;
    if (n < 3 || n > 5)
      return false;
    SV x1 = mp.getX();
    boolean isListf = (x1.tok == T.listf);
    if (!isListf && x1.tok != T.varray)
      return mp.addX(x1);
    double f0 = SV.dValue(args[0]);
    double f1 = SV.dValue(args[1]);
    double df = SV.dValue(args[2]);
    boolean addBins = (n >= 4 && args[n - 1].tok == T.on);
    String key = ((n == 5 || n == 4 && !addBins) && args[3].tok != T.off
        ? SV.sValue(args[3])
        : null);
    double[] data;
    Map<String, SV>[] maps = null;
    if (isListf) {
      data = (double[]) x1.value;
    } else {
      Lst<SV> list = x1.getList();
      data = new double[list.size()];
      if (key != null)
        maps = AU.createArrayOfHashtable(list.size());
      try {
        for (int i = list.size(); --i >= 0;)
          data[i] = SV.dValue(key == null ? list.get(i)
              : (maps[i] = list.get(i).getMap()).get(key));
      } catch (Exception e) {
        return false;
      }
    }
    int nbins = Math.max((int) Math.floor((f1 - f0) / df + 0.01f), 1);
    int[] array = new int[nbins];
    int nPoints = data.length;
    for (int i = 0; i < nPoints; i++) {
      double v = data[i];
      int bin = (int) Math.floor((v - f0) / df);
      if (bin < 0 || bin >= nbins)
        continue;
      array[bin]++;
      if (key != null) {
        Map<String, SV> map = maps[i];
        if (map == null)
          continue;
        map.put("_bin", SV.newI(bin));
        double v1 = f0 + df * bin;
        double v2 = v1 + df;
        map.put("_binMin", SV.newD(bin == 0 ? -Double.MAX_VALUE : v1));
        map.put("_binMax", SV.newD(bin == nbins - 1 ? Double.MAX_VALUE : v2));
      }
    }
    if (addBins) {
      Lst<double[]> lst = new Lst<double[]>();
      for (int i = 0; i < nbins; i++)
        lst.addLast(new double[] { f0 + df * i, array[i] });
      return mp.addXList(lst);
    }
    return mp.addXAI(array);
  }

  private boolean evaluateCache(ScriptMathProcessor mp, SV[] args) {
    if (args.length > 0)
      return false;
    return mp.addXMap(vwr.fm.cacheList());
  }

  private boolean evaluateColor(ScriptMathProcessor mp, SV[] args) {
    // color("toHSL", {r g b})         # r g b in 0 to 255 scale 
    // color("toRGB", "colorName or hex code") # r g b in 0 to 255 scale 
    // color("toRGB", {h s l})         # h s l in 360, 100, 100 scale 
    // color("rwb")                  # "" for most recently used scheme for coloring by property
    // color("rwb", min, max)        # min/max default to most recent property mapping 
    // color("rwb", min, max, value) # returns color
    // color("$isosurfaceId")        # info for a given isosurface
    // color("$isosurfaceId", value) # color for a given mapped isosurface value
    // color(ptColor1, ptColor2, n, asHSL)

    String colorScheme = (args.length > 0 ? SV.sValue(args[0]) : "");
    boolean isIsosurface = colorScheme.startsWith("$");
    if (args.length == 2 && colorScheme.equalsIgnoreCase("TOHSL"))
      return mp.addXPt(
          CU.rgbToHSL(P3d.newP(args[1].tok == T.point3f ? SV.ptValue(args[1])
              : CU.colorPtFromString(args[1].asString())), true));
    if (args.length == 2 && colorScheme.equalsIgnoreCase("TORGB")) {
      P3d pt = P3d.newP(args[1].tok == T.point3f ? SV.ptValue(args[1])
          : CU.colorPtFromString(args[1].asString()));
      return mp.addXPt(args[1].tok == T.point3f ? CU.hslToRGB(pt) : pt);
    }
    if (args.length == 4 && (args[3].tok == T.on || args[3].tok == T.off)) {
      P3d pt1 = P3d.newP(args[0].tok == T.point3f ? SV.ptValue(args[0])
          : CU.colorPtFromString(args[0].asString()));
      P3d pt2 = P3d.newP(args[1].tok == T.point3f ? SV.ptValue(args[1])
          : CU.colorPtFromString(args[1].asString()));
      boolean usingHSL = (args[3].tok == T.on);
      if (usingHSL) {
        pt1 = CU.rgbToHSL(pt1, false);
        pt2 = CU.rgbToHSL(pt2, false);
      }

      SB sb = new SB();
      V3d vd = V3d.newVsub(pt2, pt1);
      int n = args[2].asInt();
      if (n < 2)
        n = 20;
      vd.scale(1d / (n - 1));
      for (int i = 0; i < n; i++) {
        sb.append(Escape
            .escapeColor(CU.colorPtToFFRGB(usingHSL ? CU.hslToRGB(pt1) : pt1)));
        pt1.add(vd);
      }
      return mp.addXStr(sb.toString());
    }

    ColorEncoder ce = (isIsosurface ? null
        : vwr.cm.getColorEncoder(colorScheme));
    if (!isIsosurface && ce == null)
      return mp.addXStr("");
    double lo = (args.length > 1 ? SV.dValue(args[1]) : Double.MAX_VALUE);
    double hi = (args.length > 2 ? SV.dValue(args[2]) : Double.MAX_VALUE);
    double value = (args.length > 3 ? SV.dValue(args[3]) : Double.MAX_VALUE);
    boolean getValue = (value != Double.MAX_VALUE
        || lo != Double.MAX_VALUE && hi == Double.MAX_VALUE);
    boolean haveRange = (hi != Double.MAX_VALUE);
    if (!haveRange && colorScheme.length() == 0) {
      value = lo;
      double[] range = vwr.getCurrentColorRange();
      lo = range[0];
      hi = range[1];
    }
    if (isIsosurface) {
      // isosurface color scheme      
      String id = colorScheme.substring(1);
      Object[] data = new Object[] { id, null };
      if (!vwr.shm.getShapePropertyData(JC.SHAPE_ISOSURFACE, "colorEncoder",
          data))
        return mp.addXStr("");
      ce = (ColorEncoder) data[1];
    } else {
      ce.setRange(lo, hi, lo > hi);
    }
    Map<String, Object> key = ce.getColorKey();
    if (getValue)
      return mp.addXPt(CU.colorPtFromInt(
          ce.getArgb(hi == Double.MAX_VALUE ? lo : value), null));
    return mp.addX(SV.getVariableMap(key));
  }

  private boolean evaluateCompare(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {
    // compare([{bitset} or {positions}],[{bitset} or {positions}] [,"stddev"])
    // compare({bitset},{bitset}[,"SMARTS"|"SMILES"],smilesString [,"stddev"])
    // returns matrix4f for rotation/translation or stddev
    // compare({bitset},{bitset},"ISOMER", true)  14.32.12 (tautomer)
    // compare({bitset},{bitset},"ISOMER")  12.1.5
    // compare({bitset},{bitset},smartsString, "BONDS") 13.1.17
    // compare({bitset},{bitset},"SMILES", "BONDS") 13.3.9
    // compare({bitest},{bitset},"MAP","smilesString")
    // compare({bitset},{bitset},"MAP")
    // compare(@1,@2,"SMILES", "polyhedra"[,"stddev"])
    // compare(@1,@2,"SMARTS", "polyhedra"[,"stddev"])
    int narg = args.length;
    if (narg < 2 || narg > 5)
      return false;
    double stddev;
    boolean isTrue = (args[narg - 1].tok == T.on);
    if (isTrue || args[narg - 1].tok == T.off)
      narg--;
    String sOpt = SV.sValue(args[narg - 1]);
    boolean isStdDev = sOpt.equalsIgnoreCase("stddev");
    boolean isIsomer = sOpt.equalsIgnoreCase("ISOMER");
    boolean isTautomer = isIsomer && isTrue;
    boolean isBonds = sOpt.equalsIgnoreCase("BONDS");
    boolean isPoints = (args[0].tok == T.varray && args[1].tok == T.varray);
    Lst<SV> abmap = (narg >= 3 ? args[2].getList() : null);
    boolean isSmiles = (abmap == null && !isPoints && !isIsomer && narg > (isStdDev ? 3 : 2));
    BS bs1 = (args[0].tok == T.bitset ? (BS) args[0].value : null);
    BS bs2 = (args[1].tok == T.bitset ? (BS) args[1].value : null);
    String smiles1 = (bs1 == null ? SV.sValue(args[0]) : "");
    String smiles2 = (bs2 == null ? SV.sValue(args[1]) : "");
    stddev = Double.NaN;
    try {
      if (isBonds) {
        if (narg != 4)
          return false;
        // A, B, ........................BONDS
        // A, B, SMILES,...................BONDS
        smiles1 = SV.sValue(args[2]);
        isSmiles = smiles1.equalsIgnoreCase("SMILES");
        try {
          if (isSmiles)
            smiles1 = vwr.getSmiles(bs1);
        } catch (Exception ex) {
          e.evalError(ex.getMessage(), null);
        }
        double[] data = e.getSmilesExt().getFlexFitList(bs1, bs2, smiles1,
            !isSmiles);
        return (data == null ? mp.addXStr("") : mp.addXAD(data));
      }
      if (isIsomer) {
        if (narg != 3)
          return false;

        // A, B, ISOMER

        if (bs1 == null && bs2 == null) {
          String ret = vwr.getSmilesMatcher().getRelationship(smiles1, smiles2)
              .toUpperCase();
          return mp.addXStr(ret);
        }
        String mf1 = (bs1 == null
            ? vwr.getSmilesMatcher().getMolecularFormula(smiles1, false, false)
            : JmolMolecule.getMolecularFormulaAtoms(vwr.ms.at, bs1, null,
                false));
        String mf2 = (bs2 == null
            ? vwr.getSmilesMatcher().getMolecularFormula(smiles2, false, false)
            : JmolMolecule.getMolecularFormulaAtoms(vwr.ms.at, bs2, null,
                false));
        if (!mf1.equals(mf2))
          return mp.addXStr("NONE");
        if (bs1 != null)
          smiles1 = (String) e.getSmilesExt().getSmilesMatches("/strict///",
              null, bs1, null, JC.SMILES_TYPE_SMILES, true, false);
        boolean check;
        if (bs2 == null) {
          // note: find smiles1 IN smiles2 here
          check = (vwr.getSmilesMatcher().areEqual(smiles2, smiles1) > 0);
        } else {
          smiles2 = (String) e.getSmilesExt().getSmilesMatches("/strict///",
              null, bs2, null, JC.SMILES_TYPE_SMILES, true, false);
          check = (((BS) e.getSmilesExt().getSmilesMatches(
              "/strict///" + smiles1, null, bs2, null, JC.SMILES_TYPE_SMILES,
              true, false)).nextSetBit(0) >= 0);
        }
        if (!check) {
          // MF matched, but didn't match SMILES
          String s = smiles1 + smiles2;
          if (s.indexOf("/") >= 0 || s.indexOf("\\") >= 0
              || s.indexOf("@") >= 0) {
            if (smiles1.indexOf("@") >= 0
                && (bs2 != null || smiles2.indexOf("@") >= 0)
                && smiles1.indexOf("@SP") < 0) {
              // reverse chirality centers to see if we have an enantiomer
              // but only if not square planar
              int pt = smiles1.toLowerCase().indexOf("invertstereo");
              smiles1 = (pt >= 0
                  ? "/strict/" + smiles1.substring(0, pt)
                      + smiles1.substring(pt + 12)
                  : "/invertstereo strict/" + smiles1);//vwr.getSmilesMatcher().reverseChirality(smiles1);

              if (bs2 == null) {
                check = (vwr.getSmilesMatcher().areEqual(smiles1, smiles2) > 0);
              } else {
                check = (((BS) e.getSmilesExt().getSmilesMatches(smiles1, null,
                    bs2, null, JC.SMILES_TYPE_SMILES, true, false))
                        .nextSetBit(0) >= 0);
              }
              if (check)
                return mp.addXStr("ENANTIOMERS");
            }
            // remove all stereochemistry from SMILES string
            if (bs2 == null) {
              check = (vwr.getSmilesMatcher().areEqual("/nostereo/" + smiles2,
                  smiles1) > 0);
            } else {
              Object ret = e.getSmilesExt().getSmilesMatches(
                  "/nostereo/" + smiles1, null, bs2, null,
                  JC.SMILES_TYPE_SMILES, true, false);
              check = (((BS) ret).nextSetBit(0) >= 0);
            }
            if (check)
              return mp.addXStr("DIASTEREOMERS");
          }
          // MF matches, but not enantiomers or diastereomers
          String ret = "CONSTITUTIONAL ISOMERS";
          if (isTautomer) {
            // check for inchi same -- tautomers
            String inchi = vwr.getInchi(bs1, null, null);
            if (inchi != null && inchi.equals(vwr.getInchi(bs2, null, null)))
              ret = "TAUTOMERS";
          }
          return mp.addXStr(ret);
        }
        //identical or conformational 
        if (bs1 == null || bs2 == null)
          return mp.addXStr("IDENTICAL");
        stddev = e.getSmilesExt().getSmilesCorrelation(bs1, bs2, smiles1, null,
            null, null, null, false, null, null, false, JC.SMILES_TYPE_SMILES);
        return mp.addXStr(stddev < 0.2d ? "IDENTICAL"
            : "IDENTICAL or CONFORMATIONAL ISOMERS (RMSD=" + stddev + ")");
      }
      M4d m = new M4d();
      Lst<P3d> ptsA = null, ptsB = null;
      if (isSmiles) {
        // A, B, MAP
        // A, B, MAP, [pattern "H" "allH" "bestH"], ["stddev"]
        // A, B, SMILES, [pattern "H" "allH" "bestH"], ["stddev"]
        // A, B, SMARTS, pattern, ["stddev"]
        // A, B, SMILES, "polyhedron", [pattern "all" "best"], ["stddev"]
        // A, B, SMARTS, "polyhedron", pattern, ["all" "best"], ["stddev"]
        if (bs1 == null || bs2 == null)
          return false;
        sOpt = SV.sValue(args[2]);
        boolean isMap = sOpt.equalsIgnoreCase("MAP");
        isSmiles = sOpt.equalsIgnoreCase("SMILES");
        boolean isSearch = (isMap || sOpt.equalsIgnoreCase("SMARTS"));
        if (isSmiles || isSearch)
          sOpt = (narg > (isStdDev ? 4 : 3) ? SV.sValue(args[3]) : null);

        // sOpts = "H", "allH", "bestH", "polyhedra", pattern, or null
        boolean hMaps = (("H".equalsIgnoreCase(sOpt)
            || "allH".equalsIgnoreCase(sOpt)
            || "bestH".equalsIgnoreCase(sOpt)));

        boolean isPolyhedron = !hMaps && ("polyhedra".equalsIgnoreCase(sOpt)
            || "polyhedron".equalsIgnoreCase(sOpt));
        if (isPolyhedron) {
          stddev = e.getSmilesExt().mapPolyhedra(bs1.nextSetBit(0),
              bs2.nextSetBit(0), isSmiles, m);
        } else {
          ptsA = new Lst<P3d>();
          ptsB = new Lst<P3d>();
          boolean allMaps = (("all".equalsIgnoreCase(sOpt)
              || "allH".equalsIgnoreCase(sOpt)));
          boolean bestMap = (("best".equalsIgnoreCase(sOpt)
              || "bestH".equalsIgnoreCase(sOpt)));
          if ("stddev".equals(sOpt))
            sOpt = null;
          String pattern = sOpt;
          if (sOpt == null || hMaps || allMaps || bestMap) {
            // with explicitH we set to find only the first match.
            if (!isMap && !isSmiles || hMaps && isPolyhedron)
              return false;
            pattern = "/noaromatic" + (allMaps || bestMap ? "/" : " nostereo/")
                + e.getSmilesExt().getSmilesMatches((hMaps ? "H" : ""), null,
                    bs1, null, JC.SMILES_TYPE_SMILES | JC.SMILES_GEN_ALL_COMPONENTS, true, false);
          } else {
            allMaps = true;
          }
          stddev = e.getSmilesExt().getSmilesCorrelation(bs1, bs2, pattern,
              ptsA, ptsB, m, null, isMap, null, null, bestMap,
              (isSmiles ? JC.SMILES_TYPE_SMILES : JC.SMILES_TYPE_SMARTS)
                  | (!allMaps && !bestMap ? JC.SMILES_FIRST_MATCH_ONLY : 0));
          if (isMap) {
            int nAtoms = ptsA.size();
            if (nAtoms == 0)
              return mp.addXStr("");
            int nMatch = ptsB.size() / nAtoms;
            Lst<int[][]> ret = new Lst<int[][]>();
            for (int i = 0, pt = 0; i < nMatch; i++) {
              int[][] a = AU.newInt2(nAtoms);
              ret.addLast(a);
              for (int j = 0; j < nAtoms; j++, pt++)
                a[j] = new int[] { ((Atom) ptsA.get(j)).i,
                    ((Atom) ptsB.get(pt)).i };
            }
            return (allMaps ? mp.addXList(ret)
                : ret.size() > 0 ? mp.addXAII(ret.get(0)) : mp.addXStr(""));
          }
        }
      } else {
        // A, B
        // A, B, stddev
        // A, B, int[] map
        // A, B, int[] map, stddev
        // A or B can be bitset or point list
        ptsA = e.getPointVector(args[0], 0);
        ptsB = e.getPointVector(args[1], 0);
        if (ptsA == null || ptsB == null)
          return false;
        if (abmap != null) {
          narg--;
          int n = abmap.size();
          if (n > ptsA.size() || n != ptsB.size())
            return false;
          Lst<P3d> list = new Lst<P3d>();
          for (int i = 0; i < n; i++)
            list.addLast(ptsA.get(abmap.get(i).intValue - 1));
          ptsA = list;
        }
        switch (narg) {
        case 2:
          break;
        case 3:
          if (isStdDev)
            break;
          //$FALL-THROUGH$
        default:
          return false;
        }
        if (ptsA.size() == ptsB.size()) {
          Interface.getInterface("javajs.util.Eigen", vwr, "script");
          stddev = ScriptParam.getTransformMatrix4(ptsA, ptsB, m, null);
        }
      }
      // now have m and stddev
      return (isStdDev || Double.isNaN(stddev) ? mp.addXDouble(stddev)
          : mp.addXM4(m.round(1e-7d)));
    } catch (Exception ex) {
      e.evalError(ex.getMessage() == null ? ex.toString() : ex.getMessage(),
          null);
      return false;
    }
  }

  private boolean evaluateConnected(ScriptMathProcessor mp, SV[] args, int tok,
                                    int intValue)
      throws ScriptException {
    /*
     * Several options here:
     * 
     * .bondCount({_Au})
     * 
     * connected(1, 3, "single", {carbon})
     * 
     * means "atoms connected to carbon by from 1 to 3 single bonds"
     * and returns an atom set.
     * 
     * connected(1.0, 1.5, "single", {carbon}, {oxygen})
     * 
     * means "single bonds from 1.0 to 1.5 Angstroms between carbon and oxygen"
     * and returns a bond bitset.
     * 
     * connected({*}.bonds, "DOUBLE")
     * 
     * means just that and returns a bond set
     * 
     * 
     */

    if (args.length > 5)
      return false;
    double min = Integer.MIN_VALUE, max = Integer.MAX_VALUE;
    double fmin = 0, fmax = Double.MAX_VALUE;

    int order = Edge.BOND_ORDER_ANY;
    BS atoms1 = null;
    BS atoms2 = null;
    boolean haveDecimal = false;
    boolean isBonds = false;
    switch (tok) {
    case T.polyhedra:
      // polyhedra()
      // polyhedra(n)
      // polyhedra(smilesString)
      // {xx}.polyhedra()
      // {xx}.polyhedra(n)
      // {xx}.polyhedra(smilesString)
      int nv = Integer.MIN_VALUE;
      String smiles = null;
      if (args.length > 0) {
        switch (args[0].tok) {
        case T.integer:
          nv = args[0].intValue;
          break;
        case T.string:
          smiles = SV.sValue(args[0]);
          break;
        }
      }
      if (intValue == T.polyhedra)
        atoms1 = SV.getBitSet(mp.getX(), true);
      Object[] data = new Object[] { Integer.valueOf(nv), smiles, atoms1 };
      if (!vwr.shm.getShapePropertyData(JC.SHAPE_POLYHEDRA, "getCenters", data))
        data[1] = null;
      return mp.addXBs(data[1] == null ? new BS() : (BS) data[1]);
    case T.bondcount:
      // {atoms1}.bondCount({atoms2})
      SV x1 = mp.getX();
      if (x1.tok != T.bitset || args.length != 1 || args[0].tok != T.bitset)
        return false;
      atoms1 = (BS) x1.value;
      atoms2 = (BS) args[0].value;
      Lst<Integer> list = new Lst<Integer>();
      Atom[] atoms = vwr.ms.at;
      for (int i = atoms1.nextSetBit(0); i >= 0; i = atoms1.nextSetBit(i + 1)) {
        int n = 0;
        Bond[] b = atoms[i].bonds;
        for (int j = b.length; --j >= 0;)
          if (atoms2.get(b[j].getOtherAtom(atoms[i]).i))
            n++;
        list.addLast(Integer.valueOf(n));
      }
      return mp.addXList(list);
    }

    // connected(
    for (int i = 0; i < args.length; i++) {
      SV var = args[i];
      switch (var.tok) {
      case T.bitset:
        isBonds = (var.value instanceof BondSet);
        if (isBonds && atoms1 != null)
          return false;
        if (atoms1 == null)
          atoms1 = (BS) var.value;
        else if (atoms2 == null)
          atoms2 = (BS) var.value;
        else
          return false;
        break;
      case T.string:
        String type = SV.sValue(var);
        if (type.equalsIgnoreCase("hbond"))
          order = Edge.BOND_HYDROGEN_MASK;
        else
          order = Edge.getBondOrderFromString(type);
        if (order == Edge.BOND_ORDER_NULL)
          return false;
        break;
      case T.decimal:
        haveDecimal = true;
        //$FALL-THROUGH$
      default:
        int n = var.asInt();
        double f = var.asDouble();
        if (max != Integer.MAX_VALUE)
          return false;

        if (min == Integer.MIN_VALUE) {
          min = Math.max(n, 0);
          fmin = f;
        } else {
          max = n;
          fmax = f;
        }
      }
    }
    if (min == Integer.MIN_VALUE) {
      min = 1;
      max = 100;
      fmin = JC.DEFAULT_MIN_CONNECT_DISTANCE;
      fmax = JC.DEFAULT_MAX_CONNECT_DISTANCE;
    } else if (max == Integer.MAX_VALUE) {
      max = min;
      fmax = fmin;
      fmin = JC.DEFAULT_MIN_CONNECT_DISTANCE;
    }
    if (atoms1 == null)
      atoms1 = vwr.getAllAtoms();
    if (haveDecimal && atoms2 == null)
      atoms2 = atoms1;
    if (atoms2 != null) {
      BS bsBonds = new BS();
      vwr.makeConnections(fmin, fmax, order, T.identify, atoms1, atoms2,
          bsBonds, isBonds, false, 0);
      return mp.addX(SV.newV(T.bitset, BondSet.newBS(bsBonds)));
    }
    return mp.addXBs(vwr.ms.getAtomsConnected(min, max, order, atoms1));
  }

  private boolean evaluateContact(ScriptMathProcessor mp, SV[] args) {
    if (args.length < 1 || args.length > 3)
      return false;
    int i = 0;
    double distance = 100;
    int tok = args[0].tok;
    switch (tok) {
    case T.decimal:
    case T.integer:
      distance = SV.dValue(args[i++]);
      break;
    case T.bitset:
      break;
    default:
      return false;
    }
    if (i == args.length || !(args[i].value instanceof BS))
      return false;
    BS bsA = BSUtil.copy((BS) args[i++].value);
    BS bsB = (i < args.length ? BSUtil.copy((BS) args[i].value) : null);
    RadiusData rd = new RadiusData(null,
        (distance > 10 ? distance / 100 : distance),
        (distance > 10 ? EnumType.FACTOR : EnumType.OFFSET), VDW.AUTO);
    bsB = setContactBitSets(bsA, bsB, true, Double.NaN, rd, false);
    bsB.or(bsA);
    return mp.addXBs(bsB);
  }

  private boolean evaluateData(ScriptMathProcessor mp, SV[] args) {

    // x = data("someprefix*") # the data
    // x = data("somedataname") # the data
    // x = data("data2d_xxxx") # 2D data (x,y paired values)
    // x = data("data2d_xxxx", iSelected) # selected row of 2D data, with <=0
    // meaning "relative to the last row"
    // x = data({atomno < 10},"xyz") # (or "pdb" or "mol") coordinate data in
    // xyz, pdb, or mol format
    // x = data(someData,ptrFieldOrColumn,nBytes,firstLine) # extraction of a
    // column of data based on a field (nBytes = 0) or column range (nBytes >
    // 0)
    String selected = (args.length == 0 ? "" : SV.sValue(args[0]));
    switch (args.length) {
    case 0:
    case 1:
      break;
    case 2:
    case 3:
      if (args[0].tok == T.bitset)
        return mp.addXStr(vwr.getModelFileData(selected, SV.sValue(args[1]),
            args.length == 3 && SV.bValue(args[2])));
      break;
    case 4:
      int iField = args[1].asInt();
      int nBytes = args[2].asInt();
      int firstLine = args[3].asInt();
      double[] f = Parser.parseDoubleArrayFromMatchAndField(SV.sValue(args[0]),
          null, 0, 0, null, iField, nBytes, null, firstLine);
      return mp.addXStr(Escape.escapeDoubleA(f, false));
    default:
      return false;
    }
    if (selected.indexOf("data2d_") == 0) {
      // tab, newline separated data
      double[][] f1 = (double[][]) vwr.getDataObj(selected, null,
          JmolDataManager.DATA_TYPE_ADD);
      if (f1 == null)
        return mp.addXStr("");
      if (args.length == 2 && args[1].tok == T.integer) {
        int pt = args[1].intValue;
        if (pt < 0)
          pt += f1.length;
        if (pt >= 0 && pt < f1.length)
          return mp.addXStr(Escape.escapeDoubleA(f1[pt], false));
        return mp.addXStr("");
      }
      return mp.addXStr(Escape.escapeDoubleAA(f1, false));
    }

    // parallel mp.addition of double property data sets

    if (selected.endsWith("*"))
      return mp.addXList((Lst<?>) vwr.getDataObj(selected, null, 0));
    if (selected.indexOf("property_") == 0) {
      double[] f1 = (double[]) vwr.getDataObj(selected, null,
          JmolDataManager.DATA_TYPE_AD);
      return (f1 == null ? mp.addXStr("")
          : mp.addXStr(Escape.escapeDoubleA(f1, false)));
    }

    // some other data type -- just return it

    //if (args.length == 1) {
    Object[] data = (Object[]) vwr.getDataObj(selected, null,
        JmolDataManager.DATA_TYPE_UNKNOWN);
    return mp.addXStr(data == null || data.length < 2 ? "" : "" + data[1]);
    // }
  }

  /**
   * x = y.distance({atoms}) the average distance from elements of y to the
   * CENTER of {atoms}
   * 
   * x = {atomset1}.distance.min({atomset2}, asAtomSet) If asAtomSet is true,
   * returns the closest atom in atomset1 to any atom of atomset2; if false or
   * omitted, returns an array listing the distance of each atom in atomset1 to
   * the closest atom in atomset2. This array can be used to assign properties
   * to atomset1: {1.1}.property_d = {1.1}.distance.min({2.1}); color {1.1}
   * property_d.
   * 
   * x = {atomset1}.distance.min({point}, asAtomSet) If asAtomSet is true,
   * returns the atom in atomset1 closest to the specified point;if false or
   * omitted, returns the closest distance to the specified point from any atom
   * in atomset1.
   * 
   * x = {atomset1}.distance.min({atomset2}).min returns the shortest distance
   * from any atom in atomset1 to any atom in atomset2.
   * 
   * x = {atomset1}.distance.max({atomset2}, asAtomSet) If asAtomSet is true,
   * returns the furthest atom in atomset1 to any atom of atomset2; if false or
   * omitted, returns an array listing the distance of each atom in atomset1 to
   * the furthest atom in atomset2.
   * 
   * x = {atomset1}.distance.max({point}, asAtomSet) If asAtomSet is true,
   * returns the atom in atomset1 furthest from the specified point;if false or
   * omitted, returns the furthest distance to the specified point from any atom
   * in atomset1.
   * 
   * x = {atomset1}.distance.max({atomset2}).max returns the furthest distance
   * from any atom in atomset1 to any atom in atomset2.
   * 
   * x = {atomset1}.distance.all({atomset2}) returns an array or array of arrays
   * of values
   * 
   * @param mp
   * @param args
   * @param tok
   * @param op
   *        optional .min .max for distance
   * @return true if successful
   * @throws ScriptException
   */

  private boolean evaluateDotDist(ScriptMathProcessor mp, SV[] args, int tok,
                                  int op)
      throws ScriptException {
    boolean isDist = (tok == T.distance);
    SV x1, x2, x3 = null;
    switch (args.length) {
    case 2:
      if (op == Integer.MAX_VALUE) {
        x1 = args[0];
        x2 = args[1];
        break;
      }
      x3 = args[1];
      //$FALL-THROUGH$
    case 1:
      x1 = mp.getX();
      x2 = args[0];
      break;
    case 0:
      if (isDist) {
        x1 = mp.getX();
        x2 = SV.getVariable(new P3d());
        break;
      }
      //$FALL-THROUGH$
    default:
      return false;
    }

    double f = Double.NaN;
    try {
      if (tok == T.cross) {
        P3d a = P3d.newP(mp.ptValue(x1, null));
        a.cross(a, mp.ptValue(x2, null));
        return mp.addXPt(a);
      }

      P3d pt2 = (x2.tok == T.varray ? null : mp.ptValue(x2, null));
      P4d plane2 = e.planeValue(x2);
      if (isDist) {
        int minMax = (op == Integer.MIN_VALUE ? 0 : op & T.minmaxmask);
        boolean isMinMax = (minMax == T.min || minMax == T.max);
        boolean isAll = minMax == T.minmaxmask;
        switch (x1.tok) {
        case T.varray:
        case T.bitset:
          boolean isAtomSet1 = (x1.tok == T.bitset);
          boolean isAtomSet2 = (x2.tok == T.bitset);
          boolean isPoint2 = (x2.tok == T.point3f);
          BS bs1 = (isAtomSet1 ? (BS) x1.value : null);
          BS bs2 = (isAtomSet2 ? (BS) x2.value : null);
          Lst<SV> list1 = (isAtomSet1 ? null : x1.getList());
          Lst<SV> list2 = (isAtomSet2 ? null : x2.getList());
          boolean returnAtom = (isMinMax && x3 != null && x3.asBoolean());
          switch (x2.tok) {
          case T.bitset:
          case T.varray:
            //$FALL-THROUGH$
          case T.point3f:
            Atom[] atoms = vwr.ms.at;
            if (returnAtom) {
              double dMinMax = Double.NaN;
              int iMinMax = Integer.MAX_VALUE;
              if (isAtomSet1) {
                for (int i = bs1.nextSetBit(0); i >= 0; i = bs1
                    .nextSetBit(i + 1)) {
                  double d = (isPoint2 ? atoms[i].distanceSquared(pt2)
                      : ((Number) e.getBitsetProperty(bs2, list2, op, atoms[i],
                          plane2, x1.value, null, false, x1.index, false))
                              .doubleValue());
                  if (minMax == T.min ? d >= dMinMax : d <= dMinMax)
                    continue;
                  dMinMax = d;
                  iMinMax = i;
                }
                return mp.addXBs(iMinMax == Integer.MAX_VALUE ? new BS()
                    : BSUtil.newAndSetBit(iMinMax));
              }
              // list of points 
              for (int i = list1.size(); --i >= 0;) {
                P3d pt = SV.ptValue(list1.get(i));
                double d = (isPoint2 ? pt.distanceSquared(pt2)
                    : ((Number) e.getBitsetProperty(bs2, list2, op, pt, plane2,
                        x1.value, null, false, Integer.MAX_VALUE, false))
                            .doubleValue());
                if (minMax == T.min ? d >= dMinMax : d <= dMinMax)
                  continue;
                dMinMax = d;
                iMinMax = i;
              }
              return mp.addXInt(iMinMax);
            }
            if (isAll) {
              if (bs2 == null) {
                double[] data = new double[bs1.cardinality()];
                for (int p = 0, i = bs1.nextSetBit(0); i >= 0; i = bs1
                    .nextSetBit(i + 1), p++)
                  data[p] = atoms[i].distance(pt2);
                return mp.addXAD(data);
              }
              double[][] data2 = new double[bs1.cardinality()][bs2
                  .cardinality()];
              for (int p = 0, i = bs1.nextSetBit(0); i >= 0; i = bs1
                  .nextSetBit(i + 1), p++)
                for (int q = 0, j = bs2.nextSetBit(0); j >= 0; j = bs2
                    .nextSetBit(j + 1), q++)
                  data2[p][q] = atoms[i].distance(atoms[j]);
              return mp.addXADD(data2);
            }
            if (isMinMax) {
              double[] data = new double[isAtomSet1 ? bs1.cardinality()
                  : list1.size()];
              if (isAtomSet1) {
                for (int i = bs1.nextSetBit(0), p = 0; i >= 0; i = bs1
                    .nextSetBit(i + 1))
                  data[p++] = ((Number) e.getBitsetProperty(bs2, list2, op,
                      atoms[i], plane2, x1.value, null, false, x1.index, false))
                          .doubleValue();
                return mp.addXAD(data);
              }
              // list of points
              for (int i = data.length; --i >= 0;)
                data[i] = ((Number) e.getBitsetProperty(bs2, list2, op,
                    SV.ptValue(list1.get(i)), plane2, null, null, false,
                    Integer.MAX_VALUE, false)).doubleValue();
              return mp.addXAD(data);
            }
            return mp.addXObj(e.getBitsetProperty(bs1, list1, op, pt2, plane2,
                x1.value, null, false, x1.index, false));
          }
        }
      }
      P3d pt1 = mp.ptValue(x1, null);
      P4d plane1 = e.planeValue(x1);
      if (isDist) {
        if (plane2 != null && x3 != null)
          f = MeasureD.directedDistanceToPlane(pt1, plane2, SV.ptValue(x3));
        else
          f = (plane1 == null
              ? (plane2 == null ? pt2.distance(pt1)
                  : MeasureD.distanceToPlane(plane2, pt1))
              : MeasureD.distanceToPlane(plane1, pt2));
      } else {
        if (plane1 != null && plane2 != null) {
          // q1.dot(q2) assume quaternions
          f = plane1.x * plane2.x + plane1.y * plane2.y + plane1.z * plane2.z
              + plane1.w * plane2.w;
          // plane.dot(point) =
        } else {
          if (plane1 != null)
            pt1 = P3d.new3(plane1.x, plane1.y, plane1.z);
          // point.dot(plane)
          else if (plane2 != null)
            pt2 = P3d.new3(plane2.x, plane2.y, plane2.z);
          f = pt1.dot(pt2);
        }
      }
    } catch (Exception e) {
    }
    return mp.addXDouble(f);
  }

  private boolean evaluateHelix(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {
    if (args.length < 1 || args.length > 5)
      return false;
    // helix({resno=3})
    // helix({resno=3},"point|axis|radius|angle|draw|measure|array")
    // helix(resno,"point|axis|radius|angle|draw|measure|array")
    // helix(pt1, pt2, dq, "point|axis|radius|angle|draw|measure|array|")
    // helix(pt1, pt2, dq, "draw","someID")
    // helix(pt1, pt2, dq)
    int pt = (args.length > 2 ? 3 : 1);
    String type = (pt >= args.length ? "array" : SV.sValue(args[pt]));
    int tok = T.getTokFromName(type);
    if (args.length > 2) {
      // helix(pt1, pt2, dq ...)
      P3d pta = mp.ptValue(args[0], null);
      P3d ptb = mp.ptValue(args[1], null);
      if (tok == T.nada || args[2].tok != T.point4f || pta == null
          || ptb == null)
        return false;
      Qd dq = Qd.newP4((P4d) args[2].value);
      T3d[] data = MeasureD.computeHelicalAxis(pta, ptb, dq);
      //new T3[] { pt_a_prime, n, r, P3.new3(theta, pitch, residuesPerTurn) };
      return (data == null ? false
          : mp.addXObj(Escape.escapeHelical(type, tok, pta, ptb, data)));
    }
    BS bs = (args[0].value instanceof BS ? (BS) args[0].value
        : vwr.ms.getAtoms(T.resno, Integer.valueOf(args[0].asInt())));
    switch (tok) {
    case T.point:
    case T.axis:
    case T.radius:
      return mp.addXObj(getHelixData(bs, tok));
    case T.angle:
      return mp.addXDouble(((Number) getHelixData(bs, T.angle)).doubleValue());
    case T.draw:
    case T.measure:
      return mp.addXObj(getHelixData(bs, tok));
    case T.array:
      String[] data = (String[]) getHelixData(bs, T.list);
      if (data == null)
        return false;
      return mp.addXAS(data);
    }
    return false;
  }

  private Object getHelixData(BS bs, int tokType) {
    int iAtom = bs.nextSetBit(0);
    return (iAtom < 0 ? "null"
        : vwr.ms.at[iAtom].group.getHelixData(tokType, vwr.getQuaternionFrame(),
            vwr.getInt(T.helixstep)));
  }

  private boolean evaluateInChI(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {
    // {*}.inchi(options)
    // InChI.inchi("key")
    // InChI.inchi("structure")
    // smiles.inchi(options) // including "key"
    // molFIleData.inchi(options) // including "key" and "smiles"
    SV x1 = mp.getX();
    String flags = (args.length > 0 ? SV.sValue(args[0]) : "fixedh?");
    if (flags.toLowerCase().equals("standard"))
      flags = "";
    BS atoms = SV.getBitSet(x1, true);
    String molData = (atoms == null ? SV.sValue(x1) : null);
    String ret = vwr.getInchi(atoms, molData, flags);
    return (flags.indexOf("model") >= 0 ? mp.addXMap(vwr.parseJSONMap(ret)) : mp.addXStr(ret));
  }

  private boolean evaluateFind(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {

    // {*}.find("equivalentAtoms")
    // {*}.find("spacegroup")
    // "test.find("t");
    // "test.find("t","v"); // or "m" or "i"
    // {1.1}.find("2.1","map", labelFormat)
    // {*}.find("inchi",inchi-options)
    // {*}.find("crystalClass")
    // {*}.find("CF",true|false)
    // {*}.find("MF")
    // {*}.find("MF", "C2H4")
    // {*}.find("SEQUENCE")
    // {*}.find("SEQ")
    // {*}.find("SEQ", true)
    // {*}.find("SEQUENCE", true)
    // {*}.find("SEQUENCE", "H")
    // "AVA".find("SEQUENCE")
    // "a sequence".find("sequence", "i") // case insensitive
    // "a sequence".find("sequence", true) // case insensitive
    // "a sequence".find("sequence", false) // case sensitive
    // {*}.find("SMARTS", "CCCC")
    // "CCCC".find("SMARTS", "CC")
    // "CCCC".find("SMILES", "MF")
    // "CCCC".find("SMILES", "MF", true) // empirical formula, including implicit
    // {2.1}.find("SMILES", "MF", true) // empirical formula, including implicit
    // {2.1}.find("CCCC",{1.1}) // find pattern "CCCC" in {2.1} with conformation given by {1.1}
    // {*}.find("ccCCN","BONDS")
    // {*}.find("SMILES","H")
    // {*}.find("chemical",type)
    // {1.1}.find("SMILES", {2.1})
    // {1.1}.find("SMARTS", {2.1})
    // smiles1.find("SMILES", smiles2)
    // smiles1.find("SMILES", smiles2)
    // [smiles1, smiles2,...].find("SMARTS", pattern)
    // array.find("xxx")
    // array.find("xxx", "index")
    // array.find("xxx","i|m|v")
    // array.find("xxx","i|m|v", "index")

    SV x1 = mp.getX();
    boolean isList = (x1.tok == T.varray);
    boolean isAtoms = (x1.tok == T.bitset);
    boolean isEmpty = (args.length == 0);
    int tok0 = (args.length == 0 ? T.nada : args[0].tok);
    String sFind = (isEmpty || tok0 != T.string ? "" : SV.sValue(args[0]));
    boolean isOff = (args.length > 1 && args[1].tok == T.off);
    int tokLast = (tok0 == T.nada ? T.nada : args[args.length - 1].tok);
    SV argLast = (tokLast == T.nada ? SV.vF : args[args.length - 1]);
    boolean isON = !isList && (tokLast == T.on);
    boolean asIndex = (isList && argLast.tok == T.string && "index".equalsIgnoreCase(SV.sValue(argLast)));
    String flags = (args.length > 1 && args[1].tok != T.on
        && args[1].tok != T.off && args[1].tok != T.bitset ? SV.sValue(args[1])
            : "");
    boolean isSequence = !isList && !isOff
        && sFind.equalsIgnoreCase("SEQUENCE");
    boolean isSeq = !isList && !isOff && sFind.equalsIgnoreCase("SEQ");
    if (sFind.toUpperCase().startsWith("SMILES/")) {
      if (!sFind.endsWith("/"))
        sFind += "/";
      String s = sFind.substring(6) + "//";
      if (JC.isSmilesCanonical(s)) {
        flags = "SMILES";
        sFind = "CHEMICAL";
      } else {
        sFind = "SMILES";
        flags = s + flags;
      }
    } else if (sFind.toUpperCase().startsWith("SMARTS/")) {
      if (!sFind.endsWith("/"))
        sFind += "/";
      flags = sFind.substring(6) + (flags.length() == 0 ? "//" : flags);
      sFind = "SMARTS";
    }

    Object smiles = null;
    boolean isStr = false;
    boolean isPatternObj = (args.length > 0 && args[0].tok == T.pattern);
    if (!isPatternObj && x1.tok == T.string) {
      // check in case pattern is "SMILES" or "SMARTS" or one of the other options tested for next
      switch (args.length) {
      case 1:
        // InChI.find("SMILES")
        if (((String) x1.value).startsWith("InChI=")) {
          if (sFind.equalsIgnoreCase("SMILES")) {
            return mp.addXStr(vwr.getInchi(null, x1.value, "SMILES" + flags));
          }
        }
        isStr = !sFind.equals("SMILES");
        break;
      case 2:
      case 3:
        // "xx".find("yyy",FALSE|TRUE|""|"m"|"i"|"v") or some combination of m,i,v
        if (isOff || isON) {
          isStr = true;
        } else if (((String) x1.value).startsWith("InChI=")) {
          // InChI.find("SMARTS", ....);
          if (sFind.equals("SMARTS")) {
            smiles = vwr.getInchi(null, x1.value, "SMILES");
          } else {
            isStr = true;
          }
        } else if (flags.length() <= 3) {
          if (flags.replace('m', ' ').replace('i', ' ').replace('v', ' ').trim()
              .length() == 0)
            isStr = true;
        }
        break;
      }
    }

    boolean isSmiles = !isStr && sFind.equalsIgnoreCase("SMILES");
    boolean isSMARTS = isPatternObj
        || !isStr && sFind.equalsIgnoreCase("SMARTS");
    boolean isChemical = !isList && !isStr
        && sFind.equalsIgnoreCase("CHEMICAL");
    boolean isMF = !isList && !isStr && sFind.equalsIgnoreCase("MF");
    boolean isCF = !isList && !isStr && sFind.equalsIgnoreCase("CELLFORMULA");

    boolean isInchi = isAtoms && !isList && sFind.equalsIgnoreCase("INCHI");
    boolean isInchiKey = isAtoms && !isList
        && sFind.equalsIgnoreCase("INCHIKEY");
    boolean isStructureMap = (!isSmiles && !isSMARTS && tok0 == T.bitset
        && flags.toLowerCase().indexOf("map") >= 0);
    boolean isEquivalent = !isSmiles && !isSMARTS
        && ((x1.tok == T.bitset || x1.tok == T.point3f || x1.tok == T.varray)
            && sFind.toLowerCase().startsWith("equivalent"));

    try {
      if (isEquivalent) {
        switch (x1.tok) {
        case T.bitset:
          return mp
              .addXBs(vwr.ms.getSymmetryEquivAtoms((BS) x1.value, null, null));
        case T.point3f:
          return mp.addXList(
              vwr.getSymmetryEquivPoints((P3d) x1.value, sFind + flags));
        case T.varray:
          Lst<P3d> lst = new Lst<P3d>();
          Lst<SV> l0 = x1.getList();
          for (int i = 0, n = l0.size(); i < n; i++) {
            P3d p = SV.ptValue(l0.get(i));
            if (p == null)
              return false;
            lst.addLast(p);
          }
          return mp.addXList(vwr.getSymmetryEquivPointList(lst, sFind + flags));
        }
      } else if (isInchi || isInchiKey) {
        if (isInchiKey)
          flags += " key";
        return mp.addXStr(vwr.getInchi(SV.getBitSet(x1, true), null, flags));
      }
      if (isChemical) {
        BS bsAtoms = (isAtoms ? (BS) x1.value : null);
        String data = (bsAtoms == null ? SV.sValue(x1)
            : vwr.getOpenSmiles(bsAtoms));
        data = (data.length() == 0 ? ""
            : vwr.getChemicalInfo(data, flags.toLowerCase(), bsAtoms)).trim();
        if (data.startsWith("InChI"))
          data = PT.rep(PT.rep(data, "InChI=", ""), "InChIKey=", "");
        return mp.addXStr(data);
      }
      if (isSmiles || isSMARTS || isAtoms) {
        int iPt = (isStructureMap ? 0 : isSmiles || isSMARTS ? 2 : 1);
        BS bs2 = (iPt < args.length && args[iPt].tok == T.bitset
            ? (BS) args[iPt++].value
            : null);
        boolean asBonds = (argLast.tok == T.string && "bonds".equalsIgnoreCase(SV.sValue(argLast)));
        boolean isAll = (asBonds || isON);
        boolean isSmilesObj = false;
        Object ret = null;
        switch (x1.tok) {
        case T.pattern:
          isSmilesObj = true;
          //$FALL-THROUGH$
        case T.varray:
        case T.string:
          if (smiles == null && !isList) {
            smiles = x1.value;
          }
          if ((isSmiles || isSMARTS) && args.length == 1 && flags == null) {
            return false;
          }
          if (bs2 != null)
            return false;
          if (flags.equalsIgnoreCase("mf")) {
            ret = vwr.getSmilesMatcher().getMolecularFormula(smiles.toString(), isSMARTS,
                isON);
          } else {
            Object pattern = (isPatternObj ? args[0].value : flags);
            // "SMARTS",flags,asMap, allMappings
            boolean allMappings = true;
            boolean asMap = false;
            if (isPatternObj) {
              switch (args.length) {
              case 3:
                allMappings = SV.bValue(args[2]);
                //$FALL-THROUGH$
              case 2:
                asMap = SV.bValue(args[1]);
                break;
              }
            } else {
              switch (args.length) {
              case 4:
                allMappings = SV.bValue(args[3]);
                //$FALL-THROUGH$
              case 3:
                asMap = SV.bValue(args[2]);
                break;
              }
            }
            boolean isChirality = (!isPatternObj
                && pattern.equals("chirality"));
            boolean justOne = (!asMap
                && (!allMappings || !isSMARTS && !isChirality));
            try {
              if (isList) {
                Lst<SV> list = x1.getList();
                Object[] o = new Object[list.size()];
                for (int i = o.length; --i >= 0;) {
                  o[i] = list.get(i).value;
                }
                smiles = o;
              }
              ret = e.getSmilesExt().getSmilesMatches(pattern,
                  smiles, null, null,
                  isSMARTS ? JC.SMILES_TYPE_SMARTS : JC.SMILES_TYPE_SMILES,
                  !asMap, !allMappings);
              if (isList)
                return mp.addXObj(ret);
            } catch (Exception ex) {
              System.out.println(ex.getMessage());
              return mp.addXInt(-1);
            }
            int len = (isChirality ? 1
                : AU.isAI(ret) ? ((int[]) ret).length : ((int[][]) ret).length);
            if (len == 0
                && vwr.getSmilesMatcher().getLastException() != "MF_FAILED"
                && (isSmilesObj || 
                ((String) smiles).toLowerCase().indexOf("noaromatic") < 0
                && ((String) smiles).toLowerCase().indexOf("strict") < 0)) {
              // problem arising from Jmol interpreting one string as aromatic
              // and the other not, perhaps because of one using [N] as in NCI caffeine
              // and one not, as from PubChem. 
              // There is no loss in doing this search again, except for 
              ret = e.getSmilesExt().getSmilesMatches(pattern, smiles, null,
                  null,
                  JC.SMILES_NO_AROMATIC | (isSMARTS ? JC.SMILES_TYPE_SMARTS
                      : JC.SMILES_TYPE_SMILES),
                  !asMap, !allMappings);
            }
            if (justOne) {
              return mp.addXInt(!allMappings && len > 0 ? 1 : len);
            }
          }
          break;
        case T.bitset:
          BS bs = (BS) x1.value;
          if (sFind.equalsIgnoreCase("spacegroup")) {
            Object o = vwr.findSpaceGroup(null, bs, null, null, null, null,
                ("parent".equals(flags.toLowerCase()) ? JC.SG_CHECK_SUPERCELL
                    : 0));
            if (o != null) {
              String s = o.toString();
              o = PT.rep(s.substring(s.indexOf(" ") + 1), " #", "\t");
            }
            return mp.addXStr(o.toString());
          }
          if (sFind.equalsIgnoreCase("crystalClass")) {
            // {*}.find("crystalClass")
            // {*}.find("crystalClass", pt)
            int n = bs.nextSetBit(0);
            BS bsNew = null;
            if (args.length != 2) {
              bsNew = new BS();
              bsNew.set(n);
            }
            return mp.addXList(vwr.ms.generateCrystalClass(n,
                (bsNew != null ? vwr.ms.getAtomSetCenter(bsNew)
                    : argLast.tok == T.bitset
                        ? vwr.ms.getAtomSetCenter((BS) argLast.value)
                        : SV.ptValue(argLast))));
          }
          if (isMF && flags.length() != 0) {
            return mp.addXBs(JmolMolecule.getBitSetForMF(vwr.ms.at, bs, flags));
          }
          if (isMF || isCF) {
            boolean isEmpirical = isON;
            return mp.addXStr(vwr.getFormulaForAtoms(bs,
                (isMF ? "MF" : "CELLFORMULA"), isEmpirical));
          }
          if (isSequence || isSeq) {
            boolean isHH = (argLast.asString().equalsIgnoreCase("H"));
            isAll |= isHH;
            return mp.addXStr(vwr.getSmilesOpt(bs, -1, -1, (isAll
                ? JC.SMILES_GEN_BIO_ALLOW_UNMATCHED_RINGS
                    | JC.SMILES_GEN_BIO_COV_CROSSLINK
                    | (isHH ? JC.SMILES_GEN_BIO_HH_CROSSLINK : 0)
                : 0)
                | (isSeq ? JC.SMILES_GEN_BIO_NOCOMMENTS : JC.SMILES_GEN_BIO),
                null));
          }
          if (isStructureMap) {
            int[] map = null, map1 = null, map2 = null;
            Object[][] mapNames = null;
            String key = (args.length == 3 ? SV.sValue(argLast) : null);
            char itype = (key == null || key.equals("%i")
                || key.equals("number") ? 'i'
                    : key.equals("%a") || key.equals("name") ? 'a'
                        : key.equals("%D") || key.equals("index") ? 'D' : '?');
            if (key == null)
              key = "number";
            String err = null;
            flags = flags.replace("map", "").trim();
            sFind = vwr.getSmilesOpt(bs, 0, 0, 0, flags);
            if (bs.cardinality() != bs2.cardinality()) {
              err = "atom sets are not the same size";
            } else {
              try {
                int iflags = (JC.SMILES_TYPE_SMILES | JC.SMILES_MAP_UNIQUE
                    | JC.SMILES_FIRST_MATCH_ONLY);
                if (flags.length() > 0)
                  sFind = "/" + flags + "/" + sFind;
                map1 = vwr.getSmilesMatcher().getCorrelationMaps(sFind,
                    vwr.ms.at, vwr.ms.ac, bs, iflags)[0];
                int[][] m2 = vwr.getSmilesMatcher().getCorrelationMaps(sFind,
                    vwr.ms.at, vwr.ms.ac, bs2, iflags);
                if (m2.length > 0) {
                  map = new int[bs.length()];
                  for (int i = map.length; --i >= 0;)
                    map[i] = -1;
                  map2 = m2[0];
                  for (int i = map1.length; --i >= 0;)
                    map[map1[i]] = map2[i];
                  mapNames = new Object[map1.length][2];
                  BS bsAll = BS.copy(bs);
                  bsAll.or(bs2);
                  String[] names = (itype == '?' ? new String[bsAll.length()]
                      : null);
                  if (names != null)
                    names = (String[]) e.getCmdExt().getBitsetIdentFull(bsAll,
                        key, false, Integer.MAX_VALUE, false, names);
                  Atom[] at = vwr.ms.at;
                  for (int pt = 0, i = bs.nextSetBit(0); i >= 0; i = bs
                      .nextSetBit(i + 1)) {
                    int j = map[i];
                    if (j == -1)
                      continue;
                    Object[] a;
                    switch (itype) {
                    case 'a':
                      a = new String[] { at[i].getAtomName(),
                          at[j].getAtomName() };
                      break;
                    case 'i':
                      a = new Integer[] {
                          Integer.valueOf(at[i].getAtomNumber()),
                          Integer.valueOf(at[j].getAtomNumber()) };
                      break;
                    case 'D':
                      a = new Integer[] { Integer.valueOf(i),
                          Integer.valueOf(j) };
                      break;
                    default:
                      a = new String[] { names[i], names[j] };
                      break;
                    }
                    mapNames[pt++] = a;
                  }
                }
              } catch (Exception ee) {
                err = ee.getMessage();
              }
            }
            Map<String, Object> m = new Hashtable<String, Object>();
            m.put("BS1", bs);
            m.put("BS2", bs2);
            m.put("SMILES", sFind);
            if (err == null) {
              m.put("SMILEStoBS1", map1);
              m.put("SMILEStoBS2", map2);
              m.put("BS1toBS2", map);
              m.put("MAP1to2", mapNames);
              m.put("key", key);
            } else {
              m.put("error", err);
            }
            return mp.addXMap(m);
          }
          if (isSmiles || isSMARTS) {
            sFind = (args.length > 1 && args[1].tok == T.bitset
                ? vwr.getSmilesOpt((BS) args[1].value, 0, 0, 0, flags)
                : flags);
          }
          flags = flags.toUpperCase();
          BS bsMatch3D = bs2;
          if (flags.indexOf("INCHI") >= 0) {
            return mp.addXStr(vwr.getInchi(bs, null, "SMILES/" + flags));
          }
          if (flags.equals("MF")) {
            smiles = e.getSmilesExt().getSmilesMatches("", null, bs,
                bsMatch3D, JC.SMILES_TYPE_SMILES, true, false);
            ret = vwr.getSmilesMatcher().getMolecularFormula(smiles.toString(), false,
                isON);
          } else if (asBonds) {
            // this will return a single match
            int[][] map = vwr.getSmilesMatcher().getCorrelationMaps(sFind,
                vwr.ms.at, vwr.ms.ac, bs,
                (isSmiles ? JC.SMILES_TYPE_SMILES : JC.SMILES_TYPE_SMARTS)
                    | JC.SMILES_FIRST_MATCH_ONLY);
            ret = (map.length > 0 ? vwr.ms.getDihedralMap(map[0]) : new int[0]);
          } else if (flags.equals("MAP")) {
            // we add NO_AROMATIC because that is not important for structure-SMILES matching
            int[][] map = vwr.getSmilesMatcher().getCorrelationMaps(sFind,
                vwr.ms.at, vwr.ms.ac, bs,
                (isSmiles ? JC.SMILES_TYPE_SMILES : JC.SMILES_TYPE_SMARTS)
                    | JC.SMILES_MAP_UNIQUE | JC.SMILES_NO_AROMATIC);
            ret = map;
          } else {
            // SMILES or SMARTS only
            int smilesFlags = (isSmiles ?

                (flags.indexOf("OPEN") >= 0 ? JC.SMILES_TYPE_OPENSMILES
                    : JC.SMILES_TYPE_SMILES)
                : JC.SMILES_TYPE_SMARTS)
                | (isON && sFind.length() == 0 ? JC.SMILES_GEN_BIO_COV_CROSSLINK
                    | JC.SMILES_GEN_BIO_COMMENT : 0);
            if (isSmiles && flags.indexOf("/ALL/") >= 0) {
              smilesFlags |= JC.SMILES_GEN_ALL_COMPONENTS;
            }
            if (flags.indexOf("/MOLECULE/") >= 0) {
              // all molecules 
              JmolMolecule[] mols = vwr.ms.getMolecules();
              Lst<BS> molList = new Lst<BS>();
              for (int i = 0; i < mols.length; i++) {
                if (mols[i].atomList.intersects(bs)) {
                  BS bsRet = (BS) e.getSmilesExt().getSmilesMatches(sFind, null,
                      mols[i].atomList, bsMatch3D, smilesFlags, !isON, false);
                  if (!bsRet.isEmpty())
                    molList.addLast(bsRet);
                }
              }
              ret = molList;
            } else {
              ret = e.getSmilesExt().getSmilesMatches(sFind, null, bs,
                  bsMatch3D, smilesFlags, !isON, false);
            }
          }
          break;
        }
        if (ret == null)
          e.invArg();
        return mp.addXObj(ret);
      }
    } catch (Exception ex) {
      e.evalError(ex.getMessage(), null);
    }
    BS bs = new BS();
    Lst<SV> svlist = (isList ? x1.getList() : null);
    if (isList && tok0 != T.string && tok0 != T.nada) {
      SV v = args[0];
      for (int i = 0, n = svlist.size(); i < n; i++) {
        if (SV.areEqual(svlist.get(i), v))
          bs.set(i);
      }
      int[] ret = new int[bs.cardinality()];
      for (int pt = 0, i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        ret[pt++] = i + 1;
      return mp.addXAI(ret);
    }

    boolean isReverse = (flags.indexOf("v") >= 0);
    boolean isCaseInsensitive = (flags.indexOf("i") >= 0) || isOff;
    boolean asMatch = (flags.indexOf("m") >= 0);
    boolean checkEmpty = (sFind.length() == 0);
    boolean isPattern = (!checkEmpty && !isEquivalent && args.length == (asIndex ? 3 : 2));
    if (isList || isPattern) {
      JmolPatternMatcher pm = (isPattern ? getPatternMatcher() : null);
      Pattern pattern = null;
      if (isPattern) {
        try {
          pattern = pm.compile(sFind, isCaseInsensitive);
        } catch (Exception ex) {
          e.evalError(ex.toString(), null);
        }
      }
      String[] list = (checkEmpty ? null : SV.strListValue(x1));
      int nlist = (checkEmpty ? svlist.size() : list.length);
      if (Logger.debugging)
        Logger.debug("finding " + sFind);
      int n = 0;
      Matcher matcher = null;
      Lst<String> v = (asMatch ? new Lst<String>() : null);
      String what = "";
      for (int i = 0; i < nlist; i++) {
        boolean isMatch;
        if (checkEmpty) {
          SV o = svlist.get(i);
          switch (o.tok) {
          case T.hash:
            isMatch = (o.getMap().isEmpty() != isEmpty);
            break;
          case T.varray:
            isMatch = ((o.getList().size() == 0) != isEmpty);
            break;
          case T.string:
            isMatch = ((o.asString().length() == 0) != isEmpty);
            break;
          default:
            isMatch = true;
          }
        } else if (isPattern) {
          what = list[i];
          matcher = pattern.matcher(what);
          isMatch = matcher.find();
        } else {
          isMatch = (SV.sValue(svlist.get(i)).indexOf(sFind) >= 0);
        }
        if (asMatch && isMatch || !asMatch && isMatch == !isReverse) {
          n++;

          bs.set(i);
          if (asMatch)
            v.addLast(
                isReverse
                    ? what.substring(0, matcher.start())
                        + what.substring(matcher.end())
                    : matcher.group());
        }
      }
      if (!isList) {
        return (asMatch ? mp.addXStr(v.size() == 1 ? (String) v.get(0) : "")
            : isReverse ? mp.addXBool(n == 1)
                : asMatch ? mp.addXStr(n == 0 ? "" : matcher.group())
                    : mp.addXInt(n == 0 ? 0 : matcher.start() + 1));
      }
      // removed in 14.2/3.14 -- not documented and not expected      if (n == 1)
      //    return mp.addXStr(asMatch ? (String) v.get(0) : list[ipt]);
      if (asIndex) {
        Lst<SV> lst = new Lst<>();
        for (int i = bs.nextSetBit(0); i >=0; i = bs.nextSetBit(i + 1)) {
          lst.addLast(SV.newI(i + 1));
        }
        return mp.addXList(lst);
      }
      if (asMatch) {
        String[] listNew = new String[n];
        if (n > 0)
          for (int i = list.length; --i >= 0;)
            if (bs.get(i)) {
              --n;
              listNew[n] = (asMatch ? (String) v.get(n) : list[i]);
            }
        return mp.addXAS(listNew);
      }
      Lst<SV> l = new Lst<SV>();
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        l.addLast(svlist.get(i));
      return mp.addXList(l);
    }
    if (isSequence) {
      return mp.addXStr(vwr.getJBR().toStdAmino3(SV.sValue(x1)));
    }
    return mp.addXInt(SV.sValue(x1).indexOf(sFind) + 1);
  }

  /**
   * _ by itself, not as a function, is shorthand for
   * getProperty("auxiliaryInfo")
   * 
   * $ print _.keys
   * 
   * boundbox group3Counts group3Lists modelLoadNote models properties
   * someModelsHaveFractionalCoordinates someModelsHaveSymmetry
   * someModelsHaveUnitcells symmetryRange
   * 
   * 
   * _m by itself, not as a function, is shorthand for
   * getProperty("auxiliaryInfo.models")[_currentFrame]
   * 
   * $ print format("json",_m.unitCellParams)
   * 
   * [ 0.0,0.0,0.0,0.0,0.0,0.0,0.0,-2.1660376,-2.1660376,0.0,-2.1660376,
   * 2.1660376,-4.10273,0.0,0.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN ]
   * 
   * 
   * {atomset}._ by itself delivers a subset array of auxiliaryInfo.models for
   * all models in {atomset}
   * 
   * $ print {*}._..1..aflowInfo
   * 
   * (first model's aflowInfo)
   * 
   * 
   * _(key) prepends "auxiliaryInfo.models", delivering a modelCount-length
   * array of information
   * 
   * $ print _("aflowInfo[SELECT auid WHERE H__eV___VASP_ < 0]")
   * 
   * 
   * {atomset}._(key) selects for model Auxiliary info related to models of the
   * specified atoms
   * 
   * {atomset}.getProperty(key) defaults to atomInfo, but also allows key to
   * start with "bondInfo"
   * 
   * Examples:
   * 
   * print _("aflowInfo[select sg where volume_cell > 70]")
   * 
   * print {model>10}._("aflowInfo[select sg where volume_cell > 70]")
   * 
   * @param mp
   * @param args
   * @param tok0
   * @param isAtomProperty
   * @return true if no syntax problems
   * @throws ScriptException
   */
  @SuppressWarnings("unchecked")
  private boolean evaluateGetProperty(ScriptMathProcessor mp, SV[] args,
                                      int tok0, boolean isAtomProperty)
      throws ScriptException {
    int nargs = args.length;
    boolean isSelect = (isAtomProperty && tok0 == T.select);
    boolean isPivot = (isAtomProperty && tok0 == T.pivot);
    boolean isAuxiliary = (tok0 == T.__);
    int pt = 0;
    int tok = (nargs == 0 ? T.nada : args[0].tok);
    if (nargs == 2 && (tok == T.varray || tok == T.hash || tok == T.context)) {
      return mp.addXObj(
          vwr.extractProperty(args[0].value, args[1].value.toString(), -1));
    }
    BS bsSelect = (isAtomProperty && nargs == 1 && args[0].tok == T.bitset
        ? (BS) args[0].value
        : null);
    String pname = (bsSelect == null && nargs > 0 ? SV.sValue(args[pt++]) : "");
    String propertyName = pname;
    String lc = propertyName.toLowerCase();
    if (!isSelect && lc.indexOf("[select ") < 0)
      propertyName = lc;
    boolean isJSON = false;
    if (propertyName.equals("json") && nargs > pt) {
      isJSON = true;
      propertyName = SV.sValue(args[pt++]);
    }
    SV x = null;
    if (isAtomProperty) { // also includes $isosurface1.getProperty
      x = mp.getX();
      switch (x.tok) {
      case T.bitset:
        break;
      case T.string:
        String name = (String) x.value;
        Object[] data = new Object[3];
        int shapeID;
        if (name.startsWith("$")) {
          // "$P4".getProperty....
          name = name.substring(1);
          shapeID = vwr.shm.getShapeIdFromObjectName(name);
          if (shapeID >= 0) {
            data[0] = name;
            vwr.shm.getShapePropertyData(shapeID, "index", data);
            if (data[1] != null && !pname.equals("index")) {
              int index = ((Integer) data[1]).intValue();
              data[1] = vwr.shm.getShapePropertyIndex(shapeID, pname.intern(),
                  index);
            }
          }
        } else {
          shapeID = JC.shapeTokenIndex(T.getTokFromName(name));
          if (shapeID >= 0) {
            // "isosurface".getProperty...
            data[0] = pname;
            data[1] = Integer.valueOf(-1);
            vwr.shm.getShapePropertyData(shapeID, pname.intern(), data);
          }
        }
        return (data[1] == null ? mp.addXStr("") : mp.addXObj(data[1]));
      case T.varray:
        if (isPivot) {
          Lst<SV> lstx = x.getList();
          if (nargs == 0)
            return mp.addXObj(getMinMax(lstx, T.pivot, true));
          // array of hash pivot("key")
          Map<String, SV> map = new Hashtable<String, SV>();
          String sep = (nargs > 1 ? SV.sValue(args[nargs - 1]) : null);
          if (sep != null)
            nargs--;
          String[] keys = new String[nargs];
          for (int i = 0; i < nargs; i++)
            keys[i] = SV.sValue(args[i]);
          for (int i = 0, n = lstx.size(); i < n; i++) {
            SV sv = lstx.get(i);
            if (sv.tok != T.hash)
              continue;
            Map<String, SV> mapi = sv.getMap();
            String key = "";
            for (int j = 0; j < nargs; j++) {
              SV obj = mapi.get(keys[j]);
              key += (j == 0 ? "" : sep) + SV.sValue(obj);
            }
            SV vlist = map.get(key);
            if (vlist == null)
              map.put(key, vlist = SV.newV(T.varray, new Lst<SV>()));
            vlist.getList().addLast(sv);
          }
          return mp.addXMap(map);
        }
        if (bsSelect != null) {
          Lst<SV> l0 = x.getList();
          Lst<SV> lst = new Lst<SV>();
          for (int i = bsSelect.nextSetBit(0); i >= 0; i = bsSelect
              .nextSetBit(i + 1))
            lst.addLast(l0.get(i));
          return mp.addXList(lst);
        }
        //$FALL-THROUGH$
      default:
        if (tok0 == T.pivot && x.tok == T.hash) {
          Map<String, Object> map = new Hashtable<String, Object>();
          Map<String, SV> map0 = x.getMap();
          for (Entry<String, SV> e : map0.entrySet()) {
            String key = e.getKey();
            String s = e.getValue().asString();
            Lst<String> l = (Lst<String>) map.get(s);
            if (l == null)
              map.put(s, l = new Lst<String>());
            l.addLast(key);
          }
          if ("count".equals(lc)) {
            for (Entry<String, Object> e : map.entrySet()) {
              e.setValue(Integer.valueOf(((Lst<String>) e.getValue()).size()));
            }

          }
          return mp.addXMap(map);
        }
        if (isSelect)
          propertyName = "[SELECT " + propertyName + "]";
        return mp.addXObj(vwr.extractProperty(x, propertyName, -1));
      }
      if (!lc.startsWith("bondinfo") && !lc.startsWith("atominfo")
          && !lc.startsWith("modelkitinfo"))
        propertyName = "atomInfo." + propertyName;
    }
    Object propertyValue = "";
    if (propertyName.equalsIgnoreCase("fileContents") && nargs >= 2) {
      String s = SV.sValue(args[1]);
      for (int i = 2; i < nargs; i++)
        s += "|" + SV.sValue(args[i]);
      propertyValue = s;
      pt = nargs;
    } else if (nargs > pt) {
      switch (args[pt].tok) {
      case T.bitset:
        propertyValue = args[pt++].value;
        if (propertyName.equalsIgnoreCase("bondInfo") && nargs > pt
            && args[pt].tok == T.bitset)
          propertyValue = new BS[] { (BS) propertyValue, (BS) args[pt].value };
        break;
      case T.hash:
      case T.string:
        if (vwr.checkPropertyParameter(propertyName))
          propertyValue = args[pt++].value;
        break;
      }
    }
    if (isAtomProperty) {
      BS bs = (BS) x.value;
      int iAtom = bs.nextSetBit(0);
      if (iAtom < 0)
        return mp.addXStr("");
      propertyValue = bs;
    }
    if (isAuxiliary && !isAtomProperty)
      propertyName = "auxiliaryInfo.models." + propertyName;
    propertyName = PT.rep(propertyName, ".[", "[");
    Object property = vwr.getProperty(null, propertyName, propertyValue);
    if (pt < nargs)
      property = vwr.extractProperty(property, args, pt);
    return mp.addXObj(isJSON ? SV.safeJSON("value", property)
        : SV.isVariableType(property) ? property
            : Escape.toReadable(propertyName, property));
  }

  private boolean evaluateFormat(ScriptMathProcessor mp, int intValue,
                                 SV[] args, boolean isLabel)
      throws ScriptException {
    // {xxx}.label()

    // {xxx}.label("....")
    // {xxx}.yyy.format("...")
    // (value).format("...")
    // array.format([headings])
    // map.format([headings])

    // why did I do this???
    // format("base64", x)
    // format("JSON", x)
    // format("byteArray", x)
    // format("array", x)
    // better:
    // format(x, "base64")
    // format(x, "JSON")
    // format(x, "byteArray")
    // format(x, "array") 
    // format(matrix, "xyz")
    // format(matrix, "abc")
    // format(matrix, "uvw") // same as xyz
    // so now accept both!

    // format("....",a,b,c...)f -- could be format(format,"xx")
    // format("....",[a1, a2, a3, a3....])

    // matrix4f.format("xyz" | "abc" | "uvw")
    if (isLabel && args.length > 1)
      return false;
    SV x1 = (args.length < 2 || intValue == T.format ? mp.getX() : null);
    String format;
    int pt = -1;
    SV x = null;
    switch (args.length) {
    case 0:
      format = "%U";
      break;
    case 1:
      if (x1 == null)
        return false;
      switch (args[0].tok) {
      case T.varray:
        // array.format([headings])
        // map.format([headings])
        format = null;
        break;
      case T.string:
        // {xxx}.label("....")
        // {xxx}.yyy.format("...")
        // (value).format("...")
        // matrix4f.format("xyz" | "abc" | "uvw")
        format = (String) args[0].value;
        pt = SV.getFormatType(format);
        if (pt >= 0)
          x = x1;
        break;
      default:
        return false;
      }
      break;
    case 2:
      if (args[0].tok == T.string) {
        format = SV.sValue(args[0]);
        x = args[1];
        // format("xxx%s","testing");
        // format("base64", x)
        // format("JSON", x)
        // format("byteArray", x)
        // format("array", x)
      } else {
        // format(x, "base64")
        // format(x, "JSON")
        // format(x, "byteArray")
        // format(x, "array") 
        // format(matrix, "xyz")
        // format(matrix, "abc")
        // format(matrix, "uvw")
        x = args[0];
        format = SV.sValue(args[1]);
      }
      pt = SV.getFormatType(format);
      break;
    default:
      // all others -- more than 2 arguments must be format(format,a,b,c,...)
      format = (String) args[0].value;
      pt = SV.getFormatType(format);
      if (pt >= 0)
        return false;
      break;
    }
    
    switch (pt) {
    case -1:
      // {*}.label();
      // {*}.label("xxx")
      // format("xxx%s","testing");
      break;
    case SV.FORMAT_XYZ:
    case SV.FORMAT_ABC:
    case SV.FORMAT_UVW:
      return (x.tok == T.matrix4f && mp.addXStr(matToString((M4d) x.value, pt)));
    default:
//    case SV.FORMAT_JSON:
//    case SV.FORMAT_BYTEARRAY:
//    case SV.FORMAT_BASE64:
//    case SV.FORMAT_ARRAY:
      return mp.addXObj(SV.getFormat(x, pt));
    }
    BS bs = (x1 != null && x1.tok == T.bitset ? (BS) x1.value : null);
    if (!isLabel && args.length > 0 && bs == null && format != null) {
      // x.format("xxxx")
      // x1.format("%5.3f %5s", ["energy", "pointGroup"])
      // but not x1.format() or {*}.format(....)
      if (args.length == 2) {
        Lst<SV> listIn = x1.getList();
        Lst<SV> formatList = x.getList();
        if (listIn == null || formatList == null)
          return false;
        x1 = SV.getVariableList(getSublist(listIn, formatList));
      }
      args = new SV[] { args[0], x1 };
      x1 = null;
    }
    if (x1 == null) {
      if (format == null || pt >= 0 && args.length != 2)
        return false;
      if (pt >= 0 || args.length < 2 || args[1].tok != T.varray) {
        //format("%i %i", 2,3);
        //format("%i %i", [2,3]);
        Object o = SV.format(args, pt);
        return (o instanceof String ? mp.addXStr((String) o) : mp.addXObj(o));
      }
      // fill an array with applied formats
      Lst<SV> a = args[1].getList();
      SV[] args2 = new SV[] { args[0], null };
      String[] sa = new String[a.size()];
      for (int i = sa.length; --i >= 0;) {
        args2[1] = a.get(i);
        sa[i] = SV.format(args2, pt).toString();
      }
      return mp.addXAS(sa);
    }
    // arrayOfArrays.format(["energy", "pointGroup"]);
    // arrayOfMaps.format(["energy", "pointGroup"]);
    if (x1.tok == T.varray && format == null) {
      Lst<SV> listIn = x1.getList();
      Lst<SV> formatList = args[0].getList();
      Lst<SV> listOut = getSublist(listIn, formatList);
      return mp.addXList(listOut);
    }
    Object ret = null;
    if (format == null) {
      ret = "";
    } else if (bs == null) {
      ret = SV.sprintf(PT.formatCheck(format), x1);
    } else {
      // format.all??? label.all??? not documented
      // just use xxx.all.format....
      // but, yes, a single return here does give string, not an array.
      // 
      // boolean asArray = T.tokAttr(intValue, T.minmaxmask); // "all"
      ret = e.getCmdExt().getBitsetIdent(bs, format, x1.value, true, x1.index,false);     
    }
    return mp.addXObj(ret); 
  }

  /**
   * [ {...},{...}... ] ==> [[...],[...]]
   * 
   * @param listIn
   * @param formatList
   * @return sublist
   */
  private Lst<SV> getSublist(Lst<SV> listIn, Lst<SV> formatList) {
    Lst<SV> listOut = new Lst<SV>();
    Map<String, SV> map;
    SV v;
    Lst<SV> list;
    for (int i = 0, n = listIn.size(); i < n; i++) {
      SV element = listIn.get(i);
      switch (element.tok) {
      case T.hash:
        map = element.getMap();
        list = new Lst<SV>();
        for (int j = 0, n1 = formatList.size(); j < n1; j++) {
          v = map.get(SV.sValue(formatList.get(j)));
          list.addLast(v == null ? SV.newS("") : v);
        }
        listOut.addLast(SV.getVariableList(list));
        break;
      case T.varray:
        map = new Hashtable<String, SV>();
        list = element.getList();
        for (int j = 0, n1 = Math.min(list.size(),
            formatList.size()); j < n1; j++) {
          map.put(SV.sValue(formatList.get(j)), list.get(j));
        }
        listOut.addLast(SV.getVariable(map));
      }
    }
    return listOut;
  }

  private boolean evaluateList(ScriptMathProcessor mp, int tok, SV[] args)
      throws ScriptException {
    // array.add(x) 
    // array.add(sep, x) 
    // array.sub(x) 
    // array.mul(x) 
    // array.mul3(x)
    // array.div(x) 
    // array.push() 
    // array.pop() 

    // array.join()
    // array.join(sep)
    // array.join(sep,true)
    // array.join("",true) (CSV)

    // array.split()
    // array.split(sep)
    // array.split("\t",true)
    // array.split("",true) (CSV)
    
    // string.push
    // string.pop
    
    int len = args.length;
    SV x1 = mp.getX();
    SV x2;
    switch (tok) {
    case T.push:
      return (len == 2 && mp.addX(x1.pushPop(args[0], args[1]))
          || len == 1 && mp.addX(x1.pushPop(null, args[0])));
    case T.pop:
      return (len == 1 && mp.addX(x1.pushPop(args[0], null))
          || len == 0 && mp.addX(x1.pushPop(null, null)));
    case T.add:
      if (len != 1 && len != 2)
        return false;
      break;
    case T.split:
    case T.join:
      break;
    default:
      if (len != 1)
        return false;
    }
    boolean isArray1 = (x1.tok == T.varray);
    if (len == 2) {
      Object ret = listSpecial(tok, x1, SV.sValue(args[0]), args[1], isArray1);
      return (ret != null && mp.addXObj(ret));
    }
    x2 = (len == 0 ? SV.newV(T.all, "all") : args[0]);
    boolean isAll = (x2.tok == T.all);
    if (!isArray1 && x1.tok != T.string)
      return mp.binaryOp(opTokenFor(tok), x1, x2);
    boolean isScalar1 = SV.isScalar(x1);
    boolean isScalar2 = SV.isScalar(x2);

    double[] list1 = null;
    double[] list2 = null;
    Lst<SV> alist1 = x1.getList();
    Lst<SV> alist2 = x2.getList();

    String[] sList1 = null, sList2 = null;
    if (isArray1) {
      len = alist1.size();
    } else if (isScalar1) {
      len = Integer.MAX_VALUE;
    } else {
      sList1 = (PT.split(SV.sValue(x1), "\n"));
      list1 = new double[len = sList1.length];
      PT.parseDoubleArrayData(sList1, list1);
    }
    if (isAll && tok != T.join) {
      double sum = 0d;
      if (isArray1) {
        for (int i = len; --i >= 0;)
          sum += SV.dValue(alist1.get(i));
      } else if (!isScalar1) {
        for (int i = len; --i >= 0;)
          sum += list1[i];
      }
      return mp.addXDouble(sum);
    }
    if (tok == T.join && x2.tok == T.string) {
      SB sb = new SB();
      if (isScalar1) {
        sb.append(SV.sValue(x1));
      } else {
        String s = (isAll ? "" : x2.value.toString());
        for (int i = 0; i < len; i++)
          sb.append(i > 0 ? s : "").append(SV.sValue(alist1.get(i)));
      }
      return mp.addXStr(sb.toString());
    }

    SV scalar = null;
    if (isScalar2) {
      scalar = x2;
    } else if (x2.tok == T.varray) {
      len = Math.min(len, alist2.size());
    } else {
      sList2 = PT.split(SV.sValue(x2), "\n");
      list2 = new double[sList2.length];
      PT.parseDoubleArrayData(sList2, list2);
      len = Math.min(len, list2.length);
    }

    T token = opTokenFor(tok);

    if (isArray1 && isAll) {
      Lst<SV> llist = new Lst<SV>();
      return mp.addXList(addAllLists(x1.getList(), llist));
    }
    SV a = (isScalar1 ? x1 : null);
    SV b;
    boolean justVal = (len == Integer.MAX_VALUE);
    if (justVal)
      len = 1;
    SV[] olist = new SV[len];
    for (int i = 0; i < len; i++) {
      if (isScalar2)
        b = scalar;
      else if (x2.tok == T.varray)
        b = alist2.get(i);
      else if (Double.isNaN(list2[i]))
        b = SV.getVariable(SV.unescapePointOrBitsetAsVariable(sList2[i]));
      else
        b = SV.newD(list2[i]);
      if (!isScalar1) {
        if (isArray1)
          a = alist1.get(i);
        else if (Double.isNaN(list1[i]))
          a = SV.getVariable(SV.unescapePointOrBitsetAsVariable(sList1[i]));
        else
          a = SV.newD(list1[i]);
      }
      if (tok == T.join) {
        if (a.tok != T.varray) {
          Lst<SV> l = new Lst<SV>();
          l.addLast(a);
          a = SV.getVariableList(l);
        }
      }
      if (!mp.binaryOp(token, a, b))
        return false;
      olist[i] = mp.getX();
    }
    return (justVal ? mp.addXObj(olist[0]) : mp.addXAV(olist));
  }

  private Object listSpecial(int tok, SV x1, String tab, SV x2, boolean isArray1) {
    // special add, join, split
    String[] sList1 = null, sList2 = null, sList3 = null;
    if (tok == T.add) {
      // [...].add("\t", [...])
      // [...].add("", [...])
      sList1 = (isArray1 ? SV.strListValue(x1)
          : PT.split(SV.sValue(x1), "\n"));
      sList2 = (x2.tok == T.varray ? SV.strListValue(x2)
          : PT.split(SV.sValue(x2), "\n"));
      int len = Math.max(sList1.length, sList2.length);
      sList3 = new String[len];
      for (int i = 0; i < len; i++)
        sList3[i] = (i >= sList1.length ? "" : sList1[i]) + tab
            + (i >= sList2.length ? "" : sList2[i]);
      return sList3;
    }
    if (x2.tok != T.on)
      return null; // second parameter must be "true" for now.
    Lst<SV> l = x1.getList();
    boolean isCSV = (tab.length() == 0);
    if (isCSV)
      tab = ",";
    //boolean isTSV = (tab.equals("\t"));
    if (tok == T.join) {
      // [...][...].join(sep, true) [2D-array line join]
      // [...][...].join("", true)  [CSV join] ["\t" for TSV join
      SV[] s2 = new SV[l.size()];
      for (int i = l.size(); --i >= 0;) {
        Lst<SV> a = l.get(i).getList();
        if (a == null)
          s2[i] = l.get(i);
        else {
          SB sb = new SB();
          for (int j = 0, n = a.size(); j < n; j++) {
            if (j > 0)
              sb.append(tab);
            SV sv = a.get(j);
            String s = null;
            if (sv.tok == T.string) {
              String st = (String) sv.value;
              if (isCSV) {
                s = "\"" + PT.rep(st, "\"", "\"\"") + "\"";
// escape tabs?
//              } else if (isTSV && st.indexOf('\t') >= 0) {
//                s = "\"" + PT.rep(st, "\"", "\"\"") + "\"";
              }
            }
            sb.append(s == null ? "" + sv.asString() : s);
          }
          s2[i] = SV.newS(sb.toString());
        }
      }
      return s2;
    }
    // [...].split(sep, true) [split individual elements as strings]
    // [...].split("", true) [CSV split]
    Lst<SV> sa = new Lst<SV>();
    if (isCSV)
      tab = "\0";
    int[] next = new int[2];
    for (int i = 0, nl = l.size(); i < nl; i++) {
      String line = l.get(i).asString();
      if (isCSV) {
        next[1] = 0;
        next[0] = 0;
        int last = 0;
        while (true) {
          String s = PT.getCSVString(line, next);
          if (s == null) {
            if (next[1] == -1) {
              // unmatched -- continue with next line if present
              // or just close quotes gracefully
              line += (++i < nl ? "\n" + l.get(i).asString() : "\"");
              next[1] = last;
              continue;
            }
            line = line.substring(0, last)
                + line.substring(last).replace(',', '\0');
            break;
          }
          line = line.substring(0, last)
              + line.substring(last, next[0]).replace(',', '\0') + s
              + line.substring(next[1]);
          next[1] = last = next[0] + s.length();
        }
      }
      String[] linaa = line.split(tab);
      Lst<SV> la = new Lst<SV>();
      for (int j = 0, n = linaa.length; j < n; j++) {
        String s = linaa[j];
        if (s.indexOf(".") < 0)
          try {
            la.addLast(SV.newI(Integer.parseInt(s)));
            continue;
          } catch (Exception e) {
          }
        else
          try {
            la.addLast(SV.getVariable(Double.valueOf(Double.parseDouble(s))));
            continue;
          } catch (Exception ee) {
          }
        la.addLast(SV.newS(s));
      }
      sa.addLast(SV.getVariableList(la));
    }
    return SV.getVariableList(sa);
  }

  private Lst<SV> addAllLists(Lst<SV> list, Lst<SV> l) {
    int n = list.size();
    for (int i = 0; i < n; i++) {
      SV v = list.get(i);
      if (v.tok == T.varray)
        addAllLists(v.getList(), l);
      else
        l.addLast(v);
    }
    return l;
  }

  @SuppressWarnings("unchecked")
  private boolean evaluateLoad(ScriptMathProcessor mp, SV[] args,
                               boolean isFile)
      throws ScriptException {
    // file("myfile.xyz")
    // load("myfile.png",true)
    // load("myfile.txt",1000)
    // load("myfile.xyz",0,true)
    // load("myfile.json","JSON")
    // load("myfile.json","JSON", true)
    if (!checkAccess())
      return false;
    if (args.length < 1 || args.length > 3)
      return false;
    String file = FileManager.fixDOSName(SV.sValue(args[0]));
    boolean asMap = (args.length > 1 && args[1].tok == T.on);
    boolean async = (vwr.async
        || args.length > 2 && args[args.length - 1].tok == T.on);
    int nBytesMax = (args.length > 1 && args[1].tok == T.integer
        ? args[1].asInt()
        : -1);
    boolean asJSON = (args.length > 1
        && args[1].asString().equalsIgnoreCase("JSON"));
    if (asMap)
      return mp.addXMap((Map<String, Object>) vwr.fm.getFileAsMap(file, null, false));
    boolean isQues = file.startsWith("?");
    if (Viewer.isJS && (isQues || async)) {
      if (isFile && isQues)
        return mp.addXStr("");
      file = e.loadFileAsync("load()_", file, mp.oPt, true);
      // A ScriptInterrupt will be thrown, and an asynchronous
      // file load will initiate, which will return to the script 
      // at this command when the load operation has completed.
      // Note that we need to have just a simple command here.
      // The evaluation will be repeated up to this point, so for example,
      // x = (i++) + load("?") would increment i twice.
    }
    String str = isFile ? vwr.fm.getFilePath(file, false, false)
        : vwr.getFileAsString4(file, nBytesMax, false, false, true, "script");
    try {
      return (asJSON ? mp.addXObj(vwr.parseJSON(str)) : mp.addXStr(str));
    } catch (Exception e) {
      return false;
    }
  }

  private boolean evaluateMath(ScriptMathProcessor mp, SV[] args, int tok) {
    double x = SV.dValue(args[0]);
    switch (tok) {
    case T.sqrt:
      x = Math.sqrt(x);
      break;
    case T.sin:
      x = Math.sin(x * Math.PI / 180);
      break;
    case T.cos:
      x = Math.cos(x * Math.PI / 180);
      break;
    case T.tan:
      x = Math.tan(x * Math.PI / 180);
      break;
    case T.acos:
      x = Math.acos(x) * 180 / Math.PI;
      break;
    }
    return mp.addXDouble(x);
  }

  //  private boolean evaluateVolume(ScriptVariable[] args) throws ScriptException {
  //    ScriptVariable x1 = mp.getX();
  //    if (x1.tok != Token.bitset)
  //      return false;
  //    String type = (args.length == 0 ? null : ScriptVariable.sValue(args[0]));
  //    return mp.addX(vwr.getVolume((BitSet) x1.value, type));
  //  }

  private boolean evaluateMeasure(ScriptMathProcessor mp, SV[] args, int tok)
      throws ScriptException {
    int nPoints = 0;
    switch (tok) {
    case T.measure:
      // note: min/max are always in Angstroms
      // note: order is not important (other than min/max)
      // measure({a},{b},{c},{d}, min, max, property, format, units)
      // measure({a},{b},{c}, min, max, property, format, units)
      // measure({a},{b}, min, max, property, format, units)
      // measure({a},{b},{c},{d}, min, max, property, format, units)
      // measure({a} {b} "minArray") -- returns array of minimum distance values
      String property = null;
      Lst<Object> points = new Lst<Object>();
      double[] rangeMinMax = new double[] { Double.MAX_VALUE,
          Double.MAX_VALUE };
      String strFormat = null;
      String units = null;
      boolean isAllConnected = false;
      boolean isNotConnected = false;
      int rPt = 0;
      boolean isNull = false;
      RadiusData rd = null;
      int nBitSets = 0;
      double vdw = Double.MAX_VALUE;
      boolean asMinArray = false;
      boolean asFloatArray = false;
      for (int i = 0; i < args.length; i++) {
        switch (args[i].tok) {
        case T.bitset:
          BS bs = (BS) args[i].value;
          if (bs.length() == 0)
            isNull = true;
          points.addLast(bs);
          nPoints++;
          nBitSets++;
          break;
        case T.point3f:
          Point3fi v = new Point3fi();
          v.setT((P3d) args[i].value);
          points.addLast(v);
          nPoints++;
          break;
        case T.integer:
        case T.decimal:
          rangeMinMax[rPt++ % 2] = SV.dValue(args[i]);
          break;

        case T.string:
          String s = SV.sValue(args[i]);
          if (s.startsWith("property_")) {
            property = s;
            break;
          }
          if (s.equalsIgnoreCase("vdw") || s.equalsIgnoreCase("vanderwaals"))
            vdw = (i + 1 < args.length && args[i + 1].tok == T.integer
                ? args[++i].asInt()
                : 100) / 100d;
          else if (s.equalsIgnoreCase("notConnected"))
            isNotConnected = true;
          else if (s.equalsIgnoreCase("connected"))
            isAllConnected = true;
          else if (s.equalsIgnoreCase("minArray"))
            asMinArray = (nBitSets >= 1);
          else if (s.equalsIgnoreCase("asArray") || s.length() == 0)
            asFloatArray = (nBitSets >= 1);
          else if (Measurement.isUnits(s))
            units = s.toLowerCase();
          else
            strFormat = nPoints + ":" + s;
          break;
        default:
          return false;
        }
      }
      if (nPoints < 2 || nPoints > 4 || rPt > 2
          || isNotConnected && isAllConnected)
        return false;
      if (isNull)
        return mp.addXStr("");
      if (vdw != Double.MAX_VALUE && (nBitSets != 2 || nPoints != 2))
        return mp.addXStr("");
      rd = (vdw == Double.MAX_VALUE ? new RadiusData(rangeMinMax, 0, null, null)
          : new RadiusData(null, vdw, EnumType.FACTOR, VDW.AUTO));
      Object obj = (vwr.newMeasurementData(null, points))
          .set(0, null, rd, property, strFormat, units, null, isAllConnected,
              isNotConnected, null, true, 0, (short) 0, null, Double.NaN, null)
          .getMeasurements(asFloatArray, asMinArray);
      return mp.addXObj(obj);
    case T.angle:
      if ((nPoints = args.length) != 3 && nPoints != 4)
        return false;
      break;
    default: // distance
      if ((nPoints = args.length) != 2)
        return false;
    }
    P3d[] pts = new P3d[nPoints];
    for (int i = 0; i < nPoints; i++) {
      if ((pts[i] = mp.ptValue(args[i], null)) == null)
        return false;
    }
    switch (nPoints) {
    case 2:
      return mp.addXDouble(pts[0].distance(pts[1]));
    case 3:
      return mp
          .addXDouble(MeasureD.computeAngleABC(pts[0], pts[1], pts[2], true));
    case 4:
      return mp.addXDouble(
          MeasureD.computeTorsion(pts[0], pts[1], pts[2], pts[3], true));
    }
    return false;
  }

  private boolean evaluateModulation(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {
    // modulation(t456)
    // modulation(type)
    // modulation(type, t)
    // where type is a string starting with case-insensitive D, O, or M
    // (displacement, occupation, magnetic)

    String type = "";
    double t = Double.NaN;
    P3d t456 = null;
    switch (args.length) {
    case 0:
      break;
    case 1:
      switch (args[0].tok) {
      case T.point3f:
        t456 = (P3d) args[0].value;
        break;
      case T.string:
        type = args[0].asString();
        break;
      default:
        t = SV.dValue(args[0]);
      }
      break;
    case 2:
      type = SV.sValue(args[0]);
      t = SV.dValue(args[1]);
      break;
    default:
      return false;
    }
    if (t456 == null && t < 1e6)
      t456 = P3d.new3(t, t, t);
    SV x = mp.getX();
    BS bs = (x.tok == T.bitset ? (BS) x.value : new BS());
    return mp.addXList(vwr.ms.getModulationList(bs,
        (type + "D").toUpperCase().charAt(0), t456));
  }

  /**
   * plane() or intersection() or hkl()
   * 
   * @param mp
   * @param args
   * @param tok
   * @param isSelector
   * @return true
   * @throws ScriptException
   */
  private boolean evaluatePlane(ScriptMathProcessor mp, SV[] args, int tok, boolean isSelector)
      throws ScriptException {
    if (tok == T.hkl && args.length != 3 && args.length != 4
        || tok == T.intersection && args.length != 2 && args.length != 3
            && args.length != 4
        || args.length == 0 || args.length > 4)
      return false;
    P3d pt1, pt2, pt3;
    P4d plane = e.planeValue(args[0]);
    V3d norm, vTemp;
    switch (args.length) {
    case 1:
      if (tok == T.plane) {
        if (args[0].tok != T.varray)
          return false;
        double[] uvw = SV.dlistValue(args[0], 3);
        if (uvw.length != 3)
          return false;
        SymmetryInterface sym = vwr.getOperativeSymmetry();
        if (sym == null)
          return false;
        P3d ptuvw = P3d.new3(uvw[0], uvw[1], uvw[2]);
        sym.toCartesian(ptuvw, false);
        plane = P4d.new4(ptuvw.x, ptuvw.y, ptuvw.z, 0);
        if (isSelector) {
          break;
        }
      }
      return (plane != null && mp.addXPt4(plane));
    case 2:
      if (tok == T.intersection) {
        // intersection(plane, plane)
        // intersection(point, plane)
        P4d plane1 = e.planeValue(args[1]);
        if (plane1 == null)
          return false;
        pt3 = new P3d();
        norm = new V3d();
        vTemp = new V3d();

        if (plane != null) {
          Lst<Object> list = MeasureD.getIntersectionPP(plane, plane1);
          if (list == null)
            return mp.addXStr("");
          return mp.addXList(list);
        }
        pt2 = mp.ptValue(args[0], null);
        if (pt2 == null)
          return mp.addXStr("");
        return mp.addXPt(
            MeasureD.getIntersection(pt2, null, plane1, pt3, norm, vTemp));
      }
      //$FALL-THROUGH$
    case 3:
    case 4:
      switch (tok) {
      case T.hkl:
        // hkl(i,j,k)
        double offset = (args.length == 4 ? SV.dValue(args[3]) : Double.NaN);
        plane = e.getHklPlane(P3d.new3(SV.dValue(args[0]), SV.dValue(args[1]),
            SV.dValue(args[2])), offset, null);
        if (isSelector) {
          break;
        }
        return plane != null && mp.addXPt4(plane);
      case T.intersection:
        pt1 = mp.ptValue(args[0], null);
        pt2 = mp.ptValue(args[1], null);
        if (pt1 == null || pt2 == null)
          return mp.addXStr("");
        V3d vLine = V3d.newV(pt2);
        vLine.normalize();
        P4d plane2 = e.planeValue(args[2]);
        if (plane2 != null) {
          // intersection(ptLine, vLine, plane)
          pt3 = new P3d();
          norm = new V3d();
          vTemp = new V3d();
          pt1 = MeasureD.getIntersection(pt1, vLine, plane2, pt3, norm, vTemp);
          if (pt1 == null)
            return mp.addXStr("");
          return mp.addXPt(pt1);
        }
        pt3 = mp.ptValue(args[2], null);
        if (pt3 == null)
          return mp.addXStr("");
        // intersection(ptLine, vLine, ptCenter, radius)
        // intersection(ptLine, vLine, pt2); 
        // IE intersection of plane perp to line through pt2
        V3d v = new V3d();
        pt3 = P3d.newP(pt3);
        if (args.length == 3) {
          // intersection(ptLine, vLine, pt2); 
          // IE intersection of plane perp to line through pt2
          MeasureD.projectOntoAxis(pt3, pt1, vLine, v);
          return mp.addXPt(pt3);
        }
        // intersection(ptLine, vLine, ptCenter, radius)
        // IE intersection of a line with a sphere -- return list of 0, 1, or 2 points
        double r = SV.dValue(args[3]);
        P3d ptCenter = P3d.newP(pt3);
        MeasureD.projectOntoAxis(pt3, pt1, vLine, v);
        double d = ptCenter.distance(pt3);
        Lst<P3d> l = new Lst<P3d>();
        if (d == r) {
          l.addLast(pt3);
        } else if (d < r) {
          d = Math.sqrt(r * r - d * d);
          v.scaleAdd2(d, vLine, pt3);
          l.addLast(P3d.newP(v));
          v.scaleAdd2(-d, vLine, pt3);
          l.addLast(P3d.newP(v));
        }
        return mp.addXList(l);
      }
      switch (args[0].tok) {
      case T.integer:
      case T.decimal:
        if (args.length == 3) {
          // plane(r theta phi)
          double r = SV.dValue(args[0]);
          double theta = SV.dValue(args[1]); // longitude, azimuthal, in xy plane
          double phi = SV.dValue(args[2]); // 90 - latitude, polar, from z
          // rotate {0 0 r} about y axis need to stay in the x-z plane
          norm = V3d.new3(0, 0, 1);
          pt2 = P3d.new3(0, 1, 0);
          Qd q = Qd.newVA(pt2, phi);
          q.getMatrix().rotate(norm);
          // rotate that vector around z
          pt2.set(0, 0, 1);
          q = Qd.newVA(pt2, theta);
          q.getMatrix().rotate(norm);
          pt2.setT(norm);
          pt2.scale(r);
          plane = new P4d();
          MeasureD.getPlaneThroughPoint(pt2, norm, plane);
          return mp.addXPt4(plane);
        }
        break;
      case T.bitset:
      case T.point3f:
        pt1 = mp.ptValue(args[0], null);
        pt2 = mp.ptValue(args[1], null);
        if (pt2 == null)
          return false;
        pt3 = (args.length > 2
            && (args[2].tok == T.bitset || args[2].tok == T.point3f)
                ? mp.ptValue(args[2], null)
                : null);
        norm = V3d.newV(pt2);
        if (pt3 == null) {
          plane = new P4d();
          if (args.length == 2 || args[2].tok != T.integer
              && args[2].tok != T.decimal && !args[2].asBoolean()) {
            // plane(<point1>,<point2>) or 
            // plane(<point1>,<point2>,false)
            pt3 = P3d.newP(pt1);
            pt3.add(pt2);
            pt3.scale(0.5d);
            norm.sub(pt1);
            norm.normalize();
          } else if (args[2].tok == T.on) {
            // plane(<point1>,<vLine>,true)
            pt3 = pt1;
          } else {
            // plane(<point1>,<point2>,f)
            norm.sub(pt1);
            pt3 = new P3d();
            pt3.scaleAdd2(args[2].asDouble(), norm, pt1);
          }
          MeasureD.getPlaneThroughPoint(pt3, norm, plane);
          return mp.addXPt4(plane);
        }
        // plane(<point1>,<point2>,<point3>)
        // plane(<point1>,<point2>,<point3>,<pointref>)
        V3d vAB = new V3d();
        P3d ptref = (args.length == 4 ? mp.ptValue(args[3], null) : null);
        double nd = MeasureD.getDirectedNormalThroughPoints(pt1, pt2, pt3,
            ptref, norm, vAB);
        return mp.addXPt4(P4d.new4(norm.x, norm.y, norm.z, nd));
      }
    }
    if (isSelector) {
        // {*}.hkl(
        SV x1 = mp.getX();
        switch (x1.tok) {
        case T.bitset:
          Lst<SV> list = new Lst<>();
          BS bsAtoms = SV.getBitSet(x1, false);
          for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
            P3d p = new P3d(); // will be filled
            MeasureD.getPlaneProjection(vwr.ms.at[i], plane, p, new V3d());
            list.addLast(SV.getVariable(p));
          }
          return mp.addXList(list);
        case T.point3f:
          P3d pt = SV.ptValue(x1);
          P3d p = new P3d(); // will be filled
          MeasureD.getPlaneProjection(pt, plane, p, new V3d());            
          return mp.addXPt(p);
        }
    }
    if (args.length != 4)
      return false;
    double x = SV.dValue(args[0]);
    double y = SV.dValue(args[1]);
    double z = SV.dValue(args[2]);
    double w = SV.dValue(args[3]);
    return mp.addXPt4(P4d.new4(x, y, z, w));
  }

  private boolean evaluatePoint(ScriptMathProcessor mp, SV[] args) {
    // point(1.3)  // rounds toward 0
    // point(pt, true) // to screen coord 
    // point(pt, false) // from screen coord
    // point(x, y, z)
    // point(x, y, z, w)
    // point(["{1,2,3}", "{2,3,4}"])
    // point([[1,2,3], [2,3,4]])
    // point("(1/2,1/2,1/2)")

    String s = null;
    switch (args.length) {
    default:
      return false;
    case 1:
      switch (args[0].tok) {
      case T.decimal:
      case T.integer:
        return mp.addXInt(args[0].asInt());
      case T.string:
        s = (String) args[0].value;
        if (s.startsWith("(") && s.charAt(s.length() - 1) == ')') {
          // from .rxyz -- this is fractional but 1/2, for example
          return mp.addXPt(getRealPointFromFraction(s));
        }
        break;
      case T.varray:
        Lst<SV> list = args[0].getList();
        int len = list.size();
        if (len == 0) {
          return false;
        }
        switch (list.get(0).tok) {
        case T.integer:
        case T.decimal:
          break;
        case T.varray:
          Lst<SV> ar = new Lst<SV>();
          for (int i = 0; i < len; i++) {
            ar.addLast(SV.getVariable(Escape.uP("{" + list.get(i).asString() + "}")));
          }
          return mp.addXList(ar);          
        case T.string:
          s = (String) list.get(0).value;
          if (!s.startsWith("{") || Escape.uP(s) instanceof String) {
            s = null;
            break;
          }
          Lst<SV> a = new Lst<SV>();
          for (int i = 0; i < len; i++) {
            a.addLast(SV.getVariable(Escape.uP(SV.sValue(list.get(i)))));
          }
          return mp.addXList(a);
        default:
          return false;
        }
        s = "{" + s + "}";
      }
      if (s == null)
        s = SV.sValue(args[0]);
      Object pt = Escape.uP(s);
      return (pt instanceof P3d ? mp.addXPt((P3d) pt) : mp.addXStr("" + pt));
    case 2:
      P3d pt3;
      switch (args[1].tok) {
      case T.off:
      case T.on:
        // to/from screen coordinates
        switch (args[0].tok) {
        case T.point3f:
          pt3 = P3d.newP((T3d) args[0].value);
          break;
        case T.bitset:
          pt3 = vwr.ms.getAtomSetCenter((BS) args[0].value);
          break;
        default:
          return false;
        }
        if (args[1].tok == T.on) {
          // this is TO screen coordinates, 0 at bottom left
          vwr.tm.transformPt3f(pt3, pt3);
          pt3.y = vwr.tm.height - pt3.y;
          if (vwr.antialiased)
            pt3.scale(0.5d);
        } else {
          // this is FROM screen coordinates
          if (vwr.antialiased)
            pt3.scale(2d);
          pt3.y = vwr.tm.height - pt3.y;
          vwr.tm.unTransformPoint(pt3, pt3);
        }
        break;
      case T.point3f:
        // unitcell transform
        Lst<SV> sv = args[0].getList();
        if (sv == null || sv.size() != 4)
          return false;
        P3d pt1 = SV.ptValue(args[1]);
        pt3 = P3d.newP(SV.ptValue(sv.get(0)));
        pt3.scaleAdd2(pt1.x, SV.ptValue(sv.get(1)), pt3);
        pt3.scaleAdd2(pt1.y, SV.ptValue(sv.get(2)), pt3);
        pt3.scaleAdd2(pt1.z, SV.ptValue(sv.get(3)), pt3);
        break;
      default:
        return false;
      }
      return mp.addXPt(pt3);
    case 3:
      return mp.addXPt(
          P3d.new3(args[0].asDouble(), args[1].asDouble(), args[2].asDouble()));
    case 4:
      return mp.addXPt4(P4d.new4(args[0].asDouble(), args[1].asDouble(),
          args[2].asDouble(), args[3].asDouble()));
    }
  }

  /**
   * Assumes (.....)
   * @param s
   * @return real point
   */
  private P3d getRealPointFromFraction(String s) {
    s = PT.rep(s.substring(1, s.length()-1).replace(',', ' '),"  "," ");
    String[] xyz = PT.split(s.trim(), " ");
    if (xyz.length != 3)
      return null;
    P3d pt = P3d.new3(
        SimpleUnitCell.parseCalcFunctions(vwr, null, xyz[0]),
        SimpleUnitCell.parseCalcFunctions(vwr, null, xyz[1]),
        SimpleUnitCell.parseCalcFunctions(vwr, null, xyz[2])
        );
    return pt;
  }

  private boolean evaluatePrompt(ScriptMathProcessor mp, SV[] args) {
    //x = prompt("testing")
    //x = prompt("testing","defaultInput")
    //x = prompt("testing","yes|no|cancel", true)
    //x = prompt("testing",["button1", "button2", "button3"])

    if (args.length != 1 && args.length != 2 && args.length != 3)
      return false;
    String label = SV.sValue(args[0]);
    String[] buttonArray = (args.length > 1 && args[1].tok == T.varray
        ? SV.strListValue(args[1])
        : null);
    boolean asButtons = (buttonArray != null || args.length == 1
        || args.length == 3 && args[2].asBoolean());
    String input = (buttonArray != null ? null
        : args.length >= 2 ? SV.sValue(args[1]) : "OK");
    String s = "" + vwr.prompt(label, input, buttonArray, asButtons);
    return (asButtons && buttonArray != null
        ? mp.addXInt(Integer.parseInt(s) + 1)
        : mp.addXStr(s));
  }

  private boolean evaluateQuaternion(ScriptMathProcessor mp, SV[] args, int tok)
      throws ScriptException {
    P3d pt0 = null;
    // quaternion([quaternion array]) // mean
    // quaternion([quaternion array1], [quaternion array2], "relative") //
    // difference array
    // quaternion(matrix)
    // quaternion({atom1}) // quaternion (1st if array)
    // quaternion({atomSet}, nMax) // nMax quaternions, by group; 0 for all
    // quaternion({atom1}, {atom2}) // difference 
    // quaternion({atomSet1}, {atomset2}, nMax) // difference array, by group; 0 for all
    // quaternion(vector, theta)
    // quaternion(q0, q1, q2, q3)
    // quaternion("{x, y, z, w"})
    // quaternion("best")
    // quaternion(center, X, XY)
    // quaternion(mcol1, mcol2)
    // quaternion(q, "id", center) // draw code
    // axisangle(vector, theta)
    // axisangle(x, y, z, theta)
    // axisangle("{x, y, z, theta"})
    int nArgs = args.length;
    int nMax = Integer.MAX_VALUE;
    boolean isRelative = false;
    if (tok == T.quaternion) {
      if (nArgs > 1 && args[nArgs - 1].tok == T.string
          && ((String) args[nArgs - 1].value).equalsIgnoreCase("relative")) {
        nArgs--;
        isRelative = true;
      }
      if (nArgs > 1 && args[nArgs - 1].tok == T.integer
          && args[0].tok == T.bitset) {
        nMax = args[nArgs - 1].asInt();
        if (nMax <= 0)
          nMax = Integer.MAX_VALUE - 1;
        nArgs--;
      }
    }

    switch (nArgs) {
    case 0:
    case 1:
    case 4:
      break;
    case 2:
      if (tok == T.quaternion) {
        if (args[0].tok == T.varray
            && (args[1].tok == T.varray || args[1].tok == T.on))
          break;
        if (args[0].tok == T.bitset
            && (args[1].tok == T.integer || args[1].tok == T.bitset))
          break;
      }
      if ((pt0 = mp.ptValue(args[0], null)) == null
          || tok != T.quaternion && args[1].tok == T.point3f)
        return false;
      break;
    case 3:
      if (tok != T.quaternion)
        return false;
      if (args[0].tok == T.point4f) {
        if (args[2].tok != T.point3f && args[2].tok != T.bitset)
          return false;
        break;
      }
      for (int i = 0; i < 3; i++)
        if (args[i].tok != T.point3f && args[i].tok != T.bitset)
          return false;
      break;
    default:
      return false;
    }
    Qd q = null;
    Qd[] qs = null;
    P4d p4 = null;
    switch (nArgs) {
    case 0:
      return mp.addXPt4(vwr.tm.getRotationQ().toP4d());
    case 1:
    default:
      if (tok == T.quaternion && args[0].tok == T.varray) {
        Qd[] data1 = e.getQuaternionArray(args[0].getList(), T.list);
        Object mean = Qd.sphereMean(data1, null, 0.0001f);
        q = (mean instanceof Qd ? (Qd) mean : null);
        break;
      } else if (tok == T.quaternion && args[0].tok == T.bitset) {
        qs = vwr.getAtomGroupQuaternions((BS) args[0].value, nMax);
      } else if (args[0].tok == T.matrix3f) {
        q = Qd.newM((M3d) args[0].value);
      } else if (args[0].tok == T.point4f) {
        p4 = (P4d) args[0].value;
      } else {
        String s = SV.sValue(args[0]);
        Object v = Escape.uP(s.equalsIgnoreCase("best")
            ? vwr.getOrientation(T.best, "best", null, null).toString()
            : s);
        if (!(v instanceof P4d))
          return false;
        p4 = (P4d) v;
      }
      if (tok == T.axisangle)
        q = Qd.newVA(P3d.new3(p4.x, p4.y, p4.z), p4.w);
      break;
    case 2:
      if (tok == T.quaternion) {
        if (args[0].tok == T.varray && args[1].tok == T.varray) {
          Qd[] data1 = e.getQuaternionArray(args[0].getList(), T.list);
          Qd[] data2 = e.getQuaternionArray(args[1].getList(), T.list);
          qs = Qd.div(data2, data1, nMax, isRelative);
          break;
        }
        if (args[0].tok == T.varray && args[1].tok == T.on) {
          Qd[] data1 = e.getQuaternionArray(args[0].getList(), T.list);
          double[] stddev = new double[1];
          Qd.sphereMean(data1, stddev, 0.0001f);
          return mp.addXDouble(stddev[0]);
        }
        if (args[0].tok == T.bitset && args[1].tok == T.bitset) {
          Qd[] data1 = vwr.getAtomGroupQuaternions((BS) args[0].value,
              Integer.MAX_VALUE);
          Qd[] data2 = vwr.getAtomGroupQuaternions((BS) args[1].value,
              Integer.MAX_VALUE);
          qs = Qd.div(data2, data1, nMax, isRelative);
          break;
        }
      }
      P3d pt1 = mp.ptValue(args[1], null);
      p4 = e.planeValue(args[0]);
      if (pt1 != null)
        q = Qd.getQuaternionFrame(P3d.new3(0, 0, 0), pt0, pt1);
      else
        q = Qd.newVA(pt0, SV.dValue(args[1]));
      break;
    case 3:
      if (args[0].tok == T.point4f) {
        P3d pt = (args[2].tok == T.point3f ? (P3d) args[2].value
            : vwr.ms.getAtomSetCenter((BS) args[2].value));
        return mp.addXStr(Escape.drawQuat(Qd.newP4((P4d) args[0].value), "q",
            SV.sValue(args[1]), pt, 1d));
      }
      P3d[] pts = new P3d[3];
      for (int i = 0; i < 3; i++)
        pts[i] = (args[i].tok == T.point3f ? (P3d) args[i].value
            : vwr.ms.getAtomSetCenter((BS) args[i].value));
      q = Qd.getQuaternionFrame(pts[0], pts[1], pts[2]);
      break;
    case 4:
      if (tok == T.quaternion)
        p4 = P4d.new4(SV.dValue(args[1]), SV.dValue(args[2]),
            SV.dValue(args[3]), SV.dValue(args[0]));
      else
        q = Qd.newVA(P3d.new3(SV.dValue(args[0]), SV.dValue(args[1]),
            SV.dValue(args[2])), SV.dValue(args[3]));
      break;
    }
    if (qs != null) {
      if (nMax != Integer.MAX_VALUE) {
        Lst<P4d> list = new Lst<P4d>();
        for (int i = 0; i < qs.length; i++)
          list.addLast(qs[i].toP4d());
        return mp.addXList(list);
      }
      q = (qs.length > 0 ? qs[0] : null);
    }
    return mp.addXPt4((q == null ? Qd.newP4(p4) : q).toP4d());
  }

  private Random rand;

  private boolean evaluateRandom(ScriptMathProcessor mp, SV[] args) {
    if (args.length > 3)
      return false;
    if (rand == null)
      rand = new Random();
    double lower = 0, upper = 1;
    switch (args.length) {
    case 3:
      rand.setSeed((int) SV.dValue(args[2]));
      //$FALL-THROUGH$
    case 2:
      upper = SV.dValue(args[1]);
      //$FALL-THROUGH$
    case 1:
      lower = SV.dValue(args[0]);
      //$FALL-THROUGH$
    case 0:
      break;
    default:
      return false;
    }
    return mp.addXDouble((rand.nextFloat() * (upper - lower)) + lower);
  }

  private boolean evaluateRowCol(ScriptMathProcessor mp, SV[] args, int tok)
      throws ScriptException {
    if (args.length != 1)
      return false;
    int n = args[0].asInt() - 1;
    SV x1 = mp.getX();
    double[] f;
    switch (x1.tok) {
    case T.matrix3f:
      if (n < 0 || n > 2)
        return false;
      M3d m = (M3d) x1.value;
      switch (tok) {
      case T.row:
        f = new double[3];
        m.getRow(n, f);
        return mp.addXAD(f);
      case T.col:
      default:
        f = new double[3];
        m.getColumn(n, f);
        return mp.addXAD(f);
      }
    case T.matrix4f:
      if (n < 0 || n > 2)
        return false;
      M4d m4 = (M4d) x1.value;
      switch (tok) {
      case T.row:
        f = new double[4];
        m4.getRow(n, f);
        return mp.addXAD(f);
      case T.col:
      default:
        f = new double[4];
        m4.getColumn(n, f);
        return mp.addXAD(f);
      }
    case T.varray:
      // column of a[][]
      Lst<SV> l1 = x1.getList();
      Lst<SV> l2 = new Lst<SV>();
      for (int i = 0, len = l1.size(); i < len; i++) {
        Lst<SV> l3 = l1.get(i).getList();
        if (l3 == null)
          return mp.addXStr("");
        l2.addLast(n < l3.size() ? l3.get(n) : SV.newS(""));
      }
      return mp.addXList(l2);
    }
    return false;

  }

  private boolean evaluateIn(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {
    SV x1 = mp.getX();
    switch (args.length) {
    case 1:
      Lst<SV> lst = args[0].getList();
      if (lst != null)
        for (int i = 0, n = lst.size(); i < n; i++)
          if (SV.areEqual(x1, lst.get(i)))
            return mp.addXInt(i + 1);
      break;
    default:
      for (int i = 0; i < args.length; i++)
        if (SV.areEqual(x1, args[i]))
          return mp.addXInt(i + 1);
      break;
    }
    return mp.addXInt(0);
  }

  private boolean evaluateReplace(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {
    boolean isAll = false;
    String sFind, sReplace;
    switch (args.length) {
    case 0:
      isAll = true;
      sFind = sReplace = null;
      break;
    case 3:
      isAll = SV.bValue(args[2]);
      //$FALL-THROUGH$
    case 2:
      sFind = SV.sValue(args[0]);
      sReplace = SV.sValue(args[1]);
      break;
    default:
      return false;
    }
    SV x = mp.getX();
    if (x.tok == T.varray) {
      String[] list = SV.strListValue(x);
      String[] l = new String[list.length];
      for (int i = list.length; --i >= 0;)
        l[i] = (sFind == null ? PT.clean(list[i])
            : isAll ? PT.replaceAllCharacters(list[i], sFind, sReplace)
                : PT.rep(list[i], sFind, sReplace));
      return mp.addXAS(l);
    }
    String s = SV.sValue(x);
    return mp.addXStr(sFind == null ? PT.clean(s)
        : isAll ? PT.replaceAllCharacters(s, sFind, sReplace)
            : PT.rep(s, sFind, sReplace));
  }

  private boolean evaluateScript(ScriptMathProcessor mp, SV[] args, int tok)
      throws ScriptException {
    // eval(cmd)
    // eval("JSON",json)
    // javascript(cmd)
    // script(cmd)
    // script(cmd, syncTarget)
    // show(showCmd)
    if (tok != T.show && !checkAccess())
      return false; // fail for PNGJ
    if (args.length == 0 || args.length != 1 && (tok == T.show || tok == T.javascript))
      return false;
    if ((tok == T.show || tok == T.javascript) && args.length != 1
        || args.length == 0)
      return false;
    String s = SV.sValue(args[0]);
    SB sb = new SB();
    switch (tok) {
    case T.eval:
      return (args.length == 2
          ? s.equalsIgnoreCase("JSON")
              && mp.addXObj(vwr.parseJSON(SV.sValue(args[1])))
          : mp.addXObj(vwr.evaluateExpressionAsVariable(s)));
    case T.script:
      String appID = (args.length == 2 ? SV.sValue(args[1]) : ".");
      // options include * > . or an appletID with or without "jmolApplet"
      if (!appID.equals("."))
        sb.append(vwr.jsEval(appID + "\1" + s));
      if (appID.equals(".") || appID.equals("*"))
        e.runScriptBuffer(s, sb, true);
      break;
    case T.show:
      e.runScriptBuffer("show " + s, sb, true);
      break;
    case T.javascript:
      return mp.addX(vwr.jsEvalSV(s));
    }
    s = sb.toString();
    double f;
    return (Double.isNaN(f = PT.parseDoubleStrict(s)) ? mp.addXStr(s)
        : s.indexOf(".") >= 0 ? mp.addXDouble(f) : mp.addXInt(PT.parseInt(s)));
  }

  private boolean checkAccess() {
    return !vwr.haveAccessInternal(null);
  }

  /**
   * sort() or sort(n) or count() or count("xxxx")
   * 
   * @param mp
   * @param args
   * @param tok
   * @return true if no error
   * @throws ScriptException
   */
  private boolean evaluateSort(ScriptMathProcessor mp, SV[] args, int tok)
      throws ScriptException {
    if (args.length > 1)
      return false;
    if (tok == T.sort) {
      if (args.length == 1 && args[0].tok == T.string) {
        return mp.addX(mp.getX().sortMapArray(args[0].asString()));
      }
      int n = (args.length == 0 ? 0 : args[0].asInt());
      return mp.addX(mp.getX().sortOrReverse(n));
    }
    SV x = mp.getX();
    SV match = (args.length == 0 ? null : args[0]);
    if (x.tok == T.string) {
      int n = 0;
      String s = SV.sValue(x);
      if (match == null)
        return mp.addXInt(0);
      String m = SV.sValue(match);
      for (int i = 0; i < s.length(); i++) {
        int pt = s.indexOf(m, i);
        if (pt < 0)
          break;
        n++;
        i = pt;
      }
      return mp.addXInt(n);
    }
    Lst<SV> counts = new Lst<SV>();
    SV last = null;
    SV count = null;
    Lst<SV> xList = SV.getVariable(x.value).sortOrReverse(0).getList();
    if (xList == null)
      return (match == null ? mp.addXStr("") : mp.addXInt(0));
    for (int i = 0, nLast = xList.size(); i <= nLast; i++) {
      SV a = (i == nLast ? null : xList.get(i));
      if (match != null && a != null && !SV.areEqual(a, match))
        continue;
      if (SV.areEqual(a, last)) {
        count.intValue++;
        continue;
      } else if (last != null) {
        Lst<SV> y = new Lst<SV>();
        y.addLast(last);
        y.addLast(count);
        counts.addLast(SV.getVariableList(y));
      }
      count = SV.newI(1);
      last = a;
    }
    if (match == null)
      return mp.addX(SV.getVariableList(counts));
    if (counts.isEmpty())
      return mp.addXInt(0);
    return mp.addX(counts.get(0).getList().get(1));
  }

  private boolean evaluateString(ScriptMathProcessor mp, int tok, SV[] args)
      throws ScriptException {
    SV x = mp.getX();
    String sArg = (args.length > 0 ? SV.sValue(args[0])
        : tok == T.trim ? "" : "\n");
    switch (args.length) {
    case 0:
      break;
    case 1:
      if (args[0].tok == T.on) {
        return mp.addX(SV.getVariable(PT.getTokens(x.asString())));
      }
      break;
    case 2:
      if (x.tok == T.varray)
        break;
      if (tok == T.split) {
        x = SV.getVariable(PT.split(
            PT.rep((String) x.value, "\n\r", "\n").replace('\r', '\n'), "\n"));
        break;
      }
      //$FALL-THROUGH$
    default:
      return false;
    }

    if (x.tok == T.varray && tok != T.trim
        && (tok != T.split || args.length == 2)) {
      mp.addX(x);
      return evaluateList(mp, tok, args);
    }
    String s = (tok == T.split && x.tok == T.bitset
        || tok == T.trim && x.tok == T.varray ? null : SV.sValue(x));
    switch (tok) {
    case T.split:
      if (x.tok == T.bitset) {
        BS bsSelected = (BS) x.value;
        int modelCount = vwr.ms.mc;
        Lst<SV> lst = new Lst<SV>();
        for (int i = 0; i < modelCount; i++) {
          BS bs = vwr.getModelUndeletedAtomsBitSet(i);
          bs.and(bsSelected);
          lst.addLast(SV.getVariable(bs));
        }
        return mp.addXList(lst);
      }
      return mp.addXAS(PT.split(s, sArg));
    case T.join:
      if (s.length() > 0 && s.charAt(s.length() - 1) == '\n')
        s = s.substring(0, s.length() - 1);
      return mp.addXStr(PT.rep(s, "\n", sArg));
    case T.trim:
      if (s != null)
        return mp.addXStr(PT.trim(s, sArg));
      String[] list = SV.strListValue(x);
      for (int i = list.length; --i >= 0;)
        list[i] = PT.trim(list[i], sArg);
      return mp.addXAS(list);
    }
    return mp.addXStr("");
  }

  private boolean evaluateSubstructure(ScriptMathProcessor mp, SV[] args,
                                       int tok, boolean isSelector)
      throws ScriptException {
    // select substructure(....) legacy - was same as smiles(), now search()
    // select smiles(...)
    // select search(...)  now same as substructure
    // print {*}.search(...)
    // x = {*}.smiles(options)
    // x = {*}.smiles("inchi/smilesoptions") --> {*}.inchi("smiles/options")
    // compiled:
    // x = search({*})
    // y = pattern('CCCC')
    // atoms = {*}.search(y)
    // print search(x, y)
    
    SV x = (isSelector ? mp.getX() : null);
    String sx = null;
    BS bsSelected = null;
    if (x != null) {
      switch (x.tok) {
      case T.bitset:
        bsSelected = (BS) x.value;
        break;
      case T.string:
        sx = (String) x.value;
        if (sx.startsWith("InChI")) {
          mp.addX(x);
          return evaluateInChI(mp, new SV[] { SV.newS("SMILES " + (args.length > 0 ? args[0].asString() : sx.indexOf("/f") >= 0 ? "fixedh" : "amide")) });
        }
      }
    }
    boolean isSelectedSmiles = isSelector && tok == T.smiles;
    if (isSelectedSmiles) {
      // just smple {*}.smiles()
      return mp.addXObj(e.getSmilesExt().getSmilesMatches("", null, bsSelected, null, JC.SMILES_TYPE_SMILES, true, false));
    }
    if (args.length == 0 || isSelector && (tok == T.pattern || args.length > 1))
      return false;
    Object objTarget = (tok == T.search && !isSelector
        && args[0].tok == T.search ? args[0].value : null);
    if (objTarget != null && args.length < 2)
      return false;
    boolean compileSearch = (tok == T.search && !isSelector
        && args[0].tok == T.bitset);
    Object objPattern = (args[0].tok == T.pattern ? args[0].value
        : objTarget != null && args[1].tok == T.pattern ? args[1].value : null);
    if (objTarget != null && objPattern == null)
      return false;
    String pattern = (compileSearch ? null : objPattern == null ? SV.sValue(args[0]) : null);
    BS bs = new BS();
    if (pattern == null || pattern.length() > 0)
      try {
        if (compileSearch) {
          return mp.addX(
              SV.newV(T.search, vwr.getSmilesMatcher().compileSearchTarget(
                  vwr.ms.at, vwr.ms.ac, SV.getBitSet(args[0], false))));
        }
        if (objTarget != null)
          return mp.addXBs(vwr.getSmilesMatcher().getSubstructureSet(objPattern,
              objTarget, 0, null, JC.SMILES_TYPE_SMARTS));
        if (tok == T.pattern) {
          return mp.addX(SV.newV(T.pattern,
              vwr.getSmilesMatcher().compileSmartsPattern(pattern)));
        }
        if (bsSelected == null)
          bsSelected = (args.length == 2 && args[1].tok == T.bitset
              ? (BS) args[1].value
              : vwr.getModelUndeletedAtomsBitSet(-1));
        bs = vwr.getSmilesMatcher().getSubstructureSet(
            (objPattern == null ? pattern : objPattern), vwr.ms.at, vwr.ms.ac,
            bsSelected,
            (tok == T.smiles ? JC.SMILES_TYPE_SMILES : JC.SMILES_TYPE_SMARTS));
      } catch (Exception ex) {
        e.evalError(ex.getMessage(), null);
      }
    return (tok != T.pattern && mp.addXBs(bs));
  }

  @SuppressWarnings("unchecked")
  private boolean evaluateSymop(ScriptMathProcessor mp, SV[] args,
                                boolean isProperty)
      throws ScriptException {

    // In the following, "op" can be the operator number in a space group 
    // or a string representation of the operator, such as "x,-y,z"

    // options include:

    //  "axisVector", 
    //  "cartesianTranslation",
    //  "centeringVector", 
    //  "cif2", 
    //  "count",
    //  "draw", 
    //  "fractionalTranslation", 
    //  "id", 
    //  "invariant",
    //  "inversionCenter", 
    //  "label",
    //  "matrix", 
    //  "plane",
    //  "point", 
    //  "rxyz",
    //  "rotationAngle",
    //  "timeReversal", 
    //  "type",
    //  "unitTranslation", 
    //  "xyz", 
    //  "xyzCanonical",
    //  "xyzNormalized"
    //  "xyzOriginal", 

    // x = y.symop(op,atomOrPoint) 
    // Returns the point that is the result of the transformation of atomOrPoint 
    // via a crystallographic symmetry operation. The atom set y selects the unit 
    // cell and spacegroup to be used. If only one model is present, this can simply be all. 
    // Otherwise, it could be any atom or group of atoms from any model, for example 
    // {*/1.2} or {atomno=1}. The first parameter, op, is a symmetry operation. 
    // This can be the 1-based index of a symmetry operation in a file (use show spacegroup to get this listing) 
    // or a specific Jones-Faithful expression in quotes such as "x,1/2-y,z".

    // x = y.symop("invariant")
    // x = y.symop("invariant", "matrix")

    // x = y.symop(op,"label")
    // This form of the .symop() function returns a set of draw commands that describe 
    // the symmetry operation in terms of rotation axes, inversion centers, planes, and 
    // translational vectors. The draw objects will all have IDs starting with whatever 
    // is given for the label. 

    // x = y.symop(op)
    // Returns the 4x4 matrix associated with this operator.

    // x = y.symop(n,"time")
    // Returns the time reversal of a symmetry operator in a magnetic space group

    // x = y.symop(atomOrPoint, atomOrPoint)

    // x = y.symop(n, [h,k,l])
    // adds a lattice translation to the symmetry operation

    // x = y.symop(n,...,"cif2")
    // return a <symop> <translation> as nn [a b c] suitable for CIF2 inclusion

    // x= symop(matrix,...)
    // convert matrix back to xyz or other aspect

    // x = symop(.....)
    // use this model atoms

    // x = symop("wyckoffm")  -- report Wyckoff letter with multiplicity "4a"
    // x = symop("wyckoff")  -- report Wyckoff letter
  	// x = symop("wyckoff", "c") -- just one position
    // x = {atom or point}.symop("wyckoff","a") -- find first "a"-type Wyckoff position for this point
    // x = {atom}.symop("wyckoff", "coord")  -- report Wyckoff coord
	  // x = {atom}.symop("wyckoff", "coords") -- get full coordinate list
    // symop(@10, "list")
    // print symop([ops],type)
    Object o;
    if (!isProperty && args.length == 2 && args[0].tok == T.varray && args[1].tok == T.string) {
      Lst<Object> ret = new Lst<>();
      Lst<SV> list = (Lst<SV>) args[0].value;
      for (int i = 0, n = list.size(); i < n; i++) {
        o = getSymopInfo(mp, null, new SV[] {list.get(i), args[1]}, i + 1, false);
        if (o == null)
          return false;
        ret.addLast(o);
      }
      return mp.addXList(ret);
    }
    SV x1 = (isProperty ? mp.getX() : null);
    o = getSymopInfo(mp, x1, args, 0, isProperty);
    return mp.addXObj(o);
  }

  @SuppressWarnings("unchecked")
  private Object getSymopInfo(ScriptMathProcessor mp, SV x1, SV[] args,
                              int index, boolean isProperty)
      throws ScriptException {

    // static calls in SymmetryOperation
    Object o = null;

    int narg = args.length;
    String str1 = (narg == 2 && args[1].tok == T.string
        ? ((String) args[1].value).toLowerCase()
        : null);
    boolean isrxyz = "rxyz".equals(str1);
    if (str1 != null) {
      M4d m = null;
      String xyz = null;
      switch (args[0].tok) {
      case T.string:
        switch (str1) {
        case "rxyz":
        case "matrix":
          xyz = (String) args[0].value;
        }
        break;
      case T.matrix4f:
        switch (str1) {
        case "rxyz":
        case "xyz":
          m = (M4d) args[0].value;
          break;
        }
        break;
      }
      if (m != null || xyz != null)
        return vwr.getSymStatic().staticConvertOperation(xyz, m,
            isrxyz ? "rxyz" : null);
    }

    boolean isPoint = false;
    if (x1 != null && x1.tok != T.bitset && !(isPoint = (x1.tok == T.point3f)))
      return null;
    BS bsAtoms = (x1 == null || isPoint ? null : (BS) x1.value);
    P3d pt1 = (isPoint ? SV.ptValue(x1) : null);
    if (!isPoint && bsAtoms == null)
      bsAtoms = vwr.getThisModelAtoms();
    if (narg == 0) {
      String[] ops = PT.split(PT.trim((String) vwr.getSymTemp()
          .getSpaceGroupInfo(vwr.ms, null,
              (bsAtoms == null || bsAtoms.isEmpty() ? Math.max(0, vwr.am.cmi)
                  : vwr.ms.at[bsAtoms.nextSetBit(0)].mi),
              false, null)
          .get("symmetryInfo"), "\n"), "\n");
      Lst<String[]> lst = new Lst<String[]>();
      for (int i = 0, n = ops.length; i < n; i++)
        lst.addLast(PT.split(ops[i], "\t"));
      return lst;
    }
    String xyz = null;
    int tok = 0;
    int iOp = Integer.MIN_VALUE;  
    int apt = 0;
    P3d pt2 = null;
    BS bs1 = null, bs2 = null;
    boolean isWyckoff = false;
    boolean haveAtom2 = (narg > 1 && args[1].tok == T.bitset);    
    switch (args[0].tok) {
    case T.string:
      xyz = SV.sValue(args[0]);
      switch (xyz == null ? "" : xyz.toLowerCase()) {
      case "count":
        SymmetryInterface sym = vwr.getOperativeSymmetry();
        return (narg != 1 ? null
            : Integer
                .valueOf(sym == null ? 0 : sym.getSpaceGroupOperationCount()));
      case "":
        tok = T.nada;
        break;
      case JC.MODELKIT_INVARIANT:
        tok = T.var;
        break;
      case "wyckoff":
        tok = T.wyckoff;
        isWyckoff = true;
        break;
      case "wyckoffm":
        tok = T.wyckoffm;
        isWyckoff = true;
        break;
      }
      apt++;
      break;
    case T.matrix4f:
      xyz = args[0].escape();
      apt++;
      break;
    case T.integer:
      iOp = args[0].asInt();
      apt++;
      break;
    case T.bitset:
      if (!isPoint) {
        // @x.symop(@y, @z,....)
        // symop(@y,....) // basis
        // note that this:
        // @x.symop(@y,....)
        // ONLY SETS that model using @x; @y is still the first atom

        bs1 = (BS) args[0].value;
      }
      break;
    }
    
    if (bsAtoms == null) {
      if (apt < narg && args[apt].tok == T.bitset)
        (bsAtoms = new BS()).or((BS) args[apt].value);
      if (apt + 1 < narg && args[apt + 1].tok == T.bitset)
        (bsAtoms == null ? (bsAtoms = new BS()) : bsAtoms)
            .or((BS) args[apt + 1].value);
    } else if (!bsAtoms.isEmpty()){
      bsAtoms = vwr.getModelUndeletedAtomsBitSet(
         vwr.getModelIndexForAtom(bsAtoms.nextSetBit(0)));
    }

    // allow for [ h k l ] lattice translation
    P3d trans = null;
    if (narg > apt && args[apt].tok == T.varray) {
      List<SV> a = args[apt++].getList();
      if (a.size() != 3)
        return null;
      trans = P3d.new3(SV.dValue(a.get(0)), SV.dValue(a.get(1)),
          SV.dValue(a.get(2)));
    } else if (narg > apt && args[apt].tok == T.integer) {
      SimpleUnitCell.ijkToPoint3f(SV.iValue(args[apt++]), trans = new P3d(), 0,
          0);
    }
    if (pt1 == null
        && (pt1 = (narg > apt ? mp.ptValue(args[apt], bsAtoms) : null)) != null)
      apt++;
    if ((pt2 = (narg > apt ? mp.ptValue(args[apt], bsAtoms) : null)) != null)
      apt++;
    if (pt1 != null && pt2 == null && bs1 != null && !bs1.isEmpty()) {
      pt2 = pt1;
      // if it is symop(@x, "basis") or @x.symop(@y, "basis")
      
      // but @x.symop(@y, "cif2") also could work here. 
      
      // get the basis atom for this site
      bs1 = BSUtil.copy(bs1);
      bs1.and(bsAtoms);
      int a0 = vwr.ms.getSymmetryEquivAtoms(bs1, null, null).nextSetBit(0);
      if ("basis".equals(str1)) {
        return BSUtil.newAndSetBit(a0);
      }
      pt1 = P3d.newP(vwr.ms.at[a0]);
    }
    int nth = (pt2 != null && args.length > apt && iOp == Integer.MIN_VALUE
        && args[apt].tok == T.integer ? args[apt].intValue : -1);
    if (nth >= 0) // 0 here means "give me all of them"
      apt++;
    if (iOp == Integer.MIN_VALUE && tok != T.var)
      iOp = 0;
    Map<String, ?> map = null;
    if (tok == 0 && xyz != null && xyz.indexOf(",") < 0) {
      if (apt == narg) {
        map = vwr.ms.getPointGroupInfo(null);
      } else if (args[apt].tok == T.hash) {
        map = args[apt].getMap();
      }
    }
    if (map != null) {
      M3d m;
      // pointgroup. 
      int pt = xyz.indexOf('.');
      int p1 = xyz.indexOf('^');
      if (p1 > 0) {
        nth = PT.parseInt(xyz.substring(p1 + 1));
      } else {
        p1 = xyz.length();
        nth = 1;
      }
      if (pt > 0 && p1 > pt + 1) {
        iOp = PT.parseInt(xyz.substring(pt + 1, p1));
        if (iOp < 1)
          iOp = 1;
        p1 = pt;
      } else {
        iOp = 1;
      }
      xyz = xyz.substring(0, p1);
      o = map.get(xyz + "_m");
      if (o == null) {
        o = map.get(xyz);
        return (o == null ? "" : o);
      }
      P3d centerPt;
      try {
        if (o instanceof SV) {
          centerPt = (P3d) ((SV) map.get("center")).value;
          SV obj = (SV) o;
          if (obj.tok == T.matrix3f) {
            m = (M3d) obj.value;
          } else if (obj.tok == T.varray) {
            m = (M3d) obj.getList().get(iOp - 1).value;
          } else {
            return null;
          }
        } else {
          centerPt = (P3d) map.get("center");
          if (o instanceof M3d) {
            m = (M3d) o;
          } else {
            m = ((Lst<M3d>) o).get(iOp - 1);
          }
        }
        M3d m0 = m;
        m = M3d.newM3(m);
        if (nth > 1) {
          for (int i = 1; i < nth; i++) {
            m.mul(m0);
          }
        }
        if (pt1 == null)
          return m;
        pt1 = P3d.newP(pt1);
        pt1.sub(centerPt);
        m.rotate(pt1);
        pt1.add(centerPt);
        return pt1;
      } catch (Exception e) {
      }
      return null;
    }
    String desc = (narg == apt
        ? (isWyckoff ? ""
            : tok == T.var ? "id"
                : pt2 != null || pt1 == null ? "matrix" : "point")
        : SV.sValue(args[apt++]));
    if (narg > 2)
      isrxyz = "rxyz".equals(desc);

    boolean haveAtom = ((!isWyckoff || isProperty) && bsAtoms != null
        && !bsAtoms.isEmpty());
    int iatom = (haveAtom ? bsAtoms.nextSetBit(0) : -1);
    if (isWyckoff) {
      P3d pt = (haveAtom ? vwr.ms.getAtom(iatom) : pt1);
      while (desc.length() > 0 && PT.isDigit(desc.charAt(0)))
        desc = desc.substring(1);
      if (pt == null) {
        switch (desc) {
        case "":
        case "*":
          desc = "*";
          break;
        default:
          if (desc.length() == 1)
            desc += "*";
          else
            return null;
        }
      }
      if (desc.length() == 0 || desc.equalsIgnoreCase("label"))
        desc = null;
      String letter = (desc == null ? (tok == T.wyckoffm ? "" : null)
          : desc.endsWith("*") || desc.equalsIgnoreCase("coord")
              || desc.equalsIgnoreCase("coords") ? desc : desc.substring(0, 1));
      SymmetryInterface sym = vwr.getOperativeSymmetry();
      return (sym == null ? null
          : sym.getWyckoffPosition(pt,
              (letter == null ? (tok == T.wyckoffm ? "M" : null)
                  : (tok == T.wyckoffm ? "M" : "") + letter)));
    }
    desc = desc.toLowerCase();
    if (tok == T.var || desc.equals(JC.MODELKIT_INVARIANT) && isProperty) {
      if (haveAtom && pt1 == null)
        pt1 = vwr.ms.at[iatom];
      haveAtom = (pt1 != null);
      if (iatom < 0)
        iatom = vwr.getThisModelAtoms().nextSetBit(0);
    }

    if (tok == T.var && iOp == Integer.MIN_VALUE) {
      int[] ret = null;
      SymmetryInterface sym = vwr.getCurrentUnitCell();
      if (pt1 != null) {
        ret = (sym == null ? new int[0] : sym.getInvariantSymops(pt1, null));
      } else if (bsAtoms != null && !bsAtoms.isEmpty()) {
        int ia = bsAtoms.nextSetBit(0);
        pt1 = vwr.ms.at[ia];
        ret = vwr.ms.getSymmetryInvariant(ia);
      }
      if (ret != null && ret.length > 0) {
        Object[] m = new Object[ret.length];
        for (int i = 0; i < m.length; i++) {
          iOp = ret[i];
          m[i] = vwr.getSymmetryInfo(iatom, null, iOp, null, pt1, pt1, T.array,
              desc, 0, -1, 0, null);
        }
        return m;
      }
      return ret;
    }
    return (apt == args.length
        ? vwr.getSymmetryInfo(iatom, xyz, index > 0 ? index : iOp, trans, pt1,
            pt2, T.array, desc, 0, nth, 0, null)
        : null);
  }

  private boolean evaluateTensor(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {
    // {*}.tensor()
    // {*}.tensor("isc")            // only within this atom set
    // {atomindex=1}.tensor("isc")  // all to this atom
    // {*}.tensor("efg","eigenvalues")
    //     tensor(t,what)
    boolean isTensor = (args.length == 2 && args[1].tok == T.tensor);
    SV x = (isTensor ? null : mp.getX());
    if (args.length > 2 || !isTensor && x.tok != T.bitset)
      return false;
    BS bs = (BS) x.value;
    String tensorType = (isTensor || args.length == 0 ? null
        : SV.sValue(args[0]).toLowerCase());
    JmolNMRInterface calc = vwr.getNMRCalculation();
    if ("unique".equals(tensorType))
      return mp.addXBs(calc.getUniqueTensorSet(bs));
    String infoType = (args.length < 2 ? null
        : SV.sValue(args[1]).toLowerCase());
    if (isTensor) {
      return mp.addXObj(((Tensor) args[0].value).getInfo(infoType));
    }
    return mp.addXList(calc.getTensorInfo(tensorType, infoType, bs));
  }

  private boolean evaluateUserFunction(ScriptMathProcessor mp, String name,
                                       SV[] args, int tok, boolean isSelector)
      throws ScriptException {
    SV x1 = null;
    if (isSelector) {
      x1 = mp.getX();
      switch (x1.tok) {
      case T.bitset:
        break;
      case T.hash:
        // really xx.yyy where yyy is a function;
        if (args.length > 0)
          return false;
        x1 = x1.getMap().get(name);
        return (x1 == null ? mp.addXStr("") : mp.addX(x1));
      default:
        return false;
      }
    }
    name = name.toLowerCase();
    mp.wasX = false;
    Lst<SV> params = new Lst<SV>();
    for (int i = 0; i < args.length; i++) {
      params.addLast(args[i]);
    }
    if (isSelector) {
      return mp
          .addXObj(e.getBitsetProperty((BS) x1.value, null, tok, null, null,
              x1.value, new Object[] { name, params }, false, x1.index, false));
    }
    SV var = e.getUserFunctionResult(name, params, null);
    return (var == null ? false : mp.addX(var));
  }

  private boolean evaluateWithin(ScriptMathProcessor mp, SV[] args,
                                 boolean isAtomProperty)
      throws ScriptException {
    // within({atoms})
    // within (distance, group, {atom collection})
    // within (distance, true|false, {atom collection})
    // within (distance, plane|hkl, [plane definition] )
    // within (distance, coord, [point or atom center] )
    // within(distance, pt, [pt1, pt2, pt3...])
    // within(distance, [pt1, pt2, pt3...])
    // {atoms}.within(distance,[points])
    // {atoms}.within(distance,{otheratoms})
    // within("SMILES", "...", {atoms}) or {atoms}.within("SMILES", "...")
    // within("SMARTS", "...", {atoms}) or {atoms}.within("SMARTS", "...") // returns all sets
    // within("sequence", 
    // ...several more 
    int len = args.length;
    if (len < 1 || len > 5)
      return false;
    if (len == 1 && args[0].tok == T.bitset)
      return mp.addX(args[0]);
    BS bs = (isAtomProperty ? SV.getBitSet(mp.getX(), false) : null);
    double distance = 0;
    Object withinSpec = args[0].value;
    String withinStr = "" + withinSpec;
    ModelSet ms = vwr.ms;
    boolean isVdw = false;
    boolean isWithinModelSet = false;
    boolean isWithinGroup = false;
    boolean isDistance = false;
    BS bsSelected = null;
    RadiusData rd = null;
    int tok = args[0].tok;
    out: while (true) {
      switch (tok == T.string ? tok = T.getTokFromName(withinStr) : tok) {
      case T.vanderwaals:
        isVdw = true;
        withinSpec = null;
        //$FALL-THROUGH$
      case T.decimal:
      case T.integer:
        isDistance = true;
        if (len < 2
            || len == 3 && args[1].tok == T.varray && args[2].tok != T.varray)
          return false;
        distance = (isVdw ? 100 : SV.dValue(args[0]));
        switch (tok = args[1].tok) {
        case T.on:
        case T.off:
          isWithinModelSet = args[1].asBoolean();
          if (len > 2 && SV.sValue(args[2]).equalsIgnoreCase("unitcell"))
            tok = T.unitcell;
          else if (len > 2 && args[2].tok != T.bitset)
            return false;
          len = 0;
          break;
        case T.string:
          String s = SV.sValue(args[1]);
          if (s.startsWith("$")) {
            bsSelected = getAtomsNearSurface(distance, s.substring(1));
            break out;
          }
          if (s.equalsIgnoreCase("group")) {
            isWithinGroup = true;
            tok = T.group;
          } else if (s.equalsIgnoreCase("vanderwaals")
              || s.equalsIgnoreCase("vdw")) {
            withinSpec = null;
            isVdw = true;
            tok = T.vanderwaals;
          } else {
            tok = T.getTokFromName(s);
            if (tok == T.nada)
              return false;
          }
          break;
        }
        break;
      case T.varray:
        if (len == 1) {
          withinSpec = args[0].asString(); // units ids8
          tok = T.nada;
        }
        break;
      case T.branch:
        // within(branch,bs1, bs2)
        bsSelected = (len == 3 && args[1].value instanceof BS
            && args[2].value instanceof BS
                ? vwr.getBranchBitSet(((BS) args[2].value).nextSetBit(0),
                    ((BS) args[1].value).nextSetBit(0), true)
                : null);
        break out;
      case T.smiles:
      case T.substructure: // same as "SMILES"
      case T.search:
        // within("smiles", "...", {atoms})
        // within("smarts", "...", {atoms})
        // {atoms}.within("smiles", "...")
        // {atoms}.within("smarts", "...")
        boolean isOK = true;
        switch (len) {
        case 2:
          bsSelected = bs;
          break;
        case 3:
          isOK = (args[2].tok == T.bitset);
          if (isOK)
            bsSelected = (BS) args[2].value;
          break;
        default:
          isOK = false;
        }
        return isOK
            && mp.addXObj(e.getSmilesExt().getSmilesMatches(SV.sValue(args[1]),
                null, bsSelected, null,
                tok == T.search ? JC.SMILES_TYPE_SMARTS : JC.SMILES_TYPE_SMILES,
                mp.asBitSet, false));
      }
      if (withinSpec instanceof String) {
        if (tok == T.nada) {
          tok = T.spec_seqcode;
          if (len > 2)
            return false;
          len = 2;
        }
      } else if (!isDistance) {
        return false;
      }
      switch (len) {
      case 1:
        // within (sheet)
        // within (helix)
        // within (boundbox)
        // within (unitcell)
        // within (basepair)
        // within ("...a sequence...")
        switch (tok) {
        case T.sheet:
        case T.helix:
        case T.boundbox:
        case T.unitcell:
          bsSelected = ms.getAtoms(tok, null);
          break out;
        case T.basepair:
          bsSelected = ms.getAtoms(tok, "");
          break out;
        case T.spec_seqcode:
          bsSelected = ms.getAtoms(T.sequence, withinStr);
          break out;
        }
        return false;
      case 2:
        // within (atomName, "XX,YY,ZZZ")
        // within (unitcell, u);
        switch (tok) {
        case T.varray:
          break;
        case T.spec_seqcode:
          tok = T.sequence;
          break;
        case T.identifier:
        case T.atomname:
        case T.atomtype:
        case T.basepair:
        case T.sequence:
        case T.dssr:
        case T.rna3d:
        case T.domains:
        case T.validation:
          bsSelected = vwr.ms.getAtoms(tok, SV.sValue(args[1]));
          break out;
        case T.cell:
        case T.centroid:
          bsSelected = vwr.ms.getAtoms(tok, SV.ptValue(args[1]));
          break out;
        case T.unitcell:
          Lst<SV> l = args[1].getList();
          if (l == null)
            return false;
          P3d[] oabc = null;
          SymmetryInterface uc = null;
          if (l.size() != 4)
            return false;
          oabc = new P3d[4];
          for (int i = 0; i < 4; i++) {
            if ((oabc[i] = SV.ptValue(l.get(i))) == null)
              return false;
          }
          uc = vwr.getSymTemp().getUnitCell(oabc, false, null);
          bsSelected = vwr.ms.getAtoms(tok, uc);
          break out;
        }
        break;
      case 3:
        switch (tok) {
        case T.on:
        case T.off:
        case T.group:
        case T.vanderwaals:
        case T.unitcell:
        case T.plane:
        case T.hkl:
        case T.coord:
        case T.point3f:
        case T.varray:
          break;
        case T.sequence:
          // within ("sequence", "CII", *.ca)
          withinStr = SV.sValue(args[1]);
          break;
        default:
          return false;
        }
        // within (distance, group, {atom collection})
        // within (distance, true|false, {atom collection})
        // within (distance, plane|hkl, [plane definition] )
        // within (distance, coord, [point or atom center] )
        break;
      }
      P4d plane = null;
      P3d pt = null;
      Lst<SV> pts1 = null;
      int last = args.length - 1;
      switch (args[last].tok) {
      case T.point4f:
        plane = (P4d) args[last].value;
        break;
      case T.point3f:
        pt = (P3d) args[last].value;
        if (SV.sValue(args[1]).equalsIgnoreCase("hkl"))
          plane = e.getHklPlane(pt, Double.NaN, null);
        break;
      case T.varray:
        pts1 = (last == 2 && args[1].tok == T.varray ? args[1].getList()
            : null);
        pt = (last == 2 ? SV.ptValue(args[1])
            : last == 1 ? P3d.new3(Double.NaN, 0, 0) : null);
        break;
      }
      if (plane != null) {
        bsSelected = ms.getAtomsNearPlane(distance, plane);
        break out;
      } 
      
      // from here we allow bs.within(...) or within(...,bs);
      
      BS bsLast = (args[last].tok == T.bitset ? (BS) args[last].value : null);
      if (bs == null)
        bs = bsLast;
      if (last > 0 && pt == null && pts1 == null && bs == null)
        return false;
      // if we have anything, it must have a point or an array or a plane or a bitset from here on out    
      if (tok == T.unitcell) {
        boolean asMap = isWithinModelSet;
        return ((bs != null || pt != null) && mp
            .addXObj(vwr.ms.getUnitCellPointsWithin(distance, bs, pt, asMap)));
      }
      if (pt != null || pts1 != null) {
        if (args[last].tok == T.varray) {
          // within(dist, pt, [pt1, pt2, pt3...])
          // within(dist, [pt1, pt2, pt3...])
          // {*}.within(0.1, [points])
          Lst<SV> sv = args[last].getList();
          P3d[] ap3 = new P3d[sv.size()];
          for (int i = ap3.length; --i >= 0;)
            ap3[i] = SV.ptValue(sv.get(i));
          P3d[] ap31 = null;
          if (pts1 != null) {
            ap31 = new P3d[pts1.size()];
            for (int i = ap31.length; --i >= 0;)
              ap31[i] = SV.ptValue(pts1.get(i));
          }
          Object[] ret = new Object[1];
          if (bs != null) {
            bs.and(vwr.getAllAtoms());
            ap31 = vwr.ms.at;
          }
          switch (PointIterator.withinDistPoints(distance, pt, ap3, ap31, bs,
              ret)) {
          case T.bitset:
            return mp.addXBs((BS) ret[0]);
          case T.point:
            return mp.addXPt((P3d) ret[0]);
          case T.list:
            return mp.addXList((Lst<?>) ret[0]);
          case T.array: // 
            return mp.addXAI((int[]) ret[0]);
          case T.string: // ""  return
            return mp.addXStr((String) ret[0]);
          default:
            return false; // error return
          }
        }
        // this is for ALL models
        return mp.addXBs(vwr.getAtomsNearPt(distance, pt, null));
      }
      if (tok == T.sequence) {
        return mp.addXBs(vwr.ms.getSequenceBits(withinStr, bs, new BS()));
      }
      if (bs == null)
        bs = new BS();
      if (!isDistance) {
        try {
          return mp.addXBs(vwr.ms.getAtoms(tok, bs));
        } catch (Exception e) {
          return false;
        }
      }
      if (isWithinGroup)
        return mp.addXBs(vwr.getGroupsWithin((int) distance, bs));
      if (isVdw) {
        rd = new RadiusData(null, (distance > 10 ? distance / 100 : distance),
            (distance > 10 ? EnumType.FACTOR : EnumType.OFFSET), VDW.AUTO);
        if (distance < 0)
          distance = 0; // not used, but this prevents a diversion
      }
      BS bsret = vwr.ms.getAtomsWithinRadius(distance,
          (isAtomProperty ? bsLast : bs), isWithinModelSet, rd,
          isAtomProperty ? bs : null);
      if (isAtomProperty) {
        // {*}.within(0.001, x) excludes atoms x 
        bsret.andNot(bsLast);
      }
      return mp.addXBs(bsret);
    }
    // fall out after selection
    if (bsSelected != null) {
      if (bs != null)
        bsSelected.and(bs);
      return mp.addXBs(bsSelected);
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private boolean evaluateWrite(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {
    if (!checkAccess())
      return false;
    int n = args.length;
    boolean asBytes = false;
    if (n == 2 && args[1].tok == T.on) {
      n = 1;
      asBytes = true;
    }
    switch (n) {
    case 0:
      return false;
    case 1:
      // write("PNGJ")       // map
      // write("PNGJ", true) // byte array
      String type = args[0].asString().toUpperCase();
      if (type.equals("PNGJ")) {
        Object o = vwr.fm.getFileAsMap(null, "PNGJ", asBytes);
        return (asBytes ? mp.addX(SV.newV(T.barray, new BArray((byte[]) o))) : mp.addXMap((Map<String, Object>) o));
      }
      if (PT.isOneOf(type, ";ZIP;ZIPALL;JMOL;")) {
        Map<String, Object> params = new Hashtable<String, Object>();
        OC oc = new OC();
        params.put("outputChannel", oc);
        vwr.createZip(null, type, params);
        byte[] bytes = oc.toByteArray();
        if (asBytes)
          return mp.addX(SV.newV(T.barray, new BArray(bytes)));
        params = new Hashtable<String, Object>();
        vwr.readFileAsMap(Rdr.getBIS(bytes), params, null);
        return mp.addXMap(params);
      }
      break;
    }
    return mp.addXStr(e.getCmdExt().dispatch(T.write, true, args));
  }

  ///////// private methods used by evaluateXXXXX ////////

  private BS getAtomsNearSurface(double distance, String surfaceId) {
    Object[] data = new Object[] { surfaceId, null, null };
    if (e.getShapePropertyData(JC.SHAPE_ISOSURFACE, "getVertices", data))
      return getAtomsNearPts(distance, (T3d[]) data[1], (BS) data[2]);
    data[1] = Integer.valueOf(0);
    data[2] = Integer.valueOf(-1);
    if (e.getShapePropertyData(JC.SHAPE_DRAW, "getCenter", data)) {
      // this is for ALL models
      return vwr.getAtomsNearPt(distance, (P3d) data[2], null);
    }
    data[1] = Double.valueOf(distance);
    if (e.getShapePropertyData(JC.SHAPE_POLYHEDRA, "getAtomsWithin", data))
      return (BS) data[2];
    return new BS();
  }

  private BS getAtomsNearPts(double distance, T3d[] points, BS bsInclude) {
    BS bsResult = new BS();
    if (points.length == 0 || bsInclude != null && bsInclude.isEmpty())
      return bsResult;
    if (bsInclude == null)
      bsInclude = BSUtil.setAll(points.length);
    Atom[] at = vwr.ms.at;
    for (int i = vwr.ms.ac; --i >= 0;) {
      Atom atom = at[i];
      if (atom == null)
        continue;
      for (int j = bsInclude.nextSetBit(0); j >= 0; j = bsInclude
          .nextSetBit(j + 1))
        if (atom.distance(points[j]) < distance) {
          bsResult.set(i);
          break;
        }
    }
    return bsResult;
  }

  @SuppressWarnings("unchecked")
  public Object getMinMax(Object floatOrSVArray, int tok, boolean isSV) {
    double[] data = null;
    Lst<?> sv = null;
    int ndata = 0;
    Map<String, Integer> htPivot = null;
    while (true) {
      if (AU.isAD(floatOrSVArray)) {
        if (tok == T.pivot)
          return nan;
        data = (double[]) floatOrSVArray;
        ndata = data.length;
        if (ndata == 0)
          break;
      } else if (floatOrSVArray instanceof Lst<?>) {
        sv = (Lst<?>) floatOrSVArray;
        ndata = sv.size();
        if (ndata == 0) {
          if (tok != T.pivot)
            break;
        } else {
          if (tok != T.pivot) {
            SV sv0 = (SV) sv.get(0);
            if (sv0.tok == T.point3f)
              return getMinMaxPoint(sv, tok);
            if (sv0.tok == T.string && ((String) sv0.value).startsWith("{")) {
              Object pt = SV.ptValue(sv0);
              if (pt instanceof P3d)
                return getMinMaxPoint(sv, tok);
              if (pt instanceof P4d)
                return getMinMaxQuaternion((Lst<SV>) sv, tok);
              break;
            }
          }
        }
      } else {
        break;
      }
      double sum;
      int minMax;
      boolean isMin = false;
      switch (tok) {
      case T.pivot:
        htPivot = new Hashtable<String, Integer>();
        sum = minMax = 0;
        break;
      case T.min:
        isMin = true;
        sum = Double.MAX_VALUE;
        minMax = Integer.MAX_VALUE;
        break;
      case T.max:
        sum = -Double.MAX_VALUE;
        minMax = -Integer.MAX_VALUE;
        break;
      default:
        sum = minMax = 0;
      }
      double sum2 = 0;
      int n = 0;
      boolean isInt = true;
      boolean isPivot = (tok == T.pivot);
      for (int i = ndata; --i >= 0;) {
        Object o = (sv == null ? null : sv.get(i));
        SV svi = (!isSV ? null : o == null ? SV.vF : (SV) o);
        double v = (isPivot ? 1 : data == null ? SV.dValue(svi) : data[i]);
        if (Double.isNaN(v))
          continue;
        n++;
        switch (tok) {
        case T.sum2:
        case T.stddev:
          sum2 += v * v;
          //$FALL-THROUGH$
        case T.sum:
        case T.average:
          sum += v;
          break;
        case T.pivot:
          String key = (svi == null ? o.toString() : svi.asString());
          Integer ii = htPivot.get(key);
          htPivot.put(key,
              (ii == null ? Integer.valueOf(1) : Integer.valueOf(ii.intValue() + 1)));
          break;
        case T.min:
        case T.max:
          isInt &= (svi.tok == T.integer);
          if (isMin == (v < sum)) {
            sum = v;
            if (isInt)
              minMax = svi.intValue;
          }
          break;
        }
      }
      if (tok == T.pivot) {
        return htPivot;
      }
      if (n == 0)
        break;
      switch (tok) {
      case T.average:
        sum /= n;
        break;
      case T.stddev:
        if (n == 1)
          break;
        sum = Math.sqrt((sum2 - sum * sum / n) / (n - 1));
        break;
      case T.min:
      case T.max:
        if (isInt)
          return Integer.valueOf(minMax);
        break;
      case T.sum:
        break;
      case T.sum2:
        sum = sum2;
        break;
      }
      return Double.valueOf(sum);
    }
    return nan;
  }

  /**
   * calculates the statistical value for x, y, and z independently
   * 
   * @param pointOrSVArray
   * @param tok
   * @return Point3f or NaN
   */
  @SuppressWarnings("unchecked")
  private Object getMinMaxPoint(Object pointOrSVArray, int tok) {
    P3d[] data = null;
    Lst<SV> sv = null;
    int ndata = 0;
    if (pointOrSVArray instanceof Qd[]) {
      data = (P3d[]) pointOrSVArray;
      ndata = data.length;
    } else if (pointOrSVArray instanceof Lst<?>) {
      sv = (Lst<SV>) pointOrSVArray;
      ndata = sv.size();
    }
    if (sv == null && data == null)
      return nan;
    P3d result = new P3d();
    double[] fdata = new double[ndata];
    for (int xyz = 0; xyz < 3; xyz++) {
      for (int i = 0; i < ndata; i++) {
        P3d pt = (data == null ? SV.ptValue(sv.get(i)) : data[i]);
        if (pt == null)
          return nan;
        switch (xyz) {
        case 0:
          fdata[i] = pt.x;
          break;
        case 1:
          fdata[i] = pt.y;
          break;
        case 2:
          fdata[i] = pt.z;
          break;
        }
      }
      Object f = getMinMax(fdata, tok, true);
      if (!(f instanceof Number))
        return nan;
      double value = ((Number) f).doubleValue();
      switch (xyz) {
      case 0:
        result.x = value;
        break;
      case 1:
        result.y = value;
        break;
      case 2:
        result.z = value;
        break;
      }
    }
    return result;
  }

  private Object getMinMaxQuaternion(Lst<SV> svData, int tok) {
    Qd[] data;
    switch (tok) {
    case T.min:
    case T.max:
    case T.sum:
    case T.sum2:
      return nan;
    }

    // only stddev and average

    while (true) {
      data = e.getQuaternionArray(svData, T.list);
      if (data == null)
        break;
      double[] retStddev = new double[1];
      Qd result = Qd.sphereMean(data, retStddev, 0.0001f);
      switch (tok) {
      case T.average:
        return result;
      case T.stddev:
        return Double.valueOf(retStddev[0]);
      }
      break;
    }
    return nan;
  }

  private JmolPatternMatcher pm;

  private JmolPatternMatcher getPatternMatcher() {
    return (pm == null
        ? pm = (JmolPatternMatcher) Interface.getUtil("PatternMatcher", e.vwr,
            "script")
        : pm);
  }

  private T opTokenFor(int tok) {
    switch (tok) {
    case T.add:
    case T.join:
      return T.tokenPlus;
    case T.sub:
      return T.tokenMinus;
    case T.mul:
      return T.tokenTimes;
    case T.mul3:
      return T.tokenMul3;
    case T.div:
      return T.tokenDivide;
    }
    return null;
  }

  public BS setContactBitSets(BS bsA, BS bsB, boolean localOnly,
                              double distance, RadiusData rd,
                              boolean warnMultiModel) {
    boolean withinAllModels;
    BS bs;
    if (bsB == null) {
      // default is within just one model when {B} is missing
      bsB = BSUtil.setAll(vwr.ms.ac);
      BSUtil.andNot(bsB, vwr.slm.bsDeleted);
      bsB.andNot(bsA);
      withinAllModels = false;
    } else {
      // two atom sets specified; within ALL MODELS here
      bs = BSUtil.copy(bsA);
      bs.or(bsB);
      int nModels = vwr.ms.getModelBS(bs, false).cardinality();
      withinAllModels = (nModels > 1);
      if (warnMultiModel && nModels > 1 && !e.tQuiet)
        e.showString(
            GT.$("Note: More than one model is involved in this contact!"));
    }
    // B always within some possibly extended VDW of A or just A itself
    if (!bsA.equals(bsB)) {
      boolean setBfirst = (!localOnly || bsA.cardinality() < bsB.cardinality());
      if (setBfirst) {
        bs = vwr.ms.getAtomsWithinRadius(distance, bsA, withinAllModels,
            (Double.isNaN(distance) ? rd : null), null);
        bsB.and(bs);
      }
      if (localOnly) {
        // we can just get the near atoms for A as well.
        bs = vwr.ms.getAtomsWithinRadius(distance, bsB, withinAllModels,
            (Double.isNaN(distance) ? rd : null), null);
        bsA.and(bs);
        if (!setBfirst) {
          bs = vwr.ms.getAtomsWithinRadius(distance, bsA, withinAllModels,
              (Double.isNaN(distance) ? rd : null), null);
          bsB.and(bs);
        }
        // If the two sets are not the same,
        // we AND them and see if that is A. 
        // If so, then the smaller set is
        // removed from the larger set.
        bs = BSUtil.copy(bsB);
        bs.and(bsA);
        if (bs.equals(bsA))
          bsB.andNot(bsA);
        else if (bs.equals(bsB))
          bsA.andNot(bsB);
      }
    }
    return bsB;
  }
}
