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

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.jmol.api.Interface;
import org.jmol.api.JmolDataManager;
import org.jmol.api.SymmetryInterface;
import org.jmol.atomdata.RadiusData;
import org.jmol.c.STER;
import org.jmol.c.VDW;
import org.jmol.i18n.GT;
import org.jmol.minimize.Minimizer;
import org.jmol.modelkit.ModelKit;
import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.Bond;
import org.jmol.modelset.BondSet;
import org.jmol.modelset.LabelToken;
import org.jmol.modelset.Measurement;
import org.jmol.modelset.ModelSet;
import org.jmol.modelset.StateScript;
import org.jmol.modelset.Text;
import org.jmol.modelset.TickInfo;
import org.jmol.script.SV;
import org.jmol.script.ScriptCompiler;
import org.jmol.script.ScriptContext;
import org.jmol.script.ScriptError;
import org.jmol.script.ScriptEval;
import org.jmol.script.ScriptException;
import org.jmol.script.ScriptInterruption;
import org.jmol.script.ScriptMathProcessor;
import org.jmol.script.ScriptParam;
import org.jmol.script.T;
import org.jmol.shape.Measures;
import org.jmol.util.BSUtil;
import org.jmol.util.BZone;
import org.jmol.util.BoxInfo;
import org.jmol.util.C;
import org.jmol.util.Edge;
import org.jmol.util.Elements;
import org.jmol.util.Escape;
import org.jmol.util.Font;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.util.Point3fi;
import org.jmol.util.SimpleUnitCell;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.ShapeManager;
import org.jmol.viewer.StateManager;
import org.jmol.viewer.TransformManager;
import org.jmol.viewer.Viewer;
import org.jmol.viewer.Viewer.ACCESS;

import javajs.util.AU;
import javajs.util.BArray;
import javajs.util.BS;
import javajs.util.Base64;
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

public class CmdExt extends ScriptExt {

  final static int ERROR_invalidArgument = 22;

  public CmdExt() {
    // used by Reflection
  }

  /**
   * The command dispatcher for the set of commands extended by this class.
   * These commands can be from the Jmol command of their method name or another
   * command or method working as though it were a command, with a
   * non-GUI-generated compiled script.
   * 
   * @param iTok
   *        the current statement position, usually 1
   * @param flag
   *        a command-specific flag
   * @param st
   *        the compiled statement for this command
   * 
   */
  @Override
  public String dispatch(int iTok, boolean flag, T[] st)
      throws ScriptException {
    chk = e.chk;
    slen = e.slen;
    this.st = st;
    switch (iTok) {
    case T.audio:
      audio();
      break;
    case T.binary:
      st[0].value = prepareBinaryOutput((SV) st[0]);
      return null;
    case T.assign:
      assign(false);
      break;
    case T.cache:
      cache();
      break;
    case T.calculate:
      calculate();
      break;
    case T.capture:
      capture();
      break;
    case T.centerat:
      centerAt();
      break;
    case T.compare:
      compare();
      break;
    case T.console:
      console();
      break;
    case T.connect:
      connect(1);
      break;
    case T.configuration:
      configuration();
      break;
    case T.data:
      data();
      break;
    case T.hbond:
      connect(0);
      break;
    case T.image:
      image();
      break;
    case T.invertSelected:
      invertSelected();
      break;
    case T.macro:
      macro();
      break;
    case T.mapproperty:
      mapProperty();
      break;
    case T.minimize:
      minimize(false);
      break;
    case T.modelkitmode:
      modelkit();
      break;
    case T.modulation:
      modulation();
      break;
    case T.mutate:
      mutate();
      break;
    case T.navigate:
      navigate();
      break;
    case T.plot:
    case T.quaternion:
    case T.ramachandran:
      plot(st);
      break;
    case T.show:
      show();
      break;
    case T.stereo:
      stereo();
      break;
    case T.unitcell:
      unitcell(flag ? 2 : 1, false);
      break;
    case T.write:
      return write(flag ? st : null);
    case JC.SHAPE_MEASURES:
      measure();
      break;
    case JC.SHAPE_POLYHEDRA:
      polyhedra();
      break;
    case JC.SHAPE_ELLIPSOIDS:
      ellipsoid();
      break;
    case JC.SHAPE_STRUTS:
      struts();
      break;
    }
    return null;
  }

  private void audio() throws ScriptException {
    Map<String, Object> htParams = new Hashtable<>();
    String id = null;
    String filter = null;
    String filename = null;
    int i = e.iToken + 1;
    if (tokAt(i) == T.off) {
      checkLength(i + 1);
      if (!chk)
        vwr.sm.stopAudio(null);
      return;
    }
    for (; i < slen; i++) {
      switch (tokAt(i)) {
      case T.id:
        if (id != null)
          invArg();
        id = paramAsStr(++i);
        break;
      case T.string:
        filename = stringParameter(i);
        htParams.put("audioFile", filename);
        break;
      case T.filter:
        filter = stringParameter(++i);
        id = addFilterAttribute(htParams, filter, "id");
        addFilterAttribute(htParams, filter, "loop");
        addFilterAttribute(htParams, filter, "start");
        addFilterAttribute(htParams, filter, "pause");
        addFilterAttribute(htParams, filter, "play");
        addFilterAttribute(htParams, filter, "ended");
        addFilterAttribute(htParams, filter, "action");
        break;
      default:
        checkLength(i + 1);
        if (htParams.containsKey("action"))
          invArg();
        String action = paramAsStr(i);
        htParams.put("autoclose", Boolean.FALSE);
        htParams.put("action", action);
        break;
      }
    }
    if (!chk && !htParams.isEmpty()) {
      if (id == null)
        id = "audio1";
      htParams.put("id", id);
      vwr.sm.playAudio(htParams);
    }
  }

  private String addFilterAttribute(Map<String, Object> htParams, String filter,
                                String key) {
    String val = PT.getQuotedOrUnquotedAttribute(filter, key);
    if (val != null && val.length() > 0)
      htParams.put(key, val);
    return val;
   }


  private void macro() throws ScriptException {
    if (chk)
      return;
    String key = e.optParameterAsString(1);
    if (key.length() == 0) {
      showString(vwr.getMacro(null));
      return;
    }
    String macro = vwr.getMacro(key);
    if (macro == null) {
      showString(
          "macro " + key + " could not be found. Current macros include:\n"
              + vwr.getMacro(null));
      return;
    }
    showString("running " + macro);
    e.cmdScript(T.macro, macro, null, null);
  }

  /**
   * used for TRY command
   * 
   * @param context
   * @param shapeManager
   * @return true if successful; false if not
   */
  public boolean evalParallel(ScriptContext context,
                              ShapeManager shapeManager) {
    chk = e.chk;
    slen = e.slen;
    ScriptEval se = new ScriptEval().setViewer(vwr);
    se.historyDisabled = true;
    se.compiler = new ScriptCompiler(vwr);
    se.sm = shapeManager;
    try {
      se.restoreScriptContext(context, true, false, false);
      // TODO: This will disallow some motion commands
      //       within a TRY/CATCH block in JavaScript, and
      //       the code will block. 
      se.setAllowJSThreads(false);
      se.dispatchCommands(false, false, false);
    } catch (Exception ex) {
      e.vwr.setStringProperty("_errormessage", "" + ex);
      if (se.thisContext == null) {
        Logger.error("Error evaluating context " + ex);
        ex.printStackTrace();
      }
      return false;
    }
    return true;
  }

  /**
   * @param bs
   * @param label
   * @param tokenValue
   *        unused
   * @param useAtomMap
   * @param index
   * @param isExplicitlyAll
   * @return String or, if multiple or explicitly ALL, String[]
   */
  public Object getBitsetIdent(BS bs, String label, Object tokenValue,
                               boolean useAtomMap, int index,
                               boolean isExplicitlyAll) {
    return getBitsetIdentFull(bs, label, useAtomMap, index, isExplicitlyAll,
        null);
  }

  public Object getBitsetIdentFull(BS bs, String label, boolean useAtomMap,
                                   int index, boolean isExplicitlyAll,
                                   String[] sout) {
    boolean isAtoms = !(bs instanceof BondSet);
    if (isAtoms) {
      if (label == null) {
        label = vwr.getStandardLabelFormat(0);
      } else if (label.length() == 0) {
        label = "%[label]";
      }
    } else {
      // bond
      if (label != null && label.length() == 0) {
        label = "[%a1 #%i1] %4.3LENGTH [%a2 #%i2] #%#";
      } else if ("%U".equals(label)) {
        label = "";
      }
    }
    int pt = (label == null ? -1 : label.indexOf("%"));
    boolean haveIndex = (index != Integer.MAX_VALUE);
    if (bs == null || chk || isAtoms && pt < 0) {
      if (label == null)
        label = "";
      return isExplicitlyAll ? new String[] { label } : (Object) label;
    }
    ModelSet modelSet = vwr.ms;
    int n = 0;
    int[] indices = (isAtoms || !useAtomMap ? null
        : ((BondSet) bs).getAssociatedAtoms(modelSet));
    boolean asIdentity = (label == null || label.length() == 0);
    Map<String, Object> htValues = (isAtoms || asIdentity ? null
        : LabelToken.getBondLabelValues());
    LabelToken[] tokens = (asIdentity ? null
        : isAtoms ? LabelToken.compile(vwr, label, '\0', null)
            : LabelToken.compile(vwr, label, '\1', htValues));
    int nmax = (haveIndex ? 1 : bs.cardinality());
    boolean haveSout = (sout != null);
    if (!haveSout)
      sout = new String[nmax];
    P3d ptTemp = new P3d();
    for (int j = (haveIndex ? index : bs.nextSetBit(0)); j >= 0; j = bs
        .nextSetBit(j + 1)) {
      String str;
      if (isAtoms) {
        str = (asIdentity ? modelSet.at[j].getInfo()
            : LabelToken.formatLabelAtomArray(vwr, modelSet.at[j], tokens, '\0',
                indices, ptTemp));
      } else {
        Bond bond = modelSet.bo[j];
        if (bond == null)
          continue;
        str = (asIdentity ? bond.getIdentity()
            : LabelToken.formatLabelBond(vwr, bond, tokens, htValues, indices,
                ptTemp));
      }
      str = PT.formatStringI(str, "#", (n + 1));
      sout[haveSout ? j : n] = str;
      n++;
      if (haveIndex)
        break;
    }
    return nmax == 1 && !isExplicitlyAll ? sout[0] : (Object) sout;
  }

  public int getLoadSymmetryParams(int i, SB sOptions,
                                   Map<String, Object> htParams)
      throws ScriptException {
    // {i j k}
    ScriptEval eval = e;
    chk = eval.chk;
    slen = eval.slen;
    boolean isEmptyLoad = false;
    T3d lattice = null;
    int tok = tokAt(i);
    if (tok == T.leftbrace || tok == T.point3f) {
      lattice = eval.getFractionalPoint(i);
      tok = tokAt(i = eval.iToken + 1);
    }

    // default lattice {555 555 -1} (packed) 
    // for PACKED, CENTROID, SUPERCELL, RANGE, SPACEGROUP, UNITCELL

    switch (tok) {
    case T.spacegroup:
      isEmptyLoad = (htParams.get("isEmptyLoad") == Boolean.TRUE);
      //$FALL-THROUGH$
    case T.fill:
    case T.packed:
    case T.centroid:
    case T.supercell:
    case T.range:
    case T.unitcell:
      if (lattice == null)
        lattice = P3d.new3(555, 555, -1);
      // re-read this token
      eval.iToken = i - 1;
      break;
    }

    P3d offset = null;
    if (lattice != null) {
      htParams.put("lattice", lattice);
      i = eval.iToken + 1;
      sOptions.append(" " + SimpleUnitCell.escapeMultiplier(lattice));

      // {i j k} PACKED, CENTROID -- either or both; either order

      i = checkPacked(i, 0, htParams, sOptions);
      if (tokAt(i) == T.centroid) {
        htParams.put("centroid", Boolean.TRUE);
        sOptions.append(" CENTROID");
        int ptok = 0;
        if (isFloatParameter(i + 1)) {
           --i;
           ptok = T.packed;
        }
        i = checkPacked(++i, ptok, htParams, sOptions);
      }

      // {i j k} ... SUPERCELL {i' j' k'}

      if (tokAt(i) == T.supercell) {
        Object supercell;
        sOptions.append(" SUPERCELL ");
        if (eval.isPoint3f(++i)) {
          P3d pt = getPoint3f(i, false);
          if (pt.x != (int) pt.x || pt.y != (int) pt.y || pt.z != (int) pt.z
              || pt.x < 1 || pt.y < 1 || pt.z < 1) {
            eval.iToken = i;
            invArg();
          }
          supercell = pt;
          i = eval.iToken;
        } else {
          supercell = stringParameter(i);
        }
        sOptions.append(Escape.e(supercell));
        htParams.put("supercell", supercell);
        i = checkPacked(++i, 0, htParams, sOptions);
      }

      // {i j k} ... RANGE x.y  (from full unit cell set)
      // {i j k} ... RANGE -x.y (from non-symmetry set)

      double distance = 0;
      if (tokAt(i) == T.range) {
        /*
        * # Jmol 11.3.9 introduces the capability of visualizing the close
        * contacts around a crystalline protein (or any other crystal
        * structure) that are to atoms that are in proteins in adjacent unit
        * cells or adjacent to the protein itself. The option RANGE x, where x
        * is a distance in angstroms, placed right after the braces containing
        * the set of unit cells to load does this. The distance, if a positive
        * number, is the maximum distance away from the closest atom in the {1
        * 1 1} set. If the distance x is a negative number, then -x is the
        * maximum distance from the {not symmetry} set. The difference is that
        * in the first case the primary unit cell (555) is first filled as
        * usual, using symmetry operators, and close contacts to this set are
        * found. In the second case, only the file-based atoms ( Jones-Faithful
        * operator x,y,z) are initially included, then close contacts to that
        * set are found. Depending upon the application, one or the other of
        * these options may be desirable.
        */
        i++;
        distance = doubleParameter(i++);
        sOptions.append(" range " + distance);
      }
      htParams.put("symmetryRange", Double.valueOf(distance));

      // {i j k} ... SPACEGROUP "nameOrNumber"
      // {i j k} ... SPACEGROUP "IGNOREOPERATORS"
      // {i j k} ... SPACEGROUP ""

      String spacegroup = null;
      SymmetryInterface sg;
      int iGroup = Integer.MIN_VALUE;
      if (tokAt(i) == T.spacegroup) {
        ++i;
        spacegroup = PT.rep(paramAsStr(i++), "''", "\"");
        sOptions.append(" spacegroup " + PT.esc(spacegroup));
        if (spacegroup.equalsIgnoreCase("ignoreOperators")) {
          iGroup = -999;
        } else {
          if (spacegroup.length() == 0) {
            sg = vwr.getCurrentUnitCell();
            if (sg != null)
              spacegroup = sg.getSpaceGroupName();
          } else {
            if (spacegroup.indexOf(",") >= 0) // Jones Faithful
              if ((lattice.x < 9 && lattice.y < 9 && lattice.z == 0))
                spacegroup += "#doNormalize=0";
          }
          htParams.put(JC.INFO_SPACE_GROUP_NAME, spacegroup);
          iGroup = -2;
        }
      }

      // {i j k} ... UNITCELL [a b c alpha beta gamma]
      // {i j k} ... UNITCELL [ax ay az bx by bz cx cy cz] 
      // {i j k} ... UNITCELL ""  // same as current

      double[] fparams = null;
      if (tokAt(i) == T.unitcell) {
        ++i;
        String s = eval.optParameterAsString(i);
        if (s.length() == 0) {
          // unitcell "" -- use current unit cell
          sg = vwr.getCurrentUnitCell();
          if (sg != null) {
            fparams = sg.getUnitCellAsArray(true);
            offset = sg.getCartesianOffset();
          }
        } else {
          if (tokAt(i) == T.string) {
            fparams = new double[6];
            SimpleUnitCell.setAbc(s, fparams, null);
          } else {
            fparams = eval.doubleParameterSet(i, 6, 9);
          }
        }
        if (fparams == null || fparams.length != 6 && fparams.length != 9)
          invArg();
        sOptions.append(" unitcell [");
        for (int j = 0; j < fparams.length; j++)
          sOptions.append((j == 0 ? "" : " ") + fparams[j]);
        sOptions.append("]");
        htParams.put("unitcell", fparams);
        if (iGroup == Integer.MIN_VALUE)
          iGroup = -1;
        i = eval.iToken + 1;
      }
      if (iGroup != Integer.MIN_VALUE)
        htParams.put(JC.INFO_SPACE_GROUP_INDEX, Integer.valueOf(iGroup));
      if (isEmptyLoad && fparams == null && spacegroup != null
          && (iGroup = PT.parseInt(spacegroup)) != Integer.MIN_VALUE) {
        // TODO check here for number and set unit cell appropriately.

      }
    }

    // OFFSET {x y z} (fractional or not) (Jmol 12.1.17)

    boolean areFractional = false;
    if (offset == null && tokAt(i) == T.offset) {
      offset = getPoint3f(++i, true);
      areFractional = eval.coordinatesAreFractional;
    }
    if (offset != null) {
      if (areFractional) {
        offset.setT(eval.fractionalPoint);
        htParams.put("unitCellOffsetFractional",
            (areFractional ? Boolean.TRUE : Boolean.FALSE));
        sOptions.append(
            " offset {" + offset.x + " " + offset.y + " " + offset.z + "/1}");
      } else {
        sOptions.append(" offset " + Escape.eP(offset));
      }
      htParams.put(JC.INFO_UNIT_CELL_OFFSET, offset);
      i = eval.iToken + 1;
    }
    return i;
  }

  /**
   * Process FILL and PACKED and all their variants.
   * 
   * @param i
   * @param tok 
   * @param htParams
   * @param sOptions
   * @return new token position
   * @throws ScriptException
   */
  private int checkPacked(int i, int tok, Map<String, Object> htParams, SB sOptions)
      throws ScriptException {
    if (tok == 0)
      tok = tokAt(i);
    switch (tok) {
    case T.fill:
      htParams.put("packed", Boolean.TRUE);
      T3d[] oabc = null;
      tok = tokAt(++i);
      switch (tok) {
      case T.boundbox:
      case T.unitcell:
      case T.string:
      case T.spin:
        break;
      default:
        if (e.isArrayParameter(i)) {
          oabc = e.getPointArray(i, -1, false, true);
          i = e.iToken;
        } else if (isFloatParameter(i)) {
          double d = doubleParameter(i);
          oabc = new P3d[] { new P3d(), P3d.new3(d, d, d) };
        } else {
          oabc = new P3d[0];
          --i;
        }
      }
      i++;
      if (e.chk)
        return i;
      switch (tok) {
      case T.spin:
      case T.string:
        i--;
        //$FALL-THROUGH$
      case T.unitcell:
        // load .... FILL UNITCELL [conventional | primitive | rhombohedral | trigonal | a,b,c....]
        String type = e.optParameterAsString(i++).toLowerCase();
        if (PT.isOneOf(type, ";spin;conventional;primitive;rhombohedral;trigonal;")
            || type.indexOf(",") >= 0 && (type.indexOf("a") >= 0
                && type.indexOf("b") >= 0 && type.indexOf("c") >= 0)) {
          htParams.put(JC.LOAD_OPTION_FILL_RANGE, type); // "conventional" or "primitive"
          sOptions.append(" FILL UNITCELL \"" + type + "\"");
          return i;
        }
        SymmetryInterface unitCell = vwr.getCurrentUnitCell();
        if (unitCell != null) {
          oabc = BoxInfo.toOABC(
        		  unitCell.getUnitCellVerticesNoOffset(),
                  unitCell.getCartesianOffset());
          break;
        }
        //$FALL-THROUGH$
      case T.boundbox:
        oabc = BoxInfo.toOABC(vwr.ms.getBBoxVertices(), null);
        break;
      }
      switch (oabc.length) {
      case 2:
        // origin and diagonal vector
        T3d a = oabc[1];
        oabc = new T3d[] { oabc[0], P3d.newP(oabc[0]), new P3d(), new P3d() };
        oabc[1].x = a.x;
        oabc[2].y = a.y;
        oabc[3].z = a.z;
        break;
      case 3:
        // implicit origin {0 0 0} with three vectors
        oabc = new T3d[] { new P3d(), oabc[0], oabc[1], oabc[2] };
        break;
      case 4:
        break;
      default:
        // {0 0 0} with 10x10x10 cell
        oabc = new T3d[] { new P3d(), P3d.new3(10, 0, 0), P3d.new3(0, 10, 0),
            P3d.new3(0, 0, 10) };
      }
      htParams.put("fillRange", oabc);
      sOptions.append(" FILL [" + oabc[0] + oabc[1] + oabc[2] + oabc[3] + "]");
      break;
    case T.packed:
      double f = Double.NaN;
      if (isFloatParameter(++i))
        f = doubleParameter(i++);
      if (!e.chk) {
        htParams.put("packed", Boolean.TRUE);
        sOptions.append(" PACKED");
        if (!Double.isNaN(f)) {
          htParams.put("packingRange", Double.valueOf(f));
          sOptions.append(" " + f);
        }
      }
      break;
    }
    return i;
  }

  ///////////////// Jmol script commands ////////////

  private void cache() throws ScriptException {
    int tok = tokAt(1);
    String fileName = null;
    int n = 2;
    switch (tok) {
    case T.add:
    case T.remove:
      fileName = e.optParameterAsString(n++);
      //$FALL-THROUGH$
    case T.clear:
      checkLength(n);
      if (!chk) {
        if ("all".equals(fileName))
          fileName = null;
        int nBytes = vwr.cacheFileByName(fileName, tok == T.add);
        showString(nBytes < 0 ? "cache cleared"
            : nBytes + " bytes " + (tok == T.add ? " cached" : " removed"));
      }
      break;
    default:
      invArg();
    }
  }

  private void calculate() throws ScriptException {
    boolean isSurface = false;
    boolean asDSSP = false;
    BS bs1 = null;
    BS bs2 = null;
    ScriptEval e = this.e;
    int n = Integer.MIN_VALUE;
    int version = -1;
    if ((e.iToken = e.slen) >= 2) {
      e.clearDefinedVariableAtomSets();
      switch (getToken(1).tok) {
      case T.integer:
        // CALCULATE 3D <zap>
        if (intParameter(1) != 3 || !paramAsStr(2).equalsIgnoreCase("D")) {
          break;
        }
        if (vwr.am.cmi < 0)
          e.errorStr(ScriptError.ERROR_multipleModelsDisplayedNotOK,
              "calculate 3D");
        String cmd = "load append $ filter '2D';select visible;center selected";
        if (e.optParameterAsString(3).equalsIgnoreCase("ZAP")) {
          cmd += ";zap modelIndex=" + Math.max(vwr.am.cmi, 0);
        }
        if (!chk)
          e.runScript(cmd);
        return;
      case T.identifier:
        checkLength(2);
        break;
      case T.spacegroup:
        e.iToken = 1;
        bs1 = (slen == 2 ? null : atomExpressionAt(2));
        e.checkLast(e.iToken);
        if (!chk)
          e.showString(""
              + vwr.findSpaceGroup(null, bs1, null, null, null, null, JC.SG_AS_STRING));
        return;
      case T.chirality:
        e.iToken = 1;
        bs1 = (slen == 2 ? null : atomExpressionAt(2));
        e.checkLast(e.iToken);
        if (!chk)
          e.showString(vwr.calculateChirality(bs1));
        return;
      case T.formalcharge:
        checkLength(2);
        if (chk)
          return;
        n = vwr.calculateFormalCharges(null);
        showString(GT.i(GT.$("{0} charges modified"), n));
        return;
      case T.aromatic:
        checkLength(2);
        if (!chk)
          vwr.ms.assignAromaticBondsBs(true, null);
        return;
      case T.hbond:
        if (e.slen != 2) {
          // calculate hbonds STRUCTURE -- only the DSSP/DSSR structurally-defining H bonds
          asDSSP = (tokAt(++e.iToken) == T.structure);
          if (asDSSP)
            bs1 = vwr.bsA();
          else
            bs1 = atomExpressionAt(e.iToken);
          if (!asDSSP && !(asDSSP = (tokAt(++e.iToken) == T.structure)))
            bs2 = atomExpressionAt(e.iToken);
        }
        if (chk)
          return;
        n = vwr.autoHbond(bs1, bs2, false);
        if (n != Integer.MIN_VALUE)
          e.report(GT.i(GT.$("{0} hydrogen bonds"), Math.abs(n)), false);
        return;
      case T.hydrogen:
        // calculate hydrogen TRUE also does an aromatic calculation
        int itok = tokAt(2);
        boolean andBond = (itok == T.on);
        if (andBond) {
          e.iToken++;
        } else if (itok == T.stereo) {
          // calculate Hydrogen stereo 
          e.iToken++;
        }
        bs1 = (slen == e.iToken + 1 ? null : atomExpressionAt(e.iToken + 1));
        if (bs1 == null && itok == T.stereo) {
          bs1 = vwr.getAtomBitSet("_C & connected(3) & !connected(double)");
        }
        e.checkLast(e.iToken);
        if (!chk) {
          vwr.addHydrogens(bs1, 0);
          if (andBond) {
            if (bs1 == null)
              bs1 = vwr.bsA();
            vwr.makeConnections(0.1d, 1e8d, Edge.BOND_AROMATIC, T.modify, bs1,
                bs1, null, false, false, 0);
            vwr.ms.assignAromaticBondsBs(true, null);
          }
        }
        return;
      case T.partialcharge:
        e.iToken = 1;
        bs1 = (slen == 2 ? null : atomExpressionAt(2));
        e.checkLast(e.iToken);
        if (!chk)
          e.getPartialCharges(bs1);
        return;
      case T.symmetry:
      case T.pointgroup:
        if (!chk) {
          if (tokAt(2) == T.polyhedra) {
            String id = (tokAt(3) == T.string ? stringParameter(3) : null);
            bs1 = (id != null || slen == 3 ? null : atomExpressionAt(3));
            Object[] data = new Object[] { id, null, bs1 };
            showString(
                e.getShapePropertyData(JC.SHAPE_POLYHEDRA, "symmetry", data)
                    ? (String) data[1]
                    : "");
          } else {
            bs1 = (slen == 2 ? vwr.bsA() : atomExpressionAt(2));
            P3d center = null;
            if (tokAt(e.iToken) == T.center) {
              center = e.centerParameter(e.iToken + 1, null);
            }
            showString(vwr.ms.calculatePointGroup(bs1, center));
          }
        }
        return;
      case T.straightness:
        checkLength(2);
        if (!chk) {
          vwr.calculateStraightness();
          vwr.addStateScript("set quaternionFrame '" + vwr.getQuaternionFrame()
              + "'; calculate straightness", false, true);
        }
        return;
      case T.structure:
        // calculate structure {.....} ...
        bs1 = (slen < 4 || isFloatParameter(3) ? null : atomExpressionAt(2));
        switch (tokAt(++e.iToken)) {
        case T.ramachandran:
          break;
        case T.dssr:
          if (chk)
            return;
          e.showString(
              vwr.getAnnotationParser(true).calculateDSSRStructure(vwr, bs1));
          return;
        case T.dssp:
          asDSSP = true;
          // calculate structure DSSP
          // calculate structure DSSP 1.0
          // calculate structure DSSP 2.0
          version = (slen == e.iToken + 1 ? -1
              : (int) doubleParameter(++e.iToken));
          break;
        case T.nada:
          asDSSP = vwr.getBoolean(T.defaultstructuredssp);
          break;
        default:
          invArg();
        }
        if (!chk)
          showString(vwr.calculateStructures(bs1, asDSSP, true, version));
        return;
      case T.struts:
        bs1 = (e.iToken + 1 < slen ? atomExpressionAt(++e.iToken) : null);
        bs2 = (e.iToken + 1 < slen ? atomExpressionAt(++e.iToken) : null);
        checkLength(++e.iToken);
        if (!chk) {
          n = vwr.calculateStruts(bs1, bs2);
          if (n > 0) {
            setShapeProperty(JC.SHAPE_STICKS, "type",
                Integer.valueOf(Edge.BOND_STRUT));
            e.setShapePropertyBs(JC.SHAPE_STICKS, "color",
                Integer.valueOf(0x0FFFFFF), null);
            e.setShapeTranslucency(JC.SHAPE_STICKS, "", "translucent", 0.5d,
                null);
            setShapeProperty(JC.SHAPE_STICKS, "type",
                Integer.valueOf(Edge.BOND_COVALENT_MASK));
          }
          showString(GT.i(GT.$("{0} struts added"), n));
        }
        return;
      case T.surface:
        isSurface = true;
        // deprecated
        //$FALL-THROUGH$
      case T.surfacedistance:
        // preferred
        // calculate surfaceDistance FROM {...}
        // calculate surfaceDistance WITHIN {...}
        boolean isFrom = false;
        switch (tokAt(2)) {
        case T.within:
          e.iToken++;
          break;
        case T.nada:
          isFrom = !isSurface;
          break;
        case T.from:
          isFrom = true;
          e.iToken++;
          break;
        default:
          isFrom = true;
        }
        bs1 = (e.iToken + 1 < slen ? atomExpressionAt(++e.iToken) : vwr.bsA());
        checkLength(++e.iToken);
        if (!chk)
          vwr.calculateSurface(bs1, (isFrom ? Double.MAX_VALUE : -1));
        return;
      }
    }
    e.errorStr2(ScriptError.ERROR_what, "CALCULATE",
        "3D? aromatic? hbonds? hydrogen? formalCharge? partialCharge? pointgroup? straightness? structure? struts? surfaceDistance FROM? surfaceDistance WITHIN?");
  }

