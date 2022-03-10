/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-03-12 21:37:51 -0600 (Sun, 12 Mar 2006) $
 * $Revision: 4586 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

package org.jmol.shapespecial;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.V3;

import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.BondIterator;
import org.jmol.script.T;
import org.jmol.shape.Shape;
import org.jmol.util.C;
import org.jmol.util.Edge;
import org.jmol.util.JmolMolecule;
import org.jmol.util.Logger;



public class Dipoles extends Shape {

  final static short DEFAULT_MAD = 10;
  final static float DEFAULT_OFFSETSIDE = 0.40f;

  public int dipoleCount = 0;
  public Dipole[] dipoles = new Dipole[4];

  private Dipole currentDipole;
  private Dipole tempDipole;
  private P3 startCoord = new P3();
  private P3 endCoord = new P3();
  private float dipoleValue;
  private boolean isUserValue;
  private boolean isBond;
  private boolean iHaveTwoEnds;
  private int atomIndex1;
  private int atomIndex2;
  private short colix;
  private V3 calculatedDipole;
  private String wildID;
  private short mad;  

  @Override
  public void initShape() {
    // nothing to do  
  }
  
  @Override
  public void setProperty(String propertyName, Object value, BS bs) {

    if ("init" == propertyName) {
      tempDipole = new Dipole();
      tempDipole.dipoleValue = 1;
      tempDipole.mad = DEFAULT_MAD;
      atomIndex1 = -1;
      tempDipole.modelIndex = -1;
      dipoleValue = 0;
      calculatedDipole = null;
      mad = -1;
      isUserValue = isBond = iHaveTwoEnds = false;
      return;
    }

    if ("calculate" == propertyName) {
      try {
        calculatedDipole = vwr.calculateMolecularDipole((BS) value);
      } catch (Exception e) {
      }
      Logger.info("calculated molecular dipole = " + calculatedDipole + " "
          + (calculatedDipole == null ? "" : "" + calculatedDipole.length()));
      return;
    }

    if ("thisID" == propertyName) {
      wildID = null;
      String thisID = (String) value;
      if (thisID == null || PT.isWild(thisID)) {
        currentDipole = null;
        if (thisID != null)
          wildID = thisID.toUpperCase();
        return;
      }
      currentDipole = findDipole(thisID);
      if (currentDipole == null)
        currentDipole = allocDipole(thisID, "");
      tempDipole = currentDipole;
      if (thisID.equals("molecular"))
        getMolecular(null);
      return;
    }

    if ("bonds" == propertyName) {
      isBond = true;
      currentDipole = null;
      for (int i = dipoleCount; --i >= 0;)
        if (isBondDipole(i))
          return;
      getBondDipoles(); // only once if any bond dipoles are defined
      return;
    }

    if ("all" == propertyName) {
      tempDipole.lstDipoles = new Lst<Object>();
      return;
    }

    if ("on" == propertyName) {
      setPropertyTok(T.on, isBond, 0, 0);
      return;
    }

    if ("off" == propertyName) {
      setPropertyTok(T.off, isBond, 0, 0);
      return;
    }

    if ("delete" == propertyName) {
      if (wildID == null && currentDipole == null) {
        clear(false);
        return;
      }
      setPropertyTok(T.delete, isBond, 0, 0);
      return;
    }

    if ("width" == propertyName) {
      mad = tempDipole.mad = (short) (((Float) value).floatValue() * 1000);
      if (currentDipole == null)
        setPropertyTok(T.wireframe, isBond, mad, 0); //
      return;
    }

    if ("offset" == propertyName) {
      float offset = tempDipole.offsetAngstroms = ((Float) value).floatValue();
      if (currentDipole == null)
        setPropertyTok(T.axes, isBond, 0, offset);
      return;
    }

    if ("offsetPt" == propertyName) {
      tempDipole.offsetPt = (P3) value;
      if (currentDipole != null) {
        currentDipole.setOffsetPt(tempDipole.offsetPt);
      }
      return;
    }

    if ("offsetPercent" == propertyName) {
      int offsetPercent = tempDipole.offsetPercent = ((Integer) value)
          .intValue();
      if (tempDipole.dipoleValue != 0)
        tempDipole.offsetAngstroms = offsetPercent / 100f
            * tempDipole.dipoleValue;
      if (currentDipole == null)
        setPropertyTok(T.percent, isBond, 0, offsetPercent / 100f);
      return;
    }

    if ("offsetSide" == propertyName) {
      float offsetSide = ((Float) value).floatValue();
      setPropertyTok(T.sidechain, isBond, 0, offsetSide);
      return;
    }

    if ("cross" == propertyName) {
      setPropertyTok(T.cross, isBond, (((Boolean) value).booleanValue() ? 1 : 0),
          0);
      return;
    }

    if ("color" == propertyName) {
      colix = C.getColixO(value);
      if (isBond) {
        setColixDipole(colix, Edge.BOND_COVALENT_MASK, bs);
      } else if (value != null) {
        setPropertyTok(T.color, false, 0, 0);
      }
      return;
    }

    if ("translucency" == propertyName) {
      setPropertyTok(T.translucent, isBond, (value.equals("translucent") ? 1 : 0),
          0);
      return;
    }

    if ("clear" == propertyName) {
      currentDipole = null;
      clear(false);
    }

    if ("clearBonds" == propertyName) {
      clear(true);
    }

    if ("startSet" == propertyName) {
      BS bsAtoms = (BS) value;
      endCoord = null;
      startCoord = ms.getAtomSetCenter(bsAtoms);
      tempDipole.set2Value(startCoord, P3.new3(0, 0, 0), dipoleValue);
      if (bsAtoms.cardinality() == 1)
        atomIndex1 = bsAtoms.nextSetBit(0);
      return;
    }

    if ("atomBitset" == propertyName) {
      BS atomset = (BS) value;
      switch (atomset.cardinality()) {
      case 0:
        return;
      case 1:
        break;
      case 2:
        atomIndex1 = atomset.nextSetBit(0);
        startCoord = ms.at[atomIndex1];
        atomset.clear(atomIndex1);
        break;
      default:
        getMolecular(atomset);
        return;
      }
      propertyName = "endSet";
      //passes to endSet
    }

    if ("endSet" == propertyName) {
      iHaveTwoEnds = true;
      BS atomset = (BS) value;
      if (atomIndex1 >= 0 && atomset.cardinality() == 1) {
        atomIndex2 = atomset.nextSetBit(0);
        tempDipole.set2AtomValue(ms.at[atomIndex1], ms.at[atomIndex2], 1);
        currentDipole = findDipoleFor(tempDipole.thisID, tempDipole.dipoleInfo);
        tempDipole.thisID = currentDipole.thisID;
        if (isSameAtoms(currentDipole, tempDipole.dipoleInfo)) {
          tempDipole = currentDipole;
          if (dipoleValue > 0)
            tempDipole.dipoleValue = dipoleValue;
          if (mad > 0)
            tempDipole.mad = mad;
        }
      } else {
        tempDipole.set2Value(startCoord, ms.getAtomSetCenter(atomset),
            dipoleValue);
      }
      //NOTTTTTT!!!! currentDipole = tempDipole;
      return;
    }

    if ("startCoord" == propertyName) {
      startCoord.setT((P3) value);
      tempDipole.set2Value(startCoord, P3.new3(0, 0, 0), dipoleValue);
      return;
    }

    if ("endCoord" == propertyName) {
      iHaveTwoEnds = true;
      endCoord.setT((P3) value);
      tempDipole.set2Value(startCoord, endCoord, dipoleValue);
      dumpDipoles("endCoord");
      return;
    }

    if ("value" == propertyName) {
      dipoleValue = ((Float) value).floatValue();
      isUserValue = true;
      tempDipole.setValue(dipoleValue);
      if (tempDipole.offsetPercent != 0)
        tempDipole.offsetAngstroms = tempDipole.offsetPercent / 100f
            * tempDipole.dipoleValue;
      return;
    }

    if ("set" == propertyName) {
      if (isBond || !iHaveTwoEnds && tempDipole.bsMolecule == null)
        return;
      setDipole();
      setModelIndex();
      return;
    }

    if (propertyName == "deleteModelAtoms") {
      int modelIndex = ((int[]) ((Object[]) value)[2])[0];
      for (int i = dipoleCount; --i >= 0;)
        if (dipoles[i].modelIndex > modelIndex) {
          dipoles[i].modelIndex--;
        } else if (dipoles[i].modelIndex == modelIndex) {
          if (dipoles[i] == currentDipole)
            currentDipole = null;
          dipoles = (Dipole[]) AU.deleteElements(dipoles, i, 1);
          dipoleCount--;
        }
      currentDipole = null;
      return;
    }

  }

