/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-05-01 16:40:46 -0500 (Mon, 01 May 2006) $
 * $Revision: 5041 $
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
package org.jmol.script;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.JOptionPane;

import javajs.api.ZInputStream;
import javajs.util.AU;
import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.Rdr;
import javajs.util.SB;

import org.jmol.api.Interface;
import org.jmol.api.JmolScriptEvaluator;
import org.jmol.api.JmolScriptManager;
import org.jmol.i18n.GT;
import org.jmol.modelset.Atom;
import org.jmol.thread.JmolThread;
import org.jmol.util.Elements;
import org.jmol.util.Logger;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.StatusManager;
import org.jmol.viewer.Viewer;

public class ScriptManager implements JmolScriptManager {

  private Viewer vwr;
  private ScriptEval eval;

  private JmolScriptEvaluator evalTemp;

  private Thread[] queueThreads = new Thread[2];
  private boolean[] scriptQueueRunning = new boolean[2];
  private JmolThread commandWatcherThread;

  public Lst<Lst<Object>> scriptQueue = new Lst<Lst<Object>>();

  @Override
  public Lst<Lst<Object>> getScriptQueue() {
    return scriptQueue;
  }

  @Override
  public boolean isScriptQueued() {
    return isScriptQueued;
  }

  public ScriptManager() {
    // by reflection only
  }

  @Override
  public JmolScriptEvaluator setViewer(Viewer vwr) {
    this.vwr = vwr;
    eval = newScriptEvaluator();
    eval.setCompiler();
    return eval;
  }

  private ScriptEval newScriptEvaluator() {
    return ((ScriptEval) Interface.getInterface("org.jmol.script.ScriptEval",
        vwr, "setOptions")).setViewer(vwr);
  }

  @Override
  public void clear(boolean isAll) {
    if (!isAll) {
      evalTemp = null;
      return;
    }
    startCommandWatcher(false);
    interruptQueueThreads();
  }

  @Override
  public String addScript(String strScript, boolean isQuiet) {
    return (String) addScr("String", strScript, "", isQuiet);
  }

  private Object addScr(String returnType, String strScript, String statusList,
                        boolean isQuiet) {
    /**
     * @j2sNative this.useCommandWatcherThread = false;
     */
    {
    }

    if (!vwr.g.useScriptQueue) {
      clearQueue();
      vwr.haltScriptExecution();
    }
    if (commandWatcherThread == null && useCommandWatcherThread)
      startCommandWatcher(true);
    if (commandWatcherThread != null && strScript.indexOf("/*SPLIT*/") >= 0) {
      String[] scripts = PT.split(strScript, "/*SPLIT*/");
      for (int i = 0; i < scripts.length; i++)
        addScr(returnType, scripts[i], statusList, isQuiet);
      return "split into " + scripts.length + " sections for processing";
    }
    boolean useCommandThread = (commandWatcherThread != null
        && (strScript.indexOf("javascript") < 0
            || strScript.indexOf("#javascript ") >= 0));
    // scripts with #javascript will be processed at the browser end
    Lst<Object> scriptItem = new Lst<Object>();
    scriptItem.addLast(strScript);
    scriptItem.addLast(statusList);
    scriptItem.addLast(returnType);
    //    scriptItem.addLast(Boolean.FALSE);
    scriptItem.addLast(isQuiet ? Boolean.TRUE : Boolean.FALSE);
    scriptItem.addLast(Integer.valueOf(useCommandThread ? -1 : 1));
    scriptQueue.addLast(scriptItem);
    //if (Logger.debugging)
    //  Logger.info("ScriptManager queue size=" + scriptQueue.size() + " scripts; added: " 
    //    + strScript + " " + Thread.currentThread().getName());
    startScriptQueue(false);
    return "pending";
  }

  //public int getScriptCount() {
  //  return scriptQueue.size();
  //}

  @Override
  public void clearQueue() {
    scriptQueue.clear();
  }

