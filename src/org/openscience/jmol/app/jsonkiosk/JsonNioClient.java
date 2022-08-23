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

import javajs.util.P3d;


/**
 * a client of a JsonNioService -- just needs notices of the service shutting
 * down or indicating that a banner needs to be changed.
 * 
 */
public interface JsonNioClient {

  final static public String TYPES = "reply....." + // 0
      "quit......" + // 10
      "command..." + // 20
      "move......" + // 30
      "rotate...." + // 40
      "translate." + // 50
      "zoom......" + // 60
      "sync......" + // 70
      "touch....." + // 80
      "";

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

    public void syncScript(Viewer vwr, String script) {
      vwr.syncScript(script, "~", 0);
  }

    /**
     * process touch or gesture commands driven by hardware. From
     * MolecularPlayground.
     * 
     * @param vwr
     * @param json
     * @throws Exception
     */
    public void nioSync(Viewer vwr, Map<String, Object> json)
        throws Exception {
      //    "reply....." +  // 0
      //        "quit......" +  // 10
      //        "command..." +  // 20
      //        "move......" +  // 30
      //        "rotate...." +  // 40
      //        "translate." +  // 50
      //        "zoom......" +  // 60
      //        "sync......" +  // 70
      //        "touch....." +  // 80

      switch (TYPES.indexOf(JsonNioService.getString(json, "type"))) {
      case 30://"move":
        long now = latestMoveTime = System.currentTimeMillis();
        switch (TYPES.indexOf(JsonNioService.getString(json, "style"))) {
        case 40://"rotate":
          float dx = (float) JsonNioService.getDouble(json, "x");
          float dy = (float) JsonNioService.getDouble(json, "y");
          float dxdy = dx * dx + dy * dy;
          boolean isFast = (dxdy > TouchHandler.swipeCutoff);
          boolean disallowSpinGesture = vwr.getBooleanProperty("isNavigating")
              || !vwr.getBooleanProperty("allowGestures");
          if (disallowSpinGesture || isFast
              || now - swipeStartTime > TouchHandler.swipeDelayMs) {
            // it's been a while since the last swipe....
            // ... send rotation in all cases
            String msg = null;
            if (disallowSpinGesture) {
              // just rotate
            } else if (isFast) {
              if (++nFast > TouchHandler.swipeCount) {
                // critical number of fast motions reached
                // start spinning
                swipeStartTime = now;
                msg = "Mouse: spinXYBy " + (int) dx + " " + (int) dy + " "
                    + (Math.sqrt(dxdy) * TouchHandler.swipeFactor
                        / (now - previousMoveTime));
              }
            } else if (nFast > 0) {
              // slow movement detected -- turn off spinning
              // and reset the number of fast actions
              nFast = 0;
              msg = "Mouse: spinXYBy 0 0 0";
            }
            if (msg == null)
              msg = "Mouse: rotateXYBy " + dx + " " + dy;
            syncScript(vwr, msg);
          }
          previousMoveTime = now;
          break;
        case 50://"translate":
          if (!isPaused)
            pauseScript(vwr, true);
          syncScript(vwr, "Mouse: translateXYBy " + JsonNioService.getString(json, "x")
              + " " + JsonNioService.getString(json, "y"));
          break;
        case 60://"zoom":
          if (!isPaused)
            pauseScript(vwr, true);
          float zoomFactor = (float) (JsonNioService.getDouble(json, "scale")
              / (vwr.tm.zmPct / 100.0f));
          syncScript(vwr, "Mouse: zoomByFactor " + zoomFactor);
          break;
        }
        break;
      case 70://"sync":
        //sync -3000;sync slave;sync 3000 '{"type":"sync","sync":"rotateZBy 30"}'
        syncScript(vwr, "Mouse: " + JsonNioService.getString(json, "sync"));
        break;
      case 80://"touch":
        // raw touch event
        vwr.acm.processMultitouchEvent(0,
            JsonNioService.getInt(json, "eventType"),
            JsonNioService.getInt(json, "touchID"),
            JsonNioService.getInt(json, "iData"),
            P3d.new3(JsonNioService.getDouble(json, "x"),
                JsonNioService.getDouble(json, "y"),
                JsonNioService.getDouble(json, "z")),
            JsonNioService.getLong(json, "time"));
        break;
      }
    }

  }

  void nioClosed(JsonNioServer jsonNioService);

  void processNioMessage(byte[] packet) throws Exception;

  void serverCycle();

}
