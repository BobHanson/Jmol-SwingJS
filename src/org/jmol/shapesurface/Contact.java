/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-07-14 16:23:28 -0500 (Fri, 14 Jul 2006) $
 * $Revision: 5305 $
 *
 * Copyright (C) 2005 Miguel, Jmol Development
 *
 * Contact: jmol-developers@lists.sf.net,jmol-developers@lists.sourceforge.net
 * Contact: hansonr@stolaf.edu
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

package org.jmol.shapesurface;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.CU;
import javajs.util.Lst;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.V3;

import org.jmol.api.AtomIndexIterator;
import org.jmol.atomdata.AtomData;
import org.jmol.atomdata.RadiusData;
import org.jmol.atomdata.RadiusData.EnumType;
import org.jmol.c.HB;
import org.jmol.c.VDW;
import javajs.util.BS;
import org.jmol.jvxl.data.JvxlCoder;
import org.jmol.jvxl.data.MeshData;
import org.jmol.jvxl.data.VolumeData;
import org.jmol.jvxl.readers.Parameters;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.BoxInfo;
import org.jmol.util.ColorEncoder;
import org.jmol.util.ContactPair;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.TempArray;

public class Contact extends Isosurface {

  
  @Override
  public void initShape() {
    super.initShape();
    myType = "contact";
  }

  @Override
  public Object getProperty(String property, int index) {
    return getPropC(property, index);
  }

  protected int displayType;

  protected Object getPropC(String property, int index) {
    IsosurfaceMesh thisMesh = this.thisMesh;
    if (index >= 0 && (index >= meshCount || (thisMesh = isomeshes[index]) == null))
      return null;
    if (property == "jvxlFileInfo") {
      thisMesh.setJvxlColorMap(false);
      if (displayType == T.plane) {
        JvxlCoder.jvxlCreateColorData(jvxlData, thisMesh.vvs);
        float[] minmax = thisMesh.getDataMinMax();
        jvxlData.mappedDataMin = minmax[0];
        jvxlData.mappedDataMax = minmax[1];
      }
      return JvxlCoder.jvxlGetInfo(jvxlData);
    }
    return getPropI(property, index);
  }
  
  @Override
  public void setProperty(String propertyName, Object value, BS bs) {

    if ("set" == propertyName) {
      setContacts((Object[]) value, true);//!vwr.getBoolean(T.testflag4));
      return;
    }
    if ("init" == propertyName) {
      translucentLevel = 0;
    }

    setPropI(propertyName, value, bs);
  }
    
  protected Atom[] atoms;
  private int ac;
  private float minData, maxData;

  //private final static String hbondH = "_H & connected(_O|_N and his and not *.N |_S)";
  //private final static float HBOND_CUTOFF = -0.8f;
  private final static RadiusData rdVDW =  new RadiusData(null, 1, EnumType.FACTOR, VDW.AUTO);
  
