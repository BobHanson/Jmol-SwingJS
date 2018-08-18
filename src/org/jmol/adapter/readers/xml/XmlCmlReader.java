/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-16 09:53:18 -0500 (Sat, 16 Sep 2006) $
 * $Revision: 5561 $
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
package org.jmol.adapter.readers.xml;

import org.jmol.adapter.smarter.Bond;
import org.jmol.adapter.smarter.Atom;

import java.util.Hashtable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;

import javajs.util.Lst;
import javajs.util.PT;

import javajs.util.BS;
import org.jmol.api.JmolAdapter;
import org.jmol.util.BSUtil;
import org.jmol.util.Logger;

/**
 * A CML2 Reader - 
 * If passed a bufferedReader (from a file or inline string), we
 * generate a SAX parser and use callbacks to construct an
 * AtomSetCollection.
 * If passed a JSObject (from LiveConnect) we treat it as a JS DOM
 * tree, and walk the tree, (using the same processing as the SAX
 * parser) to construct the AtomSetCollection.
 * 
 * symmetry added by Bob Hanson:
 * 
 *  setSpaceGroupName()
 *  setUnitCellItem()
 *  setFractionalCoordinates()
 *  setAtomCoord()
 *  applySymmetryAndSetTrajectory()
 *
 *
 * "isotope" added 4/6/2009 Bob Hanson
 * 
 */

/* TODO 9/06
 * 
 *  
 *  We need to implement the capability to load a specific
 *  model as well as indicate the number of unit cells to load. 
 *  
 * Follow the equivalent in CIF files to see how this is done. 
 * 
 */

public class XmlCmlReader extends XmlReader {

  public XmlCmlReader() {
  }

  private String scalarDictRef;
  //String scalarDictKey;
  private String scalarDictValue;
  private String scalarTitle;
  private String cellParameterType;
  private boolean checkedSerial;
  private boolean isSerial;

  // counter that is incremented each time a molecule element is started and 
  // decremented when finished.  Needed so that only 1 atomSet created for each
  // parent molecule that exists.
  private int moleculeNesting = 0;
  private int latticeVectorPtr = 0;
  private boolean embeddedCrystal = false;
  private Properties atomIdNames;


  ////////////////////////////////////////////////////////////////
  // Main body of class; variables & functions shared by DOM & SAX alike.

  protected String[] tokens = new String[16];

  // the same atom array gets reused
  // it will grow to the maximum length;
  // ac holds the current number of atoms
  private int aaLen;
  private Atom[] atomArray = new Atom[100];

  private int bondCount;
  private Bond[] bondArray = new Bond[100];

  // the same string array gets reused
  // tokenCount holds the current number of tokens
  // see breakOutTokens
  private int tokenCount;
  //private int nModules = 0;
  private int moduleNestingLevel = 0;
  private boolean haveMolecule = false;
  private String localSpaceGroupName;
  protected boolean processing = true;
  protected int state = START;
  private int atomIndex0;
  private Lst<String[]> joinList;
  private Map<Atom, String> mapRtoA;
  private BS deleteAtoms;
  protected String moleculeID;
  
  protected Map<String, Object> htModelAtomMap;
  private boolean optimize2d;

  /**
   * state constants
   */
  final static protected int START = 0, 
    CML = 1, 
    CRYSTAL = 2, 
    CRYSTAL_SCALAR = 3,
    CRYSTAL_SYMMETRY = 4, 
    CRYSTAL_SYMMETRY_TRANSFORM3 = 5, 
    MOLECULE = 6,
    MOLECULE_ATOM_ARRAY = 7, 
    MOLECULE_ATOM = 8, 
    MOLECULE_ATOM_SCALAR = 9,
    MOLECULE_BOND_ARRAY = 10, 
    MOLECULE_BOND = 11, 
    MOLECULE_BOND_STEREO = 12, 
    MOLECULE_FORMULA = 13,
    MOLECULE_ATOM_BUILTIN = 14, 
    MOLECULE_BOND_BUILTIN = 15,
    MODULE = 16,
    SYMMETRY = 17,
    LATTICE_VECTOR = 18,
    ASSOCIATION = 19;
  /**
   * the current state
   */
  /*
   * added 2/2007  Bob Hanson:
   * 
   * <crystal id="struct" dictRef="castep:ucell">
      <cellParameter latticeType="real" parameterType="length"
        units="castepunits:a">4.592100143433e0 4.592100143433e0 2.957400083542e0</cellParameter>
      <cellParameter latticeType="real" parameterType="angle"
        units="castepunits:degree">9.000000000000e1 9.000000000000e1 9.000000000000e1</cellParameter>
    </crystal>

   * 
   */

