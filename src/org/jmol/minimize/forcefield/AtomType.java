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
  
  /**
   * MMFF special bond types 2, 3, 4, 9, 30, 37, 39, 54, 57, 58, 63, 64, 67, 75, 78,
   * 80, 81 for which a=b-c=d  bc bond should be considered single, not double
   * 
   */
  boolean sbmb;
  
  /**
   * MMFF aromatic types
   * 
   * 37, 38, 39, 44, 58, 59, 63, 64, 65, 66, 69, 78, 79, 81, 82
   * 
   */
  boolean arom;
  
  /**
   * MMFF pi lone pair type, i.e. "those atom types which have a pi lone pair
   * capable of partaking in resonance interactions with, say, an adjacent
   * multiple bond" [https://hpc.nih.gov/apps/charmm/c39b2html/mmff_params.html]
   * 
   * 6, 8, 10, 11, 12, 13, 14, 15, 26, 32, 35, 39, 40, 43, 44, 59, 62, 70, 72,
   * 76
   */
  boolean pilp;
  
  /**
   * MMFF multiple bond type "specifies cases in which double (2) or triple (3)
   * bonds are expected to be made to an atom having the listed atom type"
   * [https://hpc.nih.gov/apps/charmm/c39b2html/mmff_params.html]
   */
  int mltb;
  
  /**
   * valence (number of bonds, generally 4 for neutral carbon, regardless of bonding, but 3 in the case of isonitrile)
   */
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