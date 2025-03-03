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

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.PT;
import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Bond;
import org.jmol.api.JmolAdapter;
import org.jmol.util.Logger;

/**
 * A reader for MDLI mol and sdf files.
 * <p>
 * <a href='http://www.mdli.com/downloads/public/ctfile/ctfile.jsp'>
 * http://www.mdli.com/downloads/public/ctfile/ctfile.jsp </a>
 * <p>
 * 
 * also: http://www.mdl.com/downloads/public/ctfile/ctfile.pdf
 * 
 * simple symmetry extension via load command: 9/2006 hansonr@stolaf.edu
 * 
 * setAtomCoord(atom, x, y, z) applySymmetryAndSetTrajectory()
 * 
 * simple 2D-->3D conversion using
 * 
 * load "xxx.mol" FILTER "2D"
 * 
 * 
 * Jmol 14.8.2 adds bond type 15 (quintuple) and 16 (sextuple)
 * 
 */
public class MolReader extends AtomSetCollectionReader {

  /*
   * from ctfile.pdf:
   * 
   * $MDL REV 1 date/time
   * $MOL
   * $HDR
   * [Molfile Header Block (see Chapter 4) = name, pgm info, comment]
   * $END HDR
   * $CTAB
   * [Ctab Block (see Chapter 2) = count + atoms + bonds + lists + props]
   * $END CTAB
   * $RGP
   * rrr [where rrr = Rgroup number]
   * $CTAB
   * [Ctab Block]
   * $END CTAB
   * $END RGP
   * $END MOL
   */

  private boolean haveAtomSerials;
  protected boolean allow2D = true;
  private int iatom0;
  private V3000Rdr vr;
  private int atomCount;
  private String[] atomData;
  public BS bsDeleted;
  private boolean haveNonzeroZ;
  /**
   * fix charges for RN(=O)(O), =N(O)*, =N
   */
  private boolean fixN;
  private boolean is3D;
  private int nDouble;
  private int nH, nC;

  @Override
  public void initializeReader() throws Exception {
    fixN = checkFilterKey("FIXN");
  }

  @Override
  protected boolean checkLine() throws Exception {
    boolean isMDL = (line.startsWith("$MDL"));
    if (isMDL) {
      discardLinesUntilStartsWith("$HDR");
      rd();
      if (line == null) {
        Logger.warn("$HDR not found in MDL RG file");
        continuing = false;
        return false;
      }
    } else if (line.equals("M  END")) {
      return true;
    }
    if (doGetModel(++modelNumber, null)) {
      iatom0 = asc.ac;
      processMolSdHeader();
      processCtab(isMDL);
      vr = null;
      if (isLastModel(modelNumber)) {
        continuing = false;
        return false;
      }
    }
    if (line != null && line.indexOf("$$$$") < 0)
      discardLinesUntilStartsWith("$$$$");
    return true;
  }

  @Override
  public void finalizeSubclassReader() throws Exception {
    finalizeReaderMR();
  }

  protected void finalizeReaderMR() throws Exception {
    if (fixN) {
      addJmolScript("{search('[Nv4+0,nv4+0]')}.formalCharge=1;{search('{[Ov1-0]}[Nv4+1,nv4+1]')}.formalCharge=-1;");
    }
    check2D3D();
    if (bsDeleted != null) {
        asc.getBSAtoms(-1).andNot(bsDeleted);
    }
    isTrajectory = false;
    finalizeReaderASCR();
  }

  /**
   * Make some attempt to account for missing 2D/3D information or wrong information
   * @throws Exception 
   */
  private void check2D3D() throws Exception {
    if (haveNonzeroZ) {
      if (is2D) {
        is2D = optimize2D = false;
      }
    } else if (!is2D && !is3D) {
      // all z values zero and no 2D or 3D flag
      if (nC > 0 && nH == 0 && nDouble != 0) {
        // JME does not write the 2D/3D line correctly
        // double bonds with carbons and no hydrogens
        // assume is 2D
        is2D = true;
      }
    }
    if (is2D) {
      if (!allow2D) {
          throw new Exception("File is 2D, not 3D");
      }
      set2D();
    }
  }

