/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2018-05-31 01:39:29 +0200 (Thu, 31 May 2018) $
 * $Revision: 21913 $
 *
 * Copyright (C) 2003-2006  Miguel, Jmol Development, www.jmol.org
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

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.Interface;
import org.jmol.api.JmolParallelProcessor;
import org.jmol.api.JmolScriptFunction;
import org.jmol.api.SymmetryInterface;
import org.jmol.atomdata.RadiusData;
import org.jmol.atomdata.RadiusData.EnumType;
import org.jmol.c.PAL;
import org.jmol.c.STR;
import org.jmol.c.VDW;
import org.jmol.i18n.GT;
import org.jmol.modelset.Atom;
import org.jmol.modelset.BondSet;
import org.jmol.modelset.Group;
import org.jmol.modelset.Measurement;
import org.jmol.modelset.TickInfo;
import org.jmol.shape.MeshCollection;
import org.jmol.shape.Shape;
import org.jmol.thread.JmolThread;
import org.jmol.util.BSUtil;
import org.jmol.util.BoxInfo;
import org.jmol.util.ColorEncoder;
import org.jmol.util.Edge;
import org.jmol.util.Elements;
import org.jmol.util.Escape;
import org.jmol.util.Font;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.util.SimpleUnitCell;
import org.jmol.viewer.ActionManager;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.ShapeManager;
import org.jmol.viewer.StateManager;
import org.jmol.viewer.TransformManager;
import org.jmol.viewer.Viewer;

import javajs.util.A4d;
import javajs.util.AU;
import javajs.util.BArray;
import javajs.util.BS;
import javajs.util.Base64;
import javajs.util.Lst;
import javajs.util.M3d;
import javajs.util.M4d;
import javajs.util.MeasureD;
import javajs.util.OC;
import javajs.util.P3d;
import javajs.util.P4d;
import javajs.util.PT;
import javajs.util.Qd;
import javajs.util.SB;
import javajs.util.T3d;
import javajs.util.V3d;

public class ScriptEval extends ScriptExpr {

  
  /*
   * To make this a bit more manageable, I separated ScriptEvaluator into four parts:
   * 
   * 
   * ScriptEval                -- entry point and script command code
   * 
   *   extends ScriptExpr      -- expression parsing
   * 
   *     extends ScriptParam   -- parameter parsing
   * 
   *       extends ScriptError -- error handling 
   * 
   *   scriptext.CmdExt        -- optionally loaded, less-used commands
   *   scriptext.IsoExt        -- optionally loaded, less-used commands
   *   scriptext.MathExt       -- optionally loaded, less-used functions
   *   scriptext.SmilesExt     -- optionally loaded methods for cmds and math
   *   
   * 
   * 
   * This main class is subdivided into the following sections:
   * 
   *  global fields
   * 
   * 
   * 
   * 
   *  Bob Hanson, 2/27/2014 
   */
  
  /*
   *
   * The ScriptEvaluator class, the Viewer, the xxxxManagers, the Graphics3D
   * rendering engine, the ModelSet and Shape classes, and the Adapter file
   * reader classes form the core of the Jmol molecular visualization framework.
   * 
   * An extension of this file is org.jmol.scriptext.ScriptExt .
   * 
   * The ScriptEvaluator has just a few entry points, which you will find
   * immediately following this comment. They include:
   * 
   * public boolean compileScriptString(String script, boolean tQuiet)
   * 
   * public boolean compileScriptFile(String filename, boolean tQuiet)
   * 
   * public void evaluateCompiledScript(boolean isCmdLine_c_or_C_Option, boolean
   * isCmdLine_C_Option, boolean historyDisabled, boolean listCommands)
   * 
   * Essentially ANYTHING can be done using these three methods. A variety of
   * other methods are available via Viewer, which is the the true portal to
   * Jmol (via the JmolViewer interface) for application developers who want
   * faster, more direct processing.
   * 
   * A little Jmol history:
   * 
   * General history notes can be found at our ConfChem paper, which can be
   * found at
   * http://chemapps.stolaf.edu/jmol/presentations/confchem2006/jmol-confchem
   * .htm
   * 
   * This ScriptEvaluator class was initially written by Michael (Miguel) Howard
   * as Eval.java as an efficient means of reproducing the RasMol scripting
   * language for Jmol. Key additions there included:
   * 
   * - tokenization of commands via the Compiler class (now ScriptCompiler and
   * ScriptCompilationTokenParser) - ScriptException error handling - a flexible
   * yet structured command parameter syntax - implementations of RasMol
   * secondary structure visualizations - isosurfaces, dots, labels, polyhedra,
   * draw, stars, pmesh, more
   * 
   * Other Miguel contributions include:
   * 
   * - the structural bases of the Adapter, ModelSet, and ModelSetBio classes -
   * creation of Manager classes - absolutely amazing raw pixel bitmap rendering
   * code (org.jmol.g3d) - popup context menu - inline model loading
   * 
   * Bob Hanson (St. Olaf College) found out about Jmol during the spring of
   * 2004. After spending over a year working on developing online interactive
   * documentation, he started actively writing code early in 2006. During the
   * period 2006-2009 Bob completely reworked the script processor (and much of
   * the rest of Jmol) to handle a much broader range of functionality. Notable
   * improvements include:
   * 
   * - display/hide commands - dipole, ellipsoid, geosurface, lcaoCartoon
   * visualizations - quaternion and ramachandran commands - much expanded
   * isosurface / draw commands - configuration, disorder, and biomolecule
   * support - broadly 2D- and 3D-positionable echos - translateSelected and
   * rotateSelected commands - getProperty command, providing access to more
   * file information - data and write commands - writing of high-resolution
   * JPG, PNG, and movie-sequence JPG - generalized export to Maya and PovRay
   * formats
   * 
   * - multiple file loading, including trajectories - minimization using the
   * Universal Force Field (UFF) - atom/model deletion and addition - direct
   * loading of properties such as partial charge or coordinates - several new
   * file readers, including manifested zip file reading - default directory, CD
   * command, and pop-up file open/save dialogs
   * 
   * - "internal" molecular coordinate-based rotations - full support for
   * crystallographic formats, including space groups, symmetry, unit cells, and
   * fractional coordinates - support for point groups and molecular symmetry -
   * navigation mode - antialiasing of display and imaging - save/restore/write
   * exact Jmol state - JVXL file format for compressed rapid generation of
   * isosurfaces
   * 
   * - user-defined variables - addition of a Reverse Polish Notation (RPN)
   * expression processor - extension of the RPN processor to user variables -
   * user-defined functions - flow control commands if/else/endif, for, and
   * while - JavaScript/Java-like brace syntax - key stroke-by-key stroke
   * command syntax checking - integrated help command - user-definable popup
   * menu - language switching
   * 
   * - fully functional signed applet - applet-applet synchronization, including
   * two-applet geoWall stereo rendering - JSON format for property delivery to
   * JavaScript - jmolScriptWait, dual-threaded queued JavaScript scripting
   * interface - extensive callback development - script editor panel (work in
   * progress, June 2009)
   * 
   * Several other people have contributed. Perhaps they will not be too shy to
   * add their claim to victory here. Please add your contributions.
   * 
   * - Jmol application (Egon Willighagen) - smiles support (Nico Vervelle) -
   * readers (Rene Kanter, Egon, several others) - initial VRML export work
   * (Nico Vervelle) - WebExport (Jonathan Gutow) - internationalization (Nico,
   * Egon, Angel Herriez) - Jmol Wiki and user guide book (Angel Herriez)
   * 
   * While this isn't necessarily the best place for such discussion, open
   * source principles require proper credit given to those who have
   * contributed. This core class seems to me a place to acknowledge this core
   * work of the Jmol team.
   * 
   * Bob Hanson, 6/2009 hansonr@stolaf.edu
   */

  
  
  /////////////////// global fields ///////////////////
  
  private final static String saveList = 
      "bonds? context? coordinates? orientation? rotation? selection? state? structure?";
  
  private static int iProcess;

  public ShapeManager sm;

  public boolean isJS;
  
  private JmolThread scriptDelayThread, fileLoadThread;

  private boolean allowJSThreads = true;

  @Override
  public boolean getAllowJSThreads() {
    return allowJSThreads;
  }
  
  public void setAllowJSThreads(boolean b) {
    allowJSThreads = b;
  }

  private boolean isFuncReturn;

  // execution options:
  
  public boolean historyDisabled; // set by ScriptExt.evaluateParallel

  private boolean debugScript;
  private boolean isCmdLine_C_Option;
  private boolean isCmdLine_c_or_C_Option;
  private boolean listCommands;

  public boolean tQuiet;
  
  public boolean doReport() {
    return (!tQuiet && scriptLevel <= scriptReportingLevel);
  }

  private boolean executionStopped;
  private boolean executionPaused;
  private boolean executionStepping;
  private boolean executing;
  private boolean isEditor;

  private long timeBeginExecution;
  private long timeEndExecution;

  private boolean mustResumeEval; // see resumeEval

  //private static int evalID;

  private Thread currentThread;
  public ScriptCompiler compiler;

  public SB outputBuffer;

  private String contextPath = "";
  public String scriptFileName;
  public String functionName;
  
  public boolean isStateScript;
  
  @Override
  public boolean isStateScript() {
    return isStateScript;
  }

  public int scriptLevel;
  public static final String CONTEXT_HOLD_QUEUE = "getEvalContextAndHoldQueue";
  public static final String CONTEXT_DELAY = "delay";

  private static final long DELAY_INTERRUPT_MS = 1000000; // testing! 

  private static final int EXEC_ASYNC = -1;
  private static final int EXEC_ERR   = 1;
  private static final int EXEC_OK    = 0;

  public static int commandHistoryLevelMax = 0;
  private static int contextDepthMax = 100; // mutable using set scriptLevelMax
  private static int scriptReportingLevel = 0;

  /**
   * set a static variable, with checking for range
   */
  @Override
  public int setStatic(int tok, int ival) {
    switch (tok) {
    case T.contextdepthmax:
      if (ival >= 10)
        contextDepthMax = ival;
      return contextDepthMax;
    case T.historylevel:
      if (ival >= 0)
        commandHistoryLevelMax = ival;
      return commandHistoryLevelMax;  
    case T.scriptreportinglevel:
      if (ival >= 0)
      scriptReportingLevel = ival;
      return scriptReportingLevel;
    }
    return 0;
  }

  // created by Compiler:
  
  public T[][] aatoken;
  private short[] lineNumbers;
  private int[][] lineIndices;

  private String script;
  private String scriptExtensions;

  @Override
  public String getScript() {
    return script;
  }

  // specific to current statement:
  
  public int pc; // program counter
  public String thisCommand;
  public String fullCommand;
  private int lineEnd;
  private int pcEnd;

  // for specific commmands:
  
  private boolean forceNoAddHydrogens;

  private boolean isEmbedded;

  private boolean isGUI;
  

  public ScriptEval() {
    // by reflection as well as directly
    currentThread = Thread.currentThread();
    //evalID++;
  }

  @Override
  public ScriptEval setViewer(Viewer vwr) {
    this.vwr = vwr;
    this.compiler = (compiler == null ? (ScriptCompiler) vwr.compiler
        : compiler);
    isJS = vwr.isSingleThreaded;
    return this;
  }

  @Override
  public void setCompiler() {
    vwr.compiler = compiler = new ScriptCompiler(vwr);
  }

  // //////////////// primary interfacing methods //////////////////

  /*
   * see Viewer.evalStringWaitStatus for how these are implemented
   */
  @Override
  public boolean compileScriptString(String script, boolean tQuiet) {
    clearState(tQuiet);
    contextPath = "[script]";
    return compileScript(null, script, debugScript);
  }

  @Override
  public boolean compileScriptFile(String filename, boolean tQuiet) {
    clearState(tQuiet);
    contextPath = filename;
    String script = getScriptFileInternal(filename, null, null, null);
    return  (script != null && compileScript(filename, script, debugScript));
  }

  @Override
  public void evaluateCompiledScript(boolean isCmdLine_c_or_C_Option,
                                     boolean isCmdLine_C_Option,
                                     boolean historyDisabled,
                                     boolean listCommands, SB outputBuffer,
                                     boolean allowThreads) {
    boolean tempOpen = this.isCmdLine_C_Option;
    this.isCmdLine_C_Option = isCmdLine_C_Option;
    chk = this.isCmdLine_c_or_C_Option = isCmdLine_c_or_C_Option;
    this.historyDisabled = historyDisabled;
    this.outputBuffer = outputBuffer;
    privateFuncs = null;
    currentThread = Thread.currentThread();
    setAllowJSThreads(allowThreads & !vwr.getBoolean(T.nodelay));
    this.listCommands = listCommands;
    timeBeginExecution = System.currentTimeMillis();
    executionStopped = executionPaused = false;
    executionStepping = false;
    executing = true;
    vwr.pushHoldRepaintWhy("runEval");
    setScriptExtensions();
    vwr.hasSelected = false;
    executeCommands(false, true);
    this.isCmdLine_C_Option = tempOpen;
    if(isStateScript)
      ScriptManager.setStateScriptVersion(vwr, null); // set by compiler
    if (vwr.hasSelected && isGUI)
      vwr.setStatusSelect(null);
    vwr.hasSelected = false;
  }

  public boolean useThreads() {
    return (!chk && !vwr.headless && !vwr.autoExit
        && vwr.haveDisplay && outputBuffer == null && allowJSThreads);
  }

  private int executeCommands(boolean isTry, boolean reportCompletion) {
    boolean haveError = false;
    try {
      if (!dispatchCommands(false, false, isTry))
        return EXEC_ASYNC;
    } catch (Error er) {
      er.printStackTrace();
      vwr.handleError(er, false);
      setErrorMessage("" + er + " " + vwr.getShapeErrorState());
      errorMessageUntranslated = "" + er;
      report(errorMessage, true);
      haveError = true;
    } catch (ScriptException e) {
      if (e instanceof ScriptInterruption && (!isTry || !e.isError)) {
        
        // ScriptInterruption will be called in Java or JavaScript
        // by a THROW command, but in that case e.isError == true
        
        // ScriptInterruption will be called in JavaScript to 
        // stop this thread and initiate a setTimeout sequence 
        // that is responsible for getting us back to the
        // current point using resumeEval again.
        // it's not a real exception, but it has that 
        // property so that it can be caught here.
        
        return EXEC_ASYNC;
      }
      if (isTry) {
        vwr.setStringProperty("_errormessage", "" + e);
        return EXEC_ERR;
      }
      setErrorMessage(e.toString());
      errorMessageUntranslated = e.getErrorMessageUntranslated();
      report(errorMessage, true);
      vwr
          .notifyError(
              (errorMessage != null
                  && errorMessage.indexOf("java.lang.OutOfMemoryError") >= 0 ? "Error"
                  : "ScriptException"), errorMessage, errorMessageUntranslated);
      haveError = true;
    }
    if (haveError || !isJS || !allowJSThreads) {
      vwr.setTainted(true);
      vwr.popHoldRepaint("executeCommands" + " "
          + (scriptLevel > 0 ? JC.REPAINT_IGNORE : ""));
    }
    timeEndExecution = System.currentTimeMillis();
    if (errorMessage == null && executionStopped)
      setErrorMessage("execution interrupted");
    else if (!tQuiet && reportCompletion)
      vwr.scriptStatus(JC.SCRIPT_COMPLETED);
    executing = chk = this.isCmdLine_c_or_C_Option = this.historyDisabled = false;
    String msg = getErrorMessageUntranslated();
    vwr.setErrorMessage(errorMessage, msg);
    if (!tQuiet && reportCompletion)
      vwr.setScriptStatus("Jmol script terminated", errorMessage,
          1 + (int) (timeEndExecution - timeBeginExecution), msg);
    return (haveError ? EXEC_ERR : EXEC_OK);
  }


  /**
   * From dispatchCommands and JmolThread resumeEval.
   * 
   * After throwing a ScriptInterruption, all statements following the current
   * one are lost. When a JavaScript timeout returns from a DELAY, MOVE, MOVETO,
   * or other sleep-requiring command, it is the ScriptContext that contains all
   * have to worry about this, because the current thread is just put to sleep,
   * not stopped, but in JavaScript, where we only have one thread, we need to
   * manage this more carefully.
   * 
   * We re-enter the halted script here, using a saved script context. The
   * program counter is incremented to skip the initiating statement, and all
   * parent contexts up the line are set with mustResumeEval = true.
   * 
   * @param sco
   */

  @Override
  public void resumeEval(Object sco) {

    ScriptContext sc = (ScriptContext) sco;

    // 
    //
    //      |
    //      |
    //     INTERRUPT---
    //     (1)         |
    //      |          |
    //      |          |
    //      |      INTERRUPT----------------     
    //      |         (2)                   |     
    //      |          |                    |      
    //      |          |                    |
    //      |     resumeEval-->(1)     MOVETO_INIT
    //   (DONE)           
    //                                 (new thread) 
    //                                 MOVETO_FINISH
    //                                      |
    //                                 resumeEval-->(2)
    //                                   
    //  In Java, this is one overall thread that sleeps
    //  during the MOVETO. But in JavaScript, the setTimeout()
    //  starts a new thread and (1) and (2) are never executed.
    //  We must run resumeEval at the end of each dispatch loop.
    //
    //  Thus, it is very important that nothing is ever executed 
    //  after dispatchCommands.
    //
    // 
    //  Functions are tricky, though. How do we restart them?
    //
    //     main
    //      |
    //      ---------test()
    //                 |
    //                 |
    //                 |
    //                 -------------------zoomTo     
    //                (2)                   |     
    //                 |                    |      
    //      ---------return                 |
    //      |                          MOVETO_INIT
    //   (DONE)           
    //                                 (new thread) 
    //                                 MOVETO_FINISH
    //                                      |
    //                                 resumeEval-->(2)
    //
    // When the zoomTo is initiated, a ScriptInterrpt is thrown, which
    // stops processing in test() and main. 
    //
    // 
    setErrorMessage(null);
    if (executionStopped || sc == null || !sc.mustResumeEval) {
      resumeViewer("resumeEval");
      return;
    }
    thisContext = sc;

    if (sc.scriptLevel > 0 && sc.why != CONTEXT_HOLD_QUEUE)
      scriptLevel = sc.scriptLevel - 1;
    if (sc.isTryCatch) {
      postProcessTry(null);
      pcResume = -1;
    } else {
      if (!executionPaused)
        sc.pc++;
      restoreScriptContext(sc, true, false, false);
      pcResume = sc.pc;
    }
    switch (executeCommands(thisContext != null && thisContext.isTryCatch,
        scriptLevel <= 0)) {
    case EXEC_ASYNC:
      break;
    case EXEC_ERR:
    case EXEC_OK:
      postProcessTry(null);
      if (executing)
        executeCommands(true, false);
      break;
    }
    pcResume = -1;
  }

  private void resumeViewer(String why) {
    vwr.setTainted(true);
    vwr.popHoldRepaint(why + (chk ? JC.REPAINT_IGNORE : ""));
    vwr.queueOnHold = false;
  }

  @Override
  public void runScript(String script) throws ScriptException {
    if (!vwr.isPreviewOnly)
      runScriptBuffer(script, outputBuffer, false);
  }

  /**
   * runs a script immediately and sends selected output to a provided SB
   * @param outputBuffer
   * @param script
   * 
   * @throws ScriptException
   */
  @Override
  public void runScriptBuffer(String script, SB outputBuffer, boolean isFuncReturn)
      throws ScriptException {
    pushContext(null, "runScriptBuffer");
    contextPath += " >> script() ";
    this.outputBuffer = outputBuffer;
    setAllowJSThreads(false);
    boolean fret = this.isFuncReturn;
    this.isFuncReturn |= isFuncReturn;    
    if (compileScript(null, script + JC.SCRIPT_EDITOR_IGNORE
        + JC.REPAINT_IGNORE, false))
      dispatchCommands(false, false, false);
    popContext(false, false);
    this.isFuncReturn = fret;
  }

  /**
   * a method for just checking a script
   * 
   * @param script
   * @return a ScriptContext that indicates errors and provides a tokenized
   *         version of the script that has passed all syntax checking, both in
   *         the compiler and the evaluator
   * 
   */
  @Override
  public ScriptContext checkScriptSilent(String script) {
    ScriptContext sc = compiler.compile(null, script, false, true, false, true);
    if (sc.errorType != null)
      return sc;
    restoreScriptContext(sc, false, false, false);
    chk = true;
    isCmdLine_c_or_C_Option = isCmdLine_C_Option = false;
    pc = 0;
    try {
      dispatchCommands(false, false, false);
    } catch (ScriptException e) {
      setErrorMessage(e.toString());
      sc = getScriptContext("checkScriptSilent");
    }
    chk = false;
    return sc;
  }

  static SB getContextTrace(Viewer vwr, ScriptContext sc, SB sb,
                            boolean isTop) {
    if (sb == null)
      sb = new SB();
    int pc = Math.min(sc.pc, sc.lineNumbers[sc.lineNumbers.length - 1]);
    sb.append(getErrorLineMessage(sc.functionName, sc.scriptFileName,
        sc.lineNumbers[pc], pc, ScriptEval.statementAsString(vwr,
            sc.statement, (isTop ? sc.iToken : 9999), false)));
    if (sc.parentContext != null)
      getContextTrace(vwr, sc.parentContext, sb, false);
    return sb;
  }

  // //////////////////////// script execution /////////////////////

  @Override
  public void setDebugging() {
    debugScript = vwr.getBoolean(T.debugscript);
    debugHigh = (debugScript && Logger.debugging);
  }


  @Override
  public void haltExecution() {
    if (isEmbedded) {
      vwr.setBooleanProperty("allowEmbeddedScripts", true);
      isEmbedded = false;
    }
    resumePausedExecution();
    executionStopped = true;
  }

  @Override
  public void pauseExecution(boolean withDelay) {
    if (chk || vwr.headless)
      return;
    if (withDelay && !isJS)
      delayScript(-100);
    vwr.popHoldRepaint("pauseExecution " + withDelay);
    executionStepping = false;
    executionPaused = true;
  }

  @Override
  public void stepPausedExecution() {
    executionStepping = true;
    executionPaused = false;
    // releases a paused thread but
    // sets it to pause for the next command.
  }

  @Override
  public void resumePausedExecution() {
    executionPaused = false;
    executionStepping = false;
    if (!tQuiet)
      vwr.setScriptStatus("Jmol script resumed", errorMessage,
          1 + (int) (timeEndExecution - timeBeginExecution), "resumed");

  }

  @Override
  public boolean isExecuting() {
    return executing && !executionStopped;
  }

  @Override
  public boolean isPaused() {
    return executionPaused;
  }

  @Override
  public boolean isStepping() {
    return executionStepping;
  }

  @Override
  public boolean isStopped() {
    return executionStopped || !isJS && currentThread != Thread.currentThread();
  }

  /**
   * when paused, indicates what statement will be next
   * 
   * @return a string indicating the statement
   */
  @Override
  public String getNextStatement() {
    return (pc < aatoken.length ? getErrorLineMessage(functionName,
        scriptFileName, getLinenumber(null), pc,
        statementAsString(vwr, aatoken[pc], -9999, debugHigh)) : "");
  }

  /**
   * used for recall of commands in the application console
   * 
   * @param pc
   * @param allThisLine
   * @param addSemi
   * @return a string representation of the command
   */
  private String getCommand(int pc, boolean allThisLine, boolean addSemi) {
    if (pc >= lineIndices.length)
      return "";
    if (allThisLine) {
      int pt0 = -1;
      String s = script;
      int pt1 = s.length();
      for (int i = 0; i < lineNumbers.length; i++) {
        if (lineNumbers[i] == lineNumbers[pc]) {
          if (pt0 < 0)
            pt0 = lineIndices[i][0];
          pt1 = lineIndices[i][1];
        } else if (lineNumbers[i] == 0 || lineNumbers[i] > lineNumbers[pc]) {
          break;
        }
      }
      if (s.indexOf('\1') >= 0)
        s = s.substring(0, s.indexOf('\1'));
      int len = s.length();
      if (pt1 == len - 1 && s.endsWith("}"))
        pt1++;
      return (pt0 >= len || pt1 < pt0 || pt1 < 0 ? "" : s.substring(
          Math.max(pt0, 0), Math.min(len, pt1)));
    }
    int ichBegin = lineIndices[pc][0];
    int ichEnd = lineIndices[pc][1];
    // (pc + 1 == lineIndices.length || lineIndices[pc + 1][0] == 0 ? script
    // .length()
    // : lineIndices[pc + 1]);
    String s = "";
    if (ichBegin < 0 || ichEnd <= ichBegin || ichEnd > script.length())
      return "";
    try {
      s = script.substring(ichBegin, ichEnd);
      if (s.indexOf("\\\n") >= 0)
        s = PT.rep(s, "\\\n", "  ");
      if (s.indexOf("\\\r") >= 0)
        s = PT.rep(s, "\\\r", "  ");
      // int i;
      // for (i = s.length(); --i >= 0 && !ScriptCompiler.eol(s.charAt(i), 0);
      // ){
      // }
      // s = s.substring(0, i + 1);
      if (s.length() > 0 && !s.endsWith(";")/*
                                             * && !s.endsWith("{") &&
                                             * !s.endsWith("}")
                                             */)
        s += ";";
    } catch (Exception e) {
      Logger.error("darn problem in Eval getCommand: ichBegin=" + ichBegin
          + " ichEnd=" + ichEnd + " len = " + script.length() + "\n" + e);
    }
    return s;
  }

  private void logDebugScript(T[] st, int ifLevel) {
    int itok = iToken;
    iToken = -9999;
    if (debugHigh) {
      if (st.length > 0)
        Logger.debug(st[0].toString());
      for (int i = 1; i < st.length; ++i)
        if (st[i] != null)
          Logger.debug(st[i].toString());
      SB strbufLog = new SB();
      String s = (ifLevel > 0 ? "                          ".substring(0,
          ifLevel * 2) : "");
      strbufLog.append(s).append(
          statementAsString(vwr, st, iToken, debugHigh));
      vwr.scriptStatus(strbufLog.toString());
    } else {
      String cmd = getCommand(pc, false, false);
      if (cmd != "")
        vwr.scriptStatus(cmd);
    }
    iToken = itok;
  }

  // /////////////// string-based evaluation support /////////////////////

  /**
   * a general-use method to evaluate a "SET" type expression.
   * @param asVariable
   * @param expr
   * 
   * @return an object of one of the following types: Boolean, Integer, Float,
   *         String, Point3f, BitSet
   */

 
  @Override
  public Object evaluateExpression(Object expr, boolean asVariable, boolean compileOnly) {
    // Text.formatText for MESSAGE and ECHO
    // prior to 12.[2/3].32 was not thread-safe for compilation.
    ScriptEval e = (new ScriptEval()).setViewer(vwr);
    try {
      // disallow end-of-script message and JavaScript script queuing
      e.thisContext = thisContext;
      e.contextVariables = contextVariables;
      e.pushContext(null, "evalExp");
      e.setAllowJSThreads(false);
    } catch (ScriptException e1) {
      //ignore
    }
    boolean exec0 = executing;
    Object o = (e.evaluate(expr, asVariable, compileOnly));
    executing = exec0;
    return o;
  }

  
  
  public void runBufferedSafely(String script, SB outputBuffer) {
    if (outputBuffer == null)
      outputBuffer = this.outputBuffer;
    ScriptEval e = (new ScriptEval()).setViewer(vwr);
    boolean exec0 = executing;
    try {
      e.runScriptBuffer(script, outputBuffer, false);
    } catch (ScriptException e1) {
      e1.printStackTrace();
      //ignore
    }
    executing = exec0;
  }

  
  
  
  public static SV runUserAction(String functionName, Object[] params, Viewer vwr) {
    ScriptEval ev = (new ScriptEval()).setViewer(vwr);
    JmolScriptFunction func = vwr.getFunction(functionName.toLowerCase());
    if (func == null)
      return null;
    try {
      Lst<SV> svparams = SV.getVariableAO(params).getList();
      ev.restoreFunction(func, svparams, null);
      ev.dispatchCommands(false, true, false);
    } catch (ScriptException e) {
      return null;
    }
    SV ret = ev.getContextVariableAsVariable("_retval", false);
    return (ret == null ? SV.vT : ret);
  }
    
  @SuppressWarnings("cast")
  private Object evaluate(Object expr, boolean asVariable, boolean compileOnly) {
    try {
      if (expr instanceof String) {
        if (compileScript(null, "e_x_p_r_e_s_s_i_o_n = " + expr, false)) {
          if (compileOnly)
            return aatoken[0];
          setStatement(aatoken[0], 1);
          return (asVariable ? parameterExpressionList(2, -1, false).get(0)
              : parameterExpressionString(2, 0));
        }
      } else if (expr instanceof T[][] && ((T[][])expr)[0] instanceof T[]) {
        // in JavaScript legacy we must checking for [][] explicitly first
        setStatement(((T[][])expr)[0], 1);
        return (asVariable ? parameterExpressionList(0, -1, false).get(0)
            : parameterExpressionString(0, -1));
      } else if (expr instanceof T[]) {
        BS bs = atomExpression((T[]) expr, 0, 0, true, false, null, false);
        return (asVariable ? SV.newV(T.bitset, bs) : bs);
      }
    } catch (Exception ex) {
      Logger.error("Error evaluating: " + expr + "\n" + ex);
    }
    return (asVariable ? SV.getVariable("ERROR") : "ERROR");
  }

  /**
   * Check a map for a WHERE phrase
   * 
   */
  @Override
  public boolean checkSelect(Map<String, SV> h, T[] where) {
    boolean ok = false;
    try {
      pushContext(null, "checkSelect");
      ok = parameterExpressionSelect(h, where);
    } catch (Exception ex) {
      Logger.error("checkSelect " + ex);
    }
    popContext(false, false);
    return ok;
  }

  /**
   * A general method to evaluate a string representing an atom set.
   * Excepts one atom expression or one per line as "OR".
   * Excepts "()" as "none".
   * 
   * 
   * @param atomExpression
   * @return is a bitset indicating the selected atoms
   * 
   */
  @Override
  public BS getAtomBitSet(Object atomExpression) {

    // called by ScriptExpr and ScriptManager
    
    if (atomExpression instanceof BS)
      return (BS) atomExpression;
    BS bs = new BS();
    boolean executing = this.executing;
    try {
      pushContext(null, "getAtomBitSet");
      String scr = "select (" + atomExpression + ")";
      scr = PT.replaceAllCharacters(scr, "\n\r", "),(");
      scr = PT.rep(scr, "()", "(none)");
      if (compileScript(null, scr, false)) {
        st = aatoken[0];
        setStatement(st, 0);
        bs = atomExpression(st, 1, 0, false, false, null, true);
      }
      popContext(false, false);
    } catch (Exception ex) {
      Logger.error("getAtomBitSet " + atomExpression + "\n" + ex);
    }
    this.executing = executing;
    return bs;
  }


  // ////////////////////// supporting methods for compilation and loading

  public boolean compileScript(String filename, String strScript,
                               boolean debugCompiler) {
    scriptFileName = filename;
    strScript = fixScriptPath(strScript, filename);
    ScriptContext sc = compiler.compile(filename, strScript, false, false,
        debugCompiler && Logger.debugging, false);
    addFunction(null);
    Map<String, ScriptFunction> pf = privateFuncs;
    restoreScriptContext(sc, false, false, false);
    privateFuncs = null;
    if (thisContext != null)
      thisContext.privateFuncs = pf;
    isStateScript = compiler.isStateScript;
    forceNoAddHydrogens = (isStateScript && script.indexOf("pdbAddHydrogens") < 0);
    String s = script;
    isGUI = (s.indexOf(JC.SCRIPT_GUI) >= 0);
    if (isGUI) {
      s = PT.rep(s, JC.SCRIPT_GUI, "");
    }
    pc = setScriptExtensions();
    if (!chk && vwr.scriptEditorVisible
        && strScript.indexOf(JC.SCRIPT_EDITOR_IGNORE) < 0)
      vwr.scriptStatus("");
    script = s;
    return !error;
  }

  private String fixScriptPath(String strScript, String filename) {
    if (filename != null && strScript.indexOf("$SCRIPT_PATH$") >= 0) {
      String path = filename;
      // we first check for paths into ZIP files and adjust accordingly
      int pt = Math.max(filename.lastIndexOf("|"), filename.lastIndexOf("/"));
      path = path.substring(0, pt + 1);
      strScript = PT.rep(strScript, "$SCRIPT_PATH$/", path);
      // now replace the variable itself
      strScript = PT.rep(strScript, "$SCRIPT_PATH$", path);
    }
    return strScript;
  }

  private int setScriptExtensions() {
    String extensions = scriptExtensions;
    if (extensions == null)
      return 0;
    int pt = extensions.indexOf(JC.SCRIPT_STEP);
    if (pt >= 0) {
      executionStepping = true;
    }
    pt = extensions.indexOf(JC.SCRIPT_START);
    if (pt < 0)
      return 0;
    pt = PT.parseInt(extensions.substring(pt + JC.SCRIPT_START.length()));
    if (pt == Integer.MIN_VALUE)
      return 0;
    for (pc = 0; pc < lineIndices.length; pc++) {
      if (lineIndices[pc][0] > pt || lineIndices[pc][1] >= pt)
        break;
    }
    if (pc > 0 && pc < lineIndices.length && lineIndices[pc][0] > pt)
      --pc;
    return pc;
  }

  /**
   * Retrieve the uncompiled script or null if failed
   * @param filename
   * @param localPath
   * @param remotePath
   * @param scriptPath
   * @return  Jmol script or null
   */
  private String getScriptFileInternal(String filename, String localPath,
                                            String remotePath, String scriptPath) {
    // from "script" command, with push/pop surrounding or vwr
    if (filename.toLowerCase().indexOf("javascript:") == 0) {
      return vwr.jsEval(filename.substring(11));
    }
    String[] data = new String[2];
    data[0] = filename;
    if (!vwr.fm.getFileDataAsString(data, -1, false, true, false)) { // first opening
      setErrorMessage("io error reading " + data[0] + ": " + data[1]);
      return null;
    }
    String movieScript = "";
    if (("\n" + data[1]).indexOf("\nJmolManifest.txt\n") >= 0) {
      String path;
      if (filename.endsWith(".all.pngj") || filename.endsWith(".all.png")) {
        path = "|state.spt";
        filename += "|";
      } else {
        if (data[1].indexOf("movie.spt") >= 0) {
          data[0] = filename + "|movie.spt";
          if (vwr.fm.getFileDataAsString(data, -1, false, true, false)) { // movie.spt
            movieScript = data[1];
          }
        }
        filename += "|JmolManifest.txt";
        data[0] = filename;
        if (!vwr.fm.getFileDataAsString(data, -1, false, true, false)) { // second entry
          setErrorMessage("io error reading " + data[0] + ": " + data[1]);
          return null;
        }
        path = FileManager.getManifestScriptPath(data[1]);
      }
      if (path != null && path.length() > 0) {
        data[0] = filename = filename.substring(0, filename.lastIndexOf("|"))
            + path;
        if (!vwr.fm.getFileDataAsString(data, -1, false, true, false)) { // third entry
          setErrorMessage("io error reading " + data[0] + ": " + data[1]);
          return null;
        }
      }
      if (filename.endsWith("|state.spt")) {
        vwr.g.setO("_pngjFile", filename.substring(0, filename.length() - 10)
            + "?");
      }   
    }
    scriptFileName = filename;
    data[1] = FileManager.getEmbeddedScript(data[1]);
    String script = fixScriptPath(data[1], data[0]);
    if (scriptPath == null) {
      scriptPath = vwr.fm.getFilePath(filename, false, false);
      scriptPath = scriptPath.substring(0,
          Math.max(scriptPath.lastIndexOf("|"), scriptPath.lastIndexOf("/")));
    }
    return FileManager.setScriptFileReferences(script, localPath, remotePath,
        scriptPath) + movieScript;
  }

  // ///////////// Jmol function support  // ///////////////

  private JmolParallelProcessor parallelProcessor;

  public int pcResume = -1;

  @Override
  @SuppressWarnings("unchecked")
  public double evalFunctionFloat(Object func, Object params, double[] values) {
    try {
      Lst<SV> p = (Lst<SV>) params;
      for (int i = 0; i < values.length; i++)
        p.get(i).value = Double.valueOf(values[i]);
      ScriptFunction f = (ScriptFunction) func;
      return SV.dValue(runFunctionAndRet(f, f.name, p, null, true, false, false));
    } catch (Exception e) {
      return Double.NaN;
    }

  }

  public SV getUserFunctionResult(String name, Lst<SV> params, SV tokenAtom)
      throws ScriptException {

    // called by ScriptExpr(getBitsetProperty) and ScriptExt(evaluateUserFunction)
    //
    
    return runFunctionAndRet(null, name, params, tokenAtom, true, true, false);
  }
  
  private SV runFunctionAndRet(JmolScriptFunction function, String name,
                           Lst<SV> params, SV tokenAtom, boolean getReturn,
                           boolean setContextPath, boolean allowThreads)
      throws ScriptException {
    
    // called by cmdFlow(TRY command), cmdFunc(user-defined command),
    // evalFunctionFloat (isosurface looking for a value), getFunctionRet
    // (above)
    // 
    if (function == null) {
      // general function call
      name = name.toLowerCase();
      function = getFunction(name);
      if (function == null)
        return null;
      if (setContextPath)
        contextPath += " >> function " + name;
    } else if (setContextPath) {
      // "try"; not from evalFunctionFloat
      contextPath += " >> " + name;
    }
    pushContext(null, "runFunctionAndRet ");
    if (allowJSThreads)
      setAllowJSThreads(allowThreads);
    boolean isTry = (function.getTok() == T.trycmd);
    thisContext.isTryCatch = isTry;
    thisContext.isFunction = !isTry;
    functionName = name;
    if (isTry) {
      resetError();
      thisContext.displayLoadErrorsSave = vwr.displayLoadErrors;
      thisContext.tryPt = ++vwr.tryPt;
      vwr.displayLoadErrors = false;
      restoreFunction(function, params, tokenAtom);
      contextVariables.put("_breakval", SV.newI(Integer.MAX_VALUE));
      contextVariables.put("_errorval", SV.newS(""));
      Map<String, SV> cv = contextVariables;
      switch(executeCommands(true, false)) {
      case EXEC_ASYNC:
        // do this later
        break;
      case EXEC_ERR:
      case EXEC_OK:
      //JavaScript will not return here after DELAY   
        postProcessTry(cv);
      }
      return null;
    } else if (function instanceof JmolParallelProcessor) {
      synchronized (function) // can't do this -- too general
      {
        parallelProcessor = (JmolParallelProcessor) function;
        restoreFunction(function, params, tokenAtom);
        dispatchCommands(false, true, false); // to load the processes
        ((JmolParallelProcessor) function).runAllProcesses(vwr);
      }
    } else {
      restoreFunction(function, params, tokenAtom);
      dispatchCommands(false, true, false);
      //JavaScript will not return here after DELAY or after what???
    }
    SV v = (getReturn ? getContextVariableAsVariable("_retval", false) : null);
    popContext(false, false);
    return v;
  }

