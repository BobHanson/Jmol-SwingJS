/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-26 23:35:44 -0500 (Fri, 26 Jun 2009) $
 * $Revision: 11131 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
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
package org.openscience.jmol.app.status;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Map;

import org.jmol.api.JmolAppConsoleInterface;
import org.jmol.api.JmolStatusListener;
import org.jmol.api.JmolSyncInterface;
import org.jmol.c.CBK;
import org.jmol.util.Logger;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.jmolpanel.DisplayPanel;
import org.openscience.jmol.app.jmolpanel.JmolPanel;
import org.openscience.jmol.app.jmolpanel.console.AppConsole;

import javajs.util.PT;

public class StatusListener implements JmolStatusListener, JmolSyncInterface {

  /*
   * starting with Jmol 11.7.27, JmolStatusListener extends JmolCallbackListener
   * 
   * providing a simpler interface if all that is wanted is callback
   * functionality.
   * 
   * Only three methods are involved:
   * 
   * boolean notifyEnabled(int type) -- lets the statusManager know if there is
   * an implementation of a given callback type
   * 
   * void notifyCallback(int type, Object[] data) -- callback action; data
   * varies with callback type -- see org.jmol.viewer.StatusManager for details
   * 
   * void setCallbackFunction(String callbackType, String callbackFunction) --
   * called by statusManager in response to the "set callback" script command --
   * also used by the Jmol application to change menus and languages -- can
   * remain unimplemented if no such user action is intended
   */

  private JmolPanel jmolPanel;
  private DisplayPanel display;

  private Viewer vwr;

  JmolStatusListener userStatusListener;

  public StatusListener(JmolPanel jmolPanel, DisplayPanel display) {
    this(null, jmolPanel, display);
  }

  public StatusListener(Viewer vwr, JmolPanel jmolPanel, DisplayPanel display) {
    this.vwr = (vwr == null ? jmolPanel.vwr : vwr);
    // just required for Jmol application's particular callbacks
    this.jmolPanel = jmolPanel;
    this.display = display;
  }

  // / JmolCallbackListener interface ///
  @Override
  public boolean notifyEnabled(CBK type) {
    if (userStatusListener != null && userStatusListener.notifyEnabled(type))
      return true;
    switch (type) {
    case ATOMMOVED:
    case LOADSTRUCT:
    case SELECT:
    case CALCULATION:
    case STRUCTUREMODIFIED:
    case SYNC:
      return true;
    case ANIMFRAME:
    case MEASURE:
    case SERVICE:
    case PICK:
    case SCRIPT:
      // enabled only for SYNC 
    case ECHO:
    case ERROR:
    case MESSAGE:
    case MINIMIZATION:
    case MODELKIT:
    case DRAGDROP:
    case RESIZE:
    case CLICK:
    case HOVER:
      return (vwr != null && vwr.haveDisplay);
    case APPLETREADY:
    case AUDIO:
    case EVAL:
    case IMAGE:
      // applet only (but you could change this for your listener)
      break;
    }
    return false;
  }