  private void setContacts(Object[] value, boolean doEditCpList) {
    int contactType = ((Integer) value[0]).intValue();
    int displayType = ((Integer) value[1]).intValue();
    boolean colorDensity = ((Boolean) value[2]).booleanValue();
    boolean colorByType = ((Boolean) value[3]).booleanValue();
    BS bsA = (BS) value[4];
    BS bsB = (BS) value[5];
    RadiusData rd = (RadiusData) value[6];
    float saProbeRadius = ((Float) value[7]).floatValue();
    float[] parameters = (float[]) value[8];
    int modelIndex = ((Integer) value[9]).intValue();
    String command = (String) value[10];
    if (Float.isNaN(saProbeRadius))
      saProbeRadius = 0;
    if (rd == null)
      rd = new RadiusData(null, saProbeRadius, EnumType.OFFSET, VDW.AUTO);
    if (colorDensity) {
      switch (displayType) {
      case T.full:
      case T.trim:
      case T.plane:
        displayType = T.trim;
        break;
      case T.connect:
      case T.nci:
      case T.surface:
      case T.sasurface:
        // ok as is
        break;
      case T.cap:
        colorDensity = false;
        break;
      }
    }

    BS bs;
    ac = ms.ac;
    atoms = ms.at;

    int intramolecularMode = (int) (parameters == null || parameters.length < 2 ? 0
        : parameters[1]);
    float ptSize = (colorDensity && parameters != null && parameters[0] < 0 ? Math
        .abs(parameters[0])
        : 0.15f);
    if (Logger.debugging) {
      Logger.debug("Contact intramolecularMode " + intramolecularMode);
      Logger.debug("Contacts for " + bsA.cardinality() + ": "
          + Escape.eBS(bsA));
      Logger.debug("Contacts to " + bsB.cardinality() + ": "
          + Escape.eBS(bsB));
    }
    setPropI("newObject", null, null);
    thisMesh.setMerged(true);
    thisMesh.nSets = 0;
    thisMesh.info = null;
    String func = null;
    boolean fullyLit = true;
    switch (displayType) {
    case T.full:
      func = "(a>b?a:b)";
      break;
    case T.plane:
    case T.cap:
      func = "a-b";
      break;
    case T.connect:
      func = "a+b";
      break;
    }
    //VolumeData volumeData;
    switch (displayType) {
    case T.nci:
      colorByType = fullyLit = false;
      bs = BSUtil.copy(bsA);
      bs.or(bsB); // for now -- TODO -- need to distinguish ligand
      if (parameters[0] < 0)
        parameters[0] = 0; // reset to default for density
      sg.params.colorDensity = colorDensity;
      sg.params.bsSelected = bs;
      sg.params.bsSolvent = bsB;
      sg.setProp("parameters", parameters, null);
      setPropI("nci", Boolean.TRUE, null);
      break;
    case T.sasurface:
    case T.surface:
      colorByType = fullyLit = false;
      thisMesh.nSets = 1;
      newSurface(T.surface, null, bsA, bsB, rd, null, null, colorDensity,
          null, saProbeRadius);
      break;
    case T.cap:
      colorByType = fullyLit = false;
      thisMesh.nSets = 1;
      newSurface(T.slab, null, bsA, bsB, rd, null, null, false, null, 0);
      sg.initState();
      newSurface(T.plane, null, bsA, bsB, rd, parameters, func,
          colorDensity, sg.volumeDataTemp, 0);
      mergeMesh(null);
      break;
    case T.full:
    case T.trim:
      colorByType = false;
      newSurface(T.trim, null, bsA, bsB, rd, null, null, colorDensity, null, 0);
      if (displayType == T.full) {
        sg.initState();
        newSurface(T.trim, null, bsB, bsA, rd, parameters, func,
            colorDensity, null, 0);
        mergeMesh(null);
      } else {
        MeshData meshData = new MeshData();
        fillMeshData(meshData, MeshData.MODE_GET_VERTICES, null);
        meshData.getSurfaceSet();
        fillMeshData(meshData, MeshData.MODE_PUT_SETS, null);
      }

      break;
    case T.connect:
    case T.plane:
      /*      if (rd == null)
              rd = new RadiusData(0.25f, EnumType.OFFSET,
                  EnumVdw.AUTO);
      */
      float volume = 0;
      Lst<ContactPair> pairs = getPairs(bsA, bsB, rd, intramolecularMode, doEditCpList);
      thisMesh.info = pairs;
      volume += combineSurfaces(pairs, contactType, displayType, parameters,
          func, colorDensity, colorByType);
      thisMesh.calculatedVolume = Float.valueOf(volume);
      mergeMesh(null);
      break;
    }
    thisMesh.setMerged(false);
    if (modelIndex != Integer.MIN_VALUE)
      thisMesh.modelIndex = modelIndex;
    thisMesh.jvxlData.vertexDataOnly = true;
    thisMesh.reinitializeLightingAndColor(vwr);
    if (contactType != T.nci) {
      thisMesh.bsVdw = new BS();
      thisMesh.bsVdw.or(bsA);
      thisMesh.bsVdw.or(bsB);
    }
    setPropI("finalize", command, null);
    if (colorDensity) {
      setPropI("pointSize", Float.valueOf(ptSize), null);
    } else {
      setPropI("token", Integer.valueOf(fullyLit ? T.fullylit : T.frontlit), null);
    }
    if (thisMesh.slabOptions != null) {
      thisMesh.slabOptions = null;
      thisMesh.polygonCount0 = -1; // disable slabbing.
    }
    discardTempData(true);
    String defaultColor = null;
    switch (contactType) {
    case T.hbond:
      defaultColor = "lightgreen";
      break;
    case T.clash:
      defaultColor = "yellow";
      break;
    case T.surface:
      defaultColor = "skyblue";
      break;
    }
    ColorEncoder ce = null;
    if (colorByType) {
      ce = vwr.cm.getColorEncoder("rwb");
      ce.setRange(-0.5f, 0.5f, false);
    } else if (defaultColor != null) {
      setPropI("color", Integer.valueOf(CU
          .getArgbFromString(defaultColor)), null);
    } else if (displayType == T.nci) {
      ce = vwr.cm.getColorEncoder("bgr");
      ce.setRange(-0.03f, 0.03f, false);
    } else {
      ce = vwr.cm.getColorEncoder("rgb");
      if (colorDensity)
        ce.setRange(-0.3f, 0.3f, false);
      else
        ce.setRange(-0.5f, 1f, false);
    }
    if (ce != null)
      thisMesh.remapColors(vwr, ce, translucentLevel);
  }

