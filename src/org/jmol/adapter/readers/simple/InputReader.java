/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-28 23:13:00 -0500 (Thu, 28 Sep 2006) $
 * $Revision: 5772 $
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

package org.jmol.adapter.readers.simple;

import javajs.util.Lst;
import java.util.Hashtable;

import java.util.Map;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Bond;
import org.jmol.adapter.smarter.Atom;

import org.jmol.api.JmolAdapter;
import org.jmol.util.Logger;

import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.Quat;
import javajs.util.V3;

public class InputReader extends AtomSetCollectionReader {
  /*
   * A simple Z-matrix reader, also serves as simple input file reader for 
   * CFILE, VFILE, PQS, Orca, NWChem, GAMESS, Gaussian, MOPAC, Q-Chem, Jaguar, MolPro, 
   * and ADF, as produced by NBO6Pro
   * 
   * Can be invoked using ZMATRIX::   or with file starting with #ZMATRIX
   * 
   * MOPAC, CFILE ,and VFILE require MND::, CFI:: (or C::), and  VFI:: (or V::) respectively
   * 
   * Other invocations include: ADF::, GAU::(or G::), GMS::, JAG::, MP::, ORC::, NW::, PQS::, QC::,
   * but those are optional. 
   * 
   * # are comments; can include jmolscript: xxxx
   * 
   *  
   *  Any invalid element symbol such as X or XX indicates a dummy
   *  atom that will not be included in the model but is needed
   *  to create the structure
   * 
   * 
   * Bob Hanson hansonr@stolaf.edu 11/19/2011
   */

  // a positive dihedral is defined as being
  //  
  //               (back)
  //         +120 /
  //   (front)---O

  //  Anything after # on a line is considered a comment.
  //  A first line starting with #ZMATRIX defines the file type:
  //    Jmol z-format type (just #ZMATRIX) 
  //    Gaussian (#ZMATRIX GAUSSIAN) 
  //    Mopac (#ZMATRIX MOPAC) 
  //  Lines starting with # may contain jmolscript
  //  Blank lines are ignored:
  //  
  //  #ZMATRIX -- methane
  //  #jmolscript: spin on
  //  
  //  C
  //  H   1 1.089000     
  //  H   1 1.089000  2  109.4710      
  //  H   1 1.089000  2  109.4710  3  120.0000   
  //  H   1 1.089000  2  109.4710  3 -120.0000
  //  
  //  Bonds will not generally be added, leaving Jmol to do its autoBonding.
  //  To add bond orders, just add them as one more integer on any line
  //  other than the first-atom line:
  //  
  //  #ZMATRIX -- CO2 
  //  C
  //  O   1 1.3000                 2     
  //  O   1 1.3000    2  180       2      
  //  
  //  Any position number may be replaced by a unique atom name, with number:
  //  
  //  #ZMATRIX -- CO2
  //  C1
  //  O1   C1 1.3000                2     
  //  O2   C1 1.3000    O1  180     2      
  //  
  //  Ignored dummy atoms are any atoms starting with "X" and a number,
  //  allowing for positioning:
  //  
  //  #ZMATRIX -- CO2
  //  X1
  //  X2   X1 1.0
  //  C1   X1 1.0       X2 90
  //  O1   C1 1.3000    X2 90   X1 0  2     
  //  O2   C1 1.3000    O1 180  X2 0  2      
  //  
  //  Negative distance indicates that the second angle is a normal angle, not a dihedral:
  //  
  //  #ZMATRIX -- NH3 (using simple angles only)
  //  N1 
  //  H1 N1 1.0
  //  H2 N1 1.0 H1 107  
  //  H3 N1 -1.0 H1 107 H2 107
  //  
  //  Negative distance and one negative angle reverses the chirality:
  //  
  //  #ZMATRIX -- NH3 (using simple angles only; reversed chirality):
  //  N1 
  //  H1 N1 1.0
  //  H2 N1 1.0 H1 107  
  //  H3 N1 -1.0 H1 -107 H2 107
  //  
  //  Symbolics may be used -- they may be listed first or last:
  //  
  //  #ZMATRIX
  //  
  //  dist 1.0
  //  angle 107
  //  
  //  N1 
  //  H1 N1 dist
  //  H2 N1 dist H1 angle 
  //  H3 N1 -dist H1 angle H2 angle
  //  
  //  All atoms will end up with numbers in their names, 
  //  but you do not need to include those in the z-matrix file
  //  if atoms have unique elements or you are referring
  //  to the last atom of that type. Still, numbering is recommended.
  //  
  //  #ZMATRIX
  //  
  //  dist 1.0
  //  angle 107
  //  
  //  N                  # will be N1
  //  H N dist           # will be H2
  //  H N dist H angle   # will be H3
  //  H N -dist H2 angle H angle # H here refers to H3
  //  
  //  MOPAC format will have an initial comment section. Isotopes are in the form of "C13". 
  //  If isotopes are used, the file type MUST be identified as #ZMATRIX MOPAC. 
  //  Lines prior to the first line with less than 2 characters will be considered to be comments and ignored.
  //  
  //   AM1
  //  Ethane
  //  
  //  C
  //  C     1     r21
  //  H     2     r32       1     a321
  //  H     2     r32       1     a321      3  d4213
  //  H     2     r32       1     a321      3 -d4213
  //  H     1     r32       2     a321      3   60.
  //  H     1     r32       2     a321      3  180.
  //  H     1     r32       2     a321      3  d300
  //  
  //  r21        1.5
  //  r32        1.1
  //  a321     109.5
  //  d4213    120.0
  //  d300     300.0
  //  
  //  Gaussian will not have the third line blank and has a slightly 
  //  different format for showing the alternative two-angle format
  //  involving the eighth field having the flag 1
  //  (see http://www.gaussian.com/g_tech/g_ur/c_zmat.htm):
  //  
  //  C5 O1 1.0 C2 110.4 C4 105.4 1
  //  C6 O1 R   C2 A1    C3 A2    1
  //  
  //  Note that Gaussian cartesian format is allowed -- simply 
  //  set the first atom index to be 0.
  //  
  //  
  //  No distinction between "Variable:" and "Constant:" is made by Jmol.

