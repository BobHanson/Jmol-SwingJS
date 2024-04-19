
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
import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.SymmetryInterface;
import org.jmol.util.Logger;
import org.jmol.viewer.JC;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.M4d;
import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.SB;

/**
 * 
 * A general class to deal with Hermann-Mauguin or Hall names
 * 
 * Bob Hanson 9/2006
 * 
 * references: International Tables for Crystallography Vol. A. (2002) 
 *
 * http://www.iucr.org/iucr-top/cif/cifdic_html/1/cif_core.dic/Ispace_group_symop_operation_xyz.html
 * http://www.iucr.org/iucr-top/cif/cifdic_html/1/cif_core.dic/Isymmetry_equiv_pos_as_xyz.html
 *
 * Hall symbols:
 * 
 * https://cci.lbl.gov/sginfo/hall_symbols.html
 * 
 * and
 * 
 * https://cci.lbl.gov/cctbx/explore_symmetry.html
 * 
 * (-)L   [N_A^T_1]   [N_A^T_2]   ...  [N_A^T_P]   V(Nx Ny Nz)
 * 
 * lattice types S and T are not supported here
 * 
 * data table is from Syd Hall, private email, 9/4/2006, 
 * amended using * ** to indicate nonstandard H-M symbols or full names  
 * 
 * amended 2024.03.24 to add several ITA settings; full set of ITA settings are now encoded. 
 * 
 * NEVER ACCESS THESE METHODS DIRECTLY! ONLY THROUGH CLASS Symmetry
 * 
 * 
 *
 */

public class SpaceGroup implements Cloneable {

  private static final String NEW_HALL_GROUP     = "0;--;--;0;--;--;";
  private static final String NEW_NO_HALL_GROUP  = "0;--;--;0;--;--;--";
  private static final String SG_NONE = "--";
  static final String NO_NAME = "-- [--]"; // don't know if this is possible

  private final static int SG_ITA = -2;

  public SymmetryOperation[] operations;
  SymmetryOperation[] finalOperations;
  SymmetryOperation[] allOperations;
  Map<String, Integer> xyzList;
  char uniqueAxis = '\0'; 
  char axisChoice = '\0';
  //int cellChoice; 
  //int originChoice;
  String itaNumber;  // "3"
  String jmolId;     // "3:a"
  String clegId;
  int operationCount;
  int latticeOp = -1;
  boolean isBio;
  char latticeType = 'P'; // P A B C I F
  String itaTransform;
  /**
   * index in cleg_settings.tab; "-" if in Jmol's list but not at ITA -- 152:_2 and 154:_2
   */

  private String itaIndex;
  
 
  private int index;
  private int derivedIndex = -1;
  
  public boolean isSSG;
  private String name = "unknown!";
  private String hallSymbol;
  private String crystalClass; //schoenfliesSymbol;
  private String hmSymbol;   private String jmolIdExt;  // "a" in "3:a"
  private HallInfo hallInfo;
  private int latticeParameter;
  private int modDim;
  private boolean doNormalize = true;
  private Object info;
  private Integer nHallOperators;
  private String hmSymbolFull; 
  private String hmSymbolExt;
  private String hmSymbolAbbr;
  private String hmSymbolAlternative;
  private String hmSymbolAbbrShort;
  private char ambiguityType = '\0';

  
  private SpaceGroup setFrom(SpaceGroup sg, boolean isITA) {
    if (isITA) {
      setName(sg.itaNumber.equals("0") ? clegId : "HM:" + sg.hmSymbolFull + " #" + clegId);
      derivedIndex = SG_ITA; // prevents replacement in finalizeOperations
    } else {
      setName(sg.getName());
      derivedIndex = sg.index;
    }
    clegId = sg.clegId;
    itaIndex = sg.itaIndex;
    crystalClass = sg.crystalClass;
    hallSymbol = sg.hallSymbol;
    hmSymbol = sg.hmSymbol;
    hmSymbolAbbr = sg.hmSymbolAbbr;
    hmSymbolAbbrShort = sg.hmSymbolAbbrShort;
    hmSymbolAlternative = sg.hmSymbolAlternative;
    hmSymbolExt = sg.hmSymbolExt;
    hmSymbolFull = sg.hmSymbolFull;
    itaNumber = sg.itaNumber;
    itaTransform = sg.itaTransform;
    jmolId = sg.jmolId;
    jmolIdExt = sg.jmolIdExt;
    latticeType = sg.latticeType;
    return this;
  }

  static SpaceGroup getNull(boolean doInit, boolean doNormalize, boolean doFinalize) {
    //  getSpaceGroups();
    SpaceGroup sg = new SpaceGroup(-1, null, doInit);
    sg.doNormalize = doNormalize;
    if (doFinalize)
      sg.setFinalOperations();
    return sg;
  }
  
  private SpaceGroup(int index, String strData, boolean doInit) {
    ++sgIndex; // increment even if not using
    if (index < 0)
      index = sgIndex;
    this.index = index;
    init(doInit && strData == null);
    if (doInit && strData != null)
      buildSelf(strData);
  }

  private void init(boolean addXYZ) {
    xyzList = new Hashtable<String, Integer>();
    operationCount = 0;
    if (addXYZ)
      addSymmetry("x,y,z", 0, false);
  }
  
  public static SpaceGroup createSpaceGroup(int desiredSpaceGroupIndex,
                                                  String name,
                                                  Object data, int modDim) {
    SpaceGroup sg = null;
    if (desiredSpaceGroupIndex >= 0) {
      sg = getSpaceGroups()[desiredSpaceGroupIndex];
    } else {
      if (data instanceof Lst<?>)
        sg = createSGFromList(name, (Lst<?>) data); 
      else
        sg = determineSpaceGroupNA(name, (double[]) data);
      if (sg == null)
        sg = createSpaceGroupN(modDim <= 0 ? name: "x1,x2,x3,x4,x5,x6,x7,x8,x9".substring(0, modDim * 3 + 8));
    }
    if (sg != null)
      sg.generateAllOperators(null);
    return sg;
  }