  @Override
  public void waitForQueue() {
    // just can't do this in JavaScript. 
    // if we are here and it is single-threaded, and there is
    // a script running, then that's a problem.

    if (vwr.isSingleThreaded)
      return;
    int n = 0;
    while (isQueueProcessing()) {
      try {
        Thread.sleep(100);
        if (((n++) % 10) == 0)
          if (Logger.debugging) {
            Logger.debug(
                "...scriptManager waiting for queue: " + scriptQueue.size()
                    + " thread=" + Thread.currentThread().getName());
          }
      } catch (InterruptedException e) {
      }
    }
  }

  @Override
  public boolean isQueueProcessing() {
    return queueThreads[0] != null || queueThreads[1] != null;
  }

  synchronized private void flushQueue(String command) {
    for (int i = scriptQueue.size(); --i >= 0;) {
      String strScript = (String) (scriptQueue.get(i).get(0));
      if (strScript.indexOf(command) == 0) {
        scriptQueue.removeItemAt(i);
        if (Logger.debugging)
          Logger.debug(scriptQueue.size() + " scripts; removed: " + strScript);
      }
    }
  }

  private void startScriptQueue(boolean startedByCommandWatcher) {
    int pt = (startedByCommandWatcher ? 1 : 0);
    if (scriptQueueRunning[pt])
      return;
    scriptQueueRunning[pt] = true;
    queueThreads[pt] = new ScriptQueueThread(this, vwr, startedByCommandWatcher,
        pt);
    //System.out.println("script manager starting " + pt + " " + queueThreads[pt]);
    queueThreads[pt].start();
  }

  @Override
  public Lst<Object> getScriptItem(boolean watching,
                                   boolean isByCommandWatcher) {
    if (vwr.isSingleThreaded && vwr.queueOnHold)
      return null;
    Lst<Object> scriptItem = scriptQueue.get(0);
    int flag = (((Integer) scriptItem.get(4)).intValue());
    boolean isOK = (watching ? flag < 0
        : isByCommandWatcher ? flag == 0 : flag == 1);
    return (isOK ? scriptItem : null);
  }

  private boolean useCommandWatcherThread = false;

  @Override
  synchronized public void startCommandWatcher(boolean isStart) {
    useCommandWatcherThread = isStart;
    if (isStart) {
      if (commandWatcherThread != null)
        return;
      commandWatcherThread = (JmolThread) Interface.getInterface(
          "org.jmol.script.CommandWatcherThread", vwr, "setOptions");
      commandWatcherThread.setManager(this, vwr, null);
      commandWatcherThread.start();
    } else {
      if (commandWatcherThread == null)
        return;
      clearCommandWatcherThread();
    }
    if (Logger.debugging) {
      Logger.debug("command watcher " + (isStart ? "started" : "stopped")
          + commandWatcherThread);
    }
  }

  /*
   * CommandWatcher thread handles processing of 
   * command scripts independently of the user thread.
   * This is important for the signed applet, where the
   * thread opening remote files cannot be the browser's,
   * and commands that utilize JavaScript must.
   * 
   * We need two threads for the signed applet, because commands
   * that involve JavaScript -- the "javascript" command or math javascript() --
   * must run on a thread created by the thread generating the applet call.
   * 
   * This CommandWatcher thread, on the other hand, is created by the applet at 
   * start up -- it can cross domains, but it can't run JavaScript. 
   * 
   * The 5th vector position is an Integer flag.
   * 
   *   -1  -- Owned by CommandWatcher; ready for thread assignment
   *    0  -- Owned by CommandWatcher; running
   *    1  -- Owned by the JavaScript-enabled/browser-limited thread
   * 
   * If the command is to be ignored by the CommandWatcher, the flag is set 
   * to 1. For the watcher, the flag is first set to -1. This means the
   * command watcher owns it, and the standard script thread should
   * ignore it. The current script queue cycles.
   * 
   * if the CommandWatcher sees a -1 in element 5 of the 0 (next) queue position
   * vector, then it says, "That's mine -- I'll take it." It sets the
   * flag to 0 and starts the script queue. When that script queue removes the
   * 0-position item, the previous script queue takes off again and 
   * finishes the run.
   *  
   */

