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

package org.jmol.modelset;

import java.util.Hashtable;
import java.util.Map;



import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.T3d;

import org.jmol.api.JmolDataManager;
import org.jmol.script.SV;
import org.jmol.script.T;
import org.jmol.util.Edge;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;


public class LabelToken {

  /*
   * by Bob Hanson, 5/28/2009
   * 
   * a compiler for the atom label business.
   * 
   * Prior to this, once for every atom, twice for every bond, and 2-4 times for every
   * measurement we were scanning the format character by character. And if data were
   * involved, then calls were made for every atom to find the data set and return its
   * value. Now you can still do that, but the Jmol code doesn't. 
   * 
   * Instead, we now first compile a set of tokens -- either pure text or some
   * sort of %xxxx business. Generally we would alternate between these, so the
   * compiler is set up to initialize an array that has 2n+1 elements, where n is the
   * number of % signs in the string. This is guaranteed to be more than really necessary.
   * 
   * Because we are working with tokens, we can go beyond the limiting A-Za-z business
   * that we had before. That still works, but now we can have any standard token be
   * used in brackets:
   * 
   *   %n.m[xxxxx]
   * 
   * This complements the 
   * 
   *   %n.m{xxxxx}
   *   
   * used for data. The brackets make for a nice-looking format:
   * 
   * 
   *  print {*}.bonds.label("%6[atomName]1 - %6[atomName]2  %3ORDER  %6.2LENGTH")
   * 
   * [Note that the %ORDER and %LENGTH variables are bond labeling options, and 
   *  the 1 and 2 after %[xxx] indicate which atom in involved.
   * 
   * 
   */

  private String text;
  private String key;
  private Object data;
  private int tok;
  private int pt = -1;
  private char ch1 = '\0';
  private int width;
  private int precision = Integer.MAX_VALUE;
  private boolean alignLeft;
  private boolean zeroPad;
  private boolean intAsFloat;

  // do not change array order without changing string order as well
  // new tokens can be added to the list at the end
  // and then also added in appendTokenValue()
  // and also in Eval, to atomProperty()

  final private static String labelTokenParams = "AaBbCcDEefGgIiLlMmNnOoPpQqRrSsTtUuVvWXxxYyyZzz%%%gqW";
  final private static int[] labelTokenIds = {
      /* 'A' */T.altloc,
      /* 'a' */T.atomname,
      /* 'B' */T.atomtype,
      /* 'b' */T.temperature,
      /* 'C' */T.formalcharge,
      /* 'c' */T.chain,
      /* 'D' */T.atomindex,
      /* 'E' */T.insertion,
      /* 'e' */T.element,
      /* 'f' */T.phi,
      /* 'G' */T.groupindex,
      /* 'g' */T.monomer, //getSelectedGroupIndexWithinChain()
      /* 'I' */T.bondingradius,
      /* 'i' */T.atomno,
      /* 'L' */T.polymerlength,
      /* 'l' */T.elemno,
      /* 'M' */T.model,
      /* 'm' */T.group1,
      /* 'N' */T.molecule,
      /* 'n' */T.group,
      /* 'O' */79, // all symmetry operators
      /* 'o' */T.symmetry,
      /* 'P' */T.partialcharge,
      /* 'p' */T.psi,
      /* 'Q' */81, //occupancy 0.0 to 1.0
      /* 'q' */T.occupancy,
      /* 'R' */T.resno,
      /* 'r' */T.seqcode,
      /* 'S' */T.site,
      /* 's' */T.chain,
      /* 'T' */T.straightness,
      /* 't' */T.temperature,
      /* 'U' */T.identify,
      /* 'u' */T.surfacedistance,
      /* 'V' */T.vanderwaals,
      /* 'v' */T.vibxyz,
      /* 'W' */T.w, // identifier and XYZ coord
      /* 'X' */T.fracx,
      /* 'x' */T.atomx,
      /* 'x' */T.x,
      /* 'Y' */T.fracy,
      /* 'y' */T.atomy,
      /* 'y' */T.y,
      /* 'Z' */T.fracz,
      /* 'z' */T.atomz,
      /* 'z' */T.z,

      // not having letter equivalents:

      //new for Jmol 11.9.5:
      T.backbone, T.cartoon, T.dots, T.ellipsoid,
      T.geosurface, T.halo, T.meshRibbon, T.ribbon,
      T.rocket, T.star, T.strands, T.trace,

      T.adpmax, T.adpmin, T.atomid, T.bondcount, T.color,
      T.groupid, T.covalentradius, T.file, T.format, T.label,
      T.mass, T.modelindex, T.eta, T.omega, T.polymer, T.property,
      T.radius, T.selected, T.shape, T.sequence,
      T.spacefill, T.structure, T.substructure, T.strucno,
      T.strucid, T.symbol, T.theta, T.unitx, T.unity,
      T.unitz, T.valence, T.vectorscale, T.vibx, T.viby, T.vibz,
      T.volume, T.unitxyz, T.fracxyz, T.xyz, T.fuxyz,
      T.fux, T.fuy, T.fuz, T.hydrophobicity, T.screenx, 
      T.screeny, T.screenz, T.screenxyz, // added in 12.3.30
      T.magneticshielding, T.chemicalshift, T.chainno, T.seqid,
      T.modx, T.mody, T.modz, T.modo, T.modxyz, T.symop, 
      T.nbo, // added in 14.8.2
      T.chirality, // added in 14.11.4
      T.ciprule // added in 14.17.0
  };

