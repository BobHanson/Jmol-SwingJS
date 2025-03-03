/*
 * Copyright 2006-2011 Sam Adams <sea36 at users.sourceforge.net>
 *
 * This file is part of -InChI.
 *
 * -InChI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * -InChI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with -InChI.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jmol.inchi;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.iupac.InChIStructureProvider;
import org.iupac.InchiUtils;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.util.Edge;
import org.jmol.viewer.Viewer;

import com.sun.jna.Native;

import io.github.dan2097.jnainchi.InchiAtom;
import io.github.dan2097.jnainchi.InchiBond;
import io.github.dan2097.jnainchi.InchiBondStereo;
import io.github.dan2097.jnainchi.InchiBondType;
import io.github.dan2097.jnainchi.InchiFlag;
import io.github.dan2097.jnainchi.InchiInput;
import io.github.dan2097.jnainchi.InchiOptions;
import io.github.dan2097.jnainchi.InchiOptions.InchiOptionsBuilder;
import io.github.dan2097.jnainchi.inchi.InchiLibrary;
import io.github.dan2097.jnainchi.InchiOutput;
import io.github.dan2097.jnainchi.InchiStereo;
import io.github.dan2097.jnainchi.JnaInchi;
import javajs.util.BS;

/**
 * Interface with inchi.c via JNA (see JNA-InChI github project). 
 * 
 * For InChI to SMILES, we use JNA-InChI to read InChI's input structure, 
 * via -InChI.
 * 
 * 
 */
public class InChIJNA extends InchiJmol {

  private InchiInput inchiModel;

  @Override
  public String getInchi(Viewer vwr, BS atoms, Object molData, String options) {
    if ("version".equals(options))
  	  return getInternalInchiVersion(); 
    try {
      options = setParameters(options, molData, atoms, vwr);
      if (options == null)
        return "";
      InchiOptions ops = getOptions(options.toLowerCase());
      InchiOutput out = null;
      InchiInput in;
      if (inputInChI) {
          out = JnaInchi.inchiToInchi((String) molData, ops);
      } else if (!optionalFixedH) {
        if (atoms == null) {
          out = JnaInchi.molToInchi((String) molData, ops);
        } else {
          in = newInchiStructureBS(vwr, atoms);
          out = JnaInchi.toInchi(in, getOptions(doGetSmiles ? "fixedh" : options));
        }
      }
      if (out != null) {
        String msg = out.getMessage();
        if (msg != null)
          System.err.println(msg);
        inchi = out.getInchi();
      }
      if (doGetSmiles || getInchiModel) {
        // "getStructure" is just a debugging method 
        // to see the exposed InChI structure in string form
        // note this is the INPUT model
        inchiModel = JnaInchi.getInchiInputFromInchi(inchi).getInchiInput();
        return (doGetSmiles ? getSmiles(vwr, smilesOptions)
            : toJSON(inchiModel));
      }
      return (getKey ? JnaInchi.inchiToInchiKey(inchi).getInchiKey() : inchi);
    } catch (Throwable e) {
      System.out.println(e);
      if (e.getMessage() != null && e.getMessage().indexOf("ption") >= 0)
        System.out.println(e.getMessage() + ": " + options.toLowerCase()
            + "\n See https://www.inchi-trust.org/download/104/inchi-faq.pdf for valid options");
      else
        e.printStackTrace();
      return "";
    }
  }

  private static InchiOptions getOptions(String options) {
    InchiOptionsBuilder builder = new InchiOptionsBuilder();
    StringTokenizer t = new StringTokenizer(options);
    while (t.hasMoreElements()) {
      String o = t.nextToken();
      switch (o) {
      case "smiles":
      case "amide":
      case "imine":
        continue;
      }
      InchiFlag f = InchiFlag.getFlagFromName(o);
      if (f == null) {
        System.err.println("InChIJNA InChI option " + o + " not recognized -- ignored");
      } else {
        builder.withFlag(f);
      }
    }
    return builder.build();
  }