  private void capture() throws ScriptException {
    // capture "filename"
    // capture "filename" ROTATE axis degrees // y 5 assumed; axis and degrees optional
    // capture "filename" SPIN axis  // y assumed; axis optional
    // capture off/on
    // capture "" or just capture   -- end
    if (!chk && !vwr.allowCapture()) {
      showString("Cannot capture on this platform");
      return;
    }
    Map<String, Object> params = vwr.captureParams;
    String type = (params == null ? "GIF" : (String) params.get("type"));
    double endTime = 0; // indefinitely by default
    int mode = 0;
    int slen = e.slen;
    String fileName = "";
    boolean looping = (vwr.am.animationReplayMode != T.once);
    int i = 1;
    int tok = tokAt(i);
    boolean isTransparent = (tok == T.translucent);
    if (isTransparent)
      tok = tokAt(++i);
    String s = null;
    switch (tok == T.nada ? (tok = T.end) : tok) {
    case T.string:
      fileName = e.optParameterAsString(i++);
      if (fileName.length() == 0) {
        mode = T.end;
        break;
      }
      String lc = fileName.toLowerCase();
      if (lc.endsWith(".gift") || lc.endsWith(".pngt")) {
        isTransparent = true;
        fileName = fileName.substring(0, fileName.length() - 1);
        lc = fileName.toLowerCase();
      } else if (!lc.endsWith(".gif") && !lc.contains(".png")) {
        fileName += ".gif";
      }
      if (lc.endsWith(".png")) {
        if (!lc.endsWith("0.png"))
          fileName = fileName.substring(0, fileName.length() - 4) + "0000.png";
        type = "PNG";
      } else {
        type = "GIF";
      }
      if (isTransparent)
        type += "T";
      int pt = fileName.indexOf("0000.");
      boolean streaming = (pt < 0 || pt != fileName.lastIndexOf(".") - 4);
      boolean isRock = false;
      if (tokAt(i) == T.loop) {
        looping = true;
        tok = tokAt(++i);
      }
      switch (tokAt(i)) {
      case T.script:
        s = stringParameter(++i);
        break;
      case T.rock:
        isRock = true;
        //$FALL-THROUGH$
      case T.spin:
        String axis = "y";
        looping = true;
        i++;
        if (isRock) {
          if (i < slen && tokAt(i) != T.integer)
            axis = e.optParameterAsString(i++).toLowerCase();
          s = "rotate Y 10 10;rotate Y -10 -10;rotate Y -10 -10;rotate Y 10 10";
          int n = (i < slen ? intParameter(i++) : 5);
          if (n < 0) {
            s = PT.rep(s, "10;", "" + (-n) + ";");
          } else {
            s = PT.rep(s, "10", "" + n);
          }
        } else {
          if (i < slen)
            axis = e.optParameterAsString(i++).toLowerCase();
          s = "rotate Y 360 30;";
        }
        if (chk)
          return;
        vwr.setNavigationMode(false);
        if (axis == "" || "xyz".indexOf(axis) < 0)
          axis = "y";
        s = PT.rep(s, "Y", axis);
        break;
      case T.decimal:
      case T.integer:
        endTime = doubleParameter(i++);
        break;
      }
      if (chk)
        return;
      if (s != null) {
        boolean wf = vwr.g.waitForMoveTo;
        s = "set waitformoveto true;" + s + ";set waitformoveto " + wf;
        s = "capture " + (isTransparent ? "transparent " : "")
            + PT.esc(fileName) + (looping ? " LOOP;" : ";") + s
            + ";capture end;";
        e.cmdScript(0, null, s, null);
        return;
      }
      mode = T.movie;
      params = new Hashtable<String, Object>();
      int fps = vwr.getInt(T.animationfps);
      if (streaming) {
        params.put("streaming", Boolean.TRUE);
        if (!looping)
          showString(GT.o(GT.$(
              "Note: Enable looping using the LOOP keyword just after the file name or {0}"),
              new Object[] { "ANIMATION MODE LOOP" }));
        showString(GT.o(GT.$("Animation delay based on: {0}"),
            new Object[] { "ANIMATION FPS " + fps }));
      }
      params.put("captureFps", Integer.valueOf(fps));
      break;
    case T.end:
    case T.cancel:
      if (params != null)
        params.put("captureSilent", Boolean.TRUE);
      //$FALL-THROUGH$
    case T.on:
    case T.off:
      checkLength(-2);
      mode = tok;
      break;
    default:
      invArg();
    }
    if (chk || params == null)
      return;
    params.put("type", type);
    Integer c = Integer.valueOf(vwr.getBackgroundArgb());
    params.put("backgroundColor", c);
    params.put("fileName", fileName);
    params.put("quality", Integer.valueOf(-1));
    params.put("endTime", Long.valueOf(endTime <= 0 ? -1
        : System.currentTimeMillis() + (long) (endTime * 1000)));
    params.put("captureMode", T.nameOf(mode).toLowerCase());
    params.put("captureLooping", looping ? Boolean.TRUE : Boolean.FALSE);
    String msg = vwr.processWriteOrCapture(params);
    if (msg == null)
      msg = "canceled";
    Logger.info(msg);
  }

  private void centerAt() throws ScriptException {

    //center {*}   # mean coordinate
    //select *; centerAt AVERAGE  # same
    //
    //center        # boundbox, not mean
    //boundbox *;centerAt BOUNDBOX #same
    //
    //center {2 2 2}   # center at a given point
    //centerAt ABSOLUTE {2 2 2}  #same

    int tok = getToken(1).tok;
    switch (tok) {
    case T.absolute:
    case T.average:
    case T.boundbox:
      break;
    default:
      invArg();
    }
    P3d pt = P3d.new3(0, 0, 0);
    if (slen == 5) {
      // centerAt xxx x y z
      pt.x = doubleParameter(2);
      pt.y = doubleParameter(3);
      pt.z = doubleParameter(4);
    } else if (e.isCenterParameter(2)) {
      pt = centerParameter(2);
      e.checkLast(e.iToken);
    } else {
      checkLength(2);
    }
    if (!chk && !vwr.isJmolDataFrame())
      vwr.tm.setCenterAt(tok, pt);
  }

  /**
   * Compares one set of atoms to another, generating the matrix that takes that
   * maps the first set's coordinates to the second's. Options TRANSLATE and
   * ROTATE will generate an animation. See interactive documentation for more
   * details.
   * 
   * @throws ScriptException
   */
  private void compare() throws ScriptException {
    // compare {model1} {model2} 
    // compare {model1} {model2} ATOMS {bsAtoms1} {bsAtoms2}
    // compare {model1} {model2} ORIENTATIONS
    // compare {model1} {model2} ORIENTATIONS {bsAtoms1} {bsAtoms2}
    // compare {model1} {model2} ORIENTATIONS [quaternionList1] [quaternionList2]
    // compare {model1} {model2} SMILES "....." (empty quotes use SMILES for model1
    // compare {model1} {model2} SMARTS "....."
    // compare {model1} {model2} FRAMES
    // compare {model1} ATOMS {bsAtoms1} [coords]
    // compare {model1} [coords] ATOMS {bsAtoms1} [coords]
    // compare {model1} [coords] ATOMS {bsAtoms1}
    // compare {model1} {model2} BONDS "....."   /// flexible fit
    // compare {model1} {model2} BONDS SMILES   /// flexible fit
    // compare {model1} {model2} ATOMS {bsAtoms1} {bsAtoms2} POLYHEDRA SMILES
    // compare FRAMES
    // compare {model1} {model2} POLYHEDRA
    // compare {model1} {model2} SUBSET {polyhedra} POLYHEDRA    
    // compare {model1} {model2} ATOMS @atom1 @atom2 POLYHEDRA
    // compare FRAMES POLYHEDRA
    // compare {*} @{{*}.plane([1 0 0])} MORPH TRANSLATE
    ScriptEval eval = e;
    boolean isQuaternion = false;
    boolean doRotate = false;
    boolean doTranslate = false;
    boolean doAnimate = false;
    boolean isFlexFit = false;
    boolean doMorph = false;
    Qd[] data1 = null, data2 = null;
    BS bsAtoms1 = null, bsAtoms2 = null;
    Lst<Object[]> vAtomSets = null;
    Lst<Object[]> vQuatSets = null;
    eval.iToken = 0;
    double nSeconds = (isFloatParameter(1) ? doubleParameter(++eval.iToken)
        : Double.NaN);

    ///BS bsFrom = (tokAt(++iToken) == T.subset ? null : atomExpressionAt(iToken));
    //BS bsTo = (tokAt(++iToken) == T.subset ? null : atomExpressionAt(iToken));
    //if (bsFrom == null || bsTo == null)
    ///invArg();
    P3d[] coordTo = null;
    BS bsFrom = null, bsTo = null;
    int tok = 0;
    if (tokAt(1) == T.frame) {
      bsFrom = vwr.bsA();
    } else {
      bsFrom = atomExpressionAt(1);
      if (eval.isArrayParameter(++eval.iToken)) {
        coordTo = eval.getPointArray(eval.iToken, -1, false, true);
      } else if ((tok = tokAt(eval.iToken)) != T.atoms && tok != T.frame) {
        bsTo = atomExpressionAt(eval.iToken);
      }
    }
    BS bsSubset = null;
    boolean isSmiles = false;
    boolean isPolyhedral = false;
    String strSmiles = null;
    BS bs = BSUtil.copy(bsFrom);
    if (bsTo != null)
      bs.or(bsTo);
    boolean isToSubsetOfFrom = (coordTo == null && bsTo != null
        && bs.equals(bsFrom));
    boolean isFrames = isToSubsetOfFrom;
    boolean isAtoms = (tok == T.atoms);
    boolean isCoords = false;
    for (int i = eval.iToken + 1; i < slen; ++i) {
      switch (getToken(i).tok) {
      case T.frame:
        isFrames = true;
        if (bsTo == null)
          bsTo = BSUtil.copy(bsFrom);
        break;
      case T.smiles:
        isSmiles = true;
        tok = tokAt(i + 1);
        if (tok != T.string && tok != T.polyhedra) {
          strSmiles = "*";
          break;
        }
        //$FALL-THROUGH$
      case T.search: // SMARTS
        isPolyhedral = (tokAt(++i) == T.polyhedra);
        strSmiles = (isPolyhedral ? "polyhedra" : stringParameter(i));
        if (strSmiles.equalsIgnoreCase("polyhedra")
            || strSmiles.equalsIgnoreCase("polyhedron"))
          isPolyhedral = true;
        break;
      case T.bonds:
        isFlexFit = true;
        doRotate = true;
        strSmiles = paramAsStr(++i);
        if (strSmiles.equalsIgnoreCase("SMILES")) {
          isSmiles = true;
          if (e.tokAt(i + 1) == T.hydrogen) {
            strSmiles = "H";
            i++;
          } else {
            strSmiles = "*";
          }
        }
        break;
      case T.decimal:
      case T.integer:
        nSeconds = Math.abs(doubleParameter(i));
        if (nSeconds > 0)
          doAnimate = true;
        break;
      case T.comma:
        break;
      case T.subset:
        bsSubset = atomExpressionAt(++i);
        i = eval.iToken;
        break;
      case T.bitset:
      case T.expressionBegin:
        if (vQuatSets != null)
          invArg();
        bsAtoms1 = atomExpressionAt(eval.iToken);
        tok = (isToSubsetOfFrom ? 0 : tokAt(eval.iToken + 1));
        bsAtoms2 = (coordTo == null && eval.isArrayParameter(eval.iToken + 1)
            ? null
            : (tok == T.bitset || tok == T.expressionBegin
                ? atomExpressionAt(++eval.iToken)
                : BSUtil.copy(bsAtoms1)));
        if (bsSubset != null) {
          bsAtoms1.and(bsSubset);
          if (bsAtoms2 != null)
            bsAtoms2.and(bsSubset);
        }

        if (bsAtoms2 == null) {
          int ipt = eval.iToken;
          coordTo = eval.getPointArray(eval.iToken + 1, -1, false, true);
          if (coordTo == null)
            eval.iToken = ipt;
          else
            isCoords = true;
        } else if (bsTo != null) {
          bsAtoms2.and(bsTo);
        }
        if (vAtomSets == null)
          vAtomSets = new Lst<Object[]>();
        vAtomSets.addLast(
            new Object[] { bsAtoms1, (bsAtoms2 == null ? coordTo : bsAtoms2) });
        if (isAtoms)
          coordTo = null;
        i = eval.iToken;
        break;
      case T.polyhedra:
        // Jmol 14.31.55/15.1.55
        isSmiles = isPolyhedral = true;
        break;
      case T.mapproperty:

        break;
      case T.varray:
        if (vAtomSets != null)
          invArg();
        isQuaternion = true;
        data1 = eval.getQuaternionArray(((SV) eval.theToken).getList(), T.list);
        getToken(++i);
        data2 = eval.getQuaternionArray(((SV) eval.theToken).getList(), T.list);
        if (vQuatSets == null)
          vQuatSets = new Lst<Object[]>();
        vQuatSets.addLast(new Object[] { data1, data2 });
        break;
      case T.orientation:
        isQuaternion = true;
        break;
      case T.atoms:
        isAtoms = true;
        isQuaternion = false;
        break;
      case T.point:
        isQuaternion = false;
        break;
      case T.morph:
        doMorph = true;
        break;
      case T.rotate:
        doRotate = true;
        break;
      case T.translate:
        doTranslate = true;
        break;
      default:
        invArg();
      }
    }
    if (chk)
      return;

    // processing
    if (isFrames)
      nSeconds = 0;
    if (Double.isNaN(nSeconds) || nSeconds < 0)
      nSeconds = 1;
    else if (!doMorph && !doRotate && !doTranslate)
      doRotate = doTranslate = true;
    doAnimate = (nSeconds != 0);

    isAtoms = (!isQuaternion && strSmiles == null && !isPolyhedral
        || coordTo != null);
    if (isAtoms)
      Interface.getInterface("javajs.util.Eigen", vwr, "script"); // preload interface
    if (vAtomSets == null && vQuatSets == null) {
      if (isPolyhedral && bsSubset == null) {
        bsSubset = vwr.getAtomBitSet("polyhedra");
      }
      if (bsSubset == null) {
        bsAtoms1 = (isAtoms ? vwr.getAtomBitSet("spine") : new BS());
        if (bsAtoms1.nextSetBit(0) < 0) {
          bsAtoms1 = bsFrom;
          bsAtoms2 = bsTo;
        } else {
          bsAtoms2 = BSUtil.copy(bsAtoms1);
          bsAtoms1.and(bsFrom);
          bsAtoms2.and(bsTo);
        }
      } else {
        bsAtoms1 = BSUtil.copy(bsFrom);
        bsAtoms2 = BSUtil.copy(bsTo);
        bsAtoms1.and(bsSubset);
        bsAtoms1.and(bsFrom);
        if (bsAtoms2 != null) {
          bsAtoms2.and(bsSubset);
          bsAtoms2.and(bsTo);
        }
      }
      vAtomSets = new Lst<Object[]>();
      vAtomSets.addLast(new BS[] { bsAtoms1, bsAtoms2 });
    }

    BS[] bsFrames;
    BS bsModels = null;
    if (isFrames) {
      // compare FRAMES 
      bsModels = vwr.ms.getModelBS(bsFrom, false);
      bsFrames = new BS[bsModels.cardinality()];
      for (int i = 0, iModel = bsModels.nextSetBit(
          0); iModel >= 0; iModel = bsModels.nextSetBit(iModel + 1), i++)
        bsFrames[i] = vwr.getModelUndeletedAtomsBitSet(iModel);
    } else {
      bsModels = BSUtil.newAndSetBit(0);
      bsFrames = new BS[] { bsFrom };
    }
    for (int iFrame = 0, iModel = bsModels
        .nextSetBit(0); iFrame < bsFrames.length; iFrame++, iModel = bsModels
            .nextSetBit(iModel + 1)) {
      bsFrom = bsFrames[iFrame];
      double[] retStddev = new double[2]; // [0] final, [1] initial for atoms
      if (isFrames && isPolyhedral && iFrame == 0) {
        bsTo = bsFrom;
        continue;
      }

      Qd q = null;
      Lst<Qd> vQ = new Lst<Qd>();
      P3d[][] centerAndPoints = null;
      Lst<Object[]> vAtomSets2 = (isFrames ? new Lst<Object[]>() : vAtomSets);
      if (!isCoords) {
        for (int i = 0; i < vAtomSets.size(); ++i) {
          Object[] bss = vAtomSets.get(i);
          if (isFrames)
            vAtomSets2.addLast(
                bss = new BS[] { BSUtil.copy((BS) bss[0]), (BS) bss[1] });
          ((BS) bss[0]).and(bsFrom);
        }
      }
      P3d center = null;
      V3d translation = null;
      if (isAtoms) {
        if (coordTo != null) {
          vAtomSets2.clear();
          vAtomSets2.addLast(new Object[] { bsAtoms1, coordTo });
        }
        try {
          centerAndPoints = vwr.getCenterAndPoints(vAtomSets2, !doMorph);
        } catch (Exception ex) {
          invArg();
        }
        if (doMorph) {
          // nothing to do
        } else {
          int n = centerAndPoints[0].length - 1;
          for (int i = 1; i <= n; i++) {
            P3d aij = centerAndPoints[0][i];
            P3d bij = centerAndPoints[1][i];
            if (!(aij instanceof Atom) || !(bij instanceof Atom))
              break;
            if (!isFrames)
              Logger.info(" atom 1 " + ((Atom) aij).getInfo() + "\tatom 2 "
                  + ((Atom) bij).getInfo());
          }
          q = MeasureD.calculateQuaternionRotation(centerAndPoints, retStddev);
          double r0 = (Double.isNaN(retStddev[1]) ? Double.NaN
              : Math.round(retStddev[0] * 100) / 100d);
          double r1 = (Double.isNaN(retStddev[1]) ? Double.NaN
              : Math.round(retStddev[1] * 100) / 100d);
          showString("RMSD " + r0 + " --> " + r1 + " Angstroms");
        }
      } else if (isQuaternion) {
        if (vQuatSets == null) {
          for (int i = 0; i < vAtomSets2.size(); i++) {
            BS[] bss = (BS[]) vAtomSets2.get(i);
            data1 = vwr.getAtomGroupQuaternions(bss[0], Integer.MAX_VALUE);
            data2 = vwr.getAtomGroupQuaternions(bss[1], Integer.MAX_VALUE);
            for (int j = 0; j < data1.length && j < data2.length; j++) {
              vQ.addLast(data2[j].div(data1[j]));
            }
          }
        } else {
          for (int j = 0; j < data1.length && j < data2.length; j++) {
            vQ.addLast(data2[j].div(data1[j]));
          }
        }
        retStddev[0] = 0;
        data1 = vQ.toArray(new Qd[vQ.size()]);
        q = Qd.sphereMean(data1, retStddev, 0.0001);
        showString("RMSD = " + retStddev[0] + " degrees");
      } else {
        // SMILES
        /* not sure why this was like this:
        if (vAtomSets == null) {
          vAtomSets = new  List<BitSet[]>();
        }
        bsAtoms1 = BitSetUtil.copy(bsFrom);
        bsAtoms2 = BitSetUtil.copy(bsTo);
        vAtomSets.add(new BitSet[] { bsAtoms1, bsAtoms2 });
        */

        M4d m4 = new M4d();
        center = new P3d();
        if (bsFrom != null && strSmiles != null && ("H".equals(strSmiles)
            || "*".equals(strSmiles) || "".equals(strSmiles)))
          try {
            strSmiles = vwr.getSmilesOpt(bsAtoms1 == null ? bsFrom : bsAtoms1,
                -1, -1,
                JC.SMILES_GEN_ALL_COMPONENTS
                    | ("H".equals(strSmiles) ? JC.SMILES_GEN_EXPLICIT_H_ALL
                        : 0),
                null);
          } catch (Exception ex) {
            eval.evalError(ex.getMessage(), null);
          }
        if (isFlexFit) {
          double[] list;
          if (bsFrom == null || bsTo == null || (list = eval.getSmilesExt()
              .getFlexFitList(bsFrom, bsTo, strSmiles, !isSmiles)) == null)
            return;
          vwr.setDihedrals(list, null, 1);
        }
        double stddev;
        if (isPolyhedral) {
          BS bs1 = BS.copy(bsAtoms1);
          bs1.and(bsFrom);
          BS bs2 = BS.copy(bsAtoms2);
          bs2.and(bsTo);
          stddev = eval.getSmilesExt().mapPolyhedra(bs1.nextSetBit(0),
              bs2.nextSetBit(0), isSmiles, m4);
        } else {
          showString("COMPARE using " + (isSmiles ? "SMILES " : "SMARTS ")
              + strSmiles);
          Lst<P3d> ptsA = new Lst<P3d>();
          Lst<P3d> ptsB = new Lst<P3d>();
          stddev = eval.getSmilesExt().getSmilesCorrelation(
              bsAtoms1 == null ? bsFrom : bsAtoms1,
              bsAtoms2 == null ? bsTo : bsAtoms2, strSmiles, ptsA, ptsB, m4,
              null, false, null, center, false, JC.SMILES_IGNORE_STEREOCHEMISTRY
                  | (isSmiles ? JC.SMILES_TYPE_SMILES : JC.SMILES_TYPE_SMARTS));
        }
        if (Double.isNaN(stddev)) {
          showString("structures do not match from " + bsFrom + " to " + bsTo);
          return;
        }
        if (doTranslate) {
          translation = new V3d();
          m4.getTranslation(translation);
        }
        if (doRotate) {
          M3d m3 = new M3d();
          m4.getRotationScale(m3);
          q = Qd.newM(m3);
        }
        showString("RMSD = " + stddev + " Angstroms");

        if (isFrames) {
          T3d[] oabc = vwr.getV0abc(iModel, new Object[] { m4 });
          if (oabc != null)
            vwr.ms.setModelCagePts(iModel, oabc, null);
        }

      }
      if (centerAndPoints != null)
        center = centerAndPoints[0][0];
      if (center == null) {
        centerAndPoints = vwr.getCenterAndPoints(vAtomSets2, true);
        center = centerAndPoints[0][0];
      }
      P3d pt1 = new P3d();
      double endDegrees = Double.NaN;
      if (doMorph) {
        endDegrees = 1e10d;
      } else {
        if (doTranslate) {
          if (translation == null)
            translation = V3d.newVsub(centerAndPoints[1][0], center);
          endDegrees = 1e10d;
        }
        if (doRotate) {
          if (q == null)
            eval.evalError("option not implemented", null);
          pt1.add2(center, q.getNormal());
          endDegrees = q.getTheta();
          if (endDegrees == 0 && doTranslate) {
            if (translation.length() > 0.01d)
              endDegrees = 1e10d;
            else if (isFrames)
              continue;
            else
              doRotate = doTranslate = doAnimate = false;
          }
        }
      }
      if (Double.isNaN(endDegrees) || Double.isNaN(pt1.x))
        continue;
      Lst<P3d> ptsB = null;
      if (doMorph) {
        ptsB = new Lst<>();
        for (int n = centerAndPoints[0].length, i = 0; i < n; i++) {
          ptsB.addLast(centerAndPoints[1][i]);
        }
      } else if (doRotate && doTranslate && nSeconds != 0) {
        Lst<P3d> ptsA = vwr.ms.getAtomPointVector(bsFrom);
        M4d m4 = ScriptMathProcessor.getMatrix4f(q.getMatrix(), translation);
        ptsB = ScriptParam.transformPoints(ptsA, m4, center);
      }
      if (!eval.useThreads(true))
        doAnimate = false;
      if (vwr.rotateAboutPointsInternal(eval, center, pt1,
          endDegrees / nSeconds, endDegrees, doAnimate, bsFrom, translation,
          ptsB, null, null, null, false, doMorph ? centerAndPoints : null) && doAnimate && eval.isJS)
        throw new ScriptInterruption(eval, "compare", 1);
    }
  }

  private void configuration() throws ScriptException {
    // if (!chk && vwr.getDisplayModelIndex() <= -2)
    // error(ERROR_backgroundModelError, "\"CONFIGURATION\"");
    BS bsAtoms = null;
    BS bsSelected = (chk ? null : vwr.bsA());
    if (slen == 1) {
      if (chk)
        return;
      // configuration
      bsAtoms = vwr.ms.setConformation(bsSelected);
      vwr.ms.addStateScript("select", null, bsSelected, null, ";configuration",
          true, false);
    } else {
      int n = Integer.MIN_VALUE;
      BS bs = null;
      if (tokAt(1) == T.string) {
        n = -1000 - (stringParameter(e.checkLast(1)) + " ").codePointAt(0);
      } else if (tokAt(1) == T.integer) {
        n = intParameter(e.checkLast(1));
      } else {
        bs = bsAtoms = atomExpressionAt(1);
        if (tokAt(e.iToken) == T.string) {
          n = -1000 - (stringParameter(e.checkLast(e.iToken + 1)) + " ")
              .codePointAt(0);
        } else {
          n = intParameter(e.checkLast(e.iToken + 1));
        }
      }
      if (chk)
        return;
      vwr.addStateScript("configuration "
          + (bs == null ? "" : Escape.eBS(bsAtoms) + " ") + n + ";", true,
          false);
      bsAtoms = vwr.ms.getConformation(vwr.am.cmi, n, true, bs);
    }
    setShapeProperty(JC.SHAPE_STICKS, "type",
        Integer.valueOf(Edge.BOND_HYDROGEN_MASK));
    e.setShapeSizeBs(JC.SHAPE_STICKS, 0, bsAtoms);
    vwr.autoHbond(bsAtoms, bsAtoms, true);
    vwr.selectStatus(bsAtoms, false, 0, !e.doReport(), false);
  }