  public LabelToken() {
    // for access to static methods without preloading JavaScript
  }
  private LabelToken set(String text, int pt) {
    this.text = text;
    this.pt = pt;
    return this;
  }

  private static boolean isLabelPropertyTok(int tok) {
    for (int i = labelTokenIds.length; --i >= 0;)
      if (labelTokenIds[i] == tok)
        return true;
    return false;
  }

  public static final String STANDARD_LABEL = "%[identify]";

  private final static String twoCharLabelTokenParams = "fuv";

  private final static int[] twoCharLabelTokenIds = { T.fracx, T.fracy,
      T.fracz, T.unitx, T.unity, T.unitz, T.vibx,
      T.viby, T.vibz, };

  /**
   * Compiles a set of tokens for each primitive element of a 
   * label. This is the efficient way to create a set of labels. 
   * 
   * @param vwr
   * @param strFormat
   * @param chAtom
   * @param htValues
   * @return   array of tokens
   */
  public static LabelToken[] compile(Viewer vwr, String strFormat,
                                     char chAtom, Map<String, Object> htValues) {
    if (strFormat == null || strFormat.length() == 0)
      return null;
    if (strFormat.indexOf("%") < 0 || strFormat.length() < 2)
      return new LabelToken[] { new LabelToken().set(strFormat, -1) };
    int n = 0;
    int ich = -1;
    int cch = strFormat.length();
    while (++ich < cch && (ich = strFormat.indexOf('%', ich)) >= 0)
      n++;
    LabelToken[] tokens = new LabelToken[n * 2 + 1];
    int ichPercent;
    int i = 0;
    for (ich = 0; (ichPercent = strFormat.indexOf('%', ich)) >= 0;) {
      if (ich != ichPercent)
        tokens[i++] = new LabelToken().set(strFormat.substring(ich, ichPercent), -1);
      LabelToken lt = tokens[i++] = new LabelToken().set(null, ichPercent);
      vwr.autoCalculate(lt.tok, null);
      ich = setToken(vwr, strFormat, lt, cch, chAtom, htValues);
    }
    if (ich < cch)
      tokens[i++] = new LabelToken().set(strFormat.substring(ich), -1);
    return tokens;
  }

  //////////// label formatting for atoms, bonds, and measurements ///////////

  public String formatLabel(Viewer vwr, Atom atom, String strFormat, P3d ptTemp) {
    return (strFormat == null || strFormat.length() == 0 ? null
        : formatLabelAtomArray(vwr, atom, compile(vwr, strFormat, '\0', null),
            '\0', null, ptTemp));
  }

  /**
   * returns a formatted string based on the precompiled label tokens
   * 
   * @param vwr
   * @param atom
   * @param tokens
   * @param chAtom
   * @param indices
   * @param ptTemp 
   * @return   formatted string
   */
  public static String formatLabelAtomArray(Viewer vwr, Atom atom,
                                   LabelToken[] tokens, char chAtom,
                                   int[] indices, P3d ptTemp) {
    if (atom == null)
      return null;
    SB strLabel = (chAtom > '0' ? null : new SB());
    if (tokens != null)
      for (int i = 0; i < tokens.length; i++) {
        LabelToken t = tokens[i];
        if (t == null)
          break;
        if (chAtom > '0' && t.ch1 != chAtom)
          continue;
        if (t.tok <= 0 || t.key != null) {
          if (strLabel != null) {
            strLabel.append(t.text);
            if (t.ch1 != '\0')
              strLabel.appendC(t.ch1);
          }
        } else {
          appendAtomTokenValue(vwr, atom, t, strLabel, indices, ptTemp);
        }
      }
    return (strLabel == null ? null : strLabel.toString().intern());
  }

