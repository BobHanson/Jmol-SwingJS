/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-30 10:16:53 -0500 (Sat, 30 Sep 2006) $
 * $Revision: 5778 $
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
package org.jmol.adapter.readers.xtal;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.Bond;
import org.jmol.util.Logger;

import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.V3;

/**
 * A reader for TOPOS systre file Crystal Graph Data format.
 * 
 * http://www.topos.samsu.ru/manuals.html
 * 
 * http://gavrog.org/Systre-Help.html#file_formats
 * 
 * H-M aliases from gavrov distribution geometry/sgtable.data
 * 
 */

public class CgdReader extends AtomSetCollectionReader {

  private boolean noBondSym;

  @Override
  public void initializeReader() {
    setFractionalCoordinates(true);
    asc.setNoAutoBond();
    asc.vibScale = 1;
    forceSymmetry(!noPack); // standard here is to pack unit cell
    noBondSym = checkFilterKey("NOBONDSYM"); // do not apply bond symmetry (just display what is in the file)
  }

  private String[] tokens;
  private Map<Atom, V3[]> htEdges;
  private String lastName;
  private Lst<String> edgeData;

  @Override
  protected boolean checkLine() throws Exception {
    line = line.trim();
    if (line.length() == 0 || line.startsWith("#"))
      return true;
    if (!Character.isLetter(line.charAt(0)))
      line = lastName + " " + line;
    tokens = getTokens();
    if (tokens.length > 0) {
      lastName = tokens[0].toUpperCase();
      int pt = "NAME |CELL |GROUP|ATOM |EDGE |".indexOf(lastName);
      if (tokens.length > 1 && (pt == 0 || doProcessLines))
        switch (pt) {
        //       0     6     12    18    24
        case 0:
          if (!doGetModel(++modelNumber, null))
            return checkLastModel();
          applySymmetryAndSetTrajectory();
          setFractionalCoordinates(true);
          asc.newAtomSet();
          asc.setAtomSetName(line.substring(6).trim());
          htEdges = null;
          edgeData = null;
          break;
        case 6:
          //cell 1.1548 1.1548 1.1548 90.000 90.000 90.000
          for (int i = 0; i < 6; i++)
            setUnitCellItem(i, (i < 3 ? 10 : 1) * parseFloatStr(tokens[i + 1]));
          break;
        case 12:
          setSpaceGroupName("bilbao:" + group(tokens[1]));
          break;
        case 18:
          atom();
          break;
        case 24:
          if (!doApplySymmetry)
            break;
          if (edgeData == null)
            edgeData = new Lst<String>();
          edgeData.addLast(line);
          break;
        }
    }
    return true;
  }

  private final static String SG_ALIASES = ";P2=P121;P21=P1211;C2=C121;A2=A121;I2=I121;Pm=P1m1;Pc=P1c1;Pn=P1n1;Pa=P1a1;Cm=C1m1;Am=A1m1;Im=I1m1;Cc=C1c1;An=A1n1;Ia=I1a1;Aa=A1a1;Cn=C1n1;Ic=I1c1;P2/m=P12/m1;P21/m=P121/m1;C2/m=C12/m1;A2/m=A12/m1;I2/m=I12/m1;P2/c=P12/c1;P2/n=P12/n1;P2/a=P12/a1;P21/c=P121/c1;P21/n=P121/n1;P21/a=P121/a1;C2/c=C12/c1;A2/n=A12/n1;I2/a=I12/a1;A2/a=A12/a1;C2/n=C12/n1;I2/c=I12/c1;Pm3=Pm-3;Pn3=Pn-3;Fm3=Fm-3;Fd3=Fd-3;Im3=Im-3;Pa3=Pa-3;Ia3=Ia-3;Pm3m=Pm-3m;Pn3n=Pn-3n;Pm3n=Pm-3n;Pn3m=Pn-3m;Fm3m=Fm-3m;Fm3c=Fm-3c;Fd3m=Fd-3m;Fd3c=Fd-3c;Im3m=Im-3m;Ia3d=Ia-3d;";

  private String group(String name) {
    String name0 = null;
    if (name.charAt(0) == '"')
      name = name.substring(1, name.length() - 1);
    int pt = SG_ALIASES.indexOf(";" + name + "=");
    if (pt >= 0) {
      name0 = name;
      name = SG_ALIASES.substring(SG_ALIASES.indexOf("=", pt) + 1,
          SG_ALIASES.indexOf(";", pt + 1));
    }
    Logger.info("CgdReader using GROUP " + name
        + (name0 == null ? "" : " alias of " + name0));
    return name;
  }

  private void atom() {

    String name = getName(tokens[1]);
    // check for  ATOM "SI1" 4 0.3112 0.2500 0.3727
    int edgeCount = parseIntStr(tokens[2]);
    // check for  ATOM  1  4   5/8 5/8 5/8
    for (int i = 3; i < 6; i++)
      if (tokens[i].indexOf("/") >= 0)
        tokens[i] = "" + PT.parseFloatFraction(tokens[i]);
    Atom a = addAtomXYZSymName(tokens, 3, null, name);
    if (!doApplySymmetry)
      return;
    asc.atomSymbolicMap.put(name, a);
    asc.addVibrationVector(a.index, 1f, 3f, 7f);
    if (htEdges == null)
      htEdges = new Hashtable<Atom, V3[]>();
    htEdges.put(a, new V3[edgeCount]);
  }