  protected int ac;
  protected Lst<Atom> vAtoms = new Lst<Atom>();
  private Map<String, Integer> atomMap = new Hashtable<String, Integer>();
  private String[] tokens;
  private boolean isJmolZformat;
  private Lst<String[]> lineBuffer = new Lst<String[]>();
  private Map<String, Float> symbolicMap = new Hashtable<String, Float>();
  private boolean isMopac;
  private boolean isHeader = true;
  private boolean firstLine = true;

  @Override
  protected boolean checkLine() throws Exception {
    if (firstLine) {
      firstLine = false;
      String[] tokens = getTokens();
      if (tokens.length == 3 && parseIntStr(tokens[0]) > 0 && parseIntStr(tokens[1]) > 0 && parseIntStr(tokens[2]) > 0) {
        // NBO CON file format -- must be explicitly indicated
        readConFile();
        return continuing = false;
      }
    }
    // easiest just to grab all lines that are comments or symbolic first, then do the processing of atoms.
    cleanLine();
    if (line.length() <= 2) // for Mopac, could be blank or an atom symbol, but not an atom name
      isHeader = false;
    if (line.startsWith("#") || line.startsWith("*") || isMopac && isHeader) {
      if (line.startsWith("#ZMATRIX"))
        isJmolZformat = line.toUpperCase().indexOf("GAUSSIAN") < 0
            && !(isMopac = (line.toUpperCase().indexOf("MOPAC") >= 0));
      checkCurrentLineForScript();
      return true;
    }
    if (line.indexOf("#") >= 0)
      line = line.substring(0, line.indexOf("#"));
    if (line.indexOf(":") >= 0)
      return true; // Variables: or Constants:
    if (line.contains("$molecule")) {
      // Q-Chem input 
      rd(); // spin
      return readBlock("$end");
    }
    if (line.startsWith("$"))
      return true; // $NBO
    
    if (line.contains("%mem")) {
      // Gaussian
      discardLinesUntilBlank();
      discardLinesUntilBlank();
      rd(); // spin
      return readBlock(null);
    }
    
    if (line.contains("ATOMS cartesian")) {
      // ADF input 
      return readBlock("END");
    }
    
    if (line.contains("geometry units angstroms")) {
      // NWChem input 
      return readBlock("end");
    }

    if (line.contains("&zmat")) {
      // Jaguar input 
      return readBlock("&");
    }

    if (line.contains("%coords")) {
      // ORCA input 
      discardLinesUntilContains("coords");
      return readBlock("end");
    }
    if (line.contains("GEOM=PQS")) {
      // PQS input
      return readBlock("BASIS");
    }
    if (line.contains("geometry={")) {
      // MolPRO via NBO6
      readLines(2);
      return readBlock("}");
    }

    tokens = getTokens();

    if (tokens.length > 10)
      return readVFI();

    switch (tokens.length) {
    case 1:
      if (tokens[0].indexOf("=") < 0) {
      lineBuffer.clear();
      break;
      }
      tokens = PT.split(tokens[0], "=");
      //$FALL-THROUGH$
    case 2:
      if (parseIntStr(line) > 0 && parseInt() >= 0) {
        // int int --> Gaussian CFILE (via NBO)
        readCFI();
        return (continuing = false);
      }
      getSymbolic();
      return true;
    case 10:
      // MOPAC archive format
      if (tokens[0].equals("0"))
        return (continuing = false);
      if (tokens[1].indexOf(".") < 0)
        return true;
      if (lineBuffer.size() > 0
          && lineBuffer.get(lineBuffer.size() - 1).length < 8)
        lineBuffer.clear();
      break;
    }
    lineBuffer.addLast(tokens);
    return true;
  }

  
  private void readConFile() throws Exception {

//  1   13    1
//  5 CH4
//H    1   -1.090000    0.000000    0.000000    1    2
//C    2    0.000000    0.000000    0.000000    6    1    3    4    5
//H    3    0.363333    1.027662    0.000000    1    2
//H    4    0.363333   -0.513831   -0.889981    1
//H    5    0.363333   -0.513831    0.889981    1    2
//

     rd();
     Map<String, Atom> map = new Hashtable<String, Atom>();
     Lst<String[]>lstTokens = new Lst<String[]>();
     int n = 0;
     while (rd() != null && line.length() > 40) {
       n++;
       String[] tokens = getTokens();
       lstTokens.addLast(tokens);
       map.put(tokens[1], addAtomXYZSymName(tokens, 2, tokens[0], null)); 
     }
     for (int i = 0; i < n; i++) {
       String[] tokens = lstTokens.get(i);
       Atom a = map.get(tokens[1]);
       for (int j = 6; j < tokens.length; j++)
         asc.addBond(new Bond(a.index, map.get(tokens[j]).index, 1));
     }
     
    
  }