  public static Map<String, Object> getBondLabelValues() {
    Map<String, Object> htValues = new Hashtable<String, Object>();
    htValues.put("#", "");
    htValues.put("ORDER", "");
    htValues.put("TYPE", "");
    htValues.put("LENGTH", Double.valueOf(0));
    htValues.put("ENERGY", Double.valueOf(0));
    return htValues;
  }

  public static String formatLabelBond(Viewer vwr, Bond bond,
                                   LabelToken[] tokens,
                                   Map<String, Object> values, int[] indices, P3d ptTemp) {
    values.put("#", "" + (bond.index + 1));
    values.put("ORDER", "" + Edge.getBondOrderNumberFromOrder(bond.order));
    values.put("TYPE", Edge.getBondOrderNameFromOrder(bond.order));
    values.put("LENGTH", Double.valueOf(bond.atom1.distance(bond.atom2)));
    values.put("ENERGY", Double.valueOf(bond.getEnergy()));
    setValues(tokens, values);
    formatLabelAtomArray(vwr, bond.atom1, tokens, '1', indices, ptTemp);
    formatLabelAtomArray(vwr, bond.atom2, tokens, '2', indices, ptTemp);
    return getLabel(tokens);
  }

  public static String formatLabelMeasure(Viewer vwr, Measurement m,
                                   String label, double value, String units) {
    Map<String, Object> htValues = new Hashtable<String, Object>();
    htValues.put("#", "" + (m.index + 1));
    htValues.put("VALUE", Double.valueOf(value));
    htValues.put("UNITS", units);
    LabelToken[] tokens = compile(vwr, label, '\1', htValues);
    if (tokens == null)
      return "";
    setValues(tokens, htValues);
    Atom[] atoms = m.ms.at;
    int[] indices = m.countPlusIndices;
    for (int i = indices[0]; i >= 1; --i)
      if (indices[i] >= 0)
        formatLabelAtomArray(vwr, atoms[indices[i]], tokens, (char) ('0' + i), null, null);
    label = getLabel(tokens);
    return (label == null ? "" : label);
  }

  public static void setValues(LabelToken[] tokens, Map<String, Object> values) {
    for (int i = 0; i < tokens.length; i++) {
      LabelToken lt = tokens[i];
      if (lt == null)
        break;
      if (lt.key == null)
        continue;
      Object value = values.get(lt.key);
      lt.text = (value instanceof Double ? lt.format(((Double) value)
          .doubleValue(), null, null) : lt.format(Double.NaN, (String) value,
          null));
    }
  }

  public static String getLabel(LabelToken[] tokens) {
    SB sb = new SB();
    for (int i = 0; i < tokens.length; i++) {
      LabelToken lt = tokens[i];
      if (lt == null)
        break;
      sb.append(lt.text);
    }
    return sb.toString();
  }

  /////////////////// private methods
  