  private void getMolecular(BS bsMolecule) {
    V3 v = (bsMolecule == null ? calculatedDipole : null);
    if (v == null && bsMolecule == null) {
      v = vwr.getModelDipole();
      Logger.info("file molecular dipole = " + v + " "
          + (v != null ? "" + v.length() : ""));
    }
    if (v == null)
      try {
        calculatedDipole = v = vwr.calculateMolecularDipole(bsMolecule);
      } catch (Exception e) {
      }
    if (v == null) {
      Logger
          .warn("No molecular dipole found for this model; setting to {0 0 0}");
      v = new V3();
    }
    tempDipole.bsMolecule = bsMolecule;
    tempDipole.setPtVector(P3.new3(0, 0, 0), V3.new3(-v.x, -v.y, -v.z));
    if (tempDipole.lstDipoles != null) {
      getAllMolecularDipoles(bsMolecule);
    }    
    tempDipole.type = Dipole.DIPOLE_TYPE_MOLECULAR;
    if (currentDipole == null || currentDipole.thisID == null || bsMolecule == null)
      tempDipole.thisID = "molecular";
    setDipole();
  }

  private void getAllMolecularDipoles(BS bsAtoms) {
    JmolMolecule[] mols = ms.getMolecules();
    for (int i = mols.length; --i >= 0;) {
      JmolMolecule m = mols[i];
      if (m.atomList.intersects(bsAtoms)) {
        V3 v = null;
        try {
          v = vwr.calculateMolecularDipole(m.atomList);
        } catch (Exception e) {
        }
        if (v == null)
          continue;
        P3 center = ms.getAtomSetCenter(m.atomList);
        tempDipole.lstDipoles.addLast(new Object[] {v, center, m.atomList});        
      }
    }
  }

