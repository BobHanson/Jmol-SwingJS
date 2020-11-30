/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2018-01-28 23:38:16 -0600 (Sun, 28 Jan 2018) $
 * $Revision: 21814 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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
package org.jmol.viewer;

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.T3;

import org.jmol.api.GenericImageDialog;
import org.jmol.api.Interface;
import org.jmol.api.JmolAppConsoleInterface;
import org.jmol.api.JmolAudioPlayer;
import org.jmol.api.JmolCallbackListener;
import org.jmol.api.JmolDialogInterface;
import org.jmol.api.JmolStatusListener;
import org.jmol.c.CBK;
import javajs.util.BS;
import org.jmol.script.SV;
import org.jmol.script.T;
import org.jmol.util.JmolAudio;
import org.jmol.util.Logger;

/**
 * 
 * The StatusManager class handles all details of status reporting, including:
 * 
 * 1) saving the message in a queue that replaces the "callback" mechanism,
 * 2) sending messages off to the console, and
 * 3) delivering messages back to the main Jmol.java class in app or applet
 *    to handle differences in capabilities, including true callbacks.

atomPicked

fileLoaded
fileLoadError
frameChanged

measureCompleted
measurePending
measurePicked

newOrientation 

scriptEcho
scriptError
scriptMessage
scriptStarted
scriptStatus
scriptTerminated

userAction
vwrRefreshed

   
 * Bob Hanson hansonr@stolaf.edu  2/2006
 * 
 */

public class StatusManager {

  protected Viewer vwr;
  JmolStatusListener jsl;
  public JmolCallbackListener cbl;
  public String statusList = "";

  StatusManager(Viewer vwr) {
    this.vwr = vwr;
  }

  public boolean allowStatusReporting; // set in StateManager.global
  
  /*
   * the messageQueue provided a mechanism for saving and recalling
   * information about the running of a script. The idea was to poll
   * the applet instead of using callbacks. 
   * 
   * As it turns out, polling of applets is fraught with problems, 
   * not the least of which is the most odd behavior of some 
   * versions of Firefox that makes text entry into the URL line 
   * enter in reverse order of characters during applet polling. 
   * This bug may or may not have been resolved, but in any case,
   * callbacks have proven far more efficient than polling anyway,
   * so this mechanism is probably not particularly generally useful. 
   * 
   * Still, the mechanism is here because in addition to applet polling,
   * it provides a way to get selective information back from the applet
   * to the calling page after a script has run synchronously (using applet.scriptWait). 
   * 
   * The basic idea involves:
   * 
   * 1) Setting what types of messages should be saved
   * 2) Executing the scriptWait(script) call
   * 3) Decoding the return value of that function
   * 
   * Note that this is not meant to be a complete record of the script.
   * Rather, each messsage type is saved in its own Vector, and 
   * only most recent MAX_QUEUE_LENGTH (16) entries are saved at any time.
   * 
   * For example:
   * 
   * 1) jmolGetStatus("scriptEcho,scriptMessage,scriptStatus,scriptError",targetSuffix)
   * 
   * Here we flush the message queue and identify the status types we want to maintain.
   *
   * 2) var ret = "" + applet.scriptWait("background red;echo ok;echo hello;")
   * 
   * The ret variable contains the array of messages in JSON format, which
   * can then be reconstructed as a JavaScript array object. In this case the return is:
   * 
   * 
   * {"jmolStatus": [ 
   *  [ 
   *    [ 3,"scriptEcho",0,"ok" ],
   *    [ 4,"scriptEcho",0,"hello" ] 
   *  ],[ 
   *    [ 1,"scriptStarted",6,"background red;echo ok;echo hello;" ] 
   *  ],[ 
   *    [ 6,"scriptTerminated",1,"Jmol script terminated successfully" ] 
   *  ],[ 
   *    [ 2,"scriptStatus",0,"script 6 started" ],
   *    [ 5,"scriptStatus",0,"Script completed" ],
   *    [ 7,"scriptStatus",0,"Jmol script terminated" ] 
   *  ] 
   *  ]}
   * 
   * Decoded, what we have is a "jmolStatus" JavaScript Array. This array has 4 elements, 
   * our scriptEcho, scriptStarted, scriptTerminated, and scriptStatus messages.
   * 
   * Within each of those elements we have the most recent 16 status messages.
   * Each status record consists of four entries:
   * 
   *   [ statusPtr, statusName, intInfo, strInfo ]
   *  
   * The first entry in each record is the sequential number when that record
   * was recorded, so to reconstruct the sequence of events, simply order the arrays:
   * 
   *    [ 1,"scriptStarted",6,"background red;echo ok;echo hello;" ] 
   *    [ 2,"scriptStatus",0,"script 6 started" ],
   *    [ 3,"scriptEcho",0,"ok" ],
   *    [ 4,"scriptEcho",0,"hello" ] 
   *    [ 5,"scriptStatus",0,"Script completed" ],
   *    [ 6,"scriptTerminated",1,"Jmol script terminated successfully" ] 
   *    [ 7,"scriptStatus",0,"Jmol script terminated" ] 
   *
   * While it could be argued that the presence of the statusName in each record is
   * redundant, and a better structure would be a Hashtable, this is the way it is 
   * implemented here and required for Jmol.js. 
   * 
   * Note that Jmol.js has a set of functions that manipulate this data. 
   * 
   */
  