  private void processMolSdHeader() throws Exception {
    // We aren't being this strict. Line definitions are from ctfile.pdf (October 2003)
    // Line 1: Molecule name. This line is unformatted, but like all 
    // other lines in a molfile may not extend beyond column 80. 
    // If no name is available, a blank line must be present.
    // Caution: This line must not contain any of the reserved 
    // tags that identify any of the other CTAB file types 
    // such as $MDL (RGfile), $$$$ (SDfile record separator), 
    // $RXN (rxnfile), or $RDFILE (RDfile headers). 
    // 
    String thisDataSetName = line.trim();
    asc.setCollectionName(thisDataSetName);

    String header = line + "\n";

    // Line 2: This line has the format:
    // IIPPPPPPPPMMDDYYHHmmddSSssssssssssEEEEEEEEEEEERRRRRR
    // (FORTRAN: A2<--A8--><---A10-->A2I2<--F10.5-><---F12.5--><-I6-> )
    // User's first and last initials (l), program name (P), 
    // date/time (M/D/Y,H:m), dimensional codes (d), scaling factors (S, s), 
    // energy (E) if modeling program input, internal 
    // registry number (R) if input through MDL form. A blank line can be 
    // substituted for line 2. If the internal registry number is more than 
    // 6 digits long, it is stored in an M REG line (described in Chapter 3). 
    rd();
    if (line == null)
      return;
    header += line + "\n";
    // default will be to have this be 3D, but we will also check
    
    String dim = (line.length() >= 22 ? line.substring(20, 22) : null);
    is3D = "3D".equals(dim);
    is2D = !is3D && ("2D".equals(dim) || line.startsWith("JME "));
    
    // Line 3: A line for comments. If no comment is entered, a blank line 
    // must be present.
    rd();
    if (line == null)
      return;
    line = line.trim();
    header += line + "\n";
    Logger.info(header);
    checkCurrentLineForScript();
    asc.setInfo("fileHeader", header);
    newAtomSet(thisDataSetName);
  }

  private void processCtab(boolean isMDL) throws Exception {
    if (isMDL)
      discardLinesUntilStartsWith("$CTAB");
    if (rd() == null)
      return;
    if (line.indexOf("V3000") >= 0) {
      optimize2D = is2D && !noHydrogens;
      vr = ((V3000Rdr) getInterface("org.jmol.adapter.readers.molxyz.V3000Rdr")).set(this);
      discardLinesUntilContains("COUNTS");
      vr.readAtomsAndBonds(getTokens());
    } else {
      readAtomsAndBonds(parseIntRange(line, 0, 3), parseIntRange(line, 3, 6));
    }
    applySymmetryAndSetTrajectory();
  }

  // 0         1         2         3         4         5         6         7
  // 01234567890123456789012345678901234567890123456789012345678901234567890
  // xxxxx.xxxxyyyyy.yyyyzzzzz.zzzz aaaddcccssshhhbbbvvvHHHrrriiimmmnnneee

