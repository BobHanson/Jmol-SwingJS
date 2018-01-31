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

public enum VDW {

  JMOL(0, "Jmol",null),  //OpenBabel-1.0
  BABEL(1, "Babel",null),  //OpenBabel-2.2
  RASMOL(2, "RasMol",null),  //OpenRasmol-2.7.2.1.1
  BABEL21(3, "Babel21",null),  //OpenBabel-2.1
  
  AUTO_JMOL(0, null,"Jmol"),  //JMOL if undecided
  AUTO_BABEL(1, null,"Babel"),  //BABEL if undecided
  AUTO_RASMOL(2, null,"RasMol"),  //RASMOL if undecided
  AUTO(0, "Auto",null),  //
  
  USER(-1, "User",null),  //

  ADPMAX(-1, null, "adpmax"), 
  ADPMIN(-1, null, "adpmin"), 
  HYDRO(-1, null, "hydrophobic"),
  BONDING(-1, null, "bondingradius"),
  TEMP(-1, null, "temperature"),

  NOJMOL(-1, null,null), //surface will be adding H atoms ??
  NADA(-1, null, null);  // ABSOLUTE type -- ignore

  public int pt;
  private String type;
  private String type2;
  
  private VDW(int pt, String type, String type2) {
    this.pt = pt;
    this.type = type;
    this.type2 = type2;
  }
  public String getVdwLabel() {
    return (type == null ? type2 : type);
  }

  public static VDW getVdwType(String label) {
    if (label != null)
      for (VDW item : values())
        if (label.equalsIgnoreCase(item.type))
          return item;
    return null;
  }
  
  public static VDW getVdwType2(String label) {
    if (label != null)
      for (VDW item : values())
        if (label.equalsIgnoreCase(item.type2))
          return item;
    return null;
  }
}