  public Map<String, Lst<Lst<Object>>> messageQueue = new Hashtable<String, Lst<Lst<Object>>>();
  
  private int statusPtr = 0;
  private static int MAXIMUM_QUEUE_LENGTH = 16;
  
////////////////////Jmol status //////////////

  private boolean recordStatus(String statusName) {
    return (allowStatusReporting && statusList.length() > 0 
        && (statusList.equals("all") || statusList.indexOf(statusName) >= 0));
  }
  
  synchronized private void setStatusChanged(String statusName,
      int intInfo, Object statusInfo, boolean isReplace) {
    if (!recordStatus(statusName))
      return;
    Lst<Object> msgRecord = new Lst<Object>();
    msgRecord.addLast(Integer.valueOf(++statusPtr));
    msgRecord.addLast(statusName);
    msgRecord.addLast(Integer.valueOf(intInfo));
    msgRecord.addLast(statusInfo);
    Lst<Lst<Object>> statusRecordSet = (isReplace ? null : messageQueue.get(statusName));
    if (statusRecordSet == null)
      messageQueue.put(statusName, statusRecordSet = new  Lst<Lst<Object>>());
    else if (statusRecordSet.size() == MAXIMUM_QUEUE_LENGTH)
      statusRecordSet.removeItemAt(0);    
    statusRecordSet.addLast(msgRecord);
  }
  
  synchronized Lst<Lst<Lst<Object>>> getStatusChanged(String newStatusList) {
    /*
     * returns a Vector of statusRecordSets, one per status type,
     * where each statusRecordSet is itself a vector of vectors:
     * [int statusPtr,String statusName,int intInfo, String statusInfo]
     * 
     * This allows selection of just the type desired as well as sorting
     * by time overall.
     * 
     */

    boolean isRemove = (newStatusList.length() > 0 && newStatusList.charAt(0) == '-');
    boolean isAdd = (newStatusList.length() > 0 && newStatusList.charAt(0) == '+');
    boolean getList = false;
    if (isRemove) {
      statusList = PT.rep(statusList,
          newStatusList.substring(1, newStatusList.length()), "");
    } else {
      newStatusList = PT.rep(newStatusList, "+", "");
      if (statusList.equals(newStatusList) || isAdd
          && statusList.indexOf(newStatusList) >= 0) {
        getList = true;
      } else {
        if (!isAdd)
          statusList = "";
        statusList += newStatusList;
        if (Logger.debugging)
          Logger.debug("StatusManager messageQueue = " + statusList);
      }
    }
    Lst<Lst<Lst<Object>>> list = new Lst<Lst<Lst<Object>>>();
    if (getList)
      for (Map.Entry<String, Lst<Lst<Object>>> e : messageQueue.entrySet())
        list.addLast(e.getValue());
    messageQueue.clear();
    statusPtr = 0;
    return list;
  }

  private Map<String, String> jmolScriptCallbacks = new Hashtable<>();

  private String jmolScriptCallback(CBK callback) {
    String s = jmolScriptCallbacks.get(callback.name());
    if (s != null)
      vwr.evalStringQuietSync(s, true, false);
    if (jmolScriptCallbacks.containsKey("SYNC:" + callback.name())) 
      s = "SYNC";
    return s;
  }
  
  synchronized void setCallbackFunction(String callbackType,
                                        String callbackFunction) {
    // menu and language setting also use this route
    CBK cbk = CBK.getCallback(callbackType);
    if (cbk != null) {
      String callback = CBK.getCallback(callbackType).name();
      Logger.info("StatusManager callback set for " + callbackType + " f="
          + callbackFunction + " cb=" + callback);
      boolean isSync = (callbackFunction != null
          && callbackFunction.startsWith("SYNC:"));
      if (isSync) {
        if (callbackFunction.toLowerCase().trim().equals("sync:off")) {
          jmolScriptCallbacks.remove("SYNC:" + callback);
          Logger.info("SYNC callback for " + callback + " deactivated");
        } else {
          jmolScriptCallbacks.put("SYNC:" + callback, "_");
          Logger.info("SYNC callback for " + callback + " activated");
          return;
        }
      } else {
        String lc = "";
        int pt = (callbackFunction == null ? 0
            : (lc = callbackFunction.toLowerCase()).startsWith("script:") ? 7
                : lc.startsWith("jmolscript:") ? 11 : 0);
        if (pt == 0) {
          jmolScriptCallbacks.remove(callback);
        } else {
          jmolScriptCallbacks.put(callback,
              callbackFunction.substring(pt).trim());
        }
      }
    }
    // allow for specialized callbacks
    if (cbl != null)
      cbl.setCallbackFunction(callbackType, callbackFunction);
  }
  
