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

package org.jmol.minimize;

import org.jmol.thread.JmolThread;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

public class MinimizationThread extends JmolThread {
  
  private Minimizer minimizer;

  public MinimizationThread() {}
  
  @Override
  public int setManager(Object manager, Viewer vwr, Object options) {
    minimizer = (Minimizer) manager;
    setViewer(vwr, "MinimizationThread");
    return 0;
  }
  
  @Override
  protected void run1(int mode) throws InterruptedException {
    while (true)
      switch (mode) {
      case INIT:
        lastRepaintTime = startTime;
        haveReference = true;
        //should save the atom coordinates
        if (!this.minimizer.startMinimization())
          return;
        vwr.startHoverWatcher(false);
        mode = MAIN;
        break;
      case MAIN:
        if (!minimizer.minimizationOn() || checkInterrupted(minimizer.getThread())) {
          mode = FINISH;
          break;
        }
        currentTime = System.currentTimeMillis();
        int elapsed = (int) (currentTime - lastRepaintTime);
        int sleepTime = 33 - elapsed;
        if (!runSleep(sleepTime, CHECK1))
          return;
        mode = CHECK1;
        break;
      case CHECK1:
        lastRepaintTime = currentTime = System.currentTimeMillis();
        mode = (this.minimizer.stepMinimization() ? MAIN : FINISH);
        break;
      case FINISH:
        this.minimizer.endMinimization(true);
        vwr.startHoverWatcher(true);
        return;
      }
  }

  @Override
  protected void oops(Exception e) {
    if (this.minimizer.minimizationOn())
      Logger.error(e.toString());
  }
  

}