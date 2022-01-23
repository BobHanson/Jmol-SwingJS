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

import javajs.util.SB;

public enum CBK {

  ANIMFRAME,
  APPLETREADY,
  ATOMMOVED,
  AUDIO,
  CLICK,
  DRAGDROP,
  ECHO,
  ERROR,
  EVAL,
  HOVER,
  IMAGE,
  LOADSTRUCT,
  MEASURE,
  MESSAGE,
  MINIMIZATION,
  MODELKIT,
  PICK,
  RESIZE,
  SCRIPT,
  SELECT,
  SERVICE,
  STRUCTUREMODIFIED,
  SYNC;

  public static CBK getCallback(String name) {
    
    name = name.toUpperCase();
    int pt = name.indexOf("CALLBACK");
    if (pt > 0)
      name = name.substring(0, pt);
    for (CBK item : values())
      if (item.name().equalsIgnoreCase(name))
        return item;
    return null;
  }

  private static String nameList;

  public static synchronized String getNameList() {
    if (nameList == null) {
      SB names = new SB();
      for (CBK item : values())
        names.append(item.name().toLowerCase()).append("Callback;");
      nameList = names.toString();
    }
    return nameList;
  }
}
