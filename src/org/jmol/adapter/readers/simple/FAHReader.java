  /* $RCSfile$
   * $Author: hansonr $
   * $Date: 2006-09-10 10:36:58 -0500 (Sun, 10 Sep 2006) $
   * $Revision: 5478 $
   *
   * Copyright (C) 2004-2005  The Jmol Development Team
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
  
  import java.io.BufferedReader;
  
  import org.jmol.adapter.smarter.Atom;
  import org.jmol.adapter.smarter.AtomSetCollectionReader;
  import org.jmol.adapter.smarter.Bond;
  import org.jmol.util.Logger;
  
  import javajs.util.PT;
  import javajs.util.Rdr;
  
  /**
   * FoldingAtHome json reader.
   * 
   * Both the top and the coord files are necessary.
   * 
   * But the coord files can be loaded as trajectories if the top has already been
   * loaded for a given model.
   * 
   * 
   * 
   */
  
  public class FAHReader extends AtomSetCollectionReader {
    //
    //  {
    //    "units": "ANGSTROM",
    //    "atoms": [
    //      ["C", 0.5972, -1, 12.01078, 6, "ACE"],
    //      ["O", -0.5679, -1, 15.99943, 8, "ACE"],
    //      ["CH3", -0.3662, -1, 12.01078, 6, "ACE"],
    //
    //  [
    //   [-1.646454, 1.34258, -0.908085],
    //   [-1.58011, 1.333847, -1.014838],
    //   [-1.699982, 1.480355, -0.872761],
  
    @Override
    protected void initializeReader() {
    }
  
    @Override
    protected void finalizeSubclassReader() throws Exception {
      asc.setNoAutoBond();
      setModelPDB(true);
      finalizeReaderASCR();
    }
  
    /**
     * @return true if next line needs to be read.
     * 
     *         Note that just a single token on line 1 is NOT possible. If that
     *         were the case, the xyz reader would have captured this.
     * 
     */
    @Override
    protected boolean checkLine() throws Exception {
      if (line.startsWith("{")) {
        return readTopAtomsAndBonds();
      }
      if (line.startsWith("[")) {
        return readVnnCoords();
      }
      return true;
    }
  
    float factor = 1;
  
    int[] pt = new int[1];
  
    private BufferedReader readerSave;
  
    private String units;
  
    final static String note = " -- FAH:: is required for Frame files but not the Top file.\n"
        + " -- automatic calculation of structure using DSSP\n"
        + " -- Both files are required; three load options:\n" + "    \n"
        + "    LOAD FILES \"ViewerTop.json\" + \"ViewerFrame22.json\" // explicit joining to two files\n"
        + "    \n" + "    LOAD ViewerTop.json; \n"
        + "    LOAD XYZ FAH::ViewerFrame22.json   // first the atoms, then the coordinates\n"
        + "    \n"
        + "    LOAD FAH::ViewerFrame22.json        // just the coordinates with associated ViewerTop.json assumed present\n"
        + "    \n"
        + " -- Subsequent calls to LOAD XYZ will replace coordinates and recalculate DSSP only.\n"
        + "\n" + "    LOAD ViewerTop.json; \n"
        + "    LOAD XYZ FAH::ViewerFrame0.json \n" + "    delay 1.0\n"
        + "    LOAD XYZ FAH::ViewerFrame22.json \n" + " \n" + "";
  
    boolean readTopAtomsAndBonds() throws Exception {
      //    {
      //      "units": "ANGSTROM",
      //      "atoms": [
      //        ["C", 0.5972, -1, 12.01078, 6, "ACE"],
  
      if (readerSave == null)
        appendLoadNote(note);
      discardLinesUntilContains("\"");
      if (line.indexOf("units") >= 0) {
        pt[0] = line.indexOf(":");
        units = PT.getQuotedStringNext(line, pt);
        if (units != null && (units.equalsIgnoreCase("NM") || units.toUpperCase().indexOf("NANOMETER") >= 0)) {
          factor = 0.1f;
        }
        Logger.info("FAHReader units are " + units + " factor = " + factor);
      }
      discardLinesUntilContains("atoms");
      int index = 0;
      while (rd() != null) {
        //    ["C", 0.5972, -1, 12.01078, 6, "ACE"],
        String[] tokens = getTokens();
        pt[0] = 0;
        String name = PT.getQuotedStringNext(tokens[0], pt);
        pt[0] = 0;
        String group = PT.getQuotedStringNext(tokens[5], pt);
        int elemNo = parseIntStr(tokens[4]);
        Atom atom = new Atom();
        atom.elementNumber = (short) elemNo;
        atom.atomName = name;
        atom.group3 = group;
        atom.set(0, 0, index++);
        asc.addAtom(atom);
        if (line.trim().endsWith("]"))
          break;
      }
      discardLinesUntilContains("bonds");
      //   "bonds": [
      //    [0, 2],
  
      while (rd() != null) {
        pt[0] = line.indexOf("[") + 1;
        int a = PT.parseIntNext(line, pt);
        pt[0] = line.indexOf(",") + 1;
        int b = PT.parseIntNext(line, pt);
        asc.addBond(new Bond(a, b, 1));
        if (line.trim().endsWith("]"))
          break;
      }
      Logger.info("FAHReader " + asc.ac + " top atoms read");
      rd(); // ]
      rd(); // }
      return true;
    }
  
    private boolean readVnnCoords() throws Exception {
      //    [
      //     [-1.646454, 1.34258, -0.908085],
  
      int iatom = 0;
  
      if (asc.ac == 0 && readerSave == null
          && !"xyz".equals(htParams.get("dataType"))) {
        getTopData();
      }
      Atom[] atoms = asc.atoms;
      int atomCount = asc.ac;
  
      while (rd() != null) {
        pt[0] = 0;
        line = line.trim();
        String[] tokens = line.substring(1, line.length() - 1).split(",");
  
        float x = parseFloatStr(tokens[0]) * factor;
        float y = parseFloatStr(tokens[1]) * factor;
        float z = parseFloatStr(tokens[2]) * factor;
        Atom atom = (iatom >= atomCount ? asc.addAtom(new Atom()) : atoms[iatom]);
        atom.set(x, y, z);
        iatom++;
        if (line.trim().endsWith("]"))
          break;
      }
      Logger.info("FAHReader " + iatom + " atom coordinates read");
      if (!checkBondlengths())
        checkBondlengths();
      rd();
      return true;
    }
  
    private boolean checkBondlengths() {
      double d = 1;
      if (asc.bondCount > 0) {
        Bond b = asc.bonds[0];
        d = asc.atoms[b.atomIndex1].distance(asc.atoms[b.atomIndex2]);
      } else {
        for (int i = Math.min(asc.ac, 10); --i >= 0;) {
          for (int j = i; --j >= 0;) {
            d = Math.min(d, asc.atoms[i].distance(asc.atoms[j]));
          }
        }
      }
      if (d < 0.5f) {
        for (int i = asc.ac; --i >= 0;) {
          asc.atoms[i].scale(10);
        }
        // factor was wrong -- this is NM

        String msg = "FAHReader CORRECTION: Top.json file units=" + units + "\n but we found NANOMETERS based on\n " + (asc.bondCount > 0 
            ? "bonds[0].length" : "shortest distance among first 10 atoms") + "=" + d;
        appendLoadNote(msg);
        return false;
      } 
      return true;
  
    }
  
    private void getTopData() {
      String fileName = (String) htParams.get("fullPathName");
      int pt = fileName.indexOf("::");
      if (pt > 0)
        fileName = fileName.substring(pt + 2);
      pt = fileName.lastIndexOf(".");
      if (pt < 0)
        pt = fileName.length();
      int ptv = fileName.lastIndexOf("Frame", pt);
      fileName = fileName.substring(0, ptv) + "Top" + fileName.substring(pt);
      String data = vwr.getFileAsString3(fileName, false, null);
      Logger.info("FAHReader " + data.length() + " bytes read from " + fileName);
      boolean isError = (data.indexOf("\"atoms\"") < 0);
      if (isError) {
        Logger.error("FAHReader " + fileName + "was not found");
      } else {
        readerSave = reader;
        reader = Rdr.getBR(data);
        try {
          rd();
          checkLine();
        } catch (Exception e) {
          // TODO
        }
        reader = readerSave;
      }
    }
  
  }