  boolean notifyEnabled(CBK type) {
    return cbl != null && cbl.notifyEnabled(type);
  }

  synchronized void setStatusAppletReady(String htmlName, boolean isReady) {
    String sJmol = (isReady ? jmolScriptCallback(CBK.APPLETREADY) : null);
    if (notifyEnabled(CBK.APPLETREADY))
      cbl.notifyCallback(CBK.APPLETREADY,
          new Object[] { sJmol, htmlName, Boolean.valueOf(isReady), null });
  }

  synchronized void setStatusAtomMoved(BS bsMoved) {
    String sJmol = jmolScriptCallback(CBK.ATOMMOVED);
    setStatusChanged("atomMoved", -1, bsMoved, false);
    if (notifyEnabled(CBK.ATOMMOVED))
      cbl.notifyCallback(CBK.ATOMMOVED,
          new Object[] { sJmol, bsMoved });
  }

  /**
   * 
   * @param atomIndex  -2 for draw, -3 for bond
   * @param strInfo
   * @param map 
   */
  synchronized void setStatusAtomPicked(int atomIndex, String strInfo, Map<String, Object> map) {
    String sJmol = jmolScriptCallback(CBK.PICK);
    Logger.info("setStatusAtomPicked(" + atomIndex + "," + strInfo + ")");
    setStatusChanged("atomPicked", atomIndex, strInfo, false);
    if (notifyEnabled(CBK.PICK))
      cbl.notifyCallback(CBK.PICK,
          new Object[] { sJmol, strInfo, Integer.valueOf(atomIndex), map });
  }

  synchronized int setStatusClicked(int x, int y, int action, int clickCount, int mode) {
    // also called on drag movements
    String sJmol = jmolScriptCallback(CBK.CLICK);
    if (!notifyEnabled(CBK.CLICK))
      return action;
    // allows modification of action
    int[] m = new int[] { action, mode };
    cbl.notifyCallback(CBK.CLICK,
        new Object[] { sJmol, Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(action), Integer.valueOf(clickCount), m });
    return m[0];
  }

  synchronized void setStatusResized(int width, int height){
    String sJmol = jmolScriptCallback(CBK.RESIZE);
    if (notifyEnabled(CBK.RESIZE))
      cbl.notifyCallback(CBK.RESIZE,
          new Object[] { sJmol, Integer.valueOf(width), Integer.valueOf(height) }); 
  }

  boolean haveHoverCallback() {
    return (jmolScriptCallbacks.containsKey(CBK.HOVER) || notifyEnabled(CBK.HOVER));
  }
  
  synchronized void setStatusAtomHovered(int iatom, String strInfo) {
    String sJmol = jmolScriptCallback(CBK.HOVER);
    if (notifyEnabled(CBK.HOVER))
      cbl.notifyCallback(CBK.HOVER, 
          new Object[] {sJmol, strInfo, Integer.valueOf(iatom) });
  }
  
  synchronized void setStatusObjectHovered(String id, String strInfo, T3 pt) {
    String sJmol = jmolScriptCallback(CBK.HOVER);
    if (notifyEnabled(CBK.HOVER))
      cbl.notifyCallback(CBK.HOVER, 
          new Object[] {sJmol, strInfo, Integer.valueOf(-1), id, Float.valueOf(pt.x), Float.valueOf(pt.y), Float.valueOf(pt.z) });
  }
  
  private Map<String, GenericImageDialog> imageMap;

  /**
   * called by Viewer.loadImageData to pop up a window with an image in it
   * 
   * @param title
   * @param image  or Boolean.TRUE for "close all" or Boolean.FALSE for "close"
   */
  synchronized void showImage(String title, Object image) {
    String[] a = PT.split(title,  "\1");
    title = (a.length < 2 ? "Jmol" : a.length < 3 || a[2].equals("null") ? a[1].substring(a[1].lastIndexOf("/") + 1) : a[2]);
    String sJmol = jmolScriptCallback(CBK.IMAGE);
    if (notifyEnabled(CBK.IMAGE))
      cbl.notifyCallback(CBK.IMAGE, new Object[] { sJmol, title, image });
    if (Boolean.TRUE.equals(image)) {
      if (imageMap == null)
        return;
      Lst<String> lst = new Lst<String>();
      for (String key : imageMap.keySet())
        lst.addLast(key);
      for (int i = lst.size(); --i >= 0;)
        imageMap.get(lst.get(i)).closeMe();
      return;
    }
    if (imageMap == null)
      imageMap = new Hashtable<String, GenericImageDialog>();
    GenericImageDialog d = imageMap.get(title);
    if (Boolean.FALSE.equals(image)) {
      if (d != null)
        d.closeMe();
      return;
    }
    if (d == null && image != null)
      d = vwr.apiPlatform.getImageDialog(title, imageMap);
    if (d == null)
      return;
    if (image == null)
      d.closeMe();
    else
      d.setImage(image);
  }
  