  @Override
  protected void processXml(XmlReader parent,
                            Object saxReader) throws Exception {
    optimize2d = parent.checkFilterKey("2D");
    processXml2(parent, saxReader);
    if (optimize2d)
      set2D();
  }

  @Override
  public void processStartElement(String name, String nodeName) {
    if (!processing)
      return;
    processStart2(name);
  }

  protected void processStart2(String name) {
    name = name.toLowerCase();
    String val;
    switch (state) {
    case START:
      if (name.equals("molecule")) {
        moleculeID = atts.get("id");
        state = MOLECULE;
        haveMolecule = true;
        if (moleculeNesting == 0)
          createNewAtomSet();
        moleculeNesting++;
      } else if (name.equals("crystal")) {
        state = CRYSTAL;
      } else if (name.equals("symmetry")) {
        state = SYMMETRY;
        if ((val = atts.get("spacegroup")) != null) {
          localSpaceGroupName = val;
        } else {
          localSpaceGroupName = "P1";
          parent.clearUnitCell();
        }
      } else if (name.equals("module")) {
        moduleNestingLevel++;
        //nModules++;
      } else if (name.equals("latticevector")) {
        state = LATTICE_VECTOR;
        setKeepChars(true);
      }

      break;
    case CRYSTAL:
      // we force this to be NOT serialized by number, because we might have a1 and a1_....
      checkedSerial = true;
      isSerial = false;
      if (name.equals("scalar")) {
        state = CRYSTAL_SCALAR;
        setKeepChars(true);
        scalarTitle = atts.get("title");
        getDictRefValue();
      } else if (name.equals("symmetry")) {
        state = CRYSTAL_SYMMETRY;
        if ((val = atts.get("spacegroup")) != null) {
          localSpaceGroupName = val;
          for (int i = 0; i < localSpaceGroupName.length(); i++)
            if (localSpaceGroupName.charAt(i) == '_')
              localSpaceGroupName = localSpaceGroupName.substring(0, i)
                  + localSpaceGroupName.substring((i--) + 1);
        }
      } else if (name.equals("cellparameter")) {
        if ((val = atts.get("parametertype")) != null) {
          cellParameterType = val;
          setKeepChars(true);
        }
      }
      break;
    case LATTICE_VECTOR:
      /*
       * <lattice dictRef="castep:latticeVectors"> <latticeVector
       * units="castepunits:A" dictRef="cml:latticeVector">1.980499982834e0
       * 3.430000066757e0 0.000000000000e0</latticeVector> <latticeVector
       * units="castepunits:A" dictRef="cml:latticeVector">-1.980499982834e0
       * 3.430000066757e0 0.000000000000e0</latticeVector> <latticeVector
       * units="castepunits:A" dictRef="cml:latticeVector">0.000000000000e0
       * 0.000000000000e0 4.165999889374e0</latticeVector> </lattice>
       */
      setKeepChars(true);
      break;
    case SYMMETRY:
    case CRYSTAL_SCALAR:
    case CRYSTAL_SYMMETRY:
      if (name.equals("transform3")) {
        state = CRYSTAL_SYMMETRY_TRANSFORM3;
        setKeepChars(true);
      }
      break;
    case CRYSTAL_SYMMETRY_TRANSFORM3:
    case MOLECULE:
      if (name.equals("fragmentlist")) {
        joinList = new Lst<String[]>();
        mapRtoA = new Hashtable<Atom, String>();
        if (deleteAtoms == null)
          deleteAtoms = new BS();
      } else if (name.equals("crystal")) {
        state = CRYSTAL;
        embeddedCrystal = true;
      } else if (name.equals("molecule")) {
        state = MOLECULE;
        moleculeNesting++;
      } else if (name.equals("join")) {
        int order = -1;
        tokenCount = 0;
        if ((val = atts.get("atomrefs2")) != null) {
          breakOutTokens(val);
          if ((val = atts.get("order")) != null)
            order = parseBondToken(val);
          if (tokenCount == 2 && order > 0)
            joinList.addLast(new String[] { tokens[0], tokens[1], "" + order });
        }
      } else if (name.equals("bondarray")) {
        state = MOLECULE_BOND_ARRAY;
        bondCount = 0;
        if ((val = atts.get("order")) != null) {
          breakOutBondTokens(val);
          for (int i = tokenCount; --i >= 0;)
            bondArray[i].order = parseBondToken(tokens[i]);
        }
        if ((val = atts.get("atomref1")) != null) {
          breakOutBondTokens(val);
          for (int i = tokenCount; --i >= 0;)
            bondArray[i].atomIndex1 = getAtomIndex(tokens[i]);
        }
        if ((val = atts.get("atomref2")) != null) {
          breakOutBondTokens(val);
          for (int i = tokenCount; --i >= 0;)
            bondArray[i].atomIndex2 = getAtomIndex(tokens[i]);
        }
      } else if (name.equals("atomarray")) {
        state = MOLECULE_ATOM_ARRAY;
        aaLen = 0;
        boolean coords3D = false;
        if ((val = atts.get("atomid")) != null) {
          breakOutAtomTokens(val);
          for (int i = tokenCount; --i >= 0;)
            atomArray[i].atomName = tokens[i];
        }
        boolean is3d = (!optimize2d && (val = atts.get("x3")) != null);
        if (is3d) {
          is3d = true;
          coords3D = true;
          breakOutAtomTokens(val);
          for (int i = tokenCount; --i >= 0;)
            atomArray[i].x = parseFloatStr(tokens[i]);
          if ((val = atts.get("y3")) != null) {
            breakOutAtomTokens(val);
            for (int i = tokenCount; --i >= 0;)
              atomArray[i].y = parseFloatStr(tokens[i]);
          }
          if ((val = atts.get("z3")) != null) {
            breakOutAtomTokens(val);
            for (int i = tokenCount; --i >= 0;)
              atomArray[i].z = parseFloatStr(tokens[i]);
          }
        } else {
          if ((val = atts.get("x2")) != null) {
            breakOutAtomTokens(val);
            for (int i = tokenCount; --i >= 0;)
              atomArray[i].x = parseFloatStr(tokens[i]);
          }
          if ((val = atts.get("y2")) != null) {
            breakOutAtomTokens(val);
            for (int i = tokenCount; --i >= 0;)
              atomArray[i].y = parseFloatStr(tokens[i]);
          }
        }
        if ((val = atts.get("elementtype")) != null) {
          breakOutAtomTokens(val);
          for (int i = tokenCount; --i >= 0;)
            atomArray[i].elementSymbol = tokens[i];
        }
        for (int i = aaLen; --i >= 0;) {
          Atom atom = atomArray[i];
          if (!coords3D)
            atom.z = 0;
          addAtom(atom);
        }
      } else if (name.equals("formula")) {
        state = MOLECULE_FORMULA;
      } else if (name.equals("association")) {
        state = ASSOCIATION;
      }
      break;
    case MOLECULE_BOND_ARRAY:
      if (name.equals("bond")) {
        state = MOLECULE_BOND;
        int order = -1;
        tokenCount = 0;
        if ((val = atts.get("atomrefs2")) != null)
          breakOutTokens(val);
        if ((val = atts.get("order")) != null)
          order = parseBondToken(val);
        if (tokenCount == 2 && order > 0) {
          addNewBond(tokens[0], tokens[1], order);
        }
      }
      break;
    case MOLECULE_ATOM_ARRAY:
      if (name.equals("atom")) {
        state = MOLECULE_ATOM;
        atom = new Atom();
        parent.setFractionalCoordinates(false);
        String id = atts.get("id");
        if ((val = atts.get("name")) != null)
          atom.atomName = val;
        else if ((val = atts.get("title")) != null)
          atom.atomName = val;
        else if ((val = atts.get("label")) != null)
          atom.atomName = val;
        else
          atom.atomName = id;
        if (!checkedSerial) {
          // this is important because the atomName may not be unique
          // (as in PDB files)
          // but it causes problems in cif-derived files that involve a1 and a1_1, for instance
          isSerial = (id != null && id.length() > 1 && id.startsWith("a") && PT
              .parseInt(id.substring(1)) != Integer.MIN_VALUE);
          checkedSerial = true;
        }
        if (isSerial)
          atom.atomSerial = PT.parseInt(id.substring(1));
        if ((val = atts.get("xfract")) != null
            && (parent.iHaveUnitCell || !atts.containsKey("x3"))) {
          parent.setFractionalCoordinates(true);
          atom.set(parseFloatStr(val), parseFloatStr(atts.get("yfract")),
              parseFloatStr(atts.get("zfract")));
        } else if ((val = atts.get("x3")) != null) {
          atom.set(parseFloatStr(val), parseFloatStr(atts.get("y3")),
              parseFloatStr(atts.get("z3")));
        } else if ((val = atts.get("x2")) != null) {
          atom.set(parseFloatStr(val), parseFloatStr(atts.get("y2")), 0);
        }
        if ((val = atts.get("elementtype")) != null) {
          String sym = val;
          if ((val = atts.get("isotope")) != null)
            atom.elementNumber = (short) ((parseIntStr(val) << 7) + JmolAdapter
                .getElementNumber(sym));
          atom.elementSymbol = sym;
        }
        if ((val = atts.get("formalcharge")) != null)
          atom.formalCharge = parseIntStr(val);
      }

      break;
    case MOLECULE_BOND:
      if ((val = atts.get("builtin")) != null) {
        setKeepChars(true);
        state = MOLECULE_BOND_BUILTIN;
        scalarDictValue = val;
      } else if (name.equals("bondstereo")) {
        state = MOLECULE_BOND_STEREO;
      }
      break;
    case MOLECULE_BOND_STEREO:
      setKeepChars(true);
      state = MOLECULE_BOND_STEREO;
      break;
    case MOLECULE_ATOM:
      if (name.equals("scalar")) {
        state = MOLECULE_ATOM_SCALAR;
        setKeepChars(true);
        scalarTitle = atts.get("title");
        getDictRefValue();
      } else if ((val = atts.get("builtin")) != null) {
        setKeepChars(true);
        state = MOLECULE_ATOM_BUILTIN;
        scalarDictValue = val;
      }
      break;
    case MOLECULE_ATOM_SCALAR:
      break;
    case MOLECULE_FORMULA:
      break;
    case MOLECULE_ATOM_BUILTIN:
      break;
    case MOLECULE_BOND_BUILTIN:
      break;
    }
  }