  void interruptQueueThreads() {
    for (int i = 0; i < queueThreads.length; i++) {
      if (queueThreads[i] != null)
        queueThreads[i].interrupt();
    }
  }

  public void clearCommandWatcherThread() {
    if (commandWatcherThread == null)
      return;
    commandWatcherThread.interrupt();
    commandWatcherThread = null;
  }

  @Override
  public void queueThreadFinished(int pt) {
    //System.out.println("queuethread " + pt + " done");
    queueThreads[pt].interrupt();
    scriptQueueRunning[pt] = false;
    queueThreads[pt] = null;
    vwr.setSyncDriver(StatusManager.SYNC_ENABLE);
    vwr.queueOnHold = false;
    //System.out.println("queuethread " + pt + " done");
  }

  public void runScriptNow() {
    // from ScriptQueueThread
    if (scriptQueue.size() > 0) {
      Lst<Object> scriptItem = getScriptItem(true, true);
      if (scriptItem != null) {
        scriptItem.set(4, Integer.valueOf(0));
        startScriptQueue(true);
      }
    }
  }

  @Override
  public String evalFile(String strFilename) {
    // app -s flag
    int ptWait = strFilename.indexOf(" -noqueue"); // for TestScripts.java
    if (ptWait >= 0) {
      return (String) evalStringWaitStatusQueued("String",
          "script " + PT.esc(strFilename.substring(0, ptWait)), "", false,
          false);
    }
    return addScript("script " + PT.esc(strFilename), false);
  }

  private int scriptIndex;
  private boolean isScriptQueued = true;

  @Override
  public Object evalStringWaitStatusQueued(String returnType, String strScript,
                                           String statusList, boolean isQuiet,
                                           boolean isQueued) {
    // from the scriptManager or scriptWait()
    if (strScript == null)
      return null;
    String str = checkScriptExecution(strScript, false);
    if (str != null)
      return str;
    SB outputBuffer = (statusList == null || statusList.equals("output")
        ? new SB()
        : null);

    // typically request:
    // "+scriptStarted,+scriptStatus,+scriptEcho,+scriptTerminated"
    // set up first with applet.jmolGetProperty("jmolStatus",statusList)
    // flush list
    String oldStatusList = vwr.sm.statusList;
    vwr.getStatusChanged(statusList);
    if (vwr.isSyntaxCheck)
      Logger.info("--checking script:\n" + eval.getScript() + "\n----\n");
    boolean historyDisabled = (strScript.indexOf(")") == 0);
    if (historyDisabled)
      strScript = strScript.substring(1);
    historyDisabled = historyDisabled || !isQueued; // no history for scriptWait
    // 11.5.45
    vwr.setErrorMessage(null, null);
    // 14.11.0 need to use a new eval for each Jmol.scriptWait() or else we can
    // run into the problem when that is used in a callback that eval.aatokens is overridden
    ScriptEval eval = (isQueued ? this.eval : newScriptEvaluator());
    boolean isOK = eval.compileScriptString(strScript, isQuiet);
    String strErrorMessage = eval.getErrorMessage();
    String strErrorMessageUntranslated = eval.getErrorMessageUntranslated();
    vwr.setErrorMessage(strErrorMessage, strErrorMessageUntranslated);
    vwr.refresh(Viewer.REFRESH_SEND_WEBGL_NEW_ORIENTATION, "script complete");
    if (isOK) {
      isScriptQueued = isQueued;
      if (!isQuiet)
        vwr.setScriptStatus(null, strScript, -2 - (++scriptIndex), null);
      eval.evaluateCompiledScript(vwr.isSyntaxCheck, vwr.isSyntaxAndFileCheck,
          historyDisabled, vwr.listCommands, outputBuffer,
          isQueued || !vwr.isSingleThreaded);
    } else {
      vwr.scriptStatus(strErrorMessage);
      vwr.setScriptStatus("Jmol script terminated", strErrorMessage, 1,
          strErrorMessageUntranslated);
      if (eval.isStateScript())
        setStateScriptVersion(vwr, null); // set by compiler
    }
    if (strErrorMessage != null && vwr.autoExit)
      vwr.exitJmol();
    if (vwr.isSyntaxCheck) {
      if (strErrorMessage == null)
        Logger.info("--script check ok");
      else
        Logger.error("--script check error\n" + strErrorMessageUntranslated);
      Logger.info("(use 'exit' to stop checking)");
    }
    isScriptQueued = true;
    if (returnType.equalsIgnoreCase("String"))
      return strErrorMessageUntranslated;
    if (outputBuffer != null)
      return (strErrorMessageUntranslated == null ? outputBuffer.toString()
          : strErrorMessageUntranslated);
    // get Vector of Vectors of Vectors info ("object") or, for the applet, JSON ("json")
    Object info = vwr.getProperty(returnType, "jmolStatus", statusList);
    vwr.getStatusChanged(oldStatusList);
    return info;
  }

