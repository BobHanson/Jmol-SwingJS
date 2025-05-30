package org.jmol.adapter.readers.xtal;

import java.util.Map;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.api.JmolAdapter;
import org.jmol.symmetry.SymmetryOperation;
import org.jmol.util.Vibration;

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

  private String[] elementSymbols;

  @Override
  protected void initializeReader() throws Exception {
    super.initializeReader();
    String symbols = getFilterWithCase("elements=");
    if (symbols != null) {
      elementSymbols = PT.split(symbols.replace(',', ' '), " ");
    }
    SB sb = new SB();
    try {
      while (rd() != null)
        sb.append(line);
      processJSON(vwr.parseJSONMap(sb.toString()));
    } catch (Exception e) {
      e.printStackTrace();
    }
    continuing = false;
  }

  private void setElementsFromFileName(String fname) {
    if (fname == null)
      return;
    int pt = fname.lastIndexOf(".");
    if (pt >= 0)
      fname = fname.substring(0, pt);
    fname = fname.substring(fname.lastIndexOf("/") + 1);
    fname = fname.substring(fname.lastIndexOf("\\") + 1);
    for (int i = fname.length(); --i >= 1;) {
      if (fname.charAt(i) <= 'a')
        fname = fname.substring(0, i) + " " + fname.substring(i);
    }
    Lst<String> list = new Lst<>();
    String[] tokens = PT.split(
        PT.rep(PT.replaceAllCharacters(fname, "0123456789-._", " "), "  ", " "),
        " ");
    int nt = 0;
    for (int i = 0; i < tokens.length; i++) {
      String e = tokens[i];
      switch (e.length()) {
      case 2:
        if (e.charAt(1) < 'a')
          continue;
      case 1:
        if (e.charAt(0) < 'A')
          continue;
        int n = JmolAdapter.getElementNumber(e);
        if (n > 0) {
          list.addLast(e);
          nt++;
        }
      }
    }
    if (nt > 0) {
      elementSymbols = new String[nt];
      for (int i = nt; --i >= 0;)
        elementSymbols[i] = list.get(i);
    }
  }

  private void processJSON(Map<String, Object> json) {
    getHeaderInfo(json);
    Lst<Object> info = getList(json, "G0_std_Cell");
    getCellInfo(getListItem(info, 0));
    readAllOperators(getList(json, "G0_std_operations"));
    readAtomsAndMoments(info);
  }

  @SuppressWarnings("unchecked")
  private Lst<Object> getListItem(Lst<Object> info, int i) {
    return (Lst<Object>) info.get(i);
  }

  @SuppressWarnings("unchecked")
  private static Lst<Object> getList(Map<String, Object> json, String key) {
    return (Lst<Object>) json.get(key);
  }

  private void getHeaderInfo(Map<String, Object> json) {
    setSpaceGroupName((String) json.get("FPG_symbol"));
    appendUnitCellInfo(
        "SSG_international_symbol=" + json.get("SSG_international_symbol"));
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
    for (int i = 0, n = info.size(); i < n; i++) {
      Lst<Object> op = (Lst<Object>) info.get(i);
      M4d mspin = readMatrix(getListItem(op, 0), null);
      M4d mop = readMatrix(getListItem(op, 1), getListItem(op, 2));
      String s = SymmetryOperation.getXYZFromMatrixFrac(mop, false, false,
          false, true, true, "xyz")
          + SymmetryOperation.getSpinString(mspin, true);
      System.out.println("FSGOutput op[" + (i + 1) + "]=" + s);
      setSymmetryOperator(s);
    }
  }

  private M4d readMatrix(Lst<Object> rot, Lst<Object> trans) {
    M3d r = new M3d();
    for (int i = 0; i < 3; i++) {
      r.setRowV(i, getPoint(getListItem(rot, i)));
    }
    P3d t = (trans == null ? new P3d() : getPoint(trans));
    return M4d.newMV(r, t);
  }

  private void readAtomsAndMoments(Lst<Object> info) {
    Lst<Object> atoms = getListItem(info, 1);
    Lst<Object> ids = getListItem(info, 2);
    Lst<Object> moments = getListItem(info, 3);
    for (int i = 0, n = atoms.size(); i < n; i++) {
      P3d xyz = getPoint(getListItem(atoms, i));
      int id = (int) getValue(ids, i);
      P3d moment = getPoint(getListItem(moments, i));
      Atom a = asc.addNewAtom();
      a.setT(xyz);  
      setAtomCoord(a);
      if (elementSymbols != null && id <= elementSymbols.length) {
        a.elementSymbol = elementSymbols[id - 1];
      } else {
        a.elementNumber = (short) (id + 2); // start with boron        
      }
      // no element symbols!!
      Vibration v = new Vibration().setType(Vibration.TYPE_SPIN);
      v.set(moment.x, moment.y, moment.z);
      v.magMoment = v.length();
      if (v.magMoment > 0)
        System.out
            .println("FGSOutput moment " + i + " " + v.magMoment + " " + v);
      a.vib = v;
    }
  }
  
  @Override
  protected void finalizeSubclassReader() throws Exception {
      asc.setNoAutoBond();
      applySymmetryAndSetTrajectory();
      addJmolScript("vectors on;vectors 0.15;");
  }



}
