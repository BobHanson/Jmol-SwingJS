/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-05-19 08:25:14 -0500 (Wed, 19 May 2010) $
 * $Revision: 13133 $
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

package org.openscience.jmol.app.jsonkiosk;

import java.util.Map;

import org.jmol.viewer.Viewer;

import javajs.util.JSJSONParser;

/**
 * a client of a JsonNioService -- just needs notices of the service shutting
 * down or indicating that a banner needs to be changed.
 * 
 */
public interface JsonNioClient {

  static class TouchHandler {
    public final static float swipeCutoff = 100;
    public final static int swipeCount = 2;
    public final static float swipeDelayMs = 3000;
    public final static float swipeFactor = 30;

    public int nFast;
    public long previousMoveTime;
    public long swipeStartTime;
    public long latestMoveTime;
    public boolean wasSpinOn;
    public boolean isPaused;

    public void pauseScript(Viewer vwr, boolean isPause) {
      String script;
      if (isPause) {
        // Pause the script and save the state when interaction starts
        wasSpinOn = vwr.getBooleanProperty("spinOn");
        script = "pause; save orientation 'JsonNios-save'; spin off";
        isPaused = true;
      } else {
        script = "restore orientation 'JsonNios-save' 1; resume; spin "
            + wasSpinOn;
        wasSpinOn = false;
      }
      isPaused = isPause;
      vwr.evalString(script);
    }

    public void checkPaused(Viewer vwr) {
      long now = System.currentTimeMillis();
      // No commands for 5 seconds = unpause/restore Jmol
      if (isPaused && now - latestMoveTime > 5000)
        pauseScript(vwr, false);
    }

  }

  void nioClosed(JsonNioServer jsonNioService);

  void processNioMessage(byte[] packet) throws Exception;

  void serverCycle();

}
