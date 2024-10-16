/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

import java.util.Arrays;
import java.util.Map;

import org.jmol.api.SymmetryInterface;
import org.jmol.script.SV;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.util.AU;
import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.M4d;
import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.T3d;

/**
 * A holding class for ClegData and ClegNode, which are called by
 * org.jmol.modelkit.Modelkit to process CLEG strings.
 * 
 * This code access multiple methods in Jmol, so it is not independently
 * implemented outside that context. But it enca  psulates the essentals of 
 * 
 */
final public class CLEG {

  /**
   * allows 300, 400, 500, 600 instead of p/ l/ r/ f/
   * could be turned on; this is for Jmol testing
   * 
   */
  public static boolean allow300 = false;

  public static final String HEX_TO_RHOMB = "2/3a+1/3b+1/3c,-1/3a+1/3b+1/3c,-1/3a-2/3b+1/3c";
  public static final String RHOMB_TO_HEX = "a-b,b-c,a+b+c";
  
  /**
     * running data items for assignSpaceGroup iteration
     */
    public static class ClegData {
      
      final public String[] tokens;
      
      public SymmetryInterface sym;       
 
      public M4d trMat;

      public String errString;
  
      M4d trLink;

      private M4d trTemp = new M4d();
      ClegNode prevNode;

      Map<String, Object> retMap;
      Lst<Object> retLst;
      boolean asM4;

      public ClegData(SymmetryInterface sym, String[] tokens) {
        this.tokens = tokens;
        this.sym = sym;
      }
  
      public M4d addSGTransform(String tr, String what) {
        if (trMat == null) {
          System.out.println("ClegData reset");
          trMat = new M4d();
          trMat.setIdentity();
        }
        if (tr != null) {
          trMat.mul(matFor(tr));
        }
        if (what != null)
          System.out.println("ClegData adding " + what + " " + tr + " now " + abcFor(trMat));
        return trMat;
      }
  
      public String abcFor(M4d trm) {
        return sym.staticGetTransformABC(trm, false);
      }

      public M4d matFor(String trm) {
        return (M4d) sym.convertTransform(trm, (trm.indexOf(">") > 0 ? null : trTemp));
      }
  
  
      public void removePrevNodeTrm() {
        if (prevNode != null && prevNode.myTrm != null && !prevNode.disabled) {
          //addSGTransform("!" + prevNode.mySetting, "!prevNode.setting");
          addSGTransform("!" + prevNode.myTrm, "!prevNode.myTrm");
        }
      }
  
      public String calculate(M4d trm0) {
        trm0.invert();
        M4d trm1 = M4d.newM4(trMat);
        trm1.mul(trm0);
        return (String) sym.convertTransform(null, trm1);
      }
  
      /**
       * fill in the blanks to create a CLEG string when joined.
       * @param node
       */
      public void updateTokens(ClegNode node) {
          int index = node.index;
          String s = node.name;
          if (s.startsWith("ITA/"))
            s = s.substring(4);
          else // 133.1
            s = node.myIta + ":" + node.myTrm;
          setToken(index, s);
          if (node.calculated != null && index > 0)
            setToken(index - 1, node.calculated);
      }
  
      private void setToken(int index, String s) {
        tokens[index] = s;
      }
  
      public void addTransformLink() {
        if (trLink == null) {
          trLink = new M4d();
          trLink.setIdentity();
        }
        trLink.mul(trTemp);
      }
  
      public void setNodeTransform(ClegNode node) {
        node.myTrm = abcFor(trMat);
        node.setITAName();
      }

      public void addTransform(int index, String transform) {
          addSGTransform(transform, ">t>");
          // if the link is null, this will be the flag that we have x >> y
          addTransformLink();
          System.out.println(
              "CLEG.addTransform index=" + index + " trm=" + trMat);
      }

      public ClegNode getPrevNode() {
        return prevNode;
      }
      
      public ClegNode setPrevNode(ClegNode node) {
        return prevNode = node;
      }

      public String addPrimitiveTransform(String myIta, String myTrm) {
        String hmName = (String) sym.getSpaceGroupInfoObj("hmName", myIta + ":" + myTrm,
            false, false);
        if (hmName == null)
          return myTrm;
        char c = hmName.charAt(0);
        if ("ABCFI".indexOf(c) < 0)
          return myTrm;
        M4d t = M4d.newMV(UnitCell.getPrimitiveTransform(c), P3d.new3(0,0,0));
        t.mul(matFor(myTrm));
        return abcFor(t);
      }

      public void setReturnMap(Map<String, Object> ret) {
        if (ret != null) {
          asM4 = (ret.get("ASM4") == Boolean.TRUE);
          ret.clear();
        }
        retMap = ret;
      }

      public void setReturnLst(Lst<Object> ret) {
        if (ret != null)
          ret.clear();
        retLst = ret;        
      }

    }

    public static class ClegNode {
  
      public final static String TYPE_REFERENCE = "ref";
      public final static String CALC_SUB = "sub";
      public final static String CALC_SUBP = "sub(";
      public final static String CALC_SUPER = "super";
      public final static String CALC_SUPERP = "super(";
      public final static String CALC_SET = "set";

      String name;
      /**
       * the number 1 to 230
       */
      String myIta;
      
      /**
       * a Transformation(P,p) expression
       */
      String myTrm;

      /**
       * 0-based index of this node in a CLEG expression A > tt > B > tt >> C ...
       */
      int index;
      
      /**
       * "sub" or "super" for Jmol >sub>  >super>
       */
      private String calcNext;
      private int calcI1;
      private int calcI2;
      private int calcDepthMin;
      private int calcDepthMax;
      private int calcIndexMin;
      private int calcIndexMax;
      
      String calculated; // transform car
      boolean disabled;
      //private boolean applySetting = true;
      /**
       * Cleg ends with >, implying we want a calculation of the final transform
       * and use that to assign the final space group setting
       * 
       */
      private boolean isThisModelCalc;
  
      private String hallSymbol;
  
