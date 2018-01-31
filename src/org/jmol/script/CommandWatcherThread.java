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

package org.jmol.script;

import org.jmol.thread.JmolThread;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

public class CommandWatcherThread extends JmolThread {
  /**
   * 
   */
  private ScriptManager scriptManager;

  public CommandWatcherThread() {}
  
  
  @Override
  public int setManager(Object manager, Viewer vwr, Object params) {
    scriptManager = (ScriptManager) manager;
    setViewer(vwr, "CommmandWatcherThread"); 
    return 0;
  }

  private final static int commandDelay = 50; // was 200

  @Override
  public void run() {
    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
    while (!stopped) {
      try {
        Thread.sleep(commandDelay);
        if (!stopped) {
          scriptManager.runScriptNow();
        }
      } catch (InterruptedException ie) {
        Logger.warn("CommandWatcher InterruptedException! " + this);
        break;
      } catch (Exception ie) {
        String s = "script processing ERROR:\n\n" + ie.toString();
        /**
         * @j2sNative
         * 
         */
        {
          for (int i = 0; i < ie.getStackTrace().length; i++) {
            s += "\n" + ie.getStackTrace()[i].toString();
          }
        }
        Logger.warn("CommandWatcher Exception! " + s);
        break;
      }
    }
  }
  
  @Override
  protected void run1(int mode) throws InterruptedException {
    // N/A
  }

}