  private String checkScriptExecution(String strScript, boolean isInsert) {
    String str = strScript;
    if (str.indexOf("\1##") >= 0)
      str = str.substring(0, str.indexOf("\1##"));
    if (checkResume(str))
      return "script processing resumed";
    if (checkStepping(str))
      return "script processing stepped";
    if (checkHalt(str, isInsert))
      return "script execution halted";
    wasmHack(strScript);
    return null;
  }

  private boolean checkResume(String str) {
    if (str.equalsIgnoreCase("resume")) {
      vwr.scriptStatusMsg("", "execution resumed");
      eval.resumePausedExecution();
      return true;
    }
    return false;
  }

  private boolean checkStepping(String str) {
    if (str.equalsIgnoreCase("step")) {
      eval.stepPausedExecution();
      return true;
    }
    if (str.equalsIgnoreCase("?")) {
      vwr.scriptStatus(eval.getNextStatement());
      return true;
    }
    return false;
  }

  @Override
  public String evalStringQuietSync(String strScript, boolean isQuiet,
                                    boolean allowSyncScript) {
    // central point for all incoming script processing
    // all menu items, all mouse movement -- everything goes through this method
    // by setting syncScriptTarget = ">" the user can direct that all scripts
    // initiated WITHIN this applet (not sent to it)
    // we append #NOSYNC; here so that the receiving applet does not attempt
    // to pass it back to us or any other applet.
    if (allowSyncScript && vwr.sm.syncingScripts
        && strScript.indexOf("#NOSYNC;") < 0)
      vwr.syncScript(strScript + " #NOSYNC;", null, 0);
    if (eval.isPaused() && strScript.charAt(0) != '!')
      strScript = '!' + PT.trim(strScript, "\n\r\t ");
    boolean isInsert = (strScript.length() > 0 && strScript.charAt(0) == '!');
    if (isInsert)
      strScript = strScript.substring(1);
    String msg = checkScriptExecution(strScript, isInsert);
    if (msg != null)
      return msg;
    if (vwr.isScriptExecuting() && (isInsert || eval.isPaused())) {
      vwr.setInsertedCommand(strScript);
      if (strScript.indexOf("moveto ") == 0)
        flushQueue("moveto ");
      return "!" + strScript;
    }
    vwr.setInsertedCommand("");
    if (isQuiet)
      strScript += JC.SCRIPT_EDITOR_IGNORE;
    return addScript(strScript,
        isQuiet && !vwr.getBoolean(T.messagestylechime));
  }

