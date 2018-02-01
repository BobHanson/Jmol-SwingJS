/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2017-12-14 17:50:54 -0600 (Thu, 14 Dec 2017) $
 * $Revision: 21781 $

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.jmol.modelset;

import javajs.util.CU;
import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.T3;
import javajs.util.V3;

import org.jmol.api.JmolDataManager;
import org.jmol.api.JmolModulationSet;
import org.jmol.api.SymmetryInterface;
import org.jmol.atomdata.RadiusData;
import org.jmol.atomdata.RadiusData.EnumType;
import org.jmol.c.PAL;
import org.jmol.c.VDW;
import javajs.util.BS;
import org.jmol.modelsetbio.BioModel;
import org.jmol.script.T;
import org.jmol.util.C;
import org.jmol.util.Edge;
import org.jmol.util.Elements;
import org.jmol.util.Node;
import org.jmol.util.Point3fi;
import org.jmol.util.Tensor;
import org.jmol.util.Vibration;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;




public class Atom extends Point3fi implements Node {

  // ATOM_IN_FRAME simply associates an atom with the current model
  // but doesn't necessarily mean it is visible
  // ATOM_VIS_SET and ATOM_VISIBLE are checked once only for each atom per rendering

  public final static int ATOM_INFRAME     = 1;
  public final static int ATOM_VISSET      = 2;  // have carried out checkVisible()
  public final static int ATOM_VISIBLE     = 4;  // set from checkVisible()
  public final static int ATOM_NOTHIDDEN   = 8;
  public final static int ATOM_NOFLAGS     = ~63; // all of the above, plus balls and sticks
  public final static int ATOM_INFRAME_NOTHIDDEN = ATOM_INFRAME | ATOM_NOTHIDDEN;
  public final static int ATOM_SHAPE_VIS_MASK = ~ATOM_INFRAME_NOTHIDDEN;

  
  public static final int RADIUS_MAX = 16;
  public static final float RADIUS_GLOBAL = 16.1f;
  public static short MAD_GLOBAL = 32200;

  public char altloc = '\0';
  public byte atomID;
  public int atomSite;
  public Group group;
  private float userDefinedVanDerWaalRadius;
  byte valence;
  private short atomicAndIsotopeNumber;
  public BS atomSymmetry;

  private int formalChargeAndFlags; //  cccc ---- -*RS --hv
  
  private final static int CHARGE_OFFSET = 24;

  private final static int FLAG_MASK = 0xF;
  private final static int VIBRATION_VECTOR_FLAG = 1;
  private final static int IS_HETERO_FLAG = 2;
  
  private final static int CIP_CHIRALITY_OFFSET = 4;
  private final static int CIP_CHIRALITY_MASK = 0x1F0;
  private final static int CIP_CHIRALITY_RULE_OFFSET = 9;
  private final static int CIP_CHIRALITY_RULE_MASK = 0xE00;
  private final static int CIP_MASK = CIP_CHIRALITY_MASK | CIP_CHIRALITY_RULE_MASK; 

  public short madAtom;

  public short colixAtom;
  public byte paletteID = PAL.CPK.id;

  /**
   * 
   * MAY BE NULL
   * 
   */
  public Bond[] bonds;
  
  private int nBondsDisplayed = 0;
  public int nBackbonesDisplayed = 0;
  
  public int clickabilityFlags;
  public int shapeVisibilityFlags;

  /**
   * 
   * @param modelIndex
   * @param atomIndex
   * @param xyz
   * @param radius
   * @param atomSymmetry
   * @param atomSite
   * @param atomicAndIsotopeNumber
   * @param formalCharge
   * @param isHetero
   * @return this
   */
  
  public Atom setAtom(int modelIndex, int atomIndex,
        P3 xyz, float radius,
        BS atomSymmetry, int atomSite,
        short atomicAndIsotopeNumber, int formalCharge, 
        boolean isHetero) {
    this.mi = (short)modelIndex;
    this.atomSymmetry = atomSymmetry;
    this.atomSite = atomSite;
    this.i = atomIndex;
    this.atomicAndIsotopeNumber = atomicAndIsotopeNumber;
    if (isHetero)
      formalChargeAndFlags = IS_HETERO_FLAG;
    if (formalCharge != 0 && formalCharge != Integer.MIN_VALUE)
      setFormalCharge(formalCharge);
    userDefinedVanDerWaalRadius = radius;
    setT(xyz);
    return this;
  }

  public final void setShapeVisibility(int flag, boolean isVisible) {
    if(isVisible)
      shapeVisibilityFlags |= flag;        
    else
      shapeVisibilityFlags &=~flag;
  }
  
  public boolean isCovalentlyBonded(Atom atomOther) {
    if (bonds != null)
      for (int i = bonds.length; --i >= 0;)
        if (bonds[i].isCovalent() 
            && bonds[i].getOtherAtom(this) == atomOther)
          return true;
    return false;
  }

  public boolean isBonded(Atom atomOther) {
    if (bonds != null)
      for (int i = bonds.length; --i >= 0;)
        if (bonds[i].getOtherAtom(this) == atomOther)
          return true;
    return false;
  }

  public Bond getBond(Atom atomOther) {
    if (bonds != null)
      for (int i = bonds.length; --i >= 0;)
        if (bonds[i].getOtherAtom(atomOther) != null)
          return bonds[i];
    return null;
  }

  
  void addDisplayedBond(int stickVisibilityFlag, boolean isVisible) {
    nBondsDisplayed += (isVisible ? 1 : -1);
    setShapeVisibility(stickVisibilityFlag, (nBondsDisplayed > 0));
  } 
  
  void deleteBond(Bond bond) {
    // this one is used -- from Bond.deleteAtomReferences
    if (bonds != null)
      for (int i = bonds.length; --i >= 0;)
        if (bonds[i] == bond) {
          deleteBondAt(i);
          return;
        }
  }

  private void deleteBondAt(int i) {
    setCIPChirality(0);
    int newLength = bonds.length - 1;
    if (newLength == 0) {
      bonds = null;
      return;
    }
    Bond[] bondsNew = new Bond[newLength];
    int j = 0;
    for ( ; j < i; ++j)
      bondsNew[j] = bonds[j];
    for ( ; j < newLength; ++j)
      bondsNew[j] = bonds[j + 1];
    bonds = bondsNew;
  }

