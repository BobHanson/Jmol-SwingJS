/* $RCSfile$
 * $Author: hansonr $
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

/* 
 * Copyright (C) 2009  Joerg Meyer, FHI Berlin
 *
 * Contact: meyer@fhi-berlin.mpg.de
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

import javajs.util.PT;
import javajs.util.SB;
import javajs.util.V3;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.util.Vibration;

/**
 * Bilbao Crystallographic Database file reader
 * 
 * see, for example, http://www.cryst.ehu.es/cryst/compstru.html Comparison of
 * Crystal Structures with the same Symmetry
 * 
 * Note that this reader scrapes HTML. Keys for Bilbao format data are
 * a given bit of text such as "High symmetry structure".
 * Any changes to that (including capitalization) will cause this reader to fail.
 * The space group number is read immediately after the "pre" tag on that line.
 * 
 * filter options include:
 * 
 * HIGH include high-symmetry structure;
 * 
 * preliminary only
 * 
 * @author Bob Hanson
 */

public class BilbaoReader extends AtomSetCollectionReader {

  private boolean getHigh;
  private boolean getSym;
  private boolean normDispl;
  private boolean doDisplace;
  private String kvec;
  private int i0;
  private int nAtoms;
  private boolean isBCSfile;

  @Override
  public void initializeReader() throws Exception {
    normDispl = !checkFilterKey("NONORM");
    doDisplace = isTrajectory;
    getSym = true;
    getHigh = checkFilterKey("HIGH") && !doDisplace;
    asc.vibScale = 1;
    appendLoadNote("Bilbao Crystallographic Server\ncryst@wm.lc.ehu.es");
    if (rd().indexOf("<") < 0) {
      readBilbaoDataFile();
      continuing = false;
    }
  }

  @Override
  protected boolean checkLine() throws Exception {
    if (line.contains(">Bilbao Crystallographic Server<")) {
      line = line.substring(line.lastIndexOf(">") + 1).trim();
      if (line.length() > 0)
        appendLoadNote(line + "\n");
    } else if (line.contains("High symmetry structure<")) {
      if (getHigh)
        readBilbaoFormat("high symmetry", Float.NaN);
    } else if (line.contains("Low symmetry structure<")) {
      if (!doDisplace)
        readBilbaoFormat("low symmetry", Float.NaN);
    } else if (line.contains("structure in the subgroup basis<")) {
      if (!doDisplace)
        readBilbaoFormat("high symmetry in the subgroup basis", Float.NaN);
    } else if (line.contains("Low symmetry structure after the origin shift<")) {
      readBilbaoFormat("low symmetry after origin shift", Float.NaN);
    } else if (line.contains("<h3>Irrep:")) {
      readVirtual();
    }
    return true;
  }

  private void readBilbaoDataFile() throws Exception {
    isBCSfile = true;
    checkComment();
    while (line != null) {
      readBilbaoFormat(null, Float.NaN);
      if (rdLine() == null || line.indexOf("##disp-par##") < 0) {
        applySymmetryAndSetTrajectory();
      } else {
        readDisplacements(1);
        rdLine();
      }
    }
  }

  private boolean checkComment() {
    if (!line.startsWith("#") || line.indexOf("disp-par") >= 0)
      return false;
    if (isBCSfile) {
      appendLoadNote(line);
      if (line.startsWith("# Title:"))
        asc.setAtomSetName(line.substring(8).trim());
    }
    return true;
  }

  /*
  15
  13.800 5.691 9.420 90.0 102.3 90.0
  7
  Pb    1   4e    0.0000 0.2910 0.2500
  Pb    2   8f    0.3170 0.3090 0.3520
  P     1   8f    0.5990 0.2410 0.4470
  O     1   8f    0.6430 0.0300 0.3920
  O     2   8f    0.6340 0.4640 0.3740
  O     3   8f    0.6420 0.2800 0.6120
  O     4   8f    0.4910 0.2220 0.4200 
  */
  private void readBilbaoFormat(String title, float fAmp) throws Exception {
    setFractionalCoordinates(true);
    if (!doGetModel(++modelNumber, title))
      return;
    asc.newAtomSet();
    if (line.startsWith("Bilbao Crys:")) {
      title = line.substring(13).trim();
      rdLine();
    }
    setTitle(title);
    int ptPre = line.indexOf("<pre>");
    if (ptPre >= 0)
      line = line.substring(ptPre + 5);
    int intTableNo = parseIntStr(line);
    if (intTableNo == 0) {
      setSpaceGroupName("bilbao:" + line.substring(2));
    } else {
      while (intTableNo < 0 && rdLine() != null)
        intTableNo = parseIntStr(line);
      setSpaceGroupName("bilbao:" + intTableNo);
    }
    float[] data = new float[6];
    fillFloatArray(null, 0, data);
    for (int i = 0; i < 6; i++)
      setUnitCellItem(i, data[i]);
    i0 = asc.ac;
    nAtoms = parseIntStr(rdLine());
    for (int i = nAtoms; --i >= 0;) {
      String[] tokens = PT.getTokens(rdLine());
      if (!getSym && tokens[1].contains("_"))
        continue;
      if (tokens.length == 3)
        addAtomXYZSymName(tokens, 0,"Be", "Be1");
      else
        addAtomXYZSymName(tokens, 3, tokens[0], tokens[0] + tokens[1]);
    }
    if (Float.isNaN(fAmp)) {
      if (ptPre >= 0)
        applySymmetryAndSetTrajectory();
      return;
    }
    line = null;
    readDisplacements(fAmp);
  }