  /**
   * sets a label token based on a label string
   * 
   * @param vwr
   * @param strFormat
   * @param lt
   * @param cch
   * @param chAtom
   * @param htValues
   * @return         new position
   */
  private static int setToken(Viewer vwr, String strFormat, LabelToken lt,
                              int cch, int chAtom, Map<String, Object> htValues) {
    int ich = lt.pt + 1;
    // trailing % is OK
    if (ich >= cch) {
      lt.text = "%";
      return ich;
    }
    char ch;
    if (strFormat.charAt(ich) == '-') {
      lt.alignLeft = true;
      ++ich;
    }
    if (ich < cch && strFormat.charAt(ich) == '0') {
      lt.zeroPad = true;
      ++ich;
    }
    while (ich < cch && PT.isDigit(ch = strFormat.charAt(ich))) {
      lt.width = (10 * lt.width) + (ch - '0');
      ++ich;
    }
    lt.precision = Integer.MAX_VALUE;
    boolean isNegative = false;
    if (ich < cch && strFormat.charAt(ich) == '.') {
      ++ich;
      if (ich < cch && (ch = strFormat.charAt(ich)) == '-') {
        isNegative = true;
        ++ich;
      }
      if (ich < cch && PT.isDigit(ch = strFormat.charAt(ich))) {
        ++ich;
        lt.precision = ch - '0';
        if (ich < cch && PT.isDigit(ch = strFormat.charAt(ich))) {
          ++ich;
          lt.precision = lt.precision * 10 + (ch - '0');
        }
        if (isNegative)
          lt.precision = -1 - lt.precision;
      }
    }
    if (ich < cch && htValues != null)
      for (String key: htValues.keySet())
        if (strFormat.indexOf(key) == ich)
          return ich + (lt.key = key).length();
    if (ich < cch)
      switch (ch = strFormat.charAt(ich++)) {
      case '%':
        lt.text = "%";
        return ich;
      case '[':
        int ichClose = strFormat.indexOf(']', ich);
        if (ichClose < ich) {
          ich = cch;
          break;
        }
        String propertyName = strFormat.substring(ich, ichClose).toLowerCase();
        if (propertyName.startsWith("property_")) {
          lt.tok = T.data;
          lt.data = vwr.getDataObj(propertyName, null, JmolDataManager.DATA_TYPE_AD);
        } else if (propertyName.startsWith("validation.")) {
          lt.tok = T.validation;
          lt.data = vwr.getDataObj("property_" + propertyName.substring(11), null, JmolDataManager.DATA_TYPE_AD);
        } else if (propertyName.startsWith("unitid")) {
           lt.tok = T.id;
           lt.data = Integer.valueOf(JC.getUnitIDFlags(propertyName.substring(6)));
        } else {
          T token = T.getTokenFromName(propertyName);
          if (token != null && isLabelPropertyTok(token.tok))
            lt.tok = token.tok;
        }
        ich = ichClose + 1;
        break;
      case '{':
        // label %{altName}
        // client property name deprecated in 12.1.22
        // but this can be passed to Jmol from the reader
        // as an auxiliaryInfo array or '\n'-delimited string
        int ichCloseBracket = strFormat.indexOf('}', ich);
        if (ichCloseBracket < ich) {
          ich = cch;
          break;
        }
        String s = strFormat.substring(ich, ichCloseBracket);
        lt.data = vwr.getDataObj(s, null, JmolDataManager.DATA_TYPE_AD);
        // TODO untested j2s issue fix
        if (lt.data == null) {
          lt.data = vwr.getDataObj(s, null, JmolDataManager.DATA_TYPE_UNKNOWN);
          if (lt.data != null) {
            lt.data = ((Object[]) lt.data)[1];
            if (lt.data instanceof String)
              lt.data = PT.split((String) lt.data, "\n");
            if (!(AU.isAS(lt.data)))
              lt.data = null;
          }
          if (lt.data == null) {
            lt.tok = T.property;
            lt.data = s;
          } else {
            lt.tok = T.array;
          }
        } else {
          lt.tok = T.data;
        }
        ich = ichCloseBracket + 1;
        break;
      default:
        int i,
        i1;
        if (ich < cch && (i = twoCharLabelTokenParams.indexOf(ch)) >= 0
            && (i1 = "xyz".indexOf(strFormat.charAt(ich))) >= 0) {
          lt.tok = twoCharLabelTokenIds[i * 3 + i1];
          ich++;
        } else if ((i = labelTokenParams.indexOf(ch)) >= 0) {
          lt.tok = labelTokenIds[i];
        }
      }
    lt.text = strFormat.substring(lt.pt, ich);
    if (ich < cch && chAtom != '\0'
        && PT.isDigit(ch = strFormat.charAt(ich))) {
      ich++;
      lt.ch1 = ch;
      if (ch != chAtom && chAtom != '\1')
        lt.tok = 0;
    }
    return ich;
  }