  @Override
  public int getBondedAtomIndex(int bondIndex) {
    return bonds[bondIndex].getOtherAtom(this).i;
  }

  /*
   * What is a MAR?
   *  - just a term that Miguel made up
   *  - an abbreviation for Milli Angstrom Radius
   * that is:
   *  - a *radius* of either a bond or an atom
   *  - in *millis*, or thousandths of an *angstrom*
   *  - stored as a short
   *
   * However! In the case of an atom radius, if the parameter
   * gets passed in as a negative number, then that number
   * represents a percentage of the vdw radius of that atom.
   * This is converted to a normal MAR as soon as possible
   *
   * (I know almost everyone hates bytes & shorts, but I like them ...
   *  gives me some tiny level of type-checking ...
   *  a rudimentary form of enumerations/user-defined primitive types)
   */

  public void setMadAtom(Viewer vwr, RadiusData rd) {
    madAtom = calculateMad(vwr, rd);
  }
  
  public short calculateMad(Viewer vwr, RadiusData rd) {
    if (rd == null)
      return 0;
    float f = rd.value;
    if (f == 0)
      return 0;
    switch (rd.factorType) {
    case SCREEN:
       return (short) f;
    case FACTOR:
    case OFFSET:
      float r = 0;
      switch (rd.vdwType) {
      case TEMP:
        float tmax = vwr.ms.getBfactor100Hi();
        r = (tmax > 0 ? getBfactor100() / tmax : 0);
        break;
      case HYDRO:
        r = Math.abs(getHydrophobicity());
        break;
      case BONDING:
        r = getBondingRadius();
        break;
      case ADPMIN:
      case ADPMAX:
        r = getADPMinMax(rd.vdwType == VDW.ADPMAX);
        break;
      default:
        r = getVanderwaalsRadiusFloat(vwr, rd.vdwType);
      }
      if (rd.factorType == EnumType.FACTOR)
        f *= r;
      else
        f += r;
      break;
    case ABSOLUTE:
      if (f == RADIUS_GLOBAL)
        return MAD_GLOBAL;
      break;
    }
    short mad = (short) (f < 0 ? f: f * 2000);
    if (mad < 0 && f > 0)
      mad = 0;
    return mad; 
  }

  public float getADPMinMax(boolean isMax) {
    Object[] tensors = getTensors();
    if (tensors == null)
      return 0;
    Tensor t = (Tensor) tensors[0];
    if (t == null || t.iType != Tensor.TYPE_ADP)
      return 0;
    if (group.chain.model.ms.isModulated(i) && t.isUnmodulated)
      t = (Tensor) tensors[1];
    return t.getFactoredValue(isMax ? 2 : 1); 
  }

  public Object[] getTensors() {
    return group.chain.model.ms.getAtomTensorList(i);
  }
  
  public int getRasMolRadius() {
    return Math.abs(madAtom / 8); //  1000r = 1000d / 2; rr = (1000r / 4);
  }

  @Override
  public Edge[] getEdges() {
    return (bonds == null ? new Edge[0] : bonds);
  }
  
  @Override
  public int getBondCount() {
    return (bonds == null ? 0 : bonds.length);    
  }
  
  public void setTranslucent(boolean isTranslucent, float translucentLevel) {
    colixAtom = C.getColixTranslucent3(colixAtom, isTranslucent, translucentLevel);    
  }

  @Override
  public int getElementNumber() {
    return Elements.getElementNumber(atomicAndIsotopeNumber);
  }
  
  @Override
  public int getIsotopeNumber() {
    return Elements.getIsotopeNumber(atomicAndIsotopeNumber);
  }
  
  @Override
  public int getAtomicAndIsotopeNumber() {
    return atomicAndIsotopeNumber;
  }

  public void setAtomicAndIsotopeNumber(int n) {
    if (n < 0 || (n & 127) >= Elements.elementNumberMax || n > Short.MAX_VALUE)
      n = 0;
    atomicAndIsotopeNumber = (short) n;
  }

  public String getElementSymbolIso(boolean withIsotope) {
    return Elements.elementSymbolFromNumber(withIsotope ? atomicAndIsotopeNumber : atomicAndIsotopeNumber & 127);    
  }
  
  public String getElementSymbol() {
    return getElementSymbolIso(true);
  }

  public boolean isHetero() {
    return (formalChargeAndFlags & IS_HETERO_FLAG) != 0;
  }

  public boolean hasVibration() {
    return (formalChargeAndFlags & VIBRATION_VECTOR_FLAG) != 0;
  }

  /**
   * 
   * @param charge from -3 to 7  
   */
  public void setFormalCharge(int charge) {
    formalChargeAndFlags = (formalChargeAndFlags & FLAG_MASK) 
        | ((charge == Integer.MIN_VALUE ? 0 : charge > 7 ? 7 : charge < -3 ? -3 : charge) << CHARGE_OFFSET);
  }
  
  void setVibrationVector() {
    formalChargeAndFlags |= VIBRATION_VECTOR_FLAG;
  }
  
  @Override
  public int getFormalCharge() {
    return formalChargeAndFlags >> CHARGE_OFFSET;
  }

  // a percentage value in the range 0-100
  public int getOccupancy100() {
    float[] occupancies = group.chain.model.ms.occupancies;
    return (occupancies == null ? 100 : Math.round(occupancies[i]));
  }

  // a percentage value in the range 0-100
  public boolean isOccupied() {
    float[] occupancies = group.chain.model.ms.occupancies;
    return (occupancies == null || occupancies[i] >= 50);
  }

  // This is called bfactor100 because it is stored as an integer
  // 100 times the bfactor(temperature) value
  public int getBfactor100() {
    short[] bfactor100s = group.chain.model.ms.bfactor100s;
    return (bfactor100s == null ? 0 : bfactor100s[i]);
  }

  public float getHydrophobicity() {
    float[] values = group.chain.model.ms.hydrophobicities;
    return (values == null ? Elements.getHydrophobicity(group.groupID) : values[i]);
  }

