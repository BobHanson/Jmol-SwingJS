/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-10-14 12:33:20 -0500 (Sun, 14 Oct 2007) $
 * $Revision: 8408 $

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

import javajs.util.AU;
import javajs.util.Lst;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.jmol.api.Interface;
import org.jmol.api.JmolDataManager;
import org.jmol.api.JmolEnvCalc;
import org.jmol.api.JmolModulationSet;
import org.jmol.api.SymmetryInterface;
import org.jmol.atomdata.AtomData;
import org.jmol.atomdata.RadiusData;
import org.jmol.atomdata.RadiusData.EnumType;
import org.jmol.bspt.Bspf;
import org.jmol.c.PAL;
import org.jmol.c.VDW;
import javajs.util.BS;
import org.jmol.modelsetbio.BioModelSet;

import org.jmol.util.Elements;
import org.jmol.util.GData;

import javajs.util.A4;
import javajs.util.M3;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.Quat;

import org.jmol.util.BSUtil;
import org.jmol.util.Parser;
import org.jmol.util.Tensor;
import org.jmol.util.Escape;
import org.jmol.util.Edge;
import org.jmol.util.Logger;
import javajs.util.T3;
import javajs.util.V3;
import org.jmol.util.Vibration;

import org.jmol.viewer.JC;
import org.jmol.script.T;
import org.jmol.viewer.Viewer;

abstract public class AtomCollection {
  
  private final static float almost180 = (float) Math.PI * 0.95f;
  private final static float sqrt3_2 = (float) (Math.sqrt(3) / 2);
  private final static V3 vRef = V3.new3(3.14159f, 2.71828f, 1.41421f);

  public Viewer vwr;
  protected GData g3d;
  /**
   * If any model in the collection is a BioModel, then
   * it is also indicated here as a "bioModelset", meaning
   * 
   */
  public BioModelSet bioModelset;

  public Atom[] at;
  
  public Atom getAtom(int iatom) {
    return (iatom >= 0 && iatom < at.length ? at[iatom] : null);
  }

  public int ac;

  public Trajectory trajectory;

  protected SymmetryInterface pointGroup;

  ////////////////////
  
  private LabelToken labeler;
  
  // the maximum BondingRadius seen in this set of atoms
  // used in autobonding
  protected float maxBondingRadius = PT.FLOAT_MIN_SAFE;
  private float maxVanderwaalsRadius = PT.FLOAT_MIN_SAFE;

  private boolean hasBfactorRange;
  private int bfactor100Lo;
  private int bfactor100Hi;

  private boolean haveBSVisible, haveBSClickable;
  
  private BS bsSurface;
  private int nSurfaceAtoms;
  private int surfaceDistanceMax;

//  protected P3 averageAtomPoint;

  protected boolean haveChirality;

  /**
   *  Binary Space Partitioning Forest
   */
  protected Bspf bspf = null;

  protected boolean preserveState = true;
  public boolean canSkipLoad = true;

  public boolean haveStraightness;

  private BS bsHidden;
  public BS bsVisible, bsClickable, bsModulated;

  ////////////////////////////////////////////////////////////////
  // these may or may not be allocated
  // depending upon the AtomSetCollection characteristics
  //
  // used by Atom:
  //
  
  public Object[][] atomTensorList; // specifically now for {*}.adpmin {*}.adpmax
  public Map<String, Lst<Object>> atomTensors;

  protected int[] surfaceDistance100s;
  
  ///// SETTABLE atomic properties ///////////////
  //
  // The following references must be made if a new property is added to this list:
  //
  // 1) Add a new static value to the following TAINT_XXXX list.
  // 2) Add its T.map name to the list in setupAC().
  // 3) Follow all references to both atomType (for a string), formalCharge (for an int), or partialCharge (for a float) 
  //    These will include a reference in setAPa() and setAtomData() here and
  //    to getAtomicPropertyStateBuffer() in viewer.StateCreator
  // 4) add the settable attribute to the static int XXXX definition in script.T
  // 5) check that {*}.xxxx = ....; write state test.spt, and script test.spt all work properly
 
  public BS[] tainted;
  public static String[] userSettableValues;
  
  protected void setupAC() {
    bsHidden = new BS();
    bsVisible = new BS();
    bsClickable = new BS();
    // this allows the Google Closure compiler to skip all the TAINTED defs in Clazz.defineStatics
    if (userSettableValues == null)
      userSettableValues = ("atomName atomType coord element formalCharge hydrophobicity " +
          "ionic occupancy partialCharge temperature valence vanderWaals vibrationVector " +
          "atomNo seqID resNo chain").split(" ");
  }

  final public static int TAINT_ATOMNAME = 0;
  final public static int TAINT_ATOMTYPE = 1;
  final public static int TAINT_COORD = 2;
  final public static int TAINT_ELEMENT = 3;
  final public static int TAINT_FORMALCHARGE = 4;
  final public static int TAINT_HYDROPHOBICITY = 5;
  final public static int TAINT_BONDINGRADIUS = 6;
  final public static int TAINT_OCCUPANCY = 7;
  final public static int TAINT_PARTIALCHARGE = 8;
  final public static int TAINT_TEMPERATURE = 9;
  final public static int TAINT_VALENCE = 10;
  final public static int TAINT_VANDERWAALS = 11;
  final public static int TAINT_VIBRATION = 12;
  final public static int TAINT_ATOMNO = 13;
  final public static int TAINT_SEQID = 14;
  final public static int TAINT_RESNO = 15;
  final public static int TAINT_CHAIN = 16;
  final public static int TAINT_MAX = 17; // 1 more than last number, above
  

  String[] atomNames;
  String[] atomTypes;
  // String[] chainIDs; not necessary, as there is a place for this already in atom.group.chain
  int[] atomSerials;
  int[] atomResnos;
  int[] atomSeqIDs;
  float[] dssrData;
  public Vibration[] vibrations;
  public float[] occupancies;
  short[] bfactor100s;
  float[] partialCharges;
  float[] bondingRadii;
  float[] hydrophobicities;
  public BS bsPartialCharges;
  
  
  protected void releaseModelSetAC() {
    at = null;
    vwr = null;
    g3d = null;
    bspf = null;
    surfaceDistance100s = null;
    bsSurface = null;
    tainted = null;

    atomNames = null;
    atomTypes = null;
    atomResnos = null;
    dssrData = null;
    atomSerials = null;
    atomSeqIDs = null;
    vibrations = null;
    occupancies = null;
    bfactor100s = null;
    resetPartialCharges();
    bondingRadii = null;
    atomTensors = null;
  }

  protected void mergeAtomArrays(AtomCollection mergeModelSet) {
    tainted = mergeModelSet.tainted;
    atomNames = mergeModelSet.atomNames;
    atomTypes = mergeModelSet.atomTypes;
    atomResnos = mergeModelSet.atomResnos;
    dssrData = mergeModelSet.dssrData;
    atomSerials = mergeModelSet.atomSerials;
    atomSeqIDs = mergeModelSet.atomSeqIDs;
    vibrations = mergeModelSet.vibrations;
    occupancies = mergeModelSet.occupancies;
    bfactor100s = mergeModelSet.bfactor100s;
    bondingRadii = mergeModelSet.bondingRadii;
    partialCharges = mergeModelSet.partialCharges;
    bsPartialCharges = mergeModelSet.bsPartialCharges;
    atomTensors = mergeModelSet.atomTensors;
    atomTensorList = mergeModelSet.atomTensorList;
    bsModulated = mergeModelSet.bsModulated;
    haveStraightness = false;
    surfaceDistance100s = null;
  }
  
  public Lst<P3> getAtomPointVector(BS bs) {
    Lst<P3> v = new  Lst<P3>();
    int n = ac;
    if (bs != null) {
      for (int i = bs.nextSetBit(0); i >= 0 && i < n; i = bs.nextSetBit(i+1)) {
        v.addLast(at[i]);
      }
    }
    return v;
  }

  public boolean modelSetHasVibrationVectors(){
    return (vibrations != null);
  }
  
  public String[] getAtomTypes() {
    return atomTypes;
  }

  public float[] getPartialCharges() {
    return partialCharges;
  }

  public float[] getBondingRadii() {
    return bondingRadii;
  }
  
  public short[] getBFactors() {
    return bfactor100s;
  }

  public float[] getHydrophobicity() {
    return hydrophobicities;
  }
  

  public void setBsHidden(BS bs) { //from selection manager
    bsHidden = bs;
  }

  public boolean isAtomHidden(int iAtom) {
    return bsHidden.get(iAtom);
  }
  
  //////////// atoms //////////////
  
  public LabelToken getLabeler() {
    // prevents JavaScript from requiring LabelToken upon core load
    // will be abbreviated to JM., so don't use getOption here.
    return (labeler == null ? labeler = (LabelToken) Interface.getInterface("org.jmol.modelset.LabelToken", vwr, "ms") : labeler);
  }

  public String getAtomInfo(int i, String format, P3 ptTemp) {
    return (format == null ? at[i].getInfo() 
        : getLabeler().formatLabel(vwr, at[i], format, ptTemp));
  }

  public String getElementName(int i) {
      return Elements.elementNameFromNumber(at[i]
          .getAtomicAndIsotopeNumber());
  }

  public Quat getQuaternion(int i, char qtype) {
    return (i < 0 ? null : at[i].group.getQuaternion(qtype));
  } 

  public int getFirstAtomIndexFromAtomNumber(int atomNumber, BS bsVisibleFrames) {
    //definitely want FIRST (model) not last here
    for (int i = 0; i < ac; i++) {
      Atom atom = at[i];
      if (atom.getAtomNumber() == atomNumber && bsVisibleFrames.get(atom.mi))
        return i;
    }
    return -1;
  }