  synchronized void setFileLoadStatus(String fullPathName, String fileName,
                                      String modelName, String errorMsg,
                                      int ptLoad, boolean doCallback,
                                      Boolean isAsync) {
    if (fullPathName == null && "resetUndo".equals(fileName)) {
      JmolAppConsoleInterface appConsole = (JmolAppConsoleInterface) vwr
          .getProperty("DATA_API", "getAppConsole", null);
      if (appConsole != null)
        appConsole.zap();
      fileName = vwr.getZapName();
    }
    setStatusChanged("fileLoaded", ptLoad, fullPathName, false);
    if (errorMsg != null)
      setStatusChanged("fileLoadError", ptLoad, errorMsg, false);
    String sJmol = jmolScriptCallback(CBK.LOADSTRUCT);
    if (doCallback && notifyEnabled(CBK.LOADSTRUCT)) {
      String name = (String) vwr.getP("_smilesString");
      if (name.length() != 0)
        fileName = name;
      cbl
          .notifyCallback(CBK.LOADSTRUCT,
              new Object[] { sJmol, fullPathName, fileName, modelName,
                  errorMsg, Integer.valueOf(ptLoad),
                  vwr.getP("_modelNumber"),
                  vwr.getModelNumberDotted(vwr.ms.mc - 1),
                  isAsync });
    }
  }

  synchronized void setStatusFrameChanged(int fileNo, int modelNo, int firstNo,
                                          int lastNo, int currentFrame,
                                          float currentMorphModel, String entryName) {
    if (vwr.ms == null) 
      return;
    boolean animating = vwr.am.animationOn;
    int frameNo = (animating ? -2 - currentFrame : currentFrame);
    setStatusChanged("frameChanged", frameNo,
        (currentFrame >= 0 ? vwr.getModelNumberDotted(currentFrame) : ""), false);
    String sJmol = jmolScriptCallback(CBK.ANIMFRAME);
    if (notifyEnabled(CBK.ANIMFRAME))
      cbl.notifyCallback(CBK.ANIMFRAME,
          new Object[] {
              sJmol,
              new int[] { frameNo, fileNo, modelNo, firstNo, lastNo,
                  currentFrame }, entryName, Float.valueOf(currentMorphModel) });
    if (!animating)
      vwr.checkMenuUpdate();
  }

  synchronized boolean setStatusDragDropped(int mode, int x, int y,
                                            String fileName) {
    setStatusChanged("dragDrop", 0, "", false);
    String sJmol = jmolScriptCallback(CBK.DRAGDROP);
    if (!notifyEnabled(CBK.DRAGDROP))
      return false;
    cbl.notifyCallback(CBK.DRAGDROP,
        new Object[] { sJmol, Integer.valueOf(mode), Integer.valueOf(x),
            Integer.valueOf(y), fileName });
    return true;
  }

  synchronized void setScriptEcho(String strEcho,
                                  boolean isScriptQueued) {
    if (strEcho == null)
      return;
    setStatusChanged("scriptEcho", 0, strEcho, false);
    String sJmol = jmolScriptCallback(CBK.ECHO);
    if (notifyEnabled(CBK.ECHO))
      cbl.notifyCallback(CBK.ECHO,
          new Object[] { sJmol, strEcho, Integer.valueOf(isScriptQueued ? 1 : 0) });
  }

  synchronized void setStatusMeasuring(String status, int intInfo, String strMeasure, float value) {
    setStatusChanged(status, intInfo, strMeasure, false);
    String sJmol = null;
    if(status.equals("measureCompleted")) { 
      Logger.info("measurement["+intInfo+"] = "+strMeasure);
      sJmol = jmolScriptCallback(CBK.MEASURE);
    } else if (status.equals("measurePicked")) {
        setStatusChanged("measurePicked", intInfo, strMeasure, false);
        Logger.info("measurePicked " + intInfo + " " + strMeasure);
    }
    if (notifyEnabled(CBK.MEASURE))
      cbl.notifyCallback(CBK.MEASURE, 
          new Object[] { sJmol, strMeasure,  Integer.valueOf(intInfo), status , Float.valueOf(value)});
  }
  