  private void readCFI() throws Exception {
    // from NBO
    tokens = getTokens();
    int nAtoms = (int) getValue(0);
    int nBonds = (int) getValue(1);
    Map<String, Atom> map = new Hashtable<String, Atom>();
    for (int i = 0; i < nAtoms; i++) {
      tokens = PT.getTokens(rd());
      if (tokens[1].equals("0") || tokens[1].equals("2"))
        continue;
      Atom a = addAtomXYZSymName(tokens, 2, null, null);
      a.elementNumber = (short) getValue(1);
      map.put(tokens[0], a);
    }
    float[] bonds = fillFloatArray(null, 0, new float[nBonds * 2]);
    float[] orders = fillFloatArray(null, 0, new float[nBonds]);
    for (int i = 0, pt = 0; i < nBonds; i++)
      asc.addBond(new Bond(map.get("" + (int) bonds[pt++]).index, map.get(""
          + (int) bonds[pt++]).index, (int) orders[i]));
  }

  private boolean readVFI() throws Exception {
    // VFI format -- more than 10 tokens
    //      0   0   0   1  1  0.0000   0    0.00   0    0.00   0   2
    //      0   0   1   2 74  1.6040   1    0.00   0    0.00   0   1   3   4   5   6   7
    //      0   1   2   3  1  1.6040   2  116.57   3    0.00   0   2
    //      3   1   2   4  1  1.6040   4  116.57   5  216.00   6   2
    //      3   1   2   5  1  1.6040   7   63.43   8  108.00   9   2
    //      1   4   2   7  1  1.6040  10   63.43  11  108.00  12   2
    //      4   3   2   6  1  1.6040  13   63.43  14  108.00  15   2
    //      6   2   3   8  0  1.0000  16  120.00  17   72.00  18

    Map<String, Atom> map = new Hashtable<String, Atom>();
    Lst<String[]> bonds = new Lst<String[]>();
    while (tokens != null && tokens.length > 0) {
      for (int i = tokens.length; --i >= 11;)
        bonds.addLast(new String[] { tokens[3], tokens[i] });
      String id = tokens[3];
      tokens = (tokens[2].equals("0") ? new String[] { tokens[4] } : tokens[1]
          .equals("0") ? new String[] { tokens[4], tokens[2], tokens[5] }
          : tokens[0].equals("0") ? new String[] { tokens[4], tokens[2],
              tokens[5], tokens[1], tokens[7] }
              : new String[] { tokens[4], tokens[2], tokens[5], tokens[1],
                  tokens[7], tokens[0], tokens[9] });
      Atom atom = getAtom();
      map.put(id, atom);
      tokens = PT.getTokens(rd());
    }
    for (int i = bonds.size(); --i >= 0;) {
      String[] b = bonds.get(i);
      asc.addBond(new Bond(map.get(b[0]).index, map.get(b[1]).index, 1));
    }
    return (continuing = false);
  }