  public void setFormalCharges(BS bs, int formalCharge) {
    if (bs != null) {
      resetPartialCharges();
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        at[i].setFormalCharge(formalCharge);
        taintAtom(i, TAINT_FORMALCHARGE);
      }
    }
  }
  
  public float[] getAtomicCharges() {
    float[] charges = new float[ac];
    for (int i = ac; --i >= 0; )
      charges[i] = at[i].getElementNumber();
    return charges;
  }

  protected float getRadiusVdwJmol(Atom atom) {
    return Elements.getVanderwaalsMar(atom.getElementNumber(),
        VDW.JMOL) / 1000f;
  }
  
  public float getMaxVanderwaalsRadius() {
    //Dots
    if (maxVanderwaalsRadius == PT.FLOAT_MIN_SAFE)
      findMaxRadii();
    return maxVanderwaalsRadius;
  }

  protected void findMaxRadii() {
    float r;
    for (int i = ac; --i >= 0;) {
      Atom atom = at[i];
      if ((r = atom.getBondingRadius()) > maxBondingRadius)
        maxBondingRadius = r;
      if ((r = atom.getVanderwaalsRadiusFloat(vwr, VDW.AUTO)) > maxVanderwaalsRadius)
        maxVanderwaalsRadius = r;
    }
  }

  public void clearBfactorRange() {
    hasBfactorRange = false;
  }

  private void calcBfactorRange(BS bs) {
    if (hasBfactorRange)
      return;
    bfactor100Lo = Integer.MAX_VALUE;
    bfactor100Hi = Integer.MIN_VALUE;
    if (bs == null) {
      for (int i = 0; i < ac; i++)
        setBf(i);
    } else {
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1))
        setBf(i);
    }
    hasBfactorRange = true;
  }

  private void setBf(int i) {
    int bf = at[i].getBfactor100();
    if (bf < bfactor100Lo)
      bfactor100Lo = bf;
    else if (bf > bfactor100Hi)
      bfactor100Hi = bf;    
  }
  
  public int getBfactor100Lo() {
    //ColorManager
    if (!hasBfactorRange) {
      if (vwr.g.rangeSelected) {
        calcBfactorRange(vwr.bsA());
      } else {
        calcBfactorRange(null);
      }
    }
    return bfactor100Lo;
  }

  public int getBfactor100Hi() {
    //ColorManager
    getBfactor100Lo();
    return bfactor100Hi;
  }

  public int getSurfaceDistanceMax() {
    //ColorManager, Eval
    if (surfaceDistance100s == null)
      calcSurfaceDistances();
    return surfaceDistanceMax;
  }

  public float calculateVolume(BS bs, VDW vType) {
    // Eval
    float volume = 0;
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        volume += at[i].getVolume(vwr, vType);
    return volume;
  }
  
  int getSurfaceDistance100(int atomIndex) {
    //atom
    if (nSurfaceAtoms == 0)
      return -1;
    if (surfaceDistance100s == null)
      calcSurfaceDistances();
    return surfaceDistance100s[atomIndex];
  }

  private void calcSurfaceDistances() {
    calculateSurface(null, -1);
  }
  
  public P3[] calculateSurface(BS bsSelected, float envelopeRadius) {
    if (envelopeRadius < 0)
      envelopeRadius = JC.ENC_CALC_MAX_DIST;
    
    JmolEnvCalc ec = ((JmolEnvCalc) Interface.getOption("geodesic.EnvelopeCalculation", vwr, "ms"))
    .set(vwr, ac, null);
    ec.calculate(new RadiusData(null, envelopeRadius, EnumType.ABSOLUTE, null), 
        Float.MAX_VALUE, 
        bsSelected, BSUtil.copyInvert(bsSelected, ac), 
        false, false, false, true);
    P3[] points = ec.getPoints();
    surfaceDistanceMax = 0;
    bsSurface = ec.getBsSurfaceClone();
    surfaceDistance100s = new int[ac];
    nSurfaceAtoms = BSUtil.cardinalityOf(bsSurface);
    if (nSurfaceAtoms == 0 || points == null || points.length == 0)
      return points;
    float radiusAdjust = (envelopeRadius == Float.MAX_VALUE ? 0 : envelopeRadius);
    for (int i = 0; i < ac; i++) {
      //surfaceDistance100s[i] = Integer.MIN_VALUE;
      if (bsSurface.get(i)) {
        surfaceDistance100s[i] = 0;
      } else {
        float dMin = Float.MAX_VALUE;
        Atom atom = at[i];
        for (int j = points.length; --j >= 0;) {
          float d = Math.abs(points[j].distance(atom) - radiusAdjust);
          if (d < 0 && Logger.debugging)
            Logger.debug("draw d" + j + " " + Escape.eP(points[j])
                + " \"" + d + " ? " + atom.getInfo() + "\"");
          dMin = Math.min(d, dMin);
        }
        int d = surfaceDistance100s[i] = (int) Math.floor(dMin * 100);
        surfaceDistanceMax = Math.max(surfaceDistanceMax, d);
      }
    }
    return points;
  }

  @SuppressWarnings("unchecked")
  protected void setAtomCoord2(BS bs, int tokType, Object xyzValues) {
    P3 xyz = null;
    P3[] values = null;
    Lst<P3> v = null;
    int type = 0;
    int nValues = 1;
    if (xyzValues instanceof P3) {
      xyz = (P3) xyzValues;
    } else if (xyzValues instanceof Lst<?>) {
      v = (Lst<P3>) xyzValues;
      if ((nValues = v.size()) == 0)
        return;
      type = 1;
    } else if (AU.isAP(xyzValues)) {
      values = (P3[]) xyzValues;
      if ((nValues = values.length) == 0)
        return;
      type = 2;
    } else {
      return;
    }
    int n = 0;
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        switch (type) {
        case 1:
          if (n >= nValues)
            return;
          xyz = v.get(n++);
          break;
        case 2:
          if (n >= nValues)
            return;
          xyz = values[n++];
          break;
        }
        if (xyz != null)
          switch (tokType) {
          case T.xyz:
            setAtomCoord(i, xyz.x, xyz.y, xyz.z);
            break;
          case T.fracxyz:
            at[i].setFractionalCoordTo(xyz, true);
            taintAtom(i, TAINT_COORD);
            break;
          case T.fuxyz:
            at[i].setFractionalCoordTo(xyz, false);
            taintAtom(i, TAINT_COORD);
            break;
          case T.vibxyz:
            setAtomVibrationVector(i, xyz);
            break;
          }
      }
  }

  private void setAtomVibrationVector(int atomIndex, T3 vib) {
    setVibrationVector(atomIndex, vib);  
    taintAtom(atomIndex, TAINT_VIBRATION);
  }
  
  public void setAtomCoord(int atomIndex, float x, float y, float z) {
    if (atomIndex < 0 || atomIndex >= ac)
      return;
    Atom a = at[atomIndex];
    a.set(x, y, z);
    fixTrajectory(a);
    taintAtom(atomIndex, TAINT_COORD);
  }

  private void fixTrajectory(Atom a) {
    if (((ModelSet) this).isTrajectory(a.mi))
      trajectory.fixAtom(a);
  }

  public void setAtomCoordRelative(int atomIndex, float x, float y, float z) {
    if (atomIndex < 0 || atomIndex >= ac)
      return;
    Atom a = at[atomIndex];
    a.add3(x, y, z);
    fixTrajectory(a);
    taintAtom(atomIndex, TAINT_COORD);
  }

  protected void setAtomsCoordRelative(BS bs, float x, float y,
                                      float z) {
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1))
        setAtomCoordRelative(i, x, y, z);
  }

  protected void setAPa(BS bs, int tok, int iValue, float fValue,
                              String sValue, float[] values, String[] list) {
    int n = 0;

    if (values != null && values.length == 0 || bs == null)
      return;
    boolean isAll = (values != null && values.length == ac 
        || list != null && list.length == ac);
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      if (isAll)
        n = i;
      if (values != null) {
        if (n >= values.length)
          return;
        fValue = values[n++];
        if (Float.isNaN(fValue))
          continue;
        iValue = (int) fValue;
      } else if (list != null) {
        if (n >= list.length)
          return;
        sValue = list[n++];
      }
      Atom atom = at[i];
      switch (tok) {
      case T.atomname:
        setAtomName(i, sValue, true);
        break;
      case T.atomtype:
        setAtomType(i, sValue);
        break;
      case T.chain:
        setChainID(i, sValue);
        break;
      case T.atomno:
        setAtomNumber(i, iValue, true);
        break;
      case T.seqid:
        setAtomSeqID(i, iValue);
        break;
      case T.atomx:
      case T.x:
        setAtomCoord(i, fValue, atom.y, atom.z);
        break;
      case T.atomy:
      case T.y:
        setAtomCoord(i, atom.x, fValue, atom.z);
        break;
      case T.atomz:
      case T.z:
        setAtomCoord(i, atom.x, atom.y, fValue);
        break;
      case T.vibx:
      case T.viby:
      case T.vibz:
        setVibrationVector2(i, tok, fValue);
        break;
      case T.fracx:
      case T.fracy:
      case T.fracz:
        atom.setFractionalCoord(tok, fValue, true);
        taintAtom(i, TAINT_COORD);
        break;
      case T.fux:
      case T.fuy:
      case T.fuz:
        atom.setFractionalCoord(tok, fValue, false);
        taintAtom(i, TAINT_COORD);
        break;
      case T.elemno:
      case T.element:
        setElement(atom, iValue, true);
        break;
      case T.formalcharge:
        resetPartialCharges();
        atom.setFormalCharge(iValue);
        taintAtom(i, TAINT_FORMALCHARGE);
        break;
      case T.hydrophobicity:
        setHydrophobicity(i, fValue);
        break;
      case T.occupancy:
        // a legacy thing
        if (fValue < 2 && fValue > 0.01f)
          fValue = 100 * fValue;
        setOccupancy(i, fValue, true);
        break;
      case T.partialcharge:
        setPartialCharge(i, fValue, true);
        break;
      case T.bondingradius:
        setBondingRadius(i, fValue);
        break;
      case T.temperature:
        setBFactor(i, fValue, true);
        break;
      case T.resno:
        setAtomResno(i,  iValue);
        break;
      case T.label:
      case T.format:
        vwr.shm.setAtomLabel(sValue, i);
        break;
      case T.radius:
      case T.spacefill:
        if (fValue < 0)
          fValue = 0;
        else if (fValue > Atom.RADIUS_MAX)
          fValue = Atom.RADIUS_GLOBAL;
        atom.madAtom = ((short) (fValue * 2000));
        break;
      case T.selected:
        vwr.slm.setSelectedAtom(atom.i, (fValue != 0));
        break;
      case T.valence:
        atom.setValence(iValue);
        taintAtom(i, TAINT_VALENCE);
        break;
      case T.vanderwaals:
        if (atom.setRadius(fValue))
          taintAtom(i, TAINT_VANDERWAALS);
        else
          untaint(i, TAINT_VANDERWAALS);
        break;
      default:
        Logger.error("unsettable atom property: " + T.nameOf(tok));
        return;
      }
    }
    switch (tok) {
    case T.selected:
      vwr.slm.setSelectedAtom(-1, false);
      break;
    case T.radius:
    case T.spacefill:
      vwr.setShapeSize(JC.SHAPE_BALLS, Integer.MAX_VALUE, bs);
    }
  }

  /**
   * also handles modulation info
   * 
   * @param atomIndex
   * @param c
   * @return value or NaN
   */
  public float getVibCoord(int atomIndex, char c) {
    JmolModulationSet ms = null;
    Vibration v = null;
    switch (c) {
    case 'x':
    case 'y':
    case 'z':
      v = getVibration(atomIndex, false);
      break;
    default:
      ms = getModulation(atomIndex);
      if (ms != null) {
        v = ms.getVibration(false);
        if (v == null)
          v = (Vibration) ms;
      }
    }
    if (v == null && ms == null)
      return Float.NaN;
    switch (c) {
    case 'x':
    case 'X':
      return v.x;
    case 'y':
    case 'Y':
      return v.y;
    case 'z':
    case 'Z':
      return v.z;
    case 'O':
      return ((Float) ms.getModulation('O', null)).floatValue();
    case '1':
    case '2':
    case '3':
      T3 t = (T3) ms.getModulation('T', null);
      float x = (c == '1' ? t.x : c == '2' ? t.y : t.z);
      return (float) (x - Math.floor(x));
    default:
      return Float.NaN;
    }
  }

  public Vibration getVibration(int atomIndex, boolean forceNew) {
    Vibration v = (vibrations == null  ? null : (Vibration) vibrations[atomIndex]);
    return (v instanceof JmolModulationSet ? ((JmolModulationSet) v).getVibration(forceNew)
        : v == null && forceNew ? new Vibration() : v);
  }

  public JmolModulationSet getModulation(int iAtom) {
    Vibration v = (vibrations == null  ? null : (Vibration) vibrations[iAtom]);
    return (JmolModulationSet) (v != null && v.modDim > 0 ? v : null);
  }

  protected void setVibrationVector(int atomIndex, T3 vib) {
    if (Float.isNaN(vib.x) || Float.isNaN(vib.y) || Float.isNaN(vib.z))
      return;
    if (vibrations == null || vibrations.length < atomIndex)
      vibrations = new Vibration[at.length];
    if (vib instanceof Vibration) {
      vibrations[atomIndex] = (Vibration) vib;
    } else {
      if (vibrations[atomIndex] == null)
        vibrations[atomIndex] = new Vibration();
      vibrations[atomIndex].setXYZ(vib);
    }
    at[atomIndex].setVibrationVector();
  }

  private void setVibrationVector2(int atomIndex, int tok, float fValue) {
    Vibration v = getVibration(atomIndex, true);
    if (v == null)
      return;
    switch (tok) {
    case T.vibx:
      v.x = fValue;
      break;
    case T.viby:
      v.y = fValue;
      break;
    case T.vibz:
      v.z = fValue;
      break;
    }
    setAtomVibrationVector(atomIndex, v);
  }

  public void setAtomName(int atomIndex, String name, boolean doTaint) {
    if (doTaint && name.equals(at[atomIndex].getAtomName()))
      return;
    // can't set the name of a bioatom
    byte id = (((ModelSet) this).am[at[atomIndex].mi].isBioModel ? vwr.getJBR()
        .lookupSpecialAtomID(name) : 0);
    at[atomIndex].atomID = id;
    if (id <= 0) {
      if (atomNames == null)
        atomNames = new String[at.length];
      atomNames[atomIndex] = name;
    }
    if (doTaint)
      taintAtom(atomIndex, TAINT_ATOMNAME);
  }

  private void setAtomType(int atomIndex, String type) {
    if (type.equals(at[atomIndex].getAtomType()))
      return;
    if (atomTypes == null)
      atomTypes = new String[at.length];
    atomTypes[atomIndex] = type;
    return;
  }
  
  private void setChainID(int atomIndex, String id) {
    if (id.equals(at[atomIndex].getChainIDStr()))
      return;
    int intid = at[atomIndex].getChainID();
    BS bs = getChainBits(intid);
    Chain c = at[atomIndex].group.chain;
    c.chainID = vwr.getChainID(id, true);
    for (int i = bs.nextSetBit(0); i >= 0; i  = bs.nextSetBit(i + 1))
      taintAtom(i, TAINT_CHAIN);
  }

  public void setAtomNumber(int atomIndex, int atomno, boolean doTaint) {
    if (doTaint && atomno == at[atomIndex].getAtomNumber())
      return;
    if (atomSerials == null)
      atomSerials = new int[at.length];
    atomSerials[atomIndex] = atomno;
    if (doTaint)
      taintAtom(atomIndex, TAINT_ATOMNO);
  }
  
  public void setElement(Atom atom, int atomicNumber, boolean doTaint) {
    if (doTaint && atom.getElementNumber() == atomicNumber)
      return;
    atom.setAtomicAndIsotopeNumber(atomicNumber);
    atom.paletteID = PAL.CPK.id;
    atom.colixAtom = vwr.cm.getColixAtomPalette(atom,
        PAL.CPK.id);
    resetPartialCharges();
    if (doTaint)
      taintAtom(atom.i, TAINT_ELEMENT);
  }

  private void resetPartialCharges() {
    partialCharges = null;
    bsPartialCharges = null;
  }

  private void setAtomResno(int atomIndex, int resno) {
    if (resno == at[atomIndex].getResno())
      return;
    at[atomIndex].group.setResno(resno);
    if (atomResnos == null)
      atomResnos = new int[at.length];
    atomResnos[atomIndex] = resno;
    taintAtom(atomIndex, TAINT_RESNO);
  }
  
  private void setAtomSeqID(int atomIndex, int seqID) {
    if (seqID == at[atomIndex].getSeqID())
      return;
    if (atomSeqIDs == null)
      atomSeqIDs = new int[at.length];
    atomSeqIDs[atomIndex] = seqID;
    taintAtom(atomIndex, TAINT_SEQID);
  }
  
  protected void setOccupancy(int atomIndex, float occupancy, boolean doTaint) {
    if (doTaint && occupancy == at[atomIndex].getOccupancy100())
      return;
    if (occupancies == null) {
      if (occupancy == 100)
        return; // 100 is the default;
      occupancies = new float[at.length];
      for (int i = at.length; --i >= 0;)
        occupancies[i] = 100;
    }
    occupancies[atomIndex] = occupancy;
    if (doTaint)
      taintAtom(atomIndex, TAINT_OCCUPANCY);
  }
  
  protected void setPartialCharge(int atomIndex, float partialCharge, boolean doTaint) {
    if (Float.isNaN(partialCharge))
      return;
    if (partialCharges == null) {
      bsPartialCharges = new BS();
      if (partialCharge == 0)
        return; // no need to store a 0.
      partialCharges = new float[at.length];
    }
    bsPartialCharges.set(atomIndex);
    partialCharges[atomIndex] = partialCharge;   
    if (doTaint)
      taintAtom(atomIndex, TAINT_PARTIALCHARGE);
  }

  private void setBondingRadius(int atomIndex, float radius) {
    if (Float.isNaN(radius) || radius == at[atomIndex].getBondingRadius())
      return;
    if (bondingRadii == null)
      bondingRadii = new float[at.length];
    bondingRadii[atomIndex] = radius;
    taintAtom(atomIndex, TAINT_BONDINGRADIUS);
  }

  protected void setBFactor(int atomIndex, float bfactor, boolean doTaint) {
    if (Float.isNaN(bfactor) || doTaint && bfactor == at[atomIndex].getBfactor100())
      return;
    if (bfactor100s == null) {
      if (bfactor == 0) // there's no need to store a 0.
        return;
      bfactor100s = new short[at.length];
    }
    bfactor100s[atomIndex] = (short) ((bfactor < -327.68f ? -327.68f
        : bfactor > 327.67 ? 327.67 : bfactor) * 100 + (bfactor < 0 ? -0.5 : 0.5));
    if (doTaint)
      taintAtom(atomIndex, TAINT_TEMPERATURE);
  }

  private void setHydrophobicity(int atomIndex, float value) {
    if (Float.isNaN(value) || value == at[atomIndex].getHydrophobicity())
      return;
    if (hydrophobicities == null) {
      hydrophobicities = new float[at.length];
      for (int i = 0; i < at.length; i++)
        hydrophobicities[i] = Elements.getHydrophobicity(at[i].group.groupID);
    }
    hydrophobicities[atomIndex] = value;
    taintAtom(atomIndex, TAINT_HYDROPHOBICITY);
  }

  // loading data
  
  public void setAtomData(int type, String name, String dataString,
                          boolean isDefault) {
    float[] fData = null;
    BS bs = null;
    switch(type) {
    case TAINT_COORD:
      loadCoordinates(dataString, false, !isDefault);
      return;
    case TAINT_VIBRATION:
      loadCoordinates(dataString, true, true);
      return;
    case TAINT_MAX:
      fData = new float[ac];
      bs = BS.newN(ac);
      break;
    }
    int[] lines = Parser.markLines(dataString, ';');
    int n = 0;
    try {
      int nData = PT.parseInt(dataString.substring(0, lines[0] - 1));
      for (int i = 1; i <= nData; i++) {
        String[] tokens = PT.getTokens(PT.parseTrimmed(dataString.substring(
            lines[i], lines[i + 1] - 1)));
        int atomIndex = PT.parseInt(tokens[0]) - 1;
        if (atomIndex < 0 || atomIndex >= ac)
          continue;
        Atom atom = at[atomIndex];
        n++;
        int pt = tokens.length - 1;
        float x = PT.parseFloat(tokens[pt]);
        switch (type) {
        case TAINT_MAX:
          fData[atomIndex] = x;
          bs.set(atomIndex);
          continue;
        case TAINT_ATOMNAME:
          setAtomName(atomIndex, tokens[pt], true);
          break;
        case TAINT_ATOMNO:
          setAtomNumber(atomIndex, (int) x, true);
          break;
        case TAINT_RESNO:
          setAtomResno(atomIndex, (int) x);
          break;
        case TAINT_SEQID:
          setAtomSeqID(atomIndex, (int) x);
          break;
        case TAINT_ATOMTYPE:
          setAtomType(atomIndex, tokens[pt]);
          break;
        case TAINT_CHAIN:
          setChainID(atomIndex, tokens[pt]);
          break;
        case TAINT_ELEMENT:
          atom.setAtomicAndIsotopeNumber((int)x);
          atom.paletteID = PAL.CPK.id;
          atom.colixAtom = vwr.cm.getColixAtomPalette(atom, PAL.CPK.id);
          break;
        case TAINT_FORMALCHARGE:
          atom.setFormalCharge((int)x);          
          break;
        case TAINT_HYDROPHOBICITY:
          setHydrophobicity(atomIndex, x);
          break;
        case TAINT_BONDINGRADIUS:
          setBondingRadius(atomIndex, x);          
          break;
        case TAINT_PARTIALCHARGE:
          setPartialCharge(atomIndex, x, true);          
          break;
        case TAINT_TEMPERATURE:
          setBFactor(atomIndex, x, true);
          break;
        case TAINT_VALENCE:
          atom.setValence((int)x);     
          break;
        case TAINT_VANDERWAALS:
          atom.setRadius(x);          
          break;
        }
        taintAtom(atomIndex, type);
      }
      if (type == TAINT_MAX && n > 0)
        vwr.setData(name, new Object[] {name, fData, bs, Integer.valueOf(JmolDataManager.DATA_TYPE_AF)}, 0, 0, 0, 0, 0);
        
    } catch (Exception e) {
      Logger.error("AtomCollection.loadData error: " + e);
    }  
  }
  
  private void loadCoordinates(String data, boolean isVibrationVectors, boolean doTaint) {
    int[] lines = Parser.markLines(data, ';');
    V3 v = (isVibrationVectors ? new V3() : null);
    try {
      int nData = PT.parseInt(data.substring(0, lines[0] - 1));
      for (int i = 1; i <= nData; i++) {
        String[] tokens = PT.getTokens(PT.parseTrimmed(data.substring(
            lines[i], lines[i + 1])));
        int atomIndex = PT.parseInt(tokens[0]) - 1;
        // JavaScript will not be exact, and we will be checking for the exact value
        // Modulation setting modScale
        float x = (tokens[3].equalsIgnoreCase("1.4E-45") ? 1.4e-45f : PT.parseFloat(tokens[3]));
        float y = (tokens[4].equalsIgnoreCase("1.4E-45") ? 1.4e-45f : PT.parseFloat(tokens[4]));
        float z = PT.parseFloat(tokens[5]);
        if (isVibrationVectors) {
          v.set(x, y, z);
          setAtomVibrationVector(atomIndex, v);
        } else {
          setAtomCoord(atomIndex, x, y, z);
          if (!doTaint)
            untaint(atomIndex, TAINT_COORD);
        }
      }
    } catch (Exception e) {
      Logger.error("Frame.loadCoordinate error: " + e);
    }
  }

  public void validateBspf(boolean isValid) {
    if (bspf != null)
      bspf.isValid = isValid;
    //averageAtomPoint = null;
  }

  void validateBspfForModel(int modelIndex, boolean isValid) {
    if (bspf != null)
      bspf.validateModel(modelIndex, isValid);
  }

  // state tainting
  
  public void setPreserveState(boolean TF) {
    preserveState = TF;
  }
  ////  atom coordinate and property changing  //////////
  
  public static int getUserSettableType(String dataType) {
    boolean isExplicit = (dataType.indexOf("property_") == 0);
    String check = (isExplicit ? dataType.substring(9) : dataType);
    for (int i = 0; i < TAINT_MAX; i++)
      if (userSettableValues[i].equalsIgnoreCase(check))
        return i;
    return (isExplicit ? TAINT_MAX : -1);
  }

  public BS getTaintedAtoms(int type) {
    return tainted == null ? null : tainted[type];
  }
  
  public void taintAtoms(BS bsAtoms, int type) {
    canSkipLoad = false;
    if (!preserveState)
      return;
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1))
      taintAtom(i, type);
  }

  public void taintAtom(int atomIndex, int type) {
    if (preserveState) {
      if (tainted == null)
        tainted = new BS[TAINT_MAX];
      if (tainted[type] == null)
        tainted[type] = BS.newN(ac);
      tainted[type].set(atomIndex);
    }
    if (type == TAINT_COORD)
      taintModelCoord(atomIndex);
  }

  private void taintModelCoord(int atomIndex) {
    Model m = ((ModelSet) this).am[at[atomIndex].mi];
    validateBspfForModel(m.trajectoryBaseIndex, false);
    if (m.isBioModel)
      m.resetDSSR(true);
    pointGroup = null;
  }

  private void untaint(int atomIndex, int type) {
    if (!preserveState)
      return;
    if (tainted == null || tainted[type] == null)
      return;
    tainted[type].clear(atomIndex);
  }

  public void setTaintedAtoms(BS bs, int type) {
    if (preserveState) {
      if (bs == null) {
        if (tainted == null)
          return;
        tainted[type] = null;
        return;
      }
      if (tainted == null)
        tainted = new BS[TAINT_MAX];
      if (tainted[type] == null)
        tainted[type] = BS.newN(ac);
      BSUtil.copy2(bs, tainted[type]);
    }
    if (type == TAINT_COORD) {
      int i = bs.nextSetBit(0);
      if (i >= 0)
        taintModelCoord(i);
    }
    
  }

  public void unTaintAtoms(BS bs, int type) {
    if (tainted == null || tainted[type] == null)
      return;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1))
      tainted[type].clear(i);
    if (tainted[type].nextSetBit(0) < 0)
      tainted[type] = null;
  }

  ///////////////////////////////////////////

  /*
   * generalized; not just balls
   * 
   * This algorithm assumes that atoms are circles at the z-depth
   * of their center point. Therefore, it probably has some flaws
   * around the edges when dealing with intersecting spheres that
   * are at approximately the same z-depth.
   * But it is much easier to deal with than trying to actually
   * calculate which atom was clicked
   *
   * A more general algorithm of recording which object drew
   * which pixel would be very expensive and not worth the trouble
   */
  protected void findNearest2(int x, int y, Atom[] closest, BS bsNot, int min) {
    Atom champion = null;
    for (int i = ac; --i >= 0;) {
      if (bsNot != null && bsNot.get(i))
        continue;
      Atom contender = at[i];
      if (contender.isClickable()
          && isCursorOnTopOf(contender, x, y, min,
              champion))
        champion = contender;
    }
    closest[0] = champion;
  }

  /**
   * used by Frame and AminoMonomer and NucleicMonomer -- does NOT check for clickability
   * @param contender
   * @param x
   * @param y
   * @param radius
   * @param champion
   * @return true if user is pointing to this atom
   */
  boolean isCursorOnTopOf(Atom contender, int x, int y, int radius,
                          Atom champion) {
    return contender.sZ > 1 && !g3d.isClippedZ(contender.sZ)
        && g3d.isInDisplayRange(contender.sX, contender.sY)
        && contender.isCursorOnTopOf(x, y, radius, champion);
  }

  protected void fillADa(AtomData atomData, int mode) {
    atomData.xyz = atomData.atoms = at;
    atomData.ac = ac;
    atomData.atomicNumber = new int[ac];
    boolean includeRadii = ((mode & AtomData.MODE_FILL_RADII) != 0);
    if (includeRadii)
      atomData.atomRadius = new float[ac];
    boolean isMultiModel = ((mode & AtomData.MODE_FILL_MULTIMODEL) != 0);
    for (int i = 0; i < ac; i++) {
      Atom atom = at[i];
      if (atom.isDeleted() || !isMultiModel && atomData.modelIndex >= 0
          && atom.mi != atomData.firstModelIndex) {
        if (atomData.bsIgnored == null)
          atomData.bsIgnored = new BS();
        atomData.bsIgnored.set(i);
        continue;
      }
      atomData.atomicNumber[i] = atom.getElementNumber();
      atomData.lastModelIndex = atom.mi;
      if (includeRadii)
        atomData.atomRadius[i] = getWorkingRadius(atom, atomData); 
    }
  }
  
  ////// hybridization ///////////

  @SuppressWarnings("incomplete-switch")
  private float getWorkingRadius(Atom atom, AtomData atomData) {
    float r = 0;
    RadiusData rd = atomData.radiusData;
    switch (rd.factorType) {
    case ABSOLUTE:
      r = rd.value;
      break;
    case FACTOR:
    case OFFSET:
      switch (rd.vdwType) {
      case BONDING:
        r = atom.getBondingRadius();
        break;
      case ADPMAX:
        r = atom.getADPMinMax(true);
        break;
      case ADPMIN:
        r = atom.getADPMinMax(false);
        break;
      default:
        r = atom.getVanderwaalsRadiusFloat(vwr,
            atomData.radiusData.vdwType);
      }
      if (rd.factorType == EnumType.FACTOR)
        r *= rd.value;
      else
        r += rd.value;
    }
    return r + rd.valueExtended;
  }

  /**
   * get a list of potential H atom positions based on 
   * elemental valence and formal charge
   * 
   * @param bs
   * @param nTotal
   * @param doAll       -- whether we add to C that already have H or not.
   * @param justCarbon
   * @param vConnect 
   * @return     array of arrays of points added to specific atoms
   */
  public P3[][] calculateHydrogens(BS bs, int[] nTotal,
                                            boolean doAll, boolean justCarbon,
                                            Lst<Atom> vConnect) {
    V3 z = new V3();
    V3 x = new V3();
    P3[][] hAtoms = new P3[ac][];
    BS bsDeleted = vwr.slm.bsDeleted;
    P3 pt;
    int nH = 0;
    
    // just not doing aldehydes here -- all A-X-B bent == sp3 for now
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        if (bsDeleted != null && bsDeleted.get(i))
          continue;
        Atom atom = at[i];
        int atomicNumber = atom.getElementNumber();
        if (justCarbon && atomicNumber != 6)
          continue;
        float dHX = (atomicNumber <= 6 ? 1.1f // B, C
            : atomicNumber <= 10 ? 1.0f       // N, O
            : 1.3f);                          // S
        switch (atomicNumber) {
        case 7:
        case 8:
          dHX = 1.0f;
          break;
        case 6:
        }
        if (doAll && atom.getCovalentHydrogenCount() > 0)
          continue;
        int n = getMissingHydrogenCount(atom, false);
        if (n == 0)
          continue;
        int targetValence = aaRet[0];
        int hybridization = aaRet[2];
        int nBonds = aaRet[3];
        if (nBonds == 0 && atom.isHetero())
          continue; // no adding to water
        hAtoms[i] = new P3[n];
        int hPt = 0;
        if (nBonds == 0) {
          switch (n) {
          case 4:
            // methane
            z.set(0.635f, 0.635f, 0.635f);
            pt = P3.newP(z);
            pt.add(atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.addLast(atom);
            //$FALL-THROUGH$
          case 3:
            // nitrogen
            z.set(-0.635f, -0.635f, 0.635f);
            pt = P3.newP(z);
            pt.add(atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.addLast(atom);
            //$FALL-THROUGH$
          case 2:
            // oxygen
            z.set(-0.635f, 0.635f, -0.635f);
            pt = P3.newP(z);
            pt.add(atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.addLast(atom);
            //$FALL-THROUGH$
          case 1:
            z.set(0.635f, -0.635f, -0.635f);
            pt = P3.newP(z);
            pt.add(atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.addLast(atom);
          }
        } else {
          switch (n) {
          default:
            break;
          case 3: // three bonds needed RC
            getHybridizationAndAxes(i, atomicNumber, z, x, "sp3b", false, true);
            pt = new P3();
            pt.scaleAdd2(dHX, z, atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.addLast(atom);
            getHybridizationAndAxes(i, atomicNumber, z, x, "sp3c", false, true);
            pt = new P3();
            pt.scaleAdd2(dHX, z, atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.addLast(atom);
            getHybridizationAndAxes(i, atomicNumber, z, x, "sp3d", false, true);
            pt = new P3();
            pt.scaleAdd2(dHX, z, atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.addLast(atom);
            break;
          case 2:
            // 2 bonds needed R2C or R-N or R2C=C or O
            //                    or RC=C or C=C
            boolean isEne = (hybridization == 2 || atomicNumber == 5 || nBonds == 1
                && targetValence == 4 
                || atomicNumber == 7 && isAdjacentSp2(atom));
            getHybridizationAndAxes(i, atomicNumber, z, x, (isEne ? "sp2b"
                : targetValence == 3 ? "sp3c" : "lpa"), false, true);
            pt = P3.newP(z);
            pt.scaleAdd2(dHX, z, atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.addLast(atom);
            getHybridizationAndAxes(i, atomicNumber, z, x, (isEne ? "sp2c"
                : targetValence == 3 ? "sp3d" : "lpb"), false, true);
            pt = P3.newP(z);
            pt.scaleAdd2(dHX, z, atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.addLast(atom);
            break;
          case 1:
            // one bond needed R2B, R3C, R-N-R, R-O R=C-R R=N R-3-C
            // nbonds ......... 2 .. 3 .. 2 ... 1 ... 2 .. 1 .. 1
            // nval ........... 2 .. 3 .. 2 ... 1 ... 3 .. 2 .. 3
            // targetValence .. 3 .. 4 .. 3 ... 2 ... 4 .. 3 .. 4
            // tV - nbonds   .. 1    1    1     1     2    2    3
            // ................ sp2c sp3d sp3d  sp3b  sp2c sp2b sp
            switch (targetValence - nBonds) {
            case 1:
              // sp3 or Boron sp2 or N sp2
              if (atomicNumber == 8 && atom == atom.group.getCarbonylOxygenAtom()) {
                hAtoms[i] = null;
                continue;
              }
              if (getHybridizationAndAxes(i, atomicNumber, z, x, (hybridization == 2 || atomicNumber == 5 
                  || atomicNumber == 7 
                  && (atom.group.getNitrogenAtom() == atom || isAdjacentSp2(atom))
                  ? "sp2c"
                  : "sp3d"), true, false) != null) {
                pt = P3.newP(z);
                pt.scaleAdd2(dHX, z, atom);
                hAtoms[i][hPt++] = pt;
                if (vConnect != null)
                  vConnect.addLast(atom);
              } else {
                hAtoms[i] = new P3[0];
              }
              break;
            case 2:
              // sp2
              getHybridizationAndAxes(i, atomicNumber, z, x, (targetValence == 4 ? "sp2c"
                  : "sp2b"), false, false);
              pt = P3.newP(z);
              pt.scaleAdd2(dHX, z, atom);
              hAtoms[i][hPt++] = pt;
              if (vConnect != null)
                vConnect.addLast(atom);
              break;
            case 3:
              // sp
              getHybridizationAndAxes(i, atomicNumber, z, x, "spb", false, true);
              pt = P3.newP(z);
              pt.scaleAdd2(dHX, z, atom);
              hAtoms[i][hPt++] = pt;
              if (vConnect != null)
                vConnect.addLast(atom);
              break;
            }
          }
        }
        nH += hPt;
      }
    nTotal[0] = nH;
    return hAtoms;
  }

  private boolean isAdjacentSp2(Atom atom) {
    Bond[] bonds = atom.bonds;
    for (int i = 0; i < bonds.length; i++) {
      Bond[] b2 = bonds[i].getOtherAtom(atom).bonds;
      for (int j = 0; j < b2.length; j++)
        switch (b2[j].order) {
        case Edge.BOND_AROMATIC:
        case Edge.BOND_AROMATIC_DOUBLE:
        case Edge.BOND_COVALENT_DOUBLE:
        case Edge.BOND_COVALENT_TRIPLE:
          return true;
        }
    }
    return false;
  }

  int[] aaRet;
  
  public int getMissingHydrogenCount(Atom atom, boolean allowNegative) {
    int targetCount = atom.getTargetValence();
    if (targetCount < 0)
      return 0;
    int charge = atom.getFormalCharge();
    int valence = atom.getValence();
    Model model = ((ModelSet) this).am[atom.mi];
    String s = (model.isBioModel && !model.isPdbWithMultipleBonds ? atom.group.getGroup3() : null);
    if (aaRet == null)
      aaRet = new int[5];
    aaRet[0] = targetCount;
    aaRet[1] = charge;
    aaRet[2] = 0; // hybridization
    aaRet[3] = atom.getCovalentBondCount();
    aaRet[4] = (s == null ? 0 : valence);
    if (s != null && charge == 0) {
      if (bioModelset.getAminoAcidValenceAndCharge(s, atom.getAtomName(),
          aaRet)) {
        targetCount = aaRet[0];
        charge = aaRet[1];
      }
    }
    if (charge != 0) {
      targetCount += (targetCount == 4 ? -Math.abs(charge) : charge);
      aaRet[0] = targetCount;
    }
    int n = targetCount - valence;
    return (n < 0 && !allowNegative ? 0 : n);
  }

  public int fixFormalCharges(BS bs) {
    int n = 0;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      Atom a = at[i];
      int nH = getMissingHydrogenCount(a, true);
      if (nH != 0) {

          int c0 = a.getFormalCharge();
        int c = c0 - nH;
        a.setFormalCharge(c);
        taintAtom(i, TAINT_FORMALCHARGE);
        if (Logger.debugging)
          Logger.debug("atom " + a + " formal charge " + c0 + " -> " + c);
        n++;
      }
    }    
    return n;
  }

  public String getHybridizationAndAxes(int atomIndex, int atomicNumber, V3 z, V3 x,
                                        String lcaoTypeRaw,
                                        boolean hybridizationCompatible,
                                        boolean doAlignZ) {

    String lcaoType = (lcaoTypeRaw.length() > 0 && lcaoTypeRaw.charAt(0) == '-' ? lcaoTypeRaw
        .substring(1)
        : lcaoTypeRaw);

    if (lcaoTypeRaw.indexOf("d") >= 0 && !lcaoTypeRaw.endsWith("sp3d"))
      return getHybridizationAndAxesD(atomIndex, z, x, lcaoType);

    Atom atom = at[atomIndex];
    if (atomicNumber == 0)
      atomicNumber = atom.getElementNumber();
    Atom[] attached = getAttached(atom, 4, hybridizationCompatible);
    int nAttached = attached.length;
    int pt = lcaoType.charAt(lcaoType.length() - 1) - 'a';
    if (pt < 0 || pt > 6)
      pt = 0;
    z.set(0, 0, 0);
    x.set(0, 0, 0);
    V3[] v = new V3[4];
    for (int i = 0; i < nAttached; i++) {
      v[i] = V3.newVsub(atom, attached[i]);
      v[i].normalize();
      z.add(v[i]);
    }
    if (nAttached > 0)
      x.setT(v[0]);
    boolean isPlanar = false;
    V3 vTemp = new V3();
    if (nAttached >= 3) {
      if (x.angle(v[1]) < almost180)
        vTemp.cross(x, v[1]);
      else
        vTemp.cross(x, v[2]);
      vTemp.normalize();
      V3 vTemp2 = new V3();
      if (v[1].angle(v[2]) < almost180)
        vTemp2.cross(v[1], v[2]);
      else
        vTemp2.cross(x, v[2]);
      vTemp2.normalize();
      isPlanar = (Math.abs(vTemp2.dot(vTemp)) >= 0.95f);
    }

    boolean isSp3 = (lcaoType.indexOf("sp3") == 0);
    boolean isSp2 = (!isSp3 && lcaoType.indexOf("sp2") == 0);
    boolean isSp = (!isSp3 && !isSp2 && lcaoType.indexOf("sp") == 0);
    boolean isP = (lcaoType.indexOf("p") == 0);
    boolean isLp = (lcaoType.indexOf("lp") == 0);
    
    String hybridization = null;
    if (hybridizationCompatible) {
      if (nAttached == 0)
        return null;
      if (isSp3) {
        if (pt > 3 || nAttached > 4)
          return null;
      } else if (isSp2) {
        if (pt > 2 || nAttached > 3)
          return null;
      } else if (isSp) {
        if (pt > 1 || nAttached > 2)
          return null;
      }
      switch (nAttached) {
      case 1:
        if (atomicNumber == 1 && !isSp3)
          return null;
        if (isSp3) {
          hybridization = "sp3";
          break;
        }
        switch (attached[0].getCovalentBondCount()) {
        case 1:
          if (attached[0].getValence() != 2) {
            // C-t-C
            hybridization = "sp";
            break;
          }
          // C=C, no other atoms
          //$FALL-THROUGH$
        case 2:
          hybridization = (isSp ? "sp" : "sp2");
          break;
        case 3:
          // special case, for example R2C=O oxygen
          if (!isSp2 && !isP)
            return null;
          hybridization = "sp2";
          break;
        }
        break;
      case 2:
        if (z.length() < 0.1f) {
          // linear A--X--B
          if (lcaoType.indexOf("2") >= 0 || lcaoType.indexOf("3") >= 0)
            return null;
          hybridization = "sp";
          break;
        }
        // bent A--X--B
        hybridization = (isSp3 ? "sp3" : "sp2");
        if (lcaoType.indexOf("sp") == 0) { // align z as sp2 orbital
          break;
        }
        if (isLp) { // align z as lone pair
          hybridization = "lp"; // any is OK
          break;
        }
        hybridization = lcaoType;
        break;
      default:
        // 3 or 4 bonds
        if (isPlanar) {
          hybridization = "sp2";
        } else {
          if (isLp && nAttached == 3) {
            hybridization = "lp";
            break;
          }
          hybridization = "sp3";
        }
      }
      if (hybridization == null)
        return null;
      if (lcaoType.indexOf("p") == 0) {
        if (hybridization == "sp3")
          return null;
      } else if (lcaoType.indexOf(hybridization) < 0) {
        return null;
      }
    }

    if (pt < nAttached && !lcaoType.startsWith("p")
        && !lcaoType.startsWith("l")) {
      z.sub2(attached[pt], atom);
      z.normalize();
      return hybridization;
    }

    switch (nAttached) {
    case 0:
      if (lcaoType.equals("sp3c") || lcaoType.equals("sp2d")
          || lcaoType.equals("lpa")) {
        z.set(-0.5f, -0.7f, 1);
        x.set(1, 0, 0);
      } else if (lcaoType.equals("sp3b") || lcaoType.equals("lpb")) {
        z.set(0.5f, -0.7f, -1f);
        x.set(1, 0, 0);
      } else if (lcaoType.equals("sp3a")) {
        z.set(0, 1, 0);
        x.set(1, 0, 0);
      } else {
        z.set(0, 0, 1);
        x.set(1, 0, 0);
      }
      break;
    case 1:
      // X-C
      vTemp.setT(vRef);        
      x.cross(vTemp, z);
      if (isSp3) {
        // align z as sp3 orbital
        // with reference to atoms connected to connecting atom.
        // vRef is a pseudo-random vector
        // z is along the bond
        for (int i = attached[0].getBondCount(); --i >= 0;) {
          if (attached[0].bonds[i].isCovalent()
              && attached[0].getBondedAtomIndex(i) != atom.i) {
            x.sub2(attached[0], attached[0].bonds[i].getOtherAtom(attached[0]));
            x.cross(z, x);
            if (x.length() == 0)
              continue;
            x.cross(x, z);
            break;
          }
        }
        x.normalize();
        if (Float.isNaN(x.x)) {
          x.setT(vRef);
          x.cross(x, z);
        }
        // x is perp to bond
        vTemp.cross(z, x);
        vTemp.normalize();
        // y1 is perp to bond and x
        z.normalize();
        x.scaleAdd2(2.828f, x, z); // 2*sqrt(2)
        if (pt != 3) {
          x.normalize();
          // PI*2/3
          new M3().setAA(A4.new4(z.x, z.y, z.z,
              (pt == 2 ? 1 : -1) * 2.09439507f)).rotate(x);
        }
        z.setT(x);
        x.cross(vTemp, z);
        break;
      }
      // not "sp3" -- sp2 or lone pair
      vTemp.cross(x, z); //x and vTemp are now perpendicular to z
      switch (attached[0].getCovalentBondCount()) {
      case 1:
        if (attached[0].getValence() != 2) {
          // C-t-C
          break;
        }
        // C=C, no other atoms
        //$FALL-THROUGH$
      case 2:
        // R-C=C* or C=C=C*
        // get third atom
        boolean isCumulated = false;
        Atom a0 = attached[0];
        x.setT(z);
        vTemp.setT(vRef);        
        while (a0 != null && a0.getCovalentBondCount() == 2) {
          Bond[] bonds = a0.bonds;
          Atom a = null;
          isCumulated = !isCumulated;
          for (int i = 0; i < bonds.length; i++)
            if (bonds[i].isCovalent()) {
              a = bonds[i].getOtherAtom(a0);
              if (a != atom) {
                vTemp.sub2(a, a0);
                break;
              }
            }
          vTemp.cross(vTemp, x);
          if (vTemp.length() > 0.1f || a.getCovalentBondCount() != 2)
            break;
          atom = a0;
          a0 = a;
        }
        if (vTemp.length() > 0.1f) {
          z.cross(vTemp, x);
          // C=C or RC=C
          z.normalize();
          if (pt == 1)
            z.scale(-1);
          z.scale(sqrt3_2);
          z.scaleAdd2(0.5f, x, z);
          if (isP) {
            vTemp.cross(z, x);
            z.setT(vTemp);
            vTemp.setT(x);
          } 
          x.cross(vTemp, z);
        } else {
          z.setT(x);
          x.cross(vRef, x);
        }
        break;
      case 3:
        // special case, for example R2C=O oxygen
        getHybridizationAndAxes(attached[0].i, 0, x, vTemp, "pz", false,
            doAlignZ);
        vTemp.setT(x);
        if (isSp2) { // align z as sp2 orbital
          x.cross(x, z);
          if (pt == 1)
            x.scale(-1);
          x.scale(sqrt3_2);
          z.scaleAdd2(0.5f, z, x);
        } else {
          vTemp.setT(z);
          z.setT(x);
        }
        x.cross(vTemp, z);
        break;
      }
      break;
    case 2:
      // two attached atoms -- check for linearity
      if (z.length() < 0.1f) {
        // linear A--X--B
        if (!lcaoType.equals("pz")) {
          Atom a = attached[0];
          boolean ok = (a.getCovalentBondCount() == 3);
          if (!ok)
            ok = ((a = attached[1]).getCovalentBondCount() == 3);
          if (ok) {
            // special case, for example R2C=C=CR2 central carbon
            getHybridizationAndAxes(a.i, 0, x, z, "pz", false, doAlignZ);
            if (lcaoType.equals("px"))
              x.scale(-1);
            z.setT(v[0]);
            break;
          }
          // O-C*-O
          vTemp.setT(vRef);    
          z.cross(vTemp, x);
          vTemp.cross(z, x);
        }
        z.setT(x);
        x.cross(vTemp, z);
        break;
      }
      // bent A--X--B
      vTemp.cross(z, x);
      if (isSp2) { // align z as sp2 orbital
        x.cross(z, vTemp);
        break;
      }
      if (isSp3 || isLp) { // align z as lone pair
        vTemp.normalize();
        z.normalize();
        if (!lcaoType.equals("lp")) {
          if (pt == 0 || pt == 2) 
            z.scaleAdd2(-1.2f, vTemp, z);
          else
            z.scaleAdd2(1.2f, vTemp, z);
        }
        x.cross(z, vTemp);
        break;
      }
      // align z as p orbital
      x.cross(z, vTemp);
      z.setT(vTemp);
      if (z.z < 0) {
        z.scale(-1);
        x.scale(-1);
      }
      break;
    default:
      // 3 bonds, sp3 or sp2 and lp/p
      if (isSp3)
        break;
      if (!isPlanar) {
        // not aligned -- really sp3
        x.cross(z, x);
        break;
      }
      // align z as p orbital
      z.setT(vTemp);
      if (z.z < 0 && doAlignZ) {
        z.scale(-1);
        x.scale(-1);
      }
    }

    x.normalize();
    z.normalize();

    return hybridization;
  }
  
  /**
   * dsp3 (trigonal bipyramidal, see-saw, T-shaped) 
   * or d2sp3 (square planar, square pyramidal, octahedral)
   *  
   * @param atomIndex  
   * @param z 
   * @param x 
   * @param lcaoType
   * @return valid hybridization or null
   */
  private String getHybridizationAndAxesD(int atomIndex, V3 z, V3 x,
                                         String lcaoType) {
    // note -- d2sp3, not sp3d2; dsp3, not sp3d
    if (lcaoType.startsWith("sp3d2"))
      lcaoType = "d2sp3"
          + (lcaoType.length() == 5 ? "a" : lcaoType.substring(5));
    if (lcaoType.startsWith("sp3d"))
      lcaoType = "dsp3"
          + (lcaoType.length() == 4 ? "a" : lcaoType.substring(4));
    if (lcaoType.equals("d2sp3") || lcaoType.equals("dsp3"))
      lcaoType += "a";
    boolean isTrigonal = lcaoType.startsWith("dsp3");
    int pt = lcaoType.charAt(lcaoType.length() - 1) - 'a';
    if (z != null && (!isTrigonal && (pt > 5 || !lcaoType.startsWith("d2sp3"))
        || isTrigonal && pt > 4))
      return null;
    
    // pt: a 0   b 1   c 2   d 3   e 4   f 5
    
    Atom atom = at[atomIndex];
    Atom[] attached = getAttached(atom, 6, true);
    if (attached == null)
      return (z == null ? null : "?");
    int nAttached = attached.length;
    if (nAttached < 3 && z != null)
      return null;
    boolean isLP = (pt >= nAttached);

    // determine geometry

    int nAngles = nAttached * (nAttached - 1) / 2;
    int[][] angles = AU.newInt2(nAngles);
    
    // all attached angles must be around 180, 120, or 90 degrees
    
    int[] ntypes = new int[3];
    int[][] typePtrs = new int[3][nAngles];
    
    int n = 0;
    int _90 = 0;
    int _120 = 1;
    int _180 = 2;
    int n120_atom0 = 0;
    for (int i = 0; i < nAttached - 1; i++)
      for (int j = i + 1; j < nAttached; j++) {
        float angle = Measure
            .computeAngleABC(attached[i], atom, attached[j], true);
        // cutoffs determined empirically and meant to be generous
        int itype = (angle < 105 ? _90 : angle >= 150 ? _180 : _120);
        typePtrs[itype][ntypes[itype]] = n;
        ntypes[itype]++;
        angles[n++] = new int[] { i, j };
        if (i == 0 && itype == _120)
          n120_atom0++;
      }
    // categorization is done simply by listing 
    // the number of 90, 120, and 180 angles.
    n = ntypes[_90] * 100 + ntypes[_120] * 10 + ntypes[_180];
    if (z == null) {
      // just return geometry
      switch (n) {
      default:
        return "";
      case 0:
        return "";// just ignore atoms with only one bond? (atom.getElementNumber() == 1 ? "s" : "");
      case 1:
        return "linear";
      case 100:
      case 10:
        return "bent";
      case 111:
      case 201:
        return "T-shaped";// -- AX3E or AX3E2 or AX3E3
      case 30:
      case 120:
      case 210:
      case 300:
        if (Math.abs(Measure.computeTorsion(attached[0], atom, attached[1], attached[2], true)) > 162)
            return "trigonal planar";// -- AX3
        return "trigonal pyramidal";// -- AX3E
      case 330: 
        // may just have a rather distorted tetrahedron, as in "$phosphorus pentoxide"
        // in that case, each atom will have 1 or 3 120o angles, not 0 or 2, as in trigonal pyramid
        return (n120_atom0 % 2 == 1 ? "tetrahedral" : "uncapped trigonal pyramid");// -- AX4 or AX4E
      case 60:
      case 150:
      case 240:
        return "tetrahedral";// -- AX4
      case 402:
        return "square planar";// -- AX4E2
      case 411:
      case 501:
        return "see-saw";// -- AX4E
      case 631:
        return "trigonal bipyramidal";// -- AX5
      case 802:
        return "uncapped square pyramid";// -- AX5E
      case 1203:
        return "octahedral";// -- AX6
      }
    }

    switch (n) {
    default:
      return null;
      // 111 is also possible, but quite odd
    case 201:
      // 201 T-shaped -- could be either
      break;
    case 210:
    case 330: 
    case 411:
    case 631:
      // 210 no name (90-90-120)
      // 411 see-saw
      // 330 trigonal pyramid
      // 631 trigonal bipyramidal 
     if (!isTrigonal)
       return null;
     break;
    case 300:
    case 402:
    case 501:
    case 802:
    case 1203:
     // 300 no name (90-90-90)   
     // 402 square planar
     // 501 no name (see-saw like, but with 90o angle)
     // 802 square pyramidal
     // 1203 octahedral
      if (isTrigonal)
        return null;
     break;
    }
    // if subType a-f is pointing to an attached atom, use it
    // otherwise, we need to find the position
    if (isLP) {
      int[] a;
      BS bs;
      if (isTrigonal) {
        switch (ntypes[_120]) {
        case 0:
          // T-shaped
          z.sub2(attached[angles[typePtrs[_90][0]][0]], atom);
          x.sub2(attached[angles[typePtrs[_90][0]][1]], atom);
          z.cross(z, x);
          z.normalize();
          if (pt == 4)
            z.scale(-1);
          bs = findNotAttached(nAttached, angles, typePtrs[_180], ntypes[_180]);
          int i = bs.nextSetBit(0);
          x.sub2(attached[i], atom);
          x.normalize();
          x.scale(0.5f);
          z.scaleAdd2(sqrt3_2, z, x);
          pt = -1;
          break;
        case 1:
          // see-saw
          if (pt == 4) {
            a = angles[typePtrs[_120][0]];
            z.add2(attached[a[0]], attached[a[1]]);
            z.scaleAdd2(-2, atom, z);
            pt = -1;
          } else {
            bs = findNotAttached(nAttached, angles, typePtrs[_120], ntypes[_120]);
            pt = bs.nextSetBit(0);            
          }
          break;
        default:
          // unobserved nor-apical trigonal bipyramid
          // or highly distorted trigonal pyramid (PH3)
          bs = findNotAttached(nAttached, angles, typePtrs[_120], ntypes[_120]);
          pt = bs.nextSetBit(0);
        }
      } else {
        boolean isPlanar = false;
        if (nAttached == 4) {
          switch (ntypes[_180]) {
          case 1:
            // unobserved cis-nor-octahedron
            bs = findNotAttached(nAttached, angles, typePtrs[_180],
                ntypes[_180]);
            int i = bs.nextSetBit(0);
            if (pt == 4)
              pt = i;
            else
              pt = bs.nextSetBit(i + 1);
            break;
          default:
            // square planar
            isPlanar = true;
          }
        } else {
          // square pyramidal
          bs = findNotAttached(nAttached, angles, typePtrs[_180], ntypes[_180]);
          int i = bs.nextSetBit(0);
          for (int j = nAttached; j < pt && i >= 0; j++)
            i = bs.nextSetBit(i + 1);
          if (i == -1)
            isPlanar = true;
          else
            pt = i;
        }
        if (isPlanar) {
          // square planar or T-shaped
          z.sub2(attached[angles[typePtrs[_90][0]][0]], atom);
          x.sub2(attached[angles[typePtrs[_90][0]][1]], atom);
          z.cross(z, x);
          if (pt == 4)
            z.scale(-1);
          pt = -1;
        }
      }
    }
    if (pt >= 0)
      z.sub2(attached[pt], atom);
    if (isLP)
      z.scale(-1);
    z.normalize();
    return (isTrigonal ? "dsp3" : "d2sp3");
  }

  private Atom[] getAttached(Atom atom, int nMax, boolean doSort) {
    int nAttached = atom.getCovalentBondCount();
    if (nAttached > nMax)
      return null;
    Atom[] attached = new Atom[nAttached];
    if (nAttached > 0) {
      Bond[] bonds = atom.bonds;
      int n = 0;
      for (int i = 0; i < bonds.length; i++)
        if (bonds[i].isCovalent())
          attached[n++] = bonds[i].getOtherAtom(atom);
      if (doSort)
        Arrays.sort(attached, new AtomSorter());
    }
    return attached;
  }

  private BS findNotAttached(int nAttached, int[][] angles, int[] ptrs, int nPtrs) {
    BS bs = BS.newN(nAttached);
    bs.setBits(0, nAttached);
    for (int i = 0; i < nAttached; i++)
      for (int j = 0; j < nPtrs; j++) {
        int[] a = angles[ptrs[j]];
        if (a[0] == i || a[1] == i)
          bs.clear(i);
      }
    return bs;
  }

  protected class AtomSorter implements Comparator<Atom>{
    @Override
    public int compare(Atom a1, Atom a2) {
      return (a1.i > a2.i ? 1 : a1.i < a2.i ? -1 : 0);
    }    
  }
  
  /*
   * ******************************************************
   * 
   * These next methods are used by Eval to select for specific atom sets. They
   * all return a BitSet
   * 
   * ******************************************************
   */

  /**
   * general unqualified lookup of atom set type
   * 
   * @param tokType
   * @param specInfo
   * @param bs
   *        - to be filled
   * @return BitSet; or null if we mess up the type
   */
  public BS getAtomBitsMDa(int tokType, Object specInfo, BS bs) {
    int iSpec = (specInfo instanceof Integer ? ((Integer) specInfo).intValue()
        : 0);

    switch (tokType) {
    case T.atomname:
    case T.atomtype:
      // within(atomname
      // within(atomtype
      boolean isType = (tokType == T.atomtype);
      String names = "," + specInfo + ",";
      for (int i = ac; --i >= 0;) {
        String s = (isType ? at[i].getAtomType() : at[i].getAtomName());
        if (names.indexOf("," + s + ",") >= 0)
          bs.set(i);
      }
      return bs;
    case T.atomno:
      for (int i = ac; --i >= 0;)
        if (at[i].getAtomNumber() == iSpec)
          bs.set(i);
      return bs;
    case T.bonded:
      for (int i = ac; --i >= 0;)
        if (at[i].getCovalentBondCount() > 0)
          bs.set(i);
      return bs;
    case T.carbohydrate:
    case T.dna:
    case T.helix: // WITHIN -- not ends
    case T.nucleic:
    case T.protein:
    case T.purine:
    case T.pyrimidine:
    case T.rna:
    case T.sheet: // WITHIN -- not ends
      return (((ModelSet)this).haveBioModels ? ((ModelSet)this).bioModelset.getAtomBitsBS(tokType, null, bs) : bs);
    case T.hydrogen:
      iSpec = 1;
      //$FALL-THROUGH$
    case T.elemno:
      for (int i = ac; --i >= 0;)
        if (at[i].getElementNumber() == iSpec)
          bs.set(i);
      return bs;
    case T.hetero:
      for (int i = ac; --i >= 0;)
        if (at[i].isHetero())
          bs.set(i);
      return bs;
    case T.identifier:
      return getIdentifierOrNull((String) specInfo);
    case T.leadatom:
      for (int i = ac; --i >= 0;)
        if (at[i].isLeadAtom())
          bs.set(i);
      return bs;
    case T.polymer:
    case T.structure:
      return (((ModelSet)this).haveBioModels ? ((ModelSet)this).bioModelset.getAtomBitsBS(tokType, (BS) specInfo, bs)
          : bs);
    case T.resno:
      // could be PDB type file with UNK
      for (int i = ac; --i >= 0;)
        if (at[i].getResno() == iSpec)
          bs.set(i);
      return bs;
    case T.solvent:
      // fast search for water

      // this is faster by a factor of 2  Jmol 14.1.12  -BH

      //"@water _g>=" + GROUPID_WATER + " & _g<" + GROUPID_SOLVENT_MIN
      //+ ", oxygen & connected(2) & connected(2, hydrogen),  
      //(hydrogen) & connected(oxygen & connected(2) & connected(2, hydrogen))",

      int[] hs = new int[2];
      Atom a;
      for (int i = ac; --i >= 0;) {
        int g = at[i].group.groupID;
        if (g >= JC.GROUPID_WATER && g < JC.GROUPID_SOLVENT_MIN) {
          bs.set(i);
        } else if ((a = at[i]).getElementNumber() == 8
            && a.getCovalentBondCount() == 2) {
          Bond[] bonds = a.bonds;
          int n = 0;
          Atom b;
          for (int j = bonds.length; --j >= 0 && n < 3;)
            if (bonds[j].isCovalent()
                && (b = bonds[j].getOtherAtom(a)).getElementNumber() == 1)
              hs[n++ % 2] = b.i;
          if (n == 2) {
            bs.set(hs[1]);
            bs.set(hs[0]);
            bs.set(i);
          }
        }
      }
      return bs;
    case T.spec_alternate:
      // could be PDB type file with UNK or a modulated CIF file subsystem
      String spec = (String) specInfo;
      for (int i = ac; --i >= 0;)
        if (isAltLoc(at[i].altloc, spec))
          bs.set(i);
      return bs;
    case T.spec_atom:
      String atomSpec = ((String) specInfo).toUpperCase();
      if (atomSpec.indexOf("\\?") >= 0)
        atomSpec = PT.rep(atomSpec, "\\?", "\1");
      // / here xx*yy is NOT changed to "xx??????????yy"
      boolean allowStar = atomSpec.startsWith("?*");
      if (allowStar)
        atomSpec = atomSpec.substring(1);
      for (int i = ac; --i >= 0;)
        if (isAtomNameMatch(at[i], atomSpec, allowStar, allowStar))
          bs.set(i);
      return bs;
    case T.spec_chain:
      return BSUtil.copy(getChainBits(iSpec));
    case T.spec_name_pattern:
      return getSpecName((String) specInfo);
    case T.spec_resid:
      // could be PDB type file with UNK
      for (int i = ac; --i >= 0;)
        if (at[i].group.groupID == iSpec)
          bs.set(i);
      return bs;
    case T.spec_seqcode:
      return BSUtil.copy(getSeqcodeBits(iSpec, true));
    case T.inscode:
      for (int i = ac; --i >= 0;)
        if (at[i].group.getInsCode() == iSpec)
          bs.set(i);
      return bs;
    case T.symop:
      for (int i = ac; --i >= 0;)
        if (at[i].getSymOp() == iSpec)
          bs.set(i);
      return bs;
    }

    BS bsTemp;
    BS bsInfo = (BS) specInfo;
    int i0 = bsInfo.nextSetBit(0);
    if (i0 < 0)
      return bs;
    switch (tokType) {
    case T.model:
      // within(model
      bsTemp = BSUtil.copy(bsInfo);
      for (int i = i0; i >= 0; i = bsTemp.nextSetBit(i + 1)) {
        bs.or(((ModelSet) this).am[at[i].mi].bsAtoms);
        bsTemp.andNot(bs);
      }
      return bs;
    case T.chain:
      // within(chain
      bsTemp = BSUtil.copy(bsInfo);
      for (int i = i0; i >= 0; i = bsTemp.nextSetBit(i + 1)) {
        at[i].group.chain.setAtomBits(bs);
        bsTemp.andNot(bs);
      }
      return bs;
    case T.element:
      // within(element
      bsTemp = new BS();
      for (int i = i0; i >= 0; i = bsInfo.nextSetBit(i + 1))
        bsTemp.set(at[i].getElementNumber());
      for (int i = ac; --i >= 0;)
        if (bsTemp.get(at[i].getElementNumber()))
          bs.set(i);
      return bs;
    case T.group:
      // within(group
      bsTemp = BSUtil.copy(bsInfo);
      for (int i = i0; i >= 0; i = bsTemp.nextSetBit(i + 1)) {
        at[i].group.setAtomBits(bs);
        bsTemp.andNot(bs);
      }
      return bs;
    case T.site:
      // within(site
      bsTemp = new BS();
      for (int i = i0; i >= 0; i = bsInfo.nextSetBit(i + 1))
        bsTemp.set(at[i].atomSite);
      for (int i = ac; --i >= 0;)
        if (bsTemp.get(at[i].atomSite))
          bs.set(i);
      return bs;
    }
    Logger.error("MISSING getAtomBits entry for " + T.nameOf(tokType));
    return bs;
  }
  
  public BS getChainBits(int chainID) {
    boolean caseSensitive = vwr.getBoolean(T.chaincasesensitive);
    if (chainID >= 0 && chainID < 300 && !caseSensitive)
      chainID = chainToUpper(chainID);
    BS bs = new BS();
    BS bsDone = BS.newN(ac);
    int id;
    for (int i = bsDone.nextClearBit(0); i < ac; i = bsDone.nextClearBit(i + 1)) {
      Chain chain = at[i].group.chain;
      if (chainID == (id = chain.chainID) || !caseSensitive 
          && id >= 0 && id < 300 && chainID == chainToUpper(id)) {
        chain.setAtomBits(bs);
        bsDone.or(bs);
      } else {
        chain.setAtomBits(bsDone);
      }
    }
    return bs;
  }

  public int chainToUpper(int chainID) {
    return (chainID >= 97 && chainID <= 122 ? chainID - 32 
        : chainID >= 256 && chainID < 300 ? chainID - 191
          // must be single-character lower-case 256=="a" 
          // to lower case is to 65, so subtract (256-65) 
        : chainID);
  }

  private boolean isAltLoc(char altloc, String strPattern) {
    if (strPattern == null)
      return (altloc == '\0');
    if (strPattern.length() != 1)
      return false;
    char ch = strPattern.charAt(0);
    return (ch == '*' || ch == '?' && altloc != '\0' || altloc == ch);
  }

  public BS getSeqcodeBits(int seqcode, boolean returnEmpty) {
    BS bs = new BS();
    int seqNum = Group.getSeqNumberFor(seqcode);
    boolean haveSeqNumber = (seqNum != Integer.MAX_VALUE);
    boolean isEmpty = true;
    char insCode = Group.getInsertionCodeChar(seqcode);
    switch (insCode) {
    case '?':
      for (int i = ac; --i >= 0;) {
        int atomSeqcode = at[i].group.seqcode;
        if ((!haveSeqNumber 
            || seqNum == Group.getSeqNumberFor(atomSeqcode))
            && Group.getInsertionCodeFor(atomSeqcode) != 0) {
          bs.set(i);
          isEmpty = false;
        }
      }
      break;
    default:
      for (int i = ac; --i >= 0;) {
        int atomSeqcode = at[i].group.seqcode;
        if (seqcode == atomSeqcode || 
            !haveSeqNumber && seqcode == Group.getInsertionCodeFor(atomSeqcode) 
            || insCode == '*' && seqNum == Group.getSeqNumberFor(atomSeqcode)) {
          bs.set(i);
          isEmpty = false;
        }
      }
    }
    return (!isEmpty || returnEmpty ? bs : null);
  }
  

  /**
   * overhauled by RMH Nov 1, 2006.
   * 
   * @param identifier
   * @return null or bs
   */
  private BS getIdentifierOrNull(String identifier) {
    //a primitive lookup scheme when [ ] are not used
    //nam
    //na?
    //nam45
    //nam45C
    //nam45^
    //nam45^A
    //nam45^AC -- note, no colon here -- if present, handled separately
    //nam4? does NOT match anything for PDB files, but might for others
    //atom specifiers:
    //H?
    //H32
    //H3?

    //in the case of a ?, we take the whole thing
    // * can be used here, but not with ?
    // Jmol 14.1.14: allows initial ?* to be same as *
    // and therefore can be used in atom expression
    
    //first check with * option OFF
    BS bs = getSpecNameOrNull(identifier, false);
    
    if (identifier.indexOf("\\?") >= 0)
      identifier = PT.rep(identifier, "\\?","\1");
    return (bs != null || identifier.indexOf("?") > 0 ? bs
        : identifier.indexOf("*") > 0 ? getSpecNameOrNull(identifier, true)
        : ((ModelSet)this).haveBioModels ? ((ModelSet)this).bioModelset.getIdentifierOrNull(identifier) 
        : null);
  }

  private BS getSpecName(String name) {
    // * can be used here with ?
    BS bs = getSpecNameOrNull(name, false);
    if (bs != null)
      return bs;
    if (name.indexOf("*") > 0)     
      bs = getSpecNameOrNull(name, true);
    return (bs == null ? new BS() : bs);
  }

  public BS getSpecNameOrNull(String name, boolean checkStar) {
    /// here xx*yy is changed to "xx??????????yy" when coming from getSpecName
    /// but not necessarily when coming from getIdentifierOrNull
    BS bs = null;
    name = name.toUpperCase();
    if (name.indexOf("\\?") >= 0)
      name = PT.rep(name, "\\?","\1");
    boolean allowInitialStar = name.startsWith("?*");
      if (allowInitialStar)
        name = name.substring(1);

    for (int i = ac; --i >= 0;) {
      String g3 = at[i].getGroup3(true);
      if (g3 != null && g3.length() > 0) {
        if (PT.isMatch(g3, name, checkStar, true)) {
          if (bs == null)
            bs = BS.newN(i + 1);
          bs.set(i);
          while (--i >= 0 && at[i].getGroup3(true).equals(g3))
            bs.set(i);
          i++;
        }
      } else if (isAtomNameMatch(at[i], name, checkStar, allowInitialStar)) {
        if (bs == null)
          bs = BS.newN(i + 1);
        bs.set(i);
      }
    }
    return bs;
  }

  private boolean isAtomNameMatch(Atom atom, String strPattern, boolean checkStar, boolean allowInitialStar) {
    // called from getAtomBitsMa (spec_atom) and getSpecNameOrNull
    /// here xx*yy is changed to "xx??????????yy" when coming from getSpecName
    /// but not necessarily when coming from getIdentifierOrNull
    /// and NOT when coming from getAtomBits with Token.spec_atom
    /// because it is presumed that some names can include "*"
    return PT.isMatch(atom.getAtomName().toUpperCase(), strPattern,
        checkStar, allowInitialStar);
  }
  
  public int[] getAtomIndices(BS bs) {
    int n = 0;
    int[] indices = new int[ac];
    for (int j = bs.nextSetBit(0); j >= 0 && j < ac; j = bs.nextSetBit(j + 1))
      indices[j] = ++n;
    return indices;
  }

  public BS getAtomsNearPlane(float distance, P4 plane) {
    BS bsResult = new BS();
    for (int i = ac; --i >= 0;) {
      Atom atom = at[i];
      float d = Measure.distanceToPlane(plane, atom);
      if (distance > 0 && d >= -0.1 && d <= distance || distance < 0
          && d <= 0.1 && d >= distance || distance == 0 && Math.abs(d) < 0.01)
        bsResult.set(atom.i);
    }
    return bsResult;
  }
  
  public void clearVisibleSets() {
    haveBSVisible = false;
    haveBSClickable = false;
  }

  public void getAtomsInFrame(BS bsAtoms) {
    clearVisibleSets();
    bsAtoms.clearAll();
    for (int i = ac; --i >= 0;)
      if (at[i].isVisible(Atom.ATOM_INFRAME))
        bsAtoms.set(i);
  }

  public BS getVisibleSet(boolean forceNew) {
    if (forceNew) {
      vwr.setModelVisibility();
      vwr.shm.finalizeAtoms(null, true);
    } else if (haveBSVisible) {
      return bsVisible;
    }
    bsVisible.clearAll();
    for (int i = ac; --i >= 0;)
      if (at[i].checkVisible())
        bsVisible.set(i);
    if (vwr.shm.bsSlabbedInternal != null)
      bsVisible.andNot(vwr.shm.bsSlabbedInternal);
    haveBSVisible = true;
    return bsVisible;
  }

  public BS getClickableSet(boolean forceNew) {
    if (forceNew)
      vwr.setModelVisibility();
    else if (haveBSClickable)
      return bsClickable;
    bsClickable.clearAll();
    for (int i = ac; --i >= 0;)
      if (at[i].isClickable())
        bsClickable.set(i);
    haveBSClickable = true;
    return bsClickable;
  }

  public boolean isModulated(int i) {
    return bsModulated != null && bsModulated.get(i);
  }

  protected void deleteModelAtoms(int firstAtomIndex, int nAtoms, BS bsAtoms) {
    // all atoms in the model are being deleted here
    at = (Atom[]) AU.deleteElements(at, firstAtomIndex, nAtoms);
    ac = at.length;
    for (int j = firstAtomIndex; j < ac; j++) {
      at[j].i = j;
      at[j].mi--;
    }
    // fix modulation and tensors    
    if (bsModulated != null)
      BSUtil.deleteBits(bsModulated, bsAtoms);

    deleteAtomTensors(bsAtoms);
    atomNames = (String[]) AU.deleteElements(atomNames, firstAtomIndex,
        nAtoms);
    atomTypes = (String[]) AU.deleteElements(atomTypes, firstAtomIndex,
        nAtoms);
    atomResnos = (int[]) AU.deleteElements(atomResnos, firstAtomIndex,
        nAtoms);
    atomSerials = (int[]) AU.deleteElements(atomSerials, firstAtomIndex,
        nAtoms);
    atomSeqIDs = (int[]) AU.deleteElements(atomSeqIDs, firstAtomIndex,
        nAtoms);
    dssrData = (float[]) AU.deleteElements(dssrData, firstAtomIndex,
        nAtoms);
    bfactor100s = (short[]) AU.deleteElements(bfactor100s,
        firstAtomIndex, nAtoms);
    hasBfactorRange = false;
    occupancies = (float[]) AU.deleteElements(occupancies,
        firstAtomIndex, nAtoms);
    resetPartialCharges();
    atomTensorList = (Object[][]) AU.deleteElements(atomTensorList,
        firstAtomIndex, nAtoms);
    vibrations = (Vibration[]) AU.deleteElements(vibrations,
        firstAtomIndex, nAtoms);
    nSurfaceAtoms = 0;
    bsSurface = null;
    surfaceDistance100s = null;
    if (tainted != null)
      for (int i = 0; i < TAINT_MAX; i++)
        BSUtil.deleteBits(tainted[i], bsAtoms);
    // what about data?
  }

  public void getAtomIdentityInfo(int i, Map<String, Object> info, P3 ptTemp) {
    info.put("_ipt", Integer.valueOf(i));
    info.put("atomIndex", Integer.valueOf(i));
    info.put("atomno", Integer.valueOf(at[i].getAtomNumber()));
    info.put("info", getAtomInfo(i, null, ptTemp));
    info.put("sym", at[i].getElementSymbol());
  }

  public Object[] getAtomTensorList(int i) {
    return (i < 0 || atomTensorList == null || i >= atomTensorList.length ? null
        : atomTensorList[i]);
  }
  
  // clean out deleted model atom tensors (ellipsoids)
  private void deleteAtomTensors(BS bsAtoms) {
    if (atomTensors == null)
      return;
    Lst<String> toDelete = new Lst<String>();
    for (String key: atomTensors.keySet()) {
      Lst<Object> list = atomTensors.get(key);
      for (int i = list.size(); --i >= 0;) {
        Tensor t = (Tensor) list.get(i);
        if (bsAtoms.get(t.atomIndex1) || t.atomIndex2 >= 0 && bsAtoms.get(t.atomIndex2))
          list.removeItemAt(i);
      }
      if (list.size() == 0)
        toDelete.addLast(key);
    }
    for (int i = toDelete.size(); --i >= 0;)
      atomTensors.remove(toDelete.get(i));
  }

  
  private int atomCapacity;
  void setCapacity(int nAtoms) {
    atomCapacity += nAtoms;
  }

  public void setAtomTensors(int atomIndex, Lst<Object> list) {
    if (list == null || list.size() == 0)
      return;
    if (atomTensors == null)
      atomTensors = new Hashtable<String, Lst<Object>>();
    if (atomTensorList == null)
      atomTensorList = new Object[at.length][];
    atomTensorList = (Object[][]) AU.ensureLength(atomTensorList, at.length);
    atomTensorList[atomIndex] = getTensorList(list);
    for (int i = list.size(); --i >= 0;) {
      Tensor t = (Tensor) list.get(i);
      t.atomIndex1 = atomIndex;
      t.atomIndex2 = -1;
      t.modelIndex = at[atomIndex].mi;
      addTensor(t, t.type);
      if (t.altType != null)
        addTensor(t, t.altType);
    }
  }

  public void addTensor(Tensor t, String type) {
    type = type.toLowerCase();
    Lst<Object> tensors = atomTensors.get(type);
    if (tensors == null) {
      atomTensors.put(type, tensors = new Lst<Object>());
      tensors.ensureCapacity(atomCapacity);
    }
    tensors.addLast(t);
  }

  private static Object[] getTensorList(Lst<Object> list) {
    int pt = -1;
    boolean haveTLS = false;
    int n = list.size();
    for (int i = n; --i >= 0;) {
      Tensor t = (Tensor) list.get(i);
      if (t.forThermalEllipsoid)
        pt = i;
      else if (t.iType == Tensor.TYPE_TLS_U)
        haveTLS = true;
    }
    Object[] a = new Object[(pt >= 0 || !haveTLS ? 0 : 1) + n];
    if (pt >= 0) {
      a[0] = list.get(pt);
      if (list.size() == 1)
        return a;
    }
    // back-fills list for TLS:
    // 0 = temp, 1 = TLS-R, 2 = TLS-U
    if (haveTLS) {
      pt = 0;
      for (int i = n; --i >= 0;) {
        Tensor t = (Tensor) list.get(i);
        if (t.forThermalEllipsoid)
          continue;
        a[++pt] = t;
      }
    } else {
      for (int i = 0; i < n; i++)
        a[i] = list.get(i);
    }
    return a;
 }

  public Tensor getAtomTensor(int i, String type) {
    Object[] tensors = getAtomTensorList(i);
    if (tensors != null && type != null) {
      type = type.toLowerCase();
      for (int j = 0; j < tensors.length; j++) {
        Tensor t = (Tensor) tensors[j];
        if (t != null && (type.equals(t.type) || type.equals(t.altType)))
          return t;
      }
    }
    return null;
  }

  public Lst<Object> getAllAtomTensors(String type) {
    if (atomTensors == null)
      return null;
    if (type != null)
      return atomTensors.get(type.toLowerCase());
    Lst<Object> list = new Lst<Object>();
    for (Entry<String, Lst<Object>> e : atomTensors.entrySet())
      list.addAll(e.getValue());
    return list;
  }

  /**
   * Scales vibrations and associated vectors such that the
   * maximum length is the given value
   * 
   * @param max
   */
  public void scaleVectorsToMax(float max) {
    if (vibrations == null)
      return;
    float m = 0;
    BS bsVib = BS.newN(ac);
    for (int i = vibrations.length; --i >= 0;) {
      Vibration v = getVibration(i, false);
      if (v != null && (v.modDim == Vibration.TYPE_VIBRATION
          || v.modDim == Vibration.TYPE_SPIN)) {
        m = Math.max(m, v.length());
        bsVib.set(i);
      }
    }
    if (m == max || m == 0)
      return;
    m = max / m;
    boolean ok = false;
    for (int i = bsVib.nextSetBit(0); i >= 0; i = bsVib.nextSetBit(i + 1)) {
      Vibration v = getVibration(i, false);
      JmolModulationSet mod = getModulation(i);
      if (mod == null) {
        if (m == 0)
          return; // get out of here!
        v.scale(m);
      } else {
        mod.scaleVibration(m);
      }
      if (!ok) {
        taintAtom(i, TAINT_VIBRATION);
        ok = true;
      }
    }
    tainted[TAINT_VIBRATION].or(bsVib);
    //{*}.vxyz = {*}.vxyz.all.mul(3.0/{*}.vxyz.all.max)
  }

  public BS getAtomsFromAtomNumberInFrame(int atomNumber) {
    BS bs = vwr.getFrameAtoms();
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
      if (at[i].getAtomNumber() != atomNumber)
        bs.clear(i);
    return bs;
  }


  public Lst<P3> generateCrystalClass(int atomIndex, P3 pt) {
    SymmetryInterface sym = (atomIndex < 0 || atomIndex >= ac ? null
        : at[atomIndex].getUnitCell());
    boolean isRandom = (pt != null && Float.isNaN(pt.x));
    return (sym == null ? new Lst<P3>() : sym
        .generateCrystalClass(isRandom ? null : pt != null ? pt : at[atomIndex]));

  }


}