  synchronized void notifyError(String errType, String errMsg,
                                String errMsgUntranslated) {
    String sJmol = jmolScriptCallback(CBK.ERROR);
    if (notifyEnabled(CBK.ERROR))
      cbl.notifyCallback(CBK.ERROR,
          new Object[] { sJmol, errType, errMsg, vwr.getShapeErrorState(),
              errMsgUntranslated });
  }
  
  
  synchronized void notifyMinimizationStatus(String minStatus, Integer minSteps, 
                                             Float minEnergy, Float minEnergyDiff, String ff) {
    String sJmol = jmolScriptCallback(CBK.MINIMIZATION);
    if (notifyEnabled(CBK.MINIMIZATION))
      cbl.notifyCallback(CBK.MINIMIZATION,
          new Object[] { sJmol, minStatus, minSteps, minEnergy, minEnergyDiff, ff });
  }
  
  synchronized void setScriptStatus(String strStatus, String statusMessage,
                                    int msWalltime,
                                    String strErrorMessageUntranslated) {
    // only allow trapping of script information of type 0
    if (msWalltime < -1) {
      int iscript = -2 - msWalltime;
      setStatusChanged("scriptStarted", iscript, statusMessage, false);
      strStatus = "script " + iscript + " started";
    } else if (strStatus == null) {
      return;
    }
    String sJmol = (msWalltime == 0 ? jmolScriptCallback(CBK.SCRIPT)
        : null);
    boolean isScriptCompletion = (strStatus == JC.SCRIPT_COMPLETED);

    if (recordStatus("script")) {
      boolean isError = (strErrorMessageUntranslated != null);
      setStatusChanged((isError ? "scriptError" : "scriptStatus"), 0,
          strStatus, false);
      if (isError || isScriptCompletion)
        setStatusChanged("scriptTerminated", 1, "Jmol script terminated"
            + (isError ? " unsuccessfully: " + strStatus : " successfully"),
            false);
    }

    if (isScriptCompletion && vwr.getBoolean(T.messagestylechime)
        && vwr.getBoolean(T.debugscript))
      strStatus = vwr.getChimeMessenger().scriptCompleted(this, statusMessage, strErrorMessageUntranslated);
    Object[] data = new Object[] { sJmol, strStatus, statusMessage,
        Integer.valueOf(isScriptCompletion ? -1 : msWalltime),
        strErrorMessageUntranslated };
    if (notifyEnabled(CBK.SCRIPT))
      cbl.notifyCallback(CBK.SCRIPT, data);
    processScript(data);
  }

  void processScript(Object[] data) {
    int msWalltime = ((Integer) data[3]).intValue();
    // general message has msWalltime = 0
    // special messages have msWalltime < 0
    // termination message has msWalltime > 0 (1 + msWalltime)
    // "script started"/"pending"/"script terminated"/"script completed"
    // do not get sent to console
    
    vwr.notifyScriptEditor(msWalltime, data);
    if (msWalltime == 0)
      vwr.sendConsoleMessage(data[1] == null ? null : data[1].toString());
  }
  
  private int minSyncRepeatMs = 100;
  public boolean syncingScripts = false;
  boolean syncingMouse = false;
  boolean doSync() {
    return (isSynced && drivingSync && !syncDisabled);
  }
  
  synchronized void setSync(String mouseCommand) {
    if (syncingMouse) {
      if (mouseCommand != null)
        syncSend(mouseCommand, "*", 0);
    } else if (!syncingScripts)
      syncSend("!" + vwr.tm.getMoveToText(minSyncRepeatMs / 1000f, false), "*", 0);
  }

  private boolean drivingSync;
  private boolean isSynced;
  private boolean syncDisabled;
  boolean stereoSync;
  
  public final static int SYNC_OFF = 0;
  public final static int SYNC_DRIVER = 1;
  public final static int SYNC_SLAVE = 2;
  public final static int SYNC_DISABLE = 3;
  public final static int SYNC_ENABLE = 4;
  public final static int SYNC_STEREO = 5;
  
  void setSyncDriver(int syncMode) {
 
    // -1 slave   turn off driving, but not syncing
    //  0 off
    //  1 driving on as driver
    //  2 sync    turn on, but set as slave
    //  5 stereo
    if (stereoSync && syncMode != SYNC_ENABLE) {
      syncSend(Viewer.SYNC_NO_GRAPHICS_MESSAGE, "*", 0);
      stereoSync = false;
    }
    switch (syncMode) {
    case SYNC_ENABLE:
      if (!syncDisabled)
        return;
      syncDisabled = false;
      break;
    case SYNC_DISABLE:
      syncDisabled = true;
      break;
    case SYNC_STEREO:
      drivingSync = true;
      isSynced = true;
      stereoSync = true;
      break;
    case SYNC_DRIVER:
      drivingSync = true;
      isSynced = true;
      break;
    case SYNC_SLAVE:
      drivingSync = false;
      isSynced = true;
      break;
    default:
      drivingSync = false;
      isSynced = false;
    }
    if (Logger.debugging) {
      Logger.debug(
          vwr.appletName + " sync mode=" + syncMode +
          "; synced? " + isSynced + "; driving? " + drivingSync + "; disabled? " + syncDisabled);
    }
  }