  private void setPropertyTok(int tok, boolean bondOnly, int iValue, float fValue) {
    if (currentDipole != null)
      setPropertyFor(tok, currentDipole, iValue, fValue);
    else {
      for (int i = dipoleCount; --i >= 0;)
        if (!bondOnly || isBondDipole(i))
          if (wildID == null
              || PT.isMatch(dipoles[i].thisID.toUpperCase(), wildID,
                  true, true))
            setPropertyFor(tok, dipoles[i], iValue, fValue);
    }
  }

  private void setPropertyFor(int tok, Dipole dipole, int iValue, float fValue) {
    switch (tok) {
    case T.on:
      dipole.visible = true;
      return;
    case T.off:
      dipole.visible = false;       
      return;
    case T.delete:
      deleteDipole(dipole);
      return;
    case T.wireframe:
      dipole.mad = tempDipole.mad = (short) iValue;
      return;
    case T.axes:
      dipole.offsetAngstroms = fValue;
      return;
    case T.percent:
      dipole.offsetAngstroms = fValue * dipole.dipoleValue;
      return;
    case T.sidechain:
      dipole.offsetSide = fValue;
      return;
    case T.cross:
      dipole.noCross = (iValue == 0);
      return;
    case T.color:
      dipole.colix = colix;
      return;
    case T.translucent:
      dipole.setTranslucent(iValue == 1, translucentLevel);
      return;
    }
    Logger.error("Unkown dipole property! " + T.nameOf(tok));
  }

//  @SuppressWarnings("unchecked")
  @Override
  public boolean getPropertyData(String property, Object[] data) {
    if (property == "getNames") {
      /* just implemented for MeshCollection
      Map<String, Token> map = (Map<String, Token>) data[0];
      boolean withDollar = ((Boolean) data[1]).booleanValue();
      for (int i = dipoleCount; --i >= 0;)
        map.put((withDollar ? "$" : "") + dipoles[i].thisID, Token.tokenAnd); // just a placeholder
      return true;
      */
    }
    if (property == "checkID") {
      String key = ((String) data[0]).toUpperCase();
      boolean isWild = PT.isWild(key);
      for (int i = dipoleCount; --i >= 0;) {
        String id = dipoles[i].thisID;
        if (id.equalsIgnoreCase(key) || isWild
            && PT.isMatch(id.toUpperCase(), key, true, true)) {
          data[1] = id;
          return true;
        }
      }
      return false;
    }
    return getPropShape(property, data);
  }
  
  @Override
  public Object getProperty(String property, int index) {
    if (property.equals("list")) {
      return getShapeState();
    }
    return null;
  }

  private void getBondDipoles() {
    float[] partialCharges = ms.getPartialCharges();
    if (partialCharges == null)
      return;
    clear(true);
    Bond[] bonds = ms.bo;
    for (int i = ms.bondCount; --i >= 0;) {
      Bond bond = bonds[i];
      if (!bond.isCovalent())
        continue;
      float c1 = partialCharges[bond.atom1.i];
      float c2 = partialCharges[bond.atom2.i];
      if (c1 != c2)
        setDipoleAtoms(bond.atom1, bond.atom2, c1, c2);
    }
  }
  