      private int specialType = SpaceGroup.TYPE_SPACE;
      
      String specialPrefix = "";
      
      public ClegNode(ClegData data, int index, String name) {
        if (name == null)
          return;
        this.index = index;
        // sets this.name
        init(data, name);
      }
      
      public void disable() {
        disabled = true;
      }

    /**
     * Set specialType and specialPrefix and normalize name
     * 
     * @param name
     * @return name stripped of prefix and adjusted for 300+ if allowed
     */
    private String checkSpecial(String name) {
      switch (specialType = SpaceGroup.getExplicitSpecialGroupType(name)) {
      case SpaceGroup.TYPE_INVALID:
        return null;
      case SpaceGroup.TYPE_SPACE:
        if (!allow300)
          return name;
        int ptDot = name.indexOf(".");
        String sname = (ptDot > 0 ? name.substring(0, ptDot) : name);
        int itno = SpaceGroup.getITNo(sname, 0);
        if (itno < 300)
          return name;
        if (itno > SpaceGroup.TYPE_FRIEZE)
          return null;
        specialType = (itno / 100) * 100;
        specialPrefix = SpaceGroup.getGroupTypePrefix(itno);
        return "" + (itno - specialType) + name.substring(sname.length());
      default:
        // special
        specialPrefix = name.substring(0, 2);
        return name.substring(2);
      }

    }

    private void init(ClegData data, String name) {
      int pt = isProbableClegSetting(name);
      if (pt > 0) {
        myIta = name.substring(0, pt);
        myTrm = name.substring(pt + 1);
      }
      if (name.equals(TYPE_REFERENCE)) {
        isThisModelCalc = true;
      }
      // register and remove p/ l/ r/ f/ prefix if there is one
      name = checkSpecial(name);
      boolean isPrimitive = name.endsWith(":p");
      if (isPrimitive)
        name = name.substring(0, name.length() - 2);
      boolean isITAnDotm = name.startsWith("ITA/");
      if (isITAnDotm) {
        // ITA/140 or ITA/140.2 or ITA/p/23 or ITA/r/34.2
        name = checkSpecial(name.substring(4));
      }
      boolean isHM = false;
      hallSymbol = null;
      String hallTrm = null;
      if (specialType == SpaceGroup.TYPE_SPACE && name.charAt(0) == '[') {

        // [P 2y] is a Hall symbol
        pt = name.indexOf(']');
        if (pt < 0) {
          data.errString = "invalid Hall symbol: " + name + "!";
          return;
        }
        hallSymbol = name.substring(1, pt);
        pt = name.indexOf(":", pt);
        if (pt > 0)
          hallTrm = name.substring(pt + 1);
        name = "Hall:" + hallSymbol;
      } else if (name.startsWith("HM:")) {
        // not sure this is still useful; ok, leave this this way
        isHM = true;
      } else if (name.length() <= 3) {
        // quick check for nnn
        isITAnDotm = (SpaceGroup.getITNo(name, 0) > 0);
        if (isITAnDotm) {
          name = checkSpecial(name) + ".1";
        }
      }
      // Hall:xxxx, or JmolID, or other
      if (!isITAnDotm && hallSymbol == null && !isHM) {
        pt = (PT.isDigit(name.charAt(0)) ? name.indexOf(" ") : -1);
        if (pt > 0)
          name = name.substring(0, pt);
        if (name.indexOf('.') > 0 && !Double.isNaN(PT.parseDouble(name))) {
          // "6.1"
          isITAnDotm = true;
        }
      }
      if (isITAnDotm) {
        // We need the tranformation matrix for "10.2"
        // so that we can set the unit cell, as findSpacegroup does NOT 
        // set the unit cell in SG_IS_ASSIGN mode and there is a
        // unit cell already. This call will build the space group if necessary
        // if it is from Wyckoff 
        myTrm = (name.endsWith(".1") ? "a,b,c"
            : (String) data.sym.getITASettingValue(null, specialPrefix + name, "trm"));
        if (myTrm == null) {
          data.errString = "Unknown ITA setting: " + specialPrefix + name + "!";
          return;
        }
        String[] parts = PT.split(name, ".");
        myIta = parts[0];
      } else {
        // look-up for Hermann-Mauguin name. 
        if (myIta == null)
          myIta = (String) data.sym.getSpaceGroupInfoObj("itaNumber", specialPrefix + name,
              false, false);
        if (myTrm == null)
          myTrm = (String) data.sym.getSpaceGroupInfoObj("itaTransform", specialPrefix + name,
              false, false);

        if (hallSymbol != null && hallTrm != null) {
          if (myTrm.equals("a,b,c")) {
            myTrm = hallTrm;
          } else {
            data.errString = "Non-reference Hall symbol cannot also contain a setting: "
                + name + "!";
            return;
          }
        }
      }
      if ("0".equals(myIta)) {
        data.errString = "Could not get ITA space group for " + name + "!";
        return;
      }
      if (isPrimitive) {
        myTrm = data.addPrimitiveTransform(myIta, myTrm);
      }
      setITAName();
    }
  
      public String setITAName() {
        return name = "ITA/" + specialPrefix + myIta + ":" + myTrm;
      }
  
