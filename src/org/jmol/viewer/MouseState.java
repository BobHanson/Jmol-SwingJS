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

package org.jmol.viewer;

public class MouseState {
  /**
   * 
   */
  int x = -1000;
  int y = -1000;
  int modifiers = 0;
  public long time = -1;
  
  public String name;
  
  MouseState(String name) {
    this.name = name;
  }
  
  void set(long time, int x, int y, int modifiers) {
    this.time = time;
    this.x = x;
    this.y = y;
    this.modifiers = modifiers;
  }

  /**
   * @param current 
   * @param clickCount 
   */
  void setCurrent(MouseState current, int clickCount) {
    time = current.time;
    if (clickCount < 2) {
      x = current.x;
      y = current.y;
    }
    modifiers = current.modifiers;
  }

  public boolean inRange(int xyRange, int x, int y) {
    return (Math.abs(this.x - x) <= xyRange && Math.abs(this.y - y) <= xyRange);
  }
  
  private final static int MIN_DELAY_MS = 20;
  public boolean check(int xyRange, int x, int y, int modifiers, long time, long delayMax) {
    return (this.modifiers == modifiers 
        && (delayMax >= Integer.MAX_VALUE ? inRange(xyRange, x, y) 
            : time - this.time < delayMax && time - this.time > MIN_DELAY_MS));
  }

  public boolean is(MouseState current) {
    return (current.x == x && current.y == y && current.time == time);
  }
  
}