  /**
   * @param pairs 
   * @param contactType 
   * @param displayType 
   * @param parameters 
   * @param func 
   * @param isColorDensity 
   * @param colorByType  
   * @return               volume
   */
  private float combineSurfaces(Lst<ContactPair> pairs, int contactType,
                                int displayType, float[] parameters,
                                Object func, boolean isColorDensity,
                                boolean colorByType) {
    VolumeData volumeData = new VolumeData();
    int logLevel = Logger.getLogLevel();
    Logger.setLogLevel(0);
    float resolution = sg.params.resolution;
    int nContacts = pairs.size();
    double volume = 0;
    if (displayType == T.full && resolution == Float.MAX_VALUE)
      resolution = (nContacts > 1000 ? 3 : 10);
    BoxInfo box = new BoxInfo();
    for (int i = nContacts; --i >= 0;) {
      ContactPair cp = pairs.get(i);
      float oldScore = cp.score;
      boolean isVdwClash = (displayType == T.plane 
          && (contactType == T.vanderwaals || contactType == T.nada) 
          && cp.setForVdwClash(true));
      if (isVdwClash)
        cp.score = 0; // for now
      if (contactType != T.nada && cp.contactType != contactType)
        continue;
      int nV = thisMesh.vc;
      thisMesh.nSets++;
      if (contactType != T.nada || cp.contactType != T.vanderwaals)
        volume += cp.volume;
      setVolumeData(displayType, volumeData, cp, resolution, nContacts);
      switch (displayType) {
      case T.full:
        newSurface(displayType, cp, null, null, null, null, func,
            isColorDensity, volumeData, 0);
        cp.switchAtoms();
        newSurface(displayType, cp, null, null, null, null, null,
            isColorDensity, volumeData, 0);
        break;
      case T.trim:
      case T.plane:
      case T.connect:
        newSurface(displayType, cp, null, null, null, parameters, func,
            isColorDensity, volumeData, 0);
        if (isVdwClash && cp.setForVdwClash(false)) {
          if (colorByType)
            nV = setColorByScore(cp.score, nV);
          cp.score = oldScore;
          volume += cp.volume;
          newSurface(displayType, cp, null, null, null, parameters, func,
              isColorDensity, volumeData, 0);          
        }
        break;
      }
      if (i > 0 && (i % 1000) == 0 && logLevel == 4) {
        Logger.setLogLevel(4);
        Logger.info("contact..." + i);
        Logger.setLogLevel(0);
      }
      if (colorByType)
        setColorByScore((cp.contactType == T.hbond ? 4 : cp.score), nV);
      for (int j = thisMesh.vc; --j >= 0;)
        box.addBoundBoxPoint(thisMesh.vs[j]);
    }
    Logger.setLogLevel(logLevel);
    if (jvxlData.boundingBox == null) {
      System.out.println("???");
    } else {
      jvxlData.boundingBox[0] = box.bbCorner0;
      jvxlData.boundingBox[1] = box.bbCorner1;
    }
    this.displayType = displayType;
    return (float) volume;
  }
  
  private int setColorByScore(float score, int nV) {
    for (int iv = thisMesh.vc; --iv >= nV;)
      thisMesh.vvs[iv] = score;
    return thisMesh.vc;
  }