  private void measure() throws ScriptException {
    ScriptEval eval = e;
    String id = null;
    int pt = 1;
    short colix = 0;
    double[] offset = null;
    if (slen == 2)
      switch (tokAt(1)) {
      case T.off:
        setShapeProperty(JC.SHAPE_MEASURES, "hideAll", Boolean.TRUE);
        return;
      case T.delete:
        if (!chk)
          vwr.clearAllMeasurements();
        return;
      case T.selected:
        if (!chk)
          showMeasureCount(true);
        return;
      }
    vwr.shm.loadShape(JC.SHAPE_MEASURES);
    switch (tokAt(1)) {
    case T.search:
      String smarts = stringParameter(slen == 3 ? 2 : 4);
      if (chk)
        return;
      Atom[] atoms = vwr.ms.at;
      int ac = vwr.ms.ac;
      int[][] maps = null;
      try {
        maps = vwr.getSmilesMatcher().getCorrelationMaps(smarts, atoms, ac,
            vwr.bsA(), JC.SMILES_TYPE_SMARTS);
      } catch (Exception ex) {
        eval.evalError(ex.getMessage(), null);
      }
      if (maps == null)
        return;
      setShapeProperty(JC.SHAPE_MEASURES, "maps", maps);
      return;
    }
    switch (slen) {
    case 2:
      switch (getToken(pt).tok) {
      case T.nada:
      case T.on:
        vwr.shm.loadShape(JC.SHAPE_MEASURES);
        setShapeProperty(JC.SHAPE_MEASURES, "hideAll", Boolean.FALSE);
        return;
      case T.list:
        if (!chk)
          eval.showStringPrint(vwr.getMeasurementInfoAsString(), false);
        return;
      case T.string:
        setShapeProperty(JC.SHAPE_MEASURES, "setFormats", stringParameter(1));
        return;
      }
      eval.errorStr(ScriptError.ERROR_keywordExpected, "ON, OFF, DELETE");
      break;
    case 3: // measure delete N
      // search "smartsString"
      switch (getToken(1).tok) {
      case T.delete:
        if (getToken(2).tok == T.all) {
          if (!chk)
            vwr.clearAllMeasurements();
        } else {
          int i = intParameter(2) - 1;
          if (!chk)
            vwr.deleteMeasurement(i);
        }
        return;
      }
    }

    int nAtoms = 0;
    int expressionCount = 0;
    int modelIndex = -1;
    int atomIndex = -1;
    int ptFloat = -1;
    int[] countPlusIndexes = new int[5];
    double[] rangeMinMax = new double[] { Double.MAX_VALUE, Double.MAX_VALUE };
    boolean isAll = false;
    boolean isAllConnected = false;
    boolean isNotConnected = false;
    boolean isRange = true;
    boolean isRefreshID = false;
    RadiusData rd = null;
    Boolean intramolecular = null;
    int tokAction = T.opToggle;
    String strFormat = null;
    String property = null;
    String units = null;
    Font font = null;

    Lst<Object> points = new Lst<Object>();
    BS bs = new BS();
    BS bsSelected = null;
    Object target = null;
    TickInfo tickInfo = null;
    int nBitSets = 0;
    int mad = 0;
    double value = Double.NaN;
    String alignment = null;
    for (int i = 1; i < slen; ++i) {
      switch (getToken(i).tok) {
      default:
        error(ScriptError.ERROR_expressionOrIntegerExpected);
        break;
      case T.id:
        if (i != 1)
          invArg();
        id = eval.optParameterAsString(++i);
        continue;
      case T.identifier:
        eval.errorStr(ScriptError.ERROR_keywordExpected,
            "ALL, ALLCONNECTED, DELETE");
        break;
      case T.select:
        tokAction = T.select;
        if (tokAt(i + 1) == T.all && slen == i + 2) {
          if (!chk) {
            vwr.setShapeProperty(JC.SHAPE_MEASURES, "selectall", null);
            showMeasureCount(true);
          }
          return;
        }
        break;
      case T.selected:
        if (points.size() > 0 || nBitSets > 0 || id != null)
          invArg();
        tokAction = T.select;
        if (chk) {
          bsSelected = new BS();
        } else {
          bsSelected = (BS) vwr.getShapeProperty(JC.SHAPE_MEASURES, "selected");
          if (bsSelected.cardinality() == 0)
            return;
        }
        isAll = true;
        break;
      case T.opNot:
        if (tokAt(i + 1) != T.connected)
          invArg();
        i++;
        isNotConnected = true;
        break;
      case T.align:
        alignment = paramAsStr(++i).toLowerCase();
        break;
      case T.connected:
      case T.allconnected:
      case T.all:
        isAllConnected = (eval.theTok == T.allconnected);
        atomIndex = -1;
        isAll = true;
        if (isAllConnected && isNotConnected)
          invArg();
        break;
      case T.color:
        colix = C.getColix(eval.getArgbParam(++i));
        i = eval.iToken;
        break;
      case T.offset:
        if (eval.isPoint3f(++i)) {
          // PyMOL offsets -- {x, y, z} in angstroms
          P3d p = getPoint3f(i, false);
          offset = new double[] { 1, p.x, p.y, p.z, 0, 0, 0 };
        } else {
          offset = eval.doubleParameterSet(i, 7, 7);
        }
        i = eval.iToken;
        break;
      case T.radius:
      case T.diameter:
        mad = (int) ((eval.theTok == T.radius ? 2000 : 1000)
            * doubleParameter(++i));
        if (id != null && mad <= 0)
          mad = -1;
        break;
      case T.decimal:
        if (rd != null)
          invArg();
        isAll = true;
        isRange = true;
        ptFloat = (ptFloat + 1) % 2;
        rangeMinMax[ptFloat] = doubleParameter(i);
        break;
      case T.delete:
        if (tokAction != T.opToggle && tokAction != T.select)
          invArg();
        tokAction = T.delete;
        break;
      case T.font:
        double fontsize = doubleParameter(++i);
        String fontface = paramAsStr(++i);
        String fontstyle = paramAsStr(++i);
        if (!chk)
          font = vwr.getFont3D(fontface, fontstyle, fontsize);
        break;
      case T.integer:
        int iParam = intParameter(i);
        if (isAll) {
          isRange = true; // irrelevant if just four integers
          ptFloat = (ptFloat + 1) % 2;
          rangeMinMax[ptFloat] = iParam;
        } else {
          atomIndex = vwr.ms.getFirstAtomIndexFromAtomNumber(iParam,
              vwr.getVisibleFramesBitSet());
          if (!chk && atomIndex < 0)
            return;
          if (target != null)
            invArg();
          if ((countPlusIndexes[0] = ++nAtoms) > 4)
            eval.bad();
          countPlusIndexes[nAtoms] = atomIndex;
        }
        break;
      case T.modelindex:
        modelIndex = intParameter(++i);
        break;
      case T.off:
        if (tokAction != T.opToggle)
          invArg();
        tokAction = T.off;
        break;
      case T.on:
        if (tokAction != T.opToggle)
          invArg();
        tokAction = T.on;
        break;
      case T.range:
        isAll = true;
        isRange = true; // unnecessary
        atomIndex = -1;
        break;
      case T.intramolecular:
      case T.intermolecular:
        intramolecular = Boolean.valueOf(eval.theTok == T.intramolecular);
        isAll = true;
        isNotConnected = (eval.theTok == T.intermolecular);
        break;
      case T.vanderwaals:
        if (ptFloat >= 0)
          invArg();
        rd = eval.encodeRadiusParameter(i, false, true);
        if (rd == null)
          return;
        rd.values = rangeMinMax;
        i = eval.iToken;
        isNotConnected = true;
        isAll = true;
        intramolecular = Boolean.valueOf(false);
        if (nBitSets == 1) {
          nBitSets++;
          nAtoms++;
          BS bs2 = BSUtil.copy(bs);
          BSUtil.invertInPlace(bs2, vwr.ms.ac);
          bs2.and(vwr.ms.getAtomsWithinRadius(5, bs, false, null, null));
          points.addLast(bs2);
        }
        break;
      case T.define:
      case T.bitset:
      case T.expressionBegin:
        nBitSets++;
        //$FALL-THROUGH$
      case T.leftbrace:
      case T.point3f:
      case T.dollarsign:
        if (atomIndex >= 0 || bsSelected != null)
          invArg();
        Object[] ret = new Object[1];
        target = eval.centerParameter(i, ret);
        if (ret[0] instanceof BS) {
          target = bs = (BS) ret[0];
          if (!chk && bs.length() == 0)
            return;
        }
        if (target instanceof P3d) {
          Point3fi v = new Point3fi();
          v.setT((P3d) target);
          v.mi = (short) modelIndex;
          target = v;
        }
        if ((nAtoms = ++expressionCount) > 4)
          eval.bad();
        i = eval.iToken;
        points.addLast(target);
        break;
      case T.property:
        property = paramAsStr(i);
        break;
      case T.val:
        value = doubleParameter(++i);
        break;
      case T.string:
        // measures "%a1 %a2 %v %u"
        String s = stringParameter(i);
        if (Measurement.isUnits(s) || s.equals("default")) {
          units = s;
        } else {
          strFormat = s;
        }
        break;
      case T.ticks:
        tickInfo = eval.tickParamAsStr(i, false, true, true);
        i = eval.iToken;
        tokAction = T.define;
        break;
      }
    }
    if (rd != null && (ptFloat >= 0 || nAtoms != 2)
        || tokAction != T.select && bsSelected == null && nAtoms < 2
            && id == null && (tickInfo == null || nAtoms == 1))
      eval.bad();
    if (strFormat != null && strFormat.indexOf(nAtoms + ":") != 0)
      strFormat = nAtoms + ":" + strFormat;
    if (isRange) {
      if (rangeMinMax[1] < rangeMinMax[0]) {
        rangeMinMax[1] = rangeMinMax[0];
        rangeMinMax[0] = (rangeMinMax[1] == Double.MAX_VALUE ? Double.MAX_VALUE
            : -200);
      }
    }
    if (chk)
      return;

    isRefreshID = (id != null && target == null && tokAction == T.opToggle);
    if (target != null || bsSelected != null || tickInfo != null
        || isRefreshID) {
      if (rd == null)
        rd = new RadiusData(rangeMinMax, 0, null, null);
      if (tickInfo != null)
        tickInfo.id = "default";
      if (isRefreshID) {
        tokAction = T.refresh;
      } else if (target != null && (property != null || strFormat != null)
          && tokAction == T.opToggle) {
        tokAction = T.define;
      }
      Text text = null;
      if (font != null || alignment != null || colix != 0
          || strFormat != null && (isRefreshID || strFormat.indexOf('\n') >= 0))
        text = Text.newMeasure(vwr, font, colix);
      if (text != null) {
        text.pymolOffset = offset;
        text.setAlignmentLCR(alignment);
      }
      setShapeProperty(JC.SHAPE_MEASURES, "measure",
          vwr.newMeasurementData(id, points).set(tokAction, null, rd, property,
              strFormat, units, tickInfo, isAllConnected, isNotConnected,
              intramolecular, isAll, mad, colix, text, value, bsSelected));
      if (tokAction == T.select) {
        showMeasureCount(false);
      }
      return;
    }
    Object propertyValue = (id == null ? countPlusIndexes : id);
    switch (tokAction) {
    case T.delete:
      setShapeProperty(JC.SHAPE_MEASURES, "delete", propertyValue);
      break;
    case T.on:
      setShapeProperty(JC.SHAPE_MEASURES, "show", propertyValue);
      break;
    case T.off:
      setShapeProperty(JC.SHAPE_MEASURES, "hide", propertyValue);
      break;
    default:
      setShapeProperty(JC.SHAPE_MEASURES,
          (strFormat == null && units == null ? "toggle" : "toggleOn"),
          propertyValue);
      if (strFormat != null)
        setShapeProperty(JC.SHAPE_MEASURES, "setFormats", strFormat);
    }
  }

