/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-03-14 21:45:06 -0500 (Mon, 14 Mar 2016) $
 * $Revision: 21001 $
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

package org.jmol.shapespecial;

import org.jmol.script.T;
import org.jmol.shape.AtomShape;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Escape;

import javajs.util.SB;
import javajs.util.M3;
import javajs.util.M4;

import org.jmol.viewer.JC;

import org.jmol.atomdata.RadiusData;
import org.jmol.atomdata.RadiusData.EnumType;
import org.jmol.geodesic.EnvelopeCalculation;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;

import java.util.Hashtable;
import java.util.Map;




public class Dots extends AtomShape {

  public EnvelopeCalculation ec;
  public boolean isSurface = false;

  final static float SURFACE_DISTANCE_FOR_CALCULATION = 10f;

  BS bsOn = new BS();
  private BS bsSelected, bsIgnore;

  public static int MAX_LEVEL = JC.ENV_CALC_MAX_LEVEL;

  int thisAtom;
  float thisRadius;
  int thisArgb;

  RadiusData rdLast = new RadiusData(null, 0, null, null);

  @Override
  public void initShape() {
    super.initShape();
    translucentAllowed = false; //except for geosurface
    ec = new EnvelopeCalculation().set(vwr, ac, mads);
  }

  @Override
  public int getSize(int atomIndex) {
    // mads are actually radii not diameters
    return (mads != null ? mads[atomIndex]*2 : bsOn.get(atomIndex) ? (int) Math.floor(ec.getRadius(atomIndex) * 2000) : 0);
  }
  
  @Override
  public void setProperty(String propertyName, Object value, BS bs) {

    if ("init" == propertyName) {
      initialize();
      return;
    }

    if ("translucency" == propertyName) {
      if (!translucentAllowed)
        return; // no translucent dots, but ok for geosurface
    }

    if ("ignore" == propertyName) {
      bsIgnore = (BS) value;
      return;
    }

    if ("select" == propertyName) {
      bsSelected = (BS) value;
      return;
    }

    // next four are for serialization
    if ("radius" == propertyName) {
      thisRadius = ((Float) value).floatValue();
      if (thisRadius > Atom.RADIUS_MAX)
        thisRadius = Atom.RADIUS_GLOBAL;
      return;
    }
    if ("colorRGB" == propertyName) {
      thisArgb = ((Integer) value).intValue();
      return;
    }
    if ("atom" == propertyName) {
      thisAtom = ((Integer) value).intValue();
      if (thisAtom >= atoms.length)
        return;
      atoms[thisAtom].setShapeVisibility(vf, true);
      ec.allocDotsConvexMaps(ac);
      return;
    }
    if ("dots" == propertyName) {
      if (thisAtom >= atoms.length)
        return;
      isActive = true;
      ec.setFromBits(thisAtom, (BS) value);
      atoms[thisAtom].setShapeVisibility(vf, true);
      if (mads == null) {
        ec.setMads(null);
        mads = new short[ac];
        for (int i = 0; i < ac; i++)          
          if (atoms[i].isVisible(Atom.ATOM_INFRAME | vf)) 
            // was there a reason we were not checking for hidden?
            try {
              mads[i] = (short) (ec.getAppropriateRadius(i) * 1000);
            } catch (Exception e) {
              // ignore - someone is messing with the state file
            }
        ec.setMads(mads);
      }
      mads[thisAtom] = (short) (thisRadius * 1000f);
      if (colixes == null) 
        checkColixLength(C.BLACK, ac);
      colixes[thisAtom] = C.getColix(thisArgb);
      bsOn.set(thisAtom);
      //all done!
      return;
    }

    if ("refreshTrajectories" == propertyName) {
      bs = (BS) ((Object[]) value)[1];
      M4 m4 = (M4) ((Object[]) value)[2];
      if (m4 == null) // end of compare command
        return;
      M3 m = new M3();
      m4.getRotationScale(m);
      ec.reCalculate(bs, m);
      return;
    }

    if (propertyName == "deleteModelAtoms") {
      int firstAtomDeleted = ((int[])((Object[])value)[2])[1];
      int nAtomsDeleted = ((int[])((Object[])value)[2])[2];
      BSUtil.deleteBits(bsOn, bs);
      ec.deleteAtoms(firstAtomDeleted, nAtomsDeleted);
      // pass to AtomShape via super
    }

    setPropAS(propertyName, value, bs);
  }

  void initialize() {
    bsSelected = null;
    bsIgnore = null;
    isActive = false;
    if (ec == null)
      ec = new EnvelopeCalculation().set(vwr, ac, mads);
  }

