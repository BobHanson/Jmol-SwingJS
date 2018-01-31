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

import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.Group;
import org.jmol.viewer.JC;

/**
 * Enum for hydrogen bonding donor/acceptor type
 */
public enum HB {
  NOT, ACCEPTOR, DONOR, UNKNOWN;

  public static HB getType(Atom atom) {
    Group group = atom.group;
    boolean considerHydrogens = !atom.isHetero();
    switch (atom.getElementNumber()) {
    default:
      return NOT;
    case 1:
      if (atom.getCovalentBondCount() == 0)
        return DONOR;
      Bond[] bonds = atom.bonds;
      if (bonds == null)
        return NOT;
      switch (bonds[0].getOtherAtom(atom).getElementNumber()) {
      case 7:
      case 8:
      case 16:
        return DONOR;
      }
      return NOT;
    case 7:
      if (atom == group.getNitrogenAtom())
        return DONOR;
      if (group.groupID == JC.GROUPID_HISTIDINE)
        return UNKNOWN;
      if (atom.getCovalentHydrogenCount() > 0)
        return DONOR;
      if (considerHydrogens)
        return ACCEPTOR;
      switch (group.groupID) {
      case JC.GROUPID_ARGININE:
      case JC.GROUPID_ASPARAGINE:
      case JC.GROUPID_LYSINE:
      case JC.GROUPID_GLUTAMINE:
      case JC.GROUPID_TRYPTOPHAN:
        return DONOR;
      }
      return UNKNOWN;
    case 8:
      if (atom == group.getCarbonylOxygenAtom() || atom.getFormalCharge() == -1)
        return ACCEPTOR;
      if (atom.getCovalentBondCount() == 0 || atom.getCovalentHydrogenCount() > 0)
        return UNKNOWN;
      if (considerHydrogens)
        return ACCEPTOR;       
      switch (group.groupID) {
      case JC.GROUPID_ASPARTATE:
      case JC.GROUPID_GLUTAMATE:
        return ACCEPTOR;
      }
      return UNKNOWN;
    }
  }

  public static boolean isPossibleHBond(HB typeA, HB typeB) {
    return (typeA == NOT || typeB == NOT ? false
        : typeA == UNKNOWN || typeA != typeB);
  }
  
}