  /**
   * Jmol addition to create a InchiStructure from Jmol atoms. Currently only
   * supports single, double, aromatic_single and aromatic_double.
   * 
   * @param vwr
   * @param bsAtoms
   * @return a structure for Input
   */
  private static InchiInput newInchiStructureBS(Viewer vwr,
                                                        BS bsAtoms) {
    InchiInput mol = new InchiInput();
    InchiAtom[] atoms = new InchiAtom[bsAtoms.cardinality()];
    int[] map = new int[bsAtoms.length()];
    BS bsBonds = vwr.ms.getBondsForSelectedAtoms(bsAtoms, false);
    for (int pt = 0, i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms
        .nextSetBit(i + 1)) {
      Atom a = vwr.ms.at[i];
      String sym = a.getElementSymbolIso(false);
      int iso = a.getIsotopeNumber();
      if (a.getElementNumber() == 1) {
        sym = "H"; // in case this is D
      }
      mol.addAtom(atoms[pt] = new InchiAtom(sym, a.x, a.y, a.z));
      atoms[pt].setCharge(a.getFormalCharge());
      if (iso > 0)
        atoms[pt].setIsotopicMass(iso);
      map[i] = pt++;
    }
    Bond[] bonds = vwr.ms.bo;
    for (int i = bsBonds.nextSetBit(0); i >= 0; i = bsBonds.nextSetBit(i + 1)) {
      Bond bond = bonds[i];
      if (bond == null)
        continue;
      // partial bonds (as in ferrocene) become at least 1
      // but oddly still do not get generated, probably because the carbons are then too strange?
      InchiBondType order = getOrder(Math.max(bond.isPartial() ? 1 : 0, bond.getCovalentOrder()));
      if (order != null) {
        int atom1 = bond.getAtomIndex1();
        int atom2 = bond.getAtomIndex2();
        InchiBondStereo stereo = getInChIStereo(bond.getBondType());
        mol.addBond(new InchiBond(atoms[map[atom1]],
            atoms[map[atom2]], order, stereo));
      }
    }
    return mol;
  }

  private static InchiBondStereo getInChIStereo(int jmolOrder) {
    switch (jmolOrder) {
    case Edge.BOND_STEREO_FAR:
      return InchiBondStereo.SINGLE_1DOWN;
    case Edge.BOND_STEREO_NEAR:
      return InchiBondStereo.SINGLE_1UP;
    case Edge.BOND_STEREO_EITHER:
      // this will generate a stereo ?
      return InchiBondStereo.SINGLE_1EITHER;
    default:
      return InchiBondStereo.NONE;
    }
  }

  private static InchiBondType getOrder(int jmolOrder) {
    switch (jmolOrder) {
    case Edge.BOND_STEREO_EITHER:
    case Edge.BOND_STEREO_FAR:
    case Edge.BOND_STEREO_NEAR:
    case Edge.BOND_COVALENT_SINGLE:
      return InchiBondType.SINGLE;
    case Edge.BOND_COVALENT_DOUBLE:
      return InchiBondType.DOUBLE;
    case Edge.BOND_COVALENT_TRIPLE:
      return InchiBondType.TRIPLE;
    default:
      return null;
    }
  }