    public boolean update(ClegData data) {
      if (data.errString != null)
        return false;
      if (name == null)
        return true;

      ClegNode prev = data.prevNode;
      if (prev.isThisModelCalc)
        prev.myIta = myIta;

      /**
       * haveReferenceCell is set true if and only if we can apply the setting
       * if it is present or implied; this is the case when the current unit
       * cell is the "symmetry" cell -- that is, that cell represents the proper
       * unit cell for the current space group; this is checked for a matching
       * ITA number.
       */

      boolean haveReferenceCell = (data.trLink == null && (myIta != null
          && (myIta.equals(prev.myIta) || prev.calcNext != null)));

//      System.out.println("ClegNode update i=" + index + " n=" + name
//          + "\n data.trLink=" + data.trLink + "\n " + prev + " " + prev.myIta
//          + " " + prev.calcNext + " "
//          //+ applySetting 
//          + " haveref=" + haveReferenceCell);

      if (!haveReferenceCell)
        return true;

      M4d trm0 = M4d.newM4(data.trMat);
      data.removePrevNodeTrm();
      String trCalc = null;
      if (prev.calcNext != null) {
        boolean isSub = true;
        boolean isImplicit = false;
        boolean isCalcFunction = false;

        switch (prev.calcNext) {
        case CALC_SUPERP:
        case CALC_SUBP:
          isCalcFunction = true;
          break;
        case CALC_SUPER:
          isSub = false;
          break;
        case CALC_SUB:
          break;
        case "":
        case CALC_SET:
          // from >>; acts for sub or set
          prev.calcNext = CALC_SET;
          isImplicit = true;
          break;
        }
        int ita1 = PT.parseInt(prev.myIta);
        int ita2 = PT.parseInt(myIta);
        boolean unspecifiedSettingChangeOnly = !isCalcFunction
            && (data.retLst == null && (data.retMap == null || data.asM4) 
            && isImplicit && ita1 == ita2);
        if (!unspecifiedSettingChangeOnly) {
          int flags = (prev.calcIndexMax << 24) | (prev.calcIndexMin << 16)
              | (prev.calcDepthMax << 8) | prev.calcDepthMin;
          trCalc = (String) data.sym.getSubgroupJSON((isSub ? prev.name : name),
              (isSub ? name : prev.name), prev.calcI1, prev.calcI2, flags, data.retMap, data.retLst);
          boolean haveCalc = (trCalc != null);
          if (haveCalc) {
              if (trCalc.endsWith("!")) {
                data.errString = trCalc;
                return false;
              }
            if (!isSub)
              trCalc = "!" + trCalc;
          }

          String calc = prev.myIta + ">" + (haveCalc ? trCalc : "?") + ">"
              + myIta;
          if (!haveCalc) {
            data.errString = calc + "!";
            return false;
          }
//          System.out.println("CLEG sub := " + calc);
          data.addSGTransform(trCalc, CALC_SUB);
        }
      }
      if (!disabled)
        data.addSGTransform(myTrm, "myTrm");
      calculated = data.calculate(trm0);
      System.out.println("calculated is " + calculated);
      return true;
    }
  
      public String getName() {
        return name;
      }

      public String getCleanITAName() {
        String s = (name.startsWith("ITA/") ? name.substring(4) : name);
        if (specialType != SpaceGroup.TYPE_SPACE && !s.startsWith(specialPrefix))
          s = specialPrefix + s;
        return s;
      }
  
      public boolean isDefaultSetting() {
        return (myTrm == null || cleanCleg000(myTrm).equals("a,b,c"));
      }

    /**
     * sup(i0, i1, indexMax, depthMax)
     * 
     * @param data
     * @param token
     * @return true if sytax is correct
     */
    public boolean setCalcNext(ClegData data, String token) {
      int pt = token.length();
      switch (pt == 0 ? token : CLEG.getCalcType(token)) {
      case CALC_SUB:
        if (data.retLst != null || data.retMap != null) {
          pt = 0;
          break;
        }        
        //$FALL-THROUGH$
      case CALC_SUPER:
        calcI1 = 1;
        calcI2 = 1;
        pt = 3;
        break;
      case CALC_SUBP:
        pt = -3;
        break;
      case CALC_SUPERP:
        pt = -5;
        break;
      }
      boolean isErr = true;
      while (true) {
        if (pt == 0) {
          //G>>H
          calcIndexMin = 2;
          calcIndexMax = 0xFF;
          calcDepthMin = 1;
          calcDepthMax = 0xFF;
          isErr = false;
        } else if (pt > 0) {
          //G>SUB>H
          calcIndexMin = 2;
          calcIndexMax = 0xFF;
          calcDepthMin = 1;
          calcDepthMax = 1;
          isErr = false;
        } else {
          if (token.indexOf(")") != token.length() - 1)
            break;
          String[] params = PT.split(PT.trim(token.toLowerCase().substring(-pt + 1), ")"),
              ",");
          try {
            if (token.length() == 5 || token.indexOf('=') >= 0) {
              calcIndexMin = 2;
              calcIndexMax = 0xFF;
              calcDepthMin = 1;
              calcDepthMax = 0xFF;
              token = token.substring(0, -pt);
              for (int i = params.length; --i >= 0;) {
                String p = params[i];
                int val = Math.min(0xFF, Integer.parseInt(p.substring(p.indexOf('=') + 1)));
                if (p.startsWith("indexmax="))
                  calcIndexMax = Math.max(2, val);
                else if (p.startsWith("indexmin="))
                  calcIndexMin = Math.max(2, val);
                else if (p.startsWith("index="))
                  calcIndexMin = calcIndexMax = Math.max(2, val);
                if (p.startsWith("depthmax="))
                  calcDepthMax = Math.max(1, val);
                else if (p.startsWith("depthmin="))
                  calcDepthMin = Math.max(1, val);
                else if (p.startsWith("depth="))
                  calcDepthMin = calcDepthMax = Math.max(1, val);
              }
            } else {
              switch (params.length) {
              case 2:
                calcI2 = Math.max(0, Integer.parseInt(params[1])) & 0xFF;
                //$FALL-THROUGH$
              case 1:
                // 0 will be treated as 1
                calcI1 = Math.max(1, Integer.parseInt(params[0])) & 0xFF;
                break;
              }
            }
            token = token.substring(0, -pt);
            isErr = false;
          } catch (Exception e) {
          }
        }
        break;
      }
      if (isErr) {
        data.errString = "Error parsing CLEG " + token + "!";
        return false;
      }
      calcNext = token;
      return true;
    }
  
      @Override
      public String toString() {
        return "[ClegNode #" + index + " " + name + "   " + myIta + ":" + myTrm + (disabled ? " disabled" : "") + "]";
      }
  
    }