  private boolean readBlock(String strEnd) throws Exception {
    lineBuffer.clear();
    while (rd() != null && cleanLine() != null
        && (strEnd == null ? line.length() > 0 : line.indexOf(strEnd) < 0))
      lineBuffer.addLast(getTokens());
    return (continuing = false);
  }

  private String cleanLine() {
    // remove commas for Gaussian and parenthetical expressions for MOPAC 
    line = line.replace(',', ' ');
    int pt1, pt2;
    while ((pt1 = line.indexOf('(')) >= 0
        && (pt2 = line.indexOf('(', pt1)) >= 0)
      line = line.substring(0, pt1) + " " + line.substring(pt2 + 1);
    return (line = line.trim());
  }

  @Override
  protected void finalizeSubclassReader() throws Exception {
    int firstLine = 0;
    for (int i = firstLine; i < lineBuffer.size(); i++)
      if ((tokens = lineBuffer.get(i)).length > 0)
        getAtom();
    finalizeReaderASCR();
  }

  private void getSymbolic() {
    if (symbolicMap.containsKey(tokens[0]))
      return;
    float f = parseFloatStr(tokens[1]);
    symbolicMap.put(tokens[0], Float.valueOf(f));
    Logger.info("symbolic " + tokens[0] + " = " + f);
  }

  private Atom getAtom() throws Exception {
    Atom atom = new Atom();
    String element = tokens[0];
    int i = element.length();
    while (--i >= 0 && PT.isDigit(element.charAt(i))) {
      //continue;
    }
    if (++i == 0)
      element = JmolAdapter.getElementSymbol(parseIntStr(element));
    if (i == 0 || i == element.length()) {
      // no number -- append ac
      atom.atomName = element + (ac + 1);
    } else {
      // has a number -- pull out element
      atom.atomName = element;
      element = element.substring(0, i);
    }
    if (isMopac && i != tokens[0].length()) // C13 == 13C
      element = tokens[0].substring(i) + element;
    parseAtomTokens(atom, element);
    return atom;
  }

