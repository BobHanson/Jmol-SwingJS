/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-02-25 14:07:40 -0600 (Tue, 25 Feb 2014) $
 * $Revision: 19387 $
 *
 * Copyright (C) 2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.util;

import javajs.util.SB;


public final class Rgb16 {
  public int r;
  public int g;
  public int b;
    
  public Rgb16() {
  }

  public static Rgb16 newI(int argb) {
    Rgb16 c = new Rgb16();
    c.setInt(argb);
    return c;
  }

  public void setInt(int a) {
    r = ((a >> 8) & 0xFF00) | 0x80;
    g = ((a     ) & 0xFF00) | 0x80;
    b = ((a << 8) & 0xFF00) | 0x80;
  }

  public void setRgb(Rgb16 a) {
    r = a.r;
    g = a.g;
    b = a.b;
  }

  public void diffDiv(Rgb16 a, Rgb16 b, int divisor) {
    r = (a.r - b.r) / divisor;
    g = (a.g - b.g) / divisor;
    this.b = (a.b - b.b) / divisor;
  }
  public void setAndIncrement(Rgb16 base, Rgb16 inc) {
    r = base.r;
    base.r += inc.r;
    g = base.g;
    base.g += inc.g;
    b = base.b;
    base.b += inc.b;
  }

  public int getArgb() {
    return (                 0xFF000000 |
           ((r << 8) & 0x00FF0000)|
           (g        & 0x0000FF00)|
           (b >> 8));
  }

  @Override
  public String toString() {
    return new SB()
    .append("Rgb16(").appendI(r).appendC(',')
    .appendI(g).appendC(',')
    .appendI(b).append(" -> ")
    .appendI((r >> 8) & 0xFF).appendC(',')
    .appendI((g >> 8) & 0xFF).appendC(',')
    .appendI((b >> 8) & 0xFF).appendC(')').toString();
  }
}