  public Object syncSend(String script, Object appletNameOrProp, int port) {
    // no jmolscript option for syncSend
    if (port != 0 || notifyEnabled(CBK.SYNC)) {
      Object[] o = new Object[] { null, script, appletNameOrProp, Integer.valueOf(port) };
      if (cbl != null)
        cbl.notifyCallback(CBK.SYNC, o);
      return o[0];
    }
    return null;
  }
 
  public void modifySend(int atomIndex, int modelIndex, int mode, String msg) {
    String sJmol = jmolScriptCallback(CBK.STRUCTUREMODIFIED);
    if (notifyEnabled(CBK.STRUCTUREMODIFIED))
      cbl.notifyCallback(CBK.STRUCTUREMODIFIED,
          new Object[] { sJmol, Integer.valueOf(mode), Integer.valueOf(atomIndex), Integer.valueOf(modelIndex), msg });
  }
  
  /**
   * service is expected to return a value in the "ret" key
   * 
   * @param info
   *        with key "service"
   * @return info, for chaining
   * 
   */
  public Object processService(Map<String, Object> info) {
    Object s = info.get("service");
    if (s == null)
      return null;
    if (s instanceof SV) {
      Map<String, Object> m = new Hashtable<String, Object>();
      for (Entry<String, Object> e : info.entrySet())
        m.put(e.getKey(), SV.oValue(e.getValue()));
      info = m;
    }
    if (notifyEnabled(CBK.SERVICE))
      cbl.notifyCallback(CBK.SERVICE, new Object[] { null, info });
    return info;
  }

  public int getSyncMode() {
    return (!isSynced ? SYNC_OFF : drivingSync ? SYNC_DRIVER : SYNC_SLAVE);
  }
  
  synchronized void showUrl(String urlString) {
    if (jsl != null)
      jsl.showUrl(urlString);
  }

  public synchronized void clearConsole() {
    vwr.sendConsoleMessage(null);
    if (jsl != null)
      cbl.notifyCallback(CBK.MESSAGE, null);
  }

  float[][] functionXY(String functionName, int nX, int nY) {
    return (jsl == null ? new float[Math.abs(nX)][Math.abs(nY)] :
      jsl.functionXY(functionName, nX, nY));
  }
  
  float[][][] functionXYZ(String functionName, int nX, int nY, int nZ) {
    return (jsl == null ? new float[Math.abs(nX)][Math.abs(nY)][Math.abs(nY)] :
      jsl.functionXYZ(functionName, nX, nY, nZ));
  }
  
  /**
   * 
   * @param strEval
   * @return in Java a String; in JavaScript window.eval() 
   * 
   */
  String jsEval(String strEval) {
    return (jsl == null ? "" : jsl.eval(strEval));
  }

  /**
   * offer to let application do the image creation.
   * if text_or_bytes == null, then this is an error report.
   * 
   * @param fileNameOrError 
   * @param type
   * @param text
   * @param bytes
   * @param quality
   * @return null (canceled) or a message starting with OK or an error message
   */
  String createImage(String fileNameOrError, String type, String text, byte[] bytes,
                     int quality) {
    return (jsl == null  ? null :
      jsl.createImage(fileNameOrError, type, text == null ? bytes : text, quality));
  }

  Map<String, Object> getRegistryInfo() {
    /* 

     //note that the following JavaScript retrieves the registry:
     
        var registry = jmolGetPropertyAsJavaObject("appletInfo").get("registry")
      
     // and the following code then retrieves an array of applets:
     
        var AppletNames = registry.keySet().toArray()
      
     // and the following sends commands to an applet in the registry:
      
        registry.get(AppletNames[0]).script("background white")
        
     */
    return (jsl == null ? null : jsl.getRegistryInfo());
  }

  private int qualityJPG = -1;
  private int qualityPNG = -1;
  private String imageType;