  private boolean isBondDipole(int i) {
    if (i >= dipoles.length || dipoles[i] == null)
      return false;
    return (dipoles[i].isBondType());
  }

  private void setColixDipole(short colix, int bondTypeMask, BS bs) {
    if (colix == C.USE_PALETTE)
      return; // not implemented
    BondIterator iter = ms.getBondIteratorForType(bondTypeMask, bs);
    while (iter.hasNext()) {
      Dipole d = findBondDipole(iter.next());
      if (d != null)
        d.colix = colix;
    }
  }

  private void setDipole() {
    if (currentDipole == null)
      currentDipole = allocDipole("", "");
    currentDipole.set(tempDipole);
    currentDipole.isUserValue = isUserValue;
    currentDipole.modelIndex = vwr.am.cmi;
  }

  final private static float E_ANG_PER_DEBYE = 0.208194f;

  private void setDipoleAtoms(Atom atom1, Atom atom2, float c1, float c2) {
    Dipole dipole = findAtomDipole(atom1, atom2, true);
    float value = (c1 - c2) / 2f * atom1.distance(atom2) / E_ANG_PER_DEBYE;
    if (value < 0) {
      dipole.set2AtomValue(atom2, atom1, -value);
    } else {
      dipole.set2AtomValue(atom1, atom2, value);
    }
    dipole.type = Dipole.DIPOLE_TYPE_BOND;
    dipole.modelIndex = atom1.mi;
  }

  private int getDipoleIndexFor(String dipoleInfo, String thisID) {
    if (dipoleInfo != null && dipoleInfo.length() > 0)
      for (int i = dipoleCount; --i >= 0;)
        if (isSameAtoms(dipoles[i], dipoleInfo))
          return i;
    return getIndexFromName(thisID);
  }

  private boolean isSameAtoms(Dipole dipole, String dipoleInfo) {
    // order-independent search for two atoms:
    // looking for (xyz)(x'y'z') in (xyz)(x'y'z')(xyz)(x'y'z')
    return (dipole != null && dipole.isBondType() && (dipole.dipoleInfo + dipole.dipoleInfo)
        .indexOf(dipoleInfo) >= 0);
  }

  private int getDipoleIndex(int atomIndex1, int atomIndex2) {
    for (int i = dipoleCount; --i >= 0;) {
      if (dipoles[i] != null
          && dipoles[i].atoms[0] != null
          && dipoles[i].atoms[1] != null
          && (dipoles[i].atoms[0].i == atomIndex1
              && dipoles[i].atoms[1].i == atomIndex2 || dipoles[i].atoms[1]
              .i == atomIndex1
              && dipoles[i].atoms[0].i == atomIndex2))
        return i;
    }
    return -1;
  }

  private void deleteDipole(Dipole dipole) {
    if (dipole == null)
      return;
    if (currentDipole == dipole)
      currentDipole = null;
    int i;
    for (i = dipoleCount; dipoles[--i] != dipole;) {
    }
    if (i < 0)
      return;
    for (int j = i + 1; j < dipoleCount; ++j)
      dipoles[j - 1] = dipoles[j];
    dipoles[--dipoleCount] = null;
  }

  private Dipole findDipole(String thisID) {
    int dipoleIndex = getIndexFromName(thisID);
    if (dipoleIndex >= 0) {
      return dipoles[dipoleIndex];
    }
    return null;
  }

  private Dipole findAtomDipole(Atom atom1, Atom atom2, boolean doAllocate) {
    int dipoleIndex = getDipoleIndex(atom1.i, atom2.i);
    if (dipoleIndex >= 0) {
      return dipoles[dipoleIndex];
    }
    return (doAllocate ? allocDipole("", "") : null);
  }

  private Dipole findBondDipole(Bond bond) {
    Dipole d = findAtomDipole(bond.atom1, bond.atom2, false);
    return (d == null || d.atoms[0] == null ? null : d);
  }

  private Dipole findDipoleFor(String thisID, String dipoleInfo) {
    // must be able to identify a dipole from its ID only SECONDARILY,
    // as we want one dipole per bond. So we look for coord ID.
    int dipoleIndex = getDipoleIndexFor(dipoleInfo, thisID);
    if (dipoleIndex >= 0) {
      if (thisID.length() > 0)
        dipoles[dipoleIndex].thisID = thisID;
      return dipoles[dipoleIndex];
    }
    return allocDipole(thisID, dipoleInfo);
  }