  /**
   * 
   * @param bsA
   * @param bsB
   * @param rd
   * @param intramolecularMode
   * @param doEditCpList 
   * @return a list of pairs of atoms to process
   */
  private Lst<ContactPair> getPairs(BS bsA, BS bsB, RadiusData rd,
                                     int intramolecularMode, boolean doEditCpList) {
    Lst<ContactPair> list = new  Lst<ContactPair>();
    AtomData ad = new AtomData();
    ad.radiusData = rd;
    BS bs = BSUtil.copy(bsA);
    bs.or(bsB);
    if (bs.isEmpty())
      return list;
    ad.bsSelected = bs;
    int iModel = atoms[bs.nextSetBit(0)].mi;
    boolean isMultiModel = (iModel != atoms[bs.length() - 1].mi);
    ad.modelIndex = (isMultiModel ? -1 : iModel);
    //if (!isMultiModel)
      //thisMesh.modelIndex = iModel;
    boolean isSelf = bsA.equals(bsB);
    vwr.fillAtomData(ad, AtomData.MODE_FILL_RADII
        | (isMultiModel ? AtomData.MODE_FILL_MULTIMODEL : AtomData.MODE_FILL_MODEL)
        | AtomData.MODE_FILL_MOLECULES);
    float maxRadius = 0;
    for (int ib = bsB.nextSetBit(0); ib >= 0; ib = bsB.nextSetBit(ib + 1))
      if (ad.atomRadius[ib] > maxRadius)
        maxRadius = ad.atomRadius[ib];
    AtomIndexIterator iter = vwr.getSelectedAtomIterator(bsB, isSelf, false,
        isMultiModel);
    for (int ia = bsA.nextSetBit(0); ia >= 0; ia = bsA.nextSetBit(ia + 1)) {
      Atom atomA = atoms[ia];
      float vdwA = atomA.getVanderwaalsRadiusFloat(vwr, VDW.AUTO);
      if (isMultiModel)
        vwr.setIteratorForPoint(iter, -1, ad.atoms[ia], ad.atomRadius[ia]
            + maxRadius);
      else
        vwr.setIteratorForAtom(iter, ia, ad.atomRadius[ia] + maxRadius);
      while (iter.hasNext()) {
        int ib = iter.next();
        if (isMultiModel && !bsB.get(ib))
          continue;
        Atom atomB = atoms[ib];
        boolean isSameMolecule = (ad.atomMolecule[ia] == ad.atomMolecule[ib]);
        if (ia == ib || isSameMolecule && isWithinFourBonds(atomA, atomB))
          continue;
        switch (intramolecularMode) {
        case 0:
          break;
        case 1:
        case 2:
          if (isSameMolecule != (intramolecularMode == 1))
            continue;
        }
        float vdwB = atomB.getVanderwaalsRadiusFloat(vwr, VDW.AUTO);
        float ra = ad.atomRadius[ia];
        float rb = ad.atomRadius[ib];
        float d = atomA.distance(atomB);
        if (d > ra + rb)
          continue;
        ContactPair cp = new ContactPair(atoms, ia, ib, ra, rb, vdwA, vdwB);

        if (cp.score < 0)
          getVdwClashRadius(cp, ra - vdwA, vdwA, vdwB, d);

        // check for O--H...N or O...H--N and not considering
        // hydrogens and still have a filter
        // a bit of asymmetry here: set A may or may not have H atoms added.
        // This is particularly important for amines
        HB typeA = HB.getType(atomA);
        HB typeB = (typeA == HB.NOT ? HB.NOT
            : HB.getType(atomB));
        boolean isHBond = HB.isPossibleHBond(typeA, typeB);
        //float hbondCutoff = -1.0f;//HBOND_CUTOFF;
        float hbondCutoff = (atomA.getElementNumber() == 1 || atomB.getElementNumber() == 1 ? -1.2f : -1.0f);
        
        if (isHBond && cp.score < hbondCutoff)
          isHBond = false;
        if (isHBond && cp.score < 0)
          cp.contactType = T.hbond;
        list.addLast(cp);
      }
    }
    iter.release();
    iter = null;
    if (!doEditCpList)
      return list;
    int n = list.size() - 1;
    BS bsBad = new BS();
    for (int i = 0; i < n; i++) {
      ContactPair cp1 = list.get(i);
      for (int j = i + 1; j <= n; j++) {
        ContactPair cp2 = list.get(j);
        for (int m = 0; m < 2; m++) {
          for (int p = 0; p < 2; p++) {
            switch (checkCp(cp1, cp2, m, p)) {
            case 1:
              bsBad.set(i);
              break;
            case 2:
              bsBad.set(j);
              break;
            default:
            }
          }
        }
      }
    }
    for (int i = bsBad.length(); --i >= 0;)
      if (bsBad.get(i))
        list.removeItemAt(i);
    if (Logger.debugging)
      for (int i = 0; i < list.size(); i++)
        Logger.debug(list.get(i).toString());
    Logger.info("Contact pairs: " + list.size());
    return list;
  }