  public boolean setRadius(float radius) {
    return !Float.isNaN(userDefinedVanDerWaalRadius = (radius > 0 ? radius : Float.NaN));  
  }
  
  public void delete(BS bsBonds) {
    valence = -1;
    if (bonds != null)
      for (int i = bonds.length; --i >= 0; ) {
        Bond bond = bonds[i];
        bond.getOtherAtom(this).deleteBond(bond);
        bsBonds.set(bond.index);
      }
    bonds = null;
  }

  @Override
  public boolean isDeleted() {
    return (valence < 0);
  }

  public void setValence(int nBonds) {
    if (!isDeleted()) // no resurrection
      valence = (byte) (nBonds < 0 ? 0 : nBonds <= 0x7F ? nBonds : 0x7F);
  }

  /**
   * return the total bond order for this atom
   */
  @Override
  public int getValence() {
    if (isDeleted())
      return -1;
    int n = valence;
    if (n == 0 && bonds != null)
      for (int i = bonds.length; --i >= 0;)
        n += bonds[i].getValence();
    return n;
  }

  @Override
  public int getCovalentBondCount() {
    if (bonds == null)
      return 0;
    int n = 0;
    Bond b;
    for (int i = bonds.length; --i >= 0; )
      if (((b = bonds[i]).order & Edge.BOND_COVALENT_MASK) != 0
          && !b.getOtherAtom(this).isDeleted())
        ++n;
    return n;
  }

  @Override
  public int getCovalentHydrogenCount() {
    if (bonds == null)
      return 0;
    int n = 0;
    for (int i = bonds.length; --i >= 0; ) {
      if ((bonds[i].order & Edge.BOND_COVALENT_MASK) == 0)
        continue;
      Atom a = bonds[i].getOtherAtom(this);
      if (a.valence >= 0 && a.getElementNumber() == 1)
        ++n;
    }
    return n;
  }

  @Override
  public int getImplicitHydrogenCount() {
    return group.chain.model.ms.getMissingHydrogenCount(this, false);
  }

  @Override
  public int getTotalHydrogenCount() {
    return getCovalentHydrogenCount() + getImplicitHydrogenCount();
  }

  @Override
  public int getTotalValence() {
    int v = getValence();
    if (v < 0)
      return v;
    int h = getImplicitHydrogenCount();
    int sp2 = group.chain.model.ms.aaRet[4]; // 1 or 0
    return v + h + sp2;
  }

  @Override
  public int getCovalentBondCountPlusMissingH() {
    return getCovalentBondCount() + getImplicitHydrogenCount();
  }

  int getTargetValence() {
    switch (getElementNumber()) {
    case 6: //C
    case 14: //Si
    case 32: // Ge
      return 4;
    case 5:  // B
    case 7:  // N
    case 15: // P
      return 3;
    case 8: //O
    case 16: //S
      return 2;
    case 1:
    case 9: // F
    case 17: // Cl
    case 35: // Br
    case 53: // I
      return 1;
    }
    return -1;
  }


  public float getDimensionValue(int dimension) {
    return (dimension == 0 ? x : (dimension == 1 ? y : z));
  }

  public float getVanderwaalsRadiusFloat(Viewer vwr, VDW type) {
    // called by atomPropertyFloat as VDW_AUTO,
    // AtomCollection.fillAtomData with VDW_AUTO or VDW_NOJMOL
    // AtomCollection.findMaxRadii with VDW_AUTO
    // AtomCollection.getAtomPropertyState with VDW_AUTO
    // AtomCollection.getVdwRadius with passed on type
    return (Float.isNaN(userDefinedVanDerWaalRadius) 
        ? vwr.getVanderwaalsMarType(atomicAndIsotopeNumber, getVdwType(type)) / 1000f
        : userDefinedVanDerWaalRadius);
  }

  /**
   * 
   * @param type 
   * @return if VDW_AUTO, will return VDW_AUTO_JMOL, VDW_AUTO_RASMOL, or VDW_AUTO_BABEL
   *         based on the model type
   */
  @SuppressWarnings("incomplete-switch")
  private VDW getVdwType(VDW type) {
    switch (type) {
    case AUTO:
      type = group.chain.model.ms.getDefaultVdwType(mi);
      break;
    case NOJMOL:
      type = group.chain.model.ms.getDefaultVdwType(mi);
      if (type == VDW.AUTO_JMOL)
        type = VDW.AUTO_BABEL;
      break;
    }
    return type;
  }

  public float getBondingRadius() {
    float[] rr = group.chain.model.ms.bondingRadii;
    float r = (rr == null ? 0 : rr[i]);
    return (r == 0 ? Elements.getBondingRadius(atomicAndIsotopeNumber,
        getFormalCharge()) : r);
  }

  float getVolume(Viewer vwr, VDW vType) {
    float r1 = (vType == null ? userDefinedVanDerWaalRadius : Float.NaN);
    if (Float.isNaN(r1))
      r1 = vwr.getVanderwaalsMarType(getElementNumber(), getVdwType(vType)) / 1000f;
    double volume = 0;
    if (bonds != null)
      for (int j = 0; j < bonds.length; j++) {
        if (!bonds[j].isCovalent())
          continue;
        Atom atom2 = bonds[j].getOtherAtom(this);
        float r2 = (vType == null ? atom2.userDefinedVanDerWaalRadius : Float.NaN);
        if (Float.isNaN(r2))
          r2 = vwr.getVanderwaalsMarType(atom2.getElementNumber(), atom2
              .getVdwType(vType)) / 1000f;
        float d = distance(atom2);
        if (d > r1 + r2)
          continue;
        if (d + r1 <= r2)
          return 0;

        // calculate hidden spherical cap height and volume
        // A.Bondi, J. Phys. Chem. 68, 1964, 441-451.

        double h = r1 - (r1 * r1 + d * d - r2 * r2) / (2.0 * d);
        volume -= Math.PI / 3 * h * h * (3 * r1 - h);
      }
    return (float) (volume + 4 * Math.PI / 3 * r1 * r1 * r1);
  }

  int getCurrentBondCount() {
    return bonds == null ? 0 : bonds.length;
  }

