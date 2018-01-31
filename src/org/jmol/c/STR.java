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

package org.jmol.c;

public enum STR {
  
  // Note: These id numbers are non-negotiable. They are documented and 
  // accessible via {atom}.structure and {atom}.substructure
  // DO NOT CHANGE THEM!
  
  NOT(-1,0xFF808080),
  NONE(0,0xFFFFFFFF),
  TURN(1,0xFF6080FF),
  SHEET(2,0xFFFFC800),
  HELIX(3,0xFFFF0080),
  DNA(4,0xFFAE00FE),
  RNA(5,0xFFFD0162),
  CARBOHYDRATE(6,0xFFA6A6FA),
  HELIX310(7,0xFFA00080),
  HELIXALPHA(8,0xFFFF0080),
  HELIXPI(9,0xFF600080),
  ANNOTATION(-2,0);
  
  private int id;
  private int color;
  
  private STR(int id, int color) {
    this.id = id;
    this.color = color;
  }
  
  public int getId() {
    return id;
  }

  public int getColor() {
    return color;
  }


  /****************************************************************
   * In DRuMS, RasMol, and Chime, quoting from
   * http://www.umass.edu/microbio/rasmol/rascolor.htm
   *
   *The RasMol structure color scheme colors the molecule by
   *protein secondary structure.
   *
   *Structure                   Decimal RGB    Hex RGB
   *Alpha helices  red-magenta  [255,0,128]    FF 00 80  *
   *Beta strands   yellow       [255,200,0]    FF C8 00  *
   *
   *Turns          pale blue    [96,128,255]   60 80 FF
   *Other          white        [255,255,255]  FF FF FF
   *
   **Values given in the 1994 RasMol 2.5 Quick Reference Card ([240,0,128]
   *and [255,255,0]) are not correct for RasMol 2.6-beta-2a.
   *This correction was made above on Dec 5, 1998.
   * @param name 
   * @return     0-3 or 7-9, but not dna, rna, carbohydrate
   ****************************************************************/
  public final static STR getProteinStructureType(String name) {
    for (STR item : values())
      if (name.equalsIgnoreCase(item.name()))
        return (item.isProtein() ? item : NOT);
    return NOT;
  }

  public String getBioStructureTypeName(boolean isGeneric) {
    return (id < 0 ? "" : isGeneric && isProtein() ? "protein" : name());
  }

  private boolean isProtein() {
    return id >= 0 && id <= 3 || id >= 7;
  }
}