  private boolean isWithinFourBonds(Atom atomA, Atom atomB) {
    if (atomA.mi != atomB.mi)
      return false;
    if (atomA.isCovalentlyBonded(atomB))
      return true;
    Bond[] bondsOther = atomB.bonds;
    Bond[] bonds = atomA.bonds;
    if (bondsOther != null && bonds != null)
      for (int i = 0; i < bondsOther.length; i++) {
        Atom atom2 = bondsOther[i].getOtherAtom(atomB);
        if (atomA.isCovalentlyBonded(atom2))
          return true;
        for (int j = 0; j < bonds.length; j++)
          if (bonds[j].getOtherAtom(atomA).isCovalentlyBonded(atom2))
            return true;
      }
    return false;
  }

  /**
   * 
   * @param cp1
   * @param cp2
   * @param i1
   * @param i2
   * @return    0 (no clash); 1 (remove #1); 2 (remove #2)
   */
  private static int checkCp(ContactPair cp1, ContactPair cp2, int i1, int i2) {
    if (cp1.myAtoms[i1] != cp2.myAtoms[i2])
      return 0;
    boolean clash1 = (cp1.pt.distance(cp2.myAtoms[1 - i2]) < cp2.radii[1 - i2]);
    boolean clash2 = (cp2.pt.distance(cp1.myAtoms[1 - i1]) < cp1.radii[1 - i1]);
    // remove higher score (less overlap)
    return (!clash1 && !clash2 ? 0 : cp1.score > cp2.score ? 1 : 2);
  }

  private void newSurface(int displayType, ContactPair cp, BS bs1, BS bs2,
                          RadiusData rd, float[] parameters, Object func,
                          boolean isColorDensity, VolumeData volumeData, float sasurfaceRadius) {
    Parameters params = sg.params;
    params.isSilent = true;
    if (cp == null) {
      bs2.andNot(bs1);
      if (bs1.isEmpty() || bs2.isEmpty())
        return;
    } else {
      params.contactPair = cp;
    }
    int iSlab0 = 0, iSlab1 = 0;
    sg.initState();
    switch (displayType) {
    case T.sasurface:
    case T.surface:
    case T.slab:
    case T.trim:
    case T.full:
      RadiusData rdA, rdB;
      if (displayType == T.surface) {
        rdA = rdVDW;
        rdB = new RadiusData(null, 
            (rd.factorType == EnumType.OFFSET ? rd.value * 2 : (rd.value - 1) * 2 + 1), rd.factorType, rd.vdwType);
      } else {
        rdA = rdB = rd;
      }
      params.colorDensity = isColorDensity;
      if (isColorDensity) {
        setPropI("cutoffRange", new float[] { -100f, 0f }, null);
      }
      if (cp == null) {
        params.atomRadiusData = rdA;
        params.bsIgnore = BSUtil.copyInvert(bs1, ac);
        params.bsSelected = bs1;
        params.bsSolvent = null;
      }
      params.volumeData = volumeData;
      setPropI("sasurface", Float.valueOf(sasurfaceRadius), null);
      setPropI("map", Boolean.TRUE, null);
      if (cp == null) {
        params.atomRadiusData = rdB;
        params.bsIgnore = BSUtil.copyInvert(bs2, ac);
        params.bsSelected = bs2;
      }
      params.volumeData = volumeData;
      setPropI("sasurface", Float.valueOf(sasurfaceRadius), null);
      switch (displayType) {
      case T.full:
      case T.trim:
        iSlab0 = -100;
        break;
      case T.sasurface:
      case T.surface:
        if (isColorDensity)
          iSlab0 = -100;
        break;
      case T.slab:
        iSlab1 = -100;
      }
      break;
    case T.plane:
    case T.connect:
      if (displayType == T.connect)
        sg.setProp("parameters", parameters, null);
      if (cp == null) {
        params.atomRadiusData = rd;
        params.bsIgnore = BSUtil.copyInvert(bs2, ac);
        params.bsIgnore.andNot(bs1);
      }
      params.func = func;
      params.intersection = new BS[] { bs1, bs2 };
      params.volumeData = volumeData;
      params.colorDensity = isColorDensity;
      if (isColorDensity)
        setPropI("cutoffRange", new float[] { -5f, 0f }, null);
      setPropI("sasurface", Float.valueOf(0), null);
      // mapping
      setPropI("map", Boolean.TRUE, null);
      params.volumeData = volumeData;
      setPropI("sasurface", Float.valueOf(0), null);
      if (displayType == T.plane) {
        iSlab0 = -100;
      }
      break;
    }
    if (iSlab0 != iSlab1)
      thisMesh.getMeshSlicer().slabPolygons(TempArray.getSlabWithinRange(iSlab0, iSlab1),
          false);
    if (displayType != T.surface)
      thisMesh.setMerged(true);
  }

