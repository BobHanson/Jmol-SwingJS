/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005  The Jmol Development Team
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

package org.jmol.util;

/**
 * Class for measuring performance of Jmol code.
 */
public class Profiling {

  private static long start;

  public static void startProfiling() {
    start = getTime();
  }
  
  public static void logProfiling(String txt) {
    long delta = getTime() - start;
    String label = ("            " + delta);
    label = label.substring(label.length() - 12, label.length());
    if (delta > 100000) {
      System.err.println(label + "ns: " + txt);
    }
    System.err.flush();
    start = getTime();
  }

  public static long getTime() {
    return System.nanoTime();
  }

  public static String getUnit() {
    return "ns";
  }
}