  private void parseAtomTokens(Atom atom, String element) throws Exception {
    setElementAndIsotope(atom, element);

    if (tokens.length > 5 && tokens[1].indexOf(".") >= 0) {
      //      C
      //      O  1.200000  1                                  1  0  0
      //      H  1.080000  1   120.000000  1                  1  2  0
      //      H  1.080000  0   120.000000  0  180.000000  0   1  2  3
      //      0  0.000000  0     0.000000  0    0.000000  0   0  0  0
      String[] t = tokens;
      int l = t.length;
      tokens = (t[l - 3].equals("0") ? new String[] { t[0] }
          : t[l - 2].equals("0") ? new String[] { t[0], t[l - 3], t[1] } 
          : t[l - 1].equals("0") ? new String[] { t[0], t[l - 3], t[1], t[l - 2], t[3] }
          : new String[] { t[0], t[l - 3], t[1], t[l - 2], t[3], t[l - 1], t[5] });
    }
    int ia = getAtomIndex(1);
    int bondOrder = 0;
    switch (tokens.length) {
    case 8:
      // distance + angle + dihedral + bond order (not 1)
      // O2   C1 1.3000    O1 180  X2 0  2
      // distance + angle + angle + "1"
      // O2   C1 1.3000    O1 180  X2 120  1
    case 6:
      // distance + angle + bond order 
      // O   1 1.3000    2  180       2  
      atom = getAtomGeneral(atom, ia,
          bondOrder = (int) getValue(tokens.length - 1));
      break;
    case 5:
      if (tokens[1].equals("0")) {
        // Gaussian Sym 0 x y z 
        atom.set(getValue(2), getValue(3), getValue(4));
        break;
      }
      // distance + angle
      // H   1 1.089000  2  109.4710   
      //$FALL-THROUGH$
    case 7:
      // distance + angle + dihedral
      // H   1 1.089000  2  109.4710  3  120.0000  
      // distance + two angles
      // H3 N1 1.0 H1 107 H2 107 1
      atom = getAtomGeneral(atom, ia, 0);
      break;
    case 4:
      if (getAtomIndex(1) < 0) {
        // Gaussian cartesian
        // N1 0.0 0.0 0.0
        atom.set(getValue(1), getValue(2), getValue(3));
        break;
      }
      // distance + bond order
      // O   1 1.3000 2  
      bondOrder = (int) getValue(3);
      //$FALL-THROUGH$
    case 3:
      // distance only
      // H   1 1.089000 
      if (ac != 1 || (ia = getAtomIndex(1)) != 0) {
        atom = null;
      } else {
        atom.set(getValue(2), 0, 0);
      }
      break;
    case 1:
      if (ac != 0)
        atom = null;
      else
        atom.set(0, 0, 0);
      break;
    default:
      atom = null;
    }
    if (atom == null)
      throw new Exception("bad Z-Matrix line");
    vAtoms.addLast(atom);
    atomMap.put(atom.atomName, Integer.valueOf(ac++));
    if (element.startsWith("X") && JmolAdapter.getElementNumber(element) < 1) {
      Logger.info("#dummy atom ignored: atom " + ac + " - " + atom.atomName);
    } else {
      asc.addAtom(atom);
      setAtomCoord(atom);
      Logger.info(atom.atomName + " " + atom.x + " " + atom.y + " " + atom.z);
      if (bondOrder < 0 || isJmolZformat && bondOrder > 0)
        asc.addBond(new Bond(atom.index, vAtoms.get(ia).index, Math
            .abs(bondOrder)));
    }
  }