  private V3 vZ = new V3();
  private V3 vY = new V3();
  private V3 vX = new V3();
  private P3 pt1 = new P3();
  private P3 pt2 = new P3();
  
  private void setVolumeData(int type, VolumeData volumeData, ContactPair cp,
                             float resolution, int nPairs) {
    pt1.setT(cp.myAtoms[0]);
    pt2.setT(cp.myAtoms[1]);
    vX.sub2(pt2, pt1);
    float dAB = vX.length();
    float dYZ = (cp.radii[0] * cp.radii[0] + dAB * dAB - cp.radii[1] * cp.radii[1])/(2 * dAB * cp.radii[0]);
    dYZ = 2.1f * (float) (cp.radii[0] * Math.sin(Math.acos(dYZ)));
    Measure.getNormalToLine(pt1, pt2, vZ);
    vZ.scale(dYZ);
    vY.cross(vZ, vX);
    vY.normalize();
    vY.scale(dYZ);
    if (type != T.connect) {
      vX.normalize();
      pt1.scaleAdd2((dAB - cp.radii[1]) * 0.95f, vX, pt1);
      pt2.scaleAdd2((cp.radii[0] - dAB) * 0.95f, vX, pt2);
      vX.sub2(pt2, pt1);
    }
    if (resolution == Float.MAX_VALUE)
      resolution = (nPairs > 100 ? 3 : 10);
    
    // now set voxel counts and vectors, and grid origin

    int nX = Math.max(5, (int) Math.floor(pt1.distance(pt2) * resolution + 1));
    if ((nX % 2) == 0)
      nX++;
    int nYZ = Math.max(7, (int) Math.floor(dYZ * resolution + 1));
    if ((nYZ % 2) == 0)
      nYZ++;
    volumeData.setVoxelCounts(nX, nYZ, nYZ);
    pt1.scaleAdd2(-0.5f, vY, pt1);
    pt1.scaleAdd2(-0.5f, vZ, pt1);
    volumeData.setVolumetricOrigin(pt1.x, pt1.y, pt1.z);
    /*
    System.out.println("draw pt1 "+pt1+" color red");
    System.out.println("draw vx vector "+pt1+" "+vX+" color red");
    System.out.println("draw vy vector "+pt1+" "+vY+" color green");
    System.out.println("draw vz vector "+pt1+" "+vZ+" color blue");
    */

    vX.scale(1f/(nX-1));
    vY.scale(1f/(nYZ-1));
    vZ.scale(1f/(nYZ-1));
    volumeData.setVolumetricVector(0, vX.x, vX.y, vX.z);
    volumeData.setVolumetricVector(1, vY.x, vY.y, vY.z);
    volumeData.setVolumetricVector(2, vZ.x, vZ.y, vZ.z);

  }