  String dialogAsk(String type, String fileName, Map<String, Object> params) {
    boolean isImage = type.equals("Save Image");
    JmolDialogInterface sd = (JmolDialogInterface) Interface.getOption(
        "dialog.Dialog", vwr, "status");
    if (sd == null)
      return null;
    sd.setupUI(false);
    if (isImage)
      sd.setImageInfo(qualityJPG, qualityPNG, imageType);
    String outputFileName = sd.getFileNameFromDialog(vwr, type, fileName); // may have #NOCARTOONS#; and/or "#APPEND#; prepended
    // may have #NOCARTOONS#; and/or "#APPEND#; prepended

    if (isImage && outputFileName != null) {
      qualityJPG = sd.getQuality("JPG");
      qualityPNG = sd.getQuality("PNG");
      String sType = sd.getType();
      if (params != null) {
        params.put("qualityJPG", Integer.valueOf(qualityJPG));
        params.put("qualityPNG", Integer.valueOf(qualityPNG));
        if (sType != null)
          params.put("dialogImageType", sType);
      }
      if (sType != null)
        imageType = sType;
    }
    return outputFileName;
  }

  Map<String, Object> getJspecViewProperties(String myParam) {
    return (jsl == null ? null : jsl
        .getJSpecViewProperty(myParam == null || myParam.length() == 0 ? "" : ":" + myParam));
  }

  public int[] resizeInnerPanel(int width, int height) {
    return (jsl == null || width == vwr.getScreenWidth()
        && height == vwr.getScreenHeight() ? new int[] { width, height } : jsl
        .resizeInnerPanel("preferredWidthHeight " + width + " " + height + ";"));
  }

  /**
   * called by file droppers
   * 
   * @param data
   */
  public void resizeInnerPanelString(String data) {
    if (jsl != null)
      jsl.resizeInnerPanel(data);
  }

  private Map<String, JmolAudioPlayer>audios;
  
  public void registerAudio(String id, Map<String, Object> htParams) {
    stopAudio(id);
    if (audios == null)
      audios = new Hashtable<String, JmolAudioPlayer>();
    if (htParams == null)
      audios.remove(id);
    else
      audios.put(id, (JmolAudioPlayer) htParams.get("audioPlayer"));
  }

  private void stopAudio(String id) {
    if (audios == null)
      return;
    JmolAudioPlayer player = audios.get(id);
    if (player != null)
      player.action("kill");    
  }
  
  public void playAudio(Map<String, Object> htParams) {
    if (!vwr.getBoolean(T.allowaudio)) {
      if (htParams == null)
        return;
      htParams.put("status", "close");
      Logger.info("allowAudio is set false");
      notifyAudioStatus(htParams);
      return;
    }
    try {
      String action = (htParams == null ? "close" : (String) htParams
          .get("action"));
      String id = (htParams == null ? null : (String) htParams.get("id"));
      if (action != null && action.length() > 0) {
        if (id == null || id.length() == 0) {
          if (audios == null || audios.isEmpty())
            return;
          if (action.equals("close")) {
            for (String key : audios.keySet()) {
              JmolAudioPlayer player = audios.remove(key);
              player.action("close");
            }
          }
          return;
        }
        JmolAudioPlayer player = audios.get(id);
        if (player != null) {
          player.action(action);
          return;
        }
      }
      try {
        ((JmolAudio) Interface.getInterface("org.jmol.util.JmolAudio", vwr,
            "script")).playAudio(vwr, htParams);
      } catch (Exception e) {
        Logger.info(e.getMessage());
      }
    } catch (Throwable t) {
      // ignore
    }
  }

