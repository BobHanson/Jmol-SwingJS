/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-22 14:12:46 -0500 (Sun, 22 Oct 2006) $
 * $Revision: 5999 $
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

package org.jmol.adapter.readers.molxyz;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.PT;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollectionReader;

/**
 * A reader for Accelrys V3000 files.
 * <p>
 * <a href='http://www.mdli.com/downloads/public/ctfile/ctfile.jsp'>
 * http://www.mdli.com/downloads/public/ctfile/ctfile.jsp </a>
 * <p>
 */
public class V3000Rdr {
  private MolReader mr;
  private String line;

  public V3000Rdr() {
    // for reflection
  }
  
  V3000Rdr set(AtomSetCollectionReader mr) {
    this.mr = (MolReader) mr;
    return this;
  }

  void readAtomsAndBonds(String[] tokens) throws Exception {
    int ac = mr.parseIntStr(tokens[3]);
    readAtoms(ac);
    mr.asc.setModelInfoForSet("dimension", (mr.is2D ? "2D" : "3D"), mr.asc.iSet);

    readBonds(mr.parseIntStr(tokens[4]));    
    readUserData(ac);
  }

  // 0         1         2         3         4         5         6         7
  // 01234567890123456789012345678901234567890123456789012345678901234567890
  // xxxxx.xxxxyyyyy.yyyyzzzzz.zzzz aaaddcccssshhhbbbvvvHHHrrriiimmmnnneee

  private void readAtoms(int ac) throws Exception {
    mr.discardLinesUntilContains("BEGIN ATOM");
    for (int i = 0; i < ac; ++i) {
      rdContinuation();
      String[] tokens = PT.getTokens(line);
      int iAtom = mr.parseIntStr(tokens[2]);
      String elementSymbol = tokens[3];
      if (elementSymbol.equals("*"))
        continue;
      double x = mr.parseDoubleStr(tokens[4]);
      double y = mr.parseDoubleStr(tokens[5]);
      double z = mr.parseDoubleStr(tokens[6]);
      int charge = 0;
      int isotope = 0;
      if (mr.is2D && z != 0)
        mr.is2D = mr.optimize2D = false;

      for (int j = 7; j < tokens.length; j++) {
        String s = tokens[j].toUpperCase();
        if (s.startsWith("CHG="))
          charge = mr.parseIntAt(tokens[j], 4);
        else if (s.startsWith("MASS="))
          isotope = mr.parseIntAt(tokens[j], 5);
      }
      if (isotope > 1 && elementSymbol.equals("H"))
        isotope = 1 - isotope;
      mr.addMolAtom(iAtom, isotope, elementSymbol, charge, x, y, z);
    }
    mr.discardLinesUntilContains("END ATOM");
  }

  private void readBonds(int bondCount) throws Exception {
    mr.discardLinesUntilContains("BEGIN BOND");
    if (bondCount == 0)
      mr.asc.setNoAutoBond();
    for (int i = 0; i < bondCount; ++i) {
      int stereo = 0;
      rdContinuation();
      String[] tokens = PT.getTokens(line);
      int order = mr.parseIntStr(tokens[3]);
      String iAtom1 = tokens[4];
      String iAtom2 = tokens[5];
      String cfg = getField("CFG");
      if (cfg == null) {
        String endpts = getField("ENDPTS");
        if (endpts != null && line.indexOf("ATTACH=ALL") >= 0) {
          // not "ATTACH=ANY"
          tokens = PT.getTokens(endpts);
          int n = mr.parseIntStr(tokens[0]);
          int o = mr.fixOrder(order, 0);
          for (int k = 1; k <= n; k++)
            mr.asc.addNewBondFromNames(iAtom1, tokens[k], o);
        }
      } else {
        stereo = mr.parseIntStr(cfg);
      }
      mr.addMolBond(iAtom1, iAtom2, order, stereo);
    }
    mr.discardLinesUntilContains("END BOND");
  }

  private Map<String, String[]> userData;

  private void readUserData(int ac) throws Exception {
    userData = null;
    String pc = null;
    while (!rd().contains("END CTAB")) {
      if (!line.contains("BEGIN SGROUP"))
        continue;
      String atoms, name, data;
      while (!rdContinuation().contains("END SGROUP")) {
        if (userData == null)
          userData = new Hashtable<String, String[]>();
        if ((atoms = getField("ATOMS")) == null
            || (name = getField("FIELDNAME")) == null
            || (data = getField("FIELDDATA")) == null)
          continue;
        name = name.toLowerCase();
        boolean isPartial = (name.indexOf("partial") >= 0);
        if (isPartial) {
          if (pc == null)
            pc = name;
          else if (!pc.equals(name))
            isPartial = false;
        }
        if (isPartial) {
          Atom[] at = mr.asc.atoms;
          for (int i = mr.asc.getLastAtomSetAtomIndex(), n = mr.asc.ac; i < n; i++)
            at[i].partialCharge = 0;
        }
        String[] a = null;
        double f = 0;
        if (isPartial)
          f = mr.parseDoubleStr(data);
        else if ((a = userData.get(name)) == null)
          userData.put(name, a = new String[ac]);
        try {
          String[] tokens = PT.getTokens(atoms);
          for (int i = tokens.length; --i >= 1;) {
            String atom = tokens[i];
            if (isPartial)
              mr.asc.getAtomFromName(atom).partialCharge = f;
            else
              a[mr.parseIntStr(atom) - 1] = data;
          }
        } catch (Exception e) {
          // ignore
        }
      }
    }
    if (userData == null)
      return;
    for (String key : userData.keySet()) {
      String[] a = userData.get(key);
      double[] f = new double[a.length];
      for (int i = 0; i < a.length; i++)
        f[i] = (a[i] == null ? 0 : mr.parseDoubleStr(a[i]));
      mr.asc.setAtomProperties(key, f, -1, false);
    }
  }

  private String getField(String key) {
    int pt = line.indexOf(key + "=");
    if (pt < 0)
      return null;
    pt += key.length() + 1;
    char term = ' ';
    switch (line.charAt(pt)) {
    case '"':
      term = '"';
      break;
    case '(':
      term = ')';
      break;
    case '+':
      break;
    default:
      pt--;
      break;
    }
    return line.substring(pt + 1, (line + term).indexOf(term, pt + 1));
  }

  private String rd() throws Exception {
    return (line = mr.rd());
  }

  private String rdContinuation() throws Exception {
	    rd();
	    while (line.endsWith("-")) {
	      line = line.substring(0, line.length() - 1) + rd().substring(7);
	    }
	    return line;
  }

}
