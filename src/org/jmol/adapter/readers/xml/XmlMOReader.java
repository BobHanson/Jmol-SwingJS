package org.jmol.adapter.readers.xml;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;

import org.jmol.adapter.readers.quantum.MOReader;
import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.Resolver;
import org.jmol.quantum.QS;
import org.jmol.util.Logger;

/**
 * An abstract class accessing MOReader -- currently just for XmlMolproReader only.
 * Several assumptions here.
 * 
 * @author hansonr  Bob Hanson hansonr@stolaf.edu
 */
public abstract class XmlMOReader extends XmlCmlReader {

  private MOReader moReader;
  private boolean skipMOs;
  private Map<String, int[]> htSlaterIDs;
  private Lst<double[]> basisData;
  private String basisId;
  private boolean isSpherical;
  private int minL, maxL;
  private String[] basisIds, basisAtoms;
  private double orbOcc, orbEnergy;
  private int gaussianCount, slaterCount, coefCount, groupCount;
  private Lst<Lst<double[]>> lstGaussians;
  private int moCount;
  private String calcType;
  private int iModelMO;

  protected String dclist, dslist, fclist, fslist;
  protected boolean iHaveCoefMaps;
  private int maxContraction;
  
  @Override
  protected void processXml(XmlReader parent,
                            Object saxReader) throws Exception {
    htModelAtomMap = new Hashtable<String, Object>();
    processXml2(parent, saxReader);
  }

  protected boolean processStartMO(String localName) {
    if (!parent.doReadMolecularOrbitals)
      return false;
    if (localName.equals("molecule")) {
      String method = atts.get("method");
      if (method != null)
        calcType = method + "(" + atts.get("basis") + ")";
      return true;
    }
    if (localName.equals("basisset")) {
      iModelMO = asc.iSet;
      lstGaussians = new Lst<Lst<double[]>>();
      htSlaterIDs = new Hashtable<String, int[]>();
      coefCount = groupCount = gaussianCount = slaterCount = 0;
      if (moReader == null && !skipMOs) {
        Object rdr = Resolver.getReader("MO", parent.htParams);
        if ((rdr instanceof String)) {
          skipMOs = true;
        } else {
          moReader = (MOReader) rdr;
          moReader.asc = asc;
          if (iHaveCoefMaps) {
            int[][] m = moReader.getDfCoefMaps();
            if (dclist != null)
              QS.createDFMap(m[QS.DC], dclist, QS.CANONICAL_DC_LIST, 2);
            if (dslist != null)
              QS.createDFMap(m[QS.DS], dslist, QS.CANONICAL_DS_LIST, 2);
            if (fclist != null)
              QS.createDFMap(m[QS.FC], fclist, QS.CANONICAL_FC_LIST, 2);
            if (fslist != null)
              QS.createDFMap(m[QS.FS], fslist, QS.CANONICAL_FS_LIST, 2);            
          }
        }
      }
      if (moReader != null)
        moReader.calculationType = calcType;
      return true;
    }
    if (moReader != null) {
      if (localName.equals("basisgroup")) {
        groupCount++;
        basisId = atts.get("id");
        isSpherical = "spherical".equalsIgnoreCase(atts.get("angular"));
        minL = PT.parseInt(atts.get("minl"));
        maxL = PT.parseInt(atts.get("maxl"));
        int nContractions = PT.parseInt(atts.get("contractions"));
        // 2x + 1 = x(x+3)/2 +1
        // 4x = x(x+3)
        // [x = 0, 1]
        int n = nContractions *(isSpherical ? minL * 2 + 1 : minL * (minL + 3) / 2 + 1);
        htModelAtomMap.put(basisId+"_count", Integer.valueOf(n)); // 6 10 14 
        return true;
      }
      if (localName.equals("basisexponents")
          || localName.equals("basiscontraction")) {
        setKeepChars(true);
        return true;
      }
      if (localName.equals("orbital") && gaussianCount > 0) {
        orbOcc = PT.parseDouble(atts.get("occupation"));
        orbEnergy = PT.parseDouble(atts.get("energy"));
        setKeepChars(true);
        return true;
      }
    }
    return false;
  }
  