  @Override
  protected void setSizeRD(RadiusData rd, BS bsSelected) {
    if (rd == null)
      rd = new RadiusData(null, 0, EnumType.ABSOLUTE, null);
    if (this.bsSelected != null)
      bsSelected = this.bsSelected;

    // if mad == 0 then turn it off
    // 1 van der Waals (dots) or +1.2, calconly)
    // -1 ionic/covalent
    // 2 - 1001 (mad-1)/100 * van der Waals
    // 1002 - 11002 (mad - 1002)/1000 set radius 0.0 to 10.0 angstroms
    // 11003- 13002 (mad - 11002)/1000 set radius to vdw + additional radius
    // Short.MIN_VALUE -- ADP min
    // Short.MAX_VALUE -- ADP max

    boolean isVisible = true;
    float setRadius = Float.MAX_VALUE;
    isActive = true;

    switch (rd.factorType) {
    case OFFSET:
      break;
    case ABSOLUTE:
      if (rd.value == 0)
        isVisible = false;
      setRadius = rd.value;
      //$FALL-THROUGH$
    default:
      rd.valueExtended = vwr.getCurrentSolventProbeRadius();
    }

    float maxRadius;
    switch (rd.vdwType) {
    case ADPMIN:
    case ADPMAX:
    case HYDRO:
    case TEMP:
      maxRadius = setRadius;
      break;
    case BONDING:
      maxRadius = ms.getMaxVanderwaalsRadius() * 2; // TODO?
      break;
    default:
      maxRadius = ms.getMaxVanderwaalsRadius();
    }

    // combine current and selected set
    boolean newSet = (rdLast.value != rd.value
        || rdLast.valueExtended != rd.valueExtended || rdLast.factorType != rd.factorType
        || rdLast.vdwType != rd.vdwType || ec.getDotsConvexMax() == 0);

    // for an solvent-accessible surface there is no torus/cavity issue.
    // we just increment the atom radius and set the probe radius = 0;

    if (isVisible) {
      for (int i = bsSelected.nextSetBit(0); i >= 0; i = bsSelected
          .nextSetBit(i + 1))
        if (!bsOn.get(i)) {
          bsOn.set(i);
          newSet = true;
        }
    } else {
      boolean isAll = (bsSelected == null);
      int i0 = (isAll ? ac - 1 : bsSelected.nextSetBit(0));
      for (int i = i0; i >= 0; i = (isAll ? i - 1 : bsSelected
          .nextSetBit(i + 1)))
        bsOn.setBitTo(i, false);
    }

    for (int i = ac; --i >= 0;)
      atoms[i].setShapeVisibility(vf, bsOn.get(i));
    if (!isVisible)
      return;
    if (newSet) {
      mads = null;
      ec.newSet();
    }

    // always delete old surfaces for selected atoms
    BS[] dotsConvexMaps = ec.getDotsConvexMaps();
    if (dotsConvexMaps != null) {
      for (int i = ac; --i >= 0;)
        if (bsOn.get(i)) {
          dotsConvexMaps[i] = null;
        }
    }
    // now, calculate surface for selected atoms

    if (dotsConvexMaps == null && (colixes == null || colixes.length != ac))
      checkColixLength(C.BLACK, ac);
    ec.calculate(rd, maxRadius, bsOn, bsIgnore, !vwr.getBoolean(T.dotsurface),
        vwr.getBoolean(T.dotsselectedonly), isSurface, true);

    rdLast = rd;

  }

  @Override
  public void setAtomClickability() {
    for (int i = ac; --i >= 0;) {
      Atom atom = atoms[i];
      if ((atom.shapeVisibilityFlags & vf) == 0
          || ms.isAtomHidden(i))
        continue;
      atom.setClickable(vf);
    }
  }

  @Override
  public String getShapeState() {
    BS[] dotsConvexMaps = ec.getDotsConvexMaps();
    if (dotsConvexMaps == null || ec.getDotsConvexMax() == 0)
      return "";
    SB s = new SB();
    Map<String, BS> temp = new Hashtable<String, BS>();
    int ac = vwr.ms.ac;
    String type = (isSurface ? "geoSurface " : "dots ");
    for (int i = 0; i < ac; i++) {
      if (!bsOn.get(i) || dotsConvexMaps[i] == null)
        continue;
      if (bsColixSet != null && bsColixSet.get(i))
        BSUtil.setMapBitSet(temp, i, i, getColorCommand(type, paletteIDs[i], colixes[i], translucentAllowed));
      BS bs = dotsConvexMaps[i];
      if (!bs.isEmpty()) {
        float r = ec.getAppropriateRadius(i);
        appendCmd(s, type + i + " radius " + r + " "
            + Escape.eBS(bs));
      }
    }
    return s.append(vwr.getCommands(temp, null, "select")).toString();
  }

}
