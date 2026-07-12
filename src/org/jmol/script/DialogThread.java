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
import org.jmol.thread.JmolThread;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

class DialogThread extends JmolThread {

  private static boolean j2sPromptLoaded;
  static {
    if (!j2sPromptLoaded) {
      j2sPromptLoaded = true;
      /**
       * one-time addition of Jmol.promptAsynchronously for SwingJS only
       * 
       * @j2sNative
       * 

  Jmol.promptDialog = null;
  Jmol.promptAsynchronously = function(dialogThread, applet, msg, btns) {
    if (!Jmol.promptDialog) {
      Jmol.$after("body", Jmol.dialogHTML || (Jmol.dialogHTML = '<dialog id="Jmol.dialogDiv"><div id="Jmol.dialogMsgDiv" style="margin-bottom:1em;white-space:pre"></div><form method="dialog" id="Jmol.dialogBtns"></form></dialog>'));
      Jmol.promptDialog = document.getElementById("Jmol.dialogDiv");
    }
    if (!dialogThread) return;
    Jmol.promptDialog.returnValue = null;
    Jmol.promptDialog.addEventListener("close", function closeMe() {
      Jmol.promptDialog.removeEventListener("close", closeMe);
      var v = Jmol.promptDialog.returnValue;
      dialogThread.setData$S(v == "null" ? null : v);
    });
    document.getElementById("Jmol.dialogMsgDiv").textContent = msg;
    var btnDiv = document.getElementById("Jmol.dialogBtns");
    btnDiv.innerHTML = "";
    for (var i = 0; i < btns.length; i++) {
      var btn = document.createElement("button");
      btn.setAttribute("value", btns[i]);
      btn.textContent = btns[i];
      btn.style.margin="0 1em";
      btnDiv.appendChild(btn);
    }
    Jmol.promptDialog.showModal();
  }

       */
    }
    
    
  }
  
  String[] options;
  private String label;
  private String key = JC.CACHE_PROTOCOL + JC.CACHE_DIALOG;

  public DialogThread() {
  }

  /**
   * JavaScript only
   * 
   * @param eval
   * @param vwr
   * @param label
   * @param options
   * @return this
   * 
   */
    public DialogThread initialize(JmolScriptEvaluator eval, Viewer vwr, String label, String[] options) {
    setViewer(vwr, "DialogThread");
    this.options = options;
    this.label = label;
    setEval(eval);
    sc.pc--; // re-start this load command.
    return this;
  }
  
  @Override
  protected void run1(int mode) throws InterruptedException {
    while (true)
      switch (mode) {
      case INIT: 
        mode = MAIN;
        break;
      case MAIN:
        if (stopped || eval.isStopped()) {
          mode = FINISH;
          break;
        }
        if (Viewer.jmolObject != null)
           Viewer.jmolObject.promptAsynchronously(this, vwr.html5Applet, label, options);
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
   * @param option
   * @throws InterruptedException
   */
  void setData(String option) throws InterruptedException {
    //System.out.println("FileLoadThread async setData " + fileName);
    boolean isCanceled = option == null || option.equals(JC.ASYNC_CANCELED);
    sc.parentContext.htFileCache.put(key,
        (isCanceled ? JC.ASYNC_CANCELED : option));
    run1(FINISH);
  }
  
}