  /**
   * called from JmolAudio
   * 
   * @param htParams
   */
  synchronized public void notifyAudioStatus(Map<String, Object> htParams) {
    String status = (String) htParams.get("status");
    System.out.println(status);
    String script = (String) htParams.get(status);
    if (script != null)
      vwr.script(script);
    if (status == "ended")
      registerAudio((String) htParams.get("id"), null);
    String sJmol = jmolScriptCallback(CBK.AUDIO);
    if (notifyEnabled(CBK.AUDIO))
      cbl.notifyCallback(CBK.AUDIO,
          new Object[] { sJmol, htParams });
  }
  
  
 void syncScript(String script, String applet, int port) {
    if (Viewer.SYNC_GRAPHICS_MESSAGE.equalsIgnoreCase(script)) {
      setSyncDriver(StatusManager.SYNC_STEREO);
      syncSend(script, applet, 0);
      vwr.setBooleanProperty("_syncMouse", false);
      vwr.setBooleanProperty("_syncScript", false);
      return;
    }
    // * : all applets
    // > : all OTHER applets
    // . : just me
    // ~ : disable send (just me)
    // = : disable send (just me) and force slave
    if ("=".equals(applet)) {
      applet = "~";
      setSyncDriver(StatusManager.SYNC_SLAVE);
    }
    boolean disableSend = "~".equals(applet);
    // null same as ">" -- "all others"
    if (port > 0 || !disableSend && !".".equals(applet)) {
      syncSend(script, applet, port);
      if (!"*".equals(applet) || script.startsWith("{"))
        return;
    }
    if (script.equalsIgnoreCase("on") || script.equalsIgnoreCase("true")) {
      setSyncDriver(StatusManager.SYNC_DRIVER);
      return;
    }
    if (script.equalsIgnoreCase("off") || script.equalsIgnoreCase("false")) {
      setSyncDriver(StatusManager.SYNC_OFF);
      return;
    }
    if (script.equalsIgnoreCase("slave")) {
      setSyncDriver(StatusManager.SYNC_SLAVE);
      return;
    }
    int syncMode = getSyncMode();
    if (syncMode == StatusManager.SYNC_OFF)
      return;
    if (syncMode != StatusManager.SYNC_DRIVER)
      disableSend = false;
    if (Logger.debugging)
      Logger.debug(vwr.htmlName + " syncing with script: " + script);
    // driver is being positioned by another driver -- don't pass on the change
    // driver is being positioned by a mouse movement
    // format is from above refresh(Viewer.REFRESH_SYNC, xxx) calls
    // Mouse: [CommandName] [value1] [value2]
    if (disableSend)
      setSyncDriver(StatusManager.SYNC_DISABLE);
    if (script.indexOf("Mouse: ") != 0) {
      int serviceMode = JC.getServiceCommand(script);
      switch (serviceMode) {
      case JC.NBO_CONFIG:
      case JC.NBO_MODEL:
      case JC.NBO_RUN:
      case JC.NBO_VIEW:
      case JC.NBO_SEARCH:
        syncSend(script, ".", port);
        return;        
      case JC.JSV_NOT:
        break;
      case JC.JSV_SEND_JDXMOL:
      case JC.JSV_CLOSE:
      case JC.JSV_SEND_H1SIMULATE:
      case JC.JSV_SEND_C13SIMULATE:
        if (disableSend)
          return;
        //$FALL-THROUGH$
      case JC.JSV_STRUCTURE:
      case JC.JSV_SETPEAKS:
      case JC.JSV_SELECT:
        // from JSpecView...
        if ((script = vwr.getJSV().processSync(script, serviceMode)) == null)
          return;
      }
      //System.out.println("Jmol executing script for JSpecView: " + script);
      vwr.evalStringQuietSync(script, true, false);
      return;
    }
    mouseScript(script);
    if (disableSend)
      vwr.setSyncDriver(StatusManager.SYNC_ENABLE);
  }

  void mouseScript(String script) {
    String[] tokens = PT.getTokens(script);
    String key = tokens[1];
    try {
      key = (key.toLowerCase() + "...............").substring(0, 15);
      switch ((
          "zoombyfactor..." + 
          "zoomby........." + 
          "rotatezby......" + 
          "rotatexyby....." + 
          "translatexyby.." + 
          "rotatemolecule." + 
          "spinxyby......." + 
          "rotatearcball..").indexOf(key)) {
      case 0: //zoombyfactor
        switch (tokens.length) {
        case 3:
          vwr.zoomByFactor(PT.parseFloat(tokens[2]),
              Integer.MAX_VALUE, Integer.MAX_VALUE);
          return;
        case 5:
          vwr.zoomByFactor(PT.parseFloat(tokens[2]), javajs.util.PT
              .parseInt(tokens[3]), PT.parseInt(tokens[4]));
          return;
        }
        break;
      case 15: //zoomby
        switch (tokens.length) {
        case 3:
          vwr.zoomBy(PT.parseInt(tokens[2]));
          return;
        }
        break;
      case 30: // rotatezby
        switch (tokens.length) {
        case 3:
          vwr.rotateZBy(PT.parseInt(tokens[2]), Integer.MAX_VALUE,
              Integer.MAX_VALUE);
          return;
        case 5:
          vwr.rotateZBy(PT.parseInt(tokens[2]), javajs.util.PT
              .parseInt(tokens[3]), PT.parseInt(tokens[4]));
        }
        break;
      case 45: // rotatexyby
        vwr.rotateXYBy(PT.parseFloat(tokens[2]), PT
            .parseFloat(tokens[3]));
        return;
      case 60: // translatexyby
        vwr.translateXYBy(PT.parseInt(tokens[2]), javajs.util.PT
            .parseInt(tokens[3]));
        return;
      case 75: // rotatemolecule
        vwr.rotateSelected(PT.parseFloat(tokens[2]), PT
            .parseFloat(tokens[3]), null);
        return;
      case 90:// spinxyby
        vwr.spinXYBy(PT.parseInt(tokens[2]), PT.parseInt(tokens[3]),
            PT.parseFloat(tokens[4]));
        return;
      case 105: // rotatearcball
        vwr.rotateXYBy(PT.parseInt(tokens[2]), javajs.util.PT
            .parseInt(tokens[3]));//, PT.parseFloat(tokens[4]));
        return;
      }
    } catch (Exception e) {
      //
    }
    vwr.showString("error reading SYNC command: " + script, false);
  }



}