  private String getName(String name) {
    return (name.charAt(0) == '"' ? name.substring(1, name.length() - 1)
        : Character.isDigit(name.charAt(0)) ? "C" + name : name);
  }

  @Override
  public void finalizeSubclassReader() throws Exception {
    finalizeReaderASCR();
    if (doApplySymmetry)
      finalizeNet();
  }

  /**
   * Now that we have all the edge data we can add edges to atoms
   */
  private void finalizeEdges() {
    P3 p;
    String name;
    Atom a;
    V3[] atomEdges;
    for (int j = 0; j < edgeData.size(); j++) {
      tokens = PT.getTokens(line = edgeData.get(j));
      switch (tokens.length) {
      case 3:
        //EDGE 2 3
        name = getName(tokens[1]);
        a = asc.getAtomFromName(name);
        atomEdges = htEdges.get(a);
        p = asc.getAtomFromName(getName(tokens[2]));
        break;
      case 5:
        //EDGE 2 -2/2 2/2 0.5
        name = getName(tokens[1]);
        a = asc.getAtomFromName(name);
        atomEdges = htEdges.get(a);
        p = getCoord(2);
        break;
      case 7:
        //EDGE 0 0 0 -2/2 2/2 0.5
        atomEdges = htEdges.get(findAtom(getCoord(1)));
        p = getCoord(4);
        break;
      default:
        Logger.error("EDGE record skipped: " + line);
        continue;
      }
      for (int i = 0, n = atomEdges.length; i < n; i++)
        if (atomEdges[i] == null) {
          atomEdges[i] = V3.newV(p);
          break;
        }
    }
  }

  private P3 getCoord(int i) {
    return P3.new3(PT.parseFloatFraction(tokens[i++]), PT.parseFloatFraction(tokens[i++]),
        PT.parseFloatFraction(tokens[i++]));
  }

  private final static V3[] vecs = new V3[] { V3.new3(0, 0, -1), // -z   -7
      V3.new3(1, 0, -1), //  x-z -6
      null, V3.new3(0, 1, -1), //  y-z -4
      V3.new3(0, -1, 0), // -y   -3
      V3.new3(1, -1, 0), //  x-y -2
      V3.new3(-1, 0, 0), // -x   -1
      null, V3.new3(1, 0, 0), //  x    1
      V3.new3(-1, 1, 0), //  y-x  2    
      V3.new3(0, 1, 0), //  y    3
      V3.new3(0, -1, 1), //  z-y  4
      null, V3.new3(-1, 0, 1), //  z-x  6
      V3.new3(0, 0, 1) //  z    7
  };

  // a.vib holds {1 3 7}, corresponding to 
  // x, y, and z and allowing for 
  // x-y (-2) and y-x (2)
  // x-z (-6) and z-x (6)
  // y-z (-4) and z-y (4)

  /**
   * Using atom.vib as a proxy indicating rotation,
   * make all the bonds indicated in the atom's htEdges 
   */
  private void finalizeNet() {
    finalizeEdges();
    // atom vibration vector has been rotated and now gives us the needed orientations for the edges. 
    // could be a bit slow without partition. Let's see...
    M3 m = new M3();
    P3 pt = new P3();
    for (int i = 0, n = asc.ac; i < n; i++) {
      Atom a = asc.atoms[i];
      Atom a0 = asc.atoms[a.atomSite];
      if (noBondSym && a != a0)
        continue;
      V3[] edges = htEdges.get(a0);
      if (edges == null)
        continue;
      int ix = (int) a.vib.x + 7;
      int iy = (int) a.vib.y + 7;
      int iz = (int) a.vib.z + 7;
      // ix, iy, iz now range from 0 to 13
      m.setRowV(0, vecs[ix]);
      m.setRowV(1, vecs[iy]);
      m.setRowV(2, vecs[iz]);
      for (int j = 0, n1 = edges.length; j < n1; j++) {
        pt.sub2(edges[j], a0);
        m.rotate(pt);
        pt.add(a);
        Atom b = findAtom(pt);
        if (b != null)
          asc.addBond(new Bond(a.index, b.index, 1));
        else if (pt.x >= 0 && pt.x <= 1 && pt.y >= 0 && pt.y <= 1 && pt.z >= 0
            && pt.z <= 1)
          Logger.error(" not found: i=" + i + "  pt=" + pt + " for a=" + a
              + "\n a0=" + a0 + " edge[" + j + "]=" + edges[j] + "\n a.vib="
              + a.vib + "\n m=" + m);
      }
      a.vib = null;
    }

  }

  private Atom findAtom(P3 pt) {
    for (int i = asc.ac; --i >= 0;) 
      if (asc.atoms[i].distanceSquared(pt) < 0.00001f)
        return asc.atoms[i];
    return null;
  }

}
