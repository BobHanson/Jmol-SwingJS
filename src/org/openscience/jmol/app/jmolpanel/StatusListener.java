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
package org.openscience.jmol.app.jmolpanel;

import java.awt.Component;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javajs.util.PT;
import jspecview.application.MainFrame;

import org.jmol.api.JSVInterface;
import org.jmol.api.JmolAppConsoleInterface;
import org.jmol.api.JmolCallbackListener;
import org.jmol.api.JmolStatusListener;
import org.jmol.api.JmolSyncInterface;
import org.jmol.c.CBK;
import org.jmol.dialog.Dialog;
import org.jmol.script.T;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.JmolPlugin;
import org.openscience.jmol.app.jmolpanel.console.AppConsole;
import org.openscience.jmol.app.webexport.WebExport;

class StatusListener implements JmolStatusListener, JmolSyncInterface, JSVInterface {

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

  private JmolPanel jmol;
  private DisplayPanel display;

  private Viewer vwr;
  private MainFrame jSpecViewFrame;
  private boolean jSpecViewForceNew;
  void setViewer(Viewer vwr) {
    this.vwr = vwr;
  }
  
  StatusListener(JmolPanel jmol, DisplayPanel display) {
    // just required for Jmol application's particular callbacks
    this.jmol = jmol;
    this.display = display;  
  }
  
  // / JmolCallbackListener interface ///
  @Override
  public boolean notifyEnabled(CBK type) {
    switch (type) {
    case ANIMFRAME:
    case ECHO:
    case IMAGE:
    case LOADSTRUCT:
    case STRUCTUREMODIFIED:
    case MEASURE:
    case MESSAGE:
    case SERVICE:
    case PICK:
    case SCRIPT:
    case SYNC:
      return true;
    case AUDIO:
    case EVAL:
    case ATOMMOVED:
    case CLICK:
    case DRAGDROP:
    case ERROR:
    case HOVER:
    case MINIMIZATION:
    case RESIZE:
    case APPLETREADY:
      // applet only (but you could change this for your listener)
      break;
    }
    return false;
  }

  private Map<String, Object> nboOptions;
  