  public float getRadius() {
    return Math.abs(madAtom / 2000f);
  }

  @Override
  public int getIndex() {
    return i;
  }

  @Override
  public int getAtomSite() {
    return atomSite;
  }

  @Override
  public void getGroupBits(BS bs) {
     group.setAtomBits(bs);
   }
   
   @Override
  public String getAtomName() {
     return (atomID > 0 ? Group.specialAtomNames[atomID]
         : group.chain.model.ms.atomNames[i]);
   }
   
   @Override
  public String getAtomType() {
    String[] atomTypes = group.chain.model.ms.atomTypes;
    String type = (atomTypes == null ? null : atomTypes[i]);
    return (type == null ? getAtomName() : type);
  }
   
   @Override
  public int getAtomNumber() {
     int[] atomSerials = group.chain.model.ms.atomSerials;
     // shouldn't ever be null.
     return (atomSerials == null ? i : atomSerials[i]);
//        : group.chain.model.modelSet.isZeroBased ? atomIndex : atomIndex);
   }

   public int getSeqID() {
     int[] ids = group.chain.model.ms.atomSeqIDs;
     return (ids == null ? 0 : ids[i]);
   }
   public boolean isVisible(int flags) {
     return ((shapeVisibilityFlags & flags) == flags);
   }

   public float getPartialCharge() {
     float[] partialCharges = group.chain.model.ms.partialCharges;
     return partialCharges == null ? 0 : partialCharges[i];
   }

   /**
    * Given a symmetry operation number, the set of cells in the model, and the
    * number of operations, this method returns either 0 or the cell number (555, 666)
    * of the translated symmetry operation corresponding to this atom.
    * 
    * atomSymmetry is a bitset that is created in adapter.smarter.AtomSetCollection
    * 
    * It is arranged as follows:
    * 
    * |--overall--|---cell1---|---cell2---|---cell3---|...
    * 
    * |012..nOps-1|012..nOps-1|012..nOp-1s|012..nOps-1|...
    * 
    * If a bit is set, it means that the atom was created using that operator
    * operating on the base file set and translated for that cell.
    * 
    * If any bit is set in any of the cell blocks, then the same
    * bit will also be set in the overall block. This allows for
    * rapid determination of special positions and also of
    * atom membership in any operation set.
    * 
    *  Note that it is not necessarily true that an atom is IN the designated
    *  cell, because one can load {nnn mmm 0}, and then, for example, the {-x,-y,-z}
    *  operator sends atoms from 555 to 444. Still, those atoms would be marked as
    *  cell 555 here, because no translation was carried out. 
    *  
    *  That is, the numbers 444 in symop=3444 do not refer to a cell, per se. 
    *  What they refer to is the file-designated operator plus a translation of
    *  {-1 -1 -1/1}. 
    * 
    * @param symop        = 0, 1, 2, 3, ....
    * @param cellRange    = {444, 445, 446, 454, 455, 456, .... }
    * @param nOps         = 2 for x,y,z;-x,-y,-z, for example
    * @return cell number such as 565
    */
   public int getSymmetryTranslation(int symop, int[] cellRange, int nOps) {
     int pt = symop;
     for (int i = 0; i < cellRange.length; i++)
       if (atomSymmetry.get(pt += nOps))
         return cellRange[i];
     return 0;
   }
   
   /**
    * Looks for a match in the cellRange list for this atom within the specified translation set
    * select symop=0NNN for this
    * 
    * @param cellNNN
    * @param cellRange
    * @param nOps
    * @return     matching cell number, if applicable
    */
   public int getCellTranslation(int cellNNN, int[] cellRange, int nOps) {
     int pt = nOps;
     for (int i = 0; i < cellRange.length; i++)
       for (int j = 0; j < nOps;j++, pt++)
       if (atomSymmetry.get(pt) && cellRange[i] == cellNNN)
         return cellRange[i];
     return 0;
   }
   
   String getSymmetryOperatorList(boolean isAll) {
    String str = "";
    ModelSet f = group.chain.model.ms;
    int nOps = f.getModelSymmetryCount(mi);
    if (nOps == 0 || atomSymmetry == null)
      return "";
    int[] cellRange = f.getModelCellRange(mi);
    int pt = nOps;
    int n = (cellRange == null ? 1 : cellRange.length);
    BS bs = (isAll ? null : new BS());
    for (int i = 0; i < n; i++)
      for (int j = 0; j < nOps; j++)
        if (atomSymmetry.get(pt++))
          if (isAll) {
            str += "," + (j + 1) + cellRange[i];
          } else {
            bs.set(j + 1);
          }
    if (!isAll)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        str += "," + i;
    return (str.length() == 0 ? "" : str.substring(1));
  }
   
  /**
   * SMILES only
   */
  @Override
  public int getModelIndex() {
    return mi;
  }
   
  @Override
  public int getMoleculeNumber(boolean inModel) {
    return (group.chain.model.ms.getMoleculeIndex(i, inModel) + 1);
  }
   
  private float getFractionalCoord(boolean fixJavaFloat, char ch, boolean ignoreOffset, P3 pt) {
    pt = getFractionalCoordPt(fixJavaFloat, ignoreOffset, pt);
    return (ch == 'X' ? pt.x : ch == 'Y' ? pt.y : pt.z);
  }
    
  @Override
  public P3 getXYZ() {
    return this;
  }
  
  public P3 getFractionalCoordPt(boolean fixJavaFloat, boolean ignoreOffset, P3 pt) {
    // ignoreOffset TRUE uses the original unshifted matrix
    SymmetryInterface c = getUnitCell();
    if (c == null) 
      return this;
    if (pt == null)
      pt = P3.newP(this);
    else
      pt.setT(this);
    c.toFractional(pt, ignoreOffset);
    if (fixJavaFloat)
      PT.fixPtFloats(pt, PT.FRACTIONAL_PRECISION);
    return pt;
  }
  
  SymmetryInterface getUnitCell() {
    return group.chain.model.ms.getUnitCellForAtom(this.i);
  }
  