  SpaceGroup cloneInfoTo(SpaceGroup sg0) {
    try {
      SpaceGroup sg = (SpaceGroup) clone();
      sg.operations = sg0.operations;
      sg.finalOperations = sg0.finalOperations;
      sg.xyzList = sg0.xyzList;
      return sg;
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

  public String getItaIndex() {
    return (itaIndex != null && !SG_NONE.equals(itaIndex) 
        ? itaIndex 
            : !"0".equals(itaNumber) ? itaNumber : !SG_NONE.equals(hallSymbol) 
                ? "[" + hallSymbol + "]" 
                    : "?");
  }
  
  public int getIndex() {
    return (derivedIndex >= 0 ? derivedIndex : index);
  }
 
  /**
   * 
   * @param name
   * @param data Lst<SymmetryOperation> or Lst<M4d>
   * @return a new SpaceGroup if successful or null
   */
  private static SpaceGroup createSGFromList(String name, Lst<?> data) {
    // try unconventional Hall symbol
    SpaceGroup sg = new SpaceGroup(-1, NEW_NO_HALL_GROUP, true);
    sg.doNormalize = false;
    sg.setName(name);
    int n = data.size();
    for (int i = 0; i < n; i++) {
      Object operation = data.get(i);
      if (operation instanceof SymmetryOperation) {
        SymmetryOperation op = (SymmetryOperation) operation;
        int iop = sg.addOp(op, op.xyz, false);
        sg.operations[iop].setTimeReversal(op.timeReversal);
      } else {
        sg.addSymmetrySM("xyz matrix:" + operation, (M4d) operation);
      }
    }
    SpaceGroup sgn = sg.getDerivedSpaceGroup();
    if (sgn != null)
      sg = sgn;
    return sg;
  }

  /**
   * 
   * @param xyz
   * @param opId generally 0; -1 for subsystems
   * @param allowScaling generally false; true for subsystems
   * @return index
   */
  public int addSymmetry(String xyz, int opId, boolean allowScaling) {
    xyz = xyz.toLowerCase();
    return (xyz.indexOf("[[") < 0 && xyz.indexOf("x4") < 0 && xyz.indexOf(";") < 0 &&
        (xyz.indexOf("x") < 0 || xyz.indexOf("y") < 0 || xyz.indexOf("z") < 0) 
        ? -1 : addOperation(xyz, opId, allowScaling));
  }

  void setFinalOperations() {
    setFinalOperationsForAtoms(3, null, 0, 0, false);
  }

  void setFinalOperationsForAtoms(int dim, P3d[] atoms, int atomIndex, int count,
                                  boolean doNormalize) {
    //from AtomSetCollection.applySymmetry only
    if (hallInfo == null && latticeParameter != 0) {
      HallInfo h = new HallInfo(
          HallTranslation.getHallLatticeEquivalent(latticeParameter));
      generateAllOperators(h);
      //doNormalize = false;  // why this here?
    }
    finalOperations = null;
    isBio = (name.indexOf("bio") >= 0);
    if (!isBio && index >= getSpaceGroups().length && name.indexOf("SSG:") < 0
        && name.indexOf("[subsystem") < 0) {
      SpaceGroup sg = getDerivedSpaceGroup();
      if (sg != null && sg != this) {
        setFrom(sg, false);
      }
    }
    if (operationCount == 0)
      addOperation("x,y,z", 1, false);
    finalOperations = new SymmetryOperation[operationCount];
    SymmetryOperation op = null;
    boolean doOffset = (doNormalize && count > 0 && atoms != null);
    if (doOffset) {
      // we must apply this first to "x,y,z" JUST IN CASE the 
      // model center itself is out of bounds, because we want
      // NO operation for "x,y,z". This requires REDEFINING ATOM LOCATIONS
      op = finalOperations[0] = new SymmetryOperation(operations[0], 0, true);
      if (op.sigma == null)
        SymmetryOperation.normalizeOperationToCentroid(dim, op, atoms, atomIndex, count);
      P3d atom = atoms[atomIndex];
      P3d c = P3d.newP(atom);
      op.rotTrans(c);
      if (c.distance(atom) > 0.0001f) {
        // not cartesian, but this is OK here
        for (int i = 0; i < count; i++) {
          atom = atoms[atomIndex + i];
          c.setT(atom);
          op.rotTrans(c);
          atom.setT(c);
        }
      }
      if (!doNormalize)
        op = null;
    }
    for (int i = 0; i < operationCount; i++) {
      // not necessary to duplicate first operation if we have it already
      if (i > 0 || op == null) {
        op = finalOperations[i] = new SymmetryOperation(operations[i], 0,
            doNormalize);
      }
      if (doOffset && op.sigma == null) {
        SymmetryOperation.normalizeOperationToCentroid(dim, op, atoms, atomIndex, count);
      }
      op.getCentering();
    }
  }

  int getOperationCount() {
    if (finalOperations == null)
      setFinalOperations();
    return finalOperations.length;
  }

  M4d getOperation(int i) {
    return finalOperations[i];
  }

  /**
   *  This call retrieves the count of all operations, including operations that 
   *  arise from adding lattice translations.
   *  
   * @return allOperations.length
   */
  int getAdditionalOperationsCount() {
    if (finalOperations == null)
      setFinalOperations();
    if (allOperations == null) {
      allOperations = SymmetryOperation.getAdditionalOperations(finalOperations);
    }
    return allOperations.length - getOperationCount();
  }
  
  M4d[] getAdditionalOperations() {
    getAdditionalOperationsCount();
    return allOperations;
  }
 

  M4d getAllOperation(int i) {
    return allOperations[i];
  }

  String getXyz(int i, boolean doNormalize) {
  return (finalOperations == null ? operations[i].getXyz(doNormalize)
      : finalOperations[i].getXyz(doNormalize));
  }

  public static SpaceGroup findSpaceGroupFromXYZ(String xyzList) {
    return findOrCreateSpaceGroupXYZ(xyzList, false);
  }


  static Object getInfo(SpaceGroup sg, String spaceGroup,
                        double[] params, boolean asMap, boolean andNonstandard) {
    try {
    if (sg != null && sg.index >= SG.length) {
      SpaceGroup sgDerived = findSpaceGroup(sg.operationCount, sg.getCanonicalSeitzList());
      if (sgDerived != null)
        sg = sgDerived;   
    }
    if (params != null) {
      if (sg == null) {
        if (spaceGroup.indexOf("[") >= 0)
          spaceGroup = spaceGroup.substring(0, spaceGroup.indexOf("[")).trim();
        if (spaceGroup.equals("unspecified!"))
          return "no space group identified in file";
        sg = SpaceGroup.determineSpaceGroupNA(spaceGroup, params);
      }
    } else if (spaceGroup.equalsIgnoreCase("ALL")) {
      return SpaceGroup.dumpAll(asMap);
    } else if (spaceGroup.equalsIgnoreCase("MAP")) {
      return SpaceGroup.dumpAll(true);
    } else if (spaceGroup.equalsIgnoreCase("ALLSEITZ")) {
      return SpaceGroup.dumpAllSeitz();
    } else {
      sg = SpaceGroup.determineSpaceGroupN(spaceGroup);
    }
    if (sg == null) {
      SpaceGroup sgFound = SpaceGroup.createSpaceGroupN(spaceGroup);
      if (sgFound != null)
        sgFound = findSpaceGroup(sgFound.operationCount, sgFound.getCanonicalSeitzList());
      if (sgFound != null)
        sg = sgFound;
    } 
    if (sg != null) {
       if (asMap) {
        return sg.dumpInfoObj();
      }
      SB sb = new SB();
      while (sg != null) {
        // I don't know why there would be multiples here
        sb.append(sg.dumpInfo());
        if (sg.index >= SG.length || !andNonstandard)
          break;
        sg = SpaceGroup.determineSpaceGroupNS(spaceGroup, sg);
      }
      return sb.toString();
    }
    return asMap ? null : "?";
    } catch (Exception e) {
      return "?";
    }
  }

  /**
   * 
   * @return detailed information
   */
  public String dumpInfo() {
    Object info = dumpCanonicalSeitzList();
    if (info instanceof SpaceGroup)
      return ((SpaceGroup) info).dumpInfo();
    SB sb = new SB().append("\nHermann-Mauguin symbol: ");
    if (hmSymbol == null || hmSymbolExt == null)
      sb.append("?");
    else
      sb.append(hmSymbol)
          .append(hmSymbolExt.length() > 0 ? ":" + hmSymbolExt : "");
    if (itaNumber != null) {
      sb.append("\ninternational table number: ").append(itaNumber)
          .append(itaTransform != null ? ":" + itaTransform
              : "")
          .append("\ncrystal class: " + crystalClass);
    }
    if (jmolId != null) {
      sb.append("\nJmol_ID: ").append(jmolId).append(" ("+itaIndex+")");
    }
    sb.append("\n\n")
        .append(hallInfo == null ? "Hall symbol unknown" : Logger.debugging ?  hallInfo.dumpInfo() : "");
    sb.append("\n\n").appendI(operationCount).append(" operators")
        .append(hallInfo != null && !hallInfo.hallSymbol.equals(SG_NONE)
            ? " from Hall symbol " + hallInfo.hallSymbol + "  #"
                + jmolId
            : "")
        .append(": ");
    for (int i = 0; i < operationCount; i++) {
      sb.append("\n").append(operations[i].xyz);
    }

    //sb.append("\n\ncanonical Seitz: ").append((String) info)
    //sb.append("\n----------------------------------------------------\n");
    return sb.toString();
  }

  /**
   * 
   * @return detailed information
   */
  Object dumpInfoObj() {
    Object info = dumpCanonicalSeitzList();
    if (info instanceof SpaceGroup)
      return ((SpaceGroup) info).dumpInfoObj();
    Map<String, Object> map = new Hashtable<String, Object>();
    if (itaNumber != null && !itaNumber.equals("0")) {
      String s = (hmSymbol == null || hmSymbolExt == null ? "?" : hmSymbol + (hmSymbolExt.length() > 0 ? ":" + hmSymbolExt : ""));
      map.put("HermannMauguinSymbol", s);
      map.put("ita", Integer.valueOf(PT.parseInt(itaNumber)));
      map.put("itaIndex", itaIndex == null ? "n/a" : itaIndex);
      map.put("clegId", itaIndex == null ? "n/a" : clegId);
      if (jmolId != null)
        map.put("jmolId", jmolId);
      map.put("crystalClass", crystalClass);
      map.put("operationCount", Integer.valueOf(operationCount));
    }
    Lst<Object> lst = new Lst<Object>();
    for (int i = 0; i < operationCount; i++) {
      lst.addLast(operations[i].xyz);
    }
    map.put("operationsXYZ", lst);
//    map.put("code", getCode());
    if (hallInfo != null && hallInfo.hallSymbol != null)
      map.put("HallSymbol", hallInfo.hallSymbol);
    return map;
  }

//  private String getCode() {
//    Map<String, int[]> map = new Hashtable<String, int[]>();
//    for (int i = 0; i < operationCount; i++) {
//      String code = operations[i].getCode();
//      int[] c = map.get(code);
//      if (c == null) {
//          map.put(code,  c = new int[] {0});
//      }
//      c[0]++;
//    }
//    Set<String> keys = map.keySet();
//    String[] a = keys.toArray(new String[keys.size()]);
//    Arrays.sort(a);
//    String s = "";
//    for (int i = 0; i < a.length; i++) {
//      s += a[i] + ":" + map.get(a[i])[0] + ";";
//    }
//    return s;
//  }

  String getName() {
    return name;  
  }
/*  
  int getLatticeParameter() {
    return latticeParameter;
  }
 
  char getLatticeCode() {
    return latticeCode;
  }
*/ 
  String getLatticeDesignation() {    
    return HallTranslation.getLatticeDesignation(latticeParameter);
  }  
 
  void setLatticeParam(int latticeParameter) {
    // this does not work, because it clears the operations. 
    // Wien2K and Shelx readers only
    // implication here is that we do NOT have a Hall symbol.
    // so we generate one.
    // The idea here is that we can represent any LATT number
    // as a simple set of rotation/translation operations
    this.latticeParameter = latticeParameter;
    if (latticeParameter > 10) // use negative
      this.latticeParameter = -HallTranslation.getLatticeIndex(HallTranslation.getLatticeCode(latticeParameter));
  }

  ///// private methods /////

  /**
   * 
   * @return either a String or a SpaceGroup, depending on index.
   */
  private Object dumpCanonicalSeitzList() {
    if (nHallOperators != null) {
      hallInfo = new HallInfo(hallSymbol);
      generateAllOperators(null);
    } 
    String s = getCanonicalSeitzList();
    if (index >= SG.length) {
      SpaceGroup sgDerived = findSpaceGroup(operationCount, s);
      if (sgDerived != null)
        return sgDerived.getCanonicalSeitzList();
    }
    return (index >= 0 && index < SG.length ? hallSymbol + " = " : "") + s;
  }
  
  /**
   * 
   * @return a known space group or null
   */
  SpaceGroup getDerivedSpaceGroup() {
    if (derivedIndex == SG_ITA  || index >= 0 && index < SG.length   
        || modDim > 0 || operations == null
        || operations.length == 0
        || operations[0].timeReversal != 0)
      return this;
    if (finalOperations != null)
      setFinalOperations();
    String s = getCanonicalSeitzList();
    return (s == null ? null : findSpaceGroup(operationCount, s));
  }

  private String getCanonicalSeitzList() {
    return getCanonicalSeitzForOperations(operations, operationCount);
  }

  static String getCanonicalSeitzForOperations(SymmetryOperation[] operations, int n) {
    String[] list = new String[n];
    for (int i = 0; i < n; i++)
      list[i] = SymmetryOperation.dumpSeitz(operations[i], true);
    Arrays.sort(list, 0, n);
    SB sb = new SB().append("\n[");
    for (int i = 0; i < n; i++)
      sb.append(list[i].replace('\t',' ').replace('\n',' ')).append("; ");
    sb.append("]");
    return sb.toString();
  }

  private synchronized static SpaceGroup findSpaceGroup(int opCount, String s) {
    getSpaceGroups();
    Lst<SpaceGroup> lst = htByOpCount.get(Integer.valueOf(opCount));
    if (lst != null)
      for (int i = 0, n = lst.size(); i < n; i++) {
        SpaceGroup sg = lst.get(i);
        if (getCanonicalSeitz(sg.index).indexOf(s) >= 0)
          return SG[sg.index];
      }
    return null;
  }

  private final static Object dumpAll(boolean asMap) {
    getSpaceGroups();
    if (asMap) {
    Lst<Object> info = new Lst<Object>();
    for (int i = 0; i < SG.length; i++)
      info.addLast(SG[i].dumpInfoObj());
      return info;
    }
     SB sb = new SB();
   for (int i = 0; i < SG.length; i++)
     sb.append("\n----------------------\n" + SG[i].dumpInfo());
   return sb.toString();
  }
  
  private final static String dumpAllSeitz() {
    getSpaceGroups();
    SB sb = new SB();
    for (int i = 0; i < SG.length; i++)
      sb.append("\n").appendO(getCanonicalSeitz(i));
    return sb.toString();
  }
   
  private static String[] canonicalSeitzList;
  
  private static String getCanonicalSeitz(int i) {
    if (canonicalSeitzList == null)
      canonicalSeitzList = new String[SG.length];
    String cs = canonicalSeitzList[i];
    return (cs == null ? canonicalSeitzList[i] = SG[i].dumpCanonicalSeitzList()
        .toString() : cs);
  }

  private void setLattice(char latticeCode, boolean isCentrosymmetric) {
    //this.latticeCode = latticeCode;
    latticeParameter = HallTranslation.getLatticeIndex(latticeCode);
    if (!isCentrosymmetric)
      latticeParameter = -latticeParameter;
  }
  
  final static SpaceGroup createSpaceGroupN(String name) {
    getSpaceGroups();
    name = name.trim();
    SpaceGroup sg = determineSpaceGroupN(name);
    HallInfo hallInfo;
    if (sg == null) {
      // try unconventional Hall symbol
      hallInfo = new HallInfo(name);
      if (hallInfo.nRotations > 0) {
        sg = new SpaceGroup(-1, NEW_HALL_GROUP + name, true);
        sg.hallInfo = hallInfo;
        sg.hallSymbol = hallInfo.hallSymbol;
        sg.setName("[" + sg.hallSymbol + "]");
        sg.jmolId = null;
      } else if (name.indexOf(",") >= 0) {
        sg = new SpaceGroup(-1, NEW_NO_HALL_GROUP, true);
        sg.doNormalize = false;
        sg.generateOperatorsFromXyzInfo(name);
      }
    }
    if (sg != null)
      sg.generateAllOperators(null);
    return sg;
  }
  
  private int addOperation(String xyz0, int opId, boolean allowScaling) {
    if (xyz0 == null || xyz0.length() < 3) {
      init(false);
      return -1;
    }
    xyz0 = PT.rep(xyz0, " ", "");
    boolean isSpecial = (xyz0.charAt(0) == '=');
    if (isSpecial)
      xyz0 = xyz0.substring(1);
    int id = checkXYZlist(xyz0);
    if (id >= 0)
      return id;
    if (xyz0.startsWith("x1,x2,x3,x4") && modDim == 0) {
      xyzList.clear();
      operationCount = 0;
      modDim = PT.parseInt(xyz0.substring(xyz0.lastIndexOf("x") + 1)) - 3;
    } else if (xyz0.indexOf("m") >= 0) {
      // accept ",+m" or ",m" or ",-m"
      xyz0 = PT.rep(xyz0, "+m", "m");
      if (xyz0.equals("x,y,z,m") || xyz0.equals("x,y,z(mx,my,mz)")) {
        xyzList.clear();
        operationCount = 0;
      }

    }
    SymmetryOperation op = new SymmetryOperation(null, opId, doNormalize);
    if (!op.setMatrixFromXYZ(xyz0, modDim, allowScaling)) {
      Logger.error("couldn't interpret symmetry operation: " + xyz0);
      return -1;
    }
    if (xyz0.charAt(0) == '!') {
      xyz0 = xyz0.substring(xyz0.lastIndexOf('!') + 1);
    }
    return addOp(op, xyz0, isSpecial);
  }

  private int checkXYZlist(String xyz) {
    // problem was that in the case we are adding two half-cell translations,
    // we will get 2/2 --> nothing, instead of 1. 
    return (xyzList.containsKey(xyz)// && !(latticeOp > 0 && xyz.indexOf("/") < 0)
        ? xyzList.get(xyz).intValue() : -1);
  }

  private int addOp(SymmetryOperation op, String xyz0, boolean isSpecial) {
    String xyz = op.xyz;
    if (!isSpecial) {
      // ! in character 0 indicates we are using the symop() function and want to be explicit
      int id = checkXYZlist(xyz);
      if (id >= 0)
        return id;
      if (latticeOp < 0) {
        // do this check until we find a lattice operation
        // by removing +1/2 and +1/3 and checking for something we already have.
        String xxx = PT.replaceAllCharacters(
            modDim > 0 ? SymmetryOperation.replaceXn(xyz, modDim + 3) : xyz,
            "+123/", "");
        if (xyzList.containsKey(xxx + "!")) {
          latticeOp = operationCount;
        } else {
          xyzList.put(xxx + "!", Integer.valueOf(operationCount));
        }
      }
      xyzList.put(xyz, Integer.valueOf(operationCount));
    }
    if (!xyz.equals(xyz0))
      xyzList.put(xyz0, Integer.valueOf(operationCount));
    if (operations == null)
      operations = new SymmetryOperation[4];
    if (operationCount == operations.length)
      operations = (SymmetryOperation[]) AU.arrayCopyObject(operations,
          operationCount * 2);
    operations[operationCount++] = op;
    op.number = operationCount;
    // check for initialization of group without time reversal
    if (op.timeReversal != 0)
      operations[0].timeReversal = 1;
    if (Logger.debugging)
      Logger.debug("\naddOperation " + operationCount + op.dumpInfo());
    return operationCount - 1;
  }

  private void generateOperatorsFromXyzInfo(String xyzInfo) {
    init(true);
    String[] terms = PT.split(xyzInfo.toLowerCase(), ";");
    for (int i = 0; i < terms.length; i++)
      addSymmetry(terms[i], 0, false);
  }
  
  /// operation based on Hall name and unit cell parameters only

  private void generateAllOperators(HallInfo h) {
    if (h == null) {
      if (operationCount > 0)
        return;
      if (hallSymbol.endsWith("?")) {
        checkHallOperators();
        return;
      }
      h = hallInfo;
      operations = new SymmetryOperation[4];
      if (hallInfo == null || hallInfo.nRotations == 0)
        h = hallInfo = new HallInfo(hallSymbol);
      setLattice(hallInfo.latticeCode, hallInfo.isCentrosymmetric);
      init(true);
    }
    switch (h.latticeCode) {
    case '\0':
    case 'S':
    case 'T':
    case 'P':
      latticeType = 'P';
      break;
    default:
      latticeType = h.latticeCode;
      break;
    }
    M4d mat1 = new M4d();
    M4d operation = new M4d();
    M4d[] newOps = new M4d[7];
    for (int i = 0; i < 7; i++)
      newOps[i] = new M4d();
   //??? operationCount = 1;
    // prior to Jmol 11.7.36/11.6.23 this was setting nOps within the loop
    // and setIdentity() outside the loop. That caused a multiplication of
    // operations, not a resetting of them each time.
    for (int i = 0; i < h.nRotations; i++) {
      HallRotationTerm rt = h.rotationTerms[i];
      mat1.setM4(rt.seitzMatrix12ths);
      int nRot = rt.order;
      // this would iterate int nOps = operationCount;
      newOps[0].setIdentity();
      int nOps = operationCount;
      
      for (int j = 1; j <= nRot; j++) {
        M4d m = newOps[j];
        m.mul2(mat1, newOps[0]);
        newOps[0].setM4(m);
        for (int k = 0; k < nOps; k++) {
          operation.mul2(m, operations[k]);
          operation.m03 = ((int)operation.m03 + 12) % 12;
          operation.m13 = ((int)operation.m13 + 12) % 12;
          operation.m23 = ((int)operation.m23 + 12) % 12;
          String xyz = SymmetryOperation.getXYZFromMatrix(operation, true, true, false);
          if (checkXYZlist(xyz) >= 0)
            continue;
          addSymmetrySM("!nohalf!" + xyz, operation);
        }
      }
    }
    if (hmSymbol == null) {
      hmSymbol = SG_NONE;
    }
    if (hmSymbol.equals(SG_NONE)) {
      hallSymbol = h.hallSymbol;
      nHallOperators = Integer.valueOf(operationCount);
    }
    if (nHallOperators != null && operationCount != nHallOperators.intValue())
      Logger.error("Operator mismatch " + operationCount + " for " + this);
  }

  int addSymmetrySM(String xyz, M4d operation) {
    int iop = addOperation(xyz, 0, false);
    //System.out.println("spacegroup addding " + iop + " " + xyz);
    if (iop >= 0) {
      SymmetryOperation symmetryOperation = operations[iop];
      symmetryOperation.setM4(operation);
    }
    return iop;
  }

  final static SpaceGroup determineSpaceGroupN(String name) {
    return determineSpaceGroup(name, 0, 0, 0, 0, 0, 0, -1);
  }

  private final static SpaceGroup determineSpaceGroupNS(String name, SpaceGroup sg) {
    return determineSpaceGroup(name, 0, 0, 0, 0, 0, 0, sg.index);
  }

  final static SpaceGroup determineSpaceGroupNA(String name,
                                                     double[] unitCellParams) {
    return (unitCellParams == null ? determineSpaceGroup(name, 0, 0, 0, 0, 0, 0, -1)
        : determineSpaceGroup(name, unitCellParams[0], unitCellParams[1],
        unitCellParams[2], unitCellParams[3], unitCellParams[4],
        unitCellParams[5], -1));
  }

  private final static SpaceGroup determineSpaceGroup(String name, double a, double b,
                                                double c, double alpha,
                                                double beta, double gamma,
                                                int lastIndex) {

    int i = determineSpaceGroupIndex(name, a, b, c, alpha, beta, gamma,
        lastIndex);
    return (i >= 0 ? SG[i] : null);
  }

  private final static int NAME_UNK = 0;
  private final static int NAME_HM = 3;
  private final static int NAME_ITA = 4;
  private final static int NAME_HALL = 5;

  static boolean isXYZList(String name) {
    return (name != null && name.indexOf(",") >= 0 
        && name.indexOf("(") < 0
        && name.indexOf(":") < 0);
  }
  
  private final static int determineSpaceGroupIndex(String name, double a,
                                                    double b, double c,
                                                    double alpha, double beta,
                                                    double gamma,
                                                    int lastIndex) {
    if (isXYZList(name))
      return -1;
    getSpaceGroups();
    if (lastIndex < 0)
      lastIndex = SG.length;
    name = name.trim().toLowerCase();
    //boolean checkBilbao = false;
    if (name.startsWith("bilbao:")) {
      //      checkBilbao = true;
      name = name.substring(7);
    }
    int pt = name.indexOf("hall:");
    if (pt > 0)
      name = name.substring(pt);
    int nameType = (name.startsWith("ita/") ? NAME_ITA
        : name.startsWith("hall:") ? NAME_HALL
            : name.startsWith("hm:") ? NAME_HM : NAME_UNK);
    switch (nameType) {
    case NAME_HM:
    case NAME_HALL:
    case NAME_ITA:
      name = name.substring(nameType);
      break;
    case NAME_UNK:
      if (name.contains("[")) {
        // feeding back "P 1 [P 1]" for example
        nameType = NAME_HALL;
        name = name.substring(0, name.indexOf("[")).trim();
      } else if (name.indexOf(".") > 0) {
        // 151.1 or 152.1:a,b,c;0,0,-1/6
        nameType = NAME_ITA;
      }
    }
    String nameExt = name;
    int i;
    boolean haveExtension = false;

    if (nameType == NAME_ITA) {

    } else {

      // '_' --> ' '
      name = name.replace('_', ' ');

      // get lattice term to upper case and separated
      if (name.length() >= 2) {
        i = (name.indexOf("-") == 0 ? 2 : 1);
        if (i < name.length() && name.charAt(i) != ' ')
          name = name.substring(0, i) + " " + name.substring(i);
        name = toCap(name, 2);
      }
    }
    // get extension
    String ext = "";
    if ((i = name.indexOf(":")) > 0) {
      ext = name.substring(i + 1);
      name = name.substring(0, i).trim();
      haveExtension = (ext.length() > 0);
    }

    if (nameType != NAME_ITA && nameType != NAME_HALL && !haveExtension
        && PT.isOneOf(name, ambiguousHMNames)) {
      ext = "?";
      haveExtension = true;
    }

    // generate spaceless abbreviation "P m m m" --> "Pmmm"  "P 2(1)/c" --> "P21/c"
    String abbr = PT.replaceAllCharacters(name, " ()", "");

    SpaceGroup s;

    // exact matches:
    
    switch (nameType) {
    case NAME_ITA:
      if (haveExtension)
        for (i = 0; i < lastIndex; i++) {
          if (nameExt.equalsIgnoreCase(SG[i].itaIndex))
            return i;
        }
      else
        for (i = 0; i < lastIndex; i++) {
          if (name.equalsIgnoreCase(SG[i].itaIndex))
            return i;
        }
      break;
    case NAME_HALL:
      for (i = 0; i < lastIndex; i++) {
          if (SG[i].hallSymbol.equalsIgnoreCase(name))
            return i;
        }
      break;
    default:
    case NAME_HM:
      // Full intl table entry, including :xx

      if (nameType != NAME_HM)
        for (i = 0; i < lastIndex; i++)
          if (SG[i].jmolId.equalsIgnoreCase(nameExt))
            return i;

      // Full H-M symbol, including :xx

      // BUT some on the list presume defaults. The way to finesse this is
      // to add ":?" to a space group name to force axis ambiguity check

      for (i = 0; i < lastIndex; i++)
        if (SG[i].hmSymbolFull.equalsIgnoreCase(nameExt))
          return i;
      
      // alternative, but unique H-M symbol, specifically for F m 3 m/F m -3 m type
    for (i = 0; i < lastIndex; i++)
       if ((s = SG[i]).hmSymbolAlternative != null
           && s.hmSymbolAlternative.equalsIgnoreCase(nameExt))
         return i;

     if (haveExtension) { // P2/m:a      
       // Abbreviated H-M with intl table :xx
       for (i = 0; i < lastIndex; i++)
         if ((s = SG[i]).hmSymbolAbbr.equalsIgnoreCase(abbr)
             && s.jmolIdExt.equalsIgnoreCase(ext))
           return i;
       // shortened -- not including " 1 " terms
       for (i = 0; i < lastIndex; i++)
         if ((s = SG[i]).hmSymbolAbbrShort.equalsIgnoreCase(abbr)
             && s.jmolIdExt.equalsIgnoreCase(ext))
           return i;
     }
     // unique axis, cell and origin options with H-M abbr

     char uniqueAxis = determineUniqueAxis(a, b, c, alpha, beta, gamma);

     if (!haveExtension || ext.charAt(0) == '?')
       // no extension or unknown extension, so we look for unique axis
       for (i = 0; i < lastIndex; i++)
         if (((s = SG[i]).hmSymbolAbbr.equalsIgnoreCase(abbr)
             || s.hmSymbolAbbrShort.equalsIgnoreCase(abbr)
             || s.itaNumber.equals(abbr))
         //&& (!checkBilbao || s.isBilbao)
         )
           switch (s.ambiguityType) {
           case '\0':
             return i;
           case 'a':
             if (s.uniqueAxis == uniqueAxis || uniqueAxis == '\0')
               return i;
             break;
           case 'o':
             if (ext.length() == 0) {
               if (s.hmSymbolExt.equals("2"))
                 return i; // defaults to origin:2
             } else if (s.hmSymbolExt.equalsIgnoreCase(ext))
               return i;
             break;
           case 't':
             if (ext.length() == 0) {
               if (s.axisChoice == 'h')
                 return i; //defaults to hexagonal
             } else if ((s.axisChoice + "").equalsIgnoreCase(ext))
               return i;
             break;
           }
      break;
    }

    // inexact just the number; no extension indicated -- first in list

    if (ext.length() == 0)
      for (i = 0; i < lastIndex; i++)
        if ((s = SG[i]).itaNumber.equals(nameExt)
        //&& (!checkBilbao || s.isBilbao)
        )
          return i;
    return -1;
  }
   
  private void setJmolCode(String name) {
    jmolId = name;
    String[] parts = PT.split(name, ":");
    itaNumber = parts[0];
    jmolIdExt = (parts.length == 1 ? "" : parts[1]);
    ambiguityType = '\0';
    if (jmolIdExt.length() > 0) {
      char c = jmolIdExt.charAt(0); 
      if (jmolIdExt.equals("h") || jmolIdExt.equals("r")) {
        ambiguityType = 't';
        axisChoice = jmolIdExt.charAt(0);
      } else if (c == '1' || c == '2') {
        ambiguityType = 'o';
        // originChoice = intlTableNumberExt.charAt(0);
      } else if (jmolIdExt.length() <= 2
          || jmolIdExt.length() == 3 && c == '-' ) { // :a or :b3
        ambiguityType = 'a';
        uniqueAxis = jmolIdExt.charAt(c == '-' ? 1 : 0);
        // Q: should we include :-a1 here?
        // if (intlTableNumberExt.length() == 2)
        // cellChoice = intlTableNumberExt.charAt(1);
      } else if (jmolIdExt.contains("-")) {
        ambiguityType = '-';
        // skip when searching for a group by name
        // added 9/28/14 Jmol 14.3.7
      }
    }
  }

   private final static char determineUniqueAxis(double a, double b, double c, double alpha, double beta, double gamma) {
     if (a == b)
       return (b == c ? '\0' : 'c');
     if (b == c)
       return 'a';
     if (c == a)
       return 'b';
     if (alpha == beta)
       return (beta == gamma ? '\0' : 'c');
     if (beta == gamma)
       return 'a';
     if (gamma == alpha)
       return 'b';
     return '\0';
   }

  ///  data  ///

  private volatile static int sgIndex = -1;
  
  private volatile static String lastInfo;

  final static String SET_R = "2/3a+1/3b+1/3c,-1/3a+1/3b+1/3c,-1/3a-2/3b+1/3c";
  private final static String SET_AB = "a-b,a+b,c";
  
  /**
   * For example P 1 2 1 and P 2, meaning P 2 is somewhat ambiguous (without knowning the standard)
   */
  private volatile static String ambiguousHMNames = "";
 
  private void buildSelf(String sgLineData) {
    String[] terms = PT.split(sgLineData.toLowerCase(), ";");
    jmolId = terms[0].trim(); // International Table Number :
                                           // options
    // "0             ;--     ;--       ;0   ;--         ;--            ;"
    // intlNo:options;itaIndex;transform;nOps;schoenflies;hermannMauguin;Hall
    //   0             1        2            3         4        5


    // added # of operators to help in search phase

    // "4:b;4.1;2;c2^2;p 1 21 1;p 2yb",   //full name

    ////  terms[0] -- International Table Number and setting ////
    setJmolCode(jmolId);

    ////  terms[1] -- ITA index or '--' ////

    // could be "154:a,b,c|0,0,-1/6"
    itaIndex = terms[1].replace('|',';');
    
    ////  terms[2] -- transform
    String s = terms[2];
    // leaving "r" as is
    itaTransform = (s.length() == 0 || s.equals(SG_NONE) ? "a,b,c" : PT.rep(s, "ab", SET_AB).replace('|', ';'));
    clegId = itaNumber + ":" + itaTransform;
   
    ////  terms[3] -- number of operators ////

    if (terms[3].length() > 0) {
      nHallOperators = Integer.valueOf(terms[3]);
      Lst<SpaceGroup> lst = htByOpCount.get(nHallOperators);
      if (lst == null)
        htByOpCount.put(nHallOperators, lst = new Lst<SpaceGroup>());
      lst.addLast(this);
    }
    ////  terms[4] -- Schoenflies ////

    crystalClass = toCap(PT.split(terms[4], "^")[0], 1);

    ////  terms[5] -- Hermann-Mauguin ////

    setHMSymbol(terms[5]);

    ////  term 6 -- Hall ////

    String info;
    if ("xyz".equals(terms[6])) {
      info = hmSymbol;
      hallSymbol = "" + latticeType + "?"; // hallInfo will be used to apply lattice
    } else {
      hallSymbol = terms[6];
      if (hallSymbol.length() > 1)
        hallSymbol = toCap(hallSymbol, 2);
      info = itaNumber + hallSymbol;
      if (itaNumber.charAt(0) != '0' && info.equals(lastInfo))
        ambiguousHMNames += hmSymbol + ";";
    }
    
    // terms[6]  operator list where I ddn't figure out the Hall code 
    lastInfo = info;
    name = "HM:" + hmSymbolFull + " " + (hallSymbol == null || hallSymbol == "?" ? "[" + hallSymbol + "]" : "") + " #" + itaIndex;


    //System.out.println(intlTableNumber + (intlTableNumberExt.equals("") ? "" : ":" + intlTableNumberExt) + "\t"
    //      + hmSymbol + "\t" + hmSymbolAbbr + "\t" + hmSymbolAbbrShort + "\t"
    //    + hallSymbol);
  }

  void setHMSymbol(String name) {
    int pt = name.indexOf("#");
    if (pt >= 0)
      name = name.substring(0, pt).trim();
    hmSymbolFull = toCap(name, 1);
    latticeType = hmSymbolFull.charAt(0);
    String[] parts = PT.split(hmSymbolFull, ":");
    hmSymbol = parts[0];
    hmSymbolExt = (parts.length == 1 ? "" : parts[1]);
    pt = hmSymbol.indexOf(" -3");
    if (pt >= 1)
      if ("admn".indexOf(hmSymbol.charAt(pt - 1)) >= 0) {
        hmSymbolAlternative = (hmSymbol.substring(0, pt) + " 3" + hmSymbol
            .substring(pt + 3)).toLowerCase();
      }
    hmSymbolAbbr = PT.rep(hmSymbol, " ", "");
    hmSymbolAbbrShort = (hmSymbol.length() > 3 ? PT.rep(hmSymbol, " 1", "") : hmSymbolAbbr);
    hmSymbolAbbrShort = PT.rep(hmSymbolAbbrShort, " ", "");
  }

  private static String toCap(String s, int n) {
    return s.substring(0, n).toUpperCase() + s.substring(n);
  }

  @Override
  public String toString() {
    return asString();
  }
  
  String strName;
  public String displayName;
  
  String asString() {
    return (strName == null 
        ? (strName = 
          (jmolId == null || jmolId.equals("0")
            ? name 
            : jmolId + " HM:" + hmSymbolFull + " #" + clegId
            )
          ) 
        : strName
        );
  }

  public String getDisplayName() {
    if (displayName == null) {
      if (jmolId == null) {
        displayName = "";
      } else if (!jmolId.equals("0")) {
        displayName = jmolId + " HM: " + hmSymbolFull + " #";
      }
      if (displayName == null) {
        displayName = "Hall:" + hallSymbol; // may still need setting
      } else {
        displayName += getItaIndex();
      }
    }
    return displayName;
  }

  private static SpaceGroup[] SG;
  
  private static Map<Integer, Lst<SpaceGroup>> htByOpCount = new Hashtable<Integer, Lst<SpaceGroup>>();
  static Map<String, SpaceGroup> nameToGroup;
  
  private synchronized static SpaceGroup[] getSpaceGroups() {
    if (SG == null) {
      int n = STR_SG.length;
      nameToGroup = new Hashtable<String, SpaceGroup>();
      SpaceGroup[] defs = new SpaceGroup[n];
      for (int i = 0; i < n; i++) {
        defs[i] = new SpaceGroup(i, STR_SG[i], true);
        nameToGroup.put(defs[i].jmolId, defs[i]);
      }
      System.out.println("SpaceGroup - " +STR_SG.length + " settings generated");
      STR_SG = null;
      SG = defs;
    }
    return SG;
  }
  
  
  // primitives:
  // 1: primitive:  1. P1   2. P1¯,
  //                3. P2  10. P2/m   
  //                4. P21  11. P21/m   
  //                6. Pm   13. P2/c
  //                7. Pc   14. P21/c
  
  //                16. P222    25. Pmm2  29. Pca21  33. Pna21  50. Pban  54. Pcca   58. Pnnm   62. Pnma   
  //                17. P2221   26. Pmc21 30. Pnc2   34. Pnn2   51. Pmma  55. Pbam   59. Pmmn   
  //                18. P21212  27. Pcc2  31. Pmn21  47. Pmmm   52. Pnna  56. Pccn   60. Pbcn
  //                19. P212121 28. Pma2  32. Pba2   49. Pccm   53. Pmna  57. Pbcm   61. Pbca
  
  //                75. P4      81. P4¯   86. P42/n  92. P41212  96. P43212 102. P42nm  106. P42bc  114. P4_21c  118. P4¯n2  126. P4/nnc  130. P4/ncc  134. P42/nnm  138. P42/ncm
  //                76. P41     83. P4/m  89. P422   93. P4222   99. P4mm   103. P4cc   111. P4¯2m  115. P4¯m2   123. P4/mmm 127. P4/mbm  131. P42/mmc 135. P42/mbc
  //                77. P42     84. P42/m 90. P4212  94. P42212 100. P4bm   104. P4nc   112. P4¯2c  116. P4¯c2   124. P4/mcc 128. P4/mnc  132. P42/mcm 136. P42/mnm
  //                78. P43     85. P4/n  91. P4122  95. P4322  101. P42cm  105. P42mc  113. P4¯21m 117. P4¯b2   125. P4/nbm 129. P4/nmm  133. P42/nbc 137. P42/nmc
  //
  // + all hexagonal and rhombohedral
 
  // 2: base-centered:  5. C2     8. Cm    9. Cc    12. C2/m   15. C2/c
  //                   20. C2221 37. Ccc2 66. Cccm  
  //                   21. C222  63. Cmcm 67. Cmma
  //                   35. Cmm2  64. Cmca 68. Ccca
  //                   36. Cmc21 65. Cmmm
  
  // 3: face-centered:  22. F222 42. Fmm2   43. Fdd2    69. Fmmm   70. Fddd
  //                   143. P3  149. P312  153. P3212  158. P3c1  164. P3¯m1  
  //                   144. P31 150. P321  154. P3221  159. P31c  165. P3¯c1
  //                   145. P32 151. P3112 156. P3m1   162. P3¯1m 
  //                   147. P3¯ 152. P3121 157. P31m   163. P3¯1c
  
  
  // 4: body-centered: 80. I41    97. I422  109. I41md   121. I4¯2m   141. I41/amd  
  //                   82. I4¯    98. I4122 110. I41cd   122. I4¯2d   142. I41/acd
  //                   87. I4/m  107. I4mm  119. I4¯m2   139. I4/mmm
  //                   88. I41/a 108. I4cm  120. I4¯c2   140. I4/mcm
  
  
  /**
   * intlNo:options;nOps;schoenflies;hermannMauguin;Hall;BilbaoFlag
   * 
   * 530 settings, some with multiple names
   */
  
  // intlNo:options;nOps;schoenflies;hermannMauguin;Hall;BilbaoFlag
  //   0             1        2            3         4        5

  // the 5th term is not actually checked; we have ";-b" as 5th term right now for "not Bilbao"
  // "48:1;8;d2h^2;p n n n:1;p 2 2 -1n;-b",
  // and is probably origin choice 1.   
  // * indicates a nonstandard Hall name  "p 21" same as "p 2yb"
  // 4:c*;2;c2^2;p 1 1 21*;p 21         
  // added # of operators to help in search phase
  // "4:b;2;c2^2;p 1 21 1;p 2yb",   //full name


  private static String[] STR_SG = {
      "1;1.1;;1;c1^1;p 1;p 1",  
      "2;2.1;;2;ci^1;p -1;-p 1",  
      "3:b;3.1;;2;c2^1;p 1 2 1;p 2y",   //full name
      "3:b;3.1;;2;c2^1;p 2;p 2y",  
      "3:c;3.2;c,a,b;2;c2^1;p 1 1 2;p 2",  
      "3:a;3.3;b,c,a;2;c2^1;p 2 1 1;p 2x",  
      "4:b;4.1;;2;c2^2;p 1 21 1;p 2yb",   //full name
      "4:b;4.1;;2;c2^2;p 21;p 2yb",  
      "4:b*;4.1;;2;c2^2;p 1 21 1*;p 2y1",   //nonstandard
      "4:c;4.2;c,a,b;2;c2^2;p 1 1 21;p 2c",  
      "4:c*;4.2;c,a,b;2;c2^2;p 1 1 21*;p 21",   //nonstandard
      "4:a;4.3;b,c,a;2;c2^2;p 21 1 1;p 2xa",  
      "4:a*;4.3;b,c,a;2;c2^2;p 21 1 1*;p 2x1",   //nonstandard
      "5:b1;5.1;;4;c2^3;c 1 2 1;c 2y",   //full name
      "5:b1;5.1;;4;c2^3;c 2;c 2y",  
      "5:b2;5.2;-a-c,b,a;4;c2^3;a 1 2 1;a 2y",  
      "5:b3;5.3;c,b,-a-c;4;c2^3;i 1 2 1;i 2y",  
      "5:c1;5.4;c,a,b;4;c2^3;a 1 1 2;a 2",  
      "5:c2;5.5;a,-a-c,b;4;c2^3;b 1 1 2;b 2",  
      "5:c3;5.6;-a-c,c,b;4;c2^3;i 1 1 2;i 2",  
      "5:a1;5.7;b,c,a;4;c2^3;b 2 1 1;b 2x",  
      "5:a2;5.8;b,a,-a-c;4;c2^3;c 2 1 1;c 2x",  
      "5:a3;5.9;b,-a-c,c;4;c2^3;i 2 1 1;i 2x",  
      "6:b;6.1;;2;cs^1;p 1 m 1;p -2y",   //full name
      "6:b;6.1;;2;cs^1;p m;p -2y",  
      "6:c;6.2;c,a,b;2;cs^1;p 1 1 m;p -2",  
      "6:a;6.3;b,c,a;2;cs^1;p m 1 1;p -2x",  
      "7:b1;7.1;;2;cs^2;p 1 c 1;p -2yc",   //full name
      "7:b1;7.1;;2;cs^2;p c;p -2yc",  
      "7:b2;7.2;-a-c,b,a;2;cs^2;p 1 n 1;p -2yac",   //full name
      "7:b2;7.2;-a-c,b,a;2;cs^2;p n;p -2yac",  
      "7:b3;7.3;c,b,-a-c;2;cs^2;p 1 a 1;p -2ya",   //full name
      "7:b3;7.3;c,b,-a-c;2;cs^2;p a;p -2ya",  
      "7:c1;7.4;c,a,b;2;cs^2;p 1 1 a;p -2a",  
      "7:c2;7.5;a,-a-c,b;2;cs^2;p 1 1 n;p -2ab",  
      "7:c3;7.6;-a-c,c,b;2;cs^2;p 1 1 b;p -2b",  
      "7:a1;7.7;b,c,a;2;cs^2;p b 1 1;p -2xb",  
      "7:a2;7.8;b,a,-a-c;2;cs^2;p n 1 1;p -2xbc",  
      "7:a3;7.9;b,-a-c,c;2;cs^2;p c 1 1;p -2xc",  
      "8:b1;8.1;;4;cs^3;c 1 m 1;c -2y",   //full name
      "8:b1;8.1;;4;cs^3;c m;c -2y",  
      "8:b2;8.2;-a-c,b,a;4;cs^3;a 1 m 1;a -2y",  
      "8:b3;8.3;c,b,-a-c;4;cs^3;i 1 m 1;i -2y",   //full name
      "8:b3;8.3;c,b,-a-c;4;cs^3;i m;i -2y",  
      "8:c1;8.4;c,a,b;4;cs^3;a 1 1 m;a -2",  
      "8:c2;8.5;a,-a-c,b;4;cs^3;b 1 1 m;b -2",  
      "8:c3;8.6;-a-c,c,b;4;cs^3;i 1 1 m;i -2",  
      "8:a1;8.7;b,c,a;4;cs^3;b m 1 1;b -2x",  
      "8:a2;8.8;b,a,-a-c;4;cs^3;c m 1 1;c -2x",  
      "8:a3;8.9;b,-a-c,c;4;cs^3;i m 1 1;i -2x",  
      "9:b1;9.1;;4;cs^4;c 1 c 1;c -2yc",   //full name
      "9:b1;9.1;;4;cs^4;c c;c -2yc",  
      "9:b2;9.2;-a-c,b,a;4;cs^4;a 1 n 1;a -2yab",  
      "9:b3;9.3;c,b,-a-c;4;cs^4;i 1 a 1;i -2ya",  
      "9:-b1;9.4;c,-b,a;4;cs^4;a 1 a 1;a -2ya",  
      "9:-b2;9.5;a,-b,-a-c;4;cs^4;c 1 n 1;c -2yac",  
      "9:-b3;9.6;-a-c,-b,c;4;cs^4;i 1 c 1;i -2yc",  
      "9:c1;9.7;c,a,b;4;cs^4;a 1 1 a;a -2a",  
      "9:c2;9.8;a,-a-c,b;4;cs^4;b 1 1 n;b -2ab",  
      "9:c3;9.9;-a-c,c,b;4;cs^4;i 1 1 b;i -2b",  
      "9:-c1;9.10;a,c,-b;4;cs^4;b 1 1 b;b -2b",  
      "9:-c2;9.11;-a-c,a,-b;4;cs^4;a 1 1 n;a -2ab",  
      "9:-c3;9.12;c,-a-c,-b;4;cs^4;i 1 1 a;i -2a",  
      "9:a1;9.13;b,c,a;4;cs^4;b b 1 1;b -2xb",  
      "9:a2;9.14;b,a,-a-c;4;cs^4;c n 1 1;c -2xac",  
      "9:a3;9.15;b,-a-c,c;4;cs^4;i c 1 1;i -2xc",  
      "9:-a1;9.16;-b,a,c;4;cs^4;c c 1 1;c -2xc",  
      "9:-a2;9.17;-b,-a-c,a;4;cs^4;b n 1 1;b -2xab",  
      "9:-a3;9.18;-b,c,-a-c;4;cs^4;i b 1 1;i -2xb",  
      "10:b;10.1;;4;c2h^1;p 1 2/m 1;-p 2y",   //full name
      "10:b;10.1;;4;c2h^1;p 2/m;-p 2y",  
      "10:c;10.2;c,a,b;4;c2h^1;p 1 1 2/m;-p 2",  
      "10:a;10.3;b,c,a;4;c2h^1;p 2/m 1 1;-p 2x",  
      "11:b;11.1;;4;c2h^2;p 1 21/m 1;-p 2yb",   //full name
      "11:b;11.1;;4;c2h^2;p 21/m;-p 2yb",  
      "11:b*;11.1;;4;c2h^2;p 1 21/m 1*;-p 2y1",   //nonstandard
      "11:c;11.2;c,a,b;4;c2h^2;p 1 1 21/m;-p 2c",  
      "11:c*;11.2;c,a,b;4;c2h^2;p 1 1 21/m*;-p 21",   //nonstandard
      "11:a;11.3;b,c,a;4;c2h^2;p 21/m 1 1;-p 2xa",  
      "11:a*;11.3;b,c,a;4;c2h^2;p 21/m 1 1*;-p 2x1",   //nonstandard
      "12:b1;12.1;;8;c2h^3;c 1 2/m 1;-c 2y",   //full name
      "12:b1;12.1;;8;c2h^3;c 2/m;-c 2y",  
      "12:b2;12.2;-a-c,b,a;8;c2h^3;a 1 2/m 1;-a 2y",  
      "12:b3;12.3;c,b,-a-c;8;c2h^3;i 1 2/m 1;-i 2y",   //full name
      "12:b3;12.3;c,b,-a-c;8;c2h^3;i 2/m;-i 2y",  
      "12:c1;12.4;c,a,b;8;c2h^3;a 1 1 2/m;-a 2",  
      "12:c2;12.5;a,-a-c,b;8;c2h^3;b 1 1 2/m;-b 2",  
      "12:c3;12.6;-a-c,c,b;8;c2h^3;i 1 1 2/m;-i 2",  
      "12:a1;12.7;b,c,a;8;c2h^3;b 2/m 1 1;-b 2x",  
      "12:a2;12.8;b,a,-a-c;8;c2h^3;c 2/m 1 1;-c 2x",  
      "12:a3;12.9;b,-a-c,c;8;c2h^3;i 2/m 1 1;-i 2x",  
      "13:b1;13.1;;4;c2h^4;p 1 2/c 1;-p 2yc",   //full name
      "13:b1;13.1;;4;c2h^4;p 2/c;-p 2yc",  
      "13:b2;13.2;-a-c,b,a;4;c2h^4;p 1 2/n 1;-p 2yac",   //full name
      "13:b2;13.2;-a-c,b,a;4;c2h^4;p 2/n;-p 2yac",  
      "13:b3;13.3;c,b,-a-c;4;c2h^4;p 1 2/a 1;-p 2ya",   //full name
      "13:b3;13.3;c,b,-a-c;4;c2h^4;p 2/a;-p 2ya",  
      "13:c1;13.4;c,a,b;4;c2h^4;p 1 1 2/a;-p 2a",  
      "13:c2;13.5;a,-a-c,b;4;c2h^4;p 1 1 2/n;-p 2ab",  
      "13:c3;13.6;-a-c,c,b;4;c2h^4;p 1 1 2/b;-p 2b",  
      "13:a1;13.7;b,c,a;4;c2h^4;p 2/b 1 1;-p 2xb",  
      "13:a2;13.8;b,a,-a-c;4;c2h^4;p 2/n 1 1;-p 2xbc",  
      "13:a3;13.9;b,-a-c,c;4;c2h^4;p 2/c 1 1;-p 2xc",  
      "14:b1;14.1;;4;c2h^5;p 1 21/c 1;-p 2ybc",   //full name
      "14:b1;14.1;;4;c2h^5;p 21/c;-p 2ybc",  
      "14:b2;14.2;-a-c,b,a;4;c2h^5;p 1 21/n 1;-p 2yn",   //full name
      "14:b2;14.2;-a-c,b,a;4;c2h^5;p 21/n;-p 2yn",  
      "14:b3;14.3;c,b,-a-c;4;c2h^5;p 1 21/a 1;-p 2yab",   //full name
      "14:b3;14.3;c,b,-a-c;4;c2h^5;p 21/a;-p 2yab",  
      "14:c1;14.4;c,a,b;4;c2h^5;p 1 1 21/a;-p 2ac",  
      "14:c2;14.5;a,-a-c,b;4;c2h^5;p 1 1 21/n;-p 2n",  
      "14:c3;14.6;-a-c,c,b;4;c2h^5;p 1 1 21/b;-p 2bc",  
      "14:a1;14.7;b,c,a;4;c2h^5;p 21/b 1 1;-p 2xab",  
      "14:a2;14.8;b,a,-a-c;4;c2h^5;p 21/n 1 1;-p 2xn",  
      "14:a3;14.9;b,-a-c,c;4;c2h^5;p 21/c 1 1;-p 2xac",  
      "15:b1;15.1;;8;c2h^6;c 1 2/c 1;-c 2yc",   //full name
      "15:b1;15.1;;8;c2h^6;c 2/c;-c 2yc",  
      "15:b2;15.2;-a-c,b,a;8;c2h^6;a 1 2/n 1;-a 2yab",  
      "15:b3;15.3;c,b,-a-c;8;c2h^6;i 1 2/a 1;-i 2ya",   //full name
      "15:b3;15.3;c,b,-a-c;8;c2h^6;i 2/a;-i 2ya",  
      "15:-b1;15.4;c,-b,a;8;c2h^6;a 1 2/a 1;-a 2ya",  
      "15:-b2;15.5;a,-b,-a-c;8;c2h^6;c 1 2/n 1;-c 2yac",   //full name
      "15:-b2;15.5;a,-b,-a-c;8;c2h^6;c 2/n;-c 2yac",  
      "15:-b3;15.6;-a-c,-b,c;8;c2h^6;i 1 2/c 1;-i 2yc",   //full name
      "15:-b3;15.6;-a-c,-b,c;8;c2h^6;i 2/c;-i 2yc",  
      "15:c1;15.7;c,a,b;8;c2h^6;a 1 1 2/a;-a 2a",  
      "15:c2;15.8;a,-a-c,b;8;c2h^6;b 1 1 2/n;-b 2ab",  
      "15:c3;15.9;-a-c,c,b;8;c2h^6;i 1 1 2/b;-i 2b",  
      "15:-c1;15.10;a,c,-b;8;c2h^6;b 1 1 2/b;-b 2b",  
      "15:-c2;15.11;-a-c,a,-b;8;c2h^6;a 1 1 2/n;-a 2ab",  
      "15:-c3;15.12;c,-a-c,-b;8;c2h^6;i 1 1 2/a;-i 2a",  
      "15:a1;15.13;b,c,a;8;c2h^6;b 2/b 1 1;-b 2xb",  
      "15:a2;15.14;b,a,-a-c;8;c2h^6;c 2/n 1 1;-c 2xac",  
      "15:a3;15.15;b,-a-c,c;8;c2h^6;i 2/c 1 1;-i 2xc",  
      "15:-a1;15.16;-b,a,c;8;c2h^6;c 2/c 1 1;-c 2xc",  
      "15:-a2;15.17;-b,-a-c,a;8;c2h^6;b 2/n 1 1;-b 2xab",  
      "15:-a3;15.18;-b,c,-a-c;8;c2h^6;i 2/b 1 1;-i 2xb",  
      "16;16.1;;4;d2^1;p 2 2 2;p 2 2",  
      "17;17.1;;4;d2^2;p 2 2 21;p 2c 2",  
      "17*;17.1;;4;d2^2;p 2 2 21*;p 21 2",   //nonstandard
      "17:cab;17.2;c,a,b;4;d2^2;p 21 2 2;p 2a 2a",  
      "17:bca;17.3;b,c,a;4;d2^2;p 2 21 2;p 2 2b",  
      "18;18.1;;4;d2^3;p 21 21 2;p 2 2ab",  
      "18:cab;18.2;c,a,b;4;d2^3;p 2 21 21;p 2bc 2",  
      "18:bca;18.3;b,c,a;4;d2^3;p 21 2 21;p 2ac 2ac",  
      "19;19.1;;4;d2^4;p 21 21 21;p 2ac 2ab",  
      "20;20.1;;8;d2^5;c 2 2 21;c 2c 2",  
      "20*;20.1;;8;d2^5;c 2 2 21*;c 21 2",   //nonstandard
      "20:cab;20.2;c,a,b;8;d2^5;a 21 2 2;a 2a 2a",  
      "20:cab*;20.2;c,a,b;8;d2^5;a 21 2 2*;a 2a 21",   //nonstandard
      "20:bca;20.3;b,c,a;8;d2^5;b 2 21 2;b 2 2b",  
      "21;21.1;;8;d2^6;c 2 2 2;c 2 2",  
      "21:cab;21.2;c,a,b;8;d2^6;a 2 2 2;a 2 2",  
      "21:bca;21.3;b,c,a;8;d2^6;b 2 2 2;b 2 2",  
      "22;22.1;;16;d2^7;f 2 2 2;f 2 2",  
      "23;23.1;;8;d2^8;i 2 2 2;i 2 2",  
      "24;24.1;;8;d2^9;i 21 21 21;i 2b 2c",  
      "25;25.1;;4;c2v^1;p m m 2;p 2 -2",  
      "25:cab;25.2;c,a,b;4;c2v^1;p 2 m m;p -2 2",  
      "25:bca;25.3;b,c,a;4;c2v^1;p m 2 m;p -2 -2",  
      "26;26.1;;4;c2v^2;p m c 21;p 2c -2",  
      "26*;26.1;;4;c2v^2;p m c 21*;p 21 -2",   //nonstandard
      "26:ba-c;26.2;b,a,-c;4;c2v^2;p c m 21;p 2c -2c",  
      "26:ba-c*;26.2;b,a,-c;4;c2v^2;p c m 21*;p 21 -2c",   //nonstandard
      "26:cab;26.3;c,a,b;4;c2v^2;p 21 m a;p -2a 2a",  
      "26:-cba;26.4;-c,b,a;4;c2v^2;p 21 a m;p -2 2a",  
      "26:bca;26.5;b,c,a;4;c2v^2;p b 21 m;p -2 -2b",  
      "26:a-cb;26.6;a,-c,b;4;c2v^2;p m 21 b;p -2b -2",  
      "27;27.1;;4;c2v^3;p c c 2;p 2 -2c",  
      "27:cab;27.2;c,a,b;4;c2v^3;p 2 a a;p -2a 2",  
      "27:bca;27.3;b,c,a;4;c2v^3;p b 2 b;p -2b -2b",  
      "28;28.1;;4;c2v^4;p m a 2;p 2 -2a",  
      "28*;28.1;;4;c2v^4;p m a 2*;p 2 -21",   //nonstandard
      "28:ba-c;28.2;b,a,-c;4;c2v^4;p b m 2;p 2 -2b",  
      "28:cab;28.3;c,a,b;4;c2v^4;p 2 m b;p -2b 2",  
      "28:-cba;28.4;-c,b,a;4;c2v^4;p 2 c m;p -2c 2",  
      "28:-cba*;28.4;-c,b,a;4;c2v^4;p 2 c m*;p -21 2",   //nonstandard
      "28:bca;28.5;b,c,a;4;c2v^4;p c 2 m;p -2c -2c",  
      "28:a-cb;28.6;a,-c,b;4;c2v^4;p m 2 a;p -2a -2a",  
      "29;29.1;;4;c2v^5;p c a 21;p 2c -2ac",  
      "29:ba-c;29.2;b,a,-c;4;c2v^5;p b c 21;p 2c -2b",  
      "29:cab;29.3;c,a,b;4;c2v^5;p 21 a b;p -2b 2a",  
      "29:-cba;29.4;-c,b,a;4;c2v^5;p 21 c a;p -2ac 2a",  
      "29:bca;29.5;b,c,a;4;c2v^5;p c 21 b;p -2bc -2c",  
      "29:a-cb;29.6;a,-c,b;4;c2v^5;p b 21 a;p -2a -2ab",  
      "30;30.1;;4;c2v^6;p n c 2;p 2 -2bc",  
      "30:ba-c;30.2;b,a,-c;4;c2v^6;p c n 2;p 2 -2ac",  
      "30:cab;30.3;c,a,b;4;c2v^6;p 2 n a;p -2ac 2",  
      "30:-cba;30.4;-c,b,a;4;c2v^6;p 2 a n;p -2ab 2",  
      "30:bca;30.5;b,c,a;4;c2v^6;p b 2 n;p -2ab -2ab",  
      "30:a-cb;30.6;a,-c,b;4;c2v^6;p n 2 b;p -2bc -2bc",  
      "31;31.1;;4;c2v^7;p m n 21;p 2ac -2",  
      "31:ba-c;31.2;b,a,-c;4;c2v^7;p n m 21;p 2bc -2bc",  
      "31:cab;31.3;c,a,b;4;c2v^7;p 21 m n;p -2ab 2ab",  
      "31:-cba;31.4;-c,b,a;4;c2v^7;p 21 n m;p -2 2ac",  
      "31:bca;31.5;b,c,a;4;c2v^7;p n 21 m;p -2 -2bc",  
      "31:a-cb;31.6;a,-c,b;4;c2v^7;p m 21 n;p -2ab -2",  
      "32;32.1;;4;c2v^8;p b a 2;p 2 -2ab",  
      "32:cab;32.2;c,a,b;4;c2v^8;p 2 c b;p -2bc 2",  
      "32:bca;32.3;b,c,a;4;c2v^8;p c 2 a;p -2ac -2ac",  
      "33;33.1;;4;c2v^9;p n a 21;p 2c -2n",  
      "33*;33.1;;4;c2v^9;p n a 21*;p 21 -2n",   //nonstandard
      "33:ba-c;33.2;b,a,-c;4;c2v^9;p b n 21;p 2c -2ab",  
      "33:ba-c*;33.2;b,a,-c;4;c2v^9;p b n 21*;p 21 -2ab",   //nonstandard
      "33:cab;33.3;c,a,b;4;c2v^9;p 21 n b;p -2bc 2a",  
      "33:cab*;33.3;c,a,b;4;c2v^9;p 21 n b*;p -2bc 21",   //nonstandard
      "33:-cba;33.4;-c,b,a;4;c2v^9;p 21 c n;p -2n 2a",  
      "33:-cba*;33.4;-c,b,a;4;c2v^9;p 21 c n*;p -2n 21",   //nonstandard
      "33:bca;33.5;b,c,a;4;c2v^9;p c 21 n;p -2n -2ac",  
      "33:a-cb;33.6;a,-c,b;4;c2v^9;p n 21 a;p -2ac -2n",  
      "34;34.1;;4;c2v^10;p n n 2;p 2 -2n",  
      "34:cab;34.2;c,a,b;4;c2v^10;p 2 n n;p -2n 2",  
      "34:bca;34.3;b,c,a;4;c2v^10;p n 2 n;p -2n -2n",  
      "35;35.1;;8;c2v^11;c m m 2;c 2 -2",  
      "35:cab;35.2;c,a,b;8;c2v^11;a 2 m m;a -2 2",  
      "35:bca;35.3;b,c,a;8;c2v^11;b m 2 m;b -2 -2",  
      "36;36.1;;8;c2v^12;c m c 21;c 2c -2",  
      "36*;36.1;;8;c2v^12;c m c 21*;c 21 -2",   //nonstandard
      "36:ba-c;36.2;b,a,-c;8;c2v^12;c c m 21;c 2c -2c",  
      "36:ba-c*;36.2;b,a,-c;8;c2v^12;c c m 21*;c 21 -2c",   //nonstandard
      "36:cab;36.3;c,a,b;8;c2v^12;a 21 m a;a -2a 2a",  
      "36:cab*;36.3;c,a,b;8;c2v^12;a 21 m a*;a -2a 21",   //nonstandard
      "36:-cba;36.4;-c,b,a;8;c2v^12;a 21 a m;a -2 2a",  
      "36:-cba*;36.4;-c,b,a;8;c2v^12;a 21 a m*;a -2 21",   //nonstandard
      "36:bca;36.5;b,c,a;8;c2v^12;b b 21 m;b -2 -2b",  
      "36:a-cb;36.6;a,-c,b;8;c2v^12;b m 21 b;b -2b -2",  
      "37;37.1;;8;c2v^13;c c c 2;c 2 -2c",  
      "37:cab;37.2;c,a,b;8;c2v^13;a 2 a a;a -2a 2",  
      "37:bca;37.3;b,c,a;8;c2v^13;b b 2 b;b -2b -2b",  
      "38;38.1;;8;c2v^14;a m m 2;a 2 -2",  
      "38:ba-c;38.2;b,a,-c;8;c2v^14;b m m 2;b 2 -2",  
      "38:cab;38.3;c,a,b;8;c2v^14;b 2 m m;b -2 2",  
      "38:-cba;38.4;-c,b,a;8;c2v^14;c 2 m m;c -2 2",  
      "38:bca;38.5;b,c,a;8;c2v^14;c m 2 m;c -2 -2",  
      "38:a-cb;38.6;a,-c,b;8;c2v^14;a m 2 m;a -2 -2",  
      "39;39.1;;8;c2v^15;a e m 2;a 2 -2b",   // newer IT name
      "39;39.1;;8;c2v^15;a b m 2;a 2 -2b",  
      "39:ba-c;39.2;b,a,-c;8;c2v^15;b m e 2;b 2 -2a",  
      "39:ba-c;39.2;b,a,-c;8;c2v^15;b m a 2;b 2 -2a",  
      "39:cab;39.3;c,a,b;8;c2v^15;b 2 e m;b -2a 2",  
      "39:cab;39.3;c,a,b;8;c2v^15;b 2 c m;b -2a 2",  
      "39:-cba;39.4;-c,b,a;8;c2v^15;c 2 m e;c -2a 2",  
      "39:-cba;39.4;-c,b,a;8;c2v^15;c 2 m b;c -2a 2",  
      "39:bca;39.5;b,c,a;8;c2v^15;c m 2 e;c -2a -2a",  
      "39:bca;39.5;b,c,a;8;c2v^15;c m 2 a;c -2a -2a",  
      "39:a-cb;39.6;a,-c,b;8;c2v^15;a e 2 m;a -2b -2b",  
      "39:a-cb;39.6;a,-c,b;8;c2v^15;a c 2 m;a -2b -2b",  
      "40;40.1;;8;c2v^16;a m a 2;a 2 -2a",  
      "40:ba-c;40.2;b,a,-c;8;c2v^16;b b m 2;b 2 -2b",  
      "40:cab;40.3;c,a,b;8;c2v^16;b 2 m b;b -2b 2",  
      "40:-cba;40.4;-c,b,a;8;c2v^16;c 2 c m;c -2c 2",  
      "40:bca;40.5;b,c,a;8;c2v^16;c c 2 m;c -2c -2c",  
      "40:a-cb;40.6;a,-c,b;8;c2v^16;a m 2 a;a -2a -2a",  
      "41;41.1;;8;c2v^17;a e a 2;a 2 -2ab",    // newer IT name
      "41;41.1;;8;c2v^17;a b a 2;a 2 -2ab",
      "41:ba-c;41.2;b,a,-c;8;c2v^17;b b e 2;b 2 -2ab",  
      "41:ba-c;41.2;b,a,-c;8;c2v^17;b b a 2;b 2 -2ab",  
      "41:cab;41.3;c,a,b;8;c2v^17;b 2 e b;b -2ab 2",  
      "41:cab;41.3;c,a,b;8;c2v^17;b 2 c b;b -2ab 2",  
      "41:-cba;41.4;-c,b,a;8;c2v^17;c 2 c e;c -2ac 2",  
      "41:-cba;41.4;-c,b,a;8;c2v^17;c 2 c b;c -2ac 2",  
      "41:bca;41.5;b,c,a;8;c2v^17;c c 2 e;c -2ac -2ac",  
      "41:bca;41.5;b,c,a;8;c2v^17;c c 2 a;c -2ac -2ac",  
      "41:a-cb;41.6;a,-c,b;8;c2v^17;a e 2 a;a -2ab -2ab",  
      "41:a-cb;41.6;a,-c,b;8;c2v^17;a c 2 a;a -2ab -2ab",  
      "42;42.1;;16;c2v^18;f m m 2;f 2 -2",  
      "42:cab;42.2;c,a,b;16;c2v^18;f 2 m m;f -2 2",  
      "42:bca;42.3;b,c,a;16;c2v^18;f m 2 m;f -2 -2",  
      "43;43.1;;16;c2v^19;f d d 2;f 2 -2d",  
      "43:cab;43.2;c,a,b;16;c2v^19;f 2 d d;f -2d 2",  
      "43:bca;43.3;b,c,a;16;c2v^19;f d 2 d;f -2d -2d",  
      "44;44.1;;8;c2v^20;i m m 2;i 2 -2",  
      "44:cab;44.2;c,a,b;8;c2v^20;i 2 m m;i -2 2",  
      "44:bca;44.3;b,c,a;8;c2v^20;i m 2 m;i -2 -2",  
      "45;45.1;;8;c2v^21;i b a 2;i 2 -2c",  
      "45:cab;45.2;c,a,b;8;c2v^21;i 2 c b;i -2a 2",  
      "45:bca;45.3;b,c,a;8;c2v^21;i c 2 a;i -2b -2b",  
      "46;46.1;;8;c2v^22;i m a 2;i 2 -2a",  
      "46:ba-c;46.2;b,a,-c;8;c2v^22;i b m 2;i 2 -2b",  
      "46:cab;46.3;c,a,b;8;c2v^22;i 2 m b;i -2b 2",  
      "46:-cba;46.4;-c,b,a;8;c2v^22;i 2 c m;i -2c 2",  
      "46:bca;46.5;b,c,a;8;c2v^22;i c 2 m;i -2c -2c",  
      "46:a-cb;46.6;a,-c,b;8;c2v^22;i m 2 a;i -2a -2a",  
      "47;47.1;;8;d2h^1;p m m m;-p 2 2",  
      "48:2;48.1;;8;d2h^2;p n n n :2;-p 2ab 2bc",  
      "48:1;48.2;a,b,c|1/4,1/4,1/4;8;d2h^2;p n n n :1;p 2 2 -1n",
      "49;49.1;;8;d2h^3;p c c m;-p 2 2c",  
      "49:cab;49.2;c,a,b;8;d2h^3;p m a a;-p 2a 2",  
      "49:bca;49.3;b,c,a;8;d2h^3;p b m b;-p 2b 2b",  
      "50:2;50.1;;8;d2h^4;p b a n :2;-p 2ab 2b",  
      "50:2cab;50.2;c,a,b;8;d2h^4;p n c b :2;-p 2b 2bc",  
      "50:2bca;50.3;b,c,a;8;d2h^4;p c n a :2;-p 2a 2c",  
      "50:1;50.4;a,b,c|1/4,1/4,0;8;d2h^4;p b a n :1;p 2 2 -1ab",
      "50:1cab;50.5;c,a,b|1/4,1/4,0;8;d2h^4;p n c b :1;p 2 2 -1bc",  
      "50:1bca;50.6;b,c,a|1/4,1/4,0;8;d2h^4;p c n a :1;p 2 2 -1ac",  
      "51;51.1;;8;d2h^5;p m m a;-p 2a 2a",  
      "51:ba-c;51.2;b,a,-c;8;d2h^5;p m m b;-p 2b 2",  
      "51:cab;51.3;c,a,b;8;d2h^5;p b m m;-p 2 2b",  
      "51:-cba;51.4;-c,b,a;8;d2h^5;p c m m;-p 2c 2c",  
      "51:bca;51.5;b,c,a;8;d2h^5;p m c m;-p 2c 2",  
      "51:a-cb;51.6;a,-c,b;8;d2h^5;p m a m;-p 2 2a",  
      "52;52.1;;8;d2h^6;p n n a;-p 2a 2bc",  
      "52:ba-c;52.2;b,a,-c;8;d2h^6;p n n b;-p 2b 2n",  
      "52:cab;52.3;c,a,b;8;d2h^6;p b n n;-p 2n 2b",  
      "52:-cba;52.4;-c,b,a;8;d2h^6;p c n n;-p 2ab 2c",  
      "52:bca;52.5;b,c,a;8;d2h^6;p n c n;-p 2ab 2n",  
      "52:a-cb;52.6;a,-c,b;8;d2h^6;p n a n;-p 2n 2bc",  
      "53;53.1;;8;d2h^7;p m n a;-p 2ac 2",  
      "53:ba-c;53.2;b,a,-c;8;d2h^7;p n m b;-p 2bc 2bc",  
      "53:cab;53.3;c,a,b;8;d2h^7;p b m n;-p 2ab 2ab",  
      "53:-cba;53.4;-c,b,a;8;d2h^7;p c n m;-p 2 2ac",  
      "53:bca;53.5;b,c,a;8;d2h^7;p n c m;-p 2 2bc",  
      "53:a-cb;53.6;a,-c,b;8;d2h^7;p m a n;-p 2ab 2",  
      "54;54.1;;8;d2h^8;p c c a;-p 2a 2ac",  
      "54:ba-c;54.2;b,a,-c;8;d2h^8;p c c b;-p 2b 2c",  
      "54:cab;54.3;c,a,b;8;d2h^8;p b a a;-p 2a 2b",  
      "54:-cba;54.4;-c,b,a;8;d2h^8;p c a a;-p 2ac 2c",  
      "54:bca;54.5;b,c,a;8;d2h^8;p b c b;-p 2bc 2b",  
      "54:a-cb;54.6;a,-c,b;8;d2h^8;p b a b;-p 2b 2ab",  
      "55;55.1;;8;d2h^9;p b a m;-p 2 2ab",  
      "55:cab;55.2;c,a,b;8;d2h^9;p m c b;-p 2bc 2",  
      "55:bca;55.3;b,c,a;8;d2h^9;p c m a;-p 2ac 2ac",  
      "56;56.1;;8;d2h^10;p c c n;-p 2ab 2ac",  
      "56:cab;56.2;c,a,b;8;d2h^10;p n a a;-p 2ac 2bc",  
      "56:bca;56.3;b,c,a;8;d2h^10;p b n b;-p 2bc 2ab",  
      "57;57.1;;8;d2h^11;p b c m;-p 2c 2b",  
      "57:ba-c;57.2;b,a,-c;8;d2h^11;p c a m;-p 2c 2ac",  
      "57:cab;57.3;c,a,b;8;d2h^11;p m c a;-p 2ac 2a",  
      "57:-cba;57.4;-c,b,a;8;d2h^11;p m a b;-p 2b 2a",  
      "57:bca;57.5;b,c,a;8;d2h^11;p b m a;-p 2a 2ab",  
      "57:a-cb;57.6;a,-c,b;8;d2h^11;p c m b;-p 2bc 2c",  
      "58;58.1;;8;d2h^12;p n n m;-p 2 2n",  
      "58:cab;58.2;c,a,b;8;d2h^12;p m n n;-p 2n 2",  
      "58:bca;58.3;b,c,a;8;d2h^12;p n m n;-p 2n 2n",  
      "59:2;59.1;;8;d2h^13;p m m n :2;-p 2ab 2a",  
      "59:2cab;59.2;c,a,b;8;d2h^13;p n m m :2;-p 2c 2bc",  
      "59:2bca;59.3;b,c,a;8;d2h^13;p m n m :2;-p 2c 2a",  
      "59:1;59.4;a,b,c|1/4,1/4,0;8;d2h^13;p m m n :1;p 2 2ab -1ab",
      "59:1cab;59.5;c,a,b|1/4,1/4,0;8;d2h^13;p n m m :1;p 2bc 2 -1bc",  
      "59:1bca;59.6;b,c,a|1/4,1/4,0;8;d2h^13;p m n m :1;p 2ac 2ac -1ac",  
      "60;60.1;;8;d2h^14;p b c n;-p 2n 2ab",  
      "60:ba-c;60.2;b,a,-c;8;d2h^14;p c a n;-p 2n 2c",  
      "60:cab;60.3;c,a,b;8;d2h^14;p n c a;-p 2a 2n",  
      "60:-cba;60.4;-c,b,a;8;d2h^14;p n a b;-p 2bc 2n",  
      "60:bca;60.5;b,c,a;8;d2h^14;p b n a;-p 2ac 2b",  
      "60:a-cb;60.6;a,-c,b;8;d2h^14;p c n b;-p 2b 2ac",  
      "61;61.1;;8;d2h^15;p b c a;-p 2ac 2ab",  
      "61:ba-c;61.2;b,a,-c;8;d2h^15;p c a b;-p 2bc 2ac",  
      "62;62.1;;8;d2h^16;p n m a;-p 2ac 2n",  
      "62:ba-c;62.2;b,a,-c;8;d2h^16;p m n b;-p 2bc 2a",  
      "62:cab;62.3;c,a,b;8;d2h^16;p b n m;-p 2c 2ab",  
      "62:-cba;62.4;-c,b,a;8;d2h^16;p c m n;-p 2n 2ac",  
      "62:bca;62.5;b,c,a;8;d2h^16;p m c n;-p 2n 2a",  
      "62:a-cb;62.6;a,-c,b;8;d2h^16;p n a m;-p 2c 2n",  
      "63;63.1;;16;d2h^17;c m c m;-c 2c 2",  
      "63:ba-c;63.2;b,a,-c;16;d2h^17;c c m m;-c 2c 2c",  
      "63:cab;63.3;c,a,b;16;d2h^17;a m m a;-a 2a 2a",  
      "63:-cba;63.4;-c,b,a;16;d2h^17;a m a m;-a 2 2a",  
      "63:bca;63.5;b,c,a;16;d2h^17;b b m m;-b 2 2b",  
      "63:a-cb;63.6;a,-c,b;16;d2h^17;b m m b;-b 2b 2",  
      "64;64.1;;16;d2h^18;c m c e;-c 2ac 2",   // newer IT name
      "64;64.1;;16;d2h^18;c m c a;-c 2ac 2",  
      "64:ba-c;64.2;b,a,-c;16;d2h^18;c c m e;-c 2ac 2ac",  
      "64:ba-c;64.2;b,a,-c;16;d2h^18;c c m b;-c 2ac 2ac",  
      "64:cab;64.3;c,a,b;16;d2h^18;a e m a;-a 2ab 2ab",  
      "64:cab;64.3;c,a,b;16;d2h^18;a b m a;-a 2ab 2ab",  
      "64:-cba;64.4;-c,b,a;16;d2h^18;a e a m;-a 2 2ab",  
      "64:-cba;64.4;-c,b,a;16;d2h^18;a c a m;-a 2 2ab",  
      "64:bca;64.5;b,c,a;16;d2h^18;b b e m;-b 2 2ab",  
      "64:bca;64.5;b,c,a;16;d2h^18;b b c m;-b 2 2ab",  
      "64:a-cb;64.6;a,-c,b;16;d2h^18;b m e b;-b 2ab 2",  
      "64:a-cb;64.6;a,-c,b;16;d2h^18;b m a b;-b 2ab 2",  
      "65;65.1;;16;d2h^19;c m m m;-c 2 2",  
      "65:cab;65.2;c,a,b;16;d2h^19;a m m m;-a 2 2",  
      "65:bca;65.3;b,c,a;16;d2h^19;b m m m;-b 2 2",  
      "66;66.1;;16;d2h^20;c c c m;-c 2 2c",  
      "66:cab;66.2;c,a,b;16;d2h^20;a m a a;-a 2a 2",  
      "66:bca;66.3;b,c,a;16;d2h^20;b b m b;-b 2b 2b",  
      "67;67.1;;16;d2h^21;c m m e;-c 2a 2",   // newer IT name
      "67;67.1;;16;d2h^21;c m m a;-c 2a 2",  
      "67:ba-c;67.2;b,a,-c;16;d2h^21;c m m b;-c 2a 2a",  
      "67:cab;67.3;c,a,b;16;d2h^21;a b m m;-a 2b 2b",  
      "67:-cba;67.4;-c,b,a;16;d2h^21;a c m m;-a 2 2b",  
      "67:bca;67.5;b,c,a;16;d2h^21;b m c m;-b 2 2a",  
      "67:a-cb;67.6;a,-c,b;16;d2h^21;b m a m;-b 2a 2",  
      "68:2;68.1;;16;d2h^22;c c c e :2;-c 2a 2ac",  
      "68:2;68.1;;16;d2h^22;c c c a :2;-c 2a 2ac",  
      "68:2ba-c;68.2;b,a,-c;16;d2h^22;c c c b :2;-c 2a 2c",  
      "68:2cab;68.3;c,a,b;16;d2h^22;a b a a :2;-a 2a 2b",  
      "68:2-cba;68.4;-c,b,a;16;d2h^22;a c a a :2;-a 2ab 2b",  
      "68:2bca;68.5;b,c,a;16;d2h^22;b b c b :2;-b 2ab 2b",  
      "68:2a-cb;68.6;a,-c,b;16;d2h^22;b b a b :2;-b 2b 2ab",  
      "68:1;68.7;a,b,c|0,1/4,1/4;16;d2h^22;c c c e :1;c 2 2 -1ac",  
      "68:1;68.7;a,b,c|0,1/4,1/4;16;d2h^22;c c c a :1;c 2 2 -1ac",  
      "68:1ba-c;68.8;b,a,-c|0,1/4,1/4;16;d2h^22;c c c b :1;c 2 2 -1ac",  
      "68:1cab;68.9;c,a,b|0,1/4,1/4;16;d2h^22;a b a a :1;a 2 2 -1ab",  
      "68:1-cba;68.10;-c,b,a|0,1/4,1/4;16;d2h^22;a c a a :1;a 2 2 -1ab",  
      "68:1bca;68.11;b,c,a|0,1/4,1/4;16;d2h^22;b b c b :1;b 2 2 -1ab",  
      "68:1a-cb;68.12;a,-c,b|0,1/4,1/4;16;d2h^22;b b a b :1;b 2 2 -1ab",  
      "69;69.1;;32;d2h^23;f m m m;-f 2 2",  
      "70:2;70.1;;32;d2h^24;f d d d :2;-f 2uv 2vw",  
      "70:1;70.2;a,b,c|-1/8,-1/8,-1/8;32;d2h^24;f d d d :1;f 2 2 -1d",
      "71;71.1;;16;d2h^25;i m m m;-i 2 2",  
      "72;72.1;;16;d2h^26;i b a m;-i 2 2c",  
      "72:cab;72.2;c,a,b;16;d2h^26;i m c b;-i 2a 2",  
      "72:bca;72.3;b,c,a;16;d2h^26;i c m a;-i 2b 2b",  
      "73;73.1;;16;d2h^27;i b c a;-i 2b 2c",  
      "73:ba-c;73.2;b,a,-c;16;d2h^27;i c a b;-i 2a 2b",  
      "74;74.1;;16;d2h^28;i m m a;-i 2b 2",  
      "74:ba-c;74.2;b,a,-c;16;d2h^28;i m m b;-i 2a 2a",  
      "74:cab;74.3;c,a,b;16;d2h^28;i b m m;-i 2c 2c",  
      "74:-cba;74.4;-c,b,a;16;d2h^28;i c m m;-i 2 2b",  
      "74:bca;74.5;b,c,a;16;d2h^28;i m c m;-i 2 2a",  
      "74:a-cb;74.6;a,-c,b;16;d2h^28;i m a m;-i 2c 2",  
      "75;75.1;;4;c4^1;p 4;p 4",  
      "75:c;75.2;ab;8;c4^1;c 4;c 4", // ITA 75.2 
      "76;76.1;;4;c4^2;p 41;p 4w",  
      "76*;76.1;;4;c4^2;p 41*;p 41",   //nonstandard
      "76:c;76.2;ab;8;c4^2;c 41;c 4w", // ITA 76.2  
      "77;77.1;;4;c4^3;p 42;p 4c",  
      "77*;77.1;;4;c4^3;p 42*;p 42",   //nonstandard
      "77:c;77.2;ab;8;c4^3;c 42;c 4c", // ITA 77.2  
      "78;78.1;;4;c4^4;p 43;p 4cw",  
      "78*;78.1;;4;c4^4;p 43*;p 43",   //nonstandard
      "78:c;78.2;ab;8;c4^4;c 43;c 4cw", // ITA 78.2  
      "79;79.1;;8;c4^5;i 4;i 4",  
      "79:f;79.2;ab;16;c4^5;f 4;f 4", // ITA 79.2  
      "80;80.1;;8;c4^6;i 41;i 4bw",  
      "80:f;80.2;ab;16;c4^6;f 41;xyz",//;x,y,z;-x,-y+1/2,z+1/2;-y+3/4,x+1/4,z+1/4;y+1/4,-x+1/4,z+3/4;x,y+1/2,z+1/2;-x,-y,z;-y+1/4,x+1/4,z+3/4;y+3/4,-x+1/4,z+1/4;x+1/2,y+1/2,z;-x+1/2,-y,z+1/2;-y+1/4,x+3/4,z+1/4;y+3/4,-x+3/4,z+3/4;x+1/2,y,z+1/2;-x+1/2,-y+1/2,z;-y+3/4,x+3/4,z+3/4;y+1/4,-x+3/4,z+1/4", // ITA 80.2
      "81;81.1;;4;s4^1;p -4;p -4",  
      "81:c;81.2;ab;8;s4^1;c -4;c -4",  // ITA 81.2
      "82;82.1;;8;s4^2;i -4;i -4",  
      "82:f;82.2;ab;16;s4^2;f -4;f -4",  // ITA 82.2  
      "83;83.1;;8;c4h^1;p 4/m;-p 4",  
      "83:f;83.2;ab;16;c4h^1;c 4/m;-c 4",  // ITA 83.2  
      "84;84.1;;8;c4h^2;p 42/m;-p 4c",  
      "84*;84.1;;8;c4h^2;p 42/m*;-p 42",   //nonstandard
      "84:c;84.2;ab;16;c4h^2;c 42/m;-c 4c", // ITA 84.2  
      "85:2;85.1;;8;c4h^3;p 4/n :2;-p 4a",  
      "85:c2;85.2;ab;16;c4h^3;c 4/e :2;xyz",//;x,y,z;-x,-y+1/2,z;-y+1/4,x+1/4,z;y+3/4,-x+1/4,z;-x,-y,-z;x,y+1/2,-z;y+1/4,-x+1/4,-z;-y+3/4,x+1/4,-z;x+1/2,y+1/2,z;-x+1/2,-y,z;-y+3/4,x+3/4,z;y+1/4,-x+3/4,z;-x+1/2,-y+1/2,-z;x+1/2,y,-z;y+3/4,-x+3/4,-z;-y+1/4,x+3/4,-z", // ITA 85.2
      "85:1;85.3;a,b,c|-1/4,1/4,0;8;c4h^3;p 4/n :1;p 4ab -1ab",
      "85:c1;85.4;ab|-1/4,1/4,0;16;c4h^3;c 4/e :1;xyz",//;x,y,z;-x+1/2,-y+1/2,z;-y+1/2,x,z;y,-x+1/2,z;-x+1/2,-y,-z;x,y+1/2,-z;y+1/2,-x+1/2,-z;-y,x,-z;x+1/2,y+1/2,z;-x,-y,z;-y,x+1/2,z;y+1/2,-x,z;-x,-y+1/2,-z;x+1/2,y,-z;y,-x,-z;-y+1/2,x+1/2,-z", // ITA 85.4
      "86:2;86.1;;8;c4h^4;p 42/n :2;-p 4bc",  
      "86:1;86.3;a,b,c|-1/4,-1/4,-1/4;8;c4h^4;p 42/n :1;p 4n -1n",
      "86:c1;86.4;ab|-1/4,-1/4,-1/4;16;c4h^4;c 42/e :1;xyz",//;x,y,z;-x,-y,z;-y,x+1/2,z+1/2;y,-x+1/2,z+1/2;-x,-y+1/2,-z+1/2;x,y+1/2,-z+1/2;y+1/2,-x+1/2,-z;-y+1/2,x+1/2,-z;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y+1/2,x,z+1/2;y+1/2,-x,z+1/2;-x+1/2,-y,-z+1/2;x+1/2,y,-z+1/2;y,-x,-z;-y,x,-z", // ITA 86.4
      "86:c2;86.2;ab;16;c4h^4;c 42/e :2;xyz",//;x,y,z;-x,-y+1/2,z;-y+3/4,x+1/4,z+1/2;y+1/4,-x+1/4,z+1/2;-x,-y,-z;x,y+1/2,-z;y+3/4,-x+1/4,-z+1/2;-y+1/4,x+1/4,-z+1/2;x+1/2,y+1/2,z;-x+1/2,-y,z;-y+1/4,x+3/4,z+1/2;y+3/4,-x+3/4,z+1/2;-x+1/2,-y+1/2,-z;x+1/2,y,-z;y+1/4,-x+3/4,-z+1/2;-y+3/4,x+3/4,-z+1/2", // ITA 86.2
      "87;87.1;;16;c4h^5;i 4/m;-i 4",  
      "87:f;87.2;ab;32;c4h^5;f 4/m;xyz",//;x,y,z;-x,-y,z;-y,x,z;y,-x,z;-x,-y,-z;x,y,-z;y,-x,-z;-y,x,-z;x,y+1/2,z+1/2;-x,-y+1/2,z+1/2;-y,x+1/2,z+1/2;y,-x+1/2,z+1/2;-x,-y+1/2,-z+1/2;x,y+1/2,-z+1/2;y,-x+1/2,-z+1/2;-y,x+1/2,-z+1/2;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y+1/2,x+1/2,z;y+1/2,-x+1/2,z;-x+1/2,-y+1/2,-z;x+1/2,y+1/2,-z;y+1/2,-x+1/2,-z;-y+1/2,x+1/2,-z;x+1/2,y,z+1/2;-x+1/2,-y,z+1/2;-y+1/2,x,z+1/2;y+1/2,-x,z+1/2;-x+1/2,-y,-z+1/2;x+1/2,y,-z+1/2;y+1/2,-x,-z+1/2;-y+1/2,x,-z+1/2", // ITA 87.2
      "88:2;88.1;;16;c4h^6;i 41/a :2;-i 4ad",  
      "88:f2;88.2;ab;32;c4h^6;f 41/d :2;xyz",//;x,y,z;-x+1/4,-y+1/4,z+1/2;-y+1/4,x+1/2,z+1/4;y,-x+3/4,z+3/4;-x,-y,-z;x+1/4,y+1/4,-z+1/2;y+3/4,-x+1/2,-z+3/4;-y,x+1/4,-z+1/4;x,y+1/2,z+1/2;-x+3/4,-y+1/4,z;-y+3/4,x+1/2,z+3/4;y,-x+1/4,z+1/4;-x,-y+1/2,-z+1/2;x+3/4,y+1/4,-z;y+1/4,-x+1/2,-z+1/4;-y,x+3/4,-z+3/4;x+1/2,y+1/2,z;-x+3/4,-y+3/4,z+1/2;-y+3/4,x,z+1/4;y+1/2,-x+5/4,z+3/4;-x+1/2,-y+1/2,-z;x+3/4,y+3/4,-z+1/2;y+1/4,-x,-z+3/4;-y+1/2,x+3/4,-z+1/4;x+1/2,y,z+1/2;-x+1/4,-y+3/4,z;-y+1/4,x,z+3/4;y+1/2,-x+3/4,z+1/4;-x+1/2,-y,-z+1/2;x+1/4,y+3/4,-z;y+3/4,-x,-z+1/4;-y+1/2,x+5/4,-z+3/4", // ITA 88.2
      "88:1;88.3;a,b,c|0,-1/4,-1/8;16;c4h^6;i 41/a :1;i 4bw -1bw",
      "88:f1;88.4;ab|0,-1/4,-1/8;32;c4h^6;f 41/d :1;xyz",//;x,y,z;-x,-y+1/2,z+1/2;-y+1/4,x+3/4,z+1/4;y+3/4,-x+3/4,z+3/4;-x+3/4,-y+1/4,-z+1/4;x+1/4,y+1/4,-z+3/4;y+1/2,-x+1/2,-z;-y,x+1/2,-z+1/2;x,y+1/2,z+1/2;-x+1/2,-y+1/2,z;-y+3/4,x+3/4,z+3/4;y+3/4,-x+1/4,z+1/4;-x+3/4,-y+3/4,-z+3/4;x+3/4,y+1/4,-z+1/4;y,-x+1/2,-z+1/2;-y,x,-z;x+1/2,y+1/2,z;-x+1/2,-y,z+1/2;-y+3/4,x+5/4,z+1/4;y+1/4,-x+5/4,z+3/4;-x+1/4,-y+3/4,-z+1/4;x+3/4,y+3/4,-z+3/4;y,-x,-z;-y+1/2,x,-z+1/2;x+1/2,y,z+1/2;-x,-y,z;-y+1/4,x+5/4,z+3/4;y+1/4,-x+3/4,z+1/4;-x+1/4,-y+5/4,-z+3/4;x+1/4,y+3/4,-z+1/4;y+1/2,-x,-z+1/2;-y+1/2,x+1/2,-z", // ITA 88.4
      "89;89.1;;8;d4^1;p 4 2 2;p 4 2",  
      "89:c;89.2;ab;16;d4^1;c 4 2 2;xyz",//;x,y,z;-x,-y,z;-y,x,z;y,-x,z;-y,-x,-z;y,x,-z;-x,y,-z;x,-y,-z;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y+1/2,x+1/2,z;y+1/2,-x+1/2,z;-y+1/2,-x+1/2,-z;y+1/2,x+1/2,-z;-x+1/2,y+1/2,-z;x+1/2,-y+1/2,-z", // ITA 89.2
      "90;90.1;;8;d4^2;p 4 21 2;p 4ab 2ab",  
      "90:c;90.2;ab;16;d4^2;c 4 2 21;xyz",//;x,y,z;-x,-y,z;-y,x+1/2,z;y,-x+1/2,z;-y,-x+1/2,-z;y,x+1/2,-z;-x,y,-z;x,-y,-z;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y+1/2,x,z;y+1/2,-x,z;-y+1/2,-x,-z;y+1/2,x,-z;-x+1/2,y+1/2,-z;x+1/2,-y+1/2,-z", // ITA 90.2    
      "91;91.1;;8;d4^3;p 41 2 2;p 4w 2c",  
      "91*;91.1;;8;d4^3;p 41 2 2*;p 41 2c",   //nonstandard
      "91:c;91.2;ab;16;d4^3;c 41 2 2;xyz",//;x,y,z;-x,-y,z+1/2;-y,x,z+1/4;y,-x,z+3/4;-y,-x,-z;y,x,-z+1/2;-x,y,-z+3/4;x,-y,-z+1/4;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z+1/2;-y+1/2,x+1/2,z+1/4;y+1/2,-x+1/2,z+3/4;-y+1/2,-x+1/2,-z;y+1/2,x+1/2,-z+1/2;-x+1/2,y+1/2,-z+3/4;x+1/2,-y+1/2,-z+1/4", // ITA 91.2
      "92;92.1;;8;d4^4;p 41 21 2;p 4abw 2nw",  
      "92:c;92.2;ab;16;d4^4;c 41 2 21;xyz",//;x,y,z;-x,-y,z+1/2;-y,x+1/2,z+1/4;y,-x+1/2,z+3/4;-y,-x+1/2,-z+1/4;y,x+1/2,-z+3/4;-x,y,-z;x,-y,-z+1/2;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z+1/2;-y+1/2,x,z+1/4;y+1/2,-x,z+3/4;-y+1/2,-x,-z+1/4;y+1/2,x,-z+3/4;-x+1/2,y+1/2,-z;x+1/2,-y+1/2,-z+1/2", // ITA 92.2
      "93;93.1;;8;d4^5;p 42 2 2;p 4c 2",  
      "93*;93.1;;8;d4^5;p 42 2 2*;p 42 2",   //nonstandard
      "93:c;93.2;ab;16;d4^5;c 42 2 2;xyz",//;x,y,z;-x,-y,z;-y,x,z+1/2;y,-x,z+1/2;-y,-x,-z;y,x,-z;-x,y,-z+1/2;x,-y,-z+1/2;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y+1/2,x+1/2,z+1/2;y+1/2,-x+1/2,z+1/2;-y+1/2,-x+1/2,-z;y+1/2,x+1/2,-z;-x+1/2,y+1/2,-z+1/2;x+1/2,-y+1/2,-z+1/2", // ITA 93.2
      "94;94.1;;8;d4^6;p 42 21 2;p 4n 2n",  
      "94:c;94.2;ab;16;d4^6;c 42 2 21;xyz",//;x,y,z;-x,-y,z;-y,x+1/2,z+1/2;y,-x+1/2,z+1/2;-y,-x+1/2,-z+1/2;y,x+1/2,-z+1/2;-x,y,-z;x,-y,-z;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y+1/2,x,z+1/2;y+1/2,-x,z+1/2;-y+1/2,-x,-z+1/2;y+1/2,x,-z+1/2;-x+1/2,y+1/2,-z;x+1/2,-y+1/2,-z", // ITA 94.2
      "95;95.1;;8;d4^7;p 43 2 2;p 4cw 2c",  
      "95*;95.1;;8;d4^7;p 43 2 2*;p 43 2c",   //nonstandard
      "95:c;95.2;ab;16;d4^7;c 43 2 2;xyz",//;x,y,z;-x,-y,z+1/2;-y,x,z+3/4;y,-x,z+1/4;-y,-x,-z;y,x,-z+1/2;-x,y,-z+1/4;x,-y,-z+3/4;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z+1/2;-y+1/2,x+1/2,z+3/4;y+1/2,-x+1/2,z+1/4;-y+1/2,-x+1/2,-z;y+1/2,x+1/2,-z+1/2;-x+1/2,y+1/2,-z+1/4;x+1/2,-y+1/2,-z+3/4", // ITA 95.2
      "96;96.1;;8;d4^8;p 43 21 2;p 4nw 2abw",  
      "96:c;96.2;ab;16;d4^8;c 43 2 21;xyz",//;x,y,z;-x,-y,z+1/2;-y,x+1/2,z+3/4;y,-x+1/2,z+1/4;-y,-x+1/2,-z+3/4;y,x+1/2,-z+1/4;-x,y,-z;x,-y,-z+1/2;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z+1/2;-y+1/2,x,z+3/4;y+1/2,-x,z+1/4;-y+1/2,-x,-z+3/4;y+1/2,x,-z+1/4;-x+1/2,y+1/2,-z;x+1/2,-y+1/2,-z+1/2", // ITA 96.2
      "97;97.1;;16;d4^9;i 4 2 2;i 4 2",  
      "97:f;97.2;ab;32;d4^9;f 4 2 2;f 4 2", // ITA 97.2  
      "98;98.1;;16;d4^10;i 41 2 2;i 4bw 2bw",  
      "98:f;98.2;ab;32;d4^10;f 41 2 2;xyz",//;x,y,z;-x,-y+1/2,z+1/2;-y+3/4,x+1/4,z+1/4;y+1/4,-x+1/4,z+3/4;-y+1/4,-x+1/4,-z+3/4;y+3/4,x+1/4,-z+1/4;-x,y+1/2,-z+1/2;x,-y,-z;x,y+1/2,z+1/2;-x,-y,z;-y+1/4,x+1/4,z+3/4;y+3/4,-x+1/4,z+1/4;-y+3/4,-x+1/4,-z+1/4;y+1/4,x+1/4,-z+3/4;-x,y,-z;x,-y+1/2,-z+1/2;x+1/2,y+1/2,z;-x+1/2,-y,z+1/2;-y+1/4,x+3/4,z+1/4;y+3/4,-x+3/4,z+3/4;-y+3/4,-x+3/4,-z+3/4;y+1/4,x+3/4,-z+1/4;-x+1/2,y,-z+1/2;x+1/2,-y+1/2,-z;x+1/2,y,z+1/2;-x+1/2,-y+1/2,z;-y+3/4,x+3/4,z+3/4;y+1/4,-x+3/4,z+1/4;-y+1/4,-x+3/4,-z+1/4;y+3/4,x+3/4,-z+3/4;-x+1/2,y+1/2,-z;x+1/2,-y,-z+1/2", // ITA 98.2
      "99;99.1;;8;c4v^1;p 4 m m;p 4 -2",  
      "99:c;99.2;ab;16;c4v^1;c 4 m m;c 4 -2",  // ITA 99.2  
      "100;100.1;;8;c4v^2;p 4 b m;p 4 -2ab",  
      "100:c;100.2;ab;16;c4v^2;c 4 m g1;xyz",//;x,y,z;-x,-y,z;-y,x,z;y,-x,z;y,x+1/2,z;-y,-x+1/2,z;x,-y+1/2,z;-x,y+1/2,z;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y+1/2,x+1/2,z;y+1/2,-x+1/2,z;y+1/2,x,z;-y+1/2,-x,z;x+1/2,-y,z;-x+1/2,y,z", // ITA 100.2
      "101;101.1;;8;c4v^3;p 42 c m;p 4c -2c",  
      "101*;101.1;;8;c4v^3;p 42 c m*;p 42 -2c",   //nonstandard
      "101:c;101.2;ab;16;c4v^3;c 42 m c;xyz",//;x,y,z;-x,-y,z;-y,x,z+1/2;y,-x,z+1/2;y,x,z+1/2;-y,-x,z+1/2;x,-y,z;-x,y,z;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y+1/2,x+1/2,z+1/2;y+1/2,-x+1/2,z+1/2;y+1/2,x+1/2,z+1/2;-y+1/2,-x+1/2,z+1/2;x+1/2,-y+1/2,z;-x+1/2,y+1/2,z", // ITA 101.2
      "102;102.1;;8;c4v^4;p 42 n m;p 4n -2n",  
      "102:c;102.2;ab;16;c4v^4;c 42 m g2;xyz",//;x,y,z;-x,-y,z;-y,x+1/2,z+1/2;y,-x+1/2,z+1/2;y,x+1/2,z+1/2;-y,-x+1/2,z+1/2;x,-y,z;-x,y,z;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y+1/2,x,z+1/2;y+1/2,-x,z+1/2;y+1/2,x,z+1/2;-y+1/2,-x,z+1/2;x+1/2,-y+1/2,z;-x+1/2,y+1/2,z", // ITA 102.2
      "103;103.1;;8;c4v^5;p 4 c c;p 4 -2c",  
      "103:c;103.2;ab;16;c4v^5;c 4 c c;c 4 -2c",  
      "104;104.1;;8;c4v^6;p 4 n c;p 4 -2n",  
      "104:c;104.2;ab;16;c4v^6;c 4 c g2;xyz",//;x,y,z;-x,-y,z;-y,x,z;y,-x,z;y,x+1/2,z+1/2;-y,-x+1/2,z+1/2;x,-y+1/2,z+1/2;-x,y+1/2,z+1/2;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y+1/2,x+1/2,z;y+1/2,-x+1/2,z;y+1/2,x,z+1/2;-y+1/2,-x,z+1/2;x+1/2,-y,z+1/2;-x+1/2,y,z+1/2", // ITA 104.2
      "105;105.1;;8;c4v^7;p 42 m c;p 4c -2",  
      "105*;105.1;;8;c4v^7;p 42 m c*;p 42 -2",   //nonstandard
      "105:c;105.2;ab;16;c4v^7;c 42 c m;xyz",//;x,y,z;-x,-y,z;-y,x,z+1/2;y,-x,z+1/2;y,x,z;-y,-x,z;x,-y,z+1/2;-x,y,z+1/2;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y+1/2,x+1/2,z+1/2;y+1/2,-x+1/2,z+1/2;y+1/2,x+1/2,z;-y+1/2,-x+1/2,z;x+1/2,-y+1/2,z+1/2;-x+1/2,y+1/2,z+1/2", // ITA 105.2
      "106;106.1;;8;c4v^8;p 42 b c;p 4c -2ab",  
      "106*;106.1;;8;c4v^8;p 42 b c*;p 42 -2ab",   //nonstandard
      "106:c;106.2;ab;16;c4v^8;c 42 c g1;xyz",//;x,y,z;-x,-y,z;-y,x,z+1/2;y,-x,z+1/2;y,x+1/2,z;-y,-x+1/2,z;x,-y+1/2,z+1/2;-x,y+1/2,z+1/2;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y+1/2,x+1/2,z+1/2;y+1/2,-x+1/2,z+1/2;y+1/2,x,z;-y+1/2,-x,z;x+1/2,-y,z+1/2;-x+1/2,y,z+1/2", // ITA 106.2
      "107;107.1;;16;c4v^9;i 4 m m;i 4 -2",  
      "107:f;107.2;ab;32;c4v^9;f 4 m m;f 4 -2",  
      "108;108.1;;16;c4v^10;i 4 c m;i 4 -2c",  
      "108:f;108.2;ab;32;c4v^10;f 4 m c;xyz",//;x,y,z;-x,-y,z;-y,x,z;y,-x,z;y,x,z+1/2;-y,-x,z+1/2;x,-y,z+1/2;-x,y,z+1/2;x,y+1/2,z+1/2;-x,-y+1/2,z+1/2;-y,x+1/2,z+1/2;y,-x+1/2,z+1/2;y,x+1/2,z;-y,-x+1/2,z;x,-y+1/2,z;-x,y+1/2,z;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y+1/2,x+1/2,z;y+1/2,-x+1/2,z;y+1/2,x+1/2,z+1/2;-y+1/2,-x+1/2,z+1/2;x+1/2,-y+1/2,z+1/2;-x+1/2,y+1/2,z+1/2;x+1/2,y,z+1/2;-x+1/2,-y,z+1/2;-y+1/2,x,z+1/2;y+1/2,-x,z+1/2;y+1/2,x,z;-y+1/2,-x,z;x+1/2,-y,z;-x+1/2,y,z", // ITA 108.2
      "109;109.1;;16;c4v^11;i 41 m d;i 4bw -2",  
      "109:f;109.2;ab;32;c4v^11;f 41 d m;xyz",//;x,y,z;-x,-y+1/2,z+1/2;-y+3/4,x+1/4,z+1/4;y+1/4,-x+1/4,z+3/4;y,x,z;-y,-x+1/2,z+1/2;x+3/4,-y+1/4,z+1/4;-x+1/4,y+1/4,z+3/4;x,y+1/2,z+1/2;-x,-y,z;-y+1/4,x+1/4,z+3/4;y+3/4,-x+1/4,z+1/4;y,x+1/2,z+1/2;-y,-x,z;x+1/4,-y+1/4,z+3/4;-x+3/4,y+1/4,z+1/4;x+1/2,y+1/2,z;-x+1/2,-y,z+1/2;-y+1/4,x+3/4,z+1/4;y+3/4,-x+3/4,z+3/4;y+1/2,x+1/2,z;-y+1/2,-x,z+1/2;x+1/4,-y+3/4,z+1/4;-x+3/4,y+3/4,z+3/4;x+1/2,y,z+1/2;-x+1/2,-y+1/2,z;-y+3/4,x+3/4,z+3/4;y+1/4,-x+3/4,z+1/4;y+1/2,x,z+1/2;-y+1/2,-x+1/2,z;x+3/4,-y+3/4,z+3/4;-x+1/4,y+3/4,z+1/4", // ITA 109.2
      "110;110.1;;16;c4v^12;i 41 c d;i 4bw -2c", 
      "110:f;110.2;ab;32;c4v^12;f 41 d c;xyz",//;x,y,z;-x,-y+1/2,z+1/2;-y+3/4,x+1/4,z+1/4;y+1/4,-x+1/4,z+3/4;y,x,z+1/2;-y,-x+1/2,z;x+3/4,-y+1/4,z+3/4;-x+1/4,y+1/4,z+1/4;x,y+1/2,z+1/2;-x,-y,z;-y+1/4,x+1/4,z+3/4;y+3/4,-x+1/4,z+1/4;y,x+1/2,z;-y,-x,z+1/2;x+1/4,-y+1/4,z+1/4;-x+3/4,y+1/4,z+3/4;x+1/2,y+1/2,z;-x+1/2,-y,z+1/2;-y+1/4,x+3/4,z+1/4;y+3/4,-x+3/4,z+3/4;y+1/2,x+1/2,z+1/2;-y+1/2,-x,z;x+1/4,-y+3/4,z+3/4;-x+3/4,y+3/4,z+1/4;x+1/2,y,z+1/2;-x+1/2,-y+1/2,z;-y+3/4,x+3/4,z+3/4;y+1/4,-x+3/4,z+1/4;y+1/2,x,z;-y+1/2,-x+1/2,z+1/2;x+3/4,-y+3/4,z+1/4;-x+1/4,y+3/4,z+3/4", // ITA 110.2
      "111;111.1;;8;d2d^1;p -4 2 m;p -4 2",  
      "111:c;111.2;ab;16;d2d^1;c -4 m 2;xyz",//;x,y,z;-x,-y,z;y,-x,-z;-y,x,-z;-y,-x,-z;y,x,-z;x,-y,z;-x,y,z;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;y+1/2,-x+1/2,-z;-y+1/2,x+1/2,-z;-y+1/2,-x+1/2,-z;y+1/2,x+1/2,-z;x+1/2,-y+1/2,z;-x+1/2,y+1/2,z", // ITA 111.2
      "112;112.1;;8;d2d^2;p -4 2 c;p -4 2c",  
      "112:c;112.2;ab;16;d2d^2;c -4 c 2;xyz",//;x,y,z;-x,-y,z;y,-x,-z;-y,x,-z;-y,-x,-z+1/2;y,x,-z+1/2;x,-y,z+1/2;-x,y,z+1/2;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;y+1/2,-x+1/2,-z;-y+1/2,x+1/2,-z;-y+1/2,-x+1/2,-z+1/2;y+1/2,x+1/2,-z+1/2;x+1/2,-y+1/2,z+1/2;-x+1/2,y+1/2,z+1/2", // ITA 112.2
      "113;113.1;;8;d2d^3;p -4 21 m;p -4 2ab",  
      "113:c;113.2;ab;16;d2d^3;c -4 m 21;xyz",//;x,y,z;-x,-y,z;y,-x,-z;-y,x,-z;-y,-x+1/2,-z;y,x+1/2,-z;x,-y+1/2,z;-x,y+1/2,z;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;y+1/2,-x+1/2,-z;-y+1/2,x+1/2,-z;-y+1/2,-x,-z;y+1/2,x,-z;x+1/2,-y,z;-x+1/2,y,z", // ITA 113.2
      "114;114.1;;8;d2d^4;p -4 21 c;p -4 2n",  
      "114:c;114.2;ab;16;d2d^4;c -4 c 21;xyz",//;x,y,z;-x,-y,z;y,-x,-z;-y,x,-z;-y,-x+1/2,-z+1/2;y,x+1/2,-z+1/2;x,-y+1/2,z+1/2;-x,y+1/2,z+1/2;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;y+1/2,-x+1/2,-z;-y+1/2,x+1/2,-z;-y+1/2,-x,-z+1/2;y+1/2,x,-z+1/2;x+1/2,-y,z+1/2;-x+1/2,y,z+1/2", // ITA 114.2
      "115;115.1;;8;d2d^5;p -4 m 2;p -4 -2",  
      "115:c;115.2;ab;16;d2d^5;c -4 2 m;xyz",//;x,y,z;-x,-y,z;y,-x,-z;-y,x,-z;y,x,z;-y,-x,z;-x,y,-z;x,-y,-z;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;y+1/2,-x+1/2,-z;-y+1/2,x+1/2,-z;y+1/2,x+1/2,z;-y+1/2,-x+1/2,z;-x+1/2,y+1/2,-z;x+1/2,-y+1/2,-z", // ITA 115.2
      "116;116.1;;8;d2d^6;p -4 c 2;p -4 -2c",  
      "116:c;116.2;ab;16;d2d^6;c -4 2 c;xyz",//;x,y,z;-x,-y,z;y,-x,-z;-y,x,-z;y,x,z+1/2;-y,-x,z+1/2;-x,y,-z+1/2;x,-y,-z+1/2;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;y+1/2,-x+1/2,-z;-y+1/2,x+1/2,-z;y+1/2,x+1/2,z+1/2;-y+1/2,-x+1/2,z+1/2;-x+1/2,y+1/2,-z+1/2;x+1/2,-y+1/2,-z+1/2", // ITA 116.2
      "117;117.1;;8;d2d^7;p -4 b 2;p -4 -2ab",  
      "117:c;117.2;ab;16;d2d^7;c -4 2 g1;xyz",//;x,y,z;-x,-y,z;y,-x,-z;-y,x,-z;y,x+1/2,z;-y,-x+1/2,z;-x,y+1/2,-z;x,-y+1/2,-z;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;y+1/2,-x+1/2,-z;-y+1/2,x+1/2,-z;y+1/2,x,z;-y+1/2,-x,z;-x+1/2,y,-z;x+1/2,-y,-z", // ITA 117.2
      "118;118.1;;8;d2d^8;p -4 n 2;p -4 -2n",  
      "118:c;118.2;ab;16;d2d^8;c -4 2 g2;xyz",//;x,y,z;-x,-y,z;y,-x,-z;-y,x,-z;y,x+1/2,z+1/2;-y,-x+1/2,z+1/2;-x,y+1/2,-z+1/2;x,-y+1/2,-z+1/2;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;y+1/2,-x+1/2,-z;-y+1/2,x+1/2,-z;y+1/2,x,z+1/2;-y+1/2,-x,z+1/2;-x+1/2,y,-z+1/2;x+1/2,-y,-z+1/2", // ITA 118.2
      "119;119.1;;16;d2d^9;i -4 m 2;i -4 -2",  
      "119:f;119.2;ab;32;d2d^9;f -4 2 m;xyz",//;x,y,z;-x,-y,z;y,-x,-z;-y,x,-z;y,x,z;-y,-x,z;-x,y,-z;x,-y,-z;x,y+1/2,z+1/2;-x,-y+1/2,z+1/2;y,-x+1/2,-z+1/2;-y,x+1/2,-z+1/2;y,x+1/2,z+1/2;-y,-x+1/2,z+1/2;-x,y+1/2,-z+1/2;x,-y+1/2,-z+1/2;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;y+1/2,-x+1/2,-z;-y+1/2,x+1/2,-z;y+1/2,x+1/2,z;-y+1/2,-x+1/2,z;-x+1/2,y+1/2,-z;x+1/2,-y+1/2,-z;x+1/2,y,z+1/2;-x+1/2,-y,z+1/2;y+1/2,-x,-z+1/2;-y+1/2,x,-z+1/2;y+1/2,x,z+1/2;-y+1/2,-x,z+1/2;-x+1/2,y,-z+1/2;x+1/2,-y,-z+1/2", // ITA 119.2
      "120;120.1;;16;d2d^10;i -4 c 2;i -4 -2c",  
      "120:f;120.2;ab;32;d2d^10;f -4 2 c;xyz",//;x,y,z;-x,-y,z;y,-x,-z;-y,x,-z;y,x,z+1/2;-y,-x,z+1/2;-x,y,-z+1/2;x,-y,-z+1/2;x,y+1/2,z+1/2;-x,-y+1/2,z+1/2;y,-x+1/2,-z+1/2;-y,x+1/2,-z+1/2;y,x+1/2,z;-y,-x+1/2,z;-x,y+1/2,-z;x,-y+1/2,-z;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;y+1/2,-x+1/2,-z;-y+1/2,x+1/2,-z;y+1/2,x+1/2,z+1/2;-y+1/2,-x+1/2,z+1/2;-x+1/2,y+1/2,-z+1/2;x+1/2,-y+1/2,-z+1/2;x+1/2,y,z+1/2;-x+1/2,-y,z+1/2;y+1/2,-x,-z+1/2;-y+1/2,x,-z+1/2;y+1/2,x,z;-y+1/2,-x,z;-x+1/2,y,-z;x+1/2,-y,-z", // ITA 120.2
      "121;121.1;;16;d2d^11;i -4 2 m;i -4 2",  
      "121:f;121.2;ab;32;d2d^11;f -4 m 2;xyz",//;x,y,z;-x,-y,z;y,-x,-z;-y,x,-z;-y,-x,-z;y,x,-z;x,-y,z;-x,y,z;x,y+1/2,z+1/2;-x,-y+1/2,z+1/2;y,-x+1/2,-z+1/2;-y,x+1/2,-z+1/2;-y,-x+1/2,-z+1/2;y,x+1/2,-z+1/2;x,-y+1/2,z+1/2;-x,y+1/2,z+1/2;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;y+1/2,-x+1/2,-z;-y+1/2,x+1/2,-z;-y+1/2,-x+1/2,-z;y+1/2,x+1/2,-z;x+1/2,-y+1/2,z;-x+1/2,y+1/2,z;x+1/2,y,z+1/2;-x+1/2,-y,z+1/2;y+1/2,-x,-z+1/2;-y+1/2,x,-z+1/2;-y+1/2,-x,-z+1/2;y+1/2,x,-z+1/2;x+1/2,-y,z+1/2;-x+1/2,y,z+1/2", // ITA 121.2
      "122;122.1;;16;d2d^12;i -4 2 d;i -4 2bw",  
      "122:f;122.2;ab;32;d2d^12;f -4 d 2;xyz",//;x,y,z;-x,-y,z;y,-x,-z;-y,x,-z;-y+1/4,-x+1/4,-z+3/4;y+1/4,x+1/4,-z+3/4;x+1/4,-y+1/4,z+3/4;-x+1/4,y+1/4,z+3/4;x,y+1/2,z+1/2;-x,-y+1/2,z+1/2;y,-x+1/2,-z+1/2;-y,x+1/2,-z+1/2;-y+3/4,-x+1/4,-z+1/4;y+3/4,x+1/4,-z+1/4;x+3/4,-y+1/4,z+1/4;-x+3/4,y+1/4,z+1/4;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;y+1/2,-x+1/2,-z;-y+1/2,x+1/2,-z;-y+3/4,-x+3/4,-z+3/4;y+3/4,x+3/4,-z+3/4;x+3/4,-y+3/4,z+3/4;-x+3/4,y+3/4,z+3/4;x+1/2,y,z+1/2;-x+1/2,-y,z+1/2;y+1/2,-x,-z+1/2;-y+1/2,x,-z+1/2;-y+1/4,-x+3/4,-z+1/4;y+1/4,x+3/4,-z+1/4;x+1/4,-y+3/4,z+1/4;-x+1/4,y+3/4,z+1/4", // ITA 122.2
      "123;123.1;;16;d4h^1;p 4/m m m;-p 4 2",  
      "123:c;123.2;ab;32;d4h^1;c 4/m m m;xyz",//;x,y,z;-x,-y,z;-y,x,z;y,-x,z;-y,-x,-z;y,x,-z;-x,y,-z;x,-y,-z;-x,-y,-z;x,y,-z;y,-x,-z;-y,x,-z;y,x,z;-y,-x,z;x,-y,z;-x,y,z;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y+1/2,x+1/2,z;y+1/2,-x+1/2,z;-y+1/2,-x+1/2,-z;y+1/2,x+1/2,-z;-x+1/2,y+1/2,-z;x+1/2,-y+1/2,-z;-x+1/2,-y+1/2,-z;x+1/2,y+1/2,-z;y+1/2,-x+1/2,-z;-y+1/2,x+1/2,-z;y+1/2,x+1/2,z;-y+1/2,-x+1/2,z;x+1/2,-y+1/2,z;-x+1/2,y+1/2,z", // ITA 123.2
      "124;124.1;;16;d4h^2;p 4/m c c;-p 4 2c",  
      "124:c;124.2;ab;32;d4h^2;c 4/m c c;xyz",//;x,y,z;-x,-y,z;-y,x,z;y,-x,z;-y,-x,-z+1/2;y,x,-z+1/2;-x,y,-z+1/2;x,-y,-z+1/2;-x,-y,-z;x,y,-z;y,-x,-z;-y,x,-z;y,x,z+1/2;-y,-x,z+1/2;x,-y,z+1/2;-x,y,z+1/2;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y+1/2,x+1/2,z;y+1/2,-x+1/2,z;-y+1/2,-x+1/2,-z+1/2;y+1/2,x+1/2,-z+1/2;-x+1/2,y+1/2,-z+1/2;x+1/2,-y+1/2,-z+1/2;-x+1/2,-y+1/2,-z;x+1/2,y+1/2,-z;y+1/2,-x+1/2,-z;-y+1/2,x+1/2,-z;y+1/2,x+1/2,z+1/2;-y+1/2,-x+1/2,z+1/2;x+1/2,-y+1/2,z+1/2;-x+1/2,y+1/2,z+1/2", // ITA 124.2
      "125:2;125.1;;16;d4h^3;p 4/n b m :2;-p 4a 2b",  
      "125:c2;125.2;ab;32;d4h^3;c 4/e m g1 :2;xyz",//;x,y,z;-x,-y+1/2,z;-y+1/4,x+1/4,z;y+3/4,-x+1/4,z;-y+1/4,-x+1/4,-z;y+3/4,x+1/4,-z;-x,y,-z;x,-y+1/2,-z;-x,-y,-z;x,y+1/2,-z;y+1/4,-x+1/4,-z;-y+3/4,x+1/4,-z;y+1/4,x+1/4,z;-y+3/4,-x+1/4,z;x,-y,z;-x,y+1/2,z;x+1/2,y+1/2,z;-x+1/2,-y,z;-y+3/4,x+3/4,z;y+1/4,-x+3/4,z;-y+3/4,-x+3/4,-z;y+1/4,x+3/4,-z;-x+1/2,y+1/2,-z;x+1/2,-y,-z;-x+1/2,-y+1/2,-z;x+1/2,y,-z;y+3/4,-x+3/4,-z;-y+1/4,x+3/4,-z;y+3/4,x+3/4,z;-y+1/4,-x+3/4,z;x+1/2,-y+1/2,z;-x+1/2,y,z", // ITA 125.2
      "125:1;125.3;a,b,c|-1/4,-1/4,0;16;d4h^3;p 4/n b m :1;p 4 2 -1ab",
      "125:c1;125.4;ab|-1/4,-1/4,0;32;d4h^3;c 4/e m g1 :1;xyz",//;x,y,z;-x,-y,z;-y+1/2,x+1/2,z;y+1/2,-x+1/2,z;-y+1/2,-x+1/2,-z;y+1/2,x+1/2,-z;-x,y,-z;x,-y,-z;-x,-y+1/2,-z;x,y+1/2,-z;y,-x+1/2,-z;-y,x+1/2,-z;y,x+1/2,z;-y,-x+1/2,z;x,-y+1/2,z;-x,y+1/2,z;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y,x,z;y,-x,z;-y,-x,-z;y,x,-z;-x+1/2,y+1/2,-z;x+1/2,-y+1/2,-z;-x+1/2,-y,-z;x+1/2,y,-z;y+1/2,-x,-z;-y+1/2,x,-z;y+1/2,x,z;-y+1/2,-x,z;x+1/2,-y,z;-x+1/2,y,z", // ITA 125.4
      "126:2;126.1;;16;d4h^4;p 4/n n c :2;-p 4a 2bc",  
      "126:c1;126.2;ab;32;d4h^4;c 4/e c g2 :2;xyz",//;x,y,z;-x,-y+1/2,z;-y+1/4,x+1/4,z;y+3/4,-x+1/4,z;-y+1/4,-x+1/4,-z+1/2;y+3/4,x+1/4,-z+1/2;-x,y,-z+1/2;x,-y+1/2,-z+1/2;-x,-y,-z;x,y+1/2,-z;y+1/4,-x+1/4,-z;-y+3/4,x+1/4,-z;y+1/4,x+1/4,z+1/2;-y+3/4,-x+1/4,z+1/2;x,-y,z+1/2;-x,y+1/2,z+1/2;x+1/2,y+1/2,z;-x+1/2,-y,z;-y+3/4,x+3/4,z;y+1/4,-x+3/4,z;-y+3/4,-x+3/4,-z+1/2;y+1/4,x+3/4,-z+1/2;-x+1/2,y+1/2,-z+1/2;x+1/2,-y,-z+1/2;-x+1/2,-y+1/2,-z;x+1/2,y,-z;y+3/4,-x+3/4,-z;-y+1/4,x+3/4,-z;y+3/4,x+3/4,z+1/2;-y+1/4,-x+3/4,z+1/2;x+1/2,-y+1/2,z+1/2;-x+1/2,y,z+1/2", // ITA 126.2
      "126:1;126.3;a,b,c|-1/4,-1/4,-1/4;16;d4h^4;p 4/n n c :1;p 4 2 -1n",
      "126:c4;126.4;ab|-1/4,-1/4,-1/4;32;d4h^4;c 4/e c g2 :1;xyz",//;x,y,z;-x,-y,z;-y+1/2,x+1/2,z;y+1/2,-x+1/2,z;-y+1/2,-x+1/2,-z;y+1/2,x+1/2,-z;-x,y,-z;x,-y,-z;-x,-y+1/2,-z+1/2;x,y+1/2,-z+1/2;y,-x+1/2,-z+1/2;-y,x+1/2,-z+1/2;y,x+1/2,z+1/2;-y,-x+1/2,z+1/2;x,-y+1/2,z+1/2;-x,y+1/2,z+1/2;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y,x,z;y,-x,z;-y,-x,-z;y,x,-z;-x+1/2,y+1/2,-z;x+1/2,-y+1/2,-z;-x+1/2,-y,-z+1/2;x+1/2,y,-z+1/2;y+1/2,-x,-z+1/2;-y+1/2,x,-z+1/2;y+1/2,x,z+1/2;-y+1/2,-x,z+1/2;x+1/2,-y,z+1/2;-x+1/2,y,z+1/2", // ITA 126.4
      "127;127.1;;16;d4h^5;p 4/m b m;-p 4 2ab",  
      "127:c;127.2;ab;32;d4h^5;c 4/m m g1;xyz",//;x,y,z;-x,-y,z;-y,x,z;y,-x,z;-y,-x+1/2,-z;y,x+1/2,-z;-x,y+1/2,-z;x,-y+1/2,-z;-x,-y,-z;x,y,-z;y,-x,-z;-y,x,-z;y,x+1/2,z;-y,-x+1/2,z;x,-y+1/2,z;-x,y+1/2,z;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y+1/2,x+1/2,z;y+1/2,-x+1/2,z;-y+1/2,-x,-z;y+1/2,x,-z;-x+1/2,y,-z;x+1/2,-y,-z;-x+1/2,-y+1/2,-z;x+1/2,y+1/2,-z;y+1/2,-x+1/2,-z;-y+1/2,x+1/2,-z;y+1/2,x,z;-y+1/2,-x,z;x+1/2,-y,z;-x+1/2,y,z", // ITA 127.2
      "128;128.1;;16;d4h^6;p 4/m n c;-p 4 2n",  
      "128:c;128.2;a+b,-a+b,c;32;d4h^6;c 4/m c g2;xyz",//;x,y,z;-x,-y,z;-y,x,z;y,-x,z;y+1/2,x,-z+1/2;-y+1/2,-x,-z+1/2;x+1/2,-y,-z+1/2;-x+1/2,y,-z+1/2;-x,-y,-z;x,y,-z;y,-x,-z;-y,x,-z;-y+1/2,-x,z+1/2;y+1/2,x,z+1/2;-x+1/2,y,z+1/2;x+1/2,-y,z+1/2;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y+1/2,x+1/2,z;y+1/2,-x+1/2,z;y,x+1/2,-z+1/2;-y,-x+1/2,-z+1/2;x,-y+1/2,-z+1/2;-x,y+1/2,-z+1/2;-x+1/2,-y+1/2,-z;x+1/2,y+1/2,-z;y+1/2,-x+1/2,-z;-y+1/2,x+1/2,-z;-y,-x+1/2,z+1/2;y,x+1/2,z+1/2;-x,y+1/2,z+1/2;x,-y+1/2,z+1/2", // ITA 128.2
      "129:2;129.1;;16;d4h^7;p 4/n m m :2;-p 4a 2a",  
      "129:c2;129.2;ab;32;d4h^7;c 4/e m m :2;xyz",//;x,y,z;-x,-y+1/2,z;-y+1/4,x+1/4,z;y+3/4,-x+1/4,z;-y+3/4,-x+1/4,-z;y+1/4,x+1/4,-z;-x,y+1/2,-z;x,-y,-z;-x,-y,-z;x,y+1/2,-z;y+1/4,-x+1/4,-z;-y+3/4,x+1/4,-z;y+3/4,x+1/4,z;-y+1/4,-x+1/4,z;x,-y+1/2,z;-x,y,z;x+1/2,y+1/2,z;-x+1/2,-y,z;-y+3/4,x+3/4,z;y+1/4,-x+3/4,z;-y+1/4,-x+3/4,-z;y+3/4,x+3/4,-z;-x+1/2,y,-z;x+1/2,-y+1/2,-z;-x+1/2,-y+1/2,-z;x+1/2,y,-z;y+3/4,-x+3/4,-z;-y+1/4,x+3/4,-z;y+1/4,x+3/4,z;-y+3/4,-x+3/4,z;x+1/2,-y,z;-x+1/2,y+1/2,z", // ITA 129.2
      "129:1;129.3;a,b,c|-1/4,1/4,0;16;d4h^7;p 4/n m m :1;p 4ab 2ab -1ab",
      "129:c1;129.4;ab|-1/4,1/4,0;32;d4h^7;c 4/e m m :1;xyz",//;x,y,z;-x+1/2,-y+1/2,z;-y+1/2,x,z;y,-x+1/2,z;-y,-x+1/2,-z;y+1/2,x,-z;-x+1/2,y+1/2,-z;x,-y,-z;-x+1/2,-y,-z;x,y+1/2,-z;y+1/2,-x+1/2,-z;-y,x,-z;y,x,z;-y+1/2,-x+1/2,z;x,-y+1/2,z;-x+1/2,y,z;x+1/2,y+1/2,z;-x,-y,z;-y,x+1/2,z;y+1/2,-x,z;-y+1/2,-x,-z;y,x+1/2,-z;-x,y,-z;x+1/2,-y+1/2,-z;-x,-y+1/2,-z;x+1/2,y,-z;y,-x,-z;-y+1/2,x+1/2,-z;y+1/2,x+1/2,z;-y,-x,z;x+1/2,-y,z;-x,y+1/2,z", // ITA 129.4
      "130:2;130.1;;16;d4h^8;p 4/n c c :2;-p 4a 2ac",  
      "130:c2;130.2;ab;32;d4h^8;c 4/e c c :2;xyz",//;x,y,z;-x,-y+1/2,z;-y+1/4,x+1/4,z;y+3/4,-x+1/4,z;-y+3/4,-x+1/4,-z+1/2;y+1/4,x+1/4,-z+1/2;-x,y+1/2,-z+1/2;x,-y,-z+1/2;-x,-y,-z;x,y+1/2,-z;y+1/4,-x+1/4,-z;-y+3/4,x+1/4,-z;y+3/4,x+1/4,z+1/2;-y+1/4,-x+1/4,z+1/2;x,-y+1/2,z+1/2;-x,y,z+1/2;x+1/2,y+1/2,z;-x+1/2,-y,z;-y+3/4,x+3/4,z;y+1/4,-x+3/4,z;-y+1/4,-x+3/4,-z+1/2;y+3/4,x+3/4,-z+1/2;-x+1/2,y,-z+1/2;x+1/2,-y+1/2,-z+1/2;-x+1/2,-y+1/2,-z;x+1/2,y,-z;y+3/4,-x+3/4,-z;-y+1/4,x+3/4,-z;y+1/4,x+3/4,z+1/2;-y+3/4,-x+3/4,z+1/2;x+1/2,-y,z+1/2;-x+1/2,y+1/2,z+1/2", // ITA 130.2
      "130:1;130.3;a,b,c|-1/4,1/4,0;16;d4h^8;p 4/n c c :1;p 4ab 2n -1ab",
      "130:c1;130.4;ab|-1/4,1/4,0;32;d4h^8;c 4/e c c :1;xyz",//;x,y,z;-x+1/2,-y+1/2,z;-y+1/2,x,z;y,-x+1/2,z;-y,-x+1/2,-z+1/2;y+1/2,x,-z+1/2;-x+1/2,y+1/2,-z+1/2;x,-y,-z+1/2;-x+1/2,-y,-z;x,y+1/2,-z;y+1/2,-x+1/2,-z;-y,x,-z;y,x,z+1/2;-y+1/2,-x+1/2,z+1/2;x,-y+1/2,z+1/2;-x+1/2,y,z+1/2;x+1/2,y+1/2,z;-x,-y,z;-y,x+1/2,z;y+1/2,-x,z;-y+1/2,-x,-z+1/2;y,x+1/2,-z+1/2;-x,y,-z+1/2;x+1/2,-y+1/2,-z+1/2;-x,-y+1/2,-z;x+1/2,y,-z;y,-x,-z;-y+1/2,x+1/2,-z;y+1/2,x+1/2,z+1/2;-y,-x,z+1/2;x+1/2,-y,z+1/2;-x,y+1/2,z+1/2", // ITA 130.4
      "131;131.1;;16;d4h^9;p 42/m m c;-p 4c 2",  
      "131:c;131.2;ab;32;d4h^9;c 42/m c m;xyz",//;x,y,z;-x,-y,z;-y,x,z+1/2;y,-x,z+1/2;-y,-x,-z;y,x,-z;-x,y,-z+1/2;x,-y,-z+1/2;-x,-y,-z;x,y,-z;y,-x,-z+1/2;-y,x,-z+1/2;y,x,z;-y,-x,z;x,-y,z+1/2;-x,y,z+1/2;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y+1/2,x+1/2,z+1/2;y+1/2,-x+1/2,z+1/2;-y+1/2,-x+1/2,-z;y+1/2,x+1/2,-z;-x+1/2,y+1/2,-z+1/2;x+1/2,-y+1/2,-z+1/2;-x+1/2,-y+1/2,-z;x+1/2,y+1/2,-z;y+1/2,-x+1/2,-z+1/2;-y+1/2,x+1/2,-z+1/2;y+1/2,x+1/2,z;-y+1/2,-x+1/2,z;x+1/2,-y+1/2,z+1/2;-x+1/2,y+1/2,z+1/2", // ITA 131.2
      "132;132.1;;16;d4h^10;p 42/m c m;-p 4c 2c",  
      "132:c;132.2;ab;32;d4h^10;c 42/m m c;xyz",//;x,y,z;-x,-y,z;-y,x,z+1/2;y,-x,z+1/2;-y,-x,-z+1/2;y,x,-z+1/2;-x,y,-z;x,-y,-z;-x,-y,-z;x,y,-z;y,-x,-z+1/2;-y,x,-z+1/2;y,x,z+1/2;-y,-x,z+1/2;x,-y,z;-x,y,z;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y+1/2,x+1/2,z+1/2;y+1/2,-x+1/2,z+1/2;-y+1/2,-x+1/2,-z+1/2;y+1/2,x+1/2,-z+1/2;-x+1/2,y+1/2,-z;x+1/2,-y+1/2,-z;-x+1/2,-y+1/2,-z;x+1/2,y+1/2,-z;y+1/2,-x+1/2,-z+1/2;-y+1/2,x+1/2,-z+1/2;y+1/2,x+1/2,z+1/2;-y+1/2,-x+1/2,z+1/2;x+1/2,-y+1/2,z;-x+1/2,y+1/2,z", // ITA 132.2
      "133:2;133.1;;16;d4h^11;p 42/n b c :2;-p 4ac 2b",  
      "133:c1;133.2;ab;32;d4h^11;c 42/e c g1 :2;xyz",//;x,y,z;-x,-y+1/2,z;-y+1/4,x+1/4,z+1/2;y+3/4,-x+1/4,z+1/2;-y+1/4,-x+1/4,-z;y+3/4,x+1/4,-z;-x,y,-z+1/2;x,-y+1/2,-z+1/2;-x,-y,-z;x,y+1/2,-z;y+1/4,-x+1/4,-z+1/2;-y+3/4,x+1/4,-z+1/2;y+1/4,x+1/4,z;-y+3/4,-x+1/4,z;x,-y,z+1/2;-x,y+1/2,z+1/2;x+1/2,y+1/2,z;-x+1/2,-y,z;-y+3/4,x+3/4,z+1/2;y+1/4,-x+3/4,z+1/2;-y+3/4,-x+3/4,-z;y+1/4,x+3/4,-z;-x+1/2,y+1/2,-z+1/2;x+1/2,-y,-z+1/2;-x+1/2,-y+1/2,-z;x+1/2,y,-z;y+3/4,-x+3/4,-z+1/2;-y+1/4,x+3/4,-z+1/2;y+3/4,x+3/4,z;-y+1/4,-x+3/4,z;x+1/2,-y+1/2,z+1/2;-x+1/2,y,z+1/2", // ITA 133.2
      "133:1;133.3;a,b,c|-1/4,1/4,-1/4;16;d4h^11;p 42/n b c :1;p 4n 2c -1n",
      "133:c2;133.4;ab|-1/4,1/4,-1/4;32;d4h^11;c 42/e c g1 :1;xyz",//;x,y,z;-x+1/2,-y+1/2,z;-y+1/2,x,z+1/2;y,-x+1/2,z+1/2;-y+1/2,-x+1/2,-z+1/2;y,x,-z+1/2;-x+1/2,y,-z;x,-y+1/2,-z;-x+1/2,-y,-z+1/2;x,y+1/2,-z+1/2;y+1/2,-x+1/2,-z;-y,x,-z;y+1/2,x,z;-y,-x+1/2,z;x,-y,z+1/2;-x+1/2,y+1/2,z+1/2;x+1/2,y+1/2,z;-x,-y,z;-y,x+1/2,z+1/2;y+1/2,-x,z+1/2;-y,-x,-z+1/2;y+1/2,x+1/2,-z+1/2;-x,y+1/2,-z;x+1/2,-y,-z;-x,-y+1/2,-z+1/2;x+1/2,y,-z+1/2;y,-x,-z;-y+1/2,x+1/2,-z;y,x+1/2,z;-y+1/2,-x,z;x+1/2,-y+1/2,z+1/2;-x,y,z+1/2", // ITA 133.4
      "134:2;134.1;;16;d4h^12;p 42/n n m :2;-p 4ac 2bc",  
      "134:c2;134.2;ab;32;d4h^12;c 42/e m g2 :2;xyz",//;x,y,z;-x,-y+1/2,z;-y+1/4,x+1/4,z+1/2;y+3/4,-x+1/4,z+1/2;-y+1/4,-x+1/4,-z+1/2;y+3/4,x+1/4,-z+1/2;-x,y,-z;x,-y+1/2,-z;-x,-y,-z;x,y+1/2,-z;y+1/4,-x+1/4,-z+1/2;-y+3/4,x+1/4,-z+1/2;y+1/4,x+1/4,z+1/2;-y+3/4,-x+1/4,z+1/2;x,-y,z;-x,y+1/2,z;x+1/2,y+1/2,z;-x+1/2,-y,z;-y+3/4,x+3/4,z+1/2;y+1/4,-x+3/4,z+1/2;-y+3/4,-x+3/4,-z+1/2;y+1/4,x+3/4,-z+1/2;-x+1/2,y+1/2,-z;x+1/2,-y,-z;-x+1/2,-y+1/2,-z;x+1/2,y,-z;y+3/4,-x+3/4,-z+1/2;-y+1/4,x+3/4,-z+1/2;y+3/4,x+3/4,z+1/2;-y+1/4,-x+3/4,z+1/2;x+1/2,-y+1/2,z;-x+1/2,y,z", // ITA 134.2
      "134:1;134.3;a,b,c|-1/4,1/4,-1/4;16;d4h^12;p 42/n n m :1;p 4n 2 -1n",
      "134:c1;134.4;ab|-1/4,1/4,-1/4;32;d4h^12;c 42/e m g2 :1;xyz",//;x,y,z;-x+1/2,-y+1/2,z;-y+1/2,x,z+1/2;y,-x+1/2,z+1/2;-y+1/2,-x+1/2,-z;y,x,-z;-x+1/2,y,-z+1/2;x,-y+1/2,-z+1/2;-x+1/2,-y,-z+1/2;x,y+1/2,-z+1/2;y+1/2,-x+1/2,-z;-y,x,-z;y+1/2,x,z+1/2;-y,-x+1/2,z+1/2;x,-y,z;-x+1/2,y+1/2,z;x+1/2,y+1/2,z;-x,-y,z;-y,x+1/2,z+1/2;y+1/2,-x,z+1/2;-y,-x,-z;y+1/2,x+1/2,-z;-x,y+1/2,-z+1/2;x+1/2,-y,-z+1/2;-x,-y+1/2,-z+1/2;x+1/2,y,-z+1/2;y,-x,-z;-y+1/2,x+1/2,-z;y,x+1/2,z+1/2;-y+1/2,-x,z+1/2;x+1/2,-y+1/2,z;-x,y,z", // ITA 134.4
      "135;135.1;;16;d4h^13;p 42/m b c;-p 4c 2ab",  
      "135*;135.1;;16;d4h^13;p 42/m b c*;-p 42 2ab",   //nonstandard
      "135:c;135.2;ab;32;d4h^13;c 42/m c g1;xyz",//;x,y,z;-x,-y,z;-y,x,z+1/2;y,-x,z+1/2;-y,-x+1/2,-z;y,x+1/2,-z;-x,y+1/2,-z+1/2;x,-y+1/2,-z+1/2;-x,-y,-z;x,y,-z;y,-x,-z+1/2;-y,x,-z+1/2;y,x+1/2,z;-y,-x+1/2,z;x,-y+1/2,z+1/2;-x,y+1/2,z+1/2;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y+1/2,x+1/2,z+1/2;y+1/2,-x+1/2,z+1/2;-y+1/2,-x,-z;y+1/2,x,-z;-x+1/2,y,-z+1/2;x+1/2,-y,-z+1/2;-x+1/2,-y+1/2,-z;x+1/2,y+1/2,-z;y+1/2,-x+1/2,-z+1/2;-y+1/2,x+1/2,-z+1/2;y+1/2,x,z;-y+1/2,-x,z;x+1/2,-y,z+1/2;-x+1/2,y,z+1/2", // ITA 135.2
      "136;136.1;;16;d4h^14;p 42/m n m;-p 4n 2n",  
      "136:c;136.2;ab;32;d4h^14;c 42/m m g2;xyz",//;x,y,z;-x,-y,z;-y,x+1/2,z+1/2;y,-x+1/2,z+1/2;-y,-x+1/2,-z+1/2;y,x+1/2,-z+1/2;-x,y,-z;x,-y,-z;-x,-y,-z;x,y,-z;y,-x+1/2,-z+1/2;-y,x+1/2,-z+1/2;y,x+1/2,z+1/2;-y,-x+1/2,z+1/2;x,-y,z;-x,y,z;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y+1/2,x,z+1/2;y+1/2,-x,z+1/2;-y+1/2,-x,-z+1/2;y+1/2,x,-z+1/2;-x+1/2,y+1/2,-z;x+1/2,-y+1/2,-z;-x+1/2,-y+1/2,-z;x+1/2,y+1/2,-z;y+1/2,-x,-z+1/2;-y+1/2,x,-z+1/2;y+1/2,x,z+1/2;-y+1/2,-x,z+1/2;x+1/2,-y+1/2,z;-x+1/2,y+1/2,z", // ITA 136.2
      "137:2;137.1;;16;d4h^15;p 42/n m c :2;-p 4ac 2a",  
      "137:c1;137.2;ab;32;d4h^15;c 42/e c m :2;xyz",//;x,y,z;-x,-y+1/2,z;-y+1/4,x+1/4,z+1/2;y+3/4,-x+1/4,z+1/2;-y+3/4,-x+1/4,-z;y+1/4,x+1/4,-z;-x,y+1/2,-z+1/2;x,-y,-z+1/2;-x,-y,-z;x,y+1/2,-z;y+1/4,-x+1/4,-z+1/2;-y+3/4,x+1/4,-z+1/2;y+3/4,x+1/4,z;-y+1/4,-x+1/4,z;x,-y+1/2,z+1/2;-x,y,z+1/2;x+1/2,y+1/2,z;-x+1/2,-y,z;-y+3/4,x+3/4,z+1/2;y+1/4,-x+3/4,z+1/2;-y+1/4,-x+3/4,-z;y+3/4,x+3/4,-z;-x+1/2,y,-z+1/2;x+1/2,-y+1/2,-z+1/2;-x+1/2,-y+1/2,-z;x+1/2,y,-z;y+3/4,-x+3/4,-z+1/2;-y+1/4,x+3/4,-z+1/2;y+1/4,x+3/4,z;-y+3/4,-x+3/4,z;x+1/2,-y,z+1/2;-x+1/2,y+1/2,z+1/2", // ITA 137.2
      "137:1;137.3;a,b,c|-1/4,1/4,-1/4;16;d4h^15;p 42/n m c :1;p 4n 2n -1n",
      "137:c2;137.4;ab|-1/4,1/4,-1/4;32;d4h^15;c 42/e c m :1;xyz",//;x,y,z;-x+1/2,-y+1/2,z;-y+1/2,x,z+1/2;y,-x+1/2,z+1/2;-y,-x+1/2,-z+1/2;y+1/2,x,-z+1/2;-x+1/2,y+1/2,-z;x,-y,-z;-x+1/2,-y,-z+1/2;x,y+1/2,-z+1/2;y+1/2,-x+1/2,-z;-y,x,-z;y,x,z;-y+1/2,-x+1/2,z;x,-y+1/2,z+1/2;-x+1/2,y,z+1/2;x+1/2,y+1/2,z;-x,-y,z;-y,x+1/2,z+1/2;y+1/2,-x,z+1/2;-y+1/2,-x,-z+1/2;y,x+1/2,-z+1/2;-x,y,-z;x+1/2,-y+1/2,-z;-x,-y+1/2,-z+1/2;x+1/2,y,-z+1/2;y,-x,-z;-y+1/2,x+1/2,-z;y+1/2,x+1/2,z;-y,-x,z;x+1/2,-y,z+1/2;-x,y+1/2,z+1/2", // ITA 137.4
      "138:2;138.1;;16;d4h^16;p 42/n c m :2;-p 4ac 2ac",  
      "138:c2;138.2;ab;32;d4h^16;c 42/e m c :2;xyz",//;x,y,z;-x,-y+1/2,z;-y+1/4,x+1/4,z+1/2;y+3/4,-x+1/4,z+1/2;-y+3/4,-x+1/4,-z+1/2;y+1/4,x+1/4,-z+1/2;-x,y+1/2,-z;x,-y,-z;-x,-y,-z;x,y+1/2,-z;y+1/4,-x+1/4,-z+1/2;-y+3/4,x+1/4,-z+1/2;y+3/4,x+1/4,z+1/2;-y+1/4,-x+1/4,z+1/2;x,-y+1/2,z;-x,y,z;x+1/2,y+1/2,z;-x+1/2,-y,z;-y+3/4,x+3/4,z+1/2;y+1/4,-x+3/4,z+1/2;-y+1/4,-x+3/4,-z+1/2;y+3/4,x+3/4,-z+1/2;-x+1/2,y,-z;x+1/2,-y+1/2,-z;-x+1/2,-y+1/2,-z;x+1/2,y,-z;y+3/4,-x+3/4,-z+1/2;-y+1/4,x+3/4,-z+1/2;y+1/4,x+3/4,z+1/2;-y+3/4,-x+3/4,z+1/2;x+1/2,-y,z;-x+1/2,y+1/2,z", // ITA 138.2
      "138:1;138.3;a,b,c|-1/4,1/4,-1/4;16;d4h^16;p 42/n c m :1;p 4n 2ab -1n",
      "138:c1;138.4;ab|-1/4,1/4,-1/4;32;d4h^16;c 42/e m c :1;xyz",//;x,y,z;-x+1/2,-y+1/2,z;-y+1/2,x,z+1/2;y,-x+1/2,z+1/2;-y,-x+1/2,-z;y+1/2,x,-z;-x+1/2,y+1/2,-z+1/2;x,-y,-z+1/2;-x+1/2,-y,-z+1/2;x,y+1/2,-z+1/2;y+1/2,-x+1/2,-z;-y,x,-z;y,x,z+1/2;-y+1/2,-x+1/2,z+1/2;x,-y+1/2,z;-x+1/2,y,z;x+1/2,y+1/2,z;-x,-y,z;-y,x+1/2,z+1/2;y+1/2,-x,z+1/2;-y+1/2,-x,-z;y,x+1/2,-z;-x,y,-z+1/2;x+1/2,-y+1/2,-z+1/2;-x,-y+1/2,-z+1/2;x+1/2,y,-z+1/2;y,-x,-z;-y+1/2,x+1/2,-z;y+1/2,x+1/2,z+1/2;-y,-x,z+1/2;x+1/2,-y,z;-x,y+1/2,z", // ITA 138.4
      "139;139.1;;32;d4h^17;i 4/m m m;-i 4 2",  
      "139:f;139.2;ab;64;d4h^17;f 4/m m m;xyz",//;x,y,z;-x,-y,z;-y,x,z;y,-x,z;-y,-x,-z;y,x,-z;-x,y,-z;x,-y,-z;-x,-y,-z;x,y,-z;y,-x,-z;-y,x,-z;y,x,z;-y,-x,z;x,-y,z;-x,y,z;x,y+1/2,z+1/2;-x,-y+1/2,z+1/2;-y,x+1/2,z+1/2;y,-x+1/2,z+1/2;-y,-x+1/2,-z+1/2;y,x+1/2,-z+1/2;-x,y+1/2,-z+1/2;x,-y+1/2,-z+1/2;-x,-y+1/2,-z+1/2;x,y+1/2,-z+1/2;y,-x+1/2,-z+1/2;-y,x+1/2,-z+1/2;y,x+1/2,z+1/2;-y,-x+1/2,z+1/2;x,-y+1/2,z+1/2;-x,y+1/2,z+1/2;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y+1/2,x+1/2,z;y+1/2,-x+1/2,z;-y+1/2,-x+1/2,-z;y+1/2,x+1/2,-z;-x+1/2,y+1/2,-z;x+1/2,-y+1/2,-z;-x+1/2,-y+1/2,-z;x+1/2,y+1/2,-z;y+1/2,-x+1/2,-z;-y+1/2,x+1/2,-z;y+1/2,x+1/2,z;-y+1/2,-x+1/2,z;x+1/2,-y+1/2,z;-x+1/2,y+1/2,z;x+1/2,y,z+1/2;-x+1/2,-y,z+1/2;-y+1/2,x,z+1/2;y+1/2,-x,z+1/2;-y+1/2,-x,-z+1/2;y+1/2,x,-z+1/2;-x+1/2,y,-z+1/2;x+1/2,-y,-z+1/2;-x+1/2,-y,-z+1/2;x+1/2,y,-z+1/2;y+1/2,-x,-z+1/2;-y+1/2,x,-z+1/2;y+1/2,x,z+1/2;-y+1/2,-x,z+1/2;x+1/2,-y,z+1/2;-x+1/2,y,z+1/2", // ITA 139.2
      "140;140.1;;32;d4h^18;i 4/m c m;-i 4 2c",  
      "140:f;140.2;ab;64;d4h^18;f 4/m m c;xyz",//;x,y,z;-x,-y,z;-y,x,z;y,-x,z;-y,-x,-z+1/2;y,x,-z+1/2;-x,y,-z+1/2;x,-y,-z+1/2;-x,-y,-z;x,y,-z;y,-x,-z;-y,x,-z;y,x,z+1/2;-y,-x,z+1/2;x,-y,z+1/2;-x,y,z+1/2;x,y+1/2,z+1/2;-x,-y+1/2,z+1/2;-y,x+1/2,z+1/2;y,-x+1/2,z+1/2;-y,-x+1/2,-z;y,x+1/2,-z;-x,y+1/2,-z;x,-y+1/2,-z;-x,-y+1/2,-z+1/2;x,y+1/2,-z+1/2;y,-x+1/2,-z+1/2;-y,x+1/2,-z+1/2;y,x+1/2,z;-y,-x+1/2,z;x,-y+1/2,z;-x,y+1/2,z;x+1/2,y+1/2,z;-x+1/2,-y+1/2,z;-y+1/2,x+1/2,z;y+1/2,-x+1/2,z;-y+1/2,-x+1/2,-z+1/2;y+1/2,x+1/2,-z+1/2;-x+1/2,y+1/2,-z+1/2;x+1/2,-y+1/2,-z+1/2;-x+1/2,-y+1/2,-z;x+1/2,y+1/2,-z;y+1/2,-x+1/2,-z;-y+1/2,x+1/2,-z;y+1/2,x+1/2,z+1/2;-y+1/2,-x+1/2,z+1/2;x+1/2,-y+1/2,z+1/2;-x+1/2,y+1/2,z+1/2;x+1/2,y,z+1/2;-x+1/2,-y,z+1/2;-y+1/2,x,z+1/2;y+1/2,-x,z+1/2;-y+1/2,-x,-z;y+1/2,x,-z;-x+1/2,y,-z;x+1/2,-y,-z;-x+1/2,-y,-z+1/2;x+1/2,y,-z+1/2;y+1/2,-x,-z+1/2;-y+1/2,x,-z+1/2;y+1/2,x,z;-y+1/2,-x,z;x+1/2,-y,z;-x+1/2,y,z", // ITA 140.2
      "141:2;141.1;;32;d4h^19;i 41/a m d :2;-i 4bd 2",  
      "141:f2;141.2;ab;64;d4h^19;f 41/d d m :2;xyz",//;x,y,z;-x+1/4,-y+1/4,z+1/2;-y+3/4,x+1/2,z+1/4;y,-x+1/4,z+3/4;-y+1/4,-x+1/4,-z+1/2;y,x,-z;-x+3/4,y+1/2,-z+1/4;x,-y+1/4,-z+3/4;-x,-y,-z;x+1/4,y+1/4,-z+1/2;y+1/4,-x+1/2,-z+3/4;-y,x+3/4,-z+1/4;y+1/4,x+1/4,z+1/2;-y,-x,z;x+1/4,-y+1/2,z+3/4;-x,y+3/4,z+1/4;x,y+1/2,z+1/2;-x+3/4,-y+1/4,z;-y+1/4,x+1/2,z+3/4;y,-x+3/4,z+1/4;-y+3/4,-x+1/4,-z;y,x+1/2,-z+1/2;-x+1/4,y+1/2,-z+3/4;x,-y+3/4,-z+1/4;-x,-y+1/2,-z+1/2;x+3/4,y+1/4,-z;y+3/4,-x+1/2,-z+1/4;-y,x+1/4,-z+3/4;y+3/4,x+1/4,z;-y,-x+1/2,z+1/2;x+3/4,-y+1/2,z+1/4;-x,y+1/4,z+3/4;x+1/2,y+1/2,z;-x+3/4,-y+3/4,z+1/2;-y+1/4,x,z+1/4;y+1/2,-x+3/4,z+3/4;-y+3/4,-x+3/4,-z+1/2;y+1/2,x+1/2,-z;-x+1/4,y,-z+1/4;x+1/2,-y+3/4,-z+3/4;-x+1/2,-y+1/2,-z;x+3/4,y+3/4,-z+1/2;y+3/4,-x,-z+3/4;-y+1/2,x+5/4,-z+1/4;y+3/4,x+3/4,z+1/2;-y+1/2,-x+1/2,z;x+3/4,-y,z+3/4;-x+1/2,y+5/4,z+1/4;x+1/2,y,z+1/2;-x+1/4,-y+3/4,z;-y+3/4,x,z+3/4;y+1/2,-x+5/4,z+1/4;-y+1/4,-x+3/4,-z;y+1/2,x,-z+1/2;-x+3/4,y,-z+3/4;x+1/2,-y+5/4,-z+1/4;-x+1/2,-y,-z+1/2;x+1/4,y+3/4,-z;y+1/4,-x,-z+1/4;-y+1/2,x+3/4,-z+3/4;y+1/4,x+3/4,z;-y+1/2,-x,z+1/2;x+1/4,-y,z+1/4;-x+1/2,y+3/4,z+3/4", // ITA 141.2
      "141:1;141.3;a,b,c|0,1/4,-1/8;32;d4h^19;i 41/a m d :1;i 4bw 2bw -1bw",
      "141:f1;141.4;ab|0,1/4,-1/8;64;d4h^19;f 41/d d m :1;xyz",//;x,y,z;-x+1/2,-y,z+1/2;-y+3/4,x+1/4,z+1/4;y+1/4,-x+1/4,z+3/4;-y+1/4,-x+1/4,-z+3/4;y+1/4,x+3/4,-z+1/4;-x,y+1/2,-z+1/2;x,-y,-z;-x+1/4,-y+3/4,-z+1/4;x+1/4,y+1/4,-z+3/4;y+1/2,-x+1/2,-z;-y,x+1/2,-z+1/2;y+1/2,x,z+1/2;-y,-x,z;x+1/4,-y+1/4,z+3/4;-x+1/4,y+3/4,z+1/4;x,y+1/2,z+1/2;-x,-y,z;-y+1/4,x+1/4,z+3/4;y+1/4,-x+3/4,z+1/4;-y+3/4,-x+1/4,-z+1/4;y+1/4,x+1/4,-z+3/4;-x+1/2,y+1/2,-z;x,-y+1/2,-z+1/2;-x+1/4,-y+1/4,-z+3/4;x+3/4,y+1/4,-z+1/4;y,-x+1/2,-z+1/2;-y,x,-z;y,x,z;-y,-x+1/2,z+1/2;x+3/4,-y+1/4,z+1/4;-x+1/4,y+1/4,z+3/4;x+1/2,y+1/2,z;-x,-y+1/2,z+1/2;-y+1/4,x+3/4,z+1/4;y+3/4,-x+3/4,z+3/4;-y+3/4,-x+3/4,-z+3/4;y+3/4,x+1/4,-z+1/4;-x+1/2,y,-z+1/2;x+1/2,-y+1/2,-z;-x+3/4,-y+1/4,-z+1/4;x+3/4,y+3/4,-z+3/4;y,-x,-z;-y+1/2,x,-z+1/2;y,x+1/2,z+1/2;-y+1/2,-x+1/2,z;x+3/4,-y+3/4,z+3/4;-x+3/4,y+5/4,z+1/4;x+1/2,y,z+1/2;-x+1/2,-y+1/2,z;-y+3/4,x+3/4,z+3/4;y+3/4,-x+5/4,z+1/4;-y+1/4,-x+3/4,-z+1/4;y+3/4,x+3/4,-z+3/4;-x,y,-z;x+1/2,-y,-z+1/2;-x+3/4,-y+3/4,-z+3/4;x+1/4,y+3/4,-z+1/4;y+1/2,-x,-z+1/2;-y+1/2,x+1/2,-z;y+1/2,x+1/2,z;-y+1/2,-x,z+1/2;x+1/4,-y+3/4,z+1/4;-x+3/4,y+3/4,z+3/4", // ITA 141.4
      "142:2;142.1;;32;d4h^20;i 41/a c d :2;-i 4bd 2c",  
      "142:f2;142.2;ab;64;d4h^20;f 41/d d c :2;xyz",//;x,y,z;-x+1/4,-y+1/4,z+1/2;-y+3/4,x+1/2,z+1/4;y,-x+1/4,z+3/4;-y+1/4,-x+1/4,-z;y,x,-z+1/2;-x+3/4,y+1/2,-z+3/4;x,-y+1/4,-z+1/4;-x,-y,-z;x+1/4,y+1/4,-z+1/2;y+1/4,-x+1/2,-z+3/4;-y,x+3/4,-z+1/4;y+1/4,x+1/4,z;-y,-x,z+1/2;x+1/4,-y+1/2,z+1/4;-x,y+3/4,z+3/4;x,y+1/2,z+1/2;-x+3/4,-y+1/4,z;-y+1/4,x+1/2,z+3/4;y,-x+3/4,z+1/4;-y+3/4,-x+1/4,-z+1/2;y,x+1/2,-z;-x+1/4,y+1/2,-z+1/4;x,-y+3/4,-z+3/4;-x,-y+1/2,-z+1/2;x+3/4,y+1/4,-z;y+3/4,-x+1/2,-z+1/4;-y,x+1/4,-z+3/4;y+3/4,x+1/4,z+1/2;-y,-x+1/2,z;x+3/4,-y+1/2,z+3/4;-x,y+1/4,z+1/4;x+1/2,y+1/2,z;-x+3/4,-y+3/4,z+1/2;-y+1/4,x,z+1/4;y+1/2,-x+3/4,z+3/4;-y+3/4,-x+3/4,-z;y+1/2,x+1/2,-z+1/2;-x+1/4,y,-z+3/4;x+1/2,-y+3/4,-z+1/4;-x+1/2,-y+1/2,-z;x+3/4,y+3/4,-z+1/2;y+3/4,-x,-z+3/4;-y+1/2,x+5/4,-z+1/4;y+3/4,x+3/4,z;-y+1/2,-x+1/2,z+1/2;x+3/4,-y,z+1/4;-x+1/2,y+5/4,z+3/4;x+1/2,y,z+1/2;-x+1/4,-y+3/4,z;-y+3/4,x,z+3/4;y+1/2,-x+5/4,z+1/4;-y+1/4,-x+3/4,-z+1/2;y+1/2,x,-z;-x+3/4,y,-z+1/4;x+1/2,-y+5/4,-z+3/4;-x+1/2,-y,-z+1/2;x+1/4,y+3/4,-z;y+1/4,-x,-z+1/4;-y+1/2,x+3/4,-z+3/4;y+1/4,x+3/4,z+1/2;-y+1/2,-x,z;x+1/4,-y,z+3/4;-x+1/2,y+3/4,z+1/4", // ITA 142.2
      "142:1;142.3;a,b,c|0,1/4,-1/8;32;d4h^20;i 41/a c d :1;i 4bw 2aw -1bw",
      "142:f1;142.4;ab|0,1/4,-1/8;64;d4h^20;f 41/d d c :1;xyz",//;x,y,z;-x+1/2,-y,z+1/2;-y+3/4,x+1/4,z+1/4;y+1/4,-x+1/4,z+3/4;-y+1/4,-x+1/4,-z+1/4;y+1/4,x+3/4,-z+3/4;-x,y+1/2,-z;x,-y,-z+1/2;-x+1/4,-y+3/4,-z+1/4;x+1/4,y+1/4,-z+3/4;y+1/2,-x+1/2,-z;-y,x+1/2,-z+1/2;y+1/2,x,z;-y,-x,z+1/2;x+1/4,-y+1/4,z+1/4;-x+1/4,y+3/4,z+3/4;x,y+1/2,z+1/2;-x,-y,z;-y+1/4,x+1/4,z+3/4;y+1/4,-x+3/4,z+1/4;-y+3/4,-x+1/4,-z+3/4;y+1/4,x+1/4,-z+1/4;-x+1/2,y+1/2,-z+1/2;x,-y+1/2,-z;-x+1/4,-y+1/4,-z+3/4;x+3/4,y+1/4,-z+1/4;y,-x+1/2,-z+1/2;-y,x,-z;y,x,z+1/2;-y,-x+1/2,z;x+3/4,-y+1/4,z+3/4;-x+1/4,y+1/4,z+1/4;x+1/2,y+1/2,z;-x,-y+1/2,z+1/2;-y+1/4,x+3/4,z+1/4;y+3/4,-x+3/4,z+3/4;-y+3/4,-x+3/4,-z+1/4;y+3/4,x+1/4,-z+3/4;-x+1/2,y,-z;x+1/2,-y+1/2,-z+1/2;-x+3/4,-y+1/4,-z+1/4;x+3/4,y+3/4,-z+3/4;y,-x,-z;-y+1/2,x,-z+1/2;y,x+1/2,z;-y+1/2,-x+1/2,z+1/2;x+3/4,-y+3/4,z+1/4;-x+3/4,y+5/4,z+3/4;x+1/2,y,z+1/2;-x+1/2,-y+1/2,z;-y+3/4,x+3/4,z+3/4;y+3/4,-x+5/4,z+1/4;-y+1/4,-x+3/4,-z+3/4;y+3/4,x+3/4,-z+1/4;-x,y,-z+1/2;x+1/2,-y,-z;-x+3/4,-y+3/4,-z+3/4;x+1/4,y+3/4,-z+1/4;y+1/2,-x,-z+1/2;-y+1/2,x+1/2,-z;y+1/2,x+1/2,z+1/2;-y+1/2,-x,z;x+1/4,-y+3/4,z+3/4;-x+3/4,y+3/4,z+1/4", // ITA 142.4
      "143;143.1;;3;c3^1;p 3;p 3",  
      "144;144.1;;3;c3^2;p 31;p 31",  
      "145;145.1;;3;c3^3;p 32;p 32",  
      "146:h;146.1;;9;c3^4;r 3 :h;r 3",  
      "146:r;146.2;r;3;c3^4;r 3 :r;p 3*",  
      "147;147.1;;6;c3i^1;p -3;-p 3",  
      "148:h;148.1;;18;c3i^2;r -3 :h;-r 3",  
      "148:r;148.2;r;6;c3i^2;r -3 :r;-p 3*",  
      "149;149.1;;6;d3^1;p 3 1 2;p 3 2",  
      "150;150.1;;6;d3^2;p 3 2 1;p 3 2\"",  
      "151;151.1;;6;d3^3;p 31 1 2;p 31 2 (0 0 4)",  
      "152;152.1;;6;d3^4;p 31 2 1;p 31 2\"",  
      "152:_2;152:a,b,c|0,0,-1/3;a,b,c|0,0,-1/3;6;d3^4;p 31 2 1;p 31 2\" (0 0 -4)", //  NOTE: MSA quartz.cif gives different operators for this -- 
      "153;153.1;;6;d3^5;p 32 1 2;p 32 2 (0 0 2)",  
      "154;154.1;;6;d3^6;p 32 2 1;p 32 2\"",    
      "154:_2;154:a,b,c|0,0,-1/3;a,b,c|0,0,-1/3;6;d3^6;p 32 2 1;p 32 2\" (0 0 4)",  //  NOTE: MSA quartz.cif gives different operators for this -- 
      "155:h;155.1;;18;d3^7;r 3 2 :h;r 3 2\"",  
      "155:r;155.2;r;6;d3^7;r 3 2 :r;p 3* 2",  
      "156;156.1;;6;c3v^1;p 3 m 1;p 3 -2\"",  
      "157;157.1;;6;c3v^2;p 3 1 m;p 3 -2",  
      "158;158.1;;6;c3v^3;p 3 c 1;p 3 -2\"c",  
      "159;159.1;;6;c3v^4;p 3 1 c;p 3 -2c",  
      "160:h;160.1;;18;c3v^5;r 3 m :h;r 3 -2\"",  
      "160:r;160.2;r;6;c3v^5;r 3 m :r;p 3* -2",  
      "161:h;161.1;;18;c3v^6;r 3 c :h;r 3 -2\"c",  
      "161:r;161.2;r;6;c3v^6;r 3 c :r;p 3* -2n",  
      "162;162.1;;12;d3d^1;p -3 1 m;-p 3 2",  
      "163;163.1;;12;d3d^2;p -3 1 c;-p 3 2c",  
      "164;164.1;;12;d3d^3;p -3 m 1;-p 3 2\"",  
      "165;165.1;;12;d3d^4;p -3 c 1;-p 3 2\"c",  
      "166:h;166.1;;36;d3d^5;r -3 m :h;-r 3 2\"",  
      "166:r;166.2;r;12;d3d^5;r -3 m :r;-p 3* 2",  
      "167:h;167.1;;36;d3d^6;r -3 c :h;-r 3 2\"c",  
      "167:r;167.2;r;12;d3d^6;r -3 c :r;-p 3* 2n",  
      "168;168.1;;6;c6^1;p 6;p 6",  
      "169;169.1;;6;c6^2;p 61;p 61",  
      "170;170.1;;6;c6^3;p 65;p 65",  
      "171;171.1;;6;c6^4;p 62;p 62",  
      "172;172.1;;6;c6^5;p 64;p 64",  
      "173;173.1;;6;c6^6;p 63;p 6c",  
      "173*;173.1;;6;c6^6;p 63*;p 63 ",   //nonstandard",// space added so not identical to H-M P 63
      "174;174.1;;6;c3h^1;p -6;p -6",  
      "175;175.1;;12;c6h^1;p 6/m;-p 6",  
      "176;176.1;;12;c6h^2;p 63/m;-p 6c",  
      "176*;176.1;;12;c6h^2;p 63/m*;-p 63",   //nonstandard
      "177;177.1;;12;d6^1;p 6 2 2;p 6 2",  
      "178;178.1;;12;d6^2;p 61 2 2;p 61 2 (0 0 5)",  
      "179;179.1;;12;d6^3;p 65 2 2;p 65 2 (0 0 1)",  
      "180;180.1;;12;d6^4;p 62 2 2;p 62 2 (0 0 4)",  
      "181;181.1;;12;d6^5;p 64 2 2;p 64 2 (0 0 2)",  
      "182;182.1;;12;d6^6;p 63 2 2;p 6c 2c",  
      "182*;182.1;;12;d6^6;p 63 2 2*;p 63 2c",   //nonstandard
      "183;183.1;;12;c6v^1;p 6 m m;p 6 -2",  
      "184;184.1;;12;c6v^2;p 6 c c;p 6 -2c",  
      "185;185.1;;12;c6v^3;p 63 c m;p 6c -2",  
      "185*;185.1;;12;c6v^3;p 63 c m*;p 63 -2",   //nonstandard
      "186;186.1;;12;c6v^4;p 63 m c;p 6c -2c",  
      "186*;186.1;;12;c6v^4;p 63 m c*;p 63 -2c",   //nonstandard
      "187;187.1;;12;d3h^1;p -6 m 2;p -6 2",  
      "188;188.1;;12;d3h^2;p -6 c 2;p -6c 2",  
      "189;189.1;;12;d3h^3;p -6 2 m;p -6 -2",  
      "190;190.1;;12;d3h^4;p -6 2 c;p -6c -2c",  
      "191;191.1;;24;d6h^1;p 6/m m m;-p 6 2",  
      "192;192.1;;24;d6h^2;p 6/m c c;-p 6 2c",  
      "193;193.1;;24;d6h^3;p 63/m c m;-p 6c 2",  
      "193*;193.1;;24;d6h^3;p 63/m c m*;-p 63 2",   //nonstandard
      "194;194.1;;24;d6h^4;p 63/m m c;-p 6c 2c",  
      "194*;194.1;;24;d6h^4;p 63/m m c*;-p 63 2c",   //nonstandard
      "195;195.1;;12;t^1;p 2 3;p 2 2 3",  
      "196;196.1;;48;t^2;f 2 3;f 2 2 3",  
      "197;197.1;;24;t^3;i 2 3;i 2 2 3",  
      "198;198.1;;12;t^4;p 21 3;p 2ac 2ab 3",  
      "199;199.1;;24;t^5;i 21 3;i 2b 2c 3",  
      "200;200.1;;24;th^1;p m -3;-p 2 2 3",  
      "201:2;201.1;;24;th^2;p n -3 :2;-p 2ab 2bc 3",  
      "201:1;201.2;a,b,c|-1/4,-1/4,-1/4;24;th^2;p n -3 :1;p 2 2 3 -1n",
      "202;202.1;;96;th^3;f m -3;-f 2 2 3",  
      "203:2;203.1;;96;th^4;f d -3 :2;-f 2uv 2vw 3",  
      "203:1;203.2;a,b,c|-1/8,-1/8,-1/8;96;th^4;f d -3 :1;f 2 2 3 -1d",
      "204;204.1;;48;th^5;i m -3;-i 2 2 3",  
      "205;205.1;;24;th^6;p a -3;-p 2ac 2ab 3",  
      "206;206.1;;48;th^7;i a -3;-i 2b 2c 3",  
      "207;207.1;;24;o^1;p 4 3 2;p 4 2 3",  
      "208;208.1;;24;o^2;p 42 3 2;p 4n 2 3",  
      "209;209.1;;96;o^3;f 4 3 2;f 4 2 3",  
      "210;210.1;;96;o^4;f 41 3 2;f 4d 2 3",  
      "211;211.1;;48;o^5;i 4 3 2;i 4 2 3",  
      "212;212.1;;24;o^6;p 43 3 2;p 4acd 2ab 3",  
      "213;213.1;;24;o^7;p 41 3 2;p 4bd 2ab 3",  
      "214;214.1;;48;o^8;i 41 3 2;i 4bd 2c 3",  
      "215;215.1;;24;td^1;p -4 3 m;p -4 2 3",  
      "216;216.1;;96;td^2;f -4 3 m;f -4 2 3",  
      "217;217.1;;48;td^3;i -4 3 m;i -4 2 3",  
      "218;218.1;;24;td^4;p -4 3 n;p -4n 2 3",  
      "219;219.1;;96;td^5;f -4 3 c;f -4a 2 3",  
      "220;220.1;;48;td^6;i -4 3 d;i -4bd 2c 3",  
      "221;221.1;;48;oh^1;p m -3 m;-p 4 2 3",  
      "222:2;222.1;;48;oh^2;p n -3 n :2;-p 4a 2bc 3",  
      "222:1;222.2;a,b,c|-1/4,-1/4,-1/4;48;oh^2;p n -3 n :1;p 4 2 3 -1n",
      "223;223.1;;48;oh^3;p m -3 n;-p 4n 2 3",  
      "224:2;224.1;;48;oh^4;p n -3 m :2;-p 4bc 2bc 3",  
      "224:1;224.2;a,b,c|-1/4,-1/4,-1/4;48;oh^4;p n -3 m :1;p 4n 2 3 -1n",
      "225;225.1;;192;oh^5;f m -3 m;-f 4 2 3",  
      "226;226.1;;192;oh^6;f m -3 c;-f 4a 2 3",  
      "227:2;227.1;;192;oh^7;f d -3 m :2;-f 4vw 2vw 3",  
      "227:1;227.2;a,b,c|-1/8,-1/8,-1/8;192;oh^7;f d -3 m :1;f 4d 2 3 -1d",
      "228:2;228.1;;192;oh^8;f d -3 c :2;-f 4ud 2vw 3",  
      "228:1;228.2;a,b,c|-3/8,-3/8,-3/8;192;oh^8;f d -3 c :1;f 4d 2 3 -1ad",
      "229;229.1;;96;oh^9;i m -3 m;-i 4 2 3",  
      "230;230.1;;96;oh^10;i a -3 d;-i 4bd 2c 3",        
  };
  
  /**
   * 
   * @param lattvecs
   *        could be magnetic centering, in which case there is an additional
   *        lattice parameter that is time reversal
   * @return true if successful
   */
  public boolean addLatticeVectors(Lst<double[]> lattvecs) {
    if (latticeOp >= 0 || lattvecs.size() == 0)
      return false;
    int nOps = latticeOp = operationCount;
    boolean isMagnetic = (lattvecs.get(0).length == modDim + 4);
    int magRev = -2;
    for (int j = 0; j < lattvecs.size(); j++) {
      double[] data = lattvecs.get(j);
      if (isMagnetic) {
        magRev = (int) data[modDim + 3];
        data = AU.arrayCopyD(data, modDim + 3);
      }
      if (data.length > modDim + 3)
        return false;
      for (int i = 0; i < nOps; i++) {
        SymmetryOperation newOp = new SymmetryOperation(null, 0, true); // must normalize these
        newOp.modDim = modDim;
        SymmetryOperation op = operations[i];
        newOp.divisor = op.divisor;
        newOp.linearRotTrans = AU.arrayCopyD(op.linearRotTrans, -1);
        newOp.setFromMatrix(data, false);
        if (magRev != -2)
          newOp.setTimeReversal(op.timeReversal * magRev);
        newOp.xyzOriginal = newOp.xyz;
        addOp(newOp, newOp.xyz, true);
      }
    }
    return true;
  }

  int getSiteMultiplicity(P3d pt, UnitCell unitCell) {
    int n = finalOperations.length;
    Lst<P3d> pts = new Lst<P3d>();
    for (int i = n; --i >= 0;) {
      P3d pt1 = P3d.newP(pt);
      finalOperations[i].rotTrans(pt1);
      unitCell.unitize(pt1);
      for (int j = pts.size(); --j >= 0;) {
        P3d pt0 = pts.get(j);
        if (pt1.distanceSquared(pt0) < JC.UC_TOLERANCE2) {// was 0.000001f) {
          pt1 = null;
          break;
        }      
      }
      if (pt1 != null)
        pts.addLast(pt1);
    }
    return n / pts.size();
  }

  void setName(String name) {
    this.name = name;
    if (name != null && name.startsWith("HM:")) {
      setHMSymbol(name.substring(3));
    }

   strName = displayName = null;
  }

//  M4d getRawOperation(int i) {
//    SymmetryOperation op = new SymmetryOperation(null, 0, false);
//    op.setMatrixFromXYZ(operations[i].xyzOriginal, 0, false);
//    op.doFinalize();
//    return op;
//  }

  String getNameType(String type, SymmetryInterface uc) {
    String ret = null;
    if (type.equals("HM")) {
      ret = hmSymbol;
    } else if (type.equals("ITA")) {
      ret = itaNumber;
    } else if (type.equals("Hall")) {
      ret = hallSymbol;
    } else {
      ret = "?";
    }
    if (ret != null)
      return ret;
    // find the space group using canonical Seitz
    if (info == null)
      info = getInfo(this, hmSymbol, uc.getUnitCellParams(), true, false);
    if (info instanceof String)
      return null;
    @SuppressWarnings("unchecked")
    Map<String, Object> map = (Map<String, Object>) info;
    Object v = map.get(type.equals("Hall") ? "HallSymbol" :
      type.equals("ITA") ? "ita" : "HermannMauguinSymbol");
    return (v == null ? null : v.toString());
  }

  /**
   * Look for Jmol ID such as 10:b or 10 or 10.2 or 10:c,a,b
   * 
   * @param name
   * @return found space group or null
   */
  static SpaceGroup getSpaceGroupFromJmolClegOrITA(String name) {
    getSpaceGroups();
    int n = SG.length;
    if (name.indexOf(":") >= 0) {
      if (name.indexOf(",") >= 0) {
        // 10:c,a,b
        for (int i = 0; i < n; i++)
          if (name.equals(SG[i].clegId))
            return SG[i];
      } else {
        // 10:b
        for (int i = 0; i < n; i++)
          if (name.equals(SG[i].jmolId))
            return SG[i];
      }
    } else if (name.indexOf(".") >= 0) {
      // 10.3
      for (int i = 0; i < n; i++)
        if (name.equals(SG[i].itaIndex))
          return SG[i];
    } else {
      // just first match to ita number
      for (int i = 0; i < n; i++)
        if (name.equals(SG[i].itaNumber))
          return SG[i];
    }
    return null;
  }

  void checkHallOperators() {
    if (nHallOperators != null && nHallOperators.intValue() != operationCount) {
      if (hallInfo == null || hallInfo.nRotations > 0) {
        generateAllOperators(hallInfo); 
      } else {
        init(false);
        doNormalize = false;
        transformSpaceGroup(this, getSpaceGroupFromJmolClegOrITA(itaNumber), null, itaTransform, null);
        hallInfo = null;
      }
    }
  }

  public static SpaceGroup transformSpaceGroup(SpaceGroup sg, SpaceGroup base,
                                               Lst<Object> genPos,
                                               String transform, M4d trm) {
    if (genPos == null) {
//      base.checkHallOperators();
      base.setFinalOperations();
      // should be able to do this much slicker
      genPos = new Lst<Object>();
      for (int i = 0, n = base.getOperationCount(); i < n; i++) {
        genPos.addLast(base.getXyz(i, false));
      }
    }
    boolean normalize = (sg == null || sg.doNormalize);
    String xyzList = addTransformXYZList(sg, genPos, transform, trm, normalize);
    if (sg != null) {
      sg.setITATableNames(sg.jmolId, sg.itaNumber, "1", transform);
      return sg;
    }
    xyzList = xyzList.substring(1);
    //System.out.println("for " + transform + " " + xyzList);
    return findOrCreateSpaceGroupXYZ(xyzList, true);
  }

  private static String addTransformXYZList(SpaceGroup sg, Lst<Object> genPos,
                                            String transform, M4d trm, boolean normalize) {

    M4d trmInv = null, t = null;
    double[] v = null;
    if (transform != null) {
      if (transform.equals("r"))
        transform = SET_R;
      trm = UnitCell.toTrm(transform, trm);
      trmInv = M4d.newM4(trm);
      trmInv.invert();
      v = new double[16];
      t = new M4d();
    }
    String xyzList = addTransformedOperations(sg, genPos, trm, trmInv, t, v, "", null, normalize);
    if (sg != null) {
      double[][] c = UnitCell.getTransformRange(trm);
      if (c != null) {
        P3d p = new P3d();
        for (int i = (int)c[0][0]; i < c[1][0]; i++) {
          for (int j = (int) c[0][1]; j <= c[1][1]; j++) {
            for (int k = (int) c[0][2]; k <= c[1][2]; k++) {
              if (i == 0 && j == 0 && k == 0)
                continue;
              p.set(i, j, k);
              xyzList = addTransformedOperations(sg, genPos, trm, trmInv, t, v, xyzList, p, normalize);
            }
          }
        }
      }
    } 
    return xyzList;
  }

  public M4d[] getOpsCtr(String transform) {
    SpaceGroup sg = getNull(true, true, false);
    transformSpaceGroup(sg, this, null, "!" + transform, null);
    sg.setFinalOperations();
    SpaceGroup sg2 = getNull(true, false, false);
    transformSpaceGroup(sg2, sg, null, transform, null);
    sg2.setFinalOperations();
    return sg2.finalOperations;
  }


  private static String addTransformedOperations(SpaceGroup sg, Lst<Object> genPos,
                                               M4d trm, M4d trmInv, M4d t, double[] v, String xyzList, P3d centering, boolean normalize) {
    if (sg != null)
      sg.latticeOp = 0; // ignore looking for lattice operations
    for (int i = 0, c = genPos.size(); i < c; i++) {
      String xyz = (String) genPos.get(i);
      if (trm != null && (i > 0 || centering != null)) {
        xyz = SymmetryOperation.transformStr(xyz, trm, trmInv, t, v, centering, null, normalize, false);
      }
      if (sg == null) {
        xyzList += ";" + xyz;
      } else {
        sg.addOperation(xyz, 0, false);
      }
    }
    return xyzList;
  }

  private static SpaceGroup findOrCreateSpaceGroupXYZ(String xyzList, boolean doCreate) {
    SpaceGroup sg = new SpaceGroup(-1, NEW_NO_HALL_GROUP, true);
    sg.doNormalize = false;
    String[] xyzlist = xyzList.split(";");
    for (int i = 0, n = xyzlist.length; i < n; i++) {
      SymmetryOperation op = new SymmetryOperation(null, i, false);
      op.setMatrixFromXYZ(xyzlist[i],  0,  false);
      sg.addOp(op, xyzlist[i], false);
    }
    SpaceGroup sg1 = findSpaceGroup(sg.operationCount, sg.getCanonicalSeitzList());
    if (!doCreate)
      return sg1;
    if (sg1 != null)
      sg.setFrom(sg1, true);
    return sg;
  }


  static SpaceGroup getSpaceGroupFromIndex(int i) {
    return (SG != null && i >= 0 && i < SG.length ? SG[i] : null);
  }

  /**
   * from SpaceGroupFinder after finding a match with an ITA setting
   * @param jmolId
   * @param sg
   * @param set
   * @param tr
   */
  void setITATableNames(String jmolId, String sg, String set, String tr) {
    itaNumber = sg;
    itaIndex = (tr != null ? sg + ":" + tr : set.indexOf(".") >= 0 ? set : sg + "." + set);
    itaTransform = tr;
    clegId = sg + ":" + tr;
    this.jmolId = jmolId;
    if (jmolId != null)
      setJmolCode(jmolId);
  }

// 613 settings for 230 space groups (611 from ITA; 2 for added 152 and 154 c-shifts
  /*  see https://cci.lbl.gov/sginfo/itvb_2001_table_a1427_hall_symbols.html
 
intl#     H-M full       HM-abbr   HM-short  Hall
1         P 1            P1        P         P 1       
2         P -1           P-1       P-1       -P 1      
3:b       P 1 2 1        P121      P2        P 2y      
3:b       P 2            P2        P2        P 2y      
3:c       P 1 1 2        P112      P2        P 2       
3:a       P 2 1 1        P211      P2        P 2x      
4:b       P 1 21 1       P1211     P21       P 2yb     
4:b       P 21           P21       P21       P 2yb     
4:b*      P 1 21 1*      P1211*    P21*      P 2y1     
4:c       P 1 1 21       P1121     P21       P 2c      
4:c*      P 1 1 21*      P1121*    P21*      P 21      
4:a       P 21 1 1       P2111     P21       P 2xa     
4:a*      P 21 1 1*      P2111*    P21*      P 2x1     
5:b1      C 1 2 1        C121      C2        C 2y      
5:b1      C 2            C2        C2        C 2y      
5:b2      A 1 2 1        A121      A2        A 2y      
5:b3      I 1 2 1        I121      I2        I 2y      
5:c1      A 1 1 2        A112      A2        A 2       
5:c2      B 1 1 2        B112      B2        B 2       
5:c3      I 1 1 2        I112      I2        I 2       
5:a1      B 2 1 1        B211      B2        B 2x      
5:a2      C 2 1 1        C211      C2        C 2x      
5:a3      I 2 1 1        I211      I2        I 2x      
6:b       P 1 m 1        P1m1      Pm        P -2y     
6:b       P m            Pm        Pm        P -2y     
6:c       P 1 1 m        P11m      Pm        P -2      
6:a       P m 1 1        Pm11      Pm        P -2x     
7:b1      P 1 c 1        P1c1      Pc        P -2yc    
7:b1      P c            Pc        Pc        P -2yc    
7:b2      P 1 n 1        P1n1      Pn        P -2yac   
7:b2      P n            Pn        Pn        P -2yac   
7:b3      P 1 a 1        P1a1      Pa        P -2ya    
7:b3      P a            Pa        Pa        P -2ya    
7:c1      P 1 1 a        P11a      Pa        P -2a     
7:c2      P 1 1 n        P11n      Pn        P -2ab    
7:c3      P 1 1 b        P11b      Pb        P -2b     
7:a1      P b 1 1        Pb11      Pb        P -2xb    
7:a2      P n 1 1        Pn11      Pn        P -2xbc   
7:a3      P c 1 1        Pc11      Pc        P -2xc    
8:b1      C 1 m 1        C1m1      Cm        C -2y     
8:b1      C m            Cm        Cm        C -2y     
8:b2      A 1 m 1        A1m1      Am        A -2y     
8:b3      I 1 m 1        I1m1      Im        I -2y     
8:b3      I m            Im        Im        I -2y     
8:c1      A 1 1 m        A11m      Am        A -2      
8:c2      B 1 1 m        B11m      Bm        B -2      
8:c3      I 1 1 m        I11m      Im        I -2      
8:a1      B m 1 1        Bm11      Bm        B -2x     
8:a2      C m 1 1        Cm11      Cm        C -2x     
8:a3      I m 1 1        Im11      Im        I -2x     
9:b1      C 1 c 1        C1c1      Cc        C -2yc    
9:b1      C c            Cc        Cc        C -2yc    
9:b2      A 1 n 1        A1n1      An        A -2yab   
9:b3      I 1 a 1        I1a1      Ia        I -2ya    
9:-b1     A 1 a 1        A1a1      Aa        A -2ya    
9:-b2     C 1 n 1        C1n1      Cn        C -2yac   
9:-b3     I 1 c 1        I1c1      Ic        I -2yc    
9:c1      A 1 1 a        A11a      Aa        A -2a     
9:c2      B 1 1 n        B11n      Bn        B -2ab    
9:c3      I 1 1 b        I11b      Ib        I -2b     
9:-c1     B 1 1 b        B11b      Bb        B -2b     
9:-c2     A 1 1 n        A11n      An        A -2ab    
9:-c3     I 1 1 a        I11a      Ia        I -2a     
9:a1      B b 1 1        Bb11      Bb        B -2xb    
9:a2      C n 1 1        Cn11      Cn        C -2xac   
9:a3      I c 1 1        Ic11      Ic        I -2xc    
9:-a1     C c 1 1        Cc11      Cc        C -2xc    
9:-a2     B n 1 1        Bn11      Bn        B -2xab   
9:-a3     I b 1 1        Ib11      Ib        I -2xb    
10:b      P 1 2/m 1      P12/m1    P2/m      -P 2y     
10:b      P 2/m          P2/m      P2/m      -P 2y     
10:c      P 1 1 2/m      P112/m    P2/m      -P 2      
10:a      P 2/m 1 1      P2/m11    P2/m      -P 2x     
11:b      P 1 21/m 1     P121/m1   P21/m     -P 2yb    
11:b      P 21/m         P21/m     P21/m     -P 2yb    
11:b*     P 1 21/m 1*    P121/m1*  P21/m*    -P 2y1    
11:c      P 1 1 21/m     P1121/m   P21/m     -P 2c     
11:c*     P 1 1 21/m*    P1121/m*  P21/m*    -P 21     
11:a      P 21/m 1 1     P21/m11   P21/m     -P 2xa    
11:a*     P 21/m 1 1*    P21/m11*  P21/m*    -P 2x1    
12:b1     C 1 2/m 1      C12/m1    C2/m      -C 2y     
12:b1     C 2/m          C2/m      C2/m      -C 2y     
12:b2     A 1 2/m 1      A12/m1    A2/m      -A 2y     
12:b3     I 1 2/m 1      I12/m1    I2/m      -I 2y     
12:b3     I 2/m          I2/m      I2/m      -I 2y     
12:c1     A 1 1 2/m      A112/m    A2/m      -A 2      
12:c2     B 1 1 2/m      B112/m    B2/m      -B 2      
12:c3     I 1 1 2/m      I112/m    I2/m      -I 2      
12:a1     B 2/m 1 1      B2/m11    B2/m      -B 2x     
12:a2     C 2/m 1 1      C2/m11    C2/m      -C 2x     
12:a3     I 2/m 1 1      I2/m11    I2/m      -I 2x     
13:b1     P 1 2/c 1      P12/c1    P2/c      -P 2yc    
13:b1     P 2/c          P2/c      P2/c      -P 2yc    
13:b2     P 1 2/n 1      P12/n1    P2/n      -P 2yac   
13:b2     P 2/n          P2/n      P2/n      -P 2yac   
13:b3     P 1 2/a 1      P12/a1    P2/a      -P 2ya    
13:b3     P 2/a          P2/a      P2/a      -P 2ya    
13:c1     P 1 1 2/a      P112/a    P2/a      -P 2a     
13:c2     P 1 1 2/n      P112/n    P2/n      -P 2ab    
13:c3     P 1 1 2/b      P112/b    P2/b      -P 2b     
13:a1     P 2/b 1 1      P2/b11    P2/b      -P 2xb    
13:a2     P 2/n 1 1      P2/n11    P2/n      -P 2xbc   
13:a3     P 2/c 1 1      P2/c11    P2/c      -P 2xc    
14:b1     P 1 21/c 1     P121/c1   P21/c     -P 2ybc   
14:b1     P 21/c         P21/c     P21/c     -P 2ybc   
14:b2     P 1 21/n 1     P121/n1   P21/n     -P 2yn    
14:b2     P 21/n         P21/n     P21/n     -P 2yn    
14:b3     P 1 21/a 1     P121/a1   P21/a     -P 2yab   
14:b3     P 21/a         P21/a     P21/a     -P 2yab   
14:c1     P 1 1 21/a     P1121/a   P21/a     -P 2ac    
14:c2     P 1 1 21/n     P1121/n   P21/n     -P 2n     
14:c3     P 1 1 21/b     P1121/b   P21/b     -P 2bc    
14:a1     P 21/b 1 1     P21/b11   P21/b     -P 2xab   
14:a2     P 21/n 1 1     P21/n11   P21/n     -P 2xn    
14:a3     P 21/c 1 1     P21/c11   P21/c     -P 2xac   
15:b1     C 1 2/c 1      C12/c1    C2/c      -C 2yc    
15:b1     C 2/c          C2/c      C2/c      -C 2yc    
15:b2     A 1 2/n 1      A12/n1    A2/n      -A 2yab   
15:b3     I 1 2/a 1      I12/a1    I2/a      -I 2ya    
15:b3     I 2/a          I2/a      I2/a      -I 2ya    
15:-b1    A 1 2/a 1      A12/a1    A2/a      -A 2ya    
15:-b2    C 1 2/n 1      C12/n1    C2/n      -C 2yac   
15:-b2    C 2/n          C2/n      C2/n      -C 2yac   
15:-b3    I 1 2/c 1      I12/c1    I2/c      -I 2yc    
15:-b3    I 2/c          I2/c      I2/c      -I 2yc    
15:c1     A 1 1 2/a      A112/a    A2/a      -A 2a     
15:c2     B 1 1 2/n      B112/n    B2/n      -B 2ab    
15:c3     I 1 1 2/b      I112/b    I2/b      -I 2b     
15:-c1    B 1 1 2/b      B112/b    B2/b      -B 2b     
15:-c2    A 1 1 2/n      A112/n    A2/n      -A 2ab    
15:-c3    I 1 1 2/a      I112/a    I2/a      -I 2a     
15:a1     B 2/b 1 1      B2/b11    B2/b      -B 2xb    
15:a2     C 2/n 1 1      C2/n11    C2/n      -C 2xac   
15:a3     I 2/c 1 1      I2/c11    I2/c      -I 2xc    
15:-a1    C 2/c 1 1      C2/c11    C2/c      -C 2xc    
15:-a2    B 2/n 1 1      B2/n11    B2/n      -B 2xab   
15:-a3    I 2/b 1 1      I2/b11    I2/b      -I 2xb    
16        P 2 2 2        P222      P222      P 2 2     
17        P 2 2 21       P2221     P2221     P 2c 2    
17*       P 2 2 21*      P2221*    P2221*    P 21 2    
17:cab    P 21 2 2       P2122     P2122     P 2a 2a   
17:bca    P 2 21 2       P2212     P2212     P 2 2b    
18        P 21 21 2      P21212    P21212    P 2 2ab   
18:cab    P 2 21 21      P22121    P22121    P 2bc 2   
18:bca    P 21 2 21      P21221    P21221    P 2ac 2ac 
19        P 21 21 21     P212121   P212121   P 2ac 2ab 
20        C 2 2 21       C2221     C2221     C 2c 2    
20*       C 2 2 21*      C2221*    C2221*    C 21 2    
20:cab    A 21 2 2       A2122     A2122     A 2a 2a   
20:cab*   A 21 2 2*      A2122*    A2122*    A 2a 21   
20:bca    B 2 21 2       B2212     B2212     B 2 2b    
21        C 2 2 2        C222      C222      C 2 2     
21:cab    A 2 2 2        A222      A222      A 2 2     
21:bca    B 2 2 2        B222      B222      B 2 2     
22        F 2 2 2        F222      F222      F 2 2     
23        I 2 2 2        I222      I222      I 2 2     
24        I 21 21 21     I212121   I212121   I 2b 2c   
25        P m m 2        Pmm2      Pmm2      P 2 -2    
25:cab    P 2 m m        P2mm      P2mm      P -2 2    
25:bca    P m 2 m        Pm2m      Pm2m      P -2 -2   
26        P m c 21       Pmc21     Pmc21     P 2c -2   
26*       P m c 21*      Pmc21*    Pmc21*    P 21 -2   
26:ba-c   P c m 21       Pcm21     Pcm21     P 2c -2c  
26:ba-c*  P c m 21*      Pcm21*    Pcm21*    P 21 -2c  
26:cab    P 21 m a       P21ma     P21ma     P -2a 2a  
26:-cba   P 21 a m       P21am     P21am     P -2 2a   
26:bca    P b 21 m       Pb21m     Pb21m     P -2 -2b  
26:a-cb   P m 21 b       Pm21b     Pm21b     P -2b -2  
27        P c c 2        Pcc2      Pcc2      P 2 -2c   
27:cab    P 2 a a        P2aa      P2aa      P -2a 2   
27:bca    P b 2 b        Pb2b      Pb2b      P -2b -2b 
28        P m a 2        Pma2      Pma2      P 2 -2a   
28*       P m a 2*       Pma2*     Pma2*     P 2 -21   
28:ba-c   P b m 2        Pbm2      Pbm2      P 2 -2b   
28:cab    P 2 m b        P2mb      P2mb      P -2b 2   
28:-cba   P 2 c m        P2cm      P2cm      P -2c 2   
28:-cba*  P 2 c m*       P2cm*     P2cm*     P -21 2   
28:bca    P c 2 m        Pc2m      Pc2m      P -2c -2c 
28:a-cb   P m 2 a        Pm2a      Pm2a      P -2a -2a 
29        P c a 21       Pca21     Pca21     P 2c -2ac 
29:ba-c   P b c 21       Pbc21     Pbc21     P 2c -2b  
29:cab    P 21 a b       P21ab     P21ab     P -2b 2a  
29:-cba   P 21 c a       P21ca     P21ca     P -2ac 2a 
29:bca    P c 21 b       Pc21b     Pc21b     P -2bc -2c
29:a-cb   P b 21 a       Pb21a     Pb21a     P -2a -2ab
30        P n c 2        Pnc2      Pnc2      P 2 -2bc  
30:ba-c   P c n 2        Pcn2      Pcn2      P 2 -2ac  
30:cab    P 2 n a        P2na      P2na      P -2ac 2  
30:-cba   P 2 a n        P2an      P2an      P -2ab 2  
30:bca    P b 2 n        Pb2n      Pb2n      P -2ab -2a
30:a-cb   P n 2 b        Pn2b      Pn2b      P -2bc -2b
31        P m n 21       Pmn21     Pmn21     P 2ac -2  
31:ba-c   P n m 21       Pnm21     Pnm21     P 2bc -2bc
31:cab    P 21 m n       P21mn     P21mn     P -2ab 2ab
31:-cba   P 21 n m       P21nm     P21nm     P -2 2ac  
31:bca    P n 21 m       Pn21m     Pn21m     P -2 -2bc 
31:a-cb   P m 21 n       Pm21n     Pm21n     P -2ab -2 
32        P b a 2        Pba2      Pba2      P 2 -2ab  
32:cab    P 2 c b        P2cb      P2cb      P -2bc 2  
32:bca    P c 2 a        Pc2a      Pc2a      P -2ac -2a
33        P n a 21       Pna21     Pna21     P 2c -2n  
33*       P n a 21*      Pna21*    Pna21*    P 21 -2n  
33:ba-c   P b n 21       Pbn21     Pbn21     P 2c -2ab 
33:ba-c*  P b n 21*      Pbn21*    Pbn21*    P 21 -2ab 
33:cab    P 21 n b       P21nb     P21nb     P -2bc 2a 
33:cab*   P 21 n b*      P21nb*    P21nb*    P -2bc 21 
33:-cba   P 21 c n       P21cn     P21cn     P -2n 2a  
33:-cba*  P 21 c n*      P21cn*    P21cn*    P -2n 21  
33:bca    P c 21 n       Pc21n     Pc21n     P -2n -2ac
33:a-cb   P n 21 a       Pn21a     Pn21a     P -2ac -2n
34        P n n 2        Pnn2      Pnn2      P 2 -2n   
34:cab    P 2 n n        P2nn      P2nn      P -2n 2   
34:bca    P n 2 n        Pn2n      Pn2n      P -2n -2n 
35        C m m 2        Cmm2      Cmm2      C 2 -2    
35:cab    A 2 m m        A2mm      A2mm      A -2 2    
35:bca    B m 2 m        Bm2m      Bm2m      B -2 -2   
36        C m c 21       Cmc21     Cmc21     C 2c -2   
36*       C m c 21*      Cmc21*    Cmc21*    C 21 -2   
36:ba-c   C c m 21       Ccm21     Ccm21     C 2c -2c  
36:ba-c*  C c m 21*      Ccm21*    Ccm21*    C 21 -2c  
36:cab    A 21 m a       A21ma     A21ma     A -2a 2a  
36:cab*   A 21 m a*      A21ma*    A21ma*    A -2a 21  
36:-cba   A 21 a m       A21am     A21am     A -2 2a   
36:-cba*  A 21 a m*      A21am*    A21am*    A -2 21   
36:bca    B b 21 m       Bb21m     Bb21m     B -2 -2b  
36:a-cb   B m 21 b       Bm21b     Bm21b     B -2b -2  
37        C c c 2        Ccc2      Ccc2      C 2 -2c   
37:cab    A 2 a a        A2aa      A2aa      A -2a 2   
37:bca    B b 2 b        Bb2b      Bb2b      B -2b -2b 
38        A m m 2        Amm2      Amm2      A 2 -2    
38:ba-c   B m m 2        Bmm2      Bmm2      B 2 -2    
38:cab    B 2 m m        B2mm      B2mm      B -2 2    
38:-cba   C 2 m m        C2mm      C2mm      C -2 2    
38:bca    C m 2 m        Cm2m      Cm2m      C -2 -2   
38:a-cb   A m 2 m        Am2m      Am2m      A -2 -2   
39        A b m 2        Abm2      Abm2      A 2 -2b   
39:ba-c   B m a 2        Bma2      Bma2      B 2 -2a   
39:cab    B 2 c m        B2cm      B2cm      B -2a 2   
39:-cba   C 2 m b        C2mb      C2mb      C -2a 2   
39:bca    C m 2 a        Cm2a      Cm2a      C -2a -2a 
39:a-cb   A c 2 m        Ac2m      Ac2m      A -2b -2b 
40        A m a 2        Ama2      Ama2      A 2 -2a   
40:ba-c   B b m 2        Bbm2      Bbm2      B 2 -2b   
40:cab    B 2 m b        B2mb      B2mb      B -2b 2   
40:-cba   C 2 c m        C2cm      C2cm      C -2c 2   
40:bca    C c 2 m        Cc2m      Cc2m      C -2c -2c 
40:a-cb   A m 2 a        Am2a      Am2a      A -2a -2a 
41        A b a 2        Aba2      Aba2      A 2 -2ab  
41:ba-c   B b a 2        Bba2      Bba2      B 2 -2ab  
41:cab    B 2 c b        B2cb      B2cb      B -2ab 2  
41:-cba   C 2 c b        C2cb      C2cb      C -2ac 2  
41:bca    C c 2 a        Cc2a      Cc2a      C -2ac -2a
41:a-cb   A c 2 a        Ac2a      Ac2a      A -2ab -2a
42        F m m 2        Fmm2      Fmm2      F 2 -2    
42:cab    F 2 m m        F2mm      F2mm      F -2 2    
42:bca    F m 2 m        Fm2m      Fm2m      F -2 -2   
43        F d d 2        Fdd2      Fdd2      F 2 -2d   
43:cab    F 2 d d        F2dd      F2dd      F -2d 2   
43:bca    F d 2 d        Fd2d      Fd2d      F -2d -2d 
44        I m m 2        Imm2      Imm2      I 2 -2    
44:cab    I 2 m m        I2mm      I2mm      I -2 2    
44:bca    I m 2 m        Im2m      Im2m      I -2 -2   
45        I b a 2        Iba2      Iba2      I 2 -2c   
45:cab    I 2 c b        I2cb      I2cb      I -2a 2   
45:bca    I c 2 a        Ic2a      Ic2a      I -2b -2b 
46        I m a 2        Ima2      Ima2      I 2 -2a   
46:ba-c   I b m 2        Ibm2      Ibm2      I 2 -2b   
46:cab    I 2 m b        I2mb      I2mb      I -2b 2   
46:-cba   I 2 c m        I2cm      I2cm      I -2c 2   
46:bca    I c 2 m        Ic2m      Ic2m      I -2c -2c 
46:a-cb   I m 2 a        Im2a      Im2a      I -2a -2a 
47        P m m m        Pmmm      Pmmm      -P 2 2    
48:1      P n n n        Pnnn      Pnnn      P 2 2 -1n 
48:2      P n n n        Pnnn      Pnnn      -P 2ab 2bc
49        P c c m        Pccm      Pccm      -P 2 2c   
49:cab    P m a a        Pmaa      Pmaa      -P 2a 2   
49:bca    P b m b        Pbmb      Pbmb      -P 2b 2b  
50:1      P b a n        Pban      Pban      P 2 2 -1ab
50:2      P b a n        Pban      Pban      -P 2ab 2b 
50:1cab   P n c b        Pncb      Pncb      P 2 2 -1bc
50:2cab   P n c b        Pncb      Pncb      -P 2b 2bc 
50:1bca   P c n a        Pcna      Pcna      P 2 2 -1ac
50:2bca   P c n a        Pcna      Pcna      -P 2a 2c  
51        P m m a        Pmma      Pmma      -P 2a 2a  
51:ba-c   P m m b        Pmmb      Pmmb      -P 2b 2   
51:cab    P b m m        Pbmm      Pbmm      -P 2 2b   
51:-cba   P c m m        Pcmm      Pcmm      -P 2c 2c  
51:bca    P m c m        Pmcm      Pmcm      -P 2c 2   
51:a-cb   P m a m        Pmam      Pmam      -P 2 2a   
52        P n n a        Pnna      Pnna      -P 2a 2bc 
52:ba-c   P n n b        Pnnb      Pnnb      -P 2b 2n  
52:cab    P b n n        Pbnn      Pbnn      -P 2n 2b  
52:-cba   P c n n        Pcnn      Pcnn      -P 2ab 2c 
52:bca    P n c n        Pncn      Pncn      -P 2ab 2n 
52:a-cb   P n a n        Pnan      Pnan      -P 2n 2bc 
53        P m n a        Pmna      Pmna      -P 2ac 2  
53:ba-c   P n m b        Pnmb      Pnmb      -P 2bc 2bc
53:cab    P b m n        Pbmn      Pbmn      -P 2ab 2ab
53:-cba   P c n m        Pcnm      Pcnm      -P 2 2ac  
53:bca    P n c m        Pncm      Pncm      -P 2 2bc  
53:a-cb   P m a n        Pman      Pman      -P 2ab 2  
54        P c c a        Pcca      Pcca      -P 2a 2ac 
54:ba-c   P c c b        Pccb      Pccb      -P 2b 2c  
54:cab    P b a a        Pbaa      Pbaa      -P 2a 2b  
54:-cba   P c a a        Pcaa      Pcaa      -P 2ac 2c 
54:bca    P b c b        Pbcb      Pbcb      -P 2bc 2b 
54:a-cb   P b a b        Pbab      Pbab      -P 2b 2ab 
55        P b a m        Pbam      Pbam      -P 2 2ab  
55:cab    P m c b        Pmcb      Pmcb      -P 2bc 2  
55:bca    P c m a        Pcma      Pcma      -P 2ac 2ac
56        P c c n        Pccn      Pccn      -P 2ab 2ac
56:cab    P n a a        Pnaa      Pnaa      -P 2ac 2bc
56:bca    P b n b        Pbnb      Pbnb      -P 2bc 2ab
57        P b c m        Pbcm      Pbcm      -P 2c 2b  
57:ba-c   P c a m        Pcam      Pcam      -P 2c 2ac 
57:cab    P m c a        Pmca      Pmca      -P 2ac 2a 
57:-cba   P m a b        Pmab      Pmab      -P 2b 2a  
57:bca    P b m a        Pbma      Pbma      -P 2a 2ab 
57:a-cb   P c m b        Pcmb      Pcmb      -P 2bc 2c 
58        P n n m        Pnnm      Pnnm      -P 2 2n   
58:cab    P m n n        Pmnn      Pmnn      -P 2n 2   
58:bca    P n m n        Pnmn      Pnmn      -P 2n 2n  
59:1      P m m n        Pmmn      Pmmn      P 2 2ab -1
59:2      P m m n        Pmmn      Pmmn      -P 2ab 2a 
59:1cab   P n m m        Pnmm      Pnmm      P 2bc 2 -1
59:2cab   P n m m        Pnmm      Pnmm      -P 2c 2bc 
59:1bca   P m n m        Pmnm      Pmnm      P 2ac 2ac 
59:2bca   P m n m        Pmnm      Pmnm      -P 2c 2a  
60        P b c n        Pbcn      Pbcn      -P 2n 2ab 
60:ba-c   P c a n        Pcan      Pcan      -P 2n 2c  
60:cab    P n c a        Pnca      Pnca      -P 2a 2n  
60:-cba   P n a b        Pnab      Pnab      -P 2bc 2n 
60:bca    P b n a        Pbna      Pbna      -P 2ac 2b 
60:a-cb   P c n b        Pcnb      Pcnb      -P 2b 2ac 
61        P b c a        Pbca      Pbca      -P 2ac 2ab
61:ba-c   P c a b        Pcab      Pcab      -P 2bc 2ac
62        P n m a        Pnma      Pnma      -P 2ac 2n 
62:ba-c   P m n b        Pmnb      Pmnb      -P 2bc 2a 
62:cab    P b n m        Pbnm      Pbnm      -P 2c 2ab 
62:-cba   P c m n        Pcmn      Pcmn      -P 2n 2ac 
62:bca    P m c n        Pmcn      Pmcn      -P 2n 2a  
62:a-cb   P n a m        Pnam      Pnam      -P 2c 2n  
63        C m c m        Cmcm      Cmcm      -C 2c 2   
63:ba-c   C c m m        Ccmm      Ccmm      -C 2c 2c  
63:cab    A m m a        Amma      Amma      -A 2a 2a  
63:-cba   A m a m        Amam      Amam      -A 2 2a   
63:bca    B b m m        Bbmm      Bbmm      -B 2 2b   
63:a-cb   B m m b        Bmmb      Bmmb      -B 2b 2   
64        C m c a        Cmca      Cmca      -C 2ac 2  
64:ba-c   C c m b        Ccmb      Ccmb      -C 2ac 2ac
64:cab    A b m a        Abma      Abma      -A 2ab 2ab
64:-cba   A c a m        Acam      Acam      -A 2 2ab  
64:bca    B b c m        Bbcm      Bbcm      -B 2 2ab  
64:a-cb   B m a b        Bmab      Bmab      -B 2ab 2  
65        C m m m        Cmmm      Cmmm      -C 2 2    
65:cab    A m m m        Ammm      Ammm      -A 2 2    
65:bca    B m m m        Bmmm      Bmmm      -B 2 2    
66        C c c m        Cccm      Cccm      -C 2 2c   
66:cab    A m a a        Amaa      Amaa      -A 2a 2   
66:bca    B b m b        Bbmb      Bbmb      -B 2b 2b  
67        C m m a        Cmma      Cmma      -C 2a 2   
67:ba-c   C m m b        Cmmb      Cmmb      -C 2a 2a  
67:cab    A b m m        Abmm      Abmm      -A 2b 2b  
67:-cba   A c m m        Acmm      Acmm      -A 2 2b   
67:bca    B m c m        Bmcm      Bmcm      -B 2 2a   
67:a-cb   B m a m        Bmam      Bmam      -B 2a 2   
68:1      C c c a        Ccca      Ccca      C 2 2 -1ac
68:2      C c c a        Ccca      Ccca      -C 2a 2ac 
68:1ba-c  C c c b        Cccb      Cccb      C 2 2 -1ac
68:2ba-c  C c c b        Cccb      Cccb      -C 2a 2c  
68:1cab   A b a a        Abaa      Abaa      A 2 2 -1ab
68:2cab   A b a a        Abaa      Abaa      -A 2a 2b  
68:1-cba  A c a a        Acaa      Acaa      A 2 2 -1ab
68:2-cba  A c a a        Acaa      Acaa      -A 2ab 2b 
68:1bca   B b c b        Bbcb      Bbcb      B 2 2 -1ab
68:2bca   B b c b        Bbcb      Bbcb      -B 2ab 2b 
68:1a-cb  B b a b        Bbab      Bbab      B 2 2 -1ab
68:2a-cb  B b a b        Bbab      Bbab      -B 2b 2ab 
69        F m m m        Fmmm      Fmmm      -F 2 2    
70:1      F d d d        Fddd      Fddd      F 2 2 -1d 
70:2      F d d d        Fddd      Fddd      -F 2uv 2vw
71        I m m m        Immm      Immm      -I 2 2    
72        I b a m        Ibam      Ibam      -I 2 2c   
72:cab    I m c b        Imcb      Imcb      -I 2a 2   
72:bca    I c m a        Icma      Icma      -I 2b 2b  
73        I b c a        Ibca      Ibca      -I 2b 2c  
73:ba-c   I c a b        Icab      Icab      -I 2a 2b  
74        I m m a        Imma      Imma      -I 2b 2   
74:ba-c   I m m b        Immb      Immb      -I 2a 2a  
74:cab    I b m m        Ibmm      Ibmm      -I 2c 2c  
74:-cba   I c m m        Icmm      Icmm      -I 2 2b   
74:bca    I m c m        Imcm      Imcm      -I 2 2a   
74:a-cb   I m a m        Imam      Imam      -I 2c 2   
75        P 4            P4        P4        P 4       
76        P 41           P41       P41       P 4w      
76*       P 41*          P41*      P41*      P 41      
77        P 42           P42       P42       P 4c      
77*       P 42*          P42*      P42*      P 42      
78        P 43           P43       P43       P 4cw     
78*       P 43*          P43*      P43*      P 43      
79        I 4            I4        I4        I 4       
80        I 41           I41       I41       I 4bw     
81        P -4           P-4       P-4       P -4      
82        I -4           I-4       I-4       I -4      
83        P 4/m          P4/m      P4/m      -P 4      
84        P 42/m         P42/m     P42/m     -P 4c     
84*       P 42/m*        P42/m*    P42/m*    -P 42     
85:1      P 4/n          P4/n      P4/n      P 4ab -1ab
85:2      P 4/n          P4/n      P4/n      -P 4a     
86:1      P 42/n         P42/n     P42/n     P 4n -1n  
86:2      P 42/n         P42/n     P42/n     -P 4bc    
87        I 4/m          I4/m      I4/m      -I 4      
88:1      I 41/a         I41/a     I41/a     I 4bw -1bw
88:2      I 41/a         I41/a     I41/a     -I 4ad    
89        P 4 2 2        P422      P422      P 4 2     
90        P 4 21 2       P4212     P4212     P 4ab 2ab 
91        P 41 2 2       P4122     P4122     P 4w 2c   
91*       P 41 2 2*      P4122*    P4122*    P 41 2c   
92        P 41 21 2      P41212    P41212    P 4abw 2nw
93        P 42 2 2       P4222     P4222     P 4c 2    
93*       P 42 2 2*      P4222*    P4222*    P 42 2    
94        P 42 21 2      P42212    P42212    P 4n 2n   
95        P 43 2 2       P4322     P4322     P 4cw 2c  
95*       P 43 2 2*      P4322*    P4322*    P 43 2c   
96        P 43 21 2      P43212    P43212    P 4nw 2abw
97        I 4 2 2        I422      I422      I 4 2     
98        I 41 2 2       I4122     I4122     I 4bw 2bw 
99        P 4 m m        P4mm      P4mm      P 4 -2    
100       P 4 b m        P4bm      P4bm      P 4 -2ab  
101       P 42 c m       P42cm     P42cm     P 4c -2c  
101*      P 42 c m*      P42cm*    P42cm*    P 42 -2c  
102       P 42 n m       P42nm     P42nm     P 4n -2n  
103       P 4 c c        P4cc      P4cc      P 4 -2c   
104       P 4 n c        P4nc      P4nc      P 4 -2n   
105       P 42 m c       P42mc     P42mc     P 4c -2   
105*      P 42 m c*      P42mc*    P42mc*    P 42 -2   
106       P 42 b c       P42bc     P42bc     P 4c -2ab 
106*      P 42 b c*      P42bc*    P42bc*    P 42 -2ab 
107       I 4 m m        I4mm      I4mm      I 4 -2    
108       I 4 c m        I4cm      I4cm      I 4 -2c   
109       I 41 m d       I41md     I41md     I 4bw -2  
110       I 41 c d       I41cd     I41cd     I 4bw -2c 
111       P -4 2 m       P-42m     P-42m     P -4 2    
112       P -4 2 c       P-42c     P-42c     P -4 2c   
113       P -4 21 m      P-421m    P-421m    P -4 2ab  
114       P -4 21 c      P-421c    P-421c    P -4 2n   
115       P -4 m 2       P-4m2     P-4m2     P -4 -2   
116       P -4 c 2       P-4c2     P-4c2     P -4 -2c  
117       P -4 b 2       P-4b2     P-4b2     P -4 -2ab 
118       P -4 n 2       P-4n2     P-4n2     P -4 -2n  
119       I -4 m 2       I-4m2     I-4m2     I -4 -2   
120       I -4 c 2       I-4c2     I-4c2     I -4 -2c  
121       I -4 2 m       I-42m     I-42m     I -4 2    
122       I -4 2 d       I-42d     I-42d     I -4 2bw  
123       P 4/m m m      P4/mmm    P4/mmm    -P 4 2    
124       P 4/m c c      P4/mcc    P4/mcc    -P 4 2c   
125:1     P 4/n b m      P4/nbm    P4/nbm    P 4 2 -1ab
125:2     P 4/n b m      P4/nbm    P4/nbm    -P 4a 2b  
126:1     P 4/n n c      P4/nnc    P4/nnc    P 4 2 -1n 
126:2     P 4/n n c      P4/nnc    P4/nnc    -P 4a 2bc 
127       P 4/m b m      P4/mbm    P4/mbm    -P 4 2ab  
128       P 4/m n c      P4/mnc    P4/mnc    -P 4 2n   
129:1     P 4/n m m      P4/nmm    P4/nmm    P 4ab 2ab 
129:2     P 4/n m m      P4/nmm    P4/nmm    -P 4a 2a  
130:1     P 4/n c c      P4/ncc    P4/ncc    P 4ab 2n -
130:2     P 4/n c c      P4/ncc    P4/ncc    -P 4a 2ac 
131       P 42/m m c     P42/mmc   P42/mmc   -P 4c 2   
132       P 42/m c m     P42/mcm   P42/mcm   -P 4c 2c  
133:1     P 42/n b c     P42/nbc   P42/nbc   P 4n 2c -1
133:2     P 42/n b c     P42/nbc   P42/nbc   -P 4ac 2b 
134:1     P 42/n n m     P42/nnm   P42/nnm   P 4n 2 -1n
134:2     P 42/n n m     P42/nnm   P42/nnm   -P 4ac 2bc
135       P 42/m b c     P42/mbc   P42/mbc   -P 4c 2ab 
135*      P 42/m b c*    P42/mbc*  P42/mbc*  -P 42 2ab 
136       P 42/m n m     P42/mnm   P42/mnm   -P 4n 2n  
137:1     P 42/n m c     P42/nmc   P42/nmc   P 4n 2n -1
137:2     P 42/n m c     P42/nmc   P42/nmc   -P 4ac 2a 
138:1     P 42/n c m     P42/ncm   P42/ncm   P 4n 2ab -
138:2     P 42/n c m     P42/ncm   P42/ncm   -P 4ac 2ac
139       I 4/m m m      I4/mmm    I4/mmm    -I 4 2    
140       I 4/m c m      I4/mcm    I4/mcm    -I 4 2c   
141:1     I 41/a m d     I41/amd   I41/amd   I 4bw 2bw 
141:2     I 41/a m d     I41/amd   I41/amd   -I 4bd 2  
142:1     I 41/a c d     I41/acd   I41/acd   I 4bw 2aw 
142:2     I 41/a c d     I41/acd   I41/acd   -I 4bd 2c 
143       P 3            P3        P3        P 3       
144       P 31           P31       P31       P 31      
145       P 32           P32       P32       P 32      
146:h     R 3            R3        R3        R 3       
146:r     R 3            R3        R3        P 3*      
147       P -3           P-3       P-3       -P 3      
148:h     R -3           R-3       R-3       -R 3      
148:r     R -3           R-3       R-3       -P 3*     
149       P 3 1 2        P312      P32       P 3 2     
150       P 3 2 1        P321      P32       P 3 2     
151       P 31 1 2       P3112     P312      P 31 2 (0 
152       P 31 2 1       P3121     P312      P 31 2    
153       P 32 1 2       P3212     P322      P 32 2 (0 
154       P 32 2 1       P3221     P322      P 32 2    
155:h     R 3 2          R32       R32       R 3 2     
155:r     R 3 2          R32       R32       P 3* 2    
156       P 3 m 1        P3m1      P3m       P 3 -2"   
157       P 3 1 m        P31m      P3m       P 3 -2    
158       P 3 c 1        P3c1      P3c       P 3 -2"c
159       P 3 1 c        P31c      P3c       P 3 -2c   
160:h     R 3 m          R3m       R3m       R 3 -2    
160:r     R 3 m          R3m       R3m       P 3* -2   
161:h     R 3 c          R3c       R3c       R 3 -2"c
161:r     R 3 c          R3c       R3c       P 3* -2n  
162       P -3 1 m       P-31m     P-3m      -P 3 2    
163       P -3 1 c       P-31c     P-3c      -P 3 2c   
164       P -3 m 1       P-3m1     P-3m      -P 3 2"   
165       P -3 c 1       P-3c1     P-3c      -P 3 2"c
166:h     R -3 m         R-3m      R-3m      -R 3 2"    
166:r     R -3 m         R-3m      R-3m      -P 3* 2   
167:h     R -3 c         R-3c      R-3c      -R 3 2"c
167:r     R -3 c         R-3c      R-3c      -P 3* 2n  
168       P 6            P6        P6        P 6       
169       P 61           P61       P61       P 61      
170       P 65           P65       P65       P 65      
171       P 62           P62       P62       P 62      
172       P 64           P64       P64       P 64      
173       P 63           P63       P63       P 6c      
173*      P 63*          P63*      P63*      P 63      
174       P -6           P-6       P-6       P -6      
175       P 6/m          P6/m      P6/m      -P 6      
176       P 63/m         P63/m     P63/m     -P 6c     
176*      P 63/m*        P63/m*    P63/m*    -P 63     
177       P 6 2 2        P622      P622      P 6 2     
178       P 61 2 2       P6122     P6122     P 61 2 (0 0 5)
179       P 65 2 2       P6522     P6522     P 65 2 (0 0 1)
180       P 62 2 2       P6222     P6222     P 62 2 (0 0 4)
181       P 64 2 2       P6422     P6422     P 64 2 (0 0 2)
182       P 63 2 2       P6322     P6322     P 6c 2c   
182*      P 63 2 2*      P6322*    P6322*    P 63 2c   
183       P 6 m m        P6mm      P6mm      P 6 -2    
184       P 6 c c        P6cc      P6cc      P 6 -2c   
185       P 63 c m       P63cm     P63cm     P 6c -2   
185*      P 63 c m*      P63cm*    P63cm*    P 63 -2   
186       P 63 m c       P63mc     P63mc     P 6c -2c  
186*      P 63 m c*      P63mc*    P63mc*    P 63 -2c  
187       P -6 m 2       P-6m2     P-6m2     P -6 2    
188       P -6 c 2       P-6c2     P-6c2     P -6c 2   
189       P -6 2 m       P-62m     P-62m     P -6 -2   
190       P -6 2 c       P-62c     P-62c     P -6c -2c 
191       P 6/m m m      P6/mmm    P6/mmm    -P 6 2    
192       P 6/m c c      P6/mcc    P6/mcc    -P 6 2c   
193       P 63/m c m     P63/mcm   P63/mcm   -P 6c 2   
193*      P 63/m c m*    P63/mcm*  P63/mcm*  -P 63 2   
194       P 63/m m c     P63/mmc   P63/mmc   -P 6c 2c  
194*      P 63/m m c*    P63/mmc*  P63/mmc*  -P 63 2c  
195       P 2 3          P23       P23       P 2 2 3   
196       F 2 3          F23       F23       F 2 2 3   
197       I 2 3          I23       I23       I 2 2 3   
198       P 21 3         P213      P213      P 2ac 2ab 3
199       I 21 3         I213      I213      I 2b 2c 3 
200       P m -3         Pm-3      Pm-3      -P 2 2 3  
201:1     P n -3         Pn-3      Pn-3      P 2 2 3 -1n
201:2     P n -3         Pn-3      Pn-3      -P 2ab 2bc 3
202       F m -3         Fm-3      Fm-3      -F 2 2 3  
203:1     F d -3         Fd-3      Fd-3      F 2 2 3 -1d
203:2     F d -3         Fd-3      Fd-3      -F 2uv 2vw 3
204       I m -3         Im-3      Im-3      -I 2 2 3  
205       P a -3         Pa-3      Pa-3      -P 2ac 2ab 3
206       I a -3         Ia-3      Ia-3      -I 2b 2c 3
207       P 4 3 2        P432      P432      P 4 2 3   
208       P 42 3 2       P4232     P4232     P 4n 2 3  
209       F 4 3 2        F432      F432      F 4 2 3   
210       F 41 3 2       F4132     F4132     F 4d 2 3  
211       I 4 3 2        I432      I432      I 4 2 3   
212       P 43 3 2       P4332     P4332     P 4acd 2ab 3
213       P 41 3 2       P4132     P4132     P 4bd 2ab 3
214       I 41 3 2       I4132     I4132     I 4bd 2c 3
215       P -4 3 m       P-43m     P-43m     P -4 2 3  
216       F -4 3 m       F-43m     F-43m     F -4 2 3  
217       I -4 3 m       I-43m     I-43m     I -4 2 3  
218       P -4 3 n       P-43n     P-43n     P -4n 2 3 
219       F -4 3 c       F-43c     F-43c     F -4a 2 3 
220       I -4 3 d       I-43d     I-43d     I -4bd 2c 3
221       P m -3 m       Pm-3m     Pm-3m     -P 4 2 3  
222:1     P n -3 n       Pn-3n     Pn-3n     P 4 2 3 -1n
222:2     P n -3 n       Pn-3n     Pn-3n     -P 4a 2bc 3
223       P m -3 n       Pm-3n     Pm-3n     -P 4n 2 3 
224:1     P n -3 m       Pn-3m     Pn-3m     P 4n 2 3 -1n
224:2     P n -3 m       Pn-3m     Pn-3m     -P 4bc 2bc 3
225       F m -3 m       Fm-3m     Fm-3m     -F 4 2 3  
226       F m -3 c       Fm-3c     Fm-3c     -F 4a 2 3 
227:1     F d -3 m       Fd-3m     Fd-3m     F 4d 2 3 -1d
227:2     F d -3 m       Fd-3m     Fd-3m     -F 4vw 2vw 3
228:1     F d -3 c       Fd-3c     Fd-3c     F 4d 2 3 -1ad
228:2     F d -3 c       Fd-3c     Fd-3c     -F 4ud 2vw 3
229       I m -3 m       Im-3m     Im-3m     -I 4 2 3  
230       I a -3 d       Ia-3d     Ia-3d     -I 4bd 2c 3


   */
  
  static {
    getSpaceGroups();
  }

}