  @Override
  public boolean checkHalt(String str, boolean isInsert) {
    if (str.equalsIgnoreCase("pause")) {
      vwr.pauseScriptExecution();
      if (vwr.scriptEditorVisible)
        vwr.setScriptStatus("", "paused -- type RESUME to continue", 0, null);
      return true;
    }
    if (str.equalsIgnoreCase("menu")) {
      vwr.getProperty("DATA_API", "getPopupMenu", "\0");
      return true;
    }
    str = str.toLowerCase();
    boolean exitScript = false;
    String haltType = null;
    if (str.startsWith("exit")) {
      vwr.haltScriptExecution();
      vwr.clearScriptQueue();
      vwr.clearTimeouts();
      exitScript = str.equals(haltType = "exit");
    } else if (str.startsWith("quit")) {
      vwr.haltScriptExecution();
      exitScript = str.equals(haltType = "quit");
    }
    if (haltType == null)
      return false;
    // !quit or !exit
    if (isInsert) {
      vwr.clearThreads();
      vwr.queueOnHold = false;
    }
    if (isInsert || vwr.g.waitForMoveTo) {
      vwr.tm.stopMotion();
    }
    Logger.info(vwr.isSyntaxCheck ? haltType + " -- stops script checking"
        : (isInsert ? "!" : "") + haltType + " received");
    vwr.isSyntaxCheck = false;
    return exitScript;
  }

  @Override
  public BS getAtomBitSetEval(JmolScriptEvaluator eval, Object atomExpression) {
    if (eval == null) {
      eval = evalTemp;
      if (eval == null)
        eval = evalTemp = newScriptEvaluator();
    }
    return vwr.slm.excludeAtoms(eval.getAtomBitSet(atomExpression), false);
  }

  @Override
  public Object scriptCheckRet(String strScript, boolean returnContext) {
    // from ConsoleTextPane.checkCommand() and applet Jmol.scriptProcessor()
    if (strScript.indexOf(")") == 0 || strScript.indexOf("!") == 0) // history
      // disabled
      strScript = strScript.substring(1);
    strScript = wasmHack(strScript);
    ScriptContext sc = newScriptEvaluator().checkScriptSilent(strScript);
    return (returnContext || sc.errorMessage == null ? sc : sc.errorMessage);
  }

  public String wasmHack(String cmd) {
    if (Viewer.isJS
        && (cmd.indexOf("find('inchi')") >= 0
            || cmd.indexOf("find(\"inchi\")") >= 0)
        || cmd.indexOf(".inchi(") >= 0) {
      vwr.getInchi(null, null, null);
    }
    return cmd;
  }

  //////////////////////// open file async ///////////////////////