  private void readAtomsAndBonds(int ac, int bc) throws Exception {
    atomCount  = ac;
    for (int i = 0; i < ac; ++i) {
      rd();
      int len = line.length();
      String elementSymbol;
      double x, y, z;
      int charge = 0;
      int isotope = 0;
      int iAtom = Integer.MIN_VALUE;
      x = parseDoubleRange(line, 0, 10);
      y = parseDoubleRange(line, 10, 20);
      z = parseDoubleRange(line, 20, 30);
      // CTFile doc for V3000:
      // The “dimensional code” is maintained more explicitly. 
      // Thus “3D” really means 3D,
      // although “2D” will be interpreted as 3D if 
      // any non-zero Z-coordinates are found.
      if (len < 34) {
        // deal with older Mol format where nothing after the symbol is used
        elementSymbol = line.substring(31).trim();
      } else {
        elementSymbol = line.substring(31, 34).trim();
        if (elementSymbol.equals("H1")) {
          elementSymbol = "H";
          isotope = 1;
        }
        if (len >= 39) {
          int code = parseIntRange(line, 36, 39);
          if (code >= 1 && code <= 7)
            charge = 4 - code;
          code = parseIntRange(line, 34, 36);
          if (code != 0 && code >= -3 && code <= 4) {
            isotope = JmolAdapter.getNaturalIsotope(JmolAdapter
                .getElementNumber(elementSymbol)) + code;
          }
          //if (len >= 63) {  this field is not really an atom number. It's for atom-atom mapping in reaction files
          //  iAtom = parseIntRange(line, 60, 63);
          //  if (iAtom == 0)
          //    iAtom = Integer.MIN_VALUE;
          //}
          // previous model in series may have atom numbers indicated
          if (iAtom == Integer.MIN_VALUE && haveAtomSerials)
            iAtom = i + 1;
        }
      }
      addMolAtom(iAtom, isotope, elementSymbol, charge, x, y, z);
    }
    asc.setModelInfoForSet("dimension", (is2D ? "2D" : "3D"), asc.iSet);
    rd();
    if (line.startsWith("V  ")) {
      readAtomValues();
    }      

    // read bonds

    if (bc == 0)
      asc.setNoAutoBond();
    for (int i = 0; i < bc; ++i) {
      if (i > 0)
        rd();
      String iAtom1, iAtom2;
      int stereo = 0;
      iAtom1 = line.substring(0, 3).trim();
      iAtom2 = line.substring(3, 6).trim();
      int order = parseIntRange(line, 6, 9);
      if (order == 1 && line.length() >= 12)
        stereo = parseIntRange(line, 9, 12);
      if (stereo != 0 && !is3D)
        is2D = true;
      order = fixOrder(order, stereo);
      if (haveAtomSerials)
        asc.addNewBondFromNames(iAtom1, iAtom2, order);
      else
        asc.addNewBondWithOrder(iatom0 + parseIntStr(iAtom1) - 1, iatom0
            + parseIntStr(iAtom2) - 1, order);
    }

    // read V2000 user data

    Map<String, Object> molData = new Hashtable<String, Object>();
    Lst<String> _keyList = new Lst<String>();
    rd();
    while (line != null && line.indexOf("$$$$") != 0) {
      if (line.indexOf(">") == 0) {
        readMolData(molData, _keyList);
        continue;
      }
      if (line.startsWith("M  ISO")) {
        readIsotopes();
        continue;
      }
      if (line.startsWith("M  CHG")) {
        readCharges();
        continue;
      }
      rd();
    }
    if (atomData != null) {
      Object atomValueName = molData.get("atom_value_name");
      molData.put(atomValueName == null ? "atom_values" : atomValueName.toString(), atomData);
    }
    if (!molData.isEmpty()) {
      asc.setCurrentModelInfo("molDataKeys", _keyList);
      asc.setCurrentModelInfo("molData", molData);
    }
  }

  /**
   * Read all V  nnn lines as string data; user can adapt as needed.
   * 
   * @throws Exception
   */
  private void readAtomValues() throws Exception {
    atomData = new String[atomCount];
    for (int i = atomData.length; --i >= 0;)
      atomData[i] = "";
    while (line.indexOf("V  ") == 0) {
     int iAtom = parseIntAt(line, 3);
     if (iAtom < 1 || iAtom > atomCount) {
       Logger.error("V  nnn does not evalute to a valid atom number: " + iAtom);
       return;
     }
     String s = line.substring(6).trim();
     atomData[iAtom - 1] = s;
     rd(); 
    }
  }