  private int getAtomIndex(String label) {
    return asc.getAtomIndex(isSerial ? label.substring(1) : label);
  }

  final private static String[] unitCellParamTags = { "a", "b", "c", "alpha",
    "beta", "gamma" };

  @Override
  void processEndElement(String name) {
    // if (!uri.equals(NAMESPACE_URI))
    // return;
    //System.out.println("END: " + name);
    if (!processing)
      return;
    processEnd2(name);
  }
  
  public void processEnd2(String name) {
    name = name.toLowerCase();
    switch (state) {
    case START:
      if (name.equals("module")) {
        if (--moduleNestingLevel == 0) {
          if (parent.iHaveUnitCell)
            applySymmetryAndSetTrajectory();
          setAtomNames();
        }
      }
      break;
    case ASSOCIATION:
      if (name.equals("association"))
        state = MOLECULE;
      break;
    case CRYSTAL:
      if (name.equals("crystal")) {
        if (embeddedCrystal) {
          state = MOLECULE;
          embeddedCrystal = false;
        } else {
          state = START;
        }
      } else if (name.equals("cellparameter") && keepChars) {
        String[] tokens = PT.getTokens(chars.toString());
        setKeepChars(false);
        if (tokens.length != 3 || cellParameterType == null) {
        } else if (cellParameterType.equals("length")) {
          for (int i = 0; i < 3; i++)
            parent.setUnitCellItem(i, parseFloatStr(tokens[i]));
          break;
        } else if (cellParameterType.equals("angle")) {
          for (int i = 0; i < 3; i++)
            parent.setUnitCellItem(i + 3, parseFloatStr(tokens[i]));
          break;
        }
        // if here, then something is wrong
        Logger.error("bad cellParameter information: parameterType="
            + cellParameterType + " data=" + chars);
        parent.setFractionalCoordinates(false);
      }
      break;
    case CRYSTAL_SCALAR:
      if (name.equals("scalar")) {
        state = CRYSTAL;
        if (scalarTitle != null)
          checkUnitCellItem(unitCellParamTags, scalarTitle);
        else if (scalarDictRef != null)
          checkUnitCellItem(JmolAdapter.cellParamNames, (scalarDictValue
              .startsWith("_") ? scalarDictValue : "_" + scalarDictValue));
      }
      setKeepChars(false);
      scalarTitle = null;
      scalarDictRef = null;
      break;
    case CRYSTAL_SYMMETRY_TRANSFORM3:
      if (name.equals("transform3")) {
        // setSymmetryOperator("xyz matrix: " + chars);
        // the problem is that these matricies are in CARTESIAN coordinates, not
        // ijk coordinates
        setKeepChars(false);
        state = CRYSTAL_SYMMETRY;
      }
      break;
    case LATTICE_VECTOR:
      float[] values = getTokensFloat(chars.toString(), null, 3);
      parent.addExplicitLatticeVector(latticeVectorPtr, values, 0);
      latticeVectorPtr = (latticeVectorPtr + 1) % 3;
      setKeepChars(false);
      state = START;
      break;
    case CRYSTAL_SYMMETRY:
    case SYMMETRY:
      if (name.equals("symmetry"))
        state = (state == CRYSTAL_SYMMETRY ? CRYSTAL : START);
      if (moduleNestingLevel == 0 && parent.iHaveUnitCell && !embeddedCrystal)
        applySymmetryAndSetTrajectory();
      break;
    case MOLECULE:
      if (name.equals("fragmentlist")) {
        for (int i = joinList.size(); --i >= 0;) {
          String[] join = joinList.get(i);
          Atom r1 = asc.getAtomFromName(fixSerialName(join[0]));
          Atom r2 = asc.getAtomFromName(fixSerialName(join[1]));
          if (r1 != null && r2 != null) {
            deleteAtoms.set(r1.index);
            deleteAtoms.set(r2.index);
            addNewBond(mapRtoA.get(r1), mapRtoA.get(r2), parseIntStr(join[2]));
          }
        }
        joinList = null;
        mapRtoA = null;
      }
      if (name.equals("molecule")) {
        if (--moleculeNesting == 0) {
          // if <molecule> is within <molecule>, then
          // we have to wait until the end of all <molecule>s to
          // apply symmetry.
          applySymmetryAndSetTrajectory();
          setAtomNames();
          state = START;
        } else {
          state = MOLECULE;
        }
      }
      break;
    case MOLECULE_BOND_ARRAY:
      if (name.equals("bondarray")) {
        state = MOLECULE;
        for (int i = 0; i < bondCount; ++i)
          addBond(bondArray[i]);
        parent.applySymmetryToBonds = true;
      }
      break;
    case MOLECULE_ATOM_ARRAY:
      if (name.equals("atomarray")) {
        state = MOLECULE;
//        for (int i = 0; i < aaLen; ++i)
  //        addAtom(atomArray[i]);
      }
      break;
    case MOLECULE_BOND:
      if (name.equals("bond")) {
        state = MOLECULE_BOND_ARRAY;
      }
      break;
    case MOLECULE_ATOM:
      if (name.equals("atom")) {
        state = MOLECULE_ATOM_ARRAY;
        addAtom(atom);
        atom = null;
      }
      break;
    case MOLECULE_ATOM_SCALAR:
      if (name.equals("scalar")) {
        state = MOLECULE_ATOM;
        if ("jmol:charge".equals(scalarDictRef)) {
          atom.partialCharge = parseFloatStr(chars.toString());
        } else if (scalarDictRef != null
            && "_atom_site_label".equals(scalarDictValue)) {
          if (atomIdNames == null)
            atomIdNames = new Properties();
          atomIdNames.put(atom.atomName, chars.toString());
        }
      }
      setKeepChars(false);
      scalarTitle = null;
      scalarDictRef = null;
      break;
    case MOLECULE_ATOM_BUILTIN:
      state = MOLECULE_ATOM;
      if (scalarDictValue.equals("x3"))
        atom.x = parseFloatStr(chars.toString());
      else if (scalarDictValue.equals("y3"))
        atom.y = parseFloatStr(chars.toString());
      else if (scalarDictValue.equals("z3"))
        atom.z = parseFloatStr(chars.toString());
      else if (scalarDictValue.equals("elementType"))
        atom.elementSymbol = chars.toString();
      setKeepChars(false);
      break;
    case MOLECULE_BOND_STEREO:
      String stereo = chars.toString();
      if (bond.order == 1)
        bond.order = (stereo.equals("H") ? JmolAdapter.ORDER_STEREO_FAR : JmolAdapter.ORDER_STEREO_NEAR);
      setKeepChars(false);
      state = MOLECULE_BOND;
      break;
    case MOLECULE_BOND_BUILTIN: // ACD Labs
      state = MOLECULE_BOND;
      if (scalarDictValue.equals("atomRef")) {
        if (tokenCount == 0)
          tokens = new String[2];
        if (tokenCount < 2)
          tokens[tokenCount++] = chars.toString();
      } else if (scalarDictValue.equals("order")) {
        int order = parseBondToken(chars.toString());
        if (order > 0 && tokenCount == 2)
          addNewBond(tokens[0], tokens[1], order);
      }
      setKeepChars(false);
      break;
    case MOLECULE_FORMULA:
      state = MOLECULE;
      break;
    }
  }