  private void mergeMesh(MeshData md) {
    thisMesh.merge(md);
    if (minData == Float.MAX_VALUE) {
      // just assign it
    } else if (jvxlData.mappedDataMin == Float.MAX_VALUE) {
      jvxlData.mappedDataMin = minData;
      jvxlData.mappedDataMax = maxData;
    } else {
      jvxlData.mappedDataMin = Math.min(minData, jvxlData.mappedDataMin);
      jvxlData.mappedDataMax = Math.max(maxData, jvxlData.mappedDataMax);
    }
    minData = jvxlData.mappedDataMin;
    maxData = jvxlData.mappedDataMax;
    jvxlData.valueMappedToBlue = minData;
    jvxlData.valueMappedToRed = maxData;

  }

  @SuppressWarnings("unchecked")
  @Override
  protected void addMeshInfo(IsosurfaceMesh mesh, Map<String, Object> info) {
    if (mesh.info == null)
      return;
    Lst<Map<String, Object>> pairInfo = new  Lst<Map<String, Object>>();
    info.put("pairInfo", pairInfo);
    Lst<ContactPair> list = (Lst<ContactPair>) mesh.info;
    for (int i = 0; i < list.size(); i++) {
      Map<String, Object> cpInfo = new Hashtable<String, Object>();
      pairInfo.addLast(cpInfo);
      ContactPair cp = list.get(i);
      cpInfo.put("type", T.nameOf(cp.contactType));
      cpInfo.put("volume", Double.valueOf(cp.volume));
      cpInfo.put("vdwVolume", Double.valueOf(cp.vdwVolume));
      if (!Float.isNaN(cp.xVdwClash)) {
        cpInfo.put("xVdwClash", Double.valueOf(cp.xVdwClash));
      }
      cpInfo.put("score", Double.valueOf(cp.score));
      cpInfo.put("atoms", cp.myAtoms);
      cpInfo.put("radii", cp.radii);
      cpInfo.put("vdws", cp.vdws);
    }
  }