  /**
   * Read all M  ISO  lines. These are absolute isotope numbers.
   * 
   * @throws Exception
   */
  private void readIsotopes() throws Exception {
    try {
      int n = parseIntAt(line, 6);
      int i0 = asc.getLastAtomSetAtomIndex();
      for (int i = 0, pt = 9; i < n; i++) {
        int ipt = parseIntAt(line, pt);
        Atom atom = asc.atoms[ipt + i0 - 1];
        int iso = parseIntAt(line, pt + 4);
        pt += 8;
        String sym = PT.replaceAllCharacters(atom.elementSymbol, "0123456789", "");
        // special hack to get SMILES imine stereochemistry correct with CIR
        if (iso == 17 && sym.equals("C")) {
          atom.elementSymbol = "N";
        } else if (iso == 5 && sym.equals("H")) {
          deleteAtom(atom);
        } else {
          atom.elementSymbol = "" + iso + sym;
        }
      }
    } catch (Throwable e) {
      // ignore error here
    }
    rd();
  }

  /**
   * Read all M  CHG  lines. These are absolute charges.
   * 
   * @throws Exception
   */
  private void readCharges() throws Exception {
//    M CHGnn8 aaa vvv ...
//
//    vvv: -15 to +15. Default of 0 = uncharged atom. When present, this property supersedes
//    all charge and radical values in the atom block, forcing a 0 charge on all atoms not
//    listed in an M CHG or M RAD line    int n = parseIntAt(line, 6);
    try {
      int n = parseIntAt(line, 6);
      int i0 = asc.getLastAtomSetAtomIndex();
      for (int i = asc.ac; --i >= i0;) {
        asc.atoms[i].formalCharge = 0;
      }
      for (int i = 0, pt = 9; i < n; i++, pt += 8) {
        asc.atoms[parseIntAt(line, pt) + i0 - 1].formalCharge = parseIntAt(line, pt + 4);
      }
    } catch (Throwable e) {
      // ignore error here
    }
    rd();
  }

  public void deleteAtom(Atom atom) {
    if (bsDeleted == null)
      bsDeleted = new BS();
    bsDeleted.set(atom.index);
  }

  /**
   * Read the SDF data with name in lower case
   * 
   * @param molData
   * @param _keyList 
   * @throws Exception
   */
  private void readMolData(Map<String, Object> molData, Lst<String> _keyList) throws Exception {
    Atom[] atoms = asc.atoms;
    // "> <xxx>" becomes "xxx"
    // "> yyy <xxx> zzz" becomes "yyy <xxx> zzz"
    String dataName = PT.trim(line, "> <").toLowerCase();
    String data = "";
    double[] fdata = null;
    // officially, we need a terminating blank line, and $$$$ could be data,
    // but here we do not allow $$$$ due to Jmol legacy writing of JMOL_PARTIAL_CHARGES
    while (rd() != null && !line.equals("$$$$") && line.length() > 0)     
      data += (line.length() == 81 && line.charAt(80) == '+' ? line.substring(0, 80) : line +  "\n");
    data = PT.trim(data, "\n");
    Logger.info(dataName + ":" + PT.esc(data));
    molData.put(dataName, data);
    _keyList.addLast(dataName);

    int ndata = 0;
    if (dataName.toUpperCase().contains("_PARTIAL_CHARGES")) {
      try {
        fdata = PT.parseDoubleArray(data);
        for (int i = asc.getLastAtomSetAtomIndex(), n = asc.ac; i < n; i++)
          atoms[i].partialCharge = 0;
        int pt = 0;
        for (int i = (int) fdata[pt++]; --i >= 0;) {
          int atomIndex = (int) fdata[pt++] + iatom0 - 1;
          double partialCharge = fdata[pt++];
          atoms[atomIndex].partialCharge = partialCharge;
          ndata++;
        }
      } catch (Throwable e) {
        for (int i = asc.getLastAtomSetAtomIndex(), n = asc.ac; i < n; i++)
          atoms[i].partialCharge = 0;
        Logger.error("error reading " + dataName + " field -- partial charges cleared");
      }
      Logger.info(ndata + " partial charges read");
    } else if (dataName.toUpperCase().contains("ATOM_NAMES")) {
      ndata = 0;
      try {
        String[] tokens = PT.getTokens(data);
        int pt = 0;
        for (int i = parseIntStr(tokens[pt++]); --i >= 0;) {
          int iatom;
          // try to skip over extra stuff if it is not a number 
          while ((iatom = parseIntStr(tokens[pt++])) == Integer.MIN_VALUE){}
          int atomIndex = iatom + iatom0 - 1;
          String name = tokens[pt++];
          if (!name.equals("."))
            atoms[atomIndex].atomName = name;
          ndata++;
        }
      } catch (Throwable e) {
        Logger.error("error reading " + dataName + " field");
      }
      Logger.info(ndata + " atom names read");
    }
  }