  @SuppressWarnings("boxing")
  private static String toJSON(InchiInput mol) {
    int na = sizeof(mol.getAtoms());
    int nb = sizeof(mol.getBonds());
    int ns = sizeof(mol.getStereos());
    Map<InchiAtom, Integer> mapAtoms = new HashMap<>();
    boolean haveXYZ = false;
    for (int i = 0; i < na; i++) {
      InchiAtom a = mol.getAtom(i);
      if (a.getX() != 0 || a.getY() != 0 || a.getZ() != 0) {
        haveXYZ = true;
        break;
      }
    }
    String s = "{";
    s += "\n\"atomCount\":" + na + ",\n";
    s += "\"atoms\":[\n";
    for (int i = 0; i < na; i++) {
      InchiAtom a = mol.getAtom(i);
      mapAtoms.put(a, Integer.valueOf(i));
      if (i > 0)
        s += ",\n";
      s += "{";
      s += toJSONInt("index", Integer.valueOf(i), "");
      s += toJSONNotNone("elname", a.getElName(), ",");
      if (haveXYZ) {
        s += toJSONDouble("x", a.getX(), ",");
        s += toJSONDouble("y", a.getY(), ",");
        s += toJSONDouble("z", a.getZ(), ",");
      }
      s += toJSONNonZero("isotopeMass", a.getIsotopicMass(), ",");
      s += toJSONNonZero("charge", a.getCharge(), ",");
      s += toJSONNotNone("radical", a.getRadical(), ",");
      if (a.getImplicitHydrogen() > 0)
        s += toJSONNonZero("implicitH", a.getImplicitHydrogen(), ",");
      s += toJSONNonZero("implicitDeuterium", a.getImplicitDeuterium(), ",");
      s += toJSONNonZero("implicitProtium", a.getImplicitProtium(), ",");
      s += toJSONNonZero("implicitTritium", a.getImplicitTritium(), ",");
      s += "}";
    }
    s += "\n],";
    s += "\n\"bondCount\":" + nb + ",\n";
    s += "\n\"bonds\":[\n";

    for (int i = 0; i < nb; i++) {
      if (i > 0)
        s += ",\n";
      s += "{";
      InchiBond b = mol.getBond(i);
      s += toJSONInt("originAtom", mapAtoms.get(b.getStart()).intValue(),
          "");
      s += toJSONInt("targetAtom", mapAtoms.get(b.getEnd()).intValue(),
          ",");
      String bt = uc(b.getType());
      if (!bt.equals("SINGLE"))
        s += toJSONString("type", bt, ",");
      s += toJSONNotNone("stereo", uc(b.getStereo()), ",");
      s += "}";
    }
    s += "\n]";
    if (ns > 0) {
      s += ",\n\"stereoCount\":" + ns + ",\n";
      s += "\"stereo\":[\n";
      for (int i = 0; i < ns; i++) {
        if (i > 0)
          s += ",\n";
        s += "{";
        InchiStereo d = mol.getStereos().get(i);
        s += toJSONNotNone("type", uc(d.getType()), "");
        s += toJSONNotNone("parity", uc(d.getParity()), ",");
        InchiAtom[] an = d.getAtoms();
        int[] nbs = new int[an.length];
        for (int j = 0; j < an.length; j++) {
          nbs[j] = mapAtoms.get(an[j]).intValue();
        }
        s += toJSONArray("neighbors", nbs, ",");
        InchiAtom a = d.getCentralAtom();
        if (a != null)
          s += toJSONInt("centralAtom", mapAtoms.get(a).intValue(), ",");
        s += "}";
      }
      s += "\n]";
    }
    s += "}";
    System.out.println(s);
    return s;
  }

  private static int sizeof(List<?> list) {
    return (list == null ? 0 : list.size());
  }

  private static String toJSONArray(String key, int[] val, String term) {
    String s = term  + "\"" + key + "\":[" + val[0];
    for (int i = 1; i < val.length; i++) {
      s += "," + val[i];
    }
    return s + "]";
  }

  private static String toJSONNonZero(String key, int val, String term) {
    return (val == 0 ? "" : toJSONInt(key, val, term));
  }

  private static String toJSONInt(String key, int val, String term) {
    return term + "\"" + key + "\":" + val;
  }

  private static String toJSONDouble(String key, double val, String term) {
    String s;
    if (val == 0) {
      s = "0";
    } else {
      s = "" + (val + (val > 0 ? 0.00000001 : -0.00000001));
      s = s.substring(0, s.indexOf(".") + 5);
      int n = s.length();
      while (s.charAt(--n) == '0') {
      }
      s = s.substring(0, n + 1);
    }
    return term + "\"" + key + "\":" + s;
  }

  private static String toJSONString(String key, String val, String term) {
    return term + "\"" + key + "\":\"" + val + "\"";
  }
  private static String toJSONNotNone(String key, Object val, String term) {
    String s = val.toString();
    return ("NONE".equals(s) ? "" : term + "\"" + key + "\":\"" + s + "\"");
  }

  private Map<InchiAtom, Integer> map = new Hashtable<InchiAtom, Integer>();
  
  private InchiAtom thisAtom;
  private InchiBond thisBond;
  private InchiStereo thisStereo;

  @Override
  public void initializeModelForSmiles() {
    for (int i = getNumAtoms(); --i >= 0;) 
      map.put(inchiModel.getAtom(i), Integer.valueOf(i));
  }