  private void postProcessTry(Map<String, SV> cv) {
    if (thisContext == null)
      return;
    while (thisContext.tryPt > vwr.tryPt)
      popContext(false, false);
    boolean isJSReturn = (cv == null);
    if (isJSReturn) {
      cv = contextVariables;
    }
    vwr.displayLoadErrors = thisContext.displayLoadErrorsSave;
    popContext(false, false);
    String err = (String) vwr.getP("_errormessage");
    if (err.length() > 0) {
      cv.put("_errorval", SV.newS(err));
      resetError();
    }
    cv.put("_tryret", cv.get("_retval"));
    SV ret = cv.get("_tryret");
    if (ret.value != null || ret.intValue != Integer.MAX_VALUE) {
      try {
        cmdReturn(ret);
      } catch (ScriptException e) {
        e.printStackTrace();
      }
      return;
    }
    String errMsg = (String) (cv.get("_errorval")).value;
    if (errMsg.length() == 0) {
      int iBreak = (cv.get("_breakval")).intValue;
      if (iBreak != Integer.MAX_VALUE) {
        breakAt(pc - iBreak);
        return;
      }
    }
    // normal return will skip the catch
    if (pc + 1 < aatoken.length && aatoken[pc + 1][0].tok == T.catchcmd) {
      // set the intValue positive to indicate "not done" for the IF evaluation
      ContextToken ct = (ContextToken) aatoken[pc + 1][0];
      if (ct.contextVariables != null && ct.name0 != null)
        ct.contextVariables.put(ct.name0, SV.newS(errMsg));
      ct.intValue = (errMsg.length() > 0 ? 1 : -1) * Math.abs(ct.intValue);
    }
    if (isJSReturn)
      pc++;
  }

  private void breakAt(int pt) {
    if (pt < 0) {
      // if pt is a backward reference
      // this is a break within a try{...} block
      getContextVariableAsVariable("_breakval", false).intValue = -pt;
      pcEnd = pc;
      return;
    }
    // pt is to the FOR, WHILE, or SWITCH statement that is being exited
    int ptEnd = Math.abs(aatoken[pt][0].intValue);
    int tok = aatoken[pt][0].tok;
    if (tok == T.casecmd || tok == T.defaultcmd) {
      // breaking from SWITCH
      theToken = aatoken[ptEnd--][0];
      int ptNext = Math.abs(theToken.intValue);
      if (theToken.tok != T.end)
        theToken.intValue = -ptNext;
    } else {
      // breaking from FOR or WHILE (or PROCESS?)
      pc = -1;
      while (pc != pt && thisContext != null) {
        while (thisContext != null
            && !ScriptCompiler.isBreakableContext(thisContext.token.tok))
          popContext(true, false);
        pc = thisContext.pc;
        popContext(true, false);
      }
    }
    pc = ptEnd;
  }

  /**
   * note that functions requiring motion cannot be run in JavaScript
   * 
   * @param f
   * @param params
   * @param tokenAtom
   * @throws ScriptException
   */
  private void restoreFunction(JmolScriptFunction f, Lst<SV> params,
                               SV tokenAtom) throws ScriptException {
    ScriptFunction function = (ScriptFunction) f;
    aatoken = function.aatoken;
    lineNumbers = function.lineNumbers;
    lineIndices = function.lineIndices;
    script = function.script;
    pc = 0;
    if (function.names != null) {
      contextVariables = new Hashtable<String, SV>();
      function.setVariables(contextVariables, params);
    }
    if (tokenAtom != null)
      contextVariables.put("_x", tokenAtom);
  }

  
  ////////////////////////// defined atom sets ////////////////////////
  
  public void clearDefinedVariableAtomSets() {
    vwr.definedAtomSets.remove("# variable");
  }

  /**
   * support for @xxx or define xxx commands
   * 
   */
  private void defineSets() {
    if (!vwr.definedAtomSets.containsKey("# static")) {
      for (int i = 0; i < JC.predefinedStatic.length; i++)
        defineAtomSet(JC.predefinedStatic[i]);
      defineAtomSet("# static");
    }
    if (vwr.definedAtomSets.containsKey("# variable"))
      return;
    for (int i = 0; i < JC.predefinedVariable.length; i++)
      defineAtomSet(JC.predefinedVariable[i]);
    // Now, define all the elements as predefined sets

    // name ==> elemno=n for all standard elements, isotope-blind
    // _Xx ==> elemno=n for of all elements, isotope-blind
    for (int i = Elements.elementNumberMax; --i >= 0;) {
      String definition = " elemno=" + i;
      defineAtomSet("@" + Elements.elementNameFromNumber(i) + definition);
      defineAtomSet("@_" + Elements.elementSymbolFromNumber(i) + definition);
    }
    // name ==> _e=nn for each alternative element
    for (int i = Elements.firstIsotope; --i >= 0;) {
      String definition = "@" + Elements.altElementNameFromIndex(i) + " _e="
          + Elements.altElementNumberFromIndex(i);
      defineAtomSet(definition);
    }
    // these variables _e, _x can't be more than two characters
    // name ==> _isotope=iinn for each isotope
    // _T ==> _isotope=iinn for each isotope
    // _3H ==> _isotope=iinn for each isotope
    for (int i = Elements.altElementMax; --i >= Elements.firstIsotope;) {
      int ei = Elements.altElementNumberFromIndex(i);
      String def = " _e=" + ei;
      String definition = "@_" + Elements.altElementSymbolFromIndex(i);
      defineAtomSet(definition + def);

      definition = "@_" + Elements.altIsotopeSymbolFromIndex(i);
      defineAtomSet(definition + def);
      definition = "@_" + Elements.altIsotopeSymbolFromIndex2(i);
      defineAtomSet(definition + def);

      definition = "@" + Elements.altElementNameFromIndex(i);
      if (definition.length() > 1)
        defineAtomSet(definition + def);

      // @_12C _e=6
      // @_C12 _e=6
      int e = Elements.getElementNumber(ei);
      ei = Elements.getNaturalIsotope(e);
      if (ei > 0) {
        def = Elements.elementSymbolFromNumber(e);
        defineAtomSet("@_" + def + ei + " _e=" + e);
        defineAtomSet("@_" + ei + def + " _e=" + e);
      }
    }
    defineAtomSet("# variable");
  }

  private void defineAtomSet(String script) {
    if (script.indexOf("#") == 0) {
      vwr.definedAtomSets.put(script, Boolean.TRUE);
      return;
    }
    ScriptContext sc = compiler.compile("#predefine", script, true, false,
        false, false);
    if (sc.errorType != null) {
      vwr
          .scriptStatus("JmolConstants.java ERROR: predefined set compile error:"
              + script + "\ncompile error:" + sc.errorMessageUntranslated);
      return;
    }

    if (sc.getTokenCount() != 1) {
      vwr
          .scriptStatus("JmolConstants.java ERROR: predefinition does not have exactly 1 command:"
              + script);
      return;
    }
    T[] statement = sc.getToken(0);
    if (statement.length <= 2) {
      vwr.scriptStatus("JmolConstants.java ERROR: bad predefinition length:"
          + script);
      return;
    }
    int tok = statement[1].tok;
    if (!T.tokAttr(tok, T.identifier) && !T.tokAttr(tok, T.predefinedset)) {
      vwr.scriptStatus("JmolConstants.java ERROR: invalid variable name:"
          + script);
      return;
    }
    String name = ((String) statement[1].value).toLowerCase();
    if (name.startsWith("dynamic_"))
      name = "!" + name.substring(8);
    vwr.definedAtomSets.put(name, statement);
  }

  public BS lookupIdentifierValue(String identifier) throws ScriptException {
    
    // called by ScriptExpr and ScriptExt
    
    // all variables and possible residue names for PDB
    // or atom names for non-pdb atoms are processed here.

    // priority is given to a defined variable.

    BS bs = lookupValue(identifier, false);
    if (bs != null)
      return BSUtil.copy(bs);

    // next we look for names of groups (PDB) or atoms (non-PDB)
    bs = getAtomBits(T.identifier, identifier);
    return (bs == null ? new BS() : bs);
  }

  private BS lookupValue(String setName, boolean plurals)
      throws ScriptException {
    if (chk) {
      return new BS();
    }
    defineSets();
    setName = setName.toLowerCase();
    Object value = vwr.definedAtomSets.get(setName);
    boolean isDynamic = false;
    if (value == null) {
      value = vwr.definedAtomSets.get("!" + setName);
      isDynamic = (value != null);
    }
    if (value instanceof BS)
      return (BS) value;
    if (value instanceof T[]) { // j2s OK -- any Array here
      pushContext(null, "lookupValue");
      BS bs = atomExpression((T[]) value, -2, 0, true, false, null, true);
      popContext(false, false);
      if (!isDynamic)
        vwr.definedAtomSets.put(setName, bs);
      return bs;
    }
    if (setName.equals("water")) {
      BS bs = vwr.ms.getAtoms(T.solvent, null);
      if (!isDynamic)
        vwr.definedAtomSets.put(setName, bs);
      return bs;
    }
    if (plurals)
      return null;
    int len = setName.length();
    if (len < 5) // iron is the shortest
      return null;
    if (setName.charAt(len - 1) != 's')
      return null;
    if (setName.endsWith("ies"))
      setName = setName.substring(0, len - 3) + 'y';
    else
      setName = setName.substring(0, len - 1);
    return lookupValue(setName, true);
  }