  public static void standardizeTokens(String[] tokens, boolean isEnd) {
        for (int i = tokens.length; --i >= 0;) {
          String t = tokens[i];
          if (t.length() == 0) {
            continue;
          }
          t = cleanCleg000(t);
          if (t.endsWith(":h")) {
            if (!t.startsWith("R"))
            t = t.substring(0, t.length() - 2);
          } else if (!isEnd && t.endsWith(":" + HEX_TO_RHOMB)) {
  //don't do this          t = t.substring(0, t.lastIndexOf(":")) + ":r";
          } else if (t.endsWith(":r")) {
            if (!t.startsWith("R"))
            t = t.substring(0, t.length() - 1)
                + HEX_TO_RHOMB;
          } else if (t.equals("r")) {
            t = HEX_TO_RHOMB;
          } else if (t.equals("h")) {
            t = RHOMB_TO_HEX;
          }
  //        if (isEnd)
  //          t = SpaceGroup.canonicalizeCleg(t);
          tokens[i] = t;
        }
        System.out.println("MK StandardizeTokens " + Arrays.toString(tokens));
      }

  public static String cleanCleg000(String t) {
    return (t.endsWith(";0,0,0") ? t.substring(t.length() - 6) : t);
  }

  /**
   * Determine if the string is of the general form
   * [<type>/]<n>:...,.... but not checking for details of the it number or the setting
   * allows optional <type>/ prefix where <type> is one of 'p','l','r','f'
   * @param name
   * @return position of ":" or -1;
   */
  public static int isProbableClegSetting(String name) {
    int p = name.indexOf(":");
    int type = SpaceGroup.getExplicitSpecialGroupType(name);
    return (type >= 0 && p > 0 
        && SpaceGroup.getITNo(type == SpaceGroup.TYPE_SPACE ? name : name.substring(2), p) > 0 
        && name.indexOf(",") > p ? p : 0);
  }

  /**
   * Test nodes and transforms for valid syntax. 133:b1 is OK, perhaps, at
   * this point, but 142:0,0,0 is not and P2/m:a,b,c is not
   * and IT numbers must be in the appropriate range
   * 
   * @param tokens
   * @param sym
   * @param allow300 allow CLEG extension to 301-317 for plane, 401-480 for layer, 501-575 for rod, 601-607 for frieze
   * @return true for OK syntax
   */
  public static boolean checkFullSyntax(String[] tokens,
                                       SymmetryInterface sym, boolean allow300) {
    for (int i = 0; i < tokens.length; i++) {
      String s = tokens[i].trim();
      if (s.length() == 0 || s.startsWith("sub") || s.startsWith("super"))
        continue;
      int groupType = SpaceGroup.getExplicitSpecialGroupType(s);
      if (groupType > SpaceGroup.TYPE_SPACE)
        s = s.substring(2);
      int ptColon = s.indexOf(":");
      int ptComma = s.indexOf(",", ptColon + 1);
      int ptDot = s.indexOf(".");
      boolean isClegSetting = (ptColon > 0 && ptComma > ptColon);
      int ptHall = s.indexOf("]");
      boolean isHall = (ptHall > 0 && s.charAt(0) == '['
          && (ptColon < 0 || ptColon == ptHall + 1));
      double itno = (isHall ? 1
          : PT.parseDoubleStrict(ptColon > 0 ? s.substring(0, ptColon) : s));
      if (Double.isNaN(itno)) {
        // a transform or an H-M name without a cleg setting is ok, 
        // but anything with a dot or a colon and an expression containing "," should not be here
        if (ptDot > 0 || isClegSetting)
          return false;
      } else if (!SpaceGroup.isInRange(itno, groupType, !isClegSetting, allow300 && groupType == SpaceGroup.TYPE_SPACE)) {
        return false;
      }
      if (ptComma < 0)
        continue;
      String transform = (ptColon > 0 ? s.substring(ptColon + 1) : s);
      if (((M4d) sym.convertTransform(transform, null)).determinant3() == 0)
        return false;
    }
    return true;
  }

  public static String getCalcType(String token) {
    return (token.length() == 0 || token.equals(ClegNode.CALC_SUB) ? ClegNode.CALC_SUB
            : token.charAt(0) != 's' ? null
            : token.startsWith(ClegNode.CALC_SUBP) ? ClegNode.CALC_SUBP
            : token.equals(ClegNode.CALC_SUPER) ? ClegNode.CALC_SUPER
            : token.startsWith(ClegNode.CALC_SUPERP) ? ClegNode.CALC_SUPERP
            : null);
  }

  public static boolean isTransformOnly(String token) {
    return (isTransform(token, false) && token.indexOf(":") < 0);
  }

  public static boolean isTransform(String token, boolean checkColonRH) {
    return (token.length() == 0 || token.indexOf(',') > 0
        || "!r!h".indexOf(token) >= 0)
        || checkColonRH && (token.endsWith(":r")|| token.endsWith(":h"));
  }
  
  /**
   * The main entry point for calculation of an overall transfrom. Allows a
   * single space group to mean "current going to..."
   * 
   * @param vwr
   * @param cleg
   * @param retLstOrMap
   *        a Map or Lst that are to return solutions
   * @return matrix or null
   */
  @SuppressWarnings("unchecked")
  M4d getMatrixTransform(Viewer vwr, String cleg, Object retLstOrMap) {
    if (cleg.indexOf(">") < 0)
      cleg = ">>" + cleg;
    String[] tokens = PT.split(cleg, ">");
    if (tokens[0].length() == 0)
      tokens[0] = ClegNode.TYPE_REFERENCE;
    ClegData data = new ClegData(vwr.getSymTemp(), tokens);
    Map<String, Object> retMap = (retLstOrMap instanceof Map<?, ?>
        ? (Map<String, Object>) retLstOrMap
        : null);
    Lst<Object> retLst = (retMap == null && retLstOrMap instanceof Lst<?>
        ? (Lst<Object>) retLstOrMap
        : null);
    data.setReturnMap(retMap);
    data.setReturnLst(retLst);
    String err = assignSpaceGroup(data, new AssignedSGParams(vwr));
    if (err.indexOf("!") > 0) { 
      System.err.println(err);
      if (retMap != null)
        retMap.put("error", err);
      return null;
    }
    if (retLst == null && retMap == null) {
      System.out.println("CLEG transform: " + PT.join(tokens, '>', 0));
      cleg = data.abcFor(data.trMat);
      System.out.println("CLEG transform: " + tokens[0] + ">" + cleg + ">"
          + tokens[tokens.length - 1]);
    }
    return data.trMat;
  }

