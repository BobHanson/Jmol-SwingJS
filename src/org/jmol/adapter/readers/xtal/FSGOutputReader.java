package org.jmol.adapter.readers.xtal;

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.XtalSymmetry.FileSymmetry;
import org.jmol.api.JmolAdapter;
import org.jmol.symmetry.SymmetryOperation;
import org.jmol.util.BSUtil;
import org.jmol.util.Logger;
import org.jmol.util.SimpleUnitCell;
import org.jmol.util.Vibration;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.M3d;
import javajs.util.M4d;
import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.SB;

/**
 * A reader for JSON output from the FINDSPINGROUP program.
 * 
 * 
 * 
 * 
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

public class FSGOutputReader extends AtomSetCollectionReader {

  private short[] elementNumbers;
//  private boolean addSymmetry;
  private boolean spinOnly;
  private boolean convertToABC = true;
  private Map<String, Object> json;
  private String configuration;
  private boolean isCoplanar;
  private boolean isCollinear;
  private int firstTranslation;
  private String spinFrame;
  private String fullName;
  private final static int DEFAULT_PRECISION = 5;

  @Override
  public void initializeReader() throws Exception {
    super.initializeReader();
    //convertToABC = false;
    spinOnly = checkFilterKey("SPINONLY");
    checkNearAtoms = !checkFilterKey("NOSPECIAL"); // as in "no special positions"
    if (!filteredPrecision) {
      precision = DEFAULT_PRECISION ;
      filteredPrecision = true;
    }      
    System.out.println("FSGOutput using precision " + precision);
  //  addSymmetry = checkFilterKey("ADDSYMMETRY");
    String symbols = getFilterWithCase("elements=");
    if (symbols != null) {
      String[] s = PT.split(symbols.replace(',', ' '), " ");
      elementNumbers = new short[s.length];
      for (int i = s.length; --i >= 0;) {
        elementNumbers[i] = (short) JmolAdapter.getElementNumber(s[i]);
      }
        
    }
    SB sb = new SB();
    try {
      while (rd() != null)
        sb.append(line);
      json = vwr.parseJSONMap(sb.toString());
      processJSON();
    } catch (Exception e) {
      e.printStackTrace();
    }
    continuing = false;
  }

  private void processJSON() {
    getHeaderInfo();
    Lst<Object> info = getList(json, "G0_std_Cell");
    getCellInfo(getListItem(info, 0));
    configuration = (String) json.get("Configuration"); // "Coplanar"
    isCoplanar = "Coplanar".equals(configuration);
    isCollinear = "Collinear".equals(configuration);
    addMoreUnitCellInfo("configuration=" + configuration);
    readAllOperators(getList(json, "G0_std_operations"));
    @SuppressWarnings("unchecked")
    String[] symbols = getSymbols((Map<String, Object>)json.get("AtomTypeDict"));
    readAtomsAndMoments(info, symbols);
  }

  private String[] getSymbols(Map<String, Object> map) {
    String[] symbols = new String[map.size() + 1];
    for (Entry<String, Object> e : map.entrySet()) {
      symbols[Integer.parseInt(e.getKey())] = (String) e.getValue();
    }
    return symbols;
  }

  @SuppressWarnings("unchecked")
  private Lst<Object> getListItem(Lst<Object> info, int i) {
    return (Lst<Object>) info.get(i);
  }

  @SuppressWarnings("unchecked")
  private static Lst<Object> getList(Map<String, Object> json, String key) {
    return (Lst<Object>) json.get(key);
  }

  private void getHeaderInfo() {
    fullName = (String) json.get("SSG_international_symbol");
    setSpaceGroupName(fixName(fullName));
  }

  private String fixName(String name) {
    // Setting space group name to {P}^{2_\frac{5\pi}{6}} {6_{3}/}^{1}{m}^{2_\frac{\pi}{3}} {m}^{2_{001}}{c}|(3^1_{001},3^1_{001},1)\\^m1
    System.out.println("FSGOutput " + name);
    int pt;
    while ((pt = name.indexOf("\\frac{")) >= 0) {
      int pt2 = name.indexOf("{", pt + 7);
      int pt3 = name.indexOf("}", pt2 + 1);
      name = name.substring(0, pt) 
          + "(" + name.substring(pt+5, pt2) + "/" + name.substring(pt2, pt3) + ")" + name.substring(pt3);
    }
    name = PT.rep(name, "\\pi", "\u03C0");
    name = PT.rep(name, "\\enspace", " ");
    name = PT.rep(name, "\\infty", "\u221E");

    name = PT.rep(name, "\\\\^", "^");
    name = PT.replaceAllCharacters(name,  "{}", "");
    return name;
  }

  private void getCellInfo(Lst<Object> list) {
    setFractionalCoordinates(true);
    for (int i = 0; i < 3; i++) {
      P3d v = getPoint(getListItem(list, i));
      addExplicitLatticeVector(i, new double[] { v.x, v.y, v.z }, 0);
    }
  }

  private static P3d getPoint(Lst<Object> item) {
    return P3d.new3(getValue(item, 0), getValue(item, 1), getValue(item, 2));
  }

  private static double getValue(Lst<Object> item, int i) {
    return ((Number) item.get(i)).doubleValue();
  }

  @SuppressWarnings("unchecked")
  private void readAllOperators(Lst<Object> info) {
    int n = info.size();
    int nops = 0;
    for (int i = 0; i < n; i++) {
      Lst<Object> op = (Lst<Object>) info.get(i);
      M4d mspin = readMatrix(getListItem(op, 0), null);
      M4d mop = readMatrix(getListItem(op, 1), getListItem(op, 2));
      String s = SymmetryOperation.getXYZFromMatrixFrac(mop, false, false,
          false, true, true, SymmetryOperation.MODE_XYZ)
          + SymmetryOperation.getSpinString(mspin, true)
          + (isCoplanar ? "+" : "");
      int iop = setSymmetryOperator(s);
      if (Logger.debugging)
        System.out.println(
            "FSGOutput op[" + (i + 1) + "]=" + s + (iop < 0 ? " SKIPPED" : ""));
      if (iop >= 0) {
        boolean isTranslation = SymmetryOperation.isTranslation(mop);
        if (firstTranslation == 0 && isTranslation)
          firstTranslation = nops;
        nops++;
      }
    }
    if (firstTranslation == 0)
      firstTranslation = nops;
    System.out.println("FSGOutput G0_operationCount(initial)=" + n);
  }

  private M4d readMatrix(Lst<Object> rot, Lst<Object> trans) {
    M3d r = new M3d();
    for (int i = 0; i < 3; i++) {
      r.setRowV(i, getPoint(getListItem(rot, i)));
    }
    P3d t = (trans == null ? new P3d() : getPoint(trans));
    return M4d.newMV(r, t);
  }

  private void readAtomsAndMoments(Lst<Object> info, String[] symbols) {
    Lst<Object> atoms = getListItem(info, 1);
    Lst<Object> ids = getListItem(info, 2);
    Lst<Object> moments = getListItem(info, 3);
    for (int i = 0, n = atoms.size(); i < n; i++) {
      P3d xyz = getPoint(getListItem(atoms, i));
      int id = (int) getValue(ids, i);
        
      // this is only partially correct --
      // an element may have more than one moment, but we
      // cannot tell this from the JSON.
      Atom a = new Atom();
      a.setT(xyz);
      setAtomCoord(a);
      P3d moment = getPoint(getListItem(moments, i));
      double mag = moment.length();
      if (mag > 0) {
        if (Logger.debugging)
          System.out.println("FGSOutput moment " + i + " " + moment + " " + mag);
        Vibration v = new Vibration();
        v.setType(Vibration.TYPE_SPIN);
        v.setT(moment);
        v.magMoment = mag;
        a.vib = v;
        System.out.println("FSGOutput atom/spin " + i + " " + a.vib + " " + mag);
      } else {
        if (spinOnly)
          continue;
      }
      // no element symbols!!
      if (symbols != null) {
        a.elementSymbol = symbols[id];
      } else if (elementNumbers != null && id <= elementNumbers.length) {
        a.elementNumber = elementNumbers[id - 1];
      } else {
        a.elementNumber = (short) (id + 2); // start with boron
      }
      asc.addAtom(a);
    }
  }
  
  @Override
  protected void warnSkippingOperation(String xyz) {
    // ignore - this is from Coplanar +/-w
 }

  @Override
  public void doPreSymmetry(boolean doApplySymmetry) throws Exception {
    FileSymmetry fs = asc.getSymmetry();
    BS bs = BSUtil.newBitSet2(0, asc.ac);
    excludeAtoms(0, bs, fs);
    filterFsgAtoms(bs);
    preSymmetrySetMoments();
    System.out.println("FSGOutputReader using atoms " + bs);
    Lst<String> lst = fs.setSpinList(configuration);
    if (lst != null) {
      asc.setCurrentModelInfo("spinList", lst);
      appendLoadNote(
          lst.size() + " spin operations -- see _M.spinList and atom.spin");
    }
    System.out.println("FSGOutput operationCount=" + fs.getSpaceGroupOperationCount());
    Map<String, Object> info = getSCIFInfo(fs, lst);
    asc.setCurrentModelInfo("scifInfo", info);
    asc.setCurrentModelInfo("spinFrame", spinFrame);
  }

  /**
   * We need to generate the moment for the SCIF file
   */
  private void preSymmetrySetMoments() {
    double a = symmetry.getUnitCellInfoType(SimpleUnitCell.INFO_A);
    double b = symmetry.getUnitCellInfoType(SimpleUnitCell.INFO_B);
    double c = symmetry.getUnitCellInfoType(SimpleUnitCell.INFO_C);
    for (int i = asc.ac; --i >= 0;) {
      Vibration v = (Vibration) asc.atoms[i].vib;
      if (v != null)
        spinCartesianToFractional(v, a, b, c);
    }
  }

  /*
   * After converting to fractional, we need to 
   * scale the value by the dimensions of the cell
   * in order to set the value that will go into the SCIF
   * file. We save that value as Vibration.v0.
   * We restore the cartesian value for the rotations.
   * 
   */
  private void spinCartesianToFractional(Vibration v, double a, double b, double c) {
    P3d p = P3d.newP(v);
    symmetry.toFractional(v, true);
    v.x *= a;
    v.y *= b;
    v.z *= c;
    v.setV0();
    v.setT(p);     
  }

  private void filterFsgAtoms(BS bs) {
    for (int p = 0, i = bs.nextSetBit(0); i >= 0; p++, i = bs.nextSetBit(i + 1)) {
      asc.atoms[p] = asc.atoms[i];
      asc.atoms[p].index = p;
    }
    asc.atomSetAtomCounts[0] = asc.ac = bs.cardinality();
    
  }

  @Override
  protected void finalizeSubclassReader() throws Exception {
    asc.setNoAutoBond();
    applySymmetryAndSetTrajectory();
    addJmolScript("vectors on;vectors 0.15;");
    vibsFractional = true;
    int n = asc.getXSymmetry().setMagneticMoments(true);
    appendLoadNote(n
        + " magnetic moments - use VECTORS ON/OFF or VECTOR MAX x.x or SELECT VXYZ>0");
  }

  private Map<String, Object> getSCIFInfo(FileSymmetry fs,
                                          Lst<String> spinList) {
    Map<String, Object> m = new Hashtable<>();
    try {
      System.out.println("FSGOutput SSG_international_symbol=" + fullName);
      mput(m, "SSG_international_symbol", fullName);

      System.out.println("FSGOutput simpleName=" + sgName);
      mput(m, "simpleName", sgName);

      Integer msgNum = (Integer) json.get("MSG_num");
      System.out.println("FSGOutput MSG_num=" + msgNum);
      mput(m, "MSG_num", msgNum);

      System.out.println("FSGOutput Configuration=" + configuration);
      mput(m, "configuration", configuration);

      //int ik = ((Integer) json.get("ik")).intValue();

      String gSymbol = (String) json.get("G_symbol");
      System.out.println("FSGOutput G_symbol=" + gSymbol);
      mput(m, "G_Symbol", gSymbol);
            
      String g0Symbol = (String) json.get("G0_symbol");
      System.out.println("FSGOutput G0=" + g0Symbol);
      mput(m, "G0_Symbol", g0Symbol);
      int pt = g0Symbol.indexOf('(');
      int g0ItaNo = Integer
          .parseInt(g0Symbol.substring(pt + 1, g0Symbol.length() - 1));
      String g0HMName = g0Symbol.substring(0, pt).trim();
      mput(m, "G0_ItaNo", Integer.valueOf(g0ItaNo));
      mput(m, "G0_HMName", g0HMName);

      String l0Symbol = (String) json.get("L0_symbol");
      System.out.println("FSGOutput L0=" + l0Symbol);
      mput(m, "L0_Symbol", l0Symbol);
      pt = l0Symbol.indexOf('(');
      int l0ItaNo = Integer
          .parseInt(l0Symbol.substring(pt + 1, l0Symbol.length() - 1));

      int ik = ((Integer) json.get("ik")).intValue();

      String fsgID = "" + l0ItaNo + "." + g0ItaNo + "." + ik + ".?";
      appendLoadNote("FSG ID " + fsgID);
      mput(m, "fsgID", fsgID);
      System.out.println("fsgID=" + fsgID);
      boolean isPrimitive = g0HMName.charAt(0) == 'P';
      mput(m, "G0_isPrimitive", Boolean.valueOf(isPrimitive));
      mput(m, "G0_atomCount", Integer.valueOf(asc.ac));
      System.out.println("FSGOutput G0_atomCount=" + asc.ac);

      Lst<Object> r0 = getList(json, "transformation_matrix_ini_G0");
      Lst<Object> t0 = getList(json, "origin_shift_ini_G0");
      M4d m2g0 = readMatrix(r0, t0);
      m2g0.transpose33();
      String abcm = SymmetryOperation.getXYZFromMatrixFrac(m2g0, false, false, false, true, true, SymmetryOperation.MODE_ABC);
      mput(m, "msgTransform", abcm);
      asc.setCurrentModelInfo("unitcell_msg", abcm);

      symmetry.setUnitCellFromParams(unitCellParams, true, cellSlop);
      spinFrame = calculateSpinFrame(readMatrix(getList(json, "transformation_matrix_spin_cartesian_lattice_G0"), null));
      System.out.println("FSGOutput G0 spinFrame=" + spinFrame);
      addMoreUnitCellInfo("spinFrame=" + spinFrame);
      asc.setCurrentModelInfo("unitcell_spin", spinFrame);
      Lst<P3d> spinLattice = fs.getLatticeCentering();
      Lst<String>lattice = SymmetryOperation.getLatticeCenteringStrings(fs.getSymmetryOperations());
      if (!lattice.isEmpty()) {
        lattice.add(0, "x,y,z(u,v,w)");
        mput(m, "G0_spinLattice", lattice);
      }
      // DANGER HERE - what if G0 is not primitive?
      String abc = calculateChildTransform(spinLattice);
      mput(m, "childTransform", abc);
      System.out.println("FSGOutput G0_childTransform=" + abc);
      

      Lst<String> ops = new Lst<>();
      SymmetryOperation[] symops = fs.getSymmetryOperations();
      for (int i = 0; i < firstTranslation; i++) {
        ops.addLast(symops[i].getXyz(false));
      }
      
      if (spinList != null) {
        mput(m, "G0_spinList", spinList);
        Map<String, Integer> mapSpinToID = new Hashtable<>();
        Lst<String> scifList = null;
        int[] scifListTimeRev = new int[spinList.size()];
        String newSpinFrame = (convertToABC ? "a,b,c" : spinFrame);
        
        scifList = new Lst<String>();
        M4d msf = null, msfInv = null;
        if (!spinFrame.equals(newSpinFrame)) {
          msf = (M4d) SymmetryOperation.staticConvertOperation(spinFrame, null,
              null);
          msf.transpose();
          msfInv = M4d.newM4(msf);
          msfInv.invert();
        }
        
        for (int i = 0, n = spinList.size(); i < n; i++) {
          String fsgOp = spinList.get(i);
          mapSpinToID.put(fsgOp, Integer.valueOf(i));
          M4d m4 = (M4d) symmetry.convertTransform(fsgOp, null);
          if (msf != null) {
            m4.mul2(m4, msfInv);
            m4.mul2(msf, m4);
          }
          String s = SymmetryOperation.getXYZFromMatrixFrac(m4, false, false,
              false, false, true, "uvw");
          System.out.println(s + "\t <- " + fsgOp);
          scifList.addLast(s);
          int timeReversal = (int) Math.round(m4.determinant3());
          scifListTimeRev[i] = timeReversal;
        }
        mput(m, "spinFrame", newSpinFrame); // because we are converting it
        mput(m, "SCIF_spinList", scifList);
        mput(m, "SCIF_spinListTR", scifListTimeRev);
        ops = setSCIFSpinLists(m, mapSpinToID, ops, firstTranslation, "G0_operationURefs");
        setSCIFSpinLists(m, mapSpinToID, lattice, lattice.size(), "G0_spinLatticeURefs");
      }
      mput(m, "G0_operations", ops);
      mput(m, "G0_operationCount", Integer.valueOf(ops.size()));
      System.out.println("FSGOutput G0_operationCount(w/o translations)=" + ops.size());
      
    } catch (Exception e) {
      mput(m, "exception", e.toString());
      e.printStackTrace();
    }
    return m;
  }

  private void excludeAtoms(int i0, BS bs, FileSymmetry fs) {
    if (i0 < 0)
      return;
    for (int i = bs.nextSetBit(i0 + 1); i >= 0; i= bs.nextSetBit(i + 1)) {
      if (findSymop(i0, i, fs)) {
        bs.clear(i);
      }
    }
    excludeAtoms(bs.nextSetBit(i0 + 1), bs, fs);
  }
  
  private P3d p2 = new P3d();
  
  private boolean findSymop(int i1, int i2, FileSymmetry fs) {
    Atom a = asc.atoms[i1];
    Atom b = asc.atoms[i2];
    if (a.elementNumber != b.elementNumber)
      return false;
    SymmetryOperation[] ops = fs.getSymmetryOperations();
    int nops = fs.getSpaceGroupOperationCount();
    for (int i = 1; i < nops; i++) {
       p2.setP(a);
       ops[i].rotTrans(p2);
       symmetry.unitize(p2);
       if (p2.distanceSquared(b) < 1e-6) {
         return true;
       }
    }
    return false;
  }

  private Lst<String> setSCIFSpinLists(Map<String, Object> m,
                                Map<String, Integer> mapSpinToID,
                                Lst<String> ops, int len, String key) {
    Lst<Integer> lst = new Lst<>();
    Lst<String> lstOpsIncluded = new Lst<>();
    for (int i = 0, n = len; i < n; i++) {
      String o = ops.get(i);
      int pt = o.indexOf("(");
      String uvw = o.substring(pt + 1, o.indexOf(')', pt + 1));
      Integer ipt = mapSpinToID.get(uvw);
      lst.addLast(ipt);
      lstOpsIncluded.addLast(o);
    }
    int[] val = new int[lst.size()];
    for (int i = val.length; --i >= 0;)
      val[i] = lst.get(i).intValue();
    m.put(key, val);
    return lstOpsIncluded;
  }

  private static void mput(Map<String, Object> m, String key, Object val) {
    if (val != null)
      m.put(key, val);
  }

  private static String calculateChildTransform(Lst<P3d> spinLattice) {
    if (spinLattice == null || spinLattice.isEmpty()) {
      return "a,b,c";
    }
    double minx = 1, miny = 1, minz = 1;
    for (int i = spinLattice.size(); --i >= 0;) {
      P3d c = spinLattice.get(i);
      if (c.x > 0 && c.x < minx)
        minx = c.x;
      if (c.y > 0 && c.y < miny)
        miny = c.y;
      if (c.z > 0 && c.z < minz)
        minz = c.z;
    }
    return (minx > 0 && minx < 1 ? "" + Math.round(1 / minx) : "") + "a," //
        + (miny > 0 && miny < 1 ? "" + Math.round(1 / miny) : "") + "b," //
        + (minz > 0 && minz < 1 ? "" + Math.round(1 / minz) : "") + "c";
  }

  private String calculateSpinFrame(M4d m4) {
    if (m4 == null) {
      m4 = new M4d();
      P3d a1 = P3d.new3(1, 0, 0);
      P3d a2 = P3d.new3(0, 1, 0);
      P3d a3 = P3d.new3(0, 0, 1);

      symmetry.toFractional(a1, true);
      symmetry.toFractional(a2, true);
      symmetry.toFractional(a3, true);
      double d = a1.length();
      // TODO: what about cases where we have factor of two in cell?
      // "hexagonal", just expanded x, not x and y.
      a1.normalize();
      a2.scale(1 / d);
      a3.scale(1 / d);
      m4.setColumn4(0, a1.x, a1.y, a1.z, 0);
      m4.setColumn4(1, a2.x, a2.y, a2.z, 0);
      m4.setColumn4(2, a3.x, a3.y, a3.z, 0);
      m4.transpose();
    } else {
      m4.invert();
    }
    return SymmetryOperation.getXYZFromMatrixFrac(m4, false, false, false, true,
        true, SymmetryOperation.MODE_ABC);
  }

}
