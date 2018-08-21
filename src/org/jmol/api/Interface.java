  /* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2006  The Jmol Development Team
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

package org.jmol.api;

import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

public class Interface {

  /**
   * @param name
   * @param vwr
   * @param state
   * @return instance
   */
  public static Object getInterface(String name, Viewer vwr, String state) {
    try {
      Class<?> x = null;
//      if (vwr.isJSApplet) {
//      /**
//       * @j2sNative 
//       * 
//       * x = Clazz._4Name (name, vwr && vwr.html5Applet, state);
//       * 
//       */
//      } else {
        x = Class.forName(name);
//      }
      return (x == null ? null : x.newInstance());
    } catch (Exception e) {
      Logger.error("Interface.java Error creating instance for " + name
          + ": \n" + e);
      return null;
    }
  }

  /**
   * Note! Do not use this method with "viewer." or "util." because
   * when the JavaScript is built, "org.jmol.util" and "org.jmol.viewer"
   * are condensed to "JU" and "JV"  (javajs.util is also JU)
   * 
   * @param className
   * @param vwr
   * @param state
   * @return class instance
   */
  public static Object getOption(String className, Viewer vwr, String state) {
    return getInterface("org.jmol." + className, vwr, state);
  }

  public static Object getUtil(String name, Viewer vwr, String state) {
    return getInterface("org.jmol.util." + name, vwr, state);
  }

  public static SymmetryInterface getSymmetry(Viewer vwr, String state) {
    return (SymmetryInterface) getInterface("org.jmol.symmetry.Symmetry", vwr, state);
  }

}