  private static void appendAtomTokenValue(Viewer vwr, Atom atom, LabelToken t,
                                           SB strLabel, int[] indices, P3d ptTemp) {
    String strT = null;
    double floatT = Double.NaN;
    T3d ptT = null;
    try {
      switch (t.tok) {

      // special cases only for labels 

      case T.atomindex:
        strT = "" + (indices == null ? atom.i : indices[atom.i]);
        break;
      case T.color:
        ptT = atom.atomPropertyTuple(vwr, t.tok, ptTemp);
        break;
      case T.id:
        strT = atom.getUnitID(((Integer) t.data).intValue());
        break;
      case T.data:
      case T.validation:
        if (t.data != null) {
          floatT = ((double[]) t.data)[atom.i];
          if (t.tok == T.validation && floatT != 1 && floatT != 0) {
            Lst<Double> o = vwr.getAtomValidation(
                t.text.substring(13, t.text.length() - 1), atom);
            if (o == null) {
              System.out.println("?? o is null ??");
            } else if (o.size() == 1) {
              floatT = o.get(0).doubleValue();
            } else {
              floatT = Double.NaN;
              strT = "";
              for (int i = 0, n = o.size(); i < n; i++) {
                strT += "," + o.get(i);
              }
              if (strT.length() > 1)
                strT = strT.substring(1);
            }
          }
        }
        break;
      case T.property:
        // label %{altName}
        Object data = vwr.ms.getInfo(atom.mi, (String) t.data);
        int iatom = atom.i - vwr.ms.am[atom.mi].firstAtomIndex;
        Object o = null;
        if (iatom >= 0)
          if ((data instanceof Object[])) {
            // unlikely that this will be the case.
            // it would have to be 
            Object[] sdata = (Object[]) data;
            o = (iatom < sdata.length ? sdata[iatom] : null);
          } else if (data instanceof Lst<?>) {
            @SuppressWarnings("unchecked")
            Lst<SV> list = (Lst<SV>) data;
            o = (iatom < list.size() ? SV.oValue(list.get(iatom)) : null);
          }
        if (o == null) {
          strT = "";
        } else if (o instanceof Double) {
          floatT = ((Double) o).doubleValue();
        } else if (o instanceof Integer) {
          floatT = ((Integer) o).intValue();
        } else if (o instanceof T3d) {
          ptT = (T3d) o;
        } else {
          strT = o.toString();
        }
        break;
      case T.array:
        if (t.data != null) {
          String[] sdata = (String[]) t.data;
          strT = (atom.i < sdata.length ? sdata[atom.i] : "");
        }
        break;
      case T.formalcharge:
        int formalCharge = atom.getFormalCharge();
        strT = (formalCharge > 0 ? "" + formalCharge + "+"
            : formalCharge < 0 ? "" + -formalCharge + "-" : "");
        break;
      case T.model:
        strT = atom.getModelNumberForLabel();
        break;
      case T.occupancy:
        strT = "" + atom.atomPropertyInt(t.tok);
        break;
      case T.radius:
        floatT = atom.atomPropertyFloat(vwr, t.tok, ptTemp);
        break;
      case T.strucid:
        strT = atom.group.getStructureId();
        break;
      case T.strucno:
        int id = atom.group.getStrucNo();
        strT = (id <= 0 ? "" : "" + id);
        break;
      case T.straightness:
        if (Double.isNaN(floatT = atom.group.getGroupParameter(T.straightness)))
          strT = "null";
        break;
      case T.vibx:
      case T.viby:
      case T.vibz:
      case T.modx:
      case T.mody:
      case T.modz:
      case T.modo:
        floatT = atom.atomPropertyFloat(vwr, t.tok, ptTemp);
        if (Double.isNaN(floatT))
          strT = "";
        break;
      case T.nbo:
        strT = vwr.getNBOAtomLabel(atom);
        break;
      case T.seqcode: // see 1h4w  184^A
      case T.structure:
      case T.substructure:
        strT = atom.atomPropertyString(vwr, t.tok);
        break;
      case T.w:
        strT = atom.getIdentityXYZ(ptTemp, Atom.ID_U);
        break;
        
      // characters only
      // JavaScript switch cannot handle mixed cases of numbers and character codes
      case 79://'O':
        strT = atom.getSymmetryOperatorList(false);
        break;
      case 81://'Q':
        floatT = atom.getOccupancy100() / 100f;
        break;

      // standard 

      default:
        switch (t.tok & T.PROPERTYFLAGS) {
        case T.intproperty:
          if (t.intAsFloat)
            floatT = atom.atomPropertyInt(t.tok);
          else
            strT = "" + atom.atomPropertyInt(t.tok);
          break;
        case T.floatproperty:
          floatT = atom.atomPropertyFloat(vwr, t.tok, ptTemp);
          break;
        case T.strproperty:
          strT = atom.atomPropertyString(vwr, t.tok);
          break;
        case T.atomproperty:
          ptT = atom.atomPropertyTuple(vwr, t.tok, ptTemp);
          if (ptT == null)
            strT = "";
          break;
        default:
          // any dual case would be here -- must handle specially
        }
      }
    } catch (IndexOutOfBoundsException ioobe) {
      floatT = Double.NaN;
      strT = null;
      ptT = null;
    }
    strT = t.format(floatT, strT, ptT);
    if (strLabel == null)
      t.text = strT;
    else
      strLabel.append(strT);
  }

  private String format(double floatT, String strT, T3d ptT) {
    if (!Double.isNaN(floatT)) {
      return PT.formatF(floatT, width, precision, alignLeft, zeroPad);
    } else if (strT != null) {
      return PT.formatS(strT, width, precision, alignLeft, zeroPad);
    } else if (ptT != null) {
      if (width == 0 && precision == Integer.MAX_VALUE) {
        width = 6;
        precision = 2;
      }
      return PT.formatF(ptT.x, width, precision, false, false)
          + PT.formatF(ptT.y, width, precision, false, false)
          + PT.formatF(ptT.z, width, precision, false, false);
    } else {
      return text;
    }
  }

}