  private float getFractionalUnitCoord(boolean fixJavaFloat, char ch, P3 pt) {
    pt = getFractionalUnitCoordPt(fixJavaFloat, false, pt);
    return (ch == 'X' ? pt.x : ch == 'Y' ? pt.y : pt.z);
  }

  /**
   * @param fixJavaFloat ALWAYS set true for any new references to this method. False is for legacy only
   * @param asCartesian
   * @param pt
   * @return unit cell coord
   */
  P3 getFractionalUnitCoordPt(boolean fixJavaFloat, boolean asCartesian, P3 pt) {
    SymmetryInterface c = getUnitCell();
    if (c == null)
      return this;
    if (pt == null)
      pt = P3.newP(this);
    else
      pt.setT(this);
    if (group.chain.model.isJmolDataFrame) {
      c.toFractional(pt, false);
      if (asCartesian)
        c.toCartesian(pt, false);
    } else {
      c.toUnitCell(pt, null);
      if (!asCartesian)
        c.toFractional(pt, false);
    }
    if (fixJavaFloat)
      PT.fixPtFloats(pt, asCartesian ? PT.CARTESIAN_PRECISION : PT.FRACTIONAL_PRECISION);      
    return pt;
  }
  
  float getFractionalUnitDistance(T3 pt, T3 ptTemp1, T3 ptTemp2) {
    SymmetryInterface c = getUnitCell();
    if (c == null) 
      return distance(pt);
    ptTemp1.setT(this);
    ptTemp2.setT(pt);
    if (group.chain.model.isJmolDataFrame) {
      c.toFractional(ptTemp1, true);
      c.toFractional(ptTemp2, true);
    } else {
      c.toUnitCell(ptTemp1, null);
      c.toUnitCell(ptTemp2, null);
    }
    return ptTemp1.distance(ptTemp2);
  }
  
  void setFractionalCoord(int tok, float fValue, boolean asAbsolute) {
    SymmetryInterface c = getUnitCell();
    if (c != null)
      c.toFractional(this, asAbsolute);
    switch (tok) {
    case T.fux:
    case T.fracx:
      x = fValue;
      break;
    case T.fuy:
    case T.fracy:
      y = fValue;
      break;
    case T.fuz:
    case T.fracz:
      z = fValue;
      break;
    }
    if (c != null)
      c.toCartesian(this, asAbsolute);
  }
  
  void setFractionalCoordTo(P3 ptNew, boolean asAbsolute) {
    setFractionalCoordPt(this, ptNew, asAbsolute);
  }
  
  public void setFractionalCoordPt(P3 pt, P3 ptNew, boolean asAbsolute) {
    pt.setT(ptNew);
    SymmetryInterface c = getUnitCell();
    if (c != null)
      c.toCartesian(pt, asAbsolute && !group.chain.model.isJmolDataFrame);
  }

  boolean isCursorOnTopOf(int xCursor, int yCursor,
                        int minRadius, Atom competitor) {
    int r = sD / 2;
    if (r < minRadius)
      r = minRadius;
    int r2 = r * r;
    int dx = sX - xCursor;
    int dx2 = dx * dx;
    if (dx2 > r2)
      return false;
    int dy = sY - yCursor;
    int dy2 = dy * dy;
    int dz2 = r2 - (dx2 + dy2);
    if (dz2 < 0)
      return false;
    if (competitor == null)
      return true;
    int z = sZ;
    int zCompetitor = competitor.sZ;
    int rCompetitor = competitor.sD / 2;
    if (z < zCompetitor - rCompetitor)
      return true;
    int dxCompetitor = competitor.sX - xCursor;
    int dx2Competitor = dxCompetitor * dxCompetitor;
    int dyCompetitor = competitor.sY - yCursor;
    int dy2Competitor = dyCompetitor * dyCompetitor;
    int r2Competitor = rCompetitor * rCompetitor;
    int dz2Competitor = r2Competitor - (dx2Competitor + dy2Competitor);
    return (z - Math.sqrt(dz2) < zCompetitor - Math.sqrt(dz2Competitor));
  }

  /*
   *  DEVELOPER NOTE (BH):
   *  
   *  The following methods may not return 
   *  correct values until after modelSet.finalizeGroupBuild()
   *  
   */
   
  public String getInfo() {
    return getIdentity(true);
  } 

  public String getIdentityXYZ(boolean allInfo, P3 pt) {
    pt = (group.chain.model.isJmolDataFrame ? getFractionalCoordPt(!group.chain.model.ms.vwr.g.legacyJavaFloat, false, pt) : this);
    return getIdentity(allInfo) + " " + pt.x + " " + pt.y + " " + pt.z;  
  }
  
  String getIdentity(boolean allInfo) {
    SB info = new SB();
    String group3 = getGroup3(true);
    if (group3 != null && group3.length() > 0 && (!group3.equals("UNK") || group.chain.model.isBioModel)) {
      info.append("[");
      info.append(group3);
      info.append("]");
      String seqcodeString = group.getSeqcodeString();
      if (seqcodeString != null)
        info.append(seqcodeString);
      int chainID = group.chain.chainID;
      if (chainID != 0 && chainID != 32) {
        info.append(":");
        String s = getChainIDStr();
        if (chainID >= 256)
          s = PT.esc(s);
        info.append(s);
      }
      if (!allInfo)
        return info.toString();
      info.append(".");
    }
    info.append(getAtomName());
    if (info.length() == 0) {
      // since atomName cannot be null, this is unreachable
      info.append(getElementSymbolIso(false));
      info.append(" ");
      info.appendI(getAtomNumber());
    }
    if (altloc != '\0') {
      info.append("%");
      info.appendC(altloc);
    }
    if (group.chain.model.ms.mc > 1 && !group.chain.model.isJmolDataFrame) {
      info.append("/");
      info.append(getModelNumberForLabel());
    }
    info.append(" #");
    info.appendI(getAtomNumber());
    return info.toString();
  }

  @Override
  public String getGroup3(boolean allowNull) {
    String group3 = group.getGroup3();
    return (allowNull     
        || group3 != null && group3.length() > 0 
        ? group3 : "UNK");
  }

  @Override
  public String getGroup1(char c0) {
    char c = group.getGroup1();
    return (c != '\0' ? "" + c : c0 != '\0' ? "" + c0 : "");
  }