  private Atom getAtomGeneral(Atom atom, int ia, int bondOrder)
      throws Exception {
    int ib, ic;
    if (tokens.length < 7 && ac != 2 || (ib = getAtomIndex(3)) < 0
        || (ic = (tokens.length < 7 ? -2 : getAtomIndex(5))) == -1) {
      return null;
    }
    float d = getValue(2);
    float theta1 = getValue(4);
    float theta2 = (tokens.length < 7 ? Float.MAX_VALUE : getValue(6));
    if (tokens.length == 8 && !isJmolZformat && !isMopac && bondOrder == 1)
      // Gaussian indicator of alternative angle representation
      d = -Math.abs(d);
    return atom = setAtom(atom, ia, ib, ic, d, theta1, theta2);
  }

  private float getSymbolic(String key) {
    boolean isNeg = key.startsWith("-");
    Float F = symbolicMap.get(isNeg ? key.substring(1) : key);
    if (F == null)
      return Float.NaN;
    float f = F.floatValue();
    return (isNeg ? -f : f);
  }

  private float getValue(int i) throws Exception {
    float f = getSymbolic(tokens[i]);
    if (Float.isNaN(f)) {
      f = parseFloatStr(tokens[i]);
      if (Float.isNaN(f))
        throw new Exception("Bad Z-matrix value: " + tokens[i]);
    }
    return f;
  }

  private int getAtomIndex(int i) {
    String name;
    if (i >= tokens.length || (name = tokens[i]).indexOf(".") >= 0
        || !PT.isLetterOrDigit(name.charAt(0)))
      return -1;
    int ia = parseIntStr(name);
    if (ia <= 0 || name.length() != ("" + ia).length()) {
      // check for clean integer, not "13C1"
      Integer I = atomMap.get(name);
      if (I == null) {
        // We allow just an element name without a number if it is unique.
        // The most recent atom match will be used 
        for (i = vAtoms.size(); --i >= 0;) {
          Atom atom = vAtoms.get(i);
          if (atom.atomName.startsWith(name)
              && atom.atomName.length() > name.length()
              && PT.isDigit(atom.atomName.charAt(name.length()))) {
            I = atomMap.get(atom.atomName);
            break;
          }
        }
      }
      if (I == null)
        ia = -1;
      else
        ia = I.intValue();
    } else {
      ia--;
    }
    return ia;
  }

  private final P3 pt0 = new P3();
  private final V3 v1 = new V3();
  private final V3 v2 = new V3();
  private final P4 plane1 = new P4();
  private final P4 plane2 = new P4();

  protected Atom setAtom(Atom atom, int ia, int ib, int ic, float d,
                         float theta1, float theta2) {
    if (Float.isNaN(theta1) || Float.isNaN(theta2))
      return null;
    pt0.setT(vAtoms.get(ia));
    v1.sub2(vAtoms.get(ib), pt0);
    v1.normalize();
    if (theta2 == Float.MAX_VALUE) {
      // just the first angle being set
      v2.set(0, 0, 1);
      (Quat.newVA(v2, theta1)).transform2(v1, v2);
    } else if (d >= 0) {
      // theta2 is a dihedral angle
      // just do two quaternion rotations
      v2.sub2(vAtoms.get(ic), pt0);
      v2.cross(v1, v2);
      (Quat.newVA(v2, theta1)).transform2(v1, v2);
      (Quat.newVA(v1, -theta2)).transform2(v2, v2);
    } else {
      // d < 0
      // theta1 and theta2 are simple angles atom-ia-ib and atom-ia-ic 
      // get vector that is intersection of two planes and go from there
      Measure.getPlaneThroughPoint(setAtom(atom, ia, ib, ic, -d, theta1, 0),
          v1, plane1);
      Measure.getPlaneThroughPoint(setAtom(atom, ia, ic, ib, -d, theta2, 0),
          v1, plane2);
      Lst<Object> list = Measure.getIntersectionPP(plane1, plane2);
      if (list.size() == 0)
        return null;
      pt0.setT((P3) list.get(0));
      d = (float) Math.sqrt(d * d - pt0.distanceSquared(vAtoms.get(ia)))
          * Math.signum(theta1) * Math.signum(theta2);
      v2.setT((V3) list.get(1));
    }
    atom.scaleAdd2(d, v2, pt0);
    return atom;
  }
}