  private static class AssignedSGParams {

    final Viewer vwr;
    final boolean mkCalcOnly;
    /**
     * special case where the input is just ".", which means "of the current
     * space group"
     */
    final boolean mkIsAssign;
    final SB mkSb;

    boolean mkIgnoreAllSettings;
    SymmetryInterface mkSym00;
    BS mkBitset;
    Object mkParamsOrUC;
    boolean mkWasNode;
    int mkIndex;

    AssignedSGParams(Viewer vwr) {
      this.vwr = vwr;
      mkCalcOnly = true;
      mkIsAssign = false;
      mkSb = null;
    }

    AssignedSGParams(Viewer vwr, SymmetryInterface sym00, BS bs,
        Object paramsOrUC, int index, boolean ignoreAllSettings, SB sb,
        boolean isAssign) {
      this.vwr = vwr;
      this.mkCalcOnly = false;
      this.mkIndex = index;
      this.mkSym00 = sym00;
      this.mkBitset = bs;
      this.mkParamsOrUC = paramsOrUC;
      this.mkIgnoreAllSettings = ignoreAllSettings;
      this.mkSb = sb;
      this.mkIsAssign = isAssign;
    }

  }

  public CLEG() {
    // for instantiation only
  }


  /**
   * Entry point for 
   * @param vwr
   * @param bs
   * @param cleg
   * @param paramsOrUC
   * @param sb
   * @return log message, which indicates an error if ending with "!"
   */
  String transformSpaceGroup(Viewer vwr, BS bs, String cleg,
                                           Object paramsOrUC, SB sb) {
    SymmetryInterface sym0 = vwr.getCurrentUnitCell();
    SymmetryInterface sym = vwr.getOperativeSymmetry();
    if (sym0 != null && sym != sym0)
      sym.getUnitCell(sym0.getV0abc(null, null), false, "modelkit");
    // change << to >super> 
    if (cleg.indexOf("<") >= 0) {
      cleg = PT.rep(cleg, "<<", ">super>").replace('<', '>');
    }
    // allow unterminated to mean no change in space group
    if (cleg.length() == 0 || cleg.endsWith(">"))
      cleg += ".";
    ClegData data = new ClegData(vwr.getSymTemp(), PT.split(cleg, ">"));
    AssignedSGParams asgParams = new AssignedSGParams(vwr, sym, bs, paramsOrUC, 0,
        false, sb, cleg.equals("."));
    String ret = assignSpaceGroup(data, asgParams);
    if (ret.endsWith("!"))
      return ret;
    if (asgParams.mkIsAssign)
      sb.append(ret);
    return ret;
  }