  private void readDisplacements(float fAmp) throws Exception {
    /*
    ##disp-par## Rb1x|x0.000000x|x0.000791x|x-0.001494
    ##disp-par## Rb1_2x|x0.000000x|x0.000791x|x0.001494
     */
    for (int i = 0; i < nAtoms; i++) {
      if (line == null)
        rdLine();
      String[] tokens = PT.split(line, "x|x");
      if (getSym || !tokens[0].contains("_"))
        asc.atoms[i0 + i].vib = V3.new3(parseFloatStr(tokens[1]),
            parseFloatStr(tokens[2]), parseFloatStr(tokens[3]));
      line = null;
    }
    applySymmetryAndSetTrajectory();
    // convert the atom vibs to Cartesian displacements
    for (int i = asc.ac; --i >= i0;) {
      Atom a = asc.atoms[i];
      if (a.vib != null) {
        Vibration v = new Vibration();
        v.setT(a.vib);
        a.vib = v;
        //v.modDim = Vibration.TYPE_DISPLACEMENT;
        asc.getSymmetry().toCartesian(v, true);
        v.scale(1 / fAmp);
      }
    }
    appendLoadNote((asc.ac - i0) + " displacements");

  }

  private void setTitle(String title) {
    if (title != null) {
      asc.setAtomSetName(title);
      appendLoadNote(title);
    }
  }

  private String rdLine() throws Exception {
    while (rd() != null && (line.length() == 0 || checkComment())) {
    }
    return line;
  }

  /*
  <input type="hidden" name="irrep" value="GM1+::GM">
  <input type="hidden" name="set" value="<i>C</i><i>c</i>">
  <input type="hidden" name="iso" value=" 62 Pnma D2h-16">
  <input type="hidden" name="what" value="virtual">
  <input type="submit" value="Virtual structure">
  with only this symmetry component of the distortion frozen.</form>
  <form action="mcif2vesta/index.php" method=post>
  <input type="hidden" name="BCS" value="009
  15.312000 26.660000 29.121000 90.000000 90.000000 90.000000 
  */

  private void readVirtual() throws Exception {
    // <h3>K-vector:  GM = (0,0,0)</h3><h3>Irrep: GM1+</h3><h4>Direction: (a)</h4><h4>Isotropy Subgroup:  62 Pnma D2h-16</h4>Transformation matrix:...
    if (line.contains("<h3>K-vector:"))
      kvec = line.substring(line.indexOf("("), line.indexOf(")") + 1);
    String s = getLinesUntil("\"BCS\"");
    int pt = s.indexOf("The amplitude");
    pt = s.indexOf("=", pt);
    String amp = s.substring(pt + 2, s.indexOf(" ", pt + 2));
    float fAmp = (normDispl ? parseFloatStr(amp) : 1);
    String irrep = getAttr(s, "irrep");
    if (irrep.indexOf(":") >= 0)
      irrep = irrep.substring(0, irrep.indexOf(":"));
    //String set = getAttr(s, "set");
    //String iso = getAttr(s, "iso");
    //String what = getAttr(s, "what");
    line = line.substring(line.indexOf("value=") + 7);
    readBilbaoFormat(kvec + " " + irrep + " (" + amp + " Ang.)",
        fAmp);

  }

  private String getAttr(String s, String key) {
    int pt = s.indexOf("value", s.indexOf("\"" + key + "\""));
    s = PT.getQuotedStringAt(s, pt);
    s = PT.rep(s, "<i>", "");
    s = PT.rep(s, "</i>", "");
    return s.trim();
  }

  private String getLinesUntil(String key) throws Exception {
    SB sb = new SB();
    do {
      sb.append(line);
    } while (!rd().contains(key));
    return sb.toString();
  }
}