  @Override
  public void notifyCallback(CBK type, Object[] data) {
    if (vwr == null) {
      // during initialization
      return;
    }
    if (userStatusListener != null && userStatusListener.notifyEnabled(type))
      userStatusListener.notifyCallback(type, data);
    String strInfo = (data == null || data[1] == null ? null
        : data[1].toString());
    if (type == CBK.SYNC && JC.SYNC_GET_PREFERENCE.equals(strInfo)) {
      if (jmolPanel != null)
        jmolPanel.notifyPreferences(data);
      return;
    }
    if (vwr.haveDisplay) {
      jmolPanel.notifyServer(type, data);
    }
    switch (type) {
    case MESSAGE:
      // deprecated
      return;
    case SERVICE:
      if (display == null)
        return;
      jmolPanel.initService(data);
      return;
    case LOADSTRUCT:
      notifyFileLoaded(strInfo, (String) data[2], (String) data[3],
          (String) data[4], (Boolean) data[8]);
      break;
    case ANIMFRAME:
      int[] iData = (int[]) data[1];
      strInfo = PT.toJSON(null, iData);
      int modelIndex = iData[0];
      if (modelIndex <= -2)
        modelIndex = -2 - modelIndex; // animation is running
      //int file = iData[1];
      //int model = iData[2];
      if (vwr.haveDisplay && !vwr.am.splitFrame) {
        String menuName = (String) data[2];
        if (menuName.equals("0.0: "))
          menuName = "";
        jmolPanel.notifyMenu(menuName);
      }
      break;
    case SCRIPT:
      int msWalltime = ((Integer) data[3]).intValue();
      if (msWalltime == 0) {
        if (data[2] != null && vwr.haveDisplay) {
          jmolPanel.setStatus(1, (String) data[2]);
        }
      } else if (msWalltime == -1) {
        AppConsole console = jmolPanel.getConsole();
        if (console != null)
          console.checkUndoEnabled();
      }
      break;
    case MODELKIT:
      String state = (String) data[1];
      if (state.equals("ON")) {
        if (display.buttonModelkit != null)
          display.buttonModelkit.setSelected(true);
      } else {
        if (display.buttonRotate != null)
          display.buttonRotate.setSelected(true);
      }
      break;
    case MEASURE:
      if (jmolPanel != null)
        switch (jmolPanel.notifyMeasure(data)) {
        case CLICK:
          return;
        case PICK:
          notifyAtomPicked(strInfo);
          break;
        default:
          break;
        }
      break;
    case PICK:
      notifyAtomPicked(strInfo);
      break;
    case STRUCTUREMODIFIED:
      // JSpecView will use this, as well as VARNA
      break;
    case SYNC:
      //System.out.println("StatusListener sync; " + strInfo);
      if (jmolPanel == null)
        return;
      if (strInfo != null) {
        Boolean ok = jmolPanel.notifyPluginCommand(CBK.SYNC, data);
        if (ok != null)
         return;
      }
      
      jmolPanel.sendNioSyncRequest(null, ((Integer) data[3]).intValue(),
          strInfo);
      return;
    case AUDIO:
    case IMAGE:
    case EVAL:
    case APPLETREADY:
      // see above -- not implemented in Jmol.jar
      return;
    // passed on to listener
    case CALCULATION:
    case HOVER:
    case ATOMMOVED:
    case DRAGDROP:
    case RESIZE:
    case CLICK:
    case ERROR:
    case ECHO:
    case MINIMIZATION:
    case SELECT:
      break;
    }
    if (jmolPanel != null)
      jmolPanel.notifyGeneralCallback(type, data, strInfo);
  }

  @Override
  public void setCallbackFunction(String callbackType,
                                  String callbackFunction) {
    //if (callbackType.equalsIgnoreCase("menu")) {
    //jmol.setupNewFrame(vi/ewer);
    //return;
    //}
    if (callbackType.equalsIgnoreCase("language")) {
      jmolPanel.notifyLanguage();
      return;
    }
  }

  // / end of JmolCallbackListener interface ///

  @Override
  public String eval(String strEval) {
    String msg = "# this funcationality is implemented only for the applet.\n"
        + strEval;
    sendConsoleMessage(msg);
    return msg;
  }

  /**
   * 
   * @param fileName
   * @param type
   * @param text_or_bytes
   * @param quality
   * @return null ("you do it" or canceled) or a message starting with OK or an
   *         error message
   */
  @Override
  public String createImage(String fileName, String type, Object text_or_bytes,
                            int quality) {
    return null;
  }

  private void notifyAtomPicked(String info) {
    if (vwr.haveDisplay)
      jmolPanel.setStatus(1, info);
  }

  private void notifyFileLoaded(String fullPathName, String fileName,
                                String modelName, String errorMsg,
                                Boolean isAsync) {
    if (errorMsg != null) {
      return;
    }
    if (!vwr.haveDisplay)
      return;
    //System.out.println("StatusListener notifyFileLoaded: " + fileName);
    // this code presumes only ptLoad = -1 (error), 0 (zap), or 3 (completed)
    String title = "Jmol";
    if (fileName != null && fileName.startsWith("DROP_"))
      fileName = fileName.substring(5);
    if (modelName != null && fileName != null)
      title = (fileName.contains("&") ? "" : fileName + " - ") + modelName;
    else if (fileName != null)
      title = fileName;
    else if (modelName != null)
      title = modelName;
    jmolPanel.notifyFileOpen(fullPathName == null ? null
        : fullPathName + (isAsync == Boolean.TRUE ? " (*)" : ""), title);
  }