  @Override
  public char getBioSmilesType() {
    return  (group.isProtein() ? 'p'
        : group.isDna() ? 'd'
            : group.isRna() ? 'r'
                : group.isCarbohydrate() ? 'c'
                    : ' ');
  }

  @Override
  public boolean isPurine() {
    return group.isPurine();
  }

  @Override
  public boolean isPyrimidine() {
    return group.isPyrimidine();
  }

  @Override
  public int getResno() {
    return group.getResno();   
  }

  public boolean isClickable() {
    // certainly if it is not visible, then it can't be clickable
    return (checkVisible() 
        && clickabilityFlags != 0 
        && ((shapeVisibilityFlags | group.shapeVisibilityFlags) & clickabilityFlags) != 0);
  }

  public void setClickable(int flag) {
    if (flag == 0) {
      clickabilityFlags = 0;
    } else {
      clickabilityFlags |= flag;
      if (flag != JC.ALPHA_CARBON_VISIBILITY_FLAG)
        shapeVisibilityFlags |= flag;
    }
  }
  
  public boolean checkVisible() {
    if (isVisible(ATOM_VISSET))
      return isVisible(ATOM_VISIBLE);
    boolean isVis = isVisible(ATOM_INFRAME_NOTHIDDEN);
    if (isVis) {
      int flags = shapeVisibilityFlags;
      // Is its PDB group visible in any way (cartoon, e.g.)?
      //  An atom is considered visible if its PDB group is visible, even
      //  if it does not show up itself as part of the structure
      //  (this will be a difference in terms of *clickability*).
      // except BACKBONE -- in which case we only see the lead atoms
      if (group.shapeVisibilityFlags != 0
          && (group.shapeVisibilityFlags != JC.VIS_BACKBONE_FLAG || isLeadAtom()))
        flags |= group.shapeVisibilityFlags;
      // We know that (flags & AIM), so now we must remove that flag
      // and check to see if any others are remaining.
      // Only then is the atom considered visible.
      flags &= ATOM_SHAPE_VIS_MASK;
      // problem with display of bond-only when not clickable. 
      // bit of a kludge here.
      if (flags == JC.VIS_BOND_FLAG && clickabilityFlags == 0)
        flags = 0;
      isVis = (flags != 0);
      if (isVis)
        shapeVisibilityFlags |= ATOM_VISIBLE;
    }
    shapeVisibilityFlags |= ATOM_VISSET;
    return isVis;

  }

  @Override
  public boolean isLeadAtom() {
    return group.isLeadAtom(i);
  }
  
  @Override
  public int getChainID() {
    return group.chain.chainID;
  }

  @Override
  public String getChainIDStr() {
    return group.chain.getIDStr();
  }
  
  public int getSurfaceDistance100() {
    return group.chain.model.ms.getSurfaceDistance100(i);
  }

  public Vibration getVibrationVector() {
    return group.chain.model.ms.getVibration(i, false);
  }

  public JmolModulationSet getModulation() {
    return group.chain.model.ms.getModulation(i);
  }

  public String getModelNumberForLabel() {
    return group.chain.model.ms.getModelNumberForAtomLabel(mi);
  }
  
  public int getModelNumber() {
    return group.chain.model.ms.getModelNumber(mi) % 1000000;
  }
  
  @Override
  public String getBioStructureTypeName() {
    return group.getProteinStructureType().getBioStructureTypeName(true);
  }
  
  @Override
  public boolean equals(Object obj) {
    return (this == obj);
  }

  @Override
  public int hashCode() {
    //this overrides the Point3fi hashcode, which would
    //give a different hashcode for an atom depending upon
    //its screen location! Bug fix for 11.1.43 Bob Hanson
    return i;
  }
  
  public Atom findAromaticNeighbor(int notAtomIndex) {
    if (bonds == null)
      return null;
    for (int i = bonds.length; --i >= 0; ) {
      Bond bondT = bonds[i];
      Atom a = bondT.getOtherAtom(this);
      if (bondT.isAromatic() && a.i != notAtomIndex)
        return a;
    }
    return null;
  }

  /**
   * called by isosurface and int comparator via atomProperty()
   * and also by getBitsetProperty() 
   * 
   * @param tokWhat
   * @return         int value or Integer.MIN_VALUE
   */
  public int atomPropertyInt(int tokWhat) {
    switch (tokWhat) {
    case T.atomno:
      return getAtomNumber();
    case T.seqid:
      return getSeqID();
    case T.atomid:
      return atomID;
    case T.subsystem:
      return Math.max(0, altloc - 32);
    case T.atomindex:
      return i;
    case T.bondcount:
      return getCovalentBondCount();
    case T.chainno:
      return group.chain.chainNo;
    case T.color:
      return group.chain.model.ms.vwr.gdata.getColorArgbOrGray(colixAtom);
    case T.element:
    case T.elemno:
      return getElementNumber();
    case T.elemisono:
      return atomicAndIsotopeNumber;
    case T.file:
      return group.chain.model.fileIndex + 1;
    case T.formalcharge:
      return getFormalCharge();
    case T.groupid:
      return group.groupID; //-1 if no group
    case T.groupindex:
      return group.groupIndex;
    case T.model:
      //integer model number -- could be PDB/sequential adapter number
      //or it could be a sequential model in file number when multiple files
      return getModelNumber();
    case -T.model:
      //float is handled differently
      return group.chain.model.ms.modelFileNumbers[mi];
    case T.modelindex:
      return mi;
    case T.molecule:
      return getMoleculeNumber(true);
    case T.monomer:
      return group.getMonomerIndex() + 1;
    case T.occupancy:
      return getOccupancy100();
    case T.polymer:
      return group.getBioPolymerIndexInModel() + 1;
    case T.polymerlength:
      return group.getBioPolymerLength();
    case T.radius:
      // the comparator uses rasmol radius, unfortunately, for integers
      return getRasMolRadius();        
    case T.resno:
      return getResno();
    case T.site:
      return getAtomSite();
    case T.structure:
      return group.getProteinStructureType().getId();
    case T.substructure:
      return group.getProteinStructureSubType().getId();
    case T.strucno:
      return group.getStrucNo();
    case T.symop:
      return getSymOp();
    case T.valence:
      return getValence();
    }
    return 0;      
  }