  public Atom addMolAtom(int iAtom, int isotope, String elementSymbol,
                         int charge, double x, double y, double z) {
    if ("H".equals(elementSymbol))
      nH++;
    else if ("C".equals(elementSymbol))
      nC++;
    haveNonzeroZ |= (Math.abs(z) > 0.001d);
    switch (isotope) {
    case 0:
      break;
    case 1:
      elementSymbol = "1H";
      break;
    case 2:
      elementSymbol = "2H";
      break;
    case 3:
      elementSymbol = "3H";
      break;
    default:
      elementSymbol = isotope + elementSymbol;
    } 
    Atom atom = new Atom();
    atom.elementSymbol = elementSymbol;
    atom.formalCharge = charge;
    setAtomCoordXYZ(atom, x, y, z);
    if (iAtom == Integer.MIN_VALUE) {
      asc.addAtom(atom);
    } else {
      haveAtomSerials = true;
      atom.atomSerial = iAtom;
      asc.addAtomWithMappedSerialNumber(atom);
    }
    return atom;
  }

  int fixOrder(int order, int stereo) {
    switch (order) {
    default:
    case 0:
    case -10:
      return 1; // smiles parser error 
    case 1:
      switch (stereo) {
      case 1: // UP
        is2D = true;
        return JmolAdapter.ORDER_STEREO_NEAR;
      case 3: // DOWN, V3000
      case 6: // DOWN
        is2D = true;
        return JmolAdapter.ORDER_STEREO_FAR;
      case 2: // either, V3000
      case 4: // either
        is2D = true;
        return JmolAdapter.ORDER_STEREO_EITHER;
      }
      break;
    case 2:
    case 3:
      nDouble++;
      break;
    case 4:
      return JmolAdapter.ORDER_AROMATIC;
    case 5:
      return JmolAdapter.ORDER_PARTIAL12;
    case 6:
      return JmolAdapter.ORDER_AROMATIC_SINGLE;
    case 7:
      return JmolAdapter.ORDER_AROMATIC_DOUBLE;
    case 8:
    case 9: // haptic
      return JmolAdapter.ORDER_PARTIAL01;
    case 14:  // added ad hoc 
      return JmolAdapter.ORDER_COVALENT_QUAD;
    case 15:  // added ad hoc 
      return JmolAdapter.ORDER_COVALENT_QUINT;
    case 16:  // added ad hoc
      return JmolAdapter.ORDER_COVALENT_HEX;
    }
    return order;
  }

  public Bond addMolBond(String iAtom1, String iAtom2, int order, int stereo) {
    order = fixOrder(order, stereo);
    if (haveAtomSerials)
      return asc.addNewBondFromNames(iAtom1, iAtom2, order);
    return asc.addNewBondWithOrder(iatom0 + parseIntStr(iAtom1) - 1,
        iatom0 + parseIntStr(iAtom2) - 1, order);
  }

}