  @SuppressWarnings("static-access")
  protected boolean processEndMO(String localName) {
    if (moReader != null) {
      if (localName.equals("basisexponents")) {
        basisData = new Lst<double[]>();
        basisData.addLast(PT.parseDoubleArray(chars.toString()));
        setKeepChars(false);
        return true;
      }
      if (localName.equals("basiscontraction")) {
        double[] data = PT.parseDoubleArray(chars.toString());
        basisData.addLast(data);
        if (basisData.size() > maxContraction)
          maxContraction = basisData.size();
        setKeepChars(false);
        return true;
      }
      if (localName.equals("basisgroup")) {
        String otype;
        switch (minL) {
        case 0:
          otype = (maxL == 1 ? "L" : "S");
          break;
        case 1:
          otype = "P";
          break;
        default:
          otype = (minL <= 7 ? "SPDFGHI".substring(minL, minL + 1) : "?");
          if (isSpherical)
            otype = (2 * (minL) + 1) + otype;
        }
        lstGaussians.addLast(basisData);
        int nPrimitives = basisData.get(0).length;
        for (int i = 1, n = basisData.size(); i < n; i++) {
          htSlaterIDs.put(basisId + "_" + i,
              new int[] { -1, moReader.getQuantumShellTagID(otype),
                  gaussianCount + 1, nPrimitives });
          gaussianCount += nPrimitives;
        }
        return true;
      }
      if (localName.equals("basisset")) {
        buildSlaters();
        return true;
      }
      if (localName.equals("orbital")) {
        if (gaussianCount == 0)
          return true;
        double[] coef = PT.parseDoubleArray(chars.toString());
        if (moCount == 0) {
          if (coef.length != coefCount) {
            Logger.error("Number of orbital coefficients (" + coef.length
                + ") does not agree with expected number (" + coefCount + ")");
            moReader = null;
            return skipMOs = true;
          }
          Logger.info(coefCount + " coefficients found");
        }
        moReader.addCoef(new Hashtable<String, Object>(), coef, null,
            orbEnergy, orbOcc, moCount++);
        setKeepChars(false);
        return true;
      }
      if (localName.equals("orbitals")) {
        moReader.setMOData(true);
        Logger.info("XmlMOReader created\n " + gaussianCount + " gaussians\n "
            + slaterCount + " slaters\n " + groupCount + " groups\n " + coefCount + " orbital coefficients\n " + moCount + " orbitals");
        return true;
      }
      if (state == ASSOCIATION) {
        if (localName.equals("bases")) {
          basisIds = getXlink(atts.get("href"), "basisGroup", false);
        } else if (localName.equals("atoms")) {
          basisAtoms = getXlink(atts.get("href"), "atom", true);
        } else if (localName.equals("association")) {
          state = MOLECULE;
          for (int i = basisAtoms.length; --i >= 0;) {
            Atom a = (Atom) htModelAtomMap.get(basisAtoms[i]);
            if (a == null) {
              Logger.error("XmlMOReader atom not found; orbitals skipped: "
                  + a);
              moReader = null;
              return skipMOs = true;
            }
            htModelAtomMap.put(basisAtoms[i] + "_basis", basisIds);
          }
          slaterCount += basisIds.length * basisAtoms.length;
        }
        return true;
      }
    }
    return false;
  }

  private void buildSlaters() {
    double[][] gaussians = AU.newDouble2(gaussianCount);
    for (int i = 0, p = 0, n = lstGaussians.size(); i < n; i++) {
      basisData = lstGaussians.get(i);
      double[] exp = basisData.get(0);
      for (int ii = 1, nn = basisData.size(); ii < nn; ii++) {
        double[] coef = basisData.get(ii);
        for (int j = 0; j < exp.length; j++)
          gaussians[p++] = new double[] { (double) exp[j], (double) coef[j], 0 };
      }
    }
    moReader.gaussians = gaussians;
    Lst<int[]> slaters = new Lst<int[]>();
    String modelID = (String) htModelAtomMap.get("" + iModelMO);
    int i0 = asc.getAtomSetAtomIndex(iModelMO);
    for (int i = 0, n = asc.getAtomSetAtomCount(iModelMO); i < n; i++) {
      String[] ids = (String[]) htModelAtomMap.get(modelID
          + asc.atoms[i0 + i].atomName + "_basis");
      if (ids == null)
        continue;
      for (int k = 0; k < ids.length; k++) {
        String key = ids[k]+"_count";
        coefCount += ((Integer) htModelAtomMap.get(key)).intValue();
        for (int kk = 1; kk < maxContraction; kk++) {
          int[] slater = htSlaterIDs.get(ids[k] + "_" + kk);
          if (slater == null)
            break;
          slater = AU.arrayCopyI(slater, -1);
          moReader.shells = slaters;
          slater[0] = i + 1;
          slaters.addLast(slater);
        }
      }
    }
  }

  private String[] getXlink(String href, String key, boolean addMoleculeID) {
    // xlink:href="#xpointer(//molecule[@id='M1745BD4']//basisGroup[@id='1' or @id='2' or @id='3' or @id='4' or @id='5' or @id='6' or @id='7' or @id='8' or @id='9' or @id='10' or @id='11'])"/>
    int p = href.indexOf(key + "[") + 1;
    String[] tokens = PT.split(href.substring(p), "'");
    String[] data = new String[tokens.length / 2];
    String molID = (addMoleculeID ? PT.getQuotedAttribute(href.substring(0, p).replace('\'','"'), "molecule[@id") : "");
    for (int i = 1, pt = 0; i < tokens.length; i += 2)
      data[pt++] = molID + tokens[i]; // "a32" --> "32"
    return data;
  }

}
