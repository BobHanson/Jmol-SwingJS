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

package org.jmol.thread;

import javajs.api.GenericPlatform;

import org.jmol.viewer.ActionManager;
import org.jmol.viewer.MouseState;
import org.jmol.viewer.Viewer;

public class HoverWatcherThread extends JmolThread {
  
  /**
   * 
   */
  private ActionManager actionManager;
  private final MouseState current, moved;
  private int hoverDelay;

  /**
   * @param actionManager
   * @param current
   * @param moved
   * @param vwr
   * 
   * @j2sIgnoreSuperConstructor
   * 
   */
  public HoverWatcherThread(ActionManager actionManager, MouseState current, MouseState moved, Viewer vwr) {
    setViewer(vwr, "HoverWatcher");
    this.actionManager = actionManager;
    this.current = current;
    this.moved = moved;
    start();
  }

  @Override
  protected void run1(int mode) throws InterruptedException {
    while (true)
      switch (mode) {
      case INIT:
        if (!isJS)
          Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        mode = MAIN;
        break;
      case MAIN:
        hoverDelay = vwr.getHoverDelay();
        if (stopped || hoverDelay <= 0 || !runSleep(hoverDelay, CHECK1))
          return;
        mode = CHECK1;
        break;
      case CHECK1:
        if (moved.is(current)) {
          currentTime = System.currentTimeMillis();
          int howLong = (int) (currentTime - moved.time);
          if (howLong > (vwr.acm.zoomTrigger ? 100 : hoverDelay) && !stopped) {
            actionManager.checkHover();
          }
        }
        mode = MAIN;
        break;
      }
  }

}