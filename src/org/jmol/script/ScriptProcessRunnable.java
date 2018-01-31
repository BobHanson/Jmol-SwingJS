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

import org.jmol.util.Logger;
import org.jmol.viewer.ShapeManager;

public class ScriptProcessRunnable implements Runnable {
  /**
   * 
   */
  private final ScriptParallelProcessor parallelProcessor;
  private final ScriptProcess process;
  private Object processLock;
  private ShapeManager shapeManager;
  /**
   * 
   * @param process
   * @param lock
   * @param shapeManager 
   * @param parallelProcessor TODO
   */
  public ScriptProcessRunnable(ScriptParallelProcessor parallelProcessor, ScriptProcess process, Object lock, ShapeManager shapeManager) {
    this.parallelProcessor = parallelProcessor;
    this.process = process;
    processLock = lock;
    this.shapeManager = shapeManager;
  }

  @Override
  public void run() {
    try {
      if (this.parallelProcessor.error == null) {
        if (Logger.debugging)
          Logger.debug("Running process " + process.processName + " "
              + process.context.pc + " - " + (process.context.pcEnd - 1));
        this.parallelProcessor.eval(process.context, shapeManager);
        if (Logger.debugging)
          Logger.debug("Process " + process.processName + " complete");
      }
    } catch (Exception e) {
       e.printStackTrace();
    } catch (Error er) {
      this.parallelProcessor.clearShapeManager(er);
    } finally {
      synchronized (processLock) {
        --this.parallelProcessor.counter;
        processLock.notifyAll();
      }
    }
  }
}