  /**
   * 
   * From file dropping.
   * 
   * @param fname
   * @param flags
   *        1=pdbCartoons, 2=no scripting, 4=append, 8=noAutoPlay
   * 
   */
  @Override
  public void openFileAsync(String fname, int flags, boolean checkDims) {
    if (checkDims && FileManager.isEmbeddable(fname)) 
      checkResize(fname);
    boolean noScript = ((flags & NO_SCRIPT) != 0);
    boolean noAutoPlay = ((flags & NO_AUTOPLAY) != 0);

    String cmd = null;
    fname = fname.trim();
    if (fname.startsWith("\t")) {
      noScript = true;
      fname = fname.substring(1);
    }
    fname = fname.replace('\\', '/');
    boolean isCached = fname.startsWith("cache://");
    if (vwr.isApplet && fname.indexOf("://") < 0)
      fname = "file://" + (fname.startsWith("/") ? "" : "/") + fname;
    try {
      // using finally... here on return
      if (fname.endsWith(".pse")) {
        cmd = (isCached ? "" : "zap;") + "load SYNC " + PT.esc(fname)
            + (vwr.isApplet ? "" : " filter 'DORESIZE'");
        return;
      }
      if (fname.endsWith("jvxl")) {
        cmd = "isosurface ";
      } else if (!fname.toLowerCase().endsWith(".spt")) {
        String type = getDragDropFileTypeName(fname);
        if (type == null) {
          try {
            BufferedInputStream bis = vwr.getBufferedInputStream(fname);
            type = FileManager.determineSurfaceFileType(
                Rdr.getBufferedReader(bis, "ISO-8859-1"));
            if (type == null) {
              cmd = "script " + PT.esc(fname);
              return;
            }
          } catch (IOException e) {
            return;
          }
          cmd = "if (_filetype == 'Pdb') { isosurface sigma 1.0 within 2.0 {*} "
              + PT.esc(fname) + " mesh nofill }; else; { isosurface "
              + PT.esc(fname) + "}";
          return;
        }
        // these next will end with the escaped file name
        if (type.equals("spt::")) {
          cmd = "script " + PT.esc(fname.substring(5));
          return;
        }
        if (type.equals("dssr")) {
          cmd = "model {visible} property dssr ";
        } else if (type.equals("Jmol")) {
          cmd = "script ";
        } else if (type.equals("Cube")) {
          cmd = "isosurface sign red blue ";
        } else if (!type.equals("spt")) {
          if (flags == FILE_DROPPED) {
            flags = PDB_CARTOONS;
            switch (vwr.ms.ac == 0 ? JOptionPane.OK_OPTION
                : vwr.confirm(GT.$("Would you like to replace the current model with the selected model?"), 
                    GT.$("Would you like to append?"))) {
            case JOptionPane.CANCEL_OPTION:
              return;
            case JOptionPane.OK_OPTION:
              break;
            default:
              flags |= JmolScriptManager.IS_APPEND;
              break;
            }
          }
          boolean isAppend = ((flags & IS_APPEND) != 0);
          boolean pdbCartoons = ((flags & PDB_CARTOONS) != 0 && !isAppend);

          if (type.endsWith("::")) {
            int pt = type.indexOf("|"); 
            if (pt >= 0) {
              fname += type.substring(pt, type.length() - 2);
              //type = type.substring(0, pt) + "::";
              type = "";
            }
            fname = type + fname; 
          }
          cmd = vwr.g.defaultDropScript;
          cmd = PT.rep(cmd, "%FILE", fname);
          cmd = PT.rep(cmd, "%ALLOWCARTOONS", "" + pdbCartoons);
          if (cmd.toLowerCase().startsWith("zap") && (isCached || isAppend))
            cmd = cmd.substring(3);
          if (isAppend) {
            cmd = PT.rep(cmd, "load SYNC", "load append");
          }
          return;
        }
      }
      if (cmd == null && !noScript && vwr.scriptEditorVisible)
        vwr.showEditor(
            new String[] { fname, vwr.getFileAsString3(fname, true, null) });
      else
        cmd = (cmd == null ? "script " : cmd) + PT.esc(fname);
    } finally {
      if (cmd != null)
        vwr.evalString(cmd + (noAutoPlay ? "#!NOAUTOPLAY" : ""));
    }
  }

  private void checkResize(String fname) {
    try {
      String data = vwr.fm.getEmbeddedFileState(fname, false, "state.spt");
      if (data.indexOf("preferredWidthHeight") >= 0)
        vwr.sm.resizeInnerPanelString(data);
    } catch (Throwable e) {
      // ignore
    }
  }

  /**
   * 
   * @param fileName
   * @return "pdb" or "dssr" or "Jmol" or <modelType> + "::"
   */
  private String getDragDropFileTypeName(String fileName) {
    int pt = fileName.indexOf("::");
    if (pt >= 0)
      return fileName.substring(0, pt + 2);
    if (fileName.startsWith("="))
      return "pdb";
    if (fileName.endsWith(".dssr"))
      return "dssr";
    Object br = vwr.fm.getUnzippedReaderOrStreamFromName(fileName, null, true,
        false, true, true, null);
    String modelType = null;
    if (br instanceof ZInputStream) {
      String zipDirectory = vwr.getZipDirectoryAsString(fileName);
      if (zipDirectory.indexOf("JmolManifest") >= 0)
        return "Jmol";
      modelType = vwr.getModelAdapter()
          .getFileTypeName(Rdr.getBR(zipDirectory));
    } else if (br instanceof BufferedReader
        || br instanceof BufferedInputStream) {
      modelType = vwr.getModelAdapter().getFileTypeName(br);
    }
    if (modelType != null)
      return modelType + "::";
    if (AU.isAS(br)) {
      return ((String[]) br)[0];
    }
    return null;
  }