  private void addBond(Bond bond) {
    Atom a1 = asc.atoms[bond.atomIndex1];
    Atom a2 = asc.atoms[bond.atomIndex2];
    if (joinList != null && !checkBondToR(a1.atomName, a2.atomName))
      asc.addBond(bond);
  }

  /**
   * Checks to see if we have a bond to R and, if so, adds this R atom
   * as a key to its attached atom
   * @param a1name
   * @param a2name
   * @return true if handled so no need to add a bond
   */
  private boolean checkBondToR(String a1name, String a2name) {
    Atom a1 = asc.getAtomFromName(a1name);
    Atom a2 = asc.getAtomFromName(a2name);
    if (a1 == null || a2 == null)
      return true;
    if ("R".equals(a1.elementSymbol)) {
      mapRtoA.put(a1, a2.atomName);
      return true;
    } else if ("R".equals(a2.elementSymbol)) {
      mapRtoA.put(a2, a1.atomName);
      return true;
    }
    return false;
  }

  private void setAtomNames() {
      // for CML reader "a3" --> "N3"
      if (atomIdNames == null)
        return;
      String s;
      Atom[] atoms = asc.atoms;
      for (int i = atomIndex0; i < asc.ac; i++)
        if ((s = atomIdNames.getProperty(atoms[i].atomName)) != null)
          atoms[i].atomName = s;
      atomIdNames = null;
      atomIndex0 = asc.ac;
    }

