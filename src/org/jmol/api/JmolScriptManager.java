package org.jmol.api;



import javajs.util.BS;

import java.util.Map;

import org.jmol.modelset.Atom;

import javajs.util.Lst;
import javajs.util.P3d;

import org.jmol.viewer.Viewer;

public interface JmolScriptManager {

  JmolScriptEvaluator setViewer(Viewer vwr);
  
  void startCommandWatcher(boolean isStart);

  void clear(boolean isAll);

  void clearQueue();
  
  boolean isScriptQueued();
  
  void waitForQueue();

  Lst<Lst<Object>> getScriptQueue();

  void queueThreadFinished(int pt);

  Lst<Object> getScriptItem(boolean b, boolean startedByCommandThread);

  String evalStringQuietSync(String strScript, boolean isQuiet,
                             boolean allowSyncScript);

  Object evalStringWaitStatusQueued(String returnType, String strScript,
                                           String statusList,
                                           boolean isQuiet, boolean isQueued);

  boolean checkHalt(String str, boolean isInsert);

  BS getAtomBitSetEval(JmolScriptEvaluator eval, Object atomExpression);

  Object scriptCheckRet(String strScript, boolean returnContext);

  boolean isQueueProcessing();

  String evalFile(String strFilename);

  String evalFileArgs(String strFilename, String args);

  BS addHydrogensInline(BS bsAtoms, Lst<Atom> vConnections, P3d[] pts,
                        Map<String, Object> htParams)
      throws Exception;

  void evalCallback(String cmd, Object[] params, boolean doWait);

}