  int getSymOp() {
    return (atomSymmetry == null ? 0 : atomSymmetry.nextSetBit(0) + 1);
  }

  /**
   * called by isosurface and int comparator via atomProperty() and also by
   * getBitsetProperty()
   * 
   * @param vwr
   * 
   * @param tokWhat
   * @param ptTemp 
   * @return float value or value*100 (asInt=true) or throw an error if not
   *         found
   * 
   */
  public float atomPropertyFloat(Viewer vwr, int tokWhat, P3 ptTemp) {
    switch (tokWhat) {
    case T.adpmax:
      return getADPMinMax(true);
    case T.adpmin:
      return getADPMinMax(false);
    case T.atomx:
    case T.x:
      return x;
    case T.atomy:
    case T.y:
      return y;
    case T.atomz:
    case T.z:
      return z;
    case T.dssr:
      return group.chain.model.ms.getAtomicDSSRData(i);
    case T.backbone:
    case T.cartoon:
    case T.dots:
    case T.ellipsoid:
    case T.geosurface:
    case T.halo:
    case T.meshRibbon:
    case T.ribbon:
    case T.rocket:
    case T.star:
    case T.strands:
    case T.trace:
      return vwr.shm.getAtomShapeValue(tokWhat, group, i);
    case T.bondingradius:
      return getBondingRadius();
    case T.chemicalshift:
      return vwr.getNMRCalculation().getChemicalShift(this);
    case T.covalentradius:
      return Elements.getCovalentRadius(atomicAndIsotopeNumber);
    case T.eta:
    case T.theta:
    case T.straightness:
      return group.getGroupParameter(tokWhat);
    case T.fux:
    case T.fracx:
      return getFractionalCoord(!vwr.g.legacyJavaFloat, 'X', false, ptTemp);
    case T.fuy:
    case T.fracy:
      return getFractionalCoord(!vwr.g.legacyJavaFloat, 'Y', false, ptTemp);
    case T.fuz:
    case T.fracz:
      return getFractionalCoord(!vwr.g.legacyJavaFloat, 'Z', false, ptTemp);
    case T.hydrophobicity:
      return getHydrophobicity();
    case T.magneticshielding:
      return vwr.getNMRCalculation().getMagneticShielding(this);
    case T.mass:
      return getMass();
    case T.occupancy:
      return getOccupancy100() / 100f;
    case T.partialcharge:
      return getPartialCharge();
    case T.phi:
    case T.psi:
    case T.omega:
      if (group.chain.model.isJmolDataFrame
          && group.chain.model.jmolFrameType
              .startsWith("plot ramachandran")) {
        switch (tokWhat) {
        case T.phi:
          return getFractionalCoord(!vwr.g.legacyJavaFloat, 'X', false, ptTemp);
        case T.psi:
          return getFractionalCoord(!vwr.g.legacyJavaFloat, 'Y', false, ptTemp);
        case T.omega:
          if (group.chain.model.isJmolDataFrame
              && group.chain.model.jmolFrameType
                  .equals("plot ramachandran")) {
            float omega = getFractionalCoord(!vwr.g.legacyJavaFloat, 'Z', false, ptTemp) - 180;
            return (omega < -180 ? 360 + omega : omega);
          }
        }
      }
      return group.getGroupParameter(tokWhat);
    case T.radius:
    case T.spacefill:
      return getRadius();
    case T.screenx:
      return (vwr.antialiased ? sX / 2 : sX);
    case T.screeny:
      return vwr.getScreenHeight() - (vwr.antialiased ? sY / 2 : sY);
    case T.screenz:
      return (vwr.antialiased ? sZ / 2 : sZ);
    case T.selected:
      return (vwr.slm.isAtomSelected(i) ? 1 : 0);
    case T.surfacedistance:
      vwr.ms.getSurfaceDistanceMax();
      return getSurfaceDistance100() / 100f;
    case T.temperature: // 0 - 9999
      return getBfactor100() / 100f;
    case T.unitx:
      return getFractionalUnitCoord(!vwr.g.legacyJavaFloat, 'X', ptTemp);
    case T.unity:
      return getFractionalUnitCoord(!vwr.g.legacyJavaFloat, 'Y', ptTemp);
    case T.unitz:
      return getFractionalUnitCoord(!vwr.g.legacyJavaFloat, 'Z', ptTemp);
    case T.vanderwaals:
      return getVanderwaalsRadiusFloat(vwr, VDW.AUTO);
    case T.vectorscale:
      V3 v = getVibrationVector();
      return (v == null ? 0 : v.length() * vwr.getFloat(T.vectorscale));
    case T.vibx:
      return getVib('x');
    case T.viby:
      return getVib('y');
    case T.vibz:
      return getVib('z');
    case T.modx:
      return getVib('X');
    case T.mody:
      return getVib('Y');
    case T.modz:
      return getVib('Z');
    case T.modo:
      return getVib('O');
    case T.modt1:
      return getVib('1');
    case T.modt2:
      return getVib('2');
    case T.modt3:
      return getVib('3');
    case T.volume:
      return getVolume(vwr, VDW.AUTO);
    case T.fracxyz:
    case T.fuxyz:
    case T.unitxyz:
    case T.screenxyz:
    case T.vibxyz:
    case T.modxyz:
    case T.xyz:
      T3 v3 = atomPropertyTuple(vwr, tokWhat, ptTemp);
      return (v3 == null ? -1 : v3.length());
    }
    return atomPropertyInt(tokWhat);
  }

  public float getVib(char ch) {
    return group.chain.model.ms.getVibCoord(i, ch);
  }

  public int getNominalMass() {
    int mass = getIsotopeNumber();
    return (mass > 0 ? mass : Elements.getNaturalIsotope(getElementNumber()));
  }
  
  @Override
  public float getMass() {
    float mass = getIsotopeNumber();
    return (mass > 0 ? mass : Elements.getAtomicMass(getElementNumber()));
  }