  private void showMeasureCount(boolean isFull) {
    BS bs = (BS) vwr.getShapeProperty(JC.SHAPE_MEASURES, "selected");
    int n = bs.cardinality();
    if (isFull) {
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        showString(((Measures) vwr.shm.getShape(JC.SHAPE_MEASURES))
            .getInfoAsString(i));
      }
    }
    showString(n + (n == 1 ? " measure" : " measures") + " selected");
  }

  /**
   * 
   * @param index
   *        0 indicates hbond command
   * 
   * @throws ScriptException
   */
  private void connect(int index) throws ScriptException {
    ScriptEval eval = e;
    final double[] distances = new double[2];
    BS[] atomSets = new BS[2];
    atomSets[0] = atomSets[1] = vwr.bsA();
    double radius = Double.NaN;
    int[] colorArgb = new int[] { Integer.MIN_VALUE };
    int distanceCount = 0;
    int bondOrder = Edge.BOND_ORDER_NULL;
    int bo;
    int operation = T.modifyorcreate;
    boolean isDelete = false;
    boolean haveType = false;
    boolean haveOperation = false;
    double translucentLevel = Double.MAX_VALUE;
    boolean isColorOrRadius = false;
    int nAtomSets = 0;
    int nDistances = 0;
    BS bsBonds = new BS();
    boolean isBonds = false;
    int expression2 = 0;
    int ptColor = 0;
    double energy = 0;
    boolean addGroup = false;
    /*
     * connect [<=2 distance parameters] [<=2 atom sets] [<=1 bond type] [<=1
     * operation]
     */

    if (slen == 1) {
      if (!chk)
        vwr.rebondState(eval.isStateScript);
      return;
    }

    if (tokAt(1) == T.nbo) {
      if (!chk)
        vwr.connectNBO(e.optParameterAsString(2));
      return;
    }
    for (int i = index; i < slen; ++i) {
      switch (getToken(i).tok) {

      case T.on:
      case T.off:
        checkLength(2);
        if (!chk)
          vwr.rebondState(eval.isStateScript);
        return;
      case T.integer:
      case T.decimal:
        if (nAtomSets > 0) {
          if (haveType || isColorOrRadius)
            eval.error(ScriptError.ERROR_invalidParameterOrder);
          bo = Edge.getBondOrderFromFloat(doubleParameter(i));
          if (bo == Edge.BOND_ORDER_NULL)
            invArg();
          bondOrder = bo;
          haveType = true;
          break;
        }
        if (++nDistances > 2)
          eval.bad();
        double dist = doubleParameter(i);
        if (tokAt(i + 1) == T.percent) {
          dist = -dist / 100d;
          i++;
        }
        distances[distanceCount++] = dist;
        break;
      case T.define:
      case T.bitset:
      case T.expressionBegin:
        if (nAtomSets > 2 || isBonds && nAtomSets > 0)
          eval.bad();
        if (haveType || isColorOrRadius)
          invArg();
        atomSets[nAtomSets++] = atomExpressionAt(i);
        isBonds = eval.isBondSet;
        if (nAtomSets == 2) {
          int pt = eval.iToken;
          for (int j = i; j < pt; j++)
            if (tokAt(j) == T.identifier && paramAsStr(j).equals("_1")) {
              expression2 = i;
              break;
            }
          eval.iToken = pt;
        }
        i = eval.iToken;
        break;
      case T.group:
        addGroup = true;
        break;
      case T.color:
      case T.translucent:
      case T.opaque:
        isColorOrRadius = true;
        translucentLevel = getColorTrans(eval, i, false, colorArgb);
        i = eval.iToken;
        break;
      case T.pdb:
        boolean isAuto = (tokAt(2) == T.auto);
        checkLength(isAuto ? 3 : 2);
        if (chk)
          return;
        // from eval
        vwr.clearModelDependentObjects();
        vwr.ms.deleteAllBonds();
        BS bsExclude = new BS();
        vwr.ms.setPdbConectBonding(0, 0, bsExclude);
        if (isAuto) {
          boolean isLegacy = eval.isStateScript
              && vwr.getBoolean(T.legacyautobonding);
          vwr.ms.autoBondBs4(null, null, bsExclude, null, vwr.getMadBond(),
              isLegacy, null);
          vwr.addStateScript((isLegacy
              ? "set legacyAutoBonding TRUE;connect PDB AUTO;set legacyAutoBonding FALSE;"
              : "connect PDB auto;"), false, true);
          return;
        }
        vwr.addStateScript("connect PDB;", false, true);
        return;
      case T.auto:
      case T.create:
      case T.modify:
      case T.modifyorcreate:
        // must be an operation and must be last argument
        haveOperation = true;
        if (++i != slen)
          invArg();
        operation = eval.theTok;
        if (operation == T.auto && !(bondOrder == Edge.BOND_ORDER_NULL
            || bondOrder == Edge.BOND_H_REGULAR
            || bondOrder == Edge.BOND_AROMATIC))
          invArg();
        break;
      case T.struts:
        if (!isColorOrRadius) {
          colorArgb[0] = 0xFFFFFF;
          translucentLevel = 0.5d;
          radius = vwr.getDouble(T.strutdefaultradius);
          isColorOrRadius = true;
        }
        if (!haveOperation) {
          operation = T.modifyorcreate;
          haveOperation = true;
        }
        //$FALL-THROUGH$
      case T.identifier:
        if (eval.isColorParam(i)) {
          ptColor = -i;
          break;
        }
        //$FALL-THROUGH$
      case T.aromatic:
      case T.hbond:
        //if (i > 0) {
        // not hbond command
        // I know -- should have required the COLOR keyword
        //}
        String cmd = paramAsStr(i);
        if ((bo = Edge.getBondOrderFromString(cmd)) == Edge.BOND_ORDER_NULL)
          invArg();
        // must be bond type
        if (haveType)
          eval.error(ScriptError.ERROR_incompatibleArguments);
        haveType = true;
        switch (bo) {
        case Edge.BOND_PARTIAL01:
          switch (tokAt(i + 1)) {
          case T.decimal:
            bo = Edge.getPartialBondOrderFromFloatEncodedInt(st[++i].intValue);
            break;
          case T.integer:
            bo = (short) intParameter(++i);
            break;
          }
          break;
        case Edge.BOND_H_REGULAR:
          if (tokAt(i + 1) == T.integer) {
            bo = (short) (intParameter(++i) << Edge.BOND_HBOND_SHIFT);
            energy = doubleParameter(++i);
          }
          break;
        case Edge.TYPE_ATROPISOMER:
          if (!haveOperation) {
            operation = T.modify;
            haveOperation = true;
          }
          break;
        }
        bondOrder = bo;
        break;
      case T.radius:
        radius = doubleParameter(++i);
        isColorOrRadius = true;
        break;
      case T.none:
        if (++i != slen)
          invArg();
        //$FALL-THROUGH$
      case T.delete:
        operation = T.delete;
        // if (isColorOrRadius) / for struts automatic color
        // invArg();
        isDelete = true;
        isColorOrRadius = false;
        break;
      default:
        ptColor = i;
        break;
      }
      // now check for color -- -i means we've already checked
      if (i > 0) {
        if (ptColor == -i || ptColor == i && eval.isColorParam(i)) {
          isColorOrRadius = true;
          colorArgb[0] = eval.getArgbParam(i);
          i = eval.iToken;
        } else if (ptColor == i) {
          invArg();
        }
      }
    }
    if (chk)
      return;
    if (distanceCount < 2) {
      if (distanceCount == 0)
        distances[0] = JC.DEFAULT_MAX_CONNECT_DISTANCE;
      distances[1] = distances[0];
      distances[0] = JC.DEFAULT_MIN_CONNECT_DISTANCE;
    }
    if (isColorOrRadius) {
      if (!haveType)
        bondOrder = Edge.BOND_ORDER_ANY;
      if (!haveOperation)
        operation = T.modify;
    }
    int nNew = 0;
    int nModified = 0;
    int[] result;
    if (expression2 > 0) {
      BS bs = new BS();
      vwr.definedAtomSets.put("_1", bs);
      BS bs0 = atomSets[0];
      for (int atom1 = bs0.nextSetBit(0); atom1 >= 0; atom1 = bs0
          .nextSetBit(atom1 + 1)) {
        bs.set(atom1);
        result = vwr.makeConnections(distances[0], distances[1], bondOrder,
            operation, bs, atomExpressionAt(expression2), bsBonds, isBonds,
            false, 0);
        nNew += Math.abs(result[0]);
        nModified += result[1];
        bs.clear(atom1);
      }
    } else {
      result = vwr.makeConnections(distances[0], distances[1], bondOrder,
          operation, atomSets[0], atomSets[1], bsBonds, isBonds, addGroup,
          energy);
      nNew += Math.abs(result[0]);
      nModified += result[1];
    }
    boolean report = eval.doReport();
    if (isDelete) {
      if (report)
        eval.report(GT.i(GT.$("{0} connections deleted"), nModified), false);
      return;
    }
    if (isColorOrRadius) {
      vwr.selectBonds(bsBonds);
      if (!Double.isNaN(radius))
        eval.setShapeSizeBs(JC.SHAPE_STICKS, (int) Math.round(radius * 2000),
            null);
      finalizeObject(JC.SHAPE_STICKS, colorArgb[0], translucentLevel, 0, false,
          null, 0, bsBonds);
      vwr.selectBonds(null);
    }
    if (report)
      eval.report(GT.o(GT.$("{0} new bonds; {1} modified"),
          new Object[] { Integer.valueOf(nNew), Integer.valueOf(nModified) }),
          false);
  }

  private void console() throws ScriptException {
    switch (getToken(1).tok) {
    case T.off:
      if (!chk)
        vwr.showConsole(false);
      break;
    case T.on:
      if (!chk)
        vwr.showConsole(true);
      break;
    case T.clear:
      if (!chk)
        vwr.sm.clearConsole();
      break;
    case T.write:
      showString(stringParameter(2));
      break;
    default:
      invArg();
    }
  }

  private void data() throws ScriptException {
    ScriptEval eval = e;
    String dataString = null;
    String dataLabel = null;
    boolean isOneValue = false;
    int i;
    switch (eval.iToken = slen) {
    case 5:
      // parameters 3 and 4 are just for the ride: [end] and ["key"]
      dataString = paramAsStr(2);
      //$FALL-THROUGH$
    case 4:
    case 2:
      dataLabel = paramAsStr(1);
      if (dataLabel.equalsIgnoreCase("clear")) {
        if (!chk)
          vwr.setData(null, null, 0, 0, 0, 0, 0);
        return;
      }
      if ((i = dataLabel.indexOf("@")) >= 0) {
        dataString = ""
            + eval.getParameter(dataLabel.substring(i + 1), T.string, true);
        dataLabel = dataLabel.substring(0, i).trim();
      } else if (dataString == null && (i = dataLabel.indexOf(" ")) >= 0) {
        dataString = dataLabel.substring(i + 1).trim();
        dataLabel = dataLabel.substring(0, i).trim();
        isOneValue = true;
      }
      break;
    default:
      eval.bad();
    }
    String dataType = dataLabel.substring(0, (dataLabel + " ").indexOf(" "))
        .toLowerCase();
    if (dataType.equals("model") || dataType.equals("append")) {
      eval.cmdLoad();
      return;
    }
    if (chk)
      return;
    boolean isDefault = (dataLabel.toLowerCase().indexOf("(default)") >= 0);
    if (dataType.equals("connect_atoms")) {
      vwr.ms.connect((double[][]) parseDataArray(dataString, false));
      return;
    }
    if (dataType.indexOf("ligand_") == 0) {
      // ligand structure for pdbAddHydrogen
      vwr.setLigandModel(dataLabel.substring(7).toUpperCase() + "_data",
          dataString.trim());
      return;
    }
    if (dataType.indexOf("file_") == 0) {
      // ligand structure for pdbAddHydrogen
      vwr.setLigandModel(dataLabel.substring(5) + "_file", dataString.trim());
      return;
    }
    Object[] d = new Object[4];
    // not saving this data in the state?
    if (dataType.equals("element_vdw")) {
      // vdw for now
      d[JmolDataManager.DATA_LABEL] = dataType;
      d[JmolDataManager.DATA_VALUE] = dataString.replace(';', '\n');
      int n = Elements.elementNumberMax;
      int[] eArray = new int[n + 1];
      for (int ie = 1; ie <= n; ie++)
        eArray[ie] = ie;
      d[JmolDataManager.DATA_SELECTION] = eArray;
      d[JmolDataManager.DATA_TYPE] = Integer
          .valueOf(JmolDataManager.DATA_TYPE_STRING);
      vwr.setData("element_vdw", d, n, 0, 0, 0, 0);
      return;
    }
    if (dataType.indexOf("data2d_") == 0) {
      // data2d_someName
      d[JmolDataManager.DATA_LABEL] = dataLabel;
      d[JmolDataManager.DATA_VALUE] = parseDataArray(dataString, false);
      d[JmolDataManager.DATA_TYPE] = Integer
          .valueOf(JmolDataManager.DATA_TYPE_ADD);
      vwr.setData(dataLabel, d, 0, 0, 0, 0, 0);
      return;
    }
    if (dataType.indexOf("data3d_") == 0) {
      // data3d_someName
      d[JmolDataManager.DATA_LABEL] = dataLabel;
      d[JmolDataManager.DATA_VALUE] = parseDataArray(dataString, true);
      d[JmolDataManager.DATA_TYPE] = Integer
          .valueOf(JmolDataManager.DATA_TYPE_ADDD);
      vwr.setData(dataLabel, d, 0, 0, 0, 0, 0);
      return;
    }
    String[] tokens = PT.getTokens(dataLabel);
    if (dataType.indexOf("property_") == 0
        && !(tokens.length == 2 && tokens[1].equals("set"))) {
      BS bs = vwr.bsA();
      d[JmolDataManager.DATA_LABEL] = dataType;
      int atomNumberField = (isOneValue ? 0
          : ((Integer) vwr.getP("propertyAtomNumberField")).intValue());
      int atomNumberFieldColumnCount = (isOneValue ? 0
          : ((Integer) vwr.getP("propertyAtomNumberColumnCount")).intValue());
      int propertyField = (isOneValue ? Integer.MIN_VALUE
          : ((Integer) vwr.getP("propertyDataField")).intValue());
      int propertyFieldColumnCount = (isOneValue ? 0
          : ((Integer) vwr.getP("propertyDataColumnCount")).intValue());
      if (!isOneValue && dataLabel.indexOf(" ") >= 0) {
        if (tokens.length == 3) {
          // DATA "property_whatever [atomField] [propertyField]"
          dataLabel = tokens[0];
          atomNumberField = PT.parseInt(tokens[1]);
          propertyField = PT.parseInt(tokens[2]);
        }
        if (tokens.length == 5) {
          // DATA
          // "property_whatever [atomField] [atomFieldColumnCount] [propertyField] [propertyDataColumnCount]"
          dataLabel = tokens[0];
          atomNumberField = PT.parseInt(tokens[1]);
          atomNumberFieldColumnCount = PT.parseInt(tokens[2]);
          propertyField = PT.parseInt(tokens[3]);
          propertyFieldColumnCount = PT.parseInt(tokens[4]);
        }
      }
      if (atomNumberField < 0)
        atomNumberField = 0;
      if (propertyField < 0)
        propertyField = 0;
      int ac = vwr.ms.ac;
      int[] atomMap = null;
      BS bsTemp = BS.newN(ac);
      if (atomNumberField > 0) {
        atomMap = new int[ac + 2];
        for (int j = 0; j <= ac; j++)
          atomMap[j] = -1;
        for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1)) {
          int atomNo = vwr.ms.at[j].getAtomNumber();
          if (atomNo > ac + 1 || atomNo < 0 || bsTemp.get(atomNo))
            continue;
          bsTemp.set(atomNo);
          atomMap[atomNo] = j;
        }
        d[JmolDataManager.DATA_SELECTION] = atomMap;
      } else {
        d[JmolDataManager.DATA_SELECTION] = BSUtil.copy(bs);
      }
      d[JmolDataManager.DATA_VALUE] = dataString;
      d[JmolDataManager.DATA_TYPE] = Integer
          .valueOf(JmolDataManager.DATA_TYPE_STRING);
      vwr.setData(dataType, d, ac, atomNumberField, atomNumberFieldColumnCount,
          propertyField, propertyFieldColumnCount);
      return;
    }
    if ("occupany".equals(dataType))
      dataType = "occupancy"; // legacy misspelling in states
    int userType = AtomCollection.getUserSettableType(dataType);
    if (userType > JmolDataManager.DATA_TYPE_UNKNOWN) {
      // this is a known settable type or "property_xxxx"
      vwr.setAtomData(userType, dataType, dataString, isDefault);
      return;
    }
    // this is just information to be stored.
    d[JmolDataManager.DATA_LABEL] = dataLabel;
    d[JmolDataManager.DATA_VALUE] = dataString;
    d[JmolDataManager.DATA_TYPE] = Integer
        .valueOf(JmolDataManager.DATA_TYPE_STRING);
    vwr.setData(dataType, d, 0, 0, 0, 0, 0);
  }

  private void ellipsoid() throws ScriptException {
    ScriptEval eval = e;
    int mad = 0;
    int i = 1;
    double translucentLevel = Double.MAX_VALUE;
    boolean checkMore = false;
    boolean isSet = false;
    setShapeProperty(JC.SHAPE_ELLIPSOIDS, "thisID", null);
    // the first three options, ON, OFF, and (int)scalePercent
    // were implemented long before the idea of customized 
    // ellipsoids was considered. "ON" will produce an ellipsoid
    // with a standard radius, and "OFF" will reduce its scale to 0,
    // effectively elliminating it.

    // The new options SET and ID are much more powerful. In those, 
    // ON and OFF simply do that -- turn the ellipsoid on or off --
    // and there are many more options.

    // The SET type ellipsoids, introduced in Jmol 13.1.19 in 7/2013,
    // are created by all readers that read ellipsoid (PDB/CIF) or 
    // tensor (Castep, MagRes) data.

    switch (getToken(1).tok) {
    case T.on:
      mad = Integer.MAX_VALUE; // default for this type
      break;
    case T.off:
      break;
    case T.integer:
      mad = intParameter(1);
      break;
    case T.set:
      e.sm.loadShape(JC.SHAPE_ELLIPSOIDS);
      setShapeProperty(JC.SHAPE_ELLIPSOIDS, "select", paramAsStr(2));
      i = eval.iToken;
      checkMore = true;
      isSet = true;
      break;
    case T.id:
    case T.times:
    case T.identifier:
      e.sm.loadShape(JC.SHAPE_ELLIPSOIDS);
      if (eval.theTok == T.id)
        i++;
      setShapeId(JC.SHAPE_ELLIPSOIDS, i, false);
      i = eval.iToken;
      checkMore = true;
      break;
    default:
      invArg();
    }
    if (!checkMore) {
      eval.setShapeSizeBs(JC.SHAPE_ELLIPSOIDS, mad, null);
      return;
    }
    int[] colorArgb = new int[] { Integer.MIN_VALUE };
    while (++i < slen) {
      String key = paramAsStr(i);
      Object value = null;
      getToken(i);
      if (!isSet)
        switch (eval.theTok) {
        case T.dollarsign:
          key = "points";
          Object[] data = new Object[3];
          data[0] = eval.objectNameParameter(++i);
          if (chk)
            continue;
          eval.getShapePropertyData(JC.SHAPE_ISOSURFACE, "getVertices", data);
          value = data;
          break;
        case T.label:
          value = e.optParameterAsString(++i);
          if (((String) value).length() == 0)
            continue;
          break;
        case T.axes:
          V3d[] axes = new V3d[3];
          Lst<SV> l = null;
          switch (getToken(i + 1).tok) {
          case T.matrix3f:
            i++;
            M3d m = (M3d) eval.theToken.value;
            for (int im = 3; --im >= 0;)
              m.getColumnV(im, axes[im] = new V3d());
            break;
          case T.spacebeforesquare:
            i += 2;
            //$FALL-THROUGH$
          case T.leftsquare:
            l = new Lst<SV>();
            for (int i1 = 3; --i1 >= 0;) {
              switch (tokAt(++i)) {
              case T.leftsquare:
                break;
              default:
                if (eval.isCenterParameter(i)) {
                  l.addLast(SV.newV(T.point3f, centerParameter(i)));
                } else if (tokAt(i) == T.leftsquare) {
                  // TODO
                } else {
                  l.addLast((SV) getToken(i));
                }
              }
              i = eval.iToken;
            }
            if (getToken(++i).tok != T.rightsquare)
              invArg();
            break;
          case T.varray:
            l = ((SV) eval.theToken).getList();
            switch (l.size()) {
            case 1:
              Lst<SV> l1 = new Lst<SV>();
              for (int il = 3; --il >= 0;)
                l1.addLast((SV) getToken(++i));
              l = l1;
              break;
            case 3:
              break;
            default:
              invArg();
              break;
            }
            break;
          default:
            for (int j = 0; j < 3; j++) {
              axes[j] = V3d.newV(centerParameter(++i));
              i = eval.iToken;
            }
            break;
          }
          if (l != null) {
            for (int k = 3; --k >= 0;) {
              SV v = l.get(k);
              switch (v.tok) {
              case T.varray:
                axes[k] = V3d.new3(SV.dValue(v.getList().get(0)),
                    SV.dValue(v.getList().get(1)),
                    SV.dValue(v.getList().get(2)));
                break;
              case T.point3f:
                axes[k] = V3d.newV((T3d) v.value);
                break;
              }
            }
          }
          i = eval.iToken;
          value = axes;
          break;
        case T.center:
          value = centerParameter(++i);
          i = eval.iToken;
          break;
        case T.modelindex:
          value = Integer.valueOf(intParameter(++i));
          break;
        case T.delete:
          value = Boolean.TRUE;
          checkLength(i + 1);
          break;
        }
      // these next are for SET "XXX" or ID "XXX" syntax only
      if (value == null)
        switch (eval.theTok) {
        case T.on:
          key = "on";
          value = Boolean.TRUE;
          break;
        case T.off:
          key = "on";
          value = Boolean.FALSE;
          break;
        case T.scale:
          if (isFloatParameter(i + 1)) {
            value = Double.valueOf(doubleParameter(++i));
          } else if (eval.isCenterParameter(i)) {
            P3d p = centerParameter(i);
            value = new double[] { p.x, p.y, p.z };
          } else {
            value = eval.doubleParameterSet(++i, 3, 3);
          }
          i = eval.iToken;
          break;
        case T.define:
        case T.bitset:
        case T.expressionBegin:
          key = "atoms";
          value = atomExpressionAt(i);
          i = eval.iToken;
          break;
        case T.color:
        case T.translucent:
        case T.opaque:
          translucentLevel = getColorTrans(eval, i, true, colorArgb);
          i = eval.iToken;
          continue;
        case T.options:
          value = paramAsStr(++i);
          break;
        }
      if (value == null)
        invArg();
      setShapeProperty(JC.SHAPE_ELLIPSOIDS, key.toLowerCase(), value);
    }
    finalizeObject(JC.SHAPE_ELLIPSOIDS, colorArgb[0], translucentLevel, 0,
        false, null, 0, null);
    setShapeProperty(JC.SHAPE_ELLIPSOIDS, "thisID", null);
  }

  private void image() throws ScriptException {
    // image ...
    // image id XXXX...
    // ... "filename"
    // ... close
    // ... close "filename"
    if (!chk)
      vwr.getConsole();// especially important for JavaScript
    int pt = 1;
    String id = null;
    if (tokAt(1) == T.id) {
      // image ID ...
      id = e.optParameterAsString(++pt);
      pt++;
    }
    String fileName = e.optParameterAsString(pt);
    boolean isClose = e.optParameterAsString(slen - 1)
        .equalsIgnoreCase("close");
    if (!isClose && (slen == pt || slen == pt + 2)) {
      // image
      // image 400 400
      // image id "testing"
      // image id "testing" 400 400
      int width = (slen == pt + 2 ? intParameter(pt++) : -1);
      int height = (width < 0 ? -1 : intParameter(pt));
      Map<String, Object> params = new Hashtable<String, Object>();
      params.put("fileName", "\1\1" + id);
      params.put("backgroundColor", Integer.valueOf(vwr.getBackgroundArgb()));
      params.put("type", "png");
      params.put("quality", Integer.valueOf(-1));
      params.put("width", Integer.valueOf(width));
      params.put("height", Integer.valueOf(height));
      if (!chk)
        vwr.processWriteOrCapture(params);
      return;
    }
    pt++;
    if (isClose) {
      switch (slen) {
      case 2:
        // image close
        fileName = "closeall";
        break;
      case 3:
      case 4:
        // image "filename" close
        // image ID "testing" close
        break;
      default:
        checkLength(0);
      }
    }
    if (!chk)
      vwr.fm.loadImage(isClose ? "\1close" : fileName,
          "\1" + fileName + "\1" + ("".equals(id) || id == null ? null : id),
          false);
  }

  private void invertSelected() throws ScriptException {
    // invertSelected POINT
    // invertSelected PLANE
    // invertSelected HKL
    // invertSelected STEREO {sp3Atom} {one or two groups)
    // invertSelected ATOM {ring atom sets}
    ScriptEval e = this.e;
    P3d pt = null;
    P4d plane = null;
    BS bs = null;
    int iAtom = Integer.MIN_VALUE;
    int ipt = 1;
    switch (tokAt(1)) {
    case T.nada:
      if (chk)
        return;
      bs = vwr.bsA();
      pt = vwr.ms.getAtomSetCenter(bs);
      vwr.invertAtomCoord(pt, null, bs, -1, false);
      return;
    case T.stereo:
    case T.atoms:
      ipt++;
      //$FALL-THROUGH$
    case T.bitset:
    case T.expressionBegin:
    case T.define:
      bs = atomExpressionAt(ipt);
      if (!e.isAtomExpression(e.iToken + 1)) {
        e.checkLengthErrorPt(e.iToken + 1, e.iToken + 1);
        if (!chk) {
          for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            vwr.invertAtomCoord(null, null, bs, i, false);
          }
        }
        return;
      }
      iAtom = bs.nextSetBit(0);
      bs = atomExpressionAt(e.iToken + 1);
      break;
    case T.point:
      pt = e.centerParameter(2, null);
      break;
    case T.plane:
      plane = e.planeParameter(1, false);
      break;
    case T.hkl:
      plane = e.hklParameter(2, null, true);
      break;
    }
    e.checkLengthErrorPt(e.iToken + 1, 1);
    if (plane == null && pt == null && iAtom == Integer.MIN_VALUE)
      invArg();
    if (chk)
      return;
    if (iAtom == -1)
      return;
    vwr.invertSelected(pt, plane, iAtom, bs);
  }

  private void mapProperty() throws ScriptException {
    // map {1.1}.straightness  {2.1}.property_x resno
    BS bsFrom, bsTo;
    String property1, property2, mapKey;
    int tokProp1 = 0;
    int tokProp2 = 0;
    int tokKey = 0;
    ScriptEval e = this.e;
    while (true) {
      if (tokAt(1) == T.selected) {
        bsFrom = vwr.bsA();
        bsTo = atomExpressionAt(2);
        property1 = property2 = "selected";
      } else {
        bsFrom = atomExpressionAt(1);
        if (tokAt(++e.iToken) != T.per
            || !T.tokAttr(tokProp1 = tokAt(++e.iToken), T.atomproperty))
          break;
        property1 = paramAsStr(e.iToken);
        bsTo = atomExpressionAt(++e.iToken);
        if (tokAt(++e.iToken) != T.per
            || !T.tokAttr(tokProp2 = tokAt(++e.iToken), T.settable))
          break;
        property2 = paramAsStr(e.iToken);
      }
      if (T.tokAttr(tokKey = tokAt(e.iToken + 1), T.atomproperty))
        mapKey = paramAsStr(++e.iToken);
      else
        mapKey = T.nameOf(tokKey = T.atomno);
      e.checkLast(e.iToken);
      if (chk)
        return;
      BS bsOut = null;
      showString(
          "mapping " + property1.toUpperCase() + " for " + bsFrom.cardinality()
              + " atoms to " + property2.toUpperCase() + " for "
              + bsTo.cardinality() + " atoms using " + mapKey.toUpperCase());
      if (T.tokAttrOr(tokProp1, T.intproperty, T.floatproperty)
          && T.tokAttrOr(tokProp2, T.intproperty, T.floatproperty)
          && T.tokAttrOr(tokKey, T.intproperty, T.floatproperty)) {
        double[] data1 = getBitsetPropertyFloat(bsFrom,
            tokProp1 | T.selectedfloat, null, Double.NaN, Double.NaN);
        double[] data2 = getBitsetPropertyFloat(bsFrom,
            tokKey | T.selectedfloat, null, Double.NaN, Double.NaN);
        double[] data3 = getBitsetPropertyFloat(bsTo, tokKey | T.selectedfloat,
            null, Double.NaN, Double.NaN);
        boolean isProperty = (tokProp2 == T.property);
        double[] dataOut = new double[isProperty ? vwr.ms.ac : data3.length];
        bsOut = new BS();
        if (data1.length == data2.length) {
          Map<Double, Double> ht = new Hashtable<Double, Double>();
          for (int i = 0; i < data1.length; i++) {
            ht.put(Double.valueOf(data2[i]), Double.valueOf(data1[i]));
          }
          int pt = -1;
          int nOut = 0;
          for (int i = 0; i < data3.length; i++) {
            pt = bsTo.nextSetBit(pt + 1);
            Double F = ht.get(Double.valueOf(data3[i]));
            if (F == null)
              continue;
            bsOut.set(pt);
            dataOut[(isProperty ? pt : nOut)] = F.doubleValue();
            nOut++;
          }
          // note: this was DATA_TYPE_STRING ?? 
          if (isProperty)
            vwr.setData(property2, new Object[] { property2, dataOut, bsOut,
                Integer.valueOf(JmolDataManager.DATA_TYPE_AD), Boolean.TRUE },
                vwr.ms.ac, 0, 0, Integer.MAX_VALUE, 0);
          else if (!T.tokAttr(tokProp2, T.settable))
            error(ScriptError.ERROR_cannotSet);
          else
            vwr.setAtomProperty(bsOut, tokProp2, 0, 0, null, dataOut, null);
        }
      }
      if (bsOut == null) {
        String format = "{" + mapKey + "=%[" + mapKey + "]}." + property2
            + " = %[" + property1 + "]";
        String[] data = (String[]) getBitsetIdent(bsFrom, format, null, false,
            Integer.MAX_VALUE, false);
        SB sb = new SB();
        for (int i = 0; i < data.length; i++)
          if (data[i].indexOf("null") < 0)
            sb.append(data[i]).appendC('\n');
        if (Logger.debugging)
          Logger.debug(sb.toString());
        BS bsSubset = BSUtil.copy(vwr.slm.bsSubset);
        vwr.slm.setSelectionSubset(bsTo);
        try {
          e.runScript(sb.toString());
        } catch (Exception ex) {
          vwr.slm.setSelectionSubset(bsSubset);
          e.errorStr(-1, "Error: " + ex.getMessage());
        } catch (Error er) {
          vwr.slm.setSelectionSubset(bsSubset);
          e.errorStr(-1, "Error: " + er.toString());
        }
        vwr.slm.setSelectionSubset(bsSubset);
      }
      showString("DONE");
      return;
    }
    invArg();
  }

  private void minimize(boolean isModelkit) throws ScriptException {
    BS bsSelected = null;
    int steps = Integer.MAX_VALUE;
    double crit = 0;
    boolean addHydrogen = false;
    boolean isSilent = false;
    BS bsFixed = null;
    boolean selectedOnly = false;
    boolean groupOnly = false;
    Minimizer minimizer = vwr.getMinimizer(false);
    double range = 0;

    // may be null
    for (int i = (isModelkit ? 2 : 1); i < slen; i++) {
      switch (getToken(i).tok) {
      case T.criterion:
        crit = doubleParameter(++i);
        continue;
      case T.range:
        range = doubleParameter(++i);
        continue;
      case T.energy:
        steps = 0;
        continue;
      case T.cancel:
      case T.stop:
        checkLength(2);
        if (chk || minimizer == null)
          return;
        minimizer.setProperty(paramAsStr(i), null);
        return;
      case T.clear:
        checkLength(2);
        if (chk || minimizer == null)
          return;
        minimizer.setProperty("clear", null);
        return;
      case T.constraint:
        if (i != 1)
          invArg();
        int n = 0;
        double targetValue = 0;
        int[] aList = new int[5];
        if (tokAt(++i) == T.clear) {
          checkLength(3);
        } else {
          while (n < 4 && !isFloatParameter(i)) {
            aList[++n] = atomExpressionAt(i).nextSetBit(0);
            i = e.iToken + 1;
          }
          aList[0] = n;
          if (n == 1)
            invArg();
          targetValue = doubleParameter(e.checkLast(i));
        }
        if (!chk)
          vwr.getMinimizer(true).setProperty("constraint",
              new Object[] { aList, Double.valueOf(targetValue) });
        return;
      case T.fixed:
        if (i != 1)
          invArg();
        if (slen == ++i) {
          if (!chk) {
            BS bs = (BS) vwr.getMinimizer(true).getProperty("fixed", 0);
            if (bs == null) {
              e.showString("no atoms are fixed for minimization");
            } else {
              n = bs.cardinality();
              e.showString(n + (n == 1 ? " atom is" : " atoms are")
                  + " fixed for minimization: " + bs
                  + "\nunfix them with MINIMIZE FIX NONE");

            }
          }
          return;
        }
        bsFixed = atomExpressionAt(i);
        if (bsFixed.nextSetBit(0) < 0)
          bsFixed = null;
        i = e.iToken;
        if (!chk)
          vwr.getMinimizer(true).setProperty("fixed", bsFixed);
        if (i + 1 == slen)
          return;
        continue;
      case T.addhydrogens:
        addHydrogen = true;
        continue;
      case T.only:
      case T.selected:
        selectedOnly = true;
        break;
      case T.group:
        groupOnly = true;
        break;
      case T.silent:
        isSilent = true;
        break;
      case T.step:
        steps = intParameter(++i);
        continue;
      case T.bitset:
      case T.expressionBegin:
        selectedOnly = true;
        //$FALL-THROUGH$
      case T.select:
        if (e.theTok == T.select)
          i++;
        bsSelected = atomExpressionAt(i);
        i = e.iToken;
        if (tokAt(i + 1) == T.only) {
          i++;
          selectedOnly = true;
        }
        continue;
      default:
        invArg();
        break;
      }
    }
    if (isModelkit && (addHydrogen || selectedOnly || groupOnly))
      invArg();
    if (!chk)
      try {
        vwr.minimize(e, steps, crit, bsSelected, bsFixed, null, range,
            (addHydrogen ? Viewer.MIN_ADDH : 0)
                | (isModelkit ? Viewer.MIN_MODELKIT : 0)
                | (selectedOnly ? Viewer.MIN_SELECT_ONLY : 0)
                | (groupOnly ? Viewer.MIN_GROUP_SELECT : 0)
                | (isSilent ? Viewer.MIN_SILENT : 0));
      } catch (Exception e1) {
        // actually an async exception
        throw new ScriptInterruption(e, "minimize", 1);
      }
  }

  /**
   * Allows for setting one or more specific t-values as well as full unit-cell
   * shifts (multiples of q).
   * 
   * @throws ScriptException
   */
  private void modulation() throws ScriptException {

    // modulation on/off  (all atoms)
    // modulation vectors on/off
    // modulation {atom set} on/off
    // modulation int  q-offset
    // modulation x.x  t-offset
    // modulation {t1 t2 t3} 
    // modulation {q1 q2 q3} TRUE 
    P3d qtOffset = null;
    //    int frameN = Integer.MAX_VALUE;
    ScriptEval eval = e;
    boolean mod = true;
    boolean isQ = false;
    BS bs = null;
    int i = 1;
    switch (getToken(i).tok) {
    case T.off:
      mod = false;
      //$FALL-THROUGH$
    case T.nada:
    case T.on:
      break;
    case T.define:
    case T.bitset:
    case T.expressionBegin:
      bs = atomExpressionAt(1);
      switch (tokAt(eval.iToken + 1)) {
      case T.nada:
        break;
      case T.off:
        mod = false;
        //$FALL-THROUGH$
      case T.on:
        eval.iToken++;
        break;
      }
      eval.checkLast(eval.iToken);
      break;
    case T.leftbrace:
    case T.point3f:
      qtOffset = eval.getPoint3f(1, false, true);
      isQ = (tokAt(eval.iToken + 1) == T.on);
      break;
    default:
      String s = eval.theToken.value.toString();
      i++;
      if (s.equalsIgnoreCase("t")) {
        eval.theTok = T.decimal;
      } else if (s.equalsIgnoreCase("m") || s.equalsIgnoreCase("q")) {
        eval.theTok = T.integer;
      } else {
        invArg();
      }
      //$FALL-THROUGH$
    case T.decimal:
    case T.integer:
      // allows for form of number -- integer or double -- to determine type,
      // but allso allows using "t" or "q" followed by a number or {t1 t2 t3}
      switch (eval.theTok) {
      case T.decimal:
        if (isFloatParameter(i)) {
          double t1 = doubleParameter(i);
          qtOffset = P3d.new3(t1, t1, t1);
        } else {
          qtOffset = eval.getPoint3f(i, false, true);
        }
        break;
      case T.integer:
        if (tokAt(i) == T.integer) {
          int t = intParameter(i);
          qtOffset = P3d.new3(t, t, t);
        } else {
          qtOffset = eval.getPoint3f(i, false, true);
        }
        isQ = true;
        break;
      }
      break;
    case T.scale:
      double scale = doubleParameter(2);
      if (!chk)
        vwr.setFloatProperty("modulationScale", scale);
      return;
    }
    if (!chk) {
      vwr.tm.setVibrationPeriod(0);
      vwr.setModulation(bs, mod, qtOffset, isQ);
    }
  }

  private void mutate() throws ScriptException {
    // [modelkit] mutate {resno} "LYS" or "file identifier"
    // [modelkit] mutate {1-3} ~GGGL
    // [modelkit] mutate CREATE ~GGGL "alpha"
    // [modelkit] mutate CREATE ~GGGL [phi psi phi psi...]
    // [modelkit] mutate CREATE "ala-phe-lys" ....

    int i = e.iToken;
    BS bs;
    String alphaType = null;
    String sequence = null;
    double[] phipsi = null;
    boolean isCreate = false;
    switch (tokAt(++i)) {
    case T.create:
      alphaType = "alpha";
      isCreate = true;
      bs = null;
      switch (tokAt(++i)) {
      case T.nada:
        invArg();
        break;
      default:
        sequence = e.paramAsStr(i);
        i = e.iToken;
        switch (tokAt(i + 1)) {
        case T.nada:
          break;
        case T.leftsquare:
        case T.spacebeforesquare:
        case T.varray:
          alphaType = null;
          phipsi = e.doubleParameterSet(++i, 2, Integer.MAX_VALUE);
          if (phipsi.length % 2 == 1)
            invArg();
          i = e.iToken;
          break;
        default:
          alphaType = e.paramAsStr(++i);
          i = e.iToken;
          break;
        }
        break;
      }
      break;
    case T.integer:
      st[1] = T.o(T.string, "" + st[1].value); // allows @x and "*"
      //$FALL-THROUGH$
    default:
      bs = atomExpressionAt(i);
      i = ++e.iToken;
      break;
    case T.times:
      bs = vwr.getFrameAtoms();
      i = ++e.iToken;
      break;
    }
    if (!chk && bs != null) {
      bs.and(vwr.getFrameAtoms());
      int iatom = bs.nextSetBit(0);
      if (iatom < 0)
        invArg();
      int imodel = vwr.ms.at[iatom].mi;
      if (vwr.ms.isTrajectory(imodel) || !vwr.ms.am[imodel].isBioModel)
        return;
    }
    if (sequence == null)
      sequence = e.optParameterAsString(i);
    e.checkLast(i);
    if (chk)
      return;
    // ~ introduces single-letter sequences
    boolean isOneLetter = sequence.startsWith("~");
    boolean isFile = (!isOneLetter && !isCreate
        && (sequence.indexOf(".") >= 0 || sequence.indexOf("-") < 0
            && sequence.length() > 3 && sequence.startsWith("==")));
    String[] list;
    if (isFile) {
      list = new String[] { sequence };
      sequence = null;
    } else {
      sequence = PT.replaceAllCharacters(sequence, ",; \t\n", " ").trim()
          .toUpperCase();
      if (!isOneLetter && sequence.indexOf("-") >= 0) {
        list = PT.split(sequence, "-");
      } else {
        if (isOneLetter || sequence.length() != 3
            || !vwr.getJBR().isKnownPDBGroup(sequence, 20))
          sequence = vwr.getJBR()
              .toStdAmino3(isOneLetter ? sequence.substring(1) : sequence);
        list = PT.getTokens(sequence);
      }
    }
    e.iToken = e.slen;
    if (list.length > 0 && !vwr.getJBR().getBioModelSet(vwr.ms).mutate(bs,
        sequence, list, alphaType, phipsi))
      invArg();
  }

  private void navigate() throws ScriptException {
    /*
     * navigation on/off navigation depth p # would be as a depth value, like
     * slab, in percent, but could be negative navigation nSec translate X Y #
     * could be percentages navigation nSec translate $object # could be a draw
     * object navigation nSec translate (atom selection) #average of values
     * navigation nSec center {x y z} navigation nSec center $object navigation
     * nSec center (atom selection) navigation nSec path $object navigation nSec
     * path {x y z theta} {x y z theta}{x y z theta}{x y z theta}... navigation
     * nSec trace (atom selection)
     */
    ScriptEval eval = e;
    if (slen == 1) {
      eval.setBooleanProperty("navigationMode", true);
      return;
    }
    V3d rotAxis = V3d.new3(0, 1, 0);
    Lst<Object[]> list = new Lst<Object[]>();
    P3d pt;
    if (slen == 2) {
      switch (getToken(1).tok) {
      case T.on:
      case T.off:
        if (chk)
          return;
        eval.setObjectMad10(JC.SHAPE_AXES, "axes", 10);
        setShapeProperty(JC.SHAPE_AXES, "position",
            P3d.new3(50, 50, Double.MAX_VALUE));
        eval.setBooleanProperty("navigationMode", true);
        vwr.tm.setNavOn(eval.theTok == T.on);
        return;
      case T.stop:
        if (!chk)
          vwr.tm.setNavXYZ(0, 0, 0);
        return;
      case T.point3f:
      case T.trace:
        break;
      default:
        invArg();
      }
    }
    if (!chk && !vwr.getBoolean(T.navigationmode))
      eval.setBooleanProperty("navigationMode", true);
    for (int i = 1; i < slen; i++) {
      double timeSec = (isFloatParameter(i) ? doubleParameter(i++) : 2d);
      if (timeSec < 0)
        invArg();
      if (!chk && timeSec > 0)
        eval.refresh(false);
      switch (getToken(i).tok) {
      case T.point3f:
      case T.leftbrace:
        // navigate {x y z}
        pt = getPoint3f(i, true);
        eval.iToken++;
        if (eval.iToken != slen)
          invArg();
        if (!chk)
          vwr.tm.setNavXYZ(pt.x, pt.y, pt.z);
        return;
      case T.depth:
        double depth = doubleParameter(++i);
        if (!chk)
          list.addLast(new Object[] { Integer.valueOf(T.depth),
              Double.valueOf(timeSec), Double.valueOf(depth) });
        //vwr.setNavigationDepthPercent(timeSec, depth);
        continue;
      case T.center:
        pt = centerParameter(++i);
        i = eval.iToken;
        if (!chk)
          list.addLast(new Object[] { Integer.valueOf(T.point),
              Double.valueOf(timeSec), pt });
        //vwr.navigatePt(timeSec, pt);
        continue;
      case T.rotate:
        switch (getToken(++i).tok) {
        case T.x:
          rotAxis.set(1, 0, 0);
          i++;
          break;
        case T.y:
          rotAxis.set(0, 1, 0);
          i++;
          break;
        case T.z:
          rotAxis.set(0, 0, 1);
          i++;
          break;
        case T.point3f:
        case T.leftbrace:
          rotAxis.setT(getPoint3f(i, true));
          i = eval.iToken + 1;
          break;
        case T.identifier:
          invArg(); // for now
          break;
        }
        double degrees = doubleParameter(i);
        if (!chk)
          list.addLast(new Object[] { Integer.valueOf(T.rotate),
              Double.valueOf(timeSec), rotAxis, Double.valueOf(degrees) });
        //          vwr.navigateAxis(timeSec, rotAxis, degrees);
        continue;
      case T.translate:
        double x = Double.NaN;
        double y = Double.NaN;
        if (isFloatParameter(++i)) {
          x = doubleParameter(i);
          y = doubleParameter(++i);
        } else {
          switch (tokAt(i)) {
          case T.x:
            x = doubleParameter(++i);
            break;
          case T.y:
            y = doubleParameter(++i);
            break;
          default:
            pt = centerParameter(i);
            i = eval.iToken;
            if (!chk)
              list.addLast(new Object[] { Integer.valueOf(T.translate),
                  Double.valueOf(timeSec), pt });
            //vwr.navTranslate(timeSec, pt);
            continue;
          }
        }
        if (!chk)
          list.addLast(new Object[] { Integer.valueOf(T.percent),
              Double.valueOf(timeSec), Double.valueOf(x), Double.valueOf(y) });
        //vwr.navTranslatePercent(timeSec, x, y);
        continue;
      case T.divide:
        continue;
      case T.trace:
        P3d[][] pathGuide;
        Lst<P3d[]> vp = new Lst<P3d[]>();
        BS bs;
        if (eval.isAtomExpression(i + 1)) {
          bs = atomExpressionAt(++i);
          i = eval.iToken;
        } else {
          bs = vwr.bsA();
        }
        if (chk)
          return;
        vwr.getPolymerPointsAndVectors(bs, vp);
        int n;
        if ((n = vp.size()) > 0) {
          pathGuide = new P3d[n][];
          for (int j = 0; j < n; j++) {
            pathGuide[j] = vp.get(j);
          }
          list.addLast(new Object[] { Integer.valueOf(T.trace),
              Double.valueOf(timeSec), pathGuide });
          //vwr.navigateGuide(timeSec, pathGuide);
          continue;
        }
        break;
      case T.path:
        P3d[] path;
        double[] theta = null; // orientation; null for now
        if (getToken(i + 1).tok == T.dollarsign) {
          i++;
          // navigate timeSeconds path $id indexStart indexEnd
          String pathID = eval.objectNameParameter(++i);
          if (chk)
            return;
          setShapeProperty(JC.SHAPE_DRAW, "thisID", pathID);
          path = (P3d[]) getShapeProperty(JC.SHAPE_DRAW, "vertices");
          eval.refresh(false);
          if (path == null)
            invArg();
          int indexStart = (int) (isFloatParameter(i + 1) ? doubleParameter(++i)
              : 0);
          int indexEnd = (int) (isFloatParameter(i + 1) ? doubleParameter(++i)
              : Integer.MAX_VALUE);
          list.addLast(
              new Object[] { Integer.valueOf(T.path), Double.valueOf(timeSec),
                  path, theta, new int[] { indexStart, indexEnd } });
          //vwr.navigatePath(timeSec, path, theta, indexStart, indexEnd);
          continue;
        }
        Lst<P3d> v = new Lst<P3d>();
        while (eval.isCenterParameter(i + 1)) {
          v.addLast(centerParameter(++i));
          i = eval.iToken;
        }
        if (v.size() > 0) {
          path = v.toArray(new P3d[v.size()]);
          if (!chk)
            list.addLast(
                new Object[] { Integer.valueOf(T.path), Double.valueOf(timeSec),
                    path, theta, new int[] { 0, Integer.MAX_VALUE } });
          //vwr.navigatePath(timeSec, path, theta, 0, Integer.MAX_VALUE);
          continue;
        }
        //$FALL-THROUGH$
      default:
        invArg();
      }
    }
    if (!chk && !vwr.isJmolDataFrame() && !vwr.getBoolean(T.mode2d))
      vwr.tm.navigateList(eval, list);
  }

  private String plot(T[] args) throws ScriptException {
    ScriptEval e = this.e;
    // also used for draw [quaternion, helix, ramachandran] 
    // and write quaternion, ramachandran, plot, ....
    // and plot property propertyX, propertyY, propertyZ //
    int modelIndex = vwr.am.cmi;
    if (modelIndex < 0)
      e.errorStr(ScriptError.ERROR_multipleModelsDisplayedNotOK, "plot");
    modelIndex = vwr.ms.getJmolDataSourceFrame(modelIndex);
    int pt = args.length - 1;
    boolean isReturnOnly = (args != st);
    boolean pdbFormat = true;
    T[] statementSave = st;
    if (isReturnOnly)
      e.st = st = args;
    int tokCmd = (isReturnOnly ? T.show : args[0].tok);
    int pt0 = (isReturnOnly || tokCmd == T.quaternion
        || tokCmd == T.ramachandran ? 0 : 1);
    String filename = null;
    boolean makeNewFrame = true;
    boolean isDraw = false;
    switch (tokCmd) {
    case T.plot:
    case T.quaternion:
    case T.ramachandran:
      break;
    case T.draw:
      makeNewFrame = false;
      isDraw = true;
      break;
    case T.show:
      makeNewFrame = false;
      pdbFormat = false;
      break;
    case T.write:
      makeNewFrame = false;
      if (tokAtArray(pt, args) == T.string) {
        filename = stringParameter(pt--);
      } else if (tokAtArray(pt - 1, args) == T.per) {
        filename = paramAsStr(pt - 2) + "." + paramAsStr(pt);
        pt -= 3;
      } else {
        e.st = st = statementSave;
        e.iToken = st.length;
        error(ScriptError.ERROR_endOfStatementUnexpected);
      }
      e.slen = slen = pt + 1;
      break;
    }
    String qFrame = "";
    Object[] parameters = null;
    String stateScript = "";
    boolean isQuaternion = false;
    boolean isDerivative = false;
    boolean isSecondDerivative = false;
    boolean isRamachandranRelative = false;
    String[] props = new String[3];
    int[] propToks = new int[3];

    BS bs = BSUtil.copy(vwr.bsA());
    String preSelected = "; select " + Escape.eBS(bs) + ";\n ";
    String type = e.optParameterAsString(pt).toLowerCase();
    P3d minXYZ = null;
    P3d maxXYZ = null;
    String format = null;
    int tok = tokAtArray(pt0, args);
    if (tok == T.string)
      tok = T.getTokFromName((String) args[pt0].value);
    switch (tok) {
    default:
      e.iToken = 1;
      invArg();
      break;
    case T.data:
      e.iToken = 1;
      type = "data";
      preSelected = "";
      break;
    case T.property:
      e.iToken = pt0 + 1;
      for (int i = 0; i < 3; i++) {
        switch (tokAt(e.iToken)) {
        case T.string:
          propToks[i] = T.getTokFromName((String) e.getToken(e.iToken).value);
          break;
        default:
          propToks[i] = tokAt(e.iToken);
          break;
        case T.nada:
          if (i == 0)
            invArg();
          //$FALL-THROUGH$
        case T.format:
        case T.min:
        case T.max:
          i = 2;
          continue;
        }
        if (propToks[i] != T.property
            && !T.tokAttr(propToks[i], T.atomproperty))
          invArg();
        props[i] = getToken(e.iToken).value.toString();
        e.iToken++;
      }
      if (tokAt(e.iToken) == T.format) {
        format = stringParameter(++e.iToken);
        pdbFormat = false;
        e.iToken++;
      }
      if (tokAt(e.iToken) == T.min) {
        minXYZ = getPoint3f(++e.iToken, false);
        e.iToken++;
      }
      if (tokAt(e.iToken) == T.max) {
        maxXYZ = getPoint3f(++e.iToken, false);
        e.iToken++;
      }
      type = "property " + props[0] + (props[1] == null ? "" : " " + props[1])
          + (props[2] == null ? "" : " " + props[2]);
      if (bs.nextSetBit(0) < 0)
        bs = vwr.getModelUndeletedAtomsBitSet(modelIndex);
      stateScript = "select " + Escape.eBS(bs) + ";\n ";
      break;
    case T.ramachandran:
      if (type.equalsIgnoreCase("draw")) {
        isDraw = true;
        type = e.optParameterAsString(--pt).toLowerCase();
      }
      isRamachandranRelative = (pt > pt0 && type.startsWith("r"));
      type = "ramachandran" + (isRamachandranRelative ? " r" : "")
          + (tokCmd == T.draw ? " draw" : "");
      break;
    case T.quaternion:
    case T.helix:
      qFrame = " \"" + vwr.getQuaternionFrame() + "\"";
      stateScript = "set quaternionFrame" + qFrame + ";\n  ";
      isQuaternion = true;
      // working backward this time:
      if (type.equalsIgnoreCase("draw")) {
        isDraw = true;
        type = e.optParameterAsString(--pt).toLowerCase();
      }
      isDerivative = (type.startsWith("deriv") || type.startsWith("diff"));
      isSecondDerivative = (isDerivative && type.indexOf("2") > 0);
      if (isDerivative)
        pt--;
      if (type.equalsIgnoreCase("helix") || type.equalsIgnoreCase("axis")) {
        isDraw = true;
        isDerivative = true;
        pt = -1;
      }
      type = ((pt <= pt0 ? "" : e.optParameterAsString(pt)) + "w").substring(0,
          1);
      if (type.equals("a") || type.equals("r"))
        isDerivative = true;
      if (!PT.isOneOf(type, ";w;x;y;z;r;a;")) // a absolute; r relative
        e.evalError("QUATERNION [w,x,y,z,a,r] [difference][2]", null);
      type = "quaternion " + type + (isDerivative ? " difference" : "")
          + (isSecondDerivative ? "2" : "") + (isDraw ? " draw" : "");
      break;
    }
    st = statementSave;
    if (chk) // just in case we later add parameter options to this
      return "";

    // if not just drawing check to see if there is already a plot of this type

    if (makeNewFrame) {
      stateScript += "plot " + type;
      int ptDataFrame = vwr.ms.getJmolDataFrameIndex(modelIndex, stateScript);
      if (ptDataFrame > 0 && tokCmd != T.write && tokCmd != T.show) {
        // no -- this is that way we switch frames. vwr.deleteAtoms(vwr.getModelUndeletedAtomsBitSet(ptDataFrame), true);
        // data frame can't be 0.
        vwr.setCurrentModelIndexClear(ptDataFrame, true);
        // BitSet bs2 = vwr.getModelAtomBitSet(ptDataFrame);
        // bs2.and(bs);
        // need to be able to set data directly as well.
        // vwr.display(BitSetUtil.setAll(vwr.getAtomCount()), bs2, tQuiet);
        return "";
      }
    }

    // prepare data for property plotting

    double[] dataX = null, dataY = null, dataZ = null;
    String[] propData = new String[3];
    if (tok == T.property) {
      dataX = getBitsetPropertyFloat(bs, propToks[0] | T.selectedfloat,
          propToks[0] == T.property ? props[0] : null,
          (minXYZ == null ? Double.NaN : minXYZ.x),
          (maxXYZ == null ? Double.NaN : maxXYZ.x));
      propData[0] = props[0] + " " + Escape.eAD(dataX);
      if (props[1] != null) {
        dataY = getBitsetPropertyFloat(bs, propToks[1] | T.selectedfloat,
            propToks[1] == T.property ? props[1] : null,
            (minXYZ == null ? Double.NaN : minXYZ.y),
            (maxXYZ == null ? Double.NaN : maxXYZ.y));
        propData[1] = props[1] + " " + Escape.eAD(dataY);
      }
      if (props[2] != null) {
        dataZ = getBitsetPropertyFloat(bs, propToks[2] | T.selectedfloat,
            propToks[2] == T.property ? props[2] : null,
            (minXYZ == null ? Double.NaN : minXYZ.z),
            (maxXYZ == null ? Double.NaN : maxXYZ.z));
        propData[2] = props[2] + " " + Escape.eAD(dataZ);
      }
      if (minXYZ == null)
        minXYZ = P3d.new3(getPlotMinMax(dataX, false, propToks[0]),
            getPlotMinMax(dataY, false, propToks[1]),
            getPlotMinMax(dataZ, false, propToks[2]));
      if (maxXYZ == null)
        maxXYZ = P3d.new3(getPlotMinMax(dataX, true, propToks[0]),
            getPlotMinMax(dataY, true, propToks[1]),
            getPlotMinMax(dataZ, true, propToks[2]));
      Logger.info("plot min/max: " + minXYZ + " " + maxXYZ);
      P3d center = null;
      P3d factors = null;

      if (pdbFormat) {
        factors = P3d.new3(1, 1, 1);
        center = new P3d();
        center.ave(maxXYZ, minXYZ);
        factors.sub2(maxXYZ, minXYZ);
        factors.set(factors.x / 200, factors.y / 200, factors.z / 200);
        if (T.tokAttr(propToks[0], T.intproperty)) {
          factors.x = 1;
          center.x = 0;
        } else if (factors.x > 0.1 && factors.x <= 10) {
          factors.x = 1;
        }
        if (T.tokAttr(propToks[1], T.intproperty)) {
          factors.y = 1;
          center.y = 0;
        } else if (factors.y > 0.1 && factors.y <= 10) {
          factors.y = 1;
        }
        if (T.tokAttr(propToks[2], T.intproperty)) {
          factors.z = 1;
          center.z = 0;
        } else if (factors.z > 0.1 && factors.z <= 10) {
          factors.z = 1;
        }
        if (props[2] == null || props[1] == null)
          center.z = minXYZ.z = maxXYZ.z = factors.z = 0;
        for (int i = 0; i < dataX.length; i++)
          dataX[i] = (dataX[i] - center.x) / factors.x;
        if (props[1] != null)
          for (int i = 0; i < dataY.length; i++)
            dataY[i] = (dataY[i] - center.y) / factors.y;
        if (props[2] != null)
          for (int i = 0; i < dataZ.length; i++)
            dataZ[i] = (dataZ[i] - center.z) / factors.z;
      }
      parameters = new Object[] { bs, dataX, dataY, dataZ, minXYZ, maxXYZ,
          factors, center, format, propData };
    }

    // all set...

    if (tokCmd == T.write)
      return vwr.writeFileData(filename, "PLOT_" + type, modelIndex,
          parameters);

    String data = (type.equals("data") ? "1 0 H 0 0 0 # Jmol PDB-encoded data"
        : vwr.getPdbData(modelIndex, type, null, parameters, null, true));

    if (tokCmd == T.show)
      return data;

    if (Logger.debugging)
      Logger.debug(data);

    if (tokCmd == T.draw) {
      e.runScript(data);
      return "";
    }

    // create the new model

    String[] savedFileInfo = vwr.fm.getFileInfo();
    boolean oldAppendNew = vwr.getBoolean(T.appendnew);
    vwr.g.appendNew = true;
    boolean isOK = (data != null
        && vwr.openStringInlineParamsAppend(data, null, true) == null);
    vwr.g.appendNew = oldAppendNew;
    vwr.fm.setFileInfo(savedFileInfo);
    if (!isOK)
      return "";
    int modelCount = vwr.ms.mc;
    vwr.ms.setJmolDataFrame(stateScript, modelIndex, modelCount - 1);
    if (tok != T.property)
      stateScript += ";\n" + preSelected;
    StateScript ss = vwr.addStateScript(stateScript, true, false);

    // get post-processing script

    double radius = 150;
    String script;
    switch (tok) {
    default:
      script = "frame 0.0; frame last; reset;select visible;wireframe only;";
      radius = 10;
      break;
    case T.property:
      vwr.setFrameTitle(modelCount - 1,
          type + " plot for model " + vwr.getModelNumberDotted(modelIndex));
      script = "frame 0.0; frame last; reset;" + "select visible; spacefill 3.0"
          + "; wireframe 0;" + "draw plotAxisX" + modelCount
          + " {100 -100 -100} {-100 -100 -100} \"" + props[0] + "\";"
          + "draw plotAxisY" + modelCount
          + " {-100 100 -100} {-100 -100 -100} \"" + props[1] + "\";";
      if (props[2] != null)
        script += "draw plotAxisZ" + modelCount
            + " {-100 -100 100} {-100 -100 -100} \"" + props[2] + "\";";
      break;
    case T.ramachandran:
      vwr.setFrameTitle(modelCount - 1, "ramachandran plot for model "
          + vwr.getModelNumberDotted(modelIndex));
      script = "frame 0.0; frame last; reset;"
          + "select visible; color structure; spacefill 3.0; wireframe 0;"
          + "draw ramaAxisX" + modelCount + " {100 0 0} {-100 0 0} \"phi\";"
          + "draw ramaAxisY" + modelCount + " {0 100 0} {0 -100 0} \"psi\";";
      break;
    case T.quaternion:
    case T.helix:
      vwr.setFrameTitle(modelCount - 1, type.replace('w', ' ') + qFrame
          + " for model " + vwr.getModelNumberDotted(modelIndex));
      String color = (C.getHexCode(vwr.cm.colixBackgroundContrast));
      script = "frame 0.0; frame last; reset;"
          + "select visible; wireframe 0; spacefill 3.0; "
          + "isosurface quatSphere" + modelCount + " color " + color
          + " sphere 100.0 mesh nofill frontonly translucent 0.8;"
          + "draw quatAxis" + modelCount
          + "X {100 0 0} {-100 0 0} color red \"x\";" + "draw quatAxis"
          + modelCount + "Y {0 100 0} {0 -100 0} color green \"y\";"
          + "draw quatAxis" + modelCount
          + "Z {0 0 100} {0 0 -100} color blue \"z\";" + "color structure;"
          + "draw quatCenter" + modelCount + "{0 0 0} scale 0.02;";
      break;
    }

    // run the post-processing script and set rotation radius and display frame title
    e.runScript(script + preSelected);
    ss.setModelIndex(vwr.am.cmi);
    vwr.setRotationRadius(radius, true);
    e.sm.loadShape(JC.SHAPE_ECHO);
    showString("frame " + vwr.getModelNumberDotted(modelCount - 1)
        + (type.length() > 0
            ? " created: " + type + (isQuaternion ? qFrame : "")
            : ""));
    return "";
  }

  private void polyhedra() throws ScriptException {
    ScriptEval eval = e;
    // polyhedra
    // polyhedra on/off/delete
    // polyhedra [type]
    // where [type] is one of
    //   n [opt. BONDS]
    //   n-m [opt. BONDS]
    //   [opt. RADIUS] x.y 
    // polyhedra [type] [

    /*
     * needsGenerating:
     * 
     * polyhedra [number of vertices and/or basis] [at most two selection sets]
     * [optional type and/or edge] [optional design parameters]
     * 
     * OR else:
     * 
     * polyhedra [at most one selection set] [type-and/or-edge or on/off/delete]
     */
    boolean haveBonds = (slen == 1);
    boolean haveCenter = false;
    boolean needsGenerating = haveBonds;
    boolean onOffDelete = false;
    boolean typeSeen = false;
    boolean edgeParameterSeen = false;
    double scale = Double.NaN;
    //    int lighting = T.nada; // never implemented; fullyLit does nothing
    int nAtomSets = 0;
    eval.sm.loadShape(JC.SHAPE_POLYHEDRA);
    setShapeProperty(JC.SHAPE_POLYHEDRA, "init", Boolean.TRUE);
    double translucentLevel = Double.MAX_VALUE;
    double radius = -1;
    int[] colorArgb = new int[] { Integer.MIN_VALUE };
    int noToParam = -1;
    int atomPt = -1;
    P3d offset = null;
    double foffset = 0;
    String id = null;
    boolean ok = false;
    boolean modifyOnly = false;
    int[][] faces = null;
    P3d[] points = null;
    if (slen == 2 && tokAt(1) == T.list) {
      if (!chk)
        eval.showStringPrint(
            (String) getShapeProperty(JC.SHAPE_POLYHEDRA, "list"), false);
      return;
    }
    for (int i = 1; i < slen; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      switch (getToken(i).tok) {
      case T.wigner: // Wigner-Seitz; like Brillouin, but not reciprocal lattice, and no inclusion of scale
        scale = Double.NaN;
        //$FALL-THROUGH$
      case T.brillouin:
        int index = (e.theTok == T.wigner ? -1
            : (tokAt(i + 1) == T.integer ? intParameter(++i) : 1));
        if (index > 8)
          invArg();
        if (!chk)
          ((BZone) Interface.getInterface("org.jmol.util.BZone", vwr, "script"))
              .createBZ(vwr, index, null, false, id, scale, foffset, offset);
        return;
      case T.hash:
        propertyName = "info";
        propertyValue = e.theToken.value;
        needsGenerating = true;
        break;
      case T.point:
        propertyName = "points";
        propertyValue = Double
            .valueOf(tokAt(++i) == T.off ? 0 : e.doubleParameter(i));
        ok = true;
        break;
      case T.scale:
        scale = doubleParameter(++i);
        ok = true;
        continue;
      case T.unitcell:
        if (id != null)
          invArg();
        propertyName = "unitCell";
        propertyValue = Boolean.TRUE;
        needsGenerating = true;
        break;
      case T.only:
        e.restrictSelected(false, false);
        eval.theTok = T.on;
        //$FALL-THROUGH$
      case T.on:
      case T.delete:
      case T.off:
        if (i + 1 != slen || needsGenerating || nAtomSets > 1)
          error(ScriptError.ERROR_incompatibleArguments);
        propertyName = (eval.theTok == T.off ? "off"
            : eval.theTok == T.on ? "on" : "delete");
        onOffDelete = true;
        break;
      case T.varray:
        if (id == null || needsGenerating)
          invArg();
        needsGenerating = true;
        faces = getIntArray2(i);
        points = getAllPoints(eval.iToken + 1, 3);
        i = eval.iToken;
        if (points[0] instanceof Atom)
          setShapeProperty(JC.SHAPE_POLYHEDRA, "model",
              Integer.valueOf(((Atom) points[0]).getModelIndex()));
        propertyName = "definedFaces";
        propertyValue = new Object[] { faces, points };
        break;
      case T.full:
        propertyName = "full";
        break;
      case T.integer:
        if (id != null)
          invArg();
        propertyName = "nVertices";
        propertyValue = Integer.valueOf(intParameter(i));
        needsGenerating = true;
        if (tokAt(i + 1) == T.comma)
          i++;
        break;
      case T.bonds:
        if (id != null)
          invArg();
        if (nAtomSets > 0)
          invPO();
        needsGenerating = true;
        propertyName = "bonds";
        haveBonds = true;
        break;
      case T.auto:
        if (radius != -1)
          invArg();
        radius = 0;
        i--;
        //$FALL-THROUGH$
      case T.radius:
        i++;
        //$FALL-THROUGH$
      case T.decimal:
        if (id != null)
          invArg();
        if (nAtomSets > 0)
          invPO();
        propertyName = (radius <= 0 ? "radius" : "radius1");
        propertyValue = Double
            .valueOf(radius = (radius == 0 ? 0 : doubleParameter(i)));
        needsGenerating = true;
        break;
      case T.offset:
        if (isFloatParameter(i + 1)) {
          foffset = floatParameter(++i);
        } else {
          offset = e.centerParameter(++i, null);
          i = eval.iToken;
        }
        ok = true;
        continue;
      case T.facecenteroffset:
        setShapeProperty(JC.SHAPE_POLYHEDRA, "collapsed", null);
        //$FALL-THROUGH$
      case T.planarparam:
      case T.distancefactor:
        propertyName = T.nameOf(eval.theTok);
        switch (tokAt(i + 1)) {
        // wish I had not done this. Inconsistent with general syntax; only used here
        case T.opEQ:
        case T.comma:
          i++;
          break;
        }
        propertyValue = Double.valueOf(doubleParameter(++i));
        break;
      case T.model:
        if (id == null)
          invArg();
        propertyName = "model";
        propertyValue = Integer.valueOf(intParameter(++i));
        break;
      case T.to:
        if (nAtomSets > 1 || id != null && !haveCenter || noToParam == i)
          invPO();
        nAtomSets = 3; // don't allow two of these
        if (eval.isAtomExpression(++i)) {
          // select... polyhedron .... to ....
          propertyName = (needsGenerating || haveCenter ? "to" : "toBitSet");
          propertyValue = atomExpressionAt(i);
        } else if (eval.isArrayParameter(i)) {
          // select... polyhedron .... to [...]
          // polyhedron {...} to [...]
          propertyName = "toVertices";
          propertyValue = eval.getPointArray(i, -1, false, true);
        } else {
          error(ScriptError.ERROR_insufficientArguments);
        }
        i = eval.iToken;
        needsGenerating = true;
        break;
      case T.define:
      case T.bitset:
      case T.expressionBegin:
        if (typeSeen)
          invPO();
        switch (++nAtomSets) {
        case 1:
          if (id != null)
            invArg();
          propertyName = "centers";
          break;
        case 2:
          propertyName = "to";
          needsGenerating = true;
          break;
        default:
          eval.bad();
        }
        propertyValue = atomExpressionAt(i);
        atomPt = i = eval.iToken;
        needsGenerating |= (i + 1 == slen);
        break;
      case T.flat: // removed in Jmol 14.4 -- never documented; restored in Jmol 14.29.21
      case T.collapsed:
        // assume regenerating - but not actually recommended Jmol 16.2.13/14 
          needsGenerating = true; 
          modifyOnly = (i == 1);
        // COLLAPSED
        // COLLAPSED [faceCenterOffset]
        if (typeSeen)
          error(ScriptError.ERROR_incompatibleArguments);
        typeSeen = true;
        propertyName = (e.theTok == T.collapsed ? "collapsed" : null);
        if (isFloatParameter(i + 1))
          setShapeProperty(JC.SHAPE_POLYHEDRA, "faceCenterOffset",
              Double.valueOf(doubleParameter(++i)));
        break;
      case T.color:
      case T.translucent:
      case T.opaque:
        if (atomPt == i - 1)
          needsGenerating = true;
        translucentLevel = getColorTrans(eval, i, true, colorArgb);
        ok = true;
        i = eval.iToken;
        continue;
      case T.noedges:
      case T.edges:
      case T.frontedges:
      case T.edgesonly:
        if (atomPt == i - 1)
          needsGenerating = true;
        if (edgeParameterSeen)
          error(ScriptError.ERROR_incompatibleArguments);
        edgeParameterSeen = true;
        ok = true;
        propertyName = T.nameOf(eval.theTok);
        break;
      case T.triangles:
      case T.notriangles:
      case T.backlit:
      case T.frontlit:
      case T.fullylit:
        // never implemented
        //        lighting = eval.theTok;
        continue;
      case T.id:
      case T.times:
      case T.identifier:
      case T.string:
        if (!eval.isColorParam(i)) {
          if (i != 1)
            invPO();
          id = (eval.theTok == T.id ? stringParameter(++i)
              : eval.optParameterAsString(i));
          if (tokAt(i + 1) == T.times)
            id += paramAsStr(++i);
          setShapeProperty(JC.SHAPE_POLYHEDRA, "thisID", id);
          setShapeProperty(JC.SHAPE_POLYHEDRA, "model",
              Integer.valueOf(vwr.am.cmi));
          if (!eval.isCenterParameter(i + 1))
            continue;
          propertyName = "center";
          propertyValue = centerParameter(++i);
          i = eval.iToken;
          haveCenter = true;
          break;
        }
        //$FALL-THROUGH$
      default:
        if (eval.isColorParam(i)) {
          if (atomPt == i - 1)
            needsGenerating = true;
          colorArgb[0] = eval.getArgbParam(i);
          if (eval.isCenterParameter(i))
            noToParam = eval.iToken + 1;
          i = eval.iToken;
          continue;
        }
        invArg();
      }
      if (propertyName != null)
        setShapeProperty(JC.SHAPE_POLYHEDRA, propertyName, propertyValue);
      if (onOffDelete)
        return;
    }
    if (needsGenerating) {
      if (!typeSeen && haveBonds)
        setShapeProperty(JC.SHAPE_POLYHEDRA, "bonds", null);
      setShapeProperty(JC.SHAPE_POLYHEDRA, "generate", Boolean.valueOf(modifyOnly));
    } else if (!ok) {// && lighting == T.nada)
      error(ScriptError.ERROR_insufficientArguments);
    }
    if (offset != null)
      setShapeProperty(JC.SHAPE_POLYHEDRA, "offset", offset);
    if (!Double.isNaN(scale))
      setShapeProperty(JC.SHAPE_POLYHEDRA, "scale", Double.valueOf(scale));
    if (colorArgb[0] != Integer.MIN_VALUE)
      setShapeProperty(JC.SHAPE_POLYHEDRA, "colorThis",
          Integer.valueOf(colorArgb[0]));
    if (translucentLevel != Double.MAX_VALUE)
      eval.setShapeTranslucency(JC.SHAPE_POLYHEDRA, "", "translucentThis",
          translucentLevel, null);
    //    if (lighting != T.nada)
    //      setShapeProperty(JC.SHAPE_POLYHEDRA, "token", Integer.valueOf(lighting));
    setShapeProperty(JC.SHAPE_POLYHEDRA, "init", Boolean.FALSE);
  }

  /**
   * 
   * @param args
   * @return string for write() function
   * @throws ScriptException
   */
  private String write(T[] args) throws ScriptException {
    if (args == null && vwr.haveAccessInternal(null))
      invArg();
    ScriptEval eval = e;
    int pt = 1, pt0 = 1;
    String[] scripts = null;
    String msg = null;
    String localPath = null;
    String remotePath = null;
    String type = "SPT";
    boolean isCommand = true;
    boolean showOnly = false;
    boolean isContact = false;
    if (args == null) {
      // write command
      args = st;
      showOnly = (vwr.isApplet && !vwr.isSignedApplet
          || !vwr.haveAccess(ACCESS.ALL)
          || vwr.fm.getPathForAllFiles().length() > 0);
    } else {
      // write() function or image
      pt = pt0 = 0;
      isCommand = false;//(args == st);
      showOnly = !isCommand;
    }

    // check for special considerations involving first parameter

    int tok = tokAtArray(pt, args);
    if (tok == T.string && !isCommand) {
      T t0 = T.getTokenFromName(SV.sValue(args[0]).toLowerCase());
      if (t0 != null)
        tok = t0.tok;
    }
    switch (tok) {
    case T.nada:
      break;
    case T.quaternion:
    case T.ramachandran:
    case T.property:
      msg = plot(args);
      return (showOnly ? msg : writeMsg(msg));
    case T.script:
      // would fail in write() command.
      // not documented?
      // use?
      if (eval.isArrayParameter(pt + 1)) {
        scripts = eval.stringParameterSet(++pt);
        localPath = ".";
        remotePath = ".";
        pt0 = pt = eval.iToken + 1;
        tok = tokAt(pt);
      }
      break;
    default:
      type = SV.sValue(tokenAt(pt, args)).toUpperCase();
    }

    String driverList = vwr.getExportDriverList();
    String data = null;
    int argCount = (isCommand ? slen : args.length);
    String type2 = "";
    String val = null;
    boolean is2D = false;
    SV tVar = null;
    int nVibes = 0;
    String sceneType = null;
    boolean isCoord = false;
    BS bsFrames = null;
    int width = -1;
    int height = -1;
    boolean isExport = false;
    String fileName = null;
    int quality = Integer.MIN_VALUE;

    // accept write ...... AS type

    if (tok != T.nada && isCommand && slen > 1 && tokAt(slen - 2) == T.as) {
      type = paramAsStr(slen - 1).toUpperCase();
      pt0 = argCount;
      argCount -= 2;
      tok = T.nada;
    }

    // check type

    switch (tok) {
    case T.nada:
      break;
    case T.barray:
    case T.hash:
      type = "VAR";
      tVar = (SV) tokenAt(pt++, args);
      break;
    case T.inline:
      type = "INLINE";
      data = SV.sValue(tokenAt(++pt, args));
      pt++;
      break;
    case T.pointgroup:
      type = "PGRP";
      pt++;
      type2 = SV.sValue(tokenAt(pt, args)).toLowerCase();
      if (type2.equals("draw"))
        pt++;
      break;
    case T.coord:
      pt++;
      isCoord = true;
      break;
    case T.state:
    case T.script:
      val = SV.sValue(tokenAt(++pt, args)).toLowerCase();
      while (val.equals("localpath") || val.equals("remotepath")) {
        if (val.equals("localpath"))
          localPath = SV.sValue(tokenAt(++pt, args));
        else
          remotePath = SV.sValue(tokenAt(++pt, args));
        val = SV.sValue(tokenAt(++pt, args)).toLowerCase();
      }
      type = "SPT";
      break;
    case T.file:
    case T.function:
    case T.history:
    case T.isosurface:
    case T.menu:
    case T.mesh:
    case T.nbo:
    case T.mo:
    case T.pmesh:
      pt++;
      break;
    case T.jmol:
      type = "ZIPALL";
      pt++;
      break;
    case T.var:
      type = "VAR";
      pt += 2;
      break;
    case T.drawing:
    case T.frame:
    case T.image:
    case T.obj:
    case T.scene:
    case T.vibration:
    case T.identifier:
    case T.string:
      switch (tok) {
      case T.frame:
        BS bsAtoms;
        if (pt + 1 < argCount && args[++pt].tok == T.expressionBegin
            || args[pt].tok == T.bitset) {
          bsAtoms = eval.atomExpression(args, pt, 0, true, false, null, true);
          pt = eval.iToken + 1;
        } else {
          bsAtoms = vwr.getAllAtoms();
        }
        if (!chk)
          bsFrames = vwr.ms.getModelBS(bsAtoms, true);
        break;
      case T.drawing:
        tok = T.image;
        is2D = true;
        //$FALL-THROUGH$
      case T.image:
        type = "IMAGE";
        pt++;
        break;
      case T.scene:
        val = SV.sValue(tokenAt(++pt, args)).toUpperCase();
        if (PT.isOneOf(val, ";PNG;PNGJ;")) {
          sceneType = val;
          pt++;
        } else {
          sceneType = "PNG";
        }
        break;
      case T.vibration:
        nVibes = eval.intParameterRange(++pt, 1, 10);
        if (nVibes == Integer.MAX_VALUE)
          return "";
        if (!chk) {
          vwr.tm.setVibrationPeriod(0);
          if (!eval.isJS)
            eval.delayScript(100);
        }
        pt++;
        break;
      default:
        // identifier or string
        tok = T.image;
        break;
      }
      if (tok == T.image && pt < args.length) {
        // write IMAGE JPG
        // write JPG
        T t = T.getTokenFromName(SV.sValue(args[pt]).toLowerCase());
        if (t != null)
          type = SV.sValue(t).toUpperCase();
        if (PT.isOneOf(type, driverList.toUpperCase())) {
          // povray, maya, vrml, idtf
          pt++;
          type = type.substring(0, 1).toUpperCase()
              + type.substring(1).toLowerCase();
          // Povray, Maya, Vrml, Idtf
          isExport = true;
          if (isCommand)
            fileName = "Jmol." + type.toLowerCase();
          break;
        } else if (PT.isOneOf(type, ";ZIP;ZIPALL;SPT;STATE;")) {
          pt++;
          break;
        } else {
          type = "IMAGE";
        }
      }
      if (tokAtArray(pt, args) == T.integer) {
        width = SV.iValue(tokenAt(pt++, args));
        if (width <= 0)
          invArg();
        height = SV.iValue(tokenAt(pt++, args));
        if (height <= 0)
          invArg();
        if (tokAtArray(pt, args) == T.integer) {
          quality = SV.iValue(tokenAt(pt++, args));
        }
      }
      break;
    }

    if (pt0 < argCount) {
      // get type
      val = SV.sValue(tokenAt(pt, args));
      if (val.equalsIgnoreCase("clipboard")) {
        if (chk)
          return "";
        // if (isApplet)
        // evalError(GT.$("The {0} command is not available for the applet.",
        // "WRITE CLIPBOARD"));
      } else if (PT.isOneOf(val.toLowerCase(), JC.IMAGE_TYPES)) {
        if (tokAtArray(pt + 1, args) == T.integer
            && tokAtArray(pt + 2, args) == T.integer) {
          width = SV.iValue(tokenAt(++pt, args));
          if (width <= 0)
            invArg();
          height = SV.iValue(tokenAt(++pt, args));
          if (height <= 0)
            invArg();
        }
        if (tokAtArray(pt + 1, args) == T.integer)
          quality = SV.iValue(tokenAt(++pt, args));
      } else if (PT.isOneOf(val.toLowerCase(),
          ";xyz;xyzrn;xyzvib;mol;mol67;sdf;v2000;v3000;json;pdb;pqr;cml;cif;scif;cifp1;pwmat;pwslab;qcjson;xsf;")) {
        // this still could be overruled by a type indicated
        type = val.toUpperCase();
        if (pt + 1 == argCount)
          pt++; // no PDB keyword given
      }

      // write [image|history|state] clipboard

      // write [optional image|history|state] [JPG quality|JPEG quality|JPG64
      // quality|PNG quality|PPM|SPT] "filename"
      // write script "filename"
      // write isosurface t.jvxl
      // write isosurface t.pmesh
      // write contact t.jvxl

      if (type.equals("IMAGE")
          && PT.isOneOf(val.toLowerCase(), JC.IMAGE_OR_SCENE)) {
        type = val.toUpperCase();
        pt++;
      }
    }
    if (pt + 2 == argCount) {
      // unprocessed explicit type
      // type may be defined already, but that could be from a file name extension
      // here we override that
      // write PDB "xxx.pdb"
      SV.sValue(tokenAt(++pt, args));
      //System.out.println(val);
      //    if (s.length() > 0 && s.charAt(0) != '.') {
      //      if (val == null) {
      //System.out.println("??");
      //        type = val.toUpperCase();
      //      }
      //    }
    }

    // set the file name

    switch (tokAtArray(pt, args)) {
    case T.nada:
      // WRITE by itself will do WRITE SPT to console
      showOnly = true;
      break;
    case T.clipboard:
      break;
    case T.opIf:
      // ? by itself
      fileName = (type.equals("IMAGE") ? "?jmol.png"
          : "?jmol." + type.toLowerCase());
      break;
    case T.identifier:
    case T.string:
      fileName = SV.sValue(tokenAt(pt, args));
      if (fileName.equalsIgnoreCase("clipboard") || !vwr.haveAccess(ACCESS.ALL))
        fileName = null;
      //      else if (isCommand && argCount != slen 
      //          && (tokAt(pt + 1) == T.per || tokAt(pt + 1) == T.colon)) {
      //        fileName = "";
      //        while (pt < argCount)
      //          fileName += SV.sValue(tokenAt(pt++, args));
      //      }
      break;
    default:
      invArg();
    }

    // adjust the type from the filename and set whether this is an export

    if (type.equals("IMAGE") || type.equals("FRAME")
        || type.equals("VIBRATION")) {
      type = (fileName != null && fileName.indexOf(".") >= 0
          ? fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase()
          : "JPG");
      // Introduced in 14.3.9_2014.11.23; removed 14.3.12_2015.02.24
      // This was a bad idea; changing the file name automatically is not appropriate -- that is what AS is for
      // if (PT.isOneOf(type, ";PNGJ;PNGT;GIFT;"))
      //   fileName = fileName.substring(0, fileName.length() - 1);
    }

    if (type.equals("ISOSURFACE") || type.equals("CONTACT") || type.equals("PLB")) {
      isContact = type.equals("CONTACT");
      if (!type.equals("PLB"))
        type = (fileName != null && fileName.indexOf(".") >= 0
          ? fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase()
          : "JVXL");
      if (type.equals("PMESH"))
        type = "ISOMESH";
      else if (type.equals("PMB"))
        type = "ISOMESHBIN";
      if (type.equals("PLY"))
        type = "ISOPLY";
      else if (type.equals("PLB"))
        type = "ISOPLYBIN";
    }
    boolean isImage = PT.isOneOf(type.toLowerCase(), JC.IMAGE_OR_SCENE);
    if (!isImage) {
      if (type.equals("MNU")) {
        type = "MENU";
      } else if (type.equals("WRL") || type.equals("VRML")) {
        type = "Vrml";
        isExport = true;
      } else if (type.equals("X3D")) {
        type = "X3d";
        isExport = true;
      } else if (type.equals("STL")) {
        type = "Stl";
        isExport = true;
      } else if (type.equals("IDTF")) {
        type = "Idtf";
        isExport = true;
      } else if (type.equals("MA")) {
        type = "Maya";
        isExport = true;
      } else if (type.equals("JS")) {
        type = "Js";
        isExport = true;
      } else if (type.equals("OBJ")) {
        type = "Obj";
        isExport = true;
      } else if (type.equals("JVXL")) {
        type = "ISOSURFACE";
      } else if (type.equals("XJVXL")) {
        type = "ISOSURFACE";
      } else if (type.equals("JMOL")) {
        type = "ZIPALL";
      } else if (type.equals("HIS")) {
        type = "HISTORY";
      }
      if (type.equals("COORD") || type.equals("COORDS"))
        type = (fileName != null && fileName.indexOf(".") >= 0
            ? fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase()
            : "XYZ");
    }
    if (scripts != null) {
      if (!FileManager.isJmolType(type))
        invArg();
      if (type.equals("PNG"))
        type = "PNGJ";
    }
    if (!isImage && !isExport && !PT.isOneOf(type,
        ";SCENE;JMOL;ZIP;ZIPALL;SPT;HISTORY;MO;NBO;ISOSURFACE;MESH;PMESH;PMB;PLY;PLB;ISOMESHBIN;ISOMESH;ISOPLY;ISOPLYBIN;VAR;FILE;FUNCTION;CFI;CIF;SCIF;CIFP1;CML;JSON;XYZ;XYZRN;XYZVIB;MENU;MOL;MOL67;PDB;PGRP;PQR;PWMAT;PWSLAB;QUAT;RAMA;SDF;V2000;V3000;QCJSON;XSF;INLINE;"))
      eval.errorStr2(ScriptError.ERROR_writeWhat,
          "COORDS|FILE|FUNCTIONS|HISTORY|IMAGE|INLINE|ISOSURFACE|JMOL|MENU|MO|NBO|POINTGROUP|QUATERNION [w,x,y,z] [derivative]"
              + "|RAMACHANDRAN|SPT|STATE|VAR x|ZIP|ZIPALL|CLIPBOARD",
          "CIF|SCIF|CIFP1|CML|CFI|GIF|GIFT|JPG|JPG64|JMOL|JVXL|MESH|MOL|PDB|PMESH|PNG|PNGJ|PNGT|PPM|PQR|PWMAT|PWSLAB|SDF|CD|JSON|QCJSON|V2000|V3000|SPT|XJVXL|XSF|XYZ|XYZRN|XYZVIB|ZIP"
              + driverList.toUpperCase().replace(';', '|'));
    if (chk)
      return "";

    String[] fullPath = new String[1];
    Map<String, Object> params;
    boolean timeMsg = vwr.getBoolean(T.showtiming);

    // process write command based on data type

    if (isExport) {
      if (timeMsg)
        Logger.startTimer("export");
      Map<String, Object> eparams = new Hashtable<String, Object>();
      eparams.put("type", type);
      if (fileName != null)
        eparams.put("fileName", fileName);
      if (isCommand || fileName != null)
        eparams.put("fullPath", fullPath);
      eparams.put("width", Integer.valueOf(width));
      eparams.put("height", Integer.valueOf(height));
      try {
        data = vwr.generateOutputForExport(eparams);
      } catch (Exception e) {
        data = e.toString();// failed to write -- could be complex OBJ/MTL file
        return data;
      }
      if (data == null || data.length() == 0)
        return "";
      if (showOnly)
        return data;
      if (!type.equals("Povray") && !type.equals("Idtf") || fullPath[0] == null)
        return writeMsg(data);
      // must also produce .ini or .tex file
      String ext = (type.equals("Idtf") ? ".tex" : ".ini");
      fileName = fullPath[0] + ext;
      params = new Hashtable<String, Object>();
      params.put("fileName", fileName);
      params.put("type", ext);
      params.put("text", data);
      params.put("fullPath", fullPath);
      msg = vwr.processWriteOrCapture(params);
      // fullPath may be changed here
      if (type.equals("Idtf"))
        data = data.substring(0, data.indexOf("\\begin{comment}"));
      data = "Created " + fullPath[0] + ":\n\n" + data;
      if (timeMsg)
        showString(Logger.getTimerMsg("export", 0));
      if (msg != null) {
        boolean isError = !msg.startsWith("OK");
        if (isError)
          eval.evalError(msg, null);
        eval.report(data, isError);
      }
      return "";
    }

    // creating bytes or data

    Object bytes = null;
    boolean writeFileData = false;
    if (data == null) {
      int len = 0;
      data = type.intern();
      if (data == "MENU") {
        data = vwr.getMenu("");
      } else if (data == "PGRP") {
        data = vwr.ms.getPointGroupAsString(vwr.bsA(), null, 0, 1, null,
            null, type2.equals("draw") ? "" : null);
      } else if (data == "PDB" || data == "PQR") {
        if (showOnly) {
          data = vwr.getPdbAtomData(null, null, (data == "PQR"), isCoord);
        } else {
          writeFileData = true;
          type = "PDB_" + data + "-coord " + isCoord;
        }
      } else if (data == "FILE") {
        if ("?".equals(fileName))
          fileName = "?Jmol." + vwr.getP("_fileType");
        if (showOnly)
          data = e.getCurrentModelFileAsString(null);
        else
          writeFileData = true;
      } else if (PT.isOneOf(data,
          ";CIF;SCIF;CIFP1;SDF;MOL;MOL67;V2000;V3000;CD;JSON;XYZ;XYZRN;XYZVIB;CML;QCJSON;PWMAT;PWSLAB;XSF;")) {
        BS selected = vwr.bsA(), bsModel;
        // mechanism to pass information back from writer, changing the number of atoms written
        vwr.setErrorMessage(null, " (" + selected.cardinality() + " atoms)");
        data = vwr.getModelExtract(selected, isCoord, false, data);
        msg = vwr.getErrorMessageUn();
        vwr.setErrorMessage(null, null);
        if (vwr.am.cmi >= 0 && !selected
            .equals(bsModel = vwr.getModelUndeletedAtomsBitSet(vwr.am.cmi)))
          msg += "\nNote! Selected atom set " + selected
              + " is not the same as the current model " + bsModel;
        if (data.startsWith("ERROR:"))
          bytes = data;
      } else if (data == "CFI") {
        data = vwr.getModelFileData("selected", "cfi", false);
      } else if (data == "FUNCTION") {
        data = vwr.getFunctionCalls(null);
        type = "TXT";
      } else if (data == "VAR") {
        if (tVar == null) {
          tVar = (SV) eval.getParameter(
              SV.sValue(tokenAt(isCommand ? 2 : 1, args)), T.variable, true);
        }
        Lst<Object> v = null;
        if (tVar.tok == T.barray) {
          v = new Lst<Object>();
          v.addLast(((BArray) tVar.value).data);
        } else if (tVar.tok == T.hash) {
          v = (fileName == null ? new Lst<Object>()
              : prepareBinaryOutput(tVar));
        }
        if (v == null) {
          //          if (bytes == null) {
          data = tVar.asString();
          type = "TXT";
          //          }
        } else {
          if (fileName != null) {
            params = new Hashtable<String, Object>();
            params.put("data", v);
            if ((bytes = data = (String) vwr.createZip(fileName,
                v.size() == 1 || fileName.endsWith(".png")
                    || fileName.endsWith(".pngj") ? "BINARY" : "ZIPDATA",
                params)) == null)
              eval.evalError("#CANCELED#", null);
          }
        }
      } else if (data == "SPT") {
        if (isCoord) {
          BS tainted = vwr.ms.getTaintedAtoms(AtomCollection.TAINT_COORD);
          vwr.setAtomCoordsRelative(P3d.new3(0, 0, 0), null);
          data = vwr.getStateInfo();
          vwr.ms.setTaintedAtoms(tainted, AtomCollection.TAINT_COORD);
        } else {
          data = vwr.getStateInfo();
          if (localPath != null || remotePath != null)
            data = FileManager.setScriptFileReferences(data, localPath,
                remotePath, null);
        }
      } else if (data == "ZIP" || data == "ZIPALL") {
        if (fileName != null) {
          params = new Hashtable<String, Object>();
          if (scripts != null)
            params.put("data", scripts);
          if ((bytes = data = (String) vwr.createZip(fileName, type,
              params)) == null)
            eval.evalError("#CANCELED#", null);
        }
      } else if (data == "HISTORY") {
        data = vwr.getSetHistory(Integer.MAX_VALUE);
        type = "SPT";
      } else if (data == "MO" || data == "NBO") {
        data = getMoJvxl(Integer.MAX_VALUE, data == "NBO");
        type = "XJVXL";
      } else if (data == "PMESH" || data == "PMB") {
        if ((data = (String) getIsosurfaceJvxl(JC.SHAPE_PMESH, data)) == null)
          error(ScriptError.ERROR_noData);
        type = "XJVXL";
      } else if (data == "ISOMESH") {
        if ((data = (String) getIsosurfaceJvxl(JC.SHAPE_ISOSURFACE,
            data)) == null)
          error(ScriptError.ERROR_noData);
        type = "PMESH";
      } else if (data == "ISOMESHBIN") {
        if ((bytes = getIsosurfaceJvxl(JC.SHAPE_ISOSURFACE,
            "ISOMESHBIN")) == null)
          error(ScriptError.ERROR_noData);
        type = "PMB";
      } else if (data == "ISOPLY") {
        if ((data = (String) getIsosurfaceJvxl(JC.SHAPE_ISOSURFACE,
            data)) == null)
          error(ScriptError.ERROR_noData);
        type = "PLY";
      } else if (data == "ISOPLYBIN") {
        if ((bytes = getIsosurfaceJvxl(JC.SHAPE_ISOSURFACE,
            "ISOPLYBIN")) == null)
          error(ScriptError.ERROR_noData);
        type = "PLB";
      } else if (data == "ISOSURFACE" || data == "MESH") {
        if ((data = (String) getIsosurfaceJvxl(
            isContact ? JC.SHAPE_CONTACT : JC.SHAPE_ISOSURFACE, data)) == null)
          error(ScriptError.ERROR_noData);
        type = (data.indexOf("<?xml") >= 0 ? "XJVXL" : "JVXL");
        if (!showOnly)
          showString((String) getShapeProperty(
              isContact ? JC.SHAPE_CONTACT : JC.SHAPE_ISOSURFACE,
              "jvxlFileInfo"));
      } else {
        // image
        if (isCommand && showOnly && fileName == null) {
          showOnly = false;
          fileName = "\1";
        }
        len = -1;
        if (sceneType == null && quality < 0)
          quality = -1;
      }
      if (data == null)
        data = "";
      if (len == 0)
        len = (bytes == null ? data.length()
            : bytes instanceof String ? ((String) bytes).length()
                : ((byte[]) bytes).length);
    }

    // if write() function, then just return data
    if (!isCommand)
      return data;
    // if WRITE command, but cannot write files, send through PRINT channel
    if (showOnly) {
      eval.showStringPrint(data, true);
      return "";
    }
    // String bytes indicates an error message 
    if (bytes != null && bytes instanceof String)
      return writeMsg((String) bytes);
    // Just save the file data and return a confirmation message 
    if (writeFileData)
      return writeMsg(vwr.writeFileData(fileName, type, 0, null));
    // use vwr.processWriteOrCapture(params) for all other situations
    if (type.equals("SCENE"))
      bytes = sceneType;
    else if (bytes == null && (!isImage || fileName != null))
      bytes = data;
    if (timeMsg)
      Logger.startTimer("write");
    if (isImage) {
      eval.refresh(false);
      if (width < 0)
        width = (is2D ? 250 : vwr.getScreenWidth());
      if (height < 0)
        height = (is2D ? 250 : vwr.getScreenHeight());
    }
    params = new Hashtable<String, Object>();
    if (fileName != null)
      params.put("fileName", fileName);
    params.put("backgroundColor", Integer.valueOf(vwr.getBackgroundArgb()));
    params.put("type", type);
    if (is2D) {
      params.put("is2D", Boolean.TRUE);
      String smiles;
      Object ret = "smiles could not be generated";
      try {
        smiles = vwr.getOpenSmiles(null);
        if (smiles.length() > 0) {
          String fname = (String) vwr.setLoadFormat(true, "_" + smiles, '2',
              false);
          fname += "?width=" + width + "&height=" + height + "&format="
              + type.toLowerCase();
          showString(fname);
          ret = vwr.fm.getFileAsBytes(fname, null);
        }
      } catch (Exception e1) {
      }
      if (ret instanceof String) {
        showString((String) ret);
        return null;
      }
      bytes = ret;
    }
    if (bytes instanceof String && quality == Integer.MIN_VALUE)
      params.put("text", bytes);
    else if (bytes instanceof byte[])
      params.put("bytes", bytes);
    if (scripts != null)
      params.put("scripts", scripts);
    if (bsFrames != null)
      params.put("bsFrames", bsFrames);
    params.put("fullPath", fullPath);
    params.put("quality", Integer.valueOf(quality));
    params.put("width", Integer.valueOf(width));
    params.put("height", Integer.valueOf(height));
    params.put("nVibes", Integer.valueOf(nVibes));
    String ret = vwr.processWriteOrCapture(params);
    if (ret == null)
      ret = "canceled";
    if (isImage && ret.startsWith("OK"))
      ret += "; width=" + width + "; height=" + height;
    if (timeMsg)
      showString(Logger.getTimerMsg("write", 0));
    return writeMsg(ret + (msg == null ? "" : msg));
  }

  public Lst<Object> prepareBinaryOutput(SV tvar) {
    Map<String, SV> m = tvar.getMap();
    if (m == null || !m.containsKey("$_BINARY_$"))
      return null;
    Lst<Object> v = new Lst<Object>();
    for (Entry<String, SV> e : m.entrySet()) {
      String key = e.getKey();
      if (key.equals("$_BINARY_$"))
        continue;
      SV o = e.getValue();
      byte[] bytes = (o.tok == T.barray ? ((BArray) o.value).data : null);
      if (bytes == null) {
        String s = o.asString();
        bytes = (s.startsWith(JC.BASE64_TAG) ? Base64.decodeBase64(s)
            : s.getBytes());
      }
      if (key.equals("_DATA_")) {
        // just return this binary data value
        v = new Lst<Object>();
        v.addLast(bytes);
        return v;
      } else if (key.equals("_IMAGE_")) {
        v.add(0, key);
        v.add(1, null);
        v.add(2, bytes);
      } else {
        v.addLast(key);
        v.addLast(null);
        v.addLast(bytes);
      }
    }
    return v;
  }

  private String writeMsg(String msg) throws ScriptException {
    if (chk || msg == null)
      return "";
    boolean isError = !msg.startsWith("OK");
    if (isError) {
      e.evalError(msg, null);
      /**
       * @j2sNative
       * 
       *            alert(msg);
       */
      {
      }
    }
    e.report(msg, isError);
    return msg;
  }

  @SuppressWarnings("unchecked")
  private void show() throws ScriptException {
    ScriptEval eval = e;
    String value = null;
    String str = paramAsStr(1);
    String filter = null;
    int filterLen = 0;
    if (slen > 3 && tokAt(slen - 3) == T.divide && tokAt(slen - 2) == T.opNot) {
      filter = "!/" + paramAsStr(slen - 1);
      slen -= 3;
      filterLen = 3;
    } else if (slen > 2 && tokAt(slen - 2) == T.divide) {
      filter = "/" + paramAsStr(slen - 1);
      slen -= 2;
      filterLen = 2;
    } else if ((filter = paramAsStr(slen - 1)).lastIndexOf("/") == 0) {
      slen--;
      filterLen = 1;
    } else {
      filter = null;
    }
    String msg = null;
    String name = null;
    int len = 2;
    T token = getToken(1);
    // T.identifier for SV is set for variable names 
    int tok = (token instanceof SV && token.tok != T.identifier ? T.nada
        : token.tok);
    if (tok == T.string) {
      token = T.getTokenFromName(str.toLowerCase());
      if (token != null)
        tok = token.tok;
    }
    if (tok != T.symop && tok != T.state && tok != T.property && tok != T.file)
      checkLength(-3);
    if (slen == 2 && str.indexOf("?") >= 0) {
      msg = vwr.getAllSettings(str.substring(0, str.indexOf("?")));
      tok = -1;
    }
    switch (tok) {
    case -1:
      break;
    case T.nada:
      if (!chk)
        msg = ((SV) eval.theToken).escape();
      break;
    case T.undo:
      if (!chk)
        msg = vwr.stm.getUndoInfo();
      break;
    case T.domains:
      eval.checkLength23();
      len = st.length;
      if (!chk) {
        Object d = vwr.getModelInfo("domains");
        if (d instanceof SV)
          msg = vwr.getAnnotationInfo((SV) d, eval.optParameterAsString(2),
              T.domains);
        else
          msg = "domain information has not been loaded";
      }
      break;
    case T.property:
      msg = plot(st);
      len = st.length;
      break;
    case T.validation:
      eval.checkLength23();
      len = st.length;
      if (!chk) {
        Object d = vwr.getModelInfo("validation");
        if (d instanceof SV)
          msg = vwr.getAnnotationInfo((SV) d, eval.optParameterAsString(2),
              T.validation);
        else
          msg = "validation information has not been loaded";
      }
      break;
    case T.cache:
      if (!chk)
        msg = Escape.e(vwr.fm.cacheList());
      break;
    case T.dssr:
      eval.checkLength23();
      len = st.length;
      if (!chk) {
        Object d = vwr.getModelInfo("dssr");
        msg = (d == null ? "no DSSR information has been read"
            : len > 2
                ? SV.getVariable(vwr.extractProperty(d, stringParameter(2), -1))
                    .asString()
                : "" + SV.getVariable(d).asString());
      }
      break;
    case T.dssp:
      int version = 2;
      if (slen == 3)
        version = ((int) doubleParameter((len = 3) - 1));
      else
        checkLength(2 + filterLen);
      if (!chk)
        msg = vwr.calculateStructures(null, true, false, version);
      break;
    case T.pathforallfiles:
      checkLength(2 + filterLen);
      if (!chk)
        msg = vwr.fm.getPathForAllFiles();
      break;
    case T.polyhedra:
      if (!chk) {
        Object[] info = new Object[2];
        vwr.shm.getShapePropertyData(JC.SHAPE_POLYHEDRA, "allInfo", info);
        msg = SV.getVariable(info[1]).asString();
      }
      break;
    case T.nmr:
      if (!chk)
        vwr.getNMRPredict(eval.optParameterAsString(2));
      return;
    case T.modelkitmode:
      if (!chk && slen == 3) {
        msg = "" + vwr.getModelkitPropertySafely(eval.stringParameter(2));
        len = 3;
      }
      break;
    case T.inchi:
    case T.inchikey:
    case T.drawing:
    case T.chemical:
    case T.smiles:
      checkLength(
          (tok == T.chemical || tok == T.smiles && tokAt(2) == T.on ? len = 3
              : 2) + filterLen);
      if (chk)
        return;
      String param2 = eval.optParameterAsString(2);
      if (tok == T.chemical) {
        if ("mf".equals(param2))
          param2 = "formula";
        if ("formula".equals(param2)) {
          msg = (String) vwr.getModelInfo("formula");
          // cif files will have formula already
          if (msg != null)
            msg = PT.rep(msg, " ", "");
        }
      }
      if (msg == null) {
        try {
          switch (tok) {
          case T.inchi:
            msg = vwr.getInchi(vwr.bsA(), null, null);
            if (msg == null)
              msg = "?";
            break;
          case T.inchikey:
            msg = vwr.getInchi(vwr.bsA(), null, "key");
            break;
          case T.smiles:
            if (param2.equalsIgnoreCase("true")) {
              msg = vwr.getBioSmiles(null);
              filter = null;
            } else if (filter != null) {
              msg = vwr.getSmilesOpt(null, -1, -1, JC.SMILES_TYPE_SMILES,
                  filter + "///");
              filter = null;
            }
            break;
          default:
            msg = vwr.ms.getModelDataBaseName(vwr.bsA());
            // this does not work for NCI, because, for example, "$menthol" returns an enantiomer, but menthol/smiles uses nonstereo option
            // but we don't want to be generating our own SMILES here, do we?
            // what is the solution?
            if (msg != null && (msg.startsWith("$") || msg.startsWith(":"))) {
              msg = msg.substring(1);
            } else {
              msg = null;
            }
            break;
          }
          if (msg == null) {
            int level = Logger.getLogLevel();
            Logger.setLogLevel(4);
            msg = (tok == T.smiles ? vwr.getSmiles(null)
                : vwr.getOpenSmiles(null));
            Logger.setLogLevel(level);
          }
        } catch (Exception ex) {
          msg = ex.getMessage();
          if (msg == null) {
            msg = "";
          }
          ex.printStackTrace();
        }
        switch (tok) {
        case T.smiles:
          break;
        case T.drawing:
          if (msg.length() > 0) {
            vwr.fm.loadImage(vwr.setLoadFormat(false, "_" + msg, '2', false),
                "\1" + msg, false);
            return;
          }
          msg = "Could not show drawing -- Either insufficient atoms are selected or the model is a PDB file.";
          break;
        case T.chemical:
          len = 3;
          if (msg.length() > 0) {
            msg = vwr.getChemicalInfo(msg, param2, vwr.bsA());
            if (msg.indexOf("FileNotFound") >= 0)
              msg = "?";
          } else {
            msg = "Could not show name -- Either insufficient atoms are selected or the model is a PDB file.";
          }
        }
      }
      break;
    case T.wyckoffm:
    case T.wyckoff:
      if (chk)
        break;
      msg = ("" + vwr.evaluateExpression(
          "symop('wyckoff" + (tok == T.wyckoffm ? "m" : "") + "')")).trim();
      break;
    case T.spacegroup:
    case T.symop:
      SymmetryInterface sym = vwr.getOperativeSymmetry();
      switch (tok) {
      case T.spacegroup:
        String sdiag = eval.optParameterAsString(2);
        boolean isNames = sdiag.toLowerCase().equals("names");
        boolean isSubgroup = sdiag.toLowerCase().startsWith("subgroups");
        boolean isDiagram = sdiag.toLowerCase().startsWith("diagram");
        boolean isDiagramITA = sdiag.startsWith("diagramITA"); // undocumented - testing only
        boolean isTable = sdiag.toLowerCase().startsWith("table");
        len = slen;
        int ita = -1;
        if (isNames) {
          if (chk)
            break;
        } else if (isSubgroup || isDiagram || isTable || isDiagramITA) {
          if (chk)
            break;
          sdiag = PT.trim(sdiag
              .substring(isDiagramITA ? 10 : isSubgroup ? 9 : isDiagram ? 7 : 5)
              .trim(), "\"");
          if (sdiag.length() == 0) {
            if (sym == null) {
              msg = "Include a space group name or number to view its diagram, table, or subgroups";
              break;
            }
            if (sym.getSpaceGroupClegId().charAt(1) == '/') {
              // special not available ? 
              msg = "special group information is not implemented yet";
              break;
            }
            sdiag = sym.getIntTableNumber();
            if (sdiag == null || sdiag.equals("0")) {
              msg = "Can't display a table; don't have an ITA number.";
              break;
            }
          }
          if (msg != null)
            break;
          ita = PT.parseInt(sdiag);
          if (ita == Integer.MIN_VALUE) {
            if (sym == null) {
              sym = vwr.getSymStatic();
            }
            Map<String, Object> info = sym.getSpaceGroupInfo(vwr.ms,
                PT.rep(sdiag, "''", "\""), -1, true, null);
            if (info == null) {
              msg = "Could not find space group " + sdiag;
              break;
            }
            ita = ((Integer) ((Map<String, Object>) info.get("spaceGroupInfo"))
                .get("ita")).intValue();
          }
        }
        if (isNames || ita > 0) {
          String href = (isNames ? JC.SPACE_GROUP_LIST_BCS
              : JC.resolveDataBase(
                  isDiagramITA ? "itadiagramita"
                      : isDiagram ? "itadiagram" : "itatable",
                  PT.formatS("" + ita, 3, 0, false, true), null));
          msg = href;
          vwr.showUrl(href);
          break;
        }
        //$FALL-THROUGH$
      case T.symop:
        msg = "";
        Map<String, Object> info = null;
        if ((len = slen) == 2) {
          if (chk)
            break;
          if (sym == null)
            sym = vwr.getSymTemp();
          info = sym.getSpaceGroupInfo(vwr.ms, null, -1, false, null);
        } else if (tok == T.spacegroup) {
          String sg = paramAsStr(2);
          len = 3;
          if (chk)
            break;
          if (sg.startsWith("list/")) {
            filter = sg.substring(4);
            sg = "list";
          }
          info = vwr.getSymTemp().getSpaceGroupInfo(vwr.ms,
              PT.rep(sg, "''", "\""), -1, false, null);
        }
        if (info != null) {
          msg = (tok == T.spacegroup
              ? "" + info.get(JC.INFO_SPACE_GROUP_INFO)
                  + info.get(JC.INFO_SPACE_GROUP_NOTE)
              : "")
              + (info.containsKey("symmetryInfo") ? info.get("symmetryInfo")
                  : "");
          break;
        }
        // symop only here
        int iop = (tokAt(2) == T.integer ? intParameter(2) : 0);
        String xyz = (tokAt(2) == T.string || tokAt(2) == T.matrix4f
            ? paramAsStr(2)
            : null);
        P3d pt1 = null, pt2 = null;
        int nth = -1;
        if (slen > 3 && tokAt(3) != T.string) {
          // show symop @1 @2 ....
          // show symop n @1 @2
          // not show symop n "type"
          // not show symop "xxxxx" "type"
          Object[] ret = new Object[] { null, vwr.getFrameAtoms() };
          pt1 = eval.centerParameter(2 + (iop == 0 ? 0 : 1), ret);
          if (ret[0] != null && ((BS) ret[0]).isEmpty()) {
            len = slen;
            break;
          }
          ret[0] = null;
          if (tokAt(eval.iToken + 1) != 0 && tokAt(eval.iToken + 1) != T.string)
            pt2 = eval.centerParameter(++eval.iToken, ret);
          if (ret[0] != null && ((BS) ret[0]).isEmpty()) {
            len = slen;
            break;
          }
          if (iop == 0 && tokAt(eval.iToken + 1) == T.integer)
            nth = eval.getToken(++eval.iToken).intValue;
        }
        String type = (eval.iToken > 1 && tokAt(eval.iToken + 1) == T.string
            ? stringParameter(++eval.iToken)
            : pt1 == null && pt2 == null ? "label" : null);
        checkLength((len = ++eval.iToken) + filterLen);
        if (!chk) {
          Object o = vwr.getSymmetryInfo(vwr.getAllAtoms().nextSetBit(0), xyz,
              iop, null, pt1, pt2, 0, type, 0, nth, 0, null);
          msg = (o == null ? ""
              : o instanceof Map ? SV.getVariable(o).asString() : o.toString());
        }
        break;
      }
      break;
    case T.vanderwaals:
      VDW vdwType = null;
      if (slen > 2) {
        len = slen;
        vdwType = VDW.getVdwType(paramAsStr(2));
        if (vdwType == null)
          invArg();
      }
      if (!chk)
        msg = vwr.getDefaultVdwNameOrData(0, vdwType, null);
      break;
    case T.function:
      eval.checkLength23();
      len = slen;
      String s = eval.optParameterAsString(2);
      int pt;
      if (filter == null && (pt = s.indexOf('/')) >= 0) {
        filter = s.substring(pt + 1);
        s = s.substring(0, pt);
      }
      if (!chk)
        msg = vwr.getFunctionCalls(s);
      break;
    case T.set:
      checkLength(2 + filterLen);
      if (!chk)
        msg = vwr.getAllSettings(null);
      break;
    case T.title:
      msg = vwr.getFrameTitle();
      break;
    case T.url:
      // in a new window
      if ((len = slen) == 2) {
        if (!chk)
          vwr.showUrl(eval.getFullPathName(false));
      } else {
        name = paramAsStr(2);
        if (!chk)
          vwr.showUrl(name);
      }
      return;
    case T.color:
      str = "defaultColorScheme";
      break;
    case T.scale3d:
      str = "scaleAngstromsPerInch";
      break;
    case T.quaternion:
    case T.ramachandran:
      if (chk)
        return;
      int modelIndex = vwr.am.cmi;
      if (modelIndex < 0)
        eval.errorStr(ScriptError.ERROR_multipleModelsDisplayedNotOK,
            "show " + eval.theToken.value);
      msg = plot(st);
      len = slen;
      break;
    case T.context:
    case T.trace:
      if (!chk)
        msg = getContext(false);
      break;
    case T.colorscheme:
      name = eval.optParameterAsString(2);
      if (name.length() > 0)
        len = 3;
      if (!chk)
        value = vwr.cm.getColorSchemeList(name);
      break;
    case T.variables:
      if (!chk)
        msg = vwr.getAtomDefs(vwr.definedAtomSets) + vwr.g.getVariableList()
            + getContext(true);
      break;
    case T.trajectory:
      if (!chk)
        msg = vwr.getTrajectoryState();
      break;
    case T.historylevel:
      value = "" + ScriptEval.commandHistoryLevelMax;
      break;
    case T.loglevel:
      value = "" + Logger.getLogLevel();
      break;
    case T.debugscript:
      value = "" + vwr.getBoolean(T.debugscript);
      break;
    case T.strandcount:
      msg = "set strandCountForStrands " + vwr.getStrandCount(JC.SHAPE_STRANDS)
          + "; set strandCountForMeshRibbon "
          + vwr.getStrandCount(JC.SHAPE_MESHRIBBON);
      break;
    case T.timeout:
      msg = vwr.showTimeout((len = slen) == 2 ? null : paramAsStr(2));
      break;
    case T.defaultlattice:
      value = Escape.eP(vwr.getDefaultLattice());
      break;
    case T.minimize:
      if (!chk)
        msg = vwr.getMinimizationInfo();
      break;
    case T.axes:
      switch (vwr.g.axesMode) {
      case T.axesunitcell:
        msg = "set axesUnitcell";
        break;
      case T.axesmolecular:
        msg = "set axesMolecular";
        break;
      default:
        msg = "set axesWindow";
      }
      break;
    case T.bondmode:
      msg = "set bondMode " + (vwr.getBoolean(T.bondmodeor) ? "OR" : "AND");
      break;
    case T.strands:
      if (!chk)
        msg = "set strandCountForStrands "
            + vwr.getStrandCount(JC.SHAPE_STRANDS)
            + "; set strandCountForMeshRibbon "
            + vwr.getStrandCount(JC.SHAPE_MESHRIBBON);
      break;
    case T.hbond:
      msg = "set hbondsBackbone " + vwr.getBoolean(T.hbondsbackbone)
          + ";set hbondsSolid " + vwr.getBoolean(T.hbondssolid);
      break;
    case T.spin:
      if (!chk)
        msg = vwr.getSpinState();
      break;
    case T.ssbond:
      msg = "set ssbondsBackbone " + vwr.getBoolean(T.ssbondsbackbone);
      break;
    case T.display:// deprecated
    case T.selectionhalos:
      msg = "selectionHalos " + (vwr.getSelectionHalosEnabled() ? "ON" : "OFF");
      break;
    case T.hetero:
      msg = "set selectHetero " + vwr.getBoolean(T.hetero);
      break;
    case T.addhydrogens:
      msg = Escape.eAP(vwr.getAdditionalHydrogens(null, null,
          AtomCollection.CALC_H_DOALL | AtomCollection.CALC_H_JUSTC));
      break;
    case T.hydrogen:
      msg = "set selectHydrogens " + vwr.getBoolean(T.hydrogen);
      break;
    case T.ambientpercent:
    case T.diffusepercent:
    case T.specular:
    case T.specularpower:
    case T.specularexponent:
    case T.lighting:
      if (!chk)
        msg = vwr.getLightingState();
      break;
    case T.saved:
    case T.save:
      if (!chk)
        msg = vwr.stm.listSavedStates();
      break;
    case T.unitcell:
      if (!chk) {
        if ((len = slen) == 3) {
          msg = vwr.stm.getSavedUnitCell(paramAsStr(2));
        } else {
          msg = vwr.getUnitCellInfoText();
        }
      }
      break;
    case T.coord:
      if ((len = slen) == 2) {
        if (!chk)
          msg = vwr.getCoordinateState(vwr.bsA());
        break;
      }
      String nameC = paramAsStr(2);
      if (!chk)
        msg = vwr.stm.getSavedCoordinates(nameC);
      break;
    case T.state:
      if (!chk && eval.outputBuffer == null && filter == null)
        vwr.sm.clearConsole();
      if ((len = slen) == 2) {
        if (!chk)
          msg = vwr.getStateInfo();
        break;
      }
      if (filter != null && slen == 3) {
        if (!chk)
          msg = vwr.getStateInfo();
        break;
      } else if (tokAt(2) == T.file && (len = slen) == 4) {
        if (!chk)
          msg = vwr.fm.getEmbeddedFileState(paramAsStr(3), true, "state.spt");
        break;
      }
      len = 3;
      name = paramAsStr(2);
      if (!chk)
        msg = vwr.stm.getSavedState(name);
      break;
    case T.structure:
      if ((len = slen) == 2) {
        if (!chk)
          msg = vwr.ms.getProteinStructureState(vwr.bsA(), T.show);
        break;
      }
      String shape = paramAsStr(2);
      if (!chk)
        msg = vwr.stm.getSavedStructure(shape);
      break;
    case T.data:
      String dtype = ((len = slen) == 3 ? paramAsStr(2) : null);
      if (!chk) {
        Object data = vwr.getDataObj(dtype, null,
            JmolDataManager.DATA_TYPE_LAST);
        boolean isType = (dtype != null && "types".equals(dtype.toLowerCase()));
        boolean isAll = (isType || "*".equals(dtype));
        if (data == null) {
          msg = "no data";
        } else if (data instanceof Lst) {
          Lst<String> list = (Lst<String>) data;
          data = list.toArray(new String[list.size()]);
        } else if (!isAll) {
          Object[] odata = (Object[]) data;
          msg = Escape.encapsulateData(
              (String) odata[JmolDataManager.DATA_LABEL],
              odata[JmolDataManager.DATA_VALUE],
              ((Integer) odata[JmolDataManager.DATA_TYPE]).intValue());
        }
        if (msg == null) {
          String[] sdata = (String[]) data;
          if (isType) {
            sdata[1] = sdata[1].replace('\n', ',');
          }
          msg = Escape.e(sdata);
        }
      }
      break;
    case T.dollarsign:
      len = 3;
      msg = eval.setObjectProperty();
      break;
    case T.boundbox:
      if (!chk) {
        msg = vwr.ms.getBoundBoxCommand(true);
      }
      break;
    case T.center:
      if (!chk)
        msg = "center " + Escape.eP(vwr.tm.fixedRotationCenter);
      break;
    case T.draw:
      if (!chk)
        msg = (String) getShapeProperty(JC.SHAPE_DRAW, "command");
      break;
    case T.file:
      // as a string
      if (slen == 2) {
        // show FILE
        if (!chk) {
          if (filter == null)
            vwr.sm.clearConsole();
          msg = e.getCurrentModelFileAsString(null);
        }
        if (msg == null)
          msg = "<unavailable>";
        break;
      }
      len = 3;
      value = paramAsStr(2);
      if (!chk) {
        //show FILE xxx/xxx/xxx
        if (filter == null)
          vwr.sm.clearConsole();
        msg = vwr.getFileAsString3(value, true, null);
      }
      break;
    case T.frame:
      if (tokAt(2) == T.all && (len = 3) > 0)
        msg = vwr.getModelFileInfoAll();
      else
        msg = vwr.getModelFileInfo();
      break;
    case T.history:
      int n = ((len = slen) == 2 ? Integer.MAX_VALUE : intParameter(2));
      if (n < 1)
        invArg();
      if (!chk) {
        vwr.sm.clearConsole();
        if (eval.scriptLevel == 0)
          vwr.removeCommand();
        msg = vwr.getSetHistory(n);
      }
      break;
    case T.isosurface:
      if (!chk)
        msg = (String) getShapeProperty(JC.SHAPE_ISOSURFACE, "jvxlDataXml");
      break;
    case T.nbo:
    case T.mo:
      if (eval.optParameterAsString(2).equalsIgnoreCase("list")) {
        e.sm.loadShape(JC.SHAPE_MO);
        msg = (chk ? "" : (String) getShapeProperty(JC.SHAPE_MO, "list -1"));
        len = 3;
      } else {
        int ptMO = ((len = slen) == 2 ? Integer.MIN_VALUE : intParameter(2));
        if (!chk)
          msg = getMoJvxl(ptMO, tok == T.nbo);
      }
      break;
    case T.model:
      if (!chk)
        msg = vwr.ms.getModelInfoAsString();
      break;
    case T.measurements:
      if (!chk)
        msg = vwr.getMeasurementInfoAsString();
      break;
    case T.best:
      len = 3;
      if (!chk && slen == len) {
        msg = paramAsStr(2);
        msg = vwr
            .getOrientation(
                T.getTokFromName(msg.equals("box") ? "volume"
                    : msg.equals("rotation") ? "best" : msg),
                "best", null, null)
            .toString();
      }
      break;
    case T.rotation:
      tok = tokAt(2);
      if (tok == T.nada)
        tok = T.rotation;
      else
        len = 3;
      //$FALL-THROUGH$
    case T.translation:
    case T.moveto:
      if (!chk)
        msg = vwr.getOrientation(tok, null, null, null).toString();
      break;
    case T.orientation:
      len = 2;
      if (slen > 3)
        break;
      switch (tok = tokAt(2)) {
      case T.translation:
      case T.rotation:
      case T.moveto:
      case T.nada:
        if (!chk)
          msg = vwr.getOrientation(tok, null, null, null).toString();
        break;
      default:
        name = eval.optParameterAsString(2);
        msg = vwr.getOrientation(T.name, name, null, null).toString();
      }
      len = slen;
      break;
    case T.pdbheader:
      if (!chk)
        msg = vwr.ms.getPDBHeader(vwr.am.cmi);
      break;
    case T.pointgroup:
      String typ = eval.optParameterAsString(2);
      if (typ.length() == 0)
        typ = null;
      len = slen;
      if (!chk)
        msg = vwr.ms.getPointGroupAsString(vwr.bsA(), "show:" + typ, 0, 0, null,
            null, null);
      break;
    case T.symmetry:
      if (!chk)
        msg = vwr.ms.getSymmetryInfoAsString();
      break;
    case T.transform:
      if (!chk)
        msg = "transform:\n" + vwr.tm.matrixRotate.toString();
      break;
    case T.zoom:
      msg = "zoom "
          + (vwr.tm.zoomEnabled ? ("" + vwr.tm.getZoomSetting()) : "off");
      break;
    case T.frank:
      msg = (vwr.getShowFrank() ? "frank ON" : "frank OFF");
      break;
    case T.radius:
      str = "solventProbeRadius";
      break;
    // Chime related
    case T.sequence:
      if ((len = slen) == 3 && tokAt(2) == T.off)
        tok = T.group1;
      //$FALL-THROUGH$
    case T.basepair:
    case T.chain:
    case T.residue:
    case T.selected:
    case T.group:
    case T.atoms:
    case T.info:
      //case T.bonds: // ?? was this ever implemented? in Chime?
      if (!chk)
        msg = vwr.getChimeInfo(tok);
      break;
    // not implemented
    case T.echo:
    case T.fontsize:
    case T.help:
    case T.solvent:
      value = "?";
      break;
    case T.mouse:
      String qualifiers = ((len = slen) == 2 ? null : paramAsStr(2));
      if (!chk)
        msg = vwr.getBindingInfo(qualifiers);
      break;
    case T.menu:
      if (!chk)
        value = vwr.getMenu("");
      break;
    case T.identifier:
      if (str.equalsIgnoreCase("fileHeader")) {
        if (!chk)
          msg = vwr.ms.getPDBHeader(vwr.am.cmi);
      }
      break;
    case T.json:
    case T.var:
      str = paramAsStr(len++);
      SV v = (SV) eval.getParameter(str, T.variable, true);
      if (!chk)
        if (tok == T.json) {
          msg = v.toJSON();
        } else {
          msg = v.escape();
        }
      break;
    }
    checkLength(len + filterLen);
    if (chk)
      return;
    if (msg != null)
      showString(filterShow(msg, filter));
    else if (value != null)
      showString(str + " = " + value);
    else if (str != null) {
      if (str.indexOf(" ") >= 0)
        showString(str);
      else
        showString(str + " = "
            + ((SV) eval.getParameter(str, T.variable, true)).escape());
    }
  }

  private String filterShow(String msg, String name) {
    if (name == null)
      return msg;
    boolean isNot = name.startsWith("!/");
    name = name.substring(isNot ? 2 : 1).toLowerCase();
    String[] info = PT.split(msg, "\n");
    SB sb = new SB();
    for (int i = 0; i < info.length; i++)
      if ((info[i].toLowerCase().indexOf(name) < 0) == isNot)
        sb.append(info[i]).appendC('\n');
    return sb.toString();
  }

  private void stereo() throws ScriptException {
    STER stereoMode = STER.DOUBLE;
    // see www.usm.maine.edu/~rhodes/0Help/StereoViewing.html
    // stereo on/off
    // stereo color1 color2 6
    // stereo redgreen 5

    double degrees = TransformManager.DEFAULT_STEREO_DEGREES;
    boolean degreesSeen = false;
    int[] colors = null;
    int colorpt = 0;
    for (int i = 1; i < slen; ++i) {
      if (e.isColorParam(i)) {
        if (colorpt > 1)
          e.bad();
        if (colorpt == 0)
          colors = new int[2];
        if (!degreesSeen)
          degrees = 3;
        colors[colorpt] = e.getArgbParam(i);
        if (colorpt++ == 0)
          colors[1] = ~colors[0];
        i = e.iToken;
        continue;
      }
      switch (getToken(i).tok) {
      case T.on:
        e.checkLast(e.iToken = 1);
        e.iToken = 1;
        break;
      case T.off:
        e.checkLast(e.iToken = 1);
        stereoMode = STER.NONE;
        break;
      case T.integer:
      case T.decimal:
        degrees = doubleParameter(i);
        degreesSeen = true;
        break;
      case T.identifier:
        if (!degreesSeen)
          degrees = 3;
        stereoMode = STER.getStereoMode(paramAsStr(i));
        if (stereoMode != null)
          break;
        //$FALL-THROUGH$
      default:
        invArg();
      }
    }
    if (chk)
      return;
    vwr.setStereoMode(colors, stereoMode, degrees);
  }

  private boolean struts() throws ScriptException {
    ScriptEval eval = e;
    boolean defOn = (tokAt(1) == T.only || tokAt(1) == T.on || slen == 1);
    int mad = eval.getMadParameter();
    if (mad == Integer.MAX_VALUE)
      return false;
    if (defOn)
      mad = (int) Math.round(vwr.getDouble(T.strutdefaultradius) * 2000);
    setShapeProperty(JC.SHAPE_STICKS, "type", Integer.valueOf(Edge.BOND_STRUT));
    eval.setShapeSizeBs(JC.SHAPE_STICKS, mad, null);
    setShapeProperty(JC.SHAPE_STICKS, "type",
        Integer.valueOf(Edge.BOND_COVALENT_MASK));
    return true;
  }

  private void unitcell(int i, final boolean isModelkit)
      throws ScriptException {
    ScriptEval eval = e;
    int icell = Integer.MAX_VALUE;
    int mad10 = Integer.MAX_VALUE;
    T3d pt = null;
    TickInfo tickInfo = eval.tickParamAsStr(i, true, false, false);
    i = eval.iToken;
    String id = null;
    T3d[] oabc = null;
    Object newUC = null;
    String ucname = null;
    boolean isOffset = false;
    boolean isReset = false;
    boolean isPacked = false;
    SymmetryInterface sym = (chk ? null : vwr.getCurrentUnitCell());
    int tok = tokAt(++i);
    switch (tok) {
    case T.none:
      if (!chk)
        vwr.ms.clearUnitCell(vwr.am.cmi);
      return;
    case T.restore:
    case T.reset:
      isReset = true;
      pt = P4d.new4(0, 0, 0, -1); // reset offset and range
      eval.iToken++;
      break;
    case T.surface:
      P4d plane = eval.hklParameter(i + 1, null, false);
      T3d hkl = eval.getPointOrPlane(i + 1, ScriptParam.MODE_P34);
      i = eval.iToken;
      double zscale = Double.NaN;
      if (isFloatParameter(i + 1)) {
        zscale = doubleParameter(++i);
        if (tokAt(i + 1) == T.percent) {
          zscale = -zscale / 100;
          i++;
        }
      }
      double zoffset = Double.NaN;
      boolean zoffPercent = false;
      if (isFloatParameter(i + 1)) {
        zoffset = doubleParameter(++i);
        if (tokAt(i + 1) == T.percent) {
          zoffPercent = true;
          i++;
        }
      }
      boolean refTop = (tokAt(i + 1) == T.top);
      if (refTop)
        i++;
      eval.iToken = i;
      if (chk)
        break;
      if (hkl instanceof P3d) {
        hkl = P4d.new4(hkl.x, hkl.y, hkl.z, 0);
      } else if (Double.isNaN(zoffset)) {
        zoffset = ((P4d) hkl).w;
      }
      oabc = getUVBoxFromHKL(sym, (P4d) hkl, plane);
      P3d p = new P3d();
      V3d vt = new V3d();
      MeasureD.getPlaneProjection(new P3d(), plane, p, vt);
      if (zscale > 0) {
        oabc[3].normalize();
        oabc[3].scale(zscale);
      } else if (zscale < 0) {
        oabc[3].scale(-zscale);
      }

      if (!Double.isNaN(zoffset) && zoffset != 0) {
        // o ---pdist-----vt-------> p
        // o - offset--o'---------> p
        if (zoffPercent) {
          zoffset = zoffset / 100 * oabc[3].length();
        }
        oabc[0].scaleAdd2(zoffset, vt, oabc[0]);
      }
      if (refTop)
        oabc[0].scaleAdd2(-oabc[3].length(), vt, oabc[0]);
      ucname = "surface" + ((int) hkl.x) + ((int) hkl.y) + ((int) hkl.z);
      break;
    case T.scale:
    case T.supercell:
      pt = (tok == T.scale
          ? eval.getPointOrPlane(++i,
              ScriptParam.MODE_P3 | ScriptParam.MODE_P_ALLOW_FRACTIONAL)
          : eval.checkHKL(eval.getFractionalPoint(++i)));
      i = eval.iToken;
      if (chk)
        break;
      oabc = sym.getUnitCellVectors();
      oabc[1].scale(Math.abs(pt.x));
      oabc[2].scale(Math.abs(pt.y));
      oabc[3].scale(Math.abs(pt.z));
      pt = null;
      ucname = "supercell";
      break;
    case T.fill:
      pt = eval.getFractionalPoint(++i);
      i = eval.iToken;
      if (chk)
        break;
      pt = SimpleUnitCell.ptToIJK(eval.checkHKL(pt), 1);
      break;
    case T.isosurface:
    case T.dollarsign:
      id = eval.objectNameParameter(++i);
      break;
    case T.boundbox:
      eval.iToken = i;
      if (chk)
        break;
      P3d o = P3d.newP(vwr.getBoundBoxCenter());
      pt = vwr.getBoundBoxCornerVector();
      o.sub(pt);
      oabc = new P3d[] { o, P3d.new3(pt.x * 2, 0, 0), P3d.new3(0, pt.y * 2, 0),
          P3d.new3(0, 0, pt.z * 2) };
      pt = null;
      break;
    case T.transform:
      if (tokAt(++i) != T.matrix4f)
        invArg();
      if (chk)
        break;
      newUC = new Object[] { getToken(i).value };
      break;
    case T.matrix3f:
    case T.matrix4f:
      newUC = getToken(i).value;
      break;
    case T.center:
      switch (tokAt(++i)) {
      case T.define:
      case T.bitset:
      case T.expressionBegin:
        pt = vwr.ms.getAtomSetCenter(atomExpressionAt(i));
        i = eval.iToken;
        if (chk)
          break;
        vwr.toFractional(pt, true);
        break;
      default:
        if (eval.isCenterParameter(i)) {
          pt = centerParameter(i);
          i = eval.iToken;
          break;
        }
        invArg();
      }
      pt.x -= 0.5d;
      pt.y -= 0.5d;
      pt.z -= 0.5d;
      break;
    case T.define:
    case T.bitset:
    case T.expressionBegin:
      int iAtom = atomExpressionAt(i).nextSetBit(0);
      i = eval.iToken;
      if (!chk)
        vwr.setUnitCellAtomIndex(iAtom);
      if (iAtom < 0)
        return;
      break;
    case T.offset:
      isOffset = true;
      if (eval.isAtomExpression(i + 1)) {
        pt = eval.centerParameter(++i, null);
        i = eval.iToken;
        if (chk)
          break;
        vwr.toFractional(pt, false);
        break;
      }
      //$FALL-THROUGH$
    case T.range:
      pt = eval.getFractionalPoint(++i);
      if (pt instanceof P4d) {
        if (isOffset)
          invArg();
      } else {
        i = eval.iToken;
        if (chk)
          break;
        if (!isOffset && pt.x < 555) {
          pt = SimpleUnitCell.ptToIJK(eval.checkHKL(pt), 1);
        } else {
          pt = P4d.new4(pt.x, pt.y, pt.z, (isOffset ? 1 : 0));
        }
      }
      break;
    case T.decimal:
    case T.integer:
      double f = doubleParameter(i);
      if (f < 111) {
        // diameter
        i--;
        break;
      }
      icell = intParameter(i);
      break;
    case T.spin:
    case T.string:
    case T.identifier:
      String s = paramAsStr(i).toLowerCase();
      if (s.equals("rhombohedral") || s.equals("r")) {
        if (sym != null
            && sym.getUnitCellInfoType(SimpleUnitCell.INFO_IS_HEXAGONAL) == 0)
          return;
        s = SimpleUnitCell.HEX_TO_RHOMB;
      } else if (s.equals("trigonal") || s.equals("hexagonal")) {
        if (sym != null && sym
            .getUnitCellInfoType(SimpleUnitCell.INFO_IS_RHOMBOHEDRAL) == 0)
          return;
        s = SimpleUnitCell.RHOMB_TO_HEX;
      }
      ucname = s;
      if (s.indexOf(",") >= 0 || chk) {
        newUC = s;
        break;
      }
      String stype = null;
      // parent, standard, conventional, primitive
      vwr.ms.setModelCagePts(-1, null, null);
      // reset -- presumes conventional, so if it is not, 
      // _M.unitcell_conventional must be set in the reader.

      newUC = vwr.getModelInfo(JC.INFO_UNIT_CELL_CONVENTIONAL);
      // If the file read was loaded as primitive, 
      // newUC will be a T3[] indicating the conventional.
      boolean modelIsPrimitive = vwr.getModelInfo("isprimitive") != null;
      if (PT.isOneOf(ucname, ";parent;standard;primitive;")) {
        if (newUC == null && modelIsPrimitive) {
          showString(
              "Cannot convert unit cell when file data is primitive and have no lattice information");
          return;
        }
        // unitcell primitive "C"
        if (ucname.equals("primitive") && tokAt(i + 1) == T.string)
          stype = paramAsStr(++i).toUpperCase();
      }
      if (newUC instanceof T3d[]) {
        // from reader -- getting us to conventional
        oabc = (T3d[]) newUC;
      }
      if (stype == null)
        stype = (String) vwr.getModelInfo("latticeType");
      if (newUC != null) {
        vwr.ms.setModelCagePts(-1, vwr.getV0abc(-1, newUC), "" + newUC);
        newUC = null;
      }
      // now guaranteed to be "conventional"
      if (!ucname.equals("conventional")) {
        setShapeProperty(JC.SHAPE_AXES, "labels", null);
        s = (String) vwr.getModelInfo("unitcell_" + ucname);
        if (s == null) {
          boolean isPrimitive = ucname.equals("primitive");
          if (isPrimitive || ucname.equals("reciprocal")) {
            double scale = (slen == i + 1 ? 1
                : tokAt(i + 1) == T.integer ? intParameter(++i) * Math.PI
                    : doubleParameter(++i));
            ucname = (sym == null ? "" : sym.getSpaceGroupName() + " ")
                + ucname;
            oabc = (sym == null
                ? new P3d[] { P3d.new3(0, 0, 0), P3d.new3(1, 0, 0),
                    P3d.new3(0, 1, 0), P3d.new3(0, 0, 1) }
                : sym.getUnitCellVectors());
            if (stype == null)
              stype = (String) vwr.getSymmetryInfo(
                  vwr.getFrameAtoms().nextSetBit(0), null, 0, null, null, null,
                  T.lattice, null, 0, -1, 0, null);
            if (sym == null)
              sym = vwr.getSymTemp();
            if (!modelIsPrimitive)
              sym.toFromPrimitive(true,
                  stype.length() == 0 ? 'P' : stype.charAt(0), oabc,
                  (M3d) vwr.getCurrentModelAuxInfo().get("primitiveToCrystal"));
            if (!isPrimitive) {
              SimpleUnitCell.getReciprocal(oabc, oabc, scale);
            }
            break;
          }
        } else {
          ucname = s;
          if (s.indexOf(",") >= 0)
            newUC = s;
        }
      }
      break;
    default:
      Object[] ret = new Object[1];
      if (getUnitCellParameter(i, ret)) {
        oabc = (T3d[]) ret[0];
      } else {
        newUC = ret[0];
      }
      if (newUC == null && oabc == null) {
        if (slen > i + 1) {
          pt = eval.getFractionalPoint(i);
          i = eval.iToken;
        } else {
          // backup for diameter
          i--;
        }
      } else if (newUC != null) {
        if (!chk && isModelkit) {
          if (sym == null) {
            assignSpaceGroup(null, "P1", newUC, false, false, "unitcell");
          } else if (sym.fixUnitCell((double[]) newUC)) {
            eval.invArgStr(
                "Unit cell is incompatible with current space group");
          }
        }
        i = eval.iToken;
      } else if (oabc != null) {
        i = eval.iToken;
      }
      break;
    }
    if (isModelkit && tokAt(i + 1) == T.packed) {
      isPacked = true;
      i = ++eval.iToken;
    }
    mad10 = eval.getSetAxesTypeMad10(++i);
    eval.checkLast(eval.iToken);
    if (chk || mad10 == Integer.MAX_VALUE)
      return;
    if (oabc == null && newUC != null)
      oabc = vwr.getV0abc(-1, newUC);
    if (icell != Integer.MAX_VALUE) {
      // icell e.g. 555
      if (sym != null)
        sym.setOffset(icell);
    } else if (id != null) {
      vwr.setCurrentCage(id);
    } else if (isReset || oabc != null) {
      isReset = (isModelkit || sym == null
          ? vwr.getModelkit(false).transformAtomsToUnitCell(sym, oabc, ucname)
          : true);
      if (isReset) {
        if (isModelkit && sym != null) {
          vwr.ms.setSpaceGroup(vwr.am.cmi, sym.getUnitCell(oabc, false, null),
              null);
          return;
        }
        vwr.ms.setModelCagePts(-1, oabc, ucname);
        if (sym == null) {
          vwr.setNewRotationCenter(vwr.getUnitCellCenter());
        }
      }
    }
    SymmetryInterface sym0 = (isModelkit ?  vwr.ms.getUnitCell(-1 - vwr.am.cmi) : null);
    eval.setObjectMad10(JC.SHAPE_UCCAGE, "unitCell", mad10);
    if (pt != null) {
      sym = vwr.ms.getUnitCell(Integer.MIN_VALUE);
      if (sym != null)
        sym.setOffsetPt(pt);
    }
    if (isModelkit && sym.getFractionalOffset(true) == null) {
      assignSpaceGroup(vwr.getModelUndeletedAtomsBitSet(vwr.am.cmi),
          sym0.getSpaceGroupClegId(), null, isPacked, false, e.fullCommand);
    }
    if (tickInfo != null)
      setShapeProperty(JC.SHAPE_UCCAGE, "tickInfo", tickInfo);
    if (ucname != null)
      showString(ucname + (oabc == null ? "" : " " + Escape.e(oabc)));
  }

  /**
   * create a uvw-space unit cell from an HKL plane
   * 
   * @param uc
   * @param hkl
   * @param plane
   * @return [o a b c]
   * 
   * @throws ScriptException
   * 
   */
  private P3d[] getUVBoxFromHKL(SymmetryInterface uc, P4d hkl, P4d plane)
      throws ScriptException {
    int h = (int) hkl.x;
    int k = (int) hkl.y;
    int l = (int) hkl.z;

    if (h == 0 && k == 0 && l == 0 || h != hkl.x || k != hkl.y || l != hkl.z)
      invArg();

    // reduce {2 2 2} to {1 1 1}
    while ((h % 2) == 0 && (k % 2) == 0 && (l % 2) == 0) {
      h /= 2;
      k /= 2;
      l /= 2;
    }
    P3d[] oabc = uc.getUnitCellVectors();
    double dist0 = plane.w;
    P4d p0 = P4d.new4(plane.x, plane.y, plane.z, 0);
    int max = Math.max(Math.max(h, k), l);
    // had 0 for h or k or l 0, but that can miss some
    // possibilities when max is large. Maybe there is 
    // a better range here, but I'm not seeing it.
    //
    Lst<P3d> cpts = MeasureD.getLatticePoints(uc.getLatticeCentering(),
        //        (h == 0 ? 0 : max), (k == 0 ? 0 : max), (l == 0 ? 0 : max));
        max, max, max);
    for (int j = cpts.size(); --j >= 0;) {
      uc.toCartesian(cpts.get(j), true);
    }
    cpts = MeasureD.getPointsOnPlane(cpts.toArray(new P3d[cpts.size()]), p0);
    P3d zero = new P3d();
    double amin = -179;
    double dmin = Double.MAX_VALUE, dmin2 = Double.MAX_VALUE;
    P3d v = null, v1 = null;
    double da = amin, damin = Double.MAX_VALUE, d;
    boolean okAng, isNew, isLinear;
    for (int i = cpts.size(); --i >= 0;) {
      P3d pt = cpts.get(i);
      d = pt.length();
      isNew = isLinear = false;
      //      boolean checkAngle = false;
      // check for 0 or linear
      if (d < 0.01d || v != null && ((da = Math
          .abs(MeasureD.computeTorsion(v, zero, plane, pt, true))) > -amin))
        continue;
      isLinear = (da < 1);
      if (v == null) {
        // load v
        v = pt;
        dmin = d;
        continue;
      }
      if (d < dmin - 0.01d) {
        isNew = true;
        damin = Double.MAX_VALUE;
        da = Double.MAX_VALUE;
      } else if (d < dmin2 - 0.01d) {
        damin = Double.MAX_VALUE;
        // new v1
      } else if (d >= dmin2 + 0.01d) {
        // hexagonal
        //        checkAngle = true;
      } else {
        continue;
      }
      if (isNew || (okAng = (da > 89)) && d < dmin + 0.1d
          && (pt.x >= -0.01d && pt.y <= 0.01d)
          && (damin == Double.MAX_VALUE || damin < 89)) {
        // special first-quadrant check
        if (isLinear) {
          // discard v1
          v1 = null;
          dmin2 = Double.MAX_VALUE;
        } else {
          // replace v2 with v
          v1 = v;
          dmin2 = dmin;
        }
        v = pt;
        dmin = d;
        if (!isNew && da < damin)
          damin = da;
      } else if (okAng && d < dmin2 + 0.1d) {
        v1 = pt;
        dmin2 = d;
        if (da < damin)
          damin = da;
      }
    }
    if (v == null)
      invArg();
    P3d u = null;
    if (v1 == null) {
      for (int i = cpts.size(); --i >= 0;) {
        P3d pt = cpts.get(i);
        da = MeasureD.computeTorsion(v, zero, plane, pt, true);
        if (da < -89.9d && da > amin + 0.01d) {
          amin = da;
          u = pt;
        }
      }
    } else {
      da = MeasureD.computeTorsion(v, zero, plane, v1, true);
      if (da > 1) {
        u = v;
        v = v1;
      } else {
        u = v1;
      }
    }
    if (u == null)
      invArg();
    oabc[1] = u;
    oabc[2] = v;
    // odd number of negatives -- reverse
    boolean doReverse = ((((h < 0 ? 1 : 0) + (k < 0 ? 1 : 0) + (l < 0 ? 1 : 0))
        % 2) == 1);
    P3d w = oabc[3];
    P3d a = P3d.newP(u);
    P3d b = P3d.newP(v);
    if (h == 0 && k == 0) {
      // 001
      // no a,b intercept
      b.scale(-1);
      a.scale(-1);
      oabc[1] = a;
      oabc[2] = b;
    } else if (h == 0 && l == 0) {
      // 010
      // no a,c intercept
      //      oabc[1] = b;
      //      oabc[2] = a;
    } else if (k == 0 && l == 0) {
      //100
      // no b,c intercept
      a.scale(-1);
      b.scale(-1);
      oabc[1] = b;
      oabc[2] = a;
    }
    if (doReverse) {
      a = oabc[1];
      oabc[1] = oabc[2];
      oabc[2] = a;
    }
    w.cross(v, u);
    w.normalize();
    w.scale(dist0);
    return oabc;
  }

  /**
   * Configure the ModelKitPopup for Crystallographic symmetry viewing and
   * structure editing
   * 
   * new 14.29.45
   * 
   * see modelkit.ModelKitPopup.java
   *
   * 
   * @throws ScriptException
   */
  private void modelkit() throws ScriptException {

    // modelkit [ON(nada)/OFF/DISPLAY/HIDE]
    //    
    //  MODELKIT CENTER point or atoms (point can be fractional by adding "/1" to at least one coord)
    //
    //  -- action options include alternatives to the given commands (assign is undocumented)
    //
    //  modelkit ELEMENTKEY ON/OFF   element key
    //  modelkit MINIMIZE
    //
    //  modelkit ASSIGN ATOM [symbol|pl|mi] point
    //  modelkit ASSIGN ATOM @1 [symbol|pl|mi]
    //  modelkit ASSIGN ATOM @1 [symbol|pl|mi] point
    //  modelkit ASSIGN BOND (integer) [0,1,2,3,4,5,p,m] (default P)
    //  modelkit ASSIGN BOND {atom1 atom2} [0,1,2,3,4,5,p,m] (default P)
    //  modelkit ASSIGN BOND @1 @2 [0,1,2,3,4,5,p,m] (default P)
    //  modelkit ASSIGN SPACEGROUP    
    //  modelkit CONNECT @1 @2 [0,1,2,3,4,5,p,m] (default 1)
    //  modelkit DELETE @1
    //  modelkit ADD @1 
    //  modelkit ADD @1 "C"
    //  modelkit ADD @1 "C" point (bonding)
    //  modelkit ADD "C" point
    //  modelkit ADD "C" WYCKOFF a
    //  modelkit ADD "C" WYCKOFF G
    //  modelkit MOVETO @1 point
    //  modelkit FIXED VECTOR pt1 pt2
    //  modelkit FIXED PLANE pt1 pt2
    //  modelkit FIXED NONE
    //  modelkit ROTATE ...  (same as ROTATE for example, ROTATE BOND @1 @2 degrees)
    //  modelkit ROTATESELECTED (same as ROTATESLECTED)
    //
    // view mode     
    //  MODELKIT SYMOP [n]
    //  MODELKIT SYMOP "x,-y,z"
    //  MODELKIT SYMOP [[4x4 matrix]]
    //  MODELKIT OFFSET [{i j k}/NONE]
    //
    //  MODELKIT VIBRATION WYCKOFF|OFF
    //
    //  -- configuration options include the following; CAPS is default:
    //       
    //  modelkit SET KEY ON/OFF
    //  MODELKIT SET addHydrogens [TRUE|false]
    //  MODELKIT SET autobond [true|FALSE]
    //  MODELKIT SET clickToSetElement [TRUE|false]
    //  MODELKIT SET showSymopInfo [TRUE|false]
    //  MODELKIT SET bondtype [0,1,2,3,4,5,p,m]
    //  MODELKIT SET element [name or symbol]
    //  MODELKIT SET MODE [molecular|view|edit]
    //  MODELKIT SET UNITCELL [EXTEND|PACKED]
    //
    // edit mode -- not implemented
    //
    //  MODELKIT SET SYMMETRY [APPLYFULL|APPLYLOCAL|RETAINLOCAL] // not implemented
    //  MODELKIT POINT point or atoms // not implemented
    //    
    //  -- configuration options can be given sequentially within one MODELKIT command or in individual commands
    //  -- examples:
    //  
    //  modelkit set mode view symop 5 center @3 
    //  modelkit set mode view symop 5 center @3 offset {0 0 0}   // unitized [0,1)    

    int i = 0;
    int tok = tokAt(1);
    switch (tok) {
    case T.undo:
    case T.redo:
      if (chk)
        e.cmdUndoRedo(tok == T.undo ? T.undomove : T.redomove);
      return;
    case T.off:
      if (e.isStateScript)
        vwr.getModelkit(false).setProperty(JC.MODELKIT_UDPATE_KEY_STATE, null);
      // don't continue if the model kit does not exist and this is MODELKIT OFF by itself
      if (slen == 2 && vwr.getModelkitPropertySafely(JC.MODELKIT_EXISTS) == null)
        return;
      //$FALL-THROUGH$
    case T.nada:
    case T.on:
      if (!chk)
        vwr.setBooleanProperty("modelkitmode", tok != T.off);
      if (tokAt(i + 1) == T.nada)
        return;
      i = ++e.iToken;
      break;
    case T.minimize:
      if (vwr.am.cmi < 0)
        e.errorStr(ScriptError.ERROR_multipleModelsDisplayedNotOK,
            "MODELKIT MINIMIZE");
      minimize(true);
      return;
    case T.rotate:
    case T.rotateSelected:
      e.cmdRotate(false, true);
      return;
    case T.assign:
      ++e.iToken;
      //$FALL-THROUGH$
    case T.spacegroup:
    case T.connect:
    case T.delete:
    case T.add:
    case T.moveto:
    case T.packed:
    case T.zap:
    case T.draw:
      assign(true);
      return;
    case T.mutate:
      ++e.iToken;
      mutate();
      return;
    }
    ModelKit kit = vwr.getModelkit(false);
    while ((tok = tokAt(++i)) != T.nada) {
      String key = paramAsStr(i).toLowerCase();
      Object value = null;
      switch (tok) {
      case T.on:
      case T.off:
        if (!chk) {
          vwr.setBooleanProperty("modelkitmode", tok == T.on);
          if (tok == T.off)
            vwr.setStringProperty("picking", "identify");
        }
        continue;
      case T.display:
      case T.hide:
        key = "hidden";
        value = Boolean.valueOf(tok != T.display);
        break;
      case T.fixed:
        key = "constraint";
        value = "";
        int type = tokAt(++i);
        T3d v1 = null, v2 = null;
        P4d plane = null;
        switch (type) {
        case T.off:
        case T.none:
          if (!chk)
            vwr.setBooleanProperty("dragPicking", false);
          break;
        case T.vector:
          v1 = e.getPoint3f(++i, true, true);
          i = e.iToken;
          v2 = e.getPoint3f(++i, true, true);
          value = null;
          break;
        case T.hkl:
          plane = e.hklParameter(++i, null, true);
          value = null;
          break;
        case T.plane:
          plane = e.planeParameter(++i, false);
          value = null;
          break;
        default:
          invArg();
        }
        if (value == null) {
          if ((v1 == null || v2 == null) == (plane == null))
            invArg();
          value = new Object[] { v1, v2, plane };
        } else {
          value = null;
        }
        i = e.iToken;
        break;
      case T.set:
        // only for T/F 
        key = paramAsStr(++i);
        value = (tokAt(++i) == T.nada ? "true" : paramAsStr(i));
        break;
      case T.vibration:
        key = "vibration";
        switch (tokAt(++i)) {
        case T.off:
          value = "off";
          break;
        case T.wyckoff:
          value = "wyckoff";
          break;
        default:
          invArg();
          break;
        }
        break;
      case T.symop:
        switch (tokAt(++i)) {
        case T.string:
        case T.none:
          value = paramAsStr(i);
          break;
        case T.matrix4f:
          value = getToken(i).value;
          break;
        case T.integer:
          value = Integer.valueOf(getToken(i).intValue);
          break;
        default:
          if (e.isCenterParameter(i)) {
            key = "center";
            value = e.centerParameter(i, null);
            i = e.iToken;
          } else {
            invArg();
          }
          break;
        }
        break;
      case T.offset:
        value = paramAsStr(i + 1);
        if (value.equals("none")) {
          ++i;
          break;
        }
        //$FALL-THROUGH$
      case T.point:
        // not implemented -- "spherePoint"
      case T.center:
        value = e.atomCenterOrCoordinateParameter(++i, null);
        i = e.iToken;
        break;
      case T.mode:
        value = paramAsStr(++i).toLowerCase();
        if (!vwr.isModelKitOption('M', (String) value))
          invArg();
        break;
      case T.unitcell:
        if (e.isArrayParameter(i + 1)) {
          unitcell(2, true);
          return;
        }
        value = (tokAt(i + 1) == T.string ? paramAsStr(++i).toLowerCase() : null);
        if (value == null || !vwr.isModelKitOption('U', (String) value)) {
          unitcell(2, true);
          return;
        }
        break;
      case T.symmetry:
        // undocumented -- deprecated -- use SET SYMMETRY
        value = paramAsStr(++i).toLowerCase();
        if (!vwr.isModelKitOption('S', (String) value))
          invArg();
        break;
      default:
        if (vwr.isModelKitOption('B', key)) {
          value = Boolean.valueOf((tok = tokAt(++i)) == T.nada || tok == T.on);
          break;
        }
        if (vwr.isModelKitOption('M', key)) {
          value = key;
          key = "mode";
          break;
        }
        if (vwr.isModelKitOption('U', key)) {
          value = key;
          key = "unitcell";
          break;
        }
        invArg();
      }
      if (!chk && value != null && (value = kit.setProperty(key, value)) != null
          && key != "hidden" && !kit.isHidden())
        vwr.showString("modelkit SET " + key + " " + value.toString(), false);
    }
  }

  /**
   * Though a command, not documented. Use the MODELKIT command instead
   * 
   * @param isModelkit
   * 
   * @throws ScriptException
   */
  private void assign(boolean isModelkit) throws ScriptException {
    // [modelkit] assign...
    // modelkit connect
    /**
     * final parameter TRUE is only for internal use so that we can determine if
     * this command should be handled specially because it is an actual click of
     * the mouse and some other feature is involved.
     */
    // single model only
    if (vwr.am.cmi < 0)
      invArg();
    BS bsModelAtoms = vwr.getThisModelAtoms();
    int i = ++e.iToken;
    boolean doDraw = false;
    boolean isZap = false;
    int mode = tokAt(i);
    if (mode == T.draw) {
      // MODELKIT DRAW SPACEGROUP [name or number]
      // different from DRAW SPACEGROUP in that it 
      // does a ZAP and allows for specifying the space grroup
      // modelkit draw spacegroup -- this will ZAP
      if (tokAt(2) != T.spacegroup)
        invArg();
      doDraw = true;
      mode = T.zap;
    }
    if (mode == T.zap) {
      if (!isModelkit)
        invArg();
      isZap = true;
      mode = tokAt(++i);
      if (mode == T.nada)
        return;
    }
    // mode might be reassigned; these next assignments are just for sanity.
    boolean isAtom = (mode == T.atoms);
    boolean isBond = (mode == T.bonds);
    boolean isAdd = (mode == T.add);
    boolean isConnect = (mode == T.connect);
    boolean isDelete = (mode == T.delete);
    boolean isMove = (mode == T.moveto);
    boolean isSpacegroup = (mode == T.spacegroup);
    boolean isPacked = (mode == T.packed);
    // default is ATOM
    if ((isPacked || isSpacegroup) && !isModelkit) {
      // only options for ASSIGH are ATOMS, BONDS, ADD, CONNECT, DELETE, MOVE
      invArg();
    }
    if (isAtom || isBond || isConnect || isSpacegroup || isDelete || isMove
        || isAdd || isPacked) {
      i++;
    } else {
      isAtom = true;
      mode = T.atoms;
    }
    // initialize some modes
    int index = -1, index2 = -1;
    BS bs = null;
    if (isBond) {
      if (tokAt(i) == T.integer) {
        index = e.intParameter(i);
      } else if (getToken(i).value instanceof BondSet) {
        index = ((BS) getToken(i).value).nextSetBit(0);
      } else {
        bs = expFor(i, bsModelAtoms);
        index = bs.nextSetBit(0);
        switch (bs.cardinality()) {
        case 1:
          // assign BOND @3 @4 ...
          bs = expFor(i = ++e.iToken, bsModelAtoms);
          index2 = bs.nextSetBit(0);
          if (index2 < 0)
            invArg();
          bs.set(index);
          //$FALL-THROUGH$
        case 2:
          // assign BOND ({3 4}) ...
          bs = vwr.ms.getBondsForSelectedAtoms(bs, false);
          if (!bs.isEmpty()) {
            // bonded already
            index = bs.nextSetBit(0);
          } else {
            // was not bonded already
            isConnect = true;
            mode = T.connect;
          }
          break;
        case 0:
        default:
          invArg();
        }
      }
      i = ++e.iToken;
    } else if (isPacked) {
      // new Jmol 14.32.73
      bs = bsModelAtoms;
    } else if (isAtom && tokAt(i) == T.string || mode == T.add) {
      // new Jmol 14.29.28
      // assign ATOM "C" {0 0 0}
      // assign ADD "C" {0 0 0}
    } else if (!isSpacegroup || e.isAtomExpression(i)) {
      // assign ATOM @3 "C" {0 0 0}
      // assign CONNECT @3 @4
      // assign DELETE @1
      // assign MOVETO @1
      bs = expFor(i, bsModelAtoms);
      index = bs.nextSetBit(0);
      if (index < 0) {
        return;
      }
      i = ++e.iToken;
    }
    // parse easch mode in full
    SymmetryInterface sym = vwr.getOperativeSymmetry();
    String type = null;
    P3d pt = null;
    String wyckoff = null;
    Object paramsOrUC = null;
    P3d[] pts = null;
    if (isAdd) {
      if (e.isAtomExpression(i)) {
        bs = expFor(++e.iToken, bsModelAtoms);
        i = e.iToken;
        type = e.optParameterAsString(i + 1);
        i = e.iToken;
        if (type.toLowerCase().equals("packed")) {
          isPacked = true;
          type = "";
        }
      } else if (e.tokAt(i) == T.wyckoff) {
        e.iToken = i - 1;
        type = "O"; // general position is red
      } else {
        type = e.optParameterAsString(i);
      }
      if (e.isPoint3f(e.iToken + 1)) {
        pt = getPoint3f(++e.iToken, true);
      } else if (e.isArrayParameter(e.iToken + 1)) {
        pts = e.getPointArray(e.iToken + 1, -1, false, true);
      }
      if (type.length() == 0)
        type = null;
      switch (tokAt(e.iToken + 1)) {
      case T.packed:
        isPacked = true;
        ++e.iToken;
        break;
      case T.wyckoff:
        ++e.iToken;
        wyckoff = paramAsStr(++e.iToken);
        char w = (wyckoff.equalsIgnoreCase("all") ? 'L' : wyckoff.equalsIgnoreCase("general") ? 'G'
            : (wyckoff.length() > 1 || wyckoff.length() == 0 ? '\0'
                : wyckoff.charAt(0)));
        if (w < 'a' && w != 'A' && w != 'G' && w != 'L' || w > 'z')
          invArg();
        wyckoff = "" + w;
        if ("packed".equals(e.optParameterAsString(e.iToken + 1))) {
          isPacked = true;
          ++e.iToken;
        }
        break;
      }
    } else if (isMove) {
      if (e.isArrayParameter(i)) {
        pts = e.getPointArray(i, -1, false, true);
      } else {
        pt = getPoint3f(i, true);
      }
      i = e.iToken;
    } else if (isSpacegroup) {
      type = e.optParameterAsString(i);
      if (type.length() == 0)
        type = ".";
      sym = vwr.getOperativeSymmetry();
      if (type.equals(".")) {
        if (sym == null)
          type = "P1";
        else
          type = (i == slen ? null : sym.getSpaceGroupClegId());
      }
      if ("packed".equalsIgnoreCase(type)) {
        // allow for MODELKIT SPACEGROUP packed
        isPacked = true;
        type = sym.getSpaceGroupClegId();
      }
      // new 16.2.1/2
      if (tokAt(e.iToken + 1) == T.unitcell) {
        Object[] ret = new Object[1];
        getUnitCellParameter(e.iToken + 2, ret);
        paramsOrUC = ret[0];
        if (paramsOrUC == null)
          invArg();
      }
      if (!isPacked && tokAt(e.iToken + 1) == T.packed) {
        isPacked = true;
        ++e.iToken;
      }
    } else if (isConnect) {
      // could be from assign BOND
      if (index2 < 0) {
        // NOT assign BOND @3 @4 ...
        // assign CONNECT @3 @4
        bs = expFor(i, bsModelAtoms);
        index2 = bs.nextSetBit(0);
        type = e.optParameterAsString(++e.iToken);
      }
    } else {
      // assign atom ... 
      // assign delete ...
      // more?
      type = e.optParameterAsString(i);
      if (isAtom)
        pt = (++e.iToken < slen ? centerParameter(e.iToken) : null);
    }
    if (chk)
      return;
    // finally, preserve the state and carry out the process
    vwr.pushState();
    switch (mode) {
    case T.atoms:
      e.clearDefinedVariableAtomSets();
      vwr.getModelkit(false).cmdAssignAtom(bs, pt, type, e.fullCommand);
      break;
    case T.bonds:
      vwr.getModelkit(false).cmdAssignBond(index, (type + "-").charAt(0),
          e.fullCommand);
      break;
    case T.connect:
      vwr.getModelkit(false).cmdAssignConnect(index, index2,
          (type + "1").charAt(0), e.fullCommand);
      break;
    case T.packed:
      type = null;
      //$FALL-THROUGH$
    case T.add:
    case T.wyckoff:
      if (pts == null) {
        if (pt != null) {
          pts = new P3d[] { pt };
        } else if (bs == null && wyckoff == null) {
          // defaults to wyckoff position G
          if (type == null)
            invArg();
          else
            wyckoff = "G";
        }
      }
      int na = vwr.getModelkit(false).cmdAssignAddAtoms(
          type + (wyckoff != null ? ":" + wyckoff : ""), pts, bs,
          (isPacked ? "packed" : ""), e.fullCommand);
      if (e.doReport())
        e.report(GT.i(GT.$("{0} atoms added"), na), false);
      break;
    case T.delete:
      int nd = vwr.getModelkit(false).cmdAssignDeleteAtoms(bs);
      if (e.doReport())
        e.report(GT.i(GT.$("{0} atoms deleted"), nd), false);
      break;
    case T.moveto:
      // allow projection using this command if ends with TRUE
      int nm = vwr.getModelkit(false).cmdAssignMoveAtoms(bs, index,
          P3d.newP(pt), pts, true, false);
      if (nm > 0)
        vwr.checkCoordinatesChanged(bs);
      if (e.doReport())
        e.report(GT.i(GT.$("{0} atoms moved"), nm), false);
      break;
    case T.spacegroup:
      // "ITA" "/" 12.1
      // "ITA"
      // note: "ITA/" is now implied; adding it makes no differernce
      //        If the Jmol id is desired, it will have a colon in it. 
      if (isZap) {
        vwr.zap(false, false, false);
        sym = null;
      }
      if (isPacked && type.equalsIgnoreCase("packed")) {
        type = null;
        // allow for MODELKIT SPACEGROUP packed
        if (sym == null)
          invArg();
      } else {
        if (!isPacked) {
          int ptend = i + 1;
          for (int j = ptend; j < slen; j++) {
            if (tokAt(j) == T.packed) {
              isPacked = true;
              ptend = j;
              break;
            }
          }
          if (ptend < slen)
            type = null;
        }
        if (type == null)
          type = concatString(i, (isPacked ? "packed" : "unitcell"));
        if (type.length() > 0 && type.indexOf(":") < 0 && type.indexOf(">") < 0
            && (type.indexOf(",") > 0 || "rh".indexOf(type) >= 0)) {
          // allow for MODELKIT SPACEGROUP "a,b,2c"
          if (sym == null)
            invArg();
          String cleg = sym.getSpaceGroupClegId();
          type = (cleg == null ? "[" + sym.getSpaceGroupName() + "]:" : cleg.substring(0, cleg.indexOf(":") + 1) ) + type;
        }
      }

      String s = assignSpaceGroup(bs, type, paramsOrUC, isPacked, doDraw,
          e.fullCommand);
      boolean isError = s.endsWith("!");
      if (isError)
        e.invArgStr(s);
      if (e.doReport())
        e.showString(s);
      break;
    }
  }

  private String assignSpaceGroup(BS bs, String cleg, Object paramsOrUC,
                                  boolean isPacked, boolean doDraw, String cmd) {
     SB sb = new SB();
     String ret = vwr.getSymStatic().staticTransformSpaceGroup(bs, cleg, paramsOrUC, sb);
     boolean isError = ret.endsWith("!");
     if (isError)
       return ret;
     if (isPacked || doDraw)
       vwr.getModelkit(false).cmdAssignSpaceGroup(ret, sb, cmd, isPacked, doDraw);
     return sb.toString();
   }

  private String concatString(int i, String butNot) throws ScriptException {
    String s = "";
    for (; i < slen; i++) {
      String t = SV.sValue(e.getToken(i));
      if (t.equals(butNot)) {
        e.iToken--;
        break;
      }
      s += t;
    }
    return s;
  }

  /**
   * Accept "a,b,c:0,0,0" transformation matrix syntax or [origin a b c] or [a b
   * c alpha beta gamma]
   * 
   * @param i
   * 
   * @param ret
   *        return array to hold oabc or [params]
   * @return true if oabc
   * @throws ScriptException
   */
  private boolean getUnitCellParameter(int i, Object[] ret)
      throws ScriptException {
    ret[0] = null;
    if (tokAt(i) == T.point3f)
      return false;
    int ei = e.iToken;
    if (tokAt(i) == T.string) {
      String tr = paramAsStr(i);
      SymmetryInterface uc = vwr.getCurrentUnitCell();
      if (uc == null)
        invArg();
      if (!chk) {
        tr = SimpleUnitCell.parseSimpleMath(vwr, tr);
        ret[0] = uc.getV0abc(tr, null);
      }
      return true;
    }
    if (e.isArrayParameter(i)) {
      // from state [ pt3 pt3 pt3 pt4 ]
      e.ignoreError = true;
      if (tokAt(i + 2) != T.leftbrace) {
        // [ a b c alpha beta gamma ]
        try {
          ret[0] = e.doubleParameterSet(i, 6, 6);
          e.ignoreError = false;
          return false;
        } catch (Exception e) {
          // ignore NullPointerException
        }
      }
      try {
        // [ Origin vA vB vC ]
        // these are VECTORS, though
        ret[0] = e.getPointArray(i, 4, false, true);
        return true;
      } catch (Exception ee) {
        // ignore NullPointerException
        e.iToken = ei;
      }
      try {
        // [ [o] [va] [vb] [vc] ]
        // or [ [va] [vb] [vc] ]
        // note that in the second case, [[..]] will not work, as that will be a matrix
        // and we will never get here. 
        if (tokAt(i) == T.spacebeforesquare) {
          i += 2;
          P3d[] pts = new P3d[4];
          for (int p = 0; p < 4; p++) {
            if (e.isArrayParameter(i)) {
              double[] a = e.doubleParameterSet(i, 3, 3);
              pts[p] = P3d.newA(a);
              i = e.iToken + 1;
              if (tokAt(i) == T.comma)
                i++;
              e.iToken = i;
            } else if (p == 3) {
              for (i = p; i > 0; --i) {
                pts[i] = pts[i - 1];
              }
              pts[0] = new P3d();
            } else {
              break;
            }
            if (p == 3) {
              ret[0] = pts; 
              return true;
            }
          }
        }
      } catch (Exception ee) {
        e.iToken = ei;
      }
      e.ignoreError = false;
    }
    return true;
  }

  private BS expFor(int i, BS bsAtoms) throws ScriptException {
    BS bs = BS.copy(atomExpressionAt(i));
    bs.and(bsAtoms);
    return bs;
  }

  private String getContext(boolean withVariables) {
    SB sb = new SB();
    ScriptContext context = e.thisContext;
    while (context != null) {
      if (withVariables) {
        if (context.vars != null) {
          sb.append(getScriptID(context));
          sb.append(
              StateManager.getVariableList(context.vars, 80, true, false));
        }
      } else {
        sb.append(ScriptError.getErrorLineMessage(context.functionName,
            context.scriptFileName, e.getLinenumber(context), context.pc,
            ScriptEval.statementAsString(vwr, context.statement, -9999,
                e.debugHigh)));
      }
      context = context.parentContext;
    }
    if (withVariables) {
      if (e.contextVariables != null) {
        sb.append(getScriptID(null));
        sb.append(
            StateManager.getVariableList(e.contextVariables, 80, true, false));
      }
    } else {
      sb.append(e.getErrorLineMessage2());
    }

    return sb.toString();
  }

  private Object getIsosurfaceJvxl(int iShape, String type) {
    switch (type) {
    case "PMESH":
    case "MESH":
      type = "jvxlMeshX";
      break;
    case "ISOMESH":
      type = "pmesh";
      break;
    case "ISOMESHBIN":
    case "PMB":
      type = "pmeshbin";
      break;
    case "ISOPLY":
      type = "ply";
      break;
    case "ISOPLYBIN":
    case "PLB":
      type = "plybin";
      break;
    default:
      type = "jvxlDataXml";
      break;
    }
    return (chk ? "" : getShapeProperty(iShape, type));
  }

  @SuppressWarnings("unchecked")
  private String getMoJvxl(int ptMO, boolean isNBO) throws ScriptException {
    // 0: all; Integer.MAX_VALUE: current;
    int iShape = (isNBO ? JC.SHAPE_NBO : JC.SHAPE_MO);
    e.sm.loadShape(iShape);
    int modelIndex = vwr.am.cmi;
    if (modelIndex < 0)
      e.errorStr(ScriptError.ERROR_multipleModelsDisplayedNotOK,
          "show/write MO/NBO");
    Map<String, Object> moData = (Map<String, Object>) vwr.ms
        .getInfo(modelIndex, "moData");
    if (moData == null)
      error(ScriptError.ERROR_moModelError);
    Integer n = (Integer) getShapeProperty(iShape, "moNumber");
    if (n == null || n.intValue() == 0)
      setShapeProperty(iShape, "init", Integer.valueOf(modelIndex));
    setShapeProperty(iShape, "moData", moData);
    return (String) e.sm.getShapePropertyIndex(iShape, "showMO", ptMO);
  }

  private String getScriptID(ScriptContext context) {
    String fuName = (context == null ? e.functionName
        : "function " + context.functionName);
    String fiName = (context == null ? e.scriptFileName
        : context.scriptFileName);
    return "\n# " + fuName + " (file " + fiName
        + (context == null ? "" : " context " + context.id) + ")\n";
  }

  private T tokenAt(int i, T[] args) {
    return (i < args.length ? args[i] : null);
  }

  private static int tokAtArray(int i, T[] args) {
    return (i < args.length && args[i] != null ? args[i].tok : T.nada);
  }

  private double getPlotMinMax(double[] data, boolean isMax, int tok) {
    if (data == null)
      return 0;
    switch (tok) {
    case T.omega:
    case T.phi:
    case T.psi:
      return (isMax ? 180 : -180);
    case T.eta:
    case T.theta:
      return (isMax ? 360 : 0);
    case T.straightness:
      return (isMax ? 1 : -1);
    }
    double fmax = (isMax ? -1E10d : 1E10d);
    for (int i = data.length; --i >= 0;) {
      double f = data[i];
      if (Double.isNaN(f))
        continue;
      if (isMax == (f > fmax))
        fmax = f;
    }
    return fmax;
  }

  private Object parseDataArray(String str, boolean is3D) {
    str = Parser.fixDataString(str);
    int[] lines = Parser.markLines(str, '\n');
    int nLines = lines.length;
    if (!is3D) {
      double[][] data = AU.newDouble2(nLines);
      for (int iLine = 0, pt = 0; iLine < nLines; pt = lines[iLine++]) {
        String[] tokens = PT.getTokens(str.substring(pt, lines[iLine]));
        PT.parseDoubleArrayData(tokens,
            data[iLine] = new double[tokens.length]);
      }
      return data;
    }

    String[] tokens = PT.getTokens(str.substring(0, lines[0]));
    if (tokens.length != 3)
      return new double[0][0][0];
    int nX = PT.parseInt(tokens[0]);
    int nY = PT.parseInt(tokens[1]);
    int nZ = PT.parseInt(tokens[2]);
    if (nX < 1 || nY < 1 || nZ < 1)
      return new double[1][1][1];
    double[][][] data = AU.newDouble3(nX, nY);
    int iX = 0;
    int iY = 0;
    for (int iLine = 1, pt = lines[0]; iLine < nLines
        && iX < nX; pt = lines[iLine++]) {
      tokens = PT.getTokens(str.substring(pt, lines[iLine]));
      if (tokens.length < nZ)
        continue;
      PT.parseDoubleArrayData(tokens, data[iX][iY] = new double[tokens.length]);
      if (++iY == nY) {
        iX++;
        iY = 0;
      }
    }
    if (iX != nX) {
      System.out.println("Error reading 3D data -- nX = " + nX + ", but only "
          + iX + " blocks read");
      return new double[1][1][1];
    }
    return data;
  }

  public double[] getBitsetPropertyFloat(BS bs, int tok, String property,
                                         double min, double max)
      throws ScriptException {

    Object odata = (property == null || tok == (T.wyckoff | T.allfloat)
        || tok == (T.dssr | T.allfloat)
            ? e.getBitsetProperty(bs, null, tok, null, null, property, null,
                false, Integer.MAX_VALUE, false)
            : vwr.getDataObj(property, bs, JmolDataManager.DATA_TYPE_AD));
    if (odata == null || !AU.isAD(odata))
      return (bs == null ? null : new double[bs.cardinality()]);
    double[] data = (double[]) odata;
    if (!Double.isNaN(min))
      for (int i = 0; i < data.length; i++)
        if (data[i] < min)
          data[i] = Double.NaN;
    if (!Double.isNaN(max))
      for (int i = 0; i < data.length; i++)
        if (data[i] > max)
          data[i] = Double.NaN;
    return data;
  }

}