  @SuppressWarnings("unchecked")
  @Override
  public void notifyCallback(CBK type, Object[] data) {
    if (!jmol.plugins.isEmpty())
      for (JmolPlugin p : jmol.plugins.values())
        p.notifyCallback(type, data);
    String strInfo = (data == null || data[1] == null ? null : data[1]
        .toString());
    Map<String, Object> info;
    switch (type) {
    case LOADSTRUCT:
      notifyFileLoaded(strInfo, (String) data[2], (String) data[3],
          (String) data[4], (Boolean) data[8]);
      if (jmol.gaussianDialog != null)
        jmol.gaussianDialog.updateModel(-2);
      return;
    case ANIMFRAME:
      int[] iData = (int[]) data[1];
      int modelIndex = iData[0];
      if (modelIndex <= -2)
        modelIndex = -2 - modelIndex; // animation is running
      //int file = iData[1];
      //int model = iData[2];
      if (display.haveDisplay) {
        String menuName = (String) data[2];
        if (menuName.equals("0.0: "))
          menuName = "";
        display.status.setStatus(StatusBar.STATUS_COORD, menuName);
        if (jmol.frame != null) {
          //Font f = jmol.frame.getFont();
          //if (f != null) {
          //int m = jmol.frame.getFontMetrics(f).stringWidth("M");
          //int n = jmol.frame.getWidth() / m;
          //if (n < menuName.length())
          //menuName = menuName.substring(0, n) + "...";
          //}
          jmol.frame.setTitle(menuName);
        }
        //        if (jSpecViewFrame != null)
        //          setJSpecView("", true);
      }
      return;
    case SCRIPT:
      int msWalltime = ((Integer) data[3]).intValue();
      if (msWalltime == 0) {
        if (data[2] != null && display.haveDisplay)
          display.status.setStatus(StatusBar.STATUS_COORD, (String) data[2]);
      }
      return;
    case ECHO:
      break;
    case MEASURE:
      String mystatus = (String) data[3];
      if (mystatus.indexOf("Sequence") < 0) {
        if (mystatus.indexOf("Pending") < 0 && display.haveDisplay)
          display.measurementTable.updateTables();
        if (mystatus.indexOf("Picked") >= 0) // picking mode
          notifyAtomPicked(strInfo);
        else if (mystatus.indexOf("Completed") < 0)
          return;
      }
      break;
    case MESSAGE:
      break;
    case SERVICE:
      if (display == null)
        return;
      info = (Map<String, Object>) data[1];
      try {
        String service = (String) info.get("service");
        if ("nbo".equals(service)) {
          if ("showPanel".equals(info.get("action")))
            jmol.startNBO(info);
          //else
          //jmol.getNBOService().processRequest(info, 0);
        }
      } catch (Exception e) {
        // ignore
      }
      return;
    case PICK:
      notifyAtomPicked(strInfo);
      if (jmol.gaussianDialog != null)
        jmol.gaussianDialog.updateModel(((Integer) data[2]).intValue());
      break;
    case STRUCTUREMODIFIED:
      // 0 DONE; 1 in process
      int mode = ((Integer) data[1]).intValue();
      int atomIndex = ((Integer) data[2]).intValue();
      int modelIndexx = ((Integer) data[3]).intValue();
      notifyStructureModified(atomIndex, modelIndexx, mode);
      if (jmol.gaussianDialog != null)
        jmol.gaussianDialog.updateModel(-1);
      break;
    case SYNC:
      //System.out.println("StatusListener sync; " + strInfo);
      String lc = (strInfo == null ? "" : strInfo.toLowerCase());
      if (lc.startsWith("jspecview")) {
        setJSpecView(strInfo.substring(9).trim(), false, false);
        return;
      }
      if (lc.equals("getpreference")) {
        data[0] = (data[2] == null ? jmol.preferencesDialog : jmol
            .getPreference(data[2].toString()));
        return;
      }
      if (strInfo != null && strInfo.toLowerCase().startsWith("nbo:")) {
        if (nboOptions == null)
          nboOptions= new Hashtable<String, Object>();
        nboOptions.put("options", strInfo);
        jmol.startNBO(nboOptions);
        return;
      }
      jmol.sendNioMessage(((Integer) data[3]).intValue(), strInfo);
      return;
    case AUDIO:
    case DRAGDROP:
    case ERROR:
    case HOVER:
    case IMAGE:
    case MINIMIZATION:
    case RESIZE:
    case EVAL:
    case ATOMMOVED:
    case CLICK:
    case APPLETREADY:
      // see above -- not implemented in Jmol.jar
      return;
    }
    // cases that fail to return are sent to the console for processing
    if (jmol.service != null)
      jmol.service.scriptCallback(strInfo);
    JmolCallbackListener appConsole = (JmolCallbackListener) vwr.getProperty(
        "DATA_API", "getAppConsole", null);
    if (appConsole != null)
      appConsole.notifyCallback(type, data);
  }

  /**
   * @param atomIndex  
   * @param modelIndex 
   * @param mode 
   */
  private void notifyStructureModified(int atomIndex, int modelIndex, int mode) {
    modificationMode = mode;
    if (mode < 0) {
      switch (mode) {
      case -1: // assign atom
      case -2: // assign bond
      case -3: // connect atoms
      case -4: // delete atoms
      case -5: // delete models
        checkJSpecView(false);
        return;
      }
    }
  }

  @Override
  public void setCallbackFunction(String callbackType, String callbackFunction) {
    if (callbackType.equals("modelkit")) {
      if (callbackFunction.equals("ON"))
        display.buttonModelkit.setSelected(true);
      else
        display.buttonRotate.setSelected(true);
      return;
    }
    //if (callbackType.equalsIgnoreCase("menu")) {
      //jmol.setupNewFrame(vi/ewer);
      //return;
    //}
    if (callbackType.equalsIgnoreCase("language")) {
      JmolResourceHandler.clear();
      Dialog.setupUIManager();
      if (jmol.webExport != null) {
        WebExport.saveHistory();
        WebExport.dispose();
        jmol.createWebExport();
      }
      AppConsole appConsole = (AppConsole) vwr.getProperty("DATA_API",
          "getAppConsole", null);
      if (appConsole != null)
        appConsole.sendConsoleEcho(null);
      jmol.updateLabels();
      return;
    }
  }

  // / end of JmolCallbackListener interface ///