  private static int prevCovalentVersion = 1;

  public static void setStateScriptVersion(Viewer vwr, String version) {
    if (version != null) {
      prevCovalentVersion = Elements.bondingVersion;
      String[] tokens = PT
          .getTokens(version.replace('.', ' ').replace('_', ' '));
      try {
        int main = PT.parseInt(tokens[0]); //11
        int sub = PT.parseInt(tokens[1]); //9
        int minor = PT.parseInt(tokens[2]); //24
        if (minor == Integer.MIN_VALUE) // RCxxx
          minor = 0;
        if (main != Integer.MIN_VALUE && sub != Integer.MIN_VALUE) {
          int ver = vwr.stateScriptVersionInt = main * 10000 + sub * 100
              + minor;
          vwr.setBooleanProperty("legacyautobonding", (ver < 110924));
          vwr.g.legacyHAddition = (ver < 130117);
          vwr.setBooleanProperty("legacyjavafloat",
              (ver < 140206 || ver >= 140300 && ver < 140306));
          vwr.setIntProperty("bondingVersion", ver < 140111 ? 0 : 1);
          return;
        }
      } catch (Exception e) {
        // ignore
      }
    }
    vwr.setIntProperty("bondingVersion", prevCovalentVersion);
    vwr.setBooleanProperty("legacyautobonding", false);
    vwr.g.legacyHAddition = false;
    vwr.stateScriptVersionInt = Integer.MAX_VALUE;
  }

  @Override
  public BS addHydrogensInline(BS bsAtoms, Lst<Atom> vConnections, P3[] pts)
      throws Exception {
    int iatom = bsAtoms.nextSetBit(0);
    int modelIndex = (iatom < 0 ? vwr.ms.mc - 1 : vwr.ms.at[iatom].mi);
//    if (!vwr.ms.isAtomInLastModel(iatom))
//      return new BS();

    // must be added to the LAST data set only

    BS bsA = vwr.getModelUndeletedAtomsBitSet(modelIndex);
    vwr.g.appendNew = false;
    // BitSet bsB = getAtomBits(Token.hydrogen, null);
    // bsA.andNot(bsB);
    int atomIndex = vwr.ms.ac;
    int atomno = vwr.ms.getAtomCountInModel(modelIndex);
    SB sbConnect = new SB();
    for (int i = 0; i < vConnections.size(); i++) {
      Atom a = vConnections.get(i);
      sbConnect.append(";  connect 0 100 ").append("({" + (atomIndex++) + "}) ")
          .append("({" + a.i + "}) group;");
    }
    SB sb = new SB();
    sb.appendI(pts.length).append("\n").append(JC.ADD_HYDROGEN_TITLE)
        .append("#noautobond").append("\n");
    for (int i = 0; i < pts.length; i++)
      sb.append("H ").appendF(pts[i].x).append(" ").appendF(pts[i].y)
          .append(" ").appendF(pts[i].z).append(" - - - - ").appendI(++atomno)
          .appendC('\n');
    
    Map<String, Object> htParams = new Hashtable<String, Object>();
    htParams.put("appendToModelIndex", Integer.valueOf(modelIndex));
    vwr.openStringInlineParamsAppend(sb.toString(), htParams, true);
    eval.runScriptBuffer(sbConnect.toString(), null, false);
    BS bsB = vwr.getModelUndeletedAtomsBitSet(modelIndex);
    bsB.andNot(bsA);
    return bsB;
  }

}
