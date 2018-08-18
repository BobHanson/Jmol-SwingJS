package org.jmol.api;


import java.util.Map;

import javajs.util.SB;

import javajs.util.BS;
import org.jmol.script.SV;
import org.jmol.script.ScriptContext;
import org.jmol.script.ScriptException;
import org.jmol.script.T;
import org.jmol.viewer.ShapeManager;
import org.jmol.viewer.Viewer;

public interface JmolScriptEvaluator {

  JmolScriptEvaluator setViewer(Viewer vwr);

  ScriptContext getThisContext();

  void pushContextDown(String why);

  void resumeEval(Object sc);

  boolean getAllowJSThreads();

  void setCompiler();

  BS getAtomBitSet(Object atomExpression);

  boolean isStopped();

  void notifyResumeStatus();

  boolean isPaused();

  String getNextStatement();

  void resumePausedExecution();

  void stepPausedExecution();

  void pauseExecution(boolean b);

  boolean isExecuting();

  void haltExecution();

  boolean compileScriptFile(String strScript, boolean isQuiet);

  boolean compileScriptString(String strScript, boolean isQuiet);

  String getErrorMessage();

  String getErrorMessageUntranslated();

  ScriptContext checkScriptSilent(String strScript);

  String getScript();

  void setDebugging();

  boolean isStepping();

  ScriptContext getScriptContext(String why);

  Object evaluateExpression(Object stringOrTokens, boolean asVariable, boolean compileOnly);

  void deleteAtomsInVariables(BS bsDeleted);

  boolean evalParallel(ScriptContext context, ShapeManager shapeManager);

  void runScript(String script) throws ScriptException;

  void runScriptBuffer(String string, SB outputBuffer, boolean isFuncReturn) throws ScriptException;

  float evalFunctionFloat(Object func, Object params, float[] values);

  void evaluateCompiledScript(boolean isSyntaxCheck,
                              boolean isSyntaxAndFileCheck,
                              boolean historyDisabled, boolean listCommands,
                              SB outputBuffer, boolean allowThreads);

  String setObjectPropSafe(String id, int tokCommand);

  void stopScriptThreads();

  boolean isStateScript();

  boolean checkSelect(Map<String, SV> h, T[] where);

  void loadFileResourceAsync(String fileName) throws Exception;

  int setStatic(int tok, int value);

}