  private void addNewBond(String a1, String a2, int order) {
    if (a1 == null || a2 == null)
      return;
    parent.applySymmetryToBonds = true;
    a1 = fixSerialName(a1);
    a2 = fixSerialName(a2);
    if (joinList == null || !checkBondToR(a1, a2)) {
      asc.addNewBondFromNames(a1, a2, order);
      bond = asc.bonds[asc.bondCount - 1];
    }
  }

  private String fixSerialName(String a) {
    return (isSerial ? a.substring(1) : a);
  }

  private void getDictRefValue() {
    scalarDictRef = atts.get("dictref");
    if (scalarDictRef != null) {
      int iColon = scalarDictRef.indexOf(":");
      scalarDictValue = scalarDictRef.substring(iColon + 1);
    }
  }

  private void checkUnitCellItem(String[] tags, String value) {
    for (int i = tags.length; --i >= 0;)
      if (value.equals(tags[i])) {
        parent.setUnitCellItem(i, parseFloatStr(chars.toString()));
        return;
      }
  }

  private void addAtom(Atom atom) {
    if ((atom.elementSymbol == null && atom.elementNumber < 0)
        || Float.isNaN(atom.z))
      return;
    parent.setAtomCoord(atom);
    if (htModelAtomMap != null)
      htModelAtomMap.put(moleculeID + atom.atomName, atom);
    if (isSerial)
      asc.addAtomWithMappedSerialNumber(atom);
    else
      asc.addAtomWithMappedName(atom);
  }

