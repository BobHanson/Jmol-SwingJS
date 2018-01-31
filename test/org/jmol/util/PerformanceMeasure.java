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
 * Test class to measure performance.
 */
public class PerformanceMeasure {

  private long start;
  private String method;

  public PerformanceMeasure(String method) {
    this.method = method;
    start = System.currentTimeMillis();
  }

  public void logPerformance(String text) {
    long current = System.currentTimeMillis();
    if (current - start == 0) {
      return;
    }
    System.err.print(method);
    System.err.print(": ");
    System.err.print(current - start);
    System.err.print(" milliseconds: ");
    System.err.println(text);
  }
}