  private void sendConsoleMessage(String strStatus) {
    JmolAppConsoleInterface appConsole = (JmolAppConsoleInterface) vwr
        .getProperty("DATA_API", "getAppConsole", null);
    if (appConsole != null)
      appConsole.sendConsoleMessage(strStatus);
  }

  @Override
  public void showUrl(String url) {
    try {
      Class<?> c = Class.forName("java.awt.Desktop");
      Method getDesktop = c.getMethod("getDesktop", new Class[] {});
      Object deskTop = getDesktop.invoke(null, new Object[] {});
      Method browse = c.getMethod("browse", new Class[] { URI.class });
      Object arguments[] = { new URI(url) };
      browse.invoke(deskTop, arguments);
    } catch (Exception e) {
      Logger.error(e.getMessage());
      JmolAppConsoleInterface appConsole = (JmolAppConsoleInterface) vwr
          .getProperty("DATA_API", "getAppConsole", null);
      if (appConsole != null) {
        appConsole.sendConsoleMessage(
            "Java 6 Desktop.browse() capability unavailable. Could not open "
                + url);
      } else {
        Logger.error(
            "Java 6 Desktop.browse() capability unavailable. Could not open "
                + url);
      }
    }
  }

  @Override
  public Map<String, Object> getRegistryInfo() {
    return null;
  }

  @Override
  public int[] resizeInnerPanel(String data) {
    return jmolPanel.resizeInnerPanel(data);
  }

  @Override
  public void register(String id, JmolSyncInterface jsi) {
    // this would be a call from JSpecView requesting that Jmol 
    // register the JSpecView applet in the JmolAppletRegistry. 
  }

  @Override
  public void syncScript(String script) {
    // called from JSpecView to send "Select: <Peaks...." script
    vwr.syncScript(script, "~", 0);
  }

  @Override
  public Object processPluginRequest(String name, String action, Object value) {
    return (jmolPanel == null ? null
        : jmolPanel.processPluginRequest(name, action, value));
  }

  public void setUserStatusListener(JmolStatusListener userStatusListener) {
    this.userStatusListener = userStatusListener;
  }

  /**
   * this is just a test method for isosurface FUNCTIONXY
   * 
   * @param functionName
   * @param nX
   * @param nY
   * @return f(x,y) as a 2D array
   * 
   */
  @Override
  public double[][] functionXY(String functionName, int nX, int nY) {
    nX = Math.abs(nX);
    nY = Math.abs(nY);
    double[][] f = new double[nX][nY];
    // boolean isSecond = (functionName.indexOf("2") >= 0);
    for (int i = nX; --i >= 0;)
      for (int j = nY; --j >= 0;) {
        double x = i / 5d; // / 15d - 1;
        double y = j / 5d; // / 15d - 1;
        f[i][j] = /* (double) Math.sqrt */(x * x + y);
        if (Double.isNaN(f[i][j]))
          f[i][j] = -Math.sqrt(-x * x - y);
        // f[i][j] = (isSecond ? (double) ((i + j - nX) / (2d)) : (double) Math
        // .sqrt(Math.abs(i * i + j * j)) / 2d);
        // if (i < 10 && j < 10)
        //System.out.println(" functionXY " + i + " " + j + " " + f[i][j]);
      }

    return f; // for user-defined isosurface functions (testing only -- bob
              // hanson)
  }

  @Override
  public double[][][] functionXYZ(String functionName, int nX, int nY, int nZ) {
    nX = Math.abs(nX);
    nY = Math.abs(nY);
    nZ = Math.abs(nZ);
    double[][][] f = new double[nX][nY][nZ];
    for (int i = nX; --i >= 0;)
      for (int j = nY; --j >= 0;)
        for (int k = nZ; --k >= 0;) {
          double x = i / ((nX - 1) / 2d) - 1;
          double y = j / ((nY - 1) / 2d) - 1;
          double z = k / ((nZ - 1) / 2d) - 1;
          f[i][j][k] = x * x + y * y - z * z;//(double) x * x + y - z * z;
          // if (i == 22 || i == 23)
          //System.out.println(" functionXYZ " + i + " " + j + " " + k + " " +
          // f[i][j][k]);
        }
    return f; // for user-defined isosurface functions (testing only -- bob
              // hanson)
  }


}
