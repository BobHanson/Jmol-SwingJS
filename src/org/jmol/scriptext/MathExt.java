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

import java.io.BufferedInputStream;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javajs.util.AU;
import javajs.util.CU;
import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.Measure;
import javajs.util.OC;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.Quat;
import javajs.util.Rdr;
import javajs.util.SB;
import javajs.util.T3;
import javajs.util.V3;

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
import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.BondSet;
import org.jmol.modelset.ModelSet;
import org.jmol.script.SV;
import org.jmol.script.ScriptEval;
import org.jmol.script.ScriptException;
import org.jmol.script.ScriptMathProcessor;
import org.jmol.script.ScriptParam;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.ColorEncoder;
import org.jmol.util.Edge;
import org.jmol.util.Escape;
import org.jmol.util.JmolMolecule;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.util.Point3fi;
import org.jmol.util.SimpleUnitCell;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

public class MathExt {
  
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

  
  public boolean evaluate(ScriptMathProcessor mp, T op, SV[] args, int tok)
      throws ScriptException {
    switch (tok) {
    case T.abs:
    case T.acos:
    case T.cos:
    case T.now:
    case T.sin:
    case T.sqrt:
      return evaluateMath(mp, args, tok);
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
      return evaluateArray(mp, args, tok == T.array && op.tok == T.propselector);
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
    case T.unitcell:
      return evaluateUnitCell(mp, args, op.tok == T.propselector);
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
    case T.format:
    case T.label:
      return evaluateFormat(mp, op.intValue, args, tok == T.label);
    case T.function:
      return evaluateUserFunction(mp, (String) op.value, args, op.intValue,
          op.tok == T.propselector);
    case T.__:
    case T.select:
    case T.getproperty:
      return evaluateGetProperty(mp, args, tok, op.tok == T.propselector);
    case T.helix:
      return evaluateHelix(mp, args);
    case T.hkl:
    case T.plane:
    case T.intersection:
      return evaluatePlane(mp, args, tok);
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
      return evaluatePointGroup(mp, args);
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
    case T.search:
    case T.smiles:
    case T.substructure:
      return evaluateSubstructure(mp, args, tok, op.tok == T.propselector);
    case T.sort:
    case T.count:
      return evaluateSort(mp, args, tok);
    case T.symop:
      return evaluateSymop(mp, args, op.tok == T.propselector);
      //    case Token.volume:
      //    return evaluateVolume(args);
    case T.tensor:
      return evaluateTensor(mp, args);
    case T.within:
      return evaluateWithin(mp, args);
    case T.write:
      return evaluateWrite(mp, args);
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private boolean evaluatePointGroup(ScriptMathProcessor mp, SV[] args) {
    // pointGroup(points)
    // pointGroup(points, center)
    // pointGroup(points, center, distanceTolerance (def. 0.2 A), linearTolerance (def. 8 deg.)
    // center can be non-point to ignore, such as 0 or ""
    T3[] pts = null;
    P3 center = null;
    float distanceTolerance = Float.NaN;
    float linearTolerance = Float.NaN;
    BS bsAtoms;
    switch (args.length) {
    case 4:
      linearTolerance = args[3].asFloat();
      //$FALL-THROUGH$
    case 3:
      distanceTolerance = args[2].asFloat();
      //$FALL-THROUGH$
    case 2:
      switch (args[1].tok) {
      case T.point3f:
        center = SV.ptValue(args[1]);
        break;
      case T.bitset:
        // pointgroup {vertices} {center}
        bsAtoms = SV.getBitSet(args[1], false);
        int iatom = bsAtoms.nextSetBit(0);
        if (iatom < 0 || iatom >= vwr.ms.ac || bsAtoms.cardinality() != 1)
          return false;
        if (SV.sValue(args[0]).equalsIgnoreCase("spaceGroup")) {
          // pointgroup("spaceGroup", @1)
          Lst<P3> lst = vwr.ms.generateCrystalClass(iatom,
              P3.new3(Float.NaN, Float.NaN, Float.NaN));
          pts = new T3[lst.size()];
          for (int i = pts.length; --i >= 0;)
            pts[i] = lst.get(i);
          center = new P3();
          if (args.length == 2)
            distanceTolerance = 0; // will set tolerances especially tight
        } else {
          center = vwr.ms.at[iatom];
        }
      }
      if (pts != null)
        break;
      //$FALL-THROUGH$
    case 1:
      switch (args[0].tok) {
      case T.varray:
        Lst<SV> points = args[0].getList();
        pts = new T3[points.size()];
        for (int i = pts.length; --i >= 0;)
          pts[i] = SV.ptValue(points.get(i));
        break;
      case T.bitset:
        bsAtoms = SV.getBitSet(args[0], false);
        Lst<P3> atoms = vwr.ms.getAtomPointVector(bsAtoms);
        pts = new T3[atoms.size()];
        for (int i = pts.length; --i >= 0;)
          pts[i] = atoms.get(i);
        break;
      default:
        return false;
      }
      break;
    default:
      return false;
    }
    SymmetryInterface pointGroup = vwr.getSymTemp().setPointGroup(
        null,
        center,
        pts,
        null,
        false,
        Float.isNaN(distanceTolerance) ? vwr
            .getFloat(T.pointgroupdistancetolerance) : distanceTolerance,
        Float.isNaN(linearTolerance) ? vwr
            .getFloat(T.pointgrouplineartolerance) : linearTolerance, true);
    return mp.addXMap((Map<String, ?>) pointGroup.getPointGroupInfo(-1, null,
        true, null, 0, 1));
  }

  private boolean evaluateUnitCell(ScriptMathProcessor mp, SV[] args,
                                   boolean isSelector) throws ScriptException {
    // optional last parameter: scale
    // unitcell("-a,-b,c;0,0,0.50482") (polar groups can have irrational translations along z)
    // unitcell(uc)
    // unitcell(uc, "reciprocal")
    // unitcell(origin, [va, vb, vc])
    // unitcell(origin, pta, ptb, ptc)

    // next can be without {1.1}, but then assume "all atoms"
    // {1.1}.unitcell()
    // {1.1}.unitcell(ucconv, "primitive","BCC"|"FCC")
    // {1.1}.unitcell(ucprim, "conventional","BCC"|"FCC")

    BS x1 = (isSelector ? SV.getBitSet(mp.getX(), true) : null);
    int iatom = ((x1 == null ? vwr.getAllAtoms() : x1).nextSetBit(0));
    int lastParam = args.length - 1;
    float scale = 1;
    switch (lastParam < 0 ? T.nada : args[lastParam].tok) {
    case T.integer:
    case T.decimal:
      scale = args[lastParam].asFloat();
      lastParam--;
      break;
    }
    int tok0 = (lastParam < 0 ? T.nada : args[0].tok);
    T3[] ucnew = null;
    Lst<SV> uc = null;
    switch (tok0) {
    case T.varray:
      uc = args[0].getList();
      break;
    case T.string:
      String s = args[0].asString();
      if (s.indexOf("a=") == 0) {
        ucnew = new P3[4];
        for (int i = 0; i < 4; i++)
          ucnew[i] = new P3();
        SimpleUnitCell.setOabc(s, null, ucnew);
      } else if (s.indexOf(",") >= 0) {
        return mp.addXObj(vwr.getV0abc(s));
      }
      break;
    }
    SymmetryInterface u = null;
    boolean haveUC = (uc != null);
    if (ucnew == null && haveUC && uc.size() < 4)
      return false;
    int ptParam = (haveUC ? 1 : 0);
    if (ucnew == null && !haveUC && tok0 != T.point3f) {
      // unitcell() or {1.1}.unitcell
      u = (iatom < 0 ? null : vwr.ms.getUnitCell(vwr.ms.at[iatom].mi));
      ucnew = (u == null ? new P3[] { P3.new3(0, 0, 0), P3.new3(1, 0, 0),
          P3.new3(0, 1, 0), P3.new3(0, 0, 1) } : u.getUnitCellVectors());
    }
    if (ucnew == null) {
      ucnew = new P3[4];
      if (haveUC) {
        switch (uc.size()) {
        case 3:
          // [va. vb. vc]
          ucnew[0] = new P3();
          for (int i = 0; i < 3; i++)
            ucnew[i + 1] = P3.newP(SV.ptValue(uc.get(i)));
          break;
        case 4:
          for (int i = 0; i < 4; i++)
            ucnew[i] = P3.newP(SV.ptValue(uc.get(i)));
          break;
        case 6:
          // unitcell([a b c alpha beta gamma])
          float[] params = new float[6];
          for (int i = 0; i < 6; i++)
            params[i] = uc.get(i).asFloat();
          SimpleUnitCell.setOabc(null, params, ucnew);
          break;
        default:
          return false;
        }
      } else {
        ucnew[0] = SV.ptValue(args[0]);
        switch (lastParam) {
        case 3:
          // unitcell(origin, pa, pb, pc)
          for (int i = 1; i < 4; i++)
            (ucnew[i] = P3.newP(SV.ptValue(args[i]))).sub(ucnew[0]);
          break;
        case 1:
          // unitcell(origin, [va, vb, vc])
          Lst<SV> l = args[1].getList();
          if (l != null && l.size() == 3) {
            for (int i = 0; i < 3; i++)
              ucnew[i + 1] = SV.ptValue(l.get(i));
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
      String stype = (++ptParam > lastParam ? "" : args[ptParam].asString()
          .toUpperCase());
      if (stype.equals("BCC"))
        stype = "I";
      else if (stype.length() == 0)
        stype = (String) vwr.getSymTemp().getSymmetryInfoAtom(vwr.ms, iatom,
            null, 0, null, null, null, T.lattice, 0, -1);
      if (stype == null || stype.length() == 0)
        return false;
      if (u == null)
         u= vwr.getSymTemp();
      M3 m3 = (M3) vwr.getModelForAtomIndex(iatom).auxiliaryInfo.get("primitiveToCrystal");
      if (!u.toFromPrimitive(toPrimitive, stype.charAt(0), ucnew, m3))
        return false;
    } else if ("reciprocal".equalsIgnoreCase(op)) {
      ucnew = SimpleUnitCell.getReciprocal(ucnew, null, scale);
      scale = 1;
    }
    if (scale != 1)
      for (int i = 1; i < 4; i++)
        ucnew[i].scale(scale);
    return mp.addXObj(ucnew);
  }

  @SuppressWarnings("unchecked")
  private boolean evaluateArray(ScriptMathProcessor mp, SV[] args, boolean isSelector) throws ScriptException {
    if (isSelector) {
      SV x1 = mp.getX();
      switch (args.length == 1 ? x1.tok : T.nada) {
      case T.hash:
        // map of maps to lst of maps
        Lst<SV> lst = new Lst<SV>();
        String id = args[0].asString();
        Map<String, SV> map = x1.getMap();
        String[] keys = x1.getKeys(false);
        // values must all be maps
        for (int i = 0, n = keys.length; i < n; i++)
          if (map.get(keys[i]).getMap() == null)
            return false;
        for (int i = 0, n = keys.length; i < n; i++) {
          SV m = map.get(keys[i]);
          Map<String, SV> m1 = m.getMap();
          Map<String, SV> m2 = (Map<String, SV>) SV.deepCopy(m1, true, false);
          m2.put(id, SV.newS(keys[i]));
          lst.addLast(SV.newV(T.hash, m2));
        }
        return mp.addXList(lst);
      case T.varray:
        Map<String, SV> map1 = new Hashtable<String, SV>();
        Lst<SV> lst1 = x1.getList();
        String id1 = args[0].asString();
        // values must all be maps
        for (int i = 0, n = lst1.size(); i < n; i++) {
          Map<String, SV> m0 = lst1.get(i).getMap(); 
          if (m0 == null || m0.get(id1) == null)
            return false;
        }
        for (int i = 0, n = lst1.size(); i < n; i++){
          SV m = lst1.get(i);
          Map<String, SV> m1 = (Map<String, SV>) SV.deepCopy(m.getMap(), true, false);
          SV mid = m1.remove(id1);
          map1.put(mid.asString(), SV.newV(T.hash, m1));
        }
        return mp.addXObj(map1);
      }
      return  false;
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
    float f0 = SV.fValue(args[0]);
    float f1 = SV.fValue(args[1]);
    float df = SV.fValue(args[2]);
    boolean addBins = (n >= 4 && args[n-1].tok == T.on);
    String key = ((n == 5 || n == 4 && !addBins) && args[3].tok != T.off ? SV.sValue(args[3]) : null);
    float[] data;
    Map<String, SV>[] maps = null;
    if (isListf) {
      data = (float[]) x1.value;
    } else {
      Lst<SV> list = x1.getList();
      data = new float[list.size()];
      if (key != null)
        maps = AU.createArrayOfHashtable(list.size());
      try {
      for (int i = list.size(); --i >= 0;)
        data[i] = SV.fValue(key == null ? list.get(i) : (maps[i] = list.get(i).getMap()).get(key));
      }catch (Exception e) {
        return false;
      }
    }
    int nbins = Math.max((int) Math.floor((f1 - f0) / df + 0.01f), 1);
    int[] array = new int[nbins];
    int nPoints = data.length;
    for (int i = 0; i < nPoints; i++) {
      float v = data[i];
      int bin = (int) Math.floor((v - f0) / df);
      if (bin < 0 || bin >= nbins)
        continue;      
      array[bin]++;
      if (key != null) {
        Map<String, SV> map = maps[i];
        if (map == null)
          continue;
        map.put("_bin", SV.newI(bin));
        float v1 = f0 + df * bin;
        float v2 = v1 + df;
        map.put("_binMin", SV.newF(bin == 0 ? -Float.MAX_VALUE : v1));
        map.put("_binMax", SV.newF(bin == nbins - 1 ? Float.MAX_VALUE  : v2));        
      }
    }
    if (addBins) {
      Lst<float[]> lst = new Lst<float[]>();
      for (int i = 0; i < nbins; i++)
        lst.addLast(new float[] { f0 + df * i, array[i]});
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
      return mp.addXPt(CU.rgbToHSL(P3.newP(args[1].tok == T.point3f ? SV
          .ptValue(args[1])
          : CU.colorPtFromString(args[1].asString())), true));
    if (args.length == 2 && colorScheme.equalsIgnoreCase("TORGB")) {
      P3 pt = P3.newP(args[1].tok == T.point3f ? SV.ptValue(args[1]) : CU
          .colorPtFromString(args[1].asString()));
      return mp.addXPt(args[1].tok == T.point3f ? CU.hslToRGB(pt) : pt);
    }
    if (args.length == 4 && (args[3].tok == T.on || args[3].tok == T.off)) {
      P3 pt1 = P3.newP(args[0].tok == T.point3f ? SV.ptValue(args[0]) : CU
          .colorPtFromString(args[0].asString()));
      P3 pt2 = P3.newP(args[1].tok == T.point3f ? SV.ptValue(args[1]) : CU
          .colorPtFromString(args[1].asString()));
      boolean usingHSL = (args[3].tok == T.on);
      if (usingHSL) {
        pt1 = CU.rgbToHSL(pt1, false);
        pt2 = CU.rgbToHSL(pt2, false);
      }

      SB sb = new SB();
      V3 vd = V3.newVsub(pt2, pt1);
      int n = args[2].asInt();
      if (n < 2)
        n = 20;
      vd.scale(1f / (n - 1));
      for (int i = 0; i < n; i++) {
        sb.append(Escape.escapeColor(CU.colorPtToFFRGB(usingHSL ? CU
            .hslToRGB(pt1) : pt1)));
        pt1.add(vd);
      }
      return mp.addXStr(sb.toString());
    }

    ColorEncoder ce = (isIsosurface ? null : vwr
        .cm.getColorEncoder(colorScheme));
    if (!isIsosurface && ce == null)
      return mp.addXStr("");
    float lo = (args.length > 1 ? SV.fValue(args[1]) : Float.MAX_VALUE);
    float hi = (args.length > 2 ? SV.fValue(args[2]) : Float.MAX_VALUE);
    float value = (args.length > 3 ? SV.fValue(args[3]) : Float.MAX_VALUE);
    boolean getValue = (value != Float.MAX_VALUE || lo != Float.MAX_VALUE
        && hi == Float.MAX_VALUE);
    boolean haveRange = (hi != Float.MAX_VALUE);
    if (!haveRange && colorScheme.length() == 0) {
      value = lo;
      float[] range = vwr.getCurrentColorRange();
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
      return mp.addXPt(CU.colorPtFromInt(ce.getArgb(hi == Float.MAX_VALUE ? lo
          : value), null));
    return mp.addX(SV.getVariableMap(key));
  }

  private boolean evaluateCompare(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {
    // compare([{bitset} or {positions}],[{bitset} or {positions}] [,"stddev"])
    // compare({bitset},{bitset}[,"SMARTS"|"SMILES"],smilesString [,"stddev"])
    // returns matrix4f for rotation/translation or stddev
    // compare({bitset},{bitset},"ISOMER")  12.1.5
    // compare({bitset},{bitset},smartsString, "BONDS") 13.1.17
    // compare({bitset},{bitset},"SMILES", "BONDS") 13.3.9
    // compare({bitest},{bitset},"MAP","smilesString")
    // compare({bitset},{bitset},"MAP")
    // compare(@1,@2,"SMILES", "polyhedra"[,"stddev"])
    // compare(@1,@2,"SMARTS", "polyhedra"[,"stddev"])

    if (args.length < 2 || args.length > 5)
      return false;
    float stddev;
    String sOpt = SV.sValue(args[args.length - 1]);
    boolean isStdDev = sOpt.equalsIgnoreCase("stddev");
    boolean isIsomer = sOpt.equalsIgnoreCase("ISOMER");
    boolean isBonds = sOpt.equalsIgnoreCase("BONDS");
    
    boolean isSmiles = (!isIsomer && args.length > (isStdDev ? 3 : 2));
    BS bs1 = (args[0].tok == T.bitset ? (BS) args[0].value : null);
    BS bs2 = (args[1].tok == T.bitset ? (BS) args[1].value : null);
    String smiles1 = (bs1 == null ? SV.sValue(args[0]) : "");
    String smiles2 = (bs2 == null ? SV.sValue(args[1]) : "");
    M4 m = new M4();
    stddev = Float.NaN;
    Lst<P3> ptsA, ptsB;
    try {
      if (isSmiles) {
        if (bs1 == null || bs2 == null)
          return false;
      }
      if (isBonds) {
        if (args.length != 4)
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
        float[] data = e.getSmilesExt().getFlexFitList(bs1, bs2, smiles1,
            !isSmiles);
        return (data == null ? mp.addXStr("") : mp.addXAF(data));
      }
      if (isIsomer) {
        if (args.length != 3)
          return false;
        
        // A, B, ISOMER


        if (bs1 == null && bs2 == null)
          return mp.addXStr(vwr.getSmilesMatcher()
              .getRelationship(smiles1, smiles2).toUpperCase());
        String mf1 = (bs1 == null ? vwr.getSmilesMatcher().getMolecularFormula(
            smiles1, false) : JmolMolecule.getMolecularFormulaAtoms(vwr.ms.at, bs1,
            null, false));
        String mf2 = (bs2 == null ? vwr.getSmilesMatcher().getMolecularFormula(
            smiles2, false) : JmolMolecule.getMolecularFormulaAtoms(vwr.ms.at, bs2,
            null, false));
        if (!mf1.equals(mf2))
          return mp.addXStr("NONE");
        if (bs1 != null)
          smiles1 = (String) e.getSmilesExt().getSmilesMatches("/strict///", null, bs1,
              null, JC.SMILES_TYPE_SMILES, true, false);
        boolean check;
        if (bs2 == null) {
          // note: find smiles1 IN smiles2 here
          check = (vwr.getSmilesMatcher().areEqual(smiles2, smiles1) > 0);
        } else {
          smiles2 = (String) e.getSmilesExt().getSmilesMatches("/strict///", null, bs2,
              null, JC.SMILES_TYPE_SMILES, true, false);
          check = (((BS) e.getSmilesExt().getSmilesMatches("/strict///" + smiles1, null, bs2,
              null, JC.SMILES_TYPE_SMILES, true, false)).nextSetBit(0) >= 0);
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
              smiles1 = (pt >= 0 ? "/strict/" + smiles1.substring(0, pt) + smiles1.substring(pt + 12) 
                  : "/invertstereo strict/" + smiles1);//vwr.getSmilesMatcher().reverseChirality(smiles1);
              
              
              if (bs2 == null) {
                check = (vwr.getSmilesMatcher().areEqual(smiles1, smiles2) > 0);
              } else {
                check = (((BS) e.getSmilesExt().getSmilesMatches(smiles1, null,
                    bs2, null, JC.SMILES_TYPE_SMILES, true, false)).nextSetBit(0) >= 0);
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
                  "/nostereo/" + smiles1, null, bs2, null, JC.SMILES_TYPE_SMILES, true, false);
              check = (((BS) ret).nextSetBit(0) >= 0);
            }
            if (check)
              return mp.addXStr("DIASTEREOMERS");
          }
          // MF matches, but not enantiomers or diastereomers
          return mp.addXStr("CONSTITUTIONAL ISOMERS");
        }
        //identical or conformational 
        if (bs1 == null || bs2 == null)
          return mp.addXStr("IDENTICAL");
        stddev = e.getSmilesExt().getSmilesCorrelation(bs1, bs2, smiles1, null,
            null, null, null, false, null, null, false, JC.SMILES_TYPE_SMILES);
        return mp.addXStr(stddev < 0.2f ? "IDENTICAL"
            : "IDENTICAL or CONFORMATIONAL ISOMERS (RMSD=" + stddev + ")");
      }
      if (isSmiles) {
        // A, B, MAP
        // A, B, MAP, [pattern "H" "allH" "bestH"], ["stddev"]
        // A, B, SMILES, [pattern "H" "allH" "bestH"], ["stddev"]
        // A, B, SMARTS, pattern, ["stddev"]
        // A, B, SMILES, "polyhedron", [pattern "all" "best"], ["stddev"]
        // A, B, SMARTS, "polyhedron", pattern, ["all" "best"], ["stddev"]
        ptsA = new Lst<P3>();
        ptsB = new Lst<P3>();
        sOpt = SV.sValue(args[2]);
        boolean isMap = sOpt.equalsIgnoreCase("MAP");
        isSmiles = sOpt.equalsIgnoreCase("SMILES");
        boolean isSearch = (isMap || sOpt.equalsIgnoreCase("SMARTS"));
        if (isSmiles || isSearch)
          sOpt = (args.length > (isStdDev ? 4 : 3) ? SV.sValue(args[3]) : null);
        
        // sOpts = "H", "allH", "bestH", "polyhedra", pattern, or null
        boolean hMaps = (("H".equalsIgnoreCase(sOpt)
            || "allH".equalsIgnoreCase(sOpt) || "bestH".equalsIgnoreCase(sOpt)));
        
        
        boolean isPolyhedron = ("polyhedra".equalsIgnoreCase(sOpt));
        if (isPolyhedron)
          sOpt = (args.length > (isStdDev ? 5 : 4) ? SV.sValue(args[4]) : null);
        boolean allMaps = (("all".equalsIgnoreCase(sOpt) || "allH"
            .equalsIgnoreCase(sOpt)));
        boolean bestMap = (("best".equalsIgnoreCase(sOpt) || "bestH"
            .equalsIgnoreCase(sOpt)));
        if ("stddev".equals(sOpt))
          sOpt = null;
        String pattern = sOpt;
        if (sOpt == null || hMaps || allMaps || bestMap) {
          // with explicitH we set to find only the first match.
          if (!isMap && !isSmiles || hMaps && isPolyhedron)
            return false;
          pattern = "/noaromatic"
              + (allMaps || bestMap ? "/" : " nostereo/")
              + e.getSmilesExt().getSmilesMatches((hMaps ? "H" : ""), null,
                  bs1, null, JC.SMILES_TYPE_SMILES, true, false);
        } else {
          allMaps = true;
        }
        stddev = e.getSmilesExt().getSmilesCorrelation(
            bs1,
            bs2,
            pattern,
            ptsA,
            ptsB,
            m,
            null,
            isMap,
            null,
            null,
            bestMap,
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
          return (allMaps ? mp.addXList(ret) : ret.size() > 0 ? mp.addXAII(ret
               .get(0)) : mp.addXStr(""));
        }
      } else {
        switch (args.length) {
        case 2:
          break;
        case 3:
          if (isStdDev)
            break;
          //$FALL-THROUGH$
        default:
          return false;
        }
        // A, B
        // A, B, stddev
        ptsA = e.getPointVector(args[0], 0);
        ptsB = e.getPointVector(args[1], 0);
        if (ptsA != null && ptsB != null) {
          Interface.getInterface("javajs.util.Eigen", vwr, "script");
          stddev = Measure.getTransformMatrix4(ptsA, ptsB, m, null);
        }
      }
      return (isStdDev || Float.isNaN(stddev) ? mp.addXFloat(stddev) : mp
          .addXM4(m.round(1e-7f)));
    } catch (Exception ex) {
      e.evalError(ex.getMessage() == null ? ex.toString() : ex.getMessage(),
          null);
      return false;
    }
  }

  private boolean evaluateConnected(ScriptMathProcessor mp, SV[] args, int tok,
                                    int intValue) throws ScriptException {
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
    float min = Integer.MIN_VALUE, max = Integer.MAX_VALUE;
    float fmin = 0, fmax = Float.MAX_VALUE;

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
          order = ScriptParam.getBondOrderFromString(type);
        if (order == Edge.BOND_ORDER_NULL)
          return false;
        break;
      case T.decimal:
        haveDecimal = true;
        //$FALL-THROUGH$
      default:
        int n = var.asInt();
        float f = var.asFloat();
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
      return mp.addX(SV.newV(
          T.bitset,
          BondSet.newBS(bsBonds,
              vwr.ms.getAtomIndices(vwr.ms.getAtoms(T.bonds, bsBonds)))));
    }
    return mp.addXBs(vwr.ms.getAtomsConnected(min, max, order, atoms1));
  }

  private boolean evaluateContact(ScriptMathProcessor mp, SV[] args) {
    if (args.length < 1 || args.length > 3)
      return false;
    int i = 0;
    float distance = 100;
    int tok = args[0].tok;
    switch (tok) {
    case T.decimal:
    case T.integer:
      distance = SV.fValue(args[i++]);
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
    RadiusData rd = new RadiusData(null, (distance > 10 ? distance / 100
        : distance), (distance > 10 ? EnumType.FACTOR : EnumType.OFFSET),
        VDW.AUTO);
    bsB = setContactBitSets(bsA, bsB, true, Float.NaN, rd, false);
    bsB.or(bsA);
    return mp.addXBs(bsB);
  }

  private boolean evaluateData(ScriptMathProcessor mp, SV[] args) {

    // x = data("somedataname") # the data
    // x = data("data2d_xxxx") # 2D data (x,y paired values)
    // x = data("data2d_xxxx", iSelected) # selected row of 2D data, with <=0
    // meaning "relative to the last row"
    // x = data("property_x", "property_y") # array mp.addition of two property
    // sets
    // x = data({atomno < 10},"xyz") # (or "pdb" or "mol") coordinate data in
    // xyz, pdb, or mol format
    // x = data(someData,ptrFieldOrColumn,nBytes,firstLine) # extraction of a
    // column of data based on a field (nBytes = 0) or column range (nBytes >
    // 0)
    String selected = (args.length == 0 ? "" : SV.sValue(args[0]));
    String type = "";
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
      float[] f = Parser.parseFloatArrayFromMatchAndField(SV.sValue(args[0]), null, 0, 0,
          null, iField, nBytes, null, firstLine);
      return mp.addXStr(Escape.escapeFloatA(f, false));
    default:
      return false;
    }
    if (selected.indexOf("data2d_") == 0) {
      // tab, newline separated data
      float[][] f1 = (float[][]) vwr.getDataObj(selected, null, JmolDataManager.DATA_TYPE_AFF);
      if (f1 == null)
        return mp.addXStr("");
      if (args.length == 2 && args[1].tok == T.integer) {
        int pt = args[1].intValue;
        if (pt < 0)
          pt += f1.length;
        if (pt >= 0 && pt < f1.length)
          return mp.addXStr(Escape.escapeFloatA(f1[pt], false));
        return mp.addXStr("");
      }
      return mp.addXStr(Escape.escapeFloatAA(f1, false));
    }

    // parallel mp.addition of float property data sets

    if (selected.indexOf("property_") == 0) {
      float[] f1 = (float[]) vwr.getDataObj(selected, null, JmolDataManager.DATA_TYPE_AF);
      if (f1 == null)
        return mp.addXStr("");
      float[] f2 = (type.indexOf("property_") == 0 ? (float[]) vwr.getDataObj(selected, null, JmolDataManager.DATA_TYPE_AF)
          : null);
      if (f2 != null) {
        f1 = AU.arrayCopyF(f1, -1);
        for (int i = Math.min(f1.length, f2.length); --i >= 0;)
          f1[i] += f2[i];
      }
      return mp.addXStr(Escape.escapeFloatA(f1, false));
    }

    // some other data type -- just return it

    //if (args.length == 1) {
      Object[] data = (Object[]) vwr.getDataObj(selected, null, JmolDataManager.DATA_TYPE_UNKNOWN);
      return mp.addXStr(data == null ? "" : "" + data[1]);
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
                                  int op) throws ScriptException {
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
    default:
      return false;
    }

    if (tok == T.cross) {
      P3 a = P3.newP(mp.ptValue(x1, null));
      a.cross(a, mp.ptValue(x2, null));
      return mp.addXPt(a);
    }

    P3 pt2 = (x2.tok == T.varray ? null : mp.ptValue(x2, null));
    P4 plane2 = mp.planeValue(x2);
    float f = Float.NaN;
    try {
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
              float dMinMax = Float.NaN;
              int iMinMax = Integer.MAX_VALUE;
              if (isAtomSet1) {
                for (int i = bs1.nextSetBit(0); i >= 0; i = bs1
                    .nextSetBit(i + 1)) {
                  float d = (isPoint2 ? atoms[i].distanceSquared(pt2)
                      : ((Float) e.getBitsetProperty(bs2, list2, op, atoms[i],
                          plane2, x1.value, null, false, x1.index, false))
                          .floatValue());
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
                P3 pt = SV.ptValue(list1.get(i));
                float d = (isPoint2 ? pt.distanceSquared(pt2) : ((Float) e
                    .getBitsetProperty(bs2, list2, op, pt, plane2, x1.value,
                        null, false, Integer.MAX_VALUE, false)).floatValue());
                if (minMax == T.min ? d >= dMinMax : d <= dMinMax)
                  continue;
                dMinMax = d;
                iMinMax = i;
              }
              return mp.addXInt(iMinMax);
            }
            if (isAll) {
              if (bs2 == null) {
                float[] data = new float[bs1.cardinality()];
                for (int p = 0, i = bs1.nextSetBit(0); i >= 0; i = bs1
                    .nextSetBit(i + 1), p++)
                  data[p] = atoms[i].distance(pt2);
                return mp.addXAF(data);
              }
              float[][] data2 = new float[bs1.cardinality()][bs2.cardinality()];
              for (int p = 0, i = bs1.nextSetBit(0); i >= 0; i = bs1
                  .nextSetBit(i + 1), p++)
                for (int q = 0, j = bs2.nextSetBit(0); j >= 0; j = bs2
                    .nextSetBit(j + 1), q++)
                  data2[p][q] = atoms[i].distance(atoms[j]);
              return mp.addXAFF(data2);
            }
            if (isMinMax) {
              float[] data = new float[isAtomSet1 ? bs1.cardinality() : list1
                  .size()];
              if (isAtomSet1) {
                for (int i = bs1.nextSetBit(0), p = 0; i >= 0; i = bs1
                    .nextSetBit(i + 1))
                  data[p++] = ((Float) e.getBitsetProperty(bs2, list2, op,
                      atoms[i], plane2, x1.value, null, false, x1.index, false))
                      .floatValue();
                return mp.addXAF(data);
              }
              // list of points
              for (int i = data.length; --i >= 0;)
                data[i] = ((Float) e.getBitsetProperty(bs2, list2, op,
                    SV.ptValue(list1.get(i)), plane2, null, null, false,
                    Integer.MAX_VALUE, false)).floatValue();
              return mp.addXAF(data);
            }
            return mp.addXObj(e.getBitsetProperty(bs1, list1, op, pt2, plane2,
                x1.value, null, false, x1.index, false));
          }
        }
      }
      P3 pt1 = mp.ptValue(x1, null);
      P4 plane1 = mp.planeValue(x1);
      if (isDist) {
        if (plane2 != null && x3 != null)
          f = Measure.directedDistanceToPlane(pt1, plane2, SV.ptValue(x3));
        else
          f = (plane1 == null ? (plane2 == null ? pt2.distance(pt1) : Measure
              .distanceToPlane(plane2, pt1)) : Measure.distanceToPlane(plane1,
              pt2));
      } else {
        if (plane1 != null && plane2 != null) {
          // q1.dot(q2) assume quaternions
          f = plane1.x * plane2.x + plane1.y * plane2.y + plane1.z * plane2.z
              + plane1.w * plane2.w;
          // plane.dot(point) =
        } else {
          if (plane1 != null)
            pt1 = P3.new3(plane1.x, plane1.y, plane1.z);
          // point.dot(plane)
          else if (plane2 != null)
            pt2 = P3.new3(plane2.x, plane2.y, plane2.z);
          f = pt1.dot(pt2);
        }
      }
    } catch (Exception e) {
    }
    return mp.addXFloat(f);
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
      P3 pta = mp.ptValue(args[0], null);
      P3 ptb = mp.ptValue(args[1], null);
      if (tok == T.nada || args[2].tok != T.point4f || pta == null || ptb == null)
        return false;
      Quat dq = Quat.newP4((P4) args[2].value);
      T3[] data = Measure.computeHelicalAxis(pta, ptb, dq);
      //new T3[] { pt_a_prime, n, r, P3.new3(theta, pitch, residuesPerTurn) };
      return (data == null ? false : mp.addXObj(Escape.escapeHelical(type, tok,
          pta, ptb, data)));
    }
    BS bs = (args[0].value instanceof BS ? (BS) args[0].value : vwr.ms
        .getAtoms(T.resno, new Integer(args[0].asInt())));
    switch (tok) {
    case T.point:
    case T.axis:
    case T.radius:
      return mp.addXObj(getHelixData(bs, tok));
    case T.angle:
      return mp.addXFloat(((Float) getHelixData(bs, T.angle)).floatValue());
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
        : vwr.ms.at[iAtom].group.getHelixData(tokType, 
        vwr.getQuaternionFrame(), vwr.getInt(T.helixstep)));
  }
  
  private boolean evaluateFind(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {

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
    // {*}.find("SMARTS", "CCCC")
    // "CCCC".find("SMARTS", "CC")
    // "CCCC".find("SMILES", "MF")
    // {2.1}.find("CCCC",{1.1}) // find pattern "CCCC" in {2.1} with conformation given by {1.1}
    // {*}.find("ccCCN","BONDS")
    // {*}.find("SMILES","H")
    // {*}.find("chemical",type)
    // {1.1}.find("SMILES", {2.1})
    // {1.1}.find("SMARTS", {2.1})

    SV x1 = mp.getX();
    boolean isList = (x1.tok == T.varray);
    boolean isEmpty = (args.length == 0);
    String sFind = (isEmpty ? "" : SV.sValue(args[0]));
    String flags = (args.length > 1 && args[1].tok != T.on
        && args[1].tok != T.off  && args[1].tok != T.bitset ? SV.sValue(args[1]) : "");
    boolean isSequence = !isList && sFind.equalsIgnoreCase("SEQUENCE");
    boolean isSeq = !isList && sFind.equalsIgnoreCase("SEQ");
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

    boolean isSmiles = !isList && sFind.equalsIgnoreCase("SMILES");
    boolean isSMARTS = !isList && sFind.equalsIgnoreCase("SMARTS");
    boolean isChemical = !isList && sFind.equalsIgnoreCase("CHEMICAL");
    boolean isMF = !isList && sFind.equalsIgnoreCase("MF");
    boolean isCF = !isList && sFind.equalsIgnoreCase("CELLFORMULA");
    SV argLast = (args.length > 0 ? args[args.length - 1] : SV.vF);
    boolean isON = !isList && (argLast.tok == T.on);
    try {
      if (isChemical) {
        BS bsAtoms = (x1.tok == T.bitset ? (BS) x1.value : null);
        String data = (bsAtoms == null ? SV.sValue(x1) : vwr.getOpenSmiles(bsAtoms));
        data = (data.length() == 0 ? "" : vwr.getChemicalInfo(data,
            flags.toLowerCase(), bsAtoms)).trim();
        if (data.startsWith("InChI"))
          data = PT.rep(PT.rep(data, "InChI=", ""), "InChIKey=", "");
        return mp.addXStr(data);
      }
      if (isSmiles || isSMARTS || x1.tok == T.bitset) {
        int iPt = (isSmiles || isSMARTS ? 2 : 1);
        BS bs2 = (iPt < args.length && args[iPt].tok == T.bitset ? (BS) args[iPt++].value
            : null);
        boolean asBonds = ("bonds".equalsIgnoreCase(SV
            .sValue(args[args.length - 1])));
        boolean isAll = (asBonds || isON);
        Object ret = null;
        switch (x1.tok) {
        case T.string:
          String smiles = SV.sValue(x1);
          if (bs2 != null || isSmiles && args.length == 1)
            return false;
          if (flags.equalsIgnoreCase("mf")) {
            ret = vwr.getSmilesMatcher().getMolecularFormula(smiles, isSMARTS);
          } else {
            String pattern = flags;
            // "SMARTS",flags,asMap, allMappings
            boolean allMappings = true;
            boolean asMap = false;
            switch (args.length) {
            case 4:
              allMappings = SV.bValue(args[3]);
              //$FALL-THROUGH$
            case 3:
              asMap = SV.bValue(args[2]);
              break;
            }
            boolean justOne = (!asMap && (!allMappings || !isSMARTS && !pattern.equals("chirality")));
            try {
              ret = e.getSmilesExt().getSmilesMatches(pattern, smiles, null,
                  null,
                  isSMARTS ? JC.SMILES_TYPE_SMARTS : JC.SMILES_TYPE_SMILES,
                  !asMap, !allMappings);
            } catch (Exception ex) {
              System.out.println(ex.getMessage());
              return mp.addXInt(-1);
            }
            if (justOne) {
              int len = ((int[]) ret).length;
              return mp.addXInt(!allMappings && len > 0 ? 1 : len);
            }
          }
          break;
        case T.bitset:
          BS bs = (BS) x1.value;
          if (isMF && flags.length() != 0)
            return mp.addXBs(JmolMolecule.getBitSetForMF(vwr.ms.at, bs, flags));
          if (isMF || isCF)
            return mp.addXStr(JmolMolecule.getMolecularFormulaAtoms(vwr.ms.at, bs,
                 (isMF ? null : vwr.ms.getCellWeights(bs)), isON));
          if (isSequence || isSeq) {
            boolean isHH = (argLast.asString().equalsIgnoreCase("H"));
            isAll |= isHH;
            return mp.addXStr(vwr
                .getSmilesOpt(bs, -1, -1,
                    (isAll ? JC.SMILES_GEN_BIO_ALLOW_UNMATCHED_RINGS
                        | JC.SMILES_GEN_BIO_COV_CROSSLINK
                        | (isHH ? JC.SMILES_GEN_BIO_HH_CROSSLINK : 0) : 0)
                        | (isSeq ? JC.SMILES_GEN_BIO_NOCOMMENTS
                            : JC.SMILES_GEN_BIO), null));
          }
          if (isSmiles || isSMARTS)
            sFind = (args.length > 1 && args[1].tok == T.bitset ? vwr
                .getSmilesOpt((BS) args[1].value, 0, 0, 0, flags) : flags);
          flags = flags.toUpperCase();
          BS bsMatch3D = bs2;
          if (asBonds) {
            // this will return a single match
            int[][] map = vwr.getSmilesMatcher().getCorrelationMaps(
                sFind,
                vwr.ms.at,
                vwr.ms.ac,
                bs,
                (isSmiles ? JC.SMILES_TYPE_SMILES : JC.SMILES_TYPE_SMARTS)
                    | JC.SMILES_FIRST_MATCH_ONLY);
            ret = (map.length > 0 ? vwr.ms.getDihedralMap(map[0]) : new int[0]);
          } else if (flags.equalsIgnoreCase("map")) {
            int[][] map = vwr.getSmilesMatcher().getCorrelationMaps(
                sFind,
                vwr.ms.at,
                vwr.ms.ac,
                bs,
                (isSmiles ? JC.SMILES_TYPE_SMILES : JC.SMILES_TYPE_SMARTS)
                    | JC.SMILES_MAP_UNIQUE);
            ret = map;
          } else if (sFind.equalsIgnoreCase("crystalClass")) {
            // {*}.find("crystalClass")
            // {*}.find("crystalClass", pt)
            ret = vwr.ms
                .generateCrystalClass(
                    bs.nextSetBit(0),
                    (args.length != 2 ? null : argLast.tok == T.bitset ? vwr.ms
                        .getAtomSetCenter((BS) argLast.value) : SV
                        .ptValue(argLast)));
          } else {
            int smilesFlags = (isSmiles ?

            (flags.indexOf("OPEN") >= 0 ? JC.SMILES_TYPE_OPENSMILES
                : JC.SMILES_TYPE_SMILES) : JC.SMILES_TYPE_SMARTS)
                | (isON && sFind.length() == 0 ? JC.SMILES_GEN_BIO_COV_CROSSLINK
                    | JC.SMILES_GEN_BIO_COMMENT
                    : 0);
            if (flags.indexOf("/MOLECULE/") >= 0) {
              // all molecules 
              JmolMolecule[] mols = vwr.ms.getMolecules();
              Lst<BS> molList = new Lst<BS>();
              for (int i = 0; i < mols.length; i++) {
                if (mols[i].atomList.intersects(bs)) {
                  BS bsRet = (BS) e.getSmilesExt().getSmilesMatches(sFind, null, mols[i].atomList, bsMatch3D,
                      smilesFlags, !isON, false);
                  if (!bsRet.isEmpty())
                    molList.addLast(bsRet);
                }
              }
              ret = molList;
            } else {
              ret = e.getSmilesExt().getSmilesMatches(sFind, null, bs, bsMatch3D,
                  smilesFlags, !isON, false);
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
    boolean isReverse = (flags.indexOf("v") >= 0);
    boolean isCaseInsensitive = (flags.indexOf("i") >= 0);
    boolean asMatch = (flags.indexOf("m") >= 0);
    boolean checkEmpty = (sFind.length() == 0);
    boolean isPattern = (!checkEmpty && args.length == 2);
    if (isList || isPattern) {
      JmolPatternMatcher pm = (isPattern ? getPatternMatcher() : null);
      Pattern pattern = null;
      Lst<SV> svlist = (isList ? x1.getList() : null);
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
      BS bs = new BS();
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
            v.addLast(isReverse ? what.substring(0, matcher.start())
                + what.substring(matcher.end()) : matcher.group());
        }
      }
      if (!isList) {
        return (asMatch ? mp.addXStr(v.size() == 1 ? (String) v.get(0) : "")
            : isReverse ? mp.addXBool(n == 1) : asMatch ? mp
                .addXStr(n == 0 ? "" : matcher.group()) : mp.addXInt(n == 0 ? 0
                : matcher.start() + 1));
      }
      // removed in 14.2/3.14 -- not documented and not expected      if (n == 1)
      //    return mp.addXStr(asMatch ? (String) v.get(0) : list[ipt]);

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
  private boolean evaluateGetProperty(ScriptMathProcessor mp, SV[] args,
                                      int tok0, boolean isAtomProperty)
      throws ScriptException {
    boolean isSelect = (isAtomProperty && tok0 == T.select);
    boolean isAuxiliary = (tok0 == T.__);
    int pt = 0;
    int tok = (args.length == 0 ? T.nada : args[0].tok);
    if (args.length == 2
        && (tok == T.varray || tok == T.hash || tok == T.context)) {
      return mp.addXObj(vwr.extractProperty(args[0].value,
          args[1].value.toString(), -1));
    }
    BS bsSelect = (isAtomProperty && args.length == 1 && args[0].tok == T.bitset ? (BS) args[0].value : null);
    String pname = (bsSelect == null && args.length > 0 ? SV.sValue(args[pt++]) : "");
    String propertyName = pname;
    String lc = propertyName.toLowerCase();
    if (!isSelect && lc.indexOf("[select ") < 0)
      propertyName = lc;
    boolean isJSON = false;
    if (propertyName.equals("json") && args.length > pt) {
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
                data[1] = vwr.shm.getShapePropertyIndex(shapeID,
                    pname.intern(), index);
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
        if (bsSelect != null) {
          Lst<SV> l0 = x.getList();
          Lst<SV> lst = new Lst<SV>();
          for (int i = bsSelect.nextSetBit(0); i >= 0; i = bsSelect.nextSetBit(i + 1))
            lst.addLast(l0.get(i));
          return mp.addXList(lst);
        }
        //$FALL-THROUGH$
      default:
        if (isSelect)
          propertyName = "[SELECT " + propertyName + "]";
        return mp.addXObj(vwr.extractProperty(x, propertyName, -1));
      }
      if (!lc.startsWith("bondinfo")
          && !lc.startsWith("atominfo"))
        propertyName = "atomInfo." + propertyName;
    }
    Object propertyValue = "";
    if (propertyName.equalsIgnoreCase("fileContents") && args.length > 2) {
      String s = SV.sValue(args[1]);
      for (int i = 2; i < args.length; i++)
        s += "|" + SV.sValue(args[i]);
      propertyValue = s;
      pt = args.length;
    } else if (args.length > pt) {
      switch (args[pt].tok) {
      case T.bitset:
        propertyValue = args[pt++].value;
        if (propertyName.equalsIgnoreCase("bondInfo") && args.length > pt
            && args[pt].tok == T.bitset)
          propertyValue = new BS[] { (BS) propertyValue,
              (BS) args[pt].value };
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
    if (pt < args.length)
      property = vwr.extractProperty(property, args, pt);
    return mp.addXObj(isJSON ? SV.safeJSON("value", property) : SV
        .isVariableType(property) ? property : Escape.toReadable(propertyName,
        property));
  }

  private boolean evaluateFormat(ScriptMathProcessor mp, int intValue,
                                 SV[] args, boolean isLabel)
      throws ScriptException {
    // NOT {xxx}.label
    // {xxx}.label("....")
    // {xxx}.yyy.format("...")
    // (value).format("...")
    // format("....",a,b,c...)
    // format("....",[a1, a2, a3, a3....])
    // format("base64", x)
    // format("JSON", x)
    // format("byteArray", x)
    // format("array", x)
    SV x1 = (args.length < 2 || intValue == T.format ? mp.getX() : null);
    String format = (args.length == 0 ? "%U" : args[0].tok == T.varray ? null : SV.sValue(args[0]));
    if (!isLabel && args.length > 0 && x1 != null && x1.tok != T.bitset && format != null) {
      // x1.format(["energy", "pointGroup"]);
      // x1.format("%5.3f %5s", ["energy", "pointGroup"])
      // but not x1.format() or {*}.format(....)
      if (args.length == 2) {
        Lst<SV> listIn = x1.getList();
        Lst<SV> formatList = args[1].getList();
        if (listIn == null  || formatList == null)
          return false;
        x1 = SV.getVariableList(getSublist(listIn, formatList));
      }
      args = new SV[] {args[0], x1};
      x1 = null;
    }
    if (x1 == null) {
      int pt = (isLabel ? -1 : SV.getFormatType(format));
      if (pt >= 0 && args.length != 2)
        return false;
      if (pt >= 0 || args.length < 2 || args[1].tok != T.varray) {
        Object o = SV.format(args, pt); 
        return (format.equalsIgnoreCase("json") ? mp.addXStr((String)o) : mp.addXObj(o));
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
    if (x1.tok == T.varray && format == null) {
      Lst<SV> listIn = x1.getList();
      Lst<SV> formatList = args[0].getList();
      Lst<SV> listOut = getSublist(listIn, formatList);
      return mp.addXList(listOut);
    }
    
    BS bs = (x1.tok == T.bitset ? (BS) x1.value : null);
    boolean asArray = T.tokAttr(intValue, T.minmaxmask); // "all"
    return mp.addXObj(format == null ? "" : bs == null ? SV.sprintf(PT.formatCheck(format), x1) : e
        .getCmdExt().getBitsetIdent(bs, format, x1.value, true, x1.index,
            asArray));
    
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
        for (int j = 0, n1 = Math.min(list.size(), formatList.size()); j < n1; j++) {
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

    int len = args.length;
    SV x1 = mp.getX();
    boolean isArray1 = (x1.tok == T.varray);
    SV x2;
    switch (tok) {
    case T.push:
      return (len == 2 && mp.addX(x1.pushPop(args[0], args[1])) || len == 1
          && mp.addX(x1.pushPop(null, args[0])));
    case T.pop:
      return (len == 1 && mp.addX(x1.pushPop(args[0], null)) || len == 0
          && mp.addX(x1.pushPop(null, null)));
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
    String[] sList1 = null, sList2 = null, sList3 = null;

    if (len == 2) {
      // special add, join, split
      String tab = SV.sValue(args[0]);
      x2 = args[1];
      if (tok == T.add) {
        // [...].add("\t", [...])
        sList1 = (isArray1 ? SV.strListValue(x1) : PT
            .split(SV.sValue(x1), "\n"));
        sList2 = (x2.tok == T.varray ? SV.strListValue(x2) : PT.split(
            SV.sValue(x2), "\n"));
        sList3 = new String[len = Math.max(sList1.length, sList2.length)];
        for (int i = 0; i < len; i++)
          sList3[i] = (i >= sList1.length ? "" : sList1[i]) + tab
              + (i >= sList2.length ? "" : sList2[i]);
        return mp.addXAS(sList3);
      }
      if (x2.tok != T.on)
        return false; // second parameter must be "true" for now.
      Lst<SV> l = x1.getList();
      boolean isCSV = (tab.length() == 0);
      if (isCSV)
        tab = ",";
      if (tok == T.join) {
        // [...][...].join(sep, true) [2D-array line join]
        // [...][...].join("", true)  [CSV join]
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
              sb.append(isCSV && sv.tok == T.string ? "\""
                  + PT.rep((String) sv.value, "\"", "\"\"") + "\"" : ""
                  + sv.asString());
            }
            s2[i] = SV.newS(sb.toString());
          }
        }
        return mp.addXAV(s2);
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
              la.addLast(SV.getVariable(Float.valueOf(Float.parseFloat(s))));
              continue;
            } catch (Exception ee) {
            }
          la.addLast(SV.newS(s));
        }
        sa.addLast(SV.getVariableList(la));
      }
      return mp.addXObj(SV.getVariableList(sa));
    }
    x2 = (len == 0 ? SV.newV(T.all, "all") : args[0]);
    boolean isAll = (x2.tok == T.all);
    if (!isArray1 && x1.tok != T.string)
      return mp.binaryOp(opTokenFor(tok), x1, x2);
    boolean isScalar1 = SV.isScalar(x1);
    boolean isScalar2 = SV.isScalar(x2);

    float[] list1 = null;
    float[] list2 = null;
    Lst<SV> alist1 = x1.getList();
    Lst<SV> alist2 = x2.getList();

    if (isArray1) {
      len = alist1.size();
    } else if (isScalar1) {
      len = Integer.MAX_VALUE;
    } else {
      sList1 = (PT.split(SV.sValue(x1), "\n"));
      list1 = new float[len = sList1.length];
      PT.parseFloatArrayData(sList1, list1);
    }
    if (isAll && tok != T.join) {
      float sum = 0f;
      if (isArray1) {
        for (int i = len; --i >= 0;)
          sum += SV.fValue(alist1.get(i));
      } else if (!isScalar1) {
        for (int i = len; --i >= 0;)
          sum += list1[i];
      }
      return mp.addXFloat(sum);
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
      list2 = new float[sList2.length];
      PT.parseFloatArrayData(sList2, list2);
      len = Math.min(len, list2.length);
    }

    T token = opTokenFor(tok);

    SV[] olist = new SV[len];
    if (isArray1 && isAll) {
      Lst<SV> llist = new Lst<SV>();
      return mp.addXList(addAllLists(x1.getList(), llist));
    }
    SV a = (isScalar1 ? x1 : null);
    SV b;
    for (int i = 0; i < len; i++) {
      if (isScalar2)
        b = scalar;
      else if (x2.tok == T.varray)
        b = alist2.get(i);
      else if (Float.isNaN(list2[i]))
        b = SV.getVariable(SV.unescapePointOrBitsetAsVariable(sList2[i]));
      else
        b = SV.newF(list2[i]);
      if (!isScalar1) {
        if (isArray1)
          a = alist1.get(i);
        else if (Float.isNaN(list1[i]))
          a = SV.getVariable(SV.unescapePointOrBitsetAsVariable(sList1[i]));
        else
          a = SV.newF(list1[i]);
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
    return mp.addXAV(olist);
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

  private boolean evaluateLoad(ScriptMathProcessor mp, SV[] args, boolean isFile)
      throws ScriptException {
    // file("myfile.xyz")
    // load("myfile.png",true)
    // load("myfile.txt",1000)
    // load("myfile.xyz",0,true)
    // load("myfile.json","JSON")
    // load("myfile.json","JSON", true)

    if (args.length < 1 || args.length > 3)
      return false;
    String file = FileManager.fixDOSName(SV.sValue(args[0]));
    boolean asBytes = (args.length > 1 && args[1].tok == T.on);
    boolean async = (vwr.async || args.length > 2 && args[args.length - 1].tok == T.on);
    int nBytesMax = (args.length > 1 && args[1].tok == T.integer ? args[1].asInt() : -1);
    boolean asJSON = (args.length > 1 && args[1].asString().equalsIgnoreCase("JSON"));
    if (asBytes)
      return mp.addXMap(vwr.fm.getFileAsMap(file, null));
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
    String str = isFile ? vwr.fm.getFilePath(file, false, false) : vwr
        .getFileAsString4(file, nBytesMax, false, false, true, "script");
    try {
      return (asJSON ? mp.addXObj(vwr.parseJSON(str)) : mp.addXStr(str));
    } catch (Exception e) {
      return false;
    }
  }

  private boolean evaluateMath(ScriptMathProcessor mp, SV[] args, int tok) {
    if (tok == T.now) {
      if (args.length == 1 && args[0].tok == T.string)
        return mp.addXStr((new Date()) + "\t" + SV.sValue(args[0]));
      return mp.addXInt(((int) System.currentTimeMillis() & 0x7FFFFFFF)
          - (args.length == 0 ? 0 : args[0].asInt()));
    }
    if (args.length != 1)
      return false;
    if (tok == T.abs) {
      if (args[0].tok == T.integer)
        return mp.addXInt(Math.abs(args[0].asInt()));
      return mp.addXFloat(Math.abs(args[0].asFloat()));
    }
    double x = SV.fValue(args[0]);
    switch (tok) {
    case T.acos:
      return mp.addXFloat((float) (Math.acos(x) * 180 / Math.PI));
    case T.cos:
      return mp.addXFloat((float) Math.cos(x * Math.PI / 180));
    case T.sin:
      return mp.addXFloat((float) Math.sin(x * Math.PI / 180));
    case T.sqrt:
      return mp.addXFloat((float) Math.sqrt(x));
    }
    return false;
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
      // measure({a},{b},{c},{d}, min, max, format, units)
      // measure({a},{b},{c}, min, max, format, units)
      // measure({a},{b}, min, max, format, units)
      // measure({a},{b},{c},{d}, min, max, format, units)
      // measure({a} {b} "minArray") -- returns array of minimum distance values

      Lst<Object> points = new Lst<Object>();
      float[] rangeMinMax = new float[] { Float.MAX_VALUE, Float.MAX_VALUE };
      String strFormat = null;
      String units = null;
      boolean isAllConnected = false;
      boolean isNotConnected = false;
      int rPt = 0;
      boolean isNull = false;
      RadiusData rd = null;
      int nBitSets = 0;
      float vdw = Float.MAX_VALUE;
      boolean asMinArray = false;
      boolean asArray = false;
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
          v.setT((P3) args[i].value);
          points.addLast(v);
          nPoints++;
          break;
        case T.integer:
        case T.decimal:
          rangeMinMax[rPt++ % 2] = SV.fValue(args[i]);
          break;

        case T.string:
          String s = SV.sValue(args[i]);
          if (s.equalsIgnoreCase("vdw") || s.equalsIgnoreCase("vanderwaals"))
            vdw = (i + 1 < args.length && args[i + 1].tok == T.integer ? args[++i]
                .asInt() : 100) / 100f;
          else if (s.equalsIgnoreCase("notConnected"))
            isNotConnected = true;
          else if (s.equalsIgnoreCase("connected"))
            isAllConnected = true;
          else if (s.equalsIgnoreCase("minArray"))
            asMinArray = (nBitSets >= 1);
          else if (s.equalsIgnoreCase("asArray"))
            asArray = (nBitSets >= 1);
          else if (PT.isOneOf(s.toLowerCase(),
              ";nm;nanometers;pm;picometers;angstroms;ang;au;")
              || s.endsWith("hz"))
            units = s.toLowerCase();
          else
            strFormat = nPoints + ":" + s;
          break;
        default:
          return false;
        }
      }
      if (nPoints < 2 || nPoints > 4 || rPt > 2 || isNotConnected
          && isAllConnected)
        return false;
      if (isNull)
        return mp.addXStr("");
      if (vdw != Float.MAX_VALUE && (nBitSets != 2 || nPoints != 2))
        return mp.addXStr("");
      rd = (vdw == Float.MAX_VALUE ? new RadiusData(rangeMinMax, 0, null, null)
          : new RadiusData(null, vdw, EnumType.FACTOR, VDW.AUTO));
      return mp.addXObj((vwr.newMeasurementData(null, points)).set(0, null, rd,
          strFormat, units, null, isAllConnected, isNotConnected, null, true,
          0, (short) 0, null).getMeasurements(asArray, asMinArray));
    case T.angle:
      if ((nPoints = args.length) != 3 && nPoints != 4)
        return false;
      break;
    default: // distance
      if ((nPoints = args.length) != 2)
        return false;
    }
    P3[] pts = new P3[nPoints];
    for (int i = 0; i < nPoints; i++) {
      if ((pts[i] = mp.ptValue(args[i], null)) == null)
        return false;
    }
    switch (nPoints) {
    case 2:
      return mp.addXFloat(pts[0].distance(pts[1]));
    case 3:
      return mp
          .addXFloat(Measure.computeAngleABC(pts[0], pts[1], pts[2], true));
    case 4:
      return mp.addXFloat(Measure.computeTorsion(pts[0], pts[1], pts[2],
          pts[3], true));
    }
    return false;
  }

  private boolean evaluateModulation(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {
    String type = "";
    float t = Float.NaN;
    P3 t456 = null;
    switch (args.length) {
    case 0:
      break;
    case 1:
      switch (args[0].tok) {
      case T.point3f:
        t456 = (P3) args[0].value;
        break;
      case T.string:
        type = args[0].asString();
        break;
      default:
        t = SV.fValue(args[0]);
      }
      break;
    case 2:
      type = SV.sValue(args[0]);
      t = SV.fValue(args[1]);
      break;
    default:
      return false;
    }
    if (t456 == null && t < 1e6)
      t456 = P3.new3(t, t, t);
    SV x = mp.getX();
    BS bs = (x.tok == T.bitset ? (BS) x.value : new BS());
    return mp.addXList(vwr.ms.getModulationList(bs,
        (type + "D").toUpperCase().charAt(0), t456));
  }

  /**
   * plane() or intersection()  
   * 
   * @param mp
   * @param args
   * @param tok
   * @return true
   * @throws ScriptException
   */
  private boolean evaluatePlane(ScriptMathProcessor mp, SV[] args, int tok)
      throws ScriptException {
    if (tok == T.hkl && args.length != 3 || tok == T.intersection
        && args.length != 2 && args.length != 3 || args.length == 0
        || args.length > 4)
      return false;
    P3 pt1, pt2, pt3;
    P4 plane;
    V3 norm, vTemp;

    switch (args.length) {
    case 1:
      if (args[0].tok == T.bitset) {
        BS bs = (BS) args[0].value;
        if (bs.cardinality() == 3) {
          Lst<P3> pts = vwr.ms.getAtomPointVector(bs);
          return mp.addXPt4(Measure.getPlaneThroughPoints(pts.get(0), pts.get(1), pts.get(2),
              new V3(), new V3(), new P4()));
        }
      }
      Object pt = Escape.uP(SV.sValue(args[0]));
      if (pt instanceof P4)
        return mp.addXPt4((P4) pt);
      return mp.addXStr("" + pt);
    case 2:
      if (tok == T.intersection) {
        // intersection(plane, plane)
        // intersection(point, plane)
        if (args[1].tok != T.point4f)
          return false;
        pt3 = new P3();
        norm = new V3();
        vTemp = new V3();

        plane = (P4) args[1].value;
        if (args[0].tok == T.point4f) {
          Lst<Object> list = Measure.getIntersectionPP((P4) args[0].value,
              plane);
          if (list == null)
            return mp.addXStr("");
          return mp.addXList(list);
        }
        pt2 = mp.ptValue(args[0], null);
        if (pt2 == null)
          return mp.addXStr("");
        return mp.addXPt(Measure.getIntersection(pt2, null, plane, pt3, norm,
            vTemp));
      }
      //$FALL-THROUGH$
    case 3:
    case 4:
      switch (tok) {
      case T.hkl:
        // hkl(i,j,k)
        return mp.addXPt4(e.getHklPlane(P3.new3(SV.fValue(args[0]),
            SV.fValue(args[1]), SV.fValue(args[2]))));
      case T.intersection:
        pt1 = mp.ptValue(args[0], null);
        pt2 = mp.ptValue(args[1], null);
        if (pt1 == null || pt2 == null)
          return mp.addXStr("");
        V3 vLine = V3.newV(pt2);
        vLine.normalize();
        if (args[2].tok == T.point4f) {
          // intersection(ptLine, vLine, plane)
          pt3 = new P3();
          norm = new V3();
          vTemp = new V3();
          pt1 = Measure.getIntersection(pt1, vLine, (P4) args[2].value, pt3,
              norm, vTemp);
          if (pt1 == null)
            return mp.addXStr("");
          return mp.addXPt(pt1);
        }
        pt3 = mp.ptValue(args[2], null);
        if (pt3 == null)
          return mp.addXStr("");
        // intersection(ptLine, vLine, pt2); 
        // IE intersection of plane perp to line through pt2
        V3 v = new V3();
        Measure.projectOntoAxis(pt3, pt1, vLine, v);
        return mp.addXPt(pt3);
      }
      switch (args[0].tok) {
      case T.integer:
      case T.decimal:
        if (args.length == 3) {
          // plane(r theta phi)
          float r = SV.fValue(args[0]);
          float theta = SV.fValue(args[1]); // longitude, azimuthal, in xy plane
          float phi = SV.fValue(args[2]); // 90 - latitude, polar, from z
          // rotate {0 0 r} about y axis need to stay in the x-z plane
          norm = V3.new3(0, 0, 1);
          pt2 = P3.new3(0, 1, 0);
          Quat q = Quat.newVA(pt2, phi);
          q.getMatrix().rotate(norm);
          // rotate that vector around z
          pt2.set(0, 0, 1);
          q = Quat.newVA(pt2, theta);
          q.getMatrix().rotate(norm);
          pt2.setT(norm);
          pt2.scale(r);
          plane = new P4();
          Measure.getPlaneThroughPoint(pt2, norm, plane);
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
            && (args[2].tok == T.bitset || args[2].tok == T.point3f) ? mp
            .ptValue(args[2], null) : null);
        norm = V3.newV(pt2);
        if (pt3 == null) {
          plane = new P4();
          if (args.length == 2 || args[2].tok != T.integer && args[2].tok != T.decimal && !args[2].asBoolean()) {
            // plane(<point1>,<point2>) or 
            // plane(<point1>,<point2>,false)
            pt3 = P3.newP(pt1);
            pt3.add(pt2);
            pt3.scale(0.5f);
            norm.sub(pt1);
            norm.normalize();
          } else if (args[2].tok == T.on){
            // plane(<point1>,<vLine>,true)
            pt3 = pt1;
          } else {
            // plane(<point1>,<point2>,f)
            norm.sub(pt1);
            pt3 = new P3();
            pt3.scaleAdd2(args[2].asFloat(), norm, pt1);
          }
          Measure.getPlaneThroughPoint(pt3, norm, plane);
          return mp.addXPt4(plane);
        }
        // plane(<point1>,<point2>,<point3>)
        // plane(<point1>,<point2>,<point3>,<pointref>)
        V3 vAB = new V3();
        P3 ptref = (args.length == 4 ? mp.ptValue(args[3], null) : null);
        float nd = Measure.getDirectedNormalThroughPoints(pt1, pt2, pt3,
            ptref, norm, vAB);
        return mp.addXPt4(P4.new4(norm.x, norm.y, norm.z, nd));
      }
    }
    if (args.length != 4)
      return false;
    float x = SV.fValue(args[0]);
    float y = SV.fValue(args[1]);
    float z = SV.fValue(args[2]);
    float w = SV.fValue(args[3]);
    return mp.addXPt4(P4.new4(x, y, z, w));
  }

  private boolean evaluatePoint(ScriptMathProcessor mp, SV[] args) {
    // point(1.3)  // rounds toward 0
    // point(pt, true) // to screen coord 
    // point(pt, false) // from screen coord
    // point(x, y, z)
    // point(x, y, z, w)
    
    switch (args.length) {
    default:
      return false;
    case 1:
      if (args[0].tok == T.decimal || args[0].tok == T.integer)
        return mp.addXInt(args[0].asInt());
      String s = SV.sValue(args[0]);
      if (args[0].tok == T.varray)
        s = "{" + s + "}";
      Object pt = Escape.uP(s);
      return (pt instanceof P3 ? mp.addXPt((P3) pt) : mp.addXStr("" + pt));
    case 2:
      P3 pt3;
      switch (args[1].tok) {
      case T.off:
      case T.on:
        // to/from screen coordinates
        switch (args[0].tok) {
        case T.point3f:
          pt3 = P3.newP((T3) args[0].value);
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
            pt3.scale(0.5f);
        } else {
          // this is FROM screen coordinates
          if (vwr.antialiased)
            pt3.scale(2f);
          pt3.y = vwr.tm.height - pt3.y;
          vwr.tm.unTransformPoint(pt3, pt3);          
        }
        break;
      case T.point3f:
        // unitcell transform
        Lst<SV> sv = args[0].getList();
        if (sv == null || sv.size() != 4)
          return false;
        P3 pt1 = SV.ptValue(args[1]);
        pt3 = P3.newP(SV.ptValue(sv.get(0)));
        pt3.scaleAdd2(pt1.x, SV.ptValue(sv.get(1)), pt3);
        pt3.scaleAdd2(pt1.y, SV.ptValue(sv.get(2)), pt3);
        pt3.scaleAdd2(pt1.z, SV.ptValue(sv.get(3)), pt3);
        break;
      default:
        return false;
      }
      return mp.addXPt(pt3);      
    case 3:
      return mp.addXPt(P3.new3(args[0].asFloat(), args[1].asFloat(),
          args[2].asFloat()));
    case 4:
      return mp.addXPt4(P4.new4(args[0].asFloat(), args[1].asFloat(),
          args[2].asFloat(), args[3].asFloat()));
    }
  }

  private boolean evaluatePrompt(ScriptMathProcessor mp, SV[] args) {
    //x = prompt("testing")
    //x = prompt("testing","defaultInput")
    //x = prompt("testing","yes|no|cancel", true)
    //x = prompt("testing",["button1", "button2", "button3"])

    if (args.length != 1 && args.length != 2 && args.length != 3)
      return false;
    String label = SV.sValue(args[0]);
    String[] buttonArray = (args.length > 1 && args[1].tok == T.varray ? SV
        .strListValue(args[1]) : null);
    boolean asButtons = (buttonArray != null || args.length == 1 || args.length == 3
        && args[2].asBoolean());
    String input = (buttonArray != null ? null : args.length >= 2 ? SV
        .sValue(args[1]) : "OK");
    String s = "" + vwr.prompt(label, input, buttonArray, asButtons);
    return (asButtons && buttonArray != null ? mp
        .addXInt(Integer.parseInt(s) + 1) : mp.addXStr(s));
  }

  private boolean evaluateQuaternion(ScriptMathProcessor mp, SV[] args, int tok)
      throws ScriptException {
    P3 pt0 = null;
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
      if ((pt0 = mp.ptValue(args[0], null)) == null || tok != T.quaternion
          && args[1].tok == T.point3f)
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
    Quat q = null;
    Quat[] qs = null;
    P4 p4 = null;
    switch (nArgs) {
    case 0:
      return mp.addXPt4(vwr.tm.getRotationQ().toPoint4f());
    case 1:
    default:
      if (tok == T.quaternion && args[0].tok == T.varray) {
        Quat[] data1 = e.getQuaternionArray(args[0].getList(), T.list);
        Object mean = Quat.sphereMean(data1, null, 0.0001f);
        q = (mean instanceof Quat ? (Quat) mean : null);
        break;
      } else if (tok == T.quaternion && args[0].tok == T.bitset) {
        qs = vwr.getAtomGroupQuaternions((BS) args[0].value, nMax);
      } else if (args[0].tok == T.matrix3f) {
        q = Quat.newM((M3) args[0].value);
      } else if (args[0].tok == T.point4f) {
        p4 = (P4) args[0].value;
      } else {
        String s = SV.sValue(args[0]);
        Object v = Escape.uP(s.equalsIgnoreCase("best") ? vwr
            .getOrientationText(T.best, "best", null).toString() : s);
        if (!(v instanceof P4))
          return false;
        p4 = (P4) v;
      }
      if (tok == T.axisangle)
        q = Quat.newVA(P3.new3(p4.x, p4.y, p4.z), p4.w);
      break;
    case 2:
      if (tok == T.quaternion) {
        if (args[0].tok == T.varray && args[1].tok == T.varray) {
          Quat[] data1 = e.getQuaternionArray(args[0].getList(), T.list);
          Quat[] data2 = e.getQuaternionArray(args[1].getList(), T.list);
          qs = Quat.div(data2, data1, nMax, isRelative);
          break;
        }
        if (args[0].tok == T.varray && args[1].tok == T.on) {
          Quat[] data1 = e.getQuaternionArray(args[0].getList(), T.list);
          float[] stddev = new float[1];
          Quat.sphereMean(data1, stddev, 0.0001f);
          return mp.addXFloat(stddev[0]);
        }
        if (args[0].tok == T.bitset && args[1].tok == T.bitset) {
          Quat[] data1 = vwr.getAtomGroupQuaternions((BS) args[0].value,
              Integer.MAX_VALUE);
          Quat[] data2 = vwr.getAtomGroupQuaternions((BS) args[1].value,
              Integer.MAX_VALUE);
          qs = Quat.div(data2, data1, nMax, isRelative);
          break;
        }
      }
      P3 pt1 = mp.ptValue(args[1], null);
      p4 = mp.planeValue(args[0]);
      if (pt1 != null)
        q = Quat.getQuaternionFrame(P3.new3(0, 0, 0), pt0, pt1);
      else
        q = Quat.newVA(pt0, SV.fValue(args[1]));
      break;
    case 3:
      if (args[0].tok == T.point4f) {
        P3 pt = (args[2].tok == T.point3f ? (P3) args[2].value : vwr
            .ms.getAtomSetCenter((BS) args[2].value));
        return mp.addXStr(Escape.drawQuat(Quat.newP4((P4) args[0].value), "q",
            SV.sValue(args[1]), pt, 1f));
      }
      P3[] pts = new P3[3];
      for (int i = 0; i < 3; i++)
        pts[i] = (args[i].tok == T.point3f ? (P3) args[i].value : vwr
            .ms.getAtomSetCenter((BS) args[i].value));
      q = Quat.getQuaternionFrame(pts[0], pts[1], pts[2]);
      break;
    case 4:
      if (tok == T.quaternion)
        p4 = P4.new4(SV.fValue(args[1]), SV.fValue(args[2]),
            SV.fValue(args[3]), SV.fValue(args[0]));
      else
        q = Quat
            .newVA(
                P3.new3(SV.fValue(args[0]), SV.fValue(args[1]),
                    SV.fValue(args[2])), SV.fValue(args[3]));
      break;
    }
    if (qs != null) {
      if (nMax != Integer.MAX_VALUE) {
        Lst<P4> list = new Lst<P4>();
        for (int i = 0; i < qs.length; i++)
          list.addLast(qs[i].toPoint4f());
        return mp.addXList(list);
      }
      q = (qs.length > 0 ? qs[0] : null);
    }
    return mp.addXPt4((q == null ? Quat.newP4(p4) : q).toPoint4f());
  }

  private Random rand;

  private boolean evaluateRandom(ScriptMathProcessor mp, SV[] args) {
    if (args.length > 3)
      return false;
    if (rand == null)
      rand = new Random();
    float lower = 0, upper = 1;
    switch (args.length) {
    case 3:
      rand.setSeed((int) SV.fValue(args[2]));
      //$FALL-THROUGH$
    case 2:
      upper = SV.fValue(args[1]);
      //$FALL-THROUGH$
    case 1:
      lower = SV.fValue(args[0]);
      //$FALL-THROUGH$
    case 0:
      break;
    default:
      return false;
    }
    return mp.addXFloat((rand.nextFloat() * (upper - lower)) + lower);
  }

  private boolean evaluateRowCol(ScriptMathProcessor mp, SV[] args, int tok)
      throws ScriptException {
    if (args.length != 1)
      return false;
    int n = args[0].asInt() - 1;
    SV x1 = mp.getX();
    float[] f;
    switch (x1.tok) {
    case T.matrix3f:
      if (n < 0 || n > 2)
        return false;
      M3 m = (M3) x1.value;
      switch (tok) {
      case T.row:
        f = new float[3];
        m.getRow(n, f);
        return mp.addXAF(f);
      case T.col:
      default:
        f = new float[3];
        m.getColumn(n, f);
        return mp.addXAF(f);
      }
    case T.matrix4f:
      if (n < 0 || n > 2)
        return false;
      M4 m4 = (M4) x1.value;
      switch (tok) {
      case T.row:
        f = new float[4];
        m4.getRow(n, f);
        return mp.addXAF(f);
      case T.col:
      default:
        f = new float[4];
        m4.getColumn(n, f);
        return mp.addXAF(f);
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
        l[i] = (sFind == null ? PT.clean(list[i]) : isAll ? PT
            .replaceAllCharacters(list[i], sFind, sReplace) : PT.rep(list[i],
            sFind, sReplace));
      return mp.addXAS(l);
    }
    String s = SV.sValue(x);
    return mp.addXStr(sFind == null ? PT.clean(s) : isAll ? PT
        .replaceAllCharacters(s, sFind, sReplace) : PT.rep(s, sFind, sReplace));
  }

  private boolean evaluateScript(ScriptMathProcessor mp, SV[] args, int tok)
      throws ScriptException {
    // eval(cmd)
    // eval("JSON",json)
    // javascript(cmd)
    // script(cmd)
    // script(cmd, syncTarget)
    // show(showCmd)
    if ((tok == T.show || tok == T.javascript) && args.length != 1
        || args.length == 0)
      return false;
    String s = SV.sValue(args[0]);
    SB sb = new SB();
    switch (tok) {
    case T.eval:
      return (args.length == 2 ? s.equalsIgnoreCase("JSON")
          && mp.addXObj(vwr.parseJSONMap(SV.sValue(args[1]))) : mp.addXObj(vwr
          .evaluateExpressionAsVariable(s)));
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
    float f;
    return (Float.isNaN(f = PT.parseFloatStrict(s)) ? mp.addXStr(s) : s
        .indexOf(".") >= 0 ? mp.addXFloat(f) : mp.addXInt(PT.parseInt(s)));
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
    String sArg = (args.length > 0 ? SV.sValue(args[0]) : tok == T.trim ? ""
        : "\n");
    switch (args.length) {
    case 0:
      break;
    case 1:
      if (args[0].tok ==  T.on) {
        return mp.addX(SV.getVariable(PT.getTokens(x.asString())));
      }
      break;
    case 2:
      if (x.tok == T.varray)
        break;
      if (tok == T.split) {
        x = SV.getVariable(PT.split(PT.rep((String) x.value, "\n\r", "\n").replace('\r', '\n'), "\n"));
        break;
      }
      //$FALL-THROUGH$
    default:
      return false;      
    }
      
    if (x.tok == T.varray && tok != T.trim && (tok != T.split || args.length == 2)) {
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
    // select substucture(....) legacy - was same as smiles(), now search()
    // select smiles(...)
    // select search(...)  now same as substructure
    // print {*}.search(...)
    if (args.length == 0 || isSelector && args.length > 1)
      return false;
    BS bs = new BS();
    String pattern = SV.sValue(args[0]);
    if (pattern.length() > 0)
      try {
        BS bsSelected = (isSelector ? (BS) mp.getX().value
            : args.length == 2 && args[1].tok == T.bitset ?  (BS) args[1].value : null);
        bs = vwr.getSmilesMatcher().getSubstructureSet(pattern, vwr.ms.at,
            vwr.ms.ac, bsSelected,
            (tok == T.smiles ? JC.SMILES_TYPE_SMILES : JC.SMILES_TYPE_SMARTS));
      } catch (Exception ex) {
        e.evalError(ex.getMessage(), null);
      }
    return mp.addXBs(bs);
  }

  private boolean evaluateSymop(ScriptMathProcessor mp, SV[] args,
                                boolean haveBitSet) throws ScriptException {

    // In the following, "op" can be the operator number in a space group 
    // or a string representation of the operator, such as "x,-y,z"

    // x = y.symop(op,atomOrPoint) 
    // Returns the point that is the result of the transformation of atomOrPoint 
    // via a crystallographic symmetry operation. The atom set y selects the unit 
    // cell and spacegroup to be used. If only one model is present, this can simply be all. 
    // Otherwise, it could be any atom or group of atoms from any model, for example 
    // {*/1.2} or {atomno=1}. The first parameter, op, is a symmetry operation. 
    // This can be the 1-based index of a symmetry operation in a file (use show spacegroup to get this listing) 
    // or a specific Jones-Faithful expression in quotes such as "x,1/2-y,z".

    // x = y.symop(op,"label")
    // This form of the .symop() function returns a set of draw commands that describe 
    // the symmetry operation in terms of rotation axes, inversion centers, planes, and 
    // translational vectors. The draw objects will all have IDs starting with whatever 
    // is given for the label. 

    // x = y.symop(op)
    // Returns the 4x4 matrix associated with this operator.

    // x = y.symop(n,"time")
    // Returns the time reversal of a symmetry operator in a magnetic space group

    SV x1 = (haveBitSet ? mp.getX() : null);
    if (x1 != null && x1.tok != T.bitset)
      return false;
    BS bsAtoms = (x1 == null ? null : (BS) x1.value);
    if (bsAtoms == null && vwr.ms.mc == 1)
      bsAtoms = vwr.getModelUndeletedAtomsBitSet(0);
    int narg = args.length;
    if (narg == 0) {
      if (bsAtoms.isEmpty())
        return false;
      String[] ops = PT.split(PT.trim((String) vwr.getSymTemp()
          .getSpaceGroupInfo(vwr.ms, null, vwr.ms.at[bsAtoms.nextSetBit(0)].mi, false)
          .get("symmetryInfo"), "\n"), "\n");
      Lst<String[]> lst = new Lst<String[]>();
      for (int i = 0, n = ops.length; i < n; i++)
        lst.addLast(PT.split(ops[i], "\t"));
      return mp.addXList(lst);
    }
    String xyz = null;
    int iOp = Integer.MIN_VALUE;
    int apt = 0;
    switch (args[0].tok) {
    case T.string:
      xyz = SV.sValue(args[0]);
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
    }
    if (bsAtoms == null) {
      if (apt < narg && args[apt].tok == T.bitset)
        (bsAtoms = new BS()).or((BS) args[apt].value);
      if (apt + 1 < narg && args[apt + 1].tok == T.bitset)
        (bsAtoms == null ? (bsAtoms = new BS()) : bsAtoms)
            .or((BS) args[apt + 1].value);
    }
    P3 pt1 = null, pt2 = null;
    if ((pt1 = (narg > apt ? mp.ptValue(args[apt], bsAtoms) : null)) != null)
      apt++;
    if ((pt2 = (narg > apt ? mp.ptValue(args[apt], bsAtoms) : null)) != null)
      apt++;
    int nth = (pt2 != null && args.length > apt && iOp == Integer.MIN_VALUE
        && args[apt].tok == T.integer ? args[apt].intValue : 0);
    if (nth > 0)
      apt++;
    if (iOp == Integer.MIN_VALUE)
      iOp = 0;
    String desc = (narg == apt ? (pt2 != null ? "all" : pt1 != null ? "point"
        : "matrix") : SV.sValue(args[apt++]).toLowerCase());

    return (bsAtoms != null && !bsAtoms.isEmpty() && apt == args.length && mp
        .addXObj(vwr.getSymTemp().getSymmetryInfoAtom(vwr.ms,
            bsAtoms.nextSetBit(0), xyz, iOp, pt1, pt2, desc, 0, 0, nth)));
  }

  private boolean evaluateTensor(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {
    // {*}.tensor()
    // {*}.tensor("isc")            // only within this atom set
    // {atomindex=1}.tensor("isc")  // all to this atom
    // {*}.tensor("efg","eigenvalues")
    SV x = mp.getX();
    if (args.length > 2 || x.tok != T.bitset)
      return false;
    BS bs = (BS) x.value;
    String tensorType = (args.length == 0 ? null : SV.sValue(args[0])
        .toLowerCase());
    JmolNMRInterface calc = vwr.getNMRCalculation();
    if ("unique".equals(tensorType))
      return mp.addXBs(calc.getUniqueTensorSet(bs));
    String infoType = (args.length < 2 ? null : SV.sValue(args[1])
        .toLowerCase());
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
          .addXObj(e.getBitsetProperty((BS) x1.value, null, tok, null,
              null, x1.value, new Object[] { name, params }, false, x1.index, false));
    }
    SV var = e.getUserFunctionResult(name, params, null);
    return (var == null ? false : mp.addX(var));
  }

  private boolean evaluateWithin(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {
    if (args.length < 1 || args.length > 5)
      return false;
    int len = args.length;
    if (len == 1 && args[0].tok == T.bitset)
      return mp.addX(args[0]);
    float distance = 0;
    Object withinSpec = args[0].value;
    String withinStr = "" + withinSpec;
    int tok = args[0].tok;
    if (tok == T.string)
      tok = T.getTokFromName(withinStr);
    ModelSet ms = vwr.ms;
    boolean isVdw = false;
    boolean isWithinModelSet = false;
    boolean isWithinGroup = false;
    boolean isDistance = false;
    RadiusData rd = null;
    switch (tok) {
    case T.vanderwaals:
      isVdw = true;
      withinSpec = null;
      //$FALL-THROUGH$
    case T.decimal:
    case T.integer:
      isDistance = true;
      if (len < 2 || len == 3 && args[1].tok == T.varray
          && args[2].tok != T.varray)
        return false;
      distance = (isVdw ? 100 : SV.fValue(args[0]));
      switch (tok = args[1].tok) {
      case T.on:
      case T.off:
        isWithinModelSet = args[1].asBoolean();
        if (len > 2 && SV.sValue(args[2]).equalsIgnoreCase("unitcell"))
          tok = T.unitcell;
        len = 0;
        break;
      case T.string:
        String s = SV.sValue(args[1]);
        if (s.startsWith("$"))
          return mp.addXBs(getAtomsNearSurface(distance, s.substring(1)));
        if (s.equalsIgnoreCase("group")) {
          isWithinGroup = true;
          tok = T.group;
        } else if (s.equalsIgnoreCase("vanderwaals")
            || s.equalsIgnoreCase("vdw")) {
          withinSpec = null;
          isVdw = true;
          tok = T.vanderwaals;
        } else if (s.equalsIgnoreCase("unitcell")) {
          tok = T.unitcell;
        } else {
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
      return (len == 3 && args[1].value instanceof BS
          && args[2].value instanceof BS && mp.addXBs(vwr.getBranchBitSet(
          ((BS) args[2].value).nextSetBit(0),
          ((BS) args[1].value).nextSetBit(0), true)));
    case T.smiles:
    case T.substructure: // same as "SMILES"
    case T.search:
      // within("smiles", "...", {bitset})
      // within("smiles", "...", {bitset})
      BS bsSelected = null;
      boolean isOK = true;
      switch (len) {
      case 2:
        break;
      case 3:
        isOK = (args[2].tok == T.bitset);
        if (isOK)
          bsSelected = (BS) args[2].value;
        break;
      default:
        isOK = false;
      }
      if (!isOK)
        e.invArg();
      return mp.addXObj(e.getSmilesExt().getSmilesMatches(SV.sValue(args[1]),
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
      switch (tok) {
      case T.helix:
      case T.sheet:
      case T.boundbox:
        return mp.addXBs(ms.getAtoms(tok, null));
      case T.basepair:
        return mp.addXBs(ms.getAtoms(tok, ""));
      case T.spec_seqcode:
        return mp.addXBs(ms.getAtoms(T.sequence, withinStr));
      }
      return false;
    case 2:
      // within (atomName, "XX,YY,ZZZ")
      switch (tok) {
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
        return mp
            .addXBs(vwr.ms.getAtoms(tok, SV.sValue(args[args.length - 1])));
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
        withinStr = SV.sValue(args[2]);
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
    P4 plane = null;
    P3 pt = null;
    Lst<SV> pts1 = null;
    int last = args.length - 1;
    switch (args[last].tok) {
    case T.point4f:
      plane = (P4) args[last].value;
      break;
    case T.point3f:
      pt = (P3) args[last].value;
      if (SV.sValue(args[1]).equalsIgnoreCase("hkl"))
        plane = e.getHklPlane(pt);
      break;
    case T.varray:
      pts1 = (last == 2 && args[1].tok == T.varray ? args[1].getList() : null);
      pt = (last == 2 ? SV.ptValue(args[1]) : last == 1 ? P3.new3(Float.NaN, 0,
          0) : null);
      break;
    }
    if (plane != null)
      return mp.addXBs(ms.getAtomsNearPlane(distance, plane));
    BS bs = (args[last].tok == T.bitset ? (BS) args[last].value : null);
    if (last > 0 && pt == null && pts1 == null && bs == null)
      return false;
    // if we have anything, it must have a point or an array or a plane or a bitset from here on out    
    if (tok == T.unitcell) {
      boolean asMap = isWithinModelSet;
      return ((bs != null || pt != null) && mp.addXObj(vwr.ms
          .getUnitCellPointsWithin(distance, bs, pt, asMap)));
    }
    if (pt != null || pts1 != null) {
      if (args[last].tok == T.varray) {
        // within(dist, pt, [pt1, pt2, pt3...])
        Lst<SV> sv = args[last].getList();
        P3[] ap3 = new P3[sv.size()];
        for (int i = ap3.length; --i >= 0;)
          ap3[i] = SV.ptValue(sv.get(i));
        P3[] ap31 = null;
        if (pts1 != null) {
          ap31 = new P3[pts1.size()];
          for (int i = ap31.length; --i >= 0;)
            ap31[i] = SV.ptValue(pts1.get(i));
        }
        Object[] ret = new Object[1];
        switch (PointIterator.withinDistPoints(distance, pt, ap3, ap31, ret)) {
        case T.point:
          return mp.addXPt((P3) ret[0]);
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
      return mp.addXBs(vwr.getAtomsNearPt(distance, pt));
    }

    if (tok == T.sequence)
      return mp.addXBs(vwr.ms.getSequenceBits(withinStr, bs, new BS()));
    if (bs == null)
      bs = new BS();
    if (!isDistance)
      return mp.addXBs(vwr.ms.getAtoms(tok, bs));
    if (isWithinGroup)
      return mp.addXBs(vwr.getGroupsWithin((int) distance, bs));
    if (isVdw) {
      rd = new RadiusData(null, (distance > 10 ? distance / 100 : distance),
          (distance > 10 ? EnumType.FACTOR : EnumType.OFFSET), VDW.AUTO);
      if (distance < 0)
        distance = 0; // not used, but this prevents a diversion
    }
    return mp.addXBs(vwr.ms.getAtomsWithinRadius(distance, bs,
        isWithinModelSet, rd));
  }

  private boolean evaluateWrite(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {
    switch (args.length) {
    case 0:
      return false;
    case 1:
      String type = args[0].asString().toUpperCase();
      if (type.equals("PNGJ")) {
        return mp.addXMap(vwr.fm.getFileAsMap(null, "PNGJ"));
      }
      if (PT.isOneOf(type, ";ZIP;ZIPALL;JMOL;")) {
        Map<String, Object> params = new Hashtable<String, Object>();
        OC oc = new OC();
        params.put("outputChannel", oc);
        vwr.createZip(null, type, params);
        BufferedInputStream bis = Rdr.getBIS(oc.toByteArray());
        params = new Hashtable<String, Object>();
        vwr.getJzt().readFileAsMap(bis, params, null);
        return mp.addXMap(params);
      }
      break;
    }
    return mp.addXStr(e.getCmdExt().dispatch(T.write, true, args));
  }

  ///////// private methods used by evaluateXXXXX ////////
  
  private BS getAtomsNearSurface(float distance, String surfaceId) {
    Object[] data = new Object[] { surfaceId, null, null };
    if (e.getShapePropertyData(JC.SHAPE_ISOSURFACE, "getVertices", data))
      return getAtomsNearPts(distance, (T3[]) data[1], (BS) data[2]);
    data[1] = Integer.valueOf(0);
    data[2] = Integer.valueOf(-1);
    if (e.getShapePropertyData(JC.SHAPE_DRAW, "getCenter", data))
      return vwr.getAtomsNearPt(distance, (P3) data[2]);
    data[1] = Float.valueOf(distance);
    if (e.getShapePropertyData(JC.SHAPE_POLYHEDRA, "getAtomsWithin", data))
      return (BS) data[2];
    return new BS();
  }

  private BS getAtomsNearPts(float distance, T3[] points, BS bsInclude) {
    BS bsResult = new BS();
    if (points.length == 0 || bsInclude != null && bsInclude.isEmpty())
      return bsResult;
    if (bsInclude == null)
      bsInclude = BSUtil.setAll(points.length);
    Atom[] at = vwr.ms.at;
    for (int i = vwr.ms.ac; --i >= 0;) {
      Atom atom = at[i];
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
  public Object getMinMax(Object floatOrSVArray, int tok) {
    float[] data = null;
    Lst<SV> sv = null;
    int ndata = 0;
    Map<String, Integer> htPivot = null;
    while (true) {
      if (AU.isAF(floatOrSVArray)) {
        if (tok == T.pivot)
          return "NaN";
        data = (float[]) floatOrSVArray;
        ndata = data.length;
        if (ndata == 0)
          break;
      } else if (floatOrSVArray instanceof Lst<?>) {
        sv = (Lst<SV>) floatOrSVArray;
        ndata = sv.size();
        if (ndata == 0) {
          if (tok != T.pivot)
            break;
        } else {
          SV sv0 = sv.get(0);
          if (sv0.tok == T.point3f)
            return getMinMaxPoint(sv, tok);
          if (sv0.tok == T.string && ((String) sv0.value).startsWith("{")) {
            Object pt = SV.ptValue(sv0);
            if (pt instanceof P3)
              return getMinMaxPoint(sv, tok);
            if (pt instanceof P4)
              return getMinMaxQuaternion(sv, tok);
            break;
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
        sum = Float.MAX_VALUE;
        minMax = Integer.MAX_VALUE;
        break;
      case T.max:
        sum = -Float.MAX_VALUE;
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
        SV svi = (sv == null ? SV.vF : sv.get(i));
        float v = (isPivot ? 1 : data == null ? SV.fValue(svi) : data[i]);
        if (Float.isNaN(v))
          continue;
        n++;
        switch (tok) {
        case T.sum2:
        case T.stddev:
          sum2 += ((double) v) * v;
          //$FALL-THROUGH$
        case T.sum:
        case T.average:
          sum += v;
          break;
        case T.pivot:
          isInt &= (svi.tok == T.integer);
          String key = svi.asString();
          Integer ii = htPivot.get(key);
          htPivot.put(key,
              (ii == null ? new Integer(1) : new Integer(ii.intValue() + 1)));
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
      return Float.valueOf((float) sum);
    }
    return "NaN";
  }

  /**
   * calculates the statistical value for x, y, and z independently
   * 
   * @param pointOrSVArray
   * @param tok
   * @return Point3f or "NaN"
   */
  @SuppressWarnings("unchecked")
  private Object getMinMaxPoint(Object pointOrSVArray, int tok) {
    P3[] data = null;
    Lst<SV> sv = null;
    int ndata = 0;
    if (pointOrSVArray instanceof Quat[]) {
      data = (P3[]) pointOrSVArray;
      ndata = data.length;
    } else if (pointOrSVArray instanceof Lst<?>) {
      sv = (Lst<SV>) pointOrSVArray;
      ndata = sv.size();
    }
    if (sv == null && data == null)
      return "NaN";
    P3 result = new P3();
    float[] fdata = new float[ndata];
    for (int xyz = 0; xyz < 3; xyz++) {
      for (int i = 0; i < ndata; i++) {
        P3 pt = (data == null ? SV.ptValue(sv.get(i)) : data[i]);
        if (pt == null)
          return "NaN";
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
      Object f = getMinMax(fdata, tok);
      if (!(f instanceof Number))
        return "NaN";
      float value = ((Number) f).floatValue();
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
    Quat[] data;
    switch (tok) {
    case T.min:
    case T.max:
    case T.sum:
    case T.sum2:
      return "NaN";
    }

    // only stddev and average

    while (true) {
      data = e.getQuaternionArray(svData, T.list);
      if (data == null)
        break;
      float[] retStddev = new float[1];
      Quat result = Quat.sphereMean(data, retStddev, 0.0001f);
      switch (tok) {
      case T.average:
        return result;
      case T.stddev:
        return Float.valueOf(retStddev[0]);
      }
      break;
    }
    return "NaN";
  }

  private JmolPatternMatcher pm;

  private JmolPatternMatcher getPatternMatcher() {
    return (pm == null ? pm = (JmolPatternMatcher) Interface
        .getUtil("PatternMatcher", e.vwr, "script") : pm);
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
                               float distance, RadiusData rd,
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
        e.showString(GT
            .$("Note: More than one model is involved in this contact!"));
    }
    // B always within some possibly extended VDW of A or just A itself
    if (!bsA.equals(bsB)) {
      boolean setBfirst = (!localOnly || bsA.cardinality() < bsB.cardinality());
      if (setBfirst) {
        bs = vwr.ms.getAtomsWithinRadius(distance, bsA, withinAllModels,
            (Float.isNaN(distance) ? rd : null));
        bsB.and(bs);
      }
      if (localOnly) {
        // we can just get the near atoms for A as well.
        bs = vwr.ms.getAtomsWithinRadius(distance, bsB, withinAllModels,
            (Float.isNaN(distance) ? rd : null));
        bsA.and(bs);
        if (!setBfirst) {
          bs = vwr.ms.getAtomsWithinRadius(distance, bsA, withinAllModels,
              (Float.isNaN(distance) ? rd : null));
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