  public String atomPropertyString(Viewer vwr, int tokWhat) {
    char ch;
    String s;
    switch (tokWhat) {
    case T.altloc:
      ch = altloc;
      return (ch == '\0' ? "" : "" + ch);
    case T.atomname:
      return getAtomName();
    case T.atomtype:
      return getAtomType();
    case T.chain:
      return getChainIDStr();
    case T.chirality:
      return getCIPChirality(true);
    case T.ciprule:
      return getCIPChiralityRule();
    case T.sequence:
      return getGroup1('?');
    case T.seqcode:
      s = group.getSeqcodeString();
      return (s == null ? "" : s);
    case T.group1:
      return getGroup1('\0');
    case T.group:
      return getGroup3(false);
    case T.element:
      return getElementSymbolIso(true);
    case T.identify:
      return getIdentity(true);
    case T.insertion:
      ch = group.getInsertionCode();
      return (ch == '\0' ? "" : "" + ch);
    case T.label:
    case T.format:
      s = (String) vwr.shm.getShapePropertyIndex(JC.SHAPE_LABELS, "label", i);
      if (s == null)
        s = "";
      return s;
    case T.structure:
      return group.getProteinStructureType().getBioStructureTypeName(false);
    case T.substructure:
      return group.getProteinStructureSubType().getBioStructureTypeName(false);
    case T.strucid:
      return group.getStructureId();
    case T.shape:
      return vwr.getHybridizationAndAxes(i, null, null, "d");
    case T.symbol:
      return getElementSymbolIso(false);
    case T.symmetry:
      return getSymmetryOperatorList(true);
    }
    return ""; 
  }

  /**
   * Determine R/S chirality at this position; non-H atoms only; cached in formalChargeAndFlags
   * @param doCalculate 
   * 
   * @return one of "", "R", "S", "E", "Z", "r", "s", "?"
   */
  @Override
  public String getCIPChirality(boolean doCalculate) {
    int flags = (formalChargeAndFlags & CIP_CHIRALITY_MASK) >> CIP_CHIRALITY_OFFSET;
    if (flags == 0 && atomicAndIsotopeNumber > 1 && doCalculate) {
      flags = group.chain.model.ms.getAtomCIPChiralityCode(this);
      formalChargeAndFlags |= ((flags == 0 ? JC.CIP_CHIRALITY_NONE : flags) << CIP_CHIRALITY_OFFSET);
    }
    return (JC.getCIPChiralityName(flags));
  }

  public String getCIPChiralityRule() {
    String rs = getCIPChirality(true);
    int flags = (rs.length() == 0 ? -1 : (formalChargeAndFlags & CIP_CHIRALITY_RULE_MASK) >> CIP_CHIRALITY_RULE_OFFSET);
    return (JC.getCIPRuleName(flags + 1));
  }

  /**
   * 
   * @param c [0:unknown; 3:none; 1: R; 2: S; 5: Z; 6: E; 9: M, 10: P, +r,s
   */
  @Override
  public void setCIPChirality(int c) {
    formalChargeAndFlags = (formalChargeAndFlags & ~CIP_MASK) 
        | (c << CIP_CHIRALITY_OFFSET);
  }

  @Override
  public int getCIPChiralityCode() {
    return (formalChargeAndFlags & CIP_CHIRALITY_MASK) >> CIP_CHIRALITY_OFFSET;
  }

  @Override
  public char getInsertionCode() {
    return group.getInsertionCode();
  }
  
  public T3 atomPropertyTuple(Viewer vwr, int tok, P3 ptTemp) {
    switch (tok) {
    case T.coord:
      return P3.newP(this);
    case T.fracxyz:
      return getFractionalCoordPt(!vwr.g.legacyJavaFloat, false, ptTemp); // was !group.chain.model.isJmolDataFrame
    case T.fuxyz:
      return getFractionalCoordPt(!vwr.g.legacyJavaFloat, false, ptTemp);
    case T.unitxyz:
      return (group.chain.model.isJmolDataFrame ? getFractionalCoordPt(!vwr.g.legacyJavaFloat, false, ptTemp) 
          : getFractionalUnitCoordPt(!vwr.g.legacyJavaFloat, false, ptTemp));
    case T.screenxyz:
      return P3.new3(vwr.antialiased ? sX / 2 : sX, vwr.getScreenHeight() - (vwr.antialiased ? sY / 2 : sY), vwr.antialiased ? sZ / 2 : sZ);
    case T.vibxyz:
      return getVibrationVector();
    case T.modxyz:
      JmolModulationSet ms = getModulation();
      return (ms == null ? null : ms.getV3());
    case T.xyz:
      return this;
    case T.color:
      return CU.colorPtFromInt(
          group.chain.model.ms.vwr.gdata.getColorArgbOrGray(colixAtom),
          ptTemp);
    }
    return null;
  }

  @Override
  public int getOffsetResidueAtom(String name, int offset) {
    // used by DSSP and SMILES
    return group.getAtomIndex(name, offset);
  }
  
  @Override
  public boolean isCrossLinked(Node node) {
    return group.isCrossLinked(((Atom) node).group);
  }

  /**
   * Used by SMILES to get vector of cross-links
   */
  @Override
  public boolean getCrossLinkVector(Lst<Integer> vReturn, boolean crosslinkCovalent, boolean crosslinkHBond) {
    return group.getCrossLinkVector(vReturn, crosslinkCovalent, crosslinkHBond);
  }
  
  @Override
  public String toString() {
    return getInfo();
  }

  @Override
  public BS findAtomsLike(String atomExpression) {
    // for SMARTS searching
    return group.chain.model.ms.vwr.getAtomBitSet(atomExpression);
  }

  public String getUnitID(int flags) {
    Model m = group.getModel();
    return (m.isBioModel ? ((BioModel) m).getUnitID(this, flags) : "");
  }

  @Override
  public float getFloatProperty(String property) {
    Object data = group.chain.model.ms.vwr.getDataObj(property, null,
        JmolDataManager.DATA_TYPE_AF);
    float f = Float.NaN;
    if (data != null) {
      try {
        f = ((float[]) data)[i];
      } catch (Exception e) {
      }
    }
    return f;
  }

}
