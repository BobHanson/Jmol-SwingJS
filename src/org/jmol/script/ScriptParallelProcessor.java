/* $Author: hansonr $
 * $Date: 2010-04-22 13:16:44 -0500 (Thu, 22 Apr 2010) $
 * $Revision: 12904 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

package org.jmol.script;

import javajs.util.Lst;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jmol.api.JmolParallelProcessor;
import org.jmol.shape.MeshCollection;
import org.jmol.shape.Shape;
import org.jmol.util.Logger;
import org.jmol.viewer.ShapeManager;
import org.jmol.viewer.Viewer;

public class ScriptParallelProcessor extends ScriptFunction implements JmolParallelProcessor {

  /**
   * parallel operations
   * 
   */
  
  public ScriptParallelProcessor() {
    // for reflection
  }
  
  @Override
  public Object getExecutor() {
    return Executors.newCachedThreadPool();
  }
  
  Viewer vwr;
  public volatile int counter = 0;
  public volatile Error error = null;
  Object lock = new Object() ;
  
  @Override
  public void runAllProcesses(Viewer vwr) {
    if (processes.size() == 0)
      return;
    this.vwr = vwr;
    boolean inParallel = !vwr.isParallel() && vwr.setParallel(true);
    Lst<ShapeManager> vShapeManagers = new  Lst<ShapeManager>();
    error = null;
    counter = 0;
    if (Logger.debugging)
      Logger.debug("running " + processes.size() + " processes on "
          + Viewer.nProcessors + " processesors inParallel=" + inParallel);

    counter = processes.size();
    for (int i = processes.size(); --i >= 0;) {
      ShapeManager sm = null;
      if (inParallel) {
        sm = new ShapeManager(vwr);
        sm.setParallel();
        vShapeManagers.addLast(sm);
      }
      runProcess(processes.removeItemAt(0), sm);
    }

    synchronized (lock) {
      while (counter > 0) {
        try {
          lock.wait();
        } catch (InterruptedException e) {
        }
        if (error != null)
          throw error;
      }
    }
    mergeResults(vShapeManagers);
    vwr.setParallel(false);
  }

  void mergeResults(Lst<ShapeManager> vShapeManagers) {
    try {
      for (int i = 0; i < vShapeManagers.size(); i++)
        mergeShapes(vShapeManagers.get(i));
    } catch (Error e) {
      throw e;
    } finally {
      counter = -1;
      vShapeManagers = null;
    }
  }

  private void mergeShapes(ShapeManager shapeManager) {
    Shape[] newShapes = shapeManager.shapes;
    if (newShapes == null)
      return;
    if (vwr.shm.shapes == null)
      vwr.shm.shapes = newShapes;
    else
      for (int i = 0; i < newShapes.length; ++i)
        if (newShapes[i] != null && newShapes[i] instanceof MeshCollection) {
          if (vwr.shm.shapes[i] == null)
            vwr.shm.loadShape(i);
          ((MeshCollection) vwr.shm.shapes[i]).merge((MeshCollection) newShapes[i]);
        }
  }

  void clearShapeManager(Error er) {
    synchronized (this) {
      this.error = er;
      this.notifyAll();
    }
  }

  private Lst<ScriptProcess> processes = new  Lst<ScriptProcess>();

  @Override
  public void addProcess(String name, ScriptContext context) {
    processes.addLast(new ScriptProcess(name, context));
  }

  private void runProcess(final ScriptProcess process, ShapeManager shapeManager) {
    ScriptProcessRunnable r = new ScriptProcessRunnable(this, process, lock, shapeManager);
    Executor exec = (shapeManager == null ? null : getMyExecutor());
    if (exec != null) {
      exec.execute(r);
    } else {
      r.run();
    }
  }

  void eval(ScriptContext context, ShapeManager shapeManager) {
    vwr.evalParallel(context, shapeManager);
  }


  private Executor getMyExecutor() {
    // a Java 1.5 function
    if (vwr.executor != null || Viewer.nProcessors < 2)
      return (Executor) vwr.executor; // note -- a Java 1.5 function
    try {
      vwr.executor = getExecutor();
    } catch (Exception e) {
      vwr.executor = null;
    } catch (Error er) {
      vwr.executor = null;
    }
    if (vwr.executor == null)
      Logger.error("parallel processing is not available");
    return (Executor) vwr.executor;
  }

  
}
