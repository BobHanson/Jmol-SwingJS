/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-03-05 12:22:08 -0600 (Sun, 05 Mar 2006) $
 * $Revision: 4545 $
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

import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.util.C;
import org.jmol.util.Escape;

import javajs.util.BS;

import javajs.util.Lst;
import javajs.util.SB;
import javajs.util.P3;
import javajs.util.V3;

public class Dipole {

  String thisID = "";
  public short mad;
  public short colix = 0;
  short type;

  public P3 origin;
  public P3 center;
  public V3 vector;

  String dipoleInfo = "";
  public float dipoleValue;

  boolean isUserValue;
  public float offsetSide;
  public float offsetAngstroms;
  public P3 offsetPt;
  int offsetPercent;
  public int visibilityFlags;
  int modelIndex;

  boolean visible;
  public boolean noCross;
  boolean haveAtoms;
  boolean isValid;

  public Atom[] atoms = new Atom[2]; //for reference only
  P3[] coords = new P3[2]; //for reference only
  public Bond bond;
  public BS bsMolecule;
  public Lst<Object> lstDipoles;

  final static short DIPOLE_TYPE_UNKNOWN = 0;
  final static short DIPOLE_TYPE_POINTS = 1;
  final static short DIPOLE_TYPE_ATOMS = 2;
  final static short DIPOLE_TYPE_BOND = 3;
  final static short DIPOLE_TYPE_MOLECULAR = 4;
  final static short DIPOLE_TYPE_POINTVECTOR = 5;

  Dipole init(int modelIndex, String thisID, String dipoleInfo, short colix,
      short mad, boolean visible) {
    this.modelIndex = modelIndex;
    this.thisID = thisID;
    this.dipoleInfo = dipoleInfo;
    this.colix = colix;
    this.mad = mad;
    this.visible = visible;
    this.type = DIPOLE_TYPE_UNKNOWN;
    return this;
  }

  void setTranslucent(boolean isTranslucent, float translucentLevel) {
    colix = C.getColixTranslucent3(colix, isTranslucent, translucentLevel);
  }

  void set(Dipole d) {
    thisID = d.thisID;
    dipoleInfo = d.dipoleInfo;
    dipoleValue = d.dipoleValue;
    mad = d.mad;
    lstDipoles = d.lstDipoles;
    if (lstDipoles != null)
      isValid = true;
    offsetAngstroms = d.offsetAngstroms;
    offsetPercent = d.offsetPercent;
    offsetSide = d.offsetSide;
    vector = V3.newV(d.vector);
    origin = P3.newP(d.origin);
    if (d.offsetPt != null) {
      origin.add(d.offsetPt);
      offsetPt = P3.newP(d.offsetPt);
    }
    bsMolecule = d.bsMolecule;
    haveAtoms = (d.atoms[0] != null);
    if (haveAtoms) {
      this.atoms[0] = d.atoms[0];
      this.atoms[1] = d.atoms[1];
      centerDipole();
    } else {
      center = null;
    }
  }

  private void set2(P3 pt1, P3 pt2) {
    coords[0] = P3.newP(pt1);
    coords[1] = P3.newP(pt2);
    isValid = (coords[0].distance(coords[1]) > 0.1f);

    if (dipoleValue < 0) {
      origin = P3.newP(pt2);
      vector = V3.newV(pt1);
      dipoleValue = -dipoleValue;
    } else {
      origin = P3.newP(pt1);
      vector = V3.newV(pt2);
    }
    dipoleInfo = "" + origin + vector;
    vector.sub(origin);
    if (dipoleValue == 0)
      dipoleValue = vector.length();
    else
      vector.scale(dipoleValue / vector.length());
    this.type = DIPOLE_TYPE_POINTS;
  }

  void setValue(float value) {
    float d = dipoleValue;
    dipoleValue = value;
    if (value == 0)
      isValid = false;
    if (vector == null)
      return;
    vector.scale(dipoleValue / vector.length());
    if (d * dipoleValue < 0)
      origin.sub(vector);
  }

  void set2Value(P3 pt1, P3 pt2, float value) {
    dipoleValue = value;
    atoms[0] = null;
    set2(pt1, pt2);
  }

  void setPtVector(P3 pt1, V3 dipole) {
    setValue(dipole.length());
    P3 pt2 = P3.newP(pt1);
    pt2.add(dipole);
    set2(pt1, pt2);
    type = DIPOLE_TYPE_POINTVECTOR;
  }

  void set2AtomValue(Atom atom1, Atom atom2, float value) {
    //also from frame
    setValue(value);
    set2(atom1, atom2);
    offsetSide = Dipoles.DEFAULT_OFFSETSIDE;
    mad = Dipoles.DEFAULT_MAD;
    atoms[0] = atom1;
    atoms[1] = atom2;
    haveAtoms = true;
    centerDipole();
  }

  void centerDipole() {
    isValid = (atoms[0] != atoms[1] && dipoleValue != 0);
    if (!isValid)
      return;
    float f = atoms[0].distance(atoms[1]) / (2 * dipoleValue) - 0.5f;
    origin.scaleAdd2(f, vector, atoms[0]);
    center = new P3();
    center.scaleAdd2(0.5f, vector, origin);
    bond = atoms[0].getBond(atoms[1]);
    type = (bond == null ? Dipole.DIPOLE_TYPE_ATOMS : Dipole.DIPOLE_TYPE_BOND);
  }

  boolean isBondType() {
    return (type == Dipole.DIPOLE_TYPE_ATOMS || type == Dipole.DIPOLE_TYPE_BOND);
  }

  public String getShapeState() {
    if (!isValid)
      return "";
    SB s = new SB();
    s.append("dipole ID ").append(thisID);
    if (lstDipoles != null)
      s.append(" all ").append(Escape.eBS(bsMolecule));
    else if (haveAtoms)
      s.append(" ({").appendI(atoms[0].i).append("}) ({").appendI(atoms[1].i)
          .append("})");
    else if (coords[0] == null)
      return "";
    else
      s.append(" ").append(Escape.eP(coords[0])).append(" ")
          .append(Escape.eP(coords[1]));
    if (isUserValue)
      s.append(" value ").appendF(dipoleValue);
    if (mad != Dipoles.DEFAULT_MAD)
      s.append(" width ").appendF(mad / 1000f);
    if (offsetAngstroms != 0)
      s.append(" offset ").appendF(offsetAngstroms);
    else if (offsetPercent != 0)
      s.append(" offset ").appendI(offsetPercent);
    if (offsetSide != Dipoles.DEFAULT_OFFSETSIDE)
      s.append(" offsetSide ").appendF(offsetSide);
    if (offsetPt != null)
      s.append(" offset ").append(Escape.eP(offsetPt));
    if (noCross)
      s.append(" nocross");
    if (!visible)
      s.append(" off");
    s.append(";\n");
    return s.toString();
  }

  public void setOffsetPt(P3 pt) {
    if (offsetPt != null)
      origin.sub(offsetPt);
    offsetPt = pt;
    origin.add(pt);
  }
}