  /**
   * Assign the space group and associated unit cell from the MODELKIT
   * SPACEGROUP command.
   * 
   * All aspects depend upon having one of:
   * 
   * - a space group number: n (1-230); will be converted to n.1
   * 
   * - a valid n.m Jmol-recognized ITA setting: "13.3"
   * 
   * - an ITA space group number with a transform: "10:c,a,b"
   * 
   * - a valid jmolId such as "3:b", which will be converted to an ITA n.m
   * setting
   * 
   * - (hopefully unambiguous) Hermann-Mauguin symbol, such as "p 4/n b m :2"
   * 
   * jmolId and H-M symbols will be converted to ITA n.m format.
   * 
   * Options include two basic forms:
   * 
   * 1. non-CLEG format. Just the space group identifier: "13.3", "3:b", etc.
   * with a UNITCELL option
   * 
   * MODELKIT SPACEGROUP "P 1 2/1 1" UNITCELL <unit cell description>
   * 
   * where <unit cell description> is one of:
   * 
   * - [a b c alpha beta gamma]
   * 
   * - [ origin va vb vc ]
   * 
   * - a transform such as "a,b,c;0,0,1/2"
   * 
   * The unit cell is absolute or, if a transform, applied to the current unit
   * cell.
   * 
   * It is assumed in this case that the given unit cell is the cell for the
   * setting descibed. The standard-to-desired setting of the name is not
   * applied to the unit cell.
   * 
   * 3. CLEG format
   * 
   * identifier
   * 
   * identifier [> transform ]n > identifier
   * 
   * identifier > [ [transform >]n [> identifier ]m ]p > identifier
   * 
   * for example:
   * 
   * 
   * 
   * MODELKIT SPACEGROUP "13"
   * 
   * MODELKIT SPACEGROUP "13:-a-c,b,a"
   * 
   * MODELKIT SPACEGROUP "P 1 2/n 1"
   *
   * MODELKIT SPACEGROUP "P 2/n"
   *
   * MODELKIT SPACEGROUP "13.2" // second in Jmol's list of ITA settings for
   * space group 13
   * 
   * MODELKIT SPACEGROUP "13:b2" // Jmol code
   * 
   * MODELKIT SPACEGROUP "10:b,c,a > a-c,b,2c;0,0,1/2 > 13:a,-a-c,b" // fix -
   * this was to c2
   * 
   * Note that there are two possibilities, regarding current model to start of
   * chain, and start of chain to end of chain:
   *
   * 1) The space group number does not change (this a SETTING change)
   * 
   * 2) The space group number changes (this is a group >> subgroup or a group
   * >> supergroup change.
   * 
   *
   * 
   * Identifiers with settings are treated as follows:
   * 
   * - If the space group number is UNCHANGED, for example
   * 
   * 10 >> 10:c,a,b
   * 
   * then the final setting is applied to the unit cell as P * UC, and the space
   * group operations { R } are adjusted using (!P) * R * P, where P is the
   * transform of the setting and !P is its inverse.
   * 
   * Note that there is an implicit transform from the CURRENT unit cell and
   * space group when the two have the same space group number:
   * 
   * current == 10:b,c,a
   * 
   * MODELKIT SPACEGROUP 10:c,b,a ...
   * 
   * Here the current setting will be processed with a transform that brings it
   * from 10:b,c,a to 10:c,b,a as:
   * 
   * MODELKIT SPACEGROUP "10:b,c,a > !b,c,a > 10:a,b,c > c,a,b > 10:c,a,b"
   *
   * Note that this allows for the direct use of Hermann-Mauguin notation. In
   * the case of H-M settings WITHIN THE SAME SPACE GROUP, this can be condensed
   * to:
   * 
   * MODELKIT P 1 1 2/m >> P 2/m 1 1
   * 
   * where ">>" means "you figure it out, Jmol." Basically, this method will
   * interpret ">>" to mean "switch settings" and will implement that as
   * 
   * P 1 1 2/m > !b,c,a > c,a,b > P 2/m 1 1
   * 
   * 
   * Subgroups and Supergroups
   * 
   * - If the space group number is CHANGED, for example,
   * 
   * "10 > ... transforms ... > 13:a,-a-c,b"
   * 
   * or the current space group of the model is 10, then the final setting is
   * only applied to the operators of the final space group. Any change in unit
   * cell setting is assumed to already be accounted for by the indicated
   * transforms.
   * 
   * For example, in the case from from P 2/m to P 2/c, no unambiguous implicit
   * transformation is possible, and an error message will be returned
   * indicating that a transform is required if none is presented. Such
   * group-group transforms an be found at the BCS, specifically for ITA default
   * settings. Thus, we might write:
   * 
   * P 1 1 2/m >> 10 > a-c,b,2c;0,0,1/2 > 13 >> P 2/c 1 1
   * 
   * where we are EXPLICITY indicating that the standard-to-standard ITA
   * transform:
   * 
   * 10 > a-c,b,2c;0,0,1/2 > 13
   * 
   * as part of the CLEG sequence. In this case, the indicated transforms
   * between settings will be added to the sequence.
   * 
   * This ensures that the desired specific conversion is used.
   * 
   * Additional aspects of the notation include:
   * 
   * ! prefix - "not" or "inverse of"
   * 
   * :r setting - abbreviation for
   * :2/3a+1/3b+1/3c,-1/3a+1/3b+1/3c,-1/3a-2/3b+1/3c
   * 
   * :h setting - abbreviation for "!r":
   * :!2/3a+1/3b+1/3c,-1/3a+1/3b+1/3c,-1/3a-2/3b+1/3c
   * 
   * [xx xx xx] - Hall notation, such as [P 2/1], can also be used. See
   * http://cci.lbl.gov/sginfo/itvb_2001_table_a1427_hall_symbols.html
   * 
   * Note that this allows also for fully transformed Hall notation as a
   * setting. For example:
   * 
   * 85.4 same as [-p 4a"]:a-b,a+b,c;0,1/4,-1/8
   * 
   * Were we are using the Hall symbol for the standard setting of space group
   * 85 in this case, and applying the appropriate tranform to 85.4 (P 42/m).
   * 
   * In all cases, the two actions performed include:
   * 
   * - replacing the current unit cell
   * 
   * - replacing the space group
   * 
   * Note that in cases where there is a series of transforms, the initial
   * action of this method is to convert that sequence to a single overall
   * transform first. If any identifiers are in the chain other than the first
   * or last position, they are not checked and are simply ignored.
   * 
   * @param data
   * @param asgParams
   * @return message or error (message ending in "!")
   */
  private static String assignSpaceGroup(ClegData data,
                                         AssignedSGParams asgParams) {

    // case 1: modelkit zap SPACEGROUP sg+setting
    // display in standard cell orientation, just like loading a file
    //   create a compatible unit cell, then fill it with the space group.
    //   all setting details are handled already. 
    //  
    // case 2: modelkit SPACEGROUP sg+setting
    // determine the current spacegroup transform and remove it
    // 

    // modelkit spacegroup 10
    // modelkit spacegroup 10 unitcell [a b c alpha beta gamma]
    // modelkit spacegroup 10:b,c,a
    // modelkit zap spacegroup 10:b,c,a  unitcell [a b c alpha beta gamma]
    //   in this case, we set the unitcell on the SECOND round.

    // modelkit spacegroup 10>a,b,c>13
    // modelkit spacegroup >a,b,c>13
    // modelkit spacegroup 10:b,c,a > -b+c,-b-c,a:0,1/2,0 > 13:a,-a-c,b

    // general initialization

    Viewer vwr = asgParams.vwr;
    int index = asgParams.mkIndex;
    String[] tokens = data.tokens;
    // that is, we are adding a reference token before a setting

    boolean initializing = (index < 0);
    if (initializing) {
      index = 0;
    }
    if (index >= tokens.length)
      return "invalid CLEG expression!";
    boolean haveUCParams = (asgParams.mkParamsOrUC != null);
    if (tokens.length > 1 && haveUCParams) {
      return "invalid syntax - can't mix transformations and UNITCELL option!";
    }
    if (index == 0 && !initializing)
      standardizeTokens(tokens, false);

    boolean nextTransformExplicit = (tokens.length > index + 1
        && isTransformOnly(tokens[index + 1])
        && tokens[index + 1].length() > 0);
    String token = tokens[index].trim();
    boolean isDot = token.equals(".");
    if (index == 0) {
      if (token.length() == 0) {
        // >> or "."
        // but not >h>...
        if (asgParams.mkSym00 == null) {
          return "no starting space group for CLEG!";
        }
        if (!asgParams.mkIsAssign) {
          tokens[0] = asgParams.mkSym00.getSpaceGroupClegId();
          asgParams.mkIndex = 1;
          asgParams.mkWasNode = true;
          asgParams.mkIgnoreAllSettings = nextTransformExplicit;
          return assignSpaceGroup(data, asgParams);
        }
      }
      if (asgParams.mkIsAssign) {
        if (asgParams.mkSym00 == null)
          return "no starting space group for calculation!";
      }
    }

    boolean isCalc = (getCalcType(token) != null);

    //System.out.println("token " + token + " wasNode " + data.wasNode);

    if ((isCalc || isTransformOnly(token)) != asgParams.mkWasNode) {
      return "invalid CLEG expression, not node>transform>node>transform>....!";
    }
    asgParams.mkWasNode = !asgParams.mkWasNode;
    String calcNext = (isCalc ? token : null);
    if (isCalc) {
      token = tokens[++index].trim();
    }

    // check for "...>..>.", which picks up the previous node name. (Not sure how that works)

    boolean isFinal = (index == tokens.length - 1);
    if (isFinal && isDot && data.getPrevNode() != null) {
      token = data.getPrevNode().getCleanITAName();
    }
    int pt = token.lastIndexOf(":"); // could be "154:_2" or "R 3 2 :" or 5:a,b,c
    boolean zapped = (asgParams.mkSym00 == null);
    boolean isUnknown = false;
    boolean haveTransform = isTransform(token, false); // r !r h !h but not :r or :h
    boolean isSetting = (haveTransform && pt >= 0);
    boolean isTransformOnly = (haveTransform && !isSetting);
    boolean restarted = false;
    boolean ignoreNodeTransform = false;
    boolean ignoreFirstSetting = false;
    SymmetryInterface sym = data.sym;

    // initiaize a node if this is not just a transformation
    ClegNode node = null;
    if (!isTransformOnly) {
      data.sym = sym;
      node = new ClegNode(data, index, token);
      if (data.errString != null)
        return data.errString;
    }

    // first time only...

    if (data.getPrevNode() == null) {
      // first time through
      if (!checkFullSyntax(tokens, sym, allow300))
        return "invalid CLEG expression!";

      // check for a zap
      if (!asgParams.mkCalcOnly && !isTransformOnly && zapped
          && !node.isDefaultSetting()) {
        // modelkit zap spacegroup nnn.m or nnn:ttttt // What about non-reference H-M??
        String ita = node.myIta;
        // easiest is to restart with the default configuration and unit cell
        // modelkit zap spacegroup ita ....
        String[] cleg = new String[] { node.specialPrefix + ita };
        AssignedSGParams paramsInit = (asgParams.mkCalcOnly
            ? new AssignedSGParams(vwr)
            : new AssignedSGParams(vwr, null, null, asgParams.mkParamsOrUC, -1,
                true, asgParams.mkSb, false));
        ClegData cdInit = new ClegData(vwr.getSymTemp(), cleg);
        String err = assignSpaceGroup(cdInit, paramsInit);
        if (err.endsWith("!"))
          return err;
        if (asgParams.mkCalcOnly) {
          sym = asgParams.mkSym00 = cdInit.sym;
          data.trMat = cdInit.trMat;
        } else {
          asgParams.mkSym00 = vwr.getOperativeSymmetry();
        }
        if (asgParams.mkSym00 == null)
          return  "CLEG spacegroup initialization error!";
        zapped = false;
        restarted = true;
      }

      // check for ignoring setting transform due to explicit conversion
      // modelkit spacegroup 10:c,a,b>a,b,2c>....
      // modelkit spacegroup 10>a,b,2c>....
      // moswlkir spacegroup  >h>...

      // The first setting in the chain may have to be reversed, but . 
      // we ignore the first setting if we don't need to move the unit cell to it. 
      // :
      ignoreFirstSetting = (index == 0 // it's the first token and either...
          && (!asgParams.mkCalcOnly // this isn't just a calculation
              && !zapped // and the model has a unit cell
              && nextTransformExplicit // and there is an explicit transformation coming up next
              // ...or ...
              || asgParams.mkIsAssign // we are specifically checking the current space group
          ));
      String ita0 = (zapped ? null : asgParams.mkSym00.getSpaceGroupClegId());
      String trm0 = null;
      if (!zapped) {
        if (ita0 == null || ita0.equals("0")) {
        } else {
          int pt1 = ita0.indexOf(":");
          if (pt1 > 0) {
            trm0 = ita0.substring(pt1 + 1);
            ita0 = ita0.substring(0, pt1);
            pt1 = -1;
          }
          if (ignoreFirstSetting) {
            trm0 = (String) asgParams.mkSym00
                .getSpaceGroupInfoObj("itaTransform", null, false, false);
          }
        }
      }
      // all times, not just the first
      if (data.trMat == null) {
        data.trMat = new M4d();
        data.trMat.setIdentity();
      }
      if (!asgParams.mkIsAssign) {
        data.sym = sym;
        data.setPrevNode(new ClegNode(data, -1, ita0 == null ? null : ita0 + ":" + trm0));
        if (asgParams.mkIgnoreAllSettings)
          data.getPrevNode().disable();
        if (!data.getPrevNode().update(data))
          return data.errString;
      }
    }
    if (isCalc) {
      if (!data.getPrevNode().setCalcNext(data, calcNext)) {
        return data.errString;
      }      
    }
    if (isTransformOnly) {
      if (isFinal) {
        isUnknown = true;// now what?
        return "CLEG pathway is incomplete!";
      }
      // not a setting, not a node; could be >>
      if (token.length() > 0)
        data.addTransform(index, token);
      // ITERATE  ...
      ++asgParams.mkIndex;
      return assignSpaceGroup(data, asgParams);
    }

    // thus ends consideration of chain of transforms

    // first or last. 

    //boolean applySetting = (!asgParams.mkCalcOnly || index > 0);
    if (!ignoreFirstSetting) {
      data.sym = sym;
      if (ignoreNodeTransform)
        node.disable();
      //      if (!applySetting)
      //        node.disableSetting();
      if (!node.update(data))
        return data.errString;
      if (isFinal && isDot) {
        data.setNodeTransform(node);
      }
      data.updateTokens(node);
    }
    if (!isFinal) {
      asgParams.mkIndex++;
      data.setPrevNode(node);
      return assignSpaceGroup(data, asgParams);
    }

    // handle parameters

    double[] params = null;
    T3d[] oabc = null;
    T3d origin = null;

    params = (!haveUCParams || !AU.isAD(asgParams.mkParamsOrUC) ? null
        : (double[]) asgParams.mkParamsOrUC);
    if (zapped) {
      sym.setUnitCellFromParams(
          params == null ? new double[] { 10, 10, 10, 90, 90, 90 } : params,
          false, Double.NaN);
      asgParams.mkParamsOrUC = null;
      haveUCParams = false;
    }
    if (haveUCParams) {
      // have UNITCELL params, either [a b c..] or [o a b c] or 'a,b,c:...'
      if (AU.isAD(asgParams.mkParamsOrUC)) {
        params = (double[]) asgParams.mkParamsOrUC;
      } else {
        oabc = (T3d[]) asgParams.mkParamsOrUC;
        origin = oabc[0];
      }
    } else if (!zapped) {
      sym = data.sym = asgParams.mkSym00;
      if (data.trMat == null) {
        data.trMat = new M4d();
        data.trMat.setIdentity();
      }
      oabc = sym.getV0abc(new Object[] { data.trMat }, null);
      origin = oabc[0];
    }
    if (oabc != null) {
      params = sym.getUnitCell(oabc, false, "assign").getUnitCellParams();
      if (origin == null)
        origin = oabc[0];
    }
    // ready to roll....
    boolean isP1 = (token.equalsIgnoreCase("P1") || token.equals("ITA/1.1"));
    // TODO: P1 is only for space groups need isP1(token) or node.isP1
    try {
      BS bsAtoms;
      boolean noAtoms;
      int modelIndex = -1;
      if (asgParams.mkCalcOnly) {
        asgParams.mkBitset = bsAtoms = new BS();
        noAtoms = true;
      } else {
        if (asgParams.mkBitset != null && asgParams.mkBitset.isEmpty())
          return "no atoms specified!";
        // limit the atoms to this model if bs is null
        bsAtoms = vwr.getThisModelAtoms();
        BS bsCell = (isP1 ? bsAtoms
            : SV.getBitSet(
                vwr.evaluateExpressionAsVariable("{within(unitcell)}"), true));
        if (asgParams.mkBitset == null) {
          asgParams.mkBitset = bsAtoms;
        }
        if (asgParams.mkBitset != null) {
          bsAtoms.and(asgParams.mkBitset);
          if (!isP1)
            bsAtoms.and(bsCell);
        }
        noAtoms = bsAtoms.isEmpty();
        modelIndex = (noAtoms && vwr.am.cmi < 0 ? 0
            : noAtoms ? vwr.am.cmi
                : vwr.ms.at[bsAtoms.nextSetBit(0)].getModelIndex());
        if (!asgParams.mkIsAssign)
          vwr.ms.getModelAuxiliaryInfo(modelIndex)
              .remove(JC.INFO_SPACE_GROUP_INFO);
      }

      // old code...
      // why do this? It treats the supercell as the unit cell. 
      // is that what we want?

      //      T3d m = sym.getUnitCellMultiplier();
      //      if (m != null && m.z == 1) {
      //        m.z = 0;
      //      }

      if (!zapped && !asgParams.mkIsAssign) {
        sym.replaceTransformMatrix(data.trMat);
        // storing trm0 for SpaceGroupFinder        
      }
      if (params == null)
        params = sym.getUnitCellMultiplied().getUnitCellParams();

      isUnknown |= asgParams.mkIsAssign;
      @SuppressWarnings("unchecked")
      Map<String, Object> sgInfo = (noAtoms && isUnknown ? null
          : (Map<String, Object>) vwr.findSpaceGroup(sym,
              isUnknown ? bsAtoms : null, isUnknown ? null : node.getName(),
              params, origin, oabc,
              JC.SG_IS_ASSIGN | (asgParams.mkCalcOnly ? JC.SG_CALC_ONLY : 0)
                  | (!zapped || asgParams.mkIsAssign ? 0
                      : JC.SG_FROM_SCRATCH)));

      if (sgInfo == null) {
        return "Space group " + node.getName()
            + " is unknown or not compatible!";
      }

      if (oabc == null || zapped)
         oabc = (P3d[]) sgInfo.get("unitcell");
      token = (String) sgInfo.get("name");
      String jmolId = (String) sgInfo.get("jmolId");
      BS basis = (BS) sgInfo.get("basis");
      SpaceGroup sg = (SpaceGroup) sgInfo.remove("sg");
      sym.getUnitCell(oabc, false, null);
      sym.setSpaceGroupTo(sg == null ? jmolId : sg);
      sym.setSpaceGroupName(token);
      if (asgParams.mkCalcOnly) {
        data.sym = sym;
        return "OK";
      }
      // not a calculation
      if (basis == null) {
        basis = sym.removeDuplicates(vwr.ms, bsAtoms, true);
      }
      vwr.ms.setSpaceGroup(modelIndex, sym, basis);
      if (asgParams.mkIsAssign) {
        return token;
      }
      if (zapped || restarted) {
        vwr.runScript(
            "unitcell on; center unitcell;axes unitcell; axes 0.1; axes on;"
                + "set perspectivedepth false;moveto 0 axis c1;draw delete;show spacegroup");
      }

      // don't change the first line of this message -- it will be used in packing.

      String finalTransform = data.abcFor(data.trMat);
      if (!initializing) {
        standardizeTokens(tokens, true);
        String msg = PT.join(tokens, '>', 0)
            + (basis.isEmpty() ? "" : "\n basis=" + basis);
        System.out.println("CLEG=" + msg);
        asgParams.mkSb.append(msg).append("\n");
      }
      return finalTransform;
    } catch (Exception e) {
      if (!Viewer.isJS)
        e.printStackTrace();
      return e.getMessage() + "!";
    }
  }

  
}