  private Dipole allocDipole(String thisID, String dipoleInfo) {
    dipoles = (Dipole[]) AU.ensureLength(dipoles, dipoleCount + 1);
    if (thisID == null || thisID.length() == 0)
      thisID = "dipole" + (dipoleCount + 1);
    Dipole d = dipoles[dipoleCount++] = new Dipole().init(vwr.am.cmi, thisID,
        dipoleInfo, colix, DEFAULT_MAD, true);
    return d;
  }

  private void dumpDipoles(String msg) {
    for (int i = dipoleCount; --i >= 0;) {
      Dipole dipole = dipoles[i];
      Logger.info("\n\n" + msg + " dump dipole " + i + " " + dipole + " "
          + dipole.thisID + " " + dipole.dipoleInfo + " "
          + dipole.visibilityFlags + " mad=" + dipole.mad + " vis="
          + dipole.visible + "\n orig" + dipole.origin + " " + " vect"
          + dipole.vector + " val=" + dipole.dipoleValue);
    }
    if (currentDipole != null)
      Logger.info(" current = " + currentDipole + currentDipole.origin);
    if (tempDipole != null)
      Logger.info(" temp = " + tempDipole + " " + tempDipole.origin);
  }

  private void clear(boolean clearBondDipolesOnly) {
    if (clearBondDipolesOnly) {
      for (int i = dipoleCount; --i >= 0;)
        if (isBondDipole(i))
          deleteDipole(dipoles[i]);
      return;
    }
    for (int i = dipoleCount; --i >= 0;)
      if (!isBond || isBondDipole(i))
        deleteDipole(dipoles[i]);
  }

  @Override
  public int getIndexFromName(String thisID) {
    if (thisID == null)
      return -1;
    for (int i = dipoleCount; --i >= 0;) {
      if (dipoles[i] != null && thisID.equals(dipoles[i].thisID))
        return i;
    }
    return -1;
  }

  @Override
  public Object getShapeDetail() {
    Lst<Map<String, Object>> V = new  Lst<Map<String,Object>>();
    Map<String, Object> atomInfo;
    P3 ptTemp = new P3();
    for (int i = 0; i < dipoleCount; i++) {
      Map<String, Object> info = new Hashtable<String, Object>();
      Dipole dipole = dipoles[i];
      info.put("ID", dipole.thisID);
      info.put("vector", dipole.vector);
      info.put("origin", dipole.origin);
      if (dipole.bsMolecule != null) {
        info.put("bsMolecule", dipole.bsMolecule);
      } else if (dipole.atoms[0] != null) {
        atomInfo = new Hashtable<String, Object>();
        ms.getAtomIdentityInfo(dipole.atoms[0].i, atomInfo, ptTemp);
        Lst<Map<String, Object>> atoms = new  Lst<Map<String,Object>>();
        atoms.addLast(atomInfo);
        atomInfo = new Hashtable<String, Object>();
        ms.getAtomIdentityInfo(dipole.atoms[1].i, atomInfo, ptTemp);
        atoms.addLast(atomInfo);
        info.put("atoms", atoms);
        info.put("magnitude", Float.valueOf(dipole.vector.length()));
      }
      V.addLast(info);
    }
    return V;
  }

  private void setModelIndex() {
    if (currentDipole == null)
      return;
    currentDipole.visible = true;
    currentDipole.modelIndex = vwr.am.cmi;
  }

  @Override
  public void setModelVisibilityFlags(BS bsModels) {
    /*
     * set all fixed objects visible; others based on model being displayed
     * 
     */
    for (int i = dipoleCount; --i >= 0;) {
      Dipole dipole = dipoles[i];
      dipole.visibilityFlags = ((dipole.modelIndex < 0 || bsModels
          .get(dipole.modelIndex))
          && dipole.mad != 0
          && dipole.visible
          && dipole.origin != null
          && dipole.vector != null
          && dipole.vector.length() != 0
          && dipole.dipoleValue != 0 ? vf : 0);
    }
    //dumpDipoles("setVis");
  }

  @Override
  public String getShapeState() {
    if (dipoleCount == 0)
      return "";
    SB s = new SB();
    int thisModel = -1;
    int modelCount = ms.mc;
    for (int i = 0; i < dipoleCount; i++) {
      Dipole dipole = dipoles[i];
      if (dipole.isValid) {
        if (modelCount > 1 && dipole.modelIndex != thisModel)
          appendCmd(s, "frame "
              + vwr.getModelNumberDotted(thisModel = dipole.modelIndex));
        s.append(dipole.getShapeState());
        appendCmd(s, getColorCommandUnk("dipole", dipole.colix, translucentAllowed));
      }
    }
    return s.toString();
  }
}