  @Override
  public void deleteAtomsInVariables(BS bsDeleted) {
    for (Map.Entry<String, Object> entry : vwr.definedAtomSets.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof BS) {
        BSUtil.deleteBits((BS) value, bsDeleted);
        if (!entry.getKey().startsWith("!"))
          vwr
              .g.setUserVariable("@" + entry.getKey(), SV.newV(T.bitset, value));
      }
    }
  }
  
  
  // ///////////////// Script context support //////////////////////

 @Override
  public ScriptContext getThisContext() {
    return thisContext;
  }

  private void clearState(boolean tQuiet) {
    thisContext = null;
    scriptLevel = 0;
    setErrorMessage(null);
    contextPath = "";
    this.tQuiet = tQuiet;
  }

  @Override
  public void pushContextDown(String why) {
    scriptLevel--;
    pushContext2(null, why);
  }

  private void pushContext(ContextToken token, String why)
      throws ScriptException {
    if (scriptLevel == contextDepthMax)
      error(ERROR_tooManyScriptLevels);
    pushContext2(token, why);
  }

  private void pushContext2(ContextToken token, String why) {
    thisContext = getScriptContext(why);
    thisContext.token = token;
    if (token == null) {
      scriptLevel = ++thisContext.scriptLevel;
    } else {
      thisContext.scriptLevel = -1;
      contextVariables = new Hashtable<String, SV>();
      if (token.contextVariables != null)
        for (String key : token.contextVariables.keySet())
          ScriptCompiler.addContextVariable(contextVariables, key);
    }
    if (debugHigh || isCmdLine_c_or_C_Option)
      Logger.info("-->>----------------------".substring(0,
          Math.min(15, scriptLevel + 5))
          + scriptLevel
          + " "
          + scriptFileName
          + " "
          + token
          + " "
          + thisContext.id + " " + why + " path=" + thisContext.contextPath);
  }

  @Override
  public ScriptContext getScriptContext(String why) {
    ScriptContext context = new ScriptContext();
    if (debugHigh)
      Logger.info("creating context " + context.id + " for " + why + " path=" + contextPath);
    context.why = why;
    context.scriptLevel = scriptLevel;
    context.parentContext = thisContext;
    context.contextPath = contextPath;
    context.scriptFileName = scriptFileName;
    context.parallelProcessor = parallelProcessor;
    context.functionName = functionName;
    context.script = script;
    context.lineNumbers = lineNumbers;
    context.lineIndices = lineIndices;    
    context.saveTokens(aatoken);
    context.statement = st;
    context.statementLength = slen;
    context.pc = context.pc0 = pc;
    context.lineEnd = lineEnd;
    context.pcEnd = pcEnd;
    context.iToken = iToken;
    context.theToken = theToken;
    context.theTok = theTok;
    context.outputBuffer = outputBuffer;
    context.vars = contextVariables;
    context.isStateScript = isStateScript;

    context.errorMessage = errorMessage;
    context.errorType = errorType;
    context.iCommandError = iCommandError;
    context.chk = chk;
    context.executionStepping = executionStepping;
    context.executionPaused = executionPaused;
    context.scriptExtensions = scriptExtensions;
    context.isEditor = isEditor;

    context.mustResumeEval = mustResumeEval;
    context.allowJSThreads = allowJSThreads;
    return context;
  }

  void popContext(boolean isFlowCommand, boolean statementOnly) {
    if (thisContext == null)
      return;
    if (thisContext.scriptLevel > 0)
      scriptLevel = thisContext.scriptLevel - 1;
    // we must save (and thus NOT restore) the current statement
    // business when doing push/pop for commands like FOR and WHILE
    ScriptContext scTemp = (isFlowCommand ? getScriptContext("popFlow") : null);
    restoreScriptContext(thisContext, true, isFlowCommand, statementOnly);
    if (scTemp != null)
      restoreScriptContext(scTemp, true, false, true);
    if (debugHigh || isCmdLine_c_or_C_Option)
      Logger.info("--<<------------".substring(0,
          Math.min(15, scriptLevel + 5))
          + (scriptLevel + 1)
          + " "
          + scriptFileName
          + " isFlow "
          + isFlowCommand
          + " thisContext="
          + (thisContext == null ? "" : "" + thisContext.id)
          + " pc=" + pc + "-->" + pc + " path=" + (thisContext == null ? "" : thisContext.contextPath));
  }

  public void restoreScriptContext(ScriptContext context, boolean isPopContext,
                                   boolean isFlowCommand, boolean statementOnly) {

    executing = !chk;
    if (context == null)
      return;
    if (!isFlowCommand) {
      st = context.statement;
      slen = context.statementLength;
      pc = context.pc;
      lineEnd = context.lineEnd;
      pcEnd = context.pcEnd;
      if (statementOnly)
        return;
    }
    if (context.privateFuncs != null)
      privateFuncs = context.privateFuncs;
    mustResumeEval = context.mustResumeEval;
    script = context.script;
    lineNumbers = context.lineNumbers;
    lineIndices = context.lineIndices;
    aatoken = context.restoreTokens();
    contextVariables = context.vars;
    scriptExtensions = context.scriptExtensions;
    isEditor = context.isEditor;

    if (isPopContext) {
      contextPath = context.contextPath;
      int pt = (contextPath == null ? -1 : contextPath.indexOf(" >> "));
      if (pt >= 0)
        contextPath = contextPath.substring(0, pt);      
      scriptFileName = context.scriptFileName;
      parallelProcessor = context.parallelProcessor;
      functionName = context.functionName;
      iToken = context.iToken;
      theToken = context.theToken;
      theTok = context.theTok;

      outputBuffer = context.outputBuffer;
      isStateScript = context.isStateScript;
      thisContext = context.parentContext;
      allowJSThreads = context.allowJSThreads;
      if (debugHigh || isCmdLine_c_or_C_Option)
        Logger.info("--r------------".substring(0,
            Math.min(15, scriptLevel + 5))
            + scriptLevel
            + " "
            + scriptFileName
            + " isPop "
            + isPopContext
            + " isFlow "
            + isFlowCommand
            + " context.id="
            + context.id + " pc=" + pc + "-->" + context.pc + " " + contextPath);
    } else {
      error = (context.errorType != null);
      //isComplete = context.isComplete;
      errorMessage = context.errorMessage;
      errorMessageUntranslated = context.errorMessageUntranslated;
      iCommandError = context.iCommandError;
      errorType = context.errorType;
    }
  }

  // /////////////// error message support /////////////////

  public void setException(ScriptException sx, String msg, String untranslated) {
    // from ScriptException, while initializing
    sx.untranslated = (untranslated == null ? msg : untranslated);
    boolean isThrown = "!".equals(untranslated);
    errorType = msg;
    iCommandError = pc;
    if (sx.message == null) {
      sx.message = "";
      return;
    }
    String s = ScriptEval.getContextTrace(vwr,
        getScriptContext("setException"), null, true).toString();
    while (thisContext != null && !thisContext.isTryCatch)
      popContext(false, false);
    if (sx.message.indexOf(s) < 0) {
      sx.message += s;
      sx.untranslated += s;
    }
    resumeViewer(isThrown ? "throw context" : "scriptException");
    if (isThrown || thisContext != null || chk
        || msg.indexOf(JC.NOTE_SCRIPT_FILE) >= 0)
      return;
    Logger.error("eval ERROR: " + s + "\n" + toString());
    if (vwr.autoExit)
      vwr.exitJmol();
  }

  @SuppressWarnings("unchecked")
  public static String statementAsString(Viewer vwr, T[] statement,
                                         int iTok, boolean doLogMessages) {
    if (statement.length == 0)
      return "";
    SB sb = new SB();
    int tok = statement[0].tok;
    switch (tok) {
    case T.nada:
      return (String) statement[0].value;
    case T.end:
      if (statement.length == 2
          && (statement[1].tok == T.function || statement[1].tok == T.parallel))
        return ((ScriptFunction) (statement[1].value)).toString();
    }
    boolean useBraces = true;// (!Token.tokAttr(tok,
    // Token.atomExpressionCommand));
    boolean inBrace = false;
    boolean inClauseDefine = false;
    boolean setEquals = (statement.length > 1 && tok == T.set
        && statement[0].value.equals("")
        && (statement[0].intValue == '=' || statement[0].intValue == '#') && statement[1].tok != T.expressionBegin);
    int len = statement.length;
    for (int i = 0; i < len; ++i) {
      T token = statement[i];
      if (token == null) {
        len = i;
        break;
      }
      if (iTok == i - 1)
        sb.append(" <<");
      if (i != 0)
        sb.appendC(' ');
      if (i == 2 && setEquals) {
        if ((setEquals = (token.tok != T.opEQ)) || statement[0].intValue == '#') {
          sb.append(setEquals ? "= " : "== ");
          if (!setEquals)
            continue;
        }
      }
      if (iTok == i && token.tok != T.expressionEnd)
        sb.append("<<<<");
      switch (token.tok) {
      case T.expressionBegin:
        if (useBraces)
          sb.append("{");
        continue;
      case T.expressionEnd:
        if (inClauseDefine && i == statement.length - 1)
          useBraces = false;
        if (useBraces)
          sb.append("}");
        continue;
      case T.leftsquare:
      case T.rightsquare:
        break;
      case T.leftbrace:
      case T.rightbrace:
        inBrace = (token.tok == T.leftbrace);
        break;
      case T.define:
        if (i > 0 && ((String) token.value).equals("define")) {
          sb.append("@");
          if (i + 1 < statement.length
              && statement[i + 1].tok == T.expressionBegin) {
            if (!useBraces)
              inClauseDefine = true;
            useBraces = true;
          }
          continue;
        }
        break;
      case T.on:
        sb.append("true");
        continue;
      case T.off:
        sb.append("false");
        continue;
      case T.select:
        break;
      case T.integer:
        sb.appendI(token.intValue);
        continue;
      case T.point3f:
      case T.point4f:
      case T.bitset:
        sb.append(SV.sValue(token)); // list
        continue;
      case T.hash:
         if (Boolean.TRUE == ((Map<String, Object>)token.value).get("$_BINARY_$")) {
           sb.append("<BINARY DATA>");
           continue;
         }          
        //$FALL-THROUGH$
      case T.varray:
        sb.append(((SV) token).escape()); // list
        continue;
      case T.inscode:
        sb.appendC('^');
        continue;
      case T.spec_seqcode_range:
        if (token.intValue != Integer.MAX_VALUE)
          sb.appendI(token.intValue);
        else
          sb.append(Group.getSeqcodeStringFor(getSeqCode(token)));
        token = statement[++i];
        sb.appendC(' ');
        // if (token.intValue == Integer.MAX_VALUE)
        sb.append(inBrace ? "-" : "- ");
        //$FALL-THROUGH$
      case T.spec_seqcode:
        if (token.intValue != Integer.MAX_VALUE)
          sb.appendI(token.intValue);
        else
          sb.append(Group.getSeqcodeStringFor(getSeqCode(token)));
        continue;
      case T.spec_chain:
        sb.append("*:");
        sb.append(vwr.getChainIDStr(token.intValue));
        continue;
      case T.spec_alternate:
        sb.append("*%");
        if (token.value != null)
          sb.append(token.value.toString());
        continue;
      case T.spec_model:
        sb.append("*/");
        //$FALL-THROUGH$
      case T.spec_model2:
      case T.decimal:
        if (token.intValue < Integer.MAX_VALUE) {
          sb.append(Escape.escapeModelFileNumber(token.intValue));
        } else {
          sb.append("" + token.value);
        }
        continue;
      case T.spec_resid:
        sb.appendC('[');
        int ptr = token.intValue * 6 + 1;
        sb.append(Group.standardGroupList.substring(ptr, ptr + 3).trim());
        sb.appendC(']');
        continue;
      case T.spec_name_pattern:
        sb.appendC('[');
        sb.appendO(token.value);
        sb.appendC(']');
        continue;
      case T.spec_atom:
        sb.append("*.");
        break;
      case T.cell:
        if (token.value instanceof P3d) {
          P3d pt = (P3d) token.value;
          sb.append("cell=").append(Escape.eP(pt));
          continue;
        }
        break;
      case T.string:
        sb.append("\"").appendO(token.value).append("\"");
        continue;
      case T.opEQ:
      case T.opLE:
      case T.opGE:
      case T.opGT:
      case T.opLT:
      case T.opNE:
        // not quite right -- for "inmath"
        if (token.intValue == T.property) {
          sb.append((String) statement[++i].value).append(" ");
        } else if (token.intValue != Integer.MAX_VALUE)
          sb.append(T.nameOf(token.intValue)).append(" ");
        break;
      case T.trycmd:
        continue;
      case T.end:
        sb.append("end");
        continue;
      default:
        if (T.tokAttr(token.tok, T.identifier) || !doLogMessages)
          break;
        sb.appendC('\n').append(token.toString()).appendC('\n');
        continue;
      }
      if (token.value != null)
        sb.append(token.value.toString());
    }
//    if (iTok >= len - 1 && iTok != 9999)
//      sb.append(" <<");
    return sb.toString();
  }

  ///////////// shape get/set properties ////////////////

  /**
   * called by Viewer in setting up a PyMOL scene.
   */
  @Override
  public String setObjectPropSafe(String id, int tokCommand) {
    try {
      return setObjectProp(id, tokCommand, -1);
    } catch (ScriptException e) {
      return null;
    }
  }

  protected void setAtomProp(String prop, Object value, BS bs) {
    setShapePropertyBs(JC.SHAPE_BALLS, prop, value, bs);
  }

  public void restrictSelected(boolean isBond, boolean doInvert) {
    
    // called by ScriptParam
    
    if (!chk)
      sm.restrictSelected(isBond, doInvert);
  }

  //////////////////// showing strings /////////////////

  public void showString(String str) {
    // called by ScriptExt and ScriptError
    showStringPrint(str, false);
  }

  @Override
  public void showStringPrint(String s, boolean mustDo) {
    if (chk || s == null)
      return;
    if (outputBuffer == null)
      vwr.showString(s, mustDo);
    else
      appendBuffer(s, mustDo);
  }

  public void report(String s, boolean isError) {
    if (chk || isError && s.indexOf(" of try:") >= 0)
      return;
    if (outputBuffer == null)
      vwr.scriptStatus(s);
    else 
      appendBuffer(s, isError);
  }

  private void appendBuffer(String str, boolean mustDo) {
    if (mustDo || isFuncReturn || Logger.isActiveLevel(Logger.LEVEL_INFO))
      outputBuffer.append(str).appendC('\n');
  }

  /*
   * ****************************************************************
   * =============== command processing checks ===============================
   */

  private void addProcess(Lst<T[]> vProcess, int pc, int pt) {
    if (parallelProcessor == null)
      return;
    T[][] statements = new T[pt][];
    for (int i = 0; i < vProcess.size(); i++)
      statements[i + 1 - pc] = vProcess.get(i);
    ScriptContext context = getScriptContext("addProcess");
    context.saveTokens(statements);
    context.pc = 1 - pc;
    context.pcEnd = pt;
    parallelProcessor.addProcess("p" + (++iProcess), context);
  }

  /**
   * checks to see if there is a pause condition, during which commands can
   * still be issued, but with the ! first.
   * 
   * @return false if there was a problem
   * @throws ScriptException
   */
  private boolean checkContinue() throws ScriptException {
    if (executionStopped)
      return false;
    if (executionStepping && isCommandDisplayable(pc)) {
      vwr.scriptStatusMsg("Next: " + getNextStatement(),
          "stepping -- type RESUME to continue");
      executionPaused = true;
    } else if (!executionPaused) {
      return true;
    }
    if (Logger.debugging) {
      Logger.debug("script execution paused at command " + (pc + 1) + " level "
          + scriptLevel + ": " + thisCommand);
    }
    refresh(false);
    boolean doShowPC = true;
    while (executionPaused) {
      if (!isJS) {
        Thread.yield();
      }
      if (isEditor && doShowPC)
        notifyScriptEditor(pc);
      doShowPC = false;
      vwr.popHoldRepaint("pause " + JC.REPAINT_IGNORE);
      // does not actually do a repaint
      // but clears the way for interaction
      String script = vwr.getInsertedCommand();
      if (script.length() > 0) {
        resumePausedExecution();
        setErrorMessage(null);
        ScriptContext scSave = getScriptContext("script insertion");
        pc--; // in case there is an error, we point to the PAUSE command
        try {
          runScript(script);
        } catch (Exception e) {
          setErrorMessage("" + e);
        } catch (Error er) {
          setErrorMessage("" + er);
        }
        if (error) {
          report(errorMessage, true);
          setErrorMessage(null);
        }
        restoreScriptContext(scSave, true, false, false);
        if (!script.startsWith("resume\1") && !script.startsWith("step\1"))
          pauseExecution(false);
      }
      doDelay(ScriptDelayThread.PAUSE_DELAY);
      // JavaScript will not reach this point, 
      // but no need to pop anyway, because
      // we will be out of this thread.
      vwr.pushHoldRepaintWhy("pause");
    }
    notifyResumeStatus();
    // once more to trap quit during pause
    return !error && !executionStopped;
  }

  public void delayScript(int millis) {
    if (vwr.autoExit)
      return;
    stopScriptThreads();
    if (vwr.captureParams != null && millis > 0) {
      vwr.captureParams.put("captureDelayMS", Integer.valueOf(millis));
    }
    scriptDelayThread = new ScriptDelayThread(this, vwr, millis);
    if (isJS && allowJSThreads) {
      // abort this; wait for delay to come back.
       pc = aatoken.length;
    }
    scriptDelayThread.run();
  }

  /**
   * 
   * @param millis
   *        negative here bypasses max check
   * @throws ScriptException
   */
  private void doDelay(int millis) throws ScriptException {
    if (!useThreads())
      return;
    if (isJS)
      throw new ScriptInterruption(this, CONTEXT_DELAY, millis);
    delayScript(millis);
  }

  @Override
  public boolean evalParallel(ScriptContext context,
                                  ShapeManager shapeManager) {
    
    // now in ScriptExt
    
    return getCmdExt().evalParallel(context, shapeManager);
  }

  /**
   * provides support for the script editor
   * 
   * @param i
   * @return true if displayable (not a } )
   */
  private boolean isCommandDisplayable(int i) {
    if (i >= aatoken.length || i >= pcEnd || aatoken[i] == null)
      return false;
    return (lineIndices[i][1] > lineIndices[i][0]);
  }

  /**
   * load a static file asynchronously
   */
  @Override
  public void loadFileResourceAsync(String fileName) throws ScriptException {
    loadFileAsync(null, fileName, -Math.abs(fileName.hashCode()), false);    
  }
  
  /**
   * Allows asynchronous file loading from the LOAD or SCRIPT command. Saves the
   * context, initiates a FileLoadThread instance. When the file loading
   * completes, the file data (sans filename) is saved in the FileManager cache
   * under cache://localLoad_xxxxx. Context is resumed at this command in the
   * script, and the file is then retrieved from the cache. Only run from
   * JSmol/HTML5 when vwr.isJS;
   * 
   * Incompatibilities:
   * 
   * LOAD and SCRIPT commands, load() function only;
   * 
   * only one "?" per LOAD command
   * 
   * @param prefix
   * @param filename
   *        or null if end of LOAD command and now just clearing out cache
   * @param i
   * @param doClear
   *        ensures only one file is in the cache for a given type
   * @return cached file name if it exists
   * @throws ScriptException
   */
  public String loadFileAsync(String prefix, String filename, int i,
                              boolean doClear) throws ScriptException {
    // note that we will never know the actual file name
    // so we construct one and point to it in the scriptContext
    // with a key to this point in the script. 
    
    if (vwr.fm.cacheGet(filename, false) != null) {
      cancelFileThread();
      return filename;
    }
    if (prefix != null)
      prefix = "cache://local" + prefix;
    String key = pc + "_" + i + "_" + filename;
    String cacheName;
    if (thisContext == null) {
      // add temp context
      pushContext(null, "loadFileAsync");
    }
    if (thisContext.htFileCache == null) {
      thisContext.htFileCache = new Hashtable<String, String>();
    }
    cacheName = thisContext.htFileCache.get(key);
    if (cacheName != null && cacheName.length() > 0) {
      cancelFileThread();
      //no, problems with isosurface "?" map "?": popContext(false, false);
      vwr.queueOnHold = false;
      if ("#CANCELED#".equals(cacheName) || "#CANCELED#".equals(vwr.fm.cacheGet(cacheName, false)))
        evalError("#CANCELED#", null);
      return cacheName;
    }
    thisContext.htFileCache.put(key,
        cacheName = prefix + System.currentTimeMillis());
//    if (fileLoadThread != null && i >= 0)
//      evalError("#CANCELED#", null);
    if (doClear)
      vwr.cacheFileByName(prefix + "*", false);
    fileLoadThread = new FileLoadThread(this, vwr, filename, key, cacheName);
    if (vwr.testAsync)
      fileLoadThread.start();
    else
      fileLoadThread.run();
    if (i < 0) // no need to hang on to this - never "canceled"
      fileLoadThread = null;
    throw new ScriptInterruption(this, "load", 1);
  }

  private void cancelFileThread() {
    // file has been loaded
    fileLoadThread = null;
    if (thisContext != null && thisContext.why == "loadFileAsync") {
      // remove temp context
      popContext(false, false);
    }
  }

  @SuppressWarnings("unchecked")
  private void logLoadInfo(String msg, boolean isData) {
    if (msg.length() > 0)
      Logger.info(msg);
    SB sb = new SB();
    int modelCount = vwr.ms.mc;
    if (modelCount > 1 && !isData)
      sb.append((vwr.am.isMovie ? vwr.am.getFrameCount() + " frames"
          : modelCount + " models") + "\n");
    for (int i = 0; i < modelCount; i++) {
      Map<String, Object> moData = (Map<String, Object>) vwr
          .ms.getInfo(i, "moData");
      if (moData == null || !moData.containsKey("mos"))
        continue;
      sb.appendI(((Lst<Map<String, Object>>) moData.get("mos")).size())
          .append(" molecular orbitals in model ")
          .append(vwr.getModelNumberDotted(i)).append("\n");
    }
    if (sb.length() > 0)
     showString(sb.toString());
  }

  @Override
  public void notifyResumeStatus() {
    if (!chk && !executionStopped && !executionStepping && !executionPaused) {
      boolean isInterrupt = (error || executionStopped);
      vwr.scriptStatus("script execution " + (isInterrupt ? "interrupted" : "resumed"));
    }
  }

  /**
   * Refresh the display NOW
   * @param doDelay 
   * @throws ScriptException 
   * 
   */
  public void refresh(boolean doDelay) throws ScriptException {
    if (chk)
      return;
    vwr.setTainted(true);
    vwr.requestRepaintAndWait("refresh cmd");
    if (isJS && doDelay)
      doDelay(10); // need this to update JavaScript display
  }

  @Override
  public void stopScriptThreads() {
    if (scriptDelayThread != null) {
      scriptDelayThread.interrupt();
      scriptDelayThread = null;
    }
    if (fileLoadThread != null) {
      fileLoadThread.interrupt();
      fileLoadThread.resumeEval();
      cancelFileThread();
    }
  }
  
  // from ScriptExt 

  public String getErrorLineMessage2() {
    return getErrorLineMessage(functionName, scriptFileName,
        getLinenumber(null), pc,
        statementAsString(vwr, st, -9999, debugHigh));
  }
  
  public int getLinenumber(ScriptContext c) {
    return (c == null ? lineNumbers[pc] : c.lineNumbers[c.pc]);
  }

  
  ///////////////////////////////////////////////////////////////////////
  ///////////////////////// Jmol script commands ////////////////////////
  ///////////////////////////////////////////////////////////////////////
  
  /**
   * 
   * @param isSpt
   * @param fromFunc
   * @param isTry 
   * @return false only when still working through resumeEval
   * @throws ScriptException
   */
  public boolean dispatchCommands(boolean isSpt, boolean fromFunc, boolean isTry)
      throws ScriptException {
    if (sm == null)
      sm = vwr.shm;
    debugScript = debugHigh = false;
    if (!chk)
      setDebugging();
    if (pcEnd == 0)
      pcEnd = Integer.MAX_VALUE;
    if (lineEnd == 0)
      lineEnd = Integer.MAX_VALUE;
    if (aatoken == null)
      return true;
    if (!tQuiet) {
      tQuiet = (vwr.getInt(T.showscript) < 0);
    }
    boolean allowJSInterrupt = (
        isJS 
        && !fromFunc 
        && useThreads() 
        && vwr.getInt(T.showscript) >= 0);
    commandLoop(allowJSInterrupt);
    if (chk)
      return true;
    String script = vwr.getInsertedCommand();
    if (!"".equals(script))
      runScriptBuffer(script, null, false);
    else if (isSpt && debugScript && vwr.getBoolean(T.messagestylechime))
      vwr.getChimeMessenger().update(null);
    if (!mustResumeEval && !allowJSInterrupt || fromFunc)
      return true;
    if (!isTry && mustResumeEval || thisContext == null) {
      boolean done = (thisContext == null);
      resumeEval(thisContext);
      mustResumeEval = false;
      return done;
    }
    return true;
  }

  private void commandLoop(boolean allowJSInterrupt) throws ScriptException {
    String lastCommand = "";
    boolean isForCheck = false; // indicates the stage of the for command loop
    Lst<T[]> vProcess = null;
    long lastTime = System.currentTimeMillis();

    if (debugScript && debugHigh && !chk) {
      for (int i = pc; i < aatoken.length && i < pcEnd; i++) {
        Logger.info("Command " + i);
        if (debugScript)
          logDebugScript(aatoken[i], 0);
      }
      Logger.info("-----");
    }

    boolean isFirst = true;
    for (; pc < aatoken.length && pc < pcEnd; pc++) {
      if (allowJSInterrupt) {
        // every 1 s check for interruptions
        if (!executionPaused && System.currentTimeMillis() - lastTime > DELAY_INTERRUPT_MS) {
          pc--;
          doDelay(-1);
        }
        lastTime = System.currentTimeMillis();
      }
      if (!chk && (!executionStepping || !isFirst) && !checkContinue())
        break;
      isFirst = false;
      if (pc >= lineNumbers.length || lineNumbers[pc] > lineEnd)
        break;
      if (debugHigh) {
        long timeBegin = 0;
        timeBegin = System.currentTimeMillis();
        vwr.scriptStatus("Eval.dispatchCommands():" + timeBegin);
        vwr.scriptStatus(script);
      }

      if (debugScript && !chk)
        Logger.info("Command " + pc
            + (thisContext == null ? "" : " path=" + thisContext.contextPath));
      theToken = (aatoken[pc].length == 0 ? null : aatoken[pc][0]);
      // when checking scripts, we can't check statments
      // containing @{...}
      if (!historyDisabled && !chk && scriptLevel <= commandHistoryLevelMax
          && !tQuiet) {
        String cmdLine = getCommand(pc, true, true);
        if (theToken != null
            && cmdLine.length() > 0
            && !cmdLine.equals(lastCommand)
            && (theToken.tok == T.function || theToken.tok == T.parallel || !T
                .tokAttr(theToken.tok, T.flowCommand)))
          vwr.addCommand(lastCommand = cmdLine);
      }
      if (!chk && allowJSInterrupt) {
        String script = vwr.getInsertedCommand();
        if (!"".equals(script))
          runScript(script);
      }
      if (!setStatement(aatoken[pc], 1)) {
        // chk cannot process @{...}
//        Logger.info(getCommand(pc, true, false)
//            + " -- STATEMENT CONTAINING @{} SKIPPED");
        continue;
      }
      thisCommand = getCommand(pc, false, true);
      if (debugHigh || debugScript)
        Logger.info(thisCommand);
      String nextCommand = getCommand(pc + 1, false, true);
      fullCommand = thisCommand
          + (nextCommand.startsWith("#") ? nextCommand : "");
      getToken(0);
      iToken = 0;
      if ((listCommands || !chk && scriptLevel > 0) && !isJS) {
        int milliSecDelay = vwr.getInt(T.showscript);
        if (listCommands || milliSecDelay > 0) {
          if (milliSecDelay > 0)
            delayScript(-milliSecDelay);
          vwr.scriptEcho("$[" + scriptLevel + "." + lineNumbers[pc] + "."
              + (pc + 1) + "] " + thisCommand);
        }
      }
      if (vProcess != null
          && (theTok != T.end || slen < 2 || st[1].tok != T.process)) {
        vProcess.addLast(st);
        continue;
      }
      if (chk) {
        if (isCmdLine_c_or_C_Option)
          Logger.info(thisCommand);
        if (slen == 1 && st[0].tok != T.function && st[0].tok != T.parallel)
          continue;
      } else {
        if (debugScript)
          logDebugScript(st, 0);
        if (scriptLevel == 0 && vwr.g.logCommands)
          vwr.log(thisCommand);
        if (debugHigh && theToken != null)
          Logger.debug(theToken.toString());
        if (!isJS && isEditor && scriptLevel == 0) { 
          notifyScriptEditor(pc);
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            // TODO
          }
        }
      }
      if (theToken == null)
        continue;
      int tok = theToken.tok;
      switch (tok) {
      case T.set:
        cmdSet();
        continue;
      case T.forcmd:
        isForCheck = cmdFor(tok, isForCheck);
        continue;
      case T.process:
        pushContext((ContextToken) theToken, "PROCESS");
        if (parallelProcessor != null)
          vProcess = new Lst<T[]>();
        continue;
      default:
        if (T.tokAttr(tok, T.flowCommand)) {
          isForCheck = cmdFlow(tok, isForCheck, vProcess);
          if (theTok == T.process)
            vProcess = null; // "end process"
          continue;
        }
        processCommand(tok);
        setCursorWait(false);
        if (tok != T.step && executionStepping) {
          executionPaused = (isCommandDisplayable(pc + 1));
        }
      }
    }
  }

  //  public void terminateAfterStep() {
  //    pc = pcEnd;
  //  }

  private void notifyScriptEditor(int pc) {
    vwr.notifyScriptEditor((lineIndices[pc][0] << 16) | lineIndices[pc][1],
        null);
  }

  private void processCommand(int tok) throws ScriptException {
    if (T.tokAttr(theToken.tok, T.shapeCommand)) {
      processShapeCommand(tok);
      return;
    }
    switch (tok) {
    case T.nada:
      if (!chk && vwr.getBoolean(T.messagestylechime))
        vwr.getChimeMessenger().showHash(outputBuffer, (String) theToken.value);
      break;
    case T.push:
      pushContext((ContextToken) theToken, "PUSH");
      break;
    case T.pop:
      popContext(true, false);
      break;
    case T.colon:
      break;
    case T.animation:
      cmdAnimation();
      break;
    case T.background:
      cmdBackground(1);
      break;
    case T.bind:
      cmdBind();
      break;
    case T.bondorder:
      cmdBondorder();
      break;
    case T.cd:
      cmdCD();
      break;
    case T.center:
      cmdCenter(1);
      break;
    case T.color:
      cmdColor();
      break;
    case T.define:
      cmdDefine();
      break;
    case T.delay:
      cmdDelay();
      break;
    case T.delete:
      cmdDelete();
      break;
    case T.depth:
      cmdSlab(true);
      break;
    case T.display:
      cmdDisplay(true);
      break;
    case T.exit: // flush the queue and...
    case T.quit: // quit this only if it isn't the first command
      if (chk)
        break;
      if (pc > 0 && theToken.tok == T.exit && !vwr.autoExit) {
        vwr.clearScriptQueue();
      }
      executionStopped = (pc > 0 || !vwr.g.useScriptQueue);
      break;
    case T.exitjmol:
      if (chk)
        return;
      if (outputBuffer != null)
        Logger.warn(outputBuffer.toString());
      vwr.exitJmol();
      break;
    case T.file:
      cmdFile();
      break;
    case T.fixed:
      cmdFixed();
      break;
    case T.font:
      cmdFont(-1, 0);
      break;
    case T.frame:
    case T.model:
      cmdModel(1);
      break;
    case T.identifier:
      cmdFunc(); // when a function is a command
      break;
    case T.getproperty:
      cmdGetProperty();
      break;
    case T.gotocmd: //
      if (vwr.headless)
        break;
      cmdGoto(true);
      break;
    case T.help:
      cmdHelp();
      break;
    case T.hide:
      cmdDisplay(false);
      break;
    case T.hbond:
      cmdHbond();
      break;
    case T.history:
      cmdHistory(1);
      break;
    case T.hover:
      cmdHover();
      break;
    case T.initialize:
      switch (slen) {
      case 1:
        if (!chk)
          vwr.initialize(!isStateScript, false);
        break;
      case 2:
        if (tokAt(1) == T.inchi) {
          vwr.getInchi(null, null, null);
          if (chk) {
          } else {
            if (Viewer.isJS) {
            	// using vwr.showString here, as this is just a general message
              vwr.showString("InChI module initialized", false);
              doDelay(1);
            }
          }
          break;
        }
        //$FALL-THROUGH$
      default:
        bad();
      }
      break;
    case T.javascript:
      cmdScript(T.javascript, null, null);
      break;
    case T.load:
      cmdLoad();
      break;
    case T.log:
      cmdLog();
      break;
    case T.loop:
      cmdLoop();
      break;
    case T.message:
      cmdMessage();
      break;
    case T.move:
      cmdMove();
      break;
    case T.moveto:
      cmdMoveto();
      break;
    case T.pause: // resume is done differently
      cmdPause();
      break;
    case T.print:
      cmdPrint();
      break;
    case T.prompt:
      cmdPrompt();
      break;
    case T.scale:
      cmdScale(1);
      break;
    case T.undo:
    case T.redo:
    case T.redomove:
    case T.undomove:
      cmdUndoRedo(tok);
      break;
    case T.refresh:
      refresh(true);
      break;
    case T.reset:
      cmdReset();
      break;
    case T.restrict:
      cmdRestrict();
      break;
    case T.resume:
      if (slen == 1) {
        if (!chk)
          resumePausedExecution();
        break;
      }
      //$FALL-THROUGH$
    case T.restore:
      cmdRestore();
      break;
    case T.returncmd:
      cmdReturn(null);
      break;
    case T.rotate:
      cmdRotate(false, false);
      break;
    case T.rotateSelected:
      cmdRotate(false, true);
      break;
    case T.save:
      cmdSave();
      break;
    case T.script:
      cmdScript(T.script, null, null);
      break;
    case T.select:
      cmdSelect(1);
      break;
    case T.selectionhalos:
      cmdSelectionHalos(1);
      break;
    case T.slab:
      cmdSlab(false);
      break;
    //case Token.slice:
    // slice();
    //break;
    case T.spin:
      cmdRotate(true, false);
      break;
    case T.ssbond:
      cmdSsbond();
      break;
    case T.step:
      if (cmdPause())
        stepPausedExecution();
      break;
    case T.structure:
      cmdStructure();
      break;
    case T.subset:
      cmdSubset();
      break;
    case T.sync:
      cmdSync();
      break;      
    case T.throwcmd:
      cmdThrow();
      break;
    case T.timeout:
      cmdTimeout(1);
      break;
    case T.translate:
      cmdTranslate(false);
      break;
    case T.translateSelected:
      cmdTranslate(true);
      break;
    case T.unbind:
      cmdUnbind();
      break;
    case T.var:
      break;
    case T.vibration:
      cmdVibration();
      break;
    case T.zap:
      cmdZap(true);
      break;
    case T.zoom:
      cmdZoom(false);
      break;
    case T.zoomTo:
      cmdZoom(true);
      break;
    default:
      checkExtension(theToken.tok);
    }
  }

  private void checkExtension(int tok) throws ScriptException {
    switch (tok) {
    case T.assign:
    case T.cache:
    case T.calculate:
    case T.capture:
    case T.centerat:
    case T.compare:
    case T.configuration:
    case T.connect:
    case T.console:
    case T.hbond: // hbond connect
    case T.image:
    case T.invertSelected:
    case T.stereo:
    case T.macro:
    case T.mapproperty:
    case T.minimize:
    case T.modelkitmode:
    case T.modulation:
    case T.mutate:
    case T.data:
    case T.navigate:
    case T.plot:
    case T.quaternion:
    case T.ramachandran:
    case T.show:
    case T.write:      
      getCmdExt().dispatch(tok, false, st);
      break;
    default:
      System.out.println(T.nameOf(tok) + " is not a command");
      error(ERROR_unrecognizedCommand);
    }
  }

  private void processShapeCommand(int tok) throws ScriptException {
    int iShape = 0;
    switch (tok) {
    case T.axes:
      iShape = JC.SHAPE_AXES;
      break;
    case T.backbone:
      iShape = JC.SHAPE_BACKBONE;
      break;
    case T.boundbox:
      iShape = JC.SHAPE_BBCAGE;
      break;
    case T.cartoon:
      iShape = JC.SHAPE_CARTOON;
      break;
    case T.cgo:
      iShape = JC.SHAPE_CGO;
      break;
    case T.contact:
      iShape = JC.SHAPE_CONTACT;
      break;
    case T.dipole:
      iShape = JC.SHAPE_DIPOLES;
      break;
    case T.dots:
      iShape = JC.SHAPE_DOTS;
      break;
    case T.draw:
      iShape = JC.SHAPE_DRAW;
      break;
    case T.echo:
      iShape = JC.SHAPE_ECHO;
      break;
    case T.ellipsoid:
      iShape = JC.SHAPE_ELLIPSOIDS;
      break;
    case T.frank:
      iShape = JC.SHAPE_FRANK;
      break;
    case T.geosurface:
      iShape = JC.SHAPE_GEOSURFACE;
      break;
    case T.halo:
      iShape = JC.SHAPE_HALOS;
      break;
    case T.isosurface:
      iShape = JC.SHAPE_ISOSURFACE;
      break;
    case T.label:
      iShape = JC.SHAPE_LABELS;
      break;
    case T.lcaocartoon:
      iShape = JC.SHAPE_LCAOCARTOON;
      break;
    case T.measurements:
    case T.measure:
      iShape = JC.SHAPE_MEASURES;
      break;
    case T.meshRibbon:
      iShape = JC.SHAPE_MESHRIBBON;
      break;
    case T.mo:
      iShape = JC.SHAPE_MO;
      break;
    case T.nbo:
      iShape = JC.SHAPE_NBO;
      break;
    case T.plot3d:
      iShape = JC.SHAPE_PLOT3D;
      break;
    case T.pmesh:
      iShape = JC.SHAPE_PMESH;
      break;
    case T.polyhedra:
      iShape = JC.SHAPE_POLYHEDRA;
      break;
    case T.ribbon:
      iShape = JC.SHAPE_RIBBONS;
      break;
    case T.rocket:
      iShape = JC.SHAPE_ROCKETS;
      break;
    case T.spacefill: // aka cpk
      iShape = JC.SHAPE_BALLS;
      break;
    case T.star:
      iShape = JC.SHAPE_STARS;
      break;
    case T.strands:
      iShape = JC.SHAPE_STRANDS;
      break;
    case T.struts:
      iShape = JC.SHAPE_STRUTS;
      break;
    case T.trace:
      iShape = JC.SHAPE_TRACE;
      break;
    case T.unitcell:
      iShape = JC.SHAPE_UCCAGE;
      break;
    case T.vector:
      iShape = JC.SHAPE_VECTORS;
      break;
    case T.wireframe:
      iShape = JC.SHAPE_STICKS;
      break;
    default:
      error(ERROR_unrecognizedCommand);
    }

    // check for "OFF/delete/NONE" with no shape to avoid loading it at all
    if (sm.getShape(iShape) == null && slen == 2) {
      switch (st[1].tok) {
      case T.off:
      case T.delete:
      case T.none:
        return;
      }
    }

    // atom objects:

    switch (tok) {
    case T.backbone:
    case T.cartoon:
    case T.meshRibbon:
    case T.ribbon:
    case T.rocket:
    case T.strands:
    case T.trace:
      setSizeBio(iShape);
      return;
    case T.dots:
    case T.geosurface:
      cmdDots(iShape);
      return;
    case T.halo:
    case T.spacefill: // aka cpk
    case T.star:
      setSize(iShape, (tok == T.halo ? -1000d : 1d));
      return;
    case T.label:
      cmdLabel(1, null);
      return;
    case T.vector:
      cmdVector();
      return;
    case T.wireframe:
      cmdWireframe();
      return;
    }

    // other objects:

    switch (tok) {
    case T.axes:
      cmdAxes(1);
      return;
    case T.boundbox:
      cmdBoundbox(1);
      return;
    case T.echo:
      cmdEcho(1);
      return;
    case T.frank:
      cmdFrank(1);
      return;
    case T.unitcell:
      cmdUnitcell(1);
      return;
    case T.ellipsoid:
    case T.measurements:
    case T.measure:
    case T.polyhedra:
    case T.struts:
      getCmdExt().dispatch(iShape, false, st);
      return;
    case T.cgo:
    case T.contact:
    case T.dipole:
    case T.draw:
    case T.isosurface:
    case T.lcaocartoon:
    case T.mo:
    case T.nbo:
    case T.plot3d:
    case T.pmesh:
      getIsoExt().dispatch(iShape, false, st);
      return;
    }
  }

  private void cmdAnimation() throws ScriptException {
    boolean animate = false;
    switch (getToken(1).tok) {
    case T.on:
      animate = true;
      //$FALL-THROUGH$
    case T.off:
      if (!chk)
        vwr.setAnimationOn(animate);
      break;
    case T.morph:
      int morphCount = (int) floatParameter(2);
      if (!chk)
        vwr.am.setMorphCount(Math.abs(morphCount));
      break;
    case T.display:
      iToken = 2;
      BS bs = (tokAt(2) == T.all ? null : atomExpressionAt(2));
      checkLength(iToken + 1);
      if (!chk)
        vwr.setAnimDisplay(bs);
      return;
    case T.frame:
      if (isArrayParameter(2))
        setFrameSet(2);
      else
        cmdModel(2);
      break;
    case T.mode:
      double startDelay = 1,
      endDelay = 1;
      if (slen > 5)
        bad();
      int animationMode = T.getTokFromName(paramAsStr(2));
      switch (animationMode) {
      case T.once:
        startDelay = endDelay = 0;
        break;
      case T.loop:
      case T.palindrome:
        break;
      default:
        invArg();
      }
      if (slen >= 4) {
        startDelay = endDelay = floatParameter(3);
        if (slen == 5)
          endDelay = floatParameter(4);
      }
      if (!chk)
        vwr.am.setAnimationReplayMode(animationMode, startDelay, endDelay);
      break;
    case T.direction:
      int i = 2;
      int direction = 0;
      switch (tokAt(i)) {
      case T.minus:
        direction = -intParameter(++i);
        break;
      case T.plus:
        direction = intParameter(++i);
        break;
      case T.integer:
        direction = intParameter(i);
        break;
      default:
        invArg();
      }
      checkLength(++i);
      if (direction != 1 && direction != -1)
        errorStr2(ERROR_numberMustBe, "-1", "1");
      if (!chk)
        vwr.am.setAnimationDirection(direction);
      break;
    case T.fps:
      setIntProperty("animationFps", intParameter(checkLast(2)));
      break;
    default:
      frameControl(1);
    }
  }

  private void setFrameSet(int i) throws ScriptException {
    int[] frames = (int[]) expandDoubleArray(
        doubleParameterSet(i, 0, Integer.MAX_VALUE), 1, false);
    checkLength(iToken + 1);
    if (chk)
      return;
    Map<String, Object> movie = new Hashtable<String, Object>();
    if (frames.length > 0)
      movie.put("frames", frames);
    movie.put("currentFrame", Integer.valueOf(0));
    vwr.am.setMovie(movie);
  }

  private void cmdAxes(int index) throws ScriptException {
    // axes (index==1) or set axes (index==2)
    TickInfo tickInfo = tickParamAsStr(index, true, true, false);
    index = iToken + 1;
    int tok = tokAt(index);
    String type = optParameterAsString(index).toLowerCase();
    if (slen == index + 1 && PT.isOneOf(type, ";window;unitcell;molecular;")) {
      setBooleanProperty("axes" + type, true);
      return;
    }
    switch (tok) {
    case T.offset:
      setFloatProperty("axisOffset", floatParameter(++index));
      checkLast(iToken);
      return;
    case T.center:
      setShapeProperty(JC.SHAPE_AXES, "origin",
          centerParameter(index + 1, null));
      checkLast(iToken);
      return;
    case T.type:
      String s = stringParameter(index + 1);
      if (!PT.isOneOf(s, ";a;b;c;ab;ac;bc;abc;"))
        s = null;
      setShapeProperty(JC.SHAPE_AXES, "type", s);
      checkLast(iToken);
      return;
    case T.scale:
      setFloatProperty("axesScale", floatParameter(checkLast(++index)));
      return;
    case T.label:
      switch (tok = tokAt(index + 1)) {
      case T.off:
      case T.on:
        checkLength(index + 2);
        setShapeProperty(JC.SHAPE_AXES,
            "labels" + (tok == T.on ? "On" : "Off"), null);
        return;
      }
      String sOrigin = null;
      switch (slen - index) {
      case 7:
        // axes labels "X" "Y" "Z" "-X" "-Y" "-Z"
        setShapeProperty(JC.SHAPE_AXES, "labels", new String[] {
            paramAsStr(++index), paramAsStr(++index), paramAsStr(++index),
            paramAsStr(++index), paramAsStr(++index), paramAsStr(++index) });
        break;
      case 5:
        sOrigin = paramAsStr(index + 4);
        //$FALL-THROUGH$
      case 4:
        // axes labels "X" "Y" "Z" [origin]
        setShapeProperty(JC.SHAPE_AXES, "labels", new String[] {
            paramAsStr(++index), paramAsStr(++index), paramAsStr(++index),
            sOrigin });
        break;
      default:
        bad();
      }
      return;
    }
    // axes position [x y] or [x y %] 
    // axes position [x y] or [x y %] "xyz"
    if (type.equals("position")) {
      P3d xyp;
      if (tokAt(++index) == T.off) {
        xyp = new P3d();
      } else {
        xyp = xypParameter(index);
        if (xyp == null)
          invArg();
        index = iToken;
        setShapeProperty(JC.SHAPE_AXES, "axes2", (tokAt(index + 1) == T.string ? stringParameter(++index) : null));
      }
      setShapeProperty(JC.SHAPE_AXES, "position", xyp);
      return;
    }
    int mad10 = getSetAxesTypeMad10(index);
    if (chk || mad10 == Integer.MAX_VALUE)
      return;
    setObjectMad10(JC.SHAPE_AXES, "axes", mad10);
    if (tickInfo != null)
      setShapeProperty(JC.SHAPE_AXES, "tickInfo", tickInfo);
  }

  private void cmdBackground(int i) throws ScriptException {
    getToken(i);
    int argb;
    if (theTok == T.image) {
      // background IMAGE "xxxx.jpg"
      Object o = null;
      switch (tokAt(++i)) {
      case T.barray:
      case T.hash:
        o = getToken(i).value;
        break;
      default:
        String file = paramAsStr(checkLast(i));
        if (chk)
          return;
        if (file.equalsIgnoreCase("none") || file.length() == 0) {
          vwr.setBackgroundImage(null, null);
          return;
        }
        o = (file.startsWith(";base64,") ?  new BArray(Base64.decodeBase64(file)) : file);
      }
      if (vwr.fm.loadImage(o, null, !useThreads()))
          throw new ScriptInterruption(this,"backgroundImage", 1);
      return;
    }
    if (theTok == T.none || isColorParam(i)) {
      argb = getArgbParamLast(i, true);
      if (chk)
        return;
      setObjectArgb("background", argb);
      vwr.setBackgroundImage(null, null);
      return;
    }
    colorShape(-theTok, i + 1, true);
  }

  private void cmdBind() throws ScriptException {
    /*
     * bind "MOUSE-ACTION" actionName bind "MOUSE-ACTION" "script" 
     *   not implemented: range [xyrange] [xyrange]
     */
    String mouseAction = stringParameter(1);
    String name = paramAsStr(2);
    checkLength(3);
    if (!chk)
      vwr.bindAction(mouseAction, name);
  }

  private void cmdBondorder() throws ScriptException {
    checkLength(-3);
    int order = 0;
    switch (getToken(1).tok) {
    case T.integer:
    case T.decimal:
      if ((order = Edge.getBondOrderFromFloat(floatParameter(1))) == Edge.BOND_ORDER_NULL)
        invArg();
      break;
    default:
      if ((order = getBondOrderFromString(paramAsStr(1))) == Edge.BOND_ORDER_NULL)
        invArg();
      // generic partial can be indicated by "partial n.m"
      if (order == Edge.BOND_PARTIAL01 && tokAt(2) == T.decimal) {
        order = getPartialBondOrderFromFloatEncodedInt(st[2].intValue);
      }
    }
    setShapeProperty(JC.SHAPE_STICKS, "bondOrder", Integer.valueOf(order));
  }

  private void cmdBoundbox(int index) throws ScriptException {
    TickInfo tickInfo = tickParamAsStr(index, false, true, false);
    index = iToken + 1;
    double scale = 1;
    if (tokAt(index) == T.scale) {
      scale = floatParameter(++index);
      if (!chk && scale == 0)
        invArg();
      index++;
      if (index == slen) {
        if (!chk)
          vwr.ms.setBoundBox(null, null, true, scale);
        return;
      }
    }
    boolean byCorner = (tokAt(index) == T.corners);
    if (byCorner)
      index++;
    if (isCenterParameter(index)) {
      Object[] ret = new Object[1];
      int index0 = index;
      P3d pt1 = centerParameter(index, ret);
      index = iToken + 1;
      if (byCorner || isCenterParameter(index)) {
        // boundbox CORNERS {expressionOrPoint1} {expressionOrPoint2}
        // boundbox {expressionOrPoint1} {vector}
        P3d pt2 = (byCorner ? centerParameter(index, ret) : getPoint3f(index, true, true));
        index = iToken + 1;
        if (!chk)
          vwr.ms.setBoundBox(pt1, pt2, byCorner, scale);
      } else if (ret[0] != null && ret[0] instanceof BS) {
        // boundbox {expression}
        if (!chk)
          vwr.calcBoundBoxDimensions((BS) ret[0], scale);
      } else if (ret[0] == null && tokAt(index0) == T.dollarsign) {
        if (chk)
          return;
        P3d[] bbox = getObjectBoundingBox(objectNameParameter(++index0));
        if (bbox == null)
          invArg();
        vwr.ms.setBoundBox(bbox[0], bbox[1], true, scale);
        index = iToken + 1;
      } else {
        invArg();
      }
      if (index == slen)
        return;
    }
    int mad10 = getSetAxesTypeMad10(index);
    if (chk || mad10 == Integer.MAX_VALUE)
      return;
    if (tickInfo != null)
      setShapeProperty(JC.SHAPE_BBCAGE, "tickInfo", tickInfo);
    setObjectMad10(JC.SHAPE_BBCAGE, "boundbox", mad10);
  }

  private void cmdCD() throws ScriptException {
    if (chk)
      return;
    String dir = (slen == 1 ? null : paramAsStr(1));
    showString(vwr.cd(dir));
  }

  private void cmdCenter(int i) throws ScriptException {
    // from center (atom) or from zoomTo under conditions of not
    // windowCentered()
    if (slen == 1) {
      vwr.setNewRotationCenter(null);
      return;
    }
    if (slen == 4 && tokAt(2) == T.unitcell)
      i = 2;
    P3d center = centerParameter(i, null);
    if (center == null)
      invArg();
    if (!chk)
      vwr.setNewRotationCenter(center);
    }

  private void cmdColor() throws ScriptException {
    int i = 1;
    String strColor = (tokAt(1) == T.string ? stringParameter(1) : null);
    if (isColorParam(1)) {
      theTok = T.atoms;
    } else {
      int argb = 0;
      i = 2;
      int tok = getToken(1).tok;
      if (tok == T.string) {
        tok = T.getTokFromName(strColor);
        if (tok == T.nada)
          tok = T.string;
      }
      switch (tok) {
      case T.dollarsign:
        setObjectProperty();
        return;
      case T.altloc:
      case T.amino:
      case T.nucleic:
      case T.chain:
      case T.fixedtemp:
      case T.formalcharge:
      case T.group:
      case T.hydrophobicity:
      case T.insertion:
      case T.jmol:
      case T.molecule:
      case T.monomer:
      case T.none:
      case T.opaque:
      case T.partialcharge:
      case T.polymer:
      case T.property:
      case T.rasmol:
      case T.pymol:
      case T.spacefill:
      case T.shapely:
      case T.straightness:
      case T.structure:
      case T.surfacedistance:
      case T.temperature:
      case T.translucent:
      case T.user:
      case T.vanderwaals:
        theTok = T.atoms;
        i = 1;
        break;
      case T.string:
        i = 2;
        if (isArrayParameter(i)) {
          strColor = strColor += "="
              + SV.sValue(SV.getVariableAS(stringParameterSet(i))).replace(
                  '\n', ' ');
          i = iToken + 1;
        }
        boolean isTranslucent = (tokAt(i) == T.translucent);
        if (!chk)
          vwr.setPropertyColorScheme(strColor, isTranslucent, true);
        if (isTranslucent)
          ++i;
        if (tokAt(i) == T.range || tokAt(i) == T.absolute) {
          double min = floatParameter(++i);
          double max = floatParameter(++i);
          if (!chk)
            vwr.cm.setPropertyColorRange(min, max);
        }
        return;
      case T.range:
      case T.absolute:
        double min = floatParameter(2);
        double max = floatParameter(checkLast(3));
        if (!chk)
          vwr.cm.setPropertyColorRange(min, max);
        return;
      case T.background:
        argb = getArgbParamLast(2, true);
        if (!chk)
          setObjectArgb("background", argb);
        return;
      case T.define:
      case T.bitset:
      case T.expressionBegin:
        i = -1;
        theTok = T.atoms;
        break;
      case T.rubberband:
        argb = getArgbParamLast(2, false);
        if (!chk)
          vwr.cm.setRubberbandArgb(argb);
        return;
      case T.highlight:
      case T.selectionhalos:
        i = 2;
        if (tokAt(2) == T.opaque)
          i++;
        argb = getArgbParamLast(i, true);
        if (chk)
          return;
        sm.loadShape(JC.SHAPE_HALOS);
        setShapeProperty(JC.SHAPE_HALOS,
            (tok == T.selectionhalos ? "argbSelection" : "argbHighlight"),
            Integer.valueOf(argb));
        return;
      case T.axes:
      case T.boundbox:
      case T.unitcell:
      case T.identifier:
      case T.hydrogen:
        // color element
        String str = paramAsStr(1);
        if (checkToken(2)) {
          argb = getToken(2).tok;
          switch (argb) {
          case T.none:
            argb = T.jmol;
            break;
          case T.jmol:
          case T.rasmol:
          case T.pymol:
            break;
          default:
            argb = getArgbParam(2);
          }
        }
        if (argb == 0)
          error(ERROR_colorOrPaletteRequired);
        checkLast(iToken);
        if (str.equalsIgnoreCase("axes")
            || StateManager.getObjectIdFromName(str) >= 0) {
          setObjectArgb(str, argb);
          return;
        }
        if (setElementColor(str, argb))
          return;
        invArg();
        break;
      case T.isosurface:
      case T.contact:
        setShapeProperty(JC.shapeTokenIndex(tok), "thisID",
            MeshCollection.PREVIOUS_MESH_ID);
        break;
      }
    }
    colorShape(-theTok, i, false);
  }

  private void cmdDefine() throws ScriptException {
    // note that the standard definition depends upon the
    // current state. Once defined, a setName is the set
    // of atoms that matches the definition at that time.
    // adding DYMAMIC_ to the beginning of the definition
    // allows one to create definitions that are recalculated
    // whenever they are used. When used, "DYNAMIC_" is dropped
    // so, for example:
    // define DYNAMIC_what selected and visible
    // and then
    // select what
    // will return different things at different times depending
    // upon what is selected and what is visible
    // but
    // define what selected and visible
    // will evaluate the moment it is defined and then represent
    // that set of atoms forever.

    if (slen < 3 || !(getToken(1).value instanceof String))
      invArg();
    String setName = ((String) getToken(1).value).toLowerCase();
    if (PT.parseInt(setName) != Integer.MIN_VALUE)
      invArg();
    if (chk)
      return;
    boolean isSite = setName.startsWith("site_");
    boolean isDynamic = (setName.indexOf("dynamic_") == 0);
    if (isDynamic || isSite) {
      T[] code = new T[slen];
      for (int i = slen; --i >= 0;)
        code[i] = st[i];
      vwr.definedAtomSets
          .put("!" + (isSite ? setName : setName.substring(8)), code);
      //if (!isSite)
      //vwr.addStateScript(thisCommand, false, true); removed for 12.1.16
    } else {
      BS bs = atomExpressionAt(2);
      vwr.definedAtomSets.put(setName, bs);
      if (!chk)
        vwr.g.setUserVariable("@" + setName, SV.newV(T.bitset, bs));
    }
  }

  private void cmdDelay() throws ScriptException {
    int millis = 0;
    switch (getToken(1).tok) {
    case T.on: // this is auto-provided as a default
      millis = 1;
      break;
    case T.integer:
      millis = intParameter(1) * 1000;
      break;
    case T.decimal:
      millis = (int) (floatParameter(1) * 1000);
      break;
    default:
      error(ERROR_numberExpected);
    }
    refresh(false);
    doDelay(Math.abs(millis));
  }

  private void cmdDelete() throws ScriptException {
    if (tokAt(1) == T.dollarsign) {
      if (slen == 4 && optParameterAsString(2).equals("saved") && slen == 4) {
        vwr.stm.deleteSaved(optParameterAsString(3));
        if (doReport())
          report(GT.o(GT.$("show saved: {0}"), vwr.stm.listSavedStates()), false);
        return;
      }
      setObjectProperty();
      return;
    }
    BS bs = (slen == 1 ? null : atomExpression(st, 1, 0, true, false, null,
        false));
    if (chk)
      return;
    if (bs == null)
      bs = vwr.getAllAtoms();
    int nDeleted = vwr.deleteAtoms(bs, false);
    if (doReport())
      report(GT.i(GT.$("{0} atoms deleted"), nDeleted), false);
  }

  private void cmdDisplay(boolean isDisplay) throws ScriptException {
    BS bs = null;
    int addRemove = 0;
    int i = 1;
    int tok;
    switch (tok = tokAt(1)) {
    case T.add:
    case T.remove:
      addRemove = tok;
      tok = tokAt(++i);
      break;
    }
    boolean isGroup = (tok == T.group);
    if (isGroup)
      tok = tokAt(++i);
    switch (tok) {
    case T.dollarsign:
      setObjectProperty();
      return;
    case T.nada:
      break;
    default:
      if (slen == 4 && tokAt(2) == T.bonds)
        bs = BondSet.newBS(BSUtil.newBitSet2(0, vwr.ms.bondCount), null);
      else
        bs = atomExpressionAt(i);
    }
    if (chk)
      return;
    if (bs instanceof BondSet) {
      vwr.ms.displayBonds((BondSet) bs, isDisplay);
      return;
    }
    vwr.displayAtoms(bs, isDisplay, isGroup, addRemove, tQuiet);
  }

  private void cmdDots(int iShape) throws ScriptException {
    if (!chk)
      sm.loadShape(iShape);
    setShapeProperty(iShape, "init", null);
    double value = Double.NaN;
    EnumType type = EnumType.ABSOLUTE;
    int ipt = 1;
    boolean isOnly = false;
    while (true) {
      switch (getToken(ipt).tok) {
      case T.only:
        isOnly = true;
        //$FALL-THROUGH$
      case T.on:
        value = 1;
        type = EnumType.FACTOR;
        break;
      case T.off:
        value = 0;
        break;
      case T.ignore:
        setShapeProperty(iShape, "ignore", atomExpressionAt(ipt + 1));
        ipt = iToken + 1;
        continue;
      case T.decimal:
        isOnly = (tokAt(ipt + 1) == T.only || floatParameter(ipt) < 0);
        break;
      case T.integer:
        int dotsParam = intParameter(ipt);
        if (tokAt(ipt + 1) == T.radius) {
          ipt++;
          setShapeProperty(iShape, "atom", Integer.valueOf(dotsParam));
          setShapeProperty(iShape, "radius",
              Double.valueOf(floatParameter(++ipt)));
          if (tokAt(++ipt) == T.color) {
            setShapeProperty(iShape, "colorRGB",
                Integer.valueOf(getArgbParam(++ipt)));
            ipt++;
          }
          if (getToken(ipt).tok != T.bitset)
            invArg();
          setShapeProperty(iShape, "dots", st[ipt].value);
          return;
        }
        break;
      }
      break;
    }
    RadiusData rd = (Double.isNaN(value) ? encodeRadiusParameter(ipt, isOnly,
        true) : new RadiusData(null, value, type, VDW.AUTO));
    if (rd == null)
      return;
    if (Double.isNaN(rd.value))
      invArg();
    if (isOnly) {
      restrictSelected(false, false);
    }
    setShapeSize(iShape, rd);
  }

  private void cmdEcho(int index)
      throws ScriptException {
    if (chk)
      return;
    String text = optParameterAsString(index);
    boolean doRefresh = true;
    if (vwr.ms.getEchoStateActive()) {
      if (text.startsWith("\1")) {
        // no reporting, just screen echo, from mouseManager key press
        text = text.substring(1);
        doRefresh = false;
      }
      if (text != null)
        setShapeProperty(JC.SHAPE_ECHO, "text", text);
    }
    if (doRefresh && vwr.getRefreshing() && text != null && !text.startsWith("%SCALE"))
      showString(vwr.formatText(text));
  }

  private void cmdFile() throws ScriptException {
    int file = intParameter(checkLast(1));
    if (chk)
      return;
    int modelIndex = vwr.ms.getModelNumberIndex(file * 1000000 + 1, false,
        false);
    int modelIndex2 = -1;
    if (modelIndex >= 0) {
      modelIndex2 = vwr.ms.getModelNumberIndex((file + 1) * 1000000 + 1, false,
          false);
      if (modelIndex2 < 0)
        modelIndex2 = vwr.ms.mc;
      modelIndex2--;
    }
    vwr.setAnimationOn(false);
    vwr.am.setAnimationDirection(1);
    vwr.setAnimationRange(modelIndex, modelIndex2);
    vwr.setCurrentModelIndex(-1);
  }

  private void cmdFixed() throws ScriptException {
    BS bs = (slen == 1 ? null : atomExpressionAt(1));
    if (chk)
      return;
    vwr.setMotionFixedAtoms(bs);
  }

  @SuppressWarnings("unchecked")
  private boolean cmdFor(int tok, boolean isForCheck) throws ScriptException {
    ContextToken cmdToken = (ContextToken) theToken;
    int pt = st[0].intValue;  
    SV[] forVars = cmdToken.forVars;
    int[] pts = new int[2];
    Object bsOrList = null;
    SV forVal = null;
    SV forVar = null;
    int inTok = 0;
    boolean isOK = true;
    boolean isMinusMinus = false;
    int j = 0;
    String key = null;
    if (isForCheck && forVars != null) {
      
      // for xx IN [...] or for xx FROM [...]
      
      tok = T.in;
      // i in x, already initialized
      forVar = forVars[0];
      forVal = forVars[1];
      bsOrList = forVars[1].value;
      // nth time through
      j = ++forVal.intValue;
      if (forVal.tok == T.integer) {
        // values are stored in value as [i1 i2]
        isMinusMinus = (j < 0);
        int i1 = ((int[]) bsOrList)[0];
        int i2 = ((int[]) bsOrList)[1];
        isOK = (i1 != i2 && (i2 < i1) == isMinusMinus);
        if (isOK)
          forVar.intValue = ((int[]) bsOrList)[0] = i1 + (isMinusMinus ? -1 : 1); 
        j = -1;
      } else if (forVal.tok == T.varray) {
        isOK = (j <= ((Lst<SV>) bsOrList).size());
        if (isOK)
          forVar.setv(SV.selectItemVar(forVal));
        j = -1;
      } else {
        isBondSet = bsOrList instanceof BondSet;
        j = ((BS) bsOrList).nextSetBit(j);
        isOK = (j >= 0);
      }
    } else {
      // for (i = 1; i < 3; i = i + 1);
      // for (i = 1; i < 3; i = i + 1);
      // for (var i = 1; i < 3; i = i + 1);
      // for (;;;);
      // for (var x in {...}) { xxxxx }
      // for (var x in y) { xxxx }
      boolean isLocal = false;
      for (int i = 1, nSkip = 0; i < slen && j < 2; i++) {
        switch (tok = tokAt(i)) {
        case T.var:
          isLocal = true;
          break;
        case T.semicolon:
          if (nSkip > 0)
            nSkip--;
          else
            pts[j++] = i;
          break;
        case T.in:
        case T.from:
          key = paramAsStr(i - 1);
          nSkip -= 2;
          if (isAtomExpression(++i)) {
            inTok = T.bitset;
            bsOrList = atomExpressionAt(i);
            if (isBondSet)
              bsOrList = BondSet.newBS((BS) bsOrList, null);
            isOK = (((BS) bsOrList).nextSetBit(0) >= 0);
          } else {
            Lst<SV> what = parameterExpressionList(-i, 1, false);
            if (what == null || what.size() < 1)
              invArg();
            SV vl = what.get(0);
            switch (inTok = vl.tok) {
            case T.bitset:
              bsOrList = vl.value;
              isOK = !((BS) bsOrList).isEmpty();
              break;
            case T.varray:
              Lst<SV> v = vl.getList();
              j = v.size();
              isOK = (j > 0);
              if (isOK && tok == T.from) {
                int[] i12 = new int[] {SV.iValue(v.get(0)), SV.iValue(v.get(j - 1)) };
                isMinusMinus = (i12[1] < i12[0]);
                bsOrList = i12;
                tok = T.in;
                inTok = T.integer;
              } else {
                bsOrList = v;
              }
              break;
            case T.hash:
              Map<String, SV> m = vl.getMap();
              int n = m.keySet().size();
              isOK = (n > 0);
              if (isOK) {
                String[] keys = new String[n];
                m.keySet().toArray(keys);
                Arrays.sort(keys);
                bsOrList = keys;
              }
              break;
            default:
              invArg();
            }
          }
          i = iToken;
          break;
        case T.select:
          nSkip += 2;
          break;
        }
      }
      if (!isForCheck) {
        pushContext(cmdToken, "FOR");
        thisContext.forVars = forVars;
        forVars = null;
      }
      if (key == null) {
        if (isForCheck) {
          j = (bsOrList == null ? pts[1] + 1 : 2);
        } else {
          j = 2;
        }
        if (tokAt(j) == T.var)
          j++;
        key = paramAsStr(j);
        isMinusMinus = key.equals("--") || key.equals("++");
        if (isMinusMinus)
          key = paramAsStr(++j);
      }
      if (isOK)
        if (tok == T.in) {
          // start of FOR (i in x) block or FOR (i from x)
          forVar = getContextVariableAsVariable(key, isLocal);
          if (forVar == null && !isLocal)
            forVar = vwr.g.getAndSetNewVariable(key, false);
          if (forVar == null || forVar.myName == null) {
            if (key.startsWith("_"))
              invArg();
            if (isLocal)
              contextVariables.put(key.toLowerCase(), forVar = SV.newI(0));
            else
              forVar = vwr.g.getAndSetNewVariable(key, true);
          }
          if (inTok == T.integer) {
            // for (i from [0 31])
            forVar.tok = T.integer;
            forVar.intValue = ((int[]) bsOrList)[0];
            forVal = SV.newV(T.integer, bsOrList);
            forVal.intValue = (isMinusMinus ? Integer.MIN_VALUE : 0);
            j = -1;
          } else {
            forVal = SV.getVariable(bsOrList);
            if (inTok == T.bitset) {
              j = ((BS) bsOrList).nextSetBit(0);
              forVal.intValue = 0;
            } else {
              forVal.intValue = 1;
              forVar.setv(SV.selectItemVar(forVal));
              j = -1;
            }
          }
          if (forVars == null)
            forVars = cmdToken.forVars = new SV[2];
          forVars[0] = forVar;
          forVars[1] = forVal;
        } else {
          int vtok = tokAt(j);
          if (vtok != T.semicolon && (T.tokAttr(vtok, T.misc)
              || (forVal = getContextVariableAsVariable(key, false)) != null)) {
            if (!isMinusMinus && getToken(++j).tok != T.opEQ)
              invArg();
            if (isMinusMinus)
              j -= 2;
            setVariable(++j, slen - 1, key, false);
          }
          isOK = (pts[0] + 1 == pts[1] || parameterExpressionBoolean(pts[0] + 1, pts[1]));
        }
    }
    if (isOK && tok == T.in && j >= 0) {
      forVal.intValue = j;
      forVar.tok = T.bitset;
      if (isBondSet) {
        forVar.value = new BondSet();
        ((BondSet) forVar.value).set(j);
      } else {
        forVar.value = BSUtil.newAndSetBit(j);
      }
    }
    pt++;
    if (!isOK) {
      cmdToken.forVars = thisContext.forVars;
      popContext(true, false);
    }
    isForCheck = false;
    if (!isOK && !chk)
      pc = Math.abs(pt) - 1;
    return isForCheck;
  }

  private boolean cmdFlow(int tok, boolean isForCheck, Lst<T[]> vProcess)
      throws ScriptException {
    ContextToken ct;
    int pt = st[0].intValue;
    boolean isDone = (pt < 0 && !chk);
    boolean continuing = true;
    int ptNext = 0;
    switch (tok) {
    case T.function:
    case T.parallel:
      cmdFunc(); // when a function is a command
      return isForCheck;
    case T.trycmd:
      return isForCheck;
    case T.catchcmd:
      ct = (ContextToken) theToken;
      pushContext(ct, "CATCH");
      if (!isDone && ct.name0 != null)
        contextVariables.put(ct.name0, ct.contextVariables.get(ct.name0));
      continuing = !isDone;
      st[0].intValue = -Math.abs(pt);
      break;
    case T.switchcmd:
    case T.defaultcmd:
    case T.casecmd:
      ptNext = Math.abs(aatoken[Math.abs(pt)][0].intValue);
      switch (isDone ? 0 : cmdFlowSwitch((ContextToken) theToken, tok)) {
      case 0:
        // done
        ptNext = -ptNext;
        continuing = false;
        break;
      case -1:
        // skip this case
        continuing = false;
        break;
      case 1:
        // do this one
      }
      aatoken[pc][0].intValue = Math.abs(pt);
      theToken = aatoken[Math.abs(pt)][0];
      if (theToken.tok != T.end)
        theToken.intValue = ptNext;
      break;
    case T.ifcmd:
    case T.elseif:
      continuing = (!isDone && parameterExpressionBoolean(1, 0));
      if (chk)
        break;
      ptNext = Math.abs(aatoken[Math.abs(pt)][0].intValue);
      ptNext = (isDone || continuing ? -ptNext : ptNext);
      aatoken[Math.abs(pt)][0].intValue = ptNext;
      if (tok == T.catchcmd)
        aatoken[pc][0].intValue = -pt; // reset to "done" state
      break;
    case T.elsecmd:
      checkLength(1);
      if (pt < 0 && !chk)
        pc = -pt - 1;
      break;
    case T.endifcmd:
      checkLength(1);
      break;
    case T.whilecmd:
      if (!isForCheck)
        pushContext((ContextToken) theToken, "WHILE");
      isForCheck = false;
      if (!parameterExpressionBoolean(1, 0) && !chk) {
        pc = pt;
        popContext(true, false);
      }
      break;
    case T.breakcmd:
      if (!chk) {
        breakAt(pt);
        break;
      }
      if (slen == 1)
        break;
      int n = intParameter(checkLast(1));
      if (chk)
        break;
      for (int i = 0; i < n; i++)
        popContext(true, false);
      break;
    case T.continuecmd:
      isForCheck = true;
      if (!chk)
        pc = pt - 1;
      if (slen > 1)
        intParameter(checkLast(1));
      break;
    case T.end: // function, if, for, while, catch, switch
      switch (getToken(checkLast(1)).tok) {
      case T.trycmd:
        ScriptFunction trycmd = (ScriptFunction) getToken(1).value;
        if (chk)
          return false;
        runFunctionAndRet(trycmd, "try", null, null, true, true, true);
        return false;
      case T.function:
      case T.parallel:
        addFunction((ScriptFunction) theToken.value);
        return isForCheck;
      case T.catchcmd:
        popContext(true, false);
        break;
      case T.process:
        addProcess(vProcess, pt, pc);
        popContext(true, false);
        break;
      case T.switchcmd:
        if (pt > 0 && cmdFlowSwitch((ContextToken) aatoken[pt][0], 0) == -1) {
          // check for the default position
          for (; pt < pc; pt++)
            if ((tok = aatoken[pt][0].tok) != T.defaultcmd && tok != T.casecmd)
              break;
          continuing = (pc == pt);
        }
        break;
      case T.ifcmd:
        break;
      case T.forcmd:
      case T.whilecmd:
        continuing = false;
        isForCheck = true;
        break;
      }
      break;
    }
    if (!continuing && !chk)
      pc = Math.abs(pt) - 1;
    return isForCheck;
  }
  
  private int cmdFlowSwitch(ContextToken c, int tok) throws ScriptException {
    if (tok == T.switchcmd)
      c.addName("_var");
    SV var = c.contextVariables.get("_var");
    if (var == null)
      return 1; // OK, case found -- no more testing
    if (tok == 0) {
      // end: remove variable and do default
      //      this causes all other cases to
      //      skip
      c.contextVariables.remove("_var");
      return -1;
    }
    if (tok == T.defaultcmd) // never do the default one directly
      return -1;
    SV v = parameterExpressionToken(1);
    if (tok == T.casecmd) {
      boolean isOK = SV.areEqual(var, v);
      if (isOK)
        c.contextVariables.remove("_var");
      return isOK ? 1 : -1;
    }
    c.contextVariables.put("_var", v);
    return 1;
  }

  private void cmdFont(int shapeType, double fontsize) throws ScriptException {
    String fontface = "SansSerif";
    String fontstyle = "Plain";
    String name = "font";
    int sizeAdjust = 0;
    double scaleAngstromsPerPixel = -1;
    // font [shapeName] [size] [face] [style] [scaleAngstromsPerPixel]
    switch (iToken = slen) {
    case 6:
      scaleAngstromsPerPixel = floatParameter(5);
      if (scaleAngstromsPerPixel >= 5) // actually a zoom value
        scaleAngstromsPerPixel = vwr.tm.getZoomSetting()
            / scaleAngstromsPerPixel / vwr.getScalePixelsPerAngstrom(false);
      //$FALL-THROUGH$
    case 5:
      if (getToken(4).tok != T.identifier)
        invArg();
      fontstyle = paramAsStr(4);
      //$FALL-THROUGH$
    case 4:
      if (getToken(3).tok != T.identifier)
        invArg();
      fontface = paramAsStr(3);
      if (!isFloatParameter(2))
        error(ERROR_numberExpected);
      fontsize = floatParameter(2);
      shapeType = -1;
      break;
    case 3:
      if (!isFloatParameter(2))
        error(ERROR_numberExpected);
      if (shapeType == -1) {
        fontsize = floatParameter(2);
      } else {// labels --- old set fontsize N
        if (fontsize >= 1)
          fontsize += (sizeAdjust = 5);
        fontface = null;
      }
      break;
    case 2:
    default:
      if (shapeType == JC.SHAPE_LABELS) {
        // set fontsize
        fontsize = JC.LABEL_DEFAULT_FONTSIZE;
        name = "fontsize";
        break;
      }
      bad();
    }
    boolean isScale = (tokAt(1) == T.scale);
    if (shapeType == -1)
      shapeType = (isScale ? JC.SHAPE_ECHO : getShapeType(getToken(1).tok));

    if (shapeType == JC.SHAPE_LABELS) {
      if (fontsize < 0 || fontsize >= 1 && (fontsize < JC.LABEL_MINIMUM_FONTSIZE
          || fontsize > JC.LABEL_MAXIMUM_FONTSIZE)) {
        integerOutOfRange(JC.LABEL_MINIMUM_FONTSIZE - sizeAdjust,
            JC.LABEL_MAXIMUM_FONTSIZE - sizeAdjust);
        return;
      }
      if (!chk)
        setShapeProperty(JC.SHAPE_LABELS, "setDefaults", fontsize == 0 ? vwr.slm.noneSelected : Boolean.FALSE);
    }
    if (chk)
      return;
    Object value;
    if (name == "font") {
      if (Font.getFontStyleID(fontface) >= 0) {
        fontstyle = fontface;
        fontface = "SansSerif";
      }
      value = vwr.getFont3D(fontface, fontstyle, fontsize);
    } else {
      value = Double.valueOf(fontsize);
    }
    sm.loadShape(shapeType);
    if (isScale)
      setShapeProperty(JC.SHAPE_ECHO, "target", "%SCALE");
    setShapeProperty(shapeType, name, value);
    if (scaleAngstromsPerPixel >= 0)
      setShapeProperty(shapeType, "scalereference",
          Double.valueOf(scaleAngstromsPerPixel));    
    if (isScale)
      setShapeProperty(JC.SHAPE_ECHO, "thisID", null);
  }

  private void cmdFrank(int i) throws ScriptException {
    boolean b = true;
    if (slen > i)
      switch (getToken(checkLast(i)).tok) {
      case T.on:
        break;
      case T.off:
        b = false;
        break;
      default:
        error(ERROR_booleanExpected);
      }
    setBooleanProperty("frank", b);
  }

  private void cmdFunc() throws ScriptException {
    if (chk && !isCmdLine_c_or_C_Option)
      return;
    String name = ((String) getToken(0).value).toLowerCase();
    if (tokAt(1) == T.opEQ && tokAt(2) == T.none) {
      vwr.removeFunction(name);
      return;
    }
    if (!isFunction(name))
      error(ERROR_commandExpected);
    Lst<SV> params = (slen == 1 || slen == 3 && tokAt(1) == T.leftparen
        && tokAt(2) == T.rightparen ? null : parameterExpressionList(1, -1,
        false));
    if (chk)
      return;
    runFunctionAndRet(null, name, params, null, false, true, true);
  }

  private void cmdGetProperty() throws ScriptException {
    if (chk)
      return;
    String retValue = "";
    String property = optParameterAsString(1);
    String name = property;
    if (name.indexOf(".") >= 0)
      name = name.substring(0, name.indexOf("."));
    if (name.indexOf("[") >= 0)
      name = name.substring(0, name.indexOf("["));
    int propertyID = vwr.getPropertyNumber(name);
    Object param = "";
    switch (tokAt(2)) {
    default:
      param = optParameterAsString(2);
      break;
    case T.define:
    case T.expressionBegin:
    case T.bitset:
      param = atomExpressionAt(2);
      if (property.equalsIgnoreCase("bondInfo") && isAtomExpression(++iToken))
          param = new BS[] { (BS) param, atomExpressionAt(iToken) };
      break;
    }
    if (property.length() > 0 && propertyID < 0) {
      // no such property
      property = ""; // produces a list from Property Manager
      param = "";
    } else if (propertyID >= 0 && slen < 3) {
      if ((param = vwr.getDefaultPropertyParam(propertyID)).equals("(visible)"))
        param = vwr.ms.getVisibleSet(true);
    } else if (propertyID == vwr.getPropertyNumber("fileContents")) {
      String s = param.toString();
      for (int i = 3; i < slen; i++)
        s += paramAsStr(i);
      param = s;
    }
    retValue = (String) vwr.getProperty("readable", property, param);
    showString(retValue);
  }

  private void cmdGoto(boolean isCmd) throws ScriptException {
    String strTo = (isCmd ? paramAsStr(checkLast(1)) : null);
    int pcTo = (strTo == null ? aatoken.length - 1 : -1);
    String s = null;
    for (int i = pcTo + 1; i < aatoken.length; i++) {
      T[] tokens = aatoken[i];
      int tok = tokens[0].tok;
      switch (tok) {
      case T.message:
      case T.nada:
        s = (String) tokens[tokens.length - 1].value;
        if (tok == T.nada)
          s = s.substring(s.startsWith("#") ? 1 : 2);
        break;
      default:
        continue;
      }
      if (s.equalsIgnoreCase(strTo)) {
        pcTo = i;
        break;
      }
    }
    if (pcTo < 0)
      invArg();
    if (strTo == null)
      pcTo = 0;
    int di = (pcTo < pc ? 1 : -1);
    int nPush = 0;
    for (int i = pcTo; i != pc; i += di) {
      switch (aatoken[i][0].tok) {
      case T.push:
      case T.process:
      case T.forcmd:
      case T.catchcmd:
      case T.whilecmd:
        nPush++;
        break;
      case T.pop:
        nPush--;
        break;
      case T.end:
        switch (aatoken[i][1].tok) {
        case T.process:
        case T.forcmd:
        case T.catchcmd:
        case T.whilecmd:
          nPush--;
        }
        break;
      }
    }
    if (strTo == null) {
      pcTo = Integer.MAX_VALUE;
      for (; nPush > 0; --nPush)
        popContext(false, false);
    }
    if (nPush != 0)
      invArg();
    if (!chk)
      pc = pcTo - 1; // ... resetting the program counter
  }

  private void cmdHbond() throws ScriptException {
    if (slen == 2 && getToken(1).tok == T.calculate) {
      if (chk)
        return;
      int n = vwr.autoHbond(null, null, false);
      report(GT.i(GT.$("{0} hydrogen bonds"), Math.abs(n)), false);
      return;
    }
    if (slen == 2 && getToken(1).tok == T.delete) {
      if (chk)
        return;
      checkExtension(T.hbond);
      return;
    }
    int mad = getMadParameter();
    if (mad == Integer.MAX_VALUE)
      return;
    setShapeProperty(JC.SHAPE_STICKS, "type",
        Integer.valueOf(Edge.BOND_HYDROGEN_MASK));
    setShapeSizeBs(JC.SHAPE_STICKS, mad, null);
    setShapeProperty(JC.SHAPE_STICKS, "type",
        Integer.valueOf(Edge.BOND_COVALENT_MASK));
  }

  private void cmdHelp() throws ScriptException {
    if (chk)
      return;
    String what = optParameterAsString(1).toLowerCase();
    int pt = 0;
    if (what.startsWith("mouse") && (pt = what.indexOf(" ")) >= 0
        && pt == what.lastIndexOf(" ")) {
      showString(vwr.getBindingInfo(what.substring(pt + 1)));
      return;
    }
    if (T.tokAttr(T.getTokFromName(what), T.scriptCommand))
      what = "?command=" + what;
    vwr.getHelp(what);
  }

  private void cmdHistory(int pt) throws ScriptException {
    // history or set history
    if (slen == 1) {
      // show it
      showString(vwr.getSetHistory(Integer.MAX_VALUE));
      return;
    }
    if (pt == 2) {
      // set history n; n' = -2 - n; if n=0, then set history OFF
      int n = intParameter(checkLast(2));
      if (n < 0)
        invArg();
      if (!chk)
        vwr.getSetHistory(n == 0 ? 0 : -2 - n);
      return;
    }
    switch (getToken(checkLast(1)).tok) {
    // pt = 1 history ON/OFF/CLEAR
    case T.on:
    case T.clear:
      if (!chk)
        vwr.getSetHistory(Integer.MIN_VALUE);
      return;
    case T.off:
      if (!chk)
        vwr.getSetHistory(0);
      break;
    default:
      errorStr(ERROR_keywordExpected, "ON, OFF, CLEAR");
    }
  }

  private void cmdHover() throws ScriptException {
    if (chk)
      return;
    String strLabel = (slen == 1 ? "on" : paramAsStr(1));
    if (strLabel.equalsIgnoreCase("on"))
      strLabel = "%U";
    else if (strLabel.equalsIgnoreCase("off"))
      strLabel = null;
    vwr.setHoverLabel(strLabel);
  }

  private void cmdLabel(int index, BS bs) throws ScriptException {
    if (chk)
      return;
    sm.loadShape(JC.SHAPE_LABELS);
    Object strLabel = null;
    switch (getToken(index).tok) {
    case T.on:
      strLabel = vwr.getStandardLabelFormat(0);
      break;
    case T.off:
      break;
    case T.hide:
    case T.display:
      setShapeProperty(JC.SHAPE_LABELS, "display",
          theTok == T.display ? Boolean.TRUE : Boolean.FALSE);
      return;
    case T.varray:
      strLabel = theToken.value;
      break;
    default:
      strLabel = paramAsStr(index);
    }
    sm.setLabel(strLabel, bs == null ? vwr.bsA() : bs);
  }

  public void cmdLoad() throws ScriptException {
    boolean doLoadFiles = (!chk || isCmdLine_C_Option);
    boolean isAppend = false;
    boolean isInline = false;
    boolean isSmiles = false;
    boolean isMutate = false;
    boolean isData = false;
    boolean isAsync = vwr.async;
    boolean isConcat = false;
    boolean doOrient = false;
    boolean appendNew = vwr.getBoolean(T.appendnew);
    boolean isAudio = false;
    String filename = null;
    BS bsModels;
    int i = (tokAt(0) == T.data ? 0 : 1);
    String filter = null;
    int modelCount0 = vwr.ms.mc
        - (vwr.fm.getFileName().equals(JC.ZAP_TITLE) ? 1 : 0);
    int ac0 = vwr.ms.ac;
    SB loadScript = new SB().append("load");
    int nFiles = 1;
    Map<String, Object> htParams = new Hashtable<String, Object>();
    // ignore optional file format
    if (isStateScript) {
      htParams.put("isStateScript", Boolean.TRUE);
      if (forceNoAddHydrogens)
        htParams.put("doNotAddHydrogens", Boolean.TRUE);
    }
    String modelName = null;
    String[] filenames = null;
    String[] tempFileInfo = null;
    String errMsg = null;
    SB sOptions = new SB();
    int tokType = 0;
    int tok;

    // check for special parameters

    if (slen == 1) {
      i = 0;
    } else {
      modelName = paramAsStr(i);
      if (slen == 2 && !chk) {
        // spt, png, and pngj files may be
        // run using the LOAD command, but
        // we transfer them to the script command
        // if it is just LOAD "xxxx.xxx"
        // so as to avoid the ZAP in case these
        // do not contain a full state script
        if (modelName.endsWith(".spt") || modelName.endsWith(".png")
            || modelName.endsWith(".pngj")) {
          cmdScript(0, modelName, null);
          return;
        }
      }

      tok = tokAt(i);
      switch (tok) {
      case T.orientation:
        doOrient = true;
        loadScript.append(" orientation");
        vwr.stm.saveOrientation("preload", null);
        modelName = paramAsStr(++i);
        tok = T.getTokFromName(modelName);
        break;
      }
      // load MENU
      // load DATA "xxx" ...(data here)...END "xxx"
      // load DATA "append_and/or_orientation xxx" ...(data here)...END "append_and/or_orientation xxx"
      // load DATA "@varName"
      // load APPEND (moves pointer forward)
      // load XYZ
      // load VXYZ
      // load VIBRATION
      // load TEMPERATURE
      // load OCCUPANCY
      // load PARTIALCHARGE
      // load HISTORY
      // load NBO
      switch (tok) {
      case T.var:
        String var = paramAsStr(++i);
        filename = "@" + var;
        Object o = getVarParameter(var, false);
        if (o instanceof Map<?, ?>) {
          checkLength(3);
          loadPNGJVar(filename, o, htParams);
          return;
        }
        break;
      case T.nbo:
      case T.history:
      case T.menu:
        String m = paramAsStr(checkLast(2));
        if (!chk) {
          switch (tok) {
          case T.nbo:
            htParams.put("service", "nbo");
            htParams.put("mode", Integer.valueOf(1)); // MODEL
            htParams.put("action", "load");
            htParams.put("value", m);
            htParams.put("sync", Boolean.TRUE);
            vwr.sm.processService(htParams);
            runScript((String) htParams.get("ret"));
            break;
          case T.history:
            vwr.setHistory(m);
            break;
          case T.menu:
            vwr.setMenu(m, true);
            break;
          }
        }
        return;
      case T.mutate:
        isMutate = isAppend = true;
        appendNew = false;
        loadScript.append(" mutate");
        modelName = optParameterAsString(++i);
        tok = T.getTokFromName(modelName);
        htParams.put("appendToModelIndex", Integer.valueOf(vwr.am.cmi));
        break;
      case T.append:
        // we are looking out for state scripts after model 1.1 deletion.
        modelName = optParameterAsString(++i);
        int ami = PT.parseInt(modelName);
        isAppend = (!isStateScript || vwr.ms.mc > 0);
        if (isAppend)
          loadScript.append(" append");
        if (ami >= 0) {
          modelName = optParameterAsString(++i);
          if (isAppend) {
            loadScript.append(" " + ami);
            appendNew = false;
            htParams.put("appendToModelIndex", Integer.valueOf(ami));
          }
        }
        tok = T.getTokFromName(modelName);
        break;
      case T.audio:
        isAudio = true;
        i++;
        break;
      case T.identifier:
        i++;
        loadScript.append(" " + modelName);
        tokType = (tok == T.identifier
            && PT.isOneOf(modelName.toLowerCase(), JC.LOAD_ATOM_DATA_TYPES)
                ? T.getTokFromName(modelName)
                : T.nada);
        if (tokType != T.nada) {
          // loading just some data here
          // xyz vxyz vibration temperature occupancy partialcharge
          htParams.put("atomDataOnly", Boolean.TRUE);
          htParams.put("modelNumber", Integer.valueOf(1));
          if (tokType == T.vibration)
            tokType = T.vibxyz;
          tempFileInfo = vwr.fm.getFileInfo();
          isAppend = true;
        }
      }
      // LOAD [[APPEND]] FILE
      // LOAD [[APPEND]] INLINE
      // LOAD [[APPEND]] SMILES
      // LOAD [[APPEND]] TRAJECTORY
      // LOAD [[APPEND]] MODEL
      // LOAD ASYNC  (asynchronous -- flag for RecentFileDialog)
      // LOAD [[APPEND]] "fileNameInQuotes"

      switch (tok) {
      case T.file:
        i++;
        loadScript.append(" " + modelName);
        if (optParameterAsString(i).equals("+")) {
          isConcat = true;
          i++;
          loadScript.append(" +");
        }
        if (optParameterAsString(i).equals("-")) {
          isConcat = true;
          i++;
          loadScript.append(" -");
        }
        if (tokAt(i) == T.varray) {
          filenames = stringParameterSet(i);
          i = iToken;
          if (i + 1 != slen)
            invArg();
          if (filenames != null)
            nFiles = filenames.length;
        }
        break;
      case T.spacegroup:
        i -= 2;
        filename = "0";
        htParams.put("isEmptyLoad", Boolean.TRUE);
        //$FALL-THROUGH$
      case T.inline:
        isInline = true;
        i++;
        loadScript.append(" inline");
        break;
      case T.smiles:
        isSmiles = true;
        i++;
        break;
      case T.async:
        isAsync = true;
        htParams.put("async", Boolean.TRUE);
        i++;
        break;
      case T.trajectory:
      case T.model:
        i++;
        loadScript.append(" " + modelName);
        if (tok == T.trajectory)
          htParams.put("isTrajectory", Boolean.TRUE);
        if (isPoint3f(i)) {
          P3d pt = getPoint3f(i, false, true);
          i = iToken + 1;
          // first last stride
          htParams.put("firstLastStep",
              new int[] { (int) pt.x, (int) pt.y, (int) pt.z });
          loadScript.append(" " + Escape.eP(pt));
        } else {
          switch (tokAt(i)) {
          case T.bitset:
            bsModels = (BS) getToken(i++).value;
            htParams.put("bsModels", bsModels);
            loadScript.append(" " + Escape.eBS(bsModels));
            break;
          default:
            htParams.put("firstLastStep", new int[] { 0, -1, 1 });
          }
        }
        break;
      case T.identifier:
        // i has been incremented; continue...
        break;
      case T.data:
        String key = stringParameter(++i).toLowerCase();
        modelName = optParameterAsString(i + 1);
        isAppend = key.startsWith("append");
        if (isAppend && key.startsWith("append modelindex=")) {
          int ami = PT.parseInt(key.substring(18));
          if (ami >= 0) {
            appendNew = false;
            htParams.put("appendToModelIndex", Integer.valueOf(ami));
          }
        }
        doOrient = (key.indexOf("orientation") >= 0);
        i = addLoadData(loadScript, key, htParams, i);
        isData = true;
        break;
      default:
        modelName = "fileset";
      }
      if (filename == null && filenames == null && getToken(i).tok != T.string)
        error(ERROR_filenameExpected);
    }
    // long timeBegin = System.currentTimeMillis();

    // file name is next

    // LOAD ... "xxxx"
    // LOAD ... "xxxx" AS "yyyy"

    int filePt = i;
    int ptAs = i + 1;
    String localName = null;
    //    String annotation = null;
    //    if (tokAt(filePt + 1) == T.divide) {
    //      annotation = optParameterAsString(filePt + 2);
    //      ptAs += 2;
    //      i += 2;
    //    }
    if (tokAt(ptAs) == T.as) {
      localName = stringParameter(i = ptAs + 1);
      if (vwr.fm.getPathForAllFiles() != "") {
        // we use the LOCAL name when reading from a local path only (in the case of JMOL files)
        localName = null;
        filePt = i;
      }
    }

    String appendedData = null;
    String appendedKey = null;

    if (isSmiles && tokAt(i + 1) == T.filter) {
      ++i;
      filter = stringParameter(++i);
    }

    if (slen == i + 1) {
      // end-of-command options:
      // LOAD SMILES "xxxx" --> load "$xxxx"

      if (filename == null && (i == 0 || filenames == null
          && (filename = paramAsStr(filePt)).length() == 0))
        filename = getFullPathName(true);
      if (filename == null && filenames == null) {
        cmdZap(false);
        return;
      }
      if (filenames == null && !isInline) {
        if (isSmiles) {
          filename = "$" + filename;
        } else {
          if (filename.equals("String[]"))
            return;
          if (filename.indexOf("[") == 0) {
            filenames = Escape.unescapeStringArray(filename);
            if (filenames != null) {
              if (i == 1)
                loadScript.append(" files");
              nFiles = filenames.length;
            }
          }
        }
      }
      if (filenames != null)
        for (int j = 0; j < nFiles; j++)
          loadScript.append(" /*file*/").append(PT.esc(filenames[j]));
    } else if (isLoadOption(getToken(i + 1).tok)) {

      // more complicated command options, in order
      // (checking the tokens after "....") 

      // LOAD "" --> prevous file      

      if (filename == null && (filename = paramAsStr(filePt)).length() == 0
          && (filename = getFullPathName(true)) == null) {
        // no previously loaded file
        cmdZap(false);
        return;
      }
      if (filePt == i || localName != null)
        i++;

      // for whatever reason, we don't allow a filename with [] in it.
      if (filename.equals("String[]"))
        return;
      // MANIFEST "..."
      if ((tok = tokAt(i)) == T.manifest) {
        String manifest = stringParameter(++i);
        htParams.put("manifest", manifest);
        sOptions.append(" MANIFEST " + PT.esc(manifest));
        tok = tokAt(++i);
      }
      // n >= 0: model number
      // n < 0: vibration number
      // [index1, index2, index3,...]

      switch (tok) {
      case T.integer:
      case T.varray:
      case T.leftsquare:
      case T.spacebeforesquare:
        i = getLoadModelIndex(i, sOptions, htParams);
        break;
      }
      i = getCmdExt().getLoadSymmetryParams(i, sOptions, htParams);

      // .... APPEND DATA "appendedData" .... end "appendedData"
      // option here to designate other than "appendedData"
      // .... APPEND "appendedData" @x ....

      if (tokAt(i) == T.append) {
        // for CIF reader -- experimental
        if (tokAt(++i) == T.data) {
          i += 2;
          appendedData = (String) getToken(i++).value;
          appendedKey = stringParameter(++i);
          ++i;
        } else {
          appendedKey = stringParameter(i++);
          appendedData = stringParameter(i++);
        }
        htParams.put(appendedKey, appendedData);
      }
      if (tokAt(i) == T.filter)
        filter = stringParameter(++i);
    } else {
      Lst<String> fNames = new Lst<String>();
      if (i == 1) {
        if (tokAt(i + 1) == T.plus || tokAt(i + 1) == T.minus) {
          modelName = "files";
        } else {
          i++;
        }
        loadScript.append(" " + modelName);
      }
      if (tokAt(i + 1) == T.minus) // state from /val
        isConcat = true;
      filter = getLoadFilesList(i, loadScript, sOptions, htParams, fNames);
      filenames = fNames.toArray(new String[nFiles = fNames.size()]);
      if (!isConcat && loadScript.indexOf("/*concat*/") >= 0)
        isConcat = true;
    }

    // end of parsing

    if (!doLoadFiles)
      return;

    if (filenames != null)
      filename = "fileSet";

    // get default filter if necessary

    if (appendedData != null) {
      sOptions.append(" APPEND data \"" + appendedKey + "\"\n" + appendedData
          + (appendedData.endsWith("\n") ? "" : "\n") + "end \"" + appendedKey
          + "\"");
    }
    if (filter == null)
      filter = vwr.g.defaultLoadFilter;
    if (filter.length() > 0) {
      if (filter.toUpperCase().indexOf("DOCACHE") >= 0) {
        if (!isStateScript && !isAppend)
          vwr.cacheClear();
      }
      htParams.put("filter", filter);
      if (filter.equalsIgnoreCase("2d")) // MOL file hack
        filter = "2D-noMin";
      sOptions.append(" FILTER " + PT.esc(filter));
    }

    // store inline data or variable data in htParams

    boolean isVariable = false;
    if (filenames == null) {
      if (filename.equals("string") && vwr.am.cmi >= 0) {
        filename = vwr.getCurrentFileAsString(null);
        loadScript = new SB().append("load inline ");
        isInline = true;
      }
      if (isInline) {
        htParams.put("fileData", filename);
      } else if (filename.startsWith("@") && filename.length() > 1) {
        Object o = getVarParameter(filename.substring(1), false);
        if (o instanceof Map<?, ?>) {
          checkLength(i + 1);
          loadPNGJVar(filename, o, htParams);
          return;
        }
        isVariable = true;
        o = "" + o;
        loadScript = new SB().append("{\n    var ")
            .append(filename.substring(1)).append(" = ")
            .append(PT.esc((String) o)).append(";\n    ").appendSB(loadScript);
        htParams.put("fileData", o);
      } else if (!isData
          && !((filename.startsWith("=") || filename.startsWith("*"))
              && filename.indexOf("/") > 0)) {
        // only for cases that can get filename changed to actual reference
        String type = "";
        int pt = filename.indexOf("::");
        if (pt > 0 && pt < 20) { // trying to avoid conflict with some sort of URL that has "::" in it.
          type = filename.substring(0, pt + 2);
          filename = filename.substring(pt + 2);
        }
        filename = type
            + checkFileExists("LOAD" + (isAppend ? "_APPEND_" : "_"), isAsync,
                filename, filePt, !isAppend && pc != pcResume);

        if (filename.startsWith("cache://"))
          localName = null;
        // on first pass, a ScriptInterruption will be thrown; 
        // on the second pass, we will have the file name, which will be cache://localLoad_n__m
      }
    }

    // set up the output stream from AS keyword

    OC out = null;
    String filecat = null;
    if (localName != null) {
      if (localName.equals("."))
        localName = vwr.fm.getFilePath(filename, false, true);
      if (localName.length() == 0 || vwr.fm.getFilePath(localName, false, false)
          .equalsIgnoreCase(vwr.fm.getFilePath(filename, false, false)))
        invArg();
      String[] fullPath = new String[] { localName };
      out = vwr.getOutputChannel(localName, fullPath);
      if (out == null)
        Logger.error("Could not create output stream for " + fullPath[0]);
      else
        htParams.put("outputChannel", out);
    }

    // check for single file or string

    if (filenames == null && tokType == 0) {

      // finalize the loadScript

      loadScript.append(" ");
      if (isVariable || isInline) {
        loadScript
            .append(filename.indexOf('\n') >= 0 || isVariable ? PT.esc(filename)
                : filename);
      } else if (!isData) {
        if (localName != null)
          localName = vwr.fm.getFilePath(localName, false, false);
        if (!filename.equals("String[]"))
          loadScript.append("/*file*/")
              .append((localName != null ? PT.esc(localName) : "$FILENAME$"));
      }
      if (!isConcat && (filename.startsWith("=") || filename.startsWith("*"))
          && filename.indexOf("/") > 0) {

        // EBI domains and validations, also rna3 and dssr

        // load *1cbs/dom/xx/xx  -->  load *1cbs - *dom/xx/xx/1cbs
        // load *1cbs/val/xx/xx  -->  load *1cbs - *val/xx/xx/1cbs
        // load *1cbs/rna3d/loops  -->  load *1cbs - *rna3d/loops/downloads/1cbs
        // TODO load *1cbs/map/xx/xx  -->  load *1cbs - *map/xx/xx/1cbs (unimplemented electron density?)
        // load =1mys/dssr  -->  load =1mys + *dssr/1mys

        isConcat = true;
        int pt = filename.indexOf("/");
        String id;
        if (pt == 1 && (id = vwr.getPdbID()) != null) {
          filename = filename.substring(0, 1) + id + filename.substring(1);
          pt = filename.indexOf("/");
        } else {
          id = filename.substring(1, pt);
        }
        String ext = filename.substring(pt + 1);
        filename = filename.substring(0, pt);
        if ((pt = filename.indexOf(".")) >= 0)
          filename = filename.substring(0, pt);
        if (JC.PDB_ANNOTATIONS.indexOf(";" + ext + ";") >= 0
            || ext.startsWith("dssr--")) {
          if (filename.startsWith("="))
            filename += ".cif";
          filenames = (ext.equals("all")
              ? new String[] { filename, "*dom/" + id, "*val/" + id }
              : new String[] { filename, "*" + ext + "/" + id });
          filename = "fileSet";
          loadScript = null;
          isVariable = false;
          filecat = "-";
          //sOptions.setLength(0);
        } else {
          filename += "/" + ext;
        }
      }
      if (loadScript != null) {
        if (sOptions.length() > 0)
          loadScript.append(" /*options*/ ").append(sOptions.toString());
        if (isVariable)
          loadScript.append("\n  }");
        htParams.put("loadScript", loadScript);
      }
    }

    if (isAudio) {
      if (filename != null)
        htParams.put("audioFile", filename);
      addFilterAttribute(htParams, filter, "id");
      addFilterAttribute(htParams, filter, "pause");
      addFilterAttribute(htParams, filter, "play");
      addFilterAttribute(htParams, filter, "ended");
      addFilterAttribute(htParams, filter, "action");
      vwr.sm.playAudio(htParams);
      return;
    }

    // load model

    setCursorWait(true);
    boolean timeMsg = vwr.getBoolean(T.showtiming);
    if (timeMsg)
      Logger.startTimer("load");
    if (!isStateScript && !isAppend && !vwr.getBoolean(T.doubleprecision))
      vwr.setBooleanProperty("legacyJavaFloat", false);
    if (isMutate)
      htParams.put("isMutate", Boolean.TRUE);
    htParams.put("eval", this);
    errMsg = vwr.loadModelFromFile(null, filename, filenames, null, isAppend,
        htParams, loadScript, sOptions, tokType,
        filecat != null ? filecat : isConcat ? "+" : " ");
    if (timeMsg)
      showString(Logger.getTimerMsg("load", 0));

    // close output channel

    if (out != null) {
      vwr.fm.setFileInfo(new String[] { localName });
      Logger.info(GT.o(GT.$("file {0} created"), localName));
      showString(vwr.fm.getFilePath(localName, false, false) + " created");
      out.closeChannel();
    }

    // check for just loading an atom property

    if (tokType > 0) {
      // reset the file info in FileManager, check for errors, and return
      vwr.fm.setFileInfo(tempFileInfo);
      if (errMsg != null && !isCmdLine_c_or_C_Option)
        evalError(errMsg, null);
      return;
    }

    // check for an error

    if (errMsg != null && !isCmdLine_c_or_C_Option) {
      // note that as ZAP will already have been issued here.
      if (errMsg.indexOf(JC.NOTE_SCRIPT_FILE) == 0) {
        filename = errMsg.substring(JC.NOTE_SCRIPT_FILE.length()).trim();
        if (filename.indexOf("png|") >= 0
            && filename.endsWith("pdb|state.spt")) {
          // fix for earlier version in JavaScript saving the full state instead of just the PDB file. 
          filename = filename.substring(0, filename.lastIndexOf("|"));
          filename += filename.substring(filename.lastIndexOf("|"));
          runScript("load \"" + filename + "\"");
          return;
        }
        cmdScript(0, filename, null);
        return;
      }
      if (vwr.async && errMsg.startsWith(JC.READER_NOT_FOUND)) {
        //TODO: other errors can occur due to missing files 
        //String rdrName = errMsg.substring(JC.READER_NOT_FOUND.length());
        throw new ScriptInterruption(this, "async", 1);
        //errMsg = "asynchronous load for " + rdrName + " initiated";
      }
      evalError(errMsg, null);
    }

    if (debugHigh)
      report(
          "Successfully loaded:"
              + (filenames == null ? htParams.get("fullPathName") : modelName),
          false);

    finalizeLoad(isAppend, appendNew, isConcat, doOrient, nFiles, ac0,
        modelCount0, isData);

  }

  public String checkFileExists(String prefix, boolean isAsync, String filename, int i, boolean doClear) throws ScriptException {
    if (chk || filename.startsWith("cache://")) 
       return filename;
    if ((vwr.testAsync || Viewer.isJS)
        && (isAsync || filename.startsWith("?"))
        || vwr.apiPlatform.forceAsyncLoad(filename)) {
      filename = loadFileAsync(prefix, filename, i, doClear);
      // on first pass, a ScriptInterruption will be thrown; 
      // on the second pass, we will have the file name, which will be cache://localLoad_n__m
    }

    String[] fullPathNameOrError = vwr.getFullPathNameOrError(filename);
    filename = fullPathNameOrError[0];
    if (fullPathNameOrError[1] != null)
      errorStr(ScriptError.ERROR_fileNotFoundException, filename
          + ":" + fullPathNameOrError[1]);
    return filename;
  }

  private void addFilterAttribute(Map<String, Object> htParams, String filter,
                                  String key) {
    String val = PT.getQuotedOrUnquotedAttribute(filter, key);
    if (val != null && val.length() > 0)
      htParams.put(key, val);
  }

  private int addLoadData(SB loadScript, String key, Map<String, Object> htParams, int i) throws ScriptException {
    loadScript.append(" /*data*/ data");
    int ptVar = key.indexOf("@");
    if (ptVar >= 0)
      key = key.replace('@', '_');
    loadScript.append(" ").append(PT.esc(key));
    String strModel = (ptVar >= 0 ? ""
        + getParameter(key.substring(ptVar + 1), T.string, true)
        : paramAsStr(++i));
    strModel = Viewer.fixInlineString(strModel, vwr.getInlineChar());
    htParams.put("fileData", strModel);
    htParams.put("isData", Boolean.TRUE);
    //note: ScriptCompiler will remove an initial \n if present
    loadScript.appendC('\n').append(strModel).append(" end ")
        .append(PT.esc(key));
    if (ptVar < 0)
      i += 2; // skip END "key"
    return i;
  }

  private void loadPNGJVar(String varName, Object o, Map<String, Object> htParams) throws ScriptException {
    SV[] av = new SV[] {SV.newV(T.hash, o)};
    getCmdExt().dispatch(T.binary, false, av);
    htParams.put("imageData", av[0].value);
    OC out = vwr.getOutputChannel(null, null);
    htParams.put("outputChannel", out);
    vwr.createZip("", "BINARY", htParams);
    String modelName = "cache://VAR_" + varName;
    vwr.cacheFileByName("cache://VAR_*",false);
    vwr.cachePut(modelName, out.toByteArray());
    cmdScript(0, modelName, null);
  }

  private String getLoadFilesList(int i, SB loadScript, SB sOptions,
                                Map<String, Object> htParams, Lst<String> fNames) throws ScriptException {
    // list of file names 
    // or COORD {i j k} "fileName" 
    // or COORD ({bitset}) "fileName"
    // or FILTER "xxxx"

    Lst<Object> firstLastSteps = null;
    String filter = null;
    P3d pt = null;
    BS bs = null;
    while (i < slen) {
      switch (tokAt(i)) {
      case T.plus:
        loadScript.append("/*concat*/ +");
        ++i;
        continue;
      case T.minus:
        // =xxxx/val split into two
        loadScript.append(" -");
        ++i;
        continue;
      case T.integer:
      case T.varray:
      case T.leftsquare:
      case T.spacebeforesquare:
        i = getLoadModelIndex(i, sOptions, htParams);
        continue;
      case T.filter:
        filter = stringParameter(++i);
        ++i;
        continue;
      case T.coord:
        htParams.remove("isTrajectory");
        if (firstLastSteps == null) {
          firstLastSteps = new Lst<Object>();
          pt = P3d.new3(0, -1, 1);
        }
        if (isPoint3f(++i)) {
          pt = getPoint3f(i, false, true);
          i = iToken + 1;
        } else if (tokAt(i) == T.bitset) {
          bs = (BS) getToken(i).value;
          pt = null;
          i = iToken + 1;
        }
        break;
      case T.identifier:
        invArg();
      }
      fNames.addLast(paramAsStr(i++));
      if (pt != null) {
        firstLastSteps
            .addLast(new int[] { (int) pt.x, (int) pt.y, (int) pt.z });
        loadScript.append(" COORD " + Escape.eP(pt));
      } else if (bs != null) {
        firstLastSteps.addLast(bs);
        loadScript.append(" COORD " + Escape.eBS(bs));
      }          
      loadScript.append(" /*file*/$FILENAME" + fNames.size() + "$");
    }
    if (firstLastSteps != null)
      htParams.put("firstLastSteps", firstLastSteps);
    return filter;
  }

  private boolean isLoadOption(int tok) {
    switch (tok) {
    case T.manifest:
      // model/vibration index or list of model indices
    case T.integer:
    case T.varray:
    case T.leftsquare:
    case T.spacebeforesquare:
    // {i j k} (lattice)
    case T.leftbrace:
    case T.point3f:
    // PACKED/CENTROID, either order
    case T.packed:
    case T.centroid:
    // SUPERCELL {i j k}
    case T.supercell:
    // RANGE x.x or RANGE -x.x
    case T.fill:  // new in Jmol 14.3.14
    // FILL BOUNDBOX
    // FILL UNITCELL
    case T.range:
    // SPACEGROUP "nameOrNumber" 
    // or SPACEGROUP "IGNOREOPERATORS" 
    // or SPACEGROUP "" (same as current)
    case T.spacegroup:
    // UNITCELL [a b c alpha beta gamma]
    // or UNITCELL [ax ay az bx by bz cx cy cz] 
    // or UNITCELL "" (same as current)
    // UNITCELL "..." or UNITCELL ""
    case T.unitcell:
    // OFFSET {x y z}
    case T.offset:
    case T.data:
    // FILTER "..."
    case T.append:
      // Jmol 13.1.5 -- APPEND "data..."
      return true;
    case T.filter:
    case T.identifier:
      return (tokAt(iToken + 2) != T.coord);
    }
    return false;
  }

  private int getLoadModelIndex(int i, SB sOptions,
                                 Map<String, Object> htParams)
      throws ScriptException {
    int n;
    switch (tokAt(i)) {
    case T.integer:
      htParams.remove("firstLastStep");
      htParams.remove("bsModel");
      htParams.put("useFileModelNumbers", Boolean.TRUE);
      n = intParameter(i);
      sOptions.append(" ").appendI(n);
      if (n < 0)
        htParams.put("vibrationNumber", Integer.valueOf(-n));
      else
        htParams.put("modelNumber", Integer.valueOf(n));
      break;
    case T.varray:
    case T.leftsquare:
    case T.spacebeforesquare:
      htParams.remove("firstLastStep");
      double[] data = doubleParameterSet(i, 1, Integer.MAX_VALUE);
      BS bs = new BS();
      int[] iArray = new int[data.length];
      for (int j = 0; j < data.length; j++) {
        n = (int) data[j];
        if (data[j] >= 1 && data[j] == n)
          bs.set(n - 1);
        else
          invArg();
        iArray[j] = n;
      }
      sOptions.append(" " + Escape.eAI(iArray));
      htParams.put("bsModels", bs);
      htParams.put("useFileModelNumbers", Boolean.TRUE);
      break;
    }
    return iToken + 1;
  }

  private void finalizeLoad(boolean isAppend, boolean appendNew,
                            boolean isConcat, boolean doOrient, int nFiles,
                            int ac0, int modelCount0, boolean isData)
      throws ScriptException {
    if (isAppend && (appendNew || nFiles > 1)) {
      vwr.setAnimationRange(-1, -1);
      vwr.setCurrentModelIndex(modelCount0);
    }
    String msg;
    if (scriptLevel == 0 && !isAppend && (isConcat || nFiles < 2)
        && (msg = (String) vwr.ms.getInfoM("modelLoadNote")) != null) {
      showString(msg);
    }
    Object centroid = vwr.ms.getInfoM("centroidMinMax");
    if (AU.isAI(centroid) && vwr.ms.ac > 0) {
      BS bs = BSUtil.newBitSet2(isAppend ? ac0 : 0, vwr.ms.ac);
      vwr.ms.setCentroid(bs, (int[]) centroid);
    }
    String script = vwr.g.defaultLoadScript;
    msg = "";
    if (script.length() > 0)
      msg += "\nUsing defaultLoadScript: " + script;
    String embeddedScript;
    Map<String, Object> info = vwr.ms.msInfo;
    if (info != null && vwr.allowEmbeddedScripts()
        && (embeddedScript = (String) info.remove("jmolscript")) != null
        && embeddedScript.length() > 0) {
      msg += "\nAdding embedded #jmolscript: " + embeddedScript;
      script += ";" + embeddedScript;
      setStringProperty("_loadScript", script);
      script = "allowEmbeddedScripts = false;try{" + script
          + "} allowEmbeddedScripts = true;";
      isEmbedded = !isCmdLine_c_or_C_Option;
    } else {
      setStringProperty("_loadScript", "");
    }
    logLoadInfo(msg, isData);

    String siteScript = (info == null ? null
        : (String) info.remove("sitescript"));
    if (siteScript != null)
      script = siteScript + ";" + script;
    if (doOrient)
      script += ";restore orientation preload";
    if (script.length() > 0 && !isCmdLine_c_or_C_Option)
      // NOT checking embedded scripts in some cases
      runScript(script);
    isEmbedded = false;
  }

  private void cmdLog() throws ScriptException {
    if (slen == 1)
      bad();
    if (chk)
      return;
    String s = parameterExpressionString(1, 0);
    if (tokAt(1) == T.off)
      setStringProperty("logFile", "");
    else
      vwr.log(s);
  }

  private void cmdLoop() throws ScriptException {
    if (vwr.headless)
      return;
    // back to the beginning of this script
    if (!chk)
      pc = -1;
    cmdDelay();
    // JavaScript will not get here
  }

  private void cmdMessage() throws ScriptException {
    String text = paramAsStr(checkLast(1));
    if (chk)
      return;
    String s = vwr.formatText(text);
    if (outputBuffer == null && !vwr.isPrintOnly)
      Logger.warn(s);
    if (!s.startsWith("_"))
      report(s, false);
  }

  /**
   * ONE difference between FRAME and MODEL: model 1 sets model NAMED one in the
   * case of PDB frame 1 always sets the first model
   * 
   * @param offset
   *        will be 2 for "anim frame ..."
   * @throws ScriptException
   */
  private void cmdModel(int offset) throws ScriptException {
    boolean isFrame = (theTok == T.frame || vwr.ms.mc > 1);
    int[] frameList = new int[] { -1, -1 };
    int nFrames = 0;
    boolean useModelNumber = true;
    int modelIndex = -1;
    if (slen == 1 && offset == 1) {
      modelIndex = vwr.am.cmi;
      int m;
      if (!chk && modelIndex >= 0
          && (m = vwr.ms.getJmolDataSourceFrame(modelIndex)) >= 0)
        vwr.setCurrentModelIndex(m == modelIndex ? Integer.MIN_VALUE : m);
      return;
    }
    switch (tokAt(1)) {
    case T.mo:
      if (!chk && isFrame && slen == 2) {
        while (++modelIndex < vwr.ms.mc) {
          if (!vwr.ms.am[modelIndex].auxiliaryInfo.containsKey("moData"))
            continue;
          vwr.am.setFrame(modelIndex);
          showString("Frame set to " + (modelIndex + 1));
          return;
        }
        showString("No molecular orbitals");
      }
      return;
    case T.integer:
      if (isFrame && slen == 2) {
        // FRAME n
        if (!chk)
          vwr.am.setFrame(intParameter(1) - 1);
        return;
      }
      break;
    case T.expressionBegin:
    case T.bitset:
      modelIndex = atomExpressionAt(1).nextSetBit(0);
      if (chk || modelIndex < 0 || modelIndex >= vwr.ms.ac)
        return;
      modelIndex = vwr.ms.at[modelIndex].mi;
      if (iToken + 1 == slen) {
        vwr.setCurrentModelIndex(modelIndex);
        return;
      }
      frameList[nFrames++] = modelIndex;
      offset = iToken + 1;
      useModelNumber = false;
      break;
    case T.create:
      iToken = 1;
      int n = (tokAt(2) == T.integer ? intParameter(++iToken) : 1);
      checkLength(iToken + 1);
      if (!chk && n > 0)
        vwr.ms.createModels(n);
      return;
    case T.id:
      checkLength(3);
      String id = stringParameter(2);
      if (!chk)
        vwr.setCurrentModelID(id);
      return;
    case T.delay:
      long millis = 0;
      checkLength(3);
      switch (getToken(2).tok) {
      case T.integer:
      case T.decimal:
        millis = (long) (floatParameter(2) * 1000);
        break;
      default:
        error(ERROR_integerExpected);
      }
      if (!chk)
        vwr.setFrameDelayMs(millis);
      return;
    case T.title:
      if (checkLength23() > 0)
        if (!chk)
          vwr.setFrameTitleObj(slen == 2 ? "@{_modelName}"
              : (tokAt(2) == T.varray ? SV.strListValue(st[2]) : paramAsStr(2)));
      return;
    case T.orientation:
      if (tokAt(2) == T.decimal && tokAt(3) == T.matrix4f) {
        modelIndex = vwr.ms.getModelNumberIndex(getToken(2).intValue, false,
            false);
        M4d mat4 = (M4d) getToken(3).value;
        if (modelIndex >= 0)
          vwr.ms.am[modelIndex].mat4 = mat4;
        return;
      }
      break;
    case T.align:
      boolean isNone = (tokAt(2) == T.none);
      BS bs = (slen == 2 || isNone ? null : atomExpressionAt(2));
      if (isNone)
        iToken = 2;
      boolean isFixed = (tokAt(iToken + 1) == T.fixed);
      checkLength(iToken + (isFixed ? 2 : 1));
      if (!chk)
        vwr.setFrameOffsets(bs, isFixed);
      return;
    }
    if (getToken(offset).tok == T.minus) {
      ++offset;
      if (getToken(checkLast(offset)).tok != T.integer
          || intParameter(offset) != 1)
        invArg();
      if (!chk)
        vwr.setAnimation(T.prev);
      return;
    }
    boolean isPlay = false;
    boolean isRange = false;
    String propName = null;
    Object prop = null;
    boolean isAll = false;
    boolean isHyphen = false;
    double fFrame = 0;
    P3d frameAlign = null;
    boolean haveFileSet = vwr.haveFileSet();
    if (isArrayParameter(1)) {
      setFrameSet(1);
      isAll = true;
    } else {
      for (int i = offset; i < slen; i++) {
        switch (getToken(i).tok) {
        case T.align:
          // model 2.3 align {0 0 0}  // from state 
          if (i != 2)
            invArg();
          frameAlign = centerParameter(3, null);
          checkLength(i = iToken + 1);
          break;
        case T.all:
        case T.times:
          checkLength(offset + (isRange ? 2 : 1));
          isAll = true;
          break;
        case T.minus: // ignore
          if (nFrames != 1)
            invArg();
          isHyphen = true;
          break;
        case T.none:
          checkLength(offset + 1);
          break;
        case T.decimal:
          useModelNumber = false;
          if ((fFrame = floatParameter(i)) < 0) {
            checkLength(i + 1);
            if (!chk)
              vwr.am.morph(-fFrame);
            return;
          }
          //$FALL-THROUGH$
        case T.integer:
        case T.string:
          if (nFrames == 2)
            invArg();
          int iFrame = (theTok == T.string ? getFloatEncodedInt((String) theToken.value)
              : theToken.intValue);
          if (iFrame < 0 && nFrames == 1) {
            isHyphen = true;
            iFrame = -iFrame;
            if (haveFileSet && iFrame < 1000000)
              iFrame *= 1000000;
          }
          if (theTok == T.decimal && haveFileSet && fFrame == (int) fFrame)
            iFrame = (int) fFrame * 1000000;
          if (iFrame == Integer.MAX_VALUE) {
            useModelNumber = false;
            frameList[nFrames++] = (chk || i != 1 ? 0 : vwr
                .getModelIndexFromId(theToken.value.toString()));
            break;
          }
          if (iFrame == -1) {
            checkLength(offset + 1);
            if (!chk)
              vwr.setAnimation(T.prev);
            return;
          }
          if (iFrame >= 1000 && iFrame < 1000000 && haveFileSet)
            iFrame = (iFrame / 1000) * 1000000 + (iFrame % 1000); // initial way
          if (!useModelNumber && iFrame == 0 && nFrames == 0)
            isAll = true; // 0.0 means ALL; 0 means "all in this range
          if (iFrame >= 1000000)
            useModelNumber = false;
          frameList[nFrames++] = iFrame;
          break;
        case T.play:
          isPlay = true;
          break;
        case T.range:
          isRange = true;
          break;
        case T.property:
          if (modelIndex < 0 && (modelIndex = vwr.am.cmi) < 0)
            return;
          propName = paramAsStr(++i);
          SV sv = setVariable(++i, -1, "", false);
          if (sv != null && !chk) {
            if (propName.equalsIgnoreCase("DSSR")) {
              loadDssr(modelIndex, (String) sv.value);
              return;
            }
            prop = SV.oValue(sv);
          }
          if (!chk)
            vwr.ms.setInfo(modelIndex, propName, prop);
          return;
        default:
          frameControl(offset);
          return;
        }
      }
    }
    if (chk)
      return;
    if (isRange && nFrames == 0)
      isAll = true;
    if (isAll) {
      vwr.setAnimationOn(false);
      vwr.setAnimationRange(-1, -1);
      if (!isRange)
        vwr.setCurrentModelIndex(-1);
      return;
    }
    if (nFrames == 2 && !isRange)
      isHyphen = true;
    if (haveFileSet)
      useModelNumber = false;
    else if (useModelNumber)
      for (int i = 0; i < nFrames; i++)
        if (frameList[i] >= 0)
          frameList[i] %= 1000000;
    modelIndex = vwr.ms
        .getModelNumberIndex(frameList[0], useModelNumber, false);
    if (frameAlign != null) {
      if (modelIndex >= 0) {
        vwr.ms.translateModel(modelIndex, null);
        vwr.ms.translateModel(modelIndex, frameAlign);
      }
      return;
    }
    int modelIndex2 = -1;
    if (haveFileSet && modelIndex < 0 && frameList[0] != 0) {
      // may have frame 2.0 or frame 2 meaning the range of models in file 2
      // or frame 2.0 - 3.1   or frame 2.0 - 3.0
      if (frameList[0] < 1000000)
        frameList[0] *= 1000000;
      if (nFrames == 2 && frameList[1] < 1000000)
        frameList[1] *= 1000000;
      if (frameList[0] % 1000000 == 0) {
        frameList[0]++;
        modelIndex = vwr.ms.getModelNumberIndex(frameList[0], false, false);
        if (modelIndex >= 0) {
          int i2 = (nFrames == 1 ? frameList[0] + 1000000
              : frameList[1] == 0 ? -1
                  : frameList[1] % 1000000 == 0 ? frameList[1] + 1000001
                      : frameList[1] + 1);
          modelIndex2 = vwr.ms.getModelNumberIndex(i2, false, false);
          if (modelIndex2 < 0)
            modelIndex2 = vwr.ms.mc;
          modelIndex2--;
          if (isRange)
            nFrames = 2;
          else if (!isHyphen && modelIndex2 != modelIndex)
            isHyphen = true;
          isRange = isRange || modelIndex == modelIndex2;// (isRange ||
          // !isHyphen &&
          // modelIndex2 !=
          // modelIndex);
        }
      } else {
        // must have been a bad frame number. Just return.
        return;
      }
    }

    if (!isPlay && !isRange || modelIndex >= 0)
      vwr.setCurrentModelIndexClear(modelIndex, false);
    if (isPlay && nFrames == 2 || isRange || isHyphen) {
      if (modelIndex2 < 0)
        modelIndex2 = vwr.ms.getModelNumberIndex(frameList[1], useModelNumber,
            false);
      vwr.setAnimationOn(false);
      vwr.am.setAnimationDirection(1);
      vwr.setAnimationRange(modelIndex, modelIndex2);
      vwr.setCurrentModelIndexClear(isHyphen && !isRange ? -1
          : modelIndex >= 0 ? modelIndex : 0, false);
    }
    if (isPlay)
      vwr.setAnimation(T.resume);
  }

  private void loadDssr(int modelIndex, String data) throws ScriptException {
    if (modelIndex < 0 && (modelIndex = vwr.am.cmi) < 0)
      errorStr(ScriptError.ERROR_multipleModelsDisplayedNotOK, "load <dssr file>");
    if (!data.startsWith("{"))
      data = vwr.getFileAsString3(data, true, "script");
    clearDefinedVariableAtomSets();
    Map<String, Object> map = vwr.parseJSONMap(data);
    showString(vwr.getAnnotationParser(true).fixDSSRJSONMap(map));
    vwr.ms.setInfo(modelIndex, "dssr", map);
  }

  private void cmdMove() throws ScriptException {
    checkLength(-11);
    // rotx roty rotz zoom transx transy transz slab seconds fps
    V3d dRot = V3d.new3(floatParameter(1), floatParameter(2), floatParameter(3));
    double dZoom = floatParameter(4);
    V3d dTrans = V3d.new3(intParameter(5), intParameter(6), intParameter(7));
    double dSlab = floatParameter(8);
    double floatSecondsTotal = floatParameter(9);
    int fps = (slen == 11 ? intParameter(10) : 30);
    if (chk)
      return;
    refresh(false);
    if (!useThreads())
      floatSecondsTotal = 0;
    vwr.move(this, dRot, dZoom, dTrans, dSlab, floatSecondsTotal, fps);
    if (floatSecondsTotal > 0 && isJS)
      throw new ScriptInterruption(this, "move", 1);
  }

  private void cmdMoveto() throws ScriptException {
    // moveto time
    // moveto [time] { x y z deg} zoom xTrans yTrans (rotCenter) rotationRadius
    // (navCenter) xNav yNav navDepth
    // moveto [time] { x y z deg} 0 xTrans yTrans (rotCenter) [zoom factor]
    // (navCenter) xNav yNav navDepth
    // moveto [time] { x y z deg} (rotCenter) [zoom factor] (navCenter) xNav
    // yNav navDepth
    // where [zoom factor] is [0|n|+n|-n|*n|/n|IN|OUT]
    // moveto [time] front|back|left|right|top|bottom
    if (slen == 2 && tokAt(1) == T.stop) {
      if (!chk)
        vwr.tm.stopMotion();
      return;
    }
    double floatSecondsTotal;
    if (slen == 2 && isFloatParameter(1)) {
      floatSecondsTotal = floatParameter(1);
      if (chk)
        return;
      if (!useThreads())
        floatSecondsTotal = 0;
      if (floatSecondsTotal > 0)
        refresh(false);
      vwr.moveTo(this, floatSecondsTotal, null, JC.axisZ, 0, null, 100, 0, 0,
          0, null, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
          Double.NaN);
      if (isJS && floatSecondsTotal > 0 && vwr.g.waitForMoveTo)
        throw new ScriptInterruption(this, "moveTo", 1);
      return;
    }
    V3d axis = V3d.new3(Double.NaN, 0, 0);
    P3d center = null;
    int i = 1;
    floatSecondsTotal = (isFloatParameter(i) ? floatParameter(i++) : 2.0d);
    double degrees = 90;
    BS bsCenter = null;
    boolean isChange = true;
    boolean isMolecular = false;
    double xTrans = 0;
    double yTrans = 0;
    double zoom = Double.NaN;
    double rotationRadius = Double.NaN;
    double zoom0 = vwr.tm.getZoomSetting();
    P3d navCenter = null;
    double xNav = Double.NaN;
    double yNav = Double.NaN;
    double navDepth = Double.NaN;
    double cameraDepth = Double.NaN;
    double cameraX = Double.NaN;
    double cameraY = Double.NaN;
    double[] pymolView = null;
    Qd q = null;
    int tok = getToken(i).tok;
    switch (tok) {
    case T.pymol:
      // 18-element standard PyMOL view matrix 
      // [0-8] are 3x3 rotation matrix (inverted)
      // [9,10] are x,y translations (y negative)
      // [11] is distance from camera to center (negative)
      // [12-14] are rotation center coords
      // [15-16] are slab and depth distance from camera (0 to ignore)
      // [17] is field of view; positive for orthographic projection
      // or 21-element extended matrix (PSE file reading)
      // [18,19] are boolean depth_cue and fog settings
      // [20] is fogStart (usually 0.45)
      pymolView = doubleParameterSet(++i, 18, 21);
      i = iToken + 1;
      if (chk && checkLength(i) > 0)
        return;
      break;
    case T.quaternion:
      if (tokAt(++i) == T.molecular) {
        // see comment below
        isMolecular = true;
        i++;
      }
      if (isAtomExpression(i)) {
        isMolecular = true;
        Object[] ret = new Object[1];
        center = centerParameter(i, ret);
        if (!(ret[0] instanceof BS))
          invArg();
        bsCenter = (BS) ret[0];
        q = (chk ? new Qd() : vwr.ms.getQuaternion(bsCenter.nextSetBit(0),
            vwr.getQuaternionFrame()));
      } else {
        q = getQuaternionParameter(i, null, false);
      }
      i = iToken + 1;
      if (q == null)
        invArg();
      break;
    case T.point4f:
    case T.point3f:
    case T.leftbrace:
      // {X, Y, Z} deg or {x y z deg}
      if (isPoint3f(i)) {
        axis.setT(getPoint3f(i, true, true));
        i = iToken + 1;
        degrees = floatParameter(i++);
      } else {
        P4d pt4 = getPoint4f(i);
        i = iToken + 1;
        axis.set(pt4.x, pt4.y, pt4.z);
        degrees = (pt4.x == 0 && pt4.y == 0 && pt4.z == 0 ? Double.NaN : pt4.w);
      }
      break;
    case T.front:
      axis.set(1, 0, 0);
      degrees = 0d;
      checkLength(++i);
      break;
    case T.back:
      axis.set(0, 1, 0);
      degrees = 180d;
      checkLength(++i);
      break;
    case T.left:
      axis.set(0, 1, 0);
      checkLength(++i);
      break;
    case T.right:
      axis.set(0, -1, 0);
      checkLength(++i);
      break;
    case T.top:
      axis.set(1, 0, 0);
      checkLength(++i);
      break;
    case T.bottom:
      axis.set(-1, 0, 0);
      checkLength(++i);
      break;
    case T.axis:
      String abc = paramAsStr(++i);
      if (abc.equals("-"))
        abc += paramAsStr(++i);
      checkLength(++i);
      switch ("xyz".indexOf(abc)) {
      case 0:
        q = Qd.new4(0.5d,0.5d,0.5d,-0.5d);
        break;
      case 1:
        q = Qd.new4(0.5d,0.5d,0.5d,0.5d);
        break;
      case 2:
        q = Qd.new4(0, 0, 0, 1);
        break;
      default:
         // a b c
        SymmetryInterface uc;
        uc = vwr.getCurrentUnitCell();
        if (uc == null) {
          uc = vwr.getSymTemp().setUnitCell(new double[] { 1, 1, 1, 90, 90, 90 }, false);
        }
        q = uc.getQuaternionRotation(abc);
        if (q == null)
          invArg();
      }
      break;
    default:
      // X Y Z deg
      axis = V3d.new3(floatParameter(i++), floatParameter(i++),
          floatParameter(i++));
      degrees = floatParameter(i++);
    }
    if (q != null) {
      A4d aa;
      aa = q.toA4d();
      axis.set(aa.x, aa.y, aa.z);
      /*
       * The quaternion angle for an atom represents the angle by which the
       * reference frame must be rotated to match the frame defined for the
       * residue.
       * 
       * However, to "moveTo" this frame as the REFERENCE frame, what we have to
       * do is take that quaternion frame and rotate it BACKWARD by that many
       * degrees. Then it will match the reference frame, which is ultimately
       * our window frame.
       * 
       * We only apply this for molecular-type quaternions, because in general
       * the orientation quaternion refers to how the reference plane has been
       * changed (the orientation matrix)
       */
      degrees = (isMolecular ? -1 : 1) * (aa.angle * 180.0 / Math.PI);
    }
    if (Double.isNaN(axis.x) || Double.isNaN(axis.y) || Double.isNaN(axis.z))
      axis.set(0, 0, 0);
    else if (axis.length() == 0 && degrees == 0)
      degrees = Double.NaN;
    isChange = (tok == T.quaternion || !vwr.tm.isInPosition(axis, degrees));
    // optional zoom
    if (isFloatParameter(i))
      zoom = floatParameter(i++);
    // optional xTrans yTrans
    if (isFloatParameter(i) && !isCenterParameter(i)) {
      xTrans = floatParameter(i++);
      yTrans = floatParameter(i++);
      if (!isChange && Math.abs(xTrans - vwr.tm.getTranslationXPercent()) >= 1)
        isChange = true;
      if (!isChange && Math.abs(yTrans - vwr.tm.getTranslationYPercent()) >= 1)
        isChange = true;
    }
    if (bsCenter == null && i != slen) {
      // if any more, required (center)
      Object[] ret = new Object[1];
      center = centerParameter(i, ret);
      if (ret[0] instanceof BS)
        bsCenter = (BS) ret[0];
      i = iToken + 1;
    }
    if (center != null) {
      if (!isChange && center.distance(vwr.tm.fixedRotationCenter) >= 0.1)
        isChange = true;
      // optional {center} rotationRadius
      if (isFloatParameter(i))
        rotationRadius = floatParameter(i++);
      if (!isCenterParameter(i)) {
        if ((rotationRadius == 0 || Double.isNaN(rotationRadius))
            && (zoom == 0 || Double.isNaN(zoom))) {
          // alternative (atom expression) 0 zoomFactor
          double newZoom = Math.abs(getZoom(0, i, bsCenter, (zoom == 0 ? 0
              : zoom0)));
          i = iToken + 1;
          zoom = newZoom;
        } else {
          if (!isChange
              && Math.abs(rotationRadius - vwr.getDouble(T.rotationradius)) >= 0.1)
            isChange = true;
        }
      }
      if (zoom == 0 || Double.isNaN(zoom))
        zoom = 100;
      if (Double.isNaN(rotationRadius))
        rotationRadius = 0;

      if (!isChange && Math.abs(zoom - zoom0) >= 1)
        isChange = true;
      // (navCenter) xNav yNav navDepth

      if (i != slen) {
        navCenter = centerParameter(i, null);
        i = iToken + 1;
        if (i != slen) {
          xNav = floatParameter(i++);
          yNav = floatParameter(i++);
        }
        if (i != slen)
          navDepth = floatParameter(i++);
        if (i != slen) {
          cameraDepth = floatParameter(i++);
          if (!isChange
              && Math.abs(cameraDepth - vwr.tm.getCameraDepth()) >= 0.01f)
            isChange = true;
        }
        if (i + 1 < slen) {
          cameraX = floatParameter(i++);
          cameraY = floatParameter(i++);
          if (!isChange && Math.abs(cameraX - vwr.tm.camera.x) >= 0.01f)
            isChange = true;
          if (!isChange && Math.abs(cameraY - vwr.tm.camera.y) >= 0.01f)
            isChange = true;
        }
      }
    }
    checkLength(i);
    if (chk)
      return;
    if (!isChange)
      floatSecondsTotal = 0;
    if (floatSecondsTotal > 0)
      refresh(false);
    if (!useThreads())
      floatSecondsTotal = 0;
    if (cameraDepth == 0) {
      cameraDepth = cameraX = cameraY = Double.NaN;
    }
    if (pymolView != null)
      vwr.tm.moveToPyMOL(this, floatSecondsTotal, pymolView);
    else
      vwr.moveTo(this, floatSecondsTotal, center, axis, degrees, null, zoom,
          xTrans, yTrans, rotationRadius, navCenter, xNav, yNav, navDepth,
          cameraDepth, cameraX, cameraY);
    if (isJS && floatSecondsTotal > 0 && vwr.g.waitForMoveTo)
      throw new ScriptInterruption(this, "moveTo", 1);
  }

  public boolean isAtomExpression(int i) {
    switch(tokAt(i)) {
    case T.define: // added 5/13/17
    case T.bitset:
    case T.expressionBegin:
      return true;
    default:
      return false;
    }
  }

  private boolean cmdPause() throws ScriptException {
    if (chk || isJS && !allowJSThreads)
      return false;
    String msg = optParameterAsString(1);
    if (!vwr.getBooleanProperty("_useCommandThread")) {
      // showString("Cannot pause thread when _useCommandThread = FALSE: " +
      // msg);
      // return;
    }
    if (vwr.autoExit || !vwr.haveDisplay && !vwr.isWebGL)
      return false;
    if (scriptLevel == 0 && pc == aatoken.length - 1) {
      vwr.scriptStatus("nothing to pause: " + msg);
      return false;
    }
    msg = (msg.length() == 0 ? ": RESUME to continue." : ": "
        + vwr.formatText(msg));
    pauseExecution(true);
    vwr.scriptStatusMsg("script execution paused" + msg,
        "script paused for RESUME");
    return true;
  }

  private void cmdPrint() throws ScriptException {
    if (slen == 1) {
      if (!chk)
        showStringPrint("\0", true);
      return;
    }
    showStringPrint(parameterExpressionString(1, 0), true);
  }
  
  private void cmdPrompt() throws ScriptException {
    String msg = null;
    if (slen == 1) {
      if (!chk)
        msg = getContextTrace(vwr, getScriptContext("prompt"), null, true)
            .toString();
    } else {
      msg = parameterExpressionString(1, 0);
    }
    if (!chk)
      vwr.prompt(msg, null, null, true);
  }

  private void cmdReset() throws ScriptException {
    if (slen == 3 && tokAt(1) == T.function) {
      if (!chk)
        vwr.removeFunction(stringParameter(2));
      return;
    }
    checkLength(-2);
    if (chk)
      return;
    if (slen == 1) {
      vwr.reset(false);
      return;
    }
    // possibly "all"
    switch (tokAt(1)) {
    case T.print:
      if (!chk && outputBuffer != null)
        outputBuffer.setLength(0);
      return;
    case T.cache:
      vwr.cacheClear();
      return;
    case T.error:
      resetError();
      return;
    case T.lighting:
      vwr.stm.resetLighting();
      return;
    case T.shape:
      vwr.resetShapes(true);
      return;
    case T.function:
      vwr.clearFunctions();
      return;
    case T.structure:
      BS bsModified = new BS();
      runScript(vwr.ms.getDefaultStructure(vwr.bsA(), bsModified));
      vwr.shm.resetBioshapes(bsModified);
      return;
    case T.vanderwaals:
      vwr.setData("element_vdw", new Object[] { null, "" }, 0, 0, 0, 0, 0);
      return;
    case T.aromatic:
      vwr.ms.resetAromatic();
      return;
    case T.spin:
      vwr.reset(true);
      return;
    }
    String var = paramAsStr(1);
    if (var.charAt(0) == '_')
      invArg();
    vwr.unsetProperty(var);
  }

  private void resetError() {
    vwr.g.removeParam("_errormessage");
  }

  private void cmdRestrict() throws ScriptException {
    boolean isBond = (tokAt(1) == T.bonds);
    cmdSelect(isBond ? 2 : 1);
    restrictSelected(isBond, true);
  }

  private void cmdReturn(SV tv) throws ScriptException {
    if (chk)
      return;
    SV t = getContextVariableAsVariable("_retval", false);
    if (t != null) {
      SV v = (tv != null || slen == 1 ? null : parameterExpressionToken(1));
      if (tv == null)
        tv = (v == null ? SV.newI(0) : v);
      t.value = tv.value;
      t.intValue = tv.intValue;
      t.tok = tv.tok;
    }
    cmdGoto(false);
  }

  public void cmdRotate(boolean isSpin, boolean isSelected)
      throws ScriptException {

    // rotate is a full replacement for spin
    // spin is DEPRECATED

    /*
     * The Chime spin method:
     * 
     * set spin x 10;set spin y 30; set spin z 10; spin | spin ON spin OFF
     * 
     * Jmol does these "first x, then y, then z" I don't know what Chime does.
     * 
     * spin and rotate are now consolidated here.
     * 
     * far simpler is
     * 
     * spin x 10 spin y 10
     * 
     * these are pure x or y spins or
     * 
     * spin axisangle {1 1 0} 10
     * 
     * this is the same as the old "spin x 10; spin y 10" -- or is it? anyway,
     * it's better!
     * 
     * note that there are many defaults
     * 
     * spin # defaults to spin y 10 
     * spin 10 # defaults to spin y 10 
     * spin x # defaults to spin x 10
     * 
     * and several new options
     * 
     * spin -x 
     * spin axisangle {1 1 0} 10 
     * spin 10 (atomno=1)(atomno=2) 
     * spin 20 {0 0 0} {1 1 1}
     * 
     * spin MOLECULAR {0 0 0} 20
     * 
     * The MOLECULAR keyword indicates that spins or rotations are to be carried
     * out in the internal molecular coordinate frame, not the fixed room frame.
     * 
     * In the case of rotateSelected, all rotations are molecular and the
     * absense of the MOLECULAR keyword indicates to rotate about the geometric
     * center of the molecule, not {0 0 0}
     * 
     * Fractional coordinates may be indicated:
     * 
     * spin 20 {0 0 0/} {1 1 1/}
     * 
     * In association with this, TransformManager and associated functions are
     * TOTALLY REWRITTEN and consolideated. It is VERY clean now - just two
     * methods here -- one fixed and one molecular, two in Viewer, and two in
     * TransformManager. All the centering stuff has been carefully inspected
     * are reorganized as well.
     * 
     * Bob Hanson 5/21/06
     */

    if (slen == 2)
      switch (getToken(1).tok) {
      case T.on:
        if (!chk)
          vwr.tm.setSpinOn();
        return;
      case T.off:
        if (!chk)
          vwr.tm.setSpinOff();
        return;
      }

    BS bsAtoms = null, bsBest = null;
    double degreesPerSecond = PT.FLOAT_MIN_SAFE;
    int nPoints = 0;
    double endDegrees = Double.MAX_VALUE;
    boolean isMolecular = false;
    boolean haveRotation = false;
    double[] dihedralList = null;
    Lst<P3d> ptsA = null;
    P3d[] points = new P3d[2];
    V3d rotAxis = V3d.new3(0, 1, 0);
    V3d translation = null;
    M4d m4 = null;
    M3d m3 = null;
    boolean is4x4 = false;
    int direction = 1;
    int tok;
    Qd q = null;
    boolean helicalPath = false;
    boolean isDegreesPerSecond = false;
    boolean isSeconds = false;
    Lst<P3d> ptsB = null;
    BS bsCompare = null;
    P3d invPoint = null;
    P4d invPlane = null;
    boolean axesOrientationRasmol = vwr.getBoolean(T.axesorientationrasmol);
    boolean checkModelKit = false;
    for (int i = 1; i < slen; ++i) {
      switch (tok = getToken(i).tok) {
      case T.rotate:
      case T.rotateSelected:
        checkModelKit = (vwr.getOperativeSymmetry() != null);
        continue;
      case T.define:
      case T.bitset:
      case T.expressionBegin:
        bsBest = atomExpressionAt(i);
        if (translation != null || q != null || nPoints == 2) {
          bsAtoms = bsBest;
          ptsB = null;
          isSelected = true;
          break;
        }
        //$FALL-THROUGH$
      case T.leftbrace:
      case T.point3f:
      case T.dollarsign:
        haveRotation = true;
        if (nPoints == 2)
          nPoints = 0;
        // {X, Y, Z}
        // $drawObject[n]
        P3d pt1 = centerParameterForModel(i, vwr.am.cmi, null);
        if (!chk && tok == T.dollarsign && tokAt(i + 2) != T.leftsquare) {
          // rotation about an axis such as $line1
          isMolecular = true;
          Object[] data = new Object[] { objectNameParameter(++i),
              Integer.valueOf(vwr.am.cmi), null };
          rotAxis = (getShapePropertyData(JC.SHAPE_DRAW, "getSpinAxis", data) ? (V3d) data[2]
              : null);
        }
        points[nPoints++] = pt1;
        break;
      case T.spin:
        isSpin = true;
        continue;
      case T.internal:
      case T.molecular:
        isMolecular = true;
        continue;
      case T.selected:
        isSelected = true;
        break;
      case T.comma:
        continue;
      case T.integer:
      case T.decimal:
        if (isSpin) {
          // rotate spin ... [degreesPerSecond]
          // rotate spin ... [endDegrees] [degreesPerSecond]
          // rotate spin BRANCH <DihedralList> [seconds]
          if (degreesPerSecond == PT.FLOAT_MIN_SAFE) {
            degreesPerSecond = floatParameter(i);
          } else if (endDegrees == Double.MAX_VALUE) {
            endDegrees = degreesPerSecond;
            degreesPerSecond = floatParameter(i);
          } else {
            invArg();
          }
        } else {
          // rotate ... [endDegrees]
          // rotate ... [endDegrees] [degreesPerSecond]
          if (endDegrees == Double.MAX_VALUE) {
            endDegrees = floatParameter(i);
          } else if (degreesPerSecond == PT.FLOAT_MIN_SAFE) {
            degreesPerSecond = floatParameter(i);
            isSpin = true;
          } else {
            invArg();
          }
        }
        if (i == slen - 2 && (tokAt(i + 1) == T.misc || tokAt(i + 1) == T.string)) {
          String s = paramAsStr(++i).toLowerCase();
          if (s.equals("dps")) {
            isDegreesPerSecond = true;
          } else if (s.equals("sec")) {
            isSeconds = true;
          }
        }
        break;
      case T.minus:
        direction = -1;
        continue;
      case T.x:
        haveRotation = true;
        rotAxis.set(direction, 0, 0);
        continue;
      case T.y:
        haveRotation = true;
        rotAxis.set(0, direction, 0);
        continue;
      case T.z:
        haveRotation = true;
        rotAxis.set(0, 0, (axesOrientationRasmol && !isMolecular ? -direction
            : direction));
        continue;

        // 11.6 options

      case T.point4f:
      case T.quaternion:
      case T.best:
        if (tok == T.quaternion)
          i++;
        haveRotation = true;
        if ((q = getQuaternionParameter(i, bsBest, tok == T.best)) != null) {
          if (q.q0 == 0)
            q.q0 = 1e-10d;
          rotAxis.setT(q.getNormal());
          endDegrees = q.getTheta(); // returns [0-180]
        }
        break;
      case T.plane:
        // rotate plane @1 @2 @3
        // rotate plane picked
        P3d[] pts;
        int n;
        if (paramAsStr(i + 1).equalsIgnoreCase("picked")) {
          i++;
          @SuppressWarnings("unchecked")
          Lst<SV> lst = (Lst<SV>) vwr.getPOrNull("pickedList");
          n = lst.size();
          if (n < 3)
            return;
          pts = new P3d[3];
          for (int j = 0; j < 3; j++)
            pts[j] = vwr.ms.getAtomSetCenter(SV.getBitSet(lst.get(n - 3 + j),
                false));
        } else if (isArrayParameter(i + 1)) {
          pts = getPointArray(++i, -1, false);
          i = iToken;
        } else {
          pts = new P3d[3];
          for (int j = 0; j < 3; j++) {
            pts[j] = centerParameter(++i, null);
            i = iToken;
          }
        }
        n = pts.length;
        if (n < 3)
          return;
        q = Qd.getQuaternionFrame(pts[n - 3], pts[n - 2], pts[n - 1]);
        q = Qd.new4(1, 0, 0, 0).mulQ(q.inv().div(vwr.tm.getRotationQ()));
        rotAxis.setT(q.getNormal());
        endDegrees = q.getTheta();
        break;
      case T.axisangle:
        haveRotation = true;
        if (isPoint3f(++i)) {
          rotAxis.setT(centerParameter(i, null));
          break;
        }
        P4d p4 = getPoint4f(i);
        rotAxis.set(p4.x, p4.y, p4.z);
        endDegrees = p4.w;
        q = Qd.newVA(rotAxis, endDegrees);
        break;
      case T.branch:
        isSelected = true;
        isMolecular = true;
        haveRotation = true;
        if (isArrayParameter(++i)) {
          dihedralList = doubleParameterSet(i, 6, Integer.MAX_VALUE);
          i = iToken;
        } else {
          int iAtom1 = atomExpressionAt(i).nextSetBit(0);
          int iAtom2 = atomExpressionAt(++iToken).nextSetBit(0);
          if (iAtom1 < 0 || iAtom2 < 0)
            return;
          bsAtoms = vwr.getBranchBitSet(iAtom2, iAtom1, true);
          points[0] = vwr.ms.at[iAtom1];
          points[1] = vwr.ms.at[iAtom2];
          nPoints = 2;
        }
        break;

      // 12.0 options

      case T.translate:
        translation = V3d.newV(centerParameter(++i, null));
        isMolecular = isSelected = true;
        break;
      case T.helix:
        // screw motion, for quaternion-based operations
        helicalPath = true;
        continue;
      case T.symop:
        int symop = intParameter(++i);
        if (chk)
          continue;
        Map<String, Object> info = vwr.getSymTemp().getSpaceGroupInfo(vwr.ms,
            null, -1, false, null);
        Object[] op = (info == null ? null : (Object[]) info.get("operations"));
        if (symop == 0 || op == null || op.length < Math.abs(symop))
          invArg();
        op = (Object[]) op[Math.abs(symop) - 1];
        translation = (V3d) op[5];
        invPoint = (P3d) op[6];
        points[0] = (P3d) op[7];
        if (op[8] != null)
          rotAxis = (V3d) op[8];
        endDegrees = ((Integer) op[9]).intValue();
        if (symop < 0) {
          endDegrees = -endDegrees;
          if (translation != null)
            translation.scale(-1);
        }
        if (endDegrees == 0 && points[0] != null) {
          // glide plane
          rotAxis.normalize();
          MeasureD.getPlaneThroughPoint(points[0], rotAxis, invPlane = new P4d());
        }
        q = Qd.newVA(rotAxis, endDegrees);
        nPoints = (points[0] == null ? 0 : 1);
        isMolecular = true;
        haveRotation = true;
        isSelected = true;
        continue;
      case T.compare:
        bsCompare = atomExpressionAt(++i);
        ptsA = vwr.ms.getAtomPointVector(bsCompare);
        if (ptsA == null) {
          iToken = i;
          invArg();
        }
        i = iToken;
        ptsB = getPointVector(getToken(++i), i);
        if (ptsB == null || ptsA.size() != ptsB.size()) {
          iToken = i;
          invArg();
        }
        m4 = new M4d();
        points[0] = new P3d();
        nPoints = 1;
        Interface.getInterface("javajs.util.Eigen", vwr, "script");
        double stddev = (chk ? 0 : ScriptParam.getTransformMatrix4(ptsA, ptsB, m4,
            points[0]));
        // if the standard deviation is very small, we leave ptsB
        // because it will be used to set the absolute final positions
        if (stddev > 0.001)
          ptsB = null;
        //$FALL-THROUGH$
      case T.matrix4f:
      case T.matrix3f:
        haveRotation = true;
        m3 = new M3d();
        if (tok == T.matrix4f) {
          is4x4 = true;
          m4 = (M4d) theToken.value;
        }
        if (m4 != null) {
          // translation and rotation are calculated
          translation = new V3d();
          m4.getTranslation(translation);
          m4.getRotationScale(m3);
        } else {
          m3 = (M3d) theToken.value;
        }
        q = (chk ? new Qd() : Qd.newM(m3));
        rotAxis.setT(q.getNormal());
        endDegrees = q.getTheta();
        isMolecular = true;
        break;
      default:
        invArg();
      }
      i = iToken;
    }
    if (chk)
      return;

    // process
    if (dihedralList != null) {
      if (endDegrees != Double.MAX_VALUE) {
        isSpin = true;
        degreesPerSecond = endDegrees;
      }
    }

    if (isSelected && bsAtoms == null)
      bsAtoms = vwr.bsA();
    if (bsCompare != null) {
      isSelected = true;
      if (bsAtoms == null)
        bsAtoms = bsCompare;
    }
    if (q != null && !isSeconds && !isDegreesPerSecond) {
      isDegreesPerSecond = (degreesPerSecond > 0);
      isSeconds = !isDegreesPerSecond;
    }
    double rate = (degreesPerSecond == PT.FLOAT_MIN_SAFE ? 10
        : endDegrees == Double.MAX_VALUE ? degreesPerSecond
            : isDegreesPerSecond ? degreesPerSecond
                : isSeconds ? (endDegrees < 0 ? -1 : 1) * Math.abs(endDegrees / degreesPerSecond)
                    : (degreesPerSecond < 0) == (q == null ? endDegrees > 0
                        : true) ?
                    // -n means number of seconds, not degreesPerSecond
                    -endDegrees / degreesPerSecond
                        : degreesPerSecond);
    if (q == null && endDegrees < 0 && rate > 0)
      rate = -rate;
    if (dihedralList != null) {
      if (!isSpin) {
        vwr.setDihedrals(dihedralList, null, 1);
        return;
      }
      translation = null;
    }

    if (q != null) {
      // only when there is a translation but not a 4x4 matrix)
      // do we set the rotation to be the center of the selected atoms or model
      if (nPoints == 0 && translation != null && !is4x4)
        points[0] = vwr.ms.getAtomSetCenter(bsAtoms != null ? bsAtoms
            : isSelected ? vwr.bsA() : vwr.getAllAtoms());
      if (helicalPath && translation != null) {
        points[1] = P3d.newP(points[0]);
        points[1].add(translation);
        T3d[] ret = MeasureD.computeHelicalAxis(points[0], points[1], q);
        //  new T3[] { pt_a_prime, n, r, P3.new3(theta, pitch, residuesPerTurn), pt_b_prime };
        points[0] = (P3d) ret[0];
        double theta = ((P3d) ret[3]).x;
        if (theta != 0) {
          translation = (V3d) ret[1];
          rotAxis = V3d.newV(translation);
          if (theta < 0)
            rotAxis.scale(-1);
        }
        m4 = null;
      }
      if (isSpin && m4 == null)
        m4 = ScriptMathProcessor.getMatrix4f(q.getMatrix(), translation);
      if (points[0] != null)
        nPoints = 1;
    }
    if (invPoint != null || invPlane != null) {
      vwr.invertAtomCoord(invPoint, null, bsAtoms, -1, false);
      if (rotAxis == null)
        return;
    }
    // a thread will be required if we are spinning 
    // UNLESS we are headless, 
    // in which case we just turn off the spin
    boolean requiresThread = (isSpin && (!vwr.headless || endDegrees == Double.MAX_VALUE));
    // just turn this into a rotation if we cannot spin
    if (isSpin && !requiresThread)
      isSpin = false;
    if (!checkModelKit && nPoints < 2 && dihedralList == null) {
      if (!isMolecular) {
        // fixed-frame rotation
        // rotate x 10 # Chime-like
        // rotate axisangle {0 1 0} 10
        // rotate x 10 (atoms) # point-centered
        // rotate x 10 $object # point-centered
        if (requiresThread && bsAtoms == null && !useThreads()) {
          isSpin = false;
          if (endDegrees == Double.MAX_VALUE)
            return;
        }
        if (vwr.rotateAxisAngleAtCenter(this, points[0], rotAxis, rate,
            endDegrees, isSpin, bsAtoms)) {
          // bsAtoms can be non-null if we have a quaternion
          // rotate quaternion {2 3 4 4} {atomno<10}
          // a fixed quaternion rotation of a set of atoms
          // currently rotateAxisAngleAtCenter cannot return true
          // if isSpin is true. But that is what we are working on
          // TODO: not exactly clear here if this will work
          if (isJS && isSpin && bsAtoms == null && vwr.g.waitForMoveTo
              && endDegrees != Double.MAX_VALUE)
            throw new ScriptInterruption(this, "rotate", 1);
        }
        return;
      }

      // must be an internal rotation

      if (nPoints == 0)
        points[0] = new P3d();
      // rotate MOLECULAR
      // rotate MOLECULAR (atom1)
      // rotate MOLECULAR x 10 (atom1)
      // rotate axisangle MOLECULAR (atom1)
      points[1] = P3d.newP(points[0]);
      points[1].add(rotAxis);
      nPoints = 2;
    }
    if (nPoints == 0)
      points[0] = new P3d();
    if (nPoints < 2 || points[0].distance(points[1]) == 0) {
      points[1] = P3d.newP(points[0]);
      points[1].y += 1.0;
    }
    if (endDegrees == Double.MAX_VALUE)
      endDegrees = 0;
    if (checkModelKit) {
      if (endDegrees == 0)
        return;
      if ( bsAtoms == null || nPoints != 2 || isSpin || translation != null || dihedralList != null || ptsB != null 
          || is4x4)
        invArg();
      int na = vwr.getModelkit(false).cmdRotateAtoms(bsAtoms, points, endDegrees);
      if (doReport())
        report(GT.i(GT.$("{0} atoms rotated"), na), false);
      return;
    }
    if (endDegrees != 0 && translation != null && !haveRotation)
      translation.scale(endDegrees / translation.length());
    if (isSpin && translation != null
        && (endDegrees == 0 || degreesPerSecond == 0)) {
      // need a token rotation
      endDegrees = 0.01f;
      rate = (degreesPerSecond == PT.FLOAT_MIN_SAFE ? 0.01f
          : degreesPerSecond < 0 ?
          // -n means number of seconds, not degreesPerSecond
          -endDegrees / degreesPerSecond
              : degreesPerSecond * 0.01f / translation.length());
      degreesPerSecond = 0.01f;
    }
    if (bsAtoms != null && isSpin && ptsB == null && m4 != null) {
      ptsA = vwr.ms.getAtomPointVector(bsAtoms);
      // note that this m4 is NOT through 
      ptsB = ScriptParam.transformPoints(ptsA, m4, points[0]);
    }
    if (bsAtoms != null && !isSpin && ptsB != null) {
      vwr.setAtomCoords(bsAtoms, T.xyz, ptsB);
    } else {
      if (requiresThread && !useThreads())
        return;
      if (vwr.rotateAboutPointsInternal(this, points[0], points[1], rate,
          endDegrees, isSpin, bsAtoms, translation, ptsB, dihedralList,
          is4x4 ? m4 : null)
          && isJS && isSpin)
        throw new ScriptInterruption(this, "rotate", 1);
    }
  }

  private void cmdRestore() throws ScriptException {
    // restore orientation name time
    if (slen > 1) {
      String saveName = optParameterAsString(2);
      int tok = tokAt(1);
      switch (tok) {
      case T.unitcell:
        if (!chk)
          setModelCagePts(-1, null, null);
        return;
      case T.orientation:
      case T.rotation:
      case T.scene:
        double floatSecondsTotal = (slen > 3 ? floatParameter(3) : 0);
        if (floatSecondsTotal < 0)
          invArg();
        if (chk)
          return;
        String type = "";
        switch (tok) {
        case T.orientation:
          type = "Orientation";
          vwr.stm.restoreOrientation(saveName, floatSecondsTotal, true);
          break;
        case T.rotation:
          type = "Rotation";
          vwr.stm.restoreOrientation(saveName, floatSecondsTotal, false);
          break;
        case T.scene:
          type = "Scene";
          vwr.stm.restoreScene(saveName, floatSecondsTotal);
          break;
        }
        if (isJS && floatSecondsTotal > 0 && vwr.g.waitForMoveTo)
          throw new ScriptInterruption(this, "restore" + type, 1);
        return;
      }
      checkLength23();
      switch (tok) {
      case T.bonds:
        if (!chk)
          vwr.stm.restoreBonds(saveName);
        return;
      case T.context:
        if (chk)
          return;
        ScriptContext sc = (ScriptContext) vwr.stm.getContext(saveName);
        if (sc != null) {
          restoreScriptContext(sc, true, false, false);
          if (thisContext != null) {
            thisContext.setMustResume();
            mustResumeEval = true;
            tQuiet = true;
          }
        }
        return;
      case T.coord:
        if (chk)
          return;
        String script = vwr.stm.getSavedCoordinates(saveName);
        if (script == null)
          invArg();
        runScript(script);
        vwr.checkCoordinatesChanged();
        return;
      case T.selection:
        if (!chk)
          vwr.stm.restoreSelection(saveName);
        return;
      case T.state:
        if (chk)
          return;
        String state;
        state = vwr.stm.getSavedState(saveName);
        if (state == null) {
          if (saveName.equalsIgnoreCase("UNDO"))
            return;
          invArg();
        }
        runScript(state);
        return;
      case T.structure:
        if (chk)
          return;
        String shape = vwr.stm.getSavedStructure(saveName);
        if (shape == null)
          invArg();
        runScript(shape);
        return;
      }
    }
    errorStr2(ERROR_what, "RESTORE", saveList);
  }

  private void cmdSave() throws ScriptException {
    if (slen > 1) {
      String saveName = optParameterAsString(2);
      switch (tokAt(1)) {
      case T.bonds:
        if (!chk)
          vwr.stm.saveBonds(saveName);
        return;
      case T.context:
        if (!chk)
          saveContext(saveName);
        return;
      case T.coord:
        if (!chk)
          vwr.stm.saveCoordinates(saveName, vwr.bsA());
        return;
      case T.orientation:
      case T.rotation:
        if (!chk)
          vwr.stm.saveOrientation(saveName, null);
        return;
      case T.selection:
        if (!chk) {
          vwr.stm.saveSelection(saveName, vwr.bsA());
          vwr.stm.restoreSelection(saveName); // just to register the # of
        }
        return;
      case T.state:
        if (!chk && vwr.getBoolean(T.preservestate)) {
          vwr.stm.saveState(saveName);
          if (saveName.length() == 0) {
            showString(vwr.stm.getUndoInfo());
          }
        }
        return;
      case T.structure:
        if (!chk)
          vwr.stm.saveStructure(saveName);
        return;
      }
    }
    errorStr2(ERROR_what, "SAVE", saveList);
  }
  
  public void cmdScript(int tok, String filename, String theScript)
      throws ScriptException {
    if (tok == T.javascript) {
      checkLength(2);
      if (!chk)
        vwr.jsEval(paramAsStr(1));
      return;
    }
    boolean loadCheck = true;
    boolean isCheck = false;
    boolean doStep = false;
    boolean isAsync = vwr.async;
    int lineNumber = 0;
    int pc = 0;
    int lineEnd = 0;
    int pcEnd = 0;
    int i = 1;
    String localPath = null;
    String remotePath = null;
    String scriptPath = null;
    Lst<SV> params = null;
    if (tok == T.macro) {
      i = -2;
    }
    if (filename == null && theScript == null) {
      tok = tokAt(i);
      if (tok != T.string)
        error(ERROR_filenameExpected);
      filename = paramAsStr(i);

      if (filename.equalsIgnoreCase("async")) {
        isAsync = true;
        filename = paramAsStr(++i);
      }
      if (filename.equalsIgnoreCase("applet")) {
        filename = null;
        // script APPLET x "....."
        String appID = paramAsStr(++i);
        theScript = parameterExpressionString(++i, 0); // had _script variable??
        checkLast(iToken);
        if (chk)
          return;
        if (appID.length() == 0 || appID.equals("all"))
          appID = "*";
        if (!appID.equals(".")) {
          vwr.jsEval(appID + "\1" + theScript);
          if (!appID.equals("*"))
            return;
        }
      } else {
        tok = tokAt(slen - 1);
        doStep = (tok == T.step);
        if (filename.equalsIgnoreCase("inline")) {
          filename = null;
          theScript = parameterExpressionString(++i, (doStep ? slen - 1 : 0));
          i = iToken;
        } else {
          while (filename.equalsIgnoreCase("localPath")
              || filename.equalsIgnoreCase("remotePath")
              || filename.equalsIgnoreCase("scriptPath")) {
            if (filename.equalsIgnoreCase("localPath"))
              localPath = paramAsStr(++i);
            else if (filename.equalsIgnoreCase("scriptPath"))
              scriptPath = paramAsStr(++i);
            else
              remotePath = paramAsStr(++i);
            filename = paramAsStr(++i);
          }
          if (filename.startsWith("spt::"))
            filename = filename.substring(5);
          filename = checkFileExists("SCRIPT_", isAsync, filename, i, true);
        }
        if ((tok = tokAt(++i)) == T.check) {
          isCheck = true;
          tok = tokAt(++i);
        }
        if (tok == T.noload) {
          loadCheck = false;
          tok = tokAt(++i);
        }
        if (tok == T.line || tok == T.lines) {
          i++;
          lineEnd = lineNumber = Math.max(intParameter(i++), 0);
          if (checkToken(i)) {
            if (getToken(i).tok == T.minus)
              lineEnd = (checkToken(++i) ? intParameter(i++) : 0);
            else
              lineEnd = -intParameter(i++);
            if (lineEnd <= 0)
              invArg();
          }
        } else if (tok == T.command || tok == T.commands) {
          i++;
          pc = Math.max(intParameter(i++) - 1, 0);
          pcEnd = pc + 1;
          if (checkToken(i)) {
            if (getToken(i).tok == T.minus)
              pcEnd = (checkToken(++i) ? intParameter(i++) : 0);
            else
              pcEnd = -intParameter(i++);
            if (pcEnd <= 0)
              invArg();
          }
        }
        i = -i;
      }
    } else if (filename != null && isAsync) {
      filename = checkFileExists("SCRIPT_", isAsync, filename, i, true);
    }
    if (i < 0) {
      if (tokAt(i = -i) == T.leftparen) {
        params = parameterExpressionList(i, -1, false);
        i = iToken + 1;
      }
      checkLength(doStep ? i + 1 : i);
    }

    // processing

    if (chk && !isCmdLine_c_or_C_Option)
      return;
    if (isCmdLine_c_or_C_Option)
      isCheck = true;
    if (theScript == null) {
      theScript = getScriptFileInternal(filename, localPath, remotePath, scriptPath);
      if (theScript == null)
        invArg();
    }
    if (isMenu(theScript)) {
      vwr.setMenu(theScript, false);
      return;
    }

    
    
    boolean wasSyntaxCheck = chk;
    boolean wasScriptCheck = isCmdLine_c_or_C_Option;
    if (isCheck)
      chk = isCmdLine_c_or_C_Option = true;
    pushContext(null, "SCRIPT");
    contextPath += " >> " + filename;
    if (compileScript(filename, theScript, filename != null && debugScript)) {
      this.pcEnd = pcEnd;
      this.lineEnd = lineEnd;
      while (pc < lineNumbers.length && lineNumbers[pc] < lineNumber)
        pc++;
      this.pc = pc;
      boolean saveLoadCheck = isCmdLine_C_Option;
      isCmdLine_C_Option &= loadCheck;
      executionStepping |= doStep;

      if (contextVariables == null)
        contextVariables = new Hashtable<String, SV>();
      contextVariables.put("_arguments",
          (params == null ? SV.getVariableAI(new int[] {})
              : SV.getVariableList(params)));
      contextVariables.put("_argcount",
          SV.newI(params == null ? 0 : params.size()));
      
      if (isCheck)
        listCommands = true;
      boolean timeMsg = vwr.getBoolean(T.showtiming);
      if (timeMsg)
        Logger.startTimer("script");
      privateFuncs = null;
      dispatchCommands(false, false, false);
      if (isStateScript)
        ScriptManager.setStateScriptVersion(vwr, null);
      if (timeMsg)
        showString(Logger.getTimerMsg("script", 0));
      isCmdLine_C_Option = saveLoadCheck;
      popContext(false, false);
    } else {
      Logger.error(GT.$("script ERROR: ") + errorMessage);
      popContext(false, false);
      if (wasScriptCheck) {
        setErrorMessage(null);
      } else {
        evalError(null, null);
      }
    }

    chk = wasSyntaxCheck;
    isCmdLine_c_or_C_Option = wasScriptCheck;
  }

  private boolean isMenu(String s) {
    int pt = (s == null ? -1 : s.indexOf("Menu Structure"));
    return (pt > 0 && s.startsWith("#") &&  pt < s.indexOf("\n"));
  }

  /**
   * 
   * @param i 2 from RESTRICT BONDS, otherwise 1
   * @throws ScriptException
   */
  private void cmdSelect(int i) throws ScriptException {
    // NOTE this is called by restrict()
    if (slen == 1) {
      vwr.selectStatus(null, false, 0, !doReport(), false);
      return;
    }
    if (slen == 2 && tokAt(1) == T.only)
      return; // coming from "cartoon only"
    int tok = tokAt(2);
    BS bs = null;
    switch (tok) {
    case T.bitset:
      // select beginexpr bonds ( {...} ) endex pr
      if (getToken(2).value instanceof BondSet || tok == T.bonds
          && getToken(3).tok == T.bitset) {
        if (slen != iToken + 2)
          invArg();
        if (!chk)
          vwr.selectBonds((BS) theToken.value);
        return;
      }
      break;
    case T.measure:
    case T.bonds:
      if (slen == 5 && tokAt(3) == T.bitset) {
        bs = (BS) getToken(3).value;
        iToken++;
      } else if (isArrayParameter(4)) {
        bs = new BS();
        // 0-based here, to conform with getProperty measurementInfo.index
        int[] a = (int[]) expandDoubleArray(doubleParameterSet(4, 0,
            Integer.MAX_VALUE), 0, false);
        for (int ii = a.length; --ii >= 0;)
          if (a[ii] >= 0)
            bs.set(a[ii]);
      }
      checkLast(iToken);
      if (chk)
        return;
      if (bs == null)
        invArg();
      if (tok == T.measure)
        setShapeProperty(JC.SHAPE_MEASURES, "select", bs);
      else
        vwr.selectBonds(bs);
      return;
    }

    int addRemove = 0;
    boolean isGroup = false;
    boolean reportStatus = (tokAt(i) == T.comma);
    if (reportStatus)
      i++;
    if (getToken(1).intValue == 0 && theTok != T.off) {
      Object v = parameterExpressionToken(0).value;
      if (!(v instanceof BS))
        invArg();
      checkLast(iToken);
      bs = (BS) v;
    } else {
      tok = tokAt(i);
      switch (tok) {
      case T.on:
      case T.off:
        if (!chk)
          vwr.setSelectionHalosEnabled(tok == T.on);
        tok = tokAt(++i);
        if (tok == T.nada)
          return;
        break;
      }
      switch (tok) {
      case T.add:
      case T.remove:
        addRemove = tok;
        tok = tokAt(++i);
      }
      isGroup = (tok == T.group);
      if (isGroup)
        tok = tokAt(++i);
      // select ... beginexpr none endexpr
      bs = atomExpressionAt(i);
    }
    if (chk)
      return;
    if (isBondSet) {
      vwr.selectBonds(bs);
    } else {
      if (bs.length() > vwr.ms.ac) {
        BS bs1 = vwr.getAllAtoms();
        bs1.and(bs);
        bs = bs1;
      }
      vwr.selectStatus(bs, isGroup, addRemove, !doReport(), reportStatus);
      vwr.slm.noneSelected = Boolean.valueOf(slen == 4 && tokAt(2) == T.none);
    }
  }

  private void cmdSelectionHalos(int pt) throws ScriptException {
    boolean showHalo = false;
    switch (pt == slen ? T.on : getToken(pt).tok) {
    case T.on:
    case T.selected:
      showHalo = true;
      //$FALL-THROUGH$
    case T.off:
    case T.none:
    case T.normal:
      setBooleanProperty("selectionHalos", showHalo);
      break;
    default:
      invArg();
    }
  }

  private void cmdSet() throws ScriptException {
    /*
     * The SET command now allows only the following:
     * 
     * SET SET xxx? SET [valid Jmol Token.setparam keyword] SET labelxxxx SET
     * xxxxCallback
     * 
     * All other variables must be assigned using
     * 
     * x = ....
     * 
     * The processing goes as follows:
     * 
     * check for SET check for SET xx? check for SET xxxx where xxxx is a
     * command --- deprecated (all other settings may alternatively start with x
     * = y) check for SET xxxx where xxxx requires special checking (all other
     * settings may alternatively start with x = (math expression) check for
     * context variables var x = ... check for deprecated SET words such as
     * "radius"
     */
    if (slen == 1) {
      showString(vwr.getAllSettings(null));
      return;
    }
    boolean isJmolSet = (paramAsStr(0).equals("set"));
    String key = optParameterAsString(1);
    if (isJmolSet && slen == 2 && key.indexOf("?") >= 0) {
      showString(vwr.getAllSettings(key.substring(0, key.indexOf("?"))));
      return;
    }
    int tok = getToken(1).tok;

    int newTok = 0;
    String sval;
    int ival = Integer.MAX_VALUE;
    boolean b;
    P3d pt;

    boolean showing = (!chk && doReport()
        && !((String) st[0].value).equals("var"));

    // THESE FIRST ARE DEPRECATED AND HAVE THEIR OWN COMMAND
    // anything in this block MUST RETURN

    switch (tok) {
    case T.historylevel:
    case T.iskiosk:
    case T.saveproteinstructurestate:
    case T.showkeystrokes:
    case T.testflag1:
    case T.testflag2:
    case T.testflag3:
    case T.testflag4:
    case T.useminimizationthread:
      // these might be set in older state scripts, but they should not have been there 
      if (isStateScript)
        return;
      break;
    case T.axes:
      cmdAxes(2);
      return;
    case T.background:
      cmdBackground(2);
      return;
    case T.boundbox:
      cmdBoundbox(2);
      return;
    case T.frank:
      cmdFrank(2);
      return;
    case T.history:
      cmdHistory(2);
      return;
    case T.label:
      cmdLabel(2, null);
      return;
    case T.unitcell:
      cmdUnitcell(2);
      return;
    case T.highlight:
      sm.loadShape(JC.SHAPE_HALOS);
      setShapeProperty(JC.SHAPE_HALOS, "highlight",
          (tokAt(2) == T.off ? null : atomExpressionAt(2)));
      return;
    case T.display:// deprecated
    case T.selectionhalos:
      cmdSelectionHalos(2);
      return;
    case T.timeout:
      cmdTimeout(2);
      return;

    // THESE HAVE MULTIPLE CONTEXTS AND
    // SO DO NOT ALLOW CALCULATIONS xxx = a + b...
    // and are thus "setparam" only
    // anything in this block MUST RETURN
    case T.window:
      Object o = (isArrayParameter(2) ? doubleParameterSet(2, 2, 2)
          : tokAt(2) == T.integer
              ? new double[] { intParameter(2), intParameter(3) }
              : stringParameter(2));
      checkLast(iToken);
      if (chk)
        return;
      if (o instanceof String) {
        if (vwr.fm.loadImage(o, "\0windowImage", !useThreads()))
          throw new ScriptInterruption(this, "windowImage", 1);
      } else {
        vwr.setWindowDimensions((double[]) o);
      }
      return;
    case T.structure:
      STR type = STR.getProteinStructureType(paramAsStr(2));
      if (type == STR.NOT)
        invArg();
      double[] data = doubleParameterSet(3, 0, Integer.MAX_VALUE);
      if (data.length % 4 != 0)
        invArg();
      vwr.setStructureList(data, type);
      checkLast(iToken);
      return;
    case T.axescolor:
      ival = getArgbParam(2);
      if (!chk)
        setObjectArgb("axes", ival);
      return;
    case T.bondmode:
      b = false;
      switch (getToken(checkLast(2)).tok) {
      case T.opAnd:
        break;
      case T.opOr:
        b = true;
        break;
      default:
        invArg();
      }
      setBooleanProperty("bondModeOr", b);
      return;
    case T.debug:
    case T.debughigh:
      if (chk)
        return;
      int iLevel = (tokAt(2) == T.off
          || tokAt(2) == T.integer && intParameter(2) == 0 ? 4
              : (tok == T.debughigh ? 6 : 5));
      Logger.setLogLevel(iLevel);
      setIntProperty("logLevel", iLevel);
      if (iLevel == 4) {
        vwr.setDebugScript(false);
        if (showing)
          vwr.showParameter("debugScript", true, 80);
      }
      setDebugging();
      if (showing)
        vwr.showParameter("logLevel", true, 80);
      return;
    case T.echo:
      cmdSetEcho(0);
      return;
    case T.scale:
      cmdScale(2);
      return;
    case T.fontsize:
      cmdFont(JC.SHAPE_LABELS, checkLength23() == 2 ? 0 : floatParameter(2));
      return;
    case T.hbond:
      boolean bool = false;
      switch (tokAt(checkLast(2))) {
      case T.backbone:
        bool = true;
        //$FALL-THROUGH$
      case T.sidechain:
        setBooleanProperty("hbondsBackbone", bool);
        break;
      case T.solid:
        bool = true;
        //$FALL-THROUGH$
      case T.dotted:
        setBooleanProperty("hbondsSolid", bool);
        break;
      default:
        invArg();
      }
      return;
    case T.measure:
    case T.measurements:
      // on off here incompatible with "monitor on/off" so this is just a SET
      // option.
      switch (tok = tokAt(checkLast(2))) {
      case T.on:
      case T.off:
        setBooleanProperty("measurementlabels", tok == T.on);
        return;
      case T.dotted:
      case T.integer:
      case T.decimal:
        vwr.shm.loadShape(JC.SHAPE_MEASURES);
        int mad10 = getSetAxesTypeMad10(2);
        if (mad10 != Integer.MAX_VALUE)
          setShapeSizeBs(JC.SHAPE_MEASURES,
              tok == T.decimal ? mad10 / 10 : mad10, null);
        return;
      }
      setUnits(paramAsStr(2), T.measurementunits);
      return;
    case T.ssbond: // ssBondsBackbone
      b = false;
      // shapeManager.loadShape(JmolConstants.SHAPE_SSSTICKS);
      switch (tokAt(checkLast(2))) {
      case T.backbone:
        b = true;
        break;
      case T.sidechain:
        break;
      default:
        invArg();
      }
      setBooleanProperty("ssbondsBackbone", b);
      return;
    case T.togglelabel:
      cmdSetLabel("toggle");
      return;
    case T.usercolorscheme:
      Lst<Integer> v = new Lst<Integer>();
      for (int i = 2; i < slen; i++) {
        int argb = getArgbParam(i);
        v.addLast(Integer.valueOf(argb));
        i = iToken;
      }
      if (chk)
        return;
      int n = v.size();
      int[] scale = new int[n];
      for (int i = n; --i >= 0;)
        scale[i] = v.get(i).intValue();
      vwr.cm.ce.setUserScale(scale);
      return;
    case T.zslab:
      // sets zSlab either based on a percent value or an atom position
      if (isFloatParameter(2)) {
        checkLength(3);
        setIntProperty("zSlab", (int) floatParameter(2));
        pt = null;
      } else {
        if (!isCenterParameter(2))
          invArg();
        pt = centerParameter(2, null);
        checkLength(iToken + 1);
      }
      if (!chk)
        vwr.tm.zSlabPoint = (pt == null ? null : P3d.newP(pt));
      return;
    }

    boolean justShow = true;

    // these next may just report a value
    // require special checks
    // math expressions are allowed in most cases using xxxSetting(...)

    switch (tok) {
    case T.backgroundmodel:
      if (slen > 2) {
        String modelDotted = getSettingStr(2, false);
        int modelNumber;
        boolean useModelNumber = false;
        if (modelDotted.indexOf(".") < 0) {
          modelNumber = PT.parseInt(modelDotted);
          useModelNumber = true;
        } else {
          modelNumber = getFloatEncodedInt(modelDotted);
        }
        if (chk)
          return;
        int modelIndex = vwr.ms.getModelNumberIndex(modelNumber, useModelNumber,
            true);
        vwr.setBackgroundModelIndex(modelIndex);
        return;
      }
      break;
    case T.vanderwaals:
      if (chk)
        return;
      vwr.setAtomProperty(vwr.getAllAtoms(), T.vanderwaals, -1, Double.NaN, null,
          null, null);
      if (slen > 2 && "probe".equalsIgnoreCase(getSettingStr(2, false))) {
        runScript(Elements.VdwPROBE);
        return;
      }
      newTok = T.defaultvdw;
      //$FALL-THROUGH$
    case T.defaultvdw:
      // allows unquoted string for known vdw type
      if (slen > 2) {
        sval = paramAsStr(2);
        if (slen == 3 && VDW.getVdwType(sval) == null
            && VDW.getVdwType(sval = getSettingStr(2, false)) == null)
          invArg();
        setStringProperty(key, sval);
      }
      break;
    case T.defaultlattice:
      // in very early versions of Jmol we might
      // have accepted string "{1.0,2.0,3.0}" here,
      // but that is at least before 11.8. 
      // shouldn't be saving this in the state anyway.

      if (slen > 2) {
        SV var = parameterExpressionToken(2);
        if (var.tok == T.point3f)
          pt = (P3d) var.value;
        else {
          pt = new P3d();
          int ijk = var.asInt();
          if (ijk >= 100)
            SimpleUnitCell.ijkToPoint3f(ijk, pt, -1, 0);
        }
        if (!chk)
          vwr.setDefaultLattice(pt);
      }
      break;
    case T.defaults:
    case T.defaultcolorscheme:
      // allows unquoted "jmol" or "rasmol"
      if (slen > 2) {
        if ((theTok = tokAt(2)) == T.jmol || theTok == T.rasmol) {
          sval = paramAsStr(checkLast(2));
        } else {
          sval = getSettingStr(2, false);
        }
        setStringProperty(key, sval);
      }
      break;
    case T.formalcharge:
      ival = getSettingInt(2);
      if (ival == Integer.MIN_VALUE)
        invArg();
      if (!chk)
        vwr.ms.setFormalCharges(vwr.bsA(), ival);
      return;
    case T.language:
      // language can be used without quotes in a SET context
      // set language en
      if (slen > 2)
        setStringProperty(key, getSettingStr(2, isJmolSet));
      break;
    case T.measurementunits:
    case T.energyunits:
      if (slen > 2)
        setUnits(getSettingStr(2, isJmolSet), tok);
      break;
    case T.picking:
      if (!chk)
        vwr.setPicked(-1, false);
      if (slen > 2) {
        cmdSetPicking();
        return;
      }
      break;
    case T.pickingstyle:
      if (slen > 2) {
        cmdSetPickingStyle();
        return;
      }
      break;
    case T.property: // compiler may give different values to this token
      // set property_xxxx will be handled in setVariable
      break;
    case T.specular:
      ival = getSettingInt(2);
      if (ival == Integer.MIN_VALUE || ival == 0 || ival == 1) {
        justShow = false;
        break;
      }
      tok = T.specularpercent;
      key = "specularPercent";
      setIntProperty(key, ival);
      break;
    case T.strands:
      tok = T.strandcount;
      key = "strandCount";
      setIntProperty(key, getSettingInt(2));
      break;
    default:
      justShow = false;
    }

    if (justShow && !showing)
      return;

    // var xxxx = xxx can supercede set xxxx

    boolean isContextVariable = (!justShow && !isJmolSet
        && getContextVariableAsVariable(key, false) != null);

    if (!justShow && !isContextVariable) {

      // THESE NEXT are deprecated:

      switch (tok) {
      case T.bonds:
        newTok = T.showmultiplebonds;
        break;
      case T.hetero:
        newTok = T.selecthetero;
        break;
      case T.hydrogen:
        newTok = T.selecthydrogen;
        break;
      case T.measurementnumbers:
        newTok = T.measurementlabels;
        break;
      case T.radius:
        newTok = T.solventproberadius;
        setFloatProperty("solventProbeRadius", getSettingFloat(2));
        justShow = true;
        break;
      case T.scale3d:
        newTok = T.scaleangstromsperinch;
        break;
      case T.solvent:
        newTok = T.solventprobe;
        break;
      case T.color:
        newTok = T.defaultcolorscheme;
        break;
      case T.spin:
        sval = paramAsStr(2).toLowerCase();
        switch ("x;y;z;fps;".indexOf(sval + ";")) {
        case 0:
          newTok = T.spinx;
          break;
        case 2:
          newTok = T.spiny;
          break;
        case 4:
          newTok = T.spinz;
          break;
        case 6:
          newTok = T.spinfps;
          break;
        default:
          errorStr2(ERROR_unrecognizedParameter, "set SPIN ", sval);
        }
        if (!chk)
          vwr.setSpin(sval, (int) floatParameter(checkLast(3)));
        justShow = true;
        break;
      }
    }

    if (newTok != 0) {
      key = T.nameOf(tok = newTok);
    } else if (!justShow && !isContextVariable) {
      // special cases must be checked
      if (key.length() == 0 || key.charAt(0) == '_' && tokAt(2) != T.leftsquare) // these cannot be set by user
        error(ERROR_cannotSet);

      // these next are not reported and do not allow calculation xxxx = a + b

      String lckey = key.toLowerCase();
      if (lckey.indexOf("label") == 0 && PT.isOneOf(lckey.substring(5),
          ";front;group;atom;offset;offsetexact;offsetabsolute;pointer;alignment;toggle;scalereference;for;")) {
        if (cmdSetLabel(lckey.substring(5)))
          return;
      }
      if (isJmolSet && lckey.indexOf("shift_") == 0) {
        double f = floatParameter(2);
        checkLength(3);
        if (!chk)
          vwr.getNMRCalculation().setChemicalShiftReference(lckey.substring(6),
              f);
        return;
      }
      if (lckey.endsWith("callback"))
        tok = T.setparam;
    }
    if (isJmolSet && !T.tokAttr(tok, T.setparam)) {
      iToken = 1;
      if (!isStateScript)
        errorStr2(ERROR_unrecognizedParameter, "SET", key);
      warning(ERROR_unrecognizedParameterWarning, "SET", key);
    }

    if (!justShow && isJmolSet) {
      // simple cases
      switch (slen) {
      case 2:
        // set XXXX;
        // too bad we allow this...
        setBooleanProperty(key, true);
        justShow = true;
        break;
      case 3:
        // set XXXX val;
        // check for int and NONE just in case
        if (ival != Integer.MAX_VALUE) {
          // keep it simple
          setIntProperty(key, ival);
          justShow = true;
        }
        break;
      }
    }

    if (!justShow && !isJmolSet && tokAt(2) == T.none) {
      if (!chk)
        vwr.removeUserVariable(key.toLowerCase());
      justShow = true;
    }
    if (!justShow) {
      setVariable(1, 0, key, true);
      if (!isJmolSet)
        return;
    }
    if (showing)
      vwr.showParameter(key, true, 80);
  }


  private void cmdScale(int pt) throws ScriptException {
    // also set SCALE (for script compatibility with older versions)
    if (chk)
      return;
    String text = "%SCALE";
    switch (tokAt(pt)) {
    case T.off:
      setShapeProperty(JC.SHAPE_ECHO, "%SCALE", null);
      checkLast(pt);
      return;
    case T.on:
      pt++;
      break;
    default:
      String units = Measurement.fixUnits(optParameterAsString(pt));
      if (Measurement.fromUnits(1, units) != 0) {
        text += " " + units;
        pt++;
      } else {
        text = null;
      }
      break;
    }
    setShapeProperty(JC.SHAPE_ECHO, "thisID", "%SCALE");    
    if (tokAt(pt) == T.nada) {
      vwr.ms.setEchoStateActive(true);
      vwr.shm.loadShape(JC.SHAPE_ECHO);
      setShapeProperty(JC.SHAPE_ECHO, "target", "bottom");    
    } else {
      setShapeProperty(JC.SHAPE_ECHO, "target", "%SCALE");    
      cmdSetEcho(pt);
    }
    if (text != null)
      setShapeProperty(JC.SHAPE_ECHO, "text", text);  
    setShapeProperty(JC.SHAPE_ECHO, "thisID", null);
    refresh(false);
  }

  private void cmdSetEcho(int i) throws ScriptException {
    String propertyName = null;
    Object propertyValue = null;
    String id = null;
    boolean echoShapeActive = true;
    boolean isScale = (i > 0); // SCALE ... bottom left
    // set echo xxx
    // SCALE ...
    // SCALS units ...
    int pt = (i == 0 ? 2 : i);

    // check for ID name or just name
    // also check simple OFF, NONE
    switch (isScale ? T.nada : getToken(pt).tok) {
    case T.off:
      id = propertyName = "allOff";
      checkLength(++pt);
      break;
    case T.none:
      echoShapeActive = false;
      //$FALL-THROUGH$
    case T.all:
      // all and none get NO additional parameters;
      id = paramAsStr(2);
      checkLength(++pt);
      break;
    }
    switch (isScale ? tokAt(2) : getToken(2).tok) {
    case T.id:
        pt++;
      //$FALL-THROUGH$
    case T.left:
    case T.center:
    case T.right:
    case T.top:
    case T.middle:
    case T.bottom:
    case T.identifier:
    case T.string:
      id = paramAsStr(pt++);
      break;
    }
    if (!chk) {
      vwr.ms.setEchoStateActive(echoShapeActive);
      sm.loadShape(JC.SHAPE_ECHO);
      if (id != null) {
        if (propertyName == null && !isScale)
          setShapeProperty(JC.SHAPE_ECHO, "thisID", null);
        setShapeProperty(JC.SHAPE_ECHO, propertyName == null ? "target"
            : propertyName, id);
      }
    }

    if (pt < slen) {
      // set echo name xxx
      // pt is usually 3, but could be 4 if ID used
      if (isScale) {
        switch (tokAt(pt)) {
        case T.scale:
        case T.image:
        case T.string:
          invArg();
        }
      }
      
      switch (getToken(pt++).tok) {
      case T.align:
        propertyName = "align";
        switch (getToken(pt).tok) {
        case T.left:
        case T.right:
        case T.center:
          propertyValue = paramAsStr(pt++);
          break;
        default:
          invArg();
        }
        break;
      case T.center:
      case T.left:
      case T.right:
        propertyName = "align";
        propertyValue = paramAsStr(pt - 1);
        break;
      case T.depth:
        propertyName = "%zpos";
        propertyValue = Integer.valueOf((int) floatParameter(pt++));
        break;
      case T.display:
      case T.displayed:
      case T.on:
        propertyName = "hidden";
        propertyValue = Boolean.FALSE;
        break;
      case T.hide:
      case T.hidden:
        propertyName = "hidden";
        propertyValue = Boolean.TRUE;
        break;
      case T.model:
        int modelIndex = (chk ? 0 : modelNumberParameter(pt++));
        if (modelIndex >= vwr.ms.mc)
          invArg();
        propertyName = "model";
        propertyValue = Integer.valueOf(modelIndex);
        break;
      case T.leftsquare:
      case T.spacebeforesquare:
        // [ x y ] with or without %
        propertyName = "xypos";
        propertyValue = xypParameter(--pt);
        if (propertyValue == null)
          invArg();
        pt = iToken + 1;
        break;
      case T.integer:
        // x y without brackets
        int posx = intParameter(pt - 1);
        String namex = "xpos";
        if (tokAt(pt) == T.percent) {
          namex = "%xpos";
          pt++;
        }
        propertyName = "ypos";
        propertyValue = Integer.valueOf(intParameter(pt++));
        if (tokAt(pt) == T.percent) {
          propertyName = "%ypos";
          pt++;
        }
        checkLength(pt);
        setShapeProperty(JC.SHAPE_ECHO, namex, Integer.valueOf(posx));
        break;
      case T.offset:
        propertyName = "offset";
        if (isPoint3f(pt)) {
          P3d pt3 = getPoint3f(pt, false, true);
          // minus 1 here means from Jmol, not from PyMOL
          propertyValue = new double[] { -1, pt3.x, pt3.y, pt3.z, 0, 0, 0 };
          pt = iToken + 1;
        } else if (isArrayParameter(pt)) {
          // PyMOL offsets -- [1, scrx, scry, scrz, molx, moly, molz] in angstroms
          propertyValue = doubleParameterSet(pt, 7, 7);
          pt = iToken + 1;
        }
        break;
      case T.off:
        propertyName = "off";
        break;
      case T.scale:
        propertyName = "scale";
        propertyValue = Double.valueOf(floatParameter(pt++));
        break;
      case T.script:
        propertyName = "script";
        propertyValue = paramAsStr(pt++);
        break;
      case T.image:
        pt++;
        //$FALL-THROUGH$
      case T.string:
        boolean isImage = (theTok != T.string);
        checkLength(pt--);
        if (isImage) {
          if (id == null) {
            String[] data = new String[1];
            getShapePropertyData(JC.SHAPE_ECHO, "currentTarget", data);
            id = data[0];
          }
          if (!chk && vwr.ms.getEchoStateActive()
              && vwr.fm.loadImage(getToken(pt).value, id, !useThreads()))
            throw new ScriptInterruption(this, "setEchoImage", 1);
          return;
        }
        cmdEcho(pt);
        return;
      case T.point:
        propertyName = "point";
        propertyValue = (isCenterParameter(pt) ? centerParameter(pt, null)
            : null);
        pt = iToken + 1;
        break;
      default:
        if (isCenterParameter(pt - 1)) {
          propertyName = "xyz";
          propertyValue = centerParameter(pt - 1, null);
          pt = iToken + 1;
          break;
        }
        invArg();
      }
    }
    checkLength(pt);
    if (!chk && propertyName != null)
      setShapeProperty(JC.SHAPE_ECHO, propertyName, propertyValue);
  }

  private boolean cmdSetLabel(String str) throws ScriptException {
    sm.loadShape(JC.SHAPE_LABELS);
    Object propertyValue = null;
    setShapeProperty(JC.SHAPE_LABELS, "setDefaults", vwr.slm.noneSelected);
    while (true) {
      if (str.equals("for")) {
        BS bs = atomExpressionAt(2);
        cmdLabel(iToken + 1, bs);
        return true;
      }
      if (str.equals("scalereference")) {
        double scaleAngstromsPerPixel = floatParameter(2);
        if (scaleAngstromsPerPixel >= 5) // actually a zoom value
          scaleAngstromsPerPixel = vwr.tm.getZoomSetting()
              / scaleAngstromsPerPixel / vwr.getScalePixelsPerAngstrom(false);
        propertyValue = Double.valueOf(scaleAngstromsPerPixel);
        break;
      }
      boolean isAbsolute = false;
      if (str.equals("offset") || (isAbsolute = (str.equals("offsetabsolute") || str.equals("offsetexact")))) {
        str = "offset";
        if (isPoint3f(2)) {
          // PyMOL offsets -- {x, y, z} in angstroms
          P3d pt = getPoint3f(2, false, true);
          // minus 1 here means from Jmol, not from PyMOL
          propertyValue = new double[] { -1, pt.x, pt.y, pt.z, 0, 0, 0 };
        } else if (isArrayParameter(2)) {
          // PyMOL offsets -- [1, scrx, scry, scrz, molx, moly, molz] in angstroms
          propertyValue = doubleParameterSet(2, 7, 7);
        } else {
          int xOffset = intParameterRange(2, -JC.LABEL_OFFSET_MAX, JC.LABEL_OFFSET_MAX);
          int yOffset = intParameterRange(3, -JC.LABEL_OFFSET_MAX, JC.LABEL_OFFSET_MAX);
          if (xOffset == Integer.MAX_VALUE || yOffset == Integer.MAX_VALUE)
            return true;
          propertyValue = Integer.valueOf(JC.getOffset(xOffset, yOffset, isAbsolute));
        }
        break;
      }
      if (str.equals("alignment")) {
        switch (getToken(2).tok) {
        case T.left:
        case T.right:
        case T.center:
          str = "align";
          propertyValue = theToken.value;
          break;
        default:
          invArg();
        }
        break;
      }
      if (str.equals("pointer")) {
        int flags = JC.LABEL_POINTER_NONE;
        switch (getToken(2).tok) {
        case T.off:
        case T.none:
          break;
        case T.background:
          flags |= JC.LABEL_POINTER_BACKGROUND;
          //$FALL-THROUGH$
        case T.on:
          flags |= JC.LABEL_POINTER_ON;
          break;
        default:
          invArg();
        }
        propertyValue = Integer.valueOf(flags);
        break;
      }
      if (str.equals("toggle")) {
        iToken = 1;
        BS bs = (slen == 2 ? vwr.bsA() : atomExpressionAt(2));
        checkLast(iToken);
        if (chk)
          return true;
        vwr.shm.loadShape(JC.SHAPE_LABELS);
        vwr.shm.setShapePropertyBs(JC.SHAPE_LABELS, "toggleLabel", null, bs);
        return true;
      }
      iToken = 1;
      boolean TF = (slen == 2 || getToken(2).tok == T.on);
      if (str.equals("front") || str.equals("group")) {
        if (!TF && tokAt(2) != T.off)
          invArg();
        if (!TF)
          str = "front";
        propertyValue = (TF ? Boolean.TRUE : Boolean.FALSE);
        break;
      }
      if (str.equals("atom")) {
        if (!TF && tokAt(2) != T.off)
          invArg();
        str = "front";
        propertyValue = (TF ? Boolean.FALSE : Boolean.TRUE);
        break;
      }
      return false;
    }
    BS bs = (iToken + 1 < slen ? atomExpressionAt(++iToken) : null);
    checkLast(iToken);
    if (chk)
      return true;
    if (bs == null)
      setShapeProperty(JC.SHAPE_LABELS, str, propertyValue);
    else
      setShapePropertyBs(JC.SHAPE_LABELS, str, propertyValue, bs);
    return true;
  }

  private void cmdSetPicking() throws ScriptException {
    // set picking
    if (slen == 2) {
      setStringProperty("picking", "identify");
      return;
    }
    // set picking @{"xxx"} or some large length, ignored
    if (slen > 4 || tokAt(2) == T.string) {
      setStringProperty("picking", getSettingStr(2, false));
      return;
    }
    int i = 2;
    // set picking select ATOM|CHAIN|GROUP|MOLECULE|MODEL|SITE
    // set picking measure ANGLE|DISTANCE|TORSION
    // set picking spin fps
    String type = "SELECT";
    switch (getToken(2).tok) {
    case T.select:
    case T.measure:
    case T.spin:
      if (checkLength34() == 4) {
        type = paramAsStr(2).toUpperCase();
        if (type.equals("SPIN"))
          setIntProperty("pickingSpinRate", intParameter(3));
        else
          i = 3;
      }
      break;
    case T.delete:
      break;
    default:
      checkLength(3);
    }

    // set picking on
    // set picking normal
    // set picking identify
    // set picking off
    // set picking select
    // set picking bonds
    // set picking dragmolecule
    // set picking dragmodel
    // set picking dragselected

    String str = paramAsStr(i);
    switch (getToken(i).tok) {
    case T.on:
    case T.normal:
      str = "identify";
      break;
    case T.off:
    case T.none:
      str = "off";
      break;
    case T.select:
      str = "atom";
      break;
    case T.label:
      str = "label";
      break;
    case T.bonds: // not implemented
      str = "bond";
      break;
    case T.delete:
      checkLength(4);
      if (tokAt(3) != T.bonds)
        invArg();
      str = "deleteBond";
      break;
    }
    int mode = ((mode = str.indexOf("_")) >= 0 ? mode : str.length());
    mode = ActionManager.getPickingMode(str.substring(0, mode));
    if (mode < 0)
      errorStr2(ERROR_unrecognizedParameter, "SET PICKING " + type, str);
    setStringProperty("picking", str);
  }

  private void cmdSetPickingStyle() throws ScriptException {
    if (slen > 4 || tokAt(2) == T.string) {
      setStringProperty("pickingStyle", getSettingStr(2, false));
      return;
    }
    int i = 2;
    boolean isMeasure = false;
    String type = "SELECT";
    switch (getToken(2).tok) {
    case T.measure:
      isMeasure = true;
      type = "MEASURE";
      //$FALL-THROUGH$
    case T.select:
      if (checkLength34() == 4)
        i = 3;
      break;
    default:
      checkLength(3);
    }
    String str = paramAsStr(i);
    switch (getToken(i).tok) {
    case T.none:
    case T.off:
      str = (isMeasure ? "measureoff" : "toggle");
      break;
    case T.on:
      if (isMeasure)
        str = "measure";
      break;
    }
    if (ActionManager.getPickingStyleIndex(str) < 0)
      errorStr2(ERROR_unrecognizedParameter, "SET PICKINGSTYLE " + type, str);
    setStringProperty("pickingStyle", str);
  }

  private void cmdSlab(boolean isDepth) throws ScriptException {
    boolean TF = false;
    P4d plane = null;
    String str;
    if (isCenterParameter(1) || tokAt(1) == T.point4f)
      plane = planeParameter(1, false);
    else
      switch (getToken(1).tok) {
      case T.integer:
        int percent = intParameter(checkLast(1));
        if (!chk)
          if (isDepth)
            vwr.tm.depthToPercent(percent);
          else
            vwr.tm.slabToPercent(percent);
        return;
      case T.on:
       TF = true;
        //$FALL-THROUGH$
      case T.off:
        checkLength(2);
        setBooleanProperty("slabEnabled", TF);
        return;
      case T.reset:
        checkLength(2);
        if (chk)
          return;
        vwr.tm.slabReset();
        setBooleanProperty("slabEnabled", true);
        return;
      case T.set:
        checkLength(2);
        if (!chk)
          vwr.tm.setSlabDepthInternal(isDepth);
        return;
      case T.minus:
        str = paramAsStr(2);
        if (str.equalsIgnoreCase("hkl"))
          plane = hklParameter(3, null, true);
        else if (str.equalsIgnoreCase("plane"))
          plane = planeParameter(2, false);
        if (plane == null)
          invArg();
        plane.scale4(-1);
        break;
      case T.plane:
        switch (getToken(2).tok) {
        case T.none:
          break;
        default:
          plane = planeParameter(1, false);
        }
        break;
      case T.hkl:
        plane = (getToken(2).tok == T.none ? null : hklParameter(2, null, true));
        break;
      case T.reference:
        // only in 11.2; deprecated
        return;
      default:
        invArg();
      }
    if (!chk)
      vwr.tm.slabInternal(plane, isDepth);
  }

  /*
  private void slice() throws ScriptException{
    if(!chk && vwr.slicer==null){
     vwr.createSlicer();
    }
    int tok1 = getToken(1).tok;
    if(tok1==Token.left||tok1==Token.right){
      switch (getToken(2).tok){
      case Token.on:
        if(chk) return;
        vwr.slicer.drawSlicePlane(tok1, true);
        return;
      case Token.off:
        if(chk) return;
        vwr.slicer.drawSlicePlane(tok1, false);
        return;
      default:
        invArg();
      break;
      }
    }else{//command to slice object, not show slice planes
      String name = (String)getToken(1).value;
      //TODO - should accept "all"  for now "all" will fail silently.
      // Should check it is a valid  isosurface name
      //Should be followed by two angles, and two percents (double values)
      double[] param = new double[4];
      for (int i=2;i<6;++i){
        if(getToken(i).tok == Token.decimal){
          param[i-2]=floatParameter(i);
        } else{
          invArg();  
        }
      }
      if(!chk){
        vwr.slicer.setSlice(param[0], param[1], param[2], param[3]);
        vwr.slicer.sliceObject(name);
      }
      return; 
    }
  }
  
  */

  private void cmdSsbond() throws ScriptException {
    int mad = getMadParameter();
    if (mad == Integer.MAX_VALUE)
      return;
    setShapeProperty(JC.SHAPE_STICKS, "type",
        Integer.valueOf(Edge.BOND_SULFUR_MASK));
    setShapeSizeBs(JC.SHAPE_STICKS, mad, null);
    setShapeProperty(JC.SHAPE_STICKS, "type",
        Integer.valueOf(Edge.BOND_COVALENT_MASK));
  }

  private void cmdStructure() throws ScriptException {
    STR type = STR
        .getProteinStructureType(paramAsStr(1));
    if (type == STR.NOT)
      invArg();
    BS bs = null;
    switch (tokAt(2)) {
    case T.define:
    case T.bitset:
    case T.expressionBegin:
      bs = atomExpressionAt(2);
      checkLast(iToken);
      break;
    default:
      checkLength(2);
    }
    if (chk)
      return;
    clearDefinedVariableAtomSets();
    vwr.setProteinType(type, bs);
  }

  private void cmdSubset() throws ScriptException {
    BS bs = null;
    if (!chk)
      vwr.slm.setSelectionSubset(null);
    // hover none --> subset exprbeg "off" exprend
    if (slen != 1 && (slen != 4 || !getToken(2).value.equals("off")))
      bs = atomExpressionAt(1);
    if (!chk)
      vwr.slm.setSelectionSubset(bs);
  }

  private void cmdSync() throws ScriptException {
    // new 11.3.9
    String text = "";
    String applet = "";
    int port = PT.parseInt(optParameterAsString(1));
    if (port == Integer.MIN_VALUE) {
      checkLength(-3);
      port = 0;
      switch (slen) {
      case 1:
        // sync
        applet = "*";
        text = "ON";
        break;
      case 2:
        // sync (*) text
        applet = paramAsStr(1);
        if (applet.indexOf("jmolApplet") == 0
            || PT.isOneOf(applet, ";*;.;^;")) {
          text = "ON";
          if (!chk)
            vwr.syncScript(text, applet, 0);
          applet = ".";
          break;
        }
        text = applet;
        applet = "*";
        break;
      case 3:
        // sync applet text
        // sync applet STEREO
        applet = paramAsStr(1);
        text = (tokAt(2) == T.stereo ? Viewer.SYNC_GRAPHICS_MESSAGE
            : paramAsStr(2));
        break;
      }
    } else {
      SV v = null;
      if (slen > 2 && (v = setVariable(2, -1, "", false)) == null)
        return;
      text = (slen == 2 ? null : v.tok == T.hash ? v.toJSON() : v.asString());
      applet = null;
    }
    if (chk)
      return;
    vwr.syncScript(text, applet, port);
  }

  private void cmdThrow() throws ScriptException {
    if (chk)
      return;
    int pt = (tokAt(1) == T.context ? 2 : 1);
    SV v = (pt == 1 ? setVariable(1, slen, "thrown_value", false)
        : vwr.g.setUserVariable("thrown_value", SV.newS(optParameterAsString(2))));
    String info = v.asString();
    if (info.length() == 0 && (info = optParameterAsString(1)).length() == 0)
      info = "context";
    if (pt == 2) {
      saveContext(info);
      if (doReport())
        report(GT.o(GT.$("to resume, enter: &{0}"), info), false);
      throw new ScriptInterruption(this, info, Integer.MIN_VALUE);
    }
    evalError(info, null);
  }

  private ScriptContext saveContext(String saveName) {
    ScriptContext sc = getScriptContext("Context_" + saveName);
    vwr.stm.saveContext(saveName, sc);
    vwr.g.setUserVariable(saveName, SV.newV(T.context, sc));
    return sc;
  }

  private void cmdTimeout(int index) throws ScriptException {
    // timeout ID "mytimeout" mSec "script"
    // msec < 0 --> repeat indefinitely
    // timeout ID "mytimeout" 1000 // milliseconds
    // timeout ID "mytimeout" 0.1 // seconds
    // timeout ID "mytimeout" OFF
    // timeout ID "mytimeout" // flag to trigger waiting timeout repeat
    // timeout OFF
    String name = null;
    String script = null;
    int mSec = 0;
    if (slen == index) {
      showString(vwr.showTimeout(null));
      return;
    }
    for (int i = index; i < slen; i++)
      switch (getToken(i).tok) {
      case T.id:
        name = paramAsStr(++i);
        if (slen == 3) {
          if (!chk)
            vwr.triggerTimeout(name);
          return;
        }
        break;
      case T.off:
        break;
      case T.integer:
        mSec = intParameter(i);
        break;
      case T.decimal:
        mSec = (int) Math.round(floatParameter(i) * 1000);
        break;
      default:
        if (name == null)
          name = paramAsStr(i);
        else if (script == null)
          script = paramAsStr(i);
        else
          invArg();
        break;
      }
    if (!chk)
      vwr.setTimeout(name, mSec, script);
  }

  private void cmdTranslate(boolean isSelected) throws ScriptException {
    // translate [selected] X|Y|Z x.x [NM|ANGSTROMS]
    // translate [selected] X|Y x.x%
    // translate [selected] {x y z} [{atomExpression}]
    BS bs = null;
    int i = 1;
    int i0 = 0;
    if (tokAt(1) == T.selected) {
      isSelected = true;
      i0 = 1;
      i = 2;
    }
    if (isPoint3f(i)) {
      P3d pt = getPoint3f(i, true, true);
      bs = (iToken + 1 < slen ? atomExpressionAt(++iToken)
          : null);
      checkLast(iToken);
      if (!chk)
        vwr.setAtomCoordsRelative(pt, bs);
      return;
    }
    char xyz = (paramAsStr(i).toLowerCase() + " ").charAt(0);
    if ("xyz".indexOf(xyz) < 0)
      error(ERROR_axisExpected);
    double amount = floatParameter(++i);
    char type;
    switch (tokAt(++i)) {
    case T.nada:
    case T.define:
    case T.bitset:
    case T.expressionBegin:
      type = '\0';
      break;
    default:
      type = (optParameterAsString(i).toLowerCase() + '\0').charAt(0);
    }
    if (amount == 0 && type != '\0')
      return;
    iToken = i0 + (type == '\0' ? 2 : 3);
    bs = (isSelected ? vwr.bsA()
        : iToken + 1 < slen ? atomExpressionAt(++iToken) : null);
    checkLast(iToken);
    if (!chk) {
      vwr.translate(xyz, amount, type, bs);
      refresh(false);
    }
  }

  private void cmdUnbind() throws ScriptException {
    /*
     * unbind "MOUSE-ACTION"|all ["...script..."|actionName|all]
     */
    if (slen != 1)
      checkLength23();
    String mouseAction = optParameterAsString(1);
    String name = optParameterAsString(2);
    if (mouseAction.length() == 0 || tokAt(1) == T.all)
      mouseAction = null;
    if (name.length() == 0 || tokAt(2) == T.all)
      name = null;
    if (name == null && mouseAction != null
        && ActionManager.getActionFromName(mouseAction) >= 0) {
      name = mouseAction;
      mouseAction = null;
    }
    if (!chk)
      vwr.unBindAction(mouseAction, name);
  }

  public void cmdUndoRedo(int tok) throws ScriptException {
    // Jmol 12.1.46
    int n = 1;
    int len = 2;
    switch (tok) {
    case T.undo:
    case T.redo:
      String state = vwr.stm.getUndoRedoState(tok);
      if (state != null)
        runScript(state);  
      return;
    case T.undomove:
    case T.redomove:
      break;
    }
    switch (tokAt(1)) {
    case T.nada:
      len = 1;
      break;
    case T.all:
      n = 0;
      break;
    case T.integer:
      n = intParameter(1);
      break;
    default:
      invArg();
    }
    checkLength(len);
    if (!chk)
      vwr.undoMoveAction(tok, n);
  }

  public void setModelCagePts(int iModel, T3d[] originABC, String name) {
    if (iModel < 0)
      iModel = vwr.am.cmi;
    SymmetryInterface sym = Interface.getSymmetry(vwr, "eval");
    if (sym == null && vwr.async)
      throw new NullPointerException();
    try {
      vwr.ms.setModelCage(iModel,
          originABC == null ? null : sym.getUnitCelld(originABC, false, name));
    } catch (Exception e) {
      //
    }
  }

  private void cmdUnitcell(int i) throws ScriptException {
    getCmdExt().dispatch(T.unitcell, i == 2, null);
  }

  private void cmdVector() throws ScriptException {
    EnumType type = EnumType.SCREEN;
    double value = 1;
    checkLength(-3);
    switch (iToken = slen) {
    case 1:
      break;
    case 2:
      switch (getToken(1).tok) {
      case T.on:
        break;
      case T.off:
        value = 0;
        break;
      case T.integer:
        // diameter Pixels
        int d = intParameterRange(1, 0, 19);
        if (d == Integer.MAX_VALUE)
          return;
        value = d;
        break;
      case T.decimal:
        // radius angstroms
        type = EnumType.ABSOLUTE;
        if (Double.isNaN(value = floatParameterRange(1, 0, 3)))
          return;
        break;
      default:
        error(ERROR_booleanOrNumberExpected);
      }
      break;
    case 3:
      switch (tokAt(1)) {
      case T.trace:
          setIntProperty("vectorTrace", intParameterRange(2, 0, 20));
        return;
      case T.scale:
        if (!Double.isNaN(value = floatParameterRange(2, -100, 100)))
          setFloatProperty("vectorScale", value);
        return;
      case T.max:
        double max = floatParameter(2);
        if (!chk)
          vwr.ms.scaleVectorsToMax(max);
        return;
      }      
      break;
    }
    setShapeSize(JC.SHAPE_VECTORS, new RadiusData(null, value, type, null));
  }

  private void cmdVibration() throws ScriptException {
    checkLength(-3);
    double period = 0;
    switch (getToken(1).tok) {
    case T.on:
      checkLength(2);
      period = vwr.getDouble(T.vibrationperiod);
      break;
    case T.off:
      checkLength(2);
      period = 0;
      break;
    case T.integer:
    case T.decimal:
      checkLength(2);
      period = floatParameter(1);
      break;
    case T.scale:
      if (!Double.isNaN(period = floatParameterRange(2, -100, 100)))
        setFloatProperty("vibrationScale", period);
      return;
    case T.max:
        double max = floatParameter(2);
        if (!chk)
          vwr.ms.scaleVectorsToMax(max);
        break;
    case T.period:
      setFloatProperty("vibrationPeriod", floatParameter(2));
      return;
    case T.identifier:
      invArg();
      break;
    default:
      period = -1;
    }
    if (period < 0)
      invArg();
    if (chk)
      return;
    if (period == 0) {
      vwr.tm.setVibrationPeriod(0);
      return;
    }
    vwr.setVibrationPeriod(-period);
  }

  private void cmdWireframe() throws ScriptException {
    int mad = Integer.MIN_VALUE;
    if (tokAt(1) == T.reset)
      checkLast(1);
    else
      mad = getMadParameter();
    if (chk || mad == Integer.MAX_VALUE)
      return;
    setShapeProperty(JC.SHAPE_STICKS, "type",
        Integer.valueOf(Edge.BOND_COVALENT_MASK));
    setShapeSizeBs(JC.SHAPE_STICKS,
        mad == Integer.MIN_VALUE ? 2 * JC.DEFAULT_BOND_MILLIANGSTROM_RADIUS
            : mad, null);
  }

  private void cmdZap(boolean isZapCommand) throws ScriptException {
    if (slen == 1 || !isZapCommand) {
      boolean doAll = (isZapCommand && !isStateScript);
      if (doAll)
        vwr.cacheFileByName(null, false);
      vwr.zap(true, doAll, true);
      refresh(false);
      return;
    }
    BS bs = atomExpressionAt(1);
    if (chk)
      return;
    if (bs.nextSetBit(0) < 0 && slen == 4 && tokAt(2) == T.spec_model2) {
      int iModel = vwr.ms.getModelNumberIndex(getToken(2).intValue, false, true);
      if (iModel >= 0)
        vwr.deleteModels(iModel, null);
      return;
    }
    int nDeleted = vwr.deleteAtoms(bs, true);
    boolean isQuiet = !doReport();
    if (!isQuiet)
      report(GT.i(GT.$("{0} atoms deleted"), nDeleted), false);
    vwr.selectStatus(null, false, 0, isQuiet, false);
  }

  private void cmdZoom(boolean isZoomTo) throws ScriptException {
    if (!isZoomTo) {
      // zoom
      // zoom on|off
      int tok = (slen > 1 ? getToken(1).tok : T.on);
      switch (tok) {
      case T.in:
      case T.out:
        break;
      case T.on:
      case T.off:
        if (slen > 2)
          bad();
        if (!chk)
          setBooleanProperty("zoomEnabled", tok == T.on);
        return;
      }
    }
    P3d center = null;
    //Point3f currentCenter = vwr.getRotationCenter();
    int i = 1;
    // zoomTo time-sec
    double floatSecondsTotal = (isZoomTo ? (isFloatParameter(i) ? floatParameter(i++)
        : 1d)
        : 0d);
    if (floatSecondsTotal < 0) {
      // zoom -10
      i--;
      floatSecondsTotal = 0;
    }
    // zoom {x y z} or (atomno=3)
    int ptCenter = 0;
    BS bsCenter = null;
    if (tokAt(i) == T.unitcell || isCenterParameter(i)) {
      ptCenter = i;
      Object[] ret = new Object[1];
      center = centerParameter(i, ret);
      if (ret[0] instanceof BS)
        bsCenter = (BS) ret[0];
      i = iToken + 1;
    } else if (tokAt(i) == T.integer && getToken(i).intValue == 0) {
      bsCenter = vwr.getAtomBitSet("visible");
      center = vwr.ms.getAtomSetCenter(bsCenter);
    }

    // zoom/zoomTo [0|n|+n|-n|*n|/n|IN|OUT]
    // zoom/zoomTo percent|-factor|+factor|*factor|/factor | 0
    double zoom = vwr.tm.getZoomSetting();

    double newZoom = getZoom(ptCenter, i, bsCenter, zoom);
    i = iToken + 1;
    double xTrans = Double.NaN;
    double yTrans = Double.NaN;
    if (i != slen) {
      xTrans = floatParameter(i++);
      yTrans = floatParameter(i++);
    }
    if (i != slen)
      invArg();
    if (newZoom < 0) {
      newZoom = -newZoom; // currentFactor
      if (isZoomTo) {
        // undocumented!
        // no factor -- check for no center (zoom out) or same center (zoom in)
        if (slen == 1)
          newZoom *= 2;
        else if (center == null)
          newZoom /= 2;
      }
    }
    double max = TransformManager.MAXIMUM_ZOOM_PERCENTAGE;
    if (newZoom < 5 || newZoom > max)
      numberOutOfRange(5, max);
    if (!vwr.tm.isWindowCentered()) {
      // do a smooth zoom only if not windowCentered
      if (center != null) {
        BS bs = atomExpressionAt(ptCenter);
        if (!chk)
          vwr.setCenterBitSet(bs, false);
      }
      center = vwr.tm.fixedRotationCenter;
      if (Double.isNaN(xTrans))
        xTrans = vwr.tm.getTranslationXPercent();
      if (Double.isNaN(yTrans))
        yTrans = vwr.tm.getTranslationYPercent();
    }
    if (chk)
      return;
    if (Double.isNaN(xTrans))
      xTrans = 0;
    if (Double.isNaN(yTrans))
      yTrans = 0;
    if (!useThreads())
      floatSecondsTotal = 0;
    vwr.moveTo(this, floatSecondsTotal, center, JC.center, Double.NaN, null,
        newZoom, xTrans, yTrans, Double.NaN, null, Double.NaN, Double.NaN,
        Double.NaN, Double.NaN, Double.NaN, Double.NaN);
    if (isJS && floatSecondsTotal > 0 && vwr.g.waitForMoveTo)
      throw new ScriptInterruption(this, "zoomTo", 1);

  }

  /////////////////////////////// methods used just by cmdXXXXX methods

  private void colorShape(int shapeType, int index, boolean isBackground)
      throws ScriptException {

    boolean isScale = (Math.abs(shapeType) == T.scale);
    if (isScale) {
      shapeType = JC.SHAPE_ECHO;
    } else if (shapeType < 0) {
      shapeType = getShapeType(-shapeType);
    }
    String translucency = null;
    Object colorvalue = null;
    Object colorvalue1 = null;
    BS bs = null;
    String prefix = (index == 2 && tokAt(1) == T.balls ? "ball" : "");
    boolean isIsosurface = (shapeType == JC.SHAPE_ISOSURFACE || shapeType == JC.SHAPE_CONTACT);
    boolean doClearBondSet = false;
    double translucentLevel = Double.MAX_VALUE;
    if (index < 0) {
      bs = atomExpressionAt(-index);
      index = iToken + 1;
      if (isBondSet) {
        doClearBondSet = true;
        shapeType = JC.SHAPE_STICKS;
      }
    }
    int tok = getToken(index).tok;
    if (isBackground)
      getToken(index);
    else if ((isBackground = (tok == T.background)) == true)
      getToken(++index);
    if (isBackground)
      prefix = "bg";
    else if (isIsosurface) {
      switch (theTok) {
      case T.mesh:
        getToken(++index);
        prefix = "mesh";
        break;
      case T.phase:
        int argb = getArgbParamOrNone(++index, false);
        colorvalue1 = (argb == 0 ? null : Integer.valueOf(argb));
        getToken(index = iToken + 1);
        break;
      case T.define:
      case T.bitset:
      case T.expressionBegin:
        if (theToken.value instanceof BondSet) {
          bs = (BondSet) theToken.value;
          prefix = "vertex";
        } else {
          bs = atomExpressionAt(index);
          prefix = "atom";
        }
        // don't allow isosurface partial translucency (yet)
        //translucentLevel = Parser.FLOAT_MIN_SAFE;
        getToken(index = iToken + 1);
        break;
      }
    }
    if (!chk && (shapeType == JC.SHAPE_MO || shapeType == JC.SHAPE_NBO)
        && getIsoExt().dispatch(shapeType, true, st) != null)
      return;
    boolean isTranslucent = (theTok == T.translucent);
    if (isTranslucent || theTok == T.opaque) {
      if (translucentLevel == PT.FLOAT_MIN_SAFE)
        invArg();
      translucency = paramAsStr(index++);
      if (isTranslucent && isFloatParameter(index))
        translucentLevel = getTranslucentLevel(index++);
    }
    tok = 0;
    boolean isColor = (index < slen && tokAt(index) != T.on && tokAt(index) != T.off);
    if (isColor) {
      tok = getToken(index).tok;
      if ((!isIsosurface || tokAt(index + 1) != T.to) && isColorParam(index)) {
        int argb = getArgbParamOrNone(index, false);
        colorvalue = (argb == 0 ? null : Integer.valueOf(argb));
        if (tokAt(index = iToken + 1) != T.nada && translucency == null) {
          getToken(index);
          isTranslucent = (theTok == T.translucent);
          if (isTranslucent || theTok == T.opaque) {
            translucency = paramAsStr(index++);
            if (isTranslucent && isFloatParameter(index))
              translucentLevel = getTranslucentLevel(index++);
          }
        }
        if (isColorParam(index)) {
          argb = getArgbParamOrNone(index, false);
          colorvalue1 = (argb == 0 ? null : Integer.valueOf(argb));
          index = iToken + 1;
        }
        checkLength(index);
      } else if (shapeType == JC.SHAPE_LCAOCARTOON) {
        iToken--; // back up one
      } else {
        // must not be a color, but rather a color SCHEME
        // this could be a problem for properties, which can't be
        // checked later -- they must be turned into a color NOW.

        // "cpk" value would be "spacefill"
        String name = paramAsStr(index).toLowerCase();
        boolean isByElement = (name.indexOf(ColorEncoder.BYELEMENT_PREFIX) == 0);
        boolean isColorIndex = (isByElement || name
            .indexOf(ColorEncoder.BYRESIDUE_PREFIX) == 0);
        PAL pal = (isColorIndex || isIsosurface ? PAL.PROPERTY
            : tok == T.spacefill ? PAL.CPK : PAL.getPalette(name));
        // color atoms "cpkScheme"
        if (pal == PAL.UNKNOWN || (pal == PAL.TYPE || pal == PAL.ENERGY)
            && shapeType != JC.SHAPE_HSTICKS)
          invArg();
        Object data = null;
        BS bsSelected = (pal != PAL.PROPERTY && pal != PAL.VARIABLE
            || !vwr.g.rangeSelected ? null : vwr.bsA());
        if (pal == PAL.PROPERTY) {
          if (isColorIndex) {
            if (!chk) {
              data = getCmdExt().getBitsetPropertyFloat(bsSelected, (isByElement ? T.elemno
                  : T.groupid) | T.allfloat, null, Double.NaN, Double.NaN);
            }
          } else {
            boolean isPropertyExplicit = name.equals("property");
            // problem here with   color $isosurface1 "rwb"
            if (isPropertyExplicit
                && T.tokAttr((tok = getToken(++index).tok), T.atomproperty)
                && !T.tokAttr(tok, T.strproperty)) {
              tok = getToken(index).tok;
              String type = (tok == T.dssr ? getToken(++index).value.toString() : null);
              if (!chk) {
                data = getCmdExt().getBitsetPropertyFloat(bsSelected, tok
                    | T.allfloat, type, Double.NaN, Double.NaN);
              }
              index++;
            } else if (!isPropertyExplicit && !isIsosurface) {
              index++;
            }
            // index points to item after property
          }
        } else if (pal == PAL.VARIABLE) {
          index++;
          name = paramAsStr(index++);
          data = new double[vwr.ms.ac];
          Parser.parseStringInfestedDoubleArray(
              "" + getParameter(name, T.string, true), null, (double[]) data);
          pal = PAL.PROPERTY;
        }
        // index here points to NEXT item
        if (pal == PAL.PROPERTY) {
          String scheme = null;
          if (tokAt(index) == T.string) {
            scheme = paramAsStr(index++).toLowerCase();
            if (isArrayParameter(index)) {
              scheme += "="
                  + SV.sValue(SV.getVariableAS(stringParameterSet(index)))
                      .replace('\n', ' ');
              index = iToken + 1;
            }
          } else if (isIsosurface && isColorParam(index)) {
            scheme = getColorRange(index);
            index = iToken + 1;
          }
          if (scheme != null && !isIsosurface) {
            setStringProperty("propertyColorScheme", (isTranslucent
                && translucentLevel == Double.MAX_VALUE ? "translucent " : "")
                + scheme);
            isColorIndex = (scheme.indexOf(ColorEncoder.BYELEMENT_PREFIX) == 0 || scheme
                .indexOf(ColorEncoder.BYRESIDUE_PREFIX) == 0);
          }
          double min = 0;
          double max = Double.MAX_VALUE;
          if (!isColorIndex
              && (tokAt(index) == T.absolute || tokAt(index) == T.range)) {
            min = floatParameter(index + 1);
            max = floatParameter(index + 2);
            index += 3;
            if (min == max && isIsosurface) {
              double[] range = (double[]) getShapeProperty(shapeType, "dataRange");
              if (range != null) {
                min = range[0];
                max = range[1];
              }
            } else if (min == max) {
              max = Double.MAX_VALUE;
            }
          }
          if (isIsosurface) {
          } else if (data == null) {
            if (!chk)
              vwr.setCurrentColorRange(name);
          } else {
            if (!chk)
              vwr.cm.setPropertyColorRangeData((double[]) data, bsSelected);
          }
          if (isIsosurface) {
            checkLength(index);
            if (chk)
              return;
            isColor = false;
            ColorEncoder ce = (scheme == null ? (ColorEncoder) getShapeProperty(shapeType, "colorEncoder") : null);
            if (ce == null && (ce = vwr.cm.getColorEncoder(scheme)) == null)
              return;
            ce.isTranslucent = (isTranslucent && translucentLevel == Double.MAX_VALUE);
            ce.setRange(min, max, min > max);
            if (max == Double.MAX_VALUE)
              ce.hi = max;
            setShapeProperty(shapeType, "remapColor", ce);
            showString(((String) getShapeProperty(shapeType, "dataRangeStr"))
                .replace('\n', ' '));
            if (translucentLevel == Double.MAX_VALUE)
              return;
          } else if (max != Double.MAX_VALUE) {
            vwr.cm.setPropertyColorRange(min, max);
          }
        } else {
          index++;
        }
        checkLength(index);
        colorvalue = pal;
      }
    }
    if (chk || shapeType < 0)
      return;
    if (isScale)
      setShapeProperty(JC.SHAPE_ECHO, "target", "%SCALE");
    int typeMask;
    switch (shapeType) {
    case JC.SHAPE_STRUTS:
      typeMask = Edge.BOND_STRUT;
      break;
    case JC.SHAPE_HSTICKS:
      typeMask = Edge.BOND_HYDROGEN_MASK;
      break;
    case JC.SHAPE_SSSTICKS:
      typeMask = Edge.BOND_SULFUR_MASK;
      break;
    case JC.SHAPE_STICKS:
      typeMask = Edge.BOND_COVALENT_MASK;
      break;
    default:
      typeMask = 0;
    }
    if (typeMask == 0) {
      sm.loadShape(shapeType);
      if (shapeType == JC.SHAPE_LABELS)
        setShapeProperty(JC.SHAPE_LABELS, "setDefaults", vwr.slm.noneSelected);
    } else {
      if (bs != null) {
        vwr.selectBonds(bs);
        bs = null;
      }
      shapeType = JC.SHAPE_STICKS;
      setShapeProperty(shapeType, "type", Integer.valueOf(typeMask));
    }
    if (isColor) {
      // ok, the following options require precalculation.
      // the state must not save them as paletteIDs, only as pure
      // color values.
      switch (tok) {
      case T.partialcharge:
        getPartialCharges(bs);
        break;
      case T.surfacedistance:
      case T.straightness:
        vwr.autoCalculate(tok, null);
        break;
      case T.temperature:
        if (vwr.g.rangeSelected)
          vwr.ms.clearBfactorRange();
        break;
      case T.group:
        vwr.ms.calcSelectedGroupsCount();
        break;
      case T.polymer:
      case T.monomer:
        vwr.ms.calcSelectedMonomersCount();
        break;
      case T.molecule:
        vwr.ms.calcSelectedMoleculesCount();
        break;
      }
      if (colorvalue1 != null
          && (isIsosurface || shapeType == JC.SHAPE_CARTOON
              || shapeType == JC.SHAPE_RIBBONS || shapeType == JC.SHAPE_POLYHEDRA))
        setShapeProperty(shapeType, "colorPhase", new Object[] { colorvalue1,
            colorvalue });
      else if (bs == null)
        setShapeProperty(shapeType, prefix + "color", colorvalue);
      else
        setShapePropertyBs(shapeType, prefix + "color", colorvalue, bs);
    }
    if (translucency != null)
      setShapeTranslucency(shapeType, prefix, translucency, translucentLevel,
          bs);
    if (isScale)
      setShapeProperty(JC.SHAPE_ECHO, "thisID", null);
    if (typeMask != 0)
      setShapeProperty(JC.SHAPE_STICKS, "type",
          Integer.valueOf(Edge.BOND_COVALENT_MASK));
    if (doClearBondSet)
      vwr.selectBonds(null);
    if (shapeType == JC.SHAPE_BALLS)
      vwr.shm.checkInheritedShapes();
  }

  /*
   * Based on the form of the parameters, returns and encoded radius as follows:
   * 
   * script meaning range
   * 
   * +1.2 offset [0 - 10] 
   * -1.2 offset 0) 
   * 1.2 absolute (0 - 10] 
   * -30% 70% (-100 - 0) 
   * +30% 130% (0 
   * 80% percent (0
   */

  public void getPartialCharges(BS bs) throws ScriptException  {
    try {
      vwr.getOrCalcPartialCharges(bs, null);
    } catch (Exception e) {
      throw new ScriptInterruption(this, "partialcharge", 1); 
    }
  }

  public RadiusData encodeRadiusParameter(int index, boolean isOnly,
                                          boolean allowAbsolute)
      throws ScriptException {

    double value = Double.NaN;
    EnumType factorType = EnumType.ABSOLUTE;
    VDW vdwType = null;

    int tok = (index == -1 ? T.vanderwaals : getToken(index).tok);
    switch (tok) {
    case T.adpmax:
    case T.adpmin:
    case T.bondingradius:
    case T.hydrophobicity:
    case T.temperature:
    case T.vanderwaals:
      value = 1;
      factorType = EnumType.FACTOR;
      vdwType = (tok == T.vanderwaals ? null : VDW.getVdwType2(T
          .nameOf(tok)));
      tok = tokAt(++index);
      break;
    }
    switch (tok) {
    case T.reset:
      return vwr.rd;
    case T.auto:
    case T.rasmol:
    case T.babel:
    case T.babel21:
    case T.jmol:
      value = 1;
      factorType = EnumType.FACTOR;
      iToken = index - 1;
      break;
    case T.plus:
    case T.integer:
    case T.decimal:
      if (tok == T.plus) {
        index++;
      } else if (tokAt(index + 1) == T.percent) {
        value = (int) Math.round(floatParameter(index));
        iToken = ++index;
        factorType = EnumType.FACTOR;
        if (Math.abs(value) > 200) {
          integerOutOfRange(0, 200);
          return null;
        }
        if (isOnly)
          value = -value;
        value /= 100;
        break;
      } else if (tok == T.integer) {
        value = intParameter(index);
        // rasmol 250-scale if positive or percent (again), if negative
        // (deprecated)
        if (value > 749 || value < -200) {
          integerOutOfRange(-200, 749);
            return null;

          }
        if (value > 0) {
          value /= 250;
          factorType = EnumType.ABSOLUTE;
        } else {
          value /= -100;
          factorType = EnumType.FACTOR;
        }
        break;
      }
      double max;
      if (tok == T.plus || !allowAbsolute) {
        factorType = EnumType.OFFSET;
        max = Atom.RADIUS_MAX;
      } else {
        factorType = EnumType.ABSOLUTE;
        vdwType = VDW.NADA;
        max = 100;
      }
      value = floatParameterRange(index, (isOnly || !allowAbsolute ? -max : 0),
          max);
      if (Double.isNaN(value))
        return null;
      if (isOnly)
        value = -value;
      if (value > Atom.RADIUS_MAX)
        value = Atom.RADIUS_GLOBAL;
      break;
    default:
      if (value == 1)
        index--;
    }
    if (vdwType == null) {
      vdwType = VDW.getVdwType(optParameterAsString(++iToken));
      if (vdwType == null) {
        iToken = index;
        vdwType = VDW.AUTO;
      }
    }
    return new RadiusData(null, value, factorType, vdwType);
  }

  /**
   * Accepts a double array and expands [1 -3] to [1 2 3], for example.
   * 
   * @param a
   * @param min 
   * @param asBS 
   * @return double[] or BS
   * @throws ScriptException 
   */
  public Object expandDoubleArray(double[] a, int min, boolean asBS) throws ScriptException {
    int n = a.length;
    boolean haveNeg = false;
    BS bs = (asBS ? new BS() : null);
    try {
      for (int i = 0; i < a.length; i++)
        if (a[i] < 0) {
          n += Math.abs(a[i - 1] + a[i]) - 1; // 100 - 102 or 11 - 3
          haveNeg = true;
        }
      if (haveNeg) {
        double[] b = (asBS ? null : new double[n]);
        for (int pt = 0, i = 0; i < a.length; i++) {
          n = (int) a[i];
          if (n >= 0) {
            if (n < min)
              invArg();
            if (asBS)
              bs.set(n - 1);
            else
              b[pt++] = n;            
          } else {
            int j = (int) a[i - 1];
            int dir = (j <= -n ? 1 : -1);
            for (int j2 = -n; j != j2; j += dir, pt++)
              if (!asBS)
                b[pt] = j + dir;
              else
                bs.set(j);
          }
        }
        a = b;
        if (!asBS)
          n = a.length;
      }
      if (asBS) {
        for (int i = n; --i >= 0;)
          bs.set((int) a[i] - 1);
        return bs;
      }
      int[] ia = new int[n];
      for (int i = n; --i >= 0;)
        ia[i] = (int) a[i];
      return ia;
    } catch (Exception e) {
      invArg();
      return null;
    }
  }

  private void frameControl(int i) throws ScriptException {
    switch (getToken(checkLast(i)).tok) {
    case T.playrev:
    case T.play:
    case T.resume:
    case T.pause:
    case T.next:
    case T.prev:
    case T.rewind:
    case T.first:
    case T.last:
      if (!chk)
        vwr.setAnimation(theTok);
      return;
    }
    invArg();
  }

  public String getColorRange(int i) throws ScriptException {
    int color1 = getArgbParam(i);
    if (tokAt(++iToken) != T.to)
      invArg();
    int color2 = getArgbParam(++iToken);
    int nColors = (tokAt(iToken + 1) == T.integer ? intParameter(++iToken) : 0);
    return ColorEncoder.getColorSchemeList(ColorEncoder.getPaletteAtoB(color1,
        color2, nColors));
  }

  public String getFullPathName(boolean withType) throws ScriptException {
    String filename = (!chk || isCmdLine_C_Option ? vwr
        .fm.getFullPathName(true) : "test.xyz");
    if (filename == null)
      invArg();
    if (withType) {
      String ft = vwr.fm.getFileType();
      if (ft != null && ft.length() > 0)
        filename = ft + "::" + filename;
    }
    return filename;
  }

  private P3d[] getObjectBoundingBox(String id) {
    Object[] data = new Object[] { id, null, null };
    return (getShapePropertyData(JC.SHAPE_ISOSURFACE, "getBoundingBox", data)
        || getShapePropertyData(JC.SHAPE_PMESH, "getBoundingBox", data)
        //        || getShapePropertyData(JC.SHAPE_UCCAGE, "getBoundingBox", data)
        || getShapePropertyData(JC.SHAPE_CONTACT, "getBoundingBox", data)
        || getShapePropertyData(JC.SHAPE_NBO, "getBoundingBox", data)
        || getShapePropertyData(JC.SHAPE_MO, "getBoundingBox", data)
            ? (P3d[]) data[2]
            : null);
  }

  protected P3d getObjectCenter(String axisID, int index, int modelIndex) {

    // called by ScriptParam
    
    Object[] data = new Object[] { axisID, Integer.valueOf(index),
        Integer.valueOf(modelIndex) };
    return (getShapePropertyData(JC.SHAPE_DRAW, "getCenter", data)
        || getShapePropertyData(JC.SHAPE_ISOSURFACE, "getCenter", data)
        || getShapePropertyData(JC.SHAPE_PMESH, "getCenter", data)
        || getShapePropertyData(JC.SHAPE_CONTACT, "getCenter", data)
        || getShapePropertyData(JC.SHAPE_NBO, "getCenter", data)
        || getShapePropertyData(JC.SHAPE_MO, "getCenter", data) ? (P3d) data[2]
        : null);
  }

  protected P4d getPlaneForObject(String id, V3d vAB) {

    // called by ScriptParam
    
    int shapeType = sm.getShapeIdFromObjectName(id);
    switch (shapeType) {
    case JC.SHAPE_DRAW:
      setShapeProperty(JC.SHAPE_DRAW, "thisID", id);
      T3d[] points = (T3d[]) getShapeProperty(JC.SHAPE_DRAW, "vertices");
      if (points == null || points.length < 3 || points[0] == null
          || points[1] == null || points[2] == null)
        break;
      return MeasureD.getPlaneThroughPoints(points[0], points[1], points[2],
          new V3d(), vAB, new P4d());
    case JC.SHAPE_ISOSURFACE:
      setShapeProperty(JC.SHAPE_ISOSURFACE, "thisID", id);
      return (P4d) getShapeProperty(JC.SHAPE_ISOSURFACE, "plane");
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public Qd[] getQuaternionArray(Object quaternionOrSVData, int itype) {
    Qd[] data;
    switch (itype) {
    case T.quaternion:
      data = (Qd[]) quaternionOrSVData;
      break;
    case T.point4f:
      P4d[] pts = (P4d[]) quaternionOrSVData;
      data = new Qd[pts.length];
      for (int i = 0; i < pts.length; i++)
        data[i] = Qd.newP4(pts[i]);
      break;
    case T.list:
      Lst<SV> sv = (Lst<SV>) quaternionOrSVData;
      data = new Qd[sv.size()];
      for (int i = 0; i < sv.size(); i++) {
        P4d pt = SV.pt4Value(sv.get(i));
        if (pt == null)
          return null;
        data[i] = Qd.newP4(pt);
      }
      break;
    default:
      return null;
    }
    return data;
  }

  public int getSetAxesTypeMad10(int index) throws ScriptException {
    if (index == slen)
      return 1;
    switch (getToken(checkLast(index)).tok) {
    case T.on:
      return 1;
    case T.off:
      return 0;
    case T.dotted:
      return -1;
    case T.integer:
      return intParameterRange(index, -1, 19);
    case T.decimal:
      double angstroms = floatParameterRange(index, 0, 2);
      return (Double.isNaN(angstroms) ? Integer.MAX_VALUE : (int) Math.floor(angstroms * 10000 * 2));
    }
    if (!chk)
      errorStr(ERROR_booleanOrWhateverExpected, "\"DOTTED\"");
    return 0;
  }

  private double getSettingFloat(int pt) throws ScriptException {
    return (pt >= slen ? Double.NaN : SV.dValue(parameterExpressionToken(pt)));
}

  private int getSettingInt(int pt) throws ScriptException {
    return (pt >= slen ? Integer.MIN_VALUE : parameterExpressionToken(pt)
        .asInt());
  }

  /**
   * Accept an unquoted string if there is just one parameter regardless of its
   * type. In other words, these commands cannot accept a variable name by
   * itself.
   * 
   * @param pt
   * @param isJmolSet
   * @return string parameter
   * @throws ScriptException
   */
  private String getSettingStr(int pt, boolean isJmolSet)
      throws ScriptException {
    return (isJmolSet && slen == pt + 1 ? paramAsStr(pt)
        : parameterExpressionToken(pt).asString());
  }

  public Object getShapeProperty(int shapeType, String propertyName) {
    return sm.getShapePropertyIndex(shapeType, propertyName, Integer.MIN_VALUE);
  }

  public boolean getShapePropertyData(int shapeType, String propertyName,
                                      Object[] data) {
    return sm.getShapePropertyData(shapeType, propertyName, data);
  }

  private int getShapeType(int tok) throws ScriptException {
    int iShape = JC.shapeTokenIndex(tok);
    if (iShape < 0)
      error(ERROR_unrecognizedObject);
    return iShape;
  }

  public double getTranslucentLevel(int i) throws ScriptException {
    double f = doubleParameter(i);
    return (theTok == T.integer && f > 0 && f < 9 ? f + 1 : f);
  }

  private double getZoom(int ptCenter, int i, BS bs, double currentZoom)
      throws ScriptException {
    // where [zoom factor] is [0|n|+n|-n|*n|/n|IN|OUT]

    double zoom = (isFloatParameter(i) ? floatParameter(i++) : Double.NaN);
    if (zoom == 0 || currentZoom == 0) {
      // moveTo/zoom/zoomTo {center} 0
      double r = Double.NaN;
      if (bs == null) {
        switch (tokAt(ptCenter)) {
        case T.unitcell:
          SymmetryInterface uc = vwr.getCurrentUnitCell();
          if (uc == null)
            invArg(); 
          P3d[] pts = uc.getUnitCellVerticesNoOffset();
          P3d off = uc.getCartesianOffset();
          P3d pt = new P3d();
          BoxInfo bi = new BoxInfo();
          for (int j = 0; j < 8; j++) {
            pt.add2(off, pts[j]);
            bi.addBoundBoxPoint(pt);
          }
          bi.setBbcage(1);
          r = bi.bbCorner0.distance(bi.bbCorner1) / 2;
          if (r == 0)
            invArg();
          break;
        case T.dollarsign:
          P3d[] bbox = getObjectBoundingBox(objectNameParameter(ptCenter + 1));
          if (bbox == null || (r = bbox[0].distance(bbox[1]) / 2) == 0)
            invArg(); 
          break;          
        }
      } else {
        r = vwr.ms.calcRotationRadiusBs(bs);
      }
      if (Double.isNaN(r))
        invArg();
      currentZoom = vwr.getDouble(T.rotationradius) / r * 100;
      zoom = Double.NaN;
    }
    if (zoom < 0) {
      // moveTo/zoom/zoomTo -factor
      zoom += currentZoom;
    } else if (Double.isNaN(zoom)) {
      // moveTo/zoom/zoomTo [optional {center}] percent|+factor|*factor|/factor
      // moveTo/zoom/zoomTo {center} 0 [optional
      // -factor|+factor|*factor|/factor]
      int tok = tokAt(i);
      switch (tok) {
      case T.out:
      case T.in:
        zoom = currentZoom * (tok == T.out ? 0.5d : 2d);
        i++;
        break;
      case T.divide:
      case T.times:
      case T.plus:
        double value = floatParameter(++i);
        i++;
        switch (tok) {
        case T.divide:
          zoom = currentZoom / value;
          break;
        case T.times:
          zoom = currentZoom * value;
          break;
        case T.plus:
          zoom = currentZoom + value;
          break;
        }
        break;
      default:
        // indicate no factor indicated
        zoom = (bs == null ? -currentZoom : currentZoom);
      }
    }
    iToken = i - 1;
    return zoom;
  }

  private boolean setElementColor(String str, int argb) {
    for (int i = Elements.elementNumberMax; --i >= 0;) {
      if (str.equalsIgnoreCase(Elements.elementNameFromNumber(i))) {
        if (!chk)
          vwr.setElementArgb(i, argb);
        return true;
      }
    }
    for (int i = Elements.altElementMax; --i >= 0;) {
      if (str.equalsIgnoreCase(Elements.altElementNameFromIndex(i))) {
        if (!chk)
          vwr.setElementArgb(Elements.altElementNumberFromIndex(i), argb);
        return true;
      }
    }
    if (str.charAt(0) != '_')
      return false;
    for (int i = Elements.elementNumberMax; --i >= 0;) {
      if (str.equalsIgnoreCase("_" + Elements.elementSymbolFromNumber(i))) {
        if (!chk)
          vwr.setElementArgb(i, argb);
        return true;
      }
    }
    for (int i = Elements.altElementMax; --i >= Elements.firstIsotope;) {
      if (str.equalsIgnoreCase("_" + Elements.altElementSymbolFromIndex(i))) {
        if (!chk)
          vwr.setElementArgb(Elements.altElementNumberFromIndex(i), argb);
        return true;
      }
      if (str.equalsIgnoreCase("_" + Elements.altIsotopeSymbolFromIndex(i))) {
        if (!chk)
          vwr.setElementArgb(Elements.altElementNumberFromIndex(i), argb);
        return true;
      }
    }
    return false;
  }

  /**
   * @param shape
   * @param i
   * @param tok
   * @return true if successful
   * @throws ScriptException
   */
  public boolean setMeshDisplayProperty(int shape, int i, int tok)
      throws ScriptException {
    String propertyName = null;
    Object propertyValue = null;
    boolean allowCOLOR = (shape == JC.SHAPE_CONTACT);
    boolean checkOnly = (i == 0);
    // these properties are all processed in MeshCollection.java
    if (!checkOnly)
      tok = getToken(i).tok;
    switch (tok) {
    case T.color:
      if (allowCOLOR)
        iToken++;
      else
        break;
      //$FALL-THROUGH$
    case T.opaque:
    case T.translucent:
      if (!checkOnly)
        colorShape(shape, iToken, false);
      return true;
    case T.nada:
    case T.delete:
    case T.on:
    case T.off:
    case T.hide:
    case T.hidden:
    case T.display:
    case T.displayed:
      if (iToken == 1 && shape >= 0 && tokAt(2) == T.nada)
        setShapeProperty(shape, "thisID", null);
      if (tok == T.nada)
        return (iToken == 1);
      if (checkOnly)
        return true;
      switch (tok) {
      case T.delete:
        setShapeProperty(shape, "delete", null);
        return true;
      case T.hidden:
      case T.hide:
        tok = T.off;
        break;
      case T.displayed:
        tok = T.on;
        break;
      case T.display:
        if (i + 1 == slen)
          tok = T.on;
        break;
      }
      //$FALL-THROUGH$ for on/off/display
    case T.frontlit:
    case T.backlit:
    case T.fullylit:
    case T.contourlines:
    case T.nocontourlines:
    case T.dots:
    case T.nodots:
    case T.mesh:
    case T.nomesh:
    case T.fill:
    case T.nofill:
    case T.backshell:
    case T.nobackshell:
    case T.triangles:
    case T.notriangles:
    case T.frontonly:
    case T.notfrontonly:
      propertyName = "token";
      propertyValue = Integer.valueOf(tok);
      break;
    }
    if (propertyName == null)
      return false;
    if (checkOnly)
      return true;
    setShapeProperty(shape, propertyName, propertyValue);
    if ((tokAt(iToken + 1)) != T.nada) {
      if (!setMeshDisplayProperty(shape, ++iToken, 0))
        --iToken;
    }
    return true;
  }

  private void setObjectArgb(String str, int argb) {
    if (chk)
      return;
    vwr.setObjectArgb(str, argb);
  }

  public void setObjectMad10(int iShape, String name, int mad10) {
    if (!chk)
      vwr.setObjectMad10(iShape, name, mad10);
  }

  private String setObjectProp(String id, int tokCommand, int ptColor)
      throws ScriptException {
    Object[] data = new Object[] { id, null };
    String s = "";
    boolean isWild = PT.isWild(id);
    for (int iShape = JC.SHAPE_DIPOLES;;) {
      if (getShapePropertyData(iShape, "checkID", data)) {
        setShapeProperty(iShape, "thisID", id);
        switch (tokCommand) {
        case T.delete:
          setShapeProperty(iShape, "delete", null);
          break;
        case T.hide:
        case T.display:
          setShapeProperty(iShape, "hidden",
              tokCommand == T.display ? Boolean.FALSE : Boolean.TRUE);
          break;
        case T.show:
          //if (iShape == JmolConstants.SHAPE_ISOSURFACE && !isWild)
          //return getIsosurfaceJvxl(false, JmolConstants.SHAPE_ISOSURFACE);
          //else if (iShape == JmolConstants.SHAPE_PMESH && !isWild)
          //return getIsosurfaceJvxl(true, JmolConstants.SHAPE_PMESH);
          s += (String) getShapeProperty(iShape, "command") + "\n";
          break;
        case T.color:
          if (ptColor >= 0)
            colorShape(iShape, ptColor + 1, false);
          break;
        }
        if (!isWild)
          break;
      }
      switch (iShape) {
      case JC.SHAPE_DIPOLES:
        iShape = JC.SHAPE_ELLIPSOIDS;
        continue;
      case JC.SHAPE_ELLIPSOIDS:
        iShape = JC.SHAPE_MAX_HAS_ID;
      }
      switch (--iShape) {
      // skip MO and NBO?
      case JC.SHAPE_MO:
        iShape--;
        break;
      case JC.SHAPE_NBO:
        iShape -= 2;
        break;
      }
      if (iShape < JC.SHAPE_MIN_HAS_ID)
        break;        
    }
    return s;
  }

  public String setObjectProperty() throws ScriptException {
    // also called by show command, in ScriptExt
    String id = setShapeNameParameter(2);
    return (chk ? "" : setObjectProp(id, tokAt(0), iToken));
  }

  public String setShapeNameParameter(int i) throws ScriptException {
    String id = paramAsStr(i);
    boolean isWild = id.equals("*");
    if (id.length() == 0)
      invArg();
    if (isWild) {
      switch (tokAt(i + 1)) {
      case T.nada:
      case T.on:
      case T.off:
      case T.displayed:
      case T.hidden:
      case T.color:
      case T.delete:
        break;
      default:
        if (setMeshDisplayProperty(-1, 0, tokAt(i + 1)))
          break;
        id += optParameterAsString(++i);
      }
    }
    if (tokAt(i + 1) == T.times)
      id += paramAsStr(++i);
    iToken = i;
    return id;
  }

  public void setShapeProperty(int shapeType, String propertyName,
                               Object propertyValue) {
    if (!chk)
      sm.setShapePropertyBs(shapeType, propertyName, propertyValue, null);
  }

  public void setShapePropertyBs(int iShape, String propertyName,
                                 Object propertyValue, BS bs) {
    if (!chk)
      sm.setShapePropertyBs(iShape, propertyName, propertyValue, bs);
  }

  private void setShapeSize(int shapeType, RadiusData rd) {
    if (!chk)
      sm.setShapeSizeBs(shapeType, 0, rd, null);
  }

  public void setShapeSizeBs(int shapeType, int size, BS bs) {
    // stars, halos, balls only
    if (!chk)
      sm.setShapeSizeBs(shapeType, size, null, bs);
  }

  public void setShapeTranslucency(int shapeType, String prefix,
                                   String translucency, double translucentLevel,
                                   BS bs) {
    if (translucentLevel == Double.MAX_VALUE)
      translucentLevel = vwr.getDouble(T.defaulttranslucent);
    setShapeProperty(shapeType, "translucentLevel",
        Double.valueOf(translucentLevel));
    if (prefix == null)
      return;
    if (bs == null)
      setShapeProperty(shapeType, prefix + "translucency", translucency);
    else if (!chk)
      setShapePropertyBs(shapeType, prefix + "translucency", translucency, bs);
  }

  private void setSize(int shape, double scale) throws ScriptException {
    // halo star spacefill
    RadiusData rd = null;
    int tok = tokAt(1);
    boolean isOnly = false;
    switch (tok) {
    case T.only:
      restrictSelected(false, false);
      //$FALL-THROUGH$
    case T.on:
      break;
    case T.off:
      scale = 0;
      break;
    case T.decimal:
    case T.integer:
      isOnly = (floatParameter(1) < 0 && (tok == T.decimal || tokAt(2) == T.percent));
      //$FALL-THROUGH$
    default:
      rd = encodeRadiusParameter(1, isOnly, true);
      if (rd == null)
        return;
      if (Double.isNaN(rd.value))
        invArg();
    }
    if (rd == null)
      rd = new RadiusData(null, scale, EnumType.FACTOR, VDW.AUTO);
    if (isOnly)
      restrictSelected(false, false);
    setShapeSize(shape, rd);
  }

  private void setSizeBio(int iShape) throws ScriptException {
    int mad = 0;
    // token has ondefault1
    switch (getToken(1).tok) {
    case T.only:
      restrictSelected(false, false);
      //$FALL-THROUGH$
    case T.on:
      mad = -1; // means take default
      break;
    case T.off:
      break;
    case T.structure:
      mad = -2;
      break;
    case T.temperature:
    case T.displacement:
      mad = -4;
      break;
    case T.integer:
      if ((mad = (intParameterRange(1, 0, 1000) * 8)) == Integer.MAX_VALUE)
          return;
      break;
    case T.decimal:
      mad = (int) Math.round(floatParameterRange(1, -Shape.RADIUS_MAX,
          Shape.RADIUS_MAX) * 2000);
      if (mad == Integer.MAX_VALUE)
        return;
      if (mad < 0) {
        restrictSelected(false, false);
        mad = -mad;
      }
      break;
    case T.bitset:
      if (!chk)
        sm.loadShape(iShape);
      setShapeProperty(iShape, "bitset", theToken.value);
      return;
    default:
      error(ERROR_booleanOrNumberExpected);
    }
    setShapeSizeBs(iShape, mad, null);
  }

  private boolean setUnits(String units, int tok) throws ScriptException {
    if (tok == T.measurementunits
        && (units.toLowerCase().endsWith("hz") || PT.isOneOf(units.toLowerCase(),
            ";angstroms;au;bohr;nanometers;nm;picometers;pm;vanderwaals;vdw;"))) {
      if (!chk)
        vwr.setUnits(units, true);
    } else if (tok == T.energyunits
        && PT.isOneOf(units.toLowerCase(), ";kcal;kj;")) {
      if (!chk)
        vwr.setUnits(units, false);
    } else {
      errorStr2(ERROR_unrecognizedParameter, "set " + T.nameOf(tok), units);
    }
    return true;
  }

  
  
  @Override
  public String toString() {
    SB str = new SB();
    str.append("Eval\n pc:");
    str.appendI(pc);
    str.append("\n");
    str.appendI(aatoken.length);
    str.append(" statements\n");
    for (int i = 0; i < aatoken.length; ++i) {
      str.append("----\n");
      T[] atoken = aatoken[i];
      for (int j = 0; j < atoken.length; ++j) {
        str.appendO(atoken[j]);
        str.appendC('\n');
      }
      str.appendC('\n');
    }
    str.append("END\n");
    return str.toString();
  }

}