  /**
   * 
   * well, heh, heh... This calculates the VDW extension x at a given distance
   * for a clashing pair that will produce a volume that is equivalent to the
   * volume for the vdw contact at the point of touching (d0 = vdwA + vdwB) and
   * the transition to clash. This will provide the surface that will surround
   * the clash until the clash size is larger than it.
   * @param cp 
   * 
   * @param x0
   * @param vdwA
   * @param vdwB
   * @param d
   */
  private static void getVdwClashRadius(ContactPair cp, double x0, double vdwA, double vdwB,
                                         double d) {

    /// Volume = pi/12 * (r + R - d)^2 * (d + 2(r + R) - 3(r-R)^2/d)
    /// for +vdw +x: pi/12 * (va + vb - d + 2x)^2 * (d + 2(va + vb) + 4x - 3(va-vb)^2/d)
    
    double sum = vdwA + vdwB;
    double dif2 = vdwA - vdwB;
    dif2 *= dif2;
    double v0_nopi = x0 * x0 * (sum + 4.0/3 * x0 - dif2 / sum);
    cp.vdwVolume = cp.volume - v0_nopi * Math.PI;
    //System.out.println("v0 = " + Math.PI * v0_nopi + " v0_nopi =" + v0_nopi);
    
    /// (a + x)^2(b + 2x) = c; where x here is probe DIAMETER

    double a = (sum - d);
    double b = d + 2 * sum - 3 * dif2 / d;
    double c = v0_nopi * 12;

    
    /* from Sage:
     * 
    
a = var('a')
b = var('b')
c = var('c')
x = var('x')
eqn = (a + x)^2 * (b + 2 * x) == c
solve(eqn, x)

[

x == -1/72*(-I*sqrt(3) + 1)*(4*a^2 - 4*a*b + b^2)/(1/27*a^3 -
1/18*a^2*b + 1/36*a*b^2 - 1/216*b^3 + 1/36*sqrt((8*a^3 - 12*a^2*b +
6*a*b^2 - b^3 + 27*c)*c)*sqrt(3) + 1/4*c)^(1/3) - 1/2*(I*sqrt(3) +
1)*(1/27*a^3 - 1/18*a^2*b + 1/36*a*b^2 - 1/216*b^3 + 1/36*sqrt((8*a^3 -
12*a^2*b + 6*a*b^2 - b^3 + 27*c)*c)*sqrt(3) + 1/4*c)^(1/3) - 2/3*a -
1/6*b, 

x == -1/72*(I*sqrt(3) + 1)*(4*a^2 - 4*a*b + b^2)/(1/27*a^3 -
1/18*a^2*b + 1/36*a*b^2 - 1/216*b^3 + 1/36*sqrt((8*a^3 - 12*a^2*b +
6*a*b^2 - b^3 + 27*c)*c)*sqrt(3) + 1/4*c)^(1/3) - 1/2*(-I*sqrt(3) +
1)*(1/27*a^3 - 1/18*a^2*b + 1/36*a*b^2 - 1/216*b^3 + 1/36*sqrt((8*a^3 -
12*a^2*b + 6*a*b^2 - b^3 + 27*c)*c)*sqrt(3) + 1/4*c)^(1/3) - 2/3*a -
1/6*b, 

x == -2/3*a - 1/6*b + 1/36*(4*a^2 - 4*a*b + b^2)/(1/27*a^3 -
1/18*a^2*b + 1/36*a*b^2 - 1/216*b^3 + 1/36*sqrt((8*a^3 - 12*a^2*b +
6*a*b^2 - b^3 + 27*c)*c)*sqrt(3) + 1/4*c)^(1/3) + (1/27*a^3 - 1/18*a^2*b
+ 1/36*a*b^2 - 1/216*b^3 + 1/36*sqrt((8*a^3 - 12*a^2*b + 6*a*b^2 - b^3 +
27*c)*c)*sqrt(3) + 1/4*c)^(1/3)

]

*/
    
/* so...

x1 == f - g*(1/2-I*sqrt(3)/2)/h^(1/3) - (1/2+I*sqrt(3)/2)*h^(1/3)
x2 == f - g*(1/2+I*sqrt(3)/2)/h^(1/3) - (1/2-I*sqrt(3)/2)*h^(1/3)
x3 == f + g/h^(1/3) + h^(1/3)

where

f = -2/3*a - 1/6*b
g = (4*a^2 - 4*a*b + b^2)/36 
h = a^3/27 - a^2*b/18 + a*b^2/36 - b^3/216 + c/4 
     + sqrt(c/432*(8*a^3 - 12*a^2*b + 6*a*b^2 - b^3 + 27*c))

The problem is, that sqrt is imaginary, so the cube root is as well. 

v = a^3/27 - a^2*b/18 + a*b^2/36 - b^3/216 + c/4
u = -c/432*(8*a^3 - 12*a^2*b + 6*a*b^2 - b^3 + 27*c)

*/

    double a2 = a * a;
    double a3 = a * a2;
    double b2 = b * b;
    double b3 = b * b2;

    double f = -a * 2/3 - b/6;
    double g = (4*a2 - 4*a*b + b2)/36;
    double v = a3/27 - a2*b/18 + a*b2/36 - b3/216 + c/4;
    double u = -c/432*(8*a3 - 12*a2*b + 6*a*b2 - b3 + 27*c);
    
    
/*
Then 

h = v + sqrt(u)*I

and we can express h^1/3 as 

vvu (cos theta + i sin theta)

where

vvu = (v^2 + u)^(1/6)
theta = atan2(sqrt(u),v)/3

Now, we know this is a real solution, so we take the real part of that.
The third root seems to be our root (thankfully!)

x3 == f + g/h^(1/3) + h^(1/3)
    = f + (2*g/vvu + vvu) costheta

     */
    

    double theta = Math.atan2(Math.sqrt(u), v);
    
    double vvu = Math.pow(v*v + u, 1.0/6.0);
    double costheta = Math.cos(theta/3);
    
    // x == f + g/h^(1/3) + h^(1/3) = f + g/vvu + vvu)*costheta

    //System.out.println ("a = " + a + ";b = " + b + ";c = " + c + ";f = " + f + ";g = " + g + "");

    double x;
    
    x = f + (g/vvu + vvu) * costheta;
    //System.out.println(d + "\t" + x + "\t" + ((a + x)*(a + x) * (b + 2 * x)) + " = " + c);
    if (x > 0) {
      cp.xVdwClash = ((float) (x / 2));
    }
  }

}
