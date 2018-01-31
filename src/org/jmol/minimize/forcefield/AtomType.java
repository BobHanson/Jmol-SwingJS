/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.minimize.forcefield;

public class AtomType {
  int elemNo;
  String descr;
  String smartsCode;
  int mmType;
  int hType;
  float formalCharge;
  float fcadj;
  boolean sbmb;
  boolean arom;
  boolean pilp;
  int mltb;
  int val;
  
  AtomType(int elemNo, int mmType, int hType, float formalCharge, int val, String descr, String smartsCode) {
    this.elemNo = elemNo;
    this.mmType = mmType;
    this.hType = hType;
    this.formalCharge = formalCharge;
    this.val = val;
    this.descr = descr;
    this.smartsCode = smartsCode;
  }
}