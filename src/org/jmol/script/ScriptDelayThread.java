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
import org.jmol.viewer.Viewer;

class ScriptDelayThread extends JmolThread {

  public static final int PAUSE_DELAY = -100;
  private int millis;

  /**
   * 
   * @param eval
   * @param vwr
   * @param millis    negative to bypass pop/hold sequence
   */
  public ScriptDelayThread(JmolScriptEvaluator eval, Viewer vwr, int millis) {
    setViewer(vwr, "ScriptDelayThread");
    this.millis = millis;
    setEval(eval);
  }

  private int seconds;
  private boolean doPopPush;
  private boolean isPauseDelay;
  
  @Override
  protected void run1(int mode) throws InterruptedException {
    while (true)
      switch (mode) {
      case INIT:
        int delayMax;
        doPopPush = (millis > 0);
        isPauseDelay = (millis == PAUSE_DELAY);
        if (!doPopPush)
          millis = -millis;
        else if ((delayMax = vwr.getDelayMaximumMs()) > 0 && millis > delayMax)
          millis = delayMax;
        millis -= System.currentTimeMillis() - startTime;
        if (isJS) {
          seconds = 0;
        } else {
          seconds =  millis / 1000;
          millis -= seconds * 1000;
          if (millis <= 0)
            millis = 1;
        }
        if (doPopPush)
          vwr.popHoldRepaint("scriptDelayThread INIT");
        mode = MAIN;
        break;
      case MAIN:
        if (stopped || eval.isStopped()) {
          mode = FINISH;
          break;
        }
        if (!runSleep(seconds-- > 0 ? 1000 : (int) millis, FINISH))
          return;
        if (seconds < 0)
          millis = 0;
        mode = (seconds > 0 || millis > 0 ? MAIN : FINISH);
        break;
      case FINISH:
        if (doPopPush)
          vwr.pushHoldRepaintWhy("delay FINISH");
        if (isPauseDelay)
          eval.notifyResumeStatus();
        resumeEval();
        return;
      }
  }
}