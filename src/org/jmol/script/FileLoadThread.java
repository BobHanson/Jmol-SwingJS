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

import org.jmol.api.JmolScriptEvaluator;
import org.jmol.api.JmolToJSmolInterface;
import org.jmol.thread.JmolThread;
import org.jmol.viewer.Viewer;

class FileLoadThread extends JmolThread {

  String fileName;
  private String cacheName;
  private String key;

  /**
   * JavaScript only
   * 
   * @param eval
   * @param vwr
   * @param fileName
   * @param key 
   * @param cacheName 
   * 
   * @j2sIgnoreSuperConstructor
   * 
   */
  public FileLoadThread(JmolScriptEvaluator eval, Viewer vwr, String fileName, String key, String cacheName) {
    setViewer(vwr, "FileLoadThread");
    this.fileName = fileName;
    this.key = key;
    this.cacheName = cacheName;
    setEval(eval);
    sc.pc--; // re-start this load command.
  }
  
  @SuppressWarnings({ "null", "unused" })
  @Override
  protected void run1(int mode) throws InterruptedException {
    while (true)
      switch (mode) {
      case INIT: 
        mode = MAIN;
        break;
      case MAIN:
        if (stopped || !vwr.testAsync && eval.isStopped()) {
          mode = FINISH;
          break;
        }
        JmolToJSmolInterface jmol = null;
        /**
         * @j2sNative
         * 
         * jmol = Jmol;
         * 
         */
        {}
        if (jmol != null)
           jmol._loadFileAsynchronously(this, vwr.html5Applet, fileName, null);

        /**
         * @j2sNative
         * 
         */        
        {
          if (vwr.testAsync) {
            if (!runSleep(sleepTime, CHECK1))
              return;
            mode = CHECK1;
            break;
          }
        }
        return;
      case CHECK1:
        // vwr.testAsync only
        Object data = vwr.fm.getFileAsBytes(this.fileName, null);
        setData(this.fileName, this.fileName, data, null);
        return;    
      case FINISH:
        resumeEval();
        return;
      }
  }

  /**
   * Called by Jmol._loadFileAsyncDone(this.vwr.html5Applet). Allows for
   * callback to set the file name.
   * 
   * @param fileName
   * @param fileName0
   * @param data
   * @param myData
   *        unused in Jmol
   * @throws InterruptedException
   */
  void setData(String fileName, String fileName0, Object data, Object myData)
      throws InterruptedException {
    //System.out.println("FileLoadThread async setData " + fileName);
    boolean isCanceled = fileName.equals("#CANCELED#");
    sc.parentContext.htFileCache.put(key, (isCanceled ? fileName
        : (cacheName = cacheName.substring(0, cacheName.lastIndexOf("_") + 1)
            + fileName)));
    vwr.cachePut(cacheName, data);
    if (fileName0 != null) {
      vwr.cachePut(vwr.fm.getFilePath(fileName, true, false), data);
    }
    run1(FINISH);
  }
  
}