  private int parseBondToken(String str) {
    float floatOrder = parseFloatStr(str);
    if (Float.isNaN(floatOrder) && str.length() >= 1) {
      str = str.toUpperCase();
      switch (str.charAt(0)) {
      case 'S':
        return JmolAdapter.ORDER_COVALENT_SINGLE;
      case 'D':
        return JmolAdapter.ORDER_COVALENT_DOUBLE;
      case 'T':
        return JmolAdapter.ORDER_COVALENT_TRIPLE;
      case 'A':
        return JmolAdapter.ORDER_AROMATIC;
      case 'P':
        //TODO: Note, this could be elaborated more specifically
        return JmolAdapter.ORDER_PARTIAL12;
      }
      return parseIntStr(str);
    }
    if (floatOrder == 1.5)
      return JmolAdapter.ORDER_AROMATIC;
    if (floatOrder == 2)
      return JmolAdapter.ORDER_COVALENT_DOUBLE;
    if (floatOrder == 3)
      return JmolAdapter.ORDER_COVALENT_TRIPLE;
    return JmolAdapter.ORDER_COVALENT_SINGLE;
  }

  //this routine breaks out all the tokens in a string
  // results ar e placed into the tokens array
  private void breakOutTokens(String str) {
    StringTokenizer st = new StringTokenizer(str);
    tokenCount = st.countTokens();
    if (tokenCount > tokens.length)
      tokens = new String[tokenCount];
    for (int i = 0; i < tokenCount; ++i) {
      try {
        tokens[i] = st.nextToken();
      } catch (NoSuchElementException nsee) {
        tokens[i] = null;
      }
    }
  }