  /// atoms ///
  
  @Override
  public int getNumAtoms() {
    return sizeof(inchiModel.getAtoms());
  }

  
  @Override
  public InChIStructureProvider setAtom(int i) {
    thisAtom = inchiModel.getAtom(i);
    return this;
  }

  @Override
  public String getElementType() {
    return thisAtom.getElName();
  }

  @Override
  public double getX() {
    return thisAtom.getX();
  }

  @Override
  public double getY() {
    return thisAtom.getY();
  }

  @Override
  public double getZ() {
    return thisAtom.getZ();
  }

  @Override
  public int getCharge() {
    return thisAtom.getCharge();
  }

  @Override
  public int getIsotopicMass() {
    return InchiUtils.getActualMass(getElementType(), thisAtom.getIsotopicMass());
  }

  @Override
  public int getImplicitH() {
    return thisAtom.getImplicitHydrogen();
  }

  
  /// bonds ///
  
  @Override
  public int getNumBonds() {
    return sizeof(inchiModel.getBonds());
  }

  @Override
  public InChIStructureProvider setBond(int i) {
    thisBond = inchiModel.getBond(i);
    return this;
  }


  @Override
  public int getIndexOriginAtom() {
    return map.get(thisBond.getStart()).intValue();
  }

  @Override
  public int getIndexTargetAtom() {
    return map.get(thisBond.getEnd()).intValue();
  }

  @Override
  public String getInchiBondType() {
    InchiBondType type = thisBond.getType();
    return type.name();
  }

  /// Stereo ///
  
  @Override
  public int getNumStereo0D() {
    return sizeof(inchiModel.getStereos());
  }

  @Override
  public InChIStructureProvider setStereo0D(int i) {
    thisStereo = inchiModel.getStereos().get(i);
    return this;
  }
    
  @Override
  public int[] getNeighbors() {
    InchiAtom[] an = thisStereo.getAtoms();
    
    int n = an.length;
    int[] a = new int[n];
    
    //add for loop
    for(int i = 0; i < n; i++) {
      a[i] = map.get(an[i]).intValue();
    }
    return a;
  }

  @Override
  public int getCenterAtom() {
    InchiAtom ca = thisStereo.getCentralAtom();
    return (ca == null? -1: map.get(ca).intValue());
  }

  @Override
  public String getStereoType() {
    return uc(thisStereo.getType());
  }

  @Override
  public String getParity() {
    return uc(thisStereo.getParity());
  }
  
  private static String uc(Object o) {
    return o.toString().toUpperCase();
  }


  private static String inchiVersionInternal;

  /**
   * Get the InChI version directly from the inchi code without an API.
   * To be replaced in the future with a simple inchi IXA call?
   * 
   * Future format may change.
   * 
   * @return something like "InChI version 1, Software 1.07.2 (API Library)"
   */
  public static String getInternalInchiVersion() {
    if (inchiVersionInternal == null) {
      File f = InchiLibrary.JNA_NATIVE_LIB.getFile();
      inchiVersionInternal = extractInchiVersionInternal(f);
      if (inchiVersionInternal == null) {
        // linux will be here after Native libary deletes the file
        try {
          // that's OK; we can get it ourselves
          f = Native.extractFromResourcePath(InchiLibrary.JNA_NATIVE_LIB.getName());
          inchiVersionInternal = extractInchiVersionInternal(f);
        } catch (Exception e) {
        }
      }
      if (inchiVersionInternal == null)
        inchiVersionInternal = "unknown";
    }
    return inchiVersionInternal;
  }

  private static String extractInchiVersionInternal(File f) {
    String s = null;
    try (FileInputStream fis = new FileInputStream(f)) {
      byte[] b = new byte[(int) f.length()];
      fis.read(b);
      s = new String(b);
      int pt = s.indexOf("InChI version");
      if (pt < 0) {
        s = null;
      } else {
        s = s.substring(pt, s.indexOf('\0', pt));
      }
      fis.close();
      f.delete();
   } catch (Exception e) {
      //System.out.println(f);
      //e.printStackTrace();
      // it's gone already in Linux
    }
    return s;
  }


}