  @Override
  public String eval(String strEval) {
   String msg = "# this funcationality is implemented only for the applet.\n" + strEval;
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
    if (display.haveDisplay)
      display.status.setStatus(StatusBar.STATUS_COORD, info);
  }

  private void notifyFileLoaded(String fullPathName, String fileName,
                                String modelName, String errorMsg, Boolean isAsync) {
    if (errorMsg != null) {
      return;
    }
    if (!display.haveDisplay)
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
    jmol.notifyFileOpen(fullPathName == null ? null : fullPathName + (isAsync == Boolean.TRUE ? " (*)" : ""), title);
    checkJSpecView(fullPathName == null);
  }

  private int modificationMode;
  
  private void sendConsoleMessage(String strStatus) {
    JmolAppConsoleInterface appConsole = (JmolAppConsoleInterface) vwr
        .getProperty("DATA_API", "getAppConsole", null);
    if (appConsole != null)
      appConsole.sendConsoleMessage(strStatus);
  }

  @Override
  public void showUrl(String url) {
    /**
     * @j2sNative
     * 
     * open(url);
     * 
     */
    {
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
        appConsole
            .sendConsoleMessage("Java 6 Desktop.browse() capability unavailable. Could not open "
                + url);
      } else {
        Logger
            .error("Java 6 Desktop.browse() capability unavailable. Could not open "
                + url);
      }
    }
    }
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
  public float[][] functionXY(String functionName, int nX, int nY) {
    nX = Math.abs(nX);
    nY = Math.abs(nY);
    float[][] f = new float[nX][nY];
    // boolean isSecond = (functionName.indexOf("2") >= 0);
    for (int i = nX; --i >= 0;)
      for (int j = nY; --j >= 0;) {
        float x = i / 5f; // / 15f - 1;
        float y = j / 5f; // / 15f - 1;
        f[i][j] = /* (float) Math.sqrt */(x * x + y);
        if (Float.isNaN(f[i][j]))
          f[i][j] = -(float) Math.sqrt(-x * x - y);
        // f[i][j] = (isSecond ? (float) ((i + j - nX) / (2f)) : (float) Math
        // .sqrt(Math.abs(i * i + j * j)) / 2f);
        // if (i < 10 && j < 10)
        //System.out.println(" functionXY " + i + " " + j + " " + f[i][j]);
      }

    return f; // for user-defined isosurface functions (testing only -- bob
              // hanson)
  }

  @Override
  public float[][][] functionXYZ(String functionName, int nX, int nY, int nZ) {
    nX = Math.abs(nX);
    nY = Math.abs(nY);
    nZ = Math.abs(nZ);
    float[][][] f = new float[nX][nY][nZ];
    for (int i = nX; --i >= 0;)
      for (int j = nY; --j >= 0;)
        for (int k = nZ; --k >= 0;) {
          float x = i / ((nX - 1) / 2f) - 1;
          float y = j / ((nY - 1) / 2f) - 1;
          float z = k / ((nZ - 1) / 2f) - 1;
          f[i][j][k] = x * x + y * y - z * z;//(float) x * x + y - z * z;
          // if (i == 22 || i == 23)
          //System.out.println(" functionXYZ " + i + " " + j + " " + k + " " +
          // f[i][j][k]);
        }
    return f; // for user-defined isosurface functions (testing only -- bob
              // hanson)
  }

  @Override
  public Map<String, Object> getRegistryInfo() {
    return null;
  }

  @Override
  public int[] resizeInnerPanel(String data) {
    return jmol.resizeInnerPanel(data);
  }

  private String lastSimulate;
  
  private void checkJSpecView(boolean closeAll) {
    if (jSpecViewFrame != null && modificationMode <= 0) {
      jSpecViewForceNew = jSpecViewFrame.isVisible();
      setJSpecView(closeAll ? "none" : "", true, true);
      jSpecViewForceNew = true;
    }
  }

  public void setJSpecView(String peaks, boolean doLoadCheck, boolean isFileLoad) {
    if (peaks.startsWith(":"))
      peaks = peaks.substring(1);
    if (peaks.equals("none") || peaks.equals("NONESimulate:")) {
      if (jSpecViewFrame != null) {
        jSpecViewFrame.syncScript("close ALL");
        jSpecViewFrame.awaken(false);
      }
      return;
    }
    boolean isC13 = peaks.equals("C13Simulate:");
    boolean isSimulation = (peaks.equals("H1Simulate:") || isC13);
    boolean isStartup = (peaks.length() == 0 || isSimulation);
    boolean newSim = (isSimulation && !peaks.equals(lastSimulate));
    String data = null;
    if (isSimulation) {
      data = vwr.extractMolData(null);
      if (data == null || data.length() == 0)
        return;
    }
    if (jSpecViewFrame == null) {
      jSpecViewFrame = new MainFrame(vwr.getBoolean(T.jmolinjspecview) ? (Component) vwr.display : null, this);
      jSpecViewFrame.setSize(Math.max(1000, jmol.frame.getWidth() + 50), 600);
      jSpecViewFrame.setLocation(jmol.frame.getLocation().x + 10, jmol.frame
          .getLocation().y + 100);
      jSpecViewFrame.register("Jmol", this);
      vwr.setBooleanProperty("_jspecview", true);
      if (isStartup) {
        doLoadCheck = true;
      }
    }
    if (doLoadCheck || jSpecViewForceNew || newSim) {
      String type = "" + vwr.getP("_modelType");
      if (type.equalsIgnoreCase("jcampdx")) {
        jSpecViewForceNew = false;
        String file = "" + vwr.getP("_modelFile");
        if (file.indexOf("/") < 0)
          return;
        peaks = "hidden true; load CHECK " + PT.esc(file) + ";hidden false" + (newSim && isC13 ? ";scaleby 0.5" : null);
      } else if (isFileLoad && !jSpecViewForceNew && !newSim) {
        return;
      } else {
        jSpecViewForceNew = false;
        if (newSim)
          lastSimulate = peaks;
        String model = "" + vwr.getP("_modelNumber");
        if (data == null) {
          peaks = "hidden false";
        } else {
          data = PT.replaceAllCharacters(data, "&", "_");
          peaks = "hidden true; load CHECK " + (peaks.equals("H1Simulate:") ? "H1 " : "C13 ")
              + PT.esc("id='~" + model + "';" + data) + ";hidden false #SYNC_PEAKS";
        }
        isStartup = false;
      }
    }

    if (!jSpecViewFrame.isVisible()) {
      if (peaks.contains("<PeakData"))
        return;
      jSpecViewFrame.awaken(true);
      display.setViewer(vwr);
    }
    if (isStartup)
      peaks = "HIDDEN false";
    jSpecViewFrame.syncScript(peaks);
  }

  @Override
  public void register(String id, JmolSyncInterface jsi) {
    // this would be a call from JSpecView requesting that Jmol 
    // register the JSpecView applet in the JmolAppletRegistry. 
  }

  @Override
  public void syncScript(String script) {
    // called from JSpecView to send "Select: <Peaks...." script
    jmol.syncScript(script);    
  }

  
  // -- JSVInterface -- 
  
  private static String propertiesFileName = "jspecview.properties";

  @Override
  public void setProperties(Properties properties) {
    try {
      FileInputStream fileIn = new FileInputStream(propertiesFileName);
      properties.load(fileIn);
    } catch (Exception e) {
    }
  }

  @Override
  public void saveProperties(Properties properties) {
    // Write out current properties
    try {
      FileOutputStream fileOut = new FileOutputStream(propertiesFileName);
      properties.store(fileOut, "JSpecView Application Properties");
    } catch (Exception e) {
    }
  }

  /**
   * @param withDialog  
   * @param frame 
   */
  @Override
  public void exitJSpecView(boolean withDialog, Object frame) {
    // no exit from Jmol
  }

  /**
   * no queuing here -- called by MainFrame
   * 
   * @param script 
   */
  @Override
  public void runScript(String script) {
    jSpecViewFrame.runScriptNow(script);
    
  }

  /**
   * @param msg
   */
  @Override
  public void syncToJmol(String msg) {
    // not utilized in Jmol application -- jmolSyncInterface used instead
  }

  @Override
  public Map<String, Object> getJSpecViewProperty(String type) {
    if (type.toLowerCase().startsWith("jspecview")) {
      type = type.substring(9);
      if (type.startsWith(":"))
          type = type.substring(1);
      return (jSpecViewFrame == null ? null : jSpecViewFrame.getJSpecViewProperty(type));
    }
    return null;
  }
  
}