  void breakOutAtomTokens(String str) {
    breakOutTokens(str);
    checkAtomArrayLength(tokenCount);
  }

  void checkAtomArrayLength(int newAtomCount) {
    if (aaLen == 0) {
      if (newAtomCount > atomArray.length)
        atomArray = new Atom[newAtomCount];
      for (int i = newAtomCount; --i >= 0;)
        atomArray[i] = new Atom();
      aaLen = newAtomCount;
    } else if (newAtomCount != aaLen) {
      throw new IndexOutOfBoundsException("bad atom attribute length");
    }
  }

  void breakOutBondTokens(String str) {
    breakOutTokens(str);
    checkBondArrayLength(tokenCount);
  }

  void checkBondArrayLength(int newBondCount) {
    if (bondCount == 0) {
      if (newBondCount > bondArray.length)
        bondArray = new Bond[newBondCount];
      for (int i = newBondCount; --i >= 0;)
        bondArray[i] = new Bond(-1, -1, 1);
      bondCount = newBondCount;
    } else if (newBondCount != bondCount) {
      throw new IndexOutOfBoundsException("bad bond attribute length");
    }
  }

  private void createNewAtomSet() {
    asc.newAtomSet();
    String val;
    if (htModelAtomMap != null)
      htModelAtomMap.put("" + asc.iSet, "" + moleculeID);
    String collectionName = ((val = atts.get("title")) != null 
        || (val = atts.get("id")) != null ? val : null);
    if (collectionName != null) {
      asc.setAtomSetName(collectionName);
    }
  }
  
  @Override
  public void applySymmetryAndSetTrajectory() {
    if (moduleNestingLevel > 0 || !haveMolecule || localSpaceGroupName == null)
      return;
    parent.setSpaceGroupName(localSpaceGroupName);
    parent.iHaveSymmetryOperators = iHaveSymmetryOperators;
    parent.applySymmetryAndSetTrajectory();
  }

  @Override
  public void endDocument() {
    // CML reader uses this
    if (deleteAtoms != null) {
      BS bs = (asc.bsAtoms == null ? asc.bsAtoms = BSUtil.newBitSet2(0, asc.ac) : asc.bsAtoms);
      bs.andNot(deleteAtoms);
    }
